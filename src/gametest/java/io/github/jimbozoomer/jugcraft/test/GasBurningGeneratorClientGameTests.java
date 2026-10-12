package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
import io.github.jimbozoomer.jugcraft.machine.form.MachineStatus;
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

/**
 * Client game test for the Gas Burning Generator (docs/features/industrial-gas-burning-generator.md), burning hydrogen
 * with its coupling and intake fan turning: from the front left (the intake, the ignition box, the heat shield and the
 * generator housing), straight on, from the back right (the terminal cabinet and its socket, the exhaust outlet), from
 * its right side (the gas inlet), and shut in the dark so only its lamps and sight glasses glow; then its screen while
 * it generates and once its store is full. CI job {@code client}.
 */
public class GasBurningGeneratorClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 3, z - 16, x + 10, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 10, y + 9, z + 4));
			context.waitTicks(10);
			// Facing south, towards the camera: it fills x-1..x+2, y..y+3 and z-10..z-6, the ignition box at x-1, z-6.
			BlockPos generator = origin.offset(-1, 0, -6);
			server.runOnServer(minecraft -> build(minecraft.overworld(), generator));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), generator).status().state().working());
			check(working, "The generator should be making power for its pictures");

			// Its store fills in 47 seconds with nothing drawing it: emptied before each picture, as a cable would, and
			// its fuel topped up.
			double cx = x + 1.0;
			double cz = z - 7.5;
			feed(server, generator);
			shoot(context, singleplayer, x - 4.5, y, z - 1.5, cx, y + 1.5, cz, "jugcraft_gas_burning_generator");
			feed(server, generator);
			shoot(context, singleplayer, cx, y, z - 0.5, cx, y + 1.5, cz, "jugcraft_gas_burning_generator_front");
			feed(server, generator);
			shoot(context, singleplayer, x + 5.5, y, z - 14.5, cx, y + 1.5, cz, "jugcraft_gas_burning_generator_back");
			feed(server, generator);
			shoot(context, singleplayer, x + 7.5, y, cz, cx, y + 1.5, cz, "jugcraft_gas_burning_generator_side");

			// Its screen while it works, opened from the ignition box.
			feed(server, generator);
			place(context, singleplayer, x - 0.5, y, z - 3.0, x - 0.5, y + 0.5, z - 5.5);
			context.getInput().lookAt(generator);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_gas_burning_generator_screen");

			// A full store: nothing draws its power, so it idles and keeps its fuel.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), generator);
				((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
			});
			context.waitTicks(10);
			MachineStatus full = server.computeOnServer(minecraft -> machine(minecraft.overworld(), generator).status());
			check(full.state() == MachineLifecycle.IDLE && full.reason() == MachineStatus.Reason.POWER_FULL,
					"A full store should idle the generator, not leave it " + full);
			context.takeScreenshot("jugcraft_gas_burning_generator_store_full");
			context.setScreen(() -> null);

			// Shut in the dark at midnight, working: its lamps and sight glasses glow. Last, as the box stays.
			server.runCommand("time set midnight");
			server.runCommand("fill %d %d %d %d %d %d minecraft:black_concrete outline".formatted(x - 8, y - 1, z - 16, x + 10, y + 8, z + 4));
			feed(server, generator);
			shoot(context, singleplayer, x - 4.5, y, z - 1.5, cx, y + 1.5, cz, "jugcraft_gas_burning_generator_dark");
		}
	}

	/** The generator, facing south, its tank full of hydrogen. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.GAS_BURNING_GENERATOR.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.GAS_BURNING_GENERATOR.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		machine(level, pos).tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 2000);
	}

	/** Empties the store and tops up the hydrogen, so it keeps generating. */
	private static void feed(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> {
			FormMachineBlockEntity machine = machine(minecraft.overworld(), pos);
			((SimpleEnergyStorage) machine.energy()).setAmount(0);
			int room = machine.tanks().input(0).capacityMb() - machine.tanks().input(0).millibuckets();
			if (room > 0) {
				machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), room);
			}
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
