# The Witching Season: twenty prop sets and two lairs

Status: approved plan, being built. Batches 17, 18, 19 and 20 are implemented in source ([witchs-workshop.md](witchs-workshop.md), [crypt-and-ossuary.md](crypt-and-ossuary.md), [laboratory-larder-dining.md](laboratory-larder-dining.md) and [pumpkin-night.md](pumpkin-night.md)); the two bosses are not started. Each batch and each boss gets its own feature record, tests and pull request as it is built; this page is the master plan they follow and is updated when one lands.
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("Lets come up with another 20 very detailed well thought out additions to add for the halloween / fall season using the first 3 images for ideas of prop sets and details … Then using the 4th and 5th images lets use those as inspiration for 2 new halloween bosses which should have their own dungeons which are accessible in their own different small pocket dimensions …"), with five reference pictures, then asked that the plan also draw on a set of pocket-dimension and animation mods they uploaded and on the Soulslike Weaponry wiki's boss page.
Owner: @jimbozoomer-byte
Target milestone and tier: the prop sets are Discovery and Workshops tier; the two bosses are Specialization tier (dungeon expeditions). Nothing here gates core progression.
Primary specialty and supported player role: building (the prop sets) and adventuring (the lairs). Farmers, engineers and traders get connections listed per set.

## Contents

