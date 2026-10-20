# Stainless steel and structural alloys

Technology package 2, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Player outcome

Produce stainless fittings, ceramic-lined vessels and precision valves for chemical
equipment, or aluminum-magnesium frames and panels for construction and vehicles.
Both specialties can trade intermediates and use existing machinery. This package
adds **zero machines**, power systems or mandatory personal research gates.

Existing nickel, steel, aluminum and seawater-magnesium supply chains remain the
starting paths. No stainless or structural-alloy part is needed to build the
equipment that makes the first batch. Package 1 supplies alumina plates and
precision shafts through its independent ceramic/finishing routes.

## Regional resource and processing

Chromite generates in new Overworld chunks beneath windswept hills, windswept
gravelly hills, windswept forest, stony peaks, stony shores, Jugcraft Shield and
Highland surface biomes. The existing surface-linked ore feature uses size 32,
rarity 10, a -32 to 64 height range and outcrop chance 4. These are generator
parameters, not a guarantee of finding ore at each location. Stone/deepslate
variants use the existing mineral loot, Fortune and Silk Touch patterns; an iron
pickaxe or better is required. Existing chunks are not rewritten. Players can
explore new terrain or trade for chromite.

| Input | Existing station | Output | Base duration |
|---|---|---|---|
| 2 chromite + 250 mB water | Mineral Conditioner | 2 chromite concentrate | 160 ticks, 32 JE/t |
| 2 concentrate + 1 coke | Arc Furnace | 2 ferrochromium ingots | 320 ticks |
| 6 steel + 2 ferrochromium + 1 nickel ingot | Alloy Smelter | 9 stainless steel ingots | 400 ticks |
| 7 aluminum + 1 magnesium ingot | Alloy Smelter | 8 aluminum-magnesium ingots | 240 ticks |
| 1 alloy ingot | Metal Press | 1 corresponding plate | Existing plate process |
| 1 stainless plate | Metal Press | 2 stainless fittings | 120 ticks |
| 1 aluminum-magnesium plate | Metal Press | 1 formed panel | 120 ticks |
| 1 unfinished valve + 125 mB water; retained polishing head | Precision Finisher | 1 precision valve + 125 mB returned water | 240 ticks, 48 JE/t |
| 2 formed panels | Rocket Workshop | 1 existing rocket casing | 160 ticks |

Arc Furnace, Alloy Smelter, Metal Press and Rocket Workshop keep their existing
operating rules and power requirements. Conditioner water is consumed. Finisher
water returns to its separate outlet using package 1's cooling-loop implementation.
No chromium-only refinery, special alloy oven or duplicate polishing machine is added.

Ferrochromium is alloying stock, not pure chromium. The 9-unit and 8-unit alloy
batches are approximate whole-ingot **game recipes**, not certified grades or
real-world metallurgical formulas. Alloying conserves their input metal counts;
there is no reverse separation recipe. All three metals have ingots, nuggets and
storage blocks using the existing 9:1 conversions. Chromite also has a storage block.

## Components and immediate uses

- Four stainless plates and two finished alumina plates craft one ceramic-lined
  stainless vessel.
- One stainless fitting, gasket and precision shaft craft one unfinished valve.
  Finish it with the reusable felt polishing head to obtain a precision valve.
- Four aluminum-magnesium plates and one precision shaft craft two lightweight frames.
- Fittings provide an alternate Steel Fluid Pipe recipe. A vessel plus an existing
  Fluid Tank provides an alternate Steel Tank recipe.
- Vessels and valves provide alternate Bulk Chemical Reactor and Lined
  Fluorochemical Reactor assemblies. The lined reactor still requires PTFE
  seals, fittings and a liner; this does not bypass PTFE production.
- Frames provide an alternate Zeppelin assembly retaining its five hydrogen lift
  cells, diesel engine and chest. Frames and panels provide an alternate Howitzer
  Walker assembly retaining the Diesel Walker core, dispenser and two pistons.
- Panels feed existing rocket casings through the Rocket Workshop.

These recipes produce existing equipment with unchanged models, capacities,
handling and behavior. They do not add hidden weight, corrosion or speed bonuses.
All original recipes remain available. No old registration IDs, inventories,
connection ports, vehicle controls or save schemas are replaced. New recipe IDs
allow both routes to coexist, with shared recipe previews and locks where the
existing station supports them. Titanium alloys and additional armor/tool tiers
remain future work.

## Resources and documentation

Canonical materials live in `tools/materials.py`; package recipes, component names,
guides and crafting recipes live in `tools/structural_alloys.py`. Java registration
extends the existing material families, plate list, PetroItems and worldgen list.
The normal resource generators include this package. `tools/write_alloy_resources.py`
authors its resources and refreshes dependent shared guides/recipes without deleting
generated resource trees.

New component silhouettes are original 16×16 text pixel maps, rendered by
`tools/structural_alloys_art.py` with the repository's native icon system. Ingots,
nuggets, plates, storage blocks and chromite reuse the repository's established
material geometry/ore-overlay pipeline with new restrained metal palettes.
No external asset pack or owner-library executable was introduced. Handbook,
Encyclopedia and shared recipe-view data describe both supply routes and consumers.

## Release verification checklist

- Build, registration/resource audits, icon/style review and server/client suites.
- Ore provinces, surface-versus-cave biome selection, height, feature disabling,
  outcrops, mining tiers, Fortune/Silk Touch and untouched existing chunks.
- Independent survival producers for nickel, coke, steel, aluminum, magnesium,
  ceramic plates, shafts and polishing heads; no self-output construction gates.
- Nine/eight metal-unit alloy batches, 9:1 storage/nugget cycles, yields and recipe
  cost comparison against existing vehicle/equipment construction paths.
- Machine feature gates, preview quantities, locks, automation, retained tools,
  blocked outputs, returned coolant, pause/restart and save/reload behavior.
- Optional-mod compatibility and the pre-existing recipes, equipment and vehicles.
- Owner-led local playtest and multiplayer review before publication or merge.

Current automated results are in the integration record. Representative factory
performance and human survival balance remain unverified.
