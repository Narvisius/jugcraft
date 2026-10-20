# Factory payoffs: the power station, bigger machines that pay, clearer tanks and the big milestones

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on [ammonium nitrate](industrial-ammonium-nitrate.md) and the branches below it.

Proposal issue: none. This change comes from the contributor's review of the factory chain for fun. Mastery, progression, purpose, milestones and strategic diversity were strong. Three things were weak:
- several big machines made exactly what a simpler machine already made;
- chains ended without a payoff;
- the screens named tanks wrongly.

The contributor asked for four things, all handled here:
- big machines that are worth building;
- some machines clearly bigger and more impactful than others, balanced around big power spikes;
- advancements only for the real big achievements;
- clearer screens.

Every number here is this change's choice, for the owner's review.

Owner: jimbozoomer-byte
Target milestone and tier: the steel-era factory up to the oil era's gas turbine
Primary specialty and supported player role: power generation and chemical processing

## Player experience

### The power ladder and its big spike

Each step is a real jump, and the last one is the factory's goal.

| Generator | Size | Output | Fuel |
|---|---|---|---|
| Coal Generator | 2×2×3 | 32 JE/t | coal |
| Steam Generator | — | 64 JE/t | coal and water |
| Bio Generator | 3×4×3 | 64–96 JE/t | biogas, bioethanol |
| Gas Burning Generator | 4×5×4 | 128–256 JE/t | hydrogen, methane |
| Gas Turbine | — | 512 JE/t | gasoline, refinery gas |
| **Combined Cycle Power Plant** | **6×4×5** | **2,048 JE/t** | methane, raw syngas (1,024 JE/t on hydrogen) |

**The Combined Cycle Power Plant** is the new top of that ladder, a 88-block landmark, as large as a form may be:
- **Layout.** A gas turbine on a long skid draws air through the filter house over its generator. Its exhaust raises steam in a tall heat-recovery boiler, which has a white steam drum on top and a stack. The steam turns a second turbine on the same shaft.
- **Methane.** It gives **528 JE per millibucket**, more than the 504 JE/mB large gas generator, at 2,048 JE/t. That is 8× the Gas Burning Generator and 4× the Gas Turbine.
- **Raw syngas.** Straight from the Coal Gasifier, it burns whole at 256 JE/mB and the same 2,048 JE/t. Coal, gasified and fired here, becomes a real power station. The cleanup rack is now for chemistry, not power.
- **Hydrogen.** It burns at 128 JE/mB at 1,024 JE/t.
- **Ports.** Fuel goes in at one flange low at the back. Power leaves at the generator's terminal on its left side.
- **Animation.** The shaft coupling turns while it runs.
- **The bill:** a Gas Turbine and a Heat Recovery Unit (the two halves of a combined cycle), two Steel Tanks, an advanced circuit, a Machine Casing and three steel plates. It comes after the Gas Turbine, so it crowns the oil era's power line.
- **Feeding it** takes a real gas works:
  - a full plant burns about 8 mB of syngas a tick, which is two Coal Gasifiers;
  - or about 3.7 mB of methane a tick, from a large digester with biogas upgrading, or methanation.

### Big machines that are worth building

