# Zeppelin (batch 46)

Status: implemented (pending CI and review)
Proposal issue: none; the owner chose it in chat after the building blocks ("start the building blocks batch and then the zeppelin").
Owner: jimbozoomer-byte
Target milestone and tier: petrochemical tier (hydrogen lift cells, a diesel engine)
Primary specialty and supported player role: travel and transport

## Player experience
A rideable rigid airship, about 13 blocks long and 8 tall:
- a banded canvas envelope with red-striped tail fins;
- a riveted rust gondola with portholes, a cabin and a railed deck;
- a red bridge console with a wheel;
- two green diesel engine pods with pusher propellers that spin while the engines work.

| Action | How |
| --- | --- |
| Place | Use the item on the ground (it needs room; it faces the way you face) |
| Board (4 seats) | Use it; the first rider is the pilot |
| Fly | Forward and back keys for throttle, left and right to turn, jump to climb, sprint to sink |
| Refuel | Use a diesel, premium diesel or RP-1 kerosene bucket on it (+1000 mB each, 8000 mB tank) |
| Cargo hold (27 slots) | Sneak and use it |
| Pick up | Hit it; it drops itself and its cargo. Players only; in creative it just goes |

Speeds: top speed 0.35 blocks a tick (7 a second) forward and half that backward. It climbs or sinks at 0.12 a tick and turns 1.5° a tick.

The engines burn 5 mB a second while the pilot is pressing a key, so a full tank gives about 26 minutes of flying. With no keys pressed, or no pilot, it hovers where it is. With an empty tank it sinks gently (0.03 a tick) until it lands. Riders take no fall damage while aboard, but anyone who gets off in mid-air falls.

The pilot sees the fuel left above the hotbar every two seconds.

## Connections
- Input producers: the hydrogen lift cells (drones batch: hydrogen from the electrolytic cell), the diesel engine, steel plates and a chest. Fuel comes from the refinery: diesel, premium diesel, or kerosene from the catalytic cracker.
- Output consumer: travel and carrying cargo.
- Recipe: `HHH / HEH / PCP`, where H is a hydrogen lift cell, E a diesel engine, P a steel plate and C a chest.

## Balance and automation
Fuel is only used up, never made. The cargo hold is an ordinary 27-slot container. There is nothing to automate and no loop.

## Multiplayer and persistence
- **Server authority:** the server flies it. The client only sends the pilot's keys (`ZeppelinInputPayload`, three numbers from -1 to 1). The server clamps them, ignores anyone but the pilot, and stops answering if no keys arrive for 10 ticks.
- **Saving:** fuel and cargo are saved with the entity.
- **Hidden faces:** the model's hidden faces are culled when it is exported.

## Dependencies and assets
- No dependencies. All art is original.
- The canvas textures (`dz_*`) are drawn in `tools/zeppelin.py`, and the item icon is a 16×16 map, `tools/item_icons/zeppelin.txt`, with the big guns' (32x32 from 5 October 2026, redrawn at 16×16 on 7 October 2026, [ITEM_ICONS.md](../ITEM_ICONS.md)). The model reuses the giants' `dr_*` textures.
- The model is exported as quads to `assets/jugcraft/zeppelin_quads.json`: about 2,150 faces, 16-pixel tiles, hidden faces culled. `client/ZeppelinRenderer` draws it.

## Verification
- Planned in CI:
  - `zeppelinHoversSinksAndFlies`: a fuelled zeppelin hovers, an empty one sinks, and a piloted one holding forward and jump climbs, moves forward and burns fuel.
  - `zeppelinKeepsFuelAndCargo`: fuel and cargo survive a save and load.
  - The client screenshot `jugcraft_zeppelin`.
- Done locally: `check_mod_data.py`, which also checks that Java's flight and fuel numbers match the tool, and `check_repository.py` pass. I reviewed an offline render of the model.
- Not done: flying it in a real client, and a two-player dedicated server.

## World and event applicability
Not applicable: it is crafted and placed by players only.

## Rollout and open questions
- The hitbox (5 wide, 7.5 tall) covers the gondola and the middle of the envelope. The envelope's nose and tail can pass through blocks.
- Movement is server-driven, so with high ping the pilot's keys answer a little late.
- Ideas for later: a mooring mast block, cargo winch drops and a bigger airship.
- **Fixed 5 October 2026 (shared render fixes):** the envelope and gondola are drawn closed (no see-through gaps), and the wheel no longer flickers. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
