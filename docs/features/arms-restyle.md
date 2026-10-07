# Arms restyle: pixel-art icons and 3D models in the hand

Status: implemented on `claude/arms-restyle`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "Refine all the weapons make them look better, study how other people have made weapons that actually look good and copy that currently they look AWFUL", with Simply Swords, Epic Knights and RPG Style More Weapons attached to study ("i like these too for more unique weapons").
Owner: jimbozoomer-byte
Target milestone and tier: art only, for every arm of batches 42 to 55 ([arms.md](arms.md) to [arms-vi.md](arms-vi.md)).
Primary specialty and supported player role: fighting (looks only).

## Player experience
Every arm (34 kinds in bronze and steel) and the Arms VI kit are redrawn:

- **Inventory icons:** crisp pixel art, 32×32 (48×48 for the great arms and polearms), drawn on the exact 45-degree pixel diagonal in flat tones, lit from the top left, with a one-pixel outline. Each part reads on its own: blade, guard, grip, pommel, haft, head. These icons show in inventories, item frames, on the ground and on shelves.
- **In the hand, a 3D model:** each arm is built of boxes with real thickness: a thin blade with a raised ridge or a sunk fuller, chunky guards and pommels, round grips and hafts, deep heads on hammers and maces. It is shaped and painted from the same design as its icon and lies exactly over it, so the arm is held as before.
- **The palettes:**
  - Bronze arms are warm copper-bronze with yellow brass fittings, leather grips and oak.
  - Steel arms are blued steel with gunmetal fittings, black rubber grips, dark wood and brass rivets.
  - The ornate arms carry a garnet (bronze) or a lit green phosphor stone (steel).
- **The longbows and arbalests** are redrawn the same way, as vanilla lays out its bow and crossbow. The shields' faces are flattened to clean tones.
- **Refinement pass (the owner: "refine"):**
  - Straight blades are bevelled: the lit face a tone lighter than the shaded one, meeting down the spine, with a glint near the point.
  - The pike has iron langets below its head.
  - The quarterstaff has iron-shod, studded ends and more bands.
  - The small arms are held larger, so they read in the hand: kama 1.15 (was 0.95), war pick 1.25 (1.1), katar 0.95 (0.85), kusarigama 1.15 (1.0).
- **Second pass (the owner: "make sure they are done similarly to those other mods … dont overcomplicate them"):** every icon was checked against the studied style at 8× and anything cluttered taken out.
  - **Hilts that read as blobs:** the rapier is now plain quillons and a knuckle bow, without the cup ring. The sabre's D-guard stands clear of the grip, so the gap shows.
  - **Heads too small or busy:** the halberd's axe is larger, a long edge between two horns. The bill has a bigger hook and spike, without the back fluke. The pike has a broader leaf and no tassel. The war hammer is a squared face, a tapering beak and a spike.
  - **Stray details:** no single-pixel rivets on the maul, battle axe and war hammer heads, and no stone in the war pick's head. The katar has one cross grip, not two, and no stone.
  - **The kusarigama's chain** swings out clear of the handle, four links and its weight, rather than lying along the grip.

## The flail's head swings (5 October 2026)
The owner: "flails should have an animated ball that actually flails arounds if possible". The flail's chain and ball were part of its one in-hand model, so the whole flail turned as a rigid stick: the chain stood out sideways and the ball never hung, trailed or whipped.

