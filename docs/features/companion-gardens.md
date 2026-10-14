# Companion garden work

The subsequent [additional garden crops update](companion-garden-crops.md) adds cane, bamboo, cactus, berries, cocoa and flooded plots; its rules supersede the earlier exclusions below.

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Owner-requested on 7 October 2026, implemented locally with **OpenAI Codex (GPT-6)** on `peepo-companion`, from `2d03b7c7`. This extends the existing assignment, job, cargo and porter systems; no new dependency, inventory, item ID or art asset is introduced.

## Current zone workflow (11 October 2026)

Farmland assignments now use two Planner corners, with a separate Orchard zone option. See [garden and orchard zones](companion-garden-zones.md) for the current limits, server budget, persistence and validation. Existing fixed plots below remain supported without widening or losing route identity; their original creation UI has been replaced for farmland and planters. Mushroom and hanging-herb creation stays unchanged.

## Original fixed-plot setup (legacy saves)

1. Tame Peepo/Jughead and select it with the Companion Planner.
2. Right-click farmland (or a crop standing on it). That soil is the anchor of one garden assignment. It saves up to **eight edge-connected farmland blocks at the same height**, including the anchor. Diagonals, gaps, inaccessible/unloaded cells and soil already assigned to this companion are excluded. A larger field takes the nearest connected eight in a stable breadth-first order.
3. Hold the planner to see each saved soil cell outlined in green. The Jobs row shows `Garden (N blocks)` and the usual location, priority and live status. Left-click any member soil/crop to remove the whole plot; the row's remove button also works.
4. Repeat for up to four work slots: **four plots, at most 32 soil blocks** if every slot is used for gardening. Garden jobs and machine jobs share those four slots. A home, lunch source, Supply and Output remain separate assignments.
5. Use the ghost slot beside each garden job to choose a seed or supported raw crop without consuming it. Right-click the slot (or click with an empty cursor) to restore **Automatic planting**. Put real seeds in cargo or assign a **Supply** container; selected plots fetch only their matching seeds. Assign **Output** for surplus harvests. Use **Work** mode; the separate Porter mode remains ordinary container-to-container transport.
6. Put a hoe in **Hand**, not a cargo slot, for faster, more energy-efficient work. Gathering crops uses the [two-handed hoe swing](companion-harvest-animation.md), with a whole-body wind-up and downward strike. Planting and work without a hoe retain the ordinary interaction clip.

Plot membership is saved, not rediscovered every tick: extending or reshaping a field requires removing/reassigning the plot. Restore a missing anchor or reassign the remaining soil if the original anchor is removed. Other missing member cells are skipped. `CompanionGarden.PLOT_LIMIT` is the single current balance constant (8); it is not a new user-facing config setting.

## Actual crop behavior and costs

Companions walk to a safe position on or beside a selected crop and work locally. Mature ordinary crops are harvested with their normal loot; one matching seed is retained from that harvest, or consumed from cargo, to replant the same crop. If neither exists, the crop is harvested and the empty soil waits for seeds. With no crop selection, empty cells use the first compatible seed in cargo; Supply searches its usual bounded storage views for a compatible seed when cargo cannot cover the empty cells. A selected plot filters both planting and Supply to its chosen seed, so four plots can grow four different crops from the same Supply container.

Supported plants are vanilla `CropBlock` crops, Jugcraft's ordinary crops, and Jugcraft's corn, ornamental corn, sunflower, pepper and trellised tomato. Matching or automatically selected tall plants are picked back to their existing regrowth age without shortening or breaking the plant. Tomatoes need an existing trellis on the soil, and additional trellis above to grow normally. Tall crop tops are individually validated before picking. Leave reachable aisles around solid tall crops. Other growth systems, including adjacent stem fruits, berries, sugar cane and trees, are outside this first garden adapter. Soil must already be tilled; hoes do not expand the plot, till dirt, irrigate, apply fertilizer or accelerate crop growth.

