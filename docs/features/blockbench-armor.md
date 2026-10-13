# Armor in Blockbench: the owner's sets as Blockbench projects, rebuilt from their renders

Status: implemented on `claude/festive-volta-4fa6fw` (stacked on `claude/epic-pascal-f8gpfg`, where the armor sets were
made). The data checks pass; **not compiled in CI on this branch yet, not game-tested and not played.**
Proposal issue: none. On 8 October 2026 the owner asked: "Can you use blockbench to make the armors I was making ...
better and more accurate to the images I had provided?" Earlier the same day they had written of the Sentinel's
pauldron "can you just 1:1 copy the pixel art from the source I gave you".
Owner: jimbozoomer-byte (designs: the owner; implementation: Claude Opus 5.5).
Target milestone and tier: tooling and art for the existing armor-only tiers ([armor-designs-8-october.md](armor-designs-8-october.md)); no new items or tiers.
Primary specialty and supported player role: combat (defense), cosmetic. Everyone who wears these sets.

## Player experience
The sets look like the owner's designs. The first one rebuilt is the **Sentinel**: its helm is the owner's 9 × 9 × 9
box with their art (the keyhole, the cheek strips, the meander behind and on the sides) copied texel for texel from
their renders; its right pauldron is the owner's bent plate, measured off their front and back views; its boots are
short gold boots whose tops rise in teeth; its coat, gorget and thigh plates follow the renders. Every texel the
renders show was lifted from them.

The second is the **Frost Knight**: its helm is the owner's 9 × 9 × 9 box with their face copied texel for texel from
their front render (the eye band turning up at its ends, the nose bar, the grille), crowned with their crystals as
measured there and coloured as they painted them, the tall one rising 14 pixels above the helm; its frost is the compact
frost the render shows, a big tuft flaring out on the left of the helm and spikes down its right, carrying the render's
texels, where the earlier model had a round mane of feathers; the ice cross is on the back of the helm; the fronts of
its legs carry the owner's texels.

The third is the **Reaper**: its white V clasp now sits just under the hood's opening, where the owner's render has it,
and its pouch is the render's big square on the left hip (it was a small pouch at the waist); the robe, its strips and
the pouch carry the render's texels.

The **Paladin** and the **Templar** are Blockbench projects too. Held against the owner's four renders, their shared
build matched part for part but for two lames under each pauldron, which the renders do not show (the mail sleeve does),
so those were taken off. Their paint stays ours: lifting texels from their lit renders, where the figures stand
differently, came out too noisy to keep.

The rest of the owner's sets of 7 and 8 October (the **Dread Knight**, **Valkyrie**, **Wayfarer**, **Spartan**,
**Berserker**, **Wight King**, **Banana** and **Scarab**) are Blockbench projects as they were: the owner called the
first four "fantastic", the Berserker "ok" and the Banana and the Scarab "AWESOME", and the Wight King's render (posed,
glowing, softened) shows nothing our build lacks clearly enough to change it. So all thirteen of the owner's sets can be
opened, looked at on a player and edited in Blockbench, and what is saved there is what the game draws.

For the owner, each rebuilt set is a Blockbench project they can open, look at on a player and edit, and what they
save there is what the game draws.

## How it works
- **A set can be a Blockbench project.** `art/armor/<set>.bbmodel` holds its cubes and its texture.
  `tools/bbmodel.py` reads it (`load_set`) into the toolkit's parts, each with its own texels, so the quad budget, the
  flicker and skin checks and the generators work on it as on any set, and the atlas is the project's texture.
- **Any set can be written as one** (`python3 tools/bbmodel.py export --set <name>`), and every set survives the trip:
  `python3 tools/bbmodel.py check` exports each registered set, reads it back two ways and compares the quads and the
  atlas with the toolkit's own. Of the 20 sets, 15 come back byte for byte and 5 within the last decimal place.
