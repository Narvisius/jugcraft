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
32 named arms, each a variant of an existing kind with its own look and a perk or boon. All are in the creative Combat tab. Every one fights as its kind does: the same swing, reach, trait, two-handed blow, weapon art and motion.

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

**Boss trophies: 16 arms, two for each of eight bosses still to be made.** They have no recipe. Each boss's loot table is ready to drop one of its two (see [branches/BOSSES.md](../branches/BOSSES.md)). They last twice as long as steel, carry epic rarity, and have a boon:

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

**Looks:**
- Each arm is drawn with the restyle's toolkit: a pixel-art icon on the diagonal and a 3D model in the hand.
- Glowing parts are lit at full brightness in the hand, so they show in the dark: runes, magma, venom, soul fire, charged coils and lightning.
- Tooltips name the kind's trait or art, the boon (in aqua) and the line (in purple: the style's perk, or "A trophy of …").
- **The Runebound arms are smooth 3D models in the hand** (see [Runebound meshes](#runebound-meshes) below); the other 28 keep the restyle's pixel look.
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
  - the 32 variants: `jugcraft:gilded_longsword` … `jugcraft:leviathans_hook`, as in `tools/arms_variants.py`;
  - the four patterns: `jugcraft:gilders_pattern`, `ironclad_pattern`, `bonecarvers_pattern`, `runecarvers_pattern`.
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
- **Not run:**
  - play;
  - two players;
  - how the boons feel against real mobs.

## World and event applicability
- **Loot rarity and abilities:** trophies are epic and styles uncommon. Abilities are bounded, as above.
- **Boss containment:** the bosses themselves are not built. [branches/BOSSES.md](../branches/BOSSES.md) lists the rules each must meet (arena, readable attacks, recovery, scaling, no griefing, no farming loop).
- **No seasonal content:** the Pumpkin King idea there would have to keep its drops after the season.

## Rollout and open questions
- **Each design is a first pass for the owner to judge.** They are easy to change in `tools/arms_variants_art.py`.
- **Which boss first, and with what arena?** See [branches/BOSSES.md](../branches/BOSSES.md).
- **More styles or kinds per style** can be added to the tables; each style's pattern is shared by its arms.
