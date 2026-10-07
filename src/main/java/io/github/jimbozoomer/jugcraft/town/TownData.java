package io.github.jimbozoomer.jugcraft.town;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The walled town's design, written by tools/town.py into {@code data/jugcraft/town/town.json.gz} and read once from
 * the mod jar by the server: every block of the town in a SIZE x SIZE x HEIGHT box
 * as palette indexes (0 leaves the world's block, 1 is air), a mask of which columns are inside the wall (levelled) or
 * in the ring outside it (blended back to the land), paths on that ring, the decor sites, where each townsperson
 * stands, the ATMs and the decor themes. The shops are in {@link TownShops}.
 *
 * <p>Town coordinates: x and z from 0 to SIZE - 1, y = 0 the town's ground. {@link TownState} keeps where the town's
 * (0, 0, 0) is in the world.
 */
public final class TownData {
	public static final int KEEP = 0;
	public static final int AIR = 1;
	/** A mask value: inside the wall. Values 1..blend are blocks outside it; 0 is beyond the town. */
	public static final int INSIDE = 255;

	/** A decor site: its kind, the way it faces, and its blocks with their slot numbers (town coordinates). */
	public record Site(int index, String kind, Direction facing, int[][] blocks) {
		public BlockPos anchor() {
			return new BlockPos(blocks[0][0], blocks[0][1], blocks[0][2]);
		}
	}

	/** Where a townsperson first stands: role, shop (for shopkeepers), name and skin. */
	public record Spot(int index, String role, @Nullable String shop, String name, String skin, BlockPos pos) {
	}

	private static @Nullable TownData instance;

	public final int size;
	public final int yMin;
	public final int height;
	public final int blend;
	public final int protect;
	public final int wallTop;
	public final List<String> paletteText;
	private final BlockState[] palette;
	private final short[] blocks;
	private final byte[] mask;
	private final short[] surface;
	public final List<Site> sites;
	public final List<Spot> spots;
	public final List<BlockPos> atms;
	public final List<String> themes;
	/** kind -> theme -> slots -> choices (state text with "{facing}" and "{rotation}"). */
	public final Map<String, Map<String, List<List<String>>>> decor;
	/** theme -> slot -> state text, for the square's centrepiece. */
	public final Map<String, Map<Integer, String>> centerpiece;

	private TownData(JsonObject root) {
		size = root.get("size").getAsInt();
		yMin = root.get("y_min").getAsInt();
		height = root.get("height").getAsInt();
		blend = root.get("blend").getAsInt();
		protect = root.get("protect").getAsInt();
		wallTop = root.get("wall_top").getAsInt();
		paletteText = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray("palette")) {
			paletteText.add(e.getAsString());
		}
		palette = new BlockState[paletteText.size()];
		for (int i = 0; i < palette.length; i++) {
			palette[i] = i == KEEP ? Blocks.AIR.defaultBlockState() : parse(paletteText.get(i));
		}
		blocks = shorts(root.get("blocks").getAsString(), size * size * height);
		mask = Base64.getDecoder().decode(root.get("mask").getAsString());
		surface = shorts(root.get("surface").getAsString(), size * size);
		List<Site> sites = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray("sites")) {
			JsonObject o = e.getAsJsonObject();
			JsonArray list = o.getAsJsonArray("blocks");
			int[][] b = new int[list.size()][];
			for (int i = 0; i < b.length; i++) {
				JsonArray a = list.get(i).getAsJsonArray();
				b[i] = new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()};
			}
			sites.add(new Site(sites.size(), o.get("kind").getAsString(), direction(o.get("facing").getAsString()), b));
		}
		this.sites = List.copyOf(sites);
		List<Spot> spots = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray("spots")) {
			JsonObject o = e.getAsJsonObject();
			JsonArray p = o.getAsJsonArray("pos");
			spots.add(new Spot(spots.size(), o.get("role").getAsString(), o.has("shop") ? o.get("shop").getAsString() : null,
					o.get("name").getAsString(), o.get("skin").getAsString(),
					new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt())));
		}
		this.spots = List.copyOf(spots);
		List<BlockPos> atms = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray("atms")) {
			JsonArray a = e.getAsJsonArray();
			atms.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
		}
		this.atms = List.copyOf(atms);
		JsonObject decorRoot = root.getAsJsonObject("decor");
		List<String> themes = new ArrayList<>();
		for (JsonElement e : decorRoot.getAsJsonArray("themes")) {
			themes.add(e.getAsString());
		}
		this.themes = List.copyOf(themes);
		Map<String, Map<String, List<List<String>>>> decor = new HashMap<>();
		for (Map.Entry<String, JsonElement> kind : decorRoot.getAsJsonObject("kinds").entrySet()) {
			Map<String, List<List<String>>> byTheme = new HashMap<>();
			for (Map.Entry<String, JsonElement> theme : kind.getValue().getAsJsonObject().entrySet()) {
				List<List<String>> slots = new ArrayList<>();
				for (JsonElement slot : theme.getValue().getAsJsonArray()) {
					List<String> choices = new ArrayList<>();
					for (JsonElement choice : slot.getAsJsonArray()) {
						choices.add(choice.getAsString());
					}
					slots.add(List.copyOf(choices));
				}
				byTheme.put(theme.getKey(), List.copyOf(slots));
			}
			decor.put(kind.getKey(), byTheme);
		}
		this.decor = decor;
		Map<String, Map<Integer, String>> centre = new HashMap<>();
		for (Map.Entry<String, JsonElement> theme : decorRoot.getAsJsonObject("centerpiece").entrySet()) {
			Map<Integer, String> slots = new HashMap<>();
			for (Map.Entry<String, JsonElement> slot : theme.getValue().getAsJsonObject().entrySet()) {
				slots.put(Integer.parseInt(slot.getKey()), slot.getValue().getAsString());
			}
			centre.put(theme.getKey(), slots);
		}
		this.centerpiece = centre;
	}

	public static synchronized TownData get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static TownData load() {
		String path = "/data/jugcraft/town/town.json.gz";
		try (InputStream raw = TownData.class.getResourceAsStream(path)) {
			if (raw == null) {
				throw new IllegalStateException("missing " + path);
			}
			return new TownData(JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8)).getAsJsonObject());
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the town data", e);
			throw new IllegalStateException("Town data unreadable", e);
		}
	}

	/** The palette index at a town position (KEEP outside the data). */
	public int index(int x, int y, int z) {
		int dy = y - yMin;
		if (x < 0 || z < 0 || x >= size || z >= size || dy < 0 || dy >= height) {
			return KEEP;
		}
		return blocks[(dy * size + z) * size + x] & 0xFFFF;
	}

	public BlockState state(int index) {
		return palette[index];
	}

	/** INSIDE, 1..blend (blocks outside the wall), or 0 beyond the town; 0 outside the data too. */
	public int mask(int x, int z) {
		if (x < 0 || z < 0 || x >= size || z >= size) {
			return 0;
		}
		return mask[z * size + x] & 0xFF;
	}

	/** The block that tops a blended column (a path out of a gate), or null for the land's own. */
	public @Nullable BlockState surface(int x, int z) {
		if (x < 0 || z < 0 || x >= size || z >= size) {
			return null;
		}
		int i = surface[z * size + x] & 0xFFFF;
		return i == KEEP ? null : palette[i];
	}

	/** Whether a town column is protected: inside the wall or up to {@link #protect} blocks outside it. */
	public boolean protectedColumn(int x, int z) {
		int m = mask(x, z);
		return m == INSIDE || m >= 1 && m <= protect;
	}

	/** How many palette entries failed to parse (they became air): a test checks there are none. */
	public int unknownStates() {
		int unknown = 0;
		for (int i = 2; i < palette.length; i++) {
			if (palette[i].isAir() && !paletteText.get(i).startsWith("minecraft:air")) {
				unknown++;
			}
		}
		return unknown;
	}

	public static BlockState parse(String state) {
		try {
			return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, state, false).blockState();
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Town data: unknown block state {}", state);
			return Blocks.AIR.defaultBlockState();
		}
	}

	private static Direction direction(String name) {
		Direction d = Direction.byName(name);
		return d == null ? Direction.NORTH : d;
	}

	private static short[] shorts(String base64, int expected) {
		byte[] bytes = Base64.getDecoder().decode(base64);
		if (bytes.length != expected * 2) {
			throw new IllegalStateException("town data: " + bytes.length + " bytes, expected " + expected * 2);
		}
		short[] out = new short[expected];
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out);
		return out;
	}
}
