# Rocket launcher: high-explosive and homing rockets

Status: batch 41 is implemented on `feature/rocket-launcher-41`, stacked on `feature/zipline-40`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, approved a "rocket launcher (damage only)" in the rocketry list, and asked for it to be started while CI ran.
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier, after the batch 38 rocket workshop.
Primary specialty and supported player role: combat (a heavy ranged weapon beside the grenade launcher).

## Player experience
- **Rocket launcher** (crafted from four steel plates, a guidance unit and a tripwire hook).
  - Fires a rocket from the inventory, the other hand first. One shot every two seconds.
  - Rockets fly straight at 60 blocks a second, untouched by gravity. They burst when they hit anything, or after 5 seconds.
- **High-explosive rocket** (rocket workshop: four from two solid propellant, two guncotton and a rocket casing).
  - A burst of 12 hearts at the centre, falling off to nothing at 5 blocks.
- **Homing rocket** (rocket workshop: four from two solid propellant, two guncotton and a guidance unit).
  - Locks on to the hostile mob nearest the crosshair: within about 15 degrees of it, within 48 blocks, and in sight.
  - It steers into its target and keeps turning towards it as it moves.
  - Its burst is 9 hearts over 3.5 blocks.
  - With nothing to lock on to, it flies straight.
- **Damage only**, as the owner asked. The burst is the grenade's blast, scaled up: it hurts living things only (not armor stands, item frames or dropped items), walls shield from it, and blast protection counts. It never breaks, moves or burns a block.

## Connections
- Input producer: the rocket workshop, guncotton (field chemistry), solid propellant, guidance units.
- Output consumer: combat. Since guns slice 10A, the guns' Earthmover and Skylark Rifle fire High-Explosive Rockets too, as rockets with a fuse set to the gun's range ([guns, slice 10A](guns.md#slice-10a-the-launchers)); homing rockets stay the launcher's.
- Technology connection: batch 38 rockets, and batches 18 and 31 grenades (the same blast). Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- A high-explosive rocket costs half a solid propellant, half a guncotton and a quarter casing. That is dearer than a frag grenade (a quarter guncotton) for 1.5 times the damage and reach.
- Homing rockets cost a quarter guidance unit each.
- Nothing is produced, so there is no positive-gain loop.

## Multiplayer and persistence
- Server-authoritative: rockets are server entities; the launcher's cooldown is per player.
- The burst hurts players as well as mobs, including the one who fired it if they stand too close. The game's PvP setting still applies.
- Rockets in flight are saved like any projectile, with what is left of their fuse (since guns slice 10A; one saved before then gets the full lifetime).

## Dependencies and assets
- No new dependencies.
- Three 64x64 high-detail icons drawn with `tools/hd_art.py` in `tools/rocketry.py`. Since 6 October 2026 the launcher's and the HE, homing and line-throwing rockets' outlines are opaque: drawn part-transparent, they showed as a see-through rim on the sprite in the hand.
- In flight a rocket is drawn as its item, trailing smoke and flame.
- It uses vanilla firework launch and explosion sounds.

## Verification
- `tools/check_mod_data.py` checks against `tools/rocketry.py`: the cooldown, the homing range and the lifetime.
- Game tests (CI):
  - `heRocketHurtsButBreaksNothing`: a high-explosive rocket fired at a husk hurts it, and the glass beside the husk stays;
  - `homingRocketSteersIntoItsTarget`: a homing rocket fired past a husk turns into it.
- Not tested in CI:
  - firing by hand;
  - target picking by the crosshair;
  - how a rocket looks in flight.

## World and event applicability
Not applicable.

## Rollout and open questions
- A proper 3D rocket model in flight could replace the item sprite.
- Next rocketry batches: booster rails (batch 42, [booster-rails.md](booster-rails.md)), and liquid fuels (kerosene, liquid oxygen).
