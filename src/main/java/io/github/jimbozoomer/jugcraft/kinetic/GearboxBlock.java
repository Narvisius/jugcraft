package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;

/** A brass gearbox: passes rotation out of all six sides, to branch or turn a shaft line. */
public class GearboxBlock extends Block {
	public GearboxBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ShaftBlock.TURNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ShaftBlock.TURNING);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			KineticNetworks.invalidate(level);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		ShaftBlock.spinDown(this, state, level, pos);
	}
}
