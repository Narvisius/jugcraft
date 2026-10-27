# Herobrine: player coverage and seasonal scheduling

Owner-directed collaboration, 27 October 2026. Scheduling, persistence, adapter and tests: OpenAI Codex (GPT-6). Entity, renderer, placement, world clues and encounter content: the owner's Claude session on `claude/herobrine-haunting`. No deployment is authorized by this feature record.

## Player outcome

Every eligible active player gets their own opportunity to see Herobrine. Until **November 3, 2026 inclusive**, the first attempt becomes due after 5–10 active minutes and subsequent sightings after 10–20 active minutes. From **November 4 at midnight America/New_York**, first attempts take 45–90 active minutes and repeats 1–3 active hours. These are placement targets, not promises that a blocked/unsafe location or unseen apparition counts as a sighting. The encounter adapter's one-minute join/respawn grace precedes eligible active play.

Unseen players take priority, then active time overdue and oldest sighting. Each attempt reserves its player. No more than two attempts per second or 32 live reservations are permitted. Failure or unseen expiry produces a 30-active-second retry, never the successful-sighting cooldown. Reservations expire after 180 simulation seconds as a fallback for a lost callback; current entity lifetimes are much shorter. A successful shared sighting only credits the participants the server actually observed witnessing it. Menus, idle time, sleep, death and offline time do not accumulate scheduler credit. The adapter also checks live witness safety when a callback arrives.

First encounters are distant watchers. Close encounters require two recorded sightings, a calm player and a separate 45-active-minute event cooldown (three hours in quiet mode), plus a one-in-eight selection roll when otherwise eligible. Normal sightings continue during that longer cooldown. Claude's encounter adapter may apply stricter eligibility. Operator-forced encounters deliberately do not mutate natural scheduler progress.

This is atmospheric content, with no required crafting input, progression gate, loot or currency output. It connects to exploration, the seasonal event, Styx and the Cursed Mirror; these narrative connections are owned by the encounter/content implementation.

## Implementation boundary

- `herobrine/HerobrineSeason`: explicit local-date cutoff converted through `ZoneId`; no UTC day or fixed-offset/DST mistake.
- `herobrine/HerobrineSchedule`: dependency-free, server-thread-only scheduling with injected clock input and random generator. One `step` per server simulation second. UUID-based persistent progress; ephemeral reservations and eligible-player snapshots.
- `herobrine/HerobrineProgress`: versioned shared SavedData codec, stored once in the Overworld and marked dirty on changed progress. State survives reconnect/restart. There is no cross-world static progress map. No offline catch-up or repeated first-sighting reset.
- `herobrine/HerobrineScheduleDirector`: implements Claude's `creatures/herobrine/HerobrineDirector`. Installs a fresh world-bound director on `SERVER_STARTED`; resets on stop. `due`, `placed`, `failed`, `seen` and `ended` preserve the reservation/encounter identity. Wrong/stale callbacks do nothing. Outcomes and witness lists cannot grant credit on their own.
- `HerobrineEncounters` owns active/safety detection, bounded loaded-chunk searches, entity lifetime and sustained visible-witness detection. It never treats a successful spawn as proof the target saw him.

Server configuration in `config/jugcraft.properties`:

```properties
herobrine.haunting=on
herobrine.event_last_day=2026-11-03
herobrine.timezone=America/New_York
```

Date/timezone are loaded at server start. Invalid values log a warning and use the defaults. This is an explicit 2026 event window; it does not silently turn back on next October. Existing progress remains stored when haunting is disabled. Calendar transitions rescale the remaining fraction of a pending cooldown to the new pace; already-overdue players remain due.

## Full collaboration plan and acceptance work

Claude received the owner's plan and acknowledged the division of work in the existing session. Its scope includes distant watchers, footsteps, second-look relocation, tunnel eyes, shared sightings and rare close encounters; leafless groves, 2×2 tunnels, shrines, signs, small pyramids and rare explorable brick shoreline pyramids; actual clue chains, modest non-farmable rewards, Mirror/guards/Styx connections. Those are not claimed implemented by this scheduling patch.

