package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Where a crucible's temperature heads: towards the heat of the block beneath it ({@link #SOURCES}, by block id; a
 * campfire only while lit), or {@link #AMBIENT} with none, by {@link #RATE} degrees a tick. Worked out on the server
 * each tick from the block below, so a fire put out or lit changes the process at once. A Crucible Stand (Concordance
 * Chunk C, C-05) heats towards {@link #standTarget} for the band its crucible needs ({@link #standBand}) on the ticks it
 * pays; fed too little, it holds the crucible on some ticks it cannot pay (CrucibleStandBlockEntity.holds). Keep equal to
 * tools/concordance_alchemy.py.
 */
public final class Heat {
	public static final int AMBIENT = 20;
	public static final int RATE = 1;
	public static final Map<String, Integer> SOURCES = Map.of(
			"minecraft:magma_block", 60,
			"minecraft:soul_campfire", 100,
			"minecraft:campfire", 120,
			"minecraft:soul_fire", 150,
			"minecraft:fire", 175,
			"minecraft:lava", 220);
	/**
	 * Where a Crucible Stand heats the crucible on it for a stir at each band (Concordance Chunk C, C-05, C-P12; starting
	 * values): inside the band, so the stir can be taken. Cold is off: the crucible settles at AMBIENT.
	 */
	public static final int STAND_WARM = 65;
	public static final int STAND_HOT = 120;
	public static final int STAND_SEARING = 175;

	private Heat() {
	}

	/**
	 * The block beneath a crucible (Concordance Chunk C, C-01): its id, whether it is out (an unlit campfire; a Crucible
	 * Stand that gave nothing on its last tick, C-05) and its heat: SOURCES' for a block that heats by what it is, what a
	 * stand gave (the target it paid for, the crucible's own temperature on a tick it held it, AMBIENT when it gave
	 * nothing), or null for a block that does not heat a crucible.
	 */
	public record Below(String block, boolean unlit, @Nullable Integer heat) {
		/** A block that heats a crucible by what it is ({@link Heat#SOURCES}; null heat for one that does not). */
		public Below(String block, boolean unlit) {
			this(block, unlit, SOURCES.get(block));
		}

		/** Where it heats a crucible towards: its heat while lit, the air's otherwise. */
		public int target() {
			return heat == null || unlit ? AMBIENT : heat;
		}
	}

	/** A Crucible Stand's target for {@code band} (C-05): AMBIENT for cold, which is off. */
	public static int standTarget(Band band) {
		return switch (band) {
			case COLD -> AMBIENT;
			case WARM -> STAND_WARM;
			case HOT -> STAND_HOT;
			case SEARING -> STAND_SEARING;
		};
	}

	/**
	 * Whether a crucible waiting for {@code waiting} waits for nothing or for heat alone (C-05, C-P43): no reason, the wrong
	 * band, or a Preserve hold. A hold still running is judged from the crucible's clock (standBand's {@code held}), so a
	 * HELD its last pulse left never idles a stand once the hold is over.
	 */
	public static boolean waitsForHeat(@Nullable Diagnostic waiting) {
		return waiting == null || waiting.code() == Code.WRONG_HEAT || waiting.code() == Code.HELD;
	}

	/**
	 * The band a Crucible Stand heats the crucible on it for now (C-05, C-P12, C-P43), or null to give no heat. Preserve's
	 * hold wins. With a formula, the band of its next stir from {@code step} on (null for a cold stir or none left before
	 * bottling), but only while the crucible waits for nothing or for heat alone (waitsForHeat). With no formula, the
	 * stand's own {@code setting} (null for cold, off), unless a formula it cannot read is kept ({@code waiting} non-null).
	 * At most Mixture.MAX_OPERATIONS operations read.
	 */
	public static @Nullable Band standBand(@Nullable Formula program, int step, @Nullable Diagnostic waiting, boolean held, Band setting) {
		if (held) {
			return null;
		}
		if (program == null) {
			return waiting != null || setting == Band.COLD ? null : setting;
		}
		if (!waitsForHeat(waiting)) {
			return null;
		}
		List<Operation> operations = program.operations();
		for (int i = Math.max(0, step); i < operations.size(); i++) {
			if (operations.get(i) instanceof Operation.Stir stir) {
				return stir.band() == Band.COLD ? null : stir.band();
			}
		}
		return null;
	}

	/** The temperature a tick later, moving towards {@code target}. */
	public static int toward(int temperature, int target) {
		if (temperature < target) {
			return Math.min(target, temperature + RATE);
		}
		return Math.max(target, temperature - RATE);
	}
}
