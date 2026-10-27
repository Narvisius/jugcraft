# Companion livestock management, animal care and pen gates

## Combined Animal Care zones and naming (11 October 2026)

Implemented with OpenAI Codex (GPT-6). The Planner's shift-right-click menu selects normal assignments or two-corner Animal Care selection, cancels pending corners, or clears the selected companion. Two ground corners define an inclusive rectangle of at most **16 by 16 blocks**, with at most three blocks of ground-height difference. Vertical inclusion starts above the lower ground block and ends three blocks above the higher ground block. Selection data belongs to the held Planner stack; successful creation clears the pending corners and returns to normal mode. A yellow first corner and green/red prospective bounds preview the selection. Completed zones show their actual bounds. The first corner remains the removal anchor.

Each zone occupies one existing work slot. Shearing, Milking and Eggs start enabled and can be toggled independently in the companion menu's **Names / Zone** panel. Ready child tasks rotate after each action; missing equipment or unavailable animals in one task do not prevent another ready task. All-off zones idle. Existing livestock animations, animal/item claims, gate behavior, milk cooldown, shearing durability, atomic inventory/provenance and action costs are reused: 240 reserve JE per shear/milk action, 80 JE per egg pickup, plus transport. Tools Supply fetches shears only if enabled shearing has a ready sheep; bucket Supply is disabled when milking is off. One zone shares its job's existing Supply/Output switches and container links. Legacy radius jobs remain readable and usable without conversion.

The wrapper holds at most three child jobs per assigned zone. Each child keeps the existing bounded, backoff-based searches and server-wide search/path admission; no global animal scanner or chunk loading was added. Eligibility checks the exact zone and permission at the target. Corners, task masks and job names are serialized with assignments; transport snapshots include zone data. Job names stay separate from target identity and follow priority moves/removal compaction. Toggling tasks cancels the current action through the existing lifecycle.

The same panel renames the companion through its vanilla custom name, or any of its four jobs. Empty text restores the default. Plain text is limited to 48 characters and control/format characters are removed. Edits require the matching open menu, current job anchor, owner/party permission, reach and server rate checks. Stale rows and forged menu IDs are rejected.

Supplies and Outputs display nonempty text from signs attached to their containers, including either half of a double chest. Standard wall signs, standing signs on top, and ceiling hanging signs below are recognized by attachment direction. Front text takes precedence over back text; nonempty lines are joined. Sign text overrides the container's custom/default name. At most eight assigned containers are checked once per 20 ticks while a valid companion menu is open; unchanged labels are not resent. This is a display label, not an assignment identity or inventory change.

At this stage, breeding/slaughter were not yet implemented; the later Livestock Management extension below supersedes that limitation.

Validation: working tree based on `66dd5f28`, Windows, Java 25.0.4.1, Minecraft 26.3/Fabric Loader 0.19.5, newly generated flat test worlds. `assemble runClientGameTest -PclientTests=PeepoHarvestClientTests` passed **231 assertions** (`build/animal-care-verified.log`), including 25 new checks for corner limits, one-slot creation, exact boundaries, name/zone persistence and reordering, actual menu naming controls, stale edit rejection, toggles, all-off behavior, tools/bucket supply and all three product deliveries, sign removal and the other half of a double chest. The existing `PeepoSuppliesClientTests` class passed **34 assertions** in the preceding targeted two-class run (`build/animal-care-final-tests.log`); that run subsequently failed a double-chest test fixture whose two halves were incorrectly paired. The corrected fixture passed in the final harvest run. An actual Planner screen capture also caught and fixed duplicate background blur before the final passing run. Both final Planner/settings screenshots were visually inspected. Repository/document-link checks and all five client-test-selection checks passed. No unrelated mod suites, dedicated multi-client session, multiplayer load benchmark or new gate-layout playtest was run for this change.

Owner-requested and implemented locally with OpenAI Codex (GPT-6), 9 October 2026. The owner selected an **8-block radius** and a per-cow cooldown. This extends the four existing work assignments, priorities, supply/output links, inventory and shared server search/path budgets.

## Using it

Select an owned Peepo or Jughead with the Companion Planner, then right-click a sheep or cow. This assigns **the ground block beneath the animal at selection time** as a fixed shearing or milking point. The animal can subsequently move away; the point does not follow it. Jobs occupy one normal work slot each. A different job cannot share the same exact anchor. The GUI shows its name, coordinates, priority and status. The planner highlights the anchor and the radius's bounding square; eligible animals must actually be within 8 blocks of the point above the anchor and at most 3 blocks vertically away. Left-click the marked ground block with the planner, or remove the row in the GUI, to remove it.