| Work | Active time at 20 TPS | Reserve cost | Tool wear |
| --- | ---: | ---: | --- |
| Harvest/replant one mature ordinary crop, pick one tall plant, or plant one empty cell | 80 ticks / 4 seconds | 4 JE/t, 320 JE per uninterrupted action | None |
| Same action with a hoe in Hand | 40 ticks / 2 seconds | 4 JE/t, 160 JE per uninterrupted action | 1 durability per completed action, using normal enchantment handling |

Pathfinding/search delays and deliveries are additional. An interrupted action restarts; effort already spent is not refunded. Tool material does not add another speed multiplier. Food regeneration can offset the net reserve drain. Existing recovery thresholds, rest, shifts, eating, priorities and the settings-menu pause continue to apply. Transport retains its existing up-to-2-JE/t walking cost. This is early farming utility: seeds/soil/tools and companion food are inputs, and harvests feed cooking, lunch or later processing. It creates no electricity and bypasses no crop growth/food cost.

## Tomato overripeness compatibility (10 October 2026)

Implemented with OpenAI Codex (GPT-6), from `66dd5f28`. Companion picking now uses the tall crop's own state-dependent produce and canonical regrowth states, matching manual picking. Ripe tomato vines give tomatoes; overripe vines give rotten tomatoes. Both sections reset to the normal regrowth age with overripeness cleared. A plot changing to another crop still restores the trellis, and full cargo leaves the entire vine and inventory unchanged. Existing yield, effort, energy, ownership and transactional inventory rules remain intact; no new scans or saved fields were added.

Crop replacement also suppresses intermediate shape updates while removing the tall plant, so replacing its top section cannot break the remaining sections, destroy the trellis or spill duplicate crop loot. Rollback restores sections with the same protection.

The focused `PeepoHarvestClientTests` covers fresh and overripe tomatoes, both companion types, full cargo, crop replacement, two-section resets and absence of dropped duplicate produce. On 10 October 2026, `./gradlew runClientGameTest -PclientTests=PeepoHarvestClientTests --console=plain` passed **185 assertions**, including the existing hoe animation and wheat harvesting checks, on Windows with Java 25.0.4.1, Minecraft 26.3 and Fabric Loader 0.19.5. This compiled the main, client and test sources and ran only this client test class in a disposable world. No full suite or multiplayer playtest was run.

## Supply, output and conservation

- Each garden row uses the existing **Supply Auto/On/Off** and **Output Auto/On/Off** buttons. Gardens have no external machine item port to detect, so Auto permits the companion's transport. There is no ghost recipe requirement for a garden.
- Supply brings only enough plantable seeds for that plot's current empty cells after considering carried seeds. Pickup occupies a real cargo slot and reserves that stack while travelling. Arrival releases the seed stack for planting; it does not insert a second copy into an invented garden inventory. Unneeded seeds return using the existing return route.
- Harvests occupy the existing eight cargo slots. They keep per-slot, per-plot provenance, so a plot with Output disabled cannot have its harvests exported by another enabled plot. Player-given food, tools and unrelated cargo are never selected as garden output. Editing a cargo stack manually clears that slot's harvest provenance, handing control of that stack back to the player.
- Harvests of the same variant from the same plot may stack; they do not automatically merge into unmarked player cargo or another plot's marked cargo. Ordinary seeds retain a buffer equal to the companion's assigned garden cells that accept that seed (matching selections plus automatic plots), at most 32 per seed variant; only surplus marked seeds are exported. Carrots/potatoes can still be eaten under the existing food policy.
- Output groups already-carried harvest stacks and walks them to the container under the [batch delivery rules](companion-batch-deliveries.md). It does not require an extra empty cargo slot, so full cargo can still unload. Container permissions, sided access, capacity, assignment identity and load state are rechecked through the existing transport implementation.
- Crop loot and seed changes use inventory/provenance snapshots; if the complete harvest will not fit, the crop stays intact. Failed block changes roll back inventory changes. Full cargo causes a cooldown instead of dropping item entities. No custom Fortune bonus is added by the hoe.
- Invalidated assignments/routes leave undelivered items in cargo rather than deleting or rerouting them. The existing in-flight transport manifest is saved along with the plot identity. Death drops the same real cargo once; there is no second garden inventory to drop.

