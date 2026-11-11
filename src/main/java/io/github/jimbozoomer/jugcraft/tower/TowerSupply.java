package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.Warehouses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Everything a Drone Tower can pay from (Justin, 10 November 2026: "One of the biggest points of this mod is so you can
 * take stuff in your storage and then have the drone tower use it ... I have a chest filled with everything that's
 * needed ... attached via pipes ... If the system is hooked up to [the warehouse], it should be able to detect
 * everything in the warehouse and drag it from there too"; his picks: "Read through pipes" and "Take instantly").
 *
 * <p>In this order: the tower storage (the core's store), the depot's cargo packager, every inventory hooked up to one
 * of the tower's Cargo Exchanges (against its port, or anywhere along item pipes from it: chests, barrels, any mod's
 * storage), and every automatic warehouse hooked up that way (what's on its racks' pallets that no drone or order has
 * booked, taken at once). A tower's Cargo Exchanges are its own and any placed within reach of its depot terminal and
 * bound to it. Drop pods (tower/squad/Requisitions) and the tower's tiers (TowerCoreBlockEntity) are paid from it.
 *
 * <p>Only plain items count from the hooked-up inventories (no names, enchantments or damage), as the tower storage
 * keeps only plain items. Found afresh each time it's asked for: nothing is cached, nothing kept loaded.
 */
public final class TowerSupply {
	/** One place a tower pays from. */
	public interface Source {
		/** How many of {@code item} it holds (as far as can be taken now). */
		long count(Item item);

		/** Takes up to {@code count} of {@code item}; how many it took. */
		long take(Item item, long count);
	}

	private final List<Source> sources;

	public TowerSupply(List<Source> sources) {
		this.sources = List.copyOf(sources);
	}

	public List<Source> sources() {
		return sources;
	}

	/** {@code core}'s supply, found now (on the server; elsewhere, or with no core, the store alone or nothing). */
	public static TowerSupply of(@Nullable TowerCoreBlockEntity core) {
		if (core == null) {
			return new TowerSupply(List.of());
		}
		List<Source> out = new ArrayList<>();
		out.add(store(core));
		DroneTerminalBlockEntity terminal = core.getLevel() instanceof ServerLevel ? core.terminal() : null;
		CargoPackagerBlockEntity packager = terminal == null ? null : terminal.packagerEntity();
		if (packager != null) {
			out.add(packager(packager));
		}
		if (terminal != null && core.getLevel() instanceof ServerLevel level) {
			Hooked hooked = hooked(level, core, terminal, packager);
			if (!hooked.storages.isEmpty()) {
				out.add(new Inventories(hooked.storages));
			}
			for (TerminalBlockEntity warehouse : hooked.warehouses) {
				out.add(warehouse(level, warehouse));
			}
		}
		return new TowerSupply(out);
	}

	/** The tower storage (the core's store). */
	public static Source store(TowerCoreBlockEntity core) {
		return new Source() {
			@Override
			public long count(Item item) {
				return core.stored(item);
			}

			@Override
			public long take(Item item, long count) {
				return core.take(item, (int) Math.min(Integer.MAX_VALUE, count));
			}
		};
	}

	/** A cargo packager (the depot's logistics network). */
	public static Source packager(CargoPackagerBlockEntity packager) {
		return new Source() {
			@Override
			public long count(Item item) {
				return packager.count(s -> s.is(item));
			}

			@Override
			public long take(Item item, long count) {
				long took = 0;
				while (took < count && !packager.takeOne(s -> s.is(item)).isEmpty()) {
					took++;
				}
				return took;
			}
		};
	}

	/** An automatic warehouse: what's on its racks that nobody has booked, taken at once. */
	public static Source warehouse(ServerLevel level, TerminalBlockEntity terminal) {
		return new Source() {
			@Override
			public long count(Item item) {
				return terminal.isRemoved() ? 0 : terminal.free(level, ItemVariant.of(item));
			}

			@Override
			public long take(Item item, long count) {
				return terminal.isRemoved() ? 0 : terminal.takeNow(level, ItemVariant.of(item), count);
			}
		};
	}

	/**
	 * Inventories (any mod's item storage). Counted together in one transaction that's thrown away, so an inventory
	 * reached two ways (a double chest's two halves, two pipes to one barrel) counts once.
	 */
	public static final class Inventories implements Source {
		private final List<Storage<ItemVariant>> storages;

		public Inventories(List<Storage<ItemVariant>> storages) {
			this.storages = List.copyOf(storages);
		}

		public List<Storage<ItemVariant>> storages() {
			return storages;
		}

		@Override
		public long count(Item item) {
			return counts(List.of(item)).getOrDefault(item, 0L);
		}

		/** How many of each of {@code items}, all at once. */
		public Map<Item, Long> counts(Collection<Item> items) {
			Map<Item, Long> out = new LinkedHashMap<>();
			Map<ItemVariant, Item> wanted = new LinkedHashMap<>();
			for (Item item : items) {
				out.put(item, 0L);
				wanted.put(ItemVariant.of(item), item);
			}
			if (Transaction.isOpen()) {
				return out;                                   // (never inside another's transaction)
			}
			try (Transaction tx = Transaction.openOuter()) {
				for (Storage<ItemVariant> storage : storages) {
					if (!storage.supportsExtraction()) {
						continue;
					}
					for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
						Item item = wanted.get(view.getResource());
						if (item == null) {
							continue;
						}
						long took = view.extract(view.getResource(), view.getAmount(), tx);
						if (took > 0) {
							out.merge(item, took, Long::sum);
						}
					}
				}
				tx.abort();
			}
			return out;
		}

		@Override
		public long take(Item item, long count) {
			if (Transaction.isOpen()) {
				return 0;
			}
			ItemVariant variant = ItemVariant.of(item);
			long took = 0;
			for (Storage<ItemVariant> storage : storages) {
				if (took >= count) {
					break;
				}
				if (!storage.supportsExtraction()) {
					continue;
				}
				try (Transaction tx = Transaction.openOuter()) {
					long got = storage.extract(variant, count - took, tx);
					tx.commit();
					took += got;
				}
			}
			return took;
		}
	}

	// ------------------------------------------------------------------ the sums

	/** How many of {@code item} the whole supply has. */
	public long count(Item item) {
		long n = 0;
		for (Source s : sources) {
			n += s.count(item);
		}
		return n;
	}

	/** How many of each of {@code items} the whole supply has (each inventory looked through once). */
	public Map<Item, Long> counts(Collection<Item> items) {
		Map<Item, Long> out = new LinkedHashMap<>();
		for (Item item : items) {
			out.put(item, 0L);
		}
		for (Source s : sources) {
			if (s instanceof Inventories inv) {
				inv.counts(items).forEach((item, n) -> out.merge(item, n, Long::sum));
			} else {
				for (Item item : items) {
					out.merge(item, s.count(item), Long::sum);
				}
			}
		}
		return out;
	}

	/** What's short of {@code price}: each item and how many more are needed (none: it pays). */
	public Map<Item, Long> shortOf(Map<Item, Integer> price) {
		Map<Item, Long> have = counts(price.keySet());
		Map<Item, Long> out = new LinkedHashMap<>();
		for (Map.Entry<Item, Integer> e : price.entrySet()) {
			long missing = e.getValue() - have.getOrDefault(e.getKey(), 0L);
			if (missing > 0) {
				out.put(e.getKey(), missing);
			}
		}
		return out;
	}

	public boolean has(Map<Item, Integer> price) {
		return shortOf(price).isEmpty();
	}

	/** Takes {@code price}, each item from the sources in order; nothing at all if it hasn't all of it. */
	public boolean take(Map<Item, Integer> price) {
		if (!has(price)) {
			return false;
		}
		for (Map.Entry<Item, Integer> e : price.entrySet()) {
			long need = e.getValue();
			for (Source s : sources) {
				if (need <= 0) {
					break;
				}
				need -= s.take(e.getKey(), need);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ what's hooked up to the Cargo Exchanges

	/** The inventories and warehouses hooked up to a tower's Cargo Exchanges. */
	public record Hooked(List<Storage<ItemVariant>> storages, List<TerminalBlockEntity> warehouses) {
	}

	/**
	 * What's hooked up to the Cargo Exchanges of {@code core}'s depot ({@code terminal}): against a station's port, or at
	 * the far end of item pipes from it. The tower's own blocks (the stations, the packager, the core, the terminal) are
	 * left out; a warehouse's block stands for the whole warehouse.
	 */
	public static Hooked hooked(ServerLevel level, TowerCoreBlockEntity core, DroneTerminalBlockEntity terminal, @Nullable CargoPackagerBlockEntity packager) {
		List<Storage<ItemVariant>> storages = new ArrayList<>();
		List<TerminalBlockEntity> warehouses = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		Map<TerminalBlockEntity, Boolean> found = new IdentityHashMap<>();
		Set<BlockPos> own = new HashSet<>();
		own.add(core.getBlockPos());
		own.add(terminal.getBlockPos());
		if (packager != null) {
			own.add(packager.getBlockPos());
		}
		for (ExchangeStationBlockEntity station : stations(level, terminal)) {
			BlockState state = station.getBlockState();
			if (!(state.getBlock() instanceof ExchangeStationBlock block) || block.energy) {
				continue;
			}
			Direction facing = state.getValue(ExchangeStationBlock.FACING);
			BlockPos port = block.footprint(state).partPos(station.getBlockPos(), facing, block.portPart(state));
			BlockPos next = port.relative(facing);
			if (!level.isLoaded(next)) {
				continue;
			}
			List<Map.Entry<BlockPos, Direction>> ends = ItemNetworks.inventories(level, next);
			if (ends.isEmpty() && !(level.getBlockState(next).getBlock() instanceof io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock)) {
				ends = List.of(Map.entry(next, facing.getOpposite()));
			}
			for (Map.Entry<BlockPos, Direction> end : ends) {
				BlockPos at = end.getKey();
				if (own.contains(at) || !level.isLoaded(at) || !seen.add(at.asLong() * 8 + end.getValue().ordinal())) {
					continue;
				}
				BlockState there = level.getBlockState(at);
				if (there.getBlock() instanceof ExchangeStationBlock) {
					continue;                                 // (a station's own port: its storage is the tower's)
				}
				if (there.getBlock() instanceof PieceBlock) {
					TerminalBlockEntity w = Warehouses.terminalOf(level, at);
					if (w != null && found.put(w, true) == null) {
						warehouses.add(w);
					}
					continue;
				}
				Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, at, end.getValue());
				if (storage != null) {
					storages.add(storage);
				}
			}
		}
		return new Hooked(storages, warehouses);
	}

	/** The Cargo Exchanges (and Energy Exchanges) bound to {@code terminal}: loaded ones within reach of it. */
	public static List<ExchangeStationBlockEntity> stations(ServerLevel level, DroneTerminalBlockEntity terminal) {
		List<ExchangeStationBlockEntity> out = new ArrayList<>();
		BlockPos t = terminal.getBlockPos();
		int r = ExchangePortBlock.RANGE;
		for (int cx = (t.getX() - r) >> 4; cx <= (t.getX() + r) >> 4; cx++) {
			for (int cz = (t.getZ() - r) >> 4; cz <= (t.getZ() + r) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity e : List.copyOf(chunk.getBlockEntities().values())) {
					if (e instanceof ExchangeStationBlockEntity station && !station.isRemoved() && station.terminal() == terminal) {
						out.add(station);
					}
				}
			}
		}
		return out;
	}
}
