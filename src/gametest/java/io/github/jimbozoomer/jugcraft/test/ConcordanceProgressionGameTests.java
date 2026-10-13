package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Standing;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionCatalog;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionGraph;
import io.github.jimbozoomer.jugcraft.concordance.progression.Route;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.progression.Stages;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 24, the progression graph (docs/features/arcane-concordance-progression.md), on a real server: the graph
 * the server builds from its own rules is whole (every stage reachable by one player alone, nothing circular, nothing
 * needing a later stage, two routes through each middle stage); a fresh player climbs from nothing to Architect, the
 * first stages by ordinary evidence through the research engine; a stage once reached is kept; and each route says what
 * it still needs.
 */
public class ConcordanceProgressionGameTests {
	private static ServerPlayer fresh(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
				"rank:starbound", ProgressionGraph.PROJECT, ProgressionGraph.NOTES)) {
			helper.assertTrue(graph.nodes().containsKey(node), "the graph lacks " + node);
		}
		Set<String> alone = graph.reachable(true);
		Set<String> together = graph.reachable(false);
		for (StageDefinition stage : catalog.ordered()) {
			helper.assertTrue(alone.contains("stage:" + stage.id()), "one player alone cannot reach " + stage.id());
		}
		helper.assertTrue(together.containsAll(alone), "other players only add to what one can reach");
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
}
