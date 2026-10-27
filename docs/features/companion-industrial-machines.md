# Companion industrial-machine assistance

Owner-requested on 12 October 2026, implemented with OpenAI Codex (GPT-6) on `peepo-companion`. Extends the existing job scheduler, processor reservations, Planner, animations and machine-based reserve effort. No new assets, dependencies, recipes, currencies or standalone worker ticker.

**22 October extension:** The original first-form scope below is historical. [Expanded industrial support](companion-expanded-industry.md) now enables all 40 current recipe-processing forms, item/tool logistics and output filters, with per-lane electricity pricing. Generator forms and the dedicated coastal pump remain excluded.

## Player outcome and progression

The steel-tier [Electrolytic Separator](industrial-electrolytic-separator.md) is the first opted-in industrial form. Select a tamed Peepo or Jughead and assign any part with the Planner. Each companion uses one of its four jobs; assign the same machine to two companions for two independent helper positions. Each contributes up to 25% speed, for a combined 50%. A third waits for a position. Helpers prefer clear front ground positions and can use exterior side/back positions if the front is obstructed, including ground one block lower. The first helper uses the existing valve clip, the second the wrench clip.

The existing water/brine feed, electricity, reserved output capacity and gas/lye pipes remain necessary. There are no ordinary item inputs or outputs on this form, so item Supply/Output and item Filter remain unavailable. Companion food/lunch supplies reserve; the resulting gases/lye feed the existing chemistry chains. No new unlock is required beyond obtaining the Separator and a companion. The Gas Burning Generator remains excluded.

## Shared contract and paid progress

`ProcessorHost` lets the same `ProcessorJob` serve legacy and formed controllers. The new form-builder `companionAssistance()` option explicitly enables two helpers. Other forms remain disabled until reviewed and opted in; generators reject this option. The shared controller owns production, while `ProcessorJob` owns physical presence and exclusive, expiring UUID leases. No whole machine tick is called twice.

Assistance is considered only after a lane has paid for and performed ordinary progress, with its socket valid, work remaining and native output preflight satisfied. The controller debits each helper once per tick using `MachineCompanionEffort.perQuarter(form.family(), 0)`. Four fractional contributions buy one extra, fully paid processing step. At most one extra step per active lane can occur per tick. With future multi-lane forms, each helper's single reserve debit scales with the number of assisted lanes; fractional progress stays attached to each actual batch. This integration intentionally uses machine defaults, not live recipe lookups or new recipe override semantics.

For the Separator's 256 JE/t family, each helper spends **16 reserve JE/t (320 JE/s)** while contributing. Two spend 32 reserve JE/t combined. Gross reserve cost is separate from regeneration. Native electrical price, including profile and upgrade effects, is paid for every added step. At baseline the average electrical demand is approximately 320 JE/t with one helper or 384 JE/t with two; bonus ticks need 512 JE for that tick's normal plus added step. A recipe's total paid work, input quantities and yield are unchanged. Short batches, insufficient power and discarded fractional contributions can give less than the nominal bonus.

The controller reserves enough electrical headroom for the eligible lanes before spending helper reserve. Partial reserve withdrawals roll back and confer no bonus. A helper with less reserve than one complete payment enters its normal recovery cycle instead of repeatedly attempting unpaid work; its remaining reserve is retained. Credits are transient: they cannot move into a different batch, survive cancellation/reload, or accumulate during pauses and inactivity. Existing batch escrow, save data, output reservations, cancel refunds and atomic completion stay authoritative. The machine's ordinary native behavior continues without helpers.

## Lifecycle, permissions and server cost

Helpers retain the existing owner/party assignments, orders, settings-menu pause, schedules, meals, recovery and town-protection checks. Each productive contribution rechecks the assigned NPC, physical occupancy, active job and permissions. Machine removal/unload releases reservations and animations. Shared helper contributions also require the full requested reserve debit, avoiding a full bonus from an almost-empty reserve.

Planning reads the controller's cached processing state rather than matching recipes. New appended status values distinguish machine pause, incomplete structure and missing machine tools. Missing feed, full outputs and insufficient electricity reuse existing companion statuses. Native periodic structure checks also record unloaded parts; assistance waits while the form is not fully loaded. No chunk loads or global worker/entity searches are introduced.

Each form owns at most two job objects and at most four lanes of transient credit. Entrance discovery is bounded to 48 exterior candidates on the largest form and throttled to once per second, with existing path budgets, collision/hazard checks, navigation failure memory and non-overlapping reservations. Existing canonical-part handling and Planner frame rendering already use the form's real footprint.

## Validation

Automated tests and gameplay were not run, as requested. Final `compileJava compileClientJava assemble` passed in 44 seconds (`build/industrial-companion-build.log`), with existing deprecation/annotation warnings. The first sandboxed compile could not reach its local Gradle daemon; compilation outside that restriction succeeded. The complete Modrinth distribution was rebuilt offline with the existing 18 locked libraries; its embedded JAR and SHA-512 sidecar match the final artifacts. No client launch, launcher import or multiplayer performance claim is made.

Manual checks remaining: each facing and both companion variants; assigning an upper/rear part; one/two/three workers; narrow aisles and blocked front ports; baseline and upgraded speed/power use; depleted reserve and recovery; empty water/brine feed; full or incompatible gas/lye tanks; pause/cancel; missing parts; permissions/orders/settings interruption; removing or unloading a machine; save/reload mid-batch; legacy processors; and a future multi-lane opt-in before enabling that form.
