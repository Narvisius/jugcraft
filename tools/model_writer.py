"""Writes machine block models, blockstates and item models in one visual style.

Two styles exist. Both are visual only: they never change a block, its footprint or how it works.
- "steampunk" (tools/steampunk_models.py): detailed brass, copper and iron models for every machine.
- "classic" (tools/large_machines.py plus textured cubes): the original look.

generate_material_data.py writes DEFAULT_STYLE into the mod's own assets and the other style into
the built-in resource pack resourcepacks/alternate_machines, which players can turn on in
Options > Resource Packs. To make the other style the default, change DEFAULT_STYLE and regenerate.

Model elements are tuples (from, to, texture) or (from, to, texture, options):
- from/to are pixels in structure space (the master block is 0..16 on every axis, facing north).
- texture is a texture name, or a dict {face: name} with "*" as the default. A name ending in "!"
  stretches the whole 16x16 texture over that face (gauges, doors, windows); otherwise the texture
  is mapped by position, like vanilla's automatic UVs.
- options may hold "rotation": (axis, angle, origin) or (axis, angle, origin, rescale); rescale stretches a 45-degree
  element to span the block's diagonal (conveyor slopes). Rotated elements are never sliced between
  blocks, so keep each one inside a single block's reach.
"""
import copy
from collections import defaultdict
import json
import math
from pathlib import Path

MOD = "jugcraft"
DEFAULT_STYLE = "steampunk"
STYLES = ("steampunk", "classic")
PACK_ID = "alternate_machines"
STYLE_NAMES = {"steampunk": "Steampunk Machines", "classic": "Classic Machines"}

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ("north", "south", "east", "west", "up", "down")
# For each face: the two axes spanning it (u, v) and the axis it faces along with its side.
FACE_AXES = {"north": (0, 1, 2, 0), "south": (0, 1, 2, 1), "east": (2, 1, 0, 1), "west": (2, 1, 0, 0),
             "up": (0, 2, 1, 1), "down": (0, 2, 1, 0)}
# LargeMachineBlock.PART is 0..3.
PART_STATES = 64


def alternate_style():
    return next(style for style in STYLES if style != DEFAULT_STYLE)


def rid(path):
    return f"{MOD}:{path}"


def write(path, obj):
    if isinstance(obj, dict) and obj.get("elements"):
        finish_elements(obj["elements"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def finish_elements(elements, strip=True):
    """What every written block or item model gets (docs/ART_DIRECTION.md, Rules for everything): no two differently
    drawn faces share a plane, and no face reads outside its sprite. The list is given private copies of its elements
    first (generators share face dicts and coordinate lists between elements and models). With strip, the generators'
    private keys ("_keep_open") are dropped, so they never reach a model file."""
    elements[:] = [copy.deepcopy(e) for e in elements]
    fit_uvs(elements)
    separate_coplanar(elements)
    fit_uvs(elements)
    if strip:
        for e in elements:
            for key in [k for k in e if k.startswith("_")]:
                del e[key]


def finish_closed(elements, opaque=None):
    """finish_elements for models that must be closed (docs/ART_DIRECTION.md, Closed geometry): every left-out face of
    an unrotated opaque box that nothing covers is drawn (close_open_faces), before and after the separation pass, since
    a push can bare a strip of a face left out beside it. opaque(element) picks the boxes that count (a see-through box
    may leave faces out on purpose); by default every box does. An element's "_keep_open" lists faces it leaves out on
    purpose (a socket or a vessel's mouth looking into a hollow lined behind it); art_check's O1 allow-list names such
    models."""
    elements[:] = [copy.deepcopy(e) for e in elements]
    for _ in range(6):
        close_open_faces(elements, opaque)
        finish_elements(elements, strip=False)
        if not open_faces(elements, opaque):
            finish_elements(elements)
            return elements
    raise ValueError("finish_closed: closing and separating did not settle")


# The model axes a face's texture runs along: (across, down).
_FACE_UV_AXES = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}
# Where the closing pass probes just outside a left-out face: the centres of a 4 x 4 and of an 8 x 8 grid.
_PROBES = sorted({(k + 0.5) / n for n in (4, 8) for k in range(n)})


def open_faces(elements, opaque=None):
    """(index, face) of every left-out face of an unrotated box (not a flat plane, with at least one face drawn) that
    nothing covers: a point just outside it lies in no other unrotated box. Faces on the block's 0/16 planes are the
    seams of models cut into blocks and do not count (as in art_check O1); nor do the faces an element's "_keep_open"
    names."""
    boxes = [(i, e["from"], e["to"]) for i, e in enumerate(elements) if not e.get("rotation") and "from" in e]
    out = []
    for i, f, t in boxes:
        e = elements[i]
        present = set(e.get("faces", {}))
        if not present or len(present) == 6 or any(t[k] - f[k] <= 1e-6 for k in range(3)):
            continue
        if opaque is not None and not opaque(e):
            continue
        for face, (axis, sign) in _FACE_DIR.items():
            if face in present or face in e.get("_keep_open", ()):
                continue
            plane = t[axis] if sign > 0 else f[axis]
            if abs(plane) < 1e-6 or abs(plane - 16) < 1e-6:
                continue
            others = [k for k in range(3) if k != axis]
            exposed = False
            for a in _PROBES:
                for b in _PROBES:
                    p = [0.0, 0.0, 0.0]
                    p[axis] = plane + sign * 0.01
                    p[others[0]] = f[others[0]] + a * (t[others[0]] - f[others[0]])
                    p[others[1]] = f[others[1]] + b * (t[others[1]] - f[others[1]])
                    if not any(all(F[k] - 1e-6 <= p[k] <= T[k] + 1e-6 for k in range(3)) for j, F, T in boxes if j != i):
                        exposed = True
                        break
                if exposed:
                    break
            if exposed:
                out.append((i, face))
    return out


def _face_size(face, frm, to):
    across, down = _FACE_UV_AXES[face]
    return abs(to[across] - frm[across]), abs(to[down] - frm[down])


def _closing_face(e, face):
    """A face for left-out `face` of element e, drawn like the element's others: the opposite face's (same size) if it
    has one, otherwise a window of the face that needs the least stretch, at that face's texel density."""
    axis = _FACE_DIR[face][0]
    faces = e["faces"]
    opposite = next(f for f, (a, si) in _FACE_DIR.items() if a == axis and si == -_FACE_DIR[face][1])
    if opposite in faces:
        spec = copy.deepcopy(faces[opposite])
        spec.pop("cullface", None)
        return spec  # with no "uv" it reads its texture by position, as the opposite face does
    width, height = _face_size(face, e["from"], e["to"])
    best = None
    for name, spec in faces.items():
        w, h = _face_size(name, e["from"], e["to"])
        turned = spec.get("rotation", 0) in (90, 270)
        if turned:
            w, h = h, w
        need = (height, width) if turned else (width, height)
        if "uv" not in spec:
            # A texture read by position: the new face reads it by its own position too, at the same density.
            out = {k: copy.deepcopy(v) for k, v in spec.items() if k not in ("cullface", "rotation")}
            if best is None or 1.0 < best[0] - 1e-9:
                best = (1.0, out)
            continue
        u0, v0, u1, v1 = spec["uv"]
        uv, stretch = [u0, v0, u1, v1], 1.0
        for lo, hi, size, wanted in ((0, 2, w, need[0]), (1, 3, h, need[1])):
            span = uv[hi] - uv[lo]
            length = abs(span) / size * wanted if size > 1e-9 else abs(span)
            if length < abs(span):
                middle = (uv[lo] + uv[hi]) / 2
                direction = 1 if span >= 0 else -1
                uv[lo], uv[hi] = middle - direction * length / 2, middle + direction * length / 2
            elif length > 1e-9:
                stretch *= length / max(abs(span), 1e-9)
        if best is None or stretch < best[0] - 1e-9:
            out = {k: copy.deepcopy(v) for k, v in spec.items() if k not in ("cullface", "uv")}
            out["uv"] = [round(v, 4) + 0.0 for v in uv]
            best = (stretch, out)
    return best[1]


def close_open_faces(elements, opaque=None):
    """Draws every face open_faces finds left out and uncovered (a hole to look into: block models cull back faces).
    Returns how many it added."""
    found = open_faces(elements, opaque)
    for i, face in found:
        elements[i]["faces"][face] = _closing_face(elements[i], face)
    return len(found)


# Faces as (axis, sign) of the direction they face.
_FACE_DIR = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}
# How far a face is pushed out to stop it sharing a plane with another (pixels): invisible, but enough for the depth
# buffer out to about 70 blocks (0.02 held only to about 30).
COPLANAR_NUDGE = 0.1
# Differently drawn faces closer than this (pixels) count as flush too: the one in front is pushed out to COPLANAR_NUDGE.
MIN_GAP = 0.09


