# Arcane Concordance: the player journey

Status: the automated parts are implemented on branch `claude/concordance-steps-26-32`. **The human playtest has not
been run**; see Verification for what has.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 31) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 23, every stage. Roadmap step 31.
Primary specialty and supported player role: every player. Three routes: cultivation, exploration and combat, crafting
and infrastructure.

The brief asks for a complete journey played from a fresh start to an endgame project with ordinary controls, and its
acceptance criterion is that each route is coherent, achievable and meaningfully different. Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What a playtest decides, and what can be shown without one

Only people playing can judge:
- whether each route is **coherent**: it makes sense in order, and each stage brings something useful;
- whether the first success arrives **promptly** in real time;
- whether recovery from a mistake is **understandable** to someone who made it;
- how cooperative play **feels** with two people.

None of that has been done. What has been shown without a person:
- **Achievable.** `tools/concordance_journey.py` (`check_journey` in `tools/check_mod_data.py`) checks each route
  against the progression graph (step 24). It removes every research state the route does not take, and keeps only the
  route's own Spire. One player alone still reaches the Architect stage in the Overworld alone (the exploration route
  also goes to the Nether). Without the route's own Spire practice, the route stops short of Architect, so the check is
  not vacuous. `ConcordanceJourneyGameTests` climbs each route through the live stage rules on a server, and founds each
  route's own Spire while refusing one whose research the route never took.
- **Meaningfully different.** The routes have three different Spires and three different Adept routes. Each has at least
  two research entries and two practices that no other route uses.
- **The first success, by ordinary controls.** `ConcordanceJourneyClientGameTests` plays the opening in a real client
  with key presses only: examining, casting and opening the journal.
- **Each step's mechanics.** Every step below names the server game tests that exercise it.

## The first success

From a fresh world, with nothing from the Nether or the End and nothing past the Practitioner stage (`check_journey`):

1. Find a luminous specimen and sneak-use it: First Light is encountered, and the player is an Initiate. Specimens:
   - amethyst shards, from geodes;
   - glow lichen and glow berries, from lush caves;
   - glow ink sacs, from glow squid;
   - Jugcraft's glowcap, glimmerbloom and jack-o'-lantern mushroom.
2. In the dark (light 4 or less: a cave, or the open air at night), examine three different ones. Or study one at a
   Lampwright's Bench. First Light is understood, the player is a Practitioner, and they learn Aegis, Kindle and
   Revelation.
3. Craft an Initiate's Wand (luminous matter, a stick and a copper ingot) and hold it. Spell Engine's spell bar puts the
   first invocation on the use key and the next ones on the number keys 2, 3 and on.

**Found while preparing the playtest.**
- **Missing guidance.** The codex said only that the first invocation is cast with the use key. While a casting item is
  held, Spell Engine sends the number keys 2 and on to the spells after the first, so those keys no longer change the
  hotbar slot. Scrolling still does, and binding Spell Engine's own spell keys frees them. The wand's codex entry now has
  a **Casting** page that says so.
- **Spell order.** Jugcraft offers a player's learned invocations sorted by id, and Spell Engine keeps the order it is
  given, so the use key should cast Aegis, 2 Kindle and 3 Revelation. That order is read from the two sources. The
  client test casts with the use key and with 2, and it passed: two different invocations were cast, and whenever
  Kindle is one of them the test also requires its light in the room. Its log line naming the two is in the part of
  CI's log the tools here cannot fetch. Whether Kindle, the light First Light is about, should be on the use key is
  an open question for the owner. It would be a small change to the order `ConcordanceSpells` offers them in.

## The three routes

A route is not a script the game enforces: a player may take any research in any order. A route is the smallest set of
research one player needs to reach the Architect stage that way. Dreamwalking is on no route; any route can take it up
as well.

Two routes can also raise the Lantern Spire, which needs only First Light mastered and Circle Lore understood, and all
three routes have both. The cultivation and exploration routes raise their own Spires by choice; nothing stops them
raising the Lantern Spire instead.

<!-- journey:start -->
| Route | Research mastered | Also understood | Adept by | Master by | Practices | Spire |
|---|---|---|---|---|---|---|
| Cultivation | First Light, Verdant Husbandry, Alembic Arts, Binding Arts, Circle Lore | none | specialist | specialist | alchemy, cultivation, ritual, worker_service | Verdant Spire |
| Exploration and combat | First Light, Celestial Attunement, Crimson Rites, Relic Lore, Sympathy | Circle Lore | generalist | specialist | living_growth, observation, relic_pulse, ritual, sympathy | Star Spire |
| Crafting and infrastructure | First Light, Circle Lore, Runesmithing, Assay, Binding Arts | none | attuned | specialist | artifice, assay, ritual, worker_service | Lantern Spire |

