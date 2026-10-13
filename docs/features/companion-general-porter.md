# General porter and simpler container assignments

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

The current setup is [shared supplies and transport](companion-shared-supplies.md): four Supply/four Output containers, optional job links, tool sources, and filtered porter routes alongside Work. The single-pair instructions below describe the earlier implementation.

Owner-requested on 7 October 2026. Implemented locally by OpenAI Codex (GPT-6) on `peepo-companion`, from `b417f22a`. Reuses the shared companion menu, assignment targets, Fabric inventory transactions, walking transport and search/path budgets. No new assets, recipes, dependencies or progression gates.

## Setup and planner flow

1. Select a tamed Peepo/Jughead with the Companion Planner.
2. Right-click two ordinary containers. The first fills **Supply (blue)** and the second fills **Output (yellow)**. When both roles are occupied, a new container replaces Supply, with an action-bar message naming the new role.
3. Right-click an already assigned container to switch its role. If the opposite role is occupied, the pair swaps roles together. Both new directions must support the transfer and pass loaded/access checks before either assignment changes. An incompatible or unavailable partner leaves both assignments intact and explains why.
4. Left-click removes the link. Shift is no longer used to choose Output; Shift-right-click on a container performs the same role cycle. Shift-right-click air still clears the selected companion. Beds, workstations and lunch sources keep their usual right-click assignment.
5. Shift-right-click the companion without the planner and choose **Porter**. Keep one cargo slot free and a reachable standing position beside both containers. Keep the containers in the same dimension and within 64 blocks of each other; every navigation leg remains limited to 64 blocks.

Supply/Output have distinct blue/yellow world frames and matching GUI row markers. Home/work/lunch and the companion stay green; missing targets are orange. A loaded double chest's frame encloses both halves. The preview still reads only the selected companion's eight explicit links, with no nearby-block scan.

## Porter behavior

Porter repeatedly moves extractable items from Supply to Output. The original 32-item limit is superseded by [batch deliveries](companion-batch-deliveries.md), which fill available cargo slots and scale energy cost with the load. It needs no workstation, recipe or work-assignment slot. It uses the existing eight-slot inventory and walks between endpoints; no items teleport between containers. Item components are preserved, and both pickup into cargo and delivery out of cargo are atomic Fabric transactions.

The companion probes output capacity before pickup. Empty Supply or full Output causes waiting/retry. If Output fills after pickup, the real stack stays in cargo until delivery succeeds. Reserved cargo cannot become an automatic meal or be overwritten by lunch stocking. Player inventory edits remain allowed; removing the carried stack invalidates its manifest instead of recreating items.

**Work** still means assisting assigned machines and using their recipe-specific Supply/Output routes. **Porter** is a separate command so an ordinary helper never starts draining raw ingredients into the finished-products chest merely because its machine is idle. Workstations and per-job Auto/On/Off settings remain saved. Porter does not perform new machine jobs or use their direction toggles. A previously collected stack may finish its valid route after switching between Work and Porter; empty trips and new job selection follow the new command. Follow, Stay and Home pause transport.

Schedules, energy recovery, food/lunch, sleeping/seated rest and the settings-menu pause still apply. Walking now uses the load-scaled drain described in [batch deliveries](companion-batch-deliveries.md), with existing regeneration rules. A porter without transferable items can idle/rest; it does not claim productive machines. Returning to Work restores the existing assignment priorities.

Changing/removing/swapping an endpoint invalidates old route identities. Items already carried stay in the inventory; they are not silently sent backward along a newly swapped route. Save/reload preserves valid carried deliveries, without creating a duplicate stack. A route cannot use the same physical double chest for both endpoints, including two separate chests that are joined after assignment.

This first general porter supports one Supply/Output pair, not loose ground-item collection, filters, keep-stock targets, multiple-route balancing or cross-dimension/offline deliveries. All extractable source items are eligible. Inventory probes retain the existing limit of 128 storage views per candidate; oversized third-party inventories may expose additional slots beyond that bounded window. Specialized containers must honor Fabric's sided storage and transaction contracts.

## Authority, performance and saves

Planner edits keep ownership/party, reach, dimension, loaded-chunk, interaction, town and cooldown validation. The saved clicked face remains authoritative. The new role-switch path validates the opposite container before swapping; it never creates duplicate roles for one block. Container locks and both halves of double chests are checked again through the existing storage adapter at transfer time.

Porter shares the existing transport goal, now with a bounded eight-slot manifest, staggered 80–99 tick searches, server search/path budgets, ten-tick travel-validity cache, bounded probes, 40-tick repaths, deadlines and failure backoff. Inventory mutations recheck endpoints immediately. No new global inventory scans, worker scans, chunk tickets or independent per-tick logistics loop.

`PORTER` appends command value 4; earlier Follow/Stay/Home/Work values remain unchanged. An optional `Transport.Porter` flag defaults false for older saved deliveries. Source is stored in the existing transport `Work` field for direct routes; Output uses `Store`. Existing targets, entity IDs, menu data count and inventory indices remain stable. Update client/server together. No additional resources or currency; this is an alternate use of existing tamed companions, energy, meals and storage.

## Validation

Common/client compilation and assembly succeeded, followed by offline launcher packaging. No automated tests, game tests, in-game visuals or multiplayer load tests were run, following the owner's standing instruction.

Manual acceptance: both companion variants; partial/empty/full containers; every saved face; two competing porters/hoppers; player edits to cargo; swapping/removing links mid-trip; Follow/Work/Porter/menu transitions; recovery/night rest; death and save/reload while carrying; merged/split double chests; locked/protected/unloaded endpoints; and non-extracting/non-inserting faces. Verify item counts and profile representative server populations before deployment.

## Suggested next improvements (not implemented)

- Ghost item filters plus a minimum Supply reserve and Output stock target.
- A small held-planner readout naming the selected companion, hovered role and next click's result.
- Undo the most recent assignment or role swap.
- Optional dropped-item collection within an assigned workshop area, bounded by the same budgets.
- Clear idle reasons such as missing ingredients, full destination or blocked path near the selected assignment.
