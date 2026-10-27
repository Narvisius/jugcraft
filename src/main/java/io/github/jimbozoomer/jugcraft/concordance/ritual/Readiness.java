package io.github.jimbozoomer.jugcraft.concordance.ritual;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What a circle still needs before a rite can begin, or what a running rite has spent apart from what it still holds
 * (Concordance Chunk C, C-03; plan 03:107, 03:122, 09:132, 10:48): every shortfall at once, in the shared diagnostic
 * vocabulary. Pure, and no second validator (12:174): built from the anchor's cached shape report, one reading of each
 * channel (at most {@link StructurePattern#MAX_CHANNELS}), the reader's Focus and place, and the light; it changes nothing.
 * The start rule is not this: a start still asks each channel for one step's Ley Charge ({@link RitualMachine#fault}), so a
 * channel short only of the whole run is a warning (C-P10) that never refuses.
 *
 * @param rite  the rite this is about; null when the offerings answer only a rite the reader has not learned (never named)
 * @param lines every shortfall in order: offerings, channels, place, Focus, light, the shape
 * @param spent while a rite holds the anchor or has just completed in it, what it has spent; null before it begins
 * @param held  what a running rite holds until it ends, by item in slot order; empty otherwise
 */
public record Readiness(@Nullable RitualDefinition rite, List<Line> lines, @Nullable Spent spent, List<Held> held) {
	/** Offerings that answer only a rite the reader has not learned: nothing more is said, and it is not named. */
	public static final Readiness UNKNOWN = new Readiness(null, List.of(), null, List.of());

	public Readiness {
		lines = List.copyOf(lines);
		held = List.copyOf(held);
	}

	/** One channel as read now: whether a Ley Pylon stands there (loaded), its charge, and whether it lends to the reader or the run. */
	public record Channel(StructurePattern.Offset offset, String where, boolean pylon, long ley, boolean lends) {
		/** The pylon in a reader's words: where it stands, and where that is from the anchor. */
		public Text words() {
			return Text.of("ritual.pylon_at", where, offset.x() + ", " + offset.y() + ", " + offset.z());
		}
	}

	/** A shortfall: what it is, whether it refuses a start (or, running, stops it at the next step), and its channel, if any. */
	public record Line(Diagnostic diagnostic, boolean blocks, StructurePattern.@Nullable Offset at) {
	}

	/** The reader as the start would judge its leader: their Focus, whether they stand near enough, the light above the anchor. */
	public record Reader(int focus, boolean near, int light) {
	}

	/** What a run has spent: Focus from each participant, Ley Charge from each channel each step drawn, and what is to come. */
	public record Spent(int focus, int ley, int done, int left, int leyLeft) {
		public static Spent of(RitualDefinition rite, RitualRun run, int channels) {
			int done = Math.clamp(run.done(), 0, rite.steps());
			int left = run.phase() == RitualMachine.Phase.COMPLETE ? 0 : rite.steps() - done;
			return new Spent(rite.focus() * run.joined().size(), done * rite.ley() * channels, done, left, left * rite.ley() * channels);
		}
	}

	/** An offering held by a running rite: an item id and how many. */
	public record Held(String item, int count) {
	}

	/** How an attempt broke off, and what it had spent (null when the rules no longer know its rite). Never saved. */
	public record Ending(RitualMachine.Interruption reason, @Nullable Spent spent) {
	}

	/** Whether nothing refuses a start for this reader now. */
	public boolean ready() {
		return rite != null && spent == null && lines.stream().noneMatch(Line::blocks);
	}

	/** What it has spent, in words: so far while it runs, in all once complete; null before it begins. */
	public @Nullable Text spentText() {
		if (spent == null) {
			return null;
		}
		return spent.left() == 0 && held.isEmpty() ? Text.of("ritual.spent_all", spent.focus(), spent.ley())
				: Text.of("ritual.spent", spent.focus(), spent.ley(), spent.done(), spent.done() + spent.left());
	}

	/** Before the chosen rite begins, for one reader as its leader. Null when no rite is chosen and no unknown one matched. */
	public static @Nullable Readiness before(Choice choice, StructurePattern pattern, StructureValidator.Report shape,
			List<Channel> channels, Reader reader) {
		RitualDefinition rite = choice.rite();
		if (rite == null) {
			return choice.unknown() != null ? UNKNOWN : null;
		}
		List<Line> lines = new ArrayList<>();
		for (Offerings.Shortfall shortfall : choice.chosen() == null ? choice.missing() : List.<Offerings.Shortfall>of()) {
			RitualDefinition.Offering offering = shortfall.offering();
			Object what = offering.tag() ? offering.spec() : new Text.Ref("item", offering.item());
			lines.add(new Line(Diagnostic.of(Code.MISSING_INGREDIENT, offering.count() - shortfall.have(), what), true, null));
		}
		for (Channel channel : channels) {
			if (!channel.pylon()) {
				continue; // the shape report names it: missing, the wrong block or not loaded
			}
			if (!channel.lends()) {
				lines.add(new Line(Diagnostic.of(Code.NOT_LENT, channel.words()), true, channel.offset()));
			} else if (channel.ley() < rite.ley()) {
				lines.add(new Line(Diagnostic.of(Code.UNAVAILABLE_POWER, channel.words(), Text.of("ritual.ley", (int) channel.ley()),
						Text.of("ritual.ley_step", rite.ley())), true, channel.offset()));
			} else if (channel.ley() < (long) rite.ley() * rite.steps()) {
				lines.add(new Line(Diagnostic.of(Code.UNAVAILABLE_POWER, channel.words(), Text.of("ritual.ley", (int) channel.ley()),
						Text.of("ritual.ley_run", rite.ley() * rite.steps(), (int) (channel.ley() / rite.ley()) + 1)), false, channel.offset()));
			}
		}
		if (!reader.near()) {
			lines.add(new Line(Diagnostic.of(Code.TOO_FAR, RitualMachine.participantRange(pattern), RitualMachine.PARTICIPANT_HEIGHT), true, null));
		}
		if (reader.focus() < rite.focus()) {
			lines.add(new Line(Diagnostic.of(Code.NEEDS_FOCUS, reader.focus(), rite.focus()), true, null));
		}
		if (rite.maxLight() != null && reader.light() > rite.maxLight()) {
			lines.add(new Line(Diagnostic.of(Code.TOO_BRIGHT, reader.light(), rite.maxLight()), true, null));
		}
		List<StructureValidator.Fault> faults = shapeFaults(shape);
		if (!faults.isEmpty()) {
			lines.add(new Line(Diagnostic.of(Code.INCOMPLETE_PATTERN, new Text.Ref("structure", pattern.id()), faults.size()), true, null));
		}
		return new Readiness(rite, lines, null, List.of());
	}

	/**
	 * While a rite holds the anchor or has just completed in it: spent apart from held, and what may still stop it at its
	 * next step (a channel short or unlent, or its pylon gone or unloaded since the last step; the light above the rite's
	 * limit; while gathering, who has still to join). A participant who has walked away is not listed, nor another part
	 * broken since the last step: the next step's own check finds them.
	 */
	public static Readiness running(RitualDefinition rite, RitualRun run, List<Channel> channels, long now, int light) {
		Spent spent = Spent.of(rite, run, channels.size());
		List<Line> lines = new ArrayList<>();
		if (run.phase().reserving()) {
			int missing = 0;
			for (Channel channel : channels) {
				if (!channel.pylon()) {
					missing++; // removed or unloaded since the last step: the shape report shown is that step's
					continue;
				}
				if (!channel.lends()) {
					lines.add(new Line(Diagnostic.of(Code.NOT_LENT, channel.words()), true, channel.offset()));
				} else if (rite.ley() > 0 && channel.ley() < (long) rite.ley() * spent.left()) {
					int breaksAt = spent.done() + (int) (channel.ley() / rite.ley()) + 1;
					lines.add(new Line(Diagnostic.of(Code.UNAVAILABLE_POWER, channel.words(), Text.of("ritual.ley", (int) channel.ley()),
							Text.of("ritual.ley_left", rite.ley() * spent.left(), breaksAt)), channel.ley() < rite.ley(), channel.offset()));
				}
			}
			if (missing > 0) {
				lines.add(new Line(Diagnostic.of(Code.INCOMPLETE_PATTERN, new Text.Ref("structure", rite.structure()), missing), true, null));
			}
			if (rite.maxLight() != null && light > rite.maxLight()) {
				lines.add(new Line(Diagnostic.of(Code.TOO_BRIGHT, light, rite.maxLight()), true, null));
			}
			if (run.phase() == RitualMachine.Phase.GATHERING) {
				long left = Math.max(0L, RitualMachine.GATHER_TICKS - (now - run.since()));
				lines.add(new Line(Diagnostic.of(Code.PARTICIPANTS, run.joined().size(), rite.participants(), (int) ((left + 19) / 20)), false, null));
			}
		}
		return new Readiness(rite, lines, spent, holding(run.plan()));
	}

	/** A report's own shape faults: missing, wrong, obstructed or unloaded parts. Charge and ownership are read from the channels. */
	public static List<StructureValidator.Fault> shapeFaults(StructureValidator.Report report) {
		List<StructureValidator.Fault> out = new ArrayList<>();
		for (StructureValidator.Fault fault : report.faults()) {
			if (fault.problem() != StructureValidator.Problem.UNPOWERED && fault.problem() != StructureValidator.Problem.FOREIGN) {
				out.add(fault);
			}
		}
		return out;
	}

	/** What a plan holds, by item, in the order of the anchor's slots. */
	public static List<Held> holding(Offerings.@Nullable Plan plan) {
		if (plan == null) {
			return List.of();
		}
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (int i = 0; i < plan.take().size(); i++) {
			int take = plan.take().get(i);
			if (take > 0) {
				counts.merge(plan.items().get(i), take, Integer::sum);
			}
		}
		List<Held> out = new ArrayList<>();
		counts.forEach((item, count) -> out.add(new Held(item, count)));
		return out;
	}

	/**
	 * The rite's stage and payment table (C-04's form; plan 03:107): what Begin pays, the gathering for more than one, what
	 * each step draws and what is spent after it, what the commit consumes and makes, what waits, what an interruption keeps
	 * and loses, and the backlash, stated before anyone begins (03:122). One line a stage, from the rite's data alone.
	 */
	public static List<Text> stages(RitualDefinition rite, int channels) {
		int focus = rite.focus() * rite.participants();
		int step = rite.ley() * channels;
		int run = Math.toIntExact(rite.leyPerRun(channels));
		List<Text> out = new ArrayList<>();
		out.add(Text.of("ritual.stage.offer"));
		out.add(Text.of("ritual.stage.begin", rite.focus()));
		if (rite.participants() > 1) {
			out.add(Text.of("ritual.stage.gather", rite.participants() - 1, RitualMachine.GATHER_TICKS / 20, rite.focus(), focus));
		}
		out.add(Text.of("ritual.stage.steps", rite.steps(), Text.seconds(RitualMachine.STEP_TICKS), rite.ley(), channels, step, focus, step));
		if (rite.result() instanceof RitualDefinition.Transform transform) {
			Text.Ref made = new Text.Ref("item", transform.into());
			out.add(Text.of("ritual.stage.commit.transform", rite.steps(), made, focus, run));
			out.add(Text.of("ritual.stage.complete", made));
		} else {
			out.add(Text.of("ritual.stage.commit.effects", rite.steps(), focus, run));
		}
		out.add(Text.of("ritual.stage.broken"));
		out.add(rite.backlash() > 0 ? Text.of("ritual.stage.backlash", rite.backlash()) : Text.of("ritual.stage.no_backlash"));
		return out;
	}
}
