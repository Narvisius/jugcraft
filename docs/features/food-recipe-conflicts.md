# Food and cutting-board recipe conflicts

Owner-requested server bug fixes, 23 October 2026. Implemented with OpenAI Codex
(GPT-6) under the owner's direct authorization. This is the first batch from the
recipe conflict audit; machine, tool and decorative crafting conflicts are separate.

## Player behavior

| Input / station | Result |
| --- | --- |
| Two Popcorn + one Caramel, crafting | One Caramel Popcorn |
| One Caramel Popcorn, crafting | One Popcorn Ball |
| One Marigold, crafting | Four Marigold Petals |
| Four Marigold Petals, crafting | One Orange Dye |
| One Cake + knife, cutting board | Seven Cake Slices |
| Seven Cake Slices, crafting | Four Cavern Cake Layers |
| One Pumpkin + knife, cutting board | Four Pumpkin Slices |
| Four Pumpkin Slices, crafting | Five Pumpkin Seeds |
| One Dead Bush + knife, cutting board | One Dead Branch |
| One Dead Branch + knife, cutting board | Two Sticks |
| Ordinary Egg, furnace / smoker / campfire | One Fried Egg |
| An egg accepted by `c:eggs`, Household Frying Pan | One Cooked Egg (`wildlife_cooked_egg`) |

The vanilla one-pumpkin-to-four-seeds crafting recipe remains available. Processing
all four slices retains the imported five-seed route. Four petals still represent
one flower, and seven slices still represent one cake: the intermediate steps do
not multiply dye, seed or cake-layer yields. Popcorn ball ingredients and yield are
unchanged in total. Dead-bush processing adds a knife operation without increasing
the two-stick output. Existing food nutrition values remain unchanged.

The Household Frying Pan is distinct from the Farmhouse Kitchen Skillet and Stove.
Those use campfire cooking and still produce Fried Egg. The pan uses its existing
fuel requirements, cooks its egg in 200 ticks and retains 0.35 XP. Ordinary cooking
retains the existing 200/100/600-tick furnace/smoker/campfire times.

## Integration and compatibility

These are early kitchen/crafting routes using existing items and stations. Farm
produce supplies the ingredients; the results remain food, decoration or planting
materials. No new station, dependency, registry ID, research gate or save schema is
introduced. Both egg item IDs remain registered, so existing inventories are retained.

Sources are updated in `agriculture.py`, `survival_recipe_completion.py`,
`household_content.py` and `menagerie_content.py`. The household importer changes
the pumpkin operation to crafting and the stick input to a branch. The wildlife
importer moves its egg recipe to the pan and removes its stale smoker/campfire
recipes when rerun. Original owner-library files are untouched.

The cake-layer recipe now unlocks from cake slices through the generated survival
recipe advancement. JEI uses vanilla integration for the new crafting steps and
the refreshed station catalog for the pan and cutting recipes. Server datapack
overrides still require their own collision checks; the JEI catalog describes
bundled defaults, as documented in [JEI integration](jei-integration.md).

## Validation

The regression suite checks generation from authored/imported sources, unchanged
whole-ingredient yields, removal of stale egg recipes, and the station catalog.
The dedicated `FoodRecipeConflictGameTests` scan all loaded crafting, cutting and
cooking recipes, rather than only looking up the expected recipe ID. They check
unique matches, exact output quantities, wrong tools, insufficient single-piece
inputs, ordinary vanilla pumpkin crafting and each egg cooking station.

The local collision rescan removed all eight conflicting pairs in these six groups
(egg cooking accounts for three pairs), with no new overlaps. Remaining conflicts
from the wider audit are outside this food/cutting batch.

Passed locally on 23 October 2026 with Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3 and Java 25.0.4.1+1 (unchanged dependency pins):

- `runGameTest --offline --console=plain -I ../food-conflict-tests.init.gradle`:
  all three `FoodRecipeConflictGameTests` passed in a disposable server world.
  The local init script sets `fabric-api.gametest.filter` to
  `*:food_recipe_conflict_game_tests*`; it does not alter production behavior.
  Common, client and game-test compilation passed; the complete run took 9m40s,
  mostly preparing resources in the isolated checkout.
- `python -m unittest scripts.tests.test_food_recipe_conflicts scripts.tests.test_jei_stations -v`:
  nine tests passed. The four source-adapter tests also passed after the final
  defensive handling for non-object imported outputs.
- `python -m unittest scripts.tests.test_survival_recipes -v`: five tests passed,
  including the repository acquisition graph.
- `python scripts/check_repository.py` and `git diff --check`: passed.
- `python tools/check_mod_data.py`: passed with the cached DejaVu font directory
  supplied through `JUGCRAFT_FONT_DIR`; 6,240 material IDs, 13,187 models, 3,729
  quad parts and 176 combined models checked.
- The collision audit compared local recipes with the pinned vanilla recipe data,
  resolving ingredient tags and checking shaped/shapeless overlaps; all eight
  targeted conflicting pairs disappeared, with no newly introduced pairs.

This work does not claim a two-client multiplayer test, production-world
installation or a full server/client regression suite.
