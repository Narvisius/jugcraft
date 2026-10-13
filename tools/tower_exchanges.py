"""Industrial Drone Tower exchanges: deterministic closed 3D MBS, bounded to 64/60 parts.

Run only this file with --write to regenerate its assets. build(blocks) is the tower
generator's narrow geometry hook; coordinates follow legacy drone_tower (centre19).
Original art: OpenAI Codex (GPT-6), owner-approved tower redesign, October2026.
"""
from pathlib import Path
import argparse
import json
import math
import model_writer as mw

ROOT = Path(__file__).resolve().parent.parent
RID = lambda name: "jugcraft:" + name
TEXTURES = {name: "tower_detail_" + name for name in
            ("steel", "light_steel", "dark", "concrete", "copper", "porcelain", "rubber", "amber", "red", "panel", "screen", "vent")}
STATIONS = {"energy_exchange": "Drone Tower Energy Exchange", "cargo_exchange": "Drone Tower Cargo Exchange"}
CRAFTING = {
    "energy_exchange": (["SRS", "RCR", "SPS"], {"S": "#c:plates/steel", "R": "jugcraft:transformer_casing",
                         "C": "jugcraft:advanced_circuit", "P": "jugcraft:energy_exchange_port"}, 1),
    "cargo_exchange": (["SRS", "RCR", "SPS"], {"S": "#c:plates/steel", "R": "jugcraft:intake_funnel",
                        "C": "jugcraft:advanced_circuit", "P": "jugcraft:cargo_exchange_port"}, 1),
}


def footprint(energy, large=False):
    """Exact numbering shared by ExchangeStationBlock, not a bounding-box placeholder."""
    if large:
        return [(x, y, z) for y in range(5) for z in range(6) for x in range(9)]
    if not energy:
        return [(x, y, z) for y in range(3) for z in range(4) for x in range(5)]
    return [(x, y, z) for y in range(4) for z in range(5) for x in range(5)
            if y == 0 or y <= 2 and (1 <= z <= 3 or z == 0 and 1 <= x <= 3)
            or y == 3 and z == 2 and 1 <= x <= 3]


def state(name, facing, part, large=False):
    return f"{name}[facing={facing},part={part}" + (",large=true]" if large else "]")


def block_ids(large=False):
    return {state(name, facing, p, large): RID(state(name, facing, p, large))
            for name, facing, energy in (("energy_exchange", "west", True), ("cargo_exchange", "east", False))
            for p in range(len(footprint(energy, large)))}


def build(blocks, large=False):
    """Adds stations, preserving ground pickup/packager/terminal anchors and y0 landing field."""
    import drone_tower as d
    d.BLOCK_IDS.update(block_ids(large))
    for name, facing, energy, origin in (("energy_exchange", "west", True, (-3, 1, 23 if large else 21)),
                                        ("cargo_exchange", "east", False, (39, 1, 15 if large else 17))):
        for part, (x, y, z) in enumerate(footprint(energy, large)):
            dx, dz = (z, -x) if facing == "west" else (-z, x)
            material = state(name, facing, part, large)
            d.COLORS[material] = (72, 79, 86) if energy else (113, 120, 124)
            blocks[(origin[0] + dx, origin[1] + y, origin[2] + dz)] = material


