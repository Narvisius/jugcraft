package io.github.jimbozoomer.jugcraft.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Jugs: the town's credit, a whole number per player kept by the server (data/jugcraft_jugs.dat in the Overworld).
 * There is no Jug item: players see and spend their Jugs at the town's shops and send them to each other at its ATMs.
 * Only the server changes a balance, and never below zero or above {@link TownShops#maxBalance}.
 */
public final class Jugs {
	private Jugs() {
	}

	public static Data data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
	}

	public static long balance(MinecraftServer server, UUID player) {
		return data(server).balance(player);
	}

	/** Adds Jugs (capped at the maximum balance); returns the new balance. */
	public static long add(MinecraftServer server, UUID player, long amount) {
		return data(server).add(player, amount);
	}

	/** Takes Jugs if the player has that many; false (and nothing taken) otherwise. */
	public static boolean take(MinecraftServer server, UUID player, long amount) {
		return data(server).take(player, amount);
	}

	/**
	 * Moves Jugs from one player to another; false if the amount is not positive, the sender lacks it, they are the same
	 * player, or the recipient's balance would pass the maximum.
	 */
	public static boolean transfer(MinecraftServer server, UUID from, UUID to, long amount) {
		Data data = data(server);
		if (amount <= 0 || from.equals(to) || data.balance(from) < amount
				|| data.balance(to) + amount > TownShops.get().maxBalance) {
			return false;
		}
		data.take(from, amount);
		data.add(to, amount);
		return true;
	}

	public static final class Data extends SavedData {
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).fieldOf("balances").forGetter(d -> d.balances))
				.apply(i, Data::new));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("jugs"), Data::new, CODEC, null);

		private final Map<UUID, Long> balances = new HashMap<>();

		Data() {
		}

		Data(Map<UUID, Long> saved) {
			saved.forEach((player, balance) -> {
				if (balance > 0) {
					balances.put(player, Math.min(balance, TownShops.get().maxBalance));
				}
			});
		}

		public long balance(UUID player) {
			return balances.getOrDefault(player, 0L);
		}

		public long add(UUID player, long amount) {
			if (amount <= 0) {
				return balance(player);
			}
			long next = Math.min(TownShops.get().maxBalance, balance(player) + amount);
			balances.put(player, next);
			setDirty();
			return next;
		}

		public boolean take(UUID player, long amount) {
			long have = balance(player);
			if (amount <= 0 || have < amount) {
				return false;
			}
			if (have == amount) {
				balances.remove(player);
			} else {
				balances.put(player, have - amount);
			}
			setDirty();
			return true;
		}
	}
}
