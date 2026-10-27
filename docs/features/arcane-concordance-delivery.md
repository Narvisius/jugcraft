# Arcane Concordance: a reviewable delivery

Status: merged to main ([#250](https://github.com/jimbozoomer-byte/jugcraft/pull/250), from branch
`claude/concordance-steps-26-32`, integrated by [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) on
8 October 2026); see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 32) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: the whole Arcane Concordance, every stage. Roadmap step 32.
Primary specialty and supported player role: reviewers, maintainers and the next contributor.

The brief's acceptance criterion: the delivered build, implementation records and actual test evidence establish the
intended gameplay loops, interactions, persistence and failure recovery, and every incomplete or untested area is
identified accurately. This record is the index to that evidence, and it says where the evidence stops.

## What is delivered

| What | Where |
|---|---|
| The mod: Java (and the Kotlin of the journal's and, since Concordance Chunk A, the Spell Composer's GuiLib workspaces), data and assets | `src/main`, `src/client`; the Concordance is under `concordance/` |
| Content definitions | `data/jugcraft/concordance/`, written by `tools/concordance*.py` through `tools/generate_material_data.py` |
| Design documentation | [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md): the contract, vocabulary, library map, definitions, traceability and the 32-step checklist; and one record per system in `docs/features/arcane-concordance-*.md` |
| Player guide | [ARCANE_CONCORDANCE_GUIDE.md](../ARCANE_CONCORDANCE_GUIDE.md) and the in-game codex |
| Tests | the server and client game tests named in each record; `tools/check_mod_data.py`'s Concordance checks |
| Performance | the work budget in the [persistence record](arcane-concordance-persistence.md) and the measurements below |
| The library integration matrix | below |
| Installers | each CI run's `jugcraft-installers` artifact: the Jugcraft JAR, the Jugcraft Complete `.mrpack` with its checksum, and Modrinth metadata ([DISTRIBUTION.md](../DISTRIBUTION.md)). No dependency JAR is bundled into the mod |

## Where this build stands

