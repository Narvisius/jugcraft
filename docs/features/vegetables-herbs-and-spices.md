# Vegetables, herbs and spices: salad and summer vegetables, kitchen herbs, spices and the cinnamon tree

Status: implemented in source; **not yet played**. The Build workflow compiles it; see Verification for what has actually run.
Proposal issue: none; requested directly by the owner. This is part b of slice 7 of the [kitchen and cooking expansion](../branches/AGRICULTURE.md#the-kitchen-and-cooking-expansion-planned). On 9 October 2026 the owner was shown the candidates and chose the recommendations ("go with the recommendations"): part a gave Jugcraft's garden crops the owner's own art ([garden-crops.md](garden-crops.md)), and part b adds new vegetables, herbs and spices drawn in Jugcraft's own style. The owner's library has none of these.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier. Everything grows from seeds found in the world, on the farmland, trellises and Cooking Pot a farm already has.
Primary specialty and supported player role: farming. It supports cooks (new dishes, and seasoning for the old ones) and traders (spices that grow wild in only a few biomes).

## Player experience
**Vegetables.**
- **Lettuce, Spinach, Radishes and Peas** grow on farmland as the onion and cabbage do: eight ages and four looks.
  - Lettuce and spinach are planted from their seeds, and give their leaves and seeds when ripe.
  - Radishes and peas are planted from themselves, as onions and beans are.
  - **Peas fix nitrogen**, as beans do, so the crops around them grow half as fast again.
- **Cucumbers** climb a trellis, as the tomato does, and hang their fruit in the upper trellis.
- **Eggplants and Zucchini** are bushes a block tall, as the pepper is. They flower and then fruit.
- A right-click picks a ripe cucumber, eggplant or zucchini, and the plant fruits again.
- A cucumber, eggplant or zucchini crafts into its seeds.
- Every vegetable grows wild, and its seeds also drop from short grass.
- **Dishes:**
  - **Roasted Eggplant** and **Grilled Zucchini**, cooked in a furnace, smoker or campfire.
  - A **Green Salad** of lettuce, spinach, cucumber and radish.
  - **Pea Soup**, from the Cooking Pot.
  - The menu's **Ratatouille** is now cooked from what a ratatouille is made of: tomato, pepper, onion, eggplant and zucchini. It used beetroot and garlic until now.

**Kitchen herbs.**
- The herbs are **basil, mint, rosemary, thyme, parsley, sage, dill and chives**.
- Each is planted from a sprig of itself, on farmland or in a Planter Box. It grows a block tall through four looks: sprouting, young, bushy and grown.
- Grown, a right-click cuts one to three sprigs, and the plant grows back from its bushy look.
- **Potted herbs:** a sprig used on a flower pot plants the grown herb in it, for a windowsill or a kitchen shelf.
- **Planter Box:** a wooden box of soil that holds its own water.
  - Any crop takes it as watered farmland, wherever it stands: on a balcony, a roof or indoors.
  - It is never trampled and never dries out.
  - It takes the place of farmland and a water source, and does no more than they do.
- **Drying:**
  - Four sprigs tied with a string make a **herb bundle**.
  - A bundle hangs from the underside of a block, such as a beam or a ceiling, and falls if that block goes.
  - It dries in time, turning a dusty grey-green.
  - Taken down dried, it gives four **Dried Herbs**. Taken down fresh, it gives the bundle back.
- **Dishes:**
  - Pesto Pasta (basil).
  - Sage and Onion Stuffing.
  - Herb-Roasted Mutton (rosemary and thyme).
  - Garden Herb Soup (parsley, chives and dill).
  - Herb-Roasted Potatoes (Dried Herbs).
  - **Mint Tea**, brewed in a glass bottle, which gives a short burst of Speed.
  - **Dill Pickles:** cucumbers with dill and mustard seed in cider vinegar, cooked into a Mason Jar, sealed and shelved as the other preserves are.

**Spices.**
- **Black pepper and vanilla** climb a trellis. Each is planted from what it gives: Peppercorns, or Vanilla Pods.
  - The pepper vine hangs clusters of peppercorns, green and then red.
  - The vanilla vine flowers pale yellow and then hangs long green pods.
- **Ginger, mustard and saffron** are low crops.
  - Ginger is planted from its root.
  - Mustard is planted from its seeds, and flowers yellow.
  - Saffron grows from a **Crocus Bulb**. Its purple crocus flowers with the red threads that are Saffron.
- **The Cinnamon Tree:**
  - It grows from its sapling, and wild in jungles.
  - An axe stripping one of its logs **peels off one or two Cinnamon**, as cinnamon is the inner bark. A stripped log gives no more.
  - Its logs are logs of its own; there is no cinnamon wood set. Either log crafts into four jungle planks.
- **Paprika:**
  - A chili pepper from the Kitchen Garden dries into a **Dried Chili** in a smoker, furnace or campfire.
  - The **Mortar and Pestle** grinds a Dried Chili into two Paprika. This is the mortar the alchemists already use: hold it, with the chili in the other hand. There is no second mortar.
- **Spice Rack:** a shallow wooden rack of two shelves, hung against a wall.
  - Used while holding a spice, it takes one. It holds eight, four to a shelf, each standing up facing the room.
  - With an empty hand, it gives the last one back.
  - A comparator reads how full it is.
  - The spices are the item tag `jugcraft:spices`: Peppercorns, Vanilla Pods, Ginger, Mustard Seeds, Saffron, Cinnamon, Paprika and Dried Herbs.
- **Dishes:**
  - Gingerbread Cookies, four to a batch.
  - Chicken Curry (rice, chicken, ginger, paprika and onion).
  - Saffron Rice.
  - Vanilla Custard.
  - Cinnamon and ginger also make the cider mill's **Mulling Spices**.

**Wild plants.** Every crop here grows wild in patches on grass. Broken, a wild plant gives one or two of its seeds.

| Biome | Wild plants |
| --- | --- |
| Plains | lettuce, spinach, radishes, zucchini, thyme, parsley, sage, dill, mustard |
| Flower fields | lettuce, dill, mustard |
| Forests | spinach, peas, parsley, chives |
| Hills | radishes, peas, rosemary, thyme, saffron crocus |
| Savannas | cucumbers, eggplant, zucchini, basil, rosemary, sage, saffron crocus |
| Jungles | cucumbers, eggplant, basil, black pepper, vanilla, ginger; the cinnamon tree, one tree in sixteen chunks |
| Rivers | mint |
| Swamps | mint, ginger |
| Taigas | chives |

| **Vegetables:** cucumbers climbing their trellises at the back, then the eggplant and zucchini bushes, then lettuce, spinach, radishes and peas, every age from left to right | **Herbs:** bundles hung from a beam, fresh and dried by turns; each herb grown in a Planter Box; rosemary and thyme through their stages; the eight herbs potted |
| --- | --- |
| ![The vegetables at every age](../images/ingame_vegetables.jpg) | ![The herbs](../images/ingame_herbs.jpg) |
| **Spices:** the black pepper and vanilla vines on their trellises, ginger, mustard (yellow) and the saffron crocus (purple) at every age, and a grown Cinnamon Tree | **Spice Racks:** one full and one part full, against a wall |
| ![The spice crops and the cinnamon tree](../images/ingame_spices.jpg) | ![Two spice racks](../images/ingame_spice_racks.jpg) |
| **Wild plants:** the twenty wild vegetables, herbs and spices | **The wall:** the 48 new items in item frames |
| ![The wild plants](../images/ingame_vegetables_herbs_spices_wild.jpg) | ![The items](../images/ingame_vegetables_herbs_spices_items.jpg) |

*In-game screenshots from CI's client game test (`VegetablesHerbsSpicesClientGameTests`, software rendering, small previews).*

## Connections
- **Existing input producers:**
  - Short grass (the vegetables' seeds) and the wild plants.
  - Farmland, water, trellises and bone meal.
  - The Kitchen Garden's chili pepper (paprika).
  - The Concordance's Mortar and Pestle.
  - The pantry's Mason Jars and Cider Vinegar (pickles).
  - Rice, raw pasta, the menu's Milk Bottle, and vanilla's chicken, mutton, potatoes, carrots, bread, eggs, sugar, string and planks.
- **Existing output consumers:**
  - The Cooking Pot, furnaces and campfires (the dishes).
  - The menu's Ratatouille, the cider mill's Mulling Spices, and the Pantry Shelf (Dill Pickles).
  - The composter: every crop, sprig and spice composts.
  - Vanilla animals:
    - Lettuce feeds rabbits.
    - Radishes feed pigs.
    - The new seeds feed chickens and parrots.
  - The hydroponic bay grows every new crop from its seed, as it does the others (its recipes are written from the same tables).
- **Technology connection:**
  - The Planter Box is the low-tech way to farm where there is no ground water, such as a rooftop, a balcony or a sealed base.
  - The hydroponic bay automates the same crops with power.
- **Magic connection:** the Concordance's Mortar and Pestle grinds paprika. Herbs are not alchemical ingredients here.
- **Reachable entry path:**
  - Every seed comes from the world: short grass, or wild plants in common biomes.
  - The cinnamon sapling comes from the wild tree's leaves.
  - Nothing here needs itself to be reached, so there is no circular unlock.
- **Required vs optional; trade and solo routes:** everything is optional.
  - Paprika is needed only for the curry. The Spice Rack is decoration.
  - A player far from jungles can trade for peppercorns, vanilla, ginger or cinnamon, or carry home a single seed, root or sapling and grow their own.
- **How this stays useful without other branches:** the vegetables and herbs feed a household on their own. The spices season it.

## Balance and automation
Units are hunger points, items and random ticks. The numbers are in `tools/vegetables.py`, `tools/herbs.py` and `tools/spices.py`, which are the record of truth for the Java constants; `tools/check_mod_data.py` compares them.

| What | Number | Why |
| --- | --- | --- |
| Raw vegetables | lettuce, cucumber, eggplant, zucchini 2 / 0.3; spinach, radish 1 / 0.6; peas 1 / 0.3 | Between vanilla's beetroot (1) and carrot (3) |
| Roasted Eggplant, Grilled Zucchini | 5 / 0.6 from one vegetable worth 2 | At most `COOK_BONUS` (3) over the raw vegetable |
| Green Salad | 8 / 0.6 from lettuce, spinach, cucumber and radish (6) | +2 |
| Pea Soup | 7 / 0.6 from two peas, an onion and a carrot (5) | +2 |
| Ratatouille | 8 / 0.6 (unchanged) from tomato, pepper, onion, eggplant and zucchini (9) | Its new ingredients are worth more than the dish |
| Herb dishes | Pesto Pasta 6 / 0.7, Stuffing 7 / 0.6, Herb-Roasted Mutton 10 / 0.8, Garden Herb Soup 6 / 0.6, Herb-Roasted Potatoes 12 / 0.8 | Herbs are worth 0, so each dish is its other ingredients plus at most 3 |
| Mint Tea | 2 / 0.3 and 30 s of Speed | A drink, as the cider mill's |
| Spice dishes | Gingerbread Cookie 2 / 0.1 (four a batch), Chicken Curry 10 / 0.8, Saffron Rice 6 / 0.7, Vanilla Custard 5 / 0.6 | Spices are worth 0 |
| Dill Pickles | four servings of 2 / 0.4 from four cucumbers (8) | No gain; a jar keeps, sealed |
| Picking | eggplant, zucchini, herbs 1-3; cucumber, peppercorns 2-4; vanilla 1-2 | Vanilla is the slow, precious one |
| Regrowth after picking | herbs back to age 4 (three stages to regrow); the others back to age 5 | Herbs are cut back hard and grow again |
| Growth time | herbs as wheat; cucumber, eggplant, zucchini 1.25×; black pepper and vanilla 1.5× | As the tomato, the pepper and coffee |
| Herb bundle | four sprigs and a string give four Dried Herbs; it dries 1 random tick in 5 (about six minutes) | Drying changes the herbs, not how many: the string is the loss |
| Cinnamon | 1-2 a log, once; either log crafts into 4 jungle planks | A tree's worth of logs is a stack of spice; the bark is gone once stripped |
| Paprika | a pepper (2 hunger) dries into a chili, and the chili grinds into 2 Paprika (worth 0) | A loss in food, a gain in seasoning |
| Planter Box | 7 planks, a water bucket (the bucket comes back) and dirt | Grows as one block of watered farmland, and no faster |
| Spice Rack | 6 sticks, 3 wooden slabs | Decoration and storage for 8 |
| Grass seeds | 7 vegetable seeds join the pool; one broken short grass still gives a Jugcraft seed 1 time in 8 | Adding crops does not make grass give more seeds |

- **Balance check:** `tools/check_mod_data.py` counts every dish against its ingredients, each at the most it could give. It fails any dish that gives more than `COOK_BONUS` over them, and any roasted vegetable that gives more than `COOK_BONUS` over the raw one.
- **No loops:**
  - A bundle gives back the sprigs that went into it, less the string.
  - A cinnamon log becomes planks, and planks never become logs.
  - A pea, radish, ginger root or herb sprig plants one plant, which gives one to four back over many minutes. That is farming, not a loop.
  - `tools/check_mod_data.py` follows every agriculture recipe and fails on a loop.
- **Bounded work:**
  - Crops grow only on the game's own random ticks. A dried bundle and a ripe herb stop ticking.
  - The Spice Rack does nothing on a tick. Its renderer draws at most eight items.
  - The cinnamon tree is a vanilla tree feature.

## Multiplayer and persistence
- **Server authority:** everything happens on the server.
  - Planting, picking, potting, stripping and racking are normal block uses, so vanilla's reach check and the town's protection apply.
  - The mortar grinds only on the server, at most once in eight ticks a player (the Concordance's own rate gate, shared with alchemy grinding).
  - The Spice Rack takes only items in `jugcraft:spices`, one at a time, up to eight.
- **Persistence:**
  - Crop ages and a bundle's dryness are block state.
  - A Spice Rack's spices are saved with the block and sent to clients to draw. Broken, the rack spills them.
- **Save compatibility:** every ID is new. The one change to an existing thing is the Ratatouille recipe's ingredients, keeping its ID `jugcraft:pot_cooking/ratatouille`. The Mortar and Pestle keeps its ID and gains a use. No migration is needed.
- **Disabling `agriculture`** removes the recipes and the wild patches but keeps the registrations, so saved crops, bundles, racks and items survive.
- **Disabling `concordance`** removes the Mortar and Pestle's recipe. Paprika, and so Chicken Curry, then cannot be made. Nothing else needs paprika.

## Dependencies and assets
No new dependency.

**Textures.** Every texture is drawn in Jugcraft's own style by code from fixed seeds. The owner chose this, as their library has none of these plants:
- `tools/vegetable_textures.py`: 58 (crop stages, vines, bushes, wild plants and items).
- `tools/herb_textures.py`: 66 (herb stages, sprigs, the bundles fresh and dried, the Planter Box, Dried Herbs and the herb dishes).
- `tools/spice_textures.py`: 56 (vines, crop stages, wild plants, the cinnamon sapling and leaves, the Spice Rack and the spice items and dishes).

**The cinnamon logs** are drawn by `tools/wood_style.py` in the same manner as every other wood: vertical furrows on the bark and square rings on the ends.
- They are in its `LOGS` table, for logs that are not a wood set.
- Their stripped side is the inner bark. That is the spice, so it takes the spice's colour, not one of the owner's wood paintings.
- The bark is a smooth grey-brown, as a cinnamon tree's is.

No Mojang texture is read, traced or recoloured.

**Generated data.** The models, blockstates, loot, tags, recipes and worldgen come from `tools/agriculture_data.py`, with `tools/herb_data.py` and `tools/spice_data.py` for the potted herbs, bundles, Planter Box, cinnamon tree and Spice Rack.

## Verification
CI (9 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `a77b0a6` | Build | **Did not compile:** `PushReaction.DESTROY` does not exist in 26.3 (the herb bundles' push reaction); it is `POPPED`, as the rest of the mod uses |
| `922135a` | Build, data audit, game tests, client game tests | Compiled; **all 1198 required game tests passed** (the 13 new ones among them, and `grassDropsJugcraftSeeds` counting the new seeds); the client test passed, but its first cameras stood too far off and the drying beam hid the herbs |
| `f8aa291` | The same, with the client test's cameras moved and a spice rack close-up | **All pass:** all 1198 required game tests and every client shard. The screenshots above are from this commit. |

Run locally (9 October 2026):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`. It also checks: the new `TallCrop` entries, crops, items, wild plants, wild patches (the cinnamon tree's too) and grass seeds against Java; `HerbBundleBlock.DRY_CHANCE`; that a bundle is tied from as many sprigs as it dries into, and its loot; the potted herbs' models and loot; the Planter Box's moisture, tags and blockstate; `CinnamonLogBlock`'s bark and the log it strips into; the tree feature's trunk and leaves; the cinnamon logs' planks and log tags; `SpiceRackBlockEntity`'s slots and tag; that the renderer has a spot for each slot; `jugcraft:spices` against the tables; `SpiceGrinding` against `GRIND`, and that the mortar calls it; the Ratatouille recipe; and every new dish's balance. Breaking any of these on purpose (13 mutations, from the drying chance to a roasted vegetable's food) makes it fail | Pass, 2022 IDs |
| `python3 tools/owner_art.py --check` | Pass |
| `python3 tools/generate_material_data.py`, `python3 tools/generate_textures.py`, then `git status` | Writes this slice's data and textures only |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); run by CI |

The new game tests (`VegetablesHerbsSpicesGameTests`):
1. Each low crop's seed plants it on farmland and is used up; ripe and broken, it gives its produce.
2. Ripe, the eggplant, the zucchini and each of the eight herbs is picked for its produce in its range, goes back to its regrowth age, and ripens again.
3. Cucumber, peppercorn and vanilla seeds plant nothing on bare farmland. On a trellis they plant their vine, which climbs into a second trellis and, ripe, is picked for its produce.
4. Lettuce beside peas grows faster than lettuce alone.
5. Lettuce seeds and a basil sprig plant in Planter Boxes. Lettuce in a box grows exactly as fast as on watered farmland, and faster than on dry.
6. Herb bundles:
   - a bundle cannot hang from nothing, and hangs under a block;
   - it dries with random ticks, and then stops ticking;
   - broken fresh, it gives itself; broken dried, it gives four Dried Herbs;
   - it falls when its block is taken away.
7. Four mint and a string tie a Mint Bundle. A rosemary sprig on a flower pot pots it and is used; broken, the pot gives back its pot and the sprig.
8. An iron axe strips a cinnamon log, wears one use and peels 1-2 Cinnamon. Used again on the stripped log, it peels none.
9. A cinnamon sapling grows into its tree: cinnamon logs under a crown of its own, non-persistent leaves. Its wild patch loads.
10. A Spice Rack:
    - refuses bread;
    - takes eight paprika and no more;
    - reads 15 on a comparator when full;
    - gives the last back to an empty hand;
    - spills the rest when broken.
11. A pepper dries in a smoker into a Dried Chili. The Mortar and Pestle, with the chili in the other hand, uses one and gives two Paprika, and the mortar is kept.
12. Every new Cooking Pot dish cooks, the new Ratatouille and Dill Pickles among them, and an eggplant roasts.
13. Each of the twenty wild plants gives 1-2 of its seed, and its wild patch loads.

`AgricultureGameTests.grassDropsJugcraftSeeds` now counts the seven new seeds.

The client game test (`VegetablesHerbsSpicesClientGameTests`, CI job `client`) takes six screenshots:
1. The vegetables at every age.
2. The herbs hung in bundles fresh and dried, grown in Planter Boxes, through their stages, and potted.
3. The spice vines and crops at every age, and a grown cinnamon tree.
4. Two Spice Racks close up, one full and one part full.
5. The twenty wild plants.
6. A wall of the 48 new items.

Not done: play in a real client, and a two-client dedicated-server session.

## World and event applicability
Farming and food. The wild plants and the cinnamon tree grow in new chunks only, in the biomes above. Jugcraft's own biomes do not place the cinnamon tree yet. Nothing is seasonal.

## Rollout and open questions
- Paprika is ground only with the Concordance's Mortar and Pestle. A Crusher recipe could be added for the machine age, but it would change the machines' recipe tables and handbook, so it is left for later.
- The cinnamon tree is not yet in Jugcraft's biomes (`tools/biomes.py`).
- The Spice Rack shows each spice as its item, standing up. Jars of its own could follow.
- All new IDs are listed in the tables. Removing one later needs the usual migration and backup guidance.
