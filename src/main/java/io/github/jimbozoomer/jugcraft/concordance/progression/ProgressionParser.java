package io.github.jimbozoomer.jugcraft.concordance.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads stages and practice gates strictly ({@link Json}), roadmap step 24:
 * <pre>
 * stage:    {"schema": 1, "order": 3, "routes": [{"id": "specialist", "mastered": 2},
 *            {"id": "generalist", "understood": 5, "traditions": 5}, {"id": "circle", "research": ["jugcraft:circle_lore@understood"]}]}
 * practice: {"schema": 1, "tradition": "circlewrights", "requires": [{"research": "jugcraft:circle_lore", "state": "understood"}]}
 * milestone: {"schema": 1, "requires": [{"research": "jugcraft:circle_lore", "state": "understood"}], "stage": "master"}
 * </pre>
 * Refused: unknown fields, a route asking for nothing, a research state that is not one, more than eight routes.
 */
public final class ProgressionParser {
	public static final int MAX_ROUTES = 8;
	public static final int MAX_COUNT = 32;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable StageDefinition stage(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "stage");
			Json.only(object, "schema", "order", "routes");
			Json.schema(object);
			JsonArray routesJson = Json.array(object, "routes");
			if (routesJson.isEmpty() || routesJson.size() > MAX_ROUTES) {
				throw new Json.Invalid("routes: 1 to " + MAX_ROUTES);
			}
			List<Route> routes = new ArrayList<>();
			Set<String> ids = new HashSet<>();
			for (JsonElement element : routesJson) {
				Route route = route(element);
				if (!ids.add(route.id())) {
					throw new Json.Invalid("route " + route.id() + " appears twice");
				}
				routes.add(route);
			}
			return new StageDefinition(id.substring(id.indexOf(':') + 1), Json.range(object, "order", 1, 16), routes);
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("stage " + id + ": " + problem.getMessage());
			return null;
		}
	}

	private static Route route(JsonElement element) {
		JsonObject object = Json.object(element, "route");
		Json.only(object, "id", "research", "mastered", "understood", "traditions", "rank", "projects", "milestones");
		List<String> research = new ArrayList<>();
		if (object.has("research")) {
			for (JsonElement entry : Json.array(object, "research")) {
				String value = entry.getAsString();
				int at = value.indexOf('@');
				if (at < 0 || Stages.state(value.substring(at + 1)) == null || Stages.state(value.substring(at + 1)) == ResearchState.NONE) {
					throw new Json.Invalid("research " + value + " is not <research>@<state>");
				}
				Json.checkId(value.substring(0, at), "research");
				research.add(value);
			}
		}
		List<String> milestones = object.has("milestones") ? Json.ids(object, "milestones", false) : List.of();
		String rank = object.has("rank") ? Json.name(object, "rank") : "";
		if (!rank.isEmpty() && Stages.rank(rank) == null) {
			throw new Json.Invalid("no Conclave rank " + rank);
		}
		Route route = new Route(Json.name(object, "id"), research, Json.optional(object, "mastered", 0, MAX_COUNT, 0),
				Json.optional(object, "understood", 0, MAX_COUNT, 0), Json.optional(object, "traditions", 0, MAX_COUNT, 0), rank,
				Json.optional(object, "projects", 0, MAX_COUNT, 0), milestones);
		if (route.research().isEmpty() && route.mastered() == 0 && route.understood() == 0 && route.rank().isEmpty() && route.projects() == 0
				&& route.milestones().isEmpty()) {
			throw new Json.Invalid("route " + route.id() + " asks for nothing");
		}
		return route;
	}

	public @Nullable PracticeGate practice(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "practice");
			Json.only(object, "schema", "tradition", "requires", "stage");
			Json.schema(object);
			List<Definitions.Requirement> requires = new ArrayList<>();
			for (JsonElement element : Json.array(object, "requires")) {
				JsonObject requirement = Json.object(element, "requirement");
				Json.only(requirement, "research", "state");
				ResearchState state = Stages.state(Json.string(requirement, "state"));
				if (state == null || state == ResearchState.NONE) {
					throw new Json.Invalid("state " + Json.string(requirement, "state") + " is not a research state");
				}
				requires.add(new Definitions.Requirement(Json.id(requirement, "research"), state));
			}
			return new PracticeGate(id, object.has("tradition") ? Json.name(object, "tradition") : "", requires,
					object.has("stage") ? Json.name(object, "stage") : "");
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("practice " + id + ": " + problem.getMessage());
			return null;
		}
	}
}
