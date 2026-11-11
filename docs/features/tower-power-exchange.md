# Drone Tower power exchange

The 9 October [large-input revision](tower-large-inputs.md) adds a 9-wide, 6-deep, 5-tall variant in the tower's charcoal/gray/concrete palette for new placements and new layout-5 towers. This record retains the original 64-part variant and its validation evidence.

Owner-requested revision by OpenAI Codex (GPT-6), 8 October 2026. This revision replaces the power machine's rejected appearance with detailed industrial switchgear, using the repository's diesel-generator panels as the visual reference. It changes only the Energy Exchange and its presentation; the tower architecture and Cargo Exchange are outside this revision.

## Machine and instruments

The exchange has three painted switchgear cubicles with engraved headers, raised instrument cases, printed scales, relay windows, handles, hinges, selectors, fasteners and service louvers. A lower transformer assembly, cooling banks and junction housings complete the rear machinery. The marked sealed gland still meets copper, aluminum and silver cable bodies at the original external connection.

Live needles and numeric windows show the linked terminal's real energy storage, capacity, demand in JE/t and load percentage. Link, charged and low-power indicators use the same synchronized tower snapshot. These are Jugcraft Energy instruments; they do not invent voltage, frequency, temperature, incoming transfer readings or protection behavior. The mechanical selectors and service hardware are visual details rather than new power controls.

The client renderer reuses the existing diesel-panel overlay texture and rendering approach, with fresh exchange-specific static artwork and geometry. It reads the exact saved terminal/core/depot endpoints instead of searching for a nearby tower. Missing, unloaded or mismatched endpoints show an offline instrument state. The sole station controller is passive; there is no new ticker, energy buffer, periodic network payload or per-frame world scan. Cargo stations skip this renderer.

## Integration and saves

The existing `energy_exchange` ID, 64-part footprint, part numbering, rotation, connection face, item, recipe, buffer and transfer rules remain in use. Its upstream source is the established cable network; its downstream consumer is the tower terminal's existing 200,000 JE buffer and drone demand. The recipe continues to consume steel plates, transformer casings, an advanced circuit and the existing Energy Exchange Port. Server-side formation, ownership and exact depot checks continue to govern all transfers.

Endpoint positions and the depot UUID are sent once in the station's normal block-entity update tag on binding or chunk load. The terminal's existing snapshot gains an optional depot UUID field; older snapshots remain readable. Saved station ownership and energy data use their existing formats. Current and frozen tower layouts are not regenerated or migrated. Existing built exchanges receive the new appearance after the normal launcher update and restart.

## Art and validation

Original generator, textures and provenance are retained in `tools/tower_power_exchange.py` and `art/tower-power-exchange/`. The reference implementation is `tools/plant_panels.py` and `client/LivePanelRenderer.java`; its original diesel and nuclear assets are unchanged. The shared owner-library catalog and repository art/license policies were consulted. No screenshot pixels or external product artwork are imported.

Validation passed in a separate copy of the actual play checkout with the complete pending Claude update merged: `assemble`, 82 targeted server game tests, and the Tower, Deletion and Energy Exchange client game-test classes. All 22 tower geometry regressions pass. The station audit checks all 64 parts, 367 authored solids and 2,914 exported faces for bounds, UVs, closed faces, coplanar surfaces and cross-part seams. The Cargo Exchange, tower layouts and 298 protected drone, pad, pickup and door assets are unchanged by this power revision.

The 44 retained gameplay captures include all three cable materials and the new live panel, changed-charge and complete assembly views. The power client test uses two independently owned towers, verifies real synchronized readings, changes storage from 100,000 to 150,000 JE, and checks stale depot identity, mismatched cores and an actually unloaded terminal chunk. The server tests preserve transfer conservation and reject a replaced terminal through cached energy access until explicitly rebound. Native and gameplay reviews found no missing textures, detached components, seam issues or instrument alignment problems.

Delivery is a guarded, backed-up update to the normal launcher's pending source bundle, preserving Claude's simultaneous framed-glass work and the installed layout-4, master-console and lighting improvements. It takes effect through `6-play.bat` on the next normal restart; the running game's source, classes and saved worlds are not rewritten. Exact build, JAR and capture hashes are retained in `build/drone_tower/validation-v6.json`. A two-client dedicated-server playtest and the unrelated full repository audit were not run.
