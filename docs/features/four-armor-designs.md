# Four armor designs: Dread Knight, Valkyrie, Wayfarer and Spartan

Status: implemented on `claude/epic-pascal-f8gpfg`, stacked on `claude/pharaoh-armor` (#246, itself on #241 and #240), since the sets share the lists they are added to. **Not yet compiled, game-tested or played:** the Java compiles only in CI, which has not run on this branch.
Proposal issue: none. On 7 October 2026 the owner sent four armor designs of their own, a sheet of the four on a blank mannequin and a screenshot of each worn in game ("Here is art of new ones that I made!"), and asked: "I want this to just be for creating new armors for the modpack don't worry about crafting or implementation that way yet that will be decided later just create the armors for the modpack and make them usable in game."

Owner: jimbozoomer-byte (design: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: four new armor-only tiers beside the owner's other five ([bloodthorn-armor.md](bloodthorn-armor.md) and the sets after it). Their place in the game, and how they are obtained, are the owner's to decide later; until then they are creative-only.
Primary specialty and supported player role: combat (defense). Everyone who wears armor.

**The names are placeholders.** The sheet's title was crossed out, so each set is named for its look: Dread Knight (the dark crowned knight), Valkyrie (the white and gold winged set), Wayfarer (the blue hooded cloak) and Spartan (the gold plumed set). Nothing has been released, so renaming one now is a find-and-replace and a regeneration; once released, the IDs stay.

## Player experience
Each set is worn as a 3D model on the knight armor's toolkit ([knight-armor.md](knight-armor.md)), built from the owner's pictures part for part, and has four 16 × 16 icons of its own. The pictures show only the front (the sheet from a little above and to the left); the backs and sides are drawn in each design's own words. Preview them with `python3 tools/armor_preview.py --set <set>` (`dread_knight`, `valkyrie`, `wayfarer`, `spartan`).

Since 8 October 2026 each of the four is a Blockbench project, `art/armor/<set>.bbmodel`, which the owner can open
and edit; the modules read it ([blockbench-armor.md](blockbench-armor.md)). They were written from these builds
unchanged.

### Dread Knight (`tools/dread_knight_armor.py`, `art/armor/dread_knight.bbmodel`)
- **Helmet:** a near-black great helm whose top edge is a crown of light grey merlons (a broad one over the brow and one behind, smaller ones at the corners and the middle of each side, notched dark between); on the front, 0.5 proud, a light brow band, a nasal bar with the owner's faint pink sheen, a post beside each eye and a cheek plate under it, the eye slits black between them; a framed dark window on each side.
- **Chestplate:** a dark cuirass under a mottled grey muscle plate (the chest's two plates, the stomach's ridges, a dark line down the middle) and a banded back plate; blocky pauldrons in light and dark bands with two small spikes leaning out from each and a flared lame below; the arm banded to a flared cuff, a black band at the elbow.
- **Leggings:** a black belt with a grey buckle; on each leg a near-black cuisse to the ankle under a skirt of upright strips riveted grey along its foot.
- **Boots:** a light grey cuff at the knee over a banded greave and a light sabaton, longer at the toe.
- Closed all round: no part of the wearer shows, standing, walking, sneaking or sneak-walking, from seven views.

### Valkyrie (`tools/valkyrie_armor.py`, `art/armor/valkyrie.bbmodel`)
- **Helmet:** no helm, as drawn: a gold laurel wreath round the head (broad leaves over the brow, swept leaves along the sides, a curled boss at each front corner) and a feathered wing rising from each temple, five feather planks fanned out to a ragged edge in white, pink and lilac, turned so their broad face looks forward and out. The face shows.
- **Chestplate:** a white muscle cuirass, the chest and the stomach's ridges in mauve and blue-grey; brown straps over the shoulders, buckled gold; red cloth wound three times round each shoulder, its end standing out, and two red streamers hanging behind the arm past the hand; a dark red sleeve on the upper arm; a gold-banded white bracer on the forearm.
- **Leggings:** a brown belt studded gold; on each leg a skirt of brown leather strips studded gold at their ends, its front strips white linen. The knees are bare, as drawn.
- **Boots:** a greave round the shin, gold bands round a white band checked blue-grey, and a small feathered wing at its outer side (the owner's screenshot shows it; the sheet does not). The feet are bare, as drawn.

### Wayfarer (`tools/wayfarer_armor.py`, `art/armor/wayfarer.bbmodel`)
- **Helmet:** a deep hood in mottled navy and blue, 1.25 off the head, its face opening framed by a light teal brim and side rims; a lower step on its top so it rounds off. The face shows.
- **Chestplate:** a cloak: a mantle over the shoulders, two front panels open down the middle over a dark tunic, their inner edges teal, a silver clasp with a teal heart on the left panel, a back panel to the waist, and on each arm the cloak to the elbow (the forearms bare, as drawn). It hangs longer on the right, as drawn: a tail behind each thigh, the right one to the knee, the left one short, on the legs so it follows them.
- **Leggings:** a short kilt of dark brown leather over the thighs, a row of light studs along its hem, under a brown belt. The knees and shins are bare, as drawn.
- **Boots:** dark brown boots from mid-shin, a lighter cuff, two pale laces on the front and a winged ankle, white and ice blue with pink tips.

### Spartan (`tools/spartan_armor.py`, `art/armor/spartan.bbmodel`)
- **Helmet:** a gold Corinthian helm whose face is cut in a T (a slit across the eyes, a gap down to the chin between the cheek guards), so the face shows through it; a red and orange crest from the brow over the crown, ragged with tufts, its tail falling behind the head in two locks toward the right shoulder.
- **Chestplate:** a gold muscle cuirass; red cloth over the right shoulder and the upper arm, a sash rising across the chest toward the left of the neck and a cape down the right half of the back, its tail behind the right thigh (on the leg); on the left shoulder a gold pauldron with a bronze scroll on its outer face and a knob standing out at its foot; gold bracers. The upper arms are bare, as drawn.
- **Leggings:** a brown belt studded gold over a skirt of brown leather strips (pteruges) studded gold at their ends. The knees are bare, as drawn.
- **Boots:** gold greaves standing forward at the shin, a pale knee cap on each, over a brown sandal sole.

### Shape, size and colour
| | Dread Knight | Valkyrie | Wayfarer | Spartan |
|---|---|---|---|---|
| Parts | 46 | 56 | 31 | 36 |
| Quads: helmet, chestplate, leggings, boots | 73, 100, 35, 32 | 140, 124, 18, 48 | 43, 58, 18, 60 | 85, 65, 18, 34 |
| Quads, full set (cap 900, aim 600) | 240 | 330 | 179 | 202 |
| Atlas | 128 × 64 | 128 × 64 | 128 × 64 | 128 × 64 |
| Reach: to the side, above the head | 10.2, 3.0 | 12.1, 6.0 | 8.8, 1.9 | 9.7, 6.0 |

The Valkyrie's ribbon ends reach 12.1 pixels to the side and its top feathers 6.0 above the head, just past the toolkit's warning lines (12 and 6), so they may pop out at the very edge of the screen.

Colours (`tools/armor_paint.py`): `DREAD_KNIGHT`, `VALKYRIE`, `WAYFARER` and `SPARTAN`. The sheet is lit as Blockbench lights a model, so its fronts were taken as about 0.8 of the texture colour (as for Bloodthorn), the game screenshots only to choose between near tones; the Valkyrie's whites are the sheet's own.

### The numbers
`tools/gear.py` `ARMOR_TIERS`, `JugcraftGear.DREAD_KNIGHT_ARMOR` and the three after it: beside the owner's other sets in other strengths, each after its look. These are starting numbers for the owner to set; nothing was asked of them yet.

| | Dread Knight | Valkyrie | Wayfarer | Spartan | Netherite |
|---|---|---|---|---|---|
| Defense (helmet, chestplate, leggings, boots) | 4, 9, 7, 3 (23) | 3, 8, 7, 3 (21) | 3, 8, 6, 3 (20) | 4, 8, 7, 3 (22) | 3, 8, 6, 3 (20) |
| Toughness (each piece) | 3.5 | 3.0 | 2.5 | 3.5 | 3.0 |
| Knockback resistance (each piece) | 0.2 | 0.1 | 0 | 0.15 | 0.1 |
| Durability (multiplier) | 484, 704, 660, 572 (44) | 506, 736, 690, 598 (46) | 550, 800, 750, 650 (50) | 473, 688, 645, 559 (43) | 407, 592, 555, 481 (37) |
| Enchantability | 10 | 24 | 30 | 18 | 15 |
| Fire resistant | yes | no | no | no | yes |
| Repaired with | netherite ingots | phantom membranes | leather | bronze ingots | netherite ingots |
| Equip sound | netherite's | gold's | leather's | gold's | netherite's |

- **Dread Knight** is the heaviest plate of all (23 armor for a set, among the owner's sets the most) and as steady as Hades, but enchants worst.
- **Valkyrie** is a point of defense over netherite with its toughness, long wear and good enchanting; it mends with phantom membranes, as wings do.
- **Wayfarer** is a traveller's cloak and leathers: netherite's defense, the longest wear and the best enchanting of all, but less toughness and no knockback resistance.
- **Spartan** has the heavier helm, is tough and steady, and enchants middling; it mends with Jugcraft's bronze ingots, a hoplite's metal.

**Getting it:** for now only from the creative tab (Combat). No recipe, drop or trade exists, as the owner asked.

## Connections
- Existing input producer: none yet (creative only). The repair items come from vanilla (netherite ingots, phantom membranes, leather) and Jugcraft's alloying (bronze ingots, [tin-and-bronze.md](tin-and-bronze.md)).
- Existing output consumer: the player's armor slots; each piece enchants, trims and equips as any armor of its slot (it is in `#minecraft:head_armor` and the other slot tags).
- Technology connection: none yet. Magic connection: none yet.
- Reachable entry path: not yet, by the owner's decision ("that will be decided later"). Nothing requires these sets, so nothing is blocked on them.
- Which connections are required vs optional: all optional.

## Balance and automation
- **At or above netherite,** so they must come late. Their recipes or drops are the owner's to set, and should cost at least a netherite set's worth, whatever mends them.
- **No conversion loops:** nothing turns them back into materials.
- **Starting numbers,** not played.

## Multiplayer and persistence
- **Plain armor items:** nothing new is saved, and the server handles them as any armor (`JugcraftGear.armorTier`).
- **The 3D models are client-side only** (`client/WornModelLayer`), drawn from `assets/jugcraft/worn_models.json` (39 new entries; none of the 76 earlier ones changed) and the atlases `textures/entity/equipment/3d/<set>.png`.
- **A chestplate's parts on the legs:** the Wayfarer's cloak tails and the Spartan's cape tail are chestplate parts on the leg bones (`wayfarer_chestplate_right_leg` and so on), which the layer already draws for any worn item; worn with any leggings, they hang behind the thigh, 1.1 or more off it.
- **IDs:** `jugcraft:<set>_helmet`, `_chestplate`, `_leggings` and `_boots` for `dread_knight`, `valkyrie`, `wayfarer` and `spartan`, and the repair tags `jugcraft:repairs_<set>_gear`. Placeholder names (above) until the owner confirms them; stable once released.
- **No equipment asset files:** every piece is drawn in 3D, so no `equipment/<set>.json` is written.

## The shared parts it uses and adds
- Each set: one `ARMOR_TIERS` entry, one Java material and one `armorTier(...)` call, a row in `ArmorTiersGameTests`, a module in `armor_models.SET_MODULES`, a palette and four icon maps. `ArmorTiersClientGameTests` shows every tier, so it shoots these with no change.
- **Palette accents** (`tools/armor_paint.py`): a palette may now hold tones beyond the named ramps (the Valkyrie's third gold and feather tints, the Wayfarer's feathers, the Spartan's lit plume tips), used by name in flat paint. No earlier palette or atlas changes.
- **Icon outlines** (`tools/armor_icons.py`): an icon symbol may name a tone as `~<tone>`, that tone taken down to the outline depth, as `O` is, for parts whose own darkest tone is too light for an outline (the Valkyrie's gold, the Wayfarer's feathers). The earlier sets' maps are unchanged.

## Dependencies and assets
- No new dependencies.
- The designs are the owner's; their pictures are not committed. Every model, texture and icon here is drawn by code from them: the four modules, the four palettes and `tools/armor_icons/<set>/`. Nothing is copied or traced from any mod or from vanilla, and no pixel of the owner's pictures is in the textures.
- The owner's asset library ([art/owner-library/README.md](../../art/owner-library/README.md)) was searched first: its armor is other sets (the gun mods' Cog Knight, Treated Brass, Scrap, Diamond Steel and the like, and military helmets), none of these four designs, so nothing was taken from it.

## Verification
**Run locally on 7 October 2026,** on this branch:
- **Generators:** `generate_material_data.py`, `generate_textures.py`, then `generate_material_data.py` again, all exit 0; the second run changed nothing. Besides this work's files, `generate_textures.py` redraws the same 24 flower and entity textures it redraws on main (#240 records them); they are left as committed.
- **`check_mod_data.py`:** PASS (1574 material IDs), with `check_armor_tiers` and the art check (`tools/art_check.py`): every one of the 39 new entries is 0.00% see-through (H1, limit 0.5%), with no flicker findings (Z2, N2). Its first run caught a hole (the Wayfarer boot's toe, 0.6% see-through), now closed.
- **`check_repository.py`**, **`check_icon_maps.py`:** PASS. **`armor_smoke.py --no-render`:** all checks pass.
- **`armor_models.py`:** no problems; warnings only for the Valkyrie's ribbons and top feathers (above).
- **Java:** the two changed files parse (a syntax-only parse with the JDK's own parser; nothing compiles outside CI).
- **Head poses** (a scratch check of the head's faces against every other bone's, posed): with the head turned up to 75 degrees each way and looking from straight up to straight down, standing and sneaking, no face of a head piece lies within 0.15 pixel of a parallel face of another piece. It found the hood's and the Spartan helm's sides in the planes of the mantles at the neck, the Spartan pauldron's front in the helm's jaw plane, and three planes met only when looking straight up or down; all were moved apart.
- **Wearer audit** (`armor_preview.py --wearer`): the Dread Knight shows 0.00 model px² of the wearer over standing, walking, sneaking and sneak-walking from seven views each; with its boots left off it shows 5.7, so the audit does see the wearer. The other three leave the face, the forearms or upper arms, the knees and the feet open by design, so they were looked at rather than totalled.
- **Icons,** checked by a scratch script: each part outlined in its own darkest tone, never pure black; no fill touching transparency; no stray outline pixel and no outline two pixels thick; light from the top left.
- **Renders** with `armor_preview.py` (not committed): each set beside the owner's sheet and screenshot, front, three-quarter and back; walking, sneaking and sneak-walking from three views.

**In CI:** not run yet. It will compile the four materials and their registrations, run `ArmorTiersGameTests` on the server (the numbers above, typed in), and run `ArmorTiersClientGameTests`, which writes the `jugcraft_armor_tier_<set>_*` shots.

**Not run:** the client by hand, a two-client dedicated server, and any play.

## World and event applicability
Not applicable yet: the sets are placed in no structure, loot table or boss drop.

## Rollout and open questions
- **Names:** the four names are placeholders (above).
- **How they are obtained,** and **their numbers,** as the owner decides; until then creative-only.
- **The flames and smoke in the screenshots** were taken as the scene, not the armor; no particle effect is added. The owner may want one.
- **What is open by design:** the Valkyrie's, Wayfarer's and Spartan's faces, the Wayfarer's and Valkyrie's forearms, the Spartan's upper arms, and the knees of all three, as drawn; the Valkyrie's feet. On an armor stand those parts show the stand.
- **Where the models differ from the designs** (each can be changed):
  - **Dread Knight:** the crown's merlons are boxes on the helm's top edge; the owner's zigzag rim reads a little finer. A head turned far sweeps the helm's corner through the inner spike on each pauldron.
  - **Valkyrie:** the wings are fans of five straight planks, so their edge steps rather than curving; the shoulder cloth is three wound bands, chunkier than the owner's.
  - **Wayfarer:** the hood is blocky where the owner's rounds off, and its opening square-cut; the long side of the cloak is a tail behind the thigh rather than a fall beside the arm, so it follows the leg when walking.
  - **Spartan:** the crest is four stepped blocks with tufts, its tail two straight locks; looking straight up swings the tail's foot against the cape.
- **Known limits of the 3D layer,** as for every set: trims are kept but not drawn on the 3D models; babies wear nothing visible; mobs with odd proportions (a zombie villager's head, a skeleton's thin limbs) wear them loosely.
