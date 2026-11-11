"""The Advanced Crafting Table (take 2, approved by Justin 26 Oct): a heavy steel workstation 3 blocks wide, 2 high and
1 deep with a 6 x 6 crafting grid, placed whole from one item facing the player (tower/RoomKit; tools/room_kit.py runs
write() as one of its kits, the "workshop" room; its block is crafting/AdvancedCraftingTableBlock, display "crafting").

Structure space, facing north (the user stands north of it, its front at z 0):
- x 0..32, the bench: two drawer pedestals (3D drawers with pulls and label cards, a door) on recessed plinths, a
  toolbox on the knee-space shelf, the black phenolic top on its steel frame with the 6 x 6 fabrication grid let into it
  (lit glass with etched cells, in two halves either side of the blocks' joint), a steel nosing, the maker's plate and
  the status lamp's lens, a bench vise (jaws, jaw plates, screw, T-handle), a cordless drill lying on the top and a
  soldering station; the tool wall behind (an upstand, two power rails with sockets and switches, slotted steel with
  wrenches, screwdrivers and the pinned plan of the T1 drone, a shelf with three parts bins), the isolator on the
  vise end's post; the light gantry over it (posts, arms with braces, the LED bar, the holo emitter over the grid).
- x 32..48, the parts cabinet: four drawers, a stainless worktop, a louvred rack with six labelled parts bins, the
  touchscreen's bezel and glass.

The game draws live over it (client/AdvancedCraftingTableRenderer, client only): the items in the 36 grid slots lying
on their cells, the result turning over the grid (a drone as a cyan hologram of its own model), the status lamp lit
green while a recipe matches, the touchscreen. Its screen is textures/gui/advanced_crafting_table.png (gui()).
Model pixels: 1 px = 6.25 cm. Plain materials are 64 x 64 (4 texels a pixel); the pictures (labels, the grid's glass,
the sockets, the plan, the soldering station's readout) are packed into act_atlas, each stretched over its face and
each inside one block (a picture cut where the blocks meet would show twice).

Run with the room kits: python3 tools/room_kit.py
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import model_writer as MW
from roof_plant_art import B, _fit, octagon_axis, octagon_prism, painted, plate

PIECE = "advanced_crafting_table"
ATLAS = 512                   # the pictures' atlas (16 texels a pixel)
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
PICS = {}                     # pictures: key -> (draw, width px, height px), packed into act_atlas


def pic(key, wpx, hpx, draw):
    """A picture's texture name for a face (write() packs it into the atlas and gives the face its region)."""
    PICS.setdefault(key, (draw, wpx, hpx))
    return f"act_pic_{key}!"


TOP = 14.8                   # the bench top's surface (0.93 m)
FRAME = (12.4, 14.2)         # the top's steel frame (y), the phenolic top over it to TOP
GX, GZ, GC = 10.0, 1.2, 2.0  # the grid: west edge x, north edge z, cell size (6 x 6 cells: x 10..22, z 1.2..13.2)
GRID_Y = 14.5                # the grid glass's surface (0.3 under the top)
CX0, CX1 = 32.4, 47.6        # the parts cabinet's sides
EMITTER = (16.0, 7.2)        # over the grid's centre
LENS_Y = 28.2
LAMP = (24.0, 13.25)         # the status lamp on the frame's front (x, y); the maker's plate east of it, both in block 1
SCREEN = (CX0 + 1.3, 23.8, CX1 - 1.3, 30.2, 13.9)   # the touchscreen's glass: x0, y0, x1, y1, its face z


def _peg():
    a = np.zeros((64, 64, 3)) + np.array((62, 67, 72), float)
    a += np.random.default_rng(51).normal(0, 1.6, (64, 64, 1))
    for y in range(1, 64, 4):
        for x in range(1, 64, 4):
            a[y:y + 2, x:x + 2] = (26, 28, 30)
            a[y + 2, x:x + 2] = (84, 90, 96)
    return Image.fromarray(a.clip(0, 255).astype('uint8'))


def _louvre():
    a = np.zeros((64, 64, 3)) + np.array((62, 67, 72), float)
    a += np.random.default_rng(52).normal(0, 1.4, (64, 64, 1))
    for y in range(0, 64, 8):
        for x in range(2, 62, 10):
            a[y + 2:y + 5, x:x + 8] = (16, 16, 18)
            a[y + 5, x:x + 8] = (104, 110, 116)
    return Image.fromarray(a.clip(0, 255).astype('uint8'))


