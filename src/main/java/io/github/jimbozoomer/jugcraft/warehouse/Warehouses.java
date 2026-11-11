package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The loaded warehouse terminals of each level, so a warehouse block placed or broken can tell the terminals near it
 * to find their warehouse again (TerminalBlockEntity.markChanged; they look on their next tick, once however many
 * blocks changed).
 */
public final class Warehouses {
	/** How far from a terminal a change can matter (its search reaches TerminalBlockEntity.REACH). */
	private static final int NEAR = TerminalBlockEntity.REACH + 8;
	private static final Map<Level, Set<BlockPos>> TERMINALS = new WeakHashMap<>();

	private Warehouses() {
	}

	static void add(Level level, BlockPos pos) {
		TERMINALS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
	}

	static void remove(Level level, BlockPos pos) {
		Set<BlockPos> set = TERMINALS.get(level);
		if (set != null) {
			set.remove(pos);
		}
	}

	/** A warehouse block changed at pos: the terminals near it look again. */
	public static void changed(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}
		Set<BlockPos> set = TERMINALS.get(level);
		if (set == null) {
			return;
		}
		for (BlockPos t : set.toArray(BlockPos[]::new)) {
			if (t.distManhattan(pos) <= NEAR * 3 && Math.abs(t.getX() - pos.getX()) <= NEAR && Math.abs(t.getZ() - pos.getZ()) <= NEAR
					&& level.isLoaded(t) && level.getBlockEntity(t) instanceof TerminalBlockEntity terminal) {
				terminal.markChanged();
			}
		}
	}

	// ---- part W5: the switchboards (they power the warehouse whose terminal is nearest them, not joined to it)
	private static final Map<Level, Set<BlockPos>> SWITCHBOARDS = new WeakHashMap<>();

	static void addSwitchboard(Level level, BlockPos pos) {
		SWITCHBOARDS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
	}

	static void removeSwitchboard(Level level, BlockPos pos) {
		Set<BlockPos> set = SWITCHBOARDS.get(level);
		if (set != null) {
			set.remove(pos);
		}
	}

	static Set<BlockPos> switchboards(Level level) {
		Set<BlockPos> set = SWITCHBOARDS.get(level);
		return set == null ? Set.of() : set;
	}

	/**
	 * Part W5: the terminal a block at pos works with: the one running the warehouse it is joined to, else the nearest
	 * one running a warehouse within 48 blocks (a switchboard in the office wall, a status board, a signal box on a wall).
	 */
	public static TerminalBlockEntity terminalNear(Level level, BlockPos pos) {
		TerminalBlockEntity t = terminalOf(level, pos);
		if (t != null) {
			return t;
		}
		Set<BlockPos> set = TERMINALS.get(level);
		if (set == null) {
			return null;
		}
		TerminalBlockEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos p : set) {
			if (Math.abs(p.getX() - pos.getX()) > 48 || Math.abs(p.getZ() - pos.getZ()) > 48 || Math.abs(p.getY() - pos.getY()) > 48 || !level.isLoaded(p)
					|| !(level.getBlockEntity(p) instanceof TerminalBlockEntity terminal) || !terminal.primary()) {
				continue;
			}
			double d = p.distSqr(pos);
			if (d < bestDist) {
				best = terminal;
				bestDist = d;
			}
		}
		return best;
	}

	/** The terminal running the warehouse a block at pos belongs to (the one that runs it, if more than one is joined), if any is loaded near it. */
	public static TerminalBlockEntity terminalOf(Level level, BlockPos pos) {
		Set<BlockPos> set = TERMINALS.get(level);
		if (set == null) {
			return null;
		}
		TerminalBlockEntity any = null;
		for (BlockPos t : set) {
			if (level.isLoaded(t) && level.getBlockEntity(t) instanceof TerminalBlockEntity terminal && terminal.contains(pos)) {
				if (terminal.primary()) {
					return terminal;
				}
				any = any == null ? terminal : any;
			}
		}
		return any;
	}
}
