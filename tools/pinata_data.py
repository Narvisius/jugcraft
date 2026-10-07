"""JSON resources for the piñata party (fall addition 28), from tools/pinata.py: the items' models, names and messages,
the Blindfold's equipment asset, and the quads the client draws the piñatas from (assets/jugcraft/pinata_quads.json):
for each kind its body whole and torn (the same shapes in torn paper), and the rope it hangs by.

Called from agriculture_data.py. The advancements' and recipes' data come through HALLOWEEN_ADVANCEMENTS, SHAPED and
SHAPELESS in tools/agriculture.py. Shapes are in model pixels (16 to a block), their origin the rope's top (where it is
tied under the block it hangs from), hanging down; the front faces north (low z).
  - The pumpkin: a ribbed body of orange crepe-paper fringe, a green paper stem and leaf, and a black-paper face.
  - The star: a ball with seven cones of fringe (six round its face and one out of the front) in pink, yellow, turquoise
    and orange, tassels streaming from its lower points.
  - The bat: a black fringe body and head with pointed ears and a face, and scalloped wings spread either side.
"""
import json
import math

from decor_data import MOD, rid, box, flat_item
from decor6_data import quads
from pinata import PINATA, KINDS, STAR_COLOURS

FULL = (0, 0, 16, 16)
ALL = ("north", "south", "east", "west", "up", "down")
SHEET = ("north", "south")
DROP = PINATA["drop"] * 16          # from the rope's top to the body's foot
TOP = DROP - PINATA["height"] * 16  # from the rope's top to the body's top
CUTOUT_PREFIXES = ("pinata_face_", "pinata_tassel", "pinata_edge_", "pinata_wing")


def full(e):
    for side in e["faces"]:
        e["faces"][side]["uv"] = list(FULL)
    return e


def b(lo, hi, texture, faces=ALL, rotation=None, textures=None):
    return full(box(lo, hi, texture, faces=faces, rotation=rotation, textures=textures))


def turned(e, rotations):
    e["rotations"] = rotations
    return e


def rope(length):
    """The rope from the knot under the block down to the body's top, and the knot."""
    return [b((-0.5, -length, -0.5), (0.5, 0, 0.5), "#rope"), b((-1, -1.5, -1), (1, 0, 1), "#rope")]


def skirt(y, half_x, half_z, texture):
    """A fringe of paper strips hanging round a body's foot (cut out between the strips)."""
    return [b((-half_x, y - 3, -half_z), (half_x, y, -half_z), texture, faces=SHEET),
            b((-half_x, y - 3, half_z), (half_x, y, half_z), texture, faces=SHEET),
            b((-half_x, y - 3, -half_z), (-half_x, y, half_z), texture, faces=("east", "west")),
            b((half_x, y - 3, -half_z), (half_x, y, half_z), texture, faces=("east", "west"))]


def pumpkin():
    """The pumpkin piñata: a ribbed body of orange fringe (a box and two crossed ribs), its top, a stem and leaf, the
    jack-o'-lantern face on its front and a fringe skirt."""
    bottom, top = -DROP, -TOP
    mid = (bottom + top) / 2
    e = [b((-6, bottom + 1, -6), (6, top - 1, 6), "#fringe"),
         b((-7.5, bottom + 2, -4), (7.5, top - 2, 4), "#fringe"),
         b((-4, bottom + 2, -7.5), (4, top - 2, 7.5), "#fringe"),
         # Diagonal ribs, to round it.
         turned(b((-7, bottom + 2.5, -3), (7, top - 2.5, 3), "#fringe"), [{"origin": [0, 0, 0], "axis": "y", "angle": 45}]),
         turned(b((-7, bottom + 2.5, -3), (7, top - 2.5, 3), "#fringe"), [{"origin": [0, 0, 0], "axis": "y", "angle": -45}]),
         b((-4.5, top - 1, -4.5), (4.5, top, 4.5), "#fringe"),
         b((-4.5, bottom, -4.5), (4.5, bottom + 1, 4.5), "#fringe"),
         b((-1, top, -1), (1, top + 2.5, 1), "#stem"),
         turned(b((1, top + 0.5, -0.5), (5, top + 1.25, 2.5), "#stem"), [{"origin": [1, top + 0.5, 1], "axis": "z", "angle": 22.5}]),
         b((-5, mid - 3.5, -7.6), (5, mid + 3.5, -7.6), "#face", faces=("north",))]
    e += skirt(bottom + 2, 7.5, 4, "#edge")
    return e


def cone(colour_key, length=9.0, base=5.0):
    """A cone of fringe out along +y from the star's middle: three steps narrowing to a point."""
    out = []
    steps = ((base, 3.5), (base * 0.66, 3.0), (base * 0.33, 2.5))
    y = 4.0
    for width, step in steps:
        h = width / 2
        out.append(b((-h, y, -h), (h, y + step, h), colour_key))
        y += step
    return out, y


