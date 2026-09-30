# Storage

Status: implemented in source (PR #24); **not yet played**. Compiles in CI; game tests cover it.
Proposal issue: none; the owner selected "Storage" directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: the crate is early (iron and planks); the capacitor bank and steel tank need steel.
Primary specialty and supported player role: engineering and logistics

## Player experience
- Bank power in a **Capacitor Bank**.
- Hold a lake's worth of fluid in a **Steel Tank**.
- Keep bulk items in **Item Crates** that pipes and hoppers can feed.

Details: [TECH_TREE.md → Storage](../TECH_TREE.md#storage).

## Connections
- Input producer: every generator, pump and item line.
- Output consumer: machines and pipes.
- Technology connection: energy, fluid and item networks all use the shared lookups.
- Magic connection: none yet.
- Reachable entry path:
  - The crate needs iron plates (metal press) and planks.
  - The bank needs steel, battery boxes and an advanced circuit.
  - The tank needs steel and a tinplate tank.

## Balance and automation
- The bank holds 10× a battery box and outputs 16×.
- The tank holds 8× a tinplate tank.
- Nothing is created: all three are plain stores.

## Multiplayer and persistence
- Server-authoritative. The crate's contents, the tank's fluid and the bank's charge are saved.
- Breaking a crate drops its items. Breaking the tank or bank loses fluid or charge, as with the tinplate tank and battery box.

## Dependencies and assets
Fabric API transfer API. Original models and textures (MIT).

## Verification
- `tools/check_mod_data.py` passes (172 IDs).
- Game tests (CI):
  - `capacitorBankOutputsFromItsFront`
  - `steelTankHolds128Buckets`
  - `crateHoldsOneItemType`
- Not run: client play, two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- The crate shows its item only in a message; a front label that renders the item is a later polish.
- Keeping contents when the crate or tank is broken (as shulker boxes do) could come later.