def geometry(energy):
    """Actual static machinery: enclosed cargo housing and detailed energy cubicles.

    Cylinders are overlapping closed rectangular prisms whose union is an octagon. The
    regular exporter separates hidden coplanar caps and removes only fully buried faces;
    no transparent texture, animation, extra block entity or runtime tessellation is used.
    """
    if energy:
        from tower_power_exchange import geometry as power_geometry
        return power_geometry()
    from tower_cargo_exchange import geometry as cargo_geometry
    return cargo_geometry()
    out = []

    def box(a, b, texture="steel", rotation=None):
        assert all(a[i] < b[i] for i in range(3)), (a, b)
        entry = (a, b, TEXTURES[texture])
        out.append(entry + ({"rotation": rotation},) if rotation else entry)

    def cylinder(axis, center, start, end, radius, texture="steel"):
        """Eight-sided metal/ceramic section, with closed caps in every exported part."""
        ax = "xyz".index(axis)
        cross = [i for i in range(3) if i != ax]
        origin = list(center)
        origin[ax] = (start + end) / 2
        narrow = radius * math.tan(math.pi / 8)
        for first, second, angle in ((radius, narrow, 0), (narrow, radius, 0),
                                     (radius, narrow, 45), (narrow, radius, 45)):
            a, b = origin.copy(), origin.copy()
            a[ax], b[ax] = start, end
            for i, half in zip(cross, (first, second)):
                a[i], b[i] = origin[i] - half, origin[i] + half
            box(tuple(a), tuple(b), texture, (axis, angle, tuple(origin)) if angle else None)

    def ring_z(x, y, z, radius, thickness, depth, texture="steel"):
        """Open octagonal socket/bezel ring; a recessed backing is authored separately."""
        for n in range(8):
            angle = n * 45
            r = math.radians(angle)
            cx, cy = x + radius * math.cos(r), y + radius * math.sin(r)
            half = radius * math.tan(math.pi / 8) + .04
            # Local x is radial, y tangential; Minecraft permits only +/-45 degree turns.
            turn = ((angle + 45) % 90) - 45
            radial, tangent = thickness / 2, half
            if (angle - turn) % 180:
                radial, tangent = tangent, radial
            box((cx - radial, cy - tangent, z), (cx + radial, cy + tangent, z + depth),
                texture, ("z", turn, (cx, cy, z + depth / 2)) if turn else None)

    def bolts_front(x1, x2, y1, y2, z, texture="light_steel", size=.65):
        for x in (x1, x2):
            for y in (y1, y2):
                box((x - size / 2, y - size / 2, z), (x + size / 2, y + size / 2, z + .55), texture)

    def pull_handle(x, y, z, length=6):
        for yy in (y, y + length - .8):
            box((x, yy, z), (x + .8, yy + .8, z + 1.6), "dark")
        box((x, y, z - .8), (x + .8, y + length, z + .15), "light_steel")

    def channel_post(x, z, bottom, top):
        # Wide flanges and a narrower web make the load path visible from either side.
        for zz in (z, z + 3.1):
            box((x, bottom, zz), (x + 4, top, zz + .9), "dark")
        box((x + 1.6, bottom, z + .7), (x + 2.4, top, z + 3.3), "steel")

    deep = 80 if energy else 64
    # Poured foundation, separate steel skid and access tread; no oversized scenic cubes.
    box((0.2, 0, 0.2), (79.8, 3.5, deep - .2), "concrete")
    for x in (5, 71):
        for y in (3.7, 7):
            box((x, y, 7), (x + 4, y + .9, deep - 7), "dark")
        box((x + 1.5, 4.5, 7), (x + 2.5, 7.1, deep - 7), "steel")
    for z in (8, deep - 12):
        box((5, 8, z), (75, 11, z + 3), "steel")
    if energy:
        # Welded oil tank, raised bolted cover, steel mounting feet and lifting eyes.
        box((18, 17, 18), (62, 45.3, 62), "panel")
        box((17, 45.4, 17), (63, 47.4, 63), "dark")
        box((18.1, 47.45, 18.1), (61.9, 48, 61.9), "panel")
        for x in (20, 30, 40, 50, 60):
            for z in (18.2, 61.3):
                box((x - .45, 48.05, z - .45), (x + .45, 48.65, z + .45), "light_steel")
        for x in (19.4, 59.4):
            for z in (25, 53):
                box((x, 47.9, z), (x + 1.2, 52, z + 1), "steel")
                box((x, 47.9, z + 3), (x + 1.2, 52, z + 4), "steel")
                box((x, 51, z + .8), (x + 1.2, 52.2, z + 3.2), "steel")
        for x in (19, 58):
            for z in (20, 58):
                box((x - 1, 11.2, z - 1), (x + 4, 12.5, z + 4), "dark")
                box((x, 12.4, z), (x + 3, 17.1, z + 3), "steel")
                box((x - .6, 12.5, z - .6), (x + .2, 13.3, z + .2), "light_steel")
        # Radiators have pressed fin faces, circular headers, real return/feed branches,
        # flange clamps and a top/bottom oil connection at both ends of each bank.
        for x, tank_edge in ((2.1, 18.5), (66.9, 61.5)):
            inner = x + 5.5
            for z in range(20, 62, 3):
                box((x, 19, z), (x + 11, 43.2, z + .8), "steel")
                box((x + 1.5, 20, z - .12), (x + 9.5, 41.5, z + .12), "light_steel")
            for y in (17.6, 44.3):
                cylinder("z", (x + 5.5, y, 40), 18, 63, 1.45, "dark")
                for z in (23, 58):
                    cylinder("x", (0, y, z), min(inner, tank_edge), max(inner, tank_edge), 1.25, "steel")
                    cylinder("y", (inner, 0, z), min(y, 30), max(y, 30), 1.15, "steel")
                    cylinder("x", (0, y, z), min(inner, tank_edge) + .8, min(inner, tank_edge) + 1.7, 2.05, "light_steel")
            # Each bank is carried by a stand rather than floating off the tank wall.
            for z in (23, 57):
                box((x + 3.8, 11.2, z), (x + 7.2, 16.4, z + 2.2), "dark")
        # Three high-voltage porcelain bushings: stepped sheds, stud nuts and copper
        # jumpers bend into three separate outgoing cable clamps on the roof.
        for x in (24, 40, 56):
            cylinder("y", (x, 0, 40), 48, 49.5, 4, "dark")
            cylinder("y", (x, 0, 40), 49.6, 59.3, 1.75, "porcelain")
            for y, radius in ((50, 3.9), (52.3, 3.65), (54.6, 3.4), (56.9, 3.15)):
                cylinder("y", (x, 0, 40), y, y + .9, radius, "porcelain")
            cylinder("y", (x, 0, 40), 59.4, 62.4, 1.2, "copper")
            cylinder("y", (x, 0, 40), 60.1, 61, 2, "dark")
            box((x - .65, 61.5, 33.5), (x + .65, 62.4, 40), "copper")
            box((x - .65, 58, 33.5), (x + .65, 62, 34.4), "copper")
            cylinder("y", (x, 0, 34), 49.2, 58.5, 1.2, "rubber")
            cylinder("y", (x, 0, 34), 48, 49.3, 2.15, "steel")
        # Switchgear is a real folded enclosure. Door leaves surround the socket recess,
        # so its throat is deeper than the panel instead of a flat colored square.
        box((16.5, 16.1, 1.5), (63.5, 46.9, 14.8), "dark")
        for x in (17, 33, 49):
            if x == 33:
                # The real cable enters at the exact centre of port part26:
                # x40/y24, not the decorative old socket's y28.7. A full
                # folded cover backs the sealed gland, avoiding an open hole.
                box((x, 17, .55), (x + 14, 46, 1.25), "panel")
            else:
                box((x, 17, .55), (x + 14, 46, 1.25), "panel")
                pull_handle(x + 10.8, 31, -.65)
            for y in (20, 40):
                cylinder("y", (x + .7, 0, .4), y, y + 3, .6, "steel")
            box((x + 2, 41, -.15), (x + 6, 43.7, .3), "dark")
            box((x + 2.6, 41.6, -.4), (x + 3.7, 42.8, -.1), "amber")
            box((x + 2, 37.8, -.15), (x + 7.5, 39.2, .25), "light_steel")
        # Universal sealed compression gland: the outer rubber boot is
        # 6.4px square at the exact z0 block boundary, enclosing all three
        # cables' 6px body and 0.1px surface ridges. The neutral gasket joins
        # copper/aluminum/silver without recolouring their existing assets.
        box((33.5, 17.5, 1.25), (46.5, 30.5, 2.0), "dark")
        box((34.0, 18.0, .6), (46.0, 30.0, 1.25), "light_steel")
        ring_z(40, 24, .25, 4.15, .8, .55, "steel")
        box((36.8, 20.8, 0), (43.2, 27.2, .65), "rubber")
        box((36.5, 20.5, .65), (43.5, 27.5, 1.4), "dark")
        bolts_front(34.7, 45.3, 18.7, 29.3, .05)
        # Analog meter has a raised bezel, recessed face and dimensional pointer.
        ring_z(23.7, 28.8, -.35, 3.25, .65, .9, "light_steel")
        cylinder("z", (23.7, 28.8, 0), .15, .45, 2.85, "porcelain")
        box((23.5, 28.4, -.15), (24.0, 31.1, .05), "dark", ("z", -22.5, (23.7, 28.8, 0)))
        for x, y in ((21.8, 30.5), (23.6, 31.1), (25.4, 30.5)):
            box((x, y, -.05), (x + .35, y + .55, .12), "dark")
        # Rear inspection cover, oil drain, bolted gland and a real handwheel.
        box((34, 20, 62.1), (46, 31, 63.2), "dark")
        box((35, 21, 63.3), (45, 30, 63.7), "steel")
        bolts_front(35.8, 44.2, 21.8, 29.2, 63.75)
        cylinder("z", (40, 18, 0), 62, 68, 1.3, "copper")
        cylinder("z", (40, 18, 0), 64.5, 65.5, 2.5, "steel")
        cylinder("y", (40, 0, 67), 14, 18.2, 1.2, "copper")
        ring_z(40, 18, 68.1, 3, .6, .7, "red")
        box((37.4, 17.6, 68.15), (42.6, 18.4, 68.75), "red")
        box((39.6, 15.4, 68.15), (40.4, 20.6, 68.75), "red")
        # Two service treads keep the input panel reachable from the apron.
        box((19, 3.6, .5), (61, 6.5, 6), "dark")
        box((20, 6.7, 5), (60, 10, 10.5), "steel")
    else:
        # Bolted channel columns, gusseted canopy and gutters: load-bearing pieces
        # stay distinct, with unobstructed maintenance sides and an open conveyor throat.
        for x in (2, 74):
            for z in (3, 57):
                channel_post(x, z, 4.5, 43)
                box((x - 1, 3.6, z - 1), (x + 5, 5, z + 5), "steel")
                for xx in (x - .5, x + 4.2):
                    for zz in (z - .5, z + 4.2):
                        box((xx, 5.05, zz), (xx + .65, 5.8, zz + .65), "light_steel")
        for z in (4, 58):
            for y in (39.5, 42.3):
                box((2, y, z), (78, y + .9, z + 3), "dark")
            box((2, 40.3, z + 1.1), (78, 42.4, z + 1.9), "steel")
        for x in (3, 37.5, 74):
            box((x, 42.5, 2), (x + 3, 44, 62), "dark")
        for x, direction in ((7, 1), (72, -1)):
            for z in (4.4, 58.4):
                box((x - 5.4, 35.3, z), (x + 5.4, 37, z + 1.3), "steel",
                    ("z", 45 * direction, (x, 36.15, z + .65)))
        box((.2, 44.1, .2), (79.8, 45.4, 63.8), "panel")
        for x in range(8, 80, 12):
            box((x, 45.5, .4), (x + .65, 46.5, 63.6), "steel")
        for x in (.3, 77):
            box((x, 42.5, .3), (x + 2, 44, 63.7), "steel")
        cylinder("y", (76.9, 0, 59.5), 5, 42.4, .75, "dark")
        # Packaging and control cabinets with door gaps, piano hinge barrels, pulls,
        # separate inspection covers and folded relief louvers.
        for x in (8, 47):
            box((x, 10, 37), (x + 25, 39, 59), "dark")
            for xx in (x + 1, x + 12.7):
                box((xx, 11, 35.8), (xx + 11.3, 38, 36.8), "panel")
                pull_handle(xx + 8.2, 21, 34.5, 6.5)
                for y in (14, 31):
                    cylinder("y", (xx + .6, 0, 35.6), y, y + 4, .5, "steel")
            box((x + 2, 28.8, 35.15), (x + 10.5, 36.1, 35.65), "dark")
            for y in (30, 32, 34):
                box((x + 2.7, y, 34.65), (x + 9.8, y + .55, 35.45), "steel",
                    ("x", 22.5, (x + 6.25, y + .275, 35.05)))
            box((x + 3, 16, 59.15), (x + 21.5, 31, 59.7), "steel")
            bolts_front(x + 4.1, x + 20.4, 17.1, 29.9, 59.8)
        # Roller conveyor: individual octagonal drums turn on narrower steel shafts,
        # supported by bearing blocks inside folded frame rails. The right-hand drive
        # motor and gearbox feed a fully covered chain run, with removable service hatch.
        for z in (4, 31):
            for x in (25, 52):
                box((x, 6, z), (x + 3, 17, z + 3), "dark")
        box((23, 14, 1), (57, 15.3, 35), "dark")
        for z in range(3, 35, 3):
            cylinder("x", (0, 19.6, z), 25, 55, 1.3, "light_steel")
            cylinder("x", (0, 19.6, z), 22, 58, .6, "dark")
            for x in (22.5, 55.5):
                box((x, 18, z - 1), (x + 1.7, 21.1, z + 1), "steel")
        for x in (22, 56):
            box((x, 15.5, 1), (x + 2, 18.7, 35), "steel")
            box((x, 22, 1), (x + 2, 23.2, 35), "light_steel")
            for z in (2, 16, 31):
                box((x, 18.5, z), (x + .7, 22.3, z + 1.4), "dark")
        box((58.2, 17, 1), (61.8, 22.5, 35), "dark")
        box((61.9, 17.8, 7), (62.5, 21.7, 28), "steel")
        for z in (8, 26.5):
            box((62.55, 18.5, z), (63.1, 19.2, z + .7), "light_steel")
        box((62, 12, 25), (69.5, 20.5, 34), "steel")
        cylinder("z", (66, 16, 0), 10.2, 25, 3.4, "dark")
        cylinder("z", (66, 16, 0), 9, 11, 3, "steel")
        for z in (14, 17, 20, 23):
            cylinder("z", (66, 16, 0), z, z + .55, 3.7, "steel")
        box((64, 20.3, 17), (68, 23, 22), "dark")
        box((61.3, 7.1, 9), (70.7, 10, 34), "steel")
        for x in (62.2, 69):
            box((x, 9, 12), (x + 1, 13.5, 29), "dark")
        # Sealing bridge with guide tracks, inspection wheel and suspended press head.
        for x in (20, 57):
            box((x, 14, 23), (x + 3, 38, 29), "dark")
            box((x + .7, 21, 22.1), (x + 2.3, 35, 22.9), "light_steel")
        box((20, 35, 23), (60, 38, 29), "steel")
        cylinder("y", (40, 0, 26), 31, 35.2, 1.6, "steel")
        box((32, 29, 23), (48, 31.2, 30), "dark")
        box((33, 28, 23.5), (47, 29.1, 29.5), "steel")
        box((26, 35.1, 22.1), (34, 37.4, 22.8), "dark")
        box((27, 35.6, 21.85), (32.8, 36.9, 22.05), "amber")
        # Cargo port remains at part(2,1,0): liner, guide tracks and a retracted slatted
        # shutter behind the conveyor. Nothing extends through its loading approach.
        for x in (31, 47):
            box((x, 20.5, .2), (x + 2, 34.5, 2.5), "steel")
            box((x + .5, 21.2, -.1), (x + 1.5, 32.7, .15), "amber")
        box((31, 32.5, .2), (49, 34.5, 4), "dark")
        for y in (33, 34, 35):
            box((33, y, 3), (47, y + .7, 4), "steel")
        box((33, 21, 6), (47, 32.3, 6.6), "rubber")
        box((34, 22, 5.4), (46, 30.8, 5.9), "dark")
        # Accessible operator console: recessed display, stop button and guarded rail.
        box((5, 15, 3), (18, 34, 15), "panel")
        box((6.5, 24.3, 1.7), (16.5, 31.7, 2.8), "dark")
        box((7.2, 25, 1.65), (15.8, 31, 1.7), "screen")
        cylinder("z", (9.4, 20.8, 0), 1.7, 2.6, 1.6, "amber")
        cylinder("z", (9.4, 20.8, 0), .8, 1.65, 1.1, "red")
        cylinder("z", (14, 20.8, 0), 1.1, 2.6, .8, "dark")
        pull_handle(15.7, 15.9, 1.5, 4.5)
        for z in (20, 31):
            box((6, 4, z), (8, 20, z + 2), "steel")
        box((6, 20.2, 19), (8, 22, 34), "amber")
        # Raised maintenance tread with a shallow physical approach ramp.
        box((8.5, 3.7, 1), (20.8, 5, 33), "steel")
        box((8.5, 1.5, .1), (20.8, 3.8, 6.1), "steel",
            ("x", -22.5, (14.65, 2.65, 3.1)))
    return out


