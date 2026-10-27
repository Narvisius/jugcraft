package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.compose.Catalog;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionGraph;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Roadmap step 32's performance measurements (docs/features/arcane-concordance-delivery.md): what five pieces of the
 * Concordance's server work cost on a real server, timed with {@code System.nanoTime} and written to the log as
 * "Concordance workload: ...", which the record copies from CI:
 * <ul>
 * <li>a block change's look-up of the circle anchors in reach (every block broken or placed near a circle);</li>
 * <li>compiling a spell (every inscription, check and cast of a composed spell);</li>
 * <li>the progression graph's fixed point (the graph's own audit runs it seven times at every data load: once, and once
 * for each route through the two middle stages);</li>
 * <li>a level's whole garden allowance for one tick (16 area samples of at most 100 reads);</li>
 * <li>a Gleaner's pulse, starved (its whole Heart search) and fed (its crop search), Concordance Chunk C, C-P21 and
 * C-P68 (gleanersHeartSearch).</li>
 * </ul>
 * The times are a CI runner's, shared with every other test running at once: an indication, not a profile. The bounds
 * asserted are budgets about a hundred times what the work should take, there to catch a change that makes the work
 * grow, not to time the runner.
 */
public class ConcordanceWorkloadGameTests {
	private static long micros(long nanos, int times) {
		return nanos / 1000L / Math.max(1, times);
	}

