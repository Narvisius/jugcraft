package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.JournalClient;
import io.github.jimbozoomer.jugcraft.client.JournalScreen;
import io.github.jimbozoomer.jugcraft.client.JournalWorkspace;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Roadmap step 26 in a real client: the Concordance Journal crosses from the server and opens. A player who knows First
 * Light and has met the Crimson Rites asks for their journal; it arrives, the simple journal shows it (screenshot
 * {@code jugcraft_concordance_journal_simple}), and, with GuiLib installed as in CI's client jobs, so does the GuiLib
 * workspace ({@code jugcraft_concordance_journal_workspace}). CI job {@code client}.
 */
public class ConcordanceJournalClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
				ConcordanceProgress.grant(player, Vigil.RESEARCH, ResearchState.ENCOUNTERED);
			});
			context.runOnClient(client -> JournalClient.request());
			context.waitFor(client -> JournalClient.received() && !JournalClient.sections().isEmpty());
			context.setScreen(JournalScreen::new);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_concordance_journal_simple");
			if (FabricLoader.getInstance().isModLoaded("guilib")) {
				context.setScreen(() -> null);
				context.runOnClient(client -> JournalWorkspace.open());
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_concordance_journal_workspace");
			}
			context.setScreen(() -> null);
		}
	}
}
