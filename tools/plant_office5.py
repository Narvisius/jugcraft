"""Round 8 of the plant kit: rounder, realer things.

Round things are built as stepped cylinders (three overlapping boxes: a plus and a square, which read as a circle),
domes as cylinders narrowing as they rise, crescents and curls as short pieces turned 22.5 or 45 degrees. Pictures on
round tops are drawn where the top lies in the block (no "!"), so the pieces of one cylinder line up.

What is here: the break room food (takeout noodles in a folded carton, a gyro plate, a burger and fries, a proper
salad with cutlery and a bottle of water, a croissant and a coffee); a real fridge (bottom freezer, a door that
stands proud of the cabinet on its gasket, bar handles on stand-offs); the meeting table set out for a meeting
(papers about money and management, coffee, notepads, laptops, a speakerphone, a coffee pot); the plant manager's
oval glass coffee table on two walnut drums.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import plant_office3 as O3

S = 128


# ---------------------------------------------------------------------------------------------- shapes

def turned(box, axis, angle, origin):
    """A box turned about axis (x, y or z) through origin by angle (22.5 steps, at most 45)."""
    return tuple(box) + ({"rotation": {"angle": angle, "axis": axis, "origin": list(origin)}},)


def cyl(cx, cz, r, y0, y1, tex):
    """A round piece standing up: a plus and a square, which together read as a circle."""
    a, b = r * 0.55, r * 0.85
    return [(cx - r, y0, cz - a, cx + r, y1, cz + a, tex), (cx - a, y0, cz - r, cx + a, y1, cz + r, tex),
            (cx - b, y0, cz - b, cx + b, y1, cz + b, tex)]


def cyl_x(cy, cz, r, x0, x1, tex):
    """A round piece lying along x (a wrap, a roll). Only the square middle box carries the ends' texture; the two
    arms of the plus stop a hair short of each end (showing the side texture there), so the three never share an end
    face (round 14: the gyro's cut end, stretched over all three, z-fought at its edge)."""
    a, b = r * 0.55, r * 0.85
    side = tex.get("*") if isinstance(tex, dict) else tex
    arms = {**tex, "west": side, "east": side} if isinstance(tex, dict) else tex
    e = 0.06
    return [(x0 + e, cy - r, cz - a, x1 - e, cy + r, cz + a, arms), (x0 + e, cy - a, cz - r, x1 - e, cy + a, cz + r, arms),
            (x0, cy - b, cz - b, x1, cy + b, cz + b, tex)]


def spin(boxes, angle, origin):
    """Boxes turned together about the upright through origin (x, y, z)."""
    return [turned(b[:7], "y", angle, origin) for b in boxes]


def mug(x, z, y0):
    """A plain white mug with coffee in it and a handle (as plant_office4.mug)."""
    w, h, t = 2.4, 2.8, 0.3
    m = "pi_mug_plain"
    return [(x, y0, z, x + w, y0 + 0.3, z + w, m),
            (x, y0, z, x + t, y0 + h, z + w, m), (x + w - t, y0, z, x + w, y0 + h, z + w, m),
            (x + t, y0, z, x + w - t, y0 + h, z + t, m), (x + t, y0, z + w - t, x + w - t, y0 + h, z + w, m),
            (x + t + 0.02, y0 + 0.3, z + t + 0.02, x + w - t - 0.02, y0 + h - 0.5, z + w - t - 0.02, {"*": "pi_coffee", "up": "pi_coffee"}),
            (x + w, y0 + 0.7, z + 0.9, x + w + 0.9, y0 + 0.95, z + 1.5, m),
            (x + w + 0.65, y0 + 0.95, z + 0.9, x + w + 0.9, y0 + 1.95, z + 1.5, m),
            (x + w, y0 + 1.95, z + 0.9, x + w + 0.9, y0 + 2.2, z + 1.5, m)]


def fork(x, z, y0, angle=0):
    """A fork lying flat, pointing north: the handle, the neck, the head and four tines."""
    c = "pi_cutlery"
    out = [(x - 0.25, y0, z + 0.2, x + 0.25, y0 + 0.15, z + 3.4, c), (x - 0.15, y0, z - 0.4, x + 0.15, y0 + 0.12, z + 0.2, c),
           (x - 0.5, y0, z - 1.1, x + 0.5, y0 + 0.12, z - 0.4, c)]
    for k in range(4):
        tx = x - 0.5 + k * 0.29
        out.append((tx, y0, z - 2.3, tx + 0.13, y0 + 0.1, z - 1.1, c))
    return spin(out, angle, (x, y0, z)) if angle else out


def knife(x, z, y0, angle=0):
    """A table knife lying flat, pointing north: the handle, the blade narrowing to a round tip."""
    c = "pi_cutlery"
    out = [(x - 0.28, y0, z + 0.3, x + 0.28, y0 + 0.18, z + 3.0, c), (x - 0.3, y0, z - 2.2, x + 0.3, y0 + 0.08, z + 0.3, c),
           (x - 0.2, y0, z - 2.6, x + 0.25, y0 + 0.08, z - 2.2, c)]
    return spin(out, angle, (x, y0, z)) if angle else out


# ---------------------------------------------------------------------------------------------- the pen cup

def pen_cup(x, z, y0):
    """A pen cup with two pens, a pencil and a highlighter. Each stands in its own slice of the cup (front to
    back), its foot on the far side of the middle and leaning across, so no two touch and none goes through a wall."""
    w, h, t = 2.6, 2.6, 0.25
    c = "pi_pen_cup"
    out = [(x, y0, z, x + w, y0 + 0.25, z + w, c),
           (x, y0, z, x + t, y0 + h, z + w, c), (x + w - t, y0, z, x + w, y0 + h, z + w, c),
           (x + t, y0, z, x + w - t, y0 + h, z + t, c), (x + t, y0, z + w - t, x + w - t, y0 + h, z + w, c)]
    cx, cz, yb = x + w / 2, z + w / 2, y0 + 0.25
    for dz, lean, body, tip, length, th in ((-0.66, -1, "pi_pen_blue", "pi_pen_black", 4.6, 0.3),
                                            (-0.22, 1, "pi_pencil", "pi_eraser", 5.0, 0.32),
                                            (0.22, -1, "pi_pen_black", "pi_chrome", 4.3, 0.3),
                                            (0.66, 1, "pi_highlighter", "pi_highlighter_cap", 3.7, 0.4)):
        bx, pz = cx - 0.4 * lean, cz + dz
        angle = -22.5 * lean           # about z: a positive angle tips the top toward -x
        o = (bx, yb, pz)
        out.append(turned((bx - th / 2, yb, pz - th / 2, bx + th / 2, yb + length - 0.7, pz + th / 2, body), "z", angle, o))
        out.append(turned((bx - th / 2 - 0.03, yb + length - 0.7, pz - th / 2 - 0.03, bx + th / 2 + 0.03, yb + length,
                           pz + th / 2 + 0.03, tip), "z", angle, o))
    return out


# ---------------------------------------------------------------------------------------------- the fridge

# A stainless bottom-freezer fridge two blocks tall. The cabinet and the door (proud of it on a dark gasket) are in
# every block; the bottom block adds the freezer drawer, its handle and the kick grille; the top block adds the top
# of the door's bar handle, a maker's badge and a couple of notes held up by magnets.
FRIDGE_BODY = [
    (1.5, 0, 2, 14.5, 16, 14.5, {"*": "pi_fridge_side", "south": "pi_gasket"}),
    (1.6, 0, 14.5, 14.4, 16, 14.75, "pi_gasket"),
    (1.5, 0, 14.75, 14.5, 16, 15.3, {"*": "pi_fridge_door", "south": "pi_fridge_door"}),
]
FRIDGE_PLINTH = [
    (1.7, 0, 13.6, 14.3, 0.9, 15.32, {"*": "pi_gasket", "south": "pi_kick_grille!"}),
    # The freezer drawer, proud of the door line, with the gap above it.
    (1.5, 0.9, 15.3, 14.5, 9.6, 15.5, {"*": "pi_fridge_door", "south": "pi_fridge_door"}),
    (1.5, 9.6, 15.3, 14.5, 9.95, 15.36, "pi_gasket"),
    (3.5, 8.0, 15.5, 4.1, 8.6, 16.3, "pi_steel_bar"), (11.9, 8.0, 15.5, 12.5, 8.6, 16.3, "pi_steel_bar"),
    (3.2, 7.8, 16.3, 12.8, 8.8, 16.9, "pi_steel_bar"),
    # The door's bar handle, the bottom of it and its lower stand-off.
    (12.1, 12.5, 15.3, 12.7, 13.1, 16.2, "pi_steel_bar"),
    (11.95, 12.0, 16.2, 12.85, 16, 16.9, "pi_steel_bar"),
]
FRIDGE_CAP = [
    (11.95, 0, 16.2, 12.85, 10.5, 16.9, "pi_steel_bar"),
    (12.1, 9.4, 15.3, 12.7, 10.0, 16.2, "pi_steel_bar"),
    (1.4, 15.7, 1.9, 14.6, 16, 15.3, "pi_fridge_side"),
    (5.6, 13.6, 15.3, 10.4, 14.3, 15.36, "pi_fridge_badge!"),
    # Notes on the door under magnets.
    (3.2, 5.0, 15.3, 6.4, 9.2, 15.34, {"*": "pi_paper", "south": "pi_fridge_note_0!"}),
    (4.3, 8.7, 15.34, 5.3, 9.5, 15.6, "pi_magnet_red"),
    (7.2, 3.2, 15.3, 10.0, 6.8, 15.34, {"*": "pi_paper", "south": "pi_fridge_note_1!"}),
    (8.2, 6.4, 15.34, 9.0, 7.1, 15.6, "pi_magnet_blue"),
]


# ---------------------------------------------------------------------------------------------- food
# Each list sits on the cafe table's top: y 12.5, the top covering x and z 2 .. 14.

T = 12.5


def plate(cx, cz, r, tex="pi_plate_c"):
    """A plate: a thin round with a raised rim (the rim drawn on its top where it lies)."""
    return cyl(cx, cz, r, T, T + 0.3, {"*": "pi_plate_side", "up": tex, "down": "pi_plate_side"})


def soda_can(cx, cz):
    """A can: the printed body, the silver shoulder and the top with its ring pull."""
    return (cyl(cx, cz, 0.85, T, T + 0.2, "pi_can_silver") + cyl(cx, cz, 0.9, T + 0.2, T + 2.8, {"*": "pi_can_body", "up": "pi_can_silver"})
            + cyl(cx, cz, 0.72, T + 2.8, T + 3.1, {"*": "pi_can_silver", "up": "pi_can_top_c"}))


def takeout():
    """Takeout noodles: a paper pail widening as it rises, its four flaps folded open (the long ones turned out
    over the sides, the short ones up), the wire handle, the noodles inside with broccoli, carrot and chicken,
    chopsticks standing in it. The pail stands in the middle of the table (so the picture on its sides lines up)."""
    out = []
    cx = cz = 8
    for k in range(6):                                  # the pail, 0.6 high a step, 0.14 wider each side a step
        hw = 1.75 + k * 0.14
        out.append((cx - hw, T + k * 0.6, cz - hw, cx + hw, T + (k + 1) * 0.6, cz + hw,
                    {"*": "pi_takeout_side", "up": "pi_takeout_side", "down": "pi_takeout_side"}))
    top, hw = T + 3.6, 1.75 + 5 * 0.14
    # The food: the noodles heaped a little, with the bits on it.
    out.append((cx - hw + 0.15, top - 0.6, cz - hw + 0.15, cx + hw - 0.15, top + 0.25, cz + hw - 0.15,
                {"*": "pi_noodles", "up": "pi_noodles"}))
    out.append((cx - 1.4, top + 0.25, cz - 1.2, cx + 1.2, top + 0.6, cz + 1.3, {"*": "pi_noodles", "up": "pi_noodles"}))
    for bx, bz, kind in ((-1.2, -0.8, "broccoli"), (0.7, 0.5, "broccoli"), (0.4, -1.3, "chicken"), (-0.6, 1.0, "chicken"),
                         (1.3, -0.4, "carrot"), (-1.6, 0.6, "carrot")):
        x, z = cx + bx, cz + bz
        if kind == "broccoli":
            out += [(x - 0.15, top + 0.2, z - 0.15, x + 0.15, top + 0.8, z + 0.15, "pi_broccoli_stem"),
                    (x - 0.45, top + 0.8, z - 0.45, x + 0.45, top + 1.25, z + 0.45, {"*": "pi_broccoli", "up": "pi_broccoli"})]
        elif kind == "chicken":
            out.append(turned((x - 0.5, top + 0.2, z - 0.3, x + 0.5, top + 0.7, z + 0.3, "pi_chicken"), "y", 22.5, (x, top, z)))
        else:
            out.append(turned((x - 0.5, top + 0.25, z - 0.1, x + 0.5, top + 0.45, z + 0.1, "pi_carrot"), "y", -45, (x, top, z)))
    # The flaps: the long ones (north and south) hinged at the rim and turned out, the short ones standing up.
    # (A hair narrower than the pail's rim, so their ends don't share its sides' planes.)
    out.append(turned((cx - hw + 0.03, top, cz + hw - 0.12, cx + hw - 0.03, top + 1.5, cz + hw, "pi_takeout_side"), "x", 45, (cx, top, cz + hw)))
    out.append(turned((cx - hw + 0.03, top, cz - hw, cx + hw - 0.03, top + 1.5, cz - hw + 0.12, "pi_takeout_side"), "x", -45, (cx, top, cz - hw)))
    out.append(turned((cx + hw - 0.12, top, cz - 1.0, cx + hw, top + 1.1, cz + 1.0, "pi_takeout_side"), "z", -22.5, (cx + hw, top, cz)))
    out.append(turned((cx - hw, top, cz - 1.0, cx - hw + 0.12, top + 1.1, cz + 1.0, "pi_takeout_side"), "z", 22.5, (cx - hw, top, cz)))
    # The wire handle folded down along the east and west sides.
    out += [(cx + hw + 0.02, top - 1.6, cz - 0.05, cx + hw + 0.12, top - 0.2, cz + 0.05, "pi_wire"),
            (cx - hw - 0.12, top - 1.6, cz - 0.05, cx - hw - 0.02, top - 0.2, cz + 0.05, "pi_wire")]
    # Chopsticks standing in the noodles, leaning back.
    for dx in (-0.3, 0.3):
        out.append(turned((cx + dx - 0.15, top - 0.4, cz - 0.15, cx + dx + 0.15, top + 4.6, cz + 0.15, "pi_chopstick"),
                          "x", -22.5, (cx + dx, top, cz)))
    out += soda_can(12.0, 11.8)
    out += [(2.6, T, 10.4, 5.6, T + 0.1, 13.2, "pi_napkin"), (3.0, T + 0.1, 11.0, 5.2, T + 0.18, 12.6, "pi_soy_packet")]
    return out


def gyro_plate():
    """A gyro plate: the wrap in its paper, cut end showing the meat, tomato, onion and tzatziki; fries; a tomato and
    cucumber salad; a pot of tzatziki; a fork."""
    out = plate(8, 8, 5.2)
    y = T + 0.3
    wrap = (cyl_x(y + 1.1, 7.0, 1.1, 3.6, 9.8, {"*": "pi_pita", "west": "pi_gyro_end!", "east": "pi_gyro_end!"})
            + cyl_x(y + 1.1, 7.0, 1.17, 6.6, 10.2, {"*": "pi_wrap_paper", "west": "pi_wrap_paper", "east": "pi_wrap_paper"}))
    out += spin(wrap, 22.5, (7, y, 7))
    r = random.Random(81)
    for k in range(9):                                 # fries in a loose pile
        x, z = 9.6 + r.uniform(-1.2, 1.2), 10.4 + r.uniform(-1.0, 0.8)
        lift = (0.0 if k < 6 else 0.32) + k * 0.025      # each a hair higher, so crossing fries never share a top
        out.append(turned((x - 1.1, y + lift, z - 0.16, x + 1.1, y + lift + 0.32, z + 0.16, "pi_fry"),
                          "y", r.choice((-45, -22.5, 0, 22.5, 45)), (x, y, z)))
    for k, (x, z, t) in enumerate(((5.2, 10.4, "pi_tomato_wedge"), (6.2, 11.2, "pi_tomato_wedge"), (4.6, 11.4, "pi_cucumber"),
                                   (5.8, 9.6, "pi_cucumber"), (6.6, 10.2, "pi_onion"))):
        h = 0.5 + k * 0.03
        out.append(turned((x - 0.45, y, z - 0.3, x + 0.45, y + h, z + 0.3, t), "y", 22.5 if t != "pi_onion" else -22.5, (x, y, z)))
    out += cyl(11.2, 5.0, 0.9, y, y + 0.9, {"*": "pi_ramekin", "up": "pi_tzatziki_c"})
    out += fork(13.4, 9.0, T, 0)
    return out


def burger():
    """A burger and fries: the bun, patty, melted cheese, lettuce and tomato, a sesame top bun domed in three
    steps; fries standing in their carton; a drink with a lid and straw. On a sheet of wax paper."""
    out = [turned((3.4, T, 3.6, 10.6, T + 0.06, 10.8, "pi_wax_paper"), "y", 22.5, (7, T, 7.2))]
    cx, cz, y = 7.0, 7.2, T + 0.06
    out += cyl(cx, cz, 2.0, y, y + 0.6, {"*": "pi_bun_side", "up": "pi_bun_cut", "down": "pi_bun_cut"})
    out += cyl(cx, cz, 2.15, y + 0.6, y + 1.15, {"*": "pi_patty", "up": "pi_patty", "down": "pi_patty"})
    out.append(turned((cx - 1.9, y + 1.15, cz - 1.9, cx + 1.9, y + 1.3, cz + 1.9, "pi_cheese"), "y", 22.5, (cx, y, cz)))
    out += cyl(cx, cz, 2.25, y + 1.3, y + 1.5, "pi_lettuce")
    out += cyl(cx, cz, 1.75, y + 1.5, y + 1.75, {"*": "pi_tomato", "up": "pi_tomato_slice"})
    out += cyl(cx, cz, 2.0, y + 1.75, y + 2.45, {"*": "pi_bun_side", "down": "pi_bun_cut"})
    out += cyl(cx, cz, 1.7, y + 2.45, y + 2.95, {"*": "pi_bun_top", "up": "pi_bun_top"})
    out += cyl(cx, cz, 1.1, y + 2.95, y + 3.2, {"*": "pi_bun_top", "up": "pi_bun_top"})
    # The fries carton (red, widening upward) with fries standing in it.
    for k in range(3):
        hw, hd = 1.05 + k * 0.12, 0.65 + k * 0.08
        out.append((11.2 - hw, T + k * 0.9, 4.8 - hd, 11.2 + hw, T + (k + 1) * 0.9, 4.8 + hd,
                    {"*": "pi_fries_carton", "up": "pi_fries_carton", "south": "pi_fries_carton_front!"}))
    r = random.Random(82)
    for k in range(11):
        x, z = 11.2 + (k % 4 - 1.5) * 0.5 + r.uniform(-0.1, 0.1), 4.8 + (k // 4 - 1) * 0.42 + k * 0.013
        h = r.uniform(1.0, 2.0)
        out.append(turned((x - 0.15, T + 2.2, z - 0.15, x + 0.15, T + 2.7 + h, z + 0.15, "pi_fry"),
                          r.choice("xz"), r.choice((-22.5, 0, 0, 22.5)), (x, T + 2.7, z)))
    # The drink.
    out += cyl(11.6, 11.2, 1.0, T, T + 1.8, "pi_cup_red") + cyl(11.6, 11.2, 1.15, T + 1.8, T + 3.4, "pi_cup_red")
    out += cyl(11.6, 11.2, 1.25, T + 3.4, T + 3.65, {"*": "pi_cup_lid_side", "up": "pi_cup_lid_c"})
    out.append(turned((11.45, T + 3.0, 11.05, 11.75, T + 6.0, 11.35, "pi_straw"), "z", -22.5, (11.6, T + 3.5, 11.2)))
    return out


def salad():
    """A salad: a white bowl (narrow foot, wide lip) full of leaves, with cherry tomatoes, cucumber, red onion,
    feta and croutons on top and leaves standing up; a fork and knife on a napkin; a bottle of water."""
    cx, cz = 7.0, 7.4
    out = cyl(cx, cz, 2.1, T, T + 0.9, "pi_bowl") + cyl(cx, cz, 2.9, T + 0.9, T + 1.9, {"*": "pi_bowl", "up": "pi_bowl_rim"})
    y = T + 1.9
    out += cyl(cx, cz, 2.5, y, y + 0.45, {"*": "pi_leaves", "up": "pi_leaves"})
    out += cyl(cx, cz, 1.7, y + 0.45, y + 0.8, {"*": "pi_leaves", "up": "pi_leaves"})
    r = random.Random(83)
    for k in range(7):                                    # leaves standing up out of the heap, this way and that
        a = k / 7 * math.tau
        x, z = cx + math.cos(a) * 1.4, cz + math.sin(a) * 1.4
        axis = "x" if k % 2 else "z"
        box = (x - 0.6, y + 0.3, z - 0.05, x + 0.6, y + 1.4, z + 0.05) if axis == "x" else (x - 0.05, y + 0.3, z - 0.6, x + 0.05, y + 1.4, z + 0.6)
        out.append(turned(box + ("pi_leaf",), axis, r.choice((-22.5, 22.5)), (x, y + 0.3, z)))
    for x, z, t, s in ((-0.9, -0.6, "pi_cherry_tomato", 0.7), (0.8, 0.5, "pi_cherry_tomato", 0.7), (0.2, -1.4, "pi_cherry_tomato", 0.7),
                       (-1.3, 0.9, "pi_cucumber", 0.8), (1.2, -0.7, "pi_cucumber", 0.8), (-0.2, 1.3, "pi_feta", 0.5),
                       (0.5, -0.3, "pi_feta", 0.5), (-0.5, 0.2, "pi_crouton", 0.6), (1.5, 1.0, "pi_crouton", 0.6)):
        x, z = cx + x, cz + z
        hgt = s if t == "pi_cherry_tomato" else 0.25 if t == "pi_cucumber" else s * 0.8
        out.append(turned((x - s / 2, y + 0.75, z - s / 2, x + s / 2, y + 0.75 + hgt, z + s / 2, t), "y", 22.5, (x, y, z)))
    out.append(turned((cx - 0.9, y + 0.85, cz - 0.08, cx + 0.9, y + 0.95, cz + 0.08, "pi_red_onion"), "y", 45, (cx, y, cz)))
    # Cutlery on a napkin.
    out.append((10.2, T, 5.0, 12.6, T + 0.08, 12.4, "pi_napkin"))
    out += fork(10.9, 9.0, T + 0.08) + knife(11.9, 9.0, T + 0.08)
    # A bottle of water: the clear body with its label, the shoulder, the neck, the cap.
    bx, bz = 4.0, 12.0
    out += [(bx - 0.8, T, bz - 0.8, bx + 0.8, T + 4.0, bz + 0.8, "pi_bottle_clear"),
            (bx - 0.84, T + 1.3, bz - 0.84, bx + 0.84, T + 2.7, bz + 0.84, "pi_water_label"),
            (bx - 0.6, T + 4.0, bz - 0.6, bx + 0.6, T + 4.5, bz + 0.6, "pi_bottle_clear"),
            (bx - 0.35, T + 4.5, bz - 0.35, bx + 0.35, T + 5.0, bz + 0.35, "pi_bottle_clear"),
            (bx - 0.42, T + 5.0, bz - 0.42, bx + 0.42, T + 5.6, bz + 0.42, {"*": "pi_cap_ridged", "up": "pi_cap_blue"})]
    return out


def _segment(cx, cz, y, length, width, h, angle):
    """One plump piece of a croissant along x: the lower body and a narrower top (a rounded section)."""
    o = (cx, y, cz)
    parts = [(cx - length / 2, y, cz - width / 2, cx + length / 2, y + h * 0.6, cz + width / 2, "pi_croissant"),
             (cx - length / 2 + 0.1, y + h * 0.6, cz - width / 2 + 0.25, cx + length / 2 - 0.1, y + h, cz + width / 2 - 0.25,
              "pi_croissant")]
    return [turned(p, "y", angle, o) for p in parts] if angle else parts


def croissant():
    """Coffee and a croissant: the croissant a crescent on a side plate (a fat middle, the arms bending back, the
    thin tips), a sugar stick and a stirrer, the mug (its steam rises from FOOD_STEAM)."""
    px, pz = 6.4, 9.0
    out = plate(px, pz, 3.4, "pi_plate_small")
    y = T + 0.3
    out += _segment(px, pz + 0.4, y, 2.4, 2.0, 1.6, 0)
    for side in (1, -1):
        a1, a2 = 22.5 * side, 45 * side            # a positive turn about y swings +x toward -z
        ex, ez = px + 1.2 * side, pz + 0.4
        c1 = (ex + 0.9 * math.cos(math.radians(22.5)) * side, ez - 0.9 * math.sin(math.radians(22.5)))
        out += _segment(c1[0], c1[1], y, 1.8, 1.6, 1.25, a1)
        e1 = (ex + 1.66 * side, ez - 0.69)
        c2 = (e1[0] + 0.6 * 0.707 * side, e1[1] - 0.6 * 0.707)
        out += _segment(c2[0], c2[1], y, 1.2, 1.0, 0.8, a2)
    out += [(10.2, T, 9.4, 10.6, T + 0.4, 12.8, "pi_sugar_stick"), (11.2, T, 9.2, 11.4, T + 0.08, 13.0, "pi_stirrer")]
    out += mug(10.5, 5, T)
    return out


FOOD = [[], takeout(), gyro_plate(), burger(), salad(), croissant()]
FOOD_STEAM = {5: (11.7, 15.3, 6.2)}


# ---------------------------------------------------------------------------------------------- the meeting table

def _inside(x, z, a=39, b=23, cx=40, cz=24, margin=1.0):
    return ((x - cx) / (a - margin)) ** 2 + ((z - cz) / (b - margin)) ** 2 <= 1


def _fit(x, z, hw, hd, north):
    """Move an item's middle in from the edge (toward the table's middle line) until its corners are on the top."""
    for _ in range(60):
        if all(_inside(x + sx * hw, z + sz * hd) for sx in (-1, 1) for sz in (-1, 1)):
            return x, z
        z += 0.25 if north else -0.25
        x += 0.15 if x < 40 else -0.15
    return x, z


def meeting_items(version):
    """The oval meeting table (80 x 48 pixels, its top at y 13) set out for a meeting: at each of the ten places
    something (papers about money and management, a coffee, a notepad and pen, a laptop) or nothing; down the
    middle a speakerphone, a coffee pot, a plate of biscuits. Turned to whoever sits there."""
    r = random.Random(2600 + version)
    y = 13
    out = []
    for k in range(5):
        for north in (True, False):
            x = 8 + 16 * k
            edge = 24 - 23 * math.sqrt(max(0.0, 1 - ((x - 40) / 39) ** 2))
            z = edge + 4.5 if north else 48 - edge - 4.5
            what = r.choice(("doc", "doc+mug", "mug", "notepad", "laptop", "doc+notepad", "", "doc+mug"))
            turn = "@180" if north else ""
            far = 1 if north else -1                  # toward the middle of the table, away from the sitter
            if "laptop" in what:
                x, z = _fit(x, z, 2.6, 2.0, north)
                out += [(x - 2.4, y, z - 1.6, x + 2.4, y + 0.3, z + 1.6, {"*": "pi_laptop", "up": "pi_keyboard!" + turn})]
                lz = z + 1.6 if north else z - 1.9
                out.append((x - 2.4, y + 0.3, lz, x + 2.4, y + 3.6, lz + 0.3,
                            {"*": "pi_laptop", ("north" if north else "south"): "pi_office_screen!"}))
            if "doc" in what:
                dx = -1.8 if "+" in what else 0
                x0, z0 = _fit(x + dx, z, 2.2, 2.8, north)
                kind = r.choice((8, 9, 10, 11))
                out.append(turned((x0 - 2.2, y, z0 - 2.8, x0 + 2.2, y + 0.12, z0 + 2.8, {"*": "pi_paper", "up": f"pi_doc_{kind}!{turn}"}),
                                  "y", r.choice((-22.5, 0, 0, 22.5)), (x0, y, z0)))
            if "notepad" in what:
                dx = 2.2 if "+" in what else 0
                x0, z0 = _fit(x + dx, z, 1.8, 2.4, north)
                out.append((x0 - 1.7, y, z0 - 2.3, x0 + 1.7, y + 0.25, z0 + 2.3, {"*": "pi_paper", "up": "pi_notepad!" + turn}))
                out.append(turned((x0 + 1.9, y, z0 - 1.6, x0 + 2.2, y + 0.3, z0 + 1.6, "pi_pen_blue"), "y", 22.5, (x0 + 2, y, z0)))
            if "mug" in what:
                dx = 2.4 if "+" in what else 0
                x0, z0 = _fit(x + dx, z + far * 1.0, 1.8, 1.4, north)
                out += mug(x0 - 1.2, z0 - 1.2, y)
    # Down the middle: the speakerphone, a coffee pot, a plate of biscuits.
    out += [turned((38, y, 22, 42, y + 0.6, 26, {"*": "pi_phone_body", "up": "pi_speakerphone!"}), "y", 45, (40, y, 24))]
    out += cyl(26, 24, 1.3, y, y + 3.6, "pi_carafe") + cyl(26, 24, 1.0, y + 3.6, y + 4.4, {"*": "pi_chrome", "up": "pi_carafe_top"})
    out += [(27.1, y + 2.6, 23.7, 28.1, y + 3.3, 24.3, "pi_carafe"), (24.6, y + 0.6, 23.7, 25.0, y + 3.2, 24.3, "pi_carafe")]
    if version != 1:
        out += cyl(54, 24, 2.6, y, y + 0.3, {"*": "pi_plate_side", "up": "pi_plate_side"})
        for k in range(6):
            a = k / 6 * math.tau
            out += cyl(54 + math.cos(a) * 1.3, 24 + math.sin(a) * 1.3, 0.6, y + 0.3, y + 0.55, {"*": "pi_biscuit", "up": "pi_biscuit"})
    else:
        out += cyl(54, 24, 1.0, y, y + 3.4, "pi_bottle_clear") + cyl(54, 24, 0.5, y + 3.4, y + 4.2, "pi_bottle_clear")
        for dx, dz in ((-2, -1.6), (2, -1.6), (-2, 1.6), (2, 1.6)):
            out += cyl(54 + dx, 24 + dz, 0.6, y, y + 1.6, "pi_glass_tumbler")
    return out


def slice_into(boxes, c, r):
    """The boxes whose middles stand on block (c, r) of a piece, in that block's own coordinates."""
    out = []
    for b in boxes:
        mx, mz = (b[0] + b[3]) / 2, (b[2] + b[5]) / 2
        if c * 16 <= mx < c * 16 + 16 and r * 16 <= mz < r * 16 + 16:
            nb = (b[0] - c * 16, b[1], b[2] - r * 16, b[3] - c * 16, b[4], b[5] - r * 16, b[6])
            if len(b) > 7:
                rot = dict(b[7]["rotation"])
                o = rot["origin"]
                rot["origin"] = [o[0] - c * 16, o[1], o[2] - r * 16]
                nb += ({"rotation": rot},)
            out.append(nb)
    return out


# ---------------------------------------------------------------------------------------------- the lounge table

LW, LR = 3, 2     # the plant manager's coffee table: three blocks across, two deep


def lounge_whole():
    """The coffee table over its 48 x 32 pixels: a thick oval of glass at knee height on two walnut drums with brass
    bands, two big books and a bowl of green apples on it. Returns (glass strips, everything else)."""
    a, b, cx, cz = 23.0, 15.0, 24.0, 16.0
    glass = []
    for z in range(32):
        gz = z + 0.5 - cz
        if abs(gz) >= b:
            continue
        half = a * math.sqrt(1 - (gz / b) ** 2)
        x0, x1 = round((cx - half) * 4) / 4, round((cx + half) * 4) / 4
        first = abs(z - 1 + 0.5 - cz) >= b      # no strip before this one: its north edge shows
        last = abs(z + 1 + 0.5 - cz) >= b
        glass.append((x0, 6.8, z, x1, 7.6, z + 1, first, last))
    rest = []
    for dx in (14, 34):
        rest += cyl(dx, 16, 4.6, 0.5, 6.4, "pi_walnut_drum")
        rest += cyl(dx, 16, 4.9, 0, 0.5, "pi_brass") + cyl(dx, 16, 4.8, 6.4, 6.8, "pi_brass")
    y = 7.6
    rest.append(turned((12.5, y, 10.5, 21.5, y + 0.9, 17.5, {"*": "pi_page_edge", "up": "pi_coffee_book_0!", "down": "pi_book_cloth",
                                                            "west": "pi_book_cloth"}), "y", 22.5, (17, y, 14)))
    rest.append(turned((14, y + 0.9, 11.5, 20.5, y + 1.6, 16.5, {"*": "pi_page_edge", "up": "pi_coffee_book_1!", "down": "pi_book_cloth",
                                                                "west": "pi_book_cloth"}), "y", -22.5, (17, y, 14)))
    rest += cyl(36, 20, 1.8, y, y + 0.6, "pi_bowl_dark") + cyl(36, 20, 2.6, y + 0.6, y + 1.6, {"*": "pi_bowl_dark", "up": "pi_bowl_dark_rim"})
    for ax, az in ((35.2, 19.4), (36.8, 19.6), (36.0, 20.9)):
        rest += cyl(ax, az, 0.8, y + 0.9, y + 2.4, {"*": "pi_apple", "up": "pi_apple_top"})
    return glass, rest


def lounge_part(c, r):
    glass, rest = lounge_whole()
    out = []
    for x0, y0, z, x1, y1, z1, first, last in glass:
        if not r * 16 <= z < r * 16 + 16:
            continue
        a, b = max(x0, c * 16), min(x1, c * 16 + 16)
        if b - a <= 0.01:
            continue
        faces = {"up": "pi_glass_top", "down": "pi_glass_top",
                 "west": "pi_glass_edge" if a == x0 else None, "east": "pi_glass_edge" if b == x1 else None,
                 "north": "pi_glass_edge" if first else None, "south": "pi_glass_edge" if last else None}
        out.append((a - c * 16, y0, z - r * 16, b - c * 16, y1, z1 - r * 16, faces))
    return out + slice_into(rest, c, r)


# ---------------------------------------------------------------------------------------------- textures

def _img(c, size=S):
    a = np.zeros((size, size, 4), np.uint8)
    a[..., :3] = c[:3]
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _speckle(c, seed, colours, count, size=S):
    img = _img(c, size)
    d = ImageDraw.Draw(img)
    rr = random.Random(seed)
    for _ in range(count):
        x, y = rr.randrange(size), rr.randrange(size)
        d.rectangle([x, y, x + rr.randint(0, 2), y + rr.randint(0, 2)], fill=rr.choice(colours))
    return img


def _brushed(c, seed, vertical=True):
    rr = np.random.default_rng(seed)
    a = np.zeros((S, S, 4), np.uint8)
    streak = rr.normal(0, 6, S)
    base = np.array(c, float)
    for i in range(S):
        col = np.clip(base + streak[i] + rr.normal(0, 2, 3), 0, 255)
        if vertical:
            a[:, i, :3] = col
        else:
            a[i, :, :3] = col
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _round_top(cx, cz, r, inner, ring, outside=None, seed=0):
    """A top drawn where it lies in the block: a round of colour `inner` with a ring `ring` round its edge."""
    img = _img(outside or ring)
    d = ImageDraw.Draw(img)
    k = S / 16
    d.ellipse([(cx - r) * k, (cz - r) * k, (cx + r) * k, (cz + r) * k], fill=ring)
    rin = r * 0.8
    d.ellipse([(cx - rin) * k, (cz - rin) * k, (cx + rin) * k, (cz + rin) * k], fill=inner)
    return img


def _takeout_side():
    """The pail's paperboard: white, a red pagoda and red band in the middle of each side (which is where the
    sides are: the pail stands in the middle of the block)."""
    img = _img((246, 244, 238))
    d = ImageDraw.Draw(img)
    red = (196, 28, 36, 255)
    v = lambda y: (16 - y) * 8          # model height to texture row
    d.rectangle([0, v(T + 3.3), S, v(T + 3.1)], fill=red)
    cx, top, bot = 64, v(T + 2.7), v(T + 0.6)
    for k, wdt in enumerate((7, 10, 13)):
        y = top + k * (bot - top) / 3
        d.polygon([(cx - wdt, y + 5), (cx + wdt, y + 5), (cx + wdt - 3, y), (cx - wdt + 3, y)], fill=red)
        d.rectangle([cx - wdt + 3, y + 5, cx + wdt - 3, y + (bot - top) / 3], outline=red, width=1)
    for x in range(0, S, 32):
        d.line([(x, 0), (x, S)], fill=(228, 226, 220, 255))
    return img


def _noodles():
    img = _img((214, 168, 92))
    d = ImageDraw.Draw(img)
    rr = random.Random(1801)
    for _ in range(90):
        x, y = rr.randint(-10, S), rr.randint(-10, S)
        col = rr.choice([(236, 200, 120, 255), (200, 150, 76, 255), (246, 214, 140, 255), (180, 124, 60, 255)])
        d.arc([x, y, x + rr.randint(16, 40), y + rr.randint(8, 20)], rr.randint(0, 180), rr.randint(200, 360), fill=col, width=2)
    for _ in range(40):
        x, y = rr.randint(0, S), rr.randint(0, S)
        d.ellipse([x, y, x + 2, y + 2], fill=(110, 60, 30, 255))      # soy sauce glaze
    return img


def _sesame_bun():
    img = _img((194, 120, 52))
    a = np.asarray(img).astype(float)
    yy, xx = np.mgrid[0:S, 0:S]
    shade = 1.0 + 0.18 * np.exp(-(((xx - 56) ** 2 + (yy - 52) ** 2) / 1800))
    a[..., :3] = np.clip(a[..., :3] * shade[..., None], 0, 255)
    img = Image.fromarray(a.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    rr = random.Random(1802)
    for _ in range(70):
        x, y = rr.randint(0, S - 4), rr.randint(0, S - 4)
        d.ellipse([x, y, x + 3, y + 2], fill=(250, 238, 200, 255))
    return img


def _patty():
    return _speckle((86, 48, 30), 1803, [(60, 30, 18, 255), (110, 66, 40, 255), (40, 22, 14, 255)], 900)


def _lettuce():
    img = _speckle((112, 182, 64), 1804, [(150, 210, 90, 255), (80, 150, 50, 255)], 500)
    return img


def _leaves():
    img = _img((70, 140, 50))
    d = ImageDraw.Draw(img)
    rr = random.Random(1805)
    for _ in range(80):
        x, y = rr.randint(-8, S), rr.randint(-8, S)
        col = rr.choice([(110, 180, 70), (60, 120, 40), (150, 200, 90), (90, 40, 70), (130, 190, 80)])
        d.ellipse([x, y, x + rr.randint(10, 22), y + rr.randint(6, 14)], fill=col + (255,))
        d.line([(x + 2, y + 5), (x + 14, y + 6)], fill=(200, 230, 160, 255))
    return img


def _leaf():
    img = _img((90, 160, 56))
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, S)], fill=(190, 226, 150, 255), width=4)
    for y in range(10, S, 18):
        d.line([(64, y), (20, y - 12)], fill=(150, 200, 110, 255), width=2)
        d.line([(64, y), (108, y - 12)], fill=(150, 200, 110, 255), width=2)
    return img