	@GameTest(maxTicks = 20)
	public void measuredWorkloads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(1, 2, 1), JugcraftConcordance.CIRCLE_ANCHOR);
		helper.setBlock(new BlockPos(6, 2, 6), JugcraftConcordance.CIRCLE_ANCHOR);
		BlockPos centre = helper.absolutePos(new BlockPos(4, 2, 4));

		int lookups = 20_000;
		int found = 0;
		long start = System.nanoTime();
		for (int i = 0; i < lookups; i++) {
			found += Rituals.near(level, centre.offset(i % 9 - 4, 0, i / 9 % 9 - 4)).size();
		}
		long near = System.nanoTime() - start;

		Catalog catalog = ConcordanceData.rules().catalog();
		Instrument wand = catalog.instrumentFor("jugcraft:initiate_wand");
		Compiler.Knows mastered = research -> ResearchState.MASTERED;
		int compiles = 2_000;
		int plans = 0;
		start = System.nanoTime();
		for (int i = 0; i < compiles; i++) {
			if (Compiler.compile("ray struck sear then here creatures dazzle", catalog, mastered, wand).plan() != null) {
				plans++;
			}
		}
		long compile = System.nanoTime() - start;

		ProgressionGraph graph = ConcordanceData.rules().progression().graph();
		int walks = 50;
		int reached = 0;
		start = System.nanoTime();
		for (int i = 0; i < walks; i++) {
			reached = graph.reachable(true).size();
		}
		long walk = System.nanoTime() - start;

		int samples = 0;
		start = System.nanoTime();
		for (int i = 0; i < 16; i++) {
			if (Garden.sample(level, centre.offset(i % 4, 0, i / 4)) != null) {
				samples++;
			}
		}
		long sample = System.nanoTime() - start;

		Jugcraft.LOGGER.info(String.format(Locale.ROOT, "Concordance workload: circle look-up %d us (%d anchors loaded in the level), "
				+ "spell compile %d us, progression walk %d us (%d nodes), garden sample %d us (%d of 16 taken this tick)",
				micros(near, lookups), Rituals.loaded(level), micros(compile, compiles), micros(walk, walks), reached,
				micros(sample, samples), samples));
		helper.assertTrue(found > 0 && plans == compiles && reached > 0, "every workload did its work: " + found + ", " + plans + ", " + reached);
		helper.assertTrue(micros(near, lookups) < 200, "a circle look-up stays within 0.2 ms: " + micros(near, lookups) + " us");
		helper.assertTrue(micros(compile, compiles) < 5_000, "a compile stays within 5 ms: " + micros(compile, compiles) + " us");
		helper.assertTrue(micros(walk, walks) < 200_000, "a progression walk stays within 0.2 s: " + micros(walk, walks) + " us");
		helper.assertTrue(samples == 0 || micros(sample, samples) < 5_000, "a garden sample stays within 5 ms: " + micros(sample, samples) + " us");
		helper.succeed();
	}

	/**
	 * C-P68 (C-P21's line): a starved Gleaner pulse (its whole Heart search) at or under this many microseconds, on the
	 * runner the "Gleaner pulse" line names, keeps HEART_REACH.
	 */
	static final long HEART_SEARCH_LINE_US = 250;

	/**
	 * Concordance Chunk C, C-P21: what a Gleaner's pulse costs. Starved (no Verdance and no Verdant Heart in reach), a pulse
	 * searches its whole Heart reach, 9 by 9 by 9 = 729 positions (GleanerBlockEntity.draw). That is the worst case, and it
	 * repeats every 40 ticks while the Heart is empty or gone. The position list alone (Garden.nearest, built and sorted on
	 * every call) is timed apart. Fed, a pulse searches its crop reach (7 by 3 by 7 = 147) and finds nothing ripe. Each
	 * timing follows a thousand untimed pulses, and the line names the runner it ran on (the mod job's, in CI).
	 */
	@GameTest(maxTicks = 20)
	public void gleanersHeartSearch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = TestPlayers.mock(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at, Garden.GLEANER);
		GleanerBlockEntity gleaner = helper.getBlockEntity(at, GleanerBlockEntity.class);
		gleaner.awaken(keeper.getUUID());
		BlockPos centre = helper.absolutePos(at);
		int reach = GleanerBlockEntity.HEART_REACH;
		long loaded = Garden.nearest(centre, reach, reach).stream().filter(level::isLoaded).count();
		int pulses = 200;
		int warmup = 1_000;
		for (int i = 0; i < warmup; i++) {
			gleaner.glean(level);
		}
		int starved = 0;
		long start = System.nanoTime();
		for (int i = 0; i < pulses; i++) {
			if (gleaner.glean(level).equals("no_verdance")) {
				starved++;
			}
		}
		long search = System.nanoTime() - start;
		int positions = 0;
		start = System.nanoTime();
		for (int i = 0; i < pulses; i++) {
			positions += Garden.nearest(centre, reach, reach).size();
		}
		long listing = System.nanoTime() - start;
		BlockPos heartBed = new BlockPos(6, 1, 4);
		helper.setBlock(heartBed, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
		VerdantBedBlockEntity bed = helper.getBlockEntity(heartBed, VerdantBedBlockEntity.class);
		bed.awaken(keeper.getUUID());
		bed.setNutrients(10);
		helper.setBlock(heartBed.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = helper.getBlockEntity(heartBed.above(), VerdantHeartBlockEntity.class);
		heart.awaken(keeper.getUUID());
		heart.setVerdance(VerdantHeartBlockEntity.CAPACITY);
		String fed = gleaner.glean(level);
		for (int i = 0; i < warmup; i++) {
			gleaner.glean(level);
		}
		int idle = 0;
		start = System.nanoTime();
		for (int i = 0; i < pulses; i++) {
			if (gleaner.glean(level).equals("idle")) {
				idle++;
			}
		}
		long crops = System.nanoTime() - start;
		Jugcraft.LOGGER.info(String.format(Locale.ROOT, "Concordance workload: Gleaner pulse starved %d us (its Heart search, %d positions, %d of them loaded; "
				+ "the position list alone %d us), fed %d us (its crop search, 147 positions), each after %d untimed pulses; C-P21's line %d us: %s; "
				+ "runner: %d processors, %s, Java VM %s",
				micros(search, pulses), positions / pulses, loaded, micros(listing, pulses), micros(crops, pulses), warmup, HEART_SEARCH_LINE_US,
				micros(search, pulses) <= HEART_SEARCH_LINE_US ? "within" : "over", Runtime.getRuntime().availableProcessors(),
				System.getProperty("os.arch"), System.getProperty("java.vm.version")));
		helper.assertTrue(positions == pulses * 729 && starved == pulses,
				"The Heart reach is 729 positions; every pulse was starved: " + positions / pulses + ", " + starved + " of " + pulses);
		helper.assertTrue(fed.equals("idle") && gleaner.verdance() > 0 && idle == pulses, "Fed by the Heart, each pulse searched its crops and found none ripe: " + fed + ", " + idle);
		helper.assertTrue(micros(search, pulses) < 5_000 && micros(crops, pulses) < 5_000,
				"A Gleaner pulse stays within 5 ms: " + micros(search, pulses) + " us, " + micros(crops, pulses) + " us");
		helper.succeed();
	}
}
