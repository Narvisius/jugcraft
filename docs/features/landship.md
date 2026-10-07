# Landship (batch 49)

Status: implemented (pending CI and review)
Proposal issue: none. The owner asked for it in chat ("start with the blocks and the landship") after choosing to merge kaiserpunk and dieselpunk.
- They sent two tank pictures: a rhomboid tank wrapped in tracks with a big riveted side hub, and a heavy multi-track tank with a big gun and smokestacks.
- These were used only as mood. Nothing was copied, and all art is original.

Owner: jimbozoomer-byte
Target milestone and tier: petrochemical tier (diesel engines, steel gears, belts)
Primary specialty and supported player role: combat and travel

## Player experience
A rideable crawler tank about five blocks long, in the shape of the first tanks:
- A rhomboid hull with its tracks running right round each side frame.
- A black-lacquered casemate with gilt trim and a key-pattern frieze across the front.
- A brass-rimmed hub on each side bearing the imperial crest (Kaiserworks, batch 48).
- A sponson machine gun on each flank, amber headlamps, and twin smokestacks over a perforated engine deck.
- A turret on top with a copper cupola, whose cannon turns and lifts to follow where the driver looks.

| Action | How |
| --- | --- |
| Place | Use the item on the ground (it needs room; it faces the way you face) |
| Board (3 seats) | Use it. The first rider drives from the turret hatch; two more ride on the engine deck |
| Drive | Forward and back keys (back is half speed); left and right turn it, on the spot too |
| Cannon | Attack: fires a shell where the driver looks (25° up to 10° down), using one cannon shell from their inventory (none in creative), once every 2 seconds |
| Side guns | Hold use: a burst every half second along the driver's aim, 24 blocks' reach |
| Refuel | Use a diesel, premium diesel or RP-1 kerosene bucket on it (+1000 mB each, 6000 mB tank) |
| Pick up | Hit it until it drops (120 damage; it heals slowly). Players only; in creative it just goes |

While the driver is aboard, their own hands do nothing: attack and use go to the guns instead.

- **Movement:** 0.15 blocks a tick (3 a second), turning 2.5° a tick. It climbs one-block steps by itself. Its hull is 3.5 blocks square, so it rolls over trenches up to about 3 blocks wide.
- **Cannon shell:** it flies in a shallow arc and bursts on whatever it hits.
  - The burst is the grenades' and rockets' blast: 22 damage at the centre, falling to none at 3.5 blocks. Walls shield it, and blast protection counts.
  - It hurts living things only and **never breaks, moves or burns a block**.
- **Side guns:** 4 damage a hit, as the driver's attack, so 8 damage a second when held on a target (a burst every 10 ticks, the same as a creature's hurt cooldown). A wall stops them.
- **Crushing:** driving forward into something living does 4 damage, as the driver's attack, and shoves it aside. It never pushes or breaks blocks.
- **Fuel:** driving and turning burn 5 mB a second, so a full tank lasts about 20 minutes. Standing still and firing burn nothing. With an empty tank it can still fire but cannot move. The driver sees the fuel left above the hotbar.

Cannon Shells: a steel plate, gunpowder and a brass nugget make 4.

## Connections
- Input producers: diesel engines, steel plates and gears, belts (kinetic power), a dispenser, gunpowder and brass, and the imperial crest (Kaiserworks). Fuel comes from the refinery.
- Output consumer: fighting mobs and travel.
- Recipe: `PCP / EDE / BGB`, where P is a steel plate, C an imperial crest, E a diesel engine, D a dispenser, B a belt and G a steel gear.

## Balance and automation
- Fuel and shells are only used up, never made by the landship.
- All its damage is to living things and counts as the driver's attack, so PvP settings apply as they do to the driver's own hits.
- There is nothing to automate and no loop.

## Multiplayer and persistence
- **Server authority:** the server drives it and fires its guns. The client only sends the driver's keys (`LandshipInputPayload`, four small numbers). The server clamps them, ignores anyone but the driver, and stops answering if no keys arrive for 10 ticks. Aim comes from the driver's own synced view direction.
- **Saving:** fuel is saved with the entity.
- **Animation:** each client works out the turret's aim and the track movement from things it already has: the driver's view and the hull's motion. The cannon's recoil uses one synced number, so there is no extra networking.

## Dependencies and assets
- No dependencies. All art is original.
  - The tread texture is drawn in `tools/landship.py`; the Landship's and the Cannon Shell's icons are 16×16 maps in `tools/item_icons/`, with the big guns' (32x32 from 5 October 2026, redrawn at 16×16 on 7 October 2026, [ITEM_ICONS.md](../ITEM_ICONS.md)).
  - The model reuses the Kaiserworks `ik_*` and the giants' `dr_*` textures; its cannon and sponson guns use the tower guns' `tg_tube` steel and `tg_bore` decal. Since the 5 October 2026 art fixes the turret's brass ring sits on the casemate's gilt band (no slit under the turret), no barrel faces share a plane, the sloped front plate is two halves a tile wide (so its texture stays inside the sprite), and the smokestacks stop at 35.75 pixels, just under the cannon's lowest sweep, so the cannon no longer passes through them when the turret turns to the rear (the stack smoke rises from 2.3 blocks); the render box reaches 1.25 blocks above the hit box, so the raised cannon stays drawn ([big-guns-art-fixes.md](big-guns-art-fixes.md)).
  - The sounds are vanilla's: explosion, dispenser and minecart.
- The model is exported as quads to `assets/jugcraft/landship_quads.json` (about 1,030 faces, 16-pixel tiles, hidden faces culled) in four parts: body, turret, barrel and one track link. `client/LandshipRenderer` lays about 33 links round each side and animates them.
- The shell reuses `weapons/Blast`.

## Verification
- Planned in CI:
  - `landshipDrivesOnFuel`: a fuelled landship drives forward for its driver and burns fuel; an empty one stays still.
  - `landshipCannonHurtsButNeverBreaksBlocks`: one shot uses one shell from a survival driver, and its burst against a stone wall hurts a pig in front. Every block of the wall must still stand.
  - `landshipKeepsFuel`: fuel survives a save and load.
  - The client screenshot `jugcraft_landship`.
- Done locally:
  - `check_mod_data.py` passes. It also checks that Java's driving, gun, fuel and size numbers match the tool, along with the renderer's track path, link pitch and turret pivots.
  - `check_repository.py` passes.
  - I reviewed offline renders of the model from the front and back.
- Not done: driving it in a real client, and a two-player dedicated server.

## World and event applicability
Not applicable: it is crafted and placed by players only.

## Rollout and open questions
- The hitbox is 3.5 blocks square and 2.75 tall. The nose and tail of the tracks reach a little past it.
- The tracks animate on clients from the hull's motion, and the side guns' hits come from the driver's eye line.
- Ideas for later:
  - A heavier four-track landship with a bigger gun, like the second reference.
  - Gunners in the sponsons aiming for themselves.
  - Breaking through walls, only if the owner wants block breaking.
- **Fixed 5 October 2026 (shared render fixes):** the hull, turret and barrel are drawn closed (no see-through gaps), and the barrel's muzzle and the turret ring no longer flicker. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
