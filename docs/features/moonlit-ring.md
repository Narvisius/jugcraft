# The Moonlit Ring

Status: implemented in source; not yet run in CI (its results and the client test's pictures will be added here when they are). Part 1 of boss 8, the Alpha Werewolf, in the [bosses plan](../branches/BOSSES.md#the-alpha-werewolf-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the Moonlit Ring, an eighth lair on the shared framework ([hollow-acre.md](hollow-acre.md)), and the Howling Totem that opens it. The Alpha Werewolf, his pack and his loot are part 2, in their own pull request. It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 14 October 2026 the owner chose the Alpha Werewolf as the next boss ("Ok lets do the alpha werewolf boss 8"), built the same two-part way, his lair and summoning first.
Owner: @jimbozoomer-byte

Target milestone and tier: Discovery tier, as the werewolves of the woods are ([even-more-fall-additions.md](even-more-fall-additions.md), addition 23). The totem takes a werewolf's pelt from a full-moon night, bones and silver, which is mined from the first days.
Primary specialty and supported player role: adventuring and hunting: the fighters and hunters who already plan for full-moon nights with silver and wolfsbane.

## Player experience

### The Howling Totem

1. Craft a **Howling Totem**: a werewolf's pelt (any kind: brown, snow or shadow, `#jugcraft:werewolf_pelts`) wrapped round three bones and bound with two silver ingots.
2. Use it standing on the woodland floor (earth a tree could grow on: grass, dirt, podzol, moss, coarse or rooted dirt, mud; under a moss carpet or a layer of snow or not), in the Overworld, **at night** (any night, not only a full moon's, all year), **in werewolf country** (the woods: every forest and taiga and the Grove, `#jugcraft:werewolf_haunts`), under the open sky.
3. The totem howls at the moon, and something far off answers: you come to in the Moonlit Ring's camp. The totem is used up.
4. Where it was used, Grey Mist curls up out of the ground for 60 seconds (`lairs.gate_seconds`), pale ash drifting in it, a howl answering far off now and then. Anyone who uses the mist follows, while the party has room (`lairs.party_size`). Nobody is taken in by standing near it.
5. When something is missing, the totem only says what, and nothing is used up:
   - outside the Overworld: "Nothing here answers the totem's howl";
   - by day: "Nothing answers the totem by day";
   - out of the woods: "The totem's howl is answered only in werewolf country: the woods";
   - with anything over your head that would stop a fall, leaves too (a barrier, which cannot be seen, does not count): "Howl under the open sky, where the moon can hear";
   - off the woodland floor (on stone, sand, planks): "Stand on the woodland floor to plant the totem";
   - with wolfsbane growing or potted within six blocks (and three up or down), the werewolves' own ward reach: "The wolfsbane keeps them away";
   - within three blocks of a mist already risen into the ring: "The mist has already risen here: use it to follow";
   - when every instance of the ring is taken, the shared message that every one is taken, and to try again soon.
6. Every use, answered or not, rests the totem for two seconds. Totems stack to 16.

The Alpha Werewolf is no Witching Season boss, and no full-moon boss: the totem works on any night, all year, whatever `lairs.off_season` says. A player who has hunted one full moon's werewolves keeps the pelts, and can call him up on any night after.

### The Moonlit Ring

A clearing in an old wood, a slab of forest floor about 75 blocks across hanging in the void under a huge full moon. The sky is a deep night blue, the fog a blue-grey, pale ash drifts in the air like moonlit motes, and the music is vanilla's forest music.

- **The arrival:** the **Hunters' Camp**, a hollow at the ring's south edge under the trees: a lean-to of hides (brown wool) on spruce poles, a cold firepit ringed with stones, three werewolf rugs on the ground and a stump with a lantern on it. You come to in its middle, facing north up the path into the clearing. The camp is safe.
- **The way home:** behind you, the trail back through the trees fills with Grey Mist. Use it at any time to go back to exactly where you stood.
- **The ring,** the arena: a clearing 44 blocks across of moss, with short grass and ferns on it, walled all round by the trunks of **giant oaks** (dark oak, three or four blocks thick) in two rings, standing too close to pass between, with a thicket of **Moonwood Leaves** between and behind them. Their canopy closes overhead round the edge and opens in the middle onto the moon.
- **Clear paths:** a path of trodden earth runs round the clearing just inside the trees, and four spokes, three wide, run from the Howling Stone out to the three bell clearings and the camp. Two fallen oak logs, mossed over, and four mossy boulders give low cover. Nothing blocks a run from the centre to any bell.
- **The three bell clearings:** at the ring's west, north and east, each a small glade let into the trees at the end of a passage. In each hangs a **Warden Bell**, an old silver alarm bell under a frame of two spruce posts and a beam, with a little gabled roof, its pull rope toward the ring.
- **The Howling Stone:** at the centre, a grey outcrop of stone, andesite and mossy cobblestone, three blocks high and stepped down round it, with room on its top where the Alpha will crouch (part 2).
- **The moon:** a huge full moon, pale gold (the moon the Hollow Acre has), low over the northern trees. From the top of the Howling Stone it is seen over the canopy.
- **The light:** hidden light blocks a little over head height across the clearing and in the glades and the camp, and the camp's lantern. Every open cell of the clearing and the bell clearings has block light 7 or more.

The fixtures, in part 1:
- **A Warden Bell** is rung by any player in the ring: it tolls across the ring (a deep bell, heard into the trees) and swings for two seconds. While it swings it cannot be rung again ("The bell is still swinging"). Until the Alpha hunts (part 2), ringing one does nothing more.
- **Moonwood Leaves** are seen through but not walked through, never fall and never change with the seasons.

Everything else is the shared framework ([hollow-acre.md](hollow-acre.md#lairs-the-shared-rules)): an instance per opening, at most eight; at most four players; nothing can be built or broken (the bells are the ring's fixtures besides the Grey Mist); the mist's toll at the edges; Grave Goods on death; closing after 30 seconds empty.

## Changes from the plan

- **The totem is used from the hand,** standing on the woodland floor, as the other rites' items are, rather than set on the ground. "Natural ground" is the woodland floor as the werewolves' own spawning judges it: earth a tree could grow on. Snow counts as a layer on such earth, not as a snow block. "Under the open sky" means nothing over your head that would stop a fall, leaves included, so in a forest you look for a gap in the canopy. The plan now says so.
- **A bell's rest is its swing:** it swings for two seconds and cannot be rung again until it stops, rather than resting on a timer of its own. The plan now says so.
- **The camp** has werewolf rugs and a stump with a lantern instead of a rack of old spears, and **the moon** is the pale gold one every lair with a moon has, not silver-white. The plan now says so.
- **Howling Totems stack to 16.**

## Connections

- **Inputs:**
  - a werewolf's pelt: dropped by the werewolves that come out of the woods on full-moon nights, brown, snow-white or, rarely, shadow-black ([even-more-fall-additions.md](even-more-fall-additions.md));
  - three bones, from skeletons;
  - two silver ingots, smelted from silver ore, which is mined from the first days.
- **Outputs:** the way into the Moonlit Ring, where the Alpha Werewolf will wait (part 2).
- **Technology and magic:** the totem is the hunter's: the werewolves' pelts and silver, which already hurts them, and wolfsbane, which already wards them off, now also keeps the totem from being answered. Part 2 brings Arms VII's two Alpha trophies (Moonfang and the Howler) into survival. Of the plan's magic links, none is in the game yet (Echo reading the hunt's target, a Verdance refuge, Tether interrupting a pack signal); none will be needed.
- **Reachable entry path:** every input is reachable without the ring: werewolves come to any player in the woods on a full-moon night, and silver and bones need no machines. No circular unlock.

## Balance and automation

- **The cost:** every opening uses up a Howling Totem, and with it a werewolf's pelt. There is no cooldown beyond the totem's two-second rest: the totem is the limit, as the other rituals' items are.
- **No loop:** nothing comes out of the ring in part 1. Its lair-only blocks drop nothing and have no item, and nothing in it changes by itself once placed.
- **No automation:** only a player's own hand uses the totem, only a player's own use of the mist takes them in, and only a player in the ring rings a bell.

## Multiplayer and persistence

- **Server authority:** the server decides everything. Clients only send the use: the howl, the refusals, the mist and the entry, and the bells' swing all happen on the server. The totem's rest is counted there, and each bell can be rung at most once every two seconds, by anyone.
- **What is saved:**
  - the mist is never saved, since a restart closes its instance;
  - the bells' states are saved with the instance's chunks; each instance is placed fresh, every bell at rest;
  - a player's visit and Grave Goods are saved with the player, as for every lair.
- **Chunks:** no chunk is force-loaded. The mist shows itself only where its chunk is loaded (an entity, ticked by its chunk).
- **Griefing:** nobody is pulled in. A second totem where the mist has risen is refused and kept. Nothing in the ring can be changed but its bells, which every instance places at rest.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. Everything here is drawn by code or is the game's or Jugcraft's own:
- the ring is laid out by `tools/moonlit_ring.py`, on `tools/lair_layout.py`. Its template is vanilla's dirt, coarse and rooted dirt, podzol, moss, dirt paths, stone, andesite, cobblestone, oak, dark oak and spruce logs, spruce planks, slabs, stairs and fences, brown wool, short grass, ferns, moss carpets, a campfire and a lantern, and Jugcraft's own werewolf rugs, but for its two lair-only blocks;
- Moonwood Leaves (clusters of dark blue-green leaves lit silver along their moonward edge) and the Warden Bell (its silver, its yoke of old wood banded with iron, its clapper and its hemp rope) are painted by `tools/moonlit_ring_textures.py`, and their models are built by `tools/lair_data.py` (the bell at rest and swinging, in each of its four facings);
- the totem's icon is a 16×16 map, `tools/item_icons/howling_totem.txt`: a wolf's skull over a rolled pelt bound with silver, with a bone handle, drawn by [ITEM_ICONS.md](../ITEM_ICONS.md) in existing materials (wood, iron, old bone), and checked by `tools/check_icon_maps.py`;
- the dimension files are written by `tools/lair_data.py` from the ring's line in `tools/lairs.py`. Its music is vanilla's forest music, and its motes vanilla's white ash. Its sounds are vanilla's: the wolf's howl, the bell's toll and resonance.

No Mojang texture is read, traced or copied.

New IDs (all under `jugcraft`):
- the dimension, dimension type and biome `moonlit_ring`;
- the structure `lair/moonlit_ring`;
- the lair-only blocks `moonwood_leaves` and `warden_bell`;
- the item `howling_totem`, and its recipe;
- the item tag `werewolf_pelts`.

Nothing is renamed.

## Verification

CI: not yet run. Its results, and the client test's pictures, will be added here.

Run locally (15 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks `Lair.MOONLIT_RING` against `tools/moonlit_ring.py`, the totem, the bells and their registrations against `tools/lairs.py`, and the ring itself (`check_moonlit_ring`, below) | Pass, 2659 IDs |
| Deliberate breaks of the ring and its Java, one at a time (below) | 47 of 47 failed it |
| `python3 tools/check_icon_maps.py tools/item_icons/howling_totem.txt` | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded, with identical pixels or for other features, were put back) |
| `javac` on the new and changed Java and the two test classes, for syntax only | No syntax errors |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

`check_moonlit_ring` checks the ring as `tools/moonlit_ring.py` builds it, and `MoonlitRing.java` against it:
- `MoonlitRing.java`'s heights, the clearing's centre and radius, the glades, the camp, where the Alpha waits, the Grey Mist, and each bell in the order of `tools/lairs.py` BELL_NAMES, with its way out from the centre, are the layout's;
- the arrival stands on the camp's floor with room overhead, and the Grey Mist fills the end of the trail behind it;
- a walk from the arrival, never stepping more than half a block, reaches every open cell of the clearing and of each bell clearing, and comes within reach of each bell;
- each bell hangs at rest from its beam, facing back toward the ring, in its clearing, with a post either side, a ridge over the beam, and room to stand under it;
- the Howling Stone stands three high at the centre with room on its top where he waits, and from his eyes there the moon is seen over the northern trees;
- the light, flooded from the hidden lights, the lantern and the Grey Mist as the game floods block light, is at least `RING_LIGHT` (7) on every open cell of the clearing and the bell clearings, and the client test reads against the same, on the same number of cells;
- nothing in it changes by itself once placed: no vanilla leaves, vines, grass blocks, snow, water, lava or fire, and the camp's fire is cold.

The 47 deliberate breaks: the arrival a block up; a cell of the Grey Mist missing; a wall across the clearing; the north passage choked with leaves; the west bell ringing, the east one facing out; a post of each frame missing; the beams a block high; no room under the bells; the bells hung out in the trees; the Howling Stone two high, and no room on its top; the moon behind the canopy; the hidden lights four levels dimmer; `RING_LIGHT` raised to 10; vanilla oak leaves for the thicket; grass blocks for the moss; the camp's fire lit; water in the clearing; `MoonlitRing.java`'s standing height, centre, bells out of order, the north bell a block low, the camp a row long, where he waits, the Grey Mist a row in, the north bell's way reversed and the glades wider; `Lair.MOONLIT_RING`'s bounds and its moon; the totem's rest and mist reach; the bell's swing; the bell registered as a plain block, the leaves registered solid, the totem unregistered and out of the creative tab; the mist not following into the ring by the totem's way; a totem message and a bell's name without words; the pelts' tag missing a pelt; the bell no fixture; the client test counting a cell short, and reading against 6; `tools/lairs.py`'s totem rest, the bells' names out of order, and a pelt left out.

`check_lairs` also checks `Lair.MOONLIT_RING` against the layout (size, arrival, centre, bounds, floor and the moon), the totem's rest and reach and the bells' swing against `tools/lairs.py`, the pelts the totem takes against every werewolf kind's, the two fixtures' registrations, the bells' names, the totem in the creative tab, and the mist's way into the ring.

What the building found:
- The template leaves out blocks nobody can see, which left a pocket of air inside the Howling Stone. The game would have counted it as open, dark floor, so nothing over the floor of the clearing or a glade is left out.
- The hidden lights at level 12 left the clearing's edge and the glades' sides at 5 and 6; at 15 the darkest open cell is 8.
- The recipe audit counts the metal in every tag a recipe takes, and did not know the new pelts' tag; it is now among the tags that hold no metal.
- CI's first run failed two game tests: on a game-test server every test stands under barriers, which the totem's check counted as a roof. A barrier cannot be seen and hides no moon, so the check now passes over barriers (the sky light the other rites read passes through them too), and the game test checks that a barrier roofs nothing.

The game tests (`MoonlitRingGameTests`) show what a game-test server can:
1. the ring's dimension type and biome load from their files;
2. its template loads in the running game, at the size `Lair.MOONLIT_RING` gives, with the bells, the Moonwood Leaves, the oaks, the moss, the rugs and the Grey Mist, and the game's data version, and without anything that changes once placed;
3. the totem's checks: on grass, podzol, moss, dirt and coarse dirt, under a moss carpet or in a snow layer it is answered, and under a barrier; by day, out of the woods, under a roof of leaves or stone, on stone, or with wolfsbane near it is refused, and answered again once the roof or the wolfsbane is gone; on a moss carpet the feet are taken in its cell; the forests, the taigas and the Grove are werewolf country and the plains, the desert and the swamp are not; no mist has risen where nobody used a totem;
4. the totem used when no instance can open: the ring is full, the totem is kept, nobody moves, no mist rises; by day it is refused and kept; used from the hand, it is rested and kept;
5. the totem is crafted from each kind of pelt, three bones and two silver ingots;
6. the bells: rung by a player's hand a bell swings for two seconds and cannot be rung again meanwhile, then rests and rings again; bells and Moonwood Leaves cannot be broken and have no item; the bells are lair fixtures and the leaves are not; the leaves are seen through and not walked through; each bell is found by its place in an instance, and each clearing round its bell.

The client game test (`MoonlitRingClientGameTests`, CI job `client`) runs the totem from end to end in a real world, with its one player in survival:
1. used in the plains, the totem is refused and kept; with the land made a forest's, by day the same; at night under a stone roof, on stone, and with wolfsbane growing near, the same;
2. used by the player's hand at night on grass under the open sky in the forest, the totem opens an instance in the ring's own dimension and places the ring. It checks the camp's floor under the arrival, the Grey Mist at the end of the trail, each bell at rest and the lantern on the stump. The totem is used up and rested, Grey Mist rises where it was used, and the player comes to in the camp, unable to build;
3. a bell can be used and the leaves cannot; a bell rung by hand swings and then rests. Falling out of the ring throws the player back to the camp for the mist's toll;
4. leaving takes the player back to where they stood. A second totem at the risen mist is refused and kept, and using the mist takes the player back into the same ring;
5. when the instance closes, the player goes home and the mist is gone.

Then it takes ten pictures: the Grey Mist where the totem was used, and in the ring the camp from the arrival, the camp's lean-to and firepit, the clearing from its south edge with the moon over the northern trees, the Howling Stone, the west bell's clearing, the west bell swinging, the north bell from the clearing, the whole ring from over the camp, and the Grey Mist at the end of the trail. Last it reads the game's own block light on every open cell of the clearing and the bell clearings, and fails under 7, or if the cells are not the 1,557 the audit counts.

Not run: the two-client dedicated-server playtest, and play by hand.

## World and event applicability

The Moonlit Ring is its own dimension, so nothing here changes the Overworld. The totem works on any night, all year, in any wood in the Overworld. No season or moon phase gates it, and the werewolves' pelts it takes are kept for good.

## Rollout and open questions

The shared framework's defaults hold: four players and eight instances, Grave Goods on death.

Part 2, the Alpha Werewolf ([alpha-werewolf.md](alpha-werewolf.md)), adds:
- the fight in the ring: his hide, which halves every blow but silver's; his waking howl and his three Packmates;
- the Hunt every 24 seconds: the marked howl, his hiding in a bell clearing with a trail of raked turf to it, the ambush and the pounce, and the Warden Bell that flushes him out if it is rung in time;
- his attacks: the Claw Rake, the Pounce and the Rending Howl; the Tree Leap and the Hamstring Bite of the Moon's Fury at half health; the Last Hunt's changed route below a quarter;
- his loot: the Alpha Pelt, Moonfang and the Howler, the Packfinder Charm, the Alpha's Hood and the advancement Hunter's Moon.
