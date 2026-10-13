"""The Arcane Concordance's player journey (roadmap step 31): docs/features/arcane-concordance-journey.md.

Three routes from a fresh world to an endgame project. Each has its own traditions, its own way through the middle
stages and its own Concord Spire configuration:

- cultivation: the Greenwardens' garden, its brews and its helpers, to a Verdant Spire;
- exploration and combat: the sky, the Crimson Vigil, relics and hexes in the field, to a Star Spire;
- crafting and infrastructure: circles, rings, the assay scale and porters, to a Lantern Spire.

A route is not a script the game enforces: a player may take any research in any order. A route is the smallest set of
research entries one player needs to reach the Architect stage that way. The game tests named in each step are where
the step's mechanics are exercised on a real server.

Nothing here is generated into the game. tools/check_mod_data.py (check_journey) checks that:

- one player alone, using only the route's own research entries, reaches the Architect stage through the route's
  stage routes and its own Spire, in the Overworld alone (the exploration route also goes to the Nether). Every other
  entry, and every mastery the route does not take, is removed from the progression graph
  (tools/concordance_progression.py) first;
- each route needs its own Spire's practice: without that practice the route stops short of Architect;
- the routes differ: three Spires, three Adept routes, at least two research entries and two practices each that no
  other route uses;
- the first success (understanding First Light and casting Kindle) is the data's: its specimens, the dark it asks
  for, the wand's recipe and Kindle's cost; and it needs nothing past the Practitioner stage and nothing from the
  Nether or the End;
- every game test a step names exists;
- the record's table is the one table() renders.

Run it to print the table and any problem: python3 tools/concordance_journey.py
"""
import copy
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "main" / "resources" / "data"
CONCORDANCE = DATA / "jugcraft" / "concordance"
GAMETEST = ROOT / "src" / "gametest" / "java"

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# The first success every route shares: First Light understood in the field, then Kindle cast from an Initiate's Wand.
FIRST_SUCCESS = {
    "dark": 4,                 # the light an examination in the dark may have (research first_light, Examination)
    "distinct": 3,             # different luminous specimens examined in the dark understand First Light
    "wand": {"A": f"#{MOD}:concordance/luminous_matter", "S": "minecraft:stick", "C": "minecraft:copper_ingot"},
    "kindle_focus": 4,         # Kindle's Focus cost (invocation kindle)
    "tests": ["examiningBySneakUse", "fieldEvidenceUnderstandsFirstLight", "kindleCastsThroughSpellEngine",
              "benchStudyUnderstandsFirstLight", "theFirstSuccessByOrdinaryControls"],
}

