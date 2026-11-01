package io.github.jimbozoomer.jugcraft.town;

import java.util.EnumSet;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A townsperson: player-shaped, wearing one of the town's own skins, with a name and a role. Nobody can hurt them
 * (only {@code /kill} removes one, and the town brings a new one at its place); they never despawn. Shopkeepers stay at
 * their counters and open their shop when spoken to; vendors, the banker, the mayor, the priest, the innkeeper and the
 * baker keep to their places and chat; townsfolk stroll the streets; guards walk near their gate and fight monsters
 * that come into town; decorators walk round changing the town's decor when the season or an event changes
 * ({@link TownDecor}). All of it is the server's: the client is only told the skin and what they are doing.
 *
 * <p>The town's residents keep a day ({@link TownRoutine}): from dawn they work at their place (a counter, a planter,
 * a crate stack, a lamp, a street, the fountain), from 6 pm they gather at a tavern's door or round the fountain, and
 * at midnight they walk to bed and sleep there till dawn. Nobody walks about at night but the watch: a resident who
 * cannot reach their bed rests where they are. Innkeepers keep the taverns open all night, selling essentials.
 */
public class Townsfolk extends PathfinderMob {
	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.STRING);
	/** What they are doing at their place ({@link TownRoutine.Activity}), for the client's poses. */
	private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.INT);
	public static final int DECORATOR_REACH = 96;
	public static final int DECORATE_TIMEOUT = 900;

	private String role = "townsfolk";
	private String shop = "";
	private BlockPos home = BlockPos.ZERO;
	private int spot = -1;
	private int lastTalk;
	private @Nullable Player listener;
	private int listeningUntil;
	private TownRoutine.@Nullable Places places;
	private @Nullable BlockPos placesOrigin;
	private int placesTick = -1;
	/** Where they rest tonight, having given up on reaching their bed (cleared at dawn). */
	private @Nullable BlockPos nightRest;

	public Townsfolk(EntityType<? extends Townsfolk> type, Level level) {
		super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) nav.setCanOpenDoors(true);
        getNavigation().setRequiredPathLength(128);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "townsfolk_1");
		builder.define(ACTIVITY, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LateBedGoal());
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new Goal() {
			{ setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
			@Override public boolean canUse() {
				return listener != null && listener.isAlive() && tickCount < listeningUntil
						&& distanceToSqr(listener) < 64 && getTarget() == null;
			}
			@Override public void start() { getNavigation().stop(); }
			@Override public void tick() { if (listener != null) getLookControl().setLookAt(listener, 30, 30); }
			@Override public void stop() { listener = null; }
		});
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			public boolean canUse() {
				return isGuard() && super.canUse();
			}
		});
		// A guard glances at Herobrine once in a while; it never targets him (creatures/herobrine/HerobrineGlanceGoal).
		goalSelector.addGoal(1, new io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineGlanceGoal(this, this::isGuard));
		goalSelector.addGoal(2, new ReturnToPlaceGoal());
        goalSelector.addGoal(1, new GateDefenseGoal());
		goalSelector.addGoal(3, new DecorateGoal());
		goalSelector.addGoal(5, new StrollNearPlaceGoal());
		goalSelector.addGoal(6, new FacePlaceGoal());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true, true) {
			@Override
			public boolean canUse() {
				return isGuard() && super.canUse();
			}
		});
	}

	/** Sets this townsperson up for a place in the town (at spawning). */
	public void setUp(TownData.Spot place, BlockPos home) {
        refresh(place, home);
        dress();
    }

    public void refresh(TownData.Spot place, BlockPos home) {
        this.role = place.role();
		this.shop = place.shop() == null ? "" : place.shop();
		this.home = home.immutable();
		this.spot = place.index();
		this.placesTick = -1;
		entityData.set(SKIN, place.skin());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(isGuard() ? 48 : 24);
		setCustomName(Component.literal(place.name()));
	}

	/** What a role carries. */
	private void dress() {
		ItemStack hand = switch (role) {
			case "guard" -> new ItemStack(Items.IRON_SWORD);
			case "decorator" -> new ItemStack(Items.LANTERN);
			case "baker" -> new ItemStack(Items.BREAD);
			case "priest", "academy_radiance" -> new ItemStack(Items.BOOK);
			case "shopkeeper" -> shop.equals("florist") ? new ItemStack(Items.POPPY) : ItemStack.EMPTY;
			default -> ItemStack.EMPTY;
		};
		setItemSlot(EquipmentSlot.MAINHAND, hand);
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		setDropChance(EquipmentSlot.HEAD, 0.0F);
	}

	public String skin() {
		return entityData.get(SKIN);
	}

	public String role() {
		return role;
	}

	public String shop() {
		return shop;
	}

	public int spot() {
		return spot;
	}

	public BlockPos home() {
		return home;
	}

	public boolean isGuard() {
		return role.equals("guard") || role.equals("flak_guard") || role.equals("mecha_guard");
	}

	/** What they are doing at their place now (synced; NONE on the way somewhere, asleep, or listening). */
	public TownRoutine.Activity activity() {
		return TownRoutine.Activity.byId(entityData.get(ACTIVITY));
	}

	/** The time of day their day keeps (the Overworld's clock). */
	public long clockTime() {
		return scheduleTime();
	}

	public TownRoutine.Period period() {
		return TownRoutine.period(scheduleTime());
	}

	/** Their job: the blueprint's (revision 4 on), or their role's for anyone without one. */
	public String job() {
		TownRoutine.Places mine = places();
		return mine != null ? mine.job() : isGuard() ? "watch" : role;
	}

	/**
	 * Their places in the world, from the town's blueprint; null for anyone not in its list (an Academy instructor, a
	 * resident of an older blueprint), who keeps to their own place as the town always did.
	 */
	protected TownRoutine.@Nullable Places places() {
		if (placesTick == tickCount) {
			return places;
		}
		placesTick = tickCount;
		if (spot < 0 || !(level() instanceof ServerLevel level)) {
			return places = null;
		}
		BlockPos origin = TownState.get(level).origin();
		TownRoutine.Places relative = TownData.get().routines.get(spot);
		if (origin == null || relative == null) {
			return places = null;
		}
		if (places == null || !origin.equals(placesOrigin)) {
			places = relative.offset(origin);
			placesOrigin = origin;
		}
		return places;
	}

	/** Where they belong now: work by day, their gathering in the evening, their bed (or where they rest) at night. */
	public BlockPos anchor() {
		TownRoutine.Places mine = places();
		if (mine == null) {
			return home;
		}
		return switch (period()) {
			case MORNING, WORK -> mine.work() != null ? mine.work() : home;
			case EVENING -> mine.evening() != null ? mine.evening() : home;
			case NIGHT -> {
				if (nightRest != null) {
					yield nightRest;
				}
				BlockPos bed = bed();
				yield bed != null ? bed : home;
			}
		};
	}

	/** How far from {@link #anchor()} they wander now. */
	public int placeRange() {
		TownRoutine.Places mine = places();
		if (mine == null) {
			return range();
		}
		TownRoutine.Period period = period();
		boolean hasPlace = period.daytime() ? mine.work() != null : period != TownRoutine.Period.EVENING || mine.evening() != null;
		return TownRoutine.reach(mine.job(), range(), period, hasPlace);
	}

	/** What they look at while at their place: their work, or the middle of their gathering. */
	public @Nullable BlockPos focus() {
		TownRoutine.Places mine = places();
		if (mine == null) {
			return null;
		}
		TownRoutine.Period period = period();
		return period.daytime() ? mine.workLook() : period == TownRoutine.Period.EVENING ? mine.eveningLook() : null;
	}

	private boolean within(BlockPos at, int reach) {
		return distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) <= (double) reach * reach;
	}

	/** How far from their place a role wanders. */
	public int range() {
		return switch (role) {
			case "guard", "mecha_guard" -> 20;
            case "flak_guard" -> 0;
			case "townsfolk" -> 70;
			case "decorator" -> DECORATOR_REACH;
			default -> 2;
		};
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Only /kill (and the void) can remove a townsperson.
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Asleep only in their own bed and only at night: one loaded asleep in daylight (or whose bed has moved) wakes.
		if (isSleeping() && (!bedtime(scheduleTime()) || !getSleepingPos().map(at -> at.equals(bed())).orElse(false))) {
			stopSleeping();
		}
		if (nightRest != null && period() != TownRoutine.Period.NIGHT) {
			nightRest = null;
		}
		if (tickCount % 10 == 0) {
			updateActivity();
		}
        if (tickCount % 100 == 0) TownDefense.crew(level, this);
		if (tickCount % 100 == 0) {
			// A townsperson the town has since replaced (after it went missing) steps aside.
			UUID recorded = spot < 0 ? null : TownState.get(level).townsperson(spot);
			if (recorded != null && !recorded.equals(getUUID())) {
				discard();
				return;
			}
			boolean halloween = TownDecor.theme().equals("halloween");
			boolean costumed = !isGuard() && (role.equals("townsfolk") || role.equals("decorator") || role.equals("vendor"));
			ItemStack head = halloween && costumed ? new ItemStack(Items.CARVED_PUMPKIN) : ItemStack.EMPTY;
			if (!ItemStack.matches(getItemBySlot(EquipmentSlot.HEAD), head)) {
				setItemSlot(EquipmentSlot.HEAD, head);
			}
		}
	}

	/** What they are doing, worked out from where they are and the time of day. */
	private void updateActivity() {
		TownRoutine.Activity next = TownRoutine.Activity.NONE;
		TownRoutine.Places mine = places();
		if (mine != null && !isSleeping() && !isPassenger() && listener == null && getTarget() == null && getNavigation().isDone()
				&& within(anchor(), placeRange() + 2)) {
			TownRoutine.Period period = period();
			// A civilian kept from their bed dozes where they rest.
			next = period == TownRoutine.Period.NIGHT && !isGuard() && !mine.job().equals("innkeeper") ? TownRoutine.Activity.DOZE
					: TownRoutine.activity(mine.job(), mine.gathering(), period);
		}
		if (next.ordinal() != entityData.get(ACTIVITY)) {
			entityData.set(ACTIVITY, next.ordinal());
		}
	}

	/**
	 * A player speaks to this townsperson (every interaction comes here: no leads, name tags or other items work on
	 * them). Shopkeepers open their shop in its hours and say when it opens otherwise; innkeepers open the essentials
	 * counter at any hour; everyone else says something for the season or about their day.
	 */
	public InteractionResult talkTo(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.SUCCESS;
		}
		getLookControl().setLookAt(player);
        if (isSleeping()) {
            player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.asleep", getDisplayName()));
            return InteractionResult.SUCCESS;
        }
        listener = player;
		listeningUntil = tickCount + 80;
        if ((player.isShiftKeyDown() || role.equals("mayor")) && !TownMentors.lesson(this).isEmpty()) {
            TownMentors.consult(server, this);
            return InteractionResult.SUCCESS;
        }
		if (role.equals("academy_radiance")) {
			io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.consult(server);
			return InteractionResult.SUCCESS;
		}
		TownShops.Shop own = role.equals("shopkeeper") ? TownShops.get().shop(shop) : null;
		if (own != null) {
			if (own.openAt(clockTime())) {
				ShopMenu.open(server, this);
				server.sendOverlayMessage(Component.translatable("message.jugcraft.shop.hours", Component.translatable("shop.jugcraft." + own.id()),
						TownRoutine.clock(own.opens()), TownRoutine.clock(own.closes())));
			} else if (tickCount - lastTalk >= 20) {
				lastTalk = tickCount;
				player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.says", getDisplayName(),
						Component.translatable("message.jugcraft.shop.closed", Component.translatable("shop.jugcraft." + own.id()),
								TownRoutine.clock(own.opens()), TownRoutine.clock(own.closes()))));
			}
			return InteractionResult.SUCCESS;
		}
		if (role.equals("innkeeper") && TownShops.get().essentialsShop() != null) {
			// The tavern never shuts: a word for the hour, and the essentials counter.
			String greeting = TownRoutine.line("innkeeper", period(), Math.floorMod(getUUID().hashCode() + tickCount / 200, 3));
			if (greeting != null && tickCount - lastTalk >= 20) {
				lastTalk = tickCount;
				player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.says", getDisplayName(), Component.translatable(greeting)));
			}
			ShopMenu.openEssentials(server, this);
			return InteractionResult.SUCCESS;
		}
		if (tickCount - lastTalk < 20) {
			return InteractionResult.SUCCESS;
		}
		lastTalk = tickCount;
		String theme = TownDecor.theme();
		// Half the time something for the season, half the time something about their day.
		int line = Math.floorMod(getUUID().hashCode() + tickCount / 200, 6);
		String daily = line >= 3 ? TownRoutine.line(job(), period(), line - 3) : null;
		Component said = role.equals("banker")
				? Component.translatable("message.jugcraft.townsfolk.banker", Jugs.balance(server.level().getServer(), server.getUUID()))
				: daily != null ? Component.translatable(daily)
				: Component.translatable("message.jugcraft.townsfolk." + roleKey() + "." + theme + "." + line % 3);
		player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.says", getDisplayName(), said));
		if (isGuard()) player.sendSystemMessage(Component.translatable("message.jugcraft.town.map_hint"));
		level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/** Which set of lines a role speaks: guards and decorators have their own, everyone else the town's. */
	private String roleKey() {
        if (isGuard()) return "guard";
		return switch (role) {
			case "guard", "decorator", "priest", "mayor" -> role;
			default -> "townsfolk";
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("role", role);
		output.putString("shop", shop);
		output.putString("skin", skin());
		output.store("home", BlockPos.CODEC, home);
		output.putInt("spot", spot);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		role = input.getStringOr("role", "townsfolk");
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(isGuard() ? 48 : 24);
		shop = input.getStringOr("shop", "");
		entityData.set(SKIN, input.getStringOr("skin", "townsfolk_1"));
		home = input.read("home", BlockPos.CODEC).orElse(blockPosition());
		spot = input.getIntOr("spot", -1);
	}

    protected long scheduleTime() { return level().getOverworldClockTime(); }

    public static boolean bedtime(long dayTime) { return Math.floorMod(dayTime, 24000L) >= 18000; }

    /** Their bed; none for the watch (on duty all night) or the innkeepers (who keep the taverns open). */
    public @Nullable BlockPos bed() {
        if (isGuard() || role.equals("innkeeper") || spot < 0 || !(level() instanceof ServerLevel level)) return null;
        BlockPos origin = TownState.get(level).origin(), relative = TownData.get().beds.get(spot);
        return origin == null || relative == null ? null : origin.offset(relative);
    }

    /** Sleeping is a physical rest state, not merely a render pose. */
    @Override public void travel(net.minecraft.world.phys.Vec3 input) {
        if (isSleeping()) {
            getNavigation().stop();
            setSpeed(0); xxa = 0; yya = 0; zza = 0;
            setJumping(false);
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    /**
     * Civilian bedtime is midnight, not sunset. Guards stay on duty. No teleporting or chunk tickets. A resident who
     * gets no nearer their bed for 20 seconds (a blocked door, a bed walled in) stops trying and rests where they are
     * until dawn, rather than wandering about in the dark.
     */
    private class LateBedGoal extends Goal {
        private static final int GIVE_UP_AFTER = 4;
        private @Nullable BlockPos bed;
        private int retryAt;
        private double nearest;
        private int stalls;
        LateBedGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() {
            if (!bedtime(scheduleTime()) || isGuard() || getTarget() != null || nightRest != null) return false;
            bed = Townsfolk.this.bed();
            return bed != null && validBed();
        }
        private boolean validBed() {
            if (bed == null || !(level() instanceof ServerLevel level) || level.getChunkSource().getChunkNow(bed.getX()>>4,bed.getZ()>>4) == null) return false;
            var block = level.getBlockState(bed);
            if (!(block.getBlock() instanceof net.minecraft.world.level.block.BedBlock)
                || block.getValue(net.minecraft.world.level.block.BedBlock.PART) != net.minecraft.world.level.block.state.properties.BedPart.HEAD) return false;
            BlockPos foot = bed.relative(block.getValue(net.minecraft.world.level.block.BedBlock.FACING).getOpposite());
            if (level.getChunkSource().getChunkNow(foot.getX()>>4,foot.getZ()>>4) == null) return false;
            var other = level.getBlockState(foot);
            return other.is(block.getBlock()) && other.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT
                && (isSleeping() || !block.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED));
        }
        @Override public boolean canContinueToUse() {
            return bedtime(scheduleTime()) && !isGuard() && getTarget() == null && nightRest == null && validBed();
        }
        @Override public void start() { listener = null; retryAt = 0; nearest = Double.MAX_VALUE; stalls = 0; getNavigation().stop(); }
        @Override public void tick() {
            if (bed == null || isSleeping()) return;
            if (distanceToSqr(bed.getX()+.5,bed.getY(),bed.getZ()+.5) < 4 && Math.abs(getY()-bed.getY()) < 1.5) {
                getNavigation().stop(); startSleeping(bed);
            } else if (tickCount >= retryAt) {
                double distance = Math.sqrt(distanceToSqr(bed.getX()+.5,bed.getY(),bed.getZ()+.5));
                if (distance < nearest - 1) { nearest = distance; stalls = 0; }
                else if (++stalls >= GIVE_UP_AFTER) { nightRest = blockPosition(); getNavigation().stop(); return; }
                // Long cross-town trips are split into navigable stages by vanilla's pathfinder.
                getNavigation().moveTo(bed.getX()+.5,bed.getY(),bed.getZ()+.5, .85);
                retryAt = tickCount + 100;
            }
        }
        @Override public void stop() { if (isSleeping()) stopSleeping(); getNavigation().stop(); bed = null; }
    }

    // ---------------------------------------------------------------- goals

    /** Meet active ground raids at a gate instead of staring through a wall corner. */
    private class GateDefenseGoal extends Goal {
        private BlockPos gate;
        private int checkAt;
        GateDefenseGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (!isGuard() || role.equals("flak_guard") || isPassenger() || getTarget() != null
                    || tickCount < checkAt || !(level() instanceof ServerLevel level)) return false;
            checkAt = tickCount + 40;
            BlockPos origin = Town.origin(level);
            if (origin == null) return false;
            net.minecraft.world.entity.Entity nearest = null;
            double best = 128 * 128;
            for (var raid : io.github.jimbozoomer.jugcraft.raiders.RaiderRaids.raids(level)) {
                if (!raid.objective().equals(Town.centre(origin))) continue;
                for (var id : raid.members()) {
                    var enemy = level.getEntity(id); // Only already loaded entities.
                    if (enemy == null || !enemy.isAlive() || enemy instanceof io.github.jimbozoomer.jugcraft.raiders.RaiderBlimp) continue;
                    double distance = distanceToSqr(enemy);
                    if (distance < best) { best = distance; nearest = enemy; }
                }
            }
            if (nearest == null) return false;
            gate = io.github.jimbozoomer.jugcraft.raiders.CityRaidRoute.nearest(origin, nearest.position()).inside();
            return level.isLoaded(gate);
        }
        @Override public boolean canContinueToUse() { return getTarget() == null && tickCount < checkAt; }
        @Override public void start() { if (!within(gate, 3)) getNavigation().moveTo(gate.getX()+.5, gate.getY(), gate.getZ()+.5, 1.1); else getNavigation().stop(); }
        @Override public void stop() { getNavigation().stop(); }
    }

	/**
	 * To where they belong now ({@link #anchor()}): to work in the morning, to their gathering at 6 pm, to rest at night
	 * if their bed is out of reach, and back after a chase or a push. Someone not in the town's list comes back to their
	 * own place when they stray, as the town always did.
	 */
	private class ReturnToPlaceGoal extends Goal {
		private @Nullable BlockPos target;
		private int retryAt;

		ReturnToPlaceGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		private int slack() {
			return places() == null ? 4 : 3;
		}

		@Override
		public boolean canUse() {
			if (isSleeping() || isPassenger() || getTarget() != null || tickCount < retryAt) {
				return false;
			}
			return !within(anchor(), placeRange() + slack());
		}

		@Override
		public void start() {
			BlockPos to = anchor();
			target = to;
			double speed = places() == null ? 1.0 : period() == TownRoutine.Period.NIGHT ? 0.85 : within(to, 48) ? 0.8 : 1.0;
			getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, speed);
		}

		@Override
		public boolean canContinueToUse() {
			return !isSleeping() && !isPassenger() && !getNavigation().isDone() && anchor().equals(target);
		}

		@Override
		public void stop() {
			// A long way still to go when the path ran out: try again shortly rather than every tick.
			if (target != null && !within(target, placeRange() + slack())) {
				retryAt = tickCount + 40;
			}
			target = null;
		}
	}

	/** A short walk to somewhere near their place, now and then; never at night, except for the watch. */
	private class StrollNearPlaceGoal extends Goal {
		StrollNearPlaceGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isSleeping() || isPassenger()
					|| places() != null && !isGuard() && period() == TownRoutine.Period.NIGHT) {
				return false;
			}
			int r = placeRange();
			if (getRandom().nextInt(r <= 2 ? 200 : 80) != 0) {
				return false;
			}
			BlockPos at = anchor();
			for (int tries = 0; tries < 6; tries++) {
				BlockPos to = at.offset(getRandom().nextInt(2 * r + 1) - r, 0, getRandom().nextInt(2 * r + 1) - r);
				if (getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.6)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean canContinueToUse() {
			return !isSleeping() && !isPassenger() && !getNavigation().isDone();
		}
	}

	/** At their place, they turn to their work (a planter, a lamp, the crowd) or to the middle of their gathering. */
	private class FacePlaceGoal extends Goal {
		private int until;

		FacePlaceGoal() {
			setFlags(EnumSet.of(Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return activity() != TownRoutine.Activity.NONE && focus() != null && getRandom().nextInt(20) == 0;
		}

		@Override
		public void start() {
			until = tickCount + 60 + getRandom().nextInt(80);
		}

		@Override
		public boolean canContinueToUse() {
			return tickCount < until && activity() != TownRoutine.Activity.NONE && focus() != null;
		}

		@Override
		public void tick() {
			BlockPos at = focus();
			if (at != null) {
				getLookControl().setLookAt(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
			}
		}
	}

	/** A decorator walks to the nearest site still showing the old theme and changes it. */
	private class DecorateGoal extends Goal {
		private int site = -1;
		private int ticks;

		DecorateGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (isSleeping() || isPassenger() || !role.equals("decorator") || !(level() instanceof ServerLevel level) || tickCount % 40 != 0) {
				return false;
			}
			// Decorating is day work: in the evening they join their gathering.
			if (places() != null && !period().daytime()) {
				return false;
			}
			site = TownDecor.claim(level, blockPosition(), getUUID(), DECORATOR_REACH);
			return site >= 0;
		}

		@Override
		public void start() {
			ticks = 0;
			walk();
		}

		private void walk() {
			if (level() instanceof ServerLevel level) {
				BlockPos to = TownDecor.where(level, site);
				if (to != null) {
					getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.8);
				}
			}
		}

		@Override
		public boolean canContinueToUse() {
			return !isSleeping() && !isPassenger() && site >= 0 && level() instanceof ServerLevel level && TownDecor.stale(level, site);
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level) || site < 0) {
				return;
			}
			ticks++;
			BlockPos to = TownDecor.where(level, site);
			if (to == null) {
				site = -1;
				return;
			}
			getLookControl().setLookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
			double dx = to.getX() + 0.5 - getX();
			double dz = to.getZ() + 0.5 - getZ();
			if (dx * dx + dz * dz < 12.0 || ticks > DECORATE_TIMEOUT) {
				TownDecor.apply(level, site);
				level.playSound(null, to, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.8F, 1.0F);
				site = -1;
			} else if (getNavigation().isDone() && ticks % 40 == 0) {
				walk();
			}
		}

		@Override
		public void stop() {
			if (site >= 0) {
				TownDecor.release(site);
			}
			site = -1;
			getNavigation().stop();
		}
	}
}
