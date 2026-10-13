package io.github.jimbozoomer.jugcraft.concordance.sign;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The Concordance's shared vocabulary for showing what really happened (roadmap step 27). The server sends a sign only
 * when the thing it shows has happened ({@link Signs#show}); each client draws it in its own particles, as many as its
 * visual intensity allows ({@link Presentation}), and plays its sound if it has one. Every sign is of one of five kinds,
 * and the kinds differ in shape and movement, not only colour:
 * <ul>
 * <li><b>preparation</b>: glyphs drawn inwards to the place being readied;</li>
 * <li><b>execution</b>: a few sparks rising where work was done, or travelling from a source along a real transfer
 * ({@link #FLOW});</li>
 * <li><b>success</b>: a burst of light;</li>
 * <li><b>shortage</b>: grey ash sinking, with a hollow falling tone;</li>
 * <li><b>danger</b>: sparks and flame thrown outwards, with a sharp rising warning.</li>
 * </ul>
 * The two warnings are never hidden: at the lowest intensity they still draw a little and still sound. Effect marks show
 * where the shared effect boundary applied an effect, one look per {@link EffectKind}, as the boundary showed them before
 * this step. Keep the ids, kinds, counts and sounds equal to SIGNS in tools/concordance_signs.py.
 */
public enum Sign {
	GATHER("gather", Kind.PREPARATION, 12),
	WORK("work", Kind.EXECUTION, 4),
	FLOW("flow", Kind.EXECUTION, 8),
	DONE("done", Kind.SUCCESS, 24),
	WANT("want", Kind.SHORTAGE, 8),
	PERIL("peril", Kind.DANGER, 12),
	// Effect marks (ConcordanceEffects): where an effect was applied.
	HARM("harm", Kind.DANGER, 6),
	MEND("mend", Kind.SUCCESS, 6),
	SHOVE("shove", Kind.EXECUTION, 6),
	LIGHT("light", Kind.EXECUTION, 6),
	CHARM("charm", Kind.EXECUTION, 6),
	TOUCH("touch", Kind.EXECUTION, 6),
	UNMAKE("unmake", Kind.EXECUTION, 6);

	/** What a sign tells: getting ready, doing, done, lacking, or dangerous. */
	public enum Kind {
		PREPARATION,
		EXECUTION,
		SUCCESS,
		SHORTAGE,
		DANGER;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** A warning: always drawn a little and always sounded, whatever the intensity. */
		public boolean warns() {
			return this == SHORTAGE || this == DANGER;
		}
	}

	public final String id;
	public final Kind kind;
	/** How many particles it draws at full intensity, close by. */
	public final int particles;

	Sign(String id, Kind kind, int particles) {
		this.id = id;
		this.kind = kind;
		this.particles = particles;
	}

	/** Whether the client plays a sound of the sign's own (the warnings; the others are each feature's own sounds). */
	public boolean sounds() {
		return kind.warns() && this != HARM;
	}

	public static @Nullable Sign byIndex(int index) {
		Sign[] all = values();
		return index >= 0 && index < all.length ? all[index] : null;
	}

	public static @Nullable Sign fromId(String id) {
		for (Sign sign : values()) {
			if (sign.id.equals(id)) {
				return sign;
			}
		}
		return null;
	}

	/** The mark for an applied effect (each effect kind looks as it did before signs). */
	public static Sign forEffect(EffectKind kind) {
		return switch (kind) {
			case DAMAGE -> HARM;
			case RESTORATION, PROTECTION -> MEND;
			case MOVEMENT -> SHOVE;
			case ILLUMINATION, DETECTION -> LIGHT;
			case STATUS -> CHARM;
			case INTERACTION, HARVESTING -> TOUCH;
			case ALTERATION -> UNMAKE;
		};
	}
}
