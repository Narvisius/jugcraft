"""Industrial Drone Tower layout v7, preserving every earlier saved layout.

The immutable v1 tower supplies all functional anchors: nine tiers, 133 small-drone
capacity, hangar interiors/doors, ground pads, pickups, terminal, core and packager.
This builder replaces the exchange scenery with real formed stations and adds an
engineered industrial envelope, service access and a larger useful ground apron.
Nothing in this module changes drone models, size classes or flight mathematics.

Run this file to write ONLY the current tower, its foundation preview and UI silhouettes.
The separate preview_tower_industrial.py renders the exported runtime block models.
Authored with OpenAI Codex; original v1 generator remains tools/drone_tower.py.
"""
from __future__ import annotations

import copy
import collections
import gzip
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

import drone_tower as legacy
import tower_costs
import tower_lights

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
DATA = RESOURCES / "data/jugcraft/drone_tower/tower_industrial.json.gz"
VERSION = 7
C = legacy.C
APRON_HALF = 31
COSMETIC_APRON_HALF = 40
ROOM_X = 7
ROOM_Z = 6
PAD_SHIFT = 3
SCREEN_SIZE = (142, 434)
PHASE_IDS = {
    "reinforced_concrete", "aluminum_cladding", "carbon_composite_panel",
    "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice",
    "steel_armor_plate", "transformer_casing", "acoustic_wall_panel", "access_floor_tile",
}
COLORS = {
    "tower_service_grating": (92, 103, 112),
    "tower_service_railing": (118, 131, 140),
    "tower_vent": (64, 77, 86),
    "tower_ladder": (132, 143, 149),
    "tower_door_mechanism": (64, 71, 76),
    "wall_task_light": (171, 180, 178),
    "linear_ceiling_light": (186, 193, 188),
    "tower_master_console": (76, 89, 93),
}
legacy.COLORS.update(COLORS)
tower_lights.EMIT.update({"wall_task_light": 14, "linear_ceiling_light": 15, "tower_master_console": 4})
_BUILT: dict[int, dict] = {}
_METADATA: dict[int, dict] = {}
_LIGHTS: dict[int, dict] = {}
_APRON_LAYOUT = None
_FROZEN_UPPER_LIGHTS = None


def apron_layout():
    """Fixed optional paving outside every frozen-v5 construction operation.

    The original structure/landing field is mandatory. Future buttress feet are
    filled early as mandatory foundation, never advertised as removable paving.
    Only the disjoint two-layer outer mask is cosmetic. Its materials and lamps
    are identical in every tier, so an upgrade cannot restore removed paving.
    """
    global _APRON_LAYOUT
    if _APRON_LAYOUT is not None:
        return _APRON_LAYOUT
    with gzip.open(DATA.with_name("tower_industrial_v5.json.gz"), "rt", encoding="utf-8") as source:
        frozen = json.load(source)
    reserved, first_floor, future_floor, low_obstacles = set(), set(), set(), set()
    for tier in frozen["tiers"]:
        for row in tier["place"]:
            x, y, z = row[:3]
            reserved.add((x, y, z))
            if y == 0:
                future_floor.add((x, z))
                if tier["tier"] == 1:
                    first_floor.add((x, z))
            elif y == 1:
                # Conservative light routing: even thin old details count as
                # obstacles here, so later structures never shadow an apron lamp.
                low_obstacles.add((x, z))
        reserved.update(tuple(row) for row in tier["clear"])
        reserved.update(tuple(row) for row in tier["air"])
    plinth = frozen["plinth"]
    reserved.update((x, y, z) for x in range(-plinth, plinth + 1)
                    for z in range(-plinth, plinth + 1) for y in (-1, 0))
    columns = {(x, z) for x in range(-COSMETIC_APRON_HALF, COSMETIC_APRON_HALF + 1)
               for z in range(-COSMETIC_APRON_HALF, COSMETIC_APRON_HALF + 1)
               if all((x, y, z) not in reserved for y in (-1, 0))}
    lamps = {(x, z) for x, z in columns if x % 6 == 0 and z % 6 == 0}

    def coverage(x, z):
        # Level-eight flush lamps give level one six horizontal steps above
        # the pavement: enough block light to prevent hostile mob spawning.
        reached = {(x, z)}
        if (x, z) in low_obstacles:
            return reached
        frontier = {(x, z)}
        for _ in range(6):
            frontier = {(a + dx, b + dz) for a, b in frontier
                        for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1))
                        if (a + dx, b + dz) not in low_obstacles} - reached
            reached.update(frontier)
        return reached & columns

    covers = {p: coverage(*p) for p in lamps}
    counts = collections.Counter(p for cells in covers.values() for p in cells)
    for x, z in sorted(columns, key=lambda p: (p[1], p[0])):
        if not counts[(x, z)]:
            lamps.add((x, z))
            covers[(x, z)] = coverage(x, z)
            counts.update(covers[(x, z)])
    for p in sorted(lamps, key=lambda p: (len(covers[p]), p[1], p[0])):
        if all(counts[target] > 1 for target in covers[p]):
            lamps.remove(p)
            counts.subtract(covers[p])
    optional = {}
    for x, z in columns:
        r = max(abs(x), abs(z))
        material = ("steel_armor" if r == 40 else "access_floor_tile" if r in (38, 39)
                    else "dock_floor" if 32 <= r <= 36 else "reinforced_concrete")
        if (x, z) in lamps:
            material = tower_lights.FIXTURE
        optional[(C + x, 0, C + z)] = material
        optional[(C + x, -1, C + z)] = "reinforced_concrete"
    mandatory = {(C + x, y, C + z): "reinforced_concrete"
                 for x, z in future_floor - first_floor for y in (-1, 0)}
    _APRON_LAYOUT = optional, mandatory
    return _APRON_LAYOUT


def integrated_apron(blocks):
    """Paved 81m square with a structural inner field and removable outer courts."""
    optional, mandatory = apron_layout()
    for p, material in mandatory.items():
        blocks.setdefault(p, material)
    for p, material in optional.items():
        if p in blocks:
            raise ValueError(f"cosmetic apron overlaps mandatory construction: {p}")
        blocks[p] = material


def clear_terminal_approach(blocks):
    """Leave the central overhead transfer beam supported by the existing piers."""
    for y in range(1, 11):
        blocks.pop((C, y, C + 11), None)
    top = (C, 11, C + 11)
    if top in blocks:
        blocks[top] = "girder_z"


