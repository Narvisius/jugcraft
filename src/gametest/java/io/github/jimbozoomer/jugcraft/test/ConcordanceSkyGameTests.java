package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Attunement;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StarReckoning;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Roadmap step 15, celestial cycles and attunement (docs/features/arcane-concordance-celestial.md), on a real server.
 * Every test passes its own world time and game time, and its own view of the sky where weather would decide, so none
 * moves the world's clock or waits on rain: the calendar agrees with the grandfather clock's moon and the Halloween
 * event's Harvest Moon; an observatory gathers each occurrence once for its keeper, however the clock is turned back or
 * forward, and pays again only when enough of the world's own time has run; it says why it waits (unaligned, no sky,
 * full); an astrolabe attunes while a pattern is up and the effect stops when it sets or the attunement ends; a master's
 * recall pays once per occurrence; and claims and observatories keep what they hold through a save; and (Concordance Chunk
 * D, D-05) a holder of Stolas's pact, no master, recalls at his perch any pattern they observed, through the one recall a
 * master's shares, once an occurrence whatever the clock, as far as their resonance goes.
 */
public class ConcordanceSkyGameTests {
	private static final String FULL_MOON = "jugcraft:full_moon";
	private static final String HARVEST_MOON = "jugcraft:harvest_moon";
	private static final String LANTERN_STAR = "jugcraft:lantern_star";
	private static final String WINTER_CROWN = "jugcraft:winter_crown";
	private static final String ECHO_COMET = "jugcraft:echo_comet";
	private static final long DAY = Calendar.DAY;
	/** Midnight of day 0: the full moon is up (and nothing else that keeps to no season). */
	private static final long NIGHT_ZERO = 18000L;
	/** A sky in which only the full moon can be seen: the other patterns are hidden, so tests never depend on them. */
	private static final Function<Pattern, String> ONLY_MOON = pattern -> pattern.id().equals(FULL_MOON) ? null : "no_sky";

