# Solar tracker and heliostat field

Status: implemented on `feature/power-21` (batch 21), awaiting the owner's review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 1 October 2026: "Then lets do 7-9", item 9 of the options list ("solar tracker and heliostat"), from the backlog's "Solar tracker and concentrator".
Owner: jimbozoomer-byte
Target milestone and tier: renewable power, after the advanced solar panel (batch 10)
Primary specialty and supported player role: power

## Player experience
- **Solar tracker:** a panel on a motorised mount.
  - Its panel visibly tilts from east at sunrise to west at sunset, and lies flat at night.
  - It gives **20 JE/t** in full sun: two and a half solar panels in one block, half in rain.
  - Cables take power from any side.
- **Heliostat:** a mirror on a post that also turns after the sun. On its own it does nothing.
- **Solar receiver:** an absorber of orange-hot tubes, set on a tower above a field of heliostats.
  - It counts the heliostats under open sky in the square 8 blocks round it, from 1 to 16 blocks below it.
  - It makes **12 JE/t for each**, up to 48 heliostats (576 JE/t), in daylight; half in rain.
  - It boils a millibucket of water for every 32 JE. Pipe or pour water into it.
  - Right-click it to read how many heliostats it sees, its output and its water.

## Connections
- Input producer: sunlight, water (pumps, pipes).
- Output consumer: the power grid.
- Technology connection:
  - power;
  - fluid logistics (water);
  - the kinetic rotor renderer, which now has a "sun" mode that tilts with the time of day instead of spinning.
- Magic connection: none.
- Reachable entry path:
  - tracker: three solar panels, an electric motor, a basic circuit and steel;
  - heliostats: glass panes, an electric motor and steel;
  - receiver: steel fluid pipes, a blast furnace, an advanced circuit and steel.
- Required vs optional: optional.

## Balance and automation
- **Tracker:** 20 JE/t in one block at the cost of three panels and a motor. It trades materials for space; the total output is not larger than the three panels it uses.
- **Field:** each heliostat (half an electric motor and three panes) adds 12 JE/t in daylight. A full field of 48 makes 576 JE/t in the day and nothing at night, so it pairs with storage, such as the flow battery.
- Water is used for the steam: 18 mB/t at full output. Without water it makes nothing.
- Everything stops at night and halves in rain, like other solar power.

## Multiplayer and persistence
- Server-side.
- The receiver saves its energy and water, and re-counts its field every 5 seconds.
- Heliostats hold nothing.
- The tilt is drawn by each client from the time of day; nothing extra is sent.

## Dependencies and assets
No new dependencies. The models (`tools/kinetic_models.py`) and textures (`el_mirror`, `el_receiver` in `tools/electric_textures.py`) are original.

## Verification
- `tools/check_mod_data.py` passes.
- Game test `heliostatsHeatASolarReceiver`:
  - the receiver counts three heliostats under open sky and ignores one under a roof;
  - it makes nothing without water;
  - with water, its output matches the level's daylight and rain (36 JE/t in sun), and it boils water exactly when it makes power. The test world's time is not fixed, so daylight is read from the level.
- Not run:
  - client play (the panels and mirrors tilting, the look of the receiver);
  - a real field at different times of day;
  - two players.

## World and event applicability
Not applicable: built by players; works in any dimension with a sky and a day cycle.

## Rollout and open questions
- Heliostats do not check that their light can actually reach the receiver (blocks in between); only open sky above each mirror counts.
- A multi-block "power tower" with a bigger receiver could come later.
