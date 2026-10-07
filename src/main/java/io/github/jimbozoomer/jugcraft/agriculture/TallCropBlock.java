package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A crop that grows several blocks tall ({@link TallCrop}), such as 3-block corn.
 * <ul>
 * <li>Every block of the plant is this block; {@link #SECTION} says which one (0 is the bottom, on
 * farmland). All sections carry the same {@link #AGE}, so each shows its own slice of the plant.</li>
 * <li>Only the bottom ticks. It grows into the air above; if that is blocked, the plant waits.</li>
 * <li>Ripe plants are <b>picked</b> with a right-click (or a sickle): the produce drops and the plant
 * goes back to a younger age of the same height, so fields and mazes stay standing.</li>
 * <li>Breaking any section removes the whole plant. Only the bottom has loot, so it drops once.</li>
 * <li>From two blocks tall the plant blocks movement, like a hedge, which is what makes corn mazes work.</li>
 * <li>Climbing crops ({@link TallCrop#trellis}) are planted on a {@link TrellisBlock} and grow up into
 * trellis blocks instead of air. Every block of them drops its trellis again when broken, and they
 * always block movement, as the trellis does.</li>
 * </ul>
 */
public class TallCropBlock extends VegetationBlock implements BonemealableBlock {
	public static final int MAX_AGE = 7;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
	/** Which block of the plant this is, counted up from the bottom (0). */
	public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 2);
	private static final VoxelShape WALL = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

	private final TallCrop crop;
	private final VoxelShape[][] shapes = new VoxelShape[MAX_AGE + 1][3];

	public TallCropBlock(Properties properties, TallCrop crop) {
		super(properties);
		this.crop = crop;
		this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SECTION, 0));
		for (int age = 0; age <= MAX_AGE; age++) {
			for (int section = 0; section < 3; section++) {
				double top = section < crop.height(age) - 1 ? 16.0 : topHeight(age);
				shapes[age][section] = Block.box(2.0, 0.0, 2.0, 14.0, top, 14.0);
			}
		}
	}

	public TallCrop crop() {
		return crop;
	}

	public static boolean isRipe(BlockState state) {
		return state.getValue(AGE) == MAX_AGE;
	}

	/** The bottom block of the plant that {@code state} at {@code pos} belongs to. */
	public BlockPos bottom(BlockPos pos, BlockState state) {
		return pos.below(state.getValue(SECTION));
	}

	/** The selection box of the top block grows through the ages that share a height. */
	private double topHeight(int age) {
		int height = crop.height(age);
		int first = age;
		while (first > 0 && crop.height(first - 1) == height) {
			first--;
		}
		int last = age;
		while (last < MAX_AGE && crop.height(last + 1) == height) {
			last++;
		}
		return 6.0 + 10.0 * (age - first + 1) / (last - first + 1);
	}

	private boolean isSection(BlockState state, int section) {
		return state.is(this) && state.getValue(SECTION) == section;
	}

	private BlockState stateFor(int age, int section) {
		return this.defaultBlockState().setValue(AGE, age).setValue(SECTION, section);
	}

	private boolean isWall(BlockState state) {
		return crop.trellis || crop.height(state.getValue(AGE)) >= 2;
	}

	// ---------------------------------------------------------------- placement and survival

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BlockTags.SUPPORTS_CROPS);
	}

	/** The bottom needs farmland and light, like vanilla crops; every other section needs the one below. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		int section = state.getValue(SECTION);
		if (section > 0) {
			return isSection(level.getBlockState(pos.below()), section - 1);
		}
		return level.getRawBrightness(pos, 0) >= 8 && super.canSurvive(state, level, pos);
	}

	/** A section whose neighbour in the plant is gone breaks too (the bottom drops the loot). */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		int section = state.getValue(SECTION);
		if (direction == Direction.UP && section + 1 < crop.height(state.getValue(AGE)) && !isSection(neighborState, section + 1)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes[state.getValue(AGE)][state.getValue(SECTION)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return isWall(state) ? WALL : Shapes.empty();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return !isWall(state) && super.isPathfindable(state, type);
	}

	// ---------------------------------------------------------------- growth

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(SECTION) == 0 && state.getValue(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		if (age >= MAX_AGE || level.getRawBrightness(pos, 0) < CropGrowth.MIN_GROWTH_LIGHT) {
			return;
		}
		if (random.nextInt(CropGrowth.chanceDivisor(CropGrowth.speed(level, pos, false), crop.growthTime)) == 0) {
			growTo(level, pos, age + 1);
		}
	}

	/**
	 * Whether a plant at {@code bottom} has room to grow from {@code fromAge} to {@code toAge}: it only
	 * grows into air, or, for a climbing crop, only into trellis.
	 */
	public boolean canGrowTo(LevelReader level, BlockPos bottom, int fromAge, int toAge) {
		for (int section = crop.height(fromAge); section < crop.height(toAge); section++) {
			BlockPos pos = bottom.above(section);
			if (level.isOutsideBuildHeight(pos)
					|| !(crop.trellis ? level.getBlockState(pos).getBlock() instanceof TrellisBlock : level.isEmptyBlock(pos))) {
				return false;
			}
		}
		return true;
	}

	/** Grows the plant whose bottom is at {@code bottom} to {@code age}. Returns false if it has no room. */
	public boolean growTo(Level level, BlockPos bottom, int age) {
		BlockState current = level.getBlockState(bottom);
		if (!isSection(current, 0) || age <= current.getValue(AGE) || age > MAX_AGE
				|| !canGrowTo(level, bottom, current.getValue(AGE), age)) {
			return false;
		}
		setAge(level, bottom, age);
		return true;
	}

	/** Writes {@code age} to every section, top first, adding sections the new height needs. */
	private void setAge(Level level, BlockPos bottom, int age) {
		for (int section = crop.height(age) - 1; section >= 0; section--) {
			level.setBlock(bottom.above(section), stateFor(age, section), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		int age = state.getValue(AGE);
		return age < MAX_AGE && canGrowTo(level, bottom(pos, state), age, age + 1);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	/** Bone meal adds one or two ages, as far as there is room above. */
	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		BlockPos bottom = bottom(pos, state);
		int age = state.getValue(AGE);
		int target = Math.min(MAX_AGE, age + 1 + random.nextInt(2));
		while (target > age + 1 && !canGrowTo(level, bottom, age, target)) {
			target--;
		}
		growTo(level, bottom, target);
	}

	// ---------------------------------------------------------------- harvest

	/**
	 * Picks a ripe plant: drops 2-3 (per {@link TallCrop}) produce at {@code dropPos} and sets the plant
	 * back to its regrowth age, which has the same height. Returns false if it is not ripe.
	 */
	public boolean pick(Level level, BlockPos bottom, BlockPos dropPos) {
		BlockState state = level.getBlockState(bottom);
		if (!isSection(state, 0) || !isRipe(state)) {
			return false;
		}
		RandomSource random = level.getRandom();
		int count = crop.pickMin + random.nextInt(crop.pickMax - crop.pickMin + 1);
		Block.popResource(level, dropPos, new ItemStack(JugcraftAgriculture.item(crop.produceId), count));
		setAge(level, bottom, crop.pickReset);
		level.playSound(null, dropPos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
		return true;
	}

	/** Bone meal on an unripe plant and sickles are item actions; anything else in hand may pick. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof SickleItem || (stack.is(Items.BONE_MEAL) && !isRipe(state))) {
			return InteractionResult.PASS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!isRipe(state)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			pick(level, bottom(pos, state), pos);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Breaking an upper section drops the bottom's loot now (with the player's tool, so Fortune
	 * applies) and removes the bottom without a second drop; the rest of the plant then falls apart.
	 * Creative players get no drops. Vanilla double plants do the same. The other sections' own loot
	 * (only a climbing crop's trellis) drops as they fall.
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		int section = state.getValue(SECTION);
		if (!level.isClientSide() && section > 0) {
			BlockPos bottom = pos.below(section);
			BlockState bottomState = level.getBlockState(bottom);
			if (isSection(bottomState, 0)) {
				if (!player.preventsBlockDrops()) {
					Block.dropResources(bottomState, level, bottom, null, player, player.getMainHandItem());
				}
				level.setBlock(bottom, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, bottom, Block.getId(bottomState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item(crop.seedId));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, SECTION);
	}
}
