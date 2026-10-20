# Large Bio Generator: the Bio Generator's 5×6×5 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [Bulk Fermenter](industrial-bulk-fermenter.md) and the branches below it.

Proposal issue: none. TODO items:
- "Turn the briefs into detailed models and textures … preserving the familiar family appearance in larger variants";
- "Larger chemical machines gain higher throughput and lower energy per unit; upgraded small machines remain useful".

**From the owner's plan:** the size comes from the art brief ([Bio Generator](industrial-machine-models-and-textures.md#bio-generator)): "3×4×3 blocks; larger variant 5×6×5 blocks".

**This change's choices:** the fuel figures and the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the bio line, after the Bio Generator
Primary specialty and supported player role: farming into power at scale

## Player experience

The **Large Bio Generator** is the Bio Generator drawn larger, five blocks wide, six deep and five tall. The family is plain at a glance:
- the skid;
- the ringed olive combustion body, with its round inspection cover and pale heat shields;
- the guarded coupling and the ribbed generator housing;
- the radiator and its fan at the back;
- the fuel box on the right and the terminal box on the left;
- the exhaust folded back over the top.

The instrument panel (the controller) keeps its own, human size at the front left.

What it adds is power:
- **More from each fuel.** It gets a quarter more from each millibucket than the Bio Generator.
- **Four times the rate.**
  - Bioethanol: 240 JE/mB at 384 JE/t, against the Bio Generator's 192 at 96.
  - The Digester's biogas: 335 JE/mB at 256 JE/t, against 268 at 64.
- **A bigger tank.** It holds 8,000 mB, four buckets.
- **The same layout as the small one.** The fuel inlet is low on the right side; the power socket is low on the left side, on the terminal box. Both sit one block behind the middle.
- **Motion.** The coupling and radiator fan turn while it runs.
- **Guides.** Its Engineer's Handbook page describes it.

## Connections

- **Existing input producers:**
  - bioethanol from the Industrial and Bulk Fermenters and the Chemical Reactor;
  - biogas from the Anaerobic Digester.
- **Existing output consumers:** anything on a power cable.
- **Technology connection:** the shared form foundation's generator forms. Its fuels are on the form; the diesel generator family gives its store (60,000 JE) and its 1,024 JE/t output limit.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains a Bio Generator, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Bio Generator stays the entry machine.

## Balance and automation

These figures are this change's choice, flagged for the owner's review.

- **Fuel value.** It gets a quarter more from each fuel, the same gain the Combined Cycle Power Plant has over the gas generator.
  - Bioethanol stays below the advanced engine's 256 JE/mB, so that engine is still the best use of it.
  - Biogas at 335 JE/mB stays below methane's 448 in the gas generator.
- **Rate.** Four times the Bio Generator, the bulk machines' four-batch step. At 384 JE/t it outruns the gas generator on methane (256 JE/t) but stays far below the Combined Cycle Power Plant's 2,048, which remains the big power spike.
- **Fuel use.** A bucket of bioethanol lasts 31 seconds (625 ticks), against 100 seconds in the Bio Generator, so it wants a fermenting line behind it.
- **No loops.**
  - Bioethanol is made from crops (the Chemical Reactor spends 38,400 JE fermenting a bucket) and returns 240,000 JE a bucket here.
  - Biogas comes from farm waste.
  - Neither can be made from power alone, so no fuel here burns for more than its making costs.

## Choices made here for review

- **The fuel figures above.**
- **The bill:** a Bio Generator, two Steel Tanks, four steel plates, a steel gear and a basic circuit (`TBT / PGP / PCP`).
- **The full box.** Every block of the 5×6×5 envelope is part of the machine, as with the Bulk Fermenter, so it needs a clear 150-block space.

## Multiplayer and persistence

- **Server authority.** The server decides everything.
- **Ports.** The inlet takes only its two fuels, and the socket only gives power.
- **New registrations,** stable from now on: the block and item `jugcraft:large_bio_generator`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

**The model** is the Bio Generator's own (`bio_generator()` in `tools/industrial_forms.py`), scaled by `large_bio_generator()`:
- five thirds across and up, one and a half deep;
- the instrument panel kept at its size;
- the fuel flange and power socket redrawn at the middle of their ports' blocks;
- the stretched gauges kept inside one block each.

**Textures** are the shared ones already used by the Bio Generator.

**Turning parts** are `large_bio_generator_coupling` and `large_bio_generator_fan` in `machine_rotor_quads.json`.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions beside the Bio Generator's.

**Server game tests for CI** (`LargeBioGeneratorGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 150 blocks in every orientation, and only its two ports answer. |
| `bioethanolBurnsAt384JEATick` | Bioethanol gives 240 JE per millibucket, at 384 JE a tick. |
| `biogasBurnsAt256JEATick` | Biogas gives 335 JE per millibucket, at 256 JE a tick. |
| `everyPartLightsWhileItRuns` | Every part lights while it runs. |
| `itsFuelsAndItsBill` | The fuel figures are a quarter better and four times as fast as the Bio Generator's, bioethanol stays below the advanced engine, and the bill builds it. |

**Client game test** (`LargeBioGeneratorClientGameTests`): four views, the dark view, its screen while working and its screen with a full store.

### Results

**Run [38496961410](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38496961410) (commit 2fc145e0) passed:** the build, the data checks, every server game test (`LargeBioGeneratorGameTests` included) with and without optional integrations, and all five client test shards (`LargeBioGeneratorClientGameTests` included).

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

- The figures and the bill await the owner's review.
- The brief's other larger variants follow the same pattern.
