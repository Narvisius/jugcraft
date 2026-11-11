package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The Tower Diorama (Justin's walk through of 24 October 2026, redesign 3; take 2 approved 25 October: "a diorama of the
 * tower and generic Minecraft surroundings, something still that doesn't move and reflects true colors"): a still scale
 * model of the finished Tri-Pillar in 20 chunks of made-up land, cut square so its soil shows, on the hall's small
 * hologram table ({@link HoloMapRenderer}). Its mesh and texture are written by tools/tower_diorama.py
 * ({@code tower_mesh/tower_diorama.bin.gz}, {@code textures/entity/tower_diorama.png}): quads in the table anchor's own
 * space by kind (solid, drawn cut out; water, see-through) and by the way they face, each run sorted by its plane, so a
 * frame draws only those turned to the camera. Lit by the world like any block; nothing moves.
 */
final class TowerDiorama {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/tower_diorama.png");
	static final RenderType SOLID = RenderTypes.entityCutoutCull(TEXTURE);
	static final RenderType WATER = RenderTypes.entityTranslucentCull(TEXTURE);
	/** How far over the table's anchor block the model reaches (its tower's tip is 3.3 over the block's foot). */
	static final double HEIGHT = 3.4;
	/** Faces: +x, -x, +y, -y, +z, -z. */
	static final int DIRECTIONS = 6;
	private static final float[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
	/** Floats a corner: x, y, z, u, v. */
	static final int CORNER = 5, QUAD = 4 * CORNER;

	/** By kind (solid, water) and direction: the quads' corners, and each quad's plane (ascending). */
	final float[][][] quads = new float[2][DIRECTIONS][];
	final float[][][] planes = new float[2][DIRECTIONS][];

	private TowerDiorama() { }

	private static Optional<TowerDiorama> model;

	/** The model, read once; null if it could not be read. */
	static TowerDiorama get() {
		if (model == null) {
			model = Optional.ofNullable(load());
		}
		return model.orElse(null);
	}

	private static TowerDiorama load() {
		Identifier id = Jugcraft.id("tower_mesh/tower_diorama.bin.gz");
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing tower diorama {}", id);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				return read(in);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the tower diorama {}", id, e);
			return null;
		}
	}

	/** The model from its stream (tools/tower_diorama.py mesh_bytes). */
	static TowerDiorama read(DataInputStream in) throws java.io.IOException {
		if (in.readInt() != 0x4A54444D || in.readInt() != 1) {
			throw new IllegalStateException("not a tower diorama");
		}
		TowerDiorama d = new TowerDiorama();
		for (int kind = 0; kind < 2; kind++) {
			for (int dir = 0; dir < DIRECTIONS; dir++) {
				int n = in.readInt();
				int axis = dir / 2;
				float[] q = new float[n * QUAD], p = new float[n];
				for (int i = 0; i < n; i++) {
					for (int c = 0; c < 4; c++) {
						int o = i * QUAD + c * CORNER;
						q[o] = in.readShort() / 4096f;
						q[o + 1] = in.readShort() / 4096f;
						q[o + 2] = in.readShort() / 4096f;
						q[o + 3] = in.readUnsignedShort() / 65535f;
						q[o + 4] = in.readUnsignedShort() / 65535f;
					}
					p[i] = q[i * QUAD + axis];
				}
				d.quads[kind][dir] = q;
				d.planes[kind][dir] = p;
			}
		}
		return d;
	}

	/** Quads in all: what a frame would draw at most. */
	int count() {
		int n = 0;
		for (float[][] kind : quads) {
			for (float[] q : kind) n += q.length / QUAD;
		}
		return n;
	}

	/**
	 * Of direction {@code dir}'s quads (sorted by plane), the run turned to a camera at {@code cam} along its axis:
	 * {from, to}. A face looking along +axis shows to a camera past its plane, one along -axis to a camera short of it.
	 */
	static int[] front(int dir, float[] planes, double cam) {
		int lo = 0, hi = planes.length;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (planes[mid] < cam) lo = mid + 1;
			else hi = mid;
		}
		return dir % 2 == 0 ? new int[] {0, lo} : new int[] {lo, planes.length};
	}

	/** Draws the model at the anchor (poses at its block's corner), lit by {@code light}, seen from {@code cam} (anchor space). */
	void submit(PoseStack poses, SubmitNodeCollector collector, int light, double camX, double camY, double camZ) {
		double[] cam = {camX, camX, camY, camY, camZ, camZ};
		for (int kind = 0; kind < 2; kind++) {
			int[][] range = new int[DIRECTIONS][];
			boolean any = false;
			for (int d = 0; d < DIRECTIONS; d++) {
				range[d] = front(d, planes[kind][d], cam[d]);
				any |= range[d][1] > range[d][0];
			}
			if (!any) continue;
			float[][] byDir = quads[kind];
			collector.submitCustomGeometry(poses, kind == 0 ? SOLID : WATER, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				for (int d = 0; d < DIRECTIONS; d++) {
					float[] q = byDir[d], n = NORMALS[d];
					for (int i = range[d][0]; i < range[d][1]; i++) {
						for (int c = 0; c < 4; c++) {
							int o = i * QUAD + c * CORNER;
							g.meshVertex(q[o], q[o + 1], q[o + 2], q[o + 3], q[o + 4], n[0], n[1], n[2], 0xFFFFFFFF, light);
						}
					}
				}
			});
		}
	}
}
