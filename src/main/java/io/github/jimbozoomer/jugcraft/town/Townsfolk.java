package io.github.jimbozoomer.jugcraft.town;

import java.util.EnumSet;
import java.util.UUID;
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
 * ({@link TownDecor}). All of it is the server's: the client is only told the skin.
 */
public class Townsfolk extends PathfinderMob {
	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.STRING);
	public static final int DECORATOR_REACH = 96;
	public static final int DECORATE_TIMEOUT = 900;

	private String role = "townsfolk";
	private String shop = "";
	private BlockPos home = BlockPos.ZERO;
	private int spot = -1;
	private int lastTalk;

	public Townsfolk(EntityType<? extends Townsfolk> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "townsfolk_1");
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			public boolean canUse() {
				return isGuard() && super.canUse();
			}
		});
		goalSelector.addGoal(2, new ReturnHomeGoal());
		goalSelector.addGoal(3, new DecorateGoal());
		goalSelector.addGoal(5, new StrollNearHomeGoal());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true) {
			@Override
			public boolean canUse() {
				return isGuard() && super.canUse();
			}
		});
	}

	/** Sets this townsperson up for a place in the town (at spawning). */
	public void setUp(TownData.Spot place, BlockPos home) {
		this.role = place.role();
		this.shop = place.shop() == null ? "" : place.shop();
		this.home = home.immutable();
		this.spot = place.index();
		entityData.set(SKIN, place.skin());
		setCustomName(Component.literal(place.name()));
		dress();
	}

	/** What a role carries. */
	private void dress() {
		ItemStack hand = switch (role) {
			case "guard" -> new ItemStack(Items.IRON_SWORD);
			case "decorator" -> new ItemStack(Items.LANTERN);
			case "baker" -> new ItemStack(Items.BREAD);
			case "priest" -> new ItemStack(Items.BOOK);
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
		return role.equals("guard");
	}

	/** How far from their place a role wanders. */
	public int range() {
		return switch (role) {
			case "guard" -> 20;
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

	/**
	 * A player speaks to this townsperson (every interaction comes here: no leads, name tags or other items work on
	 * them). Shopkeepers open their shop; everyone else says something for the season.
	 */
	public InteractionResult talkTo(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.SUCCESS;
		}
		getLookControl().setLookAt(player);
		if (role.equals("shopkeeper") && TownShops.get().shop(shop) != null) {
			ShopMenu.open(server, this);
			return InteractionResult.SUCCESS;
		}
		if (tickCount - lastTalk < 20) {
			return InteractionResult.SUCCESS;
		}
		lastTalk = tickCount;
		String theme = TownDecor.theme();
		int line = Math.floorMod(getUUID().hashCode() + tickCount / 200, 3);
		Component said = role.equals("banker")
				? Component.translatable("message.jugcraft.townsfolk.banker", Jugs.balance(server.level().getServer(), server.getUUID()))
				: Component.translatable("message.jugcraft.townsfolk." + roleKey() + "." + theme + "." + line);
		player.sendSystemMessage(Component.translatable("message.jugcraft.townsfolk.says", getDisplayName(), said));
		level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/** Which set of lines a role speaks: guards and decorators have their own, everyone else the town's. */
	private String roleKey() {
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
		shop = input.getStringOr("shop", "");
		entityData.set(SKIN, input.getStringOr("skin", "townsfolk_1"));
		home = input.read("home", BlockPos.CODEC).orElse(blockPosition());
		spot = input.getIntOr("spot", -1);
	}

	// ---------------------------------------------------------------- goals

	/** Back towards their place when they have strayed (after a chase, or pushed). */
	private class ReturnHomeGoal extends Goal {
		ReturnHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			int r = range() + 4;
			return getTarget() == null && distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > (double) r * r;
		}

		@Override
		public void start() {
			getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone();
		}
	}

	/** A short walk to somewhere near their place, now and then. */
	private class StrollNearHomeGoal extends Goal {
		StrollNearHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (getRandom().nextInt(range() <= 2 ? 200 : 80) != 0) {
				return false;
			}
			for (int tries = 0; tries < 6; tries++) {
				int r = range();
				BlockPos to = home.offset(getRandom().nextInt(2 * r + 1) - r, 0, getRandom().nextInt(2 * r + 1) - r);
				if (getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.6)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone();
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
			if (!role.equals("decorator") || !(level() instanceof ServerLevel level) || tickCount % 40 != 0) {
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
			return site >= 0 && level() instanceof ServerLevel level && TownDecor.stale(level, site);
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
