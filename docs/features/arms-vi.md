# Arms VI: katanas, brazier maces, longbows, arbalests and shields

Status: implemented on `claude/arms-vi` (batch 55), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "start on the next batch of weapons", with three reference sheets (a twin-katana set with red slash effects, an iron-and-wood war kit of a mace, bows, a crossbow and shields, and a gold fantasy set), studied for their look only; nothing of them is copied.
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the arms of batches 42 to 48 ([arms.md](arms.md) to [arms-v.md](arms-v.md)).
Primary specialty and supported player role: fighting.

## Player experience
Two more arms and a war kit, each in bronze and steel.

| Item | Hit (bronze / steel) | Attacks a second | Reach | What sets it apart |
|---|---|---|---|---|
| Katana | 6 / 6.5 | 1.5 | 3.25 | A weapon art, **Seven Cuts** (below) |
| Brazier Mace | 8 / 8.5 | 1.0 | 3 | A hit sets the foe alight for 4 s; use it on a campfire, a candle or the ground to light it, as flint and steel does |

### Seven Cuts: many arcs in a breath
The katana's art, used with the use key, as Arms V's arts are ([arms-v.md](arms-v.md)): ready again after 6 s, and the wielder is busy for 18 ticks, slowed by 40%.
- **The shape of its damage: a fan ahead, seven times over.** Seven cuts, 2 ticks apart from tick 3. Each strikes every foe within the katana's reach and 110° of the view (up to 4) for 0.22 of a blow (1.54 blows in all). Every cut lands in full: the foe's damage cooldown is let go first, and each cut but the last holds the foe in place, so none is knocked out of reach.
- **What it looks like:** each cut leaves an arc of colour in the air where the blade went, crimson from bronze and pale gold from steel, falling left and right in turn, the last level and wider. The animation is a drawn cut, a rain of quick cuts turn and turn about, and a wide finishing cut.

### Longbows and arbalests
| Item | Draw or load | Arrow speed | Arrow base damage | Durability (bronze / steel) |
|---|---|---|---|---|
| Bow (vanilla) | 1 s | 3.0 | 2.0 | 384 |
| Longbow, bronze / steel | 1.3 s | 3.4 / 3.7 | 2.0 | 480 / 1350 |
| Crossbow (vanilla) | 1.25 s | 3.15 | 2.0 | 465 |
| Arbalest, bronze / steel | 1.25 s (vanilla's) | 3.4 / 3.55 | 2.1 | 480 / 1350 |

- **Shot for shot they beat a bow and crossbow:** an arrow's damage is its speed times its base damage, so a longbow's fully drawn arrow (6.8 / 7.4) and an arbalest's bolt (7.1 / 7.5) hit harder than a bow's (6) or a crossbow's (6.3), and fly flatter.
- **A second for a second they do not:** the longbow draws for longer, and neither passes vanilla's bow's 6 a second (the data check holds them to it).
- Everything else is vanilla's: arrows are drawn from the inventory and can be picked up; the bow's and the crossbow's enchantments apply (Power, Punch, Flame and Infinity; Quick Charge, Multishot and Piercing); the arbalest shoots fireworks as a crossbow does.
- **Tooltips:** what each is, and a grey line of how it compares (for example "Full draw in 1.3 s. Arrows fly 23% faster than a bow's.").

### Shields
| Shield | Raise | Covers (either side of ahead) | An axe stops it for | Wear per blocked blow | While held | Durability |
|---|---|---|---|---|---|---|
| Shield (vanilla) | 0.25 s | 90° | 5 s | full | none | 336 |
| Heater, bronze / steel | 0.15 / 0.1 s | 90° | 5 / 4 s | full | none | 400 / 900 |
| Tower, bronze / steel | 0.4 / 0.35 s | 130° | 3 / 2.5 s | three quarters | knockback resistance +0.4 / +0.5; speed -8% | 600 / 1350 |

- Hold use to block, as with a shield, in either hand. The tower shield's wider cover stops blows from well round to the side that a shield would not.
- **3D models,** built where vanilla's shield is and held with vanilla's shield poses:
  - the heater is square-shouldered and stepped to a point;
  - the tower is a tall board;
  - both have a raised metal rim, a domed boss, and boards with a leather strap and handle behind.
- **Faces:**
  - bronze heater: crimson, with a gold chevron and a gold jug;
  - steel heater: navy, with a riveted steel bend and a lit green gauge-light roundel;
  - bronze tower: oak boards bound with three riveted bronze bands;
  - steel tower: olive-painted riveted plates, with a vision slit and a stencilled number, the paint worn to the steel.

### Also
- **The handbook** has "Arms: Katana and Brazier Mace", "Arms: Longbow and Arbalest" and "Arms: Shields", in the gear chapter.
- **Advancement:** **War Kit** (forge any of the twelve), after Bronze Age.
- **Art:**
  - all sprites 64x64;
  - the brazier mace's flame flickers (four frames);
  - the longbow is drawn in three steps and the arbalest wound in three, as vanilla's bow and crossbow are, and shown loaded with an arrow or a firework;
  - the longbow is held 1.3 times a bow's size, the arbalest 1.15 times a crossbow's.

## How it works
- **The katana and the brazier mace are ArmItems:**
  - their numbers are item components, as the other arms';
  - the seven cuts are a `Move` worked by `weapons/WeaponArts` (`cut`, through `TwoHanded.foes` and the shared `hit`), with dust particles for the arcs;
  - the brazier mace's trait, `IGNITE`, is worked in `ArmItem.hurtEnemy` and `ArmItem.light`. Lighting mirrors flint and steel: it lights an unlit campfire, candle or candle cake, else sets fire on the face used where fire can go. It respects spawn protection, the town's protection and the player's build rights.
- **The longbow (`weapons/ArmBowItem`)** is vanilla's `BowItem`:
  - its `releaseUsing` draws on vanilla's curve stretched over its own draw (`power`) and looses at its own speed;
  - `createProjectile` sets the arrow's base damage.
- **The arbalest (`weapons/ArmCrossbowItem`)** is vanilla's `CrossbowItem`:
  - `shootProjectile` speeds an arrow by its speed over vanilla's 3.15 (fireworks fly as vanilla's);
  - `createProjectile` sets the base damage;
  - loading is vanilla's, so the crossbow's own item model properties (`crossbow/pull`, `charge_type`) work.
