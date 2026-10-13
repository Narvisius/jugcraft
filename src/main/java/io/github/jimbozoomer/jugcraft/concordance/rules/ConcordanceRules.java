package io.github.jimbozoomer.jugcraft.concordance.rules;

import com.google.gson.JsonElement;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Ingredient;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Preparation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Property;
import io.github.jimbozoomer.jugcraft.concordance.compose.Authored;
import io.github.jimbozoomer.jugcraft.concordance.compose.Catalog;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Composition;
import io.github.jimbozoomer.jugcraft.concordance.compose.CompositionParser;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.celestial.CelestialCatalog;
import io.github.jimbozoomer.jugcraft.concordance.celestial.CelestialParser;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.crimson.CrimsonCatalog;
import io.github.jimbozoomer.jugcraft.concordance.crimson.CrimsonParser;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Rite;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Affix;
import io.github.jimbozoomer.jugcraft.concordance.artifice.ArtificeCatalog;
import io.github.jimbozoomer.jugcraft.concordance.artifice.ArtificeParser;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Gem;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Rune;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Substrate;
import io.github.jimbozoomer.jugcraft.concordance.conclave.CommissionDefinition;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ConclaveCatalog;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ConclaveParser;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectDefinition;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.EquivalenceCatalog;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.EquivalenceParser;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Material;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Transmutation;
import io.github.jimbozoomer.jugcraft.concordance.hex.CurseDefinition;
import io.github.jimbozoomer.jugcraft.concordance.hex.HexCatalog;
import io.github.jimbozoomer.jugcraft.concordance.hex.HexParser;
import io.github.jimbozoomer.jugcraft.concordance.progression.PracticeGate;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionCatalog;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionGraph;
import io.github.jimbozoomer.jugcraft.concordance.progression.ProgressionParser;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicCatalog;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicDefinition;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicParser;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireConfiguration;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireDefinition;
import io.github.jimbozoomer.jugcraft.concordance.wonder.WonderCatalog;
import io.github.jimbozoomer.jugcraft.concordance.wonder.WonderParser;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerCatalog;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerParser;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Disturbance;
import io.github.jimbozoomer.jugcraft.concordance.ecology.EcologyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.ecology.EcologyParser;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.resource.ConversionTable;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * One validated set of Concordance definitions: what the server enforces until the next data reload. Built from
 * parsed files by {@link #build}, which drops anything whose references do not resolve (a prerequisite, unlock or
 * research that does not exist, or a prerequisite cycle) and records why in {@link #problems()}. Immutable, so a reload
 * swaps it whole and a definition never changes under a running operation.
 */
public final class ConcordanceRules {
	public static final ConcordanceRules EMPTY = new ConcordanceRules(Map.of(), Map.of(), Map.of(), ConversionTable.EMPTY, Catalog.EMPTY,
			Map.of(), Map.of(), Map.of(), AlchemyCatalog.EMPTY, EcologyCatalog.EMPTY, CelestialCatalog.EMPTY, CrimsonCatalog.EMPTY, WorkerCatalog.EMPTY, ArtificeCatalog.EMPTY,
			RelicCatalog.EMPTY, EquivalenceCatalog.EMPTY, HexCatalog.EMPTY, ConclaveCatalog.EMPTY, ProgressionCatalog.EMPTY,
			WonderCatalog.EMPTY, List.of());

	private final Map<String, Definitions.Research> research;
	private final Map<String, Definitions.Invocation> invocations;
	private final Map<String, Definitions.Working> workings;
	private final ConversionTable conversions;
	private final Catalog catalog;
	private final Map<String, Map<String, Authored>> authored;
	private final Map<String, StructurePattern> structures;
	private final Map<String, RitualDefinition> rituals;
	private final AlchemyCatalog alchemy;
	private final EcologyCatalog ecology;
	private final CelestialCatalog celestial;
	private final CrimsonCatalog crimson;
	private final WorkerCatalog workers;
	private final ArtificeCatalog artifice;
	private final RelicCatalog relics;
	private final EquivalenceCatalog equivalence;
	private final HexCatalog hexes;
	private final ConclaveCatalog conclave;
	private final ProgressionCatalog progression;
	private final WonderCatalog wonders;
	private final List<String> problems;
	private final Map<String, Definitions.Invocation> bySpell = new HashMap<>();

	private ConcordanceRules(Map<String, Definitions.Research> research, Map<String, Definitions.Invocation> invocations,
			Map<String, Definitions.Working> workings, ConversionTable conversions, Catalog catalog,
			Map<String, Map<String, Authored>> authored, Map<String, StructurePattern> structures, Map<String, RitualDefinition> rituals,
			AlchemyCatalog alchemy, EcologyCatalog ecology, CelestialCatalog celestial, CrimsonCatalog crimson, WorkerCatalog workers,
			ArtificeCatalog artifice, RelicCatalog relics, EquivalenceCatalog equivalence, HexCatalog hexes, ConclaveCatalog conclave,
			ProgressionCatalog progression, WonderCatalog wonders, List<String> problems) {
		this.research = research;
		this.invocations = invocations;
		this.workings = workings;
		this.conversions = conversions;
		this.catalog = catalog;
		this.authored = authored;
		this.structures = structures;
		this.rituals = rituals;
		this.alchemy = alchemy;
		this.ecology = ecology;
		this.celestial = celestial;
		this.crimson = crimson;
		this.workers = workers;
		this.artifice = artifice;
		this.relics = relics;
		this.equivalence = equivalence;
		this.hexes = hexes;
		this.conclave = conclave;
		this.progression = progression;
		this.wonders = wonders;
		this.problems = problems;
		for (Definitions.Invocation invocation : invocations.values()) {
			bySpell.put(invocation.spell(), invocation);
		}
	}

	public Map<String, Definitions.Research> research() {
		return research;
	}

	public Map<String, Definitions.Invocation> invocations() {
		return invocations;
	}

	public Map<String, Definitions.Working> workings() {
		return workings;
	}

	/** The explicit conversions between resource types; nothing converts without one. */
	public ConversionTable conversions() {
		return conversions;
	}

	/** The composition grammar: components and instruments. */
	public Catalog catalog() {
		return catalog;
	}

	/** Ritual structures, by id. */
	public Map<String, StructurePattern> structures() {
		return structures;
	}

	/** Rituals, by id; each names a loaded structure and research state. */
	public Map<String, RitualDefinition> rituals() {
		return rituals;
	}

	public @Nullable StructurePattern structure(String id) {
		return structures.get(id);
	}

	public @Nullable RitualDefinition ritual(String id) {
		return rituals.get(id);
	}

	/** The rituals performed at an anchor block (by block id), in id order. */
	public List<RitualDefinition> ritualsAt(String anchor) {
		List<RitualDefinition> out = new ArrayList<>();
		for (RitualDefinition ritual : rituals.values()) {
			StructurePattern pattern = structures.get(ritual.structure());
			if (pattern != null && pattern.anchor().equals(anchor)) {
				out.add(ritual);
			}
		}
		return out;
	}

	/** Alchemy's ingredients, preparations and properties (roadmap step 13). */
	public AlchemyCatalog alchemy() {
		return alchemy;
	}

	/** Organisms and disturbance sources (roadmap step 14). */
	public EcologyCatalog ecology() {
		return ecology;
	}

	/** Celestial patterns (roadmap step 15). */
	public CelestialCatalog celestial() {
		return celestial;
	}

	/** Offering rites (roadmap step 16). */
	public CrimsonCatalog crimson() {
		return crimson;
	}

	/** Familiar, spirit and construct definitions (roadmap step 17). */
	public WorkerCatalog workers() {
		return workers;
	}

	/** Substrates, gems, runes and affixes (roadmap step 19). */
	public ArtificeCatalog artifice() {
		return artifice;
	}

	/** Relics and the contexts they work in (roadmap step 20). */
	public RelicCatalog relics() {
		return relics;
	}

	/** Catalogued materials and the declared conversion graph, with their audit (roadmap step 21). */
	public EquivalenceCatalog equivalence() {
		return equivalence;
	}

	/** Every curse (roadmap step 22). */
	public HexCatalog hexes() {
		return hexes;
	}

	/** The Starbound Conclave's commissions and projects (roadmap step 23). */
	public ConclaveCatalog conclave() {
		return conclave;
	}

	/** The stages, the practice gates and the progression graph built from these rules (roadmap step 24). */
	public ProgressionCatalog progression() {
		return progression;
	}

	/** The wonders players raise and keep, and their configurations (roadmap step 25). */
	public WonderCatalog wonders() {
		return wonders;
	}

	public List<String> problems() {
		return problems;
	}

	public Definitions.@Nullable Research research(String id) {
		return research.get(id);
	}

	public Definitions.@Nullable Invocation invocation(String id) {
		return invocations.get(id);
	}

	/** The invocation that casts this Spell Engine spell, or null if it is not a Concordance spell. */
	public Definitions.@Nullable Invocation invocationForSpell(String spell) {
		return bySpell.get(spell);
	}

	public Definitions.@Nullable Working working(String id) {
		return workings.get(id);
	}

	/**
	 * An invocation as compiled for an instrument (by instrument id), or null if it does not fit that instrument. Every
	 * loaded invocation fits at least one.
	 */
	public @Nullable Authored authored(String invocation, String instrument) {
		Map<String, Authored> forms = authored.get(invocation);
		return forms == null ? null : forms.get(instrument);
	}

	/** An invocation's compiled forms, by instrument id (empty if it is not loaded). */
	public Map<String, Authored> authored(String invocation) {
		return authored.getOrDefault(invocation, Map.of());
	}

	/** Entries a player can start with no other research: there must be at least one. */
	public List<String> entryPoints() {
		List<String> out = new ArrayList<>();
		for (Definitions.Research entry : research.values()) {
			if (entry.requires().isEmpty()) {
				out.add(entry.id());
			}
		}
		return out;
	}

	/**
	 * The files under {@code data/<ns>/concordance/}: kind (research, invocation, working, conversion, component,
	 * instrument, structure, ritual, ingredient, preparation, property, organism, disturbance or pattern), id, content.
	 */
	public record Source(String kind, String id, JsonElement json) {
	}

	public static ConcordanceRules build(List<Source> sources) {
		RulesParser parser = new RulesParser();
		EcologyParser ecologyParser = new EcologyParser();
		CelestialParser celestialParser = new CelestialParser();
		Map<String, Pattern> patterns = new TreeMap<>();
		CrimsonParser crimsonParser = new CrimsonParser();
		Map<String, Rite> rites = new TreeMap<>();
		WorkerParser workerParser = new WorkerParser();
		Map<String, WorkerDefinition> workerDefinitions = new TreeMap<>();
		ArtificeParser artificeParser = new ArtificeParser();
		Map<String, Substrate> substrates = new TreeMap<>();
		Map<String, Gem> gems = new TreeMap<>();
		Map<String, Rune> runes = new TreeMap<>();
		Map<String, Affix> affixes = new TreeMap<>();
		RelicParser relicParser = new RelicParser();
		Map<String, RelicDefinition> relicDefinitions = new TreeMap<>();
		EquivalenceParser equivalenceParser = new EquivalenceParser();
		Map<String, Material> materials = new TreeMap<>();
		Map<String, Transmutation> transmutations = new TreeMap<>();
		HexParser hexParser = new HexParser();
		Map<String, CurseDefinition> curses = new TreeMap<>();
		ConclaveParser conclaveParser = new ConclaveParser();
		Map<String, CommissionDefinition> commissions = new TreeMap<>();
		Map<String, ProjectDefinition> projects = new TreeMap<>();
		ProgressionParser progressionParser = new ProgressionParser();
		Map<String, StageDefinition> stages = new TreeMap<>();
		Map<String, PracticeGate> practices = new TreeMap<>();
		WonderParser wonderParser = new WonderParser();
		Map<String, SpireDefinition> wonderDefinitions = new TreeMap<>();
		Map<String, SpireConfiguration> wonderConfigurations = new TreeMap<>();
		Map<String, Organism> organisms = new TreeMap<>();
		Map<String, Disturbance> disturbances = new TreeMap<>();
		Map<String, Definitions.Research> research = new TreeMap<>();
		Map<String, Definitions.Invocation> invocations = new TreeMap<>();
		Map<String, Definitions.Working> workings = new TreeMap<>();
		List<Conversion> conversionList = new ArrayList<>();
		Map<String, Component> components = new TreeMap<>();
		Map<String, Instrument> instruments = new TreeMap<>();
		Map<String, StructurePattern> structures = new TreeMap<>();
		Map<String, RitualDefinition> rituals = new TreeMap<>();
		Map<String, Ingredient> ingredients = new TreeMap<>();
		Map<String, Preparation> preparations = new TreeMap<>();
		Map<String, Property> properties = new TreeMap<>();
		List<String> problems = new ArrayList<>();
		for (Source source : sources) {
			switch (source.kind()) {
				case "research" -> {
					Definitions.Research entry = parser.research(source.id(), source.json());
					if (entry != null) {
						research.put(entry.id(), entry);
					}
				}
				case "invocation" -> {
					Definitions.Invocation entry = parser.invocation(source.id(), source.json());
					if (entry != null) {
						invocations.put(entry.id(), entry);
					}
				}
				case "working" -> {
					Definitions.Working entry = parser.working(source.id(), source.json());
					if (entry != null) {
						workings.put(entry.id(), entry);
					}
				}
				case "conversion" -> {
					Conversion conversion = parser.conversion(source.id(), source.json());
					if (conversion != null) {
						conversionList.add(conversion);
					}
				}
				case "component" -> {
					Component component = parser.component(source.id(), source.json());
					if (component != null) {
						components.put(component.id(), component);
					}
				}
				case "instrument" -> {
					Instrument instrument = parser.instrument(source.id(), source.json());
					if (instrument != null) {
						instruments.put(instrument.id(), instrument);
					}
				}
				case "structure" -> {
					StructurePattern structure = parser.structure(source.id(), source.json());
					if (structure != null) {
						structures.put(structure.id(), structure);
					}
				}
				case "ritual" -> {
					RitualDefinition ritual = parser.ritual(source.id(), source.json());
					if (ritual != null) {
						rituals.put(ritual.id(), ritual);
					}
				}
				case "ingredient" -> {
					Ingredient ingredient = parser.ingredient(source.id(), source.json());
					if (ingredient != null) {
						Ingredient same = ingredients.values().stream().filter(other -> other.item().equals(ingredient.item())).findFirst().orElse(null);
						if (same != null) {
							problems.add("ingredient " + ingredient.id() + ": " + ingredient.item() + " is already ingredient " + same.id());
						} else {
							ingredients.put(ingredient.id(), ingredient);
						}
					}
				}
				case "preparation" -> {
					Preparation preparation = parser.preparation(source.id(), source.json());
					if (preparation != null) {
						preparations.put(preparation.id(), preparation);
					}
				}
				case "property" -> {
					Property property = parser.property(source.id(), source.json());
					if (property != null) {
						properties.put(property.id(), property);
					}
				}
				case "organism" -> {
					Organism organism = ecologyParser.organism(source.id(), source.json());
					if (organism != null) {
						organisms.put(organism.id(), organism);
					}
				}
				case "disturbance" -> {
					Disturbance disturbance = ecologyParser.disturbance(source.id(), source.json());
					if (disturbance != null) {
						disturbances.put(disturbance.id(), disturbance);
					}
				}
				case "pattern" -> {
					Pattern pattern = celestialParser.pattern(source.id(), source.json());
					if (pattern != null) {
						patterns.put(pattern.id(), pattern);
					}
				}
				case "offering" -> {
					Rite rite = crimsonParser.rite(source.id(), source.json());
					if (rite != null) {
						rites.put(rite.id(), rite);
					}
				}
				case "worker" -> {
					WorkerDefinition worker = workerParser.worker(source.id(), source.json());
					if (worker != null) {
						workerDefinitions.put(worker.id(), worker);
					}
				}
				case "substrate" -> {
					Substrate substrate = artificeParser.substrate(source.id(), source.json());
					if (substrate != null) {
						substrates.put(substrate.id(), substrate);
					}
				}
				case "gem" -> {
					Gem gem = artificeParser.gem(source.id(), source.json());
					if (gem != null) {
						gems.put(gem.id(), gem);
					}
				}
				case "rune" -> {
					Rune rune = artificeParser.rune(source.id(), source.json());
					if (rune != null) {
						runes.put(rune.id(), rune);
					}
				}
				case "affix" -> {
					Affix affix = artificeParser.affix(source.id(), source.json());
					if (affix != null) {
						affixes.put(affix.id(), affix);
					}
				}
				case "relic" -> {
					RelicDefinition relic = relicParser.relic(source.id(), source.json());
					if (relic != null) {
						relicDefinitions.put(relic.id(), relic);
					}
				}
				case "material" -> {
					Material material = equivalenceParser.material(source.id(), source.json());
					if (material != null) {
						materials.put(source.id(), material);
					}
				}
				case "transmutation" -> {
					Transmutation transmutation = equivalenceParser.transmutation(source.id(), source.json());
					if (transmutation != null) {
						transmutations.put(transmutation.id(), transmutation);
					}
				}
				case "curse" -> {
					CurseDefinition curse = hexParser.curse(source.id(), source.json());
					if (curse != null) {
						curses.put(curse.id(), curse);
					}
				}
				case "commission" -> {
					CommissionDefinition commission = conclaveParser.commission(source.id(), source.json());
					if (commission != null) {
						commissions.put(commission.id(), commission);
					}
				}
				case "project" -> {
					ProjectDefinition project = conclaveParser.project(source.id(), source.json());
					if (project != null) {
						projects.put(project.id(), project);
					}
				}
				case "stage" -> {
					StageDefinition stage = progressionParser.stage(source.id(), source.json());
					if (stage != null) {
						stages.put(stage.id(), stage);
					}
				}
				case "practice" -> {
					PracticeGate gate = progressionParser.practice(source.id(), source.json());
					if (gate != null) {
						practices.put(gate.activity(), gate);
					}
				}
				case "wonder" -> {
					SpireDefinition wonder = wonderParser.wonder(source.id(), source.json());
					if (wonder != null) {
						wonderDefinitions.put(wonder.id(), wonder);
					}
				}
				case "wonder_configuration" -> {
					SpireConfiguration configuration = wonderParser.configuration(source.id(), source.json());
					if (configuration != null) {
						wonderConfigurations.put(configuration.id(), configuration);
					}
				}
				default -> problems.add(source.id() + ": unknown kind of Concordance file \"" + source.kind()
						+ "\" (expected research, invocation, working, conversion, component, instrument, structure, ritual, ingredient, "
						+ "preparation, property, organism, disturbance, pattern, offering, worker, substrate, gem, rune, affix, relic, material, transmutation, "
						+ "curse, commission, project, stage, practice, wonder or wonder_configuration)");
			}
		}
		problems.addAll(0, parser.problems());
		problems.addAll(ecologyParser.problems());
		problems.addAll(celestialParser.problems());
		problems.addAll(crimsonParser.problems());
		problems.addAll(workerParser.problems());
		problems.addAll(artificeParser.problems());
		problems.addAll(ArtificeParser.check(substrates, gems, runes, affixes));
		problems.addAll(relicParser.problems());
		Map<String, String> relicItems = new TreeMap<>();
		for (RelicDefinition relic : relicDefinitions.values()) {
			String previous = relicItems.put(relic.item(), relic.id());
			if (previous != null) {
				problems.add("relics " + previous + " and " + relic.id() + " are both " + relic.item());
			}
		}
		// The equivalence catalogue keeps its own problems too: while it has any, the Assayer's Scale weighs nothing.
		List<String> equivalenceProblems = new ArrayList<>(equivalenceParser.problems());
		equivalenceProblems.addAll(EquivalenceParser.check(materials, transmutations));
		problems.addAll(equivalenceProblems);
		problems.addAll(hexParser.problems());
		problems.addAll(HexParser.check(curses));
		problems.addAll(conclaveParser.problems());
		java.util.Set<String> traditions = new java.util.TreeSet<>();
		for (Definitions.Research entry : research.values()) {
			traditions.add(entry.tradition());
		}
		problems.addAll(ConclaveParser.check(commissions.values(), projects.values(), traditions, research.keySet()));
		// Prerequisites must exist and must not loop; entries in a loop could never be started.
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Definitions.Research entry : List.copyOf(research.values())) {
				for (Definitions.Requirement requirement : entry.requires()) {
					Definitions.Research needed = research.get(requirement.research());
					if (needed == null || !needed.states().containsKey(requirement.state())) {
						problems.add("research " + entry.id() + ": requires " + requirement.research() + " "
								+ requirement.state().id() + ", which does not exist");
						research.remove(entry.id());
						changed = true;
						break;
					}
				}
			}
		}
		for (String cycle : cycles(research)) {
			problems.add("research " + cycle + ": its prerequisites lead back to itself");
			research.remove(cycle);
		}
		// A component is learnt from a research state like an invocation; one naming none could never be used.
		for (Component component : List.copyOf(components.values())) {
			Definitions.Research entry = research.get(component.requires().research());
			if (entry == null || !entry.states().containsKey(component.requires().state())) {
				problems.add("component " + component.id() + ": needs " + component.requires().research() + " "
						+ component.requires().state().id() + ", which does not exist");
				components.remove(component.id());
			}
		}
		Catalog catalog = new Catalog(components, instruments);
		// Invocations need a research state to learn them from, and a composition that compiles.
		Map<String, Map<String, Authored>> authored = new TreeMap<>();
		Map<String, String> signatures = new HashMap<>();
		for (Definitions.Invocation invocation : List.copyOf(invocations.values())) {
			Definitions.Research entry = research.get(invocation.research());
			if (entry == null || !entry.states().containsKey(invocation.state())) {
				problems.add("invocation " + invocation.id() + ": learnt from " + invocation.research() + " "
						+ invocation.state().id() + ", which does not exist");
				invocations.remove(invocation.id());
				continue;
			}
			Map<String, Authored> forms = compile(invocation, catalog, problems);
			if (forms == null) {
				invocations.remove(invocation.id());
				continue;
			}
			// Two invocations that select and do the same would be one spell with two prices.
			String signature = forms.values().iterator().next().plan().signature();
			String same = signatures.putIfAbsent(signature, invocation.id());
			if (same != null) {
				problems.add("invocation " + invocation.id() + ": does what " + same + " does (" + signature + ")");
				invocations.remove(invocation.id());
				continue;
			}
			authored.put(invocation.id(), Collections.unmodifiableMap(forms));
		}
		ConversionTable conversions = ConversionTable.build(conversionList);
		problems.addAll(conversions.problems());
		// A channel runs its conversion recipe: Focus into the Radiance a lantern holds, always losing.
		for (Definitions.Working working : List.copyOf(workings.values())) {
			if (working.type() != Definitions.WorkingType.CHANNEL) {
				continue;
			}
			Conversion conversion = working.conversion() == null ? null : conversions.get(working.conversion());
			if (conversion == null || !conversion.from().equals(ResourceType.FOCUS) || !conversion.to().equals(ResourceType.RADIANCE)) {
				problems.add("working " + working.id() + ": channels by conversion " + working.conversion()
						+ ", which does not exist or does not turn focus into essence/radiance");
				workings.remove(working.id());
			} else if (conversion.toAmount() >= conversion.fromAmount() || conversion.fromAmount() > 1000) {
				// Focus returns with time; a channel that gave back as much as it took would be a free conversion.
				problems.add("working " + working.id() + ": conversion " + conversion.id() + " must give less than it takes");
				workings.remove(working.id());
			} else {
				workings.put(working.id(), working.withChannel((int) conversion.fromAmount(), (int) conversion.toAmount()));
			}
		}
		for (Definitions.Working working : List.copyOf(workings.values())) {
			Definitions.Research entry = research.get(working.research());
			if (entry == null || !entry.states().containsKey(working.state())) {
				problems.add("working " + working.id() + ": needs " + working.research() + " " + working.state().id()
						+ ", which does not exist");
				workings.remove(working.id());
			}
		}
		// A ritual is built around a loaded structure and learnt from a research state like an invocation. One that
		// draws Ley Charge needs channels to draw it from.
		for (RitualDefinition ritual : List.copyOf(rituals.values())) {
			StructurePattern structure = structures.get(ritual.structure());
			Definitions.Research entry = research.get(ritual.research());
			String problem = null;
			if (structure == null) {
				problem = "is built around structure " + ritual.structure() + ", which does not exist";
			} else if (entry == null || !entry.states().containsKey(ritual.state())) {
				problem = "is learnt from " + ritual.research() + " " + ritual.state().id() + ", which does not exist";
			} else if (ritual.ley() > 0 && structure.channels().isEmpty()) {
				problem = "draws Ley Charge, but structure " + structure.id() + " has no channels";
			}
			if (problem != null) {
				problems.add("ritual " + ritual.id() + ": " + problem);
				rituals.remove(ritual.id());
			}
		}
		// Alchemy: ingredients are found by item; one preparation is how an ingredient is used as it comes, and no two
		// preparations share a tool (a tool makes one preparation).
		Map<String, Ingredient> byItem = new LinkedHashMap<>();
		for (Ingredient ingredient : ingredients.values()) {
			byItem.put(ingredient.item(), ingredient);
		}
		Set<String> tools = new HashSet<>();
		int plain = 0;
		for (Preparation preparation : List.copyOf(preparations.values())) {
			if (preparation.tool() == null) {
				plain++;
			} else if (!tools.add(preparation.tool())) {
				problems.add("preparation " + preparation.id() + ": another preparation is already made with " + preparation.tool());
				preparations.remove(preparation.id());
			}
		}
		if (!byItem.isEmpty() && plain != 1) {
			problems.add("alchemy: exactly one preparation must have no tool (how an ingredient goes in as it comes); found " + plain);
		}
		AlchemyCatalog alchemy = new AlchemyCatalog(byItem, preparations, properties);
		// Ecology: a block is one organism and an item plants one crop, or the garden could not tell which grows.
		Map<String, String> organismBlocks = new HashMap<>();
		Map<String, String> organismItems = new HashMap<>();
		for (Organism organism : List.copyOf(organisms.values())) {
			String sameBlock = organismBlocks.putIfAbsent(organism.block(), organism.id());
			String sameItem = organism.item() == null ? null : organismItems.putIfAbsent(organism.item(), organism.id());
			if (sameBlock != null || sameItem != null) {
				problems.add("organism " + organism.id() + ": its " + (sameBlock != null ? "block" : "item") + " is already organism "
						+ (sameBlock != null ? sameBlock : sameItem));
				organisms.remove(organism.id());
			}
		}
		EcologyCatalog ecology = new EcologyCatalog(organisms, disturbances);
		// Unlock lists and invoke evidence must name things that exist (a typo would silently teach nothing).
		for (Definitions.Research entry : research.values()) {
			for (Map.Entry<ResearchState, Definitions.Unlocks> unlock : entry.unlocks().entrySet()) {
				for (String id : unlock.getValue().invocations()) {
					if (!invocations.containsKey(id)) {
						problems.add("research " + entry.id() + ": unlocks unknown invocation " + id);
					}
				}
				for (String id : unlock.getValue().workings()) {
					if (!workings.containsKey(id)) {
						problems.add("research " + entry.id() + ": unlocks unknown working " + id);
					}
				}
				for (String id : unlock.getValue().rituals()) {
					if (!rituals.containsKey(id)) {
						problems.add("research " + entry.id() + ": unlocks unknown ritual " + id);
					}
				}
			}
			for (List<EvidenceRule> rules : entry.states().values()) {
				for (EvidenceRule rule : rules) {
					if (rule.kind() == EvidenceRule.Kind.INVOKE && !invocations.containsKey(rule.invocation())) {
						problems.add("research " + entry.id() + ": its evidence names unknown invocation " + rule.invocation());
					}
				}
			}
		}
		if (!research.isEmpty() && research.values().stream().noneMatch(entry -> entry.requires().isEmpty())) {
			problems.add("no research entry can be started without another: the Concordance has no way in");
		}
		// Roadmap step 24: the canonical progression graph, built from these rules (not written beside them), and what it
		// finds wrong with them: anything one player cannot reach from a fresh world, a cycle, a step needing a later stage.
		problems.addAll(progressionParser.problems());
		ProgressionGraph graph = stages.isEmpty() ? null
				: ProgressionGraph.build(research, invocations.values(), rituals.values(), stages.values(), practices.values());
		if (graph != null) {
			for (String problem : graph.problems()) {
				problems.add("progression: " + problem);
			}
		}
		// Roadmap step 25: the wonders and their configurations name structures round their heart, a ritual for their
		// rite, research, practices and a stage that exist.
		problems.addAll(wonderParser.problems());
		problems.addAll(WonderParser.check(wonderDefinitions, wonderConfigurations.values(), structures, rituals, research, practices.keySet(),
				stages.keySet()));
		return new ConcordanceRules(Collections.unmodifiableMap(new LinkedHashMap<>(research)),
				Collections.unmodifiableMap(new LinkedHashMap<>(invocations)),
				Collections.unmodifiableMap(new LinkedHashMap<>(workings)), conversions, catalog,
				Collections.unmodifiableMap(authored), Collections.unmodifiableMap(new LinkedHashMap<>(structures)),
				Collections.unmodifiableMap(new LinkedHashMap<>(rituals)), alchemy, ecology, new CelestialCatalog(patterns), new CrimsonCatalog(rites),
				new WorkerCatalog(workerDefinitions), new ArtificeCatalog(substrates, gems, runes, affixes),
				new RelicCatalog(relicDefinitions), new EquivalenceCatalog(EquivalenceParser.byItem(materials), transmutations, equivalenceProblems),
				new HexCatalog(curses), new ConclaveCatalog(commissions, projects), new ProgressionCatalog(stages, practices, graph),
				new WonderCatalog(wonderDefinitions, wonderConfigurations), List.copyOf(problems));
	}

	/**
	 * Compiles an invocation's composition, untuned and with each tuning, for every instrument it fits, and checks what
	 * its author declared: that it costs at least the Focus its composition would ({@link #mayCost}), and that no form
	 * spends more work or makes anything last longer than {@link Definitions.Invocation#work} and
	 * {@link Definitions.Invocation#persists}. Returns null (with the reasons recorded) if it cannot be loaded; a tuning
	 * that is not a modifier, changes nothing or fits no instrument is dropped on its own.
	 */
	private static @Nullable Map<String, Authored> compile(Definitions.Invocation invocation, Catalog catalog, List<String> problems) {
		String name = "invocation " + invocation.id() + ": ";
		CompositionParser.Parsed parsed = CompositionParser.parse(invocation.composition());
		Composition composition = parsed.composition();
		if (composition == null) {
			problems.add(name + "composition \"" + invocation.composition() + "\" does not parse: " + describe(parsed.problems()));
			return null;
		}
		Map<String, Composition> tunings = new LinkedHashMap<>();
		for (String id : invocation.tunings()) {
			Component modifier = catalog.component(id);
			Composition tuned = modifier == null || modifier.slot() != Slot.MODIFIER ? null : Compiler.tune(composition, modifier, catalog);
			if (tuned == null) {
				problems.add(name + "tuning " + id + " is not a modifier that changes it");
			} else {
				tunings.put(id, tuned);
			}
		}
		Map<String, Authored> forms = new TreeMap<>();
		List<Text> refused = List.of();
		for (Instrument instrument : catalog.instruments().values()) {
			Compiler.Compilation base = Compiler.compileAuthored(composition, catalog, instrument);
			if (base.plan() == null) {
				refused = base.problems();
				continue;
			}
			Map<String, Plan> tuned = new LinkedHashMap<>();
			for (Map.Entry<String, Composition> tuning : tunings.entrySet()) {
				Plan plan = Compiler.compileAuthored(tuning.getValue(), catalog, instrument).plan();
				if (plan != null && plan.signature().equals(base.plan().signature())) {
					tuned.put(tuning.getKey(), plan);
				}
			}
			forms.put(instrument.id(), new Authored(instrument.id(), base.plan(), tuned));
		}
		if (forms.isEmpty()) {
			problems.add(name + "fits no instrument: " + describe(refused));
			return null;
		}
		for (String tuning : tunings.keySet()) {
			if (forms.values().stream().noneMatch(form -> form.tunings().containsKey(tuning))) {
				problems.add(name + "tuning " + tuning + " fits no instrument");
			}
		}
		for (Authored form : forms.values()) {
			int least = form.plan().focus();
			if (!mayCost(invocation.focus(), invocation.masteredFocus(), least)) {
				problems.add(name + "costs less than its composition: at least " + least + " Focus, and "
						+ (least - (least + 3) / 4) + " once mastered, on " + form.instrument());
				return null;
			}
			List<Plan> plans = new ArrayList<>(form.tunings().values());
			plans.add(form.plan());
			for (Plan plan : plans) {
				if (plan.limits().work() > invocation.work() || plan.persists() > invocation.persists()) {
					problems.add(name + "\"" + plan.text() + "\" spends up to " + plan.limits().work() + " work and lasts up to "
							+ plan.persists() + " ticks on " + form.instrument() + "; it declares " + invocation.work() + " and "
							+ invocation.persists());
					return null;
				}
			}
		}
		return forms;
	}

	/**
	 * Whether an invocation may cost {@code focus} ({@code mastered} once its research is mastered) when its composition
	 * costs {@code composed}: never less than a player would pay to compose it, and mastery takes off at most a quarter
	 * (rounded up). So an invocation is never a cheaper way to the same effect than the grammar.
	 */
	public static boolean mayCost(int focus, int mastered, int composed) {
		return focus >= composed && mastered >= composed - (composed + 3) / 4;
	}

	private static String describe(List<Text> texts) {
		List<String> out = new ArrayList<>();
		for (Text text : texts) {
			out.add(text.key() + text.args());
		}
		return out.isEmpty() ? "no instrument is loaded" : String.join("; ", out);
	}

	/** Entries that are part of, or depend on, a prerequisite cycle. */
	private static Set<String> cycles(Map<String, Definitions.Research> research) {
		Set<String> done = new HashSet<>();
		Set<String> bad = new HashSet<>();
		for (String start : research.keySet()) {
			visit(start, research, new HashSet<>(), done, bad);
		}
		return bad;
	}

	private static boolean visit(String id, Map<String, Definitions.Research> research, Set<String> path, Set<String> done,
			Set<String> bad) {
		if (bad.contains(id)) {
			return true;
		}
		if (done.contains(id)) {
			return false;
		}
		if (!path.add(id)) {
			bad.add(id);
			return true;
		}
		boolean looped = false;
		Definitions.Research entry = research.get(id);
		if (entry != null) {
			for (Definitions.Requirement requirement : entry.requires()) {
				looped |= visit(requirement.research(), research, path, done, bad);
			}
		}
		path.remove(id);
		if (looped) {
			bad.add(id);
		} else {
			done.add(id);
		}
		return looped;
	}
}
