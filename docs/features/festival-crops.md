# Festival Crops: gourds on stems, turnips, cranberry bogs and the chestnut tree

Status: implemented in source; **not yet played**. The Build workflow compiles it, and CI's game tests pass (details below).
Proposal issue: none; requested directly by the owner on 30 September 2026 ("start the next agriculture slice"), following slice 3 of the plan in [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#crop-roster-what-comes-next-planned).
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. The sawmill recipe for chestnut logs belongs to the machines (Workshops) and is optional.
Primary specialty and supported player role: farming and cooking; supports builders (a new wood set, gourd and lantern decorations), animal breeders and every player who eats

## Player experience
Grow the crops of autumn and winter festivals, all year round:

- **butternut squash, acorn squash and warty gourds** from stems on farmland, like pumpkins, for food and fall displays;
- **turnips**, and **Turnip Lanterns** carved from them (the original jack-o'-lantern);
- **cranberries**, a bog crop that stands in shallow water;
- **the chestnut tree**, grown from a chestnut, whose leaves ripen spiny burs you pick without cutting the tree down, and a chestnut wood set.

Foods: roasted chestnuts, baked acorn squash, squash pie, candy corn, and in the Cooking Pot, butternut squash soup, harvest stew and cranberry sauce. The player guide is the Festival Crops section of [branches/AGRICULTURE.md](../branches/AGRICULTURE.md#what-exists-now-the-festival-crops).

Nothing is seasonal: every crop, block and recipe is available all year and stays in the world. The planned Halloween and December events ([CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md#seasonal-content)) may build on these crops; none of them depends on an event.

## Connections
- Existing input producer: the Overworld (gourd patches on grass in plains, savanna, forest, taiga, swamp and dark forest; wild turnips in taiga and birch forest; ripe cranberry bushes in swamp shallows; chestnut trees in forests; short grass everywhere), vanilla farmland, water and mud, torches, sugar, eggs, honey, carrots, raw mutton, bowls. Corn comes from the Fall Harvest, onions and garlic from the Kitchen Garden.
- Existing output consumer: food for every player; the Cooking Pot (three new dishes); animal breeding through vanilla tags (`pig_food`: turnips, chestnuts; `rabbit_food`: turnips; `fox_food`: cranberries; `chicken_food` and `parrot_food`: gourd seeds); composters; building (chestnut wood in vanilla's `planks`, `logs_that_burn`, `wooden_stairs`, `wooden_slabs`, `wooden_fences` and `fence_gates` tags, so it makes sticks, crafting tables and chests and burns as fuel); `c:` convention tags (`c:crops/*`, `c:seeds/*`, `c:foods/*`).
- Technology connection: optional. The **sawmill** saws a chestnut log into 6 planks with sawdust (hand crafting gives 4), like vanilla logs.
- Magic connection: none yet.
- Reachable entry path: a hoe, farmland and a seed from grass or the wild; for cranberries, a bucket of water over dirt; for the tree, a chestnut on dirt. No crop needs another branch, a machine or an event.
- Required vs optional: nothing is required. Every dish is extra food; the sawmill recipe is optional.
- How this stays useful without other branches: food, animal feed, decorations and a wood set.

## Balance and automation
- **Gourds:** stems grow like Jugcraft crops (vanilla soil formula, ×1.5 next to beans, no crowding penalty) at wheat's pace, then place a gourd beside them, like pumpkins. 1 gourd → 4 seeds, like a pumpkin. A stem keeps growing gourds; harvest is breaking the gourd.
- **Turnips** follow vanilla carrots (the planted item back, a binomial bonus when ripe).
- **Cranberries:** one stage in 5 random ticks with light 9 or more and air above (vanilla sweet berries' rate); picking gives 2–3 and resets to flowering. Breaking a bush gives its planting berry back, plus 1–2 when ripe.
- **Chestnuts:** tree leaves with air below ripen one bur each, a stage in 10 random ticks (about a Minecraft day to ripe); picking gives 1–2. Only leaves the tree grew bear fruit, so placed leaves cannot be farmed in a compact wall. Broken leaves drop a chestnut like vanilla leaves drop saplings (5–10 %) plus the ripe bur's nuts.
- **Food values** stay within vanilla's range (full table in the branch document): squash pie equals pumpkin pie, harvest stew equals rabbit stew with five ingredients, cranberries equal sweet berries, and raw chestnuts are not food.
- **No positive-gain loops:** gourds → seeds, chestnut logs → planks, and every other recipe turns crops into food. `tools/check_mod_data.py` fails if any agriculture recipe forms a loop.
- **Grass seeds:** short grass still drops one Jugcraft seed 12.5 % of the time; it is now chosen from eighteen crops.
- **Automation:** the sickle picks ripe cranberries and cuts gourds off their stems (not gourds a player set down). There is no powered automation (planned for the engineering branch). Wood burns as fuel (logs and planks like oak, slabs half as long).

## Multiplayer and persistence
- All growth, planting, picking and stripping runs on the server. Planting uses vanilla block items (with the usual build checks), and the sickle checks `mayInteract` for each block it touches.
- Everything stores its state in block states: stem age and direction, bush age, leaf fruit. There are no block entities and no saved data.
- Server work is random ticks only, each reading a few blocks: a stem reads its soil like any Jugcraft crop, a bush the block above it, and chestnut leaves the block below them. A chestnut tree's natural leaves tick all the time to grow burs (vanilla leaves tick only while decaying); leaves players place never tick.
- A cranberry bush holds a water source like seagrass: breaking it leaves the water, and it cannot be used to create or remove water.
- New IDs only. The only existing behaviour that changes is which seeds short grass can drop (18 instead of 12, at the same overall rate). The `agriculture` switch stops recipes, wild plants, gourd and cranberry patches and chestnut trees in new chunks, and grass drops. The chestnut wood's hand and sawmill recipes stop only when `biomes` is off too, since the Orchard also grows chestnut trees ([trees-batch-1.md](trees-batch-1.md)). Registered blocks and items stay, so saved fields, bogs and trees survive.

## Dependencies and assets
No new dependencies. Uses Fabric API's flammable-block registry (content registries), loot, biome modification and creative tab APIs, which are already required. Every texture is drawn by `tools/festival_textures.py` from fixed seeds (since the [wood repaint](wood-repaint.md), the chestnut's wood and leaves by `tools/wood_style.py`); no Mojang texture is read, traced or recolored. Models reuse vanilla's model templates (cross, cube, stairs, slab, fence, fence gate) with Jugcraft textures; the stem models are rebuilt without vanilla's biome tint.

## Verification
Actual results (30 September 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also checks the gourd, cranberry and chestnut numbers against the Java constants, the stem, bush and leaf blockstates, the wood set's registrations, bog-seed rules and the new wild patches) | Pass, 292 IDs |
| `./gradlew build`, compile, with the Kitchen Garden, the Fall Harvest and `main` after #40 merged in (`4c59f82`) | Pass |
| Game tests on the headless server, same commit: 90 in total, 13 of them new here | **All 90 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`, same commit): builds a gourd patch, a cranberry bog, two chestnut trees with burs, a chestnut-wood market stall with Turnip Lanterns and every growth stage, and takes screenshots by day and at midnight | **Passes**; the screenshots in the branch document are from this run |

The 13 new game tests (`FestivalGameTests`); the grass-drop test in `AgricultureGameTests` now covers all 18 seeds:
1. gourd seeds plant a stem on farmland, and not on grass;
2. a fully grown stem puts its gourd on soil beside it (not over empty space), facing away, and bends towards it; taking the gourd away straightens the stem to grow another;
3. one sickle swing cuts a gourd off its stem, which then grows another; a gourd a player set down is left alone;
4. cranberries plant into water one block deep over mud, and not into water two blocks deep or onto dry mud; the bush keeps its water and leaves it when broken;
5. ripe cranberries are picked by hand or with a sickle, and the bushes flower again;
6. random ticks ripen a bush in shallow water, and not one with water over it;
7. a chestnut sapling grows into a chestnut tree of logs and leaves;
8. tree-grown leaves over air grow burs that ripen, and a right-click picks the chestnuts; placed leaves never fruit;
9. any axe strips a chestnut log, keeping its axis; the wood joins vanilla's wood tags and burns as fuel;
10. ripe bushes, burs and turnips drop their crop, gourds drop themselves, and the Turnip Lantern gives light 13;
11. the new foods restore the values in the branch document, raw chestnuts are not food, and the sauce does not stack;
12. the three new Cooking Pot recipes load and are found from their ingredients in any order;
13. the festival's worldgen loads: the wild turnip, gourd, cranberry and chestnut-tree patches and the chestnut tree feature.

Two runs found real problems, both fixed before this record:
- **Test 7 failed at first:** a tree needs about 10 free blocks above its sapling, more than a game-test area is tall, so the sapling could not grow. The test now plants on top of the area's ceiling. The tree feature itself was not changed.
- **The first client run showed an empty bog:** the server held all 74 bushes (the test logs the count), but a cross model of normal height sits under the water surface and could not be seen. The bush model now stands 4 pixels higher, so its top rises above the water, with textures redrawn for it and a taller outline. The second run (`4c59f82`) shows the bushes above the water.

The test server log shows no load or validation warnings for any festival loot table, tag, recipe or worldgen file, and the client log shows no model or texture errors. The three new Cooking Pot recipes log "can't be placed due to empty ingredients and will be ignored", the same notice the other multi-input recipes log; it concerns only the vanilla recipe book, and the recipes load (test 12). The server log's "Unreachable entry!" warnings come from the older ore loot tables, which PR #34 fixes.

**Not run:** a person playing in a client, a dedicated server with two players, save/restart, natural generation of the new patches and chestnut trees in a new world (test 13 only checks that the features load), random-tick growth over hours of play, leaf decay around fruiting leaves, and performance measurement. These need a play session.

## World and event applicability
Gourd patches appear in about 1 of 32 chunks of a matching biome, on grass. Cranberry patches are tried in about 1 of 4 swamp chunks and only fill water exactly one block deep over bog soil. Chestnut trees appear in about 1 of 3 forest chunks. All only in chunks generated after this feature; grass drops cover older worlds. There are no hostile or dangerous elements, and nothing is seasonal.
