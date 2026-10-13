package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.progression.Route;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.progression.Stages;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireRecord;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 31, the player journey (docs/features/arcane-concordance-journey.md), on a real server: each of the three
 * routes in tools/concordance_journey.py climbs the live stage rules its own way and founds its own Spire.
 * <p>
 * A fresh player understands First Light by ordinary evidence (three luminous specimens examined in the dark). The
 * route's other research is then granted in the states the route takes; each entry's own evidence and practice are
 * exercised by its system's tests. After each part the stage is the one the route expects, by the stage route it names
 * and not another. The Spire founded is the route's own, and a Spire whose research the route never took is refused.
 * Raising a Spire takes days and is played through by {@code ConcordanceSpireGameTests}; here its milestone is recorded
 * as raising records it, and the Architect stage follows.
 */
public class ConcordanceJourneyGameTests {
	/** A route: what reaching Adept takes and by which stage route, what reaching Master takes, its Spire and the ones refused. */
	private record Journey(String name, List<String> adeptUnderstood, List<String> adeptMastered, String adept,
			List<String> masterUnderstood, List<String> masterMastered, String spire, List<String> refused) {
	}

	private static final Journey CULTIVATION = new Journey("cultivation", List.of("jugcraft:verdant_husbandry"),
			List.of("jugcraft:first_light", "jugcraft:verdant_husbandry"), "specialist",
			List.of("jugcraft:alembic_arts", "jugcraft:binding_arts", "jugcraft:circle_lore"),
			List.of("jugcraft:alembic_arts", "jugcraft:binding_arts", "jugcraft:circle_lore"), "jugcraft:verdant_spire",
			List.of("jugcraft:star_spire"));
	private static final Journey EXPLORATION = new Journey("exploration and combat",
			List.of("jugcraft:celestial_attunement", "jugcraft:crimson_rites", "jugcraft:relic_lore", "jugcraft:sympathy"), List.of(),
			"generalist", List.of("jugcraft:circle_lore"),
			List.of("jugcraft:first_light", "jugcraft:celestial_attunement", "jugcraft:crimson_rites", "jugcraft:relic_lore",
					"jugcraft:sympathy"), "jugcraft:star_spire", List.of("jugcraft:verdant_spire"));
	private static final Journey INFRASTRUCTURE = new Journey("crafting and infrastructure", List.of("jugcraft:circle_lore"),
			List.of("jugcraft:first_light"), "attuned", List.of("jugcraft:runesmithing", "jugcraft:assay", "jugcraft:binding_arts"),
			List.of("jugcraft:circle_lore", "jugcraft:runesmithing", "jugcraft:assay", "jugcraft:binding_arts"), "jugcraft:lantern_spire",
			List.of("jugcraft:verdant_spire", "jugcraft:star_spire"));

	private static String stage(ServerPlayer player) {
		StageDefinition stage = StageProgress.stage(player);
		return stage == null ? "none" : stage.id();
	}

	private static Route route(String stage, String id) {
		StageDefinition definition = StageProgress.catalog().stages().get(stage);
		if (definition == null) {
			throw new IllegalStateException("no stage " + stage);
		}
		return definition.routes().stream().filter(route -> route.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalStateException("no route " + stage + "/" + id));
	}

	private static void grant(ServerPlayer player, List<String> research, ResearchState state) {
		for (String id : research) {
			ConcordanceProgress.grant(player, id, state);
		}
		StageProgress.refresh(player);
	}

	/** Which of the stage's routes hold for {@code player} now. */
	private static List<String> holding(ServerPlayer player, String stage) {
		Stages.Situation situation = StageProgress.situation(player);
		return StageProgress.catalog().stages().get(stage).routes().stream().filter(route -> Stages.missing(route, situation).isEmpty())
				.map(Route::id).toList();
	}

	private static void climb(GameTestHelper helper, Journey journey, BlockPos at) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		RateGate.forget(player.getUUID());
		for (String specimen : List.of("minecraft:amethyst_shard", "minecraft:glow_berries", "minecraft:glow_ink_sac")) {
			ConcordanceProgress.record(player, new Evidence.Examined(specimen, 0));
		}
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state("jugcraft:first_light"), ResearchState.UNDERSTOOD,
				journey.name() + ": three luminous specimens examined in the dark understand First Light");
		helper.assertValueEqual(stage(player), "practitioner", journey.name() + ": understanding First Light makes a Practitioner");

		grant(player, journey.adeptUnderstood(), ResearchState.UNDERSTOOD);
		grant(player, journey.adeptMastered(), ResearchState.MASTERED);
		helper.assertValueEqual(stage(player), "adept", journey.name() + ": its first part makes an Adept");
		helper.assertValueEqual(holding(player, "adept"), List.of(journey.adept()), journey.name() + ": by its own route alone");
		helper.assertValueEqual(holding(player, "master"), List.of(), journey.name() + ": and no Master yet");

		grant(player, journey.masterUnderstood(), ResearchState.UNDERSTOOD);
		grant(player, journey.masterMastered(), ResearchState.MASTERED);
		helper.assertValueEqual(stage(player), "master", journey.name() + ": its second part makes a Master");
		helper.assertTrue(holding(player, "master").contains("specialist"), journey.name() + ": five entries mastered in three traditions");

		BlockPos heart = helper.absolutePos(at);
		for (String other : journey.refused()) {
			helper.assertValueEqual(ConcordSpire.found(player, level, heart, other), "research",
					journey.name() + ": a Spire whose research the route never took is refused: " + other);
		}
		helper.assertValueEqual(ConcordSpire.found(player, level, heart, journey.spire()), "",
				journey.name() + ": the route's own Spire is founded: " + journey.spire());
		SpireRecord.of(level.getServer()).remove(ConcordSpire.id(level, heart));
		helper.assertValueEqual(holding(player, "architect"), List.of(), journey.name() + ": founding alone is not raising");
		StageProgress.milestone(player, ConcordSpire.MILESTONE);
		helper.assertValueEqual(stage(player), "architect", journey.name() + ": a raised Spire makes an Architect");
	}

	/** The three routes, each its own way to its own Spire. */
	@GameTest(maxTicks = 20)
	public void eachRouteClimbsItsOwnWay(GameTestHelper helper) {
		climb(helper, CULTIVATION, new BlockPos(1, 2, 1));
		climb(helper, EXPLORATION, new BlockPos(3, 2, 1));
		climb(helper, INFRASTRUCTURE, new BlockPos(5, 2, 1));
		helper.succeed();
	}
}
