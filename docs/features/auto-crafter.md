# Auto-Crafter

Status: implemented in source (PR #30); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots its screen.
Proposal issue: none. The owner selected "Auto-crafter" directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: workshop tier (brass plates, a basic circuit, a casing).
Primary specialty and supported player role: automation and logistics

## Player experience
- Lay out any crafting recipe in the **Auto-Crafter**'s 3×3 grid, as on a crafting table.
- Feed it more of the same items by hand, pipes or hoppers. It crafts on its own, one item every 2 seconds, and pushes the result out with eject.

Details: [TECH_TREE.md → Automation](../TECH_TREE.md#automation).

## How it works
- **The grid is both the pattern and the stock.** Each grid slot always keeps its last item, so the machine crafts only while every filled slot holds at least two. The recipe is never lost when an input runs dry.
- **Automation respects the pattern.** Pipes, extractors and hoppers can only add to grid slots that already hold that exact item; empty slots accept items only from the player. A hopper can't turn planks into a different recipe.
- **Remainders:** empty buckets, glass bottles and the like go to the slot above the output. If a recipe leaves two different kinds of remainder, the crafter waits rather than lose one.
- **Recipes:** any crafting-table recipe the server knows (vanilla, Jugcraft or data packs), found through the vanilla recipe manager.

## Connections
- Input producer: pipes, extractors, hoppers and the player.
- Output consumer: eject into pipes and inventories.
- Technology connection: a powered processor, with upgrades, side configuration, eject, redstone modes, comparators and kinetic power.
- Magic connection: none yet.

## Balance and automation
- 40 ticks per craft at 8 JE/t (320 JE per craft). Speed and efficiency upgrades apply.
- It does nothing a crafting table can't; it only saves clicks. No recipe changes, no free items.
- **Limit:** ingredients that don't stack (for example milk buckets) can never reach the two-item threshold. Recipes that need them can't be automated here yet.

## Multiplayer and persistence
Server-authoritative. The grid, output, remainder slot, progress and settings are saved like any machine's. Recipe lookup runs on the server only.

## Dependencies and assets
Fabric API. Original steampunk model (workbench with a gantry arm) and classic textures (MIT).

## Verification
- `tools/check_mod_data.py` passes.
- Game tests (CI):
  - `autoCrafterKeepsItsPattern`
  - `autoCrafterKeepsRemainders`
  - `autoCrafterAutomationFollowsPattern`
- The client screenshot `jugcraft_auto_crafter_screen` shows the grid layout.
- Not run: client play, two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- A ghost "result" preview on the screen needs the recipe on the client (recipes are server-side in 26.x), so it would need a small sync packet. That is left for later.
- Unstackable ingredients could be supported with a separate pattern (ghost) grid later.
