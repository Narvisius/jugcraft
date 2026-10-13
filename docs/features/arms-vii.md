# Arms VII: variant arms, crafted styles and boss trophies (batch 56)

Status: implemented on `claude/arms-variants`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026, after the arms restyle ([arms-restyle.md](arms-restyle.md)): "refine and make more variants". Asked how, they chose:
- **New weapon items:** "they dont all need recipes yet a bunch can be drops from bosses ill make later you can theme them around them though example: Yeti King".
- **All four styles:** gilded, dieselpunk, bone & beast, runic glow.
- **Some craftable:** "make some for boss drops brainstorm bosses we could make also make some craftible".

Owner: jimbozoomer-byte
Target milestone and tier: steel age (the machines feature), on [arms.md](arms.md) to [arms-vi.md](arms-vi.md).
Primary specialty and supported player role: fighting; smithing for crafters; trophies for a future boss branch ([branches/BOSSES.md](../branches/BOSSES.md)).

## Player experience
35 named arms, each a variant of an existing kind with its own look and a perk or boon, in three kinds of line: crafted styles, boss trophies and, since 7 October 2026, an armor set's arm. All are in the creative Combat tab. Every one fights as its kind does: the same swing, reach, trait, two-handed blow, weapon art and motion.

**Crafted styles: 16 arms in four styles.** Each is made at a smithing table from:
- the style's pattern (a smithing template, crafted);
- a steel arm of the kind;
- the style's material.

The arm keeps its enchantments and wear.

| Style | Pattern (recipe) | Material | Arms | Perk |
|---|---|---|---|---|
| Gilded: polished steel, gold, royal-blue velvet, sapphires | Gilder's Pattern: 8 gold nuggets round paper | gold ingot | longsword, rapier, sabre, halberd | takes enchantments as gold does (22; steel's 12) |
| Ironclad (dieselpunk): gun steel, olive drab, hazard stripes, rubber grips | Ironclad Pattern: yellow and black dye round a steel plate | steel plate | zweihander, maul, war pick, battle axe | lasts twice as long (1,800) |
| Bonecarved: bone, horn, leather, a garnet eye | Bonecarver's Pattern: bone, flint, leather, paper | bone block | dagger, flail (a horned skull on a spine of vertebrae, swinging free in the hand since 5 October 2026: [arms-restyle.md](arms-restyle.md#the-flails-head-swings-5-october-2026)), glaive (a jawbone blade), labrys (shoulder-blade bits) | **Gravebane:** 20% harder against the undead |
| Runebound: void-dark steel with runes that glow cyan | Runecarver's Pattern: amethyst, ectoplasm, paper | ectoplasm | nodachi, moonblade, staff (quarterstaff), war hammer | **Mark:** a struck foe glows for 4 s, seen through walls |

**Boss trophies: 16 arms, two for each of eight bosses.** They have no recipe. Each boss's loot table is ready to drop one of its two (see [branches/BOSSES.md](../branches/BOSSES.md)). The Yeti King and the Cinder Tyrant are made, and drop theirs ([yeti-king.md](yeti-king.md), [cinder-tyrant.md](cinder-tyrant.md)); the other six bosses are still to be made. They last twice as long as steel, carry epic rarity, and have a boon:

| Boss | Trophies | Boon |
|---|---|---|
| the Yeti King | Glacier Maul (maul), Rimeclaw (katar) | **Frost:** Slowness II, 3 s |
| the Cinder Tyrant | Cinderbrand (greatsword), Magmaw (earthbreaker) | **Ember:** sets the foe alight, 3 s |
| the Mire Hag | Hagthorn (scythe), Bogfang (kama) | **Venom:** Poison, 4 s |
| the Crypt Lich | Soulreaver (moonblade) | **Drain:** each hit heals you half a heart |
| the Crypt Lich | Gravewarden (executioner) | **Wither:** Wither, 3 s |
| the Iron Dreadnought | Dynamo Halberd (halberd), Piston Hammer (war hammer) | **Shock:** arcs to the nearest other foe within 4 blocks, for 30% of the blow |
| the Alpha Werewolf | Moonfang (sabre), Howler (twinblade) | **Howl:** Weakness, 3 s |
| the Storm Roc | Stormcaller (glaive), Galefeather (estoc) | **Gale:** throws the foe up and back |
| the Abyssal Leviathan | Tidebreaker (war fork), Leviathan's Hook (bill) | **Tide:** 25% harder against a foe in water or rain |