def _parts():
    r = np.random.default_rng(48)
    a = np.zeros((64, 64, 3)) + 64
    for _ in range(300):
        x, y = r.integers(0, 62, 2)
        c = int(r.choice([196, 160, 214, 128, 178]))
        a[y:y + 2, x:x + 2] = (c, c, c - 10)
    return Image.fromarray(a.clip(0, 255).astype('uint8'))


def _brushed():
    """Brushed stainless: fine horizontal grain."""
    r = np.random.default_rng(53)
    a = np.zeros((64, 64, 3)) + np.array((186, 190, 194), float)
    a += np.repeat(r.normal(0, 4.0, (64, 1, 1)), 64, axis=1)
    a += r.normal(0, 1.2, (64, 64, 1))
    return Image.fromarray(a.clip(0, 255).astype('uint8'))


MATERIALS = {
    "act_frame": lambda: painted((58, 63, 68), 31, 2.0, 0.6),        # RAL 7016 anthracite powder coat
    "act_front": lambda: painted((196, 199, 196), 32, 1.6, 0.5),     # RAL 7035 drawer fronts
    "act_top": lambda: painted((30, 31, 33), 33, 2.4, 0.2),          # black phenolic resin top
    "act_steel": _brushed,                                           # brushed stainless
    "act_alu": lambda: painted((190, 194, 198), 35, 1.6, 1.0),
    "act_handle": lambda: painted((74, 78, 82), 49, 1.4, 0.8),      # dark anodized pulls
    "act_card": lambda: painted((240, 240, 232), 50, 1.0, 0.0),
    "act_chrome": lambda: painted((220, 224, 228), 41, 2.0, 2.4),
    "act_red": lambda: painted((176, 36, 32), 36, 1.6, 0.3),
    "act_yellow": lambda: painted((228, 176, 30), 37, 1.6, 0.3),
    "act_blue": lambda: painted((34, 82, 156), 38, 1.6, 0.3),
    "act_black": lambda: painted((24, 25, 27), 39, 1.2, 0.2),
    "act_rubber": lambda: painted((34, 34, 36), 40, 3.0, 0.2),
    "act_led": lambda: painted((252, 250, 240), 42, 0.6, 0.0),
    "act_cyan": lambda: painted((110, 236, 255), 43, 0.8, 0.0),
    "act_glass": lambda: painted((16, 22, 26), 45, 1.0, 0.0),
    "act_bin_blue": lambda: painted((34, 80, 156), 46, 1.4, 0.2),
    "act_bin_yellow": lambda: painted((222, 170, 28), 47, 1.4, 0.2),
    "act_bin_red": lambda: painted((166, 34, 30), 48, 1.4, 0.2),
    "act_parts": _parts,
    "act_peg": _peg,
    "act_louvre": _louvre,
}
EMISSIVE = {"act_led", "act_cyan"}
EMISSIVE_PICS = {"grid_w", "grid_e"}          # an element whose up face is one of these glows


def grid_pic(cols):
    """The grid's etched glass, lit from under (one half, the columns cols of 0..5 counted with +x; each half stays
    inside its block so the picture is not cut): the frame, cell lines, cell dots."""
    def draw(im):
        d = ImageDraw.Draw(im)
        W, H = im.size
        d.rectangle((0, 0, W, H), fill=(8, 26, 34))
        n = len(cols)
        for i in range(n + 1):
            x = round(i * (W - 1) / n)
            edge = (i == 0 and cols[0] == 0) or (i == n and cols[-1] == 5)
            d.line((x, 0, x, H), fill=(70, 200, 236), width=4 if edge else 2)
        for j in range(7):
            y = round(j * (H - 1) / 6)
            d.line((0, y, W, y), fill=(70, 200, 236), width=4 if j in (0, 6) else 2)
        for i in range(n):
            for j in range(6):
                cx, cy = (i + 0.5) * W / n, (j + 0.5) * H / 6
                d.rectangle((cx - 1, cy - 1, cx + 1, cy + 1), fill=(40, 120, 150))
    return draw


def sockets_pic(im):
    d = ImageDraw.Draw(im)
    W, H = im.size
    d.rectangle((0, 0, W, H), fill=(186, 190, 194))
    n = 4
    for i in range(n):
        cx = H * 1.4 + (i + 0.5) * (W - H * 1.4) / n
        d.rectangle((cx - H * 0.34, H * 0.16, cx + H * 0.34, H * 0.84), fill=(234, 234, 230), outline=(96, 96, 96), width=2)
        d.ellipse((cx - H * 0.2, H * 0.3, cx + H * 0.2, H * 0.7), fill=(60, 60, 60))
        d.ellipse((cx - H * 0.12, H * 0.42, cx - H * 0.04, H * 0.58), fill=(10, 10, 10))
        d.ellipse((cx + H * 0.04, H * 0.42, cx + H * 0.12, H * 0.58), fill=(10, 10, 10))
    d.rectangle((H * 0.2, H * 0.18, H * 1.2, H * 0.82), fill=(206, 40, 30), outline=(90, 20, 16), width=2)


