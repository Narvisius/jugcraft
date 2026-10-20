# Bulk Electrolytic Separator: the Electrolytic Separator's 4×4×6 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [Bulk Chemical Oxidizer](industrial-bulk-chemical-oxidizer.md) and the branches below them.

Proposal issue: none. TODO items:
- "Develop both installed upgrade chips/modules and larger advanced physical versions" of the Separator and its sibling roles;
- "Larger chemical machines gain higher throughput and lower energy per unit; upgraded small machines remain useful";
- the [electrolysis floor](industrial-electrolysis-floor.md)'s next step, a larger Separator priced at or above 256 JE/mB.

**From the owner's plan:**
- the size, from the art brief ([Electrolytic Separator](industrial-machine-models-and-textures.md#electrolytic-separator)): "2×2×3 blocks; larger variant 4×4×6 blocks";
- the bulk profile, the foundation's selected one;
- the 256 JE/mB electrolysis floor, which the plan sets before any larger Separator.

**This change's choices:** no upgrade cards, and the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas and acid factory, after the Electrolytic Separator
Primary specialty and supported player role: hydrogen, oxygen, chlorine and lye at scale

## Player experience

The **Bulk Electrolytic Separator** is the Electrolytic Separator twice as large every way, four wide, four deep and six tall, with all of its parts:
- the plinth;
- the olive cell housing, with its stepped door, gunmetal bands and crown of electrode feedthroughs;
- the two white collection towers, with their checker shoulders, domes and level strips;
- the banded outlet pipes to their collars high on the front;
- the junction box with the power socket, and the lye return above it;
- the feed inlet at the back.

The control box (the controller) keeps its own, human size at the front left.

It splits **everything the Separator does**, four batches at once from 16,000 mB tanks:
- water into hydrogen and oxygen;
- brine into chlorine, hydrogen and lye.

As on the Separator, a full lye tank stops brine but not water.

**Ports.** The same faces as the Separator, each moved to the middle of a block of its larger body:
- the feed at the back, one block up;
- hydrogen at the collar high on the front left;
- oxygen or chlorine at the collar high on the front right;
- lye at the return in the middle of the front;
- power at the junction box low on the front, below the lye return.

**No upgrade cards.** Its screen has no card slots. The handbook says why: carded, it would make hydrogen cheaper than the factory allows.

**Guides.** Its Engineer's Handbook page describes it. JEI gives it its own recipe page with the Separator's recipes, as main's recipe view does for every form.

## Connections

- **Existing input producers:**
  - water from any pump or source;
  - brine from the brine supply the Separator already uses.
- **Existing output consumers:**
  - hydrogen for the gas generators, the Fuel Cell, the Chemical Infuser and methanation;
  - oxygen for the Chemical Oxidizer and combustion;
  - chlorine for hydrogen chloride and the PVC route;
  - lye for the existing lye consumers.
- **Technology connection:** the shared form foundation's bulk profile. It uses the Separator's own capability and recipes, so no new recipe was needed.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains an Electrolytic Separator, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Separator stays the entry machine.

## Balance and automation

- **Throughput.** Four lanes give four times an uncarded Separator's rate: 2,000 mB of hydrogen from four buckets of water in 40 seconds.
- **Energy.** A lane pays 205 JE/t instead of 256, a fifth less per batch:
  - water: 328 JE per mB of hydrogen, against an uncarded Separator's 409.6;
  - brine: 82,000 JE a bucket instead of 102,400.
- **The floor.** 328 JE/mB stays above the plan's 256 JE/mB floor. With four efficiency cards it would be 134.4, which is why it takes none.
- **Upgraded small machines remain useful.** A Separator with four efficiency cards still makes hydrogen more cheaply (168 JE/mB) at a quarter of the rate. Players choose between the cheap small machine and the fast large one.
- **Same amounts.** It makes nothing more per batch than the Separator.
- **No loops.** Every generator burns hydrogen at 128 JE/mB, well below 328. The loop audit now counts this machine (see below).

### The audits now see the larger forms

Before this change, the loop audit and the floor check in `tools/check_mod_data.py` read only each form's own recipe list. A larger form running its smaller form's recipes (the Bulk Fermenter, Infuser and Oxidizer, and now this Separator) has no list of its own, so both checks skipped it.

They now read main's `recipes_for_form(form)` in `tools/industrial_forms.py`, which gives such a form the recipes it actually runs: every recipe of its family whose capability it has. The tank checks use it too. Nothing existing breaches either check; the audit's cheapest hydrogen is still 168 JE/mB.

## Choices made here for review

- **No upgrade cards.** The other option recorded in the [electrolysis floor](industrial-electrolysis-floor.md) doc was to cap the cards. One efficiency card would give 262.4 JE/mB and two would break the floor. No cards keeps the screen and the rule simple.
- **The bill:** an Electrolytic Separator, two Steel Tanks, four steel plates, copper cable and a basic circuit (`TST / PWP / PCP`).
- **Its cells follow its model:** 81 blocks in the 4×4×6 envelope. Above the control box and beside the towers at the back, the spaces are kept free.

## Multiplayer and persistence

- **Server authority.** The server decides everything.
- **Ports.** The inlet takes only water and brine, and nothing can be put in at the outlets.
- **New registrations,** stable from now on: the block and item `jugcraft:bulk_electrolytic_separator`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

**The model** is the Separator's own (`electrolytic_separator()` in `tools/industrial_forms.py`), doubled by `bulk_electrolytic_separator()` through the shared `_doubled` helper:
- **Ports.** Each port's pieces move half a block off the seams the doubling puts them on (`BULK_SEPARATOR_PORT_SHIFTS`). The lye return also moves half a pixel forward, so its frame does not share a face with the door.
- **The power socket** is kept within one block.
- **The control box** keeps its size.

**Textures** are the Separator's shared ones.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.

**Server game tests for CI** (`BulkElectrolyticSeparatorGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 81 blocks in every orientation, its open cells stay free, and only its five ports answer. The inlet takes brine and water, not oxygen. |
| `brineSplitsFourBatchesAtOnce` | Four buckets of brine give 1,000 mB chlorine, 1,000 mB hydrogen and 2,000 mB lye for exactly 328,000 JE, each from its own outlet. |
| `itTakesNoCardsAndStaysAboveTheFloor` | It has no upgrade slots, and hydrogen from water costs 328 JE/mB, at or above the floor. |
| `everyPartLightsWhileItWorks` | Every part lights while it works. |
| `itRunsTheSeparatorsRecipesAndIsBuiltFromItsBill` | It runs exactly the Separator's two recipes, four lanes at 80%, and the bill builds it. |

**Client game test** (`BulkElectrolyticSeparatorClientGameTests`): three views, a night view, its screen while working and its screen when a full lye tank blocks brine.

### Results

**Run [38505827595](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38505827595) (commit cd0fabe7) passed:** the build, the data checks (the extended loop audit and floor check included), every server game test (`BulkElectrolyticSeparatorGameTests` included) with and without optional integrations, and all five client test shards (`BulkElectrolyticSeparatorClientGameTests` included).

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

- **Owner review:** no cards, and the bill.
- **Owner decision still open:** whether the entry Separator should also be held to the floor (see the [electrolysis floor](industrial-electrolysis-floor.md)).
