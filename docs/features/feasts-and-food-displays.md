# Feasts and food displays: feasts, pies in the owner's art, plates, platters and trays

Status: implemented in source; **not yet played**. The Build workflow compiles it; see Verification for what has actually run.
Proposal issue: none; requested directly by the owner on 7 October 2026 ("lots and lots of the food to be very decorative and displayable"). This is slice 2 of the ten-slice plan in [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#the-kitchen-and-cooking-expansion-planned); the owner asked for it next ("Lets begin the next part").
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. Every recipe uses vanilla food, Jugcraft's onion, cabbage and mutton chops, planks, sticks and terracotta.
Primary specialty and supported player role: cooking; supports farmers, ranchers and builders (dining rooms and market stalls to dress)

## Player experience
Cook a feast, set the table, and show food off:

- **Feasts.** Five dishes placed whole on a table and served four servings at a time: **Roast Chicken**, **Honey-Glazed Ham**, **Shepherd's Pie**, **Stuffed Pumpkin** and the **Gleaming Salad** (it glows, light 6, while any is left). Use a bowl on one to take a serving away (a bowl food that gives the bowl back), or use it while hungry to eat a serving in place. Each serving eaten or taken shows on the model: the chicken loses its drumsticks, then its breast; the ham is carved from the front; the pie goes a quarter at a time; the pumpkin's heap and the salad go down. The last serving leaves the leftovers (the carcass, the bone, the crumbs, the hollow pumpkin), which a use clears, giving a bone from the chicken and the ham and two pumpkin seeds from the pumpkin. Only a whole feast picks up again. A full player is told to use a bowl.
- **Pies in the owner's art.** As the owner chose, the Hearth Oven's **apple pie** now wears their top, filling, crust, slice and icon (same ID, same baking, same four slices). Two new pies join it in their art: a **chocolate pie** and a **sweet berry cheesecake**, baked like the others from pastry, sugar and their filling.
- **The pumpkin pie set down.** As the owner chose, sneak and use vanilla's pumpkin pie on a block to set it down as the owner's pumpkin pie. Eat it or cut it with a knife a slice at a time (a **Slice of Pumpkin Pie**); whole, it breaks back into the vanilla pie. Jugcraft's Pumpkin Cream Pie stays as it was.
- **Plate, Platter and Serving Tray.** Set anything on them to show it: one thing on a plate, four on a platter or a tray, each set where you use it (a quarter each). Food lies flat; a block (a cake, a pie) stands. Use a place again to take its thing back.

Every texture is the owner's own, imported unchanged, except the plate, which their library does not have (see Dependencies and assets).

| **The feast table:** each feast whole (back), half eaten, and as leftovers (front) | **The pies:** apple, chocolate, the sweet berry cheesecake and a pumpkin pie set down, whole and half eaten |
| --- | --- |
| ![Feasts at every serving](../images/ingame_feasts.jpg) | ![The pies in the owner's art](../images/ingame_feast_pies.jpg) |
| **The displays:** a plate with a slice of pie, a platter of roast food, a tray with a cake and an apple pie | **The wall:** the feasts, servings, pies, slices and displays |
| ![Plate, platter and serving tray](../images/ingame_food_displays.jpg) | ![The new items in item frames](../images/ingame_feast_items.jpg) |

*In-game screenshots from CI's client game test (`FeastsClientGameTests`, software rendering, small previews).*

## Connections
- Existing input producer: vanilla food (cooked chicken and porkchops, baked potatoes, carrots, bread, honey, sweet and glow berries, melon, pumpkins, mushrooms, cocoa, milk); Jugcraft's onion and cabbage (Kitchen Garden) and cooked mutton chops (Farmhouse Kitchen); the Hearth Oven's pastry. The displays need slabs and sticks, or white terracotta.
- Existing output consumer: food for every player. The servings and the pumpkin pie slice carry `c:foods` (and the slice `c:foods/pie`); the new pies join the Spirit Board's pie wishes. The plates, platters and trays dress the Harvest Feast Table, the kitchen and market stalls.
- Technology connection: none in this slice. The feasts are data-driven shapeless recipes, so the engineered kitchen (slice 10) can make them by machine later.
- Magic connection: none.
- Reachable entry path: every feast is a crafting-table recipe from foods reachable on the first day (a pumpkin, potatoes, a chicken, bread) plus an onion or cabbage from wild plants or grass seeds; the pies need the Hearth Oven, already reachable from bricks and stone. No circular unlock: none of these blocks is needed to make another.
- Required vs optional: all optional, extra food and decoration.
- How this stays useful without other branches: a feast feeds four with one craft; the displays are decoration.

## Balance and automation
- **A feast gives about what goes into it,** never more than one hunger point over its ingredients (`tools/feasts.py` `COOK_BONUS`, checked by `tools/check_mod_data.py`). Saturation is hunger × modifier × 2, as in Minecraft.

  | Feast | Ingredients (hunger) | Four servings (hunger / modifier each) |
  | --- | --- | --- |
  | Roast Chicken | cooked chicken, baked potato, carrot, onion, bread (19) | 5 / 0.7: 20 |
  | Honey-Glazed Ham | 2 cooked porkchops, honey bottle, sweet berries, baked potato (29) | 7 / 0.8: 28 |
  | Shepherd's Pie | 2 cooked mutton chops, 2 baked potatoes, carrot, onion (19) | 5 / 0.7: 20 |
  | Stuffed Pumpkin | pumpkin (8 as the slices a knife cuts), bread, baked potato, onion, brown mushroom, sweet berries (20) | 5 / 0.6: 20 |
  | Gleaming Salad | 2 glow berries, 2 melon slices, cabbage, sweet berries (13) | 3 / 0.6: 12 |

  The honey bottle leaves its glass bottle, as crafting with one does.
- **Pies.** The chocolate pie and the cheesecake are four slices of 4 / 0.6, like the apple pie. The placed pumpkin pie's four slices of 2 / 0.3 add up to vanilla's pumpkin pie (8 / 0.3) exactly, so setting one down is never a gain; a cut pie gives back only slices.
- **Displays.** Platter: three wooden slabs. Serving Tray: two sticks over three wooden slabs. Plates: two white terracotta make four.
- **Automation.** None. The feasts and displays are filled and emptied by hand and are not containers, so hoppers and pipes cannot reach them; a comparator reads a feast's servings and how full a display is.

## Multiplayer and persistence
- All changes happen on the server; the client only predicts. Serving, eating and clearing a feast, setting a pie down and using a display run on the server thread, one interaction at a time. Vanilla's interaction packet checks reach and spawn protection before any of this runs. Setting a pumpkin pie down runs in a use-block event phase after the default one, where the town's protection decides, and checks `mayUseItemAt` and that the block can go there, as placing a block does.
- Persistence: a feast keeps its servings and facing in its block state; a placed pie its slices; a display saves what is on each place (its block entity, `jugcraft:food_display`) and spills it when broken. Chunk unload and restart keep them.
- Disabling `agriculture` removes the recipes (load conditions) but keeps the registrations, so saved blocks and items survive. The apple pie keeps its ID; only its look changed. Everything else is new; nothing is renamed or migrated.

## Dependencies and assets
No new dependency; Fabric API's use-block event, already used elsewhere, carries setting the pumpkin pie down.

**The owner's own textures,** used as drawn (the owner, 7 October 2026: "I made all of the textures in there myself its all mine"). `tools/owner_art.py` copies each file byte-for-byte from `art/owner-library/originals/Blocks/farming and food textures/`, runs last in `tools/generate_textures.py`, and `tools/check_mod_data.py` fails if a runtime copy differs from its source. Imported here: 48 textures, plus 3 composed icons.

| Runtime texture (`assets/jugcraft/textures/`) | Library file (`farming and food textures/`) | Change |
| --- | --- | --- |
| `block/baked_pie_side`, `block/baked_pie_bottom` | `pie_side`, `pie_bottom` | renamed (Jugcraft's own `pie_side` stays for its other pies) |
| `block/{apple_pie,chocolate_pie,sweet_berry_cheesecake,pumpkin_pie}_top` and `_inside` | `<pie>_top`, `<pie>_inner` | `_inner` renamed `_inside`; the apple pie's replace Jugcraft's drawings |
| `item/{apple_pie,chocolate_pie,sweet_berry_cheesecake}`, `item/{apple_pie,chocolate_pie,sweet_berry_cheesecake,pumpkin_pie}_slice` | the same names | none |
| `block/platter`, `block/serving_tray`, `block/serving_tray_bottom`, `block/salad_bowl` | `platter`, `tray`, `tray_bottom`, `bowl` | renamed |
| `block/roast_chicken{,_details,_side_dish,_leftovers}`, `block/honey_glazed_ham{,_details,_side_dish,_leftovers}` | the same names | none |
| `block/shepherds_pie_{top,side,inside,leftovers}` | the same names (`inner` for `inside`) | `_inner` renamed `_inside` |
| `block/stuffed_pumpkin_{top,top_eaten,side,bottom,details}`, `block/gleaming_salad{,_details,_leftovers}` | the same names | none |
| `item/{roast_chicken,honey_glazed_ham,shepherds_pie,stuffed_pumpkin,gleaming_salad}` | `<feast>_block` | renamed |
| `item/bowl_of_shepherds_pie`, `item/bowl_of_stuffed_pumpkin` | `shepherds_pie`, `stuffed_pumpkin` | renamed |
| `item/bowl_of_{roast_chicken,honey_glazed_ham,gleaming_salad}` | composed: the bowl of `shepherds_pie` (rows 9 to 15) with a window of `<feast>_block` heaped in it | **composed** (`tools/feasts.py` `COMPOSED`); the owner drew no icon for these servings |

The library's [catalog](../../art/owner-library/catalog/files.csv) lists each source file's SHA-256. **The plate** (`block/plate`: a round white glazed plate with a blue band) is drawn by code (`tools/feasts_textures.py`) because the library has no plate. The models (`tools/feasts_data.py`) are drawn to fit the owner's textures: each face's UVs are where that part sits in their texture, checked against their whole-feast icons in an isometric preview.

## Verification
CI (7 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `2697dc6` | Build, data audit, game tests (with `FeastsGameTests`), client game tests (19 chosen classes, `FeastsClientGameTests` among them), repository check | **All pass.** The screenshots above are from this run. |
| later | the same | Recorded by the PR's checks. |

Run locally (7 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: also checks `FeastDish`, `FeastBlock`, `FoodDisplay` and the placed pumpkin pie against `tools/feasts.py`, every feast's balance against its ingredients, the pie fillings (now named pies), and every feast's and display's models, blockstates, loot, recipes, items and words | Pass, 1603 IDs |
| `python3 tools/generate_material_data.py`, then `git status` | No drift |
| `python3 tools/generate_textures.py` | Writes this slice's textures; it also rewrites 24 unrelated Styx textures (its flowers and an entity) differently from what is committed; that drift predates this slice, and those files are left as committed |
| `./gradlew build`, game tests and client game tests | Not run locally (no Minecraft jar here); run by CI, above |

The 4 new game tests (`FeastsGameTests`):
1. a sweet berry cheesecake bakes in the Hearth Oven and comes out whole; a hungry player eats a slice of chocolate pie (four food); a knife cuts a slice of cheesecake; both raw pies exist;
2. not sneaking, a pumpkin pie is not set down; sneaking, it is, and used up; a hungry player eats a slice (two food); a knife cuts a Slice of Pumpkin Pie; four slices add up to the vanilla pie; whole it breaks into the vanilla pie, cut into nothing;
3. a whole roast chicken reads 15 on a comparator; a bowl takes a serving (five food, one to a stack); a full player eats nothing; a hungry one eats a serving (five food); the last serving leaves leftovers (0) that a use clears for a bone; the stuffed pumpkin's leftovers give two seeds; a whole ham breaks into itself, a served shepherd's pie into nothing; the gleaming salad glows only while any is left;
4. a platter holds an apple at its north-west and bread at its south-east (a comparator reads 7) and gives the apple back to an empty hand; a plate holds one slice of cake and gives it back; broken, it spills its slice and drops itself.

The client game test (`FeastsClientGameTests`, CI job `client`) lays a feast table (each feast whole, half eaten and as leftovers), a pie table (apple, chocolate, cheesecake and pumpkin, whole and half eaten), the three displays laid with food, and a wall of the new items in item frames, and takes four screenshots; it passed on `2697dc6`.

Not done: play in a real client and a two-client dedicated-server session (two diners at one feast, two players at one platter).

## World and event applicability
Food and decoration only: no world generation, creatures, dimensions or loot tables beyond each block's own drops. Nothing is seasonal.

## Rollout and open questions
- Three serving icons are composed from the owner's bowl and feast icons, as they drew none; the owner may want to draw their own.
- The library's other feast-like art (cakes, more pies, the rest of the menu) belongs to slice 3, the menu, which adds the nachos as a sixth feast ([the-menu.md](the-menu.md)).
