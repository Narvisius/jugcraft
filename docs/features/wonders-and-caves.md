# Wonders and caves (biomes batch 7)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only, and its invented names are replaced by our own.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation (Discovery).
Primary specialty and supported player role: exploration; decoration (glowing plants).

## Player experience
Fifteen rare, strange lands and two cave biomes, spread over all four layouts of Jugcraft regions on climates the earlier batches left free, so each layout has a few wonders:

| Biome | Layout: replaces | What it is |
| --- | --- | --- |
| **Cinder Barrens** | meadow: badlands | a burnt-out waste of ash-grey tuff and gravel, smouldering with magma, grey snags on its coarse dirt, blood-red water, lava pools, ash in the air; no animals |
| **Elder Vale** | meadow: mushroom islands | a vale of the old world: bright, simple grass, plain oaks, poppies and dandelions under a clear sky; no monsters spawn |
| **Frostlight Garden** | woodland: dry snowy plains | firs and birches in the snow, frost irises and glimmerblooms, a cold, shimmering sky; no monsters spawn |
| **Gilded Shrubland** | wetland: savanna plateaus | golden grass and golden-leaved oak scrub, goldenrod, dry grass, little lakes |
| **Glimmer Grove** | wild: flower forests | jacarandas and giant red mushrooms, glimmerblooms, alliums, lilacs and hydrangeas under pink air, pink water; only witches spawn |
| **Gloomweald** | wild: dark forests | dark oaks, dead trees and giant mushrooms over leaf litter and toadstools, dark purple pools, a dim sky |
| **Glowcap Grotto** | wetland: dripstone caves | a cave of mud floors grown with **glowcaps** that light the dark, moss and glow lichen |
| **Hallowed Bog** | meadow: swamps | a pale, bright bog of willows and young white aspens standing in its water, lilies of the valley and daisies, clear blue water; no monsters spawn |
| **Highsun Meadow** | wetland: savannas, warm plains | sunny, golden-green grass, sunflowers, goldenrod and wildflowers, a few small oaks |
| **Mycelial Jungle** | wild: jungles | huge red and brown mushrooms, jungle bushes and oaks over grass and mycelium, toadstools, spore-green air; mooshrooms |
| **Shrine Springs** | wild: bamboo jungles | great oaks two blocks wide, warm pools banked with calcite, ferns, dark green grass |
| **Snowpetal Grove** | wild: dry snowy plains | blossoming cherries and white-trunked aspens in the snow, snowpetals and clover, mossy boulders, snowflakes on the air |
| **Spider Nest** | wild: dripstone caves | a cave strung with cobwebs from ceiling to floor; spiders and cave spiders |
| **Starlit Wood** | meadow: dark forests | soaring birches and tall firs, glimmerblooms and lilies of the valley, motes of light under a twilight-blue sky |
| **Toadstool Field** | woodland: mushroom islands | mycelium patched with grass, giant mushrooms, toadstools and glowcaps; mooshrooms, and no monsters, as on vanilla's mushroom islands |
| **Webwood** | meadow: mangrove swamps | willows, dead trees and vine-hung oaks strung with cobwebs, toadstools and cattails, grey air; spiders everywhere |
| **Wild Greens** | wetland: warm forests | a riot of tall grass, clover, ferns and wildflowers over swathes of coarse dirt; no trees; cattle, sheep and chickens |

- Biomes O' Plenty's names become ours: the Deadlands become the Cinder Barrens, the Origin Valley the Elder Vale, the Auroral Garden the Frostlight Garden, the Sublime Shrubland the Gilded Shrubland, the Mystic Grove the Glimmer Grove, the Ominous Woods the Gloomweald, the Glowing Grotto the Glowcap Grotto, the Blessed Bog the Hallowed Bog, the Majestic Meadow the Highsun Meadow, the Fungal Jungle the Mycelial Jungle, the Sacred Springs the Shrine Springs, the Snowblossom Grove the Snowpetal Grove, the Wondrous Woods the Starlit Wood, the Fungal Field and Fungi Forest the Toadstool Field, the Silkglade the Webwood and the Overgrown Greens the Wild Greens.
- Seasons: the Hallowed Bog and Wild Greens change colour with the seasons and take winter snow when `seasons.snow` is on. The others keep their own colours all year.
- **New plants:**
  - Glowcap: a cluster of pale mushrooms with glowing caps (light 10). It grows on any sturdy floor: stone, mud, deepslate or soil.
  - Glimmerbloom: a violet, star-shaped flower that glows softly (light 7). Magenta dye; suspicious stew gives glowing.
  - Frost iris: a pale icy-blue iris. Light blue dye; suspicious stew gives slow falling.
  - Snowpetals: white petals on the ground, like pink petals.
- **New tree shape:** the great oak, a trunk of vanilla oak two blocks wide under a huge round crown (no sapling grows it).

## Connections
- Input producer: world generation.
- Output consumer: decoration and light (glowcaps and glimmerblooms give light without power), dye, composting, mooshrooms (stew) in the Mycelial Jungle and Toadstool Field.
- Technology connection: none new.
- Magic connection: none yet. These are candidates for the magic branch's ingredients and sites ([CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md)); nothing depends on them yet.
- Reachable entry path: each is rare by design, but every one replaces a vanilla climate that exists in most worlds. The two caves replace dripstone caves, which are common.
- Required vs optional: nothing here is required.

