# Bosses branch (brainstorm)

Status: **a brainstorm; six bosses are built.** On 10 October 2026 the owner chose to build a new boss "the same way" as Madame Tatterlace ([features/tatterlace.md](../features/tatterlace.md)); the Yeti King is first, to the plan below. The same day the owner asked for "the next boss same way" as him: the Cinder Tyrant, to his plan below. Both are built. On 13 October 2026 the owner chose the next: the Mire Hag, to her plan below, who is built too; and after her, the same day, the Crypt Lich, to his plan below, who is built too. On 4 October 2026 the owner asked for variant weapons, many of them "drops from bosses ill make later", themed around those bosses ("example: Yeti King"), and to "brainstorm bosses we could make". This page is that brainstorm. The bosses' weapons already exist: Arms VII ([features/arms-vii.md](../features/arms-vii.md)) adds two trophies for each of the first eight bosses below, and their loot tables are ready (`data/jugcraft/loot_table/bosses/<boss>.json`, one of the two per roll). Building a boss is a separate, reviewed feature each time, in the order the Yeti King was built in: [BOSS_PLAYBOOK.md](BOSS_PLAYBOOK.md). Everything here is a proposal for the owner to pick from, change or drop.

## Rules every boss follows

From [CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md) ("Caves, dungeons and bosses" and "Rare loot"):
- **An arena with edges:** a lair, or a summoning that builds one. The boss does not wander out of it, and it resets when everyone leaves.
- **Readable attacks:** every big attack is telegraphed (a wind-up, a sound, a shadow on the ground) and can be dodged or blocked.
- **Recovery:** dying in the fight is not the end; gear can be fetched back, and the boss heals and resets rather than keeping its gains.
- **Multiplayer:** its health scales with the players in the arena; every player who took part gets a roll of its loot table, not just the last hit. A blow to it, or to a creature that counts for it, is taking part; at Vesperine and Madame Tatterlace, since Concordance Chunk A, so is support: healing or warding another player against the fight, or one of its objectives ([lair encounter credit](../features/lair-encounter-credit.md)).
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

The first two are the Witching Season's ([features/witching-season.md](../features/witching-season.md)); all four are on the shared lair framework (`lair/`). Vesperine and Madame Tatterlace roll their own loot table, `loot_table/entities/<boss>.json`, for each player who took part: their trophies, the Vesper Scythe and the Needle Rapier (Arms VII's), come at 15% a share and always on a player's first win. The Arms VII trophy tables `bosses/vesperine` and `bosses/tatterlace` are written by `tools/arms_variants.py` as the others' are, but nothing rolls them. The Yeti King and the Cinder Tyrant roll theirs the same way, a trophy always on a player's first win; they still credit damage alone, without Concordance Chunk A's support credit ([lair-encounter-credit.md](../features/lair-encounter-credit.md)).

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

**Yeti Whelps:** young yetis, 16 health, their blows 3 damage, quick. They go for the nearest player, drop nothing, and flee into the snow when he falls, resets or is gone. Whoever hurts one has taken part in his fight.

**Below a fifth, "The Fury of the Peaks":** his crown blazes and his eyes burn blue. Every cooldown (and his pause between attacks) is a third shorter, his slam bares twice the ring, and a charge that does not stun him is followed at once by another.

**Left alone** for 10 seconds, he climbs back to his throne, healed; the snow drifts back, his whelps flee, and who hurt him is forgotten.

### His loot (per participant)

A participant is a player who hurt him or his whelps and is within 64 blocks of the lake's centre when he falls.

| Loot | Chance |
| --- | --- |
| **Yeti Fur**, a pelt of thick white fur | 4 to 8, always |
| **A trophy:** the Glacier Maul or the Rimeclaw (his `bosses/yeti_king` table, one of the two) | 15%; certain on a player's first kill |
| **Yeti Mitten** | 25% |
| **The Yeti King's Crown** | 20% |
| 300 experience, shared out | always |
| The advancement **Abominable** | always |

- **Yeti Fur:** a cheaper Frost Horn (fur in place of the two leather and a gold ingot), his "part of what summons him again"; or two white wool.
- **Yeti Mitten:** held in the offhand, you never freeze: powder snow, his Frost Breath and the blizzard leave you warm.
- **The Yeti King's Crown:** a costume worn on the head, his crown of blue ice in small.
- **The trophies** are Arms VII's ([features/arms-vii.md](../features/arms-vii.md)), with their Frost boon; until now they were in the creative tab only.
- When he falls, Grey Mist opens in the middle of the lake, the way home.

### Rules kept

