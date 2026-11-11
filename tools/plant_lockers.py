"""Lockers as real 3D (Justin's walk through of 24 October 2026: the security room's lockers "as real 3D models; override
the model so the admin building updates too"; the locker room: "no scientist uniforms; lockers as real 3D models (crew
lockers, not science area)").

A locker block is a pair of full-height steel lockers side by side, two blocks high (a stack of two: plant_interior's
"stacks": the lower block's plinth, the upper block's cap), for a block facing north (its doors on the south face):

- the carcass: its two sides, the divider between the pair, the back, all painted steel; a toe-kick set back under it,
  a top with a lip standing over the doors;
- each door its own panel a little proud of the carcass, a shadow gap round it: the louvres (real slots, each under a
  little hood, the dark inside behind), a lift handle with a padlock hasp, a card holder with its name card, the hinge
  knuckles on its outer edge;
- open, the right-hand door swung right out on its hinges (its inside face to the open locker), and in the locker its
  hat shelf, the hanging rail (front to back), its floor; what is in it by kind: the plant's lab coat or the guard's
  jacket (the locker's own coat, drawn by client/LockerRenderer: here only the rail it hangs from) with a hard hat or a
  cap and radio on the shelf; the drone crew's jacket (navy, an amber band), a headset on the shelf and a kit bag.

Every face at least 0.1 px from any face of another box in the same plane (no z-fighting); 1 px is 6.25 cm.
"""
from __future__ import annotations

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_office5 as O5
import plant_office6 as O6

turned = O5.turned

# Painted steel (the kit's locker blue-grey), its inside a shade darker, the slots' dark, the chrome fittings.
SIDE, INSIDE, DOOR_FACE, DOOR_IN = "pi_locker_side", "pi_locker_inside", "pi_locker_door_plain", "pi_locker_door_back"
DARK, CHROME, CAP = "pi_pnl_black", "pi_chrome", "pi_cabinet_dark"

X0, X1 = 1.0, 15.0              # the carcass across
ZB, ZF = 2.0, 15.0              # its back and its front (the sides' and divider's front edges)
T = 0.6                         # sheet thickness (sides, divider, back)
MID0, MID1 = 7.7, 8.3           # the divider between the pair
DZ0, DZ1 = 14.75, 15.35         # a door's back and front (0.35 proud of the carcass front)
DOORS = ((1.75, 7.55), (8.45, 14.25))   # each door across: a 0.15 shadow gap to the sides and the divider
LOW_Y0, UP_Y1 = 0.8, 15.2       # a door's foot (over the plinth) and head (under the cap)
SLOT_H, SLOT_PITCH, SLOTS = 0.3, 0.6, 4
LOUVRE_UP, LOUVRE_LOW = 12.4, 1.8      # the lowest slot's foot, near the top (upper block) and near the foot (lower)
HANDLE_Y = (1.2, 3.6)           # the lift handle's plate (upper block): 1.08 to 1.23 m up
CARD_Y = (10.4, 11.6)           # the card holder (upper block)
RAIL_Y, SHELF_Y = 11.0, 12.4    # the hanging rail and the hat shelf (upper block), as the plant's open locker had them