# Each route: the research entries it understands and the ones it masters, the stage routes it takes (Adept, Master),
# its Spire, and its steps in order: (stage reached, what the player does, the game tests that exercise it).
ROUTES = {
    "cultivation": {
        "name": "Cultivation",
        "understood": ["first_light", "verdant_husbandry", "alembic_arts", "binding_arts", "circle_lore"],
        "mastered": ["first_light", "verdant_husbandry", "alembic_arts", "binding_arts", "circle_lore"],
        "adept": "specialist", "master": "specialist", "spire": "verdant_spire",
        "steps": [
            ("practitioner", "Examine verdant specimens, study one at the bench (Verdant Husbandry understood); place "
             "Verdant Beds, wake them and plant Concordance crops where their habitat suits them",
             ["aGreenwardenWakesTheBeds", "aCropGrowsOnlyWithinItsNiche", "anEmptyBedStopsGrowth"]),
            ("practitioner", "Feed the beds: a Mendvetch fixes, the Mulch Maw composts, the Gauge reads the habitat",
             ["aFixerFeedsThePoorestBed", "theMawFeedsThePoorestBed", "theGaugeReadsTheHabitat"]),
            ("adept", "Master Verdant Husbandry (harvest each of the four Concordance crops by hand) and First Light "
             "(cast Kindle in eight different chunks): two entries mastered (the specialist's route)",
             ["aFreshPlayerClimbsEveryStage", "eachRouteClimbsItsOwnWay"]),
            ("adept", "Brew from the harvest in the Alembic Crucible; bind a Hearthling or a Gathering Shade to help; "
             "a Verdant Heart beats Verdance into a pylon, a Gleaner harvests for it",
             ["theSameProcessMakesTheSameDraught", "aFamiliarMendsByItsBond", "aSpiritKeepsToItsAgreement",
              "theHeartBeatsVerdanceIntoAPylon", "theGleanerHarvestsForVerdance"]),
            ("master", "Master the Alembic Arts (bottle three different outcomes), the Binding Arts (be served once by "
             "each kind of worker) and Circle Lore (complete rituals in three different chunks): five entries mastered "
             "in three traditions or more (the specialist's route)",
             ["eachRouteClimbsItsOwnWay"]),
            ("architect", "Found a Verdant Spire, raise it in four phases with the Kindling ritual and three days' "
             "bone meal and Ley; its field hastens the crops round it, each step paid by its bed",
             ["foundingNeedsTheMasterStageAndTheResearch", "phasesFinishInOrderAndTheSpireIsRaised",
              "aVerdantFieldHastensOnlyWhatItsBedsPay"]),
        ],
    },
    "exploration": {
        "name": "Exploration and combat",
        "understood": ["first_light", "celestial_attunement", "crimson_rites", "relic_lore", "sympathy", "circle_lore"],
        "mastered": ["first_light", "celestial_attunement", "crimson_rites", "relic_lore", "sympathy"],
        "adept": "generalist", "master": "specialist", "spire": "star_spire",
        # The Crimson Rites are observed from a Nether specimen, and the Thornheart Blade takes nether wart.
        "dimensions": ["", "nether"],
        "steps": [
            ("practitioner", "Carry Kindle, Aegis and Revelation into the field; examine celestial, crimson, relic and "
             "sympathetic specimens where they are found and study them at the bench",
             ["sixInvocationsOneForEachRole", "aegisShieldsAndIsTuned", "revelationStaysInsideItsLimits"]),
            ("adept", "Five entries understood in five traditions (the generalist's route)",
             ["eachRouteClimbsItsOwnWay"]),
            ("adept", "Watch the sky from an Orrery Observatory and attune an Astrolabe; make offerings and grow a "
             "Thornheart Blade by varied deeds against creatures",
             ["anObservatoryGathersEachOccurrenceOnce", "anAstrolabeAttunesWhileThePatternIsUp",
              "anOfferingKeepsItsLimits", "theBladeGrowsByVariedDeedsOnly", "aKillWithTheBladeCounts"]),
            ("adept", "Wear relics on the road (Wardlight, Owlsight Circlet, Stormglass, Hearthstone); take links by "
             "touch and lay bounded curses on creatures, which anyone can investigate and remedy",
             ["aRelicWorksOnlyInItsContexts", "relicsAreFoundInTrinketSlots", "linksAreTakenByTouchAndRespectTheRules",
              "cursesAreBoundedInvestigatedAndRemedied"]),
            ("master", "Master First Light (Kindle in eight chunks; Lance, Flashstep and Lanternward follow), Celestial "
             "Attunement (three different patterns observed), the Crimson Rites (a Thornheart Blade grown to its second "
             "stage), Relic Lore (relics serving in three different contexts) and Sympathy (three different curses "
             "cast): five entries mastered (the specialist's route)",
             ["lanceStrikesAndScalesWithSpellPower", "flashstepCarriesForward", "eachRouteClimbsItsOwnWay"]),
            ("architect", "Understand Circle Lore; found a Star Spire and raise it; its field restores its keepers' "
             "Focus and reveals hostile creatures",
             ["foundingNeedsTheMasterStageAndTheResearch", "eachConfigurationsFieldDoesItsWork"]),
        ],
    },
    "infrastructure": {
        "name": "Crafting and infrastructure",
        "understood": ["first_light", "circle_lore", "runesmithing", "assay", "binding_arts"],
        "mastered": ["first_light", "circle_lore", "runesmithing", "assay", "binding_arts"],
        "adept": "attuned", "master": "specialist", "spire": "lantern_spire",
        "steps": [
            ("practitioner", "Understand Circle Lore; raise Ley Pylons, fill them from lanterns or Jugcraft Energy and "
             "build a circle the anchor checks part by part",
             ["aPylonFillsFromLightAndElectricity", "theCircleIsCheckedPartByPart"]),
            ("adept", "Master First Light (Kindle in eight different chunks) with Circle Lore understood: the attuned "
             "route; the Adept's Attunement then makes an Adept's Wand",
             ["attunementCompletesOnce", "eachRouteClimbsItsOwnWay"]),
            ("adept", "Forge Resonant Rings at the Artificer's Bench; weigh and dissolve matter at the Assayer's "
             "Scale and form it again at a loss",
             ["aRingIsForgedAndSavedBeforeItIsShown", "gemsRunesAndBondsKeepTheirRules", "aStackIsWeighedThenDissolved",
              "formingCostsMoreThanDissolvingPays"]),
            ("adept", "Key a Clockwork Porter to a Courier Post; requests are fetched and delivered under reservations",
             ["aRequestIsFetchedAndDelivered", "simultaneousRequestsNeverClaimTheSameItems", "aPorterSaysWhyItStops"]),
            ("master", "Master Circle Lore (rituals in three different chunks), Runesmithing (three different "
             "enhancements), the Assay (five different materials dissolved) and the Binding Arts (served once by each "
             "kind of worker): five entries mastered (the specialist's route)",
             ["eachRouteClimbsItsOwnWay"]),
            ("architect", "Found a Lantern Spire and raise it; its upkeep can come by courier; its field lights the "
             "dark open air so nothing hostile spawns there",
             ["phasesFinishInOrderAndTheSpireIsRaised", "theHeartAsksACourierForItsUpkeep",
              "eachConfigurationsFieldDoesItsWork"]),
        ],
    },
}

