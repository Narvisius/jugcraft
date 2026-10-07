# End biomes (biomes batch 9)

Status: in progress on branch `claude/biomes`. Green in CI (server and client game tests), every biome at its share. **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only, and its invented names are replaced by our own.
Owner: @jimbozoomer-byte
Target milestone and tier: the outer End (after the dragon).
Primary specialty and supported player role: exploration; building (coral, sandstone, moss, pale oak).

## Player experience
Five new biomes of the outer End islands. Four share the highlands with vanilla's End Highlands: vanilla's keep half, and each new one takes an eighth. The fifth takes a third of the barrens beside vanilla's End Highlands. They keep vanilla's chorus and End cities where the End Highlands have them.

| Biome | What it is |
| --- | --- |
| **Chorus Reef** | a dry reef among the chorus: sand, sandstone and dead coral blocks over the end stone, dead corals and fans, still pools, sandstone pillars |
| **Ender Wilds** | moss over the end stone under violet jacarandas and azaleas, glowcaps and glimmerblooms, glinting motes |
| **Outer Flats** | low, wide barrens of end stone, sand and gravel at the islands' edges, with dead bushes |
| **Phantom Garden** | pale moss and its carpets, pale oaks and eyeblossoms in the End's dark |
| **Rotted Expanse** | coarse dirt and soul soil seeping through the end stone, dead trees, grey snags and fallen logs, obsidian pillars and murky pools; no endermen |

- Biomes O' Plenty's names become ours: the End Reef becomes the Chorus Reef, the End Wilds the Ender Wilds, the End Flats the Outer Flats, the Spectral Garden the Phantom Garden and the End Corruption the Rotted Expanse (rot, not a fake glitch).
- No new blocks: the batch uses vanilla's and the earlier batches' (jacarandas, glowcaps, glimmerblooms, dead trees).

## Connections
- Input producer: the outer End.
- Output consumer: building (sand, sandstone, dead coral, moss, pale oak, jacaranda wood), light (glowcaps), saplings for trees in the End.
- Technology connection: none new.
- Magic connection: none yet.
- Reachable entry path: after the dragon, through an End gateway; each is a share of the outer islands.
- Required vs optional: nothing here is required.

## Balance and automation
- Everything here is vanilla material the Overworld gives too; trees in the End make wood easier to come by after the dragon, which vanilla's chorus already does for food. Obsidian pillars are a few small columns. The Rotted Expanse spawns no endermen, so it is no farm. No new conversions, no loops.

## Multiplayer and persistence
- Server authority: world generation and spawning are the server's.
- `biomes.enabled=false` (read at startup) leaves the End vanilla in new chunks. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- Placement: Fabric's End biome API (`TheEndBiomes.addHighlandsBiome`, `addBarrensBiome`), from the generated `/jugcraft/dimension_biomes.json` (`biome/JugcraftDimensions`).
  - Fabric's weights are not shares. It picks with one noise value: |noise| times the total weight, vanilla's biome first. That |noise| is mostly small, so later entries are far rarer than their weight says. Barrens are picked with the same noise, keyed by the highlands biome picked there.
  - So `tools/biomes.py` gives each End biome its share, and the generator works out the Fabric weights from the noise's quantiles (`tools/end_noise.py`, which measures them by simulating the noise): weights 0.30, 0.35, 0.50 and 2.95 for the four highlands in turn, and 7.01 for the Outer Flats.
- Ground: patches of each biome's blocks over the end stone (vanilla's vegetation patch feature, replacing only `#jugcraft:end_ground_replaceable`, which holds end stone).
- Features reused by reference: vanilla's pale oaks and pale garden flowers, chorus and End cities from the base; lakes, columns and simple blocks for pools, pillars and corals.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: biomes and their features, the generated placements current, no End biome with region rules, an Overworld surface or seasons.
- Server game test (`BiomeGameTests.dimensionBiomesArePlaced`): the game test server's End has one fixed biome, so the test builds the End's biome source as a real world does; every Jugcraft End biome must be among its biomes, and their features must sort into one order (no feature order cycle).
- Client game test (`BiomeClientGameTests`): how often each End biome turns up every 64 blocks over 4,096 blocks round the origin (every Jugcraft one must), each one's nearest place to the origin, a standing spot on its ground, and a screenshot.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37048482576](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37048482576) (commit a4b96787): green.**
  - Server game tests: all 316 passed. `dimensionBiomesArePlaced`: the End's biome source, built as a real world's is (`TheEndBiomeSource.create`, which Fabric's biome API extends), lists 10 biomes, all five of Jugcraft's among them, and their features sort into one order.
  - Client game test, the real End of a world with seed `jugcraft`:
    - Chorus Reef: standing at 768 60 -736; the shot shows end stone patched with grey, chorus beside the camera.
    - Phantom Garden: standing at 2,752 62 2,016; pale moss over end stone in the dark.
    - Ender Wilds, Outer Flats and Rotted Expanse: no standing room within 32 blocks of each one's nearest place to the origin, which lies over the outer End's void. The client test now looks up to 384 blocks round that place (commit 079af8b8).
- **Run [37052172948](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37052172948) (commit 079af8b8): green, and it showed a placement bug.**
  - Chorus Reef and Phantom Garden were photographed again (the Phantom Garden now with a pale oak).
  - Ender Wilds: nearest the origin at 736 64 -736, but only 2 columns of it within 384 blocks.
  - Outer Flats and Rotted Expanse: **not within 6,400 blocks of the origin at all.**
  - Cause: Fabric's weights are not shares (see Dependencies). With weight 0.25 each, the four highlands got about 4.8%, 1.0%, 0.1% and 0.01% of the highlands in turn. The Outer Flats could never generate: barrens beside vanilla's highlands only occur where |noise| is below 0.5, and it needed 0.67 or more.
  - Fix: each End biome now gives its share, and the generator works out Fabric weights for it (commit after 079af8b8). The client test now also samples the real End and Nether every 64 blocks over 4,096 blocks round the origin, logs how often each biome turns up, and fails if any Jugcraft one is missing.
- **Run [37055687473](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37055687473) (commit 35a11921): green. Every End biome generates at its share.**
  - Sampled every 64 blocks over 4,096 blocks round the origin, the real End held:
    - Highlands (2,907 samples): vanilla's End Highlands 1,551 (53%), Ender Wilds 408 (14%), Phantom Garden 329 (11%), Chorus Reef 328 (11%), Rotted Expanse 291 (10%). The design is 50% and four eighths.
    - Barrens (1,845 samples): the Outer Flats 349 (19% of all barrens; the design is a sixth, a third of those beside vanilla's highlands), vanilla's End Barrens 1,496.
    - Also End Midlands 4,093, Small End Islands 6,992, the central End 804.
  - Standing spots: Chorus Reef -792 62 736, Ender Wilds -640 64 -832, Phantom Garden 768 61 -696, Rotted Expanse 744 57 -720, Outer Flats 616 55 864.
  - Screenshots:
    - Chorus Reef: dead coral blocks and fans on end stone, islands floating beyond.
    - Ender Wilds: moss and a jacaranda beside sandstone pillars and sand (a Chorus Reef border).
    - Outer Flats: end stone cliffs patched with gravel.
    - Phantom Garden and Rotted Expanse, which meet there: patches of coarse dirt, soul soil and pale moss over end stone.

## World and event applicability
- Biome fit: the outer End's highlands and barrens, beside vanilla's.
