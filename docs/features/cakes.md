# Cakes: seven cakes from the owner's drawing

Status: implemented in source; **not yet played**. The Build workflow compiles it; see Verification for what has actually run.
Proposal issue: none; requested directly by the owner on 8 October 2026, who shared a page of cakes they drew ("Here are some cakes I would like made that I drew"; the page: [art/owner-library/drawings/cakes_and_bakes.png](../../art/owner-library/drawings/cakes_and_bakes.png)). Asked how to go on, the owner chose to have them rebuilt from the drawing ("Rebuild from this picture") and baked in the Hearth Oven ("Bake in Hearth Oven"): baked like the pies, set down whole and eaten or cut a quarter at a time as the drawing's INTERIOR view shows. Part of the [kitchen and cooking expansion](../branches/AGRICULTURE.md#the-kitchen-and-cooking-expansion-planned), before the owner's milkshakes and pies and tarts, which wait for the fruit crops they need.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. Nothing needs a station but the Hearth Oven the pies already use.
Primary specialty and supported player role: cooking; supports farmers (carrots, apples, beetroot, sweet berries, wheat, eggs, milk) and decorators (the cakes are made to be set out)

## Player experience
Bake the owner's cakes and set them out:

- **Seven cakes:** Carrot Cake, Birthday Cake, Ice Cream Cake, Red Velvet Cake, Cheesecake, Coffee Cake and Apple Cake, each as the owner drew it: the carrot cake's eight carrots on cream frosting dripping down its sides, the birthday cake's eight striped candles and sprinkles, the ice cream cake's layers, the red velvet's white frosting over red, the cheesecake's square of berry jam, the coffee cake's layers and the apple cake's slices of green apple.
- **Baking:** Cake Batter (two wheat, an egg, a sugar and a bucket of milk, which comes back) with a sugar and the cake's own two ingredients makes its raw cake. The Hearth Oven bakes a raw cake as it bakes a raw pie, drawn inside as a square of batter in its tin turning to the cake's colour; left in too long it comes out a **Burnt Cake**.

| Cake | Its own ingredients |
| --- | --- |
| Carrot Cake | two carrots |
| Birthday Cake | a candle and pink dye |
| Ice Cream Cake | a snowball and cocoa beans |
| Red Velvet Cake | cocoa beans and a beetroot |
| Cheesecake | a bucket of milk and sweet berries |
| Coffee Cake | Coffee Beans and cocoa beans |
| Apple Cake | two apples |

- **Set down whole,** a block wide and nine texels tall, its front to whoever set it down. A hungry player eats a quarter of it; a knife cuts a quarter off as a **slice** to take away. The front right quarter goes first, showing the cake's inside as the owner's INTERIOR drawing does, then the front left, the back left and the back right; the last quarter takes the cake. Only a whole cake can be picked up again.
- **Inside:** the carrot cake's layers are the owner's own INTERIOR drawing; the layered cakes show their layers, the frosted ones a layer of their cream through the sponge, and the birthday cake a vanilla sponge in pink cream.

| **The display:** the seven cakes and the Burnt Cake whole on the lower shelf and cut on the upper (right to left: carrot, birthday, ice cream, red velvet, cheesecake, coffee, apple, burnt) | **As the owner drew it:** the carrot cake whole and cut, seen from above its front right |
| --- | --- |
| ![The cake display](../images/ingame_cakes.jpg) | ![The carrot cake whole and cut](../images/ingame_cakes_drawn.jpg) |
| **Baking:** Hearth Ovens with a cake inside: burnt (birthday), baked (apple) and raw (red velvet), left to right | **The wall:** Cake Batter, the raw cakes, the slices and the Burnt Cake in item frames |
| ![The ovens](../images/ingame_cake_ovens.jpg) | ![The items](../images/ingame_cake_items.jpg) |

*In-game screenshots from CI's client game test (`CakeClientGameTests`, software rendering, small previews).*