# Cooperative play with shared infrastructure and separate ownership: what is shared, what stays one player's, and the
# server game tests that show it. These are single-server tests with mock players, not two clients.
COOPERATIVE = [
    ("A party raises a Conclave project together; each contributor's renown is their own",
     ["aPartyProjectSharesItsWork", "teachingCreditsTheAuthorOnce"]),
    ("Notes pass instructions between players, never observation or mastery",
     ["notesShareInstructionsNotExperience"]),
    ("A ritual for two gathers both and shields them; a participant who leaves stops it",
     ["theVigilGathersTwoAndShieldsThem", "aParticipantWhoLeavesStopsTheRitual"]),
    ("A spire's keepers share its upkeep; strangers neither count nor change it",
     ["attendanceLapsesAndReturns", "strangersNeitherCountNorChangeIt"]),
    ("Lecterns serve their owner or party; a bound relic serves only its player; Lanternward wards only the party",
     ["lecternsServeTheirOwnerOrParty", "aBoundRelicServesOnlyItsPlayer", "lanternwardWardsOnlyTheParty"]),
    ("Devices act only as their owner could: a porter, a spell's light and harm, a Spire Heart's store",
     ["aPorterTakesOnlyWhatItsKeeperCould", "lightGoesOnlyWhereItsCasterCouldBuild", "aSpireHeartsStoreCannotBeDrained"]),
    ("Simultaneous courier requests from two players never claim the same items",
     ["simultaneousRequestsNeverClaimTheSameItems"]),
]

# Mistakes a player can make on any route, and what puts them right.
RECOVERY = [
    ("A circle broken mid-ritual", "the offerings are released; a lost boundary lashes out at whoever broke it",
     ["aBrokenChannelReleasesTheOfferings", "aLostBoundaryLashesOut", "aBreakerAnswersForTheBacklash"]),
    ("A ritual started without enough Ley or Focus", "it is refused and takes nothing",
     ["refusalsTakeNothing", "aDryPylonStopsTheRitual"]),
    ("A broken anchor, crucible, post or Spire Heart", "it drops or returns what it held, once",
     ["aBrokenAnchorDropsItsOfferingsOnce", "aBrokenCrucibleDropsItsItems", "aBrokenPostSendsItsCargoBack",
      "aBrokenHeartLosesNothing"]),
    ("A spire whose upkeep runs short", "it rests, and is repaired",
     ["aDamagedSpireRestsAndIsRepaired"]),
    ("A request no longer wanted", "cancelling or recovering gives the items back",
     ["cancellingAndRecoveringGiveItemsBack"]),
    ("Salvaging the wrong ring", "salvage asks to be confirmed and never gains",
     ["salvageNeedsConfirmingAndNeverGains"]),
    ("Dying in a dream", "the escrow comes back once, and nothing is duplicated",
     ["anUnseenDeathRecoversTheEscrowOnce", "dreamsNeverDuplicatePossessions"]),
    ("Research forgotten by an operator's reset", "the stage reached is kept",
     ["aStageIsNeverLost"]),
    ("Not knowing what comes next", "the journal says what the next state and each route still need",
     ["metResearchSaysWhatComesNext", "eachRouteSaysWhatItStillNeeds"]),
]

