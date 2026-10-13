"""Blockbench bridge for the 3D armor toolkit (tools/armor_models.py): a set as a Blockbench project and back.

The owner models armor in Blockbench (https://www.blockbench.net, 5.x). This module writes any ArmorSet as a .bbmodel
project to open, look at on a player and edit there, and reads a .bbmodel back into worn_models.json quads and an
atlas, so a model made or touched up in Blockbench reaches the game texel for texel.

The project is Blockbench's Generic Model format ("free": cubes turned on any axis by any angle, a uv rectangle per
face, meshes too), format version 5.0, with the set's atlas embedded as its texture and an outliner of the player's
bones:
    head, body, right_arm, left_arm, right_leg, left_leg      a group per bone, its pivot the player's
        <item>_<bone>                                         a group per worn piece on the bone (the piece's
                                                              worn_models.json key), holding its cubes
    player                                                    the player's own boxes in grey, locked and not exported:
                                                              a guide to model round, hidden with its eye icon
Blockbench's space is the toolkit's turned half about z and raised 24 pixels: x points to the model's RIGHT, y UP from
the feet and z to the BACK (the face looks north, -z). A toolkit point (x, y, z) in body space is Blockbench's
(-x, 24 - y, z), so turns about x and y change sign and turns about z keep theirs. Both turn a box about its pivot x
first, then y, then z (Blockbench's Euler order ZYX: R = Rz Ry Rx).

Faces: Blockbench's east, west, up, down, south and north are the toolkit's right, left, top, bottom, back and front.
A face's uv is [u1, v1, u2, v2] in texels of the texture's uv size, and Blockbench puts (u1, v1), (u2, v1), (u1, v2)
and (u2, v2) on the face's corners in the order _BB_CORNERS lists them (three_custom.js setShape), each step of the
face's rotation moving them round one corner (cube.js updateUV). Vanilla's box layout, which the toolkit draws, is
that rectangle with no rotation on every face, so an exported face is its texel rectangle (u1 > u2 where a mirrored
part shows its texels flipped). A face whose texture is null is not drawn: a skipped face.

Reading: the cubes and meshes under each bone's group become that bone's quads, grouped into pieces by the name of the
group they sit in (an item id such as sentinel_helmet, or the worn_models.json key it starts, sentinel_helmet_head); a
cube straight in a bone's group belongs to the piece given for that bone (by default head: helmet, body and arms:
chestplate, legs: leggings). Groups and elements with export turned off (the player guide) are left out, as are bones'
own turns: a bone's group must stand at rest. A cube with see-through texels on any drawn face is drawn cutout.

    python3 tools/bbmodel.py export [--set NAME ...] [--out DIR]   write <DIR>/<set>.bbmodel (default art/armor)
    python3 tools/bbmodel.py check [--set NAME ...]                export each set, read it back and compare the
                                                                   quads and the atlas with the toolkit's own
    python3 tools/bbmodel.py quads FILE.bbmodel                    what a project holds, piece by piece
"""
import argparse
import base64
import io
import json
import math
import sys
import tempfile
import uuid
from pathlib import Path

import numpy as np
from PIL import Image

import armor_models as am
from dataclasses import replace

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/armor"
FORMAT_VERSION = "5.0"
MODEL_FORMAT = "free"
NAMESPACE = uuid.UUID("6d0c2a8e-3f4b-5c1d-9e7a-2b8f4c6d1e3a")   # every exported uuid is uuid5 of this and its path
BONE_COLOURS = {"head": 0, "body": 1, "right_arm": 2, "left_arm": 3, "right_leg": 4, "left_leg": 5}
GUIDE = "player"
# The piece a cube straight in a bone's group belongs to, by the item id's last word.
DEFAULT_KIND = {"head": "helmet", "body": "chestplate", "right_arm": "chestplate", "left_arm": "chestplate",
                "right_leg": "leggings", "left_leg": "leggings"}
# Bone names as other projects spell them (Blockbench's player presets, Bedrock geometry, the owner's projects).
BONE_ALIASES = {"head": "head", "body": "body", "rightarm": "right_arm", "leftarm": "left_arm",
                "rightleg": "right_leg", "leftleg": "left_leg"}

BB_FACE = {"top": "up", "bottom": "down", "right": "east", "front": "north", "left": "west", "back": "south"}
# Each Blockbench face's corners as (x, y, z) picks, 0 = from and 1 = to, in the order its uv corners go on them.
_BB_CORNERS = {"east": ((1, 1, 1), (1, 1, 0), (1, 0, 1), (1, 0, 0)),
               "west": ((0, 1, 0), (0, 1, 1), (0, 0, 0), (0, 0, 1)),
               "up": ((0, 1, 0), (1, 1, 0), (0, 1, 1), (1, 1, 1)),
               "down": ((0, 0, 1), (1, 0, 1), (0, 0, 0), (1, 0, 0)),
               "south": ((0, 1, 1), (1, 1, 1), (0, 0, 1), (1, 0, 1)),
               "north": ((1, 1, 0), (0, 1, 0), (1, 0, 0), (0, 0, 0))}