def auto_uv(face, frm, to):
    """The UV (pixels) Minecraft gives a face with no "uv": taken from the element's position."""
    fx, fy, fz = frm
    tx, ty, tz = to
    return {"down": [fx, 16 - tz, tx, 16 - fz], "up": [fx, fz, tx, tz], "north": [16 - tx, 16 - ty, 16 - fx, 16 - fy],
            "south": [fx, 16 - ty, tx, 16 - fy], "west": [fz, 16 - ty, tz, 16 - fy], "east": [16 - tz, 16 - ty, 16 - fz, 16 - fy]}[face]


def fit_uv(uv, clamp=True):
    """A UV rectangle moved inside the sprite (0..16), keeping its size (texel density) wherever the span fits: shifted
    by whole multiples of 16 when that brings it inside, so a tiling texture carries on seamlessly; a span that
    straddles a tile edge is shifted by whole tiles and then slid the least distance that brings it inside. A span
    longer than the sprite reads the whole sprite (or, with clamp=False, is left as it is)."""
    out = list(uv)
    for i, j in ((0, 2), (1, 3)):
        lo, hi = min(out[i], out[j]), max(out[i], out[j])
        if lo >= -1e-6 and hi <= 16 + 1e-6:
            continue
        if hi - lo > 16 + 1e-6:
            if clamp:
                out[i], out[j] = (0.0, 16.0) if out[i] <= out[j] else (16.0, 0.0)
            continue
        shift = -16 * math.floor((lo + 1e-6) / 16)  # the start into 0..16
        if hi + shift > 16 + 1e-6:
            over = hi + shift - 16  # slid back that far, or a tile lower and slid forward
            under = 16 - (lo + shift)
            shift = shift - over if over <= under else shift - 16 + under
        out[i], out[j] = out[i] + shift, out[j] + shift
    return [round(v, 4) + 0.0 for v in out]


def fit_uvs(elements):
    """Gives every face whose UV leaves the sprite (an automatic UV of an element reaching outside 0..16, or a written
    one) a UV inside it; otherwise Minecraft samples the neighbouring sprites of the block atlas (strips of other
    textures)."""
    for e in elements:
        for face, spec in e.get("faces", {}).items():
            uv = spec.get("uv") or auto_uv(face, e["from"], e["to"])
            if min(uv) < -1e-6 or max(uv) > 16 + 1e-6:
                spec["uv"] = fit_uv(uv)


def _rotations(e):
    return e.get("rotations") or ([e["rotation"]] if e.get("rotation") else [])


_AXIS_INDEX = {"x": 0, "y": 1, "z": 2}


def _turn(point, rotation):
    """A point turned by a block model element rotation (right-handed, as Minecraft turns them; rescale stretches
    across the axis)."""
    i = _AXIS_INDEX[rotation["axis"]]
    a, b = {0: (1, 2), 1: (2, 0), 2: (0, 1)}[i]
    rad = math.radians(rotation["angle"])
    origin = rotation.get("origin", [8, 8, 8])
    p = [point[k] - origin[k] for k in range(3)]
    out = list(p)
    out[a] = p[a] * math.cos(rad) - p[b] * math.sin(rad)
    out[b] = p[a] * math.sin(rad) + p[b] * math.cos(rad)
    if rotation.get("rescale") and abs(math.cos(rad)) > 1e-9:
        out[a] /= math.cos(rad)
        out[b] /= math.cos(rad)
    return [out[k] + origin[k] for k in range(3)]


def _world(e, point):
    for rotation in _rotations(e):
        point = _turn(point, rotation)
    return point


_CORNERS = ((0, 0), (1, 0), (1, 1), (0, 1))


