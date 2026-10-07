# Trench works (batch 50)

Status: implemented (pending CI and review)
Proposal issue: none. The owner picked "4 and 5" from the kaiserpunk ideas list in chat; this is 4. The big guns follow in batch 51.
Owner: jimbozoomer-byte
Target milestone and tier: early (sand, wool, timber, iron), with the searchlight at the steel tier
Primary specialty and supported player role: building and defence

## Player experience
The front line, to go with the landship and the big guns:

| Block | What it does | Recipe (makes) |
| --- | --- | --- |
| Sandbags (slab, stairs) | Burlap sacks laid like brickwork, blast resistance 12, for parapets and gun pits | 8 sand + wool (8) |
| Timber Revetment (slab, stairs) | Boards between posts, to hold up trench walls | 6 sticks + 3 planks (6) |
| Duckboard | A low slatted walkway for trench floors | 3 sticks + 3 wooden slabs (6) |
| Barbed Wire | Slows anything living to 35% of its speed. It cuts for 1 damage while they push through it. Sneaking players pick their way across slower but unhurt | 5 iron nuggets (4) |
| Field Telephone | Power its back and every other telephone on its channel within 256 blocks rings: its bell sounds and its front gives a full redstone signal. Use a dye to set the channel; use it empty-handed to read the channel | 2 iron plates + bell + 2 copper + redstone (2) |
| Searchlight | A lamp drum on a turning yoke that throws a long, fading beam (24 blocks) and lights its surroundings at 15. Use it to turn it a sixteenth of a turn; sneak and use it to tilt it up 15° at a time (0° to 75°). A redstone signal switches it off | 4 steel plates + glass + glowstone + iron ingot (1) |

Slabs make 6 from 3 and stairs 4 from 6, as in vanilla.

## Connections
- Input producers: sand, wool, wood, iron, copper, plates from the metal press, a bell and glowstone.
- Output consumer:
  - building trenches and gun pits for the landship and the batch 51 artillery;
  - telephones join redstone circuits far apart;
  - searchlights light up a battlefield.
- Technology connection: telephones use the control electronics' sixteen dye channels.

## Balance and automation
- Building blocks only.
- Barbed wire's damage counts as a sweet berry bush's: it cuts only things moving through it.
- No recipe returns metal, so there is no loop.

## Multiplayer and persistence
- Everything is a plain block state: a telephone's facing, channel and whether it is called or ringing, and a searchlight's yaw, tilt and whether it is lit. Nothing extra needs saving.
- **Telephones:**
  - They find each other through a per-dimension list on the server, filled as they tick (every 10 ticks, from scheduled ticks that are saved with the chunk).
  - A telephone in an unloaded chunk neither calls nor rings.
- **Searchlight:** it has an empty block entity so the client can draw its head and beam. The beam is visual only; the light comes from the block.

## Dependencies and assets
- No dependencies. All art is original and drawn in `tools/trenchworks.py`.
  - The searchlight's yoke, drum, lens and beam are exported to `assets/jugcraft/trench_quads.json` and drawn by `client/SearchlightRenderer`.
  - The beam is three widening, fading, two-sided translucent segments.
- Code is in `building/`: `Trenchworks`, `DuckboardBlock`, `BarbedWireBlock`, `FieldTelephoneBlock` and `SearchlightBlock`.

## Verification
- Planned in CI:
  - `trenchBlocksPlace`: all 10 blocks place, the duckboard is low, barbed wire has no collision and the searchlight gives light 15.
  - `barbedWireCuts`: a pig pushed through wire is hurt.
  - `fieldTelephonesRing`: powering one telephone's back makes another on the same channel ring and light a lamp in front of it. A telephone on another channel stays quiet.
  - The client screenshot `jugcraft_trench_works`.
- Done locally:
  - `check_mod_data.py` passes. It also checks Java's kinds, strengths and numbers, and the renderer's pivot, against the tool.
  - `check_repository.py` passes.
  - I reviewed a contact sheet of the new textures and redrew the sandbags once.
- Not done: a survival play-test and a two-player server.

## World and event applicability
Not applicable: crafted and placed by players only.

## Rollout and open questions
- The searchlight could later sweep by itself while powered, and the telephone could carry a variable signal strength.
- **Fixed 5 October 2026 (shared render fixes):** the searchlight's drum and yoke are drawn closed (no see-through gaps). Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
