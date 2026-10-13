package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Doily Lace: the Spindle Loft's floor, a sheet of lace two pixels thick lying at the bottom of the block, in one of the
 * doily's {@value #PATTERNS} patterns (tools/spindle_loft.py lace_pattern: the band, mesh, flower and edge). Players stand
 * on it; its holes show the dark below.
 */
public class DoilyLaceBlock extends Block {
	public static final int PATTERNS = 4;
	public static final IntegerProperty PATTERN = IntegerProperty.create("pattern", 0, PATTERNS - 1);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

	public DoilyLaceBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PATTERN, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PATTERN);
	}
}
