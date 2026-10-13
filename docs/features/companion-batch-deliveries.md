# Companion batch deliveries

Owner-requested extension of the companion transport on `peepo-companion`, implemented with OpenAI Codex (GPT-6), 8 October 2026. Reuses existing Fabric storage transactions, server job/path budgets, inventory, workstation adapters and pie animation. No new dependency, unlock, item, recipe or client packet.

## Behavior

Companions collect multiple item types for a workstation in one physical trip, using all empty slots among their eight cargo slots. Existing personal cargo, meals, costumes and equipped tools are preserved. The old 32-item transport cap is removed: each collected stack can reach its item's normal stack limit, subject to the workstation's actual recipe needs and capacity. The eight-slot maximum is 512 items when all eight collected stacks can hold 64. A recipe delivery supplies the missing ingredients for the current recipe; it does not stockpile ingredients the adapter does not request.

A pickup batches the usable contents of the container being visited. Linked supplies remain physically separate visits; this does not remotely collect from other chests or add a multi-stop collection route. Ingredients spread across chests remain supported through successive trips. Generic porter routes also fill available cargo slots and continue to honor filters, Leave and Keep limits. Garden harvests already in inventory are grouped for delivery; seed fetching and tool pickup retain their specialized behavior.

The pickup planner simulates insertion of the whole batch, including combined output capacity, then rolls all destination changes back. Actual extraction and insertion into companion inventory commit together on the server thread. This allows one raw pie and its coal to be collected together, even if the chest lists fuel first: the simulated pie unlocks the oven's fuel requirement. The actual oven changes only when the companion reaches it. Unneeded ingredients are deferred while the remaining useful ingredients unload, then returned to their source. Turning Supply Off returns all collected ingredients. Blocked outputs retain the real cargo for retry.

Every in-flight cargo slot is reserved against eating or meal insertion. The original single-stack save fields remain supported; additional bounded Pending/Unused entries remember the other stacks and slots. No items are recreated from manifests. Real inventory remains authoritative; loading checks slot bounds, duplicate slot references and matching item components. Death drops the existing inventory as before. Invalidated assignments release the job reservations but retain items in inventory.

## Energy and server bounds

While moving, transport spends `2 + max(0, stacks - 1) + max(0, ceil(items / 32) - 1)` JE per tick, based on the remaining delivery load. Empty trips and a single stack of up to 32 cost 2 JE/tick; a pie plus coal normally costs 3; two stacks totaling 128 cost 6; eight full 64-item stacks cost 24. Station work, rest, meals and regeneration retain their existing rules. Existing hearth loading/collection costs remain unchanged.

Batch planning runs only at physical pickup. It snapshots at most 128 source views and performs at most eight bounded passes, allowing dependencies such as pie-before-fuel without an unbounded search. Stock-limit totals remain bounded to 128 views and fail closed when incomplete. At most eight cargo entries are saved or processed, and delivery handles one entry per tick. Existing scheduled job searches, path budgets, loaded-chunk checks and fresh mutation-time permissions still apply. No background polling of every linked inventory is added.

## Validation

The focused `PeepoDeliveryClientTests` exercises batch planning rollback, eight-stack capacity, merging split source stacks, limits, real transport for both variants, personal/equipped item preservation, component-preserving manifest reload, full-output retention, mixed cold-hearth input, multi-ingredient cooking-pot supplies, redundant ingredients and Supply Off returns. It also checks actual load-scaled energy deductions, two competing helpers conserving 640 items, and full-stack garden output preserving replanting seeds. No two-client or large-server load claim is implied.

The accompanying hearth regression passed 10 groups / 159 assertions and the shared-supply regression passed 34 assertions (`build/companion-batch-tests.log`). The final garden-inclusive batch run passed 57 assertions, including actual energy deductions and competing carriers, and assembly succeeded (`build/companion-batch-verified.log`). Repository structure/link and whitespace checks passed. These were real client/integrated-server exercises in disposable flat worlds on Minecraft 26.3, Fabric Loader 0.19.5 and the pinned companion pack, on 8 October 2026. No player world or live server was modified; dedicated-server/two-client and large-scale load testing were not run for this change.
