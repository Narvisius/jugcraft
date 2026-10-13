package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalLine;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalPayload;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalSection;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Roadmap step 26, the Concordance Journal (docs/features/arcane-concordance-journal.md), on a real server: a newcomer's
 * journal names no research they have not met; what they meet appears with what its next state asks in words and, as
 * exact figures, how far they are; each tradition's section appears once its research is met; a stage route's needs are
 * words, not data; and the journal survives its trip to the client unchanged.
 */
public class ConcordanceJournalGameTests {
	private static JournalSection section(List<JournalSection> sections, String id) {
		return sections.stream().filter(section -> section.id().equals(id)).findFirst().orElse(null);
	}

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
	}

	/** A newcomer: their Focus and stage, research only counted, no tradition's section, and the stage's route in words. */
	@GameTest(maxTicks = 20)
	public void aNewcomersJournalNamesNothingUnmet(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		List<JournalSection> sections = Journal.build(player);
		helper.assertTrue(section(sections, "overview") != null && section(sections, "stage") != null, "overview and stage: " + sections);
		JournalSection research = section(sections, "research");
		helper.assertTrue(research != null && research.lines().size() == 1
				&& key(research.lines().get(0).text()).equals("journal.jugcraft.research.unknown_count"), "research is only counted: " + research);
		int entries = ConcordanceData.rules().research().size();
		helper.assertValueEqual(((TranslatableContents) research.lines().get(0).text().getContents()).getArgs()[0], entries,
				"every entry is still unknown");
		for (String hidden : List.of("vitae", "sky", "workers", "logistics", "relics", "hexes", "conclave", "spires", "assay")) {
			helper.assertTrue(section(sections, hidden) == null, "no " + hidden + " section before its research is met");
		}
		helper.succeed();
	}

	/** Met research shows its next state in words, with how far each way has come as exact figures. */
	@GameTest(maxTicks = 20)
	public void metResearchSaysWhatComesNext(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.ENCOUNTERED);
		JournalSection research = section(Journal.build(player), "research");
		JournalLine next = research.lines().stream().filter(line -> key(line.text()).equals("journal.jugcraft.research.next")).findFirst().orElse(null);
		helper.assertTrue(next != null, "First Light says what comes next: " + research);
		Object[] args = ((TranslatableContents) next.text().getContents()).getArgs();
		helper.assertTrue(args[3] instanceof Component how && key(how).equals("advancements.jugcraft.concordance_first_light_observed.description"),
				"in the words of the next state's advancement: " + args[3]);
		helper.assertTrue(next.exact().isPresent() && next.exact().get().getSiblings().stream().anyMatch(part -> key(part).startsWith("journal.jugcraft.rule.")),
				"with exact figures: " + next.exact());
		helper.assertTrue(research.lines().stream().noneMatch(line -> line.text().getString().contains("jugcraft:")), "no ids in the words");
		helper.succeed();
	}

	/** A tradition's own section appears once its research is met: here the Crimson Rites' Vitae. */
	@GameTest(maxTicks = 20)
	public void aTraditionsSectionAppearsOnceMet(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(section(Journal.build(player), "vitae") == null, "no Vitae before the Crimson Rites");
		ConcordanceProgress.grant(player, Vigil.RESEARCH, ResearchState.ENCOUNTERED);
		JournalSection vitae = section(Journal.build(player), "vitae");
		helper.assertTrue(vitae != null && !vitae.lines().isEmpty(), "Vitae once they are met: " + vitae);
		helper.succeed();
	}

	/** A stage route's needs are words (the research's name, a rank's name), not the graph's data. */
	@GameTest(maxTicks = 20)
	public void stageNeedsAreWords(GameTestHelper helper) {
		helper.assertValueEqual(key(StageProgress.need("research jugcraft:first_light@mastered")), "message.jugcraft.concordance.stage.need.research",
				"a research need");
		helper.assertValueEqual(key(StageProgress.need("rank starbound")), "message.jugcraft.concordance.stage.need.rank", "a rank need");
		helper.assertValueEqual(key(StageProgress.need("mastered 5 (2)")), "message.jugcraft.concordance.stage.need.mastered", "a count");
		helper.assertValueEqual(key(StageProgress.need("milestone jugcraft:spire_raised")), "message.jugcraft.concordance.stage.need.milestone",
				"a milestone");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		JournalSection stage = section(Journal.build(player), "stage");
		helper.assertTrue(stage.lines().stream().noneMatch(line -> line.text().getString().contains("@")), "no data in the routes: " + stage);
		helper.succeed();
	}

	/** The journal crosses to the client whole: the same sections, the same lines, the same words. */
	@GameTest(maxTicks = 20)
	public void theJournalSurvivesTheTrip(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		JournalPayload sent = new JournalPayload(Journal.build(player));
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		JournalPayload.CODEC.encode(buffer, sent);
		JournalPayload received = JournalPayload.CODEC.decode(buffer);
		helper.assertValueEqual(received.sections().size(), sent.sections().size(), "every section");
		for (int i = 0; i < sent.sections().size(); i++) {
			JournalSection a = sent.sections().get(i);
			JournalSection b = received.sections().get(i);
			helper.assertTrue(a.id().equals(b.id()) && a.lines().size() == b.lines().size(), "section " + a.id());
			for (int j = 0; j < a.lines().size(); j++) {
				helper.assertValueEqual(b.lines().get(j).text().getString(), a.lines().get(j).text().getString(), "line " + j + " of " + a.id());
				helper.assertValueEqual(b.lines().get(j).exact().map(Component::getString), a.lines().get(j).exact().map(Component::getString),
						"exact " + j + " of " + a.id());
			}
		}
		helper.succeed();
	}
}
