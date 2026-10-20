package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
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
import io.github.jimbozoomer.jugcraft.lair.LairDossiers;
import io.github.jimbozoomer.jugcraft.lair.LairVisit;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.LastRites;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Concordance Chunk A, A-04: the Jugcraft Encyclopedia's server side on a real server. A newcomer sees only the first
 * steps and nothing they have not met; the First Light route follows the player's research step by step; every recipe a
 * page shows is one this server has, and one a route needs but the server lacks is said to be unavailable; switched-off
 * features have no page; the Spellcraft page shows what is held; a lair is hidden until its token is carried and its
 * keeper until she is met; the route names only sales a town makes; the pages survive the trip to the client; requests
 * are rate-limited and the Inspect page reads only a near, loaded block; and the client's page model keeps its place.
 */
public class EncyclopediaGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
		helper.assertValueEqual(payload.pages().stream().map(EncyclopediaPayload.Page::id).toList(), List.of("discover"), "a newcomer's pages");
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

	private static EncyclopediaPayload.Page plain(String id) {
		return new EncyclopediaPayload.Page(id, Component.literal(id), List.of());
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
