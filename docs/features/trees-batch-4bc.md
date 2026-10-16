# Tree generation batches 4b and 4c: bald cypress and magnolia

Status: implemented and visually approved by the owner on 16 October 2026, including the connected bare-cypress revision. PR #395 is ready for review on `codex/tree-generation-batch-4bc`. Built with OpenAI Codex (GPT-6), on the visually approved batch-4 branch (PR #389, including #388 and #386). No later tree batch is included.

## Player outcome and scope

Bald cypress brings seasonal needles, renewable saplings and its own ashen timber to Bayou, Lush Swamp, Swamp Woods and Webwood. Its tall branching trunk has compact flared roots and an airy crown with occasional shroud moss. Magnolia brings an evergreen, cream-flowered broad crown and its own olive-brown timber to Flower Isle.

Both are accessible by gathering natural wood and saplings, planting and bone meal, with no progression gate. Their timber uses existing building recipes, fuel, crafting and sawmill routes; the existing tree farm provides automation. Feature switches preserve saved registrations when acquisition is disabled. World generation only affects newly generated chunks.

## Approved placement targets

These are shares of placement attempts, not guaranteed standing-tree proportions. Existing attempt counts, climate and ground decoration remain unchanged.

| Biome | Tree mix |
| --- | --- |
| Bayou | bald cypress 60%, willow 25%, live oak 15% |
| Lush Swamp | bald cypress 25%, vine oak 35%, willow 30%, maple 10% |
| Swamp Woods | bald cypress 25%, willow 45%, vine oak 20%, azalea 10% |
| Webwood | bald cypress 15%, willow 57%, dead tree 20%, vine oak 8% |
| Flower Isle | magnolia 55%, oak bush 30%, apple 15% |

## Shapes and integration

Bald cypress uses native upward branching: trunk 9 + random 0–3 + random 0–2, branch probability 0.15, 1–2 branch steps and 0–1 extra length. Its airy random-spread crown has radius 3, height 2 and 50 attempts, with shroud moss at 0.12. Native mangrove roots use cypress logs, a one-block trunk offset, width 2 and length 3. Unlike vanilla mangrove, they do not grow through or replace mud: the short foot stops at the soil surface. The prototype with vanilla's mud-through tag failed all twelve shallow-water samples, which motivated this bounded correction. Webwood retains dry-ground placement.

Magnolia uses a straight 5 + random 0–2 trunk with blob foliage radius 3, height 4. It remains evergreen and has three sparse cream flower clusters per leaf sprite. Neither tree adds fruit.

Each adds nine timber blocks (log, wood, stripped log/wood, planks, stairs, slab, fence and fence gate), leaves/needles and a sapling. Existing registration, drops, stripping, fuel, diagonal fences, recipes and feature switches handle both. Crafting yields four planks, sawing six; a tree farm takes 400 ticks and returns six logs plus the sapling. Both follow `biomes`; disabling acquisition does not remove their saved registry entries.

Cypress uses the existing seasonal-leaf system with schedule `[98, 286, 330]`, including its normal seven-day positional variation. The roadmap's approximate autumn date 292 was moved six days earlier so forced autumn (day 293) is entirely russet even at the earliest shifted positions. Summer and seasons-off stay green; winter is bare. Green and russet sprites have identical needle placement and transparency.

Bayou retains 5/6 attempts per chunk and water depth 3, Lush Swamp 3/4 and depth 2, Swamp Woods 7/9 and depth 2, Webwood 6/7 on dry ground, Flower Isle 1/2 on dry ground. Each pair uses the existing 90%/10% usual/occasional count. No second canopy feature, climate change, regional placement change or retroactive chunk modification is introduced.

## Art and provenance

The owner library README and full catalog were inspected; no matching cypress or magnolia asset was found. Planks use the approved palette entries and the established `wood_style.py` painter: cypress `#9c9391` from painting 23 and magnolia `#807d55` from London plane 2:1. Leaves and saplings extend the existing deterministic generators. Runtime bark and cut ends follow the owner's newer native-Minecraft-texture direction in `vanilla_logs.py`: tinted spruce for cypress and tinted oak for magnolia, with the shared pale-oak cut/stripped references. No copied Minecraft texture files or new dependency are added.

## Verification and preview

Verified on 16 October 2026 with OpenAI Codex (GPT-6), Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21 and Java 25.0.4.1. The branch starts from approved batch-4 commit `d3f60a68c637a2ef472bd4b3963d0db83a81b695` and depends on PRs #389, #388 and #386. Draft [PR #395](https://github.com/jimbozoomer-byte/jugcraft/pull/395) makes the work public.

All 46 Python tests pass. The full data/recipe audit passed 2,697 material IDs, 8,378 block/item models, 3,707 quad parts and 171 combined models. Repository structure/documentation links and all 18 dependency pins passed. All 1,662 server game tests passed, including both tree batches, followed by the rendered client test. The final client run passed again after improving only the inspection displays. No production code changed after the full server run.

