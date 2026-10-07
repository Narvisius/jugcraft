package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The stem of a squash or gourd, grown from its seeds on farmland like a vanilla pumpkin stem.
 * It grows through ages 0-7 with {@link CropGrowth} (so beans nearby speed it up and dense rows are not
 * penalised). Fully grown, each growth step tries one random side: if that block is air standing on a
 * block that supports vegetation ({@code minecraft:supports_stem_fruit}), the gourd grows there, facing
 * away from the stem, and the stem becomes an {@link AttachedGourdStemBlock} bending towards it.
 */
public class GourdStemBlock extends VegetationBlock implements BonemealableBlock {
	public static final int MAX_AGE = 7;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
	private static final VoxelShape[] SHAPES = new VoxelShape[MAX_AGE + 1];

	static {
		for (int age = 0; age <= MAX_AGE; age++) {
			SHAPES[age] = Block.box(7.0, 0.0, 7.0, 9.0, 2.0 + age * 2.0, 9.0);
		}
	}

	private final String gourdId;
	private final String seedId;
	private final float growthTime;

	/** {@code gourdId} is the fruit, such as {@code "butternut_squash"}; the attached stem is {@code attached_<gourd>_stem}. */
	public GourdStemBlock(Properties properties, String gourdId, String seedId, float growthTime) {
		super(properties);
		this.gourdId = gourdId;
		this.seedId = seedId;
		this.growthTime = growthTime;
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	public Block gourd() {
		return JugcraftAgriculture.block(gourdId);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BlockTags.SUPPORTS_STEM_CROPS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(AGE)];
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getRawBrightness(pos, 0) < CropGrowth.MIN_GROWTH_LIGHT
				|| random.nextInt(CropGrowth.chanceDivisor(CropGrowth.speed(level, pos, false), growthTime)) != 0) {
			return;
		}
		int age = state.getValue(AGE);
		if (age < MAX_AGE) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		} else {
			growGourd(level, pos, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		}
	}

	/**
	 * Grows the gourd on the {@code side} of a fully grown stem at {@code pos}, if that block is air on
	 * soil that supports vegetation. Returns whether it grew.
	 */
	public boolean growGourd(Level level, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		BlockPos target = pos.relative(side);
		if (!state.is(this) || state.getValue(AGE) < MAX_AGE || !level.getBlockState(target).isAir()
				|| !level.getBlockState(target.below()).is(BlockTags.SUPPORTS_STEM_FRUIT)) {
			return false;
		}
		level.setBlockAndUpdate(target, gourd().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, side));
		level.setBlockAndUpdate(pos, JugcraftAgriculture.block("attached_" + gourdId + "_stem").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, side));
		return true;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item(seedId));
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	/** Like vanilla stems: two to five ages at once, and a gourd right away if that completes the stem. */
	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		int age = Math.min(MAX_AGE, state.getValue(AGE) + 2 + random.nextInt(4));
		level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
		if (age == MAX_AGE) {
			growGourd(level, pos, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}
}
