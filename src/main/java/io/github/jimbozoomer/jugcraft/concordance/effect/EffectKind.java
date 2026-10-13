package io.github.jimbozoomer.jugcraft.concordance.effect;

import org.jspecify.annotations.Nullable;

/**
 * The ten common operations every Concordance effect is made of, whatever delivers it (a spell, an invocation, an
 * item, a ritual, a potion, a weapon, a creature or a shrine). Each acts on a creature or on a block, and each
 * application costs a fixed amount of work, the unit composed spells are capped in. Keep the ids equal to
 * EFFECT_KINDS in tools/concordance.py.
 */
public enum EffectKind {
	/** Harm to health, through the damage type of the effect's school. */
	DAMAGE("damage", On.CREATURE, 1),
	/** Health given back. */
	RESTORATION("restoration", On.CREATURE, 1),
	/** A push away from where the effect came from. */
	MOVEMENT("movement", On.CREATURE, 1),
	/** Light set in open air (a self-ending Kindled mote). */
	ILLUMINATION("illumination", On.BLOCK, 2),
	/** A status effect from the vanilla or Jugcraft registry. */
	STATUS("status", On.CREATURE, 1),
	/** Using a block as a player would (levers, buttons, doors): {@code #jugcraft:concordance/interactable}. */
	INTERACTION("interaction", On.BLOCK, 2),
	/** Gathering a ripe crop or a plant in {@code #jugcraft:concordance/harvestable}, with its drops. */
	HARVESTING("harvesting", On.BLOCK, 3),
	/** Absorption against incoming harm. */
	PROTECTION("protection", On.CREATURE, 1),
	/** Making a creature glow, visible through walls. */
	DETECTION("detection", On.CREATURE, 1),
	/**
	 * A limited, listed change to the world: putting out fire; or, in Ember's school ({@code spell_power:fire}),
	 * kindling an unlit campfire, candle or candle cake (concordance/ember/Ember.java). It never places fire.
	 */
	ALTERATION("alteration", On.BLOCK, 3);

	/** What an operation acts on. */
	public enum On {
		CREATURE,
		BLOCK
	}

	public final String id;
	public final On on;
	/** Work units one application costs (lighting updates and drops cost more than a status). */
	public final int work;

	EffectKind(String id, On on, int work) {
		this.id = id;
		this.on = on;
		this.work = work;
	}

	public static @Nullable EffectKind fromId(String id) {
		for (EffectKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
