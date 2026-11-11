"""The plant's doors, reworked (doors round): real 3D doors in real frames that swing, with closers, levers or panic
bars, and badge readers by the secured doors.

  * steel_door: a hollow metal door in a pressed steel frame (the frame lines the whole depth of the wall, with a
    face round the opening on both sides of the wall); a narrow wired-glass vision lite; butt hinges; lever handles
    on both faces, a stainless kick plate on the push side; a surface closer on the push side (a parallel arm to a
    shoe under the frame's head). Exit hardware: a rim panic bar on the push side instead of the lever.
  * glass_door: a narrow stile storefront door in dark bronze, its leaf in the framed glass's plane (the same 3 px
    the framed glass stands in, so they still line up); a push bar on the push side and an offset pull on the other;
    its closer is in the head (nothing shows). Exit hardware: a panic bar across the glass.
  * lead_door: the lead-lined door, thicker, with its painted faces (the radiation sign), heavy hinges, levers and a
    closer.

Every door here is drawn in the vanilla door's frame of reference: facing east (the way you walk in when you push
it), so the wall runs along z and the leaf closes against the west side of the block (x 0 is the face you push from,
the approach side; x 16 the side it swings into). hinge 'left' has the hinges at z 0; 'right' is mirrored. y runs
0..32 over both halves; the block models take their half (the upper half's y 16..32 moved down).

A door's states (nuclear/PlantDoorBlock.java): open, moving (swinging: the block model shows only the frame, the
leaf is drawn by client/PlantDoorRenderer.java turning about its hinge from the door's part models), paired (a
double door's leaf: no jamb or stop at its meeting edge), exit (panic hardware), reader (a badge reader on the wall
beside it, its light red while shut and green while open).
"""
import math

from PIL import Image, ImageDraw
import numpy as np

import nuclear_materials as M

MOD = "jugcraft"
DOORS = ("steel_door", "glass_door", "lead_door")
FACING_Y = {"east": 0, "south": 90, "west": 180, "north": 270}
# How long a swing takes (ticks): open, and shut (by hand or by the closer: the closer brings it in slower).
SWING_OPEN, SWING_SHUT = 10, 16


class Spec:
    """A kind of door: jw jamb face width (z), hw head depth (y), lx0 the leaf's push face (x), lt its thickness, sd
    the stop's depth (the stop stands on the frame from lx1 to lx1 + sd; the hinge pin is at its edge), frame:
    the frame lines the whole depth of the wall with a face (casing) proud of both wall faces; closer: a surface
    closer on the push side."""

    def __init__(self, key, jw, hw, lx0, lt, sd, frame, closer, lite, depth=5.0):
        self.key, self.jw, self.hw, self.lx0, self.lt, self.sd = key, jw, hw, lx0, lt, sd
        self.depth = depth
        self.lx1 = lx0 + lt
        self.frame, self.closer, self.lite = frame, closer, lite
        self.top = 32 - hw           # under side of the head

    def pivot(self):
        """The hinge pin (hinge left): at the pull face's side of the stop, by the hinge jamb."""
        return (self.lx1 + self.sd, self.jw + GAP)

    def leaf_z(self, paired):
        """The leaf's edges across the opening (hinge left), shut."""
        z0 = self.jw + GAP
        z1 = 16 - 0.05 if paired else 16 - self.jw - GAP
        return z0, z1


GAP = 0.1
SPECS = {
    "steel_door": Spec("steel_door", jw=0.8, hw=0.8, lx0=1.2, lt=1.75, sd=0.45, frame=True, closer=True, lite=True),
    "glass_door": Spec("glass_door", jw=1.0, hw=1.4, lx0=0.6, lt=1.8, sd=0.6, frame=False, closer=False, lite=False),
    "lead_door": Spec("lead_door", jw=1.0, hw=1.0, lx0=0.5, lt=2.4, sd=0.5, frame=True, closer=True, lite=False,
                      depth=5.5),
}

# Textures (tools/plant_doors.py makers): the paints and the hardware.
LEAF, FRAME, STAINLESS, CHROME, CLOSER, BLACK = "pd_leaf", "pd_frame", "pd_stainless", "pi_chrome", "pd_closer", "pd_black"
WIRED, READER, LED = "pd_wired_glass", "pd_reader", {False: "pd_led_red", True: "pd_led_green"}
BRONZE, GLASS, BAR = "fg_frame", "fg_glass", "fg_bar"
TRANSLUCENT = {WIRED, GLASS}
GLOW = {"pd_led_red", "pd_led_green"}


def B(x0, y0, z0, x1, y1, z1, tex, **extra):
    """A box: corners in door px (y 0..32), its faces' textures (a name, or {face: name, '*': name}; a name may be
    (name, uv) for a region), extra element fields (rotation)."""
    out = [round(x0, 4), round(y0, 4), round(z0, 4), round(x1, 4), round(y1, 4), round(z1, 4), tex]
    if extra:
        out.append(extra)
    return out


def faces_of(tex):
    spec = tex if isinstance(tex, dict) else {"*": tex}
    return {f: spec.get(f, spec.get("*")) for f in ("north", "south", "east", "west", "up", "down")
            if spec.get(f, spec.get("*")) is not None}


# ---------------------------------------------------------------------------------------------------- the frame

