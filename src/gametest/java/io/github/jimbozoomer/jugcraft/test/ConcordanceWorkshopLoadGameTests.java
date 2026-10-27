package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Operation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualRun;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.SignPayload;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import local.peepo.PeepoEntity;
import local.peepo.PeepoMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SystemReport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Concordance Chunk C, C-10: the measured small workshop (docs/features/concordance-chunk-c.md, C-P19 to C-P21 and
 * C-P66 to C-P68). The same world is measured twice: first as it is, then with C-10's workshop built into it. The
 * workshop is a Crucible Stand under an Alembic Crucible replaying the Stillroot Poultice with water from an Electric
 * Pump; a Lesser Circle running Root and Rivet again and again; a Peepo and a Clockwork Porter bringing the crucible its
 * Mendvetch; and nine planted Verdant Beds (and the Verdant Heart's) with a Mulch Maw and a Gleaner. Its power comes from
 * Creative Energy Cells, test scaffolding rather than a player's generators. Each span is a warm-up, then 6,000 ticks
 * read from MinecraftServer's own 100-tick ring of tick times, less this test's own work in each tick, which includes the
 * simulated player's actions (starting and taking each rite) and taking each brew out. The test also records the runner,
 * the Java options, the world, the counts, the memory trend, every item in, out, carried and stored, and what one nearby
 * player is sent. It writes them to the log as "Concordance load card | ...".
 * <p>
 * It measures only when Fabric's filter selects it and nothing else (docs/TESTING.md); in every other run it passes at
 * once, so ordinary runs are not lengthened. The game test server runs its ticks back to back
 * (GameTestServer.waitUntilNextTick runs tasks without waiting), so the figures are each tick's work as any server
 * measures it, on this runner only. The yard starts {@value #OFF} blocks in, clear of the default structure's barrier
 * walls, and its chunks are force-loaded: the game test server sets no simulation distance, so only forced chunks tick.
 * Its Measure, Traffic and Stats, and its static helpers, are shared with the pact hall's card
 * (ConcordancePactHallLoadGameTests, Concordance Chunk D, D-09), which measures with the same spans.
 */
public class ConcordanceWorkshopLoadGameTests {
	/** Fabric's id for loadCard: the test mod's id, then the class and method in snake case (TestAnnotationLocator). */
	public static final String ID = "jugcraft-test:concordance_workshop_load_game_tests_load_card";
	/** Fabric API's selection property (GameTestSystemProperties.FILTER, an impl class not imported here). */
	static final String FILTER = "fabric-api.gametest.filter";
	/** MinecraftServer keeps the last 100 ticks' times (tickTimesNanos). */
	static final int WINDOW = 100;
	static final int MEASURED = 6_000;
	static final int BASE_WARMUP = 2_000;
	static final int SHOP_WARMUP = 1_500;
	/** A warm-up also waits until the loaded chunks have held steady this long. */
	static final int SETTLE = 400;
	static final int WARMUP_CAP = 10_000;
	static final int AUDIT = 1_000;
	static final int MAX_TICKS = 33_000;
	/** The yard: a stone floor from OFF to OFF + SIZE in x and z, barriers round its edge. */
	static final int SIZE = 43;
	static final int OFF = 10;
	/** C-P19 in C-P67's form, as proposed and decided by the owner at merge review: reported here, not asserted. */
	static final double BUDGET_MEDIAN_MS = 1.0;
	static final double BUDGET_P95_MS = 2.5;
	static final double BUDGET_RETAINED_MIB = 4.0;
	/** What is asserted: generous limits a shared CI runner should never reach unless the work has grown. */
	static final double LIMIT_MEDIAN_MS = 5.0;
	static final double LIMIT_P95_MS = 12.5;
	static final double LIMIT_RETAINED_MIB = 64.0;
	static final int SUPPLY = 192;
	static final int BOWLS = 128;
	static final int CHAFF = 256;
	private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

	// The alchemy corner: theFullChainRunsFromAPumpAndACable's chain (ConcordanceAlchemyGameTests), with bowls fed from above.
	static final BlockPos STAND = p(8, 2, 8);
	static final BlockPos CRUCIBLE = STAND.above();
	static final BlockPos BOWL_HOPPER = CRUCIBLE.above();
	static final BlockPos BOWL_CHEST = BOWL_HOPPER.above();
	static final BlockPos STAND_CELL = p(6, 2, 8);
	static final BlockPos STAND_CABLE = p(7, 2, 8);
	static final BlockPos SPRING = p(8, 2, 10);
	static final BlockPos PUMP = SPRING.above();
	static final BlockPos PIPE = p(8, 3, 9);
	static final BlockPos PUMP_CELL = PUMP.above();
	// The Peepo's own supply; the courier post, its chest, and the pylon (on its own cell) that fuels the porter.
	static final BlockPos PEEPO_SUPPLY = p(8, 2, 2);
	static final BlockPos PEEPO_AT = p(6, 2, 4);
	static final BlockPos POST = p(16, 2, 12);
	static final BlockPos POST_CHEST = POST.east();
	static final BlockPos PORTER_AT = POST.west();
	static final BlockPos POST_PYLON = p(16, 2, 14);
	static final BlockPos POST_CELL = POST_PYLON.below();
	// The Lesser Circle: its pylons fed through cables under the floor, its one participant inside it (no part there).
	static final BlockPos ANCHOR = p(22, 2, 22);
	static final BlockPos PARTICIPANT = ANCHOR.offset(1, 0, 1);
	static final BlockPos CIRCLE_CELL = ANCHOR.offset(0, -1, 4);
	// The garden: sun beds north of the Gleaner, the mixed bed in a dark room south of it.
	static final BlockPos HEART_BED = p(32, 1, 22);
	static final BlockPos GLEANER = p(32, 2, 24);
	static final BlockPos GLEANER_HOPPER = GLEANER.below();
	static final BlockPos STORE = GLEANER_HOPPER.east();
	static final BlockPos MAW = p(30, 2, 23);
	static final BlockPos MAW_HOPPER = MAW.above();
	static final BlockPos MAW_CHEST = MAW_HOPPER.above();
	static final List<BlockPos> WATER = List.of(p(34, 1, 22), p(33, 1, 27));
	static final List<Planting> CROPS = List.of(
			new Planting(p(31, 1, 21), "mendvetch", true), new Planting(p(32, 1, 21), "mendvetch", true),
			new Planting(p(33, 1, 21), "mendvetch", true), new Planting(p(31, 1, 22), "sunpetal", true),
			new Planting(p(33, 1, 22), "sunpetal", true), new Planting(p(31, 1, 26), "dewmoss", false),
			new Planting(p(32, 1, 27), "dewmoss", false), new Planting(p(32, 1, 26), "gloamcap", false),
			new Planting(p(31, 1, 27), "gloamcap", false));
	static final Formula POULTICE = poultice();
	/** The clean poultice every batch must be (ConcordanceAlchemyGameTests.aStillrootPoulticeRepeatsAndADirtyBatchHealsLess). */
	static final Brew CLEAN = new Brew(List.of(new Brew.Dose("minecraft:regeneration", 1, 450, false)), "minecraft:regeneration@1");

	/** A yard position: x and z from the yard's corner, y as given. */
	static BlockPos p(int x, int y, int z) {
		return new BlockPos(OFF + x, y, OFF + z);
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

	/** tools/concordance_alchemy.py stillroot_poultice: two Mendvetch as they come, in one part of water, stirred warm four times. */
	private static Formula poultice() {
		List<Operation> process = new ArrayList<>(List.of(new Operation.Water(1)));
		for (int i = 0; i < 2; i++) {
			process.add(new Operation.Add("jugcraft:mendvetch", "jugcraft:raw"));
		}
		for (int i = 0; i < 4; i++) {
			process.add(new Operation.Stir(Band.WARM));
		}
		return new Formula(process);
	}

	/** How many of the formula's additions a batch at {@code step} has made: each is a Mendvetch in the mixture. */
	static int adds(int step) {
		List<Operation> operations = POULTICE.operations();
		int made = 0;
		for (int i = 0; i < Math.min(step, operations.size()); i++) {
			if (operations.get(i) instanceof Operation.Add) {
				made++;
			}
		}
		return made;
	}

	private enum Phase {
		BASE_WARMUP, BASE, SHOP_WARMUP, SHOP
	}

	/** A planted bed: its crop, ripe when built or one step short. */
	private record Planting(BlockPos bed, String organism, boolean ripe) {
		Block crop() {
			return switch (organism) {
				case "mendvetch" -> Garden.MENDVETCH_CROP;
				case "sunpetal" -> Garden.SUNPETAL_CROP;
				case "dewmoss" -> Garden.DEWMOSS_CROP;
				default -> Garden.GLOAMCAP_CROP;
			};
		}
	}

	/** One measured span. */
	static final class Measure {
		final String name;
		final long[] raw = new long[MEASURED];
		final long[] net = new long[MEASURED];
		final long[] heap = new long[MEASURED / WINDOW];
		final Traffic traffic = new Traffic();
		int start;
		/** The ticks of warm-up before this span: at least its minimum, then until the loaded chunk count held SETTLE ticks. */
		int warmup;
		int windows;
		long own;
		long retainedBefore;
		long retainedAfter;
		long collections;
		long collectionMillis;
		int chunksBefore;
		int chunksAfter;

		Measure(String name) {
			this.name = name;
		}
	}

	/** What the one test player is sent: packets, and their bytes as the play protocol writes them (uncompressed, unframed). */
	static final class Traffic {
		final Map<String, long[]> kinds = new HashMap<>();
		long packets;
		long bytes;
		long unsized;
		long signs;
		long signBytes;
		/** Payloads a test player never declares, sized as a client that declared them would receive them (the pact hall's card). */
		long payloads;
		long payloadBytes;

		void add(Packet<?> packet, StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec) {
			if (packet instanceof BundlePacket<?> bundle) {
				for (Packet<?> part : bundle.subPackets()) {
					add(part, codec);
				}
				return;
			}
			int size = size(packet, codec);
			long[] row = kinds.computeIfAbsent(kind(packet), unused -> new long[2]);
			row[0]++;
			row[1] += size;
			packets++;
			bytes += size;
		}

		/** A sign the server decided to show within reach: sized as the payload a client that declared it would receive. */
		void sign(Signs.Shown shown, StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec) {
			signs++;
			signBytes += size(new ClientboundCustomPayloadPacket(new SignPayload(shown.sign().ordinal(), shown.at(),
					Optional.ofNullable(shown.from()))), codec);
		}

		/** A payload the server sends only to a client that declared it (Concordance Chunk D, D-09): counted and sized, never sent. */
		void payload(CustomPacketPayload payload, StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec) {
			payloads++;
			payloadBytes += size(new ClientboundCustomPayloadPacket(payload), codec);
		}

		private static String kind(Packet<?> packet) {
			if (packet instanceof ClientboundBlockEntityDataPacket data) {
				return "block_entity_data " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(data.getType());
			}
			if (packet instanceof ClientboundCustomPayloadPacket custom) {
				return "payload " + custom.payload().type().id();
			}
			return packet.type().id().toString();
		}

		@SuppressWarnings("unchecked")
		private int size(Packet<?> packet, StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec) {
			ByteBuf buffer = Unpooled.buffer();
			try {
				codec.encode(buffer, (Packet<? super ClientGamePacketListener>) packet);
				return buffer.readableBytes();
			} catch (RuntimeException e) {
				unsized++;
				return 0;
			} finally {
				buffer.release();
			}
		}

		String top(int n) {
			return kinds.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])).limit(n)
					.map(e -> String.format(Locale.ROOT, "%s %d/%.1f KiB", e.getKey(), e.getValue()[0], kib(e.getValue()[1])))
					.collect(Collectors.joining(", "));
		}
	}

	record Stats(double median, double p95, double mean, double max, int over50, double windowMedian, double windowP95) {
		static Stats of(long[] nanos) {
			long[] sorted = nanos.clone();
			Arrays.sort(sorted);
			double sum = 0;
			int over = 0;
			for (long tick : nanos) {
				sum += tick;
				if (tick > 50_000_000L) {
					over++;
				}
			}
			double[] windows = new double[nanos.length / WINDOW];
			for (int w = 0; w < windows.length; w++) {
				double total = 0;
				for (int i = 0; i < WINDOW; i++) {
					total += nanos[w * WINDOW + i];
				}
				windows[w] = total / WINDOW;
			}
			Arrays.sort(windows);
			double windowMedian = windows.length % 2 == 1 ? windows[windows.length / 2]
					: (windows[windows.length / 2 - 1] + windows[windows.length / 2]) / 2.0;
			return new Stats(ms(middle(sorted)), ms(sorted[rank(sorted.length, 0.95)]), ms(sum / nanos.length),
					ms(sorted[sorted.length - 1]), over, ms(windowMedian), ms(windows[rank(windows.length, 0.95)]));
		}
	}

	/** Nearest-rank: the smallest value with at least q of all at or below it. */
	static int rank(int n, double q) {
		return Math.max(0, (int) Math.ceil(q * n) - 1);
	}

	/** The middle of a sorted run (the mean of the two middle values when even); named apart from Stats' median(). */
	static double middle(long[] sorted) {
		int n = sorted.length;
		return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
	}

	static double half(long[] nanos, boolean second) {
		long[] part = Arrays.copyOfRange(nanos, second ? nanos.length / 2 : 0, second ? nanos.length : nanos.length / 2);
		Arrays.sort(part);
		return ms(middle(part));
	}

	static double ms(double nanos) {
		return nanos / 1_000_000.0;
	}

	static long mib(long bytes) {
		return bytes / (1024L * 1024L);
	}

	static double kib(long bytes) {
		return bytes / 1024.0;
	}

	/** The least-squares slope of the heap in use at each window's end, in MiB per 1,000 ticks. */
	static double slope(long[] heap) {
		int n = heap.length;
		double meanX = (n - 1) / 2.0;
		double meanY = 0;
		for (long h : heap) {
			meanY += h;
		}
		meanY /= n;
		double num = 0;
		double den = 0;
		for (int i = 0; i < n; i++) {
			num += (i - meanX) * (heap[i] - meanY);
			den += (i - meanX) * (i - meanX);
		}
		return den == 0 ? 0 : num / den * (1000.0 / WINDOW) / (1024.0 * 1024.0);
	}

	/** The heap in use just after a full collection, asked for in an unmeasured tick; -1 if the JVM ignores System.gc(). */
	static long retained() {
		long before = collections();
		System.gc();
		return collections() > before ? MEMORY.getHeapMemoryUsage().getUsed() : -1L;
	}

	static long collections() {
		long n = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			n += Math.max(0L, bean.getCollectionCount());
		}
		return n;
	}

	static long collectionMillis() {
		long n = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			n += Math.max(0L, bean.getCollectionTime());
		}
		return n;
	}

	static int count(Container container, Item item) {
		int n = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			n += count(container.getItem(slot), item);
		}
		return n;
	}

	static int count(List<ItemStack> stacks, Item item) {
		int n = 0;
		for (ItemStack stack : stacks) {
			n += count(stack, item);
		}
		return n;
	}

	static int count(ItemStack stack, Item item) {
		return stack.is(item) ? stack.getCount() : 0;
	}

	/** The run: one driver callback a tick, a single map entry at a time (runAfterDelay), first the baseline, then the workshop. */
	private static final class Card {
		final GameTestHelper helper;
		final ServerLevel level;
		final MinecraftServer server;
		/** The test's own nanoseconds in each tick of the ring, by tick % WINDOW, taken out of the server's figure. */
		final long[] own = new long[WINDOW];
		final List<Signs.Shown> shown = new ArrayList<>();
		final List<String> runner = new ArrayList<>();
		final List<ChunkPos> forced = new ArrayList<>();
		/** The yard's chunks, and how many of them the card forced (the test's structure may have forced one already). */
		int yardChunks;
		int forcedCount;
		ServerPlayer player;
		EmbeddedChannel channel;
		StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec;
		AutoCloseable signs;
		Phase phase = Phase.BASE_WARMUP;
		int phaseTicks;
		int expected = -1;
		int chunks = -1;
		int steadySince;
		Measure base;
		Measure shop;
		Measure current;
		Shop workshop;
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
			// SystemReport's "JVM Flags" keeps only the -X options; this line has the rest, Fabric's filter among them.
			runner.add("JVM arguments: " + String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
		}

		/**
		 * Noon held, weather held, and no mobs spawning (in 26.3 the custom spawners, the wandering trader's among them, run
		 * only while spawn_mobs is on; its own rule is turned off as well, belt and braces), so both spans see the same
		 * world. Given back at the end.
		 */
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

		/** Every chunk under the yard ticks, as it would round a player (the game test server's simulation distance is 0). */
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

		/** The floor both spans stand on, ringed with barriers so no worker walks off it. */
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

		/** The one player: as GameTestHelper.makeMockServerPlayerInLevel makes one, but its channel kept to read what it is sent. */
		void join() {
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "load-card"), false);
			player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
			Connection connection = new Connection(PacketFlow.SERVERBOUND);
			channel = new EmbeddedChannel(new ChannelHandler[] {connection});
			server.getPlayerList().placeNewPlayer(connection, player, cookie);
			player.setGameMode(GameType.SURVIVAL);
			BlockPos at = helper.absolutePos(PARTICIPANT);
			player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
			level.getChunkSource().move(player);
			for (String research : List.of("jugcraft:first_light", "jugcraft:circle_lore", Alchemy.RESEARCH, Workers.RESEARCH, Garden.RESEARCH)) {
				ConcordanceProgress.grant(player, research, ResearchState.UNDERSTOOD);
			}
			codec = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(level.registryAccess())).codec();
		}

		void tick() {
			long began = System.nanoTime();
			int now = server.getTickCount();
			helper.assertTrue(expected < 0 || now == expected, "The driver ran every tick, so the windows line up: " + now + " after " + (expected - 1));
			expected = now + 1;
			player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
			drain();
			if (workshop != null) {
				workshop.drive(now);
			}
			phaseTicks++;
			if (!advance(now)) {
				helper.runAfterDelay(1, this::tick);
			}
			own[Math.floorMod(now, WINDOW)] = System.nanoTime() - began;
		}

		void drain() {
			Traffic traffic = current == null ? null : current.traffic;
			Object message;
			while ((message = channel.readOutbound()) != null) {
				if (traffic != null && message instanceof Packet<?> packet) {
					traffic.add(packet, codec);
				}
			}
			for (Signs.Shown sign : shown) {
				if (traffic != null && sign.at().distanceToSqr(player.position()) <= Presentation.FAR * Presentation.FAR) {
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
						workshop = new Shop(this);
						workshop.build();
						phase = Phase.SHOP_WARMUP;
						phaseTicks = 0;
						chunks = -1;
					}
				}
				case SHOP_WARMUP -> {
					if (settled(SHOP_WARMUP)) {
						shop = open("workshop", now);
						workshop.mark();
						phase = Phase.SHOP;
					}
				}
				case SHOP -> {
					if (window(shop, now) && shop.windows == MEASURED / WINDOW) {
						close(shop);
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
			measure.heap[measure.windows++] = MEMORY.getHeapMemoryUsage().getUsed();
			return true;
		}

		void close(Measure measure) {
			measure.collections += collections();
			measure.collectionMillis += collectionMillis();
			measure.chunksAfter = level.getChunkSource().getLoadedChunksCount();
			measure.retainedAfter = retained();
			current = null;
		}

		void finish() {
			try {
				signs.close();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			workshop.audit("the end");
			Shop.Tally end = workshop.tally();
			release();
			unforce();
			Stats b = Stats.of(base.net);
			Stats w = Stats.of(shop.net);
			double deltaMedian = w.median() - b.median();
			double deltaP95 = w.p95() - b.p95();
			boolean memory = base.retainedBefore >= 0 && base.retainedAfter >= 0 && shop.retainedBefore >= 0 && shop.retainedAfter >= 0;
			double retained = memory ? mib(shop.retainedAfter - shop.retainedBefore) - mib(base.retainedAfter - base.retainedBefore) : 0;
			report(b, w, end, deltaMedian, deltaP95, memory, retained);
			Shop.Tally m = workshop.marked;
			int poultices = end.poultices() - m.poultices();
			int foundations = end.foundations() - m.foundations();
			int peepo = end.peepoBrought() - m.peepoBrought();
			int porter = end.porterRequests() - m.porterRequests();
			int porterItems = end.porterItems() - m.porterItems();
			int eaten = end.eaten() - m.eaten();
			helper.assertTrue(workshop.problems.isEmpty(), "Every item in, out, carried and stored is accounted for: " + workshop.problems);
			helper.assertTrue(workshop.interruptions.isEmpty() && foundations >= 20, "Root and Rivet ran on, never broken off: " + foundations + ", " + workshop.interruptions);
			helper.assertTrue(poultices >= 5 && workshop.wrong == 0, "The crucible made the clean Stillroot Poultice again and again: " + poultices);
			// Not both: the Peepo's need is the shortage less what the porter's claimed request still brings, and each searches on
			// its own fixed period, so one carrier may win every batch in a span. Both counts are on the "work while measured" line.
			helper.assertTrue(peepo + porterItems >= 1, "Mendvetch was brought while measured: the Peepo " + peepo
					+ ", the porter " + porterItems + " in " + porter + " requests");
			helper.assertTrue(end.harvested() >= 5 && eaten >= 1, "The Gleaner harvested since the build and the Maw ate while measured: "
					+ end.harvests() + " (" + (end.harvested() - m.harvested()) + " while measured), " + eaten);
			helper.assertTrue(!workshop.waits.containsKey("needs_water") && workshop.lit >= 1, "The pump kept the tank and the stand heated: " + workshop.waits + ", lit " + workshop.lit);
			helper.assertTrue(deltaMedian <= LIMIT_MEDIAN_MS && deltaP95 <= LIMIT_P95_MS,
					String.format(Locale.ROOT, "Within the asserted limits: median %+.3f ms (%.1f), p95 %+.3f ms (%.1f)", deltaMedian, LIMIT_MEDIAN_MS, deltaP95, LIMIT_P95_MS));
			helper.assertTrue(!memory || retained <= LIMIT_RETAINED_MIB, "Retained heap within " + LIMIT_RETAINED_MIB + " MiB of the baseline's: " + retained);
			helper.succeed();
		}

		void report(Stats b, Stats w, Shop.Tally end, double deltaMedian, double deltaP95, boolean memory, double retained) {
			Shop.Tally m = workshop.marked;
			Traffic tb = base.traffic;
			Traffic tw = shop.traffic;
			double seconds = MEASURED / 20.0;
			List<String> lines = new ArrayList<>();
			lines.add("===== Concordance Chunk C workshop load card (C-10) =====");
			lines.add("build: Jugcraft " + FabricLoader.getInstance().getModContainer("jugcraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")
					+ "; Jade " + (FabricLoader.getInstance().isModLoaded("jade") ? "loaded" : "absent"));
			for (String line : runner) {
				lines.add("runner: " + line);
			}
			lines.add("world: game test server (flat world, ticks run back to back); noon and weather held, mobs and wandering traders off, random tick speed "
					+ level.getGameRules().get(GameRules.RANDOM_TICK_SPEED) + "; player list view " + server.getPlayerList().getViewDistance()
					+ " and simulation " + server.getPlayerList().getSimulationDistance() + " (the chunk map views at least 2); the yard's "
					+ yardChunks + " chunks force-loaded (" + forcedCount + " by the card); players " + server.getPlayerList().getPlayerCount() + " (a server-side test player, no client)");
			lines.add(String.format(Locale.ROOT, "spans: baseline from tick %d, workshop from tick %d; each %d ticks in %d windows of %d after warm-ups of %d and %d ticks "
					+ "(at least %d and %d, then until the loaded chunk count held %d ticks); "
					+ "tick times net of the test's own work, including the simulated player's actions and output extraction; power from Creative Energy Cells (test scaffolding, not a player's generators)",
					base.start, shop.start, MEASURED, MEASURED / WINDOW, WINDOW, base.warmup, shop.warmup, BASE_WARMUP, SHOP_WARMUP, SETTLE));
			lines.add("tick ms, net of the test's own work | median p95 mean max over-50ms | 100-tick window means: median p95");
			for (Measure measure : List.of(base, shop)) {
				Stats s = measure == base ? b : w;
				lines.add(String.format(Locale.ROOT, "%s %.3f %.3f %.3f %.1f %d | %.3f %.3f", measure.name, s.median(), s.p95(), s.mean(), s.max(), s.over50(), s.windowMedian(), s.windowP95()));
			}
			lines.add(String.format(Locale.ROOT, "raw medians %.3f and %.3f ms; the test's own work %.3f and %.3f ms a tick",
					Stats.of(base.raw).median(), Stats.of(shop.raw).median(), ms((double) base.own / MEASURED), ms((double) shop.own / MEASURED)));
			lines.add(String.format(Locale.ROOT, "delta: median %+.3f ms, p95 %+.3f ms; baseline drift (second half less first) %+.3f ms",
					deltaMedian, deltaP95, half(base.net, true) - half(base.net, false)));
			lines.add(memory ? String.format(Locale.ROOT, "memory: retained after a full collection: baseline %d -> %d MiB, workshop %d -> %d MiB (%+.0f MiB beyond the baseline's change); "
					+ "heap in use %+.2f and %+.2f MiB per 1,000 ticks; collections %d (%d ms) and %d (%d ms)", mib(base.retainedBefore), mib(base.retainedAfter),
					mib(shop.retainedBefore), mib(shop.retainedAfter), retained, slope(base.heap), slope(shop.heap), base.collections, base.collectionMillis,
					shop.collections, shop.collectionMillis) : "memory: retained heap not measured (the JVM ignored System.gc())");
			lines.add("chunks loaded: baseline " + base.chunksBefore + " -> " + base.chunksAfter + ", workshop " + shop.chunksBefore + " -> " + shop.chunksAfter);
			lines.add("counts: " + String.join(", ", workshop.built));
			lines.add("level at the end: " + level.getWatchdogStats());
			lines.add("active recipes: Alembic Crucible formula \"" + POULTICE.text() + "\" (the Stillroot Poultice); rite jugcraft:root_and_rivet");
			lines.add(String.format(Locale.ROOT, "work while measured: poultices %d, Root and Rivet %d (interrupted %d), Peepo brought %d Mendvetch, porter %d in %d requests, "
					+ "Gleaner harvests %d %s, Heart searches that drew %d (of %d pulses), Maw ate %d, crucible waits %s, stand lit at %d of %d window ends",
					end.poultices() - m.poultices(), end.foundations() - m.foundations(), end.interruptions() - m.interruptions(), end.peepoBrought() - m.peepoBrought(),
					end.porterItems() - m.porterItems(), end.porterRequests() - m.porterRequests(), end.harvested() - m.harvested(), workshop.difference(end, m), end.draws() - m.draws(),
					MEASURED / GleanerBlockEntity.GLEAN_TICKS, end.eaten() - m.eaten(), workshop.waits, workshop.lit, workshop.sampled));
			lines.add("inventory: " + workshop.inventoryLine() + "; problems " + (workshop.problems.isEmpty() ? "none" : workshop.problems));
			lines.add(String.format(Locale.ROOT, "network to the one test player (uncompressed, unframed): baseline %d packets %.1f KiB (%.2f KiB/s), workshop %d packets %.1f KiB (%.2f KiB/s); "
					+ "unsized %d and %d; signs in reach %d and %d (%.1f KiB, an upper bound: the level's budget may drop some)", tb.packets, kib(tb.bytes),
					kib(tb.bytes) / seconds, tw.packets, kib(tw.bytes), kib(tw.bytes) / seconds, tb.unsized, tw.unsized, tb.signs, tw.signs, kib(tw.signBytes)));
			lines.add("network, workshop, by kind: " + tw.top(12));
			lines.add("network not measured: Jade's and the encyclopedia's replies (they answer a client's request); compression and framing");
			lines.add(String.format(Locale.ROOT, "budget (C-P19 in C-P67's form, proposed; the owner decides at merge review): median +%.1f ms: %s; p95 +%.1f ms: %s; retained +%.0f MiB beyond the baseline's: %s. Asserted here: +%.1f, +%.1f ms, +%.0f MiB",
					BUDGET_MEDIAN_MS, deltaMedian <= BUDGET_MEDIAN_MS ? "within" : "over", BUDGET_P95_MS, deltaP95 <= BUDGET_P95_MS ? "within" : "over", BUDGET_RETAINED_MIB,
					!memory ? "not measured" : retained <= BUDGET_RETAINED_MIB ? "within" : "over", LIMIT_MEDIAN_MS, LIMIT_P95_MS, LIMIT_RETAINED_MIB));
			lines.add("===== end =====");
			for (String line : lines) {
				Jugcraft.LOGGER.info("Concordance load card | {}", line);
			}
		}
	}

	/** C-10's workshop: built in one tick, driven as its player would, and audited every AUDIT ticks. */
	private static final class Shop {
		record Tally(int poultices, int foundations, int porterRequests, int porterItems, int peepoBrought, Map<Item, Integer> harvests,
				int eaten, int draws, int interruptions) {
			int harvested() {
				return harvests.values().stream().mapToInt(Integer::intValue).sum();
			}
		}

		final Card card;
		final GameTestHelper helper;
		final ServerLevel level;
		final ServerPlayer player;
		final Set<String> problems = new LinkedHashSet<>();
		final List<String> interruptions = new ArrayList<>();
		final Map<String, Integer> waits = new TreeMap<>();
		final Map<Item, int[]> yields = new LinkedHashMap<>();
		final Set<Event> seen = new HashSet<>();
		final List<String> built = new ArrayList<>();
		CrucibleBlockEntity crucible;
		Container bowls;
		Container bowlHopper;
		Container peepoSupply;
		Container postChest;
		Container postStore;
		Container store;
		Container storeHopper;
		Container chaff;
		Container mawHopper;
		UUID peepoId;
		UUID porterId;
		CircleAnchorBlockEntity anchor;
		GleanerBlockEntity gleaner;
		MulchMawBlockEntity maw;
		CourierLedger ledger;
		Place post;
		Place here;
		Item plate;
		RitualRun recorded;
		Tally marked;
		int poultices;
		int wrong;
		int foundations;
		int plates;
		int copper;
		int riteMendvetch;
		int lit;
		int sampled;
		int draws;
		int porterRequests;
		int porterItems;
		int cells;
		int cables;
		int stones;
		int beds;
		long lastVerdance;

		Shop(Card card) {
			this.card = card;
			this.helper = card.helper;
			this.level = card.level;
			this.player = card.player;
		}

		void build() {
			// Heat and water: a Creative Energy Cell feeds the stand through a cable; a pump on a walled spring pipes water into the tank.
			cell(STAND_CELL);
			cable(STAND_CABLE);
			helper.setBlock(STAND, JugcraftConcordance.CRUCIBLE_STAND);
			helper.setBlock(CRUCIBLE, JugcraftConcordance.CRUCIBLE);
			crucible = helper.getBlockEntity(CRUCIBLE, CrucibleBlockEntity.class);
			for (BlockPos wall : List.of(SPRING.north(), SPRING.south(), SPRING.west(), SPRING.east())) {
				helper.setBlock(wall, Blocks.STONE);
			}
			helper.setBlock(SPRING, Blocks.WATER);
			helper.setBlock(PUMP, JugcraftFluids.ELECTRIC_PUMP);
			helper.setBlock(PIPE, JugcraftFluids.BRONZE_FLUID_PIPE);
			cell(PUMP_CELL);
			helper.setBlock(BOWL_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
			bowlHopper = container(BOWL_HOPPER);
			bowls = filled(BOWL_CHEST, Blocks.CHEST, new ItemStack(Items.BOWL, 64), new ItemStack(Items.BOWL, 64));
			crucible.follow(POULTICE, player.getUUID());
			// The poultice is bottled in bowls: the Peepo's port must count them as the crucible's containers, not a return.
			crucible.companion.selectSalve(true);
			// The Peepo, from its own barrel (CompanionAutomationChecks.npc and walking).
			peepoSupply = filled(PEEPO_SUPPLY, Blocks.BARREL, mendvetch(), mendvetch(), mendvetch());
			PeepoEntity peepo = PeepoMod.PEEPO.create(level, EntitySpawnReason.COMMAND);
			helper.assertTrue(peepo != null, "A Peepo is made");
			peepo.setNoAi(true);
			peepo.setNoGravity(true);
			Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(PEEPO_AT));
			peepo.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			level.addFreshEntity(peepo);
			peepo.orders.tame(player);
			peepo.preferences.carryMeals = 0;
			peepo.assignments.assign(level, helper.absolutePos(CRUCIBLE));
			peepo.assignments.assignContainer(level, helper.absolutePos(PEEPO_SUPPLY), Direction.UP, false);
			peepo.setNoAi(false);
			peepo.setNoGravity(false);
			peepo.setOnGround(true);
			peepoId = peepo.getUUID();
			// The porter at its post, more than eight blocks from the Peepo's barrel (the porter's source box), fuelled at a pylon.
			helper.setBlock(POST, Couriers.COURIER_POST);
			helper.getBlockEntity(POST, CourierPostBlockEntity.class).setOwner(player.getUUID());
			postStore = container(POST);
			postChest = filled(POST_CHEST, Blocks.CHEST, mendvetch(), mendvetch(), mendvetch());
			cell(POST_CELL);
			helper.setBlock(POST_PYLON, JugcraftConcordance.LEY_PYLON);
			WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
			helper.assertTrue(terms != null, "The porter's terms load");
			ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(porter != null, "A porter is made");
			porter.setOwner(player.getUUID());
			porter.setBody(new Body(terms.integrity(), terms.energy()));
			Vec3 by = helper.absoluteVec(Vec3.atBottomCenterOf(PORTER_AT));
			porter.snapTo(by.x, by.y, by.z, 0.0F, 0.0F);
			level.addFreshEntity(porter);
			helper.assertTrue(porter.setPost(GlobalPos.of(level.dimension(), helper.absolutePos(POST))), "The porter takes the post");
			porterId = porter.getUUID();
			ledger = CourierLedger.of(level.getServer());
			post = Couriers.place(level, helper.absolutePos(POST));
			here = Couriers.place(level, helper.absolutePos(CRUCIBLE));
			// The Lesser Circle (ConcordanceRitualGameTests.build): pylons on cables under the floor, from one cell.
			helper.setBlock(ANCHOR, JugcraftConcordance.CIRCLE_ANCHOR);
			int pylons = 0;
			for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").parts()) {
				BlockPos at2 = ANCHOR.offset(part.offset().x(), part.offset().y(), part.offset().z());
				if (part.role() == StructurePattern.Role.CHANNEL) {
					helper.setBlock(at2, JugcraftConcordance.LEY_PYLON);
					helper.getBlockEntity(at2, LeyPylonBlockEntity.class).setLey(level, LeyPylonBlockEntity.CAPACITY);
					pylons++;
				} else if (part.role() == StructurePattern.Role.BOUNDARY) {
					helper.setBlock(at2, JugcraftConcordance.WARDING_STONE);
					stones++;
				}
			}
			for (int d = -2; d <= 2; d++) {
				cable(ANCHOR.offset(d, -1, 0));
			}
			for (int d = -2; d <= 3; d++) {
				if (d != 0) {
					cable(ANCHOR.offset(0, -1, d));
				}
			}
			cell(CIRCLE_CELL);
			anchor = helper.getBlockEntity(ANCHOR, CircleAnchorBlockEntity.class);
			plate = BuiltInRegistries.ITEM.getValue(Identifier.parse("jugcraft:thallite_plate"));
			helper.assertTrue(plate != Items.AIR, "The Thallite plate is registered");
			// The garden: the Heart's bed and nine planted beds, the mixed bed in a dark room, water (on stone) within reach of each.
			bed(HEART_BED);
			helper.setBlock(HEART_BED.above(), Garden.VERDANT_HEART);
			VerdantHeartBlockEntity heart = helper.getBlockEntity(HEART_BED.above(), VerdantHeartBlockEntity.class);
			heart.awaken(player.getUUID());
			heart.setVerdance(VerdantHeartBlockEntity.CAPACITY);
			for (Planting planting : CROPS) {
				bed(planting.bed());
				OrganismCropBlock crop = (OrganismCropBlock) planting.crop();
				helper.setBlock(planting.bed().above(), crop.getStateForAge(planting.ripe() ? OrganismCropBlock.MAX_AGE : OrganismCropBlock.MAX_AGE - 1));
				List<ItemStack> yield = crop.harvestYield(crop.getStateForAge(OrganismCropBlock.MAX_AGE));
				ItemStack produce = yield.stream().filter(stack -> !stack.is(Garden.VERDANT_CHAFF)).findFirst().orElseThrow();
				yields.putIfAbsent(produce.getItem(), new int[] {produce.getCount(), count(yield, Garden.VERDANT_CHAFF)});
			}
			for (int x = 30; x <= 33; x++) {
				for (int z = 25; z <= 28; z++) {
					helper.setBlock(p(x, 3, z), Blocks.STONE);
					if (x == 30 || x == 33 || z == 25 || z == 28) {
						helper.setBlock(p(x, 2, z), Blocks.STONE);
					}
				}
			}
			for (BlockPos water : WATER) {
				helper.setBlock(water.below(), Blocks.STONE);
				helper.setBlock(water, Blocks.WATER);
			}
			helper.setBlock(GLEANER, Garden.GLEANER);
			gleaner = helper.getBlockEntity(GLEANER, GleanerBlockEntity.class);
			gleaner.awaken(player.getUUID());
			helper.setBlock(GLEANER_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
			storeHopper = container(GLEANER_HOPPER);
			store = filled(STORE, Blocks.CHEST);
			helper.setBlock(MAW, Garden.MULCH_MAW);
			maw = helper.getBlockEntity(MAW, MulchMawBlockEntity.class);
			maw.awaken(player.getUUID());
			helper.setBlock(MAW_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
			mawHopper = container(MAW_HOPPER);
			chaff = filled(MAW_CHEST, Blocks.CHEST, new ItemStack(Garden.VERDANT_CHAFF, 64), new ItemStack(Garden.VERDANT_CHAFF, 64),
					new ItemStack(Garden.VERDANT_CHAFF, 64), new ItemStack(Garden.VERDANT_CHAFF, 64));
			lastVerdance = gleaner.verdance();
			built.addAll(List.of("Crucible Stand 1", "Alembic Crucible 1", "Electric Pump 1 (bronze pipe 1, spring 1)", "Creative Energy Cells " + cells,
					"copper cables " + cables, "Ley Pylons " + (pylons + 1) + " (" + pylons + " in the circle, 1 at the post)", "Circle Anchor 1",
					"Warding Stones " + stones, "Courier Post 1", "hoppers 3", "containers 5", "Peepo 1", "Clockwork Porter 1",
					"Verdant Beds " + beds + " (" + CROPS.size() + " planted: Mendvetch 3, Sunpetal 2, Dewmoss 2, Gloamcap 2; the Verdant Heart's 1)",
					"Verdant Heart 1", "Mulch Maw 1", "Gleaner 1", "water sources " + (WATER.size() + 1)));
			start();
		}

		void cell(BlockPos at) {
			helper.setBlock(at, CreativeEnergyCellBlock.BLOCK);
			cells++;
		}

		void cable(BlockPos at) {
			helper.setBlock(at, JugcraftMachines.COPPER_CABLE);
			cables++;
		}

		void bed(BlockPos at) {
			helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
			VerdantBedBlockEntity bed = helper.getBlockEntity(at, VerdantBedBlockEntity.class);
			bed.awaken(player.getUUID());
			bed.setNutrients(8);
			beds++;
		}

		Container container(BlockPos at) {
			return (Container) level.getBlockEntity(helper.absolutePos(at));
		}

		Container filled(BlockPos at, Block block, ItemStack... stacks) {
			helper.setBlock(at, block);
			Container container = container(at);
			for (int slot = 0; slot < stacks.length; slot++) {
				container.setItem(slot, stacks[slot]);
			}
			return container;
		}

		static ItemStack mendvetch() {
			return new ItemStack(Garden.MENDVETCH, 64);
		}

		/** The Peepo and the porter, read live by their UUIDs (never a held reference; CompanionAutomationChecks' lesson). */
		PeepoEntity peepo() {
			return level.getEntity(peepoId) instanceof PeepoEntity found ? found : null;
		}

		ClockworkPorterEntity porter() {
			return level.getEntity(porterId) instanceof ClockworkPorterEntity found ? found : null;
		}

		void drive(int now) {
			rite();
			collect();
			long verdance = gleaner.verdance();
			if (verdance > lastVerdance) {
				draws++;
			}
			lastVerdance = verdance;
			if (now % WINDOW == 0) {
				Diagnostic waiting = crucible.waiting();
				waits.merge(waiting == null ? "none" : waiting.code().id, 1, Integer::sum);
				lit += helper.getBlockState(STAND).getValue(BlockStateProperties.LIT) ? 1 : 0;
				sampled++;
				history();
			}
			if (now % AUDIT == 0) {
				audit("tick " + now);
			}
		}

		/** Root and Rivet again and again: the participant takes the foundation and offers the next run's. */
		void rite() {
			RitualMachine.Phase phase = anchor.phase();
			if (phase == RitualMachine.Phase.COMPLETE) {
				use();
				foundations += take(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION);
				start();
			} else if (phase == RitualMachine.Phase.IDLE) {
				RitualRun run = anchor.run();
				if (run != null && !run.completed() && run.last() != null && !run.equals(recorded)) {
					recorded = run;
					interruptions.add(card.server.getTickCount() + " " + run.last());
				}
				if (anchor.offerings().stream().allMatch(ItemStack::isEmpty)) {
					start();
				} else {
					use();
				}
			}
		}

		void start() {
			for (ItemStack stack : List.of(new ItemStack(plate), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Garden.MENDVETCH, 4))) {
				anchor.offer(player, stack);
			}
			plates++;
			copper += 4;
			riteMendvetch += 4;
			use();
		}

		void use() {
			ConcordanceProgress.setFocus(player, 20);
			RateGate.forget(player.getUUID());
			anchor.use(player, level, false);
		}

		int take(Item item) {
			Inventory inventory = player.getInventory();
			int taken = 0;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (stack.is(item)) {
					taken += stack.getCount();
					inventory.setItem(slot, ItemStack.EMPTY);
				}
			}
			return taken;
		}

		/** Takes each brew out as it is bottled, as an extractor would, and checks that it is the clean poultice. */
		void collect() {
			ItemStack out = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			if (out.isEmpty()) {
				return;
			}
			Product product = Alchemy.product(out);
			if (out.is(JugcraftConcordance.SALVE) && product != null && product.id().equals(Alchemy.STILLROOT_POULTICE) && CLEAN.equals(out.get(JugcraftConcordance.BREW))) {
				poultices += out.getCount();
			} else {
				wrong += out.getCount();
				problems.add("not the clean Stillroot Poultice: " + out + " " + out.get(JugcraftConcordance.BREW));
			}
			crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		}

		/** The post's history (at most 32 events), read every 100 ticks and at each tally: requests done, and every item delivered. */
		void history() {
			for (Event event : ledger.ledger().history(post)) {
				if (!seen.add(event)) {
					continue;
				}
				if (event.kind().equals("done")) {
					porterRequests++;
					porterItems += event.amount();
				} else if (event.kind().equals("delivered")) {
					porterItems += event.amount();
				}
			}
		}

		int carried() {
			int n = 0;
			for (Request request : ledger.ledger().requests()) {
				if (request.ticket().destination().equals(here)) {
					n += request.progress().carried();
				}
			}
			return n;
		}

		int batches() {
			return poultices + wrong + count(crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT), JugcraftConcordance.SALVE);
		}

		int buffer() {
			int n = 0;
			for (int slot = 0; slot < CrucibleBlockEntity.BUFFER_SLOTS; slot++) {
				n += count(crucible.getItem(slot), Garden.MENDVETCH);
			}
			return n;
		}

		int peepoHeld() {
			PeepoEntity peepo = peepo();
			if (peepo == null) {
				problems.add("the Peepo is gone");
				return 0;
			}
			return count(peepo.belongings, Garden.MENDVETCH);
		}

		int aboard() {
			ClockworkPorterEntity porter = porter();
			if (porter == null) {
				problems.add("the porter is gone");
				return 0;
			}
			return porter.carriedCount();
		}

		int eaten() {
			return CHAFF - count(chaff, Garden.VERDANT_CHAFF) - count(mawHopper, Garden.VERDANT_CHAFF) - count(maw.food(), Garden.VERDANT_CHAFF);
		}

		/** What the Gleaner has harvested, by crop: its produce stored, held or in its hopper, in whole harvests, which agree with the chaff. */
		Map<Item, Integer> harvests() {
			Map<Item, Integer> byCrop = new LinkedHashMap<>();
			int chaffExpected = 0;
			for (Map.Entry<Item, int[]> crop : yields.entrySet()) {
				int made = count(store, crop.getKey()) + count(storeHopper, crop.getKey()) + count(gleaner, crop.getKey());
				if (made % crop.getValue()[0] != 0) {
					problems.add("the Gleaner's " + crop.getKey() + " is not whole harvests: " + made);
				}
				byCrop.put(crop.getKey(), made / crop.getValue()[0]);
				chaffExpected += made / crop.getValue()[0] * crop.getValue()[1];
			}
			int chaffMade = count(store, Garden.VERDANT_CHAFF) + count(storeHopper, Garden.VERDANT_CHAFF) + count(gleaner, Garden.VERDANT_CHAFF);
			if (chaffMade != chaffExpected) {
				problems.add("the Gleaner's chaff " + chaffMade + " is not its harvests' " + chaffExpected);
			}
			return byCrop;
		}

		Tally tally() {
			history();
			int peepoBrought = SUPPLY - count(peepoSupply, Garden.MENDVETCH) - peepoHeld();
			return new Tally(poultices, foundations, porterRequests, porterItems, peepoBrought, harvests(), eaten(), draws, interruptions.size());
		}

		/** The measured span begins: what has happened so far, and the samples start afresh. */
		void mark() {
			marked = tally();
			waits.clear();
			lit = 0;
			sampled = 0;
		}

		String difference(Tally end, Tally from) {
			Map<String, Integer> out = new LinkedHashMap<>();
			end.harvests().forEach((item, n) -> out.put(BuiltInRegistries.ITEM.getKey(item).getPath(), n - from.harvests().getOrDefault(item, 0)));
			return out.toString();
		}

		/** Every item in, out, carried and stored, each kind on its own line of account; the ledger's own check; nothing on the ground. */
		void audit(String when) {
			int batches = batches();
			int supplies = count(peepoSupply, Garden.MENDVETCH) + count(postChest, Garden.MENDVETCH) + count(postStore, Garden.MENDVETCH);
			int peepoHeld = peepoHeld();
			int carried = carried();
			int aboard = aboard();
			int buffer = buffer();
			int used = 2 * batches + adds(crucible.step());
			if (supplies + peepoHeld + carried + aboard + buffer + used != 2 * SUPPLY) {
				problems.add(when + ": Mendvetch " + 2 * SUPPLY + " is not supplies " + supplies + " + Peepo " + peepoHeld + " + carried " + carried
						+ " + porter " + aboard + " + buffer " + buffer + " + used " + used);
			}
			int bowlsLeft = count(bowls, Items.BOWL) + count(bowlHopper, Items.BOWL) + count(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT), Items.BOWL);
			if (bowlsLeft + batches != BOWLS) {
				problems.add(when + ": bowls " + BOWLS + " is not " + bowlsLeft + " left + " + batches + " used");
			}
			List<ItemStack> held = anchor.offerings();
			int made = foundations + count(anchor.output(), JugcraftConcordance.ROOTED_THALLITE_FOUNDATION);
			if (plates != made + count(held, plate) || copper != 4 * made + count(held, Items.COPPER_INGOT)
					|| riteMendvetch != 4 * made + count(held, Garden.MENDVETCH)) {
				problems.add(when + ": the rite's offerings (" + plates + ", " + copper + ", " + riteMendvetch + ") are not " + made + " foundations and " + held);
			}
			harvests();
			if (eaten() < 0) {
				problems.add(when + ": the Maw's chaff grew: " + eaten());
			}
			List<ItemEntity> ground = level.getEntitiesOfClass(ItemEntity.class, helper.absoluteAABB(new AABB(OFF, 0, OFF, OFF + SIZE + 1, 16, OFF + SIZE + 1)));
			if (!ground.isEmpty()) {
				problems.add(when + ": items on the ground: " + ground.stream().map(item -> item.getItem().toString()).toList());
			}
			List<String> ledgerProblems = ledger.problems();
			if (!ledgerProblems.isEmpty()) {
				problems.add(when + ": the courier ledger: " + ledgerProblems);
			}
		}

		String inventoryLine() {
			int batches = batches();
			return String.format(Locale.ROOT, "Mendvetch %d = supplies %d + Peepo %d + carried %d + porter %d + buffer %d + used %d; bowls %d = left %d + used %d; "
					+ "rite plates %d, copper %d, Mendvetch %d = %d foundations (4 copper, 4 Mendvetch each) + held %s; Maw ate %d of %d chaff; open requests %d; ground and ledger checked",
					2 * SUPPLY, count(peepoSupply, Garden.MENDVETCH) + count(postChest, Garden.MENDVETCH) + count(postStore, Garden.MENDVETCH), peepoHeld(), carried(),
					aboard(), buffer(), 2 * batches + adds(crucible.step()), BOWLS,
					count(bowls, Items.BOWL) + count(bowlHopper, Items.BOWL) + count(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT), Items.BOWL), batches,
					plates, copper, riteMendvetch, foundations, anchor.offerings(), eaten(), CHAFF, ledger.ledger().requests().size());
		}
	}
}
