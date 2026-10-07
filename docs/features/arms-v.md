# Arms V: weapon arts

Status: implemented on `claude/arms-v` (batch 48), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "start on the next batch of weapons try to make their special things be special animations that do damage in different interesting ways".
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the arms of batches 42 to 47 ([arms.md](arms.md), [arms-ii.md](arms-ii.md), [arms-iii.md](arms-iii.md), [arms-iv.md](arms-iv.md)).
Primary specialty and supported player role: fighting.

## Player experience
Six more kinds, each in bronze and steel, each with a **weapon art**: a special move used with the use key (right click) with the arm in the main hand. Each art plays its own animation, seen by every player nearby, and deals its damage in a shape of its own. Then the arm needs a few seconds before its art is ready again, shown on the hotbar as an item cooldown; plain blows are not held back. Four of the kinds swing two-handed (Arms III's committed swings).

| Arm | Hit (bronze / steel) | Attacks a second | Reach | Plain swing |
|---|---|---|---|---|
| Twinblade | 7 / 7.5 | 1.2 | 3.25 | two-handed, 14 ticks, blow at 5; 140°, 3 foes, a combo of 3 |
| Nodachi | 9.5 / 10 | 0.8 | 4 | two-handed, 20 ticks, blow at 7; 100°, 3 foes |
| Earthbreaker | 12.5 / 13 | 0.55 | 3.25 | two-handed, 24 ticks, blow at 9; 80°, 2 foes; stops a shield for 5 s |
| Katar | 4.5 / 5 | 2.0 | 2.75 | a 5-tick jab |
| Moonblade | 9 / 9.5 | 0.9 | 3.75 | two-handed, 18 ticks, blow at 6; 130°, 4 foes |
| Kusarigama | 5 / 5.5 | 1.7 | 3.25 | a 6-tick hooking cut |

### The arts: six ways of dealing damage

| Arm | Art | Ready again | The shape of its damage | Its animation |
|---|---|---|---|---|
| Twinblade | **Cyclone** | 6 s | **A ring, three times over.** Three turns, 6 ticks apart; each strikes every foe within 3 blocks *all round* (up to 8) for half a blow, with no knockback, and draws it in at 0.15 blocks a tick, so the ring keeps it. | The whole body spins three times round with the blades out level, then settles. |
| Nodachi | **Iaido** | 8 s | **A line, with a delay.** A dash of 5 ticks at 1.2 blocks a tick (about 6 blocks). Every foe within 1.25 blocks of the path is marked (up to 6). Then, 4 ticks after the dash ends, the cut lands on all of them at once, for 1.3 blows, wherever they are (within 10 blocks). | A crouch with the blade drawn low behind; the dash leaning far over it; the great cut across as the marks land; a flick of the blade. |
| Earthbreaker | **Leap Slam** | 10 s | **A circle where you land, falling off from the centre.** A leap up at 0.8 and ahead at 0.5 blocks a tick. On landing, every foe within 3.5 blocks takes a full blow at the centre, falling to half at the edge, plus 15% for each block landed below the take-off (at most 6 blocks). Each is thrown up. | A crouch, a spring with the hammer swung up, tucked in the air with it cocked behind the head (held until you land), then the slam. |
| Katar | **Flurry** | 5 s | **Many small hits on one foe.** Five jabs, 3 ticks apart, at the nearest foe within reach and 50° of your view, each for 0.28 of a blow; then a driving finish of 0.9 with knockback. Every jab lands in full: the foe's damage cooldown is let go first, and the jab holds the foe where it is (vanilla's hit would knock it back out of reach after three). | The katar and the off fist punch turn about, then a lunge. |
| Moonblade | **Crescent** | 7 s | **A travelling front that passes through.** At tick 5 a wave leaves at waist height and runs along your level view at 1.2 blocks a tick for 10 ticks (12 blocks). It strikes each foe within 1.5 blocks of its line once (up to 6), for 0.9 of a blow, 15% less for each foe it has already passed. A block stops it. | Gathered low behind, swept up across to loose the wave, then the blade levelled after it. The wave is a crescent of light (end rod sparks) with a sweep at its middle. |
| Kusarigama | **Chain Lash** | 6 s | **A ray, a pull, a follow-up.** At tick 4 the chain flies along your view up to 9 blocks, short of any block. The first foe on it takes half a blow, is hauled towards you (0.2 blocks a tick for each block it is off, at most 1.6, less its knockback resistance, which lands it 2 to 3 blocks off) and dragged from the saddle. At tick 11, if it is within 3.5 blocks and in sight, the sickle reaps it for 0.8. | The chain whirled up at the side, flung out ahead, hauled back, the sickle raised and brought round. |

- **Every hit of an art:**
  - is the arm's attack damage times the share above;
  - is struck through vanilla's own thrust attack (`Player.stabAttack`) at a full charge, so enchantments, wear and the item's hit hooks apply;
  - lands in full even in quick succession: the foe's damage cooldown is let go before each.
