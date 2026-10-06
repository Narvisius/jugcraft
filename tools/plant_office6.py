"""Round 9 of the plant kit: the ion chromatograph like the real thing, real-looking office plants, coats that hang.

- The ion chromatograph: a white tower with a blue-tinted glass door showing the modules inside (pump heads, the
  injection valve, the column, the detector, the tubing), eluent bottles on top with their lines running in, and
  a separate round sample changer (two white racks of vials on a blue base, with its needle arm) beside it.
- Plants: each variant a species built leaf by leaf: every leaf its own plane with a drawn leaf (shaded, veined,
  cut out), set at its own height and direction and tilted up or out. Fiddle-leaf fig, snake plant, rubber plant,
  monstera, areca palm, peace lily (with flowers), ZZ plant, echeverias.
- Coats: on a hanger, sloping shoulders, the body in folds and flaring at the hem, sleeves, a collar and lapels,
  pockets, buttons: a lab coat (with a pen and a dosimeter), a security jacket, a hi-vis vest.

Leaf planes: Minecraft turns an element about one axis only, by -45 .. 45 in 22.5 steps. A leaf lying along +z,
-z, +x or -x is a flat plane tipped up about the crosswise axis; leaves the other ways are upright planes turned
about y. The leaf picture is drawn base at the bottom, tip at the top, and turned per face to point outward.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import plant_office3 as O3
import plant_office5 as O5

S = 128
turned, cyl = O5.turned, O5.cyl


# ---------------------------------------------------------------------------------------------- coats

def coat(cx, ytop, zc, h, w, kind="lab"):
    """A coat on a hanger hanging from (cx, ytop): w across the body, h from the hanger to the hem, centred on
    depth zc. kind: "lab" (white, a pen in the chest pocket, a dosimeter badge), "security" (navy, a SECURITY
    patch) or "vest" (hi-vis orange, reflective bands, no sleeves)."""
    k = w / 7.6
    fab, fab2 = {"lab": ("pi_coat_white", "pi_coat_white_fold"), "security": ("pi_coat_navy", "pi_coat_navy_fold"),
                 "vest": ("pi_coat_orange", "pi_coat_orange_fold")}[kind]
    d = 1.25 * k                                   # half the body's depth
    out = []
    # The hanger: its hook up to the rail, the two arms.
    out += [(cx - 0.12, ytop, zc - 0.12, cx + 0.12, ytop + 0.8, zc + 0.12, "pi_chrome"),
            turned((cx - w / 2 + 0.3, ytop - 0.25, zc - 0.15, cx, ytop, zc + 0.15, "pi_chrome"), "z", 22.5, (cx, ytop, zc)),
            turned((cx, ytop - 0.25, zc - 0.15, cx + w / 2 - 0.3, ytop, zc + 0.15, "pi_chrome"), "z", -22.5, (cx, ytop, zc))]
    ys = ytop - 1.9 * k                            # where the shoulders meet the body
    # The shoulders, sloping down from the collar.
    out += [turned((cx - w / 2, ys, zc - d, cx, ys + 1.0 * k, zc + d, fab), "z", 22.5, (cx, ys + 1.0 * k, zc)),
            turned((cx, ys, zc - d, cx + w / 2, ys + 1.0 * k, zc + d, fab), "z", -22.5, (cx, ys + 1.0 * k, zc))]
    # The body in five strips, every other one standing a little proud (the folds), flaring out at the hem.
    ybot, yflare = ytop - h, ytop - h + 0.32 * h
    strips = 5
    for i in range(strips):
        x0 = cx - w / 2 + i * w / strips
        x1 = x0 + w / strips
        bulge = 0.14 * k if i % 2 else 0
        tex = fab if i % 2 else fab2
        out.append((x0, yflare, zc - d - bulge, x1, ys + 0.2, zc + d + bulge, tex))
        fx0 = x0 - (0.35 * k if i == 0 else 0)
        fx1 = x1 + (0.35 * k if i == strips - 1 else 0)
        out.append((fx0, ybot, zc - d - bulge - 0.12 * k, fx1, yflare, zc + d + bulge + 0.12 * k, tex))
    front = zc + d + 0.14 * k + 0.12 * k            # the front surface of the proud folds
    # The collar at the back of the neck, the lapels opening down the chest.
    out.append((cx - 1.4 * k, ys + 0.2, zc - d - 0.1, cx + 1.4 * k, ys + 1.3 * k, zc - d + 0.7 * k, fab2))
    if kind != "vest":
        out += [turned((cx - 1.5 * k, ys - 3.8 * k, front, cx - 0.1, ys + 0.6 * k, front + 0.15, fab), "z", -22.5,
                       (cx - 0.1, ys + 0.6 * k, front)),
                turned((cx + 0.1, ys - 3.8 * k, front, cx + 1.5 * k, ys + 0.6 * k, front + 0.15, fab), "z", 22.5,
                       (cx + 0.1, ys + 0.6 * k, front))]
        # The shirt behind the opening.
        out.append((cx - 0.9 * k, ys - 3.0 * k, front - 0.2, cx + 0.9 * k, ys + 0.2, front - 0.05,
                    "pi_shirt_blue" if kind == "lab" else "pi_shirt_white"))
        # Sleeves hanging at the sides, with cuffs.
        for side in (-1, 1):
            sx0, sx1 = (cx - w / 2 - 1.2 * k, cx - w / 2 + 0.05) if side < 0 else (cx + w / 2 - 0.05, cx + w / 2 + 1.2 * k)
            out.append((sx0, ybot + 0.42 * h, zc - d * 0.8, sx1, ys + 0.6 * k, zc + d * 0.8, fab))
            out.append((sx0 - 0.08, ybot + 0.37 * h, zc - d * 0.85, sx1 + 0.08, ybot + 0.42 * h + 0.05, zc + d * 0.85, fab2))
    # The placket and buttons down the front, the pockets.
    out.append((cx - 0.12 * k, ybot + 0.3, front, cx + 0.12 * k, ys - 3.6 * k, front + 0.06, fab2))
    for j in range(3):
        y = ys - 4.6 * k - j * (h * 0.18)
        out.append((cx - 0.22 * k, y, front, cx + 0.22 * k, y + 0.44 * k, front + 0.14, "pi_button"))
    for side in (-1, 1):
        px = cx + side * 2.0 * k
        out.append((px - 1.0 * k, ybot + 0.12 * h, front, px + 1.0 * k, ybot + 0.12 * h + 2.2 * k, front + 0.1, fab2))
    if kind == "lab":
        out.append((cx - 3.0 * k, ys - 3.4 * k, front, cx - 1.6 * k, ys - 1.9 * k, front + 0.1, fab2))     # chest pocket
        out.append((cx - 2.7 * k, ys - 2.4 * k, front + 0.05, cx - 2.45 * k, ys - 1.2 * k, front + 0.2, "pi_pen_blue"))
        out.append((cx + 1.7 * k, ys - 3.0 * k, front, cx + 2.7 * k, ys - 1.7 * k, front + 0.16, {"*": "pi_badge_clip", "south": "pi_dosimeter!"}))
    elif kind == "security":
        out.append((cx + 1.5 * k, ys - 3.0 * k, front, cx + 3.3 * k, ys - 2.0 * k, front + 0.08, {"*": "pi_coat_navy", "south": "pi_security_patch!"}))
    else:
        for y in (ybot + 0.25 * h, ybot + 0.5 * h):
            out.append((cx - w / 2 - 0.4 * k, y, zc - d - 0.4 * k, cx + w / 2 + 0.4 * k, y + 0.9 * k, zc + d + 0.4 * k, "pi_reflective"))
    return out


# ---------------------------------------------------------------------------------------------- the ion chromatograph

def _bottle(x, z, y0, r, h, liquid):
    """An eluent bottle: the clear bottle (square, its corners cut), the liquid inside, the blue cap."""
    return [(x - r, y0, z - r * 0.7, x + r, y0 + h, z + r * 0.7, "pi_bottle_clear"),
            (x - r * 0.7, y0, z - r, x + r * 0.7, y0 + h, z + r, "pi_bottle_clear"),
            (x - r * 0.75, y0 + 0.1, z - r * 0.75, x + r * 0.75, y0 + h * 0.7, z + r * 0.75, liquid),
            (x - r * 0.45, y0 + h, z - r * 0.45, x + r * 0.45, y0 + h + 0.7, z + r * 0.45, {"*": "pi_cap_ridged", "up": "pi_cap_blue"})]


def _tube(points, y_pairs=None):
    """A thin PTFE line through points (x, y, z), each leg straight along one axis."""
    out = []
    t = 0.09
    for (x0, y0, z0), (x1, y1, z1) in zip(points, points[1:]):
        out.append((min(x0, x1) - t, min(y0, y1) - t, min(z0, z1) - t, max(x0, x1) + t, max(y0, y1) + t, max(z0, z1) + t, "pi_tubing"))
    return out


ANALYZER = []
# The tower: white housing (sides, top, base, back), standing on the bench top (y 14.5) in front of the shelf post.
X0, X1, Z0, Z1, YB, YT = 1.2, 9.4, 5.4, 13.6, 14.5, 26.4
ANALYZER += [
    (X0, YB, Z0, X0 + 0.8, YT, Z1, {"*": "pi_ic_white", "west": "pi_ic_side!"}),
    (X1 - 0.8, YB, Z0, X1, YT, Z1, {"*": "pi_ic_white", "east": "pi_ic_side!"}),
    (X0, YT - 1.2, Z0, X1, YT, Z1, {"*": "pi_ic_white", "south": ("pi_ic_badge", [0, 0, 16, 2.34])}),
    (X0, YB, Z0, X1, YB + 0.9, Z1 + 0.2, "pi_ic_white"),
    (X0 + 0.8, YB + 0.9, Z0, X1 - 0.8, YT - 1.2, Z0 + 0.6, "pi_ic_inside"),
]
# Inside, behind the glass: the high-pressure pump (two heads), the injection valve, the column, the suppressor,
# the conductivity detector, the lines between them.
ANALYZER += [
    (2.6, 15.8, 6.0, 5.4, 18.0, 9.0, {"*": "pi_ic_module", "south": "pi_ic_pump!"}),
    (3.0, 18.0, 7.6, 3.8, 18.8, 8.4, "pi_steel"), (4.2, 18.0, 7.6, 5.0, 18.8, 8.4, "pi_steel"),
    (6.0, 15.8, 6.0, 8.0, 18.6, 9.4, {"*": "pi_ic_module", "south": "pi_ic_detector!"}),
] + cyl(4.6, 7.4, 0.8, 20.6, 21.4, {"*": "pi_steel", "up": "pi_ic_valve"}) + [
    (6.6, 19.2, 7.2, 7.2, 23.6, 7.8, "pi_ic_column"),
    (6.5, 23.6, 7.1, 7.3, 24.0, 7.9, "pi_steel"), (6.5, 18.8, 7.1, 7.3, 19.2, 7.9, "pi_steel"),
    (2.6, 22.2, 6.0, 5.2, 24.2, 8.6, {"*": "pi_ic_module", "south": "pi_ic_suppressor!"}),
] + _tube([(4.0, 18.8, 8.0), (4.0, 20.6, 8.0)]) + _tube([(5.4, 21.0, 7.4), (6.9, 21.0, 7.4), (6.9, 19.2, 7.4)]) \
  + _tube([(6.9, 24.0, 7.5), (6.9, 24.6, 7.5), (3.9, 24.6, 7.5), (3.9, 24.2, 7.5)]) \
  + _tube([(3.0, 22.2, 8.2), (3.0, 18.8, 8.2)])
# The door: a blue strip down its hinge side with the status display, the tinted glass, a handle.
ANALYZER += [
    (X0 + 0.8, YB + 0.9, Z1 - 0.4, X0 + 1.9, YT - 1.2, Z1, {"*": "pi_ic_blue", "south": "pi_ic_strip!"}),
    (X0 + 1.9, YB + 0.9, Z1 - 0.3, X1 - 0.8, YT - 1.2, Z1 - 0.15, "pi_ic_glass"),
    (X1 - 1.5, 19.0, Z1 - 0.15, X1 - 1.1, 21.6, Z1 + 0.3, "pi_ic_white"),
]
# Eluent bottles on top, lines from their caps into the top of the tower.
ANALYZER += _bottle(3.0, 9.6, YT, 1.05, 3.2, "pi_liquid_clear") + _bottle(5.4, 9.6, YT, 1.05, 3.2, "pi_liquid_clear") \
    + _bottle(7.7, 9.6, YT, 0.85, 2.6, "pi_liquid_blue")
for bx, top in ((3.0, YT + 3.9), (5.4, YT + 3.9), (7.7, YT + 3.3)):
    ANALYZER += _tube([(bx, top, 9.6), (bx, top + 0.6, 9.6), (bx, top + 0.6, 6.4), (bx, YT, 6.4)])
# The sample changer beside it: the blue base, two white racks of vials on a centre column, the tower with the arm
# and needle reaching over the outer ring.
SX, SZ = 12.9, 10.6
ANALYZER += cyl(SX, SZ, 2.8, YB, YB + 1.1, {"*": "pi_ic_blue", "up": "pi_ic_white"})
ANALYZER += cyl(SX, SZ, 2.6, YB + 2.3, YB + 2.6, {"*": "pi_ic_white", "up": "pi_ic_rack_c"})
ANALYZER += cyl(SX, SZ, 2.6, YB + 4.1, YB + 4.4, {"*": "pi_ic_white", "up": "pi_ic_rack_c"})
ANALYZER += cyl(SX, SZ, 0.6, YB + 1.1, YB + 5.6, "pi_ic_white")
for k in range(12):
    a = k / 12 * math.tau
    vx, vz = SX + math.cos(a) * 2.0, SZ + math.sin(a) * 2.0
    for y in (YB + 2.6, YB + 4.4):
        ANALYZER += [(vx - 0.22, y, vz - 0.22, vx + 0.22, y + 0.9, vz + 0.22, "pi_bottle_clear"),
                     (vx - 0.24, y + 0.9, vz - 0.24, vx + 0.24, y + 1.15, vz + 0.24, "pi_cap_blue")]
ANALYZER += [(14.6, YB + 1.1, 6.6, 15.6, YB + 7.4, 8.0, "pi_ic_white"),
             (12.9, YB + 6.6, 7.0, 14.6, YB + 7.3, 7.7, "pi_ic_white"),
             (12.75, YB + 5.6, 7.15, 13.05, YB + 6.6, 7.45, "pi_steel")]
ANALYZER += _tube([(14.6, YB + 6.0, 7.3), (9.4, YB + 6.0, 7.3)])


# ---------------------------------------------------------------------------------------------- plants

LEAF_KINDS = ["fig", "snake", "rubber", "monstera", "palm", "lily", "zz", "succulent", "spathe"]


def _plane_up(x0, z0, x1, z1, y, tex, turn):
    """A flat leaf at height y over x0..x1, z0..z1; turn: the picture's rotation on the top (the bottom face is
    turned to match, so the leaf points the same way seen from below)."""
    down = (turn + 180) % 360 if turn in (0, 180) else turn
    return {"from": [x0, y, z0], "to": [x1, y, z1],
            "faces": {"up": {"texture": f"#{tex}", "uv": [0, 0, 16, 16], "rotation": turn},
                      "down": {"texture": f"#{tex}", "uv": [0, 0, 16, 16], "rotation": down}}}


def _plane_v(x0, y0, x1, y1, z, tex):
    """An upright leaf in the x-y plane at depth z, base at the bottom."""
    return {"from": [x0, y0, z], "to": [x1, y1, z],
            "faces": {"north": {"texture": f"#{tex}", "uv": [16, 0, 0, 16]},
                      "south": {"texture": f"#{tex}", "uv": [0, 0, 16, 16]}}}


def _rot(e, axis, angle, origin):
    if angle:
        e["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
    return e


def leaf_out(bx, by, bz, direction, length, width, tilt, tex):
    """A leaf from its base (bx, by, bz) pointing out along direction (+z, -z, +x, -x), tipped up by tilt (22.5
    or 45; negative droops)."""
    hw = width / 2
    if direction == "+z":
        e = _plane_up(bx - hw, bz, bx + hw, bz + length, by, tex, 180)
        return _rot(e, "x", -tilt, (bx, by, bz))
    if direction == "-z":
        e = _plane_up(bx - hw, bz - length, bx + hw, bz, by, tex, 0)
        return _rot(e, "x", tilt, (bx, by, bz))
    if direction == "+x":
        e = _plane_up(bx, bz - hw, bx + length, bz + hw, by, tex, 90)
        return _rot(e, "z", tilt, (bx, by, bz))
    e = _plane_up(bx - length, bz - hw, bx, bz + hw, by, tex, 270)
    return _rot(e, "z", -tilt, (bx, by, bz))


def leaf_up(bx, by, bz, length, width, tex, yaw=0, lean=None):
    """An upright leaf from its base, turned about the upright by yaw, or leaning (lean = ("+z", 22.5) and the like)
    out from the middle instead."""
    e = _plane_v(bx - width / 2, by, bx + width / 2, by + length, bz, tex)
    if lean:
        d, a = lean
        axis = "x" if d in ("+z", "-z") else "z"
        sign = {"+z": 1, "-z": -1, "+x": -1, "-x": 1}[d]
        if axis == "z":
            e["from"], e["to"] = [bx, by, bz - width / 2], [bx, by + length, bz + width / 2]
            e["faces"] = {"east": {"texture": f"#{tex}", "uv": [16, 0, 0, 16]}, "west": {"texture": f"#{tex}", "uv": [0, 0, 16, 16]}}
        return _rot(e, axis, sign * a, (bx, by, bz))
    return _rot(e, "y", yaw, (bx, by, bz))


def stick(x, z, y0, y1, tex, t=0.35):
    six = ("north", "south", "east", "west", "up", "down")
    return {"from": [x - t, y0, z - t], "to": [x + t, y1, z + t], "faces": {f: {"texture": f"#{tex}"} for f in six}}


SOIL = 6.4
DIRS = ["+z", "+x", "-z", "-x"]
SIX = ("north", "south", "east", "west", "up", "down")

# Round 9b: the office plants are little trees made the way the game makes its trees (log and leaf blocks), in
# miniature: a trunk of real log, branches, a canopy of small leaf cubes heaped into its shape. Leaves are the game's
# own where they need no tint (azalea, flowering azalea, cherry), ours drawn in the game's style for the rest.
TREES = [  # name, log, leaves (several = mixed), trunk top, canopy middle (x, y, z), canopy radii (x, y, z), cubes
    ("oak", "oak_log", ["pi_mini_oak"], 15.5, (8, 19, 8), (4.6, 3.6, 4.6), 20),
    ("birch", "birch_log", ["pi_mini_birch"], 22.5, (8, 20, 8), (3.0, 5.0, 3.0), 18),
    ("spruce", "spruce_log", ["pi_mini_spruce"], 25.0, None, None, 0),
    ("azalea", "oak_log", ["minecraft:block/azalea_leaves"], 11.0, (8, 14, 8), (5.2, 3.0, 5.2), 20),
    ("flowering azalea", "oak_log", ["minecraft:block/flowering_azalea_leaves", "minecraft:block/azalea_leaves"], 11.0,
     (8, 14, 8), (5.2, 3.0, 5.2), 20),
    ("cherry", "cherry_log", ["minecraft:block/cherry_leaves"], 17.0, (8.6, 20, 8), (5.2, 3.4, 4.8), 20),
    ("jungle", "jungle_log", ["pi_mini_jungle"], 21.0, (8, 22, 8), (4.2, 3.0, 4.2), 17),
    ("acacia", "acacia_log", ["pi_mini_acacia"], 18.0, (9.4, 20, 8), (5.6, 1.6, 5.0), 18),
]


def _uv(face, x0, y0, z0, x1, y1, z1):
    """The texture area a face takes from where it lies, kept inside the texture (a tree reaches above its block)."""
    if face in ("north", "south"):
        u = (16 - x1, 16 - x0) if face == "north" else (x0, x1)
        v = (16 - y1, 16 - y0)
    elif face in ("east", "west"):
        u = (16 - z1, 16 - z0) if face == "east" else (z0, z1)
        v = (16 - y1, 16 - y0)
    else:
        u, v = (x0, x1), ((z0, z1) if face == "up" else (16 - z1, 16 - z0))

    def fit(a, b):
        span = min(16, b - a)
        a = min(max(a, 0), 16 - span)
        return a, a + span
    u, v = fit(*u), fit(*v)
    return [round(u[0], 3), round(v[0], 3), round(u[1], 3), round(v[1], 3)]


def _cube(x0, y0, z0, x1, y1, z1, key):
    return {"from": [round(x0, 3), round(y0, 3), round(z0, 3)], "to": [round(x1, 3), round(y1, 3), round(z1, 3)],
            "faces": {f: {"texture": f"#{key}", "uv": _uv(f, x0, y0, z0, x1, y1, z1)} for f in SIX}}


def _log(x, z, y0, y1, t=0.75):
    e = _cube(x - t, y0, z - t, x + t, y1, z + t, "log")
    e["faces"]["up"]["texture"] = e["faces"]["down"]["texture"] = "#log_top"
    return e


def plant_elements(i):
    """Variant i's little tree: its elements and its textures (key -> texture id, "rid:" for ours)."""
    name, log, leaves, top, mid, rad, count = TREES[i % len(TREES)]
    r = random.Random(4200 + i)
    els = []
    tex = {"log": f"minecraft:block/{log}", "log_top": f"minecraft:block/{log}_top"}
    keys = []
    for k, leaf in enumerate(leaves):
        key = f"leaves{k}"
        tex[key] = leaf if leaf.startswith("minecraft:") else "rid:" + leaf
        keys.append(key)
    # The trunk: straight, or leaning out and back (cherry, acacia), with a couple of branches into the canopy.
    if name in ("cherry", "acacia"):
        els.append(_log(8, 8, SOIL - 0.4, 12.5, 0.7))
        lean = _log(8, 8, 12, top, 0.65)
        lean["rotation"] = {"angle": -22.5, "axis": "z", "origin": [8, 12, 8]}
        els.append(lean)
    else:
        els.append(_log(8, 8, SOIL - 0.4, top, 0.8 if name != "birch" else 0.6))
    if name not in ("spruce", "birch"):
        for k, (d, axis, angle) in enumerate((("+x", "z", -45), ("-x", "z", 45), ("+z", "x", 45))):
            by = top - 3.5 + k * 0.8
            b = _cube(7.6, by, 7.6, 8.4, by + 3.6, 8.4, "log")
            b["rotation"] = {"angle": angle, "axis": axis, "origin": [8, by, 8]}
            els.append(b)
    if name == "spruce":
        # Tiers of branches narrowing to the tip, each a square and the same square turned 45 degrees.
        y = 10.0
        for k, half in enumerate((4.6, 3.8, 3.0, 2.2, 1.4, 0.8)):
            h = 2.6 if k < 5 else 2.0
            els.append(_cube(8 - half, y, 8 - half, 8 + half, y + h, 8 + half, keys[0]))
            rot = _cube(8 - half * 0.9, y + 0.6, 8 - half * 0.9, 8 + half * 0.9, y + h + 0.4, 8 + half * 0.9, keys[0])
            rot["rotation"] = {"angle": 45, "axis": "y", "origin": [8, y, 8]}
            els.append(rot)
            y += h * 0.85
    else:
        # The canopy: leaf cubes of a few sizes heaped inside an egg shape, the biggest in the middle.
        cx, cy, cz = mid
        ax, ay, az = rad
        placed = 0
        tries = 0
        while placed < count and tries < 400:
            tries += 1
            u, v, w = r.uniform(-1, 1), r.uniform(-1, 1), r.uniform(-1, 1)
            if u * u + v * v + w * w > 1:
                continue
            size = r.uniform(2.8, 4.6) * (1.25 if placed < 3 else 1)
            x, y, z = cx + u * (ax - size / 2), cy + v * (ay - size / 3), cz + w * (az - size / 2)
            hy = size * r.uniform(0.75, 1.0)
            els.append(_cube(x - size / 2, y - hy / 2, z - size / 2, x + size / 2, y + hy / 2, z + size / 2,
                             keys[placed % len(keys)]))
            placed += 1
        if name == "jungle":      # a lower clump and a hanging vine
            els.append(_cube(5.0, 14.0, 6.6, 8.0, 16.4, 9.4, keys[0]))
            els.append(_cube(8.6, 16.0, 7.0, 11.2, 18.2, 9.6, keys[0]))
    return els, tex