_BB_NORMALS = {"east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0), "south": (0, 0, 1),
               "north": (0, 0, -1)}
# The toolkit's box corners 1-8 (armor_models._CORNERS) as picks of (x0 | x1, y0 | y1, z0 | z1).
_TK_PICKS = {1: (0, 0, 0), 2: (1, 0, 0), 3: (1, 1, 0), 4: (0, 1, 0), 5: (0, 0, 1), 6: (1, 0, 1), 7: (1, 1, 1),
             8: (0, 1, 1)}


# ---------------------------------------------------------------- space
def to_bb(p):
    """A toolkit body-space point in Blockbench's space."""
    return (-p[0], 24.0 - p[1], p[2])


def from_bb(b):
    """A Blockbench point in the toolkit's body space."""
    return (-b[0], 24.0 - b[1], b[2])


def bb_pivot(bone):
    return to_bb(am.PIVOTS[bone])


def _flip(m):
    """A turn in the other space: F m F with F the half turn about z (diag(-1, -1, 1))."""
    s = (-1, -1, 1)
    return tuple(tuple(m[i][j] * s[i] * s[j] for j in range(3)) for i in range(3))


def euler_zyx(m):
    """(x, y, z) degrees with m = Rz Ry Rx (Blockbench's and the toolkit's order), rounded where exact."""
    sy = max(-1.0, min(1.0, -m[2][0]))
    y = math.asin(sy)
    if abs(math.cos(y)) > 1e-9:
        x = math.atan2(m[2][1], m[2][2])
        z = math.atan2(m[1][0], m[0][0])
    elif sy > 0:
        x, z = math.atan2(m[0][1], m[1][1]), 0.0
    else:
        x, z = math.atan2(-m[0][1], m[1][1]), 0.0
    out = []
    for a in (x, y, z):
        d = math.degrees(a)
        r = round(d, 6)
        out.append(float(round(r)) if abs(r - round(r)) < 1e-6 else r)
    back = am.euler_matrix(out)
    if max(abs(back[i][j] - m[i][j]) for i in range(3) for j in range(3)) > 1e-6:
        raise ValueError(f"euler_zyx: {out} does not rebuild the turn")
    return tuple(o + 0.0 for o in out)


def _bb_matrix(rotation):
    return am.euler_matrix(tuple(float(a) for a in rotation))


# ---------------------------------------------------------------- export
def _uuid(*path):
    return str(uuid.uuid5(NAMESPACE, "/".join(path)))


def _num(v):
    """A plain number for the file: integers stay integers, the rest rounded to 6 places."""
    r = round(float(v), 6) + 0.0
    return int(r) if r == int(r) else r


def _vec(v):
    return [_num(c) for c in v]


def _data_url(image):
    buf = io.BytesIO()
    image.save(buf, format="PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")


def _texture(name, image, index, key):
    w, h = image.size
    return {"path": "", "name": f"{name}.png", "folder": "", "namespace": "", "id": str(index), "group": "",
            "width": w, "height": h, "uv_width": w, "uv_height": h, "particle": False, "use_as_default": False,
            "layers_enabled": False, "sync_to_project": "", "render_mode": "default", "render_sides": "auto",
            "pbr_channel": "color", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
            "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": _uuid(key, "texture"),
            "source": _data_url(image)}