def _face_point(face, frm, to, a, b):
    """The point of a face at (a, b) in 0..1 along its UV's u and v (Minecraft's mapping)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {"north": (x1 - a * (x1 - x0), y1 - b * (y1 - y0), z0), "south": (x0 + a * (x1 - x0), y1 - b * (y1 - y0), z1),
            "east": (x1, y1 - b * (y1 - y0), z1 - a * (z1 - z0)), "west": (x0, y1 - b * (y1 - y0), z0 + a * (z1 - z0)),
            "up": (x0 + a * (x1 - x0), y1, z0 + b * (z1 - z0)), "down": (x0 + a * (x1 - x0), y0, z1 - b * (z1 - z0))}[face]


def _uv_turn(a, b, r):
    return {90: (b, 1 - a), 180: (1 - a, 1 - b), 270: (1 - b, a)}.get(int(r or 0) % 360, (a, b))


def _face_frame(e, face, world):
    """(corner points, corner UVs) of a face, in world space (world(element, point) turns a point) or, with world None,
    in the element's own space."""
    frm, to = e["from"], e["to"]
    spec = e["faces"][face]
    u0, v0, u1, v1 = spec.get("uv") or auto_uv(face, frm, to)
    points, uvs = [], []
    for a, b in _CORNERS:
        p = _face_point(face, frm, to, a, b)
        points.append(world(e, p) if world else list(p))
        ra, rb = _uv_turn(a, b, spec.get("rotation"))
        uvs.append((u0 + ra * (u1 - u0), v0 + rb * (v1 - v0)))
    return points, uvs


def _uv_at(frame, point):
    """The UV a face draws at a point on its plane (through the affine map of corners 0, 1 and 3)."""
    points, uvs = frame
    p0, p1, p3 = points[0], points[1], points[3]
    e1 = [p1[k] - p0[k] for k in range(3)]
    e2 = [p3[k] - p0[k] for k in range(3)]
    d = [point[k] - p0[k] for k in range(3)]
    a11 = sum(v * v for v in e1)
    a12 = sum(e1[k] * e2[k] for k in range(3))
    a22 = sum(v * v for v in e2)
    b1 = sum(d[k] * e1[k] for k in range(3))
    b2 = sum(d[k] * e2[k] for k in range(3))
    det = a11 * a22 - a12 * a12
    if abs(det) < 1e-12:
        return uvs[0]
    s, t = (b1 * a22 - b2 * a12) / det, (b2 * a11 - b1 * a12) / det
    return (uvs[0][0] + s * (uvs[1][0] - uvs[0][0]) + t * (uvs[3][0] - uvs[0][0]),
            uvs[0][1] + s * (uvs[1][1] - uvs[0][1]) + t * (uvs[3][1] - uvs[0][1]))


def _same_look(first, face1, second, face2, overlap, world):
    """Whether two faces draw the same texels everywhere in their overlap (points on their shared plane)."""
    s1, s2 = first["faces"][face1], second["faces"][face2]
    if s1.get("texture") != s2.get("texture") or first.get("light_emission", 0) != second.get("light_emission", 0) \
            or s1.get("tintindex") != s2.get("tintindex"):
        return False
    if world is None and "uv" not in s1 and "uv" not in s2 and s1.get("rotation") == s2.get("rotation"):
        return True  # automatic UVs in the same frame
    f1, f2 = _face_frame(first, face1, world), _face_frame(second, face2, world)
    for point in overlap:
        a, b = _uv_at(f1, point), _uv_at(f2, point)
        if abs(a[0] - b[0]) > 1e-3 or abs(a[1] - b[1]) > 1e-3:
            return False
    return True


def _cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def _clip(subject, clipper):
    """Convex polygon clipping (Sutherland-Hodgman), both counter-clockwise."""
    out = subject
    for i in range(len(clipper)):
        if not out:
            break
        a, b = clipper[i], clipper[(i + 1) % len(clipper)]
        inp, out = out, []

        def inside(p):
            return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9

        def cut(p, q):
            dx1, dy1, dx2, dy2 = q[0] - p[0], q[1] - p[1], b[0] - a[0], b[1] - a[1]
            den = dx1 * dy2 - dy1 * dx2
            if abs(den) < 1e-15:
                return q
            t = ((a[0] - p[0]) * dy2 - (a[1] - p[1]) * dx2) / den
            return (p[0] + t * dx1, p[1] + t * dy1)
        for j in range(len(inp)):
            p, q = inp[j], inp[(j + 1) % len(inp)]
            if inside(q):
                if not inside(p):
                    out.append(cut(p, q))
                out.append(q)
            elif inside(p):
                out.append(cut(p, q))
    return out


def _area(poly):
    return sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1]
               for i in range(len(poly))) / 2


def _ccw(poly):
    return poly if _area(poly) >= 0 else poly[::-1]


def _push(e, face, nudge, pin_uv):
    """Moves one face of an element outward by nudge (the whole element stays a closed box; a flat plane moves whole).
    With pin_uv, the faces whose automatic UV would change keep the UV they had (moved inside the sprite), so the move
    never reads past it."""
    axis, sign = _FACE_DIR[face]
    opposite = next(f for f, (a, si) in _FACE_DIR.items() if a == axis and si == -sign)
    if abs(e["to"][axis] - e["from"][axis]) < 1e-9 and opposite not in e["faces"]:
        # A one-sided flat plane (a decal) moves whole, so it stays a plane rather than becoming a box with open sides.
        e["from"][axis] = round(e["from"][axis] + sign * nudge, 4)
        e["to"][axis] = round(e["to"][axis] + sign * nudge, 4)
        return
    if pin_uv:
        for other, spec in e["faces"].items():
            if "uv" not in spec and _FACE_DIR[other][0] != axis:
                spec["uv"] = fit_uv(auto_uv(other, e["from"], e["to"]))
    if sign > 0:
        e["to"][axis] = round(e["to"][axis] + nudge, 4)
    else:
        e["from"][axis] = round(e["from"][axis] - nudge, 4)


def _solve(m, b):
    """x with m x = b (3x3, Gaussian elimination with partial pivoting)."""
    a = [list(m[i]) + [b[i]] for i in range(3)]
    for c in range(3):
        pivot = max(range(c, 3), key=lambda r: abs(a[r][c]))
        a[c], a[pivot] = a[pivot], a[c]
        for r in range(3):
            if r != c and abs(a[c][c]) > 1e-12:
                f = a[r][c] / a[c][c]
                a[r] = [a[r][k] - f * a[c][k] for k in range(4)]
    return [a[i][3] / a[i][i] if abs(a[i][i]) > 1e-12 else 0.0 for i in range(3)]


def _linear(e, world):
    """An element's turn as world(p) = m p + c: (m, c)."""
    c = world(e, [0.0, 0.0, 0.0])
    cols = [[world(e, unit)[k] - c[k] for k in range(3)] for unit in ([1.0, 0, 0], [0, 1.0, 0], [0, 0, 1.0])]
    return [[cols[j][i] for j in range(3)] for i in range(3)], c


