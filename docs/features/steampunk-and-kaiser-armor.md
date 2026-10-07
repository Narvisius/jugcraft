# Steampunk Armor and Kaiser Armor (batch 60)

Status: implemented on `claude/armor-styles` (from `main` at ca938b54), awaiting review. The Python generators and checks pass locally. **Not yet compiled, game-tested or played:** the Java compiles only in CI, which has not run on this branch yet.
Proposal issue: none. The owner, 5 October 2026: "Wait no also I want the old armors for those 2 back those were stylized, so maybe make those be "Kaiser Armor" and "Steampunk Armor" instead and then make the new ones just be bronze and steel". On the numbers: "same stats as the metal is good for now". The looks themselves started on 2 October 2026: "make sure the armor looks really cool and the copper and bronze armor is steampunk and the other one is kaiserpunk" ([tools-and-armor.md](tools-and-armor.md)).
Batch number: 60. Main has 56 (Arms VII), Arms VIII is 59, and its record says other open work claims 57 and 58. If another PR takes 60 first, this moves up.
Owner: jimbozoomer-byte
Target milestone and tier: bronze (Steampunk, the `tin` feature) and steel (Kaiser, the `machines` feature), on top of [tools-and-armor.md](tools-and-armor.md).
Primary specialty and supported player role: everyone. A cosmetic set for fighters and builders.

**Bronze and steel armor keep their current look in this PR.**
- They still wear the stylized looks drawn for them on 2 October 2026. So for now a Steampunk piece looks exactly like a bronze piece, and a Kaiser piece exactly like a steel piece, pixel for pixel, worn and in the inventory.
- Smithing one into the other changes the item's name, lore line and ID. It does not change its picture yet.
- **A following PR gives bronze and steel armor their new 3D look:** a knight design the owner drew ([knight-armor.md](knight-armor.md)). Steampunk and Kaiser Armor keep today's looks after that; a check pins their pixels (see Verification).

## Player experience
Two new four-piece armor sets, each a metal's armor in a stylized look:

| Set | Made from | Look |
| --- | --- | --- |
| **Steampunk Armor** (`jugcraft:steampunk_helmet`, `_chestplate`, `_leggings`, `_boots`) | bronze armor | **Helmet:** a bronze-crowned aviator cap with leather ear flaps and valve ear cups, and teal-glassed goggles on the brow (on the outer hat layer, so they stand proud of the cap). **Chestplate:** a bronze breastplate with a pressure gauge, a copper boiler on the back, leather straps and a buckled belt; bronze pauldrons with copper bosses, and leather bracers. **Leggings:** riveted thigh plates with copper knee cops. **Boots:** buckled leather with bronze cuffs and toe caps. |
| **Kaiser Armor** (`jugcraft:kaiser_helmet`, `_chestplate`, `_leggings`, `_boots`) | steel armor | **Helmet:** a black leather Pickelhaube with a gold star plate, a gold spike on a four-armed base (raised on the hat layer) and a steel brim; chin scales run down the cheeks. **Chestplate:** a field-grey tunic with a red-and-gold collar under a steel cuirass, a double row of gold buttons, a medal on its ribbon, a black belt with a gold buckle, gold-fringed epaulettes and red-piped cuffs. **Leggings:** field-grey breeches with red side piping and steel knee plates. **Boots:** tall polished black jackboots with steel toe caps. |

**How to make them.** At a vanilla smithing table:

| Result | Template | Base | Addition |
| --- | --- | --- | --- |
| a Steampunk piece | Steampunk Pattern | the bronze piece of the same slot | copper ingot |
| a Kaiser piece | Kaiser Pattern | the steel piece of the same slot | gold ingot |
| a bronze piece (back) | Steampunk Pattern | the Steampunk piece | bronze ingot |
| a steel piece (back) | Kaiser Pattern | the Kaiser piece | steel ingot |

