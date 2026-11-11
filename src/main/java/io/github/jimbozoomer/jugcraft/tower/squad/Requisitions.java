package io.github.jimbozoomer.jugcraft.tower.squad;

import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerRegistry;
import io.github.jimbozoomer.jugcraft.tower.TowerSupply;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;

/**
 * Requisitioning a squad (sheet 4, "the manager's computer"): using the tower manager's executive desk opens its
 * requisitions page (client/RequisitionScreen) for whoever may run the tower (its owner, their party in Party mode). A
 * squad is paid in iron, copper and gold ingots and diamonds from the tower's store and the logistics network its terminal
 * reaches (the cargo packager's goods), the store first; each one bought is a charge on the player's account
 * ({@link SquadAccounts}) that any designator they hold calls down. The first of each size costs
 * {@link JugcraftSquads#COST}; each one a player buys raises that size's price for them by 10% ({@link JugcraftSquads#price}).
 */
public final class Requisitions {
	/** The piece whose use opens the page; how near (blocks) a player must be to it to buy; how far (blocks) its tower's
	 * core may be from it. */
	public static final String DESK = "exec_desk";
	public static final double REACH = 8;
	public static final int CORE_RANGE = 128;

	private Requisitions() {
	}

	static void register() {
		RoomKitBlock.USES.put(DESK, (state, level, pos, player) -> {
			if (level instanceof ServerLevel && player instanceof ServerPlayer server) {
				send(server, pos, true);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** The tower whose desk is at {@code desk}: the nearest core within {@link #CORE_RANGE} blocks, or null. */
	public static @Nullable TowerCoreBlockEntity tower(ServerLevel level, BlockPos desk) {
		TowerCoreBlockEntity best = null;
		double bestD = (double) CORE_RANGE * CORE_RANGE;
		for (BlockPos core : TowerRegistry.get(level).allCores()) {
			double d = core.distSqr(desk);
			if (d < bestD && level.isLoaded(core) && level.getBlockEntity(core) instanceof TowerCoreBlockEntity c) {
				best = c;
				bestD = d;
			}
		}
		return best;
	}

	/** What a tower pays from, as a store (a game test's own map stands in for the core's: TowerSupply.store). */
	public interface Store {
		int stored(Item item);

		int take(Item item, int count);
	}

	/** {@code core}'s whole supply (tower/TowerSupply: its store, its network, and everything hooked up to its Cargo
	 * Exchanges: chests and other inventories, through pipes too, and automatic warehouses; null: nothing). */
	public static @Nullable TowerSupply purse(@Nullable TowerCoreBlockEntity core) {
		return core == null ? null : TowerSupply.of(core);
	}

	/** A store and a cargo packager (if any) as a supply (game tests). */
	public record Purse(Store store, @Nullable CargoPackagerBlockEntity network) {
		public TowerSupply supply() {
			List<TowerSupply.Source> sources = new ArrayList<>();
			sources.add(new TowerSupply.Source() {
				@Override
				public long count(Item item) {
					return store.stored(item);
				}

				@Override
				public long take(Item item, long count) {
					return store.take(item, (int) Math.min(Integer.MAX_VALUE, count));
				}
			});
			if (network != null) {
				sources.add(TowerSupply.packager(network));
			}
			return new TowerSupply(sources);
		}

		/** How much of each payment it has. */
		public int[] have() {
			return Requisitions.have(supply());
		}
	}

	/** How much of each payment {@code supply} has. */
	public static int[] have(TowerSupply supply) {
		Map<Item, Long> counts = supply.counts(List.of(JugcraftSquads.PAY));
		int[] out = new int[JugcraftSquads.PAY.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = (int) Math.min(Integer.MAX_VALUE, counts.getOrDefault(JugcraftSquads.PAY[i], 0L));
		}
		return out;
	}

	/** How much of each payment {@code core}'s tower has, everything it pays from together. */
	public static int[] have(@Nullable TowerCoreBlockEntity core) {
		return core == null ? new int[JugcraftSquads.PAY.length] : have(TowerSupply.of(core));
	}

	/** Whether {@code have} pays for the first squad {@code index} (its price before any's bought). */
	public static boolean affords(int[] have, int index) {
		return affords(have, JugcraftSquads.COST[index]);
	}

	/** Whether {@code have} pays {@code price}. */
	public static boolean affords(int[] have, int[] price) {
		for (int i = 0; i < have.length && i < price.length; i++) {
			if (have[i] < price[i]) {
				return false;
			}
		}
		return true;
	}

	/** Sends {@code player} the desk's page (opening it, or refreshing it). */
	static void send(ServerPlayer player, BlockPos desk, boolean open) {
		ServerLevel level = player.level();
		TowerCoreBlockEntity core = tower(level, desk);
		boolean allowed = core != null && core.mayUse(player);
		if (open && core == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.squad.no_tower"));
			return;
		}
		SquadAccounts accounts = SquadAccounts.get(level);
		List<Integer> prices = new ArrayList<>();
		for (int[] p : accounts.prices(player.getUUID())) {
			prices.addAll(SquadPayloads.list(p));
		}
		ServerPlayNetworking.send(player, new SquadPayloads.Page(desk, SquadPayloads.list(have(allowed ? core : null)),
				SquadPayloads.list(accounts.charges(player.getUUID())), prices, allowed, accounts.inField(level.getServer(), player.getUUID()), open));
	}

	/** {@code player} requisitions squad {@code index} at the desk at {@code desk}: checked, paid, charged. */
	static void buy(ServerPlayer player, BlockPos desk, int index) {
		ServerLevel level = player.level();
		if (index < 0 || index >= JugcraftSquads.SIZES.length || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(desk)) > REACH * REACH
				|| !(level.getBlockState(desk).getBlock() instanceof RoomKitBlock kit) || !DESK.equals(kit.spec.id())) {
			return;
		}
		TowerCoreBlockEntity core = tower(level, desk);
		if (core == null || !core.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.not_owner"));
			return;
		}
		SquadAccounts accounts = SquadAccounts.get(level);
		if (!pay(TowerSupply.of(core), accounts.price(player.getUUID(), index))) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.squad.short"));
			send(player, desk, false);
			return;
		}
		accounts.add(player.getUUID(), index);
		accounts.bought(player.getUUID(), index);              // the next of this size costs them 10% more
		level.playSound(null, desk, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 0.8F, 1.1F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.squad.requisitioned",
				Component.translatable("squad.jugcraft.rank." + JugcraftSquads.RANKS[index]), JugcraftSquads.SIZES[index]));
		send(player, desk, false);
		SquadPayloads.sendAccount(player);
	}

	/** Takes the first squad {@code index}'s price from {@code core}'s supply (its store first, then its network, then
	 * what's hooked up to its Cargo Exchanges); false (nothing taken) if it hasn't it all. */
	public static boolean pay(TowerCoreBlockEntity core, int index) {
		return pay(TowerSupply.of(core), index);
	}

	/** Takes squad {@code index}'s price from {@code purse}: its store first, the rest from its network (used up); false
	 * (nothing taken) if it hasn't it all. */
	public static boolean pay(Purse purse, int index) {
		return pay(purse.supply(), index);
	}

	/** Takes squad {@code index}'s price from {@code supply}, each payment from its sources in order (used up); false
	 * (nothing taken) if it hasn't it all. */
	public static boolean pay(TowerSupply supply, int index) {
		return pay(supply, JugcraftSquads.COST[index]);
	}

	/** Takes {@code price} (iron, copper, gold, diamonds) from {@code supply}, each from its sources in order (used up);
	 * false (nothing taken) if it hasn't it all. */
	public static boolean pay(TowerSupply supply, int[] price) {
		Map<Item, Integer> take = new LinkedHashMap<>();
		for (int i = 0; i < JugcraftSquads.PAY.length && i < price.length; i++) {
			if (price[i] > 0) {
				take.put(JugcraftSquads.PAY[i], price[i]);
			}
		}
		return supply.take(take);
	}
}
