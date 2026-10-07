# Rocket post: rocket pads, delivery rockets and flight plans

Status: batch 39 is implemented on `feature/rocket-post-39`, stacked on `feature/rocketry-38`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, approved the rocketry list (rocket post first among the remaining ideas) and asked that "the rocket land when its reloaded": a delivery to an unloaded area waits and lands when the area loads again.
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier, after batch 38's rocket workshop.
Primary specialty and supported player role: logistics over long distances, and trade between players' bases.

## Player experience
- **Rocket pad** (a low gunmetal platform; crafted from a microchip, three steel plates and three smooth stone). It has:
  - nine cargo slots;
  - a slot for a delivery rocket;
  - a slot for a flight plan.
- **Delivery rocket** (rocket workshop: rocket motor, rocket casing, guidance unit). It is used up on launch.
- **Flight plan** (crafted from paper, a compass and a microchip). Sneak and use it on a pad to make that pad its destination; its tooltip shows the target. It stays in the sending pad and is reused.
- **Launching:** press Launch on the pad's screen, or switch on a redstone signal into the pad (once per pulse). The screen shows the result: Launched!, or why not (no rocket, no flight plan, no cargo, the same pad, another dimension, too far, no pad at the target, or a roof overhead).
- **Flight:** 3 seconds plus one second for every 80 blocks. Range is 4096 blocks in the same dimension.
- **Landing:** the cargo goes into the target pad's cargo slots.
  - What does not fit waits and lands as room is made.
  - If the target pad has been removed, the cargo drops where it stood.
  - **If nobody is near the target**, the delivery waits, however long, and lands as soon as the area is loaded again. Nothing is force-loaded. Waiting deliveries are checked once a second.
- **Automation:** hoppers and pipes load cargo from the top and sides, and delivery rockets go only into the rocket slot. Cargo comes out of the bottom, so a pad can feed a storage system on arrival.

## Connections
- Input producer: the rocket workshop (motors, casings, guidance units), microchips, steel.
- Output consumer: any storage or machine line at the receiving end.
- Technology connection: batch 38 rockets and the vanilla firework entity and sounds. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- Each launch uses a delivery rocket: about a processor, a tungsten ingot, five steel plates and propellant. That makes it worth it for long hauls, not for moving items across a room.
- Items are moved, never copied. The cargo leaves the sending pad when the delivery is booked, and is inserted (or dropped) exactly once. No positive-gain loop.

## Multiplayer and persistence
- Server-authoritative. Launching from the screen needs a player who can build there; redstone launches need no player.
- Deliveries in flight or waiting are saved with the world (`rocket_post` saved data), so they survive restarts.
- No chunk loading: a delivery lands only when its target area is already loaded by players or other means.

## Dependencies and assets
- No new dependencies.
- The delivery rocket and flight plan icons are 64x64 high-detail art drawn with `tools/hd_art.py` in `tools/rocketry.py`.
- The pad model uses the existing dieselpunk block textures.
- The pad screen reuses the machine screen background.

## Verification
- `tools/check_mod_data.py` checks against `tools/rocketry.py`:
  - range, flight time and check interval match `RocketPost`;
  - the cargo slot count and the result names match `RocketPadBlockEntity`;
  - the pad and both items are registered.
- Game test `rocketPostDelivers` (CI):
  - a pad under a roof refuses to launch and keeps its rocket;
  - an open pad launches five diamonds, using up the rocket and keeping the plan;
  - the diamonds land in the target pad;
  - a delivery to an unloaded area 3000 blocks away is still waiting after it is due.
- Not tested in CI:
  - a waiting delivery landing once a player loads its area;
  - redstone launches;
  - the screen.

## World and event applicability
Not applicable.

## Rollout and open questions
- Next rocketry batches: line-throwing rockets with ziplines (batch 40, [zipline.md](zipline.md)), the rocket launcher (damage only), booster rails, and liquid fuels (kerosene, liquid oxygen).
