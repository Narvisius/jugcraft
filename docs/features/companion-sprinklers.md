# Companion sprinkler restocking

Owner-requested implementation, 12 October 2026, using OpenAI Codex (GPT-6). Extends existing Planner work assignments and bounded Supply/Output logistics. No new block, asset, animation, recipe, framework or dependency.

## Progression and behavior

Uses the existing workshop-tier sprinkler (bronze fluid pipe, steel plates, tinplate tank and hopper), ordinary water buckets and chemical Fertilizer. The preceding companion water-collection job can supply filled buckets from renewable source water. Crops and magical beds retain their native sprinkler interactions and growth rules.

Each explicitly assigned sprinkler occupies one of the four work slots. Supply delivers water buckets and `PetroItems.FERTILIZER` (`jugcraft:fertilizer`, displayed as Fertilizer). It keeps at most four fertilizer items stocked without removing extra player/hopper stock; native capacity remains sixteen. The existing 4,000 mB tank accepts a whole 1,000 mB bucket only when it fits. Fertilizer is optional for watering. No bottles/canteens, guano or bone meal are accepted by this adapter.

Output retrieves real empty buckets from a four-bucket buffer. Missing/full Output eventually stops bucket refills when that buffer fills, without deleting containers. Empty-hand sneak-use retrieves the buffer manually under player build, interaction and town-protection checks. Buffered buckets persist with the existing block entity and drop on removal. Old saves default to an empty buffer. Existing manual water/fertilizer interactions, fluid piping and fertilizer hopper insertion remain intact.

The sprinkler's native 50 mB/100-tick pulses, six-pulse fertilizer cadence and growth effects are unchanged. The companion supplies resources and spends only the existing load-scaled hauling effort; no stationed worker, machine speed bonus or additional work-energy charge. Supply/Output Auto/On/Off remain available. Auto uses real resource/capacity checks; it does not infer external automation for this non-machine block from nearby pipes/hoppers.

## Transactions and multiplayer cost

The shared logistics planner probes and rolls back destination insertion. A nested Fabric transaction groups tank insertion and the returned bucket; outer cargo extraction commits with that delivery. Fertilizer uses the native transactional hopper. Final-commit notifications mark the block dirty and invalidate readiness. Concurrent companions recheck actual tank/hopper/output capacity during delivery, so stale plans return unused supplies through existing handling.

No new world scan or ticker is added. Resolution touches only explicitly assigned, loaded blocks and validates protection and block identity. Approach finding, inventory scanning, batch cargo, route limits, settings pauses, rest and job priority all reuse existing companion systems. Sprinklers are separate station assignments; garden zones do not discover or operate arbitrary nearby sprinklers.

A two-chest bucket loop uses Water collection Supply = empty-bucket chest, Output = filled-bucket chest; Sprinkler Supply = filled-bucket chest, Output = empty-bucket chest. This costs two work assignments on one companion, or may be split across companions. Normal infinite-water renewal is still required for repeat bucket collection.

## Validation

The owner subsequently requested tests on 12 October. See the [companion integration regression](companion-integration-tests.md) for actual runtime coverage, fixes and remaining limits. The original implementation-time checks below are retained as historical evidence.

Common/client compilation and assembly passed (`build/companion-sprinkler-build.log`; final assembly 22 seconds). Automated tests and gameplay were not run, following the owner's current preference. The complete Modrinth distribution was rebuilt with all 18 locked libraries/platform pins preserved. Its embedded JAR matches the final build, includes the sprinkler adapter, and its SHA-512 matches the sidecar. `git diff --check` passed. Compilation and packaging do not establish gameplay or multiplayer correctness.

Manual checks remaining: assignment/removal/reordering; multiple linked chests; four-bucket/fertilizer limits; partially full tank; full/disabled Output; pipe/player/hopper changes during deliveries; transaction probes with no consumption; two workers sharing a sprinkler; closed bucket loop; reload and block-break drops; manual bucket retrieval; permissions/unloaded chunks; existing watering/fertilizer pulses.