def _overlap_corners(axis, plane, lo, hi):
    others = [i for i in range(3) if i != axis]
    corners = []
    for u, v in ((lo[0], lo[1]), (hi[0], lo[1]), (hi[0], hi[1]), (lo[0], hi[1])):
        point = [0.0] * 3
        point[axis], point[others[0]], point[others[1]] = plane, u, v
        corners.append(point)
    return corners


def separate_coplanar(elements, nudge=COPLANAR_NUDGE, pin_uv=True, pair_filter=None, world=_world, links=None):
    """Stops z-fighting: where two elements have faces on the same plane, facing the same way, overlapping and
    drawn differently, the GPU flickers between them (most often a band, dial or trim laid flush on a body). The
    smaller face (the detail) is pushed out by `nudge` so it always draws in front; the element stays a closed box.
    Faces closer than MIN_GAP count as flush too: the one in front is pushed out until it stands `nudge` proud. Faces
    that draw the same texels (same texture and UV mapping) do not flicker and are left alone.

    Rotated elements count too: elements turned the same way (whatever their origin) are compared in one shared
    unturned space, so a push survives the turn; faces that lie square to the world after turning (a face along its
    element's rotation axis, as on every gear and handwheel) are compared as turned polygons with everything else.
    pair_filter(first, face1, second, face2), if given, picks the pairs to consider; world(element, point) turns a
    point the way the model's consumer does (Minecraft's block model rotations by default). links maps id(element) to
    the elements that move with it (the pieces of one element cut into part models): a push moves each of them whose
    face lies on the same plane by as much, so no step opens at the seams between them."""
    for index, e in enumerate(elements):
        backwards = [axis for axis in range(3) if e["from"][axis] > e["to"][axis] + 1e-9]
        if backwards:
            raise ValueError(f"element {index} runs backwards on axis {'/'.join('xyz'[a] for a in backwards)} (from {e['from']} "
                             f"to {e['to']}): write its low corner as \"from\" (Minecraft draws such a box inside out)")
    live = [e for e in elements if e.get("faces")]
    reach = min(MIN_GAP, nudge)

    def flush(first, face1, second, face2, front, gap, overlap, frame):
        """Settles one pair: pushes the detail (gap 0) or the face in front (0 < gap < reach). True if it moved."""
        if pair_filter and not pair_filter(first, face1, second, face2):
            return False
        if _same_look(first, face1, second, face2, overlap, frame):
            return False
        mover, face = front
        amount = nudge if gap <= 1e-9 else round(nudge - gap, 4)
        together = [mover]
        if links:
            axis, sign = _FACE_DIR[face]

            def plane(e):
                return world(e, list(e["to"] if sign > 0 else e["from"]))[axis]
            here = plane(mover)
            together += [e for e in links.get(id(mover), ()) if e is not mover and face in e.get("faces", {})
                         and (id(e), face) not in pushed and abs(plane(e) - here) < 1e-6]
        for e in together:
            _push(e, face, amount, pin_uv)
            pushed.add((id(e), face))
        return True

    for _ in range(16):  # A push can line a face up with a third element (a stack of reliefs); passes settle it.
        moved = False
        pushed = set()
        # Elements turned by the same linear transform: boxes in one shared unturned space (each shifted by d).
        groups = {}
        for e in live:
            m, c = _linear(e, world)
            key = tuple(round(m[i][j], 6) for i in range(3) for j in range(3))
            groups.setdefault(key, []).append((e, m, c))
        for key, members in groups.items():
            ref_c = members[0][2]
            shift = {}
            for e, m, c in members:
                d = _solve(m, [c[k] - ref_c[k] for k in range(3)])
                shift[id(e)] = [0.0 if abs(v) < 1e-9 else v for v in d]
            shifted = any(any(v for v in d) for d in shift.values())

            def lo_hi(e, j):
                return e["from"][j] + shift[id(e)][j], e["to"][j] + shift[id(e)][j]

            def plane_of(e, face):
                axis, sign = _FACE_DIR[face]
                return round((e["to"][axis] if sign > 0 else e["from"][axis]) + shift[id(e)][axis], 4)

            frame = (lambda e, p: [p[k] + shift[id(e)][k] for k in range(3)]) if shifted else None
            by_face = {}
            for e, _m, _c in members:
                for face in e["faces"]:
                    by_face.setdefault(face, {}).setdefault(plane_of(e, face), []).append(e)
            for face, planes in by_face.items():
                axis, sign = _FACE_DIR[face]
                others = [i for i in range(3) if i != axis]
                ordered = sorted(planes)
                for n1, plane1 in enumerate(ordered):
                    for plane2 in ordered[n1:]:
                        gap = plane2 - plane1
                        if gap >= reach - 1e-9:
                            break
                        bucket1, bucket2 = planes[plane1], planes[plane2]
                        for i, first in enumerate(bucket1):
                            for second in (bucket1[i + 1:] if gap == 0 else bucket2):
                                if plane_of(first, face) != plane1 or plane_of(second, face) != plane2:
                                    continue  # one was pushed already this pass
                                lo = [max(lo_hi(first, j)[0], lo_hi(second, j)[0]) for j in others]
                                hi = [min(lo_hi(first, j)[1], lo_hi(second, j)[1]) for j in others]
                                if hi[0] - lo[0] <= 1e-3 or hi[1] - lo[1] <= 1e-3:
                                    continue
                                if gap == 0:
                                    def size(e):
                                        return (e["to"][others[0]] - e["from"][others[0]]) * (e["to"][others[1]] - e["from"][others[1]])
                                    mover = second if size(second) <= size(first) else first
                                else:
                                    mover = second if sign > 0 else first
                                if flush(first, face, second, face, (mover, face), gap,
                                         _overlap_corners(axis, plane1, lo, hi), frame):
                                    moved = True
        # Across differently turned elements: turned polygons on (nearly) one plane, whatever its slope.
        if len(groups) > 1:
            group_of = {}
            for key, members in groups.items():
                for e, _m, _c in members:
                    group_of[id(e)] = key
            by_normal = {}
            for e in live:
                for face in e["faces"]:
                    points = _face_frame(e, face, world)[0]
                    n = _cross([points[1][k] - points[0][k] for k in range(3)], [points[3][k] - points[0][k] for k in range(3)])
                    length = math.sqrt(sum(v * v for v in n))
                    if length < 1e-9:
                        continue
                    # Orient the normal the way the turned face looks: its centre against a point just in front.
                    local_axis, local_sign = _FACE_DIR[face]
                    centre = [(e["from"][k] + e["to"][k]) / 2 for k in range(3)]
                    centre[local_axis] = e["to"][local_axis] if local_sign > 0 else e["from"][local_axis]
                    ahead = list(centre)
                    ahead[local_axis] += local_sign
                    look = [world(e, ahead)[k] - world(e, centre)[k] for k in range(3)]
                    n = [v / length for v in n]
                    if sum(n[k] * look[k] for k in range(3)) < 0:
                        n = [-v for v in n]
                    key = tuple(round(v, 4) + 0.0 for v in n)
                    by_normal.setdefault(key, []).append((sum(n[k] * points[0][k] for k in range(3)), e, face, points))
            for key, items in by_normal.items():
                if len({group_of[id(e)] for _d, e, _f, _p in items}) < 2:
                    continue
                n = list(key)
                u = _cross(n, (1, 0, 0) if abs(n[0]) < 0.9 else (0, 1, 0))
                length = math.sqrt(sum(v * v for v in u))
                u = [v / length for v in u]
                v_ = _cross(n, u)
                items.sort(key=lambda item: item[0])
                polys = {}

                def poly(item):
                    if id(item) not in polys:
                        polys[id(item)] = _ccw([(sum(p[k] * u[k] for k in range(3)), sum(p[k] * v_[k] for k in range(3)))
                                                for p in item[3]])
                    return polys[id(item)]
                for i, back in enumerate(items):
                    for front in items[i + 1:]:
                        gap = front[0] - back[0]
                        if gap >= reach - 1e-9:
                            break
                        gap = 0.0 if gap < 1e-4 else gap
                        (_d1, first, face1, _p1), (_d2, second, face2, _p2) = back, front
                        if group_of[id(first)] == group_of[id(second)]:
                            continue  # compared in their shared space above
                        if (id(first), face1) in pushed or (id(second), face2) in pushed:
                            continue  # moved this pass: the next pass looks again
                        overlap = _clip(poly(back), poly(front))
                        if len(overlap) < 3 or abs(_area(overlap)) <= 1e-6:
                            continue
                        if gap == 0:
                            small = abs(_area(poly(front))) <= abs(_area(poly(back))) + 1e-6
                            mover = (second, face2) if small else (first, face1)
                        else:
                            mover = (second, face2)  # the one in front
                        points = [[a * u[k] + b * v_[k] + n[k] * back[0] for k in range(3)] for a, b in overlap]
                        if flush(first, face1, second, face2, mover, gap, points, world):
                            moved = True
        if not moved:
            break


