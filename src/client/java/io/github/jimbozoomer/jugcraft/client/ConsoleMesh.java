package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * A mesh written by tools/tower_control_console.py ({@code assets/jugcraft/tower_mesh/<name>.bin.gz}, textured from
 * textures/entity/tower_control_console.png): the Tower Control Console's, in its master cell's frame facing north, or
 * the Console Chair's, round its axis facing south; the Curved Video Wall's and the Drone Display Case's (with their own
 * textures). Quads by kind (shaded by the world with a baked shade per corner, full bright, glass, or a translucent
 * glow), each corner {x, y, z, u, v, nx, ny, nz, shade}; the live faces the renderer draws on (kind, id,
 * corners top-left, top-right, bottom-right, bottom-left as their user sees them); the floor band's step edges.
 */
final class ConsoleMesh {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/tower_control_console.png");
	/** Quad kinds: shaded by the world, full bright, glass (translucent, shaded), glow (translucent, full bright). */
	static final int SOLID = 0, LIT = 1, GLASS = 2, GLOW = 3, KINDS = 4;
	static final int MONITOR = 0, KEYSET = 1, CLOCK = 2, ANNUNCIATOR = 3, LAMP = 4, LCD = 5;
	/** Floats a corner: position, uv, normal, shade. */
	static final int CORNER = 9;

	final float[][] quads = new float[KINDS][];
	final byte[] liveKind;
	final byte[] liveId;
	/** Per live face: tl, tr, br, bl. */
	final float[][][] live;
	/** Per step edge: cell dx, dz and side (0 north, 1 east, 2 south, 3 west, facing north). */
	final int[][] edgeCells;
	/** Per step edge: where along the cell's side the floor reaches it (0 to 1, west to east or north to south). */
	final float[][] edgeSpans;
	/** The atlas patches (u0, v0, u1, v1) the step edges are drawn from: nosing, step light, fascia. */
	final float[][] edgeTiles;

	private ConsoleMesh(float[][] quads, byte[] liveKind, byte[] liveId, float[][][] live, int[][] edgeCells, float[][] edgeSpans,
			float[][] edgeTiles) {
		System.arraycopy(quads, 0, this.quads, 0, KINDS);
		this.liveKind = liveKind;
		this.liveId = liveId;
		this.live = live;
		this.edgeCells = edgeCells;
		this.edgeSpans = edgeSpans;
		this.edgeTiles = edgeTiles;
	}

	private static final Map<String, Optional<ConsoleMesh>> MESHES = new java.util.concurrent.ConcurrentHashMap<>();

	/** The mesh {@code name}, or null if it could not be read. */
	static ConsoleMesh get(String name) {
		return MESHES.computeIfAbsent(name, n -> Optional.ofNullable(load(n))).orElse(null);
	}