| | State |
|---|---|
| A local build | Not made: this environment has no Minecraft jars, so `./gradlew build` runs only in CI |
| A branch | `claude/concordance-steps-26-32` (steps 26 to 32, [#250](https://github.com/jimbozoomer-byte/jugcraft/pull/250)), stacked on `claude/awesome-davinci-iwv3b9` (steps 1 to 25, [#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244)); both merged |
| CI | Every push builds the mod and runs the server game tests twice (with and without the optional integrations), runs the client game tests in five shards (a pull request runs only the client test classes it needs) and assembles Jugcraft Complete |
| Merged main | Yes. [#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244) and [#250](https://github.com/jimbozoomer-byte/jugcraft/pull/250) were merged into `integration/oct8-finished-prs` on 8 October 2026, which reached `main` through [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) (`b533201f8`) the same day |
| An import-tested pack | No: no `.mrpack` has been imported into a launcher |
| A published release | No: publishing is the owner's step |

## Library integration matrix

Versions, sides and whether Jugcraft needs each library come from
[frameworks.lock.json](../../distribution/frameworks.lock.json). The build declares every required library in
`fabric.mod.json`'s `depends` and every optional one in `suggests`. CI's `optional integrations absent` job runs every
server game test with the optional libraries removed. The client game tests run with them installed.

<!-- matrix:start -->
| Library | Version (lock) | Side | Jugcraft | What the Concordance does with it | Without it | Game tests | Still limited |
|---|---|---|---|---|---|---|---|
| Fabric API | 0.161.0+26.3 | both | required | Attachments for every player record, and each wearer's worn trinkets choice, sent to everyone who sees them; item components; the rules' reload listener; menus; commands; networking (the journal and its request, signs, the Vigil's gesture, the worn trinkets choice, the Spell Composer's request, state and result, the encyclopedia's request and pages); key mappings; the Focus HUD line; block-entity load and unload (the circle index); the block-break and attack questions asked for the person behind a change, with a FakePlayer standing in for an absent owner when a server allows it; the Transfer API for the crucible's water and every courier pickup and delivery | Jugcraft does not start (Fabric Loader names it as missing) | `theJournalSurvivesTheTrip`, `aSignReachesTheClientUnchanged`, `aRequestIsFetchedAndDelivered`, `lightGoesOnlyWhereItsCasterCouldBuild`, `theCircleIndexLooksOnlyInReach`, `eachWearerChoosesWhetherTheirTrinketsShow` | No pipe carries Concordance essences; a real protection mod has not been tried (a stand-in listener answers the two questions in the tests) |
| Geckolib | 5.5.7 | both | required | Animated blocks and creatures driven by the status the server sends: Circle Anchor, Alembic Crucible (with its liquid at its volume), Verdant Heart, Mulch Maw, Gleaner, Orrery Observatory, Reliquary Shrine, Spire Heart, Oneiric Censer; Hearthling, Gathering Shade, Clockwork Porter and dream wisps; the two fire sets and the three water sets worn as the owner's armour models, each in its own texture (Jugcraft's first GeckoLib armour; Winterguard's on its own model) | Jugcraft does not start (Fabric Loader names it as missing) | `ConcordancePresentationClientGameTests`, `aPorterShowsOnceWhatItLacks`, `ConcordanceEmberGearClientGameTests`, `ConcordanceWinterClientGameTests` | Seen only in CI's small screenshot previews, the water sets since unit 4's client test passed in CI (run 38549876709, Chunk B, not merged); frame cost of a large installation not measured; the sets' glint is a Jugcraft layer, and vanilla trims are not drawn on them |
| Player Animation Library | 1.2.7+mc.26.3 | both | required | Original cast and release gestures for the invocations; the circle participants' channelling gesture; the Vigil's offering gesture, sent by the server when an offering is made; none of them plays over a gun or an arm in the main hand, on any player the client sees (CastPoses) | Jugcraft does not start (Fabric Loader names it as missing) | `aCastGivesWayToAGunOrAnArm` | No person has looked at the gestures; giving way has a test, passed in CI since Chunk A, for Spell Engine's release clip and the Vigil's gesture on the client's own player; it does not cover the fade of an ended cast, another player's view (a second client, not run) or the circle's gesture |
| Modonomicon | 2.16.0 | both | required | The Arcane Concordance codex: categories for every tradition, pages generated from the rules (components, the calendar, the Binding reasons, the spell bar's keys), entries unlocked by research advancements | Jugcraft does not start (Fabric Loader names it as missing) | `codexLoadsWithoutErrors`, `metResearchSaysWhatComesNext` | The codex has been read only as data; nobody has read it in a client |
| SmartBrainLib | 2.0.3 | both | required | The workers' brains: a nearby-players sensor and look and walk behaviours driven by the walk target each worker's own decision sets; bonds, agreements, bodies and loads stay Jugcraft's records | Jugcraft does not start (Fabric Loader names it as missing) | `aPorterSaysWhyItStops`, `aSpiritKeepsToItsAgreement`, `aFamiliarMendsByItsBond` | Pathing over long distances and through many loaded workers not profiled |
| Fusion (Connected Textures) | 1.3.16 | client | optional | Warding Stones and Verdant Beds join their neighbours, through a built-in resource pack registered only when Fusion is installed | plain cubes | none | Never seen in a client |
| LambDynamicLights - Dynamic Lights | 4.13.0+26.3 | client | optional | A lit Kindled Lantern, a charged Astrolabe and the Wardlight Lantern give light in hand (JSON keyed on items and their components; client light only) | no light in hand; world light is unchanged | none | Never seen in a client; tools/check_mod_data.py checks the JSON's components exist |
| Spell Engine | 1.10.9+26.3 | both | required | Casting: the cast timeline, targeting, cooldowns, HUD and sounds; a container source offering a player's learned invocations while an instrument is held; the casting gate; custom impacts into the shared effect boundary; the cost event; spell assignments that keep Jugcraft weapons from casting | Jugcraft does not start (Fabric Loader names it as missing) | `kindleCastsThroughSpellEngine`, `inscribeAndCastThroughSpellEngine`, `cooldownIsNeverShorterThanTheComposition`, `ConcordanceJourneyClientGameTests` | Composed deliveries are Jugcraft's own bounded traces, not Spell Engine projectiles; which invocation sits on the use key is Spell Engine's order |
| Spell Power Attributes | 1.6.2+26.3 | both | required | Schools name each Principle's damage type, so resistances apply once; the Lance scales with arcane Spell Power, Cinderbolt and Hearthflare with fire; Resonant Rings can carry arcane Spell Power, the Ember foci and the two fire sets fire; Spell Power's attribute enchantments are refused on the sets; the water sets carry none, and Spell Power's attribute enchantments are refused on them too | Jugcraft does not start (Fabric Loader names it as missing) | `lanceStrikesAndScalesWithSpellPower`, `everyStatisticIsARealAttribute`, `cinderboltBurnsAndGrowsWithFireSpellPower`, `theRegaliaRaisesTheFireToItsCeiling`, `spellPowerEnchantmentsAreRefusedOnTheSet`, `spellPowerEnchantmentsAreRefusedOnTheWaterSets` | Concordance damage adds Spell Power's points only, not its critical hits; unit 4's water-set test passed in CI (run 38549876709, Chunk B, not merged) |
| Trinkets Updated | 4.2.1+26.3 | both | required | Resonant Rings in the ring slot and the Hearthstone in the necklace slot, both given by data; the owner's Spell Focus and Bracelet slots, ported from their Curios data, for the Ember foci, the Fire Bangle, the water foci and the Water Bangle; a ring's and a focus's modifiers through Trinkets' callback; relics and the bangle read from the slots on the server; Wayfaring's Belt, Charm and Feet slots, with named callback modifiers, a slot-count attribute and Relic Lore's canEquip; the Leather Belt and Amphibian Boot drawn on the wearer by Trinkets' data-driven renderer (a render definition and block models from the owner's worn sheets), through Jugcraft's own render element, which hides them under armour and on a wearer who has chosen to hide them | Jugcraft does not start (Fabric Loader names it as missing) | `relicsAreFoundInTrinketSlots`, `gemsRunesAndBondsKeepTheirRules`, `theFociGiveFireSpellPowerThroughTrinkets`, `theBangleLeavesAHearthbindersBlowSmouldering`, `ConcordanceEmberGearClientGameTests`, `theTrinketsAreMadeAndWornAsDesigned`, `ConcordanceWayfaringClientGameTests`, `theWaterFociLengthenPreservesHold`, `theWaterBangleBracesARimekeeperEveryThirtySeconds` | Cosmetic slots count as worn for show only; of the worn things only the belt and boot are drawn on the body, in third person only, and seen so far only in CI's client screenshots; unit 4's foci and bangle tests passed in CI (run 38549876709, Chunk B, not merged), but no shot shows them in their slots |
| Cloth Config API | 26.3.159 | both | required | The Concordance settings screen: the Focus line, reduced motion, exact values, the simple journal, visual intensity and whether your worn trinkets are shown | Jugcraft does not start (Fabric Loader names it as missing) | none | No test opens the screen; the settings it writes are read by client code only, but for the worn trinkets choice, which the client sends to the server |
| Jade | 26.3.5+fabric | both | optional | Readouts from server snapshots: bench study, lantern Radiance, sconce charge, circle phase and faults, pylon charge, crucible state and Preserve's hold, beds and crops, living devices, observatory, workers, courier posts, shrines; since Concordance Chunk C (not merged), a crucible's reason and tank, a circle's needs, spending and held offerings, the Crucible Stand, a bed's strain and graft | nothing shown on looking; the same facts come from the journal, the commands and each device's own messages | `benchStudyUnderstandsFirstLight`, `preserveHoldsOneBandAndExpires`, `jadeIsToldTheTankAndWhyNeverTheMakeup`, `EncyclopediaClientGameTests`, `ConcordanceWorkshopClientGameTests` | Server tests run without Jade in CI's optional-absent job; CI runs no client test without it; the client tests are written to photograph its overlay on a crucible, a stand, an anchor and a crop (Concordance Chunk C, not merged; they pass in CI, run 38774859294); the one Jade shot viewed, the anchor's in that run, shows no Jade box at the log's 480x270 preview size, though its snapshot assertion passed, so the overlay is still not seen in a client; Preserve's hold, as Jade reads it, passed its test in CI's mod job (run 38519234934, Chunk B unit 3, not merged) |
| Just Enough Items (JEI) | 31.8.0.49 | both | optional | Recipe categories for the Lampwright's Bench, the Circle Anchor and the Resonant Ring; alchemy ingredients' properties as information (never outcomes); since Concordance Chunk A, an information page each on where the Adept's Wand and the lair bosses' loot come from (tools/acquisition.py: Vesperine's and Madame Tatterlace's, and the Yeti King's and the Cinder Tyrant's since Chunk B), with the default data's chances and offerings | no JEI pages; the codex's recipe pages remain | none | Its pages have not been seen in a client; the information pages are written from the default data, so a server data pack that changes a recipe, a boss's loot table or the Adept's Attunement is not shown |
| Mod Menu | 21.0.0 | client | optional | Opens the Concordance settings screen from the mod list | the Concordance settings key (unbound by default, in Controls) opens the same screen | none | Not seen in a client |
| Iris Shaders | 1.11.7+mc26.3 | client | optional | Not used: nothing in the Concordance needs a shader | nothing changes | none | No shader pack has been tried with the Concordance |
| Sodium | 0.9.2+mc26.3 | client | optional | Not used | nothing changes | none | Not tried with the Concordance's renderers |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 | client | optional | Runs the Kotlin of the journal's and the Spell Composer's GuiLib workspaces | the plain journal and composer screens | `ConcordanceJournalClientGameTests`, `ConcordanceComposerClientGameTests` | Only the workspaces are Kotlin |
| GuiLib | 0.12.4 | client | optional | The Concordance Journal's workspace (a tab for each section the server sends) and the Spell Composer's (the draft as a strip of words over the words the player has learned) | the plain journal and composer screens, with the same journal and the same draft | `ConcordanceJournalClientGameTests`, `ConcordanceComposerClientGameTests` | No ritual-schematic, crucible or observatory screen is built; the workspaces have no narration (the plain screens do); keyboard use not tried by a person |
| Diorama | 0.1.1+26.3 | client | optional | Launcher-installed client minimap, world map and waypoints; no Concordance progression hook | map and waypoint controls are absent; server gameplay is unchanged | `DioramaClientGameTests` | Client screen and key tests cover the pinned pack; exploration performance still needs playtesting |
| Travelers Backpack | 11.4.1 | both | optional | No Concordance-specific behavior; Complete adds portable storage through the Trinkets Back slot and Jugcraft industrial recipes, with variant abilities disabled | no backpack integration; Concordance progression is unchanged | `recipesUseIndustrialMaterialsAndRemoveBypasses`, `backSlotAndPlacedAutomationRemainAvailable`, `BackpackClientGameTests` | No essence tanks, magical upgrades or automatic resizing of existing bags |
| Forge Config API Port | 26.3.1 | both | optional | No Concordance-specific behavior; loads and synchronizes Traveler's Backpack settings | Traveler's Backpack also must be absent; Concordance progression is unchanged | `sequentialSmithingPreservesContentsAndUsesApprovedCapacities` | Existing world config overrides and saved backpack slot counts require owner-managed changes |
| Resourcify | 1.8.7 | client | optional | No Concordance-specific behavior; Complete adds in-game resource-pack and shader browsing with a guarded worker-thread shutdown fix | browser buttons are absent; normal pack selection and gameplay are unchanged | `PackBrowserClientGameTests` | Catalog screens and clean shutdown tested locally; installing a downloaded pack and third-party shader compatibility need player testing |
| Lithium | 0.26.2+mc26.3 | both | optional | General tick, AI and physics optimization; no Concordance-specific API integration | Jugcraft gameplay remains available without the optimization | `TownClientGameTests` | Compatibility covered by combined tests; representative hosted performance benchmarking remains outstanding |
| FerriteCore | 9.0.0 | both | optional | Memory reductions in block-state and model storage; no Concordance-specific API integration | Jugcraft gameplay remains available without the optimization | `TownClientGameTests` | Compatibility covered by combined tests; representative hosted performance benchmarking remains outstanding |
| ImmediatelyFast | 1.17.1+26.3 | client | optional | Client immediate-mode rendering optimization; no Concordance-specific API integration | Jugcraft gameplay remains available without the optimization | `TownClientGameTests` | Compatibility covered by combined tests; representative hosted performance benchmarking remains outstanding |
| Krypton | 0.3.2 | both | optional | Network transport optimization; no Concordance-specific API integration | Jugcraft gameplay remains available without the optimization | `TownClientGameTests` | Compatibility covered by combined tests; representative hosted performance benchmarking remains outstanding |
<!-- matrix:end -->

## What is complete, what is not, and what is assumed

**Complete in code and tested on a server in CI** (steps 1 to 30). Each has a record with its tests:
- research and Research Notes;
- typed resources;
- spell composition and the shared effect boundary;
- invocations and their combat and progression baselines;
- rituals and experimental alchemy;
- cultivation and celestial cycles;
- Crimson resources;
- spirits, familiars and constructs;
- reservation logistics;
- equipment construction, relics and shrines;
- bounded equivalence;
- links, curses, wards and dreams;
- the Starbound Conclave;
- the progression graph and the Concord Spire;
- the journal and the signs;
- authority, the economy audit, and persistence.

**Partial:**
- the player journey (step 31): the routes are checked and the opening is played by keys in a client, but no person has
  played them;
- GuiLib screens: the journal's workspace is built, and Concordance Chunk A adds the Spell Composer's
  ([record](arcane-concordance-composer.md)). The ritual schematic, crucible and observatory screens are not; their
  commands, the journal, Jade and the codex stand in.

**Placeholder content:** none known. The owner approved the art and models of steps 14 to 25, but they have been seen
only in CI's small screenshot previews.

**Untested assumptions:**
- that the balance numbers feel right in play;
- that the pace suits a real player;
- that protection mods answer Fabric's questions as the stand-in listener does;
- that two clients see the same thing;
- that shaders and a launcher import work;
- that a world saved by an earlier build loads and survives a server restart;
- that a large installation stays within its work budget in ticks and frames.

## Remaining issues

| Issue | What a player meets | Next action |
|---|---|---|
| No human playtest | Unknown: whether the climb is clear, paced and rewarding | Run the protocol in the [journey record](arcane-concordance-journey.md) for each route, in both setups |
| No two-client test | Shared rituals, spires, couriers and parties are tested only with mock players on one server | Two people, two clients and one local dedicated server, following the journey record's cooperative script |
| The spell bar's order | Holding a wand, the use key casts Aegis and 2 casts Kindle (read from the sources: Jugcraft offers the learned invocations sorted by id, and Spell Engine keeps that order) | The owner decides whether Kindle, the light First Light is about, should be on the use key. It would be a small change to the order `ConcordanceSpells` offers them in |
| No real protection mod tried | A claim mod that ignores Fabric's events would not be asked | Try a Fabric claim mod on a local server: a spell, a porter and a Gleaner at its border |
| Devices wait while their owner is away | Farms and porters pause when their owner logs off | The owner decides the default of `concordance.absent_owner_authority` |
| Not profiled under load | Unknown tick cost of many gardens, workers, rituals and spires; unknown frame cost of GeckoLib bodies and particles | Profile a large test world on a local server (tick time) and client (frame time) against the budget |
| No real old save opened | A world from before step 30 is read as version 0 by design, but no such world has been opened | Open a world saved by a build from before step 30 with a current build, save, reopen |
| A downgrade needs a backup | An older Jugcraft cannot read the versioned saves | Say so in the release notes |
| The courier ledger is read whole | A damaged ledger is not partly kept, as the other records are | Read requests one by one, keeping what cannot be read |
| Item components are not versioned | A changed component shape would need a new component id | Keep component shapes stable; version them if one must change |
| The art has not been seen in a client | Models and textures may look wrong at full size | Look at each in a client: the client tests' full screenshots are in CI's artifacts |
| Nothing published | Players cannot install it yet | Merged to `main` through #263; the owner publishes a release as [DISTRIBUTION.md](../DISTRIBUTION.md) describes |

## Test results

Steps 1 to 25: their records and [244](https://github.com/jimbozoomer-byte/jugcraft/pull/244). Steps 26 to 32, in CI:

| Step | Commit | Build run | Server game tests (both jobs) | Client game tests |
|---|---|---|---|---|
| 26 | 73de706 | 37663359511 | all 1047 passed | all shards passed |
| 27 | 8d583c6 | 37670918838 | all 1056 passed | all shards passed |
| 28 | 3bbd117 | 37676787998 | all 1064 passed | two shards passed; one cancelled by the next push |
| 29 | 9f50a1d | 37680579216 | all 1069 passed | cancelled by the next push |
| 30 | 441cd24 | 37683103100 | all 1075 passed | cancelled by the next push |
| 31 | 314126f | 37685904896 | passed in `optional integrations absent`; `mod` cancelled by the next push | cancelled by the next push |
| 32 | 6ce87a9 | 37687379240 | all 1077 passed | all shards passed; shard 1 ran `ConcordanceJourneyClientGameTests` and saved both its screenshots |

Checks of the checks: faults injected into a copy of the tree, each of which the check must report:

| Check | Injected faults | Caught |
|---|---|---|
| `check_journal` (step 26) | 2 | 2 |
| `check_signs` (27) | 6 | 6 |
| `check_authority` (28) | 8 | 8 |
| `check_economy` (29) | 8 | 8 |
| `check_persistence` (30) | 5 | 5 |
| `check_journey` (31) | 9 | 9 |
| `check_delivery` (32) | 8 | 8 |

## Performance measurements

The work budget, in work rather than time, is in the [persistence record](arcane-concordance-persistence.md).
`ConcordanceWorkloadGameTests.measuredWorkloads` times four pieces of that work on a real server, and CI's log carries the
figures. They are a CI runner's, shared with every test running at once, so they are an indication, not a profile.

Measured in Build run 37687379240 on 6ce87a9. Each job runs the test once; the times are per operation:

| Work | `mod` job | `optional integrations absent` job |
|---|---|---|
| A block change's look-up of the circle anchors in reach (14 anchors loaded in the level) | under 1 µs | under 1 µs |
| Compiling a spell (`ray struck sear then here creatures dazzle`) | 30 µs | 29 µs |
| The progression graph's fixed point (79 nodes) | 250 µs | 200 µs |
| One garden area sample (all 16 of a tick's allowance taken) | 25 µs | 27 µs |

The log rounds to whole microseconds, so the look-up shows as 0. A tick's whole garden allowance is therefore about
0.4 ms against a tick of 50 ms. The graph's own audit walks it seven times at every data load, about 1.5 ms; nothing
walks it during play.

Since Concordance Chunk C (C-10, not merged), `gleanersHeartSearch` adds a Gleaner's pulse, timed after 1,000 untimed
pulses. Measured in Build run 38774859294 on `bb856f873`, `mod` job only (the line names its runner: 4 processors,
amd64, Java VM 25.0.4.1+1-LTS):

| Work | `mod` job |
|---|---|
| A Gleaner's starved pulse: its whole Heart search, 729 positions (729 loaded) | 333 µs, over C-P21's 250 µs line (C-P68, proposed) |
| The position list alone (`Garden.nearest`, built and sorted each call) | 304 µs |
| A fed pulse: its crop search, 147 positions, nothing ripe | 62 µs |

The server's tick time with and without one small workshop is the load card's, recorded in
[Chunk C's contract](concordance-chunk-c.md#the-load-card-run-38774859294). The server's tick time with and without a pact
hall (eight pacts, a visitor, two conversations, a roster, four workshop hands) is its own card's, recorded in
[Chunk D's contract](concordance-chunk-d.md#the-pact-hall-load-card). Not measured: server tick time with a large
installation. Client frame time: `ConcordanceWorkshopClientGameTests` logs 100 frames' times under Mesa's software
renderer in CI (its shard passed in run 38774859294; that line was not read for these records); a player's hardware is
not measured.

## Connections

- Input producer and output consumer: every Concordance system.
- Reachable entry path: the [guide](../ARCANE_CONCORDANCE_GUIDE.md)'s first spell.

## Balance

None changed.

## Multiplayer and persistence

Nothing new is saved. See the persistence record for every saved format.

## Dependencies and assets

No new dependency or asset. Nothing in the lock changed.

## Verification

- Run locally:
  - `python3 tools/concordance_delivery.py`: the matrix holds;
  - `python3 tools/check_mod_data.py` on a copy of the tree, with the new `check_delivery`;
  - `python3 scripts/check_repository.py`;
  - `python3 scripts/package_modrinth.py check`: 18 locked libraries and platform pins;
  - `python3 -m unittest discover -s scripts/tests -v`: 9 tests.
  Their results are in the pull request.
- `python3 scripts/package_modrinth.py verify` could not run here: this environment's network policy refuses the
  download host (403). CI's `Verify pinned library downloads` step runs it on every push.
- `check_delivery` confirms:
  - every library in the lock has one row, with its version from the lock;
  - every game test the matrix names exists;
  - a library called unused is not referred to;
  - the record's matrix and the guide's tables are the tools'.
- Server game test added: `ConcordanceWorkloadGameTests.measuredWorkloads`.
- CI: Build run 37687379240 on 6ce87a9, every job passed: "All 1077 required tests passed" in `mod` and in `optional
  integrations absent`, and all three client shards.

## World and event applicability

Works everywhere.

## Rollout and open questions

- New identifiers: none.
- Open questions are the ones in the remaining issues: the spell bar's order, the absent-owner default, and when to
  playtest.
