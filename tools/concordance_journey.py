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
- the first loadout (tools/acquisition.py LOADOUT, Concordance Chunk A) is made by one player in the Overworld, each
  piece by its recipe or its bench working, with no research but First Light up to understood, and the Rime travel kit
  (Chunk B, B-10; tools/concordance_rime_kit.py) likewise, every piece by its recipe;
- the living workshop (WORKSHOP, Concordance Chunk C, C-10) holds from a manual start: one player alone, in the Overworld
  alone, with only the research its steps have understood so far and nothing mastered, makes every Concordance thing each
  step names (tools/acquisition.py WORKSHOP), works its practices and its rite with nothing past the Practitioner stage,
  and reaches the stage the step says; each step's own research is needed (without it something the step makes or works
  is out of reach); each step's switches are exactly those its recipes load under, the first step's only the
  Concordance's; every thing named is a row of tools/acquisition.py WORKSHOP or FACTORY and every row is named; and each
  product is brewed by its named process, from things the same player has, over a heat whose degrees lie in the band it
  stirs at;
- the pact hall (PACT, Concordance Chunk D) holds without Styx: one player alone, in the Overworld alone (the workshop
  hand's step also in the Nether, which it needs), with no structure loot, nothing else mastered and nothing past the
  Practitioner stage, reaches each step with the research it names (each step needing it) and makes every thing it
  names; every thing named is a row of tools/acquisition.py
  PACT_HALL, made under the Concordance's switch alone, and every row is named; the Open Hand Compact's materials and
  resonance and one of Stolas's offerings are had by the route; the workshop hand is sealed as SpiritAnchorBlock seals it;
  and every step names the server test that walks the whole route;
- the routes' "served once by each kind of worker" is the Binding Arts' mastery, as many kinds as the workers have, the
  workshop hand among the spirits;
- every game test a step names exists;
- the record's table is the one table() renders.

Run it to print the table and any problem: python3 tools/concordance_journey.py; with --check it prints only the
problems and exits 1 if there are any.
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
             "Scale and form it again at its essence value",
             ["aRingIsForgedAndSavedBeforeItIsShown", "gemsRunesAndBondsKeepTheirRules", "aStackIsWeighedThenDissolved",
              "formingSpendsExactValueWithoutSurcharge"]),
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