def plant_textures():
    names = set()
    for i in range(len(TREES)):
        names |= {v[4:] for v in plant_elements(i)[1].values() if v.startswith("rid:")}
    return names


def _mini_leaves(base, seed):
    """Leaves in the game's own style: 16 x 16 pixels of a few greens with gaps, enlarged with hard edges."""
    rr = random.Random(seed)
    small = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shades = [tuple(max(0, min(255, int(c * k))) for c in base) + (255,) for k in (0.72, 0.86, 1.0, 1.14)]
    for y in range(16):
        for x in range(16):
            if rr.random() < 0.16:
                continue
            small.putpixel((x, y), rr.choice(shades))
    return small.resize((S, S), Image.NEAREST)


# ---------------------------------------------------------------------------------------------- textures

def _img(c, size=S):
    a = np.zeros((size, size, 4), np.uint8)
    a[..., :3] = c[:3]
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _leaf(kind):
    """A leaf, base at the bottom, tip at the top, on a clear sheet: drawn at 512 and brought down to 128 (smooth
    edges, then cut hard so it renders cut out). Shaded darker to the edges, a lighter midrib, veins, a sheen."""
    N = 512
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(5000 + LEAF_KINDS.index(kind))
    cx = N / 2

    def outline(width_at, y0=N - 6, y1=6, steps=80):
        left, right = [], []
        for s in range(steps + 1):
            t = s / steps
            y = y0 + (y1 - y0) * t
            w = width_at(t)
            left.append((cx - w, y))
            right.append((cx + w, y))
        return left + right[::-1]

    def shade(poly, base, light, dark):
        d.polygon(poly, fill=base + (255,))
        mask = Image.new("L", (N, N), 0)
        ImageDraw.Draw(mask).polygon(poly, fill=255)
        grad = np.zeros((N, N, 4), np.uint8)
        xs = np.abs(np.arange(N) - cx) / (N / 2)
        for c in range(3):
            row = light[c] * (1 - xs) ** 2 + dark[c] * (1 - (1 - xs) ** 2)
            grad[..., c] = np.clip(row, 0, 255)[None, :]
        grad[..., 3] = (np.asarray(mask) * 0.55).astype(np.uint8)
        img.alpha_composite(Image.fromarray(grad, "RGBA"))

    def veins(colour, count, spread, y0=N - 20, y1=30, curve=0.35, width=3):
        for k in range(count):
            t = 0.08 + k / count * 0.85
            y = y0 + (y1 - y0) * t
            for side in (-1, 1):
                d.line([(cx, y), (cx + side * spread(t) * 0.55, y - spread(t) * curve), (cx + side * spread(t) * 0.92, y - spread(t) * curve * 1.6)],
                       fill=colour, width=width)

    if kind in ("fig", "rubber", "lily", "monstera", "succulent", "spathe"):
        if kind == "fig":            # violin-shaped: wide near the tip, waisted, wavy edge
            wf = lambda t: 200 * (math.sin(math.pi * min(1, t * 1.05)) ** 0.8) * (0.78 + 0.3 * t) * (1 - 0.12 * math.sin(math.pi * t * 2.2)) + 3 * math.sin(t * 40)
            base, light, dark = (70, 130, 50), (150, 200, 90), (40, 90, 30)
        elif kind == "rubber":       # oval, pointed tip, very dark and glossy
            wf = lambda t: 170 * math.sin(math.pi * t) ** 0.7
            base, light, dark = (36, 64, 40), (90, 120, 80), (20, 36, 24)
        elif kind == "lily":         # lance-shaped, glossy
            wf = lambda t: 120 * math.sin(math.pi * t ** 0.85) ** 0.9
            base, light, dark = (40, 100, 44), (110, 170, 90), (24, 70, 30)
        elif kind == "monstera":     # heart-shaped, wide
            wf = lambda t: 240 * (math.sin(math.pi * (0.12 + 0.88 * t)) ** 0.6) * (1 if t > 0.08 else 0.7)
            base, light, dark = (36, 110, 44), (100, 170, 80), (24, 76, 30)
        elif kind == "succulent":    # fat, pointed, frosty
            wf = lambda t: 220 * math.sin(math.pi * min(1, t * 0.9 + 0.1)) ** 0.5
            base, light, dark = (130, 170, 160), (190, 220, 210), (100, 140, 130)
        else:                        # the peace lily's white spathe
            wf = lambda t: 150 * math.sin(math.pi * t) ** 0.6
            base, light, dark = (238, 240, 232), (255, 255, 250), (200, 206, 196)
        poly = outline(wf)
        shade(poly, base, light, dark)
        if kind != "spathe" and kind != "succulent":
            vein = tuple(min(255, c + 50) for c in base) + (255,)
            veins(vein, 9 if kind != "lily" else 12, lambda t: wf(t), curve=0.45 if kind != "lily" else 0.7, width=3)
            d.line([(cx, N - 6), (cx, 20)], fill=tuple(min(255, c + 70) for c in base) + (255,), width=7)
            if kind == "rubber":
                d.line([(cx, N - 6), (cx, 40)], fill=(150, 60, 50, 255), width=6)
        if kind == "monstera":       # the splits from the edge toward the midrib, and the holes
            for k in range(7):
                t = 0.15 + k * 0.11
                y = (N - 6) + (6 - (N - 6)) * t
                for side in (-1, 1):
                    w = wf(t)
                    d.polygon([(cx + side * w * 1.05, y - 18), (cx + side * w * 1.05, y + 6), (cx + side * w * 0.35, y - 2)], fill=(0, 0, 0, 0))
                    if k % 2:
                        hx = cx + side * w * 0.25
                        d.ellipse([hx - 9, y - 22, hx + 9, y - 6], fill=(0, 0, 0, 0))
        if kind == "succulent":
            d.polygon([(cx - 30, 40), (cx, 6), (cx + 30, 40)], fill=(200, 110, 140, 255))
        # A sheen along one side.
        sheen = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        ImageDraw.Draw(sheen).line([(cx - 40, N - 80), (cx - 70, 140)], fill=(255, 255, 255, 34), width=22)
        mask = Image.new("L", (N, N), 0)
        ImageDraw.Draw(mask).polygon(poly, fill=255)
        sheen.putalpha(Image.fromarray(np.minimum(np.asarray(sheen)[..., 3], np.asarray(mask))))
        img.alpha_composite(sheen)
        # Nothing outside the leaf's outline (the veins were drawn a little long).
        arr = np.asarray(img).copy()
        arr[..., 3] = np.minimum(arr[..., 3], np.asarray(mask))
        img = Image.fromarray(arr, "RGBA")
    elif kind == "snake":            # a tall blade: banded dark and grey-green, yellow margins
        wf = lambda t: 150 * (1 - t) ** 0.35 * min(1, t * 6 + 0.4)
        poly = outline(wf)
        d.polygon(poly, fill=(220, 200, 70, 255))
        inner = outline(lambda t: max(0, wf(t) - 26))
        d.polygon(inner, fill=(40, 90, 50, 255))
        mask = Image.new("L", (N, N), 0)
        ImageDraw.Draw(mask).polygon(inner, fill=255)
        bands = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        bd = ImageDraw.Draw(bands)
        for y in range(0, N, 22):
            yy = y + r.randint(-6, 6)
            bd.arc([cx - 160, yy - 10, cx + 160, yy + 30], 200, 340, fill=(140, 170, 120, 200), width=6)
        bands.putalpha(Image.fromarray(np.minimum(np.asarray(bands)[..., 3], np.asarray(mask))))
        img.alpha_composite(bands)
    elif kind == "palm":             # a frond: the rachis and many narrow leaflets
        d.line([(cx, N - 4), (cx, 10)], fill=(150, 160, 70, 255), width=8)
        for k in range(26):
            t = k / 26
            y = N - 30 - t * (N - 60)
            ln = 210 * math.sin(math.pi * (0.15 + 0.8 * t)) * (1 - 0.3 * t)
            for side in (-1, 1):
                col = r.choice([(60, 140, 60), (80, 160, 70), (50, 120, 50)]) + (255,)
                d.polygon([(cx, y), (cx + side * ln, y - 40 - ln * 0.15), (cx + side * ln * 0.96, y - 30 - ln * 0.15), (cx, y + 10)], fill=col)
    elif kind == "zz":               # a stem with paired glossy oval leaflets
        d.line([(cx, N - 4), (cx, 20)], fill=(70, 110, 50, 255), width=10)
        for k in range(7):
            y = N - 70 - k * 62
            for side in (-1, 1):
                ex = cx + side * 110
                d.ellipse([min(cx, ex) + (8 if side > 0 else -8), y - 36, max(cx, ex) + (8 if side > 0 else -8), y + 26], fill=(40, 100, 40, 255))
                d.ellipse([min(cx, ex) + 30, y - 24, max(cx, ex) - 30, y - 4], fill=(90, 150, 80, 160))
        d.ellipse([cx - 50, 10, cx + 50, 110], fill=(40, 100, 40, 255))
    img = img.resize((S, S), Image.LANCZOS)
    a = np.asarray(img).copy()
    a[..., 3] = np.where(a[..., 3] >= 110, 255, 0)
    return Image.fromarray(a, "RGBA")