def plan_pic(im):
    """A drone's plan pinned on the tool wall: a blueprint, top view and side view, the title block."""
    d = ImageDraw.Draw(im)
    W, H = im.size
    d.rectangle((0, 0, W, H), fill=(30, 72, 140))
    for x in range(0, W, 10):
        d.line((x, 0, x, H), fill=(42, 90, 158))
    for y in range(0, H, 10):
        d.line((0, y, W, y), fill=(42, 90, 158))
    ink = (226, 238, 250)
    cx, cy, r = W * 0.34, H * 0.46, min(W, H) * 0.25
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        px, py = cx + dx * r * 0.78, cy + dy * r * 0.78
        d.line((cx, cy, px, py), fill=ink, width=3)
        d.ellipse((px - r * 0.42, py - r * 0.42, px + r * 0.42, py + r * 0.42), outline=ink, width=2)
    d.rectangle((cx - r * 0.3, cy - r * 0.38, cx + r * 0.3, cy + r * 0.38), outline=ink, width=3)
    d.line((W * 0.64, H * 0.56, W * 0.95, H * 0.56), fill=ink, width=3)
    d.rectangle((W * 0.74, H * 0.47, W * 0.85, H * 0.56), outline=ink, width=2)
    d.line((W * 0.64, H * 0.47, W * 0.64, H * 0.53), fill=ink, width=2)
    d.line((W * 0.95, H * 0.47, W * 0.95, H * 0.53), fill=ink, width=2)
    d.rectangle((W * 0.58, H * 0.78, W - 4, H - 4), outline=ink, width=2)
    f = _fit(d, "T1 COURIER", W * 0.36, H * 0.1)
    d.text((W * 0.79, H * 0.86), "T1 COURIER", font=f, fill=ink, anchor="mm")


def solder_pic(im):
    d = ImageDraw.Draw(im)
    W, H = im.size
    d.rectangle((0, 0, W, H), fill=(26, 28, 30))
    d.rectangle((4, 4, W * 0.62, H - 5), fill=(30, 10, 8))
    f = _fit(d, "350", W * 0.5, H * 0.66)
    d.text((W * 0.33, H / 2), "350", font=f, fill=(255, 70, 40), anchor="mm")
    for k, c in enumerate(((200, 200, 200), (200, 200, 200))):
        x = W * (0.74 + k * 0.14)
        d.ellipse((x - H * 0.12, H * 0.38, x + H * 0.12, H * 0.62), fill=c)


def door_z(z_face, x0, x1, y0, y1, tex, hinge="drawer"):
    """A 3D door or drawer front on a face square to z at z_face (looking north, -z): the front 0.4 proud, a pull."""
    e = [B((x0, y0, z_face - 0.4), (x1, y1, z_face), tex)]
    if hinge in ("west", "east"):
        hx = x1 - 1.4 if hinge == "west" else x0 + 1.0
        e.append(B((hx, y0 + (y1 - y0) * 0.35, z_face - 1.1), (hx + 0.4, y0 + (y1 - y0) * 0.65, z_face - 0.4), "act_handle"))
        kx = x0 + 0.1 if hinge == "west" else x1 - 0.5
        for y in (y0 + 1.2, y1 - 2.2):
            e.append(B((kx, y, z_face - 0.6), (kx + 0.4, y + 1.0, z_face - 0.4), "act_steel"))
    else:   # a drawer: a bar pull on two stand-offs, a label holder
        hy = y1 - 1.0
        xa, xb = x0 + (x1 - x0) * 0.22, x1 - (x1 - x0) * 0.22
        e.append(B((xa, hy - 0.3, z_face - 1.2), (xb, hy + 0.3, z_face - 0.8), "act_handle"))
        for x in (xa + 0.2, xb - 0.6):
            e.append(B((x, hy - 0.2, z_face - 0.8), (x + 0.4, hy + 0.2, z_face - 0.4), "act_handle"))
        if y1 - y0 > 2.4:
            lx0, lx1 = x0 + (x1 - x0) * 0.38, x1 - (x1 - x0) * 0.38
            e.append(B((lx0, y0 + 0.5, z_face - 0.55), (lx1, y0 + 1.4, z_face - 0.4), "act_handle"))
            e.append(B((lx0 + 0.15, y0 + 0.62, z_face - 0.66), (lx1 - 0.15, y0 + 1.28, z_face - 0.55), "act_card"))
    return e


BIN_TEXT = {}


