# The Crown of Thunder and the Thunder Vane

Status: implemented in source on branch `claude/storm-roc`; not yet run in CI (its results and the client test's pictures will be added here when they are). Part 1 of the Storm Roc, the seventh of the first eight bosses in the [bosses plan](../branches/BOSSES.md#the-storm-roc-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the Crown of Thunder, a lair on the shared framework ([hollow-acre.md](hollow-acre.md)), and the Thunder Vane that opens it. The Storm Roc, his fledglings and his loot are part 2, in their own pull request. It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 26 October 2026 the owner chose the Storm Roc as the next boss to build ("Storm Roc boss"), built the same two-part way, his lair and summoning first.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.

Target milestone and tier: Discovery tier. The vane takes feathers, copper, a lightning rod and a compass, all made in the first days; the climb to a mountain top in a storm is the real price.
Primary specialty and supported player role: exploration and boss fights: the climbers and explorers of the high country, the Highland and the Karst Pinnacles.

## Player experience

### The Thunder Vane

1. Craft a **Thunder Vane**: two feathers either side of a lightning rod on the top row, a copper ingot either side of a compass, and a copper ingot under it.
2. Raise it (use it) in the Overworld:
   - **while it rains or thunders** (any storm; on a high peak the rain falls as snow, and that counts);
   - **in the mountains**: the Highland, the Karst Pinnacles, Jugcraft's high-country landscapes, and every mountain, peak and hill biome vanilla tags (`#jugcraft:thunder_vane_lands`);
   - **high up**: standing at a height of 120 or more;
   - **under the open sky** (a barrier, which cannot be seen, roofs nothing).
3. Lightning strikes the vane, the vane spins wild and is used up, and the wind takes you to the Crown of Thunder's terrace. The bolt is only light and thunder: it hurts nobody and lights nothing.
4. **Grey Mist** tears open where you stood, whirling and crackling with sparks, thunder rolling in it now and then. For 60 seconds (`lairs.gate_seconds`) anyone who uses the mist follows, while there is room. Nobody is taken in by standing near it.
5. In fair weather, outside the mountains, below 120, under a roof, outside the Overworld, or with a mist already open within 3 blocks, the vane only says why, and nothing is used up. The same happens if every Crown is taken. Every raising, answered or not, rests the vane for 2 seconds.

It works all year: the Storm Roc is no Witching Season boss.

### The Crown of Thunder

The summit of a pinnacle above the clouds, under a storm that never breaks: a slate-grey sky, grey fog, pale ash drifting in the wind, and the music of the jagged peaks. North is the nest.
- **The terrace**, the arrival: stone bricks below the summit's south edge, two stairs up to the summit. Behind it, in an arch of grey stone, the **Grey Mist**: the way home at any time. The terrace is safe.
- **The summit**, the arena: weathered stone, calcite, gravel and moss in patches, 36 blocks across. **Boulders** a block or two high stand round its rim, with gaps between them.
- **The four Storm Rods**: tall rods of weathered copper, banded with verdigris and bound in iron, on plinths of polished andesite round a chiseled middle, 13 blocks north, east, south and west of the summit's middle. In part 2 the storm charges one before each of the Roc's dives: it burns storm-blue and white, gives light (6 at its base, 9 in its middle, 12 at its tip) and crackles with sparks.
- **The ledges**: two shelves below the summit's east and west edges, six blocks down. A player thrown off the summit there lands on one, and a flight of stairs cut into the cliff takes them back up.
- **The nest**: a crag beyond the summit's north edge, four blocks higher, and on it a great nest of **woven branches**: storm-bleached sticks, slate-grey feathers caught in the weave. The Roc waits in it (part 2).
- **Below**: a sea of storm cloud far down round the pinnacle's foot. Nobody falls far enough to reach it: twelve blocks under the summit, the mist throws a faller back to the terrace for the lair's toll (4 damage). It is never a death and never a loss of belongings.

The lair-only blocks (the Storm Rods, the woven branches and the storm clouds) cannot be broken, have no item and drop nothing. Nothing in the Crown can be used, built or broken. In this part the summit is empty: the Storm Roc comes in part 2. An instance nobody is in closes after 30 seconds, as every lair's does.

## Changes from the plan
- **The ledges** are six blocks below the summit, not five, so that the stairs up them are a full flight of six.
- **Moss, not turf.** Grass would spread and die under the boulders once placed; moss does neither, and the audit holds the Crown to nothing that changes by itself.
- **The storm clouds** are a third lair-only block the plan did not name: passable, seen through, far below anyone's reach.
- **Light.** Every lair's sky gives none, so the summit is lit by hidden light blocks: a lattice over it, a ring round its rim, and rows over the terrace and the ledges. The least block light anywhere anyone can stand is 7, as in the Moonlit Ring and the Foundry Trench.

## Connections
- Existing input producer: feathers (chickens), copper ingots and a lightning rod (copper mining and smelting), a compass (iron and redstone); weather and the mountains in the Overworld.
- Existing output consumer: the shared lair framework (`lair/Lairs`, instances, keeping belongings on death, the mist gate, the lair rules). Part 2's boss, fledglings and loot.
- Technology connection: copper and the lightning rod, the first electricity the game knows.
- Magic connection: none needed. The Grounding Strap and the Concordance's storm work will matter in part 2's fight, not in reaching it.
- Reachable entry path (prove no circular unlock): every ingredient is a vanilla item made long before; the mountains, height and storms are in every Overworld. Nothing in the Crown is needed to make the vane. (Part 2's Roc Feathers will make a vane without a lightning rod, after the first.)
- Which connections are required vs optional; trade and solo routes: none beyond the vane; vanes can be traded; the Crown can be entered alone or with up to `lairs.party_size` players.
- How this specialty stays useful without mastering every other branch: it needs only vanilla items and the Overworld.

## Balance and automation
- A vane is spent each time the Crown opens. A refused raising spends nothing and rests it for 2 seconds.
- Nothing is produced in this part: the Crown gives no loot yet, and nothing in it can be broken or carried out.
- No conversion loop: the vane is consumed and makes nothing. The bolt is vanilla's light-only lightning: it hurts nobody, sets nothing alight and charges no creeper, so a vane spent each time is no lightning farm.

## Multiplayer and persistence
- **Server authority.** The checks, the bolt, the mist and the follow run on the server. Nothing in the Crown can be used (`LairRules`): the rods and the nest are no fixtures, and only the Grey Mist takes players.
- **Instances.** Placed fresh from the template each time one opens: every rod calm, the nest empty. Gates are never saved: a restart closes every instance, and anyone found inside a closed one is sent home.
- **Per tick:** nothing of the Crown's runs while nobody is in it. A charged rod's sparks are drawn on each client (the block's `animateTick`), not sent. The vane's mist sends a few particles a tick while it is open.
- **Disable behavior:** the lairs' switch (`agriculture`) off refuses the vane; everything stays registered.

## Dependencies and assets
- No new dependency.
- New IDs:
  - the item `jugcraft:thunder_vane` and its recipe;
  - the dimension `jugcraft:crown_of_thunder` (its dimension type, level stem and biome) and the template `jugcraft:lair/crown_of_thunder`;
  - the lair-only blocks `jugcraft:storm_rod` (states `part` base, middle or tip, and `charged`), `roc_nest` and `storm_cloud`;
  - the biome tag `jugcraft:thunder_vane_lands`.
- Every texture is drawn by code (`tools/crown_of_thunder_textures.py`; the vane's icon from its map, `tools/item_icons/thunder_vane.txt`, a copper arrow on an iron rod with its compass arms). No Mojang file is read, traced or copied. The layout is `tools/crown_of_thunder.py`.

## Verification
- `python3 tools/check_mod_data.py`:
  - `check_lairs`: the `Lair.java` line from the layout; the vane's rest, reach and height; its lands are the tag's and each is a biome or a biome tag; the rods' parts and their light; the fixtures' registrations, the rods' names and the gate's follow; the recipe; the dimension, its type, biome, template and words.
  - `check_crown_of_thunder`:
    - the Java's numbers and the rods' places are the layout's;
    - the arrival stands on the terrace with room overhead, and the Grey Mist fills its arch;
    - a walk from the arrival, never stepping more than half a block (a stair is two half steps), reaches every open cell of the summit, both ledges and the terrace (so a player thrown down can climb back);
    - each rod stands whole and calm, base to tip, on its plinth's chiseled middle, 13 blocks out on its bearing, its tip open to the sky;
    - the nest is woven branches with room over it where the Roc waits;
    - block light flooded from the hidden lights and the mist is at least 7 on all 1,257 open cells;
    - nothing changes by itself once placed.
- Server game tests (`CrownOfThunderGameTests`):
  - the dimension type and biome load;
  - the template loads at the Lair's size, in the game's data version, with its blocks, and nothing in it that changes once placed;
  - the vane's checks: answered in a storm, in the mountains, at 120, under the open sky or a barrier; refused in fair weather, out of the mountains, a block too low and under a roof; which biomes are the mountains;
  - when no instance can open the vane is kept, nobody moves and no mist opens; raised from the hand it is rested;
  - the recipe;
  - a rod charges and calms, each part at its own light, and is found charged by its place; the tip narrows to a point; the rods, the nest and the clouds cannot be broken, have no item and are no fixtures; the nest is solid and the clouds are not; the summit, ledges and terrace are where the arrival, the middle and the rods say.
- Client game test (`CrownOfThunderClientGameTests`), in a real world on a stone pad built at 130:
  - in fair weather, then in the rain in the plains, then in the mountains on a pad at 110 and under a roof, the vane is refused and kept;
  - raised by hand in the rain on the open pad it is used up and rested, opens an instance in the Crown's own dimension with its terrace, Grey Mist, four calm rods on their plinths and nest, brings the player down on the terrace unable to build, and tears a mist open where it was raised;
  - a rod is no fixture; charged, its tip burns at light 12 in the game's own light; a player on a ledge is not thrown back; a fall under the Crown ends on the terrace for the toll;
  - leaving takes the player back to where they stood; a second vane at the open mist is refused and kept; the mist takes them back onto the same Crown; closing it sends them home and the mist goes;
  - pictures: the mist on the pad, the terrace, the summit, the north rod charged, the nest, the east ledge and its stairs, the Crown from above, and the Grey Mist's arch; then the game's own block light on every open cell of the summit, ledges and terrace (1,257, at least 7).
- Not run: play by hand, a dedicated server, two clients, performance measurements.

### Results
- Local, on this branch:
  - `python3 tools/generate_material_data.py` and `python3 tools/generate_textures.py` ran clean. The texture generator re-encoded 606 unrelated existing PNGs; those were reverted, and only the 6 new textures are kept.
  - `python3 tools/check_mod_data.py`: PASS (7,801 material IDs, data files and recipe audit, with `check_lairs` and
    `check_crown_of_thunder`).
  - Breaking the Java on purpose failed `check_lairs` 5 times out of 5: a rod's place, a ledge's height, the tip's light, the vane's least height and the gate's follow into the Crown. Each file was restored afterwards.
  - `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py tools/item_icons/thunder_vane.txt`: PASS.
  - `javac`, syntax only, on the new and changed Java: no parse errors (the game compiles only in CI).

## World and event applicability

The Crown is its own dimension, so nothing here changes the Overworld. The vane works in any storm, all year, on any mountain top in the Overworld; no season gates it.

## Rollout and open questions

The shared framework's defaults hold: four players and eight instances, belongings kept on death in a lair.

Part 2, the Storm Roc (`storm-roc.md`), adds the fight on the summit as the plan describes it: the Rod rule (a charged rod, a marked dive lane, and the Roc grounded when his dive passes the rod); the Wing Buffet, the Storm Dive and the Lightning Marks; at half health the Squall Rises, with Wind Strips and Gale Fledglings; below a fifth the Eye of the Storm; and his loot, the Roc Feathers, the Stormcaller and the Galefeather, the Weather-Vane Lens, the Roc's Crest and the advancement Eye of the Storm.
