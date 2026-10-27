# JEI integration

Status: implemented; expanded during the owner's 22 October 2026 server bug-fix pass. Actual tool/model: OpenAI Codex (GPT-6). See the dated verification record below for the checks actually run. The plugin also compiles in CI with Concordance Chunk A's information pages (`addAcquisitionInfo`; #337's CI on `d4c07bf56` passed every job); Concordance Chunk B adds pages only through data.
Proposal issue: none. The owner asked for "JEI/EMI recipes" on 30 September 2026. EMI has no build for Minecraft 26.3 yet (checked on its maven), so this is JEI only.
Owner: @jimbozoomer-byte
Target milestone and tier: all tiers (it only displays recipes).
Primary specialty and supported player role: information and quality of life

## Player experience
With [JEI](https://github.com/mezz/JustEnoughItems) installed:
- every Jugcraft processing machine has its own recipe page, with inputs (tags show every matching item), the result, byproducts with their chances, and the time;
- the machine is listed as the "catalyst", so looking up a machine shows what it makes.

Machine coverage includes the item processors, fluid processors, industrial forms, starter workshop stations, and nuclear enrichment/fuel fabrication. The 22 October fix adds cooking, cutting, household appliances and furniture construction, hands-on stations, and public Jug Nouveau workshop recipes.

Crafting-table recipes show in JEI's own crafting page when JEI's recipe sync reaches the client: always in a single-player world, and on a server only if JEI is installed there too. A server without JEI sends none, and JEI falls back to vanilla's own recipes (read in JEI's 31.9 source; the pin is 31.8.0.49).

**Information pages** (JEI's Information tab) cover what no recipe shows: the alchemy ingredients' properties and the mortar, how a Resonant Ring is forged, and, since Concordance Chunk A, where the things only a ritual or a lair boss gives come from (the Adept's Wand, and the Rooted Thallite Foundation (Concordance Chunk C, C-04; not merged; built in CI, the pages not seen in a client), and Vesperine's and Madame Tatterlace's loot; the Yeti King's and the Cinder Tyrant's since Concordance Chunk B), with the default data's chances ([concordance-first-loadout.md](concordance-first-loadout.md)).

**The Mulch Maw's compost** (Concordance Chunk C, C-07; not merged; built in CI, the pages not seen in a client): every item a Mulch Maw eats has an information page with its worth in quarters of a nutrient, the highest of its Maw tags as the Maw reads them. Each tag's worth comes from `recipe_view.json` `garden_compost` (`tools/concordance_ecology.py` `compost_view()`, the worths `Garden.MULCH` gives the tags), and its items from the client's copy of the server's tags, so an item a data pack adds to a Maw tag is shown too; whether JEI registers these pages after the client has the server's tags is not confirmed ([arcane-concordance-ecology.md](arcane-concordance-ecology.md)).

## How it works
- **Optional:** the plugin compiles against JEI's API only (`compileOnly`, maven.blamejared.com). The JEI mod itself is an optional entry of the framework lock, which `build.gradle` loads at runtime in development and test runs unless `-PjugcraftOptionalIntegrations=false` (as CI's "optional integrations absent" job runs). The plugin class is reached only through JEI's `jei_mod_plugin` entrypoint, so Jugcraft runs normally without JEI.
- **Recipes:** these are server data in 26.x and are not sent to clients. The plugin reads `assets/jugcraft/recipe_view.json` instead, which `tools/recipe_view.py` generates from the same tables as the recipe data.
  - A data pack that changes machine recipes on a server is not reflected in JEI; nor is one that changes a lair boss's loot table or a ritual's data (the Adept's Attunement, and Root and Rivet (Concordance Chunk C, C-04; not merged)), which is why those information pages say "with the default data".

## Dependencies and assets
- JEI 31.8.0.49 for 26.3 (MIT): its API at compile time; the mod optional (`distribution/frameworks.lock.json`).
- No JEI code or assets are shipped.

## Verification
- `tools/check_mod_data.py` passes.
- The expanded client plugin compiled locally against the pinned JEI API on 22 October 2026.
- CI compiled the plugin against the JEI API, Concordance Chunk A's `addAcquisitionInfo` included (#337's CI on `d4c07bf56`, every job passed). Concordance Chunk B adds pages only through data (`recipe_view.json` rows and their words); the plugin is unchanged by it. Concordance Chunk C (C-07) adds `addCompostInfo`; it compiled in CI with unit 4 (run 38726242149, every job passed) and again in run 38774859294.
- `check_acquisition` (`tools/check_mod_data.py`) ties the information pages to `tools/acquisition.py` and the data on disk, and since Concordance Chunk B each boss's pages to its loot class's pools, first-victory trophy and payment through `LairRewards`. It reads data, and only the loot class's and the boss package's reward and deed lines; other drops coded in Java are not seen ([concordance-first-loadout.md](concordance-first-loadout.md), Known limits).
- The focused real-client JEI test and screenshots passed on 22 October 2026; this does not substitute for the owner's multiplayer playtest.

## Rollout and open questions
- Add EMI when it publishes a 26.3 build.
- Syncing server recipes to JEI (so data packs show) would need a small packet.

## Server recipe visibility fix — 22 October 2026

The owner's missing-food report was a viewer omission: the server already has 82 pot-cooking recipes, but the generated JEI catalog had no cooking-pot category. The audit also found 85 cutting recipes (including imported household recipes), 467 furniture construction recipes, 13 household appliance recipes across five types, and the two nuclear fuel processor categories absent from JEI. Ordinary crafting/smelting recipes remain owned by JEI's vanilla integration.

`tools/station_recipe_view.py` copies the finished bundled server recipes into the existing client catalog. It preserves ingredient alternatives, counts, all outputs, output components, tools and time. Household block variants share their corresponding category. The generator writes this view after recipe generation and stale-resource cleanup, so removed recipes cannot linger in the display. A new client-only station adapter uses grids for multi-ingredient recipes, with tool slots and operating notes. It does not change ingredients, yields, timers or station behavior.

The same audit adds 204 fixed public Nouveau workings across the imbuement chamber, enchanting apparatus, scribe's table and alteration table. Imbuement pedestal materials are shown as retained requirements, with its optional Ley speedup explained; apparatus recipes mention the required Arcane Core. These are distinct from Concordance's intentionally undisclosed alchemy formulas. In total the added generated section contains 851 recipes.

Stations implemented directly in Java use their runtime enums/constants: hearth oven pies/cakes, cider pressing and ageing, preserve sealing, wool spinning and knitwear unravelling, wax dipping and candy stages. Displayed sealed jars, colored yarn, candles and candy retain their components. Candy instructions distinguish pouring, setting, crystal growth and pulling; a lollipop information page explains the sticks/tray step. Cider press entries show batches of one through eight apples, including rounded pomace yields. Fuel/preheating and water requirements are explained without pretending they are consumed at a fixed rate per batch.

The electric furnace links to vanilla smelting; the kitchen stove, skillet and Halloween bonfire link to campfire recipes, with their different processing speeds explained in information pages. The cavern nuclear furnace links to blasting. Non-item-output operations (confection creature production and biome conversion) and dynamic magical transformations/melding receive information pages. Enchantment/equipment-dependent workings and named tome presets still use their station's authoritative working selector rather than a misleading generic fixed-output recipe. This is not a claim that every possible dynamic item variant has a JEI recipe.

Passed locally: all five Python regression tests; the final focused `JeiStationClientGameTests` run and JAR packaging; repository/documentation checks; the full data/recipe/art audit (6,240 material IDs, 13,187 block/item models, 3,729 quad parts and 176 combined models); and whitespace checks. The font directory was found in the local DejaVu 2.37 cache and supplied explicitly to the art audit. The regression suite checks every bundled custom processor type against the catalog (explicitly separating special crafting serializers), exact station ingredient/output/time parity, catalyst item validity, retained imbuement materials, and generated-view freshness. The real client verifies every generated row survived loading, recipe/output searches, catalysts, all new recipe layouts, and five screenshots; the cooking, cutting, furniture, candy and imbuement pages were visually inspected. Initial test compilation required correcting the harness to Minecraft 26.3's `client.gui.setScreen` API.

The normal resource task completed once; subsequent focused runs reused that resource output after explicitly copying the regenerated catalog. The final JAR's catalog was byte-compared with its source, its new client classes with compiled output, and its retained arrow/Stalker fixes checked. Existing viewer sections and machine rows remained identical; the only old-section additions are the two fuel machines. No full server suite or multiplayer playtest was rerun for this client display change. JEI remains optional and all adapter classes stay in the client source set. Server datapack overrides still are not synchronized into the static client view.

The standalone client also successfully opened and closed a world with `-PjugcraftOptionalIntegrations=false`. The test harness initially referenced JEI types in its entrypoint, so it was split into a dependency-free entrypoint and a guarded nested test; this correction affects only the test mod, which is excluded from the delivered JAR.
