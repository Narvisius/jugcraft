# The Witch's Workshop

Status: implemented. CI builds batch 17 as first released and its game tests and client screenshots pass (below); not yet played by hand. The bigger jars (rebuilt on 6 October 2026 as their small jars made bigger) and the art fixes of 5 October 2026 are written but not yet built or run (see Verification).
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("Lets come up with another 20 very detailed well thought out additions to add for the halloween / fall season using the first 3 images for ideas of prop sets … lets do them piece by piece maybe 5 at a time"). These are prop sets 1–5 of the [Witching Season plan](witching-season.md), drawn from the reference pictures' cauldron, chandelier, broom and jars.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier for most sets; the Enchanted Broom needs Flying Ointment from the hex brews (a Bubbling Cauldron over a fire), and the Beating Heart Jar a brass nugget from the alloy smelter (Workshops tier). Of the bigger jars, the Giant's Beating Heart is Workshops tier (brass ingots and a Beating Heart Jar); the Tall Specimen Jar and Specimen Tank are Discovery tier (iron, glass and the batch 8 Specimen Jar).
Primary specialty and supported player role: building, with useful machines for farmers and brewers. These props furnish a witch's workshop: a brewing corner, candlelight, a broom closet, a collector's cabinet, a shelf of oddities.

## Player experience

### 1. The Horned Skull Cauldron
1. **Horned Skull Cauldron** (a cauldron, two iron ingots, a bone block and two goat horns; or two bones in place of the horns): a squat black-iron pot with a rounded, riveted belly on four clawed feet, and a bleached ram's skull bolted to its front, its horns curling out past the rim. It faces whoever places it.
2. **It holds potions as well as water.**
   - A water bucket fills it three deep, and a bucket takes a full pot of water back.
   - A potion pours in if the pot is empty or holds that same potion, up to three bottles.
   - A glass bottle draws a level back out, as the potion that went in.
   - What goes in comes out: nothing is gained or lost.
3. **The brew shows its colour.** The brew's surface is drawn in its potion's colour, and a potion glows: light 6 plus one a level, two more while heated. The skull's eye sockets glow in the same colour.
4. **It brews over heat.** Over a fire, campfire, magma, lava or an Ember Bed underneath:
   - the brew heaves and pops;
   - three translucent fume ribbons twist up out of it in the brew's colour, swaying and fading a block and a half up;
   - now and then a drop of brew spits over the rim.
5. **Waft it.** Stir a heated potion with the **Brew Ladle** (a bowl on two sticks). Up to four players within three blocks, the nearest first, each get its lasting effects for a quarter of their duration, and it uses one level. One bottle shared by four is still one bottle. Instant effects (healing, harming) don't waft.
6. **Things float in it.** Use one of a witch's ingredients on the brew to drop it in (the item tag `jugcraft:cauldron_floaters`):
   - from vanilla: apples, bones, spider eyes, mushrooms, roots and berries, eggs, feathers, slime, magma cream, ghast tears, nether wart, pufferfish, rabbits' feet, phantom membrane, amethyst shards and wither roses;
   - from Jugcraft: mandrake roots, garlic, acorns, chestnuts and cranberries.

   Up to three float half-sunk, bobbing and turning. An empty hand or the ladle fishes the last one out, and breaking the pot gives them back. Tools, weapons and containers never go in, so nothing is lost by a slip.
7. **Ember Bed** (four cobblestone round a coal or charcoal; two at a time): a block-high hearth of sooty fieldstones round glowing embers, with an arched draft hole in each side showing the fire (light 9).
   - It is a heat source for this cauldron, the Bubbling Cauldron and the Cooking Pot (`jugcraft:heat_sources`).
   - It spits sparks, smokes and crackles.
   - Like a magma block, it burns whoever walks on it without sneaking.

