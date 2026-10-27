package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.train.CargoWagon;
import io.github.jimbozoomer.jugcraft.train.JugcraftTrains;
import io.github.jimbozoomer.jugcraft.train.Locomotive;
import io.github.jimbozoomer.jugcraft.train.PassengerCar;
import io.github.jimbozoomer.jugcraft.train.TrackPoint;
import io.github.jimbozoomer.jugcraft.train.TrainCar;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the trains (docs/features/trains.md): a locomotive, a passenger coach and a cargo wagon coupled on
 * a straight line of rails over grass, photographed from the side, from the front and from behind the wagon, so the
 * bodies, the wheels on the rails and the gaps between the cars can be reviewed. CI job {@code client}.
 */
public class TrainClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
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
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			fill(server, x - 20, y - 3, z - 24, x + 24, y - 1, z + 30, "minecraft:grass_block");
			fill(server, x - 20, y, z - 24, x + 24, y + 20, z + 30, "minecraft:air");
			context.waitTicks(10);
			int coupled = server.computeOnServer(minecraft -> build(minecraft.overworld(), origin));
			if (coupled != 2) {
				throw new AssertionError("The coach and the wagon should both couple, but " + coupled + " did");
			}
			context.waitTicks(40);

			// The whole train from the side, at about the height of the cab's windows.
			shoot(context, singleplayer, x + 17, y + 2, z + 4, x + 4.5, y + 1.4, z + 4.0, "jugcraft_train_side");
			// The locomotive's nose, headlamp and hood from the front right.
			shoot(context, singleplayer, x + 9, y + 3, z + 17, x + 4.5, y + 1.5, z + 10.0, "jugcraft_train_front");
			// The wagon's end, the coach behind it and the couplings from behind the train.
			shoot(context, singleplayer, x + 1, y + 3, z - 8, x + 4.5, y + 1.2, z + 2.0, "jugcraft_train_rear");
		}
	}

	/** Lays the line and sets the train on it; returns how many cars coupled. */
	private static int build(ServerLevel level, BlockPos origin) {
		int x = origin.getX() + 4;
		for (int z = origin.getZ() - 16; z <= origin.getZ() + 20; z++) {
			level.setBlockAndUpdate(new BlockPos(x, origin.getY(), z), Blocks.RAIL.defaultBlockState());
		}
		Locomotive engine = put(level, new Locomotive(JugcraftTrains.LOCOMOTIVE, level), new BlockPos(x, origin.getY(), origin.getZ() + 8));
		PassengerCar coach = put(level, new PassengerCar(JugcraftTrains.PASSENGER_CAR, level), new BlockPos(x, origin.getY(), origin.getZ() + 4));
		CargoWagon wagon = put(level, new CargoWagon(JugcraftTrains.CARGO_WAGON, level), new BlockPos(x, origin.getY(), origin.getZ()));
		int coupled = 0;
		for (TrainCar car : new TrainCar[] {coach, wagon}) {
			if (engine.couple(level, car)) {
				coupled++;
			}
		}
		return coupled;
	}

	private static <T extends TrainCar> T put(ServerLevel level, T car, BlockPos rail) {
		TrackPoint point = TrackPoint.onRail(level, rail, new Vec3(0, 0, 1));
		if (point == null) {
			throw new AssertionError("No rail at " + rail);
		}
		car.place(point);
		level.addFreshEntity(car);
		return car;
	}

	private static void fill(TestServerContext server, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
		int layers = Math.max(1, 32768 / ((x1 - x0 + 1) * (z1 - z0 + 1)));
		for (int bottom = y0; bottom <= y1; bottom += layers) {
			server.runCommand("fill %d %d %d %d %d %d %s".formatted(x0, bottom, z0, x1, Math.min(y1, bottom + layers - 1), z1, block));
		}
	}

	/** Stands the camera on a barrier at (x, y, z) facing the point given, and takes a screenshot once the world has drawn. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, double fx, double fy,
			double fz, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.2f %.2f %.2f", x + 0.5, y, z + 0.5, fx, fy, fz));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}
}