def _fabric(c, seed, fold=False):
    """Cloth: soft vertical folds, a fine weave."""
    rr = np.random.default_rng(seed)
    x = np.arange(S)
    folds = 1 + 0.06 * np.sin(x / S * math.tau * 2 + seed) + 0.04 * np.sin(x / S * math.tau * 5 + seed * 2)
    if fold:
        folds *= 0.9
    a = np.zeros((S, S, 4), np.uint8)
    base = np.broadcast_to(np.array(c, float)[None, None, :] * folds[None, :, None], (S, S, 3)).copy()
    base += rr.normal(0, 3, (S, S, 1))
    a[..., :3] = np.clip(base, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _ic_badge():
    """The housing's top band: the maker's line and the model, fitted to the band (2.34 / 16 of the picture)."""
    N = 512
    h = int(N * 2.34 / 16)
    img = Image.new("RGBA", (N, N), (238, 240, 240, 255))
    d = ImageDraw.Draw(img)
    size = h - 18
    while d.textlength("ION CHROM 900", font=O3._font(size)) > N * 0.62:
        size -= 1
    d.text((20, (h - size) / 2 - 4), "ION CHROM 900", font=O3._font(size), fill=(30, 80, 150, 255))
    s2 = max(12, int(size * 0.5))
    d.text((N - 20 - d.textlength("Compact IC", font=O3._font(s2)), (h - s2) / 2 - 2), "Compact IC", font=O3._font(s2), fill=(120, 124, 130, 255))
    d.line([(0, h - 3), (N, h - 3)], fill=(200, 204, 206, 255), width=3)
    return img


def _ic_strip():
    img = _img((30, 96, 170))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 14, 108, 34], fill=(16, 22, 30, 255))
    d.rectangle([28, 20, 70, 28], fill=(90, 230, 140, 254))
    for k in range(3):
        d.ellipse([46, 50 + k * 18, 82, 62 + k * 18], fill=(230, 236, 244, 255))
    return img


