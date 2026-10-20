# Tree generation batch 6: evergreen scrub

Status: implemented and owner-approved, 16 October 2026. Public branch: `codex/tree-generation-batch-6` in `speedygroyper/jugcraft`, [PR #399](https://github.com/jimbozoomer-byte/jugcraft/pull/399). Actual tool/model: OpenAI Codex (GPT-6). Based on main after integration PR #398 merged the earlier approved tree batches, including the hawthorn harvest and recipe-view corrections. Batches 6b and 7 are excluded.

## Scope and acquisition

Add juniper trees (crooked, column and bush forms), flowering gorse and low sagebrush to the existing dry/open biomes. Juniper receives one nine-block wood set, evergreen needles, a renewable sapling and right-clickable berries. Gorse shares oak timber; sagebrush shares deadwood. All three have renewable foliage and saplings. Ordinary discovery and planting remain sufficient; no progression gate or new dependency is introduced.

Use the owner's approved muted red-violet juniper timber and grey-blue needles, muted yellow gorse over dark green, and silvery sagebrush. Extend the existing deterministic texture tools and runtime native log references. Preserve every prior species' textures and the owner library. No matching juniper, gorse or sagebrush files were found in the library inventory.

## Accepted placement plan

Percentages are absolute shares of placement attempts. Preserve existing attempt counts, except Steppe receives one attempt in 90% of chunks and two in 10%. Failed soil/space checks reduce actual tree counts.

| Biome | Batch 6 change |
| --- | --- |
| Canyon | 35% juniper, 25% juniper bush, 40% existing pine |
| Dryland | 30% juniper, 25% juniper bush, 45% existing pine |
| Xeric Shrubland | 25% juniper, 75% desert acacia |
| Dead Forest | Replace the approximately 10% oak share with juniper |
| Scrubland | 15% juniper, 85% oak bushes |
| Cold Desert | One lone-juniper attempt per six chunks, coarse dirt only |
| Field | Replace the 30% spruce share with column juniper |
| Prairie | 15% column juniper from oak bushes |
| Heathland | 60% gorse, 15% juniper bush, 25% existing pine |
| Shrubland | Fill the 30% reserved share with 20% gorse and 10% juniper bush |
| Steppe | 80% sagebrush, 20% juniper bush; exclude independent wild apple/peach patches |
| Tundra | 10% juniper bush from maple bushes |

Juniper sapling planting and generated-tree survival must agree on the supported dry soils. Steppe stays at most two blocks tall. Alpine Spawn's juniper share remains reserved for batch 8.

## Shapes and measured bounds

All five forms use vanilla trunk/foliage placers and real sapling survival checks. The crooked juniper uses a forking trunk with connected small crown lobes, replacing the proposed random-spread foliage so it cannot leave disconnected islands. The column uses a trunk of four plus zero to two logs; its tip makes the complete tree five to seven blocks tall.

| Form | Height | Width | Logs | Leaves |
| --- | --- | --- | --- | --- |
| Crooked juniper | 4–6 | 3–6 | 3–7 | 9–20 |
| Column juniper | 5–7 | 3–5 | 4–6 | 13–57 |
| Juniper bush | 2 | 5 | 1 | 27–30 |
| Gorse | 2 | 5 | 1 | 27–30 |
| Sagebrush | 1–2 | 3 | 1 | 4–8 |

These are observed ranges over twelve fixed seeds per form, not worldwide population statistics. Every tested leaf connects by faces to its own tree's logs; all five forms vary by seed. A juniper sapling selects crooked/column/bush with weights 2:1:1. Juniper accepts normal planting soils plus the native dry-vegetation soils; gorse and sagebrush keep normal sapling soils. Invalid ground and solid obstacles reject growth.

## Harvest and existing systems

Juniper berries use real empty/unripe/ripe states, side exposure for low bushes, one or two berries per harvest and natural regrowth, including seasons-off. Player-placed leaves do not regrow fruit; shears and Silk Touch do not duplicate it. Berries feed foxes and composters and substitute for sweet berries in an additional Mulling Spices recipe. Gorse flowers support bees. Sagebrush deliberately makes its single deadwood stem renewable.

Include wood crafting, stripping, tags, fuel, sawmill and appropriate tree-farm routes, with generated recipe-viewer entries. New chunks receive the biome changes; existing chunks and stable IDs remain intact, and disabling generation preserves registrations.

## Verification and delivery

Completed locally on 16 October 2026:
- Repository structure and documentation links pass.
- All 25 tree Python regression tests pass, including exact selector shares and retained attempt counts.
- Material/data/recipe/art audit passes: 2,749 material IDs, 8,385 block/item models, 3,707 quad parts and 171 combined models.
- 814 generated tree/biome, loot/model and recipe-view JSON files match their generators, including the normal model-finishing pass; all fourteen new textures match their deterministic generator. All previously committed textures remain byte-identical.
- All six focused server game tests pass: sixty seeded shapes, renewable saplings on supported soils, all juniper forms, obstruction protection, connected foliage, real right-click harvest/regrowth in every calendar mode, placement/decay rules, renewable loot, wood recipes/stripping, tree farms, native bee attraction and the original and alternative spice recipes.

The complete local `build` passed with all 1,684 server game tests. A subsequent one-line correction makes the juniper grower report its bush as the shortest form for bone-meal height validation; the final focused run passed all six tests again and rebuilt the matching JAR.

The final rendered `TreeBatchSixClientGameTests` passed with 26 screenshots and fifteen garden examples (three seeds per form). All twelve natural biome samples contained their intended batch-6 foliage. The normal-world test verifies that Steppe retains wild corn and shrubs while excluding apple and peach patch features. Its photographed sample contained 102 sagebrush leaf blocks and 77 juniper leaf blocks, with no apple or peach foliage. These counts establish presence, not global proportions; neighboring biomes can be visible in the photographs.

Every garden tree kept its shape across save/reopen. All recorded berry states, foliage types, persistence and waterlogging survived exactly; naturally grown leaves remained supported. Native cached leaf distances are allowed to settle as chunks load (observed 2 to 1); their changing cache must not be mistaken for lost fruit. The initial gallery run exposed a crimson-reference naming error, corrected to native stems/hyphae; the final run passed with those references and corrected camera framing.

[Juniper](../images/trees-batch-6/juniper_close.png) · [column form](../images/trees-batch-6/juniper_column_close.png) · [bush form](../images/trees-batch-6/juniper_bush_close.png) · [gorse](../images/trees-batch-6/gorse_close.png) · [sagebrush](../images/trees-batch-6/sagebrush_close.png) · [wood](../images/trees-batch-6/juniper_wood_close.png) · [solid foliage](../images/trees-batch-6/foliage_solid.png) · [natural Heathland](../images/trees-batch-6/biome_heathland.png) · [natural Steppe](../images/trees-batch-6/biome_steppe.png).

## Inspection delivery

The separate delivery is `tree-batch-6-preview-2026-10-16` beside this checkout. It includes **Evergreen Scrub Inspection**, the matching mod/pack, tour, screenshots and checksums. The raised garden is authored; all twelve biome stops are untouched natural generation. Garden junipers start with pickable ripe berries. Random ticks, time and weather are frozen for inspection; `/gamerule minecraft:random_tick_speed 3` restores ripening, sapling growth and leaf decay.

The delivery copy was scanned across **18,368 saved region records** and all other saved files; no actual game-test content was present. All **126 source files** and the source export were preserved. Only unused development metadata in `level.dat`, `level.dat_old` and `data/fabric/registry_entries.dat` changed in the cleaned copy.

The cleaned copy opened in ordinary Minecraft at the garden (1120.5, 180, -987.5), confirmed by the client join log. Both the pack and world ZIP pass integrity checks; every world file, the matching JAR and all eighteen pinned dependency entries match their verified inputs. No external launcher import, two-client dedicated-server session, released-world upgrade or performance benchmark is claimed. Batches 6b and 7 remain unstarted.

The owner approved the inspection world on 16 October 2026 ("It looks good") and requested the main-branch conflict check before PR approval. After integration PR #398 was squash-merged, the three batch-6 commits were reapplied onto main at `2804ba015b76e99ff341687801cb01fb908a9670`. All three patches applied unchanged, confirmed by `git range-diff`; this PR now contains only batch 6. The 25 tree regression tests, repository checks, 814 generated JSON comparisons and fourteen deterministic texture comparisons pass on that base. Combined build and current CI results are recorded on PR #399. The previously delivered inspection world is preserved. No merge or server deployment was performed.