def parts_bin(x, y, z0, z1, col, w, h):
    """A plastic parts bin, its open scoop front to z0: the body, the scoop, the parts heaped, a label."""
    zs = z0 + 1.2
    text = BIN_TEXT.get((round(x), round(y)), "M4 x 12")
    return [B((x, y, zs), (x + w, y + h, z1), {"*": col, "up": "act_parts"}),
            B((x, y, z0), (x + w, y + h * 0.55, zs), {"*": col, "up": "act_parts"}),
            B((x + 0.5, y + 0.25, z0 - 0.15), (x + w - 0.5, y + h * 0.5, z0),
              {"*": "act_front", "north": pic(f"bin_{round(x)}_{round(y)}", w - 1.0, h * 0.25, plate([text], bg=(246, 246, 240)))})]


def bench():
    e = []
    # the gantry's back posts (the bench's back legs too), rubber feet
    for x0 in (0.4, 29.8):
        e.append(B((x0 - 0.2, 0.0, 14.0), (x0 + 2.0, 0.5, 16.0), "act_rubber"))
        e.append(B((x0, 0.5, 14.2), (x0 + 1.8, 31.2, 16.0), "act_frame"))
    # two pedestals on recessed plinths; four drawers left, a drawer and a door right
    for p0, p1 in ((1.0, 12.8), (19.2, 31.0)):
        e.append(B((p0 + 0.4, 0.0, 1.8), (p1 - 0.4, 0.8, 13.8), "act_black"))
        e.append(B((p0, 0.8, 1.2), (p1, FRAME[0], 14.2), "act_frame"))
    for y0, y1 in ((0.9, 3.9), (4.1, 6.9), (7.1, 9.7), (9.9, 12.2)):
        e += door_z(1.2, 1.3, 12.5, y0, y1, "act_front")
    e += door_z(1.2, 19.5, 30.7, 9.9, 12.2, "act_front")
    e += door_z(1.2, 19.5, 30.7, 0.9, 9.7, "act_front", hinge="east")
    # the knee space: a modesty panel, a low shelf, a toolbox on it
    e.append(B((12.8, 1.6, 13.4), (19.2, FRAME[0], 14.0), "act_frame"))
    e.append(B((12.8, 1.6, 2.0), (19.2, 2.2, 13.4), "act_frame"))
    e.append(B((13.4, 2.2, 4.4), (18.6, 5.4, 10.6), "act_red"))
    e.append(B((13.3, 4.6, 4.3), (18.7, 4.9, 10.7), "act_black"))                         # the lid's seam
    for x in (14.6, 17.0):
        e.append(B((x, 5.4, 7.3), (x + 0.4, 6.0, 7.7), "act_black"))
    e.append(B((14.6, 6.0, 7.3), (17.4, 6.4, 7.7), "act_black"))                          # the handle
    e.append(B((15.6, 3.6, 4.2), (16.4, 4.4, 4.4), "act_chrome"))                         # the latch
    # the top: the steel frame, the phenolic round the grid's opening, the lit glass let in, a steel nosing
    e.append(B((0.4, FRAME[0], 0.4), (31.6, FRAME[1], 14.2), "act_frame"))
    g1x, g1z = GX + 6 * GC, GZ + 6 * GC
    for (x0, z0, x1, z1) in ((0.4, 0.4, GX, 14.2), (g1x, 0.4, 31.6, 14.2), (GX, 0.4, g1x, GZ), (GX, g1z, g1x, 14.2)):
        e.append(B((x0, FRAME[1], z0), (x1, TOP, z1), "act_top"))
    # the glass in two halves either side of the blocks' joint (x 16), each its own picture (its u runs with +x: the
    # west half's outer frame is its picture's left edge, the east half's its right)
    e.append(B((GX, FRAME[1], GZ), (16.0, GRID_Y, g1z), {"*": "act_glass", "up": pic("grid_w", 16.0 - GX, 6 * GC, grid_pic((0, 1, 2)))}))
    e.append(B((16.0, FRAME[1], GZ), (g1x, GRID_Y, g1z), {"*": "act_glass", "up": pic("grid_e", g1x - 16.0, 6 * GC, grid_pic((3, 4, 5)))}))
    e.append(B((0.2, FRAME[0] + 0.2, 0.15), (31.8, TOP + 0.1, 0.4), "act_steel"))
    # the maker's plate and the status lamp's lens on the frame's front (the game lights it: green ready, amber missing)
    e.append((list((LAMP[0] + 1.0, 12.75, 0.0)), list((LAMP[0] + 7.0, 13.75, 0.15)),
              {"*": "act_steel", "north": pic("name", 6.0, 1.0, plate(["ADVANCED CRAFTING  AF-6"], bg=(224, 226, 228)))}))
    lx, ly = LAMP
    e.append(B((lx - 0.5, ly - 0.5, 0.0), (lx + 0.5, ly + 0.5, 0.15), "act_alu"))
    e.append(B((lx - 0.32, ly - 0.32, -0.12), (lx + 0.32, ly + 0.32, 0.0), "act_glass"))
    # the vise on the front left corner: base, fixed jaw, sliding jaw, jaw plates, the guide, the screw, the T-handle
    e.append(B((1.2, TOP, 0.9), (6.0, TOP + 1.2, 4.6), "act_blue"))
    e.append(B((1.6, TOP + 1.2, 3.0), (5.6, TOP + 3.8, 4.4), "act_blue"))
    e.append(B((1.6, TOP + 1.2, 0.5), (5.6, TOP + 3.6, 1.8), "act_blue"))
    e.append(B((1.8, TOP + 2.2, 1.8), (5.4, TOP + 3.6, 2.0), "act_steel"))
    e.append(B((1.8, TOP + 2.2, 2.8), (5.4, TOP + 3.8, 3.0), "act_steel"))
    e.append(B((2.9, TOP + 1.2, 1.8), (4.3, TOP + 1.9, 3.0), "act_blue"))
    e += octagon_axis("z", (3.6, TOP + 2.0), 0.35, -1.3, 0.5, "act_chrome")
    e += octagon_axis("z", (3.6, TOP + 2.0), 0.6, -1.5, -0.8, "act_chrome")
    e += octagon_axis("x", (-1.15, TOP + 2.0), 0.2, 1.0, 6.2, "act_chrome")
    for x in (0.6, 6.2):
        e += octagon_axis("x", (-1.15, TOP + 2.0), 0.32, x, x + 0.4, "act_black")
    # the tool wall: an upstand, the power rail on it, the slotted wall, the shelf and its brackets
    e.append(B((2.2, FRAME[1], 14.2), (29.8, 16.0, 15.0), "act_frame"))
    for x0, x1 in ((2.6, 15.6), (16.4, 29.4)):            # two power rails, one in each block (each its own switch)
        e.append(B((x0, 16.0, 14.0), (x1, 17.6, 15.0), {"*": "act_alu", "north": pic("sockets", x1 - x0, 1.6, sockets_pic)}))
    e.append(B((2.2, 16.0, 15.0), (29.8, 29.6, 15.8), {"*": "act_frame", "north": "act_peg"}))
    e.append(B((2.2, 26.8, 11.8), (29.8, 27.3, 15.0), "act_frame"))
    for x in (7.6, 23.6):
        e.append(B((x, 25.4, 13.8), (x + 0.5, 26.8, 15.0), "act_frame"))
    # tools on the wall: three combination wrenches under a rack bar
    e.append(B((2.6, 25.0, 14.4), (6.8, 25.4, 15.0), "act_steel"))
    for x, h in ((3.0, 6.6), (4.4, 5.8), (5.7, 5.0)):
        e.append(B((x, 25.0 - h + 0.8, 14.55), (x + 0.45, 25.0 - 0.6, 14.95), "act_chrome"))
        e.append(B((x - 0.3, 25.0 - 0.6, 14.55), (x + 0.75, 25.0, 14.95), "act_chrome"))
        e.append(B((x - 0.25, 25.0 - h, 14.55), (x + 0.7, 25.0 - h + 0.8, 14.95), "act_chrome"))
    # screwdrivers in a holder strip
    e.append(B((7.2, 23.4, 14.0), (12.4, 24.0, 15.0), "act_steel"))
    for i, col in enumerate(("act_red", "act_yellow", "act_red", "act_black")):
        x = 7.6 + i * 1.2
        e.append(B((x, 24.0, 14.3), (x + 0.7, 25.8, 14.9), col))
        e.append(B((x + 0.25, 20.8, 14.5), (x + 0.45, 23.4, 14.7), "act_chrome"))
    # a cordless drill lying on the bench top behind the vise (its handle and battery toward the player)
    e.append(B((3.4, TOP, 10.6), (7.2, TOP + 1.0, 12.2), "act_yellow"))
    e.append(B((4.0, TOP, 8.2), (5.0, TOP + 0.9, 10.6), "act_black"))
    e.append(B((3.6, TOP, 7.2), (5.4, TOP + 1.1, 8.2), "act_black"))
    e.append(B((7.2, TOP + 0.1, 11.0), (8.0, TOP + 0.9, 11.8), "act_black"))
    e.append(B((8.0, TOP + 0.4, 11.3), (9.2, TOP + 0.6, 11.5), "act_chrome"))
    # a soldering station at the bench's other end: the unit (its readout lit), the iron in its holder, the sponge tray
    e.append(B((24.6, TOP, 9.6), (28.8, TOP + 2.4, 13.4), "act_frame"))
    e.append((list((25.0, TOP + 0.15, 9.45)), list((28.4, 15.9, 9.6)),            # its face stays under y 16 (one block)
              {"*": "act_black", "north": pic("solder", 3.4, 15.9 - TOP - 0.15, solder_pic)}))
    e += octagon_prism(22.8, 11.4, 0.9, TOP, TOP + 0.5, "act_frame")
    e += octagon_prism(22.8, 11.4, 0.5, TOP + 0.5, TOP + 3.0, "act_chrome")
    e.append(B((22.6, TOP + 1.2, 11.2), (23.0, TOP + 5.2, 11.6), "act_blue", ("x", -22.5, (22.8, TOP + 3.2, 11.4))))
    e.append(B((21.4, TOP, 9.2), (23.2, TOP + 0.5, 10.4), "act_yellow"))
    e.append(B((26.4, TOP + 0.6, 13.4), (27.0, TOP + 1.2, 14.2), "act_black"))
    # the plan, pinned
    e.append((list((21.6, 18.0, 14.85)), list((29.2, 24.6, 15.0)), {"*": "act_front", "north": pic("plan", 7.6, 6.6, plan_pic)}))
    for x in (22.2, 28.6):
        e += octagon_axis("z", (x, 24.0), 0.3, 14.55, 14.85, "act_red")
    # three bins on the shelf
    for x, col in ((2.8, "act_bin_blue"), (7.4, "act_bin_yellow"), (12.0, "act_bin_red")):
        e += parts_bin(x, 27.3, 12.4, 15.0, col, 4.2, 2.2)
    # the station's isolator on the vise end's post, its rotary handle (red on yellow), the cable down the post
    e.append(B((0.5, 18.0, 13.2), (2.1, 21.6, 14.2), "act_alu"))
    e.append(B((0.7, 18.6, 13.0), (1.9, 19.8, 13.2), "act_yellow"))
    e.append(B((1.1, 18.8, 12.6), (1.5, 19.6, 13.0), "act_red"))
    e.append(B((0.9, 19.0, 12.6), (1.7, 19.4, 12.9), "act_red"))
    e.append(B((1.0, 14.8, 13.5), (1.6, 18.0, 14.1), "act_black"))
    # the light gantry: arms forward from the posts, the front beam, the back beam, braces, the LED bar, the emitter
    for x0 in (0.4, 29.8):
        e.append(B((x0, 29.6, 1.0), (x0 + 1.8, 31.2, 14.2), "act_frame"))
        e.append(B((x0 + 0.3, 27.9, 10.3), (x0 + 1.5, 28.9, 15.0), "act_frame", ("x", -45, (x0 + 0.9, 28.4, 12.65))))
    e.append(B((2.2, 29.6, 14.2), (29.8, 31.2, 16.0), "act_frame"))
    e.append(B((2.2, 29.6, 1.0), (29.8, 31.2, 2.8), "act_frame"))
    e.append(B((3.0, 29.0, 1.2), (29.0, 29.6, 2.6), "act_alu"))
    e.append(B((3.2, 28.9, 1.4), (28.8, 29.0, 2.4), "act_led"))
    e.append(B((15.2, 29.8, 2.8), (16.8, 30.8, 14.2), "act_frame"))
    ex, ez = EMITTER
    e += octagon_prism(ex, ez, 1.4, LENS_Y + 0.2, 29.8, "act_frame")
    e += octagon_prism(ex, ez, 0.9, LENS_Y, LENS_Y + 0.2, "act_cyan")
    return e


