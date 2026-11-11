package io.github.jimbozoomer.jugcraft.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * The drones' geometry, the same on both sides (the server works out their moves with it, the client poses their
 * parts with it): tools/warehouse.py's dimensions, from the layout.
 *
 * <p>A drone's own frame: pixels, x across (its left, as it faces +z), y up, z the way it faces; its middle at (8, 0, 8).
 * Turning it by yaw (degrees, about y the usual way) faces it: 0 south, 90 east, 180 north, -90 west.
 */
public final class Kinematics {
	// ---- the pallet drone
	public static final double PD_FREE, PD_STAGE, PD_LIFT, HEAD_X, HEAD_Z, HEAD_SHIFT;
	/** Where a carried pallet's corner sits in the head's frame. */
	public static final double[] CARRIED;
	// ---- the stacker drone
	public static final double SD_BASE_X, SD_BASE_Y, SD_BASE_Z, SD_SHOULDER, SD_UPPER, SD_FORE;
	public static final double[] CUPS;
	public static final double FILM_BASE, FILM_TOP;
	// ---- pallets and cases
	public static final double PALLET_H, CASE, CASE_H, BEAM_TOP;
	public static final double[][] SPOTS;

	static {
		WarehouseLayout layout = WarehouseLayout.get();
		JsonObject pd = layout.section("pallet_drone");
		PD_FREE = pd.get("free").getAsDouble();
		PD_STAGE = pd.get("stage").getAsDouble();
		PD_LIFT = pd.get("lift").getAsDouble();
		HEAD_X = pd.get("head_x").getAsDouble();
		HEAD_Z = pd.get("head_z").getAsDouble();
		HEAD_SHIFT = pd.get("head_shift").getAsDouble();
		CARRIED = doubles(pd.getAsJsonArray("carried"));
		JsonObject sd = layout.section("stacker_drone");
		double[] base = doubles(sd.getAsJsonArray("base"));
		SD_BASE_X = base[0];
		SD_BASE_Y = base[1];
		SD_BASE_Z = base[2];
		SD_SHOULDER = sd.get("shoulder").getAsDouble();
		SD_UPPER = sd.get("upper").getAsDouble();
		SD_FORE = sd.get("fore").getAsDouble();
		CUPS = doubles(sd.getAsJsonArray("cups"));
		FILM_BASE = sd.get("film_base").getAsDouble();
		FILM_TOP = sd.get("film_top").getAsDouble();
		JsonObject pallet = layout.section("pallet");
		PALLET_H = pallet.get("height").getAsDouble();
		CASE = pallet.get("case").getAsDouble();
		CASE_H = pallet.get("case_height").getAsDouble();
		BEAM_TOP = pallet.get("beam_top").getAsDouble();
		JsonArray spots = pallet.getAsJsonArray("spots");
		SPOTS = new double[spots.size()][];
		for (int i = 0; i < spots.size(); i++) {
			SPOTS[i] = doubles(spots.get(i).getAsJsonArray());
		}
	}

	/**
	 * Part W5, consolidation: a pallet drone holds pallet A over the lane cell to the left of the stacker's at a station,
	 * its forks CONSOL_LIFT up (the pallet clear of the stacker's deck and scanners), the pallet's middle in line with that
	 * cell's; the stacker strafes CONSOL_STRAFE px toward it for each case (and back to set it on B). Its arm then
	 * reaches every case on A (bootcheck WhCheck consolidation()).
	 */
	public static final double CONSOL_STRAFE = 5.6, CONSOL_LIFT = 7.0;

	/**
	 * The top middle of case slot i (world blocks) on the pallet a pallet drone carries: the drone's middle at (x, z) on
	 * a floor at y, facing yaw (degrees), its forks at lift px, its head straight (as DroneRenderer draws it).
	 */
	public static double[] heldTop(double x, double z, double y, double yaw, double lift, int i) {
		int layer = Math.min(1, i / 4);
		double[] spot = SPOTS[i % 4];
		double lx = CARRIED[0] + spot[0] + CASE / 2, lz = CARRIED[2] + spot[1] + CASE / 2;
		double ly = lift + CARRIED[1] + PALLET_H + layer * (CASE_H + 0.05) + CASE_H;
		double ox = (lx - 8) / 16, oz = (lz - 8) / 16, a = Math.toRadians(yaw);
		return new double[] {x + ox * Math.cos(a) + oz * Math.sin(a), y + ly / 16, z - ox * Math.sin(a) + oz * Math.cos(a)};
	}

	/** The stacker's arm folded for driving (base turn, shoulder, elbow, wrist), as in the mockups. */
	public static final List<Float> PARKED = List.of(0F, -35F, 165F, 50F, 0F);

	private Kinematics() {
	}

	/** How far the pallet drone's middle mast stage is up for a lift (the inner stage twice that; free lift first). */
	public static double stage(double lift) {
		return Math.max(0, Math.min(PD_STAGE, (lift - PD_FREE) / 2.0));
	}

	/**
	 * The stacker's arm reaching its cups (pointing down) to a point: (along, across, height) from the arm's base axis,
	 * px (along: the way the drone faces; across: to its left). Returns base turn, shoulder, elbow, wrist (degrees), or
	 * null out of reach.
	 */
	public static double[] reach(double along, double across, double height) {
		double r2 = along * along + across * across;
		// The cups sit CUPS[0] across from the arm's plane: turn the base so they land on the point.
		double off = -CUPS[0];
		if (r2 <= off * off + 1e-6) {
			return null;
		}
		double rho = Math.sqrt(r2 - off * off);
		double q0 = Math.toDegrees(Math.atan2(across, along) + Math.atan2(off, rho));
		double wx = rho, wy = height + CUPS[1] - SD_SHOULDER;
		double d = Math.hypot(wx, wy);
		if (d > SD_UPPER + SD_FORE - 0.01 || d < Math.abs(SD_UPPER - SD_FORE) + 0.01) {
			return null;
		}
		double c3 = (d * d - SD_UPPER * SD_UPPER - SD_FORE * SD_FORE) / (2 * SD_UPPER * SD_FORE);
		double t3 = Math.acos(Math.max(-1, Math.min(1, c3)));            // the elbow folding the forearm forward
		double phi = Math.atan2(wx, wy);
		double k = Math.atan2(SD_FORE * Math.sin(t3), SD_UPPER + SD_FORE * Math.cos(t3));
		double q1 = Math.toDegrees(phi - k), q2 = Math.toDegrees(t3);
		return new double[] {q0, q1, q2, 180 - q1 - q2};
	}

	/**
	 * The stacker's arm for its cups at a point given in the drone's own frame (px), or null out of reach: as
	 * {@link #reach}, from the arm's base.
	 */
	public static double[] reachLocal(double x, double y, double z) {
		// The drone's own frame has x to its left; reach() takes across to its left too.
		return reach(z - SD_BASE_Z, x - SD_BASE_X, y);
	}

	private static double[] doubles(JsonArray a) {
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = a.get(i).getAsDouble();
		}
		return out;
	}
}
