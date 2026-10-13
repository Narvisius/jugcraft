# Companion planning, synchronization and movement

Owner requested performance suggestions 1, 4, 5 and 6 on 8 October 2026. Implemented with OpenAI Codex (GPT-6) on `peepo-companion`, from gameplay base `bd302b9a`. This does not change reserve capacity, production bonuses, costs, filters or assignment limits.

## Planning caches

Cooking pots, processors and cider presses share their workstation-only readiness result for one server tick. The cache uses weak block-entity keys and values containing only the tick and status, so it cannot keep a world loaded. It is shared across players and parties; different dimensions have different block entities.

Each companion separately caches resolved jobs and personal planning results for the current tick. Assignment edits, inventory changes and routine release clear these results. Job resolution checks the current block entity identity, including same-tick replacement. Loaded-chunk checks, access permissions and reservation availability remain fresh. No player or party's access decision is reused for another companion.

These are conservative planning snapshots, with at most one tick of staleness. Actual claims, production and item transfers retain their live checks and transactions. They do not grant permission or create resources. There are no new chunk tickets or global entity scans.

## Client updates

The server retains exact reserve energy and remaining food/eating ticks. Energy snapshots go to tracking clients every ten ticks, with zero/full boundaries published on the energy tick. Open machine/companion menus still read the exact server reserve through their existing menu data. Energy transaction rollback remains exact.

Eating and meal-buff durations synchronize their end times when started, refreshed, cancelled or expired; the client counts down locally. Remaining ticks keep their existing save fields, and end times are reconstructed on load. Unloaded companions gain no offline energy or meal progress. Both client and server must use this version because synchronized entity fields changed.

## Rest and navigation

- Bunk entrance/clearance and occupied vanilla-bed clearance are cached for up to twenty ticks. New bunk occupancy checks clearance immediately, and adjacent block updates invalidate the bunk cache. A blocked ladder exit retries once per second and checks each candidate once.
- Stationary wheel, bed and seat occupants only receive a position correction when displaced. Vanilla-bed reservation records renew near expiry instead of being replaced every tick. Lease/ownership checks remain active.
- Companions retain paths while making progress. Progress is sampled every ten ticks; a finished path, moved target or forty ticks without progress requests a replacement under the existing path budget. Replacement stops the old route so vanilla cannot return its cached stuck path. Station leases renew independently of path creation.
- Each companion remembers up to 32 failed station entrances for 200 ticks. Supported productive jobs and deliveries try another entrance before repeatedly choosing the same blocked side. Lunch searches retain their selected successful path. Command following and loose-food pickup also reuse progressing paths.

## Validation

Final run on 8 October 2026 completed at 11:50 local time: **52 groups and 874 assertions, zero failures**. `PeepoCompanionClientTests` passed 42 groups / 715 assertions; `PeepoHearthClientTests` passed 10 groups / 159 assertions. Compilation and JAR assembly succeeded in the same invocation (9 minutes 28 seconds). The repository structure/documentation-link check passed, and the tested JAR was packaged into the offline Jugcraft Complete `.mrpack`.

```powershell
.\build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCompanionClientTests,PeepoHearthClientTests','assemble')
python scripts/check_repository.py
python scripts/package_modrinth.py build
```

Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Java 25.0.4.1+1, with the locked development integrations. Both classes used disposable flat worlds; the companion fixture's logged origin was `(0,-56,0)`, with its automation/performance area at `(0,-56,30)`. The hearth fixture used `(0,-56,0)`. No player worlds were opened. Only these two of 111 available client-test classes executed; the shared test source set was compiled.

New runtime checks cover shared readiness reuse, personal isolation, live claim refusal, same-tick block replacement, exact transactional energy, snapshot cadence, stable meal timestamps, blocked bunk release and navigation progress/failure memory. Existing wheel, feeding, processor helpers, budgets, logistics, menus, serialization, world save/reopen and one-client dedicated-server reconnect coverage passed. Hearth checks include all four orientations, interrupted loading, full output and carried-pie persistence. The Filter GUI and hearth-wait screenshots were inspected. Logs and screenshots remain local under `build/companion-performance-final.log` and `build/run/clientGameTest/screenshots/`.

This is a reduction in repeated work, not a measured multiplayer capacity claim. A many-player, multi-dimension factory load benchmark and two independent client sessions remain separate validation work.
