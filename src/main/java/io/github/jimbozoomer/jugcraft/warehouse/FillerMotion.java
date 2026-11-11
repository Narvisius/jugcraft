package io.github.jimbozoomer.jugcraft.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drum Filling Station's gantry robot (part W4), worked out the same on the server (how long a move takes) and the
 * client (where everything is at a tick): a plan is one drum's trip, from the head's park over the pallet magazine and
 * back to it. FILL: an empty drum from a magazine place onto the pallet's spot, the tool plate shifted over its bung and
 * seated, the lance in, the drum filled, the lance out (the bung capped), home. RETURN: an emptied drum from the
 * pallet a pallet drone brought back to a free magazine place. Piece pixels (tools/warehouse_w4.py layout_w4), the
 * piece's front to +z; the head's x and z are the Z axis's (its tool plate's middle), drop how far the tool plate's
 * cups hang under the bridge.
 */
public final class FillerMotion {
	public static final int FILL = 1, RETURN = 2;
	/** The steps (ticks): a move across, the Z axis down or up, the vacuum on or off, the shift to the bung, the
	 * plate seated, the lance in or out, capping; the filling rate (mB a tick). */
	public static final int MOVE = 12, DOWN = 10, GRIP = 5, SHIFT = 6, SEAT = 5, LANCE = 10, CAP = 10;
	public static final int RATE = 400;

