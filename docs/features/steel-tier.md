# Steel tier

Status: implemented in source (PR #18); **not yet played**. Compiles in CI; game tests cover it.
Proposal issue: none; the owner selected it directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: Workshops → second material tier (after the metal press)
Primary specialty and supported player role: metallurgy and engineering

## Player experience
Build a brick **Coke Oven** and bake coal into **Coal Coke**, a better fuel. Build a **Steel Foundry** and refine iron with coke into **steel**, the metal the next machines and upgrades are made from. Neither needs power, so steel is reachable before a big power grid.

Details and numbers: [TECH_TREE.md → Steel tier](../TECH_TREE.md#steel-tier).

## Connections
- Existing input producer: vanilla coal and iron (smelted, crushed ×2 or pulverized/washed ×3), bricks, and iron plates from the metal press.
- Existing output consumer: coke fuels the coal and steam generators; steel plates and gears feed the machine upgrades ([machine control](machine-control.md)) and later machines.
- Technology connection: works with hoppers, item pipes, side configuration and eject.
- Magic connection: none yet.
- Reachable entry path: the coke oven needs only bricks, iron and a furnace. The foundry needs iron plates (metal press) and a blast furnace. Nothing here gates those.
- Required vs optional: steel will gate the upgrades; nothing earlier depends on it.

## Balance and automation
- 600 ticks per coke and 400 ticks per steel ingot; there is no energy cost.
- **Metal is conserved:** 1 iron → 1 steel, and coke is not metal. `tools/check_mod_data.py` audits this.
- Coke gives 3,200 generator ticks from one coal: twice coal's 1,600, for 30 seconds of oven time.

## Multiplayer and persistence
Server-authoritative. New IDs only. The machines save items and progress. Recipes are gated by the `machines` switch.

## Dependencies and assets
Fabric API only. The models and textures are original (MIT).

## Verification
- `tools/check_mod_data.py` passes (163 IDs).
- Game tests (CI):
  - `cokeOvenBakesCoke`
  - `steelFoundryMakesSteel`
  - `cablesConnectOnlyWherePowerGoesIn`
- Not run: client play, two-client dedicated server.

## World and event applicability
Not applicable.

## Rollout and open questions
- Coke oven byproducts (creosote, coal tar) belong to the Chemistry branch.
- An electric steel route (the arc furnace) could come later.
