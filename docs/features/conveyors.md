# Conveyors

Status: implemented in source (PR #38); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots a running line.
Proposal issue: none. The owner selected "Item conveyors" directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: bronze age, alongside kinetic power. A conveyor needs leather belts, iron plates and an iron shaft; the splitter adds bronze gears and a brass plate.
Primary specialty and supported player role: engineering, logistics

## Player experience
- Place **Conveyors** in a line; they run the way you faced when placing them.
- Drive the line with a shaft, gearbox, hand crank or electric motor on any side. One drive runs every conveyor joined to it.
- Items ride on the belt where you can see them. Pipes, hoppers, extractors and machines load it, and so do items dropped on it.
- At the end, items go into the conveyor or inventory ahead, or fall onto the ground.
- A **Conveyor Splitter** sends items left, straight on and right in turn, skipping any way that is blocked.
- A running conveyor carries players and mobs along; sneaking stands still.
- A **Conveyor Slope** carries items one block up or down (PR #44). Use it with an empty hand to switch direction. An up slope hands items to the block in front, one higher. A down slope takes them from a conveyor one block higher behind it.

Details: [TECH_TREE.md → Item logistics](../TECH_TREE.md#item-logistics).

## Connections
- Input producer: anything that inserts through Fabric's `ItemStorage.SIDED` (pipes, extractors, hoppers, machines with Eject), other conveyors, and dropped item entities.
- Output consumer: the conveyor or inventory ahead (any `ItemStorage.SIDED` target), or the ground.
- Technology connection: runs on kinetic power (KE); an electric motor lets JE drive it.
- Magic connection: none yet.

## Balance and automation
- **Speed:** 1/8 block per tick (2.5 blocks a second). Each conveyor holds up to four stacks, a quarter of a block apart, so a line moves at most a stack every 2 ticks.
- **Whole stacks ride together.** An inventory that takes only part of a stack leaves the rest waiting at the front.
- **Power:** 1 KE per conveyor per tick for the whole joined run (up to 64 conveyors). A drive that cannot cover the whole run moves nothing; a run is paid for once per tick even with several drives.
- **Joined:** two conveyors are in the same run when one feeds the other (straight on or from the side).
- **Loading:** an inserted stack waits in a one-stack buffer at the back until there is room on the belt. The buffer cannot be extracted from.
- **Side-loading:** a conveyor feeding into another's side puts items onto its middle.

## Multiplayer and persistence
- Server-authoritative: the server moves items, hands them on and saves them (stacks, positions, the input buffer and the splitter's next output).
- The client gets an update when an item comes on or goes off a conveyor, and moves items itself between updates at the same speed, stopping where the server would.
- Breaking a conveyor drops everything on it.

## Dependencies and assets
Fabric API transfer API. Original models and textures, including the animated belt texture, whose ribs run at the items' speed (MIT).

## Verification
- `tools/check_mod_data.py` passes.
- Game tests (CI):
  - `conveyorCarriesItemsIntoChest`
  - `conveyorNeedsRotation`
  - `conveyorPicksUpDroppedItems`
  - `splitterTakesTurns`
  - `conveyorSlopesGoUpAndDown`
- The client screenshot `jugcraft_conveyors` shows a running line.
- Not run: client play, two players, performance with long or many lines, carrying players.

## World and event applicability
Not applicable.

## Owner-selected performance review and pipe alternative

Industrial planning on 6 October 2026 keeps hoppers and shaft-powered conveyors as early workshop handling options. The owner specifically noted: if conveyor belts prove very costly in late-game factories, bulk transport can be replaced with item pipes whose moving items are not visible. Record this as a conditional performance direction alongside the [industrial starter workshop plan](industrial-starter-workshop-plan.md#selected-early-item-handling-and-conveyor-performance-review).

The pipe alternative uses static pipe blocks and inventory transfers without rendering moving item stacks in the world. Reuse the shared item-transport/inventory interfaces. This is not a benchmark result or a claim that current item pipes necessarily outperform conveyors; client rendering and server transport/routing costs must be measured separately.

Before deciding, compare matched conveyor and non-rendered pipe workloads at small, expanding and large late-game factory scale. Record loaded segments, active lines, moving stacks, source/destination/filter counts, players and hardware/settings. Include active, idle and blocked-output cases; measure server median/p95 tick time, client frame time, synchronization traffic and memory. Identify whether cost comes from rendering, simulation, updates or routing, and agree performance limits from that evidence.

If measured conveyor cost is unacceptable and pipe transport supplies a suitable alternative, make that replacement route accessible for late-game bulk lines and document it in the Encyclopedia. Preserve contents and stable saved identities through ordinary compatibility handling; this planning note does not convert worlds or remove conveyors. No new conveyor/pipe benchmark or game test was performed for this documentation update.

## Rollout and open questions
- Slopes change height one block at a time. There are no vertical lifts, and slopes do not carry players up.
- No filters on splitters; the item sorter does filtering.
- Moving entities on a belt is a simple push; it has not been tried with a player in a client.
