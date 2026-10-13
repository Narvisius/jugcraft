# Fall Harvest: first Agriculture crops

Status: implemented in source; **not yet played**. The Build workflow compiles it, and CI's game tests pass (details below).
Proposal issue: none; requested directly by the owner on 30 September 2026 ("an Agriculture branch, starting with fall crops like corn").
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier, with the Bronze Sickle as an optional Workshops-era upgrade
Primary specialty and supported player role: farming; supports cooks, animal breeders, builders (fields and mazes) and future engineers (oil, fibre)

## Player experience
Find wild corn, sunflowers, beans, sweet potatoes and flax, or get their seeds from grass. Plant them on farmland and grow:

- **corn three blocks tall**, which you pick by right-clicking without cutting it down, so cornfields and corn mazes stay standing;
- **sunflowers two blocks tall** with big yellow heads;
- **beans** that make every crop next to them grow faster;
- **sweet potatoes** and **flax**.

Cook roasted corn, popcorn, baked sweet potatoes and roasted sunflower seeds, or make Three Sisters Stew. Spin flax into string, and harvest whole fields with a Flint or Bronze Sickle. The full player guide and the plan for the rest of the branch are in [branches/AGRICULTURE.md](../branches/AGRICULTURE.md).

![The Fall Harvest items](../images/agriculture_items.png)

![A corn maze in the game, from above](../images/ingame_corn_maze.jpg) ![At the maze entrance](../images/ingame_corn_maze_entrance.jpg)

*In-game screenshots from CI's client game test (software rendering, small previews). The branch document has more.*

## Connections
- Existing input producer: the Overworld (wild plants in plains, savanna, forest, jungle and flower biomes; short grass everywhere), vanilla farmland, bone meal, pumpkins, bowls, flint and sticks. The Bronze Sickle uses Jugcraft bronze.
- Existing output consumer: food for every player. Vanilla string (bows, wool, leads, fishing rods). Animal breeding (vanilla `pig_food`, `chicken_food` and `parrot_food` tags). Composters. `c:` convention tags (`c:crops/*`, `c:seeds/*`, `c:foods/*`) let other mods use the crops.
- Technology connection: optional. The Bronze Sickle is the upgrade path from metallurgy. Planned links are listed in the branch document: plant oil from sunflower and flax seeds, ethanol from corn, and powered harvesting later.
- Magic connection: none yet. School herbs are a planned slice.
- Reachable entry path: a wooden hoe, farmland and a seed. Every seed has two independent sources (wild plants in common biomes, and short grass anywhere). No crop needs another branch, a machine or an event.
- Required vs optional: nothing is required. Bronze only upgrades a tool; the Flint Sickle covers the same job at a smaller size.
- How this stays useful without other branches: food, animal feed, string, decoration (mazes, sunflower fields) and a faster farm through companion planting.

## Balance and automation
- **Growth:** vanilla random ticks and the vanilla soil formula, plus a ×1.5 bonus next to a legume (beans). Dense planting is not penalised. Corn needs 1.5× and sunflowers 1.25× wheat's time per stage. Bone meal gives 1–2 stages to tall crops and 2–5 to the one-block crops (vanilla).
- **Yields:** picking corn gives 2–3 ears per regrowth (2 stages); sunflowers give 2–4 seeds. Breaking a tall plant returns its seed, plus the same harvest if ripe. Beans and sweet potatoes follow vanilla carrots; flax follows vanilla wheat. Fortune applies.
- **Food values** match vanilla equivalents: roasted corn and baked sweet potato equal a baked potato; the stew equals rabbit stew. See the branch document.
- **Conversions go one way:** 1 corn → 2 kernels, 2 flax → 1 string, cooking. `tools/check_mod_data.py` fails if agriculture recipes form a loop.
- **Automation:** the sickle harvests at most a 5×5×3 area per use for 1 durability. Vanilla hoppers and composters work as usual. No powered automation yet (planned for the engineering branch).