def preserve_upper_lights(blocks, tier):
    """Keep every above-ground recessed fixture at its frozen-v6 location."""
    global _FROZEN_UPPER_LIGHTS
    if _FROZEN_UPPER_LIGHTS is None:
        with gzip.open(DATA.with_name("tower_industrial_v6.json.gz"), "rt", encoding="utf-8") as source:
            frozen = json.load(source)
        present, _FROZEN_UPPER_LIGHTS = {}, {}
        for stage in frozen["tiers"]:
            for p in stage["clear"]:
                present.pop(tuple(p), None)
            for x, y, z, index in stage["place"]:
                present[(x, y, z)] = frozen["palette"][index]
            _FROZEN_UPPER_LIGHTS[stage["tier"]] = {
                (C + x, y, C + z) for (x, y, z), material in present.items()
                if y > 0 and base(material) == "jugcraft:red_light_strip"}
    for p in _FROZEN_UPPER_LIGHTS[tier]:
        if base(blocks.get(p, "")) not in tower_lights.FLOORS | {tower_lights.FIXTURE}:
            raise ValueError(f"frozen upper light no longer has its original floor: {p}")
        blocks[p] = tower_lights.FIXTURE


def floor_light_target(position):
    """The control room and upper task floors stay bright; exterior paving needs one."""
    x, y, z = position
    room = abs(x - C) <= ROOM_X and -ROOM_Z <= z - C <= ROOM_Z + 2
    return 5 if y > 0 or room else 1


def base(material):
    return material.split("[")[0]


def state(material, position=None):
    """Resolve v2 aliases without adding aliases or behavior to the legacy module."""
    if material.startswith("minecraft:") or material.startswith("jugcraft:"):
        resolved = material
    elif base(material) in COLORS or base(material) in ("energy_exchange", "cargo_exchange"):
        resolved = "jugcraft:" + material
    else:
        resolved = legacy.block_id(material)
    if position is not None and base(resolved).removeprefix("jugcraft:") in PHASE_IDS:
        x, y, z = position
        phase = (x & 1) | ((y & 1) << 1) | ((z & 1) << 2)
        # The Java placement path normalizes this relative phase to absolute
        # world coordinates. No world-specific offset is baked into the asset.
        resolved = resolved[:-1] + f",phase={phase}]" if "[" in resolved else resolved + f"[phase={phase}]"
    return resolved


def box(blocks, x0, y0, z0, x1, y1, z1, material, replace=False):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                if replace or (x, y, z) not in blocks:
                    blocks[(x, y, z)] = material


def beam(blocks, a, b, material="steel_girder", thickness=1):
    """A closed voxel member, including both ends; used for raked transfer braces."""
    steps = max(abs(v - u) for u, v in zip(a, b))
    for i in range(steps + 1):
        p = [round(u + (v - u) * i / max(1, steps)) for u, v in zip(a, b)]
        box(blocks, p[0], p[1], p[2], p[0] + thickness - 1,
            p[1], p[2] + thickness - 1, material)


def shell_material(y):
    """Large structural masses stay concrete; upper skins keep their tier material."""
    if y < 35:
        return "reinforced_concrete"
    material = legacy.SHELL[min(8, legacy.tier_at(y) - 1)]
    return "steel_armor" if material == "tungsten_frame" else material


def coherent_envelope(blocks, tier):
    """Replace voxel decoration with continuous poured masses and broad wall skins.

    This is a material/architecture pass over the original construction, not a
    new dock layout. New 2m UV phases supply surface scale without decorating
    every metre with a frame, bolt, illuminated block or isolated glass tile.
    """
    spire_base = next(y0 for _, kind, y0, _, _ in legacy.schedule() if kind == "spire")
    shells = set(legacy.SHELL) | {"sc_conduit", "light_cyan"}
    for (x, y, z), material in list(blocks.items()):
        name = base(material)
        if name in ("stone_bricks", "chiseled_stone"):
            blocks[(x, y, z)] = "reinforced_concrete"
        elif y >= 12 and name in ("light_cyan", "sc_conduit", "blast_glass"):
            blocks[(x, y, z)] = "graphene_lattice" if y >= spire_base else shell_material(y)
        elif name == "tungsten_frame":
            blocks[(x, y, z)] = "steel_armor"
        elif name == "hazard_plating":
            # Hazard marks belong on the formed pad and threshold, not on
            # whole structural lintels, parapets, bunker walls and rib brackets.
            blocks[(x, y, z)] = "dock_floor" if y < 1 else shell_material(y)
        if y == 17 and max(abs(x - C), abs(z - C)) >= 18 and name in ("hazard_plating", "steel_girder"):
            dx, dz = x - C, z - C
            blocks[(x, y, z)] = f"tower_service_railing[facing={direction(-dx, -dz)}]"
    # Each bunker elevation has one deliberate two-metre-high slit rather than
    # a checkerboard of isolated glass cubes. The south entrance stays open.
    for side in (1, 2):
        for u in ((-4, -3, -2) if side == 2 else (-2, -1, 0, 1, 2)):
            for y in (4, 5):
                for depth in (6, 7):
                    x, z = (C + depth, C + u) if side == 1 else (C + u, C + depth)
                    if (x, y, z) in blocks:
                        blocks[(x, y, z)] = "blast_glass"
    # Remove the remaining scattered bunker glazing away from those apertures.
    for (x, y, z), material in list(blocks.items()):
        if base(material) != "blast_glass" or y not in (4, 5):
            continue
        dx, dz = x - C, z - C
        slit = (dx in (6, 7) and -2 <= dz <= 2) or (dz in (6, 7) and -4 <= dx <= -2)
        if max(abs(dx), abs(dz)) >= 6 and not slit:
            blocks[(x, y, z)] = "reinforced_concrete"
    # Curved structural blades read as one concrete pour, not a stack of
    # unrelated material courses. The outer cap is steel and the service lights
    # are interrupted at access stations, so the structure carries the silhouette.
    if tier >= 3:
        for (sx, sz), join, foot in legacy.BUTTRESSES:
            for y in range(1, min(legacy.TIER_TOPS[tier], join + 1)):
                k = legacy.rib_k(y, join, foot, spire_base)
                depth, half = 6.0 - 3.6 * y / join, 2.6 - 1.4 * y / join
                cells = set()
                for a in range(-12, 13):
                    for b in range(-6, 7):
                        along, across = a * .5, b * .5
                        if abs(along) <= depth and abs(across) <= half:
                            kk = k + along
                            cells.add((round(C + sx * (kk + across)), round(C + sz * (kk - across))))
                for x, z in cells:
                    p = (x, y, z)
                    if base(blocks.get(p, "")) in shells:
                        blocks[p] = "reinforced_concrete"
                edge = (round(C + sx * (k + depth)), y, round(C + sz * (k + depth)))
                blocks[edge] = "light_cyan" if y % 16 in (7, 8) else "steel_armor"


