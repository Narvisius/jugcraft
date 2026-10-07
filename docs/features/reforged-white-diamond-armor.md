# Reforged White Diamond Armor: the second of the owner's armor tiers

Status: implemented on `claude/white-diamond-armor`, awaiting review. It is stacked on `claude/bloodthorn-armor` (#235), whose armor-only tier code it reuses, which is itself on the knight armor's 3D engine (#227). **Not yet compiled, game-tested or played:** the Java compiles only in CI.
Proposal issue: none. This is the second of the five armor designs the owner sent on 6 October 2026 ("Can we start implementing the next 5 that I created. Again want you to use the the complex armor for each one to capture the shape of each piece like I have... Lets do these 1 by 1"). Its picture is titled "Reforged White Diamond". The owner's answers for all five ([bloodthorn-armor.md](bloodthorn-armor.md)):
- what kind of armor: "New stronger armor tiers";
- how players get it: "They might get dropped by bosses or be craftable for now just make the armor we can figure that out later".

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: a new armor tier above netherite (endgame), beside Bloodthorn. Its place in the game, and how it is obtained, are the owner's to decide later; until then it is creative-only.
Primary specialty and supported player role: combat (defense). Everyone who wears armor.

## Player experience
**The look:** the owner's design, worn as a 3D model bigger than vanilla armor, in icy white and pale cyan shaded with light blue and lavender, built on the knight armor's toolkit ([knight-armor.md](knight-armor.md)):
- **Helmet:**
  - a helm with a big V crest of two crossing bars over the brow;
  - angular wing bars rising at the sides, sweeping back into a V behind the head over a smaller V at the nape;
  - a charcoal face plate with a T-shaped opening.
- **Chestplate:**
  - wide, wing-like pauldrons in two tiers (a wing over a lame), rising toward the outside;
  - a pale V chevron over the chest, outlined in lavender, and a V on the back to match;
  - striped plates down the body and a gem at the collar;
  - charcoal sleeves.
- **Leggings:** a long skirt of tassets: two angled front plates in an A over horizontal light and lavender stripes, wrapping the legs so the A repeats behind.
- **Boots:** a diamond plate on each toe, with a blue V notch at the top of each boot's back.
- **Colours:** the owner's render is unlit, so the palette (`armor_paint.WHITE_DIAMOND`) is its colours as they stand: whites, pale cyans, light blues and lavender, over a charcoal for the face and sleeves. In game it reads a little greyer, as every set does under the game's lighting.
- **Closed all round:** the render shows the front only; the sides and back are drawn to match it (above). A face is left out only where another box of the same piece covers it, so nothing shows through, even on an armor stand.
- **Size:** 38 parts and 203 quads for a full set (the cap is 900), on one 128 × 128 texture.
  - The crest and wing bars rise 6.3 pixels above the head; the toolkit's guideline is about 6.
  - The pauldrons reach 13.4 pixels to the side; the toolkit warns past 12, since such parts may pop out at the edge of the screen.
- **Icons:** four 16 × 16 maps of its own, in `tools/armor_icons/reforged_white_diamond/`, drawn after the owner's renders of each piece: the helm with its V crest and dark T, the winged chestplate with its V, the skirt, and the boots with their diamonds.

**The numbers** (`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.REFORGED_WHITE_DIAMOND_ARMOR`): beside Bloodthorn rather than above it, in other strengths:

| | Reforged White Diamond | Bloodthorn | Netherite |
|---|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 4, 8, 7, 3 (22) | 3, 9, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 3.0 | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.1 | 0.15 | 0.1 |
| Durability (multiplier) | 495, 720, 675, 585 (45) | 440, 640, 600, 520 (40) | 407, 592, 555, 481 (37) |
| Enchantability | 20 | 15 | 15 |
| Fire resistant | no | yes | yes |
| Repaired with | diamonds | netherite ingots | netherite ingots |
| Equip sound | diamond's | netherite's | netherite's |

Bloodthorn is the tougher of the two and resists fire; Reforged White Diamond has the heavier helm, lasts longest and enchants best. These are starting numbers for the owner to set.

**Getting it:** for now only from the creative tab (Combat). No recipe, drop or trade exists yet, as the owner asked.

## Connections
- Existing input producer: none yet (creative only); the repair item is the diamond.
- Existing output consumer: the player's armor slots; it enchants, trims and equips as any armor of its slot (it is in `#minecraft:head_armor` and the other slot tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("for now just make the armor"). It is not required by anything, so nothing is blocked on it.
- Which connections are required vs optional: all optional.

## Balance and automation
- **Above netherite,** so it must come late. Its recipe or drop is the owner's to set, and it should cost at least a netherite set's worth, even though it mends with diamonds.
- **No conversion loops:** nothing turns it back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain armor items:** nothing new is saved, and the server handles them as any armor.
- **The 3D model is client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` and the atlas `textures/entity/equipment/3d/reforged_white_diamond.png`.
- **Stable IDs:** `jugcraft:reforged_white_diamond_helmet`, `_chestplate`, `_leggings` and `_boots`, and the repair tag `jugcraft:repairs_reforged_white_diamond_gear`. The IDs keep the owner's full name, so a plain "White Diamond" could still come later without a clash.
- **No equipment asset file:** every piece is drawn in 3D, so no flat layer is drawn and no `equipment/reforged_white_diamond.json` is written.

## The shared parts it uses
Bloodthorn added the armor-only tiers ([bloodthorn-armor.md](bloodthorn-armor.md#armor-only-tiers-the-shared-part)). This set adds:
- one `ARMOR_TIERS` entry, one Java material and one `armorTier(...)` call;
- a row in `ArmorTiersGameTests`, which types its numbers in. `ArmorTiersClientGameTests` shows every tier, so it shoots this one with no change.

Beyond that, `tools/armor_preview.py`'s reference panels take an optional options dict (pose, unlit, background), because this render is unlit and in the owner's pose. Bloodthorn's comparison renders as before.

## Dependencies and assets
- No new dependencies.
- The design is the owner's (the reference picture is not committed). The model, its paint and the icons are drawn by code, in `tools/white_diamond_armor.py`, `tools/armor_paint.py` (`WHITE_DIAMOND`) and `tools/armor_icons/reforged_white_diamond/`. No Mojang or third-party texture is used.

## Verification
**Run locally on 7 October 2026,** on this branch:
- **Generators:** `generate_material_data.py`, `generate_textures.py`, then `generate_material_data.py` again, all exit 0; the second run changed nothing.
  - This set's own output: 9 `worn_models.json` entries, 4 names, the 4 vanilla slot tags, the item and model files, 4 icons, the atlas and the repair tag.
  - The knight and Bloodthorn outputs are byte-identical: 3 atlases, 224 item textures and every earlier `worn_models.json` entry.
- **`check_mod_data.py`:** PASS (1516 material IDs), with `check_armor_tiers` and main's art check (`tools/art_check.py`). Every Reforged White Diamond entry is 0.00% see-through (H1, limit 0.5%), with no flicker or texture findings.
- **`check_repository.py`:** PASS.
- **`armor_models.py`:** no problems. Its reach warnings are the crest, wings and pauldrons (see "The look").
- **`armor_smoke.py --no-render`:** all checks pass.
- **Java:** the changed files parse (syntax only; nothing compiles outside CI).
- **Wearer audit** (`armor_preview.py`'s wearer mannequin, totalled by a scratch script): 0.00 model px² of skin or outer layer shows, over 7 poses × 9 views and a walk and sneak-walk cycle (32 poses × 9 views).
- **Icons,** checked by a scratch script:
  - closed one-pixel outlines in the set's darkest tone, never pure black;
  - no stray pixels;
  - lit from the top left.
- **Renders** with `armor_preview.py` (not committed):
  - the owner's front view beside ours from a fitted camera, unlit as their render and lit as in game. The silhouettes overlap at 0.948 (intersection over union).
  - each piece alone beside the owner's render of it;
  - stand, walk and sneak from six views.

**In CI:** not run yet. It will compile the material and its registration, run `ArmorTiersGameTests` on the server, and run `ArmorTiersClientGameTests`, which writes the `jugcraft_armor_tier_reforged_white_diamond_*` shots.

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: it is placed in no structure, loot table or boss drop. When the owner chooses bosses, their records will carry its drop rates.

## Rollout and open questions
- **How it is obtained:** boss drops or a recipe, as the owner decides; until then it is creative-only.
- **Its numbers** against the other sets: side by side in different strengths, as now, or a ladder.
- **The charcoal face and sleeves:** in the owner's full render the face and arms are charcoal, but the helmet and chestplate rendered alone show an open face and no sleeves, so the charcoal may be the figure under the armor. The model keeps a charcoal face plate and charcoal sleeves; the owner may prefer bare arms or an open face.
- **Where the model differs from the design** (each can be changed if the owner wants):
  - **Tassets:** rolled rectangles, so their bottom edges tilt; the owner's flare into trapezoids with level feet.
  - **Lower body:** the skirt and boots end about a pixel higher and narrower than drawn, because the boots stop at the player's feet.
  - **Under-skirt stripes:** straight, where the owner's are gentle arcs.
  - **The V's:** exactly 45°, built from bars meeting square so the point stays clean, and the chest V is centred; the owner's sit at about 44–49° and the chest V about 1.4 pixels off-centre.
  - **The chestplate icon** has no sleeves, as in the owner's piece render.
  - If the design was built in a modelling program, its file would give the exact box sizes.
