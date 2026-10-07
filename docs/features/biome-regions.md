# Jugcraft regions

Status: in progress on branch `claude/biomes`, stacked on Alpine Spawn (`claude/alpine-spawn`). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. On 2 October 2026 the owner asked to remake the Biomes O' Plenty catalog's biomes in Jugcraft ([branches/BIOMES.md](../branches/BIOMES.md)), and chose this placement: a region layer, so that the new biomes do not shrink vanilla's away.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation (Discovery); every biomes-branch batch builds on it.
Primary specialty and supported player role: exploration.

## Player experience
- The Overworld is a patchwork of large regions, about 1 km across, with irregular borders. About half are **Jugcraft regions**, where the biomes branch's biomes grow in place of some vanilla ones. The other half are pure vanilla.
- Jugcraft regions come in four **layouts**, equally common, each with its own replacements, so one vanilla climate can hold different Jugcraft biomes in different regions:
  - **woodland** (0) and the others start from the seasonal forests (batch 1): taiga becomes Coniferous Forest, cool forests Maple Woods, and so on;
  - **meadow** (1) is open land (batch 2): cool forests become Fields, cool plains Steppe and Grassland, temperate plains Prairie and Shrubland, flower forests Flower Meadows;
  - **wetland** (2) is wet land (batch 3): swamps become Bogs, Dead Swamps, Lush Swamps and Swamp Woods, dark forests Ghost Forests and Sludge Mires, rivers Lush Rivers, and so on;
  - **wild** (3) is harsh or strange land: the deserts and scrub of batch 4 (Outback, Oasis, Wasteland, Scrubland, Lush Savanna, Burnt Forest), and later the mountains, coasts and wonders;
  - batch 4 also adds the dry grasslands to the meadow layout and two warm forests to the woodland layout;
  - a biome can grow in several layouts: the seasonal forests grow wherever no other batch takes their climate, and the wetlands' swamps and bayous in the woodland and wild layouts too.
- Every vanilla biome still exists at its full size in the vanilla regions, and each layout keeps every vanilla biome that none of its rules replaces.
- Terrain does not depend on biomes, so crossing a region's border changes the trees and grass, never the shape of the land.
- Alpine Spawn (the start) and the Pixel Hollows are in every region.

## Connections
- Input producer: world generation.
- Output consumer: every biomes-branch batch (their biomes, trees and plants), and through them settling and building.
- Technology and magic connections: none directly.
- Reachable entry path: regions are everywhere; with the default share, a Jugcraft region is usually within a kilometre or two.
- How this stays useful without other branches: it needs none.

## Balance and automation
Nothing to obtain from the engine itself. Its rules decide where each batch's biomes grow; each batch's feature document covers what they give.

## Multiplayer and persistence
- **Server authority.** Biomes are chosen by the server's world generation only.
- **Seeds.** Which cells are Jugcraft regions comes from the world seed, read as the Overworld loads (before the server places the start or generates any chunk): the same seed always makes the same world, and different seeds differ.
- **Settings** (config/jugcraft.properties):
  - `biomes.enabled=false` turns Jugcraft regions off for new chunks: the whole world is vanilla again there. The biomes, blocks and items stay registered, so old chunks and inventories keep them.
  - `biomes.region_size` (blocks across, 256 to 8192; default 1024) and `biomes.region_share` (0 to 1; default 0.5). Changing them on an existing world moves the borders for new chunks only, which can leave seams.
