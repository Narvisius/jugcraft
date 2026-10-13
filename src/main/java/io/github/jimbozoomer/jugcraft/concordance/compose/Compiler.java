package io.github.jimbozoomer.jugcraft.concordance.compose;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Turns a {@link Composition} into a {@link Plan}, or explains exactly why it cannot. It checks, for the player who
 * would cast it and the instrument it would be inscribed on:
 * <ul>
 * <li>that every name is a known component, in the right order (delivery, selection, operations, then an optional
 * termination), with modifiers only on parts they can change;</li>
 * <li>that the selection suits each operation (creatures or blocks) and that nothing harms its own caster;</li>
 * <li>that the player's research allows every component;</li>
 * <li>that it fits the instrument (capacity, distinct targets, work, branches, duration) and the player's Focus.</li>
 * </ul>
 * Every problem found is reported (not only the first), each naming the component and the limit concerned. The
 * compiler never evaluates anything: it only looks components up and adds up their numbers, so its own work is bounded
 * by the input's size ({@link Grammar}). Pure: no Minecraft types.
 */
public final class Compiler {
	/** What the caster has learned: the state of a research entry. */
	@FunctionalInterface
	public interface Knows {
		ResearchState state(String research);
	}

	public record Compilation(@Nullable Plan plan, List<Text> problems) {
		public Compilation {
			problems = List.copyOf(problems);
		}

		public boolean ok() {
			return plan != null;
		}
	}

	private final Catalog catalog;
	private final Knows knows;
	private final Instrument instrument;
	private final boolean authored;
	private final List<Text> problems = new ArrayList<>();
	private final Set<String> checkedResearch = new HashSet<>();
	private int capacity;
	private int focus;
	private int targets;
	private int work;
	private int branches;
	private int duration;

	private Compiler(Catalog catalog, Knows knows, Instrument instrument, boolean authored) {
		this.catalog = catalog;
		this.knows = knows;
		this.instrument = instrument;
		this.authored = authored;
	}

	/** Parses and compiles written text: a player's composition. */
	public static Compilation compile(String text, Catalog catalog, Knows knows, Instrument instrument) {
		CompositionParser.Parsed parsed = CompositionParser.parse(text);
		if (parsed.composition() == null) {
			return new Compilation(null, parsed.problems());
		}
		return compile(parsed.composition(), catalog, knows, instrument);
	}

	public static Compilation compile(Composition composition, Catalog catalog, Knows knows, Instrument instrument) {
		Compiler compiler = new Compiler(catalog, knows, instrument, false);
		Plan.Node root = compiler.node(composition, 0);
		return compiler.finish(composition, root);
	}

	/**
	 * Compiles an authored invocation's composition (roadmap step 10): the same grammar, checks and limits as a
	 * player's, except that authored words are allowed and research is not checked word by word (the invocation is
	 * learnt from its own research). So an invocation can never do more than a composed spell on the same instrument.
	 */
	public static Compilation compileAuthored(String text, Catalog catalog, Instrument instrument) {
		CompositionParser.Parsed parsed = CompositionParser.parse(text);
		if (parsed.composition() == null) {
			return new Compilation(null, parsed.problems());
		}
		return compileAuthored(parsed.composition(), catalog, instrument);
	}

	/** As {@link #compileAuthored(String, Catalog, Instrument)}, for a composition already parsed (or tuned). */
	public static Compilation compileAuthored(Composition composition, Catalog catalog, Instrument instrument) {
		Compiler compiler = new Compiler(catalog, research -> ResearchState.MASTERED, instrument, true);
		Plan.Node root = compiler.node(composition, 0);
		return compiler.finish(composition, root);
	}