- **The project format** is Blockbench 5's Generic Model ("free"), format version 5.0: cubes turned on any axis by any
  angle, a uv rectangle per face, the atlas embedded. Its outliner is a group per bone (`head`, `body`, `right_arm`,
  `left_arm`, `right_leg`, `left_leg`, pivots where the player's are), inside each a group per worn piece named for its
  `worn_models.json` key (`sentinel_chestplate_right_arm`), and a locked grey `player` to model round, not exported.
  The mapping between Blockbench's space and the toolkit's (a half turn about z and 24 pixels up) and the uv corner
  order were taken from Blockbench 5.2.1's source (`js/util/three_custom.js`, `js/outliner/types/cube.js`).
- **Rebuilding from renders** (`tools/armor_reference.py`): the owner's renders are kept under
  [art/armor/references/](../../art/armor/references/README.md). A camera and the figure's pose are fitted to each
  (its outline against the background, then one part's outline, here the helm's); a part's shape can be fitted so the
  art lifted from one view agrees with the others (`PhotoFit`); then each face of the model takes its texels from the
  view that sees it best, its projected corners nudged until the render's texel cells are flat colours (pixel art),
  each texel the median of its cell. Faces no render shows are filled from the face opposite. Weapons and effects in
  front of the armor are left out.
- **A camera per region** when the owner's figure stands differently from ours: `<view>@<region>.fit.json` beside a
  render is a camera of its own for that region (`front@head`, `front@legs`), fitted to it alone, for the same image.
  The Frost Knight's head and legs are not where our pose puts them in the owner's front render; its helm's camera was
  fitted to the four corners of the helm's 9 × 9 face (`fit`, from points), its legs' to their outline (`fit_region`,
  whose figure is what is brighter than the fit's `thresh`, 40 for a render on a dark blue ground).
- **Frost as the render shows it:** what the owner's front render shows round the Frost Knight's helm are cut-out
  sheets facing the front, each texel opaque where at least 40% of its cell in the render is frost, so a thin spike
  across two texels keeps one, and coloured as the render's frost there. The crystals' texels, partly hidden in the
  render behind the gable, were read off it cell by cell and continued down where it hides them.
- **What the renders taught:** the Sentinel's texture is drawn two texels to a model pixel (its keyhole slit is two
  texels wide), so its atlas has density 2; its helm is a plain box with the detail painted on (the previous model had
  raised frames and ribs the renders do not have); the figure holds its sword with the right arm 10 degrees forward,
  not 21. The Frost Knight's helm is likewise a 9 × 9 × 9 box with its face painted (one texel a pixel), and its crown
  is far taller than the earlier model's: the tall crystal rises 14 pixels above the helm.

### Editing a set in Blockbench
1. Open `art/armor/<set>.bbmodel` in Blockbench (5.x). Hide the grey `player` with its eye icon to see the armor alone.
2. Edit cubes (move, resize, turn, add) inside the piece groups, and paint the texture. A new cube goes in the group of
   the piece and bone it belongs to; a cube straight in a bone's group belongs to that bone's usual piece (head: the
   helmet; body and arms: the chestplate; legs: the leggings).
3. Save the project (File > Save Project) with the texture embedded, which Blockbench does while its Embed Textures
   setting is on (the default).
4. Run `python3 tools/generate_material_data.py` and `python3 tools/generate_textures.py`, then
   `python3 tools/armor_models.py` (flicker and skin rules) and `python3 tools/check_mod_data.py`.
Keep a bone's own group at rest (not turned) and give cubes no stretch; `load_set` refuses both. Meshes are read by
`tools/bbmodel.py quads` but a set takes cubes only, since the checks need boxes.

### Commands
```
python3 tools/bbmodel.py export --set sentinel     write art/armor/sentinel.bbmodel from the registered set
python3 tools/bbmodel.py check                     every set through a project and back: quads and atlas compared
python3 tools/bbmodel.py quads FILE.bbmodel        what a project holds, piece by piece
python3 tools/armor_reference.py compare --set sentinel    each fitted render beside our model from its camera
```

## Connections
- Existing input producer: the owner's Blockbench designs and renders; the armor items of [armor-designs-8-october.md](armor-designs-8-october.md).
- Existing output consumer: `worn_models.json` and `textures/entity/equipment/3d/<set>.png`, drawn by the client's worn-model layer as before.
- Technology connection / magic connection: none new; the sets keep their tiers and stats.
- Reachable entry path: unchanged (the sets are creative-only until the owner places them).
- For infrastructure/cosmetics: this is the art pipeline of existing items; resource links do not apply.

## Balance and automation
Nothing changes: no recipe, stat or durability is touched.

## Multiplayer and persistence
Client-side art only. Item IDs, worn-model keys and texture names keep their names, except that the Sentinel's
leggings no longer draw a waist piece of their own (`sentinel_leggings_body`): the owner's coat covers the waist. No
saved data refers to worn-model keys.

## Dependencies and assets
- No new runtime dependency. Blockbench is the owner's editor; nothing of it is in the repository or the mod. The file
  format was read from Blockbench 5.2.1's source (GPL-3.0, read only, not copied).
- New art: the owner's renders (`art/armor/references/`) and the sets' projects (`art/armor/<set>.bbmodel`), whose
  texels come from those renders where they show them (the Sentinel, the Frost Knight, the Reaper). The owner's own
  work.
- This session could not run Blockbench itself: building it from source was refused by the sandbox as outside code,
  and web.blockbench.net is not reachable from it. So the projects are checked against Blockbench's source, not opened
  in it.

## Verification
Actually run on this branch, 8 October 2026:
- `python3 tools/bbmodel.py check`: 20 of 20 sets round trip (15 byte for byte, 5 within rounding), both through
  `entries` and through `load_set`. The eight sets converted unchanged: their worn_models.json entries hold the same
  quads as before (compared regardless of order), and their atlases are byte for byte the same.
- `armor_models.py`: the quad budget counts only the faces a project's cube has texels for, and a gap of exactly
  0.1 px read back from a project passes (1e-9 slack), as it did before the round trip.
- `python3 tools/armor_models.py`: no problems for any set; the Sentinel 42 parts and 237 quads (it was 96 and 518),
  the Frost Knight 102 parts and 387 quads (it was 139 and 474), the Reaper 100 parts and 553 quads (it was 552), the
  Paladin 79 and 420 and the Templar 76 and 410 (they were 83 and 444, 80 and 434). Its warnings for the Frost Knight
  are the crown and the left frost, as tall and as wide as the owner's (the tall crystal 14.6 px above the head, the
  frost 12.1 px to the side).
