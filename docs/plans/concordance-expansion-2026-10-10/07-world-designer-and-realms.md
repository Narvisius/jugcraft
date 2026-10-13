# World Designer, magical geography and realms

**Baseline:** the editor and custom world generation are already in main. **Proposal:** expand that tool, keeping its existing file format migratable. This chapter is also the bridge between the earlier world-editor request and the magic/boss roadmap.

## 1. What the current editor really controls

The offline editor starts with a 65×65 sample grid at 32-block spacing, covering roughly 2,048×2,048 blocks. It paints height and registered biomes, places spawn, supports the current walled TownBuilder settlement, and emits a datapack/preset ZIP. It has undo, JSON save/load and a registry catalog exported through `/jugcraft design export`.

Current terrain design preserves deep caves below the blending interval and blends toward normal terrain at the border. Biome overrides are a surface-oriented feature, not an arbitrary replacement of every cave biome.

Current structure pins are start-chunk requests, capped at 32. They are not exact schematic placement, rotation, guaranteed biome-independent village generation or automatic updates to already generated chunks. Ordinary world generation may still place other structures. `/locate` is not automatically a pin database.

The exported preset names standard dimensions; arbitrary extra dimensions cannot be assumed to be inherited. Verify Jugcraft lair availability in a newly exported designed world before advertising the full magic experience.

## 2. Four authoring layers

### Layer A: terrain and ecology

Keep height/biome painting. Add better brush ergonomics before adding a giant 3D editor:
- Raise/lower with a radius and strength preview.
- Smooth and plateau tools with undo.
- River/channel drawing that computes a readable downhill profile.
- Slope and waterline overlays.
- Habitat hints for the magical plants, computed from rules where possible.
- A clear distinction between an editor prediction and actual generated terrain.

A habitat overlay is advisory. It must not claim a plant will grow when runtime soil, cover, season or permission conditions are unknown.

### Layer B: settlements and routes

The owner wants to place villages and a city. Turn “structure pin” into several explicit placement modes:
1. **Structure start:** existing behavior with honest limitations.
2. **Exact authored settlement:** a validated footprint, rotation, entrance and road connections.
3. **Building lot:** an internal plan slot for a specific compatible template.
4. **Road corridor:** an authored route with a width, slope limit and bridge/tunnel policy.

Do not disguise mode one as modes two through four. Start exact placement with the existing TownBuilder layout, because it already supplies a concrete structure and known dimensions.

Before export, detect overlaps among settlement footprints, protected spawn space, entrance sites, steep slopes and requested roads. Offer alternatives visually; do not silently move the city hundreds of blocks.

### Layer C: magical geography

Add bounded logical regions with IDs, labels and rules:
- Ley survey areas, affecting discovery or a limited environmental modifier.
- Garden habitats, linked to real existing biome/soil conditions.
- Astronomical viewpoints, with sky exposure and access.
- Ruins and discovery locations.
- Lair invitation sites, outside existing settlements unless deliberately placed.
- Conclave/civic project sites.

A ley region is not a universal extra energy supply. The first version should provide discovery identity or a small clearly specified modifier. Resource production requires an explicit device/process and economy review.

### Layer D: encounters and realm entrances

Place overworld discoveries and entrances; keep actual boss arenas in the existing lair dimensions. The city's crypt entrance can lead to Hollow Acre without carving an enormous combat arena under every city.

Separate:
- **Discovery marker:** flavor and research clue.
- **Ritual site:** optional prepared terrain that still requires a valid invitation.
- **Gateway:** a permitted entry point with existing lair lifecycle.
- **Arena template:** content authored for an instance, with safe boundaries and fixtures.
- **Dream scene:** an ephemeral authored room using existing escrow.

Each has different persistence, permissions and generation semantics. Do not use one undifferentiated “boss marker” object for all five.

## 3. Proposed versioned design contract

Adapt the current schema after reading it. The following fields are conceptual additions, not a working current-format example:

- `schemaVersion`: explicit integer migration version.
- `designId`: stable identifier.
- `catalogFingerprint`: exported registry/data revision used for editing.
- `terrain`: existing grid and biome payload.
- `settlements[]`: mode, template ID, origin, rotation, footprint and road anchors.
- `regions[]`: ID, type, bounded shape, priority and parameters.
- `sites[]`: type, feature ID, position, facing and prerequisites.
- `routes[]`: bounded points and construction policy.
- `warnings[]`: editor diagnostics, not trusted runtime instructions.