def _face_uv(uvs):
    """([u1, v1, u2, v2], rotation) putting uvs[k] on _BB_CORNERS' corner k, or None."""
    for rotation in (0, 90, 180, 270):
        order = ["A", "B", "C", "D"]
        for _ in range(rotation // 90):
            order[0], order[1], order[2], order[3] = order[2], order[0], order[3], order[1]
        at = {sym: uvs[k] for k, sym in enumerate(order)}
        (a_u, a_v), (b_u, b_v), (c_u, c_v), (d_u, d_v) = at["A"], at["B"], at["C"], at["D"]
        if a_v == b_v and c_v == d_v and a_u == c_u and b_u == d_u:
            return [a_u, a_v, d_u, d_v], rotation
    return None


def part_cube(part, bone, net, texture_index, key, density=1, colour=0):
    """A toolkit part as a Blockbench cube element: placed where the toolkit places it, each face showing the texels
    the toolkit's quad shows (net: the part's atlas region (u, v, ...), from armor_models.layout)."""
    pivot = am.PIVOTS[bone]
    lo, hi = part.origin, part.to
    if part.turns:
        r, t = am.transform(part)
        centre = am._add(am._apply(r, part.centre), t)
        origin = to_bb(am._add(centre, pivot))
        half = am._scale(part.size, 0.5)
        frm, to = am._sub(origin, half), am._add(origin, half)
        rotation = euler_zyx(_flip(r))
    else:
        a, b = to_bb(am._add(lo, pivot)), to_bb(am._add(hi, pivot))
        frm, to = tuple(min(x, y) for x, y in zip(a, b)), tuple(max(x, y) for x, y in zip(a, b))
        origin = to_bb(am._add(part.pivot, pivot))
        rx, ry, rz = part.rotation
        rotation = (-rx + 0.0, -ry + 0.0, rz + 0.0)
    w, h, d = am.net_size(part, density)
    rects = am.face_rects(w, h, d)
    own = dict(part.uvs)
    faces = {name: {"uv": [0, 0, 0, 0], "texture": None} for name in _BB_CORNERS}
    for face in am.FACES:
        if face in part.skip or (part.uvs and face not in own):
            continue
        u1, v1, u2, v2 = rects[face]
        if face == "bottom":
            v1, v2 = v2, v1
        picks = [_TK_PICKS[i] for i in am._CORNERS[face]]
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        if part.uvs:          # texels of its own, in atlas texels: no net to offset them by
            uvs = [tuple(uv) for uv in own[face]]
            net = (0, 0)
        if part.mirror:
            picks = [(1 - x, y, z) for x, y, z in picks]
        bb_picks = [(1 - x, 1 - y, z) for x, y, z in picks]
        axis = [i for i in range(3) if len({p[i] for p in bb_picks}) == 1][0]
        side = bb_picks[0][axis]
        name = {(0, 1): "east", (0, 0): "west", (1, 1): "up", (1, 0): "down", (2, 1): "south", (2, 0): "north"}[
            (axis, side)]
        at = {p: (net[0] + u, net[1] + v) for p, (u, v) in zip(bb_picks, uvs)}
        solved = _face_uv([at[c] for c in _BB_CORNERS[name]])
        if solved is None:
            raise ValueError(f"{part.name}: the {face} face's texels are not a rectangle Blockbench can show")
        uv, rot = solved
        entry = {"uv": _vec(uv), "texture": texture_index}
        if rot:
            entry["rotation"] = rot
        faces[name] = entry
    element = {"name": part.name, "box_uv": False, "rescale": False, "locked": False, "light_emission": 0,
               "render_order": "default", "allow_mirror_modeling": True, "from": _vec(frm), "to": _vec(to),
               "autouv": 0, "color": colour, "origin": _vec(origin)}
    if any(rotation):
        element["rotation"] = _vec(rotation)
    if part.inflate:
        element["inflate"] = _num(part.inflate)
    if part.mirror:
        element["mirror_uv"] = True
    element.update({"faces": faces, "type": "cube", "uuid": _uuid(key, "cube", part.name)})
    return element


def _group(name, origin, key, colour, export=True, locked=False):
    return {"name": name, "origin": _vec(origin), "color": colour, "uuid": _uuid(key, "group", name),
            "export": export, "mirror_uv": False, "isOpen": False, "locked": locked, "visibility": True, "autouv": 0}


def _guide():
    """(ArmorSet, atlas): the grey player the preview renders on (tools/armor_preview.py)."""
    import armor_preview
    import armor_paint
    parts = {bone: [am.around(f"player_{bone}", bone, 0, paint=armor_preview._skin_paint(bone))] for bone in am.BONES}
    guide = am.ArmorSet("player", armor_preview.MANNEQUIN, {"player": parts}, width=64, texture="player")
    return guide, armor_paint.paint_atlas(guide)


def export(s, atlas=None, guide=True):
    """The set as a .bbmodel project (a dict to write as JSON). atlas: its texture (default: what armor_paint paints).
    guide: add the grey player, locked and not exported."""
    import armor_paint
    atlas = atlas if atlas is not None else armor_paint.paint_atlas(s)
    nets, size = am.layout(s)
    if atlas.size != size:
        raise ValueError(f"{s.name}: atlas is {atlas.size}, the layout needs {size}")
    key = s.name
    elements, groups, outliner = [], [], []
    for bone in am.BONES:
        pieces = [(item, bones[bone]) for item, bones in s.pieces.items() if bones.get(bone)]
        if not pieces:
            continue
        bone_group = _group(bone, bb_pivot(bone), key, BONE_COLOURS[bone])
        groups.append(bone_group)
        children = []
        for item, parts in pieces:
            piece = _group(f"{item}_{bone}", bb_pivot(bone), key, BONE_COLOURS[bone])
            groups.append(piece)
            cubes = [part_cube(p, bone, nets.get(p.key, (0, 0)), 0, key, s.density, BONE_COLOURS[bone]) for p in parts]
            elements.extend(cubes)
            children.append({"uuid": piece["uuid"], "isOpen": False, "children": [c["uuid"] for c in cubes]})
        outliner.append({"uuid": bone_group["uuid"], "isOpen": True, "children": children})
    textures = [_texture(s.name, atlas, 0, key)]
    if guide:
        player, player_atlas = _guide()
        player_nets, _ = am.layout(player)
        root = _group(GUIDE, (0, 0, 0), key, 7, export=False, locked=True)
        groups.append(root)
        cubes = []
        for _, bone, part in player.parts():
            cube = part_cube(part, bone, player_nets[part.key], 1, key, 1, 7)
            cube.update({"locked": True, "export": False})
            cubes.append(cube)
        elements.extend(cubes)
        outliner.append({"uuid": root["uuid"], "isOpen": False, "children": [c["uuid"] for c in cubes]})
        textures.append(_texture("player", player_atlas, 1, key + "/player"))
    return {"meta": {"format_version": FORMAT_VERSION, "model_format": MODEL_FORMAT, "box_uv": False},
            "name": s.name, "model_identifier": "", "visible_box": [1, 1, 0], "variable_placeholders": "",
            "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
            "resolution": {"width": size[0], "height": size[1]},
            "elements": elements, "groups": groups, "outliner": outliner, "textures": textures}


def dumps(model):
    """The project as Blockbench writes it: tab-indented JSON."""
    return json.dumps(model, indent="\t", ensure_ascii=False) + "\n"


def write(s, path=None, atlas=None, guide=True):
    path = Path(path) if path else ART / f"{s.name}.bbmodel"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(dumps(export(s, atlas, guide)), encoding="utf-8")
    return path


# ---------------------------------------------------------------- import
def read(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def texture_images(model, base=None):
    """The project's textures as RGBA images, in order: embedded ones decoded, else read from their relative path."""
    out = []
    for tex in model.get("textures", []):
        source = tex.get("source") or ""
        if source.startswith("data:"):
            data = base64.b64decode(source.split(",", 1)[1])
            out.append(Image.open(io.BytesIO(data)).convert("RGBA"))
        elif base and tex.get("relative_path") and (Path(base) / tex["relative_path"]).exists():
            out.append(Image.open(Path(base) / tex["relative_path"]).convert("RGBA"))
        else:
            raise ValueError(f"texture {tex.get('name')!r} is neither embedded nor next to the project")
    return out


def bone_name(name):
    """The toolkit bone a group's name stands for, or None."""
    return BONE_ALIASES.get(name.lower().replace("_", "").replace(" ", "").replace("-", ""))


def _nodes(model):
    """(element, [ancestor groups, outermost first]) for every element, from the outliner."""
    elements = {e["uuid"]: e for e in model.get("elements", [])}
    groups = {g["uuid"]: g for g in model.get("groups", [])}
    out = []

    def walk(items, chain):
        for item in items:
            if isinstance(item, str):
                if item in elements:
                    out.append((elements[item], chain))
                continue
            group = groups.get(item.get("uuid")) or item   # format 4.x keeps the group in the outliner itself
            walk(item.get("children", []), chain + [group])
    walk(model.get("outliner", []), [])
    return out


def _place(group_chain, element):
    """(matrix R, function p -> world point) of an element at rest: its own turn about its origin, then each group's
    about the group's origin, innermost first. A bone's own group must not be turned."""
    steps = [(element.get("origin", (0, 0, 0)), element.get("rotation", (0, 0, 0)))]
    for group in reversed(group_chain):
        rotation = group.get("rotation", (0, 0, 0))
        if bone_name(group.get("name", "")) and any(float(a) for a in rotation):
            raise ValueError(f"bone group {group['name']!r} is turned {rotation}: set it back to rest (0, 0, 0)")
        steps.append((group.get("origin", (0, 0, 0)), rotation))
    total = ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0))
    for _, rotation in steps:
        total = am._mul(_bb_matrix(rotation), total)

    def place(p):
        for origin, rotation in steps:
            if any(float(a) for a in rotation):
                origin = tuple(float(c) for c in origin)
                p = am._add(am._apply(_bb_matrix(rotation), am._sub(p, origin)), origin)
        return p
    return total, place