def _ic_glass():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (70, 170, 210)
    a[..., 3] = 95
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.line([(10, 120), (60, 10)], fill=(220, 245, 255, 120), width=8)
    d.rectangle([0, 0, S - 1, S - 1], outline=(40, 120, 180, 200), width=4)
    return img


def _ic_face(kind):
    img = _img((70, 120, 170) if kind != "suppressor" else (210, 214, 218))
    d = ImageDraw.Draw(img)
    if kind == "pump":
        for k in range(2):
            d.ellipse([18 + k * 52, 30, 58 + k * 52, 70], fill=(200, 204, 210, 255), outline=(40, 44, 50, 255), width=3)
        d.rectangle([16, 90, 112, 110], fill=(30, 60, 100, 255))
    elif kind == "detector":
        d.rectangle([20, 20, 108, 60], fill=(30, 60, 100, 255))
        d.ellipse([50, 76, 78, 104], fill=(200, 204, 210, 255))
    else:
        d.rectangle([14, 14, 114, 114], outline=(120, 124, 130, 255), width=4)
        d.text((24, 50), "SUPP", font=O3._font(22), fill=(60, 64, 70, 255))
    return img


def _ic_side():
    img = _img((236, 238, 238))
    d = ImageDraw.Draw(img)
    for x in range(24, 104, 9):
        d.rounded_rectangle([x, 70, x + 4, 116], radius=2, fill=(200, 204, 206, 255))
    return img


