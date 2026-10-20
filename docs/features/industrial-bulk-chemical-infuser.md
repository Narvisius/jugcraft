# Bulk Chemical Infuser: the Chemical Infuser's 4×6×6 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [electrolysis floor](industrial-electrolysis-floor.md) and the branches below it.

Proposal issue: none. TODO items:
- "Develop both installed upgrade chips/modules and larger advanced physical versions" of the Infuser and its sibling roles;
- "Larger chemical machines gain higher throughput and lower energy per unit; upgraded small machines remain useful".

**From the owner's plan:** the size and look come from the art brief ([Chemical Infuser](industrial-machine-models-and-textures.md#chemical-infuser)): "2×3×3 blocks; larger variant 4×6×6 blocks … The bulk form enlarges this exact chamber, its paired inlets and installed-bed compartment into a tall synthesis installation." The bulk profile is the foundation's selected one.

**This change's choice:** the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas and acid factory, after the Chemical Infuser
Primary specialty and supported player role: gas chemistry at scale (methane for the power station, hydrogen chloride, sulfur trioxide)

## Player experience

The **Bulk Chemical Infuser** is the Chemical Infuser twice as large every way, four wide, six deep and six tall, with all of its parts:
- the plinth;
- the stepped reaction chamber with its olive service plates, ceramic collar and crown;
- the catalyst-bed drawer at the foot of the front, with its handle and marker;
- the ringed inspection port, the heat gauge and the amber process band;
- the paired inlet branches on its shoulders, with their red-wheeled valves;
- the closed loop into the ribbed cooling jacket, its fan turning while it works;
- the product collar and water drain at the back;
- the junction box with the power socket.

The control box (the controller) keeps its own, human size at the front left.

It runs **everything the Infuser does**, four batches at once:
- hydrogen chloride;
- ethylene dichloride;
- methanation (carbon dioxide or carbon monoxide, or biogas upgrading) over a nickel or ruthenium bed;
- contact conversion over a vanadium bed.

One catalyst bed in its socket serves all four batches. Its tanks hold 16,000 mB each.

**Ports.** The same sides as the Infuser, each moved to the middle of a block of its larger body:
- the left inlet (hydrogen, or oxygen for contact conversion) on the left shoulder;
- the right inlet on the right shoulder;
- the product high at the back;
- the water low at the back;
- power at the junction box low on the right side, near the back.

**Guides.** Its Engineer's Handbook page describes it, and the screen labels its inlets as the Infuser's do.

## Connections

- **Existing input producers:**
  - hydrogen from the Separator and Cell;
  - chlorine from brine electrolysis;
  - carbon dioxide from the Oxidizer, Fermenters and Digester;
  - carbon monoxide from the Gasifier's rack;
  - sulfur dioxide and oxygen.
- **Existing output consumers:**
  - methane for the gas generators and the Combined Cycle Power Plant;
  - hydrogen chloride for the Bulk Chemical Reactor;
  - ethylene dichloride for the PVC route;
  - sulfur trioxide for contact acid.
- **Technology connection:** the shared form foundation's bulk profile and bed socket. It uses the Infuser's own capabilities and recipes, so no new recipe was needed.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains a Chemical Infuser, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Infuser stays the entry machine, and its upgrade cards keep it useful.

## Balance and automation

- **Throughput.** Four lanes give four times the Infuser's rate. This is what a Combined Cycle Power Plant on methane needs from a gas works.
- **Energy.** A lane pays 103 JE/t instead of 128, 20% less per batch.
  - Hydrogen chloride: 7,725 JE instead of 9,600.
  - Methanation: 20,600 instead of 25,600.
- **Same amounts.** It makes nothing more per batch than the Infuser.
- **Upgrade cards** fit its terminal, as on the Infuser.
- **No new loops.** Methane still costs four millibuckets of hydrogen. The loop audit (`tools/check_mod_data.py`) counts this form's profile saving with every card, and no fuel burns for as much as its cheapest electric making costs.

## Choices made here for review

- **The bill:** a Chemical Infuser, two Steel Tanks, four steel plates, steel fluid pipe and a basic circuit (`TIT / PFP / PCP`).
- **Its cells follow its model:** 110 blocks in the 4×6×6 envelope, the open corners above and behind the chamber kept free.
- **JEI.** It has its own recipe page with the Infuser's recipes; main's recipe view gives every form one.

## Multiplayer and persistence

- **Server authority.** The server decides everything.
- **Ports.** They check what goes in, and nothing can be put in at the outlets.
- **New registrations,** stable from now on: the block and item `jugcraft:bulk_chemical_infuser`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

**The model** is the Infuser's own (`chemical_infuser()` in `tools/industrial_forms.py`), doubled by `bulk_chemical_infuser()`:
- **Ports.** Doubling puts each port's middle on a block seam, so each port's pieces move half a block onto the middle of a block face (`BULK_INFUSER_PORT_SHIFTS`): the branch, valve and flange, or the box, socket and insulators.
- **Flanges.** Each flange is kept within one block.
- **The control box** keeps its size.

**Textures** are the Infuser's shared ones.

**Turning parts:** `bulk_chemical_infuser_fan` in `machine_rotor_quads.json`.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions beside the Infuser's.

**Server game tests for CI** (`BulkChemicalInfuserGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 110 blocks in every orientation, its open cells stay free, and only its five ports answer. |
| `hydrogenChlorideRunsFourBatchesAtOnce` | 2,000 mB of hydrogen chloride for exactly 30,900 JE. |
| `oneBedServesEveryBatch` | Four methanation batches run on one nickel bed, and the bed is still there afterwards. |
| `everyPartLightsWhileItWorks` | Every part lights while it works. |
| `itRunsTheInfusersRecipesAndIsBuiltFromItsBill` | It runs exactly the Infuser's recipes, four lanes at 80%, and the bill builds it. |

**Client game test** (`BulkChemicalInfuserClientGameTests`): three views, a night view, its screen while working and its screen when blocked by a full product tank.

### Results

**Run [38499715279](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38499715279) (commit 8a32c809) passed:** the build, the data checks, every server game test (`BulkChemicalInfuserGameTests` included) with and without optional integrations, and all five client test shards (`BulkChemicalInfuserClientGameTests` included).

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

- The bill awaits the owner's review.
- The brief's other larger variants follow: the Oxidizer (4×4×6), and the Separator once the owner settles its floor ([electrolysis floor](industrial-electrolysis-floor.md)).
