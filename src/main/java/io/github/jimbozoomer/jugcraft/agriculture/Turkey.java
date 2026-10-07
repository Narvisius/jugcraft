package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A wild turkey: a tom (bronze, a red wattle and snood, a great barred tail) or a hen (brown, plainer), half and half,
 * living in flocks in woods and meadows ({@link Turkeys}). Seeds and corn kernels (item tag {@code jugcraft:turkey_food})
 * tempt and breed them; their poults grow up in twenty minutes as any animal's young do. A tom with an audience (a player
 * or a hen within {@value #STRUT_RANGE} blocks) now and then struts: he stops, fans his tail, puffs up and gobbles, for
 * {@value #STRUT_TICKS} ticks. A hen lays an egg (vanilla's) every {@value #EGG_MIN} to {@value #EGG_MAX} ticks.
 * Turkeys flutter down rather than fall. Each drops a raw turkey and a few feathers.
 */
public class Turkey extends Animal {
	public static final int MAX_HEALTH = 8;
	public static final double SPEED = 0.25;
	public static final int STRUT_RANGE = 6;
	public static final int STRUT_TICKS = 80;
	/** A tom with an audience starts to strut one tick in this many. */
	public static final int STRUT_CHANCE = 200;
	public static final int EGG_MIN = 6000;
	public static final int EGG_MAX = 12000;
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Jugcraft.id("turkey_food"));
	private static final EntityDataAccessor<Boolean> TOM = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> STRUT = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.INT);

	private int eggTime;
	/** How far the tail is fanned, 0 to 1, last tick and this (clients, for drawing). */
	private float fan;
	private float fanO;

	public Turkey(EntityType<? extends Turkey> type, Level level) {
		super(type, level);
		eggTime = nextEgg();
		entityData.set(TOM, random.nextBoolean());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TOM, false);
		builder.define(STRUT, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.4));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	private int nextEgg() {
		return EGG_MIN + random.nextInt(EGG_MAX - EGG_MIN + 1);
	}

	public boolean isTom() {
		return entityData.get(TOM);
	}

	public void setTom(boolean tom) {
		entityData.set(TOM, tom);
	}

	public boolean strutting() {
		return entityData.get(STRUT) > 0;
	}

	public int eggTime() {
		return eggTime;
	}

	public void setEggTime(int ticks) {
		eggTime = Math.max(1, ticks);
	}

	/** How far the tail is fanned at {@code partialTick}, 0 (folded) to 1 (a full fan). */
	public float fan(float partialTick) {
		return Mth.lerp(partialTick, fanO, fan);
	}

	/** Whether this tom has someone to strut for: a player, or a grown hen, within {@value #STRUT_RANGE} blocks. */
	public boolean audience(ServerLevel level) {
		if (level.getNearestPlayer(this, STRUT_RANGE) != null) {
			return true;
		}
		return !level.getEntitiesOfClass(Turkey.class, getBoundingBox().inflate(STRUT_RANGE), other -> other != this && !other.isTom() && !other.isBaby())
				.isEmpty();
	}

	/** A grown tom stops, fans his tail and gobbles. Returns whether he did (hens and poults don't). */
	public boolean strut() {
		return strutFor(STRUT_TICKS);
	}

	/** As {@link #strut()}, for {@code ticks} (tests and screenshots hold a strut longer). */
	public boolean strutFor(int ticks) {
		if (!isTom() || isBaby()) {
			return false;
		}
		entityData.set(STRUT, Math.max(1, ticks));
		getNavigation().stop();
		playSound(SoundEvents.PARROT_AMBIENT, 1.4F, 0.45F);
		gameEvent(GameEvent.ENTITY_ACTION);
		return true;
	}

	/** A hen lays an egg (vanilla's) and starts on the next. Returns whether she did. */
	public boolean layEgg(ServerLevel level) {
		if (isTom() || isBaby()) {
			return false;
		}
		Item egg = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("egg"));
		spawnAtLocation(level, egg);
		playSound(SoundEvents.WOOL_PLACE, 0.8F, 1.2F + (random.nextFloat() - random.nextFloat()) * 0.2F);
		gameEvent(GameEvent.ENTITY_PLACE);
		eggTime = nextEgg();
		return true;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Turkeys flutter down instead of falling.
		Vec3 motion = getDeltaMovement();
		if (!onGround() && motion.y < 0.0) {
			setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
		}
		if (level().isClientSide()) {
			fanO = fan;
			fan = Mth.approach(fan, strutting() ? 1.0F : 0.0F, 0.12F);
			return;
		}
		if (!(level() instanceof ServerLevel server) || !isAlive()) {
			return;
		}
		if (!isTom() && !isBaby() && --eggTime <= 0) {
			layEgg(server);
		}
		int strut = entityData.get(STRUT);
		if (strut > 0) {
			entityData.set(STRUT, strut - 1);
			getNavigation().stop();
		} else if (isTom() && !isBaby() && random.nextInt(STRUT_CHANCE) == 0 && audience(server)) {
			strut();
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(FOOD);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Turkey poult = JugcraftAgriculture.TURKEY.create(level, EntitySpawnReason.BREEDING);
		if (poult != null) {
			poult.setTom(random.nextBoolean());
		}
		ServerPlayer breeder = getLoveCause();
		if (breeder != null) {
			TrickOrTreat.award(breeder, "gobble_gobble");
		}
		return poult;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public void playAmbientSound() {
		// A low warble from a hen; a tom's is deeper still.
		playSound(SoundEvents.PARROT_AMBIENT, getSoundVolume(), (isTom() ? 0.55F : 0.75F) + random.nextFloat() * 0.1F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.GRASS_STEP, 0.15F, 1.2F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("tom", isTom());
		output.putInt("egg_lay_time", eggTime);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setTom(input.getBooleanOr("tom", false));
		eggTime = Math.clamp(input.getIntOr("egg_lay_time", nextEgg()), 1, EGG_MAX);
	}
}
