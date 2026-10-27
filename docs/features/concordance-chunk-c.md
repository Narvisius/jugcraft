# Concordance Chunk C: the living workshop (contract and handoff)

Status: contract written; unit 1 (C-01 and C-02) built, in four commits on this contract (`8d61fdb7b`): `f4efbf82c`,
`ff11cc062`, `2b53fbff0` and `e1d69574e`; a fifth for its records (`807da7f5e`), a sixth with the unit review's fixes
(`d593ae61d`; see the Fixes from the unit review under C-02), and after it the Rime and Strata client tests' fix
(`0b1039312`: one research learned a tick). Unit 2 (C-03 and C-04) built on `807da7f5e`, in two commits (`a0e73881c`,
`2c98d6d68`) and a third for its records (`4df687cc7`), then merged with `d593ae61d` and `0b1039312` in a fourth
(`2263f851b`), and a fifth with the unit review's fixes (`23f28b90e`; see the Fixes from the unit review under C-04).
Unit 3 (C-05 and C-06) built on `23f28b90e`, in three commits (`940f1f8a8`, `7b2dd6783`, `46f6e2a0e`), a fourth for its
records (`00e795594`) and a fifth with the unit review's fixes (`2f13fed54`; see Unit 3's review fixes, after C-06).
Unit 4 (C-07 to C-09) built on `23f28b90e` too, beside unit 3 rather than on it, in three commits (`5ba973632`,
`d32ac3a61`, `d6edee4f4`), a fourth for its records (`549a49d7e`) and a fifth with the unit review's fixes (`ce04da475`;
see the Fixes from the unit review under C-09); the merge of units 3 and 4 (`1c7dc577c`) joins them, and a commit on it
fixes unit 3's unloaded-crucible client check (`9098c06b1`). Unit 5 (C-10) built on `9098c06b1`, in four commits
(`a86085b3c`, `bfb08770b`, `31bc7087d`, `6d54726fa`), a fifth with the unit review's fixes (`fd2b558c8`) and a sixth
that makes the load card compile (`bb856f873`, the candidate); the commit after the candidate changes only records.
Units 1 to 5 are built and recorded on this branch. In CI:
- unit 1 (through `0b1039312`) passed every job in run 38684732031, after one re-run of client shard 1, whose failure
  was main's `PeepoPopulationClientTests` cow-breeding flake (two cows wandered apart; PR #357, since merged in main as
  `40381834a`, fixes it in main's Peepo code, which this branch does not edit);
- unit 2 (`23f28b90e`) passed every job in run 38693262154 on its first attempt;
- unit 3 (`2f13fed54`), run 38720814268: `mod` (every server game test), `optional integrations absent` and client
  shards 1 to 4 passed; shard 0 failed on `CompanionAutomationChecks` "an unloaded crucible", a test defect fixed in
  `9098c06b1` (see Combined fixes under Unit 5);
- the merge of units 3 and 4 (`1c7dc577c`) passed every job in run 38726242149, unit 4's first CI run: its server and
  client tests passed;
- `9098c06b1` passed every job in run 38731825181, every client class (171) included; `PeepoCompanionClientTests`,
  which holds the fixed check, passed in shard 0;
- unit 5's `fd2b558c8` failed to compile in run 38773020842, fixed in `bb856f873`;
- the candidate `bb856f873` (units 1 to 5) passed every job in run 38774859294, dispatched with `load_card`, after one
  re-run of client shard 1 for main's cow-breeding flake again ([Unit 5: evidence and
  handoff](#unit-5-evidence-and-handoff-c-10)).

The As built notes of C-01 to C-09 were written before these runs. The owner answered the checkpoint on 23 October 2026
(C-D5 to C-D8, see Decisions): the balance of hands-on preparation and automation stays as built, the next product
family is Gloam Ink (when it is built is not yet decided), the Crucible Stand's look fits (the owner did not ask for the
foundation's icon to be redrawn, so it stays), and Chunks B and C are reviewed as two stacked pull requests. The load
budget (C-P19 in C-P67's form) and the workflow change (`bfb08770b`, the manual-only `load_card` input and job) remain
for the owner's review in Chunk C's pull request. Chunk C is developed on branch `claude/chunk-c-living-workshop`, whose
base `e079ad4a3` is Chunk B's head `ec06f16e0` with main `4d38e9712` merged in; CI run 38617471274 on that base passed
every job (see The merge and its CI run). Since then it has been merged with the updated Chunk B, which carries main
`f904df3fd` (`b234b8967`; not yet run in CI), and the commit after the merge brings the Assayer's Scale tests and
records to main's Scale (see C-P40). `src/main/resources/data/jugcraft/alchemy/values.json` and
`docs/audits/survival-item-registry.json` now know Chunk B's and Chunk C's items (Where to resume). The first CI run of
this pull request's head (run 40003960812) ran the server game tests out of heap: 1,428 mock players were still logged in,
since the game's `makeMockServerPlayerInLevel` never logs one out. The Concordance's server tests now make theirs through
`TestPlayers.mock`, which logs each out when its test passes or fails. Not merged. Chunk B, beneath it,
is not merged either; as C-D8 decides, Chunk B's pull request is against main and Chunk C's against Chunk B's branch.
Proposal: the owner chose Chunk C on 14 October 2026 ("ok begin chunk c"), from
[the expansion plan's delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (section 5,
C1 to C4), with [workshops, ecology and pacts](../plans/concordance-expansion-2026-10-10/03-workshops-ecology-and-pacts.md),
[technology and economy](../plans/concordance-expansion-2026-10-10/06-technology-and-economy.md),
[interfaces](../plans/concordance-expansion-2026-10-10/08-interfaces-animation-and-assets.md) and
[validation](../plans/concordance-expansion-2026-10-10/10-validation-and-checkins.md). That answers only the next-school
question of Chunk B's checkpoint ([Chunk B's contract](concordance-chunk-b.md), lines 1328-1331); its other questions
(movement feel, B-P59, B-P60) are unanswered, so every B-P decision stays Proposed (10:162). The same day the owner chose
the branch and answered C-D1 to C-D3 (see Decisions).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.

