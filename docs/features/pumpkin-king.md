# The Pumpkin King

Status: implemented in source; **not yet through CI**. The Fabric Maven is out of reach here, so nothing of this part has been compiled or run as a game test (below). Part 2 of the Pumpkin King in the [bosses plan](../branches/BOSSES.md#the-pumpkin-king-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the King of the Crowfield, his rule (the Harvest), his attacks, his Jack-o'-Straws and his loot, with his two Arms VII trophies and their boon. It is built on part 1, the field and the Harvest Crown that opens it ([crowfield.md](crowfield.md)), and on the lair framework ([hollow-acre.md](hollow-acre.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 23 October 2026 the owner chose the Pumpkin King as the next Halloween work ("yes"), built the same two-part way as the bosses before him. Asked whether his trophies should be a part of their own, they chose "with part 2".
Owner: @jimbozoomer-byte

Target milestone and tier: Discovery tier, as the Witching Season's bosses are. He is reached only through the Harvest Crown (three heirloom pumpkins, two gold ingots and a hay bale; part 1). Nothing a tier needs comes only from him.
Primary specialty and supported player role: adventuring. His loot also serves farmers (the Crowward Charm keeps crows off their crops), builders and decorators (the King's Lantern), smelters (the King's Ember burns long) and costume wearers (trick-or-treating and the costume contest).

## Player experience

### He waits

The Pumpkin King hangs on the King's Post at the middle of every Crowfield, placed with the field, his arms out along the crossbar and his head dark. He is the oldest scarecrow, the one the crows answer, three times the height of a man:
- a harvest coat of burlap patched in faded red, green and blue-grey, sacking trousers tied at the ankles with rope, a rope belt, and straw bursting from the ties, the cuffs and the shoulders;
- hands of pale corn husk, and a scythe of bound corn stalks with a rusted blade;
- a crown of dried vines and five ears of gold corn, and a crow on his shoulder;
- for a head, a giant carved pumpkin with a fire inside, dark until he wakes.

His body is 1.8 blocks wide and 5.4 high.

Hanging, he takes no harm. He wakes when a player who can fight him (anyone not in creative or spectator mode) steps through the farm gate into the field (looked for every half-second), or strikes him. The farmyard is safe: nobody there wakes him. Then:
- "The Pumpkin King tears free of his post, his head bursts into flame, and the crows burst out of the corn": he tears free for two and a half seconds, his head bursting into flame, and nothing can hurt him meanwhile;
- a second in, the crows burst up out of the corn all round the field, crying;
- then he steps down off his mound, in just over half a second, onto the arm of the cross (north, south, east or west) nearest the nearest player, five blocks from the middle;
- his health is set for the party: 400, half as much again for each player after the first in the field or its farmyard, at most two and a half times (1000 for four or more).

He has armour 6 and cannot be knocked back. Poison, Wither, drowning and falls never harm him. He cannot ride, be leashed or be pushed. He strides at 0.2 blocks a tick (slower than a sprinting player), stops two and a half blocks short of his foe, and steps over the hay stacks, the cart and his mound. He fights only players in the field, and his blows reach only them. He never strides farther than 25 blocks from the middle, and if he is ever more than 26 from it, or fallen below its floor, he is set back on the arm of the cross nearest his foe.

### Straw burns

A blow of **fire** does half as much again: burning, a burning arrow or other projectile, or a blow in hand with Fire Aspect or an arm that sets alight (the Ember boon, the Ignite trait). Fire is a help, never a need.

### His rule: the Harvest

**Every 22 seconds** of fighting (18 in the Hallowed Harvest, 15 in the Last Harvest; the count waits while he attacks, calls or staggers, and while a harvest ripens) he calls the harvest:
- **He calls it:** he raises his arms and the crows cry (1.5 seconds), gathering over the gourds he calls. "He calls the harvest round the south-west hill: pick the ripening gourds before they burn!" He calls two King's Gourds (three from the Crowning on): the one nearest the player he fights, then on round the ring toward whichever of its two neighbours is nearer them.
- **They ripen** over 10 seconds, crows circling over each: they swell gold-green for five seconds, then ripen orange, glowing brighter. His bar reads "The Pumpkin King: the harvest ripens".
- **Pick a ripening gourd** (use it) and it comes off the vine: "Gourd picked: 1 of 2". Any player in the field may pick one.
- **A gourd that ripens through** lights up, its carved face burning, and a second and a half later splits open: a **Jack-o'-Straw** climbs out before its face. With four Jacks already about, its fire **feeds him** instead (4% of his health): "A gourd burns through, and its fire feeds the King".
- A picked or split gourd's hill is bare until a green gourd sprouts there 20 seconds later. A bare hill he calls sprouts at once and ripens with the rest.
- **Pick every gourd of a harvest before any lights**, and he is **bereft**: "Every gourd picked: the Pumpkin King is bereft, and his fire gutters!". He sags (staggered 2 seconds, any attack spoiled), his head's fire gutters, his bar reads "The Pumpkin King: bereft!", and for **8 seconds** every blow does half as much again, fire twice as much. His Jacks stand dazed, swiping at nobody. Everyone who picked a gourd of that harvest takes part in his fight.
- When a harvest ripens through, every gourd of it still on the vine lights at once, and a lit gourd burns too hot to pick.

From anywhere in the field, a sprinting player reaches every gourd of the longest harvest well inside its 10 seconds, alone; part 1's audit walks the field to keep that true ([crowfield.md](crowfield.md#verification)).

### Phase 1, the Reaping (full to half health)

Every attack has a wind-up you can read, a strike and a recovery. He waits at least 0.8 seconds between attacks.

| Attack | Read it by | What it does |
| --- | --- | --- |
| Scythe Sweep | He draws back his scythe (0.8 s) | A sweep across his front: 8 damage to everyone within 4.5 blocks, 75° either side of his face. Step back or round him. Close in, his most frequent attack |
| Gourd Toss | He plucks a pumpkin from the rows and lifts it overhead; a ring of embers glows round where his foe stood (1.0 s) | The flaming pumpkin arcs onto the ring (0.6 s): 7 damage within 3 blocks of it, and set alight for 3 s. Step off the ring. From 5 to 20 blocks |
| Crow Storm | He flings his arms wide and the crows gather along a line toward his foe (1.2 s) | The crows stream along the line, 14 blocks long and 3 wide, for 2 s: 2 damage every half second and Blindness for 2 s to whoever is in it. Step out of the line. Within 14 blocks |
| The Harvest | Arms raised, the crows' cry (1.5 s) | Above |

The Crow Storm's and the Head Roll's lines stop at the edge of the field.

### The Crowning (at half health)

"The Crowning: he climbs back onto his post, and his head flares white": any attack under way is spent, he climbs back onto his post and his head flares white for 2 seconds, unhurt. Two Jacks climb out of the rows on the east and west arms of the cross, nine blocks either side of his post. Then he steps down onto the arm nearest the nearest player, his fire orange again, and the Hallowed Harvest begins: his bar reads "The Pumpkin King: the Hallowed Harvest". A harvest called before the Crowning ripens on through it, and picking it all still leaves him bereft (he cannot sag on his post, but his fire gutters).

### Phase 2, the Hallowed Harvest (half to a quarter)

He keeps every attack, calls the harvest every 18 seconds (the next within 18), three gourds at a time, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Vine Snare | He drives a hand into the earth and the vines ripple along the furrow (north and south) toward where his foe stood (1.2 s) | The vines lash up there, along 2.5 blocks of the furrow either way and a block either side of it: 5 damage, rooted for 1.5 s (Slowness IV). Step across the furrow, east or west. Within 16 blocks |
| Head Roll | He lifts his burning head from his shoulders, and flame runs along a line toward his foe (1.5 s) | His head rolls along the line, 18 blocks long, in 1.2 s: 10 damage to each player within a block of its path, thrown aside. Step out of the line. From 4 to 18 blocks. Then it is back on his shoulders |

### The Last Harvest (below a quarter)

"The Last Harvest: he calls the harvest faster": he calls it every 15 seconds (the next within 15), three gourds at a time, and his fire burns white. His bar reads "The Pumpkin King: the Last Harvest". The gourds still ripen in 10 seconds, and the bereft spell and his tells stay as long as ever.

### His Jacks, the Jack-o'-Straws

Little straw men in sacking tunics belted with twine, with husk hands and warty yellow-green gourds for heads, their carved faces lit. At most four at a time:
- they take a second to climb out, unhurt and harmless meanwhile;
- 20 health; fire burns them twice as fast;
- a swipe of 3 within 1.5 blocks, once a second; quick on their feet (0.3 blocks a tick);
- they harry his foe, or the nearest player in the field;
- they stand dazed while he is bereft;
- whoever hurts one takes part in his fight;
- they crumble to straw when he falls, goes back to his post or is gone. They drop nothing, give no experience and are never saved.

### His fall

When he falls:
- "The Pumpkin King's head drops from his shoulders and rolls to a stop; his fire goes out, and the crows settle";
- his Jacks crumble, and his gourds go green;
- each participant gets their loot (below) and Uncrowned;
- a gate of **Grey Mist** three blocks wide and two high opens at the foot of his mound, on the farmyard side. It takes players home, as the Grey Mist behind the farmyard does;
- the instance is ended, and it closes once everyone has left. The next instance in that slot is placed fresh, and he hangs on the post again; the Grey Mist he opened in that slot before is cleared first.

### Left alone

If no player he can fight is in the field for 10 seconds: "The Pumpkin King hangs himself back on his post, his gourds turn green, and his fire goes dark". He is back on his post at once, healed: his Jacks crumble, his gourds are green, his fire is dark, and who took part is forgotten, so a fresh fight starts from nothing.

### His loot

Each participant rolls their own loot. A participant is a player who hurt him or his Jacks (or whose tamed creature did), healed or warded others enough ([lair-encounter-credit.md](lair-encounter-credit.md)), or picked a gourd of a harvest that left him bereft, and is alive within 64 blocks of the field's middle when he falls. Anyone who died in the fight or left the game during it is paid once they are back, as at every Witching Season boss. The loot goes straight into their inventory, and what does not fit lands at their feet. "The King of the harvest is fallen: his spoils are yours".

| Loot | Chance |
| --- | --- |
| **The King's Ember**, the coal of his head's fire | 1, always |
| **A trophy:** the Hallowreap or the Crowbill, new to Arms VII (one of the two, his `bosses/pumpkin_king` table) | 15%; one is certain on a player's first win (whoever has not yet earned Uncrowned) |
| **The Crowward Charm** | 25% |
| **The Pumpkin King's Crown** | 20% |
| 300 experience, shared out among the participants | always |
| The advancement **Uncrowned** (a challenge in the Adventure tab): "Leave the Pumpkin King bereft of his harvest, and bring him down in the Crowfield" | always |

He is a Witching Season boss: during the Halloween event, unless `lairs.event_loot` is off, each participant also gets a roll of seasonal candy and another 10% chance at the crown (judged when their share is paid). Ending the event takes nothing away.

**What they are for:**
- **The King's Ember:**
  - **Fuel:** it burns in a furnace as long as a block of coal (80 items).
  - **For those who never fight him:** with any heirloom pumpkin (shapeless) it makes a **King's Lantern**: a pumpkin carved with his grin round one of his embers, which never goes out (light 15). It faces whoever placed it, is broken with an axe and drops itself.
- **The Crowward Charm:** a crow's black feather bound with straw twine to a little carved gourd bead. Held in the offhand, crows will not come down on crops within 12 blocks of its holder, out to 6 above or below, as a lit scarecrow keeps them off (`agriculture/Scarecrows.java`). It counts wherever its holder stands.
- **The Pumpkin King's Crown:** a costume worn on the head, his crown of dried vines with five corn ears standing from it, the front one tallest, each with a husk leaf behind it. It counts for trick-or-treating and the costume contest.
- **The Hallowreap and the Crowbill:** his two Arms VII trophies ([arms-vii.md](arms-vii.md)), a scythe (a long rusted blade whose edge glows like his fire, on a snath of corn stalks bound with twine, a little carved pumpkin for its socket) and a war pick (a crow's head of black iron on a haft of bound corn stalks, an ember for its eye, its long black beak the pick, a tuft of crow's feathers behind). They fight as the steel scythe and war pick do, are epic, last twice as long as steel, and carry his boon, **Fright:** a hit sends a monster running in panic for 1.5 s, eight blocks from whoever struck it, dropping whoever it was after. Each monster can be frightened once every 6 seconds. It never frightens a boss: the conventional `#c:bosses`, every lair boss and the Headless Horseman (`#jugcraft:frightless`).

### Settings

His settings are the ones every lair boss shares, in `config/jugcraft.properties` (`lair/LairBosses.java`):
- `lairs.boss_health` (1.0, from 0.25 to 4): multiplies his health;
- `lairs.boss_damage` (1.0, from 0.25 to 4): multiplies his Scythe Sweep, Gourd Toss, Crow Storm, Vine Snare and Head Roll, and his Jacks' swipes (not the Blindness, Slowness or burning, which are vanilla's);
- `lairs.event_loot` (on): the Halloween event's extra roll;
- `lairs.off_season` (on): whether the Harvest Crown works outside the Halloween event (part 1).

