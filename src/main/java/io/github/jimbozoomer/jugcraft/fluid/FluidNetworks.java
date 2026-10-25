package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
		return pushToNeighbors(level,pos,source,maxAmount,sides,Set.of());
	}
	public static long pushToNeighbors(Level level,BlockPos pos,Storage<FluidVariant> source,long maxAmount,Iterable<Direction> sides,Set<BlockPos> excluded) {
		long moved = 0;
		for (Direction side : sides) {
			long budget = maxAmount - moved;
			if (budget <= 0) {
				break;
			}
			BlockPos neighbor = pos.relative(side);
            if (!ClaimTransfers.allowed(level, pos, neighbor)) continue;
			BlockState neighborState = level.getBlockState(neighbor);
			if (neighborState.getBlock() instanceof FluidPipeBlock pipe) {
				if (!pipe.carries(neighborState)) {
					continue;
				}
				Network network = network(level, neighbor);
				moved += network.distribute(level, pos, source, Math.min(budget, network.rate),excluded);
			} else {
				Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbor, side.getOpposite());
				if (target != null && !excluded.contains(neighbor)) {
					moved += StorageUtil.move(source, target, variant -> true, budget, null);
				}
			}
		}
		return moved;
	}

	private static Network network(Level level, BlockPos pipePos) {
		Map<BlockPos, Network> networks = CACHE.computeIfAbsent(level, l -> new HashMap<>());
		Network cached = networks.get(pipePos);
		if (cached != null && cached.revision == LandRegistry.revision(level)
                && cached.pipes.stream().allMatch(level::hasChunkAt)) {
			return cached;
		}
		Network network = Network.discover(level, pipePos);
        network.revision = LandRegistry.revision(level);
		networks.put(pipePos.immutable(), network);
		if(!network.directed)for(var pipe:network.pipes)networks.put(pipe,network);
		return network;
	}

	/** A connected set of pipes and the fluid storages touching them. */
	private static final class Network {
		private final Set<BlockPos> pipes = new HashSet<>();
		private final List<Endpoint> endpoints = new ArrayList<>();
		/** Droplets one push may send: the slowest pipe's rate, so bronze pipes limit a steel line like cables do. */
		private long rate = Long.MAX_VALUE;
        private long revision;
		private boolean truncated;
		private boolean directed;

		static Network discover(Level level, BlockPos start) {
			Network network = new Network();
			BlockState startState = level.getBlockState(start);
			if (startState.getBlock() instanceof FluidPipeBlock first && first.carries(startState)) {
				network.rate = first.transferRate();
			}
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start.immutable());
			network.pipes.add(start.immutable());

			while (!queue.isEmpty()) {
				BlockPos pipe = queue.poll();
				if (!level.hasChunkAt(pipe)) continue;
				var own = level.getBlockState(pipe);
				if (!(own.getBlock() instanceof FluidPipeBlock current)) continue;
				network.directed |= current.directionalRouting();
				for (Direction direction : Direction.values()) {
					BlockPos next = pipe.relative(direction).immutable();
                    if (!level.hasChunkAt(next) || !ClaimTransfers.allowed(level, pipe, next)) continue;
					BlockState nextState = level.getBlockState(next);
					if (!current.routesTo(own, direction, nextState)) continue;
					if (nextState.getBlock() instanceof FluidPipeBlock nextPipe) {
						if (!current.joins(own, nextState) || !nextPipe.joins(nextState, own)) continue;
						if (nextPipe.carries(nextState) && !network.pipes.contains(next) && network.pipes.size() >= MAX_PIPES) { network.truncated=true;return network; }
						// A closed valve ends the network: it is neither a pipe of it nor a storage on it.
						if (nextPipe.carries(nextState) && network.pipes.add(next)) {
							queue.add(next);
							network.rate = Math.min(network.rate, nextPipe.transferRate());
						}
					} else if (FluidStorage.SIDED.find(level, next, direction.getOpposite()) != null) {
						network.endpoints.add(new Endpoint(next, direction.getOpposite(), pipe));
					}
				}
			}
			return network;
		}

		/**
		 * Splits fluid evenly across every storage that can accept it (each counted once), except the source. A storage
		 * reached only through fluid filters takes only their fluids; one reached through an ordinary pipe takes any.
		 */
		long distribute(Level level, BlockPos sourcePos, Storage<FluidVariant> source, long budget,Set<BlockPos> excluded) {
			if(truncated) return 0;
			Map<Storage<FluidVariant>, Predicate<FluidVariant>> receivers = new LinkedHashMap<>();
			Set<Storage<FluidVariant>> preferred=new HashSet<>();
			for (Endpoint endpoint : endpoints) {
                if(!level.hasChunkAt(endpoint.pos)) continue;
                if (!ClaimTransfers.allowed(level, sourcePos, endpoint.pos)) continue;
				if (endpoint.pos.equals(sourcePos)||excluded.contains(endpoint.pos)) {
					continue;
				}
				Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, endpoint.pos, endpoint.side);
				if (storage == null || !storage.supportsInsertion() || storage == source) {
					continue;
				}
				Predicate<FluidVariant> allowed = allowedThrough(level, endpoint.pipe);
				receivers.merge(storage, allowed, Predicate::or);
				if(level.getBlockState(endpoint.pipe).getBlock() instanceof FluidPipeBlock pipe&&pipe.prefersAdjacentInventory())preferred.add(storage);
			}

			long moved = 0;
			for(var target:preferred) {
				moved+=StorageUtil.move(source,target,receivers.get(target),budget-moved,null);
				if(moved>=budget)return moved;
			}
			// Two passes: an even share first, then leftovers to whoever still has room.
			for (int pass = 0; pass < 2 && moved < budget && !receivers.isEmpty(); pass++) {
				long share = Math.max(1, (budget - moved) / receivers.size());
				for (Map.Entry<Storage<FluidVariant>, Predicate<FluidVariant>> receiver : receivers.entrySet()) {
					if (moved >= budget) {
						break;
					}
					moved += StorageUtil.move(source, receiver.getKey(), receiver.getValue(), Math.min(share, budget - moved), null);
				}
			}
			return moved;
		}

		/** What may leave the network through this pipe: anything, or only a fluid filter's fluid (nothing if unset). */
		private static Predicate<FluidVariant> allowedThrough(Level level, BlockPos pipe) {
			if (!(level.getBlockState(pipe).getBlock() instanceof FluidFilterBlock)) {
				return variant -> true;
			}
			FluidVariant filter = FluidFilterBlock.filter(level, pipe);
			return variant -> !filter.isBlank() && variant.equals(filter);
		}
	}

	/** A storage's face touching the network, and the pipe it touches. */
	private record Endpoint(BlockPos pos, Direction side, BlockPos pipe) {
	}
}