1. [What the references gave us](#what-the-references-gave-us)
2. [The order of work](#the-order-of-work)
3. [The twenty prop sets](#the-twenty-prop-sets)
   - [Batch 17: the Witch's Workshop](#batch-17-the-witchs-workshop)
   - [Batch 18: the Crypt and the Ossuary](#batch-18-the-crypt-and-the-ossuary)
   - [Batch 19: the Laboratory, the Larder and the Dining Room](#batch-19-the-laboratory-the-larder-and-the-dining-room)
   - [Batch 20: Pumpkin Night](#batch-20-pumpkin-night)
4. [The lairs: shared rules](#the-lairs-shared-rules)
5. [Boss 1: Vesperine, the Last Reaper, in the Hollow Acre](#boss-1-vesperine-the-last-reaper-in-the-hollow-acre)
6. [Boss 2: Madame Tatterlace, in the Spindle Loft](#boss-2-madame-tatterlace-in-the-spindle-loft)
7. [How it is built](#how-it-is-built)
8. [Connections, balance and anti-farming](#connections-balance-and-anti-farming)
9. [Multiplayer, persistence and seasons](#multiplayer-persistence-and-seasons)
10. [Tests each piece must pass](#tests-each-piece-must-pass)
11. [Open questions for the owner](#open-questions-for-the-owner)

## What the references gave us

**The three prop pictures.** Most of what they show is already in Jugcraft: the coffin, gargoyle, giant bone hand, witch's lantern, bone pile, flying eyeball, pillar candles, spider web, monster's head, specimen jar, candle skull, giant fake spider, bubbling cauldron, witch's broom, hanging ghost, black cat figure, grasping hands, headstones, scarecrow and the heirloom pumpkins (batches 1–16 and the graveyard packs). So each set below takes one thing from a picture and builds something new out of it, with a job to do, instead of drawing it again:

| Picture element | Becomes |
| --- | --- |
| Horned green skull cauldron (1) | 1. Horned Skull Cauldron: a potion cauldron that shares a bottle's effect round a group |
| Iron chandelier of purple candles (1) | 2. Wrought-Iron Candelabra: four fittings, six waxes, four flame colours |
| Broom (3) | 3. Enchanted Broom and Dustpan: a broom that sweeps up dropped items |
| Jars of eyeballs and a brain (3) | 4. Cabinet of Curiosities and 5. Oddity Jars (one is a redstone clock) |
| Iron-bound coffin chest (1) | 6. Iron-Bound Coffin (lockable) and the Coffin Wardrobe (armour swap) |
| Stone sarcophagus (2) | 7. Stone Sarcophagus with a sliding lid and carved effigies |
| Bones, skull and crossbones (1, 3) | 8. Bone Throne and the ossuary parlour (an enchanting bookcase) |
| Giant skeleton hand (1) | 9. The Buried Colossus: a giant's skull, ribs, spine and femur |
| Winged gargoyles (1, 3) | 10. Gargoyle Sentinels: a hostile-mob alarm, a rainspout, a weather vane-finial |
| Monster's head, green hands (3) | 11. The Reanimation Rig: a lightning harness, a brain-vat memory cell, a crawling hand |
| Purple spider (3) | 12. The Spider's Larder: cocoons, egg sacs, web drapes, silk spools |
| Ghost (3) | 13. The Poltergeist's Dinner Party and a grandfather clock |
| Purple lantern (1) | 14. Witchlight Lantern Path: lamps that wake as you walk |
| Black cat silhouettes (2) | 15. Yard Silhouettes and the Harvest Moon Lamp (shows the moon phase) |
| Red and green pumpkins (2) | 16. Red Kuri and Kabocha pumpkins |
| Pumpkins on display (2) | 17. The Farm Stand: an honesty-box shop paid in Jugs |
| Curling pumpkin stems (2) | 18. Pumpkin Vine Garlands strung between hooks |
| Scarecrow in a tattered cloak (1) | 19. The Harvest Effigy: a giant straw man burned on a fall night |
| Jack-o'-lanterns (1, 2) | 20. The Singing Pumpkin Choir |

**The two boss pictures** are mood, not designs to copy. Picture 4 (a dark scythe-bearer with long black hair, red eyes and two huge floating skulls) becomes Vesperine, the Last Reaper, attended by two skulls, Dirge and Requiem. Picture 5 (a spider with gold cuffs, a red-and-gold headdress, a purple gem, giant thread spools and a lace doily) becomes Madame Tatterlace, a spider seamstress fought on a giant doily. Both are drawn from scratch in Jugcraft's own style.

**The uploaded mods** (studied for how they work; none is added as a dependency and no code or asset is copied):

| Mod (version, licence) | How it works | What Jugcraft takes from it |
| --- | --- | --- |
| Pocket Dimension by Renwixx (1.0.0 for 1.21.11, CC0) | One data-pack dimension: a flat generator with no layers over `minecraft:the_void`, and a dimension type using the newer `attributes` format (`gameplay/bed_rule` never). Rooms are placed from a structure template 1,000 blocks apart along X, numbered by a saved counter. Saved data holds each player's room and the exact spot (dimension, position, yaw, pitch) they came from. An exit block sends you back. Fabric events tidy up on respawn, dimension change and disconnect. Walls are bedrock. | The whole shape of a lair: one void dimension per lair, instance slots far apart, a saved return point per player, an exit fixture, clean-up on respawn, change of dimension and disconnect. |
| Pocket Dimension by RafalBerezin (1.4.0, MIT) | A data pack: functions and advancements do the teleporting, with a privacy check before entry. | The privacy check: nobody is pulled into a lair unasked. Entering is always the player's own action. |
| pocket_dimension 1.0.0 for 1.20.1 (Forge, MIT) | A dimension with an indestructible floor block, protected through the `wither_immune` and `dragon_immune` tags. | Lair-only fixtures are also in those tags, as a second line of defence. |
| GeckoLib 4.9.3 for 1.21.1 (MIT) | An animation engine: Blockbench geometry, keyframed animations with easings, controllers per entity, and server-triggered animations sent by packet. | The idea of named, triggered, keyframed attack animations. Jugcraft gets this without a dependency from Minecraft's own keyframe animation system (the one the Warden and the Breeze use): attack animations are keyframe definitions, and the server starts them with entity events. A 1.21.1 jar can't run on 26.3 anyway, and a new required dependency would need a platform PR. |
| AttributeFix 21.1.3 (LGPL 2.1), with Bookshelf and Prickle | Raises vanilla's attribute caps (for example the 1,024 maximum health). | Not needed: the bosses' health stays under vanilla's cap even scaled for four players. |
| Ranged Weapon API 3.0.0 (MIT) | Bows and crossbows defined by attributes (damage, pull time, velocity). | Not needed: neither boss drops a bow. |
| Fabric API 0.116.17 for 1.21.1 | The 1.21.1 API. | Jugcraft is pinned to Fabric API 0.161.0+26.3 and stays there. |

**The Soulslike Weaponry boss page** shows what makes a boss fight memorable, and the plan follows it: every attack has a name, a wind-up you can read and a number; the fights have phases that change the rules (at half health, or when a partner falls); each boss has one rule of its own (there: weapon-adaptive armour, projectile reflection; here: Vesperine's guard and Tatterlace's unravelling floor); summoning happens at an altar with a crafted item, and the boss drops part of what summons it again; health, damage and loot are configurable by the server. Unlike several of those bosses, nothing here breaks or changes the world outside its lair.

## The order of work

As the owner asked, the props come first, five sets to a pull request, then the bosses one at a time:

| Step | Pull request | Contents |
| --- | --- | --- |
| 1 | Batch 17 | Sets 1–5 (the Witch's Workshop) and this plan |
| 2 | Batch 18 | Sets 6–10 (the Crypt and the Ossuary) |
| 3 | Batch 19 | Sets 11–15 (the Laboratory, the Larder and the Dining Room) |
| 4 | Batch 20 | Sets 16–20 (Pumpkin Night) |
| 5 | Boss 1 | The lair framework, the Hollow Acre and Vesperine |
| 6 | Boss 2 | The Spindle Loft and Madame Tatterlace (reusing the framework) |

The props come first for a second reason: the lairs are furnished with them. The Hollow Acre's chapel has the Bone Throne, candelabra, gargoyles and witchlights; the Spindle Loft has cocoons, egg sacs, web drapes and silk spools.

Every set keeps to the house rules: original art painted by code (64 × 64 or larger, never traced), stable `jugcraft:` IDs, recipes in data, server-side behaviour, a client-side renderer only where something moves, bounded work per tick, and every recipe following the agriculture feature switch.

## The twenty prop sets

Each set lists its pieces, what they do, what they cost, and the checks that will prove it. Numbers are the starting values; balance can be tuned in review.

### Batch 17: the Witch's Workshop

Built: [witchs-workshop.md](witchs-workshop.md) records it as made, with the few changes from this plan (sixteen candles on the chandelier, a curated list of what floats, the recipes settled).

#### 1. The Horned Skull Cauldron
*From picture 1's green cauldron with a horned skull, and picture 3's cauldron with curling green fumes.*

Pieces:
- **Horned Skull Cauldron** (a cauldron, two iron ingots, a bone block and two goat horns, or four bones in place of the horns): a squat black-iron pot on four clawed feet with two riveted bands. A bleached ram's skull is bolted to its front, its horns curling out past the rim. The skull's eye sockets glow in the colour of whatever is inside.
- **Ember Bed** (four cobblestone round a charcoal or coal): a low ring of sooty stones holding glowing embers (light 9). It crackles, and it counts as a heat source for this cauldron, the Bubbling Cauldron and the Cooking Pot (the `jugcraft:heat_sources` tag).
- **Brew Ladle** (two sticks and a bowl): a long wooden ladle.

What it does:
1. **It holds potions, not just water.** A water bucket fills it three deep; a water bottle adds one level. A potion goes in if the cauldron is empty or already holds that same potion, up to three bottles, and the brew takes that potion's colour. A glass bottle draws one level back out as the same potion. Nothing is gained or lost: what goes in comes out.
2. **Waft it.** Stir a heated potion with the Brew Ladle and its fumes roll out: up to four players within three blocks each get the potion's effect at a quarter of its duration, for one level. Four friends share one bottle; nobody gains more than the bottle held. (Instant effects such as Healing are not wafted.)
3. **It bubbles over heat** (fire, campfire, magma, lava or an Ember Bed underneath): the surface heaves and pops, and three translucent fume ribbons twist up out of it, curling and fading a block and a half up, in the brew's colour. Now and then a fat drop of brew spits over the rim and splashes.
4. **Things float in it.** Use any other item on it while it holds liquid and one of that item drops in and floats, half-sunk, bobbing and turning slowly (up to three: an apple, a bone, an eye, a mushroom). The ladle fishes the top one out; breaking the cauldron gives them back.
5. Light: 6 plus one per level while it holds a potion, two more while heated.

Checks: filling, drawing and mixing rules (no gain, wrong potion refused); wafting reaches at most four players, never instant effects, uses one level; floating items kept and returned; heat detection; a client screenshot of three cauldrons in three colours with floating ingredients and fumes.

#### 2. The Wrought-Iron Candelabra
*From picture 1's branching iron chandelier of purple candles.*

Pieces (all wrought iron with drip-pans and scrolled arms):
- **Floor Candelabrum** (two blocks tall): a tripod of scroll feet, a twisted stem and seven candles in two tiers.
- **Table Candelabrum**: three arms on a weighted foot.
- **Wall Girandole**: a scrolled back-plate with a small black mirror and three arms.
- **Branching Chandelier** (hangs under a block or a chain): a 3 × 3 tree of branching arms in three tiers carrying thirteen candles, hung with little hooks and chains. It is different from the existing Haunted Chandelier, which is a single ring.

What they do:
1. **Choose the wax**: use a dye to recolour every candle: ivory (bone meal or white dye), black, purple, green, orange or red.
2. **Choose the flame**: ordinary (the default), soul (use soul sand or soul soil), witchfire purple (an amethyst shard) or ghostfire green (a glow ink sac). The flames are drawn by the renderer, so they can be any colour, and flicker each on its own.
3. **Light and snuff**: flint and steel, a fire charge or a burning arrow lights them; an empty hand snuffs them. A redstone signal lights them while it lasts.
4. Light grows with the candles burning: table and girandole 9, floor 14, chandelier 15.
5. **Wax drips** run down the candles and arms the longer they burn, in four stages over three in-game days. Shears scrape them clean. The drips are only looks: they give no wax, so a burning candle is not a wax farm.

Checks: the light per fitting, the six waxes and four flames as block state, redstone lighting, drips advance only while lit and shears clear them with no drop, the chandelier falls when what holds it goes; a night screenshot of all four fittings in four flame colours.

#### 3. The Enchanted Broom and Dustpan
*From picture 3's broom.*

Pieces:
- **Enchanted Broom** (a Witch's Broom rubbed with Flying Ointment from the hex brews): a besom of bound twigs that stands on its bristles.
- **Dustpan** (an iron ingot and a copper ingot): a little tin pan with nine slots that hoppers and pipes can empty.
- **Broom Rack** (planks and iron nuggets): a wall rack with three pegs. It holds and shows any three brooms: Witch's Brooms, Enchanted Brooms or Flying Broomsticks.

What it does:
1. **It sweeps.** Once a second the broom looks for dropped items within four blocks (a 9 × 3 × 9 box). It leans and sweeps toward them, and the items slide across the floor to the nearest Dustpan in that box, which takes them in. With no Dustpan near, it sweeps them into one heap at its feet.
2. **One charge of ointment lasts three in-game days**; then the broom stands still until it is rubbed again. Its bristles glow faintly while charged.
3. Limits: at most sixteen item stacks moved per sweep, nothing taken from pressure plates' tops or from other containers, and it only moves item entities (never experience, mobs or players).

Checks: items reach the Dustpan, the per-sweep limit, nothing moves once uncharged, hoppers empty the pan, the rack shows each broom type; a screenshot of a swept floor.

#### 4. The Cabinet of Curiosities
*From picture 3's jars, and the collector's shelves they belong on.*

Pieces:
- **Curiosity Cabinet** (two blocks tall; planks, glass and gold nuggets): a carved mahogany cabinet with glazed doors, a moth-and-skull crest and brass hinges. It has three shelves of three places.
- **Bell Jar** (glass and a wooden slab): a glass dome on a turned plinth.
- **Moth Display Case** (a frame, glass and a dye): a shallow glazed wall case of pinned moths. Use it to change the specimens: luna moths, death's-head hawkmoths or atlas moths.

What they do:
1. **The cabinet shows nine things.** Aim at a shelf place and use an item to put it there, or use an empty hand to take it back, as with a chiseled bookshelf. The doors swing open while you reach in. A comparator reads how full it is.
2. **The bell jar shows one thing** under glass, turning slowly on its plinth. Blocks show as little blocks.
3. **At night the pinned moths come alive** and slowly open and close their wings.

Checks: per-place insertion and removal, comparator, items kept on break; a screenshot of a filled cabinet beside a bell jar and a moth case.

#### 5. The Oddity Jars
*From picture 3's jar of eyeballs and brain jar. These are new specimens with their own behaviour; the Specimen Jar's eye, tentacle, pumpkin and brain stay as they are.*

Pieces (glass jars with iron lids and paper labels):
- **Jar of Eyeballs** (glass and nine spider eyes): a jar packed with eyeballs in murky fluid. Every iris rolls to follow the nearest player within eight blocks, each a little behind the others.
- **Beating Heart Jar** (glass, rotten flesh, redstone and a brass nugget): a heart on a brass stand with a little dial. **It is a redstone clock.** It beats at 60, 80, 100 or 120 beats a minute (use it to change), and each beat sends a two-tick pulse of strength 15. A redstone signal into its base stops it.
- **Bat in a Jar** (glass, phantom membrane and leather): a bat hanging asleep. Come within three blocks and it wakes and flutters round the jar, squeaking, then settles after five seconds.
- **Two-Headed Snake Jar** (glass, string and a slime ball): a coiled two-headed snake in green fluid. Both heads turn slowly, and they flick their tongues when the jar is used.
- **Hand in a Jar** (glass, rotten flesh and a bone): a floating hand drumming its fingers. Powered, it points at the nearest player.

Checks: the heart's tempo and pulse timing and its stop input; the eyes' and hand's targeting stays within eight blocks; the bat wakes only within three; a screenshot of all five on a shelf.

### Batch 18: the Crypt and the Ossuary

#### 6. The Iron-Bound Coffin and the Coffin Wardrobe
*From picture 1's coffin chest with iron straps. The existing Coffin (a 27-slot chest you can sleep in) stays as it is.*

Pieces:
- **Iron-Bound Coffin** (a Coffin and four iron ingots): two blocks long, of black wood with riveted iron bands, iron corners, a hasp and a padlock, lined in purple velvet. **54 slots**, the size of a double chest. The lid swings up with a rattle of chain.
- **Skeleton Key** (an iron ingot and an iron nugget): an ornate key cut to its own pattern the first time it is used. A cut key and a blank key craft into two of the same key.
- **Coffin Wardrobe** (a Coffin, a glass pane and an armour stand): an upright coffin two blocks tall with a window in its lid. A skeleton mannequin inside wears the armour it holds.

What they do:
1. **Lock it.** Sneak-use the coffin holding a key to lock it to that key, and again to unlock it. Locked, only someone holding a matching key can open it, and hoppers and pipes can't take from it. As with vanilla's locks, a lock stops opening, not breaking; land claims protect against breaking.
2. **Swap your armour.** Use the wardrobe to swap the four armour pieces you wear with the four it holds, all at once, as an armour stand does. Pieces with Curse of Binding stay on.

Checks: 54-slot storage on both halves, lock and unlock, wrong key refused, automation refused while locked, key copying; armour swap, binding respected; a screenshot of an open coffin and a dressed wardrobe.

#### 7. The Stone Sarcophagus
*From picture 2's stone sarcophagus with a carved skull.*

- **Stone Sarcophagus** (two blocks long; stone bricks, polished deepslate or polished blackstone, with bones): a tomb-chest with carved panels and a skull boss on its front, under a heavy lid with a raised border.
- **Effigies**: the Stonemason's Chisel recarves the lid as a plain lid (as in the picture), a knight with his sword on his breast, a lady with folded hands and a lily, or a skull and crossed bones.
- **Use it** and the lid grinds aside and tilts, showing a skeleton lying within and 27 slots of storage.
- **At night, closed, it knocks**: with a player within six blocks there is a small chance each second (at most once a minute) of three slow knocks from inside, and the lid shudders.

Checks: the three stones and four lids, storage, the knock's limits; a screenshot of a crypt row.

#### 8. The Bone Throne and the Ossuary Parlour
*From picture 1's bones and picture 3's skull and crossbones.*

- **Bone Throne** (two blocks tall; bone blocks, bones, red wool and a skeleton skull): a seat framed in thigh bones, armrests ending in skulls, a red velvet cushion, and a fan of ribs behind topped by a horned skull. **Sit in it**; while someone sits at night, the crest's eye sockets glow red.
- **Ribcage Bookcase** (bones and planks): a ribcage holding six books between its ribs. It takes books, written books and enchanted books like a chiseled bookshelf, shows them, and **counts as a bookshelf for an enchanting table**. A comparator reads the last place used.
- **Skull Footstool**: a skull on crossed bones under a velvet cushion; a low seat.
- **Vertebra Floor Lamp** (two blocks tall): a column of vertebrae under a skull shade lit from inside (light 13). Use it or power it to switch it.

Checks: seating, the crest's glow, enchanting power, book slots and comparator, the lamp's switching; a screenshot of a parlour.

#### 9. The Buried Colossus
*From picture 1's giant skeleton hand. The existing Giant Bone Hand becomes one part of a giant's whole skeleton.*

- **Colossal Skull** (2 × 2 × 2; nine bone blocks): a giant's cracked skull, meant to be half-sunk in the ground. At night its eye sockets glow faintly blue. **Give it a redstone signal and its jaw drops open** with a grinding of bone, and wind moans through it.
- **Colossal Rib** (two blocks tall): a rib curving up and over. Set two facing each other and they meet as an arch you can walk under.
- **Colossal Vertebra**: a vertebra with its spine and wings that lines up along any axis, so a row of them is a spine.
- **Colossal Femur** (two blocks long): a thigh bone lying in the grass.

Checks: the multiblock placing and breaking as one, the jaw's redstone, rib pairs meeting, vertebra axes; a screenshot of a half-buried giant with the existing bone hand.

#### 10. Gargoyle Sentinels
*From the winged gargoyles of pictures 1 and 3. The existing Gargoyle stays a statue.*

- **Gargoyle Sentinel** (stone bricks, an ender eye and redstone): a gargoyle crouched on a pedestal, wings folded. **It watches for hostile mobs**: its head turns to the nearest within 16 blocks, its eyes glow brighter as it comes, and it gives a redstone signal from 1 (16 blocks away) to 15 (two or fewer). It looks every half second.
- **Gargoyle Rainspout** (stone bricks and a copper ingot): a grotesque head on a long gutter-spout, for a wall. **When it rains on it, it pours a stream**, and a cauldron (vanilla's, the Bubbling Cauldron or the Horned Skull Cauldron) up to four blocks below fills one level every ten seconds, as under dripstone.
- **Chimera Finial** (stone, a stone slab and a bone): a winged chimera on a ball finial for a roof ridge. **It is a weather sensor**: signal 7 while it rains and 15 in a thunderstorm, and in a storm it spreads its wings.

Checks: the sentinel's range, signal and look rate, players and passive mobs ignored; the spout fills only in rain and only cauldrons; the finial's signals; a screenshot of a church roof in rain.

### Batch 19: the Laboratory, the Larder and the Dining Room

#### 11. The Reanimation Rig
*From picture 3's monster's head and green hands. It works with the existing Lab Table and Tesla Coil.*

- **Lightning Harness** (copper and brass ingots, chain and a lightning rod): a ceiling cage of copper coils with two brass electrode arms on chains. **It fires** on a strong redstone pulse (13 or more), when a running Tesla Coil within eight blocks arcs to it, or when lightning strikes within four blocks above it. Arcs crack between its electrodes and down to what lies below.
- **It wakes the patient.** If a Lab Table lies under it (within four blocks), the patient under the sheet sits bolt upright for five seconds, arms out, groaning, with its eyes flashing, then sinks back. (This adds a waking pose to the Lab Table without changing what it is.)
- **Brain-Vat Console** (brass, glass, redstone and a comparator): a brass console with a brain in a glass vat, two dials and a toggle. **It is an analogue memory cell**: it remembers the strongest signal fed into its back and keeps giving it out of its front until a pulse on either side clears it. The brain sparks and glows as brightly as what it remembers, and a comparator reads it too.
- **Crawling Hand** (rotten flesh and a bone): a severed hand that drums its fingers. Powered, it scuttles round the top of its block.

Checks: the harness's three triggers, the patient's pose timing, the console's hold and clear, the hand's powered walk; a screenshot of a laboratory mid-spark.

#### 12. The Spider's Larder
*From picture 3's purple spider. The Spindle Loft uses all of these.*

- **Silk Cocoon** (string and a wool): a wrapped, body-shaped bundle hanging on a thread from a ceiling, swaying. **A 9-slot larder**: open it and it wriggles and groans faintly. Now and then at night it twitches by itself.
- **Egg Sac Cluster** (string and a spider eye): glistening white sacs on floors, walls or ceilings (like glow lichen). They pulse slowly; at night, now and then, a few tiny spiderlings skitter out and vanish. They are drawn by the renderer: no mobs are spawned.
- **Web Drape** (four cobwebs): a great sagging web curtain, two blocks by two, glinting with dew. Walking through it slows you, less than a cobweb does.
- **Silk Spool Stack** (planks and three dyed wools): three tall wooden spools wound with silk. Use a dye on a spool to rewind it in that colour.

Checks: cocoon storage and its wriggle, egg sac faces, the drape's slowing, spool colours; a screenshot of a larder corner.

#### 13. The Poltergeist's Dinner Party
*From picture 3's ghost: the party nobody living was invited to.*

- **Haunted Dining Chair** (planks and a red wool): a high-backed gothic chair you can sit in. At night, when a player comes within two blocks, it slides out from the table toward them and later slides back.
- **Floating Table Setting** (on top of any block): plates, a goblet, cutlery and a candlestick hovering a hand's breadth above the table, each bobbing out of step. At night the goblet now and then tips over and rights itself with a clink. Use it to change the setting to a tea service (the pot pours into a cup now and then) or a feast platter. The candlestick can be lit.
- **Grandfather Clock** (two blocks tall; planks, a clock, gold and glass): a carved longcase clock. **Its hands show the time of day**, its arched dial shows **the moon's phase**, and its pendulum swings. It chimes each hour, and at midnight it strikes twelve while a pale face looks out through the glass. It sends a redstone pulse on every hour, and a comparator reads the hour (1 to 12).

Checks: the chair's slide trigger, the setting's three variants, the clock's hour and phase readings against the world clock and the pulse on the hour; a night screenshot of a laid table.

#### 14. The Witchlight Lantern Path
*From picture 1's purple lantern.*

- **Witchlight Lamp-Post** (two blocks tall; iron, purple stained glass and gold): a crooked iron post with a shepherd's-crook arm holding a purple-glass lantern with a gold cap.
- **Witchlight Path Stake**: the same lantern on a knee-high stake.
- **Hanging Witchlight**: the lantern on a chain, for a ceiling.

What they do:
1. **They wake as you come.** Asleep, each glows dimly (light 3) with a single wisp turning inside. When a player comes within six blocks it flares awake (light 14) with a soft rising chime. It stays awake ten seconds after the last player leaves, then fades over two. Walk a path of them and the lamps ahead light while those behind go dark.
2. Use a dye on the glass to change its colour: purple, green, orange, blue or red.
3. A redstone signal keeps one awake.
4. Each lamp checks for players twice a second, and only nearby ones.

Checks: wake and fade timings, the redstone hold, the dye colours; a screenshot of a path, half awake.

#### 15. Yard Silhouettes and the Harvest Moon Lamp
*From picture 2's black cat silhouettes.*

- **Yard Silhouette** (planks and black dye): a black-painted cutout on a stake, a block and a half tall, with eyes that glow yellow at night. Use it to change the figure: an arched cat, a prowling cat, a witch on her broom, a flock of bats, a howling wolf or a crow. It turns in sixteen directions.
- **Harvest Moon Lamp** (2 × 2; glowstone, white wool and gold): a great glowing moon on a stand or a wall (light 15). **Its face shows tonight's moon phase**, from new to full. A comparator reads the phase: 15 at full moon down to 0 at new. That matters on werewolf nights.

Checks: the six figures, the eyes' glow only at night, the moon's phase and comparator against the world; a night screenshot of silhouettes against the moon.

### Batch 20: Pumpkin Night

#### 16. Red Kuri and Kabocha pumpkins
*From picture 2's red and green pumpkins.*

- **Red Kuri** (a red-orange, teardrop-shaped pumpkin) and **Kabocha** (squat and dark green, flecked paler) join the White, Jarrahdale and Cinderella heirlooms. Seeds come the same way as theirs, from wild plants and the Halloween Peddler. They grow on stems like squash and give four seeds each.
- **They carve.** The Carving Knife carves them into their own hand-carved blocks. Lit, a Red Kuri glows a deeper orange and a Kabocha glows greenish gold.
- **Cooking**: Red Kuri Soup and Kabocha Tempura in the Cooking Pot. Both make pumpkin pie as the other heirlooms do. They count as pumpkins wherever the `jugcraft:scarecrow_heads` and trebuchet tags take pumpkins.

Checks: growth and seeds, carving into their own blocks, recipes, tags; a screenshot of a patch.

#### 17. The Farm Stand
*From picture 2's pumpkins on show: a stall to sell your harvest to your neighbours.*

- **Farm Stand** (two blocks wide; planks, an orange and a white wool, a chest and a sign): a wooden stall under a striped awning, with a slanted shelf of six crates, a chalkboard and an honesty box.

What it does:
1. **Its owner stocks it.** The player who placed it sneak-uses it to fill six crates (a stack each) and chalk a price for each, in **Jugs**, the walled town's currency, so it adds no new money.
2. **Anyone buys from it.** Use it to see the crates and prices (the crates show their goods and the chalkboard shows the prices); buying takes the price from the buyer's Jugs and gives it to the owner, even while the owner is offline.
3. **The server checks everything**: who owns it, that the buyer is within reach and has the Jugs, that the goods are still there, that prices stay between 1 and the town's maximum balance, and that the owner's balance won't pass that maximum (then the sale is refused). One request at a time per stand.
4. Only the owner can break it; whatever it holds drops then.

Checks: stocking only by the owner, buying moves goods and Jugs exactly once, refusals (no Jugs, empty crate, owner at the cap, out of reach), offline owner paid; a two-client playtest is required before this is called done.

#### 18. Pumpkin Vine Garlands
*From picture 2's curling pumpkin stems.*

- **Pumpkin Vine Garland** (vines, a pumpkin and string) and **Autumn Leaf Garland** (leaves of maple, aspen and larch, and string): strands hung between the existing **String Light Hooks**, exactly as the Jack-o'-Lantern String Lights are, up to sixteen blocks.
- The vine garland sags in a curling twist of vine with broad leaves, little pumpkins and gourds every few blocks; the leaf garland in red, orange and gold leaves on twine. Both carry tiny warm bulbs that light whenever a hook is lit (a redstone signal or a trickle of power), as the string lights do.

Checks: hung, chained and taken down like the string lights; a screenshot of a porch.

#### 19. The Harvest Effigy
*From picture 1's scarecrow in a tattered green cloak.*

- **Harvest Effigy** (three blocks tall; three hay bales, sticks and string): a giant wicker-and-straw man with arms outstretched on a cross-pole, sheaves of corn at his feet and a tattered cloak (dye it). Like the Scarecrow, he wears any carved pumpkin as his head.

What it does:
1. **Burn him on a fall night.** At night, light his base with flint and steel or a torch. He burns for thirty seconds in a tall, roaring blaze that climbs to his head, sparks spiralling up, while nearby crows scatter cawing.
2. **Harvest Cheer**: every player within sixteen blocks gets Regeneration I for 30 seconds and Luck for five minutes, once per player per night.
3. He collapses into **Effigy Ashes**, which give two **Hearth Ash** when shovelled. Hearth Ash works as bone meal. Three hay bales are worth far more than two bone meal, so this is a loss, not a farm.
4. The blaze is drawn by the renderer: no fire blocks are placed, nothing catches, and rain puts him out.

Checks: night only, once-per-player cheer, ashes and their yield, no fire spread, rain stops it; a night screenshot mid-blaze.

#### 20. The Singing Pumpkin Choir
*From the jack-o'-lanterns of pictures 1 and 2.*

- **Singing Pumpkins** in four voices: Bass (a wide, heavy-browed face), Tenor, Alto and Soprano (a small, bright face). Each is a carved jack-o'-lantern with a hinged mouth and rolling eyes, lit from inside.

What they do:
1. **They sing.** Like a note block, each is tuned by use (two octaves) and sings its note on a redstone pulse: its mouth opens round in time with the note and its eyes widen.
2. Voices are new sounds made by Jugcraft's tools (sung vowels, synthesised, original). If that proves impractical, the fallback uses vanilla note-block instruments, one per voice, and says so.
3. Wire a row to a sequencer and they sing a tune together.

Checks: tuning across 25 pitches, the pulse trigger, the mouth's timing; a screenshot of a choir mid-song.

## The lairs: shared rules

Both bosses live in their own small pocket dimension. Everything in this section is shared, built with Boss 1 and reused by Boss 2.

**One dimension per lair.** `jugcraft:hollow_acre` and `jugcraft:spindle_loft`, each a void: a flat generator with no layers, its own biome with no natural spawns, a fixed time, no weather, no beds or respawn anchors (the `gameplay/bed_rule` attribute set to never), and their own sky, fog and music colours from the 26.3 dimension type's `attributes`. The dimension IDs are permanent once released.

**Instances.** A ritual opens an instance: a slot along the X axis, 1,024 blocks from the next. The lair is placed fresh into the slot from a structure template each time, so whatever a fight did is put right. At most eight instances per lair run at once (configurable); a ritual with none free fails, says so and uses nothing. At most four players per instance.

**Getting in.** Always the player's own choice:
1. The ritual opens a **gate** at its site for 60 seconds, and the player who performed it goes through at once.
2. Anyone else who uses the gate in that time follows them, while there is room.
3. Nobody is ever pulled in by standing nearby.

**Getting out.**
- The lair's arrival point has an exit fixture (a gate, a spool) that returns you at any time, in or out of the fight, so retreat is always possible.
- When the boss falls, a second exit opens in the arena.
- Everyone returns to exactly where they stood when they entered (dimension, position and facing), as the pocket-dimension mods do. If that place is gone (another mod's dimension removed, say), they go to their respawn point.

**Players cannot build or break anything in a lair.** The server enforces it:
1. **Breaking**: refused for everyone, before the block cracks. The only exception is an operator in creative mode, for repairs.
2. **Placing and using items on blocks**: while in a lair a player loses the build ability (the rule vanilla uses for Adventure mode). That stops blocks, buckets, flint and steel, bone meal, spawn eggs, end crystals, armour stands, item frames and paintings. Leaving restores it.
3. **Using blocks**: refused, except the lair's own fixtures (exits, the loot coffer).
4. **No world damage from the fight**: the bosses' attacks never break or place lasting blocks. The one exception, Tatterlace unravelling her own floor, is temporary and put back. Explosions there break nothing, fire can't be lit, and no fluids are in the lair.
5. **The fixtures can't be destroyed**: lair-only blocks can't be broken and are also in the `wither_immune` and `dragon_immune` block tags.
6. **The edges**: fly or fall off the island and the mist throws you back to the arrival point, for 4 damage, never below half a heart. Ender pearls and chorus fruit work within the bounds.

**Dying in a lair costs no belongings.** What a player drops when they die in a lair (items and experience) is gathered up at once as their **Grave Goods** and handed back when they respawn, in their own world. This is kept in saved data until they claim it, across logouts and restarts. Without it, a reset instance would destroy the items. Respawning always happens outside the lair.

**When an instance ends.** An instance closes:
- when its boss is dead and every player has left;
- when nobody has been inside for 30 seconds (the boss then leaves too, at full health, and the slot is freed);
- when the server restarts (every instance resets, and any boss found in a closed slot leaves).

A player who logs in inside a closed instance is sent back to their return point. Items left lying in a lair are lost when its slot is next used; the exit warns of this once.

**Respawnable.** Every ritual starts a fresh fight with a fresh boss, as often as the ritual is paid for. There is no cooldown: the ritual's cost is the limit. The boss also drops part of what summons it again (see each boss).

**Multiplayer scaling and loot.**
- The boss's health is set when it wakes: base × (1 + 0.5 per extra player), at most 2.5 × for four. That stays under vanilla's 1,024 maximum health, so no attribute mod is needed. Its damage doesn't change.
- **Every participant gets their own loot.** A participant dealt at least 5% of the boss's health, or was in the arena for at least 30 seconds of the fight. Each one's loot drops at their own feet, and only they can pick it up for the first minute.
- Experience is shared out the same way.

**Recovery.** If every player leaves or dies, the boss goes back to its throne and heals after 30 seconds. A player leaving mid-fight doesn't make it easier: health set at waking stays.

**Commands.**
- `/jugcraft lair leave` sends you home from any lair: the stranded-player escape.
- Operators have `/jugcraft lair list` and `/jugcraft lair close <lair> <slot>`.

**Configuration** (`config/jugcraft-lairs.json`, read by the server; the defaults are the numbers above):
- instances per lair;
- party size;
- gate seconds;
- health and damage multipliers;
- whether rituals work outside the Halloween event;
- whether the event adds its bonus loot.

**No chunk loaders.** A lair's chunks are loaded only while players stand in them, or for the moment it takes to place an instance. Nothing is force-loaded.

## Boss 1: Vesperine, the Last Reaper, in the Hollow Acre

*From picture 4.* A tall, pale reaper in layered black armour and torn robes, black hair streaming as if underwater, eyes red, carrying a great scythe with a moon-pale blade. Two huge dark skulls, **Dirge** and **Requiem**, drift at her shoulders trailing black wisps. She is the harvest's last reaper, and she has come to reap the reapers.

### The Hollow Acre

A floating island of black earth about 64 blocks across, in a starless night under a vast harvest moon:
- **The lych gate**: you arrive here on the south edge, and it is also the way home.
- **The black wheat**: a path between a field of blighted wheat and leaning headstones.
- **The Mown Circle**: the arena, a ring of cut stubble 40 blocks across, edged by soul-fire braziers.
- **The bone chapel**: a ruin at the north end, with an ossuary wall, gargoyles and a broken rose window. Vesperine waits seated on a Bone Throne and rises when someone steps into the circle.

It is furnished with Jugcraft's graveyard and batch 17–19 props, plus a few lair-only blocks (blighted soil, black wheat, the moon). The sky is near-black, the fog plum, and the music the mournful kind.

### The ritual: the Last Rites (no item to throw, a grave to prepare)
1. **The grave**: any Jugcraft headstone (the `jugcraft:headstones` tag), in the Overworld, at night.
2. **The candles**: at least four lit candles within four blocks of it. Any candles count: vanilla's, pillar candles, aura candles.
3. **The wreath**: lay a **Mourning Wreath** on the headstone. It is made of four mourning flowers (any of black rose, funeral lily, spider lily, asphodel, snowdrop, lily of the valley or wither rose) round a vine, so vanilla flowers will do.
4. **The bell**: ring a **Death Knell** (a gold ingot, an iron nugget and a bone; a handbell that is not used up) beside the grave.

Then the candles flare blue, the wreath is consumed, and a gate of grey mist stands open over the grave for 60 seconds. If any step is missing, the bell's chime says which (no candles, no wreath, not night), and nothing is used up.

### The fight

Vesperine: 400 health (scaled for the party), armour 10, immune to knockback, fall damage, Wither and drowning; she glides rather than walks. Dirge and Requiem: 80 health each.

**Her rule: the twin skulls guard her.** While both skulls live, she takes half damage, and a faint double halo shows it. Kill one and the halo cracks; kill both and she takes full damage. This is the fight's target-switching puzzle.

**Phase 1, "The Reaping"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Reaping Arc | She draws the scythe back high (0.6 s) and its blade glints red | A 270° sweep, 4.5 blocks: 14 damage and Wither I for 3 s |
| Harvest Lunge | She crouches, robes flaring (0.5 s) | She dashes up to 8 blocks at her target through a trail of black petals: 10 damage |
| Grief Bolt (skulls) | A skull's jaw opens and its eyes flare | Each skull fires a slow homing bolt every 4 s: 6 damage and Slowness I. **Hit a bolt** and it flies back at whoever sent it |
| Scythe Throw | She hefts the scythe overhand (0.7 s) | It spins out up to 16 blocks and back: 10 damage each way. She is **unarmed and takes 25% more damage** until it returns |
| Grave Call | Clawing hands break the soil at three spots (1.5 s) | Three thralls (skeletons with soul-flame eyes) climb out. At most four at a time; they crumble when she falls |

**At half health, "The Last Toll"**: she rises above the circle; a bell tolls; the moon turns red and every player gets Darkness for 3 s. Any slain skull re-forms at half its health. The fight changes.

**Phase 2, "The Reaping Moon"** (half health to none):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Twin Beam | A thin red line traces across the ground for 1.5 s | Both skulls pour a soul-fire beam along it, sweeping: 4 damage every quarter second while you stand in it |
| Crop Circles | Rings of pale light open under three players | 1.5 s later each ring erupts in soul flame: 12 damage |
| Shadow Step | A puff of black smoke and a whisper behind the player who last hurt her | She appears behind that player and makes a Reaping Arc |
| Death's Harvest (once, at a quarter health) | She rises toward the moon; everything darkens; souls stream out of the black wheat toward her | For 6 s, each soul that reaches her heals her 2% of her health. Strike the souls down, or light the four standing braziers that ward the circle. Then she slams down: 16 damage at the centre, less with distance, none past 10 blocks |

The arena keeps her: pulled more than 32 blocks from the circle's centre, she glides back. She can't ride, be leashed or be pushed into anything.

### Her loot (per participant)
- **Reaper's Shade** (3–6): a wisp of night-black cloth. It is a material for the reaper's costume and for **Shade Wreaths**, a cheaper Mourning Wreath (two flowers and a shade). That is the "drops part of what summons her again" link.
- **Vesper Scythe** (15%; certain on a player's first kill): a two-handed scythe using Jugcraft's two-handed weapon system: 9 damage, slow, its swing hitting everything in a 120° arc within reach 4.5. **Reap**: a kill with it heals you two hearts (once every 5 s), and every fifth kill charges the next swing to throw a pale crescent 12 blocks (8 damage).
- **Dirge Skull** and **Requiem Skull** (50% each): trophies, giant skulls that hover and glow where you place them.
- **Reaper's Hood** (20%): a costume, counting for trick-or-treat and the costume contest.
- 300 experience shared out, and the advancement **The Last Harvest**.

## Boss 2: Madame Tatterlace, in the Spindle Loft

*From picture 5.* A great spider seamstress, three blocks across, her black body fringed in red, a tall headdress of red and gold, a purple gem at her brow, gold cuffs on every leg. She holds a lace doily in her forelegs and a long needle in another. She sews shrouds for the harvest dead and would like your measurements.

### The Spindle Loft

The attic of a colossal sewing room, seen at a spider's size:
- **The pincushion**: you arrive here, on a giant red pincushion stuck with pins taller than a house.
- **The measuring-tape bridge**: leads from the pincushion to the arena.
- **The arena**: an enormous **lace doily**, a circle about 41 blocks across, hung over darkness between four giant **thread spools** in green, blue, beige and red, with a thimble and the blades of a pair of shears beside it.
- **Overhead**: taut threads run between the spools and up to dusty rafters lit by one grimy skylight.

The lace is a lair-only block you can see through and stand on. It is furnished with the Spider's Larder (batch 19): cocoons hanging from the rafters, egg sacs, web drapes and silk spools.

### The ritual: a prick of the finger (an item used on a block)
1. Craft a **Cursed Spindle**: two gold ingots, an amethyst shard, two spider eyes, three string and a stick.
2. At night, use it on a **Spinning Wheel**, the one from knitting.
3. The wheel spins wild, its thread unwinds and wraps you, you prick your finger and fall asleep, and wake on the pincushion. The spindle is used up.
4. For 60 seconds the wheel keeps spinning, and anyone who uses it with an empty hand follows.
5. In daylight the spindle only says it is too early for sleep, and nothing is used up.

### The fight

Tatterlace: 360 health (scaled for the party), armour 8, immune to knockback, fall damage, poison and webs. She climbs and leaps between the spools.

**Her rule: the floor is her work.** She can unravel the doily under you. A segment frays (2 s of loosening threads and a ripping sound), then drops away into the dark, and **re-knits after 12 seconds**. Fall through and the mist throws you back to the pincushion as at the edges. She never unravels the arrival or the ring round a spool, so there is always somewhere safe to stand.

**Phase 1, "The Fitting"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Needlepoint | She rears, two forelegs drawn back (0.5 s) | Two quick stabs: 8 damage each |
| Thimble Toss | She plucks a gold thimble from her back | It arcs over and bounces twice, ringing: 6 damage per hit |
| Binding Thread | She aims her spinnerets and a thread glints (0.6 s) | A thread shoots out. If it hits, you are tethered for 3 s and can't move more than 4 blocks from where it holds; it reels you a block toward her each second. **Hit the thread** or sprint-jump against it to snap it |
| Lace Snare | She flicks the doily in her forelegs | A spinning lace net flies out and lands as a 3 × 3 snare for 6 s: Slowness IV and no jumping while you stand in it |
| Spool Roll | She braces against a spool's flange (0.8 s) | She kicks a spool rolling across the doily in a straight line: 10 damage and a heavy knockback. It unwinds a trail of thread and is gone |

**At half health, "Taking In the Seams"**: she hauls herself up into the threads overhead; the skylight dims; she spits a ring of egg sacs round the doily's edge. The fight changes.

**Phase 2, "The Final Fitting"** (half health to a fifth):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Pin Rain | Shadows of pins sprout on the floor of a marked area (1 s) | Pins rain down: 4 damage each |
| Unravel | Threads fray along a ring or a wedge of the doily (2 s) | That part of the floor drops away for 12 s |
| Drop Strike | Her shadow swells on one player (0.8 s) | She drops from the threads onto them: 14 damage in 3 blocks. She is then on the floor and open to attack for 3 s |
| Brood | The egg sacs round the edge split | Four spiderlings (small, quick, 6 health, poisonous for 2 s) skitter out. At most six at a time; they shrivel when she falls |

**Phase 3, "Frenzied Stitching"** (below a fifth): her cuffs glow red, every cooldown shortens by a third, and she unravels two segments at once. She stays on the floor.

She keeps to the arena: more than 30 blocks from the doily's centre, she climbs back.

### Her loot (per participant)
- **Gossamer Silk** (4–8): a premium thread. It is used for the Spider's Larder and lace decorations, for knitting finer yarn at the Spinning Wheel, and **with two gold nuggets in place of one gold ingot** in the next Cursed Spindle. That is her "part of what summons her again".
- **Needle Rapier** (15%; certain on a first kill): a slim rapier, 6 damage and quick (attack speed 2.0). Three hits on the same target within 4 s **stitch** it: Slowness II for 2 s.
- **Golden Thimble** (25%): in the offhand, it turns aside the first projectile that would hit you every 15 s, with a ping of gold.
- **Tatterlace's Headdress** (20%): a wearable trophy and costume.
- 300 experience shared out, and the advancement **Unravelled**.

## How it is built

- **Dimensions**: data-pack JSON (`data/jugcraft/dimension/` and `dimension_type/`) in the 26.3 format: the `attributes` map (`gameplay/bed_rule`, `gameplay/respawn_anchor_works`, visual sky, fog and light colours, `audio/background_music`), `has_fixed_time`, `skybox`, `default_clock` and `timelines`. Checked against vanilla 26.3's own dimension types before writing, and proved by the game tests loading them. Each lair also gets a biome with no spawns.
- **Lair layouts**: generated by Python tools (as the retro game shop's template is) into structure templates. A layout wider than a template's comfortable size is split into tiles placed together.
- **Lair rules**:
  - Fabric's events: block-break `BEFORE`, attack-block and use-block callbacks, respawn, change of world, join and disconnect, after-death;
  - the player's build ability while inside;
  - one saved-data store per lair: instances, return points and Grave Goods.
  - Each lair's player list is checked once a second for the edges. No mixins are needed.
- **The bosses**: entities on the server with boss bars. Every attack is a small state machine (wind-up, strike, recovery) with its numbers in one place. Skulls, bolts, thimbles, threads, snares and spools are their own small entities.
- **Their looks**:
  - models of parts built in code, on high-resolution textures painted by Python;
  - attack animations as Minecraft's own keyframe animation definitions, started on clients by entity events (as the Warden's roar and sonic boom are);
  - glowing eyes drawn full-bright;
  - vanilla particles and sounds, pitched and layered.
- **The prop sets**: the toolkit used since batch 15 (`tools/flora_art.py` sculpting), block-entity renderers for anything that moves, and one module of data per batch (`tools/decor17.py` with `decor17_data.py`, and so on), with audits in `tools/check_mod_data.py`.

## Connections, balance and anti-farming

- **Inputs come from what players already make**:
  - graveyard flora (the wreath);
  - hex brews (the broom's ointment);
  - the Spinning Wheel (the spindle's altar);
  - brass and copper from the alloy smelter (the rig);
  - the Stonemason's Chisel (effigies);
  - the String Light Hooks (garlands);
  - Jugs (the farm stand);
  - heirloom pumpkins and the Carving Knife (Red Kuri, Kabocha);
  - crows (they scatter from the effigy).
- **Outputs feed back**:
  - lair trophies and costumes count for trick-or-treat and the costume contest;
  - Gossamer Silk makes finer yarn;
  - the moon lamp's phase signal serves werewolf nights;
  - the sentinel and the clock are redstone tools;
  - the farm stand gives farmers a market.
- **No gain loops**:
  - the cauldron returns exactly what went in, and wafting divides one bottle;
  - candle drips and the effigy's ashes are worth less than their inputs;
  - the broom moves items and makes none;
  - the heart jar and console are signal sources only.
- **No core progression is gated**: nothing a tier needs comes only from a boss. Boss loot is gear, trophies and costumes; Gossamer Silk is also a cheaper re-summon, not a key.
- **Farming the bosses**:
  - every fight costs a fresh ritual;
  - loot is per participant, and only for those who took part (no AFK leeching);
  - first-kill guarantees are once per player (stored with the advancement);
  - the boss heals and resets if abandoned;
  - instances are capped;
  - nothing a boss drops sells to a Jugcraft shop for Jugs.

## Multiplayer, persistence and seasons

- **Server authority**: every interaction above is decided on the server.
  - Clients only draw what the server's block states and entity data say.
  - Requests from the farm stand's screen are checked for reach, ownership, stock, balance and rate.
- **Persistence**:
  - prop states live in block state and block entities;
  - lair instances, return points and Grave Goods live in saved data;
  - the bosses' fight state is saved with the entity, and a restart resets the instance (above).
- **Seasons**: the rituals work on any night of the year by default. The bosses are Halloween in spirit, and an off-season route keeps their rewards reachable.
  - During the Halloween event, each kill adds one roll of seasonal candy and a costume chance.
  - The server can switch off-season rituals off in `jugcraft-lairs.json`.
  - Ending the event never removes a lair, a dimension, a player inside one, or anything earned.
- **Griefing**:
  - a lair can't be opened on someone else's behalf;
  - no boss can reach the Overworld;
  - the rituals happen at a grave or a spinning wheel but the fight happens elsewhere, so a summon can't be dropped on a settlement.

## Tests each piece must pass

- **Each prop batch**:
  - server game tests for every behaviour listed under its checks;
  - a client screenshot test of each set, at night where it glows;
  - the data audit of IDs, recipes and models;
  - a feature record with the evidence.
- **Each boss**:
  - game tests for:
    - the ritual's steps and refusals;
    - instance allocation and the cap;
    - entry by gate only within 60 s;
    - return to the exact spot;
    - build and break refusals (placing, buckets, breaking, using);
    - the void and edge return;
    - Grave Goods kept across a death and handed back;
    - health scaling;
    - per-participant loot;
    - the abandoned-instance reset;
    - restart behaviour;
    - every attack's damage and wind-up numbers;
    - phase changes at their thresholds;
  - client screenshots of the lair and of each phase.
- **Multiplayer and seasons**:
  - the farm stand and both lairs need the manual two-client dedicated-server playtest ([TESTING.md](../TESTING.md#dedicated-server-and-two-clients)) before they are called finished;
  - seasonal checks: activation, deactivation, a restart across the boundary, no duplicate first-kill reward, and earned items kept.
- **Reporting**: nothing here is claimed as play-tested until it has been.

## Open questions for the owner

These have defaults in the plan, so work can start; say if you'd like them otherwise.

1. **Season**: rituals work all year at night, with a Halloween bonus. Should they instead only work during the Halloween event, as the Headless Horseman does?
2. **Death in a lair**: belongings come back as Grave Goods. Should death in a lair cost the items instead, as dying elsewhere does?
3. **Animation library**: Minecraft's own keyframe animations, no dependency. GeckoLib would need a version built for 26.3 and a platform PR to pin it.
4. **Party size**: four players per instance, eight instances per lair.
