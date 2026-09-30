# Kinetic power

Status: implemented in source (PR #29); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots a running shaft line.
Proposal issue: none. The owner selected "Kinetic power" directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: bronze age. The shaft needs iron, the steam engine bronze and a piston, and the dynamo copper and redstone. It can all come before electricity.
Primary specialty and supported player role: engineering

## Player experience
- Turn a **Hand Crank** or fire a **Steam Engine**.
- Run **Iron Shafts** and **Brass Gearboxes** to your machines; they run on the rotation directly, without cables.
- Put a **Dynamo** on the line to feed the electric network.
- Shafts and gearboxes animate while they turn.

Details: [TECH_TREE.md → Kinetic power](../TECH_TREE.md#kinetic-power).

## Connections
- Input producer:
  - the player (hand crank);
  - generator fuel and water (steam engine), with water from buckets, pumps, pipes or a source below.
- Output consumer: every powered machine that is not a generator or battery, through any of its blocks. The dynamo feeds JE networks.
- Technology connection: a second power system alongside JE, with a one-way bridge (dynamo, 75%).
- Magic connection: none yet.

## Large Steam Engine

A 2×2×2 steam engine, four times the small one: 256 KE/t out of a shaft at the back of its upper right block, 40 mB of water per tick, fuel four times as fast. It has a screen like the steam generator's (fuel, water bucket, empty bucket) and a water source under it refills it. It burns only while something on its shaft line takes the power. Built from four small steam engines, iron plates and a casing.

## Balance and automation
- **Units:** KE per tick. A machine takes 1 KE as 1 JE, up to its normal input rate.
- **Steam engine:** 64 KE/t for 10 mB water per tick, like the steam generator.
  - It burns fuel only while something takes the power.
  - Driving machines directly loses nothing. Through a dynamo it gives 48 JE/t, less than the steam generator's 64. The shaft line trades conversion loss for cable-free, early power.
- **Hand crank:** 16 KE/t for 5 s per right-click (up to 20 s), with a little food exhaustion. It is a bootstrap, not a power plant.
- **Distribution:** power is split evenly across consumers, and the remainder goes to those with room.
  - A network has at most 256 shafts and gearboxes.
  - Networks are cached and rebuilt only when shafts, gearboxes, sources or their neighbors change.
- **No JE → KE motor yet,** so there is no conversion loop.

## Multiplayer and persistence
- Server-authoritative. The engine saves fuel, water and burn; the crank its remaining turns; the dynamo its energy.
- The "turning" look is a block state updated only when it changes (client-only updates). Shafts stop turning 10–20 ticks after their last push.

## Dependencies and assets
Fabric API transfer API. Original models and textures, including animated shaft and gear textures (MIT).

## Verification
- `tools/check_mod_data.py` passes. It now also checks animated texture strips.
- Game tests (CI):
  - `steamEngineDrivesCrusherThroughShafts`
  - `gearboxBranchesToDynamoAndMachine`
  - `handCrankChargesDynamo`
- The client screenshot `jugcraft_kinetics` shows a running line.
- Not run: client play, two players, performance with long lines.

## World and event applicability
Not applicable.

## Rollout and open questions
- Belts (connecting shafts at a distance) and an electric motor (JE → KE) are natural follow-ups.
- Shaft rotation is a texture animation, not a rotating model; a real rotating renderer could come in the polish pass.
- Speed/torque (RPM) is deliberately left out; KE per tick keeps it simple.