def apron(blocks):
    """A broad clipped rectangular apron, with service roads and equipment yards."""
    for dx in range(-APRON_HALF, APRON_HALF + 1):
        for dz in range(-APRON_HALF + 4, APRON_HALF - 3):
            if abs(dx) + abs(dz) > 52:
                continue
            p = (C + dx, 0, C + dz)
            # Do not alter the platform on which the depot discovers its pads.
            if p in blocks:
                continue
            road = abs(dz) >= 21 or abs(dx) >= 26
            blocks[p] = "dock_floor" if road else "reinforced_concrete"
            if abs(dx) == APRON_HALF or abs(dz) == APRON_HALF - 4:
                blocks[p] = "steel_armor"
    # Restrained road markings and continuous service pavement.
    for x in range(C - 24, C + 25):
        for z in (C - 24, C + 24):
            if (x, 0, z) in blocks and (x - C) % 8 in (0, 1, 2):
                blocks[(x, 0, z)] = "hazard_plating"
    for side in (-1, 1):
        # Yard barriers: never stand in the existing pads' level exit corridors.
        for x in range(C + side * 21, C + side * 30 + side, side):
            for z in (C - 9, C + 9):
                blocks[(x, 1, z)] = f"tower_service_railing[facing={'south' if z < C else 'north'}]"
        for z in range(C - 9, C + 10):
            if abs(z - C) > 2:
                blocks[(C + side * 30, 1, z)] = f"tower_service_railing[facing={'east' if side < 0 else 'west'}]"
        # Grating sits on the continuous pavement rather than replacing a full
        # floor with a 3/16-thick plate almost a metre below its neighbours.
        # Both working yards stay free of cabinets, beam posts and overhead bars.
        for dx in range(21, 30):
            for dz in range(-7, 8):
                p = (C + side * dx, 1, C + dz)
                blocks[p] = "tower_service_grating"
        # A clear three-metre approach points straight at the marked outward
        # port. Only the supported tread plate occupies the route, never a rail.
        for dx in range(21 if side > 0 else 23, 30):
            for dz in (-1, 0, 1):
                blocks[(C + side * dx, 1, C + dz)] = "tower_service_grating"


def command_room(blocks):
    """15x13 clear interior with backed walls, readable workstations and real aisles."""
    for p, material in list(blocks.items()):
        x, y, z = p
        if abs(x - C) <= ROOM_X + 2 and abs(z - C) <= ROOM_Z + 2 and 1 <= y <= 10:
            if base(material) not in {"holo_table", "drone_depot_terminal"}:
                blocks.pop(p)
    # Keep the connected landing-platform substrate outside the original plinth.
    for dx in range(-ROOM_X, ROOM_X + 1):
        for dz in range(-ROOM_Z, ROOM_Z + 1):
            if max(abs(dx), abs(dz)) <= 7:
                blocks[(C + dx, 0, C + dz)] = "access_floor_tile"
    for dx in range(-ROOM_X - 2, ROOM_X + 3):
        for dz in range(-ROOM_Z - 2, ROOM_Z + 3):
            perimeter = abs(dx) > ROOM_X or abs(dz) > ROOM_Z
            for y in range(1, 10):
                if not perimeter:
                    continue
                outer = abs(dx) == ROOM_X + 2 or abs(dz) == ROOM_Z + 2
                material = "reinforced_concrete" if outer else "acoustic_wall_panel"
                if abs(dx) >= ROOM_X + 1 and abs(dz) >= ROOM_Z:
                    material = "reinforced_concrete"
                # One deliberate window on each side, above the equipment height.
                if y in (5, 6) and ((abs(dx) >= ROOM_X + 1 and -2 <= dz <= 2)
                                   or (dz >= ROOM_Z + 1 and 3 <= abs(dx) <= 5)):
                    material = "blast_glass"
                if dz >= ROOM_Z + 1 and abs(dx) <= 1 and y <= 3:
                    continue  # clear, three-metre south entrance
                blocks[(C + dx, y, C + dz)] = material
            blocks[(C + dx, 10, C + dz)] = "reinforced_concrete" if perimeter else "steel_armor"
    # The fixed terminal and table retain their original machine anchors. All
    # other furniture is set against the wider walls, with fronts into the room.
    # Each connected 3x2 wall screen forms one real terminal-linked monitor.
    # The centre gap keeps the two displays from merging into an invalid 5x2.
    for dx in (-4, -3, -2, 2, 3, 4):
        for y in (5, 6):
            blocks[(C + dx, y, C - ROOM_Z)] = "control_screen[facing=south]"
    # One centred master console, with the full operating aisle on its south.
    # This is a real six-part assembly, clear of the fixed hologram and terminal.
    for part, (dx, dz) in enumerate(((0, 0), (1, 0), (-1, 0), (0, -1), (1, -1), (-1, -1))):
        blocks[(C + dx, 1, C + 4 + dz)] = f"tower_master_console[facing=south,part={part}]"
    # Two wall computer stations face inward. Their operator positions remain
    # reachable from the central aisle instead of a row of freestanding desks.
    for side in (-1, 1):
        facing = "west" if side < 0 else "east"
        blocks[(C + side * 6, 1, C - 2)] = f"console_desk[facing={facing}]"
        blocks[(C + side * 5, 1, C - 2)] = f"operator_chair[facing={facing}]"
    for dx, facing, zs in ((ROOM_X, "west", (-4, -1, 2)), (-ROOM_X, "east", (3, 4))):
        for dz in zs:
            for y in (1, 2):
                blocks[(C + dx, y, C + dz)] = f"equipment_rack[facing={facing}]"
    # One joined ceiling-edge cable loop, with actual corner/tee/end geometry.
    # The two short branches terminate into solid service walls; no open stubs.
    for dx in range(-6, 7):
        for dz in (-5, 5):
            blocks[(C + dx, 9, C + dz)] = "cable_tray[facing=north,shape=straight]"
    for dz in range(-4, 5):
        for dx in (-6, 6):
            blocks[(C + dx, 9, C + dz)] = "cable_tray[facing=east,shape=straight]"
    for dx, dz, facing in ((-6, -5, "south"), (6, -5, "west"), (6, 5, "north"), (-6, 5, "east")):
        blocks[(C + dx, 9, C + dz)] = f"cable_tray[facing={facing},shape=corner]"
    for side in (-1, 1):
        blocks[(C + side * 6, 9, C - 2)] = f"cable_tray[facing={'west' if side < 0 else 'east'},shape=tee]"
        blocks[(C + side * 7, 9, C - 2)] = f"cable_tray[facing={'south' if side < 0 else 'north'},shape=end]"
    # Shallow downlights mount against the continuous ceiling, clear of trays.
    for dx in (-5, -2, 1, 4):
        for dz in (-3, 0, 3):
            blocks[(C + dx, 9, C + dz)] = "linear_ceiling_light[facing=north]"
    # Proper side-facing luminaires occupy adjacent air. Their solid backing
    # wall stays intact instead of replacing a wall cell with a half-height lamp.
    for side in (-1, 1):
        for dz in (-4, 0, 4):
            blocks[(C + side * ROOM_X, 4, C + dz)] = f"wall_task_light[facing={'west' if side > 0 else 'east'}]"
    for dx in (-5, 0, 5):
        blocks[(C + dx, 4, C - ROOM_Z)] = "wall_task_light[facing=south]"


