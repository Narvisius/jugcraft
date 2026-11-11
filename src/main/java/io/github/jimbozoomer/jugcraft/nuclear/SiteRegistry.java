package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where each compiled site stands (towers and pump houses build): its anchor's place and facing, so a structure block
 * can tell which site and which of its cells it is (its look, its shape). The anchors' block entities put themselves in
 * when their chunk loads and take themselves out when it unloads, on each side; the client's chunk builder reads its
 * own list, which only changes on the main thread.
 *
 * <p>A site's frame turns with its anchor's facing: south is the site as compiled; west, north and east turn it a
 * quarter, a half and three quarters clockwise seen from above (as a blueprint turns its blocks).
 */
public final class SiteRegistry {
	private static final Map<Level, List<Placement>> LEVELS = new WeakHashMap<>();
	/** The client's placements (its one level), read by the chunk builder's threads. */
	private static volatile List<Placement> client = List.of();

	private SiteRegistry() {
	}

	/** A site in the world: its anchor block, facing, data, and its extent in the world. */
	public record Placement(BlockPos anchor, Direction facing, SiteData data, AABB bounds) {
		/** The site cell a world block is (site coordinates). */
		public BlockPos local(BlockPos world) {
			int dx = world.getX() - anchor.getX(), dy = world.getY() - anchor.getY(), dz = world.getZ() - anchor.getZ();
			int lx, lz;
			switch (facing) {
				case WEST -> {
					lx = dz;
					lz = -dx;
				}
				case NORTH -> {
					lx = -dx;
					lz = -dz;
				}
				case EAST -> {
					lx = -dz;
					lz = dx;
				}
				default -> {
					lx = dx;
					lz = dz;
				}
			}
			return new BlockPos(data.ax + lx, data.ay + dy, data.az + lz);
		}

		/** The world block a site cell is. */
		public BlockPos world(int x, int y, int z) {
			int lx = x - data.ax, ly = y - data.ay, lz = z - data.az;
			int wx, wz;
			switch (facing) {
				case WEST -> {
					wx = -lz;
					wz = lx;
				}
				case NORTH -> {
					wx = -lx;
					wz = -lz;
				}
				case EAST -> {
					wx = lz;
					wz = -lx;
				}
				default -> {
					wx = lx;
					wz = lz;
				}
			}
			return anchor.offset(wx, ly, wz);
		}

		/** A point in the site's frame from a world point (a click). */
		public Vec3 localPoint(Vec3 world) {
			// About the anchor block's middle, then back to the anchor cell's corner.
			double dx = world.x - anchor.getX() - 0.5, dy = world.y - anchor.getY(), dz = world.z - anchor.getZ() - 0.5;
			double[] l = turnBack(dx, dz);
			return new Vec3(data.ax + 0.5 + l[0], data.ay + dy, data.az + 0.5 + l[1]);
		}

		private double[] turnBack(double dx, double dz) {
			return switch (facing) {
				case WEST -> new double[] {dz, -dx};
				case NORTH -> new double[] {-dx, -dz};
				case EAST -> new double[] {-dz, dx};
				default -> new double[] {dx, dz};
			};
		}
	}

	/** A world block's site and cell. */
	public record Hit(Placement placement, int x, int y, int z) {
	}

	private static AABB bounds(BlockPos anchor, Direction facing, SiteData data) {
		Placement p = new Placement(anchor, facing, data, new AABB(anchor));
		BlockPos a = p.world(data.minX, data.minY, data.minZ), b = p.world(data.maxX, data.maxY, data.maxZ);
		return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
	}

	/** Puts a site in (its anchor's chunk loaded); returns its placement, or null if the site has no data. */
	public static @Nullable Placement add(Level level, BlockPos anchor, Direction facing, String key) {
		SiteData data = SiteData.get(key);
		if (data == null) {
			return null;
		}
		Placement p = new Placement(anchor.immutable(), facing, data, bounds(anchor, facing, data));
		if (level.isClientSide()) {
			List<Placement> next = new ArrayList<>(client);
			next.removeIf(o -> o.anchor().equals(p.anchor()));
			next.add(p);
			client = List.copyOf(next);
		} else {
			synchronized (LEVELS) {
				List<Placement> list = LEVELS.computeIfAbsent(level, l -> new ArrayList<>());
				list.removeIf(o -> o.anchor().equals(p.anchor()));
				list.add(p);
			}
		}
		return p;
	}

