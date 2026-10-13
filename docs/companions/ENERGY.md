# Companion energy

Peepo, pumpkin Peepo and Jughead share a 128,000 JE internal reserve and start full. Existing saved NPCs without energy data also start full. No energy is spent merely wandering. The integrated Companion Generator Wheel drains this reserve into its 32,000 JE buffer and exports through its two outward side faces.

## Balance

Checked against Jugcraft GitHub main on 2026-10-05:
- [Machines and electricity](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/machines-and-power.md): coal 32 JE/t; steam/geothermal 64 JE/t; battery box 400,000 JE.
- [MachineKind.java](https://github.com/jimbozoomer-byte/jugcraft/blob/main/src/main/java/io/github/jimbozoomer/jugcraft/machine/MachineKind.java): solar 8, advanced solar 64, steam/geothermal 64, diesel 256 JE/t.
- [EnergyStorage.java](https://github.com/jimbozoomer-byte/jugcraft/blob/main/src/main/java/io/github/jimbozoomer/jugcraft/energy/EnergyStorage.java): JE transfers use Fabric transactions and a sided block lookup.

The wheel budget is 64 JE/t per NPC (1,280 JE/second): a full reserve lasts 100 seconds without food regeneration. This is mid-tier burst output, not a claim of tested sustained factory output. Idle generation is deliberately much lower than industrial generators.

| Activity | Passive regeneration | Empty-to-full without food |
|---|---:|---:|
| Standing or moving off the wheel | 2 JE/t | 53 min 20 sec |
| Resting on a stool, chair or perch | 8 JE/t | 13 min 20 sec |
| Sleeping in a companion bed, at night | 16 JE/t | 6 min 40 sec |
| Powering a wheel | 0 JE/t | — |

Times assume 20 ticks/second and loaded, ticking entities. Food adds its bonus even while moving/working. Rest and generation are mutually exclusive. Night is Overworld clock ticks 13,000–22,999; dawn ends sleep. Food buffs count down even at full energy, but pause while unloaded; no offline regeneration occurs.

## Food

Hand feeding and nearby dropped food are accepted when health is missing **or energy is strictly below 95% (121,600 JE)**. The existing two-second eating animation, nutrition-based healing and bowl/bottle remainders remain. Energy regeneration begins only when eating finishes.

Quality = nutrition + rounded half of saturation restored, clamped to 1–16. Saturation means the food component's actual saturation points, not its builder's saturation modifier.
- Bonus: quality × 2 JE/t, added to passive regeneration.
- Duration: 30 + quality × 10 seconds.
- Bread: +16 JE/t for 110 seconds; steak: +28 JE/t for 170 seconds.
- Equal or better food refreshes/replaces the bonus. Weaker food still heals but cannot extend a stronger bonus. Buffs never add together.
- Custom food components work automatically, with bounded strength and duration.

Shift-right-click opens the owner/authorized-party command GUI with energy and health percentages. Ordinary empty-hand clicking still triggers blush.

The Routine tab now sets Auto/Day/Night work shifts and energy break/resume percentages. Defaults preserve breaking at 0% and resuming at 80%; the resume setting is always at least 10 percentage points higher. Schedules use the Overworld clock across dimensions, while sleep remains Overworld-night-only. Carried meals and a separately assigned lunch source reuse the same food/energy rules. See [jobs and routines](../features/companion-jobs.md).

## Furniture hooks and wheel integration

`PeepoEntity.setRestMode(CompanionEnergy.Rest.SITTING/SLEEPING/NONE)` is a server-only hook. Furniture must validate occupancy and clear the state on dismount/removal. Rest blocks wandering; direct feeding ends rest. Furniture must re-establish occupancy after load: rest itself resets to NONE to avoid immobilizing an NPC at a missing chair/bed. Colored, stackable companion beds implement the sleeping hook. Stools, shared player chairs and supported block edges implement seated rest; see SEATING.md.

`extractEnergy(requested, TransactionContext)` returns actual JE extracted, at most 64 total per game tick across all callers. Dead, eating or resting NPCs cannot supply energy. Aborted transactions restore reserve and tick budget. A wheel must insert into the receiving storage and extract the matching amount in the same Fabric transaction; do not commit unmatched extraction. There is deliberately no direct cable registration on the entity or electrical charging API. Passive recovery is suppressed during work and the following tick to avoid tick-order double recovery.

Energy, food-buff strength and remaining duration persist and synchronize. Saved values are clamped. Vanilla ground placement and the separate spawning limits are unchanged.