He follows the rules every boss follows (above): his hall is his arena and resets when everyone leaves; every big attack is read before it lands; dying there keeps everything as Grave Goods; his health scales with the party and every participant rolls his loot; nothing in his hall can be built or broken, and the only blocks he changes are his own lake's snow, which drifts back; every opening costs a Frost Horn. Nothing a tier needs comes only from him.

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

**Cinderlings:** small salamanders of glowing slag. They have 14 health and bite for 3 damage, setting you burning for 2 s. They are quick and immune to fire, but in a flooded trough they gutter out. They go for the nearest player, drop nothing, and crumble to ash when he falls, resets or is gone. Whoever hurts one has taken part in his fight.

**Below a fifth, "The Molten Heart":** his cracks blaze white. Every cooldown, and his pause between attacks, is a third shorter. His Ember Spit throws five, and his Kiln Breath sweeps 90°. Quenching him still works as before.

**Left alone** for 10 seconds, he sinks back into the crucible, healed. The troughs drain, the sluices reset, his Cinderlings crumble, and who hurt him is forgotten.

**Fire Resistance**, from a potion or the Ember invocation Hearthguard, stops his fire: the breath, the slag, the wave, the fire patches and burning. It never stops his blows: the Tail Sweep, the Body Slam, the gobs' and cinders' impact, and the Cinderlings' bites. It helps the prepared without emptying the fight. Hearthguard is the planning pack's Ember link; its Tide, Rime and Strata links wait for those schools.

### His loot (per participant)

A participant is a player who hurt him or his Cinderlings and is within 64 blocks of the bowl's centre when he falls.

| Loot | Chance |
| --- | --- |
| **Tyrant Scale**, a scale of obsidian veined with cooling magma | 3 to 6, always |
| **A trophy:** the Cinderbrand or the Magmaw (his `bosses/cinder_tyrant` table, one of the two) | 15%; certain on a player's first kill |
| **Salamander Charm** | 25% |
| **The Tyrant's Crest** | 20% |
| 300 experience, shared out | always |
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
- **Death, disconnect and restart:** as every lair. Grave Goods keep what a player falls with, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: the sluices are turned by hand, the bait is standing in a trough, and a hot Tyrant still takes half damage from any weapon.

### Rules kept

He follows the rules every boss follows (above):
- his kiln is his arena, and resets when everyone leaves;
- every big attack is read before it lands;
- dying there keeps everything as Grave Goods;
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
- **Death, disconnect and restart:** as every lair. Grave Goods keep what a player falls with, and instances close on a restart. Each participant's loot is rolled once when she falls.
- **An ordinary player** can do everything: a lamp is lit by hand, a Likeness bursts at any blow or anything thrown, her shadow shows with particles off, and her roots, pools and grasps are read before they land. Her poison never kills, and milk or the chemical reactor's Antidote cures it.

Of the planning pack's magic links, Radiance's is already in the game: its Revelation makes a creature glow, so cast on her it shows her as a lamp does. The others (Verdance clearing a path through her roots, Tether cutting the knot, Tide thinning a pool) wait for spells of those schools. None of them will be needed.

### Rules kept

She follows the rules every boss follows (above):
- her fen is her arena, and resets when everyone leaves;
- every big attack is read before it lands;
- dying there keeps everything as Grave Goods;
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
- **Death, disconnect and restart:** as every lair. Grave Goods keep what a player falls with, and instances close on a restart. Each participant's loot is rolled once when he falls.
- **An ordinary player** can do everything: a seal is broken and a name read by using them, the lit name is shown by its rune, its glow and the words over the screen, his bolts are struck back with any weapon or arrow, and every attack is read before it lands. Alone, a player breaks two seals, reads the scraped name back, and breaks the third.

Of the planning pack's magic links, Radiance's is already in the game: its Revelation makes him glow, though it does not break his ward. The others (Echo reading a scraped name back from afar, Hollow turning one of his bolts, the Circlewrights holding an alcove's chain) wait for spells of those schools. None of them will be needed.

### Rules kept

He follows the rules every boss follows (above):
- his archive is his arena, and resets when everyone leaves;
- every big attack is read before it lands, and he is never warded for good: every round of seals opens him;
- dying there keeps everything as Grave Goods;
- his health scales with the party, and every participant rolls his loot;
- nothing in his archive can be built or broken, and the only blocks that change are his own seals, stones and phylactery, which are put back;
- every opening costs a Writ of Exhumation.

Nothing a tier needs comes only from him.

## More bosses to consider (no trophies yet)

- **The Pumpkin King** (Halloween nights): a giant scarecrow made of the harvest. It would be summoned with heirloom pumpkins in a bounded field, and it ties into the giant pumpkins, the scarecrows and the Headless Horseman already in the world. It must not be the only source of anything core, and what it drops must be kept after the season ends.
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
