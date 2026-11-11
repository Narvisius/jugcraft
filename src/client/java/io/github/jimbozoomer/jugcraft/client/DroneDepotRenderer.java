package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.drone.LandingPadBlock;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a drone depot from the terminal's {@link DepotView}: every docked drone on its pad, every
 * flying drone along its path (body, arms, spinning rotors and the crate hanging from its winch), and
 * the supply pickup's lift (the hatch doors slide open, the lift raises a crate, the drone winches it
 * up, the hatch closes). Drones are not entities: each client moves them along the path the server
 * planned, so everyone sees the same flight and the server sends nothing per tick.
 */
public class DroneDepotRenderer implements BlockEntityRenderer<DroneTerminalBlockEntity, DroneDepotRenderer.State>, Sight.Always {   // culls each drone and door itself
	private final ConsoleDeskRenderer monitorRenderer;
	public DroneDepotRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
		monitorRenderer = new ConsoleDeskRenderer(context);
	}
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/drone_depot.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	/** Ticks before a drone arrives that the hatch starts opening and the lift starts rising. */
	private static final double LIFT_LEAD = 12;
	/** Ticks a hangar door takes to open or close. */
	private static final double DOOR_TICKS = 12;
	/** A drone within this many blocks of its hangar has the door open. */
	private static final double DOOR_RANGE = 10;

	/** Everything to draw, worked out once per frame. Positions are relative to the terminal block. */
	public static class State extends BlockEntityRenderState {
		final ConsoleDeskRenderer.State monitor = new ConsoleDeskRenderer.State();
		final List<float[]> drones = new ArrayList<>();
		/** One {cx, top, cz, open, rise, light} per supply pickup whose hatch is open. */
		final List<float[]> hatches = new ArrayList<>();
		/** Hangar bay doors: {x0, y0, z0, x1, y1, z1 (the opening, relative), out x, out z, open 0-1, light bits}. */
		final List<float[]> doors = new ArrayList<>();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 215 + 160;
	}

	@Override
	public void extractRenderState(DroneTerminalBlockEntity terminal, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(terminal, state, crumbling);
		state.drones.clear();
		state.hatches.clear();
		state.doors.clear();
		DepotView view = terminal.view();
		Level level = terminal.getLevel();
		state.monitor.lines.clear();
		if (level != null && cameraPos.distanceToSqr(Vec3.atCenterOf(terminal.getBlockPos())) <= 48 * 48) {
			state.monitor.facing = terminal.getBlockState().getValue(io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlock.FACING);
			ConsoleDeskRenderer.populate(state.monitor, view, level.getGameTime() + partialTick);
			state.monitor.lines.set(0, "T" + (view == null ? 0 : view.towerTier) + "  PRIMARY CONTROL");
		}
		if (view == null || level == null) {
			return;
		}
		DroneSounds.seen(terminal);
		BlockPos origin = terminal.getBlockPos();
		double now = level.getGameTime() + partialTick;
		// A ground depot's drones (no tower) are drawn to fit its 5x5 pads, in the air as well as parked.
		boolean ground = view.towerPos == null;
		for (DepotView.DockView dock : view.docks) {
			if (cameraPos.distanceToSqr(origin.getX() + dock.x(), origin.getY() + dock.y(), origin.getZ() + dock.z()) > 160 * 160) continue;
			if (!droneSeen(origin, dock.x(), dock.y(), dock.z(), 0)) continue;
			state.drones.add(drone(level, origin, dock.tier(), dock.x(), dock.y(), dock.z(), DroneModel.dockYaw(dock.tier()), -1, 0, true, 0,
					ground ? DroneModel.groundFit(dock.tier()) : 1));
		}
		// Each pickup in use (relative centre and plate top), and how open its hatch is and how far its lift has risen.
		int count = view.pickups.size();
		double[][] pickups = new double[count][];
		for (int i = 0; i < count; i++) {
			int[] p = view.pickups.get(i);
			pickups[i] = new double[] {p[0] + PlatformLayout.PICKUP_SIZE / 2.0 - origin.getX(), p[1] + LandingPadBlock.HEIGHT / 16.0 - origin.getY(),
					p[2] + PlatformLayout.PICKUP_SIZE / 2.0 - origin.getZ()};
		}
		double[] hatchOpen = new double[count];
		double[] liftRise = new double[count];
		for (DepotView.FlightView flight : view.flights) {
			FlightPath path = flight.path();
			double t = flight.ticksAt(now, view.syncTime);
			FlightPath.Pose pose = path.poseAt(t);
			boolean resting = t >= path.totalTicks() - FlightScheduler.RECHARGE_TICKS;
			if (cameraPos.distanceToSqr(origin.getX() + pose.x(), origin.getY() + pose.y(), origin.getZ() + pose.z()) <= 160 * 160
					&& droneSeen(origin, pose.x(), pose.y(), pose.z(), pose.cable())) {
				state.drones.add(drone(level, origin, flight.tier(), pose.x(), pose.y(), pose.z(), (float) pose.yaw(), (float) pose.cable(),
						(float) (now * 1.7 + flight.drone()), resting, (float) pose.level(), ground ? DroneModel.groundFit(flight.tier()) : 1));
			}
			// The pickup this flight uses: the one under its pickup point. Its hatch opens LIFT_LEAD ticks before the
			// drone arrives and closes after it leaves.
			int which = -1;
			for (int i = 0; i < count; i++) {
				if (Math.abs(pickups[i][0] - path.x(1)) < 0.6 && Math.abs(pickups[i][2] - path.z(1)) < 0.6) {
					which = i;
				}
			}
			if (which < 0) {
				continue;
			}
			double arrive = path.pickupArrive();
			double leave = path.pickupLeave();
			double open = clamp((t - (arrive - LIFT_LEAD)) / 6) * clamp((leave + 8 - t) / 6);
			hatchOpen[which] = Math.max(hatchOpen[which], open);
			if (t < arrive && t > arrive - LIFT_LEAD) {
				liftRise[which] = Math.max(liftRise[which], clamp((t - (arrive - LIFT_LEAD + 4)) / (LIFT_LEAD - 4)));
			}
		}
		// Hangar bay doors roll up while their drone is about to leave or is coming in, and roll down once it is
		// out of the way or back on standby.
		var doorFlights = new java.util.HashMap<Long, List<DepotView.FlightView>>();
		for (DepotView.FlightView flight : view.flights) {
			long key = BlockPos.asLong((int) Math.round(flight.path().x(0) * 2), 0, (int) Math.round(flight.path().z(0) * 2));
			doorFlights.computeIfAbsent(key, ignored -> new ArrayList<>()).add(flight);
		}
		for (int[] door : view.doors) {
			// Doors are cosmetic client geometry. Distant bays need no animation or light lookup.
			double cx = (door[0] + door[2] + 1) * .5, cy = door[4] + door[5] * .5, cz = (door[1] + door[3] + 1) * .5;
			if (cameraPos.distanceToSqr(cx, cy, cz) > 160 * 160) continue;
			if (!Sight.visible(door[0] - 1, door[4] - 1, door[1] - 1, door[2] + 2, door[4] + door[5] + 1, door[3] + 2)) continue;
			double open = 0;
			long doorKey = BlockPos.asLong(door[8], 0, door[9]);
			for (DepotView.FlightView flight : doorFlights.getOrDefault(doorKey, List.of())) {
				FlightPath path = flight.path();
				double dockY = path.y(0) + origin.getY();
				if (dockY < door[4] - .5 || dockY >= door[4] + door[5]) {
					continue;
				}
				double t = flight.ticksAt(now, view.syncTime);
				double home = path.totalTicks() - FlightScheduler.RECHARGE_TICKS;
				double o;
				if (view.towerLayout >= io.github.jimbozoomer.jugcraft.tower.TowerData.FIRST_INDUSTRIAL_LAYOUT) {
					double toPlane = door[6] != 0 ? Math.abs(cx - origin.getX() - path.x(0)) : Math.abs(cz - origin.getZ() - path.z(0));
					double width = door[6] != 0 ? door[3] - door[1] + 1 : door[2] - door[0] + 1;
					o = slidingDoorOpening(path, t, toPlane, width);
				} else if (t >= home) {
					o = clamp(1 - (t - home) / DOOR_TICKS);
				} else {
					FlightPath.Pose pose = path.poseAt(t);
					double dx = pose.x() - path.x(0), dy = pose.y() - path.y(0), dz = pose.z() - path.z(0);
					double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
					o = Math.min(clamp(t / DOOR_TICKS * 2), clamp((DOOR_RANGE - dist) / 3));
				}
				open = Math.max(open, o);
			}
			int light = LightCoordsUtil.getLightCoords(level, new BlockPos(door[0] + door[6], door[4], door[1] + door[7]));
			state.doors.add(new float[] {door[0] - origin.getX(), door[4] - origin.getY(), door[1] - origin.getZ(),
					door[2] + 1 - origin.getX(), door[4] + door[5] - origin.getY(), door[3] + 1 - origin.getZ(), door[6], door[7],
					(float) open, Float.intBitsToFloat(light), view.towerLayout >= io.github.jimbozoomer.jugcraft.tower.TowerData.TRIPILLAR_LAYOUT ? 2
							: view.towerLayout >= io.github.jimbozoomer.jugcraft.tower.TowerData.FIRST_INDUSTRIAL_LAYOUT ? 1 : 0,
					cameraPos.distanceToSqr(cx, cy, cz) <= 48 * 48 ? 1 : 0,
					door.length >= 13 ? door[10] : -1, door.length >= 13 ? door[11] : 0, door.length >= 13 ? door[12] : 0});
		}
		for (int i = 0; i < count; i++) {
			if (hatchOpen[i] > 0) {
				double[] p = pickups[i];
				int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(p[0] + origin.getX(), p[1] + origin.getY() + 0.5, p[2] + origin.getZ()));
				state.hatches.add(new float[] {(float) p[0], (float) p[1], (float) p[2], (float) hatchOpen[i], (float) liftRise[i], Float.intBitsToFloat(light)});
			}
		}
	}

	/** How far round its middle a drone reaches (blocks; the biggest, with its rotors). */
	static final double DRONE_REACH = 3;

	/** Whether a drone at (x, y, z) from the terminal, its cargo hanging {@code cable} below it, could be seen ({@link Sight}). */
	private static boolean droneSeen(BlockPos origin, double x, double y, double z, double cable) {
		double wx = origin.getX() + x, wy = origin.getY() + y, wz = origin.getZ() + z;
		return Sight.visible(wx - DRONE_REACH, wy - DRONE_REACH - Math.max(0, cable), wz - DRONE_REACH, wx + DRONE_REACH, wy + DRONE_REACH, wz + DRONE_REACH);
	}

	private static float[] drone(Level level, BlockPos origin, int tier, double x, double y, double z, float yaw, float cable, float spin,
			boolean resting, float cruising, float scale) {
		int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(x + origin.getX(), y + origin.getY() + 0.3, z + origin.getZ()));
		return new float[] {(float) x, (float) y, (float) z, yaw, tier, cable, resting ? 0 : spin, Float.intBitsToFloat(light), cruising, scale};
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/** Opens before the drone's clearance envelope reaches the leaf, including creative-speed flights. */
	public static double slidingDoorOpening(FlightPath path, double ticks, double homeToPlane, double width) {
		double home = path.totalTicks() - FlightScheduler.RECHARGE_TICKS;
		if (ticks >= home) return clamp(1 - (ticks - home) / DOOR_TICKS);
		FlightPath.Pose pose = path.poseAt(ticks);
		double dx = pose.x() - path.x(0), dy = pose.y() - path.y(0), dz = pose.z() - path.z(0);
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double clearance = Math.min(2.6, width * .4) + .25;
		double openingTicks = Math.min(DOOR_TICKS * .5, Math.max(.05, (homeToPlane - clearance) * 20 / path.speed()));
		return Math.min(clamp(ticks / openingTicks), clamp((DOOR_RANGE - dist) / 3));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		monitorRenderer.submit(state.monitor, poseStack, collector, camera);
		if (state.drones.isEmpty() && state.hatches.isEmpty() && state.doors.isEmpty()) {
			return;
		}
		List<float[]> drones = List.copyOf(state.drones);
		List<float[]> hatches = List.copyOf(state.hatches);
		List<float[]> doors = List.copyOf(state.doors);
		if (!drones.isEmpty()) {
			Retained.mark(collector);                    // drones at rest are kept on the card (Retained)
			collector.submitCustomGeometry(poseStack, DroneMesh.RENDER_TYPE, (pose, consumer) -> {
				Geometry g = new Geometry(pose, consumer, DroneMesh.RENDER_TYPE);
				for (float[] d : drones) {
					DroneModel.draw(g, d[0], d[1], d[2], d[3], (int) d[4], d[5], d[6], Float.floatToRawIntBits(d[7]), d[8], d[9]);
				}
			});
		}
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
			Geometry g = new Geometry(pose, consumer);
			for (float[] hatch : hatches) {
				drawHatch(g, hatch[0], hatch[1], hatch[2], hatch[3], hatch[4], Float.floatToRawIntBits(hatch[5]));
			}
		});
		if (!doors.isEmpty()) {
			List<float[]> industrial = doors.stream().filter(door -> door.length > 10 && door[10] == 1).toList();
			List<float[]> iris = doors.stream().filter(door -> door.length > 10 && door[10] == 2).toList();
			List<float[]> legacy = doors.stream().filter(door -> door.length <= 10 || door[10] == 0).toList();
			if (!iris.isEmpty()) {
				double now = net.minecraft.client.Minecraft.getInstance().level == null ? 0 : net.minecraft.client.Minecraft.getInstance().level.getGameTime();
				collector.submitCustomGeometry(poseStack, DroneMesh.RENDER_TYPE, (pose, consumer) -> {
					Geometry g = new Geometry(pose, consumer);
					for (float[] door : iris) {
						drawIrisDoor(g, door);
						drawBayLamp(g, door, now);
					}
				});
				List<float[]> tagged = iris.stream().filter(door -> door.length > 14 && door[12] >= 0 && door[11] == 1).toList();
				if (!tagged.isEmpty()) {
					collector.submitCustomGeometry(poseStack, BAY_TAGS, (pose, consumer) -> {
						Geometry g = new Geometry(pose, consumer);
						for (float[] door : tagged) drawBayTag(g, door);
					});
				}
			}
			collector.submitCustomGeometry(poseStack, DOOR_SLATS, (pose, consumer) -> {
				Geometry g = new Geometry(pose, consumer);
				for (float[] door : legacy) {
					drawDoor(g, door, false);
				}
			});
			collector.submitCustomGeometry(poseStack, DOOR_RAIL, (pose, consumer) -> {
				Geometry g = new Geometry(pose, consumer);
				for (float[] door : legacy) {
					drawDoor(g, door, true);
				}
			});
			collector.submitCustomGeometry(poseStack, INDUSTRIAL_DOOR_LEAF, (pose, consumer) -> {
				Geometry g = new Geometry(pose, consumer);
				for (float[] door : industrial) drawSlidingDoor(g, door, false);
			});
			collector.submitCustomGeometry(poseStack, INDUSTRIAL_DOOR_HARDWARE, (pose, consumer) -> {
				Geometry g = new Geometry(pose, consumer);
				for (float[] door : industrial) drawSlidingDoor(g, door, true);
			});
		}
	}

	/** The Tri-Pillar's bay number tags (tools/tripillar_fixtures.py): engraved aluminium tags, A01.. per pillar. */
	private static final RenderType BAY_TAGS = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tripillar_bay_tags.png"));
	private static final int TAG_COLS = 8, TAG_ROWS = 18;
	private static final float TAG_W = .8f, TAG_H = .3f;

	/**
	 * Where a bay's jamb fittings go, worked out from its door ({@code d}: the opening, relative, and which way is out):
	 * the centre of the jamb on the right as you look into the bay from outside, on the pillar's outer face.
	 * Returns {x, z (jamb centre on the face), normal x, normal z, along x, along z (the face's rightward direction)}.
	 */
	private static float[] rightJamb(float[] d) {
		boolean alongZ = d[6] != 0;
		int out = (int) (alongZ ? d[6] : d[7]);
		if (alongZ) {
			float x = out > 0 ? d[3] : d[0];
			float z = out > 0 ? d[2] - .5f : d[5] + .5f;
			return new float[] {x, z, out, 0, 0, -out};
		}
		float z = out > 0 ? d[5] : d[2];
		float x = out > 0 ? d[3] + .5f : d[0] - .5f;
		return new float[] {x, z, 0, out, out, 0};
	}

	/**
	 * A bay's status lamp, high on its right jamb under the lintel: a dark bezel and a lens lit green while its drone is
	 * home, amber (blinking) while it is out, dim red when the bay has no drone.
	 */
	private static void drawBayLamp(Geometry g, float[] d, double now) {
		if (d.length <= 14 || d[12] < 0) {
			return;
		}
		float[] j = rightJamb(d);
		int status = (int) d[14];
		boolean blink = status == 2 && (now % 20) < 10;
		int lens = status == 1 ? 0xFF46E05A : status == 2 ? (blink ? 0xFFFFB040 : 0xFF8A5A20) : 0xFF6A1E18;
		int bright = status == 0 ? Float.floatToRawIntBits(d[9]) : LightCoordsUtil.FULL_BRIGHT;
		float y = d[4] - .5f;
		g.reset();
		g.translate(j[0], y, j[1]);
		// bezel 0.3 x 0.3 standing 0.04 proud of the face, the lens 0.18 x 0.18 a little further out
		float nx = j[2], nz = j[3], ax = j[4], az = j[5];
		box(g, nx, nz, ax, az, .15f, .15f, 0, .04f, 0xFF1E2124, Float.floatToRawIntBits(d[9]));
		box(g, nx, nz, ax, az, .09f, .09f, .04f, .07f, lens, bright);
		g.reset();
	}

	/** A box on a face: half-width {@code hw} along the face, half-height {@code hh}, from {@code out0} to {@code out1} out of it. */
	private static void box(Geometry g, float nx, float nz, float ax, float az, float hw, float hh, float out0, float out1, int argb, int light) {
		float x0 = Math.min(nx * out0 - ax * hw, nx * out1 + ax * hw), x1 = Math.max(nx * out0 - ax * hw, nx * out1 + ax * hw);
		float z0 = Math.min(nz * out0 - az * hw, nz * out1 + az * hw), z1 = Math.max(nz * out0 - az * hw, nz * out1 + az * hw);
		flatBox(g, x0, -hh, z0, x1, hh, z1, argb, light);
	}

	/** A bay's number tag (A01...) on its right jamb, at a person's eye height above the bay floor, facing out. */
	private static void drawBayTag(Geometry g, float[] d) {
		int pillar = (int) d[12], number = (int) d[13];
		if (number < 1 || number > TAG_COLS * TAG_ROWS / 3) {
			return;
		}
		float[] j = rightJamb(d);
		float y = d[1] + Math.min(1.6f, (d[4] - d[1]) * .5f);
		int col = (number - 1) % TAG_COLS, row = pillar * (TAG_ROWS / 3) + (number - 1) / TAG_COLS;
		float u0 = (float) col / TAG_COLS, u1 = (float) (col + 1) / TAG_COLS, v0 = (float) row / TAG_ROWS, v1 = (float) (row + 1) / TAG_ROWS;
		float nx = j[2], nz = j[3], ax = j[4], az = j[5];
		float px = j[0] + nx * .015f, pz = j[1] + nz * .015f;
		int light = Float.floatToRawIntBits(d[9]);
		g.reset();
		// left-bottom, right-bottom, right-top, left-top as seen from outside (the face's normal towards the viewer)
		g.meshVertex(px - ax * TAG_W / 2, y - TAG_H / 2, pz - az * TAG_W / 2, u0, v1, nx, 0, nz, 0xFFFFFFFF, light);
		g.meshVertex(px + ax * TAG_W / 2, y - TAG_H / 2, pz + az * TAG_W / 2, u1, v1, nx, 0, nz, 0xFFFFFFFF, light);
		g.meshVertex(px + ax * TAG_W / 2, y + TAG_H / 2, pz + az * TAG_W / 2, u1, v0, nx, 0, nz, 0xFFFFFFFF, light);
		g.meshVertex(px - ax * TAG_W / 2, y + TAG_H / 2, pz - az * TAG_W / 2, u0, v0, nx, 0, nz, 0xFFFFFFFF, light);
		g.reset();
	}

	/** The roll-up doors' own textures (tools/tower_art.py), in the tower's steel: slats, and the hazard bottom rail. */
	private static final RenderType DOOR_SLATS = RenderTypes.entityCutout(Jugcraft.id("textures/entity/hangar_door_slats.png"));
	private static final RenderType DOOR_RAIL = RenderTypes.entityCutout(Jugcraft.id("textures/entity/hangar_door_rail.png"));
	private static final RenderType INDUSTRIAL_DOOR_LEAF = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tower_industrial_door_leaf.png"));
	private static final RenderType INDUSTRIAL_DOOR_HARDWARE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/tower_industrial_door_hardware.png"));

	/**
	 * A hangar's roll-up bay door in the outer face of its opening: steel slats (one block of slat texture per block
	 * of door, fixed to the door so they ride up with it) over a hazard-striped bottom rail, rolled into a drum under
	 * the lintel as it opens. Drawn in two passes, slats then rail ({@code rail}), as they use two textures.
	 */
	private static void drawDoor(Geometry g, float[] d, boolean rail) {
		float x0 = d[0], y0 = d[1], z0 = d[2], x1 = d[3], y1 = d[4], z1 = d[5];
		int ox = (int) d[6], oz = (int) d[7];
		float open = d[8];
		if (d.length > 10 && d[10] == 1) {
			drawSlidingDoor(g, d, rail);
			return;
		}
		g.reset();
		g.light = Float.floatToRawIntBits(d[9]);
		g.color = 0xFFFFFFFF;
		float thick = 0.12f;
		boolean alongZ = ox != 0;            // the door's plane is x = const and it spans z
		float plane = alongZ ? (ox > 0 ? x1 - thick : x0) : (oz > 0 ? z1 - thick : z0);
		float a0 = alongZ ? z0 : x0, a1 = alongZ ? z1 : x1;
		float height = y1 - y0;
		float bottom = y0 + height * open;
		float railH = 0.14f;
		if (rail) {
			if (y1 - bottom > 0.01f) {
				panel(g, alongZ, plane - 0.02f, plane + thick + 0.02f, a0, a1, bottom, bottom + railH, 0, 1);
			}
			return;
		}
		// Slats: block-high strips counted up from the door's bottom edge, the top one cut off at the lintel.
		for (float yA = bottom + railH; yA < y1 - 0.001f; yA += 1) {
			float yB = Math.min(yA + 1, y1);
			panel(g, alongZ, plane, plane + thick, a0, a1, yA, yB, 0, yB - yA);
		}
		// The rolled-up drum under the lintel, on the inside; it grows as the door opens.
		float drum = 0.08f + 0.22f * open;
		float in0 = alongZ ? (ox > 0 ? plane - drum + thick : plane) : (oz > 0 ? plane - drum + thick : plane);
		panel(g, alongZ, in0, in0 + drum, a0, a1, y1 - drum, y1, 0, drum);
	}

	/**
	 * The Tri-Pillar tower's iris door: four triangular petals meet at the middle of the bay opening. Each petal is
	 * hinged on its own edge and swings in flat against the bay's ceiling, floor or side wall as the door opens
	 * ({@code d[8]} 0 to 1). Closed, amber seams light the X between the petals and a ring round the hub.
	 */
	private static void drawIrisDoor(Geometry g, float[] d) {
		g.reset();
		int light = Float.floatToRawIntBits(d[9]);
		boolean alongZ = d[6] != 0;
		int outward = (int) (alongZ ? d[6] : d[7]);
		float a0 = alongZ ? d[2] : d[0], a1 = alongZ ? d[5] : d[3];
		float width = a1 - a0, height = d[4] - d[1];
		float plane = alongZ ? (outward > 0 ? d[3] - .2f : d[0] + .2f) : (outward > 0 ? d[5] - .2f : d[2] + .2f);
		float mid = (a0 + a1) * .5f, cy = (d[1] + d[4]) * .5f;
		if (alongZ) {
			g.translate(plane, cy, mid).yaw(outward > 0 ? Math.PI / 2 : -Math.PI / 2);
		} else {
			g.translate(mid, cy, plane).yaw(outward > 0 ? 0 : Math.PI);
		}
		float w = width * .5f, h = height * .5f;
		double angle = Math.max(0, Math.min(1, d[8])) * Math.PI / 2;
		int leaf = 0xFF3A4046, edge = 0xFF23272B;
		// top, bottom: hinged on the horizontal edges; left, right: on the vertical edges
		petal(g, 0, h, 1, 0, 0, angle, -w, h, w, h, leaf, edge, light);
		petal(g, 0, -h, 1, 0, 0, -angle, -w, -h, w, -h, leaf, edge, light);
		petal(g, -w, 0, 0, 1, 0, angle, -w, -h, -w, h, leaf, edge, light);
		petal(g, w, 0, 0, 1, 0, -angle, w, -h, w, h, leaf, edge, light);
		if (d[8] < .05f) {
			int amber = 0xFFFFAA3A, bright = LightCoordsUtil.FULL_BRIGHT;
			float len = (float) Math.hypot(w, h), tilt = (float) Math.atan2(h, w);
			for (float sign : new float[] {1, -1}) {
				g.push();
				g.translate(0, 0, .02f).rotate(sign * tilt, 0, 0, 1);
				flatBox(g, -len, -.025f, 0, len, .025f, .03f, amber, bright);
				g.pop();
			}
			float r = Math.min(w, h) * .2f;
			for (int k = 0; k < 16; k++) {
				g.push();
				g.translate(0, 0, .02f).rotate(k * Math.PI / 8, 0, 0, 1);
				flatBox(g, r - .03f, -r * .2f, 0, r + .03f, r * .2f, .03f, amber, bright);
				g.pop();
			}
		}
		g.reset();
	}

	/**
	 * One iris petal: the triangle from the hinge edge (e0, e1) to the opening's centre, 0.12 thick behind the
	 * plane, turned {@code angle} about the hinge axis (ax, ay, az) through (hx, hy).
	 */
	private static void petal(Geometry g, float hx, float hy, float ax, float ay, float az, double angle, float e0x, float e0y, float e1x,
			float e1y, int leaf, int edge, int light) {
		g.push();
		g.translate(hx, hy, 0).rotate(angle, ax, ay, az).translate(-hx, -hy, 0);
		float t = -.12f;
		float u = DroneMesh.WHITE_U, v = DroneMesh.WHITE_V;
		// front (outward) and back faces
		quadTri(g, e0x, e0y, 0, e1x, e1y, 0, 0, 0, 0, 0, 0, 1, leaf, light, u, v);
		quadTri(g, e1x, e1y, t, e0x, e0y, t, 0, 0, t, 0, 0, -1, leaf, light, u, v);
		// the three edges
		float[][] p = {{e0x, e0y}, {e1x, e1y}, {0, 0}};
		for (int i = 0; i < 3; i++) {
			float[] a = p[i], b = p[(i + 1) % 3];
			float nx = b[1] - a[1], ny = -(b[0] - a[0]);
			float l = (float) Math.hypot(nx, ny);
			if (l < 1e-4f) continue;
			nx /= l;
			ny /= l;
			// edge normal must point away from the triangle's centroid
			float cx = (e0x + e1x) / 3, cyy = (e0y + e1y) / 3;
			if (nx * ((a[0] + b[0]) * .5f - cx) + ny * ((a[1] + b[1]) * .5f - cyy) < 0) {
				nx = -nx;
				ny = -ny;
			}
			orientedQuad(g, new float[] {a[0], a[1], 0, a[0], a[1], t, b[0], b[1], t, b[0], b[1], 0}, nx, ny, 0, edge, light, u, v);
		}
		g.pop();
	}

	/** A triangle sent as a quad (its last corner twice), drawn on the side its normal faces. */
	private static void quadTri(Geometry g, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
			float nx, float ny, float nz, int argb, int light, float u, float v) {
		orientedQuad(g, new float[] {ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz}, nx, ny, nz, argb, light, u, v);
	}

	/** Four corners, wound so the face looks along (nx, ny, nz) (counter-clockwise seen from that side). */
	private static void orientedQuad(Geometry g, float[] p, float nx, float ny, float nz, int argb, int light, float u, float v) {
		float ux = p[3] - p[0], uy = p[4] - p[1], uz = p[5] - p[2], vx = p[6] - p[0], vy = p[7] - p[1], vz = p[8] - p[2];
		float cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
		boolean flip = cx * nx + cy * ny + cz * nz < 0;
		for (int k = 0; k < 4; k++) {
			int i = flip ? 3 - k : k;
			g.meshVertex(p[i * 3], p[i * 3 + 1], p[i * 3 + 2], u, v, nx, ny, nz, argb, light);
		}
	}

	/** A box in vertex colour (the fleet atlas's white corner). */
	private static void flatBox(Geometry g, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
		float u = DroneMesh.WHITE_U, v = DroneMesh.WHITE_V;
		// the six faces straight into the buffer (no arrays per box: Justin's JFR, per-frame garbage)
		g.meshVertex(x0, y1, z0, u, v, 0, 1, 0, argb, light); g.meshVertex(x0, y1, z1, u, v, 0, 1, 0, argb, light);
		g.meshVertex(x1, y1, z1, u, v, 0, 1, 0, argb, light); g.meshVertex(x1, y1, z0, u, v, 0, 1, 0, argb, light);
		g.meshVertex(x0, y0, z1, u, v, 0, -1, 0, argb, light); g.meshVertex(x0, y0, z0, u, v, 0, -1, 0, argb, light);
		g.meshVertex(x1, y0, z0, u, v, 0, -1, 0, argb, light); g.meshVertex(x1, y0, z1, u, v, 0, -1, 0, argb, light);
		g.meshVertex(x0, y0, z0, u, v, 0, 0, -1, argb, light); g.meshVertex(x0, y1, z0, u, v, 0, 0, -1, argb, light);
		g.meshVertex(x1, y1, z0, u, v, 0, 0, -1, argb, light); g.meshVertex(x1, y0, z0, u, v, 0, 0, -1, argb, light);
		g.meshVertex(x1, y0, z1, u, v, 0, 0, 1, argb, light); g.meshVertex(x1, y1, z1, u, v, 0, 0, 1, argb, light);
		g.meshVertex(x0, y1, z1, u, v, 0, 0, 1, argb, light); g.meshVertex(x0, y0, z1, u, v, 0, 0, 1, argb, light);
		g.meshVertex(x0, y0, z1, u, v, -1, 0, 0, argb, light); g.meshVertex(x0, y1, z1, u, v, -1, 0, 0, argb, light);
		g.meshVertex(x0, y1, z0, u, v, -1, 0, 0, argb, light); g.meshVertex(x0, y0, z0, u, v, -1, 0, 0, argb, light);
		g.meshVertex(x1, y0, z0, u, v, 1, 0, 0, argb, light); g.meshVertex(x1, y1, z0, u, v, 1, 0, 0, argb, light);
		g.meshVertex(x1, y1, z1, u, v, 1, 0, 0, argb, light); g.meshVertex(x1, y0, z1, u, v, 1, 0, 0, argb, light);
	}

	/** Opposed banks of telescoping panels fit within one-metre pockets; no server door state exists. */
	private static void drawSlidingDoor(Geometry g, float[] d, boolean rail) {
		g.reset();
		g.light = Float.floatToRawIntBits(d[9]);
		g.color = 0xFFFFFFFF;
		boolean alongZ = d[6] != 0;
		float a0 = alongZ ? d[2] : d[0], a1 = alongZ ? d[5] : d[3];
		int outward = (int) (alongZ ? d[6] : d[7]);
		// Four tracks, where needed, fit within the recessed guide and cassette depth.
		float plane = alongZ ? (outward > 0 ? d[3] - .47f : d[0] + .47f) : (outward > 0 ? d[5] - .47f : d[2] + .47f);
		float half = (a1 - a0) * .5f;
		int panels = (int) Math.ceil(half);
		float width = half / panels;
		boolean detailed = d.length > 11 && d[11] == 1;
		// Successive leaves retract faster onto parallel tracks, stacking completely behind each jamb.
		for (int side = 0; side < 2; side++) {
			for (int segment = 0; segment < panels; segment++) {
			float leafOrigin = side == 0 ? a0 + segment * width - (segment + 1) * width * d[8]
					: a1 - (segment + 1) * width + (segment + 1) * width * d[8];
			float lo = Math.max(a0, leafOrigin), hi = Math.min(a1, leafOrigin + width);
			float remaining = hi - lo;
			if (remaining < .005f) continue;
			float track = plane - outward * segment * .07f;
			float p0 = outward > 0 ? track - .055f : track, p1 = outward > 0 ? track : track + .055f;
			if (rail) {
				doorBox(g, alongZ, p0 - .012f, p1 + .012f, lo, hi, d[1], d[1] + .10f);
				doorBox(g, alongZ, p0 - .012f, p1 + .012f, lo, hi, d[4] - .08f, d[4]);
				doorBox(g, alongZ, p0 - .012f, p1 + .012f, lo, Math.min(hi, lo + .035f), d[1] + .10f, d[4] - .08f);
				doorBox(g, alongZ, p0 - .012f, p1 + .012f, Math.max(lo, hi - .035f), hi, d[1] + .10f, d[4] - .08f);
				// Pressed stiffening rails, captive fasteners and a real stand-off service handle ride with each leaf.
				for (float y = d[1] + .7f; detailed && y < d[4] - .3f; y += 1.3f) {
					doorBox(g, alongZ, p0 - .018f, p1 + .018f, lo, hi, y, y + .045f);
					if (remaining > .16f) {
						for (float bolt : new float[] {lo + .035f, hi - .055f}) {
							doorBox(g, alongZ, p0 - .023f, p1 + .023f, bolt, bolt + .02f, y + .01f, y + .03f);
						}
					}
				}
				if (detailed && segment == 0 && remaining > .38f) {
					float handle = side == 0 ? hi - .29f : lo + .20f;
					float y = d[1] + Math.min(1.05f, (d[4] - d[1]) * .42f);
					float face = outward > 0 ? p1 : p0;
					float h0 = outward > 0 ? face : face - .08f, h1 = outward > 0 ? face + .08f : face;
					for (float mount : new float[] {y, y + .29f}) {
						doorBox(g, alongZ, h0, h1, handle, handle + .07f, mount, mount + .045f);
					}
					doorBox(g, alongZ, outward > 0 ? h1 - .025f : h0, outward > 0 ? h1 : h0 + .025f,
						handle + .015f, handle + .055f, y, y + .335f);
				}
			} else {
				industrialSheet(g, alongZ, p0, p1, lo, hi, d[1], d[4], leafOrigin);
			}
			}
		}
	}

	private static final float[] HARDWARE_UV = {0, 0, 1, 1};

	private static void doorBox(Geometry g, boolean alongZ, float p0, float p1, float a0, float a1, float y0, float y1) {
		if (a1 <= a0 || y1 <= y0) return;
		if (alongZ) g.box(p0, y0, a0, p1, y1, a1, HARDWARE_UV);
		else g.box(a0, y0, p0, a1, y1, p1, HARDWARE_UV);
	}

	/** Two-metre albedo sheets move with the leaves, retaining their scale while clipping behind the jamb. */
	private static void industrialSheet(Geometry g, boolean alongZ, float p0, float p1, float a0, float a1,
			float y0, float y1, float origin) {
		for (float start = origin; start < a1 - .001f; start += 2) {
			float s = Math.max(a0, start), e = Math.min(a1, start + 2);
			if (e <= s) continue;
			float u0 = (s - start) * .5f, u1 = (e - start) * .5f;
			for (float y = y0; y < y1 - .001f; y += 2) {
				float top = Math.min(y1, y + 2), vt = 1 - (top - y) * .5f;
				float[] uv = {u0, 1, u1, vt}, reverse = {u1, 1, u0, vt};
				if (alongZ) {
					g.quad(p0, y, e, p0, top, e, p0, top, s, p0, y, s, -1, 0, 0, reverse);
					g.quad(p1, y, s, p1, top, s, p1, top, e, p1, y, e, 1, 0, 0, uv);
				} else {
					g.quad(s, y, p0, s, top, p0, e, top, p0, e, y, p0, 0, 0, -1, uv);
					g.quad(e, y, p1, e, top, p1, s, top, p1, s, y, p1, 0, 0, 1, reverse);
				}
			}
		}
	}

	/**
	 * A slab of door between {@code p0..p1} across its plane and {@code a0..a1} along it, from {@code yA} to
	 * {@code yB}: both broad faces textured with one block of texture per block along the door (cut into whole
	 * blocks so the texture never needs to repeat), {@code vBottom..vTop} giving how much of the texture's height
	 * shows (from its bottom up), plus its bottom edge. Both windings, so it shows from inside and out.
	 */
	private static void panel(Geometry g, boolean alongZ, float p0, float p1, float a0, float a1, float yA, float yB, float vBottom, float vTop) {
		float vb = 1 - vBottom, vt = 1 - Math.min(1, vTop);
		for (float s = a0; s < a1 - 0.001f; s += 1) {
			float e = Math.min(s + 1, a1);
			float u1 = e - s;
			for (float p : new float[] {p0, p1}) {
				float n = p == p0 ? -1 : 1;
				if (alongZ) {
					g.face(p, yA, s, p, yB, s, p, yB, e, p, yA, e, n, 0, 0, 0, vb, 0, vt, u1, vt, u1, vb);
				} else {
					g.face(s, yA, p, s, yB, p, e, yB, p, e, yA, p, 0, 0, n, 0, vb, 0, vt, u1, vt, u1, vb);
				}
			}
			if (alongZ) {
				g.face(p0, yA, s, p1, yA, s, p1, yA, e, p0, yA, e, 0, -1, 0, 0, 0.98f, 1, 0.98f, 1, 1, 0, 1);
			} else {
				g.face(s, yA, p0, s, yA, p1, e, yA, p1, e, yA, p0, 0, -1, 0, 0, 0.98f, 1, 0.98f, 1, 1, 0, 1);
			}
		}
	}

	/** The supply pickup lift: dark shaft, doors sliding apart, lift platform with a crate on it. */
	private static void drawHatch(Geometry g, float cx, float top, float cz, float open, float rise, int light) {
		float half = 0.4f;
		float depth = 0.9f;
		g.reset();
		g.light = light;
		g.color = 0xFFFFFFFF;
		// Shaft: floor and four walls, just inside the opening.
		g.box(cx - half, top - depth, cz - half, cx + half, top - depth + 0.02f, cz + half, DroneModel.SHAFT);
		g.box(cx - half, top - depth, cz - half, cx - half + 0.02f, top + 0.001f, cz + half, DroneModel.SHAFT);
		g.box(cx + half - 0.02f, top - depth, cz - half, cx + half, top + 0.001f, cz + half, DroneModel.SHAFT);
		g.box(cx - half, top - depth, cz - half, cx + half, top + 0.001f, cz - half + 0.02f, DroneModel.SHAFT);
		g.box(cx - half, top - depth, cz + half - 0.02f, cx + half, top + 0.001f, cz + half, DroneModel.SHAFT);
		// Doors slide apart along x.
		float slide = open * half;
		g.box(cx - half - slide, top + 0.002f, cz - half, cx - slide, top + 0.05f, cz + half, DroneModel.HATCH);
		g.box(cx + slide, top + 0.002f, cz - half, cx + half + slide, top + 0.05f, cz + half, DroneModel.HATCH);
		// Lift platform, and a crate on it while it rises.
		float liftY = top - depth + 0.03f + rise * (depth - 0.06f);
		g.box(cx - 0.34f, liftY, cz - 0.34f, cx + 0.34f, liftY + 0.04f, cz + 0.34f, DroneModel.LIFT);
		if (rise > 0) {
			DroneModel.crate(g, cx, liftY + 0.04f, cz, DroneModel.CRATE_SIZE, 0, DroneModel.WHITE);   // the same case the drones carry
			g.color = 0xFFFFFFFF;
		}
	}

	/**
	 * Emits textured, lit boxes and quads into one vertex consumer through a small transform stack
	 * (translate, rotate, scale), so drone parts can be placed, turned and tilted like model parts.
	 *
	 * <p>Each vertex is put in place in one step (Justin's frame rate, 6 November 2026: "Build pieces once"): the stack's
	 * frame and the pose are joined once each time the frame changes, not for every vertex, and the vertex goes to the
	 * buffer whole, without the game's per-vertex helpers that make two new vectors for every corner drawn (a tower floor
	 * draws a few hundred thousand a frame).
	 */
	static final class Geometry {
		private final PoseStack.Pose pose;
		private final VertexConsumer consumer;
		/** Saved frames, kept and refilled rather than made anew on each push (Justin's JFR: per-frame garbage). */
		private org.joml.Matrix4f[] stack = new org.joml.Matrix4f[0];
		private int depth;
		private final org.joml.Matrix4f matrix = new org.joml.Matrix4f();
		private final org.joml.Vector3f position = new org.joml.Vector3f();
		private final org.joml.Vector3f normal = new org.joml.Vector3f();
		/** The pose and the stack's frame joined (positions), and their turn (normals); worked out again when either changes. */
		private final org.joml.Matrix4f full = new org.joml.Matrix4f();
		private final org.joml.Matrix3f turn = new org.joml.Matrix3f();
		private final org.joml.Matrix3f scratch = new org.joml.Matrix3f();
		private boolean stale = true;
		int light = LightCoordsUtil.FULL_BRIGHT;
		int color = 0xFFFFFFFF;
		/** The render type this geometry's corners go to, when shapes that stay put may be kept on the card ({@link Retained}). */
		net.minecraft.client.renderer.rendertype.RenderType retain;

		Geometry(PoseStack.Pose pose, VertexConsumer consumer) {
			this.pose = pose;
			this.consumer = consumer;
		}

		/** As a geometry whose shapes that stay put are kept on the card, in {@code type} ({@link Retained}). */
		Geometry(PoseStack.Pose pose, VertexConsumer consumer, net.minecraft.client.renderer.rendertype.RenderType type) {
			this(pose, consumer);
			this.retain = type;
		}

		/**
		 * The shape {@code key} kept on the card where the current frame puts it, instead of its corners written now:
		 * {@code builder} gives its corners (once, turned as the frame turns them, lit by {@code packedLight}). False when it
		 * can't be (no render type to keep it in, or kept shapes are off): then the caller writes the corners.
		 */
		boolean retained(Object key, int packedLight, Retained.Builder builder, float reach) {
			if (retain == null) {
				return false;
			}
			if (stale) {
				join();
			}
			if (!Retained.queue(retain, key, packedLight, full, builder)) {
				return false;
			}
			if (consumer instanceof SightCollector.SightConsumer sight) {
				sight.learn(full.m30(), full.m31(), full.m32(), reach);   // its extent, for a block entity being measured
			}
			return true;
		}

		void push() {
			if (depth == stack.length) {
				stack = java.util.Arrays.copyOf(stack, Math.max(4, depth * 2));
			}
			if (stack[depth] == null) {
				stack[depth] = new org.joml.Matrix4f();
			}
			stack[depth++].set(matrix);
		}

		void pop() {
			if (depth == 0) {
				throw new java.util.NoSuchElementException();
			}
			matrix.set(stack[--depth]);
			stale = true;
		}

		void reset() {
			depth = 0;
			matrix.identity();
			stale = true;
		}

		Geometry translate(double x, double y, double z) {
			matrix.translate((float) x, (float) y, (float) z);
			stale = true;
			return this;
		}

		/** Turns about the vertical axis; 0 faces +z (south), positive turns towards +x. */
		Geometry yaw(double radians) {
			matrix.rotateY((float) radians);
			stale = true;
			return this;
		}

		/** Tilts about the x axis; positive tips +y towards +z (forward). */
		Geometry pitch(double radians) {
			matrix.rotateX((float) radians);
			stale = true;
			return this;
		}

		Geometry roll(double radians) {
			matrix.rotateZ((float) radians);
			stale = true;
			return this;
		}

		/** Turns {@code radians} about the axis (ax, ay, az) through the current origin. */
		Geometry rotate(double radians, double ax, double ay, double az) {
			matrix.rotate((float) radians, (float) ax, (float) ay, (float) az);
			stale = true;
			return this;
		}

		/**
		 * How far the current frame's origin is from the camera (blocks): a block entity's pose is the camera's, so the
		 * joined frame's translation is where that origin is from the eye. For the pieces' detail by distance.
		 */
		float cameraDistance() {
			if (stale) {
				join();
			}
			return (float) Math.sqrt(full.m30() * full.m30() + full.m31() * full.m31() + full.m32() * full.m32());
		}

		private void join() {
			full.set(pose.pose()).mul(matrix);
			turn.set(pose.normal()).mul(matrix.get3x3(scratch));
			stale = false;
		}

		/** One vertex with its own colour and light (the drone meshes). */
		void meshVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int argb, int packedLight) {
			put(x, y, z, u, v, nx, ny, nz, argb, packedLight);
		}

		/** A vertex into the buffer, placed by the pose and the stack's frame in one step. */
		private void put(float x, float y, float z, float u, float v, float nx, float ny, float nz, int argb, int packedLight) {
			if (stale) {
				join();
			}
			full.transformPosition(x, y, z, position);
			turn.transform(nx, ny, nz, normal);
			float l = normal.x * normal.x + normal.y * normal.y + normal.z * normal.z;
			if (l > 1e-12f && Math.abs(l - 1) > 1e-4f) {
				float k = (float) (1 / Math.sqrt(l));
				normal.x *= k;
				normal.y *= k;
				normal.z *= k;
			}
			consumer.addVertex(position.x, position.y, position.z, argb, u, v, OverlayTexture.NO_OVERLAY, packedLight, normal.x, normal.y, normal.z);
		}

		/**
		 * A mesh worked out ahead (DroneModel's parts): per corner its place, texture and unit normal (8 floats), colour and
		 * whether it glows (drawn full bright), all put in place in one pass.
		 */
		void prepared(float[] corners, int[] colours, boolean[] glows, int packedLight) {
			for (int i = 0, o = 0; i < colours.length; i++, o += 8) {
				put(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], corners[o + 5], corners[o + 6], corners[o + 7], colours[i],
						glows[i] ? LightCoordsUtil.FULL_BRIGHT : packedLight);
			}
		}

		Geometry scale(double s) {
			matrix.scale((float) s);
			stale = true;
			return this;
		}

		/** An axis-aligned box in the current frame, every face showing texture region {@code uv}. */
		void box(double x0, double y0, double z0, double x1, double y1, double z1, float[] uv) {
			float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
			quad(ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0, uv);
			quad(ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0, uv);
			quad(ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, 0, 0, -1, uv);
			quad(bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz, 0, 0, 1, uv);
			quad(ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az, -1, 0, 0, uv);
			quad(bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, 1, 0, 0, uv);
		}

		/** A box centred on (x, y, z) with the given full sizes. */
		void centred(double x, double y, double z, double sx, double sy, double sz, float[] uv) {
			box(x - sx / 2, y - sy / 2, z - sz / 2, x + sx / 2, y + sy / 2, z + sz / 2, uv);
		}

		/** A flat horizontal square of half-size {@code r} at height {@code y}, centred on (x, z). */
		void disc(double x, double y, double z, double r, float[] uv) {
			float ax = (float) (x - r), bx = (float) (x + r), az = (float) (z - r), bz = (float) (z + r), fy = (float) y;
			quad(ax, fy, az, ax, fy, bz, bx, fy, bz, bx, fy, az, 0, 1, 0, uv);
		}

		void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
				float nx, float ny, float nz, float[] uv) {
			vertex(ax, ay, az, uv[0], uv[1], nx, ny, nz);
			vertex(bx, by, bz, uv[0], uv[3], nx, ny, nz);
			vertex(cx, cy, cz, uv[2], uv[3], nx, ny, nz);
			vertex(dx, dy, dz, uv[2], uv[1], nx, ny, nz);
		}

		/**
		 * One quad with its own UV per corner, drawn from both sides: the side facing (nx, ny, nz) and the reversed one are
		 * each lifted {@link DecorDraw#TWO_SIDED_LIFT} pixels off the middle, never one plane drawn twice (the door's render
		 * type does not cull, so twins on one plane would flicker). Positions are in blocks.
		 */
		void face(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
				float nx, float ny, float nz, float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd) {
			float[] l = DecorDraw.lift(new float[][] {{ax, ay, az}, {bx, by, bz}, {cx, cy, cz}, {dx, dy, dz}}, nx, ny, nz,
					DecorDraw.TWO_SIDED_LIFT / 16.0F);
			vertex(ax + l[0], ay + l[1], az + l[2], ua, va, nx, ny, nz);
			vertex(bx + l[0], by + l[1], bz + l[2], ub, vb, nx, ny, nz);
			vertex(cx + l[0], cy + l[1], cz + l[2], uc, vc, nx, ny, nz);
			vertex(dx + l[0], dy + l[1], dz + l[2], ud, vd, nx, ny, nz);
			vertex(dx - l[0], dy - l[1], dz - l[2], ud, vd, -nx, -ny, -nz);
			vertex(cx - l[0], cy - l[1], cz - l[2], uc, vc, -nx, -ny, -nz);
			vertex(bx - l[0], by - l[1], bz - l[2], ub, vb, -nx, -ny, -nz);
			vertex(ax - l[0], ay - l[1], az - l[2], ua, va, -nx, -ny, -nz);
		}

		private void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
			put(x, y, z, u, v, nx, ny, nz, color, light);
		}
	}
}