| Machine | Before | Now |
|---|---|---|
| Industrial Fermenter | 250 mB of bioethanol from 8 crops, the same as the Chemical Reactor | **375 mB**, half again as much, from the same crops, time and energy, with its carbon dioxide still collected |
| Contact sulfuric acid (Oxidizer → Infuser → Bulk Chemical Reactor) | 500 mB of acid per sulfur dust, like the simple recipe, at six times the energy | **1,000 mB per dust**: absorption takes 250 mB of SO3 and 750 mB of water to make a bucket. It trades power and plant for sulfur |
| Hydrochloric leaching (Bulk Chemical Reactor) | 4 washed ores per ore in 160 ticks, like sulfuric leaching | the same 4 (the mod's ore ceiling), in **80 ticks**, and two at once: **four times the Chemical Reactor's leaching rate** |

### Clearer screens

The Chemical Infuser's tanks are now "Left inlet tank" and "Right inlet tank". Before, the left one was called "Hydrogen tank" even while it held oxygen for contact conversion.
- **How it works:** a form can now name a tank its own way (`container.jugcraft.form.<form>.tank.<role>`). Every other form keeps its names.
- **What hasn't changed:** the tooltip and the status line still name the fluid inside or the fluid wanted.

### Advancements only for the big milestones

Three new advancements, and no others for the new machines:
- **Something Rotten** (goal): build the 6×6×6 Anaerobic Digester. It sits under bioethanol's "Moonshine".
- **Contact Process** (goal): make a vanadium contact bed, which doubles the acid from your sulfur. It sits under "Oil of Vitriol".
- **Power Station** (challenge): build the Combined Cycle Power Plant. It sits under the Heat Recovery Unit's "Waste Not".

The Fermenter, Oxidizer, cleanup rack, Bio Generator and the other smaller machines earn no advancement of their own.

## Connections

- **Existing input producers:**
  - methane (CO2 or CO methanation, biogas upgrading);
  - raw syngas (the Coal Gasifier);
  - hydrogen (electrolysis, the cleanup rack);
  - the Gas Turbine and the Heat Recovery Unit for its bill.
- **Existing output consumers:** the electricity network. The Plant's family is the Gas Turbine's, which can send 2,048 JE/t out.
- **Technology connection:**
  - the shared form foundation, which gives the Plant its form, its fuel list and its turning shaft;
  - the existing recipe capabilities for the rebalances;
  - `MachineStatus.tankLabel` for the names.
- **Magic connection:** none.
- **Reachable entry path:** the Plant's bill comes after the Gas Turbine and the Heat Recovery Unit. Its fuels come from machines that are reachable before it. Nothing it makes is needed to build it.
- **Required vs optional:** optional. It is the strongest generator, not a gate.

## Balance and automation

- **No power loop.** `tools/check_mod_data.py` costs every fuel's cheapest electricity-only route.
  - **Hydrogen:** 128 JE/mB, below electrolysis's cheapest 168.
  - **Methane and raw syngas:** no electricity-only route. Their carbon comes from coal, crops or waste, so they are primary fuels.
- **Coal through the Plant.** One coal gives 500 mB of syngas.
  - That is 128,000 JE in the Plant, less the Gasifier's 17,400: about 110,600 JE.
  - The Steam Generator gives 102,400 JE from the same coal, and the Coal Generator 51,200.
  - So the Plant is a little better per coal, and its payoff is the rate: one machine makes what 32 Coal Generators make.
- **Methane upgrading.** Biogas burnt directly in the Bio Generator is still the most energy per waste. Upgrading it to methane and burning it in the Plant spends hydrogen to get rate. That is a choice, not a strict upgrade.
- **Acid.** Contact acid now gives twice the acid per sulfur, at about 31,100 JE per dust against the simple recipe's 4,800 for half as much. Every step can be piped.
- **The ore ceiling.** The mod's 4× ore ceiling (`ORE_LEACHING_MULTIPLIER`) is untouched. Hydrochloric leaching wins on speed, not yield.

## Choices made here for review

- **The Plant's figures:**
  - methane at 528 JE/mB and 2,048 JE/t;
  - raw syngas at 256 JE/mB and 2,048 JE/t;
  - hydrogen at 128 JE/mB and 1,024 JE/t;
  - its 6×4×5 size;
  - its bill.
- **The Fermenter's 375 mB.**
- **Contact absorption's 750 mB of water → 1,000 mB of acid.**
- **Hydrochloric leaching in 80 ticks.**
- **The three advancements**, their names and their places in the tree.
- **The Infuser's tank names.**

## Multiplayer and persistence

- The server decides everything.
- New registrations, stable from now on:
  - the block and item `jugcraft:combined_cycle_power_plant`;
  - the advancements `jugcraft:anaerobic_digester`, `jugcraft:vanadium_contact_bed` and `jugcraft:combined_cycle_power_plant`.
- The rebalanced recipes keep their IDs. Batches already running finish with their old results, which are saved in each lane.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The Plant's model is authored in `tools/industrial_forms.py` from the shared Jugcraft textures:
- gunmetal, olive, chrome, ribbed steel, the cream band, the owner's white tank body for the steam drum;
- hazard stripes, grilles, gauges, lamps and the methane still on the fuel line;
- its turning shaft coupling (`combined_cycle_power_plant_shaft` in `machine_rotor_quads.json`).

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py` (including the power-loop audit), `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the Plant was inspected from six directions.

For CI, server game tests:
- **`CombinedCyclePowerPlantGameTests`:**
  - `itStandsWholeAndOnlyItsPortsAnswer`: 88 blocks in every orientation, ports only. Methane and raw syngas go in; water and biogas do not.
  - `methaneBurnsAt2048JEATick`, `rawSyngasBurnsAt2048JEATick` and `hydrogenBurnsAt1024JEATick`: exact energy per millibucket and per tick.
  - `itIsTheBigPowerSpike`: 8× the gas generator's rate and more per millibucket; at least 4× the Gas Turbine; hydrogen below electrolysis's cost.
  - `theBigMilestonesAreAdvancements`: the three load, and the smaller machines have none.
  - `everyPartLightsWhileItRuns` and `itsBill`.
- **`IndustrialFermenterGameTests`:** 375 mB of bioethanol.
- **`BulkChemicalReactorGameTests`:** 1,000 mB of acid from 250 mB of SO3 and 750 mB of water; leaching done within 100 ticks.

Client game test: `CombinedCyclePowerPlantClientGameTests` takes five views (front left, front, back right, right side, dark) and two screens (working, store full).

### Results

- **The first run, [38466660997](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38466660997) (commit 719fba66), failed at startup.** The form foundation refuses machines wider than 6 blocks, and the Plant was drawn 7 wide. It is now 6×4×5, and `tools/check_mod_data.py` checks the 2–6 rule so this is caught locally.
- **The second run, [38471422864](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38471422864) (commit 38427fa5), failed one client test.** The Plant's 120,000 JE store fills in under three seconds at 2,048 JE/t, so after the picture test's 60-tick wait it stood idle with a full store. The test now empties the store before checking that it works.
- **The third run, [38494562417](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38494562417) (commit 94b0e85b), passed:** the build, the data checks, every server game test with and without optional integrations, and all five client test shards.

No survival playtest, two-client test or performance measurement has been done. The fun of these numbers needs a real playthrough to confirm.

## Rollout and open questions

- All figures above await the owner's review.
- Later bigger tiers (a larger digester, a bulk Separator under the electrolysis floor) should keep the same rule: each big machine is clearly better at something.
