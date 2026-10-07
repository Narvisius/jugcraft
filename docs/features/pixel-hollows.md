# Pixel Hollows

Status: implemented in source; **not yet played**. Builds and passes its game tests in CI (see Verification); the client test screenshots its blocks and a cave lined by its worldgen feature.
Proposal issue: none. The owner supplied the proposal ("Pixel Hollows: a retro-electronics cave biome (first slice)") and asked for it to be implemented on 1 October 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery → Workshops. Iron tools and a light source are enough.
Primary specialty and supported player role: dangerous underground / exploration; supports cozy building.

## Player experience
Deep under dry land the player breaks into a cave that looks like the inside of an old games console.
- The walls are **circuitstone**: dark slate streaked with copper traces, pads and vias.
- **Pixel crystal clusters** grow in scattered patches on the walls, floor and ceiling: blocky, square-faceted crystals that glow faintly, like dead pixels on an old screen.
- The cave hums: an original, quiet chiptune drone, the odd bleep, and the usual cave mood sounds. The fog is a dim violet.
- It holds **more copper and redstone** than ordinary caves (and more tin while tin is enabled).
- Builders get a palette: **circuitstone**, **polished circuitstone** (a little chip on two traces), **circuitstone bricks** and the **pixel lamp**, a full-light block of coloured LED pixels.
- It is still a cave: crystal light is sparse, so the dark pockets between patches spawn the usual hostile mobs.

The theme is original: no real consoles, games, brands or characters.

## Connections
- Existing input producer: world generation. Nothing is consumed.
- Existing output consumer:
  - Factory engineering: the extra copper and redstone (and tin, for bronze) feed the early machines and circuits.
  - Cozy building: the circuitstone set and pixel lamps.
  - The [Retro Trader](retro-trader.md) buys shards back and sells maps to the cave.
- Technology connection: copper, redstone and tin for machines; circuitstone runs on the stonecutter like any stone.
- Magic connection: none yet. Pixel shards could later become an inscription or illusion reagent; that needs its own proposal.
- Reachable entry path (prove no circular unlock): vanilla mining finds the cave, and an iron (even wooden) pickaxe mines everything in it. No Jugcraft item is needed to enter, harvest or craft its blocks.
- Which connections are required vs optional; trade and solo routes:

| Connection | Status |
| --- | --- |
| Vanilla copper and redstone | Required, already exist |
| Tin (`tin` switch) | Optional: the tin bonus is added only while tin is enabled |
| The Retro Trader's map | Optional: the cave can be found by exploring |
| Trade | Optional: explorers can sell shards, ore and decor to builders and engineers |
| Solo route | Fully soloable; nothing here gates core progression |

- How this specialty stays useful without mastering every other branch: it is a mining destination and a decor source; nothing else requires it.