def _rack_top():
    img = O5._round_top(12.9, 10.6, 2.6, (236, 238, 238, 255), (210, 214, 216, 255))
    d = ImageDraw.Draw(img)
    k = S / 16
    for j in range(12):
        a = j / 12 * math.tau
        x, z = (12.9 + math.cos(a) * 2.0) * k, (10.6 + math.sin(a) * 2.0) * k
        d.ellipse([x - 2, z - 2, x + 2, z + 2], fill=(150, 154, 158, 255))
    return img


def _security_patch():
    img = _img((30, 40, 70))
    d = ImageDraw.Draw(img)
    f = O3._font(24)
    d.text(((S - d.textlength("SECURITY", font=f)) / 2, 50), "SECURITY", font=f, fill=(236, 236, 230, 255))
    d.rectangle([4, 4, S - 5, S - 5], outline=(200, 170, 60, 255), width=4)
    return img


def _dosimeter():
    img = _img((40, 90, 190))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 20, 108, 60], fill=(16, 20, 26, 255))
    d.text((28, 26), "0.02mSv", font=O3._font(16), fill=(120, 230, 150, 255))
    d.rectangle([20, 76, 108, 100], fill=(240, 240, 236, 255))
    return img


def makers():
    out = {}
    out.update({
        "pi_mini_oak": lambda: _mini_leaves((72, 140, 40), 7001), "pi_mini_birch": lambda: _mini_leaves((118, 158, 72), 7002),
        "pi_mini_spruce": lambda: _mini_leaves((70, 110, 70), 7003), "pi_mini_jungle": lambda: _mini_leaves((50, 160, 30), 7004),
        "pi_mini_acacia": lambda: _mini_leaves((110, 140, 40), 7005),
        "pi_coat_white": lambda: _cloth((248, 248, 246), 6010), "pi_coat_white_fold": lambda: _cloth((238, 239, 238), 6011),
        "pi_coat_navy": lambda: _cloth((36, 46, 78), 6012), "pi_coat_navy_fold": lambda: _cloth((30, 38, 66), 6013),
        "pi_coat_orange": lambda: _cloth((246, 120, 20), 6014), "pi_coat_orange_fold": lambda: _cloth((232, 110, 16), 6015),
        "pi_reflective": lambda: O5._brushed((206, 210, 212), 6016, False), "pi_button": lambda: _img((214, 214, 208)),
        "pi_shirt_blue": _oxford, "pi_shirt_white": lambda: _cloth((236, 236, 232), 6018),
        "pi_badge_clip": lambda: _img((40, 90, 190)), "pi_dosimeter": _dosimeter, "pi_security_patch": _security_patch,
        "pi_ic_white": lambda: _img((236, 238, 238)), "pi_ic_side": _ic_side, "pi_ic_badge": _ic_badge,
        "pi_ic_inside": lambda: _img((200, 206, 210)), "pi_ic_module": lambda: _img((70, 120, 170)),
        "pi_ic_pump": lambda: _ic_face("pump"), "pi_ic_detector": lambda: _ic_face("detector"),
        "pi_ic_suppressor": lambda: _ic_face("suppressor"),
        "pi_ic_valve": lambda: O5._round_top(4.6, 7.4, 0.8, (60, 64, 70, 255), (190, 194, 198, 255)),
        "pi_ic_column": lambda: _img((236, 236, 230)), "pi_steel": lambda: O5._brushed((180, 184, 188), 6020),
        "pi_ic_blue": lambda: _img((30, 96, 170)), "pi_ic_strip": _ic_strip, "pi_ic_glass": _ic_glass,
        "pi_ic_rack_c": _rack_top, "pi_tubing": lambda: _img((244, 244, 240)),
    })
    return out


