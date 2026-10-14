# Companion water collection

Owner-requested implementation, 12 October 2026, using OpenAI Codex (GPT-6). Reuses Planner assignments, four work slots, Supply/Output, physical cargo, energy, budgets and the general interaction animation. No new blocks, assets, dependencies or platform changes.

## Behavior

- In normal assignment mode the Planner ray targets source water. Right-click assigns the source to the selected companion; left-click removes it. The menu also removes assignments, including a depleted source.
- Glass bottles become water potion bottles. Empty buckets become water buckets through native source pickup. Partially filled or empty Gourd Canteens refill to capacity (three), preserving their other components.
- Supply requests at most four total compatible containers after counting unreserved carried inputs. Full containers use existing marked cargo and Output delivery, including multiple linked containers. Player-given compatible inputs may also be used.
- An uninterrupted fill takes 40 ticks at 4 reserve JE/t (160 JE). Hauling uses normal cargo-scaled effort. Existing work animation and native fill sounds are reused.
- Bottles and canteens preserve water. Buckets consume the actual source; normal fluid ticks can renew an infinite pool. There is no generated water or direct machine-tank transfer.
- A missing source pauses work while retaining the assignment and completed cargo. A short handoff grace allows a renewable source to refill. Work requires mob-griefing, permission, loaded positions, energy and safe dry footing within reach.

## Server and inventory handling

Only explicitly assigned sources are considered. There are at most four cached jobs per companion. Approach discovery checks at most 24 bank candidates once per second under the shared search budget, respecting failed approach memory. No entity-area scans, fake players, chunk tickets or extra ticking block entities are introduced.

Cargo conversion and energy use Fabric transactions. Capacity probes roll back; conversion can reuse a consumed nonstackable container's slot. Bucket pickup is performed only after the real container/output conversion fits, with the source freshly validated, followed immediately by transaction commit. Concurrent companions run on the server thread and recheck the source. Filled containers retain their originating assignment for existing Output routes. Supply handoff releases the already-carried inputs without inserting duplicates or requiring a trip into the water.

## Validation

The owner subsequently requested tests on 12 October. See the [companion integration regression](companion-integration-tests.md) for actual runtime coverage, fixes and remaining limits. The original implementation-time checks below are retained as historical evidence.

Common/client compilation and assembly passed (`build/companion-water-build.log`, final build 38 seconds). Automated tests and gameplay were not run. The complete Modrinth distribution was rebuilt; its embedded JAR matches the final build and its SHA-512 matches the sidecar. All 18 locked libraries and existing platform pins were preserved. `git diff --check` passed. Packaging is not gameplay validation.

Manual checks remaining: Planner source selection/removal; all three container types; named/partly filled canteens; renewable and finite bucket sources; inaccessible banks; full inventory and Output; multiple linked Supply/Output containers; two companions sharing a source; save/reload; job removal/reordering; permissions, mob-griefing and unloaded chunks.