- **The shields (`weapons/ArmShieldItem`) block through the blocks-attacks component,** with their own numbers, and, for the tower, attribute modifiers on either hand (`jugcraft:shield_brace`, `jugcraft:shield_weight`).
  - They are vanilla `ShieldItem`s, so that raised on screen they are held as vanilla holds its shield. Vanilla turns any other blocking item as a parrying sword, and the client tests showed that swinging a raised shield out of sight in first person.
- **The kit is registered in `JugcraftArms.KIT`,** beside `ITEMS`, and both fill the combat tab.
- **Generated by:**
  - `tools/arms.py` (`RANGED`, `SHIELDS`, `kit()`) for the tables;
  - `tools/arms_kit.py` for the models and item definitions;
  - `tools/arms_kit_art.py` for the sprites.
- **Vanilla's assets were read in game, not assumed:** the bow and crossbow models and sprites, the item definitions and the shield's display transforms were logged by a throwaway client test and decoded. The models match vanilla's poses, and the sprites face as vanilla's do (bow arrow and crossbow prod to the top left). The shield's placement follows from vanilla's shield poses, which centre it there in a frame, the GUI, on the ground and on a shelf.

## Connections
- Existing input producer: bronze ingots (tin), steel ingots (the steel foundry), sticks, leather, iron nuggets, coal or charcoal, string, tripwire hooks and planks.
- Existing output consumer: the player against mobs and, where PvP is on, players; arrows and fireworks from vanilla.
- Recipes (shaped, `#` the metal's ingot):

  | Item | Pattern | Cost |
  |---|---|---|
  | Katana | `  #/ #N/L  ` | 2 ingots, iron nugget, leather |
  | Brazier Mace | `#C#/ # / S ` | 3 ingots, coal (or charcoal), stick |
  | Longbow | `#ST/S T/#ST` | 2 ingots, 3 sticks, 3 string |
  | Arbalest | `###/T$T/ S ` | 3 ingots, 2 string, tripwire hook, stick |
  | Heater Shield | `#W#/WWW/ W ` | 2 ingots, 5 planks |
  | Tower Shield | `#W#/#W#/#W#` | 6 ingots, 3 planks |

  None repeats another shaped recipe in the mod (the recipe audit compares them all, mirrored too).
- Technology connection: the batch 25 metal tiers; electroplating plates and repairs them. Magic connection: none.
- Reachable entry path: as the other arms. Required vs optional: optional; craftable solo and tradeable.

## Balance and automation
- **Melee:** per foe and per second, both kinds are below their metal's sword (bronze 9.6, steel 10.4):

  | Arm | Plain, a second | The art, to one foe | Over a cooldown, a second |
  |---|---|---|---|
  | Katana | 9.0 / 9.75 | 9.24 / 10.01 | 9.19 / 9.96 |
  | Brazier Mace | 8.0 / 8.5 | (none) | (none) |

  A brazier mace's fire adds about 1 a second while it burns (4 s, not stacking), as Fire Aspect does; that still leaves it below the sword.
- **Ranged:** a second, speed times base damage over the draw (or vanilla's 1.25 s load) stays below vanilla's bow's 6:
  - longbow 5.23 / 5.69;
  - arbalest 5.71 / 5.96.

  The worth is a harder single hit, fewer arrows spent and a flatter shot.
- **Shields:** the heater trades nothing for its quick raise but metal and a lower durability than its tier would suggest. The tower's wider cover costs a slower raise, its weight, and 6 ingots.
- `check_arms` holds Java and the tools together and checks:
  - every number;
  - the balance lines above;
  - that a shield covering more than vanilla's is slower to raise.
- No conversion, no recycling.

## Multiplayer and persistence
- **Server authority:** every cut, ignition, shot and block is worked on the server. Clients only use items (vanilla's packets) and play animations when told. The seven cuts keep Arms V's rules: a busy wielder cannot swing, and the art refuses gliding and conflicting swings. They may be used from the saddle, as the flurry may.
- **Who the cuts can strike:** the foes a two-handed blow may strike (`TwoHanded.foes`). Never allies, the wielder's mount, marker armor stands, or foes other code protects.
- **Fire:** lighting checks `mayUseItemAt`, `mayInteract` and `TownProtection`; vanilla's fire rules (and the `doFireTick` rule) govern what it spreads to.
- **Bounded work:** no per-tick code of its own beyond Arms V's art ticker, which is one art a player.
- **Saved state:** none beyond ordinary items with stable ids:
  - `jugcraft:<bronze|steel>_<katana|brazier_mace|longbow|arbalest|heater_shield|tower_shield>`;
  - loaded arbalests keep vanilla's charged-projectiles component.

  Recipes follow the `tin` and `machines` switches.

## Dependencies and assets
- No new dependencies, mixins or packets.
- **Art:**
  - `tools/arms_art.py`: the katana, and the brazier mace's four flicker frames in one 64x256 animated strip. `check_mod_data` now accepts strips of 64x64 frames, as it accepts 64x64 stills.
  - `tools/arms_kit_art.py`: the bows' and crossbows' sprites, and the shields' faces, backs and trim, each 64x64.
- **Motion:** `tools/arms_moves.py` (the katana's guard, cuts and art; the brazier mace's swings).

## Verification
- **`python3 tools/check_mod_data.py`:** PASS (local).
- **`python3 scripts/check_repository.py`:** PASS (local).
- **CI on 95b4dd08:** all green (`mod` with the server game tests, three client shards).
  - Its first-person shots show both shields raised across the view as vanilla's shield is. Before the `ShieldItem` change, a raised shield vanished.
- **Game tests** (`ArmsVIGameTests`):
  - `sevenCutsStrikeEveryFoeAheadSevenTimes`;
  - `aBrazierMaceSetsFoesAlightAndLightsBlocks`;
  - `aLongbowDrawsLongerAndLoosesFaster`;
  - `anArbalestShootsFasterAndHarder`;
  - `shieldsBlockByTheirNumbers`.
- **Client game test** (`ArmsVIClientGameTests`, with the real use key):
  - the kit on a rack and in frames;
  - a longbow drawn and loosed;
  - an arbalest wound, loaded and fired;
  - both shields blocking;
  - in first person, each shield beside vanilla's, at rest and raised;
  - the seven cuts on husks, read back from the server;
  - the brazier mace lighting fire on the ground.
- **Not run:**
  - play;
  - how the cuts, the shots and the shields feel against real mobs;
  - two players.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- **All numbers are first values for the owner to tune:**
  - the cuts' share and arc;
  - the bows' speeds and damage;
  - the shields' raise times and cover.
- **Possible next steps,** each needing the owner's choice:
  - banners on the new shields;
  - arrows of our own;
  - a slower windlass crossbow (vanilla's load time is fixed by its enchantment rules).
