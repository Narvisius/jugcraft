package io.github.jimbozoomer.jugcraft.logistics;

import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Moves items through connected item pipes, like the energy and fluid networks: pipes are passive,
 * pushers (extractors, machines set to eject) offer items, and the network hands them to the
 * inventories it touches. Items arrive instantly; the pushers limit the rate.
 *
 * <p>Routing: an item goes first to a sorter whose filter matches it, then to any other inventory,
 * taking turns between inventories so no single one fills first. Sorters whose filter does not match
 * refuse it. If nothing accepts an item, it stays where it was.
 *
 * <p>Networks are found by a bounded search, cached per level, and rebuilt when a pipe or its
 * neighbours change.
 */
public final class ItemNetworks {
	/** Hard cap on pipes in one network. */
	public static final int MAX_PIPES = 1024;

	private static final Map<Level, Map<BlockPos, Network>> CACHE = new WeakHashMap<>();

	private ItemNetworks() {
	}

	public static void invalidate(Level level) {
		CACHE.remove(level);
	}

	/**
	 * Pushes up to {@code maxItems} from {@code source} out of one side of {@code pos}: into a pipe
	 * network, or straight into an adjacent inventory. {@code exclude} lists inventories that must not
	 * receive (the one the items came from). Returns the number of items moved.
	 */
	public static long push(Level level, BlockPos pos, Direction side, Storage<ItemVariant> source, long maxItems, Set<BlockPos> exclude) {
		if (maxItems <= 0) {
			return 0;
		}
		BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) return 0;
            if (!ClaimTransfers.allowed(level, pos, neighbor)) return 0;
		if (level.getBlockState(neighbor).getBlock() instanceof ItemPipeBlock) {
			return network(level, neighbor).distribute(level, source, maxItems, exclude, pos);
		}
		if (exclude.contains(neighbor)) {
			return 0;
		}
		Storage<ItemVariant> target = ItemStorage.SIDED.find(level, neighbor, side.getOpposite());
		return target == null ? 0 : StorageUtil.move(source, target, variant -> true, maxItems, null);
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
		// Directional fittings make reachability depend on the entry pipe.
		networks.put(pipePos.immutable(), network);
		if(!network.directed && !network.colored)for(var pipe:network.pipes)networks.put(pipe,network);
		return network;
	}

	private static final class Network {
		private final Set<BlockPos> pipes = new HashSet<>();
		private final List<Endpoint> endpoints = new ArrayList<>();
		private int turn;
        private long revision;
		private long rate = Long.MAX_VALUE;
		private boolean truncated;
		private boolean directed;
		private boolean colored;
		private BlockPos entry;

		static Network discover(Level level, BlockPos start) {
			return discover(level, start, null);
		}
		static Network discover(Level level, BlockPos start, ItemVariant resource) {
			Network network = new Network();
			network.entry = start.immutable();
			ArrayDeque<Route> queue = new ArrayDeque<>();
			Set<Route> visited = new HashSet<>();
			queue.add(new Route(start.immutable(), -1));
			visited.add(queue.peek());
			network.pipes.add(start.immutable());
			while (!queue.isEmpty()) {
				Route route = queue.poll();
				BlockPos pipe = route.pos;
				if (!level.hasChunkAt(pipe)) continue;
				var own = level.getBlockState(pipe);
				if (!(own.getBlock() instanceof ItemPipeBlock current)) continue;
				network.rate = Math.min(network.rate, current.transferLimit());
				network.directed |= current.directionalRouting();
				network.colored |= current.coloredRouting();
				int color = resource == null ? -1 : current.routeColor(level, pipe, own, resource, route.color);
				if (color < -1) continue;
				for (Direction direction : Direction.values()) {
					BlockPos next = pipe.relative(direction).immutable();
                    if (!level.hasChunkAt(next) || !ClaimTransfers.allowed(level, pipe, next)) continue;
					var neighbor = level.getBlockState(next);
					if (!current.routesTo(own, direction, neighbor)) continue;
					if (neighbor.getBlock() instanceof ItemPipeBlock nextPipe) {
						if (!current.joins(own, neighbor) || !nextPipe.joins(neighbor, own)) continue;
						Route nextRoute = new Route(next, color);
						if (!visited.contains(nextRoute) && visited.size() >= MAX_PIPES) { network.truncated = true; return network; }
						if (visited.add(nextRoute)) {
							network.pipes.add(next);
							queue.add(nextRoute);
						}
					} else if (ItemStorage.SIDED.find(level, next, direction.getOpposite()) != null) {
						network.endpoints.add(new Endpoint(next, direction.getOpposite(),current.prefersAdjacentInventory()));
					}
				}
			}
			return network;
		}

		long distribute(Level level, Storage<ItemVariant> source, long budget, Set<BlockPos> exclude, BlockPos pusher) {
			return distribute(level, source, budget, exclude, pusher, null);
		}
		long distribute(Level level, Storage<ItemVariant> source, long budget, Set<BlockPos> exclude, BlockPos pusher, ItemVariant only) {
			if (truncated) return 0;
			budget = Math.min(budget, rate);
			List<Storage<ItemVariant>> sorters = new ArrayList<>();
			List<Storage<ItemVariant>> preferred = new ArrayList<>();
			List<Storage<ItemVariant>> others = new ArrayList<>();
			// Put preferred endpoints first so an ordinary alias cannot hide their priority.
			for (Endpoint endpoint : endpoints.stream().sorted(java.util.Comparator.comparing(Endpoint::preferred).reversed()).toList()) {
                if (!level.hasChunkAt(endpoint.pos)) continue;
                if (!ClaimTransfers.allowed(level, pusher, endpoint.pos)) continue;
				if (endpoint.pos.equals(pusher) || exclude.contains(endpoint.pos)) {
					continue;
				}
				Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, endpoint.pos, endpoint.side);
				if (storage == null || storage == source || !storage.supportsInsertion()
						|| sorters.contains(storage) || preferred.contains(storage) || others.contains(storage)) {
					continue;
				}
				(level.getBlockState(endpoint.pos).getBlock() instanceof ItemSorterBlock ? sorters : endpoint.preferred?preferred:others).add(storage);
			}
			if (sorters.isEmpty() && preferred.isEmpty() && others.isEmpty()) {
				return 0;
			}
			List<ItemVariant> resources = new ArrayList<>();
			int examined = 0;
			for (StorageView<ItemVariant> view : source.nonEmptyViews()) {
				if (++examined > 4096) return 0;
				if ((only == null || only.equals(view.getResource())) && !resources.contains(view.getResource())) {
					resources.add(view.getResource());
				}
			}
			long moved = 0;
			turn++;
			for (ItemVariant resource : resources) {
				if (colored && only == null) {
					// Recompute with carried color for this resource. Filter inventories can change without block updates.
					moved += discover(level, entry, resource).distribute(level,source,budget-moved,exclude,pusher,resource);
					if (moved >= budget) return moved;
					continue;
				}
				// Matching sorters first: they only accept what their filter allows.
				for (Storage<ItemVariant> sorter : sorters) {
					moved += StorageUtil.move(source, sorter, resource::equals, budget - moved, null);
					if (moved >= budget) {
						return moved;
					}
				}
				for(var target:preferred) {
					moved+=StorageUtil.move(source,target,resource::equals,budget-moved,null);
					if(moved>=budget)return moved;
				}
				for (int i = 0; i < others.size() && moved < budget; i++) {
					Storage<ItemVariant> target = others.get(Math.floorMod(turn + i, others.size()));
					moved += StorageUtil.move(source, target, resource::equals, budget - moved, null);
				}
				if (moved >= budget) {
					return moved;
				}
			}
			return moved;
		}
	}

	private record Endpoint(BlockPos pos, Direction side,boolean preferred) {
	}
	private record Route(BlockPos pos, int color) {}
}