def _cucumber():
    img = _img((60, 120, 50))
    d = ImageDraw.Draw(img)
    d.ellipse([12, 12, 116, 116], fill=(200, 230, 170, 255))
    for k in range(6):
        a = k / 6 * math.tau
        d.ellipse([60 + math.cos(a) * 22, 60 + math.sin(a) * 22, 68 + math.cos(a) * 22, 68 + math.sin(a) * 22], fill=(240, 246, 220, 255))
    return img


def _croissant():
    img = _img((204, 136, 58))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 26):
        d.rectangle([x, 0, x + 6, S], fill=(160, 94, 36, 255))
        d.rectangle([x + 8, 0, x + 14, S], fill=(230, 170, 84, 255))
    a = np.asarray(img).astype(float)
    yy = np.mgrid[0:S, 0:S][0]
    a[..., :3] *= (1.1 - 0.25 * yy / S)[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _fries_carton_front():
    img = _img((200, 30, 34))
    d = ImageDraw.Draw(img)
    d.ellipse([34, 28, 94, 88], fill=(250, 200, 40, 255))
    d.arc([48, 46, 80, 74], 20, 160, fill=(200, 30, 34, 255), width=6)
    return img


def _can_body():
    img = _img((196, 24, 34))
    d = ImageDraw.Draw(img)
    v = lambda y: int((16 - y) * 8)
    for x in range(0, S, 32):
        d.arc([x - 20, v(T + 2.0) - 10, x + 36, v(T + 1.0) + 10], 200, 340, fill=(250, 250, 250, 255), width=4)
    d.rectangle([0, v(T + 2.8), S, v(T + 2.6)], fill=(170, 20, 28, 255))
    return img


def _water_label():
    img = _img((236, 244, 250))
    d = ImageDraw.Draw(img)
    v = lambda y: int((16 - y) * 8)
    d.rectangle([0, v(T + 2.7), S, v(T + 2.3)], fill=(30, 90, 170, 255))
    d.rectangle([0, v(T + 1.6), S, v(T + 1.3)], fill=(30, 90, 170, 255))
    d.polygon([(0, v(T + 1.9)), (40, v(T + 2.1)), (80, v(T + 1.8)), (128, v(T + 2.05)), (128, v(T + 1.6)), (0, v(T + 1.6))],
              fill=(120, 180, 230, 255))
    return img


def _bottle_clear():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (210, 232, 246)
    a[..., 3] = 90
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.line([(20, 0), (20, S)], fill=(255, 255, 255, 150), width=5)
    return img


def _glass_tumbler():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (226, 240, 248)
    a[..., 3] = 70
    return Image.fromarray(a, "RGBA")


def _cap_ridged():
    img = _img((30, 80, 190))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 8):
        d.line([(x, 0), (x, S)], fill=(20, 60, 150, 255), width=2)
    return img