## Server authority and performance

Planner edits retain owner/party, player reach, dimension, rate, loaded-chunk and town protection checks. Garden discovery is a bounded horizontal traversal (eight accepted cells, at most 29 visited positions); it never forces chunks. The assignment snapshot has at most four lists of eight positions. Save/sync input is capped, deduplicated and constrained to an anchor-connected horizontal list.

Readiness scans check at most eight saved cells per plot and use the existing server-wide search budget, with an 80-99 tick refresh while idle. Active work rechecks only its selected soil/crop; tall harvests touch at most three plant sections. Navigation uses the shared path budget, timeouts and unreachable backoff. A 100-tick renewable per-soil UUID lease prevents overlapping companions from working the same crop simultaneously. Leases hold no entity references, are released by the normal job lifecycle, and have a bounded per-level table with rotating cleanup of expired entries. No entity/world scans, crop tick acceleration or chunk loaders are added.

Every modified soil/crop must remain loaded and outside protected towns, and vanilla `mobGriefing` must be enabled. The GUI reports growing crops, missing seeds, missing soil, access/path problems, full cargo and a disabled gamerule. Clearing/reordering assignments, food, rest, schedule changes and unload use the existing cancellation lifecycle. Garden save keys are additive; old targets and transport routes without a plot list load normally. Client and server should use the same build for outlines and the appended status IDs.

## Per-plot crop selection

Owner-requested follow-up implemented with OpenAI Codex (GPT-6), from `888caa4b`. The existing four ghost slots (46-49) now also serve gardens; no inventory slots or message types were added. A selected seed icon appears beside its garden row, and an empty icon means automatic planting. Unsupported items leave the current selection unchanged. Drag, shift-click, swap, clone and throw still cannot extract or insert a ghost item.

Supported seeds may be selected directly. Raw wheat/beetroot map to their seeds; carrots, potatoes and other crops that plant themselves remain their own seed. Jugcraft corn, ornamental corn, tomato, pepper, sunflower, flax, cabbage, oats and barley map to the corresponding planting seed. This is a choice only: clicking produce never converts a real item into seeds, and cooked food is not accepted. Normal farmland, light and trellis requirements still apply.

Changing the choice does not destroy young crops. When a different ordinary crop becomes mature, the companion harvests it without reserving a seed to replant that old crop. A different mature tall crop yields its usual picked produce and its plant sections are cleared instead of regrowing. The chosen crop is then planted in a separate normal work action when its real seed and suitable growing space are available. Missing seeds leave the cleared cell waiting; the previous crop is not replanted as a fallback. Tomato plant removal restores its existing trellis sections, preserving that infrastructure; remove those trellises to switch to an ordinary crop, or provide trellises when switching to tomato.

Each assignment saves an optional `GardenSeed` item ID. It follows row reordering/compaction, clears when the assignment is removed, and missing/invalid values load as automatic. Menu edits retain owner/party, loaded-state, distance, protected-town and rate checks. Icons synchronize through the existing menu slots to every viewer. Selection edits invalidate the cached garden action. The Supply plan identity includes the chosen seed, so an obsolete in-flight seed delivery follows the existing return-to-Supply behavior instead of planting the wrong crop. Seed output buffers count only plots that accept that seed. No additional periodic world or registry scans are introduced.

## Validation

**No automated tests or in-game tests were run, as requested.** Common/client compilation and JAR assembly passed via `build-local.ps1 -Tasks assemble`; the complete Modrinth pack was then rebuilt. Compilation/packaging is not evidence that navigation, crop interactions or multiplayer gameplay passed.

