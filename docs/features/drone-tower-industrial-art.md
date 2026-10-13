# Drone Tower realistic industrial assets

Owner direction, 8 October 2026: retain the imposing tall tower and make its construction,
service stations, hangars and command room believable through both realistic materials
and physical components. The owner rejected the initial short-palette, framed-block pass
as too similar to the old tower and a toy. They then approved the actual native 4×6×2-block
wall/hangar sample and asked for substantially more detail in the parts themselves.
That explicit approval governs this tower-specific realistic material treatment.

No drone body, rotor, entity atlas, item representation or animation is modified by these
art generators. Drone flight and gameplay validation belong to the main feature record.

## Runtime generation

Run `python tools/tower_realism_art.py --write --preview` from the repository root.
Its final `tools/tower_realism_runtime.py` override writes the approved materials and
models after the earlier tower defaults. The ordinary `tower.py` and `tower_art.py`
writers include this final override too. No global texture regeneration is necessary.

`tools/tower_realism_v3.py` preserves the earlier approved small sample as a separate
readback under `build/drone_tower/v3sampleassets`. It does not install runtime assets.

## Architectural materials and physical parts

Poured concrete and painted structural steel use power-of-two derivatives of the original
approved generated albedos. The two-metre building sheets are 512×512 pixels; shared
hardware materials are 256×256. Broad aluminum cladding and the subsidiary coatings are
original deterministic materials with restrained variation. The runtime pipeline resamples
copies; original source images remain unchanged.

Ten opaque materials use `TowerPanelBlock.PHASE`: Reinforced Concrete, Aluminum Cladding,
Carbon Composite Panel, Silicon Carbide Armor, Depleted Uranium Armor, Graphene Lattice,
Steel Armor Plate, Transformer Casing, Acoustic Wall Panel, and Access Floor Tile.
`phase = (worldX & 1) | ((worldY & 1) << 1) | ((worldZ & 1) << 2)` selects the corresponding
quarter of each two-metre sheet for each face. Player placement and authored tower placement
derive phase from the absolute world position. Default phase zero preserves old saved block
states. These six-face architectural cubes have boundary culling and no decorative cages
around every metre. Existing material IDs and stair/slab families remain available.
Native model roots inherit `minecraft:block/block`, preserving the game's normal
three-dimensional inventory, ground and hand transforms through their item aliases.

The three-axis steel girders retain their load-bearing I-sections and add actual splice
plates and fasteners within the section. Tungsten frames have open diagonal trusses.
Armored conduits are separate octagonal cable runs with metal clamps. Copper busbars have
porcelain stand-offs; cooling fins form real arrays; ceramic insulators have closed faceted
skirts. Their axis-specific models preserve existing pillar properties. The static service
grating, railing, ventilation cabinet and caged maintenance ladder retain real openings,
closed rails, angled louvers and guard hoops.

`tower_door_mechanism` supplies six static geometry variants: continuous jamb guides,
bolted foot anchor, top splice, longitudinal header cassette, end drive, and threshold sill.
The guides and cassette have backed channels; bolts occur at junctions. These cells are
placed outside the clear air aperture. Each item is one segment, with four segments crafted
from six steel plates, two steel ingots and one basic circuit (2.375 steel-ingot equivalents
per segment); this is not a six-block formed machine. Its language, item model, recipe and
loot use the existing drones/machines feature conditions.

Industrial moving doors have distinct `tower_industrial_door_leaf`,
`tower_industrial_door_hardware` and `tower_industrial_door_threshold` entity textures.
The leaf sheet maps over two metres. Legacy roll-up door resources remain separate.

## Command room and working decks

The terminal and command desk have separate steel supports and feet, real keyboard keys,
controls, a cable loom and a substantial monitor bezel. Equipment racks have individual
server drawers, handles, fasteners and indicator lenses. Cable trays contain separate
cables retained by physical straps. The operator chair has a swivel mechanism, adjustment
lever, steel frame, armrests, separate padding and paired caster wheels.