## Connections
- Existing input producer: wheat, eggs, sugar and milk for the batter; carrots, apples, beetroot, sweet berries, cocoa, snowballs, candles, pink dye and Coffee Beans (the [fruit crops'](fruit-crops.md)) for the cakes; the Hearth Oven and its fuel.
- Existing output consumer: the slices are foods (`c:foods`, `c:foods/cake`); the cakes set out on the feasts' tables, as the pies do.
- Technology connection: the raw cakes are data recipes, ready for the engineered kitchen (slice 10) to mix.
- Magic connection: none.
- Reachable entry path: everything a cake needs is vanilla or grown and made at Discovery tier (the Hearth Oven, coffee from seeds in short grass); no circular unlock.
- Required vs optional: all optional, food and decoration.
- How this stays useful without other branches: a cook needs only a farm and an oven.

## Balance and automation
Units are hunger points (docs/BALANCE.md); the numbers are in `tools/cakes.py`.

| Cake | A quarter gives (saturation modifier) | Whole cake |
| --- | --- | --- |
| Carrot, Birthday, Ice Cream, Red Velvet, Coffee, Apple | 4 (0.6), as a pie's slice | 16 |
| Cheesecake | 4 (0.7) | 16 |
| Burnt Cake | 1 (0.1), and one time in three Hunger, as a burnt pie | 4 |

- A cake takes more than a pie (a batter of two wheat, an egg, a sugar and milk, then another sugar and two ingredients) and gives what a pie gives; vanilla's cake gives 14 over seven bites.
- **No loops:** nothing turns a cake or slice back into its ingredients. `tools/check_mod_data.py` follows every agriculture recipe and fails on a loop.
- **Baking time** is the pies' (`HearthOvenBlockEntity`: 600 points, a point a tick at 50 degrees or more, two at full heat; burnt at 1,200).

## Multiplayer and persistence
- All changes happen on the server; eating and cutting are block uses as a pie's are, so vanilla's reach check and the town's protection apply.
- Persistence: a cake's facing and the quarters eaten are block state; a Hearth Oven saves its cake as it saves a pie (by its place in `PieFilling`). The cakes are added at the end of `PieFilling`, after the pies, so a saved oven keeps its pie.
- Every ID is new; nothing existing is renamed or migrated.
- Disabling `agriculture` removes the recipes (load conditions) but keeps the registrations, so saved cakes, slices and batter survive.

## Dependencies and assets
No new dependency.

**Rebuilt from the owner's drawing.** The owner's page is kept as [art/owner-library/drawings/cakes_and_bakes.png](../../art/owner-library/drawings/cakes_and_bakes.png) (its bottom-left corner, where the owner had scribbled out some text, painted over with the page's grey; see that folder's README). `tools/cake_art.py` reads every texture of the cakes off it: for each cake, a projection fitted once to the outline the drawing gives it (16 texels square and 9 tall) puts every texel of the three faces the drawing shows onto the page, where it is read (the median of a small grid in the texel's middle) and the sides are lit back up (the drawing shades them as Minecraft does, the south face to 0.8 and the east to 0.6, and the game shades them again). The toppings were fitted to the drawing the same way and are boxes in `tools/cakes.py`; where they hide the frosting in the drawing, it is filled from the same place mirrored. The carrot cake's inside is read off the owner's INTERIOR drawing; the other cakes' insides, which the owner did not draw, are made from their own sides (`tools/cake_art.py` INSIDE says how). No Mojang texture is read.

From the owner's own library: each slice's icon is their **Slice of Cake** (`farming and food textures/cake_slice.png`) recoloured in the cake's own colours; **Cake Batter** is their cornbread batter (`farming and food textures/cornbread_batter.png`) recoloured to a vanilla batter. Jugcraft's own: the raw cakes' icons (a square tin of batter, flecked with what went in) and the Burnt Cake (the carrot cake charred).

The cakes' models face north, the drawing turned half round (`tools/cakes.py` turn), so the quarter the INTERIOR drawing cuts away first is the model's front right. Each quarter is its own box, with the cake's inside on its cut faces; the toppings stand on the quarters they lie on. If the owner draws any of this again, their files can replace these under the same IDs.

## Verification
CI (8 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `6fc6024` | Build, data audit, game tests, and every client game test class (the owner's drawing is a shared file, so all 104 ran) | Compiled; **953 of 954 game tests passed**, every cake test among them. `arms_viiigame_tests_harpoon_hauls_its_catch` failed ("The harpoon took 0.0, not 6.0 on tick 62"); the harpoon is not touched by this branch. **All 104 client classes passed**, `CakeClientGameTests` among them; the screenshots above are from this run |
| `6fc6024`, the `mod` job run again once | Build, data audit, game tests | **All 954 required game tests passed**, the harpoon's among them: its failure did not come back |

Run locally (8 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: also checks the cakes in `PieFilling` against `tools/cakes.py` (ID, slice food, colour, in order, after the pies), `CakeBlock`'s height and quarters, the registrations, every cake's (and the Burnt Cake's) models for each slice gone, its blockstate for each slice and facing, textures, words and loot (only while whole), that each topping stands inside one quarter, the raw cakes' and slices' words and textures, the recipes and the owner's page | Pass, 1742 IDs |
| `python3 tools/owner_art.py --check` | Pass |
| `python3 tools/generate_material_data.py`, then `git status` | Writes this slice's data only |
| `python3 tools/generate_textures.py` | Writes this slice's textures; it also rewrites the same 24 unrelated Styx textures as before, left as committed |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

The new game tests (`CakeGameTests`):
1. every raw cake goes into the Hearth Oven as its cake and every cake is a `CakeBlock`; at full heat a carrot cake bakes and comes out a carrot cake, and a birthday cake left in comes out a Burnt Cake;
2. set down by a player looking north, a cake faces south, towards them;
3. whichever way a cake faces, each slice takes the next quarter in order, the front right first, and a whole cake fills its block's footprint, nine texels tall;
4. a hungry player eats a quarter (four food), a knife cuts a slice to take away, the cake keeps its facing, and the last quarter takes the cake; only a whole cake drops itself; a Burnt Cake's quarter is one food;
5. Cake Batter's and every raw cake's recipes load, and every slice is a food.

The pies' tests cover the oven itself; `PieGameTests.pieDataLoads` also loads every raw cake's recipe, every `PieFilling` being checked there.

The client game test (`CakeClientGameTests`, CI job `client`) sets the seven cakes and the Burnt Cake out on a two-tier display, whole below and cut above, the carrot cake whole and cut at the angle the owner drew it from, three Hearth Ovens with a cake inside (raw, baked, burnt), and a wall of the items in item frames, and takes four screenshots.

Not done: play in a real client and a two-client dedicated-server session.

## World and event applicability
Food and decoration, for any time of year; the birthday cake's candles are part of its model and do not light.

## Rollout and open questions
- The owner's milkshakes and pies and tarts (their other pages) come after the new fruit crops they asked for first. The [fruit crops](fruit-crops.md) brought coffee, and the Coffee Cake now takes Coffee Beans where it took Mulling Spices. The [pies and tarts](pies-and-tarts.md) are built, set down by the same block at their own height, and so are the [milkshakes](milkshakes.md).
- Cakes do not sit on the feasts' platters or the Pantry Shelf; they stand on a table as a block.
