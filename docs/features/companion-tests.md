# Companion regression tests

Owner requested companion-only automated testing on 7 October 2026, superseding the earlier instruction to skip tests. Implemented and run with OpenAI Codex (GPT-6).

## Performance revision, 8 October 2026

The [planning, synchronization, rest and navigation changes](companion-performance.md) passed the expanded companion suite: **42 groups, 715 assertions, zero failed groups**, completed at 11:46 local time. The additional four groups check shared readiness reuse, per-companion isolation, live claim validation, same-tick block replacement, exact energy rollback, reduced snapshot frequency, stable meal timestamps, obstructed bunk release, retained progressing paths and alternate entrance selection. World save/reopen and the one-client dedicated reconnect check passed again.

Existing GUI fixtures now open the nine-item Filter editor and exercise its real ghost slots, preserving cursor-item and extraction checks. The cooking fixture uses the current collision rim height. An exploratory run exposed an old slot-count expectation and an ungrounded navigation test entity; both fixtures were corrected before this successful run. A legacy multiplication character in the hearth documentation was normalized to UTF-8 so the repository link check could complete.

The final combined invocation also runs the hearth-only suite and assembles the mod; see the performance record for the complete result and limits. No unrelated gameplay test classes were selected. Current generated evidence lives in `build/companion-performance-final.log` and `build/run/clientGameTest/screenshots/`.

## Earlier result, 7 October 2026

**38 groups passed, 511 assertions, zero failed groups.** The successful expanded run finished on 7 October 2026 at 21:41 local time, on `peepo-companion`, against gameplay base commit `7633a769` plus the test additions recorded here. Gradle completed successfully in 5 minutes 20 seconds. No production gameplay changes were required by this run.

The original 19-group / 143-assertion suite also passed unchanged at 21:14 before expansion. The repository structure/documentation-link check passed after correcting three legacy Windows-encoded en dashes to UTF-8 in companion documentation; the wording is unchanged.

Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.0.4.1+1, with the repository's normal development integrations. Tests used newly generated, disposable flat worlds. No player saves were opened. Only `PeepoCompanionClientTests` executed; unrelated test classes share the compilation source set but their suites did not run.

Run from the repository root in PowerShell:

```powershell
.\build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCompanionClientTests')
```

The equivalent Gradle command is `./gradlew runClientGameTest -PclientTests=PeepoCompanionClientTests`. Do not use unfiltered `build` or `runClientGameTest` when requesting companion-only coverage.

## Coverage

- Feeding and first-feed ownership, friendly fire, blush, costume equipment, full-health/full-energy refusal, the 95% energy threshold, healing, meal buffs and restoration of the held torch after eating.
- Initial energy reserve, transactional rollback, the shared 64 JE/t extraction limit, no regeneration during work, moving passive regeneration, chair regeneration, daytime sleep refusal, food quality and recovery hysteresis.
- Eight cargo slots and two equipment slots, rollback, inventory/owner/equipment serialization, costume-slot restrictions and invalid companion-menu inputs.
- Home plus four jobs, rejection of a fifth job, ordering, removal compaction and assignment serialization.
- Lunch crate and chest-cover item conservation, full cargo, and foreign lunch-source ownership.
- Actual wheel energy conservation, repeated same-tick calls, full buffer, exclusive occupancy, release and side-only energy lookup on both lower blocks.
- Stool edge position and cardinal/diagonal rail seat geometry. This exercises the diagonal surface adapter directly; it does not establish compatibility with a separately installed diagonal-fence mod.
- Sixteen bed registrations, shared upper/lower bunk entrance, Jughead sleeping, increased sleep regeneration and ground-level exit.
- Sixteen simultaneous budget requests: four searches and eight paths admitted in one server tick, with the rest deferred. This is a budget assertion, not a server-load benchmark.
- Cooking recipe plans, invalid recipe IDs, required heat/ingredients, full outputs, ingredient filtering, one-helper exclusivity, energy cost, repeated same-tick assistance and release.
- Both companions automatically approach and stir a pot. Measured progress over 40 server ticks matches 1.5x speed and 16 JE/t energy cost. Client animation state arrives and both rigs render with the angled spoon. Feet match the rim surface and edge, and the entity remains stationary throughout stirring.
- Model pose assertions for closed sleeping eyes, Jughead clothing, pumpkin visibility, general two-arm work, forward spoon grip, moving stirring arms, planted feet, alternating seated kicks and running strides.
- Automatic loose-food pickup and eating; swamp spawn registration with groups of 1–4, separate six-per-variant local caps and saved natural-spawn origin.
- Actual companion ghost-recipe slot synchronization and slot packets: setting/clearing preserves the four real cursor items, a one-item ghost remains separate, and shift-click, creative clone, drop and hotbar swap cannot extract it. The ordinary pot GUI rejects recipe-edit buttons. Companion command packets, world save/reopen, and a real in-process dedicated server with one client disconnecting/reconnecting. Owner, cargo and preferences survive reconnect.
- Automatic seated companion unload: release does not move the entity during section removal; its safe exit occurs on a subsequent tick. A world holding the seated companion closes and reopens successfully.