def _door(x0, x1, y0, y1, louvre_y, hinge_left, upper, shut=True):
    """One door's boxes in one block: its panel round the louvre slots (its inside face the door's back), each slot's
    hood and, shut, the dark of the locker behind; the handle and the card holder on the upper half; the hinge
    knuckles on its outer edge."""
    DOOR = {"*": DOOR_FACE, "north": DOOR_IN}
    out = []
    lx0, lx1 = x0 + 1.0, x1 - 1.0                       # the louvres across
    ly0 = louvre_y
    ly1 = ly0 + (SLOTS - 1) * SLOT_PITCH + SLOT_H
    # the panel: left and right of the louvres, below and above them, and the bars between the slots
    out += [(x0, y0, DZ0, lx0, y1, DZ1, DOOR), (lx1, y0, DZ0, x1, y1, DZ1, DOOR),
            (lx0, y0, DZ0, lx1, ly0, DZ1, DOOR), (lx0, ly1, DZ0, lx1, y1, DZ1, DOOR)]
    for i in range(SLOTS - 1):
        sy1 = ly0 + i * SLOT_PITCH + SLOT_H
        out.append((lx0, sy1, DZ0, lx1, sy1 + SLOT_PITCH - SLOT_H, DZ1, DOOR))
    # behind the slots, shut, the dark of the locker; over each slot its hood, tipped out
    if shut:
        out.append((lx0, ly0, DZ0 - 0.25, lx1, ly1, DZ0 - 0.1, DARK))
    # (its ends 0.15 in from the slot's: nearer than the model writer's 0.09, its separation pass pushes the panel's
    # bars apart from the hoods and opens a seam across the door where its two blocks meet)
    for i in range(SLOTS):
        sy1 = ly0 + i * SLOT_PITCH + SLOT_H
        out.append(turned((lx0 + 0.15, sy1 - 0.08, DZ1, lx1 - 0.15, sy1 + 0.08, DZ1 + 0.28, DOOR_FACE), "x", -22.5, (lx0, sy1, DZ1)))
    # the free edge (handle side) and the hinge side
    free = x1 if hinge_left else x0
    hinge = x0 if hinge_left else x1
    if upper:
        hx0, hx1 = (free - 1.15, free - 0.35) if hinge_left else (free + 0.35, free + 1.15)
        # the recessed plate, the lift lever across it, the padlock hasp over it (a loop: two legs and its top)
        out += [(hx0, HANDLE_Y[0], DZ1, hx1, HANDLE_Y[1], DZ1 + 0.15, CHROME),
                (hx0 + 0.1, HANDLE_Y[0] + 1.05, DZ1 + 0.15, hx1 - 0.1, HANDLE_Y[0] + 1.35, DZ1 + 0.5, CHROME),
                (hx0 + 0.18, HANDLE_Y[1] - 0.1, DZ1 + 0.15, hx0 + 0.3, HANDLE_Y[1] + 0.5, DZ1 + 0.4, CHROME),
                (hx1 - 0.3, HANDLE_Y[1] - 0.1, DZ1 + 0.15, hx1 - 0.18, HANDLE_Y[1] + 0.5, DZ1 + 0.4, CHROME),
                (hx0 + 0.18, HANDLE_Y[1] + 0.5, DZ1 + 0.15, hx1 - 0.18, HANDLE_Y[1] + 0.62, DZ1 + 0.4, CHROME)]
        cx = (x0 + x1) / 2 + (-0.4 if hinge_left else 0.4)
        out += [(cx - 1.5, CARD_Y[0], DZ1, cx + 1.5, CARD_Y[1], DZ1 + 0.15, CHROME),
                (cx - 1.3, CARD_Y[0] + 0.2, DZ1 + 0.15, cx + 1.3, CARD_Y[1] - 0.2, DZ1 + 0.25, "pi_paper")]
    kx0, kx1 = (hinge - 0.25, hinge + 0.15) if hinge_left else (hinge - 0.15, hinge + 0.25)
    for ky in ((3.0,) if not upper else (11.8,)):
        out.append((kx0, ky, DZ1 - 0.1, kx1, ky + 1.4, DZ1 + 0.25, CHROME))
    return out


def _carcass(lower):
    """What every block of a stack has: the sides, the divider, the back; the lower block's toe-kick and floor."""
    out = [(X0, 0, ZB, X0 + T, 16, ZF, SIDE), (X1 - T, 0, ZB, X1, 16, ZF, SIDE),
           (MID0, 0, ZB + T, MID1, 16, ZF, SIDE), (X0 + T, 0, ZB, X1 - T, 16, ZB + T, INSIDE)]
    return out


def _plinth():
    """The lower block of a stack: the floor of each locker over a toe-kick set back 0.6 under the doors."""
    return [(X0 + 0.4, 0, ZB + 0.4, X1 - 0.4, 0.6, ZF - 0.7, CAP),
            (X0 + T, 0.6, ZB + T, MID0, LOW_Y0, ZF - 0.05, INSIDE), (MID1, 0.6, ZB + T, X1 - T, LOW_Y0, ZF - 0.05, INSIDE)]


def _cap():
    """The top of the stack: a lid over the carcass with a lip standing out over the doors."""
    return [(X0 - 0.2, 15.4, ZB - 0.2, X1 + 0.2, 16, ZF + 0.6, CAP),
            (X0 + T, UP_Y1, ZB + T, X1 - T, 15.4, ZF - 0.05, INSIDE)]


