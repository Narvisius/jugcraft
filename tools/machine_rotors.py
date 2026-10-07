"""Spinning parts of multi-block machines, drawn by client/MachineRotors (through the machines' block entity renderer)
instead of by their block models: the giant sawmill's blade and belt drive and the giant sieve's eccentric weights.

Written to assets/jugcraft/machine_rotor_quads.json by generate_material_data.py. Each entry is one rotor:

  "<name>": {"block": machine id, "axis": "x" | "y" | "z", "center": [x, y, z] (pixels, structure space),
             "property": block state property that sets it running ("lit"), "speed": degrees per tick while running,
             "ease": ticks to reach full speed or to stop, "always": true to draw it standing still while idle (the
             block models leave these parts out), "when": {property: value} that must all hold (e.g. compact=false, so a
             compact pre-batch-44 copy never shows a giant's rotor), "quads": [...]}

Quads are QuadModel's (client/QuadModel): {texture, normal, vertices [[x, y, z, u, v] x 4]}, in structure-space pixels
(north-facing, the master block at 0..16), wound counter-clockwise seen from the side the normal points to, every UV
inside 0..1 (nothing relies on texture wrapping) and mapped one texel per pixel or once across a disc face. Shapes are
true polygon prisms rather than stepped boxes, so a spinning disc stays round. They are closed solids with no two
differently drawn faces on one plane facing the same way (no flicker); faces that touch face opposite ways.
"""
import math

# How far apart (pixels) two parts of one rotor sit at the least, so their faces never share a plane.
GAP = 0.1


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def quad(points, uvs, texture, normal):
    """One QuadModel quad. The corners are reordered (with their UVs) to wind counter-clockwise seen from the normal's
    side, the order the solid render type keeps when it culls back faces."""
    pts = list(zip(points, uvs))
    p = [q[0] for q in pts]
    # Newell's normal of the polygon, so a quad with a repeated corner (a triangle) still has a winding.
    n = [0.0, 0.0, 0.0]
    for i in range(4):
        a, b = p[i], p[(i + 1) % 4]
        n[0] += (a[1] - b[1]) * (a[2] + b[2])
        n[1] += (a[2] - b[2]) * (a[0] + b[0])
        n[2] += (a[0] - b[0]) * (a[1] + b[1])
    if sum(n[i] * normal[i] for i in range(3)) < 0:
        pts = [pts[0]] + pts[1:][::-1]
    for _, (u, v) in pts:
        if not (-1e-6 <= u <= 1 + 1e-6 and -1e-6 <= v <= 1 + 1e-6):
            raise ValueError(f"rotor quad UV {u}, {v} is outside the texture ({texture})")
    return {"texture": texture, "normal": [round(c, 4) for c in normal],
            "vertices": [[round(x, 4), round(y, 4), round(z, 4), round(min(1.0, max(0.0, u)), 5), round(min(1.0, max(0.0, v)), 5)]
                         for (x, y, z), (u, v) in pts]}


def _at(axis, a, u, v):
    """A point with `a` along the axis and (u, v) on the other two axes (x, y, z order)."""
    if axis == "x":
        return (a, u, v)
    if axis == "y":
        return (u, a, v)
    return (u, v, a)


def _axis_vector(axis, sign):
    return tuple(float(sign) if axis == k else 0.0 for k in "xyz")


def prism(axis, a0, a1, polygon, side, cap, cap_box=None):
    """A closed prism along `axis` from a0 to a1 over a convex polygon given as (u, v) points (u, v: the other two
    axes in x, y, z order), counter-clockwise or clockwise. Each side face is mapped one texel per pixel from the
    texture's corner; the caps map the cap texture once across cap_box (u0, v0, u1, v1), by default the polygon's
    bounding square, so a round "face" texture (a pulley's spokes, a flange's bolts) sits on the disc."""
    if a1 - a0 > 16:
        raise ValueError("prism longer than a texture tile")
    n = len(polygon)
    cu = sum(p[0] for p in polygon) / n
    cv = sum(p[1] for p in polygon) / n
    if cap_box is None:
        half = max(max(abs(p[0] - cu), abs(p[1] - cv)) for p in polygon)
        cap_box = (cu - half, cv - half, cu + half, cv + half)
    bu0, bv0, bu1, bv1 = cap_box

    def cap_uv(u, v):
        return (u - bu0) / (bu1 - bu0), 1 - (v - bv0) / (bv1 - bv0)

    out = []
    for a, sign in ((a0, -1), (a1, 1)):
        normal = _axis_vector(axis, sign)
        # A fan from the centre, two polygon edges per quad (an odd last edge as a quad with a repeated corner).
        for i in range(0, n, 2):
            ring = [polygon[i], polygon[i + 1], polygon[(i + 2) % n]] if i + 1 < n else [polygon[i], polygon[0], polygon[0]]
            corners = [(cu, cv)] + ring
            out.append(quad([_at(axis, a, u, v) for u, v in corners], [cap_uv(u, v) for u, v in corners], cap, normal))
    length = a1 - a0
    for i in range(n):
        (ua, va), (ub, vb) = polygon[i], polygon[(i + 1) % n]
        width = math.hypot(ub - ua, vb - va)
        if width > 16:
            raise ValueError("prism side wider than a texture tile")
        # Outward normal of this side in the (u, v) plane.
        nu, nv = vb - va, -(ub - ua)
        mu, mv = (ua + ub) / 2 - cu, (va + vb) / 2 - cv
        if nu * mu + nv * mv < 0:
            nu, nv = -nu, -nv
        k = math.hypot(nu, nv)
        normal = _at(axis, 0.0, nu / k, nv / k)
        pts = [_at(axis, a0, ua, va), _at(axis, a1, ua, va), _at(axis, a1, ub, vb), _at(axis, a0, ub, vb)]
        uvs = [(0, 0), (0, length / 16), (width / 16, length / 16), (width / 16, 0)]
        out.append(quad(pts, uvs, side, normal))
    return out


