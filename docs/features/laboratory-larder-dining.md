# The Laboratory, the Larder and the Dining Room

Status: implemented. CI builds it and runs its game tests and client screenshots (below); not yet played by hand.
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("Lets come up with another 20 very detailed well thought out additions to add for the halloween / fall season using the first 3 images for ideas of prop sets … lets do them piece by piece maybe 5 at a time"). These are prop sets 11–15 of the [Witching Season plan](witching-season.md), drawn from the reference pictures' monster's head and green hands, purple spider, ghost, purple lantern and black cat silhouettes.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier. Every recipe takes early materials (copper, iron, gold, brass, glass, planks, wool, string, cobwebs, bones, rotten flesh, dyes, glowstone). The Lightning Harness's lightning rod, the console's comparator and the clock's clock need only the Overworld.
Primary specialty and supported player role: building, with useful pieces for redstone and storage. These props furnish a mad scientist's laboratory, a spider's larder, a haunted dining room and a Halloween yard; the Spindle Loft lair (boss 2) uses the larder's.

## Player experience

### 11. The Reanimation Rig
1. **Lightning Harness** (copper ingots, a lightning rod and brass ingots): a riveted copper crown with a cage of copper ribs and coil rings round a wound coil, two glowing glass valves, and two brass electrode arms hanging on chains. It hangs from the underside of a block or from a chain, and falls without it.
   - **It fires** on a strong redstone signal (13 or more), when a running Tesla Coil within eight blocks arcs, or when lightning strikes within four blocks of it. Then it rests a second before it can fire again.
   - Firing, arcs crack between its electrode tips and down to what lies below, with a crack of thunder and sparks.
   - **It wakes the patient**: if a Lab Table lies up to four blocks under it, the patient under the sheet sits bolt upright for five seconds, arms raised out of the sheet, groaning, its eyes flashing green through the sheet, then sinks back.
2. **Brain-Vat Console** (glass, redstone, rotten flesh, brass ingots and a comparator): a riveted brass console with two gauges and a toggle, and on its back a glass vat of green fluid in which a brain floats.
   - **It is an analogue memory cell**: it remembers the strongest signal fed into its back and gives it out of its front (and to a comparator) until a signal arrives at either side, which clears it to whatever its back then reads.
   - Its brain glows and sparks as brightly as what it remembers (light up to 7). Use it to be told what it remembers.
3. **Crawling Hand** (rotten flesh and a bone): a grey, stitched, severed hand lying on its block. It drums its fingers one after another; powered, it scuttles round the top of its block, fingers walking, a lap every four seconds.

### 12. The Spider's Larder
1. **Silk Cocoon** (string and white wool): a body-shaped bundle wound in silk, a face pressed through it, hanging on a thread from the underside of a block or a chain, swaying gently. It falls without what holds it.
   - **A 9-slot larder**: use it to open it, and it wriggles and groans. A comparator reads it; hoppers fill and empty it. Broken, it spills what it held.
   - Now and then at night it twitches by itself.
