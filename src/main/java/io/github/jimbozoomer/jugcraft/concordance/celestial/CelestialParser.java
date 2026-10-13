package io.github.jimbozoomer.jugcraft.concordance.celestial;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Reads celestial patterns strictly ({@link Json}):
 * <pre>
 * {"schema": 1, "principle": "radiance", "period": 8, "offset": 0, "from": 13000, "to": 23000, "clear_sky": true,
 *  "resonance": 3, "attunement": {"effect": "minecraft:night_vision", "amplifier": 0, "cost": 4, "recall": 12}}
 * </pre>
 */
public final class CelestialParser {
	/** The most resonance one occurrence pays and the most an attunement costs. */
	public static final int MAX_RESONANCE = 16;
	public static final int MAX_COST = 32;
	public static final int MAX_PERIOD = 64;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable Pattern pattern(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "pattern");
			Json.only(object, "schema", "principle", "period", "offset", "from", "to", "clear_sky", "resonance", "attunement", "season");
			Json.schema(object);
			String principle = Json.name(object, "principle");
			if (!ResourceType.PRINCIPLES.contains(principle)) {
				throw new Json.Invalid("\"principle\" is not a Principle: " + principle);
			}
			int period = Json.range(object, "period", 1, MAX_PERIOD);
			JsonObject attunement = Json.object(Json.member(object, "attunement"), "attunement");
			Json.only(attunement, "effect", "amplifier", "cost", "recall");
			int cost = Json.range(attunement, "cost", 1, MAX_COST);
			int recall = Json.range(attunement, "recall", 1, MAX_COST);
			if (recall <= cost) {
				throw new Json.Invalid("a recall costs more than attuning while the pattern is up");
			}
			String season = object.has("season") ? Json.name(object, "season") : null;
			return new Pattern(id, principle, period, Json.range(object, "offset", 0, period - 1), Json.range(object, "from", 0, Calendar.DAY - 1),
					Json.range(object, "to", 1, Calendar.DAY), Json.bool(object, "clear_sky", false), Json.range(object, "resonance", 1, MAX_RESONANCE),
					Json.id(attunement, "effect"), Json.range(attunement, "amplifier", 0, 1), cost, recall, season);
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("pattern " + id + ": " + problem.getMessage());
			return null;
		}
	}
}
