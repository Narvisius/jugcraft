# tools/data: source data for the generators (third-party, and a few of our own renders)

## makehuman_head.npz (the Holo Projector's head)

- **Source:** the MakeHuman base mesh and three macro targets from the MakeHuman repository,
  https://github.com/makehumancommunity/makehuman (master branch, its data folder):
  - `3dobjs/base.obj`;
  - `targets/macrodetails/african-female-young.target`;
  - `targets/macrodetails/asian-female-young.target`;
  - `targets/macrodetails/caucasian-female-young.target`.
- **Author:** the MakeHuman team and community.
- **Licence:** CC0 1.0 (public domain dedication). Each file's header says: "This asset was explicitly released as CC0 in
  september 2020".
- **Fetched:** 22 October 2026, with the owner's permission.
- **What was done to it** (`tools/holo_head.py` `load`):
  - The three female targets were applied a third each.
  - The vertices over the shoulders (y > 5.0) were kept with their faces, along with the upper and lower teeth helpers
    and the eye joint groups.
  - Stored as numpy arrays: `v` and faces by group.
- **Shaped for Jugcraft** in `tools/holo_head.py`, `tools/holo_head_hair.py` and `tools/holo_projector.py`, after the
  owner's character sheets and his notes on six mockups:
  - brushes for a feminine, angry face, slimmer cheeks and a narrower chin;
  - the neck cut level and a fading skirt;
  - subdivision;
  - hair, brow and lash cards and the choker, made here;
  - the visibility tables.
- **Output:** `src/main/resources/assets/jugcraft/tower_mesh/holo_head.bin.gz` and
  `textures/entity/holo_head.png`. Covered by Jugcraft's MIT licence as far as our changes go; the base data stays CC0.

## Fonts

The Holo Projector's ring of solid letters (`tools/holo_head_letters.py`) is meshed from DejaVu Sans Bold
(`/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf`, or the font named by `JUGCRAFT_HOLO_FONT`).

- Licence: DejaVu's changes are public domain; the Bitstream Vera glyphs they are based on are under the Bitstream
  Vera licence, which allows free use and modification.
- Only the outlines of the 16 letters used are kept, as triangle meshes in `holo_head.bin.gz`. No font file is shipped.

## desk_pages/ (the room kits' laptop and monitor screens)

- **Source:** Jugcraft's own: the tower console desks' live pages (`client/DeskPages.java`, drawn by
  `client/DeskScreen.java`) rendered off-screen at eight times their 246 x 149 size, then halved (984 x 596). Named
  `<role>_<page>.png` as the desks number them (`tower/ConsoleDeskBlock` ROLES).
- **Used by:** `tools/room_kit.py` (and the kits it runs: `training_kit._page_into`) for the pictures on the upper
  floor's laptops and monitors; only the 19 pages the kits show are kept.
- `16_0.png` is the Lounge TV's channel, TOWER TV (`client/TowerTv.java`, the desks' page role 16), drawn the same way
  at 984 x 557 (the TV's 16:9 glass, 26.5 x 15 px): what the TV shows from further off than it is drawn live.
- **Licence:** the project's own.

## sim_world/ (the Pilot Simulators' views)

- **What:** the six simulators' three-screen views (`pod_<n>_320x188_live.png`) and the instructor's repeaters
  (`rep_<n>_550x307_live.png`), drawn by `tools/sim_world.py` from its seeded world with the game's own block textures,
  their HUDs' changing figures (speed, height, battery, wind) left out; beside each, its `.json` lists those figures'
  boxes and readouts, which the game draws live (client/LiveFaces).
- **Why kept:** so the generators (`tools/room_extra_kit.py`, `tools/sim_kit.py`) run without the game's textures; to
  draw them again, delete them and set `JUG_VANILLA` to the client jar's `assets/minecraft/textures/block`.
