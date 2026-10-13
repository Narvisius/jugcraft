# Validation, checkpoint summaries and decision records

This is a validation plan. No fresh gameplay validation was run while writing this pack. Use the repository's current `docs/TESTING.md` for exact required checks.

## 1. Evidence categories

Keep these separate in every report:

| Evidence | What it establishes | What it does not establish |
|---|---|---|
| Source inspection | Implementation exists at a commit | Runtime success |
| Pure/unit test | A rule or algorithm behaves under tested inputs | Minecraft integration |
| Server game test | Authoritative behavior in tested game scenarios | Human readability or real two-account play |
| Client game test | Rendering/UI behavior in the automated scenario | A full multiplayer playtest |
| Dedicated server plus one client | Networked path in that configuration | Two-player interactions |
| Two independent clients | Tested multiplayer behavior | Performance at an untested population |
| Human survival journey | Reachability and experience in that run | Every seed, recipe or loadout |
| Load test | Measured workload on stated hardware | Unlimited scale |
| Pack build | Artifact generation | Successful launcher import and startup |
| Owner visual review | Acceptance of shown presentation | Technical correctness in all contexts |

Never substitute the left-hand labels for one another to make a status look greener.

## 2. Commands and scope

On Windows use the repository wrapper, for example `.\gradlew.bat build`. The Gradle build runs the server game tests; the ordinary `test` task is not the whole validation story.

Relevant commands, subject to current repository documentation:
- `python scripts/check_repository.py`
- `python tools/check_mod_data.py`
- `.\gradlew.bat build`
- `.\gradlew.bat build -PjugcraftOptionalIntegrations=false` when optional/common boundaries change
- `.\gradlew.bat runClientGameTest -PclientTests=<existing relevant classes>`
- `python tools/select_client_tests.py --base origin/main` to inspect client selection
- `node --test tools/world-designer/model.test.cjs` for editor changes

For framework/distribution changes also use the current pack check/verify/build scripts and relevant Python tests. Do not run broad expensive suites repeatedly for unchanged documentation. Once relevant tests pass, broaden only when changed shared code or unresolved concerns justify it.

A docs-only import of this pack needs document/link/source checks, not a fabricated game-test result. Generators must be rerun only for their affected outputs, following current tooling.

## 3. Scenario matrix

| Area | Required case | Expected observation |
|---|---|---|
| Cast payment | Two delayed casts overlap, one impact repeats | Each authorized cast settles at most once; one cast cannot spend for another |
| Cast permission | Target becomes protected after preview | Actual impact refuses; invalid effect does not leak through |
| Composition | Oversized graph, unknown part, stale revision | Bounded rejection; no server work explosion or unlock leak |
| Ritual | Ingredient removed, pylon empty, participant leaves | Clear pause/cancel and correct reserved/spent distinction |
| Alchemy | Output full at completion, container return needs slot | No voiding or repeated completion |
| Machine | Two automation routes request the same item | Transaction preserves exact quantity and variant |
| Worker | Death/unload/restart while carrying | Cargo has one authoritative owner and recovery path |
| Pact | Copy token, change owner, end twice | No duplicate benefit or double returned property |
| Sky | Time change and two observatories | Existing occurrence ledger prevents repeat claims |
| Hex | PvP off, ward appears, owner disconnects | Authority and expiry still enforced |
| Dream | Wake/death/logout/restart during every transition | Inventory and completion reward returned once |
| Artifice | Reopen, break/replace, reroll preview | Seeded outcome/cost policy preserved |
| Equivalence | Component item, byproduct loop, salvage | Restricted inputs refused; no profitable cycle |
| Boss | Support-only participant, pet damage, late arrival | Fair attribution without idle reward farming |
| Lair | Everyone leaves, falls, dies, server restarts | Correct reset/return; no claim of encounter resume |
| World design | Missing ID, old schema, two saves | Explicit diagnostics and world-local state |
| Presentation | Reduced particles, high GUI scale, optional libraries absent | Important information remains usable |

Write a meaningful regression for each implemented failure mode. Do not add tests that merely assert a private constant equals the same constant copied into the test.

## 4. Human encounter card

For each tested boss record:
- Commit, date, installed versions, world origin, party size and loadouts.
- Entry preparation and time to understand the first mechanic.
- Each death: what killed the player and whether the warning was understandable.
- Actual fight duration and resource use.
- Whether melee, ranged, caster and support had useful opportunities.
- Reward eligibility and exact return behavior.
- Reset/retry and any terrain/projectile leftovers.
- Screenshots/clip references and open defects.