ICONS = {"lab_coat", "security_jacket"}
MINI = {"pi_mini_oak", "pi_mini_birch", "pi_mini_spruce", "pi_mini_jungle", "pi_mini_acacia"}
SHARP = ICONS | MINI | {f"pi_leaf_{k}" for k in LEAF_KINDS} | {"pi_spathe", "pi_ic_badge", "pi_ic_strip", "pi_dosimeter", "pi_security_patch",
                                                  "pi_ic_pump", "pi_ic_detector", "pi_ic_suppressor"} | \
    {"pi_coat_white", "pi_coat_white_fold", "pi_coat_navy", "pi_coat_navy_fold", "pi_coat_orange", "pi_coat_orange_fold"}
TRANSLUCENT = MINI | {"pi_ic_glass"} | {f"pi_leaf_{k}" for k in LEAF_KINDS} | {"pi_spathe"}


# ---------------------------------------------------------------------------------------------- the uniforms (items)
# Worn and hung-up coats are drawn in 3D by client/UniformLayer.java and client/LockerRenderer.java from this sheet:
# 256 x 256, sixteen 64 x 64 materials, in this order (UniformLayer's region(col, row)).

UNIFORM_MATERIALS = ["white", "white_shade", "navy", "navy_shade",
                     "black", "pen", "shirt_blue", "shirt_white",
                     "dosimeter", "patch", "chrome", "zip",
                     "white_lining", "navy_lining", "logo", "id_badge"]