def frame_boxes(kind, paired, reader, lit):
    """The door's frame (hinge left), standing still while the leaf swings: jambs, head, stops, hinge knuckles,
    casings round the opening on both wall faces; the badge reader (approach side) if reader, lit green if lit."""
    s = SPECS[kind]
    out = []
    jw, top = s.jw, s.top
    latch = not paired
    ze = 16 - jw if latch else 16                     # the head's end on the latch side
    if kind == "glass_door":
        # The storefront frame, in the leaf's plane (x 0..3): jambs, head, and its stop (the frame's own lip).
        out.append(B(0, 0, 0, 3, 32, jw, BRONZE))
        if latch:
            out.append(B(0, 0, 16 - jw, 3, 32, 16, BRONZE))
        out.append(B(0, top, jw, 3, 32, ze, BRONZE))
        out.append(B(s.lx1, 0, jw, s.lx1 + s.sd, top, jw + 0.4, BRONZE))
        if latch:
            out.append(B(s.lx1, 0, 16 - jw - 0.4, s.lx1 + s.sd, top, 16 - jw, BRONZE))
        out.append(B(s.lx1, top - 0.4, jw + 0.4, s.lx1 + s.sd, top, ze - (0.4 if latch else 0), BRONZE))
        # The offset pivots: a plate at the head and the floor at the hinge.
        px, pz = s.pivot()
        out.append(B(px - 0.6, top - 0.25, pz - 0.05, px + 0.3, top, pz + 1.1, BRONZE))
        out.append(B(px - 0.6, 0, pz - 0.05, px + 0.3, 0.25, pz + 1.1, BRONZE))
        if reader:
            out += reader_boxes(paired, lit)
        return out
    # Jambs and head: the frame's soffit, the frame's depth into the wall (the rest of the opening is the wall's own
    # reveal: the walls are thick).
    fd = s.depth
    out.append(B(0, 0, 0, fd, 32, jw, FRAME))
    if latch:
        out.append(B(0, 0, 16 - jw, fd, 32, 16, FRAME))
    out.append(B(0, top, jw, fd, 32, ze, FRAME))
    # Stops: the lip the leaf shuts against (on the side it swings to).
    x0, x1 = s.lx1, s.lx1 + s.sd
    out.append(B(x0, 0, jw, x1, top - 0.45, jw + 0.45, FRAME))
    if latch:
        out.append(B(x0, 0, 16 - jw - 0.45, x1, top - 0.45, 16 - jw, FRAME))
    out.append(B(x0, top - 0.45, jw, x1, top, ze, FRAME))
    # The frame's face (casing), proud of the wall round the opening on the side you come from.
    cw, ct = 0.7, 0.3
    xa, xb = -ct, 0
    out.append(B(xa, 0, -cw, xb, 32 + cw, jw, FRAME))
    if latch:
        out.append(B(xa, 0, 16 - jw, xb, 32 + cw, 16 + cw, FRAME))
    out.append(B(xa, top, jw, xb, 32 + cw, ze, FRAME))
    # Hinge knuckles at the pin (three; the lead door's heavier).
    px, pz = s.pivot()
    r = 0.25 if kind == "steel_door" else 0.3
    for y in ((3.0, 15.0, 26.6) if kind == "steel_door" else (2.5, 13.5, 25.5)):
        h = 2.2 if kind == "steel_door" else 3.0
        out.append(B(px - r, y, pz - r, px + r, y + h, pz + r, CHROME if kind == "steel_door" else "nu_steel"))
    if reader:
        out += reader_boxes(paired, lit)
    return out


def reader_boxes(paired, lit):
    """A badge reader on the wall of the approach side, by the latch jamb (a pair's: by its hinge jamb, outside the
    pair): a slim black mullion reader, its light at the top."""
    z0 = 16 + 0.7 + 0.35 if not paired else -0.7 - 0.35 - 0.95
    z1 = z0 + 0.95
    out = [B(-0.55, 15.4, z0, 0, 18.4, z1, {"*": BLACK, "west": READER + "!"})]
    out.append(B(-0.62, 17.75, z0 + 0.3, -0.55, 18.05, z1 - 0.3, LED[lit]))
    return out


# ----------------------------------------------------------------------------------------------------- the leaf

def leaf_boxes(kind, paired, exit_, hinge="left"):
    """The leaf shut (hinge left; the hardware on it), y 0..32: what swings."""
    s = SPECS[kind]
    z0, z1 = s.leaf_z(paired)
    a, b = s.lx0, s.lx1
    yb, yt = 0.3, s.top - 0.1
    out = []
    if kind == "glass_door":
        out += _glass_leaf(s, z0, z1, yb, yt, paired, exit_)
        return out
    face = LEAF if kind == "steel_door" else None
    if kind == "lead_door":
        out += _lead_leaf(s, z0, z1, yb, yt)
    elif s.lite:
        # The body round the vision lite (cut through it), the wired glass in it, glazing beads round it both sides.
        zc = z1 - 4.2
        lz0, lz1, ly0, ly1 = zc - 1.4, zc + 1.4, 18.8, 27.6
        out.append(B(a, yb, z0, b, ly0, z1, face))
        out.append(B(a, ly1, z0, b, yt, z1, face))
        out.append(B(a, ly0, z0, b, ly1, lz0, face))
        out.append(B(a, ly0, lz1, b, ly1, z1, face))
        gx = (a + b) / 2
        out.append(B(gx - 0.08, ly0 - 0.3, lz0 - 0.3, gx + 0.08, ly1 + 0.3, lz1 + 0.3, {"west": WIRED, "east": WIRED}))
        for xa, xb in ((a - 0.18, a), (b, b + 0.18)):
            out.append(B(xa, ly1, lz0 - 0.35, xb, ly1 + 0.35, lz1 + 0.35, FRAME))
            out.append(B(xa, ly0 - 0.35, lz0 - 0.35, xb, ly0, lz1 + 0.35, FRAME))
            out.append(B(xa, ly0, lz0 - 0.35, xb, ly1, lz0, FRAME))
            out.append(B(xa, ly0, lz1, xb, ly1, lz1 + 0.35, FRAME))
    if kind == "steel_door":
        # Kick plate on the push side.
        # (On the active leaf of a pair it stops short of the astragal, which lies over the meeting edge.)
        kz1 = z1 - 0.5 if paired and hinge == "left" else z1 - 0.15
        out.append(B(a - 0.1, 0.5, z0 + 0.15, a, 4.5, kz1, STAINLESS))
    if paired and hinge == "left":
        # The astragal on the active leaf: a strip over the meeting edge on the push side.
        out.append(B(a - 0.25, yb, z1 - 0.35, a, yt, z1 + 0.3, FRAME))
    c = z1 - 1.3
    if exit_ or paired:
        out += panic_bar(a, z0, z1)
    else:
        out += lever(a, c, -1, key=True)
    if paired:
        out += pull_handle(b, z1 - 1.5, 1)
    else:
        out += lever(b, c, 1)
    if s.closer:
        out += closer_body(s, z0)
    return out