Crop-selection follow-up: common/client compilation and assembly passed (`build/garden-selection-assemble.log`), and the complete pack was rebuilt. No automated or in-game tests ran. Manual crop-choice checks remain: ghost item conservation and invalid clicks, clearing to Auto, four different seeds from one Supply, wrong seeds in cargo, mature replacement versus young crops, tall plants/trellises, full cargo, changing selection during delivery, multiple viewers, reordering/removal, and save/restart.

Pending manual checks: irregular/oversized fields and four-plot limits; vanilla/Jugcraft crops and tall plants; hoe timing/wear; mixed seed supplies; output full/off and completely full cargo; player cargo edits; two workers on overlapping plots; lower-priority jobs; rest/menu interruption; save/restart while carrying seeds or harvests; unloaded chunks, removed anchors, protected cells and `mobGriefing` disabled.


## Planter boxes, mushroom colonies and herb drying (11 October 2026)

Owner-requested extension implemented with OpenAI Codex (GPT-6), on the local `peepo-companion` branch after the tomato compatibility fix. The owner chose ready-made Supply bundles; loose-herb/string crafting is outside this change.

Crop plots now accept Planter Boxes alongside farmland. Mushroom plots accept Rich Soil and the colony's mushroom substrate tag, plant real brown/red mushrooms, and pick mature colonies with shears or knives. Tool retrieval reuses Supplies marked Tools and respects the Hand lock. Picking preserves the colony at `PICK_RESET`, returns its normal 2-3 mushrooms, charges 4 JE/t for 40 ticks and wears the tool once. Planting costs 4 JE/t for 80 ticks. No growth, light or shade rule changes.

Drying plots discover up to eight adjacent existing hanging bundles. Saved cells point to permanent supports; the existing target `Face=DOWN` distinguishes them from soil plots. Harvesting the bundle therefore does not destroy the assignment. Empty saved spots accept compatible ready-made bundles from cargo/Supply, fresh bundles remain untouched until naturally dried, and dried bundles use their real loot table. Hanging and collection each cost 4 JE/t for 80 ticks, with no hoe bonus or tool requirement. No new recipe/currency or duplicate inventory is introduced.

All types use the existing assignment priority, crop filter, Supply/Output settings, eight-slot cargo, harvest provenance, leases, cooldowns and global search/path budgets. Discovery never crosses plot types and remains capped at eight positions. Saved data retains the same plot/face schema and transport snapshots; no entity/world scan or chunk loading is added. Hanging approach candidates check at most ten nearby floor positions per saved cell, constrained by normal collision, hazards, protection and 2.5-block interaction reach. Full output capacity aborts the transaction without changing the crop or bundle. Unsupported filter items are rejected for each plot type. Frames and tooltips distinguish drying positions and mushroom tools.

Focused verification passed on Windows with Minecraft 26.3, Fabric Loader 0.19.5 and Java 25.0.4.1: `PeepoGardenExpansionClientTests` passed 37 assertions covering plot caps, bundle canonicalization and save/reload, filters, real Supply/Tools/Output travel for all three additions, natural-drying waits, mushroom regrowth/tool wear, missing supports and full-inventory conservation. `PeepoHarvestClientTests` passed 185 assertions for existing crops/animations and tomato regressions (222 assertions total). The expansion suite passed again after the final hanging-approach and GUI-label adjustments; common/client compilation also passed. Logs: `build/garden-expansion-tests.log` and `build/garden-expansion-final-tests.log`. No full suite or multiplayer test is claimed.

The new client suite has a 60-second scheduling estimate based on focused runs of about 55 seconds. The CI scheduler now balances residual skew by swapping classes between the longest and shortest groups, without changing coverage or relaxing the existing time-budget check. All five client-test-selection unit tests passed, including registration discovery, complete nonduplicated coverage and the four-group budget.

## Magic garden integration (11 October 2026)

Awakened Verdant Beds now participate in existing garden zones, with native organism yields and retained regrowth stages. Planting, harvest cargo and linked Supply/Output reuse the bounded garden scheduler. See [companion magic gardens](companion-magic-gardens.md) for permissions, costs, device logistics and validation limits. Maintenance remains deferred.
