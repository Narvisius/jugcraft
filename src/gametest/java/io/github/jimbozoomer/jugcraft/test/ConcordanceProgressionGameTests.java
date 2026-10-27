package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Standing;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.progression.PracticeGate;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionCatalog;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionGraph;
import io.github.jimbozoomer.jugcraft.concordance.progression.Route;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.progression.Stages;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.RimeSurvey;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Roadmap step 24, the progression graph (docs/features/arcane-concordance-progression.md), on a real server: the graph
 * the server builds from its own rules is whole (every stage reachable by one player alone, nothing circular, nothing
 * needing a later stage, two routes through each middle stage); a fresh player climbs from nothing to Architect, the
 * first stages by ordinary evidence through the research engine; a stage once reached is kept; and each route says what
 * it still needs. Since Concordance Chunk B, unit 3: winter and stone ask only for First Light, one examination of stone
 * serves Assay and Stratawrighting, and a Rimekeeper understands Rimekeeping in the Glacier Hall with no bench, though
 * its mastery, which wants a crucible filled by hand, asks for the Alembic Arts.
 */
public class ConcordanceProgressionGameTests {
	private static ServerPlayer fresh(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static String stage(ServerPlayer player) {
		StageDefinition stage = StageProgress.stage(player);
		return stage == null ? "none" : stage.id();
	}

	private static boolean done(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Jugcraft.id(advancement));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static StageDefinition definition(String id) {
		StageDefinition stage = StageProgress.catalog().stages().get(id);
		if (stage == null) {
			throw new IllegalStateException("no stage " + id);
		}
		return stage;
	}

	private static Route route(String stage, String id) {
		return definition(stage).routes().stream().filter(route -> route.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalStateException("no route " + stage + "/" + id));
	}

	/** The first effect of an invocation's composed form on the Initiate's Wand: what its cast applies. */
	private static EffectSpec authored(String id) {
		return ConcordanceData.rules().authored(id, "jugcraft:initiate_wand").plan().root().steps().getFirst().effect();
	}

	private static ConcordanceEffects.Context context(ServerLevel level, ServerPlayer actor) {
		Cause cause = Cause.of(actor.getUUID(), Cause.Origin.INVOCATION, "test:progression", ConcordanceEffects.nextSerial());
		return new ConcordanceEffects.Context(level, cause, actor, new Ledger(new Ledger.Limits(16, 256, 0)), "0/0/0", actor.position());
	}

	private static ConcordanceEffects.Result apply(ServerLevel level, ServerPlayer actor, EffectSpec spec, BlockPos at) {
		return ConcordanceEffects.apply(context(level, actor), spec, at);
	}

	private static ConcordanceEffects.Result apply(ServerLevel level, ServerPlayer actor, EffectSpec spec, LivingEntity target) {
		return ConcordanceEffects.apply(context(level, actor), spec, target);
	}

	/** A bottle of water poured into {@code crucible} by hand. */
	private static InteractionResult fill(CrucibleBlockEntity crucible, ServerPlayer player) {
		RateGate.forget(player.getUUID());
		ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
		player.setItemInHand(InteractionHand.MAIN_HAND, water);
		return crucible.use(player, player.level(), water, InteractionHand.MAIN_HAND);
	}

	/**
	 * The graph the server built from its rules is whole: research, invocations, rituals, practices and ranks are all
	 * in it, one player alone reaches every stage, other players' notes and shared rituals only add to that, and the
	 * graph finds nothing wrong.
	 */
	@GameTest(maxTicks = 20)
	public void theLiveGraphIsWhole(GameTestHelper helper) {
		ProgressionCatalog catalog = ConcordanceData.rules().progression();
		ProgressionGraph graph = catalog.graph();
		helper.assertTrue(graph != null, "the rules built no progression graph");
		helper.assertTrue(graph.problems().isEmpty(), "the progression graph has problems: " + graph.problems());
		helper.assertValueEqual(catalog.ordered().stream().map(StageDefinition::id).toList(),
				List.of("initiate", "practitioner", "adept", "master", "architect"), "the five stages, in order");
		for (String node : List.of("research:jugcraft:first_light@encountered", "research:jugcraft:dreamwalking@mastered",
				"invocation:jugcraft:kindle", "ritual:jugcraft:adept_attunement", "ritual:jugcraft:lumen_vigil", "practice:jugcraft:ritual",
				"rank:starbound", ProgressionGraph.PROJECT, ProgressionGraph.NOTES,
				// Concordance Chunk B, unit 3: winter and stone.
				"research:jugcraft:rimekeeping@mastered", "research:jugcraft:stratawrighting@mastered", "practice:jugcraft:winter_survey",
				"practice:jugcraft:rimework", "practice:jugcraft:anchoring", "invocation:jugcraft:rimebind", "invocation:jugcraft:preserve",
				"invocation:jugcraft:brace",
				// Concordance Chunk C, C-04: Root and Rivet.
				"ritual:jugcraft:root_and_rivet")) {
			helper.assertTrue(graph.nodes().containsKey(node), "the graph lacks " + node);
		}
		Set<String> alone = graph.reachable(true);
		Set<String> together = graph.reachable(false);
		for (StageDefinition stage : catalog.ordered()) {
			helper.assertTrue(alone.contains("stage:" + stage.id()), "one player alone cannot reach " + stage.id());
		}
		helper.assertTrue(together.containsAll(alone), "other players only add to what one can reach");
		helper.assertTrue(alone.contains("ritual:jugcraft:root_and_rivet"), "one player alone can work Root and Rivet (Concordance Chunk C, C-04)");
		helper.assertTrue(!alone.contains("ritual:jugcraft:lumen_vigil") && together.contains("ritual:jugcraft:lumen_vigil"),
				"a ritual for two needs a second player");
		helper.assertTrue(!alone.contains(ProgressionGraph.NOTES) && together.contains(ProgressionGraph.NOTES),
				"notes come from another player");
		helper.succeed();
	}

	/**
	 * A fresh player climbs every stage: Initiate and Practitioner by ordinary evidence the research engine records
	 * (examining, then studying at a bench), the stage following by itself; then Adept by mastering two entries, Master by
	 * mastering five in three traditions, Architect as a Starbound member who helped finish a project with six entries
	 * mastered. Each stage's advancement is awarded, and the ones before it.
	 */
	@GameTest(maxTicks = 20)
	public void aFreshPlayerClimbsEveryStage(GameTestHelper helper) {
		ServerPlayer player = fresh(helper);
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "none", "a fresh player has no stage");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glowstone_dust", 15));
		helper.assertValueEqual(stage(player), "initiate", "examining a luminous specimen makes an Initiate");
		helper.assertTrue(done(helper, player, "concordance_stage_initiate"), "the Initiate's advancement");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glow_berries", 0));
		ConcordanceProgress.record(player, new Evidence.Studied("minecraft:glowstone_dust", "jugcraft:lampwright_bench"));
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state("jugcraft:first_light"), ResearchState.UNDERSTOOD,
				"examining in the dark and studying at a bench understands First Light");
		helper.assertValueEqual(stage(player), "practitioner", "understanding First Light makes a Practitioner");
		ConcordanceProgress.grant(player, "jugcraft:circle_lore", ResearchState.MASTERED);
		ConcordanceProgress.grant(player, "jugcraft:alembic_arts", ResearchState.MASTERED);
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "adept", "two entries mastered make an Adept");
		for (String research : List.of("jugcraft:first_light", "jugcraft:verdant_husbandry", "jugcraft:celestial_attunement")) {
			ConcordanceProgress.grant(player, research, ResearchState.MASTERED);
		}
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "master", "five entries mastered in three traditions make a Master");
		ConcordanceProgress.grant(player, "jugcraft:crimson_rites", ResearchState.MASTERED);
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "master", "six mastered entries alone are not enough for an Architect");
		long now = helper.getLevel().getGameTime();
		player.setAttached(Starbound.STANDING, new Standing(true, now, now, 300,
				Map.of("lampwrights", 60, "circlewrights", 50, "alembists", 50, "greenwardens", 50, "starwatchers", 50, "crimson_vigil", 40),
				Set.of("research", "commission", "project"), Map.of("project:jugcraft:starward_chart:complete", 1), Map.of()));
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "architect", "a Starbound member who helped finish a project becomes an Architect");
		for (String id : List.of("initiate", "practitioner", "adept", "master", "architect")) {
			helper.assertTrue(done(helper, player, "concordance_stage_" + id), "the advancement for " + id);
		}
		helper.succeed();
	}

	/** A stage once reached is kept: forgetting research afterwards (an operator's reset) does not take it away. */
	@GameTest(maxTicks = 20)
	public void aStageIsNeverLost(GameTestHelper helper) {
		ServerPlayer player = fresh(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "practitioner", "First Light understood makes a Practitioner");
		ConcordanceProgress.reset(player);
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "practitioner", "the stage is kept when the research is forgotten");
		helper.succeed();
	}

	/** Every route to the next stage says exactly what it still needs, and nothing once it holds. */
	@GameTest(maxTicks = 20)
	public void eachRouteSaysWhatItStillNeeds(GameTestHelper helper) {
		ServerPlayer player = fresh(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		Stages.Situation situation = StageProgress.situation(player);
		helper.assertValueEqual(Stages.missing(route("adept", "specialist"), situation), List.of("mastered 2 (0)"), "the specialist's route");
		helper.assertValueEqual(Stages.missing(route("adept", "generalist"), situation), List.of("understood 5 (1)", "traditions 5 (1)"),
				"the generalist's route");
		helper.assertValueEqual(Stages.missing(route("adept", "attuned"), situation),
				List.of("research jugcraft:circle_lore@understood", "research jugcraft:first_light@mastered"), "the attuned route");
		helper.assertValueEqual(Stages.missing(route("architect", "shared_wonder"), situation),
				List.of("mastered 6 (0)", "rank starbound", "projects 1 (0)"), "the Architect's route");
		ConcordanceProgress.grant(player, "jugcraft:circle_lore", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.MASTERED);
		helper.assertTrue(Stages.missing(route("adept", "attuned"), StageProgress.situation(player)).isEmpty(), "the attuned route holds");
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "adept", "a route that holds reaches its stage");
		helper.succeed();
	}

	/** A milestone a feature records is kept once, however often it is recorded. */
	@GameTest(maxTicks = 20)
	public void milestonesAreRecordedOnce(GameTestHelper helper) {
		ServerPlayer player = fresh(helper);
		StageProgress.milestone(player, "jugcraft:test_milestone");
		StageProgress.milestone(player, "jugcraft:test_milestone");
		helper.assertValueEqual(player.getAttached(StageProgress.MILESTONES), List.of("jugcraft:test_milestone"), "one milestone, once");
		helper.assertTrue(StageProgress.situation(player).milestones().contains("jugcraft:test_milestone"), "the stage rules see it");
		helper.succeed();
	}

	/**
	 * Concordance Chunk B, unit 3: winter and stone ask only for First Light. The winter survey needs First Light
	 * understood and nothing else; Rimekeeping's and Stratawrighting's practices need their own entry understood;
	 * Stratawrighting begins after First Light alone; and one player alone can master both.
	 */
	@GameTest(maxTicks = 20)
	public void winterAndStoneAskOnlyForFirstLight(GameTestHelper helper) {
		ProgressionGraph graph = ConcordanceData.rules().progression().graph();
		helper.assertTrue(graph != null, "the rules built no progression graph");
		String firstLight = ProgressionGraph.research("jugcraft:first_light", ResearchState.UNDERSTOOD);
		Map<String, List<String>> needs = Map.of(
				"practice:jugcraft:winter_survey", List.of(firstLight),
				"practice:jugcraft:rimework", List.of(ProgressionGraph.research("jugcraft:rimekeeping", ResearchState.UNDERSTOOD)),
				"practice:jugcraft:anchoring", List.of(ProgressionGraph.research("jugcraft:stratawrighting", ResearchState.UNDERSTOOD)),
				ProgressionGraph.research("jugcraft:stratawrighting", ResearchState.ENCOUNTERED), List.of(firstLight));
		for (Map.Entry<String, List<String>> need : needs.entrySet()) {
			ProgressionGraph.Node node = graph.nodes().get(need.getKey());
			helper.assertTrue(node != null && node.all().equals(need.getValue()), need.getKey() + " needs only " + need.getValue() + ": "
					+ (node == null ? "no node" : node.all()));
		}
		Set<String> alone = graph.reachable(true);
		for (String research : List.of("jugcraft:rimekeeping", "jugcraft:stratawrighting")) {
			helper.assertTrue(alone.contains(ProgressionGraph.research(research, ResearchState.MASTERED)), "one player alone masters " + research);
		}
		helper.succeed();
	}

	/**
	 * Concordance Chunk B, unit 3: one examination of granite begins both Assay and Stratawrighting, and nothing else;
	 * raw iron then observes Assay, which counts it, while Stratawrighting, which does not, is still only begun.
	 */
	@GameTest(maxTicks = 20)
	public void oneExaminationServesAssayAndStratawrighting(GameTestHelper helper) {
		ServerPlayer player = fresh(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ResearchEngine.Result granite = ConcordanceProgress.record(player, new Evidence.Examined("minecraft:granite", 15));
		Set<String> begun = granite.transitions().stream().map(ResearchEngine.Transition::research).collect(Collectors.toSet());
		helper.assertValueEqual(begun, Set.of("jugcraft:assay", "jugcraft:stratawrighting"), "granite begins Assay and Stratawrighting");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:raw_iron", 15));
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state("jugcraft:assay"), ResearchState.OBSERVED, "raw iron observes Assay");
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state("jugcraft:stratawrighting"), ResearchState.ENCOUNTERED,
				"and leaves Stratawrighting begun");
		helper.succeed();
	}

	/**
	 * Concordance Chunk B, unit 3: a Rimekeeper on the way to Adept. With First Light mastered, snow and fire observe
	 * Rimekeeping and arriving in the Glacier Hall understands it, with no study of it. Its mastery still asks for the
	 * bench: Preserve counts only a crucible holding a mixture, and a crucible takes water by hand only from someone who
	 * understands the Alembic Arts, studied at a Lampwright's Bench (or read in someone's notes). Then a cold crucible and
	 * a heated one preserved through the effect boundary and a zombie bound master Rimekeeping, and two entries mastered
	 * make an Adept.
	 */
	@GameTest(maxTicks = 20)
	public void aRimekeeperReachesAdeptByTheGlacierHall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		ServerPlayer player = fresh(helper);
		BlockPos standAt = helper.absolutePos(new BlockPos(1, 2, 1));
		player.snapTo(standAt.getX() + 0.5, standAt.getY(), standAt.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.MASTERED);
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:snowball", 15));
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:coal", 15));
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state(Rime.RESEARCH), ResearchState.OBSERVED, "snow and fire observe Rimekeeping");
		helper.assertTrue(RimeSurvey.arrived(player, Lair.GLACIER_HALL.dimension), "arriving in the Glacier Hall is the field study");
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state(Rime.RESEARCH), ResearchState.UNDERSTOOD, "which understands it");
		helper.assertTrue(ConcordanceProgress.knowledge(player).progress(Rime.RESEARCH).evidence().keySet().stream().noneMatch(key -> key.startsWith("study:")),
				"with no study of Rimekeeping");

		BlockPos at = new BlockPos(1, 2, 3);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		helper.assertTrue(fill(crucible, player) == InteractionResult.FAIL && crucible.parts() == 0,
				"without the Alembic Arts no water goes into a crucible by hand");
		EffectSpec preserve = authored("jugcraft:preserve");
		helper.assertTrue(apply(level, player, preserve, helper.absolutePos(at)).applied()
				&& !ConcordanceProgress.knowledge(player).progress(Rime.RESEARCH).evidence().keySet().stream().anyMatch(key -> key.startsWith("practice:" + Rime.ACTIVITY)),
				"an empty crucible is held, but that is no cold work");

		for (String specimen : List.of("minecraft:sugar", "minecraft:sweet_berries", "minecraft:dried_kelp")) {
			ConcordanceProgress.record(player, new Evidence.Examined(specimen, 15));
		}
		ConcordanceProgress.record(player, new Evidence.Studied("minecraft:sugar", "jugcraft:lampwright_bench"));
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state(Alchemy.RESEARCH), ResearchState.UNDERSTOOD,
				"the Alembic Arts are understood at the bench");
		helper.assertTrue(fill(crucible, player) == InteractionResult.SUCCESS && crucible.parts() == 1 && crucible.band() == Band.COLD,
				"an alembist puts water in it: " + crucible.parts() + " parts at " + crucible.temperature());
		helper.assertTrue(apply(level, player, preserve, helper.absolutePos(at)).applied(), "a cold crucible preserved");
		crucible.setTemperature(60);
		helper.assertTrue(apply(level, player, preserve, helper.absolutePos(at)).applied(), "a warm one preserved");
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 6));
		helper.assertTrue(apply(level, player, authored("jugcraft:rimebind"), zombie).applied(), "a zombie bound");
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state(Rime.RESEARCH), ResearchState.MASTERED,
				"three kinds of cold work master it: " + ConcordanceProgress.knowledge(player).progress(Rime.RESEARCH).evidence().keySet());
		StageProgress.refresh(player);
		helper.assertValueEqual(stage(player), "adept", "two entries mastered make an Adept");
		helper.succeed();
	}

	/**
	 * Concordance Chunk D, D-03 (D-S12, critique 11): Stolas's Regard is in the live graph and one player alone reaches all of
	 * it. It begins after the Binding Arts are understood; it is observed by cultivation, understood by assay and mastered by
	 * the pact's first obligation, whose gate ships with it and needs it understood; no state takes notes; and the graph
	 * finds nothing unprovided.
	 */
	@GameTest(maxTicks = 20)
	public void stolasRegardIsReachable(GameTestHelper helper) {
		ProgressionCatalog catalog = ConcordanceData.rules().progression();
		ProgressionGraph graph = catalog.graph();
		helper.assertTrue(graph != null && graph.problems().isEmpty(), "the graph is whole: " + (graph == null ? "none" : graph.problems()));
		PracticeGate gate = catalog.practices().get(StolasRegard.ACTIVITY);
		helper.assertTrue(gate != null && gate.tradition().equals("spiritbinders")
				&& gate.requires().equals(List.of(new Definitions.Requirement(StolasRegard.RESEARCH, ResearchState.UNDERSTOOD))),
				"the obligation's gate ships with the research: " + gate);
		String understood = ProgressionGraph.research(StolasRegard.RESEARCH, ResearchState.UNDERSTOOD);
		Map<String, List<List<String>>> ways = Map.of(
				ProgressionGraph.research(StolasRegard.RESEARCH, ResearchState.ENCOUNTERED), List.of(List.of()),
				ProgressionGraph.research(StolasRegard.RESEARCH, ResearchState.OBSERVED), List.of(List.of("practice:jugcraft:cultivation")),
				understood, List.of(List.of("practice:jugcraft:assay")),
				ProgressionGraph.research(StolasRegard.RESEARCH, ResearchState.MASTERED), List.of(List.of("practice:" + StolasRegard.ACTIVITY)));
		for (Map.Entry<String, List<List<String>>> way : ways.entrySet()) {
			ProgressionGraph.Node node = graph.nodes().get(way.getKey());
			helper.assertTrue(node != null && node.any().equals(way.getValue()), way.getKey() + " is reached only by " + way.getValue() + ": " + node);
		}
		ProgressionGraph.Node begun = graph.nodes().get(ProgressionGraph.research(StolasRegard.RESEARCH, ResearchState.ENCOUNTERED));
		helper.assertTrue(begun.all().equals(List.of(ProgressionGraph.research(Workers.RESEARCH, ResearchState.UNDERSTOOD))),
				"it begins after the Binding Arts: " + begun.all());
		ProgressionGraph.Node obligation = graph.nodes().get("practice:" + StolasRegard.ACTIVITY);
		helper.assertTrue(obligation != null && obligation.all().equals(List.of(understood)), "the obligation needs it understood: " + obligation);
		Set<String> alone = graph.reachable(true);
		for (ResearchState state : List.of(ResearchState.ENCOUNTERED, ResearchState.OBSERVED, ResearchState.UNDERSTOOD, ResearchState.MASTERED)) {
			helper.assertTrue(alone.contains(ProgressionGraph.research(StolasRegard.RESEARCH, state)), "one player alone reaches " + state);
		}
		Definitions.Research regard = ConcordanceData.rules().research(StolasRegard.RESEARCH);
		helper.assertTrue(regard != null && regard.tradition().equals("spiritbinders") && regard.stage().equals("practitioner")
				&& regard.unlocks().isEmpty(), "the Spiritbinders' practitioner entry, unlocking nothing by data (D-08 checks mastery in code): " + regard);
		helper.succeed();
	}
}
