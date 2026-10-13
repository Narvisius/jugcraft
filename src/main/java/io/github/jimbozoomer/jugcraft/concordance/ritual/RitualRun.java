package io.github.jimbozoomer.jugcraft.concordance.ritual;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One anchor's ritual state, saved with the anchor: the phase, which ritual, who leads and who has joined, the steps
 * done, when the next check falls, the reserved offerings and how the last attempt ended. Immutable: each event returns
 * a {@link Transition}, the next state and the {@link Action}s the anchor must carry out to reach it, in order. The
 * anchor carries them out in the tick it receives them and keeps the new state only if they all succeed, so the
 * actions here are the whole of what a ritual does to the world:
 * <ul>
 * <li>{@link Pay}: once per participant, as they join;</li>
 * <li>{@link Reserve}: once, at the start;</li>
 * <li>{@link Draw}: once per step;</li>
 * <li>either {@link Consume} followed by {@link Commit} (completion), or {@link Release} (interruption), once, which
 * ends the attempt.</li>
 * </ul>
 * {@code RitualMachine} decides; this applies the decisions and is what the step 12 tests drive through every
 * interruption point.
 *
 * @param since when the current phase began (game time)
 * @param next when the next step or gathering check falls (game time)
 * @param last how the last attempt ended: null while none has, or an interruption
 * @param completed whether the last attempt completed (then {@code last} is null)
 */