The template and the addition are used up. Smithing copies the piece's components onto the result, as it does for the exosuit liveries' charge. So the piece's enchantments, wear, custom name, repair cost, armor trim and electroplating (`jugcraft:plating`, and nickel's raised durability) should carry over both ways. That is vanilla behaviour and has not yet been game-tested for these sets.

**The patterns** (crafting table, shaped; each craft makes 4, one for each piece of a set):

```
Steampunk Pattern                    Kaiser Pattern
C L C   C = copper ingot             N C N   N = gold nugget
G P G   L = leather                  B P B   C = Imperial Crest (Kaiserworks)
C L C   G = glass pane (goggles)     N R N   B = black dye
        P = paper                            P = paper, R = red dye
```

The Imperial Crest is a Kaiserworks building block ([kaiserworks.md](kaiserworks.md)): 3 black lacquer plates, 4 gold nuggets, a gold ingot and a red dye make 2. Black lacquer plates come from 8 iron plates and a black dye. The crest is in the Building tab, so the Kaiser Pattern's tooltip and the handbook name it.

**Other details:**
- **Same protection as the plain piece:** see Balance. Each styled piece has one grey lore line:
  - "Steampunk: bronze and copper, with goggles and a boiler on the back. Protects as bronze armor does."
  - "Kaiser: field grey and gilt, the parade dress of the Winged Cog. Protects as steel armor does."
- **Pattern tooltips** say what each pattern does with which ingot, both ways, and that enchantments and wear are kept.
- **Rarity:** the styled pieces are common, like their plain pieces, so a yellow name never suggests an upgrade. The patterns are uncommon, like the Arms VII patterns and vanilla's smithing templates.
- **Creative tabs:** the pieces are in Combat, and the patterns in Ingredients.
- **Advancements:**
  - **Goggles On** ("Smith a Steampunk Pattern onto a piece of bronze armor"), under Bronze Age.
  - **On Parade** ("Smith a Kaiser Pattern onto a piece of steel armor"), under Suited Up.
  - **Suited Up** now also counts the four Kaiser pieces, so one got by trade still earns it.
