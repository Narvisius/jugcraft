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
	public static final List<String> BUILT_IN = List.of("drone_tower_foundation", "arc_furnace", "small_church");
	public static final int MAX_SIZE = 48;
	public static final int MAX_BLOCKS = 8192;
	public static final int MAX_CHARS = 262_144;
	private static final Set<Block> FORBIDDEN = Set.of(Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
			Blocks.STRUCTURE_BLOCK, Blocks.STRUCTURE_VOID, Blocks.JIGSAW, Blocks.BARRIER, Blocks.LIGHT, Blocks.BEDROCK, Blocks.SPAWNER,
			Blocks.TRIAL_SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME, Blocks.END_GATEWAY, Blocks.NETHER_PORTAL,
			Blocks.REINFORCED_DEEPSLATE, Blocks.TEST_BLOCK, Blocks.TEST_INSTANCE_BLOCK);

	private static final Map<String, Blueprint> SERVER = new LinkedHashMap<>();
	private static final Map<String, Blueprint> CLIENT = new LinkedHashMap<>();

	/** One block of the blueprint, relative to the stake (already turned). */
	/**
	 * Blueprint colours: a whole build of the mod's own (blue), one part of a larger build such as a single cooling
	 * tower (green, {@code "kind": "part"} in the file), or a player's import (red, whatever the file says).
	 */
	public enum Kind {
		COMPLETE("complete", 0x5AB4FF), PART("part", 0x5ADC78), IMPORTED("imported", 0xFF5A46);

		public final String id;
		/** The hologram colour, 0xRRGGBB. */
		public final int rgb;

		Kind(String id, int rgb) {
			this.id = id;
			this.rgb = rgb;
		}

		/** The colour with an alpha, 0xAARRGGBB. */
		public int argb(int alpha) {
			return (alpha << 24) | rgb;
		}
	}

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
	/** What the blueprint is, which sets its hologram and item colour. */
	public final Kind kind;
	public final String json;
	public final int sizeX;
	public final int sizeY;
	public final int sizeZ;
	private final List<Cell> raw;

	private Blueprint(String id, String name, String source, Kind kind, String json, int sizeX, int sizeY, int sizeZ, List<Cell> raw) {
		this.id = id;
		this.name = name;
		this.source = source;
		this.kind = kind;
		this.json = json;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.raw = raw;
	}

	// ------------------------------------------------------------------ the two libraries

	/** The blueprint with this id on the server (client false) or the client (client true), or null. */
	public static synchronized @Nullable Blueprint get(String id, boolean client) {
		return (client ? CLIENT : SERVER).get(id);
	}

	public static synchronized List<Blueprint> all(boolean client) {
		return List.copyOf((client ? CLIENT : SERVER).values());
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

	/** Reads and checks a blueprint; {@link Invalid} explains the first problem found. */
	public static Blueprint parse(String id, String json, String source) throws Invalid {
		if (json.length() > MAX_CHARS) {
			throw new Invalid("Too big: " + json.length() + " characters (the limit is " + MAX_CHARS + ")");
		}
		JsonObject root;
		try {
			root = JsonParser.parseString(json).getAsJsonObject();
		} catch (Exception e) {
			throw new Invalid("Not valid JSON: " + shortMessage(e));
		}
		if (!root.has("format") || root.get("format").getAsInt() != 1) {
			throw new Invalid("\"format\" must be 1");
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
			if (entry.getKey().length() != 1 || entry.getKey().equals(".") || entry.getKey().equals(" ")) {
				throw new Invalid("Palette key \"" + entry.getKey() + "\" must be one character (not '.' or space)");
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
		if (!root.has("layers") || !root.get("layers").isJsonArray()) {
			throw new Invalid("\"layers\" is missing: a list of layers, bottom first, each a list of text rows");
		}
		JsonArray layers = root.getAsJsonArray("layers");
		int sx = 0, sz = 0;
		int sy = layers.size();
		List<int[]> placed = new ArrayList<>();
		List<BlockState> states = new ArrayList<>();
		for (int y = 0; y < layers.size(); y++) {
			JsonArray rows = layers.get(y).getAsJsonArray();
			sz = Math.max(sz, rows.size());
			for (int z = 0; z < rows.size(); z++) {
				String row = rows.get(z).getAsString();
				sx = Math.max(sx, row.length());
				for (int x = 0; x < row.length(); x++) {
					char c = row.charAt(x);
					if (c == '.' || c == ' ') {
						continue;
					}
					BlockState state = palette.get(c);
					if (state == null) {
						if (!root.getAsJsonObject("palette").has(String.valueOf(c))) {
							throw new Invalid("Layer " + y + ", row " + z + " uses '" + c + "', which is not in the palette");
						}
						continue;
					}
					placed.add(new int[] {x, y, z});
					states.add(state);
				}
			}
		}
		if (sx > MAX_SIZE || sy > MAX_SIZE || sz > MAX_SIZE) {
			throw new Invalid("Too large: " + sx + " x " + sy + " x " + sz + " (at most " + MAX_SIZE + " each way)");
		}
		if (placed.isEmpty()) {
			throw new Invalid("The blueprint has no blocks");
		}
		if (placed.size() > MAX_BLOCKS) {
			throw new Invalid("Too many blocks: " + placed.size() + " (at most " + MAX_BLOCKS + ")");
		}
		int ax = sx / 2, ay = 0, az = sz + 1;
		if (root.has("anchor")) {
			JsonArray anchor = root.getAsJsonArray("anchor");
			ax = anchor.get(0).getAsInt();
			ay = anchor.get(1).getAsInt();
			az = anchor.get(2).getAsInt();
		}
		List<Cell> cells = new ArrayList<>(placed.size());
		for (int i = 0; i < placed.size(); i++) {
			int[] p = placed.get(i);
			BlockPos offset = new BlockPos(p[0] - ax, p[1] - ay, p[2] - az);
			if (offset.equals(BlockPos.ZERO)) {
				throw new Invalid("The anchor (where the stake stands) is on a block of the blueprint; put it in front of the build");
			}
			cells.add(new Cell(offset, states.get(i)));
		}
		Kind kind = Kind.COMPLETE;
		if ("imported".equals(source)) {
			kind = Kind.IMPORTED;
		} else if (root.has("kind")) {
			String text = root.get("kind").getAsString();
			if (!text.equals("complete") && !text.equals("part")) {
				throw new Invalid("\"kind\" must be \"complete\" or \"part\"");
			}
			kind = text.equals("part") ? Kind.PART : Kind.COMPLETE;
		}
		return new Blueprint(id, name, source, kind, json, sx, sy, sz, List.copyOf(cells));
	}

	private static String shortMessage(Exception e) {
		String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		return message.length() > 120 ? message.substring(0, 120) + "..." : message;
	}

	// ------------------------------------------------------------------ use

	public int size() {
		return raw.size();
	}

	/** The cells turned by {@code rotation} about the stake: north-facing placement is the file as drawn. */
	public List<Cell> cells(Rotation rotation) {
		List<Cell> out = new ArrayList<>(raw.size());
		for (Cell cell : raw) {
			out.add(new Cell(cell.offset().rotate(rotation), cell.state().rotate(rotation)));
		}
		return out;
	}

	/** Block counts, most first (for the library's bill of materials). */
	public Map<Block, Integer> materials() {
		Map<Block, Integer> counts = new HashMap<>();
		for (Cell cell : raw) {
			counts.merge(cell.state().getBlock(), 1, Integer::sum);
		}
		Map<Block, Integer> sorted = new LinkedHashMap<>();
		counts.entrySet().stream().sorted(Map.Entry.<Block, Integer>comparingByValue().reversed())
				.forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	/** The cells as drawn (not turned), for previews. */
	public Collection<Cell> rawCells() {
		return raw;
	}
}
