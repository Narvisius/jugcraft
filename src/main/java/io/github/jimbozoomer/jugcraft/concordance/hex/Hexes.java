package io.github.jimbozoomer.jugcraft.concordance.hex;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The rules of links, curses and investigation (roadmap step 22, docs/features/arcane-concordance-hexes.md). Pure: the
 * server says what is true at the moment of use, and these rules say whether it may happen and, if not, exactly why.
 * Everything is revalidated at the moment it acts: a link when a curse is cast through it, a curse at each pulse.
 */
public final class Hexes {
	/** The furthest a caster may be from the target, for casting and for every pulse. */
	public static final int MAX_RANGE = 128;
	/** The most curses one creature carries at once. */
	public static final int MAX_CURSES = 3;
	/** The Focus one look through a scrying glass costs. */
	public static final int INVESTIGATE_FOCUS = 2;

	/**
	 * What is true when a link or curse acts: whether the target was found, whether it and the caster are in one
	 * dimension, how far apart they are, whether the target is warded against this, whether the multiplayer rules let the
	 * caster harm the target (the server's PvP setting and parties), and the game time.
	 */
	public record Situation(boolean targetFound, boolean sameDimension, double distance, boolean warded, boolean allowed, long now) {
	}

	private Hexes() {
	}

	/** Why {@code curse} may not be cast through {@code link} now, or the empty string. */
	public static String cast(@Nullable Link link, CurseDefinition curse, Situation situation, List<Curse> existing, int focus) {
		if (link == null) {
			return "no_link";
		}
		if (link.expired(situation.now())) {
			return "expired";
		}
		if (!situation.sameDimension()) {
			return "elsewhere";
		}
		if (!situation.targetFound()) {
			return "not_found";
		}
		if (situation.distance() > MAX_RANGE) {
			return "too_far";
		}
		if (!situation.allowed()) {
			return "not_allowed";
		}
		if (situation.warded()) {
			return "warded";
		}
		if (link.strength(situation.now()) < curse.strength()) {
			return "weak";
		}
		int active = 0;
		for (Curse held : existing) {
			if (held.active(situation.now())) {
				if (held.curse().equals(curse.id())) {
					return "already";
				}
				active++;
			}
		}
		if (active >= MAX_CURSES) {
			return "too_many";
		}
		if (focus < curse.focus()) {
			return "no_focus";
		}
		return "";
	}

	/**
	 * Whether a running curse pulses now: the empty string to pulse; {@code "ended"} when its time is over and
	 * {@code "not_allowed"} when the multiplayer rules no longer let its caster harm its bearer (both lift it); otherwise
	 * why this pulse is skipped ({@code "resting"}, {@code "elsewhere"} while the caster is in another dimension or
	 * offline, {@code "too_far"}, {@code "warded"}).
	 */
	public static String pulse(Curse curse, CurseDefinition definition, Situation situation) {
		if (!curse.active(situation.now())) {
			return "ended";
		}
		if (!situation.allowed()) {
			return "not_allowed";
		}
		if (situation.now() - curse.lastPulse() < definition.pulseTicks()) {
			return "resting";
		}
		if (!situation.sameDimension()) {
			return "elsewhere";
		}
		if (situation.distance() > MAX_RANGE) {
			return "too_far";
		}
		if (situation.warded()) {
			return "warded";
		}
		return "";
	}

	/** Whether a lifting reason ends the curse (rather than skipping one pulse). */
	public static boolean lifts(String reason) {
		return reason.equals("ended") || reason.equals("not_allowed");
	}

	/**
	 * The insight one more look gives: first the curse's name and remedy, then its caster, unless the caster's scrying
	 * ward hides them (then it stays named). Never less than already known.
	 */
	public static int investigate(Curse curse, boolean casterHidden) {
		int next = Math.min(Curse.TRACED, curse.insight() + 1);
		if (next == Curse.TRACED && casterHidden) {
			next = Curse.NAMED;
		}
		return Math.max(curse.insight(), next);
	}

	/** Whether {@code item} is {@code curse}'s remedy. */
	public static boolean remedies(CurseDefinition curse, String item) {
		return curse.remedy().equals(item);
	}

	/** Whether a link to {@code target} may be made by {@code linker}: never to oneself, never through a linking ward. */
	public static String link(UUID linker, UUID target, boolean warded, boolean allowed) {
		if (linker.equals(target)) {
			return "self";
		}
		if (!allowed) {
			return "not_allowed";
		}
		return warded ? "warded" : "";
	}
}