His damage numbers are Normal difficulty's: Easy and Hard scale them as they scale every monster's blows.

## Changes from the plan

- **The advancement is Uncrowned,** not Harvest Home: `jugcraft:harvest_home` is already the Halloween feast table's advancement, and an ID must not be reused.
- **The harvest runs toward his foe:** after the gourd nearest them, on round the ring toward whichever of its two neighbours is nearer them. The plan said only "its neighbour".
- **A bare hill he calls sprouts at once** and ripens with the rest; the plan did not say. Without it, a harvest called on a hill picked bare a moment before could never be whole.
- **Waking,** he steps down off his mound onto the arm of the cross nearest the nearest player, five blocks out; the plan said only that he tears free. The Crowning ends with the same step down.
- **The Crowning's two Jacks** climb out on the east and west arms of the cross, nine blocks either side of the post, on open floor clear of the hills and the cover; the plan said "out of the rows".
- **His party** is the players in the field and its farmyard, not a radius: two fields can lie close together in the lair dimension, and a radius would count another field's players.
- **The Crow Storm and the Head Roll run along the line toward his foe,** stopping at the field's edge; the plan had the Head Roll "along a row". A row runs north and south, so most players would never be in it.
- **The Vine Snare** lashes along 2.5 blocks of the furrow either way, and a block either side; the plan gave no size.
- **The Gourd Toss's pumpkin is his own** (`kings_pumpkin`), drawn as a burning pumpkin and flying in a high arc onto the ring he marked, rather than the Horseman's projectile.
- **The Last Harvest's fire burns white,** so it can be told from the Hallowed Harvest at a glance.
- **Fright runs eight blocks,** and at most 256 creatures panic at once. Besides the bosses, it spares the Headless Horseman.
- **He is a monster:** on Peaceful he is gone, as every monster is, and the field stands empty.
- **Animations by GeckoLib,** as the other lair bosses' are: GeckoLib 5.5.7 is already pinned.