**An armor set's arm: the Hades Scythe.** The owner, 6 October 2026: "I also want the scythe from my Hades Armor set." The owner's armor sets are a third kind of line beside the styles and the bosses (`SETS` in `tools/arms_variants.py`, `ArmVariants.SETS`). Like a trophy, a set's arm has no recipe, carries epic rarity, lasts twice as long as steel and has a boon. Unlike a trophy, nothing drops it yet. The owner will settle how the sets are won ("They might get dropped by bosses or be craftable for now just make the armor we can figure that out later"), so until then it is creative only. See [The Hades Scythe](#the-hades-scythe) below. Since 8 October 2026 three more of the owner's sets have an arm, drawn from the designs they sent with it ("Just made these ones aswell want them done weapons too please", and a render of the Wight King holding its sword): see [The Sentinel's and the Frost Knight's arms](#the-sentinels-and-the-frost-knights-arms) and [The Wight King's arm](#the-wight-kings-arm).

| Armor set | Arm | Boon |
|---|---|---|
| Hades Armor | Hades Scythe (scythe), `jugcraft:hades_scythe` | **Wither:** Wither, 3 s (the Gravewarden's boon) |
| Sentinel | Sentinel Longsword (longsword), `jugcraft:sentinel_longsword` | **Mark:** a struck foe glows for 4 s, seen through walls (the Runebound arms' boon) |
| Frost Knight | Frost Knight Greatsword (greatsword), `jugcraft:frost_knight_greatsword` | **Frost:** Slowness II, 3 s (the Yeti King's boon) |
| Wight King | Wight King Zweihander (zweihander), `jugcraft:wight_king_zweihander` | **Drain:** each hit heals its wielder half a heart (the Soulreaver's boon) |
| Reaper | Reaper Scythe (kama), `jugcraft:reaper_scythe`, one for each hand | **Wither:** Wither, 3 s (the Hades Scythe's boon) |

A set may also have a shield of its own shape (`SET_SHIELDS`): the Sentinel's four-pointed star, `jugcraft:sentinel_shield`, which blocks as a steel heater shield does. See [The Sentinel's shield](#the-sentinels-shield).

**A trophy with its boss: the Vesper Scythe (10 October 2026).** Vesperine, the Last Reaper, the first Witching Season boss, drops a ninth boss's trophy: the **Vesper Scythe** (scythe), `jugcraft:vesper_scythe`. It comes from her loot, 15% a kill and certain on a player's first, and has a new boon:
- **Harvest:** a kill heals you two hearts, at most once every 5 seconds;
- every fifth kill charges your next blow to loose a pale crescent, 12 blocks, 8 damage to each foe it passes.

Its line is hers (`BOSSES["vesperine"]`, "Trophy of Vesperine, the Last Reaper"), drawn in a new moon-steel style. See [vesperine.md](vesperine.md).

**A second trophy with its boss: the Needle Rapier (10 October 2026).** Madame Tatterlace, the second Witching Season boss, drops a tenth boss's trophy: the **Needle Rapier** (rapier), `jugcraft:needle_rapier`. It comes from her loot, 15% a kill and certain on a player's first, and has a new boon:
- **Stitch:** three hits on the same foe within 4 seconds stitch it, Slowness II for 2 seconds, and the count starts again.

Its line is hers (`BOSSES["tatterlace"]`, "Trophy of Madame Tatterlace"), drawn in a new needle-steel style: a long needle for a blade, its eye just above the hilt; a gold swept hilt and knuckle bow with an amethyst at its heart; a red velvet grip; and a gold thimble for a pommel. See [tatterlace.md](tatterlace.md).

**Looks:**
- Each arm is drawn with the restyle's toolkit: a pixel-art icon on the diagonal and a 3D model in the hand.
- Glowing parts are lit at full brightness in the hand, so they show in the dark: runes, magma, venom, soul fire, charged coils and lightning.
- Tooltips name the kind's trait or art, the boon (in aqua) and the line (in purple: the style's perk, "Trophy of …" for a boss's arm, or "Of the Hades Armor set" for a set's).
- **The Runebound arms are smooth 3D models in the hand** (see [Runebound meshes](#runebound-meshes) below); the other 31 keep the restyle's pixel look.
- **Second pass (the owner: "dont overcomplicate them"):** each design was checked against the studied mods at 8× and kept to one or two accents.
  - **Gilded:** the rapier and sabre use the base arms' plainer hilts in gold. The halberd uses the larger halberd head, with a gold hook and one sapphire, and no chasing or tassels.
  - **Ironclad:** no bolt grids.
  - **Runebound:** the runes are one unbroken glowing line, not dashes, and the war hammer carries a single rune diamond.
  - **Cinder Tyrant:** the Cinderbrand has a straight molten core instead of a web of cracks. The Magmaw has one seam of magma and no teeth.
  - **Iron Dreadnought:** the Dynamo Halberd is the halberd head with a charged edge and a copper coil, without hazard band, bolts or loose arcs. The Piston Hammer has two rings and one vent.
  - **Abyssal Leviathan:** the arms lose the coral. The Tidebreaker's tines rise from a bronze crossbar round a pearl. Leviathan's Hook is the bill's hook, with one glowing tide line and a pearl spike.
  - **Rimeclaw:** an open frame with one fur grip.

## Runebound meshes

The owner, 5 October 2026: "the runebound weapons need to look much better if possible don't even use minecraft esque textures make like a nicer 3d model and use that to make it really be cool and special". The four Runebound arms are now drawn in the hand as smooth meshes instead of boxes, and their icons are rendered from them. Nothing else about them changes.

- **Tier, inputs, outputs, costs:** unchanged. The same items (`jugcraft:runebound_nodachi`, `runebound_moonblade`, `runebound_staff`, `runebound_war_hammer`), recipes (a steel arm, the Runecarver's Pattern and ectoplasm at a smithing table), stats and Mark boon. It is a client-side look only: no server state, no saved data, no new item.
- **The looks** (one or two accents each, as the owner asked before):
  - **Moonblade:** a crescent of violet moon steel ground to a bright edge, with a raised bead of glowing stave runes following the crescent; a crescent-moon guard with glowing horn tips round a heart crystal in a silver bezel; a spiral cord grip between iron ferrules; a faceted crystal pommel in an iron cup.
  - **Nodachi:** one continuous curve whose edge is tempered in a glowing wave, a white line running along the wave; a short panel of glowing runes in the flat by the habaki; a gold habaki; an oval tsuba with a ring of light round its rim; a violet silk tsuka with windows of pale ray skin, between an iron kashira and fuchi.
  - **Staff:** dark ironwood with a glowing helix winding up each half, thinning and diving into the wood at its ends, short of the iron; a leather grip between iron collars; at each end an iron ferrule whose three claws hold a floating crystal.
  - **War Hammer:** a flared, chamfered head of moon steel with a glowing diamond cut into each side, a white diamond raised in it and a stave through both (the Runecarver's emblem), and a moon-gold band behind its face; a curved beak and a top spike; iron langets down an iron-banded ironwood haft; a spiral cord grip coming out of a collared iron pommel.
  - The runes, the temper and the crystals glow at full light (they show in the dark) and pulse gently (an animated texture, blended between frames).
  - **Second pass (review, 5 October 2026):** the first meshes cut the glow as a thin channel between near-black walls, which read as a dark slot and popped in and out of sight at third-person sizes; the temper line, glyphs, tsuka diamonds and cords were low-resolution texture, stair-stepped in first person; the icons were soft renders, darker than their siblings. Now the Moonblade's glow is a raised bead about 0.7 pixel wide and the Nodachi's is its tempered edge (each a steady line at 56 pixels in an offline rotation test; the Nodachi's short rune panel fills its narrow flat, about 0.3 pixel), the signature details are shapes in the mesh, and the icons are drawn in flat tones. The staff's helix stops short of its ferrules and collars and dives into the wood there, the hammer's pommel seam is a clean collar, and every part is closed.
- **How it is built:**
  - `tools/arms_mesh.py` builds each arm in its design's units (the restyle design it replaces: the same length and grip), from lofted blades (ground to an edge, a rounded bead of light set in the flats, the nodachi's temper a split in the bevel), lathe-turned parts, a diamond-tiled tsuka, swept tubes and faceted crystals. Normals are given at every corner and smoothed within each panel, so curves are smooth and creases stay sharp. It is laid on the diagonal by the same 45-degree turn about the hand as the box model, and keeps that model's hand poses, so it is held in exactly the same place in both hands, in first and third person and on armor stands.
  - The model JSON (`models/item/runebound_*_in_hand.json`) lists the quads under the Fabric model type `jugcraft:mesh`, beside the box model's `elements`. It goes through the shared model writer like every other model (so the box elements get the same post-processing) and is then rewritten a quad a line. `client/MeshItemModels.java` reads it through Fabric API's model loading API (`UnbakedModelDeserializer`) and bakes a renderer API mesh. Fabric draws that as any item model, with the enchantment glint and the arms' motion.
  - Textures: `item/runebound_mesh` (128×128, flat painted materials and gentle ramps: moon steel, the ground edge, ground steel, iron, silver, moon gold, spiral cord, leather, ironwood, ray skin, silk, crystal) and `item/runebound_rune` (64×64 frames, animated: eight stave runes on 16×16 cells, a banded ring, the hammer's stave, a plain glow and a white core). Every boundary drawn in them runs straight along the texture's rows or columns, so nothing stair-steps when the arm fills the screen.
  - Icons (48×48), rendered offline from the meshes by `arms_mesh.icon`: tipped back 35 degrees, thickened where an arm is thin (the Nodachi 1.3×, the Staff 1.15×, the War Hammer 1.12×), each pixel the flat icon tone of the material and light band most of its 36 subsamples show, with a one-pixel outline that turns cyan beside a glowing part. 19 to 22 colours each (the steel siblings use 15 to 19), and about as bright as the siblings.
- **Failure behaviour:** the type is marked `optional`: if the mesh loader is not registered, Minecraft loads the file as a vanilla model from its `elements`, the old box model. If the quads cannot be read, or the mesh cannot be baked, the loader draws the same box model, logs a warning and counts it (`MeshItemModels.FALLBACKS`). The arm never goes missing.
- **Checks** (`tools/check_mod_data.py`, `check_mesh_models`): every quad has four corners of eight numbers, unit normals, UVs within its sprite and corners within −16..32; only the glyph strip and the crystal regions glow; at most 2,000 quads an arm; every part is closed (no edge belongs to one quad only); no two surfaces within 10 degrees of parallel lie closer than 0.1 pixel where they overlap without crossing at 2 degrees or more, compared triangle by triangle as drawn, back to back as well as face to face (they could flicker); the mesh runs from the design's butt to its point along the diagonal from the hand, as the box model did; both textures are solid. Run on the first meshes, the same check fails them, as the review did: 10, 23, 100 and 172 near-parallel pairs (Nodachi, Moonblade, Staff, War Hammer) and the hammer's 40 open edges; on these meshes it finds none.
- **Offline evidence** (not a game test; previews drawn by scratch renderers from the generated meshes): close-ups from four directions of every part; a 56-pixel rotation sequence in 2.5-degree steps with nearest sampling, where the Moonblade's bead and the Nodachi's tempered edge stay a continuous line in every frame; first-person crops at 1920×1080 with no stair-stepped boundary; icons at 6× and in simulated hotbars at GUI scales 2 and 3 beside the gilded, ironclad, bonecarved and steel arms.
- **Client game test** (`RuneboundClientGameTests`, CI): asserts the `jugcraft:mesh` loader is registered, at least four mesh models were baked as meshes (`MeshItemModels.BAKED`) and none fell back to its box model (`FALLBACKS`), then shoots the four on a close rack of armor stands by day and at midnight, each held from the front by day, a Moonblade with the enchantment glint, the Moonblade and Nodachi in first person by day and the Staff at night. **Not yet run in CI** at the time of writing.
- **Not verified yet:** how the smooth per-corner lighting looks in game (expected, since Fabric's renderer passes each corner's normal to the item shader, but unseen until the CI screenshots); how bright the glow reads by day (glowing quads ignore the light level but, like vanilla's glowing elements, are still shaded by the item shader's direction lighting, so by day they read cyan to teal and at night bright: the offline renders model this); third-party renderers such as Sodium/Iris drawing Fabric item meshes; whether the `enchantment_glint_override` component syntax in the test's command is right for 26.3 (if not, that one shot shows no glint).

## The Hades Scythe

The owner, 6 October 2026, with the design of their Hades Armor set: "I also want the scythe from my Hades Armor set." The design is a render of the armored figure, the scythe and the pieces. It is a reference only and is not committed. The armor has its own record; this section covers its arm.

- **Tier, inputs, outputs, costs, unlocks:**
  - `jugcraft:hades_scythe` is an arm of the scythe kind in steel. It has the steel scythe's blow (8.5 damage, speed −3.0), reach, Reap trait, two-handed blow and motion.
  - Its boon is the existing Wither (Wither, 3 s), the Gravewarden's. Its best second is 10.06, under netherite's 12.8.
  - It is epic and lasts twice as long as steel (1,800), as a trophy does.
  - It has no recipe and no loot table. It is creative only, in the Combat tab, until the owner settles how the armor sets are won.
  - It costs nothing and unlocks nothing, and nothing needs it.
- **The armor sets' line, where it differs from a boss's:**
  - **Tables:** `SETS = {"hades": {"display": "Hades Armor"}}` sits beside `STYLES` and `BOSSES`, and is part of `LINES`. Java has `ArmVariants.SETS`, held to Python by `check_arms_variants`. Java needed nothing else: every line that is not a style is epic and lasts as a trophy does (`ArmVariants.register`), and its tooltip line is a name only (`ArmItem.traits`).
  - **Loot tables:** each boss has a table, `loot_table/bosses/<boss>.json`; a set has none. `check_arms_variants` fails if a set has a table under `bosses/`, or if any loot table names a set's arm.
  - **Recipe:** none, as for a trophy. The check's message names the set instead of calling the arm a trophy.
  - **Tooltip:** "Of the Hades Armor set", where a trophy says "Trophy of the Mire Hag". The check now also confirms that each boss's line names its boss and each set's line names its set.
  - **Handbook:** a page, "Arms: Armor Sets", after "Arms: Trophies". It names the set, its arm and the boon, and says that how they are won is still to be settled.
  - **Tests:** see Verification below. The trophy-table test would have looked for `bosses/hades` and failed; it now counts as bosses only the lines that are neither styles nor sets.
- **The look, drawn fresh by code from the owner's design:**
  - **The 3D model in the hand** (`hades_scythe` in `tools/arms_variants_art.py`) is in the arms' box style.
    - It is 53 design units long, as the scythe is, and held at 13, the middle of its red wraps (the scythe's hand is at 10).
    - It uses the scythe's hand poses, scaled 1.92 instead of 1.79, so the broader design is held at the scythe's length.
    - **Pommel:** a slate diamond 7 texels across. Its two lower edges are trimmed light, three rows deep, as the armor's plates are edged, round a bluish core.
    - **Snath:** blue-black, 4 texels wide, with two soot-black rings a texel wider each side. Near the top it bends out to the head at about 40°, with a soot collar just past the bend.
    - **Grip:** two blood-red wraps between three dark-red bands, two texels each.
    - **Head:** a slate diamond 12 texels across, trimmed light along its lower edges, round a bluish core. A short spike points out of its back corner.
    - **Blade:** its back is a cubic Bézier from the head's upper face, falling away to the left and turning down to the point, fitted to the design's proportions. It is 7 units broad, narrowing over the last third: slate along the back, lighter towards the edge, and a bright edge.
    - **Depths:** the blade is 1.6 deep, the snath 4, the pommel 4.2 and the head 4.6.
  - **The palette** is the `HADES` style: `SLATE`, `ASHEN` for the light trim, `NIGHT` for the snath, `SOOT` for the rings and `BLOOD` for the wraps. It was matched by eye and by the tone ratios measured from the design's render; no texture was copied.
  - **The 16×16 icon** is the map `tools/arms_icons/hades_scythe.txt` (`# family: polearm`), the first Arms VII variant drawn as a map:
    - span 14; blade 7 steps (0.50); 118 opaque pixels, against the scythe's 111;
    - a pale head with a slate core, a short spike below it, a broad slate blade, the dark snath with red wraps and a pale pommel.
    - Because a variant of the line is now a map, `check_icon_maps` checks all six of the Hades line's materials strictly. All pass; the slate is 18.4 from iron and 29.5 from copper.
- **Where it differs from the owner's design:**
  - **Proportions:** for its length, the snath and the pommel are about 0.8 of the design's breadth, and the head and the blade about 0.9. The arms' texels are finer than the design's voxels, and the slimmer snath keeps it close to its siblings.
  - **Head and pommel:** they are flat diamonds in the blade's plane, stepped a texel at a time, not cubes turned on a corner as drawn. The light trim and the bluish core stand in for the cube's faces.
  - **The back spike** is our reading of the head's back corner.
  - **The bend** is about 40°, against roughly 45° in the design.
  - **The icon:** the bend does not show at 16 pixels, where the neck runs straight into the head. The blade's light edge is on its back, because the icon rules light the upper-left edge; in the model it is on the cutting edge, as drawn.
  - **The palette is ours,** measured from the design. The armor is drawn separately, so the two should be compared side by side in game and matched if they differ.
- **Failure behaviour:** an ordinary item. Nothing ticks, and no saved state is added beyond the item.
- **Offline evidence** (not a game test): previews drawn by a scratch renderer from the generated files, not committed:
  - the model upright, from the front and three angles, beside the design's scythe;
  - the model on the diagonal, beside the steel scythe's and the Hagthorn's;
  - the icon at 1×, 2× and 8× on light and dark slots, beside the steel and bronze scythes, the steel halberd and glaive, and the Hagthorn.
- **Not verified yet:**
  - nothing has been run in game or in CI;
  - the owner has not seen it;
  - how it matches the Hades Armor in game.

## The Sentinel's and the Frost Knight's arms

The owner, 8 October 2026, sending the designs of two more armor sets, a gold-and-black knight with a sword and a star-shaped shield and a white knight crowned with ice with a glowing ice sword: "Just made these ones aswell want them done weapons too please". The armor is in [armor-designs-8-october.md](armor-designs-8-october.md); this section covers the swords and the shield. The renders are references only and are not committed.

- **Tier, inputs, outputs, costs, unlocks:** as the Hades Scythe's. Each is an arm of its kind in steel, with that kind's blow, reach, trait, motion and (for the greatsword) two-handed blow; epic, lasting twice as long as steel (1,800); no recipe and no loot table, so creative only in the Combat tab until the owner settles how the sets are won. They cost nothing and unlock nothing, and nothing needs them.
  - `jugcraft:sentinel_longsword`: a longsword with the existing **Mark** boon (the struck foe glows for 4 s).
  - `jugcraft:frost_knight_greatsword`: a greatsword with the existing **Frost** boon (Slowness II, 3 s).
- **The tables:** `SETS` gains `sentinel` ("Sentinel") and `frost_knight` ("Frost Knight"), and `VARIANTS` the two arms, in `tools/arms_variants.py` and `weapons/ArmVariants.java` alike. The tooltip's line reads "Of the Sentinel set" and "Of the Frost Knight set"; the handbook's "Arms: Armor Sets" page lists them, from the same table.
- **The look, drawn fresh by code from the owner's designs** (`tools/arms_variants_art.py`):
  - **Sentinel Longsword:** a broad gold blade, pale along its edge with a brown groove down its middle; a straight gold crossguard with square ends, a jet square in a pale frame at its heart (the square ring on the armor's arm); a brown leather grip; a gold pommel set with jet. Materials `SENTINEL_GOLD`, `SENTINEL_LEATHER` and `SENTINEL_JET`, matched to the armor's palette.
  - **Frost Knight Greatsword:** a long blade of glowing ice (`FROST_ICE`, a glowing material, so it is lit at full brightness in the hand), brightest down its middle; a crossguard of white frost (`FROST_WHITE`) flaring into three jagged spikes each side, frost creeping up the blade's foot, an ice gem at its heart; a white wrapped grip; an ice diamond for a pommel.
  - **Icons:** two 16×16 maps, `tools/arms_icons/sentinel_longsword.txt` and `frost_knight_greatsword.txt`, on the longsword's and the greatsword's shapes, so `check_icon_maps` holds both lines' materials strictly. The frost line's two outlines were darkened to pass it (the ice's to 51 luma, the white's to 44).
- **Not verified yet:** nothing has been run in game or in CI; the owner has not seen them.

### The Sentinel's shield
The Sentinel's design carries a shield, a four-pointed star of gold. It is not a variant (variants are swung kinds), so a set's shield is a table of its own: `SET_SHIELDS` (with `SHIELD_SHAPES`) in `tools/arms_variants.py`, `ArmVariants.SET_SHIELDS` in Java.

- **Tier, inputs, outputs, costs, unlocks:** as a set's arm. `jugcraft:sentinel_shield` is an `ArmShieldItem` (Arms VI's shield item: held and raised as vanilla's shield is) built by `JugcraftArms.shield` from the steel heater shield's numbers, so it blocks exactly as that shield does: up in 0.1 s, 90 degrees either side of ahead, an axe's blow stops it for 0.8 of vanilla's time, a blocked blow wears it as vanilla's. Epic, it lasts twice as long as the steel heater shield (1,800), is repaired with steel and enchants as steel does, and is in `#minecraft:enchantable/durability`. No recipe and nothing drops it: creative only, in the Combat tab.
- **Tooltip:** its shape's trait, **Quick Raise** ("A four-pointed star of gold, quick to raise as a heater shield is."), then its set's line, "Of the Sentinel set", as the set's arm shows it. The handbook's "Arms: Armor Sets" page lists it beside the longsword.
- **The model** (`tools/arms_kit.py` `star_elements`, 52 boxes, held by vanilla's shield poses as the kit's shields are): the star stepped out in rows a pixel tall, its half width falling from 7 at the side points to half a pixel at the top and bottom points (`STAR`, 14 × 21 pixels); on it a raised diamond frame (`STAR_FRAME`, 0.75 proud), inside the frame a recess, and in the recess a chequered diamond (`STAR_CENTRE`, 0.4 proud) with a stone at its middle (`STAR_STONE`). Every edge lies on a half pixel, so the face texture's 2 texels a model pixel meet each box's edge exactly.
- **The paint** (`tools/arms_kit_art.py` `star_face`, by model position through `star_part`, so each raised part's front carries its own): the star gold, lit from the top left, a light edge towards the light and a dark one away from it, the frame's shadow below and to its right; the frame the palest gold; the recess the darkest brown; the centre chequered in pixel squares round a pale stone. Its edges are plain gold, its back dark boards with a brown leather strap and gold rivets. Materials as the longsword's (`SET_SHIELD_MATERIALS` in `tools/arms_variants_art.py`). The kit's back and trim painters now take their materials as arguments; the kit's own shields' textures are unchanged.
- **Checks:** `check_arms_variants` holds `ArmVariants.SET_SHIELDS` and `SET_SHIELD_METAL` to the Python, and checks that each set shield is of an armor set, has a shape with a model and both tooltip keys, blocks as a kit shield that exists, and has no recipe and no loot table naming it. `ArmsVIIGameTests.setShieldsBlockAsTheirBase` checks it in game: its kind and set, its blocking (delay, axe cooldown, cover) against the steel heater shield's, twice that shield's durability, epic. `ArmsVIIClientGameTests` holds it in the off hand beside the longsword, then raises it (`jugcraft_arms_vii_held_sentinel_shield`, `jugcraft_arms_vii_sentinel_shield_raised`).

## The Wight King's arm

Among the pictures the owner sent on 8 October 2026 without words are two renders of a slate knight crowned with icicles and antlers, holding a long sword. The armor is the Wight King ([armor-designs-8-october.md](armor-designs-8-october.md#wight-king-toolswight_king_armorpy)); this is its sword.

- **Tier, inputs, outputs, costs, unlocks:** as the other set arms. `jugcraft:wight_king_zweihander` is a zweihander in steel (its blow, reach, Wide Cleave, motion and two-handed blow), epic, lasting twice as long as steel (1,800), with the existing **Drain** boon (each hit heals its wielder half a heart; a zweihander's best second, with Drain healing rather than hurting, is the Ironclad Zweihander's). No recipe and no loot table: creative only.
- **The tables:** `SETS` gains `wight_king` ("Wight King") and `VARIANTS` the zweihander, in `tools/arms_variants.py` and `weapons/ArmVariants.java` alike; its tooltip's line reads "Of the Wight King set".
- **The look** (`tools/arms_variants_art.py` `wight_king_zweihander`): a long slate blade, pale along its edges, grooved dark down its middle; a crossguard of three jagged slate shards each side, swept toward the point, a glowing cyan gem at its heart; a dark grip; a slate pommel set with a cyan stone. Materials `WIGHT_SLATE`, `WIGHT_DARK` and `WIGHT_GEM` (glowing), matched to the armor's palette. Icon: a 16×16 map on the greatsword's shape, `tools/arms_icons/wight_king_zweihander.txt`; `check_icon_maps` holds the line's materials strictly, and they pass.
- **Not verified yet:** nothing has been run in game or in CI; the owner has not seen it. `ArmsVIIClientGameTests` holds it from the front by day with the others.

## The Reaper's arm

Among the pictures the owner sent on 8 October 2026 without words is a render of a hooded reaper with two white crescents about it. They were first built as part of its armor; the owner corrected that: "He is supposed to be holding 2 short scythe weapons they arent part of the armor". The armor is the Reaper ([armor-designs-8-october.md](armor-designs-8-october.md#reaper-toolsreaper_armorpy)); this is its scythe.

- **Tier, inputs, outputs, costs, unlocks:** as the other set arms. `jugcraft:reaper_scythe` is a kama in steel (its quick hooking cuts, its reach and its trait, Clear Brush), epic, lasting twice as long as steel (1,800), with the existing **Wither** boon (Wither, 3 s; a kama's other is the Bogfang's Venom). No recipe and no loot table: creative only. It is one item; the owner's figure holds one in each hand, so a player carries two, the off hand's for show (vanilla strikes with the main hand only).
- **The tables:** `SETS` gains `reaper` ("Reaper") and `VARIANTS` the scythe, in `tools/arms_variants.py` and `weapons/ArmVariants.java` alike; its tooltip's line reads "Of the Reaper set".
- **The look** (`tools/arms_variants_art.py` `reaper_scythe`): a crescent of five bone-white links laid round an arc from the head to a point (`REAPER_CENTRE`, `REAPER_RADIUS`, `REAPER_FROM` to `REAPER_TO`), broadest at the head and narrowing, each a white frame round a thinner slate hollow, every other one standing further out as the owner's zig-zag; a short bone-white handle wrapped in slate, a slate collar under the crescent and a slate butt. Materials `REAPER_BONE` (a bone white, warmer than iron's so the two never read alike) and `REAPER_SLATE`, matched to the armor's palette. Icon: a 16×16 map of the crescent on its handle, its links' joints dark, `tools/arms_icons/reaper_scythe.txt`; `check_icon_maps` holds the line's materials strictly, and they pass.
- **Not verified yet:** nothing has been run in game or in CI. `ArmsVIIClientGameTests` holds it from the front by day with the others, and then one in each hand.

## Connections
- **Existing input producer:**
  - steel arms (the steel foundry and the arms' own recipes);
  - gold, steel plates (the plate press), bone blocks;
  - ectoplasm (ghost hunting: catching restless spirits);
  - paper, dyes, flint, leather, amethyst.
- **Existing output consumer:** the player against mobs and, where PvP is on, players.
- **Technology connection:** steel and the plate press (ironclad); the arms' tiers.
- **Magic connection:** the runebound style needs ectoplasm, from the ghost-hunting branch.
- **Reachable entry path:** a steel arm, then the pattern's ingredients, then a smithing table. All existing and craftable solo. No circular unlock.
- **Required vs optional:** all optional.
  - The styles can be crafted solo or traded.
  - The trophies wait for their bosses. Until then they are creative-only, and that is on purpose: [branches/BOSSES.md](../branches/BOSSES.md) is a proposal, and no core progression needs a trophy.
  - The Hades Scythe, the Sentinel Longsword, the Sentinel Shield, the Frost Knight Greatsword, the Wight King Zweihander and the Reaper Scythe wait for the owner to settle how the armor sets are won (a boss's drop or a recipe). Until then they are creative-only on purpose, and nothing needs them.
- **How the specialty stays useful:** a style is a look and a small perk, not a stronger tier. The arms of batches 42 to 55 stay as good.

## Balance and automation
- **A variant's blow is its kind's in steel** (the same attack damage and speed). What differs is its line's perk or boon.
- **Every boon is bounded:**
  - an effect lasts at most 5 s at amplifier at most 1, and another hit refreshes it, never stacks it;
  - a share is at most half a blow.
- **`check_arms_variants`:** no variant deals as much a second as a netherite sword (12.8), even with its boon at its best. Its bonus share is counted, as are fire (1 a second), Poison (0.8) and Wither (0.5). The best is the Bonecarved Dagger against the undead, at 12.42. Gravebane was first a flat 2.5 and Tide 40%; the check caught those at 16.1 and 13.5, and they were made shares.
- **Shock** strikes only one other foe, and only for a player wielder and a foe that player may strike (allies, mounts and protected foes are spared, as the two-handed blows spare them).
- **Smithing:** each style variant costs the steel arm plus its material and pattern. Nothing is returned or recycled, so there is no conversion loop. The recipe audit counts a style variant as its steel arm plus its material's metal.

## Multiplayer and persistence
- **Server authority:** every boon is worked on the server, in `ArmItem.hurtEnemy` and `getAttackDamageBonus`, when the arm strikes. Clients only see the effects and particles.
- **Saved state:** none beyond ordinary items with stable ids:
  - the 39 variants: `jugcraft:gilded_longsword` … `jugcraft:leviathans_hook`, the bosses' `jugcraft:vesper_scythe` and `jugcraft:needle_rapier`, then `jugcraft:hades_scythe`, `jugcraft:sentinel_longsword`, `jugcraft:frost_knight_greatsword`, `jugcraft:wight_king_zweihander` and `jugcraft:reaper_scythe`, as in `tools/arms_variants.py`;
  - the four patterns: `jugcraft:gilders_pattern`, `ironclad_pattern`, `bonecarvers_pattern`, `runecarvers_pattern`;
  - the armor sets' shield: `jugcraft:sentinel_shield`.
- **Disabling the `machines` feature** removes the recipes, not the items.

## Dependencies and assets
- **No new dependencies.**
- **The art:** original, drawn by `tools/arms_variants_art.py` on the restyle's toolkit (`tools/arms_pixel.py`, which now also marks glowing materials' boxes with `light_emission`).
- **Generated by:** `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py` (`tools/arms_variants.py`).

## Verification
- **`python3 tools/check_mod_data.py`:** PASS (local). Its new `check_arms_variants` holds `weapons/ArmVariants.java` and `tools/arms_variants.py` together:
  - the variants in order, with kind, line and boon; every number;
  - each style's smithing recipe, and each boss's loot table (exactly its trophies);
  - the tooltips;
  - the bounds, and the per-second ceiling above.
- **`python3 scripts/check_repository.py`:** PASS (local).
- **Game tests** (`ArmsVIIGameTests`): pass in CI on ca45a035 (the `mod` job, with every other server test).
  - every variant is an arm of its kind, with its line's durability, enchantability, rarity and boon, and its kind's blow in steel;
  - Frost, Venom, Wither, Howl and Mark put their effect on a pig at their length and strength; Ember sets it alight;
  - Drain heals the wielder; Gale throws the foe up and away; Shock arcs to the near pig for its share and not to one beyond reach;
  - Gravebane adds its share on a husk and nothing on a pig; Tide adds its share in water and nothing on land;
  - every style recipe and pattern recipe loads, and each boss's table drops exactly its two trophies over 40 rolls.
- **Client game test** (`ArmsVIIClientGameTests`): passes in CI on ca45a035 (shard 0), and its shots show the variants in frames and on racks, trophies held by day, and the runes, magma and venom glowing at midnight. The Glacier Maul's blow left the pig at Slowness II with 43 ticks left.
  - The first-person shot was blank on ca45a035: an earlier test in the shard leaves the GUI hidden, and hiding it hides the hand. The test now shows the GUI for that shot alone, as ArmsMotionClientGameTests does.
  - On 2564ef92 (shard 0, passed) the shot shows the Runebound Moonblade in hand at night, its runes glowing. The frost blow again left Slowness II with 43 ticks.
  - The second-pass art has not yet been through CI.

  The test covers:
  - every variant and pattern in frames, and the variants on armor-stand racks;
  - trophies held from the front by day, glowing ones at midnight, and one in first person;
  - a Glacier Maul's blow with the real attack key, its frost read back from the server.
- **The Hades Scythe (7 October 2026), run locally:**
  - **`python3 tools/check_mod_data.py`:** PASS. `check_arms_variants` now also holds `ArmVariants.SETS` to `SETS`. It fails a line that is in two groups, a boss's or set's line whose name does not name it, and a set's arm with a recipe, with a `bosses/` table of its own, or named in any loot table. Each of these breaks was tried once and caught.
  - **The art check** (run by `check_mod_data`): PASS with the new model.
  - **`python3 tools/check_icon_maps.py`:** PASS, with no warnings on the new map. The Hades line's materials are checked strictly.
  - **`python3 scripts/check_repository.py`:** PASS.
  - **The generators:** a second run of `generate_material_data.py` and of `generate_textures.py` changes nothing. Every other arm's files are byte-identical; only the lang file, the swords tag and the handbook gained entries.
- **The Hades Scythe's game tests, changed and not yet run in CI:**
  - `ArmsVIIGameTests`: every set has an arm; the Hades Scythe's Wither takes on a pig; a set's arm has no recipe and its set no `bosses/` table; the trophy-table test passes over the set line.
  - `TraitDetailsGameTests`: the Hades Scythe's expanded tooltip ends with its set's line, a name only.
  - `ArmsVIIClientGameTests`: the racks hold every variant (a third rack for the 33rd; before, they stopped at 32), and the Hades Scythe is held from the front by day.
- **The Sentinel's and the Frost Knight's arms (8 October 2026), run locally:** the generators, `check_mod_data.py` (with `check_arms_variants` holding the two new set lines and arms), `check_icon_maps.py` (both maps, the two lines' materials strictly) and `check_repository.py`; results in [armor-designs-8-october.md](armor-designs-8-october.md#verification). `ArmsVIIClientGameTests` also holds both from the front by day, and its frames wall puts the four patterns on the bottom row, since the variants' fifth row now reaches past the third frame. Not run in CI yet.
- **The Sentinel's shield (8 October 2026), run locally:** the generators and `check_mod_data.py` (with the set shield checks above); the kit's four shields' textures and models regenerate unchanged; the shield's model rendered from its elements (front, three-quarters, back) beside the owner's front view. Its game tests are new and not run in CI yet.
- **Not run:**
  - play;
  - two players;
  - how the boons feel against real mobs.

## World and event applicability
- **Loot rarity and abilities:** trophies and the armor set's arm are epic, styles uncommon. Abilities are bounded, as above.
- **Boss containment:** the bosses themselves are not built. [branches/BOSSES.md](../branches/BOSSES.md) lists the rules each must meet (arena, readable attacks, recovery, scaling, no griefing, no farming loop).
- **No seasonal content:** the Pumpkin King idea there would have to keep its drops after the season.

## Rollout and open questions
- **Each design is a first pass for the owner to judge.** They are easy to change in `tools/arms_variants_art.py`.
- **Which boss first, and with what arena?** See [branches/BOSSES.md](../branches/BOSSES.md).
- **More styles or kinds per style** can be added to the tables; each style's pattern is shared by its arms.
