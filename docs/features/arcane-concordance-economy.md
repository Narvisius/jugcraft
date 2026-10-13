# Arcane Concordance: the economy (interactions and economic loops)

Status: implemented on branch `claude/concordance-steps-26-32`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 29) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 21, every stage (Initiate to Architect). Roadmap step 29.
Primary specialty and supported player role: every player of the Concordance; no tradition of its own.

The brief's acceptance criterion: renewable systems consume declared inputs or time, and unintended feedback loops are
either removed or intentionally bounded. Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed

Each system already had its own loss checks:
- the data conversions lose (`ConversionTable` and `check_concordance`);
- composting a crop's harvest returns less than regrowing it cost (`check_ecology`);
- salvage returns less than forging (`check_artifice`);
- equivalence has its own profitable-cycle audit (`check_equivalence`);
- relic charge never becomes Ley Charge again (`check_relics`);
- an hour of offerings gives at most 60 Vitae (`check_crimson`).

No audit looked across the systems, and the hard-coded rates (Jugcraft Energy to Ley Charge, a Verdant Heart's beat,
bone meal, relic charge, a Crimson Surge) were in no loop check.

## What this step found and changed

- **A spire minted nutrients (removed).** A Verdant Spire's field aged any crop by setting its age, so a Concordance
  crop grew without paying its bed or checking its niche. Over Mendvetch, which costs nothing and fixes a nutrient a
  step when time grows it, the field's 1,440 steps a day would have made up to 1,440 nutrients from nothing. A dozen
  Verdant Hearts (each beats at most 120 nutrients a day) would turn that into about 1,900 Ley Charge a day, many
  times the spire's own upkeep. Now a hastened
  step goes through the crop's own rules (`OrganismCropBlock.hasten`): its awake bed and niche, at least one nutrient a
  step, and no fixing. Vanilla crops still simply age, as bone meal ages them.
- **Light for nothing (removed).**
  - Putting a Kindled Lantern out, or breaking a Lumen Sconce, kept the measure it was burning. So relighting a
    lantern every 399 ticks, or replacing a sconce every 1,199, gave light without spending anything.
  - Now a measure begun is a measure spent, as every rounding in the Concordance goes against the player.
  - Lighting and putting out in the same tick still costs nothing.
- **One audit across the systems** (`tools/concordance_economy.py`, checked by `check_economy`):
  - Every conversion and every source driven by time is modelled, each figure tied to the Java or data it comes from,
    so the model cannot drift from the game.
  - No cycle of conversions comes back with as much as it started with.
  - Every crop's regrow-and-compost loop is included, at its natural cost and when a spire hastens it.

## Bounded on purpose

These give more than they take, from time and a declared upkeep, and are bounded a day:

- **A Star Spire** gives each of up to 6 players on its side 1 Focus a pulse, never above 20 held. Turned into Radiance
  and Ley Charge, that is more than its own 8 Ley Charge a day: an endgame wonder's reward, capped by the pulse and the
  held Focus.
- **A Verdant Spire** ages vanilla crops for its 8 bone meal and 6 Ley Charge a day. A vanilla composter can turn that
  harvest back into more bone meal than the upkeep: an estimated 19 a day at most, from 1,440 steps of wheat at the
  composter's 65% a layer. That is a bounded harvest, never a growing one: nothing it makes raises its own rate.
- **Mendvetch grown by time** fixes a nutrient a step at a crop's natural pace, and none when hastened.

## Representative installations and every source driven by time

The table is rendered by `python3 tools/concordance_economy.py`, and `check_economy` fails if it differs:

