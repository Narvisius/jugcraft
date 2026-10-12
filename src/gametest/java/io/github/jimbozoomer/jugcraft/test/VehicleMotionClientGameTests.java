package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import io.github.jimbozoomer.jugcraft.agriculture.Broomstick;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.airship.JugcraftAirships;
import io.github.jimbozoomer.jugcraft.airship.Zeppelin;
import io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery;
import io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer;
import io.github.jimbozoomer.jugcraft.landship.JugcraftLandships;
import io.github.jimbozoomer.jugcraft.landship.Landship;
import io.github.jimbozoomer.jugcraft.walker.ArmouredWalker;
import io.github.jimbozoomer.jugcraft.walker.DieselWalker;
import io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Real server packets -> client poses: move, reverse, pivot through the yaw seam, then settle at rest. */
public class VehicleMotionClientGameTests implements FabricClientGameTest {
	private static final AtomicReference<Scene> ACTIVE = new AtomicReference<>();
	private static boolean registered;

	private static final class Scene {
		final List<Entity> vehicles;
		int motion;

		Scene(List<Entity> vehicles) {
			this.vehicles = vehicles;
		}

		void tick() {
			for (Entity entity : vehicles) {
				// Fixed authoritative trajectory isolates packet/render behaviour from steering and terrain.
				if (motion == 1 || motion == -1) {
					entity.setPos(entity.getX(), entity.getY(), entity.getZ() + motion * 0.12);
					entity.setYRot(0);
				} else if (motion == 2) {
					entity.setYRot(net.minecraft.util.Mth.wrapDegrees(entity.getYRot() + 2.5F));
				}
				entity.setDeltaMovement(Vec3.ZERO);
			}
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!registered) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				Scene scene = ACTIVE.get();
				if (scene != null) {
					scene.tick();
				}
			});
			registered = true;
		}
		try (var singleplayer = context.worldBuilder().adjustSettings(
				creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			var server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().offset(0, 6, 0));
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, y - 1, z - 8, x + 40, y - 1, z + 35));
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @p %d %d %d 0 10".formatted(x + 10, y + 4, z - 5));
			List<Integer> ids = server.computeOnServer(minecraft -> {
				var level = minecraft.overworld();
				List<Entity> entities = List.of(new DieselWalker(JugcraftWalkers.DIESEL_WALKER, level),
						new ArmouredWalker(JugcraftWalkers.ARMOURED_WALKER, level),
						new Landship(JugcraftLandships.LANDSHIP, level),
						new SelfPropelledHowitzer(JugcraftArtillery.HOWITZER, level),
						new Zeppelin(JugcraftAirships.ZEPPELIN, level),
						new Broomstick(JugcraftAgriculture.FLYING_BROOMSTICK, level));
				for (int i = 0; i < entities.size(); i++) {
					Entity entity = entities.get(i);
					entity.snapTo(x + i * 6, y + (i > 3 ? 8 : 0), z + 4, 0, 0);
					entity.setNoGravity(true);
					level.addFreshEntity(entity);
				}
				ACTIVE.set(new Scene(entities));
				return entities.stream().map(Entity::getId).toList();
			});
			context.waitTicks(30);
			context.runOnClient(client -> {
				for (int id : ids) {
					Entity vehicle = client.level.getEntity(id);
					if (vehicle == null || !SmoothFlight.isEasing(vehicle)) {
						throw new AssertionError("Vehicle does not interpolate: " + id);
					}
				}
			});
			for (int motion : new int[] {1, -1, 2}) {
				server.runOnServer(minecraft -> {
					ACTIVE.get().motion = motion;
					if (motion == 2) {
						ACTIVE.get().vehicles.forEach(entity -> entity.setYRot(100));
					}
				});
				context.waitTicks(18);
				List<double[][]> samples = new ArrayList<>();
				for (int tick = 0; tick < 40; tick++) {
					context.waitTick();
					samples.add(sample(context, ids));
				}
				for (int i = 0; i < ids.size(); i++) {
					int stalls = 0;
					for (int t = 1; t < samples.size(); t++) {
						double[] before = samples.get(t - 1)[i], after = samples.get(t)[i];
						double delta = motion == 2 ? net.minecraft.util.Mth.wrapDegrees(after[1] - before[1]) : after[0] - before[0];
						if (Math.abs(delta) < (motion == 2 ? 0.5 : 0.03)) stalls++;
						if (Math.abs(delta) > (motion == 2 ? 5.1 : 0.25)) {
							throw new AssertionError("Vehicle jumped between packets, index=" + i + " motion=" + motion + " delta=" + delta);
						}
					}
					if (stalls > 4) throw new AssertionError("Vehicle stalled between packets: index=" + i + " stalls=" + stalls);
					if (i < 4) {
						double[] first = samples.getFirst()[i], last = samples.getLast()[i];
						double left = last[2] - first[2], right = last[3] - first[3];
						if (Math.abs(left) < 0.1) throw new AssertionError("Frozen animation on vehicle " + i);
						if (i >= 2 && (motion == 2 ? left * right >= 0 : Math.signum(left) != motion || Math.signum(right) != motion)) {
							throw new AssertionError("Tracks do not reverse/pivot correctly: " + left + ", " + right);
						}
					}
				}
				context.takeScreenshot("jugcraft_vehicle_motion_" + motion);
			}
			server.runOnServer(minecraft -> ACTIVE.get().motion = 0);
			context.waitTicks(30);
			context.runOnClient(client -> {
				for (int i = 0; i < 2; i++) {
					DieselWalker walker = (DieselWalker) client.level.getEntity(ids.get(i));
					if (walker.strideWeight(1) > 0.01F) throw new AssertionError("Walker legs did not settle at rest");
				}
			});
			System.out.println("Vehicle motion passed: six vehicles, forward/reverse/pivot, frame interpolation and idle settling.");
		} finally {
			ACTIVE.set(null);
		}
	}

	private static double[][] sample(ClientGameTestContext context, List<Integer> ids) {
		return context.computeOnClient(client -> {
			double[][] result = new double[ids.size()][4];
			for (int i = 0; i < ids.size(); i++) {
				Entity vehicle = client.level.getEntity(ids.get(i));
				if (vehicle == null) throw new AssertionError("Vehicle disappeared");
				double start = 0, end = 0, middle = 0, other = 0;
				if (vehicle instanceof DieselWalker walker) {
					start = walker.stride(0); end = walker.stride(1); middle = walker.stride(0.5F);
				} else if (vehicle instanceof Landship tank) {
					start = tank.leftTrack(0); end = tank.leftTrack(1); middle = tank.leftTrack(0.5F); other = tank.rightTrack(1);
				} else if (vehicle instanceof SelfPropelledHowitzer gun) {
					start = gun.leftTrack(0); end = gun.leftTrack(1); middle = gun.leftTrack(0.5F); other = gun.rightTrack(1);
				}
				if (Math.abs(middle - (start + end) / 2) > 0.00001) throw new AssertionError("Animation snaps between render frames");
				result[i] = new double[] {vehicle.getZ(), vehicle.getYRot(), end, other};
			}
			return result;
		});
	}
}
