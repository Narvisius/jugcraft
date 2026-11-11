package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The warehouse's multi-block pieces (tools/warehouse_data.py PIECES): w blocks across (to the right of whoever faces
 * it), h high, d deep (from its back, k 0, to its front, k d - 1, where its user stands). Every block is one part,
 * part = (r * d + k) * w + c. The block clicked to place it is the origin: the front row's middle column, at the
 * bottom; it holds the piece's block entity.
 */
public enum WarehousePiece {
	INTAKE("warehouse_intake", 3, 2, 2),
	STATION("warehouse_station", 1, 2, 2),
	TERMINAL("warehouse_terminal", 3, 2, 1),
	GOODS_IN("warehouse_goods_in", 3, 2, 1),
	READY_STORAGE("warehouse_ready_storage", 3, 4, 2),
	READY_STORAGE_EXT("warehouse_ready_storage_ext", 3, 4, 2),
	GOODS_OUT("warehouse_goods_out", 3, 2, 1),
	// Part W3: the Pallet Lift (a landing on each floor, shaft sections between, the drive on top).
	LIFT_LANDING("warehouse_lift_landing", 3, 3, 2),
	LIFT_SHAFT("warehouse_lift_shaft", 3, 1, 2),
	LIFT_DRIVE("warehouse_lift_drive", 3, 2, 2),
	// Part W4: liquids in drums (the filling station behind the Liquids In panel, the emptying station by Ready Storage
	// behind the Liquids Out panel).
	DRUM_FILLER("warehouse_drum_filler", 3, 3, 2),
	DRUM_EMPTIER("warehouse_drum_emptier", 3, 3, 2),
	LIQUIDS_IN("warehouse_liquids_in", 3, 2, 1),
	LIQUIDS_OUT("warehouse_liquids_out", 3, 2, 1),
	// Part W5: the switchboard (its back row in an outer wall: the service entrance outside, the distribution board
	// inside), the status board on a wall, the signal box (redstone).
	SWITCHBOARD("warehouse_switchboard", 3, 2, 2),
	STATUS_BOARD("warehouse_status_board", 3, 2, 1),
	SIGNAL_BOX("warehouse_signal_box", 1, 1, 1);

	public final String id;
	public final int width, height, depth;

	WarehousePiece(String id, int width, int height, int depth) {
		this.id = id;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public int parts() {
		return width * height * depth;
	}

	public int origin() {
		return part(width / 2, 0, depth - 1);
	}

	public int part(int c, int r, int k) {
		return (r * depth + k) * width + c;
	}

	public int column(int part) {
		return part % width;
	}

	public int row(int part) {
		return part / (width * depth);
	}

	public int depthRow(int part) {
		return (part / width) % depth;
	}

	public int longestSide() {
		return Math.max(width, Math.max(height, depth));
	}

	/** Where part p stands from the origin, for a piece whose user looks {@code facing}. */
	public BlockPos offset(Direction facing, int p) {
		int dc = column(p) - width / 2, dk = (depth - 1) - depthRow(p);
		return BlockPos.ZERO.relative(facing.getClockWise(), dc).above(row(p)).relative(facing, dk);
	}

	/** A point in the piece's own pixels (x across, y up, z from its back to its front) in the world, from the origin block's corner. */
	public static double[] toWorld(WarehousePiece piece, Direction facing, BlockPos origin, double x, double y, double z) {
		// The piece's pixel frame: x along its right, z toward its front (against facing), origin at the back left
		// bottom corner of its part 0 (c 0, k 0), which stands at offset(facing, part(0, 0, 0)) from the origin.
		BlockPos p0 = origin.offset(piece.offset(facing, piece.part(0, 0, 0)));
		Direction right = facing.getClockWise();
		Direction front = facing.getOpposite();
		// The corner of block p0 that is the piece's back left: its left side is against right, its back toward facing.
		double cx = p0.getX() + (right.getStepX() < 0 || front.getStepX() < 0 ? 1 : 0);
		double cz = p0.getZ() + (right.getStepZ() < 0 || front.getStepZ() < 0 ? 1 : 0);
		double wx = cx + (right.getStepX() * x + front.getStepX() * z) / 16.0;
		double wz = cz + (right.getStepZ() * x + front.getStepZ() * z) / 16.0;
		return new double[] {wx, origin.getY() + y / 16.0, wz};
	}
}
