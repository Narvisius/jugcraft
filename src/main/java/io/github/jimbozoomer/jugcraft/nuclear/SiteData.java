package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A compiled site (towers and pump houses build: tools/water_works.py): its static geometry by block (quads the
 * client bakes into the chunk meshes), its collision boxes, which cells are under water or climbable, its live groups
 * (what each state the game drives draws, and the collision of a door's leaf), click zones and LED displays. Read from
 * the mod's own jar (assets/jugcraft/sites/&lt;key&gt;.bin.gz) on both sides; a dedicated server skips the quads.
 *
 * <p>Coordinates are the site's own (blocks, its origin on the grass); a cell is a block of it.
 */
public final class SiteData {
	/** Whether quads are kept (the client sets this before any site is read). */
	public static volatile boolean CLIENT = false;
	private static final Map<String, SiteData> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();

	public final String key;
	public final String[] sprites;
	/** Per sprite: 0 solid, 1 cutout, 2 translucent. */
	public final byte[] layers;
	private final Long2IntOpenHashMap index = new Long2IntOpenHashMap();
	private final int[] flags;
	private final int[] quadStart, quadCount;
	/** Per quad: sprite, lit, normal (x, y, z bytes). */
	private final short[] quadSprite;
	private final byte[] quadLit;
	private final byte[] quadNormal;
	/** Per quad: 4 corners of x, y, z (1/2048 block, from the cell's corner), u, v (1/65535 of the sprite). */
	private final short[] quadVerts;
	private final int[] boxStart;
	private final byte[] boxes;
	private final VoxelShape[] shapes;
	public final List<Group> groups = new ArrayList<>();
	public final JsonObject live;
	public final List<Variable> variables = new ArrayList<>();
	public final List<Zone> zones = new ArrayList<>();
	/** The anchor's cell. */
	public final int ax, ay, az;
	/** The cells' extent (site coordinates). */
	public final int minX, minY, minZ, maxX, maxY, maxZ;
	/** The tower shell this site has (its concrete worked out from the profile), or null. */
	public final @Nullable TowerShell shell;

	public static final int WATER = 1, LADDER = 2;

	/**
	 * A state the game drives: its name, its values (as the compiler wrote them), and how they are read. A joint one (a
	 * figure two pumps change together, as a train's flow) has members: the variables whose values make its value,
	 * the first member's the lowest digit (mixed radix, each member's own count of values).
	 */
	public record Variable(String name, String[] values, String kind, int @Nullable [] members) {
		/** A joint variable's value from its members' values (memberValue: a variable's index to its value now). */
		public int joint(java.util.function.IntUnaryOperator memberValue, List<Variable> all) {
			int out = 0, scale = 1;
			for (int m : members) {
				int n = all.get(m).values().length;
				out += Math.max(0, Math.min(n - 1, memberValue.applyAsInt(m))) * scale;
				scale *= n;
			}
			return out;
		}

		/** The value nearest {@code x} (a meter's needle). */
		public int nearest(double x) {
			int best = 0;
			double d = Double.MAX_VALUE;
			for (int i = 0; i < values.length; i++) {
				try {
					double e = Math.abs(Double.parseDouble(values[i]) - x);
					if (e < d) {
						d = e;
						best = i;
					}
				} catch (NumberFormatException ignored) {
				}
			}
			return best;
		}

		public int indexOf(String value) {
			for (int i = 0; i < values.length; i++) {
				if (values[i].equals(value)) return i;
			}
			return 0;
		}
	}

	/** What one value of a variable draws: quads (site coordinates) and, for a door, its collision. */
	public record Group(String variable, int value, float[] verts, short[] sprite, byte[] lit, byte[] normal, float[] boxes) {
		public int quads() {
			return sprite.length;
		}
	}

	/** A click zone: its name and box (site coordinates). */
	public record Zone(String name, AABB box) {
	}

	private SiteData(String key, ByteBuffer in, boolean keepQuads) {
		this.key = key;
		byte[] magic = new byte[4];
		in.get(magic);
		if (!"JSIT".equals(new String(magic, StandardCharsets.US_ASCII))) {
			throw new IllegalStateException("not a site file");
		}
		in.getShort();
		int ns = in.getShort() & 0xFFFF;
		sprites = new String[ns];
		layers = new byte[ns];
		for (int i = 0; i < ns; i++) {
			sprites[i] = string(in);
			layers[i] = in.get();
		}
		int nc = in.getInt();
		flags = new int[nc];
		quadStart = new int[nc];
		quadCount = new int[nc];
		boxStart = new int[nc + 1];
		shapes = new VoxelShape[nc];
		// First pass sizes (the buffer is read twice: once to count, once to fill).
		int mark = in.position();
		int totalQuads = 0, totalBoxes = 0;
		for (int c = 0; c < nc; c++) {
			in.position(in.position() + 7);
			int nq = in.getInt();
			int nb = in.get() & 0xFF;
			totalQuads += nq;
			totalBoxes += nb;
			in.position(in.position() + nq * (6 + 4 * 10) + nb * 6);
		}
		in.position(mark);
		int kq = keepQuads ? totalQuads : 0;
		quadSprite = new short[kq];
		quadLit = new byte[kq];
		quadNormal = new byte[kq * 3];
		quadVerts = new short[kq * 20];
		boxes = new byte[totalBoxes * 6];
		int q = 0, bxi = 0;
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE,
				z1 = Integer.MIN_VALUE;
		for (int c = 0; c < nc; c++) {
			int x = in.getShort(), y = in.getShort(), z = in.getShort();
			x0 = Math.min(x0, x);
			y0 = Math.min(y0, y);
			z0 = Math.min(z0, z);
			x1 = Math.max(x1, x);
			y1 = Math.max(y1, y);
			z1 = Math.max(z1, z);
			index.put(key(x, y, z), c);
			flags[c] = in.get() & 0xFF;
			int nq = in.getInt();
			int nb = in.get() & 0xFF;
			quadStart[c] = q;
			quadCount[c] = keepQuads ? nq : 0;
			for (int k = 0; k < nq; k++) {
				if (!keepQuads) {
					in.position(in.position() + 6 + 40);
					continue;
				}
				quadSprite[q] = in.getShort();
				quadLit[q] = in.get();
				quadNormal[q * 3] = in.get();
				quadNormal[q * 3 + 1] = in.get();
				quadNormal[q * 3 + 2] = in.get();
				for (int v = 0; v < 20; v++) {
					quadVerts[q * 20 + v] = in.getShort();
				}
				q++;
			}
			boxStart[c] = bxi;
			for (int k = 0; k < nb * 6; k++) {
				boxes[bxi++] = in.get();
			}
		}
		boxStart[nc] = bxi;
		int ng = in.getInt();
		for (int g = 0; g < ng; g++) {
			String name = string(in);
			int value = in.getShort() & 0xFFFF;
			int nq = in.getInt(), nb = in.getInt();
			float[] verts = new float[nq * 20];
			short[] sprite = new short[nq];
			byte[] lit = new byte[nq];
			byte[] normal = new byte[nq * 3];
			for (int k = 0; k < nq; k++) {
				sprite[k] = in.getShort();
				lit[k] = in.get();
				normal[k * 3] = in.get();
				normal[k * 3 + 1] = in.get();
				normal[k * 3 + 2] = in.get();
				for (int v = 0; v < 4; v++) {
					verts[k * 20 + v * 5] = in.getFloat();
					verts[k * 20 + v * 5 + 1] = in.getFloat();
					verts[k * 20 + v * 5 + 2] = in.getFloat();
					verts[k * 20 + v * 5 + 3] = (in.getShort() & 0xFFFF) / 65535F;
					verts[k * 20 + v * 5 + 4] = (in.getShort() & 0xFFFF) / 65535F;
				}
			}
			float[] bx = new float[nb * 6];
			for (int k = 0; k < bx.length; k++) {
				bx[k] = in.getFloat();
			}
			groups.add(new Group(name, value, keepQuads ? verts : new float[0], keepQuads ? sprite : new short[0], lit, normal, bx));
		}
		int jl = in.getInt();
		byte[] js = new byte[jl];
		in.get(js);
		live = JsonParser.parseString(new String(js, StandardCharsets.UTF_8)).getAsJsonObject();
		JsonArray anchor = live.getAsJsonArray("anchor");
		ax = anchor.get(0).getAsInt();
		ay = anchor.get(1).getAsInt();
		az = anchor.get(2).getAsInt();
		Map<String, Integer> named = new HashMap<>();
		for (JsonElement e : live.getAsJsonArray("variables")) {
			JsonObject v = e.getAsJsonObject();
			JsonArray vals = v.getAsJsonArray("values");
			String[] values = new String[vals.size()];
			for (int i = 0; i < values.length; i++) values[i] = vals.get(i).getAsString();
			int[] members = null;
			if (v.has("members")) {
				JsonArray ms = v.getAsJsonArray("members");
				members = new int[ms.size()];
				for (int i = 0; i < members.length; i++) members[i] = named.getOrDefault(ms.get(i).getAsString(), 0);
			}
			named.put(v.get("name").getAsString(), variables.size());
			variables.add(new Variable(v.get("name").getAsString(), values, v.get("kind").getAsString(), members));
		}
		for (JsonElement e : live.getAsJsonArray("zones")) {
			JsonObject z = e.getAsJsonObject();
			JsonArray b = z.getAsJsonArray("box");
			zones.add(new Zone(z.get("name").getAsString(), new AABB(b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(),
					b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble())));
		}
		shell = live.has("shell") ? TowerShell.MEDIUM : null;
		if (shell != null) {
			x0 = Math.min(x0, -54);
			z0 = Math.min(z0, -54);
			x1 = Math.max(x1, 54);
			z1 = Math.max(z1, 54);
			y0 = Math.min(y0, -3);
			y1 = Math.max(y1, (int) Math.ceil(shell.yCtop) + 1);
		}
		minX = x0;
		minY = y0;
		minZ = z0;
		maxX = x1;
		maxY = y1;
		maxZ = z1;
	}

	private static String string(ByteBuffer in) {
		int n = in.getShort() & 0xFFFF;
		byte[] b = new byte[n];
		in.get(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	public static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
	}

	/** The site, read once from the jar; null if there is no such site. */
	public static @Nullable SiteData get(String key) {
		SiteData d = CACHE.get(key);
		if (d != null || MISSING.containsKey(key)) {
			return d;
		}
		synchronized (CACHE) {
			d = CACHE.get(key);
			if (d != null) return d;
			try (InputStream raw = SiteData.class.getResourceAsStream("/assets/jugcraft/sites/" + key + ".bin.gz")) {
				if (raw == null) {
					MISSING.put(key, true);
					return null;
				}
				byte[] bytes;
				try (DataInputStream in = new DataInputStream(new GZIPInputStream(raw, 1 << 16))) {
					bytes = in.readAllBytes();
				}
				d = new SiteData(key, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN), CLIENT);
				CACHE.put(key, d);
				return d;
			} catch (IOException | RuntimeException e) {
				Jugcraft.LOGGER.error("Could not read site {}", key, e);
				MISSING.put(key, true);
				return null;
			}
		}
	}

	/** The cell's index, or -1 if the site has nothing there (the shell's cells are not listed). */
	public int cell(int x, int y, int z) {
		return index.getOrDefault(key(x, y, z), -1);
	}

	public int flags(int cell) {
		return cell < 0 ? 0 : flags[cell];
	}

	public int quadStart(int cell) {
		return quadStart[cell];
	}

	public int quadCount(int cell) {
		return cell < 0 ? 0 : quadCount[cell];
	}

	public int sprite(int quad) {
		return quadSprite[quad] & 0xFFFF;
	}

	public boolean lit(int quad) {
		return quadLit[quad] != 0;
	}

	public float normal(int quad, int axis) {
		return quadNormal[quad * 3 + axis] / 127F;
	}

	/** A quad's corner: x, y, z (blocks from the cell's corner), u, v (0..1 of its sprite). */
	public float vert(int quad, int corner, int part) {
		short s = quadVerts[quad * 20 + corner * 5 + part];
		return part < 3 ? s / 2048F : (s & 0xFFFF) / 65535F;
	}

	/** The cell's own collision (site frame, not turned), the shell's added. */
	public VoxelShape shape(int cell, int x, int y, int z) {
		VoxelShape s = cell >= 0 ? shapes[cell] : null;
		if (s == null) {
			s = Shapes.empty();
			if (cell >= 0) {
				for (int i = boxStart[cell]; i < boxStart[cell + 1]; i += 6) {
					s = Shapes.joinUnoptimized(s, Shapes.box((boxes[i] & 0xFF) / 16.0, (boxes[i + 1] & 0xFF) / 16.0, (boxes[i + 2] & 0xFF) / 16.0,
							(boxes[i + 3] & 0xFF) / 16.0, (boxes[i + 4] & 0xFF) / 16.0, (boxes[i + 5] & 0xFF) / 16.0), BooleanOp.OR);
				}
				if (shell != null) {
					s = Shapes.joinUnoptimized(s, shell.shape(x, y, z), BooleanOp.OR);
				}
				s = s.optimize();
				shapes[cell] = s;
			} else if (shell != null) {
				s = shell.shape(x, y, z);
			}
		}
		return s;
	}

	public int variable(String name) {
		for (int i = 0; i < variables.size(); i++) {
			if (variables.get(i).name().equals(name)) return i;
		}
		return -1;
	}

	private int @Nullable [] solid;

	/** The variables some of whose groups collide (a door's leaf), by index. */
	public int[] solidVariables() {
		int[] out = solid;
		if (out == null) {
			java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
			for (Group g : groups) {
				if (g.boxes().length > 0) {
					int i = variable(g.variable());
					if (i >= 0) set.add(i);
				}
			}
			out = set.stream().mapToInt(Integer::intValue).toArray();
			solid = out;
		}
		return out;
	}

	/** The groups by variable and value: [variable][value]. */
	public Group[][] groupTable() {
		Group[][] out = new Group[variables.size()][];
		for (int i = 0; i < out.length; i++) {
			out[i] = new Group[variables.get(i).values().length];
		}
		Map<String, Integer> vi = new HashMap<>();
		for (int i = 0; i < variables.size(); i++) vi.put(variables.get(i).name(), i);
		for (Group g : groups) {
			Integer i = vi.get(g.variable());
			if (i != null && g.value() < out[i].length) out[i][g.value()] = g;
		}
		return out;
	}
}
