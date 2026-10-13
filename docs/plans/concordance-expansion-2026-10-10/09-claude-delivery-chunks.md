# Claude delivery chunks and execution instructions

This is an implementation roadmap, not a request to build the whole roadmap in one run. The owner asked for substantial work, then a readable checkpoint and input. Each chunk therefore delivers a complete player experience, usually through three to five reviewable integration units. Work autonomously inside the selected chunk; stop at its completed checkpoint before starting the next.

Use [CLAUDE-MASTER-PROMPT.txt](CLAUDE-MASTER-PROMPT.txt) to start an agent. Use [validation and check-ins](10-validation-and-checkins.md) for evidence and report formats.

## 1. Before every chunk

1. Read repository instructions, framework lock, current integration index and the relevant feature records.
2. Record current base commit and inspect working-tree changes. Preserve other people's work.
3. Refresh relevant open PRs. The October 10 audit listed #299, #300 and #301; this list will age.
4. Create a short status delta: what in this pack is already merged, still open, changed or absent.
5. Resolve actual class/registry/generator names with search. Do not turn a conceptual name in these notes into a duplicate subsystem.
6. Identify data/saved-state/network contracts affected and the tests that already cover them.
7. Write a chunk contract: player outcome, three to five work units, existing dependencies, interfaces changed, validation and explicit deferrals.
8. Implement the first end-to-end path before multiplying content variants.
9. Complete routine fixes and relevant checks within the selected scope. Do not ask the owner about ordinary code structure or every reversible edit.
10. Deliver the checkpoint with evidence, remaining limitations and at most three meaningful decisions.

A missing real multiplayer session can remain an explicitly unverified acceptance item if accounts or a test environment are unavailable. Do not fabricate success or silently redefine a one-client test as two clients.

## 2. Chunk map

| Chunk | Complete player outcome | Dependency |
|---|---|---|
| A — Usable foundations | Learn, compose, cast, understand equipment and receive fair encounter credit | Existing main |
| B — Winter and stone | Reach a Rime/Strata workshop and complete the planned Yeti | A plus current Glacier Hall contribution |
| C — Living workshop | Garden → assay → preparation → ritual → useful automated delivery | A; B materials optional |
| D — Pact hall | Meet Stolas, negotiate, use and safely end a working pact | A and selected C foundations |
| E — Expedition season | New distinct encounters and useful school interactions | A/B and chosen supporting schools |
| F — Sky, shadow and memory | Forecast, counter a hex and finish one safe dream adventure | A/C/D as needed |
| G — Authored magical world | Place a settlement/entrance and generate a working magical world | Editor baseline plus selected content |
| H — Combined release candidate | Play the branches together with evidence and understandable guidance | The selected completed chunks |

These are dependency directions, not a demand to wait for every optional branch. For example, the editor's catalog improvements can be implemented independently, while its final integration demonstration uses completed content.

## 3. Chunk A — Usable foundations

Use [the detailed first-chunk tickets](12-detailed-first-chunk-tickets.md) for exact source entry points, payload contracts and acceptance cases.

### Player outcome

A new survival player can discover a first magical practice, compose it without commands, inspect its costs, acquire a useful loadout, enter an existing lair and qualify for reward through support as well as damage.

### A1. Reconcile the source map and acquisition routes

Read `docs/INTEGRATION_STATUS.md`, `WHAT_EXISTS.md`, Concordance feature records and boss pages. Update stale “unbuilt/unmerged” headings without erasing historical evidence.

List every new armor/weapon item affected by this chunk as crafted, traded, loot-only, creative-only or intentionally decorative. Choose a small coherent survival set first, using the owner's approved art. Do not claim all thirteen armor sets gained acquisition if only three did.

Deliver a route diagram/table with prerequisites, actual registered ingredients, optional boss variants and no circular dependency.

### A2. Visual composer over the existing compiler

Extend `concordance/compose/` and the journal/client UI. Implement learned-part browsing, bounded composition, server preview, inscription, persistence and cast demonstration. Keep text commands working as a diagnostic path.

Do not add new delivery types in this unit. Test current operations and illegal graphs. GuiLib is the rich view; the optional-absent path must remain functional. Use the same validation errors in both.

### A3. Encounter contribution

Extend shared lair seams and wire actual effective healing, protection and fixture interactions. Start with one Vesperine ward action and one Tatterlace thread rescue. Preserve current damage qualification while testing the new route.

Define receipt identity, capped contribution and death/disconnect grace. Do not build a public ranking system. Add a concrete support-only test and a zero-credit overheal/spam test.

### A4. Encyclopedia entry and first path

