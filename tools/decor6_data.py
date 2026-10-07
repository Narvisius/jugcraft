"""JSON resources for the sixth batch of Halloween decorations, the haunted house and yard, from tools/agriculture.py:
the Rocking Chair, the Lurking Eyes, the Silhouette Window, the Spooky Music Box and the Giant Fake Spider; their names,
loot and tags, and the quads the client draws the rocking chair and the spider from (assets/jugcraft/decor_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: block model
element rotation, a select on a block state property for an item and copy_state in loot. Model rotations are
right-handed: about x, a positive angle turns +y toward +z.

The chair and the spider are drawn by block entity renderers so they can move; their boxes are written out as textured
quads (vertices in model pixels, UVs as vanilla maps a block model's faces, element rotations applied, at any angle).
The chair's boxes also make the model its item shows (whose rotations keep to vanilla's 22.5-degree steps).
"""
import copy
import math

from agriculture import ROCKING_CHAIR, LURKING_EYES, SILHOUETTE_WINDOW, MUSIC_BOX, GIANT_FAKE_SPIDER
from decor_data import MOD, HORIZONTAL, SIDES, rid, turned, box, block_model, flat_item, self_drop
from decor3_data import fitted
from model_writer import separate_coplanar


# ---------------------------------------------------------------- quads for the block entity renderers

FACE_CORNERS = {
    "north": (((0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)), (0, 0, -1)),
    "south": (((1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1)), (0, 0, 1)),
    "west": (((0, 0, 1), (0, 1, 1), (0, 1, 0), (0, 0, 0)), (-1, 0, 0)),
    "east": (((1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)), (1, 0, 0)),
    "up": (((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)), (0, 1, 0)),
    "down": (((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)), (0, -1, 0)),
}
AXES = {"x": 0, "y": 1, "z": 2}


def vanilla_uv(face, x, y, z):
    return {"north": (16 - x, 16 - y), "south": (x, 16 - y), "west": (z, 16 - y), "east": (16 - z, 16 - y),
            "up": (x, z), "down": (x, 16 - z)}[face]


def turn(point, rotation):
    """Turns a point about an element rotation's axis and origin (right-handed, as block models turn)."""
    i = AXES[rotation["axis"]]
    a, b = {0: (1, 2), 1: (2, 0), 2: (0, 1)}[i]
    rad = math.radians(rotation["angle"])
    origin = rotation.get("origin", [8, 8, 8])
    p = [point[k] - origin[k] for k in range(3)]
    out = list(p)
    out[a] = p[a] * math.cos(rad) - p[b] * math.sin(rad)
    out[b] = p[a] * math.sin(rad) + p[b] * math.cos(rad)
    return [out[k] + origin[k] for k in range(3)]


# A zero-thickness plane drawn from both sides (two opposite faces on one plane) has each face lifted this far (pixels)
# along its own normal: the cutout and translucent entity types do not cull, so twins on one plane would fight.
TWO_SIDED_LIFT = 0.05
OPPOSITE = {"north": "south", "south": "north", "west": "east", "east": "west", "up": "down", "down": "up"}


def _turned(element, point):
    """A point of an element turned the way quads() turns it (each rotation in order, no rescale)."""
    for rotation in element.get("rotations") or ([element["rotation"]] if "rotation" in element else []):
        point = turn(point, rotation)
    return point