def write_assets(root=ROOT, lang=None):
    from tower_power_exchange import write_textures
    from tower_cargo_exchange import write_textures as write_cargo_textures
    write_textures(root)
    write_cargo_textures(root)
    assets = root / "src/main/resources/assets/jugcraft"
    data = root / "src/main/resources/data/jugcraft"
    for name, energy in (("energy_exchange", True), ("cargo_exchange", False)):
        elements = geometry(energy)
        textures = {texture: RID("block/" + texture) for texture in mw.texture_names(elements)}
        textures["particle"] = RID("block/tower_power_paint" if energy else "block/tower_cargo_housing")
        if energy:
            parts = mw.split_model(name, elements, footprint(energy))
        else:
            from tower_cargo_exchange import native_parts as cargo_native_parts
            parts = cargo_native_parts()
        variants = {}
        for p in range(64):
            model = f"{name}_part{p}"
            # Cargo's four unused legacy states remain untouched; only its exact
            # 60 active parts belong to the replacement. Their existing empty
            # model aliases keep old invalid/saved states harmless.
            if energy or p < len(parts):
                mw.write(assets / "models/block" / (model + ".json"),
                         {"parent": "minecraft:block/block", "ambientocclusion": False,
                          "textures": textures, "elements": parts[p] if p < len(parts) else []})
            for facing, angle in mw.FACING_Y.items():
                variants[f"facing={facing},part={p}"] = {"model": RID("block/" + model), "y": angle}
        mw.write(assets / "blockstates" / (name + ".json"), {"variants": variants})
        if energy:
            item_elements = mw.scaled_elements(elements)
        else:
            from tower_cargo_exchange import item_elements as cargo_item_elements
            item_elements = cargo_item_elements()
        # Scaling the many closed faceted caps can make new near-coplanar neighbours.
        # Settle once before write()'s final UV/separation pass, in item-model pixels.
        if energy:
            mw.finish_elements(item_elements)
        mw.write(assets / "models/item" / (name + ".json"), {
            "parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures,
            "elements": item_elements, "display": {
                "gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [.65, .65, .65]},
                "ground": {"scale": [.3, .3, .3]}, "fixed": {"scale": [.6, .6, .6]}}})
        mw.write(assets / "items" / (name + ".json"), {"model": {"type": "minecraft:model", "model": RID("item/" + name)}})
        mw.write(data / "loot_table/blocks" / (name + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": RID(name)}],
                "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": RID("blocks/" + name)})
        keys = {"S": "#c:plates/steel", "C": "jugcraft:advanced_circuit",
                "P": RID("energy_exchange_port" if energy else "cargo_exchange_port"),
                "R": RID("transformer_casing" if energy else "intake_funnel")}
        mw.write(data / "recipe" / (name + ".json"), {
            "fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                       {"condition": "jugcraft:feature_enabled", "feature": "drones"}],
            "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SRS", "RCR", "SPS"],
            "key": keys, "result": {"id": RID(name), "count": 1}})
    language = assets / "lang/en_us.json"
    standalone = lang is None
    if standalone:
        lang = json.loads(language.read_text(encoding="utf-8"))
    lang.update({"block.jugcraft.energy_exchange": "Drone Tower Energy Exchange",
                 "block.jugcraft.cargo_exchange": "Drone Tower Cargo Exchange",
                 "message.jugcraft.exchange.not_linked": "Exchange unlinked or incomplete. Place near your tower and right-click to connect.",
                 "message.jugcraft.exchange.energy": "Tower power: %s / %s JE"})
    if standalone:
        mw.write(language, lang)
    from tower_large_exchanges import write_assets as write_large_assets
    write_large_assets(root)
    write_size_variants(root)
    print("Wrote energy/cargo exchange assets (64 / 60 parts; no drone assets).")


