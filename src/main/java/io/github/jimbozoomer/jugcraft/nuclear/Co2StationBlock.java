package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A diesel room's CO2 manual release station (round 31 part 3, tools/diesel_fittings.py): on the wall by the door,
 * linked to its room's generator as the panels are (LivePanelBlockEntity). Its PULL TO RELEASE handle starts the
 * countdown, ABORT stops it, RESET after a discharge starts the purge (Co2System); its lamps, countdown and handle
 * follow the generator's CO2 system (client/EdgTelemetry.java).
 */
public class Co2StationBlock extends WallLivePanelBlock {
	/** Where its controls are on its front (model pixels, both blocks: x across, y up from the floor), and their reach. */
	static final double[] PULL = {8.0, 13.7, 3.6, 2.4}, ABORT = {5.3, 9.0, 1.3}, RESET = {10.7, 9.0, 1.3};

	public Co2StationBlock(Properties properties) {
		super(properties, 2, 3.9);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity station)) {
			return InteractionResult.PASS;
		}
		if (station.generator == null || !(level.getBlockEntity(station.generator) instanceof EquipmentBlockEntity edg)
				|| edg.kind() != EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_panel_no_edg"));
			return InteractionResult.SUCCESS_SERVER;
		}
		double[] at = local(facing, origin, hit.getLocation());
		double x = at[0], y = at[1];
		ServerLevel server = (ServerLevel) level;
		if (Math.abs(x - PULL[0]) <= PULL[2] && Math.abs(y - PULL[1]) <= PULL[3]) {
			level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			Co2System.pull(server, edg, origin, player);
		} else if (Math.hypot(x - ABORT[0], y - ABORT[1]) <= ABORT[2]) {
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 1.2F);
			Co2System.abort(server, edg, player);
		} else if (Math.hypot(x - RESET[0], y - RESET[1]) <= RESET[2]) {
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 1.0F);
			Co2System.reset(server, edg, player);
		} else {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.co2_station", DieselPanels.name(edg),
					Component.translatable("message.jugcraft.nuclear.co2_state_" + edg.co2Phase)));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
