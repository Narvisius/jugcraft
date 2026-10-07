package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
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

/**
 * Client game test for the graveyard pack's monuments: the grand obelisk, the draped urn, the Angel at the Tomb, the
 * trumpeting angel on its column, the iron mortsafe and the faithful hound, each engraved, set out on the grass; the
 * group by day, each up close, and the group at night. CI job {@code client}.
 */
public class MonumentClientGameTests implements FabricClientGameTest {
	private record Monument(String id, int dx, int dz, int stage, List<String> lines) {
	}

	private static final List<Monument> MONUMENTS = List.of(
			new Monument("grand_obelisk", 0, 6, 0, List.of("SACRED", "TO THE MEMORY OF", "COL. JAMES WARD", "1790 - 1861")),
			new Monument("draped_urn", 3, 6, 1, List.of("HER CHILDREN", "RISE UP AND", "CALL HER BLESSED")),
			new Monument("angel_at_the_tomb", 7, 6, 0, List.of("CATHERINE MAY ELLIS", "1862 - 1891", "NOT DEAD BUT SLEEPETH")),
			new Monument("trumpeting_angel", 11, 6, 0, List.of("IN MEMORY OF", "THE REV. ARTHUR LANE", "1801 - 1870")),
			new Monument("mortsafe", 14, 5, 2, List.of("W. KERR", "1823")),
			new Monument("faithful_hound", 17, 6, 1, List.of("FAITHFUL UNTO DEATH", "BRUNO")));

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 24, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 24, y + 12, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 8, y + 3, z + 3, 180, 15, "jugcraft_graveyard_monuments");
			shoot(context, singleplayer, x, y + 2, z - 2, 180, 15, "jugcraft_graveyard_obelisk");
			shoot(context, singleplayer, x + 3, y, z - 3, 180, 12, "jugcraft_graveyard_draped_urn");
			shoot(context, singleplayer, x + 8, y + 1, z - 3, 171, 22, "jugcraft_graveyard_angel_at_the_tomb");
			shoot(context, singleplayer, x + 11, y + 3, z - 1, 180, 20, "jugcraft_graveyard_trumpeting_angel");
			shoot(context, singleplayer, x + 15, y + 2, z - 1, 180, 28, "jugcraft_graveyard_mortsafe_and_hound");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 8, y + 3, z + 3, 180, 15, "jugcraft_graveyard_monuments_night");
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
		// A gravel path in front, and lanterns behind the row for the night, out of the way of the close pictures.
		for (int dx = -2; dx <= 20; dx++) {
			level.setBlock(new BlockPos(x + dx, y - 1, z - 3), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int dx : new int[] {-2, 5, 13, 20}) {
			level.setBlock(new BlockPos(x + dx, y, z - 9), Blocks.STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(new BlockPos(x + dx, y + 1, z - 9), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (Monument monument : MONUMENTS) {
			BlockPos pos = new BlockPos(x + monument.dx(), y, z - monument.dz());
			HeadstoneBlock block = (HeadstoneBlock) JugcraftAgriculture.block(monument.id());
			var state = block.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			block.setWeather(level, pos, monument.stage(), false);
			if (level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
				stone.engrave(Epitaph.of(monument.lines()));
			}
		}
	}
}