<!-- economy:start -->
| Installation, a day (24,000 ticks) | Takes at most | Gives at most | Bound | Consequence |
|---|---|---|---|---|
| Verdant Heart on a thriving bed, beside a Ley Pylon | 120 nutrients (60 bone meal) | 240 Verdance, poured as 160 Ley Charge | one beat every 200 ticks | the bed's nutrients fall; it stops on a starved bed |
| Ley Pylon on Jugcraft Energy | 1,536,000 JE | 1,536 Ley Charge | 64 JE a tick | none; Ley Charge never becomes JE again |
| Gleaner on Concordance crops | 600 Verdance | 600 harvests, of ripe crops only | one every 40 ticks | each crop falls back to step 1 and must regrow at its cost |
| Kindled Lantern, lit | 60 Radiance | light at its carrier | a measure every 400 ticks; a measure begun is spent | light |
| Lumen Sconce | 20 Radiance | light 15 | a measure every 1200 ticks; a measure begun is spent | light |
| Reliquary Shrine recharging | 2,400 Ley Charge | 9,600 relic charge | 8 charge a second | none; relic charge never becomes Ley Charge again |
| Crimson Surge | 120 Vitae | 120 Focus | one every 1200 ticks | health given (offering exhaustion) |
| Verdant Spire, kept | 8 bone meal and 6 Ley Charge | 1,440 crop steps | 6 a pulse, a pulse every 100 ticks | Concordance crops pay their beds |
| Star Spire, kept | 2 glow ink sacs and 8 Ley Charge | 240 Focus a player | 6 a pulse, a pulse every 100 ticks | hostile creatures revealed |
| Lantern Spire, kept | 4 glowstone dust and 8 Ley Charge | 960 lights | 4 a pulse, a pulse every 100 ticks | no hostile spawns in its light |

| Source driven by time | Bound |
|---|---|
| A Verdant Spire's field | at most 1,440 crop steps a day (6 a pulse, a pulse every 100 ticks); a Concordance crop pays its bed at least 1 nutrient a step and fixes none |
| A Star Spire's field | at most 240 Focus a day to each of 6 players a pulse, never above 20 held |
| A Lantern Spire's field | at most 960 Kindled lights a day (4 a pulse) |
| Mendvetch (a fixer) grown by time | 1 nutrient a growth step, at a crop's natural pace (random ticks); never when hastened |
| Offerings (health given as Vitae) | at most 60 Vitae an hour, whatever heals the giver |
| A Crimson Surge | at most 20 a day (one every 1200 ticks) |

| Crop loop (regrow, then compost the harvest) | Gives back (quarters) | Costs (quarters) |
|---|---|---|
| dewmoss, grown | 4 | 8 |
| dewmoss, hastened | 4 | 8 |
| gloamcap, grown | 3 | 16 |
| gloamcap, hastened | 3 | 16 |
| mendvetch, hastened | 3 | 8 |
| sunpetal, grown | 4 | 8 |
| sunpetal, hastened | 4 | 8 |
<!-- economy:end -->

## Interactions tested

The brief's combinations, and the tests that exercise them (new in this step marked **new**):

| Combination | Where it is tested or audited |
|---|---|
| Growth acceleration with energy generation | **new** `ConcordanceSpireGameTests.aVerdantFieldHastensOnlyWhatItsBedsPay`: a Verdant field over Mendvetch and a Sunpetal on beds of one nutrient each grows each one step and no further over 12 pulses; the beds never gain. **New** `ConcordanceGardenGameTests.aHastenedStepPaysAndFixesNothing`. `check_economy`: no gaining cycle through the Verdant Heart. |
| Healing with offering | `ConcordanceVigilGameTests.healingDoesNotResetTheOffering`, `exhaustionClearsOnlyWithTime`, `aSurgeTurnsVitaeIntoFocus`; `check_crimson`'s 60 Vitae an hour |
| Duplication with salvage | `ConcordanceArtificeGameTests.salvageNeedsConfirmingAndNeverGains`; `ConcordanceAssayGameTests.formingCostsMoreThanDissolvingPays` and `theCatalogueIsBalanced`; `ConcordanceHexGameTests.dreamsNeverDuplicatePossessions` |
| Summoned creatures with loot collection | `ConcordanceWorkerGameTests.aSpiritKeepsToItsAgreement` (a Gathering Shade moves a dropped item into its anchor, never copies it); `ConcordanceAuthorityGameTests.aPorterTakesOnlyWhatItsKeeperCould` |
| Repeated triggers with resource refunds | `ConcordanceRelicGameTests.relicsShareABudgetAndNeverAddUp` (a relic held back is not spent; two giving the same effect never add up); `ConcordanceEffectGameTests.friendlyFireToleranceAndTriggeredCause`; a composed spell's later pulses are never refunded ([ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md)) |
| Remote storage with interrupted rituals | **new** `ConcordanceRitualGameTests.aPorterFuellingFromACircleStopsItCleanly`: a porter fuels itself from a channel's pylon mid-ritual; it took exactly what the pylons lost, the ritual stops for want of power with every offering kept, and neither its Focus nor the porter's Ley Charge comes back. `ConcordanceLogisticsGameTests.cancellingAndRecoveringGiveItemsBack`, `fullStorageRemovalAndRestartLoseNothing`, `aBrokenPostSendsItsCargoBack` |
| Light burnt and relit | **new** `ConcordanceEconomyGameTests.aLanternPutOutHasSpentTheMeasureItBegan`, `aSconceTakenDownHasSpentTheMeasureItBegan` |
| Automation of a formula | `ConcordanceAlchemyGameTests.machineryRunsAFormulaTwice` |

