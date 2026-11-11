package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A drop ship is solid (Justin, 6 November 2026: "when I fly into the ship, I'm just flying through it"; then "there's
 * still no collision on the ship"): its hull, gear, thrusters, stern door and platform stop players, mobs and dropped
 * items as blocks would, and when it is down with its leaf out the leaf is a ramp to walk up into the bay. The hull's
 * shell comes as quarter-block cells in the ship's frame from tools/crew_ship.py (data/jugcraft/crew_ship/collision.bin.gz).
 * While it is down (open, letting the crew off, closing, or set down to stay) those cells are turned into boxes in the
 * world for its heading once; while it flies (when a player is near), a coarser hull of whole-block cells is used, built
 * once for each 3 degrees of heading and kept (every ship shares them), and moved along with the ship (its tilt is
 * left out). Both sides do it (players move on their own side), and entities' movement takes the boxes as it takes
 * other entities' collision boxes (mixin/CrewShipCollisionMixin). Projectiles pass.
 */
public final class CrewShipHull {
	private static final String DATA = "/data/jugcraft/crew_ship/collision.bin.gz";
	/** The world's boxes are filed by this many blocks each way, to find the few near a mover quickly. */
	private static final int BIN = 4;
	/**
	 * The ships with a hull now, one set for each side (Justin, 7 November 2026: "the ship still doesn't have collision"):
	 * in single player the client's copy of a ship and the server's have the same network id, and the game counts two
	 * entities with the same id as equal, so in one set only whichever got there first was kept, the server's; the
	 * client's never was, and the player, who moves on the client, went straight through. Each side keeps its own.
	 */
	private static final Set<CrewShip> LANDED_SERVER = ConcurrentHashMap.newKeySet(), LANDED_CLIENT = ConcurrentHashMap.newKeySet();

	private static Set<CrewShip> landed(Level level) {
		return level.isClientSide() ? LANDED_CLIENT : LANDED_SERVER;
	}
	private static float cell;
	private static short @Nullable [] cells, coarse;
	private static boolean tried;
	/** The flying hull's cell (blocks), how finely its heading is kept (degrees), and how near a player must be. */
	private static final float COARSE = 1.0F;
	private static final int YAW_STEP = 3;
	private static final double FLY_NEAR = 96;
	/** The flying hulls built so far, by heading step (built relative to the ship's frame origin). */
	private static final Map<Integer, Shape> FLYING = new ConcurrentHashMap<>();

	private CrewShipHull() {
	}

	/** A landed ship's hull in the world: its boxes, filed by BIN-block cell, and what it was built for. */
	public static final class Shape {
		final AABB bounds;
		final Long2ObjectOpenHashMap<List<AABB>> bins;
		final double x, y, z;
		final float yaw, reach;
		final boolean leaf;
		/** The coarse flying hull. */
		boolean coarse;

		/** Where the boxes' origin is in the world now (the ship's frame origin), set each tick. */
		volatile double ox, oy, oz;

		Shape(AABB bounds, Long2ObjectOpenHashMap<List<AABB>> bins, double x, double y, double z, float yaw, float reach, boolean leaf) {
			this.bounds = bounds;
			this.bins = bins;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.reach = reach;
			this.leaf = leaf;
		}

		public int boxes() {
			Set<AABB> all = Collections.newSetFromMap(new IdentityHashMap<>());
			bins.values().forEach(all::addAll);
			return all.size();
		}
	}

	/** Whether the ship is down where it lands (its hull where its entity is). */
	public static boolean down(int phase) {
		return phase == CrewShipFlight.OPEN || phase == CrewShipFlight.UNLOAD || phase == CrewShipFlight.CLOSE || phase == CrewShipFlight.PARKED;
	}

	/**
	 * Each tick, on both sides: listed with its hull while it is down (the fine hull for its heading), or flying near a
	 * player (the coarse hull for its heading, where its frame is now); dropped when gone, or flying with nobody near.
	 */
	static void track(CrewShip ship) {
		int phase = ship.phase();
		if (ship.isRemoved() || phase == CrewShipFlight.GONE) {     // (a parked ship's phase, PARKED, is past GONE: it stays)
			untrack(ship);
			return;
		}
		if (down(phase)) {
			boolean leaf = phase == CrewShipFlight.UNLOAD || phase == CrewShipFlight.PARKED;
			Shape s = ship.hull;
			if (s == null || s.coarse || s.yaw != ship.shipYaw() || s.reach != ship.reach() || s.leaf != leaf) {
				s = build(0, 0, 0, ship.shipYaw(), ship.reach(), leaf, ship.roof(), false);
			}
			s.ox = ship.getX();
			s.oy = ship.getY();
			s.oz = ship.getZ();
			ship.hull = s;
			landed(ship.level()).add(ship);
			return;
		}
		// flying: the frame where the pose puts it now
		CrewShipFlight.Pose p = ship.pose(ship.level().getGameTime() - ship.phaseStart(), new CrewShipFlight.Pose());
		double fx = ship.getX() + p.x, fy = ship.getY() + p.y, fz = ship.getZ() + p.z;
		if (ship.level().getNearestPlayer(fx, fy, fz, FLY_NEAR, false) == null) {
			untrack(ship);
			return;
		}
		int step = Math.floorMod(Math.round(p.yaw * Mth.RAD_TO_DEG / YAW_STEP), 360 / YAW_STEP);
		Shape s = FLYING.computeIfAbsent(step, k -> build(0, 0, 0, k * YAW_STEP * Mth.DEG_TO_RAD, 0, false, true, true));
		Shape own = ship.hull;
		if (own == null || own.bins != s.bins) {
			own = new Shape(s.bounds, s.bins, 0, 0, 0, s.yaw, 0, false);
			own.coarse = true;
		}
		own.ox = fx;
		own.oy = fy;
		own.oz = fz;
		ship.hull = own;
		landed(ship.level()).add(ship);
	}

	static void untrack(CrewShip ship) {
		landed(ship.level()).remove(ship);
		ship.hull = null;
	}

	/** The landed ships' boxes {@code area} touches, added to the collision shapes an entity's move found. */
	public static List<VoxelShape> withHulls(Level level, @Nullable Entity mover, AABB area, List<VoxelShape> found) {
		Set<CrewShip> landed = landed(level);
		if (landed.isEmpty() || mover instanceof CrewShip || mover instanceof Projectile || mover != null && mover.noPhysics) {
			return found;
		}
		List<VoxelShape> out = null;
		for (CrewShip ship : landed) {
			Shape s = ship.hull;
			if (s == null || ship.level() != level || ship.isRemoved()) {
				continue;
			}
			double ox = s.ox, oy = s.oy, oz = s.oz;
			AABB rel = area.move(-ox, -oy, -oz);
			if (!s.bounds.intersects(rel)) {
				continue;
			}
			Set<AABB> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			int x0 = Mth.floor(rel.minX / BIN), x1 = Mth.floor(rel.maxX / BIN), y0 = Mth.floor(rel.minY / BIN), y1 = Mth.floor(rel.maxY / BIN),
					z0 = Mth.floor(rel.minZ / BIN), z1 = Mth.floor(rel.maxZ / BIN);
			for (int bx = x0; bx <= x1; bx++) {
				for (int by = y0; by <= y1; by++) {
					for (int bz = z0; bz <= z1; bz++) {
						List<AABB> boxes = s.bins.get(BlockPos.asLong(bx, by, bz));
						if (boxes == null) {
							continue;
						}
						for (AABB b : boxes) {
							if (b.intersects(rel) && seen.add(b)) {
								if (out == null) {
									out = new ArrayList<>(found);
								}
								out.add(Shapes.create(b.move(ox, oy, oz)));
							}
						}
					}
				}
			}
		}
		return out == null ? found : out;
	}

	/** The hull's boxes for a ship down at (x, y, z) facing yaw, its leaf reaching {@code reach} down (if it is down). */
	static Shape build(double x, double y, double z, float yaw, float reach, boolean leaf, boolean roof) {
		return build(x, y, z, yaw, reach, leaf, roof, false);
	}

	/** As above; {@code coarse}: the flying hull's whole-block cells. */
	static Shape build(double x, double y, double z, float yaw, float reach, boolean leaf, boolean roof, boolean coarse) {
		short[] c = coarse ? coarse() : cells();
		float k = coarse ? COARSE : cell;
		double cy = Math.cos(yaw), sy = Math.sin(yaw);
		LongOpenHashSet set = new LongOpenHashSet();
		if (c != null) {
			// each cell of the ship's frame, sampled at four points across it, to the world's cells (no gaps when turned)
			for (int i = 0; i < c.length; i += 3) {
				double py = (c[i + 1] + 0.5) * k;
				for (int s = 0; s < 4; s++) {
					double px = (c[i] + 0.25 + 0.5 * (s & 1)) * k, pz = (c[i + 2] + 0.25 + 0.5 * (s >> 1)) * k;
					set.add(key(x + px * cy + pz * sy, y + py, z - px * sy + pz * cy, k));
				}
			}
		}
		if (leaf && !roof) {
			// the ramp: from its hinge at the bay floor's aft edge down to its foot, half-block steps (a player walks up them)
			float drop = CrewShipFlight.leafDrop(reach);
			for (double t = 0; t <= CrewShipFlight.LEAF; t += k * 0.5) {
				double py = CrewShipFlight.rampY(drop, t) - 0.15, pz = CrewShipFlight.rampZ(drop, t);
				for (double px = -4.4; px <= 4.4; px += k * 0.5) {
					set.add(key(x + px * cy + pz * sy, y + py, z - px * sy + pz * cy, k));
				}
			}
		}
		// cells joined into boxes: runs along x, then runs of those alike along z; filed by bin
		Map<Long, List<int[]>> rows = new java.util.HashMap<>();
		set.forEach((long key) -> {
			int cx = BlockPos.getX(key), cyy = BlockPos.getY(key), cz = BlockPos.getZ(key);
			rows.computeIfAbsent(BlockPos.asLong(0, cyy, cz), r -> new ArrayList<>()).add(new int[] {cx, cyy, cz});
		});
		Map<List<Integer>, List<Integer>> runs = new java.util.HashMap<>();      // (x0, x1, y) -> the z of each run
		for (List<int[]> row : rows.values()) {
			row.sort((p, q) -> Integer.compare(p[0], q[0]));
			int start = row.get(0)[0], prev = start;
			for (int i = 1; i <= row.size(); i++) {
				if (i < row.size() && row.get(i)[0] == prev + 1) {
					prev = row.get(i)[0];
					continue;
				}
				runs.computeIfAbsent(List.of(start, prev, row.get(0)[1]), r -> new ArrayList<>()).add(row.get(0)[2]);
				if (i < row.size()) {
					start = prev = row.get(i)[0];
				}
			}
		}
		Long2ObjectOpenHashMap<List<AABB>> bins = new Long2ObjectOpenHashMap<>();
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (Map.Entry<List<Integer>, List<Integer>> e : runs.entrySet()) {
			int x0 = e.getKey().get(0), x1 = e.getKey().get(1), yy = e.getKey().get(2);
			List<Integer> zs = e.getValue();
			Collections.sort(zs);
			int z0 = zs.get(0), z1 = z0;
			for (int i = 1; i <= zs.size(); i++) {
				if (i < zs.size() && zs.get(i) == z1 + 1) {
					z1 = zs.get(i);
					continue;
				}
				AABB box = new AABB(x0 * k, yy * k, z0 * k, (x1 + 1) * k, (yy + 1) * k, (z1 + 1) * k);
				for (int bx = Mth.floor(box.minX / BIN); bx <= Mth.floor(box.maxX / BIN); bx++) {
					for (int bz = Mth.floor(box.minZ / BIN); bz <= Mth.floor(box.maxZ / BIN); bz++) {
						bins.computeIfAbsent(BlockPos.asLong(bx, Mth.floor(box.minY / BIN), bz), b -> new ArrayList<>()).add(box);
					}
				}
				minX = Math.min(minX, box.minX);
				minY = Math.min(minY, box.minY);
				minZ = Math.min(minZ, box.minZ);
				maxX = Math.max(maxX, box.maxX);
				maxY = Math.max(maxY, box.maxY);
				maxZ = Math.max(maxZ, box.maxZ);
				if (i < zs.size()) {
					z0 = z1 = zs.get(i);
				}
			}
		}
		AABB bounds = bins.isEmpty() ? new AABB(x, y, z, x, y, z) : new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		return new Shape(bounds, bins, x, y, z, yaw, reach, leaf);
	}

	private static long key(double x, double y, double z, float k) {
		return BlockPos.asLong(Mth.floor(x / k), Mth.floor(y / k), Mth.floor(z / k));
	}

	/** The shell's cells grouped into whole-block cells (x, y, z triples in blocks), for the flying hull. */
	static synchronized short @Nullable [] coarse() {
		if (coarse == null) {
			short[] c = cells();
			if (c == null) {
				return null;
			}
			LongOpenHashSet set = new LongOpenHashSet();
			for (int i = 0; i < c.length; i += 3) {
				set.add(BlockPos.asLong(Mth.floor(c[i] * cell / COARSE), Mth.floor(c[i + 1] * cell / COARSE), Mth.floor(c[i + 2] * cell / COARSE)));
			}
			short[] out = new short[set.size() * 3];
			int j = 0;
			for (long key : set) {
				out[j++] = (short) BlockPos.getX(key);
				out[j++] = (short) BlockPos.getY(key);
				out[j++] = (short) BlockPos.getZ(key);
			}
			coarse = out;
		}
		return coarse;
	}

	/** The hull's shell cells (x, y, z triples, the ship's frame), read once. */
	static synchronized short @Nullable [] cells() {
		if (!tried) {
			tried = true;
			try (InputStream raw = CrewShipHull.class.getResourceAsStream(DATA)) {
				if (raw == null) {
					Jugcraft.LOGGER.error("Missing the crew drop ship's hull {}", DATA);
				} else {
					try (DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
						cell = in.readFloat();
						short[] c = new short[in.readInt() * 3];
						for (int i = 0; i < c.length; i++) {
							c[i] = in.readShort();
						}
						cells = c;
					}
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the crew drop ship's hull {}", DATA, e);
			}
		}
		return cells;
	}

	/** Whether any landed ship's hull takes up part of {@code box} (the crew's path finder keeps out of it). */
	public static boolean blocks(Level level, AABB box) {
		return !landed(level).isEmpty() && !withHulls(level, null, box, List.of()).isEmpty();
	}

	/** Whether a ship's hull is solid at a point (for the game tests). */
	public static boolean solidAt(CrewShip ship, Vec3 at) {
		return ship.hull != null && !withHulls(ship.level(), null, new AABB(at, at).inflate(0.05), List.of()).isEmpty();
	}
}