## Balance and automation
- Glowcaps and glimmerblooms are light sources found in the wild, dimmer than a torch (14). They are only light; nothing is crafted from them yet. Cobwebs in the Spider Nest and Webwood are vanilla cobwebs (a sword gives string, shears the web). No new conversions, no loops.
- Spawning: some biomes change their monster list (none in the Elder Vale, Frostlight Garden and Hallowed Bog; only witches in the Glimmer Grove; mostly spiders in the Spider Nest and Webwood). Mob caps are vanilla's.

## Multiplayer and persistence
- Server authority: world generation and spawning are the server's.
- `biomes.enabled=false` stops the biomes in new chunks and the new plants' recipes. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- Glowcaps are `agriculture/FloorPlantBlock` (any sturdy floor, gives light). Glimmerbloom and frost iris are vanilla's `FlowerBlock`, the glimmerbloom with a light level; snowpetals are `GroundCoverBlock`.
- Cave features follow vanilla's lush caves: floors and ceilings found by scanning from random heights. The grotto's mud is vanilla's vegetation patch feature, its moss vanilla's moss patch. Cobwebs are vanilla's simple block feature. Giant mushrooms are vanilla's, placed where an oak sapling could stand.
- Air colours, particles (vanilla's white ash, snowflakes and end rod motes) and water colours are biome attributes.
- Textures drawn by code in `tools/wild_textures.py`. No Mojang file is copied.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: plants, biomes and their features, non-overlapping rules, generated rules and surfaces current, and no placed feature with a biome filter nested inside another feature (it would stop chunk generation).
- Server game tests (`BiomeGameTests`): `glowingPlantsWork` (glowcaps glow at 10 on bare stone; the glimmerbloom glows at 7 and the frost iris not at all, both with potted forms; four snowpetals drop four); `regionLayoutFollowsItsRules` checks every batch 7 rule.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`), and screenshots of each surface biome found. The two caves are found by the 3D search but not photographed.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37042688480](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37042688480) (commit 81609a4c): server green; client job cancelled at the 30-minute limit.**
  - Server game tests: all 315 passed.
    - `glowingPlantsWork`: glowcaps gave light 10 on bare stone, the glimmerbloom 7; a patch of four snowpetals dropped 4.
    - Every batch 7 rule placed its biome:
      - Meadow layout: Cinder Barrens 432 entries, Starlit Wood 174, Hallowed Bog 160, Webwood 160, Elder Vale 100.
      - Wetland layout: Highsun Meadow 448, Wild Greens 220, Gilded Shrubland 128, the Glowcap Grotto cave 50.
      - Woodland layout: Toadstool Field 100, Frostlight Garden 90.
      - Wild layout: Mycelial Jungle 248, Gloomweald 174, Shrine Springs 106, Snowpetal Grove 90, Glimmer Grove 80, the Spider Nest cave 50.
  - Client game test, a real world with seed `jugcraft`. The search finished: thirteen of the seventeen were within 6,400 blocks of the start.
    - Hallowed Bog 1,089 blocks away, Glimmer Grove 1,286, Starlit Wood 1,494, the Spider Nest 1,537, Highsun Meadow 1,802, Wild Greens 1,832.
    - Frostlight Garden 2,235, Shrine Springs 2,931, Mycelial Jungle 3,008, Gloomweald 3,228, Snowpetal Grove 3,781, the Glowcap Grotto 4,917, Gilded Shrubland 6,337.
    - Not within 6,400 blocks: the Cinder Barrens (badlands, none near this start), the Elder Vale and Toadstool Field (mushroom islands) and the Webwood (mangrove swamps, the nearest vanilla one 4,457 blocks away).
  - The test then hung while finding a surface spot for the Gloomweald, after the Glimmer Grove's, and the job was cancelled at its 30-minute limit, so there are no screenshots.
  - Cause, found in run [37044032454](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37044032454)'s log: a chunk-generation worker threw "Tried to biome check an unregistered feature, or a feature that should not restrict the biome", so that chunk never finished. The Starlit Wood's tree selector picked vanilla's `minecraft:birch_tall`, a biome's top-level placed feature with a biome filter, which cannot be placed inside another feature. It now picks `minecraft:super_birch_bees` (commit a4b96787), and `check_mod_data.py` fails on any nested placed feature with a biome filter.

- **Run [37048482576](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37048482576) (commit a4b96787): green.**
  - Server game tests: all 316 passed, with the same `glowingPlantsWork` results.
  - Client game test: no chunk-generation error. The same thirteen were found at the same distances, and each surface biome was photographed. The two caves were found but, as caves, not photographed.
  - Screenshots (2 October, autumn):
    - Glimmer Grove: violet jacarandas round pink water under a pink sky.
    - Gloomweald: dark oaks and giant red mushrooms under a dim violet sky.
    - Mycelial Jungle: giant red and brown mushrooms by the water under a spore-green sky.
    - Highsun Meadow: golden-green grass dotted with flowers beside a river.
    - Wild Greens: tall grass, purple wildflowers and coarse dirt on terraced slopes.
    - Frostlight Garden: a frost iris in the snow under firs. Snowpetal Grove: snowy ground under birches.
    - Close-ups only: the Gilded Shrubland (the camera stood in golden oak scrub), the Hallowed Bog's and Starlit Wood's grass, and the Shrine Springs' slope (the camera looked into a neighbouring sparse jungle).

## World and event applicability
- Biome fit: each biome takes the climate and land shape of the vanilla biome it replaces.
- Seasons: see above.