def quads(elements, textures):
    """The textured quads of block model `elements` whose "#key" textures `textures` names (block texture names).
    Differently drawn faces that share a plane are first pulled apart (model_writer.separate_coplanar on a copy:
    whole boxes, never single quads), so nothing flickers."""
    elements = copy.deepcopy(elements)
    separate_coplanar(elements, world=_turned)
    out = []
    for element in elements:
        frm, to = element["from"], element["to"]
        rotations = element.get("rotations") or ([element["rotation"]] if "rotation" in element else [])
        for face, spec in element["faces"].items():
            corners, normal = FACE_CORNERS[face]
            points = [[to[k] if corner[k] else frm[k] for k in range(3)] for corner in corners]
            axis = [abs(c) for c in normal].index(1)
            if abs(to[axis] - frm[axis]) < 1e-9 and OPPOSITE[face] in element["faces"]:
                points = [[p[k] + normal[k] * TWO_SIDED_LIFT for k in range(3)] for p in points]
            uvs = [vanilla_uv(face, *p) for p in points]
            if "uv" in spec:
                us, vs = [u for u, _ in uvs], [v for _, v in uvs]
                u0, v0, u1, v1 = spec["uv"]
                uvs = [(u0 + (u - min(us)) / ((max(us) - min(us)) or 1) * (u1 - u0),
                        v0 + (v - min(vs)) / ((max(vs) - min(vs)) or 1) * (v1 - v0)) for u, v in uvs]
            n = list(normal)
            for rotation in rotations:
                points = [turn(p, rotation) for p in points]
                n = turn(n, dict(rotation, origin=[0, 0, 0]))
            out.append({"texture": textures[spec["texture"].lstrip("#")], "normal": [round(v, 4) for v in n],
                        "vertices": [[round(p[0], 4), round(p[1], 4), round(p[2], 4), round(u / 16, 5), round(v / 16, 5)]
                                     for p, (u, v) in zip(points, uvs)]})
    return out


# ---------------------------------------------------------------- the rocking chair

CHAIR_TEXTURES = {"wood": "rocking_chair_wood", "cushion": "rocking_chair_cushion"}


def chair_elements(item):
    """A high-backed chair facing north (its sitter looks north) on two curved runners. For the item model the
    leaning back and the runners' tips turn by 22.5 degrees; for the renderer by a gentler 15 and 20."""
    back = {"origin": [8, 8, 12.5], "axis": "x", "angle": 22.5 if item else 15}
    tip = 22.5 if item else 20
    elements = []
    for x in (2, 13):
        elements += [box((x, 0, 3), (x + 1, 1.5, 13), "#wood"),
                     box((x, 0, -1), (x + 1, 1.5, 3), "#wood", rotation={"origin": [x + 0.5, 0, 3], "axis": "x", "angle": tip}),
                     box((x, 0, 13), (x + 1, 1.5, 17), "#wood", rotation={"origin": [x + 0.5, 0, 13], "axis": "x", "angle": -tip})]
    for x in (2.5, 12.5):
        for z in (3.5, 11.5):
            elements.append(box((x, 1.5, z), (x + 1, 8, z + 1), "#wood"))
        elements += [box((x - 0.5, 11, 3), (x + 1.5, 12, 12.5), "#wood"),          # arm rests
                     box((x, 8, 3.5), (x + 1, 11, 4.5), "#wood")]                   # arm supports
    elements += [box((2, 8, 3), (14, 9, 13), "#wood"),                              # the seat
                 box((3, 9, 3.5), (13, 10, 12), "#cushion")]
    for element in [box((2.5, 8, 12), (3.5, 23, 13), "#wood"), box((12.5, 8, 12), (13.5, 23, 13), "#wood"),   # back posts
                    box((3, 20.5, 11.75), (13, 22.5, 13.25), "#wood"), box((3.5, 10, 12), (12.5, 11, 13), "#wood"),  # rails
                    box((5, 11, 12.25), (6, 20.5, 12.75), "#wood"), box((7.5, 11, 12.25), (8.5, 20.5, 12.75), "#wood"),
                    box((10, 11, 12.25), (11, 20.5, 12.75), "#wood")]:                # slats
        element["rotation"] = back
        elements.append(element)
    return elements


# ---------------------------------------------------------------- the silhouette window

def window_model(design):
    """A thin frame of dark wood round a pane of paper with the cut-out, upright across the block from west to east."""
    elements = [box((0, 0, 7), (16, 1, 9), "#frame"), box((0, 15, 7), (16, 16, 9), "#frame"),
                box((0, 1, 7), (1, 15, 9), "#frame"), box((15, 1, 7), (16, 15, 9), "#frame"),
                box((1, 1, 7.5), (15, 15, 8.5), "#paper", faces=("north", "south"),
                    uvs={"north": (1, 1, 15, 15), "south": (1, 1, 15, 15)})]
    return block_model({"frame": "silhouette_window_frame", "paper": f"silhouette_window_{design}"}, elements, "silhouette_window_frame")