### Cultivation

1. **Practitioner.** Examine verdant specimens, study one at the bench (Verdant Husbandry understood); place Verdant Beds, wake them and plant Concordance crops where their habitat suits them. Tests: `aGreenwardenWakesTheBeds`, `aCropGrowsOnlyWithinItsNiche`, `anEmptyBedStopsGrowth`.
2. **Practitioner.** Feed the beds: a Mendvetch fixes, the Mulch Maw composts, the Gauge reads the habitat. Tests: `aFixerFeedsThePoorestBed`, `theMawFeedsThePoorestBed`, `theGaugeReadsTheHabitat`.
3. **Adept.** Master Verdant Husbandry (harvest each of the four Concordance crops by hand) and First Light (cast Kindle in eight different chunks): two entries mastered (the specialist's route). Tests: `aFreshPlayerClimbsEveryStage`, `eachRouteClimbsItsOwnWay`.
4. **Adept.** Brew from the harvest in the Alembic Crucible; bind a Hearthling or a Gathering Shade to help; a Verdant Heart beats Verdance into a pylon, a Gleaner harvests for it. Tests: `theSameProcessMakesTheSameDraught`, `aFamiliarMendsByItsBond`, `aSpiritKeepsToItsAgreement`, `theHeartBeatsVerdanceIntoAPylon`, `theGleanerHarvestsForVerdance`.
5. **Master.** Master the Alembic Arts (bottle three different outcomes), the Binding Arts (be served once by each kind of worker) and Circle Lore (complete rituals in three different chunks): five entries mastered in three traditions or more (the specialist's route). Tests: `eachRouteClimbsItsOwnWay`.
6. **Architect.** Found a Verdant Spire, raise it in four phases with the Kindling ritual and three days' bone meal and Ley; its field hastens the crops round it, each step paid by its bed. Tests: `foundingNeedsTheMasterStageAndTheResearch`, `phasesFinishInOrderAndTheSpireIsRaised`, `aVerdantFieldHastensOnlyWhatItsBedsPay`.

### Exploration and combat

This route goes to the Nether as well as the Overworld.

1. **Practitioner.** Carry Kindle, Aegis and Revelation into the field; examine celestial, crimson, relic and sympathetic specimens where they are found and study them at the bench. Tests: `sixInvocationsOneForEachRole`, `aegisShieldsAndIsTuned`, `revelationStaysInsideItsLimits`.
2. **Adept.** Five entries understood in five traditions (the generalist's route). Tests: `eachRouteClimbsItsOwnWay`.
3. **Adept.** Watch the sky from an Orrery Observatory and attune an Astrolabe; make offerings and grow a Thornheart Blade by varied deeds against creatures. Tests: `anObservatoryGathersEachOccurrenceOnce`, `anAstrolabeAttunesWhileThePatternIsUp`, `anOfferingKeepsItsLimits`, `theBladeGrowsByVariedDeedsOnly`, `aKillWithTheBladeCounts`.
4. **Adept.** Wear relics on the road (Wardlight, Owlsight Circlet, Stormglass, Hearthstone); take links by touch and lay bounded curses on creatures, which anyone can investigate and remedy. Tests: `aRelicWorksOnlyInItsContexts`, `relicsAreFoundInTrinketSlots`, `linksAreTakenByTouchAndRespectTheRules`, `cursesAreBoundedInvestigatedAndRemedied`.
5. **Master.** Master First Light (Kindle in eight chunks; Lance, Flashstep and Lanternward follow), Celestial Attunement (three different patterns observed), the Crimson Rites (a Thornheart Blade grown to its second stage), Relic Lore (relics serving in three different contexts) and Sympathy (three different curses cast): five entries mastered (the specialist's route). Tests: `lanceStrikesAndScalesWithSpellPower`, `flashstepCarriesForward`, `eachRouteClimbsItsOwnWay`.
6. **Architect.** Understand Circle Lore; found a Star Spire and raise it; its field restores its keepers' Focus and reveals hostile creatures. Tests: `foundingNeedsTheMasterStageAndTheResearch`, `eachConfigurationsFieldDoesItsWork`.

### Crafting and infrastructure

1. **Practitioner.** Understand Circle Lore; raise Ley Pylons, fill them from lanterns or Jugcraft Energy and build a circle the anchor checks part by part. Tests: `aPylonFillsFromLightAndElectricity`, `theCircleIsCheckedPartByPart`.
2. **Adept.** Master First Light (Kindle in eight different chunks) with Circle Lore understood: the attuned route; the Adept's Attunement then makes an Adept's Wand. Tests: `attunementCompletesOnce`, `eachRouteClimbsItsOwnWay`.
3. **Adept.** Forge Resonant Rings at the Artificer's Bench; weigh and dissolve matter at the Assayer's Scale and form it again at a loss. Tests: `aRingIsForgedAndSavedBeforeItIsShown`, `gemsRunesAndBondsKeepTheirRules`, `aStackIsWeighedThenDissolved`, `formingCostsMoreThanDissolvingPays`.
4. **Adept.** Key a Clockwork Porter to a Courier Post; requests are fetched and delivered under reservations. Tests: `aRequestIsFetchedAndDelivered`, `simultaneousRequestsNeverClaimTheSameItems`, `aPorterSaysWhyItStops`.
5. **Master.** Master Circle Lore (rituals in three different chunks), Runesmithing (three different enhancements), the Assay (five different materials dissolved) and the Binding Arts (served once by each kind of worker): five entries mastered (the specialist's route). Tests: `eachRouteClimbsItsOwnWay`.
6. **Architect.** Found a Lantern Spire and raise it; its upkeep can come by courier; its field lights the dark open air so nothing hostile spawns there. Tests: `phasesFinishInOrderAndTheSpireIsRaised`, `theHeartAsksACourierForItsUpkeep`, `eachConfigurationsFieldDoesItsWork`.

### Cooperative play

| Shared, and what stays one player's | Server game tests |
|---|---|
| A party raises a Conclave project together; each contributor's renown is their own | `aPartyProjectSharesItsWork`, `teachingCreditsTheAuthorOnce` |
| Notes pass instructions between players, never observation or mastery | `notesShareInstructionsNotExperience` |
| A ritual for two gathers both and shields them; a participant who leaves stops it | `theVigilGathersTwoAndShieldsThem`, `aParticipantWhoLeavesStopsTheRitual` |
| A spire's keepers share its upkeep; strangers neither count nor change it | `attendanceLapsesAndReturns`, `strangersNeitherCountNorChangeIt` |
| Lecterns serve their owner or party; a bound relic serves only its player; Lanternward wards only the party | `lecternsServeTheirOwnerOrParty`, `aBoundRelicServesOnlyItsPlayer`, `lanternwardWardsOnlyTheParty` |
| Devices act only as their owner could: a porter, a spell's light and harm, a Spire Heart's store | `aPorterTakesOnlyWhatItsKeeperCould`, `lightGoesOnlyWhereItsCasterCouldBuild`, `aSpireHeartsStoreCannotBeDrained` |
| Simultaneous courier requests from two players never claim the same items | `simultaneousRequestsNeverClaimTheSameItems` |

### Recovery from mistakes

| Mistake | What puts it right | Server game tests |
|---|---|---|
| A circle broken mid-ritual | the offerings are released; a lost boundary lashes out at whoever broke it | `aBrokenChannelReleasesTheOfferings`, `aLostBoundaryLashesOut`, `aBreakerAnswersForTheBacklash` |
| A ritual started without enough Ley or Focus | it is refused and takes nothing | `refusalsTakeNothing`, `aDryPylonStopsTheRitual` |
| A broken anchor, crucible, post or Spire Heart | it drops or returns what it held, once | `aBrokenAnchorDropsItsOfferingsOnce`, `aBrokenCrucibleDropsItsItems`, `aBrokenPostSendsItsCargoBack`, `aBrokenHeartLosesNothing` |
| A spire whose upkeep runs short | it rests, and is repaired | `aDamagedSpireRestsAndIsRepaired` |
| A request no longer wanted | cancelling or recovering gives the items back | `cancellingAndRecoveringGiveItemsBack` |
| Salvaging the wrong ring | salvage asks to be confirmed and never gains | `salvageNeedsConfirmingAndNeverGains` |
| Dying in a dream | the escrow comes back once, and nothing is duplicated | `anUnseenDeathRecoversTheEscrowOnce`, `dreamsNeverDuplicatePossessions` |
| Research forgotten by an operator's reset | the stage reached is kept | `aStageIsNeverLost` |
| Not knowing what comes next | the journal says what the next state and each route still need | `metResearchSaysWhatComesNext`, `eachRouteSaysWhatItStillNeeds` |
<!-- journey:end -->

## The playtest (not run)

**Setups.** Run each route twice:
- with the complete dependency set: Jugcraft Complete, built by the Modrinth packaging pipeline from
  [frameworks.lock.json](../../distribution/frameworks.lock.json);
- with the optional integrations absent: the required libraries only (Fabric API, GeckoLib, Player Animation Library,
  Modonomicon, SmartBrainLib, Spell Engine, Spell Power, Trinkets Updated, Cloth Config). Without the optional ones,
  Jade, JEI, Fusion, LambDynamicLights, Mod Menu and GuiLib show nothing, and the plain journal screen replaces GuiLib's
  workspace.

Keep shader (Iris) results and launcher-import results separate from both.

**World and rules.**
- A fresh survival world on an isolated local server, never the owner's live server. Record the seed, the Jugcraft
  commit and the pack.
- One player per route, ordinary controls only: no commands, no creative mode. The only guides are the codex, the
  journal (J) and Jade.

**What to record at every step of the generated route lists:**
- real time and in-game days;
- what was unclear, and where the player looked for help;
- each mistake, and whether the game made the remedy plain;
- anything that blocked progress;
- whether the step felt like a useful new capability.

**Cooperative test.** Two people on two clients, joined to one dedicated server; this is not a server GameTest or one
client. Test:
- a Lumen Vigil ritual for two;
- a Conclave project as a party;
- a Spire kept by both;
- one Courier Post serving both;
- Research Notes passed between them;
- a third player outside the party who must be unable to change their devices.

Each player's research, Focus, rings and bound relics must stay their own.

| Route | Complete set | Optional integrations absent | Two clients |
|---|---|---|---|
| Cultivation | not run | not run | not run |
| Exploration and combat | not run | not run | not run |
| Crafting and infrastructure | not run | not run | not run |

## Connections

- Input producer and output consumer: every Concordance system, in the order a player meets them.
- Reachable entry path: the first success above.

## Balance

None changed. The routes are measured against the stages as they are.

## Multiplayer and persistence

Nothing new is saved. The cooperative rows above name the server tests of shared infrastructure and separate ownership;
the two-client test has not been run.

## Dependencies and assets

No new dependency or asset. The codex's new Casting page is generated text.

## Verification

- Run locally before pushing:
  - `python3 tools/concordance_journey.py`: every route holds;
  - `python3 tools/check_mod_data.py` on a copy of the tree, with the new `check_journey`;
  - `python3 scripts/check_repository.py`.
  Their results are in the pull request. None of this step's Java compiles here; CI is its first compile.
- Server game test added: `ConcordanceJourneyGameTests.eachRouteClimbsItsOwnWay`. Its First Light comes from real
  examinations; the route's other research is granted, because each entry's own evidence and practice are tested by its
  system. Raising a Spire takes days and is played through by `ConcordanceSpireGameTests`; here its milestone is recorded
  as raising records it.
- Client game test added: `ConcordanceJourneyClientGameTests.theFirstSuccessByOrdinaryControls`. The specimens and the
  wand are given, so it tests the controls, not the search.
- CI: Build run 37687379240 on 6ce87a9 (step 32), every job passed. "All 1077 required tests passed" in `mod` and in
  `optional integrations absent`, `eachRouteClimbsItsOwnWay` among them. Client shard 1 ran
  `ConcordanceJourneyClientGameTests` and saved `jugcraft_concordance_journey_first_light` and
  `jugcraft_concordance_journey_journal`. The run on 314126f (step 31) passed `optional integrations absent` before it
  was cancelled by step 32's push.

Not yet run:
- the playtest of each route, in both setups;
- the two-client cooperative test;
- a shader pack;
- a launcher import of Jugcraft Complete.

## World and event applicability

Works everywhere. The exploration route needs the Nether: the Crimson Rites are observed from a Nether specimen, and the
Thornheart Blade takes nether wart.

## Rollout and open questions

- New identifiers: none (the codex page is in the existing entry `foundations/initiate_wand`).
- Open questions:
  - should Kindle be the invocation on the use key?
  - are the three routes the ones the owner wants played first?