Implement inventory button/configurable key and a working path from discovery to first composition. Reuse server research. Include one machine/ritual inspection and one discovered boss dossier. Avoid empty tabs and unimplemented quest reward promises.

### Completion evidence

- Fresh player learns, composes, inscribes, casts and observes correct resource payment.
- Edited illegal input cannot bypass the compiler.
- Optional UI library absent works.
- A support participant earns one reward; an inactive bystander does not.
- Ordinary equipment and first magic are survival-reachable.
- Source docs and code agree at the candidate commit.

Existing tests to extend include ConcordanceComposeGameTests, ConcordanceInvocationGameTests, ConcordanceJournalGameTests, ConcordanceJournalClientGameTests, ConcordanceAuthorityGameTests, LairGameTests, VesperineGameTests and TatterlaceGameTests. Add focused client evidence for the new composer; don't rename existing suites unnecessarily.

**Owner checkpoint:** show the composer, the first journey and a support reward example. Ask whether the interface density feels right, whether acquisition should be more crafting/trade-oriented, and whether to proceed to winter/stone or prioritize a workshop.

## 4. Chunk B — Winter and stone

### Player outcome

The player can practice Rime and Strata in ordinary play, use existing Thallite, prepare for cold exploration and fight the Yeti in its accepted hall.

### B1. Integrate the current Glacier Hall

Inspect PR300 or its merged successor. Reuse the existing dimension, blocks, arrival, columns and ritual. Confirm ordinary and designed-world access. Do not rebuild the lair under another namespace.

### B2. Rime and Strata practices

Implement Preserve, Rimebind and Brace with explicit target kinds, costs, tolerance and protections. Add research evidence and one crafted instrument or upgrade using existing materials. Do not create a new ore or bypass town protections with terrain magic.

Tests: repeated slow does not permanently immobilize; Brace does not cancel boundary enforcement; preservation cannot wrap arbitrary inventories; Focus is charged once.

### B3. Yeti combat and rewards

Implement the existing planned behavior in chapter 04: wake, first phase, column-charge opening, half-health transition, bounded adds, low-health fury, reset, rewards and return. Use server attack timings and GeckoLib tells.

Build one reliable attack fully before adding all variations. Keep column collision deterministic at arena edges. Add the optional school counters only after the base ordinary-player fight works.

### B4. Cold workshop and gear

Create one useful cold preparation route and a non-boss resistance option. Connect Thallite equipment and the planned trophy weapons without raw-stat inflation. Add recipes, JEI uses, Jade state where relevant and encyclopedia guidance.

### Completion evidence

Solo melee, solo caster and a mixed party can solve the charge/column loop. Permanent safe footing survives every floor change. Adds and temporary ice clean up. Reward and return survive relevant lifecycle events. Every required cold preparation has a non-boss route.

Extend ThalliteGameTests/ThalliteGearGameTests, ConcordanceEffectGameTests, ConcordanceProgressionGameTests and lair tests. Locate the new Yeti suites from the active contribution; do not assume this October snapshot already has them.

**Owner checkpoint:** show a short charge/column sequence, a winter workshop and gear. Ask about movement feel, difficulty and the next school emphasis. Do not ask whether basic save tests should be run; run them.

## 5. Chunk C — Living workshop

### Player outcome

A player grows a magical crop, understands its assay, prepares a useful formula, powers a circle and moves exact ingredients through a helper or ordinary logistics.

### C1. Alchemy workspace

Expose current composition, heat, contamination, capacity and known formula replay. Implement two complete products first, such as a preservation wash and a useful support preparation. Existing ingredients first; no decorative recipes with no consumer.

### C2. Garden depth

Add one companion-planting rule and one stable graft trait. Make habitat requirements visible. Bound neighborhood work and include recovery. Do not attach crop death to industrial pollution.

### C3. Ritual diagnostics and one workshop rite

Show missing positions and resource readiness. Implement one of Stillwater Keeping or Root and Rivet with a declared stage/payment table. Use existing RitualMachine and pylon economics.

### C4. Industrial and worker integration

Connect an existing station or a minimal adapter to JE/water and one shared worker job. Reuse FluidRecipe, energy transactions, MachineCompanionPort and CourierLedger. Demonstrate a full destination and an unloaded target without duplication.

### Completion evidence

The entire loop is survival-reachable from a manual start. A powered version improves convenience. Formula reload, output blockage, container returns, interruption and save/reload are tested. The magical product cannot turn into an equivalence profit loop.

Extend ConcordanceAlchemyGameTests, ConcordanceAssayGameTests, ConcordanceGardenGameTests, ConcordanceRitualGameTests, ConcordanceLogisticsGameTests, ConcordanceEconomyGameTests and relevant companion tests. Add client screens and a measured small workshop load.