# ---------------------------------------------------------------- the music box

def music_box_model(open_):
    """A lacquered box with brass corners and a brass catch at the front (north). Open, its lid stands back on its
    hinge and a little ghost turns on a spindle above the velvet."""
    elements = [box((3, 0, 4), (13, 5, 12), "#lacquer", textures={"up": "#velvet"} if open_ else None)]
    for x in (2.75, 12.25):
        for z in (3.75, 11.25):
            elements.append(box((x, -0.01, z), (x + 1, 5.01, z + 1), "#brass"))
    elements.append(box((7, 2.5, 3.75), (9, 4, 4), "#brass", faces=("north", "east", "west", "up", "down")))
    if open_:
        elements += [box((3, 5, 11), (13, 6, 19), "#lacquer", rotation={"origin": [8, 5, 12], "axis": "x", "angle": -45}),
                     box((7.75, 5, 7.75), (8.25, 6, 8.25), "#brass"),
                     box((7, 6, 7), (9, 9.5, 9), "#ghost")]
    else:
        elements.append(box((3, 5, 4), (13, 6, 12), "#lacquer"))
    return fitted(block_model({"lacquer": "music_box_lacquer", "brass": "music_box_brass", "velvet": "music_box_velvet",
                               "ghost": "music_box_ghost"}, elements, "music_box_lacquer"))


# ---------------------------------------------------------------- the giant fake spider

SPIDER_TEXTURES = {"hair": "giant_fake_spider_hair", "eye": "giant_fake_spider_eye", "silk": "giant_fake_spider_silk"}