This page is the chunk's contract (chapter 9 §1 step 7 of the plan) and, as the units land, its technical handoff
(chapter 10 §7). Plan chapters are cited as `NN:line`; `MP` is the plan's `CLAUDE-MASTER-PROMPT.txt` and `SH` its
`START-HERE.md`. Code is cited as `file:line` at `e079ad4a3`: Java paths are under
`src/main/java/io/github/jimbozoomer/jugcraft/`, Peepo's under `src/main/java/` (`local/peepo/...`), client code under
`src/client/java/io/github/jimbozoomer/jugcraft/`, data under `src/main/resources/data/jugcraft/`, tests under
`src/gametest/java/io/github/jimbozoomer/jugcraft/test/` (Peepo's under `src/gametest/java/local/peepo/`), and `mc263src`
is the decompiled 26.3 reference. A fact marked *(survey)* comes from this chunk's survey reports, its delivery plan or
the plan's review and was not re-read for this page. **C-P*n*** is a proposed decision, not the owner's.

## Player outcome

The plan's outcome: "A player grows a magical crop, understands its assay, prepares a useful formula, powers a circle
and moves exact ingredients through a helper or ordinary logistics" (09:120); the chunk map's loop is "Garden → assay →
preparation → ritual → useful automated delivery" (09:28). Corrected for what is already built (see the status delta),
and with the owner's choices C-D1 to C-D3, a survival player can:

- **Alchemy (unit 1):** read an Alembic Crucible's heat, band, the block heating it, volume, tank water, hold, slots,
  formula step and why it waits, on the encyclopedia's Inspect page and in Jade; with the Alembic Arts and an Assay Glass,
  its makeup and contamination; with mastery, what the next dose and the formula would make. A formula that newer rules
  can no longer make stays set and says which step fails and why, instead of vanishing.
- **Two products (unit 1, C-D1):** brew the **Stillroot Poultice**, a Mendvetch healing salve, where a dirty batch heals
  less and the crucible says so before it is bottled; heal another player with it in a lair fight and earn a support
  share. Brew the **Kiln Draught**, a Fire Resistance draught that lets its drinker wade the Cinder Kiln's slag, named in
  the Cinder Kiln's dossier as Winter Brine is in the Glacier Hall's.
- **Ritual (unit 2, C-D2):** see everything a circle still needs before starting (offerings, Ley per pylon for a step and
  for the run, lending, participants, Focus, light, every fault with its position), what a run has spent and still
  reserves, and the rite's stage and payment table; perform **Root and Rivet**, which turns a Thallite plate into a
  **Rooted Thallite Foundation** with Ley from pylons fed by light, Verdance or JE.
- **Workshop (unit 3, C-D3):** build a **Crucible Stand** on that foundation under the crucible; it turns JE into the heat
  the running formula's next stir needs, while an Electric Pump pipes water into the crucible's tank. A campfire crucible
  works as before.
- **Logistics (unit 3, C-D4 Proposed):** have the formula's exact shortage brought by a Peepo through its existing
  crucible port, or by a Clockwork Porter through a courier request the crucible files itself; a full crucible keeps the
  rest aboard, an unloaded one waits, and nothing is lost or made twice.
- **Garden (unit 4):** read on the Inspect page and in Jade exactly what a plant needs and how far its neighbourhood
  reaches; grow Dewmoss and Gloamcap faster side by side (a mixed bed); see a harvested bed strain and recover with rest,
  compost or tending; carry one stable graft, Dewlit, on a Sunpetal bed.

Already built, and not rebuilt here:

| System | Where |
|---|---|
| The Alembic Crucible: mixture, heat by the block beneath, formula replay every 20 ticks, hopper buffer, bottle slot, output, water tank | `concordance/CrucibleBlockEntity.java:91-145, 199-307, 345-375` |
| Formulas (canonical operation text, replayed from empty) | `concordance/alchemy/Formula.java:14-68` |
| The sampling spoon, Assay Glass and mortar; reagents | `concordance/JugcraftConcordance.java:247-252`; `CrucibleBlockEntity.java:393-402` |
| Draughts and salves (a salve on another creature, the bowl back) | `concordance/BrewItem.java:39-87`; `JugcraftConcordance.java:253-255` |
| Verdance brews Regeneration (to level II), Ember Fire Resistance | `concordance/property/verdance.json`, `ember.json` |
| Preserve's hold on a crucible (Chunk B) | `CrucibleBlockEntity.java:182-195, 218-231`; `concordance/rime/Rime.java:120-136` |
| Winter Brine and Frostward, and the Glacier Hall's preparation fact (Chunk B) | `concordance/property/rime.json:3`; `concordance/rime/Frostward.java:23-32`; `lair/LairDossiers.java:213-233` |
| Verdant Beds and the five-factor habitat | `concordance/ecology/Sampler.java:15-65`; `concordance/garden/VerdantBedBlockEntity.java:140-160` |
| Sunpetal, Dewmoss, Gloamcap, Mendvetch and the Verdant Heart | `concordance/organism/*.json` |
| The living devices (Heart, Mulch Maw, Habitat Gauge, Gleaner) | `concordance/garden/VerdantHeartBlockEntity.java`, `MulchMawBlockEntity.java`, `HabitatGaugeBlockEntity.java`, `GleanerBlockEntity.java`; Jade providers at `compat/jade/JugcraftJadePlugin.java:37-40` |
| Circle Anchor, the ritual machine, the Lesser Circle and three rites | `concordance/CircleAnchorBlockEntity.java:290-372`; `concordance/ritual/` (`adept_attunement`, `lumen_vigil`, `spire_kindling`) |
| Ley Pylons, filled by Radiance, Verdance or JE | `concordance/LeyPylonBlockEntity.java:37-48, 157-163`; `JugcraftConcordance.java:242`; `concordance/conversion/` |
| The courier ledger, Courier Posts and the Clockwork Porter | `concordance/courier/CourierLedger.java:170-179`; `concordance/spirits/ClockworkPorterEntity.java:387-406` |
| Peepo's crucible port | `local/peepo/CrucibleCompanion.java:17-106` |
| Energy and fluid networks; the Electric Pump | `fluid/ElectricPumpBlockEntity.java:38-50, 90-126` |
| The encyclopedia's Inspect page (anchor and pylon) | `concordance/encyclopedia/InspectPage.java:26-96` |
| Chunk A's encounter credit | `lair/Encounter.java:204-222, 308-325`; `lair/Lairs.java:82-83` |
| The Cinder Kiln's slag spares anyone under Fire Resistance | `lair/MoltenSlagBlock.java:61-70` |

## Units

Each unit is a reviewable boundary: its own run of commits on `claude/chunk-c-living-workshop`, with its own record and
green checks before the next starts. The order lands the first end-to-end path (crop, crucible, product, rite, powered
station, delivery) in units 1 to 3, before unit 4's garden display and variants (09:16, "Implement the first end-to-end
path before multiplying content variants"; MP:66).

| Unit | Tickets | What it delivers | Record (proposed) | State |
|---|---|---|---|---|
| 1 | C-01, C-02 | The crucible's diagnostics and honest formula reload; the Stillroot Poultice and the Kiln Draught with their consumers, container returns and audits (C-D1) | `arcane-concordance-alchemy.md`, `lair-encounter-credit.md`, `cinder-kiln.md` | Built (`f4efbf82c`, `ff11cc062`, `2b53fbff0`, `e1d69574e`; records `807da7f5e`; review fixes `d593ae61d`): local checks pass; every CI job passed in run 38684732031 (through `0b1039312`, after one re-run of client shard 1 for main's `PeepoPopulationClientTests` cow-breeding flake), and again with units 2 to 5 in the candidate's run 38774859294 |
| 2 | C-03, C-04 | Ritual readiness, fault positions, spent and reserved; Root and Rivet with its stage and payment table (C-D2) | `arcane-concordance-rituals.md`, `thallite.md` | Built (`a0e73881c`, `2c98d6d68`; records `4df687cc7`; merged with unit 1's fixes `2263f851b`; review fixes `23f28b90e`): checks run and passed: `check_mod_data`, `check_repository`, `owner_art --check`, `concordance_journey --check`, `check_icon_maps`, the World Designer's node tests, `scripts/tests`, `git diff --check`; every CI job passed in run 38693262154 on its first attempt, and again in run 38774859294 |
| 3 | C-05, C-06 | The Crucible Stand with JE heat and piped water (C-D3); the shared "supply this crucible's formula" job (C-D4) | new `crucible-stand.md`; `arcane-concordance-logistics.md`, `arcane-concordance-economy.md` | Built (`940f1f8a8`, `7b2dd6783`, `46f6e2a0e`; records `00e795594`; review fixes `2f13fed54`; joined with unit 4 in the merge of units 3 and 4): checks run and passed: `owner_art --check`, `check_mod_data`, `check_repository`, `concordance_journey --check`, `check_icon_maps`, the World Designer's node tests, `scripts/tests`, `git diff --check`; its 53 audit mutations each reported, and the review fixes' 16 and five of the spec's re-run on the code they change; CI run 38720814268 on `2f13fed54`: `mod` (every server game test), `optional integrations absent` and client shards 1 to 4 passed, and shard 0 failed on `CompanionAutomationChecks` "an unloaded crucible", a test defect fixed in `9098c06b1`, which passed every job in run 38731825181 (every client class, 171; `PeepoCompanionClientTests`, which holds the fixed check, in shard 0); every job passed again in run 38774859294 |
| 4 | C-07, C-08, C-09 | Habitat made visible; one mixed-bed rule with harvest strain and recovery; one stable graft trait | `arcane-concordance-ecology.md` | Built on `23f28b90e`, beside unit 3 (`5ba973632`, `d32ac3a61`, `d6edee4f4`; records `549a49d7e`; review fixes `ce04da475`; joined with unit 3 in the merge of units 3 and 4): checks run and passed: `check_mod_data`, `check_repository`, `owner_art --check`, `concordance_journey --check`, `check_icon_maps`, the World Designer's node tests, `scripts/tests`, `git diff --check`; audit mutations 83 of 83 reported; first compiled and run in CI with the merge of units 3 and 4 (`1c7dc577c`), whose run 38726242149 passed every job, unit 4's server and client tests included; every job passed again in runs 38731825181 and 38774859294 |
| 5 | C-10 | The measured small-workshop load, the end-to-end journey, client evidence, combined fixes, the checkpoint and the handoff | this page; `arcane-concordance-journey.md`, `arcane-concordance-delivery.md`, `docs/TESTING.md` | Built on `9098c06b1` (`a86085b3c`; `bfb08770b`, the Build workflow's `load_card` input, for the owner's review; `31bc7087d`; `6d54726fa`; review fixes `fd2b558c8`; the compile fix `bb856f873`, the candidate; records in the commit after it): run and passed for the records: `check_mod_data`, `check_repository`, `concordance_journey --check`, `git diff --check`; CI: `fd2b558c8` failed to compile in run 38773020842; the candidate passed every job in run 38774859294 (after one re-run of client shard 1 for main's cow-breeding flake), the load card's job included; evidence in [Unit 5](#unit-5-evidence-and-handoff-c-10); the owner's checkpoint is awaited |

The plan allows three to five units (09:3); this is five plus the baseline.

## Baseline (C-00)

### Facts

- Branch `claude/chunk-c-living-workshop` (local `wip/chunk-c-base`) at `e079ad4a3`, a merge commit whose parents are
  Chunk B's head `ec06f16e0` (pushed as `claude/chunk-b-winter-stone`, at the same SHA by `git ls-remote`) and main
  `4d38e9712` (#358; main's last three commits are #358, #356 `1ce4e3366` and #352 `feb6d04fb`, Chunk B's base). The
  working tree was clean.
- The planning pack is the same on both sides: `git diff --stat ec06f16e0 e079ad4a3` on
  `docs/plans/concordance-expansion-2026-10-10` is empty.
- Main changed no Concordance, Peepo, lair or Jade code since Chunk B's base: `git diff --stat feb6d04fb 4d38e9712` on
  `concordance/`, `local/peepo/`, `lair/`, `compat/jade/` and `client/compat/JugcraftJadeClient.java` is empty. In the
  merge, `tools/concordance_rituals.py` gained the pylon's power-port sockets (`:527-529`), and `docs/ART_DIRECTION.md`
  two rules (silver-grey cladding; "Support parts and give pipes a purpose").
- Framework lock: no pin or dependency change is planned.

### The merge and its CI run

The owner chose "New branch, main merged (Recommended)" (C-D0b). The merge brings #356 and #358 (4,386 files between
`ec06f16e0` and `e079ad4a3`). Two files needed resolving (the merge commit's message):
- **`src/gametest/resources/fabric.mod.json`:** main's file kept as it is (#358 reformatted it to two-space indentation
  and added nine server and seven client classes), with Chunk B's four classes in their places
  (`ConcordanceRimeStrataGameTests` and `ConcordanceWinterGameTests` at lines 30-31, their client classes at 338-339): 203
  server and 171 client entry points.
- **`CHANGELOG.md`:** #356 had cut main's file to its own 17-line entry (main still has 17 lines). The merge keeps the
  full history and adds #356's section ("The Howitzer Walker, playable", `CHANGELOG.md:36`) below Chunk B's unmerged
  section (`:11`); the branch's file has 1,909 lines. Restoring main's own file is outside Chunk C; a separate task was
  suggested for it.

Everything else merged without conflict. The merge commit records, locally: `owner_art.py` and `generate_material_data.py`
change nothing afterwards; `owner_art --check`, `check_mod_data` (2663 IDs), `check_repository`, `concordance_journey`,
`check_icon_maps` and the World Designer Node tests (12) pass. Those were not re-run for this page.

CI run [38617471274](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38617471274) (Build, workflow_dispatch,
on `e079ad4a3`), read by REST on 14 October 2026: conclusion success on attempt 2.
- Attempt 1: `choose client tests`, `optional integrations absent` (6m33s), client shards 0 to 4 (22m40s, 34m44s,
  44m02s, 46m51s, 33m19s) and `client` succeeded. `mod` (12m18s) failed in its "Build mod" step (5m33s), after "Check
  generated JSON is up to date", "Check material data" and "Test world designer and pack compiler" had passed. The
  session read that failure as the nuclear-plant site clash already seen on main (Chunk B's contract, lines 1322-1323);
  this page's requests for the job log were refused by the log host (403 Forbidden, again on this page's check) and the
  job's annotations say only "Process completed with exit code 1", so the cause is the session's reading, not
  confirmed here.
- Attempt 2 re-ran `mod` once: every step passed, the build, "Verify pinned library downloads" and "Package Jugcraft
  Complete" included (14m07s).
- The run's game-test counts were not read (the logs were not reachable for this page).

### Status delta: the plan's text against the code at `e079ad4a3`

| The plan says | What is true | Evidence |
|---|---|---|
| Formula schema: "Stable recipe ID and version" (03:46) | A formula is canonical operation text, 1 to 32 operations starting with water, with no id and no version. The Peepo port already names a formula by a hash of its text. The rules carry a revision, but it counts loads in this process (it restarts with the server), so it is not a stable version | `concordance/alchemy/Formula.java:14-20, 43-68`; `local/peepo/CrucibleCompanion.java:30-36`; `concordance/ConcordanceData.java:29, 40-52, 76-77` |
| "A previously learned formula must not silently become invalid … report the new incompatibility" (03:54) | A step refused under new rules clears the program with only a sound; setting a written formula that no longer replays says only "formula invalid"; a saved program that no longer parses loads as none | `CrucibleBlockEntity.java:282-289`, `:507-512`, `:662-663` |
| "If blocked, show output-full and pause … No voiding" (03:64) | Automated bottling pauses when the output holds a different or full stack and voids nothing, but no reason is kept or shown: one WANT sign, then silence | `CrucibleBlockEntity.java:345-359, 314-320, 119-120` |
| "Assay spoon and glass already expose composition" (03:17) | As chat lines, gated by the item and research. A client's update tag carries only temperature, parts, taste and murky; the tank's water is shown nowhere | `CrucibleBlockEntity.java:393-402, 706-715`; `concordance/Alchemy.java:101-106` |
| Seven products (03:31-39); Stillroot Poultice: "Dirty batch has lower useful yield, clearly predicted" (03:35) | Only Winter Brine exists (Chunk B, unmerged), named in JEI, the codex and a dossier; its bottle reads "Draught". Verdance already brews a Regeneration salve. Contamination never lowers a helpful effect: `Outcome.of` judges each property from the dose alone and adds Nausea (from 0.5 a part) for the contaminant; bone meal also brings Hollow, so Poison | `concordance/alchemy/Outcome.java:25-41`; data `concordance/property/verdance.json`, `contaminant.json`, `hollow.json` and `concordance/ingredient/bone_meal.json`; Chunk B's contract, line 1313 |
| "Controlled Crucible Stand: JE or ordinary fuel" (06:115) | A crucible's heat comes only from a fixed map of six vanilla blocks beneath it; no JE heater exists | `concordance/alchemy/Heat.java:14-20`; `CrucibleBlockEntity.java:233-241` |
| Companion planting: "count unique families, not identical copies" (03:74) | A diversity factor already counts distinct kinds of plant in the 5×5 at plant height, identical copies once. No declared pairing exists | `Sampler.java:41-58` |
| Grafting (03:76) | Absent: no graft or plant hybrid in `src/` or `tools/` | `grep -ri graft` finds nothing; "hybrid" names only a drone tier (`drone/DroneTier.java`) |
| "Recovery: rest, compost and tending reduce disturbance" (03:77) | Crops never die; stalling is the only failure. Disturbance is read fresh from blocks and has no memory | `concordance/garden/OrganismCropBlock.java:160-164`; `Sampler.java:52-55` |
| "maximum 9×9 horizontal habitat query" (03:91) | Today's sample is 5×5 (100 reads, cached 200 ticks). The Gleaner's search for Hearts is 9×9×9 = 729 positions, where the ecology record says 147 | `Sampler.java:16-22`; `concordance/garden/GleanerBlockEntity.java:44, 127`; `arcane-concordance-ecology.md:131` |
| Missing-part diagnostics "already exist" (03:97) | Shape faults with positions exist. An idle report checks shape only (no Ley, no owner); a start checks one step's Ley; refusals come one at a time | `CircleAnchorBlockEntity.java:454-465, 521-534 (531), 507-518 (508), 330-359, 467-473` |
| Show "already-spent and still-reserved resources separately" (03:107); "correct reserved/spent distinction" (10:48) | Not shown anywhere. The data exists: the run's done, joined and plan, and `RitualDefinition.leyPerRun`, which nothing calls | `CircleAnchorBlockEntity.java:126-128`; `concordance/ritual/RitualDefinition.java:100-102` |
| Stillwater Keeping or Root and Rivet (03:100-101) | Neither exists. A result is sealed `Transform` or `Effects`, so Root and Rivet fits `Transform` with no machine change | `RitualDefinition.java:56-71` |
| Pylons: 3 Radiance → 2 Ley (01:114) | Also 3 Verdance → 2 Ley | `concordance/conversion/verdance_to_ley.json` |
| "Reuse FluidRecipe … MachineCompanionPort and CourierLedger" (09:136) | The crucible uses neither `FluidRecipe` nor `MachineCompanionPort`. Its Peepo port, `CrucibleCompanion`, implements the same `CompanionLogistics.Port`; water reaches it through the shared fluid storage | `CrucibleCompanion.java:18`; `JugcraftConcordance.java:259`; `machine/MachineCompanionPort.java:18`; neither name appears in `concordance/` or `CrucibleCompanion.java` |
| "locate CourierLedger before adding delivery work" (01:90) | `concordance/courier/CourierLedger.java`; `file(...)` is public and takes any destination | `CourierLedger.java:170-179` |
| Thallite components (06:78) | `jugcraft:thallite_plate` has two recipes and no consumer; it appears only in the `c:plates/thallite` tag, which `c:plates` includes, and no recipe asks for either tag. Both recipes load only with the `machines` feature, the hand one also with `thallite`. Since unit 2, its first consumer is Root and Rivet (C-04) | `recipe/thallite_plate_by_hand.json:2-11`; `recipe/pressing/thallite_ingot.json:2-7`; `src/main/resources/data/c/tags/item/plates/thallite.json:4`; `src/main/resources/data/c/tags/item/plates.json:16` |
| Records cite pure harnesses ("61 checks", "52", "98", "41") | Not in Git: `src/test` holds only `VanillaReferences.java` | `find src/test` |

### Defects and gaps found

**Fixed in this chunk** (1 to 7 in unit 1; 8 to 10 in unit 2; 11 to 15 in unit 4; see each ticket's As built notes):

1. A formula refused under new rules vanishes with only a sound (`CrucibleBlockEntity.java:282-289`). C-01.
2. Setting a written formula that no longer replays says only "formula invalid", without the step or the reason
   (`:507-512`; `Formula.replay` returns the refusal but not where, `Formula.java:30-41`). C-01.
3. A saved program that no longer parses is dropped on load (`:662-663`). C-01.
4. The formula's tooltip says "sneak to set a written one" (`tools/concordance_alchemy.py:153-155`); setting is a plain
   use, and sneaking with an item in hand skips the block's use entirely (mc263src `MultiPlayerGameMode.java:335-337`).
   C-01.
5. `CrucibleCompanion.status` says FULL whenever the output holds anything (`CrucibleCompanion.java:96`), but the
   crucible keeps filling a matching stack (`CrucibleBlockEntity.java:356`). C-01.
6. A formula the crucible can no longer follow still drives Peepo demand: `shortage()` and `needed()` read the remaining
   operations whatever the formula's state (`CrucibleCompanion.java:37-57`). C-01, C-06.
7. Anyone with the Alembic Arts can work, set or clear the formula of any crucible: neither `CrucibleBlock.useItemOn`
   nor `CrucibleBlockEntity.use` asks who may change the block (`CrucibleBlock.java:55-69`;
   `CrucibleBlockEntity.java:387-461, 533-553`), while the garden asks (`OrganismCropBlock.java:213`;
   `concordance/Illumination.java:32-38` → `concordance/Authority.java:79-90`). C-01 (C-P5), because C-06 makes the
   formula's setter the courier requester.
8. A ritual's interruption reason and "the offerings are still in the anchor" both go to the action bar in one tick
   (`CircleAnchorBlockEntity.java:650-656, 892-898`), so the second probably replaces the first. A code reading; no client
   was run to see it. C-03. Fixed in unit 2: one line (`circle.ended`, `.ended.spent`), held by `check_rituals`.
9. Jade's gathering line prints "?" for the participants needed (`client/compat/JugcraftJadeClient.java:290`); the
   generated `tooltip.jugcraft.concordance.jade.offering` and `.participants` keys are unused
   (`src/main/resources/assets/jugcraft/lang/en_us.json:7953-7954`). C-03. Fixed in unit 2: the snapshot carries
   `needed`, and both keys are used.
10. A start checks one step's Ley (`CircleAnchorBlockEntity.java:508`), so a run can start that its pylons cannot finish,
    with nothing said beforehand. C-03 warns (C-P10). Warned in unit 2: readiness names the step a run would break off
    at; the start rule is unchanged.
11. The ecology docs say a canteen waters beds (`arcane-concordance-ecology.md:25`; the codex's habitats page), but
    `agriculture/GourdCanteenItem.java:87-100, 116` pours only on farmland, a giant pumpkin, fire or a cauldron, never a
    bed. C-07. Fixed in unit 4: a sip wets a bed below 7 through `VerdantBedBlock.pour` (C-P56).
12. The compost JEI page is generated but never used (`tools/concordance_ecology.py:554-557`). C-07. Fixed in unit 4:
    `recipe_view.json` `garden_compost` and `JugcraftJeiPlugin.addCompostInfo` (C-P57).
13. A crop's Jade lines hide the verdict once it is ripe (`JugcraftJadeClient.java:254`), and a bed's verdict is never
    cleared when its plant goes (`VerdantBedBlockEntity.java:168-188` is written only by a crop's growth and report and
    by a Verdant Heart's work: `OrganismCropBlock.java:152-161, 248`; `VerdantHeartBlockEntity.java:105`). C-07. Fixed
    in unit 4: Jade shows a ripe crop's last verdict as it grew, and a bed's random tick forgets it once neither a crop
    nor a Heart stands on it (C-P55).
14. A plain use of any item on a Verdant Bed reported the bed instead of the item's own use (planting, fertilizer, a
    block set on it needed sneaking): `VerdantBedBlock.java:140-154`; found in unit 4's design. C-07 (C-P56). Fixed in
    unit 4: an item in the main hand is its own use; only an empty hand reports, wakes or tends.
15. Bone meal or fertilizer used plainly on a growing Greenwarden crop reported the crop instead of feeding its bed,
    contrary to the codex (`OrganismCropBlock.java:204-228`); found in unit 4's design. C-08 (C-P61). Fixed in unit 4:
    fertilizer is its own use there, and bone meal feeds the bed by the bed's own rule.

**Record defects, fixed when each unit touches its record:** the alchemy record's table lists 11 ingredients
(`arcane-concordance-alchemy.md:26-38`) where the data has 15 files in `concordance/ingredient/` (Sunpetal, Dewmoss,
Gloamcap and Mendvetch are missing); the rituals record says thirteen tests
(`arcane-concordance-rituals.md:203`) where there are 18; the ecology record says nine
(`arcane-concordance-ecology.md:187`) where there are 12, and 147 positions (`:131`) where the Heart search is 729; the
alchemy record's "Nothing alchemy makes is a resource another system consumes, so there is no conversion loop"
(`arcane-concordance-alchemy.md:164`) stops being true with C-02; the pure harnesses those records cite are not in Git.
Unit 1 corrected the alchemy record's table (15 rows) and its line 164, and says there that its harness is not in Git.
Unit 2 corrected the rituals record's count (thirteen when step 12 was built, eighteen before Chunk C, 29 now) and says
there that its "98 checks" harness is not in Git.
Unit 4 corrected the ecology record's test count (twelve before Chunk C, 33 now) and scan figures (the Maw 147, the
Gleaner 147 for crops and 729 for Hearts), and says there that its "52 checks" harness is not in Git.

**Not Chunk C's:** main's shortened `CHANGELOG.md` (above).

### Open pull requests and the review route (REST, 14 October 2026; refresh before each unit, 09:11)

18 open at this page's check: the 17 of planning (#351, #353 to #355, #357, #359 to #370) and #371, opened at 20:18
UTC on 14 October, after the survey. Of the 17, only #370's head has moved since the pull-request survey (`b0746a5da` →
`01f62e6d3`, its child commit). Files compared: each pull request's changes from its merge base with main (#371 by
`/pulls/371/files`), against the files this contract plans to touch. None touches Concordance Java or data, the
crucible, Peepo's crucible port or `CompanionLogistics`, the Jade provider or client, `InspectPage`, `LairDossiers`,
`Encounter` or the canteen.

| PR | What it is | Files it shares with Chunk C's plan | Bearing |
|---|---|---|---|
| (Chunk B, no PR) | `claude/chunk-b-winter-stone` at `ec06f16e0` | — | Chunk C is stacked on it: the Kiln Draught's dossier fact sits on B-02's dossiers and B-08's preparation fact; Root and Rivet completions feed B's Strata listener (`concordance/strata/Strata.java:34`). A change to B in review means another merge |
| #357 | Flaky client tests: a Peepo stopping short of its food, a flail ball (owner, ready) | `local/peepo/PeepoEntity.java`, `local/peepo/CompanionHusbandry.java`, `PeepoDeliveryClientTests`, `CompanionPopulationChecks` | Chunk C must not edit those files. C-01 and C-06 edit `CrucibleCompanion.java`, which neither #357 nor #369 touches |
| #369 (on #367) | The Iron Dreadnought | `CompanionHusbandry.java`, `CompanionPopulationChecks`; `CHANGELOG.md`, the gametest `fabric.mod.json`, lang, `check_mod_data.py` | As #357. (The survey found `PeepoEntity` in it too; at head `d8ac567d2` the diff from its merge base does not change it) |
| #366 | Machine screens in five looks (owner, ready) | `CHANGELOG.md`, `docs/WHAT_EXISTS.md`, lang | The stand is a block entity with no menu, so no screen conflict; `WHAT_EXISTS.md` rows may conflict |
| #351, #354 | Spodumene deposits; regional mineral sands (base before #352) | `tools/owner_art.py`, `recipe_view.json`, `check_mod_data.py`, `fabric.mod.json`, lang | `owner_art.py` is the import route if the stand's art comes from the owner's library (C-05) |
| #361 to #365 | Fork drafts, one stack (base before #352) | `docs/INTEGRATION_STATUS.md`, `recipe_view.json`, `check_mod_data.py`, `fabric.mod.json`, lang | Shared files only; they need a rebase onto main |
| #353, #367 | The Crypt Lich, part 2; the Foundry Trench | `CHANGELOG.md`, `check_mod_data.py`, `fabric.mod.json`, lang | Shared files only |
| #355, #359, #368; #360 | Guns 13A to 13C; Chima the lion cub (fork draft) | `CHANGELOG.md` (not #360), `fabric.mod.json`, lang | Shared files only |
| #370 | Adam-ondi-Ahman, part 1 (draft) | `CHANGELOG.md`, `check_mod_data.py`, `fabric.mod.json`, lang | Its part 2 keystone "has a Concordance recipe", planned only *(survey)* |
| #371 | KV-1/W walker: model, textures and animation clips (owner, ready, head `36e35b509`) | none: seven new files under `art/kv_walker/`, `docs/images/` and `tools/` (`blockbench_export.py`, `box_preview.py`, `kv_walker.py`) | None today |

**How Chunk C will be reviewed was open** (decided 23 October 2026, C-D8: two stacked pull requests). Chunk B has no
pull request, so a Chunk C pull request against main would show Chunk B's 260 changed files (`git diff --shortstat
4d38e9712 e079ad4a3`: 260 files, the restored `CHANGELOG.md` history among them) and Chunk C together: the "single
enormous unreviewable PR" the pack rules out (SH:48; 09:3 asks for reviewable units). This is a question for the owner's
checkpoint, not one of the decisions below:
- either Chunk B's pull request is opened first and Chunk C's targets Chunk B's branch. For Chunk C's diff to show only
  its own work there, Chunk B's branch would first take the same main merge (`e079ad4a3` holds only Chunk B and main);
  that is the owner's step to approve;
- or Chunk B and Chunk C are reviewed together in one pull request against main, unit by unit through the commits.

### Checks before each unit lands

- `python3 tools/check_mod_data.py`, `python3 scripts/check_repository.py` and
  `python -m unittest discover -s scripts/tests`.
- Refresh the open pull requests and main; merge main again if it moved.
- The unit's own tests (see the Validation plan), with `-PjugcraftOptionalIntegrations=false` for every unit that
  changes Jade code (units 1 to 4).

## Tickets

### C-01. The crucible says what it is doing, and keeps a formula honestly (unit 1)

**What the player sees.**
- Jade on a crucible adds the tank (for example "Water 2/3 parts") and a waiting reason: needs water, needs an
  ingredient, needs a bottle or a bowl, output full (or holding a different brew), wrong heat (waiting for a band), held
  by Preserve, formula incompatible. Never the makeup.
- The encyclopedia's Inspect page, looking at a crucible within the player's block reach, shows (C-P3):
  - **to anyone:** heat, band, the target and the block giving it ("this block does not heat a crucible" otherwise),
    the hold, volume of 6, the tank, the slots, the formula's step and the waiting reason; and a **risk line** whenever
    the formula (or, with none, the mixture) would make a harmful brew or has a searing step (03:54: unknown recipes
    "must not hide destructive risk");
  - **with the Alembic Arts understood and an Assay Glass carried:** dissolved and pending per axis a part, the
    contaminant a part against the murky and Nausea thresholds and what it takes from a fouled property (C-02), and
    operations used of 32;
  - **with the Alembic Arts mastered:** the outcome now, the formula's replay preview under the current rules, and the
    reasons. These are the existing `Assay.read` levels (`CrucibleBlockEntity.java:397-401`).
- A formula the current rules can no longer make stays set: the crucible names the step that fails and why (Inspect,
  Jade, the empty-hand status line) and shows a PERIL sign once. When the rules change back, it carries on.
- Setting a written formula that no longer replays says which step fails and why; setting one that replays says its risk
  line too. The tooltip says "use" rather than "sneak".
- Only someone who may change the block can set or clear a formula, add, pour, stir or bottle (C-P5); reading is open.

**Changes.**
- **One shared diagnostic vocabulary** (03:9; C-P6): a pure package `concordance/diagnostic` holding stable codes with
  parameters (03:9's list, plus needs water, needs a container, held, incompatible and courier quota), localised by the
  client. It is added to the pure-package list (`tools/check_mod_data.py:12310-12314`). The crucible, the stand (C-05),
  ritual readiness (C-03) and crucible filing (C-06) use it; `local/peepo/CompanionStatus.java` and the porter's `Status`
  map from it at their edges and are not replaced.
- **The waiting reason:** `automate` (`CrucibleBlockEntity.java:244-307`) keeps a transient reason instead of the bare
  `waiting` flag (`:119-120`), including the silent band wait (`:276-280`).
- **Honest reload** (C-P1): on a refused step the crucible keeps `program` and `step` and holds an INCOMPATIBLE reason
  (step and the refusal's text) instead of clearing them (`CrucibleBlockEntity.java:282-289`). It re-runs
  `Formula.replay` (at most 32 operations, `Formula.java:30-41`) once whenever `ConcordanceData.revision()` differs from
  the revision it last checked against (`ConcordanceData.java:44-52`): the existing counter, not a new one.
  `Formula.replay` reports the index of the refused step. A saved program that no longer parses is kept as text, written
  back on save and reported, not dropped (`CrucibleBlockEntity.java:662-663`).
- **The formula item path:** `formula()` (`CrucibleBlockEntity.java:505-518`) says the step and reason instead of the
  bare `formula_invalid`, and adds the risk line when it sets one.
- **INCOMPATIBLE drives nothing:** `CrucibleCompanion.needed()` is 0 and its status is `RECIPE_MISSING`; C-05's stand
  idles and C-06 files nothing for it.
- **One source for status:** `CrucibleCompanion.status` (`CrucibleCompanion.java:93-105`) maps from the crucible's
  reason, which removes the FULL mismatch.
- **Authority** (C-P5): every state-changing crucible use (`CrucibleBlockEntity.use` past the spoon and glass, and
  `useEmpty`'s clear) asks `Authority.mayChange` (`Authority.java:79-90`).
- **Jade:** the crucible snapshot (`compat/jade/ConcordanceDataProvider.java:138-161`) adds the tank's parts and the
  reason's code and parameters; the client (`JugcraftJadeClient.java:350-367`) localises them. A new check holds the
  makeup out of that snapshot, beside the update-tag lock (`tools/check_mod_data.py:16290-16294`), which stays at four
  keys. Jade's "waiting for" line keeps showing the next operation to anyone (C-P4).
- **Inspect:** `InspectPage.write` (`InspectPage.java:41-57`) gains a crucible branch reusing the encyclopedia request,
  its loaded check and `PageWriter`; the crucible branch uses the player's block reach rather than the 16 blocks the
  anchor branch allows (`:47-50`; `ConcordanceCommand.java:55`), since glass-level makeup would otherwise read through
  walls (C-P3). The snapshot logic is pure (in `concordance/alchemy`), the writer outside it.
- **Text:** the tooltip fix in `tools/concordance_alchemy.py:153-155`; lang keys for every code.

**Reuses unchanged:** `Mixture`, `Band`, `Heat`, `Assay.read`'s gating, `ConcordanceData.revision()` and `replace()`,
the encyclopedia payloads, Signs, `RateGate`.

**Tests (server):**
- `ConcordanceAlchemyGameTests` (13 today):
  - `aChangedRuleKeepsTheFormulaAndSaysWhy` and `aRestoredRuleResumesTheFormula`. They stand for a reload with
    `ConcordanceData.replace` (used so at `ConcordanceComposeGameTests.java:361`). That is global and static: a catalogue
    missing an ingredient would reach every Concordance test running beside it, so the tests swap the rules only
    through `under()`, which restores them in a `finally` within the same callback (C-P28; `check_diagnostics`);
    vanilla batches by environment and dimension, 50 a batch (`mc263src` HEAD `GameTestBatchFactory.java:32-74`), which
    is not relied on;
  - `aWrittenFormulaThatNoLongerReplaysSaysWhichStep`;
  - `aSavedFormulaThatNoLongerReadsIsKeptAndReported`;
  - `anIncompatibleFormulaAsksForNothing` (`companion.needed(...)` is 0; no filing; no heat demand);
  - the hazards of 03:67 but the container return (C-02): `aFullOutputPausesAndVoidsNothing` (a matching stack at 16 and a
    different outcome: mixture, output and bottle count unchanged, the reason shown); `twoHoppersRaceForTheBuffer` (exact
    counts); `partialWaterWaitsThenDrawsExactly`; `aRestartJustBeforeOutputBottlesOnce` (a block-entity save and load at
    the last step);
  - `theCompanionAndTheCrucibleAgreeOnFull`;
  - `onlyThoseWhoMayChangeACrucibleWorkIt` (a claim or protected town; reading stays open);
  - `theVesselShowsWhatASpoonFinds` (`ConcordanceAlchemyGameTests.java:397`) unchanged.
- `EncyclopediaGameTests.theInspectPageReadsACrucibleByResearch`: spoon level and the risk line for anyone; glass level
  only with the research and the glass; preview only for a master; nothing beyond reach or in an unloaded chunk.

**Tests (client):** `EncyclopediaClientGameTests`: a crucible's Inspect page at the ordinary and a high GUI scale; a Jade
shot of a waiting crucible.

**Acceptance:** "Formula reload, output blockage … and save/reload are tested" (09:140); 03:54; 03:62-67; 10:49.

**Save effects:** none new. The program, step and mixture are already saved (`CrucibleBlockEntity.java:669-695`); the
waiting reason and the checked revision are not; an unreadable program's text is written back under the existing
`program` key.

**Review boundary:** crucible diagnostics, the shared vocabulary, authority on crucible use, and their tests. No brew
outcome changes.

**As built (unit 1: commits `f4efbf82c` and `ff11cc062`).**
- **Built:**
  - the pure package `concordance/diagnostic`: `Code` holds 03:9's eleven codes in its order and words, then Chunk C's
    eight, each with a kind; `Diagnostic` is a code with at most four parameters, worded under
    `compose.jugcraft.diagnostic.<id>` and sent to a client as `encode()`d text. Its table is
    `tools/concordance_diagnostics.py`;
  - the crucible's reason, `CrucibleBlockEntity.waiting()`, read by the empty-hand status line, Jade, the Inspect page
    and the Peepo port;
  - the honest reload. `incompatibility()` replays the formula once per `ConcordanceData.revision()` and after a load. A
    refused formula keeps its step, and an unreadable saved one is kept under `program`;
  - `formula()` names the refused step and why (`message.jugcraft.concordance.alchemy.formula_refused`), and gives the
    risk line (`Assay.risks`) when it sets one;
  - Authority on every change;
  - `CrucibleCompanion`'s status and demand, taken from the crucible's reason;
  - the Inspect page's crucible branch (`InspectPage`, and the pure `alchemy/CrucibleInspection`), and Jade's tank and
    reason;
  - the tooltip and the codex's formulas page.
- **Departures from the ticket,** each a proposal below:
  - the code ids follow 03:9's own wording (`missing_ingredient`, `insufficient_capacity`, ...). Each code's kind sets
    the sign it shows when it begins (C-P22). The wait before a repeat, silent before, now shows a WANT sign once
    (`needs_emptying`), and Peepo shows `OCCUPIED` for it;
  - an incompatible or unreadable formula bottles nothing automatically, not even a finished batch (C-P23);
  - recording onto a blank formula stays open. Every change, clearing included, still needs the Alembic Arts (C-P24);
  - the risk line names the harmful statuses and the first searing step (C-P25), and, since the unit review, the harm
    of a mixture that is not the formula's own batch;
  - the Inspect page reads a crucible within the reader's block reach plus the server's 1.0 use allowance. Every level
    above the spoon's needs an Assay Glass carried, a master's too (C-P26);
  - Jade's raw step now reads "Next step: ..." (C-P27);
  - the reload tests swap the rules through `under()`, not in a batch of their own (C-P28);
  - `follow(Formula)` sets a formula without the item's checks. The client test uses it, so spawn protection on its
    server cannot refuse the formula;
  - `onlyThoseWhoMayChangeACrucibleWorkIt` refuses a stranger inside a claim (`TestClaims`) and, outside it, in
    adventure mode. No game test can make a town, so a protected town is not tested.
- **From the spec review:**
  - the stranger's stick is tried before any stir, so only the claim can refuse it;
  - adventure mode is a second refusal;
  - `anIncompatibleFormulaAsksForNothing` runs a formula to its end and shows that no bottle is used while it is
    incompatible, then that one pulse bottles once the rules are back (C-P23);
  - `crucibleInspection` checks that neither a key nor a `jugcraft:` id shows in the reason or the page's rows.
- **Tests (none run):**
  - 13 new `ConcordanceAlchemyGameTests`: 11 in commit 1, `jadeIsToldTheTankAndWhyNeverTheMakeup` in commit 2, and
    `aHoldIsTheReasonOnlyWhileItKeepsTheHeat` from the unit review;
  - `EncyclopediaGameTests.theInspectPageReadsACrucibleByResearch`;
  - the client test `EncyclopediaClientGameTests.crucibleInspection`, with shots
    `jugcraft_encyclopedia_inspect_crucible` and `_scale4`, and `jugcraft_concordance_crucible_jade` when Jade is
    loaded.
  There is no Minecraft jar where they were written. The Jade test checks Jade's snapshot only when Jade is loaded, so a
  build with `-PjugcraftOptionalIntegrations=false` alone does not run those checks.
- **Audits:**
  - `check_diagnostics` (new);
  - `check_alchemy`: preparation names, and a written formula set by a plain use;
  - `check_signs`: Jade's crucible snapshot and its client;
  - `check_encyclopedia`: the crucible page's reach and glass, and that it only reads;
  - the crucible in `AUTHORITY_ROUTES`, and `diagnostic` in the pure-package list.
- **Commands,** for each commit:
  - run and passed: `python3 tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
    `python3 tools/owner_art.py --check`, `python3 tools/concordance_journey.py --check`,
    `python -m unittest discover -s scripts/tests` and `git diff --check`;
  - the listed audit mutations (§1.7 and §2.3 of the unit's spec) were not run when commits 1 and 2 were made. They
    were run with the unit review's fixes, on a scratch copy: commit 1's twelve (20 variants) and commit 2's seven,
    each reported, but for dropping `ground` from `PREPARATION_NAMES`, which is reported only once the generator has
    rewritten the lang (the stale key hides it from `check_mod_data` alone);
  - not run when these notes were written: `./gradlew build` (with or without the optional integrations),
    `./gradlew runClientGameTest` and CI (they have run since, on later heads: see the status at the top).

### C-02. The Stillroot Poultice and the Kiln Draught (unit 1; C-D1)

**What the player sees.**
- **The Stillroot Poultice:** Mendvetch, water and warm stirs give a salve of Regeneration II; the generator's model
  (`tools/concordance_alchemy.py` `simulate`, re-run for this page) gives 1 water, 2 Mendvetch as they come and four
  warm stirs → Verdance 2.127 a part, Regeneration II for 450 ticks (the cap, `property/verdance.json`) *(the model,
  not a game run)*. A dirty batch heals less, and the crucible says so before bottling: the Assay Glass shows what the
  contaminant takes from the Verdance, a master's preview shows the weaker outcome, and the outcome's reasons name what
  fouled it.
- Applying a poultice to another player hurt in a lair fight earns a support share for the healing its Regeneration
  actually gives (C-P8): the same share healing spells earn (two hearts of the fight's hurt qualifies,
  `lair/Encounter.java:55-56, 103-105`). It works in every lair on the encounter credit (Vesperine, Madame Tatterlace, the
  Yeti King, the Cinder Tyrant), not the Mire Hag's or the Crypt Lich's (still on the old path, Chunk B's contract, lines
  997-1000). The dossiers' credit line says so.
- **The Kiln Draught:** a Fire Resistance draught from existing ingredients; the model gives 1 water, 1 magma cream and
  two hot stirs → Ember 1.276 a part (with 0.2 contaminant, under Nausea's 0.5), 1531 ticks, or 2 sugar → 1.435, 1722
  ticks *(the model, re-run; not a game run)*. Drunk, it lets its drinker
  wade the Cinder Kiln's slag (`MoltenSlagBlock.java:65-70`). Carrying a Kiln Seal shows the Cinder Kiln's rite with a
  preparation fact naming the Kiln Draught, and the Salamander Charm (his drop,
  `loot_table/entities/cinder_tyrant.json:47`) as the lasting answer, as Winter Brine and the Yeti Mitten are named for
  the Glacier Hall (`LairDossiers.java:213-233`).
- Both are named on the item (C-P2), in JEI's pages for their ingredients and in the codex.

**Changes.**
- **The yield rule** (C-P7): a property may declare `fouling`, how many units of it each unit of contaminant a part
  takes away before its level and duration are worked out. Verdance alone declares it (4.0, a starting value). In
  `Outcome.of` (`Outcome.java:25-41`), pure Java, with a reason `alchemy.fouled` naming the foulers (the existing
  `foulers`, `:85-101`); in `Property` (`Property.java:14-37`), the parser's allow-list and the generator's `PROPERTIES`
  and `simulate`, mirrored and held by `check_alchemy` (`check_mod_data.py:13456`). With bone meal added before the
  four warm stirs, the model (re-run for this page) gives 2.735, 3.343 and 3.951 Verdance a part for one, two and three
  bone meal, with 0.2, 0.4 and 0.6 contaminant and 0.227, 0.455 and 0.683 Hollow a part. Today all three are
  Regeneration II (two add Poison, three also Nausea). Under the rule (arithmetic, not run): one bone meal leaves 1.935,
  Regeneration I; two 1.743, Regeneration I and Poison; three 1.551, Regeneration I, Poison and Nausea. A searing
  stir's 500 milli-units of contaminant (`Mixture.java:28-29`) now also costs Verdance: one in place of the poultice's
  last warm stir leaves a one-part batch 2.215 less 2.000, 0.215 Verdance a part, under the threshold, so Nausea only
  *(the model, re-run)*.
- **Products as data** (C-P2): `concordance/product/*.json`, generated by `tools/concordance_alchemy.py`: an id, a form
  (salve or draught), the status it must carry, "nothing harmful", a name key and a codex link. `Alchemy.brew`
  (`Alchemy.java:56-66`) names a matching stack with vanilla's item name. A stack is a product by what it carries, never
  by its name. Winter Brine is named the same way, closing Chunk B's known defect (Chunk B's contract, line 1313).
- **The poultice's credit** (C-P8): `BrewItem.applySalve` (`BrewItem.java:76-87`) tells the encounter when a poultice
  another player applied takes (`actor` non-null, `:56`). `Encounter` keeps a transient mending note (helper, patient,
  until) and, on each tick vanilla's Regeneration would heal the patient, credits the health they gained that tick, at
  most 1, through `Encounter.healed` (`Encounter.java:308-325`), which already credits only the fight's hurt not yet
  healed and nothing past full health. Regeneration's interval was read from the 26.3 sources (`mc263src` HEAD:
  `RegenerationMobEffect`, a heal every `50 >> amplifier` ticks; `MobEffectInstance.tickServer` passes the time left
  before it counts down). No mixin (CLAUDE.md). Before this, brews earned nothing because the boundary's
  observer sees only RESTORATION and PROTECTION (`ConcordanceEffects.java:230-237`; `Lairs.java:82-83`).
- **The Cinder Kiln's preparation fact** (C-P9): `LairDossiers.prepare` (`LairDossiers.java:220-233`, today the Glacier
  Hall only) gains the Cinder Kiln: text and icons, no recipe grid (the Expeditions page's grids are spoken for, Chunk
  B's contract, line 1314), a codex link at the Alembic Arts observed; the Charm only while the Concordance is off or
  the data's Ember no longer brews Fire Resistance.
- **Guidance:** JEI pages for Mendvetch, magma cream and sugar name their product as a property fact (as Chunk B's
  `RIME_JEI`, `tools/concordance_alchemy.py:209, 240`); codex entries `alembic/stillroot_poultice` and
  `alembic/kiln_draught`.
- **Audits** (`check_workshop_products`, new, after `check_winter_brine`, `check_mod_data.py:13678`), one per obligation
  of the alchemy survey's section 9 *(survey)*:
  - **consumers:** the poultice's is a lair share and a friend's healing; the draught's is the slag. Neither is decorative
    (09:124);
  - **not weighable:** no product is an ingredient or a Scale material (extending `:13725-13727`); each carries
    `jugcraft:brew`, so the Scale refuses it as magical (`jugcraft` is a magic namespace,
    `equivalence/Eligibility.java:29`, checked at `:39-62`);
  - **brews never feed brews:** refused by hand, by hopper (`CrucibleBlockEntity.java:577-583`) and by the mortar;
  - **reachable from a manual start:** Mendvetch is crafted from wheat seeds, bone meal and an amethyst shard
    (`tools/concordance_ecology.py:526-528`); magma cream and sugar are vanilla;
  - **containers net to zero:** a salve gives its bowl back, dropped at the user's feet if the inventory is full
    (`BrewItem.java:83`; mc263src `Inventory.java:273-283`); a draught gives its bottle back
    (`JugcraftConcordance.java:253-254`);
  - **dilution gains nothing:** a poultice diluted to 2 parts gives two doses of 1.063 a part, Regeneration I for 318
    ticks each (`Mixture.bottle` gives every dose of a batch the same share since the unit review; before, the last
    dose took the remainder, 1.064 for 319 ticks) *(the model, re-run)*. At vanilla's
    interval (above) the two heal 6 + 6 = 12, against 18 for one Regeneration II dose of 450 ticks
    (`dilutionNeverHealsMore` measures it). The credit is bounded by the fight's hurt, not by how many doses exist;
  - **no stacking:** `Stacking.STRONGEST` (`Alchemy.java:79-81`; `effect/Stacking.java:11-15, 68-76`): a second poultice
    replaces a weaker one or, as strong and longer, extends it to its own time; otherwise nothing changes, and it never
    adds a level;
  - **the boss route:** a Tyrant Scale makes 2 magma cream (`recipe/magma_cream_from_tyrant_scale.json`, under
    `agriculture`), a boss-to-ingredient edge found through `_made_from` (`check_mod_data.py:13623`). Proposed as a
    reward route, not a loop (C-P9): the draught makes no Kiln Seal (`kiln_seal_from_scales`, `LairDossiers.java:71-73`)
    and no scale, and magma cream has vanilla sources. A second route: the Crypt Lich's Grave Dust makes 2 bone meal
    (`recipe/bone_meal_from_grave_dust.json`), and bone meal goes into Mendvetch. Both are declared in
    `PRODUCT_REWARD_ROUTES` and found by recipes alone, not through the Assayer's Scale (C-P32). Since the merge with
    main `f904df3fd` (`b234b8967`), two tool-only routes are declared there too: Tatterlace's Gossamer Silk and the Iron
    Dreadnought's Salvaged Coil reach Mendvetch and sugar only through a knife (main's household knife; a bronze knife
    by way of red matter's hoe and sticks) that a cutting recipe keeps, which `_made_from` counts as an input;
  - **Vitae:** health is not a modelled resource; the Vigil offering stays bounded by `HOURLY_VITAE_BOUND` = 60
    whatever heals the giver (`check_mod_data.py:15597, 15652-15653`).
- **Records:** `arcane-concordance-alchemy.md:164` rewritten to name the consumers and these checks;
  `lair-encounter-credit.md`, `cinder-kiln.md`.

**Reuses unchanged:** the mixture rules, `BrewItem`'s salve and bowl, `Encounter.healed` and its bounds, `LairDossiers`'
rite section, Preserve's hold.

**Tests (server):**
- `ConcordanceAlchemyGameTests`: `aStillrootPoulticeRepeatsAndADirtyBatchHealsLess` (by hand and automated; the preview
  equals the outcome); `aKilnDraughtCarriesItsDrinkerAcrossTheSlag` (no slag damage under it; the bottle back);
  `aPoulticeReturnsItsBowlToAFullInventory`; `productsNeverFeedTheCrucible`; `twoPoulticesDoNotAdd`;
  `dilutionNeverHealsMore`; `anOldUnnamedBrewInTheOutputPausesAndSaysSo` (named stacks differ in components from older
  unnamed brews, so an automated crucible pauses until its output is emptied, `CrucibleBlockEntity.java:356`; now with a
  reason).
- `ConcordanceAssayGameTests` (6 today): `workshopProductsAreNeverWeighed` (eligibility refused, the ledger unchanged).
- `LairEncounterGameTests` (10 today): `aPoulticeOnAFriendIsCreditedForWhatItHeals` (no more than the fight's hurt and
  the poultice's own healing; qualifies at two hearts); `aPoulticeOnOneselfEarnsNothing`;
  `aPoulticeOnAnUnhurtFriendEarnsNothing`; `aSecondPoulticeAddsNoCredit`; `aPoulticeOutsideTheFightEarnsNothing`;
  `onlyAPoulticeMends` (a Frostward salve earns nothing, as Chunk B recorded, its contract line 1024);
  `blowsAndSpellsBetweenItsHealsAreNotThePoultices`; `regenerationHealsOnItsVanillaInterval`.
- `EncyclopediaGameTests.theCinderKilnsRiteNamesTheKilnDraught` (and the Charm alone with the Concordance off);
  `theDossiersNameThePoulticesCredit`.
- `ConcordanceAlchemyGameTests.jadeIsToldTheTankAndWhyNeverTheMakeup` (C-01's, in unit 1's second commit).

**Tests (client):** Inspect shots of a fouled poultice's preview and of a crucible holding a Kiln Draught formula.

**Acceptance:** "Implement two complete products first … no decorative recipes with no consumer" (09:124); "container
returns … are tested" (09:140); "The magical product cannot turn into an equivalence profit loop" (09:140; 06:146-164;
10:57); "Dirty batch has lower useful yield, clearly predicted" (03:35). Not the only medicine (06:26): vanilla healing
and vanilla's Fire Resistance potion remain.

**Save effects:** names and the fouling rule reach new stacks only; bottles already made keep what they carry (the
`jugcraft:brew` component). No new saved key: the mending note is not saved, like `Encounter` itself
(`Encounter.java:49-50`).

**Review boundary:** alchemy data and generator, product naming, one `Encounter` hook (the mending credit) in lair code,
the Cinder Kiln's preparation fact, the checks and the records.

**As built (unit 1: commits `2b53fbff0` and `e1d69574e`).**
- **Built:**
  - the yield rule: `Property.fouled`; in `Outcome.of` the fouling is taken, subtracted, then judged; Verdance declares
    4.0;
  - three products as data (`concordance/product/`, read by `RulesParser.product` into `AlchemyCatalog`):
    `stillroot_poultice`, `kiln_draught` and `winter_brine`. `Alchemy.brew` names a stack that `Alchemy.product`
    matches, with vanilla's item name;
  - the Assay Glass line (what the contaminant takes) and the fouled reason;
  - codex entries and the JEI facts;
  - the Cinder Kiln's preparation fact;
  - the poultice's credit: the `BrewItem.Observer`, registered by `Lairs`, calls `Encounter.mended`; the encounter reads
    the note at three points (C-P30); `creditHealing` holds `healed`'s old body;
  - the dossiers' poultice fact;
  - `check_workshop_products` and `check_poultice_credit` (new), and `check_alchemy`'s fouling rules.
- **Departures from the ticket:**
  - no `Products` class: `Alchemy.product` and `AlchemyCatalog.product`, with `Alchemy.STILLROOT_POULTICE`;
  - a product is any brew of its form that carries its status and nothing harmful, as the server judges harm (C-P29);
  - credit is counted from Regeneration's own time left, read at each tick's end, each blow and each Concordance heal.
    The ticket said "on each tick vanilla's Regeneration would heal" (C-P30);
  - the Kiln Draught's dossier fact also names his flame (C-P31);
  - Grave Dust is a second declared reward route (C-P32);
  - `_made_from` takes `scale=False` and reads the recipe files once a run;
  - `LairDossiers.poultices()` reuses `brews`, the helper the Kiln Draught's fact added;
  - two credit tests (`aPoulticeOnOneselfEarnsNothing`, `blowsAndSpellsBetweenItsHealsAreNotThePoultices`) check that
    none of their own players qualifies, rather than that nobody does: a fight in a neighbouring test within 64 blocks
    shares their players' hurt and notes;
  - the credit tests run their steps through a small timeline helper.
- **From the spec review:**
  - the order check (fouling taken, subtracted, then judged);
  - `REGENERATION_INTERVAL` held to `50 >> amplifier`, in `check_workshop_products`;
  - the bowl test's user stands in the test's own space, and only bowls inside its bounds are counted;
  - the recipe edges read once.
- **Tests (none run):**
  - `ConcordanceAlchemyGameTests` +7 (32 in all; 33 with the unit review's);
  - `ConcordanceAssayGameTests.workshopProductsAreNeverWeighed` (7);
  - `LairEncounterGameTests` +8 (18);
  - `EncyclopediaGameTests` `theCinderKilnsRiteNamesTheKilnDraught` and `theDossiersNameThePoulticesCredit` (20 with
    C-01's);
  - `crucibleInspection`'s poultice preview (shot `jugcraft_encyclopedia_inspect_poultice`).
  Every number in their assertions is the model's or vanilla's arithmetic until they run.
- **Commands,** for each commit:
  - run and passed: as C-01's;
  - the audit mutations on a scratch copy, each reported: 30 for commit 3 and 22 for commit 4;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

**Fixes from the unit review (unit 1, a sixth commit).** The review confirmed thirteen findings; each is fixed here, and
none of the changed Java has run in a game.
- **Equal doses** (C-02). `Mixture.bottle` gave each dose a part's share rounded down and left the remainder for the
  last, so a two-part poultice bottled 1.063 then 1.064: two different salves, and an automated crucible paused on its
  own second dose (`output_differs`). Now every dose of a batch is the same share (`Vector.times`); what does not divide
  is lost, at most a thousandth a part an axis, so nothing is gained. The generator's `bottle` mirrors it,
  `check_workshop_products` holds both, and `dilutionNeverHealsMore` expects 1.063 twice and runs the two-part batch as
  a formula into one stack of two.
- **The risk line hid a live batch** (C-01, 03:54). Set over a mixture already in the crucible, or under a formula the
  rules refuse, the risk line judged only the formula's replay, though a dose of the mixture can still be bottled by
  hand. `Assay.risks` now takes whether the mixture is the formula's own batch and names its harm and searing too;
  the Inspect page passes it through `CrucibleInspection.State`, and `formula()` passes the mixture it is set over.
- **Harm as the server judges it** (C-P29). The risk line asks `Alchemy.harmfulStatus` (a harmful or unknown status) as
  well as the property's data.
- **The refusal a formula item sends** is built by `CrucibleBlockEntity.refusal`, which
  `aWrittenFormulaThatNoLongerReplaysSaysWhichStep` asserts against step 2's refusal.
- **Preserve's hold** is the reason only while, unheld, the block beneath would move the heat towards the stir's band;
  over a magma block a hot stir waits for heat, held or not (`aHoldIsTheReasonOnlyWhileItKeepsTheHeat`, new).
- **The dossiers' poultice credit** is named only while a salve of Regeneration alone is the Stillroot Poultice, the
  match `Alchemy.product` makes for the credit.
- **Audits made exact:** `canPlaceItem`'s two returns and the mortar's whole refusal condition, so a widened condition
  fails; `formula()`'s refusal and risk line; the Inspect page's risk-line inputs; the dossier's product match.
- **Words:** the searing danger lines also say a searing stir thaws all Rime.
- **Records:** the Peepo mappings each test asserts
  ([companion-farm-stand-crucible.md](companion-farm-stand-crucible.md)), with the Peepo status now asserted at the
  `NEEDS_WATER`, `NEEDS_BOTTLES`, `NO_INPUT`, `OCCUPIED` and `WRONG_TEMPERATURE` waits;
  commits 1 and 2's mutations (C-01's Commands); every change needing the Alembic Arts
  ([arcane-concordance-authority.md](arcane-concordance-authority.md)); `follow(Formula)` and the credit row's chunks in
  `WHAT_EXISTS.md`; the client test's comment, and the Assay Glass put back after its crucible shots.
- **Commands:** as C-01's, run and passed; the fixes' twelve audit mutations on a scratch copy, each reported. The pure
  alchemy packages compiled with `javac` against a stub of jspecify's `@Nullable`, and a scratch harness replayed the
  new assertions' alchemy against the real data and agreed (the risk lines with a stand-in for the registry's harm
  test). Not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

### C-03. Ritual readiness, fault positions, spent and reserved (unit 2)

**What the player sees.**
- **The Inspect page on an anchor**, for the rite the offerings point to (or the closest known one), lists every
  shortfall at once: each offering short; Ley per pylon for one step and for the whole run against what it holds
  (`RitualDefinition.leyPerRun`, `:100-102`); pylons that will not lend; participants; the viewer's Focus; light; every
  shape fault with its relative and absolute position. Then the rite's **stage and payment table** (C-04's form):
  what Begin pays, what each step draws, what completion consumes, what an interruption keeps and loses, and the
  **backlash** a broken containment deals each participant, stated before the start (03:122).
- **During a run** (gathering and channelling), the Inspect page and Jade show what is **spent** (Focus × joined; Ley:
  done × Ley × channels) apart from what is **still reserved** (the offerings), and the interruption line says both
  (03:107; 10:48).
- A refused start, an empty-hand status reading and a chat status mark every faulted part with a WANT or PERIL sign, not
  only an interruption. The Inspect page shows none: it only reads.
- Jade shows the participants needed and the offerings. An interruption's reason stays readable, in one line with
  "kept".

**Changes.**
- A pure `concordance/ritual/Readiness`, built from the anchor's cached shape report
  (`CircleAnchorBlockEntity.java:521-534`), at most 16 pylon `ley()` and `lends()` reads (a pattern has at most
  `StructurePattern.MAX_CHANNELS` = 16 channels, `ritual/StructurePattern.java:21`; the Lesser Circle has four), the
  viewer's Focus and knowledge, participants present and the light. No second validator (12:174). Its lines use C-01's
  shared vocabulary.
- The selection loop (`CircleAnchorBlockEntity.java:306-329`) becomes one function that `begin` and readiness share;
  unknown rites stay unnamed (`InspectPage.java:66-80`).
- **The start does not change** (C-P10): one step's Ley (`CircleAnchorBlockEntity.java:508`). A run-level shortfall is a
  warning, so a pylon fed by JE (64 JE/t, about 2.56 Ley a 40-tick step by arithmetic, `LeyPylonBlockEntity.java:39-40`)
  can still sustain a run.
- **Signs only on the empty-hand, chat and refusal paths** (`CircleAnchorBlockEntity.java:216-258, 330-360`), reusing
  the interruption's loop (`:658-668`) within the budget (48 a tick plus 16 warnings,
  `concordance/sign/Signs.java:29-31`). Not inside the anchor's `status(level, out)`
  (`CircleAnchorBlockEntity.java:421-437`), which the Inspect page calls (`InspectPage.java:62`) and which must only
  read (`InspectPage.java:33`); `Signs.show` sends to every player tracking the block within its reach
  (`Signs.java:95-113`).
  `Signs.show` joins the encyclopedia's forbidden list (`check_mod_data.py:16174-16180`).