# Concordance Chunk C (C-10; 09:140, "The entire loop is survival-reachable from a manual start. A powered version
# improves convenience"): the living workshop. Not a stage route: what one player alone does, in the Overworld alone, from
# a manual start (crafted Mendvetch, a campfire crucible, lantern-filled pylons) to a powered crucible a porter supplies,
# and the garden's two Chunk C rules. Each step: the stage reached with the research understood so far and nothing
# mastered (five entries in five traditions are the generalist's Adept); the research it adds; what the player does; the
# feature switches its recipes load under; the things it makes or places (tools/acquisition.py WORKSHOP and FACTORY); the
# practices and the rite it works; and the server game tests that exercise it. Each product is brewed by the process named
# (tools/concordance_alchemy.py PRODUCTS) over the heat named.
WORKSHOP = {
    "name": "The living workshop",
    "products": {"stillroot_poultice": ("mendvetch", "minecraft:magma_block"),
                 "kiln_draught": ("sugar", "minecraft:campfire")},
    # What each form is bottled in (Java: CrucibleBlockEntity.formFor; tools/concordance_alchemy.py CONTAINERS).
    "containers": {"draught": "minecraft:glass_bottle", "salve": "minecraft:bowl"},
    "steps": [
        {"stage": "practitioner", "research": ["first_light", "alembic_arts"],
         "text": "Craft Mendvetch from wheat seeds, bone meal and an amethyst shard. Set an Alembic Crucible over a campfire "
                 "and another over a magma block; with the Alembic Arts understood, brew the Kiln Draught by hand (two "
                 "sugar in a bottle of water, stirred hot twice over the campfire) and the Stillroot Poultice (two "
                 "Mendvetch, stirred warm four times over the magma block), and record the poultice's process on a formula",
         "switches": ["concordance"], "things": ["mendvetch", "crucible", "formula"], "practices": ["alchemy"],
         "tests": ["theLivingWorkshopFromAManualStart", "aCampfireCrucibleStillWorks",
                   "aStillrootPoulticeRepeatsAndADirtyBatchHealsLess", "aKilnDraughtCarriesItsDrinkerAcrossTheSlag",
                   "aPoulticeOnAFriendIsCreditedForWhatItHeals"]},
        {"stage": "practitioner", "research": ["circle_lore"],
         "text": "With Circle Lore understood, build a Lesser Circle (a Circle Anchor, four Ley Pylons and eight Warding "
                 "Stones), fill each pylon from a Kindled Lantern and perform Root and Rivet on a Thallite plate with four "
                 "copper ingots and four Mendvetch: a Rooted Thallite Foundation",
         "switches": ["concordance", "machines", "thallite"],
         "things": ["lampwright_bench", "kindled_lantern", "ley_pylon", "circle_anchor", "warding_stone", "thallite_plate",
                    "rooted_thallite_foundation"],
         "practices": ["ritual"], "ritual": "root_and_rivet",
         "tests": ["theLivingWorkshopFromAManualStart", "aPylonFillsFromLightAndElectricity",
                   "anIdleCircleSaysEverythingARiteStillNeeds", "rootAndRivetAttunesOnePlateOnce",
                   "aDryPylonStopsRootAndRivetAndKeepsTheOfferings"]},
        {"stage": "practitioner", "research": [],
         "text": "Craft a Crucible Stand from the foundation, iron and copper and set the crucible on it; feed it JE from a "
                 "Coal Generator through Copper Cable and pipe water in from an Electric Pump (bronze plates for the pump "
                 "and its pipes are pressed at a Metal Press the generator powers): following the poultice's formula, the "
                 "stand heats the crucible for each stir and pays nothing while the crucible waits",
         "switches": ["concordance", "machines", "thallite", "tin", "zinc"],
         "things": ["crucible_stand", "coal_generator", "copper_cable", "machine_casing", "metal_press", "electric_pump",
                    "bronze_fluid_pipe"],
         "practices": ["alchemy"],
         "tests": ["theLivingWorkshopFromAManualStart", "aStandFollowsTheFormulasBands", "theFullChainRunsFromAPumpAndACable",
                   "aStandIdlesForAWaitingOrIncompatibleFormula", "aCableMeetsTheStandWhereItTakesPower"]},
        {"stage": "practitioner", "research": ["binding_arts"],
         "text": "With the Binding Arts understood, set down a Clockwork Porter and key it to a Courier Post beside a "
                 "lantern-filled pylon, which fuels it: the crucible asks the post for its formula's exact shortage, which "
                 "the porter brings from a chest by the post (a Peepo serves through its crucible port as well)",
         "switches": ["concordance"], "things": ["courier_post", "clockwork_porter", "porter_key"],
         "practices": ["worker_service"],
         "tests": ["theLivingWorkshopFromAManualStart", "aCrucibleAsksOnceForItsExactShortage",
                   "aCourierFillsACrucibleUntilItIsFull", "anUnloadedCrucibleWaitsAndLosesNothing"]},
        {"stage": "adept", "research": ["verdant_husbandry"],
         "text": "With Verdant Husbandry understood (five entries in five traditions: the generalist's Adept), lay Verdant "
                 "Beds, grow Dewmoss beside Gloamcap (a mixed bed) and graft Dewlit onto a Sunpetal's bed with a Dewmoss",
         "switches": ["concordance"], "things": ["verdant_bed", "sunpetal", "dewmoss", "gloamcap"],
         "practices": ["cultivation"],
         "tests": ["theLivingWorkshopFromAManualStart", "aBedSaysWhatItsPlantNeedsAndHowFar",
                   "aMixedBedGrowsFasterOnlyForAnotherKind", "restCompostAndTendingEaseStrain",
                   "aGraftTakesOnceAndConsumesItsDonor"]},
    ],
}

