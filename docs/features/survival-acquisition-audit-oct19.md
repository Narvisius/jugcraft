# Survival recipes and acquisition chains

**Current implementation:** 124 completion recipes, 50 added source pools across
both passes, and 77 repaired runtime container links. All 5,757 registered items
are classified; the active survival graph has zero unresolved routes or invalid
ingredients. The 78 previously deferred armor/weapon acquisition designs remain
explicitly excluded from that claim. See the follow-up section below and the
[source catalog](../audits/survival-base-sources.csv). The 43-recipe checkpoint and
342-entry review queue recorded below are historical; the follow-up supersedes them.

Owner request: 19 October 2026. Implementation: OpenAI Codex (GPT-6), local only.
The owner asked for recipes for things that should be made, while preserving boss
drops and loot as encounter rewards. This pass inventories the whole local item
set and repairs confirmed missing routes; an unresolved audit entry is not an
instruction to add a generic recipe for it.

## Initial 43-recipe checkpoint

43 explicit recipes live under `data/jugcraft/recipe/survival_completion` and are
maintained by `tools/survival_recipe_completion.py`. The owner data adapter invokes
that generator last, preserving the changes after a collection regeneration.
Original owner-library files are untouched.

| Branch | Starting inputs | Process and useful outputs |
| --- | --- | --- |
| Working kitchen | Iron + planks → existing furniture workbench; iron, copper, redstone, packed ice | Light/dark freezers in the construction catalogue. Existing freezing recipes remain cold-fueled; dark finish adds black dye. |
| Storage and garden | Paper + string; pale-oak leaves + sticks | Storage package on the crafting table; pale-oak hedge through the furniture workbench. |
| Insect keeping | Wooden slabs + glass panes; existing bug net | Empty habitat → capture a butterfly, dragonfly or pixie → variant-specific filled habitat → release and recover a habitat. Both existing empty habitat IDs work. |
| Animal food | Hunt turkey/deer; cooked cod | Turkey legs and venison cook in furnace/smoker/campfire. Cooked cod cuts into a fish fillet. Raw animal products are not crafted. |
| Coconut products | Harvest ripe coconuts; existing cutting board and knife | One coconut yields two slices and one husk. No duplicate shapeless shortcut or new processing machine. |
| Komodo gear reagent | Bond with a Komodo dragon; glass bottle; harvest cooldown | Bottle spit → cutting board → solid spit reagent **and returned bottle** → existing equipment recipes. |
| Slug processing | Bonded banana-slug production | Slime → furnace → crystallized mucus. |
| End carpentry | Collect purpur blocks | Purpur planks → slabs, stairs, walls, doors and trapdoors. Walls use extra sticks so their grid does not collide with trapdoors. |
| Boat fittings | Iron, sticks, chains and purpur planks | Anchor, winch, ship wheel and flag. These keep their existing dock/boat-control behavior. |
| Cave surveying | Book + two flints + five planks | Spelunkery Table → existing tablet + paper survey → saved cave map. A cave book, paper and ink also make the codex. |
| Underwater exploration | Existing ore/steel processing → five steel plates; two copper blocks, glass, electric motor | Submarine kit. Its existing redstone fuel and water movement remain unchanged. |
| Furnace alternative | Blast furnace + five iron ingots + three copper ingots | Single-block legacy Nuclear Furnace, which still uses ordinary blast-furnace recipes/fuel. This does not replace or bypass the separate uranium multiblock. |
| Cave food/building | Existing pastry dough, cake, gingerbread crumbs and leather armor | Dough block, cut cake layers and four gingerbread armor pieces. Rusty scrap → rebar → scaffolding. Scrap acquisition remains separately tracked. |
| Volcanic building | Basalt + stonecutter | Scoria → existing polished/bricked variants; fire bricks into cracked scoria. Ash-covered campfires still need their corresponding ash material. |

Vanilla crafting/cooking recipes also receive ingredient-triggered recipe-book
unlock advancements. Furniture construction and cutting keep their existing
station interfaces. The encyclopedia explains the new entry routes in the home,
creature and cave sections.

## Acquisition and integration repairs

