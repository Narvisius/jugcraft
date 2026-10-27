package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Stolas's Regard (Concordance Chunk D, D-03; docs/features/stolas-pact.md): what a player has shown Stolas and what he
 * waits for next, read from their own research and the server's record of what they have seen in the sky. Nothing here
 * records or grants anything: Styxhexenhammer's hint and the journal's pacts section only read, so talking teaches nothing.
 * <ul>
 * <li>The research ({@value #RESEARCH}, tools/concordance_pacts.py) needs the Binding Arts understood. It is encountered by
 * examining one of {@link #SPECIMENS} (three of Styx's flowers and a feather, which anyone has without Styx), observed by
 * cultivation, understood by assay and mastered by the pact's first obligation ({@value #ACTIVITY}); no state takes notes.</li>
 * <li>The sky step, the invitation's precondition (D-04), is one observation recorded at an Orrery Observatory
 * ({@link AstralClaims#observed}). It is no research, so {@link ResearchEngine#explain} never names it: Styx and the journal
 * name it apart.</li>
 * </ul>
 */
public final class StolasRegard {
	public static final String RESEARCH = "jugcraft:stolas_regard";
	/** The practice the pact's first obligation records (D-05), which masters Stolas's Regard. Its gate is practice/pact_service. */
	public static final String ACTIVITY = "jugcraft:pact_service";
	/** Things Stolas would look at closely: the Styx flowers, and a feather for a world without Styx. */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("stolas_specimens"));

	/** What Stolas waits for next (tools/concordance_pacts.py STEPS, in this order; Styx's words are tools/styx.py's). */
	public enum Step {
		BINDING("binding"),
		EXAMINE("examine"),
		CULTIVATE("cultivate"),
		ASSAY("assay"),
		SERVE("serve"),
		KEPT("kept"),
		JOURNAL("journal");

		public final String id;

		Step(String id) {
			this.id = id;
		}
	}

	private StolasRegard() {
	}

	/**
	 * What Stolas waits for next from someone with {@code knowledge}, by the entry's own rules ({@link ResearchEngine#explain}):
	 * null for someone who has not begun the Binding Arts, or when the rules hold no Stolas's Regard (a data pack without it).
	 * A way in this class does not know (a data pack's) is {@link Step#JOURNAL}.
	 */
	public static @Nullable Step step(Knowledge knowledge, ConcordanceRules rules, EvidenceRule.TagLookup tags) {
		if (knowledge.state(Workers.RESEARCH) == ResearchState.NONE) {
			return null;
		}
		return switch (ResearchEngine.explain(knowledge, rules, RESEARCH, tags).getFirst()) {
			case ResearchEngine.Explanation.Unknown unknown -> null;
			case ResearchEngine.Explanation.NeedsPrerequisite needs ->
					needs.requirement().research().equals(Workers.RESEARCH) ? Step.BINDING : Step.JOURNAL;
			case ResearchEngine.Explanation.Complete complete -> Step.KEPT;
			case ResearchEngine.Explanation.Next next ->
					next.alternatives().isEmpty() ? Step.JOURNAL : of(next.alternatives().getFirst().rule());
		};
	}

	/** {@link #step(Knowledge, ConcordanceRules, EvidenceRule.TagLookup)} for {@code player} under the loaded rules. */
	public static @Nullable Step step(Player player) {
		return step(ConcordanceProgress.knowledge(player), ConcordanceData.rules(), ConcordanceProgress.TAG_LOOKUP);
	}

	private static Step of(EvidenceRule rule) {
		return switch (rule.kind()) {
			case EXAMINE -> Step.EXAMINE;
			case PRACTICE -> Garden.ACTIVITY.equals(rule.activity()) ? Step.CULTIVATE
					: Assaying.ACTIVITY.equals(rule.activity()) ? Step.ASSAY
					: ACTIVITY.equals(rule.activity()) ? Step.SERVE : Step.JOURNAL;
			case STUDY, INVOKE, NOTES -> Step.JOURNAL;
		};
	}

	/** How far {@code player} has come with Stolas's Regard. */
	public static ResearchState state(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH);
	}

	/** The patterns {@code player} has observed at an observatory, by the server's record, with when they last did. */
	public static Map<String, Long> observed(ServerPlayer player) {
		return AstralClaims.of(player.level().getServer()).observed(player.getUUID());
	}

	/** The sky step (D-S12): one observation recorded, ever, whatever the sky is now. Nothing is recorded for it after the fact. */
	public static boolean skySeen(ServerPlayer player) {
		return !observed(player).isEmpty();
	}

	/**
	 * What Styxhexenhammer adds for {@code listener} (D-03): their own next step and, until they have recorded one, the sky
	 * step; nothing for someone who has not begun the Binding Arts, or with the Concordance off.
	 */
	public static List<Component> hint(ServerPlayer listener) {
		if (!Workers.enabled()) {
			return List.of();
		}
		Step step = step(listener);
		if (step == null) {
			return List.of();
		}
		List<Component> lines = new ArrayList<>(2);
		lines.add(Component.translatable("message.jugcraft.styx.stolas." + step.id));
		if (step != Step.KEPT && !skySeen(listener)) {
			lines.add(Component.translatable("message.jugcraft.styx.stolas.sky"));
		}
		return List.copyOf(lines);
	}

	/**
	 * The journal's pacts section (D-03 begins it; D-07 adds the pacts): Stolas's Regard's next step with its state, and the
	 * sky step apart, with the number of patterns observed (none of these when the rules hold no Stolas's Regard); then each
	 * pact and agreement the player holds with its next obligation, and their departures (Roster.report, Concordance Chunk D, D-07).
	 */
	public static void report(ServerPlayer player, Journal.Report out) {
		if (ConcordanceData.rules().research(RESEARCH) != null) {
			Step step = step(player);
			if (step != null) {
				out.line(Component.translatable("journal.jugcraft.pacts.regard." + step.id), Component.translatable("journal.jugcraft.research.has",
						Component.translatable("journal.jugcraft.state." + state(player).id())));
			}
			Map<String, Long> seen = observed(player);
			out.line(Component.translatable(seen.isEmpty() ? "journal.jugcraft.pacts.sky_needed" : "journal.jugcraft.pacts.sky_recorded"),
					Component.translatable("journal.jugcraft.pacts.sky_count", seen.size()));
		}
		Roster.report(player, out);
	}
}
