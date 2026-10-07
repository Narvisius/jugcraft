# Material sets: ingots, nuggets, blocks, ores and bronze and steel tools

Status: implemented on `claude/material-sets`, awaiting review. Art only. **Not yet compiled or run:** the Java compiles
only in CI, which has not run on this branch; **not yet played**, and the in-game look is not yet seen.
Proposal issue: the owner, 5 to 6 October 2026: "redraw the bronze and steel tools and ores in this style too and then lets
compare against eachother" (the style of their chartreuse material-set sheet), then, after the comparison: "I like the
alternate versions for steel and bronze and then also the ingots need to be fixed they aren't the shape of vanilla's
ingots and the nuggets aren't either make them match inshape 1:1 like all mods do".
Owner: jimbozoomer-byte
Target milestone and tier: art only, for the shared materials of [base-materials.md](base-materials.md),
[tin-and-bronze.md](tin-and-bronze.md) and [tools-and-armor.md](tools-and-armor.md).
Primary specialty and supported player role: mining and metallurgy (looks only).

## Player experience
Every metal, ore and the bronze and steel tools are redrawn in the owner's material-set style
([MATERIAL_SETS.md](../MATERIAL_SETS.md)): 16×16, one clear hue per material in four tones with an outline in its
darkest, light from the top left, flat tones and no noise.

- **Ingots and nuggets, all fourteen metals** (tin, zinc, lead, silver, nickel, tungsten, uranium, titanium, bronze,
  aluminum, brass, invar, solder, steel): one shared ingot and one shared nugget, so they sit with vanilla's in an
  inventory.
  - **The ingot is the owner's,** recoloured for each metal and never redrawn. The owner sent it on 6 October 2026: "TAKE THIS AND RECOLOR IT LEAVE THE OUTLINE EXACTLY THE SAME JUST CHANGE THE COLORS and ALWAYS DO THAT FOR ALL INGOTS THAT ARE SUPPOSED TO BE SHAPED LIKE THAT".
    Its pixels are kept exactly; only its eight greens change, each to the same place on the metal's own ramp.
  - **The nugget** is a small shard on the diagonal in vanilla's form, drawn fresh from memory and resized from CI's
    in-game shots. No vanilla ingot, nugget, stone or deepslate texture was downloaded or read.
  - The client test sets vanilla's iron, gold and copper beside ours.
- **Storage blocks, all fourteen:** the owner's four-panel inlay: a light frame round a cross dividing four inset panels,
  each lit top-left.
