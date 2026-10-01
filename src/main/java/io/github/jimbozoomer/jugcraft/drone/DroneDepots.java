package io.github.jimbozoomer.jugcraft.drone;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Loaded drone terminals per level, so platform blocks can tell nearby terminals to rescan and drone
 * items can find the depot a pad belongs to. Only loaded terminals are listed; nothing scans the
 * world.
 */
public final class DroneDepots {
	/** Platform changes within this horizontal distance of a terminal mark it for a rescan. */
	public static final int NOTIFY_RANGE = 72;
	private static final Map<Level, Set<BlockPos>> TERMINALS = new WeakHashMap<>();

	private DroneDepots() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TERMINALS.clear());
	}

	static void add(Level level, BlockPos pos) {
		TERMINALS.computeIfAbsent(level, key -> new HashSet<>()).add(pos.immutable());
	}

	static void remove(Level level, BlockPos pos) {
		Set<BlockPos> set = TERMINALS.get(level);
		if (set != null) {
			set.remove(pos);
		}
	}

	/** The loaded depot terminal nearest {@code pos} within {@code range} blocks, or null (for the tower's exchange ports). */
	public static @org.jspecify.annotations.Nullable DroneTerminalBlockEntity nearest(Level level, BlockPos pos, int range) {
		DroneTerminalBlockEntity best = null;
		double bestDistance = (double) range * range;
		for (BlockPos at : List.copyOf(terminals(level))) {
			double distance = at.distSqr(pos);
			if (distance <= bestDistance && level.isLoaded(at) && level.getBlockEntity(at) instanceof DroneTerminalBlockEntity terminal) {
				best = terminal;
				bestDistance = distance;
			}
		}
		return best;
	}

	static Set<BlockPos> terminals(Level level) {
		return TERMINALS.getOrDefault(level, Collections.emptySet());
	}

	/** A platform or pad block at {@code pos} changed: nearby terminals rescan (at most once a second). */
	static void platformChanged(Level level, BlockPos pos) {
		for (BlockPos terminal : Set.copyOf(terminals(level))) {
			if (Math.abs(terminal.getX() - pos.getX()) <= NOTIFY_RANGE && Math.abs(terminal.getZ() - pos.getZ()) <= NOTIFY_RANGE
					&& level.getBlockEntity(terminal) instanceof DroneTerminalBlockEntity entity) {
				entity.markPlatformDirty();
			}
		}
	}

	/** The loaded terminal whose platform contains the pad block at {@code pos}, if any. */
	static @Nullable DroneTerminalBlockEntity depotForPad(Level level, BlockPos pos) {
		for (BlockPos terminal : Set.copyOf(terminals(level))) {
			if (level.getBlockEntity(terminal) instanceof DroneTerminalBlockEntity entity && entity.platformContains(pos)) {
				return entity;
			}
		}
		return null;
	}
}
