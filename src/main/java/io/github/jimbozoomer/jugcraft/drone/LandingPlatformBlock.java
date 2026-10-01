package io.github.jimbozoomer.jugcraft.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Landing platform and landing pad blocks. Placing or breaking one tells nearby drone terminals to
 * rescan their platform. Sneak-using a pad with an empty hand returns one docked drone to its owner.
 */
public class LandingPlatformBlock extends Block {
	private final PlatformLayout.Cell cell;

	public LandingPlatformBlock(Properties properties, PlatformLayout.Cell cell) {
		super(properties);
		this.cell = cell;
	}

	public PlatformLayout.Cell cell() {
		return cell;
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

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (cell != PlatformLayout.Cell.PAD || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			DroneTerminalBlockEntity depot = DroneDepots.depotForPad(level, pos);
			if (depot == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.no_depot"));
			} else {
				ItemStack drone = depot.unlinkDrone(player);
				if (!drone.isEmpty() && !player.getInventory().add(drone)) {
					Block.popResource(level, pos.above(), drone);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