- Six ordinary creature products now have bounded loot pools feeding existing
  gear/ammunition recipes: hammerhead shark tooth, frilled shark serrated tooth,
  cachalot tooth, giant-squid tentacle, snapping-turtle scute and crimson-mosquito
  blood sac. Each pool yields at most one; chances are 50%, except cachalot at 25%.
  Existing pools remain intact. No boss reward was made craftable.
- The Capsid's Daze conversion now uses Fabric's `c:music_discs`; the removed
  `minecraft:music_discs` tag previously left that conversion with no valid input.
- Insect capture formerly requested nonexistent IDs such as
  `fauna_bug_habitat_butterfly`. It now uses the actual color/species ID, validates
  the output before consuming a habitat, and preserves variant/animal identity.
- `companion_roly_pack` is an animal rendering asset, not a registered item. It
  receives no recipe. Roly Poly storage continues to use a chest on the animal.

## Whole-mod inventory and chain map

- [Sortable item inventory](../audits/survival-recipes.csv): one row per item,
  classification, recipe sources, unresolved prerequisites and acquisition evidence.
- [Full dependency graph](../audits/survival-recipes.json): AND requirements for
  separate ingredient slots, OR alternatives within tags, actual machine forms,
  fluids, workshop/station requirements and dynamic interaction adapters.
- The game-test registry snapshot excludes rendering-only model definitions from
  survival-item accounting. Refresh it when registrations change.

The graph reads vanilla and pinned Fabric item tags, standard recipes, industrial
recipes/capabilities, construction, cutting, brewing, Nouveau workshops, Capsid
conversions, Concordance workings/rituals, trades, tree stripping, fruit/crop
harvests, baking, candy making, spinning and explicit creature interactions.

`recipe_chain_reachable` means the item dependency path reaches ordinary vanilla
survival inputs under enabled features. `chain_with_acquisition_evidence` also
depends on documented loot, natural generation or a native interaction. Neither
status proves encounter frequency, energy throughput, research timing, world
placement or balance. Loot/configured-feature references are kept distinct from
fully specified crafting inputs. Counts and costs are in the linked recipe files;
the graph tests availability, not an economic or duplication proof.

An isolated item→block→item conversion cannot bootstrap a resource. Self-drops,
creative items and essence fallback values are not free graph sources. In
particular, teaching an item to the transmutation tablet is not its first survival
acquisition route.

## Remaining review queue

The current snapshot contains **5,757 registered Jugcraft items**:

| Classification | Items |
| --- | ---: |
| Recipe chain reaches ordinary survival inputs | 1,426 |
| Recipe chain also relies on acquisition evidence | 2,947 |
| Natural collection or native interaction evidence | 439 |
| Loot-only acquisition evidence | 242 |
| Intentionally noncraftable, including creative entries | 361 |
| No documented route yet | 297 |
| Recipe exists but an upstream source needs review | 45 |

The last two categories form a **342-item review queue**, not a claim that 342
crafting recipes are missing. Nine rendering-only definitions are excluded by the
actual game registry. Ingredient diagnostics are currently zero.

The CSV deliberately retains unresolved entries rather than classifying everything
as craftable. They include native interactions still needing an audit adapter,
unassigned rare gear, eggs/encounter tokens, naturally collected cave ingredients,
special liquid sources, and older workshop outputs. Some are working gameplay
whose acquisition is outside recipe JSON; others still need a real survival route.
The `blocked_by` column identifies the shared missing dependency to investigate
before adding downstream recipes.

Priority review: ecology seashell bootstrap; triops eggs and banana-related beast
food; cave heart of iron, rusty scrap, colored sweetish fish and Fusion fragments;
remaining knitting/baking/cauldron adapters; and unassigned special equipment.
Do not solve those by crafting a boss trophy or by enabling creative-only items.

## Reproduction and validation

1. `python tools/survival_recipe_completion.py` after the owner resource generators.
2. Run `SurvivalRecipeGameTests` in the existing Fabric server game-test task. It
   exercises real recipe reload, grid matching, selected output (including recipe
   collisions), stack limits, cutting byproducts, construction/cooking outputs,
   Capsid ingredients, and exports `survival-item-registry.json` in its run folder.