def _kick_grille():
    img = _img((30, 31, 34))
    d = ImageDraw.Draw(img)
    for x in range(6, S, 10):
        d.rectangle([x, 20, x + 5, 108], fill=(16, 16, 18, 255))
    return img


def _fridge_badge():
    img = Image.new("RGBA", (S, S), (210, 212, 214, 255))
    d = ImageDraw.Draw(img)
    f = O3._font(30)
    w = d.textlength("FROSTLINE", font=f)
    d.text(((S - w) / 2, 46), "FROSTLINE", font=f, fill=(60, 62, 66, 255))
    return img


def _fridge_note(k):
    img = _img((250, 236, 120) if k == 0 else (246, 246, 240))
    d = ImageDraw.Draw(img)
    f = O3._font(14)
    lines = (["CLEAN OUT", "FRIDAY 3PM", "unlabelled", "food goes!"] if k == 0 else
             ["WEEK 40", "Potluck", "Thu 12:00", "sign up ->"])
    for i, line in enumerate(lines):
        d.text((10, 14 + i * 26), line, font=f, fill=(40, 40, 46, 255))
    if k == 1:
        for i in range(3):
            d.line([(14, 112 - i * 0), (110, 112)], fill=(150, 150, 160, 255))
    return img


def _speakerphone():
    img = _img((44, 46, 50))
    d = ImageDraw.Draw(img)
    d.ellipse([24, 24, 104, 104], fill=(34, 36, 40, 255))
    for k in range(6):
        for j in range(6):
            d.ellipse([40 + k * 9, 40 + j * 9, 43 + k * 9, 43 + j * 9], fill=(20, 20, 22, 255))
    d.rectangle([52, 6, 76, 16], fill=(80, 200, 120, 255))
    return img


