package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The Holo Projector's head (tools/holo_projector.py writes it from tools/holo_head.py; {@code
 * assets/jugcraft/tower_mesh/holo_head.bin.gz}, drawn with textures/entity/holo_head.png): in head units (the chin near
 * y 0.07, the crown near 1.07, the face looking +z).
 *
 * <ul>
 * <li>The head: triangles over vertices that each carry a normal, a label (skin, lids, brows, lips, teeth, choker, hair,
 * mouth), a kind (surface or strand), a brightness, the lips' colour weight and a fade (the neck's skirt fading away).</li>
 * <li>The eyes: one unit sphere's triangles, placed at each eye's middle; they never turn (where they look is drawn on
 * them, the iris tile mapped round the gaze), so what of them can be seen never changes.</li>
 * <li>The hair's cards: strips of quads along each lock, the brows' too, with their lock texture.</li>
 * <li>What can be seen from where: for 162 directions, a bit for each primitive (the head's triangles, the eyes'
 * triangles, the cards' quads, in that order) set if it can be seen from that side. A primitive is drawn if it can be
 * seen from a direction within {@link #SEEN_WITHIN} of its own direction to the camera: so the hologram, see-through,
 * shows only its nearest surface, with no depth pre-pass.</li>
 * <li>The ring's letters: each glyph a solid (in letter heights), and the ring's glyphs and their places along it.</li>
 * </ul>
 */
final class HoloHeadMesh {
	private static final int MAGIC = 0x4A484844;
	/** The texture's size and its tiles (x, y, w, h), as tools/holo_projector.py lays them out. */
	static final int ATLAS_W = 512, ATLAS_H = 256;
	static final int[] WHITE = {0, 0, 16, 16}, IRIS = {288, 0, 128, 128}, STREAK = {424, 0, 64, 32}, MOTE = {424, 40, 16, 16};
	static final int[][] CARD_TILES = {{20, 0, 40, 128}, {64, 0, 40, 128}, {108, 0, 40, 128}, {152, 0, 40, 128}, {196, 0, 40, 128},
			{240, 0, 40, 128}};
	/** The iris tile reaches 60 degrees from the gaze at its edge. */
	static final float IRIS_TILE_DEG = 60;
	/** The lock textures' brightness is stored divided by this. */
	static final float CARD_BRIGHT = 1.4f;
	/** Labels. */
	static final int SKIN = 0, LIDS = 1, BROWS = 2, LIPS = 3, TEETH = 4, CHOKER = 5, HAIR = 6, MOUTH = 7;
	/** Card groups: the swept-back locks, the bun's, its loose ends, the bangs, the side locks, the brows. */
	static final int BROW_CARDS = 5;
	static final double SEEN_WITHIN = Math.toRadians(15);
	private static final int CUBE = 24;

	// the head
	final float[] v;
	final float[] n;
	final byte[] label;
	final byte[] kind;
	final float[] shade, lip, fade;
	final int[] tri;
	final int triCount;
	// the eyes
	final float eyeR;
	final float[][] eyes;
	final float[] eyeV;
	final int[] eyeTri;
	final int eyeTriCount;
	// the cards: per card its group, texture variant, first point and point count; per point left, right, normal
	final int[] cardGroup, cardVar, cardStart, cardLen;
	final float[] cardL, cardR, cardN;
	final int quadCount;
	// seen from where
	final float[] dirs;
	final long[][] seen;
	final int prims;
	/** Each primitive's middle (head units), in the bits' order. */
	final float[] centre;
	/** For each cube cell (6 faces, CUBE x CUBE), the directions near enough to its middle to matter. */
	private final int[][] near;
	// the letters
	final float[][] glyphV;
	final int[][] glyphTri;
	final float[] glyphAdvance;
	final int[] ringGlyph;
	final float[] ringX;
	final float ringLength;

	private HoloHeadMesh(DataInputStream in) throws IOException {
		if (in.readInt() != MAGIC || in.readInt() != 1) {
			throw new IOException("not a holo head");
		}
		int nv = in.readInt();
		v = new float[nv * 3];
		n = new float[nv * 3];
		label = new byte[nv];
		kind = new byte[nv];
		shade = new float[nv];
		lip = new float[nv];
		fade = new float[nv];
		for (int i = 0; i < nv; i++) {
			v[i * 3] = in.readFloat();
			v[i * 3 + 1] = in.readFloat();
			v[i * 3 + 2] = in.readFloat();
			n[i * 3] = in.readByte() / 127f;
			n[i * 3 + 1] = in.readByte() / 127f;
			n[i * 3 + 2] = in.readByte() / 127f;
			label[i] = in.readByte();
			kind[i] = in.readByte();
			shade[i] = in.readUnsignedByte() / 100f;
			lip[i] = in.readUnsignedByte() / 255f;
			fade[i] = in.readUnsignedByte() / 255f;
		}
		triCount = in.readInt();
		tri = readShorts(in, triCount * 3);
		eyeR = in.readFloat();
		eyes = new float[2][3];
		for (float[] e : eyes) {
			e[0] = in.readFloat();
			e[1] = in.readFloat();
			e[2] = in.readFloat();
		}
		int nev = in.readInt();
		eyeV = new float[nev * 3];
		for (int i = 0; i < nev * 3; i++) {
			eyeV[i] = in.readFloat();
		}
		eyeTriCount = in.readInt();
		eyeTri = readShorts(in, eyeTriCount * 3);
		int cards = in.readInt();
		cardGroup = new int[cards];
		cardVar = new int[cards];
		cardStart = new int[cards];
		cardLen = new int[cards];
		List<float[]> points = new ArrayList<>();
		int quads = 0;
		for (int c = 0; c < cards; c++) {
			cardGroup[c] = in.readUnsignedByte();
			cardVar[c] = in.readUnsignedByte();
			cardLen[c] = in.readUnsignedShort();
			cardStart[c] = points.size();
			quads += cardLen[c] - 1;
			for (int s = 0; s < cardLen[c]; s++) {
				float[] p = new float[9];
				for (int k = 0; k < 6; k++) {
					p[k] = in.readFloat();
				}
				for (int k = 6; k < 9; k++) {
					p[k] = in.readByte() / 127f;
				}
				points.add(p);
			}
		}
		quadCount = quads;
		cardL = new float[points.size() * 3];
		cardR = new float[points.size() * 3];
		cardN = new float[points.size() * 3];
		for (int i = 0; i < points.size(); i++) {
			float[] p = points.get(i);
			System.arraycopy(p, 0, cardL, i * 3, 3);
			System.arraycopy(p, 3, cardR, i * 3, 3);
			System.arraycopy(p, 6, cardN, i * 3, 3);
		}
		int nd = in.readInt();
		dirs = new float[nd * 3];
		for (int i = 0; i < nd * 3; i++) {
			dirs[i] = in.readFloat();
		}
		prims = in.readInt();
		if (prims != triCount + 2 * eyeTriCount + quadCount) {
			throw new IOException("the seen-from bits do not match the primitives: " + prims);
		}
		int words = (prims + 63) / 64;
		seen = new long[nd][words];
		for (long[] row : seen) {
			for (int w = 0; w < words; w++) {
				row[w] = in.readLong();
			}
		}
		int ng = in.readInt();
		glyphV = new float[ng][];
		glyphTri = new int[ng][];
		glyphAdvance = new float[ng];
		for (int g = 0; g < ng; g++) {
			in.readUnsignedShort();
			glyphAdvance[g] = in.readFloat();
			int gv = in.readInt();
			glyphV[g] = new float[gv * 3];
			for (int i = 0; i < gv * 3; i++) {
				glyphV[g][i] = in.readFloat();
			}
			int gt = in.readInt();
			glyphTri[g] = readShorts(in, gt * 3);
		}
		int items = in.readInt();
		ringGlyph = new int[items];
		ringX = new float[items];
		for (int i = 0; i < items; i++) {
			ringGlyph[i] = in.readUnsignedShort();
			ringX[i] = in.readFloat();
		}
		ringLength = in.readFloat();
		centre = centres();
		near = cube();
	}

	private static int[] readShorts(DataInputStream in, int count) throws IOException {
		int[] out = new int[count];
		for (int i = 0; i < count; i++) {
			out[i] = in.readUnsignedShort();
		}
		return out;
	}

	/** Each primitive's middle, in the order of the bits. */
	private float[] centres() {
		float[] c = new float[prims * 3];
		int p = 0;
		for (int t = 0; t < triCount; t++, p++) {
			for (int k = 0; k < 3; k++) {
				int i = tri[t * 3 + k] * 3;
				c[p * 3] += v[i] / 3;
				c[p * 3 + 1] += v[i + 1] / 3;
				c[p * 3 + 2] += v[i + 2] / 3;
			}
		}
		for (float[] e : eyes) {
			for (int t = 0; t < eyeTriCount; t++, p++) {
				for (int k = 0; k < 3; k++) {
					int i = eyeTri[t * 3 + k] * 3;
					c[p * 3] += (e[0] + eyeV[i] * eyeR) / 3;
					c[p * 3 + 1] += (e[1] + eyeV[i + 1] * eyeR) / 3;
					c[p * 3 + 2] += (e[2] + eyeV[i + 2] * eyeR) / 3;
				}
			}
		}
		for (int card = 0; card < cardGroup.length; card++) {
			for (int s = 0; s < cardLen[card] - 1; s++, p++) {
				int a = (cardStart[card] + s) * 3, b = a + 3;
				for (int k = 0; k < 3; k++) {
					c[p * 3 + k] = (cardL[a + k] + cardR[a + k] + cardL[b + k] + cardR[b + k]) / 4;
				}
			}
		}
		return c;
	}

	private static int face(float x, float y, float z) {
		float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
		return ax >= ay && ax >= az ? (x > 0 ? 0 : 1) : ay >= az ? (y > 0 ? 2 : 3) : (z > 0 ? 4 : 5);
	}

	/** The cube cell a direction falls in. */
	private static int cell(float x, float y, float z) {
		int f = face(x, y, z);
		float u, w, m;
		switch (f) {
			case 0, 1 -> { m = Math.abs(x); u = y / m; w = z / m; }
			case 2, 3 -> { m = Math.abs(y); u = x / m; w = z / m; }
			default -> { m = Math.abs(z); u = x / m; w = y / m; }
		}
		int i = Math.min(CUBE - 1, Math.max(0, (int) ((u + 1) / 2 * CUBE)));
		int j = Math.min(CUBE - 1, Math.max(0, (int) ((w + 1) / 2 * CUBE)));
		return (f * CUBE + i) * CUBE + j;
	}

	private int[][] cube() {
		int[][] out = new int[6 * CUBE * CUBE][];
		double reach = Math.cos(SEEN_WITHIN + Math.toRadians(5));
		int nd = dirs.length / 3;
		for (int f = 0; f < 6; f++) {
			for (int i = 0; i < CUBE; i++) {
				for (int j = 0; j < CUBE; j++) {
					float u = (i + 0.5f) / CUBE * 2 - 1, w = (j + 0.5f) / CUBE * 2 - 1, s = (f % 2 == 0) ? 1 : -1;
					float x, y, z;
					switch (f / 2) {
						case 0 -> { x = s; y = u; z = w; }
						case 1 -> { x = u; y = s; z = w; }
						default -> { x = u; y = w; z = s; }
					}
					float l = (float) Math.sqrt(x * x + y * y + z * z);
					x /= l;
					y /= l;
					z /= l;
					List<Integer> list = new ArrayList<>();
					int best = 0;
					double bestDot = -2;
					for (int d = 0; d < nd; d++) {
						double dot = x * dirs[d * 3] + y * dirs[d * 3 + 1] + z * dirs[d * 3 + 2];
						if (dot >= reach) {
							list.add(d);
						}
						if (dot > bestDot) {
							bestDot = dot;
							best = d;
						}
					}
					if (!list.contains(best)) {
						list.add(best);
					}
					out[(f * CUBE + i) * CUBE + j] = list.stream().mapToInt(Integer::intValue).toArray();
				}
			}
		}
		return out;
	}

	/** Whether primitive {@code p} can be seen from head-space point (cx, cy, cz) (the camera). */
	boolean seen(int p, float cx, float cy, float cz) {
		float x = cx - centre[p * 3], y = cy - centre[p * 3 + 1], z = cz - centre[p * 3 + 2];
		float l = (float) Math.sqrt(x * x + y * y + z * z);
		if (l < 1e-6f) {
			return true;
		}
		x /= l;
		y /= l;
		z /= l;
		double within = Math.cos(SEEN_WITHIN);
		long bit = 1L << (p & 63);
		int word = p >> 6;
		for (int d : near[cell(x, y, z)]) {
			if (x * dirs[d * 3] + y * dirs[d * 3 + 1] + z * dirs[d * 3 + 2] >= within && (seen[d][word] & bit) != 0) {
				return true;
			}
		}
		return false;
	}

	/** The first primitive of each eye's triangles, and of the cards' quads. */
	int eyeBase(int eye) {
		return triCount + eye * eyeTriCount;
	}

	int cardBase() {
		return triCount + 2 * eyeTriCount;
	}

	private static Optional<HoloHeadMesh> loaded;

	/** The head, or null if it could not be read. */
	static HoloHeadMesh get() {
		if (loaded == null) {
			loaded = Optional.ofNullable(load());
		}
		return loaded.orElse(null);
	}

	private static HoloHeadMesh load() {
		Identifier DATA = Jugcraft.id("tower_mesh/holo_head.bin.gz");
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DATA);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.error("Missing holo head {}", DATA);
				return null;
			}
			try (InputStream raw = resource.get().open(); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
				return new HoloHeadMesh(in);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the holo head {}", DATA, e);
			return null;
		}
	}

	/** Reads the head from a stream (the render checks and the offline harness). */
	static HoloHeadMesh read(InputStream raw) throws IOException {
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
			return new HoloHeadMesh(in);
		}
	}
}
