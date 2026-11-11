package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
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
 * A battery room's string disconnect switch (older rooms round 4; tools/battery_rooms.py disconnect): a fused 125 VDC
 * switch on a strut frame, one for each of a battery's two strings (labelled 1A-1, 1A-2, 1B-1, 1B-2: string 1 the
 * racks labelled STR 1, StationBatteryBlock). Its handle turns between ON (up) and OFF (left): a click
 * on it (or the handle's plate) switches the string onto or off the bus (DcRoom: the switchboard leaves that string's
 * racks out of its bus, its chargers charge the other string). A click anywhere else says how it stands.
 *
 * <p>The handle's hub: tools/battery_rooms.py BD_HUB (model pixels on the piece facing north, its door's face at z 7.8).
 */
public class BatteryDisconnectBlock extends LivePanelBlock {
	/** The handle's hub, how far round it a click works it (model pixels). */
	static final double[] HUB = {9.6, 14.4};
	static final double REACH = 2.6;

	public BatteryDisconnectBlock(Properties properties) {
		super(properties, 1, 2, 4);
	}

	public static boolean handleAt(double x, double y) {
		return Math.abs(x - HUB[0]) <= REACH && y >= HUB[1] - REACH && y <= HUB[1] + 4.2;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity d)) {
			return InteractionResult.PASS;
		}
		ServerLevel server = (ServerLevel) level;
		// (It does not tick: it finds its switchboard now, before it is worked or says how it stands.)
		d.linkBoardNow(server);
		String label = DcRoom.LABELS[LivePanelBlockEntity.variant(state) % 4];
		String name = "Battery " + label.substring(0, 2) + " string " + label.charAt(3) + " disconnect";
		double[] at = local(facing, origin, hit.getLocation());
		if (at[2] > 6.5 && handleAt(at[0], at[1])) {
			d.mode = d.mode == DcRoom.OPEN ? DcRoom.CLOSED : DcRoom.OPEN;
			d.syncNow(server);
			LivePanelBlockEntity board = DcRoom.board(level, d);
			if (board != null) {
				DcRoom.tickBoard(server, board);
			}
			level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.9F, d.mode == DcRoom.OPEN ? 0.7F : 0.9F);
			player.sendOverlayMessage(Component.translatable(d.mode == DcRoom.OPEN ? "message.jugcraft.nuclear.bd_open" : "message.jugcraft.nuclear.bd_closed",
					name));
			return InteractionResult.SUCCESS_SERVER;
		}
		LivePanelBlockEntity board = DcRoom.board(level, d);
		String bus = board == null ? "no switchboard in reach" : (board.sbFlags & DcRoom.BUS) != 0
				? String.format(Locale.ROOT, "bus %.1f V", DcRoom.busVolts(level, board)) : "bus dead";
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.bd_status", name,
				Component.translatable(d.mode == DcRoom.OPEN ? "message.jugcraft.nuclear.breaker_open" : "message.jugcraft.nuclear.breaker_closed"), bus));
		return InteractionResult.SUCCESS_SERVER;
	}
}