def ground_pads():
    """V3 pad positions; retain exit routes, count, ordering and four slots per pad."""
    pads = copy.deepcopy(legacy_data()["pads"])
    for pad in pads:
        if pad["z"] <= -9:
            pad["z"] -= PAD_SHIFT
        elif pad["z"] >= 9:
            pad["z"] += PAD_SHIFT
        elif pad["x"] <= -9:
            pad["x"] -= PAD_SHIFT
        else:
            pad["x"] += PAD_SHIFT
        # Full-height exchange housings need clear ground pad and flight lanes.
        # Four side pads use the outer apron, retaining their order and exit direction.
        if abs(pad["x"]) >= 12:
            pad["x"] = 23 if pad["x"] > 0 else -28
            pad["ex"] = 31.5 if pad["x"] > 0 else -31.5
    return pads


def reposition_ground_pads(blocks):
    for p, material in list(blocks.items()):
        if base(material) == "landing_pad":
            blocks.pop(p)
    for pad in ground_pads():
        if abs(pad["x"]) >= 23:
            # The existing depot discovers pads through connected platform cells.
            # Extend that real substrate out to the new bays, within its bounded scan.
            for dx in range(min(pad["x"], -19 if pad["x"] < 0 else 19),
                            max(pad["x"] + 4, -19 if pad["x"] < 0 else 19) + 1):
                for dz in range(pad["z"], pad["z"] + 5):
                    blocks[(C + dx, 0, C + dz)] = "landing_platform"
        for dx in range(5):
            for dz in range(5):
                blocks[(C + pad["x"] + dx, 1, C + pad["z"] + dz)] = "landing_pad"
        if abs(pad["x"]) >= 23:
            # Open matching gates through the outer service-yard railing.
            for dx in range(28 if pad["x"] > 0 else 29, 33):
                for dz in range(math.floor(pad["ez"]) - 1, math.floor(pad["ez"]) + 2):
                    for y in range(1, 4):
                        blocks.pop((C + (dx if pad["x"] > 0 else -dx), y, C + dz), None)


def transfer_structure(blocks, tier):
    if tier < 2:
        return
    # Poured piers have distinct steel bearing shoes and transfer caps. Paired
    # solid continuous transfer beams span between those bearings. A run of
    # disconnected upright I-sections cannot carry a horizontal load.
    for side in range(4):
        for u in (9, 29):
            x, z = legacy.local(0, 0, legacy.SIZE, side, u, 2, False)
            box(blocks, x - 1, 1, z - 1, x + 1, 2, z + 1, "reinforced_concrete")
            box(blocks, x, 3, z, x + 1, 9, z + 1, "reinforced_concrete", replace=True)
            box(blocks, x, 10, z, x + 1, 10, z + 1, "steel_armor", replace=True)
            for v in range(1, 7):
                xx, zz = legacy.local(0, 0, legacy.SIZE, side, u, v, False)
                box(blocks, xx - (side % 2), 10, zz - (1 - side % 2),
                    xx + (side % 2), 11, zz + (1 - side % 2), "steel_armor")
        for u in range(9, 30):
            for v in (1, 2):
                x, z = legacy.local(0, 0, legacy.SIZE, side, u, v, False)
                blocks[(x, 11, z)] = "steel_armor"
    # The west service bay's two bearing piers have a single aligned section
    # through cable height. Asymmetric footing shoulders previously read as a
    # missing/notched concrete cell beside the power socket from its approach.
    for u in (9, 29):
        x, z = legacy.local(0, 0, legacy.SIZE, 3, u, 2, False)
        box(blocks, x - 1, 1, z - 1, x + 1, 9, z + 1, "reinforced_concrete", replace=True)
        box(blocks, x - 1, 10, z - 1, x + 1, 10, z + 1, "steel_armor", replace=True)
    # Under-deck diagonal knee braces terminate on the original support columns.
    for sx in (-1, 1):
        for sz in (-1, 1):
            beam(blocks, (C + sx * 15, 5, C + sz * 15),
                 (C + sx * 10, 11, C + sz * 15), "reinforced_concrete", thickness=2)
            beam(blocks, (C + sx * 15, 5, C + sz * 15),
                 (C + sx * 15, 11, C + sz * 10), "reinforced_concrete", thickness=2)


