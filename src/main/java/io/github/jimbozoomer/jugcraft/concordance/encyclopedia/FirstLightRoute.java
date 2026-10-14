package io.github.jimbozoomer.jugcraft.concordance.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberGear;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.EdgeKind;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.Link;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.StepState;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.LairDossiers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The encyclopedia's Discover page (Concordance Chunk A, A-04): the First Light route, from finding something that glows
 * to a spell of the player's own, and on to a ward for a friend and, if they like, a lair's keeper. Each step says
 * where it stands for this player (done, next, can be done now, later, or not available on this server) and how much it
 * matters (required, recommended, or optional after a boss); a step the player can act on lists its ways: the server's
 * own recipes as crafting grids, sales in the town if one stands, and codex entries the player has opened. The numbers
 * (how dark, how many, what a spell costs) are read from the loaded rules, so a data pack that changes them changes the
 * page; a bench study's length is the bench's own. The journal's research and stage reports end it.
 * <p>
 * Nothing is named before it is met: words, invocations and spells appear only on steps that need First Light
 * understood (Kindle is named as the cast that completes its step only then), a lair only once the player has carried
 * its token or met its keeper, and the stage report counts a route through research the player has not met without
 * naming it (A-D9). It only reads.
 * <p>
 * The step ids, their order and the recipes each names are pinned by tools/check_mod_data.py (check_encyclopedia) to
 * tools/encyclopedia.py's FIRST_ROUTE, which writes the route table in docs/features/arcane-concordance-first-route.md.
 */
public final class FirstLightRoute {
	public static final String PAGE = "discover";
	public static final String FIRST_LIGHT = "jugcraft:first_light";
	/** Cast Kindle with an instrument (awarded when the first cast settles). */
	public static final Identifier FIRST_KINDLE = Jugcraft.id("concordance_first_kindle");
	/** The invocation whose cast completes the cast step. */
	public static final String KINDLE = "jugcraft:kindle";
	/** Spell Engine's spell keys: the use key, then the hotbar keys 2 to 9. */
	private static final int SPELL_KEYS = 9;
	public static final String LIGHT_EXAMPLE = "touch struck light";
	/** The route's support spell (A-D13): it wards the friend touched, which a lair fight credits. */
	public static final String WARD_EXAMPLE = "touch struck ward";
	/** The word a support spell needs, and the one that would turn it on its caster. */
	public static final String WARD_WORD = "ward";
	public static final String SELF_WORD = "here";
	/** The most invocations named under the cast step (the rest are counted). */
	public static final int MAX_INVOCATIONS = 8;
	/** The most specimens shown as icons under the find step. */
	public static final int MAX_SPECIMENS = 9;
	/** The first win over each lair's keeper (VesperineLoot and TatterlaceLoot ADVANCEMENT). */
	private static final List<Identifier> FIRST_WINS = List.of(Jugcraft.id("the_last_harvest"), Jugcraft.id("unravelled"));

	private FirstLightRoute() {
	}

	/** A step of the route: how much it matters, whether it is done, the steps it waits for, and whether this server lacks its way. */
	private record Step(String id, EdgeKind importance, boolean done, List<String> after, boolean blocked) {
	}