	/**
	 * The composition with {@code modifier} joined to the first part it changes (the spell's own parts before its
	 * branch's), or null if it changes none that has room for it. This is how a tuning applies to an invocation: the
	 * result is still a composition and compiles under every limit a player's does, so a tuning can only change numbers
	 * (never what the spell selects or does) and can never take it past its instrument.
	 */
	public static @Nullable Composition tune(Composition composition, Component modifier, Catalog catalog) {
		if (!(modifier.part() instanceof Component.Modifier change)) {
			return null;
		}
		List<Composition.Part> parts = composition.parts();
		for (int i = 0; i < parts.size(); i++) {
			Composition.Part written = parts.get(i);
			Component part = catalog.component(written.component());
			if (part == null || change.aspect().modifies != part.slot() || !changes(change.aspect(), part)
					|| written.modifiers().contains(modifier.id()) || written.modifiers().size() >= Grammar.MAX_MODIFIERS) {
				continue;
			}
			List<String> joined = new ArrayList<>(written.modifiers());
			joined.add(modifier.id());
			List<Composition.Part> tuned = new ArrayList<>(parts);
			tuned.set(i, new Composition.Part(written.component(), joined));
			return new Composition(tuned, composition.then());
		}
		Composition then = composition.then() == null ? null : tune(composition.then(), modifier, catalog);
		return then == null ? null : new Composition(parts, then);
	}

	private Compilation finish(Composition composition, Plan.@Nullable Node root) {
		if (capacity > instrument.capacity()) {
			problems.add(Text.of("problem.capacity", capacity, instrument.ref(), instrument.capacity()));
		}
		if (branches > instrument.branches()) {
			problems.add(Text.of("problem.branches", branches, instrument.ref(), instrument.branches()));
		}
		if (targets > instrument.targets()) {
			problems.add(Text.of("problem.targets", targets, instrument.ref(), instrument.targets()));
		}
		if (work > instrument.work()) {
			problems.add(Text.of("problem.work", work, instrument.ref(), instrument.work()));
		}
		int cost = Math.max(1, focus);
		if (cost > FocusPool.MAX) {
			problems.add(Text.of("problem.focus", cost, FocusPool.MAX));
		}
		if (root == null || !problems.isEmpty()) {
			return new Compilation(null, problems);
		}
		int cooldown = Math.max(Math.min(Grammar.MIN_COOLDOWN + 5 * cost, Grammar.MAX_COOLDOWN), duration + Grammar.MIN_COOLDOWN);
		return new Compilation(new Plan(composition.text(), root, cost, cooldown, capacity,
				new Ledger.Limits(targets, work, branches), duration), List.of());
	}