	private static ConsoleMesh load(String name) {
		Identifier id = Jugcraft.id("tower_mesh/" + name + ".bin.gz");
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing console mesh {}", id);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				if (in.readInt() != 0x4A54434D || in.readInt() != 1) {
					throw new IllegalStateException("not a console mesh");
				}
				int n = in.readInt();
				float[][] byKind = new float[KINDS][n * 4 * CORNER];
				int[] used = new int[KINDS];
				for (int q = 0; q < n; q++) {
					int read = in.readByte();
					int kind = read > 0 && read < KINDS ? read : SOLID;
					float[] out = byKind[kind];
					for (int k = 0; k < 4; k++) {
						int o = used[kind];
						out[o] = in.readFloat();
						out[o + 1] = in.readFloat();
						out[o + 2] = in.readFloat();
						out[o + 5] = in.readByte() / 127f;
						out[o + 6] = in.readByte() / 127f;
						out[o + 7] = in.readByte() / 127f;
						out[o + 3] = in.readFloat();
						out[o + 4] = in.readFloat();
						out[o + 8] = in.readUnsignedByte() / 255f;
						used[kind] += CORNER;
					}
				}
				for (int k = 0; k < KINDS; k++) {
					byKind[k] = Arrays.copyOf(byKind[k], used[k]);
				}
				int faces = in.readInt();
				byte[] kinds = new byte[faces], ids = new byte[faces];
				float[][][] live = new float[faces][4][3];
				for (int i = 0; i < faces; i++) {
					kinds[i] = in.readByte();
					ids[i] = in.readByte();
					for (int c = 0; c < 4; c++) {
						live[i][c][0] = in.readFloat();
						live[i][c][1] = in.readFloat();
						live[i][c][2] = in.readFloat();
					}
				}
				int edges = in.readInt();
				int[][] cells = new int[edges][3];
				float[][] spans = new float[edges][2];
				for (int i = 0; i < edges; i++) {
					cells[i][0] = in.readByte();
					cells[i][1] = in.readByte();
					cells[i][2] = in.readByte();
					spans[i][0] = in.readFloat();
					spans[i][1] = in.readFloat();
				}
				float[][] tiles = new float[3][4];
				for (float[] t : tiles) {
					for (int i = 0; i < 4; i++) {
						t[i] = in.readFloat();
					}
				}
				return new ConsoleMesh(byKind, kinds, ids, live, cells, spans, tiles);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the console mesh {}", id, e);
			return null;
		}
	}

	/**
	 * Detail by distance (Justin's frame rate, 7 November 2026, "Less detail far away"): beyond {@link #FAR}[1] blocks a
	 * mesh is drawn with its corners closer together than {@link #CELL}[1] blocks run into one, beyond {@link #FAR}[2] with
	 * {@link #CELL}[2] (the quads that shrink to nothing, or onto one another, left out): the little parts on the shelves,
	 * the console's knobs and edges, a pixel or two across on screen from there, merge; the shapes stay. Worked out once
	 * a kind and level.
	 */
	static final float[] FAR = {0, 32, 48};
	static final float[] CELL = {0, 0.035f, 0.07f};
	private static final Map<float[], float[][]> COARSE = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

	static float[] coarse(float[] corners, int level) {
		float[][] r = COARSE.computeIfAbsent(corners, c -> new float[3][]);
		if (r[level] == null) {
			r[level] = cluster(corners, CELL[level]);
		}
		return r[level];
	}

	/** The quads of {@code corners} with their corners gathered to the middles of the {@code cell}-sized cubes they fall in. */
	static float[] cluster(float[] corners, float cell) {
		int n = corners.length / CORNER;
		java.util.HashMap<Long, Integer> ids = new java.util.HashMap<>();
		int[] id = new int[n];
		float[] sum = new float[n * 3];
		int[] count = new int[n];
		for (int i = 0; i < n; i++) {
			int o = i * CORNER;
			long kx = (long) Math.floor(corners[o] / cell), ky = (long) Math.floor(corners[o + 1] / cell), kz = (long) Math.floor(corners[o + 2] / cell);
			long key = (kx & 0x1FFFFF) << 42 | (ky & 0x1FFFFF) << 21 | (kz & 0x1FFFFF);
			Integer c = ids.get(key);
			if (c == null) {
				c = ids.size();
				ids.put(key, c);
			}
			id[i] = c;
			sum[c * 3] += corners[o];
			sum[c * 3 + 1] += corners[o + 1];
			sum[c * 3 + 2] += corners[o + 2];
			count[c]++;
		}
		float[] out = new float[corners.length];
		int m = 0;
		java.util.HashSet<String> seen = new java.util.HashSet<>();
		int[] pick = new int[4];
		for (int q = 0; q + 4 <= n; q += 4) {
			int d = 0;
			for (int k = 0; k < 4; k++) {
				boolean dup = false;
				for (int j = 0; j < d; j++) {
					dup |= id[pick[j]] == id[q + k];
				}
				if (!dup) {
					pick[d++] = q + k;
				}
			}
			if (d < 3) {
				continue;
			}
			int[] u = new int[d];
			for (int j = 0; j < d; j++) {
				u[j] = id[pick[j]];
			}
			Arrays.sort(u);
			int o0 = q * CORNER;
			String key = Arrays.toString(u) + (corners[o0 + 5] > 0 ? "+" : "-") + (corners[o0 + 6] > 0 ? "+" : "-") + (corners[o0 + 7] > 0 ? "+" : "-");
			if (!seen.add(key)) {
				continue;
			}
			for (int k = 0; k < 4; k++) {
				int src = pick[Math.min(k, d - 1)], c = id[src];
				System.arraycopy(corners, src * CORNER, out, m, CORNER);
				out[m] = sum[c * 3] / count[c];
				out[m + 1] = sum[c * 3 + 1] / count[c];
				out[m + 2] = sum[c * 3 + 2] / count[c];
				m += CORNER;
			}
		}
		return Arrays.copyOf(out, m);
	}

	/** Draws the quads of one kind: shaded corners lit by the world, or full bright ones facing up (so no world shading). */
	static void emit(DroneDepotRenderer.Geometry g, float[] corners, int light, boolean lit) {
		if (corners.length > 64 * CORNER) {
			float away = g.cameraDistance();
			if (away > FAR[1]) {
				corners = coarse(corners, away > FAR[2] ? 2 : 1);
			}
		}
		for (int o = 0; o + CORNER <= corners.length; o += CORNER) {
			if (lit) {
				g.meshVertex(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], 0, 1, 0, 0xFFFFFFFF, light);
			} else {
				int s = Math.round(corners[o + 8] * 255);
				g.meshVertex(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], corners[o + 5], corners[o + 6],
						corners[o + 7], 0xFF000000 | s << 16 | s << 8 | s, light);
			}
		}
	}
}
