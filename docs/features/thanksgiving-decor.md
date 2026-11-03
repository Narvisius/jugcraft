# Thanksgiving decorations (groups D–G)

Status: implemented locally, not yet compiled or played (resources, Java and offline checks done; see Verification)
Proposal issue: none; the owner's request (groups D–G of the Thanksgiving festival) is the scope approval.
Owner: the project owner (request).

Implemented with Claude Code.
Target milestone and tier: seasonal decoration, no tier; craftable from the first day.
Primary specialty and supported player role: building and decorating (farm, market, home, feast table, town square).

The owner approved a list of 32 harvest-market, feast-table, interior and statement decorations and asked that it first
be checked against what Jugcraft already has, so nothing is added twice. This record is that inventory, what was built
for the entries still missing, and how it was checked. The festival's other phases are in
[thanksgiving-festival-nov1.md](thanksgiving-festival-nov1.md); the town-square cornucopia/city transition (its phase 6)
is not part of this work and no city block is changed here.

## Player experience

Nineteen new placeable decorations and a new Feast Table cloth, all crafted from things a farm grows or a starting player
has (sticks, wheat, produce, clay, iron and copper), with no research and no calendar gate. Each floor piece faces the
player who places it, breaks quickly with its tool (or by hand) and drops itself once. The wall pieces hang on the side of
a solid block and fall when it goes; the mobile hangs from a ceiling; the lantern stands or hangs like vanilla's.

- **Autumn Leaf Wall Garland:** garlands hung side by side on one wall join into one swag; only the two ends of a row are
  tied off with a bow and a hanging tail of leaves (`left`/`right` states, worked out from the two neighbours only).
- **Pumpkin Candle Centerpiece:** flint and steel or a fire charge lights its three candles (light 12); an empty hand
  snuffs them, as the Hurricane Lamp.
- **Autumn Leaf Table Runner:** used on a Harvest Feast Table length it lays a hessian runner strewn with maple and oak
  leaves and acorns, draping over the table's open ends, on tables of any length; swapping cloths or shears give it back
  without touching the dishes, and it drops with the length.
- Everything else is a still prop with its own outline (no full-block hitbox on a small piece).

### The inventory: the approved list against existing content

"New" entries are implemented by this work. "Covered" entries already exist and were not duplicated; the reason is given.