- A scratch check of the head poses (head turned up to 75 degrees each way, looking from straight up to straight down,
  standing and sneaking), judging faces as `armor_models.py` does on one bone (parallel, overlapping, less than 0.15 px
  apart and facing the same way, or either way when one is a cut-out): none on the Frost Knight, the Reaper, the
  Paladin or the Templar. Its first, stricter run also counted faces facing each other. On the Frost Knight it found
  the narrower helm's right side 0.05 px from the drape's and the right shoulder frost's sides, and its face and back,
  looking straight down or up, 0.1 px from the belly plate's top and the left pauldron's lower rim; on the Reaper twelve
  pairs already in the earlier model, all looking straight up or down (the face bars' fronts and the hood's back
  against the robe folds' tops, the jambs' backs and an inner side against the strap's second step). All were moved
  apart.
- `python3 tools/armor_preview.py --set frost_knight --wearer --poses stand,walk,sneak`: as before, only the arms below
  the pauldrons show (1.31 to 52.62 model px², the owner's renders show their model's plain pale blue there). The
  Reaper, the Paladin and the Templar: 0.00 model px², as before.
- `python3 tools/armor_smoke.py --no-render`: all checks passed.
- `python3 tools/generate_material_data.py` and `python3 tools/generate_textures.py`: only the changed sets' entries and
  atlases change (the texture generator also rewrote some unrelated plant textures, which are left out of this change).
- `python3 tools/check_mod_data.py`: PASS, art check allow-list unchanged (A2 1, H1 3, O1 52).
- `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py`: PASS.
- Compared with the owner's renders from their fitted cameras (`armor_reference.py compare`): the Sentinel's three,
  the Frost Knight's front from its helm's and its legs' cameras and, by eye, its back; the Reaper's front; the
  Paladin's and the Templar's two each.

Not run: opening the projects in Blockbench, the Gradle build, the game and client tests, and any play.

## World and event applicability
Not applicable: art only.

## Rollout and open questions
- The owner's own `.bbmodel` files would make every set exact. Committing them under `art/owner-library/` (or sending
  them) lets `tools/bbmodel.py` read them straight in; the rebuilds from renders are close but not their model.
- Rebuilt so far: the Sentinel, the Frost Knight and the Reaper; the Paladin and the Templar made projects with one
  change; the other eight of the owner's sets made projects unchanged (their worn models come out the same quads, in
  another order). The older sets (the knights, Bloodthorn, White Diamond, Hades, Sunset Gem, Pharaoh) stay toolkit
  modules.
- Lit renders: `armor_reference.py` divides what it lifts by Blockbench's shading when a fit says `"lighting":
  "blockbench"`, but where our parts and the owner's do not line up texel for texel (the Paladin's, the Templar's) the
  lifted art is noisy, and the faces no render shows, filled from the face opposite, can carry the wrong side's art.
- The Frost Knight's render from behind is softer and its figure stands turned, so it was not used to lift texels: the
  back of the helm is painted (the cross on mottled white), and what the front render does not show (the cuirass's
  back, the arms, the drape) keeps the earlier model's shapes and paint. Worn armor is drawn from both sides, so its
  frost sheets show from behind as well, mirrored, where the owner's Blockbench view hides them.
- Faces no render shows are filled from the faces opposite, so the inside of an arm can repeat its outside.
- Reversible: the previous Sentinel module is in Git history; restoring it and regenerating brings the old model back.