def cabinet():
    e = []
    e.append(B((CX0 + 0.4, 0.0, 1.8), (CX1 - 0.4, 0.8, 15.0), "act_black"))
    e.append(B((CX0, 0.8, 1.4), (CX1, 14.0, 15.6), "act_frame"))
    for y0, y1 in ((1.0, 3.8), (4.0, 6.8), (7.0, 10.2), (10.4, 13.6)):
        e += door_z(1.4, CX0 + 0.3, CX1 - 0.3, y0, y1, "act_front")
    e.append(B((CX0 - 0.2, 14.0, 1.0), (CX1 + 0.2, 14.8, 15.8), "act_steel"))
    # the rack: two sides, the louvred back, the cap
    for x0, x1 in ((CX0, CX0 + 0.6), (CX1 - 0.6, CX1)):
        e.append(B((x0, 14.8, 8.0), (x1, 31.0, 15.6), "act_frame"))
    e.append(B((CX0 + 0.6, 14.8, 15.0), (CX1 - 0.6, 31.0, 15.6), {"*": "act_frame", "north": "act_louvre"}))
    e.append(B((CX0 - 0.2, 31.0, 7.8), (CX1 + 0.2, 31.6, 15.8), "act_frame"))
    # six bins hung on the louvre, two rows
    cols = ("act_bin_blue", "act_bin_yellow", "act_bin_red")
    names = ["M3 BOLTS", "M4 NUTS", "CABLE TIES", "SPACERS", "M5 BOLTS", "CONNECTORS"]
    for row, y in enumerate((16.0, 20.0)):            # (their labels over y 16: a picture stays inside one block)
        for k in range(3):
            x = CX0 + 0.9 + k * 4.6
            BIN_TEXT[(round(x), round(y))] = names[row * 3 + k]
            e += parts_bin(x, y, 10.6, 15.0, cols[k], 4.2, 3.0)
    # the touchscreen: its bezel on the back, the glass (the game draws the screen on it)
    x0, y0, x1, y1, zf = SCREEN
    e.append(B((x0 - 0.4, y0 - 0.4, zf + 0.1), (x1 + 0.4, y1 + 0.4, 15.0), "act_black"))
    e.append(B((x0, y0, zf), (x1, y1, zf + 0.1), "act_glass"))
    return e