| # | Approved entry | Status | Existing ID or new ID, and why |
|---|---|---|---|
| D1 | Stacked pumpkin-and-gourd arrangement | New | `pumpkin_and_gourd_stack`. Existing stacks are carved, lit jack-o'-lanterns (`mini_pumpkin_stack`), plain pumpkins only (`holidayworks_three_pumpkins`, `holidayworks_pair_of_pumpkins`) or a 3×1×3 market display (`pumpkin_pyramid`); none is an uncarved pumpkin, white pumpkin, squash and gourd heap. |
| D2 | Wicker apple basket | New | `wicker_apple_basket`. Apples exist in crates (`red_apple_crate` etc.), a bin, a picking bag and loose (`windfall_apples`); `wooden_basket` is an empty storage basket. |
| D3 | Pear basket | New | `pear_basket` (a shallow trug); nothing displayed pears. |
| D4 | Root-vegetable hamper | New | `root_vegetable_hamper`. Single-crop crates exist (`potato_crate`, `carrot_crate`, `beetroot_crate`, `onion_crate`); no mixed, lidded hamper. |
| D5 | Corn bundle | Covered | `ornamental_corn_bundle` (a tied bundle of ornamental corn) and `corn_shock` (two-block standing bundle of stalks). |
| D6 | Tied wheat sheaf | Covered | `wheat_stook` (sheaves stood together, one block); the Giant Cornucopia also carries a tied sheaf. A second sheaf prop would duplicate it. |
| D7 | Braided hanging corn | Covered | `ornamental_corn_bundle` is the hanging braid of husked corn on a wall or door (`WallDecorationBlock`); `corn_dolly` is the woven wheat hanging. |
| D8 | Harvest produce crate | Covered | `pumpkin_crate` (holds four produce), the orchard's apple crates and the produce crates (`corn_crate`, `potato_crate`, `cabbage_crate`, `tomato_crate`, …). |
| E1 | Autumn leaf table runner (expanding tables) | New | item `autumn_leaf_table_runner`, Feast Table `runner=leaves`. The table already joins lengths without limit and swaps cloths (harvest, bats, linen); the leaf runner is a fourth cloth on that same mechanism. The table's own orange runner has printed leaves; the new one is a hessian runner with real leaves and acorns on it. |
| E2 | Harvest place setting | New | `harvest_place_setting`. `floating_table_setting` is the Halloween floating, bobbing setting with a candlestick. |
| E3 | Napkin/cutlery holder | New | `napkin_and_cutlery_caddy`. |
| E4 | Bread basket | New | `bread_basket`. Breads can be set down as single servings, but there was no lined bread basket. |
| E5 | Cranberry serving bowl | Covered | `cranberry_sauce` already sets down as its own placed serving (sneak-use; `food_serving/jugcraft/cranberry_sauce`) and serves on Feast Tables; `cranberry_preserves` is a jar. |
| E6 | Gravy boat | New | `gravy_boat`. |
| E7 | Covered butter dish | New | `covered_butter_dish`. |
| E8 | Pie stand | New | `pie_stand`. Pies themselves place as pie blocks; there was no tiered stand. |
| F1 | Autumn wreath | Covered | `autumn_wreath` (four flower colours, hangs on doors) and `bittersweet_wreath`. |
| F2 | Leaf garland with connecting/end variants | New | `autumn_leaf_wall_garland`. `autumn_leaf_garland` is a strand strung between String Light Hooks and `mantel_garland` dresses the Stone Fireplace only; neither is a wall garland that joins its neighbours with tied ends. |
| F3 | Harvest bunting | Covered | `harvest_bunting` (maple, aspen and hessian pennants on the String Light Hooks). |
| F4 | Amber jar lantern | New | `amber_jar_lantern`. Existing lights are the clear `mason_jar_candle`, `hurricane_lamp`, `barn_lantern` and the owner's lightworks lanterns; none is a glowing amber jar. |
| F5 | Pumpkin candle centerpiece | New | `pumpkin_candle_centerpiece`. |
| F6 | Small hay-bale seat | Covered | `hay_bale_seat` (sittable), plus `homeworks_hay_cushion` and the holidayworks hay bales. |
| F7 | Harvest market sign | New | `harvest_market_sign`, a painted pictorial signboard on brackets. `chalkboard_sign` remains the sign for custom words. |
| F8 | Wall-mounted copper pan rack | New | `copper_pan_rack`. Only the owner's single `household_frying_pan` existed. |
| G1 | Friendly turkey statuette | New | `turkey_statuette`. Turkeys existed as the mob, `roast_turkey` and `giant_turkey_feast` (food), not a figure. |
| G2 | Harvest scarecrow | Covered | `scarecrow` (two blocks, dyeable shirt, wears a head, guards crops) and `holidayworks_scarecrow`. |
| G3 | Overflowing wheelbarrow | Covered | `pumpkin_wheelbarrow` (two blocks, holds five produce shown in it) and `holidayworks_pumpkin_wheelbarrow`, `holidayworks_hay_wheelbarrow`. |
| G4 | Apple cider barrel with tap | Covered | `cider_barrel` (the Cider Mill's working barrel on its cradle, with its brass tap). |
| G5 | Squash cart | Covered | `pumpkin_wagon` (2×2×4, holds 28 produce) and `orchard_handcart` (1×1×2). |
| G6 | Acorn-and-leaf mobile | New | `acorn_leaf_mobile`. |
| G7 | Squirrel-with-acorn ornament | New | `squirrel_acorn_figurine`, a small carved figure; `squirrel_plush` is a midway plush toy. |
| G8 | Smaller matching cornucopia | New | `table_cornucopia`, the Giant Cornucopia's willow weave, braided rim and harvest at one block. |

19 new blocks and 1 new item; 13 entries covered by existing content. The large entries (scarecrow, wheelbarrow, cider
barrel, squash cart) were all covered, so no new multi-block was needed.

## Connections
- Existing input producer: farm crops and produce (pumpkins, white pumpkins, gourds, squash, apples, pears, carrots,
  potatoes, beetroot, turnips, corn, butter, dinner rolls, pies), sticks, wheat, clay, iron and copper.
- Existing output consumer: none; they are decorations. The runner extends the existing Harvest Feast Table.
- Technology connection: none required (copper and iron ingots only as materials).
- Magic connection: none.
- Reachable entry path (prove no circular unlock): every recipe is a shapeless crafting-table recipe of reachable items;
  none needs research, and none needs another new decoration.
- Which connections are required vs optional; trade and solo routes: all solo-craftable.
- How this specialty stays useful without mastering every other branch: decoration only.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: cosmetic blocks; they use the
  existing decoration blocks, the Feast Table's cloth mechanism and vanilla's lantern.

## Balance and automation
Recipes (all shapeless, agriculture feature switch, category building; `tools/thanksgiving_decor.py` `SHAPELESS`): the
baskets and hamper take two sticks and wheat for the willow plus three of their produce; the stack takes its pumpkins,
gourd, squash and wheat; the china pieces take clay (the gravy boat and butter dish with bone meal, the place setting with iron cutlery and flax); the caddy, pie stand and centerpiece wooden slabs;
the copper pan rack three copper ingots; the garland makes three. Nothing is returned on breaking but the block itself,
so there is no conversion loop. No automation, ticking or block entities (the centerpiece's `lit` and the garland's
joins are block states, changed only by use and neighbour updates).

## Multiplayer and persistence
Plain block states; no saved data beyond them. New IDs only; no existing ID or saved state is renamed. The Feast Table's
`runner` property gains the value `leaves` after `linen`; it is saved by name, so existing tables keep their cloth.
Placement, lighting and cloth changes go through the normal block-use path, so the existing land and town protection
applies. Disabling the agriculture feature removes the recipes, never the registrations.

## Dependencies and assets
No new dependencies. All textures are drawn by code in `tools/thanksgiving_decor_data.py` (one packed texture a prop,
one texel a model pixel, leaves as cut-out pixel maps) and the runner icon is the 16×16 map
`tools/item_icons/autumn_leaf_table_runner.txt`; no Mojang texture is read. The baskets and the Table Cornucopia reuse the
Harvest Festival Grounds' willow, braid, produce, corn, wheat and grape painters so they match the Giant Cornucopia.
The [owner library catalog](../../art/owner-library/catalog/README.md) was searched for baskets, hampers, cornucopias,
gravy boats, pan racks, garlands, wreaths, lanterns, turkeys, acorns, squirrels and wicker: it holds supplementaries'
wicker fence texture, creature textures (turkeys, acorn items) and the holidayworks decorations already integrated, but
no table-scale harvest props or textures to adapt, so nothing was copied from it.

Generated resources: block/item models, blockstates, items, loot tables, recipes, names (`en_us.json`), the
`mineable/axe` and `mineable/pickaxe` tags and textures, all through the agriculture pipeline (`agriculture.py`,
`agriculture_data.py`, `crop_textures.py`) and the Feast Table's `feast_data.py`.

## Verification
Run in this environment (offline; no Minecraft):
- The agriculture generators' own functions reproduce every committed file of these props and the Feast Table exactly
  (`agriculture_data.assets`, `loot`, `recipes` compared with the files on disk: 142 files; `crop_textures` against the
  22 new textures: no difference). The whole-repository `tools/generate_material_data.py` could not finish here (its
  `newest_shapes.py` step needs pinned vanilla files), so the shared files (names, mining tags, resource manifest) were
  merged in place by the same functions.
- `tools/art_check.py`'s block-model rules (Z1, N1, U1, O1, I1) on the 40 distinct new and changed models: no finding.
- `python3 tools/check_mod_data.py` (full run, November 3): the new `check_thanksgiving_decor` and the updated Feast
  Table runner check pass; it still exits 1, with exactly the 48 O1 findings it reports on a clean checkout (owner
  `newest/` imported models), none new.
- `python3 scripts/check_repository.py`: pass. `python3 tools/check_icon_maps.py`: pass (its warnings are the
  pre-existing legacy-list ones). `scripts/tests` `test_feast_presentation`, `test_harvest_festival`,
  `test_generated_resource_cleanup`, `test_food_recipe_conflicts`: pass. `tools/audit_recipe_overlaps.py`: no overlap
  involving the new recipes (vanilla tags cannot be resolved offline).
- Offline renders of every model at several angles and all four facings, beside existing decorations for scale.
- New server game tests `ThanksgivingDecorGameTests` (placement/facing/drop for every floor prop, the candles, the
  garland joining and falling, the lantern, the leaf runner on a Feast Table) are written but **not run**: Java
  compilation and game tests need the Fabric toolchain, which was unreachable here.

Not run: Java compilation, server and client game tests, in-game visual review, two-client play. These must pass before
release.

## World and event applicability
Seasonal in theme only: nothing is gated on the calendar, nothing spawns or changes in the world, and earned decorations
keep working all year. Existing Halloween transitions and the town decoration schedule are untouched; no city block,
town template or owner placement (church slot machines included) is changed. No opt-in arrangement template was made.

## Rollout and open questions
- Release needs a compile, `./gradlew build` (server game tests) and a client look at the new models in the game.
- Alchemy essence values for the new items fall back to the documented one-essence default until
  `tools/build_alchemy_values.py` is rerun against the game JAR.
- Owner art review is welcome; the models are deliberately chunky (one texel a pixel) to match the harvest festival set.
- Not done: an optional decorative arrangement/showroom template for the town (left to the city phase 6).
