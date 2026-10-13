# Large Drone Tower inputs

Owner-requested implementation by OpenAI Codex (GPT-6), 9 October 2026. Both tower inputs now occupy nine blocks across the front, six blocks deep and five blocks tall. The Cargo Exchange remains a completely closed black/charcoal housing; the Energy Exchange uses matching charcoal cabinets, neutral-gray mechanical assemblies and a concrete plinth. Buttons, gauges, pipe fittings and service hardware retain their physical size inside the larger equipment.

## Placement and connections

Each new machine is one inventory item placing 270 numbered cells in y/z/x order. Its one passive controller remains part 0. The original energy and cargo item IDs, recipes, technology routes, terminal energy buffer and 27-slot packager inventory remain in use. Extra structure size does not create extra stored energy or item capacity. The item preview uses the same complete footprint and blocked-cell checks as placement, including loaded chunks, world bounds, permissions and replaceability.

The marked outward port is large part 58 at canonical `(4,1,0)`, centered at model pixel `(72,24,0)`. The energy input accepts copper, aluminum and silver cable networks through the fitted gland. Brass item pipes and wall-run brass pipes feed the cargo bulkhead collar. Both external tower connection positions stay unchanged: power `(-22,2,0)` and cargo `(20,2,0)` relative to the core. New master positions are power `(-22,1,4)` and cargo `(20,1,-4)`. Live instruments retain the linked tower's exact identity and actual synchronized readings.

## Tower layout and existing worlds

New Tower Cores use layout 5, including both 270-cell machines. Four side landing pads move to supported outer-apron bays, with connected landing-platform substrate, clear railing gates and updated flight exits; the other four pads, ground pickup, terminal, packager, upper hangars, tier restrictions and fleet capacity remain intact. Chunk retention covers the new outer pads. The enlarged units fit inside the existing tower/apron envelope.

The old 64-part power and 60-part cargo footprints remain available behind saved `large=false` states. Missing size properties load as false. Existing layout 4 construction data and its ten status images are retained byte-for-byte as `tower_industrial_v4`; versions 1–3 also remain frozen. No existing world is automatically expanded over player blocks or pads. Newly placed exchange items use the large version. To review a complete new tower, use `/dronetest build industrial` at an open site at least 100 blocks from an earlier tower. The command reports an earlier nearby design instead of silently rebuilding it.

Formation and access check at most 270 loaded cells and reject mixed sizes, missing parts or mismatched ownership/endpoints. All collision shapes are sliced and rotated once into cached cell-local shapes. Both input units still have one controller each, with no added inventory, station ticker, chunk loader or recurring packet stream. The existing tower chunk-retention mechanism covers its expanded ground pad area.

## Artwork and verification

Original native assets and provenance live in `tools/tower_large_exchanges.py` and `art/tower-large-exchanges/`. Legacy model dimensions, textures and metadata remain intact. The large energy model has 774 authored solids and 7,144 exported faces; the closed cargo model has 618 solids and 6,260 faces. All 599 generated files reproduce byte-for-byte. Model bounds, UVs, closed faces, coplanar surfaces, cross-part seams, fitted cable/pipe contacts and 59,206 cargo enclosure boundary probes pass. All 24 tower geometry regressions pass, including complete machines, connected pads, unobstructed exits and frozen layout 4.

The isolated Java 25 build passes with 93 server game tests and four client game-test classes, producing 51 Minecraft captures. Checks include all four horizontal rotations, full-footprint placement and teardown, blocked previews, complete collision slicing, original saved layouts, mixed-size and forged-port rejection, all three energy cable materials, both item-pipe types, real scheduled 64-item intake, the original 27-slot cargo menu, changing energy instruments and independent tower snapshots. Actual full-machine, live-panel, enclosure and connected-socket captures were reviewed.

Delivery preserves the authenticated pending update, including concurrent nuclear, framed-glass and speed-gate work. Only the pending launcher source pair is queued for the next normal launch; live source and saves are not rewritten during delivery. The tested JAR, screenshots and validation records are retained under `build/drone_tower/`. A two-client dedicated-server playtest and unrelated full repository audit are not claimed.
