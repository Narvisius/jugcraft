# Kinetic power

Status: implemented in source (PR #29); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots a running shaft line.
Proposal issue: none. The owner selected "Kinetic power" directly on 30 September 2026, then "Belts & motor".
Owner: @jimbozoomer-byte
Target milestone and tier: bronze age. The shaft needs iron, the steam engine bronze and a piston, and the dynamo copper and redstone. It can all come before electricity.
Primary specialty and supported player role: engineering

## Player experience
- Turn a **Hand Crank** or fire a **Steam Engine**.
- Run **Iron Shafts** and **Brass Gearboxes** to your machines; they run on the rotation directly, without cables.
- Put a **Dynamo** on the line to feed the electric network.
- Shafts, belt pulleys, the hand crank, the motor's output shaft and the steam engine's flywheel really spin while they turn (PR #37); the gearbox's face gears are animated.
- Link two **Belt Pulleys** with a **Leather Belt** to carry rotation up to 16 blocks without a shaft between them.
- An **Electric Motor** turns JE from cables into rotation.

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

## Belts and the Electric Motor (PR #36)

- **Belt Pulley:** a shaft (placed like a log) that can hold a belt. Use a **Leather Belt** on one pulley, then on another: they must share an axis, sit level with each other along that axis (the belt runs square to it), be at most 16 blocks apart and have no belt yet. The belt is used up; breaking either pulley drops it. Rotation reaching one pulley leaves the other in both directions along its axis. The belt is drawn between the pulleys by a block entity renderer.
- **Electric Motor:** faces the way you look when you place it and drives the block in front. It takes up to 256 JE/t from cables, holds 8,000 JE and puts out up to 96 KE/t at 75%. JE is only used for KE something actually takes, rounded up.
- **No loop:** motor (75%) then dynamo (75%) returns 56% of the JE.
- Recipes: pulley = planks, iron shaft, planks; belt = leather, string, leather; motor = iron plates, copper wire, iron shaft, copper cable.

## Balance and automation
- **Units:** KE per tick. A machine takes 1 KE as 1 JE, up to its normal input rate.
- **Steam engine:** 64 KE/t for 10 mB water per tick, like the steam generator.
  - It burns fuel only while something takes the power.
  - Driving machines directly loses nothing. Through a dynamo it gives 48 JE/t, less than the steam generator's 64. The shaft line trades conversion loss for cable-free, early power.
- **Hand crank:** 16 KE/t for 5 s per right-click (up to 20 s), with a little food exhaustion. It is a bootstrap, not a power plant.
- **Distribution:** power is split evenly across consumers, and the remainder goes to those with room.
  - A network has at most 256 shafts and gearboxes.
  - Networks are cached and rebuilt only when shafts, gearboxes, sources or their neighbors change.

## Multiplayer and persistence
- Server-authoritative. The engine saves fuel, water and burn; the crank its remaining turns; the dynamo its energy.
- The "turning" look is a block state updated only when it changes (client-only updates). Shafts stop turning 10–20 ticks after their last push.

## Dependencies and assets
Fabric API transfer API. Original models and textures, including animated gear textures (MIT). Spinning parts are drawn by `client/KineticRotorRenderer` from `assets/jugcraft/kinetic_rotors.json`, which `tools/kinetic_rotors.py` exports from the same boxes as the block models.

## Verification
- `tools/check_mod_data.py` passes. It now also checks animated texture strips.
- Game tests (CI):
  - `steamEngineDrivesCrusherThroughShafts`
  - `gearboxBranchesToDynamoAndMachine`
  - `handCrankChargesDynamo`
  - `electricMotorDrivesCrusher`
  - `beltCarriesRotation`
  - `beltRefusesBadPulleys`
- The client screenshots `jugcraft_kinetics` and `jugcraft_belts` show running lines.
- Not run: client play, two players, performance with long lines.

## World and event applicability
Not applicable.

## Rollout and open questions
- Belts and the electric motor are in (#36). Belts do not yet change speed or reverse direction; KE has no speed.
- Shafts placed before #37 have no block entity, so they do not spin until re-placed.
- A spinning part is drawn up to 96 blocks away; further off, turning blocks show without it.
- Speed/torque (RPM) is deliberately left out; KE per tick keeps it simple.
- **Fixed 5 October 2026 (shared render fixes):** spinning rotors never share a plane with the still block and their own collars are separated, so they no longer flicker; where a shaft meets the still part (the hand crank's hub plate, the solar tracker's mount bar, the heliostat's mirror) it is set into it rather than held off it, so no see-through slit opens either. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
