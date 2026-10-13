package io.github.jimbozoomer.jugcraft.concordance.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strict JSON reading for the definitions later roadmap steps add (organisms, celestial patterns and the rest), in
 * the manner of {@link RulesParser}: an unknown field, a wrong type or a value out of range throws {@link Invalid}
 * with a message naming the field, and the step's parser turns that into a problem for the one file.
 */
public final class Json {
	public static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
	public static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

	private Json() {
	}

	/** Thrown to abandon one definition; the message is the diagnostic. */
	public static final class Invalid extends RuntimeException {
		public Invalid(String message) {
			super(message, null, false, false);
		}
	}

	public static JsonObject object(JsonElement element, String where) {
		if (element == null || !element.isJsonObject()) {
			throw new Invalid(where + " must be an object");
		}
		return element.getAsJsonObject();
	}

	public static JsonElement member(JsonObject object, String field) {
		JsonElement element = object.get(field);
		if (element == null || element.isJsonNull()) {
			throw new Invalid("missing \"" + field + "\"");
		}
		return element;
	}

	public static JsonArray array(JsonObject object, String field) {
		JsonElement element = member(object, field);
		if (!element.isJsonArray()) {
			throw new Invalid("\"" + field + "\" must be a list");
		}
		return element.getAsJsonArray();
	}

	/** Refuses any field not named (Fabric's own keys, such as fabric:load_conditions, are allowed). */
	public static void only(JsonObject object, String... fields) {
		List<String> allowed = List.of(fields);
		for (String key : object.keySet()) {
			if (!allowed.contains(key) && !key.startsWith("fabric:")) {
				throw new Invalid("unknown field \"" + key + "\"");
			}
		}
	}

	/** The data format version: a newer file is refused rather than misread. */
	public static int schema(JsonObject object) {
		int schema = range(object, "schema", 1, Integer.MAX_VALUE);
		if (schema > RulesParser.SCHEMA) {
			throw new Invalid("schema " + schema + " is newer than this version of Jugcraft understands (" + RulesParser.SCHEMA + ")");
		}
		return schema;
	}

	public static String string(JsonObject object, String field) {
		JsonElement element = member(object, field);
		if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
			throw new Invalid("\"" + field + "\" must be a string");
		}
		return primitive.getAsString();
	}

	public static String id(JsonObject object, String field) {
		String value = string(object, field);
		checkId(value, field);
		return value;
	}

	public static void checkId(String value, String field) {
		if (!ID.matcher(value).matches()) {
			throw new Invalid("\"" + field + "\" is not a valid id: \"" + value + "\"");
		}
	}

	public static String name(JsonObject object, String field) {
		String value = string(object, field);
		if (!NAME.matcher(value).matches()) {
			throw new Invalid("\"" + field + "\" is not a valid name: \"" + value + "\"");
		}
		return value;
	}

	/** A list of ids, or of ids and {@code #tags} when {@code tags} is true (the tags keep their {@code #}). */
	public static List<String> ids(JsonObject object, String field, boolean tags) {
		List<String> out = new ArrayList<>();
		for (JsonElement element : array(object, field)) {
			if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
				throw new Invalid("\"" + field + "\" must list ids");
			}
			String value = primitive.getAsString();
			checkId(tags && value.startsWith("#") ? value.substring(1) : value, field);
			out.add(value);
		}
		return List.copyOf(out);
	}

	public static int intValue(JsonElement element, String where, int min, int max) {
		if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
			throw new Invalid("\"" + where + "\" must be a whole number");
		}
		double value = primitive.getAsDouble();
		if (value != Math.rint(value) || value < min || value > max) {
			throw new Invalid("\"" + where + "\" must be a whole number from " + min + " to " + max);
		}
		return (int) value;
	}

	public static int range(JsonObject object, String field, int min, int max) {
		return intValue(member(object, field), field, min, max);
	}

	public static int optional(JsonObject object, String field, int min, int max, int fallback) {
		return object.has(field) ? range(object, field, min, max) : fallback;
	}

	public static double decimal(JsonObject object, String field, double min, double max) {
		JsonElement element = member(object, field);
		if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
			throw new Invalid("\"" + field + "\" must be a number");
		}
		double value = primitive.getAsDouble();
		if (!(value >= min && value <= max)) {
			throw new Invalid("\"" + field + "\" must be from " + min + " to " + max);
		}
		return value;
	}

	public static boolean bool(JsonObject object, String field, boolean fallback) {
		if (!object.has(field)) {
			return fallback;
		}
		JsonElement element = object.get(field);
		if (!(element instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
			throw new Invalid("\"" + field + "\" must be true or false");
		}
		return primitive.getAsBoolean();
	}

	/** One of {@code choices} (lower-case names). */
	public static String choice(JsonObject object, String field, String... choices) {
		String value = string(object, field);
		if (!List.of(choices).contains(value)) {
			throw new Invalid("\"" + field + "\" must be one of " + String.join(", ", choices) + ", not \"" + value + "\"");
		}
		return value;
	}
}
