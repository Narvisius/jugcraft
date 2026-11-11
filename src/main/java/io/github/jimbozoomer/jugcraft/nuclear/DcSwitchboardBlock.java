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
 * A DC equipment room's 125 VDC distribution switchboard (round 18; older rooms round 3, Justin: "Real 3D, breakers
 * work"): two sections, labelled by train (1A, 1B). A click on its main breaker 72-1A (through the first section's
 * door) or one of its twelve feeder breakers (the second section's dead front) switches it; what it feeds goes dead with
 * it (DcRoom). A click anywhere else says how its bus stands.
 *
 * <p>Where things are: tools/dc_room.py sb_hits (model pixels across the whole piece facing north: x 0..32, y up from
 * the floor 0..32, its front at z 15).
 */
public class DcSwitchboardBlock extends LivePanelBlock {
	/** The main breaker's front (x0, y0, x1, y1), the feeders' columns (their handles' x) and rows (their slots' foot). */
	static final double[] MAIN = {3.4, 5.6, 12.6, 14.2}, FEEDER_X = {20.0, 27.6};
	static final double FEEDER_Y0 = 26.0, FEEDER_PITCH = 3.55, HALF = 2.3, FOOT = -0.6, HEAD = 1.3;

	public DcSwitchboardBlock(Properties properties) {
		super(properties, 2, 2, 2);
	}

	/** The breaker at this point of its front: 0 the main, 1 to 12 the feeders, -1 none. */
	public static int breakerAt(double x, double y) {
		if (x >= MAIN[0] && x <= MAIN[2] && y >= MAIN[1] && y <= MAIN[3]) {
			return 0;
		}
		for (int k = 0; k < DcRoom.FEEDERS; k++) {
			double fx = FEEDER_X[k % 2], fy = FEEDER_Y0 - FEEDER_PITCH * (k / 2);
			if (Math.abs(x - fx) <= HALF && y >= fy + FOOT && y <= fy + HEAD) {
				return k + 1;
			}
		}
		return -1;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity board)) {
			return InteractionResult.PASS;
		}
		ServerLevel server = (ServerLevel) level;
		int train = DcRoom.train(state);
		String l = DcRoom.trainName(train);
		double[] at = local(facing, origin, hit.getLocation());
		// The layout is across the whole piece; the placing block is its right-hand column.
		double x = at[0] + 16 * (origin() % width);
		int b = at[2] > 14 ? breakerAt(x, at[1]) : -1;
		if (b < 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.sb_status", l,
					(board.sbFlags & DcRoom.BUS) != 0 ? String.format(Locale.ROOT, "%.1f V", DcRoom.busVolts(level, board)) : "DEAD",
					Math.round(DcRoom.charge(level, DcRoom.racks(level, board)) * 100), DcRoom.state(DcRoom.closed(board, DcRoom.MAIN)),
					Integer.bitCount(board.breakers & DcRoom.SB_DEFAULT & ~1), DcRoom.feeding(level, board)));
			return InteractionResult.SUCCESS_SERVER;
		}
		board.breakers ^= 1 << b;
		boolean closed = DcRoom.closed(board, b);
		board.changedBreakers(server);
		DcRoom.tickBoard(server, board);
		level.playSound(null, pos, b == 0 ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, b == 0 ? 0.9F : 0.6F,
				closed ? 1.4F : 1.1F);
		player.sendOverlayMessage(b == 0 ? Component.translatable("message.jugcraft.nuclear.sb_main", l, DcRoom.state(closed))
				: Component.translatable("message.jugcraft.nuclear.sb_feeder", String.format(Locale.ROOT, "72-%s-%02d", l, b), DcRoom.feederName(train, b),
						DcRoom.state(closed)));
		return InteractionResult.SUCCESS_SERVER;
	}
}
