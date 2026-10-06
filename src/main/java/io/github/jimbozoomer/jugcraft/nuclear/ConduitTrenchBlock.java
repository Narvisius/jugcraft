package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Conduit Trench: the circulating water conduit buried just under the ground, under precast concrete covers. It is a
 * full block (flush with the ground, walkable, hidden under roads and floors) and joins like the conduit itself.
 */
public class ConduitTrenchBlock extends NuclearPipeBlock {
	public ConduitTrenchBlock(Properties properties) {
		super(PipeKind.WATER, properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}
}
