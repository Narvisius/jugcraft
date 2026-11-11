package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.tower.BigMapTableBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.MapScan;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Big Map Table (tower/BigMapTableBlock; work list item 16): the table from tools/big_map_table.py's mesh, shaded by
 * the world (its toe-kick light and the projector's emitter grid full bright); over its glass well the world round the
 * tower in miniature (client/BigMap, from tower/MapScan), full bright, its edge fading out; on it the drones out with
 * their drop lines, routes home and stops ahead, the rim and its ticks and the compass; and on the four corner panels
 * the map's page, live: the view (site, land or the build radius), how far it reaches, the drones out, the clock and
 * what is on the map.
 */
public final class BigMapTableRenderer implements BlockEntityRenderer<BigMapTableBlockEntity, BigMapTableRenderer.State> {
	static final String MESH_NAME = "big_map_table";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/big_map_table.png");
	private static final RenderType TABLE = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType MAP = RenderTypes.entitySolid(DroneDepotRenderer.TEXTURE);
	private static final RenderType GLOW = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	/** The well's glass (tools/big_map_table.py GLASS_Y) and the rim on it, inside the well's lip (WELL_R 6.0). */
	static final double GLASS_Y = 0.846, RIM_R = MapScan.MAP_R + 0.12;
	/** The panels' page: 384 by 216 units over the screen (1.0 by 0.56 blocks). */
	static final float PAGE_W = 384, PAGE_H = 216;
	static final float LIFT = 0.0006F;
	static final int AMBER = 0xFFFFB04A, HOT = 0xFFFFE2A8, DIM = 0xFF78603C, BG = 0xFF080A0D, HEAD = 0xFF18120A, CHIP_OFF = 0xFF1A1610,
			CHIP_ON = 0xFF281E0E, CHIP_DARK = 0xFF100E0C, INK = 0xFF140E06;
	/**
	 * The drones (flying orange, carrying yellow) and the blueprint's stops ahead (blueprint blue): solid, lit cubes in
	 * colours that stand out on the green land (Justin, 8 November 2026: they were see-through, and green on green).
	 */
	static final int FLYING = 0xFFFF7A1E, CARRYING = 0xFFFFD23C, STOP = 0xFF3C8CFF;
	static final int LINE = 0xCCB4E6FF, ROUTE = 0xE6FFC450, RIM = 0xCCFFBA5A, TICK = 0xB0FFBA5A;
	private static final String[] VIEW_NAMES = {"SITE", "LAND", "RADIUS"};
	private static final String[] CHIPS = {"TERRAIN", "ROUTES", "TOWER", "LIVE"};

	private final Font font;

	public static final class State extends BlockEntityRenderState {
		boolean anchor;
		BigMap.Mesh map;
		/** Overlays (full bright, see-through): boxes x0 y0 z0 x1 y1 z1 colour, and flat quads; the solid boxes (drones, stops). */
		final List<float[]> boxes = new ArrayList<>();
		final List<float[]> solidBoxes = new ArrayList<>();
		final BigMap.Quads flat = new BigMap.Quads();
		float[] flatQuads = new float[0];
		final List<float[]> labels = new ArrayList<>();
		final List<String> labelText = new ArrayList<>();
		/** The panels' page. */
		boolean panels;
		int viewTier;
		String reach = "", drones = "", clock = "";
		final boolean[] chips = new boolean[CHIPS.length];
		float liftScale = 1;
		/** The camera in the table's space; whether it is far enough off for the coarse map. */
		double camX, camY, camZ;
		boolean far;
		BlockPos at = BlockPos.ZERO;
	}

	public BigMapTableRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	BigMapTableRenderer() {
		this.font = net.minecraft.client.Minecraft.getInstance().font;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** The table is 13 blocks across and its map stands over it. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	@Override
	public void extractRenderState(BigMapTableBlockEntity table, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		state.boxes.clear();
		state.flat.size = 0;
		state.labels.clear();
		state.labelText.clear();
		state.map = null;
		Level level = table.getLevel();
		state.anchor = level != null;
		if (level == null) {
			return;
		}
		BlockPos pos = table.getBlockPos();
		double now = level.getGameTime() + partialTick;
		Vec3 camera = cameraPos == null ? new Vec3(pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 6) : cameraPos;
		double distance = camera.distanceTo(Vec3.atCenterOf(pos));
		state.camX = camera.x - pos.getX();
		state.camY = camera.y - pos.getY();
		state.camZ = camera.z - pos.getZ();
		state.far = Math.hypot(state.camX - 0.5, state.camZ - 0.5) > BigMap.FAR;
		state.at = pos;
		state.liftScale = (float) Math.max(1, Math.min(8, (distance / 18) * (distance / 18)));
		BigMap.Mesh map = BigMap.mesh(pos);
		state.map = map;
		BlockPos link = table.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		// the rim and its ticks on the glass, the compass
		double rimY = GLASS_Y + 0.004;
		state.flat.ringFlat(0.5, rimY, 0.5, RIM_R, 0.024, 128, RIM);
		for (int k = 0; k < 72; k++) {
			double a = k * Math.PI / 36, len = k % 18 == 0 ? 0.12 : k % 6 == 0 ? 0.08 : 0.04;
			double sx = Math.sin(a), sz = -Math.cos(a);
			state.flat.ribbon(0.5 + sx * (RIM_R + 0.02), rimY, 0.5 + sz * (RIM_R + 0.02), 0.5 + sx * (RIM_R + 0.02 + len), rimY,
					0.5 + sz * (RIM_R + 0.02 + len), 0.014, TICK);
		}
		String[] names = {"N", "E", "S", "W"};
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2;
			double x = 0.5 + Math.sin(a) * (MapScan.MAP_R - 0.25), z = 0.5 - Math.cos(a) * (MapScan.MAP_R - 0.25);
			float yaw = (float) Math.atan2(camera.x - (pos.getX() + x), camera.z - (pos.getZ() + z));
			state.labels.add(new float[] {(float) x, (float) (BigMap.FLOOR + 0.14), (float) z, yaw, 0.02F, AMBER});
			state.labelText.add(names[k]);
		}
		// the drones out, how far the farthest is (the view the panels name)
		double far = 0;
		int out = 0;
		if (map != null && view != null && link != null) {
			double ox = map.grid.centre.getX() + 0.5, oz = map.grid.centre.getZ() + 0.5;
			double reach = map.grid.view;
			for (DepotView.FlightView flight : view.flights) {
				FlightPath path = flight.path();
				FlightPath.Pose pose = path.poseAt(flight.ticksAt(now, view.syncTime));
				double dx = link.getX() + pose.x() - ox, dz = link.getZ() + pose.z() - oz, wy = link.getY() + pose.y();
				far = Math.max(far, Math.hypot(dx, dz));
				for (int i = 0; i < path.points(); i++) {
					far = Math.max(far, Math.hypot(link.getX() + path.x(i) - ox, link.getZ() + path.z(i) - oz));
				}
				out++;
				double dist = Math.hypot(dx, dz);
				if (dist > reach) {
					dx *= reach / dist;
					dz *= reach / dist;
				}
				double mx = 0.5 + dx * map.s, mz = 0.5 + dz * map.s;
				double ground = map.y(map.groundAt(dx, dz));
				double my = Math.max(ground + 0.06, Math.min(BigMap.FLOOR + MapScan.HEADROOM + 0.3, map.y(wy)));
				double size = 0.045;
				solidBox(state, mx - size, my - size, mz - size, mx + size, my + size, mz + size, pose.cable() > 0 ? CARRYING : FLYING);
				box(state, mx - 0.004, ground, mz - 0.004, mx + 0.004, my - size, mz + 0.004, LINE);
				// the route home: dashes over the ground to the terminal, moving
				double hx = link.getX() + 0.5 - ox, hz = link.getZ() + 0.5 - oz;
				double len = Math.hypot(dx - hx, dz - hz) * map.s;
				int dashes = (int) (len / 0.14);
				for (int k = 0; k < dashes; k++) {
					double f0 = (k * 0.14 + ((now * 0.004) % 0.14)) / len, f1 = Math.min(1, f0 + 0.07 / len);
					if (f0 >= 1) break;
					double ax = dx + (hx - dx) * f0, az = dz + (hz - dz) * f0, bx = dx + (hx - dx) * f1, bz = dz + (hz - dz) * f1;
					state.flat.ribbon(0.5 + ax * map.s, map.y(map.groundAt(ax, az)) + 0.02, 0.5 + az * map.s, 0.5 + bx * map.s,
							map.y(map.groundAt(bx, bz)) + 0.02, 0.5 + bz * map.s, 0.016, ROUTE);
				}
				// the stops ahead
				for (int i = 2; i < path.points() - 1; i++) {
					double sx = link.getX() + path.x(i) - ox, sz = link.getZ() + path.z(i) - oz;
					if (Math.hypot(sx, sz) > reach) continue;
					double px = 0.5 + sx * map.s, pz = 0.5 + sz * map.s, py = map.y(map.groundAt(sx, sz));
					solidBox(state, px - 0.025, py, pz - 0.025, px + 0.025, py + 0.05, pz + 0.025, STOP);
				}
			}
		}
		state.flatQuads = state.flat.toArray();
		// the panels' page
		state.panels = distance < 24;
		state.viewTier = MapScan.VIEWS.length - 1;
		for (int t = 0; t < MapScan.VIEWS.length; t++) {
			if (far * 1.15 <= MapScan.VIEWS[t]) {
				state.viewTier = t;
				break;
			}
		}
		state.reach = map == null ? "SURVEYING..." : map.grid.view < 1000 ? "VIEW " + Math.round(map.grid.view) + " BLOCKS"
				: "VIEW " + Math.round(map.grid.view / 16) + " CHUNKS";
		state.drones = "DRONES OUT " + out;
		state.clock = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(level.getOverworldClockTime());
		state.chips[0] = map != null;
		state.chips[1] = out > 0;
		state.chips[2] = view != null && view.towerPos != null;
		state.chips[3] = level.getGameTime() - BigMap.received(pos) < MapScan.REFRESH + 100;
	}

	private static void box(State state, double x0, double y0, double z0, double x1, double y1, double z1, int colour) {
		state.boxes.add(new float[] {(float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, Float.intBitsToFloat(colour)});
	}

	private static void solidBox(State state, double x0, double y0, double z0, double x1, double y1, double z1, int colour) {
		state.solidBoxes.add(new float[] {(float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, Float.intBitsToFloat(colour)});
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.anchor) {
			return;
		}
		// nothing of it in view, nothing drawn (it is drawn off screen: the table and its map reach far past its master's cell)
		if (camera.cullFrustum != null && !camera.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(state.at.getX() - 7, state.at.getY(),
				state.at.getZ() - 7, state.at.getX() + 8, state.at.getY() + 1 + MapScan.HEADROOM + 0.5, state.at.getZ() + 8))) {
			return;
		}
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		int light = state.lightCoords;
		if (mesh != null && !BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.BIG_MAP_TABLE)) {   // else baked
			collector.submitCustomGeometry(poses, TABLE, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			});
		}
		BigMap.Mesh map = state.map == null ? null : state.far && state.map.coarse != null ? state.map.coarse : state.map;
		if (map != null) {
			// only the faces turned to the camera: of each direction's (sorted by plane) the run on the camera's side
			double[] cam = {state.camX, state.camX, state.camY, state.camY, state.camZ, state.camZ};
			int[][] solidRange = new int[BigMap.DIRECTIONS][], glowRange = new int[BigMap.DIRECTIONS][];
			for (int d = 0; d < BigMap.DIRECTIONS; d++) {
				solidRange[d] = BigMap.Mesh.front(d, map.solidPlanes[d], cam[d]);
				glowRange[d] = BigMap.Mesh.front(d, map.glowPlanes[d], cam[d]);
			}
			collector.submitCustomGeometry(poses, MAP, (pose, consumer) -> {
				for (int d = 0; d < BigMap.DIRECTIONS; d++) {
					emit(pose, consumer, map.solid[d], solidRange[d][0], solidRange[d][1]);
				}
			});
			collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
				for (int d = 0; d < BigMap.DIRECTIONS; d++) {
					emit(pose, consumer, map.glow[d], glowRange[d][0], glowRange[d][1]);
				}
			});
		}
		if (!state.solidBoxes.isEmpty()) {
			// solid, lit by their faces' turn (full bright otherwise), so each reads as a little block
			List<float[]> solid = List.copyOf(state.solidBoxes);
			collector.submitCustomGeometry(poses, MAP, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				for (float[] b : solid) {
					g.color = Float.floatToRawIntBits(b[6]);
					g.box(b[0], b[1], b[2], b[3], b[4], b[5], DroneModel.WHITE);
				}
			});
		}
		if (!state.boxes.isEmpty() || state.flatQuads.length > 0) {
			List<float[]> boxes = List.copyOf(state.boxes);
			float[] flat = state.flatQuads;
			collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				for (float[] b : boxes) {
					g.color = Float.floatToRawIntBits(b[6]);
					g.box(b[0], b[1], b[2], b[3], b[4], b[5], DroneModel.WHITE);
				}
				emit(pose, consumer, flat, 0, flat.length / BigMap.STRIDE);
			});
		}
		for (int i = 0; i < state.labels.size(); i++) {
			float[] l = state.labels.get(i);
			String text = state.labelText.get(i);
			poses.pushPose();
			poses.translate(l[0], l[1], l[2]);
			poses.mulPose(new Matrix4f().rotationY(l[3]));
			poses.scale(l[4], -l[4], l[4]);
			collector.submitText(poses, -font.width(text) / 2f, -4, Texts.of(text), false,
					Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, Float.floatToRawIntBits(l[5]), 0, 0);
			poses.popPose();
		}
		if (state.panels && mesh != null) {
			for (int f = 0; f < mesh.live.length; f++) {
				if (mesh.liveKind[f] == ConsoleMesh.LCD) {
					panel(state, poses, collector, mesh.live[f]);
				}
			}
		}
	}

	/** Draws quads {@code from} to {@code to} of the map's full bright, every normal up (the shading is in their colours). */
	static void emit(PoseStack.Pose pose, VertexConsumer consumer, float[] quads, int from, int to) {
		float[] uv = DroneModel.WHITE;
		float u = (uv[0] + uv[2]) / 2, v = (uv[1] + uv[3]) / 2;
		for (int q = from * BigMap.STRIDE, end = to * BigMap.STRIDE; q < end; q += 4) {
			consumer.addVertex(pose, quads[q], quads[q + 1], quads[q + 2]).setColor(Float.floatToRawIntBits(quads[q + 3])).setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
		}
	}

	/** One corner panel's page on its live face (tl, tr, br, bl as its reader sees them). */
	private void panel(State state, PoseStack poses, SubmitNodeCollector collector, float[][] c) {
		float rx = c[1][0] - c[0][0], ry = c[1][1] - c[0][1], rz = c[1][2] - c[0][2];
		float dx = c[3][0] - c[0][0], dy = c[3][1] - c[0][1], dz = c[3][2] - c[0][2];
		float width = (float) Math.sqrt(rx * rx + ry * ry + rz * rz), height = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (width < 1e-4F || height < 1e-4F) return;
		rx /= width; ry /= width; rz /= width;
		float ux = -dx / height, uy = -dy / height, uz = -dz / height;
		// out of the screen: right x up
		float nx = ry * uz - rz * uy, ny = rz * ux - rx * uz, nz = rx * uy - ry * ux;
		float k = width / PAGE_W;
		poses.pushPose();
		poses.mulPose(new Matrix4f(rx * k, ry * k, rz * k, 0, ux * k, uy * k, uz * k, 0, nx * k, ny * k, nz * k, 0, c[0][0], c[0][1], c[0][2], 1));
		float lift = LIFT * state.liftScale / k;
		List<float[]> rects = new ArrayList<>();
		rect(rects, 0, 0, PAGE_W, PAGE_H, BG, 1);
		rect(rects, 0, 0, PAGE_W, 31, HEAD, 2);
		for (int t = 0; t < 3; t++) {
			float x0 = 12 + t * 122;
			boolean on = t == state.viewTier;
			rect(rects, x0, 44, 112, 44, on ? AMBER : CHIP_OFF, 2);
			frame(rects, x0, 44, 112, 44, AMBER, 3);
		}
		for (int t = 0; t < CHIPS.length; t++) {
			float x0 = 12 + t * 92;
			boolean on = state.chips[t];
			rect(rects, x0, 160, 84, 36, on ? CHIP_ON : CHIP_DARK, 2);
			frame(rects, x0, 160, 84, 36, on ? AMBER : DIM, 3);
		}
		float[] white = DroneModel.WHITE;
		float u = (white[0] + white[2]) / 2, v = (white[1] + white[3]) / 2;
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
			for (float[] r : rects) {
				int colour = Float.floatToRawIntBits(r[4]);
				float z = lift * r[5];
				float[][] corners = {{r[0], -r[1]}, {r[0], -r[3]}, {r[2], -r[3]}, {r[2], -r[1]}};
				for (float[] p : corners) {
					consumer.addVertex(pose, p[0], p[1], z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
							.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
				}
			}
		});
		float textLift = lift * 5;
		text(poses, collector, "MAP TABLE", 12, 15, 2.2F, HOT, 0, textLift);
		text(poses, collector, state.clock, PAGE_W - 12, 15, 1.8F, AMBER, 2, textLift);
		for (int t = 0; t < 3; t++) {
			boolean on = t == state.viewTier;
			text(poses, collector, VIEW_NAMES[t], 12 + t * 122 + 56, 66, 2.0F, on ? INK : AMBER, 1, textLift);
		}
		text(poses, collector, state.reach, 12, 108, 1.8F, HOT, 0, textLift);
		text(poses, collector, state.drones, 12, 134, 1.8F, AMBER, 0, textLift);
		for (int t = 0; t < CHIPS.length; t++) {
			text(poses, collector, CHIPS[t], 12 + t * 92 + 42, 178, 1.4F, state.chips[t] ? HOT : DIM, 1, textLift);
		}
		poses.popPose();
	}

	private static void rect(List<float[]> rects, float x, float y, float w, float h, int colour, int layer) {
		rects.add(new float[] {x, y, x + w, y + h, Float.intBitsToFloat(colour), layer});
	}

	private static void frame(List<float[]> rects, float x, float y, float w, float h, int colour, int layer) {
		float t = 2;
		rect(rects, x, y, w, t, colour, layer);
		rect(rects, x, y + h - t, w, t, colour, layer);
		rect(rects, x, y + t, t, h - 2 * t, colour, layer);
		rect(rects, x + w - t, y + t, t, h - 2 * t, colour, layer);
	}

	/** Text on the page, its middle line at y; align 0 left, 1 centred, 2 right of x. */
	private void text(PoseStack poses, SubmitNodeCollector collector, String s, float x, float y, float scale, int colour, int align, float lift) {
		float width = font.width(s) * scale;
		float left = x - (align == 2 ? width : align == 1 ? width / 2 : 0);
		poses.pushPose();
		poses.translate(left, -(y - 4 * scale), lift);
		poses.scale(scale, -scale, scale);
		collector.submitText(poses, 0, 0, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
		poses.popPose();
	}
}