def write_size_variants(root=ROOT):
    """Missing large properties keep legacy saved parts; new items select the larger art."""
    assets = root / "src/main/resources/assets/jugcraft"
    mw.write(assets / "models/block/exchange_station_empty.json", {"parent": "minecraft:block/block",
             "textures": {"particle": RID("block/tower_cargo_housing")}, "elements": []})
    for name, energy in (("energy_exchange", True), ("cargo_exchange", False)):
        variants = {}
        for part in range(270):
            for facing, angle in mw.FACING_Y.items():
                old = f"{name}_part{part}" if part < len(footprint(energy)) else "exchange_station_empty"
                variants[f"facing={facing},part={part},large=false"] = {"model": RID("block/" + old), "y": angle}
                variants[f"facing={facing},part={part},large=true"] = {"model": RID(f"block/{name}_large_part{part}"), "y": angle}
        mw.write(assets / "blockstates" / (name + ".json"), {"variants": variants})
        mw.write(assets / "models/item" / (name + ".json"), {"parent": RID(f"item/{name}_large")})


def exported_assembly(name, energy):
    """Read the same per-block JSONs and PNGs shipped to Minecraft, not design geometry."""
    import art_check
    assets = ROOT / "src/main/resources/assets/jugcraft"
    members = [(assets / "models/block" / f"{name}_part{part}.json", tuple(c * 16 for c in cell))
               for part, cell in enumerate(footprint(energy))]
    faces, boxes = art_check._assembly(members, (assets,))
    return members, faces, boxes


