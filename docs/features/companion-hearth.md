# Companion Hearth Oven automation

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Owner-requested on 7 October 2026, implemented by OpenAI Codex (GPT-6) on `peepo-companion` from `bb2974a0`. Extends the existing planner, ghost recipe slots and [Supply/Output transport](companion-jobs.md#supply-and-output). No new recipes, dependency, assets or progression gates.

The current GUI uses the [nine-output Filter editor](companion-kitchen-filters.md): the Work row's F button opens a 3×3 grid, with tooltip **Filter**. Multiple outputs are allowed; an empty grid retains Auto. The earlier single-slot instructions below are superseded by that editor.

## Setup

1. Assign a Hearth Oven with the Companion Planner, plus separate Supply and Output containers. Leave a reachable standing space in front of the opening, with its floor at the oven's base level, and one free companion cargo slot. An elevated oven needs a reachable platform in front.
2. Shift-right-click Peepo/Jughead. Leave the oven's ghost slot blank for **Auto**, or click it with a whole raw or baked pie/cake to restrict supplies to that bake. The cursor item stays yours. A selected slot displays the baked result and its raw-bake/fuel requirements. All registered `PieFilling` pies and cakes are supported; slices and unrelated items are rejected.
3. Supply **prepared raw pies/cakes** (matching the chosen recipe when set) and fuel: tagged hearth logs, charcoal, coal or coke. The oven does not craft raw pies/cakes from pastry, batter, fruit or sugar. Coal blocks cannot fit its 3,200-tick fuel bank and are excluded.
4. Peepo checks Output has room before bringing a new pie. It loads one pie and supplies fuel when needed. Fuel is requested only for a real unfinished pie, targeting at least 600 banked burn ticks, rounded up to whole fuel items and limited by the oven's capacity. An empty oven is never continually refueled.
5. The companion carries the oven's unbaked dough/tin model for raw cargo, and the matching 3D placeable model for baked cargo, in both hands; takes one second to slide a raw pie into the opening, then stays facing the oven with a quiet waiting pose while it can heat/bake. When baked, it takes the pie directly into reserved physical cargo, plays a one-second pull-out motion and carries it to Output. A full chest after loading does not leave the finished pie cooking: it remains safely in cargo while delivery retries.

Right-click/empty-cursor clearing of the ghost slot restores Auto. Auto chooses the first eligible prepared raw pie/cake encountered in the existing bounded Supply scan that has room for its baked result in Output. A bake already inside can still receive fuel, finish and be collected. Use the job's Supply Off control to stop bringing new ingredients instead. Changing the plan does not replace or reset the current pie. All companions sharing the oven see its saved selection. Manual insertion, fuel and collection continue to work.

## Timing, interruptions and energy

The oven retains its original heat, fuel and burn rules. It holds **one pie or cake total**, not an input stack plus a separate output slot. The raw pie, baking pie and baked/burnt result occupy that same space, and another raw pie cannot enter until it is emptied. Additional prepared pies wait in the Supply container. Fuel is stored separately as up to 3,200 burn ticks (160 seconds at 20 TPS), rather than an input item stack.

It heats to 100, starts baking at 50, finishes at 600 baking points and burns at 1,200. At sustained maximum heat, baking takes 15 seconds and another 15 seconds burns it, at 20 TPS; a cold oven needs additional warm-up time. No speed bonus, free heat, remote collection or automatic cooling tray is added.

An active hot/finished pie is considered before ordinary workstation-priority transport, among at most four assigned workstations. Pending carried deliveries remain physical and finish/return first. Only one helper claims tending at a time; its UUID lease is refreshed while traveling/watching and expires after 100 ticks if abandoned. A new raw-pie/fuel delivery transitions directly to tending when heat is available, retaining the reachable standing position. An unheated pie releases tending so fuel can be fetched. Matching task priority applies again after the pie is collected.

An empty cargo slot is reserved while watching, excluding it from meal stocking/container returns. Collection rechecks the actual inventory in case the player fills that slot. Collection and cargo insertion commit in one Fabric transaction, with up to **16 JE** charged per pie collected; ordinary transport costs up to **2 JE per moving tick**. Waiting has no continuous energy charge and uses the normal passive/food recovery rules. Normal meals, shift changes, exhaustion, orders and access changes still interrupt the task.

Burning remains possible if the helper is removed, goes off shift, cannot reach the oven, loses access or has its cargo filled by the player. A resumed helper can collect a burnt pie as the real burnt result; it never converts it back. Keep storage close and avoid starting several manually loaded ovens with one helper if collection deadlines overlap. This implementation does not promise uninterrupted production while a worker is unavailable.

## Multiplayer, storage and saves

The companion-only input/output ports expose selected raw pies and valid fuel as inputs, and baked/burnt pies as outputs. Raw pies are never extracted as ordinary results. Output capacity probing rolls back; arrival rechecks recipe, need, target identity, loaded state, assignment and current storage permissions. The oven's burn/pie/progress and cargo changes are transactional. There is no public hopper/pipe registration or fake player.

Tending adds a bounded pass over the existing four work links within the existing search admission. It reuses transport path budgets, loaded-only checks, 64-block leg limit and travel deadlines. Waiting checks route validity every ten ticks and revalidates immediately before collection, without reopening external storage every waiting tick. At most one output view is inspected while tending. There is no extra oven ticker, global worker scan, chunk ticket, packet type or spawned helper entity. Arrival gives an 800-tick tending deadline; a stalled task releases/backoffs instead of holding indefinitely.

The optional `companionPie` field saves the filling's stable string id. Existing worlds with no saved selection default to automatic selection from available prepared raw pies/cakes and retain their old fire, heat, pie and baking counters. The transient tending lease is not saved; the actual oven pie and companion cargo retain their existing saved storage and are rediscovered/resumed after reload. The existing transport manifest remains the only delivery record and never recreates items. Menu size and slot indices are unchanged; its recipe flag now distinguishes Cooking Pot and Hearth Oven tooltips. Update both client and server.

### Pie handling animation (8 October 2026)

Owner-requested extension by OpenAI Codex (GPT-6), based on `59961ba6`. Four appended work actions cover loading, waiting, taking and carrying on the existing rig. Front-only approach reuses the current loaded-target/path budget; a short final step positions the companion closer to the mouth. Blocked fronts back off normally rather than loading through the back wall. Both variants and the pumpkin costume share the poses. Walking retains leg motion while both hands carry the pie. Equipped tools/lights are visually stowed during these work poses; their inventory contents are unchanged.

Loading leaves the raw pie in actual reserved cargo until the end of the one-second motion, then rechecks current recipe, need, permissions and destination before the normal atomic transfer. If interrupted before insertion, the cargo remains available to resume or return. Collection commits the real baked/burnt pie into cargo before starting the pull-out motion, preventing that cosmetic second from burning an otherwise ready pie. Delivery and interruption use the existing manifest; the display stack is never an inventory, saved item, dropped entity, reward or transfer source. Stopping a task clears its pose, and reload reconstructs the carry display from real cargo when transport resumes. A player-removed stack cancels the task normally.

Baked and burnt cargo reuse their existing full block models through item-model definitions at companion scale. Raw cargo shares `HearthOvenRenderer.submitBake` with the oven, using zero bake progress, the same texture/tints and the same geometry size. Raw pies retain pale dough and filling vents; raw cakes retain their pale batter and tins. The shared helper leaves the oven's baking visualization and browning unchanged. Raw geometry is centered between the hands and slides to its exact oven-space location. Cake item models use a higher center to account for their taller shape. The owner library/catalog were inspected (including `originals/Blocks/farming and food textures/apple_pie.png`); this change instead reuses the requested existing runtime 3D pies and adds procedural poses without copying new textures, sounds or models. Library originals remain unchanged. The already-approved Fabric transfer APIs and existing vanilla menu/goal/rendering infrastructure are retained.

One transient display stack and action start time join the existing synchronized work action/target. Only changed state is sent; motion is evaluated client-side. There is no new global scan, ticker, chunk ticket, saved registry ID, recipe or energy cost. Update server and clients together. No production throughput or oven-capacity change is made beyond the one-second loading/removal motions.

## Validation

### Cake compatibility assessment after fork sync

On 8 October 2026 the owner requested syncing fork `origin/main` at `b533201f` and assessing cake support, without tests. The incoming `PieFilling` appends Carrot Cake, Birthday Cake, Ice Cream Cake, Red Velvet Cake, Cheesecake, Coffee Cake and Apple Cake. The current ghost selector, recipe display, raw-input matching, fuel demand, tending and normal baked output already iterate that shared type, so the main workflow can be reused. Supply must contain prepared raw cakes; the companion does not craft Cake Batter or assemble raw cakes from ingredients. Oven capacity remains one bake at a time, shared by pies and cakes.

At the time this was an assessment. The follow-up below implements these code changes; the remaining visual validation is still outstanding. The assessment identified: change the companion output port's hard-coded `burnt_pie` to the filling's `burnt()` result; recognize `burnt_cake` in the carry display; change pie-only GUI wording; visually check/adjust the taller cake models in the two-handed loading/carrying poses. The existing raw-to-placeable model lookup already maps raw cakes to their baked cake model. No new job type, workstation assignment, inventory or server search is needed. The merged source was compiled without running tests; the earlier results below predate this fork update and do not validate cakes. Assessment and merge: OpenAI Codex (GPT-6).

### Automatic selection and raw cargo follow-up (8 October 2026)

Owner requested blank recipes as Auto **only for Hearth Ovens**. The other workstation selectors are unchanged. Blank selection admits any supported prepared raw bake, subject to the existing one-bake capacity, transactional Output-space probe and bounded 128-view Supply scan. The stable automatic plan ID keeps an in-flight delivery valid; explicit recipe changes still revalidate/return cargo normally. Empty ovens report missing input rather than missing recipe. No per-tick catalog/storage search, new job, packet, saved field or dependency is added.

The companion output now uses `PieFilling.burnt()` and recognizes burnt cakes for carrying. GUI wording includes cakes. Raw deliveries share the oven's renderer instead of displaying baked models. No new art files were created; the owner library/catalog were inspected and the existing runtime `pie_crust.png` texture and procedural oven geometry are reused. Implementation: OpenAI Codex (GPT-6).

The existing all-fillings regression expectation was updated to use each filling's burnt result (including burnt cake), but was not executed. Validation for this follow-up: assembly/compilation only; automated tests and in-game visual checks were not run at the owner's request. Earlier test results below predate Auto mode and cake support.

### Earlier hearth-only regression results

**8 October 2026: 10 groups passed, 115 assertions, zero failed groups.** The final hearth-only run completed successfully in 5 minutes 13 seconds on Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3 with the normal pinned development integrations. Gradle selected `PeepoHearthClientTests` alone (1 of 99 client test classes). Shared test sources compile together; no unrelated suites ran. Local evidence: `build/hearth-focused-tests-fixed.log`; the earlier failing run is `build/hearth-focused-tests.log` (generated logs are not committed).

The initial animation implementation was compiled/assembled and packaged without automated or in-game tests, following the owner's instruction at that time. On 8 October 2026 the owner requested hearth-only tests after a companion spawn crash, superseding that restriction for this scope.

The launcher log showed `IllegalStateException: Tried to access entity ID before ID assignment` from `CompanionSocial.<init>` while handling `ClientboundAddEntityPacket`. Initial social scan staggering now uses the available UUID instead of the not-yet-assigned client numeric ID. A first scoped run passed nine groups but timed out delivering from a west-facing oven. The Hearth Oven inherited the default passability of a non-full collision shape, allowing paths through its brick shell. It now declares itself non-passable, as the Cooking Pot already does. The fix changes navigation classification, not physical shape, saved data or baking rules.

The new `PeepoHearthClientTests` entrypoint selects only Hearth Oven tests and their companion spawn/render prerequisite. It reuses the existing hearth-specific checks without executing the other companion or mod suites. Run locally with:

```powershell
.\build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoHearthClientTests')
```

Coverage includes direct client construction of Peepo/Jughead/legacy Jughead before numeric ID assignment; real spawn packets; fuel/input/output transactions; tender exclusivity; a cold-oven cycle fetching pie and logs; all four oven facings with both rigs and costume variants; client-synchronized load/wait/take/carry actions and resolved 3D item models; interruption before insertion and resumption; full Output rescue; in-memory save/load of real cargo during delivery; all five fillings, one-pie capacity and burnt extraction. Tests use a fresh disposable flat world and do not open player saves. Implementation and investigation: OpenAI Codex (GPT-6), base `7d77cc7f` plus the fixes and tests documented here.

Limits: a single integrated client/server, not two-client concurrency or server-load certification. All face/pose combinations resolve and run, but only a waiting-scene screenshot was visually inspected; it is not complete visual approval. Elevated/blocked fronts, external permission changes, player interference during every transfer, full disk restart mid-delivery, pack import and optional integrations absent remain untested by this focused run.