**Owner checkpoint:** show one complete input-to-output line, the garden and recipe costs. Ask which balance of hands-on preparation and automation feels desirable, which product family to add next, and whether the visual workshop fits the desired style.

## 6. Chunk D — Pact hall

### Player outcome

Stolas is discoverable through Styx, has a distinct character, negotiates a clear agreement, performs a useful service and leaves safely when the player ends it.

### D1. Pact contract and persistence

Inspect current agreements, Bound Will and roster state. Extend them with only the fields needed by the pilot. Define active, waiting, suspended and departed behavior, including offline and restart policy.

### D2. Stolas journey and service

Implement botanical/mineral/sky evidence through existing research. Build invitation, conversation, terms, acceptance and one genuinely useful service. A stored sky route avoids calendar lock. Include failure/refusal dialogue.

### D3. Character and roster UI

Use approved asset searches, GeckoLib and bounded behavior. A complete character has authored gestures, localized text, waiting reasons and interaction feedback. Add the roster page and cancel/recovery path.

### D4. Prove schema variety

Implement one contrasting service or second small pact only after Stolas works. For example, a craft assistant with explicit reserved cargo should expose different requirements from a knowledge service. Use the result to adjust the shared schema before planning all 72.

### Completion evidence

No one can bind another player. Upkeep can suspend without hidden offline punishment. Agreement copying does not multiply benefits. End/death/unload/restart returns appropriate property once. Two users cannot both own a unique reserved service instance accidentally.

Extend ConcordanceWorkerGameTests, ConcordanceLogisticsGameTests, ConcordanceSharingGameTests, ConcordancePersistenceGameTests and relevant client companion tests.

**Owner checkpoint:** show a complete conversation-to-service-to-departure sequence. Ask about Stolas's personality/appearance, pact strictness and which six-spirit cohort should follow. Do not generate 71 shallow variations before this review.

## 7. Chunk E — Expedition season

### Player outcome

At least two new encounters feel mechanically different and reward existing magical/workshop specialties. This is a large content chunk, but each encounter remains a reviewable unit.

### E1. Glass Abbot

Implement bounded mirror geometry, fixtures, attacks, objective support credit, rewards and a full dossier. Test ordinary manual solving before adding magical assistance.

### E2. One established backlog boss

Choose Cinder Tyrant or Mire Hag using the owner's checkpoint preference. Reuse existing trophy families and add ordinary preparation routes. Maintain visual/attack identity rather than reskinning the Abbot.

### E3. A supporting school package

Deliver a few relevant operations, a utility use and one workshop use for the selected boss's schools. If the boss is Cinder, deepen controlled heat/cooling; if Mire, deepen garden antidotes and reveal/sever. Do not add five unrelated schools.

### E4. Shared expedition polish

Integrate preparation, lore clues, rewards, support contribution and replay convenience. Revisit Vesperine/Tatterlace/Yeti regression tests when shared lair code changes. Measure against current melee, magic and gun loadouts.

### Completion evidence

Each new fight is understandable without reading code. Every required mechanic has a normal interaction. Boss loot is useful, elective and not convertible into infinite invitations/resources. Reset and return work from every phase.

**Owner checkpoint:** provide a one-page comparison of encounters, short footage and actual completion-time observations. Ask which mechanics were most enjoyable, whether difficulty fits, and whether next emphasis should be industrial combat, living encounters or astronomy.

Iron Dreadnought, Warden and other dossiers remain selectable next units rather than all being crammed into this chunk.

## 8. Chunk F — Sky, shadow and memory

### Player outcome

The player can plan around the sky, understand and remove a hex, and enter/complete/leave one authored dream safely.

### F1. Observatory forecast and project

Build the current-calendar chart and one longer project with stored/recall alternatives. Preserve AstralClaims occurrence identity. Add a noncombat reason to build a better observatory.

### F2. Counter-hex practice

Expose links, expiry, protection and a practical cure. Add one encounter interaction and a consensual player-use demonstration. Per-pulse authority remains mandatory.

### F3. Dream scene

Use current dream escrow to build a short memory journey and the wake/return UI. Prove every lifecycle boundary before adding the Somnolent Cartographer's full objective encounter.

### F4. One advanced encounter

Choose Astral Adjudicator or Somnolent Cartographer according to the completed noncombat system. Do not implement both if that would leave escrow or calendar correctness untested.

### Completion evidence

Clock changes cannot mint repeat rewards. Unavailable occurrences have a usable route. Unwilling players cannot be hexed where protection disallows it. Dream inventory and rewards survive death/disconnect/restart without duplicates or loss.