Terrain changes must protect claims, towns and unclaimed player work, and require loaded, checked wilderness sites with revalidation and bounded incremental construction. Reversal must only touch unchanged event-owned placements. Non-destructive sightings must remain independent of terrain-site availability. No automatic live-world deployment, regeneration or server restart is included.

## Validation

Run `./gradlew herobrineScheduleTest` for deterministic calendar/DST, active-time, first/repeat pacing, fairness, reservations, visibility callbacks, shared witnesses, failure retries, restart snapshots, strong-scare limits and concurrency checks. This is also a `check` dependency.

Run the filtered `HerobrineScheduleGameTests` for real Minecraft SavedData codec round trips, dirty marking, corrupt-state rejection, cutoff behavior and independent shared-witness progress. These are not rendered two-client tests.

Current evidence and outstanding integration results are recorded below as they are actually run. Player perception/rendering and true two-client witness checks still need the completed encounter branch; a server-side look vector cannot certify exactly what an individual client rendered or consciously noticed. No live server or player profile was changed.

- Standalone Java 25 scheduler tests: **393 assertions passed**, including all 200 simulated active players receiving a first confirmed turn when placement succeeds.
- Common and client Java compilation passed in the combined branch. The initial encounter test run exposed a Java pattern-variable scope error in its recorder helper, then two fixture problems: GameTest's default barrier roof interfered with the outdoor follower search, and the tunnel queried skylight before its newly built roof had propagated. Fixed the helper and fixtures without relaxing production placement rules.
- Combined `herobrineScheduleTest runGameTest` with `-Dfabric-api.gametest.filter=jugcraft-test:herobrine*`: **BUILD SUCCESSFUL; all 19 required native tests passed**, plus **393 scheduler assertions**. This includes interrupted-glimpse and unloaded-corridor regressions from Claude's `cef635f789`.
- Narrative `hasSeen(UUID)` reads the persisted world ledger rather than a session-only introduction flag. The final combined rerun including this assertion also passed all 19 native tests and 393 scheduler assertions (**BUILD SUCCESSFUL**, 27 October 2026).
- Repository-link validation in this sparse integration checkout cannot pass because owner-library archives referenced by existing documentation are intentionally absent locally. This is not evidence of missing runtime resources; the full-checkout repository/data checks remain Claude's responsibility.

### Content follow-up review

Claude confirmed the scheduler and both follow-up commits `5a7719dd22` and `1c9b5a14eb` applied cleanly. Its subsequent content through `46ff704574` was integrated locally for compilation. Five removed vanilla ore-tag constants prevented compilation; the planner now uses the shared `c:ores` tag, covering Jugcraft ores as well as vanilla ores. The marks test also needed Minecraft 26.3's `getText(SignTextSlot.FRONT).getMessages(false).getFirst()` API instead of the removed sign getters. After these corrections, **`compileJava compileClientJava compileGametestJava` passed** (27 October 2026). This is compilation evidence, not terrain gameplay validation.

The review also identified issues outside the validated scheduler/encounter scope: a natural-block allowlist cannot establish that an unclaimed stone/log build or player-grown tree is untouched wilderness; block-state reads during planning must be preceded by loaded-chunk checks; undo must preserve player state and block-entity edits. These findings were sent to Claude, which owns the corresponding planner/undo changes. Do not treat the 19 passing tests above as validation of its subsequently added terrain mutations, pyramids, or narrative connections. Real client visuals and two-player observation also remain unverified.

### Rendered-client follow-up (27 October 2026)

OpenAI Codex added `HerobrineClientGameTests`, registered in the isolated client test entrypoint. `runClientGameTest -PclientTests=HerobrineClientGameTests --offline --no-daemon` passed in 2m21s. The test checks actual server-to-client entity synchronization, the registered renderer and eye resource, sustained-view witness credit, and server-originated removal when the player approaches. Screenshots `0000_jugcraft_herobrine_daylight.png` and `0001_jugcraft_herobrine_tunnel_eyes.png` were inspected: the classic player silhouette is visible in daylight and the white eyes remain visible in an enclosed dark tunnel. This used a disposable development world, not the owner's Modrinth profile. It does not establish two-client multiplayer behavior or validate the subsequently added pyramids/terrain marks.

### Two-client follow-up (27 October 2026)