The brief's library interactions:

| Interaction | State |
|---|---|
| SmartBrainLib workers with transactional storage | Tested: the logistics tests and the new porter-and-ritual test move every item and charge through transactions, counted. |
| Trinkets with modifiers | Partly: `ConcordanceRelicGameTests.relicsAreFoundInTrinketSlots` and the ring's Trinkets modifiers (`ConcordanceArtificeGameTests`). Equipping and unequipping repeatedly is not tested. |
| Spell Power with affixes and living equipment | Partly: rings' Spell Power and the Lance's scaling are tested; `check_artifice` caps one ring at a time, not the sum of a player's rings and the Thornheart Blade. |
| Casting with triggered relics | Partly: each is tested, and a triggered effect stays its caster's; no test casts while a relic fires. |
| Research with codex and recipe visibility | Partly: the journal names only met research (`ConcordanceJournalGameTests`); JEI's and the codex's visibility are not tested in a client. |
| Packet replay | Partly: a repeated request within its window does nothing (the sconce's pour, the journal, the bench and courier commands, `RateGate`); no test replays raw packets. |
| Cancellation at each charge stage | Not tested: Spell Engine's charge stages run from the client's input. Focus is taken once, at the cast (`ConcordanceGameTests.kindleCastsThroughSpellEngine`). |
| Player animation with ArmsMotion and casting; dynamic lights with gameplay light | Not tested: client-side. Gameplay light is the server's Lumen motes alone; LambDynamicLights only draws. |

## Connections

- Input producer and output consumer: every Concordance system that converts or produces a resource.
- Reachable entry path: nothing to unlock.
- Mastery: none.

## Balance

- A hastened Concordance crop now costs its bed at least a nutrient a step, and a hastened Mendvetch fixes none.
- A lantern put out, or a sconce taken down, spends the measure it began.

No other number changes.

## Multiplayer and persistence

- Server authority: unchanged; the server decides every conversion.
- Persistence: nothing new is saved. A sconce item's charge is what it kept.
- Disable behaviour: unchanged.

## Dependencies and assets

No new dependency, art, sound or animation.

## Verification

- Run locally before pushing:
  - `python3 tools/check_mod_data.py` on a copy of the tree, with the new `check_economy`;
  - regenerating the data on a second copy;
  - `python3 scripts/check_repository.py`;
  - `python3 tools/concordance_economy.py`: no gaining cycle.
  Their results are in the pull request. None of the Java of this step can compile here; CI is its first compile.
- Server game tests added:
  - `ConcordanceEconomyGameTests` (two);
  - `ConcordanceSpireGameTests.aVerdantFieldHastensOnlyWhatItsBedsPay`;
  - `ConcordanceGardenGameTests.aHastenedStepPaysAndFixesNothing`;
  - `ConcordanceRitualGameTests.aPorterFuellingFromACircleStopsItCleanly`.
- CI: the first run, on bd916c4, failed two of 1069 server tests, both fixed in 9f50a1d. One was this step's porter test,
  which asserted that Focus stayed put although it returns with time. The other was a dream test whose wisps fell where
  creatures were not live. Build run 37680579216 on 9f50a1d: `mod` and `optional integrations absent` passed, with all
  1069 server tests. This step adds no client test.

Not yet run:
- a representative installation simulated over real world time in a running world (the tests advance their systems by
  their own pulses);
- the library interactions marked untested above.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

No new identifiers. Known limits:
- the spires' bounded rewards above;
- Spell Power caps are checked one ring at a time.

Open question for the owner: should a Verdant Spire's field skip vanilla crops, so its upkeep cannot be paid from its
own harvest? It is left as a bounded reward.
