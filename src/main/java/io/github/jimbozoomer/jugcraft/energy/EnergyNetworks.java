package io.github.jimbozoomer.jugcraft.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Moves energy through connected cables. Networks are discovered by a bounded search
 * the first time a producer pushes into a cable, cached per level, and thrown away when
 * any cable in that level is placed, removed or has a neighbor change, so idle cables
 * cost nothing and topology changes are always picked up.
 */
public final class EnergyNetworks {
	/** Hard cap on cables in one network, so a huge cable mesh cannot stall a tick. */
	public static final int MAX_CABLES = 2048;

	private static final Map<Level, Map<BlockPos, Network>> CACHE = new WeakHashMap<>();

	private EnergyNetworks() {
	}

	/** Called by cables whenever the cable layout around them may have changed. */
	public static void invalidate(Level level) {
		CACHE.remove(level);
	}

	/**
	 * Pushes energy from {@code source} out of the given sides of {@code pos}: directly into
	 * adjacent storages, or through a cable network. Returns the JE actually moved.
	 */
	public static long pushToNeighbors(Level level, BlockPos pos, EnergyStorage source, long maxAmount, Iterable<Direction> sides) {
		long moved = 0;
		for (Direction side : sides) {
			long budget = Math.min(maxAmount - moved, source.getAmount());
			if (budget <= 0) {
				break;
			}
			BlockPos neighbor = pos.relative(side);
			if (level.getBlockState(neighbor).getBlock() instanceof CableBlock cable) {
				moved += network(level, neighbor).distribute(level, pos, source, Math.min(budget, cable.transferRate()));
			} else {
				EnergyStorage target = EnergyStorage.SIDED.find(level, neighbor, side.getOpposite());
				if (target != null) {
					moved += move(source, target, budget);
				}
			}
		}
		return moved;
	}

	/** Moves up to {@code maxAmount} between two storages; either the whole move commits or nothing changes. */
	public static long move(EnergyStorage from, EnergyStorage to, long maxAmount) {
		if (!from.supportsExtraction() || !to.supportsInsertion() || maxAmount <= 0) {
			return 0;
		}
		// Simulate both sides (transactions are aborted when not committed), then move the exact amount.
		long amount;
		try (Transaction simulation = Transaction.openOuter()) {
			amount = to.insert(from.extract(maxAmount, simulation), simulation);
		}
		if (amount <= 0) {
			return 0;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long extracted = from.extract(amount, transaction);
			if (extracted != amount || to.insert(extracted, transaction) != extracted) {
				return 0;
			}
			transaction.commit();
			return extracted;
		}
	}

	private static Network network(Level level, BlockPos cablePos) {
		Map<BlockPos, Network> networks = CACHE.computeIfAbsent(level, l -> new HashMap<>());
		Network cached = networks.get(cablePos);
		if (cached != null) {
			return cached;
		}
		Network network = Network.discover(level, cablePos);
		for (BlockPos cable : network.cables) {
			networks.put(cable, network);
		}
		return network;
	}

	/** A connected set of cables and the storages touching them. */
	private static final class Network {
		private final Set<BlockPos> cables = new HashSet<>();
		private final List<Endpoint> endpoints = new ArrayList<>();

		static Network discover(Level level, BlockPos start) {
			Network network = new Network();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start.immutable());
			network.cables.add(start.immutable());

			while (!queue.isEmpty() && network.cables.size() < MAX_CABLES) {
				BlockPos cable = queue.poll();
				for (Direction direction : Direction.values()) {
					BlockPos next = cable.relative(direction).immutable();
					if (level.getBlockState(next).getBlock() instanceof CableBlock) {
						if (network.cables.add(next)) {
							queue.add(next);
						}
					} else if (EnergyStorage.SIDED.find(level, next, direction.getOpposite()) != null) {
						network.endpoints.add(new Endpoint(next, direction.getOpposite()));
					}
				}
			}
			return network;
		}

		/** Splits energy evenly across every storage that can accept it (each counted once), except the source. */
		long distribute(Level level, BlockPos sourcePos, EnergyStorage source, long budget) {
			List<EnergyStorage> receivers = new ArrayList<>();
			for (Endpoint endpoint : endpoints) {
				if (endpoint.pos.equals(sourcePos)) {
					continue;
				}
				EnergyStorage storage = EnergyStorage.SIDED.find(level, endpoint.pos, endpoint.side);
				if (storage != null && storage.supportsInsertion() && storage != source
						&& !receivers.contains(storage)) {
					receivers.add(storage);
				}
			}

			long moved = 0;
			// Two passes: an even share first, then leftovers to whoever still has room.
			for (int pass = 0; pass < 2 && moved < budget && !receivers.isEmpty(); pass++) {
				long share = Math.max(1, (budget - moved) / receivers.size());
				for (EnergyStorage receiver : receivers) {
					if (moved >= budget) {
						break;
					}
					moved += move(source, receiver, Math.min(share, budget - moved));
				}
			}
			return moved;
		}
	}

	private record Endpoint(BlockPos pos, Direction side) {
	}
}