Tests cover deterministic shape variation, connected timber, compact roots, water depths 0–3, soil and obstruction protection, sapling growth, stripping and recipes, natural sapling drops, shears/Silk Touch, seasons and leaf decay. Cypress placed 12/12 samples at each depth, including dry ground, preserving the entire soil bed. Across twelve seeds, cypresses were 12–17 blocks tall and 5–6 wide with 27–156 leaf blocks; magnolias were 6–8 tall and 9 wide with 212–217 leaves. Every old tree feature and texture retained its previous data/pixels. Biome counts and climate are unchanged.

The first test selection matched no tests; the corrected filter selects both batch-4 classes. The first prototype exposed the mud-root failure above and an overly strict leaf-count assumption: an unbranched airy crown can have 27 unique leaves after 50 placement attempts, so its minimum is 20 rather than the broad magnolia's 30. These failures were corrected before the successful full run.

The ordinary inspection world uses seed `jugcraft`. It includes three seeded examples of each tree, wood and leaf comparisons, four water-depth plots, three seasonal cypresses and natural stops in all five affected biomes. The natural survey counts both dry and submerged columns, requiring cypress timber over wet Bayou columns; these counts may include overhanging limbs, so root placement itself is established by the controlled depth tests. The client test saves and reopens the six main comparison trees. The root display uses seed 3 so low branches do not obscure the foot, while the main garden retains all three first seeds. Delivery copies remove only development-test metadata after scanning saved regions; the original export remains untouched.

The final world passed save/reopen and produced 18 screenshots. Its delivery copy preserved 98 source files, scanning 10,045 region records for actual test content before removing two development datapack selections and exactly five unused registry names. Only `level.dat`, `level.dat_old` and `data/fabric/registry_entries.dat` changed. The matching JAR, world ZIP, Modrinth inspection pack, checksums, tour and screenshots are delivered together; the package's README records the final public source commit.

An ordinary `runClient` opened a separate cleaned copy on 16 October, and the local player joined `Cypress and Magnolia Inspection` at `1040.5, 180, -987.5`. This is ordinary-client load evidence, separate from the automated save/reopen check. The runtime copy can be explored without changing the packaged save.

Natural stops (teleport locations): Bayou `1872 81 -6080`; Lush Swamp `-4744 82 3840`; Swamp Woods `-4808 83 3688`; Webwood `12800 83 -10624`; Flower Isle `4288 88 -6184`. Garden: `1040 180 -988`. These are seed-specific samples, not measured global biome shares.

Screenshots: [cypress](../images/trees-batch-4bc-cypress.png), [magnolia](../images/trees-batch-4bc-magnolia.png), [Bayou](../images/trees-batch-4bc-bayou.png), [Flower Isle](../images/trees-batch-4bc-flower-isle.png), [wood sets](../images/trees-batch-4bc-woods.png), [leaves and saplings](../images/trees-batch-4bc-leaves.png), [dry roots](../images/trees-batch-4bc-roots.png), [underwater roots](../images/trees-batch-4bc-underwater.png), [russet needles](../images/trees-batch-4bc-autumn.png).

### Bare cypress revision, 16 October

The owner found the bare needles disconnected and floating. The revised sprite uses a thicker, continuous branching pattern with matching connections on all four tile edges. All 124 opaque pixels form one connected group; coverage rises from 59/256 (23%) to 124/256 (48%). Its bark palette, transparent background, leaf model, seasons and tree shape remain unchanged. A pixel comparison of all 135 generated wood/foliage textures against the previous commit found only `bald_cypress_needles_bare` changed. The saved PNG matches its generator exactly, and opposite tile edges match.

The three batch-4bc Python tests passed again, followed by `./build-local.ps1 -Tasks @('jar','runClientGameTest','-PclientTests=TreeBatchFourBCClientGameTests')`. The rendered client passed save/reopen of six trees and exported 19 screenshots, including a new single-block and tiled-wall display at `/tp @s 1040 181 -882 180 0`. The bare tree is at `/tp @s 1080 180 -1208 180 0`. The new [winter tree](../images/trees-batch-4bc-bare.png) and [bare block comparison](../images/trees-batch-4bc-bare-blocks.png) were visually inspected. The full server and data-audit results above are the earlier implementation baseline; they were not rerun for this texture-only revision.

The revised delivery uses a separate folder and preserves the original preview. Its cleaned copy preserves 97 source files, with 10,098 saved region records scanned; again only `level.dat`, `level.dat_old` and `data/fabric/registry_entries.dat` changed during development-metadata cleanup. A separate ordinary client loaded the revised copy successfully, and the local player joined at `1040.5, 180, -987.5`. The owner approved this revision on 16 October 2026 and requested ready-for-review status.

Commands: `./build-local.ps1 -Tasks @('build','runClientGameTest','-PclientTests=TreeBatchFourBCClientGameTests')`; final display-only rerun: `./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=TreeBatchFourBCClientGameTests')`. Python: `python -m unittest discover -s scripts/tests -v`, `python tools/check_mod_data.py`, `python scripts/check_repository.py`, `python scripts/package_modrinth.py check`. Local Python runs use the existing DejaVu font directory through `JUGCRAFT_FONT_DIR`.

No external launcher import, two-client multiplayer, real-calendar season transition, released-world upgrade or performance capacity claim is made. No merge, release, server deployment or later-batch work is included.