- **Handbook (Steel chapter):** a "Steampunk and Kaiser Armor" page with the Steampunk Pattern's grid, and a "Kaiser Pattern" page with its grid and the crest route, right after "Bronze and Steel Gear". The Progression chapter's Bronze Age stage gains one line pointing bronze-age players to the Steampunk Pattern.
- **The patterns' art:** a 16×16 roll of parchment with curled ends (`tools/arms_variants_art.py` `pattern16`), at vanilla's item size, as [ITEM_ICONS.md](https://github.com/jimbozoomer-byte/jugcraft/blob/claude/arms-icons-16/docs/ITEM_ICONS.md) (PR #201) asks of every new icon. The four Arms VII patterns stay legacy 32-pixel icons until they are redrawn. The Steampunk Pattern shows a pair of brass-rimmed goggles with teal glass on a leather strap. The Kaiser Pattern shows a black spiked helmet with a gold spike, plate and brim, in black and gold only.
- **No real insignia.**
  - Kaiser Armor is the parade dress of the mod's own Winged Cog empire. Kaiserworks says the crest "belongs to an empire of our own, with no real nation's arms".
  - No real nation's arms, eagle, national cross or colours appear in any text or pixel. The art has a generic star plate and a plain medal.
  - The only cross-shaped detail is the helmet's four-armed spike mount, which `tools/armor_styles.py` calls a "gold cruciform spike base". It is the ordinary fitting of a spiked helmet, not an emblem.

## Connections
- **Existing input producer:**
  - bronze armor (bronze from the alloy smelter or crafting) and steel armor (steel from the steel foundry): [tools-and-armor.md](tools-and-armor.md);
  - for Steampunk: copper ingots, leather, glass panes and paper;
  - for Kaiser: gold ingots and nuggets, black and red dye, paper, and the Imperial Crest, from black lacquer plates and the Metal Press's iron plates ([kaiserworks.md](kaiserworks.md)).
- **Existing output consumer:** the player, wearing the armor. The back recipes give the plain piece, which feeds everything plain armor already feeds. Nothing else takes a styled piece today.
- **Unlocks:** nothing is gated behind either set. They earn the two advancements, Goggles On and On Parade, and a Kaiser piece also counts for Suited Up.
- **Failure behavior:**
  - **Wrong inputs:** a wrong template, base or addition matches no recipe, so the smithing table shows no result and uses nothing (vanilla behaviour). For example: an iron helmet as the base; a bronze piece with the Kaiser Pattern; a steel piece with the Steampunk Pattern; the right pattern and base with the wrong addition.
  - **No cross-style conversion:** a Steampunk piece with the Kaiser Pattern, or a Kaiser piece with the Steampunk Pattern, matches nothing.
  - **Feature switch off** (`tin` or `machines`): the recipes go. The items, equipment assets and any armor players own stay registered and keep rendering (not yet tested).
  - **Plated or trimmed pieces** keep their plating and trim through smithing, both ways (vanilla copies the components; not yet game-tested).
- **Technology connection:** the bronze and steel rungs; smithing; the Metal Press (iron plates for the crest). The electroplating bath plates any damageable item, so it plates the styled pieces too.
- **Magic connection:** none. These are cosmetic, so no resource links apply beyond their costs.
- **Reachable entry path (no circular unlock):**
  - **Steampunk:** copper and tin, then bronze; bronze armor; the pattern, from vanilla items; a smithing table.
  - **Kaiser:** the Metal Press, then iron plates; the steel foundry, then steel armor; iron plates and black dye, then black lacquer plates; the Imperial Crest, then the pattern; a smithing table.
  - Steel armor already needs iron plates, so the crest adds no new gate. Nothing on either path consumes a styled piece, and the back recipes give a plain piece, which was already reachable.
- **Required vs optional; trade and solo routes:** optional. The plain sets protect exactly as well. Every input is an ordinary, tradeable item, and both routes work solo. Nothing is seasonal or boss-gated.
- **How the specialty stays useful:** a style is only a look, never a stronger tier. Plain bronze and steel armor stay as good, so no player is pushed down either path.
- **Shared mechanism:** `ARMOR_STYLES` in `tools/gear.py` and `JugcraftGear.restyle` are the mod's one armor-style system.
  - Thallite's planned Earthbound armor is designed the same way: template, plain piece and gold ingot give a separate, one-way item. It is on the `claude/thallite` branch, in docs/features/thallite.md.
  - It should be an `ARMOR_STYLES` entry with `"reversible": False` and `"perk": "rooted"`, not a second system.

## Balance and automation
**The numbers are the metal's, unchanged:**

| Set | Durability multiplier | Boots/legs/chest/helmet | Max durability (helmet/chest/legs/boots) | Enchantability | Toughness | Knockback res. | Repair | Rarity |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Bronze | 15 | 2/5/6/2 | 165/240/225/195 | 12 | 0.5 | 0 | `#jugcraft:repairs_bronze_gear` | common |
| **Steampunk** | 15 | 2/5/6/2 | 165/240/225/195 | 12 | 0.5 | 0 | `#jugcraft:repairs_bronze_gear` | common |
| Steel | 25 | 3/6/7/3 | 275/400/375/325 | 10 | 1.5 | 0.05 | `#jugcraft:repairs_steel_gear` | common |
| **Kaiser** | 25 | 3/6/7/3 | 275/400/375/325 | 10 | 1.5 | 0.05 | `#jugcraft:repairs_steel_gear` | common |

All four sets use the iron equip sound and are repaired at an anvil with their metal's ingot.

**Why identical:**
- **The request is about looks.** The owner said "those were stylized" and "same stats as the metal is good for now".
- **The mod's precedent for pure looks is the exosuit liveries.** They are separate IDs that differ only in their equipment asset, with every number shared ([exosuit.md](exosuit.md)).
  - Arms VII is not the precedent. There "a style is a look and a small perk, not a stronger tier" ([arms-vii.md](arms-vii.md)), and its styles do change numbers.
- **No forced route.** A stat edge would make plain bronze and steel just a step on the way.
- **Durability must match.** Smithing copies the absolute damage. Equal maximum durability means restyling is never a free part-repair, and never leaves an over-worn item.

**How "identical" is enforced:** in Java the styled materials are derived from the base material, never copied: `STEAMPUNK_ARMOR = restyle(BRONZE_ARMOR, "steampunk")` and `KAISER_ARMOR = restyle(STEEL_ARMOR, "kaiser")`. `restyle` keeps every number, the sound and the repair tag, and changes only the equipment asset. `tools/check_mod_data.py` asserts both exact lines.

**Cost of a full set, on top of the plain set:**
- **Steampunk:** 1 crafting-table craft and 4 smithings: 8 copper ingots, 2 leather, 2 glass panes and 1 paper. No machine or power.
- **Kaiser, the first set from scratch:** 3 crafting-table crafts and 4 smithings.
  1. 8 iron plates and 1 black dye make 8 black lacquer plates.
  2. 3 lacquer plates, 4 gold nuggets, 1 gold ingot and 1 red dye make 2 Imperial Crests.
  3. 1 crest, 4 gold nuggets, 2 black dye, 1 red dye and 1 paper make 4 Kaiser Patterns.
  4. 4 smithings, each with a gold ingot.
  - In all: 8 iron plates, 3 black dye, 2 red dye, 1 paper, 5 gold ingots and 8 gold nuggets (about 5.9 gold ingots). Left over: 5 lacquer plates and 1 crest.
- **Kaiser, each later set:** with the spare crest, 1 pattern craft and 4 smithings (4 gold ingots and 4 nuggets, about 4.4 gold ingots). The 5 spare lacquer plates make the next pair of crests.
- **Going back** costs one pattern and one ingot of the metal per piece, so a look is never a free toggle.

**Why a pattern craft makes 4:** the rule is one craft per thing restyled. For Arms VII the thing is one arm, so its patterns make 1; here it is a four-piece set. It also shortens the Kaiser route.

**Metal audit** (nugget units: an ingot is 9; `tools/check_mod_data.py`):
- A styled piece counts as its plain piece only: a Steampunk helmet holds bronze 45, a Kaiser chestplate steel 72. A pattern holds nothing.
- Every smithing loses metal, which the audit allows. A helmet forward: the plain helmet (45) and the addition (9) go in, 45 comes out. A helmet back: 45 and the metal's ingot (9) go in, 45 comes out.
- The bronze rule holds both ways: a bronze-only input never gives another metal.
- **No loop:** nothing turns armor back into ingots, and every smithing uses up a pattern and an addition.

**Automation:** none. Only the vanilla smithing table makes these. A pattern is an ordinary crafting recipe.

## Multiplayer and persistence
- **Server authority:** smithing is vanilla's server-side menu. There are no new packets, no new saved state and no client code; the look comes only from the equipment asset key in the material.
- **Saved state:** only item stacks, with IDs that are permanent from release:
  - `jugcraft:steampunk_helmet`, `steampunk_chestplate`, `steampunk_leggings`, `steampunk_boots`;
  - `jugcraft:kaiser_helmet`, `kaiser_chestplate`, `kaiser_leggings`, `kaiser_boots`;
  - `jugcraft:steampunk_pattern` and `jugcraft:kaiser_pattern`;
  - the equipment assets `jugcraft:steampunk` and `jugcraft:kaiser`.
- **Existing bronze and steel armor:** nothing changes in this PR. The IDs, equipment assets (`jugcraft:bronze`, `jugcraft:steel`), numbers, components, recipes and pictures are the same. No migration, datafixer or backup step is needed to install this build. There is no numbered release yet. For going back to an older build, see Rollout.
- **When bronze and steel get their new look (the following PR):** bronze and steel armor already in worlds will show the new look. A player who wants to keep today's look can smith each piece into Steampunk or Kaiser Armor, keeping its enchantments and wear.
- **Feature switches:** the Steampunk recipes, the Steampunk Pattern and the way back to bronze follow `tin`. The Kaiser recipes, the Kaiser Pattern and the way back to steel follow `machines`. Disabling either removes those recipes only; the items stay registered.
- **Two players:** nothing is shared or contested beyond vanilla's one-player smithing menu.

## Dependencies and assets
- **No new dependencies.** No platform pin, mixin, packet or per-tick work is added.
- **Armor art:** `tools/armor_styles.py`. It is original pixel art, drawn row by row on 2 October 2026 for bronze and steel armor, and now keyed `steampunk` and `kaiser`.
  - Both five-shade metal palettes are pinned in `PALETTES`, so a later repaint of the ingots cannot shift these looks.
  - Kaiser's palette is the steel ingot's of 2 October 2026. Steel armor used to follow the steel ingot's palette at run time; it now uses this pinned copy, which is identical today.
- **Bronze and steel armor** wear the knight armor's 3D models ([knight-armor.md](knight-armor.md)) and have no equipment asset. `tools/gear_textures.py` (`METAL_ARMOR_LOOK`) still writes their flat 64x32 layers in these looks, as a fallback nothing draws.
- **Pattern art:** `tools/arms_variants_art.py` `pattern()`, the painter of the Arms VII patterns. `TEAL` (the goggle glass) and `BLACK_LEATHER` (the helmet's leather) are new inks there.
- Nothing is traced or recoloured from Mojang's or any other mod's art.
- **Generated by:**
  - `python3 tools/generate_material_data.py`: the 10 item definitions and models, the 2 equipment assets, 18 recipes (8 forward, 8 back, 2 patterns), the four `minecraft:*_armor` tags, the lang, the handbook, the 2 new advancements and the changed Suited Up;
  - `python3 tools/generate_textures.py`: 14 new PNGs (8 icons, 4 worn layers, 2 patterns). No other PNG changes.

## Verification
**Run locally** on `claude/armor-styles`, from `main` at ca938b54, with the changes not yet committed. 5 October 2026.
- `python3 tools/generate_material_data.py` and `python3 tools/generate_textures.py`: both exit 0. Running both again left the tree byte-identical.
- `python3 tools/check_mod_data.py`: "PASS: 1447 material IDs, data files and recipe audit. No Minecraft build or game test performed." Its new checks:
  - `check_armor_styles`:
    - Java's `ARMOR_STYLES` and `STYLE_TEMPLATES` match `tools/gear.py`, and both `restyle(...)` lines are present;
    - bronze, steel, Steampunk and Kaiser each have 64x32 worn layers and an equipment asset naming their own texture;
    - each styled piece has its smithing recipe both ways, its slot tag and its lore line;
    - each pattern has its recipe, making 4, and its tooltip.
  - `check_armor_looks`: the 12 Steampunk and Kaiser PNGs must match, pixel for pixel, the bronze and steel armor PNGs of efd85edc (#94, the last commit that changed them).
    - It hashes the decoded RGBA pixels, not the file bytes, so another PNG encoder does not trip it.
    - Bronze and steel are deliberately not pinned, because the following PR changes them.
  - The metal audit counts a styled piece as its plain piece and a pattern as nothing.
- `python3 scripts/check_repository.py`: PASS.
- **Re-run on 6 October 2026**, with this record and the other docs in place and still uncommitted:
  - `python3 scripts/check_repository.py`: "PASS: repository structure (bootstrap) and local documentation links. No gameplay/build tests performed."
  - `python3 tools/check_mod_data.py`: the same PASS line as above, 1447 IDs.
- **Re-run on 6 October 2026, after the review fixes** (the handbook's wording about the looks; this record; kaiserworks.md):
  - `python3 tools/generate_material_data.py` and `python3 tools/generate_textures.py`: both exit 0. A second `generate_material_data.py` run left the tree unchanged, and no tracked PNG changed.
  - `python3 tools/check_mod_data.py`: the same PASS line, 1447 IDs.
  - `python3 scripts/check_repository.py`: PASS.
- **Pixel check** against efd85edc, with a scratch script:
  - all 12 new Steampunk and Kaiser PNGs are identical to the old bronze and steel PNGs, at efd85edc and at ca938b54;
  - in the working tree, the 12 bronze and steel armor PNGs have no git diff and are identical to their styled twins. "FAILURES: 0".
- **Negative tests of the new checks**, with a scratch script that patches in memory or uses a scratch copy, touching no repo file. Each of these raised an error:
  - the Java lists reordered or short;
  - the Kaiser material copied instead of derived;
  - a wrong forward addition, or a wrong result back;
  - a pattern recipe making 2;
  - a piece missing from its slot tag;
  - a wrong texture in an equipment asset;
  - a missing lore line;
  - one pixel changed in a Kaiser icon, or a Kaiser worn layer missing.

  A re-encoded PNG with the same pixels raised no error.
- **Recipe conflicts:** a scratch scan of 1,599 shaped recipes (Jugcraft's and vanilla 26.3's, tags resolved) found no grid that could match either pattern's.
- **Vanilla 26.3 armor tags:** checked by reading the 26.3 data (misode/mcmeta), not by a game test. `trimmable_armor`, `enchantable/<slot>_armor`, `enchantable/durability` and `enchantable/equippable` include the four `minecraft:<slot>_armor` tags, which hold the styled pieces. So enchanting, books and trims should work as they do for bronze and steel.

**Not run:**
- **The Java compile** (`./gradlew build`). CI will be the first compile. One known risk: `restyle` uses the `ArmorMaterial` record accessors `durability()`, `defense()`, `enchantmentValue()`, `equipSound()`, `toughness()`, `knockbackResistance()` and `repairIngredient()`, which could not be checked against 26.3 here.
- **Server game tests: written, not run.** `ArmorSetsGameTests` is listed in `fabric-gametest`, so CI's `mod` job runs it with the other server tests. It has never compiled or run.
  - `armorSetsMatchTheirMetal`:
    - Covers all 16 pieces (bronze, Steampunk, steel and Kaiser).
    - Checks each piece's slot, equipment asset, durability and enchantability.
    - Checks armor, toughness and knockback resistance against numbers typed into the test, within a millionth.
    - Checks the repair tag (the metal's ingot, not iron), common rarity and the six vanilla armor tags (`<slot>_armor`, `enchantable/armor`, `enchantable/<slot>_armor`, `enchantable/durability`, `enchantable/equippable`, `trimmable_armor`).
    - Each styled piece must have its plain piece's attribute modifiers, durability, enchantability, repair and slot, plus a lore line.
    - The two patterns must be uncommon, carry lore and not be wearable.
  - `patternsRestyleAndBackKeepEverything`:
    - For each style and piece, it builds a plain piece with Protection IV and Unbreaking III, nickel plating, a gold sentry trim (put on by vanilla's own trim recipe), 37 damage, a name and an anvil cost of 3.
    - It smiths that piece forward and back. The piece must keep all of these, and come back equal to the piece that went in.
    - Twelve wrong combinations of pattern, base and addition must match no recipe.
  - `armorSetRecipesLoad`:
    - The 8 forward and 8 back smithing recipes, both pattern recipes and the 8 plain armor recipes must load.
    - Both pattern grids must make 4.
    - Five bronze ingots in a helmet's shape must still make a bronze helmet.
- **Client game test: written, not run.** `ArmorSetsClientGameTests` is last in `fabric-client-gametest`, so it runs on client shard 2 in this branch's order. The shard moves with merge order.
  - Six armor stands, left to right: vanilla copper, bronze, Steampunk, vanilla iron, steel and Kaiser.
  - Before the stand shots, the server checks that each stand wears its four pieces and that its chestplate is drawn from its set's equipment asset. A wrong stand fails the test.
  - Shots:
    - `jugcraft_armor_sets_front`;
    - `jugcraft_armor_sets_back`, which shows Steampunk's boiler;
    - `jugcraft_armor_sets_icons`, all 24 pieces and both patterns in item frames.
  - In this PR the bronze stand will look the same as the Steampunk one, and the steel stand the same as the Kaiser one. Nobody has seen these shots yet.
  - The existing `JugcraftClientGameTests` shots (`jugcraft_steel_armor_worn`, `jugcraft_bronze_armor_worn` and `jugcraft_bronze_armor_back`) are unchanged. They still show the stylized looks.
- **Play:** client launch, the smithing table's slots accepting these patterns, JEI's smithing listing, the dedicated server and the two-client playtest.
- **Survival entry:**
  - Steampunk: in a fresh survival world, make bronze armor, craft a Steampunk Pattern, smith a piece and smith it back.
  - Kaiser: iron plates, then black lacquer plates, then an Imperial Crest, then a Kaiser Pattern, then a smithed piece and back.
- **Feature switches:** no game test starts with `tin` or `machines` off, because both are read at start-up. So "the recipes go; the items stay and render" is unchecked.

## World and event applicability
- No worldgen, loot tables, mobs, bosses or seasonal content.
- A future boss, such as the Kaiser's Zeppelin ([branches/BOSSES.md](../branches/BOSSES.md)), may drop a Kaiser Pattern as a bonus, never as the only route.

## Rollout and open questions
- **Bronze and steel's own look:** a following PR gives them the 3D knight design the owner drew ([knight-armor.md](knight-armor.md)). That PR answers the owner's request for more intricate armor models, bigger than vanilla armor, with parts that stand out from the body. This PR does not do that. Until that PR lands, each styled piece looks exactly like its plain piece.
- **Owner questions, built with their defaults:**
  - **Route and cost:** as above. Alternatives:
    - a black lacquer plate in place of the crest saves a craft and the first-time gold;
    - a gold-nugget addition in place of an ingot brings the first Kaiser set to about 2.3 gold ingots;
    - a direct crafting recipe from ingots would make a fresh piece.
  - **One-way or two-way:** two-way, as the exosuit liveries. Pure looks in the mod are two-way; looks with a perk (Arms VII, the planned Earthbound) are one-way. If the owner picks one-way, the 8 back recipes go and both pattern tooltips add "This cannot be undone."
- **Pattern art:** the owner has not seen the two pattern icons yet.
- **Known limits:**
  - **Flat art:** the spike, goggles and boiler are flat pixels on vanilla's armor layers, as before. There is no 3D render layer for these sets.
  - **Pattern size:** the two new patterns are 16×16; the four Arms VII patterns beside them in the Ingredients tab are still 32-pixel legacy icons.
  - **Coupling to Kaiserworks:** the Kaiser Pattern needs the Imperial Crest. If Kaiserworks ever gets its own feature switch, the Kaiser recipes need it too.
  - **Suited Up's icon** is the steel helmet.
  - **Display names** can change later; the IDs cannot once released.
- **Reverting to a build without these IDs:**
  - Minecraft does not load an item stack whose item ID is unknown. So going back would delete every Steampunk and Kaiser piece and pattern from inventories, containers and armor stands, along with their enchantments, trims and plating. This has not been tested.
  - Before going back, smith the pieces back to bronze or steel, or restore a world backup taken before this build was installed.
  - **Future consumers:** a recipe, machine or upgrade that takes a bronze or steel armor piece must accept its Steampunk or Kaiser twin too.
- **Release conditions:**
  - a green CI compile, server game tests and client shard;
  - someone has looked at the `jugcraft_armor_sets_*` shots;
  - the owner has looked at the patterns;
  - the play checks above.