def closed():
    """The plant kit's FURNITURE entry for a pair of closed lockers (two blocks high)."""
    low = _door(*DOORS[0], LOW_Y0, 16, LOUVRE_LOW, True, False) + _door(*DOORS[1], LOW_Y0, 16, LOUVRE_LOW, False, False)
    up = _door(*DOORS[0], 0, UP_Y1, LOUVRE_UP, True, True) + _door(*DOORS[1], 0, UP_Y1, LOUVRE_UP, False, True)
    return {"boxes": _carcass(False), "lower": low, "upper": up, "cap": _cap(), "plinth": _plinth()}


def _swung(boxes, hx, hz):
    """A door's boxes swung 90 degrees out on its hinge at (hx, hz): its front (south) face to the east."""
    out = []
    for b in boxes:
        x0, y0, z0, x1, y1, z1, t = b[:7]
        # (x, z) -> (hx + (z - hz), hz + (hx - x)) ... the free edge (smaller x) goes out to larger z
        nx0, nx1 = sorted((hx + (z0 - hz), hx + (z1 - hz)))
        nz0, nz1 = sorted((hz + (hx - x0), hz + (hx - x1)))
        if isinstance(t, dict):
            ren = {"south": "east", "east": "north", "north": "west", "west": "south"}
            t = {ren.get(k, k): v for k, v in t.items()}
        extra = dict(b[7]) if len(b) > 7 else None
        if extra and "rotation" in extra:
            r = dict(extra["rotation"])
            ox, oy, oz = r["origin"]
            r["origin"] = [hx + (oz - hz), oy, hz + (hx - ox)]
            if r["axis"] == "x":            # old +x is new -z
                r["axis"], r["angle"] = "z", -r["angle"]
            elif r["axis"] == "z":          # old +z is new +x
                r["axis"] = "x"
            extra = {"rotation": r}
        out.append((nx0, y0, nz0, nx1, y1, nz1, t) + ((extra,) if extra else ()))
    return out


def opened(kind):
    """A pair with its right-hand locker standing open: kind "lab" (the plant's: a hard hat on the shelf, its coat the
    locker's own), "security" (the guard's: a cap and radio, black boots, the locker's own jacket) or "crew" (the drone
    crew's: their jacket on the rail, a headset on the shelf, a kit bag and boots)."""
    hx, hz = DOORS[1][1] + 0.15, DZ1        # the right-hand door's hinge
    low = _door(*DOORS[0], LOW_Y0, 16, LOUVRE_LOW, True, False)
    up = _door(*DOORS[0], 0, UP_Y1, LOUVRE_UP, True, True)
    low += _swung(_door(*DOORS[1], LOW_Y0, 16, LOUVRE_LOW, False, False, shut=False), hx, hz)
    up += _swung(_door(*DOORS[1], 0, UP_Y1, LOUVRE_UP, False, True, shut=False), hx, hz)
    # inside the open one: the hat shelf; the rail (front to back for the locker's own coat, which hangs sideways as
    # in a deep narrow locker; across for the crew jacket, which faces the door)
    cx = (MID1 + X1 - T) / 2
    up.append((MID1 + 0.05, SHELF_Y, ZB + T + 0.05, X1 - T - 0.05, SHELF_Y + 0.5, ZF - 0.4, INSIDE))
    if kind == "crew":
        up += [(MID1 + 0.3, RAIL_Y, 8.8, X1 - T - 0.3, RAIL_Y + 0.4, 9.2, CHROME)]
    else:
        up += [(cx - 0.2, RAIL_Y, ZB + T + 0.3, cx + 0.2, RAIL_Y + 0.4, ZF - 1.0, CHROME),
               (cx - 0.25, RAIL_Y + 0.4, ZB + T + 0.3, cx + 0.25, SHELF_Y, ZB + T + 0.7, CHROME)]
    if kind == "lab":
        up += [(cx - 2.0, SHELF_Y + 0.5, 6.0, cx + 2.0, SHELF_Y + 2.4, 10.0, "pi_hardhat_white")]
        low += [(cx - 2.6, LOW_Y0, 7.0, cx - 0.2, LOW_Y0 + 2.6, 11.5, "pi_boot"), (cx + 0.2, LOW_Y0, 7.0, cx + 2.6, LOW_Y0 + 2.6, 11.5, "pi_boot")]
    elif kind == "security":
        up += [(cx - 2.2, SHELF_Y + 0.5, 4.5, cx + 0.6, SHELF_Y + 2.0, 8.0, "pi_security_cap"),
               (cx + 1.0, SHELF_Y + 0.5, 9.0, cx + 2.4, SHELF_Y + 3.5, 10.4, {"*": "pi_radio", "south": "pi_radio!"})]
        low += [(cx - 2.6, LOW_Y0, 7.0, cx - 0.2, LOW_Y0 + 2.6, 11.5, "pi_boot_black"),
                (cx + 0.2, LOW_Y0, 7.0, cx + 2.6, LOW_Y0 + 2.6, 11.5, "pi_boot_black")]
    else:
        # the crew jacket on its hanger, hanging from the rail facing the door; a headset on the shelf; the kit bag
        up += O6.coat(cx, RAIL_Y, 9.0, 8.4, 4.0, "crew")
        up += headset(cx - 1.8, SHELF_Y + 0.5, 8.0)
        low += [(cx - 2.7, LOW_Y0, 3.4, cx + 2.7, LOW_Y0 + 3.4, 8.6, {"*": "pi_kit_bag", "up": "pi_kit_bag_top"}),
                (cx - 2.6, LOW_Y0, 9.4, cx - 0.2, LOW_Y0 + 2.6, 13.6, "pi_boot_black"),
                (cx + 0.2, LOW_Y0, 9.4, cx + 2.6, LOW_Y0 + 2.6, 13.6, "pi_boot_black")]
    return {"boxes": _carcass(False), "lower": low, "upper": up, "cap": _cap(), "plinth": _plinth()}


