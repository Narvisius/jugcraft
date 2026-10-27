# The Crowfield

Status: implemented in source; CI passes on commit `6d125baa7` (below). The client test's ten pictures are in that run's `client-screenshots-5` artifact only: this environment cannot download CI artifacts, so they are not yet in `docs/images/` and the field's look counts as **not verified** until someone looks at them. Part 1 of the Pumpkin King, in the [bosses plan](../branches/BOSSES.md#the-pumpkin-king-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the Crowfield, a lair on the shared framework ([hollow-acre.md](hollow-acre.md)), and the Harvest Crown that opens it. The Pumpkin King, his Jacks and his loot are part 2, in their own pull request ([pumpkin-king.md](pumpkin-king.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 23 October 2026 the owner asked what Halloween work should come next and chose the Pumpkin King, after the seasonal biome audit ("yes"), on the branch they named ("yes use that branch").
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier, as the Halloween harvest is ([halloween-harvest.md](halloween-harvest.md)). The crown takes heirloom pumpkins (grown from seeds), gold and hay; the scarecrow it is used on is the agriculture branch's.
Primary specialty and supported player role: farming and adventuring: the growers who already raise heirloom pumpkins and dress scarecrows, and the fighters they bring along.

## Player experience

### The Harvest Crown

1. **Craft it:** three heirloom pumpkins (white, Jarrahdale, Cinderella, red kuri or kabocha, mixed as you like) over two gold ingots and a hay bale. It stacks to 16.
2. **Find your scarecrow:** any Jugcraft scarecrow, in the Overworld, wearing a head (any pumpkin it can wear), with the open sky over its head.
3. **Grow the harvest round it:** at least 8 pumpkins or gourds within 6 blocks of it (2 up or down). Pumpkins carved or not, jack-o'-lanterns, the heirlooms, the squashes and gourds, and giant pumpkins all count (`#jugcraft:crowfield_harvest`); a giant pumpkin counts every block of itself.
4. **Use the crown on the scarecrow at night.**
   - The crown settles on the scarecrow's head; the crows burst up out of the field crying, and Grey Mist rises where you stand.
   - You come to in the Crowfield's farmyard. The crown is used up. The scarecrow keeps its head, and every crop round it is left as it was.
   - For 60 seconds (`lairs.gate_seconds`) anyone can use the mist to follow, while there is room. Nobody is taken in by standing near it; a crow cries in it now and then.
5. **When it says no,** nothing is used up, and it says why:
   - used on anything but a scarecrow ("Crown a scarecrow standing among the harvest");
   - outside the Overworld, or by day;
   - off-season, on a server that keeps the Witching Season's rituals to the Halloween event (`lairs.off_season=off`);
   - on a bare-headed scarecrow ("The crown wants a head to sit on"), or one under a roof;
   - among too little harvest (it counts it: "Too little harvest here: 5 of 8 pumpkins or gourds round the scarecrow");
   - where its mist has already risen ("use it to follow");
   - when every Crowfield the server allows is taken.