def _cloth(c, seed):
    """Plain cloth: one colour with the faintest weave (no folds: a fold drawn across a face stretches into stripes)."""
    rr = np.random.default_rng(seed)
    a = np.zeros((S, S, 4), np.uint8)
    base = np.array(c, float)[None, None, :] + rr.normal(0, 1.6, (S, S, 1))
    a[..., :3] = np.clip(base, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def draw_uniform_sheet():
    sheet = Image.new("RGBA", (256, 256), (0, 0, 0, 255))
    tiles = {
        "white": _cloth((250, 250, 248), 6101), "white_shade": _cloth((218, 220, 220), 6102),
        "navy": _cloth((38, 48, 80), 6103), "navy_shade": _cloth((24, 31, 54), 6104),
        "black": _cloth((28, 30, 34), 6105), "pen": _img((36, 70, 190)),
        "shirt_blue": _oxford(), "shirt_white": _cloth((238, 238, 234), 6106),
        "dosimeter": _dosimeter(), "patch": _security_patch(), "chrome": O5._brushed((200, 204, 210), 6107),
        "zip": O5._brushed((140, 144, 150), 6108), "white_lining": _cloth((196, 198, 198), 6109),
        "navy_lining": _cloth((70, 80, 108), 6110), "logo": _plant_logo(), "id_badge": _id_badge(),
    }
    for k, name in enumerate(UNIFORM_MATERIALS):
        sheet.paste(tiles[name].resize((64, 64), Image.LANCZOS).convert("RGBA"), ((k % 4) * 64, (k // 4) * 64))
    return sheet


def _oxford():
    """A light blue oxford shirt: a fine basket weave of blue and white threads."""
    img = _img((160, 190, 222))
    d = ImageDraw.Draw(img)
    for y in range(0, S, 4):
        for x in range(0, S, 4):
            if (x // 4 + y // 4) % 2:
                d.rectangle([x, y, x + 1, y + 1], fill=(222, 232, 244, 255))
    return img


def _plant_logo():
    """The plant's embroidered logo: a blue trefoil in a ring with the unit's initials, on the white cloth."""
    img = _img((250, 250, 248))
    d = ImageDraw.Draw(img)
    blue = (30, 80, 160, 255)
    d.ellipse([10, 10, 118, 118], outline=blue, width=7)
    for a in (90, 210, 330):
        d.pieslice([30, 30, 98, 98], a - 28, a + 28, fill=blue)
    d.ellipse([56, 56, 72, 72], fill=blue)
    d.ellipse([52, 52, 76, 76], outline=(250, 250, 248, 255), width=3)
    return img


def _id_badge():
    """A photo ID on a clip: blue header with the plant's name, the photo, the name and the access bars."""
    img = _img((246, 246, 244))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 26], fill=(30, 80, 160, 255))
    d.text((8, 4), "JUGCRAFT NUCLEAR", font=O3._font(13), fill=(240, 244, 250, 255))
    d.rectangle([8, 34, 50, 86], fill=(200, 190, 170, 255))
    d.ellipse([19, 40, 39, 60], fill=(120, 90, 70, 255))
    d.pieslice([12, 62, 46, 100], 180, 360, fill=(60, 90, 150, 255))
    d.rectangle([8, 34, 50, 86], outline=(170, 170, 166, 255), width=2)
    d.text((58, 36), "REYES", font=O3._font(15), fill=(30, 30, 34, 255))
    d.text((58, 54), "Chemistry", font=O3._font(11), fill=(90, 90, 96, 255))
    d.rectangle([58, 70, 118, 78], fill=(30, 80, 160, 255))
    d.rectangle([58, 80, 100, 86], fill=(200, 40, 40, 255))
    for k in range(18):
        if k % 3:
            d.rectangle([8 + k * 6, 96, 10 + k * 6, 118], fill=(30, 30, 34, 255))
    return img


def uniform_icon(kind):
    """The inventory picture: the coat on its hanger, front on."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    lab = kind == "lab_coat"
    body, dark = ((236, 238, 236), (196, 200, 200)) if lab else ((40, 52, 88), (24, 32, 58))
    d.line([(64, 6), (64, 16)], fill=(170, 174, 180, 255), width=4)
    d.line([(26, 30), (64, 16), (102, 30)], fill=(170, 174, 180, 255), width=4)
    bottom = 122 if lab else 100
    d.polygon([(30, 30), (64, 22), (98, 30), (104, 110), (108, bottom), (20, bottom), (24, 110)], fill=body + (255,), outline=dark + (255,))
    d.polygon([(30, 30), (16, 36), (12, 96), (26, 96), (32, 50)], fill=body + (255,), outline=dark + (255,))
    d.polygon([(98, 30), (112, 36), (116, 96), (102, 96), (96, 50)], fill=body + (255,), outline=dark + (255,))
    d.polygon([(54, 24), (64, 52), (74, 24)], fill=((150, 182, 216) if lab else (236, 236, 232)) + (255,))
    d.line([(64, 52), (64, bottom)], fill=dark + (255,), width=2)
    d.line([(54, 24), (64, 56), (74, 24)], fill=dark + (255,), width=3)
    if lab:
        d.rectangle([44, 48, 56, 62], fill=(30, 80, 160, 255))          # the ID badge
        d.rectangle([74, 50, 90, 64], outline=dark + (255,))              # the chest pocket
        d.ellipse([78, 53, 86, 61], outline=(30, 80, 160, 255), width=2)  # its logo
        d.rectangle([86, 44, 89, 58], fill=(36, 70, 190, 255))            # the pen
        d.line([(70, 56), (70, bottom)], fill=dark + (255,), width=1)     # the placket edge
    else:
        d.rectangle([74, 46, 96, 56], fill=(220, 220, 214, 255))
    return img
