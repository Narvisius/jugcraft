# Coastal magnesium, titanium processing and polymer molding

Owner-requested local playtest, 12 October 2026. Implemented with OpenAI Codex (GPT-6 family), on `feature/starter-chemistry-playtest` after the biorefinery/aluminum checkpoint. No push or main merge is authorized by this delivery; the owner wants to test new machinery locally first.

Follow-up: the [October 13 polymer factory](polymer-factory-playtest.md) adds named HDPE/LDPE, staged monomers, continuous extrusion and clean recycling. The record below describes the October 12 checkpoint.

## Implemented player routes

Coastal Seawater Intake -> Brine Concentrator -> Mineral Conditioning Station (hydroxide, then acid preparation) -> Chloride Drying Oven -> Magnesium Electrolysis Cell. Existing starter chemistry supplies lye and hydrochloric acid. Magnesium and the recovered chlorine supply titanium processing; players may trade for any intermediate.

Raw Titanium + coke + chlorine -> Titanium Chlorination Vessel -> existing Chemical Distillation Station -> Titanium Reduction Retort with magnesium -> existing Arc Furnace. The retort includes paid salt separation and sponge cleanup. Its two dry MgCl2 items return through the magnesium cell to recover exactly two magnesium and 500 mB chlorine. Existing titanium metal/tool/armor/component identities and earlier production recipes remain available.

Polymer Molding Press + one reusable mold -> housings, fittings, insulation, existing gaskets, flexible hoses or vehicle panels. Existing plastic pellets and PVC are explicitly interchangeable only for rigid housings/panels. Fittings require PVC; insulation, gaskets and hoses require rubber. Earlier steel plates and simple pattern items make the six molds; no advanced chip, magnesium, titanium or molded output is required to build its own producer. All eight new machines use earlier steel, copper, motors, tanks, basic circuits and ceramics.

## Coastal source rules

The three-wide, three-deep, two-tall intake's lower front-right mouth samples a 3 x 3 source-water patch extending forward, two blocks deep. Each column must be open to the sky and belong to vanilla `minecraft:is_ocean` or `minecraft:is_beach`; beach intakes also require a loaded ocean-biome sample within twelve blocks. The source water must be in the Overworld at `seaLevel - 1`. Flat worlds that report an unusable bottom-layer sea level use the normal Overworld surface reference Y=62 for creative testing; actual ocean generators retain their configured sea level. Checks apply to the water, not just the machine position. Existing tagged mod biomes participate through those tags. No biome tags or generated chunks are changed by gameplay.

A complete stroke produces 200 mB of distinct seawater for 320 JE, at most once per twenty ticks: average 10 mB/t and 16 JE/t. Pausing, missing structure, missing power, unsuitable source, incompatible output or less than 200 mB output space prevents collection. The scan is bounded to eighteen water blocks, nine biome/sky columns and at most twelve nearby biome samples once per second; only loaded chunks are queried. Source blocks are preserved as a renewable coastal resource. No per-water ownership history is stored: restoring water on a qualifying coast works, while an arbitrary inland or elevated placed pool does not. Ordinary piped water and sodium-chloride brine cannot supply magnesium. There are no speed/efficiency slots on this source sampler.

## Provisional processing receipts

All quantities are fictional game balance, not real industrial operating instructions. Heat, drying, separation and vacuum cleanup are included in paid processing; there is no pressure-control minigame.

| Stage | Input -> output | Ticks x JE/t |
| --- | --- | --- |
| Concentration | 1000 mB seawater -> 250 mB magnesium brine + 750 mB water | 200 x 32 |
| Hydroxide precipitation | 250 mB magnesium brine + 250 mB lye -> 1 hydroxide + 500 mB spent mineral wash | 160 x 24 |
| Chloride preparation | 1 hydroxide + 500 mB HCl -> 500 mB prepared MgCl2 solution + 250 mB water released from the solid charge | 160 x 32 |
| Drying | 500 mB prepared solution -> 1 dry MgCl2 + 500 mB water | 160 x 48 |
| Molten-salt electrolysis | 1 dry MgCl2 -> 1 Mg ingot + 250 mB chlorine | 400 x 64 |
| Titanium chlorination | 1 raw titanium + 1 coke + 500 mB chlorine -> 1000 mB crude chloride + 250 mB CO2 | 240 x 64 |
| Shared chloride distillation | 1000 mB crude chloride -> 750 mB purified chloride + 250 mB spent mineral wash | 240 x 48 |
| Reduction and cleanup | 750 mB purified chloride + 2 Mg ingots -> 1 clean titanium sponge + 2 dry MgCl2 | 400 x 96 |
| Wash settling in Conditioner | 500 mB spent mineral wash -> 1 existing salt + 250 mB water; remainder lost | 120 x 16 |
| Molding | 4 rigid stock -> housing/panel; 2 PVC -> 2 fittings; 2 rubber -> 3 insulation or 2 gaskets; 3 rubber -> hose | 120 x 32 |