## Connections

- **Inputs:** the way in is part 1's Harvest Crown, so heirloom pumpkins, gold and hay feed every fight.
- **Outputs:**
  - **the King's Ember** is a long fuel, and with an heirloom pumpkin a light that never goes out;
  - **the Crowward Charm** keeps crows off a farmer's crops ([halloween-harvest.md](halloween-harvest.md), the crows that scarecrows keep off);
  - **the Pumpkin King's Crown** is a costume for trick-or-treating and the costume contest (`#jugcraft:trick_or_treat_costumes` and `#jugcraft:costume_hats`);
  - **the Hallowreap and the Crowbill** are Arms VII's boss trophies, the third boss's to come with him.
- **Technology and magic:** fire in any form bites deeper (Fire Aspect, the Ember boon's arms, the Ignite trait's, burning arrows), and the Cinder Tyrant's trophies are the Ember boon's. None is needed.
- **Trade and solo routes:** a solo player can do everything. A gourd is picked by using it, a ripening gourd glows with crows over it, every attack is read before it lands, and the bereft spell opens him to any weapon. Alone, a player can pick every gourd of a harvest in time. His health is lowest for one player, and the first win always brings a trophy. His loot can be traded like any item.
- **Reachable entry path:** the crown takes what the harvest and the first days give without him (part 1), and nothing he drops is needed to reach him.

