package io.github.jimbozoomer.jugcraft.tower.squad;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Calling a squad down (sheet 5): a mark from the designator's sight is checked (open sky over it, never under a roof of
 * rock; within reach; no squad of the player's already out; a charge for that squad on their account), then the charge
 * is spent and a pod launched for each soldier: one flare high over the mark, the pods splitting off it and weaving
 * down to land round the mark, 10 to 20 blocks apart, the leader's nearest the mark ({@link DropPod}).
 */
public final class SquadDrop {
	/** What became of a mark (the client's sight says so). */
	public static final int OK = 0, NO_CHARGE = 1, IN_FIELD = 2, NO_SKY = 3, TOO_FAR = 4, NO_ROOM = 5, NOT_SIGHTED = 6;

	private SquadDrop() {
	}

	/** {@code player} marks {@code at} for a squad of {@code size}: one of the results above. */
	public static int mark(ServerPlayer player, Vec3 at, int size) {
		int index = JugcraftSquads.index(size);
		ServerLevel level = player.level();
		if (index < 0 || !LaserDesignatorItem.sighted(player)) {
			return NOT_SIGHTED;
		}
		if (player.getEyePosition().distanceTo(at) > JugcraftSquads.MARK_RANGE + 2) {
			return TOO_FAR;
		}
		if (!openSky(level, at)) {
			return NO_SKY;
		}
		SquadAccounts accounts = SquadAccounts.get(level);
		if (accounts.inField(level.getServer(), player.getUUID())) {
			return IN_FIELD;
		}
		if (accounts.charges(player.getUUID())[index] <= 0 && !player.isCreative()) {
			return NO_CHARGE;
		}
		RandomSource rng = level.getRandom();
		List<Vec3> spots = spots(level, at, size, rng);
		if (spots.size() < size) {
			return NO_ROOM;
		}
		if (!player.isCreative()) {
			accounts.spend(player.getUUID(), index);
		}
		launch(level, player.getUUID(), player.position(), at, spots, index, rng);
		return OK;
	}

	/** Whether {@code at} is under open sky, outdoors: not in a world with a roof of rock, at the top of what stands there. */
	public static boolean openSky(ServerLevel level, Vec3 at) {
		if (level.dimensionType().hasCeiling()) {
			return false;
		}
		BlockPos p = BlockPos.containing(at.x, at.y - 0.01, at.z);
		if (!level.hasChunkAt(p)) {
			return false;
		}
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
		return at.y >= top - 1.5;
	}

	/**
	 * Where the pods come down: the leader's within a few blocks of the mark, the others round it, each at least
	 * {@link JugcraftSquads#SPREAD_MIN} from the rest (a little less if the ground's tight) and no more than
	 * {@link JugcraftSquads#SPREAD_MAX} from the mark (a little more likewise): on open ground (no leaves, nothing over it,
	 * not in water or lava) within 6 blocks up or down of the mark.
	 */
	public static List<Vec3> spots(ServerLevel level, Vec3 mark, int n, RandomSource rng) {
		List<Vec3> out = new ArrayList<>();
		for (int tries = 0; tries < 60 && out.isEmpty(); tries++) {
			double r = tries < 20 ? rng.nextDouble() * 3 : rng.nextDouble() * 6;
			double a = rng.nextDouble() * Mth.TWO_PI;
			Vec3 s = ground(level, mark.x + Math.cos(a) * r, mark.z + Math.sin(a) * r, mark.y);
			if (s != null) {
				out.add(s);
			}
		}
		if (out.isEmpty()) {
			return out;
		}
		for (int tries = 0; tries < 600 && out.size() < n; tries++) {
			double apart = tries < 300 ? JugcraftSquads.SPREAD_MIN : JugcraftSquads.SPREAD_MIN * 0.7;
			double far = tries < 300 ? JugcraftSquads.SPREAD_MAX : JugcraftSquads.SPREAD_MAX * 1.3;
			double r = JugcraftSquads.SPREAD_MIN * 0.8 + rng.nextDouble() * (far - JugcraftSquads.SPREAD_MIN * 0.8);
			double a = rng.nextDouble() * Mth.TWO_PI;
			Vec3 s = ground(level, mark.x + Math.cos(a) * r, mark.z + Math.sin(a) * r, mark.y);
			if (s == null) {
				continue;
			}
			boolean clear = true;
			for (Vec3 o : out) {
				if (Math.hypot(o.x - s.x, o.z - s.z) < apart) {
					clear = false;
					break;
				}
			}
			if (clear) {
				out.add(s);
			}
		}
		return out;
	}

