# Herobrine: sky islands and surface ruins

Status: implemented on `claude/herobrine-haunting` and integrated on `feature/herobrine-player-schedule`. Combined compilation, native tests and rendered galleries passed on 27 October 2026 (see Validation). Not merged to main, not deployed.

Owner: jimbozoomer-byte
Target milestone and tier: the Halloween event, any tier
Primary specialty and supported player role: exploration and atmosphere

## The owner's request (27 October 2026)

The owner approved new world-building and asked for it to be split half and half:

- **OpenAI Codex:** underground and underwater discoveries, how often they happen by depth, and the shared wiring ([herobrine-depths.md](herobrine-depths.md)).
- **Claude (this record):**
  - sky islands carrying substantial haunted buildings, not lone platforms: a ruined castle, a dead-forest manor, a hanging monastery with connecting islands, an inverted-mountain fortress, a shattered castle cluster and a watching keep;
  - proper interiors and routes, weathered stone and dark timber, restrained light, rare enough to be landmarks;
  - surface additions where the existing planner allows: watching huts, unfinished replicas of village houses, ritual clearings.

The owner's ruling: **everything he builds stays.** It stays through approach, logout and the end of the event. Only Herobrine himself vanishes. There is no automatic clean-up and no restoring of what players change. An operator may still undo a mark. Everything is there to explore and mine.

## Who builds what

- **Claude:** `creatures/herobrine/HerobrineSkyIslands` and `HerobrineSurfacePlans`, `HerobrineSkyGameTests`, and this record.
- **OpenAI Codex:**
  - the shared files: the `SKY_ISLAND` and `SURFACE_RUIN` mark types, `HerobrineMarks`, the commands, the exploration director that decides when to look, and the test manifest;
  - `HerobrineDepths`.

The planners are called through two entry points:

- `find(level, near)` for a rare automatic site;
- `plan(level, centre, variant)` for a named kind.

`VARIANTS` lists each planner's kinds for the commands. Both return a `HerobrineMarkPlans.Plan`, which the shared queue builds, records and undoes like every other mark.

## Sky islands (`HerobrineSkyIslands`)

Each island is a floating rock: moss, coarse and rooted dirt on top, stone, andesite and tuff below, deepslate at the bottom, tapering to a point. On it stands a building with rooms and a way through them. Lighting is restrained: soul lanterns, a few candles, some unlit. Each carries a sign with a cryptic line.

- **Ruined castle:**
  - curtain walls whose tops have crumbled, around a gate and a courtyard strewn with rubble;
  - four corner towers, one fallen to half its height, each with a door from the courtyard and a ladder up to its top;
  - a two-storey keep: a door, stairs up to the hall, arrow slits, a table with candles, bookshelves, a skull at the window, lanterns.
- **Dead-forest manor:** a dead island of coarse dirt with bare dark-oak trees and dead bushes. The manor is dark timber on a cobble course, with a log frame and a steep gabled roof. Inside:
  - a hall and a parlour behind a parting wall;
  - stairs to an upper floor with shelves, a rug and a skull;
  - old glass windows, some broken, and a chimney.
- **Hanging monastery:** a deep rock carrying a chapel of stone below and timber above, with pews, an altar and candles. Two small islands, at different heights, each hold a monk's cell. They are joined to the chapel by plank bridges with posts and chain rails, hung beneath with chains. A ladder goes down through the rock from the chapel floor and on, beside a pillar, to a shrine hung beneath the island on chains.
- **Inverted-mountain fortress:**
  - a narrow cone of dark rock, an upside-down mountain, with stalactites;
  - on its flat top, a fortress of deepslate brick: walls, four corner towers and a courtyard with skulls on stakes;
  - a three-floor inner tower with a ladder to its roof and arrow slits.
- **Shattered castle:** five pieces drifting at different heights:
  - a great hall's corner, with a beam and a hanging lantern;
  - a tower's stump;
  - a gate standing alone, its portcullis half down;
  - a stair climbing to nothing;
  - the broken stub of a bridge.
- **Watching keep:** a keep 20 high on a small island, four floors joined by a ladder. Its top room is open on every side, with a skull looking out.

### Where one may be planned

- **Loaded first.** Every chunk the island and its margin could read must already be loaded before any block is read. Planning never loads or generates a chunk.
- **The whole 3D volume is checked, with 6 blocks round it on every side.** It must be:
  - unclaimed;
  - outside the town;
  - empty air throughout.

  A player's block hanging in the sky inside that margin refuses it.
