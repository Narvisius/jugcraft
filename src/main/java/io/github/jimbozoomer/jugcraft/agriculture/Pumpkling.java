package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Pumpkling (fall addition 25): a hand-carved pumpkin woken by a spark (a Wisp in a Jar or a bottle of Ectoplasm,
 * item tag {@code jugcraft:pumpkling_sparks}), hopping about on little vine legs and wearing the face that was carved
 * into it. It belongs to whoever woke it and follows them, as a pet does, coming to them when they are far off.
 *
 * <p>Its owner can use it with an empty hand to have it sit and stay, light it with a torch or a soul torch (a torch
 * used on a lit one takes its torch back out), and settle it back into its pumpkin with a glass bottle while sneaking, which
 * fills with its spark again. Anyone can feed it treats ({@code jugcraft:pumpkling_treats}) to heal it. Crows keep away
 * from crops near it, as from a scarecrow wearing that head ({@link Scarecrows}). Its owner's blows don't hurt it;
 * slain, it drops its carved pumpkin, face and all, and its spark goes out.
 */
public class Pumpkling extends PathfinderMob implements CropGuard {
	public static final int MAX_HEALTH = 16;
	public static final double SPEED = 0.3;
	/** How far off it notices a treat held out to it (the attribute its tempt goal reads). */
	public static final double TEMPT_RANGE = 10.0;
	/** It follows its owner once they are further than this, in blocks, and stops this near. */
	public static final double FOLLOW_START = 6.0;
	public static final double FOLLOW_STOP = 2.5;
	/** Further than this from its owner, it comes to them at once. */
	public static final double TELEPORT_DISTANCE = 16.0;
	public static final float TREAT_HEAL = 4.0F;
	public static final TagKey<Item> SPARKS = TagKey.create(Registries.ITEM, Jugcraft.id("pumpkling_sparks"));
	public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, Jugcraft.id("pumpkling_treats"));
	private static final EntityDataAccessor<ItemStack> HEAD = SynchedEntityData.defineId(Pumpkling.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(Pumpkling.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID owner;
	/** The spark that woke it, given back when it settles. */
	private ItemStack spark = ItemStack.EMPTY;

	public Pumpkling(EntityType<? extends Pumpkling> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED)
				.add(Attributes.TEMPT_RANGE, TEMPT_RANGE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HEAD, ItemStack.EMPTY);
		builder.define(SITTING, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StayGoal());
		goalSelector.addGoal(2, new PanicGoal(this, 1.5));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(TREATS), false));
		goalSelector.addGoal(4, new FollowOwnerGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// ---------------------------------------------------------------- what it is

	/** The carved pumpkin it is: the item, with its carving and whether (and with what) it is lit. */
	public ItemStack head() {
		return entityData.get(HEAD);
	}

	public void setHead(ItemStack head) {
		entityData.set(HEAD, head.copyWithCount(1));
	}

	public boolean sitting() {
		return entityData.get(SITTING);
	}

	public void setSitting(boolean sitting) {
		entityData.set(SITTING, sitting);
		if (sitting) {
			getNavigation().stop();
		}
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
	}

	public boolean ownedBy(Player player) {
		return player.getUUID().equals(owner);
	}

	public boolean lit() {
		return ScarecrowBlockEntity.lit(head());
	}

	/** Lit, it guards as far as a scarecrow with a lit head; otherwise as far as one with a carved head. */
	@Override
	public int guardRadius() {
		return lit() ? Scarecrows.LIT : Scarecrows.HEADED;
	}

	// ---------------------------------------------------------------- waking and settling

	/** The item of the hand-carved pumpkin {@code state} with {@code carving} cut into it, lit as the block is. */
	public static ItemStack headOf(BlockState state, PumpkinCarving carving) {
		ItemStack head = new ItemStack(state.getBlock().asItem());
		head.set(JugcraftAgriculture.CARVING, carving);
		head.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CarvedPumpkinBlock.LIT, state.getValue(CarvedPumpkinBlock.LIT))
				.with(CarvedPumpkinBlock.SOUL, state.getValue(CarvedPumpkinBlock.SOUL)));
		return head;
	}

	/** Whether a spark would wake the pumpkin at {@code pos}: a hand-carved pumpkin with a face carved in it. */
	public static boolean wakeable(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof CarvedPumpkinBlock && level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin
				&& !pumpkin.carving().isBlank();
	}

	/**
	 * Wakes the hand-carved pumpkin at {@code pos} with the spark {@code player} holds in {@code hand}: the pumpkin hops
	 * out as a Pumpkling facing the way it faced, owned by {@code player}, and the spark's bottle comes back. Needs build
	 * rights (not in adventure mode); vanilla's block use has checked reach and spawn protection. Returns it, or null if
	 * nothing woke.
	 */
	public static @Nullable Pumpkling wake(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(SPARKS) || !player.mayBuild() || !wakeable(level, pos) || !(level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		Pumpkling pumpkling = JugcraftAgriculture.PUMPKLING.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pumpkling == null) {
			return null;
		}
		pumpkling.setHead(headOf(state, pumpkin.carving()));
		pumpkling.setOwner(player.getUUID());
		pumpkling.spark = held.copyWithCount(1);
		float yaw = state.getValue(CarvedPumpkinBlock.FACING).toYRot();
		pumpkling.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		pumpkling.setYHeadRot(yaw);
		pumpkling.setYBodyRot(yaw);
		level.removeBlock(pos, false);
		level.addFreshEntity(pumpkling);
		player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.02);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
		if (player instanceof ServerPlayer waker) {
			TrickOrTreat.award(waker, "little_jack");
		}
		return pumpkling;
	}

	/** Settles back into its pumpkin: the carved pumpkin drops where it stood, and {@code bottle} fills with its spark. */
	private void settle(ServerLevel level, Player player, InteractionHand hand) {
		ItemStack bottle = player.getItemInHand(hand);
		ItemStack filled = spark.isEmpty() ? new ItemStack(JugcraftAgriculture.item("ectoplasm")) : spark.copy();
		player.setItemInHand(hand, ItemUtils.createFilledResult(bottle, player, filled));
		spawnAtLocation(level, head().copy());
		setHead(ItemStack.EMPTY);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
		discard();
	}

	// ---------------------------------------------------------------- using it

	/**
	 * Anyone feeds it treats to heal it. Its owner, with an empty hand, has it sit or get up, as with a tame wolf; with a
	 * torch or a soul torch lights it, or takes a lit one's torch back out; and, sneaking with a glass bottle, settles it
	 * back into its pumpkin. Decided on the server, where its owner is known.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		boolean sneaking = player.isSecondaryUseActive();
		boolean ours = held.is(TREATS) || CarvedPumpkinBlock.isTorch(held) || held.isEmpty() || (sneaking && held.is(Items.GLASS_BOTTLE));
		if (!(level() instanceof ServerLevel level)) {
			return ours ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
		}
		if (held.is(TREATS) && getHealth() < getMaxHealth()) {
			heal(TREAT_HEAL);
			held.consume(1, player);
			level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 3, 0.2, 0.1, 0.2, 0.0);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.4F);
			return InteractionResult.SUCCESS;
		}
		if (!ownedBy(player)) {
			return ours ? InteractionResult.FAIL : super.mobInteract(player, hand);
		}
		if (sneaking && held.is(Items.GLASS_BOTTLE)) {
			settle(level, player, hand);
			return InteractionResult.SUCCESS;
		}
		if (held.isEmpty()) {
			setSitting(!sitting());
			playSound(SoundEvents.WOOD_PLACE, 0.6F, sitting() ? 0.8F : 1.4F);
			return InteractionResult.SUCCESS;
		}
		if (CarvedPumpkinBlock.isTorch(held) && !lit()) {
			setLit(true, held.is(Items.SOUL_TORCH));
			held.consume(1, player);
			playSound(SoundEvents.WOOD_PLACE, 1.0F, 1.2F);
			return InteractionResult.SUCCESS;
		}
		if (CarvedPumpkinBlock.isTorch(held)) {
			ItemStack torch = CarvedPumpkinBlock.torch(ScarecrowBlockEntity.soul(head()));
			setLit(false, false);
			if (!player.getInventory().add(torch)) {
				spawnAtLocation(level, torch);
			}
			playSound(SoundEvents.WOOD_BREAK, 0.8F, 1.2F);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Lights its head (with a soul torch's flame if {@code soul}) or puts it out. */
	public void setLit(boolean lit, boolean soul) {
		ItemStack head = head().copy();
		head.set(DataComponents.BLOCK_STATE, head.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
				.with(CarvedPumpkinBlock.LIT, lit).with(CarvedPumpkinBlock.SOUL, lit && soul));
		setHead(head);
	}

	// ---------------------------------------------------------------- following

	public @Nullable Player ownerPlayer() {
		return owner == null ? null : level().getPlayerByUUID(owner);
	}

	/** The spots round its owner it may come to: two or three blocks off, across or along. */
	private static final int[][] AROUND = around();

	private static int[][] around() {
		List<int[]> out = new ArrayList<>();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
					out.add(new int[] {dx, dz});
				}
			}
		}
		return out.toArray(new int[0][]);
	}

	/**
	 * Comes to its owner: to open ground two or three blocks from them. Every such spot is tried, in a fresh order each
	 * time so it doesn't always come to the same side, so it fails only when there is no room by them. Returns whether it
	 * found a spot.
	 */
	public boolean comeToOwner() {
		Player player = ownerPlayer();
		if (player == null || player.level() != level()) {
			return false;
		}
		BlockPos center = player.blockPosition();
		int[] order = new int[AROUND.length];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		for (int i = order.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int swap = order[i];
			order[i] = order[j];
			order[j] = swap;
		}
		for (int index : order) {
			int[] offset = AROUND[index];
			for (int dy = 1; dy >= -1; dy--) {
				BlockPos spot = center.offset(offset[0], dy, offset[1]);
				Vec3 to = Vec3.atBottomCenterOf(spot);
				if (level().getBlockState(spot.below()).isFaceSturdy(level(), spot.below(), Direction.UP)
						&& level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
					snapTo(to.x, to.y, to.z, getYRot(), getXRot());
					getNavigation().stop();
					return true;
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- harm

	/** Its owner's blows don't hurt it (it can be settled with a bottle instead). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && ownedBy(player)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		// Its pumpkin, face and all, is never lost; its spark goes out.
		if (!head().isEmpty()) {
			spawnAtLocation(level, head().copy());
			setHead(ItemStack.EMPTY);
		}
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 10, 0.2, 0.3, 0.2, 0.03);
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WOOD_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WOOD_BREAK;
	}

	@Override
	public float getVoicePitch() {
		return 1.3F + random.nextFloat() * 0.2F;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("head", ItemStack.OPTIONAL_CODEC, head());
		output.store("spark", ItemStack.OPTIONAL_CODEC, spark);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putBoolean("sitting", sitting());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setHead(input.read("head", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
		spark = input.read("spark", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		setSitting(input.getBooleanOr("sitting", false));
	}

	// ---------------------------------------------------------------- goals

	/** Sitting, it stays where it is. */
	private class StayGoal extends Goal {
		StayGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return sitting();
		}

		@Override
		public void start() {
			getNavigation().stop();
		}
	}

	/** Follows its owner: walks to them past {@link #FOLLOW_START}, and comes to them at once past {@link #TELEPORT_DISTANCE}. */
	private class FollowOwnerGoal extends Goal {
		private @Nullable Player target;
		private int repath;

		FollowOwnerGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (sitting()) {
				return false;
			}
			Player player = ownerPlayer();
			if (player == null || player.isSpectator() || player.level() != level() || distanceToSqr(player) < FOLLOW_START * FOLLOW_START) {
				return false;
			}
			target = player;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && !sitting() && target.isAlive() && target.level() == level() && distanceToSqr(target) > FOLLOW_STOP * FOLLOW_STOP;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void stop() {
			target = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}
			getLookControl().setLookAt(target, 10.0F, getMaxHeadXRot());
			if (--repath > 0) {
				return;
			}
			repath = 10;
			if (distanceToSqr(target) > TELEPORT_DISTANCE * TELEPORT_DISTANCE && comeToOwner()) {
				return;
			}
			getNavigation().moveTo(target, 1.2);
		}
	}
}
