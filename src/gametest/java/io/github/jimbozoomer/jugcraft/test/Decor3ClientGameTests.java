package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GraveMoundBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PopUpSkeletonBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ScareProp;
import io.github.jimbozoomer.jugcraft.agriculture.ScarePropBlockEntity;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Client game test for the graveyard decorations: a wrought-iron fence with a shut and an open gate, a row of Grave
 * Mounds (three with a hand up, held there by their timers), a Mourning Angel, a crypt front of crypt stone with
 * pillars, a chiseled frieze and a Crypt Door, and two Pop-Up Skeletons (one sprung); photographed by day and at night
 * (CI job {@code client}). The camera keeps out of the props' reach, so the lowered ones stay down.
 */
public class Decor3ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 26, x + 30, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 26, x + 30, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 5, y + 6, z + 6, 180, 25, "jugcraft_graveyard");
			shoot(context, singleplayer, x + 5, y + 4, z - 4, 180, 40, "jugcraft_grave_mounds");
			shoot(context, singleplayer, x + 5, y + 1, z - 2, 180, 8, "jugcraft_cemetery_fence");
			shoot(context, singleplayer, x + 5, y + 2, z - 12, 180, 5, "jugcraft_crypt");
			shoot(context, singleplayer, x + 11, y + 1, z - 8, 180, 5, "jugcraft_mourning_angel");
			shoot(context, singleplayer, x + 15, y + 1, z + 1, 180, 20, "jugcraft_pop_up_skeletons");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 5, y + 6, z + 6, 180, 25, "jugcraft_graveyard_night");
			shoot(context, singleplayer, x + 11, y + 1, z - 8, 180, 5, "jugcraft_mourning_angel_night");
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

	/** Keeps a raised scare prop up through the shots: its saved timer says it comes down (and may go again) far off. */
	private static void holdUp(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof ScarePropBlockEntity prop) {
			CompoundTag timers = new CompoundTag();
			timers.putLong("down_at", Long.MAX_VALUE / 2);
			timers.putLong("ready_at", Long.MAX_VALUE / 2);
			prop.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), timers));
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The fence along the front of the graveyard, with a shut gate and an open one.
		for (int dx = -2; dx <= 12; dx++) {
			BlockPos pos = new BlockPos(x + dx, y, z - 6);
			if (dx == 3 || dx == 7) {
				set(level, pos, state("cemetery_gate").setValue(FenceGateBlock.FACING, Direction.SOUTH).setValue(FenceGateBlock.OPEN, dx == 7));
			} else {
				set(level, pos, state("cemetery_fence"));
			}
		}
		// Join each post to its neighbours, as placing them by hand would.
		for (int dx = -2; dx <= 12; dx++) {
			BlockPos pos = new BlockPos(x + dx, y, z - 6);
			set(level, pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
		}

		// Grave mounds facing the fence; every other one has its hand up, held there for the shots by its timer.
		for (int i = 0; i < 5; i++) {
			BlockPos pos = new BlockPos(x + 1 + i * 2, y, z - 9);
			boolean up = i % 2 == 0;
			set(level, pos, state("grave_mound").setValue(GraveMoundBlock.FACING, Direction.SOUTH).setValue(ScareProp.RAISED, up));
			if (up) {
				holdUp(level, pos);
			}
		}

		// The mourning angel at the end of the row.
		BlockPos angel = new BlockPos(x + 11, y, z - 11);
		BlockState lower = state("mourning_angel").setValue(MourningAngelBlock.FACING, Direction.SOUTH);
		set(level, angel, lower);
		set(level, angel.above(), lower.setValue(MourningAngelBlock.HALF, DoubleBlockHalf.UPPER));

		// A crypt front: crypt stone with two pillars, a chiseled frieze and a crypt door in the middle.
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				String id = dy == 4 && dx >= 3 && dx <= 7 ? "chiseled_crypt_stone" : (dx == 2 || dx == 8) && dy < 4 ? "crypt_stone_pillar" : "crypt_stone";
				set(level, new BlockPos(x + dx, y + dy, z - 17), state(id));
			}
		}
		BlockPos door = new BlockPos(x + 5, y, z - 17);
		BlockState doorState = state("crypt_door").setValue(DoorBlock.FACING, Direction.SOUTH);
		set(level, door, doorState);
		set(level, door.above(), doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

		// Two pop-up skeletons on the lawn: one sprung (held by its timer), one in its crate.
		BlockPos sprung = new BlockPos(x + 14, y, z - 3);
		set(level, sprung, state("pop_up_skeleton").setValue(PopUpSkeletonBlock.FACING, Direction.SOUTH).setValue(ScareProp.RAISED, true));
		holdUp(level, sprung);
		set(level, new BlockPos(x + 16, y, z - 3), state("pop_up_skeleton").setValue(PopUpSkeletonBlock.FACING, Direction.SOUTH));
	}
}
