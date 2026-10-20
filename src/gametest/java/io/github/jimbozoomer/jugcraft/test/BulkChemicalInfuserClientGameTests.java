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

/**
 * Client game test for the Bulk Chemical Infuser (docs/features/industrial-bulk-chemical-infuser.md), making hydrogen
 * chloride four batches at once with its fan turning: from the front left, straight on (the bed drawer, the inspection
 * port and the control box), from the back right (the product collar, the water drain and the cooling jacket) and at
 * night; then its screen while it works and once a full product tank has stopped it. CI job {@code client}.
 */
public class BulkChemicalInfuserClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 3, z - 20, x + 12, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 20, x + 12, y + 11, z + 4));
			context.waitTicks(10);
			// Facing south, towards the camera: it fills x-2..x+1, y..y+5 and z-12..z-7.
			BlockPos infuser = origin.offset(-2, 0, -7);
			server.runOnServer(minecraft -> build(minecraft.overworld(), infuser));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), infuser).status().state().working());
			check(working, "The Infuser should be working for its pictures");

			// Kept fed and charged before each picture, as pipes and a cable would keep it.
			double cx = x;
			double cz = z - 9.0;
			feed(server, infuser);
			shoot(context, singleplayer, x - 6.5, y + 1, z + 1.5, cx, y + 3.0, cz, "jugcraft_bulk_chemical_infuser");
			// Straight on: the bed drawer, the inspection port, the heat gauge and the control box.
			feed(server, infuser);
			shoot(context, singleplayer, cx, y + 1, z + 2.5, cx, y + 3.0, cz, "jugcraft_bulk_chemical_infuser_front");
			feed(server, infuser);
			shoot(context, singleplayer, x + 8.5, y + 1, z - 17.5, cx, y + 3.0, cz, "jugcraft_bulk_chemical_infuser_back");
			server.runCommand("time set midnight");
			feed(server, infuser);
			shoot(context, singleplayer, x - 6.5, y + 1, z + 1.5, cx, y + 3.0, cz, "jugcraft_bulk_chemical_infuser_night");
			server.runCommand("time set noon");

			// Its screen while it works, opened from the front.
			feed(server, infuser);
			place(context, singleplayer, x - 1.5, y, z - 4.5, x - 1.5, y + 0.5, z - 7.0);
			context.getInput().lookAt(infuser);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_bulk_chemical_infuser_screen");

			// A full product tank: the gases wait, and the terminal says why.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), infuser);
				machine.cancel();
				machine.tanks().output(0).drain(machine.tanks().output(0).millibuckets());
				machine.tanks().output(0).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(), machine.form().profile().bufferMb());
			});
			context.waitTicks(10);
			MachineLifecycle state = server.computeOnServer(minecraft -> machine(minecraft.overworld(), infuser).status().state());
			check(state == MachineLifecycle.OUTPUT_BLOCKED, "The gases should wait on the full product tank, not be " + state);
			context.takeScreenshot("jugcraft_bulk_chemical_infuser_product_full");
			context.setScreen(() -> null);
		}
	}

	/** The Bulk Chemical Infuser, facing south, charged and making hydrogen chloride. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.BULK_CHEMICAL_INFUSER.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.BULK_CHEMICAL_INFUSER.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		top(machine(level, pos));
	}

	/**
	 * Tops up its store and its two gases (leaving room for a cancelled batch's to come back), and draws its product and
	 * water off, as pipes and a cable would.
	 */
	private static void feed(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> top(machine(minecraft.overworld(), pos)));
	}

	private static void top(FormMachineBlockEntity machine) {
		((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
		machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), Math.max(0, 4000 - machine.tanks().input(0).millibuckets()));
		machine.tanks().input(1).fill(PetroFluids.CHLORINE.fluid(), Math.max(0, 4000 - machine.tanks().input(1).millibuckets()));
		machine.tanks().output(0).drain(machine.tanks().output(0).millibuckets());
		machine.tanks().output(1).drain(machine.tanks().output(1).millibuckets());
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
