package io.github.jimbozoomer.jugcraft.tower.crew;

import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Where a crew drop ship sets down for the Tri-Pillar (the drop ship sheet, take 4): the nearest flat, clear spot
 * within {@link #REACH} blocks of the tower's gates big enough for it and its ramp, its stern toward the gates; or,
 * if there is none, a hover off the tier-1 roof's south edge with the ramp run out over the rail onto the deck, the
 * crew going in by the south pillar's maintenance door and down its caged ladder.
 */
public final class CrewShipSite {
	/** How far from the gates it looks (blocks). */
	public static final int REACH = 100;
	/** The gates' front (from the core) and the tower's footprint (from the core: x, z from-to), kept clear. */
	public static final BlockPos GATES = new BlockPos(0, 1, -56);
	public static final int TOWER_X = 56, TOWER_Z0 = -58, TOWER_Z1 = 50;
	/** The ship's footprint to check (its frame: x across, z from the nose to past the ramp's end) and the step. */
	static final float HALF_WIDTH = 16.5F, NOSE = -25.5F, TAIL = 33.5F, STEP = 2.5F;
	/** At most this much higher or lower than the gates. */
	static final int RISE = 8;
	/** The roof hover (from the core: the ship's origin, facing south, stern to the tower), the leaf's drop onto the deck
	 * and where the crew make for (outside the south pillar's maintenance door). */
	public static final Vec3 ROOF_HOVER = new Vec3(10.0, 20.7, 73.3);
	public static final float ROOF_REACH = 2.63F;
	public static final BlockPos ROOF_DOOR = new BlockPos(10, 21, 20);

	/** A landing: the ship's origin (its ground under the gear, or its hover), the way it faces, at the roof or not,
	 * its leaf's drop, and where the crew walk to first. */
	public record Site(Vec3 land, float yaw, boolean roof, float reach, Vec3 entrance) {
	}

	private CrewShipSite() {
	}

	/** Where the ship goes for the tower at {@code core} (built to {@code tier}); null if nowhere (no roof yet). */
	public static @Nullable Site find(ServerLevel level, BlockPos core, int tier) {
		Vec3 gates = Vec3.atBottomCenterOf(core.offset(GATES));
		int y0 = core.getY() + GATES.getY();
		for (int r = 24; r <= REACH; r += 4) {
			int n = Math.max(8, (int) (2 * Math.PI * r / 6));
			for (int k = 0; k < n; k++) {
				// out in front of the gates first (north), then round both ways
				int j = (k + 1) / 2 * (k % 2 == 0 ? 1 : -1);
				double a = 2 * Math.PI * j / n;
				double cx = gates.x + Math.sin(a) * r, cz = gates.z - Math.cos(a) * r;
				Site s = check(level, core, gates, cx, cz, y0);
				if (s != null) {
					return s;
				}
			}
		}
		return tier >= 1 ? roof(core) : null;
	}

	/** The roof hover for the tower at {@code core}. */
	public static Site roof(BlockPos core) {
		Vec3 land = ROOF_HOVER.add(core.getX(), core.getY(), core.getZ());
		return new Site(land, Mth.PI, true, ROOF_REACH, Vec3.atBottomCenterOf(core.offset(ROOF_DOOR)));
	}

	/** How far out from a drop squad (blocks, its middle from them) a pickup looks (tower/squad/SquadOrders). */
	public static final int PICKUP_NEAR = 36, PICKUP_REACH = 96;

	/**
	 * Where a ship picking up a drop squad at {@code at} sets down (release (b) of the drop squads): the nearest flat, clear
	 * spot from {@value #PICKUP_NEAR} to {@code reach} blocks out, its stern (and ramp) toward them, off any Drone Tower;
	 * null if there's none.
	 */
	public static @Nullable Site near(ServerLevel level, Vec3 at, int reach) {
		int y0 = Mth.floor(at.y);
		for (int r = PICKUP_NEAR; r <= reach; r += 4) {
			int n = Math.max(8, (int) (2 * Math.PI * r / 6));
			for (int k = 0; k < n; k++) {
				double a = 2 * Math.PI * k / n;
				Site s = check(level, null, at, at.x + Math.sin(a) * r, at.z - Math.cos(a) * r, y0);
				if (s != null) {
					return s;
				}
			}
		}
		return null;
	}

	/** The ship centred at (cx, cz), stern to the gates, if it fits there: flat ground within a block, no leaves,
	 * no water, clear of the tower ({@code core}'s; with none, of every Drone Tower), not far above or below the gates,
	 * all loaded. */
	static @Nullable Site check(ServerLevel level, @Nullable BlockPos core, Vec3 gates, double cx, double cz, int y0) {
		double dx = gates.x - cx, dz = gates.z - cz;
		float yaw = (float) Math.atan2(dx, dz);
		double c = Math.cos(yaw), s = Math.sin(yaw);
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (float px = -HALF_WIDTH; px <= HALF_WIDTH + 0.01F; px += STEP) {
			for (float pz = NOSE; pz <= TAIL + 0.01F; pz += STEP) {
				int x = Mth.floor(cx + px * c + pz * s), z = Mth.floor(cz - px * s + pz * c);
				if (core != null) {
					int rx = x - core.getX(), rz = z - core.getZ();
					if (Math.abs(rx) <= TOWER_X && rz >= TOWER_Z0 && rz <= TOWER_Z1) {
						return null;
					}
				}
				if (!level.hasChunk(x >> 4, z >> 4)) {
					return null;
				}
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				if (core == null && io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level).inTower(level, new BlockPos(x, top, z))) {
					return null;
				}
				if (top != level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)) {
					return null;
				}
				m.set(x, top - 1, z);
				if (!level.getFluidState(m).isEmpty()) {
					return null;
				}
				lo = Math.min(lo, top);
				hi = Math.max(hi, top);
				if (hi - lo > 1 || Math.abs(top - y0) > RISE) {
					return null;
				}
			}
		}
		// the leaf reaches down to the ground where its end comes (the ground there may be a block lower)
		int fx = Mth.floor(cx + CrewShipFlight.FOOT * s), fz = Mth.floor(cz + CrewShipFlight.FOOT * c);
		int foot = level.getHeight(Heightmap.Types.MOTION_BLOCKING, fx, fz);
		Vec3 land = new Vec3(cx, hi, cz);
		return new Site(land, yaw, false, CrewShipFlight.HINGE_Y + (hi - foot), gates);
	}
}
