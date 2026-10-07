# Changelog

Every change to Jugcraft, newest first, grouped by pull request. For what each thing does in game, see [docs/TECH_TREE.md](docs/TECH_TREE.md). For a full inventory with APIs and file locations, see [docs/WHAT_EXISTS.md](docs/WHAT_EXISTS.md).

**Testing so far:** everything compiles in CI, and the in-game tests (from #14 on) pass on a headless server. Nothing has been play-tested in a client or with two players on a dedicated server yet.

## Unreleased

No numbered release yet. Entries on feature branches remain proposed until their pull requests merge.

### World Designer
- An offline browser map editor sculpts terrain, paints installed biomes, places spawn and one Jugcraft walled city, and pins native village/structure start chunks.
- `/jugcraft design export` supplies the editor and live mod catalog. Its exported datapack adds the **Jugcraft Designed** preset for new worlds; existing generated chunks are not edited.
- Reuses native generation and Jugcraft's town systems. Read the [guide](docs/WORLD_DESIGNER.md) and [actual test evidence and limits](docs/features/world-designer.md).

### Unmerged: Thallite, slice 2: gear
- **Thallite tools and armor:** a sword, pickaxe, axe, shovel and hoe, and a helmet, chestplate, leggings and boots, crafted from thallite ingots like iron's and repaired with them. They mine and protect as iron does, last a little less (200 uses a tool) and enchant best of all (18).
- **Regrowth:** thallite gear you wear or hold gets back one use every 5 seconds while you stand on living soil (grass, dirt, podzol, moss, mud, rooted dirt or farmland), up to 75% of full, with a faint green sparkle. It never brings back a broken piece.
- **Earthbound armor:** an Earthbinding Template (rooted dirt, four thallite nuggets and four gold nuggets), a thallite piece and a gold ingot at a smithing table bind the piece into its gold-trimmed Earthbound twin, for good, keeping its enchantments, wear and name. Earthbound pieces are **Rooted**: on natural ground (soil, stone, sand or gravel) each takes 7.5% off knockback, 30% for a full set, and with two or more worn Regrowth works on stone, sand and gravel too.
- Both traits are named in the tooltip and described while Shift is held. Two handbook pages. Arms, the arrow, horse armor and magic come in later slices. Record: [thallite.md](docs/features/thallite.md).

### Unmerged: Reforged White Diamond Armor
- **Reforged White Diamond Armor, the second of the owner's new armor tiers:** an icy white and pale cyan set worn as a 3D model in the owner's own design: a big V crest and rising wing bars over a charcoal face plate, wide winged pauldrons over a lavender-edged V on the chest, a long skirt of tassets in an A over light and lavender stripes, and diamond plates on the toes.
- **Beside Bloodthorn, in other strengths:** defense 4, 8, 7 and 3 (helmet to boots), toughness 3.0, knockback resistance 0.1, durability 495, 720, 675 and 585, enchantability 20. Not fire resistant, and repaired with diamonds.
- **No recipe or drop yet:** creative tab only, until the owner decides. Record: [reforged-white-diamond-armor.md](docs/features/reforged-white-diamond-armor.md).

### Unmerged: Bloodthorn Armor
- **Bloodthorn Armor, the first of the owner's new armor tiers:** a crimson plate set worn as a 3D model in the owner's own design, with a fan of thorn-like spikes behind the helm, layered pauldrons and diamond plates on the forearms and knees.
- **A step above netherite:** defense 3, 9, 7 and 3 (helmet to boots), toughness 3.5, knockback resistance 0.15, durability 440, 640, 600 and 520, enchantability 15. Fire resistant, and repaired with netherite ingots.
- **No recipe or drop yet:** the owner will decide later whether bosses drop it or it is crafted, so for now it is in the creative tab only. Record: [bloodthorn-armor.md](docs/features/bloodthorn-armor.md).

### Unmerged: Knight armor for bronze and steel
- **Bronze and steel armor wear the owner's own knight design,** as real 3D models much bigger than vanilla armor, with many parts sticking out:
  - a helm wider than the head, with a big tilted crest plate in chevrons, horn and cheek fins, a nasal bar, eye slits and a gold collar;
  - a forward-angled chevron plate over the chest, layered pauldrons, and vambraces with flared cuffs;
  - a leather belt over a dark under-layer, and a skirt of plate bands to the ground, hung from the legs so it moves with them.
- **Steel** is the design as drawn, in light steel grey with mid and dark greys in hammered strips. **Bronze** is its steam-age make: warm copper-bronze with brass trim, rows of brass rivets, a brass knob on the crest and a brass belt buckle.
- **New 16×16 inventory icons** for all eight pieces, drawn to match, and the handbook's Bronze and Steel Gear page describes the new look.
- **Any armor piece can now have a 3D model:** boxes of any size, at any angle, on any body part, drawn on players, mobs and armor stands, with vanilla's glint when enchanted. The exosuit's 3D parts use the same layer and look as before.
- **Nothing else changes:** same items, IDs, stats and recipes, so armor you already have just looks new. Trims stay on the item but do not show on the 3D models, and babies wear none.
- The old steampunk and kaiserpunk looks are no longer worn by bronze and steel armor; Steampunk Armor and Kaiser Armor (below) keep them. Record: [knight-armor.md](docs/features/knight-armor.md).

### Framework foundation and Jugcraft Complete
- One pinned dependency manifest makes animation, spellcasting, UI, texture, and creature frameworks available to contributors and AI agents; see [FRAMEWORKS.md](docs/FRAMEWORKS.md).
- The Build workflow produces an importable Modrinth `.mrpack` with the original Jugcraft JAR and hashed upstream library downloads, plus standalone release dependency metadata. No startup downloader or third-party JARs are added to Jugcraft.
- Optional Jade support displays shared machines' energy and processing progress from server data. Fabric Loader is raised to 0.19.5 to satisfy the selected actual artifacts; Minecraft stays 26.3.
- Actual verification and unperformed playtests are recorded in [framework-foundation.md](docs/features/framework-foundation.md). Publication on Modrinth remains a maintainer release step.

### Unmerged: The fall fair in Minecraft's own look
- The owner found that nothing in the fall fair looked like Minecraft (its eyes, mouths, brass and bulbs). Every fair texture is now drawn at vanilla's density, 16 texels to a block, and scaled up to its file's size, so models and UVs are unchanged.
- **High Striker:** painted planks, a brass rail and bell lit like a gold block, flat glass lamps, a plain scale with ticks and a pixel-star sign. **Ring Toss:** a plank crate banded in red and white, bottles lit like vanilla glass and a striped ring.
- **The plushes** (pumpkin, jumbo pumpkin, ghost, bat, black cat, squirrel and werewolf, and their acorn, bow, leaf, rosette and stem) are wool-like felt with square pixel faces.
- **The piñatas:** paper fringe in tiers, cut along each tier's foot, torn to newsprint; the pumpkin's carved face, the bat's eyes and fangs, scalloped wings, the rope and the blindfold.
- **The balloons:** the basket's wicker, leather, planks, steel, brass, coils, tank, gauges and rope; the Harvest Stripes and Harvest Moon envelopes in flat gores with pixel maple leaves, a pixel moon, a witch and bats; the Jack-o'-Lantern's carved face; the flame; the mooring post.
- **Items:** the striker, mallet, ring, piñatas, stick, blindfold, three balloons, burner and pibal are 16-by-16 pixel icons.
- The Ferris wheel is left to the art-fix work (#219). Before and after: [fall_fair_minecraft.jpg](docs/images/fall_fair_minecraft.jpg).

### Unmerged: Clean textures, set 15: the Witching Season's last noisy props
- The dark wood of the broom rack, curiosity cabinet, iron-bound coffin, ribcage bookcase, haunted dining chair, silk spool stack and grandfather clock is long-grain planks, as vanilla planks, in place of blotches.
- Flat, without speckle: the coffin's and chair's velvet, the brain vat console's brass, the lightning harness's copper, the dustpan's tin, the witch's lantern's violet glass and both pillar candles' wax.
- The effigy ashes and the hearth ash item are soft clumped ash with a few charred flecks and embers set in place. The egg sacs' web mat is drawn as threads.
- The workshop's tinted brew, fume and wax textures (the cauldron's brew, its fumes and the candelabra's candles) are smooth, without per-pixel grain.
- The props are drawn in the clean style (`flora_art.QUIET`), switched on for just these builders. The Beating Heart Jar keeps its original look, which the owner loves. The 25 props the owner's art review redid (#219), the faces, moons, webs, pumpkins, plushes, garlands and glows are unchanged. IDs and models are unchanged. Before and after: [clean_textures_set15.jpg](docs/images/clean_textures_set15.jpg).

### Unmerged: Clean textures, set 14: the town and the Pixel Hollows
- The 18 townsfolk wear clean cloth in the manner of the vanilla skins: lit in its upper half, a shade darker below and at the hem, with no speckle. Their hair is flat with neat strands. Each keeps the face, hair style and outfit it had. The raiders, which share the painter, are unchanged.
- The Retro Trader keeps his buffalo-check flannel. His hair and beard are drawn in strands, not a checkerboard, his trousers are a plain navy plaid and his sneakers are plain black.
- Circuitstone is dark slate in thin streaks, as vanilla deepslate, under the same copper traces. The bricks are laid as deepslate bricks and polished circuitstone is smooth.
- The Jug Teller (the town's ATM) is flat cast iron and brass, lit along the top and left.
- The Pixel Hollows map is plain parchment with its folds.
- The arcade cabinet parts, crystals, pixel lamp, shard and map marker were already flat and are unchanged. So are the guide screenshots, the tower outlines and the machine screens, which are pictures rather than textures. IDs and models are unchanged. Before and after: [clean_textures_set14.jpg](docs/images/clean_textures_set14.jpg).

### Unmerged: Clean textures, set 13: tower and drones (the noisy few)
- The control screen and holo table, loose and formed, are flat navy glass behind their grid lines and glows, with plain steel sides lit along the top.
- The cargo packager's white panels are flat with a short sheen.
- Reinforced concrete keeps its soft mottling, seam and tie holes, with a few spaced pores in place of a per-pixel jitter.
- The survey stake is straight-grained wood under its red band.
- Silicon carbide armour is left to the art fixes PR (#219), which redraws the tower's armour plates. The hangar and landing pads, drones and other tower blocks were already clean. IDs and models are unchanged.

### Unmerged: Clean textures, set 12: dieselpunk war machines (the noisy few)
- Kaiserworks marble (plain, fluted and the column end) is calm cream in soft clumps with one smooth grey vein, like vanilla calcite. The black station tiles are flat with a lit edge, and their cream tiles are clumped marble.
- The glass tank's fluid is near-white in soft clumps with even diagonal ripples. Construction foam is clumped with evenly spaced pores.
- The artillery deck and wicker are left to the art fixes PR (#219), which reworks their painters, along with the 22 textures it fixes. The other war-machine tiles were already clean. IDs and models are unchanged.

### Unmerged: Clean textures, set 11: weapons and armour (the powders)
- Four noisy powders are clean 16×16 heaps in the manner of the set 9 dusts, following the item icon rules (#201): ammonium perchlorate, iodine, silver iodide and cement mix.
- Left to other work:
  - the arms icons, which #201 redraws;
  - the four textures #219 fixes;
  - the bronze and steel armour, which gets the owner's new 3D knight look in #215's follow-up.
- The exosuits, the rocketry items and the construction items are already clean pixel art and are unchanged. IDs and models are unchanged.

### Unmerged: Clean textures, set 10: machines
- Every bronze-framed machine front loses its speckle.
  - The steel plate is one tone with a few soft clumps, recessed under the frame's lit top and left edge.
  - Windows are recessed, with their contents in soft clumps instead of random pixels.
  - A running machine's glow (fire, arc light, a crystal's shine) rises in smooth bands to its brightest at the bottom, like a furnace.
  - The crusher's jaws are two neat rows of teeth, the sieve is a crossed mesh, the circuit assembler shows gold traces, and the boilers are round and lit at the top left.
- The machine casings, tanks, pump sides, plinths, the geothermal tank and stack, the crucible, the solar panel, the belts and the conveyor are cleaned the same way. Bricks take one tone a brick, lit along the top.
- The five refinery machines whose fronts were committed as pictures in the old style (catalytic reformer, chemical mixer, crystal grower, oil sand extractor, vacuum distillation unit) are now drawn by the same code.
- The leftover materials match set 9:
  - salt, phosphate, lepidolite, monazite, sawdust, lithium carbonate, rare earth oxide and thermite are clean heaps;
  - bitumen, silicon, coke, borax and ferroboron are lumpy chunks;
  - bauxite, oil sand and tincal are evenly speckled rock.
- The owner's library (#207) was used as a style reference only: many of its machine files match other mods' textures, so none is copied. IDs and models are unchanged.

### Unmerged: Clean textures, set 9: metal parts, petrochemistry and deposits
- The ores, storage blocks, raw ores, ingots and nuggets are left to the material sets (#218), as the owner chose on 6 October 2026. This set cleans the rest of the materials.
- Metal parts are drawn in `tools/material_style.py`:
  - dusts are clean heaps;
  - plates have an outline and a sheen;
  - gears have eight square teeth;
  - wire is wound on a spool;
  - washed ore is a clean chunk with drops of water.
- Bronze blend and sulfur dust are clean heaps too.
- Petrochemistry:
  - The fluid buckets share a new iron pail with the fluid in its mouth.
  - Flowing fluids run in soft streaks, and still fluids are mostly one tone with lighter swells.
  - Guncotton, PVC resin, alumina, titanium sponge, asphalt, asphalt binder, fertilizer, the cracking catalyst and the turbocharger lose their random speckle.
- The four resource deposits are packed rubble of stone and ore lumps, in the manner of gravel. IDs and models are unchanged.

### Unmerged: Clean textures, set 8: crops (outside the tree work)
- Only the crop textures that open PRs #195 (wood repaint) and #206 (new trees) don't touch are changed. Trees, logs, wood, leaves, planks and saplings are left to those PRs.
  - The squash and gourd skins vary in soft clumps with even stripes; the warty gourd keeps its warts.
  - The cooking-pot soup is a clumped surface with a few glints.
  - The turnip lantern's lid stalks are lit at the top.
  - The sauerkraut's shreds run in even lines.
- `gourd_side` now paints cleanly by default. IDs and models are unchanged.

### Unmerged: Clean textures, set 7: the fall fair
- The fall fair's high-resolution textures (the midway games and plush prizes, the piñatas and the hot-air balloons) lose their soft mottling and fine noise. They are painted in the clean, cartoon style of the werewolves:
  - Every tone sits in a few flat bands. Brass, glass and glowing bulbs shade in clean steps, and felt, enamel, canvas and wood are even.
  - Piñata fringes hang at even lengths, and the torn holes are round.
  - The squirrel plush's tail has a cream band of points along its tip.
- `tools/fur_paint.py` gains `clean_painter` and `clean_ramp`, used only by the fair's modules. Faces, stitches, stripes and painted designs keep their drawn detail. IDs and models are unchanged.
- The Ferris wheel is left to the art fixes PR (#219), which redraws it so it no longer flashes; its textures are main's here.

### Unmerged: Clean textures, set 6: fall food and kitchen
- The pies and hearth oven, the Harvest Feast Table, the candy kitchen, the cider mill, the preserves pantry and autumn foraging lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`.
  - The oven is vanilla-style bricks, its stone is stone and its embers a heap of glowing coals.
  - Pie tops are a woven lattice, each strip lit along one edge, over a clumped filling, and the cut side has clean crust layers.
  - Wood is planks: the feast table, cider press, barrel staves and pantry shelves.
  - Apple leaves are clumps with gaps, lit along their tops, as vanilla leaves. Graniteware is flecked evenly, as vanilla diorite.
  - Raw pies, slices, dough, rock candy and burnt sugar are lit at their upper left instead of speckled.
- IDs and models are unchanged.

### Unmerged: Clean textures, set 5: the Halloween harvest and festivities
- The Halloween harvest and festivities lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`. That covers the giant pumpkins and the heirloom pumpkins, the Harvest Scale, scarecrow, corn shocks and ornamental corn, gourd birdhouse and mums, the regatta and trick-or-treat costumes, the Judging Stand, festivity candles and ghosts, the Headless Horseman, wisps and trebuchet, the corn maze and ghost hunting, and the knitting, chandlery, firework launcher, spirit board, theremin, ofrenda, mooncakes and broomstick.
  - Pumpkin skins vary in soft clumps; the giant pumpkin stays mirror-symmetric.
  - Wood is planks, and straw and bark are streaks. The gravestone is stone and the trebuchet's counterweight cobblestone.
  - Ornamental corn ears are one colour with an accent kernel. The mums are round blooms over a leafy mound, and marigold petals lie in neat clumps.
  - Sugar skulls, pan de muerto, popcorn balls, guts and fizz rocks are drawn as shapes instead of scattered pixels.
- `gourd_side` in `tools/festival_textures.py` gains an opt-in `clean` mode, used here only, so the festival gourds of a later set are unchanged until that set. Faces, letters and cut-outs keep their drawn detail. IDs and models are unchanged.

### Unmerged: Clean textures, set 4: Halloween decorations, batches 8 to 14
- The next seven batches lose their per-pixel speckle and are painted in the manner of the vanilla blocks with `tools/block_style.py`: the mad scientist's lab, the yard inflatables, porch witch, poseable skeleton, grasping hands, spooky sign and dead tree, the archway and lanterns, the brazier and the shadow-puppet lamp's wood, the party games (jump-scare trap, costume runway, bowling, scoreboard, candy cache, ghost bell, fortune teller), the bonfire and hayride, the treats, and the costumes and costume trunk.
  - Surfaces are one ground tone with small clumps of its neighbours. Wood is planks.
  - Bark, hair, straw and brushed steel are streaks.
  - The archway is mossy stone bricks, the bonfire ring cobblestone, and the brazier's coals and the trunk's clothes are heaps.
  - Sweets get evenly set sugar crystals and raisins.
  - The costumes' black cloth, fur and bat wings are clumped instead of speckled.
- Faces, letters, cards and cut-outs keep their drawn detail. IDs and models are unchanged.
- The Shadow Puppet Lamp's brass, candle and paper panels, the floating witch hat's candle (it shares the lamp's candle) and the Specimen Jar's eye are left to the art fixes PR (#219), which redraws them.

### Unmerged: Clean textures, set 3: Halloween decorations, batches 1 to 7
- The first seven batches of Halloween decorations lose their per-pixel speckle and are painted in the manner of the vanilla blocks, with `bs.fill` and the other painters in `tools/block_style.py`. This covers the string lights, candy bowl, coffin, haunted portraits, fog machine, luminaria, floating candles, sconce, bunting, cauldron and hex brews, apothecary shelf, crystal ball, grimoire, broom, bobbing tub, pumpkin crate, hay bale seat, autumn wreath, leaf piles, rocking chair, lurking eyes, silhouette windows, music box, giant spider, chandelier, pipe organ, suit of armour, dust sheet, spirit mirror, curtains and creepy doll.
  - Each surface is one ground tone with small clumps of its neighbours.
  - Wood is planks; straw, husk and bristles are streaks.
  - The candy bowl is a heap of round foil sweets with candy corn on top, and the ornamental corn is rows of plump kernels.
  - Leaves are shaded along their lower edges; lace is eyelets; the plume is feathers in bands.
- Faces, cut-outs and portraits keep their drawn detail. Every pattern tiles, and IDs and models are unchanged.

### Unmerged: Clean textures, set 2: the graveyard
- The memorial stones (marble, slate, granite and sandstone in all four weathering stages, their carved, rough and knotwork faces), the chippings and flower beds, bronze and iron, oak and roof slates, the crypt set, the grave mound, the Mourning Angel, the Pop-Up Skeleton (its skull now cute, with no nose hole) and the graveyard flora, painted in the manner of the vanilla blocks so they sit beside them: stone as stone (granite speckled like vanilla granite, rough granite like cobblestone), oak as planks, chippings as gravel, beds as dirt, and moss and lichen in clumps as on mossy cobblestone; no salt-and-pepper speckle and no flat fills. Every pattern tiles, so blocks still join up. IDs and models are unchanged.

### Unmerged: Clean textures, set 1: the fall creatures
- The owner asked for every texture to lose its noise and look cleaner, flatter and cuter, set by set (15 sets, after the reference pictures). Set 1 is the fall creatures: the **werewolves**, their rugs and pelts (big flat locks of fur in neat rows instead of fine hairs), the **turkeys** and the roast turkey dinner, **squirrels** and acorns, the **crow**, the **Pumpkling**, the **Hay Golem** and the **Bat House**: flat tones and regular patterns, no random speckle. The Bat House's planks and guano, the turkey's bone and skin and the silver platter are painted in the manner of the vanilla blocks (`tools/block_style.py`). IDs and models are unchanged.

### Unmerged: Arms icons in the owner's 16×16 style
- **Every arm's inventory icon is redrawn at 16×16** in the owner's manner: on the diagonal, a one-pixel outline in each part's own dark tone, flat tones lit from the top left, chunky parts that read at a glance. Each kind is one map of letters in `tools/arms_icons/` that the owner can edit.
- **The big arms show the whole weapon**: the longsword, greatsword, zweihander, executioner, battleblade, moonblade, sabre, nodachi and halberd fit the full diagonal whole, every part present, instead of cropped.
- **The 3D models in the hand stay**; bronze becomes the owner's tan bronze and steel a dark blue-grey, in the icons and models alike, which also re-tints the bows, arbalests, shields and some variants. Record: [arms-icons-16.md](docs/features/arms-icons-16.md).
- **[ITEM_ICONS.md](docs/ITEM_ICONS.md): rules for every item icon**, measured from these maps: 16×16 at vanilla's scale, the whole item with every part present and never cropped, a one-pixel outline in each part's own dark, light from the top left, flat tones, each metal its own palette, vanilla kinds in vanilla's form drawn fresh. `tools/check_icon_maps.py` checks the maps, the materials they declare (`tools/icon_materials.py`) and the item icons' sizes, and runs with its self-test in CI through `tools/check_mod_data.py`.
- **New item icons are 16×16 only**: ART_DIRECTION.md and CLAUDE.md now say so, `tools/hd_art.py` draws no new icons, and the 122 larger icons are frozen in `tools/legacy_item_icons.txt` until each is redrawn; CI fails any other item icon larger than 16×16.
- **The 17 war machines' icons are redrawn at 16×16** after merging main, whose 32×32 icons failed that rule: the five tower guns, the Siege Mortar, Self-Propelled Howitzer, Flak Gun, Range Finder, the four shells, the Landship, the Diesel Walker, the Zeppelin and the Observation Balloon. Each is a map in `tools/item_icons/` (drawn by `tools/item_icons.py`, replacing `tools/gun_icons.py`) and keeps its old cue; the legacy list is unchanged.

### Rules: fan homages allowed
- Jugcraft may now include fan homages: creatures, characters and things inspired by films, shows, cartoons and games, with changed names and the owner's approval. Their textures, models, sounds and code must still be made for Jugcraft or licensed; nothing ripped or copied from the source. See [LICENSE_POLICY.md](LICENSE_POLICY.md#fan-homages).

### Unmerged: New trees, batch 1
- **Ten new trees, from the tree roster** ([TREES.md](docs/branches/TREES.md)):
  - **Firs:** stunted firs and thin bog firs, narrow subalpine fir spires and low fir bushes.
  - **The tamarack:** a thin larch, gold in autumn and bare in winter, over the Muskeg, Bog and Fen.
  - **Grey dead snags**, straight and bent, in the dead and burnt biomes, and on the Cinder Barrens' and Wasteland's coarse dirt.
  - **Willow bushes**, and **young aspens** standing in the Hallowed Bog's water.
  - **The swamp cedar**, in the Wetland and Ghost Forest, and the moss-hung **bigleaf maple** of the Temperate Rainforest and Redwood Forest.
- **Cedar, a new wood** from the owner's painted western red cedar: log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate, sage-green cedar leaves and a cedar sapling that grows a cedar, drawn as the other woods are. Its wood is darkened a little so it sits apart from vanilla spruce and the cypress.
- **29 biomes change their trees.** Besides the new trees, firs replace vanilla spruces in the Maple Woods, Redwood Forest and Lake District and join the Seasonal Forest; larches join the Coniferous Forest, Shield and Lake District; aspens replace the Shield's oaks and the Snowpetal Grove's birches and join the Snowy Forest; fallen dead and larch logs, willows, maples, azaleas and a rare great oak join where the roster says; and the Wasteland gets a tree try in every chunk.
- **A wood's recipes follow any switch that grows it:** `jugcraft:feature_enabled` takes an `"or"` list, so the larch's recipes stay on with either Alpine Spawn or the biomes, and the chestnut's with either agriculture or the biomes.
- New chunks only; nothing saved is renamed or removed. Record: [trees-batch-1.md](docs/features/trees-batch-1.md). Not yet played.

### Unmerged: Wood repaint
- **Every wood the mod adds is redrawn** in its colour from the owner's 24 painted woods, matched to the closest painting: bark, log ends, stripped logs and planks, so its stairs, slabs, fences and gates follow too.
- **Drawn as vanilla draws wood:**
  - **Bark:** long vertical furrows, with no rings across the trunk.
  - **Log ends:** square growth rings.
  - **Stripped wood:** straight grain.
  - **Planks:** four lit boards with staggered joints.
- **Every tree's leaves**, in every season's look, are redrawn in vanilla's fine speckle; needles, blossom, fronds and bare twigs each in their own way.
- **The owner's second set** of eight painted woods: the jacaranda takes its mauve wood, and the other seven wait in a bank for new trees (cedar, plane, walnut, wenge, elm, hickory and yew).
- **[NATURAL_TEXTURES.md](docs/NATURAL_TEXTURES.md):** how these woods were drawn, as the rule for every future wood and natural texture.
- Original textures drawn by code (`tools/wood_style.py`); nothing of vanilla's is recoloured. Record: [wood-repaint.md](docs/features/wood-repaint.md).

### Unmerged: Thallite, slice 1
- **Thallite, the Earth school's green metal** (the owner's chartreuse set): Thallite Ore and Deepslate Thallite Ore in every Overworld biome (veins of 7, 4 a chunk, Y −32 to 48), and rich pockets in Lush Caves and the Glowcap Grotto (6 more veins of 9 a chunk) that show in the cave walls. A stone pickaxe mines it; each ore drops one raw thallite, with Fortune as on vanilla's ores.
- **Smelt it** in a furnace or blast furnace (0.7 xp) into **Thallite Ingots**, which say "Green as a new shoot." Nuggets, ingots and blocks, and raw thallite and raw blocks, go 9 to 1 both ways. No alloy, on purpose.
- **Through the machines:** the Crusher and Pulverizer give 2 a block (the Pulverizer a 10% iron dust too), the Ore Washer 3 and acid leaching 4. The Ore Drill and Prospector find it. **Thallite plates** come from the Metal Press, or by hand at 2 ingots a plate.
- Its own switch, `thallite`: off stops its worldgen and recipes and keeps every block and item. Worldgen reaches new chunks only. A metal's worldgen entry may now name its biomes (as rocks do), placed through a `jugcraft:has_ore/<feature>` biome tag. Gear, arms, the arrow, horse armor and magic come in later slices. Record: [thallite.md](docs/features/thallite.md).

### Unmerged: Material sets: tools, ingots and ores redrawn
- **Every metal's ingot and nugget in vanilla's form:** the ingot is the owner's own, recoloured for each metal and never redrawn, and the nugget a shard drawn fresh from memory; one shared ingot and nugget for all fourteen metals, each in its own five-tone ramp, meant to sit beside vanilla's in an inventory as one set. A client test sets them beside vanilla's iron, gold and copper for the owner to judge. Bronze and steel take the owner's chosen palettes; the other twelve stand on the same ladder, each kept clear of the others and of vanilla's iron, gold and copper.
- **Storage blocks** in the owner's four-panel inlay; **raw ores and raw blocks** as lumpy chunks and packed lumps in each ore's own tones.
- **Ores on vanilla's own stone and deepslate**: each ore block is now vanilla's rock, referenced by name and never copied, with our ore's chunky blobs as a cut-out layer on top, so it matches the rock round it. Three blob layouts take turns.
- **Bronze and steel tools** (sword, pickaxe, axe, shovel, hoe, paxel) redrawn in the same style. Art only: IDs, recipes, drops and worldgen are unchanged and worlds need no migration; the maps are `tools/material_icons/`. Record: [material-sets.md](docs/features/material-sets.md).

### Unmerged: Steampunk Armor and Kaiser Armor
- **The stylized armor looks return as two sets of their own:**
  - **Steampunk Armor:** an aviator cap with teal goggles, a pressure gauge and a copper boiler on the back.
  - **Kaiser Armor:** a black spiked helmet, a field-grey tunic over a steel cuirass, and jackboots: the parade dress of the Winged Cog.
  - Each protects exactly as bronze or steel armor does: the same defense, toughness, durability, enchantability and repair.
- **Made at a smithing table:**
  - a **Steampunk Pattern**, a bronze piece and a copper ingot make the Steampunk piece;
  - a **Kaiser Pattern** (made with an Imperial Crest), a steel piece and a gold ingot make the Kaiser piece;
  - the same pattern and an ingot of the metal turn it back.
  - Enchantments, wear, name, trims and plating carry over. Each pattern craft makes 4, one for each piece of a set.
- **Bronze and steel armor keep their current look in this PR,** so for now each looks exactly like its Steampunk or Kaiser twin. Their IDs, stats and recipes are unchanged, and nothing needs converting. Their new 3D look comes in a following PR.
- The **Goggles On** and **On Parade** advancements (a Kaiser piece also counts for Suited Up), and two handbook pages. Record: [steampunk-and-kaiser-armor.md](docs/features/steampunk-and-kaiser-armor.md).

### Unmerged: Arms VIII, batch 59
- **Thrown arms** in bronze and steel, each an arm in the hand that you can also throw: hold use to wind it back and let go, as you throw a trident. What it strikes takes its damage (up to 8, the trident's), more with Sharpness and the like, and it comes down where it struck as itself, enchantments and wear kept, to be picked up again.
  - **Javelin:** flies far and straight.
  - **Francisca:** a throwing axe that tumbles end over end and knocks a raised shield down for 3 seconds; chops wood in the hand.
  - **Chakram:** a bladed ring that flies flat for 12 blocks, cutting every foe on its way out and back, and returns to your hand.
  - **Harpoon:** keeps its speed underwater and hauls what it strikes towards you, out of the saddle.
- Each flies as its own 3D model, with its own moves in the hand and a wind-up pose. Handbook page, the **Let Fly** advancement, six game tests and a client test with real throws. Record: [arms-viii.md](docs/features/arms-viii.md).

### Unmerged: Trait details on Shift
- **Shorter tooltips, with details on Shift:** gear with traits now lists each trait by name, then "Hold Shift for details". Holding Shift expands the tooltip with a brief description under each name. See [trait-details.md](docs/features/trait-details.md).
- **The arms use it first:** every kind's trait (Backstab, Parry, Execute and the rest), Two-Handed, the weapon arts, and the Arms VII boons, lines and trophies. The longbows, arbalests and shields use it too. Thallite's gear is next.

### Bunker and trench interiors, batch 59
- **Trench Periscope:** two blocks tall, its mirror head looking over the parapet. A comparator reads how many hostile mobs it sees (within 64 blocks, 45 degrees either side, in clear view, every second). Look through it to mark the nearest one as a Range Finder would, for your guns and fire control; sneak to clear the mark.
- **Map Table:** a campaign map on a table. Use it to list the target marks plotted within 256 blocks (who, how far, which way, how long ago); sneak-use it to lay a Fire Control Table within 4 blocks on the next one.
- **Gas Curtain:** a wet blanket across a doorway, two blocks tall, that you walk through. Let down, it keeps chlorine and smoke out of everything behind it; use it to roll it up or let it down.
- **Field Kitchen:** an iron stove that heats a Cooking Pot on top. It burns logs, coal, charcoal and coke, glows and smokes while lit, and only starts a new piece of fuel while a pot stands on it. Hoppers can fuel it.
- **Trench Stew:** beef, a potato and a carrot from the Cooking Pot: 10 food and five seconds of Regeneration.
- **Corrugated Iron** (with slab and stairs), **Timber Shoring**, the caged **Bunker Lamp** (hangs or stands) and the **Bunker Bunk** (sit on the lower bunk; no spawn point).
- New `Spotting.near` lists marks for the map table. Record: [bunker-interiors.md](docs/features/bunker-interiors.md).

### Faster CI: pull requests run only the client tests they need
- The Build workflow's new `choose client tests` job (`tools/select_client_tests.py`) picks the client game test classes that show what a pull request changed. Docs- or data-only changes run none. Build files, mixins and the test harness still run them all. Rules: [TESTING.md](docs/TESTING.md#current-foundation).
- `main` and manual runs still run every class, shared between the three client jobs by rough running time instead of every third class.
- Gradle's downloads are cached between runs.

### No more see-through or flickering models: machines, guns, vehicles, balloons and props (art fixes)
- **Closed from every side:** the grand mortar and the other tower guns, the siege mortar, howitzer and flak gun, the landship, the Diesel Walker, the zeppelin, the observation balloon's envelope and the searchlight no longer show the world through gaps in their barrels, rings, decks and hulls. The exporter drew whole 16-pixel patches of a face away wherever a smaller part sat on part of it; it now removes only what is really covered.
- **No more flashing textures:** collars, muzzle rings, hazard rims, caps, gear teeth, handwheels, the joins between a big machine's blocks and spinning shafts no longer flicker, close up or far away. Every exported model now keeps differently drawn faces at least 0.1 pixel apart (was 0.02, which failed beyond about 30 blocks), including turned parts.
- **Two-sided sheets** (string lights, pennants and garland leaves, floating-candle, Aura Candle, bonfire, witch-fire and Harvest Effigy flames, grapple and zipline lines, the drone depot's doors, the blueprint placement ghost and the quad-drawn decorations) no longer fight themselves. The floating candles' flames read the same way round from behind again.
- **No strips of other textures** on faces that reached past their own (trebuchets, the blueprint table, dead trees, the console desk, weathervanes and others); such faces keep their texture's density, so the drone depot terminal and console desk screens show their screens.
- **Witching Season props closed:** the Coffin Wardrobe, Curiosity Cabinet, oddity jars, Iron-Bound Coffin, sarcophagi, candelabra, Bone Throne, Ribcage Bookcase, Grandfather Clock and the rest no longer show holes where a box left a face out; the Horned Skull Cauldron's hollow is lined walls; the Colossal Skull keeps its open sockets with its front closed beside them; the Farm Stand, Harvest Effigy and tall flowers no longer flicker where their blocks meet.
- **No slits at spinning parts:** the hand crank's axle, the solar tracker's pivot and the heliostat's shaft meet their still parts with no gap.
- A new check, `tools/art_check.py` (run by `check_mod_data.py`), keeps it that way. Rules: [ART_DIRECTION.md](docs/ART_DIRECTION.md#rules-for-everything). Record: [see-through-and-flicker-fixes.md](docs/features/see-through-and-flicker-fixes.md).

### Steel plates, armour plate, hazard plating and concrete redrawn (art fixes)
- The owner called the steel plate blocks and blast-proof concrete horrific and said bastion concrete looks good. These blocks are redrawn the way bastion is: a few flat shades, no random speckle, no rust, and a single split seam where two blocks meet instead of a doubled dark frame round every block.
- **Dieselworks steel set** in one cool blue-grey steel: **Weathered Steel Plate** is one brushed sheet a block with a soft edge, so a wall shows quiet joints rather than a grid of framed tiles; **Riveted Steel Plate** is framed by raised rivets that line up evenly across every joint; the **Ribbed Steel Pillar** is ribbed and shaded like a round column; the **Riveted Band** is a dark strap with a rivet row; the **Steel Grating** is a bar grating whose slots carry on evenly from block to block, with a rail at the top and bottom of each half, so grating slabs no longer end in open prongs; the **Porthole Window** sits in a steel sheet with a steel ring; and the **Steel I-Beam** and the **Amber Cage Lamp**'s base and cap are the same steel, without the old brown frame and rust mark. Slabs and stairs follow, and the set shows as metal on maps. The giant machines, the zeppelin and the vehicles keep their own steel.
- **Steel Armor Plate** (drone tower) is clean 32 px pixel art instead of noise: one thick gunmetal plate a block with chamfered corners that meet round a round bolt boss wherever four plates join, with no dark band from top to bottom. **Hazard Plating**, which the tower lays round armour pads, door bays and deck edges, is now the same plate with a flat band of yellow-and-black stripes, so rim and armour meet as one steel.
- **Blast-Proof Concrete** is heavy cast concrete in two smooth lifts a block, darker and cooler than bastion, with tie holes on an even, staggered lattice. **Concrete** is its lighter, warmer, smooth sibling: one lift a block and two faint form marks, no speckle. Their slabs and stairs follow.
- The rules behind this, taken from the blocks the owner liked and rejected, are in [ART_DIRECTION.md](docs/ART_DIRECTION.md#tiling-building-blocks). Block IDs, recipes and model shapes are unchanged; the models now point at the new textures, so placed blocks just look new. The `jugcraft_dieselworks` screenshot's front row now shows the steel plates' slabs and stairs, and `jugcraft_foam_sprayer` ends with the armour plate and hazard plating with their slabs and stairs.

### The sawmill's blade turns and the sieve is a real vibrating screen (art fixes)
- **Sawmill:** the giant sawmill's blade is now a toothed 24-tooth disc that **turns while the sawmill works** (and stands still when it is idle), its front teeth cutting down into the log. It spins up and runs down smoothly instead of jumping, even on weak power. The checkerboard texture is gone: the blade is plain ground steel with expansion slots and an amber maker's plate you can watch go round. It turns at a steady pace that never looks as if it runs backwards, even at low frame rates. It runs in a slot in the bed under a red hood that leaves the front teeth bare, on an arbor in two pillow-block bearings, belted from the motor, whose pulley turns too. Each bearing face and duct joint is drawn once, whole (no doubled, squashed nut where a block seam cut it). The dust duct and hood no longer stick out of the machine's blocks (no more stretched, flickering textures there), and the amber running lamp and gauge sit on a control box at the front, so the lamp lights while it runs.
- **Sieve:** rebuilt as a vibrating screen. A red screen box on blue coil springs tilts from a feed hopper at the back down to a hazard-striped lip at the front; gravel rides over a grizzly of real steel bars onto a fine woven-wire deck with steel cross bars (the weave, the bolts and the lip's hazard stripes run on evenly, with no seam down the middle), and a vibrator motor on a full-width beam **spins its yellow eccentric weights while the sieve works**. Totes of fines, flint and nuggets stand at the front, a pan under the deck catches the fines, and the control box with the gauge and the running lamp is on the master block.
- The turning parts are drawn by the machines' renderer (`client/MachineRotors`, from `machine_rotor_quads.json`) in both machine styles; the classic pack's sawmill leaves a slot for the blade and its sieve hides the weights. The item icons show the blade. Only machines placed whole show them; a one-block copy from before batch 44 keeps its old look. IDs, blockstates, recipes and behaviour are unchanged. Record: [dieselpunk-giants.md](docs/features/dieselpunk-giants.md#turning-parts-sawmill-and-sieve-5-october-2026).

### The Flying Eyeball, the Specimen Jar's eye, the Shadow Puppet Lamp and the Ferris wheel (art fixes)
- **Flying Eyeball:** no more glowing white square behind its iris. The iris is now a cut-out disc on the eye's own white, which is a warmer ivory, with a small white glint inside the disc. Its wings no longer flicker (each was two faces fighting in one plane) and are repainted clean, in lit and shaded panels between the fingers; the item's wings are two faces 0.1 pixel apart, so they cannot fight either.
- **Specimen Jar:** the eye is rounder and drawn properly on every side as it turns (it used to show a black-topped blue cube most of the time), with a clean white, a few placed veins, a blue iris and a round pupil, its nerve hanging straight down. The jar itself is unchanged.
- **Shadow Puppet Lamp:** no more flashing: each paper panel is one face (it was drawn twice in one plane, fighting its mirror image), and the corners close. Each shadow now lies flat on the walls its light actually reaches, cut to the open wall and to what the flame can see, so it never stands across a wall, hangs over a doorway or falls where no light reaches. A shadow crossing a doorway, a corner or a pillar's edge slides smoothly across it, shown on each side, instead of vanishing or jumping to the next wall in one frame, and it fades out at the end of its range. The paper, silhouettes, shadows, brass, wood and candle are repainted clean at 32 × 32 and 16 × 16.
- **Ferris wheel:** its textures no longer flash as you move. Every part samples its texture at about four texels to a pixel (thin rims and trims used to squeeze a whole 64 × 64 picture), the textures of the wheel and cars are repainted clean in the same red, cream and brass, and no two faces share a plane any more: mitred rim lengths, a sixteen-sided hub, separate planes for the rings, spokes and ties, a closed pivot bar ending inside the rims and one face for each valance. The rims show three bands a pixel each (a warm gold edge, the red and a shaded red); the brass, cream steel and axle are banded along each member (a lit edge, the fill and, where wide enough, a shaded edge), with bevelled plates on squarer faces such as the bearings and the brass cap over the axle. The booth keeps its earlier brass and steel, now as its own textures (`ferris_wheel_booth_brass`, `ferris_wheel_booth_steel`).
- New client screenshots for CI: the eyeballs at night and from behind, the eyeball item held in the hand and in the hotbar, the eye's jar close up, the lamp close up and in a narrow corridor with a doorway (each twice, a few ticks apart), and the stopped Ferris wheel twice from 0.05 block apart. Records: [haunted-house-props.md](docs/features/haunted-house-props.md), [more-halloween.md](docs/features/more-halloween.md), [even-more-fall-additions.md](docs/features/even-more-fall-additions.md). IDs, recipes and behaviour are unchanged.

### Runebound arms as real 3D models (art fixes)
- **The Runebound Nodachi, Moonblade, Staff and War Hammer are now smooth 3D models in the hand**, not stacks of pixel boxes, after the owner's "make like a nicer 3d model ... really be cool and special":
  - **Moonblade:** a crescent of violet moon steel ground to a bright edge, a raised bead of glowing runes following the crescent, a crescent-moon guard with glowing horn tips round a heart crystal in a silver bezel, a spiral cord grip and a faceted crystal pommel.
  - **Nodachi:** a continuous curve whose edge is tempered in a glowing wave with a white line along it, a short panel of glowing runes by the habaki, a gold habaki, an oval tsuba with a ring of light round its rim and a silk-wrapped tsuka with windows of ray skin.
  - **Staff:** dark ironwood with a glowing helix winding up each half, a leather grip between iron collars, and at each end three iron claws holding a floating crystal.
  - **War Hammer:** a flared, chamfered head with a glowing diamond cut into each side, a white diamond raised in it and a stave through both, a moon-gold band, a curved beak, a top spike, langets, an iron-banded haft and a spiral cord grip.
- Curves shade smoothly and edges stay crisp. The details are shapes, not stretched pixels: the Moonblade's glow is a raised bead and the Nodachi's a glowing tempered edge, so each stays a steady line at a distance instead of a dark slot that flickers in and out as the arm turns, and the temper line, sigil and tsuka diamonds stay clean however close they are. The runes, temper and crystals glow at full light in the dark and pulse gently. Every part is closed: nothing to see through, nothing poking through anything else. They are held exactly where the old models were, in either hand, first and third person, and take the enchantment glint.
- **New icons**, rendered from the same models in flat tones as bright as their steel siblings.
- Item ids, recipes and stats are unchanged. Built on Fabric API's model loading and renderer API (already part of fabric-api, so no new dependency): `client/MeshItemModels.java` reads the meshes from `tools/arms_mesh.py`. The old box models stay in the files as a fallback. Record: [arms-vii.md](docs/features/arms-vii.md#runebound-meshes).

### Witching Season props and bigger jars (art fixes)
- **Giant's Beating Heart**: the Beating Heart Jar three times over, 3 × 3 × 3, placed and broken as one: the same jar of red murk, iron lid, label and slowly turning heart on its brass stand, just bigger. A slow redstone clock (40, 50, 60 or 72 beats a minute; use it to change), each beat a two-tick signal of 15 from its first block and a deep heartbeat, stopped by a signal from below; the heart swells with each beat as the jar's does. Glass, a Beating Heart Jar, brass ingots and a block of redstone.
- **Tall Specimen Jar** (the Specimen Jar twice over, 1 × 2 × 1) and **Specimen Tank** (three times over, 3 × 3 × 3): the same jar, glowing fluid, iron fittings and specimen, just bigger, its eye, tentacle, pumpkin or brain bobbing among rising bubbles. Sneak-use to change the specimen; broken, they keep it. Their items are the Specimen Jar's with the specimen in it, a little bigger in a slot. Made from the smaller jar with iron and glass.
- **Horned Skull Cauldron**: the pot is closed and lined, so looking in shows a dark iron well instead of the sky through its walls; the ram skull has square, Minecraft-like eye sockets and its brew glow is a flat square on them. **Skulls everywhere** (the Colossal Skull, Bone Throne, Skull Footstool, sarcophagi, bone piles, ossuary wall) have square sockets in place of #191's round ones with a glint; the lit Vertebra Floor Lamp's sockets glow whole, and the Colossal Skull's night glow shows at last (it was drawn behind the back of its sockets).
- **Bat in a Jar**: a wider, taller jar and a slightly smaller bat, so its wings stay inside the glass. The oddity jars' lids, bases and knobs are clean beveled iron, their murk lies in even bands and their labels are neat lines of ink (the Beating Heart Jar keeps its first look, which the owner loves).
- **No more see-through props**: faces closed on the Colossal Vertebra, Skull and Rib, Curiosity Cabinet, Bell Jar, the four candelabra, Coffin Wardrobe, Iron-Bound Coffin, Ribcage Bookcase, Grandfather Clock, Farm Stand and the Egg Sac Cluster's sacs (its item showed them as hollow boxes); flat cut-outs drawn by the client are one plane each instead of two that flickered; clean wrought iron on the candelabra, moth case, witchlights and Harvest Moon Lamp.
- **No more flicker**: the Iron-Bound Coffin's bands and corner caps no longer lie flush with its velvet when open, nor the Grandfather Clock's finials with its pediment; the Lab Table patient's flashing eyes show the bright middle of their glow. Rules added to [ART_DIRECTION.md](docs/ART_DIRECTION.md#rules-for-everything); record in [witchs-workshop.md](docs/features/witchs-workshop.md#6-the-bigger-jars).

### Big guns, Landship, walker and balloons (art fixes)
- **Works with the art core's exporter:** the gaps you could see through in the guns, the Landship and the Diesel Walker, and the flicker inside each of their parts, are closed by the exact hidden-face exporter (see "No more see-through or flickering models" above). These models need it and are regenerated with it. What follows is what that exporter cannot fix: moving parts, textures, decals, the balloons and the icons.
- **Clean gun steel with real bores**: every big gun's and tower gun's barrel is plain gun steel instead of a grid of bolted panels, each muzzle has one round dark bore, and port covers and hazard signs are drawn whole instead of in fragments. Yellow, khaki and olive housings are seamless painted armour with one weld seam a course; roofs, housings and brakes are long rolled steel plates instead of short offset blocks that read as brick.
- **Nothing cuts through as a gun aims or fires**: the Siege Mortar's deck meets its base and its rails clear the cradle; the Triple Battery's sleeves and housing clear its drum; the Bastion Autocannon and Fortress Rifle have mantlet slots in their roofs; the Self-Propelled Howitzer has a low engine deck the gun swings over and its exhausts behind the crew; the Flak Gun's cradle clears its pedestal. When a gun fires, only its barrel recoils, back through a cradle that stays put, so breeches no longer punch into decks and roofs.
- **Raised barrels stay drawn**: the Grand Mortar, Siege Mortar, Self-Propelled Howitzer, Flak Gun and Landship are drawn whenever any part of them is in view, so a raised barrel no longer vanishes when you look up at it from close by.
- **Landship and Diesel Walker**: no slit under the Landship's turret and no flicker on its muzzle; its smokestacks are shorter, so the cannon no longer passes through them when the turret turns to the rear. The walker's thighs, chest walls, knees and drill no longer share faces with their neighbours, so it no longer flickers as it walks.
- **Observation Balloon**: a smooth, closed envelope with three tail lobes and its own clean canvas texture (gore seams, red and cream bands, the stencilled serial reading level), instead of stair-stepped boxes with gaps.
- **Hot-air balloons have their own basket back**: since batch 51 they had been drawn with the Observation Balloon's small basket and rigging (two parts shared one name); their burner frame, fuel tanks, gauge and load cables show again.
- **Smooth rise**: the Observation Balloon, pibals and hot-air balloons now glide up on every client instead of stepping; the hot-air balloons' envelopes, rope and flame are drawn in the opaque pass so they can't show through themselves.
- **New icons** for the eight guns, the four shells, the balloon, the Range Finder, the Landship, the Diesel Walker and the Zeppelin: 32x32 pixel art with a closed outline, shading and round bores, each gun with a silhouette of its own (the Grand Mortar on its tower, the Fortress Rifle's long barrel and range finder, the Siege Mortar's railed deck).
- IDs, recipes and numbers are unchanged. Record: [big-guns-art-fixes.md](docs/features/big-guns-art-fixes.md); rules in [ART_DIRECTION.md](docs/ART_DIRECTION.md#big-models-drawn-as-quads-guns-vehicles-and-balloons).

### No see-through rims on held sprites (art fixes)
- The Power and Ronin Katanas' outlines and glow fringes, the Rocket Launcher's and the HE, homing and line-throwing rockets' outlines, and the scuba mask's, scuba tank's and free runners' outlines are opaque: they were drawn part-transparent, so the sprite's rim was see-through in the hand. The katanas' fringe is now the blade's own bright glow.

### The flails' balls swing (art fixes)
- **The bronze and steel flails' chain and spiked ball now swing freely** (the owner, 5 October 2026: "flails should have an animated ball that actually flails around"). The ball hangs from the handle's eye under gravity, trails as you walk, turn or look round, is flung round overhead and whips past after a blow, then swings on and settles. It works in third person, in first person (the guard is held a little higher so the ball hangs in sight), on armor stands and mobs, in either hand, and keeps the enchantment glint.
- **A cleaner ball that keeps out of its holder:** a rounded core with a crown of eight spikes and one below, in two tones, on a chain of four oval links; it keeps clear of its holder's own hips, legs and arms as they move, and nothing in it shares a face plane, so nothing flickers.
- **The Bonecarved Flail swings too:** its spine of vertebrae and a horned skull with square, Minecraft-style eye sockets and a row of square teeth (no nose holes).
- Client-side drawing only (`client/arms/FlailHeads.java`, a small chain simulation on the arms motion's own hooks); no ids, recipes or numbers change, nothing is saved or sent, and the icons are unchanged (PR #201 redraws them). The ball has no collision with the world. Record: [arms-restyle.md](docs/features/arms-restyle.md#the-flails-head-swings-5-october-2026). New client test `FlailClientGameTests` (not yet run in CI), which also walks the player and fails if the ball sinks into the body.

### Cute, clean creature decorations: batches 15 to 20 repainted (#191)
- **Smooth fire**: the Ember Bed's hearth now burns in smooth bands of colour on a glow of coals, and the Horned Skull Cauldron's ram skull has big round sockets and no nostrils.
- **Every creature prop of batches 15 to 20 repainted** in a clean, cute style after the owner's reference pictures: flat colour in two or three tones with lit and shaded edges and no speckle. The **Monster Head** is a bright green Frankenstein head with a blunt black fringe and sleepy closed eyes that open glowing when it wakes; the **Flying Eyeball**, plushes, moths and jar oddities are clean and glossy; skulls (the **Colossal Skull** too) have big round sockets with a glint and no nose holes; bones are smooth cream; the gargoyles have round eyes and little fangs; the **Crawling Hand** is the monster's green; the cocoon sleeps, the clock's ghost says "oo" and the **Harvest Moon** has a sleeping smile.
- Shared painters in `tools/cute_art.py`; the rules are in [ART_DIRECTION.md](docs/ART_DIRECTION.md#creatures-and-faces-cute-and-clean). Block IDs and states are unchanged, so placed props keep working and just look new.

### Pumpkin Night, Halloween decorations batch 20 (#190)
- **Red Kuri** and **Kabocha** pumpkins join the heirlooms: seeds from grass, wild patches and the Halloween Peddler; their own hand-carved pumpkins, glowing ember orange and greenish gold; **Red Kuri Soup** and **Kabocha Tempura** in the Cooking Pot; pumpkin pie, heads and the trebuchet as the other heirlooms.
- **Farm Stand**: an owner's two-block stall of six crates under a striped awning. The owner stocks and prices them in Jugs; anyone in reach buys one item at a time and the Jugs go straight to the owner, every sale checked on the server. Only its owner takes it down.
- **Pumpkin Vine Garland** and **Autumn Leaf Garland**, strung between String Light Hooks with warm bulbs that glow when a hook is lit.
- **Harvest Effigy**: a three-block wicker man in a dyeable cloak who wears a carved pumpkin. Lit at night, he burns for 30 seconds; players near get **Harvest Cheer** (Regeneration and Luck) once a night; rain puts him out. Burnt through, he leaves **Effigy Ashes** that give **Hearth Ash**, a weak fertilizer.
- **Singing Pumpkins** in four voices (bass, tenor, alto, soprano), tuned like note blocks over two octaves and sung by redstone, their mouths opening as they sing; the voices are Jugcraft's own, synthesised. Record: [pumpkin-night.md](docs/features/pumpkin-night.md).

### The Laboratory, the Larder and the Dining Room, Halloween decorations batch 19 (#188)
- **Lightning Harness** (fires on a strong signal, a Tesla Coil's arc or lightning, cracking arcs down to a Lab Table and waking its patient), **Brain-Vat Console** (an analogue memory cell: it remembers the strongest signal at its back until a side clears it) and **Crawling Hand** (drums its fingers; powered, it scuttles).
- **Silk Cocoon** (a 9-slot larder hung from a ceiling that wriggles when opened), **Egg Sac Cluster** (pulsing sacs on any faces, like glow lichen; spiderlings skitter out at night), **Web Drape** (a 2 × 2 web curtain that slows you) and **Silk Spool Stack** (three spools, each dyed to any colour).
- **Haunted Dining Chair** (sits you; at night slides out toward a player near), **Floating Table Setting** (laid for dinner, tea or a feast, bobbing over the table, its candle lit with flint) and **Grandfather Clock** (hands on the time of day, the moon's phase in its arch, a pulse and the hour struck each hour, a comparator reading the hour, and a face at the glass at midnight).
- **Witchlight Lamp-Post**, **Path Stake** and **Hanging Witchlight** (wake as a player comes near, linger, then sleep; redstone keeps them awake; five dye colours).
- **Yard Silhouette** (six black cut-out figures whose eyes glow at night, in sixteen turns) and the **Harvest Moon Lamp** (2 × 2; its face shows tonight's moon, which a comparator reads). Record: [laboratory-larder-dining.md](docs/features/laboratory-larder-dining.md).

### The Crypt and the Ossuary, Halloween decorations batch 18 (#186)
- **Iron-Bound Coffin** (54 slots; locks to a **Skeleton Key** cut from a **Key Blank**, and then opens only for someone holding that key and refuses hoppers and pipes; keys copy onto blanks at a crafting table) and the **Coffin Wardrobe** (a skeleton mannequin behind glass that swaps the armour you wear for the armour it holds; cursed pieces stay on).
- **Stone, Deepslate and Blackstone Sarcophagi** (27 slots under a lid that slides aside over a skeleton; the Stonemason's Chisel carves the lid as a knight, a lady or a skull; at night, shut, they sometimes knock).
- **Bone Throne** (a seat whose crest's eyes glow while sat in at night), **Ribcage Bookcase** (a chiseled bookshelf that powers enchanting tables), **Skull Footstool** and **Vertebra Floor Lamp**.
- **The Buried Colossus**: a 2 × 2 × 2 **Colossal Skull** whose jaw drops on redstone, **Colossal Ribs** that meet as an arch, **Colossal Vertebrae** that line up as a spine, and a **Colossal Femur**.
- **Gargoyle Sentinel** (watches for monsters within 16 blocks and gives a signal by their distance), **Gargoyle Rainspout** (pours rain into a cauldron below) and **Chimera Finial** (a rain and storm sensor that spreads its wings in a storm). Record: [crypt-and-ossuary.md](docs/features/crypt-and-ossuary.md).

### The Witch's Workshop, Halloween decorations batch 17 (#183)
- The [Witching Season plan](docs/features/witching-season.md): twenty Halloween and fall prop sets from the owner's reference pictures, in four batches, then two bosses (a scythe-bearing reaper and a spider seamstress) in their own pocket-dimension lairs. What the uploaded pocket-dimension and animation mods and the Soulslike Weaponry boss page taught, and how Jugcraft does the same without new dependencies.
- **Horned Skull Cauldron** (holds water or three bottles of one potion with nothing gained or lost; bubbles and fumes over heat; the **Brew Ladle** wafts a lasting potion onto up to four players near at a quarter duration; ingredients float in it) and the **Ember Bed** heat source.
- **Wrought-iron candelabra**: the Floor Candelabrum, Table Candelabrum, Wall Girandole and Branching Chandelier, in six waxes and four flames, lit by flint and steel, burning arrows or redstone, with wax drips that grow while lit.
- **Enchanted Broom** (anointed with Flying Ointment, it sweeps dropped items into a **Dustpan**) and the **Broom Rack**.
- **Curiosity Cabinet** (nine places behind glazed doors), **Bell Jar** and **Moth Display Case** (the moths stir at night).
- **Oddity jars**: eyeballs that watch, a beating heart that is a redstone clock, a bat that wakes, a two-headed snake and a drumming hand.
- Sculpted props may use 128 × 128 textures (`docs/ART_DIRECTION.md`; the audit allows it). Record: [witchs-workshop.md](docs/features/witchs-workshop.md).

### Arms VII, batch 56 (#184)
- **32 named variant arms,** each fighting as its kind does:
  - **Crafted styles** at a smithing table, from a steel arm, the style's pattern and a material (enchantments and wear kept): **gilded** (gold; enchants as gold), **ironclad** (dieselpunk; lasts twice as long), **bonecarved** (bone; harder against the undead) and **runebound** (glowing runes; marks foes so they glow).
  - **Boss trophies** for eight bosses still to be made, two each with a boon: the Yeti King (frost), the Cinder Tyrant (ember), the Mire Hag (venom), the Crypt Lich (drain, wither), the Iron Dreadnought (shock), the Alpha Werewolf (howl), the Storm Roc (gale) and the Abyssal Leviathan (tide). Their loot tables are ready; creative-only until the bosses exist.
- Glowing parts light up in the dark. A boss brainstorm: [branches/BOSSES.md](docs/branches/BOSSES.md). Record: [arms-vii.md](docs/features/arms-vii.md).

### Arms restyle (#180)
- **Every arm redrawn:**
  - **Icons:** crisp pixel-art icons (32 or 48 pixels, on the diagonal, flat tones, outlined);
  - **In the hand:** a 3D model with real thickness: thin blades with a raised ridge, chunky guards, round grips, deep heads;
  - **Palettes:** copper-bronze with brass and leather; blued steel with gunmetal, rubber and brass rivets.
- **The longbows, arbalests and shield faces** are redrawn to match.
- **Refined:** bevelled two-tone blades with a glint; the pike's langets; the quarterstaff's shod ends; the kama, war pick, katar and kusarigama held larger.
- **Simplified (second pass):** plainer rapier and sabre hilts; bigger, cleaner halberd, bill, pike and war hammer heads; no stray rivets or stones on the heads; a one-grip katar; the kusarigama's chain clear of its handle.
- After studying how Simply Swords, Epic Knights and RPG Style More Weapons make weapons that read well; nothing of theirs is copied. Record: [arms-restyle.md](docs/features/arms-restyle.md).

### Arms VI, batch 55 (#179)
- **Katana:** quick, clean cuts, and its art **Seven Cuts**: seven cuts in a breath across every foe ahead, each leaving an arc of colour in the air (crimson from bronze, pale gold from steel).
- **Brazier mace:** a burning brazier on a haft, with a flickering flame. It sets what it hits alight and lights campfires, candles and the ground.
- **Longbow and arbalest:** a tall bow and a crossbow with a metal prod. Each shot hits harder and flies flatter than a bow's or crossbow's, though they deal no more a second. Drawn and wound in three steps, as vanilla's are.
- **Heater and tower shields,** built in 3D: the heater is quick to raise; the tower covers your flanks and braces you, but is heavy and slow.
- After the owner's reference sheets (studied for their look; nothing is copied). Handbook pages, the **War Kit** advancement, five game tests and a client test. Record: [arms-vi.md](docs/features/arms-vi.md).

### Arms V, batch 48 (#173)
- **Weapon arts:** six new arms in bronze and steel, each with a special move used with the use key, its own animation and its own way of dealing damage. Then a few seconds before it is ready again (shown on the hotbar); plain blows are not held back.
  - **twinblade** (two-handed), **Cyclone**: three spins, each striking every foe all round and drawing them in;
  - **nodachi** (two-handed), **Iaido**: a dash; every foe passed is cut a moment later, all at once;
  - **earthbreaker** (two-handed), **Leap Slam**: leap and slam where you land, hardest at the centre and harder from a height; throws foes up, and the leap costs no fall damage of its own;
  - **katar**, **Flurry**: five quick jabs that all land, then a driving finish;
  - **moonblade** (two-handed), **Crescent**: a wave runs ahead through every foe in line until a wall stops it;
  - **kusarigama**, **Chain Lash**: the chain catches the first foe in line up to 9 blocks off, hauls it in, and the sickle reaps it.
- Every art runs on the server and is seen by every player nearby. Against one foe an art is no better than plain blows; the data check holds it to that.
- Handbook pages, the **Weapon Art** advancement, seven game tests and an end-to-end client test. Record: [arms-v.md](docs/features/arms-v.md).

### The haunted house's props, Halloween decorations batch 16 (#175)
- **Flying Eyeball** (hovers on bat wings and stares at the nearest player), **Pillar Candles** in ivory and black (vanilla candles, one to four in a dripping cluster), **Spider Web** (on any face), and the **Monster's Head** (wakes on a redstone signal: jaw open, eyes glowing, bolts sparking).
- Five **harvest plushes** for the midway's prize table: owl, hedgehog, acorn, corn and maple leaf.
- Sculpted from the owner's reference pictures; 64 × 64 textures. Six game tests, client screenshots. Record: [haunted-house-props.md](docs/features/haunted-house-props.md).

### The churchyard's ornaments, Halloween decorations batch 15 (#174)
- **Gargoyle** on a granite plinth (a graveyard monument: it weathers and takes an inscription), **Bone Pile** (heaps to four layers), **Ossuary Wall** (skulls and long bones), **Giant Bone Hand** (clenches on a redstone signal) and **Witch's Lantern** (violet glass, standing or hanging).
- Sculpted from the owner's reference pictures; 64 × 64 textures. Six game tests, client screenshots. Record: [churchyard-ornaments.md](docs/features/churchyard-ornaments.md).

### The graveyard flora (#172)
- **Seventeen plants for a haunted churchyard**, sculpted like hand-built plants (bent stems, cut-out leaves and petals at angles, bells and berries as little boxes) on 64 × 64 textures: spider lily, snowdrop, deadly nightshade, bleeding heart and the glowing ghost pipe; black rose, foxglove, funeral lily and asphodel (two tall); withered grass and the ghost fern with their tall forms; dead man's fingers; grave moss; shroud moss hanging in strands; creeping ivy.
- **The mandrake:** wild, or grown from its root; a ripe one screams when pulled, sickening bare-headed players within 8 blocks (Mind Your Ears). Its root makes Flying Ointment.
- They grow in the haunted biomes and some of vanilla's. Grave vases take them by colour. Eight game tests, client screenshots. Record: [graveyard-flora.md](docs/features/graveyard-flora.md).

### Arms IV, batch 47 (#170)

### Unmerged: The Armoured Walker, batch 58
- **Armoured Walker:** a heavy walker made from the owner's own Blender model:
  - an octagonal riveted blue-grey hull with a framed gun port and cannon
  - amber lamps, a chain slung across the front and a roof pouch
  - a jointed tool arm and a piston ram arm
  - thigh slabs, angled shins and hinged feet
- Piloted like the Diesel Walker:
  - **hold use:** fires the hull cannon (Heavy Shells, damage only, every 2 seconds)
  - **attack:** rams with the piston (16 damage, throws hard)
  - takes 90 damage to knock down
  - crafted by upgrading a Diesel Walker with steel and pistons
- The **Raider Walker** now uses the same model in raider paint. It rams with its piston and lobs grenades from its hull gun.
- Three game tests and a screenshot scene. Record: [armoured-walker.md](docs/features/armoured-walker.md).

### Unmerged: The raider faction, batch 57
- **Raiders:**
  - the **Grunt** (cleaver)
  - the **Grenadier** (lobs small grenades from range)
  - the **Officer**: rallies raiders near them. Their fall routs the rest. A player's kill takes their **Raider Insignia**.
  - the **Raider Walker**: a raider-built Diesel Walker that punches and fires a shoulder grenade launcher
  - the **Raider Blimp**: cruises over its target and drops bombs
- **Raids:**
  - After a player has played three days, at most once every three days per world, a party gathers 48–64 blocks away and marches on their base (or the town if they are near it).
  - A raid bar shows how much of the party is left. Beating a raid raises the raid level (up to 5: more raiders, blimps from level 2, a walker from level 3). Ignored raids withdraw.
- **Never griefs:** raiders break no blocks, and every grenade and bomb is a damage-only blast that spares raiders. Sentry guns, flak and town guards fight them.
- **Switch:** `raiders.enabled=false` or `raiders.raids=off`. `raiders.walkers`, `raiders.blimps`, `raiders.grace_days` and `raiders.interval_days` tune it.
- **Siege Ladders:** a grunt stuck at a wall props up a ladder and climbs it. It needs mob griefing on, and the ladder crumbles after a minute, dropping nothing.
- **Raider War Horn** (three insignia and a goat horn) calls a raid on purpose. The **Beat Them Back** advancement goes to everyone who sees a raid through.
- **Raider camps:** rare sandbagged camps in the plains, savanna and badlands, never within 512 blocks of the world spawn. Each has tents, a campfire, a supply barrel and a garrison of four to clear.
- Ten game tests and a screenshot scene. Record: [raiders.md](docs/features/raiders.md).

### Unmerged: Fire control, batch 56
- **Fire Control Table:** link up to 8 guns to it with **Fire Control Wire**. It lays every linked gun that has nobody at its controls. Its modes:
  - **Hold:** the guns stand still.
  - **Converge:** every gun lays on the table's target (your Range Finder mark), and a redstone pulse into the table fires one round from each.
  - **Parallel:** as Converge, but the guns' shells land 6 blocks apart across the line of fire.
  - **Creeping Barrage:** as Converge, but each salvo after the first lands 5 blocks further down range, six steps, then starts again.
  - **Sentry:** each gun fires by itself at the nearest hostile mob in the table's sector (90°, 180°, 270° or all round). It never fires within 12 blocks of the gun, or at a mob with a player within 8 blocks of it.
- A gunner aboard a linked gun with no mark of their own has it laid on the table's point, and fires it when they choose.
- Guns the table fires use shells from ready racks only. A comparator reads how many linked guns are ready. A ringing field telephone can give the order to fire.
- Five game tests and a screenshot scene. Record: [fire-control.md](docs/features/fire-control.md).

### Unmerged: Fortifications, batch 55
- **Bastion Concrete** (block, slab, stairs and Jugcraft's first **wall**, which joins diagonally like vanilla's), the crenellated **Bastion Parapet**, the **Steel Ladder** and a redstone-only **Blast Door**.
- **Ammo Hoist:** stack hoists into a shaft. What goes in climbs to the top and into the container on or beside it. Nothing can pull items back out.
- **Ready Rack:** holds shells beside a gun. A gunner with no shells draws from any rack within 2 blocks of the gun. It shows how full it is.
- **Extras:**
  - a strapped timber **Bunker Door** that opens by hand
  - a redstone **Sliding Gate**: panels side by side or stacked open together
  - a **Bastion Parapet Corner**
  - a **Bastion Embrasure** with a gun slit
- Five game tests and a screenshot scene. Record: [fortifications.md](docs/features/fortifications.md).

### Unmerged: Tower guns, batch 54
- Five heavy emplacements for the top of a tower, after the owner's reference picture of a heavy mortar on a turntable mount: a concrete plinth, a railed turntable, a yellow cradle and a fat black barrel.
  - For a 3x3 top: the **Bastion Mortar** and the twin-barrelled **Bastion Autocannon**.
  - For a 5x5 top: the **Grand Mortar**, the long **Fortress Rifle** (out to about 210 blocks) and the three-barrelled **Triple Battery**.
- Each needs a solid top under its whole footprint, and is crewed and aimed like the big guns.
- The Grand Mortar fires the new **Great Shell** (2 Heavy Shells and TNT). Its burst reaches 7 blocks; like every shell, it hurts creatures and never breaks blocks.
- Three game tests and a screenshot scene. Record: [tower-guns.md](docs/features/tower-guns.md).

### Unmerged: Clean steampunk textures, batch 53
- The steampunk textures (`sp_*`) get the same clean style as the dieselpunk ones: flat fills, bevelled plates with rivets, banded sheens, and wear only as a few placed marks. Iron and brass plates, wrought iron, brass, copper, tanks, planks, firebrick, glass, red iron, the hopper inside, water, lava, leaves, bark and soil lose their per-pixel noise.
- Record: [clean-steampunk-textures.md](docs/features/clean-steampunk-textures.md).

### Unmerged: Clean dieselpunk textures, batch 52
- Every dieselpunk texture is redrawn in a cleaner style: flat fills from short palettes, bevelled panels, recessed insets and two-by-two bolts, with wear only as a few small marks at corners and seams. No more per-pixel noise, and rust is now a small stain under a bolt rather than the whole surface. This covers the giants, Dieselworks blocks, zeppelin, Diesel Walker, Landship, trench works, big guns and the dieselpunk machine textures. The mechs and vehicles also get a clean soot texture for their smokestacks and openings, and the kaiserpunk brass and black lacquer lose their specks.
- Rust Plate, Riveted Rust Plate, Ribbed Rust Pillar and Rust Grating are now called Weathered Steel Plate, Riveted Steel Plate, Ribbed Steel Pillar and Steel Grating. Their IDs are unchanged, so placed blocks and items keep working.
- Record: [clean-dieselpunk-textures.md](docs/features/clean-dieselpunk-textures.md).

### Unmerged: Big guns, batch 51
- **Siege Mortar**, **Self-Propelled Howitzer**, **Flak Gun** and **Observation Balloon**, with Heavy Shells, Flak Shells and a **Range Finder** to mark targets up to 256 blocks away.
- Guns turn to a marked target (the gunner's, or a nearby spotter's) and work out the elevation that lands the shell there. Without a mark they fire where the gunner looks. Flak bursts beside flyers.
- Every burst hurts creatures and never breaks blocks. Five game tests and a screenshot scene. Record: [big-guns.md](docs/features/big-guns.md).

### Unmerged: Trench works, batch 50
- Sandbags, timber revetment, duckboards and barbed wire. The wire slows and cuts whatever pushes through it; sneak to cross carefully.
- **Field telephones:** power one's back and every telephone on its dye channel within 256 blocks rings and gives a redstone signal from its front.
- **Searchlights:** a turning, tilting lamp that throws a long beam. Redstone switches it off.
- Three game tests and a screenshot scene. Record: [trench-works.md](docs/features/trench-works.md).

### Unmerged: Arms IV, batch 47
- **Five ornate arms** in bronze and steel, drawn after the owner's reference sheets (nothing of them is copied): set stones (a garnet in bronze, a lit green phosphor stone in steel), flat-toned heads with bright edges, winged guards, lit rims and glints of light.
  - **labrys** (two-handed): its finishing blow whirls right round, striking up to 6 foes about you;
  - **battleblade** (two-handed): each hit wears every piece of the foe's armor;
  - **war fork** (two-handed thrust): half again as much damage to a foe charging at you;
  - **kama**: use on grass, ferns, vines or leaves to cut them all, 3 by 3 by 3;
  - **war pick**: mines stone and ore as its metal's pickaxe.
- Handbook pages, the **Masterwork** advancement, six game tests. The data check now also fails if an arm's id is taken elsewhere. Record: [arms-iv.md](docs/features/arms-iv.md).

### Arms III, batch 46 (#167)
- **Two-handed weapons**, after studying the Fiery Combat add-on the owner sent (nothing of it is used). Greatswords, war hammers, glaives, battle axes, scythes, quarterstaves, pikes and the new kinds now swing with both hands:
  - a click starts the swing, and the blow lands as the swing comes round, on the frame the animation lands it, not on the click;
  - the blow strikes every foe in an arc (up to 2 to 5 by kind);
  - you are slowed while the swing is in the air;
  - the last attack of each combo is a finishing blow, 25% stronger;
  - a shield in the off hand stops the swing.
- The server decides every swing and blow, and refuses vanilla's instant hit with these arms.
- **Four new two-handers** in bronze and steel:
  - **zweihander:** the widest cleave, and a guard;
  - **maul:** its finishing blow shakes the ground;
  - **executioner's sword:** half again as much against a foe at 30% health or less;
  - **bill:** hooks foes towards you and drags riders from the saddle.
- Handbook pages, the **Two-Hander** advancement, seven game tests and an end-to-end client test. Record: [arms-iii.md](docs/features/arms-iii.md).

### Arms II, batch 45 (#164)
- **Eight more arms** in bronze and steel, each with a trait of its own:
  - **dagger:** quick stabs; half again as much from behind;
  - **sabre:** 3 more damage from the saddle;
  - **estoc:** pierces armor, up to 6 more against heavy armor;
  - **battle axe:** chops wood as an axe and breaks a shield's guard;
  - **flail:** a hit slows the foe for 2 seconds;
  - **scythe:** wide sweeps, and reaps and replants ripe crops 3 by 3;
  - **quarterstaff:** knockback and a parry;
  - **pike:** the longest reach (5 blocks), half again against riders and their mounts.
- Each has its own art, guard, combo and first-person strokes. None out-damages its metal's sword per second, which the data check now enforces.
- Handbook pages, the **Armory** advancement, seven game tests. Record: [arms-ii.md](docs/features/arms-ii.md).

### The leaf blower, fall addition 30 (#139)
- **Leaf Blower:** a dieselpunk electric leaf blower in the powered tools' olive drab, gunmetal and chrome, charged at the Charging Station (40,000 JE; 4 JE a tick blowing, 6 vacuuming).
- **Blow** (hold use): a stream 8 blocks long blows items along and nudges mobs (other players only where PvP allows), puts out candles, and **herds leaf piles** a layer at a time into heaps against whatever stops them. Gone with the Wind.
- **Vacuum** (sneak): draws items in and takes leaf piles and leaf litter up into your inventory, a layer for an item, for the composter.
- Only where its user may build. Eight game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-leaf-blower).

### The hot-air balloon fiesta, fall addition 29 (#139)
- **Hot-air balloons** in three designs: Harvest Stripes, a Jack-o'-Lantern special shape with carved faces, and Harvest Moon with a witch across the moon. A wicker basket for four under a twin-coil burner; fuel it with coal, charcoal or coke.
- **Fly by heat:** the pilot fires the burner (jump) and opens the vent (back). It can't be steered: it drifts on **winds that blow different ways at different heights**, eight layers from sea level, the lowest two roughly opposite, so you can fly out low and come home higher (The Box). Gauges show height, heat, wind and fuel.
- **Pibals** to see the winds aloft; **Mooring Posts** to tether a balloon for rides; a **night glow** while the burners fire. Up, Up and Away, The Box, Mass Ascension.
- Envelopes are smooth turned surfaces painted as 768 × 384 wraps; the rest at 64 × 64. Ten game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-hot-air-balloon-fiesta).

### The piñata party, fall addition 28 (#139)
- **Three piñatas** of crepe-paper fringe: a jack-o'-lantern Pumpkin, a seven-pointed Star with tassels, and a winged Bat. Hang one from the underside of a block, and anyone can fill it with anything, up to nine stacks.
- **Swing at it:** a charged swing is a hit (the red-and-white **Piñata Stick** counts two); it swings on its rope, tears at half its hits, and bursts in confetti on the last, spraying its contents out. Piñata Party.
- **The Blindfold** blacks out your view but for a sliver; burst one blindfolded for Blind Luck.
- Painted at 64 × 64. Five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-piñata-party).

### The Ferris wheel, fall addition 27 (#139)
- **A fairground big wheel**, real-life sized: place the **Ferris Wheel** booth and the wheel rises over it, 16 blocks high, two lattice A-frames, red trussed rims with 128 bulbs, and eight cars (pumpkin, cranberry, mustard and spruce) seating two each.
- **Kinetic power turns it:** a hand crank, shaft or engine against the booth; 12 KE a tick is full speed, a turn in 40 seconds. One player can crank while friends ride.
- **Ride it:** use the booth to board the car at the bottom; sneak to get off and you're set down by the booth. A block in a car's way jams it. Round and Round, and Two to a Car for riding round together.
- It takes the planned ghost train's place (the Haunted Hayride and Jump-Scare Trap already cover that). Painted at 64 × 64. Seven game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-ferris-wheel).

### The fall fair midway, fall addition 26 (#139)
- **The High Striker:** a red-and-white fairground tower five blocks tall with lamps up its front and a bell on top. Hit its pad with a **Carnival Mallet**: the puck climbs as far as the blow was strong, lighting the lamps with a rising note. Ring the bell (a fully charged swing, better still a falling one) to win a prize (Ring the Bell).
- **Ring Toss:** a crate of nine bottles. Toss **Toss Rings** at it from three blocks or more; one over a bottle's neck is a ringer and wins a prize (Ringer!).
- **Plush prizes:** a pumpkin, a ghost, a bat, a black cat, a squirrel, a rare werewolf and the 1-in-102 **Jumbo Pumpkin Plush** (Jackpot). Squeeze one and it squeaks. Step Right Up for any prize.
- Painted at 64 × 64. Eight game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-fall-fair-midway).

### The Pumpkling, fall addition 25 (#139)
- **Wake a carved pumpkin:** use a Wisp in a Jar or a bottle of Ectoplasm on a hand-carved pumpkin with a face, and it hops up as a **Pumpkling**, a pet on little vine legs wearing the face you carved (Little Jack).
- It **follows** you, comes to you from afar, and **sits and stays** when you use it with an empty hand. A torch lights its face (a soul torch, blue); treats heal it.
- **It guards crops:** crows keep away from crops near it, as from a scarecrow. A glass bottle settles it back into its pumpkin.
- Six game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-pumpkling).

### Squirrels and acorns, fall addition 24 (#139)
- **Squirrels**, red and grey, come to forests and taiga by day. They bound about with their bushy tails, climb tree trunks, and bolt when hurt.
- **Acorns** drop from oak and dark oak leaves. Plant one on grass for an oak sapling, or roast it into **Roasted Acorns**.
- A squirrel takes an acorn lying near, carries it off and **buries it**; one in four grows into an oak. Nuts tempt and breed squirrels (Nuts About Squirrels).
- Five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#squirrels-and-acorns).

### Full-moon werewolves, fall addition 23 (#139)
- **Werewolves** come out of forests and taiga only on full-moon nights, howling: hulking, hunched wolf-men with long snouts, open jaws, long clawed arms and glowing eyes. They leap at players and villagers, shrug off half of any blow and heal, and are gone at dawn.
- **Three kinds, three tiers:**
  - the **Brown Werewolf** (I) raids livestock too, howls its pack to the hunt and flees when badly hurt;
  - the **Snow Werewolf** (II), in snowy woods, bites with frostbite and runs faster on snow;
  - the rare **Shadow Werewolf** (III), the alpha, steps out of the shadows behind its prey; its howl brings Darkness and drives the pack into a frenzy, and a sprig of wolfsbane won't stop it (Leader of the Pack).
- **Silver** hurts them two and a half times as much and stops their healing: a **Silver Dagger** and **Silver Arrows**. Slaying one with silver earns Silver Lining.
- **Wolfsbane**, a wild flower of taiga and forest (plantable and potted), wards them off: they won't hunt anyone holding a sprig or near it (Not Tonight).
- Each kind drops its own **pelt**: two make that kind's **rug**, or cut one into leather. Thirteen game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#full-moon-werewolves).

### Flying broomstick, fall addition 22 (#139)
- **Flying Broomstick**: a Witch's Broom anointed with Flying Ointment and two feathers. Use it to get on; it flies where you look (forward, back to brake, jump to climb), up to about 10 blocks a second, a quarter faster in a witch hat. It hovers where you leave it.
- **Ointment is its fuel**: 2 minutes of flight each, up to 6, burnt only in the air. Run dry, it sinks gently to the ground. Anoint a broom with Flying Ointment; sneak-use takes it back with its flight.
- No fall damage while riding, and slow falling when you get off in the air. The server checks each broom's flight and throws off a rider who moves it impossibly.
- Two advancements (Up and Away; Over the Moon, a challenge), five game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#the-flying-broomstick).

### Hex brews, fall addition 21 (#139)
- **Hex brews at the Bubbling Cauldron.** Over a fire, a brew takes a hex ingredient: green and a brown mushroom make the **Shrinking Draught**, orange and beans the **Giant's Draught**, purple and a phantom membrane **Flying Ointment**. A hex brew fills three bottles.
- **Shrinking** makes you half size for 3 minutes, small enough for one-block gaps. **Giant** makes you 1.6 times your size for 3 minutes, with a block more reach and a higher step, and is refused where there is no room to grow. A shrunk player with no room to grow back stays small until there is. **Flying Ointment** gives 30 seconds of slow falling.
- Two advancements (Drink Me, Fee-Fi-Fo-Fum), three game tests, client screenshots. Record: [even-more-fall-additions.md](docs/features/even-more-fall-additions.md#hex-brews).

### Graveyard pack 4, grounds (#139)
- **Kerbed Grave** in polished granite and marble chippings with an open book; **Planted Grave** whose flower bed goes to weeds as it is neglected; **Memorial Bench** with an inscribed plaque that seats a player; **Open Grave**, freshly dug with boards, straps, a spoil heap, a spade and a waiting cross.
- **Grave Vase**: small flowers make a bouquet of their colour (Flowers for the Dead). Fresh flowers calm the graves within 3 blocks to half the spirits; they wilt in about a day. An open grave stirs spirits twice as often.
- **Cemetery Lamp Post**: three blocks of cast iron, its lantern lit after dark and out by day.
- Game and client tests. Record: [graveyard.md](docs/features/graveyard.md#grounds).

### Graveyard pack 3, buildings (#139)
- **Family Mausoleum**: an 85-block marble temple front with a portico of Tuscan columns, the family name on its frieze, and a room inside with twelve crypt fronts that each take their own inscription, an altar under a stained-glass window and a sanctuary lamp.
- **Lych Gate** of oak and slate on rubble walls, its tie beam inscribed; **Cemetery Gateway** of granite piers with lanterns and a wrought-iron arch bearing the cemetery's name; **Columbarium** of six inscribed niches.
- **Bronze Mausoleum Door**, opened by hand. Doorways and passages are left open for doors and gates of your own.
- Buildings weather, wax and stir spirits like headstones and break as one, keeping every inscription. Game and client tests. Record: [graveyard.md](docs/features/graveyard.md#buildings).

### Graveyard pack 2, monuments (#139)
- **Six monuments**: a four-block **Grand Obelisk** in granite, a **Draped Urn**, the **Angel at the Tomb** (a grieving angel kneeling at an altar tomb, two blocks wide), a four-block **Trumpeting Angel** on a fluted column, an iron **Mortsafe** caged over a grave, and a bronze **Faithful Hound** watching on its plinth. The angels, urn and hound are sculpted.
- They weather, wax, take epitaphs and stir spirits like the headstones; the mortsafe rusts and the hound grows verdigris.
- Three game tests, client screenshots. Record: [graveyard.md](docs/features/graveyard.md#monuments).

### Graveyard pack 1, headstones (#139)
- **Nine life-sized, finely carved headstones** in four stones: a **gothic** marble headstone, two New England **slates** (willow and urn; winged skull), a child's **lamb** stone, a **broken column**, a three-block **Celtic high cross**, a **rustic scroll** on a granite boulder, a two-block **table tomb** and a **ledger stone**.
- **They weather**: clean, worn, mossy, overgrown (ivy at the last). A brush scrubs a stage off, honeycomb waxes them, an axe takes the wax off, bone meal ages them. The letters fade as the stone weathers.
- **The Stonemason's Chisel** opens an epitaph screen of four lines, checked on the server; each line is cut as large as it fits. A named Name Tag cuts the first line. Epitaphs go with the broken headstone.
- **Neglected graves stir restless spirits more often**; a well-kept churchyard is quiet.
- Two advancements, four game tests, client screenshots. Record: [graveyard.md](docs/features/graveyard.md).

### Unmerged: Landship, batch 49
- **Landship:** a rideable kaiserpunk crawler tank for three. It has a rhomboid hull with animated tracks running round each side, a crest-bearing hub, sponson guns, smokestacks, and a turret whose cannon follows where the driver looks.
- Drive with the movement keys; it climbs steps and bridges narrow trenches. Attack fires the cannon, using cannon shells; hold use for the side guns. It burns diesel or kerosene.
- The shells' burst hurts creatures and never breaks blocks. Three game tests and a screenshot scene. Record: [landship.md](docs/features/landship.md).

### Unmerged: Kaiserworks, batch 48
- 26 imperial building blocks to go with the dieselpunk set: black lacquer, riveted black and gilt-trimmed plate, polished brass, a gilt key-pattern frieze, an imperial crest (our own made-up empire), fluted marble and black iron columns, polished marble, station tiles, see-through wrought-iron lattice, leaded glass and gas lamps.
- Made from plates, calcite, glass and gold trim, never back into metal. Game test and a screenshot scene. Record: [kaiserworks.md](docs/features/kaiserworks.md).

### Unmerged: Diesel Walker, batch 47
- **Diesel Walker:** a rideable dieselpunk mech, about four blocks tall, with an open cockpit, a glowing core, a big fist and a drill arm. Walk with the movement keys (it climbs one-block steps), jump, hold use to drill the block you look at and press attack to punch. It burns diesel or kerosene from buckets. The server drives it from the pilot's keys.
- The drill breaks one block at a time as the pilot would by hand, so protected land and break checks still apply. It is no quarry.
- Four game tests and a screenshot scene. Record: [diesel-walker.md](docs/features/diesel-walker.md).

### Unmerged: Zeppelin, batch 46
- **Zeppelin:** a rideable dieselpunk airship for four. You steer it with the movement keys, jump to climb and sprint to sink. It burns diesel or kerosene from buckets and has a 27-slot cargo hold. It hovers where it is left and sinks gently when out of fuel. The server flies it from the pilot's keys.
- Two game tests and a screenshot scene. Record: [zeppelin.md](docs/features/zeppelin.md).

### Unmerged: Dieselworks, batch 45
- 25 building blocks in the giants' look: rust, riveted, patina, perforated, red iron and copper dome plate (most with slabs and stairs), riveted band, skid iron, ribbed pillars, see-through rust grating for catwalks, steel I-beams, porthole windows and amber cage lamps.
- Made from metal plates (one plate per block), never back into metal. Game test and a screenshot scene. Record: [dieselworks.md](docs/features/dieselworks.md).

### Unmerged: Dieselpunk giants, batch 44
- Sixteen one-block machines are now big, detailed multi-blocks (from 2x2x2 up to the 5x3x3 rocket workshop, with long ones like the 2x2x5 sawmill and the 4x1x2 wire drawer) in a weathered dieselpunk look: rust, patina, perforated covers, ribbed coil stacks, banded domes, copper pipes with hex fittings and amber glow.
- Machines already built in a world stay one block and keep working (`compact` state); placing the item builds the full machine.
- Game tests for forming, the compact copies and breaking. Record: [dieselpunk-giants.md](docs/features/dieselpunk-giants.md).

### Test fix: each giant tree's own fewest logs (#157)
- `BiomeGameTests.bigTreesGrow` holds each giant to the fewest logs it can grow: a giant redwood 85, a giant mahogany 37 (a trunk two wide and at least 22 or 10 tall). #134's single floor of 30 let a stunted redwood pass. Test only; nothing in game changes. Record: [big-trees-and-rainforests.md](docs/features/big-trees-and-rainforests.md).

### Unmerged: Arms motion, batch 43
- **New animations for every arm of batch 42**, seen by everyone and in first person.
  - Each kind has a guard: the side-on stance, two hands on the greatswords, hammers and polearms, the rapier en garde. Each kind also has a combo of attacks with anticipation, the blow, follow-through and a settle. The torso turns and bends, the head counters, and the feet step.
  - In first person, the held arm takes a guard and a stroke for each attack.
  - The spear and lance keep vanilla's arms and add the body.
- Keys joined by Hermite splines so the motion flows through them. Timed on vanilla's own swing, so there is no networking. Client only, with no allocation per frame and the arms only beyond 32 blocks; four client mixins.
- Learned from the combat animation mods and packs the owner sent (Better Combat, Malfu, Player Animation Library, Fresh Animations and Fresh Moves, Mo' Bends, NdRz's weapons); nothing of theirs is used. Record: [arms-motion.md](docs/features/arms-motion.md).

### Unmerged: Liquid fuels, batch 43
- **RP-1 kerosene**, hydrocracked from heavy fuel oil and hydrogen in the catalytic cracker. It is jet fuel too, burning in the gas turbine and the advanced engine.
- **Cryogenic Liquefier**: condenses oxygen into **liquid oxygen**.
- **Kerosene and liquid oxygen tanks** (chemical reactor) make **liquid rocket motors**: three rocket motors from a tank of each and two nozzles, with no solid propellant.
- Advancement, handbook pages, game test. Record: [liquid-fuels.md](docs/features/liquid-fuels.md).

### Unmerged: Booster rails, batch 42
- **Booster rail:** a powered rail with rocket thrusters, loaded with solid propellant (8 boosts each). It kicks a minecart to full speed, from a standstill or uphill, and holds it there for 10 seconds, so long tracks and climbs need far fewer powered rails. Minecarts keep their normal speed limit.
- Handbook page, game test. Record: [booster-rails.md](docs/features/booster-rails.md).

### Unmerged: Rocket launcher, batch 41
- **Rocket launcher** with **high-explosive** and **homing rockets** (rocket workshop). Rockets fly straight, burst on impact and hurt living things only: no block is ever broken. Homing rockets lock on to the hostile mob nearest the crosshair and steer into it.
- Handbook page, two game tests. Record: [rocket-launcher.md](docs/features/rocket-launcher.md).

### Unmerged: Zipline, batch 40
- **Zipline anchors** and the **line-throwing rocket:** fire a steel line between two anchors up to 96 blocks apart with a clear path, then use an anchor to ride the line to the other end. Steeper lines are faster; sneak to let go.
- Handbook page, game test. Record: [zipline.md](docs/features/zipline.md).

### Unmerged: Rocket post, batch 39
- **Rocket pads** send up to nine stacks to another pad up to 4096 blocks away: a **delivery rocket** (used up) and a **flight plan** (kept) naming the target pad. Launch from the pad's screen or with a redstone pulse.
- Deliveries are saved with the world. One whose target area isn't loaded waits and lands when the area loads again; nothing is force-loaded.
- Handbook page, game test. Record: [rocket-post.md](docs/features/rocket-post.md).

### Unmerged: Rocketry, batch 38
- **Rocket Workshop** and propellant chemistry: ammonium perchlorate, iodine (from kelp), silver iodide and solid propellant; rocket casings, nozzles, guidance units and motors.
- **Survey rocket:** surveys ores and oil under 7x7 chunks. **Cloud-seeding** and **clear-sky rockets:** five minutes of rain or clear sky, with a shared cooldown. **Signal flares:** tell nearby players where you are. **Illumination flares:** make hostile mobs glow.
- Advancements, a Rocketry handbook chapter, game test. Record: [rocketry.md](docs/features/rocketry.md).

### Unmerged: Arms, batch 42
- **Nine kinds of arms in bronze and steel:** longsword (parries 60% of a blow from in front), greatsword (two-handed, long reach), rapier (quick thrusts, a light parry), flanged mace and war hammer (break a shield's guard for 3 and 5 seconds; the hammer knocks back), glaive (sweeps at 4.25 blocks), halberd (thrusts through every target in line at 4.5), spear and lance (charge like vanilla's spears; the lance hits harder and unhorses riders).
- Each has its own swing (26.3's whack and stab, from 5 to 12 ticks), reach and in-hand size; holding use parries or charges. Every trait is a vanilla item component, so the server runs and checks them as it does its own weapons, with no per-tick code.
- 64x64 sprites, shared in-hand models, handbook pages, an advancement, three game tests and client screenshots. After studying Epic Knights and Simply Swords; nothing of theirs is used. Record: [arms.md](docs/features/arms.md).

### Unmerged: Control room, batch 37
- **Control Monitor:** six panels form a 3x2 wall screen. Cabled to a logic controller, it shows every channel's reading, a bar, a two-minute graph and ON/OFF.
- **Alarm Klaxon:** a controller switches it like a relay; it lights and sounds.
- **Control Remote:** bind it to a controller and flip a channel by hand from up to 256 blocks away.
- Advancement, handbook page, game test. Record: [control-electronics.md](docs/features/control-electronics.md).

### Unmerged: Tall sides on diagonal walls
- **Diagonal walls rise to meet what is above them.** When the block above covers all of a diagonal wall's sides and arms (another wall, a full block, a slab), they rise to the top of the block as a vanilla wall's sides do, so a diagonal wall two high has no slot between its layers. A tall straight diagonal run has no post, as a tall straight wall has none.
- One property, `tall`, on the diagonal walls: 2,048 states each, 32,768 more in all. Two more server game tests.

### Unmerged: Diagonal walls
- **Walls join diagonally.** All 32 of vanilla's walls join a wall a diagonal step away with a low wall side at 45 degrees, on the same rule as fences: neither may join straight into the corner between them, so a block in the corner keeps them apart.
- **Posts follow vanilla's rule.** A wall that runs straight on along a diagonal has no post, as a straight wall has none, unless something above calls for one (a torch, a block, a wall's post). Ends, corners and junctions keep their posts.
- **Swapped, not enlarged** (the Diagonal Fences mod's approach, in Jugcraft's own code). Vanilla's walls keep their own states. A wall that joins diagonally becomes `jugcraft:diagonal_<wall>` while it does, and turns back when its last diagonal goes. It drops, picks and is named as the vanilla wall, and is in `#minecraft:walls`. Its sides are low or none, never tall. This adds 32,768 block states instead of the 155,520 that diagonal properties on vanilla's walls would add.
- **Mobs** no longer try to step diagonally between two blocks joined diagonally, and a block with diagonals breaks into the particles of its shape without the arms.
- Shapes are worked out once for each set of straight sides, not once per state, and blocks with alike shapes share their diagonal shapes. This applies to fences, panes and bars too.
- Five more server game tests and three wall screenshots. Record: [diagonal-connections.md](docs/features/diagonal-connections.md).

### Unmerged: Control electronics, batch 36
- **Data Cable**, **Sensor**, **Relay** and **Logic Controller**. Sensors read how full a tank, battery, machine or chest is and report on a dye-colour channel. The controller's eight rules ("IF red above 90% THEN blue OFF") switch relays, whose redstone runs machines. It gives a dead band when two rules pair up.
- Advancement, handbook pages, game test. Record: [control-electronics.md](docs/features/control-electronics.md).

### Unmerged: Gas storage, batch 35
- **Gas Cylinder:** carries 8 buckets of one gas. Use it on a tank, pipe or machine to fill it, sneak to empty it; used in the air it tops up a scuba tank or grapple in the other hand. Machines and the fluid filter treat it like a bucket.
- **Ammonia Chiller:** freezes a bucket of water into ice with 5 mB of ammonia, anywhere (even the Nether), and presses four ice into packed ice and four packed ice into blue ice.
- Advancement, handbook pages, game tests. Record: [gas-storage.md](docs/features/gas-storage.md).

### Unmerged: Electroplating, batch 34
- **Electroplating Bath:** plates a tool, weapon or piece of armor with an ingot in sulfuric acid, repairing it fully without experience. **Nickel** makes it half as durable again, **silver** gives a sword or axe Smite III, **gold** makes armor count as gold for piglins. Plating again with the same metal repairs it again.
- Advancement, handbook page, tooltips, game test. Record: [electroplating.md](docs/features/electroplating.md).

### Unmerged: Hydroponics, batch 33
- **Hydroponic Bay:** grows a seed or cutting into a harvest every 30 seconds on power and **nutrient solution** (fertilizer in water, from the chemical reactor), giving the seed back. It needs no soil, sunlight or water source, so it works underground or in any dimension. It grows the vanilla crops, cotton and every agriculture crop.
- Advancement, handbook page, game test. Record: [hydroponics.md](docs/features/hydroponics.md).

### Unmerged: Construction chemistry, batch 32
- **Foam Sprayer:** fills up to 12 open blocks (air, water, lava, plants) where you aim, from 16 blocks away, with construction foam from foam canisters (chemical reactor: plastic and ammonia). Bridge gaps, seal caves, stop floods and lava. Respects spawn protection and the walled town.
- **Cement** (calcite or bone block, clay and sand, smelted) sets foam into **concrete**; concrete also crafts from cement, gravel and water. **Blast-proof concrete** (concrete round a rebar) is as blast-proof as obsidian. Both come as slabs and stairs.
- Two advancements, two handbook pages, a game test. Record: [construction-chemistry.md](docs/features/construction-chemistry.md).

### Unmerged: Diagonal connections (framework)
- **Fences, glass panes and bars join diagonally.** Two of a kind a diagonal step apart join with a rail, pane or bars at 45 degrees, as long as neither joins straight into the corner between them. This covers vanilla's 14 fences, 17 panes and 9 iron and copper bars, and all 14 Jugcraft fences, including the wrought-iron cemetery fence.
- Diagonals update live (placing, breaking, filling the corner), are part of the outline and collision, and turn with rotated or mirrored structures. Switch: `diagonal_connections.enabled`.
- The framework: four block-state properties on every fence and bars block, the tag `#jugcraft:connects_diagonally`, and a generator (`tools/diagonal_connections.py`) that gives any tagged block its 45-degree arm model from its own side model. Seven game tests and a client screenshot test. Record: [diagonal-connections.md](docs/features/diagonal-connections.md).

### Unmerged: Field chemistry, batch 31
- **Chemical grenades** for hand or launcher: **chlorine** (a cloud that hurts what breathes, through armor), **smoke** (mobs lose their target; players inside can't see), **thermite** (a burning pool that never lights blocks) and the **flashbang** (blinds players, staggers mobs, no damage). None breaks a block; other players only where PvP is on, never party members.
- **Gas mask:** keeps out chlorine, smoke and the flash; the filter wears in gas and is repaired with charcoal. A sealed scuba set also works while it has oxygen.
- **Medicines** from the chemical reactor: **first aid kit** (four hearts, 10 s cooldown), **antidote** (clears harmful effects, keeps good ones), **stimulant** (Speed II and Haste II for a minute, with hunger).
- Three advancements, three handbook pages, five game tests. Record: [field-chemistry.md](docs/features/field-chemistry.md).

### Unmerged: Pneumatic grapple, batch 30
- **Pneumatic Grapple:** a harpoon gun on compressed nitrogen (fill it at the air separation unit or a gas holder; 25 mB a shot). The hook flies up to 32 blocks: in a block it reels you in with no fall damage and a hop at the end; in a mob it drags the mob to you (not bosses or golems, never party members). Use it again to let go.
- Advancement, handbook page, four game tests, a screenshot. Record: [pneumatic-grapple.md](docs/features/pneumatic-grapple.md).

### Unmerged: Refinery upgrades, batch 29
- **Hydrotreater:** diesel + hydrogen → **premium diesel** (a quarter more power everywhere diesel burns) + hydrogen sulfide; gasoline + a tenth bioethanol → **premium gasoline** (448,000 JE a bucket in the gas turbine).
- **Sulfur recovery:** the chemical reactor turns 200 mB of hydrogen sulfide into a sulfur dust, so oil feeds the acid recipes.
- **Heat Recovery Unit:** set against a running diesel generator or gas turbine, it makes 30% of their power again from the exhaust, using water and a little lubricant.
- Two advancements, handbook pages, six game tests. Record: [refinery-upgrades.md](docs/features/refinery-upgrades.md).

### Unmerged: Room for the Retro Game Shop in every village (fix for #53)
- **Every new village gets its Retro Game Shop.** Village houses are built inside their street's plot, and some villages were laid out with no plot big enough for the 9 by 8 shop (roughly one village in thirty, going by CI runs and a model of the placer). Such a village is now laid out again, up to 8 layouts; a world seed still makes the same village.
- The village shop test now generates four rounds of the five village types (20 villages) instead of five. In the last round the shop is withheld from each village's first layout (a test-only switch), so every one of those villages must be laid out again and still get exactly one shop.

### The walled town and Jugs
- **A walled medieval town near the start of every new world**, 170–300 blocks away on the flattest dry ground: about 160 blocks across, with a stone curtain wall, 13 round towers and three gatehouses, a church with two spires, a market square with a fountain and six stalls, a bank, a town hall, and 44 half-timbered houses and shops. It is built into chunks as they load, levelled inside the wall and blended into the land outside.
- **Townsfolk:** 32 named, player-shaped townspeople in 18 original skins. Nobody can hurt them. They include shopkeepers, guards who fight monsters in town, strollers, a priest, the mayor, and decorators who change the town's decor for the seasons, Halloween, the Harvest Feast and December.
- **Protection:** players can't break or place blocks in the town, or use items on them; explosions, fire and pistons can't change it; hostile mobs don't spawn inside the wall. Operators in creative mode are exempt, and `town.protection=off` lifts it.
- **Jugs**, the town's credit, kept per player by the server: the General Store buys farm and mine goods, and the Seasonal Stall, Curiosities and the Florist sell decoration and fun items. **Jug Tellers** (ATMs) in the bank send Jugs to other players. There is no profit loop between the shops.
- Commands: `/jugcraft town`, `/jugcraft jugs`, and for operators `town place`, `town theme`, `jugs give|take`. Record: [walled-town.md](docs/features/walled-town.md).

### Unmerged: Parties finished (from #31)
- **Party screen** on the P key: members with online lights, the leader and you marked; LEAD, KICK, DISBAND or LEAVE; invite by name; accept or decline the latest invite. Every button runs the ordinary `/party` command.
- **Clickable [Accept] and [Decline]** on invites in chat.
- **`/party admin list | kick | leader | disband`** for operators (level 2), on any party.
- **Limits in the server config:** `parties.max_size`, `parties.invite_minutes`, `parties.invites_per_minute` (defaults 8, 5 and 10, as before).
- Commands and the shared party API are @Narvisius's from #31, already on main through #84.
### #53 Pixel Hollows and the Retro Trader
- **Pixel Hollows:** a rare cave biome deep under the driest land, lined with **circuitstone** and lit only by scattered, faintly glowing **pixel crystal clusters**, with an original chiptune hum. It holds 1.5× the usual copper and redstone (and tin). New building blocks: circuitstone, polished circuitstone, circuitstone bricks and the **pixel lamp**; clusters drop **pixel shards**. One mixin adds the biome to the Overworld (Fabric API has no Overworld biome API).
- **Retro Trader:** a villager profession at the new two-block-tall **arcade cabinet**. He sells a **Pixel Hollows Map** (use it to mark the nearest cave), circuitstone, lamps and shards, and buys shards back without any profit loop. Trades are 26.1+ data files.
- **Retro Game Shop:** a small storefront in every new village (one per village, all five village types; not zombie villages), with the cabinet and a villager inside.
- **Loot:** the cluster and cabinet use the 26.x loot format (#34 fixed the other tables on main), and new game tests check ore Silk Touch and Fortune and double slabs.
- **Dedicated-server check (CI):** a real client joins the game's own dedicated server, opens a machine and trades with the Retro Trader over the network, leaves and rejoins; and a world is saved and reopened with its trader, machine contents and cabinet intact. The two-client checklist is in [docs/TESTING.md](docs/TESTING.md#dedicated-server-and-two-clients).
- Seventeen game tests, eight client screenshots, a "Dead Pixels" advancement. Records: [pixel-hollows.md](docs/features/pixel-hollows.md), [retro-trader.md](docs/features/retro-trader.md).

### Fix: dyeing sky lanterns (pull request pending)
- **Sky lanterns take dye again.** Minecraft 26.3 dyes leather by a recipe per item, not by the `minecraft:dyeable` tag, so the lantern had no way to be dyed. It now has its own dyeing recipe (a lantern and any dye) and a water cauldron washes the dye out. A new game test checks both.

### Agriculture: fall additions 20, the Día de Muertos ofrenda (pull request pending, stacked on the theremin)
- **Ofrenda:** a three-tier home altar under an embroidered cloth. Set out up to six offerings; with flowers, a light, bread, a sugar skull and a drink it is complete: it glows and, at night, welcomes the restless spirits near, who come to it and show themselves, calm (Remembered).
- **Cempasúchil Marigolds** grow wild with the mums (orange dye, Marigold Petals to strew a path); **Papel Picado** flags for the wall; **Sugar Skulls**; and **Pan de Muerto** to bake.
- Meant as a respectful remembrance, not a fright. New server game tests and a client test with screenshots.

### Agriculture: fall additions 19, the theremin (pull request pending, stacked on wild turkeys)
- **Theremin:** a walnut cabinet on slender legs with a copper pitch antenna and volume loop, a speaker grille and a magic-eye tube, made with the Wire Drawer's copper wire and a note block.
- Switched on (or powered), it sings for the nearest creature within eight blocks: higher the nearer they come to the antenna, over two octaves with a vibrato, its eye glowing green (Good Vibrations).
- Comparators read how near the nearest creature is, playing or not, so it doubles as a proximity sensor. New server game tests and a client test with screenshots.

### Agriculture: fall additions 18, wild turkeys (pull request pending, stacked on the Spirit Board)
- **Wild Turkeys:** flocks of three to five come to forests, taiga, plains and meadows by day. Toms are bronze with a red wattle and a great chestnut tail; hens are brown. A tom with an audience struts: tail fanned, breast puffed, wings down, a gobble.
- Seeds and corn tempt and breed them (Gobble Gobble); hens lay eggs; each drops a raw turkey and feathers, and they flutter down instead of falling.
- **Roast Turkey:** roast a raw turkey in a furnace, smoker or on a campfire and set it on the table: six servings, eaten by hand or carved off with a Carving Knife (Carving the Bird), the drumsticks first, then the breast, then the carcass and a bone.
- New server game tests and a client test with screenshots.

### Agriculture: fall additions 17, the Spirit Board (pull request pending, stacked on pie baking)
- **Spirit Board:** a lettered talking board with a walnut planchette. By candlelight, up to four players rest their fingers on it; the nearest restless spirit answers YES and spells its name and the one thing it wishes for, then GOODBYE (Is Anybody There?). More hands, faster letters; no spirit, NO.
- Give a revealed spirit what it wished for (a pie, a candle, cider, a sweater, candy, an apple, a rose or a pumpkin) and it is laid to rest: experience and Luck (Unfinished Business).
- The planchette slides, eases and swivels over a 64 by 48 lettered face; comparators read YES and NO. New server game tests and a client test with screenshots.

### Agriculture: fall additions 16, pie baking (pull request pending, stacked on knitting)
- **Hearth Oven:** a brick bread oven fed coal, charcoal, coke or logs. It heats to 100 degrees and bakes a raw pie while at 50 or more: baked at 600 points (As Easy as Pie), burnt at 1,200. The pie shows in its mouth, going golden, then black. Comparators read it.
- **Pies:** Pastry Dough (wheat and an egg) with two of a filling and sugar makes a raw Apple, Pumpkin Cream, Cranberry, Sweet Potato or Chestnut pie. Baked, a pie is placed like a cake and eaten or cut with a Carving Knife a slice at a time; a Burnt Pie is barely food.
- 26.3 has no `Level.fuelValues()`, so the oven burns the generators' fuels and a log tag rather than vanilla's furnace fuel list. New server game tests and a client test with screenshots.

### Agriculture: fall additions 15, knitting (pull request pending, stacked on the Hay Golem)
- **Spinning Wheel:** put a skein of wool on the distaff and work the treadle (or pulse it with redstone): four turns spin it into four balls of yarn in its colour, set out where a hopper can take them. It unravels knitwear back into yarn, less a ball.
- **Knitting Needles:** with yarn in the other hand, knit a row at a time into a Knit Beanie, Wool Socks, or one of five sweaters (plain, striped, pumpkin, bat, autumn leaf), coloured the blend of its rows (Knit One, Purl Two). Sneak to change project or unpick.
- **Knitwear** is worn and shows in its colour, keeps out powder snow, and takes dye (and washes clean in a cauldron) as leather does in 26.3. Two pieces by a lit campfire make you cosy (Regeneration I); a beanie, sweater and socks earn Snug as a Bug.
- The checker's recipe audit knows 26.3's dyeing recipes. New server game tests and a client test with screenshots.

### Agriculture: fall additions 14, the Hay Golem (pull request pending, stacked on the Bat House)
- **Hay Golem:** build a T of four hay bales and put a carved pumpkin (or jack o'lantern, or a hand-carved pumpkin with a face) on top, and it comes to life (Man of Straw).
- A walking scarecrow: crows keep off crops within eight blocks of it, twelve with a lit head.
- It tends the ripe crops round its post, replanting them from their drops, and carries the harvest to the chest, barrel or hopper under its post. Lead it with wheat to move its post.
- Wheat heals it; shears take it apart again; fire hurts it double.
- All decided on the server; the client draws its head and carving. The checker compares its numbers with Java. New server game tests (one walks it to a crop with its own AI) and a client test with screenshots.

### Agriculture: fall additions 13, the Bat House (pull request pending, stacked on autumn foraging)
- **Bat House:** a slatted roost to hang on a wall. Bats roost in it by day and pour out at dusk (Night Shift); at dawn the nearest bats come back in, up to four, each leaving a guano on its tray. A house with room gains a bat at dusk now and then. Scoop the guano with an empty hand; comparators read the bats.
- **Bat Guano:** fertilizes the crops in a 3x3 patch (a dose of bone meal each), and four make a phosphate.
- `FertilizerItem` takes its area and doses, so superphosphate and guano share one rule.
- All decided on the server. The checker compares the house's numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 12, autumn foraging (pull request pending, stacked on the candy kitchen)
- **Wild mushrooms:** chanterelles, porcini, puffballs, fly agarics and the glowing jack o'lantern mushroom grow in patches on forest floors (each in its own biomes), on soil. They spread in the shade, up to five of a kind together; bone meal spreads them in any light.
- **Fairy rings:** on a full-moon night a mushroom may sprout a ring of its kind round it. Stand in a ring's centre on a full-moon night for Luck II (once a night) and Away with the Fairies.
- **Foraging Basket:** a wicker bundle for mushrooms, berries, nuts and wild fruit. Mushrooms picked with it in hand go straight in; all five earn Forager.
- **Dishes:** Sautéed Chanterelles, Roasted Porcini, Fried Puffball, and Forager's Stew from the Cooking Pot.
- All decided on the server. The checker compares the spreading, rings, soil and biomes with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 11, the candy kitchen (pull request pending, stacked on #33)
- **Candy Kettle:** a copper sugar pot with a candy thermometer. Fill it before it boils with a water bottle (syrup) or milk (cream), up to four sugar, up to two flavours (chocolate, berry, glow berry, honey, cranberry, spiced or chestnut) and any dyes. Over a fire it climbs through the candy stages, ringing a bell at each; the hottest it reaches decides the candy, so taking it off the heat holds it.
- **Candy Tray:** pour onto it, and break the candy up once set. Syrup makes rock candy (grown for a day), candy corn (poured in up to three coloured layers), salt water taffy (pulled four times while warm, or it sets hard), hard candy and lollipops, and caramel; cream makes fudge, cream caramels and toffee. Too hot burns it.
- Flavoured candy is named for its flavours and gives their short effects when eaten. All candy counts as candy for Candy Bowls and Bags.
- All decided on the server; the client draws the syrup and the thermometer's needle. The checker compares the stages, rates and flavours with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 10, face paint (pull request pending, stacked on ghost hunting)
- **Face Paint Kit:** a tin palette and brush, good for 16 faces, that paints one of six designs: a skull, a jack o'lantern, a black cat, a vampire, a witch or a scarecrow. Use it on a friend to paint them at once, hold use to paint yourself, sneak to turn the dial.
- The paint shows on the face for everyone who can see you and lasts until your head goes under water, or you die. A painted face counts as a costume for trick-or-treating and the costume contest.
- The paint is a Fabric data attachment on the player, set on the server and sent to the clients that see them. The checker compares the kit and designs with Java. New server game tests and a client test with a screenshot of each design.

### Agriculture: fall additions 9, ghost hunting (pull request pending, stacked on the corn maze)
- **Restless spirits** rise from gravestones and grave mounds at night (graves now take random ticks), a few at most near, and drift about their graves, unseen.
- **Spirit Lantern:** held in either hand, it reveals every spirit within 12 blocks to everyone near; so does a Revealing candle's glow. A revealed spirit fades into view, moans, and shies away from anyone close, though never out of its haunt, so it can be cornered. Blows pass through it; it fades at dawn.
- **Ectoplasm:** a glass bottle catches a revealed spirit (earning Ghost Hunter). Ectoplasm is the chandlery's new **Ghostly** scent: a Ghostly candle turns the players in its aura invisible, and the wax pot now hands back a scent's bottle.
- All decided on the server; clients are told only when a spirit shows, and draw it. The checker compares the spirits and the lantern with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 8, the corn maze (pull request pending, stacked on the Harvest Feast Table)
- **Corn Maze Gate:** choose a size (tiny 7 by 7 to large 19 by 19) and use it holding corn kernels: it carves a new maze, one way through, and plants three-tall **maze corn** along its walls, a kernel a stalk, with a **finish post** at the exit. It never replaces a block.
- **Running it:** walk out through the gate to start the clock and reach the finish post to stop it. The server follows every runner and voids a run for flying, climbing over the corn, leaving the maze, taking too long or a shortcut. The best times go on the gate's board, with a prize ribbon the first time a runner places and the advancement A-maze-ing.
- Maze corn gives back its kernel. The checker compares the maze with Java. New server game tests (perfect mazes, planting, timed and voided runs, the corn) and a client test with screenshots.

### Agriculture: fall additions 7, the Harvest Feast Table (pull request pending, stacked on the sky lantern festival)
- **Harvest Feast Table:** a long trestle table built a length at a time; lengths end to end join into one table. Each length holds two dishes of up to eight servings of any food or drink, drawn heaped on their plates; eat a serving with an empty hand.
- **The feast:** different foods on the table plus everyone who has eaten there in the last two minutes. A good meal gives Regeneration, a feast Absorption, a harvest feast Haste and Luck, and a grand feast Health Boost and the advancement Harvest Home, shared with every recent diner close by.
- All decided on the server; clients only draw the dishes. The checker compares the table and its tiers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 6, the sky lantern festival (pull request pending, stacked on spooky fireworks)
- **Sky Lanterns:** paper lanterns, dyed any colour and named in an anvil to carry a wish. Let one go and it rises glowing, drifting on a wind every lantern shares, so lanterns let go together drift together; it burns out after two minutes or so. A blow puts one out.
- **The lantern festival:** eight lanterns let go within 32 blocks in two minutes, by one player or many, fill the sky: everyone near gets Luck for five minutes and the advancement A Sky Full of Wishes. Once a day in one place.
- **Mooncakes** (red bean, chestnut, pumpkin), baked four at a time in the Cooking Pot. Eaten outdoors on a full-moon night, they give Luck too.
- All decided on the server; clients only draw the lanterns. The checker compares the lanterns, festival and mooncakes with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 5, spooky fireworks (pull request pending, stacked on crows and working scarecrows)
- **Spooky fireworks** that burst into pictures drawn in coloured sparks: a **bat**, a **jack o'lantern**, a **ghost** and a **skull**. Every player sees the picture the right way round, since each client draws it facing them. Crafted from paper, one to three gunpowder and the picture's ingredients; glowstone dust makes them twinkle. They hurt and break nothing, and dispensers fire them.
- **Show Launcher:** nine tubes of up to sixteen rockets each (spooky or vanilla), fired in sequence, in volleys of three or as a finale of all nine at once, fanned out across the sky. Start and stop it by hand or with redstone; hoppers can load it.
- All decided on the server; a burst is one small message to the players who can see it, and each client draws the sparks. The checker compares the rockets, launcher and pictures with Java and checks all 24 rocket recipes. New server game tests and a client test with screenshots.

### Agriculture: fall additions 4, crows and working scarecrows (pull request pending, stacked on the preserves pantry)
- **Crows** come to fields by day in flocks of two or three. They wheel over the field, cawing, then drop onto a ripe crop and peck it three growth stages back (only while the `mob_griefing` rule is on). Crops under a roof, tall crops, gourds and bushes are safe.
- **Scarecrows now work:** crows leave the crops within 4 blocks of one alone, 8 when it wears a pumpkin head, 12 when the head is lit. A crow after a crop takes flight when a scarecrow goes up beside it.
- Crows also fly off from a player who comes close (sneak to get closer), from a blow, and at nightfall. They drop feathers, for arrows and the Featherfall candle scent.
- All decided on the server; spawning follows the `spawn_mobs` rule, and nothing loads a chunk. The checker compares the crows' and scarecrows' numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 3, the preserves pantry (pull request pending, stacked on the cider mill)
- **Preserves in Mason Jars**, cooked in the Cooking Pot: sweet berry jam, apple butter (cooked down in cider), pumpkin butter (with mulling spices), cranberry preserves, glow berry jelly (Night Vision), and pickled beets, pickled peppers (Fire Resistance) and corn relish in new **Cider Vinegar**. A jar holds four servings, eaten one at a time; the last leaves the jar.
- **Unsealed jars spoil** three days after they were cooked. The **Canning Kettle** seals them: fill it with water, set it over a fire, stand up to four fresh jars in it, and twenty seconds at a rolling boil seals them. Sealed jars keep until opened, stack, and wear a gingham cap.
- **Pantry Shelf:** an open cupboard that shows off six jars.
- All decided on the server; the kettle's water and jars and the shelf's jars are drawn by each client. The Cooking Pot now stamps a jar with when it was cooked. The checker compares the pantry with Java. New server game tests and a client test with screenshots.

### Agriculture: fall additions 2, the cider mill (pull request pending, stacked on the chandlery)
- **Apple trees:** wild in plains and flower-rich places, or grown from **apple seeds**. Their leaves blossom and then hang with ripe apples to pick, about a Minecraft day apart, without the tree being cut down.
- **Cider Press:** load up to eight apples and turn the crank to grind them, one at a time, into pulp. Then turn the screw: four turns press the juice into the trough (a serving an apple) and knock out the pomace. Bottle the juice as **Sweet Cider**.
- **Cider Barrel:** pour sweet cider in; in a day it ferments into **Sparkling Cider**, in three it matures into **Aged Cider**, and a chalk mark on the barrel shows which. Broken, it keeps its cider, still ageing.
- **Mulled Cider** (sparkling cider and mulling spices in the Cooking Pot), **Apple Cider Donuts**, and **Apple Pomace** for pigs, compost and seeds. Each cider gives a short effect: Haste, Jump Boost, Absorption, Regeneration.
- All decided on the server; the press's apples, pulp, screw and juice are drawn by each client. The chestnut tree's fruiting now shares its code with the apple tree. The checker compares the press, barrel and tree with Java, and makes sure no bottle is made from nothing. New server game tests and a client test with screenshots.

### Agriculture: fall additions 1, the chandlery (pull request pending, stacked on batch 14)
- **Wax Melting Pot:** a copper pot set over a fire. Melt honeycomb (beeswax) or rotten flesh (tallow) in it, then stir in dyes (mixed as on leather), up to two scents, glowstone dust (a stronger aura, a faster burn) and redstone (a longer burn).
- **Aura Candles:** dip string in the wax to start a candle, and dip it again once each layer has cooled (dipped while warm, the layer slides off), up to four layers. Each layer makes it taller and brighter, widens its aura (5, 8, 12, 16 blocks) and adds its wax's colour, scents and burn time. Layers of different pots combine their scents; a third scent muddles the candle.
- **Lit, a candle works like a small beacon:** every four seconds it gives everyone in range its scents' effects (Speed, Jump Boost, Night Vision, Slow Falling, Water Breathing, Fire Resistance, Haste or Regeneration), or wards off monsters, makes creatures glow, or speeds up crops. It burns down as it burns and goes out for good; broken, it keeps what is left.
- All decided on the server; each client draws the wax, the candle's colour and height and its tinted flame. The checker compares the waxes, scents and numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 14, costumes (pull request pending, stacked on batch 13)
- **Six outfits**, worn on the head and drawn over the whole body: a **Vampire Cape** that flares as you walk and wraps round you when you sneak, **Mummy Wraps**, a **Skeleton Suit** whose bones glow in the dark, a **Werewolf Mask** with fur, claws and a tail, **Cat Ears and Tail** (the tail sways), and **Bat Wings** that spread and flap when you jump.
- Each counts as a trick-or-treat costume (and for the costume contest); costumed mobs wear them during the event.
- **Costume Trunk:** keeps nine costumes; use it with an empty hand to change into the next one.
- Changing is decided on the server; how outfits move is drawn by each client from boxes generated with their textures. The checker compares the outfits, the trunk and the boxes with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 13, treats (pull request pending, stacked on batch 12)
- **Witch's Brew Punch Bowl:** brew glowing green punch from berries, under rolling dry-ice fog, and ladle it into bottles; a drink makes you glow.
- **Soul Cakes** and the **Barmbrack:** a fruit loaf eaten a slice at a time, one slice hiding a ring; every slice tells a fortune.
- **Pumpkin Spice Latte** (a burst of Speed), **Pumpkin Bread**, **Spiderweb Cupcakes** and **Bat-Wing Cookies**; the cakes, cupcakes and cookies count as treats for Candy Bowls and Candy Bags.
- **Giant Candy:** block-sized props of candy corn, a lollipop, a wrapped sweet and a gumdrop.
- All decided on the server; nothing ticks. The checker compares the bowl's servings, the barmbrack's slices and fortunes, the candy's designs and the drinks' effects with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 12, night events (pull request pending, stacked on batch 11)
- **Trick-or-treaters at your door:** during the Halloween event, village children in costume knock at a Candy Bowl by a lit door; a full bowl earns thank-you gifts, an empty one gets your trees toilet-papered.
- **Toilet Paper Rolls:** throw them over trees and fences; the streamers wash off in the rain.
- **Haunted Hayride:** a four-seat hay wagon on rails; at night its riders hear spooky things from the dark.
- **Halloween Bonfire:** cooks what a campfire cooks, four at a time and twice as fast; toast **Marshmallows** on a stick over it (or a campfire), but not too long.
- All decided on the server; the flames, skewered food and wagon are drawn by each client. The checker compares the numbers, costumes and gifts with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 11, party games (pull request pending, stacked on batch 10)
- **Jump-Scare Trap:** a crate that bursts open and throws up a shrieking ghost on a spring when someone walks up, or a tripwire fires.
- **Costume Contest:** walk the **Costume Runway** in costume while the **Judges' Table** has a round open; everyone else votes by using their favourite, and the most votes win a **Best Costume Ribbon**.
- **Pumpkin Bowling:** roll a **Bowling Pumpkin** down a lane of **Skeleton Pins**; the **Bowling Scoreboard** keeps ten-pin score and stands the pins up again.
- **Candy Cache:** a hollow stump that hides treats like a Candy Bowl, one a night for each finder.
- **Monster Mash Dance Floor:** tiles light up in pulsing Halloween colours from a playing jukebox or redstone; villagers on them dance.
- **Ghost Tag:** ring the **Ghost Bell**; whoever is the ghost glows and tags others by hitting them, harmlessly, with no tag-backs.
- **Fortune Teller's Table:** a tarot card turns, the planchette slides to YES, NO or GOODBYE, and you get one of twenty silly fortunes.
- All decided on the server; the moving parts are drawn by each client. The checker compares the numbers and messages with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 10, lighting and glow (pull request pending, stacked on batch 9)
- **Black Light** and **Glow Paint:** paint skulls, bats, spiders, webs, handprints and eyes on any face; they blaze green-white under a black light nearby.
- **Witch Fire Brazier:** a brazier whose flame turns orange, green, purple or blue with a dye; it burns nothing.
- **Shadow Puppet Lamp:** its turning paper shade throws a bat, a cat and a witch round the walls of the room.
- **Mini Pumpkin Stack** and **Floating Witch Hat:** candle-lit, lit and snuffed like candles; the hats bob and turn in the air.
- The glow, flames, shade, shadows and hats are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 9, the yard and porch (pull request pending, stacked on batch 8)
- **Yard Inflatables:** a ghost, a black cat, a pumpkin stack and a spider, two blocks tall, that fill up on a click or redstone, wobble and glow, and sag flat when switched off.
- **Animatronic Porch Witch:** stirs her bubbling pot and follows you with her eyes; walk up and she throws her head back and cackles.
- **Grasping Hands:** rotting hands in a mound of dirt that snatch at the ankles of anything that steps on them (a short, harmless Slowness II); sneak past.
- **Poseable Skeleton:** use it to pose it sitting, waving, lounging or hanging.
- **Bone Wind Chimes:** bones and a little skull under a porch roof that swing and clack, more and louder in rain and storms.
- **Bat and Witch Weathervanes:** turn to point into one wind shared by the whole world, swinging about in storms.
- **Spooky Sign:** painted warnings (BEWARE, KEEP OUT, TURN BACK...) or your own words from a Name Tag or an anvil.
- **Haunted Archway** and **Dead Hollow Tree:** lantern-lit props of several blocks, placed and broken as one.
- The figures, the witch's arm and head, the chimes, the vanes and the sign's words are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 8, the mad scientist and monsters (pull request pending, stacked on batch 7)
- **Tesla Coil:** a two-block coil on the electric network (20 JE a tick) that hums, glows and throws harmless violet arcs to other running coils nearby.
- **Lab Table:** a two-block operating table whose sheeted patient sits bolt upright on a redstone signal, and twitches at night.
- **Specimen Jar:** glowing green fluid with an eye, a tentacle, a tiny pumpkin or a brain bobbing in it.
- **Mummy Sarcophagus:** a click or redstone and the lid grinds open, the mummy lurches out with its arms up, then goes back.
- **Raven on a Perch:** watches the nearest player, ruffles and croaks, caws and flaps when used.
- **Black Cat Figure:** swishes its tail, its eyes glow at night, and it arches its back and hisses at anyone who runs past.
- Arcs, the patient, the specimens, the lid and mummy, the raven and the cat's tail and eyes are drawn by each client. The checker compares the numbers with Java. New server game tests and a client test with screenshots.

### Agriculture: more Halloween, batch 7, the haunted house inside (pull request pending, stacked on decorations batch 6)
- **Haunted Chandelier:** eight candles on an iron ring that sways on its chain; at night a draft blows them all out and they relight one by one.
- **Phantom Pipe Organ:** a three-by-two organ, one prop, that plays the opening of Bach's Toccata and Fugue in D minor on a click or redstone, its keys going down by themselves; at night it sometimes plays alone.
- **Suit of Armor:** two blocks of plate on a stand whose helmet slowly turns to watch the nearest player; a red glow in its visor at night.
- **Dust Sheet:** drape it over a chair, stair, slab, chest or bookshelf; it keeps what is under it (a chest's contents too) until you pull it off. At night some sheets seem to breathe.
- **Spirit Mirror:** at night a pale face fades into the glass now and then.
- **Tattered Curtains:** cheesecloth drapes up to eight blocks long that open and shut together and sway in a draft.
- **Creepy Doll:** its head never moves while you watch, but it has turned every time you look back.
- The sway, flames, keys, helmet, sheets, face, curtains and doll's head are drawn by each client. The checker compares the numbers and the organ's tune with Java. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 6, the haunted house and yard (pull request pending, stacked on batch 5)
- **Rocking Chair:** sit in it and it rocks under you; at night, empty, it rocks on its own and creaks.
- **Lurking Eyes:** glowing eyes that peer out of a hedge at night, blink, and vanish when you come within four blocks.
- **Silhouette Window:** a bat, black cat or witch cut-out in orange paper that glows when a lamp lights the far side.
- **Spooky Music Box:** plays an original waltz on note-block sounds while powered by redstone, or once when wound by hand.
- **Giant Fake Spider:** a big hairy spider swaying on a silk thread from a ceiling, branch or cobweb; let its thread out up to four blocks.
- The chair's rocking, the eyes, the windows' glow and the spider's sway are drawn by the client. The checker compares the numbers and the tune with Java. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 5, the harvest party (pull request pending, stacked on batch 4)
- **Bobbing for Apples Tub:** drop apples in, then duck for one with an empty hand: one try in three catches an apple, and the tub splashes a moment between tries.
- **Pumpkin Crate:** shows up to four of your pumpkins, squash, gourds or melons.
- **Hay Bale Seat:** sit on it; it softens falls like a hay block.
- **Autumn Wreath:** chestnut leaves, ornamental corn and mums, for walls and doors; a mum changes its flowers.
- **Leaf Piles:** red, orange and yellow, heaped up to four layers; they soften falls and kick up leaves underfoot.
- The checker compares the numbers, colours and tags with Java and checks every state has a blockstate entry. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 4, the witch's cottage (pull request pending, stacked on batch 3)
- **Bubbling Cauldron:** fill it with a water bucket, then a spider eye, nether wart or glowstone (among others) turns it into a glowing green, purple or orange brew; over a fire it bubbles and steams. An empty bucket pours it out.
- **Apothecary Shelf:** wall shelves of corked jars, tinctures, a little skull and a candle; sneak-use to set them out four ways.
- **Crystal Ball:** a violet-misted orb on a gilt stand; gaze into it and it flares and tells you one of ten fortunes.
- **Grimoire Stand:** an open spellbook on a carved stand; use it to turn through four spreads. Its pages glow faintly and give off motes at night.
- **Witch's Broom:** a twig besom leaning on its bristles.
- The checker compares the numbers, fortunes, spreads and brew tags with Java and checks every state has a model. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 3, the graveyard (pull request pending, stacked on batch 2)
- **Wrought-Iron Cemetery Fence and Gate:** spear-topped iron pickets on finialed posts; a real fence and fence gate (vanilla's fence and gate blocks and tags).
- **Crypt set:** Crypt Stone, Chiseled Crypt Stone (a carved skull), Crypt Stone Pillars (also from the stonecutter) and a stone Crypt Door that opens by hand.
- **Grave Mound:** walk past and a zombie hand claws up out of the earth for three seconds; sneak past and it stays down; redstone holds it up.
- **Mourning Angel:** a two-block marble statue, head bowed into its hands; at night it weeps.
- **Pop-Up Skeleton:** a crate whose skeleton springs out at passers-by (or on a redstone signal).
- The checker compares the scare props' timings with Java, checks every state has a model and that the fence, gate and door are in vanilla's tags. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, batch 2 (pull request pending, stacked on the first five)
- **Luminaria:** a paper bag weighted with sand round a candle, a jack-o'-lantern face cut in its sides. Lit like a candle (light 10); any dye colours it, and it keeps its colour when broken.
- **Floating Candles:** up to four candles hanging in the air, bobbing gently (drawn by the client); 3 light a candle while lit.
- **Skeleton Hand Sconce:** a torch held out from a wall by a bony hand (light 14); snuff it by hand, relight it with flint and steel.
- **Soul-Flame Carvings:** a soul torch lights hand-carved and giant pumpkins with an ice-blue glow (at most light 10) and comes back out as a soul torch. Hand-carved pumpkins gain a `soul` block state (old worlds load with a plain candle).
- **Bat Bunting:** orange and black pennants and paper bats strung between String Light Hooks like the string lights; hooks remember which strand they hold.
- The checker compares the new blocks' numbers and the floating candles' places with Java, checks every state has a model, that the bag has a face cut through its walls and a whole inside (so the face shines with the candlelit far wall), and that the soul-lit icon uses the client's soul colours. New server game tests and a client test with screenshots.

### Agriculture: Halloween decorations, the first five (pull request pending, stacked on Halloween nights)
- **Jack-o'-Lantern String Lights** strung between **String Light Hooks** (up to 16 blocks; hooks chain); a hook lights from redstone or 1 JE a tick from the electric network, and a strand glows while either end is lit.
- **Candy Bowl:** fill it with treats; each visitor takes one a night (saved on the server), its owner any time.
- **Coffin:** a two-block 27-slot chest whose lid lifts while it is open, and a bed you sneak-use to lie down in and set your spawn (it refuses where beds explode).
- **Haunted Portrait:** four sitters; the pupils follow each player's camera and glow red at night.
- **Fog Machine:** switched on by hand or redstone, it uses 16 JE a tick and rolls ground fog over a 4–16 block radius, drawn by clients with a per-tick cap.
- The checker compares the decorations' numbers and the portraits' eyes with Java and the textures, and checks every block state has a model and every result its message. New server game tests and a client test with screenshots.

### Agriculture: Halloween nights (pull request pending, stacked on the Halloween festivities)
- **Will-o'-wisps** (event nights): glowing wisps drift over swamps and cornfields and flee when you come near (sneak to get closer). A glass bottle catches one in a **Wisp in a Jar** (a lantern, light 13). Advancement **Bottled Light**.
- **Pumpkin Chunkin' Trebuchet** (all year): load a pumpkin, set the release angle (30°–60°), let fly about 50 blocks. The server measures where it lands; a marker shows the distance; a board of the three longest throws gives the Harvest Scale's ribbons once per thrower. Advancement **Pumpkin Chunkin'** at 50 blocks.
- **Candy Bag:** now holds treats like a bundle (candy and cookies only); trick-or-treating fills it, and its tooltip shows tonight's homes.
- **The Harvest Moon:** on the nights of 31 October (new option `halloween.harvest_moon`) during the event, crops and giant pumpkins grow twice as fast and lit carvings throw sparks.
- **The Headless Horseman:** summoned near midnight during the event at a Scarecrow with a lit pumpkin head. A boss with a boss bar, charges and flaming pumpkins that never break blocks; enraged at half health; keeps to his arena; rides off with nothing at dawn or when the event ends. Defeated, he drops the **Horseman's Lantern** and **Horseman's Cloak**. Advancement **Lost His Head**.
- Wisp spawning and sky checks use heightmaps (no light lookups). The checker compares the wisps, trebuchet, Harvest Moon and Horseman numbers with Java, checks every trebuchet state has a model, the ammunition tag matches the throwing factors, the Horseman's loot is player-kill only, every failed summoning has a message, and that no block model face reads outside a see-through texture (26.3 refuses to bake those). New server game tests (including the seasonal rules) and a client test with screenshots.

### Agriculture: the Halloween festivities (pull request pending, stacked on the pumpkin regatta)
- **The carving contest:** put a hand-carved pumpkin on a **Judging Stand** and enter it (its carver only). While the Halloween event runs, every player has one vote per Halloween for someone else's carving; sneak-use a stand for the standings. When the event ends, the top three carvers get the Harvest Scale's ribbons, once (offline winners on their next visit). Votes and prizes are saved with the world.
- **Costumed mobs** (during the event): 15 % of zombies, husks, skeletons, strays and zombie villagers wear a costume hat or carved pumpkin, and drop a sweet when a player kills them.
- **The Halloween Peddler** (during the event): wandering traders arrive in a Witch Hat with four extra Halloween wares for emeralds, from a data-driven trade set.
- **Decorations, all year:** **Rounded**, **Cross** and **Obelisk Gravestones** (stonecutter) engraved with a named Name Tag's name; **Spun Cobweb** (no slowing); **Hanging Ghost**; **Candle Skull** (lights like a candle).
- **Spooky sweets** from the Cooking Pot, eaten even when full: **Glow Gum** (Glowing), **Ghost Taffy** (3 s of invisibility), **Fizz Rocks** (Jump Boost), **Witch's Licorice** (Night Vision).
- The checker compares the contest, costumed mobs, Peddler, gravestones and sweets with Java, checks the Peddler only sells things with another route, and checks `villager_trade` tags. New server game tests (including the seasonal rules: on, off, a restart, no duplicate prizes, earned ribbons kept) and a client test with screenshots.

### Agriculture: the pumpkin regatta and trick-or-treating (pull request pending, stacked on the Halloween harvest)
- **Pumpkin boats:** sneak and use the Carving Knife on top of a giant pumpkin to hollow it out. A full-grown 3×3×3 one becomes a **Pumpkin Barge** (four seats; keeps its weight, carving and torch), a 2×2×2 one a **Pumpkin Racer** (one seat). Lighter boats are faster: racers 1.15–1.30× a boat, barges 0.70–0.95×. Broken, a boat gives back its item with everything it kept. Hollowing gives the guts and (full grown) the giant seeds instead of the pumpkins.
- **The regatta:** a **Regatta Flag** and numbered **Regatta Buoys** on the water make a course. From a pumpkin boat's driver's seat, use the flag: after a countdown, round the buoys in order and come back. The server times the run; the flag keeps a board of the three best times and gives the Harvest Scale's ribbons once per racer. Advancement **Pumpkin Regatta**.
- **Trick-or-treating**, only while the Halloween event runs (by default 20 October to 3 November; operators set `halloween.start`, `halloween.end`, `halloween.timezone` and `halloween.mode` in `config/jugcraft.properties`): use a **Candy Bag** on a villager's door between dusk and midnight, in costume, with a porch light by the door. Each home gives each player one treat a night (candy, caramel, cookies, popcorn balls, caramel apples, rarely a **King-Size Candy Bar**); knocking again gets a harmless prank. Ten homes in a night earn **Full Bag**. Who got what tonight is saved, so a restart or the event ending and starting again gives no second treat; treats and costumes stay after the event.
- **Costumes:** **Witch Hat**, **Ghost Sheet** (draped over the whole wearer, hood to knees, moving with them; you look out through its eye holes) and **Scarecrow Hat**, worn on the head. Hand-carved pumpkins can now be worn like vanilla's carved pumpkin (and fool endermen).
- The checker compares every boat, regatta and trick-or-treat number, tag and table with Java, and allows vanilla tab roots as advancement parents. New server game tests (including the seasonal rules: on, off, a restart, no duplicates, earned treats kept) and a client test with screenshots.

### Agriculture: the Halloween harvest (pull request pending, stacked on Pumpkin Carving)
- **Giant pumpkins:** Giant Pumpkin Seeds plant a vine that sets one fruit and grows it to 2×2×2, then 3×3×3 (faster on moist farmland and when watered; bone meal grows the vine, sets the fruit and feeds it). Full grown it weighs 100–120 kg and gains weight up to 1000 kg until carved. It is one prop: breaking it picks it up whole as a **Giant Pumpkin** item (keeping its size, weight and carving) that places the same pumpkin back; an axe chops it into 9 pumpkins and 1–3 giant seeds. Pistons can't move it.
- **A 48×48 carving face** on each side of a full-grown giant, carved with the Carving Knife through the same screen (smaller cells, bigger brushes, starter faces blown up); a torch lights every block of it.
- **Scooping:** the first cut into a pumpkin also gives 1–2 **Pumpkin Guts** (Pumpkin Soup in the Cooking Pot) and sometimes a giant pumpkin seed.
- **Harvest Scale:** weighs the giant beside it, keeps a board of the three heaviest, gives First, Second and Third Prize Ribbons (trophies) once per pumpkin, and drives a comparator.
- **Pumpkin stencils:** trace a carved side onto a Blank Stencil; hold the stencil in your other hand while carving to press it in.
- **Heirloom pumpkins:** White, Jarrahdale and Cinderella, grown from stems, found wild, carvable into their own hand-carved blocks, and baked into pumpkin pie.
- **Scarecrow** (dye its flannel shirt; it wears any pumpkin on its shoulders for a head, like an armor stand, carving and all, and a lit one lights it), **ornamental corn** (a tall crop; its ears tie into an **Ornamental Corn Bundle** for walls), **Corn Stalks** from tall corn and the **Corn Shock**.
- **Caramel**, **Caramel Apple** (you keep the stick) and **Popcorn Ball**.
- **Bottle Gourd** (found wild), dried into a **Gourd Birdhouse** (stands or hangs) or a **Gourd Canteen** (3 sips of water for giant pumpkins, farmland, fire or a cauldron).
- **Mums** in four colours: flowers for pots, dye and suspicious stew, in wild patches.
- Short grass now drops 24 kinds of seed, still 12.5 % overall. The carving payloads now carry a face size (16 or 48), checked before anything is read. The checker compares the giant pumpkin, scale, canteen, mum and heirloom numbers with Java and checks every giant and scarecrow state has a model. New server game tests and a client test with screenshots, which also carves a giant from a stencil through the screen.

### Agriculture: Pumpkin Carving (pull request pending, stacked on the Festival Crops)
- **Carving Knife** (an iron ingot over a stick): used on the side of a pumpkin, it opens a **16×16 carving screen** for that side, at Minecraft's own pixel size. Cut through, shave the skin or erase; brush sizes 1–3, mirror, four starter faces (Classic, Cat, Ghost, Spooky), undo (Ctrl+Z), reset, a candle preview and an actual-size preview.
- **Hand-Carved Pumpkin:** the first cut lets out the seeds (as shears do) and turns the pumpkin into one that keeps a face on each of its four sides. A knife can't put skin back: carving only goes deeper.
- **Light:** a torch inside lights it, from 4 up to 15 the more is carved out; an empty hand takes the torch back. Broken, it drops with its design and its torch.
- **Roasted Pumpkin Seeds** from the furnace, smoker or campfire.
- **Server checks:** the server checks every carving (the session from using the knife, the knife in hand, reach, build permission, a valid face that only goes deeper). Carvings record their last carver for operators, and `carving.free_draw=false` in `config/jugcraft.properties` allows only the starter faces.
- The checker also verifies the carving numbers against Java, the starter faces and every refusal message, and understands select item models. Ten new game tests, and a client test that carves through the real screen with the mouse and keyboard (after the owner's first play found that clicks on the grid did nothing: 26.3 numbers mouse buttons from 1, and the screen used the old numbers).

### Agriculture: Festival Crops (pull request pending, stacked on the Kitchen Garden)
- **Gourds on stems:** butternut squash, acorn squash and warty gourds grow from stems on farmland and place their gourd beside them, like pumpkins. Stems follow Jugcraft's growth rules, so squash next to beans grows 1.5× as fast. Gourds are blocks for fall displays; 1 gourd → 4 seeds.
- **Turnips**, and the **Turnip Lantern** (a turnip over a torch): a carved turnip that gives light 13, the original jack-o'-lantern.
- **Cranberries**, a bog crop: the bush stands in a water source one block deep over bog soil (block tag `jugcraft:bog_soil`), grows only with open air above, keeps its water when broken, and is picked like sweet berries.
- **The chestnut tree:** a chestnut plants a sapling that grows into a broad tree. Its leaves grow burs that ripen over about a day and are picked with a right-click, without cutting the tree. A chestnut wood set (logs, wood, stripped, planks, stairs, slab, fence, fence gate): any axe strips it, the sawmill saws a log into 6 planks, and it joins vanilla's wood tags, fuel and fire rules.
- **Food:** roasted chestnuts, baked acorn squash, squash pie, candy corn; in the Cooking Pot, butternut squash soup, harvest stew and cranberry sauce.
- **Finding them:** gourds lie on grass, ripe cranberry bushes stand in swamp shallows and chestnut trees grow in forests (new chunks); wild turnips grow in taiga and birch forest. Short grass drops all six new seeds (18 in all, still 12.5 % overall).
- The sickle also picks cranberries and cuts gourds off their stems. Nothing is seasonal: all of it stays after the Halloween and December events are added.
- The checker also verifies gourds, the cranberry and chestnut numbers, bog seeds and every new stage model. Twelve new game tests and a client screenshot test.

### Agriculture: Kitchen Garden (pull request pending, stacked on the Fall Harvest)
- **Trellis:** a square wooden lattice (2 from 5 sticks). It stacks, stands on farmland and blocks movement like a fence.
- **Tomatoes climb trellises**, two blocks tall, and are picked without cutting them down. They grow only into trellis, never into air, and breaking the plant gives back every trellis it grew in.
- **Peppers** grow as a 1-block bush that is picked the same way. **Onion, garlic, cabbage, oats and barley** are 1-block crops, each with a wild plant in fitting biomes.
- **Cooking Pot:** 5 iron ingots and 2 sticks. It cooks while a lit campfire, fire, lava or magma block is directly under it (block tag `jugcraft:heat_sources`). It has six ingredient slots in any order and four result slots, and supports batch cooking, hoppers and comparators. Recipes are data-driven (`jugcraft:pot_cooking`): tomato, onion, vegetable and mushroom barley soups, oat porridge, chili and cabbage rolls.
- **By hand:** garden salad, barley bread, and sauerkraut (cabbage plus Jugcraft salt).
- **Changed:** short grass now drops one Jugcraft seed 12.5 % of the time (vanilla wheat seeds' rate), chosen evenly from all twelve crops. It used to be 2 % per crop, which would have been 24 % with twelve crops.
- The checker also verifies climbing-crop seeds, the Cooking Pot's numbers and recipes (no two with the same ingredients), and loops through every agriculture recipe. Thirteen new game tests and a client screenshot test.

### Agriculture branch: Fall Harvest (pull request pending)
- **New branch:** [docs/branches/AGRICULTURE.md](docs/branches/AGRICULTURE.md) covers the whole Agriculture plan: the crop roster in eight slices, non-industrial farm equipment, connections to the other branches, and rules.
- **Corn grows 3 blocks tall.** You pick it with a right-click, and the stalk stays standing and grows new ears, so cornfields and corn mazes last. From 2 blocks tall it blocks movement like a hedge, which is what makes a maze.
- **Sunflowers** grow 2 blocks tall with a big yellow head, and are picked the same way.
- **Beans, sweet potatoes and flax** are one-block crops. Beans make the crops around them grow 1.5× as fast (legume bonus, block tag `jugcraft:nitrogen_fixing_crops`).
- **Food:** corn, roasted corn, popcorn, sweet potato, baked sweet potato, roasted sunflower seeds and Three Sisters Stew. 2 flax make 1 string.
- **Flint and Bronze Sickles** harvest and replant every ripe crop in 3×3 or 5×5 (vanilla crops too).
- **Seed sources:** wild plants in fitting biomes (new chunks), and short grass (2 % per crop) anywhere.
- Also: composting, pig, chicken and parrot feed, `c:` crop, seed and food tags, and a new `agriculture` feature switch.
- Original textures from `tools/crop_textures.py`. Twelve game tests, plus a client game test with screenshots of a corn maze, the fields and every growth stage.

### Unmerged: biomes branch, Jugcraft regions and nine batches of biomes (stacked on Alpine Spawn)
- **Remaking the Biomes O' Plenty catalog in Jugcraft**, with original art and names: the roster in [docs/branches/BIOMES.md](docs/branches/BIOMES.md) plans 98 biomes in nine batches.
- **Jugcraft regions:** about half the Overworld, in regions about 1 km across, grows the new biomes in place of some vanilla ones; the rest stays vanilla. New settings: `biomes.enabled`, `biomes.region_size`, `biomes.region_share`.
- **Batch 1, the seasonal forests:** Coniferous Forest, Snowy Coniferous Forest, Maple Woods, Seasonal Forest, Aspen Glade, Dead Forest, Tundra, Snowy Forest and Muskeg.
- **New trees and wood:** maples turn red, orange and gold in autumn and stand bare in winter; aspens turn gold; firs stay green; dead wood stands grey. Each has a full wood set (dead wood without a sapling), sawmill and tree farm recipes.
- **Region layouts:** Jugcraft regions come in four layouts (woodland, meadow, wetland, wild), so one vanilla climate can grow different Jugcraft biomes in different regions.
- **Batch 2, fields and meadows** (meadow layout): Field, Flower Meadow, Grassland, Heathland, Lavender Field, Lush Grassland, Prairie, Shrubland and Steppe.
- **New plants:** lavender and tall lavender, goldenrod, heather, orange cosmos (all make dye, the small ones go in flower pots) and clover. **New tree:** the jacaranda, in violet bloom all year, with a full wood set.
- **Batch 3, wetlands** (wetland layout; swamps also elsewhere): Bog (with wild cranberries), Dead Swamp, Lush Swamp, Swamp Woods, Bayou, Floodplain, Ghost Forest, Sludge Mire, Lush River, Fen, Lake District, Quagmire, Marsh and Wetland, with muddy ponds and mud.
- **More plants:** cattails, watergrass (under water) and duckweed (on water). **New tree:** the willow, with hanging leaves and vines, yellow in autumn and bare in winter.
- **Batch 4, warm and dry:** Dryland, Xeric Shrubland, Jacaranda Glade, Lush Desert, Bone Flats, Dry River, Cold Desert, Scrubland, Lush Savanna, Outback, Oasis, Wasteland, Burnt Forest, Mediterranean Forest and Orchard, with their own ground (red sand, coarse dirt, gravel, salt flats). **New trees:** palm and cypress.
- **Batch 5, big trees and rainforests** (mostly the woodland layout): Rainforest, Eucalyptus Forest, Tropics, Subtropics, Dense Forest, Redwood Forest, Temperate Rainforest and Woodland.
- **New trees:** redwood and mahogany (four saplings in a square grow a giant, two blocks wide) and eucalyptus (rainbow-streaked bark), each with a full wood set. **New flowers:** hibiscus and hydrangea.
- **Batch 6, mountains, coasts and volcanoes:** Volcano, Canyon, Highland, Basin, Shield, Karst Pinnacles and Hot Springs (wild layout), Gravel Beach, Dune Beach, Overgrown Beach and Flower Isle (wetland layout), Ice Sheet and Ocean Trench (both). **New plant:** sea oats, which grow on sand.
- **Fixed:** the Cold Desert is no longer buried in snow.
- **Batch 7, wonders and caves:** Cinder Barrens, Elder Vale, Frostlight Garden, Gilded Shrubland, Glimmer Grove, Gloomweald, Hallowed Bog, Highsun Meadow, Mycelial Jungle, Shrine Springs, Snowpetal Grove, Starlit Wood, Toadstool Field, Webwood and Wild Greens, and two cave biomes, the Glowcap Grotto and the Spider Nest. **New plants:** glowcaps and glimmerblooms (both glow), frost irises and snowpetals.
- **Batch 8, the Nether:** Ashfall Wastes, Blighted Sands, Frost Rift, Fungal Thicket, Magma Fields, Marrow Heap, Netherbrush, Quartz Rift and Withered Hollow, rarer than vanilla's five. **New plant:** brambles.
- **Batch 9, the End:** Chorus Reef, Ender Wilds, Outer Flats, Phantom Garden and Rotted Expanse on the outer islands. With it, every biome on the roster is built.

### Unmerged: Alpine Spawn, parts 1 and 2 (stacked on the agriculture pull requests, #53 and #80)
- **New worlds start in Alpine Spawn, at an alpine village.** Alpine Spawn is a large, cool alpine meadow on mountain plateaus; it takes the place of every vanilla meadow and of the cool plateau's forest and taiga. The server moves a new world's spawn to the alpine village nearest the origin, or into the biome when there is no alpine village within 6,400 blocks.
- **Alpine villages are common:** vanilla's taiga villages on a 16-chunk grid (vanilla's is 34), only in this biome.
- **Larches** (part 2): a conifer that changes with the seasons. Its needles are green in spring and summer, turn gold in autumn and fall in winter, leaving bare twigs, then bud green again; each block turns within a week either side, so crowns change gradually. Larches grow among spruces in Alpine Spawn. The tree drops larch saplings, and comes with a full larch wood set (logs, wood, stripped forms, planks, stairs, slab, fence and gate). Larch logs saw into planks in the sawmill, and saplings grow in the tree farm.
- It has seasonal colours and winter snow from the start. Seasonal flowers, bilberries and an alpine winter come in the next parts.
- New settings: `alpine_spawn.enabled` (generation, and the larch's hand recipes) and `alpine_spawn.start` (`on` or `off`: start there).
- Server game tests for its climate entries, tags and villages, and a client test in a real world that it is where the world starts.

### Unmerged: Powered exosuit, batch 28
- Four JE-powered armor pieces (netherite protection, unbreakable): night vision, an energy shield and jetpack, speed, and fall immunity with step assist.
- Two liveries: Vanguard (gunmetal with teal lights) and Ronin (crimson and silver, conical hat, red eyes), with 3D shoulder plates, skirts and hat. Smithing liveries switch between them and keep the charge.
- The crimson Ronin katana. Inspired by Mekanism's MekaSuit (MIT); looks follow the owner's reference images; all art original.

### Unmerged: Gear, weapons and plastic blocks, batch 27
- **Scuba mask and tank:** breathe under water on oxygen (8,000 mB, 400 s); fill the tank from a gas holder or machine.
- **Free runners:** boots with no fall damage and a one-block step.
- **Power katana and power bow:** JE-powered weapons charged at the charging station; the bow fires energy arrows without ammo.
- **Plastic blocks** in all sixteen dye colours, from plastic sheets.
- High-detail art: an animated 32x32 energy katana, and double-resolution scuba gear and free runners.
- Four advancements and handbook pages. Inspired by Mekanism and Mekanism: Additions (MIT); all code and art original.

### Unmerged: Four-ingot ore and bioethanol, batch 26
- **Acid leaching:** an ore and 250 mB of sulfuric acid in the chemical reactor give 4 washed ores (the best ore route).
- **Bioethanol:** 8 crops and a bucket of water ferment into 250 mB in the chemical reactor; it burns in the gas turbine and the advanced engine.
- Inspired by Mekanism (MIT); no new machines.

### Unmerged: Tools, armor and paxels, batch 25
- Bronze and steel swords, pickaxes, axes, shovels, hoes and armor (bronze iron-tier, steel between iron and diamond).
- Paxels (pickaxe, axe and shovel in one) for every tier from wood to netherite, bronze and steel.
- Bronze armor is steampunk (goggles, pressure gauge, boiler); steel armor is kaiserpunk (Pickelhaube, field-grey tunic, jackboots).
- Two advancements and handbook pages. Inspired by Mekanism: Tools (MIT); all code and art original.

### Unmerged: Fewer chemistry machines, batch 24
- Five single-job machines folded into ones that already exist (65 machines down to 60):
  - the distillation tower vacuum-distils heavy fuel oil (was the vacuum distillation unit);
  - the catalytic cracker reforms naphtha, using a catalyst (was the catalytic reformer);
  - the chemical reactor mixes brine and fracking fluid (was the chemical mixer);
  - the **Settling Plant** (the flowback treatment unit, renamed) separates oil sand and bitumen (was the oil sand extractor);
  - the arc furnace pulls silicon boules, with argon (was the crystal grower).
- New uses: the settling plant presses mud into clay; the electrolytic cell splits water into hydrogen and oxygen.

### #80 Seasons (colours, events and winter snow)
- **Grass and leaves change colour with the server's date** in every biome that has four seasons: plains, meadows, forests (dark, dappled and cherry groves included), taigas, windswept hills and swamps.
  - Winter is dull and dormant, spring is fresh green and summer is vanilla.
  - Autumn turns oak leaves gold, orange and red in patches, then russet.
  - Colours change a little each day.
- **Winter snow (opt-in, `seasons.snow=on`):**
  - From December to February, rain falls as snow in those biomes, and up to `seasons.snow_depth` layers settle.
  - The snow melts in spring.
  - It never freezes water, lies on farmland or touches snow you placed.
- **Events on one clock:**
  - the **Harvest Feast** (`harvest_feast`: the US Thanksgiving weekend by default, Canada's, or off);
  - **December** (`december`: 1 December to 6 January by default).
  - Both are announced in chat.
- **`/jugcraft season`** shows the season, day and events. Operators can set a season, preview a date or switch snow on or off until the server stops.
- **Server settings** in `config/jugcraft.properties`: `seasons.mode`, `seasons.hemisphere`, `seasons.timezone`, plus the snow and event settings above.
- The server decides everything; clients never use their own clock. Colours and events save nothing; seasonal snow melts away.
- Server and client game tests, with a screenshot per season and one of winter snow.

### Blueprints and test blocks (same draft PR)
- **Blueprint Table:**
  - LIBRARY of the mod's structures and imported blueprints, with a front view and materials; printing is free.
  - IMPORT takes pasted `.jugbp.json` text, checks it, saves it with the world and shares it with everyone.
- **Placing blueprints:** a hologram preview up to 32 blocks away, a Survey Stake screen (progress, materials, Personal/Party, rotate, remove), and drone building layer by layer.
- **Drone Tower tier 1** now also builds the Energy Exchange and Storage Exchange, adding seven new building blocks and two ports that feed the depot.
- **Seating:** Operator Chairs can be sat on.
- **Creative-only test blocks:** the Creative Energy Cell and the Creative Supply Crate.

### Drone Depot (draft PR, stacked on Parties)
- **New blocks:** Drone Depot Terminal, Control Screen Panel, Hologram Table, Cargo Packager, and the tower-placed Landing Platform, Landing Pad and Supply Pickup Plate (no recipes: the Drone Tower builds the depot).
- **Depot:** part of the Drone Tower. Tier 1 places the base floor, eight pads (5x5 plates forming a pad with a charger port), the supply pickup (3x3 plates with a lift hatch) and the terminal building. A terminal without a tower flies no drones.
- **Drones:** nine tiers, all craftable, each with its own 3D look in the tower's graphite-and-dull-red theme (tier 9 is the Superconducting Ring Lifter). Tiers 5–9 use new parts from real materials: neodymium motors, tilt-rotor nacelles, composite rotors, hydrogen lift cells (chemical reactor), ion emitters, superconducting tape, Stirling cryocoolers and superconducting lift fans.
- **Drone Tower ([docs](docs/features/drone-tower.md)):** a Tower Core on a 15×15 chiseled stone plinth builds the Command Post (tier 1, with a furnished command room). Tiers 2–9 are flown in tile by tile by the depot's own drones, using four kinds of tower module. Tower tier N unlocks drone tier N; the full tower holds 100 drones, each in its own hangar, with seven pickups. Adds 19 building materials (nine with stairs and slabs), five furniture blocks and a tower status screen. Drones dock round the pads, fly their routes, and winch crates up from the pickup (the hatch opens and a lift raises the crate).
- **Command room screens:** a hologram table (nine sections form one table projecting a live depot map) and a screen wall (six panels form one live display), both in the tower's command room. The terminal opens a sci-fi screen (OVERVIEW, FLEET, JOBS, POWER, and a PERSONAL/PARTY button) whose text is fitted to the panel.
- **Pooled power:** standby and working draw, cached. Low power slows flights and never drops cargo.
- **Flights:** timed flight records (not mobs) with terrain-following routes, at most 5 launches per tick, and reservations. Clients get a small snapshot when something changes and move the drones themselves.
- **`BuildJobs`:** the build-job interface for blueprints and later builders, plus a development-only `/dronetest` command.
- **Tests:** tower server and client game tests; server game tests (layout rules, docks, flight paths, and in-world depots that build blocks, re-form pads, respect Party mode and link screens) and a client game test with screenshots, plus checker rules keeping drone numbers in sync.

### Parties (draft PR; proposal #21)
- **`/party` commands:** create, invite, accept, decline, leave, kick, leader and disband.
  - Invites expire after 5 minutes, and each player can send 10 a minute.
  - Parties hold up to 8 members.
- **Shared API (`JugcraftParties`):**
  - `sameParty`, `isLeader`, `partyMembers`, change listeners.
  - `mayServe` with `UseMode` (Personal/Party), which every automated system will use.
- **Saving:** parties are saved in the world folder (`jugcraft/parties.txt`).
- **Feature switch:** `parties.enabled`.
- **Tests:** seven new game tests, plus a checker rule that every party result has a chat message.

### #81 Engineer's Handbook reorganised, batch 23
- The book fits the window; the chapter list is a scrollable contents list where the open chapter shows its pages, and long pages scroll (mouse wheel or arrow keys).
- New **Progression** chapter: the road through the mod in nine stages, each a plan and a numbered chain of the items to make in order.

### #78 Machine screens redesigned, batch 22
- Every machine screen has a themed look: dieselpunk amber, electric green or lab teal, after the machine's model.
- A control terminal says what the machine is for, what it is doing, its progress, power and power rate, and holds the side controls.

### #77 Solar tracker and heliostats, batch 21
- **Solar tracker:** a panel that tilts after the sun, 20 JE/t in one block.
- **Heliostats** and a **solar receiver**: 12 JE/t per heliostat under open sky in the field below the receiver (up to 48), boiling water.

### #76 Joined tanks, glass tanks and gauges, batch 20
- Tinplate and glass tanks touching each other join into one tank (up to 64), filling from the bottom.
- **Glass tank** shows its fluid; **tank gauge** shows any tank's or machine's level in eighths.

### #75 Turbocharger and flywheel, batch 19
- **Turbocharger** in the advanced engine's new slot, with coolant water in its new second tank: up to 1,536 KE/t and 10% more KE from each mB of fuel.
- **Flywheel:** stores up to 2,000,000 KE of rotation and drives its front shaft from it; friction runs it down slowly.

### #74 Explosive weapons, batch 18
- **Guncotton** (2 cotton + 250 mB nitric acid, chemical reactor).
- **Grenades**, thrown by hand, and the **grenade launcher**, which fires them further. The blast hurts living things only: up to 16 damage, walls shield, and no block, armor stand, frame or dropped item is ever touched.
- New switch `explosives.enabled`. An advancement, a handbook page and a game test.

### #73 Flow batteries, batch 17
- **Vanadium electrolyte:** two asphalt binder and a bucket of sulfuric acid in the chemical reactor.
- **Flow battery** (3×3×2): 1,000 JE per mB of electrolyte in it, up to 64,000,000 JE with 64 buckets; 8,192 JE/t in and out. Keeps its electrolyte when broken.
- An advancement, a handbook page and a game test.

### #72 Glass chemistry, batch 16
- **Tincal**, natural borax, in desert and badlands sand; **borax**.
- **Borosilicate glass** (2 sand + borax, alloy smelter) drawn into **optical fibre**, which can replace gold in processors.
- **Ferroboron** (iron + borax): with a rare earth oxide it makes **two** neodymium magnets.
- Multi-input recipes now try the one with the most ingredients first.

### #70 Chlorine and lye, batch 15
- **PVC:** refinery gas + chlorine → vinyl chloride (synthesis converter) → PVC resin (polymerization reactor) → two plastic sheets each (metal press).
- **Soap** from lye and rotten flesh; a bar washes off every status effect.
- Two advancements, a handbook page and game tests.

### #69 Rubber and polymers, batch 14
- **Butadiene** from naphtha (chemical reactor) and **synthetic rubber** from butadiene (polymerization reactor).
- **Gaskets** (rubber + steel plate); rubber belts; gasketed steel pipe, four for two plates.
- An advancement, a handbook page and a game test.

### #68 Oxygen-blown steel and argon, batch 13
- The air separation unit also makes **argon**.
- **Boost gases:** oxygen piped into the steel foundry, or argon into the crystal grower, doubles its speed.

### #65 Nitrogen chemistry, batch 12: air separation, ammonia and nitric acid
- **Air separation unit** (2×2, six tall): splits air into nitrogen and oxygen, four to one, needing only power.
- **Synthesis converter** (3×4×2): Haber–Bosch ammonia (hydrogen + nitrogen) and Ostwald nitric acid (ammonia + oxygen + water).
- New gases nitrogen, oxygen and ammonia; nitric acid with a bucket.
- Ammonia + phosphate → 6 fertilizer; nitric acid etches microchips with half the acid.
- Three advancements, a handbook section and game tests.

### #62 Surface deposits, batch 11
- **Coal, Iron, Copper and Tin Deposits:** flat patches in the top layer of stony hills (windswept hills, stony peaks, stony shores). Picks only break them, for nothing; each block holds 1,000 units.
- **Deposit drill** (3×3, two tall): takes one coal or raw ore of each kind every 15 seconds from the deposits under it and one block round it, and pushes them into a chest, pipe, conveyor or machine beside it. Empty deposit blocks turn to stone.
- `deposits.enabled` switch, an advancement, a handbook page and game tests.

### #60 Advanced power, batch 10: big solar, a four-cylinder engine and tanks that keep their fluid
- **Advanced solar panel:** a white pedestal carrying a 3×3 array of cells, 64 JE/t in full sun (eight solar panels).
- **Advanced combustion engine** (2 long): gasoline or diesel → up to 1,024 KE/t on a shaft; through a magnet dynamo, the best JE per mB of either fuel.
- **Tanks** have a new look (white with checker bands) and **keep their fluid when broken**: the item carries the fluid and amount, shown in its tooltip.
- **Fix:** loot tables now use the Minecraft 26.x format; the old keys were silently ignored, so ore drop counts, the charging station's upper half and slab doubles were wrong. A data check and a game test guard it.
- Two advancements, handbook pages and game tests.

### #58 Farming, batch 9: harvesters, sprinklers and cotton
- **Crop harvester** (1×2): harvests and replants the ripe crops in the 9×9 field in front of it.
- **Sprinkler:** pipe-fed water gives nearby crops extra growth ticks; it also spreads fertilizer from its hopper.
- **Cotton:** a new crop; seeds from sifting coarse dirt; cotton spins into string.
- Three advancements, a Farming handbook chapter and a game test for each.

### #57 Fluid logistics, batch 8: gas holders, valves and filters
- **Gas holder** (3×3×3 Horton sphere): 1,024 buckets of one gas, and only gases.
- **Fluid valve:** a steel pipe segment that a redstone signal closes, splitting the line in two.
- **Fluid filter:** a steel pipe segment whose neighbouring tanks and machines only receive its chosen fluid; set it with a bucket or from a tank beside it (for gases).
- Three advancements, handbook pages and a game test for each.

### #56 Electronics, batch 7: silicon, chips and the cyan look
- **The cyan look** for the electronics tier, following the owner's references: near-black casings with cyan seams, cyan glass that glows while working, cyan screens, violet conduits, and a beige retro computer.
- **Crystal grower** (1×2): 4 silicon + a phosphate dopant → a silicon boule; the sawmill cuts it into 8 **silicon wafers**.
- **Lithography station** (3×2×2, a cleanroom with a monitor bank): wafer + copper wire + sulfuric acid → 4 **microchips**.
- **Processors:** the third circuit tier (circuit assembler).
- **Network terminal:** a beige retro computer that reads out the power network it is cabled to.
- Four advancements, an Electronics handbook chapter and game tests for each.

### #54 Chemistry, batch 6: advanced materials
- **Titanium:** a new mined metal (deep ore, iron pickaxe, `titanium.enabled`). No furnace smelts it.
- **The Kroll process:** raw titanium + coke + 250 mB chlorine → titanium sponge (chemical reactor); the arc furnace melts it into ingots. Chlorine's first use.
- **Leaching:** lepidolite or monazite + sulfuric acid → 2 lithium carbonate or 2 rare earth oxide.
- **Lithium battery bank** (3×2×1, electric look): 32,000,000 JE, 16,384 JE/t out of its front; built from lithium cells and titanium.
- **Neodymium magnets** (alloy smelter), and the **magnet dynamo** and **magnet motor**: 95% each way, four times the copper-wound rates, cyan-banded.
- Four advancements, handbook pages and game tests for each.

### #52 Chemistry, batch 5: electrochemistry and acids
- **Brine** (chemical mixer: salt + water) and the **electrolytic cell** (3×3×2): brine → **chlorine**, **hydrogen** (gases) and **lye**, each out of its own row.
- **Chemical reactor** (2×2×2): sulfur dust + water → **sulfuric acid**.
- **Alumina and real aluminum:** bauxite + lye → 2 alumina; 2 alumina + coke → 2 aluminum ingots in the cell. Two ingots per bauxite, twice the arc furnace.
- **Fertilizer:** phosphate + sulfuric acid; ripens every crop in a 5×5 area.
- **Fuel cell** (one block, electric look): hydrogen → 128 JE/t.
- Five advancements, a Chemistry chapter in the handbook, and a metal audit for fluid recipes.

### #51 Oil line, batch 4: industry, and the electric look
- **Polymerization reactor** (2×2×3): refinery gas → plastic pellets; the metal press makes **plastic sheets**.
- **Asphalt**, **asphalt slab** and **asphalt road line**: walking on them is 1.3× as fast.
- **Diesel engine** (2×2×3): up to 512 KE/t into a shaft line from diesel or heavy fuel oil, burning only for what is used.
- Nine oil **advancements**, a **Fuel Values** handbook page, JEI categories for every **fluid machine**, and the oil audit in [docs/BALANCE.md](docs/BALANCE.md#oil).
- **Electric look** (owner request): cables are 6 px graphite with a glowing green core (emissive), with copper, silver or aluminum collars; the battery box, capacitor bank, charging station, solar panel, electric pump, electric motor and dynamo are restyled in graphite and green light.
- Fix: a dynamo took up to twice its 128 KE/t on a strong shaft line; it is now capped per tick.
- Screenshots: a power-gear scene, and the multi-block showroom spaced to fit the oil machines.

### #50 Oil line, batch 3: fracking and diesel power
- New fluids: **fracking fluid** and **flowback water** (with buckets).
- **Chemical mixer** (2×2×2): water + sand + dried kelp → fracking fluid.
- **Fracking rig** (3×3×5): over shale oil, pumps fracking fluid down and brings up crude oil, refinery gas and flowback water.
- **Flowback treatment unit** (3×1×2): flowback water → clean water (a quarter lost) + salt.
- **Diesel generator** (3×2×2): 256 JE/t from diesel (256 JE/mB) or heavy fuel oil (128 JE/mB).
- **Gas turbine** (4×2×2): 512 JE/t from gasoline (384 JE/mB) or refinery gas (192 JE/mB), with lubricant upkeep.
- Game tests for each.

### #49 Oil line, batch 2: refining
- **Steel fluid pipes** (1,000 mB/t) and the **heavy pump** (1,000 mB/t); a pipe line now carries as much as its slowest pipe.
- New fluids: **naphtha, diesel, heavy fuel oil, lubricant, gasoline** (with buckets) and **refinery gas** (a gas: tanks and pipes only).
- **Distillation tower** (2×2×7): crude oil → gas, naphtha, diesel and heavy fuel oil, each drawn off at its own height.
- **Catalytic cracker** (2×2×4): heavy fuel oil + steam + catalyst → diesel, naphtha and gas. **Cracking catalyst** from bauxite, sand and nickel.
- **Vacuum distillation unit** (2×2×3): heavy fuel oil → lubricant + asphalt binder.
- **Catalytic reformer** (3×2×2): naphtha → gasoline + gas.
- Game tests for each.

### #47 Oil line, batch 1: oil in the world
The first five commits of the dieselpunk Chemistry branch ([plan](docs/branches/CHEMISTRY.md#petrochemistry-the-dieselpunk-oil-line)).
- **Crude oil:** a real fluid with a bucket; slow, thick, never makes new sources; works in every tank and pipe.
- **Fluid processing machines:** machines with input and output tanks, data-driven fluid recipes and tank gauges on their screens.
- **Oil reservoirs:** hidden, finite oil under Overworld chunks (pumpable or shale), fixed by the seed; the prospector reports them.
- **Pumpjack:** a 1×3×3 dieselpunk nodding donkey that pumps crude oil from the reservoir under it.
- **Oil sand extractor:** a 2×2×2 hot-water plant that washes crude oil out of oil sand and bitumen.
- Game tests for each; a new Oil chapter in the handbook.

### #46 Balance review
- New [docs/BALANCE.md](docs/BALANCE.md): every generator, conversion, store and cost in one place, with the loops that were checked.
- Charcoal burns three quarters as long as coal in Jugcraft's generators and engines. This makes tree-farm wood power slightly weaker (net about +545 JE/t per tree farm, down from +737), as the owner chose. Vanilla furnaces are unchanged.

### #44 Conveyor slopes
- **Conveyor Slope:** carries items one block up or down; use it with an empty hand to switch. Slopes join conveyor runs, and items climb and descend them visibly.
- A game test and a client screenshot.

### #43 Advancements
- A **Jugcraft** advancement tab: 22 steps from the first tin to the rocket pack, earned by having each item. Goals for steel, the steel foundry and the large steam engine; a challenge for the rocket pack.
- The handbook's Getting Started chapter lists the steps on its Milestones pages.
- A game test checks the tree loads; the data checker checks every step's items, title and parent.

### #42 Tool upgrades and a 3D rocket pack
- **Upgrade modules** for the powered tools, fitted at the charging station: Overclock, Range (5×5 drilling), Capacity, Silk Touch and Fortune.
- The worn rocket pack is now a 3D model on the wearer's back.
- The mining drill sits higher in first person.
- Four game tests.

### #41 Dieselpunk steel machines
- The steel foundry, capacitor bank, steel tank, ore drill and high-pressure extractor now look dieselpunk: gunmetal and olive paint, hazard stripes, chrome, phosphor gauges and caged lamps, an exhaust stack on the foundry and a diesel motor on the drill. Same footprints and ports; looks only.

### #40 Powered tools (the first dieselpunk gear)
- **Mining Drill:** a JE pickaxe and shovel, faster than netherite; modes for one block, 3×3 or a whole ore vein.
- **Chainsaw:** a JE axe that also cuts leaves and fells whole trees.
- **Rocket Pack:** worn on the chest; hold jump in the air to fly.
- **Charging Station:** a two-block-tall station that charges the tool on its cradle from cables.
- The tools hold JE instead of wearing out; empty, they mine like a bare hand.
- New dieselpunk textures and detailed 3D item models; [docs/ART_DIRECTION.md](docs/ART_DIRECTION.md) records the rule that higher tiers look dieselpunk.
- Five game tests and three client screenshots.

### #38 Conveyors
- **Conveyor:** carries items (drawn riding on it) the way it faces, 2.5 blocks a second, while rotation drives it: 1 KE per conveyor per tick for a whole joined run. Pipes, hoppers, machines and dropped items load it; it unloads into the conveyor or inventory ahead, or onto the ground. It carries players and mobs too.
- **Conveyor Splitter:** sends items left, straight on and right in turn.
- Four game tests and a client screenshot.

### #37 Spinning shafts and closer screenshots
- Shafts, belt pulleys, the hand crank, the electric motor's shaft and the steam engine's flywheel now really spin (a block entity renderer) instead of scrolling a texture. Shafts placed with earlier builds need re-placing to spin.
- The client test photographs a belt-and-motor line and the multi-blocks from closer, in three views.

### #36 Belts and the Electric Motor
- **Belt Pulley** and **Leather Belt:** link two pulleys up to 16 blocks apart to carry rotation; the belt is drawn between them.
- **Electric Motor:** JE → KE at 75%, up to 96 KE/t.
- Three game tests.

### #35 Bigger machines, a spinning wind turbine, the Large Steam Engine and JEI
- Machines can now fill up to 64 blocks. Resized:
  - **Alloy Smelter:** 3×2×6, with a big copper crucible tank pouring into one funnel over the furnace.
  - **Geothermal Generator:** 2×2×2.
  - **Steel Foundry:** 2×2×5.
  - **Coke Oven:** 2×2×2, with its chimney in a block on top.
  - **Wind Turbine:** 9 tall, with a 7-block rotor that spins (block entity renderer); 12–72 JE/t.
- **Large Steam Engine** (2×2×2): 256 KE/t, four times the small one.
- Machine screens: amber energy readout without a shadow; vanilla tooltips on gauges.
- **JEI:** a recipe page per machine (optional; EMI has no 26.3 build yet).
- Multi-blocks placed with earlier builds need re-placing.

### #32 Polish
- Machines with a fire, and the steam engine, smoke and crackle while running.
- Hovering the energy bar or a tank gauge shows exact JE or mB.
- The eject button reads "Eject" (green on, gray off) instead of a cut-off "Eject: off".
- The CI screenshots no longer show the chat log.

### #30 Auto-Crafter
- **Auto-Crafter:** crafts any crafting-table recipe laid out in its 3×3 grid, one every 2 seconds.
  - Each grid slot keeps one item as the pattern, and pipes and hoppers only top up matching slots.
  - Remainders such as empty bottles get their own slot.
- A powered processor with upgrades, sides, eject, redstone and kinetic power; a new grid layout on its screen.
- Three game tests and a client screenshot of its screen.

### #29 Kinetic power
- A mechanical power layer in **KE** per tick. **Iron Shafts** carry it along their axis and **Brass Gearboxes** out of all six sides; both animate while turning.
- Sources:
  - **Hand Crank:** 16 KE/t while cranked.
  - **Steam Engine:** 64 KE/t from fuel and water, burning only while something takes the power.
- Every powered machine runs straight off a shaft (1 KE = 1 JE). The **Dynamo** bridges KE into JE cables at 75%.
- Three game tests, a client screenshot of a running line, and handbook pages under Power.

### #28 Renewable resources
- **Water Wheel** (2 tall): up to 24 JE/t from flowing or falling water beside its wheel, with no fuel.
- **Cobblestone Generator:** one cobblestone a second from water and lava touching it; neither is used up.
- **Tree Farm:** grows a sapling into six logs and gives the sapling back, sometimes with an extra (apple, cocoa beans, …). Recipes are data for all nine vanilla trees.
- Steampunk models (timber water wheel with a coil dynamo, cistern-and-crucible generator, open brass growth cabinet with a grow lamp) and classic models.
- The handbook gains a "Renewables" chapter; its chapter buttons are packed tighter to fit 11 chapters.
- Three game tests.

### #25 Mining and prospecting
- **Geo-Resonance Prospector:** a hand tool that surveys the 3×3 chunks around you. It opens a steampunk-digital screen: a brass instrument with an amber CRT, valve-tube signal bars, a sweeping scan line and a resonance needle gauge.
  - Readings are deliberately vague: 1–5 bars and shallow, middle or deep for each ore family, never a chunk or block.
- **Ore Drill** (2-tall derrick): mines every ore in a 9×9 column below it, down to the bottom of the world, and refills the holes with rock. It gives whole ore blocks, so ore processing still decides the yield. It has upgrades, side configuration, eject and redstone control.
- Two game tests, a prospector screenshot in the client test, and handbook pages under Materials.

### #24 Storage
- **Capacitor Bank** (2×2): 4,000,000 JE. It charges from any side and gives power out of its front sockets at 4,096 JE/t.
- **Steel Tank** (2×2 squat riveted tank): 128 buckets.
- **Item Crate:** 32 stacks of one item, with right-click in and out and support for pipes, hoppers and comparators.
- Steampunk models (Leyden-jar bank, domed tank, banded crate) and classic models.
- Three game tests, a handbook "Storage" chapter, and a feature record.

### #22 Transmitter tiers
- **Silver Cable** (1,024 JE/t) and **Aluminum Cable** (4,096 JE/t). All cable tiers join one network, which runs at its slowest cable.
- **High-Pressure Extractor** (steel): 32 items every 4 ticks, four times the brass extractor.
- There is no faster fluid pipe: pumps (100 mB/t) are the limit, not pipes.
- Two game tests; handbook pages.

### #20 Engineer's Handbook and in-game screenshots
- **Engineer's Handbook** (book + copper ingot): an in-game guide with 9 chapters and 36 pages. Each page gives what a block does, its power use, its crafting grid and example recipes, and you can hover over items.
- The content is generated from the mod's own tables, so it can't go out of date.
- **Client game tests:** CI starts a real game client, builds a showroom of every machine, opens a machine screen and the handbook, and saves screenshots as a build artifact.

### #19 Machine control
- **Upgrades:** every powered processing machine gets two upgrade slots.
  - **Speed Upgrade:** 4 cards make it 3× as fast for twice the energy per item.
  - **Efficiency Upgrade:** 4 cards bring it to 41% of the energy.
  - Both are made from steel.
- **Redstone mode button:** ignored, run with a signal, or run without one.
- **Comparators** read stored energy (generators, battery box) or how full a machine is, from any block of a multi-block.
- Energy readouts are shortened (for example "12.5k / 20k JE") to fit the new slots.
- Four new game tests.

### #18 Steel tier
- **Coke Oven** (2 tall) bakes coal into **Coal Coke**. Coke is a 3,200-tick generator fuel and the carbon for steel.
- **Steel Foundry** (3 tall) turns 1 iron ingot + 1 coke into 1 **steel ingot**.
- Both are unpowered brick multi-blocks with steampunk and classic models.
- New steel items: ingot, nugget, block, plate and gear.
- **Fix:** cables drew a connection arm to every face of a machine, even where no power goes in (for example all around the alloy smelter). Now the arm and the energy flow use the same check.
- Three new game tests, including one for the cable fix.

### Merged 30 September 2026: PRs #8–#17

These were built as a stack, each on the one before, and merged in order (#8 first).

#### #17 Changelog and "What Exists" guide
- Adds this `CHANGELOG.md` and [`docs/WHAT_EXISTS.md`](docs/WHAT_EXISTS.md). WHAT_EXISTS is a map of all content, shared APIs, data formats, file locations and check rules, so contributors and AI agents can build alongside the existing systems.
- Adds feature records for [item logistics](docs/features/item-logistics.md) and [ore processing](docs/features/ore-processing.md).

#### #16 Ore processing depth
- **New machines:**
  - **Pulverizer:** ore → 2 dust, plus a byproduct roll.
  - **Ore Washer:** ore + water → 3 washed ore, which the pulverizer grinds.
  - **Sieve:** gravel → flint; soul sand → soul soil; both with small finds.
  - **Sawmill:** log → 6 planks + sawdust; planks → 3 sticks.
- **Three ore routes:** smelt (×1), crush or pulverize (×2), wash then pulverize (×3).
- **New items:**
  - 10 metal dusts (`c:dusts/<metal>`), which smelt into ingots. Nickel, tungsten and uranium dust use the arc furnace.
  - 10 washed ores.
  - Sawdust (4 → paper).
- **Byproducts:**
  - Machine recipes can list byproducts: chance, count, and an optional feature switch.
  - The pulverizer, sieve and sawmill have two byproduct slots, and a machine waits rather than lose a byproduct.
  - Byproduct pairs follow real ores, for example copper → gold and lead → silver.
- **Looks:** steampunk models (ball mill, washing vat with a water wheel, shaker sieve, sawbench) and classic textures.
- **Balance checks:** only ores get a bonus (×3 at most); byproducts add at most 25% of the input's metal; renewable sieve finds average under a nugget per operation.
- Seven new game tests.

#### #15 Item logistics
- **Side configuration:** every processing machine's screen has six face buttons (front, back, left, right, top, bottom). Each cycles between In, Out, Both and Off. The defaults keep the old hopper behavior.
- **Eject:** when on, the machine pushes its results out of its Out faces, 16 items every 8 ticks.
- **New blocks and items:**
  - **Brass Item Pipe:** 6 px. Items go first to matching sorters, then round-robin to the other inventories.
  - **Pneumatic Extractor:** 16 items every 8 ticks; a redstone signal pauses it.
  - **Item Sorter:** 9-slot filter.
  - **Brass Wrench:** turns machines; sneak to dismantle.
- Three new game tests.
- Fix: a multi-block machine's eject never feeds back into its own other blocks.

#### #14 Machine recipes as data, and in-game tests
- Machine recipes are now real Minecraft recipe types (`jugcraft:crushing`, `alloying`, …), one JSON file each under `data/<ns>/recipe/<type>/`. Data packs can add, change or remove them, and ingredients may be tags.
- In-game tests (Fabric game test API) run in CI with `./gradlew build`.
- **Launch-blocking fixes found by those tests** (before this PR, `main` could not start a server):
  - Fabric Loader 0.18.4 → **0.19.3**, which Fabric API 0.161.0+26.3 requires.
  - Ore worldgen moved to the 26.x format: `worldgen/feature/`, no `config` wrapper, block states as plain IDs.

#### #13 Steampunk machines
- Every machine redrawn as a detailed steampunk model: brass, copper, riveted iron, gauges, gears, valve wheels, and fireboxes that glow while running. Includes 37 original textures.
- The previous look stays available as the built-in resource pack **Jugcraft: Classic Machines**, so the change can be reverted.

#### #12 2×2 alloy smelter and power sockets
- The alloy smelter becomes a 2×2 multi-block with a visible copper **power socket**, the only place cables connect to it.
- Clear power-connection rules for every machine: each one either has a socket or takes power on any face that touches a cable.

#### #11 Multi-block machines
- Machines can occupy several blocks and are placed as one item, in the style of Immersive Engineering. Breaking any part removes the whole machine.
- **Geothermal Generator** (2 wide, lava → 64 JE/t) and **Wind Turbine** (3 tall, 4–24 JE/t by height and weather).

#### #10 Fluids, and tech-mod style cables and pipes
- **Bronze Fluid Pipe**, **Tinplate Tank** (16 buckets) and **Electric Pump**.
- Cables and pipes are thin (4 px) and connect to each other and to anything with energy or fluid storage, as in other tech mods. Items show as 3D segments.

#### #9 Parts and circuits
- **Metal Press** (ingot → plate), **Wire Drawer** (ingot → 3 wires), **Circuit Assembler** (basic and advanced circuits).
- Gears are crafted from four plates.

#### #8 Alloy smelter
- **Alloy Smelter** makes bronze, **brass**, **invar** and **solder** at metal-conserving ratios.

### Merged earlier on 30 September 2026

#### #7 Solar panel and steam generator
- **Solar Panel** (8 JE/t in sun) and **Steam Generator** (coal or bitumen plus water → 64 JE/t), plus a machine roadmap.

#### #6 Electricity and the first machines
- Jugcraft Energy (JE) with a shared `EnergyStorage` API, **Copper Cable** networks, **Coal Generator**, **Battery Box**, **Electric Furnace**, **Crusher** (ore doubling) and the **Arc Furnace** 3×3×3 multiblock.

#### #5 Base materials
- Zinc, lead, silver, nickel, tungsten, uranium, aluminum (bauxite), salt, phosphate, lithium (lepidolite), rare earths (monazite), sulfur, silicon and oil sand (bitumen).
- Feature switches in `config/jugcraft.properties`.

#### #4 Fabric 26.3 scaffold, tin and bronze
- Pinned toolchain and the CI Build workflow.
- Tin ore, raw tin, ingots and blocks, and bronze from a 3:1 copper/tin blend.
- The offline data generator and checker (`tools/`).

#### #1 Vision and development setup
- Project vision, contribution rules, design and architecture documents, and owner-directed Fabric development.