- **Shearing:** equip shears in Hand, or enable Tools on a linked Supply containing shears. The existing tool delivery swaps the previous hand item into cargo. Only adult, woolly sheep qualify. The held shears are rendered between both hands, moving across the fleece with repeated snipping sounds. Completion uses the normal `SHEAR_SHEEP` loot table and actual tool context, puts its results directly into cargo, marks the sheep sheared, and applies one normal durability use. Sheep regrow their wool through their existing behavior.
- **Milking:** give empty buckets in cargo or link Supply. Peepo fetches up to four spare empty buckets at a time, performs the ordinary two-arm interaction clip with a bucket sprite beneath the cow, and converts one real empty bucket into one milk bucket. Adult cows rest for **1,200 ticks / one minute** between companion milkings. This cooldown is stored on the cow, shared across helpers, and survives reloads and normal serialized transport. Its clock uses overworld game time across dimensions. Player milking remains unchanged; mooshroom stew and non-cow livestock are outside this job.
- **Output:** wool and milk occupy the real eight-slot inventory. Their source assignment is recorded using the existing produce ledger; Output delivers only marked products for that job. Supply/Output Auto/On/Off and container bindings continue to apply. Player-given inventory is not treated as produce. Full inventory prevents completion without consuming buckets, shearing the sheep, or dropping excess items.

An uninterrupted action takes **60 ticks / 3 seconds** and costs **4 reserve JE/t, 240 JE total**, plus existing delivery movement costs. Recipe filters are not required. Missing tools/buckets, unavailable animals, denied access, off-shift/recovering companions, water, fire, damage, babies, leashed animals and passengers prevent or interrupt work. `mobGriefing=false` disables these animal jobs and automatic gate opening. Work is interrupted by settings, commands, eating and unloading through the ordinary routine lifecycle.

## Holding animals and using gates

Each animal has a short, nonpersistent UUID work claim. Competing helpers cannot claim it simultaneously. The animal moves normally while the worker approaches; during the actual action it receives a temporary movement/look/jump goal. This stops walking without changing saved `NoAI`. The goal is removed on release; an expired claim, damage, water or fire also makes it stop holding the animal. Physical damage and normal gravity still apply.

Companions use the existing ground navigation with a narrow closed-gate exception. It preserves the shared diagonal-fence checks and does not let them walk through fence rails. They open only unpowered, permitted fence gates directly on their current path, leaving already-open/player-controlled gates alone. During the brief passage, an open gate retains its closed collision barrier **for animals** while allowing Peepo through. This prevents following livestock from using the helper's opening. Gates close on a four-tick check after the operator moves away, abandons its path or disappears, with a 40-tick maximum hold. Player interaction takes control back; powered gates are left to redstone.

The gate's `jugcraft_companion_passage` state and scheduled block tick persist, so reloads do not leave a companion-opened gate permanently open. Old states default to false. This adds one boolean property to fence gates and two narrow mixins: gate state/collision/cleanup, and access to vanilla shearing loot collection. The equipped shears and bucket are render-only props, not spawned item entities. Gates opened by players/redstone and other holes in a pen remain the player's responsibility.

## Server behavior and compatibility

Only explicit assignment anchors are searched. Searches use the server-wide admission budget, a loaded 8-block neighborhood, at most 24 eligible candidates, and an 80-99-tick idle backoff. Standing points are bounded to four candidates; moving targets refresh their approach at most every ten ticks. Active work resolves one known animal UUID and revalidates eligibility, ownership and range. A completed animal hands off through the existing short work session to the next animal without a random stroll.

Gate checks inspect at most three imminent path nodes every four ticks per moving companion. There is no global gate/world scan or new global ticker; only open gates schedule cleanup, with at most 128 live gate records per level. Expired records cannot force chunks to load. Animal claims and milk cooldowns use the already installed Fabric attachment API. New animation and status IDs are appended, and the target entity ID synchronizes only when it changes. Client and server need the same updated build.

Assignment identities reuse saved targets with explicit shearing/milking IDs, preserving old saves and transport manifests without a new format. Animal produce reuses the existing transactional inventory/provenance snapshots and saved ledger. Sheep loot is collected before committing inventory; no fake player, duplicated item, or new inventory is introduced.

## Assets and validation

