package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GiantFakeSpiderBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LurkingEyesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MusicBoxBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RockingChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilhouetteWindowBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the haunted house and yard: a house front with three Silhouette Windows lit from inside by jack
 * o'lanterns, two Rocking Chairs on its porch, a Spooky Music Box playing on a block of redstone, a Giant Fake Spider
 * hanging from the porch roof, and Lurking Eyes in a hedge; photographed by day and at night (CI job {@code client}).
 */
public class Decor6ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 24, y - 1, z + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 24, y + 12, z + 14));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			// What the client makes of the first window's light (written to the log, to check against the screenshots).
			BlockPos window = new BlockPos(x + 4, y + 2, z - 6);
			String light = context.computeOnClient(client -> "far %d, near %d, glows %s".formatted(
					SilhouetteWindowBlock.light(client.level, window.north()), SilhouetteWindowBlock.light(client.level, window.south()),
					SilhouetteWindowBlock.glows(client.level, window, Direction.SOUTH)));
			System.out.println("Silhouette window light on the client: " + light);
			shoot(context, singleplayer, x + 7, y + 4, z + 6, 180, 20, "jugcraft_haunted_house");
			shoot(context, singleplayer, x + 4, y + 2, z - 1, 180, 20, "jugcraft_rocking_chairs");
			shoot(context, singleplayer, x + 9, y + 2, z - 1, 180, 25, "jugcraft_music_box_and_spider");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 7, y + 4, z + 6, 180, 20, "jugcraft_haunted_house_night");
			shoot(context, singleplayer, x + 7, y + 2, z + 1, 180, 5, "jugcraft_silhouette_windows_night");
			shoot(context, singleplayer, x + 16, y + 2, z + 4, 180, 5, "jugcraft_lurking_eyes_night");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The house front: a dark wall along z - 6 with three windows, a room behind it lit by jack o'lanterns.
		for (int dx = 0; dx <= 14; dx++) {
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 6), Blocks.DARK_OAK_PLANKS.defaultBlockState());
				set(level, new BlockPos(x + dx, y + dy, z - 10), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
			for (int dz = -9; dz <= -7; dz++) {
				set(level, new BlockPos(x + dx, y + 6, z + dz), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		SilhouetteWindowBlock.Design[] designs = SilhouetteWindowBlock.Design.values();
		for (int i = 0; i < designs.length; i++) {
			BlockPos window = new BlockPos(x + 4 + i * 3, y + 2, z - 6);
			set(level, window, state("silhouette_window").setValue(SilhouetteWindowBlock.FACING, Direction.SOUTH)
					.setValue(SilhouetteWindowBlock.DESIGN, designs[i]));
			set(level, window.north(), Blocks.JACK_O_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING,
					Direction.SOUTH));
		}
		// The porch: a plank floor before the wall, its roof on two posts, two rocking chairs, the music box and the spider.
		for (int dx = 0; dx <= 14; dx++) {
			for (int dz = -5; dz <= -2; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
				set(level, new BlockPos(x + dx, y + 4, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		for (int dy = 0; dy <= 3; dy++) {
			set(level, new BlockPos(x, y + dy, z - 2), Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, new BlockPos(x + 14, y + dy, z - 2), Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		set(level, new BlockPos(x + 2, y, z - 4), state("rocking_chair").setValue(RockingChairBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 5, y, z - 4), state("rocking_chair").setValue(RockingChairBlock.FACING, Direction.EAST));
		// The box first, then the redstone under it: the new signal opens it.
		set(level, new BlockPos(x + 9, y + 1, z - 4), state("music_box").setValue(MusicBoxBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 9, y, z - 4), Blocks.REDSTONE_BLOCK.defaultBlockState());
		set(level, new BlockPos(x + 11, y + 3, z - 4), state("giant_fake_spider").setValue(GiantFakeSpiderBlock.DROP, 2));
		// A hedge in the yard with eyes in it.
		for (int dx = 13; dx <= 20; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 3), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
			}
		}
		for (int dx : new int[] {15, 18}) {
			set(level, new BlockPos(x + dx, y + 1, z - 2), state("lurking_eyes").setValue(LurkingEyesBlock.FACING, Direction.SOUTH));
		}
	}
}