STAGE_NAMES = {"initiate": "Initiate", "practitioner": "Practitioner", "adept": "Adept", "master": "Master",
               "architect": "Architect"}


def load_rules():
    """The research, invocations and rituals on disk, and what the progression graph needs at hand."""
    sys.path.insert(0, str(ROOT / "tools"))
    import concordance_progression as pg

    def folder(name):
        return {path.stem: json.loads(path.read_text(encoding="utf-8")) for path in sorted((CONCORDANCE / name).glob("*.json"))}
    return pg, folder("research"), folder("invocation"), folder("ritual"), pg.load_at_hand(DATA)


def walk(pg, research, invocations, rituals, at_hand, route, without_practice=False):
    """What one player alone reaches on {route} (a ROUTES value) from a fresh world, in the route's dimensions (the
    Overworld unless it names others): every research state the route does not take is removed from the graph, and the
    Spire milestone may only be had through the route's own configuration."""
    nodes = pg.graph(research, invocations, rituals, at_hand)
    without = set()
    for key in research:
        for state in pg.RESEARCH_STATES:
            taken = key in route["mastered"] if state == "mastered" else key in route["understood"]
            if not taken:
                without.add(pg.research_node(rid(key), state))
    configuration = json.loads((CONCORDANCE / "wonder_configuration" / f"{route['spire']}.json").read_text(encoding="utf-8"))
    practice = f"practice:{configuration['practice']}"
    nodes = copy.deepcopy(nodes)
    milestone = nodes[f"practice:{rid('spire_raised')}"]
    milestone["any"] = [group for group in milestone["any"] if practice in group]
    if without_practice:
        without.add(practice)
    return pg.reachable(nodes, research, alone=True, only_route={"adept": route["adept"], "master": route["master"],
                        "architect": "raised_spire"}, without=without, dimensions=set(route.get("dimensions", [""]))), practice


def practices(reached):
    return sorted(key.rsplit(":", 1)[1] for key in reached if key.startswith("practice:") and not key.endswith("spire_raised"))


def game_tests():
    """Every game test method in src/gametest."""
    names = set()
    for path in GAMETEST.rglob("*.java"):
        names.update(re.findall(r"public void (\w+)\((?:GameTestHelper|ClientGameTestContext)", path.read_text(encoding="utf-8")))
    return names


