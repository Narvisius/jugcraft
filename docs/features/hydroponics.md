# Hydroponics: the hydroponic bay and nutrient solution

Status: implemented on `feature/hydroponics-33` (batch 33), stacked on `feature/construction-32`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for hydroponics, electroplating and hydrogen/ammonia storage next, each rethought for usefulness.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier, with fertilizer (batch 24 chemistry) and the crop harvester (batch 9).
Primary specialty and supported player role: farming and base automation.

## What was rethought
- **Not "season-proof".** Seasons in Jugcraft are cosmetic: they never stop crops growing (`JugcraftSeasons`), so a greenhouse against the weather would add nothing.
- **Growing anywhere instead.** Fields need sunlight, farmland, water and space. The bay needs none of them: one block grows crops underground, in a vault, in the Nether or the End, fed by pipes and emptied by hoppers or conveyors.
- **A use for chemistry.** It runs on a nutrient solution made from fertilizer, so the phosphate and ammonia chains feed food production directly.
- **Coordinated with agriculture.** Every agriculture crop grows in it as well as the vanilla crops, as recipes gated by the `agriculture` switch. No agriculture code is changed.

## Player experience
- **Hydroponic Bay** (one block, 12 JE/t):
  - Put a seed or cutting in its slot and pipe nutrient solution into its 8,000 mB tank.
  - Every 30 seconds it harvests, using 100 mB, and gives the seed back. Grain seeds sometimes give a spare seed.
  - It needs no light, soil or water source, and works in any dimension. Upgrades speed it up as for other processors.
  - Results eject to its configured sides.
- **Nutrient Solution:** a fertilizer dissolved in a bucket of water in the chemical reactor makes 1,000 mB, enough for ten harvests. It comes in buckets and through pipes.
- **What grows** (per harvest):

  | Group | Yield per harvest |
  | --- | --- |
  | Wheat and beetroot | 2 |
  | Carrots and potatoes | 3 |
  | Melon | 6 slices |
  | Pumpkin | 1 |
  | Sugar cane, cocoa, sweet berries, nether wart and kelp | 3 each |
  | Bamboo | 4 |
  | Cactus | 2 |
  | Glow berries | 2 |
  | Mushrooms | 2 each |
  | Cotton | 2 |
  | Agriculture crops (beans, sweet potato, flax, onion, garlic, cabbage, oats, barley, turnip) | 2 to 3 |
  | Corn, ornamental corn, tomato, pepper, sunflower | 2 |
  | Every gourd and squash | 1 |

- Advancement **Soil Optional** (build a hydroponic bay). Handbook page in the Farming chapter.

## Connections
- Input producer: fertilizer (phosphate and ammonia chemistry) and water, through the chemical reactor; power; seeds from any farm.
- Output consumer: food chains, cooking, bioethanol fermentation (batch 26), cotton for guncotton, first aid kits and textiles.
- Technology connection: chemical reactor, fluid pipes, item logistics, machine upgrades. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- Per harvest: 7,200 JE plus 100 mB of solution (a tenth of a fertilizer).
- A field is free but needs space, light and time; the bay is fast and compact but costs power and fertilizer.
- The seed is kept, so a bay never runs out. Output is produce, which is not convertible back into fertilizer or power at a profit: bioethanol from 8 crops (about 5 harvests' worth) makes 250 mB, roughly 48,000 JE in a gas turbine, against about 36,000 JE spent. That is a small gain only after buying the bay, the fertilizer chain and the turbine, so it can't loop for free power.

## Multiplayer and persistence
Server-side machine logic, saved like other processors (tank and progress are saved). Recipes follow the `machines` switch, plus `agriculture` for its crops.

## Dependencies and assets
No new dependencies. Model (dieselpunk: a two-tier grow rack under violet grow lights, the olive nutrient tank at the back), classic front texture, fluid textures and bucket from the existing generators. All original.

## Verification
- `tools/check_mod_data.py`: the bay's tank and use per harvest match `tools/hydroponics.py`; 38 recipes and the fluid exist.
- Game test `hydroponicBayGrowsOnNutrients` (CI):
  - a bay with nutrient solution grows wheat and gives the seed back;
  - it refuses plain water;
  - a bay with no solution grows nothing.
- Not run: client play, many bays together.

## World and event applicability
Not applicable.

## Rollout and open questions
- Yields and timings are first values to tune.
- A larger multi-block farm, and grow lights for real fields, are possible follow-ups.
