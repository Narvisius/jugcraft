package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A werewolf (fall addition 23): a hulking wolf-man that comes out of the woods only on full-moon nights
 * ({@link Werewolves}) and turns back into nobody at all when the night ends, vanishing in a swirl of smoke.
 *
 * <p>Three kinds ({@link Kind}), three tiers of danger, each with its own pelt:
 * <ul>
 * <li><b>Brown</b> (tier I), the pack hunter of any wood: it raids livestock ({@code jugcraft:werewolf_prey}) as well as
 * people; when it finds prey its howl calls the brown werewolves within {@value #PACK_REACH} blocks to the hunt; and
 * below {@value #FLEE_BELOW} of its health it flees, until it has healed to {@value #FLEE_UNTIL}.
 * <li><b>Snow</b> (tier II), the stalker of snowy woods: its bite brings frostbite (Slowness II for
 * {@value #FROSTBITE_TICKS} ticks, and {@value #FROSTBITE_CHILL} ticks of freezing on anyone who can freeze, so leather
 * keeps the chill out); it never freezes itself and on snow runs {@value #SNOW_STRIDE} faster.
 * <li><b>Shadow</b> (tier III), the rare alpha: it steps out of the shadows behind prey {@value #SHADOW_STEP_MIN} or
 * more blocks off, every {@value #SHADOW_STEP_TICKS} ticks; its howl, every {@value #HOWL_COOLDOWN} ticks on the
 * hunt, darkens the night for players within {@value #ALPHA_REACH} blocks and drives werewolves within
 * {@value #FRENZY_REACH} into a frenzy (Strength and Speed, and its prey for theirs); and a sprig of wolfsbane in hand
 * doesn't ward it off, only planted or potted wolfsbane does. Slaying one earns Leader of the Pack.
 * </ul>
 *
 * <p>Every kind's hide shrugs off ordinary blows: everything but silver does {@value #HIDE_FACTOR} of its damage, and
 * it heals a point every {@value #REGEN_TICKS} ticks, unless silver has wounded it in the last
 * {@value #SILVER_WOUND_TICKS}. Silver (a weapon in {@code jugcraft:silver_weapons}, or a silver arrow) does
 * {@value #SILVER_FACTOR} times its damage. Slaying one with silver earns Silver Lining.
 *
 * <p>Wolfsbane wards it off ({@link Werewolves#warded}): whoever it hunts is dropped and left alone for a while. It
 * hunts players and villagers, leaps at them, and howls at the moon when it has nobody to hunt. Its kind is saved, and
 * a werewolf saved before there were kinds is brown.
 */
public class Werewolf extends Monster {
	public static final float HIDE_FACTOR = 0.5F;
	public static final float SILVER_FACTOR = 2.5F;
	public static final int REGEN_TICKS = 40;
	public static final int SILVER_WOUND_TICKS = 100;
	public static final int WARD_CHECK_TICKS = 10;
	/** How long a warded target is left alone, in ticks. */
	public static final int SHUN_TICKS = 200;
	public static final int HOWL_MIN = 300;
	public static final int HOWL_MAX = 700;
	/** The brown werewolf's pack call, flight and prey. */
	public static final int PACK_REACH = 32;
	public static final float FLEE_BELOW = 0.25F;
	public static final float FLEE_UNTIL = 0.5F;
	public static final int PREY_REACH = 16;
	/** At most one pack call this often, in ticks. */
	public static final int PACK_CALL_TICKS = 100;
	public static final TagKey<EntityType<?>> PREY = TagKey.create(Registries.ENTITY_TYPE, Jugcraft.id("werewolf_prey"));
	/** The snow werewolf's frostbite and stride. */
	public static final int FROSTBITE_TICKS = 60;
	public static final int FROSTBITE_CHILL = 80;
	public static final float SNOW_STRIDE = 0.25F;
	private static final Identifier SNOW_STRIDE_ID = Jugcraft.id("snow_stride");
	/** The shadow werewolf's step and howl. */
	public static final int SHADOW_STEP_MIN = 6;
	public static final int SHADOW_STEP_TICKS = 200;
	public static final int ALPHA_REACH = 16;
	public static final int DARKNESS_TICKS = 160;
	public static final int FRENZY_REACH = 24;
	public static final int FRENZY_TICKS = 200;
	public static final int HOWL_COOLDOWN = 600;
	public static final TagKey<Item> SILVER_WEAPONS = TagKey.create(Registries.ITEM, Jugcraft.id("silver_weapons"));
	/** Silver for a blade: soft (iron's drops, less durable than iron), quick, and enchanted easily. */
	public static final ToolMaterial SILVER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 200, 6.0F, 1.5F, 20,
			TagKey.create(Registries.ITEM, Jugcraft.id("repairs_silver_gear")));
	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(Werewolf.class, EntityDataSerializers.INT);

	/** The three kinds of werewolf and their tiers: health, damage, armour, speed, size, knockback resistance and experience. */
	public enum Kind {
		BROWN("brown", 1, 40.0, 6.0, 2.0, 0.33, 1.0, 0.3, 10, "werewolf_pelt", "werewolf_rug"),
		SNOW("snow", 2, 50.0, 8.0, 4.0, 0.32, 1.05, 0.4, 20, "snow_werewolf_pelt", "snow_werewolf_rug"),
		SHADOW("shadow", 3, 80.0, 11.0, 6.0, 0.36, 1.15, 0.6, 40, "shadow_werewolf_pelt", "shadow_werewolf_rug");

		public final String id;
		public final int tier;
		public final double health;
		public final double damage;
		public final double armor;
		public final double speed;
		public final double scale;
		public final double knockback;
		public final int xp;
		public final String pelt;
		public final String rug;

		Kind(String id, int tier, double health, double damage, double armor, double speed, double scale, double knockback, int xp,
				String pelt, String rug) {
			this.id = id;
			this.tier = tier;
			this.health = health;
			this.damage = damage;
			this.armor = armor;
			this.speed = speed;
			this.scale = scale;
			this.knockback = knockback;
			this.xp = xp;
			this.pelt = pelt;
			this.rug = rug;
		}

		/** The kind called {@code id}, or brown. */
		public static Kind byId(String id) {
			for (Kind kind : values()) {
				if (kind.id.equals(id)) {
					return kind;
				}
			}
			return BROWN;
		}

		/** Its pelt's loot table, {@code jugcraft:entities/werewolf/<kind>}. */
		public ResourceKey<LootTable> peltTable() {
			return ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/werewolf/" + id));
		}
	}

	private int lastSilverHit = -SILVER_WOUND_TICKS;
	private int nextHowl = HOWL_MIN;
	private @Nullable UUID shunned;
	private int shunnedUntil;
	/** Whom it last howled about (the pack call and the alpha's howl go out once a hunt). */
	private @Nullable UUID howledFor;
	private int nextAlphaHowl;
	private int nextPackCall;
	private int nextShadowStep = SHADOW_STEP_TICKS;
	private boolean fleeing;
	private boolean peltDropped;

	public Werewolf(EntityType<? extends Werewolf> type, Level level) {
		super(type, level);
		xpReward = Kind.BROWN.xp;
	}

	/** A brown werewolf's attributes; {@link #setKind} sets another kind's. */
	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Kind.BROWN.health).add(Attributes.MOVEMENT_SPEED, Kind.BROWN.speed)
				.add(Attributes.ATTACK_DAMAGE, Kind.BROWN.damage).add(Attributes.ARMOR, Kind.BROWN.armor).add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, Kind.BROWN.knockback).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(KIND, Kind.BROWN.ordinal());
	}

	public Kind kind() {
		int index = entityData.get(KIND);
		return index >= 0 && index < Kind.values().length ? Kind.values()[index] : Kind.BROWN;
	}

	/** Makes it {@code kind}: its attributes, size and experience (its health is left as it is; see {@link #heal}). */
	public void setKind(Kind kind) {
		entityData.set(KIND, kind.ordinal());
		base(Attributes.MAX_HEALTH, kind.health);
		base(Attributes.ATTACK_DAMAGE, kind.damage);
		base(Attributes.ARMOR, kind.armor);
		base(Attributes.MOVEMENT_SPEED, kind.speed);
		base(Attributes.SCALE, kind.scale);
		base(Attributes.KNOCKBACK_RESISTANCE, kind.knockback);
		xpReward = kind.xp;
	}

	private void base(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	@Override
	public Component getTypeName() {
		return Component.translatable("entity." + Jugcraft.MOD_ID + ".werewolf." + kind().id);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("kind", kind().id);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		// The kind first, so a werewolf summoned with only a kind gets that kind's full health.
		Kind kind = Kind.byId(input.getStringOr("kind", Kind.BROWN.id));
		setKind(kind);
		super.readAdditionalSaveData(input);
		setKind(kind);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new FleeGoal());
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45F));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
	}

	/** Whether {@code source} is silver: a silver arrow, or a blow from someone holding a silver weapon. */
	public static boolean silver(DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow arrow) {
			return arrow.getPickupItemStackOrigin().is(JugcraftAgriculture.item(Werewolves.SILVER_ARROW));
		}
		return direct instanceof LivingEntity attacker && attacker.getMainHandItem().is(SILVER_WEAPONS);
	}

	/** The damage a werewolf takes from {@code amount} of {@code source}: more from silver, less from anything else. */
	public static float damageFrom(DamageSource source, float amount) {
		if (silver(source)) {
			return amount * SILVER_FACTOR;
		}
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? amount : amount * HIDE_FACTOR;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (silver(source)) {
			lastSilverHit = tickCount;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(1.0), getZ(), 6, 0.3, 0.4, 0.3, 0.05);
		}
		return super.hurtServer(level, source, damageFrom(source, amount));
	}

	/** Whether silver has wounded it lately, so it can't heal. */
	public boolean silverWounded() {
		return tickCount - lastSilverHit < SILVER_WOUND_TICKS;
	}

	@Override
	public void die(DamageSource source) {
		if (!peltDropped && !isRemoved() && level() instanceof ServerLevel level) {
			peltDropped = true;
			if (source.getEntity() instanceof ServerPlayer slayer) {
				if (silver(source)) {
					TrickOrTreat.award(slayer, "silver_lining");
				}
				if (kind() == Kind.SHADOW) {
					TrickOrTreat.award(slayer, "leader_of_the_pack");
				}
			}
			if (level.getGameRules().get(GameRules.MOB_DROPS)) {
				dropPelt(level, source);
			}
		}
		super.die(source);
	}

	/** Rolls its kind's pelt table ({@link Kind#peltTable}) and drops what comes where it fell. */
	public List<ItemStack> dropPelt(ServerLevel level, DamageSource source) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(kind().peltTable());
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, this)
				.withParameter(LootContextParams.ORIGIN, position()).withParameter(LootContextParams.DAMAGE_SOURCE, source)
				.create(LootContextParamSets.ENTITY);
		List<ItemStack> pelts = List.copyOf(table.getRandomItems(params));
		pelts.forEach(stack -> spawnAtLocation(level, stack.copy()));
		return pelts;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (fleeing || shuns(target)) {
			return false;
		}
		return super.canAttack(target);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && kind() == Kind.SNOW && target instanceof LivingEntity bitten) {
			frostbite(bitten);
		}
		return hit;
	}

	/** The snow werewolf's bite: Slowness II, and a chill that freezes anyone who can freeze (not in leather). */
	public void frostbite(LivingEntity bitten) {
		bitten.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FROSTBITE_TICKS, 1), this);
		if (bitten.canFreeze()) {
			bitten.setTicksFrozen(Math.min(bitten.getTicksRequiredToFreeze(), bitten.getTicksFrozen() + FROSTBITE_CHILL));
		}
	}

	@Override
	public boolean canFreeze() {
		return kind() != Kind.SNOW && super.canFreeze();
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		if (tickCount % 20 == 0 && !Werewolves.fullMoon(level)) {
			turnBack(level);
			return;
		}
		if (tickCount % REGEN_TICKS == 0 && isAlive() && getHealth() < getMaxHealth() && !silverWounded()) {
			heal(1.0F);
		}
		if (tickCount % WARD_CHECK_TICKS == 0) {
			checkWard(level);
		}
		switch (kind()) {
			case BROWN -> brownTick(level);
			case SNOW -> stride(onSnow());
			case SHADOW -> shadowTick(level);
		}
		LivingEntity target = getTarget();
		if (target == null && --nextHowl <= 0) {
			howl(level);
			nextHowl = HOWL_MIN + random.nextInt(HOWL_MAX - HOWL_MIN + 1);
		}
	}

	/** The brown werewolf: it flees when badly hurt, calls the pack to new prey and goes after livestock. */
	private void brownTick(ServerLevel level) {
		checkFlight();
		LivingEntity target = getTarget();
		if (target != null && !target.getUUID().equals(howledFor) && tickCount >= nextPackCall) {
			callPack(level, target);
		} else if (target == null && !fleeing && tickCount % 20 == 0) {
			huntPrey(level);
		}
	}

	/** Below {@value #FLEE_BELOW} of its health it drops its target and flees; healed to {@value #FLEE_UNTIL}, it stops. */
	public boolean checkFlight() {
		if (kind() != Kind.BROWN) {
			return false;
		}
		if (!fleeing && getHealth() < getMaxHealth() * FLEE_BELOW) {
			fleeing = true;
			setTarget(null);
			level().playSound(null, getX(), getY(), getZ(), sound("entity.wolf.whine", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 1.0F, 0.6F);
		} else if (fleeing && getHealth() >= getMaxHealth() * FLEE_UNTIL) {
			fleeing = false;
		}
		return fleeing;
	}

	public boolean fleeing() {
		return fleeing;
	}

	/**
	 * A brown werewolf goes after the nearest livestock ({@code jugcraft:werewolf_prey}) it can see within
	 * {@value #PREY_REACH} blocks; returns it, or null.
	 */
	public @Nullable LivingEntity huntPrey(ServerLevel level) {
		if (kind() != Kind.BROWN) {
			return null;
		}
		LivingEntity nearest = null;
		for (LivingEntity prey : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREY_REACH),
				prey -> prey.isAlive() && isPrey(prey.getType()) && canAttack(prey) && hasLineOfSight(prey))) {
			if (nearest == null || distanceToSqr(prey) < distanceToSqr(nearest)) {
				nearest = prey;
			}
		}
		if (nearest != null) {
			setTarget(nearest);
		}
		return nearest;
	}

	/** Whether a brown werewolf hunts {@code type} ({@code jugcraft:werewolf_prey}). */
	public static boolean isPrey(EntityType<?> type) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(type));
		return BuiltInRegistries.ENTITY_TYPE.getOrThrow(key).is(PREY);
	}

	/**
	 * Howls over {@code prey}, calling the brown werewolves within {@value #PACK_REACH} blocks that aren't hunting to
	 * hunt it too. Returns how many came.
	 */
	public int callPack(ServerLevel level, LivingEntity prey) {
		howledFor = prey.getUUID();
		nextPackCall = tickCount + PACK_CALL_TICKS;
		howl(level);
		int called = 0;
		for (Werewolf other : level.getEntitiesOfClass(Werewolf.class, getBoundingBox().inflate(PACK_REACH),
				other -> other != this && other.kind() == Kind.BROWN && other.getTarget() == null && other.canAttack(prey))) {
			other.howledFor = prey.getUUID();
			other.setTarget(prey);
			called++;
		}
		return called;
	}

	/** Whether it stands on snow: a snow layer at its feet, or snow, a snow block or powder snow under them. */
	public boolean onSnow() {
		BlockState feet = level().getBlockState(blockPosition());
		BlockState under = level().getBlockState(blockPosition().below());
		return feet.getBlock() instanceof SnowLayerBlock || feet.is(Blocks.POWDER_SNOW) || under.getBlock() instanceof SnowLayerBlock
				|| under.is(Blocks.SNOW_BLOCK) || under.is(Blocks.POWDER_SNOW);
	}

	/** The snow werewolf's stride: {@value #SNOW_STRIDE} faster on snow. */
	public void stride(boolean snow) {
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		boolean striding = speed.hasModifier(SNOW_STRIDE_ID);
		if (snow && kind() == Kind.SNOW && !striding) {
			speed.addTransientModifier(new AttributeModifier(SNOW_STRIDE_ID, SNOW_STRIDE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		} else if ((!snow || kind() != Kind.SNOW) && striding) {
			speed.removeModifier(SNOW_STRIDE_ID);
		}
	}

	/** The shadow werewolf: on the hunt it howls (now and then) and steps out of the shadows behind its prey. */
	private void shadowTick(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target == null) {
			return;
		}
		if (tickCount >= nextAlphaHowl) {
			alphaHowl(level);
		}
		if (tickCount >= nextShadowStep && shadowStep(level)) {
			nextShadowStep = tickCount + SHADOW_STEP_TICKS;
		}
	}

	/**
	 * The alpha's howl: Darkness for the players within {@value #ALPHA_REACH} blocks in survival or adventure (as the
	 * warden gives it); Strength and Speed for the werewolves within {@value #FRENZY_REACH}, and its prey for any not
	 * hunting. Returns how many werewolves it roused.
	 */
	public int alphaHowl(ServerLevel level) {
		LivingEntity target = getTarget();
		howledFor = target == null ? null : target.getUUID();
		nextAlphaHowl = tickCount + HOWL_COOLDOWN;
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 6.0F, 0.4F);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(ALPHA_REACH))) {
			if (player.gameMode.isSurvival() && player.distanceToSqr(this) <= ALPHA_REACH * ALPHA_REACH) {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0), this);
			}
		}
		int roused = 0;
		for (Werewolf other : level.getEntitiesOfClass(Werewolf.class, getBoundingBox().inflate(FRENZY_REACH), other -> other != this)) {
			other.addEffect(new MobEffectInstance(MobEffects.STRENGTH, FRENZY_TICKS, 0), this);
			other.addEffect(new MobEffectInstance(MobEffects.SPEED, FRENZY_TICKS, 0), this);
			if (target != null && other.getTarget() == null && other.canAttack(target)) {
				other.howledFor = target.getUUID();
				other.setTarget(target);
			}
			roused++;
		}
		return roused;
	}

	/**
	 * The shadow werewolf's step: if its prey is {@value #SHADOW_STEP_MIN} or more blocks off (and within its follow range), it
	 * vanishes in smoke and steps out behind them, onto solid ground with room to stand. Returns whether it stepped.
	 */
	public boolean shadowStep(ServerLevel level) {
		LivingEntity target = getTarget();
		if (kind() != Kind.SHADOW || target == null || target.level() != level) {
			return false;
		}
		double distance = distanceTo(target);
		if (distance < SHADOW_STEP_MIN || distance > getAttributeValue(Attributes.FOLLOW_RANGE)) {
			return false;
		}
		Vec3 facing = target.getLookAngle().multiply(1.0, 0.0, 1.0);
		if (facing.lengthSqr() < 1.0E-4) {
			facing = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
		}
		facing = facing.normalize();
		for (Vec3 spot : behind(target.position(), facing)) {
			AABB box = getBoundingBox().move(spot.subtract(position()));
			BlockPos under = BlockPos.containing(spot).below();
			if (level.noCollision(this, box) && level.getBlockState(under).isFaceSturdy(level, under, Direction.UP)) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 24, 0.4, 0.8, 0.4, 0.02);
				teleportTo(spot.x, spot.y, spot.z);
				getNavigation().stop();
				getLookControl().setLookAt(target);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.x, spot.y + 1.0, spot.z, 24, 0.4, 0.8, 0.4, 0.02);
				level.playSound(null, spot.x, spot.y, spot.z, sound("entity.enderman.teleport", SoundEvents.FIRE_EXTINGUISH), SoundSource.HOSTILE,
						1.0F, 0.5F);
				return true;
			}
		}
		return false;
	}

	/** Spots behind someone at {@code at} facing {@code facing}: two, three or one and a half blocks back, level or a step up or down. */
	private static List<Vec3> behind(Vec3 at, Vec3 facing) {
		List<Vec3> spots = new ArrayList<>();
		for (double back : new double[] {2.0, 3.0, 1.5}) {
			for (int rise : new int[] {0, 1, -1}) {
				Vec3 spot = at.subtract(facing.scale(back));
				spots.add(new Vec3(spot.x, Math.floor(at.y) + rise, spot.z));
			}
		}
		return spots;
	}

	/**
	 * If wolfsbane wards whoever it hunts, it drops them and leaves them alone (earning them Not Tonight). A sprig in
	 * hand doesn't ward off a shadow werewolf. Returns whether it did.
	 */
	public boolean checkWard(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target == null || !Werewolves.warded(level, target, kind() != Kind.SHADOW)) {
			return false;
		}
		wardedOff(target);
		return true;
	}

	/** Drops {@code target}, whom wolfsbane wards, and leaves them alone; a player earns Not Tonight. */
	public void wardedOff(LivingEntity target) {
		shun(target);
		if (target instanceof ServerPlayer player) {
			TrickOrTreat.award(player, "wolfsbane_ward");
		}
	}

	/** Leaves {@code target} alone for {@value #SHUN_TICKS} ticks and slinks away from them. */
	public void shun(LivingEntity target) {
		shunned = target.getUUID();
		shunnedUntil = tickCount + SHUN_TICKS;
		setTarget(null);
		runFrom(target.position(), 1.3);
		level().playSound(null, getX(), getY(), getZ(), sound("entity.wolf.whine", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	/** Heads twelve blocks away from {@code from}. */
	private void runFrom(Vec3 from, double speed) {
		Vec3 away = position().subtract(from);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
		Vec3 to = position().add(away.scale(12.0));
		getNavigation().moveTo(to.x, to.y, to.z, speed);
	}

	/** Whether it is leaving {@code target} alone. */
	public boolean shuns(LivingEntity target) {
		return shunned != null && target.getUUID().equals(shunned) && tickCount < shunnedUntil;
	}

	/** A long, low howl at the moon, heard far off. */
	public void howl(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 4.0F, 0.55F);
	}

	/** The night is over: it is gone, in a swirl of smoke. */
	public void turnBack(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 20, 0.4, 0.8, 0.4, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 0.5F);
		discard();
	}

	private static SoundEvent sound(String id, SoundEvent fallback) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(id)).orElse(fallback);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sound("entity.wolf.hurt", SoundEvents.ZOMBIE_HURT);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sound("entity.wolf.death", SoundEvents.ZOMBIE_DEATH);
	}

	@Override
	public float getVoicePitch() {
		return (kind() == Kind.SHADOW ? 0.45F : 0.6F) + random.nextFloat() * 0.1F;
	}

	/** Badly hurt, a brown werewolf runs from whoever is nearest until it has healed ({@link #checkFlight}). */
	private class FleeGoal extends Goal {
		FleeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		private int repath;

		@Override
		public boolean canUse() {
			return fleeing;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void tick() {
			if (--repath > 0 && !getNavigation().isDone()) {
				return;
			}
			repath = 10;
			LivingEntity from = getLastHurtByMob();
			Player near = level().getNearestPlayer(Werewolf.this, PREY_REACH);
			if (near != null && (from == null || distanceToSqr(near) < distanceToSqr(from))) {
				from = near;
			}
			if (from != null) {
				runFrom(from.position(), 1.4);
			}
		}
	}
}
