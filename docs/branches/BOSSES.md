# Bosses branch (brainstorm)

Status: **a brainstorm; nine bosses are built.** On 10 October 2026 the owner chose to build a new boss "the same way" as Madame Tatterlace ([features/tatterlace.md](../features/tatterlace.md)); the Yeti King is first, to the plan below. The same day the owner asked for "the next boss same way" as him: the Cinder Tyrant, to his plan below. Both are built. On 13 October 2026 the owner chose the next: the Mire Hag, to her plan below, who is built too; and after her, the same day, the Crypt Lich, to his plan below, who is built too. On 14 October 2026 the owner chose the Iron Dreadnought, to his plan below, who is built too. The same day the owner chose the Alpha Werewolf, to his plan below. On 23 October 2026 the owner chose a Halloween boss, the Pumpkin King, to his plan below, who is built too. On 4 October 2026 the owner asked for variant weapons, many of them "drops from bosses ill make later", themed around those bosses ("example: Yeti King"), and to "brainstorm bosses we could make". This page is that brainstorm. The bosses' weapons already exist: Arms VII ([features/arms-vii.md](../features/arms-vii.md)) adds two trophies for each of the first eight bosses below, and their loot tables are ready (`data/jugcraft/loot_table/bosses/<boss>.json`, one of the two per roll). Building a boss is a separate, reviewed feature each time, in the order the Yeti King was built in: [BOSS_PLAYBOOK.md](BOSS_PLAYBOOK.md). Everything here is a proposal for the owner to pick from, change or drop.

The owner's Mars adventure, the Cydonian Sentinel, is also implemented: [feature record](../features/cydonian-sentinel.md).

## Rules every boss follows

From [CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md) ("Caves, dungeons and bosses" and "Rare loot"):
- **An arena with edges:** a lair, or a summoning that builds one. The boss does not wander out of it, and it resets when everyone leaves.
- **Readable attacks:** every big attack is telegraphed (a wind-up, a sound, a shadow on the ground) and can be dodged or blocked.
- **Recovery:** dying in the fight is not the end; players retain their entire inventory and experience, and the boss heals and resets rather than keeping its gains.
- **Multiplayer:** its health scales with the players in the arena; every player who took part gets a roll of its loot table, not just the last hit. A blow to it, or to a creature that counts for it, is taking part; at Vesperine and Madame Tatterlace since Concordance Chunk A, and the Yeti King and the Cinder Tyrant since Chunk B, so is support: healing or warding another player against the fight, or one of its objectives ([lair encounter credit](../features/lair-encounter-credit.md)).
- **No griefing:** a summoned boss cannot be called inside a town's protection or another player's claim, and its attacks do not break player builds.
- **No farming loop:** a long respawn timer per lair (or a costly summoning item), so a trophy stays a trophy.
- **Optional:** no core progression needs a boss. Its trophies are side-grades with a boon, and each boss also drops something useful to non-fighters (a crafting material, a decoration, a trophy head).

## Built so far

| Boss | Lair, and how it is opened | Records | In `main` |
|---|---|---|---|
| **Vesperine, the Last Reaper** | The Hollow Acre: the Last Rites at a Jugcraft headstone at night (four lit candles, a Mourning Wreath and the Death Knell) | [hollow-acre.md](../features/hollow-acre.md), [vesperine.md](../features/vesperine.md) | #284 and #288, through #277 |
| **Madame Tatterlace** | The Spindle Loft: a Cursed Spindle used on a Spinning Wheel at night | [spindle-loft.md](../features/spindle-loft.md), [tatterlace.md](../features/tatterlace.md) | #292 and #294, through #277 |
| **The Yeti King** | The Glacier Hall: the Frost Horn | [glacier-hall.md](../features/glacier-hall.md), [yeti-king.md](../features/yeti-king.md) | #300 and #304, through #320 |
| **The Cinder Tyrant** | The Cinder Kiln: the Kiln Seal | [cinder-kiln.md](../features/cinder-kiln.md), [cinder-tyrant.md](../features/cinder-tyrant.md) | #313 and #317, through #320 |
| **The Mire Hag** | The Lantern Fen: the Hag Stone | [lantern-fen.md](../features/lantern-fen.md), [mire-hag.md](../features/mire-hag.md) | #331 and #336, through #352 |
| **The Crypt Lich** | The Archive of Last Names: the Writ of Exhumation | [crypt-archive.md](../features/crypt-archive.md), [crypt-lich.md](../features/crypt-lich.md) | #350 and #353, through #352 |
| **The Iron Dreadnought** | The Foundry Trench: Sealed Orders | [foundry-trench.md](../features/foundry-trench.md), [iron-dreadnought.md](../features/iron-dreadnought.md) | Not yet |
| **The Alpha Werewolf** | The Moonlit Ring: the Howling Totem | [moonlit-ring.md](../features/moonlit-ring.md), [alpha-werewolf.md](../features/alpha-werewolf.md) | Not yet |
| **The Storm Roc** (part 1 so far) | The Crown of Thunder: the Thunder Vane | [crown-of-thunder.md](../features/crown-of-thunder.md) | Not yet |
| **The Pumpkin King** | The Crowfield: the Harvest Crown | [crowfield.md](../features/crowfield.md), [pumpkin-king.md](../features/pumpkin-king.md) | Not yet |

The first two are the Witching Season's ([features/witching-season.md](../features/witching-season.md)); all six, the Iron Dreadnought's trench and the Alpha Werewolf's ring are on the shared lair framework (`lair/`). Vesperine and Madame Tatterlace roll their own loot table, `loot_table/entities/<boss>.json`, for each player who took part: their trophies, the Vesper Scythe and the Needle Rapier (Arms VII's), come at 15% a share and always on a player's first win. The Arms VII trophy tables `bosses/vesperine` and `bosses/tatterlace` are written by `tools/arms_variants.py` as the others' are, but nothing rolls them. The Yeti King and the Cinder Tyrant roll theirs the same way, a trophy always on a player's first win. Both have been on Concordance Chunk A's support credit since Chunk B ([lair-encounter-credit.md](../features/lair-encounter-credit.md)). JEI's information pages on their loot and the encyclopedia's lair dossiers cover the first four (the Yeti King and the Cinder Tyrant since Chunk B; [concordance-first-loadout.md](../features/concordance-first-loadout.md), [arcane-concordance-first-route.md](../features/arcane-concordance-first-route.md)).

## The first eight (trophies built)

| Boss | Home | Encounter idea | Trophies (boon) | Loot table |
|---|---|---|---|---|
| **The Yeti King** | Tundra, Ice Sheet, Frost Rift | A white ape twice a player's height, wearing an ice crown. It throws ice boulders, its ground slam freezes the floor slick, and its roar calls lesser yetis. A cave lair in a glacier. | Glacier Maul (maul), Rimeclaw (katar): **Frost** | `bosses/yeti_king` |
| **The Cinder Tyrant** | Volcano, Magma Fields, Ashfall Wastes | A salamander lord in obsidian armour, with magma in its cracks. It rains cinders, a lava wave runs from it, and its armour sheds when cooled with water. | Cinderbrand (greatsword), Magmaw (earthbreaker): **Ember** | `bosses/cinder_tyrant` |
| **The Mire Hag** | Bog, Quagmire, Sludge Mire, Hallowed Bog | A witch of the deep swamp who hides in the fog among illusions of herself. She throws poison brews and binds players with roots; strike the real one, which casts a shadow. | Hagthorn (scythe), Bogfang (kama): **Venom** | `bosses/mire_hag` |
| **The Crypt Lich** | The graveyard's crypts, Ghost Forest | A crowned skeleton sorcerer below a crypt. It raises the restless dead in waves, drains life with a soul beam, and hides its heart in a phylactery that must be broken first. | Soulreaver (moonblade): **Drain**; Gravewarden (executioner): **Wither** | `bosses/crypt_lich` |
| **The Iron Dreadnought** | The front line (trench works) | A walking war machine bigger than the landship, with steam vents and tesla coils. It charges in straight lines, its coils arc between players who stand close, and its weak vents glow before they open. | Dynamo Halberd (halberd), Piston Hammer (war hammer): **Shock** | `bosses/iron_dreadnought` |
| **The Alpha Werewolf** | Gloomweald and dark forests, on a full moon | The pack leader of the werewolves already in the world, twice their size. It leaps between trees, howls to weaken players and call the pack, and flees to heal at dawn. | Moonfang (sabre), Howler (twinblade): **Howl** | `bosses/werewolf_alpha` |
| **The Storm Roc** | Highland, Karst Pinnacles | A thunderbird nesting on the tallest pinnacle. It dives with lightning strikes and gusts that throw players off the peak; its nest's lightning rods can be turned against it. | Stormcaller (glaive), Galefeather (estoc): **Gale** | `bosses/storm_roc` |
| **The Abyssal Leviathan** | Ocean Trench | A sea serpent in the trench's depths. It drags boats under, its whirlpool pulls players in, and it is fought from a sunken temple's ledges. | Tidebreaker (war fork), Leviathan's Hook (bill): **Tide** | `bosses/abyssal_leviathan` |

Each boss's table is ready for its encounter to roll on death. Until the bosses exist, their trophies are in the creative Combat tab only.

## The Yeti King: the plan (being built)

Built the way the Witching Season's bosses were (Vesperine in [features/vesperine.md](../features/vesperine.md), Madame Tatterlace in [features/tatterlace.md](../features/tatterlace.md)), on their lair framework ([features/hollow-acre.md](../features/hollow-acre.md)): first his lair and the summoning that opens it, then the King himself and his loot, each its own pull request with tests and pictures. The numbers here are the plan's; each part's feature record says what changed in building it. Part 1, the hall and the horn: [features/glacier-hall.md](../features/glacier-hall.md). Part 2, the King and his loot: [features/yeti-king.md](../features/yeti-king.md).

*A white ape twice a player's height, his fur thick and frosted, a crown of blue ice on his brow, his breath a cloud of frost.* He rules the glacier from a throne of ice, and the mountains' cold is his to command.

### The Glacier Hall

A vast cavern in the heart of a glacier, walled and vaulted in blue and packed ice, lit from within where daylight soaks through the ice:
- **The arrival:** a ledge high in the south wall, where a snow-choked crevasse lets in. Beside it, Grey Mist hangs in an arch of blue ice: the way home.
- **The snow ramp:** packed snow curving down the wall from the ledge to the lake, half a block lower each block, walked both ways without jumping.
- **The arena:** a frozen lake 41 blocks across, its ice crusted with drift snow, ringed by four great **ice columns** that rise to the vault. Round each column the snow is trampled hard.
- **The throne:** on the north shore, a dais of blue ice steps and a throne hewn from the ice under an arch of two mammoth tusks, his frozen hoard about it.
- **The dens:** two caves in the east and west walls at the lake's level, where his kin wait.
- **Overhead:** giant icicles hang from the vault, and a crack high in it lets a shaft of pale light fall onto the lake.

The lair-only blocks (drift snow, glare ice, giant icicles, mammoth tusk, the frozen hoard) are fixtures like the other lairs': unbreakable, no items, no drops.

### The summoning: the Frost Horn

1. Craft a **Frost Horn**: a goat horn bound with two gold ingots, two leather and two snow blocks.
2. At night, in the Overworld, blow it standing on snow or ice.
3. Its call rolls over the snow and is answered by a roar. The snow at your feet splits and a whirl of white mist rises: you fall through, and land on the ledge in the Glacier Hall, frost on your skin. The horn is used up.
4. For 60 seconds the whirl stays, and anyone who uses it follows.
5. By day, off the snow, or outside the Overworld, the horn only says why, and nothing is used up.

He is not a Witching Season boss: the horn works all year, with no Halloween bonus.

### The fight

The Yeti King: 420 health (scaled for the party: half as much again for each player after the first, at most two and a half times), armour 10, immune to knockback, fall damage, freezing and webs. He waits slumped on his throne, and wakes when a player who can fight him steps onto the lake, or strikes him: he roars and leaps down onto the ice (2 s, unhurt meanwhile). He keeps to his hall: more than 30 blocks from the lake's centre, he bounds back.

**His rule: the snow is his.** His Ground Slam blasts the drift snow from a ring of the lake round where he lands, baring **glare ice**, slick as blue ice, for 12 seconds before the snow drifts back. On it you slide, and every blow carries you further. He never bares the trampled snow round an ice column, the foot of the snow ramp or his dais, so there is always firm footing somewhere.

**Phase 1, "The Hunt"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Maul Swipe | He rears back, both fists raised (0.6 s) | A two-fisted swipe across his front, 120°, 4 blocks: 12 damage and a knockback. Up close, his most frequent attack |
| Boulder Throw | He tears a block of ice out of the lake (0.8 s) | He hurls it in an arc at his foe; it shatters where it lands: 10 damage within 2.5 blocks |
| Ground Slam | He crouches low (0.8 s) | He leaps onto his foe: 14 damage within 3 blocks, a knockback within 6, and the snow blasted from a ring 7 blocks round him (his rule) |
| Frost Breath | He draws in a breath, frost at his jaws (1 s) | A cone of freezing breath, 6 blocks long, for 1.5 s: 2 damage each quarter-second and the frost (slowed, and freezing damage once frozen through) |
| Avalanche Charge | He drops to all fours and paws the snow (0.8 s) | He charges up to 16 blocks in a straight line: 12 damage and a heavy knockback to each player he passes. **Into an ice column, he stuns himself for 3 s and takes a third more damage meanwhile** |

