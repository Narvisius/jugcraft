# Gas storage: the gas cylinder and the ammonia chiller

Status: implemented on `feature/gas-storage-35` (batch 35), stacked on `feature/electroplating-34`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for hydroponics, electroplating and hydrogen/ammonia storage next, each rethought for usefulness.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier, with the electrolytic cell (hydrogen) and the synthesis converter (ammonia).
Primary specialty and supported player role: logistics for chemistry, and building (ice roads).

## What was rethought
- **Not another big tank.** Gases already have the steel tank and the 1,024-bucket gas holder. What they lacked was a way to *carry* them: hydrogen to a fuel cell at an outpost, oxygen for a scuba tank on an expedition, nitrogen for the grapple, without running a pipe there.
- **A use for ammonia.** Ammonia only made fertilizer and nitric acid. Ammonia is the classic industrial refrigerant, so it now makes ice: anywhere, even in the Nether, where water cannot be frozen otherwise. Packing ice into blue ice is far cheaper than crafting, so fast boat roads no longer need a frozen ocean.

## Player experience
- **Gas Cylinder** (item, stacks to 1): holds 8,000 mB of one gas.
  - Use it on a tank, gas holder, pipe or machine to fill it from there. Sneak and use it to empty it into the block.
  - Machines also fill and empty it the way they do buckets, and the fluid filter can be set from it.
  - Used in the air with a scuba tank or pneumatic grapple in the other hand, it tops that up with oxygen or nitrogen.
  - It refuses liquids, and a second gas while it holds one. Its bar shows how full it is, in the gas's colour; the tooltip names the gas and the amount.
  - Crafted from five steel plates, a gasket and a fluid valve.
- **Ammonia Chiller** (one block, 24 JE/t): ammonia in its first tank, water in its second.

  | Input | Fluids | Output |
  | --- | --- | --- |
  | (empty slot) | 5 mB ammonia, 1,000 mB water | 1 ice |
  | 4 ice | 5 mB ammonia | 1 packed ice |
  | 4 packed ice | 5 mB ammonia | 1 blue ice |

  - Each batch takes 5 seconds. The 5 mB of ammonia is the refrigerant the loop loses.
  - It works in any dimension.
- Advancement **Ice Cold** (build an ammonia chiller). Handbook: a machine page and a "Gas Cylinders" page in the Chemistry chapter.

## Connections
- Input producer: hydrogen (electrolytic cell), ammonia (synthesis converter), oxygen and nitrogen (air separation unit), water, power.
- Output consumer: fuel cells, scuba tanks, grapples and any gas machine, through the cylinder. Ice for building, cooling and boat roads.
- Technology connection: Fabric fluid storage (blocks and items), fluid pipes, machine side configuration. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- The cylinder moves gas; it never makes any. It keeps exact droplets, so nothing is created or lost in rounding.
- Blue ice by chiller: 16 ice batches and 5 pressings (21 batches), about 50,400 JE and 105 mB of ammonia. By crafting: 81 ice. Ice melts back to only one water source per block, so there is no water or power loop.

## Multiplayer and persistence
- Server-side item and machine logic.
- The cylinder's contents are the existing synced, saved `jugcraft:stored_fluid` component.
- The chiller's tanks and progress are saved like other fluid processors.

## Dependencies and assets
- No new dependencies.
- Cylinder: a 64x64 high-detail item drawn with `tools/hd_art.py`.
- Chiller model (dieselpunk): an olive cabinet with chrome condenser coils, the ammonia receiver, the compressor under a fan grille, a frost-blue sight glass and an ice chute. Plus a classic front texture.
- All original.

## Verification
- `tools/check_mod_data.py`: the cylinder's capacity and the chiller's tanks match `tools/gas_storage.py`; the chiller's fluid spec and its recipes check like the other fluid machines.
- Game tests (CI):
  - `gasCylinderCarriesGas`: fills from a gas holder to 8,000 mB, refuses oxygen and water, empties into a fuel cell while sneaking, and tops up a scuba tank in the other hand.
  - `ammoniaChillerMakesIce`: water and ammonia make ice with 5 mB of ammonia used; four packed ice make blue ice; lava is refused.
- Not run: client play, the bar and tooltip in game, the Nether in play.

## World and event applicability
Not applicable.

## Rollout and open questions
- A cylinder rack block, and chilled coolant for other machines, are possible follow-ups.
- Values are first values to tune.