def spider_elements():
    """A spider hanging from its back: the thread ties on at (8, 0, 8), the body below it, the head to the north.
    Eight legs, four a side, each a thigh rising outward and a shin bending down to a foot below the body."""
    elements = [box((3, -9, 5), (13, -1, 16), "#hair"),          # abdomen
                box((5, -8, -1), (11, -3, 5), "#hair"),          # thorax
                box((6, -7, -4), (10, -3.5, -1), "#hair"),       # head
                box((6.5, -5, -4.1), (7.5, -4, -4), "#eye", faces=("north",)),
                box((8.5, -5, -4.1), (9.5, -4, -4), "#eye", faces=("north",)),
                box((7, -6.5, -4.1), (9, -5.5, -4), "#eye", faces=("north",))]
    for i, z in enumerate((-0.5, 1.0, 2.5, 4.0)):
        spread = (i - 1.5) * 18       # the legs fan from forward to back
        for side in (1, -1):
            x = 11 if side > 0 else 5
            yaw = {"origin": [x, -5, z + 0.5], "axis": "y", "angle": -spread * side}
            thigh = box((x, -5.5, z), (x + 9, -4.5, z + 1), "#hair") if side > 0 else box((x - 9, -5.5, z), (x, -4.5, z + 1), "#hair")
            thigh["rotations"] = [{"origin": [x, -5, z + 0.5], "axis": "z", "angle": 35 * side}, yaw]
            tip = x + 9 * side * math.cos(math.radians(35))
            rise = 9 * math.sin(math.radians(35))
            shin = box((tip - 0.5, -5 + rise - 13, z), (tip + 0.5, -5 + rise, z + 1), "#hair")
            shin["rotations"] = [{"origin": [tip, -5 + rise, z + 0.5], "axis": "z", "angle": 12 * side}, yaw]
            elements += [thigh, shin]
    return elements


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    chair = ROCKING_CHAIR["block"]
    # The chair is drawn by the client; its block model only gives the particles. The item shows the whole chair.
    write(models / f"{chair}.json", {"textures": {"particle": rid(f"block/{CHAIR_TEXTURES['wood']}")}})
    write(models / f"{chair}_item.json", fitted(block_model(CHAIR_TEXTURES, chair_elements(True), CHAIR_TEXTURES["wood"])))
    write(states / f"{chair}.json", {"variants": {f"facing={f}": {"model": rid(f"block/{chair}")} for f in HORIZONTAL}})
    write(root / "items" / f"{chair}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{chair}_item")}})
    lang[f"block.{MOD}.{chair}"] = ROCKING_CHAIR["display"]

    eyes = LURKING_EYES["block"]
    write(models / f"{eyes}.json", {"textures": {"particle": rid(f"block/{eyes}_particle")}})
    write(states / f"{eyes}.json", {"variants": {f"facing={f}": {"model": rid(f"block/{eyes}")} for f in HORIZONTAL + ("up", "down")}})
    flat_item(root, write, eyes)
    lang[f"block.{MOD}.{eyes}"] = LURKING_EYES["display"]

    window = SILHOUETTE_WINDOW["block"]
    for design in SILHOUETTE_WINDOW["designs"]:
        write(models / f"{window}_{design}.json", window_model(design))
    write(states / f"{window}.json", {"variants": {f"design={d},facing={f}": turned(rid(f"block/{window}_{d}"), f)
                                                   for f in HORIZONTAL for d in SILHOUETTE_WINDOW["designs"]}})
    # The item shows the design it was broken with (the loot table copies it onto it).
    first = SILHOUETTE_WINDOW["designs"][0]
    write(root / "items" / f"{window}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "design",
        "cases": [{"when": d, "model": {"type": "minecraft:model", "model": rid(f"block/{window}_{d}")}}
                  for d in SILHOUETTE_WINDOW["designs"] if d != first],
        "fallback": {"type": "minecraft:model", "model": rid(f"block/{window}_{first}")}}})
    lang[f"block.{MOD}.{window}"] = SILHOUETTE_WINDOW["display"]

    box_ = MUSIC_BOX["block"]
    write(models / f"{box_}.json", music_box_model(False))
    write(models / f"{box_}_open.json", music_box_model(True))
    write(states / f"{box_}.json", {"variants": {f"facing={f},open={str(o).lower()},powered={str(p).lower()}":
                                                 turned(rid(f"block/{box_}{'_open' if o else ''}"), f)
                                                 for f in HORIZONTAL for o in (False, True) for p in (False, True)}})
    write(root / "items" / f"{box_}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{box_}")}})
    lang[f"block.{MOD}.{box_}"] = MUSIC_BOX["display"]

    spider = GIANT_FAKE_SPIDER["block"]
    # Only the knot is a block; the thread and the spider are drawn by the client.
    write(models / f"{spider}.json", block_model({"silk": SPIDER_TEXTURES["silk"]}, [
        box((6.5, 13, 6.5), (9.5, 16, 9.5), "#silk"), box((7.5, 11.5, 7.5), (8.5, 13, 8.5), "#silk")], SPIDER_TEXTURES["silk"]))
    write(states / f"{spider}.json", {"variants": {f"drop={n}": {"model": rid(f"block/{spider}")}
                                                   for n in range(1, GIANT_FAKE_SPIDER["max_drop"] + 1)}})
    flat_item(root, write, spider)
    lang[f"block.{MOD}.{spider}"] = GIANT_FAKE_SPIDER["display"]

    write(root / "decor_quads.json", {"rocking_chair": quads(chair_elements(False), CHAIR_TEXTURES),
                                      "giant_fake_spider": quads(spider_elements(), SPIDER_TEXTURES)})


def loot(out, write):
    """Each drops itself; a window keeps its design."""
    for block in (ROCKING_CHAIR["block"], LURKING_EYES["block"], MUSIC_BOX["block"], GIANT_FAKE_SPIDER["block"]):
        write(out / f"{block}.json", self_drop(block))
    window = SILHOUETTE_WINDOW["block"]
    write(out / f"{window}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(window),
                     "modifier": {"type": "minecraft:copy_state", "block": rid(window), "properties": ["design"]}}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{window}")})


def tags(tags):
    for block in (ROCKING_CHAIR["block"], SILHOUETTE_WINDOW["block"], MUSIC_BOX["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    for block in (LURKING_EYES["block"], GIANT_FAKE_SPIDER["block"]):
        tags.add("block", "minecraft:mineable/hoe", rid(block))
