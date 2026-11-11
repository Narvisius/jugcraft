package io.github.jimbozoomer.jugcraft.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drum Emptying Station's jib and drum pump (part W4), worked out the same on the server and the client: the jib
 * swings the pump over each drum's bung in turn (its balancer running out along the arm), dips it in, pumps what the
 * order takes from that drum into the holding tank, lifts it out; then swings back to its park. Piece pixels (the
 * piece's front to +z); the jib's angle is about its pillar, 0 along +z, positive toward +x.
 */
public final class EmptierMotion {
	/** The steps (ticks): a swing, the pump dipped in or lifted out; the pump's rate (mB a tick). */
	public static final int SWING = 20, DIP = 12, RATE = 400;

	/** One drum emptied (by its index on the pallet) of so much (mB). */
	public record Step(int drum, long amount) {
		public static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("drum").forGetter(Step::drum), Codec.LONG.fieldOf("amount").forGetter(Step::amount)).apply(i, Step::new));
	}

	/** A pallet's run: its start, the drums' kind, the steps. */
	public record Plan(long start, int kind, List<Step> steps) {
		public static final Codec<Plan> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("start").forGetter(Plan::start), Codec.INT.fieldOf("kind").forGetter(Plan::kind),
				Step.CODEC.listOf().fieldOf("steps").forGetter(Plan::steps)).apply(i, Plan::new));

		public long end() {
			return start + duration(this);
		}

		public long total() {
			long n = 0;
			for (Step s : steps) {
				n += s.amount();
			}
			return n;
		}
	}

	/** The jib at a tick: its angle, how far out the balancer is, the pump dipped (0 out .. 1 in), the pump's base y,
	 * how much has been pumped so far (mB), whether it pumps now. */
	public record Pose(float angle, float radius, float dip, float pumpY, long pumped, boolean pumping) {
	}

	private record Key(int t, float angle, float radius, float dip, float y) {
	}

	private EmptierMotion() {
	}

	private static JsonObject emptier() {
		return WarehouseLayout.get().section("drum_emptier");
	}

	public static float deckTop() {
		return FillerMotion.f(emptier(), "deck") + (float) Kinematics.PALLET_H;
	}

	/** A drum's middle on the platform's pallet (x, z). */
	public static float[] spot(int i) {
		JsonArray b = emptier().getAsJsonArray("bay");
		JsonArray s = WarehouseLayout.get().section("drums").getAsJsonArray("spots").get(i % 4).getAsJsonArray();
		return new float[] {b.get(0).getAsFloat() + s.get(0).getAsFloat(), b.get(1).getAsFloat() + s.get(1).getAsFloat()};
	}

	/** The jib's pillar (x, z) and its arm's underside (y). */
	public static float[] jib() {
		JsonArray j = emptier().getAsJsonArray("jib");
		return new float[] {j.get(0).getAsFloat(), j.get(1).getAsFloat(), j.get(2).getAsFloat()};
	}

	public static float[] park() {
		JsonArray p = emptier().getAsJsonArray("park");
		return new float[] {p.get(0).getAsFloat(), p.get(1).getAsFloat()};
	}

	public static float lift() {
		return FillerMotion.f(emptier(), "pump_lift");
	}

	private static float[] over(int drum, int kind) {
		float[] s = spot(drum), b = FillerMotion.bung(kind), j = jib();
		double dx = s[0] + b[0] - j[0], dz = s[1] + b[1] - j[1];
		return new float[] {(float) Math.toDegrees(Math.atan2(dx, dz)), (float) Math.hypot(dx, dz)};
	}

	private static List<Key> keys(Plan p) {
		List<Key> out = new ArrayList<>();
		float[] park = park();
		float top = deckTop() + FillerMotion.grip(p.kind());
		int t = 0;
		out.add(new Key(t, park[0], park[1], 0, top));
		for (Step s : p.steps()) {
			float[] o = over(s.drum(), p.kind());
			out.add(new Key(t += SWING, o[0], o[1], 0, top));
			out.add(new Key(t += DIP, o[0], o[1], 1, top));
			out.add(new Key(t += (int) Math.max(1, (s.amount() + RATE - 1) / RATE), o[0], o[1], 1, top));
			out.add(new Key(t += DIP, o[0], o[1], 0, top));
		}
		out.add(new Key(t + SWING, park[0], park[1], 0, top));
		return out;
	}

	public static int duration(Plan p) {
		List<Key> k = keys(p);
		return k.get(k.size() - 1).t();
	}

	public static Pose parked(int kind) {
		float[] park = park();
		return new Pose(park[0], park[1], 0, deckTop() + FillerMotion.grip(kind), 0, false);
	}

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
		double e = f * f * (3 - 2 * f);
		float angle = (float) (a.angle() + (b.angle() - a.angle()) * e), radius = (float) (a.radius() + (b.radius() - a.radius()) * e);
		float dip = (float) (a.dip() + (b.dip() - a.dip()) * e);
		// What has gone into the tank: the steps done, and the one pumping now as far as it has run.
		long pumped = 0;
		boolean pumping = false;
		int k = 1;
		for (Step s : p.steps()) {
			Key from = keys.get(k + 1), to = keys.get(k + 2);
			if (t >= to.t()) {
				pumped += s.amount();
			} else if (t > from.t()) {
				pumped += (long) (s.amount() * (t - from.t()) / Math.max(1, to.t() - from.t()));
				pumping = true;
			}
			k += 4;
		}
		return new Pose(angle, radius, dip, a.y(), pumped, pumping);
	}
}