def _uv_rect_texels(uvs, scale, size):
    us, vs = [u * scale[0] for u, _ in uvs], [v * scale[1] for _, v in uvs]
    x0, x1 = max(0, math.floor(min(us) + 1e-6)), min(size[0], math.ceil(max(us) - 1e-6))
    y0, y1 = max(0, math.floor(min(vs) + 1e-6)), min(size[1], math.ceil(max(vs) - 1e-6))
    return x0, y0, x1, y1


def _see_through(alpha, uvs, scale):
    x0, y0, x1, y1 = _uv_rect_texels(uvs, scale, (alpha.shape[1], alpha.shape[0]))
    return x1 > x0 and y1 > y0 and bool((alpha[y0:y1, x0:x1] < 255).any())


def _bb_face_uvs(face):
    """The uv on each of a cube face's corners (in _BB_CORNERS order), its rotation applied as Blockbench does."""
    u1, v1, u2, v2 = (float(c) for c in face["uv"])
    arr = [(u1, v1), (u2, v1), (u1, v2), (u2, v2)]
    rot = int(face.get("rotation") or 0)
    while rot > 0:
        arr[0], arr[1], arr[2], arr[3] = arr[2], arr[0], arr[3], arr[1]
        rot -= 90
    return arr


def _quad(points, uvs, normal, texture, size):
    """A worn_models.json quad from body-space-at-rest points of one bone (already in its bone space)."""
    r = am._r
    return {"texture": texture, "normal": [r(-normal[0], 4), r(-normal[1], 4), r(normal[2], 4)],
            "vertices": [[r(-x, 4), r(-y, 4), r(z, 4), r(u / size[0], 5), r(v / size[1], 5)]
                         for (x, y, z), (u, v) in zip(points, uvs)]}


