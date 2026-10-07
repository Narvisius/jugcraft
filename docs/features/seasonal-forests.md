# Seasonal forests (biomes batch 1)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and chose the seasonal forests as the first batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation and building (Discovery).
Primary specialty and supported player role: exploration and building; every player meets them.

## Player experience
Nine biomes grow in Jugcraft regions, each in place of the vanilla biome with the same climate. They grow in every layout of Jugcraft regions except where a later batch takes their climate (the meadows take the cool forests and plains, the wetlands the temperate forests and cool plains; [biome-regions.md](biome-regions.md)):

| Biome | Replaces (in Jugcraft regions) | What grows |
| --- | --- | --- |
| **Coniferous Forest** | cool taiga | dense firs, with tall firs towering over them, larches and stunted firs, a few grey snags, fallen fir logs |
| **Snowy Coniferous Forest** | snowy taiga | firs, narrow subalpine fir spires and stunted firs under permanent snow |
| **Maple Woods** | cool forest | maples (and big maples), some firs |
| **Seasonal Forest** | temperate forest | oaks, maples and aspens mixed, a few firs, with leaf litter and dense pumpkin patches |
| **Aspen Glade** | birch forests | tall, slender aspens and young aspens, a few maples and dark subalpine firs |
| **Dead Forest** | dry cool plains | grey dead trees and broken snags, a few spruces and oaks, fallen logs, dead bushes, sparse brown grass; rabbits and wolves only |
| **Tundra** | cool plains | no trees: maple, willow and fir scrub, mossy boulders, dead bushes, ferns; rabbits, foxes and wolves |
| **Snowy Forest** | snowy plains (moister) | snow-covered oaks, firs, aspens and maples |
| **Muskeg** | snowy plains | tamaracks, thin bog firs and grey snags, a few dead trees and fallen logs in wet, snowy flats; rabbits only |

**The trees follow the seasons** (the server's season clock, [seasons.md](seasons.md)):
- **Maples** are green in spring and summer, turn red, orange and gold in autumn (each leaf block its own colour, so crowns are fiery mixes), and stand bare in winter.
- **Aspens** turn gold and then drop their leaves.
- **Firs** stay green all year.
- Each block turns within a week either side of its date, so a forest changes gradually; with seasons off, every tree stays green.
- The six four-season biomes (all but the snowy three) also change grass and leaf colours with the seasons, and take winter snow when `seasons.snow` is on.

**New wood:** maple (grey bark, rosy planks), aspen (white bark with black eyes, pale planks), fir (dark bark, golden planks) and dead wood (weathered grey). Each has a full wood set: log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate. Maple, aspen and fir drop saplings from their leaves.

## Connections
- Input producer: world generation (Jugcraft regions).
- Output consumer: building (four new wood sets); the sawmill turns any of their logs into 6 planks with sawdust; the tree farm grows maple, aspen and fir saplings into 6 logs; every vanilla wood recipe and fuel use takes the new wood through vanilla's tags.
- Agriculture: the Seasonal Forest's pumpkin patches; the forests join `#minecraft:is_forest` or `#minecraft:is_taiga`, so the agriculture branch's wild crops and chestnut trees that grow in forests grow here too.
- Technology connection: the sawmill and tree farm (above).
- Magic connection: none yet.
- Reachable entry path: Jugcraft regions are common; each biome replaces a common vanilla one.
- Required vs optional: nothing here is required; it is wood, scenery and food.

## Balance and automation
- The new woods are ordinary wood: 4 planks per log by hand, 6 in the sawmill, a sapling in gives 6 logs and the sapling back in the tree farm, as for vanilla trees. No new conversions, no loops.
- Dead wood has no sapling: it comes only from generated dead trees, dead snags and fallen dead logs, in the Dead Forest, the Muskeg and the other dead, burnt and wet biomes that grow them ([trees-batch-1.md](trees-batch-1.md)).
- The leaves' look is cosmetic; drops, decay and fire are vanilla's in every season.

## Multiplayer and persistence
- Server authority: world generation and the season clock are the server's; clients only see block states.
- Seasonal leaves start in today's look. Generated trees get it from the `jugcraft:seasonal_leaves` tree decorator, listed in every tree feature with seasonal leaves (world generation does not run the block's own placement hook); leaves placed by a player or grown from a sapling get it as they are placed. After that they follow the season on random ticks, a changed leaf bringing up to 128 touching leaves in loaded chunks with it (as for the larch, [alpine-spawn.md](alpine-spawn.md)). The decorator visits each generated tree's leaves once.
- `biomes.enabled=false` stops the biomes in new chunks (no Jugcraft regions) and the new woods' hand recipes. Every block, item and biome stays registered, so old chunks and inventories keep them.
- Existing worlds: new chunks only; see [biome-regions.md](biome-regions.md).

