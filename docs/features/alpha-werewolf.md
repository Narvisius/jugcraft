# The Alpha Werewolf

Status: implemented in source. The original branch passed server tests, but its client test failed at the first hunt; the October 15 integration repairs that timing assumption and has passed local combined server suites and the rendered client fight; combined GitHub CI is pending. Part 2 of boss 8 in the [bosses plan](../branches/BOSSES.md#the-alpha-werewolf-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the boss of the Moonlit Ring, his hide and the Hunt that the Warden Bells break, his Packmates, the raked turf of his trail, and his loot. It is built on part 1, the ring and the Howling Totem that opens it ([moonlit-ring.md](moonlit-ring.md)), and on the lair framework ([hollow-acre.md](hollow-acre.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 14 October 2026 the owner chose the Alpha Werewolf as the next boss ("Ok lets do the alpha werewolf boss 8"), built the same two-part way: the Moonlit Ring and its summoning, then the Alpha himself, his pack and his loot. He is the sixth of the first eight in [branches/BOSSES.md](../branches/BOSSES.md).
Owner: @jimbozoomer-byte

Target milestone and tier: Discovery tier, as the werewolves of the woods and the ring are. He is reached only through the Howling Totem (a werewolf's pelt, three bones and two silver ingots; part 1). Nothing a tier needs comes only from him.
Primary specialty and supported player role: adventuring and hunting. His loot also serves leatherworkers (the Alpha Pelt's leather), the summoning's makers (the pelt binds another totem), werewolf hunters on full-moon nights (the Packfinder Charm) and costume wearers (trick-or-treating and the costume contest).

## Player experience

### He waits

The Alpha Werewolf crouches on the top of the Howling Stone at the middle of every Moonlit Ring, placed with the ring, his head raised to the moon over the northern trees, his eyes dark. He is the oldest of the werewolves, twice the height of a man:
- a hunched back of black fur under a silver-grey mane and ruff;
- a long scarred muzzle with fangs bared, a torn right ear, and eyes that burn moon-white once he wakes;
- long arms ending in sickle claws, and a bushy tail.

His body is 1.6 blocks wide and 3.6 high; his muzzle, arms and tail reach past it, about two blocks across and four high to his ears.

Waiting, he takes no harm. He wakes when a player who can fight him (anyone not in creative or spectator mode) steps off the camp onto the ring's turf (in the clearing, within 22 blocks of its middle, looked for every half-second), or strikes him. Then:
- "The Alpha Werewolf rises on the Howling Stone and howls at the moon, and his pack lopes out of the trees": he rises and howls for two and a half seconds, his eyes blazing, and nothing can hurt him meanwhile;
- a second in, his **pack**, three **Packmates**, lope out of the trees onto the path round the clearing, north-west, north and north-east of it;
- then he leaps down off the stone, in half a second, onto the spoke nearest the nearest player, six blocks from the middle;
- his health is set for the party: 400, half as much again for each player after the first within 38 blocks of the clearing's middle (the camp included), at most two and a half times (1000 for four or more).

He has armour 8 and cannot be knocked back. Poison, Wither, drowning and falls never harm him. He cannot ride, be leashed or be pushed. He lopes at 0.22 blocks a tick (a little slower than a sprinting player), stops two blocks short of his foe, and bounds up a block (the logs, the boulders, the stone's steps). His hunting ground is the clearing, its spokes and the three bell clearings: he fights only players there, and his blows reach only them; the Hunters' Camp is safe. He never chases farther than 25 blocks from the middle, and if he is ever more than 26 from it, or fallen below its floor, he is set back on the spoke nearest his foe.

### His hide

His hide is his kind's ([even-more-fall-additions.md](even-more-fall-additions.md), addition 23): every blow, arrow and spell does half its damage, before his armour, now and then with the words "His hide turns the blow: silver bites deeper, or flush him with a bell". **Silver** does full damage: a weapon in `#jugcraft:silver_weapons` (the silver dagger) in the main hand, or a silver arrow. Silver is a help, never a need: the bells open him to every weapon.

### His rule: the Hunt

**Every 24 seconds** of fighting (20 in the Moon's Fury, 16 in the Last Hunt; the count waits while he attacks, staggers or leaps) he hunts:
- **He marks his quarry,** the player he was fighting: he throws back his head in a short, rising howl (1 second), his bar reads "The pack hunts Steve", everyone near hears "He marks Steve for the pack, and bounds off into the trees!", and his pack turns on them.
- **He goes to ground:** he bounds off (1.5 seconds), through anything and over the stone, to the middle of one of the three bell clearings, never the one nearest his quarry (one of the other two, at random). His claws tear the floor he crosses: a **trail of raked turf** two blocks wide (moss, trodden path or earth alike) leads from where he was to where he hides. "He is gone into the dark of the trees: find his trail, and ring the bell of his clearing." He crouches there, low, his eyes burning out of the clearing's dark. Running and hiding, nothing reaches him.
- **His pack drives the quarry:** the Packmates close in on them.
- **The ambush lasts 10 seconds.** 1.2 seconds before it ends, a dark **shadow** spreads where the quarry then stands, and 0.6 seconds before it ends he springs from his clearing at it in a long, high leap: 14 damage within 2.5 blocks of the shadow, and whoever it strikes is knocked flat (Slowness IV, 1 second). Step off the shadow.
- **Ring the Warden Bell of his clearing before he springs,** and he is **flushed:** "The bell rings out: the Alpha is flushed from hiding, and his hide fails him!". He staggers out along the clearing's spoke into the open (1.5 seconds, 17 blocks from the middle), and for **8 seconds** every blow does full damage, and silver half as much again. His bar reads "The Alpha Werewolf: flushed!". His pack **cowers** among the trees for 4 seconds, whining and running from the nearest player, biting nobody. Whoever rang it takes part. The next hunt waits its full gap.
- **Any other bell,** or a bell rung while he is not lying in wait, only tolls: "The bell rings out over an empty clearing". Each bell can be rung once every 2 seconds (part 1).
- If the quarry leaves his hunting ground (back to the camp) or dies, the nearest other player there becomes the quarry, and the bar says so; if nobody is left, he comes back out of the trees and the hunt is off.

From anywhere in the clearing, a sprinting player reaches the bell of any clearing within the ambush, before he springs; the audit walks the ring to keep that true (below).

### Phase 1, the Chase (full to half health)

Every attack has a wind-up you can read, a strike and a recovery. He waits at least 0.8 seconds between attacks.

| Attack | Read it by | What it does |
| --- | --- | --- |
| Claw Rake | He rears up and spreads his claws (0.7 s) | Two rakes across his front, 0.3 s apart: 6 damage each to everyone within 3 blocks, 70° either side of his face. Step back or aside. Close in, his most frequent attack |
| Pounce | He drops into a crouch, his tail lashing, and a shadow spreads at his foe's feet (1.2 s) | He leaps onto the shadow (half a second, up to 12 blocks from where he crouched): 9 damage within 2 blocks of it, and pushed off it. Step off the shadow. From 4 to 12 blocks |
| Rending Howl | His hackles rise and he fills his chest (1.0 s) | Weakness I for 6 seconds on every player within 10 blocks, and his pack runs faster (Speed II) for 6 seconds. Get out of earshot |
| The Hunt | His short, rising howl (1.0 s) | Above |

### The Pack Gathers (at half health)

"The Pack Gathers: he leaps onto the Howling Stone and howls long at the moon":
- he leaps onto the stone's top and howls for 2 seconds, unhurt; any attack or hunt under way is spent;
- as the howl rises his pack is called back up to three: each Packmate killed lopes out of the trees again;
- then he leaps down onto the spoke nearest the nearest player, and the Moon's Fury begins. His bar reads "The Alpha Werewolf: the Moon's Fury".

### Phase 2, the Moon's Fury (half to a quarter)

He keeps every attack, hunts every 20 seconds (the next within 20), and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Tree Leap | He springs into the canopy on his foe's side and is gone, the leaves shivering where he lurks (1.5 s), then a shadow spreads 2 blocks behind his foe (1.2 s) | The last 0.6 s of the shadow he drops out of the trees onto it: 10 damage within 2.5 blocks. Step off the shadow. In the trees nothing reaches him. From up to 30 blocks |
| Hamstring Bite | He lunges low, jaws wide (0.9 s) | A bite on his foe within 3 blocks: 8 damage and Slowness II for 2 seconds. Step back |

### The Last Hunt (below a quarter)

"The Last Hunt: he will change his route": he hunts every 16 seconds (the next within 16), and his bar reads "The Alpha Werewolf: the Last Hunt". He changes his route, not his speed: **3 seconds into every ambush** he slips from his clearing through the trees to another (one of the other two, at random, in 1.5 seconds), tearing a second trail: "He slips through the trees to another clearing!". The bell to ring is the one of the clearing he is in when it rings; while he slips, no bell flushes him. His tells stay as long as ever: a player at the stone's foot who sets off as he slips reaches his new clearing's bell before he springs.

### His pack, the Packmates

Young werewolves of his blood, drawn as the brown werewolves of the woods are but smaller (0.85 of their size), with the same red eyes. At most three at a time:
- 24 health and no hide: every blow does full damage, and silver half as much again;
- a bite of 4 within 1.6 blocks, once a second; fast (0.32 blocks a tick);
- in a hunt they harry his quarry, and his foe otherwise;
- **a sprig of wolfsbane** in either hand keeps them from biting its holder: they circle three blocks off and growl. It does not keep him off;
- they cower while he is flushed;
- whoever hurts one takes part in his fight;
- killed, they stay dead until the Pack Gathers. They drop nothing, give no experience and are never saved, and they melt back into the trees when he falls, goes back to his stone or is gone.

### His fall

When he falls:
- his pack melts into the trees;
- each participant gets their loot (below) and Hunter's Moon;
- "The Alpha Werewolf slumps on the turf with a last long howl, and his eyes go dark": he howls, slumps and his eyes go dark; his trail knits back at once; and a gate of **Grey Mist** three blocks wide and two high opens on the Howling Stone's top. It takes players home, as the Grey Mist behind the camp does;
- the instance is ended, and it closes once everyone has left. The next instance in that slot is placed fresh, and he crouches on the stone again; the Grey Mist he opened in that slot before is cleared first.

### Left alone

If no player he can fight is in his hunting ground for 10 seconds: "The Alpha Werewolf slinks back to his stone, and his pack melts into the trees". He is back on his stone at once, crouched and healed: his pack is gone, his trail knits, his eyes are dark, and who hurt him is forgotten, so a fresh fight starts from nothing.

### His trail: raked turf

Raked turf is the ring's floor torn by his claws: moss and earth clawed into ridges. It is a lair fixture like the ring's others (unbreakable, no item, no drops). It remembers what it was torn from (moss, a trodden path, coarse dirt, podzol or rooted dirt) and knits back to that by itself 30 seconds later, long after his ambush ends. The ferns and grass on it stand. When he goes back to his stone or falls, all of it knits at once.

### His loot

Each participant rolls their own loot. A participant is a player who hurt him or his pack, or rang the bell that flushed him, and is within 64 blocks of the clearing's middle when he falls. The loot goes straight into their inventory, and what does not fit lands at their feet. "The pack's leader is fallen: his spoils are yours".

| Loot | Chance |
| --- | --- |
| **The Alpha Pelt**, his silver-grey pelt | 1, always |
| **A trophy:** Moonfang or the Howler, Arms VII's (one of the two) | 15%; one is certain on a player's first kill (whoever has not yet earned Hunter's Moon) |
| **The Packfinder Charm** | 25% |
| **The Alpha's Hood** | 20% |
| 300 experience, shared out among the participants | always |
| The advancement **Hunter's Moon** (a challenge in the Adventure tab): "Flush the Alpha Werewolf from his hiding place and bring him down in the Moonlit Ring" | always |

He is no Witching Season boss: the Halloween event adds nothing to his loot.

**What they are for:**
- **The Alpha Pelt:**
  - **For those who never fight him:** it tans into four leather (shapeless, one pelt; a werewolf's pelt gives two).
  - **Another hunt:** it counts as a werewolf's pelt (`#jugcraft:werewolf_pelts`), so it binds a Howling Totem, one pelt for one totem.
- **The Packfinder Charm:** a wolf's fang on a leather cord, set with a pale stone. Held in the offhand, every two seconds it makes every werewolf within 32 blocks glow for three seconds, for everyone to see: the werewolves of the woods on full-moon nights, his Packmates and he himself.
- **The Alpha's Hood:** a costume worn on the head, his silver-grey wolf's head pulled over the wearer's: its crown and sides round the temples, his ears standing up from it (the right one torn short), and his long muzzle jutting over the brow. It counts for trick-or-treating and the costume contest.
- **Moonfang and the Howler:** Arms VII's two Alpha Werewolf trophies, a sabre and a twinblade, until now in the creative tab only. They fight as the steel sabre and twinblade do, are epic, last twice as long as steel, and carry his boon, **Howl**: a hit cows the foe, weakening its blows (Weakness, 3 s).

### Settings

His settings are the ones every lair boss shares, in `config/jugcraft.properties` (`lair/LairBosses.java`):
- `lairs.boss_health` (1.0, from 0.25 to 4): multiplies his health;
- `lairs.boss_damage` (1.0, from 0.25 to 4): multiplies his spring, Claw Rake, Pounce, Tree Leap, Hamstring Bite and his Packmates' bites (not the Slowness and Weakness, which are vanilla's).

`lairs.event_loot` does not touch him: he has no Halloween extra. His damage numbers are Normal difficulty's: Easy and Hard scale them as they scale every monster's blows.

## Changes from the plan

- **In the Last Hunt he slips 3 seconds into the ambush,** not halfway through. Halfway, a player who set off as he slipped had under three seconds to reach the second clearing's bell, too little from anywhere; at 3 seconds a player at the stone's foot who sets off as he slips reaches any bell before he springs. `tools/check_mod_data.py` walks it.
- **His trail tears every floor of the ring he crosses,** not only moss: the spokes and the path round the clearing are trodden earth and the glades podzol and coarse dirt, so a run along a spoke would have left no trail. Raked turf remembers what it was and knits back to that, 30 seconds after; the plan gave no time.
- **Running and hiding, nothing reaches him;** the plan did not say. His trail, his eyes in the clearing's dark and the bells are the way to him.
- **Waking,** his pack lopes out a second into his howl, onto the path round the clearing north-west, north and north-east, and he leaps down off the stone onto the spoke nearest the nearest player; the plan said only that he rises and howls and the pack lopes out.
- **The Pack Gathers** ends with the same leap down.
- **Flushed, he staggers out along his clearing's spoke,** 17 blocks from the middle, into the open.
- **The Pounce pushes off** whoever it strikes; the plan said "thrown down".
- **The Tree Leap's leaves shiver where he lurks** in the canopy on his foe's side, 22 blocks from the middle and 11 up; the plan said "along his path".
- **Knockback never moves him;** the plan said "never moves him much".
- **A bell rung while he is not lying in wait** also tolls over an empty clearing.
- **If his quarry leaves,** the nearest other player in his hunting ground becomes it, and if nobody is left the hunt is off.
- **His Packmates are drawn as the brown werewolves are,** smaller; the Rending Howl gives them Speed II.
- **He is a monster:** on Peaceful he is gone, as every monster is, and the ring stands empty.
- **Animations by GeckoLib,** as the other lair bosses' are: GeckoLib 5.5.7 is already pinned.

## Connections

- **Inputs:** the way in is part 1's Howling Totem, so a werewolf's pelt from a full-moon night, bones and silver feed every fight.
- **Outputs:**
  - **the Alpha Pelt** feeds back into the summoning (a totem, one for one) and into leather;
  - **the Packfinder Charm** helps whoever hunts the werewolves of the woods on full-moon nights ([even-more-fall-additions.md](even-more-fall-additions.md), addition 23), and finds his pack in the ring;
  - **the Alpha's Hood** is a costume for trick-or-treating and the costume contest (`#jugcraft:trick_or_treat_costumes` and `#jugcraft:costume_hats`);
  - **Moonfang and the Howler** are Arms VII's boss trophies: the sixth of its eight bosses to drop his.
- **Technology and magic:** silver (smelted from the first days) bites through his hide, and wolfsbane (grown) keeps his pack off. Of the plan's magic links (Echo reading the hunt's target, a Verdance refuge, Tether interrupting a pack signal), none is in the game yet; none is needed.
- **Trade and solo routes:** a solo player can do everything. A bell is rung by using it, his hiding place is shown by his trail and his eyes, every attack is read before it lands, and the flush opens him to any weapon. Alone, the quarry can reach his bell in time, and silver and wolfsbane only make it easier. His health is lowest for one player, and the first kill always brings a trophy. His loot can be traded like any item.
- **Reachable entry path:** the totem takes what the werewolves of the woods and the first days give without him (part 1), and nothing he drops is needed to reach him.

## Balance and automation

- **Every fight costs a summoning:** a fresh Howling Totem, as part 1 set it.
- **No gain loop:** the Alpha Pelt makes one totem (which needs bones, silver and a whole fight) or four leather; no recipe makes the Alpha Pelt, so nothing he drops turns into more of itself. Leather makes no pelt. Nothing he drops sells to a Jugcraft shop for Jugs.
- **No leeching:** only participants get loot. A bystander who took no part gets nothing, and loot is rolled per participant, so nobody takes another's.
- **First kill once:** the trophy's first-kill guarantee is stored with the advancement, so it comes once per player.
- **Abandoning resets him:** leaving him alone for 10 seconds heals him, knits his trail and forgets everyone's part. He cannot be worn down between visits.
- **Never hidden for good:** every hunt ends in a spring or a flush, and from anywhere in the clearing his clearing's bell is within a sprint before he springs.
- **No automation:** he wakes only for a player, a bell flushes him only from a player's hand, and his loot goes only to players. His Packmates drop nothing and give no experience.
- **Units:** health and damage in half hearts, before `lairs.boss_health` and `lairs.boss_damage`. Times are in ticks, 20 a second; distances in blocks, his reaches from his middle. Every number is in `tools/alpha_werewolf.py`, and `tools/check_mod_data.py` holds the Java to it.

## Multiplayer and persistence

- **Server authority:** the server decides everything: waking, targets, the hunt and its quarry, where he hides, every attack's hits, his hide and his flush, the trail, his pack's bites, the Packfinder Charm's glow, the loot and the advancement. Clients only draw what his synced state says (his action and his eyes) and play the matching animations. A bell rung tells him on the server, and only the Alpha whose ring it is answers; the game itself checks that a player is near enough to use a bell.
- **The Packfinder Charm** is checked every two seconds on the server for each player in the level: a look at the offhand, and for each one holding it a search 32 blocks round them.
- **What is saved:** he is saved with his instance's chunks: his phase and where his ring is. A passing moment is not resumed: loaded while waking, he fights on; loaded gathering his pack, the Moon's Fury begins. His hunt, his leaps and his pack are not saved: loaded fighting, he is set down at the stone's foot. His Packmates are never saved. Instances close on a restart (part 1), and he goes with his.
- **The floor:** the only blocks he changes are the ring's floor (raked turf, which knits back by itself and all at once when he resets or falls) and the Grey Mist he opens on the stone when he falls. Each instance is placed fresh.
- **Chunks:** nothing is force-loaded. His hunting ground stays loaded while players stand in the ring.
- **No griefing:** he cannot leave his lair, his attacks strike only players who can fight him in his hunting ground, and every block he changes is put back.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. GeckoLib 5.5.7 for 26.3 is already pinned (`distribution/frameworks.lock.json`); it draws him. His Packmates use the werewolves' own model (`WerewolfModel`) and brown fur. Everything is drawn by code:
- **Body and clips:** `tools/alpha_werewolf_models.py` builds his GeckoLib model and animations on Vesperine's builder: 21 clips (crouched, waking, idle, loping, galloping, every attack's wind-up and strike, the mark, hiding, the spring, the stagger, the gathering and death; his eyes dark and lit, on a controller of their own).
- **Textures:** `tools/alpha_werewolf_art.py` paints them at four times Minecraft's scale, 1024 × 1024: black fur, the silver mane and ruff, the scar and the torn ear, ivory claws and fangs. His glowmask lights his eyes. It also paints the worn hood's fur and muzzle. Raked turf's top and side are painted by `tools/moonlit_ring_textures.py`, with the ring's other blocks.
- **Icons:** the Alpha Pelt, the Packfinder Charm and the hood from icon maps (`tools/item_icons/`); the trophies' art is Arms VII's.
- **Data:** `tools/alpha_werewolf_data.py` writes his body, the names, tooltips and messages, the item models (the hood worn as its own model), the loot table, the leather recipe, the costume tags and the advancement. Raked turf's model and states are written with the ring's blocks (`tools/lair_data.py`).
- **Sounds:** vanilla's: the wolf's howl, growl, whine, hurt and death, the ravager's footfall, the sweep of a blade, azalea leaves.

No Mojang texture is read, traced or copied.

New IDs (all under `jugcraft`):
- the entities `alpha_werewolf` and `packmate`;
- the lair-only block `raked_turf`;
- the items `alpha_pelt`, `packfinder_charm` and `alphas_hood`;
- the recipe `leather_from_alpha_pelt`, the loot table `entities/alpha_werewolf`, and the advancement `hunters_moon`.

Moonfang, the Howler and `bosses/werewolf_alpha` were already Arms VII's. Nothing is renamed.

## Verification

PR #408 turf-test repair (16 October 2026, Codex/GPT-6, owner requested): the test now checks a known moss cell and the dirt-path cell separately instead of assuming the first cell of an unordered trail was moss. It verifies scheduled restoration for both, invokes each restoration tick, then tears both again to retain coverage of their restoration when the Alpha abandons the fight. Gameplay code is unchanged.

Focused validation on `6c4132681` plus this repair: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.0.4.1+1; fresh GameTest run directories using the `jugcraft-test:moonlit_glade` fixture. Ran `./gradlew runGameTest -I build/focused.gradle -PgameTestFilter=jugcraft-test:alpha_werewolf_game_tests_left_alone_he_goes_back_to_his_stone_and_his_trail_knits` through the local Java/Gradle wrapper and an ignored init script that forwards `gameTestFilter` to the test JVM as `-Dfabric-api.gametest.filter`, once with `-PjugcraftOptionalIntegrations=false` and once with default integrations. Both runs passed exactly one required test (1.654 s and 1.610 s respectively). The full local suite and client tests were not rerun, as requested. GitHub CI for the pushed repair is separate from these focused results.

Original branch CI [run 38667558143](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38667558143): server jobs passed; the Alpha client test failed with `He did not mark the player: NONE`. The test resumed a pose frozen during Rending Howl, whose remaining recovery intentionally pauses the hunt timer. The [October 15 integration](oct15-community-integration.md) waits for the bounded server transition to `MARK` instead of assuming eight client ticks. The repaired local rendered client run passed the entire fight, loot and fresh-instance reset on 15 October 2026 (`ae1adb4fe` runtime source). Both combined server suites passed all 1,734 tests. Combined GitHub CI remains pending; combat timing is unchanged.

Run locally (15 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks him (`check_alpha_werewolf`, below) | Pass, 2663 IDs |
| Deliberate breaks of his Java, the tables and the data, one at a time (below) | 60 of 60 failed it |
| `python3 tools/check_icon_maps.py` on the three new maps | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded, with identical pixels or for other features, were put back) |
| `javac` on the new and changed Java and the two test classes, for syntax only | No syntax errors |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

`check_alpha_werewolf` checks:
- his Java numbers, his attacks, his pack's, his trail's and the charm's numbers against `tools/alpha_werewolf.py`;
- that his rule can be played: the Pack Gathers before the Last Hunt; his hide turns some but not all of a blow, and a flush lets silver bite deeper; his hunts come sooner as he weakens, and each ends before the next is due; the shadow spreads before he springs; in the Last Hunt he reaches his second clearing before the shadow spreads; a flush outlasts his stagger and his pack cowers no longer; the Tree Leap's wind-up is its time in the trees and its shadow; each attack is used only within its reach; both rakes fall in the Claw Rake's strike; his trail outlasts his run and his ambush;
- that his waking, gathering, mark and every wind-up last as long as their clips;
- that where his fight looks for the ring's parts (`MoonlitRing.java`: the stone's steps and the Grey Mist on its top) is where `tools/moonlit_ring.py` builds them, and that his body fits where he waits on the stone, where he leaps down, in each clearing's middle and where he staggers out of each (in the clearing, within his leash); his pack lopes out onto open path; where he lurks in the Tree Leap is in the canopy; and the Grey Mist he leaves opens in open cells on the stone's top;
- that every floor of the clearing and the bell clearings is one his claws tear, so his trail always shows;
- that a sprinting player (0.28 blocks a tick) anywhere in the clearing comes within reach (4.5 blocks) of every bell before he springs (at most 178 of 188 ticks, over open floor, never cutting a corner), and in the Last Hunt one at the stone's foot setting off as he slips does too (at most 123 of 128);
- that his hide lets half of a plain blow through and a flush all of it (silver half as much again), nothing reaches him waking, gathering or hidden, a bell is answered only by its own ring's fight and flushes him only from the clearing he hides in;
- that whoever hurts a Packmate takes part, silver bites deeper and it melts away when he is gone; that raked turf knits back by itself and is one of the ring's fixtures; that the Packfinder Charm works from the offhand;
- his registrations, names and renderers (his glow layer, his pack drawn as the werewolves are); his GeckoLib model, every clip the Java plays, sheet sizes, glowmask and his controllers' bones; the loot table and his trophies against Arms VII's; the recipe (and none making the Alpha Pelt), costume tags, advancement and messages; and that nothing of his loads chunks or changes dimension.

The 60 deliberate breaks: his health, slip, a Packmate's bite, the charm's reach, the knit time, the pack's angles, an attack's cooldown and the bite in the Chase in the Java alone; the slip too late, the ambush too short, the Last Hunt before the Pack Gathers, slower hunts, the spring before the shadow, the Tree Leap's wind-up, the Pounce beyond its range and the trail knitting too soon in the table alone; and the same rules broken in the table and the Java together (the slip, the ambush, the Last Hunt, the hunts, the spring, a flush shorter than its stagger, a hide that turns nothing, the flush spot past his leash, the down spot inside the stone, the canopy spot in the open, the pack out in the trees, the trail knitting too soon, both rakes not in the strike, the Grey Mist a block into the stone); raked turf not tearing paths, a floor his claws would not tear, the Java misreading the stone's steps; his hide letting it all through, any bell flushing him, a blow reaching him hidden; a clip the model lacks, no eyes controller, a wake clip shorter than his waking; a message without words, a message he never sends; his box too wide for the stone's top; the flush spot, the down spot, the canopy spot and the pack's spot in the table alone; the stone's steps in the Java; a trophy chance, a recipe making the pelt, the hood no costume, the advancement granted by a trigger, the trophies not Arms VII's; the summoning or the bells not hooked; the charm from the main hand; the pack drawn as something else; no glow layer; a chunk loaded; a Packmate's hurt not counted; raked turf not knitting.

What the building found:
- Halfway through the ambush, the Last Hunt's slip left a player too little time to reach the second clearing's bell; he slips at 3 seconds (above).
- The spokes and the path round the clearing are trodden earth, so a trail of torn moss alone would not have shown along them; raked turf tears every floor of the ring and remembers it (above).
- Where he lurks in the Tree Leap, 21 blocks from the middle, sat on the canopy's ragged edge; at 22 it is always under it.
- The hood's crown was left open underneath, a hole the art check found; it is closed.
- The audit did not know his items until they were listed with the other bosses' (`tools/agriculture.py`), which also tells the recipe audit that the Alpha Pelt holds no metal.

The game tests (`AlphaWerewolfGameTests`) show what a game-test server can, with an Alpha put down on his own (no lair) over a ring laid out from the corner of an arena the clearing's size; each test lays a floor of moss and the bells it rings:
1. he crouches on his stone, his eyes dark, and takes no harm; a player in the camp does not wake him, nor does a creative player on the turf or their blow; a player who may fight him on the turf does, at full health, his eyes lit, unhurt while he wakes; his pack of three lopes out, and he leaps down onto the south spoke, toward the player, his health one player's;
2. his health scales 1, 1.5 and 2.5 times for one, two and four or more players, and waking with two players near his ring he has 1.5 times his health; Wither and poison never take hold of him; a player's blow from the camp wakes him, unhurt; his hide lets at most half of a plain blow through and silver all of it; flushed, a plain blow lands in full and silver half as much again; a blow counts as taking part, and standing by does not;
3. the Hunt marks his quarry (the bar naming them) and turns his pack on them; he runs to a clearing not their nearest and lies in wait in its middle, out of reach of a blow, over a trail of raked turf in the moss along the line he ran;
4. the bell of another clearing does not flush him; his own clearing's bell does: his hide fails for 8 seconds, his pack cowers, he staggers, and the ringer takes part; he staggers out along its spoke, and the next hunt waits its gap;
5. unflushed, a shadow spreads under his quarry and he springs onto it: 14 to the quarry, knocked flat, nothing to a player off the shadow; the hunt ends and the next waits its gap;
6. in the Last Hunt he slips to another clearing, leaving a trail; the bell of the one he left does not flush him, and the one he is in does;
7. at half health the Pack Gathers: he leaps onto the stone, unhurt, and howls his pack back up to three; then the Moon's Fury follows, hunting sooner, and he leaps down; the Chase's attacks are the Claw Rake, the Pounce and the Rending Howl, and the Moon's Fury adds the Tree Leap and the Hamstring Bite;
8. his Claw Rake strikes the player before him for 6 and not the one behind; his Hamstring Bite bites and slows (Slowness II); his Rending Howl weakens the two players within 10 blocks, not one farther, and quickens his pack;
9. his Pounce casts its shadow at his foe's feet and lands on it for 9, sparing one off it; his Tree Leap takes him into the trees, unseen and out of reach, casts its shadow 2 blocks behind his foe, and drops him onto it for 10;
10. a Packmate's bite does 4; a sprig of wolfsbane wards a player off; a plain blow does full damage to one and silver more, and counts as taking part; a Packmate is smaller than a werewolf; flushed, he sets his pack cowering; back on his stone, his pack melts away;
11. his trail tears the moss and a trodden path alike, knits back to what each was by itself (a tick scheduled), and cannot be broken and has no item; left alone 10 seconds, he goes back to his stone, healed, his eyes dark, his pack gone and his trail knit at once, the path a path again;
12. the Packfinder Charm in the offhand makes a werewolf 20 blocks off glow and not one 40 off; the Alpha Pelt tans into four leather and binds a Howling Totem; the Alpha's Hood is a costume worn on the head;
13. his loot is each participant's own: the Alpha Pelt and, on a first kill, one of his trophies into the inventory of each who struck him and of one who only rang the bell that flushed him, with a third of his experience each, Hunter's Moon theirs afterwards, nothing for a bystander; he falls with his eyes going dark, and with no lair instance no Grey Mist opens.

The client test (`AlphaWerewolfClientGameTests`) runs his whole fight in a real Moonlit Ring, with the one player in survival under Resistance: he crouches on his stone; stepping onto the turf wakes him, he howls and his pack lopes out; the Chase's Claw Rake, Pounce and Rending Howl; a hunt, his trail of raked turf into the clearing where he hides; the player ringing that clearing's bell, flushing him out; a second hunt left to run, his spring onto the shadow under the player; the Pack Gathers on the stone; the Moon's Fury's Tree Leap and Hamstring Bite; and his fall: the player's Alpha Pelt, trophy and Hunter's Moon, his pack gone, his trail knit, Grey Mist on the stone; then a fresh instance in his slot with no Grey Mist and him on his stone again. Twelve pictures along the way.

Not run: the two-client dedicated-server playtest, and play by hand.

## World and event applicability

He lives only in the Moonlit Ring, his own dimension, so nothing here changes the Overworld. The Packfinder Charm works anywhere. No season or moon phase gates him, and everything he drops is kept for good.

## Rollout and open questions

- **Difficulty:** his numbers are the plan's, but for the Last Hunt's slip. After a hand playtest the owner may want his hide thicker or thinner (half), his ambush longer or shorter (10 s), his flush longer or shorter (8 s), or his hunts more or less often (24, 20 and 16 s).
- **Still to do:** the two-client dedicated-server playtest, and a play by hand.
