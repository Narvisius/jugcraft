# Kaiserworks building blocks (batch 48)

Status: implemented (pending CI and review)
Proposal issue: none. The owner asked in chat to merge kaiserpunk and dieselpunk into one style ("start with the blocks and the landship"). They sent two tank pictures for the landship that follows. Those were used only as mood.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier (needs iron, brass and lead plates)
Primary specialty and supported player role: building and decoration

## Player experience
Imperial blocks to dress the dieselpunk set: black lacquered iron with gilt trim, polished brass, marble and leaded glass, for grand stations, halls and the fronts of factories:

| Block | Variants | Recipe (makes) |
| --- | --- | --- |
| Black Lacquer Plate | slab, stairs | 8 iron plates + black dye (8) |
| Riveted Black Plate (gold rivets) | slab, stairs | 4 black lacquer plate + gold nugget (4) |
| Gilt-Trimmed Plate | slab, stairs | 4 black lacquer plate + 4 gold nuggets (4) |
| Polished Brass Plate | slab, stairs | 4 brass plates (4) |
| Gilt Frieze (a gold key-pattern band) | | 3 black lacquer plate + 6 gold nuggets (3) |
| Imperial Crest (faces you when placed) | | 3 black lacquer plate + 4 gold nuggets + gold ingot + red dye (2) |
| Fluted Marble Column / Black Iron Column | placed along an axis | 2 polished marble / black lacquer plate + brass nugget (2) |
| Polished Marble | slab, stairs | 4 calcite (4) |
| Station Tiles (black and cream checker) | slab, stairs | 2 polished marble + 2 black lacquer plate (4) |
| Wrought Iron Lattice (see-through) | slab | 4 iron bars + black dye (4) |
| Leaded Glass (see-through) | | 5 glass + 4 lead plates (5) |
| Imperial Gas Lamp (light 15) | | 4 brass nuggets + 3 iron bars + glowstone + brass plate (2) |

Slabs make 6 from 3 and stairs 4 from 6, as in vanilla.

The crest belongs to an empire of our own, with no real nation's arms. It shows a winged gold cog on a crimson shield.

## Connections
- Existing input producer: the plates from the metal press, calcite, iron bars, glass, glowstone, gold and dye.
- Existing output consumer: building. The Imperial Crest also goes into the Landship ([landship.md](landship.md)) and the Kaiser Pattern ([steampunk-and-kaiser-armor.md](steampunk-and-kaiser-armor.md)).
- Technology/magic connection: none; this is decoration. The set matches the Dieselworks blocks (batch 45) and the giants.

## Balance and automation
One plate of metal makes one block. No recipe turns a block back into metal, so nothing gains material.

## Multiplayer and persistence
Ordinary blocks with no block entities and nothing that ticks. The crest stores which way it faces in its block state. All are mined with a pickaxe and drop themselves; a double slab drops two.

## Dependencies and assets
- No dependencies. Code: `building/Kaiserworks.java`. The lamp reuses the Dieselworks lamp block.
- Data and art: `tools/kaiserworks.py`, which draws every `ik_*` texture. Its models and recipes are written by the Dieselworks writer, which now takes a block set as its input.
- All art is original.

## Verification
- Planned in CI:
  - The game test `kaiserworksBlocksPlace`. It places all 26 blocks and checks that the lattice and leaded glass are see-through. It also checks that the crest keeps its facing and the gas lamp gives light 15.
  - The client screenshot `jugcraft_kaiserworks`: a station front with marble columns, leaded windows, the frieze and crest, a tiled floor and gas lamps.
- Done locally:
  - `check_mod_data.py` and `check_repository.py` pass.
  - The Dieselworks data is unchanged by the writer refactor: regenerating it gives identical files.
  - I reviewed an enlarged contact sheet of the new textures.
- Not done: a survival play-test.

## World and event applicability
Not applicable: the blocks are crafted and placed by players only.

## Rollout and open questions
- More shapes could follow if the owner wants them: walls, panes, railings, doors and arched windows.
