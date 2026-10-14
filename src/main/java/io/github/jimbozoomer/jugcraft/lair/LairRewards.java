package io.github.jimbozoomer.jugcraft.lair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * Lair bosses' rewards (Concordance Chunk A, A-06): when a boss falls, each player who qualified in that fight
 * ({@link Encounter}) is paid once. Its receipt is the fight and the player; however often a fall is reported the fight
 * pays once ({@link Encounter#claim}). Who is paid, and when:
 * <ul>
 * <li>alive and within {@value Encounter#REACH} blocks of the arena: at once, as before Chunk A;</li>
 * <li>one who died within reach while the fight was on: at once if alive, wherever they are; if still dead, when they
 * respawn; if away from the game, when they next join;</li>
 * <li>one who left the game within reach while the fight was on and is still away when the boss falls: when they next
 * join (one who came back first is paid like anyone else);</li>
 * <li>anyone else who walked away alive: nothing, as before.</li>
 * </ul>
 * What a player is owed is kept with them (the {@code jugcraft:lair_rewards} attachment, saved with the player, kept
 * through death) or, while they are away from the game, in the world's record (the overworld's saved data
 * {@code jugcraft:lair_rewards}, filed as {@code dimensions/minecraft/overworld/data/jugcraft/lair_rewards.dat}, at most
 * {@value #MAX_OWED} rewards a player). It is handed over once they are alive and not in a lair that has closed (one
 * who logs in there is sent home first). A reward is rolled when it is handed over, from the boss's own table (a first
 * kill and the Halloween event are judged then), and taken off the record before it is given; what does not fit in
 * their inventory lands at their feet, as before. The experience share is the boss's experience divided among everyone
 * paid, now or later.
 * <p>
 * Crash limits: the attachment and the inventory are saved together in the player's file, so what is handed into the
 * inventory is either still owed or given; what does not fit is dropped at their feet and saved with that chunk, so a
 * crash between those saves can lose it or let it be paid twice. Joining moves all of a player's kept rewards (up to
 * {@value #MAX_OWED}) off the world's record at once, across two files; a crash between their saves can lose them all,
 * or let them all be paid twice.
 */
public final class LairRewards {
	/** The most rewards waiting for one player (older ones go first). */
	public static final int MAX_OWED = 16;
	public static AttachmentType<Owed> OWED;
	private static final Map<String, Payer> PAYERS = new HashMap<>();

	/** Pays one player their reward from one boss: rolls it, gives it, gives the experience and the advancement. */
	@FunctionalInterface
	public interface Payer {
		void pay(ServerLevel level, Vec3 origin, ServerPlayer player, int experience);
	}

	/**
	 * A reward still to be paid: from which boss, which fight (the receipt), where the boss fell and the experience share.
	 */
	public record Due(String boss, UUID fight, Vec3 origin, int experience) {
		public static final Codec<Due> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("boss").forGetter(Due::boss),
				UUIDUtil.CODEC.fieldOf("fight").forGetter(Due::fight),
				Vec3.CODEC.fieldOf("origin").forGetter(Due::origin),
				Codec.INT.optionalFieldOf("experience", 0).forGetter(Due::experience)).apply(i, Due::new));
	}

	/** The rewards waiting for a player. */
	public record Owed(List<Due> due) {
		public static final Codec<Owed> CODEC = Saved.versioned("lair rewards owed", RecordCodecBuilder.create(i -> i.group(
				Due.CODEC.listOf().optionalFieldOf("due", List.of()).forGetter(Owed::due)).apply(i, Owed::new)));

		public Owed {
			due = List.copyOf(due);
		}

		/** These and {@code more}, without a second reward for the same fight, at most {@value #MAX_OWED}. */
		public Owed with(Due more) {
			List<Due> all = new ArrayList<>(due);
			if (all.stream().anyMatch(held -> held.fight().equals(more.fight()))) {
				return this;
			}
			all.add(more);
			while (all.size() > MAX_OWED) {
				all.removeFirst();
			}
			return new Owed(all);
		}
	}

	private LairRewards() {
	}

	static void register() {
		OWED = AttachmentRegistry.<Owed>builder().persistent(Owed.CODEC).copyOnDeath().buildAndRegister(Jugcraft.id("lair_rewards"));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> welcome(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handOver(newPlayer));
		// A reward owed to someone who stayed dead, or arrived, is handed over once they are alive (checked each second).
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (player.hasAttached(OWED)) {
						handOver(player);
					}
				}
			}
		});
	}

	/** Names the payer for a boss (at registration): {@code boss} is the name its rewards are kept under. */
	public static void payer(String boss, Payer payer) {
		PAYERS.put(boss, payer);
	}

	/**
	 * {@code boss} fell at {@code origin}: pays everyone who qualified in {@code fight} and is paid (above), sharing its
	 * {@code experience} out among them. Call only after {@link Encounter#claim} said yes. Returns who was paid at once.
	 */
	public static List<ServerPlayer> pay(ServerLevel level, String boss, Encounter fight, Vec3 origin, Vec3 centre, int experience) {
		List<ServerPlayer> now = new ArrayList<>();
		Payer payer = PAYERS.get(boss);
		if (payer == null) {
			return now;
		}
		MinecraftServer server = level.getServer();
		List<ServerPlayer> fallen = new ArrayList<>();
		List<UUID> away = new ArrayList<>();
		for (UUID id : fight.qualified()) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			boolean died = fight.excused(id);
			if (player == null) {
				// Away from the game: kept for them if they died or left the game in the fight.
				if (died || fight.away(id)) {
					away.add(id);
				}
			} else if (player.isAlive() && (died || (player.level() == level && player.position().distanceTo(centre) <= Encounter.REACH))) {
				now.add(player);
			} else if (!player.isAlive() && died) {
				fallen.add(player);
			}
		}
		int paid = now.size() + fallen.size() + away.size();
		if (paid == 0) {
			return now;
		}
		Due due = new Due(boss, fight.fight(), origin, Math.max(1, experience / paid));
		for (UUID id : away) {
			Data.of(server).owe(id, due);
		}
		for (ServerPlayer player : fallen) {
			owe(player, due);
		}
		for (ServerPlayer player : now) {
			payer.pay(player.level(), origin, player, due.experience());
		}
		return now;
	}

	/** Keeps {@code due} for {@code player}, to be handed over once they are alive. */
	public static void owe(ServerPlayer player, Due due) {
		Owed owed = player.getAttached(OWED);
		player.setAttached(OWED, owed == null ? new Owed(List.of(due)) : owed.with(due));
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.reward.kept"));
	}

	/**
	 * Pays a living player everything they are owed (each taken off first); false if there was nothing, they are dead,
	 * or they stand in a lair that has closed (they are about to be sent home, and what does not fit would be left there).
	 */
	public static boolean handOver(ServerPlayer player) {
		Owed owed = player.getAttached(OWED);
		if (owed == null || !player.isAlive() || (Lairs.isLair(player.level()) && Lairs.instance(player.getAttached(Lairs.VISIT)) == null)) {
			return false;
		}
		player.removeAttached(OWED);
		for (Due due : owed.due()) {
			Payer payer = PAYERS.get(due.boss());
			if (payer != null) {
				payer.pay(player.level(), due.origin(), player, due.experience());
			}
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.reward.returned"));
		return true;
	}

	/**
	 * A player joins (Fabric's join event calls this; tests too): what the world kept for them while they were away moves
	 * onto them, and is handed over if they are alive.
	 */
	public static void welcome(ServerPlayer player) {
		List<Due> kept = Data.of(player.level().getServer()).take(player.getUUID());
		if (kept.isEmpty()) {
			return;
		}
		Owed owed = player.getAttached(OWED);
		for (Due due : kept) {
			owed = owed == null ? new Owed(List.of(due)) : owed.with(due);
		}
		player.setAttached(OWED, owed);
		handOver(player);
	}

	/** Rewards kept for players away from the game when their boss fell (the overworld's saved data). */
	public static final class Data extends SavedData {
		public static final Codec<Data> CODEC = Saved.versioned("lair rewards kept", RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Owed.CODEC).optionalFieldOf("owed", Map.of()).forGetter(data -> data.owed))
				.apply(i, Data::new)));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("lair_rewards"), Data::new, CODEC, null);

		private final Map<UUID, Owed> owed = new LinkedHashMap<>();

		Data() {
			this(Map.of());
		}

		Data(Map<UUID, Owed> saved) {
			owed.putAll(saved);
		}

		public static Data of(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(TYPE);
		}

		/** Keeps {@code due} for {@code player} until they next join. */
		public void owe(UUID player, Due due) {
			Owed held = owed.get(player);
			owed.put(player, held == null ? new Owed(List.of(due)) : held.with(due));
			setDirty();
		}

		/** What is kept for {@code player}, taken off the record. */
		public List<Due> take(UUID player) {
			Owed held = owed.remove(player);
			if (held == null) {
				return List.of();
			}
			setDirty();
			return held.due();
		}

		/** What is kept for {@code player}, left on the record. */
		public List<Due> kept(UUID player) {
			Owed held = owed.get(player);
			return held == null ? List.of() : held.due();
		}
	}
}
