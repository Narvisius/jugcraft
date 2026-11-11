package io.github.jimbozoomer.jugcraft.tower;

import java.util.ArrayList;
import java.util.List;

/**
 * The automatic sliding doors' kinds (Justin, 8 November 2026: "I really like these server doors ... add similar ones to
 * the front of the facility and then other places"; take 1 of the sliding doors sheet approved with clear glass inside):
 * their size in cells and where their solid parts are, in the door's own frame (blocks; the door facing north: its
 * outside, where whoever walks up comes from, at -z; x from the master cell across, y up). The server's collision and
 * client/SlidingDoorRenderer both work from these numbers.
 *
 * <ul>
 * <li>{@link #ENTRANCE}: a building's entrance, 6 wide and 3 high: two fixed framed-glass sidelights at the back plane,
 * two framed-glass leaves sliding over them at the front plane (a clear opening 2.88 wide), the operator header across
 * the top row with the motion sensor.</li>
 * <li>{@link #DOUBLE_GLASS} and {@link #DOUBLE_STEEL}: a room's doorway 2 wide and 2 high: two leaves (clear framed glass,
 * or steel with a vision panel) hanging from a track cover on the wall's outside face, each sliding a block along it.</li>
 * <li>{@link #SINGLE_GLASS} and {@link #SINGLE_STEEL}: the same 1 wide, its leaf sliding a block to one side (the
 * block's {@code mirrored} picks which).</li>
 * </ul>
 */
public enum SlidingDoorStyle {
	ENTRANCE(6, 3, true, 2),
	DOUBLE_GLASS(2, 2, true, 2),
	DOUBLE_STEEL(2, 2, false, 2),
	SINGLE_GLASS(1, 2, true, 1),
	SINGLE_STEEL(1, 2, false, 1);

	public final int width, height;
	public final boolean glass;
	public final int leaves;

	SlidingDoorStyle(int width, int height, boolean glass, int leaves) {
		this.width = width;
		this.height = height;
		this.glass = glass;
		this.leaves = leaves;
	}

	public boolean entrance() {
		return this == ENTRANCE;
	}

	// ------------------------------------------------------------------ the entrance's numbers (blocks)

	/** The entrance: jamb width, the sidelights' and leaves' edges, the leaves' top, the header's bottom. */
	public static final double E_JAMB = 0.12, E_LEAF = 1.44, E_LEAF_TOP = 2.545, E_HEADER = 2.55;
	/**
	 * The entrance's planes through the cell (from its outside face): the leaves', the sidelights', the header's (and the
	 * jambs') front and back. The header and jambs come out flush with the curtain wall's face (its framed glass over
	 * them, its posts beside them); the leaves are set back in under the header.
	 */
	public static final double E_LEAF_Z = 0.42, E_SIDE_Z = 0.56, E_HEAD_Z0 = 0.0, E_HEAD_Z1 = 0.66, E_PANEL = 0.06;

	// ------------------------------------------------------------------ a room door's numbers (blocks)

	/** A room door: the lining's width, the leaves' plane (outside the cell, on the wall's face), the track cover's height. */
	public static final double R_LINING = 0.06, R_LEAF_Z0 = -0.095, R_LEAF_Z1 = -0.045, R_TRACK_Y0 = 2.0, R_TRACK_Y1 = 2.24, R_TRACK_Z = -0.17;

	/** How far each leaf slides open (blocks). */
	public double slide() {
		return entrance() ? E_LEAF : 1.0;
	}

	/** Ticks the leaves take to slide. */
	public int slideTicks() {
		return entrance() ? 18 : 14;
	}

	/** The solid boxes that are always there, {x0, y0, z0, x1, y1, z1} in the door's frame. */
	public List<double[]> fixedBoxes(boolean mirrored) {
		List<double[]> out = new ArrayList<>();
		if (entrance()) {
			double w = width;
			out.add(new double[] {0, 0, E_HEAD_Z0, E_JAMB, E_HEADER, E_HEAD_Z1});
			out.add(new double[] {w - E_JAMB, 0, E_HEAD_Z0, w, E_HEADER, E_HEAD_Z1});
			out.add(new double[] {E_JAMB, 0, E_SIDE_Z - E_PANEL / 2, E_LEAF + E_JAMB, E_LEAF_TOP, E_SIDE_Z + E_PANEL / 2});
			out.add(new double[] {w - E_JAMB - E_LEAF, 0, E_SIDE_Z - E_PANEL / 2, w - E_JAMB, E_LEAF_TOP, E_SIDE_Z + E_PANEL / 2});
			out.add(new double[] {0, E_HEADER, E_HEAD_Z0, w, 3, E_HEAD_Z1});
			// the top row is the header and the leaves' heads: solid across, shut or open
			out.add(new double[] {0, 2, E_HEAD_Z0, w, 3, E_HEAD_Z1});
		} else {
			double w = width;
			out.add(new double[] {0, 0, 0, R_LINING, 2, 1});
			out.add(new double[] {w - R_LINING, 0, 0, w, 2, 1});
			out.add(new double[] {0, 2 - R_LINING, 0, w, 2, 1});
		}
		return out;
	}

	/** The leaves' boxes while shut (open, they are out of the doorway and nothing of them collides). */
	public List<double[]> leafBoxes() {
		List<double[]> out = new ArrayList<>();
		if (entrance()) {
			double mid = width / 2.0;
			out.add(new double[] {mid - E_LEAF, 0, E_LEAF_Z - E_PANEL / 2, mid + E_LEAF, E_LEAF_TOP, E_LEAF_Z + E_PANEL / 2});
		} else {
			// (just inside the cell: the leaves hang just outside it, on the wall's face)
			out.add(new double[] {R_LINING, 0, 0, width - R_LINING, 2 - R_LINING, 0.06});
		}
		return out;
	}

	/** Whether a mob may plan a way through the cell at model x {@code dx}, row {@code dy} (the doorway opens for it). */
	public boolean walkable(int dx, int dy) {
		if (entrance()) {
			return dy < 2 && dx >= 2 && dx <= width - 3;
		}
		return true;
	}

	/** The style's block name. */
	public String id() {
		return switch (this) {
			case ENTRANCE -> "automatic_entrance_door";
			case DOUBLE_GLASS -> "automatic_sliding_door_glass";
			case DOUBLE_STEEL -> "automatic_sliding_door_steel";
			case SINGLE_GLASS -> "automatic_sliding_door_single_glass";
			case SINGLE_STEEL -> "automatic_sliding_door_single_steel";
		};
	}
}