	/** A trip: its kind, start tick, the magazine place, the pallet spot, the drum's kind (Drums.Kind), its liquid (mB). */
	public record Plan(int type, long start, int place, int spot, int kind, long amount) {
		public static final Codec<Plan> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("type").forGetter(Plan::type), Codec.LONG.fieldOf("start").forGetter(Plan::start),
				Codec.INT.fieldOf("place").forGetter(Plan::place), Codec.INT.fieldOf("spot").forGetter(Plan::spot),
				Codec.INT.fieldOf("kind").forGetter(Plan::kind), Codec.LONG.fieldOf("amount").forGetter(Plan::amount)).apply(i, Plan::new));

		public long end() {
			return start + duration(this);
		}
	}

	/**
	 * The head at a tick: its x and z, the plate's drop, the lance (0 up in the mast, 1 down in the drum), whether the
	 * drum is on the vacuum (carried), where the drum is (x, base y, z: carried, on the pallet or in the magazine),
	 * how much of the fill has run (0..1).
	 */
	public record Pose(float x, float z, float drop, float lance, boolean carried, float drumX, float drumY, float drumZ, float filled,
			boolean placed) {
	}

	private record Key(int t, float x, float z, float drop, float lance) {
	}

	private FillerMotion() {
	}

	private static JsonObject filler() {
		return WarehouseLayout.get().section("drum_filler");
	}

	private static JsonObject drums() {
		return WarehouseLayout.get().section("drums");
	}

	public static float f(JsonObject o, String key) {
		return o.get(key).getAsFloat();
	}

	/** A magazine place's middle (x, z). */
	public static float[] place(int i) {
		JsonArray p = filler().getAsJsonArray("mag_spots").get(i).getAsJsonArray();
		return new float[] {p.get(0).getAsFloat(), p.get(1).getAsFloat()};
	}

	public static int places() {
		return filler().getAsJsonArray("mag_spots").size();
	}

	/** A spot on the pallet in the fill bay: its drum's middle (x, z). */
	public static float[] spot(int i) {
		JsonArray b = filler().getAsJsonArray("bay");
		JsonArray s = drums().getAsJsonArray("spots").get(i % 4).getAsJsonArray();
		return new float[] {b.get(0).getAsFloat() + s.get(0).getAsFloat(), b.get(1).getAsFloat() + s.get(1).getAsFloat()};
	}

	/** Where a drum of a kind is gripped, over its base (its lid; a dewar's guard bars). */
	public static float grip(int kind) {
		return drums().getAsJsonObject("grip").get(Drums.Kind.values()[kind].model).getAsFloat();
	}

	public static float height(int kind) {
		return drums().getAsJsonObject("height").get(Drums.Kind.values()[kind].model).getAsFloat();
	}

	/** The bung (or a dewar's fill valve) from a drum's middle (x, z). */
	public static float[] bung(int kind) {
		JsonArray b = drums().getAsJsonObject("bung").getAsJsonArray(Drums.Kind.values()[kind].model);
		return new float[] {b.get(0).getAsFloat(), b.get(1).getAsFloat()};
	}

	/** The pallet's top in the fill bay (the drums stand on it). */
	public static float palletTop() {
		return f(filler(), "fill_deck") + (float) Kinematics.PALLET_H;
	}

	public static float headY() {
		return f(filler(), "head_y");
	}

	public static float magY() {
		return f(filler(), "mag_y");
	}

	public static float dropMin() {
		return f(filler(), "drop_min");
	}

	public static float[] park() {
		JsonArray p = filler().getAsJsonArray("park");
		return new float[] {p.get(0).getAsFloat(), p.get(1).getAsFloat()};
	}

	public static int fillTicks(long amount) {
		return (int) Math.max(1, (amount + RATE - 1) / RATE);
	}

	private static List<Key> keys(Plan p) {
		List<Key> out = new ArrayList<>();
		float[] park = park(), mag = place(p.place()), spot = spot(p.spot()), bung = bung(p.kind());
		float up = dropMin(), head = headY();
		float atMag = head - (magY() + grip(p.kind())), atBay = head - (palletTop() + grip(p.kind()));
		int t = 0;
		out.add(new Key(t, park[0], park[1], up, 0));
		if (p.type() == FILL) {
			out.add(new Key(t += MOVE, mag[0], mag[1], up, 0));
			out.add(new Key(t += DOWN, mag[0], mag[1], atMag, 0));
			out.add(new Key(t += GRIP, mag[0], mag[1], atMag, 0));
			out.add(new Key(t += DOWN, mag[0], mag[1], up, 0));
			out.add(new Key(t += MOVE, spot[0], spot[1], up, 0));
			out.add(new Key(t += DOWN, spot[0], spot[1], atBay, 0));
			out.add(new Key(t += GRIP, spot[0], spot[1], atBay, 0));
			float bx = spot[0] + bung[0], bz = spot[1] + bung[1];
			out.add(new Key(t += SHIFT, bx, bz, atBay - 2, 0));
			out.add(new Key(t += SEAT, bx, bz, atBay, 0));
			out.add(new Key(t += LANCE, bx, bz, atBay, 1));
			out.add(new Key(t += fillTicks(p.amount()), bx, bz, atBay, 1));
			out.add(new Key(t += LANCE, bx, bz, atBay, 0));
			out.add(new Key(t += CAP, bx, bz, up, 0));
		} else {
			out.add(new Key(t += MOVE, spot[0], spot[1], up, 0));
			out.add(new Key(t += DOWN, spot[0], spot[1], atBay, 0));
			out.add(new Key(t += GRIP, spot[0], spot[1], atBay, 0));
			out.add(new Key(t += DOWN, spot[0], spot[1], up, 0));
			out.add(new Key(t += MOVE, mag[0], mag[1], up, 0));
			out.add(new Key(t += DOWN, mag[0], mag[1], atMag, 0));
			out.add(new Key(t += GRIP, mag[0], mag[1], atMag, 0));
			out.add(new Key(t += DOWN, mag[0], mag[1], up, 0));
		}
		out.add(new Key(t + MOVE, park[0], park[1], up, 0));
		return out;
	}

	public static int duration(Plan p) {
		List<Key> k = keys(p);
		return k.get(k.size() - 1).t();
	}

	/** The head at rest over the pallet magazine. */
	public static Pose parked() {
		float[] park = park();
		return new Pose(park[0], park[1], dropMin(), 0, false, 0, 0, 0, 0, false);
	}

	/** Where everything of a trip is at a tick (time from the plan's start, partial ticks allowed). */
	public static Pose at(Plan p, double time) {
		List<Key> keys = keys(p);
		double t = Math.max(0, time);
		Key a = keys.get(0), b = a;
		int i = 1;
		for (; i < keys.size(); i++) {
			b = keys.get(i);
			if (t <= b.t()) {
				break;
			}
			a = b;
		}
		if (i >= keys.size()) {
			a = b = keys.get(keys.size() - 1);
		}
		double f = b.t() == a.t() ? 1 : Math.max(0, Math.min(1, (t - a.t()) / (b.t() - a.t())));
		// Eased: the axes start and stop gently.
		double e = f * f * (3 - 2 * f);
		float x = (float) (a.x() + (b.x() - a.x()) * e), z = (float) (a.z() + (b.z() - a.z()) * e);
		float drop = (float) (a.drop() + (b.drop() - a.drop()) * e), lance = (float) (a.lance() + (b.lance() - a.lance()) * e);
		// The drum: in the magazine (or on the pallet, returning) till the vacuum takes it, carried, then set down.
		int gripAt = keys.get(3).t(), letGo = keys.get(7).t();
		float[] mag = place(p.place()), spot = spot(p.spot());
		float[] from = p.type() == FILL ? mag : spot, to = p.type() == FILL ? spot : mag;
		float fromY = p.type() == FILL ? magY() : palletTop(), toY = p.type() == FILL ? palletTop() : magY();
		boolean carried = t >= gripAt && t < letGo;
		boolean placed = t >= letGo;
		float dx, dy, dz;
		if (carried) {
			dx = x;
			dz = z;
			dy = headY() - drop - grip(p.kind());
		} else if (placed) {
			dx = to[0];
			dz = to[1];
			dy = toY;
		} else {
			dx = from[0];
			dz = from[1];
			dy = fromY;
		}
		float filled = 0;
		if (p.type() == FILL) {
			int fillFrom = keys.get(10).t(), fillTo = keys.get(11).t();
			filled = (float) Math.max(0, Math.min(1, (t - fillFrom) / Math.max(1, fillTo - fillFrom)));
		}
		return new Pose(x, z, drop, lance, carried, dx, dy, dz, filled, placed);
	}

	/** Whether the plate is seated and the lance in (the fill running): the scale reads, the earth is on. */
	public static boolean filling(Plan p, double time) {
		if (p.type() != FILL) {
			return false;
		}
		List<Key> keys = keys(p);
		return time >= keys.get(9).t() && time < keys.get(12).t();
	}
}