## Dependencies and assets
- Textures drawn by code in `tools/forest_textures.py` (saplings); nothing read, traced or recoloured. Since the [wood repaint](wood-repaint.md), their woods and leaves are drawn by `tools/wood_style.py`.
- Biomes start from the vanilla biome they replace, by reference (`tools/biome_bases.py`: its ore, cave, lake and spring features, mobs, music and sky), with their own trees and plants (`tools/biomes.py`, `tools/trees.py`). No Mojang file is copied.
- The larch's one-off seasonal leaves became a general `SeasonalLeavesBlock`, shared by larch, maple and aspen; the larch's blocks and states are unchanged, and its tree feature now lists the `jugcraft:seasonal_leaves` decorator.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: every tree, wood set and leaf schedule is registered as the data says; every fixed season mode shows only its own look for every tree, whatever the jitter; every biome's features, tree picks, name, tags and seasons exist; the region rules match; exactly the trees with seasonal leaves list the `jugcraft:seasonal_leaves` decorator, and Java registers it.
- Server game tests (`BiomeGameTests`): maple, aspen and fir saplings grow (sizes logged), grown in autumn all their leaves in autumn colours (the fir's evergreen); the dead tree grows with no leaves; every seasonal tree's leaves follow every season mode; the new logs strip with an axe and the woods are in vanilla's tags. The larch's tests (`AlpineGameTests`) now run on the shared leaves.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`) to each biome (logged); the seasonal leaves within 24 blocks of each biome found, in chunks generated during the test far from the player (so no random tick has touched them), are all in today's look (counts logged); screenshots, from the nearest column whose surface is the biome, of Maple Woods, Seasonal Forest, Aspen Glade, Coniferous Forest, Dead Forest and Tundra from above.
- Not run: play, a dedicated server, two clients, the look of each season in play (only fixed modes are tested).

### Results
- **Run [36966158381](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36966158381) (commit 88f47fcc): green, but the screenshots showed two problems.**
  - Server game tests: all 307 required tests passed. Saplings grown in autumn: a maple with 6 logs and 131 leaves, all in autumn colours; an aspen with 8 logs and 148 leaves, all in autumn colours; a fir with 9 logs and 101 needles, none changed (evergreen). A dead tree grew 9 dead logs and no leaves. The season-mode and wood tests passed. The larch tests on the shared leaves passed: a larch grown in winter had a 9-block trunk and 34 needles, all bare.
  - Client game tests: larches grown in spring, autumn and winter had green/gold/bare needles 53/0/0, 0/82/0 and 0/0/33.
  - Screenshots, 2 October (season day 275, autumn): Maple Woods is red, orange and gold maples; Seasonal Forest is maples among autumn-tinted oaks; Coniferous Forest is dark firs along a river, with maple woods beyond; Dead Forest is grey dead trunks in dry grass.
  - **Problem 1: Aspen Glade was mostly green, with one gold aspen.** World generation placed every seasonal leaf green, and leaves only turned on a random tick. Maple crowns touch, so one tick turned a whole stand; aspens stand apart, so each waited for its own. Fix: the `jugcraft:seasonal_leaves` tree decorator gives generated leaves today's look, and the client test now checks generated leaves.
  - **Problem 2: the Tundra screenshot was under snow in October.** I first put this down to the Tundra's temperature (0.2, which lets vanilla snow lie from about y 120) and raised it to 0.25, as warm as vanilla taiga. The fix run showed the same snowy view, so that was not the cause (see below).
- **Fix run [36967610611](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36967610611) (commit a64f1aea): green.**
  - Server game tests: all 307 required tests passed, the saplings with the decorator too (a maple with 136 leaves and an aspen with 147, all in autumn colours; a fir with 170 needles, none changed).
  - Generated leaves: 13,247 seasonal leaves within 24 blocks of the nine biomes, in chunks generated during the test, were all gold on season day 275, and none was out of date. Maple Woods had 4,620, Aspen Glade 4,165 and Seasonal Forest 2,969. The counts include neighbouring biomes' trees within 24 blocks (Coniferous Forest's 165 are maples next door).
  - Aspen Glade screenshot: the aspens in front are gold. Green trees with white trunks further off on the hillside were not counted (beyond 24 blocks). They face vanilla birch forest (found at 560 912) across the region border, so they are most likely vanilla birches; that is a reading of the picture, not a test.
  - Tundra screenshot: unchanged, snowy. The search finds the nearest Tundra at any height, so the surface above it can be another biome: most likely the neighbouring Muskeg (found 64 blocks away), which is frozen and has dead trees like those in the picture. The client test now photographs, and counts leaves around, the nearest column whose surface is the biome (preferring one with the biome 16 blocks to each side). The Tundra keeps 0.25, so vanilla snow stays off its hills below about y 160.
  - Larches grown in spring, autumn and winter: green/gold/bare 57/0/0, 0/54/0 and 0/0/38. The start is unchanged.
- **Surface-spot run [36968803297](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36968803297) (commit 45c157ee): client green, server red (an older test's flaw, fixed below).**
  - Client game tests: every biome's surface was found within 64 blocks of where the search found it; the Tundra's spot is 16 blocks east, at 0 66 976. The 14,050 seasonal leaves generated around those spots were all gold on day 275 (Aspen Glade 5,060, Maple Woods 4,747, Seasonal Forest 2,930), none out of date.
  - Screenshots: Aspen Glade is a sea of gold aspens round a red maple. The Tundra is brown-green ground with red and orange maple scrub and a mossy boulder in front, and the snowy Muskeg (spruces, dead trunks) behind it: the earlier snowy view was the Muskeg. Dead Forest, Maple Woods, Seasonal Forest and Coniferous Forest look as before.
  - Server game tests: 1 of 307 failed, `SeasonGameTests.winterSnowLiesAndMeltsInSpring` ("Winter rain does not fall as snow"), from the seasons work (#80), untouched here and green in the runs before. It filled only its own blocks with plains, but a biome lookup blends the 4×4×4 biome cells up to 5 blocks around, so near the edge it could read the natural biome wherever the test was placed (this run: x 14,773,808). Tests are placed differently each run, so it failed only sometimes. Fix: the test fills plains 5 blocks past every block it reads, and asserts that the fill took.
- **Run [36969999816](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36969999816) (commit bfd59885): green.** All 307 required server game tests passed, the widened winter snow test among them; the client game tests passed.

## World and event applicability
- Biome fit: each biome takes the climate of the vanilla biome it replaces, so it borders what that biome bordered.
- Seasons: Coniferous Forest, Maple Woods, Seasonal Forest, Aspen Glade, Dead Forest and Tundra are four-season biomes (`#jugcraft:has_seasons`, `#jugcraft:has_winter_snow`); the snowy three are always frozen.
- Biomes O' Plenty's snowy variants of temperate biomes come from Jugcraft's winter instead (see the roster).

## Rollout and open questions
- Planned for later in this batch or the next: muskeg ponds that freeze in winter; toadstools; violets.
- Open: should Dead Forests have no animals at all, as the catalog says? (They have rabbits and wolves here.)