**The season:** the Pumpkin King is a Witching Season boss, as Vesperine and Madame Tatterlace are ([witching-season.md](witching-season.md#multiplayer-persistence-and-seasons)): the crown works on any night of the year by default, and only during the Halloween event when the server sets `lairs.off_season=off`. Heirloom pumpkins grow all year, so nothing about the crown is seasonal.

### The Crowfield

A pumpkin field at dusk, walled all round with standing corn, 69 blocks across and 76 long:
- **The farmyard** (the arrival): a yard of trodden earth at the field's south edge, behind a split-rail fence with a wide farm gate open into the field. A hay cart, a lamp post, a water trough, a bench with a pumpkin and a white pumpkin on it, and a raven on its perch. Behind it a lane between the corn ends in **Grey Mist**: the way home, at any time.
- **The field:** a square of tilled earth 45 blocks across, its rows running north and south, ridges of rooted earth between furrows of coarse earth. Pumpkins lie along the ridges (91 of them), the heirlooms among them and a few jack-o'-lanterns glowing, with grass and dead bushes here and there. Two stacks of hay bales and a tipped-over cart give low cover. Nothing lies on the cross through the centre or along the ring between the hills, so the way from any hill to the next is always clear.
- **The corn:** Crowfield corn, three blocks tall and too thick to pass, walls the field, the yard and the lane on every side, its tassels and ears along the top. It cannot be broken, and nothing grows or dies in it.
- **The King's hills:** eight hills of dark loam in a ring round the centre, 13 blocks apart in a three-by-three grid, so each is 13 blocks from the next round the ring. On each grows a **King's Gourd**, a great ribbed green gourd on its vine, facing the post, moss over the hill for its leaves.
- **The King's Post:** at the centre, a tall post of stripped dark oak with a crossbar, on a mound of hay bales, where the Pumpkin King hangs waiting ([pumpkin-king.md](pumpkin-king.md)).
- **The sky:** deep violet overhead, an ember haze along the horizon, and a vast harvest moon low over the north-western corn. Chaff drifts in the air; the music is the meadow's.

The King's Gourds are the field's fixtures. A gourd is **green** at rest; it **ripens** (gold-green, then orange, glowing brighter) when the King calls the harvest (part 2); it is **lit** when it ripens through (its carved face burning); and **picked**, only its vine is left. Any player in the field may pick a ripening gourd by using it. A green gourd "is green yet", a lit one "burns too hot to pick", and a picked one has "only the vine left": using them changes nothing. In part 1 nothing ripens them, so they stay green; part 2's King ripens them when he calls the harvest ([pumpkin-king.md](pumpkin-king.md#his-rule-the-harvest)).

Lair rules as everywhere: nothing can be built or broken, only the gourds and the Grey Mist can be used, falling off the field throws you back to the farmyard for the mist's toll, and an instance nobody has been in for 30 seconds closes.

## Changes from the plan

Written while building it, and the plan in [BOSSES.md](../branches/BOSSES.md#the-pumpkin-king-the-plan-being-built) now says the same:
- **The harvest starts from his foe.** The plan's "two neighbouring gourds, never across the field" would have let him call a pair 50 blocks from a lone player in a far corner: more than a sprint covers in 10 seconds. He now calls the gourd nearest the player he is fighting, and its neighbours round the ring. The audit walks it (below): the longest harvest from anywhere in the field is 24.7 blocks.
- **The Last Harvest keeps 10 seconds.** The plan's 8 seconds, with three gourds, was too short for one player; he calls more often instead (every 15 s).
- **The moon is north-west,** not due north: due north, the King's Post stood in front of it from the farm gate.
- **The hills are flat patches of dark loam** with moss for the vine, not raised mounds: a mound a block high would take a jump to reach every gourd.

## Connections

- **Inputs:**
  - heirloom pumpkins, found growing wild in their patches and grown from the seeds they give ([halloween-harvest.md](halloween-harvest.md));
  - two gold ingots and a hay bale;
  - a scarecrow, and a pumpkin for its head, from the agriculture branch;
  - a patch of eight pumpkins or gourds round it, any of the harvest's.
- **Outputs:** the way into the Crowfield, where the Pumpkin King waits ([pumpkin-king.md](pumpkin-king.md)).
- **Technology and magic:** none in part 1. Part 2 brings his trophies, two new Arms VII weapons.
- **Ties to the Halloween content already in the world:** the scarecrows and their heads, the crows (his crown sets them crying), the heirloom pumpkins and giant pumpkins, and the Witching Season's calendar rules. In part 2 he tosses flaming pumpkins of his own.
- **Reachable entry path:** every input grows or is mined without the field: heirloom pumpkins from the wild patches and their seeds, gold from the first days. No circular unlock.

## Balance and automation

- **The cost:** every opening uses up a Harvest Crown: three heirloom pumpkins, two gold ingots and a hay bale. The crown's two-second rest is the only cooldown; the crown itself is the limit, as the other rituals' items are.
- **The harvest is not spent:** the eight pumpkins round the scarecrow stay where they grow. They are the condition, not the cost.
- **No loop:** nothing comes out of the field in part 1. Its lair-only blocks drop nothing and have no item, and nothing in it changes by itself once placed.
- **No automation:** only a player's own hand uses the crown (on a scarecrow), only a player's own use of the mist takes them in, and only a player in the field picks a gourd.

## Multiplayer and persistence

- **Server authority:** the server decides everything. Clients only send the use: the crowning, its refusals, the mist and the entry, and the picking of a gourd all happen on the server. The crown's rest is counted there.
- **What is saved:**
  - the mist is never saved, since a restart closes its instance;
  - the gourds' stages are saved with the instance's chunks; each instance is placed fresh, every gourd green;
  - a player's visit and Grave Goods are saved with the player, as for every lair.
- **Chunks:** no chunk is force-loaded. The mist shows itself only where its chunk is loaded.
- **Griefing:** nobody is pulled in. A second crown where the mist has risen is refused and kept. The scarecrow and the crops round it are left untouched, so a crowning harms nobody's farm. Nothing in the field can be changed but its gourds, which every instance places green.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. Everything here is drawn by code or is the game's or Jugcraft's own:
- the field is laid out by `tools/crowfield.py`, on `tools/lair_layout.py`. Its template is vanilla's dirt, coarse and rooted dirt, podzol, dirt paths, pumpkins (carved, lit and plain), hay bales, stripped dark oak, spruce planks, slabs, fences and fence gates, moss carpets, short grass, dead bushes, water cauldrons and a lantern, and Jugcraft's own heirloom pumpkins and raven perch, but for its two lair-only blocks;
- Crowfield corn (dry stalks and arcing leaves, its tassels and ears on top) and the King's Gourd (its rind green, gold and orange, its stem scar, its burning face, its vine) are painted by `tools/crowfield_textures.py`, and their models are built by `tools/lair_data.py` (the gourd in each of its five stages and four facings; lit, its face is a slab of its own at full light);
- the crown's icon is a 16×16 map, `tools/item_icons/harvest_crown.txt`: a band of gold woven round hay with three pumpkins for its points, drawn by [ITEM_ICONS.md](../ITEM_ICONS.md) in existing materials (gold, magma, stem, straw), and checked by `tools/check_icon_maps.py`;
- the dimension files are written by `tools/lair_data.py` from the field's line in `tools/lairs.py`. Its music is vanilla's meadow music, and its motes vanilla's white ash. Its sounds are vanilla's: the parrot's call pitched down (as the crows' is), a gold crown's equip, wings, the pumpkin's carving and a crop's breaking.

No Mojang texture is read, traced or copied.

New IDs (all under `jugcraft`):
- the dimension, dimension type and biome `crowfield`;
- the structure `lair/crowfield`;
- the lair-only blocks `crowfield_corn` and `kings_gourd`;
- the item `harvest_crown`, and its recipe;
- the block tag `crowfield_harvest`.

Nothing is renamed.

## Verification

CI (23 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `44209e862` | Build, data audit, game tests, client game tests | **Failed to compile** the game tests: `CrowfieldClientGameTests` indexed its list of test crops as an array (`crops[i % crops.size()]`), which only a full compile catches |
| `16d8be1ca` | The same, the list indexed with `get` | **One failure.** Everything compiled and every game test passed; client shard 5 failed `CrowfieldClientGameTests` on its own setup: "Among too little harvest, the crown was not refused", because the pumpkins laid round the roofed scarecrow, a few blocks off, were counted in the bare one's harvest |
| `6d125baa7` | The same, the roofed scarecrow's pumpkins taken away, the arrival's floor checked as dirt path (not face-sturdy) and moss carpet counted as standing room | **All pass:** all 2267 required game tests, `CrowfieldGameTests` among them, and the client test classes `tools/select_client_tests.py` picked, `CrowfieldClientGameTests` among them: the Harvest Crown passed end to end, and the game's own block light was at least 8 on all 1,890 of the field's open cells (the darkest at 8, at 12, 5, 24, against the 7 the test asks). It took the ten pictures (`0000_jugcraft_harvest_crown_mist` to `0009_jugcraft_crowfield_exit`, 480x270), uploaded as artifact `client-screenshots-5` of run 40036507233; they have not been looked at |

Run locally (23 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks `Lair.CROWFIELD` against `tools/crowfield.py`, the crown, the harvest tag, the gourds and their registrations against `tools/lairs.py`, and the field itself (`check_crowfield`, below) | Pass |
| `python3 -m unittest discover -s scripts/tests` | Pass |
| Deliberate breaks of the field, the crown and their Java, one at a time (below) | 29 of 29 failed it |
| `python3 tools/check_icon_maps.py tools/item_icons/harvest_crown.txt` | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded, with identical pixels or for other features, were put back) |
| `javac` on the new and changed Java and the two test classes, for syntax only | No syntax errors |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

`check_crowfield` checks the field as `tools/crowfield.py` builds it, and `Crowfield.java` against it:
- `Crowfield.java`'s heights, the field's centre and edges, the hills' spacing, the farmyard, the fence and gate, where the King hangs, both Grey Mists, and each hill in the order of `tools/lairs.py` HILL_NAMES, with its place in the grid, are the layout's; and each hill is 13 blocks from the next round the ring;
- the arrival stands on the farmyard's floor with room overhead, and the Grey Mist fills the end of the lane behind it; the fence runs along the field's south edge with the farm gate open in it;
- a walk from the arrival, never stepping more than half a block, goes through the gate and reaches every open cell of the field and the yard, and comes within reach of each gourd;
- each gourd grows green on its loam, its face toward the post;
- **the harvest's walk:** from every open cell of the field, walking round the cover (a diagonal step only where both its sides are open), to within reach of each of three gourds round the ring from the one nearest, either way round, is within 80% of what a sprint (5.6 blocks a second) covers in the 10 seconds a gourd takes to ripen. The longest is 24.7 blocks, from the field's south-east corner, under 4.5 seconds;
- the King's Post stands on its mound with its crossbar, with room six high in front of it where he hangs; the Grey Mist's place at the post is open floor; and from the arrival the moon is seen over the corn;
- the corn walls the field on every side but the fence;
- the light, flooded from the hidden lights, the jack-o'-lanterns, the lantern and the Grey Mist as the game floods block light, is at least `FIELD_LIGHT` (7) on every open cell of the field, and the client test reads against the same, on the same 1,890 cells;
- nothing in it changes by itself once placed: no farmland, crops, stems, grass blocks or vanilla leaves, nothing that burns or flows.

The 29 deliberate breaks: `Crowfield.java`'s field edge, two hills out of order and a hill's grid place; the hills farther apart; the farm gate shut, and no gate at all; a gourd ripening from the start, and the gourds facing away from the post; the moon behind the post; a gap in the corn; the field's lights dimmer; farmland in the furrows; the harvest ripening in 4 seconds; the King off his mound; pumpkin stems in the rows; the crown's harvest and reach in the Java, and the crown counting another tag; the harvest tag missing a gourd, and naming a block that is none; a gourd's light and stage in the Java; the corn registered solid; the crown out of the creative tab; the mist not following into the field by the crown's way; a hill and a crown message without words; `Lair.CROWFIELD`'s arrival; the client test counting other cells.

`check_lairs` also checks `Lair.CROWFIELD` against the layout (size, arrival, centre, bounds, floor and the moon), the crown's rest, reach and harvest against `tools/lairs.py`, the harvest tag and that each of its blocks exists, the gourd's stages and light, the two blocks' registrations, the hills' names, the crown in the creative tab, and the mist's way into the field.

What the building found:
- The harvest as planned could not be picked alone from a far corner (above).
- From the farm gate the King's Post stood in front of the moon; the moon moved to the north-west.
- The lit gourd's face, drawn first as a second box over the body, left the body's front face out, which the art check counts as a hole; the face is now a slab of its own at the front.
- The first check of the gourds' facing compared them with the same layout function that placed them, so a wrong function agreed with itself (a deliberate break went uncaught); the check now asks only that each face is turned toward the post.
- CI's first runs: `CrowfieldGameTests` indexed a list as an array, which does not compile (the local syntax check cannot see types); then the client test's own setup counted the roofed scarecrow's pumpkins toward the other's harvest (they now have none: the roof is checked first). Tracing the rest of it against the layout found two more: the arrival stands on the yard's trodden path, which has floor but not a full top face, and the light's count must take the hills' moss carpets as open floor, as the audit does.
- The alchemy catalogue (`data/jugcraft/alchemy/values.json`, checked by `scripts/tests/test_alchemy_values.py`) needs a value for every item. Its builder, `tools/build_alchemy_values.py`, reads the game's own jar, which is not here, so the crown's record was added as the builder computes it: its recipe's cheapest ingredients, three heirloom pumpkins at 1, two gold ingots at 2,048 and a hay bale at 216, 4,315 in all. Nothing is made from the crown, so no other value changes.

The game tests (`CrowfieldGameTests`) show what a game-test server can:
1. the field's dimension type and biome load from their files;
2. its template loads in the running game, at the size `Lair.CROWFIELD` gives, with the corn, the gourds, the hay, the post, the farm gate, pumpkins, jack-o'-lanterns, white pumpkins and the Grey Mist, and the game's data version, and without anything that changes once placed;
3. the crown's checks: a headed scarecrow among eight of the harvest at night, under the open sky (or a barrier), is answered, from either half of it; bare-headed, under a roof, by day, off-season, on stone or among seven it is refused; a pumpkin two blocks up counts, three up does not; every heirloom, the hand-carved pumpkin, the squashes, the gourds and the giant pumpkin count, and pumpkins, carved pumpkins and jack-o'-lanterns, but not melons or hay; no mist has risen where nobody used a crown;
4. the crown used when no instance can open: the field is full, the crown is kept, nobody moves, no mist rises, and the scarecrow's head and the harvest stay; by day it is refused and kept; used on stone it is kept and not rested; used on the scarecrow from the hand it is rested and kept, and the head stays;
5. the crown is crafted from three heirlooms (the same or mixed), two gold ingots and a hay bale, and not from plain pumpkins;
6. the gourds: used by a hand, a ripening gourd is picked and a green, lit or picked one is not; each stage's light is the table's; a gourd is picked once and sprouts again; gourds and corn cannot be broken and have no item; the gourds are lair fixtures and the corn is not; the corn is seen through and not walked through, and stands in tassel; each hill is found by its place, nearest itself, 13 from its neighbour round the ring and in the field; the centre is in the field and the arrival in the yard.

The client game test (`CrowfieldClientGameTests`, CI job `client`) runs the crown from end to end in a real world, with its one player in survival:
1. on a bare-headed scarecrow the crown is refused and kept; given a head, among seven of the harvest the same; with the eighth in, by day the same; off-season on a server that keeps the rituals to the Halloween event the same, and answered with the event on; at night on a scarecrow under a roof, the same;
2. used by the player's hand on the headed scarecrow at night among the harvest, it opens an instance in the field's own dimension and places the field: the yard's floor under the arrival, the Grey Mist at the end of the lane, each hill's gourd green, the King's Post and the lamp post's lantern. The crown is used up and rested, the scarecrow keeps its head and the harvest stays, Grey Mist rises where the player stood, and they come to in the farmyard, unable to build;
3. a gourd can be used and the corn cannot; a green gourd is not picked and a ripening one is, by hand. Falling out of the field throws the player back to the farmyard for the mist's toll;
4. leaving takes the player back to where they stood. A second crown at the risen mist is refused and kept, and using the mist takes the player back into the same field;
5. when the instance closes, the player goes home and the mist is gone.

Then it takes ten pictures: the Grey Mist by the crowned scarecrow, and in the field the farmyard from the arrival, the yard's hay cart and lamp post, the field through the gate with the moon over the north-western corn, the King's Post, the west hill with its gourd, a gourd ripening and one lit, the corn wall close to, the whole field from over the yard, and the Grey Mist at the end of the lane. Last it reads the game's own block light on every open cell of the field, and fails under 7, or if the cells are not the 1,890 the audit counts.

Not run: the two-client dedicated-server playtest, and play by hand.

## World and event applicability

The Crowfield is its own dimension, so nothing here changes the Overworld: a crowning leaves the scarecrow and its crops as they were. The crown works on any night, all year, by default; a server can keep it to the Halloween event (`lairs.off_season=off`), as it can Vesperine's and Madame Tatterlace's rituals. Nothing earned is taken away when the event ends.

## Rollout and open questions

The shared framework's defaults hold: four players and eight instances, Grave Goods on death.

Part 2, the Pumpkin King ([pumpkin-king.md](pumpkin-king.md)), adds:
- the fight in the field: his waking from the post, his straw that burns (fire hurts him half as much again), his Jack-o'-Straws;
- the Harvest every 22 seconds: the gourds ripening from the one nearest his foe, picked in time to leave him bereft, or lighting to hatch a Jack or heal him;
- his attacks: the Scythe Sweep, the Gourd Toss (the Horseman's flaming pumpkins) and the Crow Storm; the Crowning at half health; the Vine Snare and the Head Roll; the Last Harvest below a quarter;
- his loot: the King's Ember, two new Arms VII trophies (the Hallowreap and the Crowbill, with the Fright boon), the Crowward Charm, the Pumpkin King's Crown, and the Witching Season's event loot during Halloween.

The owner chose for the King's trophies to come with part 2, not as a part of their own.
