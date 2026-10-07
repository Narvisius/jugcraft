# Arms VIII: thrown arms (batch 59)

Status: implemented on `claude/arms-viii`, awaiting review. Green in CI (build, server and client tests); **not yet played**.
Proposal issue: the owner, 5 October 2026: "start on the next batch of weapons" (as for Arms V and VI). After the variants of Arms VII, this batch adds what the arms could not yet do: throw them.
Batch number: 59. Main has 55 (Arms VI), Arms VII is 56, and other open work claims 57 and 58; if main takes 59 first, this moves up.

Owner: jimbozoomer-byte
Target milestone and tier: the bronze age (the `tin` feature) and the steel age (`machines`), as the other arms ([arms.md](arms.md) to [arms-vii.md](arms-vii.md)).
Primary specialty and supported player role: fighting, at range without a bow; the harpoon for fighting on and under water.

## Player experience
Four new kinds of arm in bronze and steel. Each fights in the hand like any arm, with its own numbers, moves and motion. **Hold use to wind it back, and let go to throw it**, as vanilla's trident is thrown. A shorter hold than its wind does not throw.

- **What a throw does:** it strikes the first foe in its way for its damage, more for the arm's damage enchantments (Sharpness, Smite and the rest, as Impaling adds to a trident's throw). Then the arm comes down where it struck, **as itself**, with its enchantments and wear, to be picked up. It never despawns while it waits.
- **What it costs:** a throw wears the arm by 1, and never breaks it (one about to break will not throw). Another throw of the same arm waits half a second.
- **In creative:** the arm stays in hand and the throw is a copy, which leaves nothing behind.
- **In flight** each is drawn as its own 3D model: the javelin and the harpoon point first along their flight, the francisca tumbles end over end, and the chakram spins flat.
- **The wind-up:** in third person, the javelin and the harpoon are drawn back over the shoulder with the off hand reaching ahead, the francisca is raised behind the head, and the chakram is drawn across the body for a backhand throw. In first person it is the trident's pull-back.

| Kind | In the hand (bronze, steel) | Thrown (bronze, steel) | Wind | Its own way |
|---|---|---|---|---|
| Javelin | 5.5, 6 at 1.4 a second (jabs) | 7, 8 | 0.5 s | flies far and straight (2.6, 2.8 blocks a tick) |
| Francisca | 7, 7.5 at 1.1 a second; chops wood like an axe | 6, 7 | 0.3 s | tumbles; a foe blocking it with a shield (or a parrying arm) has it knocked down for 3 s, as by an axe's blow |
| Chakram | 5, 5.5 at 1.8 a second | 4, 5 to each foe | 0.3 s | flies flat; cuts every foe in its way (up to 4); at 12 blocks or a wall it turns back, cuts each foe again on the way, and is caught into your inventory |
| Harpoon | 6, 6.5 at 1.3 a second (jabs) | 5, 6 | 0.5 s | keeps its speed underwater; hauls what it strikes towards you, out of the saddle |

## Connections
- **Existing input producer:** bronze and steel ingots, sticks, iron nuggets, leather, string.
  - Javelin: an ingot, 2 sticks, an iron nugget.
  - Francisca: 2 ingots, a stick.
  - Chakram: 3 ingots, leather.
  - Harpoon: 2 ingots, a stick, string.
- **Existing output consumer:** the player against mobs and, where PvP is on, players; enchanting (the melee enchantments).
- **Technology connection:** the batch 25 metal tiers; electroplating plates and repairs them, as the other arms.
- **Magic connection:** none.
- **Reachable entry path:** bronze or steel ingots and a crafting table; craftable solo and tradeable. No circular unlock.
- **Required vs optional:** all optional.
- **How the specialty stays useful:** a reach the melee arms lack, without a bow's ammunition; the arm must be fetched again (the chakram comes back, but strikes softest).

## Balance and automation
- **In the hand:** each is below its metal's sword a second, as every arm is (`check_arms`).
- **Thrown:** no throw hits harder than the trident's 8. Thrown one after another, as fast as the wind and the half-second wait allow, none deals as much a second as a netherite sword's blows (12.8): the steel francisca, the most, deals 8.75 (`check_arms`).
- **The chakram's two passes** strike each foe once each way, at most 4 each way, and let go of the foe's damage cooldown for the second, as the arts' repeated hits do.
- **No conversions or loops:** a throw only wears the arm.