- **While busy with an art** (its animation; for the leap, until it lands) you cannot swing or hit with the arm, and you are slowed:
  - cyclone 30%;
  - flurry, crescent and chain 50%;
  - the dash and the leap move you themselves.
- **The arts refuse when:**
  - the arm is two-handed and something that blocks is in the off hand;
  - you are gliding;
  - for the leap, you are not on the ground;
  - for the cyclone, iaido and the leap, you are riding.
- **The handbook** has "Arms: Weapon Arts", "Arms: Twinblade, Nodachi, Earthbreaker" and "Arms: Katar, Moonblade, Kusarigama", in the gear chapter.
- **Advancement:** **Weapon Art** (forge any of the six), after Bronze Age.
- **Tooltips:** each arm's art is named and described in a gold line, with its cooldown.

## How it works
- **The client sends nothing new.** The use key is vanilla's (`ArmItem.use`).
- **The server works the art (`weapons/WeaponArts`).** It keeps one map of arts in progress and steps each at the end of every server tick. The dash, the leap and the chain's pull set motion the client is told of (`ClientboundSetEntityMotionPacket`), as the grapple hook does. The crescent's wave is a point the server moves each tick, not an entity.
- **Every client that sees the wielder plays the animation, the wielder's own included** (`WeaponArtPayload`, server to client: the entity id and the phase).
- **The art clips live in `tools/arms_moves.py`:**
  - each has its own length in ticks, with a key at every tick the server lands a hit;
  - first-person keys show the art on screen;
  - the cyclone's clip also turns the whole body (a spin of 1080° through its keys, applied to the render state's body yaw);
  - the leap's first clip ends in the air and holds there until the second, the slam, begins as the server says it landed.

