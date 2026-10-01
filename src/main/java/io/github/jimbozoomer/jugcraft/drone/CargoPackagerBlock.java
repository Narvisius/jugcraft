package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cargo packager: drones collect their loads here. Right-click opens its 27 slots. Breaking it
 * drops its contents (vanilla container behaviour).
 */
public class CargoPackagerBlock extends BaseEntityBlock implements ItemConnectable {
	public CargoPackagerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CargoPackagerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CargoPackagerBlockEntity packager) {
			player.openMenu(packager);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(state.getBlock())) {
			DroneDepots.platformChanged(level, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		DroneDepots.platformChanged(level, pos);
	}
}
