package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Composing a spell for the instrument in a player's main hand: the one service behind the
 * {@code /jugcraft concordance compose} commands and the composer screen, so both give the same answer, from the same
 * checks, for the same player, text and instrument. Everything is decided here, on the server, from the server's rules,
 * the player's research and what they hold. Of what a client sends only the text is used, and only as text.
 * <p>
 * {@link #compose} (checking, or inscribing) asks in this order:
 * <ol>
 * <li>whether the Concordance is on;</li>
 * <li>whether the player holds an instrument;</li>
 * <li>for a screen's change, whether that instrument and the rules are still the ones the screen showed ({@link Held});</li>
 * <li>whether {@value #COMPOSE_RATE_TICKS} ticks have passed since their last composition, by command or screen, or
 * tuning;</li>
 * <li>whether the text compiles ({@link ComposedSpells#compile}).</li>
 * </ol>
 * Only a valid composition is written, replacing what the instrument held. {@link #show} and the command's
 * {@link #clear(ServerPlayer)} ask only what those commands always have; a screen's clear asks what composing asks.
 */
public final class Composing {
	/**
	 * The most often a player may check, inscribe or tune a composition, by command or screen alike. Keep equal to
	 * COMPOSE_RATE_TICKS in tools/concordance.py.
	 */
	public static final int COMPOSE_RATE_TICKS = 20;
	/** The {@link RateGate} action that composing and tuning share. */
	public static final String RATE_ACTION = "compose";

	private Composing() {
	}

	/** What a request came to. */
	public enum Outcome {
		/** The Concordance is switched off on this server. */
		DISABLED,
		/** No composing instrument is in the player's main hand. */
		NO_INSTRUMENT,
		/** The instrument in hand, or the rules, changed since the screen was shown them; nothing was written. */
		CHANGED,
		/** Too soon after the player's last composition or tuning. */
		TOO_FAST,
		/** Nothing is inscribed on the held item. */
		NOTHING_INSCRIBED,
		/** It does not compile; the problems say why. */
		INVALID,
		/** It compiles; nothing was written. */
		VALID,
		/** It compiled and is now inscribed on the instrument. */
		INSCRIBED,
		/** The inscription was wiped. */
		CLEARED
	}

	/**
	 * What a request came to, with everything needed to say so.
	 *
	 * @param instrument the instrument in hand, if there was one
	 * @param text the canonical text of a valid composition; otherwise the text as it was checked (trimmed), or empty
	 * @param plan the compiled spell (valid and inscribed only)
	 * @param problems why it does not compile (invalid only)
	 */
	public record Result(Outcome outcome, @Nullable Instrument instrument, String text, @Nullable Plan plan, List<Text> problems) {
		public Result {
			problems = List.copyOf(problems);
		}

		/** The lines that go with it: what a valid spell does and costs, or every problem with an invalid one. */
		public List<Text> lines() {
			return plan != null && instrument != null ? plan.explain(instrument) : problems;
		}
	}

	/**
	 * What a screen was showing when its player chose to inscribe or clear: the revision of the rules then in force
	 * ({@link ConcordanceData#revision}) and the instrument then in hand. A screen's change is made only while both still
	 * hold; otherwise nothing is written ({@link Outcome#CHANGED}), so a spell is never written on an instrument, or
	 * under rules, other than the ones its player saw it checked against. Nothing is added to the item for this: the held
	 * stack is compared whole.
	 */
	public record Held(int rules, ItemStack stack) {
		public static Held of(ServerPlayer player) {
			return new Held(ConcordanceData.revision(), player.getMainHandItem().copy());
		}

		public boolean holds(ServerPlayer player) {
			return rules == ConcordanceData.revision() && ItemStack.matches(stack, player.getMainHandItem());
		}
	}

	/**
	 * Checks {@code text} for the player and the instrument in their main hand and, with {@code inscribe}, writes it on
	 * the instrument if it is valid.
	 *
	 * @param shown for a screen's request, what the screen showed (a change is refused unless it still holds); null for a
	 *     command
	 */
	public static Result compose(ServerPlayer player, String text, boolean inscribe, @Nullable Held shown) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return answer(Outcome.DISABLED, null);
		}
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			return answer(Outcome.NO_INSTRUMENT, null);
		}
		if (shown != null && !shown.holds(player)) {
			return answer(Outcome.CHANGED, instrument);
		}
		if (!RateGate.allow(player, RATE_ACTION, COMPOSE_RATE_TICKS)) {
			return answer(Outcome.TOO_FAST, instrument);
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, instrument, text);
		Plan plan = compiled.plan();
		if (plan == null) {
			return new Result(Outcome.INVALID, instrument, text.trim(), null, compiled.problems());
		}
		if (!inscribe) {
			return new Result(Outcome.VALID, instrument, plan.text(), plan, List.of());
		}
		player.getMainHandItem().set(JugcraftConcordance.INSCRIPTION, new Inscription(plan.text(), plan.focus(), plan.cooldown()));
		return new Result(Outcome.INSCRIBED, instrument, plan.text(), plan, List.of());
	}

	/** The spell inscribed on the held instrument, compiled as it would be cast now. */
	public static Result show(ServerPlayer player) {
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			return answer(Outcome.NO_INSTRUMENT, null);
		}
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		if (inscription == null) {
			return answer(Outcome.NOTHING_INSCRIBED, instrument);
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, instrument, inscription.text());
		Plan plan = compiled.plan();
		return plan == null ? new Result(Outcome.INVALID, instrument, inscription.text(), null, compiled.problems())
				: new Result(Outcome.VALID, instrument, plan.text(), plan, List.of());
	}

	/** Wipes the inscription on the item in the player's main hand: the command's clear, which has never asked more. */
	public static Result clear(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		if (ComposedSpells.inscription(held) == null) {
			return answer(Outcome.NOTHING_INSCRIBED, ComposedSpells.instrument(player));
		}
		held.remove(JugcraftConcordance.INSCRIPTION);
		return answer(Outcome.CLEARED, ComposedSpells.instrument(player));
	}

	/**
	 * A screen's clear: asked as composing is (the Concordance on, an instrument in hand, still the one the screen
	 * showed, and the shared rate), then as the command's.
	 */
	public static Result clear(ServerPlayer player, Held shown) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return answer(Outcome.DISABLED, null);
		}
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null) {
			return answer(Outcome.NO_INSTRUMENT, null);
		}
		if (!shown.holds(player)) {
			return answer(Outcome.CHANGED, instrument);
		}
		if (!RateGate.allow(player, RATE_ACTION, COMPOSE_RATE_TICKS)) {
			return answer(Outcome.TOO_FAST, instrument);
		}
		return clear(player);
	}

	private static Result answer(Outcome outcome, @Nullable Instrument instrument) {
		return new Result(outcome, instrument, "", null, List.of());
	}
}
