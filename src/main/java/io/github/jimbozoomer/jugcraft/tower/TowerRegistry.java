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
 * unloaded) is forgotten when checked. Older saves (one core per player) load as a list of one.
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

	/** Records {@code core} as one of {@code owner}'s towers in this dimension. */
	public void claim(UUID owner, BlockPos core) {
		List<Long> list = cores.computeIfAbsent(owner.toString(), key -> new ArrayList<>());
		if (!list.contains(core.asLong())) {
			list.add(core.asLong());
			setDirty();
		}
	}

	/** Forgets {@code core} (it was broken), whoever owned it. */
	public boolean needsChunk(ChunkPos chunk, BlockPos excluded) {
		for (List<Long> list : cores.values()) {
			for (long packed : list) {
				BlockPos core = BlockPos.of(packed);
				if (core.equals(excluded)) continue;
				int radius = TowerCoreBlockEntity.FOOTPRINT;
				ChunkPos min = ChunkPos.containing(core.offset(-radius, 0, -radius));
				ChunkPos max = ChunkPos.containing(core.offset(radius, 0, radius));
				if (chunk.x() >= min.x() && chunk.x() <= max.x() && chunk.z() >= min.z() && chunk.z() <= max.z()) return true;
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
