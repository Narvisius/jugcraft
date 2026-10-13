package io.github.jimbozoomer.jugcraft.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A blueprint (format 1, {@code *.jugbp.json}; see docs/BLUEPRINT_FORMAT.md): a palette of block states and
 * the structure as bottom-to-top layers of text rows ('.' or ' ' = nothing required). {@code anchor} is where
 * the Survey Stake stands, in blueprint coordinates. Blueprints are drawn with the build extending north
 * (towards -z) from the stake; {@link #cells} turns them for placing.
 *
 * <p>The server keeps the library (the built-in blueprints shipped in the jar, and blueprints imported at a
 * Blueprint Table, saved with the world) and sends every blueprint to each client, so stakes only sync their
 * blueprint id. The two sides keep separate maps because singleplayer runs both in one game.
 */
public final class Blueprint {
	/** The blueprints shipped with Jugcraft (tools/blueprints.py BUILT_IN). */
	public static final List<String> BUILT_IN = List.of("drone_tower_foundation", "drone_tower_industrial_foundation", "arc_furnace", "small_church");
	/**
	 * The built-in blueprints too big to send over the network (the nuclear plants and their parts, from
	 * tools/nuclear_plants.py): each side reads them from its own copy of the jar when they are first wanted.
	 */
	public static final List<String> LARGE_BUILT_IN = largeBuiltIns();
	/** Limits for imported blueprints. */
	public static final int MAX_SIZE = 48;
	public static final int MAX_BLOCKS = 8192;
	/** Limits for built-in blueprints (a whole power plant). */
	public static final int BUILT_IN_MAX_SIZE = 512;
	public static final int BUILT_IN_MAX_BLOCKS = 1_500_000;
	public static final int MAX_CHARS = 262_144;
	private static final Set<Block> FORBIDDEN = Set.of(Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
			Blocks.STRUCTURE_BLOCK, Blocks.STRUCTURE_VOID, Blocks.JIGSAW, Blocks.BARRIER, Blocks.LIGHT, Blocks.BEDROCK, Blocks.SPAWNER,
			Blocks.TRIAL_SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME, Blocks.END_GATEWAY, Blocks.NETHER_PORTAL,
			Blocks.REINFORCED_DEEPSLATE, Blocks.TEST_BLOCK, Blocks.TEST_INSTANCE_BLOCK);

	private static final Map<String, Blueprint> SERVER = new LinkedHashMap<>();
	private static final Map<String, Blueprint> CLIENT = new LinkedHashMap<>();

	/** The large built-ins listed in the jar (data/jugcraft/blueprint/large.txt, one id a line). */
	private static List<String> largeBuiltIns() {
		try (InputStream in = Blueprint.class.getResourceAsStream("/data/jugcraft/blueprint/large.txt")) {
			if (in == null) {
				return List.of();
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
		} catch (java.io.IOException e) {
			return List.of();
		}
	}

	/**
	 * The blueprint item's colour in the inventory: a complete build of the mod's own, an individual structure or a
	 * whole set (blue); a partial structure, one part of a set such as a single cooling tower (green); or a player's
	 * import (red).
	 */
	public enum Category {
		/** Several parts making a whole, such as a complete power plant: {@code "kind": "set"}. */
		SET("set"),
		/** One structure on its own, not a part and not divided: the default ({@code "kind": "individual"}). */
		INDIVIDUAL("individual"),
		/** One part of a set, such as a single cooling tower: {@code "kind": "part"}. */
		PARTIAL("part");

		public final String id;

		Category(String id) {
			this.id = id;
		}
	}

	public enum Kind {
		COMPLETE("complete"), PART("part"), IMPORTED("imported");

		public final String id;

		Kind(String id) {
			this.id = id;
		}
	}

	/** One block of the blueprint, relative to the stake (already turned). */
	public record Cell(BlockPos offset, BlockState state) {
	}

	/** A blueprint that does not pass the checks; the message says why in plain words. */
	public static final class Invalid extends Exception {
		public Invalid(String message) {
			super(message);
		}
	}

	public final String id;
	public final String name;
	/** "built in" or "imported". */
	public final String source;
	/** The colour of the blueprint item: blue complete, green a part of a larger build, red imported. */
	public final Kind kind;
	/** Which list the Blueprint Table shows it in (imports have their own IMPORT tab). */
	public final Category category;
	/** The file's text, or "" for a large built-in (each side reads those from its own copy of the jar). */
	public final String json;
	public final int sizeX;
	public final int sizeY;
	public final int sizeZ;
	/** Where the stake stands, in the file's coordinates. */
	final int anchorX, anchorY, anchorZ;
	/**
	 * The cells, bottom layer first: offsets from the stake packed into one int each ({@link #pack}), and the index of
	 * each cell's block in {@link #palette}. Kept compact because a whole power plant is a third of a million blocks.
	 */
	private final int[] packed;
	private final short[] stateOf;
	private final BlockState[] palette;
	private final BlockState[][] turnedPalette = new BlockState[4][];
	/** The parts a set is made of, for the placement preview: id, offset of the part's corner from the stake, turn. */
	public final List<PartRef> parts;
	private final float[][] hull = new float[4][];

	/** A part of a set: the part blueprint, where its file's corner goes (from the stake, as drawn) and its quarter turns. */
	public record PartRef(String id, int x, int y, int z, int turns, int sizeX, int sizeY, int sizeZ) {
	}

	private Blueprint(String id, String name, String source, Kind kind, Category category, String json, int sizeX, int sizeY, int sizeZ,
			int anchorX, int anchorY, int anchorZ, int[] packed, short[] stateOf, BlockState[] palette, List<PartRef> parts) {
		this.id = id;
		this.name = name;
		this.source = source;
		this.kind = kind;
		this.category = category;
		this.json = json;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.anchorZ = anchorZ;
		this.packed = packed;
		this.stateOf = stateOf;
		this.palette = palette;
		this.parts = parts;
	}

	/** Offsets from the stake fit in 11 bits across (x, z: -1024..1023) and 10 bits up (y: -512..511). */
	static int pack(int x, int y, int z) {
		return ((x + 1024) << 21) | ((z + 1024) << 10) | (y + 512);
	}

	static int unpackX(int p) {
		return (p >>> 21) - 1024;
	}

	static int unpackZ(int p) {
		return ((p >>> 10) & 0x7FF) - 1024;
	}

	static int unpackY(int p) {
		return (p & 0x3FF) - 512;
	}

	// ------------------------------------------------------------------ the two libraries

	/** The blueprint with this id on the server (client false) or the client (client true), or null. */
	public static synchronized @Nullable Blueprint get(String id, boolean client) {
		Map<String, Blueprint> map = client ? CLIENT : SERVER;
		Blueprint found = map.get(id);
		if (found == null && LARGE_BUILT_IN.contains(id)) {
			// Large built-ins are read from this side's own jar the first time they are wanted.
			found = builtIn(id);
			if (found != null) {
				map.put(id, found);
			}
		}
		return found;
	}

	/** Every blueprint on one side, the large built-ins included (read from the jar if they were not yet). */
	public static synchronized List<Blueprint> all(boolean client) {
		for (String id : LARGE_BUILT_IN) {
			get(id, client);
		}
		return List.copyOf((client ? CLIENT : SERVER).values());
	}

	/** Every blueprint id on one side, without reading the large built-ins. */
	public static synchronized List<String> ids(boolean client) {
		java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>((client ? CLIENT : SERVER).keySet());
		out.addAll(LARGE_BUILT_IN);
		return List.copyOf(out);
	}

	/** The blueprints the server sends to players: everything but the large built-ins, which clients read themselves. */
	static synchronized List<Blueprint> synced() {
		return SERVER.values().stream().filter(b -> !b.local()).toList();
	}

	/** A large built-in: not sent over the network (each side reads it from its own jar). */
	public boolean local() {
		return LARGE_BUILT_IN.contains(id);
	}

	static synchronized void put(Blueprint blueprint, boolean client) {
		(client ? CLIENT : SERVER).put(blueprint.id, blueprint);
	}

	static synchronized void clear(boolean client) {
		(client ? CLIENT : SERVER).clear();
	}

	/** Loads a built-in blueprint from the jar. */
	static @Nullable Blueprint builtIn(String id) {
		String path = "/data/jugcraft/blueprint/" + id + ".jugbp.json";
		try (InputStream in = Blueprint.class.getResourceAsStream(path)) {
			if (in == null) {
				Jugcraft.LOGGER.error("Missing built-in blueprint {}", path);
				return null;
			}
			return parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8), "built in");
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Built-in blueprint {} is invalid: {}", id, e.getMessage());
			return null;
		}
	}

	// ------------------------------------------------------------------ reading and checking

	/**
	 * Reads and checks a blueprint; {@link Invalid} explains the first problem found.
	 *
	 * <p>Format 1 rows have one character a block. Format 2 rows are run-length coded ("12.3A" is twelve empty cells
	 * then three of A), and a built-in format 2 file may also list {@code parts}: other built-in blueprints placed
	 * inside it ({@code {"id", "at": [x, y, z], "turn": quarter turns clockwise}}), under its own layers.
	 */
	public static Blueprint parse(String id, String json, String source) throws Invalid {
		boolean builtIn = "built in".equals(source);
		if (json.length() > MAX_CHARS && !builtIn) {
			throw new Invalid("Too big: " + json.length() + " characters (the limit is " + MAX_CHARS + ")");
		}
		JsonObject root;
		try {
			root = JsonParser.parseString(json).getAsJsonObject();
		} catch (Exception e) {
			throw new Invalid("Not valid JSON: " + shortMessage(e));
		}
		int format = root.has("format") ? root.get("format").getAsInt() : 0;
		if (format != 1 && format != 2) {
			throw new Invalid("\"format\" must be 1 (or 2, with run-length rows)");
		}
		String name = root.has("name") ? root.get("name").getAsString().strip() : "";
		if (name.isEmpty() || name.length() > 48) {
			throw new Invalid("\"name\" is missing or longer than 48 characters");
		}
		if (!root.has("palette") || !root.get("palette").isJsonObject()) {
			throw new Invalid("\"palette\" is missing: it maps one character to a block, like {\"A\": \"minecraft:stone_bricks\"}");
		}
		Map<Character, BlockState> palette = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("palette").entrySet()) {
			if (entry.getKey().length() != 1 || entry.getKey().equals(".") || entry.getKey().equals(" ")
					|| (format == 2 && Character.isDigit(entry.getKey().charAt(0)))) {
				throw new Invalid("Palette key \"" + entry.getKey() + "\" must be one character (not '.', a space" + (format == 2 ? " or a digit)" : ")"));
			}
			String text = entry.getValue().getAsString();
			BlockState state;
			try {
				state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, text, false).blockState();
			} catch (Exception e) {
				throw new Invalid("Unknown block \"" + text + "\" in palette " + entry.getKey() + " (" + shortMessage(e) + ")");
			}
			if (FORBIDDEN.contains(state.getBlock())) {
				throw new Invalid("\"" + text + "\" (palette " + entry.getKey() + ") is not allowed in blueprints");
			}
			if (!state.isAir()) {
				palette.put(entry.getKey().charAt(0), state);
			}
		}
		// Cells in file coordinates; a later cell at the same place replaces an earlier one (layers go over parts).
		Map<Long, BlockState> cells = new LinkedHashMap<>();
		int sx = 0, sz = 0, sy = 0;
		List<PartRef> parts = new ArrayList<>();
		if (root.has("parts")) {
			if (!builtIn || format != 2) {
				throw new Invalid("Only the built-in blueprints can be made of parts");
			}
			Map<String, Blueprint> loaded = new HashMap<>();
			for (JsonElement element : root.getAsJsonArray("parts")) {
				JsonObject ref = element.getAsJsonObject();
				String partId = ref.get("id").getAsString();
				Blueprint part = loaded.containsKey(partId) ? loaded.get(partId) : builtIn(partId);
				loaded.put(partId, part);
				if (part == null) {
					throw new Invalid("Part \"" + partId + "\" is missing");
				}
				JsonArray at = ref.getAsJsonArray("at");
				int px = at.get(0).getAsInt(), py = at.get(1).getAsInt(), pz = at.get(2).getAsInt();
				int turns = ref.has("turn") ? Math.floorMod(ref.get("turn").getAsInt(), 4) : 0;
				int w = part.sizeX, d = part.sizeZ;
				for (int i = 0; i < part.size(); i++) {
					int fx = unpackX(part.packed[i]) + part.anchorX, fy = unpackY(part.packed[i]) + part.anchorY, fz = unpackZ(part.packed[i]) + part.anchorZ;
					BlockState state = part.palette[part.stateOf[i]];
					int cw = w, cd = d;
					for (int t = 0; t < turns; t++) {
						// A quarter turn clockwise (seen from above) inside the part's footprint: (x, z) -> (depth-1-z, x).
						int nx = cd - 1 - fz;
						fz = fx;
						fx = nx;
						int swap = cw;
						cw = cd;
						cd = swap;
						state = state.rotate(Rotation.CLOCKWISE_90);
					}
					cells.put(key(fx + px, fy + py, fz + pz), state);
				}
				int fw = turns % 2 == 0 ? w : d, fd = turns % 2 == 0 ? d : w;
				parts.add(new PartRef(partId, px, py, pz, turns, fw, part.sizeY, fd));
				sx = Math.max(sx, px + fw);
				sy = Math.max(sy, py + part.sizeY);
				sz = Math.max(sz, pz + fd);
			}
		}
		if (!root.has("layers") || !root.get("layers").isJsonArray()) {
			if (parts.isEmpty()) {
				throw new Invalid("\"layers\" is missing: a list of layers, bottom first, each a list of text rows");
			}
		} else {
			JsonArray layers = root.getAsJsonArray("layers");
			sy = Math.max(sy, layers.size());
			for (int y = 0; y < layers.size(); y++) {
				JsonArray rows = layers.get(y).getAsJsonArray();
				sz = Math.max(sz, rows.size());
				for (int z = 0; z < rows.size(); z++) {
					String row = rows.get(z).getAsString();
					int x = 0;
					for (int i = 0; i < row.length(); i++) {
						int count = 1;
						if (format == 2 && Character.isDigit(row.charAt(i))) {
							int j = i;
							while (j < row.length() && Character.isDigit(row.charAt(j))) {
								j++;
							}
							if (j >= row.length() || j - i > 4) {
								throw new Invalid("Layer " + y + ", row " + z + ": a count must be followed by one block character");
							}
							count = Integer.parseInt(row.substring(i, j));
							i = j;
						}
						char c = row.charAt(i);
						if (c == '.' || c == ' ') {
							x += count;
							continue;
						}
						BlockState state = palette.get(c);
						if (state == null && !root.getAsJsonObject("palette").has(String.valueOf(c))) {
							throw new Invalid("Layer " + y + ", row " + z + " uses '" + c + "', which is not in the palette");
						}
						for (int k = 0; k < count; k++, x++) {
							if (state != null) {
								cells.put(key(x, y, z), state);
							} else {
								cells.remove(key(x, y, z));
							}
						}
					}
					sx = Math.max(sx, x);
				}
			}
		}
		if (root.has("size") && root.get("size").isJsonArray()) {
			// The declared size counts too (empty space at the edges, which the rows leave out).
			JsonArray size = root.getAsJsonArray("size");
			sx = Math.max(sx, size.get(0).getAsInt());
			sy = Math.max(sy, size.get(1).getAsInt());
			sz = Math.max(sz, size.get(2).getAsInt());
		}
		int maxSize = builtIn ? BUILT_IN_MAX_SIZE : MAX_SIZE;
		int maxBlocks = builtIn ? BUILT_IN_MAX_BLOCKS : MAX_BLOCKS;
		if (sx > maxSize || sy > maxSize || sz > maxSize) {
			throw new Invalid("Too large: " + sx + " x " + sy + " x " + sz + " (at most " + maxSize + " each way)");
		}
		if (cells.isEmpty()) {
			throw new Invalid("The blueprint has no blocks");
		}
		if (cells.size() > maxBlocks) {
			throw new Invalid("Too many blocks: " + cells.size() + " (at most " + maxBlocks + ")");
		}
		int ax = sx / 2, ay = 0, az = sz + 1;
		if (root.has("anchor")) {
			JsonArray anchor = root.getAsJsonArray("anchor");
			ax = anchor.get(0).getAsInt();
			ay = anchor.get(1).getAsInt();
			az = anchor.get(2).getAsInt();
		}
		// Bottom layer first (drones and holograms work up from the lowest unfinished layer).
		List<Map.Entry<Long, BlockState>> sorted = new ArrayList<>(cells.entrySet());
		sorted.sort(java.util.Comparator.comparingInt(e -> keyY(e.getKey())));
		int[] packed = new int[sorted.size()];
		short[] stateOf = new short[sorted.size()];
		Map<BlockState, Integer> index = new LinkedHashMap<>();
		for (int i = 0; i < sorted.size(); i++) {
			long k = sorted.get(i).getKey();
			int ox = keyX(k) - ax, oy = keyY(k) - ay, oz = keyZ(k) - az;
			if (ox == 0 && oy == 0 && oz == 0) {
				throw new Invalid("The anchor (where the stake stands) is on a block of the blueprint; put it in front of the build");
			}
			if (ox < -1024 || ox > 1023 || oz < -1024 || oz > 1023 || oy < -512 || oy > 511) {
				throw new Invalid("The anchor is too far from the build");
			}
			packed[i] = pack(ox, oy, oz);
			Integer n = index.computeIfAbsent(sorted.get(i).getValue(), s -> index.size());
			if (n > Short.MAX_VALUE) {
				throw new Invalid("Too many different blocks");
			}
			stateOf[i] = (short) (int) n;
		}
		String declared = root.has("kind") ? root.get("kind").getAsString() : "individual";
		Category category = null;
		for (Category c : Category.values()) {
			if (c.id.equals(declared)) {
				category = c;
			}
		}
		if (category == null) {
			throw new Invalid("\"kind\" must be \"individual\" (one structure), \"set\" (several parts making a whole) or \"part\" (one part of a set)");
		}
		Kind kind = "imported".equals(source) ? Kind.IMPORTED : category == Category.PARTIAL ? Kind.PART : Kind.COMPLETE;
		String kept = builtIn && LARGE_BUILT_IN.contains(id) ? "" : json;
		return new Blueprint(id, name, source, kind, category, kept, sx, sy, sz, ax, ay, az, packed, stateOf,
				index.keySet().toArray(BlockState[]::new), List.copyOf(parts));
	}

	private static long key(int x, int y, int z) {
		return ((long) (x + 2048) << 40) | ((long) (z + 2048) << 20) | (y + 2048);
	}

	private static int keyX(long k) {
		return (int) (k >>> 40) - 2048;
	}

	private static int keyZ(long k) {
		return (int) ((k >>> 20) & 0xFFFFF) - 2048;
	}

	private static int keyY(long k) {
		return (int) (k & 0xFFFFF) - 2048;
	}

	private static String shortMessage(Exception e) {
		String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		return message.length() > 120 ? message.substring(0, 120) + "..." : message;
	}

	// ------------------------------------------------------------------ use

	public int size() {
		return packed.length;
	}

	/** Where the stake stands, in the file's coordinates (x across, y up, z rows). */
	public int anchorX() {
		return anchorX;
	}

	public int anchorY() {
		return anchorY;
	}

	public int anchorZ() {
		return anchorZ;
	}

	/** Bigger than an ordinary blueprint: drones, holograms and the preview work on it a few layers at a time. */
	public boolean large() {
		return packed.length > MAX_BLOCKS || sizeX > MAX_SIZE || sizeY > MAX_SIZE || sizeZ > MAX_SIZE;
	}

	/** Cell {@code i}'s offset from the stake, turned by {@code rotation}. Cells go bottom layer first. */
	public BlockPos offset(int i, Rotation rotation) {
		return new BlockPos(unpackX(packed[i]), unpackY(packed[i]), unpackZ(packed[i])).rotate(rotation);
	}

	/** Cell {@code i}'s height above (or below) the stake; the same however it is turned. */
	public int offsetY(int i) {
		return unpackY(packed[i]);
	}

	/** Cell {@code i}'s block, turned by {@code rotation}. */
	public BlockState state(int i, Rotation rotation) {
		BlockState[] turned = turnedPalette[rotation.ordinal()];
		if (turned == null) {
			turned = new BlockState[palette.length];
			for (int k = 0; k < palette.length; k++) {
				turned[k] = palette[k].rotate(rotation);
			}
			turnedPalette[rotation.ordinal()] = turned;
		}
		return turned[stateOf[i]];
	}

	/** The highest cell above the stake. */
	public int top() {
		return packed.length == 0 ? 0 : unpackY(packed[packed.length - 1]);
	}

	/** The cells turned by {@code rotation} about the stake: north-facing placement is the file as drawn. */
	public List<Cell> cells(Rotation rotation) {
		List<Cell> out = new ArrayList<>(packed.length);
		for (int i = 0; i < packed.length; i++) {
			out.add(new Cell(offset(i, rotation), state(i, rotation)));
		}
		return out;
	}

	/** Block counts, most first (for the library's bill of materials). */
	public Map<Block, Integer> materials() {
		int[] counts = new int[palette.length];
		for (short s : stateOf) {
			counts[s]++;
		}
		Map<Block, Integer> merged = new HashMap<>();
		for (int k = 0; k < palette.length; k++) {
			merged.merge(palette[k].getBlock(), counts[k], Integer::sum);
		}
		Map<Block, Integer> sorted = new LinkedHashMap<>();
		merged.entrySet().stream().sorted(Map.Entry.<Block, Integer>comparingByValue().reversed())
				.forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	/** The cells as drawn (not turned), for previews. */
	public Collection<Cell> rawCells() {
		return cells(Rotation.NONE);
	}

	/**
	 * A rough outer skin of the build for the placement preview of a large blueprint: the faces between filled and
	 * empty boxes of a few blocks each, as {x0, y0, z0, x1, y1, z1, up} quads from the stake (up = 1 for a top face).
	 * A hollow cooling tower keeps its shape; a solid hall is a box.
	 */
	public synchronized float[] hull(Rotation rotation) {
		float[] cached = hull[rotation.ordinal()];
		if (cached != null) {
			return cached;
		}
		int step = Math.max(4, (Math.max(sizeX, Math.max(sizeY, sizeZ)) + 47) / 48);
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		BlockPos[] at = new BlockPos[packed.length];
		for (int i = 0; i < packed.length; i++) {
			BlockPos p = offset(i, rotation);
			at[i] = p;
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
			maxZ = Math.max(maxZ, p.getZ());
		}
		int nx = (maxX - minX) / step + 1, ny = (maxY - minY) / step + 1, nz = (maxZ - minZ) / step + 1;
		int[] count = new int[nx * ny * nz];
		for (BlockPos p : at) {
			count[(((p.getX() - minX) / step) * ny + (p.getY() - minY) / step) * nz + (p.getZ() - minZ) / step]++;
		}
		// A box counts as filled when a fair share of it is (a thin shell still shows as a shell).
		int threshold = Math.max(1, step * step / 3);
		java.util.function.IntPredicate filled = idx -> count[idx] >= threshold;
		List<float[]> faces = new ArrayList<>();
		for (int x = 0; x < nx; x++) {
			for (int y = 0; y < ny; y++) {
				for (int z = 0; z < nz; z++) {
					if (!filled.test((x * ny + y) * nz + z)) {
						continue;
					}
					float x0 = minX + x * step, y0 = minY + y * step, z0 = minZ + z * step;
					float x1 = x0 + step, y1 = y0 + step, z1 = z0 + step;
					if (y + 1 >= ny || !filled.test((x * ny + y + 1) * nz + z)) faces.add(new float[] {x0, y1, z0, x1, y1, z1, 1});
					if (x + 1 >= nx || !filled.test(((x + 1) * ny + y) * nz + z)) faces.add(new float[] {x1, y0, z0, x1, y1, z1, 0});
					if (x == 0 || !filled.test(((x - 1) * ny + y) * nz + z)) faces.add(new float[] {x0, y0, z0, x0, y1, z1, 0});
					if (z + 1 >= nz || !filled.test((x * ny + y) * nz + z + 1)) faces.add(new float[] {x0, y0, z1, x1, y1, z1, 0});
					if (z == 0 || !filled.test((x * ny + y) * nz + z - 1)) faces.add(new float[] {x0, y0, z0, x1, y1, z0, 0});
				}
			}
		}
		float[] out = new float[faces.size() * 7];
		for (int i = 0; i < faces.size(); i++) {
			System.arraycopy(faces.get(i), 0, out, i * 7, 7);
		}
		hull[rotation.ordinal()] = out;
		return out;
	}
}
