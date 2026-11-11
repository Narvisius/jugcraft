package io.github.jimbozoomer.jugcraft.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Blueprints stacked floor on floor (part W3; Justin, 2026-10-17: pick the floors when you place it, and ADD A FLOOR
 * at the warehouse's terminal). A stack is three built-in part blueprints of one building (tools/warehouse_bp.py
 * write_stacks): the base (floor 1 and the ground out front), a middle floor and the top (the last floor and the roof
 * over it), all drawn in the same frame, each band FH (storey) blocks tall. {@code data/jugcraft/blueprint/stacks.json}
 * lists them.
 *
 * <p>Ids: {@code <kind>} is the stack with its default floors, {@code <kind>@<n>} with n floors (the base, n - 2
 * middles, the top), and {@code <kind>@add} one more floor for a finished building (a middle and the top, staked so
 * that its middle lands on the old top floor). Each side builds them from its own jar (Blueprint.get), like the large
 * built-ins, so they are never sent over the network.
 */
public final class BlueprintStacks {
	/** One stacked building as stacks.json describes it (positions in its parts' frame, x across, y up, z rows). */
	public record Stack(String kind, String name, String base, String middle, String top, int storey, int min, int def, int width, int depth,
			int hallWidth, int hallDepth, BlockPos anchor, BlockPos terminal, BlockPos addAnchor) {
		/** The height of the top part (its floor and the roof and parapet over it). */
		public int topHeight() {
			return storey + 2;
		}

		/** The highest block of an n-floor stack above its stake. */
		public int topAbove(int floors) {
			return (floors - 1) * storey + topHeight() - 1 - anchor.getY();
		}

		/** The most floors that fit with the stake at y (the world's highest block maxY). */
		public int maxFloors(int stakeY, int maxY) {
			int n = min;
			while (n < MAX && stakeY + topAbove(n + 1) <= maxY) {
				n++;
			}
			return n;
		}

		public String id(int floors) {
			return kind + "@" + floors;
		}

		public String addId() {
			return kind + ADD;
		}
	}

	public static final String ADD = "@add";
	/** The most floors a stack can have (its offsets must fit Blueprint.pack). */
	public static final int MAX = 60;
	private static final Map<String, Stack> STACKS = load();
	/** Called when a Survey Stake's build is finished (the warehouse's terminal hears of its floors this way). */
	public static final List<BiConsumer<ServerLevel, SurveyStakeBlockEntity>> COMPLETED = new ArrayList<>();

	private BlueprintStacks() {
	}

	private static Map<String, Stack> load() {
		Map<String, Stack> out = new LinkedHashMap<>();
		try (InputStream in = BlueprintStacks.class.getResourceAsStream("/data/jugcraft/blueprint/stacks.json")) {
			if (in == null) {
				return out;
			}
			JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				JsonArray size = o.getAsJsonArray("size"), hall = o.getAsJsonArray("hall");
				out.put(e.getKey(), new Stack(e.getKey(), o.get("name").getAsString(), o.get("base").getAsString(), o.get("middle").getAsString(),
						o.get("top").getAsString(), o.get("storey").getAsInt(), o.get("min").getAsInt(), o.get("default").getAsInt(),
						size.get(0).getAsInt(), size.get(1).getAsInt(), hall.get(0).getAsInt(), hall.get(1).getAsInt(),
						pos(o.getAsJsonArray("anchor")), pos(o.getAsJsonArray("terminal")),
						pos(o.getAsJsonArray("add_anchor"))));
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Blueprint stacks unreadable: {}", e.toString());
		}
		return out;
	}

	private static BlockPos pos(JsonArray a) {
		return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}

	/** Every stack, in stacks.json's order. */
	public static List<Stack> all() {
		return List.copyOf(STACKS.values());
	}

	/** The stack kinds (their default-floors ids), for the Blueprint Table's library. */
	public static List<String> kinds() {
		return List.copyOf(STACKS.keySet());
	}

	/** The stack an id belongs to ({@code <kind>}, {@code <kind>@<n>} or {@code <kind>@add}), or null. */
	public static @Nullable Stack of(String id) {
		if (id == null) {
			return null;
		}
		int at = id.indexOf('@');
		Stack stack = STACKS.get(at < 0 ? id : id.substring(0, at));
		if (stack == null || at < 0 || id.endsWith(ADD)) {
			return stack;
		}
		try {
			int n = Integer.parseInt(id.substring(at + 1));
			return n >= stack.min && n <= MAX ? stack : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** True for a stack's own id (not its parts). */
	public static boolean isStack(String id) {
		return of(id) != null;
	}

	public static boolean isAdd(String id) {
		return id != null && id.endsWith(ADD) && of(id) != null;
	}

	/** One of the stacks' part blueprints (built in, read from the jar like the large ones). */
	public static boolean isPart(String id) {
		for (Stack s : STACKS.values()) {
			if (id.equals(s.base) || id.equals(s.middle) || id.equals(s.top)) {
				return true;
			}
		}
		return false;
	}

	/** The floors of a stack id (its default for the bare kind; 0 for an added floor). */
	public static int floors(String id) {
		Stack stack = of(id);
		if (stack == null) {
			return 0;
		}
		int at = id.indexOf('@');
		if (at < 0) {
			return stack.def;
		}
		return id.endsWith(ADD) ? 0 : Integer.parseInt(id.substring(at + 1));
	}

	/** The blueprint file (format 2, made of the stack's parts) for a stack id. */
	static String compose(String id) {
		Stack s = of(id);
		if (s == null) {
			throw new IllegalArgumentException(id);
		}
		JsonObject root = new JsonObject();
		root.addProperty("format", 2);
		root.addProperty("author", "Jugcraft (built in)");
		root.add("palette", new JsonObject());
		JsonArray parts = new JsonArray();
		BlockPos anchor;
		if (isAdd(id)) {
			root.addProperty("name", s.name + ": another floor");
			root.addProperty("kind", "part");
			root.addProperty("description", "One more floor for a " + s.name + ": a middle floor on the old top floor, the top floor and roof over it.");
			parts.add(part(s.middle, 0));
			parts.add(part(s.top, s.storey));
			anchor = s.addAnchor;
		} else {
			int n = floors(id);
			root.addProperty("name", s.name + " (" + n + " floors)");
			root.addProperty("kind", "individual");
			root.addProperty("description", "The pre-made " + s.name + " with " + n + " floors (shift+scroll with it in hand for more or fewer).");
			parts.add(part(s.base, 0));
			for (int k = 1; k < n - 1; k++) {
				parts.add(part(s.middle, k * s.storey));
			}
			parts.add(part(s.top, (n - 1) * s.storey));
			anchor = s.anchor;
		}
		root.add("parts", parts);
		JsonArray a = new JsonArray();
		a.add(anchor.getX());
		a.add(anchor.getY());
		a.add(anchor.getZ());
		root.add("anchor", a);
		return root.toString();
	}

	private static JsonObject part(String id, int y) {
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		JsonArray at = new JsonArray();
		at.add(0);
		at.add(y);
		at.add(0);
		o.add("at", at);
		return o;
	}

	/** Tells the listeners a stake's build is done (SurveyStakeBlockEntity, just before the stake goes). */
	static void completed(ServerLevel level, SurveyStakeBlockEntity stake) {
		for (BiConsumer<ServerLevel, SurveyStakeBlockEntity> listener : COMPLETED) {
			try {
				listener.accept(level, stake);
			} catch (RuntimeException e) {
				Jugcraft.LOGGER.error("Blueprint completion listener failed", e);
			}
		}
	}
}
