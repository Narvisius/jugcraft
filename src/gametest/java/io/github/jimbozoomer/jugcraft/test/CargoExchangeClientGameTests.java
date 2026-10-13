package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.CargoExchangeRenderChecks;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.wallrun.WallRuns;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real stock packets, scheduled intake, the original 27-slot menu, machine captures and isolated unloads. */
public class CargoExchangeClientGameTests implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("tower") || GuideScreenshotGameTests.active()) return;
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender(); var server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int alignedX = Math.floorDiv(origin.getX(), 16) * 16;
			int alignedZ = Math.floorDiv(origin.getZ(), 16) * 16;
			// The first tower's exact station, packager and terminal must occupy three distinct chunks.
			BlockPos firstCore = new BlockPos(alignedX - 35, origin.getY() - 1, alignedZ + 16), secondCore = firstCore.east(90);
			BlockPos first = firstCore.offset(20, 1, -4), second = secondCore.offset(20, 1, -4), power = firstCore.offset(-22, 1, 4);
			server.runCommand("time set noon"); server.runCommand("weather clear");
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld(); var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true; player.onUpdateAbilities();
				for (BlockPos at : new BlockPos[] {firstCore, secondCore}) {
					level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
					var core = (TowerCoreBlockEntity) level.getBlockEntity(at);
					core.setOwner(at.equals(firstCore) ? player.getUUID() : java.util.UUID.randomUUID()); core.buildInstantly(level, 1);
					var terminal = core.terminal(); terminal.rescanNow(); var store = terminal.packagerEntity();
					if (at.equals(firstCore)) {
						store.setItem(0, new ItemStack(Items.STONE_BRICKS, 16)); store.setItem(8, new ItemStack(Items.IRON_INGOT, 64));
						store.setItem(26, new ItemStack(Items.IRON_SWORD));
					} else store.setItem(0, new ItemStack(Items.DIAMOND, 7));
					terminal.refreshClients();
				}
			});
			context.waitFor(client -> CargoExchangeRenderChecks.ready(client, first, second));
			CargoExchangeRenderChecks.independentSnapshotsAndDispatcher(context, first, second, power);
			CargoExchangeRenderChecks.stateSemantics(context);
			BlockPos port = firstCore.offset(20, 2, 0), chestPos = port.east(4);
			// Use the real placement packet against the marked outward face, bypassing the cargo menu by sneaking.
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:brass_item_pipe");
			server.runCommand("tp @p %s %s %s 90 0".formatted(firstCore.getX() + 23, firstCore.getY() + 1.2, firstCore.getZ() + .5));
			context.waitTicks(15); context.getInput().holdKey(options -> options.keyShift); context.waitTicks(2);
			context.getInput().lookAt(port); context.waitTicks(2); context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(3); context.getInput().releaseKey(options -> options.keyShift);
			assertConnectedPipe(context, server, port, JugcraftLogistics.BRASS_ITEM_PIPE);
			if (!context.computeOnClient(client -> client.gui.screen() == null))
				throw new AssertionError("Sneaking normal pipe placement opened the station menu");
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
				((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.GRAVEL, 64));
				BlockPos secondPipe = port.east(2);
				var placement = new BlockPlaceContext(level, minecraft.getPlayerList().getPlayers().getFirst(), InteractionHand.MAIN_HAND,
						new ItemStack(JugcraftLogistics.BRASS_ITEM_PIPE), new BlockHitResult(Vec3.atCenterOf(secondPipe), Direction.UP, secondPipe, false));
				level.setBlock(secondPipe, JugcraftLogistics.BRASS_ITEM_PIPE.getStateForPlacement(placement), Block.UPDATE_ALL);
				level.setBlock(port.east(3), JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
			});
			context.waitFor(client -> CargoExchangeRenderChecks.items(client, first) == 145);
			if (!server.computeOnServer(minecraft -> {
				var terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(firstCore)).terminal();
				return terminal.packagerEntity().count(stack -> true) == 145
						&& ((ChestBlockEntity) minecraft.overworld().getBlockEntity(chestPos)).isEmpty();
			})) throw new AssertionError("Cargo display did not follow the real scheduled extractor's conserved 64-item intake");
			context.runOnClient(client -> {
				client.getWindow().setWindowed(1280, 720); client.options.guiScale().set(2); client.options.fov().set(70);
				if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
			});
			BlockPos operatorPanel = firstCore.offset(20, 2, -2); // The translated panel keeps its original world position.
			photo(context, server, firstCore, 24, 1.5, -1.3, operatorPanel, "jugcraft_cargo_intake_live_operator_pod");
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			context.getInput().lookAt(operatorPanel); context.waitTicks(5); context.getInput().pressKey(options -> options.keyUse);
			context.waitFor(client -> client.gui.screen() instanceof ContainerScreen);
			boolean menuStock = context.computeOnClient(client -> {
				if (!(client.player.containerMenu instanceof ChestMenu menu) || menu.getContainer().getContainerSize() != 27) return false;
				int items = 0; for (int i = 0; i < 27; i++) items += menu.getSlot(i).getItem().getCount();
				return items == 145;
			});
			if (!menuStock) throw new AssertionError("Normal intake right-click did not open the original real 27-slot stock inventory");
			context.waitTicks(5); context.takeScreenshot("jugcraft_cargo_intake_original_inventory");
			context.runOnClient(client -> client.player.closeContainer()); context.waitTicks(5);
			photo(context, server, firstCore, 27, 3, 5, firstCore.offset(18, 2, 0), "jugcraft_cargo_intake_enclosed_housing");
			photo(context, server, firstCore, 24, 3, .5, port, "jugcraft_cargo_intake_connected_socket_front");
			photo(context, server, firstCore, 26.5, 3.5, 5.5, port, "jugcraft_cargo_intake_connected_socket_oblique");
			photo(context, server, firstCore, 12.5, 4, -9, firstCore.offset(17, 3, -1), "jugcraft_cargo_intake_rear_service_side");
			Block wallPipe = WallRuns.WALL_OF.get(JugcraftLogistics.BRASS_ITEM_PIPE);
			server.runOnServer(minecraft -> WallRuns.put(minecraft.overworld(), port.east(), wallPipe, Blocks.POLISHED_DEEPSLATE.defaultBlockState()));
			assertConnectedPipe(context, server, port, wallPipe);
			photo(context, server, firstCore, 23.5, 2.2, 2.5, port, "jugcraft_cargo_intake_wall_pipe_socket_close");
			CargoExchangeRenderChecks.unloadStoreThenTerminal(context, first);
		}
	}
	private static void assertConnectedPipe(ClientGameTestContext context, TestServerContext server, BlockPos port, Block pipe) {
		BlockPos tube = port.east();
		context.waitFor(client -> client.level.getBlockState(tube).is(pipe)
				&& client.level.getBlockState(tube).getValue(PipeBlock.WEST));
		if (!server.computeOnServer(minecraft -> {
			var level = minecraft.overworld(); var state = level.getBlockState(tube);
			return state.is(pipe) && state.getValue(PipeBlock.WEST) && Math.abs(state.getShape(level, tube).bounds().minX) < 1E-6;
		}) || !context.computeOnClient(client -> Math.abs(client.level.getBlockState(tube).getShape(client.level, tube).bounds().minX) < 1E-6))
			throw new AssertionError("Normally placed carrier has no real west arm reaching the cargo socket face");
	}
	private static void photo(ClientGameTestContext context, TestServerContext server, BlockPos core,
			double x, double y, double z, BlockPos lookAt, String name) {
		server.runCommand("tp @p %s %s %s 90 0".formatted(core.getX() + x, core.getY() + y, core.getZ() + z));
		context.waitTicks(15); context.getInput().lookAt(lookAt); context.waitTicks(10); context.takeScreenshot(name);
	}
}
