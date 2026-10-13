package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Spindle Loft's measuring tape, the bridge from the pincushion down to the doily: a one-pixel plate at the bottom of
 * the block or halfway up ({@link #HALF}), so the tape falls half a block a block and players walk down it (and up it)
 * without jumping. {@link #MARK} is which strip of the tape it is: its west edge, its middle, its east edge, or the middle
 * with a red mark.
 */
public class MeasuringTapeBlock extends Block {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final IntegerProperty MARK = IntegerProperty.create("mark", 0, 3);
	private static final VoxelShape LOWER = Block.box(0, 0, 0, 16, 1, 16);
	private static final VoxelShape UPPER = Block.box(0, 8, 0, 16, 9, 16);

	public MeasuringTapeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(MARK, 1));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, MARK);
	}
}
