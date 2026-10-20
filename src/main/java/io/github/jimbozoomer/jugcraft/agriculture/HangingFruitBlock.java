package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Renewable fruit attached to its own tree's canopy; no block entity or per-tick scan. */
public class HangingFruitBlock extends Block {
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
	public static final int RIPE = 2;
	private final String leaves;
	private final String fruit;
	private final int minimum;
	private final int maximum;
	private final VoxelShape shape;

	public HangingFruitBlock(String leaves, String fruit, int minimum, int maximum, Properties properties) {
		super(properties);
		this.leaves = leaves;
		this.fruit = fruit;
		this.minimum = minimum;
		this.maximum = maximum;
		this.shape = fruit.equals("dates") ? Block.box(4, 6, 4, 12, 16, 12) : Block.box(6, 8, 6, 10, 16, 10);
		registerDefaultState(defaultBlockState().setValue(AGE, 0));
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape; }
	@Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
	}
	@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above()).is(JugcraftAgriculture.block(leaves));
	}
	@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}
	@Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(AGE) < RIPE; }
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.canSurvive(level, pos) && state.getValue(AGE) < RIPE && random.nextInt(4) == 0)
			level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
	}

	/** Read the current server state so repeated use cannot collect the same fruit twice. */
	public boolean pick(Level level, BlockPos pos) {
		if (level.isClientSide()) return false;
		BlockState state = level.getBlockState(pos);
		if (!state.is(this) || state.getValue(AGE) != RIPE || !state.canSurvive(level, pos)) return false;
		level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
		Block.popResource(level, pos, new ItemStack(JugcraftAgriculture.item(fruit), minimum + level.getRandom().nextInt(maximum - minimum + 1)));
		level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1, 1);
		return true;
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(AGE) != RIPE) return InteractionResult.PASS;
		if (!level.isClientSide()) pick(level, pos);
		return InteractionResult.SUCCESS;
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
}