The owner library and complete catalog were inspected for shearing/milking assets. Existing equipped-item models, vanilla runtime sounds and the original procedural companion rig are used; no library files, new textures, or dependencies were imported. Milk uses the requested ordinary interaction clip; shearing shares the rig's existing two-hand reach solver.

Common and client compilation and assembly succeeded with `build-local.ps1 -Tasks @('compileJava','compileClientJava','assemble')`. The local Modrinth pack was rebuilt with `scripts/package_modrinth.py build`, including its 18-library lock validation. No automated tests or gameplay runs were performed, per the owner's continuing instruction. Pending manual checks: both variants/costumes, wool colors and regrowth, tool breaking, bucket conservation and full cargo, two helpers sharing a pen, damaged/moving animals, closed and diagonal pen layouts, player/redstone gate takeover, interruptions, reload with an open gate, and multiplayer load.

## Egg collection (11 October 2026)

Owner-requested addition implemented with OpenAI Codex (GPT-6). Right-clicking a chicken with the selected companion's Planner creates `peepo_companion:egg_collection_job` at the ground beneath the chicken. It consumes one of the same four work slots, preserves priorities, saves/synchronizes with the existing target format, and uses the animal-area outline and removal controls. The area remains fixed after the chicken moves or disappears.

The job collects real loaded dropped items in `minecraft:eggs` (including white, brown and blue eggs), within the existing eight-block sphere and three-block vertical limit. It does not alter chicken AI or laying frequency, generate eggs, collect unrelated drops or require tools/Supply. Collected eggs feed the existing cooking/crafting chains through linked Output and the shared eight-slot cargo/provenance ledger. Player-given cargo is never relabeled as output. Each pickup action takes 20 ticks at 4 reserve JE/t (80 JE), plus normal delivery movement costs. Partial capacity collects only what fits and leaves the remainder as the same ground stack; full cargo preserves every item.

Searches use the shared admission budget, 80-99-tick idle backoff and at most 24 eligible candidates from loaded nearby entity sections. Each candidate has at most five standing positions. Once selected, only its UUID is resolved; moving drops refresh their approach at most every ten ticks. A transient Fabric attachment claims the item for one helper with a 100-tick expiry, renewed by the existing routine. Pickup rechecks assignment, claim, gamerule, protected positions, delay, ground/water/fire state, radius, reach and line of sight. Capacity probes roll back the existing inventory and provenance transaction snapshots; only the committed count is removed from the ground item on the server thread. No chunk loading, global scanner, additional inventory or animation assets were added. Existing general interaction, pickup sound and pen-gate behavior are reused.

Focused checks are included in `PeepoHarvestClientTests` through `CompanionEggCollectionChecks`: chicken assignment/reload, fixed anchor, no Supply demand, radius/delay/type exclusions, competing helper claims, full/partial cargo, real navigation and Output for all three egg colors, personal-cargo preservation and removal. Common/client compilation passed. The focused client run passed **206 assertions**, including 21 new egg checks and all 185 existing harvest checks (`build/egg-collection-tests.log`, Windows, Java 25.0.4.1, Minecraft 26.3/Fabric Loader 0.19.5). An initial automatic-trip timeout exposed the shared routine's arrival tolerance exceeding pickup reach; the job now finishes the short final approach before collecting, and the rerun passed. All five client-test-selection checks also passed. No unrelated mod suites, multiplayer load or dedicated pen-gate playtests were run for this addition.

## Livestock Management and selectable species (11 October 2026)

Implemented locally with **OpenAI Codex (GPT-6)**. The combined zone is now displayed as Livestock Management, with independent Shearing, Milking, Eggs, Breeding and Slaughter switches. Saved `animal_care_job` identities remain unchanged. New and old zones default to the original mask 7; breeding/slaughter are opt-in. Per-zone population rules save and synchronize registry IDs, total breeding targets and adult retention counts. No species is selected automatically. The existing Names / Zone page opens Population settings: discover animals in the loaded zone, cycle types, add/remove, and adjust counts. Up to eight explicit types, each 0Ã¢â‚¬â€œ64 with initial target 10/keep 6. Menu edits validate owner/party access, reach, open menu, anchor, row and rate limits; discovery uses the shared search budget and bounded payloads.