	private static ServerPlayer player(GameTestHelper helper, ResearchState state) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		if (state != ResearchState.NONE) {
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, Sky.RESEARCH, state);
		}
		RateGate.forget(player.getUUID());
		return player;
	}

	/** An observatory at {@code at}, placed by {@code placer}. */
	private static ObservatoryBlockEntity observatory(GameTestHelper helper, BlockPos at, ServerPlayer placer) {
		helper.setBlock(at, Sky.OBSERVATORY);
		BlockPos absolute = helper.absolutePos(at);
		Sky.OBSERVATORY.setPlacedBy(helper.getLevel(), absolute, helper.getBlockState(at), placer, ItemStack.EMPTY);
		return (ObservatoryBlockEntity) helper.getLevel().getBlockEntity(absolute);
	}

	private static ItemStack astrolabe(int charge) {
		ItemStack stack = new ItemStack(Sky.ASTROLABE);
		AstrolabeItem.setCharge(stack, charge);
		return stack;
	}

	// ---------------------------------------------------------------- the calendar

	/**
	 * The sky keeps the world's clock: the moon phase agrees with the grandfather clock's at every time tried, the full
	 * moon is up on night 0 and not on night 1, the Harvest Moon pattern is up exactly when the Halloween event's Harvest
	 * Moon is, and the forecast and clock read sensibly.
	 */
	@GameTest(maxTicks = 20)
	public void theSkyKeepsTheWorldsClock(GameTestHelper helper) {
		helper.assertTrue(Sky.catalog().patterns().size() == 6, "Six patterns load: " + Sky.catalog().patterns().keySet());
		for (long time = -3 * DAY; time < 20 * DAY; time += 5003) {
			helper.assertTrue(Calendar.moonPhase(time) == GrandfatherClockBlock.moonPhase(time), "The moon agrees with the grandfather clock at " + time);
		}
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		helper.assertTrue(full != null && Sky.up(NIGHT_ZERO).contains(full) && !Sky.up(NIGHT_ZERO + DAY).contains(full),
				"The full moon is up on night 0 and not on night 1");
		Pattern harvest = Sky.catalog().pattern(HARVEST_MOON);
		for (long time : new long[] {NIGHT_ZERO, 6000L, 3 * DAY + 14000}) {
			helper.assertTrue(Sky.inSeason(harvest, time) == HarvestMoon.rising(time), "The Harvest Moon pattern keeps the event's Harvest Moon at " + time);
		}
		helper.assertTrue(Sky.forecast(0L, Sky.FORECAST_DAYS).size() >= 2, "The forecast reads today and what comes round");
		helper.assertTrue(Sky.clock(0L).equals("06:00") && Sky.clock(13000L).equals("19:00") && Sky.clock(18000L).equals("00:00"),
				"Ticks read as a clock: " + Sky.clock(0L) + " " + Sky.clock(13000L) + " " + Sky.clock(18000L));
		helper.succeed();
	}

	// ---------------------------------------------------------------- gathering

	/**
	 * An observatory gathers the full moon once a night for its keeper: the same night again, the clock turned back a
	 * week, or turned forward to the next full moon or far beyond, pays nothing; a week of the world's own time later the
	 * next full moon pays; a second observatory of the same keeper gathers nothing more; a full one stops.
	 */
	@GameTest(maxTicks = 20)
	public void anObservatoryGathersEachOccurrenceOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer starwatcher = player(helper, ResearchState.UNDERSTOOD);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(1, 1, 1), starwatcher);
		helper.assertTrue(observatory.aligned() && starwatcher.getUUID().equals(observatory.keeper()), "A Starwatcher's observatory is aligned to them");
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		int moon = full.resonance();
		long game = 1_000_000L;
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO, game, ONLY_MOON).equals("gathering") && observatory.resonance() == moon,
				"It gathers the full moon: " + observatory.resonance());
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO + 2000, game + 2000, ONLY_MOON).equals("gathered") && observatory.resonance() == moon,
				"The same night pays once");
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO - 8 * DAY, game + 4000, ONLY_MOON).equals("gathered") && observatory.resonance() == moon,
				"Turning the clock back a week pays nothing");
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO + 8 * DAY, game + 6000, ONLY_MOON).equals("too_soon") && observatory.resonance() == moon,
				"Turning the clock forward to the next full moon pays nothing yet");
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO + 80 * DAY, game + 8000, ONLY_MOON).equals("too_soon") && observatory.resonance() == moon,
				"Nor turning it far forward");
		long week = game + Calendar.minGap(full);
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO + 8 * DAY, week, ONLY_MOON).equals("gathering") && observatory.resonance() == 2 * moon,
				"The next full moon, after enough of the world's own time, pays: " + observatory.resonance());
		ObservatoryBlockEntity second = observatory(helper, new BlockPos(4, 1, 1), starwatcher);
		helper.assertTrue(second.gather(level, NIGHT_ZERO + 8 * DAY, week + 100, ONLY_MOON).equals("gathered") && second.resonance() == 0,
				"A second observatory of the same keeper gathers nothing more");
		observatory.setResonance(ObservatoryBlockEntity.CAPACITY - 1);
		helper.assertTrue(observatory.gather(level, NIGHT_ZERO + 16 * DAY, week + Calendar.minGap(full), ONLY_MOON).equals("full")
				&& observatory.resonance() == ObservatoryBlockEntity.CAPACITY - 1, "A full observatory stops, and says so");
		helper.succeed();
	}

	/**
	 * Why an observatory waits: placed by a novice it is unaligned until a Starwatcher's empty hand aligns it; under a
	 * roof it sees no sky (by the real sky over it, the server's).
	 */
	@GameTest(maxTicks = 20)
	public void anObservatorySaysWhyItWaits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer novice = player(helper, ResearchState.NONE);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(1, 1, 1), novice);
		helper.assertTrue(!observatory.aligned() && observatory.gather(level, NIGHT_ZERO, 2_000_000L, ONLY_MOON).equals("unaligned"),
				"A novice's observatory is unaligned and gathers nothing");
		ServerPlayer starwatcher = player(helper, ResearchState.UNDERSTOOD);
		observatory.use(starwatcher, level);
		helper.assertTrue(observatory.aligned() && starwatcher.getUUID().equals(observatory.keeper()), "A Starwatcher's empty hand aligns it");
		helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
		String status = observatory.gather(level, NIGHT_ZERO, 2_000_000L);
		helper.assertTrue(status.equals(level.dimension() == Level.OVERWORLD ? "no_sky" : "elsewhere") && observatory.resonance() == 0,
				"Under a roof it sees no sky: " + status);
		helper.succeed();
	}

	// ---------------------------------------------------------------- attunement

	/**
	 * An astrolabe attunes a Starwatcher to the full moon while it is up: it spends the cost, and the pulse gives night
	 * vision while the moon is up and not once it has set; the attunement ends by game time. A novice cannot attune, and
	 * an astrolabe holding too little does not.
	 */
	@GameTest(maxTicks = 20)
	public void anAstrolabeAttunesWhileThePatternIsUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		ServerPlayer novice = player(helper, ResearchState.NONE);
		ItemStack held = astrolabe(10);
		helper.assertTrue(held.has(Sky.RESONANT), "A charged astrolabe is resonant (what an optional dynamic light keys on)");
		helper.assertTrue(!AstrolabeItem.attune(novice, level, held, NIGHT_ZERO, 100L, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == 10, "A novice cannot attune");
		ServerPlayer starwatcher = player(helper, ResearchState.UNDERSTOOD);
		ItemStack poor = astrolabe(full.attuneCost() - 1);
		helper.assertTrue(!AstrolabeItem.attune(starwatcher, level, poor, NIGHT_ZERO, 100L, ONLY_MOON).equals(InteractionResult.SUCCESS),
				"An astrolabe holding too little does not attune");
		long game = level.getGameTime();
		helper.assertTrue(AstrolabeItem.attune(starwatcher, level, held, NIGHT_ZERO, game, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == 10 - full.attuneCost(), "A Starwatcher attunes, spending " + full.attuneCost());
		Attunement attunement = starwatcher.getAttached(Sky.ATTUNEMENT);
		long lasts = Math.min(full.to() - Calendar.tickOfDay(NIGHT_ZERO), Calendar.MAX_ATTUNEMENT_TICKS);
		helper.assertTrue(attunement != null && attunement.pattern().equals(FULL_MOON) && !attunement.recalled() && attunement.until() == game + lasts,
				"Attuned until the moon sets: " + attunement);
		helper.assertTrue(Sky.pulse(starwatcher, attunement, NIGHT_ZERO + 100, game + 100) && starwatcher.hasEffect(MobEffects.NIGHT_VISION),
				"While the moon is up the pulse gives night vision");
		starwatcher.removeAllEffects();
		helper.assertTrue(!Sky.pulse(starwatcher, attunement, DAY + 6000, game + 200) && !starwatcher.hasEffect(MobEffects.NIGHT_VISION),
				"Once it has set, nothing");
		helper.assertTrue(!Sky.pulse(starwatcher, attunement, NIGHT_ZERO + 200, attunement.until()) && starwatcher.getAttached(Sky.ATTUNEMENT) == null,
				"At its end the attunement is gone");
		AstrolabeItem.setCharge(held, 0);
		helper.assertTrue(!held.has(Sky.RESONANT), "An empty astrolabe is not resonant");
		helper.succeed();
	}

	/**
	 * A master recalls the pattern they last observed when nothing is up: dearer, shorter, and once per occurrence; the
	 * next full moon allows another. A practitioner cannot recall, and nobody recalls what they never observed.
	 */
	@GameTest(maxTicks = 20)
	public void aMasterRecallsOncePerOccurrence(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		long noon = DAY + 6000;
		helper.assertTrue(Sky.up(noon).isEmpty(), "Nothing is up at noon on day 1");
		ServerPlayer master = player(helper, ResearchState.MASTERED);
		ItemStack held = astrolabe(AstrolabeItem.CAPACITY);
		long game = 3_000_000L;
		helper.assertTrue(!AstrolabeItem.attune(master, level, held, noon, game, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY, "Nothing observed, nothing to recall");
		AstralClaims.of(level.getServer()).observe(master.getUUID(), FULL_MOON, game);
		helper.assertTrue(AstrolabeItem.attune(master, level, held, noon, game + 10, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost(), "A master recalls the full moon for " + full.recallCost());
		Attunement recalled = master.getAttached(Sky.ATTUNEMENT);
		helper.assertTrue(recalled != null && recalled.recalled() && recalled.until() == game + 10 + Calendar.RECALL_TICKS, "A recall is shorter: " + recalled);
		helper.assertTrue(Sky.pulse(master, recalled, noon, game + 20) && master.hasEffect(MobEffects.NIGHT_VISION), "A recall works whatever the sky");
		helper.assertTrue(!AstrolabeItem.attune(master, level, held, noon + 1000, game + 50_000, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost(), "Once per occurrence");
		helper.assertTrue(AstrolabeItem.attune(master, level, held, noon + 8 * DAY, game + 10 + Calendar.minGap(full), ONLY_MOON).equals(InteractionResult.SUCCESS),
				"After the next full moon, another");
		ServerPlayer practitioner = player(helper, ResearchState.UNDERSTOOD);
		AstralClaims.of(level.getServer()).observe(practitioner.getUUID(), FULL_MOON, game);
		helper.assertTrue(!AstrolabeItem.attune(practitioner, level, astrolabe(AstrolabeItem.CAPACITY), noon, game, ONLY_MOON).equals(InteractionResult.SUCCESS),
				"Only a master recalls");
		helper.succeed();
	}

	// ---------------------------------------------------------------- saving

	/** Claims, observations and an observatory's store, keeper and status survive a save. */
	@GameTest(maxTicks = 20)
	public void theSkyKeepsWhatItOwesThroughASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer starwatcher = player(helper, ResearchState.UNDERSTOOD);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(2, 1, 2), starwatcher);
		observatory.gather(level, NIGHT_ZERO, 4_000_000L, ONLY_MOON);
		AstralClaims claims = AstralClaims.of(level.getServer());
		claims.observe(starwatcher.getUUID(), FULL_MOON, 4_000_000L);
		Tag saved = AstralClaims.CODEC.encodeStart(NbtOps.INSTANCE, claims).getOrThrow();
		AstralClaims loaded = AstralClaims.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
		helper.assertTrue(loaded.ledger(starwatcher.getUUID()).equals(claims.ledger(starwatcher.getUUID()))
				&& loaded.ledger(starwatcher.getUUID()).claimed(FULL_MOON), "The keeper's claims are saved exactly");
		helper.assertTrue(loaded.observed(starwatcher.getUUID()).equals(claims.observed(starwatcher.getUUID())), "Observations are saved exactly");
		helper.assertTrue(loaded.claim(starwatcher.getUUID(), FULL_MOON, 0, 4_000_100L, 96_000L) != AstralLedger.Outcome.GRANTED,
				"After a restart the same night still does not pay");
		CompoundTag tag = observatory.saveWithoutMetadata(level.registryAccess());
		ObservatoryBlockEntity copy = new ObservatoryBlockEntity(observatory.getBlockPos(), observatory.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(copy.resonance() == observatory.resonance() && starwatcher.getUUID().equals(copy.keeper())
				&& copy.status().equals(observatory.status()), "The observatory keeps its resonance, keeper and status");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-03: the sky step

	/**
	 * Concordance Chunk D, D-03: the sky step (the invitation's precondition) is the server's record of an observation, not a
	 * window, and asks no research. A novice has none; looking when nothing is up records nothing; once the full moon is
	 * observed, the step holds at noon with nothing up, and through a save of the claims.
	 */
	@GameTest(maxTicks = 20)
	public void aRecordedObservationCountsWithoutAWindow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer novice = player(helper, ResearchState.NONE);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(1, 1, 1), novice);
		long noon = DAY + 6000;
		helper.assertTrue(!StolasRegard.skySeen(novice), "No observation, no sky step");
		helper.assertTrue(observatory.observe(novice, level, noon, 5_000_000L, ONLY_MOON) == 0 && !StolasRegard.skySeen(novice),
				"Nothing up: nothing recorded");
		helper.assertTrue(observatory.observe(novice, level, NIGHT_ZERO, 5_000_000L, ONLY_MOON) == 1
				&& StolasRegard.observed(novice).containsKey(FULL_MOON), "A novice observes the full moon: no research asked");
		helper.assertTrue(Sky.up(noon).isEmpty() && StolasRegard.skySeen(novice), "Read at noon, with nothing up, it still counts");
		AstralClaims claims = AstralClaims.of(level.getServer());
		AstralClaims loaded = AstralClaims.CODEC.parse(NbtOps.INSTANCE, AstralClaims.CODEC.encodeStart(NbtOps.INSTANCE, claims).getOrThrow()).getOrThrow();
		helper.assertTrue(!loaded.observed(novice.getUUID()).isEmpty(), "And through a save of the claims");
		helper.succeed();
	}

	/**
	 * Concordance Chunk D, D-03 (10:53): turning the clock gives nothing new. An observation and a gathering made, the clock
	 * turned back a week and the moon watched again from a second observatory adds no pattern, no research evidence and no
	 * resonance: the ledger holds the occurrence once.
	 */
	@GameTest(maxTicks = 20)
	public void aTurnedClockMakesNoNewClaim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer starwatcher = player(helper, ResearchState.UNDERSTOOD);
		ObservatoryBlockEntity first = observatory(helper, new BlockPos(1, 1, 1), starwatcher);
		ObservatoryBlockEntity second = observatory(helper, new BlockPos(4, 1, 1), starwatcher);
		long game = 6_000_000L;
		helper.assertTrue(first.observe(starwatcher, level, NIGHT_ZERO, game, ONLY_MOON) == 1
				&& first.gather(level, NIGHT_ZERO, game, ONLY_MOON).equals("gathering"), "Observed and gathered once");
		Knowledge knowledge = ConcordanceProgress.knowledge(starwatcher);
		long resonance = first.resonance();
		helper.assertTrue(second.observe(starwatcher, level, NIGHT_ZERO - 8 * DAY, game + 100, ONLY_MOON) == 1,
				"The clock turned back a week: the moon is up again");
		helper.assertValueEqual(StolasRegard.observed(starwatcher).keySet(), Set.of(FULL_MOON), "No new pattern");
		helper.assertTrue(ConcordanceProgress.knowledge(starwatcher).equals(knowledge), "No new research evidence");
		helper.assertTrue(second.gather(level, NIGHT_ZERO - 8 * DAY, game + 100, ONLY_MOON).equals("gathered") && second.resonance() == 0
				&& first.resonance() == resonance, "No second claim of the occurrence");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-05: Star-reckoning

	private static Pacts.Reckoning reckon(ServerLevel level, ServerPlayer player, PactPerchBlockEntity perch, String pattern, long time, long gameTime) {
		return StarReckoning.reckon(level, player, perch.holder(), pattern, time, gameTime);
	}

	/** D-05 (D-S1, E3): the pact's holder, no master, recalls at the perch a pattern they observed, at its recall cost, for 2,400 ticks, once an occurrence; the game day's quota stops a second; the next occurrence pays another. */
	@GameTest(maxTicks = 20)
	public void stolasRecallsAnObservedPatternOncePerOccurrence(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ConcordanceWorkerGameTests.regarded(helper);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, holder, new BlockPos(2, 1, 2), g0);
		ConcordanceWorkerGameTests.observed(level, holder, FULL_MOON, LANTERN_STAR);
		ItemStack held = ConcordanceWorkerGameTests.holding(holder, AstrolabeItem.CAPACITY);
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		long noon = DAY + 6000;
		helper.assertTrue(!Sky.state(holder).atLeast(ResearchState.MASTERED) && Sky.up(noon).isEmpty(), "No master, and nothing up at noon on day 1");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, noon, g0), Pacts.Reckoning.RECALLED, "The pact's holder recalls the full moon they observed");
		Attunement recalled = holder.getAttached(Sky.ATTUNEMENT);
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost() && recalled != null && recalled.recalled()
				&& FULL_MOON.equals(recalled.pattern()) && recalled.until() == g0 + Calendar.RECALL_TICKS, "at its recall cost, for 2,400 ticks: " + recalled);
		helper.assertTrue(Sky.pulse(holder, recalled, noon, g0 + 20) && holder.hasEffect(MobEffects.NIGHT_VISION), "its effect is theirs whatever the sky");
		helper.assertTrue(ConcordanceWorkerGameTests.done(level, perch, g0) == 1 && new AstralLedger.Claim(0L, g0).equals(
				AstralClaims.of(level.getServer()).ledger(holder.getUUID()).claims().get(AstrolabeItem.recallKey(full))),
				"the day's one reckoning, claimed under recall:<pattern> for occurrence 0");
		helper.assertValueEqual(reckon(level, holder, perch, LANTERN_STAR, noon + 1000, g0 + 1000), Pacts.Reckoning.QUOTA_SPENT, "A second the same game day is refused");
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost() && !ConcordanceWorkerGameTests.recalledEver(level, holder, LANTERN_STAR),
				"and spends and claims nothing");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, noon + DAY, g0 + DAY), Pacts.Reckoning.RECALLED_ALREADY, "The next game day, the same occurrence is recalled already");
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost() && ConcordanceWorkerGameTests.done(level, perch, g0 + DAY) == 0,
				"nothing spent, and the refusal uses no quota");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, 8 * DAY + 6000, g0 + Calendar.minGap(full)), Pacts.Reckoning.RECALLED,
				"The next full moon's occurrence pays another");
		helper.assertValueEqual(AstrolabeItem.charge(held), AstrolabeItem.CAPACITY - 2 * full.recallCost(), "at the same cost");
		helper.succeed();
	}

	/**
	 * D-05 (D-S1, E3): a master's own recall and Star-reckoning share one claim on one key: whichever asks first, the other is
	 * refused for that occurrence and spends nothing (the astrolabe full each time, so only the shared claim can refuse it).
	 */
	@GameTest(maxTicks = 20)
	public void aMastersRecallAndStolassShareOneClaim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer master = ConcordanceWorkerGameTests.regarded(helper);
		ConcordanceProgress.grant(master, Sky.RESEARCH, ResearchState.MASTERED);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, master, new BlockPos(2, 1, 2), g0);
		AstralClaims.of(level.getServer()).observe(master.getUUID(), FULL_MOON, g0);
		ItemStack held = ConcordanceWorkerGameTests.holding(master, AstrolabeItem.CAPACITY);
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		long noon = DAY + 6000;
		helper.assertTrue(AstrolabeItem.attune(master, level, held, noon, g0, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - full.recallCost(), "A master recalls by their own route, nothing being up");
		AstrolabeItem.setCharge(held, AstrolabeItem.CAPACITY);
		helper.assertValueEqual(reckon(level, master, perch, FULL_MOON, noon + 1000, g0 + DAY), Pacts.Reckoning.RECALLED_ALREADY,
				"Star-reckoning the same occurrence, a full astrolabe in hand, is recalled already");
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY && ConcordanceWorkerGameTests.done(level, perch, g0 + DAY) == 0,
				"and spends nothing");
		long later = g0 + Calendar.minGap(full);
		helper.assertValueEqual(reckon(level, master, perch, FULL_MOON, 8 * DAY + 6000, later), Pacts.Reckoning.RECALLED, "Star-reckoning the next occurrence first");
		AstrolabeItem.setCharge(held, AstrolabeItem.CAPACITY);
		helper.assertTrue(!AstrolabeItem.attune(master, level, held, 8 * DAY + 7000, later + 100, ONLY_MOON).equals(InteractionResult.SUCCESS)
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY, "the master's own recall of it, a full astrolabe in hand, is refused, spending nothing");
		helper.assertTrue(new AstralLedger.Claim(1L, later).equals(AstralClaims.of(level.getServer()).ledger(master.getUUID()).claims().get(AstrolabeItem.recallKey(full))),
				"One claim, on one key, for both routes");
		helper.succeed();
	}

	/** D-05 (E3): the clock moved on the same game day does not reset the quota; turned back, forward or far forward the next game day, it pays no second recall; only the world's own time does. */
	@GameTest(maxTicks = 20)
	public void aChangedClockGivesNoSecondRecall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ConcordanceWorkerGameTests.regarded(helper);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, holder, new BlockPos(2, 1, 2), g0);
		ConcordanceWorkerGameTests.observed(level, holder, FULL_MOON);
		ItemStack held = ConcordanceWorkerGameTests.holding(holder, AstrolabeItem.CAPACITY);
		Pattern full = Sky.catalog().pattern(FULL_MOON);
		long noon = DAY + 6000;
		int once = AstrolabeItem.CAPACITY - full.recallCost();
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, noon, g0), Pacts.Reckoning.RECALLED, "Recalled once");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, 16 * DAY + 6000, g0 + 100), Pacts.Reckoning.QUOTA_SPENT,
				"The clock moved on two weeks the same game day: the quota is the game day's");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, noon - 8 * DAY, g0 + DAY), Pacts.Reckoning.RECALLED_ALREADY,
				"Turned back a week, the next game day: that occurrence has paid");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, 8 * DAY + 6000, g0 + DAY + 1), Pacts.Reckoning.RECALLED_ALREADY,
				"Turned forward to the next full moon: too soon by the world's own time");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, 80 * DAY + 6000, g0 + DAY + 2), Pacts.Reckoning.RECALLED_ALREADY, "Nor far forward");
		helper.assertValueEqual(AstrolabeItem.charge(held), once, "and none of it spent anything");
		helper.assertValueEqual(reckon(level, holder, perch, FULL_MOON, 8 * DAY + 6000, g0 + Calendar.minGap(full)), Pacts.Reckoning.RECALLED,
				"After enough of the world's own time, the next occurrence pays");
		helper.succeed();
	}

	/** D-05 (the balance pinned): outside its event a holder who once observed the Harvest Moon recalls it every game day for 12 resonance until their astrolabe runs short; the Winter Crown every other day for 9. */
	@GameTest(maxTicks = 20)
	public void theHarvestMoonRecallsOutsideItsEvent(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ConcordanceWorkerGameTests.regarded(helper);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 200);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, holder, new BlockPos(2, 1, 2), g0);
		ConcordanceWorkerGameTests.observed(level, holder, HARVEST_MOON);
		ItemStack held = ConcordanceWorkerGameTests.holding(holder, AstrolabeItem.CAPACITY);
		Pattern harvest = Sky.catalog().pattern(HARVEST_MOON);
		for (long day = 1; day <= 3; day++) {
			helper.assertTrue(!Sky.inSeason(harvest, day * DAY + 6000) && !Sky.up(day * DAY + 6000).contains(harvest), "Outside its event at noon on day " + day);
		}
		helper.assertValueEqual(reckon(level, holder, perch, HARVEST_MOON, DAY + 6000, g0), Pacts.Reckoning.RECALLED, "Day 1: recalled");
		Attunement recalled = holder.getAttached(Sky.ATTUNEMENT);
		helper.assertTrue(Sky.pulse(holder, recalled, DAY + 6000, g0 + 20) && holder.hasEffect(MobEffects.HERO_OF_THE_VILLAGE),
				"Hero of the Village, outside the Harvest Moon");
		helper.assertValueEqual(reckon(level, holder, perch, HARVEST_MOON, 2 * DAY + 6000, g0 + DAY), Pacts.Reckoning.RECALLED,
				"Day 2: recalled again, a new occurrence each day");
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - 2 * harvest.recallCost()
				&& reckon(level, holder, perch, HARVEST_MOON, 3 * DAY + 6000, g0 + 2 * DAY) == Pacts.Reckoning.TOO_LITTLE
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - 2 * harvest.recallCost(),
				"Day 3: refused, this astrolabe holding too little (the quota is the only hard limit: another astrolabe would pay)");
		AstrolabeItem.setCharge(held, AstrolabeItem.CAPACITY);
		ConcordanceWorkerGameTests.observed(level, holder, WINTER_CROWN);
		Pattern crown = Sky.catalog().pattern(WINTER_CROWN);
		helper.assertValueEqual(reckon(level, holder, perch, WINTER_CROWN, 3 * DAY + 6000, g0 + 2 * DAY), Pacts.Reckoning.RECALLED,
				"The Winter Crown on day 3 (the refused recall used no quota)");
		helper.assertValueEqual(reckon(level, holder, perch, WINTER_CROWN, 4 * DAY + 6000, g0 + 3 * DAY), Pacts.Reckoning.RECALLED_ALREADY,
				"day 4 is the same occurrence");
		helper.assertValueEqual(reckon(level, holder, perch, WINTER_CROWN, 5 * DAY + 6000, g0 + 4 * DAY), Pacts.Reckoning.RECALLED, "day 5 another: every other day");
		helper.assertValueEqual(AstrolabeItem.charge(held), AstrolabeItem.CAPACITY - 2 * crown.recallCost(), "at 9 resonance each");
		helper.succeed();
	}

	/** D-05 (D-S1, E1): only a pattern the holder observed themself is recalled: not one unobserved, unknown or observed by another; once observed, it is. */
	@GameTest(maxTicks = 20)
	public void anUnobservedPatternIsRefused(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ConcordanceWorkerGameTests.regarded(helper);
		ServerPlayer other = ConcordanceWorkerGameTests.regarded(helper);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, holder, new BlockPos(2, 1, 2), g0);
		ConcordanceWorkerGameTests.observed(level, holder, FULL_MOON);
		ConcordanceWorkerGameTests.observed(level, other, LANTERN_STAR);
		ItemStack held = ConcordanceWorkerGameTests.holding(holder, AstrolabeItem.CAPACITY);
		long noon = DAY + 6000;
		helper.assertTrue(reckon(level, holder, perch, ECHO_COMET, noon, g0) == Pacts.Reckoning.NOT_OBSERVED
				&& reckon(level, holder, perch, LANTERN_STAR, noon, g0) == Pacts.Reckoning.NOT_OBSERVED
				&& reckon(level, holder, perch, "jugcraft:no_such_pattern", noon, g0) == Pacts.Reckoning.NOT_OBSERVED,
				"Never observed, observed only by another player, or unknown: refused");
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY && !ConcordanceWorkerGameTests.recalledEver(level, holder, ECHO_COMET)
				&& ConcordanceWorkerGameTests.done(level, perch, g0) == 0, "spending, claiming and counting nothing");
		helper.assertValueEqual(StarReckoning.choices(level.getServer(), holder.getUUID()).stream().map(Pattern::id).toList(), List.of(FULL_MOON),
				"Stolas offers what they observed");
		AstralClaims.of(level.getServer()).observe(holder.getUUID(), ECHO_COMET, g0);
		helper.assertTrue(reckon(level, holder, perch, ECHO_COMET, noon, g0) == Pacts.Reckoning.RECALLED
				&& AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY - Sky.catalog().pattern(ECHO_COMET).recallCost(),
				"Once observed, the 19-day Echo Comet is recalled without waiting for it");
		helper.succeed();
	}
}
