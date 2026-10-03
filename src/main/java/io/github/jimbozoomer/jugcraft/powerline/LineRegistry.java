package io.github.jimbozoomer.jugcraft.powerline;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Every loaded power line structure in a level, on either side: its anchor, its kind, and which anchor each of
 * its invisible frame blocks belongs to. Filled as anchors load and emptied as they unload, so it costs nothing to
 * keep and answers "what structure is this frame part of" and "which structures are near here" at once.
 */
public final class LineRegistry {
	private static final Map<Level, LineRegistry> LEVELS = new WeakHashMap<>();

	private final Map<BlockPos, LineKind> anchors = new HashMap<>();
	private final Map<BlockPos, BlockPos> frames = new HashMap<>();

	public static LineRegistry of(Level level) {
		synchronized (LEVELS) {
			return LEVELS.computeIfAbsent(level, l -> new LineRegistry());
		}
	}

	synchronized void add(BlockPos anchor, LineKind kind) {
		LineKind before = anchors.put(anchor.immutable(), kind);
		if (before != null && before != kind) {
			for (BlockPos frame : before.frames()) {
				frames.remove(anchor.offset(frame), anchor);
			}
		}
		for (BlockPos frame : kind.frames()) {
			frames.put(anchor.offset(frame), anchor.immutable());
		}
	}

	/** Forgets a structure (only if it is still this kind there: an upgrade may already have put its successor in). */
	synchronized void remove(BlockPos anchor, LineKind kind) {
		if (anchors.remove(anchor, kind)) {
			for (BlockPos frame : kind.frames()) {
				frames.remove(anchor.offset(frame), anchor);
			}
		}
	}

	/** The anchor a frame block (or the anchor itself) belongs to. */
	public synchronized @Nullable BlockPos anchorOf(BlockPos pos) {
		if (anchors.containsKey(pos)) {
			return pos;
		}
		return frames.get(pos);
	}

	public synchronized @Nullable LineKind kindAt(BlockPos anchor) {
		return anchors.get(anchor);
	}

	/** A copy of every loaded anchor and its kind. */
	public synchronized Map<BlockPos, LineKind> anchors() {
		return Map.copyOf(anchors);
	}
}
