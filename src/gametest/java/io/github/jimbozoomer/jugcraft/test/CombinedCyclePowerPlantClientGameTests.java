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
 * Client game test for the Combined Cycle Power Plant (docs/features/industrial-factory-payoffs.md), burning methane
 * with its shaft turning: from the front left (the controller, the generator under the filter house, the gas turbine),
 * straight on, from the back right (the stack, the steam turbine and the fuel skid), from its right side (the boiler),
 * and shut in the dark so only its lamps glow; then its screen while it generates and once its store is full. CI job
 * {@code client}.
 */
public class CombinedCyclePowerPlantClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 14, y - 3, z - 24, x + 14, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 14, y, z - 24, x + 14, y + 12, z + 4));
			context.waitTicks(10);
			// Facing south, towards the camera: six blocks across from x-3, five tall and four deep back from z-8; the
			// controller cabinet at x-3, z-8.
			BlockPos generator = origin.offset(-3, 0, -8);
			server.runOnServer(minecraft -> build(minecraft.overworld(), generator));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			// At 2,048 JE a tick its 120,000 JE store fills in under three seconds: empty it first, as a cable would.
			feed(server, generator);
			context.waitTicks(5);
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), generator).status().state().working());
			check(working, "The generator should be making power for its pictures");

			// Its store fills in under three seconds with nothing drawing it: emptied before each picture, as a cable would, and
			// its fuel topped up.
			double cx = x;
			double cz = z - 9.5;
			feed(server, generator);
			shoot(context, singleplayer, x - 9.5, y + 2, z + 2.5, cx, y + 2.0, cz, "jugcraft_combined_cycle_power_plant");
			feed(server, generator);
			shoot(context, singleplayer, cx, y + 1, z + 3.0, cx, y + 2.0, cz, "jugcraft_combined_cycle_power_plant_front");
			feed(server, generator);
			shoot(context, singleplayer, x + 10.5, y + 2, z - 20.5, cx, y + 2.0, cz, "jugcraft_combined_cycle_power_plant_back");
			feed(server, generator);
			shoot(context, singleplayer, x + 12.5, y + 1, cz, cx, y + 2.0, cz, "jugcraft_combined_cycle_power_plant_side");

			// Its screen while it works, opened from the controller cabinet.
			feed(server, generator);
			place(context, singleplayer, x - 2.5, y, z - 5.0, x - 2.5, y + 0.5, z - 7.5);
			context.getInput().lookAt(generator);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_combined_cycle_power_plant_screen");

			// A full store: nothing draws its power, so it idles and keeps its fuel.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), generator);
				((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
			});
			context.waitTicks(10);
			MachineStatus full = server.computeOnServer(minecraft -> machine(minecraft.overworld(), generator).status());
			check(full.state() == MachineLifecycle.IDLE && full.reason() == MachineStatus.Reason.POWER_FULL,
					"A full store should idle the generator, not leave it " + full);
			context.takeScreenshot("jugcraft_combined_cycle_power_plant_store_full");
			context.setScreen(() -> null);

			// Shut in the dark at midnight, working: its lamps and sight glasses glow. Last, as the box stays.
			server.runCommand("time set midnight");
			server.runCommand("fill %d %d %d %d %d %d minecraft:black_concrete outline".formatted(x - 14, y - 1, z - 24, x + 14, y + 11, z + 4));
			feed(server, generator);
			shoot(context, singleplayer, x - 9.5, y + 2, z + 2.5, cx, y + 2.0, cz, "jugcraft_combined_cycle_power_plant_dark");
		}
	}

	/** The power plant, facing south, its tank full of methane. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.COMBINED_CYCLE_PLANT.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.COMBINED_CYCLE_PLANT.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		machine(level, pos).tanks().input(0).fill(PetroFluids.METHANE.fluid(), 2000);
	}

	/** Empties the store and tops up the methane, so it keeps generating. */
	private static void feed(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> {
			FormMachineBlockEntity machine = machine(minecraft.overworld(), pos);
			((SimpleEnergyStorage) machine.energy()).setAmount(0);
			int room = machine.tanks().input(0).capacityMb() - machine.tanks().input(0).millibuckets();
			if (room > 0) {
				machine.tanks().input(0).fill(PetroFluids.METHANE.fluid(), room);
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
