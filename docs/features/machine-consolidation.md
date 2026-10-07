# Fewer chemistry machines, more uses for each

Status: implemented on `feature/chemistry-24` (batch 24), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 2 October 2026: "I want to condense how many complicated machines we have if we can we should reuse the machines we have but we should give them more uses that make sense I feel like maybe we are overwhelming the amount of machines?" They approved the plan in the feature list document, then asked to keep the flowback treatment unit: "I like how the flowbakc treatment unit looks can we think of more uses for it?"
Owner: jimbozoomer-byte
Target milestone and tier: oil, chemistry and electronics (steel tier and later)
Primary specialty and supported player role: chemistry

## Player experience
Five single-job machines are gone. Their recipes now run in machines the player already builds, so the oil and chemistry chains need fewer blocks and fewer recipes to learn. The mod goes from 65 machines to 60.

| Removed machine | Its job now runs in | How the player switches jobs |
| --- | --- | --- |
| Vacuum Distillation Unit | **Distillation Tower** | Pipe heavy fuel oil in instead of crude oil: 400 mB of lubricant from a fifth draw-off one block up, and two asphalt binder in the tower's new slot. |
| Catalytic Reformer | **Catalytic Cracker** | Pipe naphtha in instead of heavy fuel oil. Reforming now uses one cracking catalyst per bucket, like cracking: 900 mB of gasoline (new draw-off, one block up) and 100 mB of refinery gas (the gas draw-off). |
| Chemical Mixer | **Chemical Reactor** | Brine (2 salt + water) and fracking fluid (2 sand + dried kelp + water) are two more reactor recipes. |
| Oil Sand Extractor | **Settling Plant** (the flowback treatment unit, renamed) | Put oil sand or bitumen in its new input slot and pipe water in. |
| Crystal Grower | **Arc Furnace** | Put 4 silicon and a phosphate in the arc furnace; it now has two input slots. Argon piped into the controller doubles its speed, on every arc recipe. |

New uses for machines that stay:
- **Settling Plant** (kept for its look, at the owner's request): settling basins and a filter press now separate three things.
  - Flowback water → clean water + salt, as before.
  - Oil sand and bitumen + hot water → crude oil + sand, from the oil sand extractor.
  - Mud → 4 clay balls + 250 mB water: a steady clay supply for bricks and the coke oven.
- **Electrolytic Cell:** splits plain water, slowly. A bucket gives 500 mB of hydrogen (middle row) and 250 mB of oxygen (top row) in 40 seconds: an early fuel for the fuel cell, before brine or the air separation unit.

## Connections
- Every product keeps its consumers: lubricant (gas turbine), asphalt binder (roads, vanadium electrolyte), gasoline (turbine, advanced engine), brine (electrolytic cell), fracking fluid (fracking rig), crude oil (refining), silicon boules (sawmill wafers).
- Technology connection: fluid machines, pipes and draw-offs; the arc furnace multiblock; boost gases.
- Magic connection: none.
- Reachable entry path: every recipe moved to a machine of the same tier or earlier, so no product got harder to reach. The arc furnace (steel tier) now pulls boules that used to need the crystal grower (titanium, aluminum, advanced circuit), so electronics starts a little earlier.
- Required vs optional: the same as before for each product.

## Balance and automation
- **No product changed amount.** Every moved recipe keeps its inputs and outputs, except two.
  - **Reforming uses one cracking catalyst per bucket of naphtha.** It shares the cracker's catalyst slot, so both jobs can run from one stocked cracker.
  - **The silicon boule costs 25,600 JE instead of 51,200.** The arc furnace uses 64 JE/t, the crystal grower 128 JE/t, both for 400 ticks.
- **Water electrolysis is a loss:** 204,800 JE for 500 mB of hydrogen, worth 64,000 JE in the fuel cell (31%). Fluid processors take no upgrade cards, so nothing lowers that.
- **Mud to clay** matches vanilla: dripstone turns a mud block into a clay block, four balls. The water out is no gain, since water is infinite anyway.
- `check_mod_data.py` still checks every fluid recipe for fluid from nothing and metal conservation. It now also checks the new output-tank targets: they must exist, hold the amount, and never share a tank within one recipe.

## Multiplayer and persistence
- Server-side machines on the shared fluid-machine framework.
- **Removed blocks:** `oil_sand_extractor`, `vacuum_distillation_unit`, `catalytic_reformer`, `chemical_mixer` and `crystal_grower` are no longer registered. Any already placed would disappear from a world. Nobody has played yet, so they are removed outright, as proposed in the feature list. Keeping them as uncraftable legacy blocks is still possible if the owner wants it.
- **Changed slots:**
  - the arc furnace has 3 slots (was 2);
  - the distillation tower has 1 (was 0);
  - the settling plant has 2 (was 1);
  - the tower has 5 output tanks (was 4) and the cracker 4 (was 3).
  An old save's arc furnace would find its output in the new second input slot. This has not been exercised on a real save.
- **Recipe-format change:** fluid recipe results may name their output tank (`"tank": n`); results without it go by position, as before. Arc smelting recipes are now multi-ingredient (`"ingredients"`), so data packs that add arc furnace recipes need the new format.

## Dependencies and assets
No new dependencies. The removed machines' models and textures are deleted. The settling plant keeps its model and gets a new display name; its ID is unchanged.

## Verification
- `tools/check_mod_data.py` PASS (292 IDs) and `scripts/check_repository.py` PASS locally.
- Game tests (CI), new or rewritten:
  - `towerVacuumDistilsHeavyFuelOil`, `crackerReformsNaphtha`;
  - `reactorMixesFrackingFluid`, `reactorMixesBrine`;
  - `settlingPlantTanksOnlyTakeWhatTheyUse`, `settlingPlantWashesOilFromOilSand`, `settlingPlantPressesMudIntoClay`, `treatmentCleansFlowback`;
  - `cellSplitsWater`;
  - `arcFurnacePullsABoule`, `argonSpeedsUpTheArcFurnace`, which build the 3×3×3 arc furnace.
- The client test adds a screenshot of the distillation tower's screen, the one with the most outputs. The progress arrow now moves left when outputs crowd it.
- Not run: client play, two players, an old save with the removed machines.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- The settling plant's new name is the owner's to confirm; reverting it is a one-line change in `tools/machines.py`.
- More candidates if the owner wants fewer still: the polymerization reactor's jobs could fold into the synthesis converter, and the fuel cell could become a mode of the electrolytic cell.
