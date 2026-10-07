package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A squirrel (fall addition 24): red or grey, half and half, small and quick, living in the woods ({@link Squirrels}).
 * It scampers up tree trunks (logs are climbable to it), bolts from danger, and is tempted and bred by nuts (item tag
 * {@code jugcraft:squirrel_food}: acorns, chestnuts).
 *
 * <p>It gathers acorns: a squirrel with nothing in its paws goes for an acorn lying within {@value #SEEK_RANGE} blocks
 * and carries it off. After {@value #BURY_TICKS} ticks it buries it in the earth it stands on (woodland floor, as
 * {@link Werewolves#woodlandFloor} judges it), and
 * {@value #SAPLING_CHANCE} of the time a forgotten acorn sprouts there as an oak sapling (not within
 * {@value #SAPLING_SPACE} blocks of another sapling or a log). So squirrels plant the woods they live in.
 */
public class Squirrel extends Animal {
	public static final int MAX_HEALTH = 6;
	public static final double SPEED = 0.32;
	public static final int SEEK_RANGE = 10;
	public static final int BURY_TICKS = 200;
	public static final float SAPLING_CHANCE = 0.25F;
	public static final int SAPLING_SPACE = 3;
	public static final String ACORN = "acorn";
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Jugcraft.id("squirrel_food"));
	private static final EntityDataAccessor<Boolean> GREY = SynchedEntityData.defineId(Squirrel.class, EntityDataSerializers.BOOLEAN);

	/** How long it has carried what is in its paws. */
	private int carried;

	public Squirrel(EntityType<? extends Squirrel> type, Level level) {
		super(type, level);
		entityData.set(GREY, random.nextBoolean());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GREY, false);
	}

	public boolean isGrey() {
		return entityData.get(GREY);
	}

	public void setGrey(boolean grey) {
		entityData.set(GREY, grey);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(5, new GatherAcornGoal());
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public static Item acorn() {
		return JugcraftAgriculture.item(ACORN);
	}

	/** Whether it is carrying an acorn. */
	public boolean carrying() {
		return getMainHandItem().is(acorn());
	}

	/** Takes one acorn from {@code item} into its paws. */
	public void take(ItemEntity item) {
		ItemStack stack = item.getItem();
		setItemInHand(InteractionHand.MAIN_HAND, stack.split(1));
		if (stack.isEmpty()) {
			item.discard();
		} else {
			item.setItem(stack);
		}
		carried = 0;
		playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.6F);
	}

	/** Trees are ladders to a squirrel: it climbs a log it runs into. */
	@Override
	public boolean onClimbable() {
		return super.onClimbable() || (horizontalCollision && level().getBlockState(blockPosition().relative(getDirection())).is(BlockTags.LOGS));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level() instanceof ServerLevel level && carrying() && onGround() && ++carried >= BURY_TICKS) {
			bury(level);
		}
	}

	/**
	 * Buries the acorn in its paws in the earth it stands on, if it stands on earth; sometimes a forgotten one sprouts
	 * as an oak sapling. Returns whether it buried one.
	 */
	public boolean bury(ServerLevel level) {
		BlockPos spot = blockPosition();
		BlockState ground = level.getBlockState(spot.below());
		if (!carrying() || !Werewolves.woodlandFloor(level, spot)) {
			return false;
		}
		setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		carried = 0;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 8, 0.15, 0.05, 0.15, 0.05);
		level.playSound(null, spot, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.NEUTRAL, 0.6F, 1.4F);
		if (random.nextFloat() < SAPLING_CHANCE && roomForSapling(level, spot)) {
			level.setBlock(spot, Blocks.OAK_SAPLING.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		return true;
	}

	/** Whether an oak sapling may sprout at {@code spot}: open air on earth, no sapling or log within the spacing. */
	public static boolean roomForSapling(ServerLevel level, BlockPos spot) {
		if (!level.getBlockState(spot).isAir() || !Werewolves.woodlandFloor(level, spot)) {
			return false;
		}
		for (BlockPos at : BlockPos.betweenClosed(spot.offset(-SAPLING_SPACE, -1, -SAPLING_SPACE), spot.offset(SAPLING_SPACE, 2, SAPLING_SPACE))) {
			BlockState state = level.getBlockState(at);
			if (state.is(BlockTags.SAPLINGS) || state.is(BlockTags.LOGS)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(FOOD);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Squirrel kit = JugcraftAgriculture.SQUIRREL.create(level, EntitySpawnReason.BREEDING);
		if (kit != null) {
			kit.setGrey(partner instanceof Squirrel other && random.nextBoolean() ? other.isGrey() : isGrey());
		}
		ServerPlayer breeder = getLoveCause();
		if (breeder != null) {
			TrickOrTreat.award(breeder, "nuts_about_squirrels");
		}
		return kit;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.FOX_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FOX_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FOX_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.8F + random.nextFloat() * 0.3F;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		// The acorn it carried falls with it (and only once: its paws are emptied before any equipment drops).
		if (carrying()) {
			spawnAtLocation(level, getMainHandItem().copy());
			setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("grey", isGrey());
		output.putInt("carried", carried);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setGrey(input.getBooleanOr("grey", false));
		carried = Math.clamp(input.getIntOr("carried", 0), 0, BURY_TICKS);
	}

	/** With empty paws, it goes for an acorn lying near and takes it. */
	private class GatherAcornGoal extends Goal {
		private @Nullable ItemEntity target;
		private int cooldown;

		GatherAcornGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (carrying() || !getMainHandItem().isEmpty() || isBaby() || --cooldown > 0) {
				return false;
			}
			cooldown = 20;
			List<ItemEntity> acorns = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(SEEK_RANGE),
					item -> item.getItem().is(acorn()) && item.isAlive());
			target = acorns.isEmpty() ? null : acorns.get(random.nextInt(acorns.size()));
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && target.isAlive() && !carrying() && distanceToSqr(target) < (SEEK_RANGE + 4) * (SEEK_RANGE + 4);
		}

		@Override
		public void start() {
			if (target != null) {
				getNavigation().moveTo(target, 1.1);
			}
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}
			if (distanceToSqr(target) < 1.5) {
				take(target);
				target = null;
			} else if (getNavigation().isDone()) {
				getNavigation().moveTo(target, 1.1);
			}
		}

		@Override
		public void stop() {
			target = null;
		}
	}
}
