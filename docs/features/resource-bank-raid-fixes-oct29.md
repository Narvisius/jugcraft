# October 29: imported art, aircraft banking and city raid approaches

Local follow-up to player reports against 20261029.1. Prepared with OpenAI Codex.
No dependency updates, live world edits, client-channel promotion or server restart.

## Original imported art

863 model-referenced textures were absent from the block/item atlas sources:
415 AE network textures, 363 spellbook textures, 51 construction textures,
21 entity sheets used by item/block models, six magnetizer faces, six nuclear
charge faces and one statue sheet. The PNGs already existed; their geometry and
UVs are retained. Explicit sprite sources make these available to the model
baker without stitching unrelated GUI sheets or all entity textures.

`tools/model_atlas.py` runs at the end of material generation so subsequent
imports cannot silently drop the atlas entries. It is additive and idempotent,
and fails if a newly referenced texture lacks a runtime file. A final metadata
pass supplies particle materials from existing face textures and the actual
Gecko skins. It also repairs Firefly Jar's inappropriate cube parent, the iron
fence's diagonal texture alias, and the renamed vanilla iron-chain reference.
The Mimic's vanilla chest sheet is explicitly stitched. The JE Load Tester uses
a byte-identical copy of its supplied texture in the block atlas, fixing the
client's rejection of a block that previously referenced the item atlas.

The offline item/blockstate graph has 38,048 roots, 38,764 reachable models,
and no missing model/parent files. The unused imported Lava Lily item model
references geometry absent from the supplied source, but no registered item or
blockstate reaches it; it is not replaced with invented artwork.

## Aircraft

Source meshes face positive Z; the pilot's left wing is positive X. A positive
Z roll raised that wing during a left turn. Reverse the body bank and the
warship envelope's extra roll. Retain smoothing, yaw controls, original rotor
and rudder animations, UVs, sounds and camera offsets.

## City raids

New raids gather along the east, south or west gate approaches. Ground troops
use exterior waypoints and the authored gate passages instead of a diagonal
line to the city center. Existing off-axis ground raids can use the same route
without changing saved raid data. The outer route stays west of the Styx estate,
and older raiders approaching from beyond it detour around its perimeter. Walkers stop in the open forecourt because
their 4.6-block height cannot clear the raised portcullis.

Navigation prefers road elevation under the roof, checks loaded destinations,
and no longer forces MoveControl into an unreachable wall. Failed paths retry
at bounded intervals. Melee troops ignore unreachable battlement targets. Foot and mecha guards move to the threatened gate; flak
crews keep their stations. Guards' local acquisition range is 48 blocks, and
an occluded mecha cannon target no longer prevents navigation until the next
target refresh. Existing invulnerability and civilian/player protection remain.

No building, wall, claim, decoration or owner-placed block is edited. Terrain
or player-built obstacles can still obstruct a route; this does not authorize
raiders to destroy or teleport through them.

## Validation

Local checks on October 29, Minecraft 26.3 / Fabric Loader 0.19.5, the pinned
Complete dependencies, Java 25.0.4.1:

- `compileJava compileClientJava compileGametestJava --offline --no-daemon`: passed.
- `runGameTest` filtered to `jugcraft-test:raid_approach_game_tests*`: both passed.
  Checks all three authored passages, 24 approach bearings, and actual infantry
  navigation through a cropped copy of the east gate (including its roof).
- `runGameTest` filtered to `jugcraft-test:city_defense_game_tests*`: all five passed.
  Covers bystander protection, invulnerable guards, flak and walker firing,
  animation/ballistics and bank access.
- Offline reference graph: 38,764 reachable models resolved. Atlas coverage:
  all 11,804 unique model texture references covered; generation is idempotent.
- `runClientGameTest -PclientTests=ImportedModelAtlasClientTests,AircraftFlightClientGameTests`:
  passed. Verified 11,805 runtime model texture IDs (including a test resource),
  a gallery of 22 reported items, both turn directions on all seven aircraft,
  engine-loop resources and both F5 views. The gallery and biplane left/right
  screenshots were visually inspected.
- Final `ImportedModelAtlasClientTests` recheck: passed after the metadata fixes.
  Resolved 11,818 unique runtime textures and 360,887 Jimtafacts block states
  without missing baked faces. The expanded 27-item gallery was visually
  inspected. No missing-texture, unresolved-texture or rejected-block-model
  warnings remained in this run. Existing OS performance-counter, anisotropic
  option and optional RPG Series loot-tag warnings remain outside this fix.
- Final raid-route recheck after the Styx perimeter adjustment: both tests passed
  (2.113 seconds in-game; Gradle completed successfully).

Logs include `build/oct29-resource-recheck.log`, `build/oct29-client-fixes-tests.log`,
`build/oct29-defense-tests.log` and `build/oct29-raid-tests-final.log`; the graph summary is
`build/oct29-model-reference-audit.json`. JEI page numbers are not stable
identifiers; use the named item families and gallery as the regression scope.
This is focused local validation, not a full release suite or live-server test.
