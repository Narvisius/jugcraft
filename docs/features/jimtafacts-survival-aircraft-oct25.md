# Jimtafacts survival rewards and aircraft repairs

Local work by OpenAI Codex, 25 October 2026. Not released or deployed.

## Survival routes

Existing source loot is preserved; these additional reward pools use native
Jugcraft balance values rather than claiming to reproduce missing source rules.

| Item | Route |
| --- | --- |
| Arcane Debris | Buried Nether ore, Y 8–32, two placement attempts per chunk and up to two blocks per vein |
| Untranslated Archevoker Logbook | Archevoker killed by a player |
| Translated Archevoker Logbook | Crouch-use the untranslated book on an enchanting table, with one Ancient Knowledge Fragment in the other hand |
| Ancient Furled Map | Evoker Fort chest, 35% extra reward |
| Legendary Spell Book | Dead King participant reward, 25% |
| Tarnished Helmet | Dead King participant reward, 25% independently |
| Lurker Ring | Catacomb hidden trough treasure, 20% |
| Whispers of Ice disc | Ice Spider Den dungeon treasure, 10% |
| Unchained Book | Citadel tome loot, 35%, including the bookshelf table that references it |

Both ordinary and ominous Dead King rewards use the existing participant rules.
Translation respects claims and consumes one fragment in survival. The translated
book has eight readable pages. Ordinary debris drops one Pyrium Ingot; Silk Touch
retains the block and Fortune does not multiply it.

Ore appears in newly generated chunks only. Existing opened treasure containers
are not refilled, and existing terrain is not regenerated.

## Aircraft

- Imported Blockbench UVs use each texture's logical UV dimensions. The supplied
  biplane PNG is 128×128 but its UV grid is 16×16; using the PNG dimensions sampled
  the wrong section of the atlas. Original textures remain unchanged.
- Steering controls interpolate, and powered turns bank the aircraft. Supplied
  rudder, elevator and propeller animation expressions remain in use.
- Engine loops start at the aircraft's actual position and stop when their
  aircraft is removed or leaves the current client world. Passengers hear a
  listener-relative engine loop so the extended F5 view does not mute it;
  observers still hear spatial audio.
- F5 distance scales with aircraft size, retaining vanilla obstruction checks.
  First-person view is unchanged.

Vertical slabs are explicitly outside this batch at the owner's request.

## Validation

All eight Python reward, UV and acquisition-graph tests passed together. Common,
client and game-test sources compile. All 146 Jimtafacts server game tests passed
on October 25, including all four new survival tests. The ore regression caught
the inappropriate regular-vein generator for this tiny deposit; the final data
uses scattered ore and tests verify actual block placement and preservation of
building materials. All 11 aircraft/furniture server regressions also passed.
The rendered flight test passed for all seven aircraft, asserting powered
steering, interpolated banking, passenger engine sound state and front/back F5
distance, with 14 screenshots. Visual review prompted a wider Bamboo Hopper
camera; the final seven-aircraft rerun passed and its screenshot now shows the
aircraft fitting in view. Audio state was checked, not listening
quality on a physical speaker/headset. No two-client multiplayer flight test
was performed.

No live-server or personal play-instance files have been changed.

Local commands and evidence:

- `python -X utf8 -m unittest scripts.tests.test_jimtafacts_survival_rewards scripts.tests.test_survival_recipes -v`
- `gradlew runGameTest --offline -I build/group-a-aircraft.init.gradle`
  (filter `jugcraft-test:jimta*`, 146 tests).
- `gradlew runGameTest --offline -I build/group-a-aircraft-server.init.gradle`
  (filter `jugcraft-test:owner_furniture_aircraft*`, 11 tests).
- `gradlew runClientGameTest jar --offline -PclientTests=AircraftFlightClientGameTests`
- Logs: `build/group-a-acquisition-final.log`,
  `build/group-a-server-tests-verified.log`, `build/group-a-aircraft-runtime.log`,
  `build/group-a-aircraft-visual-final.log`.
- Screenshots: `build/run/clientGameTest/screenshots/*aircraft_oct25*`.

Build uses the repository's pinned Minecraft 26.3/Fabric dependencies and Java
25. Tests create disposable local worlds. This work is based on main revision
`7317e5d44ac520d8cd04cb933f6756d8905bc239`; changes are still uncommitted locally.
