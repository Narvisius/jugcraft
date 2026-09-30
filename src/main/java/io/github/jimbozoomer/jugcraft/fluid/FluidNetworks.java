package io.github.jimbozoomer.jugcraft.fluid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Moves fluid through connected pipes, mirroring {@code EnergyNetworks}: pipes are passive,
 * a pusher (the electric pump) offers fluid, and the network splits it evenly across every
 * fluid storage touching it. Networks are found by a bounded search, cached per level and
 * dropped whenever a pipe or its neighbors change. Amounts are Fabric droplets.
 */
public final class FluidNetworks {
	/** Hard cap on pipes in one network, so a huge pipe mesh cannot stall a tick. */
	public static final int MAX_PIPES = 1024;
	/** Fabric measures fluid in droplets; Jugcraft's numbers are millibuckets. */
	public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

	private static final Map<Level, Map<BlockPos, Network>> CACHE = new WeakHashMap<>();

	private FluidNetworks() {
	}

	/** Called by pipes and fluid blocks whenever the layout around them may have changed. */
	public static void invalidate(Level level) {
		CACHE.remove(level);
	}

	/**
	 * Pushes fluid from {@code source} out of the given sides of {@code pos}: directly into
	 * adjacent fluid storages, or through a pipe network. Returns the droplets actually moved.
	 */
	public static long pushToNeighbors(Level level, BlockPos pos, Storage<FluidVariant> source, long maxAmount, Iterable<Direction> sides) {
		long moved = 0;
		for (Direction side : sides) {
			long budget = maxAmount - moved;
			if (budget <= 0) {
				break;
			}
			BlockPos neighbor = pos.relative(side);
			if (level.getBlockState(neighbor).getBlock() instanceof FluidPipeBlock pipe) {
				moved += network(level, neighbor).distribute(level, pos, source, Math.min(budget, pipe.transferRate()));
			} else {
				Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbor, side.getOpposite());
				if (target != null) {
					moved += StorageUtil.move(source, target, variant -> true, budget, null);
				}
			}
		}
		return moved;
	}

	private static Network network(Level level, BlockPos pipePos) {
		Map<BlockPos, Network> networks = CACHE.computeIfAbsent(level, l -> new HashMap<>());
		Network cached = networks.get(pipePos);
		if (cached != null) {
			return cached;
		}
		Network network = Network.discover(level, pipePos);
		for (BlockPos pipe : network.pipes) {
			networks.put(pipe, network);
		}
		return network;
	}

	/** A connected set of pipes and the fluid storages touching them. */
	private static final class Network {
		private final Set<BlockPos> pipes = new HashSet<>();
		private final List<Endpoint> endpoints = new ArrayList<>();

		static Network discover(Level level, BlockPos start) {
			Network network = new Network();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start.immutable());
			network.pipes.add(start.immutable());

			while (!queue.isEmpty() && network.pipes.size() < MAX_PIPES) {
				BlockPos pipe = queue.poll();
				for (Direction direction : Direction.values()) {
					BlockPos next = pipe.relative(direction).immutable();
					if (level.getBlockState(next).getBlock() instanceof FluidPipeBlock) {
						if (network.pipes.add(next)) {
							queue.add(next);
						}
					} else if (FluidStorage.SIDED.find(level, next, direction.getOpposite()) != null) {
						network.endpoints.add(new Endpoint(next, direction.getOpposite()));
					}
				}
			}
			return network;
		}

		/** Splits fluid evenly across every storage that can accept it (each counted once), except the source. */
		long distribute(Level level, BlockPos sourcePos, Storage<FluidVariant> source, long budget) {
			List<Storage<FluidVariant>> receivers = new ArrayList<>();
			for (Endpoint endpoint : endpoints) {
				if (endpoint.pos.equals(sourcePos)) {
					continue;
				}
				Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, endpoint.pos, endpoint.side);
				if (storage != null && storage.supportsInsertion() && storage != source
						&& !receivers.contains(storage)) {
					receivers.add(storage);
				}
			}

			long moved = 0;
			// Two passes: an even share first, then leftovers to whoever still has room.
			for (int pass = 0; pass < 2 && moved < budget && !receivers.isEmpty(); pass++) {
				long share = Math.max(1, (budget - moved) / receivers.size());
				for (Storage<FluidVariant> receiver : receivers) {
					if (moved >= budget) {
						break;
					}
					moved += StorageUtil.move(source, receiver, variant -> true, Math.min(share, budget - moved), null);
				}
			}
			return moved;
		}
	}

	private record Endpoint(BlockPos pos, Direction side) {
	}
}