def cube_faces(element, chain, bone):
    """[(toolkit face name, 4 bone-space points, 4 uvs, bone-space normal)] for a cube, in the toolkit's face and
    corner order (a cube marked mirror_uv in the toolkit's mirrored order), only its drawn faces."""
    frm = [float(c) for c in element["from"]]
    to = [float(c) for c in element["to"]]
    g = float(element.get("inflate", 0) or 0)
    stretch = [float(c) for c in element.get("stretch", (1, 1, 1))]
    centre = [(a + b) / 2 for a, b in zip(frm, to)]
    half = [(b - a) / 2 for a, b in zip(frm, to)]
    lo = [centre[i] - (half[i] + g) * stretch[i] for i in range(3)]
    hi = [centre[i] + (half[i] + g) * stretch[i] for i in range(3)]
    matrix, place = _place(chain, element)
    pivot = am.PIVOTS[bone]
    mirror = bool(element.get("mirror_uv")) and not element.get("box_uv")
    out = []
    for face in am.FACES:
        picks = [_TK_PICKS[i] for i in am._CORNERS[face]]
        if mirror:
            picks = [(1 - x, y, z) for x, y, z in picks][::-1]
        bb_picks = [(1 - x, 1 - y, z) for x, y, z in picks]
        axis = [i for i in range(3) if len({p[i] for p in bb_picks}) == 1][0]
        name = {(0, 1): "east", (0, 0): "west", (1, 1): "up", (1, 0): "down", (2, 1): "south", (2, 0): "north"}[
            (axis, bb_picks[0][axis])]
        data = element["faces"].get(name)
        if not data or data.get("texture") is None:
            continue
        corner_uv = dict(zip(_BB_CORNERS[name], _bb_face_uvs(data)))
        points = []
        for p in bb_picks:
            local = tuple(hi[i] if p[i] else lo[i] for i in range(3))
            body = from_bb(place(local))
            points.append(am._sub(body, pivot))
        n = am._apply(matrix, _BB_NORMALS[name])
        out.append((face, points, [corner_uv[p] for p in bb_picks], (-n[0], -n[1], n[2]), data))
    return out


def mesh_faces(element, chain, bone):
    """[(4 bone-space points, 4 uvs, bone-space normal, face data)] for a mesh's faces of 3 or 4 corners, each in
    Blockbench's own corner order (MeshFace.getSortedVertices); a triangle repeats its last corner."""
    matrix, place = _place(chain, element)
    origin = [float(c) for c in element.get("origin", (0, 0, 0))]
    pivot = am.PIVOTS[bone]
    vertices = element["vertices"]
    out = []
    for key, face in element.get("faces", {}).items():
        keys = list(face["vertices"])
        if face.get("texture") is None:
            continue
        if len(keys) not in (3, 4):
            raise ValueError(f"mesh {element.get('name')!r}: face {key} has {len(keys)} corners (use 3 or 4)")
        local = {k: am._add([float(c) for c in vertices[k]], origin) for k in keys}
        if len(keys) == 4:
            keys = _sorted_quad(keys, local)
        pts = [place(local[k]) for k in keys]
        a, b = am._sub(pts[1], pts[0]), am._sub(pts[2], pts[0])
        n = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
        length = math.sqrt(am._dot(n, n)) or 1.0
        n = am._scale(n, 1 / length)
        uvs = [tuple(float(c) for c in face["uv"][k]) for k in keys]
        points = [am._sub(from_bb(p), pivot) for p in pts]
        if len(points) == 3:
            points.append(points[2])
            uvs.append(uvs[2])
        out.append((points, uvs, (-n[0], -n[1], n[2]), face))
    return out


