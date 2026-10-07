package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the turning machine parts (client/MachineRotors): a powered giant sawmill cutting oak logs and a
 * powered giant sieve sifting gravel, both running (amber lamps lit). Shots: both machines together; the sawmill's side
 * close up twice, three ticks apart, so the blade (7 degrees a tick, 24 teeth) and its belt drive show as turned 21
 * degrees between them; and the sieve from the front and from its corner, its vibrator weights spinning. CI job
 * {@code client}.
 */
public class MachineMotionClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 4, y - 3, z - 12, x + 14, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 12, x + 14, y + 8, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// Long enough for both machines to start (they ease up to speed over about half a second).
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			// Both machines from the south: the sawmill's side (blade, pulleys and belt) and the sieve's front (deck).
			shoot(context, singleplayer, x + 5, y + 2, z - 0.5, 180, 25, "jugcraft_sawmill_sieve_running");
			// The sawmill's blade face on, then again three ticks later: the blade, the maker's plate and the pulleys' spokes
			// have turned 21 degrees (not a multiple of the 15-degree tooth pitch).
			place(context, singleplayer, x + 2.75, y, z - 1.5, 180, 8);
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_sawmill_blade_a");
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_sawmill_blade_b");
			// The sieve from the front (its tilted deck faces the camera) and from its front left corner.
			shoot(context, singleplayer, x + 9, y + 1, z - 3, 180, 32, "jugcraft_sieve_running");
			shoot(context, singleplayer, x + 7, y + 1, z - 3.5, 220, 28, "jugcraft_sieve_running_corner");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z, int yaw,
			int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted((int) Math.floor(x), y - 1, (int) Math.floor(z)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f %d %d", x, y, z, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z, int yaw,
			int pitch, String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	/**
	 * The sawmill faces west, so its right side (pulleys, belt and motor) faces south, the camera; the blade turns in a
	 * plane facing the camera. The sieve faces south. Both are formed, charged full and given work.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		BlockPos sawmill = origin.offset(0, 0, -6);
		BlockPos sieve = origin.offset(8, 0, -6);
		running(level, sawmill, MachineKind.SAWMILL, Direction.WEST, new ItemStack(Items.OAK_LOG, 64));
		running(level, sieve, MachineKind.SIEVE, Direction.SOUTH, new ItemStack(Items.GRAVEL, 64));
		// A log waiting at the sawmill's front (its west end) and a gravel heap by the sieve, for scale.
		level.setBlock(origin.offset(-2, 0, -5), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(origin.offset(11, 0, -8), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static void running(ServerLevel level, BlockPos pos, MachineKind kind, Direction facing, ItemStack input) {
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(kind);
		BlockState state = block.formed(block.defaultBlockState().setValue(MachineBlock.FACING, facing));
		level.setBlock(pos, state, Block.UPDATE_ALL);
		block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
			if (machine.energyFor(null) instanceof SimpleEnergyStorage energy) {
				energy.setAmount(energy.getCapacity());
			}
			machine.setItem(0, input);
		}
	}
}
