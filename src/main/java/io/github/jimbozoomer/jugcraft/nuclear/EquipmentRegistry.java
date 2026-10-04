package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Every loaded piece of nuclear equipment in a level, on either side: its anchor, kind and facing, and which anchor
 * each of its invisible frame blocks belongs to. Filled as anchors load and emptied as they unload. Its version goes
 * up whenever equipment or a nuclear pipe comes or goes, so reactor units know to look at themselves again.
 */
public final class EquipmentRegistry {
	private static final Map<Level, EquipmentRegistry> LEVELS = new WeakHashMap<>();

	public record Entry(EquipmentKind kind, Direction facing) {
	}

	private final Map<BlockPos, Entry> anchors = new HashMap<>();
	private final Map<BlockPos, BlockPos> frames = new HashMap<>();
	private int version;

	public static EquipmentRegistry of(Level level) {
		synchronized (LEVELS) {
			return LEVELS.computeIfAbsent(level, l -> new EquipmentRegistry());
		}
	}

	synchronized void add(BlockPos anchor, EquipmentKind kind, Direction facing) {
		Entry before = anchors.put(anchor.immutable(), new Entry(kind, facing));
		if (before != null) {
			for (BlockPos frame : before.kind().frames(before.facing())) {
				frames.remove(anchor.offset(frame), anchor);
			}
		}
		for (BlockPos frame : kind.frames(facing)) {
			frames.put(anchor.offset(frame), anchor.immutable());
		}
		version++;
	}

	synchronized void remove(BlockPos anchor) {
		Entry entry = anchors.remove(anchor);
		if (entry != null) {
			for (BlockPos frame : entry.kind().frames(entry.facing())) {
				frames.remove(anchor.offset(frame), anchor);
			}
		}
		version++;
	}

	/** Something nuclear changed (a pipe placed or broken). */
	public synchronized void touch() {
		version++;
	}

	public synchronized int version() {
		return version;
	}

	/** The anchor a frame block (or the anchor itself) belongs to. */
	public synchronized @Nullable BlockPos anchorOf(BlockPos pos) {
		if (anchors.containsKey(pos)) {
			return pos;
		}
		return frames.get(pos);
	}

	public synchronized @Nullable Entry at(BlockPos anchor) {
		return anchors.get(anchor);
	}

	public synchronized Map<BlockPos, Entry> anchors() {
		return Map.copyOf(anchors);
	}
}