def lever(x, c, side, key=False):
    """A lever handle on the face at x (side -1: the west face, +1 the east), its spindle at z c, a metre up: a narrow
    escutcheon plate, the spindle, the lever pointing back toward the hinge with a short return to the door."""
    def X(p, q):
        u, v = x + side * p, x + side * q
        return min(u, v), max(u, v)
    out = []
    xa, xb = X(0, 0.2)
    out.append(B(xa, 14.4, c - 0.45, xb, 18.2, c + 0.45, CHROME))
    xa, xb = X(0.2, 0.8)
    out.append(B(xa, 16.0, c - 0.2, xb, 16.4, c + 0.2, CHROME))
    xa, xb = X(0.8, 1.2)
    out.append(B(xa, 15.9, c - 2.4, xb, 16.5, c + 0.3, CHROME))
    xa, xb = X(0.45, 0.8)
    out.append(B(xa, 15.95, c - 2.4, xb, 16.45, c - 2.0, CHROME))
    if key:
        xa, xb = X(0.2, 0.32)
        out.append(B(xa, 14.75, c - 0.22, xb, 15.2, c + 0.22, BLACK))
    return out


def panic_bar(a, z0, z1):
    """A rim exit device on the push face (x < a): the latch mechanism's case by the latch edge, the touch bar on its
    housing back to an end cap by the hinge."""
    out = []
    out.append(B(a - 1.5, 14.6, z1 - 3.4, a, 17.4, z1 - 0.4, STAINLESS))
    out.append(B(a - 0.9, 15.2, z0 + 1.8, a, 16.9, z1 - 3.4, STAINLESS))
    out.append(B(a - 1.45, 15.45, z0 + 2.3, a - 0.9, 16.65, z1 - 3.4, {"*": STAINLESS, "west": BLACK}))
    out.append(B(a - 1.5, 15.0, z0 + 1.2, a, 17.1, z0 + 1.8, STAINLESS))
    return out


def pull_handle(b, c, side):
    """An offset pull (on a pair's other face): two stand-off posts and the grip."""
    out = []
    for y in (13.3, 20.3):
        out.append(B(b, y, c - 0.2, b + 1.0, y + 0.4, c + 0.2, CHROME))
    out.append(B(b + 1.0, 12.8, c - 0.25, b + 1.45, 21.2, c + 0.25, CHROME))
    return out


def closer_body(s, z0):
    """The closer's body on the push face at the top of the leaf by the hinge, and its spindle (the arm's pivot)."""
    a = s.lx0
    sx, sz = spindle(s)
    out = [B(a - 1.0, s.top - 2.8, z0 + 1.0, a, s.top - 1.4, z0 + 6.6, CLOSER)]
    out.append(B(sx - 0.2, s.top - 1.4, sz - 0.2, sx + 0.2, s.top - 1.05, sz + 0.2, BLACK))
    return out


# The closer's arm: a main arm from the spindle and a forearm to the shoe under the head; shut, the arm folds flat
# along the door (the shoe the difference of the two lengths from the spindle).
ARM_MAIN, ARM_FORE = 7.8, 3.8


def spindle(s):
    z0 = s.jw + GAP
    return s.lx0 - 0.5, z0 + 5.9


def shoe(s):
    sx, sz = spindle(s)
    return sx, sz + ARM_MAIN - ARM_FORE


def turn(p, pivot, deg):
    """A point (x, z) turned deg about the pivot as Minecraft turns about y (+90: z to x)."""
    r = math.radians(deg)
    dx, dz = p[0] - pivot[0], p[1] - pivot[1]
    return (pivot[0] + dx * math.cos(r) + dz * math.sin(r), pivot[1] - dx * math.sin(r) + dz * math.cos(r))


def elbow(s, deg):
    """The arm's elbow with the leaf open deg: where the arm (from the spindle, turned with the leaf) and the forearm
    (from the shoe) meet, on the latch side of the line between them."""
    S = turn(spindle(s), s.pivot(), deg)
    F = shoe(s)
    dx, dz = F[0] - S[0], F[1] - S[1]
    d = math.hypot(dx, dz)
    d = min(d, ARM_MAIN + ARM_FORE - 1e-6)
    a = (ARM_MAIN ** 2 - ARM_FORE ** 2 + d * d) / (2 * d)
    h = math.sqrt(max(0.0, ARM_MAIN ** 2 - a * a))
    mx, mz = S[0] + a * dx / d, S[1] + a * dz / d
    # The two meeting points either side of the line S..F: the one further to +z (the latch side).
    e1 = (mx + h * dz / d, mz - h * dx / d)
    e2 = (mx - h * dz / d, mz + h * dx / d)
    return S, (e1 if e1[1] > e2[1] else e2), F


