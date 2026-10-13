package io.github.jimbozoomer.jugcraft.concordance.compose;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

/**
 * A compiled spell: a bounded tree of nodes (the spell and its {@code then} branches) with every number worked out,
 * the Focus it costs, its cooldown, and the limits it runs under. The server executes a plan through a {@link Ledger}
 * built from {@link #limits()}, so a running spell can never reach more targets, spend more work or take more branches
 * than were compiled here, whatever it meets in the world.
 *
 * @param text the canonical composition it was compiled from
 * @param focus Focus taken once, when the spell first takes effect
 * @param cooldown ticks before it can be cast again (at least as long as any of its pulses lasts)
 * @param capacity instrument capacity its components take
 * @param duration ticks the longest-lasting node keeps pulsing after it lands
 */
public record Plan(String text, Node root, int focus, int cooldown, int capacity, Ledger.Limits limits, int duration) {
	/**
	 * One part of the tree: where it lands, what it chooses and what it does there.
	 *
	 * @param depth 0 for the spell, 1 for its branch, 2 for that branch's
	 * @param range delivery range in blocks, modifiers applied
	 * @param radius selection radius in blocks, modifiers applied
	 * @param targets the most its selection chooses per operation and pulse
	 * @param pulses times it acts at the same place (1: once)
	 * @param interval ticks between pulses
	 */
	public record Node(int depth, String delivery, Component.Form form, int range, String selection, Component.Pick pick, int radius,
			int targets, List<Step> steps, @Nullable String termination, int pulses, int interval, @Nullable Node then) {
		public Node {
			steps = List.copyOf(steps);
		}

		/** Ticks this node keeps pulsing after it first acts. */
		public int lingers() {
			return (pulses - 1) * interval;
		}
	}

	/**
	 * One operation in a node, as compiled.
	 *
	 * @param index its place among the node's operations (part of its ledger key)
	 * @param scaling damage added per point of Spell Power above the base ({@link Component.Operation#scaling})
	 */
	public record Step(int index, String component, EffectSpec effect, String principle, double scaling) {
	}

	/**
	 * This plan with its damage scaled by the caster's Spell Power: each damage step with a scaling factor (only damage
	 * that names its school has one) gains {@code floor(scaling * power above base)} in that school, capped at the
	 * strongest damage allowed. This is the one place Spell Power enters a Concordance effect; the effect boundary
	 * applies the result as it is, and the damage type's resistances apply once, in the damage itself.
	 *
	 * @param powerAboveBase the caster's Spell Power above the school's base value, by school id
	 */
	public Plan scaled(ToDoubleFunction<String> powerAboveBase) {
		return new Plan(text, scaled(root, powerAboveBase), focus, cooldown, capacity, limits, duration);
	}

	private static Node scaled(Node node, ToDoubleFunction<String> powerAboveBase) {
		List<Step> steps = new ArrayList<>();
		for (Step step : node.steps()) {
			EffectSpec effect = step.effect();
			if (effect.kind() == EffectKind.DAMAGE && step.scaling() > 0 && effect.school() != null) {
				double above = Math.max(0.0, powerAboveBase.applyAsDouble(effect.school()));
				int added = (int) Math.min(EffectSpec.MAX_MAGNITUDE, Math.floor(step.scaling() * above));
				effect = effect.withMagnitude(Math.min(EffectSpec.MAX_MAGNITUDE, effect.magnitude() + added));
			}
			steps.add(new Step(step.index(), step.component(), effect, step.principle(), step.scaling()));
		}
		Node then = node.then() == null ? null : scaled(node.then(), powerAboveBase);
		return new Node(node.depth(), node.delivery(), node.form(), node.range(), node.selection(), node.pick(), node.radius(),
				node.targets(), steps, node.termination(), node.pulses(), node.interval(), then);
	}

	/**
	 * The most ticks anything this plan makes can last after it is cast: for each node, its pulses plus its
	 * longest-lasting effect. Instant effects (damage, a push) last nothing.
	 */
	public int persists() {
		int longest = 0;
		for (Node node = root; node != null; node = node.then()) {
			int effect = 0;
			for (Step step : node.steps()) {
				effect = Math.max(effect, step.effect().duration());
			}
			longest = Math.max(longest, node.lingers() + effect);
		}
		return longest;
	}