**At half health, "The King's Roar":** he bounds back onto his dais and roars for 3 seconds, unhurt. A blizzard rises in the hall, chilling everyone near (the frost, for a moment), and his kin answer: two **Yeti Whelps** climb out of the dens. Then he leaps back down, and the fight changes.

**Phase 2, "The Blizzard"** (half health to a fifth): he keeps his Maul Swipe, Boulder Throw, Ground Slam and Avalanche Charge, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Icicle Fall | He roars at the vault, and icicles' shadows spread on the snow round his foe (1 s) | Eight icicles fall one by one: 6 damage to a player within a block of where one lands |
| Glacial Spikes | He drives a fist into the ice, and cracks race towards his foe (0.8 s) | Spikes of ice burst up along the cracks, 12 blocks: 10 damage, thrown up |
| Kin Call | He beats his chest and howls (1 s) | Two Yeti Whelps climb out of the dens; at most four at a time |

**Yeti Whelps:** young yetis, 16 health, their blows 3 damage, quick. They go for the nearest player, drop nothing, and flee into the snow when he falls, resets or is gone. Whoever hurts one, or whose tamed creature does, has taken part in his fight.

**Below a fifth, "The Fury of the Peaks":** his crown blazes and his eyes burn blue. Every cooldown (and his pause between attacks) is a third shorter, his slam bares twice the ring, and a charge that does not stun him is followed at once by another.

**Left alone** for 10 seconds, he climbs back to his throne, healed; the snow drifts back, his whelps flee, and who took part is forgotten.

### His loot (per participant)

A participant is a player who qualified in his fight: by a blow to him or a whelp (their own or their tamed creature's, the felling blow included), by healing or warding other players against 4 of his side's blows with the Concordance; by baiting his charge into an ice column, having already taken part; or by bracing another player who then takes one of his throws, a Brace on oneself or the Water Bangle's counting for no one (since Concordance Chunk B, [lair-encounter-credit.md](../features/lair-encounter-credit.md); the Brace since its unit 5, B-09). One alive within 64 blocks of the lake's centre when he falls is paid at once; one who died in the fight once back on their feet; one who left the game in it on joining again. Each fall pays each player once.

| Loot | Chance |
| --- | --- |
| **Yeti Fur**, a pelt of thick white fur | 4 to 8, always |
| **A trophy:** the Glacier Maul or the Rimeclaw (his `bosses/yeti_king` table, one of the two) | 15%; certain on a player's first kill |
| **Yeti Mitten** | 25% |
| **The Yeti King's Crown** | 20% |
| 300 experience, shared out among everyone paid | always |
| The advancement **Abominable** | always |

- **Yeti Fur:** a cheaper Frost Horn (fur in place of the two leather and a gold ingot), his "part of what summons him again"; or two white wool.
- **Yeti Mitten:** held in the offhand, you never freeze: powder snow, his Frost Breath and the blizzard leave you warm. It stays the permanent prize; since Concordance Chunk B, Winter Brine (brewed at an Alembic Crucible) gives Frostward, the timed answer to his frost ([the winter record](../features/arcane-concordance-winter.md)).
- **The Yeti King's Crown:** a costume worn on the head, his crown of blue ice in small.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Frost boon; until now they were in the creative tab only. Since Concordance Chunk B, the boon's slow is also Rimekeeping practice (an elective), with no number changed.
- When he falls, Grey Mist opens in the middle of the lake, the way home.

### Rules kept

He follows the rules every boss follows (above): his hall is his arena and resets when everyone leaves; every big attack is read before it lands; dying there keeps everything: nothing is dropped, and the player respawns with it all; his health scales with the party and every participant rolls his loot; nothing in his hall can be built or broken, and the only blocks he changes are his own lake's snow, which drifts back; every opening costs a Frost Horn. Nothing a tier needs comes only from him.

## The Cinder Tyrant: the plan (being built)

The second of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md) as the Yeti King was: first his lair and the summoning that opens it, then the Tyrant himself and his loot, each its own pull request with tests and pictures. Part 1, the kiln and the seal: [features/cinder-kiln.md](../features/cinder-kiln.md). Part 2, the Tyrant and his loot: [features/cinder-tyrant.md](../features/cinder-tyrant.md).

The design joins two sources:
- this page's own idea for him: cinders, a lava wave, armour that sheds when cooled with water;
- the planning pack's "Kiln Beneath the Mountain" proposal ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)): a basalt kiln bowl, three cooling sluices, four safe shelves, a heat channel, and armour cooled by baiting his slam into a sluice's flood.

The numbers here are the plan's. Each part's feature record says what changed in building it.

*A salamander lord the size of a cart, armoured in plates of obsidian with magma glowing in their cracks, a crest of obsidian spikes down his spine and a tail like a club of basalt.* He broods in a kiln beneath a volcano, and the mountain's fire is his.

### The Cinder Kiln

A kiln hollowed out of a volcano's roots: a bowl of basalt under a dome of kiln brick, open at its crown to a vent where the smoke rises. Ash drifts in the air, and the slag lights it red.
- **The arrival:** a basalt ledge high in the south wall, at the mouth of a lava tube long since cooled. Beside it, Grey Mist hangs in an arch of kiln brick: the way home.
- **The stair:** basalt steps from the ledge down to the bowl, half a block lower each block, walked both ways without jumping.
- **The bowl, the arena:** 44 blocks across, floored with cracked basalt.
- **The heat channel:** a trench of **molten slag** running from the **forge mouth** in the north wall to the **crucible**, a round pool of slag in the bowl's north third. He waits sunk in the crucible. The slag burns whoever steps in it.
- **The sluices:** three great iron sluice gates in the west, east and south walls. Each holds back an underground stream above a **quench trough**: a paved strip of trough stone running from the gate toward the bowl's middle.
- **The shelves:** four raised shelves of basalt round the bowl (north-west, north-east, south-west, south-east), up a step. The slag never reaches them.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. They are kiln brick, molten slag, trough stone and the sluice gates. Only the sluice gates can be worked, by any player in the kiln.

### The summoning: the Kiln Seal

1. Craft a **Kiln Seal**: a disc of four obsidian round a magma block, rimmed with two gold ingots and marked with two blaze powder.
2. Press it into a **magma block**, in the Nether or in a volcanic land of the Overworld (the Volcano or the Cinder Barrens), at any hour.
3. The magma cracks open and a vent of sparks and smoke rises from it. You sink through the stone and land on the ledge in the Cinder Kiln. The seal is used up.
4. For 60 seconds the vent stays, and anyone who uses it follows.
5. Off a magma block, or anywhere else, the seal only says why, and nothing is used up.

Like the Yeti King, he is not a Witching Season boss: the seal works all year.

### The fight

The Cinder Tyrant has 440 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 10, and is immune to fire, lava, knockback and fall damage.

He waits sunk in the crucible, only his crest above the slag. He wakes when a player who can fight him steps down onto the bowl's floor, or strikes him: he rises roaring from the slag and crawls out (2 s, unhurt meanwhile). He keeps to his kiln: more than 30 blocks from the bowl's centre, he bounds back.

**His rule: the heat is his.** While his cracks glow he is **hot**, and every blow on him does half its damage. To **quench** him:
1. Turn a sluice's wheel. Its trough floods for 10 seconds, then drains, and the sluice takes 20 seconds to fill again.
2. Bait his **Body Slam** into a flooded trough. The water bursts into steam and he is quenched for 8 seconds.
3. Quenched, his armour cools black and cracks, flaking away. Blows do their full damage and a quarter more, and he is slowed.
4. Then he heats again: his cracks flare back over a second.

The slag never cools. The shelves and the dry troughs are firm footing.

**Phase 1, "The Kiln"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Tail Sweep | He coils, his tail raised and glowing (0.8 s) | His tail sweeps round his back and flanks, 4 blocks: 10 damage, a knockback, and you burn for 3 s. Close in, his most frequent attack |
| Ember Spit | His throat glows, and three glowing marks spread on the floor near his foe (1.2 s) | Three gobs of magma arc onto the marks: 8 damage within 1.5 blocks of each, and a patch of fire for 4 s |
| Body Slam | He rears up and his plates flare (1 s) | He leaps onto his foe: 12 damage within 3 blocks, a knockback within 5. **Into a flooded trough, he is quenched** |
| Kiln Breath | He draws in, and a glowing sector spreads on the floor before him (1.5 s) | A blast of kiln heat through a 60° sector, 8 blocks long, for 1.5 s: 3 fire damage each half-second, and you burn |
| Mantle Shed | He shudders, his cracks flaring white (2 s) | Burning shards of his mantle fall and crawl off as two **Cinderlings**, at most four at a time |

**At half health, "The Eruption":**
- He climbs onto the lip of the forge mouth and roars for 3 seconds, unhurt.
- Cinders spit from the vent, and the heat channel **surges**, announced: for 4 seconds the slag spills 2 blocks over its banks. The shelves stay dry.
- Two Cinderlings crawl out of the slag.
- Then he comes down, and the fight changes.

**Phase 2, "The Eruption"** (half health to a fifth): he keeps his Tail Sweep, Ember Spit, Body Slam, Kiln Breath and Mantle Shed, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Cinder Rain | He roars at the vent, and glowing marks spread round his foe (1.2 s) | Six cinders fall one by one: 6 damage to a player within a block of where one lands, and a patch of fire |
| Lava Wave | He beats his tail on the floor, and the floor round him cracks and glows (1.5 s) | A low ring of molten slag rolls out from him to 12 blocks over 2 s: 8 fire damage and burning to anyone it passes, unless they jump it or stand on a shelf |

In this phase the sluices take turns: one at a time is choked with slag and will not turn. It is shown by the slag glowing in its gate, and the choke moves every 20 seconds, so one or two always work. Every 30 seconds the channel surges again, announced as before.

**Cinderlings:** small salamanders of glowing slag. They have 14 health and bite for 3 damage, setting you burning for 2 s. They are quick and immune to fire, but in a flooded trough they gutter out. They go for the nearest player, drop nothing, and crumble to ash when he falls, resets or is gone. Whoever hurts one, or whose tamed creature does, has taken part in his fight.

**Below a fifth, "The Molten Heart":** his cracks blaze white. Every cooldown, and his pause between attacks, is a third shorter. His Ember Spit throws five, and his Kiln Breath sweeps 90°. Quenching him still works as before.

**Left alone** for 10 seconds, he sinks back into the crucible, healed. The troughs drain, the sluices reset, his Cinderlings crumble, and who took part is forgotten.

**Fire Resistance**, from a potion or the Ember invocation Hearthguard, stops his fire: the breath, the slag, the wave, the fire patches and burning. It never stops his blows: the Tail Sweep, the Body Slam, the gobs' and cinders' impact, and the Cinderlings' bites. It helps the prepared without emptying the fight. Hearthguard is the planning pack's Ember link; its Tide, Rime and Strata links wait for those schools.

### His loot (per participant)

