package io.github.jimbozoomer.jugcraft.concordance.journal;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy;
import io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The Concordance Journal (roadmap step 26): everything the server knows about one player's Concordance work and what
 * stands in its way, in one place. Research (each entry's state, what the next one asks in words and, as exact figures,
 * how far each way to it has come), the stage and every route onward, and each tradition's own report (Vitae, the sky,
 * workers, deliveries, relics, curses and wards, the Conclave, spires, Prima Materia), each shown once its research is
 * met (or, for curses, while one touches the player).
 * <p>
 * The reports are the same ones the chat commands print: each feature writes its lines once, and the commands and the
 * journal show them. Nothing here decides anything: the journal reads the server's own records when the player asks
 * (at most once every {@value #REQUEST_TICKS} ticks) and only for that player; research a player has not yet
 * encountered is counted, never named.
 */
public final class Journal {
	public static final int MAX_SECTIONS = 24;
	public static final int MAX_LINES = 96;
	/** How often a player may ask for their journal (ticks). */
	public static final int REQUEST_TICKS = 10;

	/** Where a report writes: a line in words and, optionally, the exact figures behind it. */
	@FunctionalInterface
	public interface Report {
		void line(Component text, @Nullable Component exact);

		default void line(Component text) {
			line(text, null);
		}
	}

	/** One section's source: shown only when {@code shown} holds for the player (and it writes anything). */
	private record Contributor(String id, Predicate<ServerPlayer> shown, BiConsumer<ServerPlayer, Report> report) {
	}

	private static final List<Contributor> CONTRIBUTORS = new ArrayList<>();

	private Journal() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(JournalRequestPayload.TYPE, JournalRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(JournalPayload.TYPE, JournalPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(JournalRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (RateGate.allow(player, "journal", REQUEST_TICKS)) {
				ServerPlayNetworking.send(player, new JournalPayload(build(player)));
			}
		});
		contribute("overview", player -> true, Journal::overview);
		contribute("research", player -> true, Journal::research);
		contribute("stage", player -> true, (player, out) -> StageProgress.describe(player, out::line));
		contribute("vitae", knows(Vigil.RESEARCH), (player, out) -> Vigil.status(player, out::line));
		contribute("sky", knows(Sky.RESEARCH), (player, out) -> Sky.forecast(Sky.time(player.level()), Sky.FORECAST_DAYS).forEach(out::line));
		contribute("workers", knows(Workers.RESEARCH), (player, out) -> Workers.list(player, out::line));
		contribute("logistics", knows(Workers.RESEARCH), (player, out) -> Couriers.list(player, out::line));
		contribute("relics", knows(Reliquary.RESEARCH), (player, out) -> Reliquary.report(player, out::line));
		contribute("hexes", knows(Sympathy.RESEARCH).or(Sympathy::touched), (player, out) -> Sympathy.report(player, out::line));
		contribute("conclave", player -> Starbound.standing(player).member(), (player, out) -> {
			Starbound.status(player, out::line);
			Starbound.commissions(player, out::line);
		});
		contribute("spires", player -> true, Journal::spires);
		contribute("assay", knows(Assaying.RESEARCH), (player, out) -> out.line(
				Component.translatable("journal.jugcraft.assay.balance", Assaying.balance(player)),
				Component.translatable("journal.jugcraft.assay.cap", Assay.MAX_BALANCE)));
	}

	/** Adds a section, in the order sections are shown. */
	public static void contribute(String id, Predicate<ServerPlayer> shown, BiConsumer<ServerPlayer, Report> report) {
		CONTRIBUTORS.add(new Contributor(id, shown, report));
	}

	private static Predicate<ServerPlayer> knows(String research) {
		return player -> ConcordanceProgress.knowledge(player).state(research) != ResearchState.NONE;
	}

	/** The journal as {@code player} sees it now (the request handler and tests call this). */
	public static List<JournalSection> build(ServerPlayer player) {
		List<JournalSection> sections = new ArrayList<>();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			sections.add(new JournalSection("disabled", title("overview"),
					List.of(new JournalLine(Component.translatable("message.jugcraft.concordance.disabled"), Optional.empty()))));
			return sections;
		}
		for (Contributor contributor : CONTRIBUTORS) {
			if (sections.size() >= MAX_SECTIONS || !contributor.shown().test(player)) {
				continue;
			}
			List<JournalLine> lines = new ArrayList<>();
			try {
				contributor.report().accept(player, (text, exact) -> {
					if (lines.size() < MAX_LINES) {
						lines.add(new JournalLine(text, Optional.ofNullable(exact)));
					}
				});
			} catch (RuntimeException failure) {
				// One feature's fault must not hide the others: the section says it could not be read, the log says why.
				Jugcraft.LOGGER.warn("Concordance Journal: the {} section failed for {}", contributor.id(), player.getName().getString(), failure);
				lines.clear();
				lines.add(new JournalLine(Component.translatable("journal.jugcraft.unreadable"), Optional.empty()));
			}
			if (!lines.isEmpty()) {
				sections.add(new JournalSection(contributor.id(), title(contributor.id()), lines));
			}
		}
		return sections;
	}

	public static Component title(String section) {
		return Component.translatable("journal.jugcraft.section." + section);
	}

	// ---------------------------------------------------------------- the journal's own sections

	/** Focus, the stage reached, and how much of the Concordance is still unknown. */
	private static void overview(ServerPlayer player, Report out) {
		out.line(Component.translatable("journal.jugcraft.overview.focus", ConcordanceProgress.currentFocus(player), FocusPool.MAX));
		StageDefinition stage = StageProgress.stage(player);
		out.line(stage == null ? Component.translatable("journal.jugcraft.overview.no_stage")
				: Component.translatable("journal.jugcraft.overview.stage", StageProgress.name(stage.id())));
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		int known = 0;
		int mastered = 0;
		int total = 0;
		for (Definitions.Research entry : ConcordanceData.rules().research().values()) {
			ResearchState state = knowledge.state(entry.id());
			total++;
			known += state == ResearchState.NONE ? 0 : 1;
			mastered += state == ResearchState.MASTERED ? 1 : 0;
		}
		out.line(Component.translatable("journal.jugcraft.overview.research", known, mastered), Component.translatable("journal.jugcraft.overview.research_total", total));
	}

	/**
	 * Every research entry the player has met: its state, and what the next state asks in words (the advancement's own
	 * description) with, as exact figures, how far each way to it has come; or which entry must come first. Entries not
	 * yet encountered are only counted.
	 */
	public static void research(ServerPlayer player, Report out) {
		ConcordanceRules rules = ConcordanceData.rules();
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		int unknown = 0;
		for (Definitions.Research entry : rules.research().values()) {
			ResearchState state = knowledge.state(entry.id());
			if (state == ResearchState.NONE) {
				unknown++;
				continue;
			}
			Component name = ConcordanceProgress.researchName(entry.id());
			Component stateName = stateName(state);
			for (ResearchEngine.Explanation explanation : ResearchEngine.explain(knowledge, rules, entry.id(), ConcordanceProgress.TAG_LOOKUP)) {
				switch (explanation) {
					case ResearchEngine.Explanation.Unknown missing -> out.line(Component.translatable("journal.jugcraft.research.unknown", name));
					case ResearchEngine.Explanation.Complete complete -> out.line(Component.translatable("journal.jugcraft.research.complete", name, stateName));
					case ResearchEngine.Explanation.NeedsPrerequisite needs -> out.line(Component.translatable("journal.jugcraft.research.needs", name,
							stateName, ConcordanceProgress.researchName(needs.requirement().research()), stateName(needs.requirement().state())),
							Component.translatable("journal.jugcraft.research.has", stateName(needs.has())));
					case ResearchEngine.Explanation.Next next -> {
						Identifier advancement = ConcordanceProgress.advancementFor(entry.id(), next.state());
						Component how = Component.translatable("advancements." + advancement.getNamespace() + "." + advancement.getPath() + ".description");
						MutableComponent exact = Component.empty();
						for (int i = 0; i < next.alternatives().size(); i++) {
							ResearchEngine.RuleProgress rule = next.alternatives().get(i);
							if (i > 0) {
								exact.append(Component.translatable("journal.jugcraft.or"));
							}
							exact.append(Component.translatable("journal.jugcraft.rule." + rule.rule().kind().id, Math.min(rule.have(), rule.need()), rule.need()));
						}
						out.line(Component.translatable("journal.jugcraft.research.next", name, stateName, stateName(next.state()), how),
								next.alternatives().isEmpty() ? null : exact);
					}
				}
			}
		}
		if (unknown > 0) {
			out.line(Component.translatable("journal.jugcraft.research.unknown_count", unknown));
		}
	}

	private static Component stateName(ResearchState state) {
		return Component.translatable("journal.jugcraft.state." + state.id());
	}

	/** The spires the player keeps or shares, or nothing at all when they keep none (the section is then left out). */
	private static void spires(ServerPlayer player, Report out) {
		List<Component> lines = new ArrayList<>();
		if (ConcordSpire.report(player, lines::add) > 0) {
			lines.forEach(out::line);
		}
	}
}
