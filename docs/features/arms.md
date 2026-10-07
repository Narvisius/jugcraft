# Arms

Status: merged in #152 (batch 42). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, with the Epic Knights and Simply Swords jars attached: "look at how these mods do weapons and animations and quality and depict the weapons and learn from it and make a bunch of new types of weapons / swords / maces / glaives / spears and give them nice animations and see how these mods keep them optimized", then "go ahead and start on the weapons in a new batch".
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the bronze and steel swords of batch 25.
Primary specialty and supported player role: fighting.

## Player experience
Nine kinds of arms, each in bronze (steampunk: brass fittings, leather wraps, warm bronze) and steel (kaiserpunk: blued steel, gunmetal, black rubber grips, a brass rivet). Each has its own way of fighting. None out-damages the plain sword per second; each trades some of that for a trait.

| Arm | Hit (bronze / steel) | Attacks a second | Reach (blocks) | Swing | Trait |
|---|---|---|---|---|---|
| Longsword | 7 / 7.5 | 1.3 | 3.25 | whack, 8 ticks | **Parry:** hold use to block 60% of a blow from in front (60° either side). Sweeps. |
| Greatsword | 10 / 10.5 | 0.8 | 3.75 | whack, 20 ticks (11 before batch 46) | Two-handed and heavy: extra knockback; a hit stops a shield blocking for 2 s. Sweeps. |
| Rapier | 4.5 / 5 | 2.0 | 3.5 | stab, 5 ticks | Quick thrusts; parries 35%. Sweeps. |
| Flanged Mace | 9 / 9.5 | 0.9 | 3 | whack, 9 ticks | A hit stops a shield blocking for 3 s. |
| War Hammer | 11 / 11.5 | 0.7 | 3 | whack, 22 ticks (12 before batch 46) | The heaviest blow, a knockback of 1, stops a shield blocking for 5 s (as an axe); wears 2 a hit. |
| Glaive | 9 / 9.5 | 0.9 | 4.25 | whack, 18 ticks (10 before batch 46) | A blade on a pole that sweeps at a long reach. |
| Halberd | 10 / 10.5 | 0.8 | 1–4.5 | stab, 12 ticks | Thrusts through every target in line, like the spear's jab; a hit stops a shield blocking for 3 s. |
| Spear | 3 / 3.5 jab | 1.05 / 1.0 | 2–4.5 | stab | Vanilla's spear in Jugcraft metals: hold use to charge, harder at a run or on a horse. |
| Lance | 4 / 4.5 jab | 0.8 / 0.77 | 2.5–5.5 | stab | A horseman's charge: 1.3 / 1.4 times the spear's charge damage, unhorses riders at lower speeds. |

For comparison: the bronze sword hits 6 at 1.6 a second, the steel sword 6.5, the iron axe 9 at 0.9.

- **Animations.** Each kind has its own swing, from 26.3's swing animations: whacks of different lengths (a war hammer's swing takes twice a sword's), stabs for the rapier and halberd. Holding use brings a parrying sword up in the blocking pose and a spear or lance down into the charging pose. Long arms are held larger, with the hand kept on the grip, and come up into the hand faster when switched to, as vanilla's spear does, so they never hang half-raised.
- **Enchanting.** Swords' kinds (longsword, greatsword, rapier, glaive) are in `#minecraft:swords`: Sharpness, Sweeping Edge, Looting and the rest. Spears and lances are in `#minecraft:spears` (Lunge). Maces, hammers and halberds take the melee weapon enchantments and Unbreaking and Mending.
- **Tooltips** say each kind's trait. Handbook: three pages (swords; maces and hammers; polearms) in the gear chapter. Advancement: **Man-at-Arms** (forge any arm), after Bronze Age.