- **Existing worlds.** Old chunks are never rewritten. New chunks follow the regions, so a border can cut across the line between old and new chunks.
- **Cost.** A biome lookup does a few integer hashes to find its region, and in a Jugcraft region one lookup in its layout (an R-tree like vanilla's). The four layouts are built once per world.

## How it works
- `mixin/OverworldBiomeBuilderMixin` wraps vanilla's Overworld biome builder. Alpine Spawn's replacements apply first; then `JugcraftRegions.Recorder` passes every entry on to vanilla's table and records its version in each layout.
- A rule says: in these layouts, a vanilla biome, in vanilla's temperature and humidity bands (and, if given, one half of the weirdness range, which vanilla also uses to pick its own variants), becomes this Jugcraft biome. Vanilla's entries often span several bands (a swamp covers cool and temperate climates), so before the rules apply, the recorder cuts each entry at the band edges and at zero weirdness (`JugcraftRegions.split`); the pieces cover exactly what the entry did, and rules match exact climates. The first rule that matches an entry decides it, and `tools/check_mod_data.py` refuses rules that could match the same entry. The rules are data: `tools/biomes.py` writes `/jugcraft/region_rules.json`, which Java reads.
- At the builder's end it lists every Jugcraft biome in vanilla's table once, at a climate no place has (`UNREACHABLE`: every parameter at the edge, plus the largest offset). World generation then knows the biomes' features and structures, while vanilla's own lookups never pick them.
- `mixin/MultiNoiseBiomeSourceMixin` wraps the climate lookup inside the source's resolvers (26.3's `createResolver` and `createResolverForChunk`, where the place is known) and answers from the region's layout when the place is in a Jugcraft region and the source is an Overworld that lists the Jugcraft biomes; otherwise vanilla answers. Every lookup of world generation, structures, spawning and `/locate` goes through them.
- **Surfaces.** Vanilla's surface rules choose sand, terracotta and so on by biome ID, so a Jugcraft biome would otherwise get grass and dirt. A biome may set its own ground (`"surface"` in `tools/biomes.py`: floor, the layer under it, noise patches); `tools/biomes.py` writes these as the material rule `jugcraft:overworld/surface`, and a small override of vanilla's top-level `minecraft:overworld` rule (vanilla's named parts, by reference) runs it first. It acts only in Jugcraft biomes that set a surface. A mod or data pack that also replaces that top-level rule would conflict.
- Regions are a jittered Voronoi diagram of cells `region_size` across; a hash of the seed and the cell decides whether a cell is Jugcraft, and which layout it uses.
- Rules and settings are data in `tools/biomes.py`; `python3 tools/check_mod_data.py` checks that Java matches.

## Dependencies and assets
No new dependencies, assets or vanilla files. Two small mixins: one extends #53's Overworld biome builder hook, and one is the biome source lookup.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py` checks that Java's rules, bands, defaults and settings match `tools/biomes.py`, and that every rule's biome exists.
- Server game tests (`BiomeGameTests`):
  - the four layouts were recorded, every rule placed its biome in each of its layouts (counts logged), and vanilla's table lists every Jugcraft biome exactly once, at the unreachable climate; vanilla's table, cut the same way, is logged by biome, climate bands and weirdness half, the facts later batches' rules are planned from;
  - regions cover about the configured share of a 128 km square, every layout is common, they are the same on every lookup, and change with the seed (logged);
- `PixelHollowsGameTests.overworldFeatureOrderHasNoCycle` covers the new biomes' features too.
- Client game test (`BiomeClientGameTests`): a real world (seed `jugcraft`) finds at least two thirds of the Jugcraft biomes, and vanilla taiga, forest and birch forest, within 6,400 blocks of the start (distances logged), with screenshots. This is the test of the lookup mixin in a real world.
- Not run: a dedicated server, other seeds, the look of region borders in play, performance measurements.

### Results
- **Run [36966158381](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36966158381) (commit 88f47fcc): green.** (An earlier run failed to compile: 26.3's `WorldData` has no `worldGenOptions()`. The seed is now read as the Overworld loads, through Fabric's `ServerLevelEvents.LOAD`.)
  - Server game tests: all 307 required tests passed.
    - The Jugcraft layout has 7,595 entries. Every rule placed its biome: Aspen Glade 176 entries, Coniferous Forest 172, Dead Forest 80, Maple Woods 172, Muskeg 192, Seasonal Forest 176, Snowy Coniferous Forest 310, Snowy Forest 106, Tundra 172. Vanilla's table has 7,604: the same 7,595 plus the nine Jugcraft biomes at the unreachable climate.
    - Regions: 8,822 of 16,384 samples (54%) of a 128 km square are Jugcraft regions (setting: half). With the next seed, 8,193 samples (50%) change.
  - Client game test, a real world with seed `jugcraft`. The start is unchanged: 320 106 512, at an alpine village in Alpine Spawn. All nine biomes are within 6,400 blocks of it:

    | Biome | Distance from the start (blocks) |
    |---|---|
    | Muskeg | 543 |
    | Tundra | 550 |
    | Aspen Glade | 603 |
    | Maple Woods | 607 |
    | Snowy Forest | 668 |
    | Seasonal Forest | 689 |
    | Snowy Coniferous Forest | 715 |
    | Coniferous Forest | 853 |
    | Dead Forest | 2,489 |

    Vanilla taiga (at 368 528), forest (240 656) and birch forest (560 912) are still near the start. This is the first evidence that the lookup mixin applies in a real world: its resolver targets and their place arguments work, and `/locate`-style searches see the regions.
  - The screenshots and two problems they showed are in [seasonal-forests.md](seasonal-forests.md). Region borders were not looked at.

## World and event applicability
- Only the Overworld's multi-noise source (vanilla's Overworld preset, or another preset built by vanilla's Overworld builder). Superflat, single-biome and other mods' biome sources are untouched.
- Seasons apply per biome, as each batch's biomes declare.

## Rollout and open questions
- Batch 1 (seasonal forests) came with the engine; batch 2 added the layouts; batch 3 cut entries at the band edges. Later batches add rules to their layouts.
- Open questions: is 1 km the right region size, and half the right share? Should borders be smoothed (blended a few chunks wide) rather than sharp?
