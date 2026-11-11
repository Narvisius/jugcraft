package io.github.jimbozoomer.jugcraft.tower.squad;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Each player's drop pod squads (kept with the overworld, for every dimension): the charges on their account (a squad
 * of each size requisitioned and not yet called down: any designator they hold can spend them), who of theirs is in
 * the field (their legionaries and pods still on the way: one squad at a time), and how many of each size they've
 * bought (their prices: {@link #price}).
 */
public final class SquadAccounts extends SavedData {
	/** A player's record: charges for squads of 3, 5 and 7, who of theirs is out, and how many of each size they've
	 * requisitioned (each one raises that size's price for them: {@link JugcraftSquads#price}). */
	public record Account(UUID player, List<Integer> charges, List<UUID> field, List<Integer> bought) {
		static final Codec<Account> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("player").forGetter(Account::player),
				Codec.INT.listOf().fieldOf("charges").forGetter(Account::charges),
				UUIDUtil.CODEC.listOf().optionalFieldOf("field", List.of()).forGetter(Account::field),
				Codec.INT.listOf().optionalFieldOf("bought", List.of()).forGetter(Account::bought)).apply(i, Account::new));
	}

	static final Codec<SquadAccounts> CODEC = Account.CODEC.listOf().xmap(SquadAccounts::new, SquadAccounts::accounts);
	static final SavedDataType<SquadAccounts> TYPE = new SavedDataType<>(Jugcraft.id("drop_squads"), SquadAccounts::new, CODEC, null);

	private final Map<UUID, int[]> charges = new HashMap<>();
	private final Map<UUID, List<UUID>> field = new HashMap<>();
	private final Map<UUID, int[]> bought = new HashMap<>();

	public SquadAccounts() {
	}

	private SquadAccounts(List<Account> list) {
		for (Account a : list) {
			int[] c = new int[JugcraftSquads.SIZES.length];
			for (int k = 0; k < c.length && k < a.charges().size(); k++) {
				c[k] = a.charges().get(k);
			}
			charges.put(a.player(), c);
			if (!a.bought().isEmpty()) {
				int[] b = new int[JugcraftSquads.SIZES.length];
				for (int k = 0; k < b.length && k < a.bought().size(); k++) {
					b[k] = a.bought().get(k);
				}
				bought.put(a.player(), b);
			}
			if (!a.field().isEmpty()) {
				field.put(a.player(), new ArrayList<>(a.field()));
			}
		}
	}

	private List<Account> accounts() {
		List<Account> out = new ArrayList<>();
		java.util.Set<UUID> players = new java.util.HashSet<>(charges.keySet());
		players.addAll(field.keySet());
		players.addAll(bought.keySet());
		for (UUID p : players) {
			int[] c = charges.getOrDefault(p, new int[JugcraftSquads.SIZES.length]);
			List<Integer> cl = new ArrayList<>();
			for (int v : c) {
				cl.add(v);
			}
			List<Integer> bl = new ArrayList<>();
			for (int v : bought.getOrDefault(p, new int[0])) {
				bl.add(v);
			}
			out.add(new Account(p, cl, field.getOrDefault(p, List.of()), bl));
		}
		return out;
	}

	public static SquadAccounts get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public static SquadAccounts get(ServerLevel level) {
		return get(level.getServer());
	}

	/** The charges on {@code player}'s account for each squad size (a copy). */
	public int[] charges(UUID player) {
		return charges.getOrDefault(player, new int[JugcraftSquads.SIZES.length]).clone();
	}

	/** Adds a charge for squad {@code index}. */
	public void add(UUID player, int index) {
		charges.computeIfAbsent(player, k -> new int[JugcraftSquads.SIZES.length])[index]++;
		setDirty();
	}

	/** How many of each squad size {@code player} has requisitioned (a copy). */
	public int[] bought(UUID player) {
		return bought.getOrDefault(player, new int[JugcraftSquads.SIZES.length]).clone();
	}

	/** What squad {@code index} costs {@code player} now (up 10% for each of that size they've bought). */
	public int[] price(UUID player, int index) {
		return JugcraftSquads.price(index, bought(player)[index]);
	}

	/** Every size's price for {@code player} now, size by size. */
	public int[][] prices(UUID player) {
		int[][] out = new int[JugcraftSquads.SIZES.length][];
		for (int k = 0; k < out.length; k++) {
			out[k] = price(player, k);
		}
		return out;
	}

	/** Notes that {@code player} requisitioned squad {@code index} (its price goes up for them). */
	public void bought(UUID player, int index) {
		bought.computeIfAbsent(player, k -> new int[JugcraftSquads.SIZES.length])[index]++;
		setDirty();
	}

	/** Spends a charge for squad {@code index}; false if there was none. */
	public boolean spend(UUID player, int index) {
		int[] c = charges.get(player);
		if (c == null || c[index] <= 0) {
			return false;
		}
		c[index]--;
		setDirty();
		return true;
	}

	/**
	 * Who of {@code player}'s is in the field (pods on the way and legionaries), still there: the dead forgotten, and those
	 * nowhere loaded too (a squad left behind out of reach is recalled when its player calls another: each, loaded again,
	 * finds itself off the record and goes; {@link #listed}).
	 */
	public List<UUID> field(MinecraftServer server, UUID player) {
		List<UUID> out = field.get(player);
		if (out == null) {
			return List.of();
		}
		boolean changed = out.removeIf(id -> {
			for (ServerLevel level : server.getAllLevels()) {
				Entity e = level.getEntity(id);
				if (e != null) {
					return !e.isAlive();
				}
			}
			return true;
		});
		if (out.isEmpty()) {
			field.remove(player);
		}
		if (changed) {
			setDirty();
		}
		return List.copyOf(out);
	}

	/** Whether {@code id} is on {@code player}'s field record (one off it was recalled). */
	public boolean listed(UUID player, UUID id) {
		List<UUID> list = field.get(player);
		return list != null && list.contains(id);
	}

	/** Whether {@code player} has a squad out (any of theirs still in the field). */
	public boolean inField(MinecraftServer server, UUID player) {
		return !field(server, player).isEmpty();
	}

	/** Records {@code id} (a pod or a legionary) as {@code player}'s in the field. */
	public void out(UUID player, UUID id) {
		List<UUID> list = field.computeIfAbsent(player, k -> new ArrayList<>());
		if (!list.contains(id)) {
			list.add(id);
			setDirty();
		}
	}

	/** Forgets {@code id} (dead, sent home, a pod gone). */
	public void back(UUID player, UUID id) {
		List<UUID> list = field.get(player);
		if (list != null && list.remove(id)) {
			if (list.isEmpty()) {
				field.remove(player);
			}
			setDirty();
		}
	}

	/** Clears {@code player}'s field record (an operator's or a test's reset). */
	public void clearField(UUID player) {
		if (field.remove(player) != null) {
			setDirty();
		}
	}
}
