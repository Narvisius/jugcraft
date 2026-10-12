# The Yeti King

Status: implemented in source. CI builds it, and its game tests and client game test pass (below). This is part 2 of boss 3 in the [bosses plan](../branches/BOSSES.md#the-yeti-king-the-plan-being-built): the boss of the Glacier Hall, his kin and his thrown and fallen things, and his loot. It is built on part 1, the hall and the Frost Horn that opens it ([glacier-hall.md](glacier-hall.md)), and on the lair framework ([hollow-acre.md](hollow-acre.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 10 October 2026 the owner asked for the next boss "exact same way you did the spider but adjusted for that character", and chose a new boss built from start to finish; the Yeti King is the owner's own example in [branches/BOSSES.md](../branches/BOSSES.md).
Owner: @jimbozoomer-byte

Target milestone and tier: Specialization tier (dungeon expeditions), as the lairs are. He is reached only through the Frost Horn, which takes Discovery-tier things. Nothing a tier needs comes only from him.
Primary specialty and supported player role: adventuring. His loot also serves the summoning's makers (Yeti Fur in the next horn), cold-biome travellers (the Yeti Mitten) and costume wearers (trick-or-treating and the costume contest).

## Player experience

### He waits

The Yeti King sits slumped on the throne of every Glacier Hall, placed with the hall, asleep. He is a great white ape of the glacier, twice a player's height and more (3.4 blocks tall, 2 wide):
- shaggy white fur in long locks, shadowed blue-grey and frosted blue at their tips, with a shaggy mantle over his shoulders;
- a bare blue-grey face, palms, knuckles and soles; eyes of glacier blue under a heavy white brow; ivory fangs;
- long arms that reach his knuckles to the snow, the fur hanging from his forearms hung with icicles;
- a crown of blue ice, its tallest point at the front.

Waiting, he takes no harm. He wakes when a player who can fight him (anyone not in creative or spectator mode) steps onto the lake, or strikes him. Then:
- "The Yeti King rises from his throne with a roar that shakes the ice", and his bar shows;
- he roars on his throne for a second and leaps down onto the lake below his dais in another, and nothing can hurt him meanwhile;
- his health is set for the party: 420, half as much again for each player after the first within 38 blocks of the lake's centre, at most two and a half times (1050 for four or more).

He has armour 10 and cannot be knocked back. He takes no fall damage, never freezes and no web or powder snow holds him; he walks his own snow and ice without sliding. He cannot ride, be leashed or be pushed. On the lake he lopes at a quarter-block a tick on his legs and knuckles and stops two and a half blocks short of his foe. If he is ever more than 30 blocks from the lake's centre, or fallen below it, he bounds back to its middle.

### His rule: the snow is his

His **Ground Slam** blasts the drift snow from the lake round where he lands, baring **glare ice**, as slick as blue ice, for **12 seconds** before the snow drifts back over it. On it you slide, and every blow carries you further. He bares only drift snow: never the trampled snow round an ice column, at the ramp's foot, before his dais or on the dens' paths, nor the glare ice the wind has already scoured, so there is always firm footing somewhere.

### Phase 1, the Hunt (full to half health)

Every attack has a wind-up you can read, a strike and a recovery. He waits at least a second between attacks.

| Attack | Read it by | What it does |
| --- | --- | --- |
| Maul Swipe | He rears back on his right, that fist raised out wide behind him (0.6 s) | His fist rakes across his front, 120°, 4 blocks past his body: 12 damage and a knockback. Up close, his most frequent attack |
| Boulder Throw | He digs both fists into the snow and heaves a block of ice up over his head (0.8 s) | He hurls it in an arc at where his foe stood; it shatters where it lands, or on the first player it strikes: 10 damage within 2.5 blocks |
| Ground Slam | He crouches low, fists high, while a ring of frost spreads round his foe (0.8 s) | He leaps onto where his foe stood (0.8 s in the air): 14 damage within 3 blocks, a knockback within 6, and the drift snow blasted bare within 7 (his rule) |
| Frost Breath | He swells his chest, head thrown back, frost at his jaws (1 s) | A cone of freezing breath before him, 60° and 6 blocks, for 1.5 s: 2 damage every quarter-second and the frost. It stacks past freezing you through, so standing in it lets the cold bite |
| Avalanche Charge | He drops to all fours and paws the snow (0.8 s) | He charges in a straight line at 0.7 blocks a tick for up to 1.2 s (about 16 blocks): 12 damage and a heavy knockback to each player he passes, once each. **Into an ice column he crashes, reels for 3 s and takes a third more damage meanwhile** ("The Yeti King crashes into the ice and reels"). The charge also ends at his leash |

### The King's Roar (at half health)

"The King's Roar: a blizzard howls through the hall, and his kin answer". He bounds back onto the top step of his dais and roars for 3 seconds, unhurt:
- a blizzard chills everyone near: the frost, to just short of freezing them through;
- two **Yeti Whelps** climb out of the dens in the side walls.

Then he leaps back down onto the lake below the dais, and the fight changes.

### Phase 2, the Blizzard (half health to a fifth)

He keeps his Maul Swipe, Boulder Throw, Ground Slam and Avalanche Charge, stops breathing frost, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Icicle Fall | He roars up at the vault, fists raised, and dark shadows spread on the snow round his foe (1 s) | Eight icicles fall one by one over 0.8 s, onto marks within 3 blocks of his foe, from 12 blocks up: 6 damage to a player within a block of where one shatters |
| Glacial Spikes | He raises a fist high while cracks race across the ice towards his foe (0.8 s) | He drives it into the ice: a spike bursts up each tick along the line, 12 blocks: 10 damage and thrown up. Each stands a second, then crumbles |
| Kin Call | He beats his chest and calls (1 s) | Two Yeti Whelps climb out of the dens; at most four at a time |

**Yeti Whelps:** round white cubs with big glacier eyes and a tuft of blue frost, 16 health, their blows 3 damage, quick. They go for the nearest player, never freeze, drop nothing, give no experience and are never saved. They flee into the snow when he falls, resets or is gone. Whoever hurts one has taken part in his fight.

### The Fury of the Peaks (below a fifth)

"The Fury of the Peaks: his crown blazes, and his eyes burn blue". Blue fire wraps the points of his crown and rises from his eyes. Every cooldown, and his pause between attacks, is a third shorter; his Ground Slam bares a wider ring, 10 blocks; and a charge that does not stun him is followed at once by another.

### His fall

When he falls:
- his whelps flee into the snow, and his blocks of ice, icicles and spikes are gone;
- the snow drifts back over everything he bared, at once;
- each participant gets their loot (below) and Abominable;
- "The Yeti King falls, and the glacier is still", and a gate of **Grey Mist** two blocks wide and two high opens in the middle of the lake. It takes players home, as the arch beside the ledge does;
- the instance is ended, and it closes once everyone has left. The mist stays until then; the next instance in that slot clears it as he takes his throne again.

### Left alone

If no player he can fight is within 38 blocks of the lake's centre for 10 seconds: "The Yeti King climbs back onto his throne". The snow drifts back, he heals fully, his crown is calm again, his whelps flee and his things are gone. Who hurt him is forgotten, so a fresh fight starts from nothing.

### His loot

Each participant rolls their own loot. A participant is a player who hurt him or his whelps and is within 64 blocks of the lake's centre when he falls. The loot goes straight into their inventory, and what does not fit lands at their feet. "His frozen hoard is yours".

| Loot | Chance |
| --- | --- |
| **Yeti Fur**, a thick tuft of his white fur | 4 to 8, always |
| **A trophy:** the Glacier Maul or the Rimeclaw, Arms VII's (one of the two) | 15%; one is certain on a player's first kill (whoever has not yet earned Abominable) |
| **Yeti Mitten** | 25% |
| **The Yeti King's Crown** | 20% |
| 300 experience, shared out among the participants | always |
| The advancement **Abominable** (a challenge in the Adventure tab) | always |

He is no Witching Season boss: the Halloween event adds nothing to his loot.

**What they are for:**
- **Yeti Fur:**
  - **A cheaper Frost Horn:** a goat horn, a gold ingot, two snow blocks and Yeti Fur, shapeless. The fur takes the two leather's place and one gold ingot's. This is the plan's "part of what summons him again".
  - **Shorn:** one fur makes two white wool.
- **Yeti Mitten:** held in the offhand, its wearer never freezes. Powder snow, his Frost Breath and his blizzard leave them warm: whatever frost they gather is gone each tick, and his frost never lands on them.
- **The Yeti King's Crown:** a costume worn on the head, his crown of blue ice in small: a band of ice and six points rising from it, the tallest at the front. It counts for trick-or-treating and the costume contest.
- **The Glacier Maul and the Rimeclaw:** Arms VII's two Yeti King trophies, until now in the creative tab only. The maul fights as the steel maul does and the Rimeclaw as the steel katar; both are epic, last twice as long as steel, read "Trophy of the Yeti King", and carry his boon, **Frost:** a hit chills the foe, Slowness II for 3 seconds.

### Settings

His settings are the ones every lair boss shares, in `config/jugcraft.properties` (`lair/LairBosses.java`):
- `lairs.boss_health` (1.0, from 0.25 to 4): multiplies his health;
- `lairs.boss_damage` (1.0, from 0.25 to 4): multiplies his blows, blocks of ice, slam, breath, charge, icicles and spikes (not his whelps' bites).

`lairs.event_loot` does not touch him: he has no Halloween extra. His damage numbers are Normal difficulty's: Easy and Hard scale them as they scale every monster's blows.

## Changes from the plan

- **Maul Swipe is one fist, not two.** The plan raised both fists; he rears back on his right and rakes it across his front. Its reach (120°, 4 blocks) and damage are the plan's.
- **The Fury's slam bares a ring of 10 blocks,** not twice the Hunt's 7: 14 would have bared most of the lake at once and left no snow to stand on.
- **Frost Breath stacks the frost past freezing through** (up to 10 seconds of it), so the cold's own damage can bite; the blizzard stops just short, as the plan's "the frost, for a moment".
- **Where he lands when he wakes,** and after his roar: on the lake four blocks out from the foot of his dais. **Where he roars from:** the front of his dais's top step, before the throne. The plan did not say.
- **His block of ice is drawn from nowhere:** he heaves it up as if torn from the lake, but the lake is not changed. It leaves his hands 4.5 blocks up, where his model holds it over his head.
- **His icicles and spikes are entities, not blocks.** The hall's giant icicles stay on the vault; the falling ones are their own, dropped from 12 blocks over their marks. The spikes stand a second and crumble, and no block of the lake is touched.
- **The charge ends at his leash** as well as at a column. A charge that does not stun him, in the Fury of the Peaks, is followed by exactly one more.
- **His whelps are `jugcraft:yeti_whelp`,** and **his blocks of ice, icicles and spikes** `jugcraft:hurled_boulder`, `jugcraft:falling_icicle` and `jugcraft:glacial_spike`.
- **His voice is the polar bear's,** pitched down, and his roars the warden's.
- **He is a monster:** on Peaceful he is gone, as every monster is, and the Glacier Hall stands empty.
- **Animations by GeckoLib,** as the other lair bosses' are: GeckoLib 5.5.7 is already pinned.

## Connections

- **Inputs:** the way in is part 1's Frost Horn, blown on snow at night, so goats' horns, gold, leather and snow feed every fight.
- **Outputs:**
  - **Yeti Fur** feeds back into the summoning (the cheaper Frost Horn) and into wool;
  - **the Yeti Mitten** is an offhand charm for powder snow and the cold biomes;
  - **the Yeti King's Crown** is a costume for trick-or-treating and the costume contest (`#jugcraft:trick_or_treat_costumes` and `#jugcraft:costume_hats`);
  - **the Glacier Maul and the Rimeclaw** are Arms VII's boss trophies: the first of the eight bosses still to be made in Arms VII's list to drop his.
- **Technology and magic:** none required. He is fought with whatever players bring, and nothing in a tier needs his loot.
- **Trade and solo routes:** a solo player can do everything. His health is lowest for one player, and the first kill always brings a trophy. His loot can be traded like any item.
- **Reachable entry path:** the horn takes Discovery-tier things (part 1), and nothing he drops is needed to reach him.

## Balance and automation

- **Every fight costs a summoning:** a fresh Frost Horn, as part 1 set it.
- **No gain loop:** fur makes a horn that still needs a goat horn, gold, snow and a whole fight, and fur shears into two wool, while wool makes no fur. Nothing he drops turns into more of itself. Nothing he drops sells to a Jugcraft shop for Jugs.
- **No leeching:** only participants get loot. A bystander who never struck him or a whelp gets nothing, and loot is rolled per participant, so nobody takes another's.
- **First kill once:** the trophy's first-kill guarantee is stored with the advancement, so it comes once per player.
- **Abandoning resets him:** leaving him alone for 10 seconds heals him and forgets everyone's part. He cannot be worn down between visits.
- **No automation:** he wakes only for a player, bares his lake only in a fight, and his loot goes only to players. His whelps drop nothing and give no experience.
- **Units:** health and damage in half hearts, before `lairs.boss_health` and `lairs.boss_damage`. Times are in ticks, 20 a second; distances in blocks. Every number is in `tools/yeti_king.py`, and `tools/check_mod_data.py` holds the Java to it.

## Multiplayer and persistence

- **Server authority:** the server decides everything: waking, targets, every attack's hits, the bared snow and its drifting back, the phases, his kin, the frost and the mitten's warmth, the loot and the advancement. Clients only draw what his synced state says (his action and whether he is furious) and play the matching animations.
- **What is saved:** he is saved with his instance's chunks. His phase, his fury and the cells of his lake he has bared are saved, and a passing moment (waking, his roar) resumes as the phase it leads to. When he is loaded, the snow drifts back over what he had bared. His whelps, blocks of ice, icicles and spikes are never saved. Instances close on a restart (part 1), and he goes with his.
- **The snow:** a bared patch is set back to drift snow cell by cell, and only where it is still glare ice.
- **Chunks:** nothing is force-loaded. His arena stays loaded while players stand in the hall.
- **The mitten** is looked at once a tick for each player; nothing about it is stored.
- **No griefing:** he cannot leave his lair, his attacks strike only players who can fight him, and the only blocks he changes are his own lake's snow, which drifts back.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. GeckoLib 5.5.7 for 26.3 is already pinned (`distribution/frameworks.lock.json`); it draws him, his whelps, his blocks of ice, his icicles and his spikes. Everything is drawn by code:
- **Bodies and clips:** `tools/yeti_king_models.py` builds the GeckoLib models and animations on Vesperine's builder: 24 of his (throne, wake, leap, idle, walk, every attack's wind-up and strike, stunned, roar, call, death, and his crown calm and blazing, on a controller of their own), the whelp's walk and idle, the block's tumble, the icicle's fall and the spike's burst.
- **Textures:** `tools/yeti_king_art.py` paints them at four times Minecraft's scale: 1024 × 1024 for him (his parts need a 256 sheet), 256 × 256 for the whelp, the block of ice, the icicle and the spike. His glowmask lights his eyes and the Fury's blue fire. It also paints the worn crown's ice and band.
- **Icons:** Yeti Fur, the Yeti Mitten and the crown from icon maps (`tools/item_icons/`), with a new icon material for his fur (`yeti_fur`, `tools/icon_materials.py`); the trophies' art is Arms VII's.
- **Data:** `tools/yeti_king_data.py` writes the names, tooltips and messages, the item models (the crown worn as its own model), the loot table, the two recipes, the costume tags and the advancement.

No Mojang texture is read, traced or copied.

## Verification

![The Yeti King slumped asleep on his throne under the ivory tusks, the dais's tiers of blue ice below him](../images/ingame_yeti_king_throne.jpg)
![Woken, he stands up on his throne, his arms out](../images/ingame_yeti_king_wakes.jpg)
![Maul Swipe: on the glare ice in the lake's middle he swings an arm through a white arc](../images/ingame_yeti_king_maul_swipe.jpg)
![Boulder Throw: he heaves a block of ice up over his head](../images/ingame_yeti_king_boulder_throw.jpg)
![Ground Slam: he leaps high beside an ice column](../images/ingame_yeti_king_ground_slam.jpg)
![From above: a wide round of the lake's drift snow blasted bare to glare ice where he came down, he in its middle, the columns and his dais beyond](../images/ingame_yeti_king_slam_ice.jpg)
![Frost Breath: hunched on the glare ice, he breathes a cloud of frost across it](../images/ingame_yeti_king_frost_breath.jpg)
![Avalanche Charge: he charges on all fours across the glare ice, snow flying about him](../images/ingame_yeti_king_charge.jpg)
![The King's Roar: back on his dais before the throne, his arms raised](../images/ingame_yeti_king_roar.jpg)
![His kin: a whelp just out, in a den in the hall's side, among the den's old bones and the white pelts on its floor](../images/ingame_yeti_king_kin.jpg)
![Icicle Fall: icicles falling one above another before the hall's wall, their shadows on the glare ice, and he roaring beside them](../images/ingame_yeti_king_icicle_fall.jpg)
![Glacial Spikes: shards of ice bursting up from the glare ice before him, a whelp beyond](../images/ingame_yeti_king_glacial_spikes.jpg)
![The Fury of the Peaks: he stands on the glare ice facing the camera, blue fire rising from his crown, the throne beyond](../images/ingame_yeti_king_fury.jpg)
![After his fall: Grey Mist standing over the glare ice in the lake's middle, the drift snow round it, the empty throne beyond](../images/ingame_yeti_king_defeated.jpg)

*The client game test's pictures (CI, commit `bd610bd`; the Glacial Spikes from `f2cd4aa`, whose run caught the spikes as they burst): waiting and waking; the Hunt's Maul Swipe, Boulder Throw and Ground Slam, the glare ice his slam bared, his Frost Breath and his Avalanche Charge; the King's Roar and his kin in a den; the Blizzard's Icicle Fall and Glacial Spikes; the Fury of the Peaks; and after his fall. Each attack is frozen mid-move. The test client renders at 480x270.*

CI (10 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `d6a30b6` | Build | **Failed to compile:** his knockback set `hurtMarked`, a field 26.3 does not have. Nothing else ran |
| `f2cd4aa` | Build, data audit, every server game test with and without the optional integrations, and every client test class (his test arena is a new structure file, which the selection counts as shared) | His knockback now sends the player's motion packet, as the spider's spool does. **All pass but one:** his nine game tests in both server jobs, and `YetiKingClientGameTests` (his whole fight) with every other client class. `mod` failed 1 of its 1292 required tests: Arms VIII's javelin test, which on `main` too now and then flies its javelin through its pig (pull request 303); `optional integrations absent` passed it on the same commit. The Fury's picture was taken too close, and the falling icicles were lost among the vault's own |
| `bd610bd` | The same, with pull request 303's fix for the javelin ported (`294651b`), and those two pictures re-aimed | **All pass:** all 1293 required game tests (the fix's new test among them), and every client test class. The pictures above are from this commit, but for the Glacial Spikes |

Run locally (10 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks (`check_yeti`) his Java numbers, attacks and his things' numbers against `tools/yeti_king.py`; that where his fight looks for the hall's parts (`GlacierHall.java`: the lake, its centre and radius, the columns, the top step, the throne, the roar's place and the dens) is where `tools/glacier_hall.py` builds them, and that his throne, the place he roars from, where he lands and the dens' mouths are clear in the template and stand on its floor; that the block he hurls leaves from where his model holds it; his registrations, names and renderers; the GeckoLib models, every clip the Java plays, sheet sizes, glowmask and controllers' bones; the loot table and his trophies against Arms VII's; recipes, costume tags, advancement and messages; and that nothing of his loads chunks or changes dimension. Ten deliberate changes to his Java, one at a time, each failed it | Pass, 2159 IDs |
| `python3 tools/check_icon_maps.py` on the three new maps | Pass; the new `yeti_fur` material passes the palette rules |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded with identical pixels were reverted) |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

The first audit run found the west den's mouth holding a pelt, a carpet; the check now lets a carpet stand where his kin come out.

The game tests (`YetiKingGameTests`) show what a game-test server can, with a king put down on his own (no lair) over a hall laid out from the corner of a hall-sized arena:
1. waiting on his throne, he takes no harm; a player's blow wakes him at full health, and he leaps down onto his lake below the dais, to hunt;
2. his health scales 1, 1.5 and 2.5 times for one, two and four or more players; in his fury his cooldowns are a third shorter and his slam's ring wider;
3. Maul Swipe strikes a player in front of him and spares one behind him;
4. his slam bares the drift snow within its ring to glare ice, never the trampled snow nor the snow beyond, and 12 seconds later the snow has drifted back;
5. an Avalanche Charge at a foe beyond an ice column stops short of it with him stunned, and stunned he takes a third more damage;
6. his kin come out of the dens, never more than four at once, and when he is left alone they flee and he is back on his throne, healed;
7. his blizzard chills a player to just short of freezing them through, his breath's frost stacks past it, and a Yeti Mitten keeps its wearer warm;
8. a glacial spike strikes a player and throws them up, and an icicle shatters on its mark, striking the player there;
9. his loot is each participant's own: two who struck him each get Yeti Fur and, on a first kill, one trophy, and both earn Abominable; a bystander who never struck him gets nothing.

The client game test (`YetiKingClientGameTests`, CI job `client`) runs his fight in a real Glacier Hall, with its one player in survival under Resistance:
1. an instance opens with him waiting on his throne;
2. stepping onto the lake wakes him, and he comes down onto it to hunt;
3. the Hunt's five attacks, his slam leaving bare glare ice where he came down;
4. at half health the King's Roar from his dais, with two whelps out of the dens, then down again for the Blizzard;
5. the Blizzard's icicles and spikes;
6. below a fifth, the Fury of the Peaks;
7. when he falls (with a patch of his lake bare and his kin about), the player has Yeti Fur, one trophy (a first kill) and Abominable, the snow has drifted back, his kin are gone, and Grey Mist stands in the lake's middle;
8. a fresh instance opened in his slot has none of the old instance's Grey Mist.

Along the way it takes a picture of each part of the fight (above).

Not run: the two-client dedicated-server playtest, and play by hand. No test yet times his attacks against real players, measures how long a fight lasts, or tries sliding on his glare ice, his charge or his blocks of ice on a moving player.

## World and event applicability

He is confined to the Glacier Hall, his own dimension; nothing of his reaches the Overworld, and the only blocks he changes are his lake's snow, which drifts back. His loot rarity is set above, and his one boon is his trophies' Frost. The horn, and so the fight, works on any night of the year. No event adds to him or takes him away.

## Rollout and open questions

Known limits:
- untested by hand and with two clients, so his numbers are the plan's and may need tuning once he is played;
- on Peaceful there is no fight;
- Yeti Fur has two uses so far.

With him, the first of the bosses Arms VII's trophies wait for is built.