def _sorted_quad(keys, at):
    """Blockbench's MeshFace.getSortedVertices: the four corners in an order that goes round the face."""
    def test(base1, base2, top, check):
        line = am._sub(base2, base1)
        ll = am._dot(line, line) or 1.0
        foot = am._add(base1, am._scale(line, am._dot(am._sub(top, base1), line) / ll))
        normal = am._sub(foot, top)
        return am._dot(normal, am._sub(check, base2)) > 0
    v = [at[k] for k in keys]
    if test(v[1], v[2], v[0], v[3]):
        return [keys[2], keys[0], keys[1], keys[3]]
    if test(v[0], v[1], v[2], v[3]):
        return [keys[0], keys[2], keys[1], keys[3]]
    return keys


def _piece_kind(name, items):
    """The item a piece group's name stands for (the longest item id it starts with), or None."""
    low = name.lower()
    best = None
    for item in items:
        if low.startswith(item) and (best is None or len(item) > len(best)):
            best = item
    if best:
        return best
    for kind in am.PIECE_CAPS:
        if low == kind or low.startswith(kind + "_") or low.endswith("_" + kind):
            return kind
    return None


def entries(model, texture, items=None, default=None, texture_index=0, base=None):
    """({worn_models.json key: [quads]}, atlas image) from a project: every exported cube and mesh face on the given
    texture, under the bones' groups. texture: the name the quads use (e.g. "entity/equipment/3d/sentinel"). items:
    the set's item ids (piece groups are matched to them; default: the item ids the groups name). default: {bone:
    item} for cubes straight in a bone's group (default: the set's item ending in DEFAULT_KIND[bone])."""
    atlas = texture_images(model, base)[texture_index]
    tex_meta = model["textures"][texture_index]
    uv_size = (float(tex_meta.get("uv_width") or model["resolution"]["width"]),
               float(tex_meta.get("uv_height") or model["resolution"]["height"]))
    scale = (atlas.width / uv_size[0], atlas.height / uv_size[1])
    alpha = np.asarray(atlas)[:, :, 3]
    out = {}
    order = []
    for element, chain in _nodes(model):
        if element.get("export") is False or any(g.get("export") is False for g in chain):
            continue
        bones = [(i, bone_name(g.get("name", ""))) for i, g in enumerate(chain)]
        bones = [(i, b) for i, b in bones if b]
        if not bones:
            raise ValueError(f"{element.get('name')!r} is not under a bone's group (head, body, right_arm, ...)")
        index, bone = bones[-1]
        piece_groups = [g.get("name", "") for g in chain[index + 1:]]
        item = None
        for name in reversed(piece_groups):
            item = _piece_kind(name, items or [])
            if item:
                break
        if item in am.PIECE_CAPS and items:
            item = next((i for i in items if am.piece_kind(i) == item), None)
        if item is None:
            if default and bone in default:
                item = default[bone]
            elif items:
                item = next((i for i in items if am.piece_kind(i) == DEFAULT_KIND[bone]), None)
            if item is None:
                raise ValueError(f"{element.get('name')!r} on {bone}: no piece group names its item")
        key = f"{item}_{bone}"
        if key not in out:
            out[key] = []
            order.append(key)
        etype = element.get("type", "cube")
        if etype == "cube":
            faces = cube_faces(element, chain, bone)
            faces = [f for f in faces if int(f[4].get("texture")) == texture_index]
            cutout = any(_see_through(alpha, uvs, scale) for _, _, uvs, _, _ in faces)
            for _, points, uvs, normal, _ in faces:
                quad = _quad(points, uvs, normal, texture, uv_size)
                if cutout:
                    quad["cutout"] = True
                out[key].append(quad)
        elif etype == "mesh":
            faces = [f for f in mesh_faces(element, chain, bone) if int(f[3].get("texture")) == texture_index]
            for points, uvs, normal, _ in faces:
                quad = _quad(points, uvs, normal, texture, uv_size)
                if _see_through(alpha, uvs, scale):
                    quad["cutout"] = True
                out[key].append(quad)
        else:
            raise ValueError(f"{element.get('name')!r}: Blockbench {etype} elements are not supported (cubes, meshes)")
    return {k: out[k] for k in order}, atlas


def set_entries(s, path):
    """A registered set's worn_models.json entries and atlas read from its project, in the set's own entry order."""
    model = read(path)
    found, atlas = entries(model, s.texture, items=list(s.pieces), base=Path(path).parent)
    return in_set_order(s, found), atlas


def in_set_order(s, found):
    """{key: quads} in the order the set lists its pieces and their bones (armor_models.set_quads), then the rest."""
    found = dict(found)
    ordered = {}
    for item, bones in s.pieces.items():
        for bone in bones:
            key = f"{item}_{bone}"
            if key in found:
                ordered[key] = found.pop(key)
    ordered.update(found)
    return ordered


