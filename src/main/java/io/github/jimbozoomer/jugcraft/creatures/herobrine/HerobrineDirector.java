package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The boundary between Herobrine's schedule and his encounters (docs/features/herobrine.md). The schedule decides who is
 * due an encounter and when, remembers it per player, and owns pacing; {@link HerobrineEncounters} runs the live tick,
 * reports each player's state, places him, watches who really sees him, and reports back. Every callback runs on the
 * server thread. Until a schedule is installed with {@link HerobrineEncounters#setDirector}, {@link #NONE} asks for
 * nothing and he only appears on an operator's command.
 */
public interface HerobrineDirector {
	/** Asks for no encounters. */
	HerobrineDirector NONE = (server, candidates) -> List.of();

	/**
	 * A player the schedule may consider, once a second. {@code active}: in the Overworld, alive, not spectating or
	 * asleep, a minute past joining or respawning, and not idle (moved or looked round within
	 * {@value HerobrineEncounters#IDLE_TICKS} ticks). {@code calm}: active and safe for a close encounter (no menu open,
	 * not hurt recently, not burning, falling, drowning or badly wounded).
	 */
	record Candidate(ServerPlayer player, boolean active, boolean calm) {
	}

	/**
	 * One encounter to attempt now. {@code reservation} is the schedule's own token, handed back unchanged in every
	 * callback; an operator's command uses {@link #FORCED}.
	 */
	record Request(UUID player, Herobrine.Kind kind, long reservation) {
	}

	/** The reservation of an encounter an operator forced with a command. */
	long FORCED = -1L;

	/** The reservation of a Cursed Mirror's reflection ({@link HerobrineEncounters#reflection}); the schedule did not ask. */
	long MIRROR = -2L;

	/** Why nothing was placed. Nothing was shown, so nothing should count as seen; the schedule may retry. */
	enum Failure {
		/** The player is not online, alive and in the Overworld. */
		TARGET_GONE,
		/** A close encounter was asked for a player who is not calm, or has not yet seen him at a distance. */
		TARGET_UNSAFE,
		/** He is already out for this player. */
		ALREADY_HAUNTED,
		/** No loaded place to stand that the player could see (or, for a close encounter, no room behind them). */
		NO_VISIBLE_SPOT
	}

	/** Once a second, if {@code herobrine.haunting} is on: the encounters to attempt now. */
	List<Request> due(MinecraftServer server, List<Candidate> candidates);

	/** He is standing for {@code request}, as encounter {@code encounter}. */
	default void placed(Request request, UUID encounter) {
	}

	/** Nothing was placed for {@code request}. */
	default void failed(Request request, Failure failure) {
	}

	/**
	 * {@code player} has really seen encounter {@code encounter}: while attentive (calm, awake, not idle; see
	 * {@link HerobrineEncounters#attentive}) he was on their screen, unobstructed, for {@value Herobrine#SEEN_TICKS} ticks,
	 * or they hit him. Once per player per encounter; any player may be a witness, not only the one he came for.
	 */
	default void seen(Request request, UUID encounter, UUID player) {
	}

	/** Encounter {@code encounter} is over, for {@code outcome}; {@code witnesses} are everyone {@link #seen} named. */
	default void ended(Request request, UUID encounter, Herobrine.Outcome outcome, Set<UUID> witnesses) {
	}

	/**
	 * Whether {@code player} has ever really seen him, by the schedule's saved record (other parts of Jugcraft, such as
	 * Styx's lines, ask through {@link HerobrineEncounters#hasSeen}). By default nobody, beyond this session's sightings.
	 */
	default boolean hasSeen(UUID player) {
		return false;
	}
}
