# Companion Cider Barrel

Implemented locally on `peepo-companion` at the owner's request on 11 October 2026 with OpenAI Codex (GPT-6). Reuses the Discovery-tier Cider Barrel and existing Planner assignments, nine-item Filter UI, Supply/Output transport, transaction/cargo handling, priorities, permissions, path budgets and load-dependent delivery energy. No dependency, new artwork/animation, recipe, item or energy currency is added.

## Workflow and progression

Sweet Cider from the Cider Press is supplied to a barrel. Its Filter editor offers explicit Sparkling and Aged buttons without requiring players to own a sample drink. Presets replace the filter with one ghost output. Only sparkling/aged ghosts are accepted; blank or both chooses sparkling, aged-only waits for aged. The barrel owns this saved selection, shared across helpers. Existing town, ownership, distance, loaded-target, menu validity, assignment identity and edit-rate validation protects preset edits as well as ghosts.

The 16-serving barrel keeps the base game's batch age: 24,000 ticks to sparkling, 72,000 to aged. Output is never falsely relabeled: late sparkling collection returns actual aged cider. Products feed drinking, cooking and preserving through the existing item chain. Manual controls and natural aging remain available with no helper. This is a logistics-only job, like the Canning Kettle: Peepo works elsewhere while aging, with no station occupation or aging energy debit. Normal delivery movement costs apply.

## Storage and failure behavior

Companion pours reserve each consumed Sweet Cider bottle for subsequent bottling. A saved, capped reserve accepts extra glass bottles only for uncovered servings, including manually filled barrels. Extraction consumes exactly one real serving and one reserved bottle per finished drink; spare bottles are a separate output view. Both views and insertion use a common Fabric transaction snapshot covering servings, start time, bottles and the loading deadline. Simulation/rollback emits no world updates and creates no items. Resource and readiness are checked live before extraction, including a stage or filter change. Concurrent helpers cannot overfill or double-draw.

An empty barrel opens one fixed 100-tick unloading window for multi-stack cargo. Companion top-ups within it do not move the original start time; after it closes no companion Sweet Cider is accepted until the barrel empties. Manual top-ups retain their original clock-reset behavior and close that window. Full output leaves real cider in the barrel; canceled/obsolete supply uses existing cargo return handling. No unloaded chunk scanning/loading, entity searches, or new tick loop is added. Readiness is constant-time; aging uses the existing world timestamp and 20-tick visual mark update.

Bottles, loading deadline and filter save on the block entity; older saves default to no reserved bottles and sparkling. Shift-empty-hand interaction recovers reserved bottles, retaining overflow. Breaking drops these real bottles separately; the existing BarrelCider item component continues to carry all remaining cider and its age. The work preference is a station setting and resets when the barrel is broken/replaced. The solid barrel is explicitly non-pathfindable.

## Validation and limits

The owner requested no tests. Common/client compilation and assembly passed with `gradlew.bat assemble`; the Modrinth distribution was rebuilt and its bundled mod JAR and SHA-512 checksum verified. Existing deprecation/annotation warnings remain. No automated tests, game tests, or gameplay playtest are run. Manual validation remains for all facings, actual delivery batches, stage transitions, mode changes, concurrent helpers, full Output, manual handoffs and reload/break recovery. Compilation and archive verification alone do not establish gameplay or multiplayer performance.