A participant is a player who qualified in his fight: by a blow to him or a Cinderling (their own or their tamed creature's, the felling blow included), or by healing or warding other players against 4 of his side's blows with the Concordance (since Concordance Chunk B, [lair-encounter-credit.md](../features/lair-encounter-credit.md); his slag's heat and burning are no one's blows, and he names no objective of his own). One alive within 64 blocks of the bowl's centre when he falls is paid at once; one who died in the fight once back on their feet; one who left the game in it on joining again. Each fall pays each player once.

| Loot | Chance |
| --- | --- |
| **Tyrant Scale**, a scale of obsidian veined with cooling magma | 3 to 6, always |
| **A trophy:** the Cinderbrand or the Magmaw (his `bosses/cinder_tyrant` table, one of the two) | 15%; certain on a player's first kill |
| **Salamander Charm** | 25% |
| **The Tyrant's Crest** | 20% |
| 300 experience, shared out among everyone paid | always |
| The advancement **Tempered** | always |

- **Tyrant Scale:** two magma cream, for Fire Resistance; or a cheaper Kiln Seal, his part of what summons him again.
- **Salamander Charm:** held in the offhand, hot ground never burns you. That covers magma blocks and his slag.
- **The Tyrant's Crest:** a costume worn on the head, his crest of obsidian spikes in small with the magma glowing between them.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Ember boon; until now they were in the creative tab only.
- When he falls, the crucible's slag cools to black glass, and Grey Mist opens in the bowl's middle, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the ledge and the stair are safe. He wakes when a player steps down onto the bowl's floor or strikes him. Grey Mist is in the arch by the ledge, and in the bowl's middle when he falls.
- **Fixtures:** the three sluice gates, worked by any player in the kiln, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the kiln owns it. Only these change:
  - the troughs' water, which drains after 10 seconds;
  - the surge's spilled slag, which ebbs after 4;
  - the fire patches, which go out;
  - the crucible's cooling when he falls.

  Every one of these comes back when he resets, and each instance is placed fresh.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his gobs, cinders, waves, fire patches and Cinderlings are his, capped, and gone with him.
- **Death, disconnect and restart:** as every lair. Players keep their entire inventory and experience; Grave Goods catch residual drops, and instances close on a restart. Each participant's share is paid once and rolled when it is handed over: at once, or kept for one who died or left the game in the fight.
- **An ordinary player** can do everything: the sluices are turned by hand, the bait is standing in a trough, and a hot Tyrant still takes half damage from any weapon.

### Rules kept

He follows the rules every boss follows (above):
- his kiln is his arena, and resets when everyone leaves;
- every big attack is read before it lands;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- his health scales with the party, and every participant rolls his loot;
- nothing in his kiln can be built or broken, and the only blocks that change are his own troughs and slag, which come back;
- every opening costs a Kiln Seal.

Nothing a tier needs comes only from him: Fire Resistance and magma cream come from brewing and the Nether as ever.

## The Mire Hag: the plan (being built)

The third of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md) as the Yeti King and the Cinder Tyrant were: first her lair and the summoning that opens it, then the Hag herself and her loot, each its own pull request with tests and pictures. On 13 October 2026 the owner chose her as the next boss ("Go with recommendations" on the Cinder Tyrant's checkpoint). Part 1, the fen and the stone: [features/lantern-fen.md](../features/lantern-fen.md). Part 2, the Hag and her loot: [features/mire-hag.md](../features/mire-hag.md).

The design joins two sources:
- this page's own idea for her: a witch who hides in the fog among illusions of herself, throws poison brews and binds players with roots, and must be struck where she truly is, the one who casts a shadow;
- the planning pack's "Lantern Fen" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)): a shallow marsh with firm boardwalks and four lamps; the real Hag leaves a different ripple and answers to light, and relighting a lamp reveals her; her decoys share a tell that anything thrown can test, and never drop anything; root lanes, a lobbed pool, a decoy exchange and a close swipe; and at 55% two lamps go out while a knot of roots crawls toward a third.

The numbers here are the plan's. Each part's feature record says what changed in building it.

*A crone of the deep fen, bent nearly double and a head taller than a man even so, wrapped in a shawl of grey moss over rotted sackcloth, sedge and fen-roots grown through her hair. Her fingers are long and knotted as roots, her eyes the sickly green of marsh-light, and she leans on a crooked staff of bog oak with a little lantern swinging from its crook.* She leads travellers into the fen by its light, and by her likenesses.

### The Lantern Fen

A drowned fen floating in a night of mist:
- **The arrival:** the end of a jetty on the fen's south edge, a sunken punt at its mooring. Behind it, Grey Mist hangs between two dead willows whose crowns have grown together: the way home.
- **The marsh, the arena:** 44 blocks across, black water knee-deep over mud, with tussocks of sedge, reeds and cattails, lily pads, and here and there a drowned stump. Wading is slow.
- **The boardwalks:** walkways of willow planks two blocks wide, laid a block over the mud, so the water lies just under them. One runs from the jetty north to the hut, and a ring joins four lamp platforms round the marsh. They are firm and quick.
- **The lamps:** four great lanterns on posts, one on each platform (north-west, north-east, south-west and south-east), burning. Any player can light one that has gone out, by using it.
- **The hut:** the Hag's hut on stilts at the north edge, its porch over the water facing the marsh, her cauldron on it. She waits there.
- **Round it:** banks of reeds and cattails, dead willows hung with moss and vine, toadstools; beyond them, mist over open water to the fen's edge.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the lamps can be worked, by any player in the fen.

### The summoning: the Hag Stone

1. Craft a **Hag Stone**: a flint with a hole worn through it, threaded on string, bound with two gold ingots and two blocks of mud, and set with a fermented spider eye.
2. At night, standing in the shallow water or on the mud of a wetland (the Bog, the Quagmire, the Sludge Mire, the Hallowed Bog, the Marsh, the Fen, the Dead Swamp, the Lush Swamp, the Swamp Woods, or vanilla's swamps), look through it.
3. Through its hole you see a lantern burning far off in mist. The water stills, mist rises from it with a marsh-light bobbing in it, and the fen takes you: you wake on the jetty. The stone is used up.
4. For 60 seconds the mist stays, and anyone who uses it follows. Nobody is taken in by standing near it.
5. By day, out of the water and off the mud, out of the wetlands, or outside the Overworld, the stone only says why, and nothing is used up.

Like the Yeti King, she is not a Witching Season boss: the stone works all year, at night.

### The fight

The Mire Hag has 360 health, scaled for the party: half as much again for each player after the first, at most two and a half times. She has armour 6: the hunt is in finding her, not in wearing her down. Poison, drowning, knockback and falls never harm her, and she wades the fen as if it were dry land.

She waits on her porch, bent over her cauldron. She wakes when a player who can fight her steps off the jetty into the marsh, or strikes her: she looks up, the lamps gutter, and she steps down into the water as the mist rises (2 s, unhurt meanwhile). She keeps to her fen: more than 30 blocks from the marsh's middle, she glides back.

**Her rule: the light finds her.** She is never alone. Her **Likenesses** walk the fen with her, two at first: each looks just as she does and mirrors her gestures, but casts no shadow and leaves no ripple where it wades. She casts a shadow, and the water rings round her feet.
- Anything striking a Likeness (a blow, an arrow, a thrown snowball or egg) bursts it into a puff of marsh gas, harmlessly, and another forms from the mist a while later.
- Within 8 blocks of a burning lamp her Likenesses gutter and fade, and she cannot hide: her outline shows.
- Lighting a lamp that has gone out shows her for 5 seconds wherever she is, and bursts every Likeness in its light.

**Phase 1, "The Lure"** (full to 55% health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Clawing Swipe | She draws back her hand of roots (0.6 s) | Claws across her front, 3 blocks: 8 damage. Close in, her most frequent attack |
| Root Lanes | She plunges her staff into the water, and lines of bubbles race out toward her foe (1.2 s) | Roots burst up along three lanes 12 blocks long: 6 damage, and held fast for 1.5 s. Never twice running on one player |
| Poison Brew | She draws a green flask from her shawl (0.8 s) | Lobbed at her foe, it shatters into a pool of venom 3 blocks across for 6 s: Poison while in it, which never kills |
| Likeness Exchange | She and her Likenesses shimmer as the mist thickens (2 s) | They trade places in a burst of mist, and any Likeness burst comes back |

**At 55% health, "Lights Out":**
- she cackles for 3 seconds, unhurt, and two lamps gutter out;
- a **Root Knot** bursts from the water beside a third lamp's boardwalk and crawls along it toward the lamp: if it reaches the lamp, that lamp goes out too. It has 30 health; cut it down and the lamp keeps burning;
- then the fight changes.

**Phase 2, "The Drowned Dark"** (55% to a fifth): her Likenesses are up to four, and she keeps every attack and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Mire Grasp | The water churns round her foe (1.2 s) | Hands of mud reach up in a ring 3 blocks across: 6 damage, and slowed for 2 s |
| Snuff | She pinches the wick of her lantern, and the nearest lamp flickers (1.5 s) | That lamp goes out; at most one every 20 seconds |

**Below a fifth, "The Last Light":** her lantern flares and she can hide no longer: her Likenesses burst and do not come back, and her outline shows. Every cooldown, and her pause between attacks, is a third shorter.

**Left alone** for 10 seconds, she climbs back onto her porch, healed. Her Likenesses burst, the lamps relight, the pools and roots are gone, and who hurt her is forgotten.

### Her loot (per participant)

A participant is a player who hurt her, her Likenesses or the Root Knot, and is within 64 blocks of the marsh's middle when she falls.

| Loot | Chance |
| --- | --- |
| **Hagmoss**, grey moss from her shawl | 3 to 6, always |
| **A trophy:** the Hagthorn or the Bogfang (her `bosses/mire_hag` table, one of the two) | 15%; certain on a player's first kill |
| **The Hag's Eye** | 25% |
| **The Hag's Shawl** | 20% |
| 300 experience, shared out | always |
| The advancement **Seen Through** | always |

- **Hagmoss:** a cheaper Hag Stone, her part of what summons her; and for those who never fight her, light grey dye or compost (settled in part 2).
- **The Hag's Eye:** a hag stone that has seen through her. Held in the offhand, it makes invisible creatures near you glow.
- **The Hag's Shawl:** a costume worn on the head, her hood of grey moss and sedge.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Venom boon; until now they were in the creative tab only.
- When she falls, her lantern goes out, the mist lifts, and Grey Mist opens in the marsh's middle, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the jetty is safe. She wakes when a player steps off it into the marsh or strikes her. Grey Mist hangs at the jetty's end, and in the marsh's middle when she falls.
- **Fixtures:** the four lamps, which any player in the fen may light, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the fen owns it. Only the lamps change, going out and being lit, and every lamp burns again when she resets; each instance is placed fresh. Her roots, pools and grasping hands are hers, not blocks.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or she is left alone for 10 seconds.
- **Clean-up:** her Likenesses, the Root Knot, her brews' pools, roots and hands are hers, capped, and gone with her.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when she falls.
- **An ordinary player** can do everything: a lamp is lit by hand, a Likeness bursts at any blow or anything thrown, her shadow shows with particles off, and her roots, pools and grasps are read before they land. Her poison never kills, and milk or the chemical reactor's Antidote cures it.

Of the planning pack's magic links, Radiance's is already in the game: its Revelation makes a creature glow, so cast on her it shows her as a lamp does. The others (Verdance clearing a path through her roots, Tether cutting the knot, Tide thinning a pool) wait for spells of those schools. None of them will be needed.

### Rules kept

She follows the rules every boss follows (above):
- her fen is her arena, and resets when everyone leaves;
- every big attack is read before it lands;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- her health scales with the party, and every participant rolls her loot;
- nothing in her fen can be built or broken, and the only blocks that change are her own lamps, which burn again;
- every opening costs a Hag Stone.

Nothing a tier needs comes only from her.

## The Crypt Lich: the plan (being built)

The fourth of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md) as the bosses before him were: first his lair and the summoning that opens it, then the Lich himself and his loot, each its own pull request with tests and pictures. On 13 October 2026 the owner chose him as the next boss ("go with recommendations" on the Mire Hag's checkpoint). Part 1, the archive and the writ: [features/crypt-archive.md](../features/crypt-archive.md). Part 2, the Lich and his loot: [features/crypt-lich.md](../features/crypt-lich.md).

The design joins two sources:
- this page's own idea for him: a crowned skeleton sorcerer below a crypt, who raises the restless dead in waves, drains life with a soul beam, and hides his heart in a phylactery that must be broken first;
- the planning pack's "Archive of Last Names" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)): three sarcophagus alcoves, a central lectern and four clear lanes; three links protect the Lich, and the vulnerable one is shown by a lit inscription with a rune on it, so symbols and text, not colour alone, tell them apart; a wrong link answers with a short, recoverable add, at most four; named bone lanes, a slow soul projectile that can be struck back, a grave pull that leaves a marked way out, and a modest sweep; and at half health he erases an inscription, which the lectern restores.

The numbers here are the plan's. Each part's feature record says what changed in building it.

*A crowned skeleton, gaunt and a head taller than a man, in the mouldering robe of a scholar hung with chains of tarnished gold. His crown is black iron set with soul-fire, his eyes are cold blue flames, and his staff is a thighbone bound to a sceptre.* He was the sexton who dug the graves, then the scribe who kept their names, and at last he wrote his own name out of death's ledger and crowned himself. He keeps the Archive of Last Names below the graves, and his heart in a phylactery bound by the three names he bore: **the Sexton**, **the Scribe** and **the Crowned**.

### The Archive of Last Names

A vaulted crypt deep under the graves, lit by soul-fire:
- **The arrival:** a landing at the top of a stair, at the hall's south end, where the stair from the opened sarcophagus comes down. Behind it, Grey Mist fills the stair back up: the way home. The landing and the stair are safe.
- **The hall, the arena:** about 40 blocks square under a ribbed vault, floored with old flagstones and ledger stones (the floor graves, where his dead rise). Four pairs of thick pillars stand in its quarters, cover from his soul beam.
- **The four lanes:** paved walks three blocks wide, inlaid with bone, from the central dais to the three alcoves (west, north and east) and south to the lectern. They are kept clear.
- **The three alcoves:** one for each of his names, the Sexton's to the west, the Crowned's to the north behind his throne, the Scribe's to the east. In each a stone sarcophagus lies with its head to the back wall, and on that wall a carved **name-stone**: the name, and its rune (a spade, a crown, a quill). Under it a **grave seal** bears the same rune.
- **The dais:** at the hall's heart, a round step with his **phylactery** on a plinth: an urn of bone and gold with soul-fire in it.
- **The throne:** in the north apse, raised on steps, before the Crowned's alcove. He waits there.
- **The Lectern of Last Names:** at the foot of the stair, the archive's great book open on it. Any player can read it.
- **Round it:** the walls are the archive: shelves of ledgers and ribcage bookcases, ossuary niches of skulls and bones, candles in skulls, soul lanterns hung on chains.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the grave seals and the lectern can be used, by any player in the archive.

### The summoning: the Writ of Exhumation

1. Craft a **Writ of Exhumation**: three paper over two bones and a bottle of ectoplasm (from a restless spirit, by the spirit lantern), signed with an ink sac.
2. Serve it on a stone sarcophagus (stone brick, deepslate or blackstone) that lies in a crypt: under a roof, where no sky can be seen from its lid. Any hour, all year: underground, night and day are alike.
3. The lid grinds aside, cold blue fire and grey mist well up from the dark inside, and a stair goes down: you wake on the archive's landing. The writ is used up.
4. For 60 seconds the mist stays over the open sarcophagus, and anyone who uses it follows. Nobody is taken in by standing near it. Then the lid grinds shut.
5. Under the open sky, outside the Overworld, or on a sarcophagus whose mist is already rising, the writ only says why, and nothing is used up.

Like the Yeti King and the Mire Hag, he is not a Witching Season boss. There is no generated crypt yet: the crypt is any a player builds, or any cellar under a graveyard.

### The fight

The Crypt Lich has 400 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 8. Wither, poison, drowning, falls and knockback never harm him.

He waits on his throne, seated, his crown dark. He wakes when a player who can fight him steps off the stair onto the hall's floor, or strikes him: the soul-fire flares down the lanes, and he rises (2 s, unhurt meanwhile). He keeps to his hall: more than 30 blocks from the dais, he glides back.

**His rule: the phylactery.** Three **soul chains** of blue fire run from the grave seals to his phylactery, and a fourth from it to him. While any chain holds, he is **warded**: every blow, arrow and spell is turned aside with a flare of the chains, and does nothing.
- One name at a time is **lit**: its stone glows and its rune burns, its seal flickers with soul-fire, and the bar over the screen names it ("He answers to the Scribe").
- **Use the lit name's seal** and it breaks: that chain snaps, and the next name lights.
- **Use a seal whose name is dark,** and it holds: a Restless Dead claws its way out of that sarcophagus instead.
- **Break all three,** and the phylactery cracks and the fourth chain snaps: he is **exposed** for 12 seconds, staggered for the first 2. Then the chains reforge (1.5 s, the seals relighting one by one) and the names light again in a new order.

**Phase 1, "The Warded King"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Grave Sweep | He draws his staff back over his shoulder (0.7 s) | Sweeps 3.5 blocks across his front: 7 damage, and thrown back. Close in |
| Bone Lanes | He calls one of his names, and the lane to its alcove cracks with blue fire (1.2 s) | Bones burst up the whole lane: 6 damage, and tossed up. He calls the lane nearest his foe; off the lane is safe |
| Soul Bolt | A skull of soul-fire forms in his hand (0.8 s) | It drifts at his foe, slow and turning a little: 6 damage, and Wither for 3 s. Struck back (any blow or arrow) it flies at him: warded, it staggers him and spoils his next attack; exposed, 10 damage |
| Raise the Dead | He spreads his arms and the floor graves glow (1.5 s) | Two Restless Dead climb out of the graves nearest his foe; at most four at a time |

**At half health, "The Erasure":**
- he shrieks for 2 seconds, unhurt, and the chains reforge at once;
- from now on, whenever they reforge, he **scrapes one name from its stone**. That name comes last, and when it lights its stone is blank: its seal will not break until somebody **reads the name back at the Lectern of Last Names** (using it), or 30 seconds pass;
- then the fight changes.

**Phase 2, "The Unwritten"** (half to a fifth): he keeps every attack, calls two lanes at once, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Soul Drain | A thin beam of blue light joins him to his foe (1.0 s) | For 3 s it drains 1 health every half second, 6 in all, and heals him as much. A pillar between them, or 16 blocks, breaks it |
| Grave Pull | A ring of soul-fire 10 blocks wide flares round him, one sixth of it dark (1.5 s) | Everyone in the ring but those in its dark part is dragged to him, and bones burst round his feet half a second later: 8 damage within 3.5 blocks. The dark part is the way out |

**Below a fifth, "The Last Name":** his phylactery shatters, the chains are gone for good, and he is exposed to the end. Every cooldown, and his pause between attacks, is a third shorter, and his dead rise every 12 seconds.

**Left alone** for 10 seconds, he climbs back to his throne, healed. His dead crumble, the chains reforge, every name is restored, and who hurt him is forgotten.

### His loot (per participant)

A participant is a player who hurt him or his dead, broke one of his seals, or read a name back at the lectern, and is within 64 blocks of the dais when he falls.

| Loot | Chance |
| --- | --- |
| **Grave Dust**, the dust of his bones | 3 to 6, always |
| **A trophy:** the Soulreaver or the Gravewarden (his `bosses/crypt_lich` table, one of the two) | 15%; certain on a player's first kill |
| **The Emptied Phylactery** | 25% |
| **The Lich's Crown** | 20% |
| 300 experience, shared out | always |
| The advancement **Nameless** | always |

- **Grave Dust:** a cheaper Writ of Exhumation, his part of what summons him; and for those who never fight him, bone meal for the garden (settled in part 2).
- **The Emptied Phylactery:** his urn, cracked and empty. Held in the offhand, Wither never takes hold of you.
- **The Lich's Crown:** a costume worn on the head, his crown of black iron with the soul-fire guttering in it.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Drain and Wither boons; until now they were in the creative tab only.
- When he falls, his crown rolls from his skull, the soul-fire goes out down the lanes, and Grey Mist opens on the dais, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the landing and the stair are safe. He wakes when a player steps off the stair onto the hall's floor or strikes him. Grey Mist fills the stair back up, and opens on the dais when he falls.
- **Fixtures:** the three grave seals and the Lectern of Last Names, which any player in the archive may use, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the archive owns it. Only the fixtures change (a seal broken, a name lit, scraped or restored, the phylactery cracked), and all of them are put back when he resets; each instance is placed fresh. His bones, bolts and dead are his, not blocks.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his dead, bolts, bone lanes and chains are his, capped, and gone with him.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: a seal is broken and a name read by using them, the lit name is shown by its rune, its glow and the words over the screen, his bolts are struck back with any weapon or arrow, and every attack is read before it lands. Alone, a player breaks two seals, reads the scraped name back, and breaks the third.

Of the planning pack's magic links, Radiance's is already in the game: its Revelation makes him glow, though it does not break his ward. The others (Echo reading a scraped name back from afar, Hollow turning one of his bolts, the Circlewrights holding an alcove's chain) wait for spells of those schools. None of them will be needed.

### Rules kept

He follows the rules every boss follows (above):
- his archive is his arena, and resets when everyone leaves;
- every big attack is read before it lands, and he is never warded for good: every round of seals opens him;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- his health scales with the party, and every participant rolls his loot;
- nothing in his archive can be built or broken, and the only blocks that change are his own seals, stones and phylactery, which are put back;
- every opening costs a Writ of Exhumation.

Nothing a tier needs comes only from him.

## The Iron Dreadnought: the plan (being built)

The fifth of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md) as the bosses before him were: first his lair and the summoning that opens it, then the Dreadnought himself and his loot, each its own pull request with tests and pictures. On 14 October 2026 the owner chose him as the next boss ("go with recommendations" on the Crypt Lich's checkpoint). He is the first boss of the dieselpunk tiers. The planning pack asked that he wait until the industrial materials and the guns were settled; the October 13 integration (#352) brought the chemistry, the fuels and nine guns into `main`, and his summoning and loot use only what is there.

The design joins two sources:
- this page's own idea for him: a walking war machine bigger than the landship, with steam vents and tesla coils, which charges in straight lines, arcs its coils between players who stand close, and whose weak vents glow before they open;
- the planning pack's "Foundry Trench" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)): a ruined industrial test pit with three grounded pylons, two service gantries and permanent cover; an announced coil charge picks a pylon, and players throw a fixture to route the discharge into the ground, which overloads his armour and opens his vents for six seconds; a piston punch, a rivet fan, a telegraphed mortar and a straight charge; and at half health a terminal fouled by a part of him, which a short interaction clears, always leaving at least two good routes.

The numbers here are the plan's. Each part's feature record says what changed in building it. Part 1, the trench and the orders: [features/foundry-trench.md](../features/foundry-trench.md); part 2, the Dreadnought and his loot: [features/iron-dreadnought.md](../features/iron-dreadnought.md).

*A walking fortress of riveted gunmetal, five blocks wide and six tall, on two piston legs. Olive-drab plates worn bright at the edges, yellow-and-black hazard stripes on its ram, two ribbed tesla coils on its shoulders, a battery of rivet guns under steel shutters, a mortar in its back and a searchlight for an eye.* He was built in the foundry behind the lines to break them, and never sent; the test pit he was proved in became his ground, and he keeps it still, waiting in his assembly gantry for orders.

### The Foundry Trench

A ruined test pit sunk behind the front line, under a sky of drifting smoke:
- **The arrival:** a timber-shored **dugout** two steps down off the fire trench, under a corrugated iron roof: bunks, a lamp, a map table and the telephone the orders came by. In its back doorway, Grey Mist: the way home. The dugout and the fire trench are safe.
- **The fire trench:** along the west half of the pit's south rim: sandbags on its parapet over the pit, timber revetments, duckboards and a periscope. From its west end a ramp of sandbag steps goes down into the pit's south-west corner. Barbed wire runs round the rest of the rim.
- **The pit, the arena:** 47 blocks by 41 of blast-scorched test-bed concrete, eight blocks below the rim. Three concrete **bastions** stand in three of its quarters, and in the fourth the burnt-out hull of a landship on a rail spur: **permanent cover**, which stops his rivets and stuns him if he charges into it.
- **The three pylons:** grounded discharge towers of steel lattice at the pit's west, east and south, each with a tesla crown at its top. At the foot of each a **breaker** (a knife switch on a steel cabinet) faces the pit, and before it a copper **ground terminal** is set in the floor. A lane of bare concrete with a hazard stripe down its middle runs from the pit's centre to each, and nothing stands on them.
- **The two service gantries:** steel catwalks along the west and east walls, five blocks up, with ladders at both ends: a way round him, and a view of the pylons.
- **The assembly gantry:** at the pit's north end, a steel cradle with a crane over it, where he stands dormant. He waits there.
- **Round it:** the pit walls are reinforced concrete with blast doors and searchlights on the rim; smoke stacks lean over the far side.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the three breakers can be used, by any player in the trench.

### The summoning: Sealed Orders

1. Craft **Sealed Orders**: three paper over a steel plate and copper wire, sealed with a honeycomb.
2. Use them on a **Field Telephone** that stands in a trench: under the open sky, with at least four trench-works blocks (sandbags, timber revetment, duckboards, and their slabs and stairs) within three blocks of it. Any hour, all year.
3. The bell rings, a whistle shrills down the line, and you go over the top: you wake in the Foundry Trench's dugout. The orders are used up.
4. For 60 seconds the line stays open and the telephone's bell keeps ringing; anyone who uses the telephone follows. Nobody is taken in by standing near it. Then the line goes dead.
5. Under a roof, with no trench round the telephone, outside the Overworld, or on a telephone whose line is already open, the orders only say why, and nothing is used up.

He is not a Witching Season boss. There is no generated front line: the trench is any a player digs and lines with trench works.

### The fight

The Iron Dreadnought has 440 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 12. Poison, Wither, drowning, falls and knockback never harm him.

He stands dormant in his gantry, his searchlight dark. He wakes when a player who can fight him steps off the ramp onto the pit's floor, or strikes him: his boilers fire and his stacks smoke (2.5 s, unhurt meanwhile), his eye lights, and he steps down from the gantry. He keeps to his pit: more than 34 blocks from its centre, he walks back.

**His rule: the discharge.** His plating is thick: while his vents are shut, every blow does a quarter of its damage.
- Every 20 seconds he **charges his coils**: he plants his legs and his coils whine, and one pylon's crown crackles with violet light, its lamp burns amber, and the bar over the screen names it ("Discharge to the west pylon"). He picks the pylon nearest the player he is fighting. The charge takes 3 seconds.
- **Throw that pylon's breaker** before it ends (use it), and the bolt is drawn into the pylon and earthed through its terminal, and the surge comes back into him: he **overloads**. His vents blow open, glowing orange, and for 6 seconds every blow does full damage; he staggers for the first 1.5. Then the vents clang shut.
- **Leave it,** and the discharge runs out along that pylon's lane: 8 damage to every player within 3 blocks of the lane, and his plating holds.
- A breaker whose pylon is dark only sparks. Every breaker springs open again after the discharge.

**Phase 1, "The Advance"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Piston Punch | His arm draws back on its piston with a hiss of steam (0.8 s) | A blow 4 blocks across his front: 9 damage, and thrown back hard. Step aside |
| Rivet Fan | The shutters on his shoulder battery snap open (0.9 s) | Five hot rivets in a fan at his foe: 4 damage each. Cover stops them |
| Trench Charge | He lowers his ram, his stacks belch black smoke and the dust kicks up ahead of him (1.2 s) | He charges straight ahead up to 24 blocks: 10 damage, and thrown aside. Run into cover or a pylon's footing, and he is stunned for 2 seconds with a vent knocked open: full damage meanwhile |
| Coil Arc | His coils crackle and a thread of violet light reaches for his foe (1.0 s) | 5 damage to his foe, then the arc jumps to the nearest other player within 4 blocks, and on, up to three jumps, 5 damage each. Spread out |

**At half health, "The Torn Plate":**
- he grinds to a halt for 2 seconds, unhurt, and tears a plate from his hull;
- he hurls it at a pylon, and its breaker is **fouled**: jammed with the plate, it will not throw until somebody clears it (using it three times knocks the plate loose). Each time his vents shut, he fouls another;
- from now on his charge lights **two** pylons, and either breaker routes it. At least two of the three are always clear;
- then the fight changes.

**Phase 2, "The Overcharge"** (half to a fifth): he keeps every attack, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Mortar | He drops to one knee and the hatch on his back swings open; red smoke marks the ground at his foe's feet (1.5 s) | A shell lands on the mark: 10 damage within 3 blocks. Move off the mark |
| Blowdown | His stacks shriek and steam hisses from round his feet (1.0 s) | A burst of scalding steam 4 blocks round him: 6 damage, and slowed for 2 s. Back off |
| Sparkers | A hatch in his belly drops open (1.2 s) | Two **Sparkers**, little coil crawlers, scuttle at the nearest players and discharge against them (4 damage) unless they are struck first (6 health); at most four at a time |

**Below a fifth, "Red Line":** his boilers redline, his vents jam open for good, and every blow does full damage to the end. Every cooldown, and his pause between attacks, is a third shorter, and he charges his coils every 12 seconds.

**Left alone** for 10 seconds, he walks back to his gantry, healed. His Sparkers burn out, the fouled breakers are cleared, the pylons go dark, and who hurt him is forgotten.

### His loot (per participant)

A participant is a player who hurt him or his Sparkers, threw a breaker that routed a discharge, or cleared a fouled one, and is within 64 blocks of the pit's centre when he falls.

| Loot | Chance |
| --- | --- |
| **Salvaged Coil**, copper wound from his tesla coils | 3 to 6, always |
| **A trophy:** the Dynamo Halberd or the Piston Hammer (his `bosses/iron_dreadnought` table, one of the two) | 15%; certain on a player's first kill |
| **The Grounding Strap** | 25% |
| **The Dreadnought Visor** | 20% |
| 300 experience, shared out | always |
| The advancement **Grounded** | always |

- **Salvaged Coil:** cheaper Sealed Orders, his part of what calls him up; and for those who never fight him, copper wire for the workshop (settled in part 2, with no loop that gains).
- **The Grounding Strap:** a braided copper strap and clamp. Held in the offhand, it halves the harm done to you by lightning and electric arcs.
- **The Dreadnought Visor:** a costume worn on the head, his riveted visor with the searchlight in it.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Shock boon; until now they were in the creative tab only.
- When he falls, he sags in his tracks with a long hiss of steam, his eye goes dark, and Grey Mist opens in the gantry he came from, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the dugout and the fire trench are safe. He wakes when a player steps off the ramp onto the pit's floor or strikes him. Grey Mist fills the dugout's steps back up, and opens in his gantry when he falls.
- **Fixtures:** the three breakers, which any player in the trench may use, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the trench owns it. Only the fixtures change (a pylon lit, a breaker thrown or fouled), and all of them are put back when he resets; each instance is placed fresh. His rivets, shells, steam and Sparkers are his, not blocks, and break nothing.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his Sparkers, rivets, shells and charges are his, capped, and gone with him.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: a breaker is thrown and cleared by using it, the live pylon is shown by its crackling crown, its amber lamp and the words over the screen, cover is there for anyone, and every attack is read before it lands. Alone, a player can reach the lit pylon in the charge's 3 seconds, since he picks the pylon nearest them.

Of the planning pack's magic links (a Tempest spell priming a terminal, Strata bracing against a piston, Clockheart helpers working a breaker, Tide cooling a vent), none is in the game yet: they wait for spells of those schools. None of them will be needed.

### Rules kept

He follows the rules every boss follows (above):
- his trench is his arena, and resets when everyone leaves;
- every big attack is read before it lands, and he is never plated for good: every routed discharge opens him, and so does a charge into cover;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- his health scales with the party, and every participant rolls his loot;
- nothing in his trench can be built or broken, and the only blocks that change are his own pylons and breakers, which are put back;
- every opening costs Sealed Orders.

Nothing a tier needs comes only from him: first electricity, steel, actuators and the chemistry are all made without him.

## The Alpha Werewolf: the plan (being built)

The sixth of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md) as the bosses before him were: first his lair and the summoning that opens it, then the Alpha himself, his pack and his loot, each its own pull request with tests and pictures. On 14 October 2026 the owner chose him as the next boss ("Ok lets do the alpha werewolf boss 8"), after the Iron Dreadnought's checkpoint.

The design joins three sources:
- this page's own idea for him: the pack leader of the werewolves already in the world, twice their size, who leaps between trees, howls to weaken players and call the pack, and flees to heal at dawn;
- the planning pack's "Moonlit Pursuit" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)): a moonlit woodland ring with clear paths and three bell clearings; the pack marks a target before a hunt, and ringing the right bell breaks its coordination and opens a recovery window; a 0.7-second claw combination, a 1.2-second pounce shadow, a marked howl and at most three pack allies; at low health he changes his pursuit route rather than simply getting faster; his ground trails stay visible without particles; and the invitation must not be locked to the calendar;
- the werewolves of the woods ([features/even-more-fall-additions.md](../features/even-more-fall-additions.md), fall addition 23): their hide that shrugs off half of every blow but silver's, their pelts, and wolfsbane, which wards them off.

The numbers here are the plan's. Each part's feature record says what changed in building it. Part 1, the ring and the totem: [features/moonlit-ring.md](../features/moonlit-ring.md); part 2, the Alpha, his pack and his loot, will have `features/alpha-werewolf.md`.

*A werewolf twice the height of a man, the werewolves' build made massive: a silver-grey mane over a hunched black-furred back, scarred muzzle, a torn ear, claws like sickles and eyes that burn moon-white.* He is the oldest of the werewolves, the one the others answer when they howl. His hunting ground is a ring of giant oaks under a moon that never sets, where the old wardens hung bells to warn of the pack, and nobody has rung them in a long while.

### The Moonlit Ring

A clearing in an old wood, under a huge full moon:
- **The arrival:** the **Hunters' Camp**, a hollow at the ring's south edge under the trees: a lean-to of hides on spruce poles, a cold firepit ringed with stones, werewolf rugs on the ground and a stump with a lantern on it. Behind it the trail back through the trees fills with Grey Mist: the way home. The camp is safe.
- **The ring, the arena:** a clearing about 44 blocks across of moonlit grass and moss, walled all round by the trunks of **giant oaks** standing too close to pass between. Their canopy closes overhead round the edge and opens in the middle onto the moon.
- **Clear paths:** a path of trodden earth runs round the ring just inside the trees, and three spokes run from the centre to the bell clearings. Between them grow grass and low ferns, with a few fallen logs and boulders for low cover. Nothing blocks a run from the centre to any bell.
- **The three bell clearings:** at the ring's west, north and east, each a small glade let into the tree wall. In each hangs a **Warden Bell**, an old silver alarm bell under a shingled frame on two posts, with a pull rope. Any player in the ring may ring them. A bell rung swings for 2 seconds, and cannot be rung again until it stops.
- **The Howling Stone:** at the centre, a grey outcrop three blocks high with a flat, mossy top, where he crouches waiting, head raised to the moon.
- **The moon:** a huge full moon, pale gold, low over the northern trees.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the three Warden Bells can be used. One more lair-only block, **raked turf** (moss torn by his claws), is the trail he leaves; it knits back by itself (part 2).

### The summoning: the Howling Totem

1. Craft a **Howling Totem**: a werewolf's pelt (any kind) wrapped round three bones and bound with two silver ingots.
2. Use it standing on the woodland floor (earth a tree could grow on: grass, dirt, podzol or moss, under snow or not) **at night** (any night, not only a full moon, all year), **in werewolf country** (the Overworld's woods: biome tag `jugcraft:werewolf_haunts`), under the open sky (nothing over your head, not even leaves).
3. The totem howls at the moon and something far off answers: you come to in the Moonlit Ring's camp. The totem is used up, and Grey Mist rises where it stood: for 60 seconds anyone can walk into the mist and follow. Nobody is taken in by standing near it.
4. By day, under a roof, outside werewolf country or the Overworld, or within 6 blocks of growing wolfsbane ("the wolfsbane keeps them away"), the totem only says why, and nothing is used up; likewise if every ring is taken.

Werewolf pelts come from the werewolves of full-moon nights and keep, so a player who has hunted one full moon can call him up on any night after.

### The fight

The Alpha Werewolf has 400 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 8. Falls and knockback never harm or move him much: he is heavy.

**His hide**, like his kind's: every blow but silver's (a weapon in `jugcraft:silver_weapons`, or a silver arrow) does half its damage. Silver does full damage. Silver is a help, never a need: the bells open him to every weapon.

He crouches on the Howling Stone, head raised to the moon, eyes dark. He wakes when a player who can fight him steps off the camp onto the ring's turf, or strikes him: he rises and howls at the moon (2.5 s, unhurt meanwhile), his eyes blaze, and his pack, three **Packmates**, lope out of the trees. He keeps to the ring: more than 26 blocks from its centre, he is set back.

**His rule: the Hunt.** Every 24 seconds he hunts:
- **He marks his quarry:** he throws back his head in a short, rising howl (1.0 s), and the bar over the screen names the player he was fighting ("The pack hunts Steve").
- **He goes to ground:** he bounds off into the trees and hides in one of the three bell clearings, never the one his quarry stands nearest. His claws tear the turf as he runs, so a **trail of raked turf** leads from where he was to where he hides, and his eyes glint out of that clearing's dark.
- **His pack drives the quarry:** the Packmates close in on the quarry, snapping at their heels.
- **The ambush lasts 10 seconds.** Then he springs: a long leap from the clearing at the quarry wherever they are. A shadow spreads where he will land, 1.2 seconds before: 14 damage within 2.5 blocks of it, and the quarry is knocked flat (Slowness for 1 s).
- **Ring the Warden Bell in his clearing before he springs,** and he is **flushed**: startled into the open, he staggers out (1.5 s), his hide fails and every blow does full damage (silver half as much again) for 8 seconds, and his pack cowers among the trees for 4 seconds.
- A bell in an empty clearing only tolls. Each bell can be rung once every 2 seconds.

From anywhere in the ring, a sprinting player reaches any bell well inside the ambush's 10 seconds, alone or not; the audit will walk the ring to keep that true.

**Phase 1, "The Chase"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Claw Rake | He rears up and spreads his claws (0.7 s) | Two rakes across his front, 3 blocks: 6 damage each. Step back or aside |
| Pounce | He drops into a crouch and a shadow spreads where he will land (1.2 s) | He leaps up to 12 blocks onto the shadow: 9 damage within 2 blocks, and thrown down. Step off the shadow |
| Rending Howl | His hackles rise and he fills his chest (1.0 s) | Weakness for 6 s on players within 10 blocks, and his pack runs faster for 6 s. Get out of earshot |
| The Hunt | His short, rising howl (1.0 s) | Above |

**At half health, "The Pack Gathers":** he leaps onto the Howling Stone and howls long at the moon (2 s, unhurt). His pack is called back up to three, and the fight changes.

**Phase 2, "The Moon's Fury"** (half to a quarter): he keeps every attack, hunts every 20 seconds, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Tree Leap | He springs into the ring trees out of sight; the leaves shiver along his path (1.5 s), then a shadow spreads behind a player (1.2 s) | He drops from the trees onto the shadow: 10 damage within 2.5 blocks. Step off the shadow |
| Hamstring Bite | He lunges low, jaws wide (0.9 s) | A bite on his foe: 8 damage and Slowness II for 2 s. Step back |

**Below a quarter, "The Last Hunt":** he changes his route, not his speed. In every ambush, halfway through, he slips from his clearing through the trees to another, leaving a second trail; the bell to ring is the one in the clearing he is in when it rings. His tells stay as long as ever. He hunts every 16 seconds.

**Left alone** for 10 seconds, he slinks back to his stone to lick his wounds, healed. His pack melts back into the trees, the raked turf knits, and who hurt him is forgotten.

**His pack, the Packmates:** young werewolves of his blood, smaller than he is: 24 health, no hide (silver still does half as much again), a 4-damage bite, fast. At most three at a time; they harry the quarry in a hunt, and his foe otherwise. A sprig of wolfsbane in hand keeps them from biting its holder (they circle and growl), but not him. Killed, they stay dead until he gathers the pack again. They drop nothing and give no experience.

### His loot (per participant)

A participant is a player who hurt him or his pack, or rang the bell that flushed him, and is within 64 blocks of the ring's centre when he falls.

| Loot | Chance |
| --- | --- |
| **Alpha Pelt**, his silver-grey pelt | 1, always |
| **A trophy:** Moonfang or Howler (his `bosses/werewolf_alpha` table, one of the two) | 15%; certain on a player's first kill |
| **The Packfinder Charm** | 25% |
| **The Alpha's Hood** | 20% |
| 300 experience, shared out | always |
| The advancement **Hunter's Moon** | always |

- **Alpha Pelt:** for those who never fight, four leather for the workshop (a werewolf's pelt gives two); and it counts as a werewolf's pelt, so it makes a Howling Totem, one for one, with no loop that gains.
- **The Packfinder Charm:** the planning pack's tracking equipment. Held in the offhand, it marks every werewolf within 32 blocks with a glow (seen by everyone) every 2 seconds: the hunter becomes the hunted on full-moon nights.
- **The Alpha's Hood:** a costume worn on the head, his silver-grey wolf's head.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Howl boon; until now they were in the creative tab only.
- When he falls, he slumps on the turf with a last long howl, his eyes going dark, and Grey Mist opens on the Howling Stone, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the camp is safe. He wakes when a player steps off the camp onto the ring's turf or strikes him. Grey Mist fills the trail behind the camp, and opens on the Howling Stone when he falls.
- **Fixtures:** the three Warden Bells, which any player in the ring may ring, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the ring owns it. Only his trail changes it (raked turf, which knits back by itself, and all of it is put back when he resets); each instance is placed fresh.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his Packmates are his, capped at three, and gone with him.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: a bell is rung by using it, his hiding place is shown by his trail and his eyes, every attack is read before it lands, and the flush opens him to any weapon. Alone, the quarry can reach his bell in time, and silver and wolfsbane only make it easier.

Of the planning pack's magic links (Echo reading the hunt's target, a Verdance refuge, Tether interrupting a pack signal), none is in the game yet: they wait for spells of those schools. None of them will be needed. The planning pack's offered story of being turned into a werewolf is not part of this fight: no blow of his changes a player.

### Rules kept

He follows the rules every boss follows (above):
- his ring is his arena, and resets when everyone leaves;
- every attack is read before it lands, and his hide is never his last word: every bell rung in time opens him to any weapon;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- his health scales with the party, and every participant rolls his loot;
- nothing in his ring can be built or broken, and the only blocks that change are his own trail, which knits back;
- every opening costs a Howling Totem, and a full moon is never required to make or use one.

Nothing a tier needs comes only from him: silver, leather and the werewolves' own pelts all come without him.
## Adam-ondi-Ahman and the Cydonian Sentinel: the plan (being built)

The owner's own boss, proposed on 14 October 2026: a peaceful valley near the start, modelled on Adam-ondi-Ahman, whose ancient altar opens a way to an instanced dungeon on Mars and a hard, multi-phase boss. The owner chose:
- **the real name**, Adam-ondi-Ahman, for the valley and its altar;
- **one valley near the start of every new world, and rare ones elsewhere**, with the rarity a setting;
- **a crafted late-game key** to wake the altar;
- **a boss at the top of the planning pack's envelope**, made harder by its mechanics rather than by numbers outside it.

It is built by [the boss playbook](BOSS_PLAYBOOK.md), in three pull requests rather than two, because the summoning needs a place before it needs a lair:
1. **Part 1, the valley and the altar** (this plan's first section): the biome, the knoll of altars and where they generate. The altar is there, and dormant. Record: [features/adam-ondi-ahman.md](../features/adam-ondi-ahman.md).
2. **Part 2, the Sanctum and the keystone:** the lair on Mars, the Altar Keystone, and the rite that opens the way. Record: [features/cydonian-sanctum.md](../features/cydonian-sanctum.md), which says what changed in building it (among them the Concordance's recipe: dreamglass and an astrolabe).
3. **Part 3, the Sentinel, its adds and its loot.** Record: [features/cydonian-sentinel.md](../features/cydonian-sentinel.md), which says what changed in building it (among them its trophies: the Corelance, a pike, and the Redstar Cleaver, a battle axe).

The numbers here are the plan's. Each part's feature record says what changed in building it.

### The place: Adam-ondi-Ahman

The real Adam-ondi-Ahman stands on the bluffs above the Grand River in northern Missouri, where early accounts describe the ruins of stone altars on its hills, and where Latter-day Saints settled in 1838. Jugcraft's valley is modelled on that landscape:
- **The valley:** a quiet, open river valley about 350 blocks across, of rolling prairie grass and wildflowers, goldenrod and a few springs, with oaks and birches standing alone or in twos and threes. It has four seasons. Monsters are few: every kind that spawns there spawns sparsely (vanilla's spawn costs, as in the soul sand valley). Rivers and lakes through it keep their own biomes, so the river is still a river.
- **The knoll of altars:** at the valley's heart, a grassy knoll rising about nine blocks over the land, with a worn path up its south side. On its crown stand three altars of ancient stone, stacked: a broad, broken footing of mossy rubble; a second, smaller course of cracked and mossy stone blocks; and a third of carved stone. On the top lies the **Ancient Altar**, a stone older than the rest, carved with a red star.
- **The old settlement:** at the knoll's foot, the log footings of two cabins, a fallen chimney, and a spring in a ring of stones.
- **The Ancient Altar** cannot be broken, like an end portal frame. In part 1 it is dormant: used, it only says so ("The altar is dormant. Its oldest stone is carved with a red star."). Everything else on the knoll is ordinary stone, logs and earth.

**Where it is:**
- **Near the start:** one valley in every new world, 800 to 1,600 blocks from the world's start, on suitable land (below). It is chosen once, on the world's first start, after Alpine Spawn and the town have settled, and saved. Only the land's noise is read to choose it, so no chunk is generated to do so.
- **Elsewhere, rare:** the Overworld is divided into cells 3,072 blocks square. Half of them (chosen by the seed) try up to six places, inset from the cell's edges, and the first whose land is suitable becomes a valley. The cell size (`adam_ondi_ahman.spacing`) and the share (`adam_ondi_ahman.chance`) are settings.
- **Suitable land:** inland and dry; temperate (not frozen, not desert-hot); neither the peaks nor a river's bed; and gentle, so the knoll does not sit on a cliff. A place that fails is skipped, and the cell tries its next.
- `/locate biome jugcraft:adam_ondi_ahman` finds the nearest valley.

### The way to Mars: the Altar Keystone (part 2)

1. Craft an **Altar Keystone**. There are two recipes, so that neither specialty is forced through the other. The factories' recipe uses titanium and tungsten ingots and advanced circuits; the Concordance's uses a late reagent of its own. Both are settled in part 2, from things players already make.
2. At night, set it into the Ancient Altar: use it on the altar.
3. The three altars kindle with red fire one after another (3 seconds), and a column of red light rises from the top. A **red mist** opens over the altar, and you step through onto Mars, into the Sanctum's gate chamber. The keystone crumbles to dust.
4. For 60 seconds the mist stays, and anyone who uses it follows. Nobody is taken in by standing near it.
5. By day, with the mist already open, or with every instance of the Sanctum taken, the keystone only says why, and nothing is used up.

It works all year, and is no Witching Season boss.

### The Cydonian Sanctum (part 2)

Ruins under the butterscotch sky of Cydonia on Mars, cut into red rock. Two small moons hang over the crater: Phobos, and Deimos further off. The air is thin and the ground light: in the Sanctum everyone jumps higher (Jump Boost I) and takes half damage from falls.
- **The gate chamber, the arrival:** a hall cut into the rock. The red mist hangs in an arch behind the arrival: the way home. It is safe.
- **The Dust Gallery:** a corridor about 40 blocks long with two hazards and a puzzle:
  - **dust vents** in its floor hiss and puff on a fixed rhythm. Read by a hiss and a rising wisp (1 s), a puff deals 3 damage and throws you back;
  - **the Star Door** at its end is carved with three constellations. Three **star dials** stand along the gallery's walls. Turn each one (use it) until its carved stars match one of the door's; the door's figures light as they are matched, and when all three are lit the door grinds open. Any player can do it.
- **The crater, the arena:** a crater about 44 blocks across of red sand and rust-red rock, ringed by its rim wall.
  - **Four standing stones** stand round it, carved like the altar: shelter from the storm (his rule).
  - **The Sentinel's plinth** is on the north rim, where it waits.
  - **Small craters** scattered on the floor are where his sentries rise.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the star dials and the red mist can be used.

### The fight (part 3)

**The Cydonian Sentinel:** *a construct four blocks tall of red stone plates and rusted iron, built round a floating core of red light. Its arms are pillars, its face a carved visor, and the altar's red star is cut into its chest.*

It has 480 health, scaled for the party: half as much again for each player after the first, at most two and a half times. Its shell gives armour 12. Knockback, falls, fire, drowning, poison and wither never harm it.

It waits on its plinth, a statue with its core dark. It wakes when a player who can fight it steps onto the crater floor, or strikes it: its core flares, and it steps down (2.5 s, unhurt meanwhile). It keeps to its crater: more than 30 blocks from the centre, it strides back.

**Its rule: the Red Storm and the standing stones.** Every 35 seconds (25 from phase 2, 20 in phase 3, 15 in the enrage) it calls the storm.
- **The tell (2.5 s):** it plants its arms and the sky reddens. Dust streams in from one side of the crater, and the bar over the screen names it ("The storm comes from the east").
- **The storm (6 s):** a scouring wind crosses the crater from that side. Anyone not sheltered in the lee of a standing stone, or under the rim on the windward side, takes 2 damage every half second and is pushed downwind.
- **There is always shelter:** the stones are spaced so that from anywhere on the floor the lee of one is within 12 blocks.

**Its weak point: the core.** When it charges its Core Lance, its chest plates open. While they are open its core takes double damage, and its armour does not count. The first blow, arrow or thrown weapon of 6 damage or more during the wind-up staggers it (2 s) and the lance fails.

**Phase 1, "The Sentinel"** (full health to 60%):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Hammerfall | It raises both fists (0.8 s) | A two-fisted slam in front of it: 14 damage within 3 blocks, a knockback within 6 |
| Shard Volley | Rust shards bristle along its arms (0.7 s) | Five shards in a fan at its foe, 24 blocks: 5 damage each |
| Quake Line | It stamps, and a glowing crack runs at its foe (1.0 s) | Spikes of red rock burst up along a 14-block line: 10 damage, thrown up |
| Core Lance | Its chest plates open, the core brightening (1.2 s) | A beam 16 blocks long at its foe for 1.5 s: 4 damage each quarter-second. The core is open (above) |

**At 60%, "The Shell Cracks":** it roars for 3 seconds, unhurt, and two **Shard Sentries** rise from the craters. Then the fight changes.

**Phase 2, "The Red Storm"** (60% to 25%): it keeps every attack, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Sentry Call | It raises a fist and the craters glow (1.0 s) | Two Shard Sentries rise; at most four at a time |
| Storm Shards | During its storm, shards ride the wind (the storm's own tell) | Its Shard Volley thrown downwind reaches the whole crater |

**Shard Sentries:** fist-sized stones circling a red spark, 16 health. They float slowly after the nearest player and shoot a slow shard (3 damage) every 3 seconds. They drop nothing and crumble when it falls, resets or is gone. Whoever hurts one has taken part in its fight.

**At 25%, "The Unbound Core":** its shell bursts apart (3 s, unhurt). The core floats free, a red-hot sphere wreathed in orbiting shards, about two blocks over the floor. With no shell its armour is 4, and it is faster. Its attacks:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Sweeping Lance | It turns, and its light gathers (1.0 s) | A beam sweeps 90° over 2 s: 4 damage each quarter-second. It is always open now, but nothing staggers it |
| Shard Burst | Its orbit spins faster, whining (0.8 s) | Twelve shards fly outward: 6 damage each. A standing stone is cover |
| Starfall | Red marks glow on the ground round each player (2.0 s) | Meteors strike the marks: 10 damage within 2 blocks. Afterwards it sinks to the ground for 3 s, dimmed: the moment for blows |

**Below 10%, "Red Giant":** every cooldown, and its pause between attacks, is a third shorter. Its storm comes every 15 seconds, and its Starfall marks two places round each player.

**Left alone** for 10 seconds, it strides back to its plinth and reforms, healed. Its sentries crumble, and who hurt it is forgotten.

### Its loot (per participant)

A participant is a player who hurt it or its sentries, and is within 64 blocks of the crater's centre when it falls.

| Loot | Chance |
| --- | --- |
| **Cydonian Shards**, red crystal from its shell | 4 to 8, always |
| **A trophy:** one of its two (below) | 15%; certain on a player's first kill |
| **The Lowfall Core** | 25% |
| **The Sentinel's Visor** | 20% |
| 400 experience, shared out | always |
| The advancement **Red Planet** | always |

- **Cydonian Shards:** a cheaper Altar Keystone (its part of what summons it again). For those who never fight it, the Crusher grinds a shard into rare earth oxide, a second source of a material the factories already use. No recipe makes shards back.
- **The Lowfall Core:** held in the offhand, you fall as on Mars: slowly, and with no fall damage.
- **The Sentinel's Visor:** a costume worn on the head, its carved visor in small.
- **The trophies:** it is not one of the first eight, so its two trophies are new, made the way Arms VII's are ([features/arms-vii.md](../features/arms-vii.md)), with a **Rust** boon: a struck foe's armour counts for less for a few seconds. A lance and a war axe are proposed; part 3 settles them.
- When it falls, its core gutters out, the storm dies, and the red mist opens in the crater's middle, the way home.

### The shared lair contract

- **Arrival, staging, waking and return:** the gate chamber and the gallery are safe from it. It wakes when a player steps onto the crater floor or strikes it. The red mist in the gate chamber is always the way home, and it opens in the crater when it falls.
- **Fixtures:** the star dials, the Star Door, which opens itself, and the red mist. Nothing else can be used, built or broken.
- **The floor:** the Sanctum owns it. The Star Door is the only block that changes, and each instance is placed fresh. Its storms, shards, spikes and meteors are its own, not blocks.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or it is left alone for 10 seconds.
- **Clean-up:** its sentries, shards, spikes and meteors are its own, capped, and gone with it.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when it falls.
- **An ordinary player** can do everything: dials are turned by using them, the storm's side is shown by the dust and the words over the screen, the stones are cover from it, its core is open to any weapon, and every attack is read before it lands.

### Rules kept

It follows the rules every boss follows (above):
- its crater is its arena, and resets when everyone leaves;
- every big attack is read before it lands, and there is always shelter from its storm;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- its health scales with the party, and every participant rolls its loot;
- nothing in the Sanctum can be built or broken;
- every opening costs an Altar Keystone.

Nothing a tier needs comes only from it. The valley in the Overworld is never an arena: nothing fights there, and nothing there is changed by the fight.

## The Pumpkin King: the plan (being built)

A Halloween boss, built by [the boss playbook](BOSS_PLAYBOOK.md) as the bosses before him were: first his field and the summoning that opens it, then the King himself, his Jacks and his loot, each its own pull request with tests and pictures. On 23 October 2026 the owner asked for the next Halloween work and chose him, after the seasonal biome audit.

The design joins three sources:
- this page's own idea for him (below, "More bosses to consider"): a giant scarecrow made of the harvest, summoned with heirloom pumpkins in a bounded field, tied to the giant pumpkins, the scarecrows and the Headless Horseman already in the world; never the only source of anything core, and his drops kept after the season;
- the planning pack's later backlog ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md), section 5): "cooperative harvest preparation and protecting a bounded field", avoiding "seasonal-only core ingredients or crop destruction outside the instance";
- the Halloween harvest already in the world ([features/halloween-harvest.md](../features/halloween-harvest.md), [features/halloween-nights.md](../features/halloween-nights.md)): the heirloom pumpkins, the scarecrows and their heads, the crows they keep off, and the Horseman's flaming pumpkins.

**The season.** He is a Witching Season boss, as Vesperine and Madame Tatterlace are ([features/witching-season.md](../features/witching-season.md#multiplayer-persistence-and-seasons)): his crown works on any night of the year by default, and only during the Halloween event when the server sets `lairs.off_season=off`. During the event his kill adds the Witching Season's event loot (`lairs.event_loot`): a roll of seasonal candy and a better chance of his costume. Ending the event takes nothing away.

The numbers here are the plan's. Each part's feature record says what changed in building it. Part 1, the field and the crown: [features/crowfield.md](../features/crowfield.md); part 2, the King, his Jacks and his loot: [features/pumpkin-king.md](../features/pumpkin-king.md#changes-from-the-plan).

*A scarecrow three times the height of a man, stuffed with straw into a patched harvest coat of burlap and sacking, hands of twisted corn husk, a crown of dried vines and corn ears, and for a head a giant carved pumpkin with a fire inside that burns out through its eyes and grin.* He is the oldest scarecrow, the one the crows answer. His field is a pumpkin patch walled with corn under a dusk that never ends, where whatever is planted ripens for him.

### The Crowfield

A pumpkin field at dusk, walled all round with standing corn:
- **The arrival:** the **farmyard**, at the field's south edge: a yard of trodden earth behind a split-rail fence with a farm gate open into the field, a hay cart, a lamp post, a water trough, a bench of pumpkins and a crow on its perch. Behind it a lane between the corn fills with Grey Mist: the way home. The yard is safe.
- **The field, the arena:** a square of tilled earth 45 blocks across, its rows running north and south, ridges of rooted earth between furrows. Pumpkins lie along the ridges, heirlooms among them (white, Jarrahdale, Cinderella, red kuri and kabocha), with a few jack-o'-lanterns glowing; low cover from two stacks of hay bales and a tipped-over cart.
- **The corn:** the field is walled by **Crowfield corn**, three blocks tall and too thick to pass, its tassels over the top.
- **The King's hills:** eight hills of dark loam in a ring round the centre, in a three-by-three grid 13 blocks apart (the centre taken by his post), so each is 13 blocks from the next round the ring. On each grows a **King's Gourd**, a great green gourd on its vine. They are the field's fixtures (part 2 ripens them).
- **The King's Post:** at the centre, a tall post with a crossbar on a mound of hay bales, where he hangs waiting like any scarecrow.
- **The moon:** a vast harvest moon low over the north-western corn; the sky a deep violet, an ember haze along the horizon.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. Only the King's Gourds can be used.

### The summoning: the Harvest Crown

1. Craft a **Harvest Crown**: three heirloom pumpkins (any) over two gold ingots and a hay bale.
2. Use it on a **scarecrow** that wears a head (any pumpkin it can wear), **at night**, in the Overworld, with the scarecrow's head **under the open sky**, **among the harvest**: at least 8 pumpkins or gourds within 6 blocks of it (pumpkins carved or not, jack-o'-lanterns, the heirlooms, the squashes and gourds, giant pumpkins: `#jugcraft:crowfield_harvest`).
3. The crown settles on the scarecrow's head, the crows burst up out of the field crying, and Grey Mist rises where the crowner stands: they come to in the Crowfield's farmyard. The crown is used up; the scarecrow and the crops are untouched. For 60 seconds anyone can walk into the mist and follow. Nobody is taken in by standing near it.
4. By day, outside the Overworld, with the scarecrow bare-headed or roofed over, with too few pumpkins about, or off-season on a server that keeps the rituals to the Halloween event, the crown only says why, and nothing is used up; likewise if every field is taken.

Heirloom pumpkins grow all year from seeds found wild or traded, so the crown is never seasonal.

### The fight

The Pumpkin King has 400 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 6. Falls never harm him.

**Straw burns:** fire (burning, a fire weapon, flaming arrows) hurts him half as much again. Fire is a help, never a need.

He hangs on his post, arms out on the crossbar, his head dark. He wakes when a player who can fight him steps through the farm gate into the field, or strikes him: he tears free of the post (2.5 s, unhurt meanwhile), his head bursts into flame and the crows burst out of the corn. He keeps to the field: more than 26 blocks from its centre, he is set back.

**His rule: the Harvest.** Every 22 seconds he calls the harvest:
- **He calls it:** he raises his arms and the crows cry (1.5 s), and two King's Gourds start to ripen: the one nearest the player he is fighting, and its neighbour round the ring. A ripening gourd swells and turns from green to gold to orange over 10 seconds, glowing, crows circling over it.
- **Pick a ripening gourd** (use it) before it ripens, and it comes off the vine. Its hill is bare until a new green gourd sprouts there 20 seconds later.
- **A gourd that ripens** lights up with a burning face and splits open: a **Jack-o'-Straw** climbs out (at most four at a time; beyond that the gourd feeds him instead, healing 4% of his health).
- **Pick every gourd of a harvest before any lights**, and he is **bereft**: his head's fire gutters, he sags on his straw legs (staggered 2 s), and for 8 seconds every blow does half as much again (fire twice), and his Jacks stand dazed.

From anywhere in the field, a sprinting player reaches every gourd of a harvest well inside its 10 seconds, alone or not: part 1's audit walks the field round the cover, from every open cell to the three gourds of the longest harvest either way round the ring, and keeps that within 80% of the time.

**Phase 1, "The Reaping"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Scythe Sweep | He draws back his scythe of bound corn and a rusted blade (0.8 s) | A sweep across his front, 4.5 blocks: 8 damage. Step back |
| Gourd Toss | He plucks a pumpkin from the rows and lifts it overhead; a ring of embers glows where it will land (1.0 s) | A flaming pumpkin (the Horseman's) at a player up to 20 blocks away: 7 damage within 3 blocks, and set alight for 3 s. Step off the ring |
| Crow Storm | He flings his arms wide and the crows rise from the corn (1.2 s) | The crows stream along a line from him, 14 blocks long and 3 wide, for 2 s: 2 damage every half second and Blindness for 2 s. Step out of the line |
| The Harvest | Arms raised, the crows' cry (1.5 s) | Above |

**At half health, "The Crowning":** he climbs back onto his post and his head flares white (2 s, unhurt). Two Jacks climb out of the rows, and the fight changes.

**Phase 2, "The Hallowed Harvest"** (half to a quarter): he keeps every attack, calls the harvest every 18 seconds, three gourds in a row round the ring from the one nearest his foe, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Vine Snare | He drives a hand into the earth and the vines ripple along a furrow toward a player (1.2 s) | Vines lash up where they stand: 5 damage, rooted for 1.5 s (Slowness IV). Step across the furrow |
| Head Roll | He lifts his burning head from his shoulders and sets it rolling (1.5 s) | It rolls along a line toward a player, 18 blocks long and 2 wide: 10 damage, thrown aside. Step out of the line |

**Below a quarter, "The Last Harvest":** he calls the harvest every 15 seconds, three gourds at a time; they still ripen in 10, and the bereft window and his tells stay as long as ever.

**Left alone** for 10 seconds, he climbs back onto his post, healed. His Jacks crumble to straw, his gourds go green again, and who hurt him is forgotten.

**His Jacks, the Jack-o'-Straws:** little straw men with carved gourds for heads, lit inside: 20 health, a 3-damage swipe, quick on their feet. Fire burns them twice as fast. At most four at a time. They drop nothing and give no experience, and crumble when he falls or resets.

### His loot (per participant)

A participant is a player who hurt him or his Jacks, or picked a gourd of a harvest that left him bereft, and is within 64 blocks of the field's centre when he falls.

| Loot | Chance |
| --- | --- |
| **The King's Ember**, the coal of his head's fire | 1, always |
| **A trophy:** one of two new Arms VII weapons for him, the **Hallowreap** (a scythe) or the **Crowbill** (a war pick), with a new boon, **Fright** (his `bosses/pumpkin_king` table, one of the two) | 15%; certain on a player's first kill |
| **The Crowward Charm** | 25% |
| **The Pumpkin King's Crown** | 20% |
| 300 experience, shared out | always |
| The advancement **Uncrowned** | always |

- **The King's Ember:** for those who never fight, a fuel that burns as long as a block of coal (80 items), and with any heirloom pumpkin it makes a **King's Lantern**, a jack-o'-lantern whose fire never goes out. No loop: nothing makes an ember but him.
- **The Crowward Charm:** held in the offhand, crows will not come down on crops within 12 blocks of its holder, as a lit scarecrow keeps them off (`CropGuard`).
- **The Pumpkin King's Crown:** a costume worn on the head, his crown of dried vines and corn ears (in `#jugcraft:trick_or_treat_costumes`).
- **The trophies** are new to Arms VII ([features/arms-vii.md](../features/arms-vii.md)), which made none for him: two weapons and the **Fright** boon (a struck monster, never a boss, panics and runs for 1.5 s, once every 6 s per target). They come with part 2, as the owner chose.
- During the Halloween event (and `lairs.event_loot=on`), a roll of seasonal candy and 10% more for the crown, as at Vesperine's and Madame Tatterlace's.
- When he falls, his head drops from his shoulders and rolls to a stop, its fire going out; his straw collapses, the crows settle, and Grey Mist opens at his post, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the farmyard is safe. He wakes when a player steps through the farm gate into the field, or strikes him. Grey Mist fills the lane behind the yard, and opens at his post when he falls.
- **Fixtures:** the eight King's Gourds, which any player in the field may pick when one ripens, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the field owns it. Only his gourds change (green, ripening, lit, picked), and all of them go back when he resets; each instance is placed fresh.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his Jacks are his, capped at four, and gone with him.
- **Death, disconnect and restart:** as every lair. Grave Goods keep what a player falls with, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: a gourd is picked by using it, a ripening gourd glows with crows over it, every attack is read before it lands, and the bereft window opens him to any weapon. Alone, a player can pick both gourds of a harvest in time; fire only makes it easier.
- **Outside the instance:** nothing. The crown is used up, but the scarecrow, its head and every crop round it are left as they were, and no crow of his comes out.

### Rules kept

He follows the rules every boss follows (above):
- his field is his arena, and resets when everyone leaves;
- every attack is read before it lands, and every harvest can be picked in time by one player;
- dying there keeps everything as Grave Goods;
- his health scales with the party, and every participant rolls his loot;
- nothing in his field can be built or broken, and the only blocks that change are his own gourds;
- every opening costs a Harvest Crown, and it works all year unless the server keeps it to Halloween.

Nothing a tier needs comes only from him: heirloom pumpkins, gold and hay come without him, and so does every crop he guards.

## The Storm Roc: the plan (being built)

The seventh of the first eight, built by [the boss playbook](BOSS_PLAYBOOK.md): first his lair and the summoning that opens it, then the Roc himself, his fledglings and his loot, each its own pull request with tests and pictures. On 26 October 2026 the owner chose him as the next boss to build ("Storm Roc boss").

The design joins two sources:
- this page's own idea for him: a thunderbird nesting on the tallest pinnacle of the Highland and the Karst Pinnacles, who dives with lightning and gusts that throw players off the peak, and whose nest's lightning rods can be turned against him;
- the planning pack's "Crown of Thunder" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md#storm-roc--the-crown-of-thunder)):
  - a sheltered summit ring with four rods, ledges and recovery routes;
  - a fall that costs no belongings;
  - a marked rod charged before a dive, which grounds him when his dive is lined up with it;
  - a one-second wing cone, a two-second dive lane, wind strips with ground arrows that stay, and widely spaced lightning marks;
  - flights short enough that melee players get regular openings, and no flight equipment needed.

The numbers here are the plan's. Each part's feature record says what changed in building it. Part 1, the summit and the vane: [features/crown-of-thunder.md](../features/crown-of-thunder.md). Part 2, the Roc, his fledglings and his loot, will have `features/storm-roc.md`.

*An eagle as big as a cart, slate-grey with a storm-blue sheen, his flight feathers tipped white like lightning, a hooked beak of dark horn and eyes that burn electric blue. When he spreads his wings the air crackles.* He nests on the highest crag of the highest peak, and the storms follow him.

### The Crown of Thunder

The summit of a pinnacle above the clouds, under a storm that never breaks:
- **The arrival:** a **terrace** below the summit's south edge, a few steps down from it. Behind it, in an arch of grey stone, the **Grey Mist**: the way home. The terrace is safe.
- **The summit, the arena:** a ring of weathered stone, calcite and turf about 36 blocks across, rimmed by **boulders** with gaps between them. The boulders give shelter from the wind, and the gaps are where a gust can throw a player off.
- **The four Storm Rods:** at the summit's north, east, south and west, 13 blocks from its middle: tall copper rods on stone plinths. A rod charged by the storm crackles and glows blue (part 2).
- **The ledges:** below the summit's east and west edges, five blocks down, two ledges catch a player thrown off. From each, steps climb back up the cliff to the summit: the recovery routes.
- **The nest:** on a crag beyond the summit's north edge, four blocks higher, a great nest of woven branches, where he waits.
- **Below:** only cloud and sky. A fall past the ledges is caught by the mist, which throws you back to the arrival for the lair's toll. It is never a death and never a loss of belongings.

The lair-only blocks are fixtures like the other lairs': unbreakable, no items, no drops. These are the **Storm Rods** and the **nest's woven branches**. Nothing in the Crown can be used, built or broken. Only the storm charges the rods.

### The summoning: the Thunder Vane

1. Craft a **Thunder Vane** from two feathers, a lightning rod, three copper ingots and a compass.
2. Use it **under the open sky, while it rains or thunders**:
   - **in the mountains:** the Highland, the Karst Pinnacles, or a mountain, peak or hill biome (biome tag `jugcraft:thunder_vane_lands`);
   - **high up:** standing at y 120 or above.
3. Lightning strikes beside you and the vane spins wild, and the wind takes you to the Crown of Thunder's terrace. The vane is used up, and Grey Mist rises where you stood: for 60 seconds anyone can walk into the mist and follow.
4. In fair weather, under a roof, below y 120, outside the mountains or outside the Overworld, the vane only says why, and nothing is used up. The same happens if every Crown is taken.

The bolt is only light and thunder: it hurts nobody and lights nothing.

### The fight

The Storm Roc has 400 health, scaled for the party: half as much again for each player after the first, at most two and a half times. He has armour 8. Lightning and falls never harm him, and knockback hardly moves him.

He perches on his nest, wings folded. He wakes when a player who can fight him steps onto the summit, or strikes him: he rises screeching as lightning splits the sky (2.5 s, unhurt meanwhile). He keeps to the summit: more than 30 blocks from its middle, he is set back.

He flies, but never for long: no flight lasts more than 6 seconds before he lands to strike, so melee players always get their openings.

**His rule: the Storm Rods.** Every 16 seconds he dives:
- **A rod charges:** one of the four Storm Rods crackles and glows blue (2 s). The bar over the screen says which ("The north rod is charged").
- **He marks his lane:** he climbs over the summit's edge and fixes on the player he was fighting. A line of sparks runs from him through that player and follows them for 2 seconds, then holds still.
- **He dives** down the lane: 12 damage, and a heavy shove aside, to everyone within 1.5 blocks of it.
- **If his lane passes within 2.5 blocks of the charged rod,** the rod grounds him as he passes it. A bolt arcs from the rod into him, he crashes onto the summit and lies there stunned for 6 seconds, wings spread. Every blow does half as much again, and he cannot fly.
  - So the player he marks puts the charged rod between him and themselves while the lane follows them.
  - Then they step out of the lane as he comes.
- A rod he passed far from discharges harmlessly.

From anywhere on the summit, a sprinting player can reach a place that lines the charged rod up between him and themselves inside the 4 seconds of the tell. The audit will walk the summit to keep that true.

**Phase 1, "The Gathering Storm"** (full to half health):

| Attack | Read it by | What it does |
| --- | --- | --- |
| Wing Buffet | He lands, rears back and spreads his wings wide (1.0 s) | A gust in a cone in front of him, 6 blocks deep: 7 damage, and thrown 3 blocks back. Step behind him or to the side |
| Storm Dive | A rod charges, and a line of sparks marks his lane (2.0 s) | Above |
| Lightning Marks | Three crackling rings on the summit, at least 6 blocks apart (1.5 s) | A bolt strikes each ring: 8 damage within 1.5 blocks. Step out of the ring |

**At half health, "The Squall Rises":** he climbs high and circles screaming (3 s, unhurt). The wind rises, and his fledglings leave the nest.

**Phase 2, "The Squall"** (half to a fifth): he keeps every attack, dives every 14 seconds, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Wind Strips | Arrows appear on the summit floor in two strips 3 blocks wide, pointing the way the wind will blow (1.5 s) | For 4 seconds the wind blows along each strip and pushes everyone in it the way its arrows point, toward the summit's edge. Behind a boulder you are sheltered. The arrows stay until the wind drops |
| Fledglings | He screams to the nest | Up to three Gale Fledglings |

**Below a fifth, "The Eye of the Storm":** he dives every 12 seconds and marks five rings, not three. His tells stay as long as ever.

**Left alone** for 10 seconds, he flies back to his nest, healed. The rods go quiet, the wind drops, his fledglings return to the nest, and who hurt him is forgotten.

**His fledglings, the Gale Fledglings:** half-grown rocs, 20 health, a 3-damage peck, quick fliers that dart at his foe. He has at most three at a time. They come at half health and again every 30 seconds while he has fewer than three. They drop nothing and give no experience.

### His loot (per participant)

A participant is a player who hurt him or his fledglings, or stood within 2.5 blocks of the rod that grounded him, and is within 64 blocks of the summit's middle when he falls.

| Loot | Chance |
| --- | --- |
| **Roc Feathers**, two or three | 1, always |
| **A trophy:** Stormcaller or Galefeather (his `bosses/storm_roc` table, one of the two) | 15%; certain on a player's first kill |
| **The Weather-Vane Lens** | 25% |
| **The Roc's Crest** | 20% |
| 300 experience, shared out | always |
| The advancement **Eye of the Storm** | always |

- **Roc Feathers** are for those who never fight: each makes four feathers, and two make the next Thunder Vane without its lightning rod. Neither recipe gains anything.
- **The Weather-Vane Lens,** held in the offhand, tells its holder the coming weather every few seconds: "Rain in about 6 minutes", "Thunder now". It is a convenience: vanilla's lightning rods and plain watching of the sky still do the same work.
- **The Roc's Crest:** a costume worn on the head, his slate-and-storm-blue crest of feathers.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Gale boon. Until now they were in the creative tab only.
- When he falls, he crashes onto the summit with a last thunderclap, and Grey Mist opens on his nest's crag, the way home.

### The shared lair contract

The planning pack's chapter 04 asks each lair to answer these:
- **Arrival, staging, waking and return:** the terrace is safe. He wakes when a player steps onto the summit or strikes him. The Grey Mist stands in the terrace's arch, and opens on the nest's crag when he falls.
- **Fixtures:** the Storm Rods, which only the storm charges, and the Grey Mist. Nothing else can be used, built or broken.
- **The floor:** the summit owns it, and his fight changes no block of it. His wind arrows are laid over it and lifted when the wind drops, and each instance is placed fresh.
- **Who fights:** players who may fight a lair's boss (`LairBosses`), with party health as above.
- **Reset:** when no eligible player is left, or he is left alone for 10 seconds.
- **Clean-up:** his fledglings, lightning marks and wind arrows are his, capped, and gone with him.
- **Death, disconnect and restart:** as every lair. A player who falls drops nothing and respawns with everything they carried, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything without flying gear:
  - every attack is read before it lands;
  - the charged rod is named and glows;
  - a fall off the summit lands on a ledge with steps back up, or is caught by the mist;
  - the grounding opens him to any weapon.

Of the planning pack's magic links, none is needed:
- Tempest handling the rods' charge more easily;
- Radiance showing the rod to use;
- Strata's Brace easing the wind's shove.

They wait for those schools' spells. The Iron Dreadnought's Grounding Strap halves his lightning, as it halves all electric harm.

### Rules kept

He follows the rules every boss follows (above):
- the summit is his arena, and resets when everyone leaves;
- every attack is read before it lands, and every dive can be turned against him;
- dying there keeps everything: nothing is dropped, and the player respawns with it all;
- his health scales with the party, and every participant rolls his loot;
- nothing on the summit can be built or broken, and his fight changes no block of it;
- every opening costs a Thunder Vane, made from things players already make.

Nothing a tier needs comes only from him: feathers, copper and weather all come without him.

## More bosses to consider (no trophies yet)

- **The Pumpkin King** (Halloween nights): a giant scarecrow made of the harvest. It would be summoned with heirloom pumpkins in a bounded field, and it ties into the giant pumpkins, the scarecrows and the Headless Horseman already in the world. It must not be the only source of anything core, and what it drops must be kept after the season ends. *Now built, to [his plan above](#the-pumpkin-king-the-plan-being-built).*
- **The Headless Horseman, as a boss fight:** it already exists as a Halloween-night encounter ([features/halloween-nights.md](../features/halloween-nights.md)). It could gain a lair and a trophy, such as a lantern-headed flail.
- **The Marrow Colossus** (Nether, Marrow Heap): a giant made of bones that sheds parts of itself as it is hurt. Bone-themed trophies would fit the bonecarved style.
- **The Chorus Sovereign** (End, Chorus Reef and Phantom Garden): a crowned shulker-like being that teleports with its arena. It suits levitation or void boons.
- **The Hollow Monarch** (the Pixel Hollows): a retro-game boss with glitching sprites and pixel projectiles. A good fit for the arcade's style.
- **The Kaiser's Zeppelin** (the dieselpunk tiers): an airship fought from a sky platform or the player's own zeppelin. Its weak points are its gondolas.
- **The Frost Wyrm** (Ice Sheet): if the Yeti King is built first, a dragon of the far north for the late game.
- **The Hive Matriarch** (deep caves): an insectoid queen in a hive dungeon, for the underground milestone in [ROADMAP.md](../ROADMAP.md).

## Next steps (the owner's choice)

1. Pick the first boss to build. The Yeti King is the owner's own example, and its biomes are built: he is being built first (above).
2. Build it by [the boss playbook](BOSS_PLAYBOOK.md): its plan here, then its lair and summoning, then the boss and its loot, each with a feature record (template: [features/TEMPLATE.md](../features/TEMPLATE.md)) giving the arena, attacks, scaling, reset and loot rules above.
3. Have its death roll `bosses/<boss>.json` for each player who fought, as the trophies expect.