	public static void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return;
		}
		ConcordanceRules rules = ConcordanceData.rules();
		Definitions.Research research = rules.research(FIRST_LIGHT);
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		if (research == null) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.missing"));
		} else {
			steps(player, out, rules, research, knowledge);
		}
		out.heading(Component.translatable("encyclopedia.jugcraft.discover.research"));
		Journal.research(player, out.report());
		out.heading(Component.translatable("encyclopedia.jugcraft.discover.stage"));
		StageProgress.describe(player, out::fact, id -> knowledge.state(id) != ResearchState.NONE);
		out.link(Component.translatable("encyclopedia.jugcraft.link.journal", Component.keybind("key.jugcraft.concordance_journal")), Link.journal());
	}

	private static void steps(ServerPlayer player, PageWriter out, ConcordanceRules rules, Definitions.Research research, Knowledge knowledge) {
		ResearchState state = knowledge.state(FIRST_LIGHT);
		boolean understood = state.atLeast(ResearchState.UNDERSTOOD);
		Identifier wandRecipe = Jugcraft.id("initiate_wand");
		List<Inscription> inscriptions = inscriptions(player);
		boolean lairs = JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)
				&& (out.present(Jugcraft.id("death_knell")) || out.present(Jugcraft.id("cursed_spindle")));
		boolean instrument = carries(player, stack -> stack.is(JugcraftConcordance.INSTRUMENTS));

		List<Step> route = new ArrayList<>();
		route.add(new Step("find", EdgeKind.REQUIRED, state.atLeast(ResearchState.ENCOUNTERED)
				|| carries(player, stack -> stack.is(JugcraftConcordance.LUMINOUS)), List.of(), false));
		route.add(new Step("examine", EdgeKind.REQUIRED, state.atLeast(ResearchState.ENCOUNTERED), List.of("find"), false));
		route.add(new Step("dark", EdgeKind.REQUIRED, state.atLeast(ResearchState.OBSERVED), List.of("examine"), false));
		route.add(new Step("understand", EdgeKind.REQUIRED, understood, List.of("dark"), false));
		route.add(new Step("wand", EdgeKind.REQUIRED, instrument, List.of(), !out.present(wandRecipe)));
		route.add(new Step("cast", EdgeKind.REQUIRED, done(player, FIRST_KINDLE), List.of("understand", "wand"), false));
		route.add(new Step("compose", EdgeKind.REQUIRED, !inscriptions.isEmpty(), List.of("understand", "wand"), false));
		route.add(new Step("support", EdgeKind.RECOMMENDED, inscriptions.stream().anyMatch(FirstLightRoute::supports), List.of("compose"), false));
		route.add(new Step("equip", EdgeKind.RECOMMENDED, equipped(player), List.of("understand"), false));
		if (lairs) {
			route.add(new Step("lair", EdgeKind.OPTIONAL_BOSS, FIRST_WINS.stream().anyMatch(id -> done(player, id)), List.of("compose"), false));
		}
		Map<String, StepState> states = states(route);

		out.heading(Component.translatable("encyclopedia.jugcraft.route.heading", ConcordanceProgress.researchName(FIRST_LIGHT)));
		Map<ResearchState, List<EvidenceRule>> ways = research.states();
		List<Item> specimens = specimens(ways.get(ResearchState.ENCOUNTERED));
		if (route(out, "find", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.find"), null, specimens)) {
			for (Item specimen : specimens) {
				out.sales(specimen);
			}
			out.codex("radiance/first_light", null, null);
		}

		if (route(out, "examine", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.examine",
				Component.keybind("key.sneak"), Component.keybind("key.use")), null, List.of())) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.examine.air"));
		}

		int dark = darkest(ways.get(ResearchState.OBSERVED));
		if (route(out, "dark", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.dark", dark),
				light(player, knowledge), List.of())) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.dark.where"));
		}

		if (route(out, "understand", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.understand",
				ConcordanceProgress.researchName(FIRST_LIGHT)), null, List.of())) {
			understanding(out, ways.get(ResearchState.UNDERSTOOD), knowledge);
		}

		Component wand = JugcraftConcordance.INITIATE_WAND.getDefaultInstance().getHoverName();
		if (route(out, "wand", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.wand", wand), null,
				List.of(JugcraftConcordance.INITIATE_WAND))) {
			out.recipe(EdgeKind.REQUIRED, Jugcraft.id("initiate_wand"), Component.translatable("encyclopedia.jugcraft.route.make", wand));
			out.codex("foundations/initiate_wand", null, null);
		}

		// Kindle is named only once First Light is understood (it is learned then); before, the step is any invocation.
		boolean kindle = understood && knowledge.invocations().containsKey(KINDLE);
		Component castText = kindle ? Component.translatable("encyclopedia.jugcraft.route.cast.kindle", Component.translatable(Invocations.nameKey(KINDLE)))
				: Component.translatable("encyclopedia.jugcraft.route.cast");
		if (route(out, "cast", states, EdgeKind.REQUIRED, castText, null, List.of())) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.cast.how", Component.keybind("key.use"),
					Component.keybind("key.hotbar.2"), Component.keybind("key.hotbar.3")));
			// The spell bar offers the invocations in the order they are known (ConcordanceSpells.containers): the use key
			// casts the first, and the hotbar keys 2 to 9 the next ones.
			int index = new ArrayList<>(knowledge.invocations().keySet()).indexOf(KINDLE);
			if (kindle && index < SPELL_KEYS) {
				out.fact(Component.translatable("encyclopedia.jugcraft.route.cast.goal", Component.translatable(Invocations.nameKey(KINDLE)),
						Component.keybind(index == 0 ? "key.use" : "key.hotbar." + (index + 1))));
			}
			invocations(out, knowledge);
			out.codex("invocations/kindle", "jugcraft:first_light", ResearchState.UNDERSTOOD);
		}

		Instrument held = example(player, rules);
		if (route(out, "compose", states, EdgeKind.REQUIRED, Component.translatable("encyclopedia.jugcraft.route.compose"), null, List.of())) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.compose.how", Component.keybind("key.jugcraft.concordance_journal"),
					Component.translatable("screen.jugcraft.journal.compose")));
			out.fact(Component.translatable("encyclopedia.jugcraft.route.compose.example", LIGHT_EXAMPLE), cost(player, held, LIGHT_EXAMPLE), List.of());
			out.link(Component.translatable("encyclopedia.jugcraft.link.composer"), Link.composer());
			out.codex("composition/composing", "jugcraft:first_light", ResearchState.UNDERSTOOD);
		}

		if (route(out, "support", states, EdgeKind.RECOMMENDED, Component.translatable("encyclopedia.jugcraft.route.support"), null, List.of())) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.support.example", WARD_EXAMPLE), cost(player, held, WARD_EXAMPLE), List.of());
			if (lairs) {
				// Only where lairs open: the boss's reward is what a ward for a friend earns.
				out.fact(Component.translatable("encyclopedia.jugcraft.route.support.credit", number(Encounter.WARDING),
						SELF_WORD + " struck " + WARD_WORD));
			}
		}

		if (route(out, "equip", states, EdgeKind.RECOMMENDED, Component.translatable("encyclopedia.jugcraft.route.equip"), null, List.of())) {
			equipment(out, rules);
		}

		if (lairs && route(out, "lair", states, EdgeKind.OPTIONAL_BOSS, Component.translatable("encyclopedia.jugcraft.route.lair"), null, List.of())) {
			boolean found = false;
			for (LairDossiers.Boss boss : LairDossiers.Boss.values()) {
				LairDossiers.Discovery discovery = LairDossiers.discovery(player, boss);
				if (discovery == LairDossiers.Discovery.UNKNOWN) {
					continue;
				}
				// Expeditions shows a lair's rite once its token is carried, and its keeper only once she is met.
				out.fact(Component.translatable(discovery == LairDossiers.Discovery.RITE ? "encyclopedia.jugcraft.route.lair.known_rite"
						: "encyclopedia.jugcraft.route.lair.known", LairDossiers.name(boss.lair)));
				found = true;
			}
			if (found) {
				out.link(Component.translatable("encyclopedia.jugcraft.link.page.expedition"), Link.page(LairDossiers.PAGE));
			} else {
				out.fact(Component.translatable("encyclopedia.jugcraft.route.lair.unknown"));
			}
		}
	}

	/**
	 * Where each step stands: done; else waiting for a step before it (later); else not available on this server
	 * (blocked); else the first required step that can be taken is next (or, when none can, the first recommended one)
	 * and the rest can be done now. A step after a boss is never next.
	 */
	private static Map<String, StepState> states(List<Step> route) {
		Map<String, Boolean> done = new HashMap<>();
		route.forEach(step -> done.put(step.id(), step.done()));
		Predicate<Step> ready = step -> !step.done() && !step.blocked() && step.after().stream().allMatch(id -> done.getOrDefault(id, false));
		String next = route.stream().filter(ready).filter(step -> step.importance() == EdgeKind.REQUIRED).map(Step::id).findFirst()
				.orElseGet(() -> route.stream().filter(ready).filter(step -> step.importance() == EdgeKind.RECOMMENDED).map(Step::id)
						.findFirst().orElse(""));
		Map<String, StepState> states = new HashMap<>();
		for (Step step : route) {
			StepState state;
			if (step.done()) {
				state = StepState.DONE;
			} else if (!step.after().stream().allMatch(id -> done.getOrDefault(id, false))) {
				state = StepState.LATER;
			} else if (step.blocked()) {
				state = StepState.BLOCKED;
			} else {
				state = step.id().equals(next) ? StepState.NEXT : StepState.OPEN;
			}
			states.put(step.id(), state);
		}
		return states;
	}

	/**
	 * Writes step {@code id}'s row, as {@link #states} placed it; returns whether the player can act on it now (next,
	 * can be done now, or blocked here), when its ways follow.
	 */
	private static boolean route(PageWriter out, String id, Map<String, StepState> states, EdgeKind importance, Component text,
			@Nullable Component exact, List<Item> icons) {
		StepState state = states.getOrDefault(id, StepState.LATER);
		out.step(state, importance, text, exact, icons);
		return state == StepState.NEXT || state == StepState.OPEN || state == StepState.BLOCKED;
	}

	/**
	 * The ways to understanding, from the loaded rules: each alternative, with how far the player has come. Any one of
	 * them is enough, so none is called required when there are several.
	 */
	private static void understanding(PageWriter out, @Nullable List<EvidenceRule> rules, Knowledge knowledge) {
		Map<String, Long> evidence = knowledge.progress(FIRST_LIGHT).evidence();
		List<EvidenceRule> ways = rules == null ? List.of() : rules;
		EdgeKind sole = ways.size() == 1 ? EdgeKind.REQUIRED : EdgeKind.RECOMMENDED;
		if (ways.size() > 1) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.understand.any"));
		}
		for (EvidenceRule rule : ways) {
			int have = Math.min(rule.progress(evidence, ConcordanceProgress.TAG_LOOKUP), rule.distinct());
			Component exact = Component.translatable("journal.jugcraft.rule." + rule.kind().id, have, rule.distinct());
			switch (rule.kind()) {
				case EXAMINE -> out.edge(sole, Component.translatable("encyclopedia.jugcraft.route.understand.examine", rule.distinct(),
						rule.maxLight() == null ? Examination.DARK_LIGHT : rule.maxLight()), exact, List.of());
				case STUDY -> {
					Item station = item(rule.station());
					Component name = station == null ? Component.literal(String.valueOf(rule.station())) : station.getDefaultInstance().getHoverName();
					out.edge(EdgeKind.RECOMMENDED, Component.translatable("encyclopedia.jugcraft.route.understand.study", name,
							LampwrightBenchBlockEntity.STUDY_TICKS / 20), exact, station == null ? List.of() : List.of(station));
					Identifier recipe = rule.station() == null ? null : Identifier.tryParse(rule.station());
					if (recipe != null) {
						out.recipe(EdgeKind.RECOMMENDED, recipe, Component.translatable("encyclopedia.jugcraft.route.make", name));
					}
					out.codex("radiance/lampwright_bench", null, null);
				}
				case NOTES -> {
					out.edge(EdgeKind.TRADE, Component.translatable("encyclopedia.jugcraft.route.understand.notes"), exact,
							List.of(JugcraftConcordance.RESEARCH_NOTES_ITEM));
					out.codex("foundations/research_notes", null, null);
				}
				default -> out.edge(sole, Component.translatable("encyclopedia.jugcraft.route.understand.other"), exact, List.of());
			}
		}
	}

	/** The invocations the player may cast, with what each costs them; the rest of the Concordance's are only counted. */
	private static void invocations(PageWriter out, Knowledge knowledge) {
		int shown = 0;
		for (Map.Entry<String, Integer> invocation : knowledge.invocations().entrySet()) {
			if (shown >= MAX_INVOCATIONS) {
				break;
			}
			out.fact(Component.translatable("encyclopedia.jugcraft.route.cast.invocation", Component.translatable(Invocations.nameKey(invocation.getKey())),
					invocation.getValue()));
			shown++;
		}
		if (knowledge.invocations().size() > shown) {
			out.fact(Component.translatable("encyclopedia.jugcraft.route.cast.more", knowledge.invocations().size() - shown));
		}
	}

	/** The first loadout (A-D10): the Kindled Lantern, the two fire sets, the Lesser Focus of Fire and the Fire Bangle. */
	private static void equipment(PageWriter out, ConcordanceRules rules) {
		Definitions.Working lantern = rules.workings().get("jugcraft:kindle_lantern");
		Item kindled = lantern == null ? null : item(lantern.result());
		Item station = lantern == null ? null : item(lantern.station());
		Item work = lantern == null ? null : item(lantern.work());
		Item specimen = lantern == null || lantern.specimen() == null ? null : item(lantern.specimen());
		if (kindled != null && station != null && work != null && specimen != null) {
			out.edge(EdgeKind.RECOMMENDED, Component.translatable("encyclopedia.jugcraft.route.equip.lantern", name(kindled), name(station), name(work),
					name(specimen), ConcordanceProgress.researchName(lantern.research())), null, List.of(kindled, work, specimen));
			out.sales(work);
		}
		set(out, "pyromancers", List.of("pyromancers_hat", "pyromancers_robes", "pyromancers_leggings", "pyromancers_boots"));
		set(out, "pyromaniacs", List.of("pyromaniacs_hood", "pyromaniacs_tunic", "pyromaniacs_pants", "pyromaniacs_shoes"));
		out.recipe(EdgeKind.RECOMMENDED, Jugcraft.id("lesser_fire_focus"), Component.translatable("encyclopedia.jugcraft.route.equip.lesser_fire_focus"));
		out.recipe(EdgeKind.RECOMMENDED, Jugcraft.id("fire_bangle"), Component.translatable("encyclopedia.jugcraft.route.equip.fire_bangle"));
	}

	/** A fire set, named only when this server has every piece's recipe (its grids are the recipe book's and JEI's). */
	private static void set(PageWriter out, String set, List<String> pieces) {
		List<Item> items = new ArrayList<>();
		for (String piece : pieces) {
			Item item = EmberGear.ARMOR.get(piece);
			if (item == null || !out.present(Jugcraft.id(piece))) {
				return;
			}
			items.add(item);
		}
		out.edge(EdgeKind.RECOMMENDED, Component.translatable("encyclopedia.jugcraft.route.equip." + set), null, items);
	}

	/** The specimens a rule counts (listed items, then each tag's), at most {@value #MAX_SPECIMENS}. */
	private static List<Item> specimens(@Nullable List<EvidenceRule> rules) {
		Set<Item> out = new LinkedHashSet<>();
		for (EvidenceRule rule : rules == null ? List.<EvidenceRule>of() : rules) {
			for (String id : rule.specimens().items()) {
				Item item = item(id);
				if (item != null) {
					out.add(item);
				}
			}
			for (String tag : rule.specimens().tags()) {
				Identifier id = Identifier.tryParse(tag);
				if (id != null) {
					out.addAll(PageWriter.items(TagKey.create(Registries.ITEM, id), MAX_SPECIMENS));
				}
			}
		}
		return out.stream().limit(MAX_SPECIMENS).toList();
	}

	/** The darkest light an observation allows, from the rules (an examination at this light or less is in the dark). */
	private static int darkest(@Nullable List<EvidenceRule> rules) {
		for (EvidenceRule rule : rules == null ? List.<EvidenceRule>of() : rules) {
			if (rule.kind() == EvidenceRule.Kind.EXAMINE && rule.maxLight() != null) {
				return rule.maxLight();
			}
		}
		return Examination.DARK_LIGHT;
	}

	/**
	 * The light at the player's eyes now, read as an examination reads it, and the darkest light they have examined a
	 * specimen of First Light at so far.
	 */
	private static Component light(ServerPlayer player, Knowledge knowledge) {
		int now = player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition()));
		long darkest = Long.MAX_VALUE;
		for (Map.Entry<String, Long> evidence : knowledge.progress(FIRST_LIGHT).evidence().entrySet()) {
			if (evidence.getKey().startsWith(EvidenceRule.Kind.EXAMINE.id + ":")) {
				darkest = Math.min(darkest, evidence.getValue());
			}
		}
		return darkest == Long.MAX_VALUE ? Component.translatable("encyclopedia.jugcraft.route.dark.light_none", now)
				: Component.translatable("encyclopedia.jugcraft.route.dark.light", now, darkest);
	}

	/** What a spell would cost this player on {@code instrument}, compiled under the server's rules; null if it does not compile. */
	private static @Nullable Component cost(ServerPlayer player, @Nullable Instrument instrument, String text) {
		if (instrument == null) {
			return null;
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, instrument, text);
		Plan plan = compiled.plan();
		return plan == null ? null : Component.translatable("compose.jugcraft.inscription_cost", plan.focus(), Text.seconds(plan.cooldown()));
	}

	/** The instrument the examples are worked out for: the one in hand, or the Initiate's Wand. */
	private static @Nullable Instrument example(ServerPlayer player, ConcordanceRules rules) {
		Instrument held = ComposedSpells.instrument(player);
		return held != null ? held : rules.catalog().instrumentFor(BuiltInRegistries.ITEM.getKey(JugcraftConcordance.INITIATE_WAND).toString());
	}

	/** Every inscription on an instrument the player carries. */
	private static List<Inscription> inscriptions(ServerPlayer player) {
		List<Inscription> out = new ArrayList<>();
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			Inscription inscription = stack.is(JugcraftConcordance.INSTRUMENTS) ? ComposedSpells.inscription(stack) : null;
			if (inscription != null) {
				out.add(inscription);
			}
		}
		return out;
	}

	/**
	 * Whether an inscription is a support spell: some part of it wards (the word with or without modifiers, such as
	 * {@code ward+prolong}, at any level of a {@code then} chain) and its first delivery is not {@code here}, which
	 * turns the spell on its caster (a later level's {@code here} takes the first level's target).
	 */
	static boolean supports(Inscription inscription) {
		boolean first = true;
		boolean self = false;
		boolean ward = false;
		for (String token : inscription.text().toLowerCase(Locale.ROOT).trim().split("\\s+")) {
			String word = token.split("\\+", 2)[0];
			if (word.equals("then")) {
				first = false;
			} else if (word.equals(WARD_WORD)) {
				ward = true;
			} else if (first && word.equals(SELF_WORD)) {
				self = true;
			}
		}
		return ward && !self;
	}

	/** Whether the player carries or wears a piece of the first loadout: a Kindled Lantern, a fire-set piece, a fire focus or the Fire Bangle. */
	private static boolean equipped(ServerPlayer player) {
		Set<Item> loadout = new LinkedHashSet<>(EmberGear.ARMOR.values());
		loadout.add(JugcraftConcordance.KINDLED_LANTERN);
		loadout.add(EmberGear.LESSER_FIRE_FOCUS);
		loadout.add(EmberGear.FIRE_FOCUS);
		loadout.add(EmberGear.FIRE_BANGLE);
		return carries(player, stack -> loadout.contains(stack.getItem())) || Wayfaring.worn(player, stack -> loadout.contains(stack.getItem())) != null;
	}

	/** Whether any stack the player carries (hotbar, inventory, armour and off hand) passes {@code which}. */
	static boolean carries(ServerPlayer player, Predicate<ItemStack> which) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && which.test(stack)) {
				return true;
			}
		}
		return false;
	}

	/** Whether the player has earned the single-criterion advancement {@code id} (false if it does not exist). */
	static boolean done(ServerPlayer player, Identifier id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static @Nullable Item item(@Nullable String id) {
		Identifier parsed = id == null ? null : Identifier.tryParse(id);
		if (parsed == null) {
			return null;
		}
		Item item = BuiltInRegistries.ITEM.getValue(parsed);
		return item == Items.AIR ? null : item;
	}

	private static Component name(Item item) {
		return item.getDefaultInstance().getHoverName();
	}

	/** A whole number as a whole number, a fraction as given. */
	private static String number(float value) {
		return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
	}
}