def ngon(cu, cv, r, sides, phase=0.0):
    """A regular polygon round (cu, cv) with its corners on radius r."""
    return [(cu + r * math.cos(phase + 2 * math.pi * k / sides), cv + r * math.sin(phase + 2 * math.pi * k / sides))
            for k in range(sides)]


def disc(axis, a0, a1, cu, cv, r, side, cap, sides=12):
    """A round disc or shaft: a regular prism whose flats sit at r (its corners a little further out)."""
    corner = r / math.cos(math.pi / sides)
    return prism(axis, a0, a1, ngon(cu, cv, corner, sides, math.pi / sides), side, cap,
                 (cu - corner, cv - corner, cu + corner, cv + corner))


def saw_blade(x0, x1, cy, cz, r_gullet, r_tip, teeth, face, edge, tip_at=0.8):
    """A circular saw blade in the y-z plane between x0 and x1: `teeth` ratchet teeth, each a gentle back rising from
    its gullet to the tip at tip_at of the pitch and a steep face dropping to the next gullet. Seen turning the way
    a negative speed turns it about +x (the front teeth moving down, into the log), the steep faces lead. The two faces
    map the `face` texture once across the disc; the rim and tooth faces sample one ground-steel row of `edge`, one
    texel per pixel, so they never shimmer as the blade turns."""
    pitch = 2 * math.pi / teeth

    def yz(r, a):
        return cy + r * math.sin(a), cz + r * math.cos(a)

    def face_uv(y, z):
        return (z - (cz - r_tip)) / (2 * r_tip), 1 - (y - (cy - r_tip)) / (2 * r_tip)

    out = []
    thickness = x1 - x0
    for k in range(teeth):
        a = k * pitch
        g0, tip, g1 = yz(r_gullet, a), yz(r_tip, a + tip_at * pitch), yz(r_gullet, a + pitch)
        for x, sign in ((x0, -1), (x1, 1)):
            pts = [(x, cy, cz), (x, *g0), (x, *tip), (x, *g1)]
            out.append(quad(pts, [face_uv(p[1], p[2]) for p in pts], face, (float(sign), 0.0, 0.0)))
        for (ya, za), (yb, zb) in ((g0, tip), (tip, g1)):
            length = math.hypot(yb - ya, zb - za)
            ny, nz = zb - za, -(yb - ya)
            my, mz = (ya + yb) / 2 - cy, (za + zb) / 2 - cz
            if ny * my + nz * mz < 0:
                ny, nz = -ny, -nz
            ny, nz = ny / length, nz / length
            pts = [(x0, ya, za), (x1, ya, za), (x1, yb, zb), (x0, yb, zb)]
            out.append(quad(pts, [(0, 0), (0, thickness / 16), (length / 16, thickness / 16), (length / 16, 0)], edge,
                            (0.0, ny, nz)))
    return out


def turned(quads, axis, degrees, origin):
    """The quads turned about an axis through `origin` (right-handed, like a block model element's rotation)."""
    i = "xyz".index(axis)
    j, k = [m for m in range(3) if m != i]
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))

    def rot(p, o):
        d = [p[m] - o[m] for m in range(3)]
        out = list(d)
        out[j] = d[j] * c - d[k] * s
        out[k] = d[j] * s + d[k] * c
        return [out[m] + o[m] for m in range(3)]

    result = []
    for q in quads:
        verts = [rot(v[:3], origin) + v[3:] for v in q["vertices"]]
        normal = rot(q["normal"], (0, 0, 0))
        result.append({"texture": q["texture"], "normal": [round(v, 4) for v in normal],
                       "vertices": [[round(v[0], 4), round(v[1], 4), round(v[2], 4), v[3], v[4]] for v in verts]})
    return result


def turn_point(point, axis, degrees, origin):
    return turned([{"texture": "", "normal": [0, 0, 0], "vertices": [list(point) + [0, 0]] * 4}], axis, degrees, origin)[0][
        "vertices"][0][:3]


def export():
    """The rotor table for machine_rotor_quads.json (see the module docstring)."""
    import steampunk_models  # noqa: F401  (loads giant_models after the helpers it builds on)
    import giant_models
    out = {}
    for name, rotor in giant_models.ROTORS.items():
        out[name] = {
            "block": rotor["block"], "axis": rotor["axis"], "center": [round(v, 4) for v in rotor["center"]],
            "property": rotor["property"], "speed": rotor["speed"], "ease": rotor.get("ease", 10),
            "always": rotor.get("always", False), "when": dict(rotor.get("when", {})), "quads": rotor["quads"],
        }
    return out