Extend ConcordanceSkyGameTests, ConcordanceHexGameTests, ConcordancePersistenceGameTests and relevant presentation/client tests.

**Owner checkpoint:** show forecast, cure and dream return, then ask about puzzle depth and the next advanced encounter. A transformation specialty needs its own owner-reviewed design choice; do not insert permanent character changes into this chunk by surprise.

## 9. Chunk G — Authored magical world

### Player outcome

The owner can design terrain/biomes, place one exact supported settlement and a magical discovery/entrance, export a pack, and play that generated world with working lairs.

### G1. Catalog honesty and migration

Add capability flags and unresolved-ID states. Preserve old files and current pins. Introduce a versioned schema migration with tests.

### G2. Magic-site layer

Implement a bounded site/region model, top-down editing, validation and export. Choose one actual discovery site as the vertical slice. No global mana overlay or live unsafe server file upload.

### G3. Exact TownBuilder placement

Use actual footprint/rotation rules, terrain preview, collision checks and idempotent cross-chunk placement. Verify an actual generated settlement.

### G4. Realm integration and arena diagnostics

Verify designed-world dimension registration, gate entry/return and two distinct world saves. Add arena-anchor overlays if useful for current bosses. Roads/building lots can follow in a separate extension after exact placement works.

### Completion evidence

Run editor tests and actual world generation. Demonstrate missing mod IDs, an old design import, rotated placement, chunk borders and lair return. State clearly that already generated chunks are unchanged.

Use `node --test tools/world-designer/model.test.cjs`, WorldDesignerGameTests and WorldDesignerClientGameTests, plus relevant lair tests.

**Owner checkpoint:** show editor design beside generated world screenshots. Ask about settlement controls, map scale and next brush/road capability.

## 10. Chunk H — Combined release candidate

### Player outcome

The selected features work as one playable mod/pack. Documentation reflects reality, progression is reachable and known limitations are visible.

### H1. Progression and economy

Play a fresh manual magician, industrialist, gardener/trader and mixed-party route. Measure first-electricity timing if claiming the target. Audit all new material, healing and resource loops.

### H2. Multiplayer and persistence

Use a temporary local/staging dedicated server when available; no public server is required to develop. Two independent accounts should exercise claims, PvP, support rewards, trading, workers, lairs and restart. If unavailable, record the gap and complete automated/one-client evidence separately.

### H3. Performance and presentation

Measure an agreed representative factory, ritual, garden and encounter workload. Verify supported optional integrations present/absent. Review owner assets, legibility and interface scaling.

### H4. Documentation and candidate packaging

Update feature records, integration index, pathway pages, provenance and distribution notes. Build the pack only through existing scripts and lock. A generated artifact is not a successful launcher import. Do not deploy a server or publish a release without the relevant current authorization.

### Completion evidence

A named candidate commit, exact dependency versions, reproducible commands, screenshots, recorded human sessions, open issues and migration notes. No blanket “all tests passed” when some categories were unavailable.

**Owner checkpoint:** a release-readiness page with Ready / Needs fix / Not yet tested, plus the top three experience decisions. The next action is chosen from evidence, not from how many code files were added.

## 11. Multi-agent handoff discipline

These notes can support several Claude agents, but do not assume permission to spawn or message agents on the owner's behalf. When parallel work is authorized:

- Give each agent a bounded interface and file ownership area.
- Keep one integrator for registries, shared rules, language generation and build/lock changes.
- Separate client workspace, server contract and content work only after agreeing payload/schema.
- Share exact commit/interface revisions and report changes promptly.
- Prefer independent branches/worktrees according to repository rules.
- Integrate one unit at a time with relevant checks; do not mass-merge conflicting feature branches.
- Never assign multiple agents to edit the same generated registry or rewrite the same master plan concurrently.
- Require actual model attribution and preserve original feature authors.

A useful task prompt names the player outcome, current source entry points, required invariants, tests and deliverable. “Build all the magic” is not a workable assignment.

## 12. Stop rules

Stop at a chunk boundary with a concrete demonstration and check-in. Continue routine implementation inside the authorized chunk without repeatedly asking permission.

Stop dependent work sooner only for a real unresolved product decision, missing required access/input, destructive operation or an unresolvable contract conflict. Complete independent work while waiting. If the owner changes direction, update the remaining chunk contract and decision log instead of discarding completed work.

Never call a chunk complete because a response is long, the budget is low, a model was generated, or tests compile. Completion is the promised player outcome plus honest evidence.

[Next: validation and check-in templates](10-validation-and-checkins.md)
