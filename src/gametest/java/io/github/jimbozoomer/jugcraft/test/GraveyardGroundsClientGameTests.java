package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.GraveVaseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LampPostBlock;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the graveyard pack's grounds: a kerbed grave before a gothic headstone, two planted graves (one
 * kept, one let go to weeds), an open grave, grave vases with fresh and wilted flowers, a memorial bench and two lamp
 * posts along a gravel path; the whole by day, each up close, and at night by the lamps. CI job {@code client}.
 */
public class GraveyardGroundsClientGameTests implements FabricClientGameTest {
	/** Each memorial: id, where its part 0 stands (right, back from the origin), stage, epitaph. */
	private record Grave(String id, int dx, int dz, int stage, List<String> lines) {
	}

	private static final List<Grave> GRAVES = List.of(
			new Grave("kerbed_grave", 0, 3, 0, List.of("ALICE", "WREN", "1850 - 1921")),
			new Grave("gothic_headstone", 0, 5, 0, List.of("ALICE WREN", "BELOVED WIFE")),
			new Grave("planted_grave", 2, 3, 0, List.of("TOM BELL", "1888")),
			new Grave("planted_grave", 4, 3, 3, List.of("FORGOTTEN")),
			new Grave("open_grave", 7, 3, 0, List.of("R.I.P.")),
			new Grave("memorial_bench", 11, 4, 1, List.of("IN MEMORY OF", "ADA GREY")));

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left: CI shares the client tests out
			// between parallel jobs, and only the first job's opening test hides them.
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 12, x + 18, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 12, x + 18, y + 10, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 6, y + 3, z + 5, 180, 22, "jugcraft_graveyard_grounds");
			shoot(context, singleplayer, x + 1, y + 1, z, 180, 30, "jugcraft_graveyard_kerbed_and_planted");
			shoot(context, singleplayer, x + 3, y + 1, z, 180, 34, "jugcraft_graveyard_kept_and_neglected");
			shoot(context, singleplayer, x + 7, y + 1, z, 180, 36, "jugcraft_graveyard_open_grave");
			shoot(context, singleplayer, x + 11, y, z - 1, 180, 10, "jugcraft_graveyard_memorial_bench");
			shoot(context, singleplayer, x + 1, y + 1, z - 2, 180, 40, "jugcraft_graveyard_grave_vases");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			// Let each lamp post's own check see the dark.
			context.waitTicks(LampPostBlock.CHECK_TICKS + 20);
			shoot(context, singleplayer, x + 6, y + 3, z + 5, 180, 22, "jugcraft_graveyard_grounds_night");
			server.runCommand("time set noon");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		// Only into air, so standing on the ground leaves no hole in it for the later pictures.
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(20);
		context.takeScreenshot(name);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A gravel path in front.
		for (int dx = -3; dx <= 15; dx++) {
			level.setBlock(new BlockPos(x + dx, y - 1, z - 1), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (Grave grave : GRAVES) {
			BlockPos pos = new BlockPos(x + grave.dx(), y, z - grave.dz());
			HeadstoneBlock block = (HeadstoneBlock) JugcraftAgriculture.block(grave.id());
			BlockState state = block.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			block.setWeather(level, pos, grave.stage(), false);
			if (level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
				stone.engrave(Epitaph.of(grave.lines()));
			}
		}
		// Vases: fresh red flowers by the kerbed grave, white ones by the kept grave, wilted ones by the neglected.
		BlockState vase = JugcraftAgriculture.block(JugcraftAgriculture.GRAVE_VASE).defaultBlockState();
		level.setBlock(new BlockPos(x + 1, y, z - 5), vase.setValue(GraveVaseBlock.FLOWERS, GraveVaseBlock.Bouquet.RED), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 3, y, z - 5), vase.setValue(GraveVaseBlock.FLOWERS, GraveVaseBlock.Bouquet.WHITE), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 5, y, z - 5), vase.setValue(GraveVaseBlock.FLOWERS, GraveVaseBlock.Bouquet.PURPLE)
				.setValue(GraveVaseBlock.WILTED, true), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x - 1, y, z - 5), vase.setValue(GraveVaseBlock.FLOWERS, GraveVaseBlock.Bouquet.MIXED), Block.UPDATE_ALL);
		// Lamp posts at either end of the path.
		for (int dx : new int[] {-2, 14}) {
			BlockPos base = new BlockPos(x + dx, y, z - 2);
			BlockState post = JugcraftAgriculture.block(JugcraftAgriculture.LAMP_POST).defaultBlockState().setValue(LampPostBlock.FACING, Direction.SOUTH);
			level.setBlock(base, post, Block.UPDATE_ALL);
			post.getBlock().setPlacedBy(level, base, post, null, ItemStack.EMPTY);
		}
	}
}