2. **Egg Sac Cluster** (string and a spider eye; three at a time): glistening white sacs on a mat of web, on floors, walls or ceilings, any or all of a block's faces at once, like glow lichen. Their glints pulse slowly. At night, now and then, three tiny spiderlings skitter out across a face and vanish (drawn by the client: no mobs are spawned). Broken, it gives one cluster a face.
3. **Web Drape** (four cobwebs): a great sagging web curtain two blocks by two, glinting with dew, placed facing you from the block you aim at to your right and up. You can walk through it, slowed (0.6 across, 0.75 up and down, where a cobweb's is 0.25 and 0.05). Breaking any part brings it all down, for one drape.
4. **Silk Spool Stack** (wool and planks): three tall turned walnut spools on a board with a needle. **Use a dye on a spool to rewind it in that colour**; they start white, purple and red.

### 13. The Poltergeist's Dinner Party
1. **Haunted Dining Chair** (dark oak planks, red wool and sticks; two at a time): a tall gothic chair with turned legs, a tufted red velvet seat and a pierced tracery back under a carved crest and finials. It faces away from whoever places it, toward the table they stood across from.
   - **Sit in it** (not while sneaking).
   - **At night, when a player comes within two blocks of an empty chair, it slides out from the table toward them**, juddering and scraping, and five seconds later slides back.
2. **Floating Table Setting** (a bowl, a glass bottle, an iron nugget and a candle): a place laid in the air over a table, each piece bobbing out of step a hand's breadth above it. You walk through it.
   - **Use it** to lay the table in turn for **dinner** (a pewter charger with a napkin, a fork and knife, a goblet of wine and a candlestick), **tea** (a saucer and cup, and a teapot that now and then tips and pours) or **a feast** (a roast bird on a platter, garnished, with a goblet and a candlestick).
   - At night the goblet now and then tips right over and rights itself.
   - **Flint and steel or a fire charge lights its candle** (light 7); a sneaking empty hand snuffs it.
3. **Grandfather Clock** (dark oak planks, glass, a clock and a gold ingot; two blocks tall): a walnut longcase clock with a moulded plinth, a glazed trunk door showing the brass weights on their chains and the swinging pendulum, a hood with brass-capped columns, a brass dial with an ivory chapter ring, an arch of night sky above it, and a broken pediment with brass finials.
   - **Its hands show the time of day** (dawn is six, noon and midnight twelve), and **the moon in its arch shows tonight's phase**.
   - **On each hour it sends a redstone pulse** and strikes the hour on its bell, a second apart. A comparator reads the hour, 1 to 12.
   - **At midnight**, for ten seconds, a pale face looks out through the glass of its trunk.

### 14. The Witchlight Lantern Path
1. **Witchlight Lamp-Post** (iron ingots, a gold nugget and purple stained glass; two blocks tall): a fluted cast-iron post on a stone foot, with a crook arm and scroll holding a lantern out in front.
2. **Witchlight Path Stake** (a gold nugget, purple stained glass and a stick; two at a time): the lantern on a twisted iron stake with two curls, knee-high.
3. **Hanging Witchlight** (an iron chain, a gold nugget and purple stained glass): the lantern on a chain, for a ceiling, a beam or a fence.

Each lantern is a pierced iron base, four corner posts, leaded glass panes and a gothic peaked cap, and what burns in it is a wisp, turning and bobbing in a soft glow of the glass's colour.
- **They wake as you come.** Asleep, each glows dimly (light 3), its wisp small. When a player comes within six blocks it flares awake (light 14) with a soft rising chime; the glow swells over two seconds. It stays awake ten seconds after the last player leaves, then fades. Walk a path of them and the lamps ahead light as those behind go dark.
- **A redstone signal keeps one awake** (into the lantern's block, or a lamp-post's foot).
- **Use a dye on the glass** to change its colour: purple, lime (green), orange, light blue (blue) or red.

### 15. The Yard Silhouettes and the Harvest Moon Lamp
1. **Yard Silhouette** (planks, black dye and a stick; two at a time): a black-painted plywood cut-out on a weathered stake. It needs ground under it, and you walk through it.
   - It faces whoever places it, in any of sixteen directions.
   - **Use it to change the figure**: an arched cat on a fence rail, a prowling cat, a witch on her broom (with her cat riding behind), a flock of bats, a wolf howling on a rock, or a crow on a post. It says which.
   - **At night their eyes glow yellow.**
2. **Harvest Moon Lamp** (white wool, glowstone and gold ingots; 2 × 2): a great moon two blocks across in a wrought-iron ring sixteen bars round, on two short posts on a footed base, with a garland of autumn leaves over its lower arc and a pumpkin at each foot. Placed facing you from the block you aim at to your right and up; breaking any block brings it all down, for one lamp.
   - **Its face shows tonight's moon phase**, with a sleepy face in its craters. Lit, it shines (light 15) and its face glows; use any block of it to light or put it out.
   - **A comparator reads the phase**: 15 at full moon, 11, 8 and 4 as it wanes, 0 at new. That matters on werewolf nights.

## Changes from the plan
- **The Silk Spool Stack** takes any wool and planks, and its spools start white, purple and red, recoloured by dye.
- **The Harvest Moon Lamp** stands on its own base; it does not hang on a wall.
- **The Lightning Harness's recipe** has no chain: it takes copper, a lightning rod and brass. The Brain-Vat Console's has rotten flesh for its brain.
- **The Floating Table Setting** can be placed anywhere, not only over a table.
- **Art fixes, 5 and 6 October 2026**: the Egg Sac Cluster's sacs are closed boxes (its item seen from behind showed them hollow), the Grandfather Clock is closed and its corner finials no longer lie flush with its pediment, the Lab Table patient's flashing eyes show the bright middle of their glow, and the client's cut-outs are one plane each. The full list is in [witchs-workshop.md](witchs-workshop.md#changes-from-the-plan).

## Connections
- Existing input producer:
  - copper, iron, gold, glass, planks, wool, string, cobwebs, bones, rotten flesh, spider eyes, dyes, glowstone;
  - the brass ingot (the factories), the lightning rod, the comparator and the clock.
- Existing output consumer:
  - the Lab Table (batch 8): the harness wakes its patient;
  - the Tesla Coil (batch 8): its arcs fire a harness near it;
  - redstone: the console (memory cell), the clock (hourly pulse and hour reading), the moon lamp (phase reading), the hand, the harness;
  - storage: the cocoon (9 slots);
  - werewolves (fall 23): the moon lamp reads the full moon they rise on;
  - building; the Spindle Loft lair (boss 2) is furnished with the larder's pieces.
- Technology connection: redstone, comparators, hoppers. Magic connection: the moon's phase (werewolves), the Lab Table's patient.
- Reachable entry path: all need only early materials and a crafting table. None needs these props first.
- Required vs optional: everything is optional; nothing gates progression.

## Balance and automation
- **The console** stores one signal strength; it makes no items.
- **The cocoon** holds 9 slots for string and a wool, less than a chest.
- **The clock and the moon lamp** read the world's clock and moon; they do not change them.
- **Spools and witchlights** take a dye each to recolour; nothing is given back.
- **Work done**: the harness looks for lightning every second tick within four blocks and for running Tesla Coils every five ticks within eight; witchlights look for players twice a second within six blocks; a chair looks once a second within two; the clock checks the world's hour each tick (a division). All are bounded and local. Spiderlings, arcs, sways, bobbing, hands, glows and faces are client animations.
- **Recipes**: each takes materials in and gives the prop; breaking gives the prop back. Egg sacs (three from a string and a spider eye), chairs (two) and path stakes and silhouettes (two each) are worth no more than they cost, and nothing turns them back. No loops.

## Multiplayer and persistence
- **Server-side**: firing and waking, remembering and clearing, scuttling, opening and wriggling, recolouring, sliding, laying and lighting, striking and pulsing, waking lamps, changing figures and lighting the moon are all decided on the server.
- **Build rights**: dyeing spools and witchlights needs the right to use blocks there.
- **Saved**:
  - block state: the harness's and the hand's power, the console's memory and clearing, the egg sacs' faces, the drape's and the moon lamp's parts and light, the chair's slide, the setting and its candle, the clock's pulse, the lamps' colour and waking, the silhouette's figure and turn;
  - block entities: the cocoon's 9 stacks, the spools' colours, and the clock's last hour;
  - the harness's cooldown, the chair's time out and a lamp's last sight of a player are not saved; at worst a lamp or chair settles a moment late after loading.
- **Drops on breaking**: the cocoon spills what it holds. The drape and the moon lamp drop once from their first block.
- **New IDs**:
  - blocks and items `lightning_harness`, `brain_vat_console`, `crawling_hand`, `silk_cocoon`, `egg_sac_cluster`, `web_drape`, `silk_spool_stack`, `haunted_dining_chair`, `floating_table_setting`, `grandfather_clock`, `witchlight_lamp_post`, `witchlight_path_stake`, `hanging_witchlight`, `yard_silhouette`, `harvest_moon_lamp`;
  - block entity types `lightning_harness`, `crawling_hand`, `silk_cocoon`, `egg_sac_cluster`, `silk_spool_stack`, `haunted_dining_chair`, `floating_table_setting`, `grandfather_clock`, `witchlight`, `yard_silhouette`, `harvest_moon_lamp`;
  - their recipes.
- **Feature switch**: the recipes follow the agriculture feature switch.

## Dependencies and assets
- **No new dependencies.**
- **Models**: built on `tools/flora_art.py` by `tools/decor19_data.py`, which paints their textures by code (128 × 128 for the chair, the setting, the clock and the silhouettes; 64 × 64 for the rest). The egg sacs' texture is a strip of six 32 × 32 frames, so they pulse.
- **Art style**: the creature props (faces, bones, fur, scales, fire) are painted with the clean, cute painters in `tools/cute_art.py` since 5 October 2026, after the owner asked for simpler, smoother creatures ([ART_DIRECTION.md](../ART_DIRECTION.md#creatures-and-faces-cute-and-clean)).
- **Moving parts**: the hand and its fingers, the patient's arms and eyes, the cocoon, the spiderling, the spools' silk, the chair, the setting's pieces and flame, the clock's hands, moon, pendulum and ghost, the witchlights' wisp and glow, the silhouettes and their eyes, and the moon's faces are in `assets/jugcraft/decor19_quads.json`. The glows, the wisp, the spiderling and the moon's eight faces are small textures under `textures/entity`; the harness's arcs reuse the Tesla Coil's.
- **Originality**: all original. The look follows the owner's reference pictures, and nothing is traced from them.

## Verification
CI's Build workflow passed on d0675afc (run 37229731350): the generated data matched, the audit passed, the mod built, all 795 server game tests passed (these fourteen among them), and all three client shards passed and took the screenshots in [the gallery](../branches/AGRICULTURE.md#the-laboratory-the-larder-and-the-dining-room). Runs before it found, and the commits after them fixed:
- 37229123018: a client renderer turned the spiderlings with a quaternion, and 26.3's `PoseStack.mulPose` takes only a matrix; it now builds one;
- 37229257288: the test sources named `Items.BLUE_DYE` and `Items.LIME_DYE`, which 26.3 doesn't have; the tests look dyes (and the client test the redstone torch) up by ID. One client shard of this run died earlier, before building, on Maven Central's rate limit (HTTP 429).

- `python3 scripts/check_repository.py` and `python3 tools/check_mod_data.py`: pass locally and in CI (1367 IDs). The audit's new check holds Java's numbers to `tools/decor19.py`, checks every block is registered, named, textured, dropped and has its recipe, every renderer is registered and every quad model is generated.
- `LaboratoryLarderDiningGameTests` (fourteen tests):
  1. the harness finds a table three blocks below and none over bare floor, waits unpowered, fires on a strong signal, rests through its cooldown, wakes the patient, fires again once rested, and falls without its ceiling;
  2. the console keeps the strongest signal, remembers a full signal after it goes, gives it from its front only (and to a comparator), and a side signal clears it and lets go;
  3. the hand runs while powered and stops;
  4. the cocoon is placed under a ceiling (not without one), holds nine stacks, a comparator reads it, it opens and wriggles, and with its ceiling gone it falls and spills what it held;
  5. egg sacs cover the floor and a wall at once, with their block entity, and two faces drop two clusters;
  6. the drape is placed in four parts, all walk-through, and breaking one brings all down for one drape;
  7. the spools run left to right from the front, a dye recolours the spool it touches for one dye, the others keep theirs, and the colours are saved;
  8. the chair's slide rules (night, empty, someone near, not out already), it seats a player, slides out and back in;
  9. the setting is laid for dinner, tea, a feast and dinner again, nothing collides, flint lights the candle (light 7) and a sneaking hand snuffs it;
  10. the clock's hours (dawn six, noon and midnight twelve), minutes, moon phases and midnight face; placed two tall, it notes the hour, a new hour pulses both halves and strikes, a comparator reads the hour, the hour it saw is saved, and the pulse ends;
  11. witchlight rules (near or powered wakes, lingers, sleeps), a stake asleep at light 3, woken by a player near to 14, dyed green with lime dye, asleep again long after; a lamp-post woken by power at its foot, its light in its lantern; a hanging lantern needs something above;
  12. a silhouette placed by someone looking south faces them, changes through all six figures and back, and falls without ground;
  13. the moon lamp's brightness for all eight phases, placed in four lit parts, a comparator reads tonight's moon, one use darkens it all, and breaking any part brings it down for one lamp;
  14. recipes and loot tables load for all fifteen blocks.
- `LaboratoryLarderDiningClientGameTests`: eight screenshots of the whole scene, the laboratory as the harness fires, the larder, the dining room and the yard, then the dining room, the yard and the laboratory at midnight.

Not run:
- Building with them by hand.
- A two-player dedicated server (one player walking a witchlight path while another watches the lamps wake and sleep; a chair sliding out for one player while another sits).
- This environment can't run a game client interactively.

## World and event applicability
Placed by players only; no worldgen. They work all year, and nothing depends on the Halloween event.

## Rollout and open questions
- Open: whether the Haunted Dining Chair should slide out for creative players (it does for every player now).
- Open: whether witchlights should also wake for tamed animals or villagers walking a path.
- **Fixed 5 October 2026 (shared render fixes):** the Lightning Harness and the Grandfather Clock no longer show holes where a box left a face out (the clock's back wall ends showed through its sides), and the Silk Spool Stack's tinted silk is closed at both ends. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