## Connections
- Existing input producer: bronze ingots (tin, batch 4) and steel ingots (the steel foundry); sticks and leather.
- Output consumer: the player, against mobs and (where PvP is on) players.
- Recipes (shaped, `#` the metal's ingot, `S` stick, `L` leather): longsword ` # / # /#L#`, greatsword ` # /###/#L#`, rapier `  #/ # /L  `, flanged mace ` ##/ ##/S  `, war hammer `###/#S#/ S `, glaive ` ##/ S#/S  `, halberd `###/ S#/S  `, spear `  #/ S /S  `, lance `  #/#S#/S  `. None repeats another arm's or a tool's.
- Repair: as the metal's tools (`#jugcraft:repairs_bronze_gear`, `#jugcraft:repairs_steel_gear`). Durability and enchantability: the metal's (bronze 320 and 14, steel 900 and 12).
- Technology connection: the metal tiers of batch 25; electroplating (batch 34) plates and repairs them like any weapon. Magic connection: none. Required vs optional: optional.

## Balance and automation
- Damage is in vanilla's units (half-hearts); speed is attacks a second. Every arm's damage a second is below its metal's sword (bronze 9.6, steel 10.4): the longsword 9.1 / 9.75, rapier 9 / 10, greatsword 8 / 8.4, maces 7.7–8.6, polearms 8–8.6. The traits (reach, parry, shield-breaking, piercing, the charge) are the reason to choose one.
- Reach beyond 3 blocks costs speed: the glaive and halberd are slow, and the halberd and lance cannot hit closer than 1 and 2.5 blocks.
- A parry stops part of a blow only from in front, takes 0.1 s to raise, and wears the arm (1 plus half the blow, for blows of 3 or more). It blocks nothing a shield cannot (`#minecraft:bypasses_shield`), and an axe or war hammer stops it as it stops a shield.
- No conversion, no recycling: arms are made, not melted down. The metal audit counts each arm's ingots.

## Multiplayer and persistence
- Everything is the game's own item components (attack range, swing animation, weapon, blocking, piercing and kinetic weapon), so the server decides every hit, reach, block and charge as it does for vanilla's weapons; nothing is taken from the client. There is no Jugcraft code that runs per tick or per attack.
- Saved as ordinary items with stable ids (`jugcraft:<metal>_<kind>`). Recipes follow the `tin` (bronze) and `machines` (steel) feature switches; the items stay registered when a switch is off.

## Dependencies and assets
- No new dependencies, mixins or entities.
- Art (since restyled: pixel-art icons and 3D models in the hand, [arms-restyle.md](arms-restyle.md)): one 64x64 sprite per arm, drawn by `tools/arms_art.py` with the high-detail renderer (`tools/hd_art.py`), laid along the diagonal as vanilla's swords so it serves the inventory and the hand. A spear or lance also has its sprite mirrored, point to the top left, for the hand, as vanilla's spears do.
- Models: each arm's model is two lines over a shared in-hand model per kind (`models/item/arms_<kind>.json`), written by `tools/arms.py`: vanilla's sword poses (the lance: vanilla's spear poses) made larger by the kind's size and moved so the hand stays on the grip. The spear uses vanilla's `item/spear_in_hand` itself.
- Learned from, not copied: Epic Knights (all rights reserved) and Simply Swords (Timefall Development License) were read for their approach only:
  - a table of weapon kinds and their traits;
  - large sprites on a handful of shared parent models with display transforms, the materials differing only by texture;
  - a blocking pose for parrying;
  - no per-tick work outside of use.
  
  Their animations come from optional mods (Epic Fight; Better Combat and PlayerAnimator). Jugcraft gets its from 26.3's own swing and use animations, so it needs neither. None of their code, models, numbers or textures is used.

## Verification
- `tools/check_mod_data.py`: `check_arms` keeps `weapons/JugcraftArms.java` and `tools/arms.py` the same (kinds and every number, the charges, the lance and parry constants) and the in-hand models equal to their poses; the textures are 64x64, the recipes audit, the tags are known.
- Game tests (CI):
  - `armsCarryTheirKindsComponents`: every arm has its reach, swing, shield-breaking, parry, piercing, charge, tags and damage, logged as a table (`[arms]`);
  - `aLongswordParriesPartOfABlowFromTheFront`: a parrying zombie takes 4 of a blow of 10 from in front, all 10 from behind, and all of a fall of 10;
  - `aWarHammerBreaksAShieldsGuard`: the war hammer stops a shield for 5 s and the flanged mace for 3; a bronze sword does not.
- Client screenshots (`ArmsClientGameTests`): `jugcraft_arms_bronze_rack`, `jugcraft_arms_steel_rack` (armor stands holding each arm), `jugcraft_arms_frames` (item frames), `jugcraft_arms_held_steel_greatsword`, `_steel_halberd`, `_steel_lance` (first person), `jugcraft_arms_greatsword_front`, `jugcraft_arms_parry_front` and `jugcraft_arms_parry` (holding use with a longsword), `jugcraft_arms_lance_charge`.
- Not run: play (how the swings, reach and parry feel), mounted lance charges, a player blocking with a shield against a war hammer (vanilla breaks only players' shields; the test checks the number vanilla reads), two players, PvP.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions. Mobs do not spawn with arms.

## Rollout and open questions
- All numbers are first values for the owner to tune.
- Since Arms III (batch 46, [arms-iii.md](arms-iii.md)) the greatsword, war hammer and glaive swing two-handed: the click starts a longer swing, the blow lands as it comes round on every foe in an arc, and the combo ends in a finishing blow.
- Arms II (batch 45, [arms-ii.md](arms-ii.md)) adds daggers, sabres, estocs, battle axes, flails, scythes, quarterstaves and pikes, with traits worked by `ArmItem`.
- More metals (iron, diamond, netherite and later Jugcraft alloys) would be more rows in `tools/arms.py` and art styles in `tools/arms_art.py`.
- Custom attack animations beyond 26.3's whack and stab (an overhead hammer blow, a two-handed greatsword swing) would need client rendering code; left for a later batch if wanted.