def hangar_frames(blocks, docks):
    """One engineered assembly per doorway, with continuous tracks and cassette.

    The guides attach to concrete jambs; splices/anchors occur at the actual
    ends of a member. Machinery is recessed into the original structural
    frame, outside the air aperture, while the client-side sliding panel keeps
    its original plane and travel volume. It cannot project into an upper
    level's vertical flight column above a lower bay's exit.
    """
    for dock in docks:
        x0, z0, x1, z1, y, h, ox, oz = dock["door"]
        across_x = x0 != x1
        ends = ((x0 - 1, z0), (x1 + 1, z1)) if across_x else ((x0, z0 - 1), (x1, z1 + 1))
        for ex, ez in ends:
            for yy in range(y, y + h + 1):
                blocks[(ex - ox, yy, ez - oz)] = "reinforced_concrete"
                part = 1 if yy == y else 2 if yy == y + h else 0
                blocks[(ex, yy, ez)] = f"tower_door_mechanism[facing={direction(ox, oz)},part={part}]"
        # The housing is a complete horizontal cassette, with only one drive
        # at an end. Full-width thresholds continue the pad edge out of the bay.
        for xx in range(x0, x1 + 1):
            for zz in range(z0, z1 + 1):
                blocks[(xx - ox, y + h, zz - oz)] = "steel_armor"
                part = 4 if xx == x1 and zz == z1 else 3
                blocks[(xx, y + h, zz)] = f"tower_door_mechanism[facing={direction(ox, oz)},part={part}]"
                blocks[(xx, y - 1, zz)] = f"tower_door_mechanism[facing={direction(ox, oz)},part=5]"


def direction(dx, dz):
    return "east" if dx > 0 else "west" if dx < 0 else "south" if dz > 0 else "north"


def facade_services(blocks, tier):
    if tier < 3:
        return
    top = min(100, legacy.TIER_TOPS[tier])
    # Deep concrete shear piers run continuously through the storeys. Steel
    # risers and a genuinely backed maintenance cage attach to those piers.
    for sx in (-1, 1):
        for sz in (-1, 1):
            box(blocks, C + sx * 10 - 1, 17, C + sz * 10 - 1,
                C + sx * 10 + 1, top - 1, C + sz * 10 + 1, "reinforced_concrete", replace=True)
            for y in range(17, top):
                blocks[(C + sx * 11, y, C + sz * 10)] = "conduit"
                blocks[(C + sx * 10, y, C + sz * 12)] = f"tower_ladder[facing={direction(0, sz)}]"
    # A bank of louvers/glazing occupies an unused, contiguous wall bay, never
    # a row of standalone vent blocks dotted along every metre of the facade.
    for t, kind, y0, y1, index in legacy.schedule():
        if t > tier or kind != "floor":
            continue
        x0, z0, size = legacy.floor_square(index)
        for side in range(4):
            candidates, run = [], []
            for u in range(legacy.CHAMFER + 1, size - legacy.CHAMFER - 1):
                x, z = legacy.local(x0, z0, size, side, u, 0, False)
                available = all((x, y0 + dy, z) in blocks for dy in (1, 2, 3))
                if available:
                    run.append(u)
                else:
                    candidates.append(run)
                    run = []
            candidates.append(run)
            bank = max(candidates, key=len)
            if len(bank) >= 3:
                # One three-metre bank with its two-metre louver stack and
                # concrete lintel, rather than six independent toy appliances.
                for u in bank[-3:]:
                    x, z = legacy.local(x0, z0, size, side, u, 0, False)
                    for yy in (y0 + 2, y0 + 3):
                        blocks[(x, yy, z)] = f"tower_vent[facing={('north','east','south','west')[side]}]"
                    blocks[(x, y0 + 1, z)] = "steel_armor"
        # Corner service terraces, never a ring balcony across the hangar doors.
        for sx in (-1, 1):
            for sz in (-1, 1):
                for a in range(7, 11):
                    for b in range(7, 11):
                        if a + b > 17:
                            continue
                        x, z = C + sx * a, C + sz * b
                        p = (x, y1 - 1, z)
                        if p in blocks:
                            blocks[p] = "steel_armor"
                        if a + b == 17:
                            blocks[(x, y1, z)] = f"tower_service_railing[facing={direction(-sx, -sz)}]"


def service_spine(blocks, tier):
    if tier < 3:
        return
    top = min(99, legacy.TIER_TOPS[tier] - 1)
    # A lit, glazed access/lift shaft in the empty atrium, not in a hangar footprint.
    for y in range(11, top + 1):
        for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            blocks[(C + dx, y, C + dz)] = "steel_girder"
        for dx in (-1, 0, 1):
            blocks[(C + dx, y, C + 2)] = "blast_glass"
        for dx in (-2, 2):
            for dz in (-1, 0, 1):
                blocks[(C + dx, y, C + dz)] = "blast_glass"
        blocks[(C - 1, y, C - 2)] = "conduit"
        blocks[(C, y, C - 2)] = "steel_armor"
        blocks[(C + 1, y, C - 2)] = "light_cyan" if y % 6 == 1 else "steel_armor"
        blocks[(C, y, C - 3)] = "tower_ladder[facing=north]"
    for t, kind, y0, y1, _ in legacy.schedule():
        if t > tier or kind != "floor":
            continue
        for dx in range(-3, 4):
            if dx == 0:
                continue  # maintenance ladder passes continuously through the landing
            blocks[(C + dx, y0, C - 3)] = "access_floor_tile"
            blocks.pop((C + dx, y0 + 1, C - 3), None)
            blocks.pop((C + dx, y0 + 2, C - 3), None)
        # Landing identification light and a cabinet for the actual lift machinery.
        blocks[(C + 3, y0 + 1, C - 2)] = "equipment_rack[facing=west]"


def buttress_engineering(blocks, tier):
    if tier < 3:
        return
    core_top = next(y0 for t, kind, y0, _, _ in legacy.schedule() if kind == "spire")
    for (sx, sz), join, foot in legacy.BUTTRESSES:
        x, z = C + sx * foot, C + sz * foot
        # Footing cap, anchored feet and a readable steel load-transfer collar.
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                if abs(dx) + abs(dz) <= 7:
                    blocks[(x + dx, 1, z + dz)] = "reinforced_concrete"
                    if abs(dx) + abs(dz) == 7:
                        blocks[(x + dx, 2, z + dz)] = "steel_armor"
        for y in range(8, min(join, legacy.TIER_TOPS[tier]), 8):
            k = legacy.rib_k(y, join, foot, core_top)
            half = 2.6 - 1.4 * y / join
            # Transverse compression diaphragm crossing the complete closed rib.
            a = (round(C + sx * (k + half)), y, round(C + sz * (k - half)))
            b = (round(C + sx * (k - half)), y, round(C + sz * (k + half)))
            beam(blocks, a, b, "steel_armor")
        # Access to the real rib-supported pickup platforms, from underneath.
    for pickup in [p for p in _METADATA[tier]["pickups"] if p["tier"] > 2]:
        px, py, pz = pickup["x"] + 1, pickup["y"], pickup["z"] + 1
        sx, sz = (1 if px > C else -1), (1 if pz > C else -1)
        for yy in range(py - 5, py):
            blocks[(px - 2 * sx, yy, pz - 2 * sz)] = "reinforced_concrete"
        beam(blocks, (px - sx * 2, py - 5, pz - sz * 2),
             (px + sx, py - 1, pz + sz), "reinforced_concrete")
        for k in range(-2, 3):
            # Railings are low and outside the 3x3 winching surface.
            blocks[(px - sx * 2, py, pz + k)] = f"tower_service_railing[facing={direction(sx, 0)}]"
            blocks[(px + k, py, pz - sz * 2)] = f"tower_service_railing[facing={direction(0, sz)}]"


