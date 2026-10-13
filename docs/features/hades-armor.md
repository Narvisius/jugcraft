# Hades Armor and the Hades Scythe: the third of the owner's armor tiers

Status: implemented on `claude/hades-armor`, awaiting review. It is built on `integration/oct7-pr-backlog`, which carries the knight armor's 3D engine (#227) and Bloodthorn and Reforged White Diamond (#235), whose armor-only tier code it reuses. **Not yet compiled, game-tested or played:** the Java compiles only in CI.
Proposal issue: none. This is the third of the five armor designs the owner sent on 6 October 2026 ("Can we start implementing the next 5 that I created. Again want you to use the the complex armor for each one to capture the shape of each piece like I have... Lets do these 1 by 1"), with its weapon: "I also want the scythe from my Hades Armor set." Its picture is titled "HADES ARMOR". The owner's answers for all five ([bloodthorn-armor.md](bloodthorn-armor.md)):
- what kind of armor: "New stronger armor tiers";
- how players get it: "They might get dropped by bosses or be craftable for now just make the armor we can figure that out later".

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: a new armor tier above netherite (endgame), beside Bloodthorn and Reforged White Diamond, with an arm of its own. Its place in the game, and how it is obtained, are the owner's to decide later; until then the armor and the scythe are creative-only.
Primary specialty and supported player role: combat. Everyone who wears armor; scythe wielders.

## Player experience
**The armor:** the owner's design, worn as a 3D model bigger than vanilla armor, in dark slates and steel greys with pinkish-grey edges, blue-black under-layer and a blood-red tabard, built on the knight armor's toolkit ([knight-armor.md](knight-armor.md)):
- **Helmet:**
  - a narrow helm whose front is a beak: two plates at 45° meeting in a ridge, each with a raised V brow over three slits;
  - cheek plates and a chin bar below the beak;
  - two big horns rising in two segments, with a light band at the joint.
- **Chestplate:**
  - layered pauldrons that rise toward the outside, a mantle over a fin;
  - a V of bars over a slate breastplate in a pinkish-grey frame;
  - dark sleeves showing between pauldron and breastplate;
  - the top of the red tabard, hanging below the breastplate from under the V.
