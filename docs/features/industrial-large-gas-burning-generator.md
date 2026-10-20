# Large Gas Burning Generator: the Gas Burning Generator's 5×6×5 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [Large Bio Generator](industrial-large-bio-generator.md) and the branches below it.

Proposal issue: none. TODO items:
- "Turn the briefs into detailed models and textures … preserving the familiar family appearance in larger variants";
- the owner's direction to make some machines bigger and more impactful than others, with big power spikes.

**From the owner's plan:** the size comes from the art brief ([Gas Burning Generator](industrial-machine-models-and-textures.md#gas-burning-generator)): "4×5×4 blocks; larger variant 5×6×5 blocks … The larger form scales the same engine-generator train, preserving its recognizable division."

**This change's choices:** the fuel figures and the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas factory, after the Gas Burning Generator
Primary specialty and supported player role: gas power at scale

## Player experience

The **Large Gas Burning Generator** is the Gas Burning Generator drawn larger, five blocks wide, six deep and five tall. It keeps the same train:
- the skid;
- the round intake with its turning fan;
- the ribbed olive combustion section under its arched pale heat shield;
- the guarded coupling;
- the large ribbed generator housing;
- the tall terminal cabinet at the rear right;
- the exhaust along the left;
- the capped gas inlet on the right.

The ignition box (the controller) keeps its own, human size at the front left. The open space above the skid stays open, as on the smaller machine.

What it adds is power:
- **Four times the rate.**
  - Hydrogen: 128 JE/mB at 512 JE/t, against the Gas Burning Generator's 128 JE/t.
  - Methane: 504 JE/mB at 1,024 JE/t, against 448 at 256.
- **A bigger tank.** It holds 8,000 mB, four buckets.
- **The same layout as the small one.**
  - The gas inlet is low on the right side, the second block back.
  - Power leaves at the socket on the back of the terminal cabinet.
- **Motion.** The intake fan and coupling turn while it runs.
- **Guides.** Its Engineer's Handbook page describes it.

## Connections

- **Existing input producers:**
  - hydrogen from the Electrolytic Separator and Electrolytic Cell;
  - methane from the Infuser's methanation, from CO2 or the Gasifier's CO.
- **Existing output consumers:** anything on a power cable.
- **Technology connection:** the shared form foundation's generator forms. Its fuels are on the form; the gas turbine family gives its store (120,000 JE) and its 2,048 JE/t output limit.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains a Gas Burning Generator, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Gas Burning Generator stays the entry machine.

## Balance and automation

These figures are this change's choice, flagged for the owner's review.

**The power ladder on methane:**

| Machine | JE/mB | JE/t |
| --- | --- | --- |
| Gas Burning Generator | 448 | 256 |
| Large Gas Burning Generator | 504 | 1,024 |
| Combined Cycle Power Plant | 560 | 2,048 |

The plant stays the factory's big power spike; the large generator is the step between.

- **Hydrogen gets no better per millibucket.** A higher value would beat electrolysis's cost, a power loop, which the data check refuses. It only burns faster.
- **Methane gets an eighth more.** Making a millibucket takes four of hydrogen, which costs more than 504 JE even by the cheapest electrolysis.
- **Fuel use.** A bucket of methane lasts about 25 seconds (492 ticks).

## Choices made here for review

- **The fuel figures above.**
- **The bill:** a Gas Burning Generator, two Steel Tanks, four steel plates, a steel gear and a basic circuit (`TBT / PGP / PCP`).
- **Its cells** follow its model, as the smaller one's do: 101 blocks in the 5×6×5 envelope, the rest left open and kept free.

## Multiplayer and persistence

- **Server authority.** The server decides everything.
- **Ports.** The inlet takes only its two gases, and the socket only gives power.
- **New registrations,** stable from now on: the block and item `jugcraft:large_gas_burning_generator`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

**The model** is the Gas Burning Generator's own (`gas_burning_generator()` in `tools/industrial_forms.py`), scaled by `large_gas_burning_generator()`:
- five quarters across and up, six fifths deep;
- the ignition box kept at its size;
- the stretched gauges, lamps and socket kept inside one block each.

**Its cells** are worked out from where the model reaches (`form_layers`). The same rule reproduces the smaller generator's cells exactly.

**Textures** are the shared ones already used by the Gas Burning Generator.

**Turning parts:** `large_gas_burning_generator` in `machine_rotor_quads.json`.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions beside the Gas Burning Generator's.

**Server game tests for CI** (`LargeGasBurningGeneratorGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 101 blocks in every orientation, its open cells stay free, and only its two ports answer. |
| `hydrogenBurnsAt512JEATick` | Hydrogen gives 128 JE per millibucket, at 512 JE a tick. |
| `methaneBurnsAt1024JEATick` | Methane gives 504 JE per millibucket, at 1,024 JE a tick. |
| `everyPartLightsWhileItRuns` | Every part lights while it runs. |
| `itsFuelsAndItsBill` | The ratios to the Gas Burning Generator hold, the plant still beats it, and the bill builds it. |

**Client game test** (`LargeGasBurningGeneratorClientGameTests`): four views, the dark view, its screen while working and its screen with a full store.

### Results

**Run [38497894808](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38497894808) (commit ba67868a) passed:** the build, the data checks, every server game test (`LargeGasBurningGeneratorGameTests` included) with and without optional integrations, and all five client test shards (`LargeGasBurningGeneratorClientGameTests` included). The first attempt's client shard 3 failed in `PeepoDeliveryClientTests` (the companion's "meal" wait timed out), a test this change does not touch; that shard passed on its one re-run.

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

The figures and the bill await the owner's review.
