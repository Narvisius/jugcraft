package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A drone crossing's beacon (part W3): a post at the walkway's edge with amber lamps both ways, LIT (flashing, as the
 * terminal switches it) while a drone is booked through the crossing beside it.
 */
public class BeaconBlock extends DecorBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BeaconBlock(Properties properties) {
		super("crossing_beacon", properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WarehouseLayout.get().plain("crossing_beacon", state.getValue(FACING));
	}
}