	/** Takes a site out (its anchor unloaded or gone); returns what it was, or null. */
	public static @Nullable Placement remove(Level level, BlockPos anchor) {
		Placement was = null;
		if (level.isClientSide()) {
			List<Placement> next = new ArrayList<>(client);
			for (Placement p : client) {
				if (p.anchor().equals(anchor)) was = p;
			}
			next.removeIf(o -> o.anchor().equals(anchor));
			client = List.copyOf(next);
		} else {
			synchronized (LEVELS) {
				List<Placement> list = LEVELS.get(level);
				if (list != null) {
					for (Placement p : list) {
						if (p.anchor().equals(anchor)) was = p;
					}
					list.removeIf(o -> o.anchor().equals(anchor));
				}
			}
		}
		return was;
	}

	/** The server's placements of one site in a level (its anchors' chunks loaded). */
	public static List<Placement> placements(Level level, String key) {
		synchronized (LEVELS) {
			List<Placement> l = LEVELS.get(level);
			if (l == null) return List.of();
			List<Placement> out = new ArrayList<>();
			for (Placement p : l) {
				if (p.data().key.equals(key)) out.add(p);
			}
			return out;
		}
	}

	/** The site an anchor has in a level now (the client's list on the client), or null. */
	public static @Nullable Placement at(Level level, BlockPos anchor) {
		if (level.isClientSide()) {
			for (Placement p : client) {
				if (p.anchor().equals(anchor)) return p;
			}
			return null;
		}
		synchronized (LEVELS) {
			List<Placement> l = LEVELS.get(level);
			if (l != null) {
				for (Placement p : l) {
					if (p.anchor().equals(anchor)) return p;
				}
			}
		}
		return null;
	}

	/** The client changed level: its sites start over. */
	public static void clearClient() {
		client = List.of();
	}

	/**
	 * The site a world block belongs to. A level finds its own; anything else (the client's chunk builder, a
	 * pathfinding region) is taken to be the client's world on the client and nothing on the server.
	 */
	public static @Nullable Hit find(@Nullable BlockGetter getter, BlockPos pos) {
		List<Placement> list;
		if (getter instanceof Level level && !level.isClientSide()) {
			synchronized (LEVELS) {
				List<Placement> l = LEVELS.get(level);
				list = l == null ? List.of() : List.copyOf(l);
			}
		} else if (getter instanceof Level || getter == null || onClientThreads()) {
			list = client;
		} else {
			return null;
		}
		Placement best = null;
		BlockPos bestLocal = null;
		for (Placement p : list) {
			if (!p.bounds().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				continue;
			}
			BlockPos l = p.local(pos);
			if (p.data().cell(l.getX(), l.getY(), l.getZ()) >= 0 || p.data().shell != null && p.data().shell.has(l.getX(), l.getY(), l.getZ())) {
				best = p;
				bestLocal = l;
				break;
			}
			if (best == null) {
				best = p;
				bestLocal = l;
			}
		}
		return best == null ? null : new Hit(best, bestLocal.getX(), bestLocal.getY(), bestLocal.getZ());
	}

	/** Whether this thread belongs to the client (its main thread or a chunk builder), as far as can be told. */
	private static boolean onClientThreads() {
		return CLIENT_SIDE;
	}

	/** Set by the client's initializer: lookups from anything but a server level use the client's list. */
	public static volatile boolean CLIENT_SIDE = false;

	/** The anchor block entity a placement belongs to, if loaded. */
	public static @Nullable BlockEntity anchorOf(Level level, Placement p) {
		return level.isLoaded(p.anchor()) ? level.getBlockEntity(p.anchor()) : null;
	}
}
