package io.github.jimbozoomer.jugcraft.season;

/**
 * Whether winter snow is falling now, as each side knows it. The server sets its own from its settings and date; a
 * client sets its own from the server's {@link SeasonPayload} and clears it on leaving. In singleplayer both live in
 * one game, so they are kept apart: {@link #snowing()} answers the server's on the server's thread and the client's
 * everywhere else (biomes ask it from both sides).
 */
public final class SeasonState {
	private static volatile boolean serverSnowing;
	private static volatile boolean clientSnowing;
	private static volatile Thread serverThread;

	private SeasonState() {
	}

	/** Whether winter snow falls now in biomes tagged {@code #jugcraft:has_winter_snow}, for the side asking. */
	public static boolean snowing() {
		return Thread.currentThread() == serverThread ? serverSnowing : clientSnowing;
	}

	/** The server's own, for server code (snow laying and melting, the command, what players are sent). */
	public static boolean serverSnowing() {
		return serverSnowing;
	}

	static void setServerSnowing(boolean value) {
		serverSnowing = value;
	}

	/** What the server last told this client (false when not connected). */
	public static void setClientSnowing(boolean value) {
		clientSnowing = value;
	}

	/** Called on the server's thread as it starts. */
	static void serverStarted() {
		serverThread = Thread.currentThread();
		serverSnowing = false;
	}

	static void serverStopped() {
		serverThread = null;
		serverSnowing = false;
	}
}
