# Wood repaint: every tree's wood and leaves in vanilla's manner

Status: implemented on `claude/wood-repaint`, awaiting review. Green in CI, with in-game screenshots; **not yet played**.
Proposal issue: the owner, 5 October 2026, with a screenshot of 24 stripped logs they had repainted: "heres how I redid and painted all the different wood types can you use these match them closest to the new ones we have added into the game and then make all the other things related to each one be based off these, they should look more similar to how the vanilla textures are in the way I did them here". They followed it with pictures of the trees they like (vanilla-like fine-speckled leaves and bark), "Recoloring vanilla log textures is a good way to start if you feel like you are over complicating them" and "I don't want rings in the trees make them similar to vanilla in terms of that".
Owner: jimbozoomer-byte
Target milestone and tier: art only, for the 13 woods of the agriculture and biomes branches ([festival-crops.md](festival-crops.md), [alpine-spawn.md](alpine-spawn.md), [seasonal-forests.md](seasonal-forests.md), [wetlands.md](wetlands.md), [warm-and-dry.md](warm-and-dry.md), [big-trees-and-rainforests.md](big-trees-and-rainforests.md)).
Primary specialty and supported player role: building and exploring (looks only).

## Player experience
Every wood the mod adds is redrawn in its colour from the owner's paintings, and drawn the way vanilla draws wood. That covers its bark, log ends, stripped log, stripped log ends and planks; the wood blocks, stairs, slabs, fences and fence gates show these too. Every tree's leaves are redrawn as well.

![Before and after](../images/wood_repaint.png)

- **Bark:** long vertical furrows that wander a pixel now and then, with ridges between them and a few light flecks. **No rings across the trunk.** Each tree keeps its own bark colour and character: the aspen is white with dark marks, the cypress and redwood stringy, and the eucalyptus green with rainbow streaks.
- **Log ends:** square growth rings out to the edge, inside a one-pixel ring of bark (stripped, inside a darker ring of the wood), as vanilla's.
- **Stripped wood:** straight vertical grain in the painting's colour, with broken streaks in two darker tones and a few light ones.
- **Planks:** four boards a block, each lit along its top with a dark seam beneath, short grain dashes and a butt joint in a different place on each board.
- **Leaves, in every look** (green, autumn, gold, bare): vanilla's fine speckle of one- and two-pixel leaves in four tones, with small see-through gaps.
  - **Needles** (larch, fir, cypress, redwood) are darker, with short slanting strokes.
  - **The jacaranda's blossom** is violet over a little green.
  - **The palm's fronds** have long blades.
  - **Bare branches** are twigs in the bark's colour.
  - **The chestnut's burs and ripe nuts** sit on the new leaves.
  - **Fast graphics:** the see-through gaps keep a dark colour, so trees still look dense.

**Which painting each wood follows.** Paintings are numbered left to right, then top to bottom, from 0. The colour is sampled from the painting's side.

