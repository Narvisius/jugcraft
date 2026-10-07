# Refinery upgrades: premium fuels, sulfur recovery and heat recovery

Status: implemented on `feature/refinery-29` (batch 29), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for more useful chemistry and picked, from Claude's list, the energy ideas in this order: refinery upgrade → sulfur recovery → combined-cycle heat recovery ("rethink some of them if they seem not very useful").
Owner: jimbozoomer-byte
Target milestone and tier: oil line (steel tier and later), after the diesel generator and gas turbine.
Primary specialty and supported player role: power and refining; it rewards a refinery built as one connected web.

## Player experience
- **Hydrotreater** (new, two by two and three tall, its catalyst bed built in, 128 JE/t):
  - **Hydrotreating:** 1,000 mB diesel + 100 mB hydrogen → 1,000 mB **premium diesel** (drawn off the base) + 100 mB **hydrogen sulfide** (drawn off the top), every 6 seconds.
  - **Blending:** 900 mB gasoline + 100 mB bioethanol → 1,000 mB **premium gasoline**, every 2 seconds.
- **Premium fuels** burn better everywhere diesel or gasoline burns:

  | Machine | Premium diesel | Premium gasoline | Before |
  | --- | --- | --- | --- |
  | Diesel generator | 320 JE/mB | — | diesel 256 |
  | Diesel engine | 320 KE/mB | — | diesel 256 |
  | Gas turbine | — | 448 JE/mB | gasoline 384 |
  | Advanced engine | 400 KE/mB | 512 KE/mB | diesel 320, gasoline 448 |

- **Sulfur recovery** (the chemical reactor, the Claus process): 200 mB hydrogen sulfide → 1 sulfur dust. Oil becomes a renewable-per-barrel source of sulfur for every acid recipe.
- **Heat Recovery Unit** (new, one block, two tall):
  - Put it against a running **diesel generator** or **gas turbine**. It boils water in their exhaust and makes **30%** of their output again: 77 JE/t from a diesel generator, 154 JE/t from a gas turbine.
  - Needs water: 1 mB per 64 JE, piped in or from a water source directly below it (20 mB/t).
  - Needs lubricant: 1 mB every 40 ticks it runs.
  - The heat is taken from the generator, so two units on one generator share it; a unit touching two generators takes from both.
- Two advancements: **Top Shelf** (premium fuel) and **Waste Not** (heat recovery unit).
- Handbook: hydrotreater, Premium Fuels and heat recovery pages in the Oil chapter; both machines in the Oil progression step.

## Connections
- Existing input producers: diesel and gasoline (distillation tower, cracker), hydrogen (electrolytic cell, from brine or water), bioethanol (chemical reactor, from crops), lubricant (distillation tower), water.
- Output consumers: every fluid generator and engine; sulfur dust feeds sulfuric acid, which feeds fertilizer, leaching, lithium, rare earths, lithography and vanadium electrolyte.
- New uses for existing by-products: the brine cell's hydrogen, the tower's lubricant, farm bioethanol.
- Technology connection: power (generators, engines), chemistry (acids). Magic: none.
- Required vs optional: all optional upgrades. Nothing that worked before needs them.

## Balance and automation
| Step | Cost | Gain |
| --- | --- | --- |
| Hydrotreating a bucket of diesel | 15,360 JE + 100 mB hydrogen | +64,000 JE in the diesel generator; 100 mB H₂S (half a sulfur dust) |
| Blending a bucket of premium gasoline | 5,120 JE | 448,000 JE against 364,800 for the gasoline and bioethanol apart (+83,200) |
| Heat recovery on a gas turbine | water, 1 mB lubricant / 40 ticks | +154 JE/t on the turbine's 512 |

- Hydrogen from water costs about 41,000 JE per 100 mB (410 JE/mB), so hydrotreating with electrolysed water hydrogen nets only about +7,600 JE a bucket. With the brine cell's by-product hydrogen it nets about +48,600. The design rewards using by-products.
- No loops: every gain uses up oil or crops. The heat recovery unit makes power only from heat a fuel-burning generator produced this tick or the last, never from its own output, and two units cannot both take the same heat.
- A diesel bucket yields 100 mB hydrogen sulfide, half a sulfur dust, which makes 250 mB of sulfuric acid: enough to leach one ore.

## Multiplayer and persistence
- Everything runs on the server. The exhaust a generator has banked is not saved (it is at most two ticks old); the unit's two small remainders are not saved either.
- New fluids are ordinary Jugcraft fluids (premium fuels have buckets; hydrogen sulfide is a gas, tanks and pipes only).
- Recipes follow the `crude_oil` switch (plus `salt` for hydrotreating, `machines` for blending, `sulfur` for sulfur recovery). The machines need `machines`.

## Dependencies and assets
No new dependencies. Models in both styles (dieselpunk in `tools/dieselpunk_models.py`, classic in `tools/large_machines.py`), front textures in `tools/generate_textures.py`, fluid textures from `tools/petro.py` colours. All original.

## Verification
- `tools/check_mod_data.py`: fluids and gases match Java; fuel values match `FluidFuels`; tank specs, footprints and recipe tanks match; no recipe makes fluid from nothing.
- Game tests (CI):
  - `hydrotreaterMakesPremiumDiesel`, `hydrotreaterBlendsPremiumGasoline`, `reactorRecoversSulfur`, `premiumDieselBurnsBetter`;
  - `heatRecoveryUnitUsesTurbineExhaust`: about 30% of the turbine's output, 1 mB of water per 64 JE, nothing without water;
  - `heatRecoveryUnitsShareOneTurbine`: two units together never exceed one unit's share.
- Client screenshots: both machines appear in the multi-block row (`jugcraft_multiblocks_*`).
- Not run: client play, two players, a long-running refinery.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- The 30% recovery, the premium fuel values and the 200 mB per sulfur dust are first values for the owner to tune.
- The heat recovery unit does not attach to the diesel engine or advanced engine (shaft power), the fuel cell or the steam generator.