def _coffee_book(k):
    import paintings
    img = Image.new("RGBA", (S, S), (240, 236, 226, 255))
    pic = paintings.painted([2, 13][k], 128, 90, 3000 + k).convert("RGBA")
    img.paste(pic, (0, 0))
    d = ImageDraw.Draw(img)
    title = ["NORTHERN LIGHTS", "ARCHITECTURE"][k]
    size = 18
    while d.textlength(title, font=O3._font(size)) > S - 12:
        size -= 1
    d.text(((S - d.textlength(title, font=O3._font(size))) / 2, 98), title, font=O3._font(size), fill=(30, 30, 34, 255))
    return img


def _walnut_drum():
    img = _img((92, 58, 36))
    d = ImageDraw.Draw(img)
    rr = random.Random(1806)
    for x in range(0, S, 2):
        if rr.random() < 0.35:
            d.line([(x, 0), (x + rr.randint(-3, 3), S)], fill=(70, 42, 26, 255))
    return img


def makers():
    return {
        "pi_cutlery": lambda: _brushed((196, 200, 206), 1901, False),
        "pi_steel_bar": lambda: _brushed((188, 192, 198), 1902),
        "pi_eraser": lambda: _img((236, 140, 150)), "pi_highlighter_cap": lambda: _img((214, 200, 30)),
        "pi_fridge_side": lambda: _brushed((178, 182, 186), 1903),
        "pi_fridge_door": lambda: _brushed((204, 207, 210), 1904),
        "pi_gasket": lambda: _img((34, 35, 38)), "pi_kick_grille": _kick_grille, "pi_fridge_badge": _fridge_badge,
        "pi_fridge_note_0": lambda: _fridge_note(0), "pi_fridge_note_1": lambda: _fridge_note(1),
        "pi_magnet_red": lambda: _img((200, 40, 40)), "pi_magnet_blue": lambda: _img((40, 90, 200)),
        "pi_plate_side": lambda: _img((236, 238, 236)),
        "pi_plate_c": lambda: _round_top(8, 8, 5.2, (248, 248, 246, 255), (226, 228, 226, 255)),
        # Iris treats the _s suffix as a specular-map companion, not a base texture.
        "pi_plate_small": lambda: _round_top(6.4, 9.0, 3.4, (248, 248, 246, 255), (226, 228, 226, 255)),
        "pi_can_silver": lambda: _brushed((190, 192, 196), 1905),
        "pi_can_body": _can_body,
        "pi_can_top_c": lambda: _round_top(12.0, 11.8, 0.72, (176, 178, 182, 255), (206, 208, 212, 255)),
        "pi_takeout_side": _takeout_side, "pi_noodles": _noodles,
        "pi_broccoli": lambda: _speckle((50, 120, 40), 1807, [(80, 150, 60, 255), (30, 90, 30, 255)], 1200),
        "pi_broccoli_stem": lambda: _img((150, 190, 110)), "pi_chicken": lambda: _speckle((214, 160, 96), 1808, [(170, 110, 60, 255)], 400),
        "pi_carrot": lambda: _img((236, 120, 40)), "pi_wire": lambda: _img((180, 182, 186)),
        "pi_chopstick": lambda: _img((214, 180, 120)), "pi_napkin": lambda: _img((250, 250, 246)),
        "pi_soy_packet": lambda: _img((60, 40, 30)),
        "pi_pita": lambda: _speckle((226, 190, 130), 1809, [(196, 150, 90, 255), (240, 210, 160, 255)], 500),
        "pi_gyro_end": lambda: _gyro_end(),
        "pi_wrap_paper": lambda: _speckle((240, 236, 226), 1810, [(220, 214, 200, 255)], 300),
        "pi_fry": lambda: _speckle((236, 190, 80), 1811, [(210, 160, 60, 255), (250, 214, 120, 255)], 400),
        "pi_tomato_wedge": lambda: _speckle((210, 40, 36), 1812, [(240, 120, 90, 255)], 300),
        "pi_cucumber": _cucumber, "pi_onion": lambda: _img((236, 226, 236)),
        "pi_ramekin": lambda: _img((244, 244, 240)),
        "pi_tzatziki_c": lambda: _speckle((240, 244, 236), 1813, [(120, 170, 100, 255)], 300),
        "pi_wax_paper": lambda: _speckle((240, 232, 210), 1814, [(200, 40, 40, 255)], 60),
        "pi_bun_side": lambda: _img((204, 136, 64)), "pi_bun_cut": lambda: _speckle((236, 214, 170), 1815, [(220, 196, 150, 255)], 500),
        "pi_bun_top": _sesame_bun, "pi_patty": _patty,
        "pi_cheese": lambda: _img((250, 186, 40)), "pi_lettuce": _lettuce,
        "pi_tomato": lambda: _img((214, 46, 40)), "pi_tomato_slice": lambda: _speckle((214, 46, 40), 1816, [(240, 120, 90, 255)], 600),
        "pi_fries_carton": lambda: _img((200, 30, 34)), "pi_fries_carton_front": _fries_carton_front,
        "pi_cup_red": lambda: _speckle((200, 36, 40), 1817, [(230, 230, 230, 255)], 40),
        "pi_cup_lid_side": lambda: _img((236, 236, 232)),
        "pi_cup_lid_c": lambda: _round_top(11.6, 11.2, 1.25, (236, 236, 232, 255), (210, 210, 206, 255)),
        "pi_straw": lambda: _img((244, 244, 244)),
        "pi_bowl": lambda: _img((244, 244, 240)),
        "pi_bowl_rim": lambda: _round_top(7.0, 7.4, 2.9, (60, 130, 50, 255), (244, 244, 240, 255)),
        "pi_leaves": _leaves, "pi_leaf": _leaf,
        "pi_cherry_tomato": lambda: _speckle((214, 30, 30), 1818, [(250, 110, 100, 255)], 120),
        "pi_feta": lambda: _img((246, 246, 236)), "pi_crouton": lambda: _speckle((214, 160, 80), 1819, [(170, 110, 40, 255)], 300),
        "pi_red_onion": lambda: _img((150, 60, 120)),
        "pi_bottle_clear": _bottle_clear, "pi_water_label": _water_label, "pi_cap_ridged": _cap_ridged,
        "pi_croissant": _croissant,
        "pi_sugar_stick": lambda: _img((246, 246, 240)), "pi_stirrer": lambda: _img((214, 190, 150)),
        "pi_speakerphone": _speakerphone, "pi_carafe": lambda: _brushed((40, 42, 46), 1907),
        "pi_carafe_top": lambda: _round_top(10, 8, 1.0, (30, 30, 34, 255), (190, 192, 196, 255)),
        "pi_biscuit": lambda: _speckle((200, 150, 80), 1820, [(120, 70, 30, 255)], 200),
        "pi_glass_tumbler": _glass_tumbler,
        "pi_walnut_drum": _walnut_drum, "pi_coffee_book_0": lambda: _coffee_book(0), "pi_coffee_book_1": lambda: _coffee_book(1),
        "pi_book_cloth": lambda: _img((40, 50, 70)),
        "pi_bowl_dark": lambda: _img((40, 44, 50)),
        "pi_bowl_dark_rim": lambda: _round_top(4, 4, 2.6, (24, 26, 30, 255), (40, 44, 50, 255)),
        "pi_apple": lambda: _speckle((120, 190, 60), 1821, [(170, 210, 80, 255), (90, 150, 40, 255)], 300),
        "pi_apple_top": lambda: _speckle((120, 190, 60), 1822, [(90, 60, 30, 255)], 20),
    }


def _gyro_end():
    """The cut end of the wrap: the pita round the edge, the meat, tomato, onion, lettuce and tzatziki inside."""
    img = _img((226, 190, 130))
    d = ImageDraw.Draw(img)
    d.ellipse([10, 10, 118, 118], fill=(150, 90, 50, 255))
    rr = random.Random(1823)
    for _ in range(40):
        x, y = rr.randint(24, 100), rr.randint(24, 100)
        d.rectangle([x, y, x + 8, y + 5], fill=rr.choice([(240, 240, 230, 255), (210, 40, 40, 255), (90, 160, 60, 255), (110, 60, 30, 255)]))
    return img


SHARP = {"pi_fridge_badge", "pi_fridge_note_0", "pi_fridge_note_1", "pi_coffee_book_0", "pi_coffee_book_1",
         "pi_fries_carton_front", "pi_speakerphone"}
TRANSLUCENT = {"pi_bottle_clear", "pi_glass_tumbler"}
