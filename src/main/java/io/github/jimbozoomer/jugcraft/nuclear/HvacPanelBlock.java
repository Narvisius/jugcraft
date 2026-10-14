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
 * The control room HVAC control panel (round 26, #11: HvacRoom), three wide and two high. Right-clicking its mode
 * selector turns it a position to the right (NORMAL, AUTO, EMERGENCY, round again; sneaking, to the left); anywhere
 * else it says the room's state.
 */
public class HvacPanelBlock extends LivePanelBlock {
	/** The selector's plate on the front (model pixels across the whole panel, facing north: tools/hvac_room.py HP_SEL). */
	static final double SEL_X0 = 2.4, SEL_X1 = 10.8, SEL_Y0 = 7.0, SEL_Y1 = 15.6, FRONT = 15.2;

	public HvacPanelBlock(Properties properties) {
		super(properties, 3, 2, 1);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		// From the placing block (the middle column) to across the whole panel.
		double x = at[0] + 16 * (origin() % width);
		if (x >= SEL_X0 && x <= SEL_X1 && at[1] >= SEL_Y0 && at[1] <= SEL_Y1 && at[2] > FRONT - 1) {
			panel.mode = Math.floorMod(panel.mode + (player.isShiftKeyDown() ? -1 : 1), 3);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.8F);
			// The room answers at once.
			HvacRoom.tick(panel, (ServerLevel) level, HvacRoom.CONTROL);
			panel.syncNow((ServerLevel) level);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.hvac_mode", HvacRoom.MODES[panel.mode]));
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.hvac_panel", HvacRoom.describe(panel)));
		return InteractionResult.SUCCESS_SERVER;
	}
}
