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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Client game test for the Bulk Fermenter (docs/features/industrial-bulk-fermenter.md), fermenting wheat with its drive
 * turning: from the front left, straight on (the level strip, the manway, the broth outlet and the instrument box), from
 * the back right (the services box, the gas flange and the water inlet) and at night; then its screen while it works and
 * once a full alcohol tank has stopped it. CI job {@code client}.
 */
public class BulkFermenterClientGameTests implements FabricClientGameTest {
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
			// Facing south, towards the camera: it fills x-2..x+2, y..y+5 and z-11..z-7.
			BlockPos fermenter = origin.offset(-2, 0, -7);
			server.runOnServer(minecraft -> build(minecraft.overworld(), fermenter));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			boolean working = server.computeOnServer(minecraft -> machine(minecraft.overworld(), fermenter).status().state().working());
			check(working, "The Fermenter should be working for its pictures");

			// Kept fed and charged before each picture, as pipes and a cable would keep it.
			double cx = x + 0.5;
			double cz = z - 8.5;
			feed(server, fermenter);
			shoot(context, singleplayer, x - 6.5, y + 1, z + 1.5, cx, y + 3.0, cz, "jugcraft_bulk_fermenter");
			// Straight on: the level strip, the manway, the broth outlet and the instrument box of the front.
			feed(server, fermenter);
			shoot(context, singleplayer, cx, y + 1, z + 2.5, cx, y + 3.0, cz, "jugcraft_bulk_fermenter_front");
			feed(server, fermenter);
			shoot(context, singleplayer, x + 8.5, y + 1, z - 17.5, cx, y + 3.0, cz, "jugcraft_bulk_fermenter_back");
			server.runCommand("time set midnight");
			feed(server, fermenter);
			shoot(context, singleplayer, x - 6.5, y + 1, z + 1.5, cx, y + 3.0, cz, "jugcraft_bulk_fermenter_night");
			server.runCommand("time set noon");

			// Its screen while it works, opened from the front.
			feed(server, fermenter);
			place(context, singleplayer, x - 1.5, y, z - 4.5, x - 1.5, y + 0.5, z - 7.0);
			context.getInput().lookAt(fermenter);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_bulk_fermenter_screen");

			// A full alcohol tank: the wheat waits, and the terminal says why.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = machine(minecraft.overworld(), fermenter);
				machine.cancel();
				machine.tanks().output(0).drain(machine.tanks().output(0).millibuckets());
				machine.tanks().output(0).fill(PetroFluids.BIOETHANOL.source(), machine.form().profile().bufferMb());
			});
			context.waitTicks(10);
			MachineLifecycle state = server.computeOnServer(minecraft -> machine(minecraft.overworld(), fermenter).status().state());
			check(state == MachineLifecycle.OUTPUT_BLOCKED, "The wheat should wait on the full alcohol tank, not be " + state);
			context.takeScreenshot("jugcraft_bulk_fermenter_product_full");
			context.setScreen(() -> null);
		}
	}

	/** The Bulk Fermenter, facing south, charged and fermenting wheat. */
	private static void build(ServerLevel level, BlockPos pos) {
		BlockState state = IndustrialForms.BULK_FERMENTER.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		IndustrialForms.BULK_FERMENTER.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		top(machine(level, pos));
	}

	/**
	 * Tops up its store, its wheat and its water (leaving room for a cancelled batch's to come back), and draws its
	 * alcohol and gas off, as a conveyor, pipes and a cable would.
	 */
	private static void feed(TestServerContext server, BlockPos pos) {
		server.runOnServer(minecraft -> top(machine(minecraft.overworld(), pos)));
	}

	private static void top(FormMachineBlockEntity machine) {
		((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
		machine.tanks().input(0).fill(Fluids.WATER, Math.max(0, 4000 - machine.tanks().input(0).millibuckets()));
		machine.container().setItem(0, new ItemStack(Items.WHEAT, 32));
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