def crew_coat_rack():
    """The drone crew's coat rack (the locker room's, in place of the plant's with its lab coat and hard hats): the
    wall rail and hat shelf, a crew jacket and a hi-vis vest on hangers, two headsets on the shelf."""
    out = [(1, 12.5, 0, 15, 13.5, 1.2, "pi_veneer"), (1, 15, 0, 15, 15.5, 4.5, "pi_veneer")]
    out += O6.coat(4.8, 12.6, 2.6, 11.0, 6.4, "crew") + O6.coat(12.2, 12.6, 3.0, 9.8, 4.6, "vest")
    for x in (3.0, 9.6):
        out += headset(x, 15.5, 2.7)
    return out


def headset(x, y, zc):
    """A crew headset lying on a shelf at height y: its two ear cups, the band arching over between them."""
    out = []
    for cx in (x + 0.5, x + 3.1):
        out += [(cx - 0.55, y, zc - 0.9, cx + 0.55, y + 1.1, zc + 0.9, "pi_headset"),
                (cx - 0.15, y + 1.1, zc - 0.25, cx + 0.15, y + 2.0, zc + 0.25, "pi_headset")]
    out.append((x + 0.35, y + 2.0, zc - 0.25, x + 3.25, y + 2.3, zc + 0.25, "pi_headset"))
    return out


# ---------------------------------------------------------------------------------------------- textures

def _plain(c):
    a = np.zeros((M.CELL, M.CELL, 4), np.uint8)
    a[..., :3] = c
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _kit_bag_top():
    """The kit bag's top: its cloth, the zip running along it (black tape, the amber pull line)."""
    img = O6._cloth((46, 50, 44), 6036)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 58, M.CELL, 69), fill=(30, 31, 30, 255))
    d.rectangle((0, 62, M.CELL, 65), fill=(246, 168, 44, 255))
    return img


def makers():
    """The lockers' textures (drawn at 128 px; the kit brings them down to 64): the doors' paint, its inside, the crew's
    headset and kit bag."""
    return {
        DOOR_FACE: lambda: M.painted((120, 132, 150), 6040, chip_count=1, drip_count=0, dirt=0.1),
        DOOR_IN: lambda: M.painted((134, 147, 163), 6041, chip_count=0, drip_count=0, dirt=0.05),
        "pi_headset": lambda: M.painted((40, 42, 46), 6042, chip_count=0, drip_count=0, dirt=0.05),
        "pi_kit_bag": lambda: O6._cloth((46, 50, 44), 6035), "pi_kit_bag_top": _kit_bag_top,
    }