3. `python tools/audit_survival_recipes.py --registry <exported-registry.json> --write`
   refreshes the checked-in audit snapshot, JSON and CSV.
4. `python -m unittest discover -s scripts/tests -p test_survival_recipes.py -v`
   checks graph cycles, missing tags, alternatives, station dependencies and entry routes.
5. Run `MenagerieGameTests`, including the new twelve-variant habitat capture
   regression and the existing survival ownership/resource tests.

Initial real-game validation rejected a proposed recipe for the rendering-only
Roly Pack; that recipe was removed. The next run caught the purpur wall/trapdoor
grid collision; the wall recipe was corrected.

Final local results on 19 October 2026:

- `assemble`: passed, producing `build/libs/jugcraft-0.1.0-alpha.jar`.
- `SurvivalRecipeGameTests`: all 3 tests passed, including real matching and
  selected outputs for all 43 recipes, Capsid inputs and the registry export.
  Log: `../survival-recipes-gametest-oct19-3.log`.
- `MenagerieGameTests`: all 20 tests passed, including all 12 habitat variants.
  Log: `../survival-recipes-menagerie-oct19.log`.
- Python acquisition-graph regressions: all 5 tests passed.
  Log: `../survival-recipes-python-oct19-final.log`.
- Repository structure/documentation checks passed.
  Log: `../survival-recipes-repository-oct19.log`.

These are focused regressions; the full server suite was not rerun for this pass.
No human survival playtest or multiplayer capacity claim is implied. Nothing is
pushed or installed into an owner Modrinth profile.

## Follow-up: base resources and remaining survival chains

The second pass adds 81 explicit recipes (124 total in the completion generator)
and 44 named source pools, retaining the first pass's six animal-material pools.
It also connects native interactions that a recipe-only scan missed. The active
report is regenerated by `tools/audit_survival_recipes.py`; the figures above
describe the earlier checkpoint, not the current queue.

### Practical source-to-output routes

| Source | Supplies | Existing downstream uses |
| --- | --- | --- |
| Harvest shellstone, then use the stonecutter | Seashells | Ecology crafting and decorative shell chains |
| Existing orchard bananas, prepared at a cutting board | Beast banana, then pocket banana food variant | Creature food and pet feeding; preserves the existing banana-pup planting recipe |
| Breed caimans, platypuses, terrapins, triops and six cave reptiles | Their actual eggs | Native hatch/release paths; normal parent cooldowns and ownership checks |
| Adult Firebirds | Feather matching the bird's variant | Existing feather equipment recipes |
| Wildlife capture net and living starfish | Four saved-color starfish items | Release or use the captured item through existing recipes |
| Occupied natural ant hills | Queen ant via the existing state-sensitive drop | Ant-colony progression; empty hills do not generate queens |
| Leafcutter ants, cockroaches, Bunfungus, Enderiophages and Soul Vultures | Pupae, oothecae, spores, residue and soul hearts | Existing creature/material recipes |
| Magnetrons and cave encounters | Heart of Iron, Fusion fragments, bone worms and other encounter materials | Cave equipment, records and progression; boss loot stays encounter loot |
| Candy Cavity soda pools in new terrain | Purple soda source fluid | Bucket collection and existing soda recipes |
| Cave structures and landscape keeps/camps | Their assigned chest loot | Cave paintings, darts, maps, treasure rings and other original rewards |
| Existing mills, ovens, cauldrons and knitting tools | Baked goods, processed ingredients and yarn products | Full native recipes included in the graph with station prerequisites |
| Existing machines, ocean intake and air separation | Processed metals, chemicals and fluids | Technology chains retain their actual machines and source requirements |
| Styx, native alchemy and Nouveau interactions | Flowers, reagents, binding items and helper homes | Existing magical crafting and progression |

The [source catalog](../audits/survival-base-sources.csv) lists acquisition evidence,
processes, and direct downstream consumers. The [complete inventory](../audits/survival-recipes.csv)
also contains recipe and exclusion details. Vanilla prerequisites are explicitly
marked as assumptions. A recipe with several tag alternatives needs one valid
member per slot, not every member of the tag.

