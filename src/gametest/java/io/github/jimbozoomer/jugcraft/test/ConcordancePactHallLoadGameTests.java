package io.github.jimbozoomer.jugcraft.test;

import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.collectionMillis;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.collections;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.count;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.half;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.kib;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.mib;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.ms;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.retained;
import static io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.slope;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Roster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.Measure;
import io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.Stats;
import io.github.jimbozoomer.jugcraft.test.ConcordanceWorkshopLoadGameTests.Traffic;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SystemReport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-09 (D-P128 to D-P132): the pact hall's load card, beside Chunk C's workshop card
 * (ConcordanceWorkshopLoadGameTests), whose Measure, Traffic, Stats and static helpers it shares. The same world is measured
 * twice: first with its nine players standing where they will stand, then with the pact hall built round them. The hall:
 * eight holders, each keeping Stolas's pact at a Pact Perch they placed (sealed under the shipped offer as unit 2's tests
 * seal it, the compact left out), four offerings in its escrow, so each perch looks at its pact once a second and each
 * Stolas thinks once a second; a ninth perch where a guest is negotiating, a visiting Stolas listening; holder 0 reading
 * their own pact (the second open conversation); holder 1's roster read every ROSTER_TICKS, as its screen asks; and four
 * workshop hands (holders 0 to 3), each sealed at a Spirit Anchor bound by Porter Key to its holder's Courier Post,
 * carrying a load of glowstone from a chest five blocks away whenever its holder files a request (every REQUEST_TICKS,
 * staggered). The test empties each post back into its chest, as a hopper below it would (scaffolding: nothing is made or
 * lost). Each span is a warm-up, then MEASURED ticks read from MinecraftServer's own 100-tick ring of tick times, less this
 * test's own work in each tick: the simulated players' requests, filings and restocking are that work, and are timed
 * apart. After the spans, in one unmeasured tick, it times a perch's look, a Stolas's think, a pact page and a roster
 * listing call by call. It records the runner, the world, the terms, the counts, memory, D-09's rules sampled every 100
 * ticks while measured, every item in, out, carried and kept, and what holder 0 is sent, as "Pact hall load card | ...".
 * <p>
 * It measures only when Fabric's filter selects it and nothing else (docs/TESTING.md); in every other run it passes at
 * once, so ordinary runs are not lengthened. In CI it runs after the workshop's card in the same opted-in job, in a server
 * of its own. The game test server runs its ticks back to back (GameTestServer.waitUntilNextTick), so the figures are each
 * tick's work on this runner only: a CI runner's timings are not a player's hardware.
 */
public class ConcordancePactHallLoadGameTests {
	/** Fabric's id for loadCard: the test mod's id, then the class and method in snake case (TestAnnotationLocator). */
	public static final String ID = "jugcraft-test:concordance_pact_hall_load_game_tests_load_card";
	/** Fabric API's selection property (GameTestSystemProperties.FILTER, an impl class not imported here). */
	static final String FILTER = "fabric-api.gametest.filter";
	/** The workshop card's: its Measure is sized by its own two, which these equal (check_pact_hall_load_card). */
	static final int WINDOW = 100;
	static final int MEASURED = 6_000;
	static final int BASE_WARMUP = 2_000;
	static final int HALL_WARMUP = 1_500;
	/** A warm-up also waits until the loaded chunks have held steady this long. */
	static final int SETTLE = 400;
	static final int WARMUP_CAP = 10_000;
	static final int AUDIT = 1_000;
	static final int MAX_TICKS = 33_000;
	/** The yard: a stone floor from OFF to OFF + SIZE in x and z, barriers round its edge. */
	static final int SIZE = 43;
	static final int OFF = 10;
	/** D-09's hall: eight holders, each keeping a pact with Stolas; the first four also keep a workshop hand. */
	static final int HOLDERS = 8;
	static final int HANDS = 4;
	/** Offerings each holder pays into their pact's escrow at the start: Stolas's cap (stolas.json: one a period, four ahead). */
	static final int ESCROW = 4;
	/** Glowstone each workshop's chest starts with; one request asks for one LOAD, the hand's carry. */
	static final int SUPPLY = 128;
	static final int LOAD = 64;
	/** A holder files a request at their post this often, if none is open there (hand w at tick 100 w), and each hand carries at least MIN_LOADS while measured. */
	static final int REQUEST_TICKS = 400;
	static final int MIN_LOADS = 10;
	/** An open conversation's reader asks again this often: well inside PactSessions.TIMEOUT_TICKS, so it stays open. */
	static final int CONVERSE_TICKS = 200;
	/** The open roster is read again this often, as its screen asks (client RosterClient.REFRESH_TICKS, D-P118). */
	static final int ROSTER_TICKS = 40;
	/** Calls timed one by one after the spans: looks and thinks CALLS each, pages and roster listings READS each, after as many untimed. */
	static final int CALLS = 1_000;
	static final int READS = 200;
	/** D-P129, in C-P67's form, proposed: reported here, not asserted. */
	static final double BUDGET_MEDIAN_MS = 1.0;
	static final double BUDGET_P95_MS = 2.5;
	static final double BUDGET_RETAINED_MIB = 4.0;
	/** What is asserted: generous limits a shared CI runner should never reach unless the work has grown (C's). */
	static final double LIMIT_MEDIAN_MS = 5.0;
	static final double LIMIT_P95_MS = 12.5;
	static final double LIMIT_RETAINED_MIB = 64.0;
	/** The pattern every player has observed (a guest must have, to be invited: PactMenu.observed). */
	static final String PATTERN = "jugcraft:full_moon";

	/** A yard position: x and z from the yard's corner, y as given. */
	static BlockPos p(int x, int y, int z) {
		return new BlockPos(OFF + x, y, OFF + z);
	}

	/** Holder {@code i}'s perch, in a row five blocks apart; they stand two blocks south of it (well inside Pacts.REACH). */
	static BlockPos perchAt(int i) {
		return p(4 + 5 * i, 2, 6);
	}

	static BlockPos standAt(int i) {
		return perchAt(i).south(2);
	}

	/** The guest's perch, keeping nothing, and where the guest stands. */
	static final BlockPos GUEST_PERCH = p(21, 2, 15);
	static final BlockPos GUEST_AT = GUEST_PERCH.south(2);

