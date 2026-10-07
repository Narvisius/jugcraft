package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Squirrel;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for squirrels and acorns (fall addition 24): in an oak wood, a red squirrel sitting up on a stump
 * with an acorn in its paws, a grey one and a kit on the grass with acorns lying about, and an oak sapling one of them
 * planted; up close, side-on, and the wood from further off; then acorns and roasted acorns in frames by a smoker. CI
 * job {@code client}.
 */
public class SquirrelClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 16, x + 14, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 14, y + 12, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			int i = 0;
			for (String item : new String[] {Squirrel.ACORN, "roasted_acorns"}) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 9 + 2 * i++, y + 1, z - 6, item));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 2, y, z - 1, 180, 24, "jugcraft_squirrel_acorn");
			shoot(context, singleplayer, x + 2, y, z, 180, 22, "jugcraft_squirrels");
			shoot(context, singleplayer, x + 6, y, z - 3, 90, 22, "jugcraft_squirrel_side");
			shoot(context, singleplayer, x + 3, y + 2, z + 4, 180, 15, "jugcraft_squirrel_wood");
			shoot(context, singleplayer, x + 10, y, z - 3, 180, 10, "jugcraft_acorns");
			server.runCommand("kill @e[type=jugcraft:squirrel]");
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

	/** A squirrel standing still at (x, y, z) facing {@code yaw}: red or grey, a kit, with an acorn in its paws or not. */
	private static void squirrel(ServerLevel level, double x, double y, double z, float yaw, boolean grey, boolean kit, boolean acorn) {
		Squirrel squirrel = JugcraftAgriculture.SQUIRREL.create(level, EntitySpawnReason.COMMAND);
		if (squirrel == null) {
			return;
		}
		squirrel.setNoAi(true);
		squirrel.setPersistenceRequired();
		squirrel.setGrey(grey);
		if (kit) {
			squirrel.setAge(-24000);
		}
		if (acorn) {
			squirrel.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Squirrel.acorn()));
		}
		squirrel.snapTo(x, y, z, yaw, 0.0F);
		squirrel.setYHeadRot(yaw);
		squirrel.setYBodyRot(yaw);
		level.addFreshEntity(squirrel);
	}

	/** An oak: a trunk {@code height} tall under a round crown of leaves that won't decay. */
	private static void oak(ServerLevel level, int x, int y, int z, int height) {
		var leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int dy = height - 2; dy <= height + 1; dy++) {
			int r = dy >= height ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 1 || (dx + dz + dy) % 2 == 0)) {
						continue;
					}
					level.setBlock(new BlockPos(x + dx, y + dy, z + dz), leaves, Block.UPDATE_CLIENTS);
				}
			}
		}
		for (int dy = 0; dy < height; dy++) {
			level.setBlock(new BlockPos(x, y + dy, z), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** An acorn lying on the grass that no one picks up. */
	private static void acornOnGround(ServerLevel level, double x, double y, double z) {
		ItemEntity acorn = new ItemEntity(level, x, y, z, new ItemStack(Squirrel.acorn()));
		acorn.setDeltaMovement(0.0, 0.0, 0.0);
		acorn.setNeverPickUp();
		level.addFreshEntity(acorn);
	}

	/**
	 * An oak wood: three oaks, a stump, ferns and grass, an oak sapling sprung from a buried acorn; on and about the
	 * stump, the squirrels and some acorns; off to one side, a log wall with a smoker below the frames.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		oak(level, x - 1, y, z - 6, 6);
		oak(level, x + 6, y, z - 7, 7);
		oak(level, x + 2, y, z - 11, 6);
		level.setBlock(new BlockPos(x + 2, y, z - 3), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 4, y, z - 5), Blocks.OAK_SAPLING.defaultBlockState(), Block.UPDATE_ALL);
		for (int[] at : new int[][] {{0, -3}, {5, -4}, {-2, -4}, {1, -8}, {4, -9}, {7, -4}}) {
			level.setBlock(new BlockPos(x + at[0], y, z + at[1]), (at[0] + at[1]) % 2 == 0 ? Blocks.FERN.defaultBlockState()
					: Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
		}
		// Facing the camera (south, yaw 0): the red one up on the stump with its acorn, the grey one and a kit below.
		squirrel(level, x + 2.5, y + 1.0, z - 2.5, 0.0F, false, false, true);
		squirrel(level, x + 3.6, y, z - 2.0, 35.0F, true, false, false);
		squirrel(level, x + 1.4, y, z - 1.9, -25.0F, false, true, false);
		acornOnGround(level, x + 1.0, y + 0.1, z - 3.1);
		acornOnGround(level, x + 3.9, y + 0.1, z - 3.2);
		acornOnGround(level, x + 1.8, y + 0.1, z - 2.4);
		// The larder: a log wall for the frames, and a smoker that roasts acorns.
		for (int dx = 8; dx <= 12; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 7), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.setBlock(new BlockPos(x + 10, y, z - 6), Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
				Block.UPDATE_ALL);
	}
}
