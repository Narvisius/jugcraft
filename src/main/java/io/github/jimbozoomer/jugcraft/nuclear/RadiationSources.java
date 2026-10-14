package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Radioactive blocks in a level: what a block entity registers while it is loaded (a reactor open for refuelling,
 * stored spent fuel, later corium and fallout). Strength is the dose rate one block away, in mSv/h.
 */
public final class RadiationSources {
	private static final Map<Level, Map<BlockPos, Source>> LEVELS = new WeakHashMap<>();

	public record Source(BlockPos pos, double strength, RadiationKind kind) {
	}

	private RadiationSources() {
	}

	public static synchronized void set(Level level, BlockPos pos, double strength, RadiationKind kind) {
		Map<BlockPos, Source> sources = LEVELS.computeIfAbsent(level, l -> new HashMap<>());
		if (strength <= 0) {
			sources.remove(pos);
		} else {
			sources.put(pos.immutable(), new Source(pos.immutable(), strength, kind));
		}
	}

	public static void clear(Level level, BlockPos pos) {
		set(level, pos, 0, RadiationKind.NUCLEAR);
	}

	/** A copy of the level's sources. */
	public static synchronized Iterable<Source> of(Level level) {
		Map<BlockPos, Source> sources = LEVELS.get(level);
		return sources == null ? java.util.List.of() : java.util.List.copyOf(sources.values());
	}
}
