package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.GraveyardBuildingBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the graveyard pack's buildings: the family mausoleum with its Bronze Mausoleum Door hung, the
 * lych gate with a pair of gates, the cemetery gateway with cemetery gates in its opening, and the columbarium, each
 * inscribed, along a gravel path; the row by day, each up close, the mausoleum's room from inside (its crypts, its altar
 * under the stained glass, its lamp), the mausoleum overgrown, and the row and the room at night. It prints what the
 * client received of each building's inscriptions. CI job {@code client}.
 */
public class GraveyardBuildingClientGameTests implements FabricClientGameTest {
	/** Each building: id, where its part 0 stands (right, back from the origin), its inscriptions by slot. */
	private record Building(String id, int dx, int dz, Map<Integer, List<String>> inscriptions) {
	}

	private static final List<Building> BUILDINGS = List.of(
			new Building("family_mausoleum", 0, 2, Map.of(0, List.of("HALLOWAY"),
					1, List.of("SIR EDMUND", "HALLOWAY", "1690 - 1761"), 2, List.of("DAME AGNES", "HALLOWAY", "1702 - 1770"),
					4, List.of("ELIZA HALLOWAY", "1801 - 1866"), 7, List.of("JOHN HALLOWAY", "1799 - 1870"),
					8, List.of("MARY HALLOWAY", "1830 - 1835", "ASLEEP IN JESUS"), 10, List.of("THOMAS HALLOWAY", "1832 - 1899"))),
			new Building("lych_gate", 8, 2, Map.of(0, List.of("I AM THE RESURRECTION", "AND THE LIFE"))),
			new Building("cemetery_gateway", 15, 2, Map.of(0, List.of("HOLLOWMERE"))),
			new Building("columbarium", 20, 2, Map.of(0, List.of("IN PACE"), 1, List.of("ADA GREY", "1881 - 1950"),
					2, List.of("W. GREY", "1879 - 1944"), 4, List.of("ROSE LEE", "1902 - 1961"), 6, List.of("F. MARSH", "1890 - 1958"))));

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 12, x + 26, y - 1, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 12, x + 26, y + 12, z + 12));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				for (Building building : BUILDINGS) {
					BlockPos pos = new BlockPos(x + building.dx(), y, z - building.dz());
					if (client.level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
						System.out.println("[graveyard test] " + building.id() + " on the client: " + stone.epitaph().lines() + " " + stone.more());
					}
				}
			});

			shoot(context, singleplayer, x + 9, y + 4, z + 9, 180, 16, "jugcraft_graveyard_buildings");
			shoot(context, singleplayer, x, y + 1, z + 3, 180, 8, "jugcraft_graveyard_mausoleum");
			shoot(context, singleplayer, x, y, z - 4, 180, 4, "jugcraft_graveyard_mausoleum_altar");
			shoot(context, singleplayer, x - 1, y, z - 4, 270, 6, "jugcraft_graveyard_mausoleum_crypts");
			shoot(context, singleplayer, x + 6, y + 1, z + 4, 180, 12, "jugcraft_graveyard_lych_gate");
			shoot(context, singleplayer, x + 13, y + 1, z + 4, 180, 8, "jugcraft_graveyard_gateway");
			shoot(context, singleplayer, x + 20, y, z + 1, 180, 4, "jugcraft_graveyard_columbarium");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				BlockPos master = new BlockPos(x, y, z - 2);
				((HeadstoneBlock) JugcraftAgriculture.block("family_mausoleum")).setWeather(level, master, HeadstoneBlock.OVERGROWN, false);
			});
			context.waitTicks(20);
			shoot(context, singleplayer, x + 3, y + 1, z + 3, 165, 10, "jugcraft_graveyard_mausoleum_overgrown");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 9, y + 4, z + 9, 180, 16, "jugcraft_graveyard_buildings_night");
			shoot(context, singleplayer, x, y, z - 4, 180, 4, "jugcraft_graveyard_mausoleum_night");
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
		// A gravel path along the fronts.
		for (int dx = -4; dx <= 23; dx++) {
			for (int dz = -1; dz <= 0; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		for (Building building : BUILDINGS) {
			BlockPos pos = new BlockPos(x + building.dx(), y, z - building.dz());
			GraveyardBuildingBlock block = (GraveyardBuildingBlock) JugcraftAgriculture.block(building.id());
			BlockState state = block.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			if (level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
				building.inscriptions().forEach((slot, lines) -> stone.engrave(slot, Epitaph.of(lines)));
			}
		}
		// Facing south, each building runs north from its part 0 and its right-hand side is to the east.
		weather(level, "lych_gate", x + 8, y, z - 2, 2);
		weather(level, "cemetery_gateway", x + 15, y, z - 2, 1);
		// The bronze door in the mausoleum's doorway, hung by someone walking in (facing north).
		BlockState door = JugcraftAgriculture.block(JugcraftAgriculture.MAUSOLEUM_DOOR).defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
		level.setBlock(new BlockPos(x, y, z - 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
		level.setBlock(new BlockPos(x, y + 1, z - 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
		// A pair of oak gates in the lych gate's passage, and the cemetery gates in the gateway's opening.
		BlockState oak = Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH);
		for (int dx : new int[] {6, 7}) {
			level.setBlock(new BlockPos(x + dx, y, z - 2), oak, Block.UPDATE_CLIENTS);
		}
		BlockState iron = JugcraftAgriculture.block("cemetery_gate").defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH);
		for (int dx = 12; dx <= 14; dx++) {
			level.setBlock(new BlockPos(x + dx, y, z - 2), iron, Block.UPDATE_CLIENTS);
		}
	}

	private static void weather(ServerLevel level, String id, int x, int y, int z, int stage) {
		((HeadstoneBlock) JugcraftAgriculture.block(id)).setWeather(level, new BlockPos(x, y, z), stage, false);
	}
}