# Concordance Chunk D (D-03 to D-09; D-S1, D-S2, D-S3, D-S12): the pact hall, found through Styx or without him, to the
# workshop hand it unlocks. Not a stage route: what one player alone does in the Overworld alone, with the research each
# step names understood (its own needed: without it the step is out of reach), nothing else mastered, nothing past the
# Practitioner stage and no structure loot (Styx's flowers are his gift, so a feather stands in, and an amethyst shard
# keeps the pact); a step that names its dimensions may go there too (the workshop hand's Spirit Anchor takes a soul
# lantern, from the Nether). Each step: the research it adds, what the player does, the Concordance things it makes or
# places (tools/acquisition.py PACT_HALL), the graph nodes it reaches besides them and the server game tests that exercise it;
# PACT_TEST walks every step, in order, on one survival player. The sky step is no research state: it is the invitation's
# precondition, had through an observatory under an open night sky. The Open Hand Compact's resonance is had only from an
# observatory its player keeps, which only a Starwatcher does (tools/concordance_progression.py RESONANCE_SOURCES), so the
# route understands Celestial Attunement. The workshop hand is no research unlock (research unlocks name only
# invocations, workings and rituals; D-P72): SpiritAnchorBlock seals it for one who has mastered Stolas's Regard
# (tools/concordance_workers.py HAND_RESEARCH, which check_hand holds to the Java) and understands the Binding Arts, at a
# Spirit Anchor a Porter Key bound to their Courier Post, so the walk adds that seal (HAND) as PACT["hand"] says.
PACT_TEST = "thePactHallFromStyxToDeparture"
HAND = f"worker:{rid('workshop_hand')}"
PACT = {
    "name": "The pact hall",
    "hand": {"research": ["binding_arts"], "things": ["spirit_anchor", "porter_key", "courier_post"]},
    "steps": [
        {"research": ["first_light", "binding_arts"], "things": [],
         "text": "With the Binding Arts understood, examine a feather (or a Stolas Starflower, Ravenquill Lupine or "
                 "Amethyst Mourningbell, Styxhexenhammer's cuttings); Styx, where the world has him, tells each listener "
                 "their own next step and teaches nothing, and the journal's pacts section names the same step without him",
         "reaches": [f"research:{rid('stolas_regard')}@encountered"],
         "tests": ["styxHintsByTheListenersOwnProgress", "talkingGrantsNoResearch", "notesNeverStandInForStolasEvidence",
                   "thePactsSectionNamesTheSkyStep", PACT_TEST]},
        {"research": ["verdant_husbandry"], "things": ["verdant_bed", "sunpetal"],
         "text": "With Verdant Husbandry understood, lay a Verdant Bed, grow a Sunpetal (or any organism crop) and harvest "
                 "it ripe by hand",
         "reaches": [f"research:{rid('stolas_regard')}@observed"],
         "tests": ["styxHintsByTheListenersOwnProgress", "stolasRegardIsReachable", PACT_TEST]},
        {"research": ["assay"], "things": ["assayers_scale"],
         "text": "With the Assay understood, dissolve a sample at an Assayer's Scale: Stolas's Regard is understood, and no "
                 "research notes stand in for any of its evidence",
         "reaches": [f"research:{rid('stolas_regard')}@understood"],
         "tests": ["notesNeverStandInForStolasEvidence", "stolasRegardIsReachable", PACT_TEST]},
        {"research": [], "things": ["observatory"],
         "text": "Record a sky observation at an Orrery Observatory while a pattern is up and visible: the invitation's "
                 "precondition, no research, which Styx and the journal name apart",
         "reaches": ["encounter:night", "encounter:open_sky"],
         "tests": ["styxNamesTheSkyStepUntilOneIsRecorded", "aRecordedObservationCountsWithoutAWindow",
                   "aTurnedClockMakesNoNewClaim", "thePactsSectionNamesTheSkyStep", PACT_TEST]},
        {"research": ["celestial_attunement"], "things": ["astrolabe"],
         "text": "With Celestial Attunement understood, keep that observatory: it gathers Astral Resonance from each "
                 "pattern's occurrence under the open night sky, and fills your Astrolabe",
         "reaches": ["resource:astral_resonance"],
         "tests": ["anObservatoryGathersEachOccurrenceOnce", PACT_TEST]},
        {"research": [], "things": ["pact_perch"],
         "text": "Build a Pact Perch where you may build and invite Stolas: he arrives and names every term before anything "
                 "is spent; a refusal spends nothing and says why; accept, and the Open Hand Compact (its materials and "
                 "your Astrolabe's resonance) is spent once, in the call that seals the pact",
         "reaches": [],
         "tests": ["stolasExplainsBeforeAgreeing", "aPerchIsPlacedOnlyWhereItsPlacerMayBuild", "aRefusalSpendsNothing",
                   "acceptingSealsOnceAndSpendsOnce", PACT_TEST]},
        {"research": [], "things": [],
         "text": "Once a game day at the perch, Star-reckoning recalls a pattern you observed, paid from your Astrolabe, "
                 "under the one claim a master's recall makes; record an observation while the pact is active (its first "
                 "obligation): Stolas's Regard is mastered",
         "reaches": [f"practice:{rid('pact_service')}", f"research:{rid('stolas_regard')}@mastered"],
         "tests": ["stolasRecallsAnObservedPatternOncePerOccurrence", "aMastersRecallAndStolassShareOneClaim",
                   "theObligationMastersTheEntryOnce", PACT_TEST]},
        {"research": [], "things": ["spirit_anchor", "courier_post", "porter_key"], "dimensions": ["", "nether"],
         "text": "With Stolas's Regard mastered, bind a Spirit Anchor to your Courier Post with a Porter Key and seal a "
                 "workshop hand there: it carries the post's exact requests, a crucible's own shortage among them, as a "
                 "spirit's service. The anchor's soul lantern is the Nether's: the one step that leaves the Overworld",
         "reaches": [HAND],
         "tests": ["theHandNeedsStolasRegardMastered", "aHandFillsACrucibleUntilItIsFull", "aHandsServiceIsASpiritsService",
                   PACT_TEST]},
        {"research": [], "things": [],
         "text": "Keep the pact with one offering a week (an amethyst shard, or one of Styx's flowers): one missed is warned, "
                 "graced a day and then suspends the service, nothing taken; an offering resumes it, and time away is never "
                 "owed. End it at the perch or from the roster: what was paid ahead comes back once, and Stolas departs",
         "reaches": ["item:minecraft:amethyst_shard"],
         "tests": ["upkeepWarnsThenSuspendsAfterGraceAndResumes", "aReturnAfterAbsenceGetsTheFullGrace",
                   "endingReturnsTheEscrowOnce", "endingAnOrphanReturnsItsEscrowOnce", PACT_TEST]},
    ],
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


def _band(degrees):
    """The band a crucible at {degrees} stirs at (tools/concordance_alchemy.py BANDS; Java: alchemy/Band.of)."""
    import concordance_alchemy as al
    reached = [band for band, (start, _dissolve) in al.BANDS.items() if start is not None and degrees >= start]
    return max(reached, key=lambda band: al.BANDS[band][0], default="cold")


def _switches(recipe):
    """The feature switches the recipe file {recipe} (under data/jugcraft/recipe) loads under, or None if it is missing."""
    path = DATA / MOD / "recipe" / f"{recipe}.json"
    if not path.is_file():
        return None
    body = json.loads(path.read_text(encoding="utf-8"))
    return {condition.get("feature") for condition in body.get("fabric:load_conditions", [])
            if condition.get("condition") == f"{MOD}:feature_enabled"}


def workshop_walk(pg, research, invocations, rituals, at_hand, understood, cap=None):
    """What one player alone reaches in the Overworld alone, with only the research entries {understood} understood and
    nothing mastered: every other research state is removed from the graph, to which every Concordance thing the workshop
    names (tools/acquisition.py WORKSHOP) and every product's ingredients and heat are added as a step needing them would
    add them. Returns (reached, the graph's problems)."""
    import acquisition
    import concordance_alchemy as al
    problems = []
    nodes = pg.graph(research, invocations, rituals, at_hand, problems)
    for item in acquisition.WORKSHOP:
        pg.item_node(nodes, at_hand, rid(item), problems)
    for product, (process, heat) in WORKSHOP["products"].items():
        for operation in al.PRODUCTS.get(product, {}).get("processes", {}).get(process, []):
            if operation[0] == "add":
                pg.item_node(nodes, at_hand, al.INGREDIENTS[operation[1]]["item"], problems)
        pg.item_node(nodes, at_hand, heat, problems)
        container = WORKSHOP["containers"].get(al.PRODUCTS.get(product, {}).get("form"))
        if container:
            pg.item_node(nodes, at_hand, container, problems)
    without = {pg.research_node(rid(key), state) for key in research for state in pg.RESEARCH_STATES
               if state == "mastered" or key not in understood}
    return pg.reachable(nodes, research, cap=cap, dimensions={""}, without=without), problems


def _workshop_wants(step):
    """The nodes a workshop step makes or works: its Concordance things, its practices and its rite."""
    import acquisition
    wants = [f"item:{rid(item)}" for item in step["things"] if item in acquisition.WORKSHOP]
    wants += [f"practice:{rid(practice)}" for practice in step["practices"]]
    return wants + ([f"ritual:{rid(step['ritual'])}"] if step.get("ritual") else [])


def workshop_problems(pg, research, invocations, rituals, at_hand):
    """What is wrong with the living workshop (see the module's description): an empty list when it holds."""
    import acquisition
    import concordance_alchemy as al
    found = []
    steps = WORKSHOP["steps"]
    named = [item for step in steps for item in step["things"]]
    rows = {**acquisition.WORKSHOP, **acquisition.FACTORY}
    for item in sorted(set(named) - set(rows)):
        found.append(f"journey workshop: {item} is no row of tools/acquisition.py WORKSHOP or FACTORY")
    for item in sorted(set(rows) - set(named)):
        found.append(f"journey workshop: tools/acquisition.py's {item} is named by no step")
    if steps and steps[0]["switches"] != ["concordance"]:
        found.append("journey workshop: the manual start needs the Concordance's switch alone")
    understood = []
    for number, step in enumerate(steps, 1):
        for key in step["research"]:
            if key not in research:
                found.append(f"journey workshop step {number}: {key} is not a research entry")
        understood = understood + [key for key in step["research"] if key not in understood]
        switches = set()
        for item in step["things"]:
            recipe = acquisition.recipe_file(item, rows.get(item, ""))
            loads = _switches(recipe) if recipe else set()
            if loads is None:
                found.append(f"journey workshop step {number}: recipe/{recipe}.json, which makes {item}, is missing")
            switches |= loads or set()
        if switches != set(step["switches"]):
            found.append(f"journey workshop step {number}: its recipes load under {sorted(switches)}, not "
                         f"{step['switches']}")
        reached, graph_problems = workshop_walk(pg, research, invocations, rituals, at_hand, understood)
        capped, _ = workshop_walk(pg, research, invocations, rituals, at_hand, understood, cap="practitioner")
        found.extend(f"journey workshop step {number}: {problem}" for problem in graph_problems)
        stage = max((key[len("stage:"):] for key in reached if key.startswith("stage:")), key=lambda s: pg.STAGES[s]["order"],
                    default="none")
        if stage != step["stage"]:
            found.append(f"journey workshop step {number}: understanding {understood} makes one {stage}, not "
                         f"{step['stage']}")
        wants = _workshop_wants(step)
        for want in wants:
            if want not in capped:
                found.append(f"journey workshop step {number}: one player alone in the Overworld, with {understood} "
                             f"understood and nothing past the Practitioner stage, does not reach {want}")
        for key in step["research"]:
            lost, _ = workshop_walk(pg, research, invocations, rituals, at_hand, [k for k in understood if k != key])
            if all(want in lost for want in wants):
                found.append(f"journey workshop step {number}: nothing it makes or works needs {key}")
    final, _ = workshop_walk(pg, research, invocations, rituals, at_hand, understood, cap="practitioner")
    for product, (process, heat) in WORKSHOP["products"].items():
        operations = al.PRODUCTS.get(product, {}).get("processes", {}).get(process)
        if not operations:
            found.append(f"journey workshop: {product} has no process {process} (tools/concordance_alchemy.py PRODUCTS)")
            continue
        for operation in operations:
            if operation[0] == "add" and f"item:{al.INGREDIENTS[operation[1]]['item']}" not in final:
                found.append(f"journey workshop: {product}'s {operation[1]} is not had in the Overworld by the route")
            if operation[0] == "stir" and _band(al.HEAT_SOURCES.get(heat, al.AMBIENT)) != operation[1]:
                found.append(f"journey workshop: {product} stirs {operation[1]}, but {heat} holds a crucible at "
                             f"{al.HEAT_SOURCES.get(heat, al.AMBIENT)}, {_band(al.HEAT_SOURCES.get(heat, al.AMBIENT))}")
        if f"item:{heat}" not in final:
            found.append(f"journey workshop: {product}'s heat, {heat}, is not had in the Overworld by the route")
        form = al.PRODUCTS[product]["form"]
        container = WORKSHOP["containers"].get(form, "")
        if al.CONTAINERS.get(form, "?") not in container or f"item:{container}" not in final:
            found.append(f"journey workshop: {product} is bottled in a {al.CONTAINERS.get(form)}, which WORKSHOP's "
                         f"containers ({container or 'none'}) do not give in the Overworld by the route")
    return found


def _stolas():
    """Stolas's offer as shipped (data/jugcraft/concordance/pact/stolas.json)."""
    return json.loads((CONCORDANCE / "pact" / "stolas.json").read_text(encoding="utf-8"))


def _offerings(definition):
    """The items the tag {definition}'s upkeep takes holds (#jugcraft:stolas_offerings), or none."""
    namespace, _, path = definition.get("upkeep", {}).get("offering", ":").partition(":")
    tag = DATA / namespace / "tags" / "item" / f"{path}.json"
    values = json.loads(tag.read_text(encoding="utf-8")).get("values", []) if path and tag.is_file() else []
    return [value if isinstance(value, str) else value.get("id", "") for value in values]


def pact_walk(pg, research, invocations, rituals, at_hand, understood, dimensions=None, blocked=()):
    """What one player alone reaches in {dimensions} (the Overworld alone by default), with no structure loot and nothing
    past the Practitioner stage, with only the research entries {understood} understood and nothing mastered, Stolas's
    Regard aside (its states are what the route reaches): the graph, to which every thing the pact hall makes or places
    (tools/acquisition.py PACT_HALL), the Open Hand Compact's materials, every offering Stolas takes and the workshop
    hand's seal (HAND, as PACT["hand"] and tools/concordance_workers.py HAND_RESEARCH say) are added as a step needing
    them would add them; the nodes {blocked} are taken away as well. Returns (reached, the graph's problems)."""
    import acquisition
    import concordance_workers as wk
    problems = []
    nodes = pg.graph(research, invocations, rituals, at_hand, problems)
    definition = _stolas()
    for item in [rid(item) for item in acquisition.PACT_HALL] + [cost["item"] for cost in definition["compact"]["items"]] \
            + _offerings(definition):
        pg.item_node(nodes, at_hand, item, problems)
    hand = PACT["hand"]
    nodes[HAND] = pg.node("", [pg.research_node(rid(key), "understood") for key in hand["research"]]
                          + [pg.research_node(wk.HAND_RESEARCH, "mastered")]
                          + [pg.item_node(nodes, at_hand, rid(item), problems) for item in hand["things"]], kind="worker")
    without = {pg.research_node(rid(key), state) for key in research for state in pg.RESEARCH_STATES
               if key != "stolas_regard" and (state == "mastered" or key not in understood)}
    reached = pg.reachable(nodes, research, cap="practitioner", dimensions=set(dimensions or [""]), without=without | set(blocked))
    return reached, problems


def _pact_wants(step):
    """The nodes a pact-hall step reaches: those it names and every Concordance thing it makes or places."""
    return list(step["reaches"]) + [f"item:{rid(item)}" for item in step["things"]]


def pact_problems(pg, research, invocations, rituals, at_hand):
    """What is wrong with the pact hall's route (PACT): an empty list when it holds."""
    import acquisition
    found = []
    steps = PACT["steps"]
    named = [item for step in steps for item in step["things"]]
    for item in sorted(set(named) - set(acquisition.PACT_HALL)):
        found.append(f"journey pact: {item} is no row of tools/acquisition.py PACT_HALL")
    for item in sorted(set(acquisition.PACT_HALL) - set(named)):
        found.append(f"journey pact: tools/acquisition.py's {item} is named by no step")
    for item, how in acquisition.PACT_HALL.items():
        recipe = acquisition.recipe_file(item, how)
        loads = _switches(recipe) if recipe else {"concordance"}
        if loads != {"concordance"}:
            found.append(f"journey pact: {item} must be made under the Concordance's switch alone, not {sorted(loads or [])}")
    understood = []
    for number, step in enumerate(steps, 1):
        if PACT_TEST not in step["tests"]:
            found.append(f"journey pact step {number}: {PACT_TEST} walks every step, so the step names it")
        for key in step["research"]:
            if key not in research:
                found.append(f"journey pact step {number}: {key} is not a research entry")
        understood = understood + [key for key in step["research"] if key not in understood]
        dimensions = step.get("dimensions", [""])
        where = " and ".join({"": "the Overworld", "nether": "the Nether", "end": "the End"}[d] for d in dimensions)
        reached, graph_problems = pact_walk(pg, research, invocations, rituals, at_hand, understood, dimensions)
        found.extend(f"journey pact step {number}: {problem}" for problem in graph_problems)
        wants = _pact_wants(step)
        for want in wants:
            if want not in reached:
                found.append(f"journey pact step {number}: one player alone in {where}, with {understood} understood, "
                             f"nothing else mastered, no structure loot and nothing past the Practitioner stage, does not "
                             f"reach {want}")
        for key in step["research"]:
            lost, _ = pact_walk(pg, research, invocations, rituals, at_hand, [k for k in understood if k != key], dimensions)
            if all(want in lost for want in wants):
                found.append(f"journey pact step {number}: nothing it reaches needs {key}")
        if dimensions != [""]:
            home, _ = pact_walk(pg, research, invocations, rituals, at_hand, understood)
            if all(want in home for want in wants):
                found.append(f"journey pact step {number}: nothing it reaches needs {where}; the Overworld alone serves")
    final, _ = pact_walk(pg, research, invocations, rituals, at_hand, understood)
    definition = _stolas()
    for cost in definition["compact"]["items"]:
        if f"item:{cost['item']}" not in final:
            found.append(f"journey pact: the Open Hand Compact's {cost['item']} is not had in the Overworld by the route")
    if definition["compact"].get("resonance", 0) > 0 and "resource:astral_resonance" not in final:
        found.append("journey pact: the Open Hand Compact's resonance is not had by the route")
    offerings = _offerings(definition)
    if not any(f"item:{item}" in final for item in offerings):
        found.append(f"journey pact: none of Stolas's offerings {offerings} is had in the Overworld by the route without Styx")
    import concordance_workers as wk
    unmastered, _ = pact_walk(pg, research, invocations, rituals, at_hand, understood, ["", "nether"],
                              blocked={pg.research_node(wk.HAND_RESEARCH, "mastered")})
    if HAND in unmastered:
        found.append(f"journey pact: the workshop hand is sealed without {wk.HAND_RESEARCH} mastered, "
                     "as SpiritAnchorBlock never seals it")
    return found


def worker_kinds_problems(research):
    """The routes' "served once by each kind of worker" is the Binding Arts' mastery: the worker_service practice of as many
    distinct kinds as the workers have (tools/concordance_workers.py WORKERS). The workshop hand (Concordance Chunk D,
    D-P71) serves as a spirit, so the kinds stay three and no route's words change; a new kind would make the routes'
    words and the Binding Arts' distinct count wrong together."""
    import concordance_workers as wk
    found = []
    kinds = sorted({info["kind"] for info in wk.WORKERS.values()})
    rules = research.get("binding_arts", {}).get("states", {}).get("mastered", {}).get("any", [])
    if not any(rule.get("type") == "practice" and rule.get("activity") == rid("worker_service")
               and rule.get("distinct") == len(kinds) for rule in rules):
        found.append(f"journey: the Binding Arts are not mastered by being served once by each of the {len(kinds)} kinds of "
                     f"worker {kinds}, as the routes that master them say")
    if wk.WORKERS.get("workshop_hand", {}).get("kind") != "spirit":
        found.append("journey: the workshop hand must serve as a spirit, one of the kinds the routes count")
    return found


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
    # The first loadout (Chunk A, A-07 and A-D10; tools/acquisition.py LOADOUT): one player makes every piece in the
    # Overworld, by its recipe or its bench working, with no research but First Light up to understood (the
    # Practitioner stage's own route; mastering two entries, or understanding five, would make them an Adept).
    import acquisition
    import concordance_rime_kit as kit
    unmade = []
    for item in [*acquisition.LOADOUT, *kit.ITEMS]:
        pg.item_node(nodes, at_hand, rid(item), unmade)
    found.extend(f"journey: the first loadout or the Rime travel kit: {problem}" for problem in unmade)
    beyond = {pg.research_node(rid(key), state) for key in research for state in pg.RESEARCH_STATES
              if key != "first_light" or state == "mastered"}
    first = pg.reachable(nodes, research, cap="practitioner", dimensions={""}, without=beyond)
    for item in acquisition.LOADOUT:
        if f"item:{rid(item)}" not in first:
            found.append(f"journey: the first loadout's {item} needs more than First Light understood (research past the "
                         "Practitioner stage), another player, the Nether or the End")
    # The Rime travel kit (Chunk B, B-10): every piece made the same way, before the Adept stage, in the Overworld.
    for item in kit.ITEMS:
        if f"item:{rid(item)}" not in first:
            found.append(f"journey: the Rime travel kit's {item} needs more than First Light understood, another player, "
                         "the Nether or the End")
    # The living workshop (Concordance Chunk C, C-10).
    found.extend(workshop_problems(pg, research, invocations, rituals, at_hand))
    # The pact hall (Concordance Chunk D, D-03 to D-09), and the routes' three kinds of worker with the workshop hand among them.
    found.extend(pact_problems(pg, research, invocations, rituals, at_hand))
    found.extend(worker_kinds_problems(research))
    # Every game test named exists.
    tests = game_tests()
    named = list(FIRST_SUCCESS["tests"])
    for route in ROUTES.values():
        for _stage, _text, names in route["steps"]:
            named += names
    for step in WORKSHOP["steps"]:
        named += step["tests"]
    for step in PACT["steps"]:
        named += step["tests"]
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
    import acquisition
    items = {**acquisition.WORKSHOP, **acquisition.FACTORY}

    def thing(item):
        return lang.get(f"item.{MOD}.{item}") or lang.get(f"block.{MOD}.{item}") or item
    lines += ["", f"### {WORKSHOP['name']}", "",
              "From a manual start to a powered crucible a porter supplies, and the garden's mixed bed and graft "
              "(Concordance Chunk C). It is no stage route: one player alone does each step in the Overworld, with the "
              "research understood so far and nothing mastered. The factory's parts are made by their own recipes; their "
              "bronze, plates and gears are the factory's.", "",
              "| Step | Stage | Understands | Switches | Makes or places | Server game tests |", "|---|---|---|---|---|---|"]
    for number, step in enumerate(WORKSHOP["steps"], 1):
        made = ", ".join(thing(item) + ("" if item in acquisition.WORKSHOP else " (factory)") for item in step["things"]
                         if item in items)
        lines.append(f"| {number}. {step['text']} | {STAGE_NAMES[step['stage']]} | {names(step['research']) or 'nothing new'} "
                     f"| {', '.join(step['switches'])} | {made} | {tests(step['tests'])} |")
    lines += ["", f"### {PACT['name']}", "",
              "Stolas found through Styx or without him, his pact kept and ended, and the workshop hand it unlocks "
              "(Concordance Chunk D). No stage route: one player alone does each step in the Overworld (the workshop hand's "
              "goes to the Nether for its anchor's soul lantern), with the research understood so far, nothing else mastered "
              "and no structure loot (a feather stands in for Styx's flowers, and an amethyst shard keeps the pact). "
              f"`{PACT_TEST}` walks every step on one survival player.", "",
              "| Step | Understands | Makes or places | Server game tests |", "|---|---|---|---|"]
    for number, step in enumerate(PACT["steps"], 1):
        made = ", ".join(thing(item) for item in step["things"]) or "nothing new"
        lines.append(f"| {number}. {step['text']} | {names(step['research']) or 'nothing new'} | {made} | {tests(step['tests'])} |")
    lines += ["", "### Cooperative play", "", "| Shared, and what stays one player's | Server game tests |", "|---|---|"]
    for text, listed in COOPERATIVE:
        lines.append(f"| {text} | {tests(listed)} |")
    lines += ["", "### Recovery from mistakes", "", "| Mistake | What puts it right | Server game tests |", "|---|---|---|"]
    for mistake, remedy, listed in RECOVERY:
        lines.append(f"| {mistake} | {remedy} | {tests(listed)} |")
    return lines


if __name__ == "__main__":
    if "--check" not in sys.argv:
        print("\n".join(table()))
    issues = problems()
    print("\n".join(issues) if issues else "journey: every route holds")
    sys.exit(1 if issues else 0)
