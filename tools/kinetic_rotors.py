"""Exports the spinning parts of kinetic blocks (kinetic_models.ROTORS) for client/KineticRotorRenderer.

Each rotor becomes a list of textured quads in model pixels (north-facing, before the block state's
rotation), with element rotations already applied and UVs mapped the way vanilla block models map
them, so a spinning rotor looks exactly like its still block model. The file also carries each
block's block-state rotations, so the renderer turns the rotor the same way the block model is turned.
"""
import math
from pathlib import Path

import kinetic_models
from logistics_models import FACING_ROTATION
from model_writer import fit_uv, separate_boxes, unpack

TEXTURES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/jugcraft/textures/block"
AXIS_ROTATION = {"x": {"y": 90}, "y": {"x": 90}, "z": {}}
AXIS_INDEX = {"x": 0, "y": 1, "z": 2}

# Corners of each face, counter-clockwise seen from outside (the order the renderer's cull face expects),
# as (use max x?, use max y?, use max z?), and the face normal.
FACE_CORNERS = {
    "north": (((0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)), (0, 0, -1)),
    "south": (((1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1)), (0, 0, 1)),
    "west": (((0, 0, 1), (0, 1, 1), (0, 1, 0), (0, 0, 0)), (-1, 0, 0)),
    "east": (((1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)), (1, 0, 0)),
    "up": (((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)), (0, 1, 0)),
    "down": (((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)), (0, -1, 0)),
}


def vanilla_uv(face, x, y, z):
    """Vanilla's automatic UV (pixels) of a point on a face."""
    return {"north": (16 - x, 16 - y), "south": (x, 16 - y), "west": (z, 16 - y), "east": (16 - z, 16 - y),
            "up": (x, z), "down": (x, 16 - z)}[face]


def rotate(point, rotation):
    """Applies an element rotation (axis, degrees, origin) to a point or, with origin (0, 0, 0), a normal."""
    axis, angle, origin = rotation
    i = AXIS_INDEX[axis]
    a, b = [j for j in range(3) if j != i]
    # Rotated elements are symmetric shapes (see steampunk_models), so the direction does not matter.
    rad = math.radians(angle)
    p = [point[k] - origin[k] for k in range(3)]
    out = list(p)
    out[a] = p[a] * math.cos(rad) - p[b] * math.sin(rad)
    out[b] = p[a] * math.sin(rad) + p[b] * math.cos(rad)
    return [out[k] + origin[k] for k in range(3)]


def face_texture(texture, face):
    return texture.get(face, texture.get("*")) if isinstance(texture, dict) else texture


# A zero-thickness plane drawn from both sides (two opposite faces on one plane) has each face lifted this far (pixels)
# along its own normal: the cutout and translucent entity types do not cull, so twins on one plane would fight.
TWO_SIDED_LIFT = 0.05
OPPOSITE = {"north": "south", "south": "north", "west": "east", "east": "west", "up": "down", "down": "up"}


def turn_for_separation(point, rotation):
    """rotate() with an element's "rotation" option, for model_writer.separate_boxes."""
    return rotate(point, tuple(rotation[:3]))


def quads(elements, separate=True):
    """Textured quads of model elements (tuples, see model_writer). With separate (the default) differently drawn faces
    that share a plane are first pulled apart (model_writer.separate_boxes, whole boxes, never single quads), so a
    collar, band or cap never flickers against the body it sits on."""
    if separate:
        elements = separate_boxes(elements, turn=turn_for_separation)
    out = []
    for item in elements:
        frm, to, texture, options = unpack(item)
        rotation = options.get("rotation")
        pinned = options.get("uv", {})
        for face, (corners, normal) in FACE_CORNERS.items():
            name = face_texture(texture, face)
            if name is None:
                continue
            stretch = name.endswith("!")
            name = name.rstrip("!")
            if (TEXTURES / f"{name}.png.mcmeta").exists():
                raise ValueError(f"rotor texture {name} is animated; the renderer draws the whole image")
            points = [[to[k] if corner[k] else frm[k] for k in range(3)] for corner in corners]
            uvs = [vanilla_uv(face, *p) for p in points]
            us, vs = [u for u, _ in uvs], [v for _, v in uvs]
            if stretch:
                uvs = [((u - min(us)) / (max(us) - min(us)) * 16, (v - min(vs)) / (max(vs) - min(vs)) * 16) for u, v in uvs]
            else:
                # A pinned UV (kept by a separation push) or the automatic one moved inside the sprite by whole tiles.
                u0, v0, u1, v1 = pinned.get(face) or fit_uv([min(us), min(vs), max(us), max(vs)], clamp=False)
                uvs = [(u0 + (u - min(us)) / ((max(us) - min(us)) or 1) * (u1 - u0),
                        v0 + (v - min(vs)) / ((max(vs) - min(vs)) or 1) * (v1 - v0)) for u, v in uvs]
            axis = [abs(c) for c in normal].index(1)
            if abs(to[axis] - frm[axis]) < 1e-9 and face_texture(texture, OPPOSITE[face]) is not None:
                points = [[p[k] + normal[k] * TWO_SIDED_LIFT for k in range(3)] for p in points]
            n = list(normal)
            if rotation:
                points = [rotate(p, rotation) for p in points]
                n = rotate(n, (rotation[0], rotation[1], (0, 0, 0)))
            out.append({"texture": name, "normal": [round(v, 4) for v in n],
                        "vertices": [[round(p[0], 4), round(p[1], 4), round(p[2], 4), round(u / 16, 5), round(v / 16, 5)]
                                     for p, (u, v) in zip(points, uvs)]})
    return out


def export(kinetic_blocks):
    rotors = {}
    for block, rotor in kinetic_models.ROTORS.items():
        states = kinetic_blocks[block]["states"]
        if states == "axis":
            prop, table = "axis", AXIS_ROTATION
        elif states in ("facing", "horizontal"):
            prop = "facing"
            table = {f: r for f, r in FACING_ROTATION.items() if states == "facing" or f not in ("up", "down")}
        else:
            prop, table = None, {}
        rotors[block] = {
            "axis": rotor["axis"], "center": list(rotor["center"]), "property": rotor["property"],
            "speed": rotor["speed"], "variant_property": prop,
            "variants": {value: [r.get("x", 0), r.get("y", 0)] for value, r in table.items()},
            "quads": quads(rotor["elements"]),
        }
        if rotor.get("mode"):
            rotors[block]["mode"] = rotor["mode"]
    return rotors
