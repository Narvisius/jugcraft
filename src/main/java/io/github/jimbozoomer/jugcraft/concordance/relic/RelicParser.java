package io.github.jimbozoomer.jugcraft.concordance.relic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads relic definitions strictly ({@link Json}), roadmap step 20:
 * <pre>
 * {"schema": 1, "item": "jugcraft:wardlight", "capacity": 64, "owned": false, "modes": [
 *   {"id": "reveal", "contexts": ["off_hand"], "target": "hostiles", "status": "minecraft:glowing", "amplifier": 0,
 *    "duration": 60, "range": 12, "interval": 40, "cost": 1, "needs": {"sky": false, "night": false, "calm": false,
 *    "wounded": false, "dimensions": []}}]}
 * </pre>
 * Refused: a mode with no context, "inventory" or "cosmetic" as a context (carried loose or worn for show never works),
 * one context in two modes, a self mode with a range (or installed: a shrine has no holder), an allies or hostiles mode
 * with none, a pulse that costs nothing or more than the relic holds, and numbers out of bounds.
 */
public final class RelicParser {
	public static final int MAX_CAPACITY = 256;
	public static final int MAX_RANGE = 32;
	public static final int MIN_INTERVAL = 20;
	public static final int MAX_INTERVAL = 1200;
	public static final int MAX_DURATION = 600;
	public static final int MAX_AMPLIFIER = 1;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable RelicDefinition relic(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "relic");
			Json.only(object, "schema", "item", "capacity", "owned", "modes");
			Json.schema(object);
			int capacity = Json.range(object, "capacity", 1, MAX_CAPACITY);
			List<Mode> modes = new ArrayList<>();
			Set<Context> seen = EnumSet.noneOf(Context.class);
			for (JsonElement element : Json.array(object, "modes")) {
				Mode mode = mode(Json.object(element, "mode"), capacity);
				for (Context context : mode.contexts()) {
					if (!seen.add(context)) {
						throw new Json.Invalid("context \"" + context.id + "\" is in two modes");
					}
				}
				modes.add(mode);
			}
			if (modes.isEmpty()) {
				throw new Json.Invalid("a relic needs at least one mode");
			}
			return new RelicDefinition(id, Json.id(object, "item"), capacity, Json.bool(object, "owned", false), List.copyOf(modes));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("relic " + id + ": " + problem.getMessage());
			return null;
		}
	}

	private static Mode mode(JsonObject object, int capacity) {
		Json.only(object, "id", "contexts", "target", "status", "amplifier", "duration", "range", "interval", "cost", "needs");
		Set<Context> contexts = EnumSet.noneOf(Context.class);
		for (JsonElement element : Json.array(object, "contexts")) {
			Context context = element.isJsonPrimitive() ? Context.fromId(element.getAsString()) : null;
			if (context == null || !context.working()) {
				throw new Json.Invalid("\"contexts\" must name main_hand, off_hand, head, chest, legs, feet, trinket or installed");
			}
			contexts.add(context);
		}
		if (contexts.isEmpty()) {
			throw new Json.Invalid("a mode needs at least one context");
		}
		Mode.Target target = Mode.Target.valueOf(Json.choice(object, "target", "self", "allies", "hostiles").toUpperCase(java.util.Locale.ROOT));
		int range = Json.range(object, "range", 0, MAX_RANGE);
		if (target == Mode.Target.SELF && range != 0) {
			throw new Json.Invalid("a self mode has no range");
		}
		if (target != Mode.Target.SELF && range == 0) {
			throw new Json.Invalid("an allies or hostiles mode needs a range");
		}
		if (target == Mode.Target.SELF && contexts.contains(Context.INSTALLED)) {
			throw new Json.Invalid("an installed mode cannot serve \"self\": a shrine has no holder");
		}
		JsonObject needs = Json.object(Json.member(object, "needs"), "needs");
		Json.only(needs, "sky", "night", "calm", "wounded", "dimensions");
		Mode.Conditions conditions = new Mode.Conditions(Json.bool(needs, "sky", false), Json.bool(needs, "night", false),
				Json.bool(needs, "calm", false), Json.bool(needs, "wounded", false),
				needs.has("dimensions") ? Json.ids(needs, "dimensions", false) : List.of());
		return new Mode(Json.name(object, "id"), contexts, target, Json.id(object, "status"), Json.range(object, "amplifier", 0, MAX_AMPLIFIER),
				Json.range(object, "duration", 20, MAX_DURATION), range, Json.range(object, "interval", MIN_INTERVAL, MAX_INTERVAL),
				Json.range(object, "cost", 1, capacity), conditions);
	}
}