def station():
    BIN_TEXT.update({(3, 27): "M4 x 12", (7, 27): "WASHERS", (12, 27): "O-RINGS"})
    return bench() + cabinet()


# the piece's cells as it faces north (part n is cell n, the anchor first): 3 wide (x), 2 high
CELLS = {PIECE: [[x, y, 0] for x in range(3) for y in range(2)]}
DISPLAY = {PIECE: "crafting"}
# where the live parts are, for the renderer (pixels, structure space facing north): the grid (west x, north z, cell,
# its glass's top), the emitter (x, z) and its lens (y), the lamp (x, y), the screen (x0, y0, x1, y1, its face z)
LIVE = {"grid": [GX, GZ, GC, GRID_Y], "emitter": [EMITTER[0], EMITTER[1], LENS_Y], "lamp": list(LAMP), "screen": list(SCREEN)}


def _one_block(elements):
    """Every picture's element inside one block (split where the blocks meet, a stretched picture would show twice)."""
    import math as _m
    for frm, to, tex, *_ in elements:
        texs = tex.values() if isinstance(tex, dict) else [tex]
        if any(str(t).startswith("act_pic_") for t in texs):
            for a in range(3):
                if _m.floor(frm[a] / 16) != _m.floor((to[a] - 1e-6) / 16):
                    raise ValueError(f"adv_crafting_table: the picture {sorted(map(str, texs))} at {frm}..{to} crosses a block")