def problems():
    """What is wrong with the journey: an empty list when every route holds (see the module's description)."""
    found = []
    pg, research, invocations, rituals, at_hand = load_rules()
    seen = {}
    for key, route in ROUTES.items():
        for entry in route["understood"] + route["mastered"]:
            if entry not in research:
                found.append(f"journey {key}: {entry} is not a research entry")
        if not set(route["mastered"]) <= set(route["understood"]):
            found.append(f"journey {key}: a mastered entry must be understood first")
        reached, practice = walk(pg, research, invocations, rituals, at_hand, route)
        missing = [stage for stage in pg.STAGES if f"stage:{stage}" not in reached]
        if missing:
            found.append(f"journey {key}: one player alone on this route does not reach {missing}")
        blocked, _ = walk(pg, research, invocations, rituals, at_hand, route, without_practice=True)
        if "stage:architect" in blocked:
            found.append(f"journey {key}: the route reaches Architect without its own Spire's practice ({practice})")
        seen[key] = (set(route["mastered"]) | set(route["understood"]), set(practices(reached)))
    for key, route in ROUTES.items():
        others = [other for other in ROUTES if other != key]
        own_entries = seen[key][0] - set().union(*(seen[other][0] for other in others))
        own_practices = seen[key][1] - set().union(*(seen[other][1] for other in others))
        if len(own_entries) < 2 or len(own_practices) < 2:
            found.append(f"journey {key}: needs two research entries and two practices no other route uses "
                         f"({sorted(own_entries)}, {sorted(own_practices)})")
    for field in ("spire", "adept"):
        if len({route[field] for route in ROUTES.values()}) != len(ROUTES):
            found.append(f"journey: each route needs its own {field}")
    # The first success is the data's.
    first_light = research.get("first_light", {})
    rules = first_light.get("states", {}).get("understood", {}).get("any", [])
    if not any(rule.get("type") == "examine" and rule.get("max_light") == FIRST_SUCCESS["dark"]
               and rule.get("distinct") == FIRST_SUCCESS["distinct"] for rule in rules):
        found.append("journey: First Light is no longer understood by examining FIRST_SUCCESS's specimens in the dark")
    wand = json.loads((DATA / MOD / "recipe" / "initiate_wand.json").read_text(encoding="utf-8"))
    if wand.get("key") != FIRST_SUCCESS["wand"]:
        found.append(f"journey: the Initiate's Wand's recipe is {wand.get('key')}, not FIRST_SUCCESS's")
    if invocations.get("kindle", {}).get("focus") != FIRST_SUCCESS["kindle_focus"]:
        found.append(f"journey: Kindle no longer costs {FIRST_SUCCESS['kindle_focus']} Focus")
    nodes = pg.graph(research, invocations, rituals, at_hand)
    early = pg.reachable(nodes, research, cap="practitioner", dimensions={""})
    if f"invocation:{rid('kindle')}" not in early:
        found.append("journey: Kindle needs something past the Practitioner stage, or from the Nether or the End")
    # Every game test named exists.
    tests = game_tests()
    named = list(FIRST_SUCCESS["tests"])
    for route in ROUTES.values():
        for _stage, _text, names in route["steps"]:
            named += names
    for _text, names in COOPERATIVE:
        named += names
    for _mistake, _remedy, names in RECOVERY:
        named += names
    for name in sorted(set(named) - tests):
        found.append(f"journey: no game test {name}")
    return found


def table():
    """The record's generated part: the table of routes (what each takes, how it climbs, its Spire), each route's steps
    with the game tests that exercise them, cooperative play and recovery from mistakes."""
    pg, research, invocations, rituals, at_hand = load_rules()
    lines = ["| Route | Research mastered | Also understood | Adept by | Master by | Practices | Spire |",
             "|---|---|---|---|---|---|---|"]

    lang = json.loads((ROOT / "src" / "main" / "resources" / "assets" / MOD / "lang" / "en_us.json").read_text(encoding="utf-8"))

    def names(keys):
        return ", ".join(lang.get(f"research.{MOD}.{key}", key) for key in keys)
    for key, route in ROUTES.items():
        reached, _ = walk(pg, research, invocations, rituals, at_hand, route)
        understood_only = [entry for entry in route["understood"] if entry not in route["mastered"]]
        spire = route["spire"].replace("_", " ").title()
        lines.append(f"| {route['name']} | {names(route['mastered'])} | {names(understood_only) or 'none'} | "
                     f"{route['adept']} | {route['master']} | {', '.join(practices(reached))} | {spire} |")

    def tests(listed):
        return ", ".join(f"`{name}`" for name in listed)
    for route in ROUTES.values():
        lines += ["", f"### {route['name']}", ""]
        if route.get("dimensions", [""]) != [""]:
            lines.append("This route goes to the Nether as well as the Overworld.")
            lines.append("")
        for number, (stage, text, listed) in enumerate(route["steps"], 1):
            lines.append(f"{number}. **{STAGE_NAMES[stage]}.** {text}. Tests: {tests(listed)}.")
    lines += ["", "### Cooperative play", "", "| Shared, and what stays one player's | Server game tests |", "|---|---|"]
    for text, listed in COOPERATIVE:
        lines.append(f"| {text} | {tests(listed)} |")
    lines += ["", "### Recovery from mistakes", "", "| Mistake | What puts it right | Server game tests |", "|---|---|---|"]
    for mistake, remedy, listed in RECOVERY:
        lines.append(f"| {mistake} | {remedy} | {tests(listed)} |")
    return lines


if __name__ == "__main__":
    print("\n".join(table()))
    issues = problems()
    print("\n".join(issues) if issues else "journey: every route holds")
