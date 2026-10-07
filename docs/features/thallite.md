# Thallite: the Earth school's green metal

Status: approved. The owner chose this concept on 5 October 2026 and approved its numbers on 6 October 2026: "the
Thallite numbers all look good carry on". It is built in slices, each its own PR (below).
- **Slice 1 (ore and materials)** is implemented on `claude/thallite-ore` (PR
  [#221](https://github.com/jimbozoomer-byte/jugcraft/pull/221)), awaiting review. Its CI passed; it has not been played.
- **Slice 2 (gear)** is implemented on `claude/thallite-gear`, awaiting review. **It has not been compiled or played.**
- Slices 3 to 5 are not built.

Proposal issue: none. On 5 October 2026 the owner drew a complete set for a new ore in one chartreuse palette and asked
what it should be: "maybe something in the magic tier". The set covers:
- ore in stone and deepslate, raw chunk and block, nugget, ingot and storage block;
- sword, pickaxe, axe, shovel and hoe;
- helmet, chestplate, leggings and boots, each plain and gold-trimmed;
- daggers, a sabre, longswords, a spear, a hammer, a spiked mace, a bow, a crossbow, an arrow and horse armor.

A design panel worked up three magic-tier concepts (thallite, chrysolite and aurivert), and critics checked them against
the project's rules and for fun. The owner picked thallite. How the set is drawn:
[MATERIAL_SETS.md](../MATERIAL_SETS.md).

Owner: jimbozoomer-byte
Target milestone and tier: the Discovery stage, at iron level beside bronze. It is the material of the Earth school
([CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md)) once magic exists.
Primary specialty and supported player role: magic (Earth) and early gear. An engineer can use it too, with no magic.

## Player experience
> Thallite is a green metal that grows in old stone wherever roots and water reach, and gear forged from it slowly
> regrows on living soil. Bind a piece with gold at a smithing table and it becomes Earthbound: it holds you to the
> ground, regrows even on bare stone, and becomes the Earth school's armor once spells arrive.

### What it is
- **The mineral:** "thallite" is an old name for green, iron-rich epidote, from the Greek *thallos*, "a young green
  shoot".
- **The legend:** medieval alchemists believed metals ripened in the earth, so a green metal was "unripe gold".
- **The owner's art already tells the story:**
  - the leaf-shaped nugget is the shoot;
  - the sage raw chunk is the mineral as dug;
  - the chartreuse ingot is the worked metal;
  - the gold trim is the gold that ripens it.
- **The palette** stays sage and chartreuse with gold, apart from uranium's yellow-green and emerald's blue-green.
- **The name** is near the poison thallium, so a lore line sets the tone: "green as a new shoot".

### Where it is found
Overworld only, in stone and deepslate.
- **Everywhere:** vein size 7, 4 per chunk, Y −32 to 48 (between lead and silver).
- **Rich pockets:** 6 more veins of size 9 per chunk at the same heights, only in vanilla Lush Caves and Jugcraft's
  Glowcap Grotto. That roughly triples the ore there.
  - The in-world reason: it forms where roots and water work through old rock.
  - These veins are never thrown away for touching air, so green streaks show in the cave walls.
  - Azalea trees and hanging roots mark the caves from above.
- **Mining:** an ore drops one raw thallite, and Fortune works as on vanilla ores.

### Getting it (no magic or machine needed)
1. Mine it with a stone pickaxe, like tin, zinc and lead.
2. Smelt it in a furnace or blast furnace (0.7 xp).
3. Nuggets, ingots and blocks, and raw ore and raw blocks, convert 9 to 1 both ways.
4. Craft plain tools and armor at a crafting table in the usual shapes; ingots repair them.
5. Bind armor with gold at a smithing table to make it Earthbound (below).

### Where its gear sits

| Tools | Uses | Speed | Damage bonus | Enchantability | Drops |
|---|---|---|---|---|---|
| **Thallite** | 200 | 6.0 | 2.0 | **18** | iron |
| Iron | 250 | 6.0 | 2.0 | 14 | iron |
| Bronze | 320 | 6.5 | 2.0 | 14 | iron |
| Steel | 900 | 7.0 | 2.5 | 12 | diamond |

| Armor | Durability multiplier | Defense | Toughness | Enchantability |
|---|---|---|---|---|
| **Thallite** | 13 | 2/5/6/2 (iron's) | 0 | **18** |
| Iron | 15 | 2/5/6/2 | 0 | 9 |
| Bronze | 15 | 2/5/6/2 | 0.5 | 12 |

Thallite enchants better and keeps itself going, but it wears out sooner and digs slower than bronze. Steel, diamond and
netherite stay stronger.

### What makes it different
Two bounded traits; neither adds damage, defense or speed.

- **Regrowth (all thallite gear):**
  - Every 5 seconds, each thallite item worn or held gets back one use while its holder stands on living soil
    (`#jugcraft:living_ground`: grass, dirt, coarse dirt, podzol, mud, moss, rooted dirt and farmland by name, and
    whatever else vanilla's `#minecraft:dirt`, `#minecraft:grass_blocks` and `#minecraft:mud` tags hold).
  - It stops at 75% of full and never brings back a broken item.
  - A tool goes from empty to 75% in about 12 minutes outdoors.
  - It shows as a faint green sparkle (vanilla's happy-villager particle) and as the trait "Regrowth" in the tooltip,
    described while Shift is held
    ([trait-details.md](https://github.com/jimbozoomer-byte/jugcraft/blob/claude/trait-details/docs/features/trait-details.md)).
- **Rooted (Earthbound armor only):**
  - Each Earthbound piece gives +0.075 knockback resistance while the wearer stands on natural ground
    (`#jugcraft:earthen_ground`: living soil plus stone, deepslate, sand, gravel).
  - A full set gives 0.3, under netherite's 0.4; in the air or water, nothing.
  - With two or more Earthbound pieces worn, Regrowth works on any natural ground.

### The two looks: plain and Earthbound
- **Plain (Unbound):** thallite as forged, with Regrowth.
- **Gold-trimmed (Earthbound):** the same stats, plus Rooted, and Regrowth on stone.
- **Making it Earthbound:** at a smithing table, an **Earthbinding Template**, the plain piece and a gold ingot give
  `earthbound_thallite_<piece>`.
  - It is a separate item with its own worn look.
  - It keeps its enchantments, wear and name.
  - It is one-way.
- **The template:** rooted dirt in the centre, four thallite nuggets at the sides and four gold nuggets at the corners.
  A full set costs about 5.8 gold ingots.
- **The green gems** on the leggings and boots are the anchors of its roots.
- **Only the armor has two looks,** as drawn. Earthbound ships with the plain armor and does not wait for magic.

### Weapons and the rest of the set
- **Arms:** only the kinds drawn: dagger, sabre, longsword, greatsword, spear, war hammer, flanged mace, longbow and
  arbalest. The owner named the sheet's two long blades on 6 October 2026: "The big sword should be a great sword and the
  other one should be a longsword". They
  use bronze's numbers and thallite's durability, and are drawn from the 16×16 arm maps in thallite's colours.
- **Thallite Arrow:** base damage 2.0, as vanilla's. Its "Rooting" stops the target jumping for 2 seconds and gives
  Slowness I for 2 seconds, through a temporary attribute modifier, so no mixin. Bosses in `#jugcraft:unrootable` are
  immune.
- **Thallite horse armor:** 5 protection (iron's). The horse is Rooted (+0.2 knockback resistance) on natural ground.
  - The sheet draws it twice, nearly alike; the owner left the choice to us ("whichever horse armor you think fits best").
  - We follow the second drawing, at the bottom of the sheet. Its head and neck rise higher and clear of the blanket, so it
    reads as a horse at 16×16, and as the later of the two it looks like the revision. Its red and gold band stays.

## Connections
- **Existing input producer:** the world itself (ore). Ore processing gives more:
  - Crusher and Pulverizer: 2 a block (the Pulverizer with a 10% iron dust byproduct);
  - Ore Washer: 3;
  - acid leaching: 4.
- **Existing output consumer:** players' tools, armor and arms. Machines take thallite plates and dust through shared
  `c:` tags.
- **Technology connection:**
  - a METALS entry with its `c:` tags, so the Ore Drill and Prospector find it;
  - ore processing as above;
  - a plate (Metal Press) with a dearer hand-made route.
  - **No alloy, on purpose:** thallite's technology role is narrow, as [DESIGN.md](../DESIGN.md) allows when the reason
    is stated.
- **Magic connection:** the Earth school's metal. These come in the first magic-system PR, not here:
  - **The Thallite Focus:** Earth's starting instrument; 1 raw thallite, 2 ingots, 1 stick.
  - **Two first spells** sharing one temporary block, both with tight limits:
    - **Earthen Steps:** a short stair or bridge;
    - **Earthen Bulwark:** a 3×2 wall.
  - **Earthbound armor** lengthens earthwork.
  - **Later ideas:** a Block of Thallite as the Stonebloom herb's Earth "influence", thallite dust as one of several
    Earth catalysts, and an Earth Staff. Silver, salt and lead stay with other schools.
- **Reachable entry path:** stone pickaxe, then furnace, ingot, gear, template and Earthbound. No step needs its own
  output, and no magic or machine is required.
- **Required vs optional:** all optional. It can be crafted solo or traded: engineers can wear plain thallite with no
  magic, and mages trade raw ore to engineers for the washer's triple yield.
- **How the specialty stays useful:** cheap, easy to enchant, self-mending gear for farmers and early players, and the
  Earth school's armor later. Bronze lasts longer and steel goes further.

## Balance and automation
- **Numbers above are starting points,** not played.
- **Regrowth adds uses, never metal.** It stops at 75%, so Mending and anvils still matter. Gear never recycles into
  metal, so there is no loop.
- **Rooted is capped** at 0.3 for a full set. If it proves too strong in PvP, cut it to 0.05 a piece.
- **Check that thallite does not crowd out bronze** for early players. Its fewer uses and slower digging are meant to
  prevent that.
- **Server cost:** Regrowth is one check per player every 5 seconds over at most 6 slots. Rooted is a temporary
  attribute modifier refreshed twice a second, only for players wearing Earthbound pieces. No per-block or per-entity
  scans.

## Multiplayer and persistence
- **Server authority:** everything is worked on the server. Clients only draw the tooltip and the leaf particle.
- **Saved state:** none beyond ordinary items and blocks.
- **Stable IDs:**
  - `jugcraft:thallite_*` and `jugcraft:raw_thallite*`;
  - `jugcraft:earthbound_thallite_<piece>`;
  - `jugcraft:earthbinding_template`, `jugcraft:thallite_arrow` and `jugcraft:thallite_horse_armor`.
- **Disabling `thallite`** stops its worldgen and recipes but keeps every registration.
- **No retrogeneration:** existing chunks get no thallite.

## Dependencies and assets
- **No new dependencies.**
- **Art:** everything is drawn by code from 16×16 maps, after the owner's sheet, following
  [MATERIAL_SETS.md](../MATERIAL_SETS.md). The sheet is a style reference and is not committed. Slice 1's textures are
  drawn from the material-set maps of PR #218 in thallite's own ramps; nothing of Mojang's is read, traced or
  recoloured, and the ore models reference vanilla's stone and deepslate by name.
  - The worn armor layers (plain and Earthbound) and the arms' 3D in-hand textures are not on the sheet, so they are
    drawn to the same rules.

## Building it, in slices
Each slice builds on the open PRs whose systems it uses, so it waits for them or is stacked on them:
1. **Ore and materials:** the ore, material forms, worldgen (with a small optional "biomes" key on a metal's worldgen
   entry, which only rocks had) and processing. Its art is drawn by the material-set maps and ore overlays of the
   material-sets PR, so it is stacked on that branch. **Implemented on `claude/thallite-ore`** (below).
2. **Gear:** tools, both armor looks, the Earthbinding Template, Regrowth and Rooted. **Implemented on
   `claude/thallite-gear`** (below).
   - Earthbound is an entry in the one armor-style system of the Steampunk and Kaiser PR
     ([#215](https://github.com/jimbozoomer-byte/jugcraft/pull/215)), one-way and with a perk.
   - The traits use the Shift details of [#216](https://github.com/jimbozoomer-byte/jugcraft/pull/216).
3. **Arms:** the drawn kinds (dagger, sabre, longsword, greatsword, spear, war hammer, flanged mace, longbow and
   arbalest), first adding a list of arm kinds per metal to `tools/arms.py`; today every gear tier makes all of its kinds.
   Their icons come from the 16×16 arm maps of [#201](https://github.com/jimbozoomer-byte/jugcraft/pull/201).
4. **Arrow and horse armor:** the mod's first of each, built as shared code later ores can reuse.
5. **The Focus and Earth's first spells:** in the first magic-system PR, coordinated before work starts.

## Slice 1, as built: ore and materials
Branch `claude/thallite-ore`, stacked on the material-sets branch (PR #218), whose maps and ore template draw its art.

**What a player gets:**
- **Thallite Ore** and **Deepslate Thallite Ore** (`jugcraft:thallite_ore`, `jugcraft:deepslate_thallite_ore`): a
  stone pickaxe mines them (as tin, zinc and lead), each drops one **Raw Thallite** (`jugcraft:raw_thallite`), Fortune
  works as on vanilla's ores (vanilla's `ore_drops` formula) and Silk Touch gives the ore.
- **Smelting:** raw thallite or either ore, furnace (200 ticks) or blast furnace (100 ticks), 0.7 xp, gives a
  **Thallite Ingot** (`jugcraft:thallite_ingot`). Thallite dust smelts back into an ingot the same way (0.1 xp).
- **9 to 1 both ways:** nuggets and ingots (`jugcraft:thallite_nugget`), ingots and the **Block of Thallite**
  (`jugcraft:thallite_block`), raw thallite and the **Block of Raw Thallite** (`jugcraft:raw_thallite_block`).
- **The lore line:** the ingot's tooltip carries "Green as a new shoot." in grey italics (`tooltip.jugcraft.thallite_ingot`).
- **No alloy**, on purpose.

**Worldgen** (new chunks only; no retrogeneration):

| Placed feature | Veins | Size | Y (trapezoid) | Where |
|---|---|---|---|---|
| `jugcraft:ore_thallite` | 4 a chunk | 7 | −32 to 48 | every Overworld biome |
| `jugcraft:ore_thallite_rich` | 6 more a chunk | 9 | −32 to 48 | the biome tag `#jugcraft:has_ore/thallite_rich`: `minecraft:lush_caves` and `jugcraft:glowcap_grotto` |

- Both replace stone with the ore and deepslate with the deepslate ore, like every Jugcraft ore.
- **Never discarded on air exposure:** both have `discard_chance_on_air_exposure` 0.0, as every Jugcraft ore already
  has, so the rich veins show in cave walls.
- **With the `biomes` switch off** the Glowcap Grotto is never placed; Lush Caves still carry the rich veins, since the
  tag names them directly.
- **How it is built, generically:** a metal's or mineral's entry in `tools/materials.py` may now have `rich_gen`, a
  second worldgen entry placed as `ore_<name>_rich`, and either entry may name `biomes` (biome ids or `#` tags), as rocks
  already do. `tools/generate_material_data.py` writes those biomes as the biome tag `jugcraft:has_ore/<placed feature>`,
  `JugcraftWorldgen` places the listed veins (its `biomeOres` table) by that tag, and `tools/check_mod_data.py` checks
  that Java, the tag and the table agree. Rocks keep their own `biomes`, read in Java as before.

**Processing, through the shared systems** (thallite is in `COMPONENTS["dust"]`, so also `WASHED_ORES`, and in
`COMPONENTS["plate"]`; `BYPRODUCTS["thallite"]` in `tools/machines.py`):

| Route | Per ore block | As the record asks |
|---|---|---|
| Crusher | 2 raw thallite | yes |
| Pulverizer | 2 thallite dust, and a 10% chance of 1 iron dust | yes |
| Ore Washer (500 mB water) | 3 washed thallite ore, each pulverized into 1 dust (with the same 10% iron dust roll) | yes |
| Acid leaching (Chemical Reactor, 250 mB sulfuric acid) | 4 washed thallite ore | yes |

- The Pulverizer also grinds raw thallite or an ingot into 1 dust, as for every dust metal.
- **Thallite Plate** (`jugcraft:thallite_plate`, `#c:plates/thallite`): the Metal Press makes 1 from an ingot. **By
  hand:** 2 ingots stacked in a column at a crafting table make 1 plate (half the metal lost), the dearer route. It is
  the only hand-made plate in the mod; it is built generically (`hand_plate` on a METALS entry, recipe
  `thallite_plate_by_hand`).
- New items for this: **Thallite Dust** (`jugcraft:thallite_dust`, `#c:dusts/thallite`) and **Washed Thallite Ore**
  (`jugcraft:washed_thallite_ore`).
- **Tags** (all automatic for a METALS entry): `c:ores/thallite`, `c:ores_in_ground/stone` and `/deepslate`,
  `c:ore_rates/singular`, `c:raw_materials/thallite`, `c:ingots/thallite`, `c:nuggets/thallite`,
  `c:storage_blocks/thallite` and `/raw_thallite`, `minecraft:mineable/pickaxe`, `minecraft:needs_stone_tool`.
- **The Ore Drill** takes thallite ore through `c:ores`. **The Prospector** hears it: it is a family in
  `OreSurvey.FAMILIES`, which is a fixed list, so it was added there by hand.
- **No mechanical gear** (`thallite_gear`): the record names only a plate and dust for machines.

**The feature switch `thallite`** (`config/jugcraft.properties`, `thallite.enabled`): off stops both worldgen features
and every thallite recipe (smelting, compaction, crushing, pulverizing, washing, leaching, dust smelting and the
hand-made plate) through `jugcraft:feature_enabled`. Every block and item stays registered whatever the switch.
- **Like every metal,** the Metal Press's ingot-to-plate recipe is gated by `machines` only, so with `thallite` off a
  player can still press thallite ingots they already have.

**Art** ([MATERIAL_SETS.md](../MATERIAL_SETS.md)): the ore overlays (layout `ore_a`), raw ore, raw block, nugget, ingot
and storage block are drawn from the shared maps in `tools/material_icons/` in thallite's two ramps:
- **Its metal** (ingot, nugget, storage block): the owner's chartreuse sheet, `303f12 4e611d 7c8a37 aab053 dbdd85`
  (outline, dark, mid, light, highlight).
  - **One nudge:** the sheet's outline is `354514`. That sits only 25 luma under the dark tone, and
    `tools/check_mod_data.py` asks every metal for 30, so the outline is darkened to `303f12`. That keeps its hue and
    saturation and moves it by CIEDE2000 2.0. Nothing else moved.
  - Its nearest metals are uranium (CIEDE2000 15.9 over the ingot's body) and brass (17.9), well over the floor of 8.
- **Its ore** (overlay, raw ore, raw block): the sheet's sage, `4a5a2e 6e8048 98a86a c0cc8e e4ecb8`.
- **The plate, dust and washed ore** are drawn from the metal ramp too (`part_palette` in `tools/generate_textures.py`
  now falls back to a metal's material-set ramp). The older metals keep their old part palettes.

**Code and data changed:**
- `tools/materials.py` (the METALS entry, the switch, `rich_gen`, `ore_gens()`), `tools/machines.py` (the byproduct),
  `tools/generate_material_data.py` (rich veins, biome tags, the hand-made plate, the lore line),
  `tools/material_icons.py` (the ramps), `tools/generate_textures.py` (part palette), `tools/handbook.py` (the Ores page
  says where it is richer) and `tools/check_mod_data.py` (the lore and the biome-limited veins).
- `MetalFamily` (`Builder.lore()`), the new `LoreItem`, `JugcraftMaterials`, `JugcraftComponents`,
  `JugcraftWorldgen`, `JugcraftConfig` and `OreSurvey`.
- Tests: `ThalliteGameTests` and `ThalliteClientGameTests`.

## Slice 2, as built: gear
Branch `claude/thallite-gear`. It is stacked on `claude/thallite-ore` and merges the two open PRs whose systems it uses:
- the armor-style system of [#215](https://github.com/jimbozoomer-byte/jugcraft/pull/215) (`claude/armor-styles`);
- the Shift trait details of [#216](https://github.com/jimbozoomer-byte/jugcraft/pull/216) (`claude/trait-details`).

Until those merge, its diff against `claude/thallite-ore` shows their changes too.

**What a player gets:**
- **Tools:** `jugcraft:thallite_<sword|pickaxe|axe|shovel|hoe>`.
  - Crafted from thallite ingots and sticks in iron's shapes, and repaired with thallite ingots
    (`#jugcraft:repairs_thallite_gear`, which holds `#c:ingots/thallite`).
  - Iron's attack numbers (the axe 6 + 2, -3.1; the hoe -2, -1), iron's drops and speed, 200 uses, enchantability 18.
- **Armor:** `jugcraft:thallite_<helmet|chestplate|leggings|boots>`, iron's defense (2, 6, 5, 2), no toughness or
  knockback resistance, enchantability 18. The durability multiplier is 13: 143, 208, 195 and 169 uses.
- **Earthbound armor:** `jugcraft:earthbound_thallite_<piece>`.
  - The same numbers in a gold-trimmed look (equipment asset `jugcraft:earthbound_thallite`), plus Rooted.
  - It is the `earthbound_thallite` entry in `tools/gear.py` `ARMOR_STYLES`: one-way (`"reversible": False`, so no recipe
    turns it back) and with a perk (`"perk": "rooted"`).
- **The Earthbinding Template** (`jugcraft:earthbinding_template`, uncommon): rooted dirt in the centre, a thallite
  nugget on each side and a gold nugget in each corner make one.
  - At a smithing table, the template, a thallite armor piece and a gold ingot make the Earthbound piece. Vanilla's
    smithing keeps its enchantments, wear, name and trim.
  - A full set takes 4 templates and 4 gold ingots: 16 gold nuggets and 4 ingots, about 5.8 ingots of gold.
- **No paxel and no arms yet.** Thallite is not in `PAXEL_TIERS`. Its `GEAR_TIERS` entry has `"arms": False`, so
  `tools/arms.py` `METALS` (and `JugcraftArms.METALS`) stay bronze and steel until slice 3 adds its drawn kinds.

**The traits** (`gear/ThalliteGear`, numbers in `tools/gear.py` and checked against Java by
`tools/check_mod_data.py`):
- **Who:** a server tick handles players only, never mobs or armor stands. Every 10 ticks it reads the block under each
  player's feet: in the air or in water there is none.
- **Regrowth, every 100 ticks:**
  - Each stack in `#jugcraft:thallite_gear` (all 13 thallite pieces) in either hand or an armor slot loses one damage if
    it has more than the cap's. The cap is `max - max × 75 / 100`: 50 of 200 for a tool, 36 of 143 for a helmet.
  - It works on `#jugcraft:living_ground`, or on `#jugcraft:earthen_ground` with 2 or more Earthbound pieces worn.
  - When anything mended, two happy-villager sparkles show at the player.
- **Rooted, every 10 ticks:** the transient knockback resistance modifier `jugcraft:rooted` is removed. If the player
  stands on `#jugcraft:earthen_ground`, it is added back at 0.075 for each piece in `#jugcraft:earthbound_armor` worn.
  It is never saved.
- **Natural ground,** `#jugcraft:earthen_ground`: `#jugcraft:living_ground`, `#minecraft:base_stone_overworld` (stone,
  granite, diorite, andesite, tuff and deepslate), `#minecraft:sand` and gravel, with stone, deepslate and sand by name
  too.
- **Why soils are named:** the first CI run (job 112483970162) showed grass outside `#minecraft:dirt` in 26.3, so
  Regrowth and Rooted did nothing on grass. Each ground tag now names its blocks as well as vanilla's tags.
- **The tooltip** (`gear/ThalliteGearItem`):
  - every piece names **Regrowth** (green), and an Earthbound piece also its lore line first and **Rooted** (gold);
  - while Shift is held, each is described, through #216's `TraitTooltips`;
  - the texts are `tooltip.jugcraft.thallite.<trait>` and `.trait`, from `tools/gear.py` `TRAITS`.
- **The handbook:** two pages after Steampunk and Kaiser Armor, "Thallite Gear" (with the pickaxe's recipe) and
  "Earthbound Armor" (with the template's).

**Art:**
- **The tools** are the material-set maps (`tools/material_icons/<tool>.txt`) in thallite's metal ramp, as bronze's and
  steel's are. `material_icons.TOOL_METALS` now includes thallite, and `PAXEL_METALS` keeps the paxel to bronze and steel.
- **The armor icons, worn layers and template** are drawn in `tools/thallite_armor.py` (called from
  `tools/gear_textures.py`), after the owner's sheet: a dome helmet with a visor, a breastplate with a round green boss,
  green gems at the knees and boot cuffs, and gold trim on the Earthbound pieces. They are drawn fresh; the sheet is not
  committed or traced.

**Code and data changed:**
- **Tools:**
  - `tools/gear.py`: the tier, the style, and the traits' numbers, texts and tags;
  - `tools/arms.py`: `METALS` skips a metal with `"arms": False`;
  - `tools/material_icons.py` and `tools/gear_textures.py`;
  - `tools/thallite_armor.py` (new);
  - `tools/arms_variants_art.py`: the parchment, shared with the template;
  - `tools/handbook.py`;
  - `tools/check_mod_data.py`: `check_thallite_gear`.
- **Java:** `gear/JugcraftGear`, and the new `gear/ThalliteGear` and `gear/ThalliteGearItem`.
- **Tests:** the new `ThalliteGearGameTests`, and more scenes in `ThalliteClientGameTests`.

## Verification
Slice 1. Run locally on 6 October 2026, on the working tree of `claude/thallite-ore`:
- **`python3 tools/generate_textures.py`:** 10 new PNGs (the four block textures, raw thallite, ingot, nugget, dust,
  plate and washed ore). No existing PNG changed.
- **`python3 tools/generate_material_data.py`:** the thallite data (models, blockstates, item definitions, loot tables,
  recipes, tags, both worldgen features and the biome tag), plus the shared files that list every ore or plate (the
  `c:` and vanilla tag lists, the lang file, the handbook and the recipe view). No other file changed.
- **Determinism:** a second run of both generators gave an identical tree (every file under `src/main/resources` and
  `tools` hashed after each run).
- **`python3 tools/check_mod_data.py`:** `PASS: 1447 material IDs, data files and recipe audit.` (1437 before.)
- **Negative test of the new checks:**
  - **What was broken** (temporary edits, then restored): the rich veins moved into `JugcraftWorldgen`'s Overworld-wide
    list, `.lore()` dropped from the builder and Lush Caves' partner removed from the biome tag.
  - **Result:** four errors and exit 1. Restored, it passed again.
- **`python3 scripts/check_repository.py`:** PASS.
- **Java syntax only** (a parser, not a compiler): the seven new or changed main classes and the two test classes parse.
- **A preview sheet** of the textures at 6x, beside the owner's sheet, was rendered and looked at (scratch, not committed).
  The ores are shown over our stand-in rock, since vanilla's stone is not on the machine.

Written, not run (CI only):
- **`ThalliteGameTests`** (server):
  - registration, the lore item, tags and tool tier;
  - drops with a stone pickaxe, Silk Touch and Fortune III;
  - smelting and blasting, and 9 to 1 both ways;
  - every processing yield above and the Metal Press and hand-made plates;
  - a powered Pulverizer grinding thallite ore into two dusts;
  - both placed features loading, the rich biome tag holding Lush Caves and the Glowcap Grotto and not plains or
    dripstone caves, and Lush Caves (not plains) carrying the rich veins after Fabric's biome modifications;
  - both veins placed into stone and deepslate making the right ore;
  - the switch existing, on by default, its condition following it, and every thallite recipe loading.
- **`ThalliteClientGameTests`**, shots `jugcraft_thallite_*`:
  - the ore wall (vanilla's iron, copper and gold, thallite, then Jugcraft's uranium and tin, stone over deepslate),
    whole and close up;
  - the ores in the floor;
  - vanilla's raw iron, nugget, ingot, iron dust, plate and washed ore in frames above thallite's;
  - the iron and thallite blocks with their raw blocks on top;
  - the survival inventory;
  - the ingot in the hand.
  - It also reads the lore line back through the client's language.

Not run: `./gradlew build` (nothing here has been compiled), both test classes, the client, play, a dedicated server
with two clients, and the veins' real rates in a generated world (the tests check the features and their biomes, not
how much ore a chunk gets). Update this list once CI has run.

Slice 2. Run locally on 6 October 2026, on the working tree of `claude/thallite-gear`:
- **`python3 tools/generate_textures.py`:** 18 new PNGs (the five tools, the eight armor icons, the template and the four
  worn layers). No existing PNG changed: the Steampunk and Kaiser patterns, redrawn through `parchment16`, came out
  pixel-identical.
- **`python3 tools/generate_material_data.py`:** the 14 items' models and definitions, the two equipment assets, 14
  recipes, the four new tags and the repair tag, and the shared files that list gear (vanilla's tool and armor tags, the
  lang file and the handbook). No other file changed.
- **Determinism:** a second run of both generators changed nothing.
- **`python3 tools/check_mod_data.py`:** `PASS: 1489 material IDs, data files and recipe audit.` (1475 before).
- **Negative test of `check_thallite_gear`:**
  - **What was broken** (temporary edits, then restored): Java's `ROOTED_PER_PIECE` set to 0.08, a piece dropped from
    `#jugcraft:earthbound_armor`, and Rooted's description removed from the lang file.
  - **Result:** three errors and exit 1. Restored, it passed again.
- **`python3 scripts/check_repository.py`:** PASS.
- **Java syntax only** (a parser, not a compiler): the three changed or new main classes and the two test classes parse.
- **Art previews** (scratch, not committed), looked at:
  - every icon at 1×, 2× and 8× on light and dark slots, beside the owner's sheet;
  - the worn layers on a rough box-model render, front, back and three-quarter, plain and Earthbound.

  A script checked the maps: 16×16 icons, a closed outline with no fill touching air, no stray outline pixels or
  pinholes, plain and Earthbound with the same silhouettes, and 64×32 layers.

Written, not run (CI only):
- **`ThalliteGearGameTests`** (server):
  - every piece's numbers, tags, asset and class;
  - the template's recipe, and one-way Earthbinding keeping enchantments, wear and name (and no tool bound);
  - Regrowth on grass, farmland and moss but not stone or air, stopping at the cap, and on stone and sand with two
    Earthbound pieces;
  - Rooted per piece, and none off natural ground;
  - an armor stand's footing on grass;
  - the traits in every piece's tooltip, folded and expanded.
- **`ThalliteClientGameTests`**, new shots:
  - `jugcraft_thallite_gear_icons` (tools, template and both armor sets in frames);
  - `jugcraft_thallite_gear_worn` and `_worn_back` (two armor stands);
  - `jugcraft_thallite_gear_tooltip` and `_tooltip_details` (the Earthbound chestplate hovered, folded and with details).

Not run for slice 2: `./gradlew build`, both test classes, a real player standing on grass (the tick is tested through
its parts, not end to end), the client, play, and a dedicated server with two clients.

## World and event applicability
- **Overworld worldgen only:**
  - the rich pockets lie in vanilla Lush Caves and Jugcraft's Glowcap Grotto;
  - with the biomes feature off, Lush Caves still carry them.
- **No bosses, dimensions or seasonal content.**

## Rollout and open questions
- **Slice 1, for the owner:**
  - **The outline nudge:** `354514` to `303f12` (above). The other way is to exempt thallite from the 30-luma rule,
    which would weaken a check.
  - **The ore on stone:** the sage blobs read paler on light stone than on the sheet. That is the approved ore ramp on
    the shared `ore_a` layout; the client screenshots will show whether to darken it.
  - **The hand-made plate's cost:** 2 ingots for 1 plate. The record asks only for "dearer". It makes thallite the one
    metal with a hand-made plate, an exception to "plates need their machines" (TECH_TREE.md).
  - **The Metal Press recipe** follows every metal in being gated by `machines` only (above).
  - **The lore line** sits on the ingot alone.
- **Save compatibility:** slices 1 and 2 only add IDs; nothing is renamed or removed. Existing worlds get thallite only
  in new chunks. Rooted's modifier is transient, so nothing about it is saved.
- **Slice 2, for the owner:**
  - **The armor art** follows the sheet's design but is drawn fresh, so it is boxier and a little darker than the sheet.
    If the owner wants their own armor icons used exactly, as with the ingot, each is a 16-row map in
    `tools/thallite_armor.py`.
  - **Living soil** is the record's list by name plus vanilla's dirt, grass and mud tags, so it may also count a few
    soils beyond the list, such as mycelium or pale moss.
  - **The particle** is vanilla's green happy-villager sparkle, not a leaf: a leaf particle needs an API this mod has
    not compiled against yet.
  - **Players only:** mobs that pick up thallite gear get neither trait.
  - **The feature switch** stops the recipes, but gear already made keeps its traits.
- **Approved by the owner on 6 October 2026:** the numbers above; the two long blades (the big one a greatsword, the other a
  longsword); and the horse armor left to us (the second drawing).
- **Tooltips:** Regrowth, Rooted and Rooting (the arrow) are named in the tooltip and described while Shift is held, as
  the owner asked: "make it so if the player holds shift while hovering their mouse over gear that has more complex traits
  like Thallite it expands the hovering UI and has a brief description of the unique traits".
- **First magic PR:** spell costs come from the shared magic resource. Until it exists, the Focus spends its own
  durability and does not regrow, so it is never a hidden mana bar.
