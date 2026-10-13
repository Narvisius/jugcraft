package io.github.jimbozoomer.jugcraft.concordance.compose;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;

/**
 * The absolute limits of the composition grammar. Data (components and instruments) can set lower limits, never higher
 * ones, so however a data pack is written a spell stays inside these. Keep them equal to COMPOSE_LIMITS in
 * tools/concordance.py.
 */
public final class Grammar {
	/** The longest composition text, in characters. */
	public static final int MAX_TEXT = 256;
	/** The most component names in one composition, modifiers and branches included. */
	public static final int MAX_NAMES = 24;
	/** Operations in one part of a composition (before or after a {@code then}). */
	public static final int MAX_OPERATIONS = 3;
	/** Modifiers on one component. */
	public static final int MAX_MODIFIERS = 2;
	/** How deep {@code then} may nest: a spell, its branch, and that branch's branch. */
	public static final int MAX_DEPTH = 2;
	public static final int MAX_BRANCHES = 2;
	public static final int MAX_RANGE = 32;
	public static final int MAX_RADIUS = 6;
	public static final int MAX_TARGETS = 16;
	public static final int MAX_WORK = 256;
	public static final int MAX_CAPACITY = 64;
	public static final int MAX_PULSES = 5;
	public static final int MIN_INTERVAL = 10;
	public static final int MAX_INTERVAL = 200;
	public static final int MAX_DURATION = EffectSpec.MAX_DURATION;
	/** The most capacity or Focus one component may take, and the most a modifier may add (percent or blocks). */
	public static final int MAX_COMPONENT_COST = 16;
	public static final int MAX_MODIFIER_AMOUNT = 200;
	/** Shortest and longest cooldown a composed spell gets. */
	public static final int MIN_COOLDOWN = 10;
	public static final int MAX_COOLDOWN = 200;

	private Grammar() {
	}
}