Desk and terminal monitors face the keyboard and operator, local +z for the north-facing
desk (the operator looks north). Their visible display spans x=1.15..14.85 and
y=16.15..24.45 pixels, on z=3.75 with outward normal +z; the live renderer uses z=3.85
to stand 0.10 pixels clear of the native surface. The screen front maps its complete opaque,
blank dark-glass sprite and emits at level 10; live readouts have no fake labels underneath.
Wall-mounted Control Screens retain their separate facing semantics and native -z front.

The legacy `light_cyan` palette alias resolves to `red_light_strip`. It now supplies a
solid steel floor tile with an actual recessed sealed opal lamp, gasket, metal surround and
retaining screws, instead of a glowing band across a cube. It preserves full-block support
and never projects above the floor. `warning_light` is a compact sealed caged beacon with
driver housing, fixing shoe, bolts, cable gland, gasket and faceted lens; its geometry is
bounded by x/z=3.5..12.5 and y=0..8 pixels. Ceiling luminaires have separate driver shells,
gaskets, opal diffusers and latch clips, bounded by (2,14.25,4)..(14,16,12). Dedicated opal
materials emit light in the native models without changing pad or door materials. Actual
world light levels and fixture collision are set by the corresponding block registrations.

The nine Hologram Table sections form the same 3×3 operations table, now with actuator
legs, service compartments, removable backed doors, projector cradle, faceted optics and
control pads. Its existing footprint, part numbers and exact 13-pixel height are preserved.
The live hologram remains the client renderer's responsibility. Screen and optical surfaces
use native per-element light emission; baked screen graphics do not claim to show live values.

Ground landing pads, elevated supply pickups and all four hangar pad layouts retain their
existing part numbers. Joined high-resolution decks carry approach markings and functional
contact/hatch indications. Their native models add recessed charging cassettes, copper
contacts, fasteners, edge lamp housings and a physical hatch surround. Loose plates also
use this geometry. All ground pad/pickup geometry stays within the existing 0..3-pixel
collision plate; hangar pad geometry stays within its existing 0..16-pixel block. The
client's moving pickup lift is independent of this static installed plate.

## Provenance

Original source materials: `art/tower-v3-sample/cast_concrete_albedo.png` and
`painted_structural_steel_albedo.png`, both 1254×1254, generated with the built-in OpenAI
image-generation tool. Their prompts and preserved source record are in
`art/tower-v3-sample/README.md`. The owner-approved tower concept supplied overall direction.
The shared owner asset library was inspected first, including `concrete0.png`,
`concrete_brick0.png`, `sheetmetal_steel.png` and `steel_scaffolding.png`; no library originals
are edited. Geometry, UV layout, supporting materials and pipeline implementation were
authored with OpenAI Codex (GPT-6). Runtime assets are covered by the repository MIT license.

## Verification

The latest model audit examined 279 model files, resolving to 266 unique meshes and material
bindings with 6340 faces (fully resolved parent aliases and duplicate desk geometry count
once). Material and assembly checks covered 64 derivatives and seven complete assemblies:
the nine-section table, ground pad, pickup,
and all four hangar pad layouts. UV range, inverted bounds, exposed missing faces,
coplanar/near-coplanar surfaces and assembly seams had zero findings. Height checks confirm
ground plates 0..3, hangar decks 0..16 and the table 0..13 pixels. All checked material
derivatives are opaque and power-of-two. Blast Glass retains its intentional translucency.

Previews read exported JSON and actual PNGs, use their real UVs, cull back faces and
z-buffer the geometry. `build/drone_tower/industrial_runtime_parts.png` shows the individual
parts, chair, assembled table and contact/hatch models. `industrial_operations.png` assembles
the table parts and installed deck sprites. `industrial_fixtures.png` shows the exported
floor fitting, beacon, ceiling fitting and desk from the operator's side. The five revised
fixture/desk model files passed the same UV, closure and surface checks; their measured
extents are recorded in `build/drone_tower/fixture-audit.json`. SHA-256
comparison confirmed 298 protected door, pad, pickup and drone resources unchanged.
These are offline asset inspections, not game
screenshots. Compilation, server tests and in-game client verification are reported by the
main tower redesign task; no gameplay result is inferred from these previews.
