package io.github.jimbozoomer.jugcraft.concordance.conclave;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads commissions and projects strictly ({@link Json}), roadmap step 23:
 * <pre>
 * commission: {"schema": 1, "tradition": "lampwrights", "tier": 1, "deliver": "minecraft:glowstone_dust", "count": 16,
 *              "renown": 4, "reward": "minecraft:amethyst_shard", "reward_count": 2}
 *             (or "practice": "jugcraft:ritual" in place of "deliver" and "count")
 * project:    {"schema": 1, "tradition": "starwatchers", "renown": 30, "reward": "minecraft:spyglass", "reward_count": 1,
 *              "stages": [{"id": "survey", "renown": 10,
 *              "contributors": 2, "days": 2, "requirements": [{"id": "lenses", "deliver": "minecraft:glass_pane", "count": 32},
 *              {"id": "watch", "practice": "jugcraft:observation", "count": 2}, {"id": "attuned", "research": "jugcraft:..."}]}]}
 * </pre>
 * Refused: unknown fields, a tier outside 1 to 3, a commission paying in what it asks for, more than five stages or six
 * requirements a stage, a cooperation rule without a solo alternative (more than four contributors or seven days), and
 * duplicate stage or requirement ids.
 */
public final class ConclaveParser {
	public static final int MAX_TIER = 3;
	public static final int MAX_COUNT = 256;
	public static final int MAX_RENOWN = 40;
	public static final int MAX_REWARD = 16;
	public static final int MAX_STAGES = 5;
	public static final int MAX_REQUIREMENTS = 6;
	public static final int MAX_CONTRIBUTORS = 4;
	public static final int MAX_DAYS = 7;
	public static final int MAX_PRACTICE = 8;
	public static final int MAX_PROJECT_RENOWN = 100;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable CommissionDefinition commission(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "commission");
			Json.schema(object);
			boolean delivery = object.has("deliver");
			if (delivery == object.has("practice")) {
				throw new Json.Invalid("needs exactly one of deliver or practice");
			}
			if (delivery) {
				Json.only(object, "schema", "tradition", "tier", "deliver", "count", "renown", "reward", "reward_count");
			} else {
				Json.only(object, "schema", "tradition", "tier", "practice", "renown", "reward", "reward_count");
			}
			String item = delivery ? Json.id(object, "deliver") : null;
			String reward = Json.id(object, "reward");
			if (reward.equals(item)) {
				throw new Json.Invalid("it pays in what it asks for");
			}
			return new CommissionDefinition(id, Json.name(object, "tradition"), Json.range(object, "tier", 1, MAX_TIER), item,
					delivery ? Json.range(object, "count", 1, MAX_COUNT) : 1, delivery ? null : Json.id(object, "practice"),
					Json.range(object, "renown", 1, MAX_RENOWN), reward, Json.range(object, "reward_count", 1, MAX_REWARD));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("commission " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable ProjectDefinition project(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "project");
			Json.only(object, "schema", "tradition", "renown", "reward", "reward_count", "stages");
			Json.schema(object);
			JsonArray stagesJson = Json.array(object, "stages");
			if (stagesJson.isEmpty() || stagesJson.size() > MAX_STAGES) {
				throw new Json.Invalid("stages: 1 to " + MAX_STAGES);
			}
			List<ProjectDefinition.Stage> stages = new ArrayList<>();
			Set<String> stageIds = new HashSet<>();
			for (JsonElement element : stagesJson) {
				ProjectDefinition.Stage stage = stage(element);
				if (!stageIds.add(stage.id())) {
					throw new Json.Invalid("stage " + stage.id() + " appears twice");
				}
				stages.add(stage);
			}
			return new ProjectDefinition(id, Json.name(object, "tradition"), Json.range(object, "renown", 1, MAX_PROJECT_RENOWN),
					Json.id(object, "reward"), Json.range(object, "reward_count", 1, MAX_REWARD), List.copyOf(stages));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("project " + id + ": " + problem.getMessage());
			return null;
		}
	}

	private static ProjectDefinition.Stage stage(JsonElement element) {
		JsonObject object = Json.object(element, "stage");
		Json.only(object, "id", "renown", "contributors", "days", "requirements");
		JsonArray requirementsJson = Json.array(object, "requirements");
		if (requirementsJson.isEmpty() || requirementsJson.size() > MAX_REQUIREMENTS) {
			throw new Json.Invalid("requirements: 1 to " + MAX_REQUIREMENTS);
		}
		List<Requirement> requirements = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (JsonElement entry : requirementsJson) {
			Requirement requirement = requirement(entry);
			if (!ids.add(requirement.id())) {
				throw new Json.Invalid("requirement " + requirement.id() + " appears twice");
			}
			requirements.add(requirement);
		}
		return new ProjectDefinition.Stage(Json.name(object, "id"), Json.range(object, "renown", 1, MAX_RENOWN),
				Json.range(object, "contributors", 1, MAX_CONTRIBUTORS), Json.range(object, "days", 1, MAX_DAYS), List.copyOf(requirements));
	}

	private static Requirement requirement(JsonElement element) {
		JsonObject object = Json.object(element, "requirement");
		int kinds = (object.has(Requirement.DELIVER) ? 1 : 0) + (object.has(Requirement.PRACTICE) ? 1 : 0) + (object.has(Requirement.RESEARCH) ? 1 : 0);
		if (kinds != 1) {
			throw new Json.Invalid("a requirement needs exactly one of deliver, practice or research");
		}
		String id = Json.name(object, "id");
		if (object.has(Requirement.DELIVER)) {
			Json.only(object, "id", Requirement.DELIVER, "count");
			return new Requirement(id, Requirement.DELIVER, Json.id(object, Requirement.DELIVER), Json.range(object, "count", 1, MAX_COUNT * 2));
		}
		if (object.has(Requirement.PRACTICE)) {
			Json.only(object, "id", Requirement.PRACTICE, "count");
			return new Requirement(id, Requirement.PRACTICE, Json.id(object, Requirement.PRACTICE), Json.range(object, "count", 1, MAX_PRACTICE));
		}
		// A research requirement is always met once, by one contributor who understands it: a solo player can meet it.
		Json.only(object, "id", Requirement.RESEARCH);
		return new Requirement(id, Requirement.RESEARCH, Json.id(object, Requirement.RESEARCH), 1);
	}

	/**
	 * The commissions and projects against the traditions and research that exist: every tradition is one research
	 * entries have, every research requirement names an entry, and each tier has at least one commission so every rank
	 * has something to take.
	 */
	public static List<String> check(Collection<CommissionDefinition> commissions, Collection<ProjectDefinition> projects, Set<String> traditions,
			Set<String> research) {
		List<String> problems = new ArrayList<>();
		Set<Integer> tiers = new HashSet<>();
		for (CommissionDefinition commission : commissions) {
			tiers.add(commission.tier());
			if (!traditions.contains(commission.tradition())) {
				problems.add("commission " + commission.id() + ": no research belongs to the tradition " + commission.tradition());
			}
		}
		if (!commissions.isEmpty()) {
			for (int tier = 1; tier <= MAX_TIER; tier++) {
				if (!tiers.contains(tier)) {
					problems.add("commissions: none of tier " + tier);
				}
			}
		}
		for (ProjectDefinition project : projects) {
			if (!traditions.contains(project.tradition())) {
				problems.add("project " + project.id() + ": no research belongs to the tradition " + project.tradition());
			}
			for (ProjectDefinition.Stage stage : project.stages()) {
				for (Requirement requirement : stage.requirements()) {
					if (requirement.type().equals(Requirement.RESEARCH) && !research.contains(requirement.target())) {
						problems.add("project " + project.id() + " stage " + stage.id() + ": unknown research " + requirement.target());
					}
				}
			}
		}
		return problems;
	}
}