### 2. The wrought-iron candelabra
1. Four iron fittings (candles, iron nuggets and ingots), their candles drawn by the client:
   - **Floor Candelabrum** (two blocks tall): four scrolled feet, a twisted stem with knops, scroll brackets, and seven candles in two tiers and a crown;
   - **Table Candelabrum**: three candles on scrolled arms, standing on a block;
   - **Wall Girandole** (a tinted-glass looking-glass among its candles): three candles before a cast back-plate, a black mirror and a crest; it hangs on a wall;
   - **Branching Chandelier**: three tiers of arms reaching a block out on every side and down into the block below, sixteen candles, hung from its crown by chains. It hangs under a block or a chain and falls without it. It is different from the Haunted Chandelier's single ring.
2. **Six waxes**: use a dye to recolour every candle: ivory (white dye or bone meal), black, purple, green, orange or red.
3. **Four flames**: ordinary, soul (soul sand or soul soil), witchfire purple (an amethyst shard) or ghostfire green (a glow ink sac). Each flame flickers on its own, at full brightness.
4. **Lighting**:
   - flint and steel, a fire charge or a burning arrow lights them, and an empty hand snuffs them;
   - a redstone signal lights them while it lasts;
   - lit, they give light 9 (table, girandole), 14 (floor candelabrum, both halves) or 15 (chandelier).
5. **Wax drips** run down the candles while they burn and pool in their pans, in three stages over about three in-game days. Shears scrape them off and give nothing, so a burning candle is no wax farm.

