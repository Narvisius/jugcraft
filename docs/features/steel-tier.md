# Steel tier

Status: implemented in source (PR #18); **not yet played**. Compiles in CI; game tests cover it.
Proposal issue: none; the owner selected it directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: Workshops → second material tier (after the metal press)
Primary specialty and supported player role: metallurgy and engineering

## Player experience
Build a brick **Coke Oven** and bake coal into **Coal Coke**, a better fuel. Build a **Steel Foundry** and refine iron with coke into **steel**, the metal the next machines and upgrades are made from. Neither needs power, so steel is reachable before a big power grid.

Details and numbers: [TECH_TREE.md → Steel tier](../TECH_TREE.md#steel-tier).

## Owner-selected industrial planning direction (6–7 October 2026)

The [independent steel and bulk-metallurgy plan](industrial-steel-and-bulk-metallurgy-plan.md) selects Coke Oven -> Steel Foundry as the simple core route, with **charcoal-based starter steel as an alternative in the same foundry** and coke suited to efficient bulk production. Extra refining steps belong to later expansion. Steel develops alongside first electricity, without becoming a new prerequisite for the starter generator or basic circuits.

The owner also selected **hammer/bench steel plates for small batches**, with the same metal yield as machine forming, and **larger foundries and presses alongside existing-machine upgrades** for the first bulk expansion. The larger press uses **shaft drive with later motor drive and reusable interchangeable dies for plates, rods and structural shapes**. **Optional steel casting uses reusable molds in a basin attached to the heat-upgraded larger foundry**, with **fuel-fired heat and hand bellows that later accept shaft drive**. Accessible upgraded clay-based refractories and shared bench/kiln tooling support this pre-electric entry. Metal items melt and transfer internally; casts finish automatically once the readable heat level and material requirements are met. An optional larger Coke Oven supports bulk supply, and compatible alloy equipment gains heat upgrades. Common plates, rods, gears and housings keep component depth manageable. Plate-based routes and small workshops stay useful. Exact construction, work rates, carbon costs, clay/tooling recipes and stock products remain to develop. The next selected topic is industrial chemistry and advanced materials.

Charcoal steel, hammer/bench steel plates, forming dies and steel casting are selected future additions, **not recipes implemented by this brief**. The source behavior and numbers below describe the existing coke route. The starter plan's manual iron plates must make foundry construction reachable before a powered workshop. The linked plan separates selected expansion roles from proposed specifications and additional candidates.

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