	/**
	 * Workshop {@code w}'s Courier Post. Its chest is five blocks east (beyond the hand's 2.5-block reach, so it walks), its
	 * Spirit Anchor three blocks south, and its hand starts beside the post. The workshops lie 20 blocks apart east to west
	 * and 12 north to south, so no post has another workshop's chest within Couriers.SOURCE_RADIUS (8).
	 */
	static BlockPos postAt(int w) {
		return p(4 + 20 * (w % 2), 2, 24 + 12 * (w / 2));
	}

	static BlockPos chestAt(int w) {
		return postAt(w).east(5);
	}

	static BlockPos anchorAt(int w) {
		return postAt(w).south(3);
	}

	static BlockPos handAt(int w) {
		return postAt(w).offset(1, 0, 1);
	}

	static boolean selected() {
		return ID.equals(System.getProperty(FILTER));
	}

	@GameTest(maxTicks = MAX_TICKS, skyAccess = true)
	public void loadCard(GameTestHelper helper) {
		if (!selected()) {
			helper.succeed();
			return;
		}
		new Card(helper).begin();
	}

	private enum Phase {
		BASE_WARMUP, BASE, HALL_WARMUP, HALL
	}

	/** The simulated players' actions, timed apart while measured. */
	private enum Action {
		ANSWER, ROSTER, FILE, RESTOCK
	}

	/** The run: one driver callback a tick (runAfterDelay), baseline first, then the hall: C's card's rhythm, for this yard and its nine players. */
	private static final class Card {
		final GameTestHelper helper;
		final ServerLevel level;
		final MinecraftServer server;
		/** The test's own nanoseconds in each tick of the ring, by tick % WINDOW, taken out of the server's figure. */
		final long[] own = new long[WINDOW];
		final List<Signs.Shown> shown = new ArrayList<>();
		final List<String> runner = new ArrayList<>();
		final List<ChunkPos> forced = new ArrayList<>();
		/** The eight holders (holder 0 is the observed player), then the guest; each one's channel at the same index. */
		final List<ServerPlayer> players = new ArrayList<>();
		final List<EmbeddedChannel> channels = new ArrayList<>();
		/** Messages the other eight players were sent while measured: in the baseline, in the hall. */
		final long[] others = new long[2];
		int yardChunks;
		int forcedCount;
		StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec;
		AutoCloseable signs;
		Phase phase = Phase.BASE_WARMUP;
		int phaseTicks;
		int expected = -1;
		int chunks = -1;
		int steadySince;
		Measure base;
		Measure hall;
		Measure current;
		Hall work;
		boolean advanceTime;
		boolean advanceWeather;
		boolean spawnMobs;
		boolean spawnTraders;

		Card(GameTestHelper helper) {
			this.helper = helper;
			this.level = helper.getLevel();
			this.server = level.getServer();
		}

		void begin() {
			helper.assertTrue(server.getPlayerList().getPlayerCount() == 0, "The load card runs alone: no other test's player is here");
			helper.assertTrue(Workers.enabled() && PactPerchBlockEntity.definition() != null && WorkshopHandEntity.terms() != null,
					"The Concordance is on, and Stolas's offer and the workshop hand's terms are loaded");
			describeRunner();
			hold();
			force();
			ground();
			join();
			signs = Signs.observe(sign -> {
				if (current != null && sign.level() == level) {
					shown.add(sign);
				}
			});
			helper.runAfterDelay(1, this::tick);
		}

		void describeRunner() {
			String report = new SystemReport().toLineSeparatedString();
			for (String key : List.of("Minecraft Version", "Operating System", "Java Version", "Java VM Version", "Processor Vendor",
					"Processor Name", "Frequency (GHz)", "Number of physical packages", "Number of physical CPUs",
					"Number of logical CPUs", "Memory", "JVM Flags")) {
				report.lines().filter(line -> line.startsWith(key + ": ")).findFirst().ifPresent(runner::add);
			}
			runner.add("max heap " + mib(Runtime.getRuntime().maxMemory()) + " MiB; collectors " + ManagementFactory.getGarbageCollectorMXBeans()
					.stream().map(GarbageCollectorMXBean::getName).collect(Collectors.joining(", ")));
			runner.add("JVM arguments: " + String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
		}

		/** Noon held (inside the hand's hours, 0 to 12000), weather held, no mobs or wandering traders: both spans see the same world. Given back at the end. */
		void hold() {
			GameRules rules = level.getGameRules();
			advanceTime = rules.get(GameRules.ADVANCE_TIME);
			advanceWeather = rules.get(GameRules.ADVANCE_WEATHER);
			spawnMobs = rules.get(GameRules.SPAWN_MOBS);
			spawnTraders = rules.get(GameRules.SPAWN_WANDERING_TRADERS);
			rules.set(GameRules.ADVANCE_TIME, false, server);
			rules.set(GameRules.ADVANCE_WEATHER, false, server);
			rules.set(GameRules.SPAWN_MOBS, false, server);
			rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
			helper.setTime(6000L);
		}

		void release() {
			GameRules rules = level.getGameRules();
			rules.set(GameRules.ADVANCE_TIME, advanceTime, server);
			rules.set(GameRules.ADVANCE_WEATHER, advanceWeather, server);
			rules.set(GameRules.SPAWN_MOBS, spawnMobs, server);
			rules.set(GameRules.SPAWN_WANDERING_TRADERS, spawnTraders, server);
		}

		/** Every chunk under the yard ticks (the game test server's simulation distance is 0). */
		void force() {
			BlockPos low = helper.absolutePos(p(0, 0, 0));
			BlockPos high = helper.absolutePos(p(SIZE, 0, SIZE));
			for (int cx = Math.min(low.getX(), high.getX()) >> 4; cx <= Math.max(low.getX(), high.getX()) >> 4; cx++) {
				for (int cz = Math.min(low.getZ(), high.getZ()) >> 4; cz <= Math.max(low.getZ(), high.getZ()) >> 4; cz++) {
					ChunkPos chunk = new ChunkPos(cx, cz);
					yardChunks++;
					if (!level.getChunkSource().getForceLoadedChunks().contains(chunk.pack())) {
						level.setChunkForced(cx, cz, true);
						forced.add(chunk);
					}
				}
			}
			forcedCount = forced.size();
		}

		void unforce() {
			for (ChunkPos chunk : forced) {
				level.setChunkForced(chunk.x(), chunk.z(), false);
			}
			forced.clear();
		}

		void ground() {
			for (int x = 0; x <= SIZE; x++) {
				for (int z = 0; z <= SIZE; z++) {
					boolean edge = x == 0 || z == 0 || x == SIZE || z == SIZE;
					helper.setBlock(p(x, 1, z), edge ? Blocks.BARRIER : Blocks.STONE);
					if (edge) {
						helper.setBlock(p(x, 2, z), Blocks.BARRIER);
						helper.setBlock(p(x, 3, z), Blocks.BARRIER);
					}
				}
			}
		}

		/** The nine players, in both spans: joined as C's one is (makeMockServerPlayerInLevel's way), in survival, each channel kept and drained every tick. */
		void join() {
			for (int i = 0; i <= HOLDERS; i++) {
				boolean guest = i == HOLDERS;
				ServerPlayer player = player(guest ? "pact-guest" : "pact-holder-" + i, guest ? GUEST_AT : standAt(i));
				ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
				ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
				// A hand's holder has mastered Stolas's Regard (WorkshopHandEntity.regarded); everyone else has understood it (PactMenu.knows).
				ConcordanceProgress.grant(player, StolasRegard.RESEARCH, !guest && i < HANDS ? ResearchState.MASTERED : ResearchState.UNDERSTOOD);
				AstralClaims.of(server).observe(player.getUUID(), PATTERN, 0L);
				RateGate.forget(player.getUUID());
			}
			codec = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(level.registryAccess())).codec();
		}

