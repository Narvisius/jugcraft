package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Warding Stone: a ritual boundary block ({@code #jugcraft:concordance/ritual_boundary}). Placed or taken away, it
 * tells the circles nearby to check again ({@link Rituals#changed}). With Fusion installed, neighbouring stones join
 * into one carved band (the built-in pack {@code fusion_textures}).
 */
public class WardingStoneBlock extends Block {
	public WardingStoneBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel server && !oldState.is(state.getBlock())) {
			Rituals.changed(server, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Rituals.changed(level, pos);
	}
}