| Wood | Painting | Colour |
|---|---|---|
| chestnut | 11 | `#b6a075` |
| larch | 1 | `#c98634` |
| maple | 0 | `#cc9d71` |
| aspen | 9 | `#d5d1ce` |
| fir | 16 | `#ceb678` |
| dead | 23 | `#7a7a7a` |
| jacaranda | second set, row 6 (was 7) | `#7a5a5e` (was `#a97b74`) |
| willow | 22 | `#c7c785` |
| palm | 10 | `#e5d5b2` |
| cypress | 14 | `#916558` |
| redwood | 15 | `#ab5740` |
| eucalyptus | 21 | `#bda281`, with its pastel flecks |
| mahogany | 2 | `#7a1f0d` |
| cedar (added by the tree roster's batch 1, [trees-batch-1.md](trees-batch-1.md)) | second set, row 0 | `#725543` (painted `#84654d`, darkened off vanilla spruce and the cypress) |

The match is by colour first, then by species where two paintings were close: the speckled painting is the rainbow eucalyptus, and the deep red one is mahogany. The other eleven paintings are not used yet. Any pairing is one line to change in `WOOD` in `tools/wood_style.py`.

### The owner's second set
Later the same day the owner sent eight complete woods they had painted (bark, log end, stripped side, stripped end and planks each): "heres some wood I did you can use these and use them to recolor for future wood or if you think this fits any of the current better". Their colours were sampled from the screenshot, and each was drawn by `tools/wood_style.py` in its own colours beside the owner's painting to compare like for like.

Three independent judges then mapped the eight rows: one by the owner's intent, one by species realism, and one by distinctness from the other woods and from vanilla's. A change was made only where two agreed:
- **The jacaranda takes row 6's mauve wood** (`#7a5a5e`, was `#a97b74`). It is the only purple wood, it suits the violet-blossom tree, and it moves the jacaranda further from the cypress. Its bark is unchanged, so only its log end, stripped log and ends, and planks are redrawn.
- **The other seven rows waited in a bank** (`OWNER_BANK` in `tools/wood_style.py`) for new trees, each under the species judged to suit it. Row 0 is now used: it became the **cedar** in the tree roster's batch 1 ([trees-batch-1.md](trees-batch-1.md)), and six rows remain banked:

| Row | Suggested tree | Wood | Bark |
|---|---|---|---|
| 0 | western red cedar: **now used, for the cedar** (wood darkened to `#725543`, and the bark with it by the same lightness ratio; drawn stringy) | `#84654d` | reddish-brown, furrowed |
| 1 | London plane | `#9b8059` | pale olive-grey, mottled |
| 2 | black walnut (the clock, theremin and planchette are described as walnut) | `#67533c` | charcoal, furrowed |
| 3 | wenge | `#544233` | dark mossy olive |
| 4 | elm | `#866448` | dark brown, blocky |
| 5 | shagbark hickory | `#78573c` | pale grey, in strips |
| 7 | yew (the longbows are described as yew) | `#654135` | grey-brown, flaky |

Before a banked wood becomes a tree, check it beside vanilla's and the others. Rows 0, 4 and 5 are close to each other, and 2, 4 and 5 lie near vanilla's dark oak and spruce, so nudge them apart. One judge each also proposed:
- row 6's purple bark for the jacaranda too;
- row 7 for the mahogany, which is more saturated than any vanilla wood;
- row 4 for the chestnut, whose planks are close to vanilla birch's.

The owner chose to keep all three as they are: the jacaranda's grey-brown bark, the mahogany's deep red and the chestnut's light tan. Rows 4 and 7 stay banked for an elm and a yew.

## Connections
- Existing input producer: none changed.
- Existing output consumer: none changed.
- Technology/magic connection: none; art only.
- For cosmetics: supported systems are the existing wood sets and trees, unchanged. Resource links do not apply.

## Balance and automation
No change: no recipe, number, tag or behaviour differs.

## Multiplayer and persistence
Every texture name, block ID, item ID and model is the same, so worlds, placed blocks and inventories are untouched. Only the pictures change. Nothing is saved, and no client/server code changes.

## Dependencies and assets
- **No new dependencies.**
- **Code:** `tools/wood_style.py` draws all 89 textures (65 wood, 24 leaf) from fixed seeds. It runs last in `tools/crop_textures.py`. The old wood and leaf drawing is removed from `tools/forest_textures.py`, `tools/larch_textures.py`, `tools/wild_textures.py` and `tools/festival_textures.py`, which keep the saplings and everything else.
- **No Mojang or other texture is read, traced or recoloured.** The owner suggested recolouring vanilla's log textures, but the project rules forbid copied proprietary assets in Git ([CLAUDE.md](../../CLAUDE.md)). So every texture is original, drawn by code in vanilla's manner, as the owner's paintings are.
- **The owner's screenshot is not committed.** Only the colours sampled from it are, in `WOOD`.
- **Generated by:** `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py`.

## Verification
- **`python3 tools/check_mod_data.py`:** PASS (local).
- **`python3 scripts/check_repository.py`:** PASS (local).
- **Texture diff against `main` (local):** exactly the 89 wood and leaf textures changed. The other 2,589 textures, saplings included, are byte-identical, and no texture was added or removed.
- **Offline previews:** each wood's textures, and log and leaf blocks drawn as cubes, were rendered and looked over before committing (the sheet above).
- **Client game test (`WoodClientGameTests`)**, written for CI:
  - every tree grown side by side in summer, three or so to a shot, with each tree's logs and leaves logged;
  - a sample wall of each wood: log, stripped log and planks up the face, log and stripped ends and a slab on top, and stairs, a fence and a gate before it;
  - the larch, maple, aspen and willow grown again in autumn.
- **CI results:**
  - **82a5c0a1, the first push:** all green. The biome survey's screenshots (shard 2) and the chestnut and larch scenes show the new woods and leaves in game.
  - **614c264a:** all green. `WoodClientGameTests` grew every tree. Logs and leaves near each trunk:

    | Tree | Logs | Leaves |
    |---|---|---|
    | chestnut | 6 | 134 |
    | larch | 10 | 57 |
    | maple | 6 | 136 |
    | aspen | 8 | 148 |
    | fir | 9 | 97 |
    | dead | 11 | none |
    | jacaranda | 9 | 85 |
    | willow | 7 | 85 |
    | palm | 10 | 79 |
    | cypress | 10 | 34 |
    | redwood | 13 | 106 |
    | eucalyptus | 13 | 45 |
    | mahogany | 10 | 80 |

    Its shots showed the HUD and hand, which an earlier test in the shard had hidden and the test's toggle showed again.
  - **97ccc856:** the test hides the HUD only if it is showing, as `OfrendaClientGameTests` does.
  - **bd1a4513 (with main merged in): all green.** The build, the server tests and all three client shards passed. The tree, autumn and sample-wall shots are clean.
- **Not run:** play, and a look at the trees in-game beyond CI's screenshots.

## World and event applicability
Not applicable: no worldgen, tree shapes, biomes or seasonal rules change. Seasonal leaves keep all their looks, redrawn.

## Rollout and open questions
- **First pass for the owner to judge.** Colours, pairings and patterns are each a line or two in `tools/wood_style.py`.
- **Log ends keep growth rings,** as vanilla's do; "no rings" is read as no bands across the bark. If the owner wants plain ends too, that is one function.
- **Tree shapes** (hanging willow leaves, tiered conifers, as in the owner's pictures) are not changed here.
- **Reversible:** reverting the commit restores every old texture; nothing in a world depends on them.