		ServerPlayer player(String name, BlockPos at) {
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
			ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
			Connection connection = new Connection(PacketFlow.SERVERBOUND);
			channels.add(new EmbeddedChannel(new ChannelHandler[] {connection}));
			server.getPlayerList().placeNewPlayer(connection, player, cookie);
			player.setGameMode(GameType.SURVIVAL);
			BlockPos to = helper.absolutePos(at);
			player.snapTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.0F, 0.0F);
			level.getChunkSource().move(player);
			helper.assertTrue(!player.isCreative() && !player.hasInfiniteMaterials(), "Each player is an ordinary survival player: " + name);
			players.add(player);
			return player;
		}

		void tick() {
			long began = System.nanoTime();
			int now = server.getTickCount();
			helper.assertTrue(expected < 0 || now == expected, "The driver ran every tick, so the windows line up: " + now + " after " + (expected - 1));
			expected = now + 1;
			for (ServerPlayer player : players) {
				player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
			}
			drain();
			if (work != null) {
				work.drive(now);
			}
			phaseTicks++;
			if (!advance(now)) {
				helper.runAfterDelay(1, this::tick);
			}
			own[Math.floorMod(now, WINDOW)] = System.nanoTime() - began;
		}

		/** Every player's channel emptied; what holder 0 is sent is sized (C's Traffic), the others' messages only counted. */
		void drain() {
			Traffic traffic = current == null ? null : current.traffic;
			for (int i = 0; i < channels.size(); i++) {
				EmbeddedChannel channel = channels.get(i);
				Object message;
				while ((message = channel.readOutbound()) != null) {
					if (traffic == null || !(message instanceof Packet<?> packet)) {
						continue;
					}
					if (i == 0) {
						traffic.add(packet, codec);
					} else {
						others[current == base ? 0 : 1]++;
					}
				}
			}
			for (Signs.Shown sign : shown) {
				if (traffic != null && sign.at().distanceToSqr(players.get(0).position()) <= Presentation.FAR * Presentation.FAR) {
					traffic.sign(sign, codec);
				}
			}
			shown.clear();
		}

		boolean advance(int now) {
			switch (phase) {
				case BASE_WARMUP -> {
					if (settled(BASE_WARMUP)) {
						base = open("baseline", now);
						phase = Phase.BASE;
					}
				}
				case BASE -> {
					if (window(base, now) && base.windows == MEASURED / WINDOW) {
						close(base);
						work = new Hall(this);
						work.build();
						phase = Phase.HALL_WARMUP;
						phaseTicks = 0;
						chunks = -1;
					}
				}
				case HALL_WARMUP -> {
					if (settled(HALL_WARMUP)) {
						hall = open("pact hall", now);
						work.mark();
						phase = Phase.HALL;
					}
				}
				case HALL -> {
					if (window(hall, now) && hall.windows == MEASURED / WINDOW) {
						close(hall);
						finish();
						return true;
					}
				}
			}
			return false;
		}

		boolean settled(int least) {
			int loaded = level.getChunkSource().getLoadedChunksCount();
			if (loaded != chunks) {
				chunks = loaded;
				steadySince = phaseTicks;
			}
			helper.assertTrue(phaseTicks <= WARMUP_CAP, "The world settled within " + WARMUP_CAP + " ticks (" + loaded + " chunks, changing)");
			return phaseTicks >= least && phaseTicks - steadySince >= SETTLE;
		}

		/** Opens a span after this tick, which is not measured: a full collection first, for the retained heap. */
		Measure open(String name, int now) {
			Measure measure = new Measure(name);
			measure.retainedBefore = retained();
			measure.collections = -collections();
			measure.collectionMillis = -collectionMillis();
			measure.chunksBefore = level.getChunkSource().getLoadedChunksCount();
			measure.warmup = phaseTicks;
			measure.start = now + 1;
			current = measure;
			return measure;
		}

		/** At each window's end, the ring's 100 ticks: what the server measured, and that less the test's own work. */
		boolean window(Measure measure, int now) {
			int since = now - measure.start;
			if (since <= 0 || since % WINDOW != 0) {
				return false;
			}
			long[] ring = server.getTickTimesNanos();
			for (int tick = now - WINDOW; tick < now; tick++) {
				int slot = Math.floorMod(tick, WINDOW);
				int index = tick - measure.start;
				measure.raw[index] = ring[slot];
				measure.net[index] = Math.max(0L, ring[slot] - own[slot]);
				measure.own += own[slot];
			}
			measure.heap[measure.windows++] = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			return true;
		}

		void close(Measure measure) {
			measure.collections += collections();
			measure.collectionMillis += collectionMillis();
			measure.chunksAfter = level.getChunkSource().getLoadedChunksCount();
			measure.retainedAfter = retained();
			current = null;
		}