Current graph: 5,757 registered Jugcraft items; 1,520 ordinary-input recipe chains,
3,044 chains with acquisition evidence, 538 natural/interaction sources, 284
loot-only items, 78 deferred acquisition designs and 293 intentionally noncraftable
entries. There are zero unresolved active chains, invalid ingredient references or
missing Jugcraft loot-table links in referenced templates. Nine unused alternate
chest tables are reported separately and do not supply the graph. The source and
consumer catalog contains 3,631 rows, including fluid processes and explicitly
assumed vanilla prerequisites.

### Container fixes and persistence

Some cave templates had empty chests with no assigned loot. Their runtime copies
now point to the matching existing tables. Generated metal, rusty and gingerbread
barrels use the real cabinet inventory; the rusty barrel was previously decorative.
Cabinets now use vanilla randomizable-container handling so unopened treasure
survives saving, rolls once, and becomes an ordinary persistent inventory afterward.

Landscape keep/camp chests also contained legacy loot-table paths that no longer
existed. Those paths now point to the actual adapted tables, preserving their
original treasure rather than inventing a crafting shortcut. Supplied originals
and authored filled inventories remain unchanged. New template assignments affect
newly generated structures, not already placed containers in existing saves.

Unreferenced chest tables are excluded from acquisition evidence and listed
separately in the JSON report. Their mere existence no longer proves a source.

### Deliberate remaining work

- Acquisition design for **72 armor pieces and six matching weapons** remains
  deferred by the earlier owner instruction. These 78 entries are explicitly
  labeled `deferred_acquisition_design`, not claimed as survival-obtainable.
- Creative/debug/internal entries and the two legacy catfish bucket aliases remain
  separate from active survival content.
- Survival balancing, resource abundance, new-terrain encounter rates and long
  multiplayer sessions require owner playtesting. The graph is qualitative and
  does not prove that every production chain has enjoyable costs or timing.

### Final combined validation — 19 October 2026

- `assemble runGameTest`: **all 1,990 required server tests passed** on the final
  resources. Workspace log: `../survival-sources-full-oct19-final.log`.
- Real matching and selected outputs for **all 124 completion recipes**, including
  cutting, cooking, construction and stonecutting. Regression checks caught and
  corrected banana-pup and wool-hat grid collisions.
- **44 added source pools** resolve after the real reload and yield the intended
  registered rewards when their random gate is forced in the test fixture.
- **77 template containers** retain unopened loot across save/load, consume the
  assignment once, and cannot reroll after their inventory is emptied and reloaded.
- Soda-basin placement and source-to-bucket collection pass. Breeding tests cover
  ten new egg routes and ownership denial. Starfish tests capture and release all
  four actual variants; decorative placement remains supported. Firebirds produce
  one matching feather each and save their production cooldown.
- Five Python graph regressions pass, including zero unresolved active chains,
  invalid ingredients or missing Jugcraft loot-table links in referenced templates.
  Workspace log: `../survival-sources-python-oct19-final.log`.
- The source report was regenerated from this final game run's item registry.
  All 124 generated recipe JSON files match their generator definitions; source
  and container fixtures contain no duplicate cases.
- Offline data/model validation passes: 5,969 material IDs, 11,666 block/item
  models, 3,709 quad parts and 171 grouped model renders. Workspace log:
  `../survival-sources-data-oct19-final.log`. The final fedora pattern was additionally
  verified in the final game run after this offline check.
- Repository structure/documentation links pass. Workspace log:
  `../survival-sources-repository-oct19-final.log`.

Local artifact: `build/libs/jugcraft-0.1.0-alpha.jar`.
SHA-256: `9e714d713703bfa23a4e185411e86afb34c31b6570126ec7abb24a9bf34e0c61`.
This is automated server/data validation, not a human survival/balance playtest or
a new multiplayer capacity measurement. No owner Modrinth profile was changed.

Implementation and audit assistance: OpenAI Codex (GPT-6). Local only.
