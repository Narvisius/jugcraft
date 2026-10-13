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
public class DroneDepotRenderer implements BlockEntityRenderer<DroneTerminalBlockEntity, DroneDepotRenderer.State> {
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
		for (DepotView.DockView dock : view.docks) {
			if (cameraPos.distanceToSqr(origin.getX() + dock.x(), origin.getY() + dock.y(), origin.getZ() + dock.z()) > 160 * 160) continue;
			state.drones.add(drone(level, origin, dock.tier(), dock.x(), dock.y(), dock.z(), DroneModel.dockYaw(dock.tier()), -1, 0, true, 0));
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
			if (cameraPos.distanceToSqr(origin.getX() + pose.x(), origin.getY() + pose.y(), origin.getZ() + pose.z()) <= 160 * 160) {
				state.drones.add(drone(level, origin, flight.tier(), pose.x(), pose.y(), pose.z(), (float) pose.yaw(), (float) pose.cable(),
						(float) (now * 1.7 + flight.drone()), resting, (float) pose.level()));
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
					(float) open, Float.intBitsToFloat(light), view.towerLayout >= io.github.jimbozoomer.jugcraft.tower.TowerData.FIRST_INDUSTRIAL_LAYOUT ? 1 : 0,
					cameraPos.distanceToSqr(cx, cy, cz) <= 48 * 48 ? 1 : 0});
		}
		for (int i = 0; i < count; i++) {
			if (hatchOpen[i] > 0) {
				double[] p = pickups[i];
				int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(p[0] + origin.getX(), p[1] + origin.getY() + 0.5, p[2] + origin.getZ()));
				state.hatches.add(new float[] {(float) p[0], (float) p[1], (float) p[2], (float) hatchOpen[i], (float) liftRise[i], Float.intBitsToFloat(light)});
			}
		}
	}

	private static float[] drone(Level level, BlockPos origin, int tier, double x, double y, double z, float yaw, float cable, float spin,
			boolean resting, float cruising) {
		int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(x + origin.getX(), y + origin.getY() + 0.3, z + origin.getZ()));
		return new float[] {(float) x, (float) y, (float) z, yaw, tier, cable, resting ? 0 : spin, Float.intBitsToFloat(light), cruising};
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
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
			Geometry g = new Geometry(pose, consumer);
			for (float[] d : drones) {
				DroneModel.draw(g, d[0], d[1], d[2], d[3], (int) d[4], d[5], d[6], Float.floatToRawIntBits(d[7]), d[8]);
			}
			for (float[] hatch : hatches) {
				drawHatch(g, hatch[0], hatch[1], hatch[2], hatch[3], hatch[4], Float.floatToRawIntBits(hatch[5]));
			}
		});
		if (!doors.isEmpty()) {
			List<float[]> industrial = doors.stream().filter(door -> door.length > 10 && door[10] == 1).toList();
			List<float[]> legacy = doors.stream().filter(door -> door.length <= 10 || door[10] != 1).toList();
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
			float c = DroneModel.CRATE_SIZE / 2;
			g.box(cx - c, liftY + 0.04f, cz - c, cx + c, liftY + 0.04f + DroneModel.CRATE_SIZE, cz + c, DroneModel.CRATE);
		}
	}

	/**
	 * Emits textured, lit boxes and quads into one vertex consumer through a small transform stack
	 * (translate, rotate, scale), so drone parts can be placed, turned and tilted like model parts.
	 */
	static final class Geometry {
		private final PoseStack.Pose pose;
		private final VertexConsumer consumer;
		private final java.util.ArrayDeque<org.joml.Matrix4f> stack = new java.util.ArrayDeque<>();
		private org.joml.Matrix4f matrix = new org.joml.Matrix4f();
		private final org.joml.Vector3f position = new org.joml.Vector3f();
		private final org.joml.Vector3f normal = new org.joml.Vector3f();
		int light = LightCoordsUtil.FULL_BRIGHT;
		int color = 0xFFFFFFFF;

		Geometry(PoseStack.Pose pose, VertexConsumer consumer) {
			this.pose = pose;
			this.consumer = consumer;
		}

		void push() {
			stack.push(new org.joml.Matrix4f(matrix));
		}

		void pop() {
			matrix = stack.pop();
		}

		void reset() {
			stack.clear();
			matrix = new org.joml.Matrix4f();
		}

		Geometry translate(double x, double y, double z) {
			matrix.translate((float) x, (float) y, (float) z);
			return this;
		}

		/** Turns about the vertical axis; 0 faces +z (south), positive turns towards +x. */
		Geometry yaw(double radians) {
			matrix.rotateY((float) radians);
			return this;
		}

		/** Tilts about the x axis; positive tips +y towards +z (forward). */
		Geometry pitch(double radians) {
			matrix.rotateX((float) radians);
			return this;
		}

		Geometry roll(double radians) {
			matrix.rotateZ((float) radians);
			return this;
		}

		Geometry scale(double s) {
			matrix.scale((float) s);
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
			matrix.transformPosition(position.set(x, y, z));
			matrix.transformDirection(normal.set(nx, ny, nz)).normalize();
			consumer.addVertex(pose, position.x, position.y, position.z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light).setNormal(pose, normal.x, normal.y, normal.z);
		}
	}
}