def star():
    """The star piñata: a ball of fringe with six cones round its face and one out of the front, tassels streaming from
    the tips of its three lower cones."""
    centre = -(TOP + 12.0)
    e = [b((-4, centre - 4, -4), (4, centre + 4, 4), "#ball"),
         turned(b((-4.5, centre - 3, -3), (4.5, centre + 3, 3), "#ball"), [{"origin": [0, centre, 0], "axis": "z", "angle": 45}]),
         turned(b((-4.5, centre - 3, -3), (4.5, centre + 3, 3), "#ball"), [{"origin": [0, centre, 0], "axis": "z", "angle": -45}])]
    angles = (90, 30, -30, -90, -150, 150)
    for i, angle in enumerate(angles):
        parts, tip = cone(f"#c{i}")
        rot = [{"origin": [0, 0, 0], "axis": "z", "angle": angle - 90}]
        for part in parts:
            part["from"][1] += centre
            part["to"][1] += centre
            rot_here = [{"origin": [0, centre, 0], "axis": "z", "angle": angle - 90}]
            e.append(turned(part, rot_here))
        if angle < 0:
            # Tassels from the tip of a lower cone: two streamers hanging straight down.
            rad = math.radians(angle)
            tx, ty = tip * math.cos(rad), centre + tip * math.sin(rad)
            for dz in (-0.6, 0.6):
                e.append(b((tx - 1.5, ty - 9, dz), (tx + 1.5, ty, dz), "#tassel", faces=SHEET))
    # The front cone, out towards the viewer (north).
    parts, _ = cone("#c6")
    for part in parts:
        part["from"][1] += centre
        part["to"][1] += centre
        e.append(turned(part, [{"origin": [0, centre, 0], "axis": "x", "angle": -90}]))
    return e


def bat():
    """The bat piñata: a black fringe body and head, pointed ears, a face, and scalloped wings spread either side, swept
    a little back."""
    bottom, top = -DROP, -TOP
    e = [b((-4, bottom + 1, -3), (4, bottom + 9, 3), "#fringe"),
         b((-3, bottom, -2.5), (3, bottom + 1, 2.5), "#fringe"),
         b((-3.5, bottom + 9, -3.5), (3.5, top, 3), "#fringe", textures={"north": "#face"}),
         b((-3.5, top, -1), (-1.5, top + 3, 0.5), "#fringe"), b((1.5, top, -1), (3.5, top + 3, 0.5), "#fringe")]
    for side in (-1, 1):
        x0, x1 = (4, 16) if side > 0 else (-16, -4)
        wing = b((x0, bottom + 2, 0), (x1, top - 1, 0), "#wing", faces=SHEET)
        e.append(turned(wing, [{"origin": [4 * side, 0, 0], "axis": "y", "angle": -20 * side}]))
    e += skirt(bottom + 1.5, 4, 3, "#edge")
    return e


def textures(kind, torn):
    t = "_torn" if torn else ""
    if kind == "pumpkin":
        return {"fringe": f"pinata_fringe_orange{t}", "stem": "pinata_fringe_green", "face": "pinata_face_pumpkin",
                "edge": "pinata_edge_orange", "rope": "pinata_rope"}
    if kind == "star":
        out = {"ball": f"pinata_fringe_pink{t}", "tassel": "pinata_tassel", "rope": "pinata_rope"}
        out.update({f"c{i}": f"pinata_fringe_{colour}{t}" for i, colour in enumerate(STAR_COLOURS)})
        return out
    return {"fringe": f"pinata_fringe_black{t}", "face": "pinata_face_bat", "wing": f"pinata_wing{t}", "edge": "pinata_edge_purple",
            "rope": "pinata_rope"}


SHAPES = {"pumpkin": pumpkin, "star": star, "bat": bat}


def drawn(elements, texture_map):
    out = quads(elements, texture_map)
    for quad in out:
        if quad["texture"].startswith(CUTOUT_PREFIXES):
            quad["cutout"] = True
    return out


def pinata_quads():
    out = {}
    for kind in KINDS:
        for torn in (False, True):
            out[f"pinata_{kind}" + ("_torn" if torn else "")] = drawn(SHAPES[kind](), textures(kind, torn))
    # The rope: from the knot down to each kind's top.
    out["pinata_rope"] = drawn(rope(TOP), {"rope": "pinata_rope"})
    out["pinata_rope_star"] = drawn(rope(TOP - 1.0), {"rope": "pinata_rope"})
    return out


def assets(root, write, lang):
    for kind, spec in KINDS.items():
        flat_item(root, write, spec["item"])
    flat_item(root, write, PINATA["blindfold"])
    write(root / "models" / "item" / f"{PINATA['stick']}.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{PINATA['stick']}")}})
    write(root / "items" / f"{PINATA['stick']}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{PINATA['stick']}")}})
    write(root / "equipment" / "blindfold.json", {"layers": {"humanoid": [{"texture": rid("blindfold")}]}})
    path = root / "pinata_quads.json"
    path.write_text("{\n" + ",\n".join(json.dumps(name) + ": [\n" + ",\n".join(json.dumps(q, separators=(",", ":")) for q in qs) + "\n]"
                                         for name, qs in pinata_quads().items()) + "\n}\n", encoding="utf-8")
    for kind, spec in KINDS.items():
        lang[f"item.{MOD}.{spec['item']}"] = spec["display"]
    lang[f"item.{MOD}.{PINATA['stick']}"] = PINATA["stick_display"]
    lang[f"item.{MOD}.{PINATA['blindfold']}"] = PINATA["blindfold_display"]
    lang[f"entity.{MOD}.{PINATA['entity']}"] = PINATA["entity_display"]
    lang[f"item.{MOD}.pinata.tooltip"] = "Hang it from the underside of a block; fill it with anything"
    lang[f"message.{MOD}.pinata.full"] = "The piñata is full"
    lang[f"message.{MOD}.pinata.no_room"] = "A piñata needs two clear blocks below what it hangs from"
    lang[f"message.{MOD}.pinata.filled"] = "Into the piñata: %s"


def tags(tags):
    pass
