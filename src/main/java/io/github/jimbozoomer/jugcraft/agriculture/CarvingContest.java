package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The Halloween carving contest, decided on the server. A hand-carved pumpkin standing on a Judging Stand
 * is an entry once a player enters it (its carver, or anyone if it has forgotten its carver by being moved).
 * While the Halloween event runs ({@link HalloweenSeason}), every player has one vote per contest: using a
 * stand votes for its entrant (voting again moves the vote), never for oneself. Each Halloween is its own
 * contest ({@link HalloweenSeason#year()}). Once the event ends (or a later Halloween begins), the entrants
 * with the most votes get the Harvest Scale's First, Second and Third Prize Ribbons; a winner who is offline
 * gets theirs on their next visit. Votes, results and prizes still owed are saved with the world, and every
 * contest is awarded once.
 */
public final class CarvingContest {
	public static final int PLACES = HarvestScaleBlockEntity.BOARD;
	/** How often the server checks whether a contest is over and who is owed a prize. */
	public static final int CHECK_TICKS = 200;
	/** At most this many voters per contest and this many contests are kept. */
	public static final int MAX_VOTERS = 4096;
	public static final int MAX_CONTESTS = 8;

	public enum Result {
		ENTERED, ALREADY_ENTERED, NOT_YOURS, VOTED, MOVED, SAME, OWN_ENTRY, NO_ENTRY, NO_PUMPKIN, CLOSED, FULL
	}

	/** Entries, votes and prizes are server-side; the client only sees the stand and the messages. */

	/** One place in the standings. */
	public record Standing(UUID entrant, String name, int votes) {
	}

	private CarvingContest() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				check(server);
			}
		});
	}

	/** The hand-carved pumpkin standing on the stand at {@code stand}, or null. */
	static CarvedPumpkinBlockEntity pumpkinOn(ServerLevel level, BlockPos stand) {
		BlockState above = level.getBlockState(stand.above());
		return above.getBlock() instanceof CarvedPumpkinBlock && level.getBlockEntity(stand.above()) instanceof CarvedPumpkinBlockEntity pumpkin
				&& !pumpkin.carving().isBlank() ? pumpkin : null;
	}

	/** Enters the carving on the stand for {@code player}: its carver may, or anyone if the pumpkin no longer knows its carver. */
	public static Result enter(ServerPlayer player, BlockPos stand) {
		ServerLevel level = player.level();
		if (!(level.getBlockEntity(stand) instanceof JudgingStandBlockEntity entry)) {
			return Result.NO_PUMPKIN;
		}
		CarvedPumpkinBlockEntity pumpkin = pumpkinOn(level, stand);
		if (pumpkin == null) {
			return Result.NO_PUMPKIN;
		}
		if (entry.entrant().isPresent()) {
			return Result.ALREADY_ENTERED;
		}
		if (pumpkin.carverId().isPresent() && !pumpkin.carverId().get().equals(player.getUUID())) {
			return Result.NOT_YOURS;
		}
		entry.enter(player.getUUID(), player.getName().getString());
		return Result.ENTERED;
	}

	/** Votes for the entrant of the stand at {@code stand}, while the event runs: one vote per player per contest. */
	public static Result vote(ServerPlayer voter, BlockPos stand) {
		ServerLevel level = voter.level();
		if (!HalloweenSeason.active()) {
			return Result.CLOSED;
		}
		CarvedPumpkinBlockEntity pumpkin = pumpkinOn(level, stand);
		if (!(level.getBlockEntity(stand) instanceof JudgingStandBlockEntity entry) || pumpkin == null) {
			return Result.NO_PUMPKIN;
		}
		if (entry.entrant().isEmpty()) {
			return Result.NO_ENTRY;
		}
		UUID entrant = entry.entrant().get();
		// Someone else recarved the entered pumpkin: the entry no longer stands.
		if (pumpkin.carverId().isPresent() && !pumpkin.carverId().get().equals(entrant)) {
			entry.clear();
			return Result.NO_ENTRY;
		}
		if (entrant.equals(voter.getUUID())) {
			return Result.OWN_ENTRY;
		}
		Data data = data(level.getServer());
		int year = HalloweenSeason.year();
		if (data.get(year) != null && data.get(year).awarded) {
			return Result.CLOSED; // This Halloween's contest was already decided (the event was switched off and on again).
		}
		Contest contest = data.contest(year);
		UUID before = contest.votes.get(voter.getUUID());
		if (before == null && contest.votes.size() >= MAX_VOTERS) {
			return Result.FULL;
		}
		contest.votes.put(voter.getUUID(), entrant);
		contest.names.put(entrant, entry.entrantName());
		data.setDirty();
		return before == null ? Result.VOTED : before.equals(entrant) ? Result.SAME : Result.MOVED;
	}

	/** This Halloween's standings, most votes first (ties by name). */
	public static List<Standing> standings(MinecraftServer server, int year) {
		Contest contest = data(server).contests.get(year);
		return contest == null ? List.of() : contest.standings();
	}

	/** Awards every contest that is over, and hands out prizes owed to players who are online. */
	public static void check(MinecraftServer server) {
		Data data = data(server);
		int year = HalloweenSeason.year();
		boolean running = HalloweenSeason.active();
		for (Map.Entry<Integer, Contest> entry : data.contests.entrySet()) {
			Contest contest = entry.getValue();
			if (!contest.awarded && (!running || entry.getKey() < year)) {
				award(server, data, entry.getKey(), contest);
			}
		}
		for (Iterator<Map.Entry<UUID, List<String>>> it = data.prizes.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, List<String>> owed = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(owed.getKey());
			if (player == null) {
				continue;
			}
			for (String ribbon : owed.getValue()) {
				ItemStack stack = new ItemStack(JugcraftAgriculture.item(ribbon));
				player.sendSystemMessage(Component.translatable("message.jugcraft.carving_contest.prize", stack.getHoverName()));
				if (!player.getInventory().add(stack)) {
					player.spawnAtLocation(player.level(), stack); // Full inventory: drop it at their feet.
				}
			}
			it.remove();
			data.setDirty();
		}
	}

	private static void award(MinecraftServer server, Data data, int year, Contest contest) {
		contest.awarded = true;
		data.setDirty();
		List<Standing> standings = contest.standings();
		if (standings.isEmpty()) {
			return;
		}
		server.getPlayerList().broadcastSystemMessage(Component.translatable("message.jugcraft.carving_contest.over", year), false);
		for (int place = 0; place < Math.min(PLACES, standings.size()); place++) {
			Standing standing = standings.get(place);
			data.prizes.computeIfAbsent(standing.entrant(), id -> new ArrayList<>()).add(HarvestScaleBlockEntity.RIBBONS.get(place));
			server.getPlayerList().broadcastSystemMessage(Component.translatable("message.jugcraft.carving_contest.place",
					place + 1, standing.name(), standing.votes()), false);
		}
	}

	public static Data data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
	}

	/** One Halloween's contest: who voted for whom, the entrants' names, and whether it was awarded. */
	public static final class Contest {
		static final Codec<Contest> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC).fieldOf("votes").forGetter(c -> c.votes),
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).fieldOf("names").forGetter(c -> c.names),
				Codec.BOOL.fieldOf("awarded").forGetter(c -> c.awarded)).apply(i, Contest::new));

		final Map<UUID, UUID> votes;
		final Map<UUID, String> names;
		boolean awarded;

		Contest() {
			this(Map.of(), Map.of(), false);
		}

		Contest(Map<UUID, UUID> votes, Map<UUID, String> names, boolean awarded) {
			this.votes = new LinkedHashMap<>();
			votes.entrySet().stream().limit(MAX_VOTERS).forEach(e -> this.votes.put(e.getKey(), e.getValue()));
			this.names = new HashMap<>(names);
			this.awarded = awarded;
		}

		public boolean awarded() {
			return awarded;
		}

		public List<Standing> standings() {
			Map<UUID, Integer> counts = new HashMap<>();
			votes.values().forEach(entrant -> counts.merge(entrant, 1, Integer::sum));
			List<Standing> out = new ArrayList<>();
			counts.forEach((entrant, n) -> out.add(new Standing(entrant, names.getOrDefault(entrant, "?"), n)));
			out.sort(Comparator.comparingInt(Standing::votes).reversed().thenComparing(Standing::name).thenComparing(Standing::entrant));
			return out;
		}
	}

	/** Every contest kept (the latest {@link #MAX_CONTESTS}) and the prizes still owed (data/jugcraft_carving_contest.dat). */
	public static final class Data extends SavedData {
		/** A contest's year as a map key: a bad key is a decoding error, not an exception. */
		private static final Codec<Integer> YEAR = Codec.STRING.comapFlatMap(text -> {
			try {
				return DataResult.success(Integer.parseInt(text));
			} catch (NumberFormatException e) {
				return DataResult.error(() -> "Not a contest year: " + text);
			}
		}, String::valueOf);
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(YEAR, Contest.CODEC).fieldOf("contests").forGetter(d -> d.contests),
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING.listOf()).fieldOf("prizes").forGetter(d -> d.prizes))
				.apply(i, Data::new));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("carving_contest"), Data::new, CODEC, null);

		final TreeMap<Integer, Contest> contests = new TreeMap<>();
		final Map<UUID, List<String>> prizes = new HashMap<>();

		Data() {
		}

		Data(Map<Integer, Contest> contests, Map<UUID, List<String>> prizes) {
			this.contests.putAll(contests);
			while (this.contests.size() > MAX_CONTESTS) {
				this.contests.pollFirstEntry();
			}
			prizes.forEach((player, ribbons) -> this.prizes.put(player, new ArrayList<>(ribbons)));
		}

		Contest contest(int year) {
			Contest contest = contests.computeIfAbsent(year, y -> new Contest());
			while (contests.size() > MAX_CONTESTS) {
				contests.pollFirstEntry();
			}
			return contest;
		}

		public Contest get(int year) {
			return contests.get(year);
		}

		/** Prizes still owed to a player (ribbon item IDs). */
		public List<String> owed(UUID player) {
			return List.copyOf(prizes.getOrDefault(player, List.of()));
		}
	}
}