## Multiplayer and persistence
- All growth, picking and harvesting runs on the server. Sickles check `mayBuild` and `mayInteract` (spawn protection and claims) for every block they touch. Clients send only vanilla use-item and break-block actions.
- Tall crops store everything in block states (`age`, `section`); there is no block entity or saved data to migrate. Only the bottom block ticks. Breaking any block removes the whole plant, and its loot drops exactly once (only the bottom block has loot; upper-block breaks remove the bottom without a second drop).
- New IDs only; no existing content changes. The `agriculture` switch in `config/jugcraft.properties` stops recipes, wild plants in new chunks and grass drops. Registered crops and items stay, so planted fields in saved worlds survive.
- Wild plants generate only in chunks created after this feature is added; grass drops cover older worlds.

## Dependencies and assets
No new dependencies. Uses Fabric API's loot, biome modification and creative tab events, which are already required. Every texture is drawn by `tools/crop_textures.py` from fixed seeds; no Mojang texture is read, traced or recolored. Since the [garden crops](garden-crops.md) (9 October 2026), the corn (and ornamental corn), the corn and corn kernel items and the wild corn wear the owner's own textures instead. The `ingame_*` images are real screenshots from the client game test. The stage lineups and the item strip come from `tools/render_agriculture.py` and are approximate renders.

## Verification
Actual results (30 September 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also checks that the agriculture Java matches `tools/agriculture.py`, that every crop state has a model, and that there are no recipe loops) | Pass |
| `./gradlew build`, compile | Pass |
| Game tests on the headless server: 36 in total, 12 of them new | **All 36 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`): plants a corn maze, a cornfield, a sunflower field, crop rows, every growth stage and the wild plants, then takes screenshots | **Passes.** The crops render with transparent cut-outs; the screenshots are a CI artifact |

The 12 agriculture game tests (`src/gametest/.../AgricultureGameTests.java`):
1. corn grows to three blocks tall, every block showing the right section and age;
2. a block above stops the growth;
3. right-clicking ripe corn drops ears and keeps the plant three blocks tall;
4. breaking the top block removes the whole plant and drops kernels and ears once;
5. two-block corn blocks movement (using the collision query entities use), and knee-high corn does not;
6. corn kernels plant corn on farmland;
7. a sickle harvests and replants ripe beans and leaves unripe ones alone, for 1 durability;
8. beans speed up a neighbour by exactly ×1.5 and do not boost themselves;
9. the new 26.x-format loot tables drop flax, sweet potatoes and wild-plant seeds;
10. corn fills a composter;
11. foods restore the values in the table, raw beans are not edible, and the stew does not stack;
12. short grass drops Jugcraft seeds.

The server log shows no load or validation warnings for any agriculture file: loot tables, recipes, tags and worldgen. The only loot warnings come from the older ore tables (see below).

**Not run:** a person playing in a client, a dedicated server with two players, save/restart, natural wild-plant generation in a new world, random-tick growth over time, and performance measurement. These need a play session.

## World and event applicability
The wild plants follow each crop's real origin: corn's wild ancestor in warm grasslands, sunflowers on prairies, beans at forest edges, sweet potatoes in the tropics, flax in meadows. Patches appear in about 1 of 24 chunks of a matching biome, on grass only. There are no hostile or dangerous elements. The crops are permanent, not seasonal, and later Halloween and December content will reuse them without removing anything.

## Rollout and open questions
- Tune growth times and yields after a real playtest.
- Corn's collision (a maze wall from two blocks tall) is deliberate; if players prefer to walk through fields, the option is a crop-specific flag.
- Recipe viewers need a plugin to show the new foods' uses; none is confirmed for 26.3 yet.
- Found while testing, outside this feature: the existing ore loot tables use the pre-26.x format, and the game test server reports "Unreachable entry!" for all 23 of them. This probably means ores drop themselves instead of raw ore. It needs its own fix.
