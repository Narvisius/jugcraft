# Bulk Chemical Oxidizer: the Chemical Oxidizer's 4×4×6 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [Bulk Chemical Infuser](industrial-bulk-chemical-infuser.md) and the branches below it.

Proposal issue: none. TODO items:
- "Develop both installed upgrade chips/modules and larger advanced physical versions" of the Oxidizer and its sibling roles;
- "Larger chemical machines gain higher throughput and lower energy per unit; upgraded small machines remain useful".

**From the owner's plan:** the size comes from the art brief ([Chemical Oxidizer](industrial-machine-models-and-textures.md#chemical-oxidizer)): "2×2×3 blocks; larger variant 4×4×6 blocks". The bulk profile is the foundation's selected one.

**This change's choice:** the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas and acid factory, after the Chemical Oxidizer
Primary specialty and supported player role: sulfur dioxide and carbon dioxide at scale

## Player experience

The **Bulk Chemical Oxidizer** is the Chemical Oxidizer twice as large every way, four wide, four deep and six tall, with all of its parts:
- the plinth;
- the squat dark-steel reaction throat, with its cream refractory ring round the bolted hatch, the reaction slit that glows while it burns, and the residue drawer;
- the gas column and ringed collector hood, its outlet rising to a collar on top;
- the broad olive hopper at the back, with its hazard-striped lip, chute and guarded trough, and the feed screw turning in it;
- the capped oxidant inlet;
- the junction box with the power socket.

The control panel (the controller) keeps its own, human size at the front left.

It burns **everything the Oxidizer does**, four batches at once from 16,000 mB tanks:
- sulfur dust into sulfur dioxide;
- coal or charcoal into carbon dioxide.

**Ports.** The same sides as the Oxidizer, each moved to the middle of a block of its larger body:
- feed at the hopper's mouth on top at the back;
- oxygen low on the right side at the front;
- the gas at the collar on top at the front;
- power at the junction box low on the left side.

**Guides.** Its Engineer's Handbook page describes it.

## Connections

- **Existing input producers:**
  - sulfur dust from sulfur ore;
  - coal and charcoal;
  - oxygen from the Separator, Cell and air separation unit.
- **Existing output consumers:**
  - sulfur dioxide for contact conversion and sulfuric acid;
  - carbon dioxide for methanation.
- **Technology connection:** the shared form foundation's bulk profile. It uses the Oxidizer's own capability and recipes, so no new recipe was needed.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains a Chemical Oxidizer, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Oxidizer stays the entry machine, and its upgrade cards keep it useful.

## Balance and automation

- **Throughput.** Four lanes give four times the Oxidizer's rate, enough sulfur dioxide for a Bulk Chemical Infuser's contact conversion.
- **Energy.** A lane pays 77 JE/t instead of 96, about 20% less per batch: 7,700 JE instead of 9,600.
- **Same amounts.** It makes nothing more per batch than the Oxidizer.
- **Upgrade cards** fit its terminal, as on the Oxidizer.

## Choices made here for review

- **The bill:** a Chemical Oxidizer, two Steel Tanks, four steel plates, an electric motor and a basic circuit (`TOT / PMP / PCP`).
- **Its cells follow its model:** 90 blocks in the 4×4×6 envelope, the open spaces beside the hopper kept free.
- **JEI.** It has its own recipe page with the Oxidizer's recipes; main's recipe view gives every form one.

## Multiplayer and persistence

- **Server authority.** The server decides everything.
- **Ports.** They check what goes in, and nothing can be put in at the outlet.
- **New registrations,** stable from now on: the block and item `jugcraft:bulk_chemical_oxidizer`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

**The model** is the Oxidizer's own (`chemical_oxidizer()` in `tools/industrial_forms.py`), doubled by `bulk_chemical_oxidizer()` through the same `_doubled` helper as the Bulk Chemical Infuser:
- **Ports.** Each port's pieces move half a block off the seams the doubling puts them on (`BULK_OXIDIZER_PORT_SHIFTS`). The hopper, chute, trough and screw move together.
- **Flanges** are kept within one block each.
- **The control panel** keeps its size.

**Textures** are the Oxidizer's shared ones.

**Turning parts:** `bulk_chemical_oxidizer_screw` in `machine_rotor_quads.json`.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.

**Server game tests for CI** (`BulkChemicalOxidizerGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 90 blocks in every orientation, its open cells stay free, and only its four ports answer. |
| `sulfurBurnsFourBatchesAtOnce` | 1,000 mB of sulfur dioxide for exactly 30,800 JE. |
| `everyPartLightsWhileItWorks` | Every part lights while it works. |
| `itRunsTheOxidizersRecipesAndIsBuiltFromItsBill` | It runs exactly the Oxidizer's two recipes, four lanes at 80%, and the bill builds it. |

**Client game test** (`BulkChemicalOxidizerClientGameTests`): three views, a night view, its screen while working and its screen when blocked by a full product tank.

### Results

**Run [38500691578](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38500691578) (commit b2611732) passed:** the build, the data checks, every server game test (`BulkChemicalOxidizerGameTests` included) with and without optional integrations, and all five client test shards (`BulkChemicalOxidizerClientGameTests` included). The first attempt's client shard 3 failed in `PeepoDeliveryClientTests` (the companion's "meal" wait timed out), a test this change does not touch, as on the Large Gas Burning Generator's run; that shard passed on its one re-run.

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

- The bill awaits the owner's review.
- The brief's 4×4×6 Separator waited on the [electrolysis floor](industrial-electrolysis-floor.md); it is since built without upgrade cards: the [Bulk Electrolytic Separator](industrial-bulk-electrolytic-separator.md).
