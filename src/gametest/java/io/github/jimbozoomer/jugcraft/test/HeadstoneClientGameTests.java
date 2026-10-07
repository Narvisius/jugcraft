package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OpenEpitaphPayload;
import io.github.jimbozoomer.jugcraft.client.EpitaphScreen;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the graveyard pack's headstones: a churchyard of all nine, each engraved and at its own stage of
 * weathering, between a gravel path and a wrought-iron fence, and one headstone at each of the four stages side by side;
 * the whole churchyard by day, each group up close to read its epitaphs, the table tomb and ledger stone from their
 * feet, the four stages, the churchyard at night by lantern light, and the epitaph screen as the Stonemason's Chisel
 * opens it. It also prints what the client received of each epitaph. CI job {@code client}.
 */
public class HeadstoneClientGameTests implements FabricClientGameTest {
	/** Each headstone: id, where (right, back from the origin), stage, epitaph. */
	private record Grave(String id, int dx, int dz, int stage, List<String> lines) {
	}

	private static final List<Grave> GRAVES = List.of(
			new Grave("gothic_headstone", 0, 5, 0, List.of("IN LOVING MEMORY", "ELIZA THORNE", "1841 - 1899", "AT REST")),
			new Grave("willow_urn_headstone", 2, 5, 1, List.of("HERE LYES Ye BODY", "OF CAPt JOHN AMES", "WHO DIED OCT 31", "1786")),
			new Grave("winged_skull_headstone", 4, 5, 2, List.of("MEMENTO MORI", "SILAS WICKHAM", "1702", "AS I AM SO SHALL YE BE")),
			new Grave("lamb_headstone", 6, 5, 0, List.of("LITTLE ROSE", "BUDDED ON EARTH", "TO BLOOM IN HEAVEN")),
			new Grave("rustic_scroll_headstone", 8, 5, 3, List.of("THOMAS BRIGGS", "STONEMASON", "1832 - 1904")),
			new Grave("broken_column", 0, 9, 1, List.of("HENRY CALDER", "1820 - 1849", "CUT DOWN", "IN HIS PRIME")),
			new Grave("celtic_cross", 3, 9, 2, List.of("MARY O'NEILL", "1856 - 1931", "REST IN PEACE")),
			new Grave("table_tomb", 6, 9, 1, List.of("SIR EDMUND HALLOWAY", "1690 - 1761", "REQUIESCAT IN PACE")),
			new Grave("ledger_stone", 9, 9, 2, List.of("HERE LIETH", "ANNE PRYOR", "1744")),
			// One headstone at each stage of weathering, side by side.
			new Grave("gothic_headstone", 0, 13, 0, List.of("JOHN HARROW", "1801 - 1868")),
			new Grave("gothic_headstone", 2, 13, 1, List.of("JOHN HARROW", "1801 - 1868")),
			new Grave("gothic_headstone", 4, 13, 2, List.of("JOHN HARROW", "1801 - 1868")),
			new Grave("gothic_headstone", 6, 13, 3, List.of("JOHN HARROW", "1801 - 1868")));

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 18, x + 16, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 18, x + 16, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				for (Grave grave : GRAVES.subList(0, 9)) {
					BlockPos pos = new BlockPos(x + grave.dx(), y, z - grave.dz());
					if (client.level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
						System.out.println("[graveyard test] epitaph on the client, " + grave.id() + ": " + stone.epitaph().lines());
					}
				}
			});

			shoot(context, singleplayer, x + 5, y + 3, z, 180, 24, "jugcraft_graveyard_headstones");
			shoot(context, singleplayer, x, y, z - 3, 180, 18, "jugcraft_graveyard_gothic");
			shoot(context, singleplayer, x + 3, y, z - 2, 180, 14, "jugcraft_graveyard_slates");
			shoot(context, singleplayer, x + 7, y, z - 3, 180, 20, "jugcraft_graveyard_lamb_and_scroll");
			shoot(context, singleplayer, x + 1, y + 1, z - 5, 180, 2, "jugcraft_graveyard_column_and_cross");
			shoot(context, singleplayer, x + 7, y + 1, z - 7, 180, 42, "jugcraft_graveyard_tomb_and_ledger");
			shoot(context, singleplayer, x + 3, y, z - 10, 180, 14, "jugcraft_graveyard_weathering");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 5, y + 3, z, 180, 24, "jugcraft_graveyard_headstones_night");
			server.runCommand("time set noon");
			context.setScreen(() -> new EpitaphScreen(new OpenEpitaphPayload(new BlockPos(x, y, z - 5), Epitaph.of(GRAVES.get(0).lines()).padded())));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_graveyard_epitaph_screen");
			context.setScreen(() -> null);
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
		// A gravel path in front, and a wrought-iron fence behind with lanterns on stone posts.
		for (int dx = -2; dx <= 12; dx++) {
			level.setBlock(new BlockPos(x + dx, y - 1, z - 1), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(new BlockPos(x + dx, y, z - 15), JugcraftAgriculture.block("cemetery_fence").defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int dx : new int[] {-2, 4, 12}) {
			BlockPos post = new BlockPos(x + dx, y, z - 15);
			level.setBlock(post, Blocks.STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(post.above(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_ALL);
		}
		for (Grave grave : GRAVES) {
			BlockPos pos = new BlockPos(x + grave.dx(), y, z - grave.dz());
			HeadstoneBlock headstone = (HeadstoneBlock) JugcraftAgriculture.block(grave.id());
			BlockState state = headstone.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			headstone.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			headstone.setWeather(level, pos, grave.stage(), false);
			if (level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
				stone.engrave(Epitaph.of(grave.lines()));
			}
		}
	}
}
