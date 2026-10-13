package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A taut thread in the Spindle Loft, two pixels thick through the middle of the block along its axis: up from the spools
 * to the rafters, and across between them. Nothing collides with it.
 */
public class TautThreadBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	public static final EnumProperty<ThreadColour> COLOUR = SpoolThreadBlock.COLOUR;
	private static final VoxelShape X = Block.box(0, 7, 7, 16, 9, 9);
	private static final VoxelShape Y = Block.box(7, 0, 7, 9, 16, 9);
	private static final VoxelShape Z = Block.box(7, 7, 0, 9, 9, 16);

	public TautThreadBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(COLOUR, ThreadColour.WHITE));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X;
			case Y -> Y;
			case Z -> Z;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, COLOUR);
	}
}