## Balance and automation
- **Where:** a cave biome in the Overworld's climate table (`PixelHollows.PARAMETERS`): the driest land (humidity −1.0 to −0.6), coast or inland short of the far inland (continentalness −0.11 to 0.8), at depth 0.3 to 0.9, which is about 40 to 115 blocks below the surface. On ordinary terrain that is mostly below Y 32 and in the deepslate band. Vanilla's dripstone caves take continentalness 0.8 to 1.0 over the same depths, so the Pixel Hollows stops at 0.8: where two cave biomes are equally close in climate, the winner would be left to the climate search's cache and could differ from one search to the next. Lush caves take humidity 0.7 to 1.0. The Pixel Hollows should be rarer than either; its real frequency has not been measured (see Verification).
- **Ore bonus:** the biome lists vanilla's copper and redstone features, plus extra attempts of the same vanilla features with half the vanilla count and the same height spread, filtered to the biome (`pixel_hollows_copper`: 8 × `ore_copper_small`, trapezoid −16..112; `pixel_hollows_redstone`: 2 × `ore_redstone`, uniform −64..15; `pixel_hollows_redstone_lower`: 4, trapezoid −96..−32). Inside the biome that is 1.5× the usual average. Tin likewise (`pixel_hollows_tin`: 4 × `jugcraft:ore_tin`, trapezoid −32..96) while `tin` is on. No new ore.
- **Other vanilla features kept:** lava lakes, amethyst geodes, monster rooms, the vanilla ores (coal, iron, gold, redstone, diamond, lapis, copper) and water and lava springs, in vanilla's order. Left out on purpose: stone-variety blobs (the walls are circuitstone), glow lichen (the only light is the crystals) and surface plants. The game test `overworldFeatureOrderHasNoCycle` proves the order is consistent with every other Overworld biome.
- **Lining:** `jugcraft:pixel_hollows_lining` is a vanilla ore feature of circuitstone (vein size 64) that replaces stone and deepslate, but not ores: 212 attempts per chunk between Y −56 and 192 (the biome reaches up to about Y 160 under mountains), kept only where they land in the biome, run after the ores so exposed ore still shows. Circuitstone is in `#minecraft:deepslate_ore_replaceables`, so lining that spills into a neighbouring chunk decorated later does not take that chunk's ores away (they become the deepslate kind). Walls are mostly circuitstone with streaks of the rock around it.
- **Crystals:** `pixel_crystals_floor` (31 attempts per chunk) and `pixel_crystals_ceiling` (18), over the lining's span, look up to 12 blocks down (or up) from a random point for a solid floor (ceiling) and put a cluster on it, inside the biome only. Many attempts find no cave and place nothing, so clusters stay sparse.
- **Pixel crystal clusters:** static: no growth, no random ticks, no block entity. Light 3. They drop 1–2 pixel shards (Fortune adds up to one per level); Silk Touch takes the cluster. Clusters are finite, so shards are finite.
- **Recipes (data-driven):**
  - Stonecutter: circuitstone → polished circuitstone or circuitstone bricks, and polished → bricks, 1:1.
  - Crafting: 4 circuitstone → 4 polished; 4 polished → 4 bricks (like deepslate).
  - 4 pixel shards + 1 glass → 1 pixel lamp.
  - Nothing makes pixel shards; the data checker fails any recipe that does.
- **Automation:** only what vanilla mining allows. There are no machines or conversions here, so no power or essence loops.