### 3. The Enchanted Broom, Dustpan and Broom Rack
1. **Enchanted Broom** (a Witch's Broom, an amethyst shard and string): a besom of bound twigs on a crooked handle, an amethyst bound into it with a purple cord, standing on its bristles.
2. **Wake it with Flying Ointment** (the hex brew): rub it on and the bottle comes back. It wakes for three in-game days, and won't take more while still nearly full. Awake:
   - it sways and its bristle-tips glow;
   - once a second it looks for dropped items within four blocks (a box a block up and down);
   - it leans and swishes toward them, pushing up to sixteen at a time toward the nearest Dustpan in that box, which takes them in;
   - with no Dustpan, it sweeps them into a heap at its own feet.

   It moves only dropped items, and makes nothing.
3. **Dustpan** (an iron ingot and a copper ingot): a tin dustpan with a wooden handle.
   - It holds nine stacks; open it like a one-row chest, or empty it with a hopper or a pipe.
   - The heap of sweepings shows in the pan, and a comparator reads how full it is.
4. **Broom Rack** (three iron nuggets over three planks): a dark oak rail for a wall with three brass pegs. Each peg holds and shows one broom: Witch's Brooms, Enchanted Brooms or Flying Broomsticks (the item tag `jugcraft:brooms`). Use a peg with a broom to hang it and again to take it down.

### 4. The Cabinet of Curiosities
1. **Curiosity Cabinet** (dark oak planks, a gold nugget and glass panes; two blocks tall): a carved mahogany cabinet on bun feet, with three velvet-lined shelves behind two glazed doors and a crown with a carved moth-and-skull crest.
   - Aim at one of its nine places on the front and use any item to put one of it there; use it again to take the thing back, as with a chiseled bookshelf.
   - The doors swing open while you reach in, and a comparator reads how full it is.
2. **Bell Jar** (three glass and a wooden slab): a glass dome with a brass knob on a turned plinth. It shows one thing under glass, turning slowly once every twelve seconds.
3. **Moth Display Case** (an item frame, a glass pane and green dye): a shallow glazed case for a wall, holding a big moth and two smaller ones pinned over a hand-written label.
   - Use it to change the specimens: luna moths (pale green, eyespots, long tails), death's-head hawkmoths (with the skull on the thorax) or atlas moths (rust red with white windows).
   - **At night the moths come alive** and slowly open and close their wings.

### 5. The Oddity Jars
Squat glass jars on dark bases under iron lids, with paper labels:
1. **Jar of Eyeballs** (seven spider eyes, an iron nugget and glass): nine eyeballs in murky fluid. Each rolls to follow the nearest player within eight blocks, the later ones a little slower, so they never turn as one. With nobody near, they wander.
2. **Beating Heart Jar** (glass, an iron nugget, rotten flesh, redstone and a brass nugget): a heart on a brass stand with a little dial, in red fluid. **It is a redstone clock.**
   - It beats at 60, 80, 100 or 120 beats a minute; use it to change the tempo.
   - Each beat gives a two-tick signal of 15 to every side but the one below, and the heart swells with it.
   - A signal into its base stops it, and it starts again when that signal goes.
3. **Bat in a Jar** (glass, an iron nugget, phantom membrane and leather): a bat hanging asleep from the lid. When a player comes within three blocks it wakes with a flap, squeaks and flutters round the jar for five seconds, then settles. It wakes again for the next player to come close. Its jar is a little wider and taller than the others (12.4 pixels across, 15.4 tall), so the bat's wings stay inside the glass as it flies.
4. **Two-Headed Snake Jar** (glass, an iron nugget, string and a slime ball): a coiled snake in pale green fluid whose two heads sway this way and that. Use the jar and both heads flick their tongues and hiss.
5. **Hand in a Jar** (glass, an iron nugget, rotten flesh and a bone): a stitched hand floating in yellowed fluid, drumming its fingers one after another. While it has a redstone signal, it points at the nearest player.

### 6. The bigger jars
Added on 5 October 2026, when the owner asked for bigger jars, and rebuilt on 6 October 2026: each is now its small jar made bigger and nothing else. The owner found the first Giant's Beating Heart, a vat of its own design, "so so ugly", and wanted "the heart in the jar but just a bigger version in the exact same style of jar". So every box of the small jar's model, and what floats in it, is two or three times bigger, drawn on the small jar's own textures, and the client draws the small jar's own heart or specimen in it, as much bigger, moving just as it does in the small jar.

Each is several blocks placed and broken as one, like the Colossal Skull: it stands from the block aimed at to the placer's right, up and away, and isn't placed if anything is in the way. Breaking any block of it breaks it all and drops it once. Each block's shape is its piece of the small jar's box made as much bigger, so the empty corners of the bigger props can be walked into.
1. **Giant's Beating Heart** (Workshops tier; five glass, a Beating Heart Jar, two brass ingots and a block of redstone): the Beating Heart Jar three times over, three blocks wide, tall and deep: its glass jar of red murk on a dark iron base, its iron lid and knob, its paper label, and the heart turning slowly on its brass stand.
   - **It is a slow redstone clock**: 40, 50, 60 or 72 beats a minute (use any block of it with an empty hand to change the tempo).
   - Each beat gives a two-tick signal of 15 from its first block (bottom front corner on the placer's left) to every side but the one below, with a deep heartbeat.
   - A redstone signal into any of its nine bottom blocks from below stops it, and it starts again when that signal goes.
   - The heart swells with each beat and settles, just as the jar's does.
   - Its item is the Beating Heart Jar's, with the heart resting on its stand inside, a little bigger in a slot.
2. **Tall Specimen Jar** (an iron ingot over a Specimen Jar over glass): the Specimen Jar twice over, one block across and two tall, its fluid glowing (light 8).
3. **Specimen Tank** (four iron ingots, four glass and a Tall Specimen Jar): the Specimen Jar three times over, three blocks wide, tall and deep (light 10).
   - Both hold one of the Specimen Jar's specimens (an eye, a tentacle, a pumpkin or a brain), two or three times bigger, bobbing and turning in the middle of the fluid among rising bubbles just as in the small jar.
   - Sneak-use any block of one to put in the next specimen. Broken, it keeps its specimen.
   - Their items are the Specimen Jar's own, with the specimen they hold, a little bigger in a slot the bigger the jar.

## Changes from the plan
- The Branching Chandelier has sixteen candles, not thirteen: a candle in the middle of its crown would have stood in its own stem, so the top tier has four.
- Things are floated with a plain use, not sneaking (vanilla skips a block's own use while a player sneaks holding something), so what floats is a curated item tag rather than "any item".
- Recipes settled: two bones stand in for the goat horns, and the Jar of Eyeballs takes seven spider eyes (a crafting grid holds nine things in all).
- The Ember Bed is a full block, so the cauldron stands on its stones rather than floating over a low fire.
- **Art fixes, 5 and 6 October 2026** (the owner's feedback on the first screenshots, and a review of the fixes):
  - **Horned Skull Cauldron**: its pot is closed. Looking in, the old pot's walls showed the sky through their backs (the inside of a box has no faces). The neck and rim are now rings of boxes built by `flora_art.box_ring`, lined inside, and the empty pot shows a dark iron well. The ram skull has square, Minecraft-like eye sockets, and the brew's glow covers them exactly as a flat square.
  - **Skulls everywhere** have square sockets: the Colossal Skull, the Bone Throne's crest, the Skull Footstool, the sarcophagi's skull lids, the bone piles and the ossuary wall (every skull drawn by `cute_art.skull_face` or `decor18_data.human_skull`). Glows on them are flat squares on the sockets, not round blobs with a glint:
    - a lit skull's small sockets glow whole (on the Vertebra Floor Lamp's 12-texel face, a dark rim would have left one lit texel each);
    - the Colossal Skull's night glow lies 0.1 pixel in front of the dark hollow behind its sockets (`decor18.SKULL`); it was drawn 0.9 pixel behind it and never showed.
  - **Bat in a Jar**: the jar is wider and taller and the bat a touch smaller and nearer the middle, so its wingtips never pass through the glass (they reached 7.4 pixels from the middle in a jar 3.7 wide; now 5.4 in one 5.8 wide).
  - **Clean jar fittings**: the Bat in a Jar's, Hand in a Jar's, Two-Headed Snake Jar's and Jar of Eyeballs' lids, bases and knobs are flat iron with a one-pixel bevel instead of speckled wrought iron, their murk lies in three even bands, and their labels are flat paper with even lines of ink and a paper back. The Beating Heart Jar keeps its first look on purpose, hammered lid and all, because the owner called it awesome; on a shelf it is the one jar in the old style. The small Specimen Jar is unchanged.
  - **Props that showed the sky through their backs** are closed: the Colossal Vertebra (its canal is now a dark well), Colossal Skull, Colossal Rib, Curiosity Cabinet (door posts and crest), Bell Jar (its shoulder), the four candelabra (drip pans are solid iron to their corners), Coffin Wardrobe, Iron-Bound Coffin (lid panel and hasp, seen open), Ribcage Bookcase, Grandfather Clock, the Farm Stand (table ends, apron and awning ends) and the Egg Sac Cluster (each sac is now a closed box, its back 0.1 pixel in front of the web on a plane of its own; the item seen from behind showed the sacs as hollow boxes).
  - **No faces flush with others**: the Iron-Bound Coffin's bands and corner caps stand 0.1 pixel proud of its rim and of its lid's underside (seen open, they lay 0.01 pixel from the velvet and flickered), and the Grandfather Clock's corner finials 0.1 pixel proud of its pediment's turned pieces. The Lab Table patient's eyes show the bright middle of their glow, their UVs inside its picture (they sampled outside it).
  - **No doubled quads**: the client's cut-out quads (the bat's wings and the other batch 17 to 20 cut-outs, the Harvest Effigy's cloak, the spiderlings, the witchlights' glow and the lab table's eyes) are each one plane, drawn from both sides, instead of two planes back to back that flickered.
  - **Clean wrought iron**: the candelabra's bars and knops, the moth case's frame and the Harvest Moon Lamp's iron are flat iron with a bevel, and the twisted bars (candelabra and witchlights) have even ridges, instead of speckle.

## Connections
- Existing input producer:
  - mob drops and vanilla materials: bone, iron, goat horns, spider eyes, rotten flesh, string, phantom membrane, leather, slime;
  - dyes and candles;
  - the hex brews' Flying Ointment, for the broom;
  - the alloy smelter's brass, for the heart jar;
  - the Witch's Broom.
- Existing output consumer:
  - brewers (wafting shares one bottle among friends; potions keep in the pot);
  - farmers and builders (the broom and Dustpan gather dropped items for hoppers and pipes);
  - redstone (the heart jar's clock, the cabinet's and bell jar's comparator signal);
  - building.
- Technology connection: redstone (the candelabra, the heart and the hand), hoppers and pipes (the Dustpan), brass. Magic connection: hex brews (the broom's ointment), potions (the cauldron).
- Reachable entry path: all but two sets need only early materials and a crafting table. The broom needs a Bubbling Cauldron over a fire, and the heart jar a brass nugget, both reachable and neither needing these props first.
- Required vs optional: everything is optional; nothing gates progression.

## Balance and automation
- **The cauldron**:
  - what goes in comes out: a bottle in, a bottle out of the same potion;
  - wafting divides one level among at most four players at a quarter duration each, so four players get one bottle's worth between them, no more;
  - instant effects don't waft;
  - floating things are kept and given back.
- **The candelabra**: drips are looks only and give nothing; shears clear them and are worn.
- **The broom**:
  - it only moves dropped items, at most sixteen a sweep, once a second, within four blocks; it makes nothing;
  - one Flying Ointment (three nights' brewing and a bottle) buys three in-game days;
  - asleep, it doesn't tick at all.
- **The heart jar** is a signal source and nothing more; its beats are scheduled ticks, one a beat.
- **The bat jar** looks for a player within three blocks every ten ticks, nearby only.
- **The Giant's Beating Heart** is a signal source and nothing more, like the heart jar it's made from: one scheduled tick a beat on its first block only, and the other 26 blocks never tick. It costs a Beating Heart Jar, two brass ingots and a block of redstone, more than the clock it replaces. Its signal can't reach below, so it can't stop itself.
- **The Tall Specimen Jar and Specimen Tank** do nothing but glow; each costs the smaller jar it is made from, more iron and glass. Changing the specimen costs nothing and gives nothing.
- **The client draws**: the eyes, the hand's pointing, the moths and the cabinet's doors are drawn by clients from what they already know. The server does nothing for them.
- **Recipes**: each takes materials in and gives the prop; breaking gives the prop back. The Ember Bed makes two from four cobblestone and a coal, worth less than it uses.

## Multiplayer and persistence
- **Server-side**: every change is decided on the server, through vanilla's paths:
  - filling, drawing, stirring and floating;
  - colouring, lighting and snuffing;
  - anointing, sweeping and racking;
  - placing things on show;
  - the heart's tempo (and the Giant's Beating Heart's);
  - the bigger jars' specimens.
- **Build rights**: changing a cauldron, a candelabrum or a moth case needs them (the player's build ability), so the lairs' protection will cover them later.
- **Saved**:
  - block state, and in block entities: the cauldron's potion and floating things, the broom's charge, the Dustpan's nine stacks, and the cabinet's, bell jar's and rack's things;
  - the cabinet's door swing and the snake's tongues are client animations, not saved.
- **Drops on breaking**: the cabinet, bell jar, rack, cauldron and Dustpan spill what they hold. The bigger jars drop once, from their first block's loot table (the others match nothing); the Tall Specimen Jar and Specimen Tank keep their specimen on the item.
- **Bigger jars**: every block of one is saved in its block state (`facing`, `part`, the heart's `beat` and `tempo`, a vessel's `specimen`); the block entity on the first block holds nothing and is there for drawing.
- **New IDs**:
  - blocks and items `horned_skull_cauldron`, `ember_bed`, `floor_candelabrum`, `table_candelabrum`, `wall_girandole`, `branching_chandelier`, `enchanted_broom`, `dustpan`, `broom_rack`, `curiosity_cabinet`, `bell_jar`, `moth_display_case`, `jar_of_eyeballs`, `beating_heart_jar`, `bat_in_a_jar`, `two_headed_snake_jar`, `hand_in_a_jar`, and (the bigger jars) `giant_beating_heart`, `tall_specimen_jar`, `specimen_tank`;
  - the item `brew_ladle`;
  - block entity types `horned_skull_cauldron`, `candelabrum`, `enchanted_broom`, `dustpan`, `showcase`, `moth_display_case`, `oddity_jar`, `giant_beating_heart`, `specimen_vessel`;
  - item tags `jugcraft:brooms` and `jugcraft:cauldron_floaters`;
  - their recipes.
- **Feature switch**: the recipes follow the agriculture feature switch.

## Dependencies and assets
- **No new dependencies.**
- **Models**: built on `tools/flora_art.py` by `tools/decor17_data.py`, which paints their textures by code (64 × 64, or 128 × 128 for the cauldron, candelabra, broom, cabinet and moth case).
- **Art style**: the creature props (faces, bones, fur, scales, fire) are painted with the clean, cute painters in `tools/cute_art.py` since 5 October 2026, after the owner asked for simpler, smoother creatures ([ART_DIRECTION.md](../ART_DIRECTION.md#creatures-and-faces-cute-and-clean)).
- **The 128 × 128 rule**: the toolkit's texture size is now a choice. `docs/ART_DIRECTION.md` and the audit allow 128 × 128 for a sculpted prop that packs many pieces.
- **Client textures**: five small greyscale textures the client tints: brew, fume, wax, flame and glow (a flat square since 5 October 2026).
- **The bigger jars' models**: each is its small jar's model made bigger (`decor17_data.grown`: every box scaled about the jar's bottom middle, set at the middle of the prop's floor, each face's picture pinned first), on the small jar's own textures (`beating_heart_jar`, and `specimen_jar_iron`, `_glass` and `_fluid`). Each block of a multi-block prop has its own model, the prop's box cut at the block boundaries (`decor17_data.cell_part`), and a multipart block state picks it by `part` and turns it by `facing`. The heart and the specimens are the small jars' own client quads (`oddity_heart`, `oddity_heart_stand`, `specimen_*`), drawn by the small jars' renderers' code in a pose scaled up with the jar (`OddityJarRenderer.heart`, `SpecimenJarRenderer.draw`). The blocks' shapes are the small jars' boxes made as much bigger (`MultiDecorationBlock.grown`) and cut the same way (`partShapes`). `MultiDecorationBlock.box(across, up, deep)` lists the cells in the order `decor17_data.cells` counts them.
- **Moving parts and layout**: the moving parts are in `assets/jugcraft/decor17_quads.json`, and the candelabra's candle layout in `/jugcraft/candelabra.json`, which Java reads.
- **Originality**: all original. The look follows the owner's reference pictures, and nothing is traced from them.

## Verification
**The bigger jars as their small jars made bigger (6 October 2026)**: not yet built or run. This replaces the vat, the giant heart's own heart quads (`giant_heart_*`), the big specimens (`big_specimen_*`) and the 2 × 2 × 2 tank described in the entries below, and the audit's checks of them. Locally, with no Gradle and no game:
- `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py` (exit 0), `python3 tools/check_mod_data.py` (PASS, 1440 IDs) and `python3 scripts/check_repository.py` (PASS); `javac` parse-only over every Java source, no errors.
- The audit's bigger-jar check now compares the giant heart's size, tempos and pulse, and each specimen jar's light, scale and blocks, with `tools/decor17.py`; checks that each one's part models, put back together, fill its small jar's model made `scale` times bigger and draw only on the small jar's textures; that its shapes and renderer scale the small jar's (`grown(...BOX...)`, `OddityJarRenderer.heart`, `SpecimenJarRenderer.draw`); and that the Beating Heart Jar's heart, at its fullest swell and turned any way, stays inside its glass (and so the giant's inside its own).
- Drawn outside the game from the generated models and quads: the Giant's Beating Heart drawn a third the size is the Beating Heart Jar pixel for pixel, its heart and stand and all, and the Tall Specimen Jar and Specimen Tank are the Specimen Jar two and three times over. The only flicker pixels flagged are single pixels where the lid's knob meets the lid, the same in the small jar.
- Each bigger item is its small jar's, drawn 1.05 (the heart), 1.25 (tall) and 1.45 (tank) times as big in a slot and moved to the slot's middle; worked out from the models, each stays inside its 16-pixel slot.
- The server game tests now also check the shapes: the giant heart's first block holds 4.3 pixels of the jar each way, its bottom middle block is full and its top middle block 8.8 pixels tall (three times the Beating Heart Jar's 3.9 to 12.1 and 13.6); the tall jar is a full block under one 10 pixels tall and the tank's first block holds 4 pixels of it each way (the Specimen Jar's 4 to 12 and 13, twice and three times over); and the tank has 27 blocks. `BiggerJarsClientGameTests` frames the giant heart's item too.

**Review fixes (6 October 2026)**: not yet built or run. Locally, with no Gradle and no game, after the last generator change:
- `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py` (exit 0), `python3 tools/check_mod_data.py` (PASS, 1440 IDs) and `python3 scripts/check_repository.py` (PASS); `javac` parse-only over every Java source, no errors. The audit now also checks that no two faces of the giant heart's parts or of a big specimen lie on one plane closer than 0.1 pixel and that their UVs stay inside their texture (it catches the old vena cava), and that the Colossal Skull's glow matches `decor18.SKULL` and lies in front of the hollow.
- The Giant's Beating Heart's pulse: its first block's beat changed its own neighbouring parts at once, and a part finding no tick waiting scheduled a whole period, which took the one place a block has for a waiting tick; so the signal stayed on a whole period and it beat at half its tempo. It now schedules its next tick before it changes, and its parts ignore changes from the heart itself. A model of vanilla's tick rules gives beats at 30, 90, 150, 210 and 270 ticks with the signal on for 150 of 300 before, and a beat every 30 ticks, on for 2 each (18 of 300), now. A new server game test, `giantHeartPulsesOnTheScheduler`, leaves a placed heart to the real scheduler and checks a beat every period and each signal two ticks long.
- Drawn outside the game, open-box pixels (where a box shows its inside: the sky drawn one-sided, its far walls drawn from both sides as the client draws these) over 72 views: the big eye 17,277 to 8, the brain 21,950 to 28, the tentacle 77 to 3, the pumpkin 56 to 0. The heart's flicker pixels from behind and the side over five moments of a beat: 8,667 to 293, single pixels where boxes meet (its vena cava now stands inside its atrium's back, and the two boxes rounding its sides are closed at their ends). The open Iron-Bound Coffin's flicker pixels: 3,642 to 268 (head, three views) and 1,941 to 207 (foot, two). The Egg Sac Cluster's item over 60 views: 14,755 to 11,023, what is left being the web's own gaps.
- A new client screenshot, `jugcraft_bigger_jars_items`, shows the Specimen Tank's items with each specimen and a Tall Specimen Jar's in frames. The skull glows show in the existing `jugcraft_crypt_and_ossuary_night` and `_colossus_night`, the egg sacs in `jugcraft_laboratory_larder_dining_larder`.

**Bigger jars and art fixes (5 October 2026)**: not yet built or run. Locally, with no Gradle and no game:
- `python3 tools/generate_textures.py`, `python3 tools/generate_material_data.py`, `python3 tools/check_mod_data.py` (PASS, 1440 IDs) and `python3 scripts/check_repository.py` (PASS). The audit's Witch's Workshop check now also compares the bat jar's numbers and wing reach, the cauldron's square eye glow, and (a new check) the bigger jars' sizes, tempos, pulse, lub-dub, swell anchors, lights, specimen middles, scales and bubbles with `tools/decor17.py`, that the heart sits inside the murk at the peak of its swell, that the specimens fit their fluid, that every part model exists and both renderers are registered.
- `javac` parse-only over every Java source: no errors. Nothing was compiled against Minecraft.
- Drawn outside the game from the generated models and quads, from several angles each, looking into the empty cauldron, and with see-through pixels counted (the sky showing through the back of a face): the cauldron went from 20,319 such pixels over 24 views to 1, the other props listed above from hundreds or thousands to nearly none.
- New server game tests in `WitchsWorkshopGameTests`, to run in CI:
  - `giantHeartStandsAndBeats`: placed facing north it fills 27 blocks with its block entity on the first only; with a block in the way it isn't placed; its first block beats (15 to the sides, none below, none from the middle block), stops after its pulse, changes tempo when any block is used, doesn't beat with power under its far corner, and broken from its back it goes whole and drops once;
  - `biggerSpecimenJarsKeepTheirSpecimens`: the tall jar stands two tall and the tank 2 × 2 × 2, glowing; sneak-use on a tank's upper block and the jar's top puts a tentacle in every block; a plain use changes nothing; broken anywhere each goes whole and drops once with its tentacle;
  - `workshopDataLoads` now also loads their recipes and loot.
- New client screenshots, to take in CI: `WitchsWorkshopClientGameTests` adds `jugcraft_witchs_workshop_cauldrons_inside` (an empty and a swiftness cauldron seen from above); `BiggerJarsClientGameTests` takes `jugcraft_bigger_jars`, `_giant_heart`, `_giant_heart_above`, `_specimens`, `_night` and `_giant_heart_night`.

**Batch 17 as first released:**
CI's Build workflow passed on 9447e0b7 (run 37219365133): the generated data matched, the audit passed, the mod built, every server game test passed (these eight among them), and all three client shards passed and took the screenshots below. Runs before it found, and the commits after them fixed:
- 37217854334: names that are gone in 26.3 (the dye items, the cat's hiss, `Player.drop`, `hurtMarked`);
- 37218300868: the same dye name in a game test;
- 37219004856: one game test aiming at the cabinet in floats, which can't hold a sixteenth of a block millions of blocks out; the test now aims in doubles.

Run 37218545936 did not get as far as compiling: Maven Central refused Gradle's downloads (403). It was superseded rather than re-run.

- `python3 scripts/check_repository.py` and `python3 tools/check_mod_data.py`: pass locally and in CI. The audit's new Witch's Workshop check:
  - compares the cauldron's, broom's, Dustpan's, rack's, cabinet's, bell jar's, jars' and candelabra's numbers, tempos, waxes and flames with `tools/decor17.py`;
  - checks the generated candle layout;
  - checks each block is registered, named, drops and has its recipe and texture;
  - checks that the Ember Bed is a heat source and the two item tags match;
  - checks that the quads, `DecorQuads` and all seven renderers are wired up.
- `WitchsWorkshopGameTests` (eight tests):
  1. the cauldron fills, empties, takes three of one potion and refuses a fourth or another, and gives the same potion back;
  2. over an Ember Bed it heats, and a ladle wafts long swiftness onto the four nearest of five players at a quarter duration, using one level (an instant potion doesn't waft);
  3. only ingredients float, three at most, an empty hand fishes one out, and breaking gives them back;
  4. the candelabra:
     - a table candelabrum takes purple wax and witchfire, lights at 9, snuffs, and follows redstone;
     - drips grow only while lit, and shears clear them giving nothing;
     - a floor candelabrum stands two tall and both halves light at 14;
     - a chandelier falls with its ceiling;
     - each fitting has 7, 3, 3 and 16 candles;
  5. Flying Ointment wakes the broom (the bottle comes back, a second refused), it sweeps apples by the Dustpan into it, and pushes a far bone toward it;
  6. the displays:
     - every one of the cabinet's nine places is picked by a hit on it, a skull goes onto the place used, a comparator reads it, and an empty hand takes it back;
     - the bell jar shows one thing;
     - the rack refuses an apple, hangs a broom on the peg used, and falls without its wall;
  7. the oddities:
     - the moth case changes its moths;
     - the heart's beat gives 15 to the sides but not below and ends after its pulse, its tempo changes with use, and it is stopped by power from below;
     - a player coming near wakes the bat;
     - the hand is powered by redstone;
  8. recipes and loot load.
- `WitchsWorkshopClientGameTests`: a dark oak workshop, by day and by candlelight: the whole room, the cauldrons close up, the cabinet with the moth cases and broom rack, the jars on their table, the broom and Dustpan, the candelabra at night, and the moths at night.

Not run:
- Building with them by hand.
- A two-player dedicated server (two players stirring one cauldron, both wafted).
- This environment can't run a game client interactively.

## World and event applicability
Placed by players only; no worldgen. They work all year, and nothing depends on the Halloween event.

## Rollout and open questions
- Open: whether the Enchanted Broom should also sweep into a hopper or a chest when there is no Dustpan.
- Open: whether wafting should also work for splash or lingering potions poured in, which it can't hold today.
- **Fixed 5 October 2026 (shared render fixes):** the Horned Skull Cauldron's hollow is built from lined walls (no solid box fills it, no see-through slits round its rim), the candelabra's drip pans, sockets and the Branching Chandelier's arm-tip drops are closed (the drops were written upside down, so they drew inside out), and the Curiosity Cabinet, Moth Display Case, Enchanted Broom and oddity jars no longer show holes where a box left a face out. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
