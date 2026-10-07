# Mountains, coasts and volcanoes (biomes batch 6)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation (Discovery).
Primary specialty and supported player role: exploration; mining (surface coal, volcanic stone).

## Player experience
Thirteen biomes of mountains, shores and seas. A biome in Minecraft 26.3 does not shape the land: its height and steepness come from the climate. So each of these takes a vanilla biome whose land already has the right shape (peaks, shattered hills, badlands plateaus, beaches, deep seas) and gives it new ground, plants and air. The **wild** layout of Jugcraft regions takes the mountains and hills; the **wetland** layout takes the coasts and islands; both take the frozen and deep seas.

| Biome | Layout: replaces | What it is |
| --- | --- | --- |
| **Volcano** | wild: stony peaks, windswept savannas | blackstone and basalt slopes cracked with magma, lava pools banked with blackstone, basalt spires, drifting ash under a grey sky; no animals |
| **Canyon** | wild: wooded badlands | cliffs banded in terracotta with grassy ledges, pines and spruce scrub, pools of lava |
| **Highland** | wild: windswept hills | high, treeless grassland: tall grass, coarse dirt, gravel and stone, mossy boulders; sheep, cattle and rabbits |
| **Basin** | wild: windswept gravelly hills | a barren basin of gravel, stone and andesite with dead bushes; no animals |
| **Shield** | wild: windswept forests | humps of bare stone and andesite with seams of coal at the surface, firs, pines, spruces, larches and aspens, and lakes |
| **Karst Pinnacles** | wild: jagged peaks | steep peaks clothed in grass, pines and spruce scrub, pale limestone (calcite) showing through, a grey-green sky; pandas |
| **Hot Springs** | wild: groves | pines over warm, turquoise pools banked with calcite, blackstone showing through; trail ruins |
| **Ice Sheet** | wetland and wild: frozen seas | a frozen sea under ice and floes of packed ice; polar bears |
| **Ocean Trench** | wetland and wild: deep and deep cold seas | a dark floor of deepslate, gravel and obsidian under dim, inky water |
| **Gravel Beach** | wetland: cool beaches | a shore of gravel; no animals |
| **Dune Beach** | wetland: temperate beaches | sand with dune grass and **sea oats** |
| **Overgrown Beach** | wetland: warm beaches | grass, oak scrub, tall grass and sea oats, sand between |
| **Flower Isle** | wetland: mushroom islands | a peaceful island of grass, sunflowers, poppies, meadow flowers and hydrangeas; no monsters spawn |

- Biomes O' Plenty's Arctic and Icy Hills merge into the Ice Sheet, the Canyon Ravine into the Canyon, the Knoll, Mountain and Mountain Foothills into the Highland, the Volcano Edge into the Volcano, the Jade Cliffs become the Karst Pinnacles, the Oceanic Abyss the Ocean Trench, and the Flower Island the Flower Isle.
- The beaches keep vanilla's beach structures (buried treasure, beached shipwrecks), and the Ocean Trench vanilla's deep-sea ones (ocean monuments, ruins).
- Seasons: the Highland, Shield, Hot Springs and Flower Isle change colour with the seasons; the Highland, Shield and Hot Springs take winter snow when `seasons.snow` is on.
- **New plant:** sea oats, a tall grass of the dunes that stands on sand as well as soil.

## Connections
- Input producer: world generation.
- Output consumer: mining (coal at the surface in the Shield; blackstone, basalt, magma and tuff in the Volcano; calcite in the Hot Springs and Karst Pinnacles; obsidian in the Ocean Trench), building, composting (sea oats).
- Technology connection: none new; the surface coal is ordinary coal ore.
- Magic connection: none yet.
- Reachable entry path: mountains, beaches and seas are common; each layout is a quarter of Jugcraft regions.
- Required vs optional: nothing here is required.

## Balance and automation
- The Shield's surface coal is small seams (a few blocks every other chunk), the same coal ore as underground. The Ocean Trench's obsidian is small patches on a deep floor. No new conversions, no loops.

## Multiplayer and persistence
- Server authority: world generation is the server's.
- `biomes.enabled=false` stops the biomes in new chunks. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- Surfaces come from Jugcraft's material rule ([warm-and-dry.md](warm-and-dry.md)); the Canyon uses vanilla's badlands bands (`minecraft:bandlands`) by reference, and the beaches vanilla's sand-or-sandstone and gravel-or-stone rules.
- Sea oats are `agriculture/DunePlantBlock`: vanilla's two-block plant, standing wherever vanilla's dry grass can (`#minecraft:supports_dry_vegetation`).
- Lava and hot pools are vanilla's lake feature; ice floes and coal seams vanilla's disk feature; basalt spires vanilla's block column feature. Vanilla's lava lake, pines and spruces by reference.
- Textures drawn by code in `tools/wild_textures.py`. No Mojang file is copied.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: biomes and their features, non-overlapping rules, generated rules and surfaces current, and Java registering every plant kind.
- Server game tests (`BiomeGameTests`): `seaOatsGrowOnSand` (on sand, not on stone, two blocks, drops one); `regionLayoutFollowsItsRules` checks every batch 6 rule.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`), and screenshots of each biome found.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37041750692](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37041750692) (commit 44f70069): green.**
  - Server game tests: all 314 passed.
    - `seaOatsGrowOnSand`: sea oats stood on sand and not on stone; broken, they dropped 1.
    - Every batch 6 rule placed its biome:
      - Wild layout: Hot Springs 312 entries, Volcano 296, Canyon 288, Shield 142, Karst Pinnacles 120, Highland 114, Basin 96.
      - Wetland layout: Dune Beach 110, Gravel Beach 110, Overgrown Beach 110, Flower Isle 100.
      - Both layouts: Ice Sheet 40, Ocean Trench 40.
  - Client game test, a real world with seed `jugcraft`. Eleven of the thirteen were within 6,400 blocks of the start:
    - Ice Sheet 515 blocks away, Ocean Trench 590, Shield 1,619, Gravel Beach 2,023, Overgrown Beach 2,172, Dune Beach 2,748.
    - Hot Springs 2,988, Karst Pinnacles 3,548, Highland 3,791, Volcano 3,815, Basin 6,407.
    - Not within 6,400 blocks: the Canyon and the Flower Isle. Their vanilla climates are rare (wooded badlands, mushroom islands), and no vanilla badlands at all lie within 6,400 blocks of this start.
  - Screenshots (2 October, autumn):
    - Volcano: a dark blackstone and basalt peak with magma and ash under a grey sky.
    - Highland: grassy terraces with tall grass and stone. Basin: grey gravel and stone slopes.
    - Hot Springs: pines and calcite banks. Karst Pinnacles: pale calcite among grass.
    - Ice Sheet: floes of packed ice on a frozen sea. Ocean Trench: dark, inky water over kelp.
    - Dune Beach: sand with grass, beside a neighbouring forest. Shield: a close-up of grass.
    - The Gravel and Overgrown Beaches' shots looked out over the sea from the shore; the client test now prefers dry ground.

## World and event applicability
- Biome fit: each biome takes the climate and the land shape of the vanilla biome it replaces.
- Seasons: see above.
