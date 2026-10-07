package io.github.jimbozoomer.jugcraft.styx;

import io.github.jimbozoomer.jugcraft.town.TownBuilder;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** A fixed layout-3 district east of the original town, built by its existing loaded-chunk queue.
 * The old town asset, centre, NPC indexes and saved chunk keys do not move. */
public final class StyxTownDistrict {
	public static final int LAYOUT = 3;
	public static final int X = 192, Z = 52, WIDTH = 67, DEPTH = 49, HEIGHT = 63;
	public static final int MAX_X = X + WIDTH - 1;
	public static final int ROAD_X = 175, ROAD_Z_MIN = 94, ROAD_Z_MAX = 98;
	private static final Map<Integer, List<StyxConservatory.Placement>> COLUMNS = new HashMap<>();
	private StyxTownDistrict() { }

	public static BlockPos home(BlockPos town) { return town.offset(X, 0, Z); }

	public static boolean contains(int x, int z) {
		return x >= X && x <= MAX_X && z >= Z && z < Z + DEPTH
				|| x >= ROAD_X && x < X && z >= ROAD_Z_MIN && z <= ROAD_Z_MAX;
	}

	public static boolean overlaps(BlockPos origin, ChunkPos chunk) {
		int x = (chunk.x() << 4) - origin.getX(), z = (chunk.z() << 4) - origin.getZ();
		return x + 15 >= X && x <= MAX_X && z + 15 >= Z && z < Z + DEPTH
				|| x + 15 >= ROAD_X && x < X && z + 15 >= ROAD_Z_MIN && z <= ROAD_Z_MAX;
	}

	public static boolean canReserve(ServerLevel level, BlockPos origin) {
		return StyxState.get(level).origin.isEmpty() && origin.getY() >= level.getMinY()
				&& origin.getY() + HEIGHT < level.getMaxY();
	}

	/** Reserves the one resident before chunks build. Also repairs a save interrupted between the two saved-data writes. */
	public static void reserve(ServerLevel level) {
		TownState town = TownState.get(level);
		if (!town.styxDistrict() || town.origin() == null) return;
		StyxState state = StyxState.get(level);
		if (state.origin.isPresent()) return;
		state.origin = Optional.of(home(town.origin()));
		state.layout = LAYOUT;
		state.townHome = true;
		state.placed = 0;
		state.setDirty();
	}

	/** Construction completion uses saved chunk progress, never an unloaded-entity lookup or a forced load. */
	public static boolean ready(ServerLevel level, BlockPos origin) {
		TownState town = TownState.get(level);
		if (!town.styxDistrict() || town.origin() == null || !home(town.origin()).equals(origin)) return false;
		for (int cx = (town.origin().getX() + ROAD_X) >> 4; cx <= (origin.getX() + WIDTH - 1) >> 4; cx++) {
			for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + DEPTH - 1) >> 4; cz++) {
				ChunkPos chunk = new ChunkPos(cx, cz);
				if (overlaps(town.origin(), chunk) && !town.built(chunk.pack())) return false;
			}
		}
		return true;
	}

	private static List<StyxConservatory.Placement> column(int x, int z) {
		if (COLUMNS.isEmpty()) {
			for (var block : StyxConservatory.placements(LAYOUT)) {
				COLUMNS.computeIfAbsent(block.offset().getZ() * WIDTH + block.offset().getX(), key -> new ArrayList<>()).add(block);
			}
		}
		return COLUMNS.getOrDefault(z * WIDTH + x, List.of());
	}

	/** Called only within TownBuilder's one-loaded-chunk-per-tick budget, once per saved chunk. */
	public static void buildColumn(ServerLevel level, LevelChunk chunk, BlockPos town, int x, int z) {
		int wx = town.getX() + x, wz = town.getZ() + z, floor = town.getY();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		boolean estate = x >= X;
		int top = Math.max(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, wx & 15, wz & 15), floor + (estate ? HEIGHT - 1 : 4));
		for (int y = floor + 1; y <= top; y++) {
			// The connector begins under the last gate roof overhang; preserve authored gate blocks overhead.
			int gate = estate ? TownData.KEEP : TownData.get().index(x, y - floor, z);
			TownBuilder.set(level, at.set(wx, y, wz), gate > TownData.AIR ? TownData.get().state(gate) : Blocks.AIR.defaultBlockState());
		}
		for (int y = floor - 1; y >= Math.max(level.getMinY(), floor - TownBuilder.FILL_DEPTH); y--) {
			var current = level.getBlockState(at.set(wx, y, wz));
			if (!current.isAir() && current.getFluidState().isEmpty() && !current.canBeReplaced()) break;
			TownBuilder.set(level, at, y >= floor - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
		}
		TownBuilder.set(level, at.set(wx, floor, wz), estate ? Blocks.GRASS_BLOCK.defaultBlockState()
				: (z == ROAD_Z_MIN || z == ROAD_Z_MAX ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState());
		if (estate) for (var block : column(x - X, z - Z)) {
			TownBuilder.set(level, home(town).offset(block.offset()), block.state());
		}
		// Join the town's five-wide road to the existing three-wide garden walk, clear of its lamp posts.
		if (estate && x < X + 10 && z >= Z + 44 && z <= Z + 46) {
			TownBuilder.set(level, at.set(wx, floor, wz), Blocks.COBBLESTONE.defaultBlockState());
		}
	}
}