		/** Its players leave the game (PlayerList.remove, as a logout: their conversations and roster forgotten). */
		void leave() {
			for (ServerPlayer player : players) {
				if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
					server.getPlayerList().remove(player);
				}
			}
		}

		void finish() {
			try {
				signs.close();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			work.audit("the end");
			Hall.Tally end = work.tally();
			Hall.Calls calls = work.timeCalls();
			release();
			unforce();
			Stats b = Stats.of(base.net);
			Stats h = Stats.of(hall.net);
			double deltaMedian = h.median() - b.median();
			double deltaP95 = h.p95() - b.p95();
			boolean memory = base.retainedBefore >= 0 && base.retainedAfter >= 0 && hall.retainedBefore >= 0 && hall.retainedAfter >= 0;
			double retained = memory ? mib(hall.retainedAfter - hall.retainedBefore) - mib(base.retainedAfter - base.retainedBefore) : 0;
			report(b, h, end, calls, deltaMedian, deltaP95, memory, retained);
			leave();
			Hall.Tally m = work.marked;
			int[] loads = work.loads(end, m);
			helper.assertTrue(work.problems.isEmpty(), "D-09's rules held and every item is accounted for: " + work.problems);
			helper.assertTrue(Arrays.stream(loads).min().orElse(0) >= MIN_LOADS,
					"Each workshop hand carried at least " + MIN_LOADS + " loads while measured: " + Arrays.toString(loads));
			helper.assertTrue(end.answers() - m.answers() >= 2 * (MEASURED / CONVERSE_TICKS) && end.reads() - m.reads() >= MEASURED / ROSTER_TICKS,
					"Both conversations were answered, and the roster read, on schedule while measured: " + (end.answers() - m.answers()) + ", "
							+ (end.reads() - m.reads()));
			helper.assertTrue(deltaMedian <= LIMIT_MEDIAN_MS && deltaP95 <= LIMIT_P95_MS,
					String.format(Locale.ROOT, "Within the asserted limits: median %+.3f ms (%.1f), p95 %+.3f ms (%.1f)", deltaMedian, LIMIT_MEDIAN_MS, deltaP95, LIMIT_P95_MS));
			helper.assertTrue(!memory || retained <= LIMIT_RETAINED_MIB, "Retained heap within " + LIMIT_RETAINED_MIB + " MiB of the baseline's: " + retained);
			helper.succeed();
		}

		void report(Stats b, Stats h, Hall.Tally end, Hall.Calls calls, double deltaMedian, double deltaP95, boolean memory, double retained) {
			Hall.Tally m = work.marked;
			Traffic tb = base.traffic;
			Traffic th = hall.traffic;
			double seconds = MEASURED / 20.0;
			int[] loads = work.loads(end, m);
			PactDefinition offer = PactPerchBlockEntity.definition();
			WorkerDefinition.Spirit hand = WorkshopHandEntity.terms();
			List<String> lines = new ArrayList<>();
			lines.add("===== Concordance Chunk D pact hall load card (D-09) =====");
			lines.add("build: Jugcraft " + FabricLoader.getInstance().getModContainer("jugcraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")
					+ "; Jade " + (FabricLoader.getInstance().isModLoaded("jade") ? "loaded" : "absent"));
			for (String line : runner) {
				lines.add("runner: " + line);
			}
			lines.add("world: game test server (flat world, ticks run back to back); noon and weather held, mobs and wandering traders off, random tick speed "
					+ level.getGameRules().get(GameRules.RANDOM_TICK_SPEED) + "; player list view " + server.getPlayerList().getViewDistance()
					+ " and simulation " + server.getPlayerList().getSimulationDistance() + " (the chunk map views at least 2); the yard's "
					+ yardChunks + " chunks force-loaded (" + forcedCount + " by the card); players " + players.size() + " in both spans (" + HOLDERS
					+ " holders and a guest: server-side test players, no client)");
			lines.add(String.format(Locale.ROOT, "spans: baseline from tick %d, pact hall from tick %d; each %d ticks in %d windows of %d after warm-ups of %d and %d ticks "
					+ "(at least %d and %d, then until the loaded chunk count held %d ticks); tick times net of the test's own work, the simulated players' "
					+ "requests, filings and restocking included (timed apart below)",
					base.start, hall.start, MEASURED, MEASURED / WINDOW, WINDOW, base.warmup, hall.warmup, BASE_WARMUP, HALL_WARMUP, SETTLE));
			lines.add("tick ms, net of the test's own work | median p95 mean max over-50ms | 100-tick window means: median p95");
			for (Measure measure : List.of(base, hall)) {
				Stats s = measure == base ? b : h;
				lines.add(String.format(Locale.ROOT, "%s %.3f %.3f %.3f %.1f %d | %.3f %.3f", measure.name, s.median(), s.p95(), s.mean(), s.max(), s.over50(),
						s.windowMedian(), s.windowP95()));
			}
			lines.add(String.format(Locale.ROOT, "raw medians %.3f and %.3f ms; the test's own work %.3f and %.3f ms a tick",
					Stats.of(base.raw).median(), Stats.of(hall.raw).median(), ms((double) base.own / MEASURED), ms((double) hall.own / MEASURED)));
			lines.add(String.format(Locale.ROOT, "delta: median %+.3f ms, p95 %+.3f ms; baseline drift (second half less first) %+.3f ms",
					deltaMedian, deltaP95, half(base.net, true) - half(base.net, false)));
			lines.add(memory ? String.format(Locale.ROOT, "memory: retained after a full collection: baseline %d -> %d MiB, pact hall %d -> %d MiB (%+.0f MiB beyond the baseline's change); "
					+ "heap in use %+.2f and %+.2f MiB per 1,000 ticks; collections %d (%d ms) and %d (%d ms)", mib(base.retainedBefore), mib(base.retainedAfter),
					mib(hall.retainedBefore), mib(hall.retainedAfter), retained, slope(base.heap), slope(hall.heap), base.collections, base.collectionMillis,
					hall.collections, hall.collectionMillis) : "memory: retained heap not measured (the JVM ignored System.gc())");
			lines.add("chunks loaded: baseline " + base.chunksBefore + " -> " + base.chunksAfter + ", pact hall " + hall.chunksBefore + " -> " + hall.chunksAfter);
			lines.add("counts: " + String.join(", ", work.counts));
			lines.add("level at the end: " + level.getWatchdogStats());
			lines.add(String.format(Locale.ROOT, "active terms: Stolas's pact %s (radius %d, hours %d to %d, %d a day; upkeep %d of #%s every %d ticks, grace %d, "
					+ "up to %d ahead); the workshop hand's courier agreement (radius %d, hours %d to %d, %d loads a day, %d a trip)", offer.work(), offer.radius(),
					offer.from(), offer.to(), offer.quota(), offer.count(), offer.offering(), offer.period(), offer.grace(), offer.cap(), hand.radius(), hand.from(),
					hand.to(), hand.quota(), hand.carry()));
			lines.add(String.format(Locale.ROOT, "the hall's rules (by the code's constants, not sampled: each Stolas thinks every %d ticks, each perch "
					+ "looks every %d, writing at most every %d); sampled every 100 ticks while measured (%d samples): one presence a pact, one pact a "
					+ "holder, the visitor listening and each hand its anchor's at every one (problems below); the oldest last look seen %d ticks; "
					+ "Stolas %s; hands %s (at their day's quota %d)", WorkerEntity.THINK_TICKS, PactPerchBlockEntity.LOOK_TICKS, Pacts.WRITE_TICKS,
					work.sampled, work.oldestLook, work.stolasStates, work.handStates, work.atQuota));
			lines.add(String.format(Locale.ROOT, "work while measured: loads %d (by hand %s; a request is one load of %d), glowstone delivered %d, requests filed %d, "
					+ "conversation answers %d, roster reads %d (%d rows at the last)", Arrays.stream(loads).sum(), Arrays.toString(loads), LOAD,
					end.delivered() - m.delivered(), end.filed() - m.filed(), end.answers() - m.answers(), end.reads() - m.reads(), work.rosterRows));
			lines.add(String.format(Locale.ROOT, "the players' actions, timed apart (part of the test's own work, so not in the tick figures above): answers %s, "
					+ "roster reads %s, filings %s, restocks %s; %.3f ms a tick in all", work.timing(Action.ANSWER), work.timing(Action.ROSTER),
					work.timing(Action.FILE), work.timing(Action.RESTOCK), work.actedMs() / MEASURED));
			lines.add(String.format(Locale.ROOT, "calls timed after the spans (one unmeasured tick; %d looks and thinks, %d pages and listings, each after as many untimed): "
					+ "a perch's look %.2f us, a Stolas's think %.2f us, holder 0's pact page %.2f us, holder 1's roster listing %.2f us (%d rows read)",
					CALLS, READS, calls.look(), calls.think(), calls.page(), calls.roster(), calls.rows()));
			lines.add("inventory: " + work.inventoryLine() + "; problems " + (work.problems.isEmpty() ? "none" : work.problems));
			lines.add(String.format(Locale.ROOT, "network to holder 0, the observed player (uncompressed, unframed): baseline %d packets %.1f KiB (%.2f KiB/s), "
					+ "pact hall %d packets %.1f KiB (%.2f KiB/s); unsized %d and %d; signs in reach %d and %d (%.1f KiB, an upper bound); pact states a client "
					+ "that declared them would be sent %d (%.1f KiB)", tb.packets, kib(tb.bytes), kib(tb.bytes) / seconds, th.packets, kib(th.bytes),
					kib(th.bytes) / seconds, tb.unsized, th.unsized, tb.signs, th.signs, kib(th.signBytes), th.payloads, kib(th.payloadBytes)));
			lines.add(String.format(Locale.ROOT, "network to the others: the other eight players were sent %d and %d messages (counted, not sized); the guest's pact states "
					+ "%d (%.1f KiB) and holder 1's roster states %d (%.1f KiB), sized as declared clients would receive them", others[0], others[1],
					work.guestSent.payloads, kib(work.guestSent.payloadBytes), work.rosterSent.payloads, kib(work.rosterSent.payloadBytes)));
			lines.add("network, pact hall, holder 0 by kind: " + th.top(12));
			lines.add("not measured: Jade's replies (they answer a client's request); compression and framing; client frame time (no client here)");
			lines.add("not in the workload: Star-reckoning (once a game day a pact; its 2,400-tick attunement is the sky's, as a master's recall), an offering "
					+ "falling due, a warning or a suspension (the shipped period is 7 game days), ending a pact, a second roster, an unloading chunk");
			lines.add(String.format(Locale.ROOT, "budget (D-P129 in C-P67's form, proposed; the owner decides at merge review): median +%.1f ms: %s; p95 +%.1f ms: %s; "
					+ "retained +%.0f MiB beyond the baseline's: %s. Asserted here: +%.1f, +%.1f ms, +%.0f MiB", BUDGET_MEDIAN_MS,
					deltaMedian <= BUDGET_MEDIAN_MS ? "within" : "over", BUDGET_P95_MS, deltaP95 <= BUDGET_P95_MS ? "within" : "over", BUDGET_RETAINED_MIB,
					!memory ? "not measured" : retained <= BUDGET_RETAINED_MIB ? "within" : "over", LIMIT_MEDIAN_MS, LIMIT_P95_MS, LIMIT_RETAINED_MIB));
			lines.add("===== end =====");
			for (String line : lines) {
				Jugcraft.LOGGER.info("Pact hall load card | {}", line);
			}
		}
	}

	/** The pact hall: built in one tick, driven as its players would, its rules sampled every 100 ticks while measured and its items audited every AUDIT ticks. */
	private static final class Hall {
		/** What has happened so far: requests done by hand (a request is one load), glowstone delivered, requests filed, answers and roster reads. */
		record Tally(int[] done, int delivered, int filed, int answers, int reads) {
		}

		/** Per-call costs timed after the spans, in microseconds, and the rows the reads produced. */
		record Calls(double look, double think, double page, double roster, int rows) {
		}

		final Card card;
		final GameTestHelper helper;
		final ServerLevel level;
		final MinecraftServer server;
		final Set<String> problems = new LinkedHashSet<>();
		final List<String> counts = new ArrayList<>();
		final Map<String, Integer> stolasStates = new TreeMap<>();
		final Map<String, Integer> handStates = new TreeMap<>();
		final Set<Event> seen = new HashSet<>();
		final PactPerchBlockEntity[] perches = new PactPerchBlockEntity[HOLDERS];
		final UUID[] pacts = new UUID[HOLDERS];
		final CourierPostBlockEntity[] posts = new CourierPostBlockEntity[HANDS];
		final Container[] chests = new Container[HANDS];
		final SpiritAnchorBlockEntity[] anchors = new SpiritAnchorBlockEntity[HANDS];
		final UUID[] hands = new UUID[HANDS];
		final Place[] places = new Place[HANDS];
		final int[] done = new int[HANDS];
		/** The players' actions while measured: nanoseconds and count, by Action. */
		final long[][] acted = new long[Action.values().length][2];
		/** What the guest and the roster's reader would be sent, sized as declared clients would receive it. */
		final Traffic guestSent = new Traffic();
		final Traffic rosterSent = new Traffic();
		PactPerchBlockEntity guestPerch;
		PactMenu negotiating;
		PactMenu reading;
		CourierLedger ledger;
		Tally marked;
		int delivered;
		int filed;
		int answers;
		int reads;
		int rosterSequence;
		int rosterRows;
		int sampled;
		long oldestLook;
		int atQuota;

		Hall(Card card) {
			this.card = card;
			this.helper = card.helper;
			this.level = card.level;
			this.server = card.server;
		}

		void build() {
			long now = level.getGameTime();
			PactDefinition offer = PactPerchBlockEntity.definition();
			helper.assertTrue(offer.cap() * offer.count() == ESCROW, "Stolas's escrow holds " + ESCROW + " offerings: " + offer.cap() + " x " + offer.count());
			ledger = CourierLedger.of(server);
			// Eight pacts, each at its holder's own perch, sealed under the shipped offer (the compact left out, as unit 2's tests seal), its escrow full.
			for (int i = 0; i < HOLDERS; i++) {
				ServerPlayer holder = card.players.get(i);
				PactPerchBlockEntity perch = place(perchAt(i), holder);
				AgreementHolder.Sealed sealed = perch.holder().seal(level, holder, PactPerchBlockEntity.sealing(offer, null, null), now);
				helper.assertTrue(sealed.outcome() == Pacts.Outcome.DONE, "Holder " + i + "'s pact with Stolas is sealed: " + sealed.outcome());
				perches[i] = perch;
				pacts[i] = perch.holder().agreement().record();
				holder.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, ESCROW));
				int paid = PactKeeping.deposit(level, holder, perch.holder(), Items.AMETHYST_SHARD, now);
				helper.assertTrue(paid == ESCROW, "Holder " + i + " pays " + ESCROW + " offerings into the escrow: " + paid);
			}
			// Four workshops: a post, a chest of glowstone five blocks off, an anchor bound to the post by Porter Key, a hand sealed there.
			for (int w = 0; w < HANDS; w++) {
				ServerPlayer holder = card.players.get(w);
				posts[w] = WorkshopHands.post(helper, holder, postAt(w));
				places[w] = Couriers.place(level, helper.absolutePos(postAt(w)));
				helper.setBlock(chestAt(w), Blocks.CHEST);
				chests[w] = (Container) level.getBlockEntity(helper.absolutePos(chestAt(w)));
				chests[w].setItem(0, new ItemStack(Items.GLOWSTONE_DUST, LOAD));
				chests[w].setItem(1, new ItemStack(Items.GLOWSTONE_DUST, SUPPLY - LOAD));
				SpiritAnchorBlockEntity anchor = WorkshopHands.anchor(helper, holder, anchorAt(w));
				helper.assertTrue(WorkshopHands.use(helper, holder, WorkshopHands.keyFor(helper, holder, postAt(w)), anchorAt(w)) == InteractionResult.SUCCESS,
						"Holder " + w + " binds their anchor to their post" + WorkshopHands.why(helper, holder, anchorAt(w), postAt(w)));
				RateGate.forget(holder.getUUID());
				helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, holder, anchor)), WorkshopHands.SEALED, "and seals a workshop hand there");
				WorkshopHandEntity hand = level.getEntity(anchor.spirit()) instanceof WorkshopHandEntity found ? found : null;
				helper.assertTrue(hand != null && holder.getUUID().equals(hand.owner()), "The hand answers holder " + w);
				BlockPos by = handAt(w);
				WorkshopHands.stand(helper, hand, by.getX() + 0.5, by.getY(), by.getZ() + 0.5);
				anchors[w] = anchor;
				hands[w] = hand.getUUID();
			}
			// The two open conversations: the guest negotiating at a perch keeping nothing (a visitor comes), holder 0 reading their pact.
			ServerPlayer guest = card.players.get(HOLDERS);
			guestPerch = place(GUEST_PERCH, guest);
			PactMenu.open(guest, level, guestPerch);
			helper.assertTrue(guest.containerMenu instanceof PactMenu && guestPerch.visitor() != null, "The guest's conversation opens and Stolas comes to hear it");
			negotiating = (PactMenu) guest.containerMenu;
			ServerPlayer reader = card.players.get(0);
			PactMenu.open(reader, level, perches[0]);
			helper.assertTrue(reader.containerMenu instanceof PactMenu, "Holder 0 opens their own pact at their perch");
			reading = (PactMenu) reader.containerMenu;
			// Holder 1's roster opens at the first ROSTER_TICKS boundary (drive), so its gate never meets a second read too soon.
			helper.assertTrue(stolases().size() == HOLDERS + 1, "Nine Stolas are in the hall: " + stolases().size());
			counts.addAll(List.of("Pact Perches " + (HOLDERS + 1) + " (" + HOLDERS + " keeping a pact with Stolas, the guest's keeping none)",
					"Stolas " + (HOLDERS + 1) + " (" + HOLDERS + " serving, 1 visiting)", "players " + card.players.size() + " (" + HOLDERS + " holders, 1 guest)",
					"Bound Wills " + (HOLDERS + HANDS) + " (" + HOLDERS + " pacts, " + HANDS + " hand agreements)",
					"escrow " + HOLDERS * ESCROW + " amethyst shards (" + ESCROW + " a pact)", "Courier Posts " + HANDS, "Spirit Anchors " + HANDS + " (each bound to its post)",
					"Workshop Hands " + HANDS, "chests " + HANDS + " (" + SUPPLY + " glowstone each, five blocks from their post)",
					"open conversations 2 (the guest negotiating at the ninth perch; holder 0 reading their pact; each asked again every " + CONVERSE_TICKS + " ticks)",
					"open rosters 1 (holder 1's, read every " + ROSTER_TICKS + " ticks)", "requests filed every " + REQUEST_TICKS + " ticks at each post (staggered by 100), when none is open"));
		}

		/** A Pact Perch at {@code at}, placed by {@code owner} (its block's setPlacedBy records them, as a player's placing does). */
		PactPerchBlockEntity place(BlockPos at, ServerPlayer owner) {
			helper.setBlock(at, Workers.PACT_PERCH);
			BlockPos absolute = helper.absolutePos(at);
			Workers.PACT_PERCH.setPlacedBy(level, absolute, level.getBlockState(absolute), owner, new ItemStack(Workers.PACT_PERCH_ITEM));
			return helper.getBlockEntity(at, PactPerchBlockEntity.class);
		}

		AABB yard() {
			return helper.absoluteAABB(new AABB(OFF, 0, OFF, OFF + SIZE + 1, 16, OFF + SIZE + 1));
		}

		List<StolasEntity> stolases() {
			return level.getEntitiesOfClass(StolasEntity.class, yard(), stolas -> !stolas.gone());
		}

		void drive(int now) {
			for (int w = 0; w < HANDS; w++) {
				if (Math.floorMod(now - 100 * w, REQUEST_TICKS) == 0 && open(w) == 0) {
					long started = System.nanoTime();
					Couriers.request(card.players.get(w), level, posts[w], new ItemStack(Items.GLOWSTONE_DUST), LOAD);
					acted(Action.FILE, started);
					filed++;
				}
				if (Math.floorMod(now - 5 * w, 20) == 0) {
					long started = System.nanoTime();
					restock(w);
					acted(Action.RESTOCK, started);
				}
			}
			if (Math.floorMod(now, CONVERSE_TICKS) == 0) {
				converse(card.players.get(HOLDERS), negotiating, guestSent);
			}
			if (Math.floorMod(now - CONVERSE_TICKS / 2, CONVERSE_TICKS) == 0) {
				converse(card.players.get(0), reading, null);
			}
			if (Math.floorMod(now, ROSTER_TICKS) == 0) {
				long started = System.nanoTime();
				RosterStatePayload state = roster();
				acted(Action.ROSTER, started);
				if (state == null) {
					problems.add(now + ": holder 1's roster was not answered");
				} else {
					reads++;
					rosterRows = state.rows().size();
					if (card.current != null) {
						rosterSent.payload(state, card.codec);
					}
				}
			}
			if (now % WINDOW == 0) {
				history();
				if (card.current != null) {
					sample(level.getGameTime());
				}
			}
			if (now % AUDIT == 0) {
				audit("tick " + now);
			}
		}

		void acted(Action action, long started) {
			if (card.current != null) {
				acted[action.ordinal()][0] += System.nanoTime() - started;
				acted[action.ordinal()][1]++;
			}
		}

		/** A reader asks to hear the terms again, as a screen sends (CHOOSE: the next sequence, the revision last read); the conversation stays open. */
		void converse(ServerPlayer reader, PactMenu menu, @Nullable Traffic sent) {
			long started = System.nanoTime();
			PactStatePayload last = menu.lastSent();
			PactStatePayload state = PactMenu.receive(reader, new PactRequestPayload(menu.containerId, menu.lastSequence() + 1, PactRequestPayload.CHOOSE, 0,
					last == null ? 0 : last.revision()));
			acted(Action.ANSWER, started);
			if (state == null || reader.containerMenu != menu) {
				problems.add(level.getGameTime() + ": " + reader.getScoreboardName() + "'s conversation was not answered, or closed");
				return;
			}
			answers++;
			if (card.current != null) {
				(sent == null ? card.current.traffic : sent).payload(state, card.codec);
			}
		}

		/** Holder 1's open roster asks again, as its screen does every ROSTER_TICKS (RosterClient.REFRESH_TICKS, D-P118). */
		@Nullable RosterStatePayload roster() {
			return Roster.receive(card.players.get(1), new RosterRequestPayload(++rosterSequence, RosterRequestPayload.OPEN, RosterRequestPayload.NO_ROW, 0));
		}

		/** Requests still in the ledger at workshop {@code w}'s post (its holder files one only when none is). */
		int open(int w) {
			int n = 0;
			for (Request request : ledger.ledger().requests()) {
				if (request.ticket().post().equals(places[w])) {
					n++;
				}
			}
			return n;
		}

		/** What the ledger holds for workshop {@code w}'s requests: in transit, or stranded at its post. */
		int carried(int w) {
			int n = 0;
			for (Request request : ledger.ledger().requests()) {
				if (request.ticket().post().equals(places[w])) {
					n += request.progress().carried();
				}
			}
			return n;
		}

		/** Workshop {@code w}'s post emptied into its chest, as a hopper below the post would take its deliveries (moved, never made or lost). */
		void restock(int w) {
			StorageUtil.move(ContainerStorage.of(posts[w], null), ContainerStorage.of(chests[w], null), variant -> true, Long.MAX_VALUE, null);
		}

		/** Each post's history (at most Logistics.HISTORY events), read every 100 ticks and at each tally: requests done and glowstone delivered, each event once. */
		void history() {
			for (int w = 0; w < HANDS; w++) {
				for (Event event : ledger.ledger().history(places[w])) {
					if (!seen.add(event)) {
						continue;
					}
					if (event.kind().equals("done")) {
						done[w]++;
						delivered += event.amount();
					} else if (event.kind().equals("delivered")) {
						delivered += event.amount();
					}
				}
			}
		}

		/** Every 100 ticks while measured: D-09's rules, and what each Stolas and hand is doing. */
		void sample(long gameTime) {
			sampled++;
			BoundWills wills = BoundWills.of(server);
			int present = stolases().size();
			if (present != HOLDERS + 1) {
				problems.add(gameTime + ": " + present + " Stolas in the hall, not " + (HOLDERS + 1));
			}
			for (int i = 0; i < HOLDERS; i++) {
				UUID holder = card.players.get(i).getUUID();
				StolasEntity stolas = perches[i].stolas(level);
				if (stolas == null || !pacts[i].equals(stolas.record()) || !holder.equals(stolas.owner())) {
					problems.add(gameTime + ": holder " + i + "'s pact has no presence of its own at its perch");
				}
				long kept = wills.ledger().heldBy(holder).stream().filter(will -> PactDefinition.STOLAS.equals(Pacts.counterpart(will.counterpart()))).count();
				if (kept != 1) {
					problems.add(gameTime + ": holder " + i + " keeps " + kept + " pacts with Stolas");
				}
				BoundWill record = wills.ledger().get(pacts[i]);
				BoundWill.Upkeep upkeep = record == null || record.pact() == null ? null : record.pact().upkeep();
				if (upkeep == null) {
					problems.add(gameTime + ": holder " + i + "'s pact or its upkeep is gone");
				} else {
					long since = gameTime - upkeep.lookedAt();
					oldestLook = Math.max(oldestLook, since);
					if (since > Pacts.WRITE_TICKS + PactPerchBlockEntity.LOOK_TICKS) {
						problems.add(gameTime + ": holder " + i + "'s pact was last looked at " + since + " ticks ago");
					}
				}
				stolasStates.merge(stolas == null ? "none" : stolas.status().id + (stolas.listening() ? " listening" : ""), 1, Integer::sum);
			}
			StolasEntity visitor = guestPerch.stolas(level);
			if (visitor == null || visitor.owner() != null || !visitor.listening()) {
				problems.add(gameTime + ": no visitor listens at the guest's perch");
			}
			for (int w = 0; w < HANDS; w++) {
				WorkshopHandEntity hand = level.getEntity(hands[w]) instanceof WorkshopHandEntity found && !found.isRemoved() ? found : null;
				if (hand == null || !hands[w].equals(anchors[w].spirit())) {
					problems.add(gameTime + ": hand " + w + " is not its anchor's");
					continue;
				}
				handStates.merge(hand.status().id, 1, Integer::sum);
				if (hand.status() == Status.FINISHED) {
					atQuota++;
				}
			}
		}

		/** Every item in, out, carried and kept, each workshop on its own account; each pact's escrow; both ledgers' own checks; nothing on the ground. */
		void audit(String when) {
			for (int w = 0; w < HANDS; w++) {
				int chest = count(chests[w], Items.GLOWSTONE_DUST);
				int post = count(posts[w], Items.GLOWSTONE_DUST);
				int carried = carried(w);
				if (chest + post + carried != SUPPLY) {
					problems.add(when + ": workshop " + w + "'s glowstone " + SUPPLY + " is not chest " + chest + " + post " + post + " + carried " + carried);
				}
			}
			for (int i = 0; i < HOLDERS; i++) {
				int kept = BoundWills.of(server).escrow(pacts[i]).stream().mapToInt(ItemStack::getCount).sum();
				int pocket = card.players.get(i).getInventory().countItem(Items.AMETHYST_SHARD);
				if (kept != ESCROW || pocket != 0) {
					problems.add(when + ": holder " + i + "'s escrow " + kept + " and pocket " + pocket + " are not " + ESCROW + " and 0 (no period falls due)");
				}
			}
			List<ItemEntity> ground = level.getEntitiesOfClass(ItemEntity.class, yard(), ItemEntity::isAlive);
			if (!ground.isEmpty()) {
				problems.add(when + ": items on the ground: " + ground.stream().map(item -> item.getItem().toString()).toList());
			}
			List<String> courier = ledger.problems();
			if (!courier.isEmpty()) {
				problems.add(when + ": the courier ledger: " + courier);
			}
			List<String> bound = BoundWills.of(server).problems();
			if (!bound.isEmpty()) {
				problems.add(when + ": the Bound Wills: " + bound);
			}
		}

		Tally tally() {
			history();
			return new Tally(done.clone(), delivered, filed, answers, reads);
		}

		/** The measured span begins: what has happened so far, and the samples start afresh. */
		void mark() {
			marked = tally();
			stolasStates.clear();
			handStates.clear();
			sampled = 0;
			oldestLook = 0;
			atQuota = 0;
		}

		int[] loads(Tally end, Tally from) {
			int[] loads = new int[HANDS];
			for (int w = 0; w < HANDS; w++) {
				loads[w] = end.done()[w] - from.done()[w];
			}
			return loads;
		}

		String timing(Action action) {
			long[] row = acted[action.ordinal()];
			return String.format(Locale.ROOT, "%d at %.1f us", row[1], row[1] == 0 ? 0.0 : row[0] / 1000.0 / row[1]);
		}

		double actedMs() {
			long nanos = 0;
			for (long[] row : acted) {
				nanos += row[0];
			}
			return ms((double) nanos);
		}

		/**
		 * After the spans, in one unmeasured tick: holder 0's perch's look (PactKeeping.tick, its ticker's call), its Stolas's think,
		 * holder 0's pact page (PactKeeping.page, what an answer reads) and holder 1's roster listing (Roster.list, what a roster read
		 * reads), each after as many untimed calls. Each reads the hall as it stands: nothing falls due, nobody leaves, and nothing is
		 * written but the last look's time, at most once.
		 */
		Calls timeCalls() {
			long gameTime = level.getGameTime();
			long time = level.getOverworldClockTime();
			BlockPos at = helper.absolutePos(perchAt(0));
			AgreementHolder holder = perches[0].holder();
			StolasEntity stolas = perches[0].stolas(level);
			ServerPlayer reader = card.players.get(0);
			ServerPlayer listed = card.players.get(1);
			if (stolas == null) {
				problems.add("the timed calls found no Stolas at holder 0's perch");
				return new Calls(0, 0, 0, 0, 0);
			}
			long look = 0;
			long think = 0;
			long page = 0;
			long roster = 0;
			int rows = 0;
			for (int pass = 0; pass < 2; pass++) {
				long start = System.nanoTime();
				for (int i = 0; i < CALLS; i++) {
					PactKeeping.tick(level, holder, at, gameTime);
				}
				look = System.nanoTime() - start;
				start = System.nanoTime();
				for (int i = 0; i < CALLS; i++) {
					stolas.think(level, time, gameTime);
				}
				think = System.nanoTime() - start;
				start = System.nanoTime();
				for (int i = 0; i < READS; i++) {
					rows += PactKeeping.page(level, reader, holder, gameTime).rows().size();
				}
				page = System.nanoTime() - start;
				start = System.nanoTime();
				for (int i = 0; i < READS; i++) {
					rows += Roster.list(listed, gameTime).rows().size();
				}
				roster = System.nanoTime() - start;
			}
			if (rows == 0 || stolas.gone()) {
				problems.add("the timed calls read nothing, or Stolas left: " + rows + " rows");
			}
			return new Calls(look / 1000.0 / CALLS, think / 1000.0 / CALLS, page / 1000.0 / READS, roster / 1000.0 / READS, rows);
		}

		String inventoryLine() {
			int inChests = 0;
			int inPosts = 0;
			int carried = 0;
			for (int w = 0; w < HANDS; w++) {
				inChests += count(chests[w], Items.GLOWSTONE_DUST);
				inPosts += count(posts[w], Items.GLOWSTONE_DUST);
				carried += carried(w);
			}
			int escrow = 0;
			int held = 0;
			for (int i = 0; i < HOLDERS; i++) {
				escrow += BoundWills.of(server).escrow(pacts[i]).stream().mapToInt(ItemStack::getCount).sum();
				held += BoundWills.of(server).ledger().heldBy(card.players.get(i).getUUID()).size();
			}
			return String.format(Locale.ROOT, "glowstone %d = chests %d + posts %d + carried %d (each workshop's %d on its own account); escrow %d amethyst shards = "
					+ "%d pacts x %d, none taken (no period fell due); the compact not paid (sealed as unit 2's tests seal); Bound Wills held %d; open requests %d; "
					+ "ground and both ledgers checked", HANDS * SUPPLY, inChests, inPosts, carried, SUPPLY, escrow, HOLDERS, ESCROW, held,
					ledger.ledger().requests().size());
		}
	}
}
