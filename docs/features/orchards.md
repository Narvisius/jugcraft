# Orchards: pear, peach, lemon and orange trees

Status: implemented in source; **not yet played**. The Build workflow compiles it; see Verification for what has actually run.
Proposal issue: none; requested directly by the owner on 7 October 2026 ("lots more crops, plants, food, cooking devices, preparation systems ... lots and lots of the food to be very decorative and displayable"). This is slice 6 of the ten-slice plan in [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#the-kitchen-and-cooking-expansion-planned). The owner's library has no fruit trees, grapes or berries, so the owner was asked how to go on and chose "Slice 6, Jugcraft art", and of the plan's roster "Pear, peach, lemon, orange" (no grapes, hops or berries yet). The apple tree is unchanged.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. A fruit, found on a wild tree or traded, starts an orchard; nothing needs a station but the Hearth Oven, the Cooking Pot and the Canning Kettle the farm already has.
Primary specialty and supported player role: farming; supports cooks (juices, pies, preserves) and traders (fruit from biomes others do not live in)

## Player experience
Plant an orchard that fruits every year:

- **Four fruit trees,** each grown from its seed: Pear Seeds, a Peach Pit, Lemon Seeds and Orange Seeds. A fruit crafts into its seed, and broken leaves drop one now and then. Planted on dirt or grass, a sapling grows (in time, or with bone meal) into its tree: vanilla oak logs under a crown of its own leaves, each tree in its own shape (the pear tall and pointed, the peach low and wide, the lemon and orange small and round).
- **Blossom and fruit,** as the apple tree's: leaves the tree grew itself, with air below them, flower (pear blossom white with dark red centres, peach blossom pink, citrus blossom white) and then hang with ripe fruit. **A right-click picks the ripe fruit** (one to three drop below) and the leaves start again; the tree is never cut down. Leaves a player places never fruit. Broken ripe, they drop their fruit.
- **The fruit:** a Pear, Peach or Orange is as filling as an apple; a Lemon, sour, half as much.
- **What it makes:**
  - **Orange Juice** (two oranges in a bottle; Health Boost) and **Lemonade** (a lemon and sugar; Speed), drunk even on a full stomach. A sneaking player sets either down as a bottle standing on a table, as the menu's drinks are.
  - **Peach Pie** and **Lemon Meringue Pie,** baked in the Hearth Oven as the other pies are, set down whole and eaten or cut a slice at a time. The lemon pie is topped with toasted meringue.
  - **Orange Marmalade, Peach Preserves and Pear Butter,** cooked into a Mason Jar in the Cooking Pot, sealed in the Canning Kettle and shown on a Pantry Shelf as the other preserves are.
- **Wild trees:** pears and peaches in the Orchard; lemons and oranges in the Mediterranean Forest and the Subtropics; and, outside Jugcraft's biomes, pears in forests, peaches in plains and savannas, lemons in savannas and oranges in savannas and jungles.

| **Ripe:** the pear, peach, lemon and orange trees hung with fruit (left to right), the blossoming row behind | **In blossom:** the same trees in flower, seen from the far side (orange, lemon, peach, pear) |
| --- | --- |
| ![The ripe trees](../images/ingame_orchard_ripe.jpg) | ![The trees in blossom](../images/ingame_orchard_blossom.jpg) |
| **The table:** the peach and lemon meringue pies (one cut), orange juice and lemonade set down, a Pantry Shelf of the new preserves, and the four saplings (the pear and peach in blossom) | **The wall:** the fruit, seeds, leaves, juices, raw pies, slices and preserves in item frames |
| ![The table](../images/ingame_orchard_table.jpg) | ![The items](../images/ingame_orchard_items.jpg) |

*In-game screenshots from CI's client game test (`OrchardClientGameTests`, software rendering, small previews).*

## Connections
- Existing input producer: wild trees in five vanilla biome groups and three Jugcraft biomes; bone meal grows a sapling as any sapling's. Sugar, eggs, glass bottles, pastry dough and Mason Jars as the other recipes take them.
- Existing output consumer: the juices and set-down bottles (the menu's PlacedDishBlock), the Hearth Oven's pies (PieFilling), the pantry's preserves (PreserveJarItem, the Canning Kettle and Pantry Shelf). The fruit is in `c:foods/fruit`, for other mods' recipes.
- Technology connection: the preserves and pies are data recipes in the Cooking Pot and Hearth Oven, ready for the engineered kitchen (slice 10).
- Magic connection: none.
- Reachable entry path: a wild tree (or a traded fruit) gives fruit; a fruit crafts into its seed; the seed plants a sapling. No circular unlock: nothing grown on these trees is needed to reach them.
- Required vs optional: all optional, more food and decoration.
- How this stays useful without other branches: the trees feed a farm and stock a kitchen on their own.

## Balance and automation
Units are hunger points (docs/BALANCE.md); the numbers are in `tools/orchard.py`.

| Food | Hunger (saturation modifier) | Made from |
| --- | --- | --- |
| Pear, Peach, Orange | 4 (0.3), as an apple | a tree |
| Lemon | 2 (0.1) | a tree |
| Orange Juice | 5 (0.5), Health Boost 60 s | 2 oranges (8) and a bottle |
| Lemonade | 4 (0.4), Speed 30 s | a lemon (2), 2 sugar and a bottle |
| Peach Pie, Lemon Meringue Pie | 4 slices of 4 (0.6) | 2 peaches, or 2 lemons and 2 eggs, with pastry dough and sugar (as the apple pie) |
| Orange Marmalade | 4 servings of 3 (0.4) | 3 oranges and a lemon (14), 2 sugar, a jar |
| Peach Preserves | 4 servings of 3 (0.4) | 3 peaches (12), 2 sugar, a jar |
| Pear Butter | 4 servings of 4 (0.5) | 4 pears (16), sugar, a jar |

- **No dish gives more than three hunger over its ingredients** (`tools/menu.py` `COOK_BONUS`, checked by `tools/check_mod_data.py` for the juices); lemonade gives two more than its lemon, orange juice three less than its oranges. The preserves give what went in or less.
- **Fruiting:** a leaves block steps from bare to blossom to ripe one stage in 10 random ticks (`FRUIT_CHANCE`, as the apple's), about a Minecraft day at the default tick rate; a ripe cluster gives 1-3 fruit. Each tree has many leaves, so a grown tree gives a steady harvest without being cut.
- **Seeds:** one fruit crafts into one seed; broken leaves give one 5% of the time (more with Fortune), as the apple's.
- **No loops:** nothing makes fruit from a recipe; fruit to seed goes one way. `tools/check_mod_data.py` follows every agriculture recipe and fails on a loop.
- **Bounded work:** leaves tick only when the game random-ticks them, as vanilla leaves do (and only the tree's own: placed leaves do not tick).

## Multiplayer and persistence
- All changes happen on the server; picking is a normal block use, so vanilla's reach check and the town's protection apply. A pick drops items into the world as breaking a block does.
- Persistence: a leaves block's fruit is block state; nothing else is stored. Chunk unload and restart keep it.
- Every ID is new; nothing existing is renamed or migrated. The two pies are added at the end of `PieFilling`, so a Hearth Oven's saved pie keeps its place.
- Disabling `agriculture` removes the recipes and the wild patches (load conditions and the gated world generation) but keeps the registrations, so saved trees, fruit, pies and jars survive. The Jugcraft biomes' trees are placed under the `biomes` switch, as the chestnut trees in the Orchard are.

## Dependencies and assets
No new dependency.

**Jugcraft's own art,** drawn by code in `tools/orchard_textures.py` from fixed seeds; no Mojang texture is read, traced or recoloured, and none of the owner's files is used (the owner has drawn no fruit trees). The leaves are painted as every Jugcraft tree's are (`tools/wood_style.py` `paint_leaves`, the same painter, each tree in its own green), with blossom or fruit drawn over them; the juices are the bottle every Jugcraft drink is drawn in; the pies are drawn by `tools/pie_textures.py` from their filling's colour (the lemon pie with its new meringue top) and the jars by the pantry's painter. If the owner draws these later, their files can replace these under the same IDs.

The trees use vanilla oak logs, so no new wood is added. The set-down juices' models are fitted to their icons as the menu's are (`tools/menu_data.py`).

## Verification
CI (7 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `76bd76d` | Build, data audit, game tests, client game tests | Compiled; **948 of 949 game tests passed**: `juicesAreDrinks` failed ("orange_juice leaves its bottle"), its mock player not being in survival (a creative player keeps a drink and gets no bottle back). The client screenshots were taken, but the pantry shelf stood side-on |
| `a343378` | The same, the test's player in survival (as the cider test's) and the shelf facing the camera | **All pass:** all 949 required game tests (`OrchardGameTests` among them) and the chosen client classes (`OrchardClientGameTests`, `MenuClientGameTests` and `PieClientGameTests` among them). The screenshots above are from this commit. |

Run locally (7 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: also checks `OrchardTree` against `tools/orchard.py` (each tree's seed and pick, in order), `OrchardLeavesBlock.FRUIT_CHANCE`, the registrations, every tree's sapling, leaves (a model and texture for each fruit stage), item models, loot (a seed now and then and the ripe fruit), tags and words, each tree's feature (oak and its own leaves, in its shape), wild patch and the Jugcraft biomes that pick it, the juices' recipes and textures; and, with the others, the foods and seeds against Java, the juices' balance, the set-down juices, the two pies and the three preserves | Pass, 1719 IDs |
| `python3 tools/owner_art.py --check` | Pass |
| `python3 tools/generate_material_data.py`, then `git status` | Writes this slice's data only |
| `python3 tools/generate_textures.py` | Writes this slice's textures; it also rewrites the same 24 unrelated Styx textures as before, left as committed |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

The new game tests (`OrchardGameTests`):
1. each of the four saplings grows its tree: an oak trunk under at least twelve of its own leaves, grown (not persistent) and bare;
2. for each tree, leaves the tree grew blossom and then ripen on random ticks, a right-click picks them and the fruit drops; placed leaves never fruit;
3. ripe leaves broken by hand drop their fruit;
4. each fruit's seed recipe loads, and the seed, used on dirt, plants the sapling;
5. orange juice and lemonade are drunk on a full stomach, leave their bottles and give Health Boost and Speed;
6. the recipes (juices, the two raw pies, the three preserves), every leaves' and sapling's loot table and each tree's wild patch and biome placement load; the Cooking Pot makes peach preserves and pear butter.

The existing tests cover the rest with the others: every set-down dish (the juices among them) is a block that sets down as itself (`MenuGameTests`), and every pie filling's raw pie recipe loads and its slice is a food (`PieGameTests`).

The client game test (`OrchardClientGameTests`, CI job `client`) grows the four trees twice, a row ripe and a row in blossom, sets a table with the two pies, the two juices, the saplings and a Pantry Shelf of the new preserves, hangs the slice's items on a wall, and takes four screenshots.

Not done: play in a real client and a two-client dedicated-server session.

## World and event applicability
Farming and food. The trees grow wild in new chunks only, in the biomes above. Their leaves are evergreen (they do not follow Jugcraft's seasons, as the apple's do not). Nothing is seasonal.

## Rollout and open questions
- The owner's own art for these trees, if they draw it, replaces Jugcraft's under the same IDs.
- Grapes, hops and raspberries stay on the roster for a later slice, as do cherry trees. Plum and banana trees and blueberries came with the [fruit crops](fruit-crops.md), which add the plum and banana as orchard trees (the banana on its own stem).
- The Cider Press still presses only apples; pears could make perry in it later. The Fruit Salad is still made with an apple.