- The reason and "kept" become one combined overlay line (`CircleAnchorBlockEntity.java:650-656`).
- Jade: the anchor snapshot (`ConcordanceDataProvider.java:119-136`) adds the participants needed, the offerings and,
  during a run, spent and reserved; the client fixes the "?" (`JugcraftJadeClient.java:290`) and uses the dormant keys.
- A checker rule in `check_rituals` (`check_mod_data.py:12330`): steps × Ley ≤ a pylon's capacity (64,
  `LeyPylonBlockEntity.java:37`) for every rite. No rule compares them today: `check_rituals` holds each number to its
  own limit only, and those limits allow 30 steps × 16 Ley = 480 (`RitualDefinition.MAX_STEPS`, `MAX_LEY`,
  `ritual/RitualDefinition.java:28-29`).

**Reuses unchanged:** `RitualMachine`, `RitualRun`, `StructureValidator`, `Offerings`, the Signs budget, the encyclopedia
payloads.

**Tests (server):**
- `ConcordanceRitualGameTests` (18 today): `anIdleCircleSaysEverythingARiteStillNeeds`; `aRefusedStartMarksEveryFault`
  (`SignWatch`); `aStatusReadingMarksFaultsButTheInspectPageDoesNot`; `spentAndReservedAreShownApart` (mid-run and after
  each interruption kind); `theInterruptionReasonIsKept`; `unknownRitesStayUnnamed`.
- `theCircleIsCheckedPartByPart` must keep passing unchanged: its comment says charge and ownership count only when a
  ritual asks, and it asserts that someone else's pylon and a pylon short of a step's charge each refuse the start,
  taking no Focus (`ConcordanceRitualGameTests.java:229-240`).
- `EncyclopediaGameTests`: the anchor's Inspect readiness and payment lines
  (`theInspectPageShowsARitesReadinessAndPayments`).

**Tests (client):** an anchor's Inspect page (`jugcraft_encyclopedia_inspect_readiness`); a Jade shot of a gathering
anchor (`jugcraft_concordance_anchor_gathering_jade`, when Jade is loaded).

**Acceptance:** "Show missing positions and resource readiness" (09:132); 03:107; 03:122; 10:48.

**Save effects:** none. **Network:** no new payload; Jade's anchor snapshot carries more values.

**Review boundary:** ritual diagnostics only; no change to `RitualMachine`.

**As built (unit 2: commit `a0e73881c`).**
- **Built:**
  - the pure `ritual/Choice`: the one choice of rite a leader's start and readiness share (`CircleAnchorBlockEntity.choose`);
    nothing offered chooses nothing;
  - the pure `ritual/Readiness`: `before` (every shortfall before a start, each marked as refusing or not), `running`
    (spent apart from held, and what may stop the next step, the light included), `stages` (the stage and payment table
    from the rite's data, for every rite), `shapeFaults`; with `Spent`, `Held`, `Ending`, `Channel`, `Line` and
    `Reader`;
  - the anchor's `readiness` (the cached report, one reading of each channel, the reader's Focus and place, the light;
    no sign), `spent`, `ending` and `endingLine`; `participantsNeeded` made public for Jade;
  - signs: a reading in chat (an empty-hand reading, the command) marks every faulted part, once every `MARK_TICKS` (20)
    ticks a player (`markReadiness`); a refused start marks the parts that refused it (`markFaults`, which an
    interruption now shares). Nothing on the Inspect page's or Jade's path shows a sign;
  - an interruption in one line (`circle.ended`, `.ended.spent`: why, where the offerings are, the Focus and Ley Charge
    lost), its `Ending` kept in memory until the next start or a load; `circle.kept` gave way to `circle.release.kept`
    and `.release.dropped`;
  - fault positions as "x, y, z (dx, dy, dz from the anchor)" (`circle.where`); Jade keeps "x,y,z";
  - the Inspect page's anchor branch: readiness, or spent and held while a rite runs, the last attempt, and the stage
    table, for a rite the reader knows;
  - Jade: the participants needed (the "?" fixed), how many are channelling, the first three offerings and "+n",
    spent and held while a rite holds the anchor (`ConcordanceDataProvider.SHOWN_OFFERINGS`);
  - five diagnostic codes appended to `Code` and `tools/concordance_diagnostics.py` (`not_lent`, `needs_focus`,
    `too_bright`, `too_far`, `participants`); `unavailable_power` and `incomplete_pattern` given their parameters, and
    `missing_ingredient`'s words made general (C-P33); `ComposeText` names a `structure`;
  - audits: `check_rituals` (steps × Ley Charge within a full pylon's 64; every rite at the anchor on one structure; one
    choice; the read-only paths; the chat reading's marks and gate; a refused start's marks and the one-step start; one
    `tell` an interruption; the readiness words; Jade's anchor snapshot keys, and that it names no rite, Focus or
    research; the client's "?" gone) and `check_encyclopedia` (`markReadiness`, `markFaults` and the chat reading
    forbidden on the encyclopedia's path).
- **Departures from the ticket,** each a proposal below or a spec reconciliation:
  - "participants" is not an idle shortfall: a start needs only its leader. Before a start the others appear as the
    stage table's gather row; while a rite gathers, a `participants` line shows joined, needed and the seconds left
    (C-P34);
  - each shape fault is not repeated in readiness: one `incomplete_pattern` line counts the faults to mend, and the
    anchor's status lines above it, on the same page, list each with both positions;
  - a pylon short only of the whole run is a warning naming the step the run would break off at; the start is unchanged
    (C-P10);
  - a fully matched rite the reader has not learned is never named on the Inspect page or in Jade; the start's refusal
    still names it, as before (C-P34);
  - a joined participant who has walked away is not listed while a rite runs: the next step's own check finds them;
  - the held line leaves out an item the client does not know, rather than showing an empty icon;
  - in the tests, pylons are set with `setLey(level, n)`; the idle test waits 5 ticks after placing glowstone, as
    `theVigilNeedsTheDark` does, before reading the light; the client circle for the Jade shot is walled and roofed so
    the vigil's light check holds at any hour (the overworld's midnight sky light was not confirmed), with
    `time set midnight` kept as well.
- **From the spec review:** `InspectPage.anchor` declares its ritual list once (the spec's block would not have
  compiled); running readiness reads the light, as the ticket's "what may still stop it" asks; the Jade shot is taken
  under night vision so the sealed circle is not black (night vision changes only the client's rendering, not the
  server's light); the client check rejects `jugcraft:` ids as well as keys; `incomplete_pattern` reads "... is not
  complete: faults to mend: n"; the decisions are numbered C-P33 on, after unit 1's C-P32.
- **Tests (none run):**
  - 6 new `ConcordanceRitualGameTests` (24 then): `anIdleCircleSaysEverythingARiteStillNeeds`,
    `aRefusedStartMarksEveryFault`, `aStatusReadingMarksFaultsButTheInspectPageDoesNot`,
    `spentAndReservedAreShownApart`, `theInterruptionReasonIsKept`, `unknownRitesStayUnnamed`;
  - `EncyclopediaGameTests.theInspectPageShowsARitesReadinessAndPayments` (21);
  - the client test `EncyclopediaClientGameTests.anchorReadiness`, with shots `jugcraft_encyclopedia_inspect_readiness`
    and `jugcraft_concordance_anchor_gathering_jade` (when Jade is loaded).
  No test reads the action bar's one line: a mock player's overlay cannot be read. No test covers running readiness's
  light line; only the scratch harness below does.
- **Commands** for commit `a0e73881c`:
  - run and passed: `python3 tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
    `python3 tools/owner_art.py --check`, `python3 tools/concordance_journey.py --check`,
    `python3 tools/check_icon_maps.py`, `node --test tools/world-designer/model.test.cjs`,
    `python -m unittest discover -s scripts/tests` and `git diff --check`;
  - the spec's audit mutations on a scratch copy, each reverted: all 19 caught (its 14, five of them in two variants);
    one more, running readiness without the light, is caught by no audit;
  - a scratch harness outside Git, re-run for the records at `2c98d6d68`: the pure Concordance packages compiled with
    JDK 21 and 37 checks of `Choice` and `Readiness` with the game tests' figures passed;
  - not run when these notes were written: `./gradlew build` (with or without the optional integrations),
    `./gradlew runClientGameTest` and CI (they have run since, on later heads: see the status at the top).

### C-04. Root and Rivet (unit 2; C-D2)

**What the player sees.** A rite at the Lesser Circle turns one Thallite plate into a **Rooted Thallite Foundation**,
the base the Crucible Stand is built on (C-05). Its stage and payment table, on the Inspect page before the start
(C-P11 holds the numbers, starting values):

| Stage | When | Paid at this stage | Still reserved | Spent so far | If interrupted |
|---|---|---|---|---|---|
| Offer | the anchor idle | — | nothing (slots open) | — | a sneaking use takes the offerings back |
| Begin | the leader's empty-hand use | 6 Focus | the offerings: 1 Thallite plate, 4 copper ingots, 4 Mendvetch | 6 Focus | offerings kept and unlocked; the Focus is lost |
| Step k of 4 | every 40 ticks (8 s in all) | 2 Ley from each of the 4 pylons | the offerings | 6 Focus and 8k Ley | offerings kept; spent Ley and Focus lost; a broken containment also deals each participant 4 backlash |
| Commit | after step 4 | the offerings consumed and the foundation made, in one tick | — | 6 Focus, 32 Ley, the offerings | — |
| Complete | until a participant takes it | — | the foundation, in the anchor | — | the anchor broken drops it |

32 Ley is 48 Radiance or 48 Verdance through a pylon's conversion, or 32,000 JE (arithmetic); 8 a pylon fits its 64.
One participant; any light. Copper and Mendvetch are the chain's "copper/iron components" (06:78) and "Verdance-compatible
binding from existing garden materials" (06:80). The offerings collide with no existing rite's (the attunement's wand,
amethyst, gold and glowstone: `concordance/ritual/adept_attunement.json`); the most specific match wins
(`CircleAnchorBlockEntity.java:313-317`).
A completion credits Stratawrighting's `foundation` through Chunk B's listener (`Strata.java:34`).

**Changes.**
- Data `concordance/ritual/root_and_rivet.json`, a `Transform` (`RitualDefinition.java:63-64`): no machine change.
- Generator: `RITUALS` in `tools/concordance_rituals.py:112`; the JEI page is automatic for a transform
  (`recipe_station()`, `:593`).
- New item `rooted_thallite_foundation`: a 16×16 icon per `docs/ITEM_ICONS.md` (the owner's library searched first,
  CLAUDE.md), lang, a codex entry linked from Stratawrighting.
- Research (C-P11): Circle Lore understood, like the three rites (`concordance/research/circle_lore.json:53-61`).
- Registrations: `PRODUCERS` in `tools/concordance_progression.py:101` and `RITUALS` in `tools/acquisition.py:96` (each
  lists the attunement's wand today); the rite as a Ley sink in `tools/concordance_economy.py` (06:144).
- **Feature switches:** the plate's recipes load only with `machines` (both) and `thallite` (by hand):
  `recipe/thallite_plate_by_hand.json:2-11`, `recipe/pressing/thallite_ingot.json:2-7`. With either off the rite exists
  but its plate cannot be made, and the powered route (C-05) is out of reach; the campfire crucible is not.
  `check_rituals` states that dependency. The rules parser tolerates `fabric:` keys
  (`concordance/rules/RulesParser.java:671-679`), but whether Fabric applies load conditions to the Concordance's own
  loader is unverified and no Concordance data uses them, so the rite carries none.

**Tests (server):** `ConcordanceRitualGameTests`: `rootAndRivetAttunesOnePlateOnce`;
`aDryPylonStopsRootAndRivetAndKeepsTheOfferings`; `rootAndRivetSavedMidRunLapses` (the pattern of
`aRitualSavedMidRunLapses`); `theAttunementsOfferingsNeverChooseRootAndRivet`; `rootAndRivetIsStrataFoundationWork`.
`ConcordanceAssayGameTests.theFoundationIsNeverWeighed`. `ConcordanceProgressionGameTests`: the node
`ritual:jugcraft:root_and_rivet` joins the list at `ConcordanceProgressionGameTests.java:130-136`.

**Acceptance:** "Implement one of Stillwater Keeping or Root and Rivet with a declared stage/payment table. Use existing
RitualMachine and pylon economics" (09:132); interruption tested (09:140).

**Save effects:** new stable ids `jugcraft:root_and_rivet` and `jugcraft:rooted_thallite_foundation`, in use from the
first release.

**Review boundary:** ritual data, one item, the generator and audit entries.

**As built (unit 2: commit `2c98d6d68`).**
- **Built:**
  - `concordance/ritual/root_and_rivet.json`, generated from `RITUALS` in `tools/concordance_rituals.py` with C-P11's
    numbers; Circle Lore understood unlocks it;
  - the item `rooted_thallite_foundation` (`JugcraftConcordance.ROOTED_THALLITE_FOUNDATION`, a `DescribedItem` in the
    Ingredients tab), its lang, models and icon;
  - the codex entry `circles/root_and_rivet`: the rite, "Stages and Payment" (`_payment_text`), the plate's crafting
    recipe with the switches it needs (`_switches_text`), and the foundation; Stratawrighting's Understood page links it,
    the codex's first `entry://` link;
  - JEI: the rite in the Circle Anchor category and an information page on where the foundation comes from
    (`tools/acquisition.py` `RITUALS`);
  - registrations: `PRODUCERS` in `tools/concordance_progression.py`, `EXCLUDED` in
    `tools/concordance_equivalence.py` (and the generated tag), and `RITUAL_SINKS` in `tools/concordance_economy.py`
    with its row in the economy record's table.
- **Three checker gaps found by the design and fixed:**
  - `check_rituals` looked an offering's recipe up by file name; it now counts any recipe, in any recipe folder, whose
    result the offering is (the plate's hand recipe is `thallite_plate_by_hand`, its pressing `pressing/thallite_ingot`);
  - `#c:ingots/thallite` joined the progression's `VANILLA_TAGS`, so the plate's hand recipe has a reachable input;
  - `check_progression` asks for a codex recipe page readable before a step needs the plate: the entry's crafting page.
- **The switch statement** (C-P41): `RITUALS` states the feature switches a rite's offerings need beside the
  Concordance's (`"switches": ["machines", "thallite"]`), and `check_rituals` holds the statement to the recipes, one
  step down, an offering that is a tag included. Concordance data carries no load condition, and the checker forbids
  one.
- **Also held by the checks:** what a ritual makes is excluded from the scale, made by that ritual in the progression
  graph and given its JEI page (`check_rituals`); every codex `entry://` link names an entry on disk, the foundation's
  icon is its map pixel for pixel, and the `thallite` material's four tones are the metal's (`check_concordance`); the
  sink is its ritual's run (`check_economy`).
- **The icon:** the owner's library was searched first (its candidates are in the rituals record's Item-icon
  provenance); nothing fitted, so it is drawn fresh to ITEM_ICONS.md from `tools/item_icons/rooted_thallite_foundation.txt`
  in a new `thallite` material, the Thallite metal's ramp with a darker outline. Not yet reviewed by the owner.
- **Departures from the spec:**
  - the icon's PNG was written by the texture generator's own `save` for this one image, from
    `concordance_ritual_art.textures()`, rather than by running `tools/generate_textures.py` whole, which this unit's
    instructions ruled out; `check_concordance` holds it to its map;
  - from the spec review: the codex's payment page reads "k times 8 Ley Charge" and names Verdance beside Radiance and
    JE; the JEI page says "Root and Rivet turns", without "the", and only an instrument "keeps what was inscribed";
    `RITUAL_SINKS`' comment says why the Adept's Attunement is not a sink (its wand is an excluded instrument); the switch
    audit reads an offering that is a tag through its members; the five ritual tests, their helpers and the assay test
    were written from the review's code.
- **Tests (none run):**
  - 5 new `ConcordanceRitualGameTests` (29 in all): `rootAndRivetAttunesOnePlateOnce`,
    `aDryPylonStopsRootAndRivetAndKeepsTheOfferings`, `rootAndRivetSavedMidRunLapses`,
    `theAttunementsOfferingsNeverChooseRootAndRivet`, `rootAndRivetIsStrataFoundationWork`;
  - `ConcordanceAssayGameTests.theFoundationIsNeverWeighed` (8);
  - `ConcordanceProgressionGameTests.theLiveGraphIsWhole`: the node `ritual:jugcraft:root_and_rivet`, reachable alone;
  - the client shot `jugcraft_rooted_thallite_foundation` (`EncyclopediaClientGameTests.rootedFoundation`).
- **Commands** for commit `2c98d6d68`:
  - run and passed: as C-03's;
  - the spec's audit mutations on a scratch copy, each reverted: its 13 in 22 variants and one more for a tag offering,
    23 caught. Mutation 13 (Mendvetch deleted from `_item_name`) surfaced as a crash (`KeyError`), not as a reported
    error. A control (C5', correction 5's `offering_needs` undone, the spec's own loop) raised no switch error for the
    tag offering: it failed only on the scratch rite's unrelated research, lang and progression errors, so the spec's
    loop misses a tag offering. A foundation-to-Ley conversion of 31 is, as the spec expects, not a gaining cycle;
  - `python3 tools/select_client_tests.py --base 807da7f5e` chose `EncyclopediaClientGameTests`;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

**Fixes from the unit review (unit 2, a fifth commit, after the merge `2263f851b`).** The review confirmed seventeen
findings. Sixteen are fixed here or in the merge; the seventeenth, a commit message, is left as written. None of the
changed Java has run in a game.
- **The branch had moved on.** Unit 2 was built on `807da7f5e`, and the branch has since gained `d593ae61d` (unit 1's
  review fixes) and `0b1039312` (the Rime and Strata client tests). They were merged in (`2263f851b`), not rebased, so
  the commits these records cite keep their hashes and the branch moves forward without a force-push. Two conflicts
  were resolved: the status paragraph, and an import block in `EncyclopediaClientGameTests` (both kept). Commit
  `2c98d6d68`'s message says "a third rite" where Circle Lore unlocks a fourth; rewording it would rewrite that commit,
  so it stands.
- **A pylon gone while a rite runs** (C-03). While a rite runs the anchor shows the last step's shape report, and
  running readiness skipped a channel with no pylon, so a pylon removed or unloaded said nothing until the next step
  broke the rite off. `Readiness.running` now counts such channels in one blocking `incomplete_pattern` line, and
  `spentAndReservedAreShownApart` asserts it with one channel read as gone. Another part broken since the last step
  still shows only at the next step (Risks).
- **The whole-run warning** ends "unless it is refilled meanwhile" (`ritual.ley_run`, `ritual.ley_left`): on JE a pylon
  gains about 2.56 Ley Charge a step, more than any rite draws from it (C-P10).
- **Marks only for what blocks** (C-P35). A reading in chat marks a pylon for a blocking line (short of a step, or not
  lent), never for a warning (`markReadiness`).
- **Words.** The held line reads "Held until it ends, and consumed only if it completes", since a rite that breaks off
  keeps or drops its offerings. The ending line reads "the attempt lost %s Focus and %s Ley Charge": the Focus is every
  participant's together, and each participant is told it (C-P37).
- **The switch audit** (C-P41) follows a machine recipe's `{"ingredient", "count"}` entries (an advanced circuit also
  needs `lead` and `silicon`), and counts a condition with `"or"` alternatives as needing none of its switches alone (a
  chestnut slab needs no `agriculture`). Every rite's result is unchanged: Root and Rivet needs `machines` and
  `thallite`.
- **The codex-link audit** (C-P42) also holds a link's `@anchor` to a page id of its entry and its `#n` to one of its
  pages, as Modonomicon 2.16.0's `BookLink.fromEntry` does.
- **The economy's ritual sink** words its offerings from `tools/concordance_rituals.py` (`offerings()`), as the JEI page
  does, so the economy table follows the rite; its row is re-pasted from the tool.
- **Records.** C-04's mutation count above (23 caught, a control, mutation 13 a crash). Unit 2's insertions marked "not
  merged, not run" (since unit 5's records, "not merged" with the CI run that passed) in [arcane-concordance-rime-strata.md](arcane-concordance-rime-strata.md),
  [arcane-concordance-economy.md](arcane-concordance-economy.md), [thallite.md](thallite.md) and
  [jei-integration.md](jei-integration.md). Thallite's switch sentence (a plate made earlier can still be offered). The
  icon table's full path for `table_plate.png` and the rituals record's lang list ("among them"). Two more unconfirmed
  risks: the codex pages' length, and the recipe page when a switch removes the plate's recipe.
- **Commands** for the merge and the fixes:
  - run and passed on the merged tree with the fixes: `python3 -B tools/generate_material_data.py` (the merge
    regenerated unchanged; the fixes changed only the lang), `python3 -B tools/check_mod_data.py`,
    `python3 scripts/check_repository.py`, `python3 tools/owner_art.py --check`,
    `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
    `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
    `git diff --check`;
  - the fixes' audit mutations, in place and each reverted. Reported: fix 4 undone (a scratch rite offering an
    advanced circuit, stated with its five switches), fix 5 undone (one offering a chestnut slab, stated with none),
    a link to `@p9`, `#9` or `#x`, and Root and Rivet taking 5 copper ingots without the economy table re-pasted. Not
    reported, as intended: a link to `#1`, `@p1` or `#3@p0`. Controls: each scratch rite with its fix in place, a link
    to `@p9` under the old link audit and the copper change under the old hand-written offerings were not reported,
    which is what the old audits missed;
  - C-03's and C-04's suites re-run in place on the fixed tree: the same results as above (C-03's 19 caught, its extra
    light case missed; C-04's 23 caught, mutation 13 a crash, C5' raising no switch error);
  - the pure readiness classes compiled with `javac` against a stub of jspecify's `@Nullable`, and the scratch harness
    passed every check, three new ones included (a pylon gone blocks; every pylon there says nothing; a completed run
    says nothing);
  - `python3 tools/select_client_tests.py`, on the fixes as staged: against the branch's tip `0b1039312` (unit 2 with
    its fixes) it chose `EncyclopediaClientGameTests`, and against the merge `2263f851b` (the fixes alone) the same.
    Against `807da7f5e` it now chose ten classes, because unit 1's review fixes and the Rime and Strata client tests are
    in that range too;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

### C-05. The Crucible Stand: JE heat for the next stir, piped water (unit 3; C-D3)

**What the player sees.**
- A stand under a crucible, crafted from the Rooted Thallite Foundation, iron and copper (C-P12), takes JE from cables.
  With a formula running it heats toward the band of the formula's next stir: cold off (the crucible settles at the
  ambient 20), warm 65, hot 120, searing 175 (starting values), drawing about 8, 16 and 32 JE a tick (C-D3's starting
  rates). A formula of mixed bands replays unattended; today a crucible waits silently for its band
  (`CrucibleBlockEntity.java:276-280`).
- It idles, drawing nothing, while the crucible waits for anything but heat (water, an ingredient, a container, room,
  a hold) or its formula is incompatible: no unlimited burning (03:193). A Preserve hold wins over the stand.