def arm_boxes(s, deg):
    """The closer's arm with the leaf open deg: two flat bars (each a box along z from its pivot, turned about y to
    point at the other end) and the shoe (still, under the head)."""
    S, E, F = elbow(s, deg)
    y0 = s.top - 1.05
    out = []
    for (P, Q, ya, yb) in ((S, E, y0, y0 + 0.3), (E, F, y0 + 0.32, y0 + 0.6)):
        L = math.hypot(Q[0] - P[0], Q[1] - P[1])
        ang = math.degrees(math.atan2(Q[0] - P[0], Q[1] - P[1]))      # from +z toward +x
        box = B(P[0] - 0.2, ya, P[1] - 0.2, P[0] + 0.2, yb, P[1] + L + 0.2, CLOSER)
        if abs(ang) > 1e-3:
            box.append({"rotation": {"origin": [round(P[0], 4), round(ya, 4), round(P[1], 4)], "axis": "y",
                                     "angle": round(ang, 3)}})
        out.append(box)
    out.append(B(F[0] - 0.35, y0 + 0.6, F[1] - 0.5, F[0] + 0.35, s.top, F[1] + 0.5, CLOSER))
    return out


def _glass_leaf(s, z0, z1, yb, yt, paired, exit_):
    """A narrow stile storefront leaf: stiles, top rail, a deep bottom rail, the glass; the push bar (or a panic bar)
    on the push side, the offset pull on the other."""
    a, b = s.lx0, s.lx1
    st, tr, br = 1.6, 1.6, 4.0
    out = [B(a, yb, z0, b, yt, z0 + st, BRONZE), B(a, yb, z1 - st, b, yt, z1, BRONZE),
           B(a, yb, z0 + st, b, yb + br, z1 - st, BRONZE), B(a, yt - tr, z0 + st, b, yt, z1 - st, BRONZE)]
    gx = (a + b) / 2
    out.append(B(gx - 0.1, yb + br - 0.3, z0 + st - 0.3, gx + 0.1, yt - tr + 0.3, z1 - st + 0.3,
                 {"west": GLASS + "!", "east": GLASS + "!"}))
    if exit_:
        out += panic_bar(a, z0, z1)
    else:
        # The push bar: across the glass on stand-offs.
        out.append(B(a - 0.95, 15.3, z0 + 1.0, a - 0.45, 16.3, z1 - 1.0, BAR))
        for z in (z0 + 1.6, z1 - 2.4):
            out.append(B(a - 0.45, 15.55, z, a, 16.05, z + 0.8, BAR))
    out += pull_handle(b, z1 - 1.0, 1) if not paired else pull_handle(b, z1 - 1.2, 1)
    return out


def _lead_leaf(s, z0, z1, yb, yt):
    """The lead-lined leaf: its painted faces (the radiation sign outside), a lead edge."""
    a, b = s.lx0, s.lx1
    out = []
    for (ya, yc, half) in ((yb, 16, "bottom"), (16, yt, "top")):
        # The picture over the leaf's face (its half), whatever the leaf's width.
        v0, v1 = (16 - (yc - (16 if half == "top" else 0)), 16 - (ya - (16 if half == "top" else 0)))
        out.append(B(a, ya, z0, b, yc, z1, {"*": "lead_door_edge",
                                            "west": (f"lead_door_outside_{half}", [0, v0, 16, v1]),
                                            "east": (f"lead_door_inside_{half}", [0, v0, 16, v1])}))
    return out


# ------------------------------------------------------------------------------------------ turning and halves

TURN_FACES = {90: {"west": "south", "south": "east", "east": "north", "north": "west"},
              -90: {"west": "north", "north": "east", "east": "south", "south": "west"}}


def _uv_tex(box):
    """The box's faces as (texture, uv) taken from where they are now (so a turned or moved copy shows the same).
    A box in the upper half (y 16..32) takes its pictures as the upper block would."""
    x0, y0, z0, x1, y1, z1 = box[:6]
    dy = 16 if y0 >= 16 - 1e-6 and y1 > 16 else 0
    out = {}
    for f, t in faces_of(box[6]).items():
        if isinstance(t, tuple):
            out[f] = t
            continue
        name = t.rstrip("!")
        uv = [0, 0, 16, 16] if t.endswith("!") else auto_uv(f, x0, y0 - dy, z0, x1, y1 - dy, z1)
        out[f] = (name, uv)
    return out


def cut16(boxes, keep=()):
    """Boxes across y 16 cut in two there (the faces at the cut dropped), so each piece takes its picture from its
    own block; boxes in keep (indices) stay whole."""
    out, kept = [], set()
    for i, b in enumerate(boxes):
        y0, y1 = b[1], b[4]
        if i in keep or y1 <= 16 or y0 >= 16:
            if i in keep:
                kept.add(len(out))
            out.append(list(b))
            continue
        spec = faces_of(b[6])
        lo = {f: t for f, t in spec.items() if f != "up"}
        up = {f: t for f, t in spec.items() if f != "down"}
        out.append([b[0], y0, b[2], b[3], 16, b[5], lo] + b[7:])
        out.append([b[0], 16, b[2], b[3], y1, b[5], up] + b[7:])
    return out, kept


def auto_uv(face, x0, y0, z0, x1, y1, z1):
    """The texture area a face takes from its position (vanilla's), kept inside the texture."""
    if face in ("north", "south"):
        u = (16 - x1, 16 - x0) if face == "north" else (x0, x1)
        v = (16 - y1, 16 - y0)
    elif face in ("east", "west"):
        u = (16 - z1, 16 - z0) if face == "east" else (z0, z1)
        v = (16 - y1, 16 - y0)
    else:
        u, v = (x0, x1), ((z0, z1) if face == "up" else (16 - z1, 16 - z0))

    def fit(p, q):
        span = min(16, q - p)
        p = min(max(p, 0), 16 - span)
        return p, p + span
    u, v = fit(*u), fit(*v)
    return [round(u[0], 3), round(v[0], 3), round(u[1], 3), round(v[1], 3)]