	/** The ground's top at (x, z) if a pod can land there (see {@link #spots}), the middle of its block; else null. */
	private static Vec3 ground(ServerLevel level, double x, double z, double nearY) {
		BlockPos col = BlockPos.containing(x, nearY, z);
		if (!level.hasChunkAt(col)) {
			return null;
		}
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
		if (top != level.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ()) || Math.abs(top - nearY) > 6) {
			return null;                                 // under leaves or anything else, or a cliff away
		}
		BlockPos below = new BlockPos(col.getX(), top - 1, col.getZ());
		if (!level.getFluidState(below).isEmpty() || !level.getFluidState(below.above()).isEmpty()
				|| !level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
			return null;
		}
		return new Vec3(col.getX() + 0.5, top, col.getZ() + 0.5);
	}

	/** How far apart (blocks) two pods in flight are kept: side to side, or one above the other. */
	public static final double CLEAR_SIDE = 3.5, CLEAR_ABOVE = 6.0;
	/** How widely the pods' points in the sky are spread about the flare (blocks): across the way they come from, along
	 * it, and up or down. */
	static final double SPREAD_ACROSS = 90, SPREAD_ALONG = 50, SPREAD_UP = 60;
	/** Where the flare hangs from the mark (blocks): out toward the ship that fires them, and up (high in the sky, far over
	 * the clouds: the pods come in on a long slant from there, fast). */
	public static final double FLARE_OUT = 120, FLARE_UP = 520;

	/**
	 * The flare and the pods: one pod a spot, the first the leader's; each hatch toward the mark (the leader's toward
	 * the player). The leader's pod sets off from the flare, the others each from their own point in the sky about it
	 * (Justin, 10 November 2026: "more spread out flight paths while still landing around as close as they are now"),
	 * one after another over up to {@value DropPod#SPREAD_TICKS} ticks in no set order, and none comes within
	 * {@link #CLEAR_SIDE} blocks of another on the way (a pod's weave and point are drawn again till it's clear of those
	 * already planned: "make sure the pods don't run into each other on the way down").
	 */
	public static void launch(ServerLevel level, UUID owner, Vec3 from, Vec3 mark, List<Vec3> spots, int index, RandomSource rng) {
		long now = level.getGameTime();
		UUID squad = UUID.randomUUID();
		SquadAccounts accounts = SquadAccounts.get(level);
		for (Plan p : plan(mark, spots, rng)) {
			Vec3 s = p.spot();
			Vec3 face = (p.place() == 0 ? from : mark).subtract(s);
			float yaw = (float) Math.atan2(-face.x, -face.z);
			DropPod pod = JugcraftSquads.DROP_POD.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
			if (pod == null) {
				continue;
			}
			pod.snapTo(s.x, s.y, s.z, 0, 0);
			pod.setUp(owner, squad, index, p.place(), now, p.delay(), p.flare(), p.seed(), yaw);
			level.addFreshEntity(pod);
			accounts.out(owner, pod.getUUID());
		}
	}

	/** One pod's flight: where it lands, its place in the squad, its delay, its point in the sky (from where it lands)
	 * and its weave. */
	public record Plan(Vec3 spot, int place, int delay, Vector3f flare, int seed) {
	}

	/** The squad's flights (see {@link #launch}), worked out before any pod is made. */
	public static List<Plan> plan(Vec3 mark, List<Vec3> spots, RandomSource rng) {
		double a = rng.nextDouble() * Mth.TWO_PI;
		// the flare: high over the mark, off toward where the ship that fires them is
		Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a)), across = new Vec3(-Math.sin(a), 0, Math.cos(a));
		Vec3 flare = mark.add(out.scale(FLARE_OUT)).add(0, FLARE_UP, 0);
		int n = spots.size();
		int[] delays = delays(n, rng);
		List<Plan> out_ = new ArrayList<>();
		for (int k = 0; k < n; k++) {
			Vec3 s = spots.get(k);
			Plan best = null;
			double bestClear = Double.NEGATIVE_INFINITY;
			for (int tries = 0; tries < 48; tries++) {
				Vec3 sky = k == 0 && tries == 0 ? flare : flare.add(across.scale((rng.nextDouble() * 2 - 1) * SPREAD_ACROSS))
						.add(out.scale((rng.nextDouble() * 2 - 1) * SPREAD_ALONG)).add(0, (rng.nextDouble() * 2 - 1) * SPREAD_UP, 0);
				Vec3 off = sky.subtract(s);
				Plan p = new Plan(s, k, delays[k], new Vector3f((float) off.x, (float) off.y, (float) off.z), rng.nextInt());
				double c = Double.POSITIVE_INFINITY;
				for (Plan q : out_) {
					c = Math.min(c, clearance(p, q));
				}
				if (c > bestClear) {
					best = p;
					bestClear = c;
				}
				if (c >= 0) {
					break;
				}
			}
			out_.add(best);
		}
		return out_;
	}

	/** Each pod's delay (ticks): spread over up to {@value DropPod#SPREAD_TICKS} in no set order, at least a little apart. */
	static int[] delays(int n, RandomSource rng) {
		int[] d = new int[n];
		if (n <= 1) {
			return d;
		}
		int gap = Math.min(12, DropPod.SPREAD_TICKS / (n - 1));
		int free = DropPod.SPREAD_TICKS - gap * (n - 1);
		double[] cuts = new double[n];
		for (int i = 0; i < n; i++) {
			cuts[i] = rng.nextDouble();
		}
		java.util.Arrays.sort(cuts);
		double lo = cuts[0], hi = cuts[n - 1];
		int[] sorted = new int[n];
		for (int i = 0; i < n; i++) {
			sorted[i] = (int) Math.round(hi > lo ? (cuts[i] - lo) / (hi - lo) * free : 0) + i * gap;
		}
		// dealt out to the pods in no set order
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			order.add(i);
		}
		for (int i = n - 1; i > 0; i--) {
			java.util.Collections.swap(order, i, rng.nextInt(i + 1));
		}
		for (int i = 0; i < n; i++) {
			d[order.get(i)] = sorted[i];
		}
		return d;
	}

	/**
	 * How clear of each other two pods stay while both are in flight (blocks): the least, over their flights, of how far
	 * past {@link #CLEAR_SIDE} apart side to side or {@link #CLEAR_ABOVE} one above the other they are (below 0: they'd
	 * touch).
	 */
	public static double clearance(Plan p, Plan q) {
		int t0 = JugcraftSquads.FLARE_TICKS + Math.max(p.delay(), q.delay());
		int t1 = JugcraftSquads.FLARE_TICKS + JugcraftSquads.FALL_TICKS + Math.min(p.delay(), q.delay());
		Vector3f a = new Vector3f(), b = new Vector3f();
		double worst = Double.POSITIVE_INFINITY;
		for (float t = t0; t <= t1; t += 0.5F) {                    // (every half tick: they go some 5 blocks a tick)
			DropPod.path(p.flare(), p.seed(), t - p.delay(), a).add((float) p.spot().x, (float) p.spot().y, (float) p.spot().z);
			DropPod.path(q.flare(), q.seed(), t - q.delay(), b).add((float) q.spot().x, (float) q.spot().y, (float) q.spot().z);
			double side = Math.hypot(a.x - b.x, a.z - b.z) - CLEAR_SIDE, above = Math.abs(a.y - b.y) - CLEAR_ABOVE;
			worst = Math.min(worst, Math.max(side, above));
		}
		return worst;
	}
}
