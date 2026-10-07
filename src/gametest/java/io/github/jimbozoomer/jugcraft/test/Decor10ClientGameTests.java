package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BlackLightBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingWitchHatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GlowPaintBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MiniPumpkinStackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShadowPuppetLampBlock;
import io.github.jimbozoomer.jugcraft.agriculture.WitchFireBrazierBlock;
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
 * Client game test for lighting and glow: a dark stone room with two Black Lights over a back wall covered in Glow
 * Paint (and paint on the floor, the ceiling and a side wall) and a Shadow Puppet Lamp on a table in the middle,
 * throwing its shadows round the walls; outside, four Witch Fire Braziers (orange, green, purple, blue), Mini Pumpkin
 * Stacks and Floating Witch Hats. Photographed by day and at night, the paint with the black lights on and off; then the
 * lamp close up, and a second lamp at the end of a narrow corridor with a doorway, each twice a few ticks apart as its
 * shade turns (CI job {@code client}).
 */
public class Decor10ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 26, y - 1, z + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 26, y + 12, z + 14));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 15, y + 3, z + 6, 180, 15, "jugcraft_lighting");
			shoot(context, singleplayer, x + 16, y + 1, z + 1, 180, 10, "jugcraft_witch_fire_braziers");
			shoot(context, singleplayer, x + 5, y + 1, z - 3, 180, 5, "jugcraft_glow_paint");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 15, y + 3, z + 6, 180, 15, "jugcraft_lighting_night");
			shoot(context, singleplayer, x + 16, y + 1, z + 1, 180, 10, "jugcraft_witch_fire_braziers_night");
			shoot(context, singleplayer, x + 5, y + 1, z - 3, 180, 5, "jugcraft_glow_paint_night");
			shoot(context, singleplayer, x + 8, y + 1, z - 3, 135, 5, "jugcraft_shadow_puppet_lamp_night");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int dx : new int[] {2, 8}) {
					BlockPos light = new BlockPos(x + dx, y + 3, z - 11);
					level.setBlock(light, level.getBlockState(light).setValue(BlackLightBlock.LIT, false), Block.UPDATE_ALL);
				}
			});
			shoot(context, singleplayer, x + 5, y + 1, z - 3, 180, 5, "jugcraft_glow_paint_unlit_night");
			// The lamp close up, twice a few ticks apart as its shade turns: one silhouette to a panel, the corners closed.
			shoot(context, singleplayer, x + 5, y + 1, z - 4, 180, 35, "jugcraft_shadow_puppet_lamp_close_night");
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_shadow_puppet_lamp_close_night_later");
			// A lamp at the end of a narrow corridor with a doorway in its side: each shadow lies flat on the wall its ray
			// meets and is cut to the open wall, never hanging over the doorway.
			server.runOnServer(minecraft -> corridor(minecraft.overworld(), origin));
			context.waitTicks(20);
			shoot(context, singleplayer, x + 22, y, z - 5, 180, 8, "jugcraft_shadow_puppet_lamp_corridor_night");
			context.waitTicks(40);
			context.takeScreenshot("jugcraft_shadow_puppet_lamp_corridor_night_later");
		}
	}

	/** A corridor three wide from z - 16 to z - 6 at x + 21 to x + 23, roofed, a doorway in its east wall, a lit lamp at its end. */
	private static void corridor(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		BlockState stone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		for (int dz = -16; dz <= -6; dz++) {
			for (int dx = 20; dx <= 24; dx++) {
				set(level, new BlockPos(x + dx, y + 3, z + dz), stone);
				for (int dy = 0; dy <= 2; dy++) {
					boolean wall = dx == 20 || dx == 24 || dz == -16;
					set(level, new BlockPos(x + dx, y + dy, z + dz), wall ? stone : Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int dy = 0; dy <= 1; dy++) {
			set(level, new BlockPos(x + 24, y + dy, z - 11), Blocks.AIR.defaultBlockState());
		}
		set(level, new BlockPos(x + 22, y, z - 13), state("shadow_puppet_lamp").setValue(ShadowPuppetLampBlock.LIT, true));
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
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

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void paint(ServerLevel level, BlockPos pos, Direction facing, GlowPaintBlock.Design design) {
		set(level, pos, state("glow_paint").setValue(GlowPaintBlock.FACING, facing).setValue(GlowPaintBlock.DESIGN, design));
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		BlockState stone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		// A dark room from x to x + 10 and z - 12 to z - 2, a doorway at the front.
		for (int dx = 0; dx <= 10; dx++) {
			for (int dz = -12; dz <= -2; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), stone);
				set(level, new BlockPos(x + dx, y + 5, z + dz), stone);
				boolean edge = dx == 0 || dx == 10 || dz == -12 || dz == -2;
				for (int dy = 0; dy <= 4; dy++) {
					set(level, new BlockPos(x + dx, y + dy, z + dz), edge ? stone : Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int dy = 0; dy <= 1; dy++) {
			set(level, new BlockPos(x + 5, y + dy, z - 2), Blocks.AIR.defaultBlockState());
		}
		// Two black lights, on, over a back wall of glow paint, and paint on the floor, the ceiling and a side wall.
		for (int dx : new int[] {2, 8}) {
			set(level, new BlockPos(x + dx, y + 3, z - 11), state("black_light").setValue(BlackLightBlock.FACING, Direction.SOUTH)
					.setValue(BlackLightBlock.LIT, true));
		}
		GlowPaintBlock.Design[] designs = GlowPaintBlock.Design.values();
		for (int i = 0; i < designs.length; i++) {
			paint(level, new BlockPos(x + 2 + i, y + 1 + i % 2, z - 11), Direction.SOUTH, designs[i]);
		}
		paint(level, new BlockPos(x + 3, y, z - 8), Direction.UP, GlowPaintBlock.Design.HAND);
		paint(level, new BlockPos(x + 7, y, z - 9), Direction.UP, GlowPaintBlock.Design.HAND);
		paint(level, new BlockPos(x + 6, y + 4, z - 8), Direction.DOWN, GlowPaintBlock.Design.WEB);
		paint(level, new BlockPos(x + 1, y + 2, z - 9), Direction.EAST, GlowPaintBlock.Design.EYE);
		// The shadow puppet lamp, lit, on a table in the middle of the room.
		set(level, new BlockPos(x + 5, y, z - 6), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		set(level, new BlockPos(x + 5, y + 1, z - 6), state("shadow_puppet_lamp").setValue(ShadowPuppetLampBlock.LIT, true));
		// Outside: four braziers, one of each flame, pumpkin stacks between them and witch hats floating over them.
		int dx = 13;
		for (WitchFireBrazierBlock.Flame flame : WitchFireBrazierBlock.Flame.values()) {
			set(level, new BlockPos(x + dx, y, z - 4), state("witch_fire_brazier").setValue(WitchFireBrazierBlock.FLAME, flame));
			set(level, new BlockPos(x + dx, y + 3, z - 5), state("floating_witch_hat").setValue(FloatingWitchHatBlock.LIT, true));
			dx += 2;
		}
		for (int px : new int[] {14, 16, 18}) {
			set(level, new BlockPos(x + px, y, z - 2), state("mini_pumpkin_stack").setValue(MiniPumpkinStackBlock.FACING, Direction.SOUTH));
		}
	}
}