- **Every ore and deepslate ore** (tin, zinc, lead, silver, nickel, tungsten, uranium, titanium, rock salt, phosphorite,
  lepidolite, monazite): **vanilla's own stone or deepslate with our ore on top.** The block model references
  `minecraft:block/stone` or `minecraft:block/deepslate` by name (nothing of Mojang's is copied) and lays the ore's
  overlay over it: five to seven chunky rounded blobs in the ore's own tones, cut out, no outline. The ore's rock now
  matches the rock round it exactly. Three overlay layouts are taken in turn so neighbouring ores differ.
- **Raw ores and raw blocks, the eight mined metals:** a lumpy rounded chunk, and lumps packed like cobblestone, in the
  ore's own tones.
- **Bronze and steel tools:** sword, pickaxe, axe, shovel, hoe and paxel in the owner's alternate palettes: bronze warm
  tan-gold, steel dark blue-grey, on a two-tone stick.

![Before and after: every metal's ingot, nugget and storage block, every ore and deepslate ore, the raw ores and raw blocks, and the bronze and steel tools](../images/material_sets_before_after.png)

*Before (above) and after (below), drawn from the textures outside the game at 4×. The new ores are shown over our own
stand-in rock (`host_stone`, `host_deepslate`), since vanilla's stone is not on the machine that drew the picture; in
game each sits on vanilla's own stone or deepslate. The in-game look is CI's to show (Verification).*

What changed in the repository:
- 94 textures: 42 metal textures (`item/<metal>_ingot`, `item/<metal>_nugget`, `block/<metal>_block`), 24 ore overlays
  (`block/<ore>_ore`, `block/deepslate_<ore>_ore`), 16 raw textures (`item/raw_<metal>`, `block/raw_<metal>_block`) and
  12 tools (`item/{bronze,steel}_{sword,pickaxe,axe,shovel,hoe,paxel}`).
- 24 ore block models, now children of the new `models/block/template_ore.json`.
- The maps, `tools/material_icons/*.txt` (16: the six tools, `ingot`, `nugget`, `storage_block`, `raw`, `raw_block`, the
  overlays `ore_a`, `ore_b` and `ore_c`, and `host_stone` and `host_deepslate`), and their loader, ramps and model writer,
  `tools/material_icons.py`. `tools/generate_textures.py` and `tools/gear_textures.py` call it for the textures and
  `tools/generate_material_data.py` for the ore models.

### The palettes
Bronze and steel are the owner's chosen palettes (their "alternate versions"). The same choice named brass fittings
for bronze and gunmetal for steel; none of this change's maps has fittings, so they are recorded in MATERIAL_SETS.md for
the weapons and armor and used by no texture yet. The other twelve metals are their old palettes moved onto the ladder
those two set, by one rule, each with its own hue, saturation floor and ceiling and luma offset, chosen together so that
no two metals are closer than 8 (CIEDE2000 over an ingot's dark, mid and light tones), vanilla's iron, gold and copper
included, and each ingot keeps the tint of its own metal's plate, gear and dust
([MATERIAL_SETS.md, "The metals"](../MATERIAL_SETS.md#the-metals) has the rule and the table). The ores, raw ores and raw
blocks use each ore's own five tones, as the owner approved them in the comparison.

**Re-solved after review:** the first ramps put lead, titanium and solder almost on top of steel (CIEDE2000 5 to 7
between them; their ingots' mean colours 3 to 7 apart, against 10 to 16 on `main`), and pushed tin, silver, aluminum,
nickel, invar and brass into saturated blues and yellows, 17 to 20 from their own plates and dusts. The twelve were solved
again with the pair floor, which `tools/check_mod_data.py` now enforces, and with each ingot's tint held to its own
metal's items. The closest pairs now sit at the floor (lead and steel, tin and invar, lead and tungsten: 8.0), and the
closest two ingots' mean colours are 5.7 apart (tin and invar), against 2.0 on `main` (silver and aluminum).

### How an ore block is built
`template_ore.json` has two cubes:
- **The rock:** a full cube with `#stone` on its sides and `#stone_top` on top and bottom.
- **The overlay:** a second full cube with exactly the same corners (`0` to `16`), every face `#overlay`, laid on as
  vanilla's `grass_block` lays its side overlay.
- **Every face:** UV pinned to `[0, 0, 16, 16]` and `cullface` on its own side, so buried ores draw nothing.
- **Particles:** stone's.

Each ore's model sets the textures:
- **Stone ores:** `stone` = `minecraft:block/stone`.
- **Deepslate ores:** `stone` = `minecraft:block/deepslate` and `stone_top` = `minecraft:block/deepslate_top`.
- **Both:** `overlay` = `jugcraft:block/<the ore>`.

How 26.3 draws it, from the evidence in this repository:
- **No render layer is set anywhere**, and none is needed. There is no block render-layer code in `src` (no
  `BlockRenderLayerMap`, `ChunkSectionLayer` or `ItemBlockRenderTypes`; the `RenderType` uses in `src/client` are
  for entity, block-entity and preview rendering, not chunk layers) and no `render_type` in any model, yet the crops, saplings,
  potted plants and full-cube leaves render cut out in CI's client screenshots (`docs/images/ingame_crop_stages.jpg`,
  `ingame_chestnut_trees.jpg`).
- **26.3 judges see-through faces by their pixels.** It has a `force_translucent` texture form (used by blast glass) and
  refuses to bake a face that reads outside a see-through texture.
- **So a texture's alpha decides the layer:** an overlay whose pixels are only fully clear or fully opaque draws cut out
  over the solid rock. `tools/check_mod_data.py` checks every overlay is exactly that.
- **Why the two cubes are flush:** each overlay face has exactly the same corners as the rock face under it, so both
  get exactly the same depth, and the overlay is drawn after the rock (the later element, or the cut-out layer after the
  solid one), so it shows at every distance. Vanilla's `grass_block` is built the same way (from memory of its model:
  vanilla's assets are not on this machine). This is reasoning, not something a screenshot has shown yet. The first
  version set the overlay 0.02 px proud instead, which beyond about 32 blocks is less than a depth buffer can tell apart,
  so faraway ore faces could have speckled with stone.
- **Written outside the coplanar pass:** `generate_material_data.py`'s `write()` runs `model_writer.separate_coplanar`,
  which would nudge one of the two cubes and, on the north and south faces, push the rock in front of the overlay (found
  while prototyping). `material_icons.write_templates` writes the template itself, in the same JSON format.

**Deepslate's top:** our deepslate ores show `deepslate_top` on top and bottom, as natural deepslate (`axis=y`) does, so
they match the deepslate round them. Vanilla's own deepslate ores show the banded side on top; ours deliberately differ
there.

**The fallback:** if the client screenshots show the overlay failing (black or missing blobs, stone over the blobs, or
flicker), set `OVERLAY = False` in `tools/material_icons.py` and run both generators. The ore textures then become full textures
drawn on our own host-rock maps (same file names), the models go back to `cube_all` and the template is not written.

## Connections
- Existing input producer: unchanged (worldgen, smelting, the alloy smelter and steel foundry make the same items).
- Existing output consumer: unchanged.
- Technology connection: none changed. IDs, recipes, drops, tags, loot tables and worldgen are unchanged; placed blocks and
  items keep working and just look new.
- Magic connection: none.
- Reachable entry path: unchanged.
- Which connections are required vs optional; trade and solo routes: unchanged.
- How this specialty stays useful without mastering every other branch: unchanged.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: art only. It touches no
  progression.

## Balance and automation
None. No recipe, cost, drop or rate changes.

## Multiplayer and persistence
Nothing is saved and nothing needs migrating: the block and item IDs are unchanged, the blockstates and item definitions
are unchanged (they already pointed at `jugcraft:block/<ore>`), and only textures and the ore block models changed.
Disabling a feature in `config/jugcraft.properties` is unaffected. A resource pack that replaces `block/<ore>_ore.png`
with a full texture still works: the full texture covers the stone beneath.

## Dependencies and assets
No new dependency. Vanilla's `minecraft:block/stone`, `minecraft:block/deepslate` and `minecraft:block/deepslate_top` are
referenced by name, never copied, as `models/block/mortsafe_0_clean.json` already references `minecraft:block/coarse_dirt`.
Every map is drawn fresh (MIT); the owner's sheet is a style reference only, and no Mojang texture is read, traced or
recoloured. AI-assisted: Claude Opus 5.5 (`claude-opus-5-5`).

## Verification
Run locally (6 October 2026, and again after the review fixes):
- **`python3 tools/generate_textures.py`:** exactly the 94 intended PNGs change. The mod icon, the mineral blocks and
  items, plates, gears, dusts, washed ores, the armor and the vanilla-tier paxels are byte-identical.
- **`python3 tools/generate_material_data.py`:** exactly the 24 ore models change and `template_ore.json` is new.
- **Determinism:** a second run of both generators gives an identical tree.
- **`python3 tools/check_mod_data.py`:** `PASS: 1437 material IDs, data files and recipe audit.` Its new
  `check_material_sets()` checks:
  - every map loads;
  - every texture drawn from the maps equals the committed PNG (CI does not re-run `generate_textures.py`);
  - the template's two cubes, UVs and cullfaces;
  - every ore model;
  - every overlay's alpha is only 0 or 255;
  - every metal ramp steps up in luma, D at least 30 above O;
  - no two metal ramps, and no ramp and vanilla's iron, gold or copper stand-in, are closer than 8 (`PAIR_FLOOR`).
- **Negative tests:** a one-pixel change to `tin_ingot.png`, a deepslate host in `tin_ore.json` and a half-transparent
  pixel in `zinc_ore.png` gave four errors and exit 1. Restored, it passed. After the review fixes, in memory only (no file
  touched): solder's ramp set to steel's and brass's moved next to gold gave the two look-alike errors (and six
  stale-texture errors); a template with the overlay set proud again was caught as not the committed template.
- **`python3 scripts/check_repository.py`:** PASS.
- **A before-and-after sheet** of all 94 textures was rendered from the PNGs and looked at. It shows the new ores over our
  own host-rock maps, since vanilla's stone is not on the machine. The picture above
  (`docs/images/material_sets_before_after.png`) is a compact version of it, drawn the same way from the committed PNGs
  (before: `main`; after: this change) by a scratch script that is not part of the repository.

In CI (not run locally; Java compiles only in CI): `MaterialSetsClientGameTests`, shots `jugcraft_material_sets_*`:
- **The ore wall** (`ores_wall`, about 14 blocks off, and `ores_close_1` to `4`): vanilla stone over vanilla deepslate
  with ores set in both rocks, in four groups: vanilla's iron, copper, gold or coal ore, then three of ours (coal beside
  our minerals). Each close-up is one group.
- **The ore floor** (`ores_floor`): stone and deepslate let into the floor with ores in them, for the top faces.
- **The blocks** (`blocks_1` to `5`): five panels of storage blocks, each vanilla's iron, gold and copper blocks then
  three of ours (two in the last), with vanilla's raw blocks then three of ours (two in the third) on top of the first
  three.
- **Ingots and nuggets** (`ingots_1` to `5`, `ingots_close_1` to `5`): five frame panels, each vanilla's iron, gold and
  copper (ingots above, nuggets below) beside three of ours (two in the last). Each panel is shown whole, then close up
  on vanilla's gold and copper beside two of ours.
- **Tools** (`tools_1`, `tools_2`, `tools_close_1` to `4`): vanilla's iron tools above our bronze ones and vanilla's
  gold tools above our steel ones (our older-style iron and gold paxels above our paxels), whole and three a close-up.
- **Raw ores** (`raw_1` to `3`): vanilla's raw iron, gold and copper beside three of ours a panel (two in the last).
- **The survival inventory** (`inventory_ingots`, `inventory_blocks`, `inventory_raw`, `inventory_ores`,
  `inventory_tools`): ours beside vanilla's in the slots. Every row but the bronze and steel tool rows starts with
  vanilla's; in `inventory_tools` vanilla's iron tools are the row above them and its gold tools are in the hotbar.
- **In the hand** (`held_steel_pickaxe`, `held_bronze_sword`, `held_bronze_ingot`).

The test fails on any missing item or block ID; whether the overlay renders right is for the screenshots to show, not
the log. Until CI's `client-screenshots-*` artifacts are looked at, the in-game look counts as **not verified**.

**Not run:** `./gradlew build` (the Java, `MaterialSetsClientGameTests` included, has not been compiled); the client
test itself; play; a dedicated server with two clients (nothing here is multiplayer-specific). Update this list once CI
has run on the branch.

## World and event applicability
Not applicable: art only.

## Rollout and open questions
- **The ingot is the owner's, recoloured** (6 October 2026), after two drafts of ours. Our ingot was first drawn from
  memory of vanilla's, then resized from CI's shots. The owner judged it still unlike vanilla's, sent their own ingot, and
  set the rule: every ingot of that shape is theirs, recoloured. `check_mod_data` pins the map. The record of the earlier
  drafts follows.
- **The nugget form is drawn from memory** of vanilla's, then corrected once from CI's in-game shots.
  - **The first run** (f554b439) set vanilla's gold and copper ingots and nuggets beside ours.
  - **What it showed:**
    - our nugget, a flat lump 8 by 6 with a knob, did not look like vanilla's, which are small shards on the diagonal;
    - our ingot, 14 by 10, read smaller and thinner than vanilla's.
  - **Measured from the shots:** vanilla's ingot spans about 16 by 11 to 12 pixels with its outline, and its nuggets are
    about 5 to 7 wide by 7 to 8 tall, leaning up-right.
  - **The fix:** the maps were redrawn fresh to those sizes. The ingot is now 16 by 12 with a squared near end, and the
    nugget a 7 by 8 shard.
  - **Nothing was traced:** the shots are 480 by 270 JPEGs, in which an ingot is a few dozen blurred pixels, so they
    give sizes and forms, not pixels.
  - The next CI run's close-ups are the judge again. Iterate `tools/material_icons/ingot.txt` and `nugget.txt` on the
    owner's word.
- **Only bronze and steel's palettes were approved;** the other twelve follow the rule above. Most are greys told apart
  mostly by lightness, and the closest pairs sit at the floor of 8 (lead and steel, tin and invar, lead and tungsten,
  zinc and solder, silver and titanium); the owner may want some spread further, or a tint changed.
- **Plates, gears, dusts and two bronze items keep the old palettes:** each metal's plate, gear and dust (drawn from
  `part_palette` in `tools/generate_textures.py`, which the armor shares), `bronze_blend` and the farming
  `bronze_sickle` (`tools/crop_textures.py`) are unchanged. The new ingots keep each metal's tint, but the ladder's dark
  outline and darker body make the pale metals' ingots read darker than their own plates and dusts: the mean colour of
  tin's, silver's, aluminum's and brass's ingots is 13 to 16 (CIEDE2000) from their plate or dust, against 5 to 7 on
  `main`; the other metals' are 1 to 11, bronze's (the approved palette) 10. Follow-up: draw those items from
  `METAL_RAMPS` once the armor PRs, which share `part_palette`, have landed.
- **Four approved ore ramps** (tin, tungsten, uranium, titanium) are dark or low in contrast between their darkest tones;
  PR #201's draft icon rules (`tools/check_icon_maps.py`) would fail their raw chunks if pointed at them. They are the
  owner's chosen look, so they stay.
- **Side effects, art only:** the electroplating bath's anode plates are drawn with `nickel_block` and `silver_block`, and
  the classic machine pack's alloy smelter cap with `bronze_block`, so they show crops of the new four-panel faces.
- **The vanilla-tier paxels** (wood to netherite) keep the older mask, so they no longer match the bronze and steel paxels.
- **The mod icon** (`assets/jugcraft/icon.png`) keeps the old ore and ingot; redrawing it is the owner's call.
- **The bronze and steel armor** is untouched (it is being redone separately), so its icons differ in style from the new
  tools until that lands.
- **If the overlay misbehaves in 26.3,** flip `OVERLAY = False` (above) and say so in the pull request.
- **Follow-ups:**
  - once the armor PRs land, delete the old `SWORD` to `HOE` masks and the loop in `tools/gear_textures.py` that still draws
    them (the maps draw over them);
  - point PR #201's `tools/check_icon_maps.py` at `tools/material_icons/` once both are in;
  - `docs/features/tools-and-armor.md` still says the tool icons are drawn "from hand-made masks"; `claude/armor-styles`
    rewrites that line, so whichever of the two lands second makes it say that the bronze and steel tools are drawn from
    the maps in `tools/material_icons/`;
  - **CI time:** `MaterialSetsClientGameTests` is listed before the arms classes, so it runs in client shard 2 (about 23
    of its 30 minutes on `main`) and moves six arms classes between shards. Watch that shard's time on the first run; if
    it nears 30 minutes, move the entry to a position that lands in shard 1 (the lightest), rather than raising the
    timeout;
  - `docs/NATURAL_TEXTURES.md` is not on `main`; it arrives with PR #195, whose ore row still describes an ore drawn on a
    host rock ("The host rock as above, with three or four small clusters of the mineral in three tones, lit on their
    upper sides."). This change does not create the file. Whichever of the two PRs lands second replaces that row with
    the one below, with `MATERIAL_SETS.md` made a link to [MATERIAL_SETS.md](../MATERIAL_SETS.md) (in `docs/`, the
    target is plain `MATERIAL_SETS.md`):

    ```
    | Ores | Not drawn on a host rock: the block model layers the ore's overlay (chunky blobs, cut out, no outline) over vanilla's own stone or deepslate, referenced by name, never copied (MATERIAL_SETS.md). |
    ```
