# Tree generation batch 5: rowan, elder and seasonal fruit

Status: implemented on `codex/tree-generation-batch-5` in [PR #396](https://github.com/jimbozoomer-byte/jugcraft/pull/396), owner visually approved on 16 October 2026 and ready for review. Authorized by the owner on 16 October 2026. OpenAI Codex (GPT-6). Builds on owner-approved PR #395 and its tree-batch prerequisites. The 41 open upstream PRs and public branches of upstream plus all four public forks were checked: no rowan, elder or seasonal-fruit implementation was identified. The existing tree-roster branch is the design source.

## Design published before implementation

Rowan brings the shared dusty-pink fruitwood set, seasonal leaves, renewable saplings and scarlet berries. Elder is a broad shrub with one hidden vanilla oak stem, its own seasonal leaves/sapling, cream flower clusters and dark berries. Batch 5b (hawthorn), 5c (persimmon) and later trees are excluded.

| Location | Placement target |
| --- | --- |
| Snowy Forest | Replace the remaining 36% vanilla-oak attempts with rowan; retain fir 30%, aspen 20%, maple 14% and 7/8 attempts |
| Lake District | Rowan 10%, oak 44%, fir 30%, larch 10%, fancy oak 6%; retain existing density |
| Highland | One rowan attempt per eight chunks on average, followed by its sapling survival check; no normal forest layer |
| Woodland | Elder 4% of existing attempts, drawn from the oak share; retain canopy density and other trees |
| Wetland | Elder 10% replacing the remaining spruce; retain swamp cedar 50%, willow 40% and density |

Rowan uses native forking trunks (4 + 0–2 + 0–1) and an airy random-spread crown (radius 2, height 3, 40 attempts). Elder uses a single hidden stem and native bush foliage (radius 3, offset 1, height 2). A bounded `fruiting_shrub` decorator removes four of that tree's own bottom-row leaf blocks, two blocks out from the stem, while preserving its soil and hidden stem. The initial unbroken bush had no exposed underside at all, so it could not ripen berries on flat ground; these four openings give middle-row leaves air below without weakening the shared fruit rules. It never removes foreign blocks or foliage outside the generated tree. Placement shares are attempts, not promised standing-tree proportions. Only new chunks change.

`SeasonalFruitLeavesBlock` composes the existing seasonal-leaf behavior with the established fruit property and harvest rules. Season changes preserve fruit stage, including ripe berries on bare winter twigs. Natural, supported leaves with air below ripen 0 → 1 → 2 on random ticks in every season, including seasons-off. Player-placed leaves never produce fruit; harvesting resets only fruit. Leaf decay, shears, Silk Touch, saplings and feature switches retain existing semantics. No per-tick world scans or new block entities.

Use one deterministic fruit placement across all three seasonal looks, with three fruit stages per look. Rowan follows days 98/255/305; elder 88/283/328. The roster's approximate rowan leaf-out day 100 moves two days earlier so the spring preset (day 105) is fully green even with the existing ±7-day position jitter. Rowan berries yield a small bitter snack, Mason Jar jelly, the existing berry-candy flavour and Warding candle scent. Elderberries provide a snack, purple dye, berry-candy flavour and an existing witch-cauldron floater. These use current recipes and tags without new progression gates.

Fruitwood uses the roster's approved `#ab7d76` palette and the current native bark/reference approach. New foliage uses the established generators and connected bare twigs. Preserve the owner-approved pine and every other existing texture. Inspect the shared owner library before adding art.

## Acquisition, uses and limits

Both trees are early-game, optional resources. Find them in new chunks, collect saplings from ordinary leaf drops and replant on normal sapling soil. Obstruction and unsuitable soil reject growth without destroying the sapling or obstacle. Rowan supplies fruitwood; elder keeps its one hidden vanilla oak stem. Existing chunks and IDs are preserved, and turning off `biomes.enabled` stops new Jugcraft biome generation without removing saved content. Fruitwood's crafting/sawing switch is `biomes`; the separate tree-farm routes retain the existing machines switch.

| Producer/input | Output/consumer |
| --- | --- |
| Rowan log/wood, including stripped forms | Four fruitwood planks by hand or six at the existing sawmill; the complete nine-block wood set uses existing recipes and tags |
| Rowan or elder sapling in the tree farm | Six logs of its existing timber and the same renewable sapling, through the shared 400-tick route |
| Supported natural leaf with air below | One fruit stage per ten random ticks on average; two stages to ripen, yielding one or two berries |
| Rowan berries | Small snack (1 hunger, 0.1 saturation modifier); berry candy; Warding candle scent |
| Six rowan berries, two sugar, one Mason Jar | Rowan jelly in the existing heated Cooking Pot, 300 ticks; existing serving and reusable-container behavior |
| Elderberries | Snack (2 hunger, 0.2 saturation modifier); one purple dye each; berry candy; existing witch-cauldron floater |

Fruit is never accelerated with bone meal; it renews with random ticks in every season. Existing medium compost values are reused for berries. Seasonal leaves continue random ticking while ripe or player-placed so they can change appearance; each tick has constant fruit work and a bounded catch-up of at most 128 connected leaves when the season changes. Harvest and growth remain server-authoritative. The shared fruit class also preserves the existing companion harvest integration through inheritance; no new companion behavior is added.

## Assets and shared systems

The owner library README and complete catalog were inspected for rowan, elder and fruitwood; no matching supplied asset was found. New 16×16 images come from the existing deterministic wood/foliage generators, the owner's approved fruitwood palette, and the roster's foliage ramps. Fruit clusters use separate species seeds and wrap at the texture edges, but keep their positions through the three seasonal looks. Bare foliage reuses the owner-approved connected twig generator with each tree's own palette. The native log reference treatment supplies fruitwood's bark and ends; no Mojang image is copied. All 135 pre-existing generated wood/foliage images compare pixel-for-pixel with announcement commit `558dd0136`.

No dependency or platform pins change: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and the existing 18-entry companion pack lock. This feature uses native tree placers, existing seasonal and fruit rules, Mason Jars, Cooking Pot, sawmill, tree farm, candy/scents/floater tags and the established inspection-world harness. It does not add a separate renderer, inventory, progression gate or block entity.

## Review images

[Timber comparison](../images/trees-batch-5/fruitwood.png) · [all season/fruit states in solid-leaf mode](../images/trees-batch-5/fruit_solid.png) · [bare rowan](../images/trees-batch-5/rowan_bare.png) · [bare elder](../images/trees-batch-5/elder_bare.png) · [natural Highland](../images/trees-batch-5/biome_highland.png) · [natural Wetland](../images/trees-batch-5/biome_wetland.png).

## Verification on 16 October 2026

CI follow-up: the owner reported a failed check on PR #396. The `mod` job stopped before compilation because the generated-resource comparison found three missing rows in `recipe_view.json`: fruitwood sawing and rowan/elder tree farming. The committed viewer data now matches the shared generator and the exact generated Git blob reported by CI (`e31c4936`), preserving every prior recipe row and other category. This data-only correction leaves gameplay recipes and trees unchanged; the focused generation check was repeated without rerunning the local gameplay suite. Actual tool/model: OpenAI Codex (GPT-6).

Production implementation: `1af1b026e2b3e71a5e7fd1df3470b3860af19db6`, based on public design commit `558dd01364ba76f9259cbff68b7ee1427533871b` and approved PR #395. Subsequent changes refine the inspection survey and normalize generated JSON key order; no tree, biome, berry or recipe behavior changed after the full server pass. Actual tool/model: OpenAI Codex (GPT-6).

- The complete `build` server stage passed **all 1,668 required tests**, including six new batch-5 tests. Coverage includes twelve seeds per sapling, real fruit-bearing crowns, soil/obstacle protection, stripping and wood recipes, every calendar mode, repeat harvest, fruit-preserving seasonal catch-up, leaf decay, persistent/buried leaves, shears/Silk Touch, renewable saplings, tree farms, berry consumers and Cooking Pot jelly.
- All **50 Python tests** passed. The four new checks cover biome shares/density, the bounded Highland extra, all 18 season/fruit models and stable fruit artwork, shared timber and tree-farm renewal, and generated seasonal decorators.
- The full data/recipe audit passed: **2,713 material IDs**, **8,379 block/item models**, 3,707 quad parts and 171 models drawn together. All **114 generated tree JSON files** also match the generator's exact normalized text; focused biome, calendar and wood-switch audits passed after the shrub correction.
- Repository/documentation and **18 locked dependency** checks passed. No platform or dependency versions changed.

| Native sapling trials (12 seeds each) | Height | Width | Logs | Leaves |
| --- | --- | --- | --- | --- |
| Rowan | 8–9 | 3–6 | 5–9 | 20–46 |
| Elder | 3 | 7 | 1 | 69–74 |

These are measured whole-tree bounds in Minecraft 26.3. Rowan retains the proposed native trunk/crown parameters; their actual 8–9-block result supersedes the roster's rough 6–8 estimate. Every elder trial has four natural leaves with air below, and the opening decorator preserves a foreign block placed in its path. Berry growth is separately exercised in every season, including seasons-off.

The first full server run had one existing crow crop-raiding failure; it did not recur in the complete passing run, and no crow code was changed. The audit caught rowan's spring/jitter mismatch, and a real-crown test caught the completely enclosed elder underside; both were corrected before the passing run. Initial focused selections and local font configuration were corrected before reporting results.

The first rendered-client run found no elder in the closest Wetland. A follow-up survey showed that fragment contained 912 wet columns and only one dry column. The inspection harness now checks a bounded set of nearby patches and, if needed, eight additional regional anchors. This selects a **natural content-presence stop**, not a statistically representative placement-share sample. It never plants a tree at a natural biome stop or changes the generation settings to force a pass.

The rendered client test passed, preserving all six main garden trees and all 18 season/fruit combinations across save/reopen, and exporting 16 screenshots. All five natural biome stops contain the intended new tree: Snowy Forest (248, 64, -640), Lake District (4432, 65, -1600), Highland (-2312, 86, -3904), Woodland (-1664, 93, 1728), Wetland (6360, 66, 1240). The Wetland regional stop contained 606 elder leaf blocks in the passing survey. These leaf counts establish presence, not tree proportions.

The final gallery separates the berry stage display from the wood comparison so the wood cannot hide the green row. It uses a normal world with seed `jugcraft`, a labeled authored garden, three seeds per species, six seasonal fruit trees and natural biome tour stops. Inspection time, weather, mob spawning and random ticks are frozen. `/gamerule minecraft:random_tick_speed 3` enables normal fruit ripening and leaf decay again. Fruitwood comparisons: `/tp @s 1013 181 -900 180 0`; berry stages: `/tp @s 1013 181 -850 180 0`.

The final gallery rerun passed (`build/tree5-client-gallery.log`) with 16 screenshots and the same save/reopen checks. A separate delivery copy was scanned across **14,916 saved region records** and all other save files; no actual test content was present. Only development metadata in `level.dat`, `level.dat_old` and `data/fabric/registry_entries.dat` changed. All **112 source files** and the original export remained intact. The cleaned copy then opened in ordinary Minecraft at the garden (1040.5, 180, -987.5), confirmed by the normal client join log.

Delivery: `tree-batch-5-preview-2026-10-16` beside this checkout, containing the inspection save, tour, 16 screenshots, matching JAR, world ZIP, companion pack and SHA-256 manifest. Earlier inspection worlds are preserved.

Commands and local evidence: `./build-local.ps1 -Tasks @('build','runClientGameTest','-PclientTests=TreeBatchFiveClientGameTests','--continue')` (`build/tree5-final-build-client.log`, all server tests passed; initial client Wetland sample failed), `./build-local.ps1 -Tasks @('assemble','runClientGameTest','-PclientTests=TreeBatchFiveClientGameTests')` (`build/tree5-client-regional.log`), `python -m unittest discover -s scripts/tests -v`, `python tools/check_mod_data.py`, `python scripts/check_repository.py`, `python scripts/package_modrinth.py check`. Local Python runs use the existing `JUGCRAFT_FONT_DIR` DejaVu dependency.

No external launcher import, two independent clients, released-world upgrade, live calendar transition over real days or player-capacity benchmark is claimed. Only new chunks receive the generation changes. Preview saves and compiled builds remain outside Git. Batch 5b, 5c and later trees remain unstarted.