def crown(blocks, tier):
    if tier < 9:
        return
    # Four three-metre louver banks share an enclosed mechanical shoulder.
    # Flat equipment enclosures sit on a real transfer floor; the needle above
    # is a continuous skin, with a narrow access riser rather than bright rings.
    for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for u in range(-1, 2):
            for y in range(176, 184):
                x, z = C + sx * 5 + sz * u, C + sz * 5 + sx * u
                blocks[(x, y, z)] = "tower_vent[facing=" + direction(sx, sz) + "]" if y in (179, 180, 181) else "steel_armor"
            blocks[(C + sx * 5 + sz * u, 184, C + sz * 5 + sx * u)] = "steel_armor"
    # One closed shoulder roof supports every maintenance ledge. It also caps
    # the large open void behind the plant louvers; no isolated railing or grating.
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            if abs(dx) + abs(dz) <= 8:
                blocks[(C + dx, 185, C + dz)] = "steel_armor"
            if max(abs(dx), abs(dz)) == 5 and abs(dx) + abs(dz) <= 8:
                blocks[(C + dx, 186, C + dz)] = "tower_service_grating"
                blocks[(C + dx, 187, C + dz)] = f"tower_service_railing[facing={direction(-dx, -dz)}]"


def continuous_supports(blocks):
    """Orient actual spanning I-sections, close diagonal/isolated decorative stubs."""
    columns = {p for p, m in blocks.items() if base(m) in {"steel_girder", "iron", "cargo_chute"}}
    for x, y, z in columns:
        # Every upper service deck and raked leg needs a continuous bearing mass;
        # only straight multi-block vertical members remain exposed I-sections.
        if (x, y - 1, z) in columns or (x, y + 1, z) in columns:
            if y <= 11 and max(abs(x - C), abs(z - C)) > ROOM_X:
                blocks[(x, y, z)] = "reinforced_concrete"
            continue
        along_x = (x - 1, y, z) in columns or (x + 1, y, z) in columns
        along_z = (x, y, z - 1) in columns or (x, y, z + 1) in columns
        blocks[(x, y, z)] = "girder_x" if along_x and not along_z else "girder_z" if along_z and not along_x else "steel_armor"


def task_fixtures(blocks, tier):
    """Backed neutral task lights, safely outside ground drone approaches and service yards."""
    for sx in (-1, 1):
        for sz in (-1, 1):
            for dx, dz in ((17, 9), (9, 17), (17, 17)):
                x, z = C + sx * dx, C + sz * dz
                if all((x, y, z) not in blocks for y in (1, 2)):
                    blocks[(x, 1, z)] = "steel_armor"
                    blocks[(x, 2, z)] = "light_red"
    for side in (-1, 1):
        for u in range(-6, 7, 3):
            blocks[(C + side * (ROOM_X + 3), 4, C + u)] = f"wall_task_light[facing={'east' if side > 0 else 'west'}]"
            blocks[(C + u, 4, C + side * (ROOM_Z + 3))] = f"wall_task_light[facing={'south' if side > 0 else 'north'}]"
    if tier < 2:
        return
    # Flush sealed luminaires sit directly against the real deck soffit.
    # Retain its hidden service routes and the full unobstructed volume below,
    # rather than suspending ringed conduit stacks over the ground pads.
    for dx in range(-16, 17, 4):
        for dz in range(-16, 17, 4):
            if abs(dx) + abs(dz) > 30 or abs(dx) <= ROOM_X + 2 and abs(dz) <= ROOM_Z + 2:
                continue
            x, z = C + dx, C + dz
            if (x, 12, z) in blocks and all((x, y, z) not in blocks for y in range(7, 12)):
                blocks[(x, 11, z)] = "ceiling_light_panel"


def mount_floor_beacons(blocks):
    """Floor-mounted housings need a real, full-height bearing below their base.

    Legacy full-cube lights also marked bare air, ceiling undersides and stacked
    lamp columns. The compact sealed beacon cannot occupy those old positions.
    Remove those decorations; the floor-light planner relights usable surfaces.
    A real existing I-section post receives a solid bearing cap, never a new pole.
    """
    girders = {"steel_girder", "iron", "girder_x", "girder_z"}
    bearings = (tower_lights.FLOORS - girders) | {"light_cyan", "landing_platform"}
    for (x, y, z), material in list(blocks.items()):
        if base(material) not in {"light_red", "warning_light"}:
            continue
        below = (x, y - 1, z)
        support = base(blocks.get(below, ""))
        if support in girders:
            blocks[below] = "steel_armor"
        elif support not in bearings:
            blocks.pop((x, y, z))


def power_socket_access(blocks):
    """Continuous three-metre working approach from the exact west-facing port.

    Protect the cable and overhead service envelope after all structural/flight
    carving, without touching any of the 64 formed power-station parts.
    """
    for dx in range(-29, -22):
        for dz in range(-1, 2):
            for y in range(2, 9):
                blocks.pop((C + dx, y, C + dz), None)