One first magnesium ingot costs 50,240 JE including 1000 mB intake pumping and all magnesium stages, before producing its external lye/acid or settling wash. One titanium sponge consumes a new raw titanium and coke, plus 65,280 JE for chlorination/purification/reduction; recycling the two salt items adds 51,200 JE. Existing arc melting is additional. Recycling conserves the two magnesium units; it does not produce electricity or extra magnesium. CO2 capture at the chlorinator uses the existing collect/vent-excess policy, with Keep gas available. Chlorine and all useful liquid/solid products require storage; chlorine is never silently vented by the cell.

The earlier aluminum and ethanol systems remain compatible. Empty the Distillation Station's old feeds/products before changing chemical jobs; its role labels now say Distilled product / Distillation residue. The Conditioner also needs old tanks emptied between incompatible recipes. Entry forms retain 2000 mB tanks, one work lane and normal shared upgrades; electrolysis/reduction use the existing electrolysis family buffer. All registered transport faces, JEI categories, Handbook pages, Encyclopedia entries and acquisition goals are included.

## Molded products have current uses

- Housing + two steel plates -> one existing machine casing.
- PVC fitting + bronze fluid pipe + steel plate -> three existing steel fluid pipes.
- Insulation + two copper ingots + one tin ingot -> eight existing copper cables.
- Hose + steel fluid pipe + steel plate -> existing fluid valve.
- Vehicle panel + four coke -> four existing carbon-composite building/vehicle panels.
- Gaskets use their existing consumers. All older construction recipes remain available.

Molds stay in the dedicated shared tool socket, locked while their batch is running and retained after completion/cancellation. Ordinary transport faces cannot extract tooling. There is no additional offcut award from already fully allocated product feed. Grade-specific recycling, HDPE/LDPE/PTFE, extended ethylene/VCM routes, aluminum-magnesium alloys, new titanium equipment statistics and large bulk plants remain separate follow-ups.

## Appearance and provenance

New native models use the existing muted industrial steel, olive, ceramic, rubber and copper palette, with supported tanks, seated pumps, braced press columns, real feed chutes and short pipes terminating in their process bodies. Existing MachineRotors drive visible couplings/fans only while working. No new renderer, power system, dependency or runtime downloader.

The supplied owner library was searched. Six unchanged 16 x 16 mold textures come from `art/owner-library/originals/Blocks/`: `mold_packing_4.png`, `mold_rod.png`, `mold_wire.png`, `mold_gear.png`, `mold_bullet_casing.png`, and `mold_plate.png`, in housing/fitting/insulation/gasket/hose/panel order. Originals and provenance remain intact. Existing alumina/salt/aluminum icons represent the three new magnesium stocks; names distinguish these chemical identities. Five molded products have small native 3D item models using the existing material kit. New fluids use the established seeded fluid generator with appended identities. No owner-library program or JAR is executed.

## Validation

- Full Gradle build: all **1,410 required server game tests passed**. New tests cover every processing/molding receipt, exact energy and retained molds, rotated ports, coast eligibility and paid strokes, rejected freshwater/brine, blocked recovery, saved reduction and real construction recipes.
- Native client test passed for all eight machines: working/stopped rotation, four working views, menus and connected battery/cable test stations, producing 56 screenshots and a separate stocked test yard. Front and rear rendered views were inspected for supported geometry and complete textures.
- All 24 selected Python tests passed. Data, registration, recipe, port and repository checks passed. Art audit passed for 2,247 material IDs, 6,577 block/item models, 3,504 quad parts and 154 assembled models, with existing exception counts unchanged.
- Initial source tests exposed the flat generator's unusable bottom-layer sea level and delayed sky-light updates after placing a roof. The bounded creative-world fallback and synchronous motion-blocking height check resolve both; the complete final suite passed afterward.

Local delivery includes a separate saved yard, launcher, Modrinth test pack, source-commit record and SHA-256 checksums. Stations are independently stocked and paused for inspection; connecting them into a complete factory remains part of owner playtesting. Automated evidence does not substitute for owner playtesting, two-client testing, Modrinth app import or representative server-load measurement.
