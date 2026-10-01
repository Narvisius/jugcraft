package io.github.jimbozoomer.jugcraft.tower;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Which Drone Tower Core each player owns in a dimension (docs/features/drone-tower.md): one per player per
 * dimension. Saved with the dimension ({@code data/jugcraft_towers.dat}), so it holds across unloaded chunks
 * and restarts. A registered core that is no longer a core (broken while unloaded) is forgotten when checked.
 */
public final class TowerRegistry extends SavedData {
	private static final Codec<Map<String, Long>> MAP = Codec.unboundedMap(Codec.STRING, Codec.LONG);
	static final Codec<TowerRegistry> CODEC = MAP.xmap(TowerRegistry::new, data -> data.cores);
	static final SavedDataType<TowerRegistry> TYPE = new SavedDataType<>(Jugcraft.id("towers"), TowerRegistry::new, CODEC, null);

	/** Owner UUID (as text) to the core's position. */
	private final Map<String, Long> cores;

	TowerRegistry() {
		this(Map.of());
	}

	TowerRegistry(Map<String, Long> cores) {
		this.cores = new HashMap<>(cores);
	}

	public static TowerRegistry get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The core {@code owner} owns in this dimension, or null. Forgets a core that is gone (when its chunk is loaded). */
	public @Nullable BlockPos coreOf(ServerLevel level, UUID owner) {
		Long packed = cores.get(owner.toString());
		if (packed == null) {
			return null;
		}
		BlockPos pos = BlockPos.of(packed);
		if (level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof TowerCoreBlockEntity)) {
			cores.remove(owner.toString());
			setDirty();
			return null;
		}
		return pos;
	}

	/** Records {@code core} as {@code owner}'s tower in this dimension. */
	public void claim(UUID owner, BlockPos core) {
		cores.put(owner.toString(), core.asLong());
		setDirty();
	}

	/** Forgets {@code core} (it was broken), whoever owned it. */
	public void release(BlockPos core) {
		if (cores.values().removeIf(packed -> packed == core.asLong())) {
			setDirty();
		}
	}
}