def separate_boxes(elements, nudge=COPLANAR_NUDGE, pair_filter=None, turn=None):
    """separate_coplanar for model tuples (from, to, texture[, options]) as the generators write them; returns new
    tuples. Pinned UVs come back as options["uv"] = {face: [u0, v0, u1, v1]}. turn(point, rotation), if given, turns
    a point by an element's "rotation" option the way the caller's exporter does."""
    dicts = []
    for item in elements:
        frm, to, texture, options = unpack(item)
        d = element(frm, to, texture, rotation=options.get("rotation"))
        for face, spec in d["faces"].items():
            if face in options.get("uv", {}):
                spec["uv"] = list(options["uv"][face])
        d["_source"] = item
        dicts.append(d)
    world = _world
    if turn:
        def world(e, point):
            rotation = unpack(e["_source"])[3].get("rotation")
            return turn(point, rotation) if rotation else list(point)
    separate_coplanar(dicts, nudge, pin_uv=True, pair_filter=pair_filter and (
        lambda a, fa, b, fb: pair_filter(a["_source"], fa, b["_source"], fb)), world=world)
    out = []
    for d, item in zip(dicts, elements):
        frm, to, texture, options = unpack(item)
        uv = {face: spec["uv"] for face, spec in d["faces"].items()
              if "uv" in spec and not str(_face_texture(texture, face)).endswith("!")}
        options = dict(options, uv=uv) if uv else options
        out.append((list(d["from"]), list(d["to"]), texture, options))
    return out


def unpack(element):
    frm, to, texture = element[:3]
    options = element[3] if len(element) > 3 else {}
    return frm, to, texture, options


def _face_texture(texture, face):
    tex = texture.get(face, texture.get("*")) if isinstance(texture, dict) else texture
    return tex


# Textures that glow (electric look): an element drawn only with these is lit at full brightness in the dark
# (Minecraft's per-element "light_emission"; it lights the element itself, not the blocks around it).
EMISSIVE = {"el_glow", "el_glow_cyan", "el_glow_violet"}


def element(frm, to, texture, uv=False, skip=(), rotation=None):
    """One model element; uv=True gives explicit UVs for elements outside 0..16 or scaled ones."""
    faces = {}
    names = {v.rstrip("!") for v in (texture.values() if isinstance(texture, dict) else [texture]) if v is not None}
    for face in FACES:
        if face in skip:
            continue
        tex = _face_texture(texture, face)
        if tex is None:
            continue
        stretch = tex.endswith("!")
        tex = tex.rstrip("!")
        ref = tex if tex.startswith("#") else f"#{tex}"
        entry = {"texture": ref}
        if stretch:
            entry["uv"] = [0, 0, 16, 16]
        elif uv:
            u_axis, v_axis = FACE_AXES[face][:2]
            width = min(16, abs(to[u_axis] - frm[u_axis]))
            height = min(16, abs(to[v_axis] - frm[v_axis]))
            entry["uv"] = [0, 0, round(width, 3), round(height, 3)]
        faces[face] = entry
    out = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if names and names <= EMISSIVE:
        out["light_emission"] = 15
    if rotation:
        axis, angle, origin = rotation[:3]
        out["rotation"] = {"origin": [round(v, 4) for v in origin], "axis": axis, "angle": angle}
        if len(rotation) > 3 and rotation[3]:
            out["rotation"]["rescale"] = True
    return out


def texture_names(elements):
    names = set()
    for item in elements:
        texture = unpack(item)[2]
        values = texture.values() if isinstance(texture, dict) else [texture]
        names |= {value.rstrip("!") for value in values if value is not None}
    return sorted(name for name in names if not name.startswith("#"))