def fixed(boxes):
    """The boxes with their faces' pictures pinned (texture, uv) where they are."""
    return [b[:6] + [_uv_tex(b)] + b[7:] for b in boxes]


def turned90(boxes, pivot, sign):
    """The boxes turned exactly 90 degrees (sign +1: z to x, as the leaf of a left-hinged door opens) about the
    pivot (x, z): new boxes, faces renamed, their pictures kept."""
    out = []
    for b in fixed(boxes):
        x0, y0, z0, x1, y1, z1 = b[:6]
        pts = [turn((x, z), pivot, 90 * sign) for x in (x0, x1) for z in (z0, z1)]
        xs, zs = [p[0] for p in pts], [p[1] for p in pts]
        names = TURN_FACES[90 * sign]
        faces = {names.get(f, f): t for f, t in b[6].items()}
        nb = [min(xs), y0, min(zs), max(xs), y1, max(zs), faces] + b[7:]
        if len(nb) > 7 and "rotation" in nb[7]:
            r = nb[7]["rotation"]
            o = turn((r["origin"][0], r["origin"][2]), pivot, 90 * sign)
            nb[7] = {"rotation": dict(r, origin=[round(o[0], 4), r["origin"][1], round(o[1], 4)])}
        out.append([round(v, 4) for v in nb[:6]] + nb[6:])
    return out


def swung(boxes, pivot, deg):
    """The boxes as they stand with the leaf open deg (any angle; for the mock-ups): each turned about the pivot by
    an element rotation (Minecraft's rotation about y; +: z toward x)."""
    out = []
    for b in fixed(boxes):
        nb = list(b[:7])
        nb.append({"rotation": {"origin": [round(pivot[0], 4), 0, round(pivot[1], 4)], "axis": "y", "angle": round(deg, 3)}})
        out.append(nb)
    return out


def mirrored(boxes):
    """Hinge right: mirrored in z (16 - z); north and south faces swap, pictures keep reading the right way round."""
    out = []
    for b in fixed(boxes):
        x0, y0, z0, x1, y1, z1 = b[:6]
        faces = {}
        for f, t in b[6].items():
            f2 = {"north": "south", "south": "north"}.get(f, f)
            faces[f2] = t
        nb = [x0, y0, round(16 - z1, 4), x1, y1, round(16 - z0, 4), faces] + [dict(e) for e in b[7:]]
        if len(nb) > 7 and "rotation" in nb[7]:
            r = dict(nb[7]["rotation"])
            r["origin"] = [r["origin"][0], r["origin"][1], round(16 - r["origin"][2], 4)]
            r["angle"] = -r["angle"]
            nb[7] = {"rotation": r}
        out.append(nb)
    return out


def halves(boxes, keep_low=()):
    """The boxes (cut at y 16 already) into the lower and upper half models (the upper's moved down 16); boxes in
    keep_low (indices) go whole into the lower one."""
    lo, up = [], []
    for i, b in enumerate(boxes):
        if i in keep_low or b[4] <= 16 + 1e-6:
            lo.append(b)
        else:
            up.append(_moved(b, -16))
    return lo, up


def _moved(b, dy):
    nb = [b[0], round(b[1] + dy, 4), b[2], b[3], round(b[4] + dy, 4), b[5], b[6]] + [dict(e) for e in b[7:]]
    if len(nb) > 7 and "rotation" in nb[7]:
        r = dict(nb[7]["rotation"])
        r["origin"] = [r["origin"][0], round(r["origin"][1] + dy, 4), r["origin"][2]]
        nb[7] = {"rotation": r}
    return nb


# ------------------------------------------------------------------------------------------- whole door states

def door_boxes(kind, hinge, open_, moving, paired, exit_, reader):
    """Everything in a door's block models (both halves, y 0..32, cut at 16, pictures pinned): (boxes, indices kept
    whole in the lower half: the reader)."""
    s = SPECS[kind]
    # A pair's reader is drawn by its first leaf only (medium_plant's: hinge right), outside the pair.
    reader = reader and (not paired or hinge == "right")
    frame = frame_boxes(kind, paired, reader, open_)
    n_reader = 2 if reader else 0
    frame, keep = cut16(frame, set(range(len(frame) - n_reader, len(frame))))
    boxes = fixed(frame)
    if not moving:
        leaf = fixed(cut16(leaf_boxes(kind, paired, exit_, hinge))[0])
        arm = fixed(arm_boxes(s, 90 if open_ else 0)) if s.closer else []
        if open_:
            leaf = turned90(leaf, s.pivot(), 1)
        boxes += leaf + arm
    elif s.closer:
        boxes += fixed(arm_boxes(s, 0)[-1:])          # the shoe stays under the head while the arm swings
    if hinge == "right":
        boxes = mirrored(boxes)
    return boxes, keep


def leaf_part(kind, hinge, paired, exit_):
    """The leaf shut with its hardware (y 0..32, cut at 16): the part client/PlantDoorRenderer.java turns about the
    hinge."""
    boxes = fixed(cut16(leaf_boxes(kind, paired, exit_, hinge))[0])
    return mirrored(boxes) if hinge == "right" else boxes


# ---------------------------------------------------------------------------------------------------- textures

S = 64


def _leaf_paint():
    return M.painted((150, 158, 164), 4101, chip_count=0, drip_count=0, dirt=0.08, size=S)