Discovery accepts compatible `Animal` entities, including modded types, and excludes hostile entities. Breeding matches a same-type adult pair with food both animals accept through `isFood`, consumes two real cargo items and calls normal `setInLove`. Vanilla mating, offspring creation and cooldowns remain authoritative. Tamed/owned animals are excluded from breeding as well as slaughter. Special breeding systems that do not use those hooks need adapters. Shearing/milking retain their existing sheep/cow compatibility; adding an arbitrary species does not create a new wool or milk interaction. Only explicitly configured types are bred/slaughtered. Breeding food container remainders enter cargo/Output in the same transaction; lack of room aborts feeding without consuming food. Animal-zone supply and tool trips release/equip carried items at collection, then approach the animal rather than demanding a walk to the arbitrary ground corner. Supply demand is independent of the temporary work-goal scan cooldown. Its eligible species groups are classified at most once per tick rather than once per inventory view; live cargo counts still participate correctly in rolled-back insertion probes.

The wrapper rotates five enabled task children and, only after actual loot overflow, a bounded cleanup child. Idle planning shares an 80Ã¢â‚¬â€œ99 tick census of at most 256 animals per zone; population actions also acquire a fresh census through existing server-wide budgets. Fully loaded zone chunks are required without force-loading. Crowded censuses pause population changes. Existing per-animal expiring claims prevent simultaneous care, feeding or attacks. Pending pairs/in-love animals count toward future births; reserved kills count against surplus adults. Before feeding or each attack, current counts and output backlog are rechecked. This coordinates helpers with consistent policies, rather than enforcing a universal hard population cap against players or external breeding. No world ticker or per-player/global animal scanner was introduced.

Breeding takes two 60-tick stages at 4 reserve JE/t (480 JE per pair), with temporary animal hold goals released for normal mating. Slaughter requires a sword/axe, three free unreserved cargo slots, accessible linked Output, and no known marked-product backlog. Babies, named animals, tamed/owned animals, leashed entities, passengers/vehicles, invulnerable animals, in-love animals and parents on cooldown are excluded. Protection and mob-griefing checks apply. Each 40-tick attack costs 6 JE/t (240 JE), invokes normal mob damage with the equipped weapon and wears the real hand item; several attacks may be needed. It reuses the general interaction clip. Slaughter work counts as tool use; equipped weapons are stowed during unrelated actions.

Normal synchronous death drops are captured as actual ItemEntities in a narrow attack scope, transactionally moved into cargo and tagged with the existing job provenance for Output. Overflow stays on the ground with persistent worker/zone provenance and is later collected by the existing bounded pickup job. Marked drops do not merge with unrelated ground stacks. No replacement loot rolls, instant entity removal, fake player or extra inventory is used. Custom delayed/external loot emissions outside the synchronous attack scope remain ordinary ground drops. The capture is bounded to 64 immediate item entities; further marked items stay on the ground. The overflow flag persists to permit recovery after reload.

The new population test has an explicit 120-second scheduling estimate. With 130 registered client suites, perfect four-way division now exceeds the previous 25-minute estimate ceiling; the selector test retains the 60-minute CI timeout headroom with a 30-minute estimated ceiling and adds a maximum imbalance of 30 seconds above the ideal mean. It still checks complete, duplicate-free test assignment. No assets or dependencies were added.

Validation: working tree based on `66dd5f28`, Windows, Java 25.0.4.1, Minecraft 26.3/Fabric Loader 0.19.5, new flat test worlds. Final `assemble runClientGameTest -PclientTests=PeepoPopulationClientTests` passed **25 population assertions** (`build/population-final.log`). The existing **231 harvest/animal-care assertions** passed in `build/livestock-verified.log`, and **34 supply assertions** passed in `build/livestock-final-tests.log` before the final slaughter-only readiness correction. Five client-test-selector checks and repository/document-link checks passed. Planner, five-switch zone and Population screenshots were visually inspected at the companion screen's supported test-window scale.

Population coverage includes safe new/old-save defaults, count bounds, concurrent pair/cull reservations, full cargo, real preserved overflow stacks, food demand across work release, actual cow and modded-turkey breeding with food conservation and normal cooldowns, automatic weapon fetching/wear, real death-loot Output, retained adult counts, named/baby/tamed/unselected protections, rule persistence, modded-type discovery/addition through the open menu, synchronized count editing and forged-menu rejection. The final regression deliberately starts a short weapon-fetch trip with only three free cargo slots, and verifies that personal cargo survives the tool swap and loot delivery.

