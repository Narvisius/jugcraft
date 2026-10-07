# World Designer

Owner-requested implementation, 5 October 2026. Actual implementation tool/model: **OpenAI Codex (GPT-6)**, under the owner's direct instruction. Original MIT code; no copied third-party editor, asset pack, JavaScript library, or mod JAR. Uses the existing Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and approved dependency lock. No new runtime dependencies.

## Player result

An offline map editor draws terrain and biome plans, places spawn and one existing Jugcraft walled city, and pins installed native structure start chunks. `/jugcraft design export` supplies the UI and a catalog from the actual instance. Export produces a datapack with a new `jugcraft:designed` world preset. Read the [user and contributor guide](../WORLD_DESIGNER.md) for instructions and limitations.

This is creator infrastructure, not survival progression: no new recipes, conversion loop, currency, multiplayer permissions, or automatic downloader. The operator export command only writes a timestamped local directory. The browser only reads files chosen by the user and downloads files on explicit Save/Export clicks.

## Integration contract

Schema 1 stores name, bounded immutable height grid, biome cell IDs, spawn, optional town, and up to 32 structure pins. Grid spacing must be a power of two (4–128); width 5–129; height values finite 40–256; exactly width-squared samples. A palette contains at most 512 biomes. The editor rejects missing registered content, invalid catalog versions, submerged spawn/city centers, pins lacking permitted biome paint, duplicate/overlapping approximate footprints and markers too close to the feathered border.

`jugcraft:design_density` wraps the configured density and surface-height functions. It preserves density outside the region and below Y8, transitions to authored land by Y32, and feathers terrain over two grid cells at each edge. The normal noise generator still performs surface material selection, aquifers and biome decoration. `jugcraft:design` wraps the seeded Overworld biome source, including Jugcraft's existing region hooks, with authored surface biomes. Generator data is serialized in world settings; it is not a global mutable state or post-generation chunk replacement.

`jugcraft:design_pin` participates in native structure generation only at its chosen chunk. Biome/terrain rules still apply, random structures remain, and vanilla locate searches are not extended. `AlpineSpawn` delegates designed-world spawn placement before its normal search. `TownPlanner` delegates the new world's single city to the existing town placement/build/state systems. No-city designs suppress ordinary automatic town selection. Town-disabled configuration remains respected. Existing saves using standard generators take their previous paths.

Catalog block IDs reserve space for future building tools; they do not imply a working block painter. Roads, arbitrary city assembly, rotations, schematic import, cave sculpting, custom water levels, 3D previews, collision exclusion of random structures, and retrogen are outside this version.

## Validation evidence

Evidence was collected on Windows on 5 October 2026 with the approved full dependency collection. Tests use disposable local worlds; the owner's normal server was not used.

- `node --test tools/world-designer/model.test.cjs`: 11 model/compiler tests passed, covering brushes, negative coordinates, interpolation, draft reopening, malformed files, missing mods, biome restrictions, square-footprint corner collisions, JSON round trips and ZIP layout.
- `WorldDesignerGameTests`: the three new server tests passed. They exercise density/cave/border behavior, biome codec round trips and rejection, fixed placements, and a live catalog containing 168 biomes and 53 structures.
- `WorldDesignerClientGameTests`: a real client generated a new custom world (`jugcraft-designer` seed). Authored base surfaces measured 80 and 128; the desert and Jugcraft Alpine Spawn paints reached generated chunks; spawn was (-128,80,0), city corner (160,127,160). The generator, markers and terrain survived save/reopen. The in-game editor export command also ran.
- Repository structure/link checks and the 1,437-material data/recipe audit passed.
- Browser inspection covered narrow and desktop layouts, loading the live catalog, coordinate city placement, undo/redo, a canvas brush, biome filtering, preview modes, validation and the export action. The embedded browser did not return a downloadable-file path, so ZIP byte validation was performed through the same shared compiler headlessly.
- An actual compiler-exported ZIP was loaded by a separate, disposable dedicated server with `level-type=jugcraft:designed`. It started successfully, reported the planned spawn and city, used the painted Alpine Spawn biome, and placed the city center at (400,79,400). After force-loading the chosen native village start chunk (-32,0), shutting down and inspecting the saved region data, `jugcraft:village_alpine` had **148 child pieces** in that exact start chunk. The ZIP also passed Python's CRC/archive validation. The server was bound to localhost and stopped after the test.

- `gradlew.bat build runClientGameTest --continue --offline --no-daemon -PclientTestShard=80 -PclientTestShards=999`: full build passed, **all 824 required server tests passed**, and the selected client world/persistence test passed. The first full server run had one intermittent failure in the existing `pixel_hollows_game_tests_every_village_has_one_shop` test (one desert village lacked a shop). A fresh-world rerun passed all 824 without changes to that village system.
- Packaged a development `.mrpack` containing the matching Jugcraft JAR and the same 18 pinned upstream library downloads. Inspected the JAR for the four editor resources and six generation classes. Launcher import of this preview pack was not performed.
- The strengthened client check also passed with optional integrations present and absent: generated ground reached the client near the flying test camera, and save/reopen still preserved the plan. **Visual Minecraft validation remains outstanding:** the automated captures showed only sky with both dependency configurations, including after a second capture. This has not been isolated to the capture API versus actual rendering; do not cite those captures as visual proof or treat this preview as release-ready. A normal-client visual playtest is required before release.

No two-player server playtest, arbitrary third-party worldgen compatibility, maximum-grid benchmark, or exhaustive seed coverage is claimed. The client test is deliberately one relevant class, not the entire existing client suite. The browser UI was exercised through localhost; direct `file:` navigation is blocked by the automation browser's URL policy, so that specific opening path was not automation-tested.

## Reproduction and rollback

Run `./gradlew build --no-daemon`, then `./gradlew runClientGameTest --no-daemon -PclientTestShard=80 -PclientTestShards=999` to select the current editor test class alone. If the test entrypoint order changes, update the shard index; normal CI still includes every class in its three shards. Run the Node tests and `python scripts/check_repository.py` / `python tools/check_mod_data.py` too.

To review the actual downloaded format, export a fresh catalog and a design ZIP, start a new world with that pack and preset, generate its pinned chunks, then save and reopen. Validate structure starts, not just map markers or `/locate` output. Keep original worlds untouched. Roll back a designed world by restoring its world backup **with the matching Jugcraft build and datapack**, not by removing its required codecs.