	/** Compiles one level (the spell, or a branch); null if it has problems (which are recorded). */
	private Plan.@Nullable Node node(Composition composition, int depth) {
		if (depth > Grammar.MAX_DEPTH) {
			problems.add(Text.of("problem.too_deep", Grammar.MAX_DEPTH));
			return null;
		}
		int before = problems.size();
		List<Component> parts = new ArrayList<>();
		List<List<Component>> modifiers = new ArrayList<>();
		resolve(composition, parts, modifiers);
		Plan.Node child = null;
		if (composition.then() != null) {
			branches++;
			child = node(composition.then(), depth + 1);
		}
		if (problems.size() > before) {
			return null;
		}
		// Order: delivery, selection, operations, then an optional termination.
		Component delivery = null;
		Component selection = null;
		Component termination = null;
		List<Component> operations = new ArrayList<>();
		List<List<Component>> operationModifiers = new ArrayList<>();
		List<Component> deliveryModifiers = List.of();
		List<Component> selectionModifiers = List.of();
		for (int i = 0; i < parts.size(); i++) {
			Component part = parts.get(i);
			Slot expected = delivery == null ? Slot.DELIVERY : selection == null ? Slot.SELECTION : null;
			if (termination != null) {
				problems.add(Text.of("problem.after_termination", part.ref(), termination.ref()));
				return null;
			}
			if (expected != null && part.slot() != expected) {
				problems.add(Text.of("problem.expected", part.ref(), part.slot().ref(), expected.ref()));
				return null;
			}
			switch (part.slot()) {
				case DELIVERY -> {
					if (delivery != null) {
						problems.add(Text.of("problem.expected", part.ref(), part.slot().ref(), Slot.OPERATION.ref()));
						return null;
					}
					delivery = part;
					deliveryModifiers = modifiers.get(i);
				}
				case SELECTION -> {
					if (selection != null) {
						problems.add(Text.of("problem.expected", part.ref(), part.slot().ref(), Slot.OPERATION.ref()));
						return null;
					}
					selection = part;
					selectionModifiers = modifiers.get(i);
				}
				case OPERATION -> {
					if (operations.contains(part)) {
						problems.add(Text.of("problem.duplicate", part.ref()));
					}
					operations.add(part);
					operationModifiers.add(modifiers.get(i));
				}
				case TERMINATION -> {
					if (operations.isEmpty()) {
						problems.add(Text.of("problem.expected", part.ref(), part.slot().ref(), Slot.OPERATION.ref()));
						return null;
					}
					termination = part;
				}
				case MODIFIER -> throw new IllegalStateException("modifiers are resolved onto their parts");
			}
		}
		if (delivery == null || selection == null || operations.isEmpty()) {
			Slot missing = delivery == null ? Slot.DELIVERY : selection == null ? Slot.SELECTION : Slot.OPERATION;
			problems.add(Text.of("problem.missing", missing.ref()));
			return null;
		}
		if (operations.size() > Grammar.MAX_OPERATIONS) {
			problems.add(Text.of("problem.too_many_operations", Grammar.MAX_OPERATIONS));
		}
		Component.Delivery deliveryPart = (Component.Delivery) delivery.part();
		Component.Selection selectionPart = (Component.Selection) selection.part();
		if (depth > 0 && deliveryPart.form() != Component.Form.HERE) {
			problems.add(Text.of("problem.branch_delivery", delivery.ref()));
		}
		int range = deliveryPart.range() + added(deliveryModifiers, Component.Aspect.RANGE);
		if (range > Grammar.MAX_RANGE) {
			problems.add(Text.of("problem.range", delivery.ref(), range, Grammar.MAX_RANGE));
		}
		int radius = selectionPart.radius() + added(selectionModifiers, Component.Aspect.RADIUS);
		if (radius > Grammar.MAX_RADIUS) {
			problems.add(Text.of("problem.radius", selection.ref(), radius, Grammar.MAX_RADIUS));
		}
		Component.Termination ending = termination == null ? new Component.Termination(1, 0) : (Component.Termination) termination.part();
		List<Plan.Step> steps = new ArrayList<>();
		for (int i = 0; i < operations.size(); i++) {
			Component operation = operations.get(i);
			Component.Operation part = (Component.Operation) operation.part();
			EffectSpec effect = part.effect();
			if (!selectionPart.pick().suits(effect.kind().on)) {
				problems.add(Text.of("problem.selection", operation.ref(), new Text.Ref("on", effect.kind().on.name().toLowerCase(Locale.ROOT)),
						selection.ref()));
			}
			if (depth == 0 && deliveryPart.form() == Component.Form.HERE && selectionPart.pick() == Component.Pick.STRUCK
					&& effect.intent() == Intent.HARMFUL) {
				problems.add(Text.of("problem.harms_caster", operation.ref()));
			}
			if (selectionPart.pick() == Component.Pick.ALLIES && effect.intent() == Intent.HARMFUL) {
				problems.add(Text.of("problem.harms_allies", operation.ref(), selection.ref()));
			}
			int magnitude = effect.magnitude();
			int ticks = effect.duration();
			for (Component modifier : operationModifiers.get(i)) {
				Component.Modifier change = (Component.Modifier) modifier.part();
				if (change.aspect() == Component.Aspect.MAGNITUDE) {
					magnitude += Math.max(1, effect.magnitude() * change.amount() / 100);
				} else if (change.aspect() == Component.Aspect.DURATION) {
					ticks += effect.duration() * change.amount() / 100;
				}
			}
			int strongest = switch (effect.kind()) {
				case STATUS -> EffectSpec.MAX_AMPLIFIER;
				case MOVEMENT -> EffectSpec.MAX_PUSH;
				default -> EffectSpec.MAX_MAGNITUDE;
			};
			if (magnitude > strongest) {
				problems.add(Text.of("problem.magnitude", operation.ref(), magnitude, strongest));
				magnitude = strongest;
			}
			if (ticks > instrument.duration()) {
				problems.add(Text.of("problem.duration", operation.ref(), Text.seconds(ticks), instrument.ref(),
						Text.seconds(instrument.duration())));
				ticks = Math.min(ticks, EffectSpec.MAX_DURATION);
			}
			steps.add(new Plan.Step(i, operation.id(), effect.withMagnitude(magnitude).withDuration(ticks), part.principle(), part.scaling()));
			focus += (operation.focus() + cost(operationModifiers.get(i))) * ending.pulses();
			work += ending.pulses() * effect.kind().work * selectionPart.targets();
		}
		Plan.Node node = new Plan.Node(depth, delivery.id(), deliveryPart.form(), range, selection.id(), selectionPart.pick(), radius,
				selectionPart.targets(), steps, termination == null ? null : termination.id(), ending.pulses(), ending.interval(), child);
		if (node.lingers() > instrument.duration()) {
			problems.add(Text.of("problem.duration", termination == null ? delivery.ref() : termination.ref(), Text.seconds(node.lingers()),
					instrument.ref(), Text.seconds(instrument.duration())));
		}
		focus += delivery.focus() + selection.focus() + (termination == null ? 0 : termination.focus()) + cost(deliveryModifiers)
				+ cost(selectionModifiers);
		work += deliveryPart.form() == Component.Form.HERE ? 0 : 1;
		targets += selectionPart.targets();
		duration = Math.max(duration, node.lingers());
		return problems.size() > before ? null : node;
	}

