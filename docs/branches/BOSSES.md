# Bosses branch (brainstorm)

Status: **a brainstorm; two bosses are being built.** On 10 October 2026 the owner chose to build a new boss "the same way" as Madame Tatterlace ([features/tatterlace.md](../features/tatterlace.md)); the Yeti King is first, to the plan below. The same day the owner asked for "the next boss same way" as him: the Cinder Tyrant, to his plan below. On 4 October 2026 the owner asked for variant weapons, many of them "drops from bosses ill make later", themed around those bosses ("example: Yeti King"), and to "brainstorm bosses we could make". This page is that brainstorm. The bosses' weapons already exist: Arms VII ([features/arms-vii.md](../features/arms-vii.md)) adds two trophies for each of the first eight bosses below, and their loot tables are ready (`data/jugcraft/loot_table/bosses/<boss>.json`, one of the two per roll). Building a boss is a separate, reviewed feature each time, in the order the Yeti King was built in: [BOSS_PLAYBOOK.md](BOSS_PLAYBOOK.md). Everything here is a proposal for the owner to pick from, change or drop.

## Rules every boss follows

From [CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md) ("Caves, dungeons and bosses" and "Rare loot"):
- **An arena with edges:** a lair, or a summoning that builds one. The boss does not wander out of it, and it resets when everyone leaves.
- **Readable attacks:** every big attack is telegraphed (a wind-up, a sound, a shadow on the ground) and can be dodged or blocked.
- **Recovery:** dying in the fight is not the end; gear can be fetched back, and the boss heals and resets rather than keeping its gains.
- **Multiplayer:** its health scales with the players in the arena; every player who fought gets a roll of its loot table, not just the last hit.
- **No griefing:** a summoned boss cannot be called inside a town's protection or another player's claim, and its attacks do not break player builds.
- **No farming loop:** a long respawn timer per lair (or a costly summoning item), so a trophy stays a trophy.
- **Optional:** no core progression needs a boss. Its trophies are side-grades with a boon, and each boss also drops something useful to non-fighters (a crafting material, a decoration, a trophy head).

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
