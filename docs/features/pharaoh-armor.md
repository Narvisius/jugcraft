# Pharaoh Armor: the fifth of the owner's armor tiers

Status: implemented on `claude/pharaoh-armor`, awaiting review. It is stacked on `claude/sunset-gem-armor` (#241, [sunset-gem-armor.md](sunset-gem-armor.md)), itself on `claude/hades-armor` (#240), since the sets share the lists they are added to. It uses the armor-only tier code that came with Bloodthorn ([bloodthorn-armor.md](bloodthorn-armor.md)) on the knight armor's 3D engine ([knight-armor.md](knight-armor.md)). **Not yet compiled, game-tested or played:** the Java compiles only in CI.
Proposal issue: none. This is the last of the five armor designs the owner sent on 6 October 2026 ("Can we start implementing the next 5 that I created. Again want you to use the the complex armor for each one to capture the shape of each piece like I have... Lets do these 1 by 1"). Its picture is titled "PHARAOH". The owner's answers for all five ([bloodthorn-armor.md](bloodthorn-armor.md)):
- what kind of armor: "New stronger armor tiers";
- how players get it: "They might get dropped by bosses or be craftable for now just make the armor we can figure that out later".

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: a new armor tier above netherite (endgame), beside the owner's other sets. Its place in the game, and how it is obtained, are the owner's to decide later; until then it is creative-only.
Primary specialty and supported player role: combat (defense). Everyone who wears armor.

## Player experience
**The look:** the owner's design, worn as a 3D model bigger than vanilla armor, in gold and teal with red gems over tan linen, built on the knight armor's toolkit ([knight-armor.md](knight-armor.md)):
- **Helmet:** a nemes headdress: a cap striped upright in gold and teal under a gold brow band, with the uraeus (a red gem on a gold diamond) on the brow; on each side a tall flap in level gold and teal stripes from above the crown down to the shoulder, its top a rounded teal hump that rises above the crown's outer corner, bulging at the temples and coming in toward the jaw; lappets beside the face, gold over dark teal ends beside the chin; a tan face plate narrowing in steps to the chin, and a short beard.
- **Chestplate:**
  - a broad collar of gold and teal rings round the neck, with a red gem at its front;
  - a breastplate framing a teal square in gold, and a framed back plate to match;
  - a banded corselet over tan linen sleeves;
  - gold-framed teal bracers, with two near-black rings round each wrist inside them.
- **Leggings:** a skirt of striped lames in gold, tan and teal, each rolled a little so the bands dip in a V at the front and back; it splits per leg when walking, as the knight's skirt does.
- **Boots:** sandal-greaves: tan wraps, a gold-rimmed teal cuff, a framed teal shin plate, a gold ankle strap and a gold sole with a toe lip (the owner's picture hides the boots).
- **Colours:** the owner's render is unlit (its ramps change hue as they darken, which lighting cannot do), so the palette (`armor_paint.PHARAOH`) is its colours as they stand. In the palette, the metal is the gold, "gold" names the two gem reds, "leather" the teal (its darkest the near-black of the bands) and the under-layer the tan. "seam" and "void" are two darker gold-browns that only the icons' outlines use.
- **The tan face and arms are armor:** a tan face plate (a mask with no eyes, as drawn) and tan linen sleeves, so the helm is closed and the set looks as drawn on any skin and on an armor stand, as White Diamond's charcoal does.
- **Closed all round:** the render shows the front only; the sides and back are drawn to match it: the headdress's stripes run all round, a teal knot framed in gold sits at the back of the head over a short queue in gold wraps, the collar's ring continues behind, and the corselet's and skirt's bands and the skirt's V repeat at the back. A face is left out only where another box of the same piece covers it, so nothing shows through, even on an armor stand.
- **Size:** 72 parts and 392 quads for a full set (the cap is 900), on one 128 × 128 texture. The humps rise 3.6 pixels above the head and the flaps reach 8.6 pixels to the side; nothing passes the toolkit's warning limits.
- **Icons:** four 16 × 16 maps of its own, in `tools/armor_icons/pharaoh/`.

**The numbers** (`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.PHARAOH_ARMOR`): beside the others, in other strengths:

| | Pharaoh | Sunset Gem | Hades | Reforged White Diamond | Bloodthorn | Netherite |
|---|---|---|---|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 4, 9, 6, 3 (22) | 3, 8, 7, 3 (21) | 3, 8, 7, 3 (21) | 4, 8, 7, 3 (22) | 3, 9, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 3.0 | 3.0 | 4.0 | 3.0 | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.1 | 0.1 | 0.2 | 0.1 | 0.15 | 0.1 |
| Durability (multiplier) | 451, 656, 615, 533 (41) | 528, 768, 720, 624 (48) | 462, 672, 630, 546 (42) | 495, 720, 675, 585 (45) | 440, 640, 600, 520 (40) | 407, 592, 555, 481 (37) |
| Enchantability | 22 | 25 | 12 | 20 | 15 | 15 |
| Fire resistant | yes | no | yes | no | yes | yes |
| Repaired with | gold ingots | amethyst shards | netherite ingots | diamonds | netherite ingots | netherite ingots |
| Equip sound | gold's | gold's | netherite's | diamond's | netherite's | netherite's |

Pharaoh has the heaviest helm (with White Diamond) and chestplate (with Bloodthorn), leggings only as heavy as netherite's, the second-best enchanting and resists fire. These are starting numbers for the owner to set.

**Getting it:** for now only from the creative tab (Combat). No recipe, drop or trade exists yet, as the owner asked.

## Connections
- Existing input producer: none yet (creative only); the repair item is the gold ingot.
- Existing output consumer: the player's armor slots; it enchants, trims and equips as any armor of its slot (it is in `#minecraft:head_armor` and the other slot tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("for now just make the armor"). It is not required by anything, so nothing is blocked on it.
- Which connections are required vs optional: all optional.

## Balance and automation
- **Above netherite,** so it must come late. Its recipe or drop is the owner's to set, and it should cost at least a netherite set's worth, even though it mends with gold.
- **No conversion loops:** nothing turns it back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain armor items:** nothing new is saved, and the server handles them as any armor.
- **The 3D model is client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` and the atlas `textures/entity/equipment/3d/pharaoh.png`.
- **Stable IDs:** `jugcraft:pharaoh_helmet`, `_chestplate`, `_leggings` and `_boots`, and the repair tag `jugcraft:repairs_pharaoh_gear`.
- **No equipment asset file:** every piece is drawn in 3D, so no `equipment/pharaoh.json` is written.

## The shared parts it uses
One `ARMOR_TIERS` entry, one Java material and one `armorTier(...)` call, and a row in `ArmorTiersGameTests`; `ArmorTiersClientGameTests` shows every tier, so it shoots this one with no change. `tools/armor_preview.py` gains a `pharaoh` pose (arms 12° out, head turned 13°) and a reference layout for the owner's render.

## Dependencies and assets
- No new dependencies.
- The design is the owner's (the reference picture is not committed). The model, its paint and the icons are drawn by code, in `tools/pharaoh_armor.py`, `tools/armor_paint.py` (`PHARAOH`) and `tools/armor_icons/pharaoh/`. No Mojang or third-party texture is used.

## Verification
**Run locally on 7 October 2026:**
- **The set,** built in its own branch:
  - **Generators:** data, textures, data again, all exit 0; the second run changed nothing. The earlier sets' outputs are byte-identical.
  - **`check_mod_data.py`:** PASS, with `check_armor_tiers` and the art check (`tools/art_check.py`): every Pharaoh entry is 0.00% see-through (H1, limit 0.5%), with no flicker findings (Z2, N2).
  - **`armor_models.py`:** no problems and no warnings. **`armor_smoke.py --no-render`:** all checks pass. **`check_repository.py`**, **`check_icon_maps.py`:** PASS.
  - **Cross-piece flicker** (a scratch check for faces of different pieces in one plane): none over 125 poses: standing, sneaking, 8 points of the walk, the sneak-walk, the owner's pose, and 112 head poses (turned up to 80 degrees, looking straight up and down). It found three pairs in the first draft and two in the headdress rework, all fixed.
  - **Wearer audit** (`armor_preview.py`'s wearer mannequin, totalled by a scratch script): 0.00 model px² of skin or outer layer shows, over 7 poses × 9 views, the walk and sneak-walk cycle (32 poses × 9 views) and 25 head poses. With the boots left off it reports 54.7, so it does see the wearer.
  - **Paint audit** (scratch): no face falls back to the plain mid-gold paint. The first draft had 12 such faces, on the boot cuffs, the ankle straps and the skirt's lining, and showed gold where the docstring says teal; fixed by naming those faces.
  - **Icons,** checked by a scratch script: each part outlined in its own darkest tone, never pure black; no open fill; lit from the top left.
  - **Renders** with `armor_preview.py` (not committed): the owner's front view beside ours from a fitted camera, unlit as their render and lit as in game. The silhouettes overlap at 0.930 (intersection over union), and the head alone at 0.933. Also the head close up, front and three-quarter; the boots and hem standing and mid-stride; stand, walk and sneak from six views.
- **After stacking on Sunset Gem** (#241): Pharaoh's changes were applied on Sunset Gem's head.
  - **Conflicts:** only in the lists the sets share (tiers, materials, registrations, test rows, palettes, icon maps, model modules, preview poses and references), each kept with every set's entry.
  - **Generators:** data, textures, data again, all exit 0, with nothing left to change but the 24 textures main already redraws (see #240), left as committed. Pharaoh's 14 generated files are identical to the reviewed work, and the other sets' 54 worn-model entries to Sunset Gem's.
  - **`check_mod_data.py`:** PASS (1558 material IDs). **`check_repository.py`**, **`check_icon_maps.py`:** PASS. **`armor_models.py`:** no problems. **`armor_smoke.py --no-render`:** all pass.
  - **Java:** the 2 changed files parse (syntax only; nothing compiles outside CI).

**In CI:** not run yet. It will compile the material and its registration, run `ArmorTiersGameTests` on the server, and run `ArmorTiersClientGameTests`, which writes the `jugcraft_armor_tier_pharaoh_*` shots.

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: it is placed in no structure, loot table or boss drop. When the owner chooses bosses, their records will carry its drop rates.

## Rollout and open questions
- **How it is obtained:** boss drops or a recipe, as the owner decides; until then creative-only.
- **Its numbers** against the other sets: side by side in different strengths, as now, or a ladder.
- **The tan face and arms:** kept as armor (a face plate and linen sleeves); the owner may prefer the wearer's own face and arms to show.
- **Where the model differs from the design** (each can be changed if the owner wants):
  - **The humps** are stepped boxes, so in lit three-quarter views they read as stacked ledges rather than the owner's smooth domes.
  - **The crown** is wider than drawn: it has to clear the hat layer, so it narrows by the humps overhanging its corners and by paint.
  - **The flaps** sit 2.75 pixels behind the crown's face, as the owner's turned head shows them; a symmetric model cannot match both sides of that turned head (the owner's left flap bulges a little further at the brow).
  - **Centring:** the owner's chest gem, breastplate and uraeus sit about a texel right of centre; here they are centred.
  - **The forearms:** the black bands are read as two rings round the wrist inside the bracer; the owner's arms may be turned a little more than 12 degrees.
  - **The skirt:** the gold bands are about 0.8 to 0.9 pixel thick against the owner's 1.2 or so, and the V comes from a 4.5-degree roll of each lame; it splits per leg when walking, as the knight's does.
  - **Colours:** the owner's middle red (the chest gem's) is left out, and their teal is a little lighter overall.
  - **A moving head:** looking hard down swings the lappet ends toward the collar, and looking steeply up swings the tail toward the back (both checked clear of the other pieces).
  - **Invented** for what the render does not show: the boots (sandal-greaves), the back of the head (a teal knot framed in gold over a short queue), the back plate, and the backs of the collar, corselet and skirt.
  - If the design was built in a modelling program, its file would give the exact box sizes.