	/**
	 * Looks every name up, checking research and modifiers as it goes, and adds up capacity. Fills {@code parts} and
	 * {@code modifiers} (one list per part) with what resolved.
	 */
	private void resolve(Composition composition, List<Component> parts, List<List<Component>> modifiers) {
		for (Composition.Part written : composition.parts()) {
			Component part = catalog.component(written.component());
			if (part == null) {
				problems.add(Text.of("problem.unknown", Composition.shortName(written.component())));
				continue;
			}
			if (part.slot() == Slot.MODIFIER) {
				problems.add(Text.of("problem.loose_modifier", part.ref()));
				continue;
			}
			use(part);
			if (written.modifiers().size() > Grammar.MAX_MODIFIERS) {
				problems.add(Text.of("problem.too_many_modifiers", part.ref(), Grammar.MAX_MODIFIERS));
			}
			List<Component> joined = new ArrayList<>();
			for (String id : written.modifiers()) {
				Component modifier = catalog.component(id);
				if (modifier == null) {
					problems.add(Text.of("problem.unknown", Composition.shortName(id)));
					continue;
				}
				if (modifier.slot() != Slot.MODIFIER) {
					problems.add(Text.of("problem.not_modifier", modifier.ref(), part.ref()));
					continue;
				}
				use(modifier);
				Component.Modifier change = (Component.Modifier) modifier.part();
				if (joined.contains(modifier)) {
					problems.add(Text.of("problem.duplicate", modifier.ref()));
				} else if (change.aspect().modifies != part.slot() || !changes(change.aspect(), part)) {
					problems.add(Text.of("problem.modifier", modifier.ref(), change.aspect().ref(), part.ref()));
				}
				joined.add(modifier);
			}
			parts.add(part);
			modifiers.add(joined);
		}
	}

	/** Counts a component's capacity and checks the caster may use it (each component's research once). */
	private void use(Component component) {
		capacity += component.capacity();
		if (component.authored() && !authored) {
			problems.add(Text.of("problem.authored", component.ref()));
			return;
		}
		Definitions.Requirement needs = component.requires();
		ResearchState has = knows.state(needs.research());
		if (!has.atLeast(needs.state()) && checkedResearch.add(component.id())) {
			problems.add(Text.of("problem.research", component.ref(), new Text.Ref("research", needs.research()),
					new Text.Ref("state", needs.state().id())));
		}
	}

	/** Whether a modifier of this aspect would change this component at all. */
	private static boolean changes(Component.Aspect aspect, Component part) {
		return switch (part.part()) {
			case Component.Operation operation -> switch (aspect) {
				case MAGNITUDE -> switch (operation.effect().kind()) {
					case DAMAGE, RESTORATION, MOVEMENT, STATUS, PROTECTION -> true;
					default -> false;
				};
				case DURATION -> operation.effect().duration() > 0;
				default -> false;
			};
			case Component.Selection selection -> aspect == Component.Aspect.RADIUS && selection.radius() > 0;
			case Component.Delivery delivery -> aspect == Component.Aspect.RANGE && delivery.range() > 0;
			default -> false;
		};
	}

	private static int added(List<Component> modifiers, Component.Aspect aspect) {
		int total = 0;
		for (Component modifier : modifiers) {
			Component.Modifier change = (Component.Modifier) modifier.part();
			if (change.aspect() == aspect) {
				total += change.amount();
			}
		}
		return total;
	}

	private static int cost(List<Component> modifiers) {
		int total = 0;
		for (Component modifier : modifiers) {
			total += modifier.focus();
		}
		return total;
	}
}
