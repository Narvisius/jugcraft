package io.github.jimbozoomer.jugcraft.concordance.progression;

import io.github.jimbozoomer.jugcraft.concordance.conclave.Rank;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * The canonical progression graph (roadmap step 24, docs/features/arcane-concordance-progression.md), built from the
 * rules themselves rather than written beside them, so it cannot drift from what the game does:
 * <ul>
 * <li>each research state needs the state before it (the first, the entry's prerequisites) and one of its evidence
 * alternatives: an examination or a study (things to hand, checked on the server), an invocation, a practice (which
 * needs its {@link PracticeGate}) or another player's notes (social);</li>
 * <li>each invocation and ritual needs the research state that teaches it (a ritual for more than one participant is
 * social);</li>
 * <li>the Conclave's ranks need the oath (First Light understood) and its projects a Fellow;</li>
 * <li>each stage needs the stage before it and one of its routes.</li>
 * </ul>
 * Pure. It answers what one player alone can reach from a fresh world (a fixed point: a node is reached when all its
 * needs and one of its alternatives are), which nodes a cycle blocks, which steps need something from a later stage,
 * and whether each middle stage has more than one route of its own.
 */
public final class ProgressionGraph {
	public static final String NOTES = "social:notes";
	public static final String PROJECT = "project:conclave";
	public static final String OATH_RESEARCH = "jugcraft:first_light";

	/** A node: what it needs, all of it; groups of which one must wholly hold; whether it needs another player. */
	public record Node(String id, String stage, List<String> all, List<List<String>> any, boolean social) {
	}

	private final Map<String, Node> nodes = new LinkedHashMap<>();
	private final List<StageDefinition> stages;
	private final Map<String, Integer> order = new HashMap<>();
	private final Map<String, String> traditions = new HashMap<>();
	private final List<String> problems = new ArrayList<>();

	private ProgressionGraph(Collection<StageDefinition> stages) {
		this.stages = Stages.ordered(stages);
		for (StageDefinition stage : this.stages) {
			order.put(stage.id(), stage.order());
		}
	}

	public static String research(String id, ResearchState state) {
		return "research:" + id + "@" + state.id();
	}

	/** Builds the graph from the rules' research, invocations, rituals, stages and practice gates. */
	public static ProgressionGraph build(Map<String, Definitions.Research> research, Collection<Definitions.Invocation> invocations,
			Collection<RitualDefinition> rituals, Collection<StageDefinition> stages, Collection<PracticeGate> practices) {
		ProgressionGraph graph = new ProgressionGraph(stages);
		graph.nodes.put(NOTES, new Node(NOTES, "", List.of(), List.of(), true));
		Map<String, PracticeGate> gates = new HashMap<>();
		for (PracticeGate gate : practices) {
			gates.put(gate.activity(), gate);
		}
		for (Definitions.Research entry : research.values()) {
			graph.traditions.put(entry.id(), entry.tradition());
			String previous = null;
			for (ResearchState state : ResearchState.values()) {
				List<EvidenceRule> rules = entry.states().get(state);
				if (state == ResearchState.NONE || rules == null) {
					continue;
				}
				List<String> all = new ArrayList<>();
				if (previous == null) {
					for (Definitions.Requirement requirement : entry.requires()) {
						all.add(research(requirement.research(), requirement.state()));
					}
				} else {
					all.add(previous);
				}
				List<List<String>> any = new ArrayList<>();
				for (EvidenceRule rule : rules) {
					any.add(switch (rule.kind()) {
						case EXAMINE, STUDY -> List.of();
						case INVOKE -> List.of("invocation:" + rule.invocation());
						case PRACTICE -> List.of("practice:" + rule.activity());
						case NOTES -> List.of(NOTES);
					});
				}
				String id = research(entry.id(), state);
				graph.nodes.put(id, new Node(id, entry.stage(), all, any, false));
				previous = id;
			}
		}
		for (Definitions.Invocation invocation : invocations) {
			String id = "invocation:" + invocation.id();
			graph.nodes.put(id, new Node(id, stageOf(research, invocation.research()), List.of(research(invocation.research(), invocation.state())),
					List.of(), false));
		}
		for (RitualDefinition ritual : rituals) {
			String id = "ritual:" + ritual.id();
			graph.nodes.put(id, new Node(id, stageOf(research, ritual.research()), List.of(research(ritual.research(), ritual.state())), List.of(),
					ritual.participants() > 1));
		}
		for (PracticeGate gate : practices) {
			String id = "practice:" + gate.activity();
			List<String> all = new ArrayList<>();
			String stage = "";
			for (Definitions.Requirement requirement : gate.requires()) {
				all.add(research(requirement.research(), requirement.state()));
				String needed = stageOf(research, requirement.research());
				if (graph.later(needed, stage)) {
					stage = needed;
				}
			}
			if (!gate.stage().isEmpty()) {
				// A milestone that needs a stage reached first (a raised spire needs a Master to found it).
				all.add("stage:" + gate.stage());
				if (graph.later(gate.stage(), stage)) {
					stage = gate.stage();
				}
			}
			graph.nodes.put(id, new Node(id, stage, all, List.of(), false));
		}
		for (Rank rank : Rank.values()) {
			String id = "rank:" + rank.id;
			graph.nodes.put(id, new Node(id, "", List.of(research(OATH_RESEARCH, ResearchState.UNDERSTOOD)), List.of(), false));
		}
		graph.nodes.put(PROJECT, new Node(PROJECT, "", List.of("rank:" + Rank.FELLOW.id), List.of(), false));
		graph.analyse(research, gates);
		return graph;
	}

	private static String stageOf(Map<String, Definitions.Research> research, String id) {
		Definitions.Research entry = research.get(id);
		return entry == null ? "" : entry.stage();
	}

	/** Whether stage {@code a} comes after stage {@code b} ("" is before every stage). */
	private boolean later(String a, String b) {
		return order.getOrDefault(a, 0) > order.getOrDefault(b, 0);
	}

	public Map<String, Node> nodes() {
		return nodes;
	}

	public List<StageDefinition> stages() {
		return stages;
	}

	public List<String> problems() {
		return problems;
	}

	/** Everything one player reaches from a fresh world (with {@code alone} false, other players' notes and rituals too). */
	public Set<String> reachable(boolean alone) {
		return reachable(alone, null);
	}

	/** {@link #reachable(boolean)} with each stage allowed only its route {@code onlyRoute} where it has one by that id. */
	private Set<String> reachable(boolean alone, @Nullable Map<String, String> onlyRoute) {
		Set<String> reached = new HashSet<>();
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Node node : nodes.values()) {
				if (!reached.contains(node.id()) && !(alone && node.social()) && holds(node, reached)) {
					reached.add(node.id());
					changed = true;
				}
			}
			StageDefinition before = null;
			for (StageDefinition stage : stages) {
				String id = "stage:" + stage.id();
				if (!reached.contains(id) && (before == null || reached.contains("stage:" + before.id()))) {
					for (Route route : stage.routes()) {
						if (onlyRoute != null && onlyRoute.containsKey(stage.id()) && !onlyRoute.get(stage.id()).equals(route.id())) {
							continue;
						}
						if (routeHolds(route, reached)) {
							reached.add(id);
							changed = true;
							break;
						}
					}
				}
				before = stage;
			}
		}
		return reached;
	}

	private static boolean holds(Node node, Set<String> reached) {
		if (!reached.containsAll(node.all())) {
			return false;
		}
		if (node.any().isEmpty()) {
			return true;
		}
		for (List<String> alternative : node.any()) {
			if (reached.containsAll(alternative)) {
				return true;
			}
		}
		return false;
	}

	private boolean routeHolds(Route route, Set<String> reached) {
		for (String entry : route.research()) {
			int at = entry.indexOf('@');
			if (at < 0 || !reached.contains("research:" + entry)) {
				return false;
			}
		}
		if (route.mastered() > 0 && !counted(reached, ResearchState.MASTERED, route.mastered(), route.traditions())) {
			return false;
		}
		if (route.understood() > 0 && !counted(reached, ResearchState.UNDERSTOOD, route.understood(), route.mastered() > 0 ? 0 : route.traditions())) {
			return false;
		}
		if (!route.rank().isEmpty() && !reached.contains("rank:" + route.rank())) {
			return false;
		}
		if (route.projects() > 0 && !reached.contains(PROJECT)) {
			return false;
		}
		for (String milestone : route.milestones()) {
			if (!reached.contains("practice:" + milestone)) {
				return false;
			}
		}
		return true;
	}

	private boolean counted(Set<String> reached, ResearchState least, int need, int traditionsNeeded) {
		Set<String> entries = new HashSet<>();
		Set<String> across = new HashSet<>();
		for (String id : reached) {
			if (!id.startsWith("research:")) {
				continue;
			}
			int at = id.indexOf('@');
			ResearchState state = Stages.state(id.substring(at + 1));
			String entry = id.substring("research:".length(), at);
			if (state != null && state.atLeast(least) && entries.add(entry)) {
				across.add(traditions.getOrDefault(entry, ""));
			}
		}
		return entries.size() >= need && across.size() >= traditionsNeeded;
	}

	private void analyse(Map<String, Definitions.Research> research, Map<String, PracticeGate> gates) {
		// Every node names only things that exist (a stage is reached by its routes, not a node).
		for (Node node : nodes.values()) {
			for (String need : node.all()) {
				if (!nodes.containsKey(need) && !(need.startsWith("stage:") && order.containsKey(need.substring("stage:".length())))) {
					problems.add(node.id() + " needs " + need + ", which nothing provides");
				}
			}
			for (List<String> alternative : node.any()) {
				for (String need : alternative) {
					if (!nodes.containsKey(need)) {
						problems.add(node.id() + " has an alternative needing " + need + ", which nothing provides");
					}
				}
			}
		}
		for (Definitions.Research entry : research.values()) {
			if (!order.containsKey(entry.stage())) {
				problems.add("research " + entry.id() + ": its stage " + entry.stage() + " is not a defined stage");
			}
		}
		for (StageDefinition stage : stages) {
			for (Route route : stage.routes()) {
				for (String entry : route.research()) {
					if (!nodes.containsKey("research:" + entry)) {
						problems.add("stage " + stage.id() + " route " + route.id() + ": " + entry + " is no research state");
					}
				}
				if (!route.rank().isEmpty() && Stages.rank(route.rank()) == null) {
					problems.add("stage " + stage.id() + " route " + route.id() + ": no rank " + route.rank());
				}
				for (String milestone : route.milestones()) {
					if (!gates.containsKey(milestone)) {
						problems.add("stage " + stage.id() + " route " + route.id() + ": the milestone " + milestone + " has no gate");
					}
				}
			}
		}
		// No step needs something only a later stage provides.
		for (Node node : nodes.values()) {
			if (node.stage().isEmpty()) {
				continue;
			}
			for (String need : node.all()) {
				Node required = nodes.get(need);
				if (required != null && later(required.stage(), node.stage())) {
					problems.add(node.id() + " (" + node.stage() + ") needs " + need + " from the later stage " + required.stage());
				}
			}
			boolean early = node.any().isEmpty() || node.any().stream().anyMatch(alternative -> alternative.stream().map(nodes::get)
					.allMatch(required -> required == null || !later(required.stage(), node.stage())));
			if (!early) {
				problems.add(node.id() + " (" + node.stage() + ") can only be reached with something from a later stage");
			}
		}
		// What one player reaches alone from a fresh world; what a cycle blocks.
		Set<String> alone = reachable(true);
		for (Node node : nodes.values()) {
			if (node.id().startsWith("research:") && !alone.contains(node.id())) {
				problems.add(node.id() + " cannot be reached by one player alone: it needs " + firstMissing(node, alone));
			}
		}
		for (StageDefinition stage : stages) {
			if (!alone.contains("stage:" + stage.id())) {
				problems.add("stage " + stage.id() + " cannot be reached by one player alone");
			}
		}
		for (List<String> cycle : blockingCycles(alone)) {
			problems.add("circular: " + String.join(" > ", cycle));
		}
		// Each middle stage (after the first two, before the last) has routes of its own: at least two that each reach it.
		for (int i = 2; i + 1 < stages.size(); i++) {
			StageDefinition stage = stages.get(i);
			int working = 0;
			for (Route route : stage.routes()) {
				if (reachable(true, Map.of(stage.id(), route.id())).contains("stage:" + stage.id())) {
					working++;
				}
			}
			if (working < 2) {
				problems.add("stage " + stage.id() + " has " + working + " route(s) a player can take alone: a middle stage needs two");
			}
		}
	}

	private static String firstMissing(Node node, Set<String> reached) {
		for (String need : node.all()) {
			if (!reached.contains(need)) {
				return need;
			}
		}
		List<String> wanted = new ArrayList<>();
		for (List<String> alternative : node.any()) {
			for (String need : alternative) {
				if (!reached.contains(need)) {
					wanted.add(need);
				}
			}
		}
		return wanted.isEmpty() ? "nothing (unreachable through another node)" : "one of " + wanted;
	}

	/** Cycles among unreachable nodes (every edge, alternatives included): the ones that block progress. */
	private List<List<String>> blockingCycles(Set<String> reached) {
		Map<String, List<String>> edges = new HashMap<>();
		for (Node node : nodes.values()) {
			if (reached.contains(node.id())) {
				continue;
			}
			List<String> out = new ArrayList<>();
			for (String need : node.all()) {
				if (!reached.contains(need) && nodes.containsKey(need)) {
					out.add(need);
				}
			}
			for (List<String> alternative : node.any()) {
				for (String need : alternative) {
					if (!reached.contains(need) && nodes.containsKey(need)) {
						out.add(need);
					}
				}
			}
			edges.put(node.id(), out);
		}
		List<List<String>> cycles = new ArrayList<>();
		Set<String> done = new TreeSet<>();
		for (String start : new TreeSet<>(edges.keySet())) {
			if (done.contains(start)) {
				continue;
			}
			// A depth-first walk that reports the first cycle it closes from this start.
			Deque<String> path = new ArrayDeque<>();
			Set<String> onPath = new HashSet<>();
			List<String> cycle = walk(start, edges, path, onPath, new HashSet<>());
			if (cycle != null) {
				cycles.add(cycle);
				done.addAll(cycle);
			}
			done.add(start);
		}
		return cycles;
	}

	private static @Nullable List<String> walk(String at, Map<String, List<String>> edges, Deque<String> path, Set<String> onPath, Set<String> seen) {
		path.addLast(at);
		onPath.add(at);
		seen.add(at);
		for (String next : edges.getOrDefault(at, List.of())) {
			if (onPath.contains(next)) {
				List<String> cycle = new ArrayList<>();
				boolean in = false;
				for (String step : path) {
					in |= step.equals(next);
					if (in) {
						cycle.add(step);
					}
				}
				cycle.add(next);
				return cycle;
			}
			if (!seen.contains(next)) {
				List<String> found = walk(next, edges, path, onPath, seen);
				if (found != null) {
					return found;
				}
			}
		}
		path.removeLast();
		onPath.remove(at);
		return null;
	}
}
