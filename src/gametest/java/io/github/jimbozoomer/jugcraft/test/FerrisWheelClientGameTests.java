package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheel;
import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HighStrikerBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Midway;
import io.github.jimbozoomer.jugcraft.agriculture.PlushBlock;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the Ferris wheel (fall addition 27): the wheel standing over its booth at a fall fair, a hand
 * crank turning it, a High Striker and plushes beside it; close up at its foot; the view from a car at the top; at
 * night, its lights on; and stopped, close by at an angle, twice from 0.05 block apart (a pair to compare for shimmer).
 * CI job {@code client}.
 */
public class FerrisWheelClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 14, y - 3, z - 16, x + 14, y - 1, z + 22));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 14, y, z - 16, x + 14, y + 20, z + 22));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 2, z + 14, 180, -14, "jugcraft_ferris_wheel");
			shoot(context, singleplayer, x + 4, y + 1, z - 1, 145, 8, "jugcraft_ferris_wheel_booth");

			// A ride: aboard the car at the bottom, turned to the top, looking out over the fair.
			place(context, singleplayer, x, y + 1, z - 2, 180, 0);
			server.runCommand("ride @p mount @e[type=jugcraft:ferris_wheel,limit=1,sort=nearest]");
			server.runOnServer(minecraft -> wheel(minecraft.overworld(), origin).setAngle((float) Math.PI * 0.95F));
			context.waitTicks(10);
			server.runCommand("rotate @p 0 25");
			context.waitTicks(30);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_ferris_wheel_ride");
			server.runCommand("ride @p dismount");
			context.waitTicks(10);

			server.runCommand("time set 18000");
			server.runOnServer(minecraft -> crank(minecraft.overworld(), origin));
			context.waitTicks(10);
			shoot(context, singleplayer, x, y + 2, z + 14, 180, -14, "jugcraft_ferris_wheel_night");
			server.runCommand("time set noon");

			// The wheel stopped (its crank taken away), from close by at an angle, then again from 0.05 block to the side:
			// compared, the two show any texture that shimmers as the camera moves (only the outlines should shift).
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				level.setBlock(booth(origin).east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				FerrisWheel wheel = wheel(level, origin);
				wheel.setSpeed(0.0F);
				wheel.setAngle(0.2F);
			});
			context.waitTicks(30);
			shoot(context, singleplayer, x + 9, y + 4, z + 4, 138, -16, "jugcraft_ferris_wheel_still");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 138 -16", x + 9.55, y + 4, z + 4.5));
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_ferris_wheel_still_moved");
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

	private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
	}

	/** The booth's place: six blocks north of the start, facing south (towards the camera). */
	private static BlockPos booth(BlockPos origin) {
		return origin.north(6);
	}

	private static FerrisWheel wheel(ServerLevel level, BlockPos origin) {
		List<FerrisWheel> wheels = FerrisWheelBlock.wheels(level, booth(origin));
		return wheels.get(0);
	}

	/** Cranks the hand crank beside the booth as far as it goes (20 seconds of turning). */
	private static void crank(ServerLevel level, BlockPos origin) {
		if (level.getBlockEntity(booth(origin).east()) instanceof HandCrankBlockEntity crank) {
			crank.addTurns(HandCrankBlockEntity.MAX_TICKS);
		}
	}

	/**
	 * The fair: a plank floor round the booth; the booth facing south with its wheel, a hand crank on its east side
	 * cranked; a High Striker to the west with its puck half way up; plushes on hay bales to the east.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		BlockPos booth = booth(origin);
		int bx = booth.getX();
		int by = booth.getY();
		int bz = booth.getZ();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -2; dz <= 4; dz++) {
				set(level, bx + dx, by - 1, bz + dz, ((dx + dz) & 1) == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
			}
		}
		level.setBlock(booth, JugcraftAgriculture.block(FerrisWheelBlock.ID).defaultBlockState().setValue(FerrisWheelBlock.FACING, Direction.SOUTH),
				Block.UPDATE_ALL);
		FerrisWheel wheel = FerrisWheel.raise(level, booth, Direction.SOUTH);
		if (wheel != null) {
			wheel.setAngle(0.2F);
		}
		level.setBlock(booth.east(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
		crank(level, origin);
		// A High Striker to the west, its puck half way up.
		BlockState striker = JugcraftAgriculture.block("high_striker").defaultBlockState().setValue(HighStrikerBlock.FACING, Direction.SOUTH)
				.setValue(HighStrikerBlock.LEVEL, 4);
		for (int part = 0; part < HighStrikerBlock.PARTS; part++) {
			set(level, bx - 10, by + part, bz + 3, striker.setValue(HighStrikerBlock.PART, part));
		}
		// Plushes on hay bales to the east.
		String[] plushes = {"pumpkin_plush", "ghost_plush", "black_cat_plush", Midway.JACKPOT};
		for (int i = 0; i < plushes.length; i++) {
			set(level, bx + 9 + i, by, bz + 3, Blocks.HAY_BLOCK.defaultBlockState());
			set(level, bx + 9 + i, by + 1, bz + 3, JugcraftAgriculture.block(plushes[i]).defaultBlockState().setValue(PlushBlock.FACING, Direction.SOUTH));
		}
	}
}
