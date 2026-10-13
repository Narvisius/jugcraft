package io.github.jimbozoomer.jugcraft.concordance.sign;

import java.util.Locale;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * How much the Concordance draws on this client (roadmap step 27): its visual intensity and reduced motion (the
 * player's own settings, kept in config/jugcraft-client.properties) and one budget of particles a tick shared by every
 * Concordance effect, so a large installation draws no more than the budget however many blocks it has. Signs spend
 * from the budget first; a block's ambient sparkle may use only {@link #AMBIENT_SHARE} of it. Beyond {@link #NEAR}
 * blocks a sign draws half, beyond {@link #FAR} nothing (the server sends no further).
 * <p>
 * Presentation only. The client sets it ({@link #configure}) and starts each tick's budget ({@link #newTick}); a block's
 * {@code animateTick}, which only runs on clients, reads it. The server never decides anything from it, which a game
 * test checks by casting with every setting.
 */
public final class Presentation {
	/** How far away a sign is drawn at all, and sent: blocks. */
	public static final double FAR = 48.0;
	/** Beyond this a sign draws half its particles. */
	public static final double NEAR = 24.0;
	/** The share of a tick's budget a block's ambient effects may use (in hundredths). */
	public static final int AMBIENT_SHARE = 50;
	/** How many particles a warning draws at the least, at any intensity within {@link #FAR}. */
	public static final int WARNING_FLOOR = 2;
	/** How much rarer a warning state's ambient effect is at minimal intensity. */
	public static final int WARNING_RARITY = 6;

	/** The player's choice: how many particles the Concordance draws. Keep equal to INTENSITY in tools/concordance_signs.py. */
	public enum Intensity {
		/** Every particle, up to 160 a tick. */
		FULL(160, 100, 1),
		/** Half the particles of each sign, ambient sparkles a third as often, up to 64 a tick. */
		REDUCED(64, 50, 3),
		/** Warnings only (a little), no ambient sparkles, up to 16 a tick. */
		MINIMAL(16, 0, 0);

		/** Particles a tick, shared by every Concordance effect on this client. */
		public final int budget;
		/** The share of each sign's particles drawn (percent); warnings never fall below the floor. */
		public final int share;
		/** How much rarer ambient sparkles are (0: none). */
		public final int ambientRarity;

		Intensity(int budget, int share, int ambientRarity) {
			this.budget = budget;
			this.share = share;
			this.ambientRarity = ambientRarity;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static Intensity fromId(@Nullable String id) {
			for (Intensity intensity : values()) {
				if (intensity.id().equals(id)) {
					return intensity;
				}
			}
			return FULL;
		}
	}

	private static volatile Intensity intensity = Intensity.FULL;
	private static volatile boolean reducedMotion;
	private static int spent;
	private static int ambientSpent;

	private Presentation() {
	}

	/** The client's settings, as its options screen and file hold them. */
	public static void configure(Intensity chosen, boolean calm) {
		intensity = chosen;
		reducedMotion = calm;
	}

	public static Intensity intensity() {
		return intensity;
	}

	/** Reduced motion: calmer effects (no drifting, no sliding) and fewer ambient sparkles. */
	public static boolean reducedMotion() {
		return reducedMotion;
	}

	/** A new client tick: the budget is whole again. */
	public static void newTick() {
		spent = 0;
		ambientSpent = 0;
	}

	/**
	 * How many of {@code sign}'s particles to draw {@code distance} blocks away at {@code intensity} (before the
	 * budget): its count scaled by the intensity's share, halved beyond {@link #NEAR}, none beyond {@link #FAR}; a
	 * warning never fewer than {@link #WARNING_FLOOR}.
	 */
	public static int count(Sign sign, Intensity intensity, double distance) {
		if (!(distance <= FAR)) {
			return 0;
		}
		int count = (sign.particles * intensity.share + 99) / 100;
		if (distance > NEAR) {
			count = (count + 1) / 2;
		}
		return sign.kind.warns() ? Math.max(count, WARNING_FLOOR) : count;
	}

	/**
	 * Takes up to {@code wanted} particles from this tick's budget and returns how many may be drawn. A warning may
	 * always draw its floor, over the budget, so no setting and no crowd of effects hides one.
	 */
	public static int take(int wanted, boolean warning) {
		int left = Math.max(0, intensity.budget - spent);
		int granted = Math.min(wanted, warning ? Math.max(left, WARNING_FLOOR) : left);
		spent += granted;
		return Math.max(0, granted);
	}

	/**
	 * Whether a block's ambient effect shows on this display tick: 1 in {@code chance} at full intensity ({@code calm}
	 * with reduced motion), rarer at reduced intensity, never at minimal, and only while the ambient share of this
	 * tick's budget lasts.
	 */
	public static boolean ambient(RandomSource random, int chance, int calm) {
		return ambient(random, chance, calm, false);
	}

	/**
	 * As {@link #ambient(RandomSource, int, int)}; a {@code warning} state (a searing mixture, a damaged spire) is never
	 * hidden: at minimal intensity it still shows, {@value #WARNING_RARITY} times rarer, and it does not wait on the
	 * ambient share.
	 */
	public static boolean ambient(RandomSource random, int chance, int calm, boolean warning) {
		Intensity now = intensity;
		int rarity = now.ambientRarity == 0 ? (warning ? WARNING_RARITY : 0) : now.ambientRarity;
		if (rarity == 0) {
			return false;
		}
		int odds = (reducedMotion ? calm : chance) * rarity;
		if (random.nextInt(Math.max(1, odds)) != 0 || !warning && ambientSpent * 100 >= now.budget * AMBIENT_SHARE) {
			return false;
		}
		ambientSpent++;
		spent++;
		return true;
	}
}