def check_assets():
    """Bounded station-only export checks, including seams across all 64/60 block parts."""
    import art_check
    errors = []
    for name, energy in (("energy_exchange", True), ("cargo_exchange", False)):
        members, faces, boxes = exported_assembly(name, energy)
        item_model = ROOT / "src/main/resources/assets/jugcraft/models/item" / f"{name}.json"
        for path, _ in members + [(item_model, (0, 0, 0))]:
            model = json.loads(path.read_text(encoding="utf-8"))
            elements, lookup, _ = art_check._resolve(model, (ROOT / "src/main/resources/assets/jugcraft",))
            own, raw_uv = art_check._model_faces(elements, lookup)
            bad_uv = [uv for uv in raw_uv if min(uv) < -.001 or max(uv) > 16.001]
            if bad_uv:
                errors.append(f"{path.stem}: UVs outside sprite: {bad_uv[0]}")
            if art_check._open_faces(elements, own):
                errors.append(f"{path.stem}: exposed missing face")
            for el in elements:
                if any(el["from"][a] >= el["to"][a] for a in range(3)):
                    errors.append(f"{path.stem}: reversed or zero-volume element")
                if min(el["from"]) < -16 or max(el["to"]) > 32:
                    errors.append(f"{path.stem}: element outside Minecraft model bounds")
            for i, j, gap, polygon, u, v, normal, plane in art_check._pairs(own, art_check.N1_GAP):
                if not (gap == 0 and art_check._layered(elements, own[i], own[j])) and art_check._differ(
                        own[i], own[j], polygon, u, v, normal, plane):
                    errors.append(f"{path.stem}: coplanar/near-coplanar differently drawn surfaces ({gap:.4f}px)")
                    break
        errors.extend(art_check._cross_errors(name, faces, boxes, set(footprint(energy))))
        print(f"{name}: {len(members)} parts, {len(geometry(energy))} authored solids, "
              f"{len(faces)} exported faces checked")
    if errors:
        raise ValueError("\n".join(errors))
    print("Station model bounds, UVs, closed faces, coplanar surfaces and cross-part seams pass.")


