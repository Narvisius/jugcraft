# Tree batch 10: baobabs and hanging fruit

Status: implemented, awaiting owner inspection; owner-authorized 18 October 2026. Public branch `codex/tree-generation-batch-10` in `speedygroyper/jugcraft`; [draft PR #417](https://github.com/jimbozoomer-byte/jugcraft/pull/417). Built with OpenAI Codex (GPT-6), from approved PR #413 plus main `ffc7f7bae`. Earlier open tree PRs remain dependencies. No batch 11 work is included.

## Scope and player outcome

Implement the hanging-fruit batch in [the tree roster](../branches/TREES.md#batch-10-hanging-fruit): normal and giant baobabs, the nine-block baobab wood set, evergreen leaves, a sand-tolerant sapling, hanging pods, edible fruit and planting seeds; date palms using existing palm timber/fronds and renewable hanging date clusters. Reuse shared wood, sapling, food, harvest and fermentation systems. Existing chunks are not rewritten.

Outback keeps its current attempt budget with desert acacia 50%, mallee 30%, baobab 10% and giant baobab 10%. Oasis becomes palm 40%, small palm 15%, tamarisk 10% and date palm 35%. Dry River fills its reserved 60% with date palms alongside 40% tamarisk, keeping its established one attempt per chunk. Lush Savanna remains unchanged; its optional lone giant was not selected.

Normal baobabs use a short straight trunk and spreading crown; four saplings grow a broad two-block giant with a sparse crown. Date palms are taller and straight with drooping fronds. Hanging fruit ripens on random ticks and right-click harvest resets the fruit, preserving the tree. Dates yield 2–4 and use the existing fermentable conversion. Fruit has no core progression gate. Soil, obstruction, harvesting, renewal, native biome generation and save/reopen will be tested before delivery.

## Implementation and provenance

Normal baobabs use a straight 5–7-block trunk with acacia foliage. Giant baobabs use a 6–9-block, 2×2 trunk and sparse random-spread crown (radius three, 40 attempts). Date palms use a straight 8–11-block trunk and narrow cherry-style foliage (radius three, height four, 50% bottom/corner holes, 60% hanging leaves and 30% extensions). The planned height-two cherry crown was rejected by the actual 26.3 codec; four is its minimum. Runtime shape tests also showed radius two could leave only three frond blocks, so radius three keeps a usable drooping crown. Fruit decorators attach only below their own leaves, at probability .1 for pods and .2 for dates, with one-block exclusion spacing. These probabilities apply to eligible leaves, not to whole trees. Biome counts are attempts, not guaranteed placements.

Baobab wood extends the shared nine-form registry, stripping, charcoal, hand crafting, sawmill and renewable tree-farm recipes. Normal/giant saplings grow on grass/dirt or dry-vegetation soils including sand/red sand. A blocked 2×2 growth preserves all four saplings. Palm saplings now select ordinary palms 60% or date palms 40% and accept sand. There is no separate date timber or date sapling.

Pods/clusters have three growth stages. Unripe blocks advance one stage with a 25% chance on a random tick; ripe blocks stop ticking. Server-side right-click reads the current state, resets it before dropping 1–2 fruit or 2–4 dates, and rejects another harvest until ripe. Breaking ripe fruit normally drops food; shears or Silk Touch preserve one block without copying ripeness, so replanting cannot duplicate ripe harvests. Removing or replacing its supporting canopy removes the fruit. Baobab fruit crafts into one planting seed. Dates enter the existing fermentable tag and processing routes. No block entity, global scan, new dependency or custom foliage placer is introduced.

Baobab timber follows owner painting 21's `#bb9b8e` palette through the existing `tools/wood_style.py` generator, with grey-brown plated bark. Other wood seeds and previously approved art are preserved. Leaves, sapling, stalk-connected pods/date clusters and food icons use the existing editable 16px Canvas painters in `tools/hanging_fruit_textures.py`. No owner-library executable was run or vanilla texture copied. Main's new alchemy catalogue was extended with generated explicit values for this batch and the earlier tree-stack items; existing numeric values and provenance were preserved.

Existing chunks are not rewritten. Added IDs remain registered when generation/recipe switches are disabled. A save containing new blocks needs the matching build; rollback should restore its prior backup and mod set. The normal inspection world is separate from the user's server world.

## Verification

Validated implementation commit: `ba966adf7`. Verified on 18 October 2026 with Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, JDK 25.0.4.1+1 and the unchanged 18-library framework lock.

- `python -m unittest discover -s scripts/tests -v`: all **87 passed**, with local DejaVu fonts configured. Includes weights/budgets, renewable routes, exact new sprites and connected fruit stalks.
- Generated tree/fruit verification: **180 JSON files** matched their generators. Repository/link and pinned-library configuration checks passed.
- `./gradlew build runClientGameTest -PclientTests=HangingFruitClientGameTests` through the local pinned-JDK wrapper: **BUILD SUCCESSFUL; all 1,959 required server tests passed**. New tests cover 12 seeds per form, reproducibility, shape bounds, own-canopy attachment, single/square saplings on dry soil, blocked giants preserving all four saplings, ripening, hand/shears right-click, repeated-use protection, canopy removal, normal/preserved loot, seed planting, canonical sapling item identity, crafting, sawmill and renewable tree-farm recipes.
- Measured normal baobabs: 6–8 high and five wide; giant baobabs: 8–11 high and five wide; date palms: 10–13 high and five wide, with 32–47 frond blocks. Heights include crowns, not just trunks.
- Rendered client inspection passed: 18 seeded gallery trees, all nine timber forms, fruit growth stages, both leaf rendering modes and naturally generated hanging fruit in Outback, Oasis and Dry River. Saved, closed and reopened the world, checking every gallery tree and all six fruit displays. Screenshots were visually inspected.

The normal **Baobab and Date Palm Inspection** world uses seed `jugcraft`, an authored comparison garden and a separate natural-biome tour. Saplings, seeds, bone meal, shears and an axe are supplied. Random ticks are paused for comparison; `/gamerule minecraft:random_tick_speed 3` restores fruit ripening. Preparation preserved all 95 source save files, scanned 9,213 saved region records, and changed only `level.dat`, `level.dat_old` and `data/fabric/registry_entries.dat` in the delivery copy to remove unused development metadata.

Focused `runGameTest` with the Batch 10 filter and `-PjugcraftOptionalIntegrations=false`: all **four tests passed**. This is server-side absence coverage; a separate client run without the optional presentation libraries was not performed.

All 18 locked library downloads/hashes passed `scripts/package_modrinth.py verify`. Both inspection archives passed integrity checks, the pack's JAR matches the tested build, every archived world file matches the prepared copy, and all dependency pins are unchanged. The ordinary client (without the test mod) successfully opened the prepared world at `(1120.5, 180, -987.5)`, beside the comparison rows. The new trees lie west of that arrival point. This verifies local loading, not external launcher import.

`python tools/check_mod_data.py` passed the complete audit of 2,989 material IDs, data files and recipes, including the art checks of 8,426 block/item models, 3,707 quad parts and 171 combined models. The prepared inspection delivery is in `tree-batch-10-2026-10-18` beside the repository, with a matching JAR, complete pack, world ZIP, screenshots, tour, cleanup evidence and SHA-256 sums. External launcher import, two-client dedicated-server play, prior-release migration, multiplayer performance and owner visual approval are not claimed. Main reconciliation retained the prior separate moss/path assertions. Batch 11 remains unstarted.
