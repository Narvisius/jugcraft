package io.github.jimbozoomer.jugcraft.concordance.wonder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads wonders and their configurations strictly ({@link Json}), roadmap step 25:
 * <pre>
 * wonder:        {"schema": 1, "heart": "jugcraft:spire_heart", "stage": "master", "rite": "jugcraft:spire_kindling",
 *                 "rite_range": 12, "attendance_days": 7, "pulse": 100,
 *                 "phases": [{"id": "foundation", "structure": "jugcraft:spire_foundation"},
 *                            {"id": "shaft", "structure": "jugcraft:spire_shaft", "practices": 2},
 *                            {"id": "crown", "structure": "crown", "rite": true}, {"id": "kindling", "sustain": 3}]}
 * configuration: {"schema": 1, "wonder": "jugcraft:concord_spire", "tradition": "lampwrights",
 *                 "requires": [{"research": "jugcraft:first_light", "state": "mastered"}], "practice": "jugcraft:ritual",
 *                 "crown": "jugcraft:spire_crown_lantern", "upkeep": {"item": "minecraft:glowstone_dust", "count": 4, "ley": 8},
 *                 "field": {"kind": "illumination", "radius": 24, "count": 4}}
 * </pre>
 * Refused: unknown fields, numbers outside their limits, a phase asking for nothing, two phases with one id, no crown
 * phase or two, the rite asked for twice. {@link #check} then refuses references to things that do not exist.
 */
public final class WonderParser {
	public static final int MAX_PHASES = 6;
	public static final int MAX_PRACTICES = 8;
	public static final int MAX_SUSTAIN = 7;
	public static final int MAX_ATTENDANCE = 28;
	public static final int MIN_PULSE = 20;
	public static final int MAX_PULSE = 1200;
	public static final int MAX_RITE_RANGE = 16;
	public static final int MAX_RADIUS = 32;
	public static final int MAX_COUNT = 8;
	public static final int MAX_UPKEEP = 64;
	public static final int MAX_LEY = 64;
	public static final int MAX_REQUIRES = 6;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable SpireDefinition wonder(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "wonder");
			Json.only(object, "schema", "heart", "stage", "rite", "rite_range", "attendance_days", "pulse", "phases");
			Json.schema(object);
			List<SpireDefinition.Phase> phases = new ArrayList<>();
			Set<String> ids = new HashSet<>();
			int crowns = 0;
			int rites = 0;
			for (JsonElement element : Json.array(object, "phases")) {
				JsonObject phase = Json.object(element, "phase");
				Json.only(phase, "id", "structure", "practices", "rite", "sustain");
				String structure = phase.has("structure") ? Json.string(phase, "structure") : "";
				if (!structure.isEmpty() && !structure.equals(SpireDefinition.CROWN)) {
					Json.checkId(structure, "structure");
				}
				SpireDefinition.Phase read = new SpireDefinition.Phase(Json.name(phase, "id"), structure,
						Json.optional(phase, "practices", 0, MAX_PRACTICES, 0), Json.bool(phase, "rite", false), Json.optional(phase, "sustain", 0, MAX_SUSTAIN, 0));
				if (read.structure().isEmpty() && read.practices() == 0 && !read.rite() && read.sustain() == 0) {
					throw new Json.Invalid("phase " + read.id() + " asks for nothing");
				}
				if (!ids.add(read.id())) {
					throw new Json.Invalid("phase " + read.id() + " appears twice");
				}
				crowns += read.structure().equals(SpireDefinition.CROWN) ? 1 : 0;
				rites += read.rite() ? 1 : 0;
				phases.add(read);
			}
			if (phases.isEmpty() || phases.size() > MAX_PHASES) {
				throw new Json.Invalid("phases: 1 to " + MAX_PHASES);
			}
			if (crowns != 1 || rites > 1) {
				throw new Json.Invalid("one phase has the crown, and at most one asks for the rite");
			}
			return new SpireDefinition(id, Json.id(object, "heart"), Json.name(object, "stage"), Json.id(object, "rite"),
					Json.range(object, "rite_range", 1, MAX_RITE_RANGE), Json.range(object, "attendance_days", 1, MAX_ATTENDANCE),
					Json.range(object, "pulse", MIN_PULSE, MAX_PULSE), phases);
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("wonder " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable SpireConfiguration configuration(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "configuration");
			Json.only(object, "schema", "wonder", "tradition", "requires", "practice", "crown", "upkeep", "field");
			Json.schema(object);
			List<Definitions.Requirement> requires = new ArrayList<>();
			for (JsonElement element : Json.array(object, "requires")) {
				JsonObject requirement = Json.object(element, "requirement");
				Json.only(requirement, "research", "state");
				ResearchState state = ResearchState.fromId(Json.string(requirement, "state"));
				if (state == null || state == ResearchState.NONE) {
					throw new Json.Invalid("state " + Json.string(requirement, "state") + " is not a research state");
				}
				requires.add(new Definitions.Requirement(Json.id(requirement, "research"), state));
			}
			if (requires.isEmpty() || requires.size() > MAX_REQUIRES) {
				throw new Json.Invalid("requires: 1 to " + MAX_REQUIRES);
			}
			JsonObject upkeep = Json.object(Json.member(object, "upkeep"), "upkeep");
			Json.only(upkeep, "item", "count", "ley");
			JsonObject field = Json.object(Json.member(object, "field"), "field");
			Json.only(field, "kind", "radius", "count");
			FieldKind kind = FieldKind.fromId(Json.string(field, "kind"));
			if (kind == null) {
				throw new Json.Invalid("field kind " + Json.string(field, "kind") + " is not illumination, growth or focus");
			}
			return new SpireConfiguration(id, Json.id(object, "wonder"), Json.name(object, "tradition"), requires, Json.id(object, "practice"),
					Json.id(object, "crown"), Json.id(upkeep, "item"), Json.range(upkeep, "count", 1, MAX_UPKEEP), Json.range(upkeep, "ley", 0, MAX_LEY),
					kind, Json.range(field, "radius", 1, MAX_RADIUS), Json.range(field, "count", 1, MAX_COUNT));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("wonder configuration " + id + ": " + problem.getMessage());
			return null;
		}
	}

	/**
	 * What does not hold together: a configuration of an unknown wonder; a structure that does not exist or is not built
	 * around the wonder's heart; a rite that is not a ritual; research or a practice nobody can have; a tradition no
	 * research belongs to; a stage that is not one; a wonder with fewer than two configurations.
	 */
	public static List<String> check(Map<String, SpireDefinition> wonders, Collection<SpireConfiguration> configurations,
			Map<String, StructurePattern> structures, Map<String, RitualDefinition> rituals, Map<String, Definitions.Research> research,
			Set<String> practices, Set<String> stages) {
		List<String> problems = new ArrayList<>();
		for (SpireDefinition wonder : wonders.values()) {
			if (!rituals.containsKey(wonder.rite())) {
				problems.add("wonder " + wonder.id() + ": its rite " + wonder.rite() + " is no ritual");
			}
			if (!stages.contains(wonder.stage())) {
				problems.add("wonder " + wonder.id() + ": " + wonder.stage() + " is no stage");
			}
			for (SpireDefinition.Phase phase : wonder.phases()) {
				if (!phase.structure().isEmpty() && !phase.structure().equals(SpireDefinition.CROWN)) {
					problems.addAll(structure(wonder, phase.structure(), structures));
				}
			}
			long count = configurations.stream().filter(configuration -> configuration.wonder().equals(wonder.id())).count();
			if (count < 2) {
				problems.add("wonder " + wonder.id() + ": " + count + " configuration(s); a wonder offers at least two");
			}
		}
		Set<String> traditions = new HashSet<>();
		for (Definitions.Research entry : research.values()) {
			traditions.add(entry.tradition());
		}
		for (SpireConfiguration configuration : configurations) {
			SpireDefinition wonder = wonders.get(configuration.wonder());
			String where = "wonder configuration " + configuration.id() + ": ";
			if (wonder == null) {
				problems.add(where + "its wonder " + configuration.wonder() + " does not exist");
				continue;
			}
			problems.addAll(structure(wonder, configuration.crown(), structures).stream().map(problem -> where + problem).toList());
			if (!traditions.contains(configuration.tradition())) {
				problems.add(where + "no research belongs to the tradition " + configuration.tradition());
			}
			if (!practices.contains(configuration.practice())) {
				problems.add(where + configuration.practice() + " is no practice");
			}
			for (Definitions.Requirement requirement : configuration.requires()) {
				Definitions.Research entry = research.get(requirement.research());
				if (entry == null || !entry.states().containsKey(requirement.state())) {
					problems.add(where + "needs " + requirement.research() + " " + requirement.state().id() + ", which does not exist");
				}
			}
		}
		return problems;
	}

	private static List<String> structure(SpireDefinition wonder, String id, Map<String, StructurePattern> structures) {
		StructurePattern pattern = structures.get(id);
		if (pattern == null) {
			return List.of("structure " + id + " does not exist");
		}
		if (!pattern.anchor().equals(wonder.heart())) {
			return List.of("structure " + id + " is built around " + pattern.anchor() + ", not " + wonder.heart());
		}
		return List.of();
	}
}
