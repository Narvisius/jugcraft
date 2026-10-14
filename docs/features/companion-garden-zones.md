# Companion farmland and orchard zones

The subsequent [additional garden crops update](companion-garden-crops.md) adds cane, bamboo, cactus, berries, cocoa and flooded plots; its rules supersede the earlier exclusions below.

Implemented with OpenAI Codex (GPT-6), 11 October 2026, at the owner's request. Extends the existing garden jobs, crop choices, two-corner Planner, per-block claims, eight-slot cargo and linked Supply/Output. No new dependencies, packets, item registrations, assets, fake players or chunk loaders.

## Player behavior and progression

The Planner menu offers Farmland and Orchard alongside Livestock Management. Each zone uses two inclusive ground corners, maximum 16×16 horizontally and three blocks of ground-height difference, and occupies one of four work slots. Corner frames preview valid/invalid sizes; saved farmland zones show green bounds and orchards show yellow-green bounds. Remove either corner or the job row. Zone identity uses the first corner and survives replacement of that ground block. Permissions, reach, ownership, dimension and cooldown remain server-authoritative.

Farmland zones discover actual farmland and planter boxes between the corner heights, allowing paths, gaps and newly tilled ground. They retain the single crop/seed choice, mature harvesting/replanting, native tall-crop regrowth, actual seed Supply and marked-harvest Output. Soil is not tilled automatically. Four-zone seed reserves stay bounded to 32 per compatible seed variant (eight per zone); supplies only fill currently observed planting needs, rather than all potential blocks. Existing hoe timing, wear and JE costs remain unchanged.

Orchards inspect fruit-bearing leaves through eight blocks above the higher ground corner. They support the shared FruitingLeavesBlock behavior: apples, chestnuts and all six OrchardTree kinds. Native fruit yields are reused via a public yield-only accessor, with inventory capacity checked before resetting FRUIT to zero. Leaves/logs stay intact. Safe standing positions are searched below and beside clusters, with unobstructed overhead reach up to eight blocks; trunks, floors and intervening canopy collision prevent picking. Harvesting takes 80 ticks at 4 JE/t, plus existing load-based delivery costs. No tools, saplings, water or fuel are consumed. The existing general interaction animation is reused. Tree planting, felling, loose ground-item collection and unrelated modded fruit systems are outside this change.

Both workflows consume food/companion reserve and existing agriculture infrastructure, producing food/crafting inputs without changing natural growth or yields. Orchard output uses the same per-slot, per-assignment provenance as crops and livestock. Full inventory rolls back all cargo changes and leaves the ripe cluster intact. Shared expiring cell claims and live block-state checks prevent double harvesting by overlapping workers. No new inventory is introduced.

## Persistence and bounded discovery

GardenZone stores only first/last corners and orchard kind. Target save/load, client assignment synchronization, and in-flight transport manifests carry that same metadata. Legacy eight-cell plots and their crop selections, names, route links and provenance remain unchanged; they can be removed and redrawn explicitly. Mushroom colonies and hanging herbs keep their existing connected plots.

A job inspects at most 32 candidate positions per discovery slice, no more than once per second and only under the shared search budget. It retains at most 32 observed cells for current work and supply/path queries. Orchards scan vertically per column; farms scan by height. No repeated full-volume scan occurs for each item candidate. Work selection checks this bounded window with at most four zone approach searches per pass; all new paths still use the existing path budget and unreachable memory. Partial zone sweeps request another scheduler search after one second; completed idle sweeps use the normal growing cooldown. Discovery cache/cursors are transient and rebuild gradually after reload. Unloaded cells are skipped without loading their chunks; permissions and mob griefing are rechecked for real mutations.

Supply/output reuse existing policies and containers. Orchard Supply has no current demand; Output exports only that job's marked fruit. Moving/removing assignments or entering rest/menu states follows existing cancellation. No server-wide crop ticker or registry scan was added.

## Validation

Owner's current preference remains manual gameplay testing, so no automated tests were run. Common/client compilation and `assemble` passed. `git diff --check` passed. The rebuilt Modrinth distribution contains the byte-identical final mod JAR, its SHA-512 matches the sidecar, and the pack builder confirmed the existing 18 locked libraries and platform pins. Gameplay still needs verification: 1×1 and reversed corners; 16×16 acceptance and 17-block rejection; slopes; newly tilled cells; legacy saves and in-flight seeds; crop selector; overlapping workers; fruit regrowth/yields; full cargo/output; overhead obstructions; all eight supported fruit kinds; unloaded chunks and protected cells; rename/reorder/removal; client outlines and Planner scaling. No multiplayer load or rendered gameplay result is claimed.