def _rigid(element, chain):
    """(R, c): the Blockbench placement of a cube at rest as p -> R p + c (its own turn about its origin, then its
    groups')."""
    matrix, place = _place(chain, element)
    c = place((0.0, 0.0, 0.0))
    return matrix, c


def cube_part(element, chain, bone):
    """A Blockbench cube as a toolkit Part on `bone` with its own texels (Part.uvs, atlas texels of the project's
    texture). Its box is the cube's, turned x and y about the half turn; any turns of the cube and its groups become
    one turn about the bone's origin with the box moved to suit, so the part lands exactly where Blockbench draws the
    cube."""
    if any(float(c) != 1.0 for c in element.get("stretch", (1, 1, 1))):
        raise ValueError(f"cube {element.get('name')!r} is stretched: apply the stretch to its size in Blockbench")
    frm = [float(c) for c in element["from"]]
    to = [float(c) for c in element["to"]]
    r_bb, c_bb = _rigid(element, chain)
    flip = (-1.0, -1.0, 1.0)
    r = _flip(r_bb)
    t = tuple(flip[i] * c_bb[i] + (0.0, 24.0, 0.0)[i] - am.PIVOTS[bone][i] for i in range(3))
    origin = (-to[0], -to[1], frm[2])
    size = tuple(b - a for a, b in zip(frm, to))
    shift = am._apply(tuple(zip(*r)), t)      # R^T t: the turn is about the bone's origin, so move the box by it
    uvs = []
    faces = {f: (pts, uv) for f, pts, uv, _, _ in cube_faces(dict(element, mirror_uv=False), chain, bone)}
    for face in am.FACES:
        if face in faces:
            uvs.append((face, tuple(tuple(float(c) for c in uv) for uv in faces[face][1])))
    rotation = euler_zyx(r) if any(abs(r[i][j] - (1.0 if i == j else 0.0)) > 1e-12 for i in range(3) for j in range(3)) \
        else (0.0, 0.0, 0.0)
    return am.Part(element.get("name") or element["uuid"], am._add(origin, shift), size, paint="solid",
                   rotation=rotation, inflate=float(element.get("inflate", 0) or 0), uvs=tuple(uvs))


def load_set(name, palette, path, items=None, texture=None, default=None):
    """A registered set read from its Blockbench project: an ArmorSet whose parts are the project's cubes (each with
    its own texels) and whose atlas is the project's texture. items: the set's item ids in the order its entries are
    listed (default: as the project's piece groups name them). Part names are the cubes' names, made unique."""
    model = read(path)
    texture = texture or f"{am.TEXTURE_DIR}/{name}"
    images = texture_images(model, Path(path).parent)
    atlas = images[0]
    meta = model["textures"][0]
    uv_size = (int(meta.get("uv_width") or model["resolution"]["width"]),
               int(meta.get("uv_height") or model["resolution"]["height"]))
    if atlas.size != uv_size:
        atlas = atlas.resize(uv_size, Image.NEAREST)
    alpha = np.asarray(atlas)[:, :, 3]
    pieces, seen = {}, {}
    for element, chain in _nodes(model):
        if element.get("export") is False or any(g.get("export") is False for g in chain):
            continue
        if element.get("type", "cube") != "cube":
            raise ValueError(f"{path}: {element.get('name')!r} is a {element.get('type')}; armor sets take cubes")
        bones = [(i, bone_name(g.get("name", ""))) for i, g in enumerate(chain)]
        bones = [(i, b) for i, b in bones if b]
        if not bones:
            raise ValueError(f"{path}: {element.get('name')!r} is not under a bone's group")
        index, bone = bones[-1]
        item = None
        for group in reversed(chain[index + 1:]):
            item = _piece_kind(group.get("name", ""), items or [])
            if item:
                break
        if item in am.PIECE_CAPS:
            item = next((i for i in (items or []) if am.piece_kind(i) == item), f"{name}_{item}")
        if item is None:
            item = (default or {}).get(bone) or next((i for i in (items or []) if am.piece_kind(i) == DEFAULT_KIND[bone]),
                                                     f"{name}_{DEFAULT_KIND[bone]}")
        part = cube_part(element, chain, bone)
        base = part.name.replace("/", "_") or "cube"
        seen[base] = seen.get(base, 0) + 1
        unique = base if seen[base] == 1 else f"{base}_{seen[base]}"
        cut = any(_see_through(alpha, uv, (1.0, 1.0)) for _, uv in part.uvs)
        part = replace(part, name=unique, cutout=cut)
        pieces.setdefault(item, {}).setdefault(bone, []).append(part)
    if items:
        pieces = {i: pieces[i] for i in items if i in pieces} | {i: b for i, b in pieces.items() if i not in items}
    return am.ArmorSet(name, palette, {i: {b: bs[b] for b in am.BONES if b in bs} for i, bs in pieces.items()},
                       texture=texture, image=atlas, atlas_size=uv_size)