def _pack(assets):
    """The pictures into act_atlas; their regions (uv, 16 = the atlas's side)."""
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    x = y = row = 0
    regions = {}
    for key, (draw, wpx, hpx) in sorted(PICS.items(), key=lambda kv: (-kv[1][2], kv[0])):
        w, h = max(1, round(wpx * 16)), max(1, round(hpx * 16))
        if x + w > ATLAS:
            x, y, row = 0, y + row + 2, 0
        if y + h > ATLAS:
            raise ValueError("adv_crafting_table: the pictures do not fit the atlas")
        p = Image.new("RGBA", (w, h), (255, 255, 255, 255))
        draw(p)
        img.paste(p, (x, y))
        regions[key] = [round(v, 5) for v in (x * 16 / ATLAS, y * 16 / ATLAS, (x + w) * 16 / ATLAS, (y + h) * 16 / ATLAS)]
        x += w + 2
        row = max(row, h)
    img.save(assets / "textures/block/act_atlas.png")
    return regions


def _finish(elements, regions):
    """A part's elements as written: pictures pointed at their atlas regions; the lit ones (the LED bar, the lens, the
    grid's glass) glow; a piece of a box with no face left dropped."""
    elements = [e for e in elements if e.get("faces")]
    for e in elements:
        names = set()
        lit = False
        for face, spec in e["faces"].items():
            t = spec["texture"].lstrip("#")
            if t.startswith("act_pic_"):
                key = t[len("act_pic_"):]
                spec["texture"] = "#act_atlas"
                spec["uv"] = list(regions[key])
                lit |= face == "up" and key in EMISSIVE_PICS
            names.add(spec["texture"].lstrip("#"))
        if lit or (names and names <= EMISSIVE):
            e["light_emission"] = 15
            e["shade"] = False
    return elements


