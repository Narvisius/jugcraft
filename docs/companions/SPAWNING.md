# Natural spawning

Peepo: primary Swamp, Lush Swamp, Bayou; secondary Marsh, Wetland, Lush River.
Jughead: primary Beach, Dune Beach, Overgrown Beach; secondary Oasis, Hot Springs.

Both use the passive creature spawn cycle in the Overworld, groups of 1–4, primary weight 8 and secondary weight 4. Weights are relative selection weights, not percentages or guaranteed encounter times. Vanilla animal population, loaded terrain and available ground affect encounters. No weather, season, time or difficulty restriction is added.

Only dry surface positions on grass blocks, dirt-family blocks, sand, mud, moss or calcite qualify. Normal placement and collision checks also apply. Natural spawning checks at each group member stop further spawns once six naturally spawned, living companions of that type are loaded within a horizontal 128-block radius. Peepo and Jughead have separate counts. Summoned companions do not count; natural origin is saved. The cap prevents additional spawning, not movement or loading of existing companions into the area.

Spawns use the live natural spawning cycle, including existing chunks. Initial chunk-generation spawning is disabled because neighboring entities are not yet available to enforce the cap there. No chunks are force-loaded for counting. Normal mob spawning must be enabled. Companions remain persistent.

The four biome tags in `src/main/resources/data/peepo_companion/tags/worldgen/biome` can be overridden by data packs. Jugcraft entries are optional, so the mod also runs alone with Swamp and Beach habitats. Custom biome placement was checked against Jugcraft's main-branch `tools/biomes.py` and `src/main/resources/jugcraft/region_rules.json` on 2026-10-05. A biome present in both tags gets only the primary entry.

Validation: client integration checks cover vanilla habitat tags, natural spawn rejection underwater and beneath roofs, the sixth/seventh cap boundary, separate species counts, summoned exemptions and saved natural origin. Jugcraft custom-biome integration is data-driven and requires testing with the full pack; encounter frequency has not been measured across survival seeds.

