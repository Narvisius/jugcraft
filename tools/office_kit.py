"""The tower's open offices (work list item 22 part B, room 6, the south-east office; room 8 will share it), to the
standard Justin set on the lab: everything at its real size (a pixel is 6.25 cm), modelled rather than painted on
(no drawers drawn on a picture), the screens live in game, materials at one density and seamless, things varied.

Pieces: office_bench (3 x 2: two desks a side back to back, the acoustic screen between; its clutter a variant),
office_bench_single (3 x 1: two desks facing a wall), phone_booth (two high), recycling_station, storage_cupboard (two
high), wall_whiteboard (2 x 1, on a wall), office_noticeboard (2 x 1, on a wall). With them the planning office's steel
filing cabinets and copier, the lab's task chairs, the manager's coat stand, the digital clock.
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
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None
SCR = None
PRT = None      # the boards' prints (32 texels a pixel)
MON = None      # the monitors' and laptops' screens (48 texels a pixel; live in game)
MONO = MK.MONO
swap_xy, rod_x = LK.swap_xy, LK.rod_x
cable, cut_y, cut_z, bevel_box, label_plate = RK.cable, RK.cut_y, RK.cut_z, RK.bevel_box, RK.label_plate
_fit = MK._text_fit

MATERIALS = {
    "ok_fabric": lambda: MK.weave((86, 100, 118), 3601, 1, 4), "ok_frame": lambda: PK.laminate((236, 236, 234), 3602),
    "ok_ped": lambda: PK.laminate((232, 232, 230), 3603), "ok_mat": lambda: TK.solid((40, 42, 46), 3604, 1.0),
    "ok_booth": lambda: MK.weave((62, 92, 84), 3605, 1, 4), "ok_felt": lambda: MK.weave((130, 136, 140), 3606, 1, 3),
    "ok_board": lambda: TK.solid((246, 247, 248), 3607, 0.6), "ok_bin_blue": lambda: TK.solid((40, 90, 170), 3608, 1.4),
    "ok_bin_green": lambda: TK.solid((46, 130, 70), 3609, 1.4), "ok_bin_black": lambda: TK.solid((38, 38, 42), 3610, 1.2),
    "ok_bin_yellow": lambda: TK.solid((226, 186, 36), 3611, 1.4), "ok_glass": lambda: MK.rgba((200, 220, 226), 70, 3612),
    "ok_ream": lambda: TK.solid((236, 236, 232), 3613, 1.0), "ok_ream_wrap": lambda: TK.solid((60, 110, 170), 3614, 1.4),
}


# ------------------------------------------------------------------ pictures
def board_writing(img):
    """The whiteboard's writing: this week's plan in marker, a sketch of the pad layout, a list ticked off."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(246, 247, 248))
    fb = ImageFont.truetype(FONT, max(8, round(H * 0.075)))
    fr = ImageFont.truetype(FONT_R, max(8, round(H * 0.055)))
    d.text((W * 0.04, H * 0.06), "WEEK 43", font=fb, fill=(30, 60, 160))
    rows = [("Mon", "fleet audit - T2s", (30, 60, 160)), ("Tue", "R-101 crane: re-route BRAVO", (190, 40, 40)), ("Wed", "pad 3 resurfacing", (30, 30, 34)),
            ("Thu", "new pilots induction 10:00", (30, 120, 60)), ("Fri", "monthly report due!", (190, 40, 40))]
    for k, (day, t, c) in enumerate(rows):
        y = H * (0.2 + k * 0.1)
        d.text((W * 0.05, y), day, font=fr, fill=(30, 30, 34))
        d.text((W * 0.14, y), t, font=fr, fill=c)
        if k < 2:
            d.line((W * 0.53, y + H * 0.03, W * 0.55, y + H * 0.05, W * 0.58, y), fill=(30, 120, 60), width=max(2, H // 90))
    # the pad layout sketched, an arrow, a box
    cx, cy = W * 0.7, H * 0.42
    for k in range(3):
        x = cx + (k - 1) * W * 0.09
        d.ellipse((x - W * 0.035, cy - W * 0.035, x + W * 0.035, cy + W * 0.035), outline=(30, 30, 34), width=max(2, H // 120))
        d.text((x, cy), str(k + 1), font=fr, fill=(30, 30, 34), anchor="mm")
    d.line((cx + W * 0.09, cy - W * 0.05, cx + W * 0.13, cy - W * 0.09), fill=(190, 40, 40), width=max(2, H // 100))
    d.text((cx + W * 0.13, cy - W * 0.12), "CLOSED", font=fr, fill=(190, 40, 40), anchor="mm")
    d.rectangle((W * 0.56, H * 0.62, W * 0.84, H * 0.9), outline=(30, 60, 160), width=max(2, H // 120))
    d.text((W * 0.58, H * 0.66), "Q4 targets", font=fr, fill=(30, 60, 160))
    d.text((W * 0.58, H * 0.75), "on time 97%", font=fr, fill=(30, 60, 160))
    d.text((W * 0.58, H * 0.83), "deliveries +12%", font=fr, fill=(30, 60, 160))
    d.text((W * 0.05, H * 0.8), "DON'T ERASE", font=fb, fill=(190, 40, 40))
    return img


def notice(title, lines, band):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 246))
        d.rectangle((0, 0, W, H * 0.18), fill=band)
        d.text((W / 2, H * 0.09), title, font=_fit(d, title, FONT, round(H * 0.1), W * 0.7), fill=(255, 255, 255), anchor="mm")
        y = H * 0.26
        for t in lines:
            d.text((W * 0.07, y), t, font=_fit(d, t, FONT_R, round(H * 0.065), W * 0.86), fill=(30, 30, 34))
            y += H * 0.1
        return img
    return draw


def rota(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246))
    d.text((W / 2, H * 0.06), "KITCHEN ROTA", font=_fit(d, "KITCHEN ROTA", FONT, round(H * 0.07), W * 0.7), fill=(30, 30, 34), anchor="mm")
    names = ["REYES", "OKAFOR", "SILVA", "BRANDT", "HADDAD"]
    for k in range(6):
        y = H * (0.14 + k * 0.13)
        d.line((W * 0.05, y, W * 0.95, y), fill=(120, 120, 126), width=1)
        if k < 5:
            d.text((W * 0.08, y + H * 0.035), ["MON", "TUE", "WED", "THU", "FRI"][k], font=_fit(d, "MON", FONT, round(H * 0.05), W * 0.2), fill=(30, 30, 34))
            d.text((W * 0.4, y + H * 0.035), names[k], font=_fit(d, names[k], FONT_R, round(H * 0.05), W * 0.5), fill=(30, 60, 160))
    return img


def card(img):
    """A birthday card (front): balloons."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(252, 236, 210))
    for k, c in enumerate(((220, 60, 60), (60, 120, 220), (240, 190, 40))):
        x, y = W * (0.3 + k * 0.2), H * (0.3 + (k % 2) * 0.08)
        d.ellipse((x - W * 0.1, y - W * 0.13, x + W * 0.1, y + W * 0.13), fill=c)
        d.line((x, y + W * 0.13, W * 0.5, H * 0.75), fill=(90, 90, 90), width=1)
    d.text((W / 2, H * 0.86), "HAPPY BIRTHDAY", font=_fit(d, "HAPPY BIRTHDAY", FONT, round(H * 0.08), W * 0.9), fill=(160, 40, 80), anchor="mm")
    return img


def booth_sign(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 34, 38))
    d.text((W / 2, H / 2), "PHONE", font=_fit(d, "PHONE", FONT, round(H * 0.6), W - 8), fill=(220, 230, 236), anchor="mm")
    return img


# ------------------------------------------------------------------ the desks
PAGES = ["6_0", "7_0", "8_1", "9_0", "10_1", "11_0", "12_2", "13_0", "14_1", "3_1"]


def pedestal(x0, z0, front):
    """A mobile pedestal (40 by 55 cm, 60 cm tall) from (x0, z0), its drawers to `front` ("north" or "south"): the
    white body on its four castors, the pencil drawer and two drawers proud of it, their bar pulls, the lock."""
    w, d = 6.4, 8.8
    s = -1 if front == "north" else 1
    fz = z0 if front == "north" else z0 + d
    out = []
    for x in (x0 + 0.4, x0 + w - 1.0):
        for z in (z0 + 0.4, z0 + d - 1.0):
            out += [box((x, 0.6, z), (x + 0.6, 0.8, z + 0.6), mat("black"))] + rod_x(x + 0.1, x + 0.5, 0.3, z + 0.3, 0.3, "grip_black", n=3)
    out.append(box((x0, 0.8, z0), (x0 + w, 9.6, z0 + d), mat("ped")))
    for k, (y0, y1) in enumerate(((7.6, 9.4), (4.4, 7.4), (1.0, 4.2))):
        out.append(box((x0 + 0.1, y0, fz), (x0 + w - 0.1, y1, fz + s * 0.25), mat("ped")))
        hy = y1 - 0.9
        out.append(box((x0 + 2.0, hy, fz + s * 0.25), (x0 + 4.4, hy + 0.3, fz + s * 0.55), mat("chrome")))
    out += swap_yz(round_part(x0 + w - 1.0, 8.9, 0.2, min(fz + s * 0.25, fz + s * 0.4), max(fz + s * 0.25, fz + s * 0.4), "chrome", n=3))
    return out


def workstation(x0, side, rng, n, spine=15.8):
    """One desk of a bench (1.5 m wide, 80 cm deep) from x0: side A's user sits north of it looking south (its edge at z
    3.2), side B's south looking north (its edge at z 28.8). The top; two monitors on their arm off the spine (or one
    and a laptop), keyboard and mouse on a mat; then what this desk has: the phone, a mug, a notepad and pen, papers, a
    photo, a little plant, a headset, a lamp, a bottle; the pedestal under it at the user's right, a bin."""
    A_ = side == "A"
    zu = 3.2 if A_ else 28.8                  # the user's edge
    zs = spine if A_ else 16.2                # the spine edge
    d = 1 if A_ else -1                       # toward the spine
    zz = lambda a: zu + d * a                 # a depth from the user's edge
    span = lambda a, b: (min(zz(a), zz(b)), max(zz(a), zz(b)))
    t = 11.8
    out = [box((x0 + 0.1, t - 0.4, min(zu, zs)), (x0 + 23.8, t, max(zu, zs)), mat("oak"))]
    cx = x0 + 12.0
    face = "north" if A_ else "south"
    # the monitor arm: its pole at the spine, the bar, the monitors (live in game: the tower's pages)
    pz = zz(11.9)
    out += round_part(cx, pz, 0.32, t, t + 6.0, "graphite", n=3)
    out.append(box((cx - 6.0, t + 5.4, min(pz, zz(11.2))), (cx + 6.0, t + 5.8, max(pz, zz(11.2))), mat("graphite")))
    laptop = rng.random() < 0.25
    mons = [cx - 8.75, cx + 0.15] if not laptop else [cx - 4.3]
    for k, mx in enumerate(mons):
        z0, z1 = span(10.6, 11.2)
        page = PAGES[(n * 2 + k) % len(PAGES)]
        sz0, sz1 = (z0 - 0.05, z0) if A_ else (z1, z1 + 0.05)
        out += [box((mx, t + 2.2, z0), (mx + 8.6, t + 7.4, z1), mat("black")),
                {**box((mx + 0.15, t + 2.35, sz0), (mx + 8.45, t + 7.25, sz1), faces("black", **{face: MON.face(f"ok_mon_{page}", 8.3, 4.9, MK.monitor_screen(page))})), "shade": False}]
        if rng.random() < 0.3:
            col = rng.choice(("holder_yellow", "eraser", "holder_green"))
            sx = mx + rng.uniform(0.2, 7.2)
            out.append(box((sx, t + 6.2, sz0 - 0.02 if A_ else sz1), (sx + 1.0, t + 7.2, sz0 if A_ else sz1 + 0.02), mat(col)))
    if laptop:
        lz0, lz1 = span(6.8, 10.4)
        hz = lz1 if A_ else lz0
        lid = {"angle": 22.5 * d, "axis": "x", "origin": [cx + 7.8, t + 0.3, hz]}
        page = PAGES[(n * 2 + 1) % len(PAGES)]
        kb2 = MK.keyboard_top if A_ else (lambda img: MK.keyboard_top(img).rotate(180))
        out += [box((cx + 5.0, t, lz0), (cx + 10.6, t + 0.3, lz1), faces("alu", up=A.region(f"ok_lkbd_{side}", 5.6, 3.6, kb2))),
                box((cx + 5.0, t + 0.3, hz - 0.3 if A_ else hz), (cx + 10.6, t + 3.9, hz if A_ else hz + 0.3), mat("alu"), dict(lid)),
                {**box((cx + 5.2, t + 0.5, hz - 0.35 if A_ else hz + 0.3), (cx + 10.4, t + 3.7, hz - 0.3 if A_ else hz + 0.35),
                       faces("black", **{face: MON.face(f"ok_lap_{page}", 5.2, 3.2, MK.monitor_screen(page))}), dict(lid)), "shade": False}]
    # keyboard, mat, mouse
    kz0, kz1 = span(2.4, 5.0)
    kb = MK.keyboard_top if A_ else (lambda img: MK.keyboard_top(img).rotate(180))
    out += [box((cx - 4.0, t, kz0), (cx + 4.0, t + 0.35, kz1), faces("black", up=A.region(f"ok_kbd_{side}", 8.0, 2.6, kb))),
            box((cx + 5.0, t, min(zz(2.0), zz(6.4))), (cx + 9.4, t + 0.05, max(zz(2.0), zz(6.4))), mat("mat")),
            box((cx + 6.6, t + 0.05, min(zz(3.4), zz(4.8))), (cx + 7.6, t + 0.45, max(zz(3.4), zz(4.8))), mat("black"))]
    # what this desk has on it
    if rng.random() < 0.5:
        pz0, pz1 = span(5.6, 9.6)
        px = x0 + rng.choice((1.2, 18.4))
        out += [box((px, t, pz0), (px + 4.4, t + 0.6, pz1), mat("black")),
                box((px + 0.8, t + 0.4, min(zz(5.8), zz(9.4))), (px + 4.0, t + 0.8, max(zz(5.8), zz(9.4))), faces("black", up=SCR.face(f"ok_phone_{side}", 3.2, 3.6, MK.phone_face if A_ else TK.turned180(MK.phone_face))),
                    {"angle": -22.5 * d, "axis": "x", "origin": [px + 2.2, t + 0.6, zz(7.6)]})]
    if rng.random() < 0.7:
        mx_, mz_ = x0 + rng.uniform(1.5, 4.0), zz(rng.uniform(2.0, 4.0))
        c = rng.choice(("mug_red", "mug_grey", "white", "navy"))
        out += MK.round_ring(mx_, mz_, 0.55, 0.1, t, t + 1.4, c, 1.0, "coffee", n=4) + [box((mx_ + 0.55, t + 0.3, mz_ - 0.15), (mx_ + 0.9, t + 1.1, mz_ + 0.15), mat(c))]
    if rng.random() < 0.6:
        nz0, nz1 = span(1.0, 5.0)
        nx = x0 + rng.uniform(16.0, 19.0)
        out += [box((nx, t, nz0), (nx + 3.0, t + 0.3, nz1), faces("paper", up=A.region(f"ok_note_{n}", 3.0, 4.0, MK.page_sheet(1100 + n)))),
                box((nx + 3.2, t, zz(2.0) - 0.1), (nx + 5.6, t + 0.2, zz(2.0) + 0.1), mat("navy"))]
    if rng.random() < 0.5:
        sz0, sz1 = span(6.0, 10.0)
        sx = x0 + rng.choice((1.0, 18.0))
        for j in range(rng.randint(2, 5)):
            out.append(box((sx + j * 0.05, t + j * 0.12, sz0), (sx + 3.4 + j * 0.05, t + (j + 1) * 0.12, sz1), mat(rng.choice(("paper", "ream", "manila")))))
    if rng.random() < 0.3:
        fz = zz(rng.uniform(8.5, 9.5))
        fx = x0 + rng.uniform(2.0, 5.0)
        out += [box((fx, t, fz - 0.2), (fx + 2.6, t + 2.0, fz + 0.2), mat("frame"),
                    {"angle": 22.5 * d, "axis": "x", "origin": [fx + 1.3, t, fz]})]
    if rng.random() < 0.3:
        gx, gz = x0 + rng.uniform(19.0, 22.0), zz(rng.uniform(8.0, 9.6))
        out += round_ring(gx, gz, 0.9, 0.15, t, t + 1.6, "pot", 1.3, "soil", n=4) + MK.snake_plant(gx, gz, t + 1.3, scale=0.42)
    if rng.random() < 0.4:
        hx, hz = x0 + 22.6, zz(9.0)
        out += [box((hx, t - 1.6, hz - 0.15), (hx + 0.4, t - 0.4, hz + 0.15), mat("graphite")),
                box((hx - 0.6, t - 2.8, hz - 0.8), (hx + 0.6, t - 1.6, hz + 0.8), mat("black"))]
    if rng.random() < 0.25:
        lx, lz = x0 + 21.0, zz(10.2)
        out += round_part(lx, lz, 0.9, t, t + 0.3, "black", n=3) + round_part(lx, lz, 0.15, t + 0.3, t + 5.5, "chrome", n=3)
        out += [box((lx - 3.2, t + 5.3, lz - 0.6), (lx + 0.2, t + 5.9, lz + 0.6), mat("black")),
                {**box((lx - 3.0, t + 5.25, lz - 0.4), (lx - 0.2, t + 5.3, lz + 0.4), mat("light")), "shade": False}]
    if rng.random() < 0.3:
        bx, bz = x0 + rng.uniform(3.0, 6.0), zz(rng.uniform(6.0, 8.0))
        out += round_part(bx, bz, 0.45, t, t + 3.2, "water", n=3) + round_part(bx, bz, 0.3, t + 3.2, t + 3.7, "glove_blue", n=3)
    # the pedestal at the user's right, a bin
    if A_:
        out += pedestal(x0 + 1.6, 4.4, "north")
    else:
        out += pedestal(x0 + 23.8 - 1.6 - 6.4, 16.0 + 3.6, "south")
    if rng.random() < 0.5:
        bx = x0 + (18.0 if A_ else 4.0)
        out += round_ring(bx, zz(6.0), 1.4, 0.15, 0, 4.4, "basket", 0.3, "basket", n=4)
    return out


def office_bench(double=True, v=0):
    """A bench of desks (3 blocks long, built along x 0..48): two desks a side (back to back when double, 2 blocks deep;
    one side against a wall when not, 1 block): the white A-frame legs on their feet at the ends and the middle, the
    spine beam and the cable tray along it, the oak tops, the fabric acoustic screen down the spine with its aluminium
    cap; each desk its own things (v changes what lies where)."""
    fr = "frame"
    zA, zB = 3.2, (28.8 if double else 16.0)
    out = []
    for x in (0.6, 23.6, 46.6):
        out += [box((x, 0, zA + 0.4), (x + 0.8, 0.6, zB - 0.4), mat(fr)), box((x, 10.6, zA + 0.8), (x + 0.8, 11.4, zB - 0.8), mat(fr)),
                box((x, 0.6, zA + 1.2), (x + 0.8, 10.6, zA + 2.0), mat(fr)), box((x, 0.6, zB - 2.0), (x + 0.8, 10.6, zB - 1.2), mat(fr))]
    out += [box((1.4, 10.6, 15.6 if double else 14.4), (46.6, 11.4, 16.0 if double else 15.2), mat(fr)), box((1.4, 9.2, 13.6 if double else 12.4), (46.6, 9.4, 18.4 if double else 14.4), mat("wire"))]
    sz = (15.8, 16.2) if double else (15.2, 15.6)
    out += [box((0.2, 11.8, sz[0]), (47.8, 19.6, sz[1]), mat("fabric")), box((0.2, 19.6, sz[0] - 0.05), (47.8, 19.8, sz[1] + 0.05), mat("alu"))]
    rng = random.Random(9100 + v * 17 + (0 if double else 7))
    n = v * 8
    for x0 in (0.0, 24.0):
        out += workstation(x0 + 0.05, "A", rng, n, 15.8 if double else 15.2)
        n += 1
        if double:
            out += workstation(x0 + 0.05, "B", rng, n)
            n += 1
    return out


# ------------------------------------------------------------------ the phone booth
def phone_booth():
    """A one-person phone booth (two high: 1 m square, 2 m tall; its glass door to the north): the felt-lined shell in its
    green fabric, the roof with its light (lit in game while someone is in it) and its fan grille, the glass door in its
    aluminium frame with its long handle, the stool, the shelf for a laptop with the socket under it; the PHONE sign
    over the door."""
    b, felt = "booth", "felt"
    out = [box((0.4, 0, 0.6), (15.6, 0.6, 15.6), mat("graphite")),
           box((0.4, 0.6, 1.0), (1.2, 31.2, 15.6), mat(b)), box((14.8, 0.6, 1.0), (15.6, 31.2, 15.6), mat(b)),
           box((1.2, 0.6, 14.8), (14.8, 31.2, 15.6), mat(b)), box((0.4, 31.2, 0.6), (15.6, 32.0, 15.6), mat(b)),
           box((1.2, 0.6, 14.6), (14.8, 31.2, 14.8), mat(felt)), box((1.2, 0.6, 1.0), (1.4, 31.2, 14.6), mat(felt)), box((14.6, 0.6, 1.0), (14.8, 31.2, 14.6), mat(felt)),
           box((1.4, 31.0, 1.0), (14.6, 31.2, 14.6), mat(felt))]
    out += [box((1.2, 0.6, 0.6), (14.8, 1.2, 1.0), mat("alu")), box((1.2, 27.0, 0.6), (14.8, 31.2, 1.0), mat(b)),
            box((1.2, 1.2, 0.6), (1.8, 27.0, 1.0), mat("alu")), box((14.2, 1.2, 0.6), (14.8, 27.0, 1.0), mat("alu")), box((1.8, 26.4, 0.6), (14.2, 27.0, 1.0), mat("alu")),
            box((1.8, 1.2, 0.72), (14.2, 26.4, 0.88), mat("glass")),
            box((12.6, 11.0, 0.2), (12.9, 17.0, 0.6), mat("chrome")), box((12.6, 11.0, 0.6), (12.9, 11.3, 0.72), mat("chrome")), box((12.6, 16.7, 0.6), (12.9, 17.0, 0.72), mat("chrome")),
            box((3.0, 28.0, 0.5), (13.0, 30.2, 0.6), faces(b, north=A.region("ok_booth_sign", 10.0, 2.2, booth_sign)))]
    out += round_part(8.0, 9.0, 1.4, 0.6, 7.2, "graphite", n=4) + round_part(8.0, 9.0, 2.2, 7.2, 8.0, "booth", n=4)
    out += [box((1.4, 11.6, 10.6), (14.6, 12.2, 14.6), mat("oak")), box((3.0, 9.6, 14.2), (4.6, 10.6, 14.6), mat("white")),
            box((3.3, 9.9, 14.15), (3.5, 10.2, 14.2), mat("black")), box((4.0, 9.9, 14.15), (4.2, 10.2, 14.2), mat("black"))]
    out += [{**box((5.0, 30.9, 5.0), (11.0, 31.0, 11.0), mat("light")), "shade": False}, box((6.4, 30.8, 11.6), (9.6, 31.0, 13.6), mat("grille"))]
    return out


# ------------------------------------------------------------------ the recycling station
def recycling_station():
    """Three bins side by side under their sign (a block wide, their fronts to the north): paper (blue), mixed (green)
    and general (black), each with its lid and its slot, the label on its front."""
    out = []
    for k, (c, lab, slot) in enumerate((("bin_black", "GENERAL", "flap"), ("bin_green", "MIXED", "hole"), ("bin_blue", "PAPER", "slot"))):
        x0 = 0.6 + k * 5.0
        out += [box((x0, 0, 4.0), (x0 + 4.6, 10.4, 14.8), mat(c)), box((x0 - 0.1, 10.4, 3.8), (x0 + 4.7, 11.2, 15.0), mat(c)),
                box((x0 + 0.6, 6.0, 3.95), (x0 + 4.0, 7.4, 4.0), faces(c, north=SCR.face(f"ok_bin_{k}", 3.4, 1.4, label_plate(lab, (250, 250, 248)))))]
        if slot == "slot":
            out.append(box((x0 + 0.8, 11.15, 6.0), (x0 + 3.8, 11.25, 6.8), mat("black")))
        elif slot == "hole":
            out += round_part(x0 + 2.3, 7.4, 1.0, 11.15, 11.25, "black", n=3)
        else:
            out.append(box((x0 + 0.6, 11.2, 4.4), (x0 + 4.0, 11.5, 8.4), mat(c), {"angle": 22.5, "axis": "x", "origin": [x0 + 2.3, 11.2, 8.4]}))
    out.append(box((1.0, 13.0, 15.4), (15.0, 15.6, 15.6), faces("white", north=SCR.face("ok_recycle", 14.0, 2.6, PK.header_card("RECYCLE", "PAPER  -  MIXED CANS AND PLASTIC  -  GENERAL", (40, 120, 60))))))
    return out


# ------------------------------------------------------------------ the storage cupboard
def storage_cupboard():
    """A tall stationery cupboard (two high: 90 cm wide, 1.95 m; its front to the north): the white body on its plinth,
    two doors, the right one standing open on its hinges; the shelves inside: reams of copy paper, boxes of
    envelopes, toner, files, a first box of pens, a label maker."""
    w = "ped"
    out = [box((0.8, 0, 3.6), (15.2, 0.8, 15.6), mat("black")), box((0.8, 0.8, 4.0), (1.4, 31.2, 15.8), mat(w)), box((14.6, 0.8, 4.0), (15.2, 31.2, 15.8), mat(w)),
           box((0.8, 31.2, 3.8), (15.2, 31.8, 15.8), mat(w)), box((1.4, 0.8, 15.2), (14.6, 31.2, 15.8), mat(w)), box((1.4, 0.8, 4.2), (14.6, 1.4, 15.2), mat(w))]
    for y in (8.0, 15.0, 22.0):
        out.append(box((1.4, y, 4.4), (14.6, y + 0.4, 15.2), mat(w)))
    out += [box((0.8, 0.8, 3.4), (8.0, 31.2, 4.0), mat(w)), box((6.8, 14.0, 3.0), (7.2, 18.0, 3.4), mat("chrome")),
            box((15.2, 0.8, -3.6), (15.8, 31.2, 3.6), mat(w)), box((15.8, 14.0, -3.0), (16.1, 18.0, -2.6), mat("chrome"))]
    # what is on the shelves (the right half shows through the open door)
    for k in range(5):
        y = 1.4 + k * 1.3
        out.append(box((8.4, y, 6.0), (13.8, y + 1.3, 14.4), faces("ream", north=A.region(f"ok_ream_{k}", 5.4, 1.3, label_plate("A4  COPY PAPER  80G", (60, 110, 170), (255, 255, 255))))))
    out += [box((2.0, 8.4, 6.0), (7.4, 11.8, 14.0), faces("cardboard", north=A.region("ok_env", 5.4, 3.4, label_plate("ENVELOPES C4", (210, 190, 150))))),
            box((8.4, 8.4, 7.0), (11.0, 12.6, 13.0), mat("black")), box((11.4, 8.4, 7.0), (14.0, 12.6, 13.0), mat("black")),
            box((8.4, 12.6, 7.0), (11.0, 13.0, 13.0), mat("holder_blue")), box((11.4, 12.6, 7.0), (14.0, 13.0, 13.0), mat("holder_red"))]
    for j in range(7):
        c = ["bnd_navy", "bnd_red", "bnd_green", "bnd_grey", "bnd_blue", "bnd_black", "bnd_orange"][j]
        out.append(box((8.4 + j * 0.8, 15.4, 8.0), (9.1 + j * 0.8, 20.4, 14.6), mat(c)))
    out += [box((8.6, 22.4, 7.0), (11.4, 23.6, 11.0), faces("ped", north=A.region("ok_pens", 2.8, 1.2, label_plate("PENS x50", (240, 240, 236))))),
            box((11.8, 22.4, 7.6), (13.8, 23.4, 10.6), mat("holder_yellow")), box((2.0, 22.4, 6.0), (7.0, 26.0, 14.0), mat("cardboard"))]
    return out


# ------------------------------------------------------------------ on the walls
def wall_whiteboard():
    """A wall whiteboard (2 x 1, 1.8 by 0.9 m; its face to the north): the aluminium frame, the white board with this
    week's plan written on it, the pen tray along its foot with three markers and the eraser, two magnets holding a
    sheet up."""
    out = [box((0.6, 1.4, 15.0), (31.4, 2.0, 16.0), mat("alu")), box((0.6, 15.6, 15.0), (31.4, 16.2, 16.0), mat("alu")),
           box((0.6, 2.0, 15.0), (1.2, 15.6, 16.0), mat("alu")), box((30.8, 2.0, 15.0), (31.4, 15.6, 16.0), mat("alu")),
           box((1.2, 2.0, 15.4), (30.8, 15.6, 16.0), faces("board", north=PRT.face("ok_board", 29.6, 13.6, board_writing)))]
    out += [box((4.0, 1.0, 14.0), (28.0, 1.4, 15.4), mat("alu")), box((4.0, 1.4, 14.0), (28.0, 1.8, 14.2), mat("alu"))]
    for k, c in enumerate(("navy", "red", "glove_green")):
        x = 8.0 + k * 2.6
        out += [box((x, 1.4, 14.4), (x + 2.0, 1.75, 14.75), mat("white")), box((x + 2.0, 1.4, 14.4), (x + 2.5, 1.75, 14.75), mat(c))]
    out += [box((18.0, 1.4, 14.3), (21.4, 2.2, 15.0), mat("felt")), box((18.0, 2.2, 14.3), (21.4, 2.6, 15.0), mat("black"))]
    out += [box((1.6, 9.0, 15.35), (5.0, 13.8, 15.4), faces("paper", north=SCR.face("ok_board_sheet", 3.4, 4.8, PK.notam_sheet(["PAD 3 PLAN", "", "CLOSED 24 OCT", "0600 - 1800", "USE PAD 1 / 2"]))))]
    out += swap_yz(round_part(3.3, 13.4, 0.4, 15.05, 15.35, "red", n=3))
    return out


def office_noticeboard():
    """The office notice board (2 x 1; its face to the north): cork in an aluminium frame; the fire drill notice, the
    kitchen rota, the first aiders, a birthday card, the social club's poster, a sticky note; push pins."""
    out = [box((0.4, 0.4, 15.0), (31.6, 1.0, 16.0), mat("alu")), box((0.4, 15.0, 15.0), (31.6, 15.6, 16.0), mat("alu")),
           box((0.4, 1.0, 15.0), (1.0, 15.0, 16.0), mat("alu")), box((31.0, 1.0, 15.0), (31.6, 15.0, 16.0), mat("alu")),
           box((1.0, 1.0, 15.2), (31.0, 15.0, 16.0), mat("cork"))]
    sheets = [(26.6, 8.6, 3.4, 4.75, "ok_n_fire", notice("FIRE DRILL", ["THURSDAY 26 OCT, 11:00", "assemble at point B", "(the north car park)", "do not use the lifts"], (190, 40, 40))),
              (22.2, 8.8, 3.4, 4.75, "ok_n_rota", rota),
              (17.8, 8.4, 3.4, 4.75, "ok_n_aid", notice("FIRST AIDERS", ["REYES  ext 214", "LINDQVIST  ext 231", "the room is on", "this floor, SE"], (40, 140, 70))),
              (12.6, 7.0, 4.4, 6.2, "ok_n_social", notice("SOCIAL CLUB", ["QUIZ NIGHT", "FRIDAY 3 NOV", "CREW LOUNGE 18:30", "teams of four", "sign up below:", "______________"], (60, 80, 170))),
              (8.0, 9.4, 2.8, 3.6, "ok_n_card", card),
              (2.4, 7.8, 4.6, 3.4, "ok_n_parking", notice("PARKING", ["bays 1-12 are for the", "night shift from 1 Nov"], (90, 90, 96))),
              (26.4, 2.2, 3.4, 4.75, "ok_n_it", notice("IT", ["password changes", "due by 31 OCT", "ask the help desk", "ext 400"], (30, 30, 34)))]
    rng = random.Random(9310)
    for x0, y0, w, h, key, draw in sheets:
        out.append(box((x0, y0, 15.15), (x0 + w, y0 + h, 15.2), faces("paper", north=SCR.face(key, w, h, draw))))
        for px in (x0 + 0.3, x0 + w - 0.3):
            out += swap_yz(round_part(px, y0 + h - 0.3, 0.18, 14.85, 15.15, rng.choice(("red", "glove_blue", "holder_yellow", "glove_green")), n=3))
    out.append(box((19.0, 3.0, 15.14), (21.4, 5.4, 15.2), faces("paper", north=SCR.face("ok_n_sticky", 2.4, 2.4, PK.sticky("cake in the\nlounge 3pm!", (250, 180, 200))))))
    return out


# ------------------------------------------------------------------ room 8's additions: the year planner, the pigeonholes, the team table
MONTHS = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"]


def planner_print(img):
    """A laminated year planner (2026): a row a month, a column a day, the weekends shaded; in marker and stickers the
    team's leave (blocks of colour), the audits (red dots), the fleet's services (blue), a line for today."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246))
    d.rectangle((0, 0, W, H * 0.1), fill=(30, 60, 120))
    d.text((W * 0.02, H * 0.05), "2026  YEAR PLANNER  -  OPERATIONS SUPPORT", font=_fit(d, "2026  YEAR PLANNER  -  OPERATIONS SUPPORT", FONT, round(H * 0.06), W * 0.7), fill=(255, 255, 255), anchor="lm")
    gx0, gy0, gx1, gy1 = W * 0.06, H * 0.14, W * 0.99, H * 0.97
    cw, rh = (gx1 - gx0) / 31, (gy1 - gy0) / 12
    fs = ImageFont.truetype(FONT, max(7, round(rh * 0.42)))
    rng = random.Random(9500)
    import datetime
    for m in range(12):
        y = gy0 + m * rh
        d.text((W * 0.01, y + rh / 2), MONTHS[m], font=fs, fill=(30, 30, 34), anchor="lm")
        ndays = (datetime.date(2026 + (m == 11), (m + 1) % 12 + 1, 1) - datetime.date(2026, m + 1, 1)).days
        for dd in range(31):
            x = gx0 + dd * cw
            if dd < ndays:
                wd = datetime.date(2026, m + 1, dd + 1).weekday()
                if wd >= 5:
                    d.rectangle((x, y, x + cw, y + rh), fill=(226, 228, 232))
            else:
                d.rectangle((x, y, x + cw, y + rh), fill=(90, 92, 96))
            d.rectangle((x, y, x + cw, y + rh), outline=(190, 192, 196))
        for _ in range(rng.randint(1, 3)):
            a = rng.randint(0, 24)
            ln = rng.randint(2, 6)
            c = rng.choice(((240, 170, 60), (120, 200, 120), (230, 120, 160), (140, 170, 230)))
            d.rectangle((gx0 + a * cw + 1, y + rh * 0.2, gx0 + (a + ln) * cw - 1, y + rh * 0.8), fill=c)
        for _ in range(rng.randint(0, 2)):
            a = rng.randint(0, 27)
            c = rng.choice(((200, 40, 40), (40, 80, 200)))
            cx, cy = gx0 + (a + 0.5) * cw, y + rh / 2
            d.ellipse((cx - cw * 0.3, cy - cw * 0.3, cx + cw * 0.3, cy + cw * 0.3), fill=c)
    tx = gx0 + 22.5 * cw
    d.line((tx, gy0 + 9 * rh, tx, gy0 + 10 * rh), fill=(200, 30, 30), width=max(2, round(cw * 0.15)))
    for dd in range(0, 31, 5):
        d.text((gx0 + (dd + 0.5) * cw, H * 0.12), str(dd + 1), font=ImageFont.truetype(FONT_R, max(7, round(rh * 0.3))), fill=(80, 80, 86), anchor="mm")
    return img


def year_planner():
    """The year planner on the wall (2 x 1; its face to the north): the laminated print in its aluminium frame, two
    sticky notes stuck to it, the marker hanging on its cord from the corner."""
    out = [box((0.6, 1.2, 15.0), (31.4, 1.8, 16.0), mat("alu")), box((0.6, 15.6, 15.0), (31.4, 16.2, 16.0), mat("alu")),
           box((0.6, 1.8, 15.0), (1.2, 15.6, 16.0), mat("alu")), box((30.8, 1.8, 15.0), (31.4, 15.6, 16.0), mat("alu")),
           box((1.2, 1.8, 15.4), (30.8, 15.6, 16.0), faces("board", north=PRT.face("ok_planner", 29.6, 13.8, planner_print)))]
    out += [box((3.0, 3.0, 15.34), (5.4, 5.4, 15.4), faces("paper", north=SCR.face("ok_pl_sticky_0", 2.4, 2.4, PK.sticky("AUDIT\nwk 46!", (250, 236, 110))))),
            box((25.4, 10.6, 15.34), (27.8, 13.0, 15.4), faces("paper", north=SCR.face("ok_pl_sticky_1", 2.4, 2.4, PK.sticky("leave:\nask Reyes", (160, 220, 250)))))]
    out += cable([(1.4, 1.2, 14.9), (1.4, -2.6, 14.9)], 0.04, "black") + [box((1.15, -4.8, 14.75), (1.65, -2.6, 15.05), mat("white")), box((1.15, -2.6, 14.75), (1.65, -2.1, 15.05), mat("navy"))]
    return out


POST_NAMES = ["REYES", "OKAFOR", "SILVA", "BRANDT", "NAKAMURA", "LINDQVIST", "MORROW", "HADDAD", "PETROV", "ADEYEMI", "KOWALSKI", "DUBOIS"]


def mail_sorter():
    """The post pigeonholes (a block wide, 1 m; its front to the north): a low cupboard with two doors, on it the
    pigeonhole unit of twelve slots (four across, three up), a name under each; letters, envelopes, a small parcel and a
    magazine in some of them."""
    lam = "laminate"
    out = [box((0.8, 0, 6.4), (15.2, 0.6, 15.4), mat("black")), box((0.6, 0.6, 6.0), (15.4, 7.2, 15.8), mat(lam)),
           box((0.7, 0.8, 5.7), (7.9, 6.9, 6.0), mat(lam)), box((8.1, 0.8, 5.7), (15.3, 6.9, 6.0), mat(lam)),
           box((7.2, 3.4, 5.4), (7.5, 5.2, 5.7), mat("chrome")), box((8.5, 3.4, 5.4), (8.8, 5.2, 5.7), mat("chrome")),
           box((0.6, 7.2, 6.0), (15.4, 7.6, 15.8), mat(lam)),
           box((0.6, 7.6, 6.0), (1.0, 16.0, 15.8), mat(lam)), box((15.0, 7.6, 6.0), (15.4, 16.0, 15.8), mat(lam)),
           box((1.0, 7.6, 15.4), (15.0, 16.0, 15.8), mat(lam)), box((0.6, 16.0, 6.0), (15.4, 16.4, 15.8), mat(lam))]
    for k in range(1, 4):
        x = 1.0 + k * 3.5
        out.append(box((x - 0.1, 7.6, 6.0), (x + 0.1, 16.0, 15.4), mat(lam)))
    rng = random.Random(9600)
    for r in range(3):
        y0 = 7.6 + r * 2.8
        if r:
            out.append(box((1.0, y0 - 0.5, 6.0), (15.0, y0, 15.4), mat(lam)))
        for c in range(4):
            x0 = 1.0 + c * 3.5
            n = r * 4 + c
            ly0, ly1 = (y0 - 0.45, y0 - 0.05) if r else (7.25, 7.55)
            out.append(box((x0 + 0.5, ly0, 5.95), (x0 + 3.0, ly1, 6.0), faces(lam, north=SCR.face(f"ok_post_{n}", 2.5, ly1 - ly0, label_plate(POST_NAMES[n], (246, 246, 240))))))
            kind = rng.choice(("none", "letters", "letters", "envelope", "parcel", "magazine"))
            if kind == "letters":
                for q in range(rng.randint(1, 3)):
                    out.append(box((x0 + 0.4 + q * 0.1, y0 + q * 0.06, 7.0 + q * 0.3), (x0 + 3.0 + q * 0.1, y0 + 0.06 + q * 0.06, 12.0 + q * 0.3), mat("paper")))
            elif kind == "envelope":
                out.append(box((x0 + 0.3, y0, 7.2), (x0 + 3.1, y0 + 0.1, 14.0), mat("manila")))
            elif kind == "parcel":
                out.append(box((x0 + 0.5, y0, 8.0), (x0 + 2.9, y0 + 1.8, 13.0), mat("cardboard")))
            elif kind == "magazine":
                out.append(box((x0 + 0.3, y0, 7.0), (x0 + 3.1, y0 + 0.25, 12.4), faces("paper", up=A.region(f"ok_mag_{n}", 2.8, 5.4, MK.magazine(("DRONE WORLD", "FLIGHT OPS", "AIRSPACE")[n % 3], ((40, 80, 160), (160, 40, 40), (40, 120, 70))[n % 3])))))
    return out


def team_table():
    """The team table (3 x 1, built along x 0..48: 2.8 by 0.9 m): the oak top on white A-frame legs, the power module in
    the middle with its sockets, the conference phone; two notepads and pens, a laptop open, a jug of water and
    glasses."""
    fr = "frame"
    out = [box((0.8, 11.4, 1.6), (47.2, 12.0, 14.4), mat("oak"))]
    for x in (2.0, 45.2):
        out += [box((x, 0, 2.4), (x + 0.8, 0.6, 13.6), mat(fr)), box((x, 10.6, 2.8), (x + 0.8, 11.4, 13.2), mat(fr)),
                box((x, 0.6, 3.2), (x + 0.8, 10.6, 4.0), mat(fr)), box((x, 0.6, 12.0), (x + 0.8, 10.6, 12.8), mat(fr))]
    out += [box((2.8, 10.6, 7.6), (45.2, 11.4, 8.4), mat(fr)),
            box((20.0, 11.95, 6.2), (28.0, 12.2, 9.8), mat("black")), box((20.6, 12.2, 6.6), (22.0, 12.25, 7.6), mat("white")),
            box((22.6, 12.2, 6.6), (24.0, 12.25, 7.6), mat("white")), box((24.6, 12.2, 6.6), (26.0, 12.25, 7.6), mat("white"))]
    t = 12.0
    out += round_part(24.0, 11.2, 1.6, t, t + 0.4, "graphite", n=4) + round_part(24.0, 11.2, 0.6, t + 0.4, t + 0.6, "black", n=3)
    out += [box((22.6, t + 0.4, 9.4), (25.4, t + 0.45, 10.2), faces("black", up=SCR.face("ok_conf", 2.8, 0.8, label_plate("CALL  00:42", (8, 12, 16), (120, 210, 255), live={"part": "00:42", "src": "timer", "period": 2700}))))]
    for (nx, nz, n) in ((6.0, 3.0, 0), (36.0, 10.0, 1)):
        out += [box((nx, t, nz), (nx + 3.0, t + 0.3, nz + 4.0), faces("paper", up=A.region(f"ok_tnote_{n}", 3.0, 4.0, MK.page_sheet(1200 + n)))),
                box((nx + 3.3, t, nz + 1.0), (nx + 5.7, t + 0.2, nz + 1.2), mat("navy"))]
    out += [box((12.0, t, 9.8), (17.6, t + 0.3, 13.4), faces("alu", up=A.region("ok_tlkbd", 5.6, 3.6, MK.keyboard_top))),
            box((12.0, t + 0.3, 13.1), (17.6, t + 3.9, 13.4), mat("alu"), {"angle": 22.5, "axis": "x", "origin": [14.8, t + 0.3, 13.4]}),
            {**box((12.2, t + 0.5, 13.05), (17.4, t + 3.7, 13.1), faces("black", north=MON.face("ok_tlap", 5.2, 3.2, MK.monitor_screen("12_2"))), {"angle": 22.5, "axis": "x", "origin": [14.8, t + 0.3, 13.4]}), "shade": False}]
    out += round_ring(32.0, 5.0, 0.9, 0.1, t, t + 3.0, "glass", 2.2, "water", n=4)
    for (gx, gz) in ((34.4, 4.0), (35.8, 6.4), (30.0, 3.0)):
        out += MK.round_ring(gx, gz, 0.45, 0.06, t, t + 1.6, "glass", 0.9, "water", n=4)
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, PRT, MON
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **MATERIALS}
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(512, TX, "atlas")
    SCR = PK.ShelfAtlas(2048, 4 * TX, "screens")
    PRT = PK.ShelfAtlas(1024, 2 * TX, "prints")
    MON = PK.ShelfAtlas(2048, 3 * TX, "monitors")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK):
        m.A, m.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("ok_atlas"), "screens": ref("ok_screens"), "prints": ref("ok_prints"), "monitors": ref("ok_monitors"), "particle": ref("ok_frame")})

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

    def tall(name, els, extra="", key=None):
        v = {}
        for h, part in zip(("lower", "upper"), cut_y(prep(els), 2)):
            model(f"{key or name}_{h}", part, True)
            v.update(faced(f"{key or name}_{h}", f",half={h}{extra}"))
        return v

    def wide(name, els, n):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}"))
        return v

    v = {}
    for k in range(6):
        for xi, xpart in enumerate(cut_parts(prep(office_bench(True, k)), 3)):
            for zi, part in enumerate(cut_z(xpart, 2)):
                model(f"office_bench_{k}_{zi * 3 + xi}", part, True)
                v.update(faced(f"office_bench_{k}_{zi * 3 + xi}", f",part={zi * 3 + xi},variant={k}"))
    states("office_bench", v)
    v = {}
    for k in range(4):
        for p, part in enumerate(cut_parts(prep(office_bench(False, k)), 3)):
            model(f"office_bench_single_{k}_{p}", part, True)
            v.update(faced(f"office_bench_single_{k}_{p}", f",part={p},variant={k}"))
    states("office_bench_single", v)
    states("phone_booth", tall("phone_booth", phone_booth()))
    model("recycling_station", recycling_station())
    states("recycling_station", faced("recycling_station"))
    states("storage_cupboard", tall("storage_cupboard", storage_cupboard()))
    states("wall_whiteboard", wide("wall_whiteboard", wall_whiteboard(), 2))
    states("office_noticeboard", wide("office_noticeboard", office_noticeboard(), 2))
    states("year_planner", wide("year_planner", year_planner(), 2))
    model("mail_sorter", mail_sorter())
    states("mail_sorter", faced("mail_sorter"))
    states("team_table", wide("team_table", team_table(), 3))
    A.img.save(tex_dir / "ok_atlas.png")
    SCR.img.save(tex_dir / "ok_screens.png")
    PRT.img.save(tex_dir / "ok_prints.png")
    MON.img.save(tex_dir / "ok_monitors.png")
    return A.y + A.row, SCR.y + SCR.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
