package io.github.jimbozoomer.jugcraft.concordance.worker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Reads worker definitions strictly ({@link Json}), one of three kinds:
 * <pre>
 * {"schema": 1, "kind": "familiar", "follow": 8, "support": {"effect": "minecraft:regeneration", "amplifier": 0, "duration": 100}}
 * {"schema": 1, "kind": "spirit", "work": "gather", "radius": 8, "from": 13000, "to": 23000, "quota": 64, "carry": 16}
 * {"schema": 1, "kind": "construct", "integrity": 64, "wear": 1, "repair": {"item": "minecraft:copper_ingot", "amount": 16},
 *  "energy": 64, "trip_energy": 2, "carry": 16}
 * </pre>
 */
public final class WorkerParser {
	/** The kinds of work a spirit can agree to (each needs its own server behaviour). */
	public static final List<String> WORK = List.of("gather");
	public static final int MAX_RADIUS = 12;
	public static final int MAX_CARRY = 64;
	public static final int MAX_QUOTA = 256;
	public static final int MAX_SUPPORT_TICKS = 200;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable WorkerDefinition worker(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "worker");
			String kind = Json.choice(object, "kind", "familiar", "spirit", "construct");
			switch (kind) {
				case "familiar" -> {
					Json.only(object, "schema", "kind", "follow", "support");
					Json.schema(object);
					JsonObject support = Json.object(Json.member(object, "support"), "support");
					Json.only(support, "effect", "amplifier", "duration");
					return new WorkerDefinition.Familiar(id, Json.range(object, "follow", 2, 16), Json.id(support, "effect"),
							Json.range(support, "amplifier", 0, 1), Json.range(support, "duration", 20, MAX_SUPPORT_TICKS));
				}
				case "spirit" -> {
					Json.only(object, "schema", "kind", "work", "radius", "from", "to", "quota", "carry");
					Json.schema(object);
					return new WorkerDefinition.Spirit(id, Json.choice(object, "work", WORK.toArray(String[]::new)), Json.range(object, "radius", 1, MAX_RADIUS),
							Json.range(object, "from", 0, 23999), Json.range(object, "to", 0, 24000), Json.range(object, "quota", 1, MAX_QUOTA),
							Json.range(object, "carry", 1, MAX_CARRY));
				}
				default -> {
					Json.only(object, "schema", "kind", "integrity", "wear", "repair", "energy", "trip_energy", "carry");
					Json.schema(object);
					JsonObject repair = Json.object(Json.member(object, "repair"), "repair");
					Json.only(repair, "item", "amount");
					int integrity = Json.range(object, "integrity", 1, 1024);
					int energy = Json.range(object, "energy", 1, 1024);
					return new WorkerDefinition.Construct(id, integrity, Json.range(object, "wear", 1, integrity), Json.id(repair, "item"),
							Json.range(repair, "amount", 1, integrity), energy, Json.range(object, "trip_energy", 1, energy),
							Json.range(object, "carry", 1, MAX_CARRY));
				}
			}
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("worker " + id + ": " + problem.getMessage());
			return null;
		}
	}
}