## Balance and automation

- **Every fight costs a summoning:** a fresh Harvest Crown, as part 1 set it.
- **No gain loop:** no recipe makes the King's Ember, so nothing turns back into it; the King's Lantern breaks back only into itself. The ember's 80 items of burning are a fuel's, not a source of anything. Nothing he drops sells to a Jugcraft shop for Jugs.
- **Alchemy values:** the ember, the charm, the crown and the trophies take the boss-drop fallback (1, "balance pending"), as the other bosses' loot does; the King's Lantern is the sum of its recipe.
- **No leeching:** only participants get loot, rolled per participant, so nobody takes another's. A bystander who took no part gets nothing.
- **First win once:** the trophy's first-win guarantee is stored with the advancement, so it comes once per player.
- **Abandoning resets him:** leaving him alone for 10 seconds heals him and forgets everyone's part. He cannot be worn down between visits.
- **Feeding is bounded:** a gourd feeds him only with four Jacks already about, and only 4% of his health; picking the harvest stops it.
- **No automation:** he wakes only for a player, a gourd is picked only from a player's hand, and his loot goes only to players. His Jacks drop nothing and give no experience.
- **Units:** health and damage in half hearts, before `lairs.boss_health` and `lairs.boss_damage`. Times in ticks, 20 a second; distances in blocks, his reaches from his middle. Every number is in `tools/pumpkin_king.py` (Fright's in `tools/arms_variants.py`), and `tools/check_mod_data.py` holds the Java to them.

## Multiplayer and persistence

- **Server authority:** the server decides everything: waking, his foe, the harvest and which gourds it calls, every pick and whether it leaves him bereft, every attack's hits, fire's bite, his Jacks, Fright's panic, the Crowward Charm's guard, the loot and the advancement. Clients only draw what his synced state says (his action, his fire, his head off his shoulders, a pumpkin in his hand) and play the matching animations. A gourd picked tells him on the server, and only the King whose field it is answers; the game itself checks that a player is near enough to use it.
- **The Crowward Charm** is looked at only when a crow would come down on a crop (`Scarecrows.guarded`), among the living things within 12 blocks of that crop.
- **Fright** keeps its panicking creatures (at most 256) and when each was last frightened only while the server runs, and forgets them when it stops.
- **What is saved:** he is saved with his instance's chunks: his phase and where his field is. A passing moment is not resumed: loaded while waking, he fights on; loaded crowned, the Hallowed Harvest begins. His harvest, his attacks and his Jacks are not saved: loaded fighting, he is set down on the farmyard's arm of the cross and his gourds are green. His Jacks, his tossed pumpkins and his rolling head are never saved. Instances close on a restart (part 1), and he goes with his.
- **The floor:** the only blocks he changes are his gourds (green, ripening, lit, picked; all green again when he resets or falls) and the Grey Mist he opens when he falls. Each instance is placed fresh.
- **Chunks:** nothing is force-loaded. His field stays loaded while players stand in it.
- **No griefing:** he cannot leave his lair, his attacks strike only players who can fight him in his field, his pumpkins never break or light a block, and every block he changes is put back.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. GeckoLib 5.5.7 for 26.3 is already pinned (`distribution/frameworks.lock.json`); it draws him, his Jacks, his pumpkin and his head. Everything is drawn by code:
- **Bodies and clips:** `tools/pumpkin_king_models.py` builds the four GeckoLib models and their animations on Vesperine's builder: his 26 clips (hanging, tearing free, idle, striding, every attack's wind-up and strike, the call, the stagger, the Crowning and death; his fire dark, lit, white and guttering, his head on and off, and a pumpkin in his hand or not, each on a controller of its own), his Jacks' six (climbing out, idle, walking, the swipe, dazed and crumbling), and the head's roll and the pumpkin's tumble.
- **Textures:** `tools/pumpkin_king_art.py` paints them at four times Minecraft's scale (his sheet 1024 × 1024): burlap and sacking with patches and stitching, straw, corn husk, the rusted blade, the vine-and-corn crown and the carved pumpkin. Each has a glowmask for its carved face's fire. It also paints the King's Lantern's faces and the worn crown's vine, corn and husk.
- **Icons:** the King's Ember, the Crowward Charm and the crown from icon maps (`tools/item_icons/`); the Hallowreap and the Crowbill from Arms VII icon maps (`tools/arms_icons/`) and its art (`tools/arms_variants_art.py`: a new style for his line, with corn stalk, twine, straw wrap, rind, rusted iron, crow iron and crow feather).
- **Data:** `tools/pumpkin_king_data.py` writes his bodies, the names, tooltips and messages, the item models (the crown worn as its own model), the King's Lantern's block model, states and loot, the loot table, the lantern's recipe, the costume tags, the crow wards (`#jugcraft:crow_wards`), the creatures Fright spares (`#jugcraft:frightless`) and the advancement. The trophies and their `bosses/pumpkin_king` table are written with the other Arms VII variants (`tools/arms_variants.py`).
- **Sounds:** vanilla's: the parrot's cry for his crows, the blaze's shot, the ender dragon's wingbeat, the ravager's footfall, the trident's riptide and a blade's sweep, crops breaking, a fire charge, a pumpkin carved, a fire put out and grass breaking.