	/**
	 * What the plan is, regardless of its numbers: for each node, its delivery form, selection and the kinds of effect
	 * it applies. Two invocations with the same signature would be the same spell with different numbers; tunings
	 * (modifiers) change numbers only, never the signature.
	 */
	public String signature() {
		StringBuilder out = new StringBuilder();
		for (Node node = root; node != null; node = node.then()) {
			if (node != root) {
				out.append(" then ");
			}
			out.append(node.form().id()).append('/').append(node.pick().id()).append(':');
			List<String> kinds = new ArrayList<>();
			for (Step step : node.steps()) {
				kinds.add(step.effect().kind().id + "/" + step.effect().intent().id);
			}
			kinds.sort(null);
			out.append(String.join(",", kinds));
		}
		return out.toString();
	}

	/** A readable account of what the spell does and costs, from the same numbers the server runs. */
	public List<Text> explain(Instrument instrument) {
		List<Text> lines = new ArrayList<>();
		explain(root, lines);
		lines.add(Text.of("explain.cost", focus, Text.seconds(cooldown)));
		lines.add(Text.of("explain.limits", limits.targets(), limits.work(), capacity, instrument.ref(), instrument.capacity()));
		return lines;
	}

	private static void explain(Node node, List<Text> lines) {
		if (node.depth() > 0) {
			lines.add(Text.of("explain.then"));
		}
		Text.Ref delivery = Text.component(node.delivery());
		switch (node.form()) {
			case HERE -> lines.add(Text.of(node.depth() == 0 ? "explain.delivery.here" : "explain.delivery.here_then", delivery));
			case TOUCH -> lines.add(Text.of("explain.delivery.touch", delivery, node.range()));
			case RAY -> lines.add(Text.of("explain.delivery.ray", delivery, node.range()));
		}
		Text.Ref selection = Text.component(node.selection());
		switch (node.pick()) {
			case STRUCK -> lines.add(Text.of("explain.selection.struck", selection));
			case CREATURES -> lines.add(Text.of("explain.selection.creatures", selection, node.targets(), node.radius()));
			case ALLIES -> lines.add(Text.of("explain.selection.allies", selection, node.targets(), node.radius()));
			case BLOCKS -> lines.add(Text.of("explain.selection.blocks", selection, node.targets(), node.radius()));
		}
		for (Step step : node.steps()) {
			lines.add(operation(step));
			if (step.scaling() > 0 && step.effect().kind() == EffectKind.DAMAGE) {
				lines.add(Text.of("explain.scaling", Text.component(step.component()), String.format(Locale.ROOT, "%.2f", step.scaling())));
			}
		}
		if (node.pulses() > 1) {
			lines.add(Text.of("explain.termination.pulse", Text.component(node.termination() == null ? "" : node.termination()),
					node.pulses(), Text.seconds(node.interval())));
		}
		if (node.then() != null) {
			explain(node.then(), lines);
		}
	}

	private static Text operation(Step step) {
		EffectSpec effect = step.effect();
		Text.Ref name = Text.component(step.component());
		String key = "explain.operation." + effect.kind().id;
		return switch (effect.kind()) {
			case DAMAGE, RESTORATION -> Text.of(key, name, effect.magnitude());
			case MOVEMENT -> Text.of(key, name, String.format(Locale.ROOT, "%.1f", effect.magnitude() / 10.0));
			case ILLUMINATION, DETECTION -> Text.of(key, name, Text.seconds(effect.duration()));
			case STATUS -> Text.of(key, name, new Text.Ref("status", effect.status() == null ? "" : effect.status()),
					effect.magnitude() + 1, Text.seconds(effect.duration()));
			case PROTECTION -> Text.of(key, name, 4 * (effect.protectionAmplifier() + 1), Text.seconds(effect.duration()));
			case INTERACTION, HARVESTING, ALTERATION -> Text.of(key, name);
		};
	}
}
