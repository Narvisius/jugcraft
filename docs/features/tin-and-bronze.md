# Tin and Bronze

Status: implemented and compiles; **not yet played**. A client launch and a two-client dedicated-server test are still required.
Proposal issue: #2
Owner: @jimbozoomer-byte
Target milestone and tier: shared materials; Discovery stage
Primary specialty and supported player role: metallurgy (miners, engineers, workshop mages)

## Player experience
Mine tin ore (dark cassiterite) with a stone pickaxe and smelt it into tin. Combine 3 copper ingots and 1 tin ingot into 4 bronze blend, then smelt the blend into bronze, the workshop metal later features build with.

## Connections
- Existing input producer: Overworld ore generation plus vanilla copper.
- Existing output consumer: none yet. The processing machine and starter magical instrument proposals are the intended consumers (see #2).
- Technology connection: bronze bearings, gears and fittings (planned).
- Magic connection: bell-bronze instruments (planned).
- Reachable entry path (prove no circular unlock): stone pickaxe → tin ore → vanilla furnace → tin ingot; tin + vanilla copper → blend → furnace → bronze. Only vanilla tools and blocks.
- Which connections are required vs optional; trade and solo routes: nothing beyond vanilla is required. Everything is possible solo by hand; tin and bronze are natural trade goods.
- How this specialty stays useful without mastering every other branch: every later tier needs modest amounts of bronze.

## Balance and automation
| Recipe | Input → output | Metal units in → out (nugget = 1) |
| --- | --- | --- |
| Smelt/blast raw tin or tin ore | 1 → 1 tin ingot (0.7 XP; 200/100 ticks) | 9 → 9 |
| Nuggets, ingots, blocks, raw blocks | 9 ⇄ 1 | lossless |
| Bronze blend | 3 copper + 1 tin → 4 blend | 36 → 36 |
| Smelt/blast bronze blend | 1 → 1 bronze ingot (0.1 XP) | 9 → 9 |

No recipe turns bronze back into copper or tin. `tools/check_mod_data.py` audits every recipe for metal gain. Recipes consume `c:` convention tags, so tin from other installed mods also works. Worldgen: size-9 veins, 8 attempts per chunk, Y −32 to 96 (trapezoid), Overworld only; roughly half of vanilla copper. These are starting targets to tune by measurement.

## Multiplayer and persistence
No block entities, screens, packets or tick logic: everything is vanilla mining, crafting and smelting, which the server controls. `config/jugcraft.properties` has `tin.enabled`; `false` stops new tin ore generation and removes the tin/bronze recipes (via the `jugcraft:feature_enabled` resource condition). Blocks and items always stay registered, so saves keep them. No retrogeneration: chunks generated before tin is enabled never contain tin.

## Dependencies and assets
Fabric API only. All textures are original, drawn by `tools/generate_textures.py` from fixed seeds (MIT, Jugcraft contributors); no Mojang texture is read, traced or recolored. Tin ore, raw tin and its raw block, and the tin and bronze ingots, nuggets and blocks were since redrawn as material sets ([material-sets.md](material-sets.md)); the bronze blend keeps its seeded texture.

## Verification
Actually run (30 September 2026, in a sandbox without access to Minecraft or Fabric downloads):
- `python3 tools/check_mod_data.py`: PASS for 11 IDs. Negative test: a gain recipe, a bronze-to-tin recipe and a missing texture were each detected.
- `python3 scripts/check_repository.py`: PASS.
- `javac` over the sources without Minecraft on the classpath: only missing-library errors, no syntax errors.

- GitHub Build workflow (PR #4): the first run failed on two renamed Fabric API 26.3 names (creative tabs, resource-condition signature); after fixing them from the fabric-api 0.161.0+26.3 source, `./gradlew build` → BUILD SUCCESSFUL.

Not run yet: GameTests, client launch, dedicated server with two clients, restart/persistence, config toggle, worldgen sampling, performance. The planned scenarios are listed in #2.

## World and event applicability
Ore generation only in newly generated Overworld chunks. Not seasonal; no creatures, bosses, loot or dimensions.

## Rollout and open questions
Platform pins in `gradle.properties` came from web search and must be confirmed by the first build. Acquisition should stay disabled in releases until a consumer ships (#2). Recipe-book unlock advancements are not added yet.
