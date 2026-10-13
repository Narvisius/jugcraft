# Liquid fuels: RP-1 kerosene, liquid oxygen and liquid rocket motors

Status: batch 43 is implemented on `feature/liquid-fuels-43`, stacked on `feature/booster-rails-42`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, approved "liquid fuels (kerosene, LOX)" in the rocketry list, and asked for them to be started while CI ran.
Owner: jimbozoomer-byte
Target milestone and tier: refinery and electronics tiers, after the batch 38 rocket workshop.
Primary specialty and supported player role: refining, power and rocketry.

## Owner-requested fuel-family expansion (7 October 2026)

The [industrial chemical catalog](industrial-chemical-catalog-and-routes.md#rocket-families-and-phase-conversion) records future methane/LOX, LH2/LOX and hydrazine/MMH/N2O4 roles alongside existing RP-1/LOX, with station/satellite uses. The owner selects Rotary Condensators for appropriate ordinary phase changes and upgraded Cryogenic Liquefiers for rocket liquids, building on this existing family. Exact per-fluid capabilities, cooling costs, recipes, progression and space compatibility remain to design; this plan preserves existing kerosene, oxygen and motor routes and implements no new launches or dimensions.

The [twelfth owner batch](industrial-chemistry-and-fuels-plan.md#grid-storage-synthetic-fuels-and-cryogenics-decisions-twelfth-batch), recorded 8 October 2026, selects independently obtainable cobalt/ceramic reusable tooling, one synthetic-crude feed for shared refining/upgrading, existing oxygen cooling followed by methane and then a more advanced hydrogen capability, insulated shared-tank variants, and hydrazine before MMH. Exact supplies, recipes, material/energy/conversion receipts, capability tiers and engine/oxidizer compatibility remain to specify. This adds no boil-off/maintenance mechanic, destination, launch or change to the current fuel/rocket behavior below.

## Player experience
- **RP-1 kerosene** (a straw-tinted liquid with a bucket), hydrocracked in the **catalytic cracker**.
  - Inputs: heavy fuel oil, and hydrogen in the tank where water normally goes, over the cracking catalyst.
  - A bucket of heavy fuel oil and 200 mB of hydrogen give 800 mB of kerosene (drawn off with the naphtha) and 100 mB of refinery gas.
  - When switching the cracker between cracking with water and hydrocracking, empty the naphtha tank, since the two products share it.
  - It is also jet fuel: 448 JE/mB in the gas turbine (as premium gasoline) and 480 in the advanced engine.
- **Cryogenic Liquefier** (crafted from an ammonia chiller, two fluid tanks, an electric motor, an advanced circuit and steel plates; 96 JE/t).
  - Condenses a bucket of oxygen into 250 mB of **liquid oxygen** (a pale blue liquid with a bucket) every 4 seconds.
- **Propellant tanks** (chemical reactor): a rocket casing and a bucket of kerosene make a **kerosene tank**; with liquid oxygen instead, a **liquid oxygen tank**.
- **Liquid rocket motors** (rocket workshop): a kerosene tank, a liquid oxygen tank and two rocket nozzles make three rocket motors.
  - These are the same motors every rocket uses, made without solid propellant, so no ammonium perchlorate, aluminum or rubber.
- Advancement **Minus 183** (build a cryogenic liquefier). Handbook: a Liquid Fuels page and the liquefier's machine page.

## Connections
- Input producer: the refinery (heavy fuel oil), the electrolytic cell (hydrogen), the air separation unit (oxygen), steel.
- Output consumer: every rocket (batches 38 to 41), and the gas turbine and advanced engine as fuel.
- Technology connection: the oil and gas lines and rocketry. Magic connection: none.
- Required vs optional: optional; solid propellant still works.

## Balance and automation
- **Motors:** two casings, two nozzles and a bucket each of kerosene and liquid oxygen make three motors. The solid route uses one casing, one nozzle and two solid propellant per motor, so the liquid route is cheaper in metal but needs the refinery, hydrogen and the liquefier.
- **Kerosene as fuel:** 800 mB at 448 JE/mB is 358,400 JE in the gas turbine, from a bucket of heavy fuel oil (128,000 JE burnt as it is) plus hydrogen and the cracker's power. That is in line with premium fuels.
- Neither fluid turns back into its inputs, so there is no positive-gain loop.

## Multiplayer and persistence
- Server-authoritative machines and fluids, saved as for every other fluid machine.

## Dependencies and assets
- No new dependencies.
- The two tank icons are 64x64 high-detail art (`tools/hd_art.py`).
- The liquefier model is dieselpunk: a frosted cold box with heat-exchanger stacks and a dewar.
- The fluid textures are generated from their colours like the other fluids.

## Verification
- `tools/check_mod_data.py`:
  - fuel values match `FluidFuels`;
  - the fluids are registered in `PetroFluids`;
  - the new recipes pass the fluid recipe audit (no recipe gives out more fluid than goes in).
- Game test `cryogenicLiquefierMakesLiquidOxygen` (CI):
  - a bucket of oxygen becomes 250 mB of liquid oxygen;
  - kerosene burns in the gas turbine and the advanced engine at its listed values.
- Not tested in CI:
  - hydrocracking in the multiblock cracker;
  - filling the tanks;
  - the motor recipe;
  - the visuals.

## World and event applicability
Not applicable.

## Rollout and open questions
- This completes the approved rocketry list (survey and weather rockets, flares, rocket post, zipline, launcher, booster rails, liquid fuels). Space launches remain on hold until the owner decides.