## Multiplayer and persistence
- **Server authority:** every throw is made on the server, from the item's own release after a wind it times itself. The flight, hits, wear, pull and return are all worked there; clients only draw the arm in flight and play its sounds.
- **Who it strikes:** a player's throw strikes only what that player may strike: allies, their own mount, pets and foes another rule protects (the town) are spared, as by the two-handed blows.
- **Saved state:** an arm in flight is saved as an entity (`jugcraft:thrown_arm`) with the arm it carries, whether it is a creative copy, whether the chakram is on its way back, how long it has flown and where it was thrown from. One that has come down is an ordinary item.
- **Stable ids:** `jugcraft:<bronze|steel>_<javelin|francisca|chakram|harpoon>` and the entity `jugcraft:thrown_arm`.
- **Disabling** the `tin` or `machines` feature removes the recipes, not the items.

## Dependencies and assets
- **No new dependencies.**
- **The art:** original, drawn by `tools/arms_art.py` (icons and 3D models, as every arm's), with moves in `tools/arms_moves.py`.
- **Generated by:** `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py`.

## Verification
- **`python3 tools/check_mod_data.py`:** PASS (local). It holds `JugcraftArms.THROWN`, its constants and the renderer's sizes to `tools/arms.py`, and checks the balance above.
- **`python3 scripts/check_repository.py`:** PASS (local).
- **Game tests (`ArmsVIIIGameTests`)**, written for CI, with mock players' real throws (the item's own release after a full wind):
  - every thrown arm is a `ThrownArmItem` with its numbers;
  - a steel javelin strikes a still pig for 8 and comes down as itself, worn by one;
  - a bronze chakram cuts two pigs in line on its way out and again on its way back (8 each), and is caught into its thrower's inventory;
  - a steel harpoon strikes a pig 8 blocks off for 6 and hauls it more than 2 blocks in;
  - a francisca's blow knocks a raised shield down (it goes on cooldown and is lowered);
  - a creative throw strikes, leaves the javelin in hand and nothing behind.
- **Client game test (`ArmsVIIIClientGameTests`)**, written for CI:
  - the four arms hanging in flight before the camera;
  - the javelin wound back with the real use key, from the front, and as it leaves;
  - the francisca in first person, at rest and wound back;
  - a real survival throw of a steel javelin at a still pig (its damage and the javelin come down, read back from the server);
  - a bronze chakram thrown past a pig and back into the inventory.
- **Results (CI):**
  - **dd60fa0a, the first compile, failed:** `Entity.invulnerableTime` is private on 26.3. c8e227da lets go of `LivingEntity.damageCooldownTime` instead, as the arts do. Every other new call compiled (`EnchantmentHelper.modifyDamage`, `BlocksAttacks.disable`, `ItemStack.hurtWithoutBreaking`, `getYRot(float)`).
  - **c8e227da:** the build and all 777 server game tests passed, `ArmsVIIIGameTests` among them (the chakram took each pig from 10 to 2: 8 each, both ways). Client shard 1 failed in the older `ArmsClientGameTests`: with 38 arms a metal its racks need a sixth shot. 40d59d79 gives them one.
  - **40d59d79: all green.** The build, all 777 server tests and the three client shards passed. `ArmsVIIIClientGameTests`:
    - the survival javelin throw took 8.0 from the pig, and the javelin came down;
    - the chakram came back into the inventory once, having taken 8 from the pig;
    - its shots show the javelin wound back over the shoulder and let go, the francisca raised in first person, the javelin come down by the struck pig, and the chakram on its way out. The four in flight are drawn, but small at that distance.
  - **36506e23 and 0570f0d0, with main (#184 and #192) merged in:** the server tests failed once, on both commits. The chakram took 8 from the near pig and only 4 from the far one: its way back missed the far pig. The hit cooldown was ruled out: the field the return pass resets is the one 26.3's `LivingEntity.hurtServer` checks. Each cut knocks its pig back, which can carry it off the walk or out of the chakram's way back. 39f1f6c3 holds the test's pigs still (knockback resistance 1), clears the corridor the chakram flies along, and names where the pigs ended up if it fails.
  - **39f1f6c3: all green.** The build, all 777 server tests (the chakram took both pigs from 10 to 2) and the three client shards passed.
- **Not run:** play; two players; how the throws feel against real mobs; the harpoon underwater (only its code path, which undoes water's drag).

## World and event applicability
Not applicable: no worldgen, dimensions, bosses or seasonal content.

## Rollout and open questions
- **Each design is a first pass** for the owner to judge; easy to change in `tools/arms_art.py` and the numbers in `tools/arms.py`.
- **Variants:** thrown arms in the Arms VII styles, or as boss trophies (a Leviathan harpoon), are left for later.
- **Enchantments:** the trident's own (Loyalty, Riptide, Channeling, Impaling) are not offered; Loyalty-like return is the chakram's alone.
