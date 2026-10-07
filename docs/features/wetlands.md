# Wetlands (biomes batch 3)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation, farming and building (Discovery).
Primary specialty and supported player role: exploration and farming (cranberries, mud), building (willow wood).

## Player experience
Fourteen wet biomes, most in the **wetland** layout of Jugcraft regions. The swamps and bayous also grow in the woodland and wild layouts, where nothing else claims those climates.

| Biome | Replaces | What grows |
| --- | --- | --- |
| **Bog** | cool swamps (one weirdness half) | reddish-orange grass, maple scrub, tamaracks and thin bog firs, bushes and berry bushes; **cranberries** ripe in its shallow, muddy pools |
| **Dead Swamp** | cool swamps (the other half) | dark, muddy ponds, sparse dead trees and grey snags standing in them, fallen logs, cattails; no animals; villages |
| **Lush Swamp** | temperate swamps (one half) | vibrant grass and blue water, tall oaks hung with vines, willows, a few maples, cattails, ferns, berries |
| **Swamp Woods** | temperate swamps (the other half) | willows, vine-hung oaks and azalea trees over duckweed and lily pads; animals |
| **Bayou** | warm mangrove swamps | willows standing in the water, trailing vines like moss, cattails and ferns on mud; overcast |
| **Floodplain** | hot mangrove swamps | willow scrub with oaks among it, tall grass, orange cosmos; lily pads and watergrass in the floods |
| **Ghost Forest** | dark forests (one half; wetland layout) | grey snags and dead trees, with a living fringe of cedars, around many lakes |
| **Sludge Mire** | dark forests (the other half) | a dense canopy of dark oaks and big oaks over mud and sludgy, algae-covered pools; no animals; villages |
| **Lush River** | temperate and warm rivers (wetland layout) | duckweed, lily pads and watergrass; willow scrub, willows and a few oak bushes on the banks |
| **Fen** | old-growth spruce taiga (wetland layout) | thin bog firs, tamaracks and willow scrub over muddy pools, cattails and lily pads |
| **Lake District** | temperate forests (wetland layout) | oaks, firs and larches broken by many lakes with muddy shores |
| **Quagmire** | dry cool plains (wetland layout) | muddy flats and brown ponds, cattails; no trees, no animals |
| **Marsh** | cool plains (wetland layout) | a wide green plain of shallow lakes full of watergrass, ringed by cattails; no trees |
| **Wetland** | temperate plains (wetland layout) | grass and mud, ferns, cedars, willows and a few spruces, cattails, purple water; villages |

- Biomes O' Plenty's Deep Bayou merges into the Bayou, the Land of Lakes Marsh into the Marsh, and the Land of Lakes becomes the Lake District.
- Seasons: all fourteen change colour with the seasons; all but the Bayou, Floodplain and Lush River take winter snow when `seasons.snow` is on ([seasons.md](seasons.md)).
- **New plants:**
  - Cattail: a two-block reed of the water's edge.
  - Watergrass: grows under water like seagrass; only shears take it.
  - Duckweed: floats on still water like a lily pad.