def _frame_paint():
    return M.painted((112, 118, 124), 4102, chip_count=0, drip_count=0, dirt=0.1, size=S)


def _stainless():
    return M.brushed((182, 186, 190), 4103, "h", dirt=0.05, size=S)


def _closer():
    return M.brushed((160, 162, 160), 4104, "h", dirt=0.05, size=S)


def _black():
    return M.to_img(M.base((34, 35, 37), S))


def _wired():
    """Wired glass: a faint grey-green tint and the wire's square mesh, a 1/2 px square (translucent; mapped by
    position, so the mesh stays square whatever the lite's size)."""
    n = 128
    out = np.zeros((n, n, 4), np.uint8)
    out[..., :3] = (198, 216, 212)
    out[..., 3] = 46
    for k in range(0, n, 4):
        out[k, :, :3] = (104, 110, 110)
        out[k, :, 3] = 120
        out[:, k, :3] = (104, 110, 110)
        out[:, k, 3] = 120
    return Image.fromarray(out, "RGBA")


def _reader():
    """The badge reader's face: black, the contactless mark, a slot for the light at the top."""
    img = Image.new("RGBA", (S, S), (30, 31, 33, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, S - 3, S - 3], outline=(52, 54, 58, 255), width=2)
    cx, cy = S // 2, int(S * 0.58)
    for r in (6, 11, 16):
        d.arc([cx - r, cy - r, cx + r, cy + r], 300, 60, fill=(120, 124, 130, 255), width=2)
    d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(120, 124, 130, 255))
    return img


def _led(colour):
    return M.to_img(M.base(colour, 16))


def makers():
    return {"pd_leaf": _leaf_paint, "pd_frame": _frame_paint, "pd_stainless": _stainless, "pd_closer": _closer,
            "pd_black": _black, "pd_wired_glass": _wired, "pd_reader": _reader,
            "pd_led_red": lambda: _led((236, 44, 36)), "pd_led_green": lambda: _led((60, 230, 90))}


# ------------------------------------------------------------------------------------------------ the roller door

# The roller door's parts that stand still (nuclear/RollerDoorBlock.java: a wall of blocks in an opening; its states
# say where in the door a block is: pos bottom / middle / top / single (its column one block high), left / right
# ends): drawn facing north like the door's own models (the curtain at z 6.5..9.5, the coil and its barrel on the
# north side: inside). The guides (steel channels the curtain's edges run in) up both ends of the opening, the bottom
# bar on the curtain's edge (at the foot shut, at the head open; client/RollerDoorRenderer.java draws it moving), the
# barrel's end brackets at the ends, the motor operator on the left end's bracket and its drive chain.
SLATS, STEEL, MOTOR = "pi_roller_slats", "nu_steel", "pd_motor"
CURTAIN_Z = (6.5, 9.5)


def roller_boxes(pos, left, right, open_, moving):
    out = []
    top = pos in ("top", "single")
    bottom = pos in ("bottom", "single")
    if not open_ and not moving:
        out.append(B(0, 0, 6.5, 16, 16, 9.5, {"*": SLATS + "!", "up": SLATS, "down": SLATS}))
    for end, on in (("left", left), ("right", right)):
        if not on:
            continue
        g = [B(0, 0, 5.6, 1.9, 16, 6.2, STEEL), B(0, 0, 9.8, 1.9, 16, 10.4, STEEL), B(0, 0, 6.2, 0.6, 16, 9.8, STEEL)]
        if top:
            # The barrel's end bracket (a plate on the wall over the guide) and, on the left, the motor.
            g.append(B(0, 9.0, 0.3, 0.7, 16, 6.2, STEEL))
            if end == "left":
                # On the wall's inside face beside the opening: the motor and its gearbox on a plate, the drive
                # chain's guard across to the barrel's end.
                g += [B(-5.2, 8.6, -0.4, -0.6, 15.2, 0, STEEL), B(-4.8, 10.4, -4.2, -1.0, 14.6, -0.4, MOTOR),
                      B(-4.4, 8.8, -3.6, -1.4, 10.4, -0.8, MOTOR), B(-5.6, 11.2, -3.8, -4.8, 13.8, -0.8, BLACK),
                      B(-1.0, 11.2, -1.6, 0.6, 13.2, 1.2, STEEL)]
        if end == "right":
            g = [_mx(b) for b in g]
        out += g
    if bottom and not open_ and not moving:
        out += [B(0, 0, 6.0, 16, 1.1, 6.5, STEEL), B(0, 0, 9.5, 16, 1.1, 10.0, STEEL)]
    if top and open_ and not moving:
        out += [B(0, 14.9, 6.0, 16, 16, 6.5, STEEL), B(0, 14.9, 9.5, 16, 16, 10.0, STEEL),
                B(0, 14.6, 6.5, 16, 14.9, 9.5, BLACK), B(0, 14.9, 6.5, 16, 16, 9.5, {"*": SLATS + "!", "down": BLACK})]
    return out


def _mx(b):
    """A box mirrored in x (16 - x), east and west faces swapped."""
    spec = faces_of(b[6])
    faces = {{"east": "west", "west": "east"}.get(f, f): t for f, t in spec.items()}
    return [round(16 - b[3], 4), b[1], b[2], round(16 - b[0], 4), b[4], b[5], faces] + b[7:]


