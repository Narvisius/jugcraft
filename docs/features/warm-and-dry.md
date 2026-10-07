# Warm and dry lands (biomes batch 4)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation and building (Discovery).
Primary specialty and supported player role: exploration; building (palm and cypress wood).

## Player experience
Fifteen warm or dry biomes. The meadow layout takes the dry grasslands and sandy country, the wild layout the harsher deserts and scrub, and the woodland layout two warm forests:

| Biome | Layout: replaces | What grows |
| --- | --- | --- |
| **Dryland** | meadow: dry savanna | hot, dry grassland under a lilac sky: pines, oaks and brush oaks, bushes, cacti, pillars of bone; desert villages |
| **Xeric Shrubland** | meadow: savanna plateau | low hills of sand and grass, small acacias, cacti, dead shrubs, little lakes |
| **Jacaranda Glade** | meadow: warm forest | blossoming jacarandas with cherries, oaks with bees and azaleas; blue orchids and lilies of the valley |
| **Lush Desert** | meadow: desert (one half) | orange dunes of red sand that see rain: dune grass, acacia brush, bushes, wildflowers; villages |
| **Bone Flats** | meadow: desert (other half) | dry, coarse flats with pillars of bone, dead bushes and dry grass |
| **Dry River** | meadow: hot rivers | a river between sandy banks with dead bushes |
| **Cold Desert** | meadow: dry snowy plains | gravel and coarse dirt, dry grass, hidden powder snow; too dry for snow; no animals; snowy villages |
| **Scrubland** | wild: dry savanna | flat scrub of dry grass and wildflowers, scattered oak bushes |
| **Lush Savanna** | wild: savanna | a field of poppies and rose bushes on grass blotched with coarse dirt; no trees, no animals |
| **Outback** | wild: desert (one half) | red sand patched with grass, tiny acacias and cacti, pools of water and lava; villages |
| **Oasis** | wild: desert (other half) | sand around pools of water, palms, grass and sugar cane |
| **Wasteland** | wild: badlands | dried salt flats (calcite) with rock-salt outcrops, dead trees and grey snags on its coarse dirt, dead grass; no animals; husks |
| **Burnt Forest** | wild: warm forest | grey snags and dead trunks on scorched grass and coarse dirt, fallen logs, ash drifting in the air |
| **Mediterranean Forest** | woodland: warm forest | tall cypresses, oaks and dark oaks, shrubs, peonies; villages |
| **Orchard** | woodland: temperate plains | the agriculture branch's chestnut trees, oaks with bees and flowering azaleas, rose bushes, daisies |

