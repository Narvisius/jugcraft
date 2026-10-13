package io.github.jimbozoomer.jugcraft.concordance.crimson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Reads offering rites strictly ({@link Json}):
 * <pre>
 * {"schema": 1, "health": 4, "vitae": 4, "exhaustion": 3, "floor": 8, "cooldown": 100}
 * </pre>
 */
public final class CrimsonParser {
	/** The most health one offering takes (ten hearts) and the longest cooldown (a day). */
	public static final int MAX_HEALTH = 20;
	public static final int MAX_COOLDOWN = 24000;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable Rite rite(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "offering");
			Json.only(object, "schema", "health", "vitae", "exhaustion", "floor", "cooldown");
			Json.schema(object);
			int health = Json.range(object, "health", 1, MAX_HEALTH);
			return new Rite(id, health, Json.range(object, "vitae", 1, health), Json.range(object, "exhaustion", 1, Exhaustion.MAX),
					Json.range(object, "floor", 1, MAX_HEALTH - 1), Json.range(object, "cooldown", 0, MAX_COOLDOWN));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("offering " + id + ": " + problem.getMessage());
			return null;
		}
	}
}
