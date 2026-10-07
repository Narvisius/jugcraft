package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings;
import io.github.jimbozoomer.jugcraft.client.CarvingScreen;
import java.util.Arrays;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for pumpkin carving: a row of hand-carved pumpkins on hay bales (the starter faces and two
 * free designs, one pumpkin carved on two sides), photographed by day and lit at midnight; and the carving
 * screen, opened by the server as the knife does, used (a starter face pressed in, the candle preview on),
 * photographed and finished, after which the server must hold the carved face; and a pumpkin carved by real
 * mouse and keyboard input, as a player does (CI job {@code client}).
 */
public class CarvingClientGameTests implements FabricClientGameTest {
	private static final String[] BAT = {
			"................", "................", "................", "................", "................",
			"##....#..#....##", "###...####...###", "#####.####.#####", "################", ".######..######.",
			"..####....####..", "...##......##...", "................", "................", "................", "................"};
	private static final String[] STAR = {
			"s..............s", "................", ".......##.......", ".......##.......", "......####......",
			".##############.", "..############..", "...##########...", "....########....", "...####..####...",
			"..###......###..", ".##..........##.", "................", "................", "s..............s", "................"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 1, z - 10, x + 20, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 10, x + 20, y + 8, z + 6));
			server.runOnServer(minecraft -> buildRow(minecraft.overworld(), origin));
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x + 6, y + 2, z + 4, 180, 12, "jugcraft_carved_pumpkins");

			// The carving screen, end to end: the server opens it as the knife does; Done sends the face back.
			BlockPos target = origin.offset(17, 0, -3);
			server.runOnServer(minecraft -> minecraft.overworld().setBlock(target, Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 20", target.getX() + 0.5, y, target.getZ() + 2.5));
			context.waitTicks(20);
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftAgriculture.item("carving_knife")));
				PumpkinCarvings.open(player, target, Direction.SOUTH);
			});
			context.waitForScreen(CarvingScreen.class);
			context.clickScreenButton("Apply");
			context.clickScreenButton("Mirror: Off");
			context.clickScreenButton("Candle: Off");
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_carving_screen");
			context.clickScreenButton("Done");
			context.waitForScreen(null);
			context.waitTicks(20);
			boolean carved = server.computeOnServer(minecraft -> {
				BlockState state = minecraft.overworld().getBlockState(target);
				return state.getBlock() instanceof CarvedPumpkinBlock
						&& minecraft.overworld().getBlockEntity(target) instanceof CarvedPumpkinBlockEntity pumpkin
						&& Arrays.equals(pumpkin.carving().face(PumpkinCarving.faceIndex(state.getValue(CarvedPumpkinBlock.FACING), Direction.SOUTH)),
								CarvingTemplates.ALL.get(0).face());
			});
			System.out.println("[carving test] face carved over the network: " + carved);
			if (!carved) {
				throw new AssertionError("The carving screen's face did not reach the pumpkin");
			}

			carveByHand(context, server, origin.offset(15, 0, -3), y);

			server.runOnServer(minecraft -> lightAll(minecraft.overworld(), origin));
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 6, y + 2, z + 4, 180, 12, "jugcraft_carved_pumpkins_night");
			shoot(context, singleplayer, x + 17, y, z - 1, 180, 28, "jugcraft_carved_pumpkin_close");
		}
	}

	/**
	 * Carves a pumpkin the way a player does, through real input: the use key with the knife opens the screen,
	 * then mouse clicks and a drag on the grid, a right-click erase, Ctrl+Z, the Shave tool and Done. The server
	 * must then hold exactly that face.
	 */
	private static void carveByHand(ClientGameTestContext context, TestServerContext server, BlockPos target, int y) {
		server.runOnServer(minecraft -> {
			minecraft.overworld().setBlock(target, Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
			minecraft.getPlayerList().getPlayers().get(0).setItemInHand(InteractionHand.MAIN_HAND,
					new ItemStack(JugcraftAgriculture.item("carving_knife")));
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 20", target.getX() + 0.5, y, target.getZ() + 2.5));
		context.waitTicks(20);
		context.getInput().lookAt(target);
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(CarvingScreen.class);
		context.waitTicks(2);

		TestInput input = context.getInput();
		clickCell(context, 3, 3, InputConstants.MOUSE_BUTTON_LEFT); // Cut is the starting tool
		moveToCell(context, 4, 10); // drag a line along row 10
		input.holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(1);
		for (int x = 5; x <= 9; x++) {
			moveToCell(context, x, 10);
		}
		input.releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(1);
		clickCell(context, 6, 10, InputConstants.MOUSE_BUTTON_RIGHT); // right-click erases this session's cut...
		pressUndo(context); // ...and Ctrl+Z brings it back
		context.waitTicks(1);
		clickCell(context, 9, 10, InputConstants.MOUSE_BUTTON_RIGHT); // this erase stays
		context.clickScreenButton("Shave");
		clickCell(context, 12, 3, InputConstants.MOUSE_BUTTON_LEFT);
		context.takeScreenshot("jugcraft_carving_by_hand");
		context.clickScreenButton("Done");
		context.waitForScreen(null);
		context.waitTicks(20);

		int[] expected = face(
				"................", "................", "................", "...#........s...",
				"................", "................", "................", "................",
				"................", "................", "....#####.......", "................",
				"................", "................", "................", "................");
		int[] actual = server.computeOnServer(minecraft -> {
			BlockState state = minecraft.overworld().getBlockState(target);
			if (state.getBlock() instanceof CarvedPumpkinBlock
					&& minecraft.overworld().getBlockEntity(target) instanceof CarvedPumpkinBlockEntity pumpkin) {
				return pumpkin.carving().face(PumpkinCarving.faceIndex(state.getValue(CarvedPumpkinBlock.FACING), Direction.SOUTH));
			}
			return new int[PumpkinCarving.SIZE];
		});
		boolean match = Arrays.equals(expected, actual);
		System.out.println("[carving test] face carved by mouse and keyboard: " + match);
		if (!match) {
			throw new AssertionError("The face carved with the mouse is wrong. Expected:\n" + rows(expected) + "Got:\n" + rows(actual));
		}
	}

	/** Moves the real cursor to the middle of a cell of the open carving screen's grid. */
	private static void moveToCell(ClientGameTestContext context, int x, int y) {
		double[] window = context.computeOnClient(client -> {
			CarvingScreen screen = (CarvingScreen) client.gui.screen();
			// The cursor is set in window pixels; the screen works in GUI units.
			double guiPerPixelX = MouseHandler.getScaledXPos(client.getWindow(), 1.0);
			double guiPerPixelY = MouseHandler.getScaledYPos(client.getWindow(), 1.0);
			return new double[] {screen.cellCentreX(x) / guiPerPixelX, screen.cellCentreY(y) / guiPerPixelY};
		});
		context.getInput().setCursorPos(window[0], window[1]);
		context.waitTicks(1);
	}

	/**
	 * Presses Ctrl+Z (Cmd+Z on macOS) through the game's keyboard handler, as the event a real keyboard sends: the Z
	 * key, its key code and the platform's shortcut modifier. Fabric's {@code TestInput.pressKey} can't express it,
	 * because it sends every key with no modifiers, even while {@code holdControl} holds Ctrl.
	 */
	private static void pressUndo(ClientGameTestContext context) {
		context.runOnClient(client -> {
			KeyEvent undo = new KeyEvent(InputConstants.KEY_Z, InputConstants.KEYCODE_Z, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
			client.keyboardHandler.keyPress(client.getWindow().handle(), InputConstants.PRESS, undo);
			client.keyboardHandler.keyPress(client.getWindow().handle(), InputConstants.RELEASE, undo);
		});
	}

	private static void clickCell(ClientGameTestContext context, int x, int y, int button) {
		moveToCell(context, x, y);
		context.getInput().pressMouse(button);
		context.waitTicks(1);
	}

	private static String rows(int[] face) {
		StringBuilder text = new StringBuilder();
		for (int y = 0; y < PumpkinCarving.SIZE; y++) {
			for (int x = 0; x < PumpkinCarving.SIZE; x++) {
				text.append(".s#?".charAt(PumpkinCarving.pixel(face[y], x)));
			}
			text.append('\n');
		}
		return text.toString();
	}

	/** Stands the player at a spot (on an invisible barrier) and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static int[] face(String... rows) {
		int[] face = new int[PumpkinCarving.SIZE];
		for (int y = 0; y < PumpkinCarving.SIZE; y++) {
			for (int x = 0; x < PumpkinCarving.SIZE; x++) {
				char pixel = rows[y].charAt(x);
				face[y] = PumpkinCarving.withPixel(face[y], x, pixel == '#' ? PumpkinCarving.CUT : pixel == 's' ? PumpkinCarving.SHAVED : PumpkinCarving.SKIN);
			}
		}
		return face;
	}

	/** A carved pumpkin on a hay bale, its front facing south (towards the camera). */
	private static void pumpkin(ServerLevel level, BlockPos pos, PumpkinCarving carving) {
		level.setBlock(pos.below(), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		BlockState state = JugcraftAgriculture.block("hand_carved_pumpkin").defaultBlockState()
				.setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH).setValue(CarvedPumpkinBlock.GLOW, carving.glow());
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity entity) {
			entity.setCarving(carving, null);
		}
	}

	private static void buildRow(ServerLevel level, BlockPos origin) {
		int column = 0;
		for (CarvingTemplates.Template template : CarvingTemplates.ALL) {
			pumpkin(level, origin.offset(column * 2, 1, -5), PumpkinCarving.BLANK.withFace(0, template.face()));
			column++;
		}
		pumpkin(level, origin.offset(column * 2, 1, -5), PumpkinCarving.BLANK.withFace(0, face(BAT)));
		column++;
		pumpkin(level, origin.offset(column * 2, 1, -5), PumpkinCarving.BLANK.withFace(0, face(STAR)));
		// A pumpkin carved on two sides: the classic face in front and the cat on its west side (face 1 when facing south).
		pumpkin(level, origin.offset(-2, 1, -3), PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face())
				.withFace(PumpkinCarving.faceIndex(Direction.SOUTH, Direction.WEST), CarvingTemplates.ALL.get(1).face()));
		// Plain pumpkins between them, for comparison.
		for (int dx = 1; dx < column * 2; dx += 2) {
			level.setBlock(origin.offset(dx, 0, -5), Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Puts a torch in every carved pumpkin around. */
	private static void lightAll(ServerLevel level, BlockPos origin) {
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, -10), origin.offset(20, 3, 6))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof CarvedPumpkinBlock && state.getValue(CarvedPumpkinBlock.GLOW) > 0) {
				level.setBlock(pos, state.setValue(CarvedPumpkinBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
	}
}
