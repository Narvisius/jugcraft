# Placed-food remodels: eight plated meals sculpted on real dishware

Status: implemented in source; **not yet seen in game**. Offline renders and the repository's offline checks only (see Verification).
Proposal issue: none; requested directly by the owner (Group C, "High-quality placed-food remodels, all eight").
Owner: @jimbozoomer-byte
Target milestone and tier: unchanged from [the menu](the-menu.md) (Milestone 3, Discovery tier); this is an art change only.
Primary specialty and supported player role: cooking; builders dressing kitchens, dining rooms and market stalls.

Implemented with Claude Code.

On 3 November 2026 the owner made this way of building food the rule for every future dish: see [FOOD_MODELS.md](../FOOD_MODELS.md).

## Player experience
Eight of [the menu](the-menu.md)'s dishes, set down with a sneaking use, were a flat tray or plate of coloured sticks and
squares (the old template sampled the inventory icon's colours). They are now the food itself, built piece by piece in
the owner's palette at vanilla's 16 x 16 density, in the manner of the Shepherd's Pie, Gleaming Salad and Nachos feasts:

| Dish (ID) | Dishware | The food |
| --- | --- | --- |
| Cornbread Stuffing (`cornbread_stuffing`) | the owner's cast-iron Skillet, handle to the back | a bed of crumbs with toasted cornbread cubes of every size, three turned askew, cranberries, sage leaves and a mushroom slice |
| Vegetable Noodles (`vegetable_noodles`) | a flared wooden bowl, lined inside | wavy noodles heaped in the middle with strands draped over them; carrot batons and red pepper strips laid at angles, cabbage, onion and rings of green onion |
| Squid Ink Pasta (`squid_ink_pasta`) | the glazed cream-and-blue plate, so the dark pasta reads | ink-dark spaghetti twirled into a nest (each round turned further than the one below), lit strands wound round it and loose on the plate; cherry tomato halves, curls of squid, parsley |
| Bacon and Eggs (`bacon_and_eggs`) | the wooden plate | two strips of bacon (lean and fat rows) that rise and dip, a raised piece arching clear of the plate; two fried eggs with round, crisp-edged whites and raised yolks |
| Steak and Potatoes (`steak_and_potatoes`) | the wooden plate | a thick steak with its fat cap and grill-marked crown, cut across its front to show pink, a slice lying in front; mashed potato with a pat of butter, two golden roast potatoes and parsley (the sides of the owner's icon) |
| Roasted Mutton Chops (`roasted_mutton_chops`) | the wooden plate | three chops with round eyes of meat and a roasted fat edge, their bones rising over the back of the plate to knuckles; roast potatoes, red onion and a rosemary sprig |
| Pasta with Meatballs (`pasta_with_meatballs`) | the glazed plate | a mound of spaghetti with strands draped over it, sauce pooled on top, three rounded meatballs nestled in it with sauce on them, basil and grated cheese |
| Tomato Sauce (`tomato_sauce`) | a flared wooden bowl, lined inside, the sauce capping it below the rim | stirred sauce, two basil leaves and a wooden spoon standing in it, its handle leaning on the rim |

The ID "Mutton Chops" in the request was resolved to `roasted_mutton_chops`, the plated meal with dishware (the
language file's `block.jugcraft.roasted_mutton_chops`). The ingredient items `mutton_chops` and `cooked_mutton_chops`
(placed through the generic food servings) and `pasta_with_mutton_chop` are unchanged.

Nothing else changed: the IDs, blockstates (four facings, no servings), recipes, food values, eating, setting down,
taking back, drops and the flat item icons are as they were. The blocks have no servings property, so each keeps one
model (no portion stages were added; adding them would need Java and blockstate changes). Every model stays inside its
block's outline (`PlacedDishBlock.DishShape`: PLATE 1..15 across and 5.5 high, BOWL 3..13 and 6.5) and the particle
texture is still the owner's icon.

## Connections
- Existing input producer: unchanged (Cooking Pot and crafting recipes in `tools/menu.py`).
- Existing output consumer: unchanged (the player eats the dish; placing and breaking are as before).
- Technology connection: none (cosmetic).
- Magic connection: none (cosmetic).
- Reachable entry path (prove no circular unlock): unchanged.
- Which connections are required vs optional; trade and solo routes: unchanged.
- How this specialty stays useful without mastering every other branch: unchanged.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: block models and textures only; no gameplay data changed.

## Balance and automation
No change. Recipes, hunger and saturation, bowl return and loot are generated from the unchanged `tools/menu.py`.

## Multiplayer and persistence
No change. Same block IDs and states, so placed dishes in existing worlds keep working and show the new models.

## Dependencies and assets
- Source of truth: `tools/menu_meal_models.py` (the dishes' pieces and dishware) and `tools/menu_meal_textures.py` (their
  textures, drawn by code). `tools/menu_data.py` `dish_model` sends these eight there; every other placed dish still uses
  its template in `tools/feast_meal_models.py`, whose now-unreachable branches for these eight were removed (no other
  model's output changed). `tools/generate_textures.py` draws the textures; `tools/generate_material_data.py` writes the
  models through `menu_data`, so a regeneration reproduces both.
- How the textures fit the models: each meal wears `<dish>_top` (the dish seen from above, one texel a model pixel, read by
  position by every upward face) and `<dish>_side` (each material's rows seen from the side, plus small swatches for the
  tops of turned pieces), like the owner's `shepherds_pie_top`/`_side`. The top drawing is rasterised from the model's
  own pieces (each piece's material, its north and west edges lit and south and east edges shaded, as the owner draws),
  then a short grid of placed details (grill marks, glints, sauce dabs, herb flecks), so drawing and geometry always agree.
- New textures (16 x 16, opaque, drawn by code, deterministic): `textures/block/<dish>_top.png` and `<dish>_side.png` for
  the eight dishes, and the shared dishware `meal_wood_plate_top`, `meal_wood_side` (plate and bowls),
  `meal_glazed_plate_top` and `meal_glazed_side`. No other generator writes these names.
- Palettes: written out as hex in `tools/menu_meal_textures.py`, taken from the owner's library drawings of the same food
  in `art/owner-library/originals/Blocks/farming and food textures/` (the eight dishes' icons, `cooked_bacon.png`,
  `fried_egg.png`, `cornbread.png`, `raw_pasta.png`, `cooked_mutton_chops.png`, `beef_patty.png`, `tomato.png`,
  `cabbage_leaf.png`; the woods from `bowl.png` and `platter.png`). No library file is copied, read at runtime or changed.
  The glazed plate takes the cream and blue of Jugcraft's own Plate (`tools/feasts_textures.py`), its large whites kept
  under 235.
- Reused runtime textures: the owner's Skillet (`block/skillet_top`, `skillet_side`, `skillet_bottom`, already imported
  by `tools/owner_art.py` from the library's `skillet_*.png`), unchanged.
- No new dependency.

## Verification
What actually ran (offline, no game client):
- Targeted regeneration through the repository's own writers (`menu_data.assets` with `generate_material_data.write`,
  `menu_meal_textures.draw_all` with `generate_textures.save`): only the eight models change; the other 65 placed-dish
  models and every blockstate regenerate byte-identically; a second run is byte-identical (idempotent).
- `tools/art_check.py`'s block-model rules (I1, U1, Z1, N1, O1) on the eight models: no findings. A face-burial probe
  finds no face wholly inside other boxes (they are dropped, `menu_meal_models.drop_buried`), and no element is left
  without faces.
- Every model, including its turned pieces, stays inside its block outline; 21 to 33 elements each.
- Offline renders (back faces culled, every face shaded as the game shades it) at normal game scale from four sides, up
  close, from below, and turned through all four facings, beside the reference feasts.
- `python3 scripts/check_repository.py`: passes.
- `python3 tools/check_mod_data.py`: exits 1, with exactly the same 48 findings (O1 holes in imported `newest/` models)
  as on the untouched base commit; nothing new from this change.
- `python3 tools/generate_material_data.py` (in a copy of the tree): stops at `tools/newest_shapes.py`, which needs
  pinned vanilla model files the environment cannot download, after the menu's models are written; up to there it
  writes the eight models byte-for-byte as committed and leaves the other menu models unchanged.

Not run: Gradle compilation, server or client game tests, and looking at the dishes in game (the build's Maven
repositories are unreachable from the environment this was made in).

## World and event applicability
Not applicable: decorative models of existing foods.

## Rollout and open questions
- To check in game: the dishes at normal distance and up close, each facing, and from below (on a glass block).
- The glazed plate (Squid Ink Pasta, Pasta with Meatballs) departs from the owner's icons, which show every meal on a
  wooden plate, so the dark squid-ink pasta reads; switch `MEALS[...]["dish"]` to `wood_plate` if the owner prefers wood.
- Turned pieces (bones, strands, the spoon) use 22.5 and 45 degree turns, which vanilla model rotation allows.
- Reversible: restoring the old look is removing the eight entries from `menu_meal_models.MEALS` with the old
  `feast_meal_models.py` branches; no saved data depends on the models.