def separated_bounds(elements, footprint=None):
    """separate_coplanar over a whole structure-space model (tuples), before slice_model cuts it into part models, so
    a detail and the body it sits on are pulled apart even when they end up in different part files. With the
    footprint, an element slice_model will keep whole with written UVs is compared with those UVs (the look it gets).
    Returns, for each element, its pushed (from, to), or None where nothing moved."""
    dicts = []
    for item in elements:
        frm, to, texture, options = unpack(item)
        written = False
        if footprint is not None:
            kind, _index, inside = _placement(frm, to, options.get("rotation"), footprint)
            written = kind == "whole" and not inside
        dicts.append(element(frm, to, texture, uv=written, rotation=options.get("rotation")))
    separate_coplanar(dicts, pin_uv=False)
    out = []
    for item, d in zip(elements, dicts):
        frm, to = unpack(item)[:2]
        moved = any(abs(d["from"][k] - frm[k]) > 1e-9 or abs(d["to"][k] - to[k]) > 1e-9 for k in range(3))
        out.append((list(d["from"]), list(d["to"])) if moved else None)
    return out


def separate_parts(parts, footprint, sources=None):
    """After slice_model: separate_coplanar over every part model of a multi-block at once, each in its block of the
    structure, comparing the UVs each part's faces really have (part-local, fitted into the sprite). It settles what
    the pass before slicing cannot see: pushes that reach across a part boundary, and pieces whose UVs differ only
    because they sit in different part files. sources (from slice_model) names the element each piece was cut from:
    the pieces of one element move together, so no step opens at the seam between two parts (whose cut faces are
    left out). Works in place on the parts' element dicts."""
    union, offsets, pieces = [], {}, defaultdict(list)
    for index, part in enumerate(parts):
        part[:] = [copy.deepcopy(e) for e in part]
        fit_uvs(part)
        for position, e in enumerate(part):
            offsets[id(e)] = [footprint[index][k] * 16 for k in range(3)]
            union.append(e)
            if sources is not None and sources[index][position] is not None:
                pieces[sources[index][position]].append(e)
    links = {id(e): group for group in pieces.values() if len(group) > 1 for e in group}

    def world(e, point):
        q = _world(e, point)
        return [q[k] + offsets[id(e)][k] for k in range(3)]
    separate_coplanar(union, world=world, links=links)
    return parts


def split_model(name, elements, footprint):
    """A structure-space model (tuples) cut into its part models with no shared planes inside or across them:
    separated_bounds before the cut, slice_model, then separate_parts with the pieces of each cut element linked."""
    sources = []
    parts = slice_model(name, elements, footprint, separated_bounds(elements, footprint), sources=sources)
    return separate_parts(parts, footprint, sources)


def _placement(frm, to, rotation, footprint):
    """How slice_model places an element: ("slice", None, None) when the footprint covers it, else ("whole", index of
    the part nearest its centre, whether it fits inside that part's block)."""
    if not rotation:
        volume = (to[0] - frm[0]) * (to[1] - frm[1]) * (to[2] - frm[2])
        covered, pieces = 0.0, 0
        for offset in footprint:
            low = [offset[axis] * 16 for axis in range(3)]
            a = [max(frm[axis], low[axis]) for axis in range(3)]
            b = [min(to[axis], low[axis] + 16) for axis in range(3)]
            if all(a[axis] < b[axis] for axis in range(3)):
                covered += (b[0] - a[0]) * (b[1] - a[1]) * (b[2] - a[2])
                pieces += 1
        if pieces and abs(covered - volume) < 1e-6:
            return "slice", None, None
    center = list(rotation[2]) if rotation else [(frm[axis] + to[axis]) / 2 for axis in range(3)]
    index = min(range(len(footprint)), key=lambda i: sum(
        (center[axis] - (footprint[i][axis] * 16 + 8)) ** 2 for axis in range(3)))
    low = [footprint[index][axis] * 16 for axis in range(3)]
    inside = min(frm[axis] - low[axis] for axis in range(3)) >= 0 and max(to[axis] - low[axis] for axis in range(3)) <= 16
    return "whole", index, inside


def slice_model(name, elements, footprint, moved=None, sources=None):
    """Cuts one structure-space model into per-part models, like Immersive Engineering's split models. moved (from
    separated_bounds) gives elements' pushed bounds: the cut follows the original bounds, then each piece's outer faces
    take the pushed planes. A list given as sources is filled, per part, with the index of the element each piece was
    cut from (None for an element kept whole), for separate_parts."""
    parts = [[] for _ in footprint]
    if sources is not None:
        sources[:] = [[] for _ in footprint]
    for number, item in enumerate(elements):
        frm, to, texture, options = unpack(item)
        new_frm, new_to = moved[number] if moved and moved[number] else (frm, to)
        rotation = options.get("rotation")
        kind, index, inside = _placement(frm, to, rotation, footprint)
        if kind == "slice":
            for index, offset in enumerate(footprint):
                low = [offset[axis] * 16 for axis in range(3)]
                a = [max(frm[axis], low[axis]) for axis in range(3)]
                b = [min(to[axis], low[axis] + 16) for axis in range(3)]
                if all(a[axis] < b[axis] for axis in range(3)):
                    # Drop faces created by the cut: they are inside the element.
                    skip = []
                    for face, (_, _, axis, side) in FACE_AXES.items():
                        edge = b[axis] if side else a[axis]
                        original = to[axis] if side else frm[axis]
                        if edge != original:
                            skip.append(face)
                    # The element's own faces in this piece take their pushed planes.
                    a = [new_frm[axis] if a[axis] == frm[axis] else a[axis] for axis in range(3)]
                    b = [new_to[axis] if b[axis] == to[axis] else b[axis] for axis in range(3)]
                    local_a = [a[axis] - low[axis] for axis in range(3)]
                    local_b = [b[axis] - low[axis] for axis in range(3)]
                    parts[index].append(element(local_a, local_b, texture, skip=skip))
                    if sources is not None:
                        sources[index].append(number)
            continue
        # Reaches outside the footprint, or is rotated: keep it whole on the part nearest its center.
        low = [footprint[index][axis] * 16 for axis in range(3)]
        local_a = [new_frm[axis] - low[axis] for axis in range(3)]
        local_b = [new_to[axis] - low[axis] for axis in range(3)]
        if min(local_a) < -16 or max(local_b) > 32:
            raise ValueError(f"{name}: element {frm}..{to} is too far from its part")
        local_rotation = None
        if rotation:
            local_rotation = (rotation[0], rotation[1], [rotation[2][axis] - low[axis] for axis in range(3)], *rotation[3:])
        parts[index].append(element(local_a, local_b, texture, uv=not inside, rotation=local_rotation))
        if sources is not None:
            sources[index].append(None)
    return parts