## Connections
- Existing input producer: bronze ingots (tin), steel ingots (the steel foundry), sticks, leather and iron nuggets.
- Existing output consumer: the player against mobs and, where PvP is on, players.
- Recipes (shaped, `#` the metal's ingot, `N` an iron nugget):

  | Arm | Pattern | Cost |
  |---|---|---|
  | Twinblade | ` ##/ L /## ` | 4 ingots, leather |
  | Nodachi | `  #/ ##/L  ` | 3 ingots, leather |
  | Earthbreaker | `###/#S#/ SS` | 5 ingots, 3 sticks |
  | Katar | `# #/#L#` | 4 ingots, leather |
  | Moonblade | `## /  #/L# ` | 4 ingots, leather |
  | Kusarigama | `## /  #/NN ` | 3 ingots, 2 iron nuggets |

  None repeats another shaped recipe in the mod (the recipe audit compares them all, mirrored too).
- Technology connection: the batch 25 metal tiers; electroplating plates and repairs them. Magic connection: none.
- Reachable entry path: as the other arms. Required vs optional: optional; craftable solo and tradeable.

## Balance and automation
- **Plain blows:** per foe and per second, every kind is below its metal's sword (bronze 9.6, steel 10.4), counting the finishing blow over the combo.
- **Against one foe, an art is no better than plain blows.** `check_arms` holds every kind to this:
  - over one cooldown, the art's damage to that foe plus plain blows for the rest of the cooldown stays below the sword, a second;
  - the leap is counted on level ground; leaping down onto foes from a height adds up to 0.9 of a blow, at the price of the fall damage below the take-off.

  | Arm | Plain, a second (bronze / steel) | The art, to one foe | Over a cooldown, a second |
  |---|---|---|---|
  | Twinblade | 9.1 / 9.75 | 10.5 / 11.25 | 9.03 / 9.68 |
  | Nodachi | 8.55 / 9.0 | 12.35 / 13.0 | 9.13 / 9.61 |
  | Earthbreaker | 7.73 / 8.04 | 12.5 / 13.0 | 8.67 / 9.02 |
  | Katar | 9.0 / 10.0 | 10.35 / 11.5 | 9.27 / 10.3 |
  | Moonblade | 9.11 / 9.62 | 8.1 / 8.55 | 9.23 / 9.74 |
  | Kusarigama | 8.5 / 9.35 | 6.5 / 7.15 | 8.45 / 9.3 |

- **What an art is worth is its shape:**
  - many foes at once (the cyclone, iaido, the slam, the crescent);
  - getting somewhere (the dash, the leap);
  - bringing a foe to you (the chain);
  - a burst before a foe can step away (the flurry).
- **Each art's damage and reach is bounded:** at most 6 to 8 foes, the ranges in the table, and one art at a time a player. The cooldowns are fixed.
- No conversion, no recycling.

## Multiplayer and persistence
- **Server authority:**
  - every hit, pull, throw and dash is worked on the server;
  - a client only asks to use the item (vanilla's packet) and plays animations when told;
  - the server checks the arm in the main hand, the cooldown, the off hand, the ground, the saddle and that no art or two-handed swing is in progress.
- **Who an art can strike:** the foes a two-handed blow may strike (`TwoHanded.target` and `allowed`). Never:
  - allies;
  - the wielder's mount;
  - marker armor stands;
  - foes other code protects (`AttackEntityCallback`, such as the town's townsfolk).

  The cyclone and the slam need line of sight from the wielder. The crescent and the chain are stopped by blocks. A pull or a throw follows only a hit that landed, so where PvP is off nothing happens to players.
- **No exploits:**
  - vanilla's hit is refused while busy with an art, as for two-handed arms;
  - an art cannot start during a two-handed swing, nor a swing during an art;
  - the dash and the leap use vanilla's impulse rule for falls: only a drop below where they began counts.
- **Bounded work:** each tick covers the arts in progress only (one a player at most), each looking at entities within a small box.
- **Saved state:** none beyond ordinary items with stable ids (`jugcraft:<bronze|steel>_<twinblade|nodachi|earthbreaker|katar|moonblade|kusarigama>`). Arts in progress are not saved: leaving ends them. Recipes follow the `tin` and `machines` switches.

## Dependencies and assets
- **No new dependencies or mixins.** One new packet, `jugcraft:weapon_art` (server to client).
- **The client:**
  - uses the existing `AttackStrengthAccessor`;
  - reads the render state's body yaw (`bodyRot`) for the cyclone's spin, in the existing render state hook.
- **Art:** twelve 64x64 sprites from `tools/arms_art.py`, in the ornate style of batch 47. There is a new `flat_blade` helper for flat-toned straight blades. The kusarigama is the kama with a chain and weight slung under it.
- **Motion:** `tools/arms_moves.py` (`art`, `fp_art`; an art clip's `ticks`, `hold` and `spin` in `tools/arms_motion.py`), written to `assets/jugcraft/arms_motion/<kind>.json` under `arts`.

## Verification
- **`python3 tools/check_mod_data.py`** (`check_arms`, `check_arms_motion`):
  - Java and the tools agree on the kinds, the arts table, the move list and every art constant;
  - each art's first clip lasts its ticks and has a key at every hit tick (to half a tick);
  - the leap holds in the air and its slam starts from there;
  - a spin ends a whole number of turns round;
  - plain blows and the art over a cooldown stay below the sword;
  - the client receives the packet.

  Each check was broken on purpose once, and each failed:
  - a cooldown changed in Java;
  - iaido's cut key moved;
  - a spin ending part way round;
  - the flurry's share raised to 0.45.
- **Game tests** (CI job `gametest`, `ArmsVGameTests`, in a 16-block arena):
  - `aCycloneStrikesAllRoundThreeTimes`: four husks about the wielder take 3 hits each and none before the first turn; one 5 blocks off is spared; busy, slowed and on cooldown meanwhile, free after;
  - `iaidoCutsEveryFoePassedAMomentAfterTheDash`: the dash pushes ahead at 1.2 blocks a tick; the husks passed are untouched until the delay is out, then cut together; one aside is spared;
  - `aLeapSlamsHardestAtItsCentreAndFromAHeight`: from a 3-block platform; the close husk takes more than the one at the edge (1.307 and 1.021 blows: the falloff and the drop's bonus), both are thrown up; one beyond the radius is spared;
  - `aFlurryLandsEveryJab`: five jabs and the finish on one husk, in full; none behind. The first CI run caught only 3 jabs landing: vanilla's hit knocked the husk out of reach, so the jabs now hold the foe in place;
  - `aCrescentRunsThroughFoesUntilAWall`: two husks in line (the second 15% less); one aside and one behind a wall are spared;
  - `aChainLashHaulsInTheFirstFoeAndReapsIt`: a husk 7 blocks off is struck, hauled in and reaped (the reap needs it within 3.5 blocks; the reap's own knockback then throws it back a little); a close one is struck and reaped; the one behind it is spared. (The first end-to-end run left a pig 3.76 blocks off at the reap, short of the sickle's 3.5; the pull was raised from 0.15 to 0.2 a block.);
  - `anArtKeepsItsRules`: a two-handed art refuses with a shield (a katar's does not) and the leap refuses in mid-air; a plain hit is refused while busy and allowed after, while the cooldown still runs.
- **Client game tests:**
  - `WeaponArtsClientGameTests`, end to end with the use key: each art on husks (the chain on a pig), with real movement:
    - the cyclone's three hits on four husks;
    - iaido carrying the player more than 3 blocks and cutting the two passed;
    - the leap rising over 1.5 blocks, landing, striking and costing no health;
    - the flurry;
    - the crescent's two hits and one spared;
    - the chain hauling the pig in by more than 2 blocks and reaping it.

    Screenshots are taken mid-art from the front, and two arts in first person.
  - The arms racks and frames show the new kinds.
  - `ArmsMotionClientGameTests` covers their guard and swing.
- **Not run:**
  - play;
  - how each art feels against real mobs that move and fight back;
  - the arts on slopes, stairs, in water and in tight caves;
  - two players watching each other's arts.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- **All numbers are first values for the owner to tune:**
  - shares, cooldowns and ranges;
  - the dash's speed;
  - the leap's height.
- **Possible next steps:**
  - arts for earlier kinds;
  - an art that the attack key charges, rather than the use key;
  - sounds of our own.

  Each needs the owner's choice.
