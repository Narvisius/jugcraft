package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.agriculture.WerewolfRugBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for full-moon werewolves (fall addition 23): by day, wolfsbane wild on a forest floor and potted on a
 * stump, the three rugs (brown, snow and shadow) before a fireplace, and the silver dagger, silver arrows and the three
 * pelts in frames; then, on the full-moon night (with night vision, to see by), the three kinds of werewolf posed in a
 * spruce clearing, the shadow werewolf snarling, up close and from further off. CI job {@code client}.
 */
public class WerewolfClientGameTests implements FabricClientGameTest {
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
			server.runCommand("difficulty normal");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 16, x + 14, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 14, y + 14, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			int i = 0;
			for (String item : new String[] {"silver_dagger", Werewolves.SILVER_ARROW, Werewolf.Kind.BROWN.pelt, Werewolf.Kind.SNOW.pelt,
					Werewolf.Kind.SHADOW.pelt}) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 7 + i++, y + 2, z - 5, item));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 2, y + 2, z + 1, 180, 35, "jugcraft_wolfsbane");
			shoot(context, singleplayer, x + 9, y + 1, z, 180, 30, "jugcraft_werewolf_rug_and_silver");

			// The full-moon night: the first night of the world's first moon cycle.
			server.runCommand("time set midnight");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			context.waitTicks(10);
			server.runOnServer(minecraft -> werewolves(minecraft.overworld(), origin));
			context.waitTicks(40);
			shoot(context, singleplayer, x + 3, y + 1, z - 3, 180, 3, "jugcraft_werewolf");
			shoot(context, singleplayer, x + 3, y + 3, z + 2, 180, 20, "jugcraft_werewolves_full_moon");
			server.runCommand("effect clear @p");
			server.runCommand("kill @e[type=jugcraft:werewolf]");
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

	/**
	 * A spruce clearing: trunks and podzol, wolfsbane growing through it and potted on a stump; by a cobbled fireplace
	 * on planks, the rug.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		for (int dx = -2; dx <= 6; dx++) {
			for (int dz = -9; dz <= -2; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.PODZOL.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		for (int[] trunk : new int[][] {{-2, -9}, {6, -9}, {-1, -12}, {5, -13}, {2, -14}}) {
			for (int dy = 0; dy <= 6; dy++) {
				level.setBlock(new BlockPos(x + trunk[0], y + dy, z + trunk[1]), Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		Block wolfsbane = JugcraftAgriculture.block(Werewolves.WOLFSBANE);
		for (int[] at : new int[][] {{0, -3}, {1, -4}, {3, -3}, {4, -5}, {0, -6}, {2, -7}, {5, -4}, {-1, -5}}) {
			level.setBlock(new BlockPos(x + at[0], y, z + at[1]), wolfsbane.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(new BlockPos(x + 2, y, z - 5), Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 2, y + 1, z - 5), JugcraftAgriculture.block("potted_" + Werewolves.WOLFSBANE).defaultBlockState(),
				Block.UPDATE_ALL);
		// The fireplace corner: planks, a cobbled wall for the frames, and the rug before a campfire.
		for (int dx = 7; dx <= 11; dx++) {
			for (int dz = -5; dz <= -1; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int dy = 0; dy <= 4; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 6), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.setBlock(new BlockPos(x + 9, y, z - 5), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		int dx = 8;
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			level.setBlock(new BlockPos(x + dx++, y, z - 3), JugcraftAgriculture.block(kind.rug).defaultBlockState()
					.setValue(WerewolfRugBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
		}
	}

	/**
	 * The three kinds posed in the clearing: the brown werewolf side-on, the snow werewolf facing the camera, the shadow
	 * werewolf snarling.
	 */
	private static void werewolves(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// Clear of the trunks: one standing in a trunk suffocates, and flashes red in the picture.
		float[][] poses = {{1.0F, -8.5F, 70.0F}, {3.5F, -7.5F, 0.0F}, {6.0F, -6.8F, -15.0F}};
		for (int n = 0; n < poses.length; n++) {
			float[] pose = poses[n];
			Werewolf werewolf = JugcraftAgriculture.WEREWOLF.create(level, EntitySpawnReason.COMMAND);
			if (werewolf != null) {
				Werewolf.Kind kind = Werewolf.Kind.values()[n];
				werewolf.setKind(kind);
				werewolf.setHealth(werewolf.getMaxHealth());
				werewolf.setAggressive(kind == Werewolf.Kind.SHADOW);
				werewolf.setNoAi(true);
				werewolf.setPersistenceRequired();
				werewolf.snapTo(x + pose[0], y, z + pose[1], pose[2], 0.0F);
				werewolf.setYHeadRot(pose[2]);
				werewolf.setYBodyRot(pose[2]);
				level.addFreshEntity(werewolf);
			}
		}
	}
}
