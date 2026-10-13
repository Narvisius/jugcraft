# Companion Planner and explicit assignments

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Implemented locally with OpenAI Codex (GPT-6), at the owner's request, on branch `peepo-companion` (renamed from `codex/peepo-wheel`). This is one home **plus four workstations**, as clarified by the owner.

## Use

Craft the **Companion Planner** from an iron nugget above three paper, with a stick below the paper. It is also in Tools & Utilities; ID `peepo_companion:companion_planner`.

1. Feed a companion once to tame it. Right-click it with the planner to select it; its name appears on the tool.
2. Right-click a companion bed/bunk or either half of a vanilla bed to assign home. A new home replaces the previous home.
3. Right-click up to four Generator Wheels, Hand Cranks, Cider Presses, Jugcraft machines, cooking pots, or farmland plots to assign work. [Garden assignments](companion-gardens.md) each save up to eight connected soil blocks (four plots / 32 blocks maximum). Any part of a supported multi-block machine resolves to its controller. Assigning work switches the command to Work.
4. Hold the selected planner to see green frames around that companion and its home/work/lunch assignments, blue for Supply, and yellow for Output. A missing/replaced target is orange. Only loaded targets in the current dimension are drawn.
5. Left-click an assigned block with the planner to remove its assignment without breaking it. Shift-right-click air to clear the selection. Selecting another companion replaces the selection on that tool only.
6. Shift-right-click a companion normally to open its inventory/commands. Home, four work rows and separate Lunch, Supply and Output rows show names and coordinates. Hover for full name, dimension and server status. The x buttons also remove assignments, including targets that have been destroyed, unloaded, or left in another dimension.
7. Use the up/down arrows beside work rows to set priority. Work 1 at the top is highest; home stays fixed. The order is saved with the companion. Removing a workstation closes the gap; new assignments go at the bottom. Existing saves with gaps are compacted while preserving their relative order.
8. Right-click a lunch crate or lunch cover with the planner to bind the separate food source. Use the Routine tab for work shifts, recovery thresholds, food preferences, carried meals and optional alerts. See [jobs and routines](companion-jobs.md).
9. Right-click ordinary containers to fill Supply then Output. Click an assigned container again to switch its role; an existing pair swaps after validating both directions. Select **Porter** in the companion GUI for direct Supply-to-Output deliveries. See [general porter setup](companion-general-porter.md).

Assignments must be within 64 blocks of the selected companion when added. The companion must remain loaded, within 128 blocks of the player, and in the same dimension when editing with the tool. There is no remote teleport or chunk loading.

## Actual work and rest behavior

Generator Wheels are functional jobs: once the planner is used, Work mode uses assigned wheels only. Full, occupied, invalid and temporarily unreachable wheels are skipped, allowing other assigned wheels to be considered. Exhausted companions still rest and recover through the existing energy routine. Nearby seats and lunch sources retain the existing behavior; food is allowed near the assigned places within the GUI's range.

[Cooking Pots now support assistance](companion-cooking.md): one helper gives +50% cooking speed, with a saved ghost recipe selector in the companion GUI used by Supply transport. [Processor helper teams](companion-jobs.md#processor-assistance-and-helper-teams) now support all 36 item/fluid/extraction/farming processors. Full multiblocks take two helpers at +25% each; single-block/compact machines take one at +50%. Power generators and storage remain Unsupported job.

Work selection uses row priority before distance. Reordering in Work mode prompts a new selection without interrupting exhaustion recovery or changing the selected command. While working a lower-priority wheel, the companion checks higher-priority links every 80-99 ticks, with at most two path attempts and the existing unreachable cooldown. It switches only after a usable higher-priority wheel can be reached and claimed; otherwise it keeps its current job. Full, occupied, unloaded, missing and unimplemented jobs do not block lower priorities. Reordering uses the existing server-validated menu buttons and assignment snapshot; it adds no per-tick world scan or separate save format.

The selected home limits bed choice to that bed/bunk. Vanilla beds have an adapter that shares the existing seat reservations, uses a safe ground approach, refuses beds occupied by players, and sleeps only at night. Companions keep their existing closed-eye sleeping pose and wake/dismount behavior. Bunks retain their bottom entrance.

## Authority, persistence and cost

- Every change is server-authoritative and checks current owner/party permissions, player reach, loaded chunks, dimension, level interaction permission and the built-in town protection. The planner never breaks blocks. Edits have a five-tick cooldown.
- The tool stores the selected UUID/dimension, display name and a refreshable entity ID. Those values never grant permission. Only the held tool refreshes its entity ID/name once per second, without an entity/world scan.
- A fixed eight-entry target array persists one home, four jobs, one lunch source, Supply and Output with dimension, position and block identity. Duplicate links and a fifth workstation are rejected. Missing targets stay visible/removable instead of silently being reassigned.
- Existing saves retain old area orders until the planner is first used. Explicit targets then take precedence; removing all workstation links does not resume random wheel assignment.
- A small entity-data snapshot changes only when assignments change/load. Clients cache its parsed form. The menu reads the tracked companion; preview rendering touches at most eight assigned targets, never scans nearby machines. Multi-block frame bounds use at most 256 footprint cells.
- Station searches retain their staggered 80-99 tick interval, at most two path attempts per search, and unreachable-target cooldown. Explicit navigation is bounded to 64 blocks and only uses loaded chunks. No server lighting/outline entities or ticking tool block entities were added.

## Progression and assets

Discovery/workshop convenience: paper comes from crops, stick from wood, nugget from iron. The output configures tamed companions and their homes/workplaces without changing machine recipes, energy currencies or research unlocks. No tool durability or per-assignment resource cost.

The owner library/catalog was inspected for a suitable clipboard/planner; none matched. The original 16x16 clipboard icon is generated by `tools/generate_companion_planner.py`, with a wood-colored outline, paper center and green clip. No library originals were modified and no third-party art/code was imported.

## Validation and manual checks

Compilation/packaging uses `build-local.ps1 -Tasks assemble`. Automated tests and game launches are intentionally not run, following the owner's standing instruction. Runtime verification is pending.

Manual cases: select Peepo and Jughead; assign one home and four wheels; reject duplicate/fifth; click every wheel part and both vanilla-bed halves; check another owner's denial and allowed/revoked party access; clear assignments through tool/GUI; change dimensions; unload/reload and restart; destroy assigned blocks; check full/occupied/unreachable wheel fallback; test night sleeping in vanilla beds and top bunks; hold planner to inspect frames; check long names/coordinates in the GUI; confirm left-click never mines in survival/creative.

## Possible follow-ups

- Optional round-robin work as an alternative to the implemented priority order.
- Live status, day/night schedules, configurable recovery thresholds, assigned lunch and carried meals are implemented in [jobs and routines](companion-jobs.md).
- Bulk assignment for several selected companions, with a preview and per-station worker limits.

Supply/Output extension (7 October 2026), revised by the owner's role-cycling request: right-click containers to assign and switch Supply/Output. These are separate links, not work slots. Clicked-face automation rules and double-chest locks apply. Work mode uses Cooking Pot ghost recipes and the implemented machine/kitchen logistics; Porter mode performs direct container-to-container delivery. Cider Press loading, pressing, bottling and collection are automated. Hand Cranks accept one operator and pause for a full connected flywheel. See [jobs and limits](companion-jobs.md#hand-crank-cider-press-and-transport).
