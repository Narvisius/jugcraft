package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Assay;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Property;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Sampler;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.encyclopedia.InspectPage;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Bestiary;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Encyclopedia;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Navigator;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairDossiers;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.LairVisit;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.LastRites;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.town.JugcraftTown;
import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.town.TownDecor;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Concordance Chunk A, A-04: the Jugcraft Encyclopedia's server side on a real server. A newcomer sees only the first
 * steps and nothing they have not met; the First Light route follows the player's research step by step; every recipe a
 * page shows is one this server has, and one a route needs but the server lacks is said to be unavailable; switched-off
 * features have no page; the Spellcraft page shows what is held; a lair is hidden until its token is carried and its
 * keeper until she is met; the route names only sales a town makes; the pages survive the trip to the client; requests
 * are rate-limited and the Inspect page reads only a near, loaded block; and the client's page model keeps its place.
 * Since Concordance Chunk B (B-02) the Glacier Hall has its dossier, each rite says whether it waits for the Halloween
 * season, a first win over any keeper is the route's lair step, and the Expeditions page holds every keeper whole;
 * and (B-08) the Glacier Hall's rite names Winter Brine and the Yeti Mitten. Since Concordance Chunk C (C-01) the
 * Inspect page reads an Alembic Crucible within the reader's own block reach, more with an Assay Glass carried and the
 * Alembic Arts learned, showing no sign and changing nothing, and gives a waiting formula's own reason; and (C-02) the
 * Cinder Kiln's rite names the Kiln Draught and the Salamander Charm. Since Concordance Chunk C (C-03), an anchor's
 * Inspect page states a known rite's readiness and its stage and payment table; and (C-07) a Verdant Bed and its crop
 * within the reader's reach, each factor judged with how far it reaches and the light as sky and lamps, reading nothing in;
 * and (C-09) a bed's page, and its host crop's, name the graft it carries and judge by the grafted niche.
 */