Runtime screenshots of the companion GUI, ghost recipe slot, ordinary pot and both stirring variants were inspected. Local evidence is under `build/peepo-rim-evidence/`; Gradle's disposable run files are under `build/run/clientGameTest/`. These generated logs, images and worlds are not committed.

The expanded run's log and screenshots are preserved in `build/peepo-automation-evidence/`, with the complete console log also at `build/peepo-new-features-final.log`. The new planner screenshot confirms blue Supply, yellow Output and the selected companion's green frame. The expanded GUI screenshot shows the custom Jughead name, four jobs, inline transport buttons and separate hearth/furnace recipe ghosts without overlapping controls.

## Added automation coverage

The focused suite also runs `CompanionAutomationChecks` and `CompanionClipChecks`; these helpers are not separate test entry points. They use the same disposable world and the real server/client runtime.

- All 36 supported processor kinds expose the expected helper count and work clip. Actual compact and full-size electric furnaces exercise autonomous helper approach, exclusive worker slots, 1.5x combined progress, normal electricity payment for extra production, and the then-current 16 JE/t for the compact helper or 8 JE/t per large-machine helper. This is representative production coverage, not a recipe test for every processor. The 9 October [machine effort revision](companion-jobs.md#machine-effort-balance-and-recipe-overrides) changes furnace rates to 4/2 JE/t; its expectation was updated but the suite was not rerun, per the owner.
- Machine item ports detect committed insertions and view extractions, ignore aborted and nested-aborted transactions, exclude companion transfers, and follow changed side configuration without reacquiring the port. Input and output hopper detection are separate. Auto disables only the observed direction; On overrides it; Off blocks it.
- A multi-ingredient alloy recipe accepts exactly one batch and rejects unrelated items. Output collection cannot extract input ingredients. Recipe selection saves, restores, clears, and rejects invalid outputs.
- Supply and Output modes follow workstation reordering, removal compaction and serialization. Planner role changes preserve clicked faces, swap an existing pair, and reject an invalid opposite endpoint without partially changing assignments.
- Real planner event handlers select the owned companion, use the same right-click gesture for both container roles, swap them, and consume left-click removal without breaking the container. Client assignment synchronization and rendered role frames are captured.
- A porter physically picks up a maximum 32-item stack, retains cargo when Output becomes full, and resumes to deliver the complete source quantity with its custom item components. Serialization preserves the carried manifest and Porter mode. Two active porters compete for one source; every sampled server state retains exactly the original total across source, cargo and destination.
- Disabling Supply during a trip returns the already-collected ingredient to its source without inserting it into the processor or duplicating it.
- The companion menu holds a moving companion in place while open, accepts the Porter and independent Supply/Output command packets, and releases movement after closing. Whole-pie and standard-machine ghost slots set, clear and reselect recipes through actual inventory packets without consuming cursor items.
- Both rigs, with and without the pumpkin costume, exercise the valve, lever, mallet, wrench and crank clips over eight phases. Arms and bodies move, transforms remain finite, and returning to idle resets arm scale. These are pose assertions, not visual approval of every tool from every camera angle.
- The crank pays for accepted kinetic power exactly once per tick, pauses for a full flywheel, stays paused at 95%, resumes below 90%, and yields to a player operating the crank.
- Cider input capacity, transactional apple/bottle conversion and output rollback preserve quantities. A real companion supplies apples and bottles, operates the press and delivers cider.
- Canning checks transactional water filling, returned buckets, required heat, jar freshness components, spoiled-input refusal, and protection of unfinished jars. A real companion supplies the kettle and delivers sealed jars and the empty bucket.
- Hearth checks selected raw pies, transactional input/output, bounded log fuel, oversized-fuel rejection, one tender, and destination-capacity checks before starting a pie. A real companion fetches a raw pie and logs, tends the oven and delivers the baked result before it burns.

Test fixtures use a synchronized player teleport (server-only `snapTo` does not move the connected client), the furnace recipe that actually matches iron ore, and logs from the hearth's wood tag. GUI fixtures wait for server confirmation of item-return packets before replacing an inventory slot, and use server ticks for command cooldowns. Earlier exploratory failures from those fixtures are retained in the local logs; they were not production-code fixes.

## Earlier regression retained

Releasing a seated companion from Fabric's entity-unload callback could teleport it into another entity section while Minecraft was removing its old tracking entry. This produced `IllegalStateException: Entity is already tracked!` during shutdown and prevented a clean save close.

Unload now releases the routine with movement deferred. Bed, seat and cooking safe-exit markers remain available for the next active tick/load. Wheel release similarly records a safe exit instead of moving from the unload callback. Ordinary releases still move immediately. No data schema or dependency changes were needed.

## Limits

This is not a two-player concurrency or multiplayer-capacity certification. Two porters share one route in the integration world; the dedicated reconnect test has one real client. Large populations across dimensions, large factory loads, every machine/recipe combination, every biome, survival crafting, external furniture/diagonal-fence integrations, optional dependencies absent, and dynamic-light/shader appearance remain outside this run. The GUI tests validate data and button packets; they do not automate every mouse interaction or every GUI scale. Carried porter serialization is checked in memory; a disk restart during an active haul remains a manual case. Existing Blockbench exports were not regenerated.