def _textures(elements, ns):
    names = sorted({spec["texture"].lstrip("#") for e in elements for spec in e.get("faces", {}).values()})
    out = {n: f"{ns}:block/{n}" for n in names}
    out["particle"] = f"{ns}:block/act_frame"
    return out


def write(assets, ns="jugcraft"):
    """The textures, the part models and the blockstate; the screen's texture."""
    assets = Path(assets)
    for d in ("textures/block", "textures/gui", "models/block", "blockstates"):
        (assets / d).mkdir(parents=True, exist_ok=True)
    for name, make in MATERIALS.items():
        make().save(assets / f"textures/block/{name}.png")
    els = station()
    _one_block(els)
    regions = _pack(assets)
    cells = [tuple(c) for c in CELLS[PIECE]]
    variants = {}
    for i, part in enumerate(MW.split_model(PIECE, els, cells)):
        part = _finish(part, regions)
        model = {"textures": _textures(part, ns), "elements": part}
        (assets / f"models/block/{PIECE}_{i}.json").write_text(json.dumps(model, indent=1) + "\n")
        for f, yr in FACINGS.items():
            variants[f"facing={f},part={i}"] = {"model": f"{ns}:block/{PIECE}_{i}", **({"y": yr} if yr else {})}
    (assets / f"blockstates/{PIECE}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    gui().save(assets / "textures/gui/advanced_crafting_table.png")
    return [PIECE]


# ------------------------------------------------------------------ the screen (client/AdvancedCraftingScreen)
GUI_W, GUI_H = 236, 238
GUI_GRID = (10, 24)           # the grid's first slot frame (each slot 18; the item drawn 1 in)
GUI_RESULT = (168, 61)        # the result's frame (26 square; the item drawn 5 in)
GUI_ARROW = (134, 72)         # the arrow's left end (lit by the screen while a recipe matches)
GUI_INV = (37, 152)           # the inventory's first slot frame; the hotbar 58 under it
CYAN = (70, 200, 230)


def gui():
    """The screen's background (256 square): a slate panel in the machines' style (speckled, bevelled, cyan trim), the
    6 x 6 grid's slots in their inset, the dim arrow, the result's frame, the inventory's and hotbar's slots."""
    r = np.random.default_rng(4)
    a = np.zeros((GUI_H, GUI_W, 3)) + np.array((40, 52, 60), float)
    a += r.normal(0, 3.0, (GUI_H, GUI_W, 1))
    a[r.random((GUI_H, GUI_W)) < 0.06] -= 12
    a[0, :] = a[:, 0] = (88, 106, 116)
    a[-1, :] = a[:, -1] = (20, 26, 30)
    a[1, 1:-1] = a[1:-1, 1] = (64, 80, 90)
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    im.paste(Image.fromarray(a.clip(0, 255).astype("uint8")), (0, 0))
    d = ImageDraw.Draw(im)

    def rect(x0, y0, x1, y1, c):
        d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=c)

    def inset(x0, y0, x1, y1, c):
        rect(x0, y0, x1, y1, c)
        rect(x0, y0, x1, y0 + 1, (16, 20, 24))
        rect(x0, y0, x0 + 1, y1, (16, 20, 24))
        rect(x0, y1 - 1, x1, y1, (92, 112, 122))
        rect(x1 - 1, y0, x1, y1, (92, 112, 122))

    def slot(x, y, size=18):
        inset(x, y, x + size, y + size, (30, 38, 44))

    for cx, cy in ((3, 3), (GUI_W - 4, 3), (3, GUI_H - 4), (GUI_W - 4, GUI_H - 4)):
        rect(cx, cy, cx + 1, cy + 1, CYAN)
    rect(6, 16, GUI_W - 6, 17, CYAN)
    gx, gy = GUI_GRID
    inset(gx - 2, gy - 2, gx + 6 * 18 + 2, gy + 6 * 18 + 2, (18, 24, 28))
    for j in range(6):
        for i in range(6):
            slot(gx + i * 18, gy + j * 18)
    ax, ay = GUI_ARROW
    rect(ax, ay, ax + 18, ay + 3, (40, 60, 70))
    for k in range(5):
        rect(ax + 18 + k, ay - 4 + k, ax + 19 + k, ay + 7 - k, (40, 60, 70))
    rx, ry = GUI_RESULT
    slot(rx, ry, 26)
    rect(6, 138, GUI_W - 6, 139, (70, 90, 100))
    ix, iy = GUI_INV
    for j in range(3):
        for i in range(9):
            slot(ix + i * 18, iy + j * 18)
    for i in range(9):
        slot(ix + i * 18, iy + 58)
    return im


if __name__ == "__main__":
    import sys
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "src/main/resources/assets/jugcraft"
    print(write(out))
