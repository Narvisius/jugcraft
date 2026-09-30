# Renewable resources

Status: implemented in source (PR #28); **not yet played**. Compiles in CI; game tests cover it.
Proposal issue: none. The owner selected "Renewable resources" directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier:
- The water wheel and cobblestone generator are early (bronze, a casing and cable).
- The tree farm needs a basic circuit.
Primary specialty and supported player role: engineering and base building

## Player experience
- Build a **Water Wheel** beside a waterfall or stream for steady power without fuel.
- Put a **Cobblestone Generator** between water and lava for endless cobblestone to feed the crusher, sieve and building.
- Load a **Tree Farm** with a sapling for endless logs to feed the sawmill.

Details: [TECH_TREE.md → Renewable resources](../TECH_TREE.md#renewable-resources).

## Connections
- Input producer:
  - water and lava in the world;
  - saplings;
  - power networks (the cobblestone generator and tree farm).
- Output consumer:
  - the crusher (cobblestone → gravel → sand), sieve (gravel → flint and finds) and sawmill (logs → planks and sawdust);
  - the power networks (water wheel).
- Technology connection: the cobblestone generator and tree farm are processing machines, with upgrades, side configuration, eject, redstone modes and comparators. The water wheel is a generator.
- Magic connection: none yet.

## Balance and automation
- **Water wheel:** 8 JE/t for each of its two blocks with flowing water on its right side, 12 if the water is falling, so up to 24 JE/t.
  - That is less than a steam generator (64) but needs no fuel.
  - Still (source) water gives nothing, so it must be a real stream or fall. It is checked once a second.
- **Cobblestone generator:** 1 cobblestone per 20 ticks at 4 JE/t. It needs water and lava touching any sides; neither is used up, as with a vanilla cobblestone generator.
- **Tree farm:** 1 sapling → 6 logs in 400 ticks at 16 JE/t (6,400 JE per tree).
  - The sapling always comes back.
  - There is a 10% chance of an extra: an apple (oak, dark oak), cocoa beans (jungle), pink petals (cherry), a pale moss carpet (pale oak) or a stick.
- **No free metal:** no recipe here makes or consumes metal. The sieve's existing metal-find limit still applies downstream.

## Multiplayer and persistence
- Server-authoritative. The machines save their items, energy, progress, sides and upgrades.
- The water and lava checks are recomputed after loading.

## Dependencies and assets
Fabric API. Original models and textures, including new leaf, bark, soil and lava textures (MIT).

## Verification
- `tools/check_mod_data.py` passes.
- Game tests (CI):
  - `cobblestoneGeneratorNeedsWaterAndLava`
  - `treeFarmGrowsLogs`
  - `waterWheelTurnsInFlowingWater`
- Not run: client play, two players, performance with many wheels.

## World and event applicability
Not applicable.

## Rollout and open questions
- The tree farm could later take bone meal as a speed-up, or grow crops.
- The water wheel's wheel model overhangs into the water column beside it. Water renders around it.