- With no formula, an empty-hand use by someone who may change the stand cycles off, warm, hot, searing (06:115's "select
  a known heat schedule"), chosen on the server and rate-gated; no client slider (08:59).
- Unpowered it gives nothing, and the crucible cools by the existing rule (`Heat.java:25-31`).
- Water comes from an Electric Pump on a water source (8 JE a tick, paid only on ticks it moves fluid; a source block is
  an endless spring: `fluid/ElectricPumpBlockEntity.java:41, 101-104, 123-125`) through a pipe into the crucible's tank,
  which takes water only and never gives it back (`CrucibleBlockEntity.java:124-145`; `JugcraftConcordance.java:259`).
- A campfire crucible works exactly as before. Jade on the stand shows its energy, setting and draw.

**Changes.**
- A block and block entity with a `LIT` state: a `SimpleEnergyStorage` registered on `EnergyStorage.SIDED` as the pylon's
  is (`JugcraftConcordance.java:242`; `LeyPylonBlockEntity.java:47-48`) and `EnergyConnectable` (`LeyPylonBlock.java:42`).
  It pays JE only on a tick when its target is above ambient and a crucible sits directly above (one `getBlockEntity`,
  no scan), and only while the crucible's reason (C-01) allows.
- `CrucibleBlockEntity.heatBelow` (`:233-241`) asks the stand when the stand is beneath. The targets are constants in
  `Heat`, mirrored beside the heat constants in `tools/concordance_alchemy.py:33-36` as `Heat.java:5-10` requires.
- **Authority** (C-P12): the manual setting asks `Authority.mayChange`.
- **Feature switch:** with `concordance` off the stand draws no JE and gives no heat, as the crucible stops
  (`CrucibleBlockEntity.java:213, 388`).
- **No wear and no maintenance** (D-004, 10:157).
- **Economy:** a one-way "JE → heat only" row in `tools/concordance_economy.py` and `arcane-concordance-economy.md`.
- **Art:** the owner's library is searched first (CLAUDE.md; candidates listed in `art/owner-library/catalog/files.csv`,
  suitability not yet judged: `heating_tray_side.png` and `heating_tray_top.png` in "farming and food textures", the
  treated-iron set in `Blocks/Guns/block`); an import goes through an `OWNER_FILES`-style table so
  `owner_art.py --check` protects it (that file is also changed by #351 and #354), otherwise generated in code as the
  crucible's GeckoLib model is (`tools/concordance_alchemy.py:531-532`). It follows main's two new art rules
  (`docs/ART_DIRECTION.md`: supported parts and purposeful pipes; silver-grey cladding) and gets power-port sockets
  through `machine_connections.with_model_sockets`
  (`tools/machine_connections.py:75`), as the pylon does (`tools/concordance_rituals.py:527-529`). Owner visual review is
  a checkpoint item (09:144).
- **Jade:** a provider for the stand (`compat/jade/JugcraftJadePlugin.java:30-45` registers one per block-entity class).

**Tests (server):**
- `ConcordanceAlchemyGameTests`: `aStandFollowsTheFormulasBands` (a warm-then-hot formula replayed twice, unattended);
  `anUnpoweredStandLetsTheCrucibleCool` (the reason is wrong heat; the Peepo port says `WRONG_TEMPERATURE`, as it does
  today for a stir out of its band, `CrucibleCompanion.java:100`);
  `aStandIdlesForAWaitingOrIncompatibleFormula`; `aStandIsSetOnlyByThoseWhoMayChangeIt`;
  `theFullChainRunsFromAPumpAndACable` (a creative cell, a cable, the stand; a pump on water, a pipe, the tank; the exact
  water drawn); `aCampfireCrucibleStillWorks`; `aSwitchedOffConcordanceStandDrawsNothing`.
- `ConcordanceEconomyGameTests` (2 today): `aStandTurnsElectricityIntoHeatOnly` (JE drawn equals rate × heated ticks; an
  idle stand draws nothing; nothing comes out).
- `ConcordancePersistenceGameTests`: the stand's save and stamp.
- A cable connect-and-retract check after main's `MachineConnectionGameTests` (`:27`, `:82`), which covers only machine
  blocks.

**Tests (client):** a workshop shot at the ordinary scale.

**Acceptance:** "Connect an existing station or a minimal adapter to JE/water" (09:136); "A powered version improves
convenience" (09:140); the industrial apothecary "does not unlock all magic automatically" (03:25): research, exact
ingredients and process still gate it.

**Save effects:** a new block entity (energy, manual setting); new stable ids `jugcraft:crucible_stand` (block, item,
block entity). An older build drops the unknown block.

**Review boundary:** one block, the heat hook, the economy row, its art and tests.

**As built (unit 3: commits `940f1f8a8`, the art, and `7b2dd6783`; record [crucible-stand.md](crucible-stand.md)).**
- **Built:**
  - `CrucibleStandBlock` (`LIT`; a ticker on the server only; `useWithoutItem` for an empty hand, `useItemOn` passing
    anything held to the item) and `CrucibleStandBlockEntity` (`energy`, a `SimpleEnergyStorage` of 640 JE taking at
    most 64 a tick, extracting none, its `insert` closed with the switch off; `setting`; `given`; `plan`, `heat`,
    `useEmpty`, `statusLine`; the record `Plan`), registered in `JugcraftConcordance` as `CRUCIBLE_STAND` and
    `CRUCIBLE_STAND_ENTITY`, with `EnergyStorage.SIDED` null for `UP`;
  - `Heat`: `STAND_WARM`, `STAND_HOT`, `STAND_SEARING` (65, 120, 175), `standTarget`, `waitsForHeat`, `standBand`, and a
    third component of `Heat.Below`, the heat a stand paid for; `CrucibleBlockEntity.below` (the ticket's `heatBelow`,
    renamed by unit 1) asks a stand beneath for `given()`: one block-entity read, no scan;
  - the diagnostic code `no_crucible`, appended after unit 2's codes; "too little JE" is `unavailable_power` with three
    lines (C-P49);
  - Jade: `ConcordanceDataProvider.STAND` and `standSnapshot` (read-only; `energy`, `capacity`, `following`, `aim`,
    `target`, `draw`, `reason`, `waits`, or `disabled`), the client's own `StandTooltip`;
  - generated from `tools/concordance_alchemy.py`: `STAND_TARGETS`, `STAND_JE`, `STAND_CAPACITY`, `STAND_INTAKE`,
    `STAND_SET_TICKS`, `STAND_PORTS`, the two models (sockets through `with_model_sockets`), the blockstate, item, loot,
    recipe, lang, the mineable tag and the codex entry `alembic/crucible_stand`; `OWNER_BLOCKS_FILES` for the art,
    copied by `tools/owner_art.py`, whose `block_asset_imports` now merges every slice's Blocks-folder table;
  - `tools/concordance_economy.py`: the resource `crucible_heat`, the conversion `je_to_heat` and its anchors,
    `STAND_JE` and the installation row (768,000 JE a day searing); the economy record's table pasted from the tool;
  - unit 2's foundation: its tooltip and codex page now name the stand (`tools/concordance_rituals.py`);
  - audits: `check_crucible_stand` (new), the stand's constants and recipe in `check_alchemy`, its route in
    `AUTHORITY_ROUTES`, the heat sink in `check_economy`.
- **Departures from the ticket,** each a proposal below or a review correction:
  - no `EnergyConnectable` marker: a cable meets the stand only on a face that takes JE, never its seat (C-P44);
  - it pays only while the crucible is no hotter than the band's target (C-P43); a `HELD` reason counts as a wait for
    heat (`Heat.waitsForHeat`) and the hold itself is judged from the crucible's clock, so a hold that has ended never
    idles the stand;
  - the recipe also needs the `machines` and `thallite` switches (C-P47; the spec had `machines` only, and its review
    added `thallite`, which Root and Rivet's plate needs);
  - only an empty hand sets or reads it; anything held is used as itself, so a crucible is placed on it by a plain use
    (`useItemOn`; the review's correction: the default use would have cycled the setting for any item held);
  - the art is the owner's (C-P48): treated iron plates and block and the copper coil seen from its side, byte copies,
    rather than a code-drawn coil, so `tools/concordance_alchemy_art.py` and `tools/generate_textures.py` are not
    touched. The owner's end-on coil is not imported: in this model the coil's top lies under the deck and its bottom on
    the cladding, so it would never show (the review's correction);
  - Jade's stand snapshot names its band `aim`, since the crucible's client branch matches the first `band` key, and
    its lines are their own enum;
  - the cable check is `ConcordanceAlchemyGameTests.aCableMeetsTheStandWhereItTakesPower`, not a new
    `MachineConnectionGameTests` case; the six cables are placed first, then the stand (the order does not matter:
    a cable's placement schedules the tick that resolves its arms);
  - in `theFullChainRunsFromAPumpAndACable` the spring stands on stone, so its water does not spread into neighbouring
    tests (the review's correction).
- **Tests (none run):**
  - 8 new `ConcordanceAlchemyGameTests` (41 in all): `aStandFollowsTheFormulasBands`,
    `anUnpoweredStandLetsTheCrucibleCool`, `aStandIdlesForAWaitingOrIncompatibleFormula`,
    `aStandIsSetOnlyByThoseWhoMayChangeIt`, `theFullChainRunsFromAPumpAndACable`, `aCampfireCrucibleStillWorks`,
    `aSwitchedOffConcordanceStandDrawsNothing`, `aCableMeetsTheStandWhereItTakesPower`; and
    `anIncompatibleFormulaAsksForNothing` extended (no heat from a stand beneath);
  - `ConcordanceEconomyGameTests.aStandTurnsElectricityIntoHeatOnly` (3 in all);
  - `ConcordancePersistenceGameTests.theCrucibleStandKeepsItsEnergyAndSettingThroughASave`, and the stand in
    `blockEntitiesStampTheirVersion`;
  - the client test `EncyclopediaClientGameTests.crucibleStandWorkshop`, with shots
    `jugcraft_concordance_crucible_stand_workshop` and `jugcraft_concordance_crucible_stand_jade` (when Jade is loaded).
- **Commands** (for the records, on the whole unit): see C-06's. The spec's C-05 audit mutations, in place with every
  file restored after each: its 22 in 33 variants and the review's two (the `useItemOn` override deleted, the
  `thallite` condition dropped), 35 in all, each reported. Dropping `jade.stand_waits` from the generator's table alone
  is reported only once the generator rewrites the lang.

### C-06. One shared job: keep this crucible's formula supplied (unit 3; C-D4)

**What the player sees.** A crucible running a formula that waits for an ingredient asks the nearest Courier Post its
formula's setter may use for exactly the shortage; a Clockwork Porter fetches it. A Peepo assigned to the crucible fills
the same need without over-supplying. A full crucible keeps the rest aboard the porter until there is room; an unloaded
one waits with the cargo intact; cancelling returns it.

**Changes.**
- **One demand:** the shortage moves into the crucible (from `CrucibleCompanion.shortage()`, `:39-47`, private today),
  and both carriers read it. It is nothing for an incompatible formula (C-01).
- **Filing** (C-P13), after `ConcordSpire.restock` (`concordance/spire/ConcordSpire.java:557-581`), with its
  `nearestPost` (`:583-603`, 9 chunks of block entities a call) extracted as a shared helper, not copied:
  - requester: the formula's setter, recorded when it is set (`CrucibleBlockEntity.java:505-518`); destination: the
    crucible; the exact `ItemVariant`, so a ground reagent stays exact (`CourierLedger.java:170-176`); posts within 16
    blocks (`ConcordSpire.java:106`) the setter may use;
  - at most one open request; filed when the crucible starts waiting for an ingredient, not on every 20-tick step, with
    a backoff after finding no post or after `TOO_MANY`;
  - `TOO_MANY`: a crucible's requests count against its setter's 16 and the world's 1,024
    (`concordance/logistics/Logistics.java:37, 39, 121-127`); the crucible shows a courier-quota reason and backs off, so
    many crucibles cannot spend a player's quota in a loop;
  - **lifecycle:** the request is cancelled (its cargo goes back) when the formula is cleared
    (`CrucibleBlockEntity.java:533-553`), replaced,
    set by a different player or turns incompatible;
  - **removal:** a `preRemoveSideEffects` override calls `CourierLedger.destinationGone` (`CourierLedger.java:273-283`)
    and still drops
    the slots, as the Spire Heart does (`concordance/spire/SpireHeartBlockEntity.java:216-223`), so
    `aBrokenCrucibleDropsItsItems` (`ConcordanceAlchemyGameTests.java:354`) keeps passing.
- **The Peepo side:** `CrucibleCompanion.needed` (`CrucibleCompanion.java:49-57`) subtracts the open request's remainder.
- **Delivery is existing code:** a porter delivers into any container through `ItemStorage.SIDED.find(level, to, null)`
  and reports `FULL` or `DESTINATION_UNLOADED` (`ClockworkPorterEntity.java:387-406`).
- **The failure cases of 03:193:**

| Case | What happens | Shown by |
|---|---|---|
| Destination fills | the rest stays aboard, noted "full"; delivered once there is room. Since the unit review a crucible files only for what fits in it now, and nothing while none would, so no porter waits at a crucible it could never fill; slots a hopper fills after filing still leave the porter on `FULL`, holding the post's queue until there is room or the formula is cleared | new: `aCourierFillsACrucibleUntilItIsFull`; the unit review: `aCrucibleWithNoRoomAsksForNothing` |
| Access removed | filing needs a post the setter may use (`CourierPostBlockEntity.mayUse`); a source its keeper may not open releases the claim, `BLOCKED_BY_ACCESS` (`ClockworkPorterEntity.java:424-427`) | new: `aPostTheSetterMayNotUseIsNeverAsked` |
| Path fails | the porter gives up after a failed walk and releases the claim; the cargo stays with the request (`ClockworkPorterEntity.java:378-385`). Not 03:193's "three times": existing behaviour, unchanged | existing code; re-run `ConcordanceLogisticsGameTests` |
| Target chunk unloads | `DESTINATION_UNLOADED`, no ledger step, the lease renewed (`ClockworkPorterEntity.java:389-392, 335`) | new: `anUnloadedCrucibleWaitsAndLosesNothing` |
| Entity dies | a discarded porter strands its cargo, recovered exactly once | new: `aDiscardedPorterStrandsItsCargoOnce` |
| Owner logs out | a request is served only while its requester is answering: `OWNER_OFFLINE` (`ClockworkPorterEntity.java:324-328`); an unclaimed one then holds the post's queue for every request behind it; a known limit | new: `anOfflineSetterPausesTheCourier` |
| Source broken | the claim is released, `source_gone` (`ClockworkPorterEntity.java:423-427`) | existing code |
| Server restarts | the ledger's codec and the crucible's keys round-trip | new: the codec round trip; `ConcordancePersistenceGameTests` |

**Tests (server):** `ConcordanceLogisticsGameTests` (5 today): the new tests above, plus
`aCrucibleAsksOnceForItsExactShortage`, `aClearedOrReplacedFormulaCancelsItsRequest` (cleared, replaced, another setter,
incompatible), `aFullCourierQuotaWaitsAndBacksOff` and `aBrokenCrucibleTurnsItsRequestBack`. Each audits `problems()`
and checks chest + crucible + carried = N after every step.
`ConcordancePersistenceGameTests.theCrucibleKeepsItsRequestThroughASave`: the keys present; absent (an older save) they
read as none; the stamp stays 1 (`CrucibleBlockEntity.java:671`).

**Tests (client, Peepo):** in `PeepoIntegrationClientTests` and `CompanionAutomationChecks` (existing classes): a
programmed formula's `needed()` is the exact shortage; a full crucible leaves the companion's cargo with it;
`noOverSupplyWithAnOpenCourierRequest`; an unloaded crucible: the job reports `UNLOADED` and keeps its cargo (no test
under `src/gametest` checks `CompanionStatus.UNLOADED` today); an incompatible formula asks for nothing.

**Acceptance:** "one shared worker job … Demonstrate a full destination and an unloaded target without duplication"
(09:136), for both carriers (C-D4); 10:50-51; 03:191's cancel and recover.

**Save effects:** optional crucible keys `formula_owner` (UUID) and `request` (long), read with defaults; the stamp
stays 1.

**Review boundary:** crucible demand and filing, the shared post helper, the port edit and tests. No edit to
`PeepoEntity`, `CompanionHusbandry` or the other files #357 and #369 change.

**As built (unit 3: commit `46f6e2a0e`; records [arcane-concordance-logistics.md](arcane-concordance-logistics.md) and
[companion-farm-stand-crucible.md](companion-farm-stand-crucible.md)).**
- **Built:**
  - the shared helper `Couriers.nearestPost(level, from, user, reach)` and `Couriers.FILING_REACH` (16), extracted from
    `ConcordSpire` (whose copy is gone; `restock` calls the helper), the reach capped at `FILING_REACH`;
  - in `CrucibleBlockEntity`: the record `Shortage`, `shortage()`, `courierRemainder`, `sample`, `backoffAfter`, the
    private `supply`, `reconcile`, `turnBack` and `forgetRequest`; `automate` turns a refused formula's request back,
    reconciles every pulse and asks on an ingredient wait; `follow(Formula, UUID)` beside `follow(Formula)` (no setter;
    tests and operators); the formula item records its user as the setter; `useEmpty` turns the request back before it
    clears; a `preRemoveSideEffects` override calls `CourierLedger.destinationGone` and then the container drop;
    optional keys `formula_owner` and `request`, the stamp still 1; accessors `formulaOwner()` and `request()`;
  - `Diagnostic.courierQuota(count, what, seconds)`, and `courier_quota`'s words with those parameters;
  - the Peepo port (`CrucibleCompanion`): its private shortage deleted; `needed` is the crucible's shortage less
    `courierRemainder`; `COURIER_QUOTA` reads `NO_INPUT`;
  - `tools/concordance_logistics.py`: `FILING_REACH`, `CRUCIBLE_BACKOFF`, `CRUCIBLE_MAX_BACKOFF`; the notes `full` and
    `destination_gone` reworded ("where it was going is full", "... was broken"); the Couriers entry's fourth page, "A
    Crucible's Own Requests";
  - audits: `check_crucible_supply` (new), `FILING_REACH` in `check_logistics`, `Authority.answering` in the crucible's
    `AUTHORITY_ROUTES`.
- **Departures from the ticket and the spec,** each a proposal below or a review correction:
  - the Peepo port subtracts the remainder only while a courier holds the request: its claim's lease live and the
    request not being taken back (C-P50, refining C-P13). Checking the lease, not only that a worker is named, keeps a
    porter unloaded or killed with its claim from stalling the Peepo (the review's correction);
  - every pulse reconciles the request (C-P51). As built it also forgets a request not bound for this crucible or not
    its setter's, and one being taken back; it turns back one with nothing carried that no longer matches what is
    lacked first, asks for more than is lacked, or whose post is gone or barred to its setter (the post's chunk loaded);
  - filing needs the setter answering (`Authority.answering`: here, that is online, alive and in the crucible's
    dimension, or the server's absent-owner option), so a party member's crucibles file no new request for someone
    away, which would stall a shared post's queue, where a porter serves the oldest request first (C-P53; the spec had
    filing without the setter online, and its review changed it). The rule stops new filings only: a request still
    unclaimed when its setter leaves holds the queue (the unit review);
  - `turnBack` cancels as the request's own requester, the only one `Logistics.cancel` accepts;
  - a crucible files the whole shortage, or as much of it as fits in it now, measured as a porter delivers, and nothing
    while none would (the unit review: a request it had no room for left a porter on `FULL` for good); it is at most 32
    (a formula's operations), inside the ledger's 256;
  - the note `full` is reworded as well as `destination_gone` (the review's correction);
  - neither `unloaded_target` nor `unavailable_worker` is raised: the porter's `Status` and the Peepo's
    `CompanionStatus` already say so at their own edges, so their parameters stay unfixed (`Code`'s comments say so);
  - `aCrucibleAsksOnceForItsExactShortage` saves and loads the ledger by its codec mid-delivery (the contract's
    "codec round trip"; the review's correction);
  - the Peepo unload check keeps the companion walking on its own the whole time, fenced in with barriers where its
    chunk is forced, rather than frozen, so its own transport meets the unloaded crucible (the review's correction); and
    the Peepo group is named `noOverSupplyWithAnOpenCourierRequest`, as the ticket's test list names it.
- **Tests (none run):**
  - 9 new `ConcordanceLogisticsGameTests` (14 in all): `aCrucibleAsksOnceForItsExactShortage`,
    `aClearedOrReplacedFormulaCancelsItsRequest`, `aFullCourierQuotaWaitsAndBacksOff`,
    `aBrokenCrucibleTurnsItsRequestBack`, `aCourierFillsACrucibleUntilItIsFull`, `aPostTheSetterMayNotUseIsNeverAsked`,
    `anUnloadedCrucibleWaitsAndLosesNothing`, `aDiscardedPorterStrandsItsCargoOnce`, `anOfflineSetterPausesTheCourier`;
    each audits `problems()` and counts every item, in containers and carried, after every step; every test post has
    an owner;
  - `ConcordancePersistenceGameTests.theCrucibleKeepsItsRequestThroughASave` (9 in all, with C-05's);
  - `ConcordanceAlchemyGameTests.anIncompatibleFormulaAsksForNothing` extended (a refused formula turns its request
    back and files no other; restored, it asks again; broken, the request goes);
  - Peepo, client: `PeepoIntegrationClientTests` groups "crucible demand: the exact shortage, nothing for an
    incompatible formula" and "crucible demand: noOverSupplyWithAnOpenCourierRequest"; `CompanionAutomationChecks`
    groups "a full crucible leaves the companion's cargo with it" and "an unloaded crucible: the job says UNLOADED and
    keeps its cargo";
  - to re-run: `ConcordanceSpireGameTests.theHeartAsksACourierForItsUpkeep` (the extracted helper) and the five
    existing logistics tests.
- **Commands** for the unit, run for its records on the tree with all four commits, and passed:
  `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`. The audit mutations, in place with every file restored after each, against `check_mod_data`'s
  Concordance checks and `owner_art.errors()`: C-06's 15 from the spec and three more (`Authority.answering` dropped,
  the lease check dropped, a turn-back cancelling as the formula's owner), each reported; with C-05's 35, all 53. The
  tree was unchanged afterwards (`git status` clean). `python3 tools/select_client_tests.py --base 23f28b90e` chose
  every client class (171), since `CompanionAutomationChecks` is a shared file. Not run when these notes were written: `./gradlew
  build` (with or without the optional integrations), `./gradlew runClientGameTest` and CI (they have run since, on later heads: see the status at the top).

### Unit 3's review fixes (commit "Chunk C unit 3: fixes from the unit review")

The unit review's confirmed findings, each applied; none of it compiled or run when written (its CI is in the status at
the top: run 38720814268, then runs 38726242149 and 38731825181).
- **No request it has no room for** (C-06, C-P53): `supply` measures how much of the item fits through the lookup a
  porter delivers by (`ItemStorage.SIDED`, no side, `CourierLedger.room`) and files that much, or nothing (no backoff,
  so it asks at its next pulse once there is room). Before, a crucible whose five buffer slots held other things (a
  hopper-fed crucible after a formula change, or a formula of more than five distinct ingredients) filed anyway, and its
  porter waited on `FULL`, holding the post's queue, until the formula was cleared. New test
  `ConcordanceLogisticsGameTests.aCrucibleWithNoRoomAsksForNothing`.
- **A trickle-fed stand reaches its band** (C-05, C-P43 amended): fed less than its rate (JE in within 20 ticks), a tick
  it cannot pay holds the crucible where it is while the crucible is below the band and, following a formula, at the
  band's lowest degree; inside the band such a tick still cools. Short of JE it goes dark only after 10 such ticks in a
  row (no light change for any other reason is delayed). The plan carries the crucible's temperature (still one
  block-entity read); the crucible reads a stand as out when it gave nothing, not by its light. New test
  `ConcordanceAlchemyGameTests.aTrickleFedStandStillReachesItsBand` (42 in all): fed 8 JE a tick, a stand set to hot
  brings its crucible into the band, keeping its light on, and one following a formula takes both hot stirs.
- **A hold over a stand is named as the hold** (C-01): with a stand beneath, `automate` takes the stir's own band as
  where the heat would go unheld, as the stand aims inside it, so a Preserve hold keeping the crucible from a hotter
  stir is `held`, as over a campfire, not `wrong_heat` (both read `WRONG_TEMPERATURE` at the Peepo port).
  `aStandIdlesForAWaitingOrIncompatibleFormula` asserts it.
- **Tests and audits:** `aCrucibleAsksOnceForItsExactShortage` lowers the shortage by hand while a porter carries; the
  request is kept and delivered, one glowstone left over (C-P51). `aFullCourierQuotaWaitsAndBacksOff` breaks its
  crucible at the end, so it leaves no request open in the shared ledger. `check_crucible_supply` holds the room check,
  `reconcile`'s order and `shortage()`'s step order; `check_crucible_stand` the trickle rule and how the crucible reads a
  stand; `check_economy` that `STAND_JE` is `tools/concordance_alchemy.py`'s.
- **Records:** C-P53 and the logistics, alchemy and authority records say the setter rule stops new filings only (an
  unclaimed request of a setter who has left holds the post's queue); "online" reads "here (online, alive and in the
  crucible's dimension)"; the two over-supply paths are named; the changelog's stand textures, the foundation's line and
  this table's unit 2 row are corrected; the unverified container drop is "read, not seen".
- **Departures from the review,** recorded: the edge hold for a stand following a formula is added, since without it a
  stand fed exactly half its rate leaves the crucible alternating between the band's lowest degree and the one below,
  and every 20-tick pulse can land on the lower one, so a stir could wait for good (with no formula the review's rule
  stands, and holding a band costs C-D3's rates); the 10-tick dark delay applies only to ticks short of JE, so a stand
  idling for any other reason still goes dark at once, as C-P43 and the tests say; commit `46f6e2a0e`'s message is not
  rewritten (an earlier commit), and its "never filled twice" is corrected here and in the logistics record.
- **Commands,** run on the tree with the fixes before it was committed, and passed: as C-06's (`select_client_tests.py`
  is run on the commit, after it). Audit mutations, in place with every file restored after each (the tree compared
  before and after), against `check_crucible_stand`, `check_crucible_supply` and `check_economy`: the fixes' 16 (no room
  check, the whole shortage filed, the room measured from the top face or after the post scan, carried cargo turned
  back, the more-than-lacked or item rule dropped, a `HashMap` shortage, `STAND_JE` drifting, a hold inside the band,
  the edge hold without a formula or whatever the feed, dark at once, a hold above the target, the crucible reading the
  light, a second payment) and five of the spec's re-run on the changed code (C-05's `Block.UPDATE_ALL`, a loop in
  `plan`, the temperature comparison removed, `below()` without the stand; C-06's `request > 0L` dropped): all 21
  reported.

### C-07. Habitat made visible (unit 4)

**What the player sees.**
- The Inspect page on a Verdant Bed or a crop shows each factor's value against the organism's ranges (least, ideal from,
  ideal to, most: for example Sunpetal's light `[11,13,15,15]`), the verdict, and reasons that name the reach: "within 2
  blocks, at plant height" for diversity, "5×5, a block above and below" for disturbance (`Sampler.java:16-22`). Light is
  shown as sky and lamps (C-P14; niches unchanged).
- A bed's verdict clears once no crop stands on it; Jade on a ripe crop shows its last verdict.
- A canteen waters a Verdant Bed, as the docs and codex say (C-P15); JEI has the compost page.

**Changes.**
- `InspectPage` gains a bed and crop branch, from the server (the rules are server-only, `ConcordanceData.java:28`).
- A bed's random tick (`concordance/garden/VerdantBedBlock.java:70`) clears a stale note when no organism crop stands
  above it.
- Jade: a crop has no block entity, so no server data of its own (`JugcraftJadePlugin.java:30-45`); its lines read the
  bed's update tag (`JugcraftJadeClient.java:238-258`; `VerdantBedBlockEntity.java:242-251`). This ticket only stops
  hiding the verdict when ripe (`JugcraftJadeClient.java:254`); C-08 and C-09 grow the tag.
- The canteen (`GourdCanteenItem.java:89-90, 116`) waters beds; `compost_view()` (`tools/concordance_ecology.py:554-557`)
  goes into `recipe_view.json` with a JEI reader.
- The ecology record's counts and scan figures corrected (above).

**Reuses unchanged:** `Verdict`, `Niche`, `Sampler`, the bed's cached sample, the encyclopedia payloads.

**Tests (server):** `ConcordanceGardenGameTests` (12 today): `aBedSaysWhatItsPlantNeedsAndHowFar`;
`aVerdictClearsWhenThePlantGoes`; `aCanteenWatersABed`. `EncyclopediaGameTests.theInspectPageReadsABedAndACrop`.

**Tests (client):** a bed's Inspect page; a Jade shot of a crop.

**Acceptance:** "Make habitat requirements visible" (09:128); 03:79.

**Save effects:** none. **Review boundary:** read-only garden diagnostics and the two routine fixes.

**As built (unit 4: commit `5ba973632`).**
- **Built:**
  - the pure `ecology/HabitatInspection` (`State`, `unjudged`, `judged`, `verdict`, `pace`, `unplanted`): each factor's
    line names how far its reading reaches (moisture the bed's own, with water's reach of 4, `Garden.WATER_REACH`; light
    as sky and lamps, the greater counting; the bed's nutrients; diversity within `Sampler.RADIUS` 2 at the plant's
    height; disturbance in the 5 by 5, `Sampler.HEIGHT` 1 above and below), judged by the verdict's own fit against all
    four of the range's bounds, or "any will do" for a factor the niche leaves open;
  - `VerdantBedBlockEntity.inspect` (the habitat growth would read, from the cached sample, and the light's two layers;
    it only reads) and `forgetVerdict` (the verdict only, synced and saved), which the bed's random tick calls once
    neither a crop nor a Verdant Heart stands on it (defect 13);
  - the Inspect page's garden branch (`InspectPage.GARDEN_REACH`, 1.0 beyond the reader's block reach, as the
    crucible's): a bed, or a crop through the bed below it; a dormant bed is not read, a bare one is unjudged, a crop
    with no bed says so; codex links to `garden/habitats` (encountered) and `garden/organisms` (observed);
  - Jade: a ripe crop shows its bed's last verdict, "As it grew: ..." (defect 13);
  - `VerdantBedBlock.useWithoutItem` passes for any item in the main hand (defect 14), and `VerdantBedBlock.pour`, which
    the Gourd Canteen's new `BED` case calls: a bed below 7, `RateGate` ("garden", 4 ticks), then `Authority.mayChange`
    (a refusal shows `missing_permission`); a sip is spent only when it wets (defect 11);
  - JEI: `JugcraftJeiPlugin.addCompostInfo` reads `recipe_view.json` `garden_compost` (`tools/recipe_view.py`, from
    `compost_view()`): an information page for each item of the Maw's tags, at its highest tag's worth (defect 12);
  - lang: `compose.jugcraft.ecology.inspect.*`, `encyclopedia.jugcraft.inspect.garden.habitat` and `.here`,
    `tooltip.jugcraft.concordance.jade.as_it_grew`; the codex's habitats page names the Inspect page;
  - audits: `check_ecology`'s C-07 block, `check_encyclopedia`'s garden reach block and read-only list (`.note(`,
    `forgetVerdict(`, `forgetSample(`, `.take(`, `.give(`, `.grow(`, `.hasten(`), the client-read rule's needles
    (`Garden.catalog()`, `Garden.organism(`, `.organism()`, `.niche()`) and `AUTHORITY_ROUTES` for `VerdantBedBlock`.
- **Departures from the ticket and the spec:**
  - the held-item rule (the spec's R2) is C-07's, not C-08's, so the canteen needs no pass of its own in `useItemOn`;
  - from the spec's critique: the short and excess judgements name all four bounds (its correction 8);
    `aBedSaysWhatItsPlantNeedsAndHowFar`, `aVerdictClearsWhenThePlantGoes` and the Inspect test prefetch their samples
    at ticks 10, 20 and 30 (4; `aCanteenWatersABed` takes no sample); the client section holds random ticks at 0 while
    it runs (5);
  - built on `23f28b90e` beside unit 3, not on unit 3's head as the spec planned: `AUTHORITY_ROUTES`' bed line follows
    the Gleaner's, with no crucible line of unit 3's here; the decisions are C-P54 to C-P57 (the spec's placeholders
    C-P80 to C-P83), after unit 3's C-P43 to C-P53.
- **Tests (none run):** `aBedSaysWhatItsPlantNeedsAndHowFar`, `aVerdictClearsWhenThePlantGoes` and `aCanteenWatersABed`
  (15 garden tests then); `EncyclopediaGameTests.theInspectPageReadsABedAndACrop` (22); the client section
  `EncyclopediaClientGameTests.gardenInspection`, with shots `jugcraft_concordance_ripe_crop_jade` (when Jade is loaded)
  and `jugcraft_encyclopedia_inspect_bed`.
- **Commands** for commit `5ba973632`:
  - run and passed: `python3 -B tools/generate_material_data.py` twice (the second run changing nothing),
    `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`, `python3 tools/owner_art.py --check`,
    `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
    `node --test tools/world-designer/model.test.cjs` and `python3 -m unittest discover -s scripts/tests`;
    `git diff --check` on the commit's diff, run for these records: clean;
  - the spec's audit mutations: the commit's run left no count on record, so they were run again for these records, in
    place on the final tree (`d6edee4f4`), each restored and the tree unchanged afterwards: all 23 reported (its 18,
    five of them in two variants). Mutation 8, the pour without Authority, is reported by the pour's own audit;
    `check_authority`, which the spec also expected to report it, does not, because the bed block's tending keeps the
    same call;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

### C-08. One mixed-bed rule, and harvest strain with recovery (unit 4)

**What the player sees.**
- **A mixed bed** (C-P16): Dewmoss and Gloamcap, both shade plants (`concordance/organism/dewmoss.json` light
  `[0,0,9,12]`; `gloamcap.json` `[0,0,6,9]`), grow faster when the other kind grows within the 5×5 at plant height.
  Identical copies never count. The Inspect page and Jade say "mixed bed: +25%" (a starting value).
- **Harvest strain** (C-P17): a bed that was just harvested is strained, up to 3, and the strain counts against its
  plant's disturbance limit. It only ever worsens the fit: it never supplies disturbance a plant needs, so a harvested
  Gloamcap bed (`[1,3,10,15]`; 0 stalls it, `concordance/ecology/Niche.java:40-48`) still needs its magic blocks. Rest,
  compost and a Greenwarden's tending ease it. It never kills. Inspection and Jade say "recently harvested".
- **What strain costs:** Sunpetal (`[0,0,2,5]`) stays ideal at strain 1 or 2 on an undisturbed bed and halves its pace
  (tolerating) at 3, or sooner where the area already disturbs it; Mendvetch (`[0,0,3,6]`) stays ideal up to 3 on an
  undisturbed bed; Dewmoss declares no disturbance range and is untouched.

**Changes.**
- **Mixed bed:** an optional `neighbours` field on the organism, wired through the parser's allow-list, the `Organism`
  record, the generator's `ORGANISMS` and `organism_json`, and `check_ecology` (`check_mod_data.py:14104`).
  `Sampler.Area` (`Sampler.java:36-39`) carries the kinds set it already builds (`:42-50`, at most 25 entries): no new
  reads. The bonus multiplies the speed at `OrganismCropBlock.java:165` and leaves the per-step cost (`:170`) alone, so it
  cannot mint nutrients. No Mendvetch pairing (already a legume) and no fixer as beneficiary. It is called "mixed bed":
  "companion" means Peepo here.
- **Strain:**
  - the bed notes `ripened` when growth reaches the last age (`OrganismCropBlock.java:173`); the next growth attempt
    below ripe counts as a harvest: strain + 1, the note cleared. Every harvest path (hand, Gleaner, Peepo, Crop
    Harvester, sickle, reap, spell, breaking and replanting) resets or removes the crop and regrows through `grow`
    *(survey: the garden report's harvest-path table)*, so no block hook is needed. A crop placed ripe by a command
    never noted `ripened`, so its first harvest strains nothing;
  - `Habitat` carries the strain beside the five factors, not as a sixth (the Gauge's modes, Jade's five-number array,
    `JugcraftJadeClient.java:374-378`, and the checker's factor order stay); `Verdict.of` judges disturbance as the worse
    of the area alone and the area plus strain;
  - eased by rest (one a bed random tick), by **explicit compost points only** (bone meal on the bed,
    `VerdantBedBlock.java:129`; bone meal or fertilizer on the crop, `OrganismCropBlock.java:271-276`; the Mulch Maw's
    mulch, `concordance/garden/MulchMawBlockEntity.java:95-96`) and by a Greenwarden's empty-hand tending
    (`VerdantBedBlock.java:140-154`, at most once a 600 ticks a bed, `Authority`-checked; as built, on the bed or, since
    the unit review, on its growing crop). Not in `give`: nitrogen fixing
    calls `give` through `Garden.nourish` on every Mendvetch step for its bed and four neighbours
    (`Garden.java:297-312, 340-352`; `OrganismCropBlock.java:175-177`), so easing there would keep those beds unstrained;
  - it never reads industrial pollution or main's process emissions.
- **The bed's update tag** (`VerdantBedBlockEntity.java:242-251`) gains the strain and the mixed-bed state, and its
  reasons new kinds: a network-contract change. `reasonText` (`JugcraftJadeClient.java:213-221`) treats field 1 as a
  factor id, so its decoder changes too.

**Tests (server):** `ConcordanceGardenGameTests`: `aMixedBedGrowsFasterOnlyForAnotherKind`;
`theBonusSpendsNoExtraNutrientAndMintsNone`; `everyHarvestPathStrainsTheBedOnce` (hand, Gleaner, Crop Harvester, sickle,
breaking and replanting); `fixingDoesNotEaseStrain`; `restCompostAndTendingEaseStrain`;
`strainNeverSuppliesDisturbance` (Gloamcap still stalls with no magic blocks); `strainNeverKillsAndNeverReadsPollution`;
`strainSurvivesASave`; `samplingStaysWithinTheBudget` (`:445`) extended: still 100 reads. Re-run, since they harvest and
regrow: `theGleanerHarvestsForVerdance` (`:364`), `ConcordanceSpireGameTests.aVerdantFieldHastensOnlyWhatItsBedsPay`
(`:406`), the Peepo regrowth case (`PeepoIntegrationClientTests.java:169-171`, which places its crop ripe) and
`ConcordanceJourneyGameTests`.

**Acceptance:** "one companion-planting rule … Bound neighborhood work and include recovery. Do not attach crop death to
industrial pollution" (09:128); D-005 (10:158).

**Save effects:** optional bed keys `strain` and `ripened`, absent in older saves and read as 0 and false; the stamp
stays 1 (`VerdantBedBlockEntity.java:227`). A persistence test reads an older bed.

**Review boundary:** ecology rules, the bed and the generator's data, the bed's update tag and Jade decoder, and tests.

**As built (unit 4: commit `d32ac3a61`).**
- **Built:**
  - pure ecology: `Neighbours` (`organisms`, `percent`; `NONE`, `MAX_PERCENT` 50, `any`, `line`), `Organism.neighbours`
    and the parser's `neighbours` (other organisms, never itself or twice, 1 to 50 percent, never on a fixer);
    `Sampler.Area.kinds` (the kinds it already read: still 100 reads) and `Sampler.habitat(..., strain)`;
    `Habitat.strain`, `MAX_STRAIN` 3 and `strained()`, the strain a sixth value of the record but not a sixth factor
    (the Gauge's modes, Jade's five-number habitat and the checker's factor order stay); `Verdict.strained`,
    `Verdict.of`'s worse of the area alone and the area with the strain, and the `ecology.strained.excess` and
    `.tolerable` reasons; `HabitatInspection`'s `disturbance_strained` row; `EcologyCatalog.mixed`;
  - the rules report a neighbour that is not another crop costing nutrients (`ConcordanceRules`), and such a name speeds
    nothing;
  - the bed: `strain`, `ripened`, `strainNow` (it only reads), `ripen`, `settle`, `rest`, `compost`, `tend`, `setStrain`
    (tests only), `mixed`, `mixedLine`, `strainLine` and `notes`; the keys `STRAIN` and `RIPENED`, saved when
    set and always sent, never with the keeper; its reports add the mixed-bed and strain lines;
  - growth: `OrganismCropBlock.grow` counts a harvest the bed saw before the dormant check and tells the bed when a step
    reaches ripe; `speed` holds the mixed bed's bonus and the cost is untouched; `useItemOn` makes fertilizer on a
    growing crop its own use, and bone meal on one feed the bed below by the bed's own rule (`RateGate`, then
    `Illumination.mayChange` on the bed, a refusal consuming the use) and compost it (defect 15); `performBonemeal`
    composts only when it feeds;
  - `Garden.poorest` and `TEND_TICKS` (600); the Mulch Maw composts the bed each nutrient goes to; `Garden.nourish` and
    `give` never compost;
  - the bed block: its random tick rests a strain (`rest`), bone meal on it composts, and a Greenwarden's empty hand
    tends (`tend`: an awake, strained bed, `Garden.knows`, `Authority.mayChange`; once every 600 ticks a bed, not
    saved);
  - Jade: the bed snapshot's `strain` (read, never counted); the client's `reasonText` shows a reason with no factor as
    sent, and a strain line on a bed and on its crop;
  - the Inspect page's mixed-bed line (the strain is in the disturbance row);
  - the generator: `MIXED_BED_PERCENT` 25, `MAX_MIXED_BED_PERCENT` 50, `MAX_STRAIN` 3, `TEND_TICKS` 600, `neighbours` on
    the Dewmoss and Gloamcap organisms, the codex's "Mixed Beds and Strain" page (organisms p6), and the messages
    `garden.tended` and `garden.tended_lately`;
  - audits in `check_ecology`: the generator's neighbours and their rules; the bonus in the speed only, and that `grow`
    uses `speed()`; only rest, compost and tending ease strain, compost only at its listed points; the tending gate; the
    crop's bone meal and fertilizer rule; the saved and sent keys; Jade's five-factor habitat; the read-only page and
    `strainNow`; that the garden never reads industrial pollution or process emissions (D-005); the encyclopedia's
    read-only list gains `.settle(`, `.compost(`, `.tend(`, `setStrain(` and `.ripen(`.
- **Departures from the ticket and the spec:**
  - a bed rests only while no crop grows on it (the spec's D-a; C-P60, refining C-P17's "one a bed random tick"), by
    arithmetic and not measured: see the ecology record's Balance;
  - bone meal or fertilizer used plainly on a growing crop is a compost point (the spec's R10; defect 15); from the
    critique (3), bone meal there asks the bed's rate gate and who may change the bed before it feeds, a refusal
    consuming the use, and fertilizer keeps its own area rule;
  - a harvest is counted whatever took the crop: a growth attempt, the bed's random tick, compost or tending counts one
    when the crop the bed saw ripen no longer stands ripe, so a Hay Golem, a crow raid and breaking a ripe crop to move
    it count too, and a crop placed or aged ripe without growth never does;
  - the Crop Harvester has its own test, and the parts' two older-bed persistence tests are one,
    `anOlderBedLoadsWithoutItsNewKeys` (R14); this tree's persistence class has 8 tests, not the spec's 10, as unit 3's
    two are not here;
  - from the critique: the nine garden tests pasted from the part designs and the critique (1), with the sickle used
    sneaking (a plain use is the hand's harvest) and the `strained` helper ending on a crop still growing, so no later
    path can take it; an audit that `grow` uses `speed()` (2, mutation 23);
  - the decisions are C-P58 to C-P61 (the spec's C-P84 to C-P87).
- **Tests (none run):** 9 new garden tests (24 then): `aMixedBedGrowsFasterOnlyForAnotherKind`,
  `theBonusSpendsNoExtraNutrientAndMintsNone`, `everyHarvestPathStrainsTheBedOnce` (a hand, a Gleaner, a sneaking
  sickle, a reap, breaking and planting again), `theCropHarvesterStrainsTheBedOnce`, `fixingDoesNotEaseStrain`,
  `restCompostAndTendingEaseStrain`, `strainNeverSuppliesDisturbance`, `strainNeverKillsAndNeverReadsPollution` and
  `strainSurvivesASave`; `samplingStaysWithinTheBudget` extended (still 100 reads, and its 25 kinds);
  `ConcordancePersistenceGameTests.anOlderBedLoadsWithoutItsNewKeys`; `gardenInspection`'s strain step (the client is
  told strain 2; Jade and the page show it).
- **Commands** for commit `d32ac3a61`:
  - run and passed: the generator twice (the second run changing nothing), `check_mod_data`, `check_repository`,
    `owner_art --check`, `concordance_journey --check`, `check_icon_maps`, the World Designer's node tests,
    `scripts/tests` and `git diff --check`;
  - the spec's audit mutations with the critique's, each applied in place and restored: 28 of 28 reported (23 rows, four
    of them in variants), at the commit and again on the final tree for these records;
  - the pure ecology classes compiled with `javac` (JDK 21) against stubs, and a scratch harness outside Git passed 41
    checks of the mixed bed, the parser's refusals and strain; for these records it was run again on the final tree, its
    catalogue built through C-09's three-argument constructor;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

### C-09. One stable graft trait (unit 4)

**What the player sees.** A gardener with Verdant Husbandry understood uses a Dewmoss, not sneaking, on a Sunpetal crop.
The Dewmoss is consumed and that bed's Sunpetal becomes **Dewlit** (C-P18): it tolerates shade (light `[9,11,15,15]`
for `[11,13,15,15]`) but needs wetter soil (moisture `[4,5,7,7]` for `[2,4,7,7]`). The graft stays on the bed through
every harvest path, replanting, saves and restarts; another organism growing there clears it. Produce, cost and alchemy
are unchanged. Inspection and Jade name it.

**Changes.**
- **The gesture reaches the crop:** an `OrganismCropBlock.useItemOn` override that, for a donor this host accepts,
  grafts and consumes the action before the empty-hand fallback, and otherwise returns as today so a ripe crop still
  harvests. A sneaking use with an item in hand never reaches the block (mc263src `MultiPlayerGameMode.java:335-337`);
  without the override a plain use falls through to `useWithoutItem` (`:343-348`), which harvests a ripe crop
  (`OrganismCropBlock.java:204-228`). Gated by research, `Authority` (as the harvest, `:213`) and `RateGate`.
- One trait in `concordance/graft/*.json` (host, donor, niche overrides), generated; `check_ecology` holds the pilot's
  cap of one (03:81) and that it changes no produce or cost.
- **Bed-bound, not an item component** (C-P18, a deviation from 03:76 and 03:81): Peepo plants with `setBlock`
  (`local/peepo/CompanionGarden.java:612`), so no item component reaches the crop; `OrganismCropBlock` has no
  `setPlacedBy`; loot-table harvests would drop unstamped produce *(survey)*.
  Cuttings are deferred.
- **Every niche reader** asks one helper for the bed's niche: growth (`OrganismCropBlock.java:160`), the empty-hand
  report (`:247`, `:254`), the bed's note that Jade shows (`VerdantBedBlockEntity.java:171`) and the Habitat Gauge
  (`concordance/garden/HabitatGaugeBlockEntity.java:110`). The Verdant Heart reads its own niche
  (`VerdantHeartBlockEntity.java:104`) and is untouched.
- The bed's update tag gains the graft (the network-contract change of C-08).

**Tests (server):** `ConcordanceGardenGameTests`: `aGraftTakesOnceAndConsumesItsDonor`;
`aSneakingUseNeitherGraftsNorPlaces`; `aDonorOnARipeCropGraftsAndAnyOtherItemStillHarvests`;
`anUnresearchedGardenerCannotGraft`; `aGraftIsRefusedWhereTheGardenerMayNotBuild`;
`aGraftSurvivesEveryHarvestPathAndASave`; `everyNicheReaderSeesTheGraft` (growth, the report, the note and the Gauge
agree); `aGraftChangesOnlyTheNiche`; `anotherPlantClearsTheGraft`.

**Acceptance:** "one stable graft trait" (09:128); "Breeding cannot duplicate rare unique outputs or become a Prima
Materia loop" (03:81): the produce is unchanged and uncatalogued.

**Save effects:** an optional bed key `graft`. **Review boundary:** the graft data, the bed field, one gesture, the
niche helper and tests.

**As built (unit 4: commit `d6edee4f4`).**
- **Built:**
  - pure ecology: `Graft` (`id`, `host`, `donor`, `overrides`; `apply`, `lines`), `EcologyParser.graft` (`schema`,
    `host`, `donor` and `niche` and nothing else; a niche that changes nothing is refused) and `EcologyCatalog.grafts`,
    `graft` (by id, and by host and item), `niche(organism, graft)` and `graftLines`;
  - the rules read `concordance/graft` and refuse a graft that does not join two different loaded crops, puts its host's
    nutrient floor below its cost, or repeats a host and donor (`ConcordanceRules`); `ComposeText` names a `graft`;
  - data `concordance/graft/dewlit.json`, from `GRAFT_CAP` (1) and `GRAFTS` through `graft_json` in
    `tools/concordance_ecology.py`; the codex's "Grafting" page (organisms p7, once Verdant Husbandry is understood);
  - the gesture: `OrganismCropBlock.useItemOn`, for a Greenwarden with the Concordance on, finds the graft for the crop
    and the held item and calls `graftBed`: `RateGate`, a bed below, `Authority.mayChange` at the crop and at the bed (a
    refusal shows `missing_permission`), and no graft the rules know already on the bed; it sets the graft and spends
    one donor, and every outcome consumes the use;
  - the bed: `graft`, `graftFor` (a client's), `setGraft`, `clearGraftUnlessFor` (growth's, before the dormant check)
    and `niche`; the keys `GRAFT` (saved when set) and `GRAFT_HOST` (in the update tag only, while grafted);
  - every niche reader asks `VerdantBedBlockEntity.niche`: `OrganismCropBlock.grow` and `report`,
    `VerdantBedBlockEntity.note`, `HabitatGaugeBlockEntity.read` and `InspectPage.garden`; the Verdant Heart reads its
    own;
  - shown: the reports (a bed's, and its host crop's), the Inspect page (`grafted`: on a bed's page, and on a crop's
    when the crop is the graft's host) and Jade (the bed snapshot's `graft` when the rules know it; the client's line on
    the bed, and on a crop when the update tag's host is that crop);
  - lang: `graft.jugcraft.dewlit`, `compose.jugcraft.ecology.graft` and `.graft.range`,
    `message.jugcraft.concordance.garden.grafted` and `.graft_held`, `tooltip.jugcraft.concordance.jade.graft`;
  - audits: `check_graft` (one graft; one range better and one worse; nothing but a host, a donor and ranges; the data
    the generator's; the loader; the one niche helper and no other reader; the gesture's research, rate limit, Authority
    at crop and bed and consumed refusals; only the gesture grafts and only growth clears; the saved and sent keys;
    Jade; the Inspect page; the lang), `AUTHORITY_ROUTES` for `OrganismCropBlock` (crop and bed) and the encyclopedia's
    read-only list (`setGraft(`, `clearGraftUnlessFor(`).
- **Departures from the ticket and the spec:**
  - from the critique: a graft the rules no longer know no longer blocks its bed: the next gesture replaces it (its
    correction 6; C-P64); the Inspect page names a graft on a crop's page only when the crop is its host, as Jade does
    (7; C-P65); the codex says "on its host's crop, ripe or growing" (8);
  - the codex page is appended as organisms p7 (the spec's R11), so no page key is renumbered;
  - the decisions are C-P62 to C-P65 (the spec's C-P88 to C-P91).
- **Tests (none run):** 9 new garden tests (33 in all): `aGraftTakesOnceAndConsumesItsDonor`,
  `aSneakingUseNeitherGraftsNorPlaces`, `aDonorOnARipeCropGraftsAndAnyOtherItemStillHarvests`,
  `anUnresearchedGardenerCannotGraft`, `aGraftIsRefusedWhereTheGardenerMayNotBuild`,
  `aGraftSurvivesEveryHarvestPathAndASave`, `everyNicheReaderSeesTheGraft`, `aGraftChangesOnlyTheNiche` and
  `anotherPlantClearsTheGraft`; `EncyclopediaGameTests.theInspectPageNamesAGraft` (23);
  `anOlderBedLoadsWithoutItsNewKeys` extended (a bed from before grafts reads ungrafted and saves none);
  `gardenInspection`'s graft step (the client is told the graft and its host; Jade and the page name it).
- **Commands** for commit `d6edee4f4`:
  - run and passed: the generator twice (the second run changing nothing), `check_mod_data`, `check_repository`,
    `owner_art --check`, `concordance_journey --check`, `check_icon_maps`, the World Designer's node tests,
    `scripts/tests` and `git diff --check`;
  - the spec's audit mutations with the critique's, each applied in place and restored: 32 of 32 reported (30 rows, two
    of them in two variants), at the commit and again for these records, through a scratch harness of five audits
    (`check_ecology`, `check_graft`, `check_encyclopedia`, `check_authority`, `check_diagnostics`) with the generated
    files written by hand, not through the generator or the whole `check_mod_data`. Mutation 5 (Dewlit's nutrients
    `[0, 0, 32, 32]`) in fact crashed the generator and `check_mod_data` (the codex's `graft_text` joined a full range's
    `None`) until the unit review fixed `graft_text`;
  - a scratch harness outside Git passed 28 checks of the graft (the parser and its refusals, the catalogue's niche and
    lines, a graft whose crops are not loaded) on the compiled pure classes, at the commit and again for these records;
  - for these records, `python3 tools/select_client_tests.py --base 23f28b90e --head d6edee4f4` chose 12 of 171 client
    classes: `ConcordanceEmberGearClientGameTests`, `ConcordancePresentationClientGameTests`,
    `EncyclopediaClientGameTests`, `FluoriteClientGameTests`, `PeepoActionClientTests`, `PeepoCompanionClientTests`,
    `PeepoGardenExpansionClientTests`, `PeepoHarvestClientTests`, `PeepoIntegrationClientTests`,
    `PeepoSuppliesClientTests`, `SpodumeneClientGameTests` and `WorldDesignerClientGameTests`, noting that no client
    test shows the Jade client, the JEI plugin, `ComposeText`, `EcologyParser`, `Neighbours` or `recipe_view.json`;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

**Fixes from the unit review (unit 4, a fifth commit).** The review confirmed ten findings and rejected sixteen; all ten
are fixed here. None of the changed Java had been compiled against Minecraft or run in a game when this was written;
its first CI run, run 38726242149 on the merge of units 3 and 4, passed every job.
- **A graft with a full range crashed the generator** (C-09). `graft_text` joined `_range`'s `None` for a range such as
  `[0, 0, 32, 32]`, so mutation 5 crashed `tools/generate_material_data.py` and `check_mod_data` (the codex is built
  before `check_graft`). It now writes "nutrients any"; Dewlit's page is unchanged. The mutation record above says how
  the mutations were really run.
- **Jade could show another plant's verdict** (C-07, C-P55). A crop newly set on a bed (`OrganismCropBlock.onPlace`,
  server only, not for a step or a harvest of the same crop) and a graft (`graftBed`) now forget the bed's last
  verdict at once; before, a Dewmoss planted where a Sunpetal stalled showed the Sunpetal's reasons until its own first
  growth attempt. `aVerdictClearsWhenThePlantGoes` and `aGraftTakesOnceAndConsumesItsDonor` assert it, and
  `check_ecology` audits both calls.
- **Tending could not be reached on a planted bed** (C-08, C-P61). A crop's shape covers its bed from above, so in a
  field only edge beds could be tended. A Greenwarden's empty hand on a growing crop now tends the bed beneath through
  the bed's own rule (`VerdantBedBlock.tendBy`, which asks `Authority` at the bed), after the crop's rate gate, then
  reports. `restCompostAndTendingEaseStrain` tends through a crop and is refused through one on a claimed bed;
  `check_ecology` holds the callers of `tendBy` to the bed and the crop. Not done (optional): a crop read again during
  the cooldown still says "tended lately".
- **A rare flake** (about 0.1% of runs): `restCompostAndTendingEaseStrain` now resets the compost bed's nutrients, which
  a random step before tick 46 could lower, before its exact asserts.
- **Records.** Bees reach these crops (confirmed by reading, under Risks; a gap since step 14, recorded in the ecology
  record). The Jade staleness risk is "until the crop's next growth attempt". The ecology record's test count (step 14
  added ten; "the ten garden tests"). The persistence record names the bed's new keys. The merge note (Where to resume)
  names the conflicting files from a trial merge. `setStrain` is "tests only" (no game code calls it; also `setGraft`'s
  Javadoc, which said "operators"). The departure note names the two garden tests that prefetch.
- **Commands** for the fixes:
  - run and passed: `python3 -B tools/generate_material_data.py` (changing nothing: Dewlit's page reads as before),
    `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`, `python3 tools/owner_art.py --check`,
    `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
    `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
    `git diff --check`;
  - mutation 5 again, in place and restored, through the generator and the whole `check_mod_data`: the generator ran
    (rewriting `dewlit.json` and the lang) and `check_mod_data` reported "below its host's cost" and "one range for the
    better and one for the worse"; the old `graft_text` raises `TypeError` on the same data;
  - the fixes' audit mutations, each applied in place and restored, through the five-audit harness: 11 of 11 reported
    (`onPlace` forgetting nothing, forgetting on a step of the same crop, or missing; the graft not forgetting; the
    crop's tending removed, with an item in hand, at the crop's own position or without the crop's rate gate; a third
    caller of `tendBy`; `tendBy` without `Authority`; the crop calling the bed entity's `tend` itself);
  - `python3 tools/select_client_tests.py` on the fixes as staged: against `549a49d7e` it chose
    `EncyclopediaClientGameTests`, `PeepoActionClientTests`, `PeepoGardenExpansionClientTests`,
    `PeepoHarvestClientTests` and `PeepoIntegrationClientTests` (noting that no client test shows
    `VerdantBedBlockEntity`); against `23f28b90e` the same 12 classes as for the records above;
  - not run when these notes were written: the build, the game tests and CI (they have run since, on later heads: see the status at the top).

### C-10. The measured workshop, the journey, the evidence and the checkpoint (unit 5)

- **The load card** (06:182; 10:79-85; `docs/TESTING.md:41`):
  - **workload:** a stand and crucible replaying the Stillroot Poultice, a pump, one circle running Root and Rivet, two
    workers (a Peepo and a porter), nine beds with a Maw and a Gleaner;
  - **recorded:** hardware (the runner's CPU from its log), Java options, baseline world, warm-up, measured duration
    (at least 6,000 ticks), player count, loaded chunks, machine, worker and plant counts, active recipes;
  - **measured:** median and p95 server tick time over 100-tick windows (`MinecraftServer.getTickTimesNanos()`, a
    100-entry ring, mc263src `MinecraftServer.java:286, 1696-1697`), with and without the workshop in the same world and
    build; the memory trend; **inventory correctness** (every item in, out, carried and stored accounted for, the ledger's
    `problems()` empty); **network traffic** at the existing send points (block-entity updates, Jade, encyclopedia and
    sign payloads), counted if a cheap counter proves feasible, otherwise stated as not measured;
  - **client frame time:** CI's client shards render in software (`.github/workflows/build.yml:109`), so frame times
    there describe that renderer only; a player's hardware stays not run unless someone runs it;
  - **runner** (C-P20): a server game test of its own, opted in so ordinary runs are not lengthened (the game test
    server runs its ticks back to back, mc263src `GameTestServer.waitUntilNextTick`, so the card takes its ticks × their
    mean time: measured, "1 GAME TESTS COMPLETE IN 10.16 s" in run 38774859294); if a client class is used instead, it
    gets a `WEIGHTS` entry
    (`tools/select_client_tests.py:53`) to stay under the 60-minute job limit (`build.yml:102`);
  - **budget** (C-P19), agreed before merge.
- **The journey:** `tools/concordance_journey.py` and `ConcordanceJourneyGameTests` gain the workshop route from a manual
  start: crafted Mendvetch, a campfire crucible, lantern-filled pylons. Every new item passes `check_acquisition`.
- **Client evidence** (08:169-181): the crucible's, a bed's and the anchor's Inspect pages; the workshop at the ordinary
  scale; one high GUI scale shot; Jade and GuiLib absent where relevant. Each new client class is registered in
  `tools/select_client_tests.py` and the gametest `fabric.mod.json`.
- **Records:** each unit's feature record (tier, input producer, output consumer, costs, unlocks, failure behaviour and
  test evidence, CLAUDE.md), `docs/WHAT_EXISTS.md`, `docs/INTEGRATION_STATUS.md` and `docs/ARCANE_CONCORDANCE.md`
  (MP:71), and an "Unmerged: Chunk C" section in `CHANGELOG.md`.
- **The checkpoint** (09:144; 10:87-121): one complete input-to-output line, the garden and recipe costs; at most three
  decisions (09:18; 10:113-116). 09:144 names three (the balance of hands-on and automated work, the next product
  family, whether the visual workshop fits the desired style). The review route (above) goes with it as a question, and
  the load budget is put at merge review (`docs/TESTING.md:41`), not as a fourth decision.
- **The handoff** (10:133-146) on this page.

**As built (unit 5):**
- **The load card** (C-P66, C-P67): `ConcordanceWorkshopLoadGameTests.loadCard` (`a86085b3c`; compile fix
  `bb856f873`). It measures only when Fabric's game test filter names exactly its id,
  `jugcraft-test:concordance_workshop_load_game_tests_load_card`; in every other run, every ordinary build included, it
  passes on its first tick and places nothing. Locally, the command in `docs/TESTING.md` (Java 25). In CI, a manual
  Build run with `load_card` ticked runs it alone in the "workshop load card" job and repeats its lines as one notice
  (`bfb08770b`, a workflow change for the owner's review).
  - The workshop it measures: a Crucible Stand under an Alembic Crucible replaying the Stillroot Poultice, with water
    from an Electric Pump through a bronze pipe; a Lesser Circle running Root and Rivet again and again, its pylons fed
    by cable; a Peepo and a Clockwork Porter bringing Mendvetch; nine planted Verdant Beds, a Mulch Maw and a Gleaner.
    A tenth bed, under the Verdant Heart the Gleaner draws its Verdance from, is an addition the workload needs. Power
    comes from four Creative Energy Cells: test scaffolding, not a player's generators. The crucible's companion filter
    is set to the salve, so its bowls count as its containers.
  - It holds noon and the weather, turns mob and wandering-trader spawning off, force-loads its yard's chunks (the game
    test server sets no simulation distance, so only forced chunks tick) and measures the same world twice, baseline
    first: each span a warm-up (at least 2,000 and 1,500 ticks, then until the loaded chunk count holds 400 ticks),
    then 6,000 ticks from the server's 100-tick ring, net of the test's own work (the simulated player's actions and
    output extraction included). Before its assertions it logs the runner (every JVM argument included), the world,
    the counts, memory, every item in, out, carried and stored, and what one test player is sent.
  - Measured once, in run 38774859294 on `bb856f873`; the block is quoted under [Unit 5](#the-load-card-run-38774859294).
    On that runner (AMD EPYC 7763, 2 physical and 4 logical CPUs, Java 25.0.4.1) the workshop added +0.240 ms to the
    median tick and +0.550 ms to the p95, net, and retained heap rose +0 MiB beyond the baseline's change: within
    C-P67's proposed budget, which the owner decides at merge review. While measured: poultices 21, Root and Rivet 37
    (interrupted 0), the porter 44 Mendvetch in 22 requests and the Peepo 0, Gleaner harvests 2, the Maw ate 150;
    problems none. The game test took 10.16 s.
  - Not measured: client frame time on a player's hardware; two clients; a busy server; Jade's and the encyclopedia's
    replies (they answer a client's request); compression and framing; any other hardware.
- **C-P21's measurement** (C-P68): `ConcordanceWorkloadGameTests.gleanersHeartSearch` times a starved Gleaner pulse
  (its whole 729-position Heart search) and a fed one (its 147-position crop search), each after 1,000 untimed pulses,
  in every ordinary build. The `mod` job of run 38774859294 (job 116559763087) logged: "Concordance workload: Gleaner
  pulse starved 333 us (its Heart search, 729 positions, 729 of them loaded; the position list alone 304 us), fed 62 us
  (its crop search, 147 positions), each after 1000 untimed pulses; C-P21's line 250 us: over; runner: 4 processors,
  amd64, Java VM 25.0.4.1+1-LTS". Over the line, so by C-P68 a change is proposed and not applied: the position list
  alone took 304 of the 333 µs, which points at caching `Garden.nearest`'s fixed offset order; a vertical reach of ±1
  (243 positions) is the other. `HEART_REACH` stays 4 until the owner decides. One run on one shared runner, beside
  the other server tests.
- **The journey** (C-P69): `WORKSHOP`, `workshop_walk`, `workshop_problems`, `_band`, `_switches` and `_workshop_wants`
  in `tools/concordance_journey.py` (`--check` prints only the problems and exits 1 on one); `WORKSHOP`, `FACTORY`,
  `CHUNK_C_ITEMS` and `recipe_file` in `tools/acquisition.py`; kelp, barrel, writable book and bowl in
  `VANILLA_SOURCES`; `check_acquisition`'s workshop rows; `ConcordanceJourneyGameTests`' `WORKSHOP_RECIPES`,
  `theWorkshopsRecipesLoad` and `theLivingWorkshopFromAManualStart`, in the existing `jugcraft-test:drone_depot`
  structure (16×8×16 air), so the client-test selector sees no new shared file. Both tests passed in the `mod` job of
  run 38774859294 ("All 1836 required tests passed"). The test places its factory parts with `setBlock`, as compact
  one-block coal generators; a crafted generator builds the full multi-block. The route's table is in
  [the journey record](arcane-concordance-journey.md#the-living-workshop).
- **Client evidence** (C-P70): `ConcordanceWorkshopClientGameTests` (`6d54726fa`; review fixes `fd2b558c8`), registered
  in the gametest `fabric.mod.json`, 200 s in `WEIGHTS` (an estimate) and held by `check_workshop_client_evidence`. One
  client on the game's own dedicated server, at the ordinary GUI scale, random ticks held at 0:
  - the whole workshop in one view;
  - the stand lit and then dark (its crucible waiting for Mendvetch) from one place;
  - the crucible's Inspect page naming its stand;
  - Root and Rivet under way beside the workshop: its Inspect page at the ordinary scale and at GUI scale 4 (a 1920×1080
    window, the applied scale checked), and, with Jade loaded, Jade's snapshot checked and its overlay shot;
  - the rite completes (one foundation from exactly its offerings, 2 Ley Charge a step from each pylon, taken by its
    participant) and the batch makes two poultices in its two bowls;
  - 100 frames' render times logged: Mesa's software renderer in CI, never a player's hardware.

  It ran in client shard 4 of run 38774859294, which passed. Which of its seven shots were viewed is under
  [Unit 5](#client-evidence-run-38774859294); its frame-time line was not read for these records.
  `EncyclopediaClientGameTests` is unchanged: its crucible (unit 1, with a GUI scale 4 shot), anchor (Chunk A and unit
  2) and bed (unit 4) pages stand for C-10's three Inspect pages. Jade absent: the server tests, in the `optional
  integrations absent` job, which passed; no client runs without Jade. GuiLib: not relevant (Chunk C adds no GuiLib
  code; the Inspect page is the encyclopedia's own screen). JEI's pages: not seen.
- **Combined fixes, the checkpoint and the handoff:** under [Unit 5](#unit-5-evidence-and-handoff-c-10). The
  checkpoint is reported to the owner, not kept on this page.

## Interfaces expected to change

- **Saved data:** crucible: optional `formula_owner` and `request` (C-06); an unreadable program kept as text under
  `program` (C-01). Bed: optional `strain`, `ripened` and `graft`. A new stand block entity (energy, manual setting).
  No change to the formula item or the crucible's update tag. Stamps stay at 1 for the crucible and the bed.
- **Registries (stable once released):** `crucible_stand` (block, item, block entity); `rooted_thallite_foundation`;
  the ritual `root_and_rivet`; the product ids `stillroot_poultice` and `kiln_draught` (and `winter_brine`, C-P2); the
  graft trait `dewlit`; the organism's `neighbours` field and a property's `fouling` field; lang keys, codex entries
  (`alembic/stillroot_poultice`, `alembic/kiln_draught`, a Root and Rivet entry, a stand entry).
- **Shared code:** the pure `concordance/diagnostic` package; `Outcome` (fouling) and `Property`; `Formula.replay`
  (the refused step's index); `CrucibleBlockEntity` (`automate`, `use` and `useEmpty`'s authority, `formula`,
  `heatBelow`, the shortage, filing, `preRemoveSideEffects`); `CrucibleCompanion` (status, needed); `Alchemy.brew`
  (naming); `BrewItem.applySalve` and `Encounter` (the mending credit); `LairDossiers.prepare` (the Cinder Kiln);
  `ritual/Readiness`; `CircleAnchorBlockEntity` (the shared selection, signs on refusal and status, the combined line);
  `InspectPage` (crucible, bed and crop branches); the Jade provider and client (crucible, anchor, stand, bed and crop
  lines); `ConcordSpire.nearestPost` as a shared helper; `Sampler.Area` (kinds), `Habitat` (strain), `Verdict`
  (disturbance), `OrganismCropBlock` (`useItemOn`, strain, the niche helper), `VerdantBedBlockEntity` (strain, graft, the
  update tag), the canteen.
- **Tools:** `concordance_alchemy.py` (fouling, products, the stand's targets), `concordance_ecology.py` (neighbours,
  strain, graft, the compost page), `concordance_rituals.py`, `concordance_economy.py`, `concordance_progression.py`,
  `acquisition.py`, `concordance_journey.py`, `select_client_tests.py`, an `OWNER_FILES` table if the stand's art is
  imported; `check_mod_data.py`: `check_workshop_products`, the fouling mirror, the Ley-capacity rule, the graft,
  mixed-bed and strain rules, `diagnostic` in the pure list, the makeup kept out of Jade's crucible snapshot,
  `Signs.show` in the encyclopedia's forbidden list, and a rule that no client class reads `ConcordanceData.rules()` or
  `Alchemy.catalog()` (none does today; the rules are server-only statics, `ConcordanceData.java:28`, and the client
  tests' dedicated server may share the client's process, which would hide such a read).
- **Network:** no new payload. Jade snapshots carry more values; the bed's update tag grows (C-08, C-09), a contract
  change between server and client of the same build.

**As built in unit 1:**
- **Diagnostics:** `Diagnostic` (`of`, the crucible's factories, `text`, `encode`, `decode`) and `Code` (with
  `Code.Kind`).
- **The crucible:** `CrucibleBlockEntity.automate` (now public, returning its reason), `waiting`, `incompatibility`,
  `unreadableProgram`, `follow`, `statusLine`, `tankParts` and `below`.
- **Pure alchemy:**
  - `Formula.incompatibility` and `incompatible`; `Mixture.Refused` gains `step`; `Operation.describe` and
    `Operation.ingredient`;
  - `Assay.risks`, `CrucibleInspection` and `Heat.Below`;
  - `Property.fouled` (and the `fouling` field), `Product`, and `AlchemyCatalog.product`.
- **Shared Concordance code:** `Alchemy.glassLevel`, `product` and `STILLROOT_POULTICE`; `ComposeText` words a line
  nested in a diagnostic; `BrewItem.Observer` and `observe`.
- **Lairs:** `Encounter.mended`, `regenerationHeals`, `POULTICE_STATUS`, `REGENERATION_HEAL` and the private
  `creditHealing`; `LairDossiers.brews`, `poultices` and `FIRE_RESISTANCE`.
- **Encyclopedia and Jade:** `InspectPage.CRUCIBLE_REACH`; the Jade crucible snapshot's `tank` and `reason`.
- **Tools:** `tools/concordance_diagnostics.py`; `tools/concordance_alchemy.py` (`fouled`, `PRODUCTS`,
  `PRODUCT_REWARD_ROUTES`, `REGENERATION_INTERVAL`, `realised`); `check_diagnostics`, `check_workshop_products`,
  `check_poultice_credit`.
- **No new saved key:** an unreadable program reuses `program`. The crucible's update tag still has its four keys.

**As built in unit 2:**
- **Pure ritual code:** `ritual/Choice` (`of`, `NONE`, `rite`) and `ritual/Readiness` (`before`, `running`, `stages`,
  `shapeFaults`, `holding`, `ready`, `spentText`, `UNKNOWN`; records `Channel`, `Line`, `Reader`, `Spent`, `Held`,
  `Ending`).
- **The anchor:** `CircleAnchorBlockEntity.choose`, `readiness`, `spent`, `ending`, `endingLine` (an instance method and
  a static one) and `MARK_TICKS`; `participantsNeeded` made public; `faultText` gives both positions; the private
  `markReadiness`, `markFaults` and `part`.
- **Jade:** `ConcordanceDataProvider.SHOWN_OFFERINGS`; the anchor snapshot's `needed`, `offering<i>`, `offerings`,
  `spent_focus`, `spent_ley` and `reserved`.
- **Diagnostics:** `Code` gains `NOT_LENT`, `NEEDS_FOCUS`, `TOO_BRIGHT`, `TOO_FAR` and `PARTICIPANTS`, after unit 1's
  codes; `MISSING_INGREDIENT`, `UNAVAILABLE_POWER` and `INCOMPLETE_PATTERN` take their parameters. `ComposeText` names a
  `structure`.
- **Registries:** `JugcraftConcordance.ROOTED_THALLITE_FOUNDATION`; the ritual `jugcraft:root_and_rivet`.
- **Tools:** `RITUAL_SINKS` (with `STEP_TICKS` and `FOCUS_REGEN_TICKS`) in `tools/concordance_economy.py`;
  `PRODUCERS` and `VANILLA_TAGS` in `tools/concordance_progression.py`; `RITUALS` in `tools/acquisition.py`; the
  `"switches"` key, `_payment_text` and `_switches_text` in `tools/concordance_rituals.py`; `ROOT_AND_RIVET` in
  `tools/concordance_strata.py`; the `thallite` material in `tools/icon_materials.py`.
- **No new saved key or payload:** the ending lives in memory; Jade's anchor snapshot carries more values.

**As built in unit 3:**
- **The stand (C-05):** `CrucibleStandBlock` (`LIT`, `useItemOn`, `useWithoutItem`) and `CrucibleStandBlockEntity`
  (`energy`, `setting`, `given`, `plan`, `heat`, `useEmpty`, `statusLine`, `rate`, the record `Plan`; `JE_WARM`,
  `JE_HOT`, `JE_SEARING`, `CAPACITY`, `INTAKE`, `SET_TICKS`); `JugcraftConcordance.CRUCIBLE_STAND` and
  `CRUCIBLE_STAND_ENTITY`.
- **Pure alchemy:** `Heat.STAND_WARM`, `STAND_HOT`, `STAND_SEARING`, `standTarget`, `waitsForHeat` and `standBand`;
  `Heat.Below` gains a third component, `heat` (its two-argument constructor keeps the old behaviour).
- **The crucible (C-05, C-06):** `below` asks a stand beneath; `Shortage`, `shortage`, `courierRemainder`, `sample`,
  `backoffAfter`, `follow(Formula, UUID)`, `formulaOwner`, `request`, `preRemoveSideEffects`; the constants
  `FORMULA_OWNER`, `REQUEST`, `POST_REACH`, `FILING_BACKOFF` and `MAX_FILING_BACKOFF`.
- **Couriers:** `Couriers.nearestPost(ServerLevel, BlockPos, UUID, int)` and `FILING_REACH`; `ConcordSpire`'s own
  `nearestPost` removed.
- **Diagnostics:** `Code.NO_CRUCIBLE` (appended); `Diagnostic.courierQuota`; `courier_quota`'s parameters fixed.
- **Peepo:** `CrucibleCompanion`'s private `shortage()` removed; `needed` and `status` read the crucible's.
- **Jade:** `ConcordanceDataProvider.STAND` and `standSnapshot`; the client's `StandTooltip`.
- **Tools:** `tools/concordance_alchemy.py` (`OWNER_BLOCKS_FILES`, `STAND_*`, `stand_model`), `tools/owner_art.py`
  (`block_asset_imports` merges every slice's table), `tools/concordance_economy.py` (`crucible_heat`, `je_to_heat`,
  `STAND_JE`), `tools/concordance_logistics.py` (`FILING_REACH`, `CRUCIBLE_BACKOFF`, `CRUCIBLE_MAX_BACKOFF`, two notes,
  the fourth codex page), `tools/concordance_diagnostics.py`, `tools/concordance_rituals.py` (the foundation's words);
  `check_crucible_stand` and `check_crucible_supply`.
- **Saved data:** the stand's `energy` and `setting` (stamp 1); the crucible's optional `formula_owner` and `request`
  (its stamp still 1). No new payload; Jade's stand snapshot is new.

**As built in unit 4:**
- **Pure ecology:** `HabitatInspection` (`State`, `unjudged`, `judged`, `verdict`, `pace`, `unplanted`), `Neighbours`
  and `Graft`; `Organism.neighbours`; `EcologyParser.graft`; `EcologyCatalog.mixed`, `grafts`, `graft` (by id; by host
  and item), `niche`, `graftLines` and the grafts as its constructor's third argument; `Sampler.Area.kinds` and
  `Sampler.habitat(..., strain)`; `Habitat.strain`, `MAX_STRAIN` and `strained`; `Verdict.strained`.
- **The garden:** `Garden.poorest` and `TEND_TICKS`; `VerdantBedBlockEntity` `inspect`, `forgetVerdict`, `strain`,
  `ripened`, `strainNow`, `settle`, `rest`, `compost`, `tend`, `setStrain`, `mixed`, `mixedLine`, `strainLine`, `notes`,
  `graft`, `graftFor`, `setGraft`, `clearGraftUnlessFor`, `niche` and the keys `STRAIN`, `RIPENED`, `GRAFT` and
  `GRAFT_HOST`; `VerdantBedBlock.pour` and its empty-hand rule; `OrganismCropBlock.useItemOn`, `speed` and the private
  `graftBed`; the Gourd Canteen's `BED` pour.
- **Encyclopedia, Jade and JEI:** `InspectPage.GARDEN_REACH` and its garden branch (`garden`, `grafted`); the Jade bed
  snapshot's `strain` and `graft`; the client's `reasonText` (a reason with no factor), `strain` and the crop's "As it
  grew" and graft lines; `ComposeText` names a `graft`; `JugcraftJeiPlugin.addCompostInfo` and `recipe_view.json`'s
  `garden_compost`.
- **Tools:** in `tools/concordance_ecology.py`, `MIXED_BED_PERCENT`, `MAX_MIXED_BED_PERCENT`, `MAX_STRAIN`,
  `TEND_TICKS`, `GRAFT_CAP`, `GRAFTS`, `graft_json`, `graft_text`, `_mixed_and_strain_text` and the organisms'
  `neighbours`; `garden_compost` in `tools/recipe_view.py`; `INSPECT`'s `garden.*` in `tools/encyclopedia.py`;
  `check_graft`, and the rules above in `check_ecology`, `check_encyclopedia`, `check_diagnostics` and
  `AUTHORITY_ROUTES`.
- **Saved and sent:** bed keys `strain`, `ripened` and `graft`, saved only when set, the stamp staying 1; the update tag
  always adds `strain` and `ripened`, and `graft` and `graft_host` while grafted. No new block, item or payload.

**As built in unit 5:**
- **No new id, saved key, payload, lang key or test structure:** the journey's server test reuses
  `jugcraft-test:drone_depot`; the game's own code is unchanged.
- **Server game tests:** `ConcordanceWorkshopLoadGameTests` (`loadCard`, its `ID` and window constants; registered in
  the gametest `fabric.mod.json` after `ConcordanceWorkloadGameTests`); `ConcordanceWorkloadGameTests.gleanersHeartSearch`
  and `HEART_SEARCH_LINE_US` (250); `ConcordanceJourneyGameTests`' `WORKSHOP_RECIPES`, `theWorkshopsRecipesLoad` and
  `theLivingWorkshopFromAManualStart`.
- **Client game test:** `ConcordanceWorkshopClientGameTests`, registered after `ConcordanceWayfaringClientGameTests`;
  `WEIGHTS["ConcordanceWorkshopClientGameTests"]` = 200 in `tools/select_client_tests.py`.
- **Tools:** in `tools/check_mod_data.py`, `check_load_card`, `check_workshop_client_evidence`, `WORKSHOP_CLIENT`,
  `WORKSHOP_CLIENT_SHOTS`, `WORKSHOP_CLIENT_PIECES` and `_inside_block`; in `tools/acquisition.py`, `WORKSHOP`,
  `FACTORY`, `CHUNK_C_ITEMS` and `recipe_file`, which `check_acquisition` ties to the files on disk; in
  `tools/concordance_journey.py`, `WORKSHOP`, `workshop_walk`, `workshop_problems`, `_band`, `_switches` and
  `_workshop_wants`, and `--check`'s exit status; `VANILLA_SOURCES` in `tools/concordance_progression.py` (kelp,
  barrel, writable book, bowl); Jade's row of `tools/concordance_delivery.py`'s `MATRIX`, pasted into the delivery
  record.
- **Workflow (`bfb08770b`, for the owner's review):** the Build workflow's `workflow_dispatch` input `load_card`
  (boolean, default false) and the job `load-card` ("workshop load card"), which runs only on a manual run with it
  ticked. No existing job, trigger, gate or permission changes.
- **Records:** `docs/TESTING.md` gives the card's command and points to C-P19 for its limits.

## Dependencies and order

1. C-00: done (the merge, its CI, this page).
2. Unit 1: C-01, then C-02 (its preview, risk line and reasons use C-01's).
3. Unit 2: C-03, then C-04 (its table is shown by C-03's readiness).
4. Unit 3: C-05 after C-04 (its recipe consumes the foundation) and C-01 (the reasons that make it idle); C-06 after
   C-01 (the shortage and incompatibility).
5. Unit 4: C-07, then C-08 and C-09 (their lines sit on C-07's Inspect branch).
6. Unit 5 after units 1 to 4.

The smallest first unit is C-01: no new id, no owner input, the first seven defects above fixed and the reload,
blockage and restart evidence. C-07 to C-09 need no owner input and could start while other units wait, but they land
after the first path (09:16).

## Risks

- **Base and review:** Chunk C stacks on Chunk B, which has no pull request, plus a main merge; how it is reviewed was
  open (above; decided 23 October 2026, C-D8). A change to Chunk B in review, or a new main, means another merge.
- **Shared hot files** with open pull requests: lang, the gametest `fabric.mod.json`, `check_mod_data.py`,
  `CHANGELOG.md`, `recipe_view.json`, `tools/owner_art.py` (#351, #354), `docs/WHAT_EXISTS.md` (#366),
  `docs/INTEGRATION_STATUS.md` (#361 to #365).
- **Plan wording, deviations to show at the checkpoint:** "Reuse FluidRecipe … MachineCompanionPort" (09:136): the
  crucible path reuses the `Port` interface and the shared fluid storage instead; the graft is bed-bound, not an item
  component (03:76, 03:81); the Inspect page stands in for the notebook screen (08:55-59); the 9×9 ceiling is unused
  (03:91); the formula's revision is process-local, not a stable version (03:46); no preservation wash this chunk
  (09:124's "such as"): the owner chose the Kiln Draught.
- **The fouling rule changes existing brews:** any Verdance brew carrying contaminant (from bone meal, gloamcap, spider
  eye, magma cream or a searing stir: `concordance/ingredient/*.json`) heals less than before. Bottles already made
  keep what they carry; written formulas replay to the new outcome.
- **Named stacks** do not stack with older unnamed brews of the same outcome, and an automated crucible pauses on one in
  its output (tested, with a reason).
- **The Kiln Draught also turns the Cinder Tyrant's fire:** his flame is `jugcraft:flame`
  (`lair/tyrant/CinderTyrantEntity.java:475-477`), listed in `#minecraft:is_fire`
  (`src/main/resources/data/minecraft/tags/damage_type/is_fire.json`), which vanilla does not apply under Fire Resistance
  (mc263src `LivingEntity.java:1155`). Vanilla's Fire Resistance potion and the crucible's Ember brews already
  do this; the dossier fact will advertise it. The Salamander Charm stops only hot ground
  (`lair/tyrant/SalamanderCharm.java:7-12`). A balance question for the checkpoint.
- **The poultice's credit** (as built, C-P30): food, a vanilla potion or a beacon healing in the same tick as one of
  the poultice's heals can be credited for that heal, at most one health and never past the fight's hurt; heals from
  Concordance spells and blows are read out exactly. A poultice replaced by a stronger or longer Regeneration (a potion,
  a beacon, milk) ends its note, so heals vanilla's hidden instance gives after are not credited (under-credit, which is
  safe). A real client's patient (`doTick`, with `FoodData.tick`) is not run; the tests run the patient's tick by hand.
- **The crucible's reason** comes from its last pulse, so it can be up to 20 ticks old in Jade, the status line and
  Peepo (for example FULL just after the output is emptied). The incompatibility is rechecked lazily and is never stale.
- **Authority is a behaviour change** (C-P5): adventure mode, spawn protection on a dedicated server with operators,
  claims and protected towns now refuse crucible work. Recording stays open (C-P24).
- **An incompatible formula** stops even a finished batch from bottling automatically (C-P23).
- **Naming by effect** makes any Regeneration salve carrying nothing harmful a Stillroot Poultice, which earns credit
  (C-P29).
- **Unit 1, not yet seen by a person:** the Expeditions page with the poultice's fact for all four keepers stays within
  the 96 rows a page holds (about 77 by the spec review's count; `theExpeditionsPageHoldsEveryKeeper` checks it, a
  server test that passed in CI from run 38684732031 on), and whether the client test's Jade shot shows Jade's overlay
  (`takeScreenshot` with the HUD; its shard passed, not viewed).
- **Feature switches:** the foundation needs `machines` and `thallite` (C-04); with either off the powered route is out
  of reach and the campfire route stays. Whether Fabric applies load conditions to Concordance data is unverified.
- **Strain** is a new soft cost on every harvest path, Peepo's and the Crop Harvester's included: visible, capped and
  rejectable at the checkpoint. A crop placed ripe by a command never strains its bed.
- **Graft:** breaking a crop and replanting the same organism keeps the bed's graft. Proposed as acceptable (C-P18), for
  the owner to confirm: no item is duplicated.
- **Over-supply** between a Peepo and a courier, if the subtraction is wrong: tested. **Requester online:** a courier
  serves a crucible only while its formula's setter answers.
- **Holds versus the stand:** a Preserve hold freezes the band, so a stand-driven formula waits; by design and documented.
- **Test isolation:** `ConcordanceData.replace` is global; a reload test that forgets to restore the rules would fail
  other Concordance tests. Unit 1's tests swap the rules only through `under()`, which `check_diagnostics` enforces
  (C-P28).
- **Load measurement** on CI is noisy and hardware-specific: numbers are recorded, and only an agreed, generous limit is
  asserted. As built in unit 5 (the load card, C-P66):
  - the game test server runs its ticks back to back, not at a 20-tick server's rhythm, so per-tick work is comparable
    and wall-clock behaviour (CPU boost, cache warmth, collections per second) is not;
  - one run on one shared runner, baseline first, so JIT warmth favours the workshop's span; the drift line (+0.023 ms
    in run 38774859294) reports this and does not remove it;
  - an autosave may land about once a span (every 6,000 ticks) and would show in the maximum and the over-50 ms count;
    the card does not say what caused the baseline's 64.3 ms maximum (its one tick over 50 ms) or the workshop's 29.3 ms
    (run 38774859294);
  - the packet counter's own garbage adds to both spans' collection pressure; its encoding time is excluded, its effect
    on collection is not;
  - the test player is not a client: it does not move, the test acknowledges its chunk batches, payloads sent only to
    a client that can take them never reach it (signs are counted from `observe`, an upper bound), and Jade's and the
    encyclopedia's replies need a client and are not measured;
  - the Peepo brought 0 Mendvetch while measured (the porter 44): whether it served the raised crucible at all in the
    headless server, or lost every search to the porter, the card's lines do not say. The card asserts only that
    Mendvetch was brought;
  - the Gleaner's harvests while measured were 2 (a Mendvetch and a Dewmoss; no Sunpetal or Gloamcap), so the harvest
    assertion counts harvests since the build, five crops having been planted ripe;
  - forcing the yard's chunks changes what ticks: its 12 chunks, flat ground far below included, tick in both spans;
  - `JAVA_TOOL_OPTIONS` reaches every Java program in that shell or CI step, Gradle's own included: harmless, but noisy;
  - the porter's integrity of 64 allows about 64 trips, so a much longer span could wear it out; in run 38774859294 it
    served 22 requests while measured.
- **Unit 2, not yet seen by a person:**
  - the cached shape report can hold a refused start's charge and ownership faults for up to 100 ticks: the status lines
    and Jade show them while readiness reads the channels itself, so the two can disagree for that window;
  - `report()` may send a block update when the linked channels change under the Inspect page or Jade, as it already
    did: a cache refresh, not a sign or a saved change;
  - the one interruption line is long and may be clipped on a narrow action bar; no test reads the action bar;
  - the ending is lost on a reload (C-P37), and `TAMPERED`'s line under-counts its last draw;
  - an operator's command reads any loaded circle, so it can mark one from afar; the marks are signs only;
  - the foundation had no consumer until unit 3 (against 09:124's "no decorative recipes"; since unit 3, the Crucible
    Stand's recipe), and its icon is a draft whose roots may read as legs;
  - the codex's first `entry://` link was checked against Modonomicon's source by the design, never clicked; the audit
    stops a link to a missing entry, page id or page number, not a wrong link format;
  - widening `VANILLA_TAGS` with `c:ingots/thallite` also lets the "Overworld things" recipe rules accept thallite
    ingots; the switch rule looks one step down only, so an ore-only or loot-only route is not seen;
  - combined offerings: the Kindling's 11 items beat Root and Rivet's 9 (the existing rule, now pinned by a test);
  - the economy model is conservative: it states 900 Focus a day where one player regains 600;
  - the client shots depend on Jade drawing into `takeScreenshot` and on the client test's server sharing its process;
  - while a rite runs, a part other than a channel's pylon broken since the last step (a Warding Stone, the clearance)
    shows only at the next step, within 40 ticks: until then the status lines and Jade show that step's report. A pylon
    gone or unloaded is a readiness line at once (the unit review);
  - the codex pages' length: `circles.root_and_rivet.p1` is about 926 characters and the Lesser Circle's "Reading It"
    (`circles.lesser_circle.p2`) about 788; neither has been seen in a client, so either may overflow its page;
  - with the `machines` or `thallite` switch off the plate's recipe is gone, and Root and Rivet's codex entry still has
    its recipe page. In Modonomicon 2.16.0's source `BookRecipePage` then logs "Recipe … not found" and shows a barrier
    in the recipe's place without blocking the book; not seen in a client.
- **Unit 3, not yet seen by a person:**
  - the stand and the crucible tick in no fixed order, so the crucible heats on the stand's last decision, at most a
    tick late; the JE paid stays exactly the rate times the ticks paid, and the tests read its light in retried
    assertions;
  - the crucible's reason can be up to 20 ticks old, so after a refill a stand may idle up to 20 ticks more;
  - a stand fed less than its rate holds the crucible on the ticks it cannot pay while the crucible is below the band
    (following a formula, also at the band's lowest degree), so it reaches the band for the same JE a degree, and its
    light goes dark only after 10 such ticks in a row (C-P43 as amended by the unit review; before, a stand fed below
    half its rate never reached the band, paid all it received and changed its light almost every tick). Inside the
    band such a tick cools, so with no formula, fed at or below half its rate, it hovers at the band's lowest degree
    and the one below it;
  - whether the coil drawn full-bright reads as lit in daylight, whether the owner's treated iron reads as the
    silver-grey cladding, and whether a one-block stand suits "real-life-sized stations" are for the owner's review;
  - with no `EnergyConnectable`, a worldgen structure could not pre-connect cables to a stand (none places one);
  - with `machines` or `thallite` off, the stand's recipe does not load and its codex recipe page shows it missing
    (Modonomicon's behaviour as read for unit 2, not seen);
  - the model's `light_emission` key and automatic UVs follow the shipped charged Ley Pylon's model; 26.3's model
    loader was not read for them;
  - the courier ledger, its ids and `problems()` are world-wide, and an ownerless post serves anyone: every new test
    post has an owner, test structures stand a few blocks apart, and each synchronous test body is assumed not to
    interleave with another test's ticks;
  - over-supply stays possible, bounded and with nothing lost, through stranded cargo taken up later, and through a
    request a Peepo fills before any courier has claimed it, which a porter then claims and picks up before the
    crucible's next pulse (cargo carried is always delivered, C-P51); two WANT signs show when a quota wait begins and
    when it ends;
  - a crucible files only for what fits, but slots a hopper fills after it filed leave the porter on `FULL`, holding
    the post's queue until there is room or the formula is cleared;
  - a request outlives an unloaded crucible and counts against its setter's 16 until delivered or cancelled; delivery
    does not ask `Authority` at the crucible (C-P53); an unclaimed request open when its setter leaves holds the post's
    queue for every request behind it (`OWNER_OFFLINE`) until they return or someone who may change the crucible clears
    its formula;
  - the Peepo unload check depends on vanilla's chunk tickets and unload delay (1,200 ticks allowed) and on the
    companion's transport retrying while fenced in; the full-crucible check on the companion handing back unused
    cargo; both read in the code when written. Both are `CompanionAutomationChecks`, run by `PeepoCompanionClientTests`:
    the unloaded-crucible check failed in run 38720814268 (shard 0) on a test defect, fixed in `9098c06b1`, and both
    passed in run 38731825181 (shard 0);
  - the unit's hot shared files: lang, `check_mod_data.py`, `CHANGELOG.md`, `tools/owner_art.py` (#351, #354),
    `docs/WHAT_EXISTS.md` (#366), `docs/INTEGRATION_STATUS.md` (#361 to #365); the open pull requests' file lists read
    for C-06 include none of the three Peepo files it edits (`CrucibleCompanion`, `PeepoIntegrationClientTests`,
    `CompanionAutomationChecks`).
- **Unit 4, not yet seen by a person:**
  - behaviour changes, all Proposed and visible: an item held in the main hand never reports, wakes or tends a bed
    (C-P56); a Greenwarden holding a Dewmoss cannot harvest a ripe Sunpetal by a plain use, which grafts or is refused
    (C-P62); bone meal on a growing crop feeds its bed rather than reporting (C-P61); a Greenwarden's empty hand on a
    growing crop tends its bed before reporting (C-P61, the unit review);
  - resting only while no crop grows (C-P60): a Gleaner-run Sunpetal bed with no compost settles at strain 3 (half
    pace), by the model's arithmetic, not measured; a crop stalled by strain lets its bed rest, so strain never stops a
    plant for good;
  - counting whatever took the crop also counts a crow raid, a Hay Golem and breaking a ripe crop to move it; a crop
    placed ripe, or aged by bees, never strains (an under-count, which is safe);
  - bees do reach these crops in 26.3 (confirmed by reading, the unit review): the mirror's
    `data/minecraft/tags/block/bee_growables.json` includes `#minecraft:crops`, its `Bee.java` steps any `CropBlock` by
    `getStateForAge(age + 1)` and `setBlockAndUpdate`, and the generator puts the four crops in `#minecraft:crops`
    (`crops.json`, from `tools/concordance_ecology.py`'s `tags()`). A gap since roadmap step 14: a bee's step skips `grow`,
    so a stalled crop advances with no niche check, no nutrient cost and no drying; it never calls `ripen()`, so that
    harvest never strains; and it never clears a graft. The owner may choose to move the crops to a
    `jugcraft:organism_crops` tag that fertilizer and garden maintenance also accept;
  - bone meal used sneaking or from a dispenser, and fertilizer's area, still reach `performBonemeal` without asking who
    may change the bed, as on any vanilla crop, and each such dose that feeds a bed eases a strain;
  - when a bed was last tended is not saved, so a restart allows one early tending;
  - the Inspect page and Jade can differ: the page takes a fresh reading, while Jade shows growth's last note, stale
    until the crop's next growth attempt (about 68 seconds on average); since the unit review a crop newly set on the
    bed, or a graft, forgets the last plant's note at once, so Jade never shows one plant's reasons on another. A ripe
    crop makes no growth attempt, so one grafted, or set there ripe, shows no "As it grew" until it is harvested and
    grows again;
  - one `RateGate` ("garden", 4 ticks) serves the canteen, bone meal, reports, tending and grafting;
  - the Habitat Gauge's disturbance mode shows the area only; its suitability mode includes the strain and the graft;
  - a Verdant Heart set on a recently harvested bed sees one strain in its verdict until the bed rests;
  - there is no explicit ungraft: a graft stays on an empty bed or under a device, and one a data pack removes stays
    saved, read by nothing, until the next graft replaces it or the data returns;
  - a refused graft consumes the use, and the client may swing as it predicts: cosmetic;
  - the tests share the level's 16 samples a tick: they prefetch at ticks 10, 20 and 30 and read at 41 to 55, so a
    collision makes a reading wait and a test fail, a risk existing tests already carry;
  - the JEI compost pages are not seen at runtime, add pages to many vanilla items (leaves, saplings, flowers) and
    depend on JEI registering after the client has the server's tags, which is unconfirmed;
  - the bed's update tag grows by up to four keys, a contract between a server and a client of the same build;
  - unconfirmed by reading: whether Fabric fills `Item.BY_BLOCK` for the crops' items (replanting or air after a sickle
    or the Crop Harvester; the tests accept both); the Crop Harvester's footprint against the test's glowstone; a
    gametest server's `Component.getString()` resolving mod lang; `lookAt`'s aim at the crop and the bed; light settled
    by ticks 44 to 46; every test of a batch starting on one tick; the two new codex pages' length;
  - nothing of the unit had been compiled against Minecraft or run when this was written (since then, the merge's run
    38726242149 passed every job, unit 4's tests included).
- **Unit 5, not yet seen by a person:**
  - the client view's framing is arithmetic for FOV 70 and a 16:9 window; in the viewed `jugcraft_concordance_workshop`
    the whole workshop is in view but small at that distance;
  - the client shots rely on about 200 ticks of formula and Root and Rivet's 160; the class passed in run 38774859294;
  - one client only; no CI job runs a client without Jade;
  - the client class's weight (200 s) is an estimate: run 38774859294's shard 4, which ran it, took 32 min 23 s in all;
  - the Jade overlay on the anchor is not seen: no Jade box is visible in the 480×270 preview of
    `jugcraft_concordance_anchor_root_and_rivet_jade`, while the test's Jade snapshot assertion passed;
  - the stand's lit and dark pair answers C-P48's daylight question only as far as Mesa's software renderer shows it;
    the lit shot was viewed (the copper coil lit), the dark one not;
  - the journey test is end to end, so its first failure hides the rest; the porter works on its own time and the
    900-tick budget relies on it; the courier ledger is shared with every test running at once and `problems()` is read
    only at the end; it uses compact one-block generators where a player's are multi-block; the factory's own
    reachability is left to the factory's graph;
  - the workflow change makes a pull request that changes `build.yml` run every client class; GitHub took the
    `load_card` input from this branch's file in run 38774859294; the notice's length limit is not known.
- **Unverified:** the null-side Fabric item insert (inferred from a passing post test *(survey)*; the C-06 design read
  Fabric API 0.161.0's source: a `Container` falls back to its plain slots, each asking `canPlaceItem`; not re-read for
  these notes); vanilla's container drop when a crucible overrides `preRemoveSideEffects` (read in the mirror's `HEAD`,
  `BlockEntity.java:262-270`, not seen in a game); whether Jade's accessor gives the viewing player;
  `#minecraft:supports_crops` on these crops (bees reach them: unit 4's risks); Fabric's load conditions on Concordance
  data; whether the client tests' dedicated server runs in the client's process.

## Explicit don'ts

1. Do not build another cauldron economy, a universal assembler, a second mana pool, a second logistics ledger or a
   parallel worker system (03:17; 06:122).
2. Do not clear, reroll or silently invalidate a formula; do not void output; do not duplicate spent heat or power on a
   break (03:54; 03:62-65).
3. No client-sent temperatures, totals or stacks; requests choose permitted settings (08:59; 08:99).
4. Keep the makeup out of the crucible's update tag and Jade (`check_mod_data.py:16290-16294`;
   `ConcordanceDataProvider.java:138-142`).
5. No crop death; no pollution read in the garden (09:128; D-005).
6. No new ore; the "Tide-bearing mineral" stays a role (03:41).
7. No product as the only source of steel, copper cable, clean water or ordinary medicine (06:26).
8. No wear, lubricant or replacement parts on the stand (D-004).
9. Do not edit `PeepoEntity`, `CompanionHusbandry`, `CompanionPopulationChecks` or `PeepoDeliveryClientTests` (#357,
   #369); do not replace Peepo with generic clones (03:181).
10. Do not change Preserve's crucible-only scope (`Rime.java:31-33`) or Chunk B's Proposed decisions without the owner.
11. Do not hand-edit generated data; change the generators.
12. Do not execute anything in the owner's library, and do not import whole trees (08:167).
13. Do not add a mixin for the poultice's credit while the tick rule works (CLAUDE.md: avoid unnecessary mixins). Main's
    own heal and effect mixins (`NouveauThreadMixin`; since main's #475, `JimtaSpellStatusMixin` and
    `JimtaCurioLivingMixin`) are allowed while they name nothing of the fight's credit (`check_mod_data`).
14. Do not show signs, or write anything, from the anchor's shared `status()` or any encyclopedia page.
15. Do not read the server-only rules on the client.
16. Do not mark a Proposed decision approved for want of a reply; do not count one client as two; do not call the chunk
    complete because tests compile (10:162; 09:20; 09:315).
17. Do not restore or rewrite main's `CHANGELOG.md` here; this branch's merge resolution is the only change.

## Unit 5: evidence and handoff (C-10)

### Candidate and CI

- Candidate: `bb856f873` on `claude/chunk-c-living-workshop` (units 1 to 5 on the base `e079ad4a3`; not merged, no
  pull request). CI run [38774859294](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38774859294), a
  workflow_dispatch of the Build workflow with `load_card` ticked, in two attempts; every job passed by the second.
  The commit after the candidate changes only records.
- Attempt 1: every job passed except client shard 1, which failed on main's `PeepoPopulationClientTests` cow-breeding
  flake, the same that failed unit 1's run 38684732031 (PR #357, since merged in main as `40381834a`, fixes it in main's
  Peepo code, which this branch does not edit).
- Attempt 2 re-ran client shard 1 alone, and it passed, so every job passed.
- The jobs, start to end:
  - `mod`: "1836 GAME TESTS COMPLETE IN 5.493 min" and "All 1836 required tests passed"; the job took 18 min 38 s. No
    game test is marked optional, so every server game test of units 1 to 5 passed, the journey's two new ones and
    `gleanersHeartSearch` among them; the load card, not selected there, passed at once;
  - `optional integrations absent`: passed, 7 min 9 s;
  - client shards: 0 in 38 min 2 s, 1 in 34 min 6 s (attempt 2), 2 in 26 min 31 s, 3 in 44 min 43 s, 4 in 32 min 23 s.
    `ConcordanceWorkshopClientGameTests` ran in shard 4. The longest, shard 3, stays inside its 60-minute limit;
  - workshop load card: 2 min 16 s; "1 GAME TESTS COMPLETE IN 10.16 s" and "All 1 required tests passed".
- Runs on the way (this branch has no pull request):
  - 38684732031 (unit 1, through `0b1039312`): every job passed after one re-run of client shard 1, whose failure was
    main's `PeepoPopulationClientTests` cow-breeding flake (two cows wandered apart);
  - 38693262154 (unit 2, `23f28b90e`): every job passed on its first attempt;
  - 38720814268 (unit 3, `2f13fed54`): `mod` (every server game test), `optional integrations absent` and client
    shards 1 to 4 passed; shard 0 failed on `CompanionAutomationChecks` "an unloaded crucible" (below);
  - 38726242149 (the merge of units 3 and 4, `1c7dc577c`): every job passed; unit 4's first CI run, its server and
    client tests passed;
  - 38731825181 (`9098c06b1`): every job passed, every client class (171) included; `PeepoCompanionClientTests`, which
    holds the fixed check, passed in shard 0;
  - 38773020842 (`fd2b558c8`, unit 5 before its compile fix): the game tests' compile failed: inside the load card's
    `Stats` record, `median(sorted)` resolved to the record's own `median()` accessor. `bb856f873` renames the helper
    `middle(long[])`.
- Local, for the records commit: `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check` and `git diff --check`, each run and passed. `./gradlew build`,
  `./gradlew runGameTest` and `./gradlew runClientGameTest` ran in CI only.

### Combined fixes

- `9098c06b1`, on the merge of units 3 and 4: unit 3's `CompanionAutomationChecks` "an unloaded crucible" failed in
  shard 0 of run 38720814268. It was a test defect: the check lifted the forced chunks in the same tick it moved the
  player back, so the companion was reloaded as a new copy and the check read the removed one; in the game the delivery
  is one transaction. Only the check changed (`CompanionAutomationChecks.java`), and it passed in run 38731825181.
- Unit 5's own: the unit review's fixes (`fd2b558c8`: the GUI scale 4 shot taken at scale 4 in a larger window, the
  stand's dark shot waiting for its crucible's reason, the load card asserting only that Mendvetch was brought, and
  tighter audits) and the compile fix (`bb856f873`). CI found no other fault in unit 5.

### The demonstration (09:120, 09:136, 09:140)

"Server" means a game-test server with test (mock) players; "client" means one real client on the game's own dedicated
server, never two. Every server test named passed in run 38774859294's `mod` job, and in `optional integrations
absent` with its Jade checks skipped; the client classes passed by their shards in run 38731825181 (every client
class) and, for `ConcordanceWorkshopClientGameTests`, in shard 4 of run 38774859294. Each test starts from its own
fresh structure; only the journey test walks one player through the whole workshop, and no person has played it.

| Item | Evidence |
|---|---|
| Survival-reachable from a manual start (09:140) | Server: `ConcordanceJourneyGameTests.theLivingWorkshopFromAManualStart` (one survival mock player with vanilla materials, two Thallite ingots, two full Kindled Lanterns and a blank formula: Mendvetch crafted by the server's recipe matcher, the Kiln Draught over a campfire and the poultice over a magma block by hand, Root and Rivet from lantern-filled pylons, the stand fed by a coal generator and a pump, a porter bringing the formula's Mendvetch from a chest at its post, a mixed bed and a Dewlit graft; nothing lost or made twice) and `theWorkshopsRecipesLoad`. Checks: `concordance_journey.py --check`, `check_acquisition`'s workshop rows |
| Powered convenience | Server: the stand's tests in `ConcordanceAlchemyGameTests`, `theFullChainRunsFromAPumpAndACable`. Client: `ConcordanceWorkshopClientGameTests` (the stand heating a formula's batch to two poultices, its water from the pump) |
| Formula reload | `aChangedRuleKeepsTheFormulaAndSaysWhy`, `aRestoredRuleResumesTheFormula`, `aWrittenFormulaThatNoLongerReplaysSaysWhichStep`, `aSavedFormulaThatNoLongerReadsIsKeptAndReported` |
| Output blockage | `aFullOutputPausesAndVoidsNothing`, `anOldUnnamedBrewInTheOutputPausesAndSaysSo`, `theCompanionAndTheCrucibleAgreeOnFull` |
| Container returns | `aPoulticeReturnsItsBowlToAFullInventory` |
| Interruption | `aDryPylonStopsRootAndRivetAndKeepsTheOfferings`, `theInterruptionReasonIsKept` |
| Save and reload | `aRestartJustBeforeOutputBottlesOnce`, `rootAndRivetSavedMidRunLapses`, `theCrucibleStandKeepsItsEnergyAndSettingThroughASave`, `theCrucibleKeepsItsRequestThroughASave`, `anOlderBedLoadsWithoutItsNewKeys`, `strainSurvivesASave`, `aGraftSurvivesEveryHarvestPathAndASave` |
| Full destination and unloaded target (09:136) | Server: `aCourierFillsACrucibleUntilItIsFull`, `anUnloadedCrucibleWaitsAndLosesNothing`, `aBrokenCrucibleTurnsItsRequestBack`. Client: `PeepoIntegrationClientTests` (crucible demand), `CompanionAutomationChecks` through `PeepoCompanionClientTests` (a full crucible; an unloaded one, "delivered once, none lost or doubled") |
| No profit loop | `workshopProductsAreNeverWeighed`, `theFoundationIsNeverWeighed` (since the merge with main `f904df3fd`, `theFoundationIsNeverFormed`, not yet run), `productsNeverFeedTheCrucible`, `aStandTurnsElectricityIntoHeatOnly`, `theBonusSpendsNoExtraNutrientAndMintsNone`; `check_workshop_products`, `check_economy` |
| Poultice credit | the eight tests unit 1 added to `LairEncounterGameTests` |
| Garden | the 21 tests unit 4 added to `ConcordanceGardenGameTests` (33 in the class) |
| Load (C-10) | `ConcordanceWorkshopLoadGameTests.loadCard` (below); `ConcordanceWorkloadGameTests.gleanersHeartSearch` (C-10's As built notes) |

### The load card (run 38774859294)

The "workshop load card" job's output, as the job logged it (job 116559763522, 2026-10-15 17:08:45 to 17:11:01 UTC, on
`bb856f873`; one trailing space trimmed, after "0 total;"):

```text
===== Concordance Chunk C workshop load card (C-10) =====
build: Jugcraft 0.1.0-alpha; Jade loaded
runner: Minecraft Version: 26.3
runner: Operating System: Linux (amd64) version 6.17.0-1022-azure
runner: Java Version: 25.0.4.1, Eclipse Adoptium
runner: Java VM Version: OpenJDK 64-Bit Server VM (mixed mode, sharing), Eclipse Adoptium
runner: Processor Vendor: AuthenticAMD
runner: Processor Name: AMD EPYC 7763 64-Core Processor
runner: Frequency (GHz): -0.00
runner: Number of physical packages: 1
runner: Number of physical CPUs: 2
runner: Number of logical CPUs: 4
runner: Memory: 465567744 bytes (444 MiB) / 889192448 bytes (848 MiB) up to 4192206848 bytes (3998 MiB)
runner: JVM Flags: 0 total;
runner: max heap 3998 MiB; collectors G1 Young Generation, G1 Concurrent GC, G1 Old Generation
runner: JVM arguments: -Dfabric-api.gametest.filter=jugcraft-test:concordance_workshop_load_game_tests_load_card -Dfabric-api.gametest -Dfabric.dli.config=/home/runner/work/jugcraft/jugcraft/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=server -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Duser.country -Duser.language=en -Duser.variant
world: game test server (flat world, ticks run back to back); noon and weather held, mobs and wandering traders off, random tick speed 3; player list view 0 and simulation 0 (the chunk map views at least 2); the yard's 12 chunks force-loaded (11 by the card); players 1 (a server-side test player, no client)
spans: baseline from tick 2996, workshop from tick 10497; each 6000 ticks in 60 windows of 100 after warm-ups of 2000 and 1500 ticks (at least 2000 and 1500, then until the loaded chunk count held 400 ticks); tick times net of the test's own work, including the simulated player's actions and output extraction; power from Creative Energy Cells (test scaffolding, not a player's generators)
tick ms, net of the test's own work | median p95 mean max over-50ms | 100-tick window means: median p95
baseline 0.074 0.124 0.092 64.3 1 | 0.076 0.123
workshop 0.314 0.674 0.370 29.3 0 | 0.350 0.521
raw medians 0.078 and 0.330 ms; the test's own work 0.005 and 0.029 ms a tick
delta: median +0.240 ms, p95 +0.550 ms; baseline drift (second half less first) +0.023 ms
memory: retained after a full collection: baseline 375 -> 376 MiB, workshop 377 -> 377 MiB (+0 MiB beyond the baseline's change); heap in use +24.79 and +44.90 MiB per 1,000 ticks; collections 0 (0 ms) and 0 (0 ms)
chunks loaded: baseline 961 -> 961, workshop 961 -> 961
counts: Crucible Stand 1, Alembic Crucible 1, Electric Pump 1 (bronze pipe 1, spring 1), Creative Energy Cells 4, copper cables 11, Ley Pylons 5 (4 in the circle, 1 at the post), Circle Anchor 1, Warding Stones 8, Courier Post 1, hoppers 3, containers 5, Peepo 1, Clockwork Porter 1, Verdant Beds 10 (9 planted: Mendvetch 3, Sunpetal 2, Dewmoss 2, Gloamcap 2; the Verdant Heart's 1), Verdant Heart 1, Mulch Maw 1, Gleaner 1, water sources 3
level at the end: players: 1, entities: 3,3,3,81,81,0,0 [jugcraft:clockwork_porter:1,peepo_companion:peepo:1,minecraft:player:1], block_entities: 19 [jugcraft:ley_pylon:5,jugcraft:creative_energy_cell:4,minecraft:hopper:3,jugcraft:gleaner:1,jugcraft:circle_anchor:1], block_ticks: 0, fluid_ticks: 0, chunk_source: Chunks[S] W: 961 E: 3,3,3,81,81,0,0
active recipes: Alembic Crucible formula "water 1; add jugcraft:mendvetch jugcraft:raw; add jugcraft:mendvetch jugcraft:raw; stir warm; stir warm; stir warm; stir warm" (the Stillroot Poultice); rite jugcraft:root_and_rivet
work while measured: poultices 21, Root and Rivet 37 (interrupted 0), Peepo brought 0 Mendvetch, porter 44 in 22 requests, Gleaner harvests 2 {mendvetch=1, sunpetal=0, dewmoss=1, gloamcap=0}, Heart searches that drew 0 (of 150 pulses), Maw ate 150, crucible waits {missing_ingredient=26, none=34}, stand lit at 29 of 60 window ends
inventory: Mendvetch 384 = supplies 330 + Peepo 0 + carried 0 + porter 0 + buffer 1 + used 53; bowls 128 = left 102 + used 26; rite plates 47, copper 188, Mendvetch 188 = 46 foundations (4 copper, 4 Mendvetch each) + held [1 jugcraft:thallite_plate, 4 minecraft:copper_ingot, 4 jugcraft:mendvetch, 0 minecraft:air, 0 minecraft:air, 0 minecraft:air]; Maw ate 187 of 256 chaff; open requests 0; ground and ledger checked; problems none
network to the one test player (uncompressed, unframed): baseline 310 packets 3.1 KiB (0.01 KiB/s), workshop 7773 packets 144.9 KiB (0.48 KiB/s); unsized 0 and 0; signs in reach 0 and 864 (58.8 KiB, an upper bound: the level's budget may drop some)
network, workshop, by kind: block_entity_data jugcraft:circle_anchor 150/31.6 KiB, minecraft:system_chat 185/23.6 KiB, minecraft:move_entity_pos 1883/18.4 KiB, minecraft:set_entity_motion 1706/13.3 KiB, block_entity_data jugcraft:crucible 191/12.2 KiB, minecraft:move_entity_pos_rot 935/11.0 KiB, block_entity_data jugcraft:verdant_bed 89/9.9 KiB, minecraft:sound 245/7.7 KiB, minecraft:block_update 512/6.0 KiB, minecraft:rotate_head 1184/3.5 KiB, minecraft:set_entity_data 316/3.3 KiB, minecraft:set_time 300/2.9 KiB
network not measured: Jade's and the encyclopedia's replies (they answer a client's request); compression and framing
budget (C-P19 in C-P67's form, proposed; the owner decides at merge review): median +1.0 ms: within; p95 +2.5 ms: within; retained +4 MiB beyond the baseline's: within. Asserted here: +5.0, +12.5 ms, +64 MiB
===== end =====
```

Read with C-P67's proposed budget (median +1.0 ms, p95 +2.5 ms, retained +4 MiB beyond the baseline's): within, on this
runner, in this one run. The card names its runner and claims nothing for other hardware. The Peepo brought no
Mendvetch while measured; the porter brought it all (Risks, Load measurement).

### Client evidence (run 38774859294)

What was viewed for these records: shots extracted from client shard 4's log of run 38774859294 at the log's 480×270
preview size (the full-size images are the run's `client-screenshots-<shard>` artifacts, not downloaded). A screenshot
shows presentation; the tests' checks are the evidence of behaviour.

| Item | Shot (class) | Seen |
|---|---|---|
| The whole workshop at the ordinary scale | `jugcraft_concordance_workshop` (`ConcordanceWorkshopClientGameTests`) | Viewed: the whole workshop, small at that distance; the stand and crucible cluster on the left, the circle in the centre, the beds on the right |
| The stand lit | `jugcraft_concordance_stand_lit` (same) | Viewed: the crucible on the stand, its copper coil lit, the pump and a Creative Energy Cell beside it |
| The stand dark, its crucible waiting | `jugcraft_concordance_stand_dark` (same) | Not viewed; its shard passed |
| The crucible's Inspect page on its stand | `jugcraft_encyclopedia_inspect_crucible_stand` (same) | Viewed. It reads: "Alembic Crucible at 7, -59, 11 / 62 degrees (Warm) / Crucible Stand beneath heats it towards 65 degrees / 2 of 6 parts / Its tank holds 3 of 3 parts of water / Formula: step 3 of 9: Mendvetch, as it comes / Waiting to go in: 3 Mendvetch / Bottles and bowls: 2 Bowl" |
| Root and Rivet under way: spent apart from held, its stage table | `jugcraft_encyclopedia_inspect_root_and_rivet`, `_scale4` (same) | Not viewed; its shard passed |
| Root and Rivet in Jade | `jugcraft_concordance_anchor_root_and_rivet_jade` (same) | Viewed: the circle with its anchor; no Jade box is visible at the 480×270 size. The test's Jade snapshot assertion passed |
| The foundation's icon | `jugcraft_rooted_thallite_foundation` (`EncyclopediaClientGameTests`) | Not viewed; its shard passed. The icon itself was viewed separately: a green Thallite plate with copper rivets and roots beneath; the roots may read as legs. The owner did not ask for it to be redrawn at the checkpoint (C-D7) |
| The crucible's Inspect pages (unit 1) | `jugcraft_encyclopedia_inspect_crucible`, `_scale4`, `_inspect_poultice`, `jugcraft_concordance_crucible_jade` (`EncyclopediaClientGameTests`) | Not viewed; its shard passed |
| The anchor's readiness (unit 2) | `jugcraft_encyclopedia_inspect_readiness`, `jugcraft_concordance_anchor_gathering_jade` (same) | Not viewed; its shard passed |
| The stand's workshop (unit 3) | `jugcraft_concordance_crucible_stand_workshop`, `jugcraft_concordance_crucible_stand_jade` (same) | Not viewed; its shard passed |
| A bed's Inspect page and Jade on a crop (unit 4) | `jugcraft_encyclopedia_inspect_bed`, `jugcraft_concordance_ripe_crop_jade` (same) | Not viewed; its shard passed |

The frame-time line `ConcordanceWorkshopClientGameTests` logs (100 frames, Mesa's software renderer) was not read for
these records.

### Not run

A person playing any of it, by hand, or looking at the shots not viewed above; two clients, and a dedicated multiplayer
playtest; a client without the optional integrations; client frame times on a player's hardware; Jade's and the
encyclopedia's reply traffic; JEI's pages in a client; `runServer`; the larger workload (H3, deferred).

### Known defects and limits

- The Gleaner's starved pulse is over C-P21's line (C-P68): 333 µs against 250 µs in run 38774859294 (one run, one
  shared runner). A change is proposed (caching `Garden.nearest`'s order, or a vertical reach of ±1), not applied.
- In the load card the Peepo brought no Mendvetch while measured; why is not known from its lines.
- Bees step these crops without strain, niche or graft clearing (unit 4's risks).
- An offline setter's unclaimed request holds its post's queue (C-P53).
- Jade's overlay has not been seen: the one Jade shot viewed shows no Jade box at the preview's size.
- The codex pages' lengths have not been seen in a client.
- Outside Chunk C: main's `PeepoPopulationClientTests` cow-breeding flake (#357, merged in main as `40381834a`) failed
  client shard 1 in runs 38684732031 and 38774859294, each passing on its re-run.
- The other as-built limits of units 1 to 5 are under Risks above.

### Record fields (CLAUDE.md)

Every row is at the Practitioner stage. The tests named passed in CI (above).

| Feature | Input producer | Output consumer | Costs | Unlocks | Failure behaviour | Test evidence |
|---|---|---|---|---|---|---|
| Crucible diagnostics (C-01) | the crucible's state | the reader, Peepo's status, the stand | none; deeper glass reading needs research and the glass | nothing | a refused formula stays set and drives nothing; an unreadable one is kept as text | C-01's alchemy tests, `theInspectPageReadsACrucibleByResearch`, `EncyclopediaClientGameTests` |
| Stillroot Poultice (C-02) | Mendvetch (a bed's harvest, or wheat seeds, bone meal and an amethyst shard for 2), water, a bowl | healing; a friend in a lair fight (credit) | 2 Mendvetch, a part of water, a bowl (returned), 4 warm stirs | nothing | a dirty batch heals less, predicted | the product tests and `LairEncounterGameTests`' credit tests |
| Kiln Draught (C-02) | magma cream (a Tyrant Scale gives 2) or 2 sugar | the Kiln's slag, his flame | 1 cream or 2 sugar, a part, a bottle, 2 hot stirs | nothing | the wrong heat waits | `aKilnDraughtCarriesItsDrinkerAcrossTheSlag`, `theCinderKilnsRiteNamesTheKilnDraught` |
| Readiness (C-03) | the circle | the reader, the start | none | nothing | warns for a run short; marks faults | C-03's tests in `ConcordanceRitualGameTests`, `theInspectPageShowsARitesReadinessAndPayments` |
| Root and Rivet (C-04) | a Thallite plate (Metal Press or 2 Thallite ingots; `machines` and `thallite` switches), 4 copper, 4 Mendvetch; Ley from Radiance, Verdance or JE | the stand's recipe; a Strata foundation elective | 6 Focus; 32 Ley Charge (48 Radiance, 48 Verdance or 32,000 JE); 8 s | the stand | an interruption keeps the offerings, in one line; backlash 4 | C-04's tests in `ConcordanceRitualGameTests`, `theFoundationIsNeverWeighed` (now `theFoundationIsNeverFormed`) |
| Crucible Stand (C-05) | the foundation, 6 iron, 2 copper; JE | the crucible's heat | 8, 16 or 32 JE a tick only for a stir; a 640 JE store | nothing | dark while the crucible waits on anything else; its JE lost when broken | the stand's alchemy tests, `aStandTurnsElectricityIntoHeatOnly`, the persistence test, `ConcordanceWorkshopClientGameTests` |
| Two carriers (C-06) | the crucible's shortage | its buffer | a porter trip (2 Ley Charge, 1 integrity); 16 requests a setter | nothing | full keeps cargo aboard; unloaded waits; turned back when cleared | the 10 tests unit 3 added to `ConcordanceLogisticsGameTests`, the persistence test, the Peepo checks |
| Habitat (C-07) | a bed's sample | the reader | a canteen sip | nothing | a dormant bed is not read; a gone plant's verdict is forgotten | `ConcordanceGardenGameTests`, `theInspectPageReadsABedAndACrop`, `gardenInspection` |
| Mixed bed and strain (C-08) | Dewmoss with Gloamcap; harvests | pace; the disturbance fit | strain +1 a harvest, at most 3 | nothing | never kills, never reads industry | `ConcordanceGardenGameTests` |
| Dewlit (C-09) | a Dewmoss on a Sunpetal crop | the Sunpetal's niche on that bed | 1 Dewmoss | nothing | refusals consume the use; another plant clears it | `ConcordanceGardenGameTests`, `theInspectPageNamesAGraft` |
| The measured workshop (C-10) | the workshop of C-01 to C-09 | the owner's merge review (C-P67) | none in play | nothing | — | `loadCard`, `gleanersHeartSearch`, the journey's two tests, `ConcordanceWorkshopClientGameTests` |

### The handoff (10:133-146)

- **Base and final:** `e079ad4a3` → `bb856f873`, then this records commit. Tickets C-01 to C-10; the contracts changed
  are under Interfaces expected to change and each ticket's As built notes.
- **APIs reused:** `CompanionLogistics.Port` (`CrucibleCompanion`), `CourierLedger.file`, `Couriers.nearestPost`,
  `EnergyStorage.SIDED`, Fabric Transfer fluid storage, `RitualMachine` (a Transform result), `Sampler` and
  `SampleBudget`, `Authority` and `RateGate`, `Signs`, `ComposeText`, `InspectPage` and `PageWriter`, the Jade provider,
  the JEI plugin, `owner_art.py`.
- **Schemas, defaults and migration:** product and graft JSON; the organism's `neighbours` and a property's `fouling`
  fields; optional saved keys, stamps still 1; older saves load (`anOlderBedLoadsWithoutItsNewKeys` and the persistence
  tests); no migration.
- **Generators and provenance:** `tools/concordance_alchemy.py`, `concordance_ecology.py`, `concordance_rituals.py`,
  `concordance_economy.py`, `concordance_logistics.py`, `concordance_diagnostics.py`, `acquisition.py`,
  `concordance_journey.py`. The stand's art through `owner_art.py --provenance alchemy`; the foundation's icon drawn
  fresh.
- **Commands and results:** the CI runs above; the local checks in each unit's row of the Units table; screenshots in
  each run's `client-screenshots-<shard>` artifacts; the load card's lines in its job's log and notice.
- **Decisions:** C-D0, C-D0b and C-D1 to C-D3 accepted by the owner, and at the checkpoint on 23 October 2026 C-D5 to
  C-D8 (hands-on and automation as built; Gloam Ink next; the stand's look fits, and the foundation's icon, which the
  owner did not ask to be redrawn, stays; two stacked pull requests); C-D4 and C-P1 to C-P70 Proposed. The load budget
  (C-P19 in C-P67's form) and the workflow change (`bfb08770b`, the manual-only `load_card` input and job) remain for
  the owner's review in Chunk C's pull request.
- **Next smallest complete outcome:** Gloam Ink (C-D6; when it is built is not yet decided): one formula from a mixed
  bed's Gloamcap with one existing consumer (Research Notes).

## Deferrals

- **Rimewash** (a Winter Brine salve holding a crucible's band): it would copy Preserve's Proposed hold (B-P13, B-P35;
  Chunk B's contract, lines 1413 and 1435) and preserve a band, not a reagent.
- **The reagent-preservation chain** (06:87-96), **Stillwater Keeping** and the Reagent Cabinet (03:100; 06:119): no
  reagent freshness exists to preserve, Stillwater needs a new result kind, and Preserve "never wraps an inventory"
  (Chunk B's contract, line 1064). Brine for opened preserve jars likewise.
- **Mending Salve** as the delivery plan framed it (a renamed Regeneration salve): superseded by the Stillroot Poultice.
- The other products of 03:31-39: Dewglass Wash, Hearth Lacquer, Gloam Ink, Threadwax, Assayer's Flux.
- A dedicated alchemy notebook or GuiLib workspace; the ritual planner screen with client ghosts (08:49-59); a per-player
  formula book.
- Graft cuttings as items; a visible graft tint (it would need blockstates and art).
- Season shelter and harvest choice (03:75, 03:78).
- Construct body upgrades (06:85; 03:195): outside C1 to C4, and no chunk of chapter 9 names them.
- Poultice credit in the Mire Hag's and the Crypt Lich's fights (they are not on the encounter credit).
- The agreed larger workload (H3, 09:282).

## Validation plan

| Unit | Server game tests | Client game tests | Checks | Save tests |
|---|---|---|---|---|
| 0 | `./gradlew build` on the merge: run 38617471274, `mod` passed on its one re-run | the five client shards passed in that run (attempt 1; which classes `choose client tests` picked was not read) | as the merge commit records: `check_mod_data`, `check_repository`, `owner_art --check`, `concordance_journey`, `check_icon_maps`, the World Designer tests | — |
| 1 | `ConcordanceAlchemyGameTests`, `ConcordanceAssayGameTests`, `LairEncounterGameTests`, `EncyclopediaGameTests`, `ConcordancePersistenceGameTests` | `EncyclopediaClientGameTests` (crucible pages, two scales; Jade) | `check_alchemy` (the fouling mirror), `check_winter_brine`, `check_workshop_products` (new), the Jade-snapshot lock, the pure list, the client-read rule; the build without optional integrations | restart before output; an unreadable saved program |
| 2 | `ConcordanceRitualGameTests`, `ConcordanceAssayGameTests`, `ConcordanceProgressionGameTests`, `EncyclopediaGameTests` | the anchor's Inspect page; Jade on an anchor | `check_rituals` and the Ley-capacity rule; the encyclopedia's forbidden list; `check_acquisition`; the build without optional integrations | a mid-run lapse |
| 3 | `ConcordanceAlchemyGameTests`, `ConcordanceEconomyGameTests`, `ConcordanceLogisticsGameTests`, `ConcordancePersistenceGameTests`, the cable check | `PeepoIntegrationClientTests`, `CompanionAutomationChecks`; a workshop shot | `check_alchemy` (the stand's targets), `check_economy`, `owner_art.py --check` if art is imported; the build without optional integrations | the stand; the crucible's keys present and absent; the ledger's codec |
| 4 | `ConcordanceGardenGameTests`, `EncyclopediaGameTests`, `ConcordancePersistenceGameTests`; re-run the Gleaner, Spire-field, Peepo regrowth and journey tests (the Crop Harvester's strain test runs only when `JUGCRAFT_TEST_FOCUS` is unset or names `machines`) | a bed's Inspect page; Jade on a crop | `check_ecology` (neighbours, strain, graft), the compost page; the build without optional integrations | an older bed; strain and graft through a save |
| 5 | `ConcordanceJourneyGameTests` (`theWorkshopsRecipesLoad`, `theLivingWorkshopFromAManualStart`), `ConcordanceWorkloadGameTests.gleanersHeartSearch` and, opted in, `ConcordanceWorkshopLoadGameTests.loadCard`: run 38774859294 on `bb856f873`, `mod` passed ("All 1836 required tests passed"; the card passing at once there), `optional integrations absent` passed, and the "workshop load card" job passed with the card measuring ("All 1 required tests passed") | `ConcordanceWorkshopClientGameTests` (new; shard 4 of run 38774859294, passed); every client shard of that run passed, shard 1 on its re-run | every check, `check_load_card` and `check_workshop_client_evidence` (new); for the records commit `check_mod_data`, `check_repository`, `concordance_journey --check` and `git diff --check`, run and passed | — |

Every unit also runs `tools/check_mod_data.py`, `scripts/check_repository.py`, `scripts/tests` and the build with its
server game tests, and its CI by workflow_dispatch of the Build workflow with every client test class, as Chunk B did.

**Evidence rules** (10:5-22):
- Each command is reported on its own as run and passed, run and failed, or not run. CI on a commit counts for that
  commit only.
- A server game test with several test players is not a party of people. One client on a dedicated server is not two
  clients; the two-client dedicated-server session and a human journey stay not run unless two accounts are available
  (09:20).
- A CI runner's timings are not a player's hardware: the load card names the runner and claims nothing for other
  machines. A software-rendered frame time is that renderer's.
- A screenshot shows presentation, not balance; nothing is called "balanced" without a human playtest.
- Numbers from the generator's model or arithmetic stay labelled so until a game test measures them.

## Decisions

Chunk A's and Chunk B's decisions are unchanged: B-D0 to B-D3 are the owner's, and every B-P stays Proposed (Chunk B's
contract, lines 1390-1462). At most three decisions go to the owner at Chunk C's checkpoint (09:18; 10:113-116).

| ID | Date | Decision | Status | Affects |
|---|---|---|---|---|
| C-D0 | 14 October 2026 | "ok begin chunk c": Chunk C is the next chunk. It answers Chunk B's checkpoint question on the next school only; movement feel, B-P59 and B-P60 stay open, and every B-P decision stays Proposed | Accepted (owner) | All of Chunk C |
| C-D0b | 14 October 2026 | "New branch, main merged (Recommended)": Chunk C is developed on `claude/chunk-c-living-workshop`, started from Chunk B's head `ec06f16e0` with main `4d38e9712` (#356, #358) merged in (merge commit `e079ad4a3`; its CI run 38617471274 passed every job, the `mod` job on its one re-run after a first-attempt failure the session read as the known nuclear-plant site-clash flake from main; the log was not reachable to confirm the cause). The merge's two resolutions: the gametest `fabric.mod.json` is main's two-space file plus Chunk B's four classes; for `CHANGELOG.md`, #356 had cut main's file to its own 17-line entry, and the merge keeps the full history and adds #356's section (main itself is still short; a separate task was suggested to restore it) | Accepted (owner) | C-00, every unit |
| C-D1 | 14 October 2026 | "Poultice + Kiln Draught (Recommended)": (1) the Stillroot Poultice, a Mendvetch healing salve from the Alembic Crucible; a dirty batch gives less useful healing, predicted before brewing (this needs a real Mixture/Outcome rule, since today contamination only adds harmful effects and never lowers yield: designed in the generator and Java, mirrored and audited); healing another player with it at a lair boss earns a support share (Encounter credit for realised Regeneration healing from a poultice applied by another player, bounded so it cannot be farmed); (2) the Kiln Draught, a Fire Resistance draught (existing ingredients such as magma cream or sugar, per the alchemy survey) that lets a player wade the Cinder Kiln's slag (`MoltenSlagBlock` already honours Fire Resistance), named in the Cinder Kiln's dossier as a preparation fact, as Winter Brine is for the Glacier Hall; the Tyrant Scale to magma cream route audited as a boss-to-ingredient edge | Accepted (owner) | C-01, C-02, C-10 |
| C-D2 | 14 October 2026 | "Root and Rivet (Recommended)": turns a Thallite plate into a Rooted Thallite Foundation, the base the Crucible Stand is built on; uses the existing ritual engine (Transform result) and pylon economics, with a declared stage and payment table | Accepted (owner) | C-03, C-04, C-05 |
| C-D3 | 14 October 2026 | "Crucible Stand (Recommended)": under the Alembic Crucible, on the rooted foundation; turns JE into the heat the running formula's next stir needs (starting rates about 8, 16 and 32 JE/t for warm, hot and searing), while an Electric Pump pipes water into the crucible's tank; manual heat sources still work | Accepted (owner) | C-05, C-06, C-10 |
| C-D4 | 14 October 2026 | Both carriers, one demand: the crucible's formula shortage, served by a Peepo through the existing `CrucibleCompanion` port and by a Clockwork Porter through a courier request the crucible files (09:136 names both `MachineCompanionPort` and `CourierLedger`). Not asked; the session's default | Proposed | C-06 |
| C-D5 | 23 October 2026 | At the checkpoint, the balance of hands-on preparation and automation (09:144): keep it as built. The campfire crucible and carrying by hand stay a complete route; the Crucible Stand and the couriers stay optional conveniences paid in JE (the stand's heat; a porter's Ley Charge, which electricity can give) | Accepted (owner) | C-05, C-06, C-10 |
| C-D6 | 23 October 2026 | At the checkpoint, the next product family (09:144): Gloam Ink (03:27-37: Gloamcap and an approved Hollow carrier, by covered extraction, for research notes, safe links and ward inscriptions; excess contamination yields an unusable sludge, recoverable through a mundane route). (A note, not the owner's answer: when it is built is not yet decided.) | Accepted (owner) | the next work (Deferrals; Where to resume) |
| C-D7 | 23 October 2026 | At the checkpoint, the workshop's look (09:144): the Crucible Stand's look fits. The Rooted Thallite Foundation's icon was not asked to be redrawn and stays | Accepted (owner) | C-04, C-05 |
| C-D8 | 23 October 2026 | The review route (Open pull requests and the review route): Chunks B and C are reviewed as two stacked pull requests, Chunk B's against main and Chunk C's against Chunk B's branch | Accepted (owner) | C-00, every unit |
| C-P1 | 14 October 2026 | A formula's identity is its canonical text (the Peepo plan id hashes it); the rules' revision is `ConcordanceData.revision()`, process-local, so not 03:46's stable version (a recorded deviation); no new formula save key; an incompatibility is re-derived after a load or reload; an unreadable saved program is kept as text and reported. As built in unit 1: `incompatibility()` replays the formula once per revision and after a load, and names the refused step; an unreadable program is kept under `program` (see C-P23) | Proposed | C-01 |
| C-P2 | 14 October 2026 | Products are generated data; a brew matching one is named on the stack with vanilla's item name, recognised by what it carries, never its name; Winter Brine is named the same way. Alternative: JEI and codex names only. As built in unit 1, for the Stillroot Poultice, the Kiln Draught and Winter Brine; how a stack is matched is C-P29 | Proposed | C-02 |
| C-P3 | 14 October 2026 | A crucible's Inspect page: spoon level and an ungated risk line (a harmful outcome or a searing step) for anyone; glass level with the Alembic Arts understood and an Assay Glass carried; the outcome and replay preview for a master; within the player's block reach, not 16 blocks. As built in unit 1, refined by C-P25 (the risk line's words) and C-P26 (the reach's allowance, and the glass carried at every level above the spoon's) | Proposed | C-01 |
| C-P4 | 14 October 2026 | Jade keeps showing a running formula's next operation to anyone, as today, beside JEI's "formulas are never shown" (an inconsistency noted, not changed). As built in unit 1; the line reads "Next step: ..." (C-P27) | Proposed | C-01 |
| C-P5 | 14 October 2026 | Every state-changing crucible use (setting or clearing a formula, water, adds, stirs, bottling) asks `Authority.mayChange`; spoon, glass and empty-hand reading stay open. As built in unit 1, with recording onto a blank formula left open (C-P24) | Proposed | C-01, C-06 |
| C-P6 | 14 October 2026 | One shared diagnostic vocabulary in a pure `concordance/diagnostic` package for the crucible, the stand, ritual readiness and crucible filing; `CompanionStatus` and the porter's `Status` map from it at their edges. As built in unit 1 for the crucible: `Code` (19 codes in four kinds) and `Diagnostic`; `CrucibleCompanion.status` maps from it; the ids and kinds are C-P22 | Proposed | C-01, C-03, C-05, C-06 |
| C-P7 | 14 October 2026 | The yield rule: a property may declare `fouling`; Verdance alone does, 4.0 (each unit of contaminant a part takes 4 units of Verdance before level and duration), a starting value; a reason names the foulers. As built in unit 1; `check_alchemy` holds the order (taken, subtracted, then judged) and the generator's mirror | Proposed | C-02 |
| C-P8 | 14 October 2026 | The poultice's credit: on each tick Regeneration heals, the health a patient gains, at most 1, credited through `Encounter.healed` to the player who applied the poultice, while it lasts; transient; only a poultice (a Frostward or any other salve earns nothing); no mixin. As built in unit 1 as C-P30 refines it, credited through `creditHealing` (`healed`'s old body) | Proposed | C-02 |
| C-P9 | 14 October 2026 | The Kiln Draught is a draught carrying Fire Resistance and nothing harmful; its dossier fact is text and icons in the Cinder Kiln's rite section with a codex link at the Alembic Arts observed and the Salamander Charm as the lasting answer (the Charm alone when the Concordance is off or Ember no longer brews Fire Resistance); the Tyrant Scale → magma cream edge (C-D1's audit) is recorded as a reward route, not a loop: the draught makes no Kiln Seal or scale, and magma cream has vanilla sources. As built in unit 1, with C-P31 (his flame) and C-P32 (the Grave Dust route) | Proposed | C-02 |
| C-P10 | 14 October 2026 | A run-level Ley shortfall warns and does not refuse; the start rule (one step's Ley) is unchanged; signs mark faults on the empty-hand, chat and refusal paths only. As built in unit 2: the warning names the step the run would break off at (since the unit review, "unless it is refilled meanwhile": a pylon on JE gains more a step than any rite draws); `check_rituals` holds the one-step start; how signs mark is C-P35 | Proposed | C-03 |
| C-P11 | 14 October 2026 | Root and Rivet is taught by Circle Lore understood; offerings a Thallite plate, 4 copper ingots, 4 Mendvetch; 1 participant, 6 Focus, 4 steps of 2 Ley a pylon, any light, backlash 4 (starting values). As built in unit 2, with these numbers; the foundation it makes is C-P40 | Proposed | C-04 |
| C-P12 | 14 October 2026 | The stand: targets warm 65, hot 120, searing 175 (starting values) at C-D3's rates; it follows the next stir's band only while the formula can proceed and idles otherwise; a Preserve hold wins; with no formula an empty-hand use by someone who may change it cycles its setting; crafted from the foundation, iron and copper; no wear. As built in unit 3, with these targets and rates: it pays only for heat a stir can take (C-P43), on five faces (C-P44), from its own store (C-P45); the setting is C-P46 and the recipe C-P47 | Proposed | C-05 |
| C-P13 | 14 October 2026 | Crucible filing: the requester is the formula's setter; one open request; filed on entering the ingredient wait, with backoff after no post or `TOO_MANY` (200 ticks doubling to 1200, starting values); cancelled when the formula is cleared, replaced, set by another or incompatible; the Peepo port subtracts the open remainder. As built in unit 3, with these values: filed on an ingredient wait while no request is open and no backoff runs (once a wait, as filing on entry would); also turned back when no longer what it lacks (C-P51); the remainder counted only while a courier holds the request (C-P50); the quota's words and the backoff's reset are C-P52, who files and when C-P53 | Proposed | C-06 |
| C-P14 | 14 October 2026 | Light is shown as sky and lamps; niches are unchanged. As built in unit 4: the Inspect page gives the light at the plant as sky and lamps and judges the greater, as growth does (C-P54) | Proposed | C-07 |
| C-P15 | 14 October 2026 | The canteen waters Verdant Beds and the compost JEI page is wired, rather than deleting their claims. As built in unit 4: the canteen pours on beds through the bed's own rule (C-P56), and JEI shows the compost pages (C-P57) | Proposed | C-07 |
| C-P16 | 14 October 2026 | The mixed bed: Dewmoss and Gloamcap, +25% pace each way within the 5×5 (a starting value); no Mendvetch pairing, no fixer beneficiary; named "mixed bed". As built in unit 4: an organism's optional `neighbours`; Dewmoss and Gloamcap name each other at 25 (C-P58) | Proposed | C-08 |
| C-P17 | 14 October 2026 | Harvest strain: +1 a seen harvest, at most 3; it only ever worsens the disturbance fit (alternative: plain disturbance, which would let a harvested Gloamcap bed grow with no magic block); eased by rest (one a bed random tick), explicit compost points (bone meal, fertilizer, the Maw's mulch) and a Greenwarden's tending (once a 600 ticks a bed), never by nitrogen fixing; never kills. As built in unit 4: counted and shown as C-P59 says; rest only while no crop grows on the bed (C-P60); compost includes bone meal and fertilizer used plainly on a growing crop, and tending asks Authority (C-P61) | Proposed | C-08 |
| C-P18 | 14 October 2026 | One graft, Dewlit (Sunpetal host, Dewmoss donor): light `[9,11,15,15]`, moisture `[4,5,7,7]` (starting values); Verdant Husbandry understood; a plain use, not sneaking; bed-bound, cuttings deferred; produce, cost and alchemy unchanged. As built in unit 4, with C-P62 to C-P65 | Proposed | C-09 |
| C-P19 | 14 October 2026 | The load budget, agreed before merge: the workshop adds at most 1.0 ms median and 2.5 ms p95 server tick, with a flat memory trend over 6,000 ticks, on the stated runner; recorded, not claimed for other hardware. As built in unit 5, in C-P67's form: reported by the load card, not asserted; measured once, in run 38774859294 on `bb856f873`: +0.240 ms median and +0.550 ms p95, net, and retained heap +0 MiB beyond the baseline's change, within it on that runner. The owner decides it at merge review | Proposed | C-10 |
| C-P20 | 14 October 2026 | The load runner: a server game test of its own, opted in so ordinary CI runs are unchanged; CI's software-rendered frame times recorded as that renderer's only. As built in unit 5 as C-P66 (the card) and C-P70 (the client class, whose frame times are Mesa's) | Proposed | C-10 |
| C-P21 | 14 October 2026 | The Gleaner's Heart search (729 positions) is measured in C-10 and the record corrected; its radius changes only if the measurement says so. As built in unit 4: the ecology record now gives the Maw 147, the Gleaner 147 for crops and 729 for Hearts; the measurement stays in C-10. As built in unit 5 as C-P68: measured in run 38774859294's `mod` job, a starved pulse took 333 µs against the 250 µs line (over; the position list alone 304 µs), so a change is proposed, not applied, and `HEART_REACH` stays 4 | Proposed | C-07, C-10 |
| C-P22 | 14 October 2026 | Diagnostic ids use 03:9's own wording, then Chunk C's (`needs_water`, `needs_container`, `held`, `incompatible`, `courier_quota`, `output_differs`, `needs_emptying`, `unreadable`). Each code has a kind that sets the sign shown when it begins: a shortage or a blockage a WANT, a fault a PERIL with the breaking sound, a condition (heat, a hold) none. Codes no unit raises yet have words without parameters until their unit fixes both. The crucible's formerly silent wait before a repeat now shows a WANT sign once | Proposed | C-01, C-03, C-05, C-06 |
| C-P23 | 14 October 2026 | A formula the rules refuse, or a saved one that no longer reads, drives nothing: no step and no automated bottle, even of a finished batch. Bottling by hand stays open; clearing stays a sneak-use by someone who may change the crucible | Proposed | C-01 |
| C-P24 | 14 October 2026 | Recording a blank formula stays open to anyone with the Alembic Arts (it changes nothing in the crucible); every other change asks Authority (within C-P5) | Proposed | C-01 |
| C-P25 | 14 October 2026 | The ungated risk line names the harmful statuses a dose would carry and the formula's first searing step, rather than "would harm" alone. Since the unit review, it also names the harm (and a searing stir) of a mixture that is not the formula's own batch (there before it was set, or under a formula the rules refuse), and a status is harmful when the server judges it so as well as when its property says | Proposed | C-01 |
| C-P26 | 14 October 2026 | The Inspect page reads a crucible within the reader's block reach plus the server's 1.0 use allowance; every level beyond the spoon's needs an Assay Glass carried, a master's included, as the glass itself does | Proposed | C-01 |
| C-P27 | 14 October 2026 | Jade carries the reason as `Diagnostic.encode()` text (code and parameters, at most 1024 characters, else the code alone) and the tank as whole parts. Jade's next step stays canonical text (C-P4) while the Inspect page words it | Proposed | C-01 |
| C-P28 | 14 October 2026 | A test that stands for a reload swaps the rules only through a helper that restores them in a `finally` within the same tick, not in a batch of its own | Proposed | C-01, C-10 |
| C-P29 | 14 October 2026 | A product is any brew of its form carrying its status and nothing harmful (judged by the server); a sweet-berry Regeneration salve is therefore a Stillroot Poultice; a brew matching two products stays unnamed | Proposed | C-02 |
| C-P30 | 14 October 2026 | Poultice credit is read from Regeneration's own time left at each tick's end, each blow and each Concordance heal, at most one health a heal. One note per patient; a later poultice that takes replaces it; it ends when the Regeneration is no longer the poultice's or the patient leaves the fight. The helper is credited wherever they stand, as a ward's caster is | Proposed | C-02 |
| C-P31 | 14 October 2026 | The Kiln Draught's dossier fact says it also turns the Tyrant's flame (breath, waves, patches), as Fire Resistance already does | Proposed | C-02 |
| C-P32 | 14 October 2026 | Grave Dust → bone meal → Mendvetch is a second declared reward route; boss routes are audited by recipes alone, not through the Assayer's Scale. Since the merge with main `f904df3fd` (`b234b8967`), Gossamer Silk and Salvaged Coil are declared too, as two tool-only routes to Mendvetch and sugar (through a knife a cutting recipe keeps, which `_made_from` counts as an input) | Proposed | C-02 |
| C-P33 | 14 October 2026 | Rites use the shared codes. An offering short is `missing_ingredient`, its words made general for crucible and anchor. `unavailable_power` takes (what, held, needed) as lines and `incomplete_pattern` takes (structure, faults to mend); both are first raised here. `not_lent`, `needs_focus`, `too_bright`, `too_far` and `participants` are appended after unit 1's codes | Proposed | C-03, C-05 |
| C-P34 | 14 October 2026 | Readiness judges its reader as the rite's leader. Nothing offered chooses no rite. A fully matched rite the reader has not learned is never named on the Inspect page or in Jade; the start's refusal still names it, as before. Before a start, the other participants appear as the stage table's gather row; during a gathering, as `participants`. As built in unit 2, where a joined participant who has walked away is not listed while the rite runs: the next step's own check finds them | Proposed | C-03 |
| C-P35 | 14 October 2026 | An empty-hand or command reading marks every faulted part where it stands, at most once a second a player: shape faults with their interruption's sign, and a channel short of a step or unlent for the reader with WANT (since the unit review, not one only warned short of the run). A refused start marks the refusing check's faults with that refusal's sign. The Inspect page and Jade never mark | Proposed | C-03 |
| C-P36 | 14 October 2026 | Fault positions read "x, y, z (dx, dy, dz from the anchor)" in chat, the command and the Inspect page; Jade keeps "x,y,z" | Proposed | C-03 |
| C-P37 | 14 October 2026 | An interruption is told in one line: why, where the offerings are, and the Focus and Ley Charge the attempt lost, every participant's Focus together (without the loss when the rules no longer know the rite). It is kept in memory for the Inspect page until the next start or a load, never saved. TAMPERED's line under-counts its last draw | Proposed | C-03 |
| C-P38 | 14 October 2026 | Jade on an anchor shows the participants it needs, who is channelling, the first three offerings and "+n", and, while gathering or channelling, the Focus and Ley Charge spent and the offerings held. Never the rite, anyone's Focus held or research | Proposed | C-03 |
| C-P39 | 14 October 2026 | Every rite's stage and payment table is worded from its data on the Inspect page, for a reader who knows the rite; the codex states Root and Rivet's in its own words (`_payment_text`, for a transforming rite; C-04) | Proposed | C-03, C-04 |
| C-P40 | 14 October 2026 | The Rooted Thallite Foundation is a plain described item in the Ingredients tab. The Assayer's Scale never weighs it, nothing turns it back, and the economy models it as a sink of 32 Ley Charge and 6 Focus a run. Its icon is drawn fresh, because nothing in the owner's library fitted, and awaits the owner's review. Since the merge with main `f904df3fd` (`b234b8967` and the commit after it): main's Scale values every default item, so the foundation can be weighed and dissolved at its catalogued worth (the one-essence fallback while `values.json` has no record for it), but it is no tablet output, so it is never formed; `theFoundationIsNeverWeighed` is renamed `theFoundationIsNeverFormed`, and the codex says the Scale will not form it. At the checkpoint the owner did not ask for the icon to be redrawn (C-D7) | Proposed | C-04, C-05 |
| C-P41 | 14 October 2026 | A rite's offerings may need feature switches beside the Concordance's. The generator states them (`"switches"`) and `check_rituals` holds that statement to the recipes, one step down, an offering that is a tag through its members (since the unit review, a machine recipe's `{"ingredient", "count"}` entries followed too, and a condition with `"or"` alternatives counted as needing none of its switches alone). Concordance data carries no load condition. Any recipe that makes an offering counts as its way to make it | Proposed | C-04 |
| C-P42 | 14 October 2026 | The codex's first `entry://` link (Stratawrighting's Understood page to Root and Rivet). Every codex link is audited to an entry on disk (since the unit review, its `@anchor` to a page id and its `#n` to a page of the entry), since a broken link stops Modonomicon opening the book (as read from its source by the C-04 design; not seen in a client) | Proposed | C-04 |
| C-P43 | 15 October 2026 | A Crucible Stand pays its band's rate on a tick only when all of these hold: the Concordance is on; a crucible stands directly above; no Preserve hold; the crucible waits for nothing or for heat alone (`WRONG_HEAT`, or a `HELD` left by a hold now over: the hold itself is judged from the crucible's clock); there is a band; and the crucible is no hotter than the band's target. Otherwise it pays nothing, is dark, and the crucible cools by the usual rule. Refines C-P12's "only while the formula can proceed": coasting down to a cooler band costs nothing. Amended by the unit review: fed less than its rate (JE in within the last 20 ticks), a tick it cannot pay holds the crucible where it is while the crucible is below the band, and, following a formula, at the band's lowest degree, so a trickle reaches the band for the same JE a degree and a pulse finds the crucible in it; inside the band such a tick cools as before, so holding a band still costs C-D3's rates. Short of JE, it goes dark only after 10 such ticks in a row, and a crucible over it reads it as out exactly when it gave nothing | Proposed | C-05 |
| C-P44 | 15 October 2026 | JE goes in on five faces, never the seat (`EnergyStorage.SIDED` gives nothing for `UP`). The stand is not an `EnergyConnectable`, so a cable meets it only where it takes power (`CableBlock.connectsTo`). A departure from the ticket's "and `EnergyConnectable`": the marker matters only in worldgen, and no structure places a stand | Proposed | C-05 |
| C-P45 | 15 October 2026 | It holds 640 JE, takes at most 64 JE a tick (the Ley Pylon's rate), never gives any back, and takes none with the Concordance switched off. The JE it holds is lost when it is broken | Proposed | C-05 |
| C-P46 | 15 October 2026 | With no formula above, a plain empty-hand use by someone `Authority.mayChange` allows cycles off, warm, hot, searing, at most once every 10 ticks a player (`RateGate`). Sneaking only reads. While a formula is set, the formula decides and the setting is kept. As built, with the review's correction: only an empty hand sets or reads it; anything held is used as itself (a crucible placed on it) | Proposed | C-05 |
| C-P47 | 15 October 2026 | Recipe `ICI / ICI / IFI`: 6 iron ingots, 2 copper ingots and the Rooted Thallite Foundation. It loads with the `concordance`, `machines` and `thallite` switches (the last two are what the foundation's plate needs, and `machines` is the JE) | Proposed | C-05 |
| C-P48 | 15 October 2026 | The stand's art is the owner's, as byte copies through `tools/owner_art.py`: treated iron plates (cladding), the treated iron block (posts and deck) and the copper coil seen from its side (`coilside.png`); the end-on coil is not imported, since in this model it would never show. Jugcraft's `thallite_block` is the slab. Lit is the coil drawn full-bright (`light_emission`); no block light and no signs. Awaits the owner's visual review (09:144) | Proposed | C-05 |
| C-P49 | 15 October 2026 | One new diagnostic code, `no_crucible` (blocked), appended after unit 2's `participants`. A stand short of a tick's JE is `unavailable_power` with three lines. Jade puts "Idle while its crucible waits:" before the crucible's own reason | Proposed | C-05 |
| C-P50 | 15 October 2026 | Refines C-P13: the Peepo port subtracts the crucible's request's remainder (asked less delivered) only while a courier holds the request: its claim's lease live and the request not being taken back. A request nobody serves never stalls a Peepo | Proposed | C-06 |
| C-P51 | 15 October 2026 | Each pulse reconciles the crucible's request. A closed one is forgotten (as built, also one not bound for it, not its setter's, or being taken back). One with nothing carried is turned back when its item is no longer what is lacked first, when it asks for more than is lacked, or when its post is gone or barred to its setter. Cargo already carried is always delivered | Proposed | C-06 |
| C-P52 | 15 October 2026 | `courier_quota` takes (count, what, seconds), is blocked (one WANT sign when it begins) and reads `NO_INPUT` at the Peepo port. The backoff is 200 ticks doubling to 1,200, reset by a filing or a turn-back, never saved. Finding no post shows no courier words: a crucible without couriers is an ordinary crucible | Proposed | C-06 |
| C-P53 | 15 October 2026 | Filing needs its setter answering (here, that is online, alive and in the crucible's dimension, or the server's absent-owner option). This stops new filings only: an unclaimed request open when its setter leaves holds the post's queue for every request behind it (`OWNER_OFFLINE`) until they return or someone who may change the crucible clears its formula (a known limit; turning such a request back is for the owner to decide). Since the unit review a crucible files only for what fits in it now, measured as a porter delivers, and nothing while none would, with no backoff. The same player setting the same formula again keeps the request. A formula saved before C-06 has no setter and asks no courier until it is set again. Delivery does not ask `Authority` at the crucible (a known limit) | Proposed | C-06 |
| C-P54 | 15 October 2026 | The Inspect page reads a garden bed: a Verdant Bed, or a crop through the bed below it, within the reader's block reach plus 1.0 (`GARDEN_REACH`, as a crucible's, C-P26). It takes a fresh reading from the bed's cached sample, sampled again only as a plant would; a dormant bed is not read; a bed with nothing on it that a niche judges gives its readings unjudged. Each factor is a judged row naming its reach, with all four bounds of its range; light is shown as sky and lamps and judged as the greater. `Verdict.reasons` (Jade, chat) is unchanged | Proposed | C-07 |
| C-P55 | 15 October 2026 | Stale verdicts clear: a bed forgets its verdict on its random tick once neither a crop nor a Verdant Heart stands on it, and the cleared note is synced and saved. Jade on a ripe crop shows its bed's last verdict as "As it grew: ..." | Proposed | C-07 |
| C-P56 | 15 October 2026 | A held item is its own use on a bed: an item in the main hand is that item's own use on a Verdant Bed, and the bed reports, wakes or tends only for an empty main hand. A Gourd Canteen's sip wets a bed below 7 through `VerdantBedBlock.pour`: `RateGate` ("garden", 4 ticks), then `Authority` (a refusal shows `missing_permission`); a sip is spent only when it wets. The pour is not gated by the switch, as the bed's own watering is not | Proposed | C-07 |
| C-P57 | 15 October 2026 | JEI's compost page: an information page for each item a Mulch Maw eats, worth its highest tag as `Garden.mulch` reads it (`recipe_view.json` `garden_compost`; the client's tags, the default data's worths) | Proposed | C-07 |
| C-P58 | 15 October 2026 | The mixed bed: a crop may declare `neighbours: {organisms, percent}` (1 to 50), naming other crops that cost nutrients, never itself; Dewmoss and Gloamcap name each other at 25 (a starting value). The kinds come from the sample's existing set, with no new read. The bonus is speed only; a fixer neither takes a bonus nor is named as a neighbour, and the rules report a bad neighbour | Proposed | C-08 |
| C-P59 | 15 October 2026 | Harvest strain is counted and shown: a bed notes `ripened` when growth reaches the last age, and the next time it sees its crop below ripe or gone (a growth attempt, its random tick, compost or tending) it counts one, up to 3, whatever took the crop (a Hay Golem or a crow too). A crop placed ripe never strains. `Verdict.of` judges disturbance as the worse of the area alone and the area with the strain. Shown as the `ecology.strained.*` reasons, "Recently harvested: strain n of 3" (reports and Jade) and the Inspect page's disturbance row; a harvest not yet counted shows as counted (`strainNow`) | Proposed | C-08 |
| C-P60 | 15 October 2026 | Rest: one strain rests away a bed random tick, only while no crop grows on the bed (fallow, ripe, or stalled or dormant by its last note); a random tick that counts a harvest rests nothing; with the switch off no strain changes. It refines C-P17's "one a bed random tick", from the model's arithmetic (a random tick about every 1,365 ticks, a Sunpetal's regrowth about 16,400), not a measurement | Proposed | C-08 |
| C-P61 | 15 October 2026 | Compost and tending: each dose of bone meal on the bed, bone meal or fertilizer on its crop (now a plain use on a growing crop: fertilizer by its own area rule, bone meal by the bed's rate gate and who may change the bed, a refusal consuming the use) or a Mulch Maw's nutrient eases one strain; `give` and `nourish` never do. Tending: a Greenwarden's empty main hand on an awake, strained bed they may change (Authority), or on its growing crop (the unit review: a crop covers its bed from above; `VerdantBedBlock.tendBy` asks at the bed), eases one, at most once every 600 ticks a bed; not saved, so a restart allows one early tending. Anyone else only reads the bed | Proposed | C-08 |
| C-P62 | 15 October 2026 | The graft gesture: a plain main-hand use, by a Greenwarden with the Concordance on, of a graft's donor item on its host's crop, at any age. Rate-limited; refused for no bed, for Authority refused at the crop or the bed, or for a bed already carrying a graft the rules know. Every outcome consumes the action and never harvests, reports or plants; success spends one donor. Anyone else's donor is just an item | Proposed | C-09 |
| C-P63 | 15 October 2026 | Graft data and checks: `concordance/graft/<id>.json` is `{schema, host, donor, niche}` only. The loader requires two different loaded crops, a grafted nutrient floor at least the host's cost and one graft a host and donor. `check_graft` holds the pilot's cap of one graft, exactly one range better and one worse (03:76), and no cost, yield or alchemy. A bed carries one graft | Proposed | C-09 |
| C-P64 | 15 October 2026 | Clearing a graft: the first growth attempt of another organism's crop clears it (time, a sprinkler, a Spire's hastening), before the dormant check; reading never clears it. A graft the rules no longer know is kept, read by nothing, and replaced by the next graft. Breaking the bed loses it; a Verdant Heart or a device set on the bed does not clear it | Proposed | C-09 |
| C-P65 | 15 October 2026 | Graft network and save: clients are told `graft` and `graft_host` only while grafted, never the keeper. Jade names a graft on a crop only when the crop is its host, and on a bed only when the rules know it; the Inspect page does the same. The save writes `graft` only when set, never `graft_host`; the stamp stays 1 | Proposed | C-09 |
| C-P66 | 15 October 2026 | The load runner as built, refining C-P20. `ConcordanceWorkshopLoadGameTests.loadCard` measures only when Fabric's filter (`fabric-api.gametest.filter`) names exactly its id; in every other run it passes on its first tick. Locally the filter goes through `JAVA_TOOL_OPTIONS` (`docs/TESTING.md`); in CI, through a manual Build run's `load_card` input, which runs the card alone in its own job (`bfb08770b`, a workflow change awaiting the owner). It force-loads its yard and holds noon and the weather, with mob and wandering-trader spawning off. It measures the baseline first, then the workshop: each span a warm-up (at least 2,000 and 1,500 ticks, then until the loaded chunks hold 400 ticks), then 6,000 ticks of the server's ring, net of the test's own work (the simulated player's actions and output extraction included). Power comes from Creative Energy Cells | Proposed | C-10 |
| C-P67 | 15 October 2026 | The budget's form, refining C-P19 (for merge review, not a checkpoint decision). Tick time: at most +1.0 ms median and +2.5 ms p95 per tick, net. "Flat memory trend": the heap retained after a full collection grows by at most 4 MiB more than the baseline's over the measured 6,000 ticks. Network traffic is recorded with no budget. The test asserts only +5.0 ms, +12.5 ms and +64 MiB | Proposed | C-10 |
| C-P68 | 15 October 2026 | C-P21's line. If a starved Gleaner pulse (its whole 729-position Heart search) takes at most 250 µs on the runner the "Gleaner pulse" line names (the `mod` job's, in CI), `HEART_REACH` stays 4. If it is over, a change is proposed and not applied: a vertical reach of ±1 (243 positions), or caching `Garden.nearest`'s fixed offset order. Timed by `ConcordanceWorkloadGameTests.gleanersHeartSearch` after 1,000 untimed pulses, in every ordinary build. Measured in run 38774859294: 333 µs, over (see C-10's As built notes) | Proposed | C-07, C-10 |
| C-P69 | 15 October 2026 | The living workshop in the journey: not a stage route but five steps, one player alone in the Overworld with the research understood so far and nothing mastered. Nothing it needs is past the Practitioner stage, though its five understood entries make that player a generalist Adept (an open question in the journey record). The Kiln Draught is made by the sugar process over a campfire, the poultice over a magma block. The factory's deeper materials stay the factory's. `concordance_journey.py --check` exits 1 on a problem. The server test reuses `jugcraft-test:drone_depot` and adds no structure | Proposed | C-10 |
| C-P70 | 15 October 2026 | Client evidence is a new class, `ConcordanceWorkshopClientGameTests`, rather than an extension of `EncyclopediaClientGameTests`. Its weight is 200 s, an estimate. Random ticks are held at 0. The GUI scale 4 shot is taken in a 1920×1080 window with the applied scale checked, since the test's own 854×480 window allows no scale above 2. The frame-time observation describes Mesa's software renderer only | Proposed | C-10 |

## Where to resume

Units 1 to 5 (C-01 to C-10) are built and recorded, and the candidate `bb856f873` passed every CI job in run 38774859294
(the status at the top; [Unit 5](#unit-5-evidence-and-handoff-c-10)). Nothing is merged. The owner answered the
checkpoint on 23 October 2026 (C-D5 to C-D8): hands-on and automated work as built, Gloam Ink next (when it is built is
not yet decided), the Crucible Stand's look kept, and Chunks B and C reviewed as two stacked pull requests (Chunk B's
against main, Chunk C's against Chunk B's branch). The load budget (C-P19 in C-P67's form) and the workflow change
(`bfb08770b`, the manual-only `load_card` input and job) remain for the owner's review in Chunk C's pull request. C-D0,
C-D0b, C-D1 to C-D3 and C-D5 to C-D8 are the owner's; C-D4 and C-P1 to C-P70 are Proposed. Chunk C has since been merged
with Chunk B carrying main `f904df3fd` (`b234b8967`), and the commit after it brings the Assayer's Scale tests and
records to main's Scale (C-P40). Before more work:
1. done on 23 October 2026: `src/main/resources/data/jugcraft/alchemy/values.json` regenerated with
   `tools/build_alchemy_values.py`, its `--minecraft-jar` input a zip of vanilla 26.3's `data/minecraft/recipe` and
   `data/minecraft/tags/item` from the decompiled reference (the same input rebuilds main's own file byte for byte;
   nothing of it is committed): the Crucible Stand is valued from its recipe (1,793), and the Rooted Thallite
   Foundation, made by a ritual, sits at the generator's labelled fallback of 1, below every route's cost, never formed
   (C-P40). `docs/audits/survival-item-registry.json` is main's exported registry plus the 17 items Chunks B and C
   register (every registered jugcraft item on main has an `assets/jugcraft/items` definition, and each added one is
   registered in Java); a game run's export (`SurvivalRecipeGameTests`) can replace it. The audit's derived JSON and CSV
   are left as main has them (main's own copies do not reproduce on main). `scripts/tests` passes, 195 tests;
2. refresh main and the open pull requests (#369 for the Peepo files; #357 is merged in main as `40381834a`; the hot
   shared files: lang, the gametest `fabric.mod.json`, `check_mod_data.py`, `CHANGELOG.md`, `docs/TESTING.md`,
   `WHAT_EXISTS.md` with #366, `INTEGRATION_STATUS.md` with #361 to #365), and merge main again if it has moved past
   `f904df3fd`;
3. the next smallest outcome (C-D6; when it is built is not yet decided): one Gloam Ink formula from a mixed bed's
   Gloamcap with Research Notes as its consumer.

## How this contract was checked

- Re-read at `e079ad4a3` for this page: the crucible (fields, tick, heat, automation, bottling, use, formula, empty-hand
  use, faces, save and load, update tag), `CrucibleBlock`, `Formula`, `Outcome`, `Property`, `Mixture`'s constants and
  bottling, `Heat`, `Band`, `Alchemy`, `BrewItem`, `ConcordanceData`, `Composing`'s use of the revision;
  `CrucibleCompanion` and `CompanionStatus`; `Encounter` (credit, healing, wards, claims), `Lairs`' observer,
  `ConcordanceEffects`' observer call, `LairDossiers`' rite and preparation, `MoltenSlagBlock`, `SalamanderCharm`, the
  Tyrant's flame and his loot table; the anchor (use, begin, status, observe, report, interruption, removal),
  `RitualDefinition`, `Signs`, `Strata`'s listener, `InspectPage`, the Jade plugin, provider and client lines named;
  `Sampler`, `Niche`, `OrganismCropBlock`, `VerdantBedBlockEntity`, `VerdantBedBlock`'s uses, `Garden.nourish` and
  `bedAndNeighbours`, the Maw's giving, every niche reader, the organism, ingredient and property data, the canteen, the
  Gleaner's reach; `CourierLedger.file` and `destinationGone`, `Logistics`' limits, the porter's statuses and delivery,
  `ConcordSpire.restock` and `nearestPost`, the Spire Heart's removal, `Couriers.request`; the pylon, `LeyPylonBlock`,
  the Electric Pump, the crucible's fluid registration; `Authority` and `Illumination`; `RulesParser`'s allow-list; the
  Thallite plate's recipes and tags; `tools/check_mod_data.py` at the lines cited; `tools/concordance_alchemy.py`,
  `concordance_ecology.py`, `concordance_rituals.py` and `machine_connections.py` at the lines cited; the test classes
  named (their `@GameTest` counts), `theCircleIsCheckedPartByPart`'s assertions and the progression node list;
  `.github/workflows/build.yml`, `docs/TESTING.md:41`, `tools/select_client_tests.py:53`; the records' cited lines; plan
  chapters 09 (sections 1 to 2, 5, 11 and 12), 03 (whole), 06 (sections 1 to 9), 08 (sections 3, 5, 7 to 9) and 10
  (whole), `CLAUDE-MASTER-PROMPT.txt` 60-90 and `START-HERE.md` 44-50; mc263src `MultiPlayerGameMode.performUseItemOn`,
  `Inventory.placeItemBackInInventory` and `MinecraftServer`'s tick times.
- Read by REST on 14 October 2026: the open pull requests and their heads; run 38617471274's jobs and steps, both
  attempts. Compared in a scratch repository: each open pull request's files from its merge base against this plan's
  files (#370's head fetched again).
- Checked again after the page was written (14 October 2026): every code, data, test and record citation above opened
  at `e079ad4a3` at its cited lines; the plan-pack and Chunk B contract lines cited; the merge's parents, its two
  resolutions and the diff counts; by REST, the open pull requests (18, #371 new, its files by `/pulls/371/files`), that
  neither `claude/chunk-b-winter-stone` nor `claude/chunk-c-living-workshop` has a pull request, run 38617471274's jobs
  and steps for both attempts and the failed `mod` job's annotations (its log was refused, 403). Corrected here: line
  numbers (09:16 for "end-to-end path first", `concordance_alchemy.py:33-36`, the plate tag file), the C-02 model
  figures, the Peepo heat status, the Stacking rule, the theCircleIsCheckedPartByPart summary, the bed-note writers, the
  canteen's targets, the construct-upgrade deferral, #371, and survey labels on facts now verified.
- Taken from the survey reports, the delivery plan or its review without re-reading, and marked *(survey)*: the
  alchemy survey's section 9 obligations; the harvest-path table; loot-table harvests dropping unstamped produce; the
  null-side insert; #370's Concordance keystone.
- Not determined: the cause of run 38617471274's first `mod` failure (the log host refused this page's requests; recorded
  from the session's reading); whether Fabric applies load conditions to the Concordance's loader; whether the client
  tests' dedicated server shares the client's process. Answered for unit 1 (C-01's and C-02's As built notes):
  Regeneration's healing interval in 26.3, read in `mc263src` HEAD; how a batch is isolated (vanilla batches by
  environment and dimension; unit 1 does not rely on it).
- Run for this page: the alchemy generator's model (`tools/concordance_alchemy.py` `simulate`, imported read-only from a
  scratch script) for the C-02 figures; `python3 scripts/check_repository.py` (passed) and `git diff --check` on the
  page. Not run: any build, game test, data generator or other check.
- Unit 1's notes (15 October 2026): written from the unit's four commits, re-read for the names, keys, counts and test
  names given; the figures re-run from the generator's model (`bottle`, `outcome`, `poultice_verdance`, `realised`,
  imported read-only from a scratch script). The checks run for the records' commit are in the alchemy record's
  Verification.
- Unit 2's notes (15 October 2026): written from the unit's two commits (`a0e73881c`, `2c98d6d68`) and their scratch
  logs (the checks, the audit mutations and the client-test selection), re-read for the names, keys, counts, test names
  and lang given; the library candidates' hashes taken again from `art/owner-library` (read, nothing executed). Run for
  the records: the scratch harness (above, C-03's As built), and the checks in the rituals record's Verification.
- Unit 3's notes (15 October 2026): written from the unit's three commits (`940f1f8a8`, `7b2dd6783`, `46f6e2a0e`) read
  as diffs against `23f28b90e`, re-read for the names, keys, constants, test names and counts (`@GameTest` counted in
  each class), lang and generated words given; the stand's provenance printed by
  `python3 tools/owner_art.py --provenance alchemy`, and the rejected candidates' hashes and transparent texels taken
  again from `art/owner-library` (read, nothing executed); the container drop read in the mirror's `HEAD`. Run for the
  records: the checks and the 53 audit mutations in C-06's As built notes.
- Unit 4's notes (15 October 2026): written from the unit's three commits (`5ba973632`, `d32ac3a61`, `d6edee4f4`), their
  messages and scratch logs (the checks, the generator's two runs and the C-08 and C-09 audit mutations), with the
  spec's code compared line by line against the commits for the departures; re-read for the names, keys, counts, test
  names and lang given. Run for these records on the final tree: the three audit-mutation suites (C-07's written again
  from the spec's table), the two pure harnesses, `git diff --check` on each code commit, `select_client_tests.py`, and
  the checks in the ecology record's Verification.
- Unit 5's notes (15 October 2026): written from the unit's commits (`a86085b3c`, `bfb08770b`, `31bc7087d`,
  `6d54726fa`, `fd2b558c8`, `bb856f873`) and their messages, the code re-read for the names, constants and test names
  given, and the unit's specification and its review. The CI facts are those of run 38774859294 and the earlier runs
  named in the status, as the session read them: the load card's block from its job's log; the `mod` job's "Gleaner pulse" line and
  test count from that job's log, fetched for these records through the GitHub connector's job-log tool (the log host
  itself is refused here, 403); the four shots viewed, from client shard 4's log. Every test name in the demonstration
  was found in the test sources. Run for the records: `python3 -B tools/check_mod_data.py`,
  `python3 scripts/check_repository.py`, `python3 tools/concordance_journey.py --check` and `git diff --check`, each
  passed. Not run here: any build, game test, client or other check.
