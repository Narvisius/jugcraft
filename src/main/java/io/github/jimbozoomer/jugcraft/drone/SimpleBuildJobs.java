package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A minimal build-job source: a list of positions and the block wanted at each. The Blueprint System
 * (#23) will be the real source; this one exists for game tests and for the development-only
 * {@code /dronetest} command, so drones can be tried before blueprints exist.
 */
public final class SimpleBuildJobs implements BuildJobs.Source {
	private final UUID jobId = UUID.randomUUID();
	private final UUID owner;
	private final UseMode mode;
	private final Map<BlockPos, BlockState> wanted = new LinkedHashMap<>();
	private final Map<BlockPos, UUID> reservedBy = new HashMap<>();
	private final Map<BlockPos, Long> reservedUntil = new HashMap<>();
	private int filled;

	public SimpleBuildJobs(UUID owner, UseMode mode) {
		this.owner = owner;
		this.mode = mode;
	}

	public void want(BlockPos pos, BlockState state) {
		wanted.put(pos.immutable(), state);
	}

	public int remaining() {
		return wanted.size();
	}

	public int filled() {
		return filled;
	}

	@Override
	public List<BuildJobs.Target> openTargets(ServerLevel level, BlockPos center, int radius, UUID depotOwner, UseMode depotMode, int max) {
		if (!JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)) {
			return List.of();
		}
		long now = level.getGameTime();
		List<BuildJobs.Target> open = new ArrayList<>();
		wanted.entrySet().stream()
				.filter(entry -> Math.abs(entry.getKey().getX() - center.getX()) <= radius && Math.abs(entry.getKey().getZ() - center.getZ()) <= radius)
				.filter(entry -> reservedUntil.getOrDefault(entry.getKey(), Long.MIN_VALUE) < now)
				// Bottom-up: never offer a position above one that is still waiting, or the upper block
				// could land first and cover it (drones come down from above).
				.filter(entry -> !wanted.containsKey(entry.getKey().below()))
				.sorted(Comparator.comparingInt((Map.Entry<BlockPos, BlockState> entry) -> entry.getKey().getY())
						.thenComparingDouble(entry -> entry.getKey().distSqr(center)))
				.limit(max)
				.forEach(entry -> open.add(new BuildJobs.Target(this, jobId, owner, mode, entry.getKey(), entry.getValue(), null)));
		return open;
	}

	@Override
	public boolean reserve(ServerLevel level, BuildJobs.Target target, UUID depot, long untilTick) {
		long now = level.getGameTime();
		UUID holder = reservedBy.get(target.pos());
		if (holder != null && !holder.equals(depot) && reservedUntil.getOrDefault(target.pos(), Long.MIN_VALUE) >= now) {
			return false;
		}
		reservedBy.put(target.pos(), depot);
		reservedUntil.put(target.pos(), untilTick);
		return true;
	}

	@Override
	public void release(ServerLevel level, BuildJobs.Target target, UUID depot) {
		if (depot.equals(reservedBy.get(target.pos()))) {
			reservedBy.remove(target.pos());
			reservedUntil.remove(target.pos());
		}
	}

	@Override
	public boolean stillWanted(ServerLevel level, BuildJobs.Target target, UUID depotOwner, UseMode depotMode) {
		return wanted.containsKey(target.pos()) && JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)
				&& level.getBlockState(target.pos()).canBeReplaced();
	}

	@Override
	public boolean fill(ServerLevel level, BuildJobs.Target target, BlockState state) {
		if (!level.getBlockState(target.pos()).canBeReplaced()) {
			return false;
		}
		level.setBlockAndUpdate(target.pos(), state);
		wanted.remove(target.pos());
		filled++;
		return true;
	}
}
