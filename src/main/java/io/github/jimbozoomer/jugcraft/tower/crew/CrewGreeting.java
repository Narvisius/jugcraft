package io.github.jimbozoomer.jugcraft.tower.crew;

/**
 * How a tower crew member greets a player (Justin's greetings sheet, approved 5 November 2026 with a word too, now and
 * then a rude one): awake, within {@link #REACH} blocks and facing more or less the player's way, each crew member greets
 * a player at most once a minute: a wave; security and the gate guards salute; a nod with their hands full or seated at
 * work; seated with a coffee, the coffee raised; asleep, nothing. With it now and then a word in chat, at most once in
 * {@link #LINE_AGAIN} ticks from each and {@link #ANY_LINE} from anyone: for the time of day, or one in {@link #RUDE}
 * rude (and then no friendly gesture). The client draws all of it for its own player (client/CrewGreetings); the server
 * isn't asked anything. This decides which.
 */
public final class CrewGreeting {
	public static final int NONE = 0, WAVE = 1, SALUTE = 2, NOD = 3, CHEERS = 4;
	/** Ticks a greeting runs (up over 6, held, down over the last 6). */
	public static final int TICKS = 30;
	/** How near (blocks) and how nearly facing them (degrees either side) a player is greeted. */
	public static final double REACH = 4.0;
	public static final float ANGLE = 70F;
	/** Ticks between one crew member's greetings to a player, between their words, and between anyone's words. */
	public static final int AGAIN = 1200, LINE_AGAIN = 12000, ANY_LINE = 400;
	/** One word in RUDE is rude. */
	public static final int RUDE = 10;
	/** Lines in each time of day's set, and rude lines (message.jugcraft.tower_crew.greet.*). */
	public static final int LINES = 4, RUDE_LINES = 12;

	private CrewGreeting() {
	}

	/** The job a skin is for (its name less the number: controller_3 is a controller). */
	public static String job(String skin) {
		int i = skin.lastIndexOf('_');
		return i > 0 ? skin.substring(0, i) : skin;
	}

	/** The greeting for what they are doing: {@code act} and {@code prop} as the crew member's, seated or not. */
	public static int kind(String job, int act, int prop, boolean seated) {
		if (TowerCrew.asleep(act)) {
			return NONE;
		}
		if (seated) {
			return act == TowerCrew.ACT_MUG ? CHEERS : NOD;
		}
		if (prop == TowerCrew.PROP_BATON || act == TowerCrew.ACT_HOLSTER) {
			return NONE;                                        // the baton out: they're busy with it
		}
		if (prop == TowerCrew.PROP_RIFLE || act == TowerCrew.ACT_AIM) {
			// the rifle in both hands: a nod (none while aiming it, or with the armour's blade out)
			return act == TowerCrew.ACT_AIM || PlasmaBlade.out(act) ? NONE : NOD;
		}
		if (job.equals("security") || job.equals("gate_guard")) {
			return SALUTE;
		}
		return switch (prop) {
			case TowerCrew.PROP_RADIO, TowerCrew.PROP_TOOLBOX, TowerCrew.PROP_MUG, TowerCrew.PROP_TRAY, TowerCrew.PROP_CART -> NOD;
			default -> WAVE;
		};
	}

	/**
	 * The word's key: rude if {@code roll} (0 to RUDE - 1) is 0, else one for the time of day ({@code dayTime}, the
	 * world's clock: morning 05:00-12:00, afternoon, evening from 18:00, night from 22:00); {@code pick} chooses in the set.
	 */
	public static String line(long dayTime, int roll, int pick) {
		if (roll == 0) {
			return "message.jugcraft.tower_crew.greet.rude." + Math.floorMod(pick, RUDE_LINES);
		}
		int t = (int) Math.floorMod(dayTime, 24000L);
		String when = t < 6000 || t >= 23000 ? "morning" : t < 12000 ? "afternoon" : t < 16000 ? "evening" : "night";
		return "message.jugcraft.tower_crew.greet." + when + "." + Math.floorMod(pick, LINES);
	}
}
