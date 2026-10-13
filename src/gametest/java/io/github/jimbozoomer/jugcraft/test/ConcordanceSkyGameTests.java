package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Attunement;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
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
 * recall pays once per occurrence; and claims and observatories keep what they hold through a save.
 */
public class ConcordanceSkyGameTests {
	private static final String FULL_MOON = "jugcraft:full_moon";
	private static final String HARVEST_MOON = "jugcraft:harvest_moon";
	private static final long DAY = Calendar.DAY;
	/** Midnight of day 0: the full moon is up (and nothing else that keeps to no season). */
	private static final long NIGHT_ZERO = 18000L;
	/** A sky in which only the full moon can be seen: the other patterns are hidden, so tests never depend on them. */
	private static final Function<Pattern, String> ONLY_MOON = pattern -> pattern.id().equals(FULL_MOON) ? null : "no_sky";

	private static ServerPlayer player(GameTestHelper helper, ResearchState state) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
}
