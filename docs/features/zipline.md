# Zipline: zipline anchors and the line-throwing rocket

Status: batch 40 is implemented on `feature/zipline-40`, stacked on `feature/rocket-post-39`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, approved the rocketry list, which includes the "line-throwing rocket/zipline", and asked for it to be started while CI ran.
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier, after the batch 38 rocket workshop.
Primary specialty and supported player role: travel and base-building (crossing valleys, rivers and between towers).

## Player experience
- **Zipline anchor** (crafted, two at a time, from three steel plates, a tripwire hook and two iron bars): a steel post with a pulley on top.
- **Line-throwing rocket** (rocket workshop: a rocket motor, 4 aluminum wire and 8 string).
  - To use it, stand within 4 blocks of an anchor with no line, look at another anchor up to 96 blocks away, and use the rocket.
  - The rocket strings a steel line between the two anchors and is used up.
  - It won't string a line if something solid is in the way, if either anchor already has a line, or if the anchors are too far apart. The overlay says which.
- **Riding:** use either anchor with an empty hand. You hang below the line and run to the other end.
  - Speed is 8 blocks a second on the level, up to 20 on a steep drop. Rides work uphill too.
  - At the end you are set down beside the far anchor. Sneak to let go early; you fall from there.
- **Line:** a taut steel line, drawn from the anchor it was fired from and visible from up to 128 blocks.
- Breaking either anchor takes the line down at both ends. Anyone riding is let go.

## Connections
- Input producer: the rocket workshop (rocket motors), aluminum wire, steel.
- Output consumer: none (travel).
- Technology connection: batch 38 rockets. The line is drawn like the pneumatic grapple's line. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- Each line costs a rocket motor, so it is for crossings worth building. A line carries one rider at a time per ride, but anyone can ride. Nothing is produced.
- No resources are created or destroyed by riding. No positive-gain loop.

## Multiplayer and persistence
- Server-authoritative: the trolley entity is moved by the server and riders follow it; no movement is trusted from the client.
- Lines are saved in the anchors. A ride in progress is not saved: a trolley reloaded from a save disappears and its rider is set down.
- Stringing a line needs a player who may build at both anchors.

## Dependencies and assets
- No new dependencies.
- The line-throwing rocket icon is 64x64 high-detail art drawn with `tools/hd_art.py` in `tools/rocketry.py`.
- The anchor model uses the existing dieselpunk textures. The line reuses the grapple line's texture, tinted steel grey.

## Verification
- `tools/check_mod_data.py` checks against `tools/rocketry.py`:
  - range and reach match `ZiplineAnchorBlockEntity`;
  - its `Result` names match the messages;
  - the anchor and the rocket are registered.
- Game test `ziplineCarriesARider` (CI):
  - a line is refused through a stone block;
  - with the block gone, the line is strung;
  - a second line from either anchor is refused;
  - a zombie rides the line and is set down beside the far anchor;
  - breaking the far anchor takes the line down at the near one.
- Not tested in CI:
  - firing the rocket by hand;
  - a player riding;
  - sneaking off mid-line;
  - how the line looks.

## World and event applicability
Not applicable.

## Rollout and open questions
- A visible trolley model could replace the invisible one.
- Next rocketry batches: the rocket launcher (batch 41, [rocket-launcher.md](rocket-launcher.md)), booster rails, and liquid fuels (kerosene, liquid oxygen).
