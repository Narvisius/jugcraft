# The Crypt Lich

Status: implemented in source; CI has not run on it yet (results and the client test's pictures go below once it has). This is part 2 of boss 6 in the [bosses plan](../branches/BOSSES.md#the-crypt-lich-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the boss of the Archive of Last Names, his ward and phylactery, his Restless Dead, his Soul Bolts and bones, and his loot. It is built on part 1, the archive and the Writ of Exhumation that opens it ([crypt-archive.md](crypt-archive.md)), and on the lair framework ([hollow-acre.md](hollow-acre.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 13 October 2026 the owner answered the Mire Hag's checkpoint with "go with recommendations", whose next chunk was the Crypt Lich, built the same two-part way: the crypt lair and its summoning, then the Lich, who raises the dead in waves, drains life with a soul beam, and has a phylactery that must be broken; the Crypt Lich is the next of the first eight in [branches/BOSSES.md](../branches/BOSSES.md).
Owner: @jimbozoomer-byte

Target milestone and tier: Specialization tier (dungeon expeditions), as the lairs are. He is reached only through the Writ of Exhumation, which takes things a player has early on: paper, bones, an ink sac and a bottle of ectoplasm from a restless spirit. Nothing a tier needs comes only from him.
Primary specialty and supported player role: adventuring. His loot also serves the summoning's makers (Grave Dust in the next writ), gardeners (Grave Dust into bone meal), whoever fights wither skeletons (the Emptied Phylactery) and costume wearers (trick-or-treating and the costume contest).

## Player experience

### He waits

The Crypt Lich waits on the bone throne in the north apse of every Archive of Last Names, placed with the archive, seated, his crown dark. He is a crowned skeleton, gaunt and a head taller than a man (0.9 blocks wide, 2.5 high):
- a scholar's robe gone to mould, long to the floor, hung with chains of tarnished gold;
- a skull with eyes of cold blue flame, and on it a crown of black iron with eight points, soul-fire burning on them;
- long bony arms in bell sleeves; in his right hand a staff, a thighbone bound to a sceptre; in his left, as he readies a Soul Bolt, a skull of soul-fire.

Waiting, he takes no harm. He wakes when a player who can fight him (anyone not in creative or spectator mode) stands on the hall's floor (anywhere in the hall, but not on the landing or the stair, which stay safe), or strikes him. Then:
- "The Crypt Lich rises from his throne: soul-fire runs down the lanes, and his chains burn", his crown lights, and his bar shows;
- he rises from his throne for three quarters of a second, then comes down the apse's steps in an arc in a second and a quarter, onto the north lane two blocks into the hall; nothing can hurt him meanwhile;
- his health is set for the party: 400, half as much again for each player after the first within 38 blocks of the dais, at most two and a half times (1000 for four or more);
- his chains are forged and the first name lights.

He has armour 8 and cannot be knocked back. Wither, poison, drowning and falls never harm him. He cannot ride, be leashed or be pushed. He glides over the floor at 0.16 blocks a tick and stops three blocks short of his foe. If he is ever more than 30 blocks from the dais, or fallen below the floor, he is drawn back in a burst of souls, four blocks north of the dais.

### His rule: the phylactery

Three **soul chains** of blue fire run from the three grave seals to his phylactery on the dais, and a fourth from it to him. While they hold he is **warded**: every blow, arrow and spell is turned aside with a flare and a clink of chains, and does nothing ("His chains turn the blow aside: break the lit name's seal", now and then).
- **One name at a time is lit:** its name-stone burns, its seal flickers with soul-fire, and his bar names it: "The Crypt Lich: he answers to the Scribe". The names light in an order of his choosing, new each time his chains are forged.
- **Use the lit name's seal** (any player, by hand) and it breaks: "The seal of the Scribe breaks, and its chain snaps", and the next name lights.
- **Use a seal whose name is dark,** and it holds: "That name is dark: the seal holds, and the dead stir", and a Restless Dead claws its way out at the foot of that alcove's sarcophagus.
- **Break all three,** and "The phylactery cracks: the Crypt Lich is exposed!": he is **exposed** for 12 seconds and staggered for the first 2, and every blow hurts him. Then his chains reforge over a second and a half, the seals mending one by one, and a name lights again ("The chains reforge, and a name lights").

Breaking a seal and reading a name back both count as taking part in his fight.

### Phase 1, the Warded King (full to half health)

Every attack has a wind-up you can read, a strike and a recovery. He waits at least a second between attacks.

| Attack | Read it by | What it does |
| --- | --- | --- |
| Grave Sweep | He draws his staff back over his shoulder, soul-fire on it (0.7 s) | His staff sweeps 3.5 blocks past his body, 70° either side of his face: 7 damage, and thrown back. Close in, his most frequent attack |
| Bone Lanes | He calls a name ("The Lich calls the Sexton: the lane cracks with blue fire"), and the lane to its alcove cracks with blue fire (1.2 s) | Bones burst up the whole lane, a clump every 2 blocks down its middle, for a second: whoever stands on it takes 6 damage and is tossed up. He calls the lane nearest his foe; off the lane is safe |
| Soul Bolt | A skull of soul-fire forms in his left hand (0.8 s) | It drifts at his foe at 0.3 blocks a tick, turning a little: 6 damage, and Wither for 3 s. Struck back (any blow or arrow), it turns and flies at him, twice as fast: warded, it staggers him for 2 seconds and spoils the attack he had begun; exposed, 10 damage |
| Raise the Dead | He spreads his arms and the floor graves nearest his foe glow (1.5 s) | Two Restless Dead climb out of the ledger stones nearest his foe; at most four walk at once |

**A Restless Dead** is a skeleton a little shorter than a man, hunched in a torn grave-shroud, its eyes cold soul-fire. It climbs out for a second, unhurt meanwhile, then shambles at the nearest player: 20 health, 4 damage a blow, armour 2. Whoever hurts one takes part in his fight. It crumbles when he falls, climbs back to his throne or is gone, drops nothing and gives no experience.

### The Erasure (at half health)

"The Erasure: the Lich shrieks, and scrapes a name from its stone". He shrieks for 2 seconds, unhurt, and his chains reforge at once. From now on, whenever they reforge, he **scrapes the name that comes last** from its stone. When its turn comes it lights, but its stone stays blank, and its seal will not break: "The name is scraped from its stone: read it back at the Lectern of Last Names". Reading the names at the lectern (any player, using it) cuts it again, and "The scraped name burns back into its stone"; or, left lit for 30 seconds, it burns back by itself. Then the Unwritten begins.

### Phase 2, the Unwritten (half to a fifth)

He keeps every attack, calls two lanes at once, and adds:

| Attack | Read it by | What it does |
| --- | --- | --- |
| Soul Drain | A thin beam of blue light joins his hand to his foe (1 s) | For 3 s it takes 1 health every half second, 6 in all, and heals him as much. A block between his eyes and his foe's (a pillar), or 16 blocks, breaks it, and it ends |
| Grave Pull | A ring of soul-fire 10 blocks across flares round him, one sixth of it dark with smoke (1.5 s) | Everyone in the ring but those in its dark part is dragged to him; half a second later bones burst round his feet: 8 damage within 3.5 blocks. The dark part is the way out |

### The Last Name (below a fifth)

"The Last Name: his phylactery shatters, and his chains are gone". His crown flares; every name goes dark; he is exposed to the end. Every cooldown, and his pause between attacks, is a third shorter, and two of his dead rise every 12 seconds.

### His fall

When he falls:
- his dead crumble, and his bolts and bones are gone;
- each participant gets their loot (below) and Nameless;
- "The Crypt Lich falls: his crown rolls from his skull, and the soul-fire goes out": he sinks to his knees and his crown rolls from his skull, the soul-fire goes out down the lanes, and a gate of **Grey Mist** three blocks wide and two high opens on the dais where the phylactery stood. It takes players home, as the Grey Mist behind the landing does;
- the instance is ended, and it closes once everyone has left. The next instance in that slot is placed fresh, its phylactery whole on its plinth, and he waits on his throne again; the Grey Mist he opened in that slot before is cleared first.

### Left alone

If no player he can fight is within 38 blocks of the dais for 10 seconds: "The Crypt Lich returns to his throne, and the names are cut again". He heals fully, his dead crumble, his bolts and bones are gone, every seal is mended, every name is dark and cut again, the phylactery is whole, and who hurt him is forgotten, so a fresh fight starts from nothing.

### His loot

Each participant rolls their own loot. A participant is a player who hurt him or his dead, broke one of his seals, read a name back at the lectern or struck back one of his bolts, and is within 64 blocks of the dais when he falls. The loot goes straight into their inventory, and what does not fit lands at their feet. "His grave goods are yours".

| Loot | Chance |
| --- | --- |
| **Grave Dust**, his bones ground to dust | 3 to 6, always |
| **A trophy:** the Soulreaver or the Gravewarden, Arms VII's (one of the two) | 15%; one is certain on a player's first kill (whoever has not yet earned Nameless) |
| **The Emptied Phylactery** | 25% |
| **The Lich's Crown** | 20% |
| 300 experience, shared out among the participants | always |
| The advancement **Nameless** (a challenge in the Adventure tab): "Break the Crypt Lich's chains and bring him down in the Archive of Last Names" | always |

He is no Witching Season boss: the Halloween event adds nothing to his loot.

**What they are for:**
- **Grave Dust:**
  - **A cheaper Writ of Exhumation:** three paper, two Grave Dust and an ink sac, shapeless. The Grave Dust takes the place of the two bones and the bottle of ectoplasm. This is the plan's "part of what summons him".
  - **For those who never fight him:** one Grave Dust makes two bone meal, for the garden.
- **The Emptied Phylactery:** his urn of bone and gold, cracked and empty. Held in the offhand, Wither never takes hold of you: the server takes it off you on its tick, and its harm is turned aside. It does nothing against other harm.
- **The Lich's Crown:** a costume worn on the head, his crown of black iron with soul-fire burning on its eight points. It counts for trick-or-treating and the costume contest.
- **The Soulreaver and the Gravewarden:** Arms VII's two Crypt Lich trophies, until now in the creative tab only. The Soulreaver fights as the steel moonblade does and the Gravewarden as the steel executioner; both are epic, last twice as long as steel, read "Trophy of the Crypt Lich", and carry his boons: the Soulreaver's **Drain** (each hit heals its wielder half a heart) and the Gravewarden's **Wither** (Wither for 3 seconds).

### Settings

His settings are the ones every lair boss shares, in `config/jugcraft.properties` (`lair/LairBosses.java`):
- `lairs.boss_health` (1.0, from 0.25 to 4): multiplies his health;
- `lairs.boss_damage` (1.0, from 0.25 to 4): multiplies his Grave Sweep, Bone Lanes, Soul Bolt, Soul Drain and Grave Pull (not the Soul Bolt's Wither, which is vanilla's).

`lairs.event_loot` does not touch him: he has no Halloween extra. His damage numbers are Normal difficulty's: Easy and Hard scale them as they scale every monster's blows.

## Changes from the plan

- **He wakes when a player stands on the hall's floor,** anywhere in the hall and up to four blocks over it, looked for every half-second, or strikes him. The plan said "steps off the stair onto the hall's floor", as it is; the landing and the stair are not the hall's floor.
- **Waking** is three quarters of a second rising from the throne and a glide down the apse's steps in an arc of a second and a quarter, onto the north lane two blocks into the hall. The plan said 2 seconds, unhurt, as it is.
- **The Grave Sweep** reaches 70° either side of his face; the plan gave its reach only.
- **A Soul Bolt struck back** staggers him for 2 seconds while he is warded, and the stagger spoils the attack he had begun. The plan said "staggers him and spoils his next attack". It drifts at 0.3 blocks a tick, turning toward its mark, struck back flies at 0.6, bursts on anything solid and fizzles after 8 seconds.
- **The scraped name's 30 seconds** run from when it lights, not from the Erasure.
- **Bone Lanes' bones** burst a clump every 2 blocks down the lane's middle and stand for a second, then sink in half a second. Whoever stands on the lane, or over its edge, and no more than 2.5 blocks over the floor is struck.
- **A dark seal's Restless Dead** climbs out at the foot of that alcove's sarcophagus, and counts toward the four that may walk at once.
- **A player who strikes back one of his bolts** takes part in his fight too; the plan named hurting him or his dead, breaking a seal and reading a name back.
- **The Emptied Phylactery** also turns aside the Wither effect's harm while it is held, the moment it would land, besides taking the effect off on the server's tick.
- **His Restless Dead, Soul Bolts and bones are their own entities,** `jugcraft:restless_dead`, `jugcraft:soul_bolt` and `jugcraft:lich_bones`.
- **His bolts leave from his left hand** (1.3125 blocks ahead of his feet and 1.71875 up, where his model's left hand is as he looses it).
- **He is a monster:** on Peaceful he is gone, as every monster is, and the archive stands empty.
- **Animations by GeckoLib,** as the other lair bosses' are: GeckoLib 5.5.7 is already pinned.

## Connections

- **Inputs:** the way in is part 1's Writ of Exhumation, served on a stone sarcophagus in a crypt, so paper, bones, an ink sac and ectoplasm feed every fight.
- **Outputs:**
  - **Grave Dust** feeds back into the summoning (the cheaper writ), and into bone meal;
  - **the Emptied Phylactery** is an offhand charm against Wither (wither skeletons, the Wither's skulls' effect, his own bolts);
  - **the Lich's Crown** is a costume for trick-or-treating and the costume contest (`#jugcraft:trick_or_treat_costumes` and `#jugcraft:costume_hats`);
  - **the Soulreaver and the Gravewarden** are Arms VII's boss trophies: the fourth of its eight bosses to drop his.
- **Technology and magic:** none required. Radiance's Revelation, already in the game, makes a creature glow, so cast on him it shows him, though it does not break his ward. The plan's Echo, Hollow and Circlewright links wait for those schools.
- **Trade and solo routes:** a solo player can do everything. A seal is broken and a name read by hand, the lit name is shown by its burning stone, its flickering seal and the words on his bar, his bolts are struck back with any weapon or arrow, and every attack is read before it lands. Alone, a player breaks two seals, reads the scraped name back, and breaks the third. His health is lowest for one player, and the first kill always brings a trophy. His loot can be traded like any item.
- **Reachable entry path:** the writ takes what a player has early on (part 1), and nothing he drops is needed to reach him.

## Balance and automation

- **Every fight costs a summoning:** a fresh Writ of Exhumation, as part 1 set it.
- **No gain loop:** Grave Dust makes a writ that still needs paper, an ink sac, a stone sarcophagus in a crypt and a whole fight, or bone meal; no recipe makes Grave Dust, so nothing he drops turns into more of itself. Nothing he drops sells to a Jugcraft shop for Jugs.
- **No leeching:** only participants get loot. A bystander who took no part gets nothing, and loot is rolled per participant, so nobody takes another's.
- **First kill once:** the trophy's first-kill guarantee is stored with the advancement, so it comes once per player.
- **Abandoning resets him:** leaving him alone for 10 seconds heals him, mends his seals and forgets everyone's part. He cannot be worn down between visits.
- **Never warded for good:** every round of seals opens him, and a scraped name burns back by itself within 30 seconds.
- **No automation:** he wakes only for a player, a seal breaks and the lectern is read only by a player's hand, and his loot goes only to players. His dead, bolts and bones drop nothing and give no experience.
- **Units:** health and damage in half hearts, before `lairs.boss_health` and `lairs.boss_damage`. Times are in ticks, 20 a second; distances in blocks. Every number is in `tools/crypt_lich.py`, and `tools/check_mod_data.py` holds the Java to it.

## Multiplayer and persistence

- **Server authority:** the server decides everything: waking, targets, every attack's hits, the ward, which name is lit, a seal's breaking, the scraped name and its reading back, his dead, his bolts and their striking back, the Emptied Phylactery, the loot and the advancement. Clients only draw what his synced state says (his action and his crown's fire) and play the matching animations. A seal used or the lectern read tells him on the server, and only the Lich whose archive it is answers.
- **What is saved:** he is saved with his instance's chunks: his phase and where his archive is. A passing moment (waking, the Erasure) is not resumed: loaded then he lands in the hall and his chains reforge; loaded fighting, they reforge too, with a scraped name in the Unwritten; loaded in the Last Name, he has none. His dead, bolts and bones are never saved. Instances close on a restart (part 1), and he goes with his.
- **The fixtures:** the only blocks he changes are his own archive's seals (broken and mended), name-stones (dark, lit, scraped) and phylactery (cracked and whole), and the Grey Mist he opens on the dais when he falls. All are put back when he resets, and each instance is placed fresh.
- **Chunks:** nothing is force-loaded. His arena stays loaded while players stand in the archive.
- **The Emptied Phylactery** is looked at each tick on the server for each player holding it.
- **No griefing:** he cannot leave his lair, his attacks strike only players who can fight him, and every fixture he changes is put back.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. GeckoLib 5.5.7 for 26.3 is already pinned (`distribution/frameworks.lock.json`); it draws him, his Restless Dead, his Soul Bolts and his bones. Everything is drawn by code:
- **Bodies and clips:** `tools/crypt_lich_models.py` builds the GeckoLib models and animations on Vesperine's builder: 24 of his (seated, waking, idle, walking, every attack's wind-up and strike, the stagger, the shriek and death; his crown's fire, dark, lit and flaring, on a controller of its own; and the skull in his left hand, gone or forming, on another); the Restless Dead's climb, idle, walk and blow; the Soul Bolt's drift; and the bones' burst, stand and sink.
- **Textures:** `tools/crypt_lich_art.py` paints them at four times Minecraft's scale: 512 × 512 for him, 256 × 256 for the Restless Dead, the Soul Bolt and the bones. His glowmask lights his eyes, his crown's fire and the skull in his hand; the Restless Dead's its eyes; the bolt's its fire. It also paints the worn crown's iron and fire.
- **Icons:** Grave Dust, the Emptied Phylactery and the crown from icon maps (`tools/item_icons/`), on two new icon materials, old bone and soul-fire; the trophies' art is Arms VII's.
- **Data:** `tools/crypt_lich_data.py` writes the bodies, the names, tooltips and messages, the item models (the crown worn as its own model), the loot table, the two recipes, the costume tags and the advancement.

No Mojang texture is read, traced or copied.

## Verification

CI: not run yet on this branch. Its results, and the client test's pictures, go here once it has run.

Run locally (13 October 2026):

| Check | Result |
| --- | --- |
| `python3 tools/check_mod_data.py`: now also checks (`check_crypt_lich`) his Java numbers, attacks and his things' numbers against `tools/crypt_lich.py`, and that his rule can be played (the Erasure comes before the Last Name; his exposure outlasts its stagger; every seal mends within the reforging; a scraped name burns back by itself; Raise the Dead never raises more than his cap; the Grave Pull's bones reach less far than its ring, so its dark part is a way out; a Soul Bolt reaches his farthest foe within its life, and leaves from his model's left hand); that where his fight looks for the archive's parts (`CryptArchive.java`: the dais's middle, his throne, where he comes down, the lanes, the floor graves, where a dark seal's dead climbs out and where the Grey Mist opens) is where `tools/crypt_archive.py` builds them, and that he waits at the throne, comes down to open floor on the north lane, his dead rise on open floor over the ledger stones and at each sarcophagus's foot, the lanes he calls are the alcoves' and never the lectern's, the Grey Mist opens where the phylactery stood, and every cell of the hall's floor lies within his leash; that his chains turn every blow aside, a seal is answered only by its own archive's fight, and the lectern's reading reaches him; that a struck-back bolt staggers him while he is warded; that whoever hurts a Restless Dead takes part; his registrations, names and renderers; the GeckoLib models, every clip the Java plays, sheet sizes, glowmasks and controllers' bones; the loot table and his trophies against Arms VII's; recipes (none making Grave Dust), costume tags, advancement and messages; the Emptied Phylactery on the server's tick; and that nothing of his loads chunks or changes dimension. 42 deliberate changes to his Java, the tables and the data, one at a time, each failed it | Pass, 2202 IDs |
| `python3 tools/check_icon_maps.py` on the three new maps | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded with identical pixels were reverted) |
| `javac` on the new Java and tests, for syntax only | No errors |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); to be run by CI |

The game tests (`CryptLichGameTests`) show what a game-test server can, with a Lich put down on his own (no lair) over an archive laid out from near the corner of an arena the hall's size; each test lays the floors and fixtures it needs:
1. he waits on his throne, his crown dark, and takes no harm; a creative player's blow does not wake him; a player who may fight him standing on the hall's floor does, at full health, his crown lit, unhurt while he wakes; he comes down to the hall's floor where he wakes, his health one player's, all three seals whole, the first name of his order lit and the others dark, and the phylactery whole;
2. his health scales 1, 1.5 and 2.5 times for one, two and four or more players, and waking with two players near his hall he has 1.5 times his health; Wither and poison never take hold of him; a player's blow from afar wakes him, unhurt; the Last Name makes his cooldowns a third shorter, breaks his chains for good and flares his crown, and then a blow hurts him and counts as taking part, and a bystander does not;
3. his rule: while his chains burn a blow is turned aside; a player's hand on the lit name's seal breaks it, darkens its name and counts as taking part, and the next name lights; with a chain left he is still warded; with all three broken the phylactery cracks and he is exposed, staggered for 2 seconds, and a blow hurts him;
4. Raise the Dead raises two Restless Dead at the two floor graves nearest the player; a player's hand on a dark name's seal holds it, keeps the lit name, and raises one at that alcove's sarcophagus's foot; never more than four walk at once; a Restless Dead is unhurt while it climbs out, afterwards a blow on it counts as taking part, and when he climbs back to his throne his dead crumble;
5. exposed, he stays so for 12 seconds; then his chains reforge, turning blows aside meanwhile; reforged, every seal and the phylactery are whole and a name burns again;
6. at half health he shrieks the Erasure, unhurt; his chains reforge at once with the first name lit and the last one scraped from its stone; 2 seconds later the Unwritten begins; in its turn the scraped name is lit but blank, and its seal holds; reading the names at the lectern cuts it again and counts as taking part, and then its seal breaks and exposes him;
7. a scraped name left lit 30 seconds burns back by itself; at a fifth of his health the Last Name begins: his chains are gone, the phylactery cracked, his crown flaring, every name dark, and a blow hurts him;
8. his Grave Sweep strikes a player before him and one 45° aside, throwing the one before him back, and spares one beside him, one behind him and one out of reach;
9. his Bone Lanes call the two lanes nearest the player (in the Unwritten's way); the bones strike and toss up a player on each, and spare one between the lanes and one on a lane not called; in the fight, while his chains hold, he calls the one lane nearest, a clump of bones bursts every 2 blocks down its middle, and they sink and are gone;
10. his Soul Bolt drifts from his throne at a player and strikes and withers them; struck back by a player, a bolt turns and reaches him, and while he is warded it staggers him for 2 seconds, unhurt, and counts as taking part; exposed, a struck-back bolt hurts him;
11. his Soul Drain takes health from the player and heals him as much; a pillar between them hides the player from him, and breaks the Soul Drain begun;
12. his Grave Pull's ring has its dark part 30° either side of where it lies; it drags two players in the ring toward him and spares the one in its dark part and one out of reach; then six clumps of bones burst round his feet, striking only the player within 3.5 blocks;
13. strayed farther than 30 blocks from his dais, he is drawn back into his hall;
14. left alone 10 seconds, he climbs back to his throne, healed, his crown dark, every seal mended and every name dark, the phylactery whole, his dead crumbled, and who hurt him forgotten;
15. his loot is each participant's own: Grave Dust and, on a first kill, one of his trophies into the inventory of each who struck him and of one who only read the names back, with a third of his experience each, Nameless theirs afterwards, nothing for a bystander; he falls with his crown going dark, and with no lair instance the phylactery is left cracked;
16. the Emptied Phylactery in the offhand turns Wither's harm aside and takes the effect off on the server's tick, but not other harm; put away, Wither hurts again.

The client test (`CryptLichClientGameTests`) runs his whole fight in a real Archive of Last Names, with the one player in survival under Resistance: he waits on his throne; standing on the hall's floor wakes him and he comes down, his chains burning and a name lit; his chains at rest; the Warded King's four attacks, two dead risen from the floor graves, and a dark name's seal raising one from its sarcophagus; the three seals broken in their order, the phylactery cracked and he exposed; the Erasure, a name scraped; the Unwritten's Soul Drain and Grave Pull; the scraped name's seal holding until the player reads it back at the lectern, then breaking; the Last Name; and his fall: the player's Grave Dust, trophy and Nameless, his dead and bolts gone, Grey Mist on the dais; then a fresh instance in his slot with no Grey Mist, the phylactery whole, every seal whole and him on his throne again. Thirteen pictures along the way.

**New game APIs:** `BossEvent.setName` (his bar names the lit name; vanilla since 1.9) and `DamageTypes.WITHER` (the Emptied Phylactery; a sibling of the `HOT_FLOOR` and `FALL` types already used). The build compiles them against the pinned game.

## Rollout and open questions

- **Difficulty:** his numbers are the plan's. After a hand playtest the owner may want him exposed longer or shorter (12 s), a scraped name to burn back sooner (30 s), or his dead to rise more or less often.
- **Still to do:** the two-client dedicated-server playtest, and a play by hand.
