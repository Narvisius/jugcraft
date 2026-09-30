# Machine control

Status: implemented in source (PR #19); **not yet played**. Compiles in CI; four game tests cover it.
Proposal issue: none; the owner selected it directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: steel tier (upgrades need steel)
Primary specialty and supported player role: engineering and automation

## Player experience
- Make machines faster or cheaper to run with **Speed** and **Efficiency Upgrades**.
- Switch machines on and off with **redstone**.
- Read machines with **comparators**: stored energy, or how full a machine is.

Details and numbers: [TECH_TREE.md → Machine control](../TECH_TREE.md#machine-control).

## Connections
- Existing input producer: steel plates and gears ([steel tier](steel-tier.md)), basic circuits, copper wire, redstone.
- Existing output consumer: every powered processing machine; redstone and comparator circuits.
- Technology connection: works with logistics (upgrade slots are never exposed to pipes) and with the power grid.
- Magic connection: none yet.
- Reachable entry path: the steel tier, circuits and the metal press all come earlier.
- Required vs optional: fully optional.

## Balance and automation
- Speed costs energy: +25% energy per item per card, so four cards double the energy per item.
- Efficiency saves 20% per card, compounding.
- At most 4 of each kind count.
- The highest draw (four speed cards) stays within every machine's input rate.
- No items or energy are created; recipes and outputs are unchanged.

## Multiplayer and persistence
- Server-authoritative. The redstone button sends only a button ID.
- Upgrade cards are stored in the machine's own inventory and drop when it is broken.
- Older saves load with empty upgrade slots and the redstone mode ignored.

## Dependencies and assets
Fabric API only. Upgrade card textures are original (MIT).

## Verification
- `tools/check_mod_data.py` passes (165 IDs).
- Game tests (CI):
  - `speedUpgradesShortenProcessing`
  - `efficiencyUpgradesSaveEnergy`
  - `redstoneHighWaitsForSignal`
  - `comparatorAndUpgradeSlots`
- Not run: client use of the new button and slots, two-client dedicated server.

## World and event applicability
Not applicable.

## Rollout and open questions
- A comparator beside a multi-block's non-master block reads the right value, but only refreshes when a neighbor update reaches it (the machine notifies neighbors of its master block only).
- Capacity or range upgrades could come later.