# ---------------------------------------------------------------- checks
def _close(a, b, tol):
    return all(abs(x - y) <= tol for x, y in zip(a, b))


def _cyclic(quad):
    """The quad with its corners turned to start at the least one, so two lists of the same corners in the same
    winding compare equal wherever they start."""
    vs = quad["vertices"]
    start = min(range(len(vs)), key=lambda i: vs[i])
    return dict(quad, vertices=vs[start:] + vs[:start])


def compare(ours, theirs, tol=2e-4, cyclic=False):
    """Messages where two {key: [quads]} differ (positions and normals within tol, uvs within tol / 2). cyclic: the
    same corners in the same winding from another start are the same quad."""
    out = []
    if cyclic:   # and the faces of an entry in any order
        def norm(qs):
            return sorted((_cyclic(q) for q in qs), key=lambda q: json.dumps(q["vertices"]))
        ours = {k: norm(qs) for k, qs in ours.items()}
        theirs = {k: norm(qs) for k, qs in theirs.items()}
    for key in sorted(set(ours) | set(theirs)):
        a, b = ours.get(key, []), theirs.get(key, [])
        if len(a) != len(b):
            out.append(f"{key}: {len(a)} quads against {len(b)}")
            continue
        for i, (qa, qb) in enumerate(zip(a, b)):
            same = (qa["texture"] == qb["texture"] and bool(qa.get("cutout")) == bool(qb.get("cutout"))
                    and _close(qa["normal"], qb["normal"], tol)
                    and all(_close(va[:3], vb[:3], tol) and _close(va[3:], vb[3:], tol / 2)
                            for va, vb in zip(qa["vertices"], qb["vertices"])))
            if not same:
                out.append(f"{key} quad {i}: {json.dumps(qa)} against {json.dumps(qb)}")
                break
    return out


def roundtrip(s, tmp=None):
    """Exports the set, reads the project back and compares quads and atlas with the toolkit's own. Messages, empty
    when the project is exact."""
    import armor_paint
    atlas = armor_paint.paint_atlas(s)
    model = json.loads(dumps(export(s, atlas)))
    ours = json.loads(json.dumps(am.set_quads(s)))
    theirs, back = entries(model, s.texture, items=list(s.pieces))
    theirs = in_set_order(s, theirs)
    out = compare(ours, theirs)
    if list(ours) != list(theirs):
        out.append(f"entry order differs: {list(ours)} against {list(theirs)}")
    if back.tobytes() != atlas.convert("RGBA").tobytes():
        out.append("the embedded atlas differs from the painted one")
    exact = json.dumps(ours) == json.dumps(theirs)
    # and as a set of its own, read back through load_set: the same quads, part for part
    with tempfile.TemporaryDirectory(prefix="jugcraft-bbmodel-", dir=tmp) as scratch:
        path = Path(scratch) / f"{s.name}.bbmodel"
        path.write_text(dumps(model), encoding="utf-8")
        loaded = load_set(s.name, s.palette, path, items=list(s.pieces), texture=s.texture)
        again = json.loads(json.dumps(am.set_quads(loaded)))
        out += [f"load_set: {line}" for line in compare(ours, again, cyclic=True)]
        if armor_paint.paint_atlas(loaded).tobytes() != atlas.convert("RGBA").tobytes():
            out.append("load_set: the atlas differs")
    return out, exact


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    for name in ("export", "check"):
        p = sub.add_parser(name)
        p.add_argument("--set", action="append", help="a registered set's name (default: every set)")
        if name == "export":
            p.add_argument("--out", type=Path, default=ART)
            p.add_argument("--no-guide", action="store_true", help="leave out the grey player")
    p = sub.add_parser("quads")
    p.add_argument("file", type=Path)
    args = parser.parse_args()
    if args.command == "quads":
        model = read(args.file)
        found, atlas = entries(model, "texture", base=args.file.parent)
        print(f"{args.file.name}: texture {atlas.size[0]}x{atlas.size[1]}")
        for key, quads in found.items():
            print(f"  {key}: {len(quads)} quads")
        return 0
    chosen = [s for s in am.sets() if not args.set or s.name in args.set]
    if args.set and len(chosen) != len(args.set):
        parser.error(f"unknown set in {args.set}; registered: {[s.name for s in am.sets()]}")
    failed = False
    for s in chosen:
        if args.command == "export":
            path = write(s, args.out / f"{s.name}.bbmodel", guide=not args.no_guide)
            print(path.relative_to(ROOT) if path.is_relative_to(ROOT) else path)
        else:
            problems, exact = roundtrip(s)
            print(f"{s.name}: {'exact' if exact else 'same within rounding' if not problems else 'DIFFERS'}")
            for line in problems[:10]:
                failed = True
                print("  " + line)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