def preview_assets():
    """Create truthful closeups from exported JSON/PNGs with back-face culling and z-buffer."""
    import numpy as np
    from PIL import Image, ImageDraw
    import armor_preview as raster
    output = ROOT / "build/drone_tower"
    output.mkdir(parents=True, exist_ok=True)
    metadata = {"source": "exported runtime block-part JSON and PNG textures",
                "renderer": "offline orthographic, static back-face culling and z-buffer; not a game screenshot",
                "stations": {}}
    for name, energy in (("energy_exchange", True), ("cargo_exchange", False)):
        _, faces, _ = exported_assembly(name, energy)
        images = {}
        for face in faces:
            if face.tex not in images:
                images[face.tex] = np.array(Image.open(face.tex).convert("RGBA"))
        views = (("front", (35, 24)), ("rear", (145, 24)))
        panels = []
        counts = []
        for title, view in views:
            camera = raster.camera(view)[0]
            quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), images[f.tex])
                     for f in faces if np.dot(f.normal, camera) > 0]
            image = raster.render(quads, view, scale=8, background=(35, 39, 44), lighting="blockbench")
            # Fixed canvases make the two views easy to compare without altering the geometry.
            panel = Image.new("RGB", (980, 760), (35, 39, 44))
            panel.paste(image, ((980 - image.width) // 2, 42 + (700 - image.height) // 2))
            draw = ImageDraw.Draw(panel)
            draw.text((18, 14), STATIONS[name] + " / " + title + " / actual exported assets", fill=(230, 233, 237))
            draw.text((18, 738), "Offline model review - static geometry, shared runtime materials - no drones", fill=(183, 193, 202))
            panels.append(panel)
            counts.append(len(quads))
        sheet = raster.sheet(panels, 2)
        target = output / f"{name}_v3.png"
        sheet.save(target)
        metadata["stations"][name] = {"parts": len(footprint(energy)), "authored_solids": len(geometry(energy)),
                                       "exported_faces": len(faces), "visible_faces": counts,
                                       "image": target.name}
        print(target)
    mw.write(output / "exchange_v3_review.json", metadata)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--preview", action="store_true")
    args = parser.parse_args()
    if args.write:
        write_assets()
    if args.check:
        check_assets()
    if args.preview:
        preview_assets()
    if not (args.write or args.check or args.preview):
        print("Use --write, --check and/or --preview for the exchange stations only.")