**Tier, inputs, outputs, costs:** art only, for the bronze and steel flails ([arms-ii.md](arms-ii.md)) and the Bonecarved Flail ([arms-vii.md](arms-vii.md)); no recipe, number, id, tag or component changes. Input: what an entity holds and how its arm moves (the arms motion, [arms-motion.md](arms-motion.md)). Output: the chain and ball drawn each frame. Cost, on the client only: for each entity holding a flail, two item-model lookups, a few 4×4 matrix products, a chain of six points stepped in at most eight sub-steps of eight constraint passes (each pass tests the five moving points against the body's boxes: at most 22 box tests, each a few dozen multiplications and three sines and cosines), and six small item models drawn a frame; nothing per tick, nothing on the server, no packets.

**Player experience:**
- **In the hand** the flail is its handle, ending in an eye, and the chain (four oval links, each turned a quarter to the last) and the spiked ball hang from the eye and move on their own: they hang under gravity, trail as you walk, turn or look round, are flung round overhead and whip past after a blow, then swing on and settle in about a second and a half.
- **In first person** the flail's guard is held higher (`tools/arms_moves.py` `FL_FP`), so the ball hangs in sight; it swings as you turn your head or strike, and never closer than 0.45 blocks to your eye.
- Players, armor stands and mobs, in either hand and left-handed; the enchantment glint shows on the chain and ball too.
- **The ball:** a rounded core of four boxes (a cube and three crossing slabs of different sizes), a crown of eight spikes round its middle (four square to it and four between them, each turned about the upright only) and one below, and a brass lug where the chain hooks on, about as big as the old one. 15 boxes, in two tones (the game shades the faces too), each spike a single box with its point in the light tone: a stepped core, two-tier spikes and spikes turned about several axes speckled at a glance. A link is four boxes.
- **It keeps out of its holder** (third person): the ball and the links keep clear of the holder's own body as it is posed that frame. Walking, the legs and the arm swing through where the ball hangs, and it is pushed aside as they come, so it no longer sinks into a hip, a thigh or an arm (at most a spike's tip grazes). A ball found already inside a part, which a fast overhead blow can sweep the arm or the head through within a frame, is left to pass through rather than thrown out through the nearest face, which may be the far one.
- **The Bonecarved Flail** swings the same way: a spine of five vertebrae (each a round body on a cord, with side processes and a spine at the back, all facing one way) and a horned skull in the house's clean creature style: square plum eye sockets as Minecraft's skulls have, no nose holes, a row of square teeth on the jaw, two horns from the temples and a horn lug on the crown (9 boxes; a vertebra is four). Its face looks the way the haft points.
- **The icon is unchanged.** A swinging icon was drawn with an earlier version of this work and left out: [PR #201](https://github.com/jimbozoomer-byte/jugcraft/pull/201) redraws the arms' icons in the owner's own 16×16 style, and the two would clash.

**How it works:**
- **Generators.**
  - `tools/arms_art.py`: `flail_handle` is the handle and its eye (`FLAIL_EYE`); `HANDLES` makes a kind's 3D model its handle alone (`model_design`), laid out exactly as the whole flail was (`layout` still fits the old full design), so the hand holds it where it always did.
  - `tools/arms_variants_art.py` does the same for the Bonecarved Flail (`HANDLES`, `bonecarved_flail_handle`, `model_design`).
  - `tools/arms_heads.py` writes `models/item/arms_flail_link.json` and `arms_flail_ball.json`, and `bonecarved_flail_link.json` and `bonecarved_flail_ball.json` (`VARIANT_HEADS`; no parent, no display transforms), each metal's `<item>_link` and `<item>_ball` on its own `_model` texture, whose free bottom-right corner holds flat swatches (chain, blade and fitting tones, a face's tone set by which way it faces; the skull's face and teeth just above them), and `assets/jugcraft/arms_heads.json`: per item, the eye and the grip in model pixels, the links' pitch and twist (a quarter turn for chain links, none for vertebrae), the lug, the ball's reach, the haft's radius and the handle's four hand poses.
  - `items/<metal>_flail.json` and `items/bonecarved_flail.json` are a `custom_model_data` select: `flail_link` and `flail_ball` give the parts, anything else the flail as before.
  - The `_model` texture stays one frame.
- **Java, client only** (`client/arms/FlailHeads.java`, hooked into the arms motion's mixins: [arms-motion.md](arms-motion.md#mixins)).
  - Each frame, for each hand holding a flail, a render-only copy of the stack carrying `flail_link` or `flail_ball` is resolved into an item render state: the same atlas, light and glint as the flail.
  - The chain is a Verlet chain: gravity 0.05 blocks a tick squared, 92% of its speed kept each tick, sub-steps of at most a quarter tick, links that only resist stretching (eight passes, then each point drawn back to its link's length so links never part), a heavy ball, kept clear of the haft and of the holder's body.
  - Third person: simulated in the body's upright root frame. The root is kept as the arm starts to be drawn, the item's frame where it is drawn; the body's turns and moves since the last step are taken out of the chain, so it keeps its place in the world.
  - First person: simulated in the world's axes about the eye, from the camera's pitch and yaw; the hand's frame is kept after its pose and its swing, and the head drawn as the hand pass returns.
  - The hand poses are applied as the game applies an item model's display transform (move, turn about x, y then z, scale, centre; a left hand mirrors the x move and the y and z turns), so the chain meets the handle's eye in either hand.
  - **The holder's body:** `ArmsHumanoidModelMixin`, after vanilla's pose and the arms motion's, calls `FlailHeads.pose`, which keeps the head's, torso's, arms' and legs' pivots and turns for that render state. Each part is a box about its pivot (a player's, in model pixels), grown by the outer layer (a jacket, a sleeve, trousers) and turned as the model turns it (z, then y, then x); a point is tested in the part's own frame. The ball's middle keeps its spikes' reach from every part (`BODY_CLEAR`), a link its half width (`chain_radius` in `arms_heads.json`) from the head, torso and legs (not the arms: the chain hangs from the hand). A point near a part is pushed straight out of it; a point inside one is left. The body is measured as `HumanoidModel.setupAnim` leaves it, with a player's parts: a model that turns its limbs again afterwards (an armor stand's set pose, a zombie's raised arms) is measured before that, so there the ball keeps clear of where those limbs would hang. A holder whose model is not a humanoid's, or a baby, has no body measured, and its ball swings free; a head drawn before its body's pose is known uses the body as last posed, a frame old at most.
  - **Once a frame, a view per render state:** a chain steps only when its time has moved on. A frame fills the world's render state and the inventory's paper doll's (read a whole tick ahead) before it draws either, so each state keeps its own view of the heads (`FlailHeads.View`: the model's root, the body's yaw, whether it is tilted, whether it is the doll's, the posed body), and neither overwrites the other's. The doll's view never steps the chain: it draws it as the world left it, on the doll's body.
- **Failure behaviour:**
  - A missing or unreadable `arms_heads.json` is logged, and flails show their handles only.
  - Anything thrown while drawing a head is logged once, and heads are switched off for the session (handles only), rather than breaking the frame.
  - A mixin target missing in a future version stops the game at start (`defaultRequire` 1); every target is one the arms motion already uses.
  - Beyond 32 blocks, or while the body is tilted (swimming, gliding, lying, spinning in a riptide, dying), the ball hangs still.
  - A chain unseen for a second, or moved more than two blocks at once, or no longer finite, starts again hanging.
  - The ball does not collide with the world or with other entities, only keeps clear of its own holder's body; another mod's renderer, the head slot or an item frame shows the handle (frames, the ground and shelves show the icon).
- **Data check** (`tools/check_mod_data.py` `check_flail_heads`): `arms_heads.json` is what the generator writes; the link and ball models exist, have no parent, and no two faces facing one way lie within 0.1 pixel of one plane (rotated boxes compared after their turns); the flail's in-hand model reaches no further than its eye; each flail's item definition picks its parts; its `_model` texture is one frame; `FlailHeads` names the same strings; the mixins (`ArmsHumanoidModelMixin` calling `FlailHeads.pose` among them) and the client start-up call it.

**Verification:**
- **Local:** the generators and `check_mod_data.py` and `check_repository.py` pass; `javac` parses every source file. A line-for-line Python port of `FlailHeads` (frames, hand poses and physics) was driven by the flail's real attack clips (`tools/arms_motion.py`) and drawn with the real handle, link and ball models: the chain meets the eye in every frame, hangs straight down at rest, links never part, the ball whips past after each blow and settles; a turning or walking body swings it out; a paper doll drawn between world frames does not disturb it. With the body collider ported too and vanilla's walking leg and arm swing, the ball walking at full speed sank up to 0.22 blocks into the near thigh without it and at most 0.012 (a fifth of a pixel: a spike's tip) with it; through the overhead and side strikes it comes at most 0.03 blocks inside the arm or head, for a frame or two. Not a game test.
- **CI** (not yet run): `FlailClientGameTests` checks the heads load, the ball hangs below the eye on guard (third person and first person) and the chain never parts through a strike, and takes `jugcraft_flail_bronze_guard`, `jugcraft_flail_steel_guard`, `jugcraft_flail_bonecarved_guard`, `jugcraft_flail_strike_3` and `_8`, `jugcraft_flail_walk` (from behind, walking off: it fails if the ball came more than a pixel, 1/16 block, inside the holder's body, or never met a posed body), `jugcraft_flail_turn`, `jugcraft_flail_stands` (an armor stand with a flail in each hand), `jugcraft_flail_inventory` (the paper doll), and `jugcraft_flail_first_person_guard` and `_strike`. It is kept short (eleven shots and about 270 ticks of waiting, from thirteen and about 350), as it runs in the busiest client shard. `ArmsMotionClientGameTests` adds the flail to its first-person shots, and its third-person `jugcraft_motion_flail_*` and `ArmsClientGameTests`' racks show the head.
- **Not run:** play; frame-time measurement; a second client; whether the left hand's mirrored pose matches the game's exactly (the off-hand armor stand shot will show it).

**Owner choices:** the higher first-person guard; whether the icon should swing too, once the 16×16 icons have landed; the ball's size, its spikes and the chain's length (`arms_heads.HEADS`); how lively the swing is (`FlailHeads.GRAVITY` and `KEEP`); how near the body the ball may come (`FlailHeads.BODY_CLEAR`).

## How it was studied
- **Simply Swords and Epic Knights (studied, not copied):**
  - 32 and 48-pixel sprites along the diagonal;
  - three to five interior tones a part, a bright edge facing the light and a dark one away from it;
  - a light outline on the lit side and a black one on the other;
  - about ten colours an item, no dithering or noise;
  - in the hand, scaled up 1.7 to 1.9 times.

  Our old sprites were 64×64, rendered with soft shading, dithering and glints, with no outline. That is why they read as muddy.
- **RPG Style More Weapons (studied, not copied):** weapons built of 40 to 85 boxes with painted textures, ornate guards and dark grips, real thickness.
- **None of their textures, models or code is used,** as the repository's rules require. Every design here is original, drawn by code in `tools/arms_art.py`.

## How it works
- **One design per kind:** `tools/arms_art.py`, along the weapon's own axis, in parts (strips with a profile, polygons, discs, rings, bars). Each part has a material and a thickness. The designs are about 28 units for each unit of the kind's in-hand size, so every arm in the hand has the same texel size.
- **The toolkit, `tools/arms_pixel.py`:**
  - draws the icon on the pixel diagonal: tones from each part's distance to its lit and shaded edges, then the outline;
  - bevels a part when asked (light on one side of its bevel line, mid on the other) and adds glints, one pixel near white;
  - draws the upright texture for the model;
  - builds the model's boxes: greedy rectangles per thickness, each turned 45 degrees about the hand so the model lies over the icon. Front and back faces are textured from the upright image, the sides from its edge texels.
  - At most 32 boxes a model.
- **Icon layout:** each icon is fitted whole, a pixel in from its edges. A wide head makes the rest smaller, and the hand pose makes up for it (`hand factor`), so every arm is as long in the hand as before.
- **Models:**
  - `models/item/arms_<kind>.json` is the 3D model, with vanilla's sword poses, or the spear's for the spear and lance, scaled as before.
  - `models/item/<item>_in_hand.json` textures it (`textures/item/<item>_model.png`).
  - `models/item/<item>.json` is the icon.
  - `items/<item>.json` picks the icon for the GUI, ground, frames and shelves, and the 3D model otherwise.
  - The brazier mace's flame flickers in both (its model is shaped to fit all four frames).
- **The data check** now allows 48×48 textures (`check_mod_data`), which the long arms' icons use. `docs/ART_DIRECTION.md` says so.

## Connections
None changed: no recipes, numbers, ids, tags, components or Java. Every arm, its tooltips, its motion and its weapon art are as before.

## Balance and automation
Not applicable (art only).

## Multiplayer and persistence
- **Saved state:** unchanged; ids are stable.
- **Removed:** the textures `textures/item/<bronze|steel>_<spear|lance>_in_hand.png`. The 3D models replace them, and no saved data refers to a texture.
- **Client-only:** the in-hand models are plain JSON item models, with no new rendering code.

## Dependencies and assets
- No new dependencies.
- The art is generated: `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py`.

## Verification
- **`python3 tools/check_mod_data.py`:** PASS (local).
- **`python3 scripts/check_repository.py`:** PASS (local).
- **Every arm model is checked to be within the JSON model limits:** coordinates −16 to 32, ±45° turns, UVs 0 to 16.
- **CI:** the client tests' racks, frames, held-arm, two-handed, weapon-art and Arms VI screenshots show the new art in game. Results are in the pull request.
  - **The run on fdae38d2** showed every kind's model in third person and ten in first person. The scythe's blade point floated apart from the blade, so the toolkit now joins pieces that per-texel sampling splits.
  - **The run on fb178b85** shows the scythe and kama blades whole in first and third person, and the flail, kusarigama, rapier and twinblade in one piece.
- **Not run:** play; how the models look in every pose of the arms motion.

## World and event applicability
Not applicable.

## Rollout and open questions
- **Each design is a first pass for the owner to judge:** proportions, palettes and details are easy to change in `tools/arms_art.py`.
- **Possible next steps,** each needing the owner's choice:
  - more ornate variants;
  - glowing parts (an emissive layer);
  - the same restyle for the power katana, power bow and other weapons outside the arms.