def station_boxes():
    """The roller door's push-button station on the wall (facing north, the viewer looking north at it: the wall at
    z 0): a grey enclosure, its OPEN, CLOSE and STOP buttons (black, black, a red mushroom: nuclear/
    RollerDoorStationBlock.java BUTTONS), the conduit up out of it."""
    out = [B(5.4, 4.0, 0, 10.6, 13.0, 1.4, {"*": "pd_station", "south": "pd_station_face!"})]
    for k, (y, tex, d) in enumerate(((10.6, BLACK, 0.5), (8.1, BLACK, 0.5), (5.2, "pd_red", 0.9))):
        r = 0.9 if k < 2 else 1.25
        out.append(B(8 - r, y - r, 1.4, 8 + r, y + r, 1.4 + d, tex))
    out.append(B(7.4, 13.0, 0, 8.6, 16, 1.0, STEEL))
    return out


def roller_makers():
    def station():
        return M.painted((128, 132, 134), 4111, chip_count=0, drip_count=0, dirt=0.15, size=S)

    def station_face():
        img = M.to_img(M.base((128, 132, 134), 128))
        d = ImageDraw.Draw(img)
        for y, word in ((22, "OPEN"), (50, "CLOSE"), (84, "STOP")):
            d.rectangle([34, y - 6, 94, y + 6], fill=(230, 232, 228, 255))
            d.text((64, y), word, fill=(20, 20, 20, 255), anchor="mm")
        return img
    return {"pd_motor": lambda: M.painted((70, 92, 84), 4110, chip_count=0, drip_count=0, dirt=0.2, size=S),
            "pd_station": station, "pd_station_face": station_face, "pd_red": lambda: _led((190, 30, 28))}



# ----------------------------------------------------------------------------------------------------- writing

PART_COUNT = 36          # nuclear/PlantDoors.java PARTS
ARMS = {"steel_door": 28, "lead_door": 30}
STALLS = ("restroom_stall_door", "restroom_stall_door_accessible")
NAMES = {"roller_door_station": "Roller Door Push-Button Station"}
VERSIONS = {"steel_door": "Steel Door", "glass_door": "Glass Door"}
LANG = {"message.jugcraft.door_reader_denied": "Access denied: this door's badge reader lets in its owner and their party"}
for _door, _name in VERSIONS.items():
    LANG[f"block.jugcraft.{_door}_exit"] = f"{_name} (Panic Bar)"
    LANG[f"block.jugcraft.{_door}_reader"] = f"{_name} (Badge Reader)"
    LANG[f"block.jugcraft.{_door}_exit_reader"] = f"{_name} (Panic Bar, Badge Reader)"
CRAFTING = {"roller_door_station": (["PBP", "PRP"], {"P": "#c:plates/iron", "B": "minecraft:stone_button", "R": "minecraft:redstone"}, 1)}


def part_index(kind, hinge, paired=False, exit_=False, latched=False, accessible=False):
    """The part model of a leaf (nuclear/PlantDoors.java leafPart)."""
    h = 0 if hinge == "left" else 1
    if kind in STALLS:
        return 20 + h * 4 + int(latched) * 2 + int(accessible)
    if kind == "lead_door":
        return 16 + h * 2 + int(paired)
    if kind == "maintenance_door":
        return 32 + h * 2 + int(paired)
    return (8 if kind == "glass_door" else 0) + h * 4 + int(paired) * 2 + int(exit_)


def arm_link(kind, fore):
    """A closer arm link for the renderer: along +z from its pivot at (0, 0)."""
    s = SPECS[kind]
    y0 = s.top - 1.05
    ya, yb, length = (y0 + 0.32, y0 + 0.6, ARM_FORE) if fore else (y0, y0 + 0.3, ARM_MAIN)
    return [B(-0.2, ya, -0.2, 0.2, yb, length + 0.2, CLOSER)]


def parts():
    """Every part model, by index: [boxes]."""
    import restroom
    out = {}
    for kind in DOORS:
        for hinge in ("left", "right"):
            for paired in (False, True):
                for exit_ in ((False, True) if kind != "lead_door" else (False,)):
                    out[part_index(kind, hinge, paired, exit_)] = leaf_part(kind, hinge, paired, exit_)
    for door in STALLS:
        acc = door.endswith("accessible")
        for hinge in ("left", "right"):
            for latched in (False, True):
                lo = restroom.door_leaf(True, hinge, latched, acc)
                up = [tuple(b[:1]) + (b[1] + 16,) + tuple(b[2:4]) + (b[4] + 16,) + tuple(b[5:]) for b in restroom.door_leaf(False, hinge, latched, acc)]
                out[part_index(door, hinge, latched=latched, accessible=acc)] = [list(b) for b in lo + up]
    for kind, k in ARMS.items():
        out[k] = arm_link(kind, False)
        out[k + 1] = arm_link(kind, True)
    # the Tri-Pillar's maintenance door's leaves (tools/maintenance_door.py: 32 to 35)
    import maintenance_door
    out.update(maintenance_door.parts())
    assert sorted(out) == list(range(PART_COUNT)), sorted(out)
    return out


_FACE = {(0, 0): "west", (0, 1): "east", (1, 0): "down", (1, 1): "up", (2, 0): "north", (2, 1): "south"}