No Mojang texture is read, traced or copied.

New IDs (all under `jugcraft`):
- the entities `pumpkin_king`, `jack_o_straw`, `kings_pumpkin` and `kings_head`;
- the items `kings_ember`, `crowward_charm` and `pumpkin_kings_crown`, and the block `kings_lantern` with its item;
- the Arms VII trophies `hallowreap` and `crowbill`, and their table `bosses/pumpkin_king`;
- the recipe `kings_lantern`, the loot tables `entities/pumpkin_king` and `blocks/kings_lantern`, the tags `crow_wards` (items) and `frightless` (entity types), and the advancement `uncrowned`.

Nothing is renamed.

## Verification

Run locally (26 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks him (`check_pumpkin_king`, below) | `check_pumpkin_king` passes. Five `tower_costs.BLOCK_VALUE` lines fail on this machine only: the solder ingot has two recipes, `tools/tower_costs.py` takes whichever the file system lists first, and CI lists them the other way and passes. Nothing of this part fails |
| Deliberate breaks of his Java, the tables and the data, one at a time (below) | 13 of 13 failed it |
| `python3 -m unittest discover -s scripts/tests` | Pass, 268 tests, once the survival audit's item snapshot listed the six new items (below) |
| `python3 tools/check_icon_maps.py` on the three new item maps and the two trophy maps | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the files they rewrote for other features, with identical pixels or not, were put back) |
| `javac` on the new and changed Java and the two test classes, for syntax only | No syntax errors |
| `./gradlew build`, game tests and client game tests | Not run (the Fabric Maven is out of reach here); to be run by CI |

