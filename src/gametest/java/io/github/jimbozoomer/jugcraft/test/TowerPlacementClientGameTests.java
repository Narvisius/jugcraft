package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.TowerLoadScreen;
import io.github.jimbozoomer.jugcraft.client.TowerPlaceScreen;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerLoadMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Tower Core from a real client (Justin, 28 October 2026): right-clicking the ground with it places nothing
 * and opens the warning with the finished tower's size; CANCEL leaves the ground as it was; PLACE TOWER HERE places the
 * core and opens its first-layer loading screen; LOAD ALL (creative) fills it and BUILD FIRST LAYER starts the
 * foundation with tier 1 queued behind it. Screenshots of the warning and the loading screen, empty and full.
 */
public class TowerPlacementClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("tower")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// the ground in front of the spawn, the player above it looking down at it
			BlockPos ground = new BlockPos(origin.getX(), origin.getY() - 1, origin.getZ() + 8);
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(JugcraftTower.CORE.asItem()));
			});
			server.runCommand("tp @p %d.5 %d %d.5".formatted(ground.getX(), ground.getY() + 3, ground.getZ() + 2));
			context.waitTicks(10);
			context.getInput().lookAt(ground);
			context.waitTicks(2);

			// a right-click: the warning, and nothing placed
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(TowerPlaceScreen.class);
			context.waitTicks(3);
			if (server.computeOnServer(minecraft -> core(minecraft.overworld(), ground) != null)) {
				throw new AssertionError("the core was placed before PLACE TOWER HERE");
			}
			context.takeScreenshot("jugcraft_tower_place_warning");

			// CANCEL (as Esc does): still nothing
			context.runOnClient(client -> client.gui.screen().onClose());
			context.waitTicks(25);
			if (server.computeOnServer(minecraft -> core(minecraft.overworld(), ground) != null)) {
				throw new AssertionError("CANCEL placed the core");
			}

			// again, and PLACE TOWER HERE once it is live
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(TowerPlaceScreen.class);
			boolean early = context.computeOnClient(client -> ((TowerPlaceScreen) client.gui.screen()).armed());
			if (early) {
				throw new AssertionError("PLACE TOWER HERE was live at once");
			}
			context.waitTicks(25);
			context.runOnClient(client -> ((TowerPlaceScreen) client.gui.screen()).placeTower());
			context.waitForScreen(TowerLoadScreen.class);
			BlockPos placed = server.computeOnServer(minecraft -> {
				TowerCoreBlockEntity core = core(minecraft.overworld(), ground);
				return core == null ? null : core.getBlockPos();
			});
			if (placed == null) {
				throw new AssertionError("PLACE TOWER HERE did not place the core");
			}
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_load_first_layer");

			// LOAD ALL (creative: the whole first layer), then BUILD FIRST LAYER
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
					((TowerLoadScreen) client.gui.screen()).getMenu().containerId, TowerLoadMenu.LOAD_ALL));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_tower_load_first_layer_ready");
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
					((TowerLoadScreen) client.gui.screen()).getMenu().containerId, TowerLoadMenu.BUILD));
			context.waitTicks(10);
			String state = server.computeOnServer(minecraft -> {
				TowerCoreBlockEntity core = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(placed);
				return core.buildingFoundation() && core.firstLayerQueued() ? "" : "foundation " + core.foundationState() + ", queued "
						+ core.firstLayerQueued();
			});
			if (!state.isEmpty()) {
				throw new AssertionError("BUILD FIRST LAYER did not start the foundation with tier 1 queued: " + state);
			}
			boolean closed = context.computeOnClient(client -> client.gui.screen() == null);
			if (!closed) {
				throw new AssertionError("the loading screen stayed open after BUILD FIRST LAYER");
			}
		}
	}

	/** The core placed on {@code ground}: on it, or set into it. */
	private static @Nullable TowerCoreBlockEntity core(ServerLevel level, BlockPos ground) {
		if (level.getBlockEntity(ground.above()) instanceof TowerCoreBlockEntity core) {
			return core;
		}
		return level.getBlockEntity(ground) instanceof TowerCoreBlockEntity sunk ? sunk : null;
	}
}