- Biomes O' Plenty's Seasonal Orchard merges into the Orchard (and its yellow trees are the Aspen Glade's, batch 1).
- Seasons: the Jacaranda Glade, Mediterranean Forest and Orchard change colour with the seasons (the Orchard also takes winter snow when `seasons.snow` is on); the rest are tropical, arid or frozen and do not.
- **New trees:**
  - The palm: a tall trunk that bends as it rises, a flat crown of fronds. It grows on sand as well as grass.
  - The cypress: a tall, narrow column of dark scale-leaves.
  - Both have full wood sets and saplings. Small desert acacias use vanilla acacia wood.
- **Ground:** these biomes lay their own ground where the climate calls for it: red sand and red sandstone, sand, coarse dirt, gravel with powder snow, and calcite salt flats.

## Connections
- Input producer: world generation.
- Output consumer: building (palm and cypress wood); rock salt (the salt ore) shows at the surface in the Wasteland (the same ore, which needs a stone pickaxe); the agriculture branch's chestnuts grow wild in the Orchard.
- Technology connection: the sawmill and tree farm take palm and cypress like the other woods.
- Magic connection: none yet.
- Reachable entry path: savannas and deserts are common; each layout is a quarter of Jugcraft regions.
- Required vs optional: nothing here is required. The Wasteland's surface salt is a convenience; salt also comes from rock salt underground and from brine, as before.

## Balance and automation
- Palm and cypress are ordinary wood. Rock salt outcrops are small and rare (a few blocks every few chunks of Wasteland) and drop what rock salt ore always drops. No new conversions, no loops.

## Multiplayer and persistence
- Server authority: world generation is the server's.
- `biomes.enabled=false` stops the biomes in new chunks and the new woods' hand recipes. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- **Surfaces:** Jugcraft's material rule (`jugcraft:overworld/surface`, generated from the biomes' "surface" in `tools/biomes.py`) sets the floor and the layer under it, with noise patches, in Jugcraft biomes that ask for it. It runs first through a small override of vanilla's top-level `minecraft:overworld` material rule, which lists vanilla's named parts by reference with Jugcraft's added. Another mod or data pack that also replaces `minecraft:overworld`'s top-level rule would conflict with this one (the last loaded wins); vanilla's own surface rules are not touched.
- Trees use vanilla's bending, forking and straight trunk placers and acacia and spruce foliage. Textures drawn by code in `tools/wild_textures.py`. No Mojang file is copied. Since the [wood repaint](wood-repaint.md), their woods and leaves are drawn by `tools/wood_style.py`.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: trees, biomes and their features, non-overlapping rules, generated rules and surfaces current.
- Server game tests (`BiomeGameTests`): `warmTreesGrow` grows a palm and a cypress from saplings (sizes logged); `regionLayoutFollowsItsRules` checks every batch 4 rule.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`), and screenshots of each biome found, which show whether the surfaces generate.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37038318322](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37038318322) (commit 9654ad30): green.**
  - Server game tests: all 311 required tests passed.
    - `warmTreesGrow`: a palm of 8 logs and 55 leaves, and a cypress of 10 logs and 38 leaves. `jacarandaSaplingsGrow`: 12 logs and 78 leaves.
    - Every batch 4 rule placed its biome:
      - Meadow layout: Lush Desert 400 entries, Bone Flats 308, Jacaranda Glade 220, Dryland 180, Xeric Shrubland 128, Cold Desert 90, Dry River 80.
      - Wild layout: Wasteland 432, Outback 400, Oasis 308, Burnt Forest 220, Lush Savanna 180, Scrubland 180.
      - Woodland layout: Mediterranean Forest 220, Orchard 158.
    - The data packs loaded with Jugcraft's material rule override in place.
  - Client game test, a real world with seed `jugcraft`. Eight of the fifteen were within 6,400 blocks of the start:
    - Orchard 999 blocks away, Cold Desert 1,033, Lush Savanna 1,176, Scrubland 1,242, Burnt Forest 1,260, Mediterranean Forest 1,865, Jacaranda Glade 2,673.
    - Dryland was found 8,016 blocks away (the search covers a square).
    - Not within 6,400 blocks: the seven that replace deserts, badlands, savanna plateaus and hot rivers (Lush Desert, Bone Flats, Outback, Oasis, Wasteland, Xeric Shrubland and Dry River). The client test now also logs how near the vanilla deserts, badlands and savanna plateaus are, to tell a climate missing near the start from a placement fault.
  - Screenshots (2 October, autumn):
    - Dryland: lilac sky, pines and oaks, bone pillars, sand by the water.
    - Jacaranda Glade: violet jacarandas among cherries and oaks.
    - Scrubland: dry grass and bushes on a coastal slope. Lush Savanna: poppies and rose bushes on grass and coarse dirt.
    - Burnt Forest: charred trunks on coarse dirt under grey, ashy air.
    - Mediterranean Forest: dark cypress columns among oaks in autumn colours. Orchard: chestnut trees and azaleas.
    - Jugcraft's own surfaces generate: coarse dirt in the Burnt Forest and Lush Savanna.
    - The Cold Desert was buried in snow: vanilla lays snow on any freezing biome as the land is made. It now drops that step and has no precipitation.
- **Run [37041750692](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37041750692) (commit 44f70069): green.**
  - The Cold Desert now shows bare gravel and coarse dirt.
  - The climate check found no vanilla desert or badlands within 6,400 blocks of this seed's start, so the seven biomes that replace them could not be near either. It found a savanna plateau 5,772 blocks away; no Xeric Shrubland was found, as layout and share make the replaced climate rarer still.

## World and event applicability
- Biome fit: each biome takes the climate of the vanilla biome it replaces.