The survival audit (`tools/audit_survival_recipes.py`) reads its items from `docs/audits/survival-item-registry.json`, a snapshot that `SurvivalRecipeGameTests` exports from the game's real registry. That game test cannot run here, so the six items this part registers (the ember, the charm, the crown, the lantern and the two trophies) were added to the snapshot by hand, from their registrations. The next real export should agree; if it does not, it is the one to keep.

`check_pumpkin_king` checks:
- his Java numbers, his attacks and his Jacks', the loot's, Fright's and the charm's numbers against `tools/pumpkin_king.py` and `tools/arms_variants.py`;
- that his rule can be played: the Crowning before the Last Harvest; fire bites deeper, and bereft deeper still; each harvest is called, ripens, lights and splits before the next is due, and they come sooner as he weakens; he calls no more gourds than part 1's harvest walk proves one player can pick, and they ripen in the time it is measured against; his stagger is shorter than his bereft spell; each attack is used only within its reach; the Crowning's Jacks have their places, within the four;
- that his waking, the Crowning, the call and every wind-up last as long as their clips, and his Jacks' climb as long as theirs; that his body is as tall as his model, and hanging, his shoulders meet the crossbar;
- that where his fight looks for the field's parts is where `tools/crowfield.py` builds them: he steps down onto open floor with room for him, and his Jacks climb out of the rows and before each gourd's face onto open floor with room for them;
- that the harvest's picks, the bereft spell, his deeds and fire's bite are wired as above; that his Jacks burn twice as fast, take part for him, stand dazed and crumble; that the field's hooks, the gourds' picks, his rewards, Fright and the lantern's light are registered;
- that Fright frightens only monsters that walk, never one in `#jugcraft:frightless`, and at most 256; that every lair boss with a bar is in both the table and the tag; that the charm counts as a lit scarecrow;
- his registrations, names and renderers (each with its glow layer); his GeckoLib models, every clip the Java plays, the sheets and glowmasks (size, no half-transparent pixels, no overlapping UV regions) and his controllers' bones; the loot table and his trophies against Arms VII's; the lantern, its recipe (and none making the ember), the costume tags, the crow wards, the advancement and messages; and that nothing of his loads chunks or changes dimension.

