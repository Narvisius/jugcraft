package io.github.jimbozoomer.jugcraft.test;

import com.klikli_dev.modonomicon.client.gui.book.node.BookParentNodeScreen;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaClient;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaRoutesScreen;
import io.github.jimbozoomer.jugcraft.client.HandbookScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Both guide routes open from the shared encyclopedia and preserve handbook/back navigation. */
public class EncyclopediaRoutesClientGameTests implements FabricClientGameTest {
	/** Use the pinned game's input codes (26.3 uses SDL, not GLFW). */
	private static final int ESCAPE = InputConstants.KEY_ESCAPE;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getServer().runCommand("gamemode survival @a");
			singleplayer.getConnection().waitForChunksRender();

			// The key, from the game.
			context.getInput().pressKey(EncyclopediaClient.key());
			context.waitForScreen(EncyclopediaScreen.class);
			context.runOnClient(client -> ((Button) ((EncyclopediaScreen) client.gui.screen())
					.find(Component.translatable("screen.jugcraft.routes.open")))
					.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
			context.waitForScreen(EncyclopediaRoutesScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_encyclopedia_home");

			// Technology: a milestone opens the handbook at its stage, which closes back to the route.
			context.runOnClient(client -> ((EncyclopediaRoutesScreen) client.gui.screen()).showRoute(0));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_encyclopedia_technology");
			context.getInput().pressKey(InputConstants.KEY_DOWN);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitForScreen(HandbookScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_encyclopedia_handbook");
			context.getInput().pressKey(ESCAPE);
			context.waitForScreen(EncyclopediaRoutesScreen.class);
			int route = context.computeOnClient(client -> ((EncyclopediaRoutesScreen) client.gui.screen()).route());
			if (route != 0) {
				throw new AssertionError("Esc in the handbook should return to the technology route, not " + route);
			}

			// Magic: the codex is loaded, and a milestone opens it at its category.
			context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
			context.waitFor(client -> client.gui.screen() instanceof EncyclopediaRoutesScreen screen && screen.route() < 0);
			boolean codex = context.computeOnClient(client -> BookDataManager.get().getBook(Jugcraft.id("arcane_concordance")) != null);
			if (!codex) {
				throw new AssertionError("The Arcane Concordance codex is not loaded on the client");
			}
			context.runOnClient(client -> ((EncyclopediaRoutesScreen) client.gui.screen()).showRoute(1));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_encyclopedia_magic");
			context.getInput().pressKey(InputConstants.KEY_DOWN);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitForScreen(BookParentNodeScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_encyclopedia_codex");
			context.setScreen(() -> null);

			// The guide directory is a child of the live encyclopedia; closing returns there.
			context.getInput().pressKey(EncyclopediaClient.key());
			context.waitForScreen(EncyclopediaScreen.class);
			context.runOnClient(client -> ((Button) ((EncyclopediaScreen) client.gui.screen())
					.find(Component.translatable("screen.jugcraft.routes.open")))
					.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
			context.waitForScreen(EncyclopediaRoutesScreen.class);
			context.getInput().pressKey(ESCAPE);
			context.waitForScreen(EncyclopediaScreen.class);
			context.getInput().pressKey(ESCAPE);
			context.waitForScreen(null);
		}
	}
}