- **New tree: the willow.** A short trunk under a broad crown whose leaves hang in long curtains, with vines trailing like moss. Its leaves follow the seasons: green from spring, yellow in autumn, bare in winter (the server's season clock, as for the maple). Full wood set; saplings.
- **New ground:** mud patches and muddy ponds (vanilla mud), and tall oaks hung with vines.

## Connections
- Input producer: world generation.
- Output consumer: agriculture (the Bog grows the agriculture branch's cranberries wild, ripe for picking: a reachable source besides planting), building (willow wood; vanilla mud to bricks), composting (the new plants).
- Technology connection: the sawmill and tree farm take willow like the other woods.
- Magic connection: none yet.
- Reachable entry path: swamps and mangrove swamps are common, and three of four layouts grow wetland swamps.
- Required vs optional: nothing here is required.

## Balance and automation
- Willow is ordinary wood (4 planks by hand, 6 in the sawmill). The plants compost like vanilla's seagrass and lily pads. Wild cranberries are the same block the agriculture branch farms; picking them works as on a farm.

## Multiplayer and persistence
- Server authority: world generation and the season clock are the server's.
- `biomes.enabled=false` stops the biomes in new chunks and the willow's hand recipes. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- Plants are registered from `/jugcraft/plants.json` (`tools/plants.py`): cattail is vanilla's DoublePlantBlock, watergrass `agriculture/WaterPlantBlock` (seagrass-like, our own), duckweed `agriculture/FloatingPlantBlock` (rests on water like a lily pad).
- Ponds are vanilla's lake feature filled with water and banked with mud; mud patches are vanilla's disk feature. Willows use vanilla's cherry foliage shape and vine decorator.
- Textures drawn by code in `tools/wild_textures.py`. No Mojang file is copied. Since the [wood repaint](wood-repaint.md), the willow's wood and leaves are drawn by `tools/wood_style.py`.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: plants, the willow (leaf schedule against every season mode, the seasonal decorator), biomes and their features, and non-overlapping rules.
- Server game tests (`BiomeGameTests`):
  - `seasonalForestSaplingsGrow` now grows a willow too: all its leaves in autumn colours in autumn.
  - `wetlandPlantsWork`: watergrass stands only in water and keeps its water; duckweed floats only on water; a cattail takes two blocks and drops one.
  - `regionLayoutFollowsItsRules`: every batch 3 rule places its biome in each of its layouts.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`), generated leaves in today's look, and screenshots of each wetland found.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37037252461](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37037252461) (commit 2809360f): failed to compile.** 26.3 has no `WaterlilyBlock`, so duckweed got its own `FloatingPlantBlock`.
- **Run [37038021655](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37038021655) (commit e62dd3a7): green.**
  - Server game tests: all 310 required tests passed.
    - `wetlandPlantsWork`: watergrass and duckweed stood only where they should, and a cattail dropped 1.
    - `seasonalForestSaplingsGrow`: a willow of 7 logs and 84 leaves, all 84 in autumn colours.
    - The wetland layout placed every batch 3 biome: Lake District 178 entries, Marsh 174, Lush River 160, Wetland 158, Fen 100, Sludge Mire 92, Ghost Forest 82, Bayou 80, Floodplain 80, Quagmire 80, Bog 40, Dead Swamp 40, Lush Swamp 40 and Swamp Woods 40. The cut vanilla table has 9,045 entries per layout.
  - Client game test, a real world with seed `jugcraft`. Twelve of the fourteen wetlands were within 6,400 blocks of the start:
    - Bog 1,740 blocks away, Dead Swamp 1,757, Lush River 1,847, Marsh 2,437, Lake District 2,691, Quagmire 4,249.
    - Wetland 4,786, Sludge Mire 4,790, Fen 5,013, Swamp Woods 6,004, Lush Swamp 6,052, Ghost Forest 6,291.
    - The Bayou and Floodplain (warm and hot mangrove swamps, which are rare) were not within 6,400 blocks.
    - Generated seasonal leaves near them were all in today's look. Willows were still green on day 275, as their schedule says (they turn on day 283).
  - Screenshots (2 October, autumn):
    - Swamp Woods: willows hung with leaves and vines over grassy terraces.
    - Lush Swamp: tall oaks in autumn colours. Fen: firs and dark oaks over muddy pools.
    - Lake District: lakes ringed with autumn-coloured trees. Ghost Forest: a lake with lily pads and grey dead trees beyond.
    - Lush River: oak bushes on its banks. Marsh: shallow water with cattails and watergrass on a green plain.
    - Wetland: mud patches in olive grass with firs and willows. Bog: mud and cattails.
    - Dead Swamp and Sludge Mire: the camera stood over their murky ponds and shows mostly water.
    - Quagmire: the shot shows dead trees, small firs and maple scrub, which the Quagmire does not grow. It most likely looks across its region's edge into a neighbouring Dead Forest (the same climate in another layout). The client test then preferred a spot where the camera's view has the biome too, but run [37041750692](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37041750692) took the same spot and the same view. It now logs the biome at the picture's centre, to tell a region edge from a placement fault. Open until that run reports.

## World and event applicability
- Biome fit: each biome takes the climate of the vanilla biome it replaces.
- Seasons: see above.