`runClientGameTest -PclientTests=HerobrineNetworkTests -I scripts/owner-network.init.gradle --offline --no-daemon` passed in 4m12s. Two separate client JVMs connected to a loopback-only disposable dedicated test server, with separate temporary profiles/offline identities. Both received the same encounter; the initially looking-away peer received no witness credit; after turning it gained credit independently; approach removal synchronized to both clients. The peer exited successfully. Evidence is under `build/run/clientGameTest/owner-network-evidence/herobrine-a3917fc0-bac6-4c4c-a0dc-e4a604ba7de5/` (passed marker, peer log and screenshot); the primary screenshot is `jugcraft_herobrine_shared_primary`. This is automated two-client evidence on one PC, not a public-server load test or a human multiplayer playtest.
### Final combined terrain and client validation (27 October 2026)

The combined branch through Claude's `49ae6532db` plus the fixture/API corrections now passes **all 40 required Herobrine native GameTests and all 393 standalone scheduler assertions**. Command: `herobrineScheduleTest runGameTest --offline --no-daemon`, with `JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=jugcraft-test:herobrine*`. Result: BUILD SUCCESSFUL in 2m2s; log `build/reports/herobrine-coordination/terrain-verified.log`. This supersedes the earlier 19-test evidence for the integrated content. It includes terrain planning, approved wilderness restrictions, construction/recovery, full sign-edit preservation, completed-trail routing and guard timing. The final fixture changes keep grove/shrine bounds inside their arena, use the pinned sign API, and wait for roof lighting before the newcomer placement assertion.

Both rendered test classes were then run together: `runClientGameTest '-PclientTests=HerobrineClientGameTests,HerobrineNetworkTests' -I scripts/owner-network.init.gradle --offline --no-daemon`. **BUILD SUCCESSFUL in 3m29s**; log `build/reports/herobrine-coordination/client-final.log`. The first network run's peer screenshot had captured its loading screen despite passing networking assertions. The test now waits for both clients to finish loading before freezing simulation. Final screenshots were inspected: the peer sees the shared apparition in the world and white eyes remain visible in the dark tunnel. The final two-client passed marker and peer log are under `build/run/clientGameTest/owner-network-evidence/herobrine-699b4acf-95bc-4364-8a4f-7d4fb4a4de26/`; screenshots/logs are also preserved under `build/reports/herobrine-coordination/visual-final/`.

These are automated development-world checks, not a human atmosphere/audio playtest or public-server load test. The Cursed Mirror response is an API seam awaiting the actual mirror caller from separate PR #498. Completed pyramid sites are not rechecked for later player mining. No main merge, release, live server restart, or personal Modrinth profile change was performed. Test implementation and review were assisted by OpenAI Codex in coordination with the owner's Claude session.

### Full-build fixture repair (27 October 2026)

The first full main build (Actions run 40585568666) completed 3,036 native GameTests with one failure: the newcomer fixture checked its roof lighting after a fixed five ticks, before the asynchronous skylight update had settled. Both roof-dependent encounter tests now wait for the actual covered condition, bounded to 160 ticks within a 200-tick test budget. The encounter and witness assertions remain intact; production gameplay is unchanged.

Local revalidation: `herobrineScheduleTest runGameTest --offline --no-daemon`, filtered to `jugcraft-test:herobrine*`, passed all 70 required native tests and 393 scheduler assertions in 2m56s. Evidence: `build/herobrine-release/roof-fixture-tests.log`. Full GitHub validation and live deployment remain pending. Repair authored with OpenAI Codex (GPT-6).

The repair's first GitHub run passed the roof checks but exposed a separate patrol/glance race: a moving guard's body rotation could clamp its head away from Herobrine at the assertion. The glance now reserves movement for its two-second duration and stops the current navigation path, while still yielding to combat and never approaching or targeting Herobrine. The regression starts with an active patrol and checks that navigation pauses and the head turns toward the apparition. Validation of this additional repair is pending.

Local revalidation of the patrol fix passed all 70 required native Herobrine tests and 393 scheduler assertions in 2m34s (`build/herobrine-release/guard-patrol-tests-2.log`). The first new regression setup lacked grounding immediately after `snapTo`; marking the guard grounded on the already-built stone floor allows an actual navigation path before the encounter. No assertions were removed. GitHub validation is still required before merge/deployment.