## Multiplayer and persistence
- Server-side only: world generation, drops and recipes. No custom packets; clients get models, textures and sounds.
- Ordinary blocks: no ownership or concurrent-use rules needed.
- **Stable IDs:** `jugcraft:pixel_hollows` (biome), `jugcraft:circuitstone`, `jugcraft:polished_circuitstone`, `jugcraft:circuitstone_bricks`, `jugcraft:pixel_crystal_cluster`, `jugcraft:pixel_shard`, `jugcraft:pixel_lamp`; worldgen `jugcraft:pixel_hollows_lining`, `jugcraft:pixel_crystals_floor`, `…_ceiling`, `jugcraft:pixel_hollows_copper`, `…_redstone`, `…_redstone_lower`, `…_tin`.
- **Existing worlds:** the biome appears only in newly generated chunks. Generated chunks are never rewritten, so where old and new chunks meet underground a cave can end abruptly at the old chunk's edge.
- **Disable switch:** `pixel_hollows.enabled=false` in `config/jugcraft.properties` stops new generation (the biome leaves the climate table and the tin bonus is not added) and its recipes. The blocks, items and the biome stay registered, so builds and generated caves survive. (The proposal kept the recipes on when disabled; Jugcraft's rule is that a switch stops acquisition, recipes included.)

## Dependencies and assets
- No new dependency. Fabric API places features but has no way to add an Overworld biome, so one small mixin, `mixin/OverworldBiomeBuilderMixin`, appends the biome's climate entry after vanilla's (`jugcraft.mixins.json`). No TerraBlender or Biolith.
- The biome, lining, crystals and bonus ores are all data (`data/jugcraft/worldgen/`), generated from `tools/pixel_hollows.py`.
- Original assets: textures drawn by `tools/pixel_hollows_textures.py` and models built in `tools/retro_models.py`; the ambient loop synthesised by `tools/pixel_hollows_sound.py` (written to an Ogg file with ffmpeg). The bleeps reuse the game's own note-block "bit" sound by name in `sounds.json`; no Mojang file is copied. MIT.

## Verification
Build workflow run 36815372169 on commit 568409c (1 October 2026, merged with `main` after chemistry batch 5): Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub-hosted Ubuntu; game tests on the headless test server, client tests with Mesa software rendering.

**Passed:**
- `python3 scripts/check_repository.py`, `python3 tools/check_mod_data.py` (IDs, assets, loot, tags, the biome's feature references, no recipe makes shards, no pre-26 loot keys) and the generated-JSON check.
- `./gradlew build`: all 103 game tests, including `PixelHollowsGameTests`:
  - `pixelCrystalClusterDrops`: 1–2 shards (both seen in 64 breaks), Silk Touch takes the cluster, Fortune III at most 5.
  - `pixelHollowsBlocksDropThemselves`: circuitstone needs a pickaxe and drops itself; lamp light 15; cluster light 3 and no random ticks; the cabinet drops once, from its lower half.
  - `pixelCrystalFeaturesFaceTheCave`: the floor and ceiling features place clusters facing up and down.
  - `pixelHollowsRecipes`: lamp (4 shards + glass → 1), cabinet, stonecutting.
  - `pixelHollowsJoinsTheOverworld`: the biome is registered and in the Overworld's climate table.
  - `overworldFeatureOrderHasNoCycle`: every Overworld biome's features sort into one order.
  - `oresNeedSilkTouchToDropThemselves` (with the shared loot fix): tin ore gives raw tin, 1–4 with Fortune III, and itself only with Silk Touch.
- Dedicated server (client job, `JugcraftServerClientGameTests`, one client): the game's own dedicated server started in-process on port 25565 and the client joined. The Retro Trader's novice offers reached the client after a real right-click (`[1 jugcraft:pixel_hollows_map, 8 jugcraft:circuitstone]`, screenshot `jugcraft_dedicated_server_trader`), a machine's screen showed the server's items, and after leaving and joining again the player still had the 7 pixel shards given before leaving and the world was unchanged. A singleplayer world holding a trader, a machine and an arcade cabinet was saved, closed and reopened with identical contents (the trader kept his offers).
- Client screenshots, looked at: `jugcraft_pixel_hollows_blocks` (the four blocks, a cluster and the cabinet) and `jugcraft_pixel_hollows_cave` (a carved cavity set to the biome, lined by the real `pixel_hollows_lining` feature, clusters on its floor and ceiling).

**Failed, then fixed on this branch:** 26.3 changed two data formats without load errors. Loot tables written the pre-26 way lost their conditions and functions (clusters and every Jugcraft ore dropped themselves to any tool; the loot validator logged "Unreachable entry"), so the generator now writes 26.3's form. The crystal features' first block-state and placement format did not load in 26.3 and were rewritten.

**Not run:** how often and where the biome appears on real seeds (26.3's climate sampler is not public API in the form the plan assumed, and the test worlds are superflat); a survival playthrough; finding the biome in naturally generated terrain in a client; the old-chunk upgrade test; hostile-spawn checks in dark pockets; two clients on one dedicated server (the checklist is in docs/TESTING.md); chunk-generation timing with and without the feature.

## World and event applicability
- Biome fit: a deep cave under dry land; the surface above is desert, savanna, badlands or dry plains, so the cave has no surface tell. It is not under oceans.
- Danger: hostile mobs spawn as in other caves (the biome's monster list is vanilla's cave list); the crystals light only small patches.
- Not seasonal; no pets, bosses or dimensions.

## Rollout and open questions
- The lining's cost (96 ore attempts per chunk inside the biome) has not been measured.
- The climate numbers are a first guess and have not been measured on real seeds (see Verification); they may need adjusting once someone has played it.
- A magic use for pixel shards, hostile cave creatures, an abandoned-arcade ruin and a boss are later proposals.
