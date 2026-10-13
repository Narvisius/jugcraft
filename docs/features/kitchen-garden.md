# Kitchen Garden: trellises, seven crops and the Cooking Pot

Status: implemented in source; **not yet played**. The Build workflow compiles it, and CI's game tests pass (details below).
Proposal issue: none; requested directly by the owner on 30 September 2026 ("start the next agriculture slice"), following slice 2 of the plan in [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#crop-roster-what-comes-next-planned).
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. The Cooking Pot needs iron and a campfire; sauerkraut optionally uses salt from the mining branch.
Primary specialty and supported player role: farming and cooking; supports animal breeders, builders (trellis gardens) and every player who eats

## Player experience
Grow a kitchen garden next to the Fall Harvest fields:

- **tomatoes that climb trellises**, two blocks tall, and are picked without cutting them down;
- **pepper bushes**, picked the same way;
- **onions, garlic, cabbage, oats and barley**, one-block crops that follow vanilla rules.

Stand trellises on farmland (stack two for tomatoes), plant tomato seeds on them, and pick red tomatoes all season. Put an iron **Cooking Pot** on a campfire and cook soups, porridge, chili and cabbage rolls from six ingredient slots. Make garden salad, sauerkraut (with Jugcraft salt) and barley bread by hand. The player guide is the Kitchen Garden section of [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#what-exists-now-the-kitchen-garden).

![Rows of trellises with ripe red tomatoes](../images/ingame_tomato_trellis.jpg) ![The Cooking Pot's screen](../images/ingame_cooking_pot_screen.jpg)

*In-game screenshots from CI's client game test (software rendering, small previews). The branch document has more.*

## Connections
- Existing input producer: the Overworld (seven new wild plants in jungle, savanna, badlands, plains, hills, forest, taiga and windswept biomes; short grass everywhere), vanilla farmland, campfires, bowls, sticks, iron, bread, carrots, potatoes, sugar, brown mushrooms and raw beef. Beans come from the Fall Harvest. Salt comes from the mining branch's rock salt and is only needed for sauerkraut.
- Existing output consumer: food for every player. Animal breeding through vanilla tags (`rabbit_food`, `pig_food`, `cow_food`, `sheep_food`, `goat_food`, `horse_food`, `chicken_food`, `parrot_food`). Composters. `c:` convention tags (`c:crops/*`, `c:seeds/*`, `c:foods/*`) let other mods use the crops and dishes.
- Technology connection: optional. The pot's heat sources are a block tag (`jugcraft:heat_sources`), so a later engineering heater can join it without code. Cooking Pot recipes use the same data format as Jugcraft's multi-input machine recipes.
- Magic connection: none yet. Garlic is noted in the plan as a future ward for the planned Vampirism school; nothing is built for it.
- Reachable entry path: a hoe, farmland and a seed from a wild plant or grass; for tomatoes, 5 sticks for two trellises; for the pot, 5 iron ingots, 2 sticks and a campfire. No crop needs another branch, a machine or an event.
- Required vs optional: nothing is required. Every Cooking Pot dish is extra food; the salt recipe is one optional dish.
- How this stays useful without other branches: food with more variety and value than vanilla, animal feed, and garden decoration.

## Balance and automation
- **Growth:** the same rules as the Fall Harvest (vanilla soil formula, ×1.5 next to beans, no crowding penalty). Tomatoes and peppers take 1.25× wheat's time per stage and are picked back to stage 5 of 7.
- **Yields:** picking gives 2–4 tomatoes or 1–3 peppers per regrowth. Onions and garlic follow vanilla carrots; cabbage, oats and barley follow vanilla wheat. Fortune applies to broken crops.
- **Food values** stay within vanilla's range: raw vegetables like a carrot or apple, bread like bread, soups between mushroom stew and rabbit stew, and chili (six ingredients including beef) at rabbit stew's hunger with a steak's saturation. Full table in the branch document.
- **No positive-gain loops:** seeds come from produce (1 tomato → 2 seeds, 1 pepper → 2 seeds), and every other recipe turns crops into food. `tools/check_mod_data.py` fails if any agriculture recipe (crafting, cooking or Cooking Pot) forms a loop, and if two Cooking Pot recipes share the same ingredients.
- **Grass seeds:** short grass now drops one Jugcraft seed 12.5 % of the time, chosen evenly from all twelve crops. That is vanilla wheat seeds' rate, and it no longer grows as crops are added. With the Fall Harvest's 2 % per crop, twelve crops would have made grass drop a Jugcraft seed 24 % of the time.
- **Automation:** hoppers fill the pot from the top and sides and empty it from the bottom, and a comparator reads its result slots. There is no powered automation (planned for the engineering branch). The sickle harvests the new crops: it picks tomatoes and peppers and replants the others.

## Multiplayer and persistence
- All growth, planting, picking and cooking runs on the server. Planting on a trellis checks `mayBuild` and `mayInteract` (spawn protection and claims). The pot's screen syncs progress and heat through vanilla container data; clients send only vanilla slot clicks.
- Crops and trellises store everything in block states. The Cooking Pot has a block entity that saves its ten slots and its cooking progress (a save and restart has not been tested), and it drops its contents when broken.
- The trellis rule prevents duplication: a climbing crop grows only into trellis blocks it replaces, and each of its blocks drops exactly that trellis again.
- New IDs only. The only existing behaviour that changes is the short-grass seed drop (see Balance). The `agriculture` switch stops recipes (crafting and Cooking Pot), wild plants in new chunks and grass drops. Registered blocks and items stay, so saved gardens and pots survive.

## Dependencies and assets
No new dependencies. Uses Fabric API's menu, block entity, loot, biome modification and creative tab APIs, which are already required. Every texture is drawn by `tools/kitchen_textures.py` from fixed seeds; no Mojang texture is read, traced or recolored. Since the [garden crops](garden-crops.md) (9 October 2026), the tomato, onion and cabbage, their items, the wild tomato, onion and cabbage and the Garden Salad wear the owner's own textures instead. The Cooking Pot screen reuses Jugcraft's own machine screen background.

## Verification
Actual results (30 September 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also checks climbing-crop seeds, the Cooking Pot's numbers, recipe files and unique ingredient sets, and loops through every agriculture recipe) | Pass, 249 IDs |
| `./gradlew build`, compile, with the Fall Harvest and `main` after #36 merged in (`5119d4a`) | Pass |
| Game tests on the headless server, same commit: 68 in total, 13 of them new here | **All 68 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`): plants tomato trellis rows, crop rows, oat and barley fields, every growth stage, the wild plants and a Cooking Pot on a campfire, takes screenshots, then opens the pot's screen by using it | **Passes** on `e5b0001`, the last commit before `main` after #36 was merged in; the screenshots above are from that run |

The 13 new game tests (`KitchenGardenGameTests`, plus one in `AgricultureGameTests`):
1. tomato seeds do nothing on bare farmland and plant a tomato in a trellis standing on farmland, using one seed;
2. a tomato grows only into trellis: without a second trellis it stops at one block, with one it climbs to two, and it blocks movement;
3. picking a ripe tomato keeps the plant; breaking it returns the seed and both trellises;
4. trellises and climbing tomatoes keep the farmland under them (40 ticks later);
5. picking a ripe pepper drops peppers and sets the bush back to flowering;
6. ripe onions, garlic, cabbage, oats and barley drop their harvest and seeds (26.x loot tables);
7. one sickle swing replants ripe onions and picks ripe peppers;
8. the new foods restore the values in the branch document, onions, garlic, oats and barley are not edible raw, and soups do not stack;
9. every Cooking Pot recipe loads and is found from its ingredients in any order;
10. on a lit campfire the pot cooks two batches of tomato soup from ingredients spread over three slots, into separate result slots;
11. without heat, or with a stray item in a slot, the pot does not cook and does not show broth;
12. breaking a pot drops the pot and its contents;
13. tall corn keeps its farmland (`AgricultureGameTests`): Minecraft 26.3 counts a two-block corn plant as a "solid" block, and this checks that the farmland under it survives anyway.

The first run of test 10 found a real problem: soups do not stack, so the pot, which then had one result slot, stopped after one bowl. It now has four result slots.

The test server log shows no load or validation warnings for any Kitchen Garden loot table, model, tag or worldgen file. The Cooking Pot recipes log "can't be placed due to empty ingredients and will be ignored", the same notice the existing multi-input machine recipes log. It concerns only the vanilla recipe book, and the recipes load and cook (tests 9 and 10). The server log's "Unreachable entry!" warnings come from the older ore loot tables, which PR #34 fixes.

**Not run:** a person playing in a client, a dedicated server with two players, save/restart (the pot saves its slots and progress, untested), natural wild-plant generation in a new world, random-tick growth over time, and performance measurement. These need a play session.

## World and event applicability
The wild plants follow each crop's real origin (tomatoes and peppers in warm biomes, garlic in woodland, cabbage on windy hills, grains in fields and dry hills). Patches appear in about 1 of 24 chunks of a matching biome, on grass only. There are no hostile or dangerous elements. Nothing is seasonal.
