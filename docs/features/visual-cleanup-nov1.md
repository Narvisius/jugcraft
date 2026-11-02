# November 1 local visual cleanup

Owner selected the original-model review and reported missing-texture/aircraft
follow-up. Prepared by OpenAI Codex (GPT-6), locally on the existing audit-fix
branch. No publication, server action or personal launcher/profile change.

## Runtime geometry

112 defining model templates receive bounded surface separation. The shared
templates cover furniture, bridge rails, holiday decorations, backpacks, jars,
plants, lanterns, hanging baskets and held weapons. Original images, materials,
explicit UVs, rotations and animation files remain unchanged. Bounds move at most
0.3 model pixels; most corrections are 0.1 pixel or less. Hanging basket handles
are trimmed by 0.02 pixel, retaining both inward and outward surfaces.

`tools/imported_geometry_repairs.json` records exact before/after element hashes,
changed elements and reasons. `tools/imported_geometry_repairs.py` applies them
idempotently at atlas generation, refusing unreviewed source geometry drift.
Owner-library originals are untouched. Reimporting sources followed by normal
atlas generation reapplies the same bounded changes.

The checker now accounts for opaque rotated occluders in their actual local
coordinates, opaque texels of coplanar cover panels, identical rotated overlay
quads, and the exact centre seam of native Jugtania WallBlock arms. Negative
controls still reject partial/transparent/reversed caps, exposed corners outside
rotated solids and missing exterior wall faces. Adding duplicate lamp caps and
book covers was rejected during review because their authored panels already
close those surfaces; no such duplicate faces remain in the runtime models.

The full block-model scan checks 38,254 distinct models: the original 708 findings
are reduced to 48 possible open-surface findings, with zero coplanar or
near-coplanar overlap findings. These 48 remain visible in the review report;
they are not silently exempted or described as 48 confirmed defects.

## Texture and aircraft coverage

The earlier atlas/banking repair is already present in this checkout. No second
bank-sign reversal is applied. Aircraft rendering exposes its actual body-pose
transform to the client regression; both turns must lower the correct wing at
four headings on each of the seven aircraft. Original animation, sounds, camera
offsets and control behavior remain intact.

The texture test retains runtime atlas and baked-block checks and the reported
27-item gallery. An additional 128-item gallery includes repair representatives,
storage-cell tiers, paintballs, presses, cores, quantum content, tools and spell
books. The newer collection test includes side/back views of representative
assembled decorations. JEI page numbers are not stable identifiers.

## Validation

- Eight focused Python regression tests passed (bounded patches, idempotence/drift,
  rotated overlays, rotated occlusion, flat panels, native wall joins effective runtime material fixture freshness and legacy-renderer misuse).
- Atlas generation completed without missing required texture references.
- All 263 changed main-resource files matched the compiled resource output before
  the rendered test rerun. The rerun skips only the resource-copy task.
- First client launch stopped at Gradle's offline asset download, before Minecraft
  opened. The pinned development asset download then succeeded. It did not modify
  a player's Modrinth profile.
- The first rendered audit stopped on unused parent-template material defaults.
  The corrected audit checks all existing imported sprites plus a separately
  validated list of effective materials after native child overrides; missing
  effective materials still fail. The fixture is checked against current models.
- Corrected rendered atlas audit passed: **20,126** unique textures stitched and
  **360,887** Jimtafacts block states resolved without missing face sprites.
- Visually reviewed the reported-item screenshot and all three 128-item gallery
  pages: presses, cells, paintballs, quantum pieces, cores, spellbooks and repaired
  decoration representatives render without the missing-texture checkerboard.
- Combined rendered run **passed** (`build/nov1-visual-client-final.log`, 6m07s):
  ImportedModelAtlasClientTests, AircraftFlightClientGameTests and
  NewestCollectionClientTests. All seven aircraft passed both steering directions,
  actual transformed wing orientation at four yaw headings, F5 distance and sound
  resource checks. Newer collection: **837,276 states / 742,135 distinct models**.
- Reviewed representative bamboo/jungle lamps, basket, paper lantern, crystal
  cube, spreader, backpack, awning and joined oak-stump views. Original shapes
  remain; joined stump faces close in the tested L-shaped assembly. This does not
  prove every orientation/state of every imported component.
- Screenshot review caught a broken **lunch-basket inventory/Jade icon** that
  exception-only item resolution missed. Its ordinary item still used a legacy
  `builtin/entity` parent without a native special renderer. It now inherits the
  supplied closed-basket mesh. A repository-wide test checks ordinary native item
  branches for this mistake, excluding legitimate special-renderer bases. The focused
  rendered rerun passed in 3m52s; its final gallery page was visually reviewed and
  shows the basket mesh correctly. Quartz bench/sofa particle aliases were also
  changed from invalid `minecraft:block/quartz_block` to the actual vanilla side
  texture. Final expanded atlas verification includes vanilla materials too: **20,394 references resolved**, with no remaining `Missing textures in model` log entries. The final focused client run passed in **3m45s** (`build/nov1-final-atlas-client.log`).
- Repository structure/local documentation-link check and diff whitespace check
  passed. Eight focused Python tests pass after the basket correction.

The earlier eight-audit-fix batch remains intact. These notes do not claim a full
release suite, live deployment or owner multiplayer acceptance.

## Retained limitations

The supplied Undying Charm is a single 16x16 PNG, both in the owner library and
in runtime resources, but its supplied animation metadata requests six frames.
Minecraft rejects the five absent frames. This is recorded separately for source
animation completion; no replacement frames or changes to the supplied image
were invented in this model/texture cleanup. Some unused template/special-renderer
particle aliases still produce loading warnings; resolved block faces and effective
ordinary materials are checked separately. The 48 component-level open-surface
diagnostics remain in the report for exhaustive assembled-state acceptance.
