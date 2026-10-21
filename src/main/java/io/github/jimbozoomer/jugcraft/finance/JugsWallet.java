package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.town.Jugs;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** The Exchange's view of the town's Jugs: the one currency, kept by {@link Jugs}. */
final class JugsWallet implements FinanceBook.Wallet {
	private final MinecraftServer server;

	JugsWallet(MinecraftServer server) {
		this.server = server;
	}

	@Override
	public long balance(UUID id) {
		return Jugs.balance(server, id);
	}

	@Override
	public boolean add(UUID id, long amount) {
		if (amount < 0 || amount > TownShops.get().maxBalance - Jugs.balance(server, id)) {
			return false;
		}
		Jugs.add(server, id, amount);
		return true;
	}

	@Override
	public boolean take(UUID id, long amount) {
		return amount == 0 || Jugs.take(server, id, amount);
	}

	@Override
	public Map<UUID, Long> all() {
		return Jugs.data(server).balances();
	}
}