def scaled_elements(elements):
    """The whole machine scaled down into one block, for the inventory and hand."""
    items = [unpack(item) for item in elements]
    low = [min(min(f[axis], t[axis]) for f, t, _, _ in items) for axis in range(3)]
    high = [max(max(f[axis], t[axis]) for f, t, _, _ in items) for axis in range(3)]
    scale = 16 / max(high[axis] - low[axis] for axis in range(3))
    shift = [(16 - (high[axis] - low[axis]) * scale) / 2 for axis in range(3)]

    def place(point):
        return [(point[axis] - low[axis]) * scale + shift[axis] for axis in range(3)]

    out = []
    for frm, to, texture, options in items:
        rotation = options.get("rotation")
        if rotation:
            rotation = (rotation[0], rotation[1], place(rotation[2]), *rotation[3:])
        out.append(element(place(frm), place(to), texture, uv=True, rotation=rotation))
    return out


# ------------------------------------------------------------------ writers

class StyleWriter:
    """Writes one style's machine assets under an assets/<mod> root."""

    def __init__(self, root):
        self.root = Path(root)

    def model(self, name, obj):
        write(self.root / "models" / "block" / f"{name}.json", obj)

    def item_model(self, name, obj):
        write(self.root / "models" / "item" / f"{name}.json", obj)

    def blockstate(self, name, obj):
        write(self.root / "blockstates" / f"{name}.json", obj)

    def item(self, name, model):
        write(self.root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model}})

    def single_variant(self, name, model):
        self.blockstate(name, {"variants": {"": {"model": model}}})

    def facing_lit_states(self, name, lit):
        variants = {}
        for facing, y in FACING_Y.items():
            rotation = {"y": y} if y else {}
            for state in ("false", "true"):
                on = state == "true" and lit
                variants[f"facing={facing},lit={state}"] = {"model": rid(f"block/{name}_on" if on else f"block/{name}"),
                                                             **rotation}
        self.blockstate(name, {"variants": variants})

    def large_states(self, name, parts, lit):
        variants = {}
        for facing, y in FACING_Y.items():
            rotation = {"y": y} if y else {}
            for state in ("false", "true"):
                for part in range(PART_STATES):
                    if part < parts:
                        on = "_on" if state == "true" and lit else ""
                        model = rid(f"block/{name}_part{part}{on}")
                    else:
                        model = rid("block/large_machine_empty")
                    variants[f"facing={facing},lit={state},part={part}"] = {"model": model, **rotation}
        self.blockstate(name, {"variants": variants})

    def enlarged_states(self, name, parts, lit):
        """A batch 44 giant: compact=false is the multi-block (as large_states); compact=true is a copy built before it
        was enlarged, one block with the old model ({name} and {name}_on) whatever its part."""
        variants = {}
        for facing, y in FACING_Y.items():
            rotation = {"y": y} if y else {}
            for state in ("false", "true"):
                on = "_on" if state == "true" and lit else ""
                for part in range(PART_STATES):
                    big = rid(f"block/{name}_part{part}{on}") if part < parts else rid("block/large_machine_empty")
                    variants[f"compact=false,facing={facing},lit={state},part={part}"] = {"model": big, **rotation}
                    variants[f"compact=true,facing={facing},lit={state},part={part}"] = {
                        "model": rid(f"block/{name}{on}"), **rotation}
        self.blockstate(name, {"variants": variants})

    def empty_part(self, particle):
        self.model("large_machine_empty", {"textures": {"particle": rid(f"block/{particle}")}, "elements": []})


def write_classic(root, machines, parts, fluid_blocks):
    """The original look: textured orientable cubes, plus the large machines in tools/large_machines.py."""
    # New battery hardware shares its authored electrical cabinet in both packs.
    import battery_expansion
    battery_machines = {k:v for k,v in machines.items() if k in battery_expansion.BLOCKS}
    machines = {k:v for k,v in machines.items() if k not in battery_machines}
    from large_machines import ENLARGED, FOOTPRINTS, MODELS, FRONTS
    from machine_connections import with_sockets, legacy_ports, permanent_ports, write_dynamic_sockets
    out = StyleWriter(root)
    write_dynamic_sockets(root, MODELS, FOOTPRINTS)
    if battery_machines:
        write_steampunk(root, battery_machines, {}, {})
    import jimtafacts_industry
    for machine, info in machines.items():
        if machine in jimtafacts_industry.MACHINES and jimtafacts_industry.MACHINES[machine][3] is None:
            continue  # Original owner housing is written to both styles by its importer.
        if machine in ENLARGED:
            # The compact look (copies built before batch 44) is the old orientable cube.
            for suffix, front in (("", "front"), ("_on", "front_on")):
                if suffix and not info["lit"]:
                    continue
                out.model(f"{machine}{suffix}", {
                    "parent": "minecraft:block/orientable",
                    "textures": {"top": rid(f"block/{info.get('top', 'machine_top')}"), "side": rid("block/machine_side"),
                                 "front": rid(f"block/{info.get('front', f'{machine}_{front}')}")},
                })
        if machine in FOOTPRINTS:
            front = FRONTS[machine]
            connected_elements = with_sockets(MODELS[machine], permanent_ports(machine, FOOTPRINTS[machine]))
            textures = {name: rid(f"block/{name}") for name in texture_names(connected_elements)}
            textures["front"] = rid(f"block/{front}")
            textures["particle"] = rid("block/machine_side")
            sliced = split_model(machine, connected_elements, FOOTPRINTS[machine])
            for index, elements in enumerate(sliced):
                out.model(f"{machine}_part{index}", {"ambientocclusion": False, "textures": textures, "elements": elements})
                if info["lit"]:
                    out.model(f"{machine}_part{index}_on", {"parent": rid(f"block/{machine}_part{index}"),
                                                            "textures": {"front": rid(f"block/{front}_on")}})
            out.empty_part("machine_side")
            if machine in ENLARGED:
                out.enlarged_states(machine, len(FOOTPRINTS[machine]), info["lit"])
            else:
                out.large_states(machine, len(FOOTPRINTS[machine]), info["lit"])
            # Parts a renderer draws (giant_models.ITEM_EXTRAS: the sawmill's blade) stand in the item icon too.
            from steampunk_models import ITEM_EXTRAS
            extras = ITEM_EXTRAS.get(machine, [])
            out.item_model(machine, {"parent": "minecraft:block/block",
                                     "textures": {**textures, **{name: rid(f"block/{name}") for name in texture_names(extras)}},
                                     "elements": scaled_elements(connected_elements + extras)})
            out.item(machine, rid(f"item/{machine}"))
            continue
        for suffix, front in (("", "front"), ("_on", "front_on")):
            if suffix and not info["lit"]:
                continue
            front_texture = info.get("front", f"{machine}_{front}")
            out.model(f"{machine}{suffix}", {
                "parent": "minecraft:block/orientable",
                "textures": {"top": rid(f"block/{info.get('top', 'machine_top')}"), "side": rid("block/machine_side"),
                             "front": rid(f"block/{front_texture}")},
            })
        out.facing_lit_states(machine, info["lit"])
        out.item(machine, rid(f"block/{machine}"))

    for part in parts:
        out.single_variant(part, rid(f"block/{part}"))
        out.model(part, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{part}")}})
        out.item(part, rid(f"block/{part}"))

    for block in fluid_blocks:
        if block in ("insulated_fluid_tank", "vacuum_insulated_tank"):
            from cryogenic_models import tank_model
            out.single_variant(block, rid(f"block/{block}"))
            out.model(block, tank_model(block == "vacuum_insulated_tank"))
            out.item(block, rid(f"block/{block}"))
            continue
        out.single_variant(block, rid(f"block/{block}"))
        out.model(block, {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": rid(f"block/{block}_top"), "side": rid(f"block/{block}_side"),
                         "bottom": rid(f"block/{block}_bottom")}})
        out.item(block, rid(f"block/{block}"))


