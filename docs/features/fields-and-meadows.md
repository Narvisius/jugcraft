# Fields and meadows (biomes batch 2)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)), and then to carry on through every batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation and building (Discovery).
Primary specialty and supported player role: exploration, farming and building; open land for settling.

## Player experience
Nine open biomes grow in the **meadow** layout of Jugcraft regions (a quarter of them), each in place of the vanilla biome with the same climate:

| Biome | Replaces (meadow layout) | What grows |
| --- | --- | --- |
| **Field** | cool forest | flat land with teal grass, small spruces and clumps of oak bush, sunflowers and many flowers; foxes; villages |
| **Flower Meadow** | flower forest | no trees; vanilla flowers and wildflowers on almost every block |
| **Grassland** | cool plains (moister) | no trees; blue-green grass carpeted with clover |
| **Heathland** | savanna (moister) | brown-tinged grass, heather and wildflowers, pines and oak bushes; wild horses |
| **Lavender Field** | sunflower plains | lavender and tall lavender everywhere, jacarandas in bloom and oaks with bees |
| **Lush Grassland** | sparse jungle, warm plains | small jungle trees and oak bushes among orange cosmos and oxeye daisies; parrots; villages |
| **Prairie** | temperate plains (one weirdness half) | tall grass and goldenrod, oak bushes and the odd big oak; villages |
| **Shrubland** | temperate plains (the other half) | dense oak bushes and bushes, alliums, no trees; wild horses |
| **Steppe** | dry cool plains | tan, dry grass and dead bushes, nothing taller; horses, donkeys and llamas |

- Biomes O' Plenty's Forested Field and old Field merge into the Field, and its Thicket into the Shrubland.
- Seasons: all nine change grass and leaf colours with the seasons ([seasons.md](seasons.md)). Seven take winter snow when `seasons.snow` is on; the Heathland and Lush Grassland have mild winters and do not.
- **New plants** (each with an item; every small flower also has a potted form and makes dye):
  - Lavender (purple dye), and Tall Lavender, two blocks high (two purple dye).
  - Goldenrod (yellow dye), Heather (magenta dye) and Orange Cosmos (orange dye).
  - Clover: a flowerbed of up to four clumps, like pink petals; no dye.
- **New tree: the jacaranda**, a forked trunk under a wide, flat crown of violet blossom that flowers all year. It has a full wood set (grey bark, rosy planks) and drops saplings.
- Oak bushes (one oak log in a ball of oak leaves) dot the Field, Heathland, Prairie, Shrubland and Lush Grassland.

## Connections
- Input producer: world generation (Jugcraft regions, meadow layout).
- Output consumer: building (the jacaranda wood set), dyes (four new dye sources), bees and honey (every new plant is a bee flower and in `#minecraft:flowers`), suspicious stew (the small flowers), composting.
- Agriculture: the open, flat land and villages suit farms; the plains-based biomes keep vanilla's pumpkins, sugar cane and wild crops of their base.
- Technology connection: the sawmill saws jacaranda logs into 6 planks, and the tree farm grows jacaranda saplings, as for the other woods.
- Magic connection: none yet.
- Reachable entry path: a quarter of Jugcraft regions use the meadow layout, and each biome replaces a common vanilla one.
- Required vs optional: nothing here is required.

## Balance and automation
- Plants are vanilla flowers in behaviour: one dye per small flower, two per tall flower (vanilla's rates); bone meal on Tall Lavender drops a copy, as on a lilac. Compost chances follow vanilla's flowers (clover as low as grass).
- The jacaranda is ordinary wood: 4 planks per log by hand, 6 in the sawmill. No new conversions, no loops.

## Multiplayer and persistence
- Server authority: world generation is the server's.
- `biomes.enabled=false` stops the biomes in new chunks and the new hand recipes (dyes, jacaranda wood). Every block, item and biome stays registered.
- Existing worlds: new chunks only; see [biome-regions.md](biome-regions.md).

## Dependencies and assets
- Plants are registered alike from the generated `/jugcraft/plants.json` (`tools/plants.py`, `JugcraftAgriculture.registerWildPlants`): vanilla's FlowerBlock and TallFlowerBlock, and `GroundCoverBlock` (ground cover that works like pink petals), with vanilla properties. Models use vanilla's cross, potted-cross and flowerbed shapes by reference.
- Textures drawn by code in `tools/wild_textures.py`; nothing read, traced or recoloured.
- Biomes start from vanilla bases by reference (`tools/biome_bases.py`) and add their own plants (`tools/biomes.py` EXTRAS). No Mojang file is copied.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: every plant has its blockstate, models, item, name, loot and tags; the rules do not overlap; every tree feature and placed feature a biome names exists.
- Server game tests (`BiomeGameTests`):
  - `wildPlantsWork`: each small flower is a flower that stands on grass, with a potted form and an item; Tall Lavender takes two blocks and drops one; a clover patch of four drops four.
  - `jacarandaSaplingsGrow`: a sapling grows a jacaranda (sizes logged).
  - `regionLayoutFollowsItsRules`: every batch 2 rule places its biome in the meadow layout.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`) to each biome, and screenshots of all nine from above.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37035901054](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37035901054) (commit b48ea821): failed to compile.** 26.3's `FlowerBedBlock` takes another constructor argument, so clover got its own `GroundCoverBlock`.
- **Run [37036379281](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37036379281) (commit bfc35e1f): green.**
  - Server game tests: all 309 required tests passed. `wildPlantsWork`: tall lavender dropped 1, a clover patch of four dropped 4. `jacarandaSaplingsGrow`: a jacaranda of 11 logs and 86 leaves. Every batch 2 rule placed its biome in the meadow layout (Field 172 entries, Grassland 172, Heathland 178, Lush Grassland 172, Flower Meadow 80, Prairie 80, Steppe 80, Lavender Field 76, Shrubland 76). The four layouts covered 2,515, 2,414, 2,033 and 1,860 of 16,384 samples.
  - Client game test, a real world with seed `jugcraft`: all 18 Jugcraft biomes were within 6,400 blocks of the start. Batch 2's: Grassland 550, Field 607, Lush Grassland 2,020, Prairie 2,176, Heathland 2,485, Steppe 2,489, Shrubland 3,303, Lavender Field 3,724 and Flower Meadow 3,765 blocks away. Generated seasonal leaves near them were all in today's look.
  - Screenshots (2 October, autumn): the Prairie shows goldenrod and tall grass under a big oak in autumn colours; the Steppe tan, dry grass; the Lavender Field lavender on grass below snowy peaks; the Heathland pines, oak bushes and wildflowers; the Field small spruces and oak bushes among yellow flowers; the Shrubland oak bushes in grass; the Lush Grassland oak bushes on a warm shore; the Flower Meadow a hillside of poppies and tulips. The Grassland shot mostly shows the snowy Muskeg next to it.

## World and event applicability
- Biome fit: each biome takes the climate of the vanilla biome it replaces, so it borders what that biome bordered.
- Seasons: see above.