def flight_clearance(blocks, original, docks, protected):
    """Remove new architectural details from complete hangar/exit/climb volumes."""
    for p in protected:
        blocks.pop(p, None)
    for dock in docks:
        x0, z0, x1, z1, bottom, h, ox, oz = dock["door"]
        ex, ez = dock["ex"], dock["ez"]
        span = int(math.ceil(abs(ex - dock["x"]) + abs(ez - dock["z"]))) + 2
        # Door-sized full mouth, including any extra external frame depth.
        for depth in range(span + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    for y in range(bottom, bottom + h):
                        blocks.pop((x + ox * depth, y, z + oz * depth), None)
        # Do not place an overhang in the departure/arrival vertical column.
        radius = {"small": 1, "medium": 2, "large": 3}[dock["size"]]
        for x in range(math.floor(ex) - radius, math.floor(ex) + radius + 1):
            for z in range(math.floor(ez) - radius, math.floor(ez) + radius + 1):
                for y in range(bottom, legacy.SPIRE_TOP + 1):
                    p = (x, y, z)
                    if p not in original:
                        blocks.pop(p, None)


def build_raw(tier):
    old_preview, old_meta = legacy.PREVIEW, legacy.META
    legacy.PREVIEW, legacy.META = False, {"docks": [], "pickups": []}
    try:
        original = legacy.build_raw(tier)
        meta = copy.deepcopy(legacy.META)
        protected = set(legacy.HANGAR_SPACE)
        pads = dict(legacy.HANGAR_PADS)
    finally:
        legacy.PREVIEW, legacy.META = old_preview, old_meta
    _METADATA[tier] = meta
    blocks = dict(original)
    import exchanges
    old_stations = {}
    exchanges.build(old_stations)
    for p in old_stations:
        if p[1] > 0 or not (0 <= p[0] < legacy.SIZE and 0 <= p[2] < legacy.SIZE):
            blocks.pop(p, None)
    coherent_envelope(blocks, tier)
    apron(blocks)
    command_room(blocks)
    transfer_structure(blocks, tier)
    hangar_frames(blocks, meta["docks"])
    facade_services(blocks, tier)
    service_spine(blocks, tier)
    buttress_engineering(blocks, tier)
    crown(blocks, tier)
    continuous_supports(blocks)
    # Station module owns the exact formed parts; only its render geometry is new.
    import tower_exchanges
    tower_exchanges.build(blocks, large=True)
    flight_clearance(blocks, original, meta["docks"], protected)
    blocks.update(pads)
    # Existing ground pickup/packager/pad and machine anchors are never replaced.
    anchors = {"landing_platform", "supply_pickup", "cargo_packager", "drone_depot_terminal", "holo_table"}
    for p, m in original.items():
        if base(m) in anchors:
            blocks[p] = m
    reposition_ground_pads(blocks)
    task_fixtures(blocks, tier)
    # Ground drones leave beneath the deck at their existing y level.
    for pad in ground_pads():
        ex, ez = C + pad["ex"], C + pad["ez"]
        # V2's bottom pads belong to small tier-1 drones. Larger tiers have
        # their own upper hangars; don't cut neighbouring formed stations away.
        for x in range(math.floor(ex) - 1, math.floor(ex) + 2):
            for z in range(math.floor(ez) - 1, math.floor(ez) + 2):
                for y in range(1, legacy.SPIRE_TOP + 1):
                    p = (x, y, z)
                    if p not in original:
                        blocks.pop(p, None)
    power_socket_access(blocks)
    mount_floor_beacons(blocks)
    clear_terminal_approach(blocks)
    integrated_apron(blocks)
    blocks.pop((C, 0, C), None)
    return blocks


_LEGACY_DATA = None


def legacy_data():
    global _LEGACY_DATA
    if _LEGACY_DATA is None:
        with gzip.open(RESOURCES / "data/jugcraft/drone_tower/tower.json.gz", "rt", encoding="utf-8") as f:
            _LEGACY_DATA = json.load(f)
    return _LEGACY_DATA


def build(tier):
    if tier not in _BUILT:
        if tier > 1:
            build(tier - 1)
        blocks = build_raw(tier)
        preserve_upper_lights(blocks, tier)
        # Thin/static details are not full spawnable blocks. Existing planner remains
        # authoritative for ordinary floors, roofs, ledges and the enlarged apron.
        old_see = set(tower_lights.SEE_THROUGH)
        tower_lights.SEE_THROUGH.update(("tower_service_grating", "tower_service_railing", "tower_ladder", "tower_door_mechanism",
                                        "wall_task_light", "linear_ceiling_light", "tower_master_console"))
        try:
            _LIGHTS[tier] = floor_task_lighting(blocks)
        finally:
            tower_lights.SEE_THROUGH.clear()
            tower_lights.SEE_THROUGH.update(old_see)
        _BUILT[tier] = blocks
    return dict(_BUILT[tier])


def floor_task_lighting(blocks):
    """Sparse ground lighting; optional paving never supplies required safety light.

    Upper fixtures are frozen v6, and the control room retains level-five task
    light. Exterior ground needs only level one. Remove every redundant selected
    lamp after the initial grid/occlusion repair pass instead of keeping surplus.
    """
    light = {}
    cosmetic = apron_layout()[0]
    for pos, material in blocks.items():
        level = tower_lights.EMIT.get(base(material))
        # Required structure must retain its own task lighting after the
        # owner removes every optional apron tile and its embedded lamp.
        if level and pos not in cosmetic:
            tower_lights.spread(blocks, light, pos, level)
    permanent_light = dict(light)
    surfaces = {p for p in tower_lights.surfaces(blocks)
                if p not in cosmetic and base(blocks[p]) in tower_lights.FLOORS}
    ground = {p for p in surfaces if p[1] == 0}
    # Ground level-eight lamps can affect level-five targets only up to y2.
    # Higher floors already keep their exact frozen-v6 lighting unchanged.
    targets = {(x, y + 1, z): floor_light_target((x, y, z))
               for x, y, z in surfaces if y <= 2}
    probes, added = {}, {}

    def probe(source):
        if source not in probes:
            values = {}
            tower_lights.spread(blocks, values, source, tower_lights.EMIT[tower_lights.FIXTURE])
            probes[source] = values
        return probes[source]

    def grid_priority(p):
        x, _, z = p
        if (x - C) % 6 == 0 and (z - C) % 6 == 0:
            return 0
        if (x - C) % 3 == 0 and (z - C) % 3 == 0:
            return 1
        return 2

    for target, threshold in sorted(targets.items(), key=lambda row: (row[0][1], row[0][2], row[0][0])):
        if light.get(target, 0) >= threshold:
            continue
        x, y, z = target
        reach = tower_lights.EMIT[tower_lights.FIXTURE] - threshold - y
        candidates = [(x + dx, 0, z + dz) for dx in range(-reach, reach + 1)
                      for dz in range(-(reach - abs(dx)), reach - abs(dx) + 1)
                      if (x + dx, 0, z + dz) in ground]
        candidates.sort(key=lambda p: (grid_priority(p), abs(p[0] - x) + abs(p[2] - z), p[2], p[0]))
        source = next((p for p in candidates if probe(p).get(target, 0) >= threshold), None)
        if source is None:
            raise ValueError(f"ground lighting cannot preserve task target {target}")
        added[source] = blocks[source]
        blocks[source] = tower_lights.FIXTURE
        tower_lights.spread(blocks, light, source, tower_lights.EMIT[tower_lights.FIXTURE])
    counts = collections.Counter({p: 1 for p, threshold in targets.items()
                                  if permanent_light.get(p, 0) >= threshold})
    covers = {source: {p for p, value in probe(source).items()
                      if p in targets and value >= targets[p]} for source in added}
    for cells in covers.values():
        counts.update(cells)
    for source in sorted(added, key=lambda p: (len(covers[p]), p[2], p[0])):
        if all(counts[p] > 1 for p in covers[source]):
            blocks[source] = added.pop(source)
            counts.subtract(covers[source])
    return added


def write_gzip(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(json.dumps(data, separators=(",", ":")).encode("utf-8"))


def export(path=DATA):
    palette, ids, tiers, previous, cleared = [], {}, [], {}, set()
    for tier in range(1, 10):
        blocks = build(tier)
        place = []
        for (x, y, z), material in sorted(blocks.items(), key=lambda row: (row[0][1], row[0][2], row[0][0])):
            if previous.get((x, y, z)) == material:
                continue
            resolved = state(material, (x - C, y, z - C))
            if resolved not in ids:
                ids[resolved] = len(palette)
                palette.append(resolved)
            place.append([x - C, y, z - C, ids[resolved]])
        clear = [[x - C, y, z - C] for x, y, z in previous if (x, y, z) not in blocks]
        need = legacy.interior(blocks)
        air = [[x - C, y, z - C] for x, y, z in sorted(need - cleared - set(previous), key=lambda p: (p[1], p[2], p[0]))]
        cleared |= need
        # Copy exact existing runtime dock/pickup metadata, not recollected estimates.
        original_tier = legacy_data()["tiers"][tier - 1]
        # tower_costs values dummy station parts proportionally: all 64/60
        # parts together cost one crafted energy/cargo station, not 64/60 kits.
        names = [palette[p[3]].split("[")[0].split(":")[1] for p in place]
        costs = tower_costs.module_cost(tier, names, station_parts={"energy_exchange": 270, "cargo_exchange": 270})
        tiers.append({"tier": tier, "name": original_tier["name"], "top": original_tier["top"],
                      "place": place, "clear": clear, "air": air,
                      "docks": original_tier["docks"], "pickups": original_tier["pickups"],
                      "capacity": original_tier["capacity"], "modules": costs})
        previous = blocks
    cosmetic = [[x - C, y, z - C] for x, y, z in sorted(apron_layout()[0], key=lambda p: (p[1], p[2], p[0]))]
    data = {"layout_version": VERSION, "palette": palette, "plinth": legacy_data()["plinth"],
            "terminal": legacy_data()["terminal"], "pads": ground_pads(),
            "cosmetic_apron": cosmetic, "tiers": tiers}
    write_gzip(path, data)
    return data


def blueprint_stages(path=None):
    stages = []
    for tier in range(1, 10):
        columns = {}
        for x, y, z in build(tier):
            lo, hi = columns.get((x, z), (y, y))
            columns[(x, z)] = min(lo, y), max(hi, y)
        stages.append({"name": f"Tier {tier}: {legacy.TIER_NAMES[tier - 1]}", "top": legacy.TIER_TOPS[tier],
                       "columns": [[x - C, z - C, lo, hi] for (x, z), (lo, hi) in sorted(columns.items())]})
    if path is None:
        path = RESOURCES / "assets/jugcraft/blueprint_stages/drone_tower_industrial_foundation.json.gz"
    write_gzip(path, {"anchor": "jugcraft:drone_tower_core", "stages": stages})
    # Both foundation items place new cores, so both must preview the new
    # industrial layout. This preview is not persisted tower construction data.
    if path.name == "drone_tower_industrial_foundation.json.gz":
        write_gzip(path.with_name("drone_tower_foundation.json.gz"),
                   {"anchor": "jugcraft:drone_tower_core", "stages": stages})


def screen_image(tier):
    image = Image.new("RGBA", SCREEN_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    built = build(tier) if tier else {(x, 0, z): "reinforced_concrete" for x in range(C - 7, C + 8) for z in range(C - 7, C + 8)}
    nxt = build(tier + 1) if tier < 9 else {}

    def front(blocks):
        columns = {}
        for (x, y, z), material in blocks.items():
            if (x, y) not in columns or z > columns[(x, y)][0]:
                columns[(x, y)] = (z, material)
        return columns

    # Match the status legend: muted red fill and dashed edge show the next
    # unbuilt stage. Fit the full 81m apron into the fixed 142px UI width.
    current = front(built)
    ghost = set(front(nxt)) - set(current)
    def extent(x):
        a = math.floor((x - C + COSMETIC_APRON_HALF) * SCREEN_SIZE[0] / 81)
        b = math.floor((x - C + COSMETIC_APRON_HALF + 1) * SCREEN_SIZE[0] / 81) - 1
        return a, b
    for x, y in ghost:
        edge = any((x + dx, y + dy) not in ghost for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        fill = (200, 60, 46, 230) if edge and (x + y) % 3 else (120, 34, 28, 46)
        px, right = extent(x)
        py = (215 - y) * 2
        draw.rectangle((px, py, right, py + 1), fill=fill)
    for (x, y), (_, material) in current.items():
        name = base(material)
        colour = legacy.COLORS.get(name, (93, 103, 113))
        px, right = extent(x)
        py = (215 - y) * 2
        draw.rectangle((px, py, right, py + 1), fill=colour + (255,))
    return image


def write_all():
    data = export()
    blueprint_stages()
    import blueprints
    foundation = blueprints.drone_tower_industrial_foundation()
    target = RESOURCES / "data/jugcraft/blueprint/drone_tower_industrial_foundation.jugbp.json"
    target.write_text(json.dumps(foundation, indent=1) + "\n", encoding="utf-8")
    gui = RESOURCES / "assets/jugcraft/textures/gui/tower_industrial"
    gui.mkdir(parents=True, exist_ok=True)
    for tier in range(10):
        screen_image(tier).save(gui / f"tier_{tier}.png", optimize=True)
    return data


if __name__ == "__main__":
    data = write_all()
    for tier in data["tiers"]:
        print(f"tier {tier['tier']}: {len(tier['place'])} placed, {len(tier['docks'])} hangars, "
              f"capacity {tier['capacity']}, modules {tier['modules']}")
