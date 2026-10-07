package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Hay Golem: a straw man with a carved pumpkin for a head. It is built like an iron golem, from hay bales: two stacked,
 * an arm either side of the upper one (nothing under the arms), and a carved head put on top (any
 * {@code jugcraft:hay_golem_heads} item: a carved pumpkin, a jack o'lantern, or a hand-carved pumpkin with a face cut).
 *
 * <p>It keeps a post: where it was built, or wherever someone holding wheat leads it and leaves it. It is a walking
 * scarecrow ({@link Scarecrows}): crows keep off the crops within {@value Scarecrows#HEADED} blocks of it, or
 * {@value Scarecrows#LIT} with a lit head. It tends the crops round its post: every {@value #TEND_TICKS} ticks it looks for
 * a ripe crop within {@value #POST_RADIUS} blocks of its post, walks to it, harvests it (the crop's own drops), replants it
 * from them when they hold its seed, and keeps the rest in its pouch ({@value #POUCH_SLOTS} stacks). It carries a pouch of
 * {@value #CARRY} or more home to its post, or whatever it has once no crop has been ripe for {@value #IDLE_TICKS} ticks, and
 * puts it into the container under its post (a chest, barrel or hopper), or sets it down there. It only harvests while the
 * {@code mob_griefing} game rule is on and the agriculture feature is enabled.
 *
 * <p>Wheat heals it {@value #WHEAT_HEAL} health; shears take it apart again (its hay bales, head and pouch); fire hurts it
 * twice as much. It never despawns, and is saved with its head, post and pouch. Killed, it drops wheat (loot table
 * {@code jugcraft:entities/hay_golem}), its head and its pouch.
 */
public class HayGolem extends PathfinderMob implements CropGuard {
	public static final int MAX_HEALTH = 20;
	public static final double SPEED = 0.25;
	/** How far round its post it tends crops and wanders. */
	public static final int POST_RADIUS = 8;
	/** How far above or below its post it looks for crops. */
	public static final int SEARCH_HEIGHT = 2;
	/** How often (ticks) it looks for a ripe crop. */
	public static final int TEND_TICKS = 100;
	/** How long (ticks) it works at a crop before it is harvested. */
	public static final int WORK_TICKS = 10;
	/** How long (ticks) it tries to reach a crop, or its post, before it gives up. */
	public static final int GIVE_UP_TICKS = 300;
	public static final int POUCH_SLOTS = 9;
	/** It takes its pouch home once it holds this many items. */
	public static final int CARRY = 32;
	/** It takes what it has home once it has harvested nothing for this many ticks. */
	public static final int IDLE_TICKS = 200;
	public static final float WHEAT_HEAL = 4.0F;
	public static final float FIRE_FACTOR = 2.0F;
	/** The hay bales it is built from, and gives back when sheared. */
	public static final int HAY_BALES = 4;
	/** How near (blocks) it must be to a crop to harvest it, or to its post to unload. */
	public static final double REACH = 1.8;
	/** How near (blocks) a player holding wheat must be to lead it. */
	public static final double LEAD_RANGE = 8.0;
	/** What it may wear as a head (item tag {@code jugcraft:hay_golem_heads}). */
	public static final TagKey<Item> HEADS = TagKey.create(Registries.ITEM, Jugcraft.id("hay_golem_heads"));
	private static final EntityDataAccessor<ItemStack> HEAD = SynchedEntityData.defineId(HayGolem.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Boolean> WORKING = SynchedEntityData.defineId(HayGolem.class, EntityDataSerializers.BOOLEAN);

	private @Nullable BlockPos post;
	private final List<ItemStack> pouch = new ArrayList<>();
	private int tendCooldown;
	private int idle;

	public HayGolem(EntityType<? extends HayGolem> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HEAD, ItemStack.EMPTY);
		builder.define(WORKING, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new FollowWheatGoal());
		goalSelector.addGoal(2, new CarryHomeGoal());
		goalSelector.addGoal(3, new TendGoal());
		goalSelector.addGoal(4, new KeepPostGoal());
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	// ---------------------------------------------------------------- building

	/** Whether {@code stack} can be a golem's head: a head item with a face (a hand-carved pumpkin must have one cut). */
	public static boolean faced(ItemStack stack) {
		if (!stack.is(HEADS)) {
			return false;
		}
		if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CarvedPumpkinBlock) {
			return !stack.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK).isBlank();
		}
		return true;
	}

	private static boolean hay(Level level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.HAY_BLOCK);
	}

	/**
	 * The side one arm of a golem is on, if {@code chest} is the upper of two stacked hay bales with a hay bale either side
	 * of it (east and west, or north and south) and nothing under either arm; otherwise null.
	 */
	public static @Nullable Direction arms(Level level, BlockPos chest) {
		if (!hay(level, chest) || !hay(level, chest.below())) {
			return null;
		}
		for (Direction side : new Direction[] {Direction.EAST, Direction.SOUTH}) {
			BlockPos one = chest.relative(side);
			BlockPos other = chest.relative(side.getOpposite());
			if (hay(level, one) && hay(level, other) && level.getBlockState(one.below()).canBeReplaced()
					&& level.getBlockState(other.below()).canBeReplaced()) {
				return side;
			}
		}
		return null;
	}

	/**
	 * Fabric's use-block callback: a head with a face used on the top of a golem's hay bales brings it to life (on the
	 * server; the client only swings its arm). Anything else passes.
	 */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		if (player.isSpectator() || hit.getDirection() != Direction.UP || !faced(held) || !player.getAbilities().mayBuild
				|| !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
			return InteractionResult.PASS;
		}
		BlockPos chest = hit.getBlockPos();
		Direction side = arms(level, chest);
		if (side == null) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			HayGolem golem = build(server, chest, side, held, player);
			if (golem != null) {
				held.consume(1, player);
				if (player instanceof ServerPlayer builder) {
					TrickOrTreat.award(builder, "man_of_straw");
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Turns the hay bales round {@code chest} into a golem wearing (one of) {@code head}, facing {@code builder}. */
	public static @Nullable HayGolem build(ServerLevel level, BlockPos chest, Direction side, ItemStack head, @Nullable Player builder) {
		HayGolem golem = JugcraftAgriculture.HAY_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (golem == null) {
			return null;
		}
		BlockPos legs = chest.below();
		for (BlockPos pos : List.of(chest.relative(side), chest.relative(side.getOpposite()), chest, legs)) {
			level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)));
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		golem.setPos(legs.getX() + 0.5, legs.getY(), legs.getZ() + 0.5);
		float yaw = builder == null ? 0.0F
				: (float) Math.toDegrees(Math.atan2(builder.getZ() - golem.getZ(), builder.getX() - golem.getX())) - 90.0F;
		golem.setYRot(yaw);
		golem.setYHeadRot(yaw);
		golem.setYBodyRot(yaw);
		golem.setHead(head.copyWithCount(1));
		golem.setPost(legs);
		level.addFreshEntity(golem);
		level.playSound(null, legs, SoundEvents.GRASS_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 1.2, golem.getZ(), 12, 0.4, 0.8, 0.4, 0.0);
		return golem;
	}

	// ---------------------------------------------------------------- state

	public ItemStack head() {
		return entityData.get(HEAD);
	}

	public void setHead(ItemStack head) {
		entityData.set(HEAD, head.isEmpty() ? ItemStack.EMPTY : head.copyWithCount(1));
	}

	/** Whether it is bent over a crop, harvesting (sent to clients for its arms). */
	public boolean working() {
		return entityData.get(WORKING);
	}

	/** Bends it over a crop or straightens it (its tending does this; tests and screenshots set it on a golem without AI). */
	public void setWorking(boolean working) {
		entityData.set(WORKING, working);
	}

	/** Where it keeps: where it was built or was last led to. */
	public BlockPos post() {
		return post == null ? blockPosition() : post;
	}

	public void setPost(BlockPos pos) {
		post = pos.immutable();
	}

	/** How far it guards crops from crows: as a scarecrow wearing its head would. */
	@Override
	public int guardRadius() {
		ItemStack head = head();
		return head.isEmpty() ? Scarecrows.BARE : ScarecrowBlockEntity.lit(head) ? Scarecrows.LIT : Scarecrows.HEADED;
	}

	/** A copy of what it carries. */
	public List<ItemStack> pouch() {
		return pouch.stream().map(ItemStack::copy).toList();
	}

	/** How many items it carries. */
	public int carried() {
		int count = 0;
		for (ItemStack stack : pouch) {
			count += stack.getCount();
		}
		return count;
	}

	/** Puts as much of {@code stack} as fits into its pouch; {@code stack} keeps the rest. */
	public void pocket(ItemStack stack) {
		for (ItemStack held : pouch) {
			if (stack.isEmpty()) {
				return;
			}
			if (ItemStack.isSameItemSameComponents(held, stack)) {
				int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
				held.grow(moved);
				stack.shrink(moved);
			}
		}
		while (!stack.isEmpty() && pouch.size() < POUCH_SLOTS) {
			pouch.add(stack.split(stack.getMaxStackSize()));
		}
	}

	// ---------------------------------------------------------------- work

	/** Whether it may harvest crops in {@code level}: the {@code mob_griefing} game rule (as crows) and the feature. */
	public static boolean mayTend(ServerLevel level) {
		return Crow.mayPeck(level) && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE);
	}

	/** Whether the chunk holding {@code pos} is loaded (it never loads one). */
	private static boolean loaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
	}

	/** The ripe crop nearest it within {@value #POST_RADIUS} blocks of its post (in loaded chunks), or null. */
	public @Nullable BlockPos findRipeCrop(ServerLevel level) {
		BlockPos centre = post();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dx = -POST_RADIUS; dx <= POST_RADIUS; dx++) {
			for (int dz = -POST_RADIUS; dz <= POST_RADIUS; dz++) {
				if (dx * dx + dz * dz > POST_RADIUS * POST_RADIUS) {
					continue;
				}
				BlockPos column = centre.offset(dx, 0, dz);
				if (!loaded(level, column)) {
					continue;
				}
				for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
					BlockPos pos = column.above(dy);
					if (Crow.tempting(level.getBlockState(pos))) {
						double distance = distanceToSqr(Vec3.atCenterOf(pos));
						if (distance < bestDistance) {
							bestDistance = distance;
							best = pos.immutable();
						}
					}
				}
			}
		}
		return best;
	}

	/**
	 * Harvests the ripe crop at {@code pos}: its drops (as a player's hand gets them) go into its pouch (what doesn't fit
	 * drops), less one seed, which replants it; with no seed among the drops the crop is gone. Returns whether it did.
	 */
	public boolean harvest(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!Crow.tempting(state) || !mayTend(level) || !(state.getBlock() instanceof CropBlock crop)) {
			return false;
		}
		List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), this, ItemStack.EMPTY);
		Item seed = state.getBlock().asItem();
		boolean replant = false;
		for (ItemStack drop : drops) {
			if (!replant && drop.is(seed)) {
				drop.shrink(1);
				replant = true;
			}
		}
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		level.setBlock(pos, replant ? crop.getStateForAge(0) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.gameEvent(this, GameEvent.BLOCK_CHANGE, pos);
		for (ItemStack drop : drops) {
			pocket(drop);
			if (!drop.isEmpty()) {
				Block.popResource(level, pos, drop);
			}
		}
		idle = 0;
		return true;
	}

	/**
	 * Empties its pouch at its post: into the container under it (not a furnace or the like, which take items by side), what
	 * fits, and the rest set down on the ground. Returns how many items it put down.
	 */
	public int unload(ServerLevel level) {
		int count = carried();
		BlockPos at = post();
		BlockEntity below = level.getBlockEntity(at.below());
		Container container = below instanceof Container c && !(below instanceof WorldlyContainer) ? c : null;
		for (ItemStack stack : pouch) {
			if (container != null) {
				insert(container, stack);
			}
			if (!stack.isEmpty()) {
				Block.popResource(level, at, stack);
			}
		}
		if (container != null) {
			container.setChanged();
		}
		pouch.clear();
		if (count > 0) {
			level.playSound(null, at, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 0.8F, 1.0F);
		}
		return count;
	}

	/** Puts as much of {@code stack} into {@code container} as it takes; {@code stack} keeps the rest. */
	static void insert(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (!container.canPlaceItem(slot, stack)) {
				continue;
			}
			ItemStack in = container.getItem(slot);
			int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
			if (in.isEmpty()) {
				container.setItem(slot, stack.split(Math.min(stack.getCount(), limit)));
			} else if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < limit) {
				int moved = Math.min(stack.getCount(), limit - in.getCount());
				in.grow(moved);
				stack.shrink(moved);
			}
		}
	}

	/** Shears: back to its hay bales, head and pouch, set down where it stood. */
	public void dismantle(ServerLevel level) {
		BlockPos at = blockPosition();
		Block.popResource(level, at, new ItemStack(Blocks.HAY_BLOCK, HAY_BALES));
		dropKeepings(level);
		level.playSound(null, at, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 12, 0.4, 0.8, 0.4, 0.02);
		discard();
	}

	private void dropKeepings(ServerLevel level) {
		BlockPos at = blockPosition();
		if (!head().isEmpty()) {
			Block.popResource(level, at, head().copy());
			setHead(ItemStack.EMPTY);
		}
		for (ItemStack stack : pouch) {
			Block.popResource(level, at, stack);
		}
		pouch.clear();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.WHEAT) && getHealth() < getMaxHealth()) {
			if (level() instanceof ServerLevel server) {
				heal(WHEAT_HEAL);
				held.consume(1, player);
				server.playSound(null, getX(), getY(), getZ(), SoundEvents.GRASS_PLACE, SoundSource.NEUTRAL, 1.0F, 1.2F);
				server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 6, 0.4, 0.6, 0.4, 0.0);
			}
			return InteractionResult.SUCCESS;
		}
		if (held.is(Items.SHEARS)) {
			if (level() instanceof ServerLevel server) {
				dismantle(server);
				held.hurtAndBreak(1, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Hay burns: fire hurts it twice as much. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return super.hurtServer(level, source, source.is(DamageTypeTags.IS_FIRE) ? amount * FIRE_FACTOR : amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel server) {
			dropKeepings(server);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (post == null) {
			post = blockPosition();
		}
		if (tendCooldown > 0) {
			tendCooldown--;
		}
		if (idle < IDLE_TICKS) {
			idle++;
		}
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GRASS_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.GRASS_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.GRASS_STEP, 0.3F, 0.9F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("head", ItemStack.OPTIONAL_CODEC, head());
		output.store("post", BlockPos.CODEC, post());
		output.store("pouch", ItemStack.CODEC.listOf(), List.copyOf(pouch));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setHead(input.read("head", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
		post = input.read("post", BlockPos.CODEC).orElse(null);
		pouch.clear();
		input.read("pouch", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).limit(POUCH_SLOTS)
				.forEach(pouch::add));
	}

	// ---------------------------------------------------------------- goals

	/** Follows a player holding wheat; where they leave it becomes its post. */
	class FollowWheatGoal extends Goal {
		private @Nullable Player leader;

		FollowWheatGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private boolean leading(@Nullable Player player) {
			return player != null && player.isAlive() && !player.isSpectator() && player.distanceToSqr(HayGolem.this) <= LEAD_RANGE * LEAD_RANGE
					&& (player.getMainHandItem().is(Items.WHEAT) || player.getOffhandItem().is(Items.WHEAT));
		}

		@Override
		public boolean canUse() {
			Player near = level().getNearestPlayer(HayGolem.this, LEAD_RANGE);
			if (!leading(near)) {
				return false;
			}
			leader = near;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return leading(leader);
		}

		@Override
		public void tick() {
			if (leader == null) {
				return;
			}
			getLookControl().setLookAt(leader, 30.0F, 30.0F);
			if (distanceToSqr(leader) > 2.5 * 2.5) {
				getNavigation().moveTo(leader, 1.1);
			} else {
				getNavigation().stop();
			}
		}

		@Override
		public void stop() {
			leader = null;
			getNavigation().stop();
			setPost(blockPosition());
		}
	}

	/** Takes a full pouch (or what it has, once idle) home and empties it. */
	class CarryHomeGoal extends Goal {
		private int ticks;

		CarryHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !pouch.isEmpty() && (carried() >= CARRY || idle >= IDLE_TICKS);
		}

		@Override
		public boolean canContinueToUse() {
			return !pouch.isEmpty() && ticks < GIVE_UP_TICKS;
		}

		@Override
		public void start() {
			ticks = 0;
			BlockPos home = post();
			getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
		}

		@Override
		public void tick() {
			ticks++;
			BlockPos home = post();
			Vec3 at = Vec3.atBottomCenterOf(home);
			if (position().distanceToSqr(at) <= REACH * REACH) {
				if (level() instanceof ServerLevel server) {
					unload(server);
				}
			} else if (getNavigation().isDone()) {
				getNavigation().moveTo(at.x, at.y, at.z, 1.0);
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
			if (ticks >= GIVE_UP_TICKS && !pouch.isEmpty() && level() instanceof ServerLevel server) {
				// Couldn't get home: it sets its load down where it is, rather than carrying it for ever.
				BlockPos was = post;
				setPost(blockPosition());
				unload(server);
				post = was;
			}
		}
	}

	/** Every {@value #TEND_TICKS} ticks, goes to the nearest ripe crop round its post and harvests it. */
	class TendGoal extends Goal {
		private @Nullable BlockPos crop;
		private int work;
		private int ticks;

		TendGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (tendCooldown > 0 || carried() >= CARRY || !(level() instanceof ServerLevel server) || !mayTend(server)) {
				return false;
			}
			tendCooldown = TEND_TICKS;
			crop = findRipeCrop(server);
			return crop != null;
		}

		@Override
		public boolean canContinueToUse() {
			return crop != null && ticks < GIVE_UP_TICKS && Crow.tempting(level().getBlockState(crop));
		}

		@Override
		public void start() {
			work = 0;
			ticks = 0;
			if (crop != null) {
				getNavigation().moveTo(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5, 1.0);
			}
		}

		@Override
		public void tick() {
			if (crop == null) {
				return;
			}
			ticks++;
			Vec3 at = Vec3.atBottomCenterOf(crop);
			getLookControl().setLookAt(at.x, at.y, at.z);
			if (position().distanceToSqr(at) <= REACH * REACH) {
				getNavigation().stop();
				setWorking(true);
				if (++work >= WORK_TICKS && level() instanceof ServerLevel server) {
					harvest(server, crop);
					crop = null;
					tendCooldown = 0;
				}
			} else if (getNavigation().isDone()) {
				getNavigation().moveTo(at.x, at.y, at.z, 1.0);
			}
		}

		@Override
		public void stop() {
			setWorking(false);
			crop = null;
		}
	}

	/** Wanders near its post, and walks back if it has strayed. */
	class KeepPostGoal extends Goal {
		KeepPostGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			BlockPos home = post();
			boolean strayed = position().distanceToSqr(Vec3.atBottomCenterOf(home)) > POST_RADIUS * POST_RADIUS;
			return strayed || getRandom().nextInt(120) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone();
		}

		@Override
		public void start() {
			BlockPos home = post();
			int range = POST_RADIUS / 2;
			int x = home.getX() + getRandom().nextInt(2 * range + 1) - range;
			int z = home.getZ() + getRandom().nextInt(2 * range + 1) - range;
			int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (Math.abs(y - home.getY()) > SEARCH_HEIGHT + 1) {
				x = home.getX();
				y = home.getY();
				z = home.getZ();
			}
			getNavigation().moveTo(x + 0.5, y, z + 0.5, 0.8);
		}
	}
}
