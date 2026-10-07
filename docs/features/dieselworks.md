# Dieselworks building blocks (batch 45)

Status: implemented (pending CI and review)
Proposal issue: none; the owner chose it in chat ("start the building blocks batch") from the dieselpunk ideas list.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier (needs iron, copper and steel plates)
Primary specialty and supported player role: building and decoration

## Player experience
Blocks in the look of the batch 44 dieselpunk giants, so a factory can be built to match its machines:

| Block | Variants | Recipe (makes) |
| --- | --- | --- |
| Weathered Steel Plate (`rust_plate`) | slab, stairs | 4 iron plates (4) |
| Riveted Steel Plate (`riveted_rust_plate`) | slab, stairs | 4 weathered steel plate + iron nugget (4) |
| Patina Plate | slab, stairs | 4 copper plates (4) |
| Perforated Patina Plate | | 4 patina plate + iron nugget (4) |
| Red Iron Plate | slab, stairs | 8 weathered steel plate + red dye (8) |
| Copper Dome Plate | slab, stairs | 4 patina plate + copper ingot (4) |
| Riveted Band | | 4 weathered steel plate + 4 iron nuggets (4) |
| Skid Iron | | 4 steel plates (4) |
| Ribbed Patina Pillar / Ribbed Steel Pillar (`ribbed_rust_pillar`) | placed along an axis | 2 patina / weathered steel plate (2) |
| Steel Grating (`rust_grating`, see-through) | slab | 4 iron bars (4) |
| Steel I-Beam (placed along an axis) | | 7 steel plates (6) |
| Porthole Window | | 4 weathered steel plate + glass (4) |
| Amber Cage Lamp (light 14) | | 7 iron nuggets + glowstone + weathered steel plate (2) |

Slabs make 6 from 3 and stairs 4 from 6, as in vanilla.

## Connections
- Existing input producer: the plates from the metal press, iron bars, glass, glowstone and dye.
- Existing output consumer: building only.
- Technology/magic connection: none; this is decoration. The patina, red iron, copper dome and skid blocks reuse the giants' textures (`dr_*`). The steel set has its own `dw_*` textures (see below).

## The steel set's textures (5 October 2026)
The owner called the steel plate blocks horrific ("like you didn't even try") and pointed to bastion concrete as the look they like. The two steel plates had reused the giants' machine textures: warm brown-grey, framed in a near-black outline on all four sides (so a wall became a grid of dark-mortared tiles), with a rust stain and pip-like bolts stamped on every block. The whole steel set now has its own textures in one cool blue-grey steel palette (`tools/dieselworks.STEEL`), drawn by the rules in [ART_DIRECTION.md](../ART_DIRECTION.md#tiling-building-blocks):

| Block | Texture | Look |
| --- | --- | --- |
| Weathered Steel Plate, slab, stairs | `dw_steel_plate` | One brushed sheet a block: the seam split across the edge (lit top and left one shade over the fill, seam bottom and right), brushed streaks one shade up |
| Riveted Steel Plate, slab, stairs | `dw_steel_plate_riveted` | The same sheet framed by twelve raised rivets four pixels apart, so the rivet rows carry on evenly across every joint; none crosses the slab cut |
| Ribbed Steel Pillar | `dw_ribbed_steel` (side), `dw_steel_plate` (end) | A rib every four rows, shaded across like a round column |
| Riveted Band | `dw_steel_band` | A dark strap with a raised rivet every four pixels, unbroken across the block edge |
| Steel Grating, slab | `dw_grating` | A see-through bar grating: bearing bars every four pixels and a pair of cross rails at the slab cut drawn like the pair at the block edge, so both slab halves are closed; 3 x 6 slots |
| Porthole Window | `dw_porthole_steel` | The bolted ring and clear round glass set in a steel sheet, so the wall's seams run past it |
| Steel I-Beam | `dw_steel_plate` (flanges and web) | Plain rolled steel; the I shape shows by its faces' light |
| Amber Cage Lamp | `dw_steel_plate` (base and cap) | The cage bars and amber glass are unchanged |

No colour is random per pixel, and there is no rust. The plates' joints are single, split seams about as strong as one of bastion's course lines, and the rivets are raised (their shadow is one shade under the fill), so a wall does not read as a grid of framed tiles or dice pips. The blocks show as metal on maps (they were brown); only skid iron keeps the giants' warm brown. The giants, the zeppelin and the vehicles keep their own `dr_*` steel, and `dw_porthole` (the old warm ring) stays for the zeppelin's and the raiders' quads, which share it. Block IDs, recipes and model shapes are unchanged; the models point at the new textures, so placed blocks just look new.

The first redraw (5 October 2026) was reviewed the same day. That review found that the grating's middle row of holes crossed the slab cut, so a grating slab ended in open prongs, and that the I-beam and lamp still showed the giants' brown frame and rust mark. It also found the plates' lit edges and dark rivet shadows too strong. These are fixed as described above.

## Balance and automation
One plate of metal makes one block. No recipe turns a block back into metal, so nothing gains material.

## Multiplayer and persistence
Ordinary blocks with no block entities and nothing that ticks. All are mined with a pickaxe and drop themselves; a double slab drops two.

## Dependencies and assets
No dependencies. Code: `building/Dieselworks.java`. Data and art: `tools/dieselworks.py`. All art is original.

## Verification
- Planned in CI: the game test `dieselworksBlocksPlace`. It places all 25 blocks and checks that the grating is see-through, the I-beam is not a full cube and the lamp gives light 14.
- Planned in CI: the client screenshot `jugcraft_dieselworks`, a riveted wall with portholes, a grating catwalk on I-beams and lamps, ribbed steel pillars, a riveted band and a weathered steel floor; since 5 October 2026 the front row also holds the steel plates' slabs and stairs (dx 5 to 8, all inside the frame, with the slab in front of the middle I-beam so the beam stays in view).
- Done offline for the 5 October 2026 textures: 3 x 3 tilings, perspective renders of the screenshot scene (main, first redraw and final), close-ups of the catwalk, I-beams and walls, both grating slab halves, and a palette and noise check (three to seven colours each, no step under 10 brightness, no all-round dark outline, joints about as strong as bastion's course lines). Not yet seen in the game.
- Done locally: `check_mod_data.py` and `check_repository.py` pass, and offline renders of the block models were reviewed.
- Not done: a survival play-test.

## World and event applicability
Not applicable: the blocks are crafted and placed by players only.

## Rollout and open questions
- More shapes could follow if the owner wants them: walls, panes, ladders and doors in these materials.