- **Leggings:** a skirt of angled plates flaring out, wrapping the sides, over a striped back apron. The tabard continues as a narrow strip on each leg down nearly to the hem, with ragged ends, so it moves with the legs.
- **Boots:** tall boots with a lit cuff, slate strips, a toe cap with a V and a heel plate (the owner's picture cuts the boots off).
- **Colours:** the owner's render is unlit, so the palette (`armor_paint.HADES`) is its colours as they stand. In the palette, "gold" names the blue slate and "leather" the blood-red cloth.
- **Closed all round:** the render shows the front only; the back of the helm (nested V's over a nape guard), the back of the breastplate (a second V) and the back of the skirt are drawn to match it. A face is left out only where another box of the same piece covers it, so nothing shows through, even on an armor stand.
- **Size:** 57 parts and 341 quads for a full set (the cap is 900), on one 128 × 128 texture.
  - The horns rise 10.4 pixels above the head.
  - The fins reach 12.7 pixels to the side and the guards 12.3; the toolkit warns past 12, since such parts may pop out at the edge of the screen.
- **Icons:** four 16 × 16 maps of its own, in `tools/armor_icons/hades/`.

**The Hades Scythe** (`jugcraft:hades_scythe`): the set's weapon, an Arms VII variant of the scythe ([arms-vii.md](arms-vii.md#the-hades-scythe)):
- It fights as every scythe does: its swing, reach, trait (Reap), two-handed blow, weapon art and motion are the scythe's.
- **Wither:** a hit withers the foe (Wither, 3 s), the boon the Gravewarden carries.
- Like a boss's trophy it is epic, twice as hard-wearing as steel and has no recipe. Unlike one, no loot table drops it yet: it is creative-only until the owner decides how the armor sets are won.
- Its tooltip names its set: "Of the Hades Armor set".
- **The look:** the owner's scythe: a near-black snath with blood-red grip wraps, a light diamond pommel and a long curved slate blade with a back spike, in the arms' box style; a 16 × 16 icon map (`tools/arms_icons/hades_scythe.txt`).
- **A new kind of line:** the owner's armor sets are a third kind of variant line beside the crafted styles and the boss trophies (`SETS` in `tools/arms_variants.py`, `ArmVariants.SETS`), so the other sets can gain arms the same way.

**The numbers** (`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.HADES_ARMOR`): beside the other two, in other strengths:

| | Hades | Reforged White Diamond | Bloodthorn | Netherite |
|---|---|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 3, 8, 7, 3 (21) | 4, 8, 7, 3 (22) | 3, 9, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 4.0 | 3.0 | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.2 | 0.1 | 0.15 | 0.1 |
| Durability (multiplier) | 462, 672, 630, 546 (42) | 495, 720, 675, 585 (45) | 440, 640, 600, 520 (40) | 407, 592, 555, 481 (37) |
| Enchantability | 12 | 20 | 15 | 15 |
| Fire resistant | yes | no | yes | yes |
| Repaired with | netherite ingots | diamonds | netherite ingots | netherite ingots |

Hades is the toughest and the hardest to knock back, a point lower in defense and the poorest to enchant. These are starting numbers for the owner to set.

**Getting them:** for now only from the creative tab (Combat). No recipe, drop or trade exists yet, as the owner asked.

## Connections
- Existing input producer: none yet (creative only); the armor's repair item is netherite, the scythe's steel's.
- Existing output consumer: the player's armor slots and hands; the armor enchants, trims and equips as any armor of its slot, and the scythe enchants as any scythe (it joins its kind's item tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("for now just make the armor"). Nothing requires them, so nothing is blocked on them.
- Which connections are required vs optional: all optional.

## Balance and automation
- **Above netherite,** so the armor must come late. Its recipe or drop is the owner's to set, and it should cost at least a netherite set's worth.
- **The scythe** is bounded as every Arms VII variant is: its boon is at most 5 s at amplifier 1, refreshed and never stacked, and no variant deals as much a second as a netherite sword.
- **No conversion loops:** nothing turns them back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain items:** the armor pieces are plain armor items and the scythe an `ArmItem`, as every variant is. Nothing new is saved; the server handles them as any armor and arm, and the Wither boon is worked on the server when the scythe strikes.
- **The 3D armor is client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` and the atlas `textures/entity/equipment/3d/hades.png`.
- **Stable IDs:** `jugcraft:hades_helmet`, `_chestplate`, `_leggings`, `_boots`, `jugcraft:hades_scythe`, and the repair tag `jugcraft:repairs_hades_gear`.
- **No equipment asset file:** every armor piece is drawn in 3D, so no `equipment/hades.json` is written.

## Dependencies and assets
- No new dependencies.
- The design is the owner's (the reference picture is not committed). The armor model, its paint and the icons are drawn by code, in `tools/hades_armor.py`, `tools/armor_paint.py` (`HADES`) and `tools/armor_icons/hades/`; the scythe in `tools/arms_variants_art.py` and `tools/arms_icons/hades_scythe.txt`. No Mojang or third-party texture is used.

## Verification
**Run locally on 7 October 2026:**
- **The armor**, built in its own branch (before the scythe and the base were merged in):
  - **Generators:** data, textures, data again; the second run changed nothing, and every other resource file kept its hash (all knight, Bloodthorn and White Diamond outputs).
  - **`check_mod_data.py`:** PASS. **`armor_models.py`:** no problems (the reach warnings above). **`armor_smoke.py --no-render`:** all pass. **`check_repository.py`:** PASS.
  - **See-through (H1):** 0.00% on all 9 entries, with no coplanar faces inside any entry.
  - **Wearer audit:** 0.00 model px² of skin or outer layer shows, over 8 poses × 9 views and a 32-pose walk, sprint and sneak-walk cycle.
  - **A scratch check for faces of different pieces in one plane:** none, standing, in the owner's pose, sneaking, and at 8 phases each of walking and sneak-walking. It found two flickers in the first draft (the collar against the chest V's bars, the sleeves' feet against the belt's), both fixed.
  - **Icons:** all four pass the icon rules.
  - **Renders** beside the owner's picture (not committed): the silhouettes overlap at 0.834 (whole figure), 0.793 (helmet), 0.762 (chestplate) and 0.858 (leggings) intersection over union.
- **The scythe**, built in its own branch:
  - **Generators:** clean; the other arms' output is byte-identical (16,314 of 16,317 files; the rest only gained entries).
  - **`check_mod_data.py`:** PASS, with `check_icon_maps.py` and the new set-line checks, each shown to fail when broken (a chest table dropping the scythe, a boss table for the set, a recipe, a line in two groups, Java out of step, a tooltip naming a boss).
  - **Java:** the 6 changed files parse (syntax only).
- **After merging the two and the base** (`claude/bloodthorn-armor`, with White Diamond and the newest main):
  - **Generators:** data, textures, data again, all exit 0; nothing is left to change.
  - **`check_mod_data.py`:** PASS (1521 material IDs), with the art check: every Hades entry is 0.00% see-through.
  - **`check_repository.py`:** PASS. **`armor_smoke.py --no-render`:** all pass. **`armor_models.py`:** no problems.
  - **Java:** the 8 changed files parse (syntax only).

- **After merging `integration/oct7-pr-backlog`** (the knight armor, Bloodthorn and White Diamond as merged there, thallite gear and the rest of the 7 October backlog):
  - **Each conflict** took the integration branch's side, with this branch's own changes applied on top; the result differs from the integration branch by exactly those changes, file for file and line for line.
  - **Generators:** data, textures, data again, all exit 0. Hades' output is as before: its 9 `worn_models.json` entries are identical to this branch's before the merge, and the knight, Bloodthorn and White Diamond entries to the integration branch's.
  - **Left out:** the texture generator also redraws 24 textures that came with the integration branch (the conservatory's flowers and Styxhexenhammer); the integration branch alone does the same, so they are not this change's and are left as committed.
  - **`check_mod_data.py`:** PASS (1550 material IDs), with the art check. **`check_repository.py`**, **`check_icon_maps.py`:** PASS. **`armor_models.py`:** no problems. **`armor_smoke.py --no-render`:** all pass.
  - **Java:** the 8 changed files parse (syntax only).

**In CI:** not run yet. It will compile the material, the variant and the set line; run `ArmorTiersGameTests` and `ArmsVIIGameTests` (now with the set line, and the scythe's Wither landing on a pig) and `TraitDetailsGameTests`; and shoot `ArmorTiersClientGameTests` (`jugcraft_armor_tier_hades_*`) and `ArmsVIIClientGameTests` (every variant racked, the scythe held).

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: they are placed in no structure, loot table or boss drop. When the owner chooses bosses, their records will carry the drop rates.

## Rollout and open questions
- **How they are obtained:** boss drops or recipes, as the owner decides; until then creative-only.
- **The numbers** against the other sets: side by side in different strengths, as now, or a ladder. Whether the scythe should be stronger than a steel variant, as the armor is above netherite.
- **Where the armor differs from the design** (each can be changed if the owner wants):
  - **Pauldrons:** they rise as in the owner's render of the chestplate alone; in the owner's full figure, with the arms raised, the fins stand level. The fins reach 12.7 pixels, where the owner's chestplate render reaches about 15.8.
  - **Tabard:** it starts under the V's point and the leg strips are 3 pixels wide; in the owner's renders the red starts at the waist, is wider under the V and narrows down. The owner's chestplate render hangs the cloth about 8 pixels below its hem, which on the body would cut through the walking legs, so the leggings carry it on.
  - **Helm:** the owner's helm outline is somewhat wider and deeper; the cheek plates are a reading of the two prongs under the owner's helm.
  - **The chest V's bars** overlap at the point under a small cap, since a V this steep cannot be joined cleanly with boxes.
  - **Colour:** a little paler and less slate than the owner's.
  - **Invented** for the parts the picture does not show: the boots, the helm's back, the back of the breastplate and the skirt's back and side wraps.
- **Where the scythe differs:** the snath and pommel are about 0.8 of the design's thickness and the head and blade 0.9; the head and pommel are flat stepped diamonds rather than cubes turned on a corner; the bend at the top is about 40° (the design's about 45°); the 16 × 16 icon cannot show the bend.
- If the designs were built in a modelling program, their files would give the exact box sizes.
