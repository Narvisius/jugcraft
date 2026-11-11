package io.github.jimbozoomer.jugcraft.tower;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Tower Cores each player owns in a dimension (docs/features/drone-tower.md). A player may have several
 * towers in a dimension, but each must stand outside the build radius of their others
 * ({@link TowerCoreBlockEntity#BUILD_RADIUS_CHUNKS}). Saved with the dimension ({@code data/jugcraft_towers.dat}),
 * so it holds across unloaded chunks and restarts. A registered core that is no longer a core (broken while
 * unloaded) is forgotten when checked. Older saves (one core per player) load as a list of one. It also says where the
 * towers stand, so nothing spawns in or on them ({@link #inTower}).
 */
public final class TowerRegistry extends SavedData {
	private static final Codec<Map<String, List<Long>>> MAP = Codec.withAlternative(
			Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(old -> {
				Map<String, List<Long>> out = new HashMap<>();
				old.forEach((owner, core) -> out.put(owner, List.of(core)));
				return out;
			}, data -> Map.of()));
	static final Codec<TowerRegistry> CODEC = MAP.xmap(TowerRegistry::new, data -> data.cores);
	static final SavedDataType<TowerRegistry> TYPE = new SavedDataType<>(Jugcraft.id("towers"), TowerRegistry::new, CODEC, null);

	/** Owner UUID (as text) to their cores' positions. */
	private final Map<String, List<Long>> cores;

	TowerRegistry() {
		this(Map.of());
	}

	TowerRegistry(Map<String, List<Long>> cores) {
		this.cores = new HashMap<>();
		cores.forEach((owner, list) -> this.cores.put(owner, new ArrayList<>(list)));
	}

	public static TowerRegistry get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Every tower core in this dimension (for tower/crew/TowerCrewCare). */
	public List<BlockPos> allCores() {
		List<BlockPos> out = new ArrayList<>();
		for (List<Long> list : cores.values()) {
			for (long packed : list) {
				out.add(BlockPos.of(packed));
			}
		}
		return out;
	}

	/** The cores {@code owner} owns in this dimension. Forgets cores that are gone (when their chunk is loaded). */
	public List<BlockPos> coresOf(ServerLevel level, UUID owner) {
		List<Long> list = cores.get(owner.toString());
		if (list == null) {
			return List.of();
		}
		if (list.removeIf(packed -> level.isLoaded(BlockPos.of(packed)) && !(level.getBlockEntity(BlockPos.of(packed)) instanceof TowerCoreBlockEntity))) {
			setDirty();
		}
		return list.stream().map(BlockPos::of).toList();
	}

	/**
	 * One of {@code owner}'s other towers whose build radius {@code pos} is inside (in chunks, each way), or null
	 * when a new core may go there.
	 */
	public @Nullable BlockPos coreCovering(ServerLevel level, UUID owner, BlockPos pos) {
		ChunkPos chunk = ChunkPos.containing(pos);
		for (BlockPos core : coresOf(level, owner)) {
			if (!core.equals(pos) && TowerCoreBlockEntity.chunkDistance(ChunkPos.containing(core), chunk) <= TowerCoreBlockEntity.BUILD_RADIUS_CHUNKS) {
				return core;
			}
		}
		return null;
	}

	/** Records {@code core} as one of {@code owner}'s towers in this dimension (and nobody else's). */
	public void claim(UUID owner, BlockPos core) {
		track(owner, core);
	}

	/** The key a core nobody owns yet is listed under (never a player's UUID). */
	static final String NOBODY = "nobody";

	/**
	 * Makes sure {@code core} is listed, under its owner or, if it has none yet, under nobody (Justin, 6 November 2026:
	 * "no tower with a crew near ... Is it just not detecting the one that's already placed?"). A core set some other way
	 * than a player placing it (built by a command, or in a world from before the list) was never listed, so its crew
	 * never came, mobs still spawned in it and /crewship couldn't find it; each core now lists itself when it loads.
	 */
	public void track(@Nullable UUID owner, BlockPos core) {
		String key = owner == null ? NOBODY : owner.toString();
		long packed = core.asLong();
		List<Long> mine = cores.get(key);
		if (mine != null && mine.contains(packed)) {
			return;
		}
		for (List<Long> list : cores.values()) {
			list.removeIf(p -> p == packed);
		}
		cores.values().removeIf(List::isEmpty);
		cores.computeIfAbsent(key, k -> new ArrayList<>()).add(packed);
		setDirty();
	}

	/** Forgets {@code core} (it was broken), whoever owned it. */
	public boolean needsChunk(ServerLevel level, ChunkPos chunk, BlockPos excluded) {
		for (List<Long> list : cores.values()) {
			for (long packed : list) {
				BlockPos core = BlockPos.of(packed);
				if (core.equals(excluded)) continue;
				int radius = TowerCoreBlockEntity.footprintRadius(level, core);
				ChunkPos min = ChunkPos.containing(core.offset(-radius, 0, -radius));
				ChunkPos max = ChunkPos.containing(core.offset(radius, 0, radius));
				if (chunk.x() >= min.x() && chunk.x() <= max.x() && chunk.z() >= min.z() && chunk.z() <= max.z()) return true;
			}
		}
		return false;
	}

	/** Blocks over a column's top that still count as on the tower (a mob stands on the roof, its head over it). */
	static final int OVER_TOP = 3;

	/**
	 * Whether {@code pos} is in or on one of this dimension's Drone Towers: over a column of the finished tower's plan
	 * ({@link TowerData.Extent}, the current design), from its foot to {@link #OVER_TOP} blocks over that column's top.
	 * Nothing spawns there of itself (Justin, 5 November 2026: "make it so mobs do not spawn on/in the tower";
	 * mixin/TowerSpawnMixin). A core that is gone (its chunk loaded, no core there) counts no more.
	 */
	public boolean inTower(ServerLevel level, BlockPos pos) {
		if (cores.isEmpty()) {
			return false;
		}
		TowerData.Extent extent = TowerData.get().extent();
		for (List<Long> list : cores.values()) {
			for (long packed : list) {
				int x = pos.getX() - BlockPos.getX(packed), z = pos.getZ() - BlockPos.getZ(packed), y = pos.getY() - BlockPos.getY(packed);
				int top = extent.top(x, z);
				if (top == Integer.MIN_VALUE || y < extent.minY() - 1 || y > top + OVER_TOP) {
					continue;
				}
				BlockPos core = BlockPos.of(packed);
				if (level.isLoaded(core) && !(level.getBlockEntity(core) instanceof TowerCoreBlockEntity)) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	public void release(BlockPos core) {
		boolean changed = false;
		for (List<Long> list : cores.values()) {
			changed |= list.removeIf(packed -> packed == core.asLong());
		}
		if (changed) {
			cores.values().removeIf(List::isEmpty);
			setDirty();
		}
	}
}