- **Never over the town, however high.** The town's own protection reaches only from 12 below to 96 above its ground. So each column of the volume and margin is asked at the town's height (`protectedTownColumn`). OpenAI Codex's queue asks the same before placing each sky-island block, so a town placed while one is building stops it.
- **High over the ground:** its lowest block at least 24 blocks above anything below it or its margin (the ground, trees or a player's tower).
- **Well below the top of the world:** its margin at least 32 blocks below.
- **Spacing:** no other sky island, finished or rising, within 1024 blocks on either axis. Spacing comes from the saved mark records.
- **Bounded:** at most 15000 blocks.

`find` weighs up to 4 sites, 48–112 blocks from where it is asked, of random kinds. Each site is as high as its clearance needs, plus up to 16 blocks. `lowestTop(level, x, z, variant)` gives the lowest height for an island's top over a column, reading only loaded land.

Each design is drawn from a seed fixed by its column and kind, so the same place always gives the same island. It is turned to one of four headings.

### Built to be undone exactly

The queue builds a plan in order:

1. the solid blocks, bottom up;
2. the fences and iron bars;
3. last, the fixtures: lanterns, candles, ladders, signs, rugs and skulls.

A fixture is drawn only where what holds it is drawn too.

Undo puts back only blocks still exactly as he left them, so no block may change on its own once placed:

- stairs never meet at a corner;
- fences and iron bars are placed after everything beside them and never side by side;
- glass panes are the only blocks that join freely, and the record allows that;
- the ground is moss, coarse or rooted dirt (never grass, dirt or podzol, which change under snow or shade);
- there are no fluids, falling blocks, leaves, vines, redstone or containers.

Fences, bars and panes are placed already joined to what will be round them.

## Surface ruins (`HerobrineSurfacePlans`)

- **Watching hut:** dark timber, 5 by 5, with a slab roof.
  - One window looks towards the nearest player within 96 blocks (towards `toward` when planned by hand); there is a door at the side.
  - Inside are a stool facing the window, an unlit candle, cobwebs and "I WATCHED YOU FROM HERE".
- **Unfinished house:** an original layout like a plains village house, 7 by 7: a cobblestone footing, oak log corners, plank walls, glass windows and an oak stair roof. No game structure file is copied. It was left unfinished:
  - the back wall and one side stop short;
  - only half the roof is on;
  - planks, a log and a crafting table lie inside;
  - a ladder leads up to the unfinished top.
- **Ritual circle:** eight standing stones of mossy cobblestone round a cobblestone altar with a skull. There are candles on the stones (some lit) and round the altar.

### Where one may be planned

- **The usual checks:** the same as his other marks: loaded before any read, wild-looking land with a 6-block margin, nothing claimed or in the town, every block placed into air or plain ground cover.
- **Level ground:** a footprint column one block low takes a cobblestone foundation; any higher or lower refuses it.
- **A ritual circle needs a real clearing:** within 7 blocks, no leaves overhead and nothing standing on the ground. It never cuts a tree or takes a plant away.
- **They only add blocks.**
- **Spacing:** no other surface ruin within 128 blocks.
- **`find` tries several places:** the place it is asked first, then up to 5 random places within 12 blocks, kinds in random order.

## Persistence and undo

Every finished island and ruin stays in the world and in the saved records indefinitely, and players may climb, explore and mine it. One stopped part-built (by a claim, a player's block or a long wait) stays as it is, recorded and undoable, and does not count for spacing. `/jugcraft herobrine marks undo <id>` takes back only blocks still exactly as he left them; anything a player changed stays.

## Validation

`HerobrineSkyGameTests` is registered and passed with the combined Herobrine suite. It checks:

- each of the six sky islands is planned over the arena:
  - every block at least 24 above the ground under its own column (measured before building) and 32 under the world's top;
  - only into empty air, between a lower bound and 15000 blocks;
  - no fences or bars side by side;
  - built whole, every block exactly as planned, its sign written;
  - undone with nothing kept and every block back as it was;
- a sky island is refused by a block in its margin, too little clearance, the top of the world, or a claim;
- a sky island is refused over the town even far above the town's own 96-block protection. The test places a town for a moment and forgets it before returning; a town another test placed is checked by column only. Planning in unloaded land is refused (for sky islands and surface ruins) and loads nothing;
- a standing sky island keeps another away, and once it is undone the place is free again;
- the three surface ruins, on a field of grass blocks (survival terrain), build whole and come down the same way, and a hut turned to the south has its window in the south wall;
- a surface ruin is refused by a player's build in its margin or a bump in its floor. A circle is refused under leaves. A standing ruin keeps another away.

Validated by OpenAI Codex on the combined implementation at `96097d3027`: compilation, all 70 native Herobrine GameTests and 393 scheduler assertions passed. Both client galleries passed together and produced 32 screenshots covering all 22 new cave/water/sky/surface variants. All six island silhouettes and undersides and all three surface ruins were inspected in the rendered gallery. Surface tests use grass blocks, guarding against the 26.3 natural-soil tag mismatch fixed during integration. Full state/sign/undo checks passed for every island.

Exact commands and local evidence paths are in [herobrine-depths.md](herobrine-depths.md#validation). The gallery uses disposable controlled terrain. Human traversal, natural-world placement distribution, multiplayer load/performance and live deployment have not been verified. Neither the owner's personal playtest nor live server was controlled.
