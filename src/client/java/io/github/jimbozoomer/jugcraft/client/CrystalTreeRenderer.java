package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock;
import net.minecraft.core.BlockPos;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

/**
 * The Crystal Tree (tower/CrystalTreeBlock; work list item 2, approved 22 October 2026): the planter and the bench from
 * tools/crystal_tree.py, and the tree grown from the master's seed ({@link CrystalTreeGeometry}, cached by seed), drawn
 * live: pulses of light travel up the trunk's veins, out along the limbs and down through the crystals (each glowing
 * vertex's brightness read from its path distance), the threads sway slowly, each with its crystals, and each crystal's
 * soft glow is a halo turned to face the camera.
 */
public final class CrystalTreeRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, CrystalTreeRenderer.State> {
	static final String MESH_NAME = "crystal_tree_planter";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/crystal_tree.png");
	private static final RenderType SOLID = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLOW = GlowTypes.glow(TEXTURE);
	/** The pulses' lookup: brightness every 5 cm of path, out to 40 m. */
	private static final double BUCKET = 0.05;
	private static final int BUCKETS = 800;
	/** How fast the threads sway (radians a second). */
	private static final double SWAY = 0.7;

	public CrystalTreeRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		boolean present;
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

	/** The tree reaches four blocks round its master cell and eight over it. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	// ------------------------------------------------------------------ the atlas map and the grown trees

	private static Map<String, float[]> tiles;

	/** Where each tile is on the atlas (u0 v0 u1 v1), from tower_mesh/crystal_tree.json. */
	static synchronized Map<String, float[]> tiles() {
		if (tiles == null) {
			Map<String, float[]> out = new HashMap<>();
			try {
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("tower_mesh/crystal_tree.json"));
				if (resource.isPresent()) {
					try (InputStream in = resource.get().open()) {
						var json = com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
						for (var e : json.getAsJsonObject("tiles").entrySet()) {
							var a = e.getValue().getAsJsonArray();
							out.put(e.getKey(), new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()});
						}
					}
				} else {
					Jugcraft.LOGGER.error("Missing the crystal tree's atlas map");
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the crystal tree's atlas map", e);
			}
			tiles = out;
		}
		return tiles;
	}

	/** The last few trees grown (a world rarely shows more than a couple at once). */
	private static final Map<Long, CrystalTreeGeometry> TREES = new LinkedHashMap<>(8, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, CrystalTreeGeometry> eldest) {
			return size() > 6;
		}
	};

	static CrystalTreeGeometry tree(long seed) {
		return TREES.computeIfAbsent(seed, s -> CrystalTreeGeometry.grow(s, tiles()));
	}

	/** Each tree's bark that never moves, for the chunk builders (any thread; client/BakedFixtures bakes it). */
	private static final Map<Long, float[]> STILL = new java.util.concurrent.ConcurrentHashMap<>();

	static float[] stillBark(long seed) {
		if (STILL.size() > 16) {
			STILL.clear();
		}
		return STILL.computeIfAbsent(seed, s -> CrystalTreeGeometry.grow(s, tiles()).barkStill);
	}

	/**
	 * The tree's still bark into the chunk (Justin's frame rate, 8 November 2026, "Bake the crystal trees": about half its
	 * bark, some 45,000 corners a frame each tree). Same texture as the planter (its sprite on the block atlas); lit by the
	 * world where each quad is and shaded by its normal, as the renderer's bark is.
	 */
	static void bakeBark(net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter e,
			net.minecraft.client.resources.model.sprite.Material.Baked sprite, BlockPos pos) {
		float[] b = stillBark(CrystalTreeBlock.seed(pos));
		final int B = CrystalTreeGeometry.BARK;
		for (int o = 0; o + 4 * B <= b.length; o += 4 * B) {
			for (int k = 0; k < 4; k++) {
				int c = o + k * B;
				e.pos(k, b[c], b[c + 1], b[c + 2]);
				e.uv(k, b[c + 3], b[c + 4]);
				e.normal(k, b[c + 5], b[c + 6], b[c + 7]);
				e.color(k, 0xFFFFFFFF);
			}
			e.cullFace(null);
			e.materialBake(sprite, net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView.BAKE_NORMALIZED);
			e.chunkLayer(net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);
			e.emissive(false);
			e.ambientOcclusion(net.fabricmc.fabric.api.util.TriState.FALSE);
			e.shadeMode(net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode.ENHANCED);
			e.emit();
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		Level level = be.getLevel();
		state.present = level != null && be.getBlockState().getBlock() instanceof CrystalTreeBlock;
		state.seed = CrystalTreeBlock.seed(be.getBlockPos());
		state.seconds = level == null ? 0 : (level.getGameTime() + partialTick) / 20.0;
	}

	private static int grey(float b, int alpha) {
		int v = Math.max(0, Math.min(255, Math.round(b * 255)));
		return alpha << 24 | v << 16 | v << 8 | v;
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.present) {
			return;
		}
		ConsoleMesh planter = ConsoleMesh.get(MESH_NAME);
		CrystalTreeGeometry tree = tree(state.seed);
		double t = state.seconds;
		// this moment's pulses along the path, and each thread's sway
		float[] pulses = new float[BUCKETS];
		for (int i = 0; i < BUCKETS; i++) {
			pulses[i] = CrystalTreeGeometry.pulse(i * BUCKET, t);
		}
		int threads = tree.threads.length / CrystalTreeGeometry.THREAD;
		float[] swayX = new float[threads], swayZ = new float[threads];
		for (int i = 0; i < threads; i++) {
			int o = i * CrystalTreeGeometry.THREAD;
			double k = tree.threads[o + 3] * Math.sin(SWAY * t + tree.threads[o]);
			swayX[i] = (float) (tree.threads[o + 1] * k);
			swayZ[i] = (float) (tree.threads[o + 2] * k);
		}
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, SOLID, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (planter != null && !BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CRYSTAL_TREE)) {   // else baked
				ConsoleMesh.emit(g, planter.quads[ConsoleMesh.SOLID], light, false);
				ConsoleMesh.emit(g, planter.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			}
			// the bark that never moves is baked into the chunk with the planter; here only what sways
			float[] b = BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CRYSTAL_TREE) ? tree.barkSway : tree.bark;
			for (int o = 0; o + CrystalTreeGeometry.BARK <= b.length; o += CrystalTreeGeometry.BARK) {
				float x = b[o], z = b[o + 2];
				int th = (int) b[o + 8];
				if (th >= 0) {
					float w = b[o + 9] * b[o + 9];
					x += swayX[th] * w;
					z += swayZ[th] * w;
				}
				g.meshVertex(x, b[o + 1], z, b[o + 3], b[o + 4], b[o + 5], b[o + 6], b[o + 7], 0xFFFFFFFF, light);
			}
			// far off (Justin's "Less detail far away") the veins' pulses, a pixel wide from there, are left out
			float[] v = g.cameraDistance() > FAR ? new float[0] : tree.veins;
			for (int o = 0; o + CrystalTreeGeometry.VEIN <= v.length; o += CrystalTreeGeometry.VEIN) {
				float p = pulse(pulses, v[o + 5]);
				g.meshVertex(v[o], v[o + 1], v[o + 2], v[o + 3], v[o + 4], 0, 1, 0, grey(p, 255), LightCoordsUtil.FULL_BRIGHT);
			}
		});
		// the camera, in the tree's frame, for the halos to face it
		double cx = camera.pos.x - state.blockPos.getX(), cy = camera.pos.y - state.blockPos.getY(), cz = camera.pos.z - state.blockPos.getZ();
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (planter != null) {
				ConsoleMesh.emit(g, planter.quads[ConsoleMesh.GLOW], LightCoordsUtil.FULL_BRIGHT, true);
			}
			float[] q = tree.glow;
			for (int o = 0; o + CrystalTreeGeometry.GLOW <= q.length; o += CrystalTreeGeometry.GLOW) {
				float x = q[o], z = q[o + 2];
				int th = (int) q[o + 10];
				if (th >= 0) {
					float w = q[o + 11] * q[o + 11];
					x += swayX[th] * w;
					z += swayZ[th] * w;
				}
				float s = q[o + 8], base = q[o + 9];
				float bright = s < 0 ? base : Math.min(1f, base * (0.8f + 0.5f * pulse(pulses, s)));
				g.meshVertex(x, q[o + 1], z, q[o + 3], q[o + 4], q[o + 5], q[o + 6], q[o + 7], grey(bright, 255), LightCoordsUtil.FULL_BRIGHT);
			}
			// the halos: a soft round glow round each crystal, turned to the camera
			float[] tile = tiles().getOrDefault("halo", new float[] {0, 0, 0, 0});
			float[] h = g.cameraDistance() > FAR ? new float[0] : tree.halos;
			for (int o = 0; o + CrystalTreeGeometry.HALO <= h.length; o += CrystalTreeGeometry.HALO) {
				double x = h[o], y = h[o + 1], z = h[o + 2], r = h[o + 3];
				int th = (int) h[o + 6];
				if (th >= 0) {
					float w = h[o + 7] * h[o + 7];
					x += swayX[th] * w;
					z += swayZ[th] * w;
				}
				double vx = cx - x, vy = cy - y, vz = cz - z, l = Math.sqrt(vx * vx + vy * vy + vz * vz);
				if (l < 1e-4) {
					continue;
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
				float bright = Math.min(1f, 0.55f + 0.45f * pulse(pulses, h[o + 4]));
				int color = grey(1f, Math.round(bright * 255));
				float nx = (float) vx, ny = (float) vy, nz = (float) vz;
				g.meshVertex((float) (x - rx * r + ux * r), (float) (y + uy * r), (float) (z - rz * r + uz * r), tile[0], tile[1], nx, ny, nz, color,
						LightCoordsUtil.FULL_BRIGHT);
				g.meshVertex((float) (x - rx * r - ux * r), (float) (y - uy * r), (float) (z - rz * r - uz * r), tile[0], tile[3], nx, ny, nz, color,
						LightCoordsUtil.FULL_BRIGHT);
				g.meshVertex((float) (x + rx * r - ux * r), (float) (y - uy * r), (float) (z + rz * r - uz * r), tile[2], tile[3], nx, ny, nz, color,
						LightCoordsUtil.FULL_BRIGHT);
				g.meshVertex((float) (x + rx * r + ux * r), (float) (y + uy * r), (float) (z + rz * r + uz * r), tile[2], tile[1], nx, ny, nz, color,
						LightCoordsUtil.FULL_BRIGHT);
			}
		});
	}

	/** Beyond this many blocks the veins and the halos are left out. */
	static final float FAR = 40;

	private static float pulse(float[] pulses, float s) {
		int i = (int) (s / BUCKET);
		return pulses[Math.max(0, Math.min(BUCKETS - 1, i))];
	}
}