def write_steampunk(root, machines, parts, fluid_blocks):
    """Detailed models for everything, from tools/steampunk_models.py."""
    from large_machines import ENLARGED, FOOTPRINTS
    from steampunk_models import COMPACT, MODELS, GLOW, CUBES, PARTICLE
    from machine_connections import with_sockets, legacy_ports, permanent_ports, write_dynamic_sockets
    out = StyleWriter(root)
    write_dynamic_sockets(root, MODELS, FOOTPRINTS)

    from large_machines import FRONTS

    def textures_for(elements, machine=None):
        textures = {name: rid(f"block/{name}") for name in texture_names(elements)}
        textures["particle"] = rid(f"block/{PARTICLE}")
        # Models drawn with the machine's own front ("#front": the nuclear fuel machines) take it from FRONTS.
        uses_front = any(v is not None and v.rstrip("!") == "#front"
                         for item in elements
                         for v in (unpack(item)[2].values() if isinstance(unpack(item)[2], dict) else [unpack(item)[2]]))
        if uses_front and machine in FRONTS:
            textures["front"] = rid(f"block/{FRONTS[machine]}")
        return textures

    def glow_override(elements):
        return {name: rid(f"block/{GLOW[name]}") for name in texture_names(elements) if name in GLOW}

    import jimtafacts_industry
    for machine, info in machines.items():
        if machine in jimtafacts_industry.MACHINES and jimtafacts_industry.MACHINES[machine][3] is None:
            continue  # Original owner housing is written to both styles by its importer.
        elements = with_sockets(MODELS[machine], permanent_ports(machine, FOOTPRINTS.get(machine, [(0, 0, 0)])))
        textures = textures_for(elements, machine)
        lit = info["lit"] and bool(glow_override(elements))
        if machine in FOOTPRINTS:
            sliced = split_model(machine, elements, FOOTPRINTS[machine])
            for index, part_elements in enumerate(sliced):
                out.model(f"{machine}_part{index}", {"ambientocclusion": False, "textures": textures,
                                                     "elements": part_elements})
                if lit:
                    out.model(f"{machine}_part{index}_on", {"parent": rid(f"block/{machine}_part{index}"),
                                                            "textures": glow_override(elements)})
            out.empty_part(PARTICLE)
            if machine in ENLARGED:
                # Copies built before batch 44 keep the old one-block model; it glows only if the big one does, since
                # both share the LIT state.
                compact = with_sockets(COMPACT[machine], legacy_ports(machine, [(0, 0, 0)]))
                out.model(machine, {"parent": "minecraft:block/block", "textures": textures_for(compact),
                                    "elements": slice_model(machine, compact, [(0, 0, 0)])[0]})
                out.model(f"{machine}_on", {"parent": rid(f"block/{machine}"), "textures": glow_override(compact)}
                          if lit and glow_override(compact) else {"parent": rid(f"block/{machine}")})
                out.enlarged_states(machine, len(FOOTPRINTS[machine]), lit)
            else:
                out.large_states(machine, len(FOOTPRINTS[machine]), lit)
            # Parts a renderer draws (giant_models.ITEM_EXTRAS: the sawmill's blade) stand in the item icon too.
            from steampunk_models import ITEM_EXTRAS
            item_elements = elements + ITEM_EXTRAS.get(machine, [])
            out.item_model(machine, {"parent": "minecraft:block/block", "textures": textures_for(item_elements, machine),
                                     "elements": scaled_elements(item_elements)})
            out.item(machine, rid(f"item/{machine}"))
            continue
        out.model(machine, {"parent": "minecraft:block/block", "textures": textures,
                            "elements": slice_model(machine, elements, [(0, 0, 0)])[0]})
        if lit:
            out.model(f"{machine}_on", {"parent": rid(f"block/{machine}"), "textures": glow_override(elements)})
        out.facing_lit_states(machine, lit)
        out.item(machine, rid(f"block/{machine}"))

    for part in parts:
        out.single_variant(part, rid(f"block/{part}"))
        out.model(part, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{CUBES[part]}")}})
        out.item(part, rid(f"block/{part}"))

    for block in fluid_blocks:
        if block in ("insulated_fluid_tank", "vacuum_insulated_tank"):
            from cryogenic_models import tank_model
            out.single_variant(block, rid(f"block/{block}"))
            out.model(block, tank_model(block == "vacuum_insulated_tank"))
            out.item(block, rid(f"block/{block}"))
            continue
        from machine_connections import exterior_ports
        sockets = legacy_ports(block, [(0, 0, 0)]) or [(p, f, "fluid") for p, f, _ in exterior_ports()]
        elements = with_sockets(MODELS[block], sockets)
        out.single_variant(block, rid(f"block/{block}"))
        out.model(block, {"parent": "minecraft:block/block", "textures": textures_for(elements),
                          "elements": slice_model(block, elements, [(0, 0, 0)])[0]})
        out.item(block, rid(f"block/{block}"))


WRITERS = {"classic": write_classic, "steampunk": write_steampunk}


def pack_metadata(style):
    """pack.mcmeta for the built-in pack; the format range matches Fabric's own 26.3 test pack."""
    look = "the original classic look" if style == "classic" else "the steampunk look"
    return {"pack": {"description": f"Gives Jugcraft's machines {look}",
                     "min_format": 71, "max_format": 2048}}