At least one tester should approach with only the in-game information. If the developer must explain every attack verbally, the encounter is not sufficiently taught.

## 5. Combined workshop/load card

Record hardware, Java options, baseline world, warmup duration, measured duration, player count, loaded chunks, machine/worker/plant counts and active recipes. Measure median and p95 server tick time plus client frame-time observations and memory trend.

Compare before/after using the same workload and build conditions. State numerical budgets in the feature brief before claiming success. Avoid ungrounded guarantees such as “supports hundreds of factories.” A healthy small test does not prove a busy public server will be healthy.

No public server currently needs to exist for development. Use isolated local worlds and temporary test servers. If two accounts or appropriate runtime access are unavailable, complete other checks and mark the remaining session honestly.

## 6. Owner checkpoint format

Keep the main checkpoint to approximately 300–500 words, with evidence and detailed notes linked underneath.

### Template

**Chunk [letter]: [player outcome]**

**What you can do now**
- Three to five concrete player actions.
- State where to start and what ordinary materials are needed.

**What changed**
- The meaningful additions and any changed old behavior.
- Mark proposals still awaiting implementation.

**What I checked**
- Exact candidate commit.
- Relevant automated results.
- Human/demo evidence.
- Explicitly untested categories.

**What to look at**
- Two to four screenshots or short clips.
- A quick three-to-five-step demonstration route.

**Decisions for you**
1. One experience or visual choice, with a recommendation.
2. One meaningful scope/difficulty tradeoff if needed.
3. Next chunk preference only if direction is genuinely open.

**Next proposed chunk**
- Outcome, dependency and main risk in two sentences.

Do not include a wall of commit messages, hundreds of item IDs or fifteen implementation questions. Put those in the technical handoff.

### Example after Chunk A

“You can now build a legal spell visually, see why an invalid part fails, and inscribe it into your current wand. The encyclopedia opens from inventory and explains the first Radiance route. Vesperine and Tatterlace can credit real support actions. Existing casting still owns cost settlement.

The relevant server/client tests passed at [actual SHA]. The optional GuiLib absence path was tested. A two-account boss session remains untested.

Please review the composer screenshot and support example. I recommend keeping this compact spell-strip layout for early instruments and exposing branches only when used. Should the next large chunk be winter/stone and the Yeti, or the living workshop?”

This is an example format, not a claim that those features or tests have been completed.

## 7. Technical handoff format

Store with the chunk:
- Base and final commit.
- Implemented ticket IDs and changed contracts.
- Existing APIs reused.
- New schemas, defaults and migration behavior.
- Generator outputs and source provenance.
- Commands/results, evidence paths and limitations.
- Known defects with reproduction.
- Accepted owner decisions and unresolved choices.
- Next smallest complete player outcome.

Distinguish completed implementation from design suggestions in a check-in. An agent reading only the handoff should know where to resume and what not to redo.

## 8. Decision ledger

Use a small table; do not create a new project-management system unless needed.

| ID | Date | Decision | Status | Affects | Evidence |
|---|---|---|---|---|---|
| D-001 | Prior owner direction | Approved pinned frameworks are available; optionality preserved | Accepted | All chunks | Repository framework/owner records |
| D-002 | Prior owner direction | Stolas first, then all 72 distinct spirits | Accepted direction | D and later cohorts | ROADMAP |
| D-003 | Prior owner direction | Encyclopedia opens without an item | Accepted direction | A/UI | Encyclopedia brief |
| D-004 | Prior owner direction | Automatic gas pressure, no routine industrial maintenance | Accepted | C/industry | Industrial plans |
| D-005 | Prior owner direction | Pollution attracts raiders without crop/biome harm | Accepted | C/H | Waste/recycling plan |
| D-006 | This pack | Glass Abbot as the first wholly new boss | Proposed | E | Boss compendium |
| D-007 | This pack | Visual composer/support credit before more sprawling systems | Recommended | A | Audit and chunk plan |

Add owner answers with date and implications. Do not mark a new recommendation “approved” because no reply arrived.

## 9. Release and stop conditions

A candidate is ready for owner review when the promised chunk is playable, relevant checks have evidence, limitations are visible and a demonstration is concrete. It is not automatically authorized for a public server or public release.

A chunk is not complete if its central path is a stub, its only acquisition is creative mode when survival was promised, its reward is unused, or its save behavior is untested after a persistence change.

At the boundary, stop and ask for the owner's input as requested. Retain a short resumable handoff so the next agent can continue without rereading every conversation.

[Research basis](11-research-notes-and-sources.md)