def _separate(boxes):
    """plant_interior.separate (no two faces in one plane), but a face a box does not draw (one dropped where a box
    was cut at y 16) neither moves nor moves another: the cut pieces of a hinge knuckle or an astragal meet at the
    blocks' seam with no slit between them."""
    import plant_interior as PI
    done, out = [], []
    for box in boxes:
        b = list(box[:6])
        if len(box) <= 7:
            drawn = faces_of(box[6])
            for axis in range(3):
                o1, o2 = [a for a in range(3) if a != axis]
                for side in (0, 1):
                    if _FACE[axis, side] not in drawn:
                        continue
                    k = axis + 3 * side
                    for other, odrawn in done:
                        if _FACE[axis, side] in odrawn and abs(other[k] - b[k]) < 1e-6 \
                                and min(b[o1 + 3], other[o1 + 3]) - max(b[o1], other[o1]) > 1e-6 \
                                and min(b[o2 + 3], other[o2 + 3]) - max(b[o2], other[o2]) > 1e-6:
                            moved = b[k] + (PI.NUDGE if side else -PI.NUDGE)
                            inside = 0 <= b[axis] and b[axis + 3] <= 16
                            if inside and not 0 <= moved <= 16 and b[axis + 3] - b[axis] > 2 * PI.NUDGE:
                                moved = b[k] - (PI.NUDGE if side else -PI.NUDGE)
                            b[k] = moved
                            break
            done.append((b, drawn))
        out.append(tuple(b) + tuple(box[6:]))
    return out


def _model(boxes, rid):
    """A block model of the boxes (the plant kit's writer: no two faces in one place), its glass translucent."""
    import plant_interior as PI
    import zfight
    if not boxes:
        return {"textures": {"particle": rid("block/pi_roller_slats")}, "elements": []}
    elements, used = PI._elements(_separate([tuple(b) for b in boxes]), sorted(GLOW))
    textures = {t: ("minecraft:block/" + t[3:] if t.startswith("mc_") else rid(f"block/{t}")) for t in sorted(used)}
    textures["particle"] = textures[sorted(used)[0]]
    m = zfight.fix({"textures": textures, "elements": elements})
    for t in list(m["textures"]):
        if t in TRANSLUCENT:
            m["textures"][t] = {"sprite": rid(f"block/{t}"), "force_translucent": True}
    return m


def write_assets(write, rid, assets, lang):
    """The doors' models and blockstates (steel, glass, lead-lined), the part models, the roller door's, its
    station's; their names."""
    models = assets / "models" / "block"
    seen = {}

    def put(name, model):
        key = repr(model)
        if key in seen:
            return seen[key]
        seen[key] = name
        write(models / f"{name}.json", model)
        return name

    for kind in DOORS:
        variants = {}
        for hinge in ("left", "right"):
            for paired in (False, True):
                for exit_ in (False, True):
                    for reader in (False, True):
                        for open_ in (False, True):
                            for moving in (False, True):
                                boxes, keep = door_boxes(kind, hinge, open_, moving, paired, exit_, reader)
                                lo, up = halves(boxes, keep)
                                tag = f"{'o' if open_ else 'c'}{'m' if moving else ''}{'p' if paired else ''}{'x' if exit_ else ''}{'r' if reader else ''}"
                                for half, bx in (("lower", lo), ("upper", up)):
                                    n = put(f"{kind}_{half}_{hinge}_{tag}", _model(bx, rid))
                                    for facing, y in FACING_Y.items():
                                        key = f"exit={str(exit_).lower()},facing={facing},half={half},hinge={hinge}," \
                                              f"moving={str(moving).lower()},open={str(open_).lower()}," \
                                              f"paired={str(paired).lower()},reader={str(reader).lower()}"
                                        variants[key] = {"model": rid(f"block/{n}"), **({"y": y} if y else {})}
        write(assets / "blockstates" / f"{kind}.json", {"variants": variants})
    # The part models (never placed: client/PlantDoorRenderer.java draws them).
    pv = {}
    for k, boxes in parts().items():
        n = put(f"plant_door_part_{k}", _model(boxes, rid))
        pv[f"part={k}"] = {"model": rid(f"block/{n}")}
    write(assets / "blockstates" / "plant_door_part.json", {"variants": pv})
    lang["block.jugcraft.plant_door_part"] = "Door Part"
    # The roller door: where in the door each block is.
    rv = {}
    for pos in ("bottom", "middle", "top", "single"):
        for left in (False, True):
            for right in (False, True):
                for open_ in (False, True):
                    for moving in (False, True):
                        n = put(f"roller_door_{pos}_{int(left)}{int(right)}{int(open_)}{int(moving)}",
                                _model(fixed(roller_boxes(pos, left, right, open_, moving)), rid))
                        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                            key = f"facing={facing},left={str(left).lower()},moving={str(moving).lower()},open={str(open_).lower()}," \
                                  f"pos={pos},right={str(right).lower()}"
                            rv[key] = {"model": rid(f"block/{n}"), **({"y": y} if y else {})}
    write(assets / "blockstates" / "roller_door.json", {"variants": rv})
    write(models / "roller_door.json", _model(fixed([B(0, 0, 6.5, 16, 16, 9.5, {"*": SLATS + "!", "up": SLATS, "down": SLATS})]), rid))
    # The push-button station.
    write(models / "roller_door_station.json", _model(fixed(station_boxes()), rid))
    write(assets / "blockstates" / "roller_door_station.json", {"variants": {
        f"facing={f}": {"model": rid("block/roller_door_station"), **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(assets / "items" / "roller_door_station.json", {"model": {"type": "minecraft:model", "model": rid("block/roller_door_station")}})
    lang.update({f"block.jugcraft.{k}": v for k, v in NAMES.items()})
    lang.update(LANG)


def door_loot(rid, door):
    """Only the lower half drops the door, keeping its hardware (exit, reader) on the item."""
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": rid(door),
        "modifier": {"type": "minecraft:copy_state", "block": rid(door), "properties": ["exit", "reader"]},
        "condition": {"type": "minecraft:match_block", "blocks": rid(door), "state": {"half": "lower"}}}],
        "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": rid(f"blocks/{door}")}


def textures():
    """The doors' textures, drawn at their size (the wired glass keeps its part transparency)."""
    out = {k: f() for k, f in makers().items()}
    out.update({k: f() for k, f in roller_makers().items()})
    return out
