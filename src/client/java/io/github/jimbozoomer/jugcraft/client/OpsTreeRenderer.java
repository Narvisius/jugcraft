package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.OpsTreeBlock;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The flight-ops trees (tower/OpsTreeBlock; Justin's choice of 5 November 2026), baked by tools/ops_trees.py and drawn
 * live here. Each placed tree shows one of the baked variants, turned by an angle of its own (both from its seed).
 *
 * <ul>
 * <li>The Ember Oak: its bed (never turned, as the columns collide), then the tree; light breathes through the cracks in
 * its bark (a slow wave up the trunk and out along the limbs, a quick flicker over it), its hot leaves and the coals in
 * the gravel flicker, soft ember glows hang in the crown and sparks rise out of it to the ceiling, fading as they go.</li>
 * <li>The Drift Isle: its basin, the emitter rings pulsing outwards and waves of light going up the lift beam; over it the
 * island, bobbing gently and turning once in five minutes, its root tips pulsing; five stones circling it, tumbling.</li>
 * </ul>
 * Everything moving is drawn here, on the client: the server only keeps the blocks.
 */
public final class OpsTreeRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, OpsTreeRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/ops_trees.png");
	private static final RenderType SOLID = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLOW = GlowTypes.glow(TEXTURE);
	/** The baked variants of each tree. */
	static final int VARIANTS = 3;
	/** The island: how far it bobs (blocks), its bob's period and how long it takes to turn once (seconds). */
	static final double BOB = 0.07, BOB_PERIOD = 7.5, TURN = 300;
	/** Floats a billboard: the Ember Oak's glows (x y z radius phase) and sparks (x z from to size speed phase), the Drift
	 * Isle's root tips (x y z radius phase) and stones (orbit's radius, height, angle at 0, size, speed, phase). */
	static final int HALO = 5, SPARK = 7, TIP = 5, STONE = 6;

	public OpsTreeRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		OpsTreeBlock.@Nullable Kind kind;
		long seed;
		double seconds;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	/** The trees reach four blocks round their master cells and seven over them. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** Which baked variant the tree at this seed shows. */
	static int variant(long seed) {
		return (int) Math.floorMod(seed >>> 7, (long) VARIANTS);
	}

	/** How far the tree at this seed is turned (radians; Geometry.yaw's sense). */
	static double turn(long seed) {
		return ((seed >>> 20) & 0xFFFF) / 65536.0 * 2 * Math.PI;
	}

	// ------------------------------------------------------------------ the map: atlas tiles, billboards per variant

	static final class Info {
		final Map<String, float[]> tiles = new HashMap<>();
		final float[][] halos = new float[VARIANTS][], sparks = new float[VARIANTS][], tips = new float[VARIANTS][], stones = new float[VARIANTS][];
		/** The Ember Oak's flames: how many moments their tile holds side by side, and how far apart they are in u. */
		int flameFrames = 1;
		float flameStep;

		float[] tile(String name) {
			return tiles.getOrDefault(name, new float[] {0, 0, 0, 0});
		}
	}

	private static Info info;

	static Info info() {
		if (info == null) {
			Info out = new Info();
			for (int k = 0; k < VARIANTS; k++) {
				out.halos[k] = out.sparks[k] = out.tips[k] = out.stones[k] = new float[0];
			}
			try {
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("tower_mesh/ops_trees.json"));
				if (resource.isPresent()) {
					try (InputStream in = resource.get().open()) {
						var json = com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
						for (var e : json.getAsJsonObject("tiles").entrySet()) {
							var a = e.getValue().getAsJsonArray();
							out.tiles.put(e.getKey(), new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()});
						}
						if (json.has("flame_frames")) {
							out.flameFrames = Math.max(1, json.get("flame_frames").getAsInt());
							out.flameStep = json.get("flame_step").getAsFloat();
						}
						var ember = json.getAsJsonArray("ember_oak");
						var drift = json.getAsJsonArray("drift_isle");
						for (int k = 0; k < VARIANTS; k++) {
							if (k < ember.size()) {
								out.halos[k] = floats(ember.get(k).getAsJsonObject().getAsJsonArray("halos"));
								out.sparks[k] = floats(ember.get(k).getAsJsonObject().getAsJsonArray("sparks"));
							}
							if (k < drift.size()) {
								out.tips[k] = floats(drift.get(k).getAsJsonObject().getAsJsonArray("tips"));
								out.stones[k] = floats(drift.get(k).getAsJsonObject().getAsJsonArray("stones"));
							}
						}
					}
				} else {
					Jugcraft.LOGGER.error("Missing the flight-ops trees' map");
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the flight-ops trees' map", e);
			}
			info = out;
		}
		return info;
	}

	private static float[] floats(com.google.gson.JsonArray rows) {
		int n = 0;
		for (var r : rows) {
			n += r.getAsJsonArray().size();
		}
		float[] out = new float[n];
		int i = 0;
		for (var r : rows) {
			for (var v : r.getAsJsonArray()) {
				out[i++] = v.getAsFloat();
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		Level level = be.getLevel();
		state.kind = level != null && be.getBlockState().getBlock() instanceof OpsTreeBlock tree ? tree.kind : null;
		state.seed = OpsTreeBlock.seed(be.getBlockPos());
		state.seconds = level == null ? 0 : (level.getGameTime() + partialTick) / 20.0;
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.kind == null) {
			return;
		}
		double cx = camera.pos.x - state.blockPos.getX(), cy = camera.pos.y - state.blockPos.getY(), cz = camera.pos.z - state.blockPos.getZ();
		if (state.kind == OpsTreeBlock.Kind.EMBER_OAK) {
			emberOak(state, poses, collector, cx, cy, cz);
		} else {
			driftIsle(state, poses, collector, cx, cy, cz);
		}
	}

	private static void emberOak(State state, PoseStack poses, SubmitNodeCollector collector, double cx, double cy, double cz) {
		ConsoleMesh bed = ConsoleMesh.get("ops_ember_bed");
		int k = variant(state.seed);
		ConsoleMesh tree = ConsoleMesh.get("ops_ember_oak_" + k), lit = ConsoleMesh.get("ops_ember_oak_" + k + "_bright"),
				flame = ConsoleMesh.get("ops_ember_oak_" + k + "_flame");
		double yaw = turn(state.seed), t = state.seconds;
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, SOLID, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			boolean baked = BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.EMBER_OAK);   // the bed and bark in the chunk's mesh
			if (bed != null && !baked) {
				ConsoleMesh.emit(g, bed.quads[ConsoleMesh.SOLID], light, false);
			}
			if (tree != null) {
				turned(g, yaw);
				if (!baked) {
					ConsoleMesh.emit(g, tree.quads[ConsoleMesh.SOLID], light, false);
					if (lit != null) {   // the plates' sides and the leaves: full bright, steady (in the chunk's mesh when baked)
						ConsoleMesh.emit(g, lit.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
					}
				}
				float[] c = tree.quads[ConsoleMesh.LIT];
				for (int o = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER) {
					float b = (float) (0.8 + 0.2 * Math.sin(1.9 * t + 17 * c[o] + 11 * c[o + 2]));
					g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3], c[o + 4], 0, 1, 0, grey(b, 255), LightCoordsUtil.FULL_BRIGHT);
				}
			}
		});
		Info info = info();
		// the camera in the turned tree's frame (its glows and sparks face it)
		double[] cam = unturn(cx, cz, yaw);
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			turned(g, yaw);
			if (tree != null) {
				// the cracks' light worked out once a quad (at its first corner) rather than at every corner: the quads are a
				// pixel or two across, so it looks the same, at a quarter of the sums (Justin's frame rate, 7 November 2026)
				float[] c = tree.quads[ConsoleMesh.GLOW];
				int bright = 0;
				for (int o = 0, i = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER, i++) {
					if ((i & 3) == 0) {
						bright = grey(crack(c[o], c[o + 1], c[o + 2], t), 255);
					}
					g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3], c[o + 4], 0, 1, 0, bright, LightCoordsUtil.FULL_BRIGHT);
				}
			}
			if (flame != null) {
				// the small flames licking off the standing leaves' tips (r184): each steps through its moments (its own
				// pace and start, so they flicker apart) and burns up brighter as the wave climbing the tree reaches it
				float[] c = flame.quads[ConsoleMesh.GLOW];
				float du = 0;
				int colour = 0;
				for (int o = 0, i = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER, i++) {
					if ((i & 3) == 0) {
						float x = c[o], y = c[o + 1], z = c[o + 2];
						double pace = 8 + 3 * frac(13.7 * x + 7.3 * z);
						du = Math.floorMod((long) Math.floor(t * pace + 31 * x + 17 * z), (long) info.flameFrames) * info.flameStep;
						double w = 0.5 + 0.5 * Math.sin(1.7 * t - 1.25 * (y + 0.6 * Math.hypot(x - 0.5, z - 0.5)));
						colour = grey((float) (0.8 + 0.2 * w), (int) Math.round(255 * (0.5 + 0.5 * w)));
					}
					g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3] + du, c[o + 4], 0, 1, 0, colour, LightCoordsUtil.FULL_BRIGHT);
				}
			}
			float[] halo = info.tile("ember_halo"), spark = info.tile("spark");
			boolean far = g.cameraDistance() > FAR;
			float[] h = far ? new float[0] : info.halos[k];
			for (int o = 0; o + HALO <= h.length; o += HALO) {
				float a = (float) (0.55 + 0.25 * Math.sin(0.9 * t + h[o + 4]));
				billboard(g, h[o], h[o + 1], h[o + 2], h[o + 3], cam[0], cy, cam[1], halo, grey(1f, Math.round(a * 255)));
			}
			float[] s = far ? new float[0] : info.sparks[k];
			for (int o = 0; o + SPARK <= s.length; o += SPARK) {
				double span = s[o + 3] - s[o + 2], f = frac(t * s[o + 5] / span + s[o + 6]);
				double x = s[o] + 0.12 * Math.sin(2.1 * t + s[o + 6] * 20), z = s[o + 1] + 0.12 * Math.cos(1.7 * t + s[o + 6] * 17);
				float a = (float) (Math.min(1, f / 0.12) * Math.min(1, (1 - f) / 0.4));
				billboard(g, x, s[o + 2] + span * f, z, s[o + 4], cam[0], cy, cam[1], spark, grey(1f, Math.round(a * 255)));
			}
		});
	}

	/** The cracks' light at a point of the bark: a slow wave going up the trunk and out the limbs, a quick flicker on it. */
	static float crack(float x, float y, float z, double t) {
		double s = y + 0.6 * Math.hypot(x - 0.5, z - 0.5);
		double wave = 0.5 + 0.5 * Math.sin(1.7 * t - 1.25 * s);
		double flicker = 0.9 + 0.1 * Math.sin(5.3 * t + 13 * x + 7 * z);
		return (float) ((0.5 + 0.5 * wave) * flicker);
	}

	private static void driftIsle(State state, PoseStack poses, SubmitNodeCollector collector, double cx, double cy, double cz) {
		ConsoleMesh base = ConsoleMesh.get("ops_drift_isle_base"), stone = ConsoleMesh.get("ops_drift_stone");
		int k = variant(state.seed);
		ConsoleMesh isle = ConsoleMesh.get("ops_drift_isle_" + k);
		double t = state.seconds;
		double bob = BOB * Math.sin(2 * Math.PI * t / BOB_PERIOD), spin = turn(state.seed) + 2 * Math.PI * t / TURN;
		int light = state.lightCoords;
		Info info = info();
		float[] stones = info.stones[k];
		collector.submitCustomGeometry(poses, SOLID, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (base != null) {
				if (!BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.DRIFT_ISLE)) {   // else in the chunk's mesh
					ConsoleMesh.emit(g, base.quads[ConsoleMesh.SOLID], light, false);
				}
				float[] c = base.quads[ConsoleMesh.LIT];
				for (int o = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER) {
					double r = Math.hypot(c[o] - 0.5, c[o + 2] - 0.5);
					float b = (float) (0.65 + 0.35 * (0.5 + 0.5 * Math.sin(2.2 * t - 3.0 * r)));
					g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3], c[o + 4], 0, 1, 0, grey(b, 255), LightCoordsUtil.FULL_BRIGHT);
				}
			}
			if (isle != null) {
				g.push();
				g.translate(0.5, bob, 0.5).yaw(spin).translate(-0.5, 0, -0.5);
				ConsoleMesh.emit(g, isle.quads[ConsoleMesh.SOLID], light, false);
				g.pop();
			}
			if (stone != null) {
				for (int o = 0; o + STONE <= stones.length; o += STONE) {
					double a = stones[o + 2] + stones[o + 4] * t, tumble = 0.5 * t + stones[o + 5];
					g.push();
					g.translate(0.5 + stones[o] * Math.cos(a), stones[o + 1] + 0.12 * Math.sin(0.8 * t + stones[o + 5]), 0.5 + stones[o] * Math.sin(a))
							.yaw(tumble).pitch(tumble * 0.7).scale(stones[o + 3]);
					ConsoleMesh.emit(g, stone.quads[ConsoleMesh.SOLID], light, false);
					g.pop();
				}
			}
		});
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (base != null) {
				float[] c = base.quads[ConsoleMesh.GLOW];
				for (int o = 0; o + ConsoleMesh.CORNER <= c.length; o += ConsoleMesh.CORNER) {
					float a = (float) (0.55 + 0.45 * (0.5 + 0.5 * Math.sin(2.4 * t - 4.0 * c[o + 1])));
					g.meshVertex(c[o], c[o + 1], c[o + 2], c[o + 3], c[o + 4], 0, 1, 0, grey(1f, Math.round(a * 255)), LightCoordsUtil.FULL_BRIGHT);
				}
			}
			// the root tips go round and up and down with the island
			float[] tip = info.tile("tip"), p = info.tips[k];
			double cs = Math.cos(spin), sn = Math.sin(spin);
			for (int o = 0; o + TIP <= p.length; o += TIP) {
				double lx = p[o] - 0.5, lz = p[o + 2] - 0.5;
				double x = 0.5 + lx * cs + lz * sn, z = 0.5 - lx * sn + lz * cs;
				float a = (float) (0.55 + 0.45 * Math.sin(1.6 * t + p[o + 4]));
				billboard(g, x, p[o + 1] + bob, z, p[o + 3] * 1.6, cx, cy, cz, tip, grey(1f, Math.round(Math.max(0, a) * 255)));
			}
		});
	}

	/** Beyond this many blocks the halos and sparks are left out (Justin's "Less detail far away"). */
	static final float FAR = 40;

	// ------------------------------------------------------------------ helpers

	/** Turns what follows by yaw about the tree's middle (over the master cell's middle). */
	private static void turned(DroneDepotRenderer.Geometry g, double yaw) {
		g.translate(0.5, 0, 0.5).yaw(yaw).translate(-0.5, 0, -0.5);
	}

	/** (x, z) of the block's frame in the frame turned by yaw about the tree's middle. */
	static double[] unturn(double x, double z, double yaw) {
		double dx = x - 0.5, dz = z - 0.5, c = Math.cos(yaw), s = Math.sin(yaw);
		return new double[] {0.5 + dx * c - dz * s, 0.5 + dx * s + dz * c};
	}

	private static double frac(double v) {
		return v - Math.floor(v);
	}

	/** A soft round glow of radius r at (x, y, z), turned to face the camera at (cx, cy, cz) (the same frame). */
	private static void billboard(DroneDepotRenderer.Geometry g, double x, double y, double z, double r, double cx, double cy, double cz,
			float[] tile, int color) {
		double vx = cx - x, vy = cy - y, vz = cz - z, l = Math.sqrt(vx * vx + vy * vy + vz * vz);
		if (l < 1e-4 || (color >>> 24) == 0) {
			return;
		}
		vx /= l;
		vy /= l;
		vz /= l;
		// right = up x view, up' = view x right
		double rx = vz, rz = -vx, rl = Math.sqrt(rx * rx + rz * rz);
		if (rl < 1e-4) {
			rx = 1;
			rz = 0;
			rl = 1;
		}
		rx /= rl;
		rz /= rl;
		double ux = vy * rz, uy = vz * rx - vx * rz, uz = -vy * rx;
		float nx = (float) vx, ny = (float) vy, nz = (float) vz;
		int f = LightCoordsUtil.FULL_BRIGHT;
		g.meshVertex((float) (x - rx * r + ux * r), (float) (y + uy * r), (float) (z - rz * r + uz * r), tile[0], tile[1], nx, ny, nz, color, f);
		g.meshVertex((float) (x - rx * r - ux * r), (float) (y - uy * r), (float) (z - rz * r - uz * r), tile[0], tile[3], nx, ny, nz, color, f);
		g.meshVertex((float) (x + rx * r - ux * r), (float) (y - uy * r), (float) (z + rz * r - uz * r), tile[2], tile[3], nx, ny, nz, color, f);
		g.meshVertex((float) (x + rx * r + ux * r), (float) (y + uy * r), (float) (z + rz * r + uz * r), tile[2], tile[1], nx, ny, nz, color, f);
	}

	private static int grey(float b, int alpha) {
		int v = Math.max(0, Math.min(255, Math.round(b * 255)));
		return Math.max(0, Math.min(255, alpha)) << 24 | v << 16 | v << 8 | v;
	}
}
