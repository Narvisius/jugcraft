"""The crew lounge and kitchen (work list item 22 part B, room 7), to the standard Justin set on the lab: everything at
its real size (a pixel is 6.25 cm), modelled rather than painted on (no doors or drawers drawn on a picture), the
screens live in game, materials at one density and seamless, things varied.

Pieces: kitchen_run (10 x 1, two high: the kitchen along a wall, its modules built whole), fridge_freezer (two high),
drinks_fridge (two high), vending_machine (two high), kitchen_island (4 x 1), bar_stool, dining_table (2 x 1, its
things a variant), dining_chair (pushed in, pulled out, turned), lounge_rug (4 x 3 floor blocks), foosball_table
(2 x 1), media_console (3 x 1), lounge_tv (3 x 1, on a wall: the TOWER TV channel live). With them the crew rest's sofas, coffee table, recliners, side table and water
dispenser, the manager's bookcases and floor lamp, the office's stationery cupboard as the larder, the clock.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
import lab_kit as LK
import rest_kit as RK
import planning_kit as PK
import office_kit as OK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None
SCR = None
RUG = None
TVS = None                        # the lounge TV's picture, a texture of its own (the screens' atlas is full)
MONO = MK.MONO
swap_xy, rod_x = LK.swap_xy, LK.rod_x
cable, cut_y, cut_z, label_plate = RK.cable, RK.cut_y, RK.cut_z, RK.label_plate
_fit = MK._text_fit


MATERIALS = {
    "lk2_front": lambda: PK.laminate((70, 82, 74), 3701), "lk2_carcass": lambda: PK.laminate((236, 236, 232), 3702),
    "lk2_quartz": lambda: RK.speckle((204, 202, 196), 3703), "lk2_glassblack": lambda: TK.solid((16, 18, 22), 3704, 0.8),
    "lk2_baize": lambda: MK.weave((46, 120, 70), 3705, 1, 3), "lk2_team_red": lambda: TK.solid((176, 36, 36), 3706, 1.4),
    "lk2_team_blue": lambda: TK.solid((40, 70, 160), 3707, 1.4), "lk2_vend": lambda: PK.laminate((36, 38, 44), 3708),
    "lk2_vend_red": lambda: PK.laminate((150, 30, 34), 3709), "lk2_vcoil": lambda: TK.solid((170, 174, 180), 3710, 1.2),
    "lk2_can_red": lambda: TK.solid((200, 30, 36), 3711, 1.4), "lk2_can_blue": lambda: TK.solid((30, 80, 190), 3712, 1.4),
    "lk2_can_green": lambda: TK.solid((40, 150, 70), 3713, 1.4), "lk2_can_silver": lambda: TK.brushed((190, 194, 200), 3714, 2),
    "lk2_can_orange": lambda: TK.solid((236, 130, 30), 3715, 1.4), "lk2_bottle": lambda: MK.rgba((150, 200, 230), 140, 3716),
    "lk2_fruit_orange": lambda: TK.solid((236, 140, 40), 3717, 1.6), "lk2_apple": lambda: TK.solid((190, 40, 40), 3718, 1.8),
    "lk2_banana": lambda: TK.solid((236, 206, 70), 3719, 1.6), "lk2_stem": lambda: TK.solid((60, 130, 60), 3720, 1.8),
    "lk2_bloom": lambda: TK.solid((236, 120, 150), 3721, 1.6), "lk2_bread": lambda: TK.solid((200, 150, 90), 3722, 1.8),
    "lk2_steel_black": lambda: PK.laminate((40, 40, 44), 3723),
    # the lounge TV: its matt black back and panel, the dark brushed frame, the maker's badge, the wall bracket
    "lk2_tv_body": lambda: TK.solid((22, 22, 24), 3724, 2.0), "lk2_tv_frame": lambda: TK.brushed((44, 45, 50), 3725, 4.0),
    "lk2_tv_logo": lambda: TK.brushed((190, 192, 196), 3726, 6.0), "lk2_tv_bracket": lambda: TK.solid((30, 30, 32), 3727, 3.0),
}


# ------------------------------------------------------------------ pictures
def hob_top(img):
    """The induction hob's black glass: four rings and the touch controls."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(16, 18, 22))
    for (cx, cy, r) in ((0.28, 0.3, 0.17), (0.72, 0.3, 0.13), (0.28, 0.72, 0.13), (0.72, 0.68, 0.19)):
        d.ellipse((W * cx - W * r, H * cy - W * r, W * cx + W * r, H * cy + W * r), outline=(90, 94, 100), width=max(1, W // 120))
        d.ellipse((W * cx - W * r * 0.55, H * cy - W * r * 0.55, W * cx + W * r * 0.55, H * cy + W * r * 0.55), outline=(60, 64, 70), width=1)
    for k in range(6):
        x = W * (0.2 + k * 0.12)
        d.text((x, H * 0.93), "-+O|<>"[k], font=ImageFont.truetype(FONT, max(7, round(H * 0.05))), fill=(150, 150, 156), anchor="mm")
    return img


def oven_panel(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(20, 22, 26))
    TK.live_text(img, (W * 0.5, H * 0.5), "14:32", _fit(d, "14:32", MONO, round(H * 0.6), W * 0.3), (240, 150, 60), "mm", bg=(20, 22, 26), src="time")
    for k in range(4):
        d.ellipse((W * (0.06 + k * 0.06), H * 0.3, W * (0.06 + k * 0.06) + H * 0.4, H * 0.7), outline=(120, 120, 126), width=1)
    return img


def coffee_screen(text):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(8, 12, 16))
        d.text((W / 2, H * 0.36), text, font=_fit(d, text, FONT, round(H * 0.32), W * 0.86), fill=(120, 210, 255), anchor="mm")
        d.text((W / 2, H * 0.74), "ESPRESSO  LATTE  TEA", font=_fit(d, "ESPRESSO  LATTE  TEA", FONT_R, round(H * 0.18), W * 0.9), fill=(200, 200, 210), anchor="mm")
        return img
    return draw


def wrapper(name, bg, fg):
    """A snack's wrapper seen from the front of the machine."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=bg)
        d.rectangle((0, H * 0.7, W, H), fill=tuple(int(c * 0.75) for c in bg))
        d.text((W / 2, H * 0.4), name, font=_fit(d, name, FONT, round(H * 0.34), W * 0.9), fill=fg, anchor="mm")
        return img
    return draw


SNACKS = [("CRISPS", (220, 40, 40), (255, 240, 200)), ("CHOCO", (90, 50, 30), (250, 210, 120)), ("NUTS", (40, 120, 60), (250, 250, 220)),
          ("COOKIES", (40, 80, 170), (255, 255, 255)), ("MINTS", (230, 230, 236), (40, 140, 90)), ("BAR", (240, 170, 30), (80, 30, 10))]


def vend_keypad(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 32, 36))
    d.rectangle((W * 0.08, H * 0.04, W * 0.92, H * 0.2), fill=(10, 16, 12))
    d.text((W / 2, H * 0.12), "SELECT", font=_fit(d, "SELECT", MONO, round(H * 0.08), W * 0.7), fill=(90, 230, 120), anchor="mm")
    for r in range(4):
        for c in range(3):
            x0, y0 = W * (0.12 + c * 0.27), H * (0.26 + r * 0.12)
            d.rectangle((x0, y0, x0 + W * 0.2, y0 + H * 0.09), fill=(70, 72, 78))
            d.text((x0 + W * 0.1, y0 + H * 0.045), "123456789*0#"[r * 3 + c], font=_fit(d, "8", FONT, round(H * 0.06), W * 0.15), fill=(240, 240, 240), anchor="mm")
    d.text((W / 2, H * 0.8), "CARD  /  PHONE", font=_fit(d, "CARD  /  PHONE", FONT_R, round(H * 0.05), W * 0.86), fill=(200, 200, 206), anchor="mm")
    return img


def fridge_sign(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(26, 90, 170))
    d.text((W / 2, H / 2), "COLD DRINKS", font=_fit(d, "COLD DRINKS", FONT, round(H * 0.56), W - 10), fill=(255, 255, 255), anchor="mm")
    return img


def rug_pattern(img):
    """A woven rug: a deep blue field, a border of two bands, a geometric medallion, a fringe at the short ends."""
    W, H = img.size
    y, x = np.mgrid[0:H, 0:W]
    u, v = x / W, y / H
    base = np.zeros((H, W, 3))
    base[:] = (40, 56, 92)
    border = (np.minimum(np.minimum(u, 1 - u) * W, np.minimum(v, 1 - v) * H))
    k = W / 57.6                                  # texels a pixel
    base[border < 2.4 * k] = (176, 150, 110)
    base[(border >= 1.0 * k) & (border < 1.6 * k)] = (130, 50, 46)
    base[(border >= 2.4 * k) & (border < 3.0 * k)] = (200, 190, 160)
    du, dv = np.abs(u - 0.5) * W, np.abs(v - 0.5) * H
    diamond = du / (W * 0.22) + dv / (H * 0.3)
    base[(diamond < 1.0) & (diamond > 0.88)] = (200, 190, 160)
    base[diamond < 0.55] = (130, 50, 46)
    base[(diamond < 0.3)] = (176, 150, 110)
    rng = np.random.default_rng(3730)
    base += rng.normal(0, 4, (H, W))[..., None]
    base[((x // max(1, int(k * 0.5))) + (y // max(1, int(k * 0.5)))) % 2 == 0] *= 0.96
    img.paste(TK._img(base), (0, 0))
    return img


def felt_field(img):
    """The foosball table's field: the green felt, its white lines, the centre circle."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(46, 120, 70))
    for k in range(0, W, max(2, W // 20)):
        d.rectangle((k, 0, k + W // 40, H), fill=(52, 128, 76))
    lw = max(1, H // 60)
    d.rectangle((W * 0.02, H * 0.04, W * 0.98, H * 0.96), outline=(236, 236, 236), width=lw)
    d.line((W / 2, H * 0.04, W / 2, H * 0.96), fill=(236, 236, 236), width=lw)
    d.ellipse((W / 2 - H * 0.16, H / 2 - H * 0.16, W / 2 + H * 0.16, H / 2 + H * 0.16), outline=(236, 236, 236), width=lw)
    for x0, x1 in ((0.02, 0.1), (0.9, 0.98)):
        d.rectangle((W * x0, H * 0.3, W * x1, H * 0.7), outline=(236, 236, 236), width=lw)
    return img


def newspaper(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(232, 230, 222))
    d.text((W / 2, H * 0.1), "THE VALLEY POST", font=_fit(d, "THE VALLEY POST", FONT, round(H * 0.09), W * 0.9), fill=(20, 20, 24), anchor="mm")
    d.line((W * 0.05, H * 0.18, W * 0.95, H * 0.18), fill=(20, 20, 24), width=1)
    d.text((W * 0.05, H * 0.22), "TOWER HITS 1,000,000", font=_fit(d, "TOWER HITS 1,000,000", FONT, round(H * 0.07), W * 0.9), fill=(20, 20, 24))
    d.rectangle((W * 0.05, H * 0.34, W * 0.5, H * 0.66), fill=(150, 150, 150))
    for k in range(10):
        y = H * (0.36 + k * 0.06)
        d.line((W * 0.55, y, W * 0.95, y), fill=(120, 120, 120), width=1)
    for k in range(4):
        y = H * (0.72 + k * 0.06)
        d.line((W * 0.05, y, W * 0.95, y), fill=(120, 120, 120), width=1)
    return img


# ------------------------------------------------------------------ the kitchen run
F = "front"          # the doors and drawers: sage green
C = "carcass"
Q = "quartz"


def door_pair(x0, y0, y1, z):
    """Two doors side by side in a 16 wide module from x0 (their fronts at z, toward +z), the gaps between, their
    bar handles upright at the meeting edges."""
    out = [box((x0 + 0.15, y0, z), (x0 + 7.9, y1, z + 0.35), mat(F)), box((x0 + 8.1, y0, z), (x0 + 15.85, y1, z + 0.35), mat(F))]
    for hx in (x0 + 7.0, x0 + 8.7):
        out += [box((hx, y1 - 3.6, z + 0.7), (hx + 0.3, y1 - 0.8, z + 0.95), mat("chrome")),
                box((hx, y1 - 3.4, z + 0.35), (hx + 0.3, y1 - 3.1, z + 0.7), mat("chrome")), box((hx, y1 - 1.3, z + 0.35), (hx + 0.3, y1 - 1.0, z + 0.7), mat("chrome"))]
    return out


def drawers(x0, rows, z):
    """Drawers in a 16 wide module (rows: their y spans), each with a long bar handle."""
    out = []
    for (y0, y1) in rows:
        out += [box((x0 + 0.15, y0, z), (x0 + 15.85, y1, z + 0.35), mat(F)),
                box((x0 + 4.0, y1 - 1.1, z + 0.7), (x0 + 12.0, y1 - 0.8, z + 0.95), mat("chrome")),
                box((x0 + 4.3, y1 - 1.1, z + 0.35), (x0 + 4.6, y1 - 0.8, z + 0.7), mat("chrome")), box((x0 + 11.4, y1 - 1.1, z + 0.35), (x0 + 11.7, y1 - 0.8, z + 0.7), mat("chrome"))]
    return out


def tap(x, z, y0):
    """A kitchen mixer tap: its round body, the high swan neck over the bowl, the lever on top."""
    out = round_part(x, z, 0.5, y0, y0 + 1.6, "chrome", n=3) + round_part(x, z, 0.3, y0 + 1.6, y0 + 6.4, "chrome", n=3)
    out += cable([(x, y0 + 6.4, z), (x, y0 + 7.2, z + 0.8), (x, y0 + 7.2, z + 3.4), (x, y0 + 6.4, z + 4.2), (x, y0 + 5.4, z + 4.2)], 0.26, "chrome")
    out += [box((x - 0.15, y0 + 1.6, z - 0.15), (x + 0.15, y0 + 1.9, z + 2.0), mat("chrome"), {"angle": 22.5, "axis": "x", "origin": [x, y0 + 1.6, z]})]
    return out


def kitchen_run():
    """The kitchen along a wall (10 modules, two high, built whole along x 0..160; the wall at z 0, its user looking
    north at it). Base units on their recessed plinth: the sage-green doors and drawers on the white carcass, each with
    its bar handle; the quartz worktop with its upstand; the tiled splashback; the wall cupboards with the light strip
    under them, open shelves of mugs and glasses over the coffee corner. Module by module: drawers; the oven under the
    induction hob, the chimney hood over it; pan drawers; doors; the sink (its steel bowl set under the worktop, the
    tap, a brush and the soap); the dishwasher behind its panel; doors; two coffee machines (their screens live), the
    kettle, the toaster, a mug tree; the microwave (its clock live)."""
    out = [box((0.6, 0, 1.0), (159.4, 0.9, 9.0), mat("black")), box((0, 0.9, 0.3), (160, 7.8, 9.6), mat(C)),
           box((0, 7.8, 0.3), (66.0, 13.8, 9.6), mat(C)), box((78.0, 7.8, 0.3), (160, 13.8, 9.6), mat(C)),
           box((66.0, 7.8, 0.3), (78.0, 13.8, 2.0), mat(C)), box((66.0, 7.8, 8.6), (78.0, 13.8, 9.6), mat(C))]
    zf = 9.6
    mods = ["drawers", "oven", "pans", "doors", "sink", "dishwasher", "doors", "coffee_a", "coffee_b", "micro"]
    for m, name in enumerate(mods):
        x0 = m * 16.0
        if name == "drawers":
            out += drawers(x0, ((10.4, 13.6), (5.8, 10.2), (1.0, 5.6)), zf)
        elif name == "pans":
            out += drawers(x0, ((7.4, 13.6), (1.0, 7.2)), zf)
        elif name in ("doors", "sink", "coffee_a", "micro"):
            out += door_pair(x0, 1.0, 13.6, zf)
        elif name == "coffee_b":
            out += drawers(x0, ((10.4, 13.6), (1.0, 10.2)), zf)
        elif name == "dishwasher":
            out += [box((x0 + 0.15, 1.0, zf), (x0 + 15.85, 13.6, zf + 0.35), mat(F)),
                    box((x0 + 3.0, 12.4, zf + 0.7), (x0 + 13.0, 12.7, zf + 0.95), mat("chrome")),
                    box((x0 + 3.3, 12.4, zf + 0.35), (x0 + 3.6, 12.7, zf + 0.7), mat("chrome")), box((x0 + 12.4, 12.4, zf + 0.35), (x0 + 12.7, 12.7, zf + 0.7), mat("chrome"))]
        elif name == "oven":
            out += [box((x0 + 0.15, 1.0, zf), (x0 + 15.85, 1.8, zf + 0.35), mat(F)),
                    box((x0 + 0.3, 1.8, zf), (x0 + 15.7, 11.4, zf + 0.4), mat("glassblack")),
                    box((x0 + 1.4, 2.6, zf + 0.4), (x0 + 14.6, 8.8, zf + 0.42), mat("glass")),
                    box((x0 + 2.0, 10.4, zf + 0.8), (x0 + 14.0, 10.7, zf + 1.05), mat("stainless")),
                    box((x0 + 2.3, 10.4, zf + 0.4), (x0 + 2.6, 10.7, zf + 0.8), mat("stainless")), box((x0 + 13.4, 10.4, zf + 0.4), (x0 + 13.7, 10.7, zf + 0.8), mat("stainless")),
                    {**box((x0 + 0.3, 11.6, zf), (x0 + 15.7, 13.6, zf + 0.4), faces("glassblack", south=SCR.face("lk2_oven", 15.4, 2.0, oven_panel))), "shade": False}]
    # the worktop, its upstand; the sink's bowl under it (its opening at x 66..78, z 2.2..8.4)
    wt = [(0, 0, 64.0, 10.2), (80.0, 0, 160, 10.2), (64.0, 0, 80.0, 2.0), (64.0, 8.6, 80.0, 10.2)]
    for x0, z0, x1, z1 in wt:
        out.append(box((x0, 13.8, z0), (x1, 14.6, z1), mat(Q)))
    out += [box((64.0, 13.8, 2.0), (66.0, 14.6, 8.6), mat(Q)), box((78.0, 13.8, 2.0), (80.0, 14.6, 8.6), mat(Q)), box((0, 14.6, 0.3), (160, 15.4, 0.7), mat(Q))]
    out += [box((66.0, 8.0, 2.0), (66.2, 13.8, 8.6), mat("stainless")), box((77.8, 8.0, 2.0), (78.0, 13.8, 8.6), mat("stainless")),
            box((66.2, 8.0, 2.0), (77.8, 13.8, 2.2), mat("stainless")), box((66.2, 8.0, 8.4), (77.8, 13.8, 8.6), mat("stainless")),
            box((66.2, 8.0, 2.2), (77.8, 8.2, 8.4), mat("stainless"))]
    out += round_part(72.0, 5.3, 0.6, 8.2, 8.3, "grip_black", n=3)
    out += tap(72.0, 1.2, 14.6)
    out += round_part(76.0, 1.4, 0.45, 14.6, 16.6, "glove_green", n=3) + round_part(76.0, 1.4, 0.2, 16.6, 17.2, "white", n=3)
    out += [box((79.2, 14.6, 3.0), (79.6, 15.0, 6.4), mat("beech")), box((79.0, 14.6, 6.4), (79.8, 15.2, 7.4), mat("pla_grey"))]
    # the hob, the hood and its chimney
    out.append(box((17.0, 14.6, 1.4), (31.0, 14.8, 9.4), faces("glassblack", up=SCR.face("lk2_hob", 14.0, 8.0, hob_top))))
    out += [box((16.4, 24.0, 0.3), (31.6, 25.2, 8.4), mat("stainless")), box((17.4, 25.2, 0.3), (30.6, 26.4, 6.4), mat("stainless")),
            box((20.6, 26.4, 0.3), (27.4, 31.9, 4.6), mat("stainless")),
            box((17.0, 23.9, 1.0), (31.0, 24.0, 7.8), mat("grille")), {**box((18.0, 23.85, 6.0), (20.0, 23.9, 7.0), mat("light")), "shade": False},
            {**box((28.0, 23.85, 6.0), (30.0, 23.9, 7.0), mat("light")), "shade": False}]
    # the splashback; the wall cupboards and the open shelves; the light strip
    out.append(box((0, 15.4, 0), (160, 23.2, 0.3), {**mat("tile"), "down": {"texture": "#tile"}}))
    for m in range(10):
        x0 = m * 16.0
        if m == 1:
            continue
        if m in (7, 8):
            for y in (23.2, 27.4, 31.4):
                out.append(box((x0, y, 0.3), (x0 + 16, y + 0.4, 5.2), mat("oak")))
            out.append(box((x0, 23.2, 0), (x0 + 16, 31.8, 0.3), mat(C)))
            continue
        out += [box((x0, 23.2, 0.3), (x0 + 16, 31.8, 5.6), mat(C))] + door_pair(x0, 23.3, 31.7, 5.6)
    for m in (0, 2, 3, 4, 5, 6, 9):
        out.append({**box((m * 16.0 + 1.0, 23.0, 3.4), (m * 16.0 + 15.0, 23.2, 4.4), mat("light")), "shade": False})
    # the mugs and glasses on the shelves
    rng = random.Random(3740)
    for m in (7, 8):
        for y in (23.6, 27.8):
            x = m * 16.0 + 1.0
            while x < m * 16.0 + 14.6:
                if rng.random() < 0.55:
                    c = rng.choice(("mug_red", "white", "navy", "mug_grey", "white"))
                    out += MK.round_ring(x + 0.6, 2.6, 0.55, 0.1, y, y + 1.4, c, 0.2, c, n=4)
                    x += 1.6
                else:
                    out += MK.round_ring(x + 0.45, 2.6, 0.45, 0.06, y, y + 1.8, "bottle", 0.2, "bottle", n=4)
                    x += 1.3
    # the coffee corner: two machines (screens live), the kettle, the toaster, the mug tree
    t = 14.6
    for (x0, w, scr) in ((113.0, 6.0, "lk2_cof_a"), (122.0, 5.0, "lk2_cof_b")):
        out += [box((x0, t, 1.0), (x0 + w, t + 7.6, 7.2), mat("steel_black")), box((x0 + 0.3, t, 6.8), (x0 + w - 0.3, t + 0.6, 8.6), mat("stainless")),
                box((x0 + 0.6, t + 0.6, 7.2), (x0 + w - 0.6, t + 2.8, 7.4), mat("grip_black")),
                box((x0 + w / 2 - 0.8, t + 3.6, 7.2), (x0 + w / 2 + 0.8, t + 4.4, 8.0), mat("stainless")),
                box((x0 + w / 2 - 0.5, t + 3.2, 7.6), (x0 + w / 2 - 0.2, t + 3.6, 7.9), mat("stainless")), box((x0 + w / 2 + 0.2, t + 3.2, 7.6), (x0 + w / 2 + 0.5, t + 3.6, 7.9), mat("stainless")),
                {**box((x0 + 0.6, t + 5.2, 7.2), (x0 + w - 0.6, t + 6.8, 7.25), faces("steel_black", south=SCR.face(scr, w - 1.2, 1.6, coffee_screen("READY")))), "shade": False},
                box((x0 + 0.4, t + 7.6, 1.4), (x0 + w - 0.4, t + 8.0, 6.4), mat("grip_black"))]
        out += MK.round_ring(x0 + w / 2, 7.6, 0.55, 0.1, t + 0.6, t + 1.8, "white", 0.3, "coffee", n=4)
    out += round_part(131.0, 4.0, 1.1, t, t + 0.4, "black", n=4) + round_part(131.0, 4.0, 1.0, t + 0.4, t + 3.4, "stainless", n=4)
    out += [box((130.6, t + 3.4, 3.6), (131.4, t + 3.8, 4.4), mat("black")), box((132.0, t + 1.2, 3.7), (132.9, t + 3.0, 4.3), mat("black")),
            box((129.6, t + 2.4, 3.8), (130.2, t + 2.9, 4.2), mat("stainless"))]
    out += [box((134.0, t, 1.6), (138.6, t + 3.2, 5.0), mat("stainless")), box((134.8, t + 3.2, 2.4), (137.8, t + 3.25, 2.8), mat("black")),
            box((134.8, t + 3.2, 3.8), (137.8, t + 3.25, 4.2), mat("black")), box((138.6, t + 1.6, 3.0), (138.9, t + 2.4, 3.6), mat("black"))]
    out += round_part(141.0, 4.0, 0.9, t, t + 0.3, "oak", n=3) + round_part(141.0, 4.0, 0.18, t + 0.3, t + 5.0, "oak", n=3)
    for k, (dx, dz, c) in enumerate(((1.0, 0.0, "mug_red"), (-1.0, 0.0, "navy"), (0.0, 1.0, "white"))):
        out += [box((141.0 + dx * 0.2 - 0.1, t + 4.0 - k * 0.8, 4.0 + dz * 0.2 - 0.1), (141.0 + dx * 1.0 + 0.1, t + 4.2 - k * 0.8, 4.0 + dz * 1.0 + 0.1), mat("oak"))]
        out += MK.round_ring(141.0 + dx * 1.4, 4.0 + dz * 1.4, 0.5, 0.1, t + 2.4 - k * 0.8, t + 3.8 - k * 0.8, c, 0.2, c, n=4)
    # the microwave (its clock live)
    out += [box((145.0, t, 1.2), (154.0, t + 5.0, 7.6), mat("steel_black")), box((145.6, t + 0.6, 7.6), (151.6, t + 4.4, 7.7), mat("glass")),
            box((151.4, t + 0.6, 7.6), (151.8, t + 4.4, 7.9), mat("stainless")),
            {**box((152.2, t + 3.4, 7.6), (153.6, t + 4.2, 7.65), faces("steel_black", south=SCR.face("lk2_micro", 1.4, 0.8, label_plate("14:32", (6, 10, 8), (80, 230, 120), live={"src": "time"})))), "shade": False}]
    for k in range(3):
        out.append(box((152.4, t + 1.0 + k * 0.7, 7.6), (153.4, t + 1.5 + k * 0.7, 7.75), mat("grip_black")))
    # a fruit bowl and a tea towel over the oven's handle
    out += MK.round_ring(40.0, 5.0, 2.0, 0.25, t, t + 1.0, "white", 0.3, "white", n=5)
    for (dx, dz, c, r) in ((-0.6, -0.4, "fruit_orange", 0.6), (0.6, 0.3, "apple", 0.55), (0.0, 0.7, "fruit_orange", 0.55), (-0.5, 0.6, "apple", 0.5)):
        out += round_part(40.0 + dx, 5.0 + dz, r, t + 0.3, t + 0.3 + r * 1.9, c, n=3)
    out += [box((37.6, t + 1.2, 3.0), (41.6, t + 1.6, 4.2), mat("banana"), {"angle": 22.5, "axis": "y", "origin": [39.6, t + 1.2, 3.6]})]
    out += [box((22.0, 9.4, zf + 1.05), (25.0, 10.7, zf + 1.15), mat("red")), box((22.0, 6.6, zf + 0.95), (25.0, 9.4, zf + 1.05), mat("red"))]
    return out


# ------------------------------------------------------------------ the fridges and the vending machine
def fridge_freezer():
    """An American fridge freezer (90 cm wide, 1.78 m; two high, its front to the north): the stainless body on its
    kick grille, the freezer door and the fridge door with their long bar handles on standoffs, the water and ice
    recess in the freezer door (its paddle, its display, live in game), the hinge covers; magnets holding a note, a
    photo and a takeaway menu on the fridge door."""
    s = "stainless"
    out = [box((0.8, 0, 5.0), (15.2, 0.6, 15.4), mat("grille")), box((0.8, 0.6, 4.6), (15.2, 28.6, 15.8), mat(s)),
           box((0.9, 0.7, 4.2), (7.95, 28.5, 4.6), mat(s)), box((8.05, 0.7, 4.2), (15.1, 28.5, 4.6), mat(s)),
           box((1.2, 28.5, 4.4), (3.0, 28.8, 5.6), mat("black")), box((13.0, 28.5, 4.4), (14.8, 28.8, 5.6), mat("black"))]
    for hx in (7.0, 8.7):
        out += [box((hx, 9.0, 3.4), (hx + 0.35, 24.0, 3.75), mat("chrome")),
                box((hx, 9.4, 3.75), (hx + 0.35, 9.8, 4.2), mat("chrome")), box((hx, 23.2, 3.75), (hx + 0.35, 23.6, 4.2), mat("chrome"))]
    out += [box((2.0, 15.0, 4.0), (6.2, 21.0, 4.2), mat("black")), box((2.0, 15.0, 4.2), (6.2, 15.2, 4.6), mat("black")),
            box((3.6, 17.6, 3.8), (4.6, 19.6, 4.0), mat("grip_black")), box((2.4, 15.2, 3.9), (5.8, 15.6, 4.0), mat("grille")),
            {**box((2.6, 21.4, 4.15), (5.6, 22.4, 4.2), faces("black", north=SCR.face("lk2_ice", 3.0, 1.0, label_plate("3 C   ICE", (8, 14, 20), (120, 210, 255), live={"part": "3", "src": "value", "base": 3, "spread": 0.7, "period": 90})))), "shade": False}]
    out += [box((9.6, 18.0, 4.15), (12.4, 21.2, 4.2), faces("paper", north=SCR.face("lk2_menu", 2.8, 3.2, OK.notice("PIZZA", ["PALERMO'S", "free delivery", "over 20", "tel 0118 2240"], (200, 40, 40))))),
            box((12.8, 16.0, 4.15), (14.4, 17.6, 4.2), faces("paper", north=A.region("lk2_photo", 1.6, 1.6, MK.photo("fleet")))),
            box((10.4, 12.4, 4.15), (13.4, 15.2, 4.2), faces("paper", north=SCR.face("lk2_note", 3.0, 2.8, PK.sticky("label your\nfood please!\n- mgmt", (250, 236, 110)))))]
    for (mx, my, c) in ((10.8, 20.8, "red"), (13.6, 17.2, "glove_blue"), (11.9, 14.8, "glove_green")):
        out += swap_yz(round_part(mx, my, 0.3, 3.9, 4.15, c, n=3))
    return out


def drinks_fridge():
    """A glass-fronted drinks cooler (60 cm wide, 1.9 m; two high, its front to the north): the black body, its lit
    COLD DRINKS header, the glass door in its frame with the long handle, five wire shelves of cans and bottles in
    their rows, the light down the inside."""
    b = "steel_black"
    out = [box((2.4, 0, 4.0), (13.6, 1.0, 15.6), mat("grille")),
           box((2.4, 1.0, 4.0), (3.0, 30.4, 15.8), mat(b)), box((13.0, 1.0, 4.0), (13.6, 30.4, 15.8), mat(b)),
           box((3.0, 1.0, 4.0), (13.0, 2.2, 15.8), mat(b)), box((3.0, 26.4, 4.0), (13.0, 30.4, 15.8), mat(b)),
           box((3.0, 2.2, 15.4), (13.0, 26.4, 15.8), mat(b)),
           {**box((2.6, 27.4, 3.7), (13.4, 30.0, 3.8), faces(b, north=SCR.face("lk2_cold", 10.8, 2.6, fridge_sign))), "shade": False},
           box((2.4, 1.6, 3.4), (3.0, 27.0, 4.0), mat(b)), box((13.0, 1.6, 3.4), (13.6, 27.0, 4.0), mat(b)),
           box((3.0, 1.6, 3.4), (13.0, 2.2, 4.0), mat(b)), box((3.0, 26.4, 3.4), (13.0, 27.0, 4.0), mat(b)),
           box((3.0, 2.2, 3.6), (13.0, 26.4, 3.7), mat("glass")),
           box((3.4, 8.0, 2.8), (3.8, 21.0, 3.2), mat("chrome")), box((3.4, 8.4, 3.2), (3.8, 8.8, 3.4), mat("chrome")), box((3.4, 20.2, 3.2), (3.8, 20.6, 3.4), mat("chrome")),
           box((3.0, 2.2, 15.2), (13.0, 26.4, 15.4), mat("white")),
           {**box((12.2, 2.4, 4.6), (12.6, 26.2, 5.0), mat("light")), "shade": False}]
    kinds = [("can_red", 0.42, 1.9), ("can_blue", 0.42, 1.9), ("can_silver", 0.42, 1.9), ("can_green", 0.42, 1.9), ("can_orange", 0.42, 1.9), ("bottle", 0.5, 3.4)]
    rng = random.Random(3750)
    for k, y in enumerate((2.8, 7.4, 12.0, 16.6, 21.2)):
        out.append(box((3.0, y - 0.2, 4.4), (13.0, y, 15.0), mat("wire")))
        tex, r, h = kinds[(k * 2) % len(kinds)] if k != 4 else kinds[5]
        for row_z in (6.0, 9.0):
            x = 3.9
            while x < 12.4:
                tex2, r2, h2 = (tex, r, h) if rng.random() < 0.8 else kinds[rng.randrange(len(kinds))]
                out += round_part(x, row_z, r2, y, y + h2, tex2, n=3)
                if tex2 == "bottle":
                    out += round_part(x, row_z, 0.2, y + h2, y + h2 + 0.6, "white", n=3)
                x += 2 * r2 + 0.25
    return out


def vending_machine():
    """A snack vending machine (90 cm wide, 1.83 m; two high, its front to the north): the black and red cabinet on its
    feet, the big window in its frame, six shelves of spiral coils with their snacks in rows, the lit keypad panel (its
    display live in game), the card reader, the delivery flap at the foot."""
    b, r = "vend", "vend_red"
    out = [box((1.0, 0, 4.0), (2.0, 0.6, 15.0), mat("black")), box((14.0, 0, 4.0), (15.0, 0.6, 15.0), mat("black")),
           box((10.4, 0.6, 3.6), (15.4, 29.2, 15.8), mat(b)), box((0.6, 0.6, 3.6), (1.6, 29.2, 15.8), mat(b)),
           box((1.6, 0.6, 3.6), (10.4, 7.8, 15.8), mat(b)), box((1.6, 27.8, 3.6), (10.4, 29.2, 15.8), mat(b)),
           box((0.6, 29.2, 3.4), (15.4, 29.6, 15.8), mat(r)),
           box((0.6, 0.6, 3.2), (15.4, 1.2, 3.6), mat(r)),
           box((1.0, 7.2, 3.2), (11.0, 7.8, 3.6), mat("alu")), box((1.0, 27.8, 3.2), (11.0, 28.4, 3.6), mat("alu")),
           box((1.0, 7.8, 3.2), (1.6, 27.8, 3.6), mat("alu")), box((10.4, 7.8, 3.2), (11.0, 27.8, 3.6), mat("alu")),
           box((1.6, 7.8, 3.38), (10.4, 27.8, 3.42), mat("glass")),
           box((1.6, 7.8, 15.0), (10.4, 27.8, 15.8), mat("white")),
           {**box((1.8, 27.4, 4.0), (10.2, 27.6, 14.4), mat("light")), "shade": False},
           box((2.0, 2.0, 3.0), (10.0, 5.6, 3.6), mat("black")), box((2.4, 2.4, 2.9), (9.6, 5.2, 3.0), mat("grip_black")),
           {**box((11.6, 14.0, 3.5), (14.8, 24.0, 3.6), faces(b, north=SCR.face("lk2_keypad", 3.2, 10.0, vend_keypad))), "shade": False},
           box((12.0, 10.4, 3.2), (14.4, 12.8, 3.6), mat("black")), box((12.4, 11.2, 3.1), (14.0, 11.4, 3.2), mat("grille"))]
    rng = random.Random(3760)
    for k in range(6):
        y = 8.2 + k * 3.25
        out.append(box((1.6, y - 0.2, 4.0), (10.4, y, 14.8), mat("vend")))
        for j in range(4):
            cx = 2.6 + j * 2.2
            name, bg, fg = SNACKS[(k * 4 + j * 3 + rng.randrange(3)) % len(SNACKS)]
            n = rng.randint(2, 4)
            for q in range(n):
                z0 = 4.6 + q * 2.6
                out.append(box((cx - 0.9, y, z0), (cx + 0.9, y + 2.4, z0 + 0.8), faces("white", north=A.region(f"lk2_snack_{name}", 1.8, 2.4, wrapper(name, bg, fg)))))
            for q in range(4):
                z0 = 4.4 + q * 2.6
                out.append(box((cx - 1.0, y + 0.1, z0), (cx + 1.0, y + 0.25, z0 + 0.15), mat("vcoil")))
                out.append(box((cx - 1.0, y + 0.1, z0 + 1.3), (cx - 0.85, y + 2.0, z0 + 1.45), mat("vcoil")))
            out.append(box((cx - 0.8, y - 0.1, 3.7), (cx + 0.8, y + 0.0, 3.75), faces("vend", north=A.region(f"lk2_price_{k}_{j}", 1.6, 0.1, label_plate(f"{k}{j}", (240, 240, 236))))))
    return out


# ------------------------------------------------------------------ the island and its stools
def kitchen_island():
    """The kitchen island (4 x 1, built whole along x 0..64): its cupboards to the south (doors and drawers, as the
    run's), the quartz top overhanging north as a breakfast bar with the footrest along it; on it a fruit bowl, a jug of
    flowers, the paper left open, two mugs."""
    out = [box((0.6, 0, 7.0), (63.4, 0.9, 14.4), mat("black")), box((0, 0.9, 6.0), (64, 13.8, 15.0), mat(C))]
    for m in range(4):
        x0 = m * 16.0
        out += drawers(x0, ((10.4, 13.6), (5.8, 10.2), (1.0, 5.6)), 15.0) if m in (0, 3) else door_pair(x0, 1.0, 13.6, 15.0)
    # (the doors and drawers above face +z: the island's south side; the run's helpers put their handles out that way)
    out += [box((-0.4, 13.8, 0.6), (64.4, 14.6, 15.6), mat(Q)), box((-0.4, 0.9, 6.0), (0, 13.8, 15.0), mat(Q)), box((64, 0.9, 6.0), (64.4, 13.8, 15.0), mat(Q))]
    out += rod_x(1.0, 63.0, 4.0, 5.2, 0.25, "chrome", n=3)
    for x in (2.0, 22.0, 42.0, 62.0):
        out.append(box((x - 0.15, 4.0, 5.2), (x + 0.15, 4.3, 6.0), mat("chrome")))
    t = 14.6
    out += MK.round_ring(10.0, 8.0, 2.2, 0.25, t, t + 1.1, "steel_black", 0.3, "steel_black", n=5)
    for (dx, dz, c, rr) in ((-0.7, -0.5, "apple", 0.6), (0.6, 0.3, "fruit_orange", 0.6), (-0.2, 0.8, "apple", 0.55), (0.9, -0.6, "fruit_orange", 0.5)):
        out += round_part(10.0 + dx, 8.0 + dz, rr, t + 0.3, t + 0.3 + rr * 1.9, c, n=3)
    out += [box((8.0, t + 1.3, 6.6), (12.2, t + 1.7, 7.6), mat("banana"), {"angle": -22.5, "axis": "y", "origin": [10.0, t + 1.3, 7.0]})]
    out += round_ring(30.0, 8.0, 1.1, 0.12, t, t + 3.6, "bottle", 3.0, "water", n=4)
    for k, (dx, dz, h) in enumerate(((0.0, 0.0, 6.4), (0.5, 0.4, 5.6), (-0.5, 0.3, 5.9), (0.3, -0.5, 5.2), (-0.3, -0.4, 6.0))):
        out += [box((30.0 + dx - 0.06, t + 3.0, 8.0 + dz - 0.06), (30.0 + dx + 0.06, t + h, 8.0 + dz + 0.06), mat("stem")),
                box((30.0 + dx - 0.4, t + h, 8.0 + dz - 0.4), (30.0 + dx + 0.4, t + h + 0.7, 8.0 + dz + 0.4), mat("bloom" if k % 2 == 0 else "fruit_orange"))]
    out += [box((40.0, t, 3.0), (48.0, t + 0.12, 8.6), faces("paper", up=A.region("lk2_paper", 8.0, 5.6, newspaper)))]
    out += MK.round_ring(52.0, 4.0, 0.55, 0.1, t, t + 1.4, "mug_grey", 1.0, "coffee", n=4) + MK.round_ring(55.0, 3.4, 0.55, 0.1, t, t + 1.4, "white", 0.2, "white", n=4)
    return out


def bar_stool(turn=0.0):
    """A bar stool (seat at 65 cm): the round oak seat on its black steel frame, four legs, the footring."""
    out = round_part(8.0, 8.0, 2.4, 10.0, 10.8, "oak", n=4)
    for (x, z) in ((5.6, 5.6), (9.8, 5.6), (5.6, 9.8), (9.8, 9.8)):
        out.append(box((x, 0, z), (x + 0.6, 10.0, z + 0.6), mat("steel_black")))
    out += [box((5.6, 3.6, 5.6), (10.4, 4.0, 6.0), mat("steel_black")), box((5.6, 3.6, 10.0), (10.4, 4.0, 10.4), mat("steel_black")),
            box((5.6, 3.6, 6.0), (6.0, 4.0, 10.0), mat("steel_black")), box((10.0, 3.6, 6.0), (10.4, 4.0, 10.0), mat("steel_black")),
            box((5.6, 9.4, 5.6), (10.4, 10.0, 10.4), mat("steel_black"))]
    if turn:
        for e in out:
            e.setdefault("rotation", {"angle": turn, "axis": "y", "origin": [8.0, 0.0, 8.0]})
    return out


# ------------------------------------------------------------------ the dining tables and chairs
def dining_table(v):
    """A dining table for four (2 x 1, built along x 0..32: 1.8 by 0.8 m): the oak top on its black steel frame and four
    square legs; on it v 0 a jug of flowers, salt and pepper and the napkins; v 1 two plates with cutlery, a glass, the
    salt and pepper; v 2 a laptop left open, a mug, the napkins."""
    out = [box((1.6, 11.4, 1.6), (30.4, 12.0, 14.4), mat("oak")), box((2.6, 10.6, 2.6), (29.4, 11.4, 3.2), mat("steel_black")),
           box((2.6, 10.6, 12.8), (29.4, 11.4, 13.4), mat("steel_black"))]
    for x in (2.6, 28.6):
        for z in (2.6, 12.6):
            out.append(box((x, 0, z), (x + 0.8, 10.6, z + 0.8), mat("steel_black")))
    out += [box((3.4, 10.6, 3.2), (4.0, 11.4, 12.8), mat("steel_black")), box((28.0, 10.6, 3.2), (28.6, 11.4, 12.8), mat("steel_black"))]
    t = 12.0
    salt = round_part(15.0, 8.0, 0.35, t, t + 1.3, "white", n=3) + round_part(15.0, 8.0, 0.25, t + 1.3, t + 1.5, "chrome", n=3)
    salt += round_part(16.2, 8.0, 0.35, t, t + 1.3, "black", n=3) + round_part(16.2, 8.0, 0.25, t + 1.3, t + 1.5, "chrome", n=3)
    napkins = [box((18.0, t, 7.2), (20.2, t + 1.6, 8.8), mat("chrome")), box((18.2, t + 0.2, 7.4), (20.0, t + 1.9, 8.6), mat("white"))]
    if v == 0:
        out += salt + napkins + round_ring(12.0, 8.0, 0.8, 0.1, t, t + 2.4, "bottle", 2.0, "water", n=4)
        for k, (dx, dz, h) in enumerate(((0.0, 0.0, 5.0), (0.35, 0.3, 4.2), (-0.35, 0.25, 4.6))):
            out += [box((12.0 + dx - 0.05, t + 2.0, 8.0 + dz - 0.05), (12.0 + dx + 0.05, t + h, 8.0 + dz + 0.05), mat("stem")),
                    box((12.0 + dx - 0.35, t + h, 8.0 + dz - 0.35), (12.0 + dx + 0.35, t + h + 0.6, 8.0 + dz + 0.35), mat("bloom" if k != 1 else "banana"))]
    elif v == 1:
        out += salt
        for (px, pz) in ((8.0, 4.4), (24.0, 11.6)):
            out += round_part(px, pz, 2.0, t, t + 0.25, "white", n=4) + round_part(px, pz, 1.5, t + 0.25, t + 0.35, "white", n=4)
            out += [box((px - 2.8, t, pz - 1.0), (px - 2.6, t + 0.08, pz + 1.4), mat("chrome")), box((px + 2.6, t, pz - 1.4), (px + 2.8, t + 0.08, pz + 1.2), mat("chrome"))]
        out += MK.round_ring(11.0, 3.6, 0.5, 0.06, t, t + 2.2, "bottle", 0.2, "water", n=4)
    else:
        out += napkins + MK.round_ring(8.0, 5.0, 0.55, 0.1, t, t + 1.4, "mug_red", 1.0, "coffee", n=4)
        out += [box((20.0, t, 2.6), (25.6, t + 0.3, 6.2), mat("alu")),
                box((20.0, t + 0.3, 2.6), (25.6, t + 3.9, 2.9), mat("alu"), {"angle": -22.5, "axis": "x", "origin": [22.8, t + 0.3, 2.6]}),
                {**box((20.2, t + 0.5, 2.9), (25.4, t + 3.7, 2.95), faces("black", south=SCR.face("lk2_lap", 5.2, 3.2, MK.monitor_screen("6_1"))), {"angle": -22.5, "axis": "x", "origin": [22.8, t + 0.3, 2.6]}), "shade": False}]
    return out


def dining_chair(v):
    """A dining chair (seat at 45 cm): the oak seat and the curved oak back on a black steel frame. v 0 tucked in, v 1
    pulled out, v 2 pulled out and turned."""
    dz = 0.0 if v == 0 else 3.0
    out = [box((4.6, 7.0, 4.0 + dz), (11.4, 7.6, 10.6 + dz), mat("oak"))]
    for x in (4.8, 10.6):
        for z in (4.2, 9.8):
            out.append(box((x, 0, z + dz), (x + 0.6, 7.0, z + 0.6 + dz), mat("steel_black")))
    out += [box((4.8, 7.6, 10.0 + dz), (5.4, 13.0, 10.4 + dz), mat("steel_black")), box((10.6, 7.6, 10.0 + dz), (11.2, 13.0, 10.4 + dz), mat("steel_black")),
            box((4.4, 10.8, 10.4 + dz), (11.6, 13.4, 10.9 + dz), mat("oak")), box((5.0, 10.8, 10.9 + dz), (11.0, 13.4, 11.2 + dz), mat("oak")),
            box((5.4, 2.2, 4.4 + dz), (10.6, 2.6, 4.8 + dz), mat("steel_black")), box((5.4, 2.2, 10.0 + dz), (10.6, 2.6, 10.4 + dz), mat("steel_black"))]
    if v == 2:
        for e in out:
            e["rotation"] = {"angle": 22.5, "axis": "y", "origin": [8.0, 0.0, 8.0 + dz]}
    return out


# ------------------------------------------------------------------ the rug, the foosball table, the media console
RUG_W, RUG_D = 4, 3


def lounge_rug():
    """The lounge rug as the floor under it (4 x 3 floor blocks): the vinyl floor block, the rug lying on it (3.6 by
    2.6 m, 8 mm thick) with its border and medallion and the fringes at its ends."""
    W, D = RUG_W * 16.0, RUG_D * 16.0
    out = [box((0, 0, 0), (W, 16, D), {k: {"texture": "#vinyl_base"} for k in ("north", "south", "east", "west", "down", "up")})]
    out.append(box((3.2, 16.0, 3.2), (W - 3.2, 16.5, D - 3.2), faces("rug_side", up=RUG.face("lk2_rug", W - 6.4, D - 6.4, rug_pattern))))
    for x in np.arange(3.4, W - 3.4, 0.8):
        for z0, z1 in ((2.4, 3.2), (D - 3.2, D - 2.4)):
            out.append(box((x, 16.0, z0), (x + 0.25, 16.1, z1), mat("rug_side")))
    return out


def foosball_table():
    """A foosball table (2 x 1, built along x 0..32: 1.4 by 0.75 m, 90 cm): the wooden cabinet on its four legs and
    levellers, the felt field with its lines, the goals at the ends, eight chrome rods across it with their black
    grips (each side its own), the players on them in red and blue, the bead counters on top, a ball."""
    w = "walnut"
    x0, x1, z0, z1 = 4.8, 27.2, 2.0, 14.0
    out = []
    for x in (x0 + 0.4, x1 - 1.6):
        for z in (z0 + 0.4, z1 - 1.6):
            out += [box((x, 0.4, z), (x + 1.2, 10.8, z + 1.2), mat(w)), box((x + 0.2, 0, z + 0.2), (x + 1.0, 0.4, z + 1.0), mat("black"))]
    out += [box((x0, 10.8, z0), (x1, 14.4, z0 + 0.6), mat(w)), box((x0, 10.8, z1 - 0.6), (x1, 14.4, z1), mat(w)),
            box((x0, 10.8, z0 + 0.6), (x0 + 0.6, 14.4, z1 - 0.6), mat(w)), box((x1 - 0.6, 10.8, z0 + 0.6), (x1, 14.4, z1 - 0.6), mat(w)),
            box((x0 + 0.6, 11.4, z0 + 0.6), (x1 - 0.6, 11.6, z1 - 0.6), faces("baize", up=A.region("lk2_field", x1 - x0 - 1.2, z1 - z0 - 1.2, felt_field)))]
    for x in (x0, x1 - 0.6):
        out.append(box((x - 0.01, 11.6, 6.4), (x + 0.61, 13.2, 9.6), mat("black")))
    rods = [(1, "red"), (2, "red"), (5, "blue"), (3, "red"), (3, "blue"), (5, "red"), (2, "blue"), (1, "blue")]
    for k, (n, team) in enumerate(rods):
        rx = x0 + 2.0 + k * (x1 - x0 - 4.0) / 7
        grip_side = -1 if team == "red" else 1
        zs = (z0 - 2.4, z1 + 0.6) if grip_side < 0 else (z0 - 0.6, z1 + 2.4)
        out += swap_yz(round_part(rx, 13.2, 0.16, zs[0], zs[1], "chrome", n=3))
        gz0 = zs[0] if grip_side < 0 else zs[1] - 2.2
        out += swap_yz(round_part(rx, 13.2, 0.4, gz0, gz0 + 2.2, "black", n=3))
        for j in range(n):
            pz = z0 + 0.6 + (j + 1) * (z1 - z0 - 1.2) / (n + 1)
            out += [box((rx - 0.35, 11.9, pz - 0.3), (rx + 0.35, 13.8, pz + 0.3), mat("team_red" if team == "red" else "team_blue")),
                    box((rx - 0.28, 13.8, pz - 0.28), (rx + 0.28, 14.3, pz + 0.28), mat("wsteel")),
                    box((rx - 0.2, 11.65, pz - 0.35), (rx + 0.2, 11.9, pz + 0.35), mat("black"))]
    out += round_part(15.4, 7.0, 0.22, 11.6, 12.04, "white", n=3)
    for (cx, team) in ((x0 + 3.0, "red"), (x1 - 3.0, "blue")):
        out.append(box((cx - 2.0, 14.4, 1.6), (cx + 2.0, 14.5, 2.0), mat("chrome")))
        for b in range(5):
            out.append(box((cx - 1.8 + b * 0.8, 14.3, 1.55), (cx - 1.3 + b * 0.8, 14.7, 2.05), mat("team_red" if team == "red" else "team_blue")))
    return out


def media_console():
    """The media console under the lounge's screen (3 x 1, built along x 0..48: its front to the north, the wall at the
    south): the oak top and sides on black feet, three open bays and two doors; the games console in the middle bay
    (its light on), two controllers on top, the soundbar, a stack of board games, a speaker either end."""
    o = "oak"
    out = [box((0.8, 0, 6.0), (1.6, 1.4, 14.6), mat("steel_black")), box((46.4, 0, 6.0), (47.2, 1.4, 14.6), mat("steel_black")),
           box((0.4, 1.4, 5.6), (47.6, 2.0, 15.4), mat(o)), box((0.4, 7.4, 5.6), (47.6, 8.0, 15.4), mat(o)),
           box((0.4, 2.0, 5.6), (1.0, 7.4, 15.4), mat(o)), box((47.0, 2.0, 5.6), (47.6, 7.4, 15.4), mat(o)),
           box((1.0, 2.0, 15.0), (47.0, 7.4, 15.4), mat("steel_black"))]
    for x in (16.0, 32.0):
        out.append(box((x - 0.3, 2.0, 5.8), (x + 0.3, 7.4, 15.0), mat(o)))
    out += [box((1.1, 2.1, 5.4), (15.6, 7.3, 5.8), mat("front")), box((32.4, 2.1, 5.4), (46.9, 7.3, 5.8), mat("front")),
            box((14.0, 4.0, 5.1), (14.4, 5.6, 5.4), mat("chrome")), box((33.6, 4.0, 5.1), (34.0, 5.6, 5.4), mat("chrome"))]
    out += [box((19.0, 2.0, 7.0), (29.0, 3.4, 13.6), mat("white")), box((19.0, 3.4, 7.4), (29.0, 3.6, 13.2), mat("black")),
            {**box((20.0, 2.6, 6.95), (21.2, 2.8, 7.0), mat("light")), "shade": False}]
    t = 8.0
    for (cx, cz) in ((10.0, 9.0), (13.6, 10.2)):
        out += [box((cx - 1.1, t, cz - 0.6), (cx + 1.1, t + 0.5, cz + 0.6), mat("black")),
                box((cx - 1.4, t, cz - 0.2), (cx - 1.0, t + 0.45, cz + 1.0), mat("black")), box((cx + 1.0, t, cz - 0.2), (cx + 1.4, t + 0.45, cz + 1.0), mat("black"))]
    out += [box((16.0, t, 6.6), (32.0, t + 1.4, 8.6), mat("black")), box((16.2, t + 0.2, 6.5), (31.8, t + 1.2, 6.6), mat("grille"))]
    for k, c in enumerate(("team_red", "team_blue", "holder_green", "holder_yellow")):
        # (clear of the speaker beside them: it starts at x 41.6)
        out.append(box((34.0 + k * 0.15, t + k * 0.6, 8.0 + k * 0.1), (41.0 + k * 0.15, t + (k + 1) * 0.6, 13.0 + k * 0.1), mat(c)))
    for x in (2.4, 41.6):
        out += [box((x, t, 9.0), (x + 4.0, t + 6.4, 13.0), mat("black")), box((x + 0.4, t + 1.0, 8.9), (x + 3.6, t + 5.4, 9.0), mat("grille"))]
    return out


# ------------------------------------------------------------------ the lounge TV
# The picture of a 75 inch class TV, 1.65 by 0.93 m (26.5 by 15 px), and the thin frame round it.
TV_X0, TV_X1, TV_Y0, TV_Y1, TV_RIM = 10.75, 37.25, 0.5, 15.5, 0.15
TV_ROLE = 16                     # the desks' page role of its channel (tower/RoomKitDisplayBlock.TV, client/TowerTv)


def tv_picture(img):
    """The TV's picture: TOWER TV (tools/data/desk_pages/16_0.png, drawn by client/TowerTv), live in game."""
    ImageDraw.Draw(img).rectangle((0, 0, img.size[0], img.size[1]), fill=(12, 24, 52))
    TK._page_into(img, f"{TV_ROLE}_0", (0, 0, img.size[0], img.size[1]))
    return img


def lounge_tv():
    """The lounge TV (Justin's walk through of 24 October 2026, notes 16 and 20; take 1 approved 25 October), built whole
    along x 0..48, its picture's middle over the middle block, its face to the north, the wall to the south: a slim panel
    2.8 cm deep, its picture under glass in a dark metal frame a hair proud of it, the maker's badge under it, the
    electronics' hump behind, on a flat wall bracket (the panel's front 12 cm off the wall). One block high, its bottom
    2 cm over its block's foot (hung at 1 m, its picture's middle at 1.5 m). The picture is the TOWER TV channel, live."""
    x0, x1, y0, y1 = TV_X0 - TV_RIM, TV_X1 + TV_RIM, TV_Y0 - TV_RIM, TV_Y1 + TV_RIM
    out = [box((20.6, 4.0, 15.75), (27.4, 11.0, 16.0), mat("tv_bracket")),                          # the plate on the wall
           box((21.0, 3.2, 15.1), (21.6, 11.8, 15.75), mat("tv_bracket")), box((26.4, 3.2, 15.1), (27.0, 11.8, 15.75), mat("tv_bracket")),
           box((14.0, 2.2, 14.7), (34.0, 13.2, 15.1), mat("tv_body")),                              # the hump
           box((x0, y0, 14.35), (x1, y1, 14.7), mat("tv_body")),                                     # the panel
           {**box((TV_X0, TV_Y0, 14.25), (TV_X1, TV_Y1, 14.35),
                  faces("tv_body", north=TVS.face("lk2_tv", TV_X1 - TV_X0, TV_Y1 - TV_Y0, tv_picture))), "shade": False},
           box((x0, y0, 14.15), (x1, TV_Y0, 14.35), mat("tv_frame")), box((x0, TV_Y1, 14.15), (x1, y1, 14.35), mat("tv_frame")),
           box((x0, TV_Y0, 14.15), (TV_X0, TV_Y1, 14.35), mat("tv_frame")), box((TV_X1, TV_Y0, 14.15), (x1, TV_Y1, 14.35), mat("tv_frame")),
           box((23.3, y0 - 0.2, 14.25), (24.7, y0, 14.55), mat("tv_logo"))]                         # the badge
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, RUG, TVS
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **OK.MATERIALS, **MATERIALS}
    mats["lk2_rug_side"] = lambda: TK.solid((60, 70, 100), 3790, 2.0)
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(512, TX, "atlas")
    SCR = PK.ShelfAtlas(2048, 4 * TX, "screens")
    RUG = PK.ShelfAtlas(1024, 12, "rug")
    TVS = PK.ShelfAtlas(1024, 32, "tvscreen")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK, OK):
        m.A, m.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[4:] if k.startswith("lk2_") else k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("lk2_atlas"), "screens": ref("lk2_screens"), "rug": ref("lk2_rugs"), "particle": ref("lk2_carcass"),
              "tvscreen": ref("lk2_tvscreen"),
              "vinyl_base": "jugcraft:block/vinyl_floor"})

    def model(name, elements, prepared=False):
        if not prepared:
            prep(elements)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, variants):
        (bs_dir / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")

    def prep(elements):
        for e in elements:
            for i in range(3):
                if e["from"][i] > e["to"][i]:
                    e["from"][i], e["to"][i] = e["to"][i], e["from"][i]
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        return elements

    def faced(model_name, extra="", off=0):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": (y - off) % 360} if (y - off) % 360 else {})} for f, y in FACINGS.items()}

    def tall(name, els):
        v = {}
        for h, part in zip(("lower", "upper"), cut_y(prep(els), 2)):
            model(f"{name}_{h}", part, True)
            v.update(faced(f"{name}_{h}", f",half={h}"))
        return v

    def wide(name, els, n, extra="", key=None):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{key or name}_{p}", part, True)
            v.update(faced(f"{key or name}_{p}", f",part={p}{extra}"))
        return v

    v = {}
    for h, half in zip(("lower", "upper"), cut_y(prep(kitchen_run()), 2)):
        for p, part in enumerate(cut_parts(half, 10)):
            model(f"kitchen_run_{h}_{p}", part, True)
            v.update(faced(f"kitchen_run_{h}_{p}", f",half={h},part={p}"))
    states("kitchen_run", v)
    states("fridge_freezer", tall("fridge_freezer", fridge_freezer()))
    states("drinks_fridge", tall("drinks_fridge", drinks_fridge()))
    states("vending_machine", tall("vending_machine", vending_machine()))
    states("kitchen_island", wide("kitchen_island", kitchen_island(), 4))
    v = {}
    for k, turn in enumerate((0.0, 22.5)):
        model(f"bar_stool_{k}", bar_stool(turn))
        v.update(faced(f"bar_stool_{k}", f",variant={k}"))
    states("bar_stool", v)
    v = {}
    for k in range(3):
        v.update(wide("dining_table", dining_table(k), 2, f",variant={k}", key=f"dining_table_{k}"))
    states("dining_table", v)
    v = {}
    for k in range(3):
        model(f"dining_chair_{k}", dining_chair(k))
        v.update(faced(f"dining_chair_{k}", f",variant={k}"))
    states("dining_chair", v)
    v = {}
    for xi, xpart in enumerate(cut_parts(prep(lounge_rug()), RUG_W)):
        for zi, part in enumerate(cut_z_any(xpart, RUG_D)):
            model(f"lounge_rug_{zi * RUG_W + xi}", part, True)
            v[f"part={zi * RUG_W + xi}"] = {"model": ref(f"lounge_rug_{zi * RUG_W + xi}")}
    states("lounge_rug", v)
    states("foosball_table", wide("foosball_table", foosball_table(), 2))
    states("media_console", wide("media_console", media_console(), 3))
    states("lounge_tv", wide("lounge_tv", lounge_tv(), 3))
    A.img.save(tex_dir / "lk2_atlas.png")
    SCR.img.save(tex_dir / "lk2_screens.png")
    RUG.img.save(tex_dir / "lk2_rugs.png")
    TVS.img.save(tex_dir / "lk2_tvscreen.png")
    return A.y + A.row, SCR.y + SCR.row, RUG.y + RUG.row


def cut_z_any(elements, n):
    """cut_z for a floor piece: its up faces' pictures cut along z too (their v share)."""
    out = [[] for _ in range(n)]
    for e in elements:
        z0, z1 = e["from"][2], e["to"][2]
        for p in range(n):
            a, b = max(z0, p * 16), min(z1, (p + 1) * 16)
            if b - a < 1e-6:
                continue
            q = json.loads(json.dumps(e))
            q["from"][2], q["to"][2] = round(a - p * 16, 4), round(b - p * 16, 4)
            fc = q["faces"]
            for k in ("up", "down", "east", "west"):
                uv = fc.get(k, {}).get("uv")
                if uv and (a > z0 + 1e-6 or b < z1 - 1e-6):
                    sv = (uv[3] - uv[1]) / (z1 - z0)
                    lo, hi = (a - z0, b - z0) if k != "down" else (z1 - b, z1 - a)
                    fc[k]["uv"] = [uv[0], round(uv[1] + lo * sv, 5), uv[2], round(uv[1] + hi * sv, 5)]
            if a > z0 + 1e-6:
                fc.pop("north", None)
            if b < z1 - 1e-6:
                fc.pop("south", None)
            out[p].append(q)
    return out


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