The 13 deliberate breaks: his health in the Java; the bereft spell from any harvest; the Last Harvest before the Crowning; a wind-up in the Java alone; a Crowning Jack's place in the cover (table and Java); Fright sparing nobody; a message without words; a loot chance; a boss left out of Fright's table; a chunk loaded; his tear-free clip shorter than his waking (table and Java); his Jacks' fire bite; the lantern unlit. The first try missed the boss left out of the table (only the tag mismatch caught it); the check now holds every lair boss with a bar to both.

What the building found:
- `jugcraft:harvest_home`, the plan's advancement, is the Halloween feast table's already; his is Uncrowned (above).
- The King's Lantern's loot table used the pre-26 `conditions` key; it now uses the pool's `condition`.
- The audit did not know his entities, so `#jugcraft:frightless` listed "unknown" entries, until the known entity IDs were read from every lair's and the agriculture branch's registrations.
- The worn crown's husk leaves were bare planes whose faces flickered against the ears; they are thin boxes, with only the outer face of each touching pair kept.
- A pose that names a bone's keys dropped that bone's waves, until the clips added them back (`looped()`).
- Counted by distance, his party took in players beside his field who were not in it (another test's, or another field's); it is the field and its farmyard (above).
- Two more Arms VII variants make 43, and in `ArmsVIIClientGameTests` the 43rd's frame would have hung over the first pattern's; the patterns hang two blocks farther along.