Early runtime checks caught and corrected the unnecessary trip to the zone corner, the final approach/reach ordering, and weapon supply readiness depending on a work-goal scan cooldown. Tool readiness now uses the bounded census separately from that goal, classified at most once per tick and by at most eight species rules. The three-slot loot requirement applies to actual slaughter, after the temporary cargo-held weapon is equipped. GUI tests now wait for companion data/panel synchronization before clicking. No unrelated mod suites, multiplayer load benchmark, dedicated multi-client session or new gate-layout playtest was run.

## Fractional animal approach correction (18 October 2026)

PR #420's client CI timed out while Peepo carried four seeds and remained
`TRAVELLING` toward turkeys. Vanilla navigation accepts a waypoint near the
center of its block; an animal's standing point can be closer to a block edge.
A completed block path can therefore leave Peepo just outside the job's
0.8-block arrival distance. Rebuilding that same completed path does not close
the gap.

A fixed-position regression reproduced this before the movement change:
`navigationDone=true`, four seeds untouched, both parents still adults, and
Peepo approximately 0.8003 blocks from the approach. The existing free-roaming
test also passed on a separate diagnostic run, confirming that a single rerun
could hide the issue.

The shared work routine now finishes a completed path with a short level step
to the actual approach position. It requires grounded movement within 1.5 blocks,
a clear swept collision box, loaded/permitted positions, solid footing, and no
harmful blocks along the segment. Checks use at most eight short segments and
perform no new path search or chunk loading. Existing arrival/feeding distances,
claims, costs and breeding behavior remain unchanged.

The population regression holds its two parents still only until feeding, then
restores their normal AI to require a real offspring. The original free-roaming
cow/turkey and slaughter scenarios remain. Additional assertions reject walls,
closed gates, missing footing, magma and long or vertical shortcuts. Timeout
diagnostics now include the actual species, approach point and navigation state
instead of showing cows only. Implemented with OpenAI Codex (GPT-6).

Focused validation on Minecraft 26.3 / Java 25 / Fabric Loader 0.19.5:

- The new fixed-position case failed before the movement correction, with a
  completed path and no feeding (`build/peepo-population-regression-before.log`).
- `runClientGameTest -PclientTests=PeepoPopulationClientTests,PeepoHarvestClientTests,PeepoSuppliesClientTests`
  compiled the change and passed **46 supply** and **231 harvest/animal-care**
  assertions. The new population fixture then needed its grounded state set
  explicitly after placing the synthetic pose; its airborne rejection is now
  checked too (`build/peepo-population-fix-relevant.log`).
- After that fixture-only correction, only `PeepoPopulationClientTests` was
  rerun: **36 assertions passed**, including normal calf/poult births, precise
  arrival, food conservation, safety barriers and slaughter/Output
  (`build/peepo-population-fix-final.log`). Production movement code was unchanged
  between these two validation runs.
- Repository/document links and whitespace checks passed. No full local mod,
  server, Python or client-suite rerun, new preview world, or next tree batch.

## Breeding test enclosure correction (23 October 2026)

Implemented with OpenAI Codex (GPT-6). PR #452 at `c5a4a9393` passed
Foundation, both server build configurations and 15 of 16 client shards.
The remaining shard timed out in the roaming turkey breeding scenario:
the parents were about 9.5 blocks apart in opposite corners of its 8 by 8
test pen, outside the existing 8-block partner range, with all four seeds
still in cargo. The unchanged focused suite passed locally (36 assertions),
confirming that a successful rerun alone does not address this random layout.

The supplied cow/turkey breeding scenarios now use temporary 6 by 6 pen
walls, keeping the roaming parents within partner range. Normal movement,
Supply collection, feeding, mating, offspring, food conservation and cooldown
checks remain active with the same timeouts. The original enclosure is restored
before slaughter and Output checks, preserving the Supply chest and Output
barrel. A separate fixed-position case recreates the distant CI pair and checks
that an admitted population search rejects it without feeding or consuming
seeds. No production behavior, breeding range, population limits or CI gates
changed.

Validation: Windows, Java 25, Minecraft 26.3 / Fabric Loader 0.19.5, new flat
test world. `runClientGameTest -PclientTests=PeepoPopulationClientTests` passed
**41 assertions in 2m33s** (`build/population-pr452-fix.log`), including the
new distance checks, actual calf/poult births, slaughter/Output and menu edits.
The baseline run is recorded in `build/population-pr452-before.log`.
Repository/documentation links and whitespace checks passed. No unrelated
local suites, new multiplayer playtest or production-code changes.