public record RitualRun(RitualMachine.Phase phase, @Nullable String ritual, @Nullable UUID leader, List<UUID> joined, int done,
		long since, long next, Offerings.@Nullable Plan plan, RitualMachine.@Nullable Interruption last, boolean completed) {
	public static final RitualRun IDLE = new RitualRun(RitualMachine.Phase.IDLE, null, null, List.of(), 0, 0L, 0L, null, null, false);

	public RitualRun {
		joined = List.copyOf(joined);
		if (phase.reserving() != (plan != null)) {
			throw new IllegalArgumentException("offerings are reserved exactly while a ritual gathers or channels");
		}
		if (phase != RitualMachine.Phase.IDLE && ritual == null) {
			throw new IllegalArgumentException(phase.id + " names its ritual");
		}
	}

	public sealed interface Action {
	}

	/** Take {@code focus} Focus from a participant (checked beforehand: the anchor refuses a join it cannot pay). */
	public record Pay(UUID participant, int focus) implements Action {
	}

	/** Lock the anchor's slots: the plan's offerings are spoken for. */
	public record Reserve(Offerings.Plan plan) implements Action {
	}

	/** Draw {@code ley} Ley Charge from every channel, all or none. */
	public record Draw(int ley) implements Action {
	}

	/** Consume exactly the reserved offerings. Always directly followed by {@link Commit}. */
	public record Consume(Offerings.Plan plan) implements Action {
	}

	/** Make the ritual's result: the transformed item, or its effects (and record each participant's practice). */
	public record Commit(String ritual, List<UUID> participants) implements Action {
		public Commit {
			participants = List.copyOf(participants);
		}
	}

	/** Unlock the reserved offerings, which stay in the anchor ({@link RitualMachine.Release#KEPT}) or drop. */
	public record Release(RitualMachine.Release where, Offerings.Plan plan) implements Action {
	}

	/** Deal backlash damage to each participant present. */
	public record Backlash(int damage, List<UUID> participants) implements Action {
		public Backlash {
			participants = List.copyOf(participants);
		}
	}

	public record Transition(RitualRun next, List<Action> actions) {
		public Transition {
			actions = List.copyOf(actions);
		}
	}

	/** No change: an event that does not apply in this phase (the anchor tells the player why). */
	public Transition stay() {
		return new Transition(this, List.of());
	}

	/**
	 * The leader starts a ritual whose offerings matched as {@code plan}, paying its Focus: the offerings are reserved
	 * and it gathers, or channels at once if it needs only one participant.
	 */
	public Transition start(RitualDefinition definition, UUID leader, Offerings.Plan plan, long now) {
		if (phase != RitualMachine.Phase.IDLE) {
			return stay();
		}
		List<Action> actions = new ArrayList<>();
		actions.add(new Pay(leader, definition.focus()));
		actions.add(new Reserve(plan));
		RitualRun gathering = new RitualRun(RitualMachine.Phase.GATHERING, definition.id(), leader, List.of(leader), 0, now,
				now + RitualMachine.STEP_TICKS, plan, null, false);
		return new Transition(gathering.full(definition, now), actions);
	}

	/** Another participant joins a gathering ritual, paying its Focus. */
	public Transition join(RitualDefinition definition, UUID participant, long now) {
		if (phase != RitualMachine.Phase.GATHERING || joined.contains(participant) || joined.size() >= definition.participants()) {
			return stay();
		}
		List<UUID> more = new ArrayList<>(joined);
		more.add(participant);
		RitualRun next = new RitualRun(phase, ritual, leader, more, done, since, this.next, plan, null, false);
		return new Transition(next.full(definition, now), List.of(new Pay(participant, definition.focus())));
	}

	/** Once everyone has joined, channeling begins; its first step falls one step from now. */
	private RitualRun full(RitualDefinition definition, long now) {
		if (joined.size() < definition.participants()) {
			return this;
		}
		return new RitualRun(RitualMachine.Phase.CHANNELING, ritual, leader, joined, 0, now, now + RitualMachine.STEP_TICKS, plan,
				null, false);
	}

	/**
	 * A server tick at {@code now}, with what the anchor sees (only looked at when a check falls due: the anchor
	 * passes null otherwise, and builds the observation only when {@link #due} says so).
	 */
	public Transition tick(RitualDefinition definition, RitualMachine.@Nullable Observation seen, long now) {
		if (lapsed(now)) {
			return interrupt(definition, RitualMachine.Interruption.LAPSED);
		}
		if (!due(now) || seen == null) {
			return stay();
		}
		if (phase == RitualMachine.Phase.GATHERING) {
			RitualMachine.Decision decision = RitualMachine.gather(definition, seen, now - since);
			if (decision instanceof RitualMachine.Interrupt interrupt) {
				return interrupt(definition, interrupt.reason());
			}
			// Still waiting (everyone joining moves it on at once, in join).
			return new Transition(new RitualRun(phase, ritual, leader, joined, done, since, now + RitualMachine.STEP_TICKS, plan, null,
					false), List.of());
		}
		RitualMachine.Decision decision = RitualMachine.step(definition, done, seen);
		return switch (decision) {
			case RitualMachine.Interrupt interrupt -> interrupt(definition, interrupt.reason());
			case RitualMachine.Continue next -> new Transition(new RitualRun(phase, ritual, leader, joined, next.step(), since,
					now + RitualMachine.STEP_TICKS, plan, null, false), List.of(new Draw(definition.ley())));
			case RitualMachine.Commit commit -> {
				boolean waits = definition.result() instanceof RitualDefinition.Transform;
				RitualRun after = waits
						? new RitualRun(RitualMachine.Phase.COMPLETE, ritual, leader, joined, definition.steps(), now, now, null, null, true)
						: new RitualRun(RitualMachine.Phase.IDLE, ritual, null, List.of(), 0, now, now, null, null, true);
				yield new Transition(after, List.of(new Draw(definition.ley()), new Consume(plan), new Commit(ritual, joined)));
			}
		};
	}

	/** Whether a step or gathering check falls due at {@code now}. */
	public boolean due(long now) {
		return phase.reserving() && now >= next;
	}

	/**
	 * Whether the anchor missed a whole step: it was not run while the ritual should have been checked (its chunk
	 * stopped ticking). A ritual never catches up on missed steps; it lapses.
	 */
	public boolean lapsed(long now) {
		return phase.reserving() && now > next + RitualMachine.STEP_TICKS;
	}

	/**
	 * Stops a gathering or channeling ritual: its offerings are released as the interruption's consequence says, and
	 * backlash dealt if it lets containment fail. {@code definition} is null when the rules no longer have it. In any
	 * other phase nothing happens: there is nothing reserved to release, so an interruption can never return
	 * offerings that a completion consumed.
	 */
	public Transition interrupt(@Nullable RitualDefinition definition, RitualMachine.Interruption reason) {
		if (!phase.reserving()) {
			return stay();
		}
		List<Action> actions = new ArrayList<>();
		RitualMachine.Consequence consequence = reason.consequence();
		if (consequence.backlash() && definition != null && definition.backlash() > 0) {
			actions.add(new Backlash(definition.backlash(), joined));
		}
		actions.add(new Release(consequence.offerings(), plan));
		return new Transition(new RitualRun(RitualMachine.Phase.IDLE, ritual, null, List.of(), 0, since, next, null, reason, false),
				actions);
	}

	/** The transformed item was taken from a completed ritual: the anchor is free again. */
	public Transition collected() {
		if (phase != RitualMachine.Phase.COMPLETE) {
			return stay();
		}
		return new Transition(new RitualRun(RitualMachine.Phase.IDLE, ritual, null, List.of(), 0, since, next, null, null, true), List.of());
	}
}
