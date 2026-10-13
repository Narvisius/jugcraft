package io.github.jimbozoomer.jugcraft.concordance.hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * Reads curse definitions strictly ({@link Json}), roadmap step 22:
 * <pre>
 * {"schema": 1, "reagent": "minecraft:cobweb", "status": "minecraft:slowness", "amplifier": 0, "pulse_duration": 200,
 *  "pulse_ticks": 400, "total_ticks": 6000, "strength": 30, "focus": 6, "remedy": "minecraft:sugar"}
 * </pre>
 * Refused: a status above level II, a pulse longer than 30 s or more often than every 5 s, a curse longer than ten
 * minutes, unknown fields; {@link #check} refuses two curses sharing a reagent and a remedy that is its own reagent.
 */
public final class HexParser {
	public static final int MAX_AMPLIFIER = 1;
	public static final int MAX_PULSE_DURATION = 600;
	public static final int MIN_PULSE_TICKS = 100;
	public static final int MAX_TOTAL_TICKS = 12_000;
	public static final int MAX_FOCUS = 20;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable CurseDefinition curse(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "curse");
			Json.only(object, "schema", "reagent", "status", "amplifier", "pulse_duration", "pulse_ticks", "total_ticks", "strength", "focus",
					"remedy");
			Json.schema(object);
			int pulse = Json.range(object, "pulse_ticks", MIN_PULSE_TICKS, MAX_TOTAL_TICKS);
			return new CurseDefinition(id, Json.id(object, "reagent"), Json.id(object, "status"), Json.range(object, "amplifier", 0, MAX_AMPLIFIER),
					Json.range(object, "pulse_duration", 20, MAX_PULSE_DURATION), pulse, Json.range(object, "total_ticks", pulse, MAX_TOTAL_TICKS),
					Json.range(object, "strength", 1, Link.FRESH), Json.range(object, "focus", 1, MAX_FOCUS), Json.id(object, "remedy"));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("curse " + id + ": " + problem.getMessage());
			return null;
		}
	}

	/** The curses as a whole: one curse per reagent, and no curse lifted by its own reagent. */
	public static List<String> check(Map<String, CurseDefinition> curses) {
		List<String> problems = new ArrayList<>();
		Map<String, String> reagents = new TreeMap<>();
		for (CurseDefinition curse : curses.values()) {
			String previous = reagents.put(curse.reagent(), curse.id());
			if (previous != null) {
				problems.add("curses " + previous + " and " + curse.id() + " share the reagent " + curse.reagent());
			}
			if (curse.remedy().equals(curse.reagent())) {
				problems.add("curse " + curse.id() + ": its remedy is its own reagent");
			}
		}
		return problems;
	}
}
