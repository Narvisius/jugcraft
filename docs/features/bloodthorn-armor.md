# Bloodthorn Armor: the first of the owner's armor tiers

Status: implemented on `claude/bloodthorn-armor`, awaiting review. It is stacked on `claude/knight-armor` (#227), whose 3D armor engine it uses. **Not yet compiled, game-tested or played:** the Java compiles only in CI.
Proposal issue: none. The owner sent five armor designs of their own on 6 October 2026 ("Can we start implementing the next 5 that I created. Again want you to use the the complex armor for each one to capture the shape of each piece like I have... Lets do these 1 by 1"). This is the first, a crimson set with no title on its picture, which the owner named:
- the name: "Bloodthorn Armor";
- what kind of armor: "New stronger armor tiers";
- how players get it: "They might get dropped by bosses or be craftable for now just make the armor we can figure that out later".

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: a new armor tier, a step above netherite (endgame). Its place in the game, and how it is obtained, are the owner's to decide later; until then it is creative-only.
Primary specialty and supported player role: combat (defense). Everyone who wears armor.

## Player experience
**The look:** the owner's design, worn as a 3D model bigger than vanilla armor, in crimson, coral and plum, built on the knight armor's toolkit ([knight-armor.md](knight-armor.md)):
- **Helmet:** a tall, boxy great helm, its flat front cut by three black slits (two wide, a narrower one below), over a dark neck band. Behind the head rises the upper half of a big rounded back plate, with a fan of square spikes like a crown of thorns: a tall, thick one at the centre, then a long pair and a lower pair splayed wider, all leaning back.
- **Chestplate:**
  - a high collar and a breastplate;
  - the lower half of the back plate, narrowing in steps to a lip;
  - tall pauldrons tilted up toward the outside, each a main block, a top tier and a flared lame;
  - vambraces with a diamond plate on the outer forearm, and dark gloves;
  - a magenta V over the waist, with a near-black stripe.
- **Leggings:**
  - a dark belt;
  - tassets on each thigh whose tops dip to a V at the centre line, painted in nested L's;
  - a front plate on each leg, crossed by a dark strap;
  - diamond knee plates.
- **Boots:** chunky plated boots in magenta and plum strips, notched dark at the top.
- **Colours** run from orange and coral at the helm, through red and crimson at the chest, to magenta at the arms and thighs and plum at the feet. They are painted in broken horizontal strips over a near-black purple under-layer (`armor_paint.BLOODTHORN`, sampled from the owner's render).
- **Closed all round:** what the render does not show (the sides, the inner sides of the thighs and boots, the undersides) is drawn to match it. A face is left out only where another box of the same piece covers it, so nothing shows through, even on an armor stand.
- **Size:** 43 parts and 245 quads for a full set (the cap is 900), on one 128 × 128 texture.
  - The tall centre spike rises 10 pixels above the head.
  - The low spikes and the pauldrons' lames reach about 12.7 pixels to the side. The toolkit warns past 12, since such parts may pop out at the edge of the screen.
- **Icons:** four 16 × 16 maps of its own, in `tools/armor_icons/bloodthorn/`: the helm under its spikes, the pauldrons with the V, the tassets with their knee diamonds, and the boots.

**The numbers** (`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.BLOODTHORN_ARMOR`), a step above netherite in each:

| | Bloodthorn | Netherite |
|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 3, 9, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.15 | 0.1 |
| Durability (multiplier) | 440, 640, 600, 520 (40) | 407, 592, 555, 481 (37) |
| Enchantability | 15 | 15 |
| Fire resistant | yes | yes |
| Repaired with | netherite ingots | netherite ingots |

A full set is 22 armor of the 30 vanilla allows, and 14 toughness. These are starting numbers for the owner to set, like the other four sets' when they come.

**Getting it:** for now only from the creative tab (Combat). No recipe, drop or trade exists yet, as the owner asked; the owner will decide between boss drops and crafting.

## Connections
- Existing input producer: none yet (creative only); the repair item is netherite, from vanilla's Nether.
- Existing output consumer: the player's armor slots; it enchants, trims and equips as any armor of its slot (it is in `#minecraft:head_armor` and the other slot tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("for now just make the armor"). It is not required by anything, so nothing is blocked on it.
- Which connections are required vs optional: all optional.
- For infrastructure and cosmetics: it is gear with numbers, so the entry path above is owed once the owner decides it.

## Balance and automation
- **Above netherite,** so it must come late. Its recipe or drop is the owner's to set, and it should cost at least a netherite set's worth.
- **No conversion loops:** nothing turns it back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain armor items:** nothing new is saved, and the server handles them as any armor.
- **The 3D model is client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` and the atlas `textures/entity/equipment/3d/bloodthorn.png`.
- **Stable IDs:** `jugcraft:bloodthorn_helmet`, `bloodthorn_chestplate`, `bloodthorn_leggings` and `bloodthorn_boots`, and the repair tag `jugcraft:repairs_bloodthorn_gear`.
- **No equipment asset file:** the pieces' equippable asset ID is `jugcraft:bloodthorn`, and with every piece in 3D no flat layer is drawn, so no `equipment/bloodthorn.json` is written (as for bronze and steel).

## Armor-only tiers, the shared part
The five sets are armor-only tiers with numbers of their own. This first one adds the shared code the other four reuse:
- **`tools/gear.py` `ARMOR_TIERS`:** a tier's display name, armor numbers, repair item and fire resistance. Its pieces get their names, item models, the slot tags and a repair tag; no recipes.
- **`JugcraftGear.ARMOR_TIERS`** and **`armorTier(tier, material, fireResistant)`** register the four pieces.
- **`tools/check_mod_data.py` `check_armor_tiers`** checks:
  - the list and each material against `gear.py`;
  - the registration call and the repair tag;
  - that every piece has a 3D model and an icon, and no equipment asset file.
- **Tests:**
  - `ArmorTiersGameTests` types each tier's numbers in and checks every piece;
  - `ArmorTiersClientGameTests` shows every tier in `JugcraftGear.ARMOR_TIERS`, so the next set is shown without changing it.

## Dependencies and assets
- No new dependencies.
- The design is the owner's (`docs` has no copy; the reference picture is not committed). The model, its paint and the icons are drawn by code, in `tools/bloodthorn_armor.py`, `tools/armor_paint.py` (`BLOODTHORN`) and `tools/armor_icons/bloodthorn/`. No Mojang or third-party texture is used.

## Verification
**Run locally on 7 October 2026,** on this branch after merging `claude/knight-armor` (which carries main):
- **Generators:** `generate_material_data.py`, `generate_textures.py`, then `generate_material_data.py` again, all exit 0. The merged tree is what they write, with nothing left to change.
  - Bloodthorn's own output: 9 `worn_models.json` entries, 4 names, the 4 vanilla slot tags, the item and model files, 4 icons, the atlas `textures/entity/equipment/3d/bloodthorn.png` and the repair tag.
  - Before the merge, the knight armor's entries, atlases and icons were byte-identical to their baseline.
- **`check_mod_data.py`:** PASS (1512 material IDs, after main brought thallite slice 1, the wood repaint and trees batch 1). This includes the new `check_armor_tiers` and main's art check (`tools/art_check.py`). Every Bloodthorn entry is 0.00% see-through (H1, limit 0.5%), with no flicker or texture findings.
- **`check_repository.py`:** PASS.
- **`armor_models.py`:** no problems. Five reach warnings: the centre spike, the low spikes and the pauldrons' lames (see "The look").
- **`armor_smoke.py --no-render`:** all checks pass.
- **Java:** the three changed or new Java files parse (syntax only; nothing compiles outside CI).
- **Wearer audit** (`armor_preview.py`'s wearer mannequin, totalled by a scratch script): 0.00 model px² of skin or outer layer shows.
  - It covers 7 poses × 9 views, plus a walk and sneak-walk cycle (32 poses × 9 views).
  - With the left boot left off, it reports 44.9, so it does see the wearer.
- **Icons,** checked by a scratch script:
  - each outline is its part's own darkest tone, never pure black;
  - no fill pixel touches transparency;
  - each icon is lit from the top left.
- **Renders** with `armor_preview.py` (not committed):
  - the owner's two views beside ours from fitted cameras, lit as their render and as the game lights entities;
  - stand, walk and sneak from six views.

  The silhouettes overlap the owner's at 0.898 (front three-quarter) and 0.892 (back), as intersection over union.

**In CI:** not run yet. It will:
- compile `armorTier` and the material;
- run `ArmorTiersGameTests` on the server;
- run `ArmorTiersClientGameTests`, which writes the `jugcraft_armor_tier_bloodthorn_*` shots.

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: it is placed in no structure, loot table or boss drop. When the owner chooses bosses, their records will carry its drop rates.

## Rollout and open questions
- **How it is obtained:** boss drops or a recipe, as the owner decides; until then it is creative-only.
- **Its numbers** against the other four sets, once they exist: whether they form a ladder or sit side by side with different strengths.
- **A perk:** the name suggests thorns (hurting attackers). None is added; the owner may want one.
- **Where the model differs from the design** (each can be changed if the owner wants):
  - **Pauldrons:** in the front view ours sit 1–2 pixels lower and about 1 further out at the flared lame. In the back view they match, so the two views disagree.
  - **Spikes:** the low pair's tips are about 1.5 pixels higher than drawn.
  - **Knee diamonds:** ours are squares on their corners, about 4 pixels across. The owner's read narrower and taller (about 2.4 × 4.3); the toolkit's diamond is always square.
  - **The V:** its dark notch sits behind the breastplate, so only the near-black stripe reads as the dark V.
  - **Helm:** a little narrower in the owner's render. Ours is 9.5 pixels wide, the least that clears the hat layer.
  - **Neck:** the owner's dark band is the collar's plum inside; ours is a plum band on the helm. With the helmet off, the collar shows a closed magenta top.
  - **Strips:** generated runs with the render's share of each tone, not texel-exact copies.
  - **Scale:** the owner's two views differ in scale by 4.6%, so either their zoom differs or our proportions are off by that much.
  - If the design was built in a modelling program, its file would give the exact box sizes.
- **The other four sets** (Reforged White Diamond, Hades with its scythe, Sunset Gem, Pharaoh) each add an `ARMOR_TIERS` entry, a model module and icon maps. The registration, checks and tests here cover them as they come.
