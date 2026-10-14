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
 * Client game test for the Bulk Chemical Reactor (docs/features/industrial-bulk-chemical-reactor.md), dissolving hydrogen
 * chloride in water with its lamps and activity strip lit and its stirring drive turning: from the front left (the
 * control panel, the access plate, the acid outlet and the ore chute), straight on, from the back right (the motor, the
 * absorption inlet and the junction box) and at night, then its screen while it works and once a full acid tank has
 * stopped it. CI job {@code client}.
 */
public class BulkChemicalReactorClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 3, z - 18, x + 8, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 18, x + 8, y + 8, z + 4));
			context.waitTicks(10);
			// Facing south, towards the camera: it fills x..x+3, y..y+3 and z-9..z-6.
			BlockPos reactor = origin.offset(0, 0, -6);
			server.runOnServer(minecraft -> build(minecraft.overworld(), reactor));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), reactor).status().state().working());
			check(working, "The Bulk Chemical Reactor should be working for its pictures");

			// Kept fed and charged before each picture, as pipes and a cable would keep it.
			double cx = x + 2.0;
			double cz = z - 7.0;
			feed(server, reactor);
			shoot(context, singleplayer, x - 3.5, y, z - 1.0, cx, y + 2.0, cz, "jugcraft_bulk_chemical_reactor");
			// Straight on: the access plate, the activity window, the outlet, the chute and the control panel.
			feed(server, reactor);
			shoot(context, singleplayer, cx, y, z + 0.5, cx, y + 2.0, cz, "jugcraft_bulk_chemical_reactor_front");
			feed(server, reactor);
			shoot(context, singleplayer, x + 7.0, y, z - 14.0, cx, y + 2.0, cz, "jugcraft_bulk_chemical_reactor_back");
			server.runCommand("time set midnight");
			feed(server, reactor);
			shoot(context, singleplayer, x - 3.5, y, z - 1.0, cx, y + 2.0, cz, "jugcraft_bulk_chemical_reactor_night");
			server.runCommand("time set noon");

			// Its screen while it works, opened from the front.
			feed(server, reactor);
			place(context, singleplayer, x + 0.5, y, z - 3.5, x + 0.5, y + 0.5, z - 6.0);
			context.getInput().lookAt(reactor);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_bulk_chemical_reactor_screen");

			// A full acid tank: the gas waits, and the terminal says why.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), reactor);
				machine.cancel();
				machine.tanks().output(0).drain(machine.tanks().output(0).millibuckets());
				machine.tanks().output(0).fill(PetroFluids.HYDROCHLORIC_ACID.source(), machine.form().profile().bufferMb());
			});
			context.waitTicks(10);
			MachineLifecycle state = server.computeOnServer(minecraft -> machine(minecraft.overworld(), reactor).status().state());
			check(state == MachineLifecycle.OUTPUT_BLOCKED, "The gas should wait on the full acid tank, not be " + state);
			context.takeScreenshot("jugcraft_bulk_chemical_reactor_product_full");
			context.setScreen(() -> null);
		}
	}

	/** The Bulk Chemical Reactor, facing south, charged and dissolving hydrogen chloride. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.BULK_CHEMICAL_REACTOR.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.BULK_CHEMICAL_REACTOR.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		top(machine(level, pos));
	}

	/**
	 * Tops up its store, its water and its gas (leaving room for a cancelled batch's to come back), and draws its acid
	 * off, as pipes and a cable would.
	 */
	private static void feed(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> top(machine(minecraft.overworld(), pos)));
	}

	private static void top(FormMachineBlockEntity machine) {
		((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
		machine.tanks().input(0).fill(Fluids.WATER, Math.max(0, 4000 - machine.tanks().input(0).millibuckets()));
		machine.tanks().input(1).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(), Math.max(0, 2000 - machine.tanks().input(1).millibuckets()));
		machine.tanks().output(0).drain(machine.tanks().output(0).millibuckets());
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
