# Sunset Gem Armor: the fourth of the owner's armor tiers

Status: implemented on `claude/sunset-gem-armor`, awaiting review. It is stacked on `claude/hades-armor` (#240, [hades-armor.md](hades-armor.md)), since the sets share the lists they are added to; that branch is on `integration/oct7-pr-backlog`, with the knight armor, Bloodthorn and White Diamond. It uses the armor-only tier code that came with Bloodthorn ([bloodthorn-armor.md](bloodthorn-armor.md)) on the knight armor's 3D engine ([knight-armor.md](knight-armor.md)). **Not yet compiled, game-tested or played:** the Java compiles only in CI.
Proposal issue: none. This is the fourth of the five armor designs the owner sent on 6 October 2026 ("Can we start implementing the next 5 that I created. Again want you to use the the complex armor for each one to capture the shape of each piece like I have... Lets do these 1 by 1"). Its picture is titled "SUNSET GEM". The owner's answers for all five ([bloodthorn-armor.md](bloodthorn-armor.md)):
- what kind of armor: "New stronger armor tiers";
- how players get it: "They might get dropped by bosses or be craftable for now just make the armor we can figure that out later".

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: a new armor tier above netherite (endgame), beside the owner's other sets. Its place in the game, and how it is obtained, are the owner's to decide later; until then it is creative-only.
Primary specialty and supported player role: combat (defense). Everyone who wears armor.

## Player experience
**The look:** the owner's design, worn as a 3D model bigger than vanilla armor, in a sunset gradient (cream and yellow at the top, through apricot and peach, to coral and red at the feet, with crimson and mauve bands) over an olive under-layer, built on the knight armor's toolkit ([knight-armor.md](knight-armor.md)):
- **Helmet:** a crown of gem shards round the top of the helm, a tall one at the front and more round the back, each tip trailing small detached sparkle cubes; an olive face plate.
- **Chestplate:**
  - wings of gem shards rising from the shoulders, three blades bending into a crescent and a steeper shard, ending in sparkles;
  - a striped breastplate with a cream collar band, and a small cream gem between the shoulder blades where the wings meet;
  - pauldrons of three stacked plates;
  - olive sleeves and bracers.
- **Leggings:** a striped skirt with a red V on the belt, red flaps wrapping each leg, mauve bands at the hem, and shards with sparkles flaring at the hips.
- **Boots:** a crimson cuff with a mauve middle over peach and coral strips, with a shard and a sparkle on the outer side (the owner's picture cuts the boots off; these follow its visible top).
- **Colours:** the owner's render is unlit, so the palette (`armor_paint.SUNSET_GEM`) is its colours as they stand. In the palette, "gold" names the crimson and mauve and the under-layer is the olive.
- **Closed all round:** the render shows the front only; the crown, the breastplate's back, the belt, the flaps and the boots are drawn round the back to match it. Every sparkle is its own closed cube, clear of every other box. A face is left out only where another box of the same piece covers it, so nothing shows through, even on an armor stand.
- **Size:** 65 parts and 366 quads for a full set (the cap is 900), on one 128 × 128 texture.
  - The crown's tallest shard rises 5.3 pixels above the head.
  - The wing sparkles reach 13.6 pixels to the side, the hip sparkles 13.1 and the outer wing blade 12.1; the toolkit warns past 12, since such parts may pop out at the edge of the screen.
- **Icons:** four 16 × 16 maps of its own, in `tools/armor_icons/sunset_gem/`. The chestplate icon leaves out the olive forearms, as the owner's render of the piece does.

**The numbers** (`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.SUNSET_GEM_ARMOR`): beside the others, in other strengths:

| | Sunset Gem | Hades | Reforged White Diamond | Bloodthorn | Netherite |
|---|---|---|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 3, 8, 7, 3 (21) | 3, 8, 7, 3 (21) | 4, 8, 7, 3 (22) | 3, 9, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 3.0 | 4.0 | 3.0 | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.1 | 0.2 | 0.1 | 0.15 | 0.1 |
| Durability (multiplier) | 528, 768, 720, 624 (48) | 462, 672, 630, 546 (42) | 495, 720, 675, 585 (45) | 440, 640, 600, 520 (40) | 407, 592, 555, 481 (37) |
| Enchantability | 25 | 12 | 20 | 15 | 15 |
| Fire resistant | no | yes | no | yes | yes |
| Repaired with | amethyst shards | netherite ingots | diamonds | netherite ingots | netherite ingots |
| Equip sound | gold's | netherite's | diamond's | netherite's | netherite's |

Sunset Gem lasts longest and enchants best of all, at Hades' defense, a point less than Bloodthorn's, and netherite's toughness. These are starting numbers for the owner to set.

**Getting it:** for now only from the creative tab (Combat). No recipe, drop or trade exists yet, as the owner asked.

## Connections
- Existing input producer: none yet (creative only); the repair item is the amethyst shard.
- Existing output consumer: the player's armor slots; it enchants, trims and equips as any armor of its slot (it is in `#minecraft:head_armor` and the other slot tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("for now just make the armor"). It is not required by anything, so nothing is blocked on it.
- Which connections are required vs optional: all optional.

## Balance and automation
- **Above netherite,** so it must come late. Its recipe or drop is the owner's to set, and it should cost at least a netherite set's worth, even though it mends with amethyst shards.
- **No conversion loops:** nothing turns it back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain armor items:** nothing new is saved, and the server handles them as any armor.
- **The 3D model is client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` and the atlas `textures/entity/equipment/3d/sunset_gem.png`.
- **Stable IDs:** `jugcraft:sunset_gem_helmet`, `_chestplate`, `_leggings` and `_boots`, and the repair tag `jugcraft:repairs_sunset_gem_gear`.
- **No equipment asset file:** every piece is drawn in 3D, so no `equipment/sunset_gem.json` is written.

## The shared parts it uses
One `ARMOR_TIERS` entry, one Java material and one `armorTier(...)` call, and a row in `ArmorTiersGameTests`; `ArmorTiersClientGameTests` shows every tier, so it shoots this one with no change. `tools/armor_preview.py` gains a `sunset` pose (arms 12° out, head straight) and an `across` option for a reference camera aimed beside the figure, since the owner's render is cropped off-centre.

## Dependencies and assets
- No new dependencies.
- The design is the owner's (the reference picture is not committed). The model, its paint and the icons are drawn by code, in `tools/sunset_gem_armor.py`, `tools/armor_paint.py` (`SUNSET_GEM`) and `tools/armor_icons/sunset_gem/`. No Mojang or third-party texture is used.

## Verification
**Run locally on 7 October 2026,** on this branch before Hades was merged in:
- **Generators:** data, textures, data again, all exit 0; the second run changed nothing. The earlier sets' `worn_models.json` entries, atlases and icons are byte-identical.
- **`check_mod_data.py`:** PASS, with `check_armor_tiers` and main's art check (`tools/art_check.py`). Every Sunset Gem entry is 0.00% see-through (H1, limit 0.5%), with no flicker findings.
- **`check_repository.py`:** PASS. **`check_icon_maps.py`:** PASS.
- **`armor_models.py`:** no problems. Its reach warnings are the wing and hip sparkles and the outer wing blade (see "The look").
- **`armor_smoke.py --no-render`:** all checks pass.
- **Wearer audit** (`armor_preview.py`'s wearer mannequin, totalled by a scratch script): 0.00 model px² of skin or outer layer shows, over 8 poses × 9 views (the owner's pose among them) and a 32-pose walk and sneak-walk cycle. With a sleeve, a bracer and the under-skirt removed it reports 23–39, so it does see the wearer.
- **Icons,** checked by a scratch script: outlines in the set's darkest tone (luma 40), never pure black; lit from the top left.
- **Renders** with `armor_preview.py` (not committed): the owner's front view beside ours from a fitted camera, unlit as their render and lit as in game; each piece beside the owner's render of it; stand, walk and sneak from six views.

**After merging `claude/hades-armor`** (Hades Armor, the Hades Scythe, and Bloodthorn and White Diamond as #235 merged them):
- **Generators:** data, textures, data again, all exit 0; the last run changed nothing.
- **Both sets kept their output:** the 9 `worn_models.json` entries of each are identical to its own branch's, as are the knight, Bloodthorn and White Diamond entries, and every Sunset Gem file. Only the files that list every set changed (the names, the 3D model list and the four slot tags).
- **`check_mod_data.py`:** PASS (1525 material IDs), with the art check: no armor finding, and none is allow-listed.
- **`check_repository.py`:** PASS. **`check_icon_maps.py`:** PASS. **`armor_models.py`:** no problems (the reach warnings above). **`armor_smoke.py --no-render`:** all pass.
- **Java:** the 2 merged files parse (syntax only).

**After merging Hades again,** rebuilt on `integration/oct7-pr-backlog` (#240):
- **The merge** needed no hand resolution, and the branch differs from Hades by exactly Sunset Gem's own changes, file for file and line for line.
- **Generators:** data, textures, data again, all exit 0, with nothing left to change but the 24 textures the integration branch already redraws (Hades' record), left as committed. Sunset Gem's 9 entries are identical to its own branch's, and the other sets' 45 to Hades'.
- **`check_mod_data.py`:** PASS (1554 material IDs). **`check_repository.py`**, **`check_icon_maps.py`:** PASS. **`armor_models.py`:** no problems. **`armor_smoke.py --no-render`:** all pass. **Java:** the 2 changed files parse.

**In CI:** not run yet. It will compile the material and its registration, run `ArmorTiersGameTests` on the server, and run `ArmorTiersClientGameTests`, which writes the `jugcraft_armor_tier_sunset_gem_*` shots.

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: it is placed in no structure, loot table or boss drop. When the owner chooses bosses, their records will carry its drop rates.

## Rollout and open questions
- **How it is obtained:** boss drops or a recipe, as the owner decides; until then creative-only.
- **Its numbers** against the other sets: side by side in different strengths, as now, or a ladder.
- **The olive face and arms:** the owner's piece renders show no olive, so it is most likely the figure under the armor. The model keeps an olive face plate (the helm has to be closed) and olive sleeves and bracers, so the set looks as drawn on any skin; the owner may prefer bare arms.
- **Where the model differs from the design** (each can be changed if the owner wants):
  - **Wings:** three blades bending into a crescent, a steeper shard and three sparkles a side; the owner's fan of shards round the head is denser, and its crescent smoother.
  - **Pauldrons:** three stacked plates, a little redder and boxier than the owner's rounder arm guards.
  - **Belt:** the red V is painted on the belt; the owner's is two thin bars with the breastplate showing inside it.
  - **Flaps:** each is one rolled plate with parallel edges; the owner's fan out more and are narrower at the waist. The hem and boots are boxes, where the owner's lower skirt flares a little.
  - **Clearances:** the breastplate is about 0.4 pixel wider each side than drawn, to clear vanilla armor's shell; the closed helm's cheeks end a little lower than the owner's jaw.
  - **The crown** spreads less sideways at the band than the owner's helmet render.
  - **Motion:** the wings hang on the body, so they do not swing when walking; the crown can pass through the wing roots when the head turns far.
  - If the design was built in a modelling program, its file would give the exact box sizes.