The game tests (`PumpkinKingGameTests`) show what a game-test server can, with a King put down on his own (no lair) over a plain field the Crowfield's size (`jugcraft-test:crowfield_plain`), its hills and gourds laid out by each test:
1. he hangs on his post, his fire dark, and takes no harm; a player in the farmyard does not wake him, nor does a creative player in the field or their blow; a player who may fight him in the field does, at full health, his fire lit, unhurt while he tears free; then he steps down to fight;
2. fire bites deeper (a plain blow of 4 and fire of 4, and bereft both deeper still, his armour set aside to read them);
3. he calls the harvest on the gourds nearest his foe, arms raised, and after his call they ripen and his bar says so;
4. a gourd that ripens through lights, ending the harvest, and splits: a Jack climbs out, and with four about it feeds him instead;
5. picking one gourd of two neither leaves him bereft nor ends the harvest; picking both does: he sags, his fire gutters, his bar says so, the pickers take part, his Jack stands dazed, and the spell ends with his fire lit again;
6. a lit gourd cannot be picked, and a harvest one of whose gourds lit leaves him unbereft;
7. at half health he is crowned on his post, his fire white, unhurt, and two Jacks climb out; after it he is in the Hallowed Harvest, calling three gourds, and steps down toward his foe; below a quarter, the Last Harvest;
8. his Scythe Sweep strikes the player before him and not the one behind; his crows strike and blind the player in their line and not the one beside it;
9. his Gourd Toss lifts a pumpkin, marking his foe's place, and tosses it: it strikes and sets alight the player on its ring and not one six blocks off;
10. his vines strike and root the player where they lash and not one across the furrow; his head leaves his shoulders and rolls, striking the player in its line and not one beside it;
11. a Jack takes no harm climbing out; fire burns it twice as fast; striking it takes part; it stands dazed while he is bereft and crumbles when he goes back to his post;
12. left alone, he hangs himself back on his post, healed, his fire dark, his gourds green, his Jacks crumbled, and who took part forgotten;
13. the Crowward Charm in the offhand guards crops, and not in the main hand or without it; the King's Ember burns; an ember and an heirloom pumpkin make a King's Lantern; the crown is a costume worn on the head;
14. Fright panics a husk, dropping its target, for its time, and not again within its cooldown; it never frightens a boss (the King) or a creature that is no monster (a cow);
15. his loot is each participant's own: the King's Ember and, on a first win, one of his trophies into the inventory of each who struck him and of one who only picked a harvest that left him bereft, with a share of his experience each and Uncrowned theirs afterwards, nothing for a bystander; he falls with his fire going out.

The client test (`PumpkinKingClientGameTests`) runs his whole fight in a real Crowfield, with the one player in survival under Resistance: he hangs on his post; stepping into the field wakes him; a harvest called on the south hills, the player picking both gourds and leaving him bereft; the Reaping's Scythe Sweep, Gourd Toss and Crow Storm, posed for their pictures; the Crowning on his post; the Hallowed Harvest's Vine Snare and Head Roll; a Jack-o'-Straw; and his fall: the player's ember, trophy and Uncrowned, his Jacks gone, his gourds green, Grey Mist at his mound; then a fresh instance in his slot with no Grey Mist and him on his post again. Twelve pictures along the way.

Not run: all of the game tests and the client test (CI will run them), the two-client dedicated-server playtest, and play by hand.

## World and event applicability

He lives only in the Crowfield, his own dimension, so nothing here changes the Overworld. The Crowward Charm works anywhere crows come down on crops. The Harvest Crown works all year unless the server keeps it to Halloween (part 1); the event only adds to his loot, and everything he drops is kept for good.

## Rollout and open questions

- **Difficulty:** his numbers are the plan's. After a hand playtest the owner may want his harvests more or less often (22, 18 and 15 s), the bereft spell longer or shorter (8 s), fire's bite stronger or weaker (half as much again), or his Jacks tougher.
- **Pictures:** once CI runs the client test, its twelve pictures should be looked at and added here.
- **Still to do:** the two-client dedicated-server playtest, and a play by hand.
