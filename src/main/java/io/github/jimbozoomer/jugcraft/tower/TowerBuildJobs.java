package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Drone Tower tier under construction, offered to drones as build jobs. Each job is one {@link TowerData.Tile}
 * (up to 3x3 blocks of one layer): a drone flies it in and the whole tile is placed, block by block. The
 * modules were paid when the upgrade started, so the depot takes nothing from its packager
 * ({@link BuildJobs.Target#supplied()}). Only the tower's own depot builds it.
 *
 * <p>Tiles are offered bottom layer first: only the lowest unfinished layer and the one above it, and never a
 * tile with an unfinished tile under it (drones come down from above, so a tile placed first would cover the
 * one below). The tower goes up from the ground, never in mid-air.
 */
final class TowerBuildJobs implements BuildJobs.Source {
	/** Tiles offered per request (the depot asks once a second). */
	private static final int SCAN = 512;

	private final TowerCoreBlockEntity core;
	private final UUID jobId = UUID.randomUUID();
	private int tierNumber;
	private TowerData.Tier tier;
	private BitSet filled;
	private final Map<Integer, UUID> reservedBy = new HashMap<>();
	private final Map<Integer, Long> reservedUntil = new HashMap<>();
	/** Layer-and-cell keys of tiles not yet placed, to check "is there an open tile under this one". */
	private final Set<Long> open = new HashSet<>();
	/** First tile not yet placed (tiles are sorted bottom up). */
	private int firstOpen;

	TowerBuildJobs(TowerCoreBlockEntity core, int tier, BitSet filled) {
		this.core = core;
		reset(tier, filled);
	}

	void reset(int tierNumber, BitSet filled) {
		this.tierNumber = tierNumber;
		this.tier = TowerData.get().tier(Math.max(1, tierNumber));
		this.filled = filled;
		open.clear();
		for (int i = 0; i < tier.tiles.size(); i++) {
			if (!filled.get(i)) {
				open.add(cellKey(tier.tiles.get(i)));
			}
		}
		firstOpen = filled.nextClearBit(0);
	}

	private static long cellKey(TowerData.Tile tile) {
		return cellKey(tile.y(), Math.floorDiv(tile.x(), TowerData.TILE), Math.floorDiv(tile.z(), TowerData.TILE));
	}

	private static long cellKey(int y, int cx, int cz) {
		return y * 1_000_000L + (cx + 500) * 1000L + (cz + 500);
	}

	private BlockPos at(int[] block) {
		return core.getBlockPos().offset(block[0], block[1], block[2]);
	}

	/** Tile {@code index} of the target (stored in its position: the tile's first block). */
	private int tileOf(BuildJobs.Target target) {
		BlockPos rel = target.pos().subtract(core.getBlockPos());
		for (int i = firstOpen; i < tier.tiles.size(); i++) {
			TowerData.Tile tile = tier.tiles.get(i);
			if (tile.x() == rel.getX() && tile.y() == rel.getY() && tile.z() == rel.getZ()) {
				return i;
			}
		}
		return -1;
	}

	private boolean mayServe(UUID depotOwner, UseMode depotMode) {
		UUID owner = core.owner();
		DroneTerminalBlockEntity terminal = core.terminal();
		return owner != null && terminal != null && JugcraftParties.mayServe(depotOwner, depotMode, owner, terminal.mode());
	}

	@Override
	public boolean crewMayFinish() {
		return true;
	}

	@Override
	public List<BuildJobs.Target> openTargets(ServerLevel level, BlockPos center, int radius, UUID depotOwner, UseMode depotMode, int max) {
		List<BuildJobs.Target> out = new ArrayList<>();
		if (level != core.getLevel() || !mayServe(depotOwner, depotMode) || core.building() != tierNumber) {
			return out;
		}
		DroneTerminalBlockEntity terminal = core.terminal();
		if (terminal == null || terminal.owner() == null || !terminal.owner().equals(depotOwner)) {
			return out; // only the tower's own depot builds the tower
		}
		long now = level.getGameTime();
		firstOpen = filled.nextClearBit(firstOpen);
		if (firstOpen >= tier.tiles.size()) {
			return out;
		}
		// Ground up: only the lowest unfinished layer and the one above it are open for building (the tiles are
		// sorted bottom up), so nothing is built hanging in mid-air ahead of what holds it up.
		int ceiling = tier.tiles.get(firstOpen).y() + 1;
		int scanned = 0;
		for (int i = firstOpen; i < tier.tiles.size() && out.size() < max && scanned < SCAN; i++) {
			if (filled.get(i)) {
				continue;
			}
			TowerData.Tile tile = tier.tiles.get(i);
			if (tile.y() > ceiling) {
				break;
			}
			if (reservedUntil.getOrDefault(i, Long.MIN_VALUE) >= now) {
				continue; // in flight: doesn't use up the scan, so a busy fleet still sees the next tiles
			}
			scanned++;
			// Never over an open tile: any open tile in the layers below, in the same cell or a neighbour.
			if (coveredBelow(tile)) {
				continue;
			}
			if (tile.blocks().length == 0) {
				// Clear-only: if the cells are empty already (the usual case) it is done without a flight.
				if (alreadyClear(level, tile)) {
					complete(level, i, tile);
					continue;
				}
				out.add(new BuildJobs.Target(this, jobId, core.owner(), terminal.mode(), at(tier.air[tile.air()[0]]),
						net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), null, true));
				continue;
			}
			int[] first = tier.place[tile.blocks()[0]];
			out.add(new BuildJobs.Target(this, jobId, core.owner(), terminal.mode(), at(first), TowerData.get().state(first[3]), null, true));
		}
		return out;
	}

	private boolean coveredBelow(TowerData.Tile tile) {
		int cx = Math.floorDiv(tile.x(), TowerData.TILE);
		int cz = Math.floorDiv(tile.z(), TowerData.TILE);
		TowerData.Tile lowest = tier.tiles.get(Math.min(firstOpen, tier.tiles.size() - 1));
		for (int y = tile.y() - 1; y >= lowest.y(); y--) {
			if (open.contains(cellKey(y, cx, cz))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean reserve(ServerLevel level, BuildJobs.Target target, UUID depot, long untilTick) {
		int index = tileOf(target);
		if (index < 0 || filled.get(index)) {
			return false;
		}
		UUID holder = reservedBy.get(index);
		if (holder != null && !holder.equals(depot) && reservedUntil.getOrDefault(index, Long.MIN_VALUE) >= level.getGameTime()) {
			return false;
		}
		reservedBy.put(index, depot);
		reservedUntil.put(index, untilTick);
		return true;
	}

	@Override
	public void release(ServerLevel level, BuildJobs.Target target, UUID depot) {
		int index = tileOf(target);
		if (index >= 0 && depot.equals(reservedBy.get(index))) {
			reservedBy.remove(index);
			reservedUntil.remove(index);
		}
	}

	@Override
	public boolean stillWanted(ServerLevel level, BuildJobs.Target target, UUID depotOwner, UseMode depotMode) {
		int index = tileOf(target);
		return index >= 0 && !filled.get(index) && core.building() == tierNumber && mayServe(depotOwner, depotMode);
	}

	@Override
	public boolean fill(ServerLevel level, BuildJobs.Target target, BlockState state) {
		int index = tileOf(target);
		if (index < 0 || filled.get(index)) {
			return false;
		}
		complete(level, index, tier.tiles.get(index));
		return true;
	}

	private boolean alreadyClear(ServerLevel level, TowerData.Tile tile) {
		for (int a : tile.air()) {
			BlockPos at = at(tier.air[a]);
			if (level.isLoaded(at) && !TowerCoreBlockEntity.clearable(level, at)) {
				continue;
			}
			if (!level.isLoaded(at) || !level.getBlockState(at).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Clears the tile's stray blocks, places its blocks and marks it done. */
	private void complete(ServerLevel level, int index, TowerData.Tile tile) {
		for (int a : tile.air()) {
			TowerCoreBlockEntity.clear(level, at(tier.air[a]));
		}
		for (int b : tile.blocks()) {
			int[] block = tier.place[b];
			TowerCoreBlockEntity.place(level, at(block), TowerData.get().state(block[3]));
		}
		open.remove(cellKey(tile));
		// Another tile in the same cell and layer (rare) keeps the cell open.
		for (int i = firstOpen; i < tier.tiles.size(); i++) {
			if (i != index && !filled.get(i) && cellKey(tier.tiles.get(i)) == cellKey(tile)) {
				open.add(cellKey(tile));
				break;
			}
		}
		reservedBy.remove(index);
		reservedUntil.remove(index);
		core.tileFilled(index);
	}
}