The runtime should parse only supported fields, cap counts and coordinate ranges, reject NaN/infinity, validate identifiers and provide precise errors. Unknown mod registry IDs should produce a visible unresolved state on import. Never silently substitute a different biome or structure with the same display name.

Store data per world/server and per loaded design context. No static global “active map” shared across different saves. A server exports a sanitized catalog; a client editor cannot upload arbitrary paths, run commands or edit another world without an explicitly authorized server workflow.

### Region resolution

For a first implementation:
1. Index region bounding boxes.
2. Query only the bounded set relevant to the position.
3. Resolve by explicit priority, then stable ID as tie-break.
4. Return a small immutable rule result.
5. Cache where useful with invalidation on design revision.
6. Avoid a global per-tick sweep of every region and player.

Overlapping regions should have a deterministic preview. “Last painted wins” is only valid if the saved ordering makes that rule explicit.

## 4. Exact settlement placement: a safe first vertical slice

1. Import the actual current TownBuilder footprint and orientation rules into the catalog.
2. Show a scaled footprint, road entrances and required clearance in the editor.
3. Let the owner select position/rotation and preview terrain cuts/fills.
4. Generate a deterministic plan with a saved seed and version.
5. Validate collisions against other authored plans before export.
6. At generation time, apply only to new chunks using a bounded placement contract.
7. Make cross-chunk placement idempotent so the same building is not generated twice.
8. Preserve normal surrounding biome/terrain behavior outside the footprint.
9. Generate a new test world, walk every route and inspect foundations.
10. Show the owner the actual generated city, not only a browser drawing.

A later “regenerate this region” tool is a separate destructive capability requiring region backup, preview and explicit action. Do not sneak it into an ordinary import or reload.

## 5. Lair authoring tools

Use existing Python/template generators and approved model tools rather than build a full general-purpose 3D package immediately.

Add a small arena descriptor:
- Bounds, arrival and exit.
- Safe platforms and forbidden/fall areas.
- Fixture IDs and positions.
- Spawn anchors and path lanes.
- Temporary terrain masks and restoration template.
- Telegraph areas and intended visibility.
- Boss leash/reset radii.
- Screenshot viewpoints and automated checks.

A top-down overlay can display these anchors in the offline editor or a simple developer viewer. It should validate that the Yeti's charge lanes meet columns, Tatterlace's permanent routes survive every allowed floor combination, and Vesperine's four wards are reachable.

Use the same IDs for descriptor, runtime fixture and test assertion. Do not keep three hand-entered coordinate lists that drift.

## 6. Mod compatibility and registry catalogs

The editor can support modded content because it works with exported identifiers and capabilities, not a hard-coded vanilla-only list. That is an architectural direction, not a claim every external worldgen mod is compatible.

Catalog entries need ID, localized label where available, kind, dimension relevance and supported placement mode. For structures, include whether exact placement is available or only a native start request. Do not infer an exact template from an arbitrary structure ID.

A saved design retains unresolved IDs and lets the owner relink them. It should distinguish:
- Missing mod/content.
- Present but incompatible schema.
- Valid ID with no current exact-placement adapter.
- Valid placement that failed a terrain/biome rule.
- Already generated region where changes do not apply.

Test with actual installed registry exports. Do not invent a dependency on an external map editor when the purpose is to own the Jugcraft pipeline.

## 7. World and realm acceptance

Minimum scenarios:
- New ordinary world with all lairs.
- New designed world with the same lairs.
- Imported design with missing biome and structure IDs.
- Two separate worlds loaded sequentially with different designs.
- Save/reopen and new chunk generation.
- A settlement at a chunk boundary and a rotated settlement.
- Spawn outside/inside the authored terrain border.
- An invitation site near a protected town.
- A player returning from a lair after server restart.
- An old schema file migrated and re-exported without losing the original.

Use existing editor tests, WorldDesignerGameTests and WorldDesignerClientGameTests, then inspect a real exported ZIP in a fresh world. A browser preview and a passing JSON parser do not prove the generated city is walkable.

## 8. Delivery order

The first editor expansion should be modest but complete: precise capabilities in the catalog, a magic-site layer, an actual generated entrance, and designed-world/lair compatibility evidence. Then exact TownBuilder placement. Roads and building lots follow. A full editable 3D city, arbitrary schematic import and live terraforming remain later projects with separate scope.

[Next: interfaces, animation and assets](08-interfaces-animation-and-assets.md)
