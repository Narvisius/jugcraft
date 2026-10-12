package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Client game test for the Electrolytic Separator (docs/features/industrial-electrolytic-separator.md), splitting water
 * with its lamp and level strips lit: from the front left (the control box, the door, the bus connection, the lye
 * return and the outlet collars), straight on, from the back right (the feed inlet) and at night, then its screen
 * while it works and once a full lye tank has stopped brine. CI job {@code client}.
 */
public class ElectrolyticSeparatorClientGameTests implements FabricClientGameTest {
	/** A standing player's eyes above their feet. */
	private static final double EYE_HEIGHT = 1.62;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left.
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 3, z - 14, x + 8, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 14, x + 8, y + 8, z + 4));
			context.waitTicks(10);
			// Facing south, towards the camera: it fills x..x+1, y..y+2 and z-7..z-6.
			BlockPos separator = origin.offset(0, 0, -6);
			server.runOnServer(minecraft -> build(minecraft.overworld(), separator));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), separator).status().state().working());
			check(working, "The Separator should be working for its pictures");

			// Its 60,000 JE store runs it for about 230 ticks at 256 JE a tick: topped up before each picture, as a cable
			// would keep it.
			double cx = x + 1.0;
			double cz = z - 6.0;
			charge(server, separator);
			shoot(context, singleplayer, x - 1.5, y, z - 2.5, cx, y + 1.5, cz, "jugcraft_electrolytic_separator");
			// Straight on: the ports and controls of the front.
			charge(server, separator);
			shoot(context, singleplayer, cx, y, z - 1.0, cx, y + 1.5, cz, "jugcraft_electrolytic_separator_front");
			charge(server, separator);
			shoot(context, singleplayer, x + 3.5, y, z - 9.5, cx, y + 1.5, cz, "jugcraft_electrolytic_separator_back");
			server.runCommand("time set midnight");
			charge(server, separator);
			shoot(context, singleplayer, x - 1.5, y, z - 2.5, cx, y + 1.5, cz, "jugcraft_electrolytic_separator_night");
			server.runCommand("time set noon");

			// Its screen while it works, opened from the front.
			charge(server, separator);
			place(context, singleplayer, x + 0.5, y, z - 3.5, cx, y + 1.0, cz);
			context.getInput().lookAt(separator);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_electrolytic_separator_screen");

			// A full lye tank: brine waits, and the terminal says why.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), separator);
				machine.cancel();
				machine.tanks().input(0).drain(machine.tanks().input(0).millibuckets());
				machine.tanks().output(2).fill(PetroFluids.LYE.source(), machine.form().profile().bufferMb());
				machine.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
			});
			context.waitTicks(10);
			MachineLifecycle state = server.computeOnServer(minecraft -> machine(minecraft.overworld(), separator).status().state());
			check(state == MachineLifecycle.OUTPUT_BLOCKED, "Brine should wait on the full lye tank, not be " + state);
			context.takeScreenshot("jugcraft_electrolytic_separator_lye_full");
			context.setScreen(() -> null);
		}
	}

	/** The Separator, facing south, charged and splitting a bucket of water. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.ELECTROLYTIC_SEPARATOR.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.ELECTROLYTIC_SEPARATOR.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		FormMachineBlockEntity machine = machine(level, pos);
		((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
		machine.tanks().input(0).fill(Fluids.WATER, 1000);
	}

	private static void charge(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> {
			FormMachineBlockEntity machine = machine(minecraft.overworld(), pos);
			((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
		});
	}

	private static FormMachineBlockEntity machine(ServerLevel level, BlockPos pos) {
		return (FormMachineBlockEntity) level.getBlockEntity(pos);
	}

	/**
	 * Stands the camera at (x, y, z) with its eyes on (tx, ty, tz), on a barrier, and waits for the world to draw. The
	 * teleport's "facing" aims from the feet, so the point it is given is lowered by the eye height.
	 */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z,
			double tx, double ty, double tz) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted((int) Math.floor(x), y - 1, (int) Math.floor(z)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f facing %.2f %.2f %.2f", x, y, z, tx, ty - EYE_HEIGHT, tz));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z,
			double tx, double ty, double tz, String name) {
		place(context, singleplayer, x, y, z, tx, ty, tz);
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
