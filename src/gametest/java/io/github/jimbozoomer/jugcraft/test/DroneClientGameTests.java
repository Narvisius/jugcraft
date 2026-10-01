package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.drone.SimpleBuildJobs;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.function.BiPredicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for the Drone Depot and Parties, with screenshots from a real client:
 *
 * <ul>
 * <li>an expanded depot: a 21x19 base with six 5x5 pads one block apart (each forms into a pad with a
 * charger port), a 3x3 supply pickup with the cargo packager beside it, the terminal and one drone of every
 * tier (the depot engine on a bare platform; in survival the Drone Tower builds all this);
 * <li>a close-up of the supply pickup while a drone winches up a crate (hatch open, lift raised);
 * <li>drones in flight over the build site, the finished 21-block wall, and every drone design docked;
 * <li>the terminal screen
 * (OVERVIEW and FLEET tabs) and /party output.
 * </ul>
 */
public class DroneClientGameTests implements FabricClientGameTest {
	private static final int WALL_BLOCKS = 21;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 28, y - 1, z - 12, x + 16, y - 1, z + 30));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 28, y, z - 12, x + 16, y + 14, z + 30));

			// Base: 21 wide (x - 10 .. x + 10), 19 deep (z .. z + 18), level with the floor.
			BlockPos platform = new BlockPos(x - 10, y - 1, z);
			// The terminal stands just west of the base. (In survival a Drone Tower builds the depot; this test
			// drives the depot engine directly, on a bare platform, with every drone tier allowed.)
			BlockPos terminal = platform.offset(-3, 1, 8);
			server.runCommand("setblock %d %d %d jugcraft:drone_depot_terminal[facing=east]".formatted(terminal.getX(), terminal.getY(), terminal.getZ()));
			SimpleBuildJobs[] jobs = new SimpleBuildJobs[1];
			server.runOnServer(minecraft -> jobs[0] = buildDepot(minecraft.overworld(), minecraft.getPlayerList().getPlayers().getFirst(), platform, terminal));
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			setHudHidden(context, true);

			// The supply pickup while a drone winches up its crate: wait until one is right over the hatch.
			BlockPos pickup = platform.offset(10, 1, 15);
			server.runCommand("tp @p %d.5 %d %d.5 -140 38".formatted(pickup.getX() - 4, y + 4, pickup.getZ() + 5));
			waitFor(context, server, terminal, (view, now) -> view.flights.stream().anyMatch(f -> {
				double ticks = f.ticksAt(now, view.syncTime);
				return ticks > f.path().pickupArrive() - 3 && ticks < f.path().pickupArrive() + 4;
			}), 600);
			context.takeScreenshot("jugcraft_drone_pickup_lift");

			// Drones in flight over the depot and the build site.
			server.runCommand("tp @p %d %d %d 0 38".formatted(x, y + 18, z - 10));
			waitFor(context, server, terminal, (view, now) -> view.flights.size() >= 4, 600);
			context.waitTicks(30);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_drones_in_flight");

			// Wait for the wall to be finished and every drone to be home.
			int remaining = WALL_BLOCKS;
			for (int i = 0; i < 150 && remaining > 0; i++) {
				context.waitTicks(20);
				charge(server, terminal);
				remaining = server.computeOnServer(minecraft -> jobs[0].remaining());
			}
			if (remaining > 0) {
				String status = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(terminal) instanceof DroneTerminalBlockEntity t
						? t.status().stream().map(c -> c.getString()).collect(java.util.stream.Collectors.joining(" | ")) : "no terminal");
				throw new AssertionError("The drones left " + remaining + " of " + WALL_BLOCKS + " wall blocks unbuilt. Terminal: " + status);
			}
			waitFor(context, server, terminal, (view, now) -> view.flights.isEmpty(), 1200);
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_drone_depot_done");

			// Every drone design, docked: the large ones on the first row, medium and small on the second.
			String[] names = {"t7_aerostat", "t8_ion_glider", "t9_heavy_lifter"};
			for (int pad = 0; pad < 3; pad++) {
				server.runCommand("tp @p %.1f %d %.1f 180 32".formatted(x - 5.5 + 6 * pad, y + 4, z + 11.0));
				context.waitTicks(15);
				context.takeScreenshot("jugcraft_drone_" + names[pad]);
			}
			server.runCommand("tp @p %.1f %d %.1f 180 36".formatted(x - 2.5, y + 4, z + 17.0));
			context.waitTicks(15);
			context.takeScreenshot("jugcraft_drones_medium_and_small");

			// The terminal screen: sit at the console and use the terminal, then the FLEET tab.
			setHudHidden(context, false);
			server.runCommand("tp @p %d.5 %d %d.5 -90 25".formatted(terminal.getX() - 2, y, terminal.getZ()));
			context.waitTicks(10);
			context.getInput().lookAt(terminal);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(DroneTerminalScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_terminal_overview");
			context.runOnClient(client -> client.gui.screen().children().stream()
					.filter(child -> child instanceof Button button && button.getMessage().getString().equals("FLEET"))
					.findFirst().ifPresent(child -> ((Button) child).onPress(null)));
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_terminal_fleet");
			context.setScreen(() -> null);

			// Parties: the player types /party create and /party info; the replies show in chat.
			context.runOnClient(client -> client.player.connection.sendCommand("party create"));
			context.waitTicks(5);
			context.runOnClient(client -> client.player.connection.sendCommand("party info"));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_party_info");

			server.runOnServer(minecraft -> BuildJobs.unregister(jobs[0]));
		}
	}

	/**
	 * Keeps the terminal charged, standing in for the power cables of a real base: one of every tier
	 * draws 600 JE/t while flying, which empties the 200,000 JE buffer in about 17 seconds.
	 */
	private static void charge(TestServerContext server, BlockPos terminal) {
		server.runOnServer(minecraft -> {
			if (minecraft.overworld().getBlockEntity(terminal) instanceof DroneTerminalBlockEntity t) {
				t.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
			}
		});
	}

	/** Waits (up to {@code maxTicks}) until the terminal's client view passes {@code test} at the current game time. */
	private static void waitFor(ClientGameTestContext context, TestServerContext server, BlockPos terminal, BiPredicate<DepotView, Double> test, int maxTicks) {
		for (int i = 0; i < maxTicks; i++) {
			if (i % 20 == 0) {
				charge(server, terminal);
			}
			boolean ready = context.computeOnClient(client -> client.level != null
					&& client.level.getBlockEntity(terminal) instanceof DroneTerminalBlockEntity entity && entity.view() != null
					&& test.test(entity.view(), (double) client.level.getGameTime()));
			if (ready) {
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("Timed out waiting for the drones (" + maxTicks + " ticks)");
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in. */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/**
	 * A 21x19 base: two rows of three 5x5 pads one block apart (z 2-6 and 8-12), a 3x3 supply pickup
	 * (z 14-16) with the packager (stone bricks) beside it, the terminal beside the base
	 * (owned and powered here), one drone of every tier (21 of 24 pad slots) and a 7x3 wall to build.
	 */
	private static SimpleBuildJobs buildDepot(ServerLevel level, ServerPlayer owner, BlockPos platform, BlockPos terminalPos) {
		for (int dx = 0; dx < 21; dx++) {
			for (int dz = 0; dz < 19; dz++) {
				level.setBlock(platform.offset(dx, 0, dz), JugcraftDrones.LANDING_PLATFORM.defaultBlockState(), 3);
				int px = dx - 2, pz = dz - 2;
				if (px >= 0 && px < 17 && px % 6 < 5 && pz >= 0 && pz < 11 && pz % 6 < 5) {
					level.setBlock(platform.offset(dx, 1, dz), JugcraftDrones.LANDING_PAD.defaultBlockState(), 3);
				}
				if (dx >= 9 && dx <= 11 && dz >= 14 && dz <= 16) {
					level.setBlock(platform.offset(dx, 1, dz), JugcraftDrones.SUPPLY_PICKUP.defaultBlockState(), 3);
				}
			}
		}
		BlockPos packagerPos = platform.offset(12, 1, 15);
		level.setBlock(packagerPos, JugcraftDrones.CARGO_PACKAGER.defaultBlockState(), 3);
		((CargoPackagerBlockEntity) level.getBlockEntity(packagerPos)).setItem(0, new ItemStack(Items.STONE_BRICKS, 64));

		if (!(level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity terminal)) {
			throw new AssertionError("No terminal at " + terminalPos + "; found "
					+ level.getBlockState(terminalPos) + " (below: " + level.getBlockState(terminalPos.below()) + ")");
		}
		terminal.setOwner(owner.getUUID());
		terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
		terminal.rescanNow();
		// A depot without a Drone Tower flies no drones; this showcase allows every tier.
		terminal.allowTiersWithoutTower(9);
		for (DroneTier tier : DroneTier.values()) {
			if (!terminal.linkDrone(owner, tier)) {
				throw new AssertionError("Could not link " + tier + ": " + terminal.status().get(1).getString());
			}
		}

		SimpleBuildJobs jobs = new SimpleBuildJobs(owner.getUUID(), UseMode.PERSONAL);
		BlockPos wall = platform.offset(7, 1, 24);
		for (int dx = 0; dx < 7; dx++) {
			for (int dy = 0; dy < 3; dy++) {
				jobs.want(wall.offset(dx, dy, 0), Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		BuildJobs.register(jobs);
		return jobs;
	}
}
