# Arms II

Status: implemented on `claude/arms-ii` (batch 45), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "start on the next batch of weapons", after the arms of batch 42 ([arms.md](arms.md)) and their motion (batch 43, [arms-motion.md](arms-motion.md)).
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the arms of batch 42 and the bronze and steel swords of batch 25.
Primary specialty and supported player role: fighting; the scythe also serves farmers.

## Player experience
Eight more kinds of arms, each in bronze and steel, drawn in the same two styles as batch 42 (steampunk bronze with brass and leather; kaiserpunk blued steel and gunmetal). Batch 42's kinds differ mostly by their numbers and 26.3's own weapon components. Each new kind except the quarterstaff also has a **trait** that changes how it fights.

| Arm | Hit (bronze / steel) | Attacks a second | Reach (blocks) | Swing | Trait |
|---|---|---|---|---|---|
| Dagger | 4 / 4.5 | 2.3 | 2.5 | stab, 4 ticks | **Backstab:** a blow from behind (within 70° of straight behind the foe's body) deals half again as much. |
| Sabre | 5 / 5.5 | 1.8 | 3 | whack, 6 ticks | **Saddle:** 3 more damage while riding. Sweeps. |
| Estoc | 6 / 6.5 | 1.4 | 3.5 | stab, 7 ticks | **Armor pierce:** 0.3 more for each point of the foe's armor, at most 6. |
| Battle Axe | 11 / 11.5 | 0.7 | 3.25 | whack, 22 ticks (12 before batch 46) | **Chop:** mines wood as its metal's axe does. Stops a shield blocking for 5 s; wears 2 a hit. |
| Flail | 8 / 8.5 | 1.0 | 3.25 | whack, 10 ticks | **Daze:** a hit slows the foe (Slowness II) for 2 s. |
| Scythe | 8 / 8.5 | 1.0 | 4 | whack, 18 ticks (10 before batch 46) | **Reap:** use on a ripe crop to harvest every ripe crop in the 3 by 3 around it and replant them. Sweeps. |
| Quarterstaff | 5 / 5.5 | 1.6 | 3.5 | whack, 12 ticks (7 before batch 46) | Knocks back (1, as the war hammer); hold use to parry 50% of a blow from in front. |
| Pike | 8 / 8.5 | 0.8 | 2–5 | stab, 16 ticks (10 before batch 46) | **Riders:** half again as much damage against anything riding or ridden (a horseman, or his horse). The longest reach, but nothing nearer than 2 blocks. |

For comparison: the bronze sword hits 6 at 1.6 a second, the steel sword 6.5; the iron axe hits 9 at 0.9.

- **Motion.** Each kind has its own guard, combo and first-person strokes, made as batch 43's ([arms-motion.md](arms-motion.md)):
  - dagger: a low guard, then a quick stab and a slash;
  - sabre: a light cavalry blade, cutting forehand and backhand;
  - estoc: point forward, then a deep thrust and a high thrust;
  - battle axe: two hands, chopping from high and from the side;
  - flail: the ball swung round overhead and brought down, then a side swing;
  - scythe: two hands on the snath, a long low reaping sweep and the return;
  - quarterstaff: two hands wide apart, a strike, a jab and a sweep;
  - pike: two hands, levelled, a thrust and a high thrust.
- **Enchanting.** The sabre and scythe are swords' kinds (`#minecraft:swords`: Sharpness, Sweeping Edge, Looting and the rest). The others take the melee weapon enchantments, Unbreaking and Mending.
- **Tooltips** say each kind's trait. Handbook: two pages in the gear chapter, "Arms: Blades of Skill" (dagger, sabre, estoc, quarterstaff) and "Arms: Heavy and Long" (battle axe, flail, scythe, pike). Advancement: **Armory** (forge any of the eight), after Bronze Age.

## Connections
- Existing input producer: bronze ingots (tin, batch 4) and steel ingots (the steel foundry); sticks and leather.
- Existing output consumer: the player, against mobs and (where PvP is on) players; the scythe also harvests the farming branch's crops (any `CropBlock`).
- Technology connection: the metal tiers of batch 25; electroplating (batch 34) plates and repairs them like any weapon. Magic connection: none.
- Reachable entry path: the same as batch 42. Bronze arms need only tin and copper (early); steel arms need the steel foundry. No arm unlocks anything, so there is no cycle.
- Required vs optional: optional. Every kind can be crafted solo, and arms trade like any item.
- The specialty stays useful alone: each trait answers a situation (sneaking up, riding, armored foes, woodcutting, fleeing foes, harvests, crowds, cavalry). None is needed to progress.
- Recipes (shaped, `#` the metal's ingot, `S` stick, `L` leather):

  | Arm | Pattern | Cost |
  |---|---|---|
  | Dagger | ` #/L ` | 1 ingot, leather |
  | Sabre | ` #/ #/L ` | 2 ingots, leather |
  | Estoc | `  #/ # /#L ` | 3 ingots, leather |
  | Battle axe | `###/#S / S ` | 4 ingots, 2 sticks |
  | Flail | `  #/ # /S  ` | 2 ingots, stick |
  | Scythe | `###/  S/ S ` | 3 ingots, 2 sticks |
  | Quarterstaff | `  #/ S /#  ` | 2 ingots, stick |
  | Pike | `  #/ S /SS ` | 1 ingot, 3 sticks |

  None repeats another Jugcraft recipe (every shaped recipe in the mod was compared, mirrored too, by a one-off script that is not part of the checks) or a vanilla tool's shape.
- Repair, durability and enchantability are the metal's, as batch 42's arms (bronze 320 and 14, steel 900 and 12).

## Balance and automation
- Damage is in vanilla's units (half-hearts); speed is attacks a second. Without its trait, every kind deals less damage a second than its metal's sword (bronze 9.6, steel 10.4):
  - dagger 9.2 / 10.35;
  - sabre 9 / 9.9;
  - quarterstaff 8 / 8.8;
  - estoc 8.4 / 9.1;
  - flail and scythe 8 / 8.5;
  - battle axe 7.7 / 8.05;
  - pike 6.4 / 6.8.
  
  `check_arms` now fails if any arm's damage a second reaches its metal's sword.
- The traits pay only in their situation:
  - backstab only from behind;
  - the saddle only while riding;
  - armor pierce only against armor, at most 6 (a foe with 20 armor);
  - riders only against a rider or a mount.
  
  The dagger's short reach and the pike's 2-block minimum are the price of their speed and length.
- The flail's daze is Slowness II for 2 s, renewed by each hit. It cannot stack longer, and a flail hits once a second.
- The battle axe chops as its metal's axe, with no axe abilities: no stripping, scraping or wax removal. It is slower to swing than an axe and wears 2 a hit, so it is a weapon that also cuts wood, not a better axe.
- Reaping:
  - The drops are the crop's own loot table (as breaking it by hand) less one seed, which replants it. It makes no more than hand harvesting; it saves clicks and replanting.
  - Each crop reaped wears the scythe by 1, so a full 3 by 3 costs 9.
  - Only ripe crops are taken; young ones are left.
- No conversion, no recycling. The metal audit counts each arm's ingots.

## Multiplayer and persistence
- The numbers, reach, sweep, parry and shield breaking are the game's own item components, decided by the server as for vanilla weapons.
- The traits run on the server:
  - the bonus to a blow in `Item.getAttackDamageBonus`, which `Player.attack` calls on the server's copy of the player;
  - the daze in `Item.hurtEnemy`, only on the server;
  - the reaping in `Item.useOn`, which changes blocks only on the server.
  
  The client can claim nothing: whether the attacker is behind, riding, or what the foe's armor is, the server reads from its own entities.
- The scythe reaps only crops the player may change: `mayUseItemAt` (adventure mode, world border), `mayInteract` (spawn protection) and the town's protection (`TownProtection.denies`). The 3 by 3 is bounded, and the work happens once per use, not per tick.
- Saved as ordinary items with stable ids (`jugcraft:<bronze|steel>_<dagger|sabre|estoc|battle_axe|flail|scythe|quarterstaff|pike>`). Recipes follow the `tin` (bronze) and `machines` (steel) feature switches; the items stay registered when a switch is off.

## Dependencies and assets
- No new dependencies, mixins, entities or packets. The motion uses batch 43's player and mixins.
- Art: one 64x64 sprite per arm, drawn by `tools/arms_art.py` with the high-detail renderer, laid along the diagonal as vanilla's swords. New shapes there:
  - a curved blade (sabre, scythe);
  - a bearded axe head;
  - chain links and a spiked ball (flail).
- Models: shared in-hand models per kind (`models/item/arms_<kind>.json`), as batch 42.
- Motion: `tools/arms_moves.py` (guards, combos, first-person strokes) writes `assets/jugcraft/arms_motion/<kind>.json`.
- All original; nothing from the mods studied for batches 42 and 43 is used.

## Verification
- `python3 tools/check_mod_data.py` (`check_arms`):
  - keeps `weapons/JugcraftArms.java` and `tools/arms.py` the same: kinds, every number, the trait of each kind and the trait constants;
  - fails if an arm's damage a second reaches its metal's sword;
  - checks the models, textures (64x64), recipes and tags;
  - `check_arms_motion` checks the eight new motion files and that `ArmsMotion.java` reads them.
- Game tests (CI job `gametest`, `ArmsIIGameTests`):
  - `aDaggerStabsHarderFromBehind`: a blow of 4 gains 2 from behind and nothing from in front;
  - `aSabreCutsHarderFromTheSaddle`: 3 more from a husk riding a horse, nothing on foot;
  - `anEstocPiercesArmor`: armor 0, 10 and 30 give 0, 3 and 6 (the cap);
  - `aPikeHitsRidersAndMountsHarder`: half again against the rider and the horse, nothing against a foe on foot;
  - `aFlailDazes`: a hit leaves Slowness II for 40 ticks;
  - `aBattleAxeChopsWood`: it mines a log faster than by hand, and stone no faster; the war hammer mines a log no faster;
  - `aScytheReapsAndReplants`: a mock survival player uses a steel scythe on the farmland in the middle of a 3 by 3 of wheat (8 ripe, 1 young). The 8 ripe crops are reaped and replanted, the young crop and a ripe one 2 blocks outside are left, 8 wheat drop, and the scythe wears by 8.
  - Batch 42's `armsCarryTheirKindsComponents` covers the new kinds' components and tags.
- Client screenshots (CI job `client`):
  - `ArmsClientGameTests`: `jugcraft_arms_bronze_rack_ii` and `jugcraft_arms_steel_rack_ii` (armor stands), and the new kinds in `jugcraft_arms_frames`;
  - `ArmsMotionClientGameTests`: each new kind's guard and swing from the front, and first person for the dagger, scythe and pike.
- **Not run:**
  - play: how the traits feel and whether the numbers are right;
  - a mounted sabre or a pike against a real cavalry charge, beyond the test's husk on a horse;
  - reaping by a real client, other crops than wheat (beetroot, carrots, potatoes, Jugcraft's), and reaping inside a town;
  - two players or PvP.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions. Mobs do not spawn with arms. The scythe works on any crop all year; seasons change nothing.

## Rollout and open questions
- Since Arms III (batch 46, [arms-iii.md](arms-iii.md)) the battle axe, scythe, quarterstaff and pike swing two-handed: a longer swing whose blow lands as it comes round, on every foe in an arc.
- All numbers are first values for the owner to tune.
- The backstab reads the foe's body turn, which for most mobs follows where they walk; a mob standing still and looking round may be backstabbed from where its head faces.
- The pike's riders bonus does not apply to the spear's or lance's charge (those are vanilla's kinetic weapons and are untouched).
- Reaping uses each crop's own loot table and vanilla's `CropBlock` age, so a crop that is not a `CropBlock` (sweet berries, nether wart, pitcher plants, cocoa) is not reaped.