public class EncyclopediaGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static EncyclopediaPayload build(ServerPlayer player) {
		return Encyclopedia.build(player, new EncyclopediaRequestPayload(1, Optional.empty()), PageWriter::of);
	}

	private static EncyclopediaPayload.Page page(EncyclopediaPayload payload, String id) {
		return payload.pages().stream().filter(page -> page.id().equals(id)).findFirst().orElse(null);
	}

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
	}

	private static Object[] args(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getArgs() : new Object[0];
	}

	/** Every translation key in a component, its arguments and its siblings. */
	private static void keys(Component component, List<String> out) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			out.add(translatable.getKey());
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component inner) {
					keys(inner, out);
				}
			}
		}
		component.getSiblings().forEach(sibling -> keys(sibling, out));
	}

	private static List<String> keys(EncyclopediaPayload payload) {
		List<String> out = new ArrayList<>();
		payload.notices().forEach(notice -> keys(notice, out));
		for (EncyclopediaPayload.Page page : payload.pages()) {
			keys(page.title(), out);
			for (EncyclopediaPayload.Row row : page.rows()) {
				keys(row.text(), out);
				row.exact().ifPresent(exact -> keys(exact, out));
			}
		}
		return out;
	}

	/** The route's step rows, by step id (the step's text key is encyclopedia.jugcraft.route.<id>, or a variant of it, such as cast.kindle). */
	private static Map<String, EncyclopediaPayload.Row> steps(EncyclopediaPayload payload) {
		EncyclopediaPayload.Page discover = page(payload, "discover");
		if (discover == null) {
			return Map.of();
		}
		return discover.rows().stream().filter(row -> row.kindOf() == EncyclopediaPayload.Kind.STEP)
				.collect(Collectors.toMap(row -> key(row.text()).substring("encyclopedia.jugcraft.route.".length()).split("\\.")[0], row -> row,
						(a, b) -> a, java.util.LinkedHashMap::new));
	}

	private static EncyclopediaPayload.StepState state(EncyclopediaPayload payload, String step) {
		EncyclopediaPayload.Row row = steps(payload).get(step);
		return row == null ? null : row.stateOf();
	}

	private static List<EncyclopediaPayload.Row> rows(EncyclopediaPayload payload) {
		return payload.pages().stream().flatMap(page -> page.rows().stream()).toList();
	}

	private static List<EncyclopediaPayload.Grid> grids(EncyclopediaPayload payload) {
		return rows(payload).stream().flatMap(row -> row.grid().stream()).toList();
	}

	/** A lair's rows on the Expeditions page: from its heading to the next lair's. */
	private static List<EncyclopediaPayload.Row> section(EncyclopediaPayload.Page page, Lair lair) {
		List<EncyclopediaPayload.Row> out = new ArrayList<>();
		boolean in = false;
		for (EncyclopediaPayload.Row row : page == null ? List.<EncyclopediaPayload.Row>of() : page.rows()) {
			if (row.kindOf() == EncyclopediaPayload.Kind.HEADING && key(row.text()).startsWith("lair.jugcraft.")) {
				in = key(row.text()).equals("lair.jugcraft." + lair.id);
			}
			if (in) {
				out.add(row);
			}
		}
		return out;
	}

	private static EncyclopediaPayload.Row row(List<EncyclopediaPayload.Row> rows, String key) {
		return rows.stream().filter(row -> key(row.text()).equals(key)).findFirst().orElse(null);
	}

	private static boolean grid(List<EncyclopediaPayload.Row> rows, EncyclopediaPayload.EdgeKind edge, String recipe) {
		return rows.stream().anyMatch(row -> row.edgeOf() == edge && row.grid().map(grid -> grid.recipe().equals(Jugcraft.id(recipe))).orElse(false));
	}

	/**
	 * A lair that keeps no season, from its token to a win: the token shows the rite (its words and token, its recipe's
	 * grid, all year) and not the keeper; inside the lair, or owed a share, the keeper is met (support's thresholds, one of
	 * two trophies on a first win, each loot with JEI's words, the cheaper token after a boss, the way home); the
	 * advancement marks them beaten.
	 */
	private static void keeperDossier(GameTestHelper helper, LairDossiers.Boss boss, Item token, List<Item> trophies) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		String lair = boss.lair.id;
		String dossier = "encyclopedia.jugcraft.dossier.";
		player.getInventory().add(new ItemStack(token));
		helper.assertValueEqual(LairDossiers.discovery(player, boss), LairDossiers.Discovery.RITE, "carrying " + token + " finds the lair");
		EncyclopediaPayload found = build(player);
		List<EncyclopediaPayload.Row> rite = section(page(found, LairDossiers.PAGE), boss.lair);
		EncyclopediaPayload.Row words = row(rite, dossier + lair + ".rite");
		helper.assertTrue(words != null && words.icons().contains(token), "the rite, with its token: " + words);
		helper.assertTrue(grid(rite, EncyclopediaPayload.EdgeKind.REQUIRED, boss.rite.getFirst()), "the token's recipe, as a required grid");
		helper.assertTrue(row(rite, dossier + "all_year") != null, "the rite works all year");
		helper.assertTrue(keys(found).stream().noneMatch(key -> key.equals("entity.jugcraft." + boss.boss) || key.startsWith(dossier + "first_win")),
				"the keeper is not named before met");

		player.setAttached(Lairs.VISIT, new LairVisit(lair, 0, UUID.randomUUID(), "minecraft:overworld", 0.0, 64.0, 0.0, 0.0F, 0.0F));
		try {
			helper.assertValueEqual(LairDossiers.discovery(player, boss), LairDossiers.Discovery.MET, "inside the lair the keeper is met");
			List<EncyclopediaPayload.Row> met = section(page(build(player), LairDossiers.PAGE), boss.lair);
			EncyclopediaPayload.Row credit = row(met, dossier + lair + ".credit");
			Object[] numbers = credit == null ? new Object[0] : args(credit.text());
			helper.assertTrue(numbers.length == 3 && numbers[0].equals(String.valueOf((int) Encounter.HEALING))
					&& numbers[1].equals(String.valueOf((int) Encounter.WARDING)) && numbers[2].equals((int) Encounter.REACH),
					"support's thresholds are the encounter's: " + List.of(numbers));
			EncyclopediaPayload.Row first = row(met, dossier + "first_win.pair");
			helper.assertTrue(first != null && first.icons().equals(trophies), "a first win brings one of the two trophies: " + first);
			helper.assertValueEqual(met.stream().map(row -> key(row.text())).filter(key -> key.startsWith("jei.jugcraft.acquire.")).toList(),
					boss.loot.stream().map(loot -> "jei.jugcraft.acquire." + loot).toList(), "each loot, with JEI's words, in the table's order");
			helper.assertTrue(grid(met, EncyclopediaPayload.EdgeKind.OPTIONAL_BOSS, boss.afterWin.getFirst()), "the cheaper token, optional after a boss");
			helper.assertTrue(row(met, dossier + lair + ".leave") != null && row(met, dossier + "made_from") == null, "the way home; nothing made from the loot");
		} finally {
			player.removeAttached(Lairs.VISIT);
		}
		player.setAttached(LairRewards.OWED, new LairRewards.Owed(List.of(new LairRewards.Due(boss.boss, UUID.randomUUID(), Vec3.ZERO, 0))));
		try {
			helper.assertValueEqual(LairDossiers.discovery(player, boss), LairDossiers.Discovery.MET, "owed a share, the keeper is met");
			helper.assertTrue(row(section(page(build(player), LairDossiers.PAGE), boss.lair), dossier + "owed") != null, "and the kept share is named");
		} finally {
			// Never left for the hand-over that pays a living player's kept shares each second (LairRewards).
			player.removeAttached(LairRewards.OWED);
		}
		TrickOrTreat.award(player, boss.advancement);
		helper.assertValueEqual(LairDossiers.discovery(player, boss), LairDossiers.Discovery.DEFEATED, "a win is remembered");
		List<EncyclopediaPayload.Row> won = section(page(build(player), LairDossiers.PAGE), boss.lair);
		EncyclopediaPayload.Row title = row(won, dossier + "won");
		helper.assertTrue(title != null && key((Component) args(title.text())[0]).equals("advancements.jugcraft." + boss.advancement + ".title")
				&& row(won, dossier + "first_win.pair") == null, "beaten: the win is named and the first-win promise is gone");
		helper.succeed();
	}

	private static List<String> expectedSteps() {
		List<String> steps = new ArrayList<>(List.of("find", "examine", "dark", "understand", "wand", "cast", "compose", "support", "equip"));
		if (JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			steps.add("lair");
		}
		return steps;
	}

	/** A newcomer: only Discover; finding something that glows is next, the wand can be made now, the rest waits; nothing unmet is named. */
	@GameTest(maxTicks = 20)
	public void aNewcomerSeesOnlyTheFirstSteps(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		EncyclopediaPayload payload = build(player);
		helper.assertValueEqual(payload.pages().stream().map(EncyclopediaPayload.Page::id).toList(),
				List.of("discover", "nouveau", "menagerie", "woodland", "household", "habitats", "beasts", "caverns", "jimta_network", "jimta_guides"),
				"a newcomer's first steps and ungated owner-collection references");
		Map<String, EncyclopediaPayload.Row> steps = steps(payload);
		helper.assertValueEqual(List.copyOf(steps.keySet()), expectedSteps(), "the route's steps, in order");
		for (Map.Entry<String, EncyclopediaPayload.Row> step : steps.entrySet()) {
			EncyclopediaPayload.StepState want = switch (step.getKey()) {
				case "find" -> EncyclopediaPayload.StepState.NEXT;
				case "wand" -> EncyclopediaPayload.StepState.OPEN;
				default -> EncyclopediaPayload.StepState.LATER;
			};
			helper.assertValueEqual(step.getValue().stateOf(), want, "step " + step.getKey());
		}
		List<String> keys = keys(payload);
		for (String key : keys) {
			helper.assertFalse(key.startsWith("spell.jugcraft.") || key.startsWith("component.jugcraft.") || key.startsWith("entity.jugcraft.")
					|| key.startsWith("lair.jugcraft.") || key.startsWith("research.jugcraft.") && !key.equals("research.jugcraft.first_light"),
					"a newcomer is told nothing they have not met: " + key);
		}
		EncyclopediaPayload.Page discover = page(payload, "discover");
		EncyclopediaPayload.Row counted = discover.rows().stream().filter(row -> key(row.text()).equals("journal.jugcraft.research.unknown_count"))
				.findFirst().orElse(null);
		helper.assertTrue(counted != null && args(counted.text())[0].equals(ConcordanceData.rules().research().size()),
				"the research section counts every entry as still unknown: " + counted);
		helper.succeed();
	}

	/** Each step is done as the research is: examined, in the dark (with the light given exactly), understood, a wand, cast, composed, a ward. */
	@GameTest(maxTicks = 20)
	public void theRouteFollowsTheResearch(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:amethyst_shard", 12));
		EncyclopediaPayload examined = build(player);
		helper.assertTrue(state(examined, "find") == EncyclopediaPayload.StepState.DONE && state(examined, "examine") == EncyclopediaPayload.StepState.DONE
				&& state(examined, "dark") == EncyclopediaPayload.StepState.NEXT, "examined in the light: " + steps(examined).keySet());

		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:amethyst_shard", 0));
		EncyclopediaPayload dark = build(player);
		EncyclopediaPayload.Row darkRow = steps(dark).get("dark");
		helper.assertTrue(darkRow.stateOf() == EncyclopediaPayload.StepState.DONE && darkRow.exact().isPresent()
				&& key(darkRow.exact().get()).equals("encyclopedia.jugcraft.route.dark.light") && String.valueOf(args(darkRow.exact().get())[1]).equals("0"),
				"examined in the dark, the darkest light kept exactly: " + darkRow.exact());
		helper.assertValueEqual(state(dark, "understand"), EncyclopediaPayload.StepState.NEXT, "understanding comes next");

		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glowstone_dust", 2));
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glow_ink_sac", 4));
		EncyclopediaPayload understood = build(player);
		helper.assertTrue(ConcordanceProgress.knowledge(player).state(FIRST_LIGHT) == ResearchState.UNDERSTOOD
				&& state(understood, "understand") == EncyclopediaPayload.StepState.DONE, "three in the dark understand First Light");
		helper.assertValueEqual(state(understood, "wand"), EncyclopediaPayload.StepState.NEXT, "then the wand");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		EncyclopediaPayload wand = build(player);
		helper.assertTrue(state(wand, "wand") == EncyclopediaPayload.StepState.DONE && state(wand, "cast") == EncyclopediaPayload.StepState.NEXT
				&& state(wand, "compose") == EncyclopediaPayload.StepState.OPEN, "with a wand, cast next and compose now");
		helper.assertTrue(keys(wand).contains(Invocations.nameKey("jugcraft:kindle")), "once understood, the invocations are named");
		// The spell bar offers Dawn Aegis, Kindle and Revelation in that order: the use key casts Dawn Aegis, 2 Kindle.
		helper.assertValueEqual(key(steps(wand).get("cast").text()), "encyclopedia.jugcraft.route.cast.kindle", "once understood, the cast step names Kindle");
		EncyclopediaPayload.Row goal = rows(wand).stream().filter(row -> key(row.text()).equals("encyclopedia.jugcraft.route.cast.goal")).findFirst().orElseThrow();
		helper.assertTrue(args(goal.text())[1] instanceof Component bound && bound.getContents() instanceof KeybindContents keybind
				&& keybind.getName().equals("key.hotbar.2"), "Kindle is cast with the second spell key: " + List.of(args(goal.text())));

		ConcordanceProgress.award(player, Jugcraft.id("concordance_first_kindle"));
		helper.assertValueEqual(state(build(player), "cast"), EncyclopediaPayload.StepState.DONE, "Let There Be is the cast step");

		RateGate.forget(player.getUUID());
		Composing.Result light = Composing.compose(player, "touch struck light", true, Composing.Held.of(player));
		EncyclopediaPayload composed = build(player);
		helper.assertTrue(light.outcome() == Composing.Outcome.INSCRIBED && state(composed, "compose") == EncyclopediaPayload.StepState.DONE
				&& state(composed, "support") == EncyclopediaPayload.StepState.NEXT, "a spell inscribed: compose done, a ward for a friend next");

		RateGate.forget(player.getUUID());
		Composing.Result ward = Composing.compose(player, "touch struck ward", true, Composing.Held.of(player));
		helper.assertTrue(ward.outcome() == Composing.Outcome.INSCRIBED && state(build(player), "support") == EncyclopediaPayload.StepState.DONE,
				"a ward for a friend inscribed: support done");
		RateGate.forget(player.getUUID());
		Composing.compose(player, "here struck ward", true, Composing.Held.of(player));
		helper.assertValueEqual(state(build(player), "support"), EncyclopediaPayload.StepState.NEXT, "a ward on oneself is not support");
		RateGate.forget(player.getUUID());
		Composing.Result prolonged = Composing.compose(player, "touch struck ward+prolong", true, Composing.Held.of(player));
		helper.assertTrue(prolonged.outcome() == Composing.Outcome.INSCRIBED && state(build(player), "support") == EncyclopediaPayload.StepState.DONE,
				"a ward with a modifier is support too: " + prolonged.outcome());
		helper.succeed();
	}

	/** Every grid a page shows is a recipe this server has; the wand's is its diagonal shaped grid. */
	@GameTest(maxTicks = 20)
	public void everyRecipeShownIsOnTheServer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer newcomer = player(helper);
		ServerPlayer understood = player(helper);
		ConcordanceProgress.grant(understood, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		understood.getInventory().add(new ItemStack(JugcraftLairs.DEATH_KNELL));
		for (ServerPlayer player : List.of(newcomer, understood)) {
			for (EncyclopediaPayload.Grid grid : grids(build(player))) {
				helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, grid.recipe())).isPresent(),
						"a grid shows a recipe the server has: " + grid.recipe());
				helper.assertTrue(grid.slots().size() <= EncyclopediaPayload.Grid.MAX_SLOTS && grid.result() != Items.AIR, "a whole grid: " + grid);
			}
		}
		EncyclopediaPayload.Grid wand = grids(build(newcomer)).stream().filter(grid -> grid.recipe().equals(Jugcraft.id("initiate_wand")))
				.findFirst().orElse(null);
		helper.assertTrue(wand != null && wand.station() == EncyclopediaPayload.Grid.SHAPED && wand.width() == 3 && wand.result() == JugcraftConcordance.INITIATE_WAND,
				"the wand's recipe is shown as a shaped 3-wide grid: " + wand);
		Set<Item> inputs = wand.slots().stream().flatMap(List::stream).collect(Collectors.toSet());
		helper.assertTrue(inputs.contains(Items.STICK) && inputs.contains(Items.COPPER_INGOT)
				&& (inputs.contains(Items.AMETHYST_SHARD) || inputs.contains(Items.GLOWSTONE_DUST)), "a stick, a copper ingot and luminous matter: " + inputs);
		helper.succeed();
	}

	/** A server without the wand's and the bench's recipes: the wand step is not available, and neither recipe is shown. */
	@GameTest(maxTicks = 20)
	public void anAbsentRecipeIsNeverAdvertised(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:amethyst_shard", 0));
		PageWriter.Recipes real = PageWriter.recipes(helper.getLevel());
		Identifier wand = Jugcraft.id("initiate_wand");
		Identifier bench = Jugcraft.id("lampwright_bench");
		PageWriter.Recipes without = id -> id.equals(wand) || id.equals(bench) ? Optional.empty() : real.find(id);
		EncyclopediaPayload payload = Encyclopedia.build(player, new EncyclopediaRequestPayload(1, Optional.empty()), p -> new PageWriter(p, without));
		helper.assertValueEqual(state(payload, "wand"), EncyclopediaPayload.StepState.BLOCKED, "the wand cannot be made here");
		helper.assertTrue(rows(payload).stream().anyMatch(row -> row.kindOf() == EncyclopediaPayload.Kind.EDGE
				&& row.stateOf() == EncyclopediaPayload.StepState.BLOCKED && key(row.text()).equals("encyclopedia.jugcraft.absent")),
				"the missing required recipe is said to be unavailable");
		helper.assertTrue(grids(payload).stream().noneMatch(grid -> grid.recipe().equals(wand) || grid.recipe().equals(bench)), "neither is shown");
		helper.assertValueEqual(state(payload, "understand"), EncyclopediaPayload.StepState.NEXT, "understanding is next");
		helper.assertTrue(rows(payload).stream().anyMatch(row -> key(row.text()).equals("encyclopedia.jugcraft.route.understand.study") && row.grid().isEmpty()),
				"the study at a bench is still a way, without a grid");
		helper.succeed();
	}

	/** The Concordance off: no Concordance page, and a notice; the lairs off: no dossier even with a token carried. */
	@GameTest(maxTicks = 20)
	public void switchedOffFeaturesHaveNoPages(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		player.getInventory().add(new ItemStack(JugcraftLairs.DEATH_KNELL));
		boolean concordance = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		boolean lairs = JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE);
		EncyclopediaPayload off;
		EncyclopediaPayload noLairs;
		// Switched back at once: the server runs other tests.
		try {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
			off = build(player);
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, concordance);
		}
		try {
			JugcraftConfig.setFeatureEnabled(JugcraftLairs.FEATURE, false);
			noLairs = build(player);
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftLairs.FEATURE, lairs);
		}
		helper.assertTrue(off.pages().stream().noneMatch(page -> List.of("discover", "spellcraft", "inspect").contains(page.id())),
				"no Concordance page with it off: " + off.pages().stream().map(EncyclopediaPayload.Page::id).toList());
		helper.assertTrue(off.notices().stream().anyMatch(notice -> key(notice).equals("message.jugcraft.concordance.disabled")), "and a notice says so");
		helper.assertTrue(page(noLairs, "expedition") == null, "no dossier with the lairs off");
		helper.assertTrue(lairs && page(build(player), "expedition") != null || !lairs, "a dossier with them on");
		helper.succeed();
	}

	/** Spellcraft: the wand's limits, what is inscribed and its cost, the invocations known with their cost, the words, and the composer. */
	@GameTest(maxTicks = 20)
	public void spellcraftShowsWhatIsHeld(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		EncyclopediaPayload.Page early = page(build(player), "spellcraft");
		helper.assertTrue(early != null && early.rows().stream().noneMatch(row -> row.link().map(EncyclopediaPayload.Link::target).orElse("")
				.equals("composition/composing")), "a wand carried shows Spellcraft, without the locked codex entry");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		Composing.compose(player, "touch struck light", true, Composing.Held.of(player));
		EncyclopediaPayload.Page page = page(build(player), "spellcraft");
		EncyclopediaPayload.Row instrument = page.rows().stream().filter(row -> key(row.text()).equals("encyclopedia.jugcraft.spellcraft.instrument"))
				.findFirst().orElseThrow();
		Object[] limits = args(instrument.exact().orElseThrow());
		helper.assertTrue(limits[0].equals(8) && limits[1].equals(6) && limits[2].equals(48) && limits[3].equals(1) && String.valueOf(limits[4]).equals("60"),
				"the Initiate's Wand's limits: " + List.of(limits));
		EncyclopediaPayload.Row inscribed = page.rows().stream().filter(row -> key(row.text()).equals("compose.jugcraft.inscription")).findFirst().orElseThrow();
		helper.assertTrue(args(inscribed.text())[0].equals("touch struck light") && args(inscribed.exact().orElseThrow())[0].equals(2)
				&& String.valueOf(args(inscribed.exact().orElseThrow())[1]).equals("1"), "what is inscribed and what it costs");
		Map<String, Integer> known = ConcordanceProgress.knowledge(player).invocations();
		Map<String, Object> shown = page.rows().stream().filter(row -> key(row.text()).equals("encyclopedia.jugcraft.route.cast.invocation"))
				.collect(Collectors.toMap(row -> key((Component) args(row.text())[0]), row -> args(row.text())[1]));
		helper.assertValueEqual(shown, known.entrySet().stream().collect(Collectors.toMap(entry -> Invocations.nameKey(entry.getKey()), Map.Entry::getValue)),
				"every invocation known, with its cost");
		helper.assertTrue(page.rows().stream().anyMatch(row -> row.link().map(EncyclopediaPayload.Link::kind).orElse(-1) == EncyclopediaPayload.Link.COMPOSER),
				"a way into the Spell Composer");
		helper.assertTrue(page.rows().stream().anyMatch(row -> row.link().map(EncyclopediaPayload.Link::target).orElse("").equals("composition/composing")),
				"and the codex's Composing Spells, now open");
		helper.succeed();
	}

	/** A lair is unseen until its token is carried, its keeper until met; a first win is remembered. */
	@GameTest(maxTicks = 20)
	public void aBossIsHiddenUntilDiscovered(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		helper.assertTrue(page(build(player), "expedition") == null, "a newcomer has no dossier");

		player.getInventory().add(new ItemStack(JugcraftLairs.DEATH_KNELL));
		EncyclopediaPayload rite = build(player);
		EncyclopediaPayload.Page dossier = page(rite, "expedition");
		helper.assertTrue(dossier != null && LairDossiers.discovery(player, LairDossiers.Boss.VESPERINE) == LairDossiers.Discovery.RITE,
				"carrying a Death Knell shows the Hollow Acre's rite");
		EncyclopediaPayload.Row ritual = dossier.rows().stream().filter(row -> key(row.text()).equals("encyclopedia.jugcraft.dossier.hollow_acre.rite"))
				.findFirst().orElseThrow();
		helper.assertTrue(args(ritual.text())[1].equals(LastRites.CANDLES) && args(ritual.text())[2].equals(LastRites.CANDLE_RANGE),
				"the rite's numbers are the game's: " + List.of(args(ritual.text())));
		helper.assertTrue(keys(rite).stream().noneMatch(key -> key.startsWith("entity.jugcraft.") || key.equals("lair.jugcraft.spindle_loft")),
				"no keeper, and no other lair, before they are met");

		player.setAttached(Lairs.VISIT, new LairVisit("spindle_loft", 0, UUID.randomUUID(), "minecraft:overworld", 0.0, 64.0, 0.0, 0.0F, 0.0F));
		try {
			helper.assertValueEqual(LairDossiers.discovery(player, LairDossiers.Boss.TATTERLACE), LairDossiers.Discovery.MET, "inside her loft she is met");
			EncyclopediaPayload met = build(player);
			helper.assertTrue(grids(met).stream().anyMatch(grid -> grid.recipe().equals(Jugcraft.id("cursed_spindle_from_silk"))),
					"her silk's cheaper spindle is shown");
			helper.assertTrue(rows(met).stream().anyMatch(row -> row.edgeOf() == EncyclopediaPayload.EdgeKind.OPTIONAL_BOSS
					&& row.grid().map(grid -> grid.recipe().equals(Jugcraft.id("cursed_spindle_from_silk"))).orElse(false)), "as optional, after a boss");
			EncyclopediaPayload.Row credit = rows(met).stream().filter(row -> key(row.text()).equals("encyclopedia.jugcraft.dossier.spindle_loft.credit"))
					.findFirst().orElseThrow();
			Object[] numbers = args(credit.text());
			helper.assertTrue(numbers[0].equals(String.valueOf((int) Encounter.HEALING)) && numbers[1].equals(String.valueOf((int) Encounter.WARDING))
					&& numbers[2].equals((int) Encounter.REACH), "support's thresholds are the encounter's: " + List.of(numbers));
		} finally {
			player.removeAttached(Lairs.VISIT);
		}
		TrickOrTreat.award(player, "the_last_harvest");
		helper.assertValueEqual(LairDossiers.discovery(player, LairDossiers.Boss.VESPERINE), LairDossiers.Discovery.DEFEATED, "a win is remembered");
		helper.assertTrue(rows(build(player)).stream().anyMatch(row -> key(row.text()).equals("encyclopedia.jugcraft.dossier.made_from")
				&& row.grid().map(grid -> grid.recipe().equals(Jugcraft.id("reaper_hood"))).orElse(false)),
				"the Reaper's Hood is made from her loot, not cheaper with it");
		helper.succeed();
	}

	/** Concordance Chunk B (B-02): the Glacier Hall's dossier, from a Frost Horn carried to a win over the Yeti King. */
	@GameTest(maxTicks = 20)
	public void theGlacierHallsDossier(GameTestHelper helper) {
		keeperDossier(helper, LairDossiers.Boss.YETI_KING, JugcraftLairs.FROST_HORN, List.of(JugcraftYeti.glacierMaul(), JugcraftYeti.rimeclaw()));
	}

	/**
	 * Concordance Chunk B (B-08, B-P29): the Glacier Hall's rite names what readies a player for his frost, as text and
	 * icons with no grid: Winter Brine's Frostward for the data's longest time, with a codex link once the Alembic Arts
	 * are observed, and the Yeti Mitten. With the Concordance off, the Mitten alone.
	 */
	@GameTest(maxTicks = 20)
	public void theGlacierHallsRiteNamesWinterBrine(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE) || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			helper.succeed();
			return;
		}
		String prepare = "encyclopedia.jugcraft.dossier.glacier_hall.prepare";
		String codex = "book.jugcraft.arcane_concordance.alembic.winter_brine.name";
		ServerPlayer player = player(helper);
		player.getInventory().add(new ItemStack(JugcraftLairs.FROST_HORN));
		EncyclopediaPayload carried = build(player);
		List<EncyclopediaPayload.Row> rite = section(page(carried, LairDossiers.PAGE), Lair.GLACIER_HALL);
		EncyclopediaPayload.Row brine = row(rite, prepare);
		helper.assertTrue(brine != null && brine.kindOf() == EncyclopediaPayload.Kind.FACT && brine.grid().isEmpty(),
				"carrying a Frost Horn, the rite names Winter Brine as a fact with no grid: " + rite.stream().map(row -> key(row.text())).toList());
		Object[] seconds = args(brine.text());
		helper.assertTrue(seconds.length == 1 && seconds[0].equals(Alchemy.catalog().property("rime").maxTicks() / 20),
				"Frostward's longest time is the data's: " + List.of(seconds));
		helper.assertValueEqual(brine.icons(), List.of(Items.SNOWBALL, JugcraftConcordance.CRUCIBLE.asItem(), JugcraftConcordance.DRAUGHT,
				JugcraftYeti.YETI_MITTEN), "snowballs, the crucible, a draught and the Mitten");
		helper.assertFalse(keys(carried).contains(codex), "no codex link before the Alembic Arts are observed");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.OBSERVED);
		helper.assertTrue(keys(build(player)).contains(codex), "the Alembic Arts observed, the dossier links Winter Brine's codex entry");

		// The Concordance off: the Mitten alone (switched back at once; the server runs other tests).
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		List<String> off;
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			off = section(page(build(player), LairDossiers.PAGE), Lair.GLACIER_HALL).stream().map(row -> key(row.text())).toList();
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertTrue(off.contains(prepare + ".mitten") && !off.contains(prepare), "with the Concordance off, the Mitten alone: " + off);
		helper.succeed();
	}

	/** Concordance Chunk B (B-02): the Cinder Kiln's dossier, from a Kiln Seal carried to a win over the Cinder Tyrant. */
	@GameTest(maxTicks = 20)
	public void theCinderKilnsDossier(GameTestHelper helper) {
		keeperDossier(helper, LairDossiers.Boss.CINDER_TYRANT, JugcraftLairs.KILN_SEAL, List.of(JugcraftTyrant.cinderbrand(), JugcraftTyrant.magmaw()));
	}

	/**
	 * Concordance Chunk C (C-02, C-P9): the Cinder Kiln's rite names what readies a player for his slag and flame, as text
	 * and icons with no grid: the Kiln Draught's Fire Resistance for the data's longest time (magma cream or sugar, the
	 * crucible, a draught) and the Salamander Charm, with a codex link once the Alembic Arts are observed. Named only while
	 * the data's Ember brews Fire Resistance; with the Concordance off, the Charm alone.
	 */
	@GameTest(maxTicks = 20)
	public void theCinderKilnsRiteNamesTheKilnDraught(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE) || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			helper.succeed();
			return;
		}
		String prepare = "encyclopedia.jugcraft.dossier.cinder_kiln.prepare";
		String codex = "book.jugcraft.arcane_concordance.alembic.kiln_draught.name";
		ServerPlayer player = player(helper);
		player.getInventory().add(new ItemStack(JugcraftLairs.KILN_SEAL));
		EncyclopediaPayload carried = build(player);
		List<EncyclopediaPayload.Row> rite = section(page(carried, LairDossiers.PAGE), Lair.CINDER_KILN);
		EncyclopediaPayload.Row draught = row(rite, prepare);
		helper.assertTrue(draught != null && draught.kindOf() == EncyclopediaPayload.Kind.FACT && draught.grid().isEmpty() && row(rite, prepare + ".charm") == null,
				"carrying a Kiln Seal, the rite names the Kiln Draught as a fact with no grid: " + rite.stream().map(row -> key(row.text())).toList());
		Object[] seconds = args(draught.text());
		helper.assertTrue(seconds.length == 1 && seconds[0].equals(Alchemy.catalog().property("ember").maxTicks() / 20),
				"Fire Resistance's longest time is the data's: " + List.of(seconds));
		helper.assertValueEqual(draught.icons(), List.of(Items.MAGMA_CREAM, Items.SUGAR, JugcraftConcordance.CRUCIBLE.asItem(), JugcraftConcordance.DRAUGHT,
				JugcraftTyrant.SALAMANDER_CHARM), "magma cream, sugar, the crucible, a draught and the Charm");
		helper.assertFalse(keys(carried).contains(codex), "no codex link before the Alembic Arts are observed");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.OBSERVED);
		helper.assertTrue(keys(build(player)).contains(codex), "the Alembic Arts observed, the dossier links the Kiln Draught's codex entry");

		// Named only while the data's Ember brews Fire Resistance.
		Property ember = Alchemy.catalog().property("ember");
		helper.assertTrue(LairDossiers.brews(ember, LairDossiers.FIRE_RESISTANCE) && !LairDossiers.brews(null, LairDossiers.FIRE_RESISTANCE)
				&& !LairDossiers.brews(new Property("ember", "minecraft:speed", Intent.HELPFUL, 600, 10000, 1, 1200, 2400, 0L), LairDossiers.FIRE_RESISTANCE),
				"the Kiln Draught is named only while Ember brews Fire Resistance: " + ember);

		// The Concordance off: the Charm alone (switched back at once; the server runs other tests).
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		List<String> off;
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			off = section(page(build(player), LairDossiers.PAGE), Lair.CINDER_KILN).stream().map(row -> key(row.text())).toList();
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertTrue(off.contains(prepare + ".charm") && !off.contains(prepare), "with the Concordance off, the Charm alone: " + off);
		helper.succeed();
	}

	/** With the Halloween event over and lairs.off_season off, the Last Rites wait for the season; the horn and the seal work all year. */
	@GameTest(maxTicks = 20)
	public void eachRiteSaysWhenItWorks(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		for (Item token : List.of(JugcraftLairs.DEATH_KNELL, JugcraftLairs.FROST_HORN, JugcraftLairs.KILN_SEAL)) {
			player.getInventory().add(new ItemStack(token));
		}
		HalloweenSeason.Mode mode = HalloweenSeason.mode();
		String offSeason = JugcraftConfig.textOption("lairs.off_season");
		EncyclopediaPayload.Page page;
		// Switched back at once: the server runs other tests.
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			JugcraftConfig.setTextOption("lairs.off_season", "off");
			page = page(build(player), LairDossiers.PAGE);
		} finally {
			HalloweenSeason.setMode(mode);
			JugcraftConfig.setTextOption("lairs.off_season", offSeason);
		}
		helper.assertTrue(row(section(page, Lair.HOLLOW_ACRE), "message.jugcraft.lair.rite.off_season") != null, "out of season the Last Rites wait for it");
		for (Lair lair : List.of(Lair.GLACIER_HALL, Lair.CINDER_KILN)) {
			List<EncyclopediaPayload.Row> rows = section(page, lair);
			helper.assertTrue(row(rows, "encyclopedia.jugcraft.dossier.all_year") != null && row(rows, "message.jugcraft.lair.rite.off_season") == null,
					lair.id + "'s rite works all year: " + rows.stream().map(row -> key(row.text())).toList());
		}
		helper.succeed();
	}

	/** The route's lair step: open once a spell is composed, done by a first win over any keeper (each in turn). */
	@GameTest(maxTicks = 20)
	public void aWinOverAnyKeeperIsTheLairStep(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE) || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			helper.succeed();
			return;
		}
		for (LairDossiers.Boss boss : LairDossiers.Boss.values()) {
			ServerPlayer player = player(helper);
			ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
			Composing.Result light = Composing.compose(player, "touch struck light", true, Composing.Held.of(player));
			helper.assertTrue(light.outcome() == Composing.Outcome.INSCRIBED && state(build(player), "lair") == EncyclopediaPayload.StepState.OPEN,
					"composed, the lair step can be done now (" + boss + ")");
			TrickOrTreat.award(player, boss.advancement);
			helper.assertValueEqual(state(build(player), "lair"), EncyclopediaPayload.StepState.DONE, boss.advancement + " completes the lair step");
		}
		helper.succeed();
	}

	/**
	 * Every keeper found and beaten: each dossier is whole (with the Stillroot Poultice's credit while the Concordance is
	 * on, Chunk C, C-02), every recipe named is drawn as a grid, in order, within PageWriter.MAX_GRIDS, and no row is
	 * dropped. A keeper past the bound needs the page's layout changed first.
	 */
	@GameTest(maxTicks = 20)
	public void theExpeditionsPageHoldsEveryKeeper(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		List<Identifier> wanted = new ArrayList<>();
		for (LairDossiers.Boss boss : LairDossiers.Boss.values()) {
			TrickOrTreat.award(player, boss.advancement);
			for (List<String> recipes : List.of(boss.rite, boss.afterWin, boss.madeFrom)) {
				recipes.forEach(recipe -> wanted.add(Jugcraft.id(recipe)));
			}
		}
		EncyclopediaPayload.Page page = page(build(player), LairDossiers.PAGE);
		helper.assertTrue(page != null, "every keeper beaten has a dossier");
		List<Identifier> drawn = page.rows().stream().flatMap(row -> row.grid().stream()).map(EncyclopediaPayload.Grid::recipe).toList();
		helper.assertValueEqual(drawn, wanted, "every recipe named, drawn in order (" + drawn.size() + " grids of " + PageWriter.MAX_GRIDS + ")");
		helper.assertTrue(page.rows().stream().noneMatch(row -> key(row.text()).equals("encyclopedia.jugcraft.more"))
				&& key(page.rows().getLast().text()).equals("encyclopedia.jugcraft.dossier.grave_goods"),
				"no row dropped: " + page.rows().size() + " rows of " + EncyclopediaPayload.Page.MAX_ROWS);
		String dossier = "encyclopedia.jugcraft.dossier.";
		for (LairDossiers.Boss boss : LairDossiers.Boss.values()) {
			List<String> keys = section(page, boss.lair).stream().map(row -> key(row.text())).toList();
			List<String> whole = new ArrayList<>(List.of("lair.jugcraft." + boss.lair.id, dossier + boss.lair.id + ".rite", dossier + "party",
					"entity.jugcraft." + boss.boss, dossier + boss.lair.id + ".credit", dossier + "credit.wards", dossier + "won",
					dossier + boss.lair.id + ".leave", dossier + "grave_goods"));
			boss.loot.forEach(loot -> whole.add("jei.jugcraft.acquire." + loot));
			if (JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
				whole.add(dossier + "credit.poultice");
			}
			helper.assertTrue(keys.containsAll(whole), boss + "'s dossier is whole: " + keys);
		}
		helper.succeed();
	}

	/**
	 * Concordance Chunk C (C-02, C-P8): a keeper's dossier says a Stillroot Poultice on a friend counts as healing, with a
	 * salve's icon, right after the wards' rule. With the Concordance off it is not named, and the wards' rule stays.
	 */
	@GameTest(maxTicks = 20)
	public void theDossiersNameThePoulticesCredit(GameTestHelper helper) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE) || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			helper.succeed();
			return;
		}
		String dossier = "encyclopedia.jugcraft.dossier.";
		ServerPlayer player = player(helper);
		TrickOrTreat.award(player, LairDossiers.Boss.CINDER_TYRANT.advancement);
		List<EncyclopediaPayload.Row> rows = section(page(build(player), LairDossiers.PAGE), Lair.CINDER_KILN);
		List<String> keys = rows.stream().map(row -> key(row.text())).toList();
		EncyclopediaPayload.Row poultice = row(rows, dossier + "credit.poultice");
		helper.assertTrue(poultice != null && poultice.icons().equals(List.of(JugcraftConcordance.SALVE))
				&& keys.indexOf(dossier + "credit.poultice") == keys.indexOf(dossier + "credit.wards") + 1,
				"beaten, the keeper's dossier names the poultice's credit, with a salve, after the wards' rule: " + keys);
		Product salve = Alchemy.catalog().product(Product.Form.SALVE, List.of(Encounter.POULTICE_STATUS), false);
		helper.assertTrue(salve != null && Alchemy.STILLROOT_POULTICE.equals(salve.id()),
				"named because a salve of that status alone is the Stillroot Poultice, whose credit Alchemy.product finds: " + salve);

		// The Concordance off: not named (switched back at once; the server runs other tests).
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		List<String> off;
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			off = section(page(build(player), LairDossiers.PAGE), Lair.CINDER_KILN).stream().map(row -> key(row.text())).toList();
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertTrue(off.contains(dossier + "credit.wards") && !off.contains(dossier + "credit.poultice"),
				"with the Concordance off, the wards' rule and no poultice: " + off);
		helper.succeed();
	}

	/** Sales on the route are the town's own: glow ink sacs are sold, amethyst and copper are not; and only where a town stands. */
	@GameTest(maxTicks = 20)
	public void theRouteNamesOnlyRealSales(GameTestHelper helper) {
		String theme = TownDecor.theme();
		helper.assertFalse(TownShops.get().sales(Items.GLOW_INK_SAC, theme).isEmpty(), "Curiosities sells glow ink sacs");
		helper.assertTrue(TownShops.get().sales(Items.AMETHYST_SHARD, theme).isEmpty() && TownShops.get().sales(Items.COPPER_INGOT, theme).isEmpty(),
				"no shop sells amethyst or copper (the General Store only buys them)");
		boolean town = JugcraftConfig.isFeatureEnabled(JugcraftTown.FEATURE) && Town.origin(helper.getLevel().getServer().overworld()) != null;
		EncyclopediaPayload payload = build(player(helper));
		boolean trade = rows(payload).stream().anyMatch(row -> row.edgeOf() == EncyclopediaPayload.EdgeKind.TRADE && key(row.text()).equals("encyclopedia.jugcraft.trade.town"));
		helper.assertValueEqual(trade, town, "a sale is named exactly when a town stands");
		helper.succeed();
	}

	/** The pages cross to the client whole, and well under the size a packet may be. */
	@GameTest(maxTicks = 20)
	public void theEncyclopediaSurvivesTheTrip(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		player.getInventory().add(new ItemStack(JugcraftLairs.DEATH_KNELL));
		player.getInventory().add(new ItemStack(JugcraftLairs.CURSED_SPINDLE));
		EncyclopediaPayload sent = build(player);
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		EncyclopediaPayload.CODEC.encode(buffer, sent);
		int size = buffer.writerIndex();
		EncyclopediaPayload received = EncyclopediaPayload.CODEC.decode(buffer);
		helper.assertTrue(size < 256 * 1024, "the encyclopedia is " + size + " bytes");
		helper.assertValueEqual(received.sequence(), sent.sequence(), "the sequence");
		helper.assertValueEqual(received.pages().size(), sent.pages().size(), "every page");
		for (int i = 0; i < sent.pages().size(); i++) {
			EncyclopediaPayload.Page a = sent.pages().get(i);
			EncyclopediaPayload.Page b = received.pages().get(i);
			helper.assertTrue(a.id().equals(b.id()) && a.title().getString().equals(b.title().getString()) && a.rows().size() == b.rows().size(), "page " + a.id());
			for (int j = 0; j < a.rows().size(); j++) {
				EncyclopediaPayload.Row x = a.rows().get(j);
				EncyclopediaPayload.Row y = b.rows().get(j);
				helper.assertTrue(x.kind() == y.kind() && x.state() == y.state() && x.edge() == y.edge() && x.text().getString().equals(y.text().getString())
						&& x.exact().map(Component::getString).equals(y.exact().map(Component::getString)) && x.icons().equals(y.icons())
						&& x.grid().equals(y.grid()) && x.link().equals(y.link()), "row " + j + " of " + a.id());
			}
		}
		helper.succeed();
	}

	/**
	 * Two requests in one tick: the first is answered, the second waits for its tick. The Inspect page reads a Circle
	 * Anchor three blocks away (its own report), and nothing out of reach or not loaded.
	 */
	@GameTest(maxTicks = 40)
	public void requestsAreGatedAndAnswered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		Encyclopedia.receive(player, new EncyclopediaRequestPayload(1, Optional.empty()));
		boolean first = !Encyclopedia.waiting(player);
		Encyclopedia.receive(player, new EncyclopediaRequestPayload(2, Optional.empty()));
		helper.assertTrue(first && Encyclopedia.waiting(player), "the first is answered and the second waits");

		BlockPos anchorAt = new BlockPos(1, 2, 4);
		helper.setBlock(anchorAt, JugcraftConcordance.CIRCLE_ANCHOR);
		BlockPos absolute = helper.absolutePos(anchorAt);
		EncyclopediaPayload near = Encyclopedia.build(player, new EncyclopediaRequestPayload(3, Optional.of(absolute)), PageWriter::of);
		EncyclopediaPayload.Page inspect = page(near, "inspect");
		helper.assertTrue(inspect != null, "a near anchor is inspected");
		List<String> report = new ArrayList<>();
		((CircleAnchorBlockEntity) level.getBlockEntity(absolute)).status(level, line -> report.add(line.getString()));
		List<String> shown = inspect.rows().stream().skip(1).limit(report.size()).map(row -> row.text().getString()).toList();
		helper.assertValueEqual(shown, report, "the Inspect page is the anchor's own report");

		BlockPos far = absolute.offset(0, 0, 17);
		BlockPos unloaded = new BlockPos(absolute.getX() + 100_000, absolute.getY(), absolute.getZ());
		for (BlockPos pos : List.of(far, unloaded)) {
			EncyclopediaPayload away = Encyclopedia.build(player, new EncyclopediaRequestPayload(4, Optional.of(pos)), PageWriter::of);
			helper.assertTrue(page(away, "inspect") == null, "nothing out of reach or unloaded is inspected: " + pos);
		}
		helper.runAfterDelay(Encyclopedia.REQUEST_TICKS + 2, () -> {
			helper.assertFalse(Encyclopedia.waiting(player), "the waiting request is answered at its tick");
			// Answered, not dropped: answering records the time, so the gate is shut again two ticks later.
			helper.assertFalse(RateGate.allow(player, Encyclopedia.RATE_ACTION, Encyclopedia.REQUEST_TICKS),
					"the waiting request was answered at its tick, not dropped");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- an Alembic Crucible (Concordance Chunk C, C-01)

	private static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at, Block beneath) {
		helper.setBlock(at.below(), beneath);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return (CrucibleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
	}

	/** Works a crucible by hand with {@code held}, as a use of it does. */
	private static InteractionResult work(CrucibleBlockEntity crucible, ServerPlayer player, ItemStack held) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return crucible.use(player, (ServerLevel) player.level(), held, InteractionHand.MAIN_HAND);
	}

	/** The Inspect page's rows for {@code player} looking at {@code pos} (absolute), or null when there is none. */
	private static List<EncyclopediaPayload.Row> inspected(ServerPlayer player, BlockPos pos) {
		EncyclopediaPayload.Page page = page(Encyclopedia.build(player, new EncyclopediaRequestPayload(5, Optional.of(pos)), PageWriter::of), InspectPage.PAGE);
		return page == null ? null : page.rows();
	}

	/** The facts under the heading whose key is {@code heading}, as shown, up to the next heading. */
	private static List<String> factsUnder(List<EncyclopediaPayload.Row> rows, String heading) {
		List<String> out = new ArrayList<>();
		boolean in = false;
		for (EncyclopediaPayload.Row row : rows) {
			if (row.kindOf() == EncyclopediaPayload.Kind.HEADING) {
				in = key(row.text()).equals(heading);
			} else if (in && row.kindOf() == EncyclopediaPayload.Kind.FACT) {
				out.add(row.text().getString());
			}
		}
		return out;
	}

	private static List<String> headingKeys(List<EncyclopediaPayload.Row> rows) {
		return rows.stream().filter(row -> row.kindOf() == EncyclopediaPayload.Kind.HEADING).map(row -> key(row.text())).toList();
	}

	/** Every translation key in the rows' text and exact text. */
	private static List<String> allKeys(List<EncyclopediaPayload.Row> rows) {
		List<String> out = new ArrayList<>();
		for (EncyclopediaPayload.Row row : rows) {
			keys(row.text(), out);
			row.exact().ifPresent(exact -> keys(exact, out));
		}
		return out;
	}

	/** The simulation's lines as the page shows them. */
	private static List<String> worded(List<Text> lines) {
		return lines.stream().map(line -> ComposeText.show(line).getString()).toList();
	}

	/** A row's arguments, or none if there is no such row. */
	private static List<Object> argsOf(List<EncyclopediaPayload.Row> rows, String key) {
		EncyclopediaPayload.Row row = row(rows, key);
		return row == null ? List.of() : List.of(args(row.text()));
	}

	/**
	 * C-01 (C-P3, C-P26): the Inspect page reads an Alembic Crucible within the reader's own block reach. Anyone sees its
	 * heat and what heats it, volume, tank, formula, the risk line (a spider eye's Poison), its slots and a spoon's sample;
	 * a carried Assay Glass adds the glass's measures once the Alembic Arts are understood and the formula's outcome under
	 * the rules in force once they are mastered (a glass without the research, or the research without a glass, reads as a
	 * spoon). Reading shows no sign and changes nothing. A clean formula set over a harmful mixture still has that
	 * mixture's harm named. A crucible past the reader's reach (though within the anchor's 16 blocks), or not loaded, has
	 * no page; and a formula's own reason for waiting is on its page.
	 */
	@GameTest(maxTicks = 60)
	public void theInspectPageReadsACrucibleByResearch(GameTestHelper helper) {
		AlchemyCatalog catalog = Alchemy.catalog();
		String inspect = "compose.jugcraft.alchemy.inspect.";
		BlockPos atA = new BlockPos(1, 2, 4);
		CrucibleBlockEntity a = crucible(helper, atA, Blocks.CAMPFIRE);
		ServerPlayer alembist = player(helper);
		ConcordanceProgress.grant(alembist, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(alembist, Alchemy.RESEARCH, ResearchState.UNDERSTOOD);
		a.setTemperature(120);
		work(a, alembist, PotionContents.createItemStack(Items.POTION, Potions.WATER));
		work(a, alembist, new ItemStack(Items.SPIDER_EYE));
		work(a, alembist, new ItemStack(Items.STICK));
		helper.assertTrue(a.mixture().history().size() == 3 && a.mixture().outcome(catalog).key().startsWith("minecraft:poison"),
				"A spider eye in water, stirred hot once, would poison: " + a.mixture());
		a.setItem(0, new ItemStack(Items.SUGAR, 3));
		BlockPos absA = helper.absolutePos(atA);
		Mixture mixtureA = a.mixture();
		int temperatureA = a.temperature();
		ServerPlayer reader = player(helper);
		SignWatch watch = new SignWatch(helper);

		// Anyone: what is seen, the slots, a spoon's sample and the risk, never the makeup.
		List<EncyclopediaPayload.Row> anyone = inspected(reader, absA);
		helper.assertTrue(anyone != null, "Anyone within reach reads the crucible");
		List<String> keys = allKeys(anyone);
		for (String wanted : List.of(inspect + "heat", inspect + "heated", inspect + "volume", inspect + "tank", inspect + "no_formula",
				"compose.jugcraft.alchemy.risk.harm", "block.minecraft.campfire", "encyclopedia.jugcraft.inspect.crucible.buffer",
				"encyclopedia.jugcraft.inspect.crucible.glass", "effect.minecraft.poison")) {
			helper.assertTrue(keys.contains(wanted), "Anyone is told " + wanted + ": " + keys);
		}
		helper.assertTrue(argsOf(anyone, inspect + "heated").size() == 2 && Integer.valueOf(120).equals(argsOf(anyone, inspect + "heated").get(1)),
				"The campfire beneath heats it towards 120: " + argsOf(anyone, inspect + "heated"));
		helper.assertValueEqual(factsUnder(anyone, inspect + "sample.spoon"), worded(Assay.read(mixtureA, temperatureA, Assay.SPOON, catalog)),
				"a spoon's sample");
		for (String hidden : List.of("compose.jugcraft.alchemy.assay.axis", "compose.jugcraft.alchemy.assay.contaminant", inspect + "thresholds",
				inspect + "operations", "compose.jugcraft.alchemy.reason", "compose.jugcraft.alchemy.too_weak", inspect + "preview")) {
			helper.assertTrue(!keys.contains(hidden), "Anyone is not told " + hidden);
		}
		helper.assertTrue(keys.stream().noneMatch(key -> key.startsWith("compose.jugcraft.diagnostic.")), "With no formula there is no reason: " + keys);

		// A glass without the research, or the research without a glass, reads as a spoon.
		reader.getInventory().add(new ItemStack(JugcraftConcordance.ASSAY_GLASS));
		helper.assertTrue(headingKeys(inspected(reader, absA)).contains(inspect + "sample.spoon"), "A glass without the Alembic Arts reads as a spoon");
		ConcordanceProgress.grant(reader, Alchemy.RESEARCH, ResearchState.UNDERSTOOD);
		reader.getInventory().clearContent();
		helper.assertTrue(headingKeys(inspected(reader, absA)).contains(inspect + "sample.spoon"), "The Alembic Arts without a glass read as a spoon");

		// Understood, with a glass: the glass's measures.
		reader.getInventory().add(new ItemStack(JugcraftConcordance.ASSAY_GLASS));
		List<EncyclopediaPayload.Row> glass = inspected(reader, absA);
		List<String> glassKeys = allKeys(glass);
		helper.assertValueEqual(factsUnder(glass, inspect + "sample.glass"), worded(Assay.read(mixtureA, temperatureA, Assay.GLASS, catalog)),
				"the glass's sample");
		helper.assertValueEqual(argsOf(glass, inspect + "operations"), List.<Object>of(3, Mixture.MAX_OPERATIONS), "three steps taken");
		helper.assertValueEqual(argsOf(glass, inspect + "thresholds"), List.<Object>of("0.30", "0.25", "0.50"),
				"its contaminant against the murky and Nausea thresholds");
		helper.assertTrue(!glassKeys.contains("compose.jugcraft.alchemy.reason") && !glassKeys.contains(inspect + "preview")
				&& !glassKeys.contains("encyclopedia.jugcraft.inspect.crucible.glass"), "No outcome yet, and no word about a glass: " + glassKeys);

		// Mastered, with a glass: what a dose would do; the glass put away, a spoon's reading again.
		ConcordanceProgress.grant(reader, Alchemy.RESEARCH, ResearchState.MASTERED);
		helper.assertValueEqual(factsUnder(inspected(reader, absA), inspect + "sample.mastered"),
				worded(Assay.read(mixtureA, temperatureA, Assay.MASTERED, catalog)), "a master's sample");
		reader.getInventory().clearContent();
		helper.assertTrue(headingKeys(inspected(reader, absA)).contains(inspect + "sample.spoon"), "A master without a glass reads as a spoon");
		watch.close();
		helper.assertTrue(watch.all().isEmpty(), "Reading shows no sign: " + watch.all());
		helper.assertTrue(a.mixture().equals(mixtureA) && a.temperature() == temperatureA && a.getItem(0).getCount() == 3,
				"Reading changes nothing: " + a.mixture());

		// A clean formula set over that mixture: the mixture is not its batch and can still be bottled by hand, so the
		// page names its Poison to anyone, though the formula's own dose would carry none (plan 03:54).
		ItemStack clean = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		clean.set(JugcraftConcordance.FORMULA, "water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot");
		helper.assertTrue(work(a, alembist, clean).consumesAction() && a.program() != null && a.step() == 0 && a.mixture().equals(mixtureA),
				"A clean formula is set over it");
		List<String> over = allKeys(inspected(player(helper), absA));
		helper.assertTrue(over.contains("compose.jugcraft.alchemy.risk.harm") && over.contains("effect.minecraft.poison"),
				"The page still names the mixture's Poison: " + over);

		// A formula: its step in words, the tank, and the risk of its outcome and its searing stir, to anyone.
		BlockPos atB = new BlockPos(3, 2, 4);
		CrucibleBlockEntity b = crucible(helper, atB, Blocks.STONE);
		ItemStack written = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		written.set(JugcraftConcordance.FORMULA, "water 1; add minecraft:spider_eye jugcraft:raw; stir searing");
		helper.assertTrue(work(b, alembist, written).consumesAction() && b.program() != null, "The formula is set");
		BlockPos absB = helper.absolutePos(atB);
		SignWatch second = new SignWatch(helper);
		ServerPlayer novice = player(helper);
		List<EncyclopediaPayload.Row> rows = inspected(novice, absB);
		helper.assertTrue(rows != null, "Anyone within reach reads it");
		List<String> novel = allKeys(rows);
		helper.assertTrue(novel.contains(inspect + "unheated") && novel.contains("block.minecraft.stone"), "Stone beneath heats nothing: " + novel);
		helper.assertValueEqual(argsOf(rows, inspect + "tank"), List.<Object>of(0, CrucibleBlockEntity.WATER_PER_BUCKET), "an empty tank");
		List<Object> step = argsOf(rows, inspect + "step");
		helper.assertTrue(step.size() == 3 && step.get(0).equals(1) && step.get(1).equals(3) && step.get(2) instanceof Component words
				&& key(words).equals("compose.jugcraft.alchemy.op.water"), "Step 1 of 3, in words: " + step);
		helper.assertValueEqual(argsOf(rows, "compose.jugcraft.alchemy.risk.searing"), List.<Object>of(3), "step 3 stirs it searing");
		helper.assertTrue(novel.contains("effect.minecraft.poison") && novel.contains("effect.minecraft.nausea"),
				"A dose would bring Poison and Nausea: " + novel);
		helper.assertTrue(!headingKeys(rows).contains(inspect + "preview"), "No outcome without a glass and mastery");
		reader.getInventory().add(new ItemStack(JugcraftConcordance.ASSAY_GLASS));
		helper.assertValueEqual(factsUnder(inspected(reader, absB), inspect + "preview"),
				worded(((Mixture.Applied) b.program().replay(catalog)).mixture().outcome(catalog).reasons()), "a master's preview of the formula");
		second.close();
		helper.assertTrue(second.all().isEmpty() && b.step() == 0, "Reading shows no sign and takes no step: " + second.all());

		// Past the reader's own reach, though within the anchor's 16 blocks, or not loaded: no page.
		BlockPos away = helper.absolutePos(new BlockPos(1, 2, -4));
		reader.snapTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5, 0.0F, 0.0F);
		helper.assertTrue(reader.distanceToSqr(Vec3.atCenterOf(absA)) <= 256 && !reader.isWithinBlockInteractionRange(absA, InspectPage.CRUCIBLE_REACH),
				"The reader stands within the anchor's reach but past their own");
		helper.assertTrue(inspected(reader, absA) == null, "A crucible past the reader's reach has no page");
		helper.assertTrue(inspected(reader, absA.offset(100_000, 0, 0)) == null, "nor one not loaded");

		// Its first pulse finds the tank empty: the page gives the crucible's own reason.
		helper.succeedWhen(() -> {
			Diagnostic reason = b.waiting();
			helper.assertTrue(reason != null, "Waiting for the formula's first pulse");
			List<EncyclopediaPayload.Row> now = inspected(novice, absB);
			String said = ComposeText.show(reason.text()).getString();
			helper.assertTrue(now != null && now.stream().anyMatch(row -> row.text().getString().equals(said)), "The page gives its reason: " + said);
		});
	}

	// ---------------------------------------------------------------- a Verdant Bed and its crop (Concordance Chunk C, C-07)

	/** The row judging one factor ("%s: %s", its first argument that factor's line), or null. */
	private static Component factorRow(List<EncyclopediaPayload.Row> rows, String factor) {
		for (EncyclopediaPayload.Row row : rows) {
			Object[] parts = args(row.text());
			if (key(row.text()).equals("compose.jugcraft.ecology.inspect.judged") && parts.length == 2 && parts[0] instanceof Component line
					&& key(line).equals("compose.jugcraft.ecology.inspect." + factor)) {
				return row.text();
			}
		}
		return null;
	}

	/** The arguments of a judged row's factor line (0) or judgement (1). */
	private static List<Object> part(Component judged, int which) {
		return judged == null ? List.of() : List.of(args((Component) args(judged)[which]));
	}

	/**
	 * C-07 (C-P14, C-P54): the Inspect page reads a Verdant Bed, and a crop through the bed below it, within the reader's own
	 * block reach: the bed's moisture and nutrients, the crop's step and pace, the verdict and each factor against the crop's
	 * niche with how far it reaches, the light as sky and lamps; the bed and the crop give one reading. A bare bed gives its
	 * readings unjudged; a dormant one says so and is not read. Reading shows no sign and changes nothing; past reach or not
	 * loaded, there is no page.
	 */
	@GameTest(maxTicks = 80)
	public void theInspectPageReadsABedAndACrop(GameTestHelper helper) {
		UUID keeper = new UUID(14L, 7L);
		BlockPos bedAt = new BlockPos(1, 1, 4);
		BlockPos cropAt = bedAt.above();
		helper.setBlock(bedAt, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
		VerdantBedBlockEntity bed = (VerdantBedBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(bedAt));
		bed.awaken(keeper);
		helper.setBlock(cropAt, Garden.SUNPETAL_CROP);
		helper.setBlock(cropAt.east(), Blocks.GLOWSTONE);
		BlockPos bareAt = new BlockPos(3, 1, 4);
		helper.setBlock(bareAt, Garden.VERDANT_BED);
		VerdantBedBlockEntity bareBed = (VerdantBedBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(bareAt));
		bareBed.awaken(keeper);
		BlockPos dormantAt = new BlockPos(5, 1, 4);
		helper.setBlock(dormantAt, Garden.VERDANT_BED);
		// Both areas are built now and never change: their samples are taken early, away from the other tests' sampling ticks.
		for (int tick : new int[] {10, 20, 30}) {
			helper.runAtTickTime(tick, () -> {
				bed.habitat(helper.getLevel());
				bareBed.habitat(helper.getLevel());
			});
		}
		helper.runAtTickTime(45, () -> {
			ServerLevel level = helper.getLevel();
			ServerPlayer reader = player(helper);
			helper.setBlock(cropAt, Garden.SUNPETAL_CROP); // a random tick may have grown it meanwhile
			bed.setNutrients(10);
			bed.note("waiting", List.of()); // so a page that noted its reading would be caught
			int moisture = bed.moisture();
			BlockPos absBed = helper.absolutePos(bedAt);
			BlockPos absCrop = helper.absolutePos(cropAt);
			SignWatch watch = new SignWatch(helper);
			List<EncyclopediaPayload.Row> onBed = inspected(reader, absBed);
			List<EncyclopediaPayload.Row> onCrop = inspected(reader, absCrop);
			helper.assertTrue(onBed != null && onCrop != null, "Anyone within reach reads a bed and the crop on it");
			helper.assertTrue(key((Component) argsOf(onBed, "encyclopedia.jugcraft.inspect.at").get(0)).equals("block.jugcraft.verdant_bed")
					&& key((Component) argsOf(onCrop, "encyclopedia.jugcraft.inspect.at").get(0)).equals("block.jugcraft.sunpetal_crop"),
					"Each page names what was looked at");
			helper.assertValueEqual(onCrop.stream().skip(1).map(row -> row.text().getString()).toList(),
					onBed.stream().skip(1).map(row -> row.text().getString()).toList(), "The bed and its crop give one reading");
			helper.assertValueEqual(argsOf(onCrop, "message.jugcraft.concordance.garden.bed"), List.<Object>of(moisture, 10, VerdantBedBlockEntity.CAPACITY),
					"the bed's moisture and nutrients");
			List<Object> step = argsOf(onCrop, "message.jugcraft.concordance.garden.crop");
			helper.assertTrue(step.size() == 4 && step.get(1).equals(0) && step.get(3) instanceof Component pace
					&& key(pace).equals("compose.jugcraft.ecology.growth.tolerating"), "Step 0, growing slowly: " + step);
			List<String> keys = allKeys(onCrop);
			helper.assertTrue(keys.contains("compose.jugcraft.ecology.inspect.verdict") && keys.contains("compose.jugcraft.ecology.growth.tolerating")
					&& headingKeys(onCrop).contains("encyclopedia.jugcraft.inspect.garden.habitat"), "the verdict, under the crop's needs: " + keys);
			List<Object> light = part(factorRow(onCrop, "light"), 0);
			helper.assertTrue(light.size() == 3 && light.get(0).equals(Math.max((Integer) light.get(1), (Integer) light.get(2)))
					&& light.get(0).equals(level.getRawBrightness(absCrop, 0)) && light.get(1).equals(level.getBrightness(LightLayer.SKY, absCrop))
					&& light.get(2).equals(level.getBrightness(LightLayer.BLOCK, absCrop)) && (Integer) light.get(2) >= 13,
					"The light as sky and lamps, the greater counting: " + light);
			helper.assertValueEqual(part(factorRow(onCrop, "light"), 1), List.<Object>of(13, 15, 11, 15), "against the sunpetal's light");
			helper.assertValueEqual(part(factorRow(onCrop, "diversity"), 0), List.<Object>of(1, Sampler.RADIUS), "alone, within 2 blocks");
			helper.assertTrue(key((Component) args(factorRow(onCrop, "diversity"))[1]).equals("compose.jugcraft.ecology.inspect.tolerable")
					&& part(factorRow(onCrop, "diversity"), 1).equals(List.<Object>of(3, 8, 1, 8)), "one kind is only tolerable");
			helper.assertValueEqual(part(factorRow(onCrop, "disturbance"), 0), List.<Object>of(0, 5, 5, Sampler.HEIGHT),
					"no working magic in the 5 by 5, a block above and below");
			List<String> bare = allKeys(inspected(reader, helper.absolutePos(bareAt)));
			helper.assertTrue(bare.contains("compose.jugcraft.ecology.inspect.unplanted") && bare.contains("compose.jugcraft.ecology.inspect.light")
					&& !bare.contains("compose.jugcraft.ecology.inspect.judged") && !bare.contains("compose.jugcraft.ecology.inspect.verdict"),
					"A bare bed gives its readings unjudged: " + bare);
			List<String> dormant = allKeys(inspected(reader, helper.absolutePos(dormantAt)));
			helper.assertTrue(dormant.contains("message.jugcraft.concordance.garden.dormant") && !dormant.contains("compose.jugcraft.ecology.inspect.light"),
					"A dormant bed says so and is not read: " + dormant);
			helper.setBlock(cropAt, ((OrganismCropBlock) Garden.SUNPETAL_CROP).getStateForAge(OrganismCropBlock.MAX_AGE));
			List<Object> ripe = argsOf(inspected(reader, absCrop), "message.jugcraft.concordance.garden.crop");
			helper.assertTrue(ripe.size() == 4 && ripe.get(3) instanceof Component word && key(word).equals("compose.jugcraft.ecology.mature"), "Ripe: " + ripe);
			watch.close();
			helper.assertTrue(watch.all().isEmpty() && bed.growth().equals("waiting") && bed.reasons().isEmpty() && bed.nutrients() == 10
					&& bed.moisture() == moisture, "Reading shows no sign and notes, takes and dries nothing: " + bed.growth());
			BlockPos away = helper.absolutePos(new BlockPos(1, 2, -4));
			reader.snapTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5, 0.0F, 0.0F);
			helper.assertTrue(reader.distanceToSqr(Vec3.atCenterOf(absBed)) <= 256 && !reader.isWithinBlockInteractionRange(absBed, InspectPage.GARDEN_REACH),
					"The reader stands within the anchor's reach but past their own");
			helper.assertTrue(inspected(reader, absBed) == null && inspected(reader, absCrop) == null, "A bed and a crop past reach have no page");
			helper.assertTrue(inspected(reader, absBed.offset(100_000, 0, 0)) == null, "nor one not loaded");
			helper.succeed();
		});
	}

	private static EncyclopediaPayload.Page plain(String id) {
		return new EncyclopediaPayload.Page(id, Component.literal(id), List.of());
	}

	/**
	 * C-09 (C-P65): the Inspect page names the graft a bed carries and each range it changes against its host's own, on the
	 * bed's page and on its host crop's, and judges the crop's factors by the grafted niche (the fifth reader); a plain bed's
	 * page names none; and a crop that is not the graft's host has none named on its page, judged by its own niche, though
	 * its bed's page names it.
	 */
	@GameTest(maxTicks = 80)
	public void theInspectPageNamesAGraft(GameTestHelper helper) {
		UUID keeper = new UUID(14L, 8L);
		BlockPos graftedAt = new BlockPos(2, 1, 2), plainAt = new BlockPos(2, 1, 5), otherAt = new BlockPos(5, 1, 2);
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos at : List.of(graftedAt, plainAt, otherAt)) {
			helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
			VerdantBedBlockEntity bed = (VerdantBedBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
			bed.awaken(keeper);
			bed.setNutrients(10);
			helper.setBlock(at.above(), ((OrganismCropBlock) (at.equals(otherAt) ? Garden.DEWMOSS_CROP : Garden.SUNPETAL_CROP)).getStateForAge(1));
			beds.add(bed);
		}
		beds.get(0).setGraft("jugcraft:dewlit");
		for (int tick : new int[] {10, 20, 30}) {
			helper.runAtTickTime(tick, () -> beds.forEach(bed -> bed.habitat(helper.getLevel())));
		}
		helper.runAtTickTime(46, () -> {
			for (BlockPos at : List.of(graftedAt, plainAt, otherAt)) { // re-wet and reset: the block entity stays
				helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
				helper.setBlock(at.above(), ((OrganismCropBlock) (at.equals(otherAt) ? Garden.DEWMOSS_CROP : Garden.SUNPETAL_CROP)).getStateForAge(1));
			}
			// Grafted only now, so no growth of the Dewmoss (another organism) could have cleared it.
			beds.get(2).setGraft("jugcraft:dewlit");
			ServerPlayer reader = player(helper);
			for (BlockPos at : List.of(graftedAt.above(), graftedAt)) {
				List<EncyclopediaPayload.Row> rows = inspected(reader, helper.absolutePos(at));
				helper.assertTrue(rows != null && headingKeys(rows).contains("compose.jugcraft.ecology.graft"), "The page at " + at + " names the graft");
				List<String> keys = allKeys(rows);
				helper.assertTrue(keys.containsAll(List.of("graft.jugcraft.dewlit", "item.jugcraft.sunpetal", "item.jugcraft.dewmoss")), "Dewlit: " + keys);
				List<List<Object>> ranges = rows.stream().filter(row -> key(row.text()).equals("compose.jugcraft.ecology.graft.range"))
						.map(row -> List.of(args(row.text())).subList(1, 9)).toList();
				helper.assertValueEqual(ranges, List.<List<Object>>of(List.<Object>of(4, 7, 5, 7, 2, 7, 4, 7), List.<Object>of(9, 15, 11, 15, 11, 15, 13, 15)),
						"each range against the Sunpetal's own");
				helper.assertValueEqual(part(factorRow(rows, "moisture"), 1), List.<Object>of(5, 7, 4, 7), "moisture judged by Dewlit's range");
			}
			List<EncyclopediaPayload.Row> plain = inspected(reader, helper.absolutePos(plainAt));
			helper.assertTrue(plain != null && !allKeys(plain).contains("compose.jugcraft.ecology.graft"), "An ungrafted bed names none");
			helper.assertValueEqual(part(factorRow(plain, "moisture"), 1), List.<Object>of(4, 7, 2, 7), "and judges by the Sunpetal's own");
			List<EncyclopediaPayload.Row> otherCrop = inspected(reader, helper.absolutePos(otherAt.above()));
			List<EncyclopediaPayload.Row> otherBed = inspected(reader, helper.absolutePos(otherAt));
			helper.assertTrue(otherCrop != null && !allKeys(otherCrop).contains("compose.jugcraft.ecology.graft")
					&& otherBed != null && headingKeys(otherBed).contains("compose.jugcraft.ecology.graft"),
					"A Dewmoss on a Dewlit bed: its page names no graft, as Jade names none; its bed's page names it");
			helper.assertValueEqual(part(factorRow(otherCrop, "moisture"), 1), List.<Object>of(7, 7, 5, 7), "the Dewmoss judged by its own niche");
			helper.succeed();
		});
	}

	/** The client's page model: the first answer opens on its first page, stale answers are ignored, the page read is kept, Back and turns. */
	@GameTest(maxTicks = 20)
	public void theNavigatorKeepsItsPlace(GameTestHelper helper) {
		Navigator navigator = new Navigator();
		EncyclopediaPayload.Page discover = plain("discover");
		EncyclopediaPayload.Page spellcraft = plain("spellcraft");
		EncyclopediaPayload.Page keys = plain("keys");
		navigator.local(List.of(), List.of(keys));
		helper.assertTrue(!navigator.received() && navigator.current() == keys, "before an answer only the client's own page");
		helper.assertTrue(navigator.receive(1, List.of(discover, spellcraft), List.of(keys), null) && navigator.current() == discover,
				"the first answer opens on its first page, not the one the client made");
		helper.assertTrue(navigator.go("spellcraft") && navigator.current() == spellcraft && navigator.canGoBack(), "a link turns the page");
		helper.assertFalse(navigator.receive(0, List.of(discover), List.of(keys), null), "an older answer is ignored");
		helper.assertTrue(navigator.current() == spellcraft, "and changes nothing");
		helper.assertTrue(navigator.receive(2, List.of(discover, spellcraft), List.of(keys), null) && navigator.current() == spellcraft, "a refresh keeps the page");
		helper.assertTrue(navigator.back() && navigator.current() == discover && !navigator.canGoBack(), "Back returns to the page left");
		helper.assertTrue(navigator.turn(-1) && navigator.current() == keys && navigator.turn(1) && navigator.current() == discover, "turns wrap round");
		navigator.go("spellcraft");
		helper.assertTrue(navigator.receive(3, List.of(discover), List.of(keys), "keys") && navigator.current() == keys,
				"a page no longer sent falls back to the one preferred");
		navigator.reset();
		helper.assertTrue(!navigator.received() && navigator.pages().isEmpty() && navigator.current() == null, "forgotten on leaving");
		helper.succeed();
	}

	// ---------------------------------------------------------------- a rite's readiness (Concordance Chunk C, C-03)

	/** A stone floor and a complete Lesser Circle round an anchor at (3, 2, 3), its pylons full (Concordance Chunk C, C-03). */
	private static CircleAnchorBlockEntity circle(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		BlockPos anchor = new BlockPos(3, 2, 3);
		helper.setBlock(anchor, JugcraftConcordance.CIRCLE_ANCHOR);
		for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").parts()) {
			BlockPos at = anchor.offset(part.offset().x(), part.offset().y(), part.offset().z());
			if (part.role() == StructurePattern.Role.CHANNEL) {
				helper.setBlock(at, JugcraftConcordance.LEY_PYLON);
				((LeyPylonBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at))).setLey(helper.getLevel(), 64);
			} else if (part.role() == StructurePattern.Role.BOUNDARY) {
				helper.setBlock(at, JugcraftConcordance.WARDING_STONE);
			}
		}
		return (CircleAnchorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(anchor));
	}

	/** The rows after the heading keyed {@code headingKey}, up to the next heading. */
	private static List<EncyclopediaPayload.Row> rowsUnder(List<EncyclopediaPayload.Row> rows, String headingKey) {
		List<EncyclopediaPayload.Row> out = new ArrayList<>();
		boolean in = false;
		for (EncyclopediaPayload.Row row : rows) {
			if (row.kindOf() == EncyclopediaPayload.Kind.HEADING) {
				in = key(row.text()).equals(headingKey);
			} else if (in) {
				out.add(row);
			}
		}
		return out;
	}

	private static List<String> stageKeys(List<EncyclopediaPayload.Row> rows) {
		return rows.stream().map(row -> key(row.text()).replace("compose.jugcraft.ritual.stage.", "")).toList();
	}

	/**
	 * C-03 (plan 03:107, 03:122): looking at an anchor whose offerings answer a rite the reader knows, the Inspect page
	 * names the rite, says nothing is missing, and states its stages and what each pays, from its data (the attunement's
	 * transform, the vigil's gathering and effects), backlash included. It shows no sign.
	 */
	@GameTest(maxTicks = 40)
	public void theInspectPageShowsARitesReadinessAndPayments(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SignWatch signs = new SignWatch(helper);
		CircleAnchorBlockEntity anchor = circle(helper);
		ServerPlayer reader = player(helper);
		BlockPos stand = helper.absolutePos(new BlockPos(4, 2, 4));
		reader.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.grant(reader, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(reader, "jugcraft:circle_lore", ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(reader, 20);
		for (ItemStack stack : List.of(new ItemStack(JugcraftConcordance.INITIATE_WAND), new ItemStack(Items.AMETHYST_SHARD, 4),
				new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.GLOWSTONE_DUST, 4))) {
			anchor.offer(reader, stack);
		}
		List<EncyclopediaPayload.Row> rows = inspected(reader, anchor.getBlockPos());
		helper.assertValueEqual(headingKeys(rows), List.of("encyclopedia.jugcraft.inspect.at", "encyclopedia.jugcraft.inspect.readiness",
				"encyclopedia.jugcraft.inspect.stages", "encyclopedia.jugcraft.inspect.rituals"), "the anchor, its rite's readiness and payments");
		EncyclopediaPayload.Row named = row(rows, "encyclopedia.jugcraft.inspect.readiness");
		List<EncyclopediaPayload.Row> ready = rowsUnder(rows, "encyclopedia.jugcraft.inspect.readiness");
		helper.assertTrue(key((Component) args(named.text())[0]).equals("ritual.jugcraft.adept_attunement") && ready.size() == 1
				&& key(ready.get(0).text()).equals("encyclopedia.jugcraft.inspect.readiness.ready"), "nothing is missing");
		RitualDefinition attunement = ConcordanceData.rules().ritual("jugcraft:adept_attunement");
		List<EncyclopediaPayload.Row> stages = rowsUnder(rows, "encyclopedia.jugcraft.inspect.stages");
		helper.assertValueEqual(stages.stream().map(row -> row.text().getString()).toList(),
				Readiness.stages(attunement, 4).stream().map(line -> ComposeText.show(line).getString()).toList(), "the attunement's table");
		helper.assertValueEqual(stageKeys(stages), List.of("offer", "begin", "steps", "commit.transform", "complete", "broken", "backlash"), "its stages");
		Object[] commit = args(stages.get(3).text());
		helper.assertTrue(List.of(args(stages.get(1).text())).equals(List.of(6)) && List.of(args(stages.get(2).text())).equals(List.of(5, "2", 2, 4, 8, 6, 8))
				&& List.of(commit[0], commit[2], commit[3]).equals(List.of(5, 6, 40)) && key((Component) commit[1]).equals("item.jugcraft.adept_wand")
				&& List.of(args(stages.get(6).text())).equals(List.of(4)), "its payments, from its data");
		anchor.use(reader, level, true); // takes the attunement's offerings back
		anchor.offer(reader, new ItemStack(Items.GLOW_BERRIES, 4));
		rows = inspected(reader, anchor.getBlockPos());
		RitualDefinition vigil = ConcordanceData.rules().ritual("jugcraft:lumen_vigil");
		stages = rowsUnder(rows, "encyclopedia.jugcraft.inspect.stages");
		helper.assertTrue(key((Component) args(row(rows, "encyclopedia.jugcraft.inspect.readiness").text())[0]).equals("ritual.jugcraft.lumen_vigil"),
				"the vigil's readiness");
		helper.assertValueEqual(stages.stream().map(row -> row.text().getString()).toList(),
				Readiness.stages(vigil, 4).stream().map(line -> ComposeText.show(line).getString()).toList(), "the vigil's table");
		helper.assertValueEqual(stageKeys(stages), List.of("offer", "begin", "gather", "steps", "commit.effects", "broken", "backlash"), "with its gathering");
		helper.assertTrue(List.of(args(stages.get(1).text())).equals(List.of(4)) && List.of(args(stages.get(2).text())).equals(List.of(1, 30, 4, 8))
				&& List.of(args(stages.get(3).text())).equals(List.of(3, "2", 1, 4, 4, 8, 4)) && List.of(args(stages.get(4).text())).equals(List.of(3, 8, 12))
				&& List.of(args(stages.get(6).text())).equals(List.of(2)), "the vigil's payments");
		signs.close();
		helper.assertTrue(signs.all().isEmpty(), "The page shows no sign: " + signs.all());
		helper.succeed();
	}

	/**
	 * The bestiary (docs/features/bestiary-field-notes.md): a newcomer's Expeditions page has no field notes; once they
	 * have defeated a Jugcraft creature (and been defeated by it), its notes give its own numbers and their tally.
	 */
	@GameTest
	public void theBestiaryListsOnlyCreaturesFought(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		helper.assertTrue(Bestiary.fought(player).isEmpty(), "A newcomer has fought " + Bestiary.fought(player));
		helper.assertFalse(keys(build(player)).contains("encyclopedia.jugcraft.bestiary.heading"), "A newcomer sees field notes");
		EntityType<?> creature = BuiltInRegistries.ENTITY_TYPE.stream()
				.filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Jugcraft.MOD_ID))
				.filter(type -> type.getCategory() != MobCategory.MISC && DefaultAttributes.hasSupplier(type))
				.findFirst().orElseThrow(() -> helper.assertionException("Jugcraft has no living creature"));
		player.awardStat(Stats.ENTITY_KILLED.get(creature), 3);
		player.awardStat(Stats.ENTITY_KILLED_BY.get(creature), 1);
		List<Bestiary.Entry> fought = Bestiary.fought(player);
		helper.assertTrue(fought.size() == 1 && fought.getFirst().type() == creature && fought.getFirst().defeated() == 3
				&& fought.getFirst().defeatedBy() == 1, "The bestiary counts " + fought);
		EncyclopediaPayload.Page expedition = page(build(player), "expedition");
		helper.assertTrue(expedition != null, "No Expeditions page after a fight");
		List<String> rows = new ArrayList<>();
		expedition.rows().forEach(row -> keys(row.text(), rows));
		helper.assertTrue(rows.contains("encyclopedia.jugcraft.bestiary.heading") && rows.contains("encyclopedia.jugcraft.bestiary.record")
				&& rows.contains(creature.getDescriptionId()), "The field notes show " + rows);
		helper.succeed();
	}
}
