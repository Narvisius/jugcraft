"""The tower's drone engineering lab (work list item 22 part B, room 3), built to rooms 1 and 2's standard (everything
modelled, screens sharp, materials seamless and at one density, things varied):

- lab_workbench (joins up in rows): a steel-framed beech-topped bench, its drawer unit, a vise on some; on it a drone
  frame on its stand half built, a parts tray, a toolbox, a heat gun, a prop balancer, a battery in its LiPo bag.
- lab_pegboard (on the wall over a bench): the board, its hooks and tools (wrenches, screwdrivers, pliers, the hex
  keys, a tape, scissors), the little shelf of cans.
- lab_tool_cabinet: a roll cab, six drawers with their pulls, the top chest and its lid, casters; red or graphite.
- cad_desk (joins up): the design workstation, its two monitors (CAD drawings of the fleet's frames and boards: live
  in game), keyboard, 3D mouse and mouse, a part of a drone being designed, a notebook, a mug.
- task_chair: mesh back, five-star base; swivelled variants.
- electronics_bench (joins up): the ESD mat and the instrument shelf, the soldering station and fume extractor, the
  oscilloscope and bench supply (their traces and figures live in game), the magnifier lamp over a flight controller,
  the parts drawers, a multimeter and its probes.
- bench_stool: round seat, foot ring, five-star base.
- test_cage (a frame of blocks): the flight test cage, its aluminium frame, the netting on its sides and over it, its
  door, the foam floor and landing pad inside.
- printer_farm (two high): a steel rack of four 3D printers, each printing a part of a drone, its spool, its lit strip.
- lipo_cabinet (two high): the fire-safe battery charging cabinet, its vents, its label, its status lights.
- thrust_test_stand: the motor test rig on its table: the load cell beam, the motor and prop, the shield, the logger.
- parts_rack (two high): steel shelving of labelled bins, boxes and props in bags; every shelf its own.
- lab_plotter (2 x 1): the large-format printer, its roll, a frame drawing feeding out.
- plan_chest (2 x 1): the flat file of wide drawers, drawings on top.
- rolling_whiteboard (2 x 1, two high): on castors, a drone sketched on it, its marker tray.
- flight_case: hard cases for the drones, stacked; variants.
"""
from __future__ import annotations

import json
import math
import random
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, ORANGE, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None
SCR = None
MONO = MK.MONO


# ------------------------------------------------------------------ materials
MATERIALS = {
    "lk_steel_blue": lambda: TK.solid((46, 72, 112), 3202, 1.8), "lk_cab_red": lambda: TK.solid((178, 34, 30), 3203, 1.6),
    "lk_cab_grey": lambda: TK.solid((62, 66, 72), 3204, 1.6), "lk_esd": lambda: TK.solid((52, 104, 150), 3205, 2.0),
    "lk_pegboard": lambda: pegboard_tex(), "lk_yellow": lambda: TK.solid((236, 186, 34), 3206, 2.0),
    "lk_foam": lambda: foam(), "lk_net": lambda: net(), "lk_alu": lambda: TK.brushed((186, 190, 196), 3207, 3),
    "lk_carbon": lambda: carbon(), "lk_pla_orange": lambda: TK.solid((232, 120, 36), 3208, 1.5),
    "lk_pla_grey": lambda: TK.solid((150, 154, 160), 3209, 1.5), "lk_pla_black": lambda: TK.solid((36, 36, 38), 3210, 1.2),
    "lk_bin_blue": lambda: TK.solid((40, 90, 170), 3211, 1.5), "lk_bin_red": lambda: TK.solid((190, 46, 40), 3212, 1.5),
    "lk_bin_yellow": lambda: TK.solid((226, 186, 40), 3213, 1.5), "lk_cardboard": lambda: TK.solid((184, 142, 96), 3214, 3.5),
    "lk_bag": lambda: MK.rgba((230, 236, 240), 120, 3215), "lk_shield": lambda: MK.rgba((210, 226, 236), 70, 3216),
    "lk_copper": lambda: TK.brushed((196, 116, 64), 3217, 3), "lk_green_pcb": lambda: TK.solid((30, 100, 60), 3218, 2.0),
    "lk_case_black": lambda: MK.leather((34, 34, 36), 3219, grain=2.6), "lk_case_orange": lambda: MK.leather((216, 112, 36), 3220, grain=2.6),
    "lk_whiteboard": lambda: TK.solid((244, 246, 246), 3221, 0.8), "lk_spool_blue": lambda: TK.solid((40, 110, 200), 3222, 1.5),
    "lk_tape_yellow": lambda: hazard(), "lk_beech": lambda: butcher(), "lk_beech_side": lambda: MK.wood((190, 150, 106), 3245, lines=30, depth=(0.02, 0.06)),
    "lk_beech_end": lambda: beech_end(), "lk_foam_a": lambda: foam_tile((58, 60, 64), 3260), "lk_foam_b": lambda: foam_tile((84, 88, 94), 3262),
    "lk_pad_orange": lambda: foam_tile((222, 96, 32), 3264), "lk_hickory": lambda: MK.wood((196, 150, 96), 3266, lines=24, depth=(0.03, 0.08)),
    "lk_grip_black": lambda: MK.leather((30, 30, 32), 3268, grain=3.0), "lk_amber_off": lambda: TK.solid((70, 60, 40), 3270, 1.5)}


def pegboard_tex():
    """Hardboard pegboard: holes every 16 texels (a pixel), seamless."""
    a = np.stack([np.full((256, 256), c, float) for c in (196, 170, 130)], -1) + TK._noise(3230, 2)[..., None]
    y, x = np.mgrid[0:256, 0:256]
    hole = ((x % 16 - 8) ** 2 + (y % 16 - 8) ** 2) < 5
    a[hole] = (40, 34, 28)
    return TK._img(a)


def foam():
    """Interlocking floor foam: soft green, a faint texture."""
    a = np.stack([np.full((256, 256), c, float) for c in (64, 122, 74)], -1) + TK._noise(3231, 3)[..., None]
    return TK._img(a)


def net():
    """The test cage's netting: black cords on a 2-pixel mesh, the holes clear (cutout)."""
    a = np.zeros((256, 256, 4), np.uint8)
    y, x = np.mgrid[0:256, 0:256]
    cord = ((x % 32) < 3) | ((y % 32) < 3)
    a[cord] = (24, 24, 26, 255)
    return Image.fromarray(a, "RGBA")


def carbon():
    """Carbon fibre plate: a fine twill weave."""
    y, x = np.mgrid[0:256, 0:256]
    t = ((x // 4 + y // 4) % 2) * 10 - 5
    return TK._img(np.stack([np.full((256, 256), c, float) for c in (34, 34, 36)], -1) + t[..., None] + TK._noise(3232, 2)[..., None])


def hazard():
    y, x = np.mgrid[0:256, 0:256]
    s = ((x + y) // 32) % 2 == 0
    return TK._img(np.where(s[..., None], np.array([236, 186, 34], float), np.array([30, 30, 32], float)))


# ------------------------------------------------------------------ pictures
def _fit(d, t, f, s, w):
    return MK._text_fit(d, t, f, s, w)


def cad_screen(kind):
    """A CAD workstation's screen: the program's dark frame, its tool bars, the model drawn in orange lines (a quad's
    frame from above, a hexacopter's arm in section, a flight controller's board layout, the fleet's T4 wing)."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 10, 12))
        b = round(W * 0.025)
        d.rectangle((b, b, W - b, H - round(W * 0.05)), fill=(36, 38, 44))
        d.rectangle((b, b, W - b, b + H * 0.07), fill=(52, 56, 64))
        for k in range(10):
            d.rectangle((b + W * 0.02 + k * W * 0.05, b + H * 0.015, b + W * 0.055 + k * W * 0.05, b + H * 0.055), fill=(92, 98, 110))
        d.rectangle((b, b + H * 0.07, b + W * 0.16, H - round(W * 0.05)), fill=(44, 46, 52))
        for k in range(12):
            d.rectangle((b + W * 0.015, b + H * (0.1 + k * 0.06), b + W * 0.14, b + H * (0.13 + k * 0.06)), fill=(70, 74, 84))
        cx, cy = W * 0.58, H * 0.52
        s = min(W, H) * 0.3
        col, thin = (238, 140, 40), (150, 160, 176)
        lw = max(1, round(W / 300))
        if kind == "quad":
            for a in (45, 135, 225, 315):
                r = math.radians(a)
                ex, ey = cx + math.cos(r) * s * 1.2, cy + math.sin(r) * s * 1.2
                d.line((cx, cy, ex, ey), fill=col, width=lw * 3)
                d.ellipse((ex - s * 0.45, ey - s * 0.45, ex + s * 0.45, ey + s * 0.45), outline=thin, width=lw)
                d.ellipse((ex - s * 0.12, ey - s * 0.12, ex + s * 0.12, ey + s * 0.12), outline=col, width=lw)
            d.rectangle((cx - s * 0.35, cy - s * 0.5, cx + s * 0.35, cy + s * 0.5), outline=col, width=lw * 2)
        elif kind == "arm":
            d.polygon([(cx - s * 1.6, cy - s * 0.2), (cx + s * 1.4, cy - s * 0.35), (cx + s * 1.6, cy), (cx + s * 1.4, cy + s * 0.35), (cx - s * 1.6, cy + s * 0.2)], outline=col)
            for k in range(6):
                x = cx - s * 1.2 + k * s * 0.5
                d.ellipse((x - s * 0.08, cy - s * 0.08, x + s * 0.08, cy + s * 0.08), outline=thin, width=lw)
            d.line((cx - s * 1.6, cy + s * 0.6, cx + s * 1.6, cy + s * 0.6), fill=thin, width=lw)
        elif kind == "board":
            d.rectangle((cx - s, cy - s, cx + s, cy + s), outline=(80, 200, 120), width=lw * 2)
            rng = random.Random(9)
            for k in range(40):
                x0, y0 = cx + rng.uniform(-s, s), cy + rng.uniform(-s, s)
                d.line((x0, y0, x0 + rng.uniform(-s, s) * 0.5, y0), fill=(200, 160, 60), width=lw)
            d.rectangle((cx - s * 0.3, cy - s * 0.3, cx + s * 0.3, cy + s * 0.3), outline=col, width=lw * 2)
        else:
            d.polygon([(cx - s * 1.8, cy + s * 0.6), (cx, cy - s * 0.8), (cx + s * 1.8, cy + s * 0.6), (cx, cy + s * 0.2)], outline=col)
            d.line((cx, cy - s * 0.8, cx, cy + s * 0.2), fill=thin, width=lw)
        f = ImageFont.truetype(MONO, max(6, round(H * 0.04)))
        d.text((b + W * 0.18, H - round(W * 0.05) - H * 0.06), {"quad": "T1_FRAME_REV7", "arm": "T2_ARM_SECTION", "board": "FC_V4_LAYOUT",
                                                                "wing": "T4_WING_OUTLINE"}[kind] + ".CAD", font=f, fill=(150, 160, 176))
        return img
    return draw


def scope_screen(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(8, 12, 10))
    for k in range(1, 10):
        d.line((W * k / 10, 0, W * k / 10, H), fill=(26, 40, 32))
    for k in range(1, 8):
        d.line((0, H * k / 8, W, H * k / 8), fill=(26, 40, 32))
    pts = [(x, H * 0.38 + H * 0.18 * math.sin(x / W * math.tau * 3)) for x in range(0, W, 2)]
    d.line(pts, fill=(240, 220, 60), width=max(1, round(W / 200)))
    pts = [(x, H * 0.72 + (H * 0.1 if (x // (W / 12)) % 2 else -H * 0.1)) for x in range(0, W, 2)]
    d.line(pts, fill=(60, 200, 240), width=max(1, round(W / 200)))
    # live: the two channels, each on its half of the graticule (the signal's noise on them)
    thick = round(max(1, round(W / 200)) / (H / 2), 4)
    grid = dict(bg=[8, 12, 10], grid=[26, 40, 32], gridn=10, thick=thick)
    TK.live(img, "wave", (0, 0, W, H / 2), shape="sine", fg=[240, 220, 60], cycles=3, mid=0.76, amp=0.72, jitter=0.03, **grid)
    TK.live(img, "wave", (0, H / 2, W, H), shape="square", fg=[60, 200, 240], cycles=6, mid=0.44, amp=-0.4, jitter=0.04, **grid)
    return img


def psu_screen(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(10, 6, 6))
    f = ImageFont.truetype(MONO, round(H * 0.42))
    # live: the set voltage, steady; the current the load draws
    TK.live_part(img, (W * 0.05, H * 0.08), "16.8V", "16.8V", f, (255, 70, 40), "la", bg=(10, 6, 6), src="value", base=16.8, spread=0.04, dp=1, period=30, unit="V")
    TK.live_part(img, (W * 0.55, H * 0.08), "2.40A", "2.40A", f, (80, 230, 110), "la", bg=(10, 6, 6), src="value", base=2.4, spread=0.22, dp=2, period=5, unit="A")
    return img


def label(text, bg=(244, 242, 232), fg=(30, 30, 34), stripe=None, live=None):
    """A plate of text; live ({"part": the part of a one-line text that changes, the whole if none, and its readout's
    params}) marks that part live (an appliance's clock, a meter's figure: training_kit.live_text)."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=bg)
        if stripe:
            d.rectangle((0, 0, W, H * 0.22), fill=stripe)
        lines = text.split("\n")
        y = H * (0.6 if stripe and len(lines) == 1 else 0.5) - (len(lines) - 1) * H * 0.18
        for ln in lines:
            f = _fit(d, ln, FONT, max(6, round(H * 0.4 / len(lines) * 1.4)), W - 4)
            if live and len(lines) == 1:
                params = dict(live)
                TK.live_part(img, (W / 2, y), ln, params.pop("part", ln), f, fg, "mm", bg=bg, **params)
            else:
                d.text((W / 2, y), ln, font=f, fill=fg, anchor="mm")
            y += H * 0.36
        return img
    return draw


def warn_label(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(246, 230, 70))
    d.rectangle((3, 3, W - 4, H - 4), outline=(20, 20, 20), width=3)
    tri = [(W * 0.5, H * 0.08), (W * 0.62, H * 0.32), (W * 0.38, H * 0.32)]
    d.polygon(tri, fill=(20, 20, 20))
    for t, y, s in (("LiPo CHARGING", 0.46, 12), ("KEEP DOORS SHUT", 0.66, 8), ("NO BATTERY UNATTENDED", 0.82, 7)):
        d.text((W / 2, H * y), t, font=_fit(d, t, FONT, s, W - 10), fill=(20, 20, 20), anchor="mm")
    return img


def blueprint(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(246, 246, 242))
    d.rectangle((4, 4, W - 5, H - 5), outline=(40, 60, 120), width=2)
    cx, cy, s = W * 0.45, H * 0.5, min(W, H) * 0.28
    for a in (45, 135, 225, 315):
        r = math.radians(a)
        d.line((cx, cy, cx + math.cos(r) * s, cy + math.sin(r) * s), fill=(40, 60, 120), width=2)
        d.ellipse((cx + math.cos(r) * s - s * 0.3, cy + math.sin(r) * s - s * 0.3, cx + math.cos(r) * s + s * 0.3, cy + math.sin(r) * s + s * 0.3), outline=(40, 60, 120))
    d.rectangle((W * 0.75, H * 0.72, W - 8, H - 8), outline=(40, 60, 120))
    for k in range(3):
        d.line((W * 0.77, H * (0.77 + k * 0.06), W - 12, H * (0.77 + k * 0.06)), fill=(40, 60, 120))
    return img


def sketch(img):
    """The whiteboard: a drone sketched in marker, its arms measured, notes beside it."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(244, 246, 246))
    k = W / 28.0
    blue, red, black = (30, 60, 170), (190, 40, 36), (30, 30, 34)
    cx, cy = W * 0.32, H * 0.5
    for a in (45, 135, 225, 315):
        r = math.radians(a)
        ex, ey = cx + math.cos(r) * 6 * k, cy + math.sin(r) * 6 * k
        d.line((cx, cy, ex, ey), fill=black, width=round(0.35 * k))
        d.ellipse((ex - 2.4 * k, ey - 2.4 * k, ex + 2.4 * k, ey + 2.4 * k), outline=blue, width=round(0.25 * k))
    d.rectangle((cx - 1.6 * k, cy - 2.2 * k, cx + 1.6 * k, cy + 2.2 * k), outline=black, width=round(0.3 * k))
    d.line((cx - 6 * k, cy + 8 * k, cx + 6 * k, cy + 8 * k), fill=red, width=round(0.2 * k))
    f = ImageFont.truetype(FONT, round(1.1 * k))
    d.text((cx, cy + 9.2 * k), "450 mm", font=f, fill=red, anchor="mm")
    for n, t in enumerate(("T1 REV 8", "- arms +12 mm", "- 7 in props", "- battery 6S", "- test cage FRI")):
        d.text((W * 0.62, H * 0.18 + n * 2.2 * k), t, font=f, fill=blue if n else red)
    return img


def thrust_graph(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(14, 16, 20))
    d.rectangle((W * 0.05, H * 0.08, W * 0.95, H * 0.85), outline=(60, 66, 76))
    pts = [(W * 0.05 + t * W * 0.9, H * 0.85 - (t ** 1.6) * H * 0.7) for t in [i / 40 for i in range(41)]]
    d.line(pts, fill=(238, 140, 40), width=max(1, round(W / 150)))
    TK.live_part(img, (W * 0.08, H * 0.12), "THRUST 2.31 kg", "2.31", ImageFont.truetype(MONO, round(H * 0.09)), (200, 210, 220), "la",
                 bg=(14, 16, 20), src="value", base=2.31, spread=0.1, dp=2, period=4)
    return img


def printer_screen(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(14, 22, 34))
    f = ImageFont.truetype(MONO, round(H * 0.3))
    d.text((W * 0.06, H * 0.1), "PRINTING 64%", font=f, fill=(180, 220, 255))
    d.rectangle((W * 0.06, H * 0.62, W * 0.94, H * 0.8), outline=(80, 120, 160))
    d.rectangle((W * 0.06, H * 0.62, W * 0.62, H * 0.8), fill=(80, 170, 240))
    # live: the print getting on (a print of an hour and a half, then the next)
    tb = d.textbbox((W * 0.06, H * 0.1), "PRINTING 64%", font=f)
    TK.live(img, "progress", (0, 0, W, H), bg=[14, 22, 34], fg=[180, 220, 255], on=[80, 170, 240], off=[80, 120, 160], label="PRINTING ",
            text=[0.06, round((tb[1] + tb[3]) / 2 / H, 4), round((tb[3] - tb[1]) / H, 4)], bar=[0.06, 0.62, 0.94, 0.8],
            base=0, rate=round(100 / 5400, 6), cycle=5400, min=0, max=100)
    return img


# ------------------------------------------------------------------ small things
def drone_frame(cx, cz, y0, s=1.0, built=0.6):
    """A drone frame on the bench, part built: the carbon plates, the arms, the motors on some, the flight stack."""
    out = [box((cx - 1.6 * s, y0, cz - 1.2 * s), (cx + 1.6 * s, y0 + 0.25, cz + 1.2 * s), mat("carbon")),
           box((cx - 1.0 * s, y0 + 0.25, cz - 0.8 * s), (cx + 1.0 * s, y0 + 0.9, cz + 0.8 * s), mat("green_pcb")),
           box((cx - 1.6 * s, y0 + 1.0, cz - 1.2 * s), (cx + 1.6 * s, y0 + 1.2, cz + 1.2 * s), mat("carbon"))]
    for k in range(4):
        out.append(box((cx - 0.15 + (k % 2) * 2.6 * s - 1.3 * s, y0 + 0.25, cz - 0.15 + (k // 2) * 2.0 * s - 1.0 * s),
                       (cx + 0.15 + (k % 2) * 2.6 * s - 1.3 * s, y0 + 1.0, cz + 0.15 + (k // 2) * 2.0 * s - 1.0 * s), mat("alu")))
    for k, ang in enumerate((45, -45)):
        out.append(box((cx - 4.2 * s, y0 + 0.05, cz - 0.4 * s), (cx + 4.2 * s, y0 + 0.25, cz + 0.4 * s), mat("carbon"),
                       {"angle": ang, "axis": "y", "origin": [cx, y0, cz]}))
    for k, (dx, dz) in enumerate(((2.97, 2.97), (-2.97, 2.97), (2.97, -2.97), (-2.97, -2.97))):
        if k / 4 < built:
            out += round_part(cx + dx * s, cz + dz * s, 0.55 * s, y0 + 0.25, y0 + 1.0, "black")
    return out


def tray(x0, z0, w, d, y0, tex="black", contents="screws"):
    out = [box((x0, y0, z0), (x0 + w, y0 + 0.15, z0 + d), mat(tex)),
           box((x0, y0 + 0.15, z0), (x0 + w, y0 + 0.6, z0 + 0.15), mat(tex)), box((x0, y0 + 0.15, z0 + d - 0.15), (x0 + w, y0 + 0.6, z0 + d), mat(tex)),
           box((x0, y0 + 0.15, z0 + 0.15), (x0 + 0.15, y0 + 0.6, z0 + d - 0.15), mat(tex)), box((x0 + w - 0.15, y0 + 0.15, z0 + 0.15), (x0 + w, y0 + 0.6, z0 + d - 0.15), mat(tex))]
    rng = random.Random(zlib.crc32(f"{x0}{z0}{contents}".encode()))
    for k in range(int(w * d * 0.8)):
        x, z = x0 + 0.3 + rng.uniform(0, w - 0.6), z0 + 0.3 + rng.uniform(0, d - 0.6)
        out.append(box((x - 0.1, y0 + 0.15, z - 0.1), (x + 0.1, y0 + 0.3, z + 0.1), mat("chrome" if contents == "screws" else rng.choice(("red", "orange", "black")))))
    return out


def laptop_at(x0, z0, y0, page_key, draw, w=6.0, d=4.0, lid=3.8, kind="graphite"):
    """A laptop open toward its user (as training_kit's), its screen a picture of our own."""
    body = {"silver": "alu", "graphite": "graphite", "black": "black"}[kind]
    out = [box((x0, y0, z0), (x0 + w, y0 + 0.35, z0 + d), faces(body, up=A.region(f"lk_deck_{kind}_{w}", w, d, TK.deck(kind))))]
    out.append(box((x0 + 0.1, y0 + 0.35, z0), (x0 + w - 0.1, y0 + 0.35 + lid, z0 + 0.3),
                   faces(body, south=SCR.face(page_key, w - 0.2, lid, draw), north=A.region(f"lk_lid_{kind}_{w}", w - 0.2, lid, TK.lid_back(kind))),
                   {"angle": -22.5, "axis": "x", "origin": [x0 + w / 2, y0 + 0.35, z0 + 0.3]}))
    return out


# ------------------------------------------------------------------ the workbench and pegboard
# ------------------------------------------------------------------ the tool cabinet
def tool_cabinet(colour):
    """The roll cab, its user looking north: the body on its casters, six drawers and their bar pulls, the top chest with
    its lid shut; red or graphite."""
    c = {"red": "cab_red", "grey": "cab_grey"}[colour]
    out = [box((1.0, 1.2, 2.0), (15.0, 10.6, 14.4), mat(c)),
           box((1.0, 10.6, 2.0), (15.0, 11.0, 14.6), mat("black")),
           box((1.2, 11.0, 2.4), (14.8, 14.6, 14.0), mat(c)), box((1.0, 14.6, 2.2), (15.0, 15.4, 14.2), mat(c))]
    for k, (y0, y1) in enumerate(((9.0, 10.4), (7.6, 8.8), (6.2, 7.4), (4.8, 6.0), (3.2, 4.6), (1.4, 3.0))):
        out += [box((1.3, y0, 14.4), (14.7, y1, 14.8), mat(c)), box((2.2, y0 + 0.35, 14.8), (13.8, y0 + 0.75, 15.2), mat("chrome"))]
    for x0 in (1.4, 13.0):
        for z0 in (2.4, 12.4):
            out += [box((x0, 0.9, z0), (x0 + 1.6, 1.2, z0 + 1.6), mat("chrome")), box((x0 + 0.4, 0, z0 + 0.3), (x0 + 1.2, 0.9, z0 + 1.3), mat("black"))]
    out += [box((1.3, 12.4, 14.0), (14.7, 14.2, 14.3), mat(c)), box((6.0, 13.0, 14.3), (10.0, 13.4, 14.6), mat("chrome"))]
    # the push handle on its side on two brackets, the lock, the drawers' label strips, a rubber mat and tools on top
    out += [box((15.0, 4.6, 4.0), (16.0, 5.0, 4.4), mat("chrome")), box((15.0, 4.6, 11.6), (16.0, 5.0, 12.0), mat("chrome")),
            box((15.6, 5.0, 4.0), (16.0, 9.8, 4.4), mat("chrome")), box((15.6, 5.0, 11.6), (16.0, 9.8, 12.0), mat("chrome")),
            box((15.6, 9.4, 4.4), (16.0, 9.8, 11.6), mat("chrome")),
            box((13.6, 10.5, 14.8), (14.2, 11.1, 15.0), mat("chrome"))]
    for y0 in (9.0, 7.6, 6.2, 4.8, 3.2, 1.4):
        out.append(box((10.6, y0 + 0.85, 14.8), (12.6, y0 + 1.15, 14.86), mat("white")))
    out += [box((1.4, 15.4, 2.4), (14.6, 15.5, 14.0), mat("grip_black"))]
    return out


# ------------------------------------------------------------------ the CAD desk
DESK_TOP = 12.6
CADS = [("quad", "arm"), ("board", "quad"), ("wing", "board"), ("arm", "wing")]


def cad_body():
    t = DESK_TOP
    return [box((0, t - 1.0, 0.8), (16, t, 15.2), {**mat("oak"), "down": {"texture": "#steel"}}),
            box((0, 3.6, 1.6), (16, t - 1.0, 2.1), mat("perf", skip=("up",)))]


def cad_end(x):
    return TK.desk_end(x)


def cad_top(v):
    """A designer's place (v): the two monitors on their arm (CAD drawings), keyboard, the 3D mouse, a mouse, a part of
    a drone (an arm, a motor, a prop), a notebook, a mug; or between the places, a printed part and the drawings."""
    t = DESK_TOP
    out = []
    if v < len(CADS):
        a, b = CADS[v]
        out += [box((6.8, t, 1.2), (9.2, t + 0.4, 3.0), mat("graphite")), box((7.7, t + 0.4, 1.8), (8.3, t + 6.0, 2.4), mat("graphite")),
                box((1.0, t + 5.2, 1.92), (15.0, t + 5.8, 2.28), mat("graphite"))]
        for x0, kind in ((0.6, a), (8.1, b)):
            out += [box((x0 + 2.2, t + 3.0, 2.4), (x0 + 5.1, t + 6.4, 3.0), mat("graphite")),
                    {**box((x0, t + 2.6, 3.0), (x0 + 7.3, t + 7.2, 3.4), faces("black", south=SCR.face(f"lk_cad_{kind}", 7.3, 4.6, cad_screen(kind)))), "shade": False}]
        out += [box((3.6, t, 9.0), (11.6, t + 0.4, 11.6), faces("black", up=A.region("lk_kbd", 8.0, 2.6, MK.keyboard_top))),
                box((1.0, t, 9.4), (2.8, t + 0.6, 11.2), mat("graphite")), box((1.3, t + 0.6, 9.7), (2.5, t + 1.3, 10.9), mat("chrome")),
                box((12.6, t, 9.6), (13.8, t + 0.5, 11.4), mat("black"))]
        if v == 0:
            out.append(box((10.0, t, 5.0), (14.4, t + 0.3, 5.8), mat("carbon"), {"angle": 22.5, "axis": "y", "origin": [12.2, t, 5.4]}))
        elif v == 1:
            out += round_part(12.6, 5.6, 1.0, t, t + 1.2, "black") + round_part(12.6, 5.6, 0.3, t + 1.2, t + 1.8, "chrome")
        elif v == 2:
            out.append(box((9.4, t, 5.2), (15.2, t + 0.12, 5.8), mat("pla_black"), {"angle": -22.5, "axis": "y", "origin": [12.3, t, 5.5]}))
        else:
            out += MK.round_ring(13.4, 5.4, 1.0, 0.18, t, t + 2.2, "pla_orange", 1.7, "coffee", n=5)
        out += [box((1.0, t, 12.6), (4.6, t + 0.3, 15.0), faces("paper", up=A.region(f"lk_note_{v}", 3.6, 2.4, TK.paper(500 + v))))]
    elif v == len(CADS):
        out += [box((2.0, t, 3.0), (7.0, t + 0.3, 9.0), faces("paper", up=A.region("lk_print_0", 5.0, 6.0, blueprint)),
                    {"angle": 22.5, "axis": "y", "origin": [4.5, t, 6]}),
                box((9.0, t, 4.0), (12.0, t + 1.6, 6.4), mat("pla_orange")), box((9.4, t, 7.0), (13.6, t + 0.8, 8.0), mat("pla_grey"))]
    return out


# ------------------------------------------------------------------ the task chair and the stool
def task_chair(swivel=0.0):
    """A mesh task chair, its sitter looking north: the five-star base on casters (as the executive chair's), the gas
    lift, the padded seat, the mesh back in its frame, the arms; swivelled above its base."""
    base = MK.exec_chair(0.0)
    base = base[:11]                                                      # (the hub, five legs and their casters)
    top = [box((7.2, 2.0, 7.2), (8.8, 3.6, 8.8), mat("black")), box((7.5, 3.6, 7.5), (8.5, 6.0, 8.5), mat("chrome")),
           box((5.0, 6.0, 5.0), (11.0, 6.6, 11.0), mat("black")),
           box((3.0, 6.6, 2.2), (13.0, 8.2, 12.4), {**mat("fabric"), "down": {"texture": "#black"}}),
           box((7.2, 6.6, 12.4), (8.8, 9.6, 13.6), mat("black")),
           box((3.0, 9.6, 12.6), (3.6, 19.0, 13.4), mat("black")), box((12.4, 9.6, 12.6), (13.0, 19.0, 13.4), mat("black")),
           box((3.6, 9.6, 12.6), (12.4, 10.2, 13.4), mat("black")), box((3.6, 18.4, 12.6), (12.4, 19.0, 13.4), mat("black")),
           box((3.6, 10.2, 12.8), (12.4, 18.4, 13.2), {"north": {"texture": "#mesh"}, "south": {"texture": "#mesh"}})]
    for x0 in (2.2, 13.2):
        top += [box((x0, 6.6, 7.4), (x0 + 0.6, 10.6, 8.2), mat("black")), box((x0 - 0.4, 10.6, 5.2), (x0 + 1.0, 11.2, 10.4), mat("black"))]
    if swivel:
        for e in top:
            e["rotation"] = {"angle": swivel, "axis": "y", "origin": [8, 0, 8]}
    return base + top


def bench_stool():
    """A lab stool: the five-star base on glides, the tall gas lift, the foot ring, the round padded seat."""
    base = MK.exec_chair(0.0)[:11]
    out = base + [box((7.2, 2.0, 7.2), (8.8, 4.0, 8.8), mat("black")), box((7.5, 4.0, 7.5), (8.5, 11.0, 8.5), mat("chrome"))]
    # (the seat 41 cm across, the foot ring 47: a drafting stool's)
    ring = round_ring(8, 8, 3.8, 0.4, 6.0, 6.4, "chrome", 0.0, "chrome", n=6)
    out += [e for e in ring if e["to"][1] - e["from"][1] > 0.1]
    out += [box((4.6, 6.0, 7.8), (11.4, 6.4, 8.2), mat("chrome")), box((7.8, 6.0, 4.6), (8.2, 6.4, 7.8), mat("chrome")), box((7.8, 6.0, 8.2), (8.2, 6.4, 11.4), mat("chrome"))]
    out += round_part(8, 8, 1.8, 11.0, 11.6, "black") + round_part(8, 8, 3.3, 11.6, 13.4, "fabric", n=5)
    return out


# ------------------------------------------------------------------ the electronics bench
EB_TOP = 12.6


def eb_body():
    t = EB_TOP
    return [box((0, t - 1.0, 0.8), (16, t, 15.2), {**mat("cab_grey"), "down": {"texture": "#steel"}}),
            box((0, t, 2.0), (16, t + 0.1, 15.0), mat("esd")),
            box((0, 3.6, 1.6), (16, t - 1.0, 2.1), mat("perf", skip=("up",))),
            # the instrument shelf at the back on its uprights
            box((0, t + 6.4, 0.8), (16, t + 7.0, 6.2), mat("cab_grey")),
            box((0.2, t + 0.1, 0.9), (0.8, t + 6.4, 1.5), mat("steel")), box((15.2, t + 0.1, 0.9), (15.8, t + 6.4, 1.5), mat("steel"))]


def knob_s(cx, cy, r, z0):
    return swap_yz(round_part(cx, cy, r, z0, z0 + 0.3, "black", n=3))


EBS = 4


# ------------------------------------------------------------------ the test cage
def parked_drone(cx, cz, y0, s=2.6):
    """A T1 parked on the cage's pad, full size (the shelf model's shape at s times its size, standing on its skids)."""
    out = []
    for e in MK.model_drone(0, 0, 0)[3:]:
        (x0, a0, z0), (x1, a1, z1) = e["from"], e["to"]
        b = 1.75
        q = json.loads(json.dumps(e))
        q["from"] = [round(cx + x0 * s, 4), round(y0 + (a0 - b + 0.55) * s, 4), round(cz + z0 * s, 4)]
        q["to"] = [round(cx + x1 * s, 4), round(y0 + (a1 - b + 0.55) * s, 4), round(cz + z1 * s, 4)]
        if "rotation" in q:
            o = q["rotation"]["origin"]
            q["rotation"]["origin"] = [round(cx + o[0] * s, 4), round(y0 + (o[1] - b + 0.55) * s, 4), round(cz + o[2] * s, 4)]
        out.append(q)
    return out


# ------------------------------------------------------------------ the 3D printer farm
# ------------------------------------------------------------------ the LiPo cabinet
def lipo_cabinet(half):
    """The fire-safe battery charging cabinet (two high), its user looking north: yellow steel, its two doors (their
    vents, the long handles), the warning label, the status lights, the vent stack on its top."""
    y = "yellow"
    if half == "lower":
        out = [box((1.0, 0, 2.0), (15.0, 1.0, 14.0), mat("black")), box((0.6, 1.0, 1.2), (15.4, 16, 14.6), mat(y)),
               box((0.8, 1.4, 14.6), (7.9, 16, 15.0), mat(y)), box((8.1, 1.4, 14.6), (15.2, 16, 15.0), mat(y))]
        for k in range(5):
            out += [box((1.6, 2.4 + k * 0.8, 15.0), (7.0, 2.8 + k * 0.8, 15.15), mat("black")), box((9.0, 2.4 + k * 0.8, 15.0), (14.4, 2.8 + k * 0.8, 15.15), mat("black"))]
        out += [box((7.2, 8.0, 15.0), (7.6, 16, 15.5), mat("chrome")), box((8.4, 8.0, 15.0), (8.8, 16, 15.5), mat("chrome"))]
        return out
    out = [box((0.6, 0, 1.2), (15.4, 12.0, 14.6), mat(y)),
           box((0.8, 0, 14.6), (7.9, 11.6, 15.0), mat(y)), box((8.1, 0, 14.6), (15.2, 11.6, 15.0), mat(y)),
           box((0.4, 12.0, 1.0), (15.6, 12.8, 14.8), mat(y)),
           box((7.2, 0, 15.0), (7.6, 4.0, 15.5), mat("chrome")), box((8.4, 0, 15.0), (8.8, 4.0, 15.5), mat("chrome")),
           box((1.6, 4.0, 15.0), (7.0, 10.4, 15.1), faces("yellow", south=A.region("lk_warn", 5.4, 6.4, warn_label)))]
    for k, c in enumerate(("green", "green", "amber", "green")):
        out.append({**box((9.4 + k * 1.2, 8.4, 15.0), (10.0 + k * 1.2, 9.0, 15.15), mat(c)), "shade": False})
    out += round_part(8, 8, 1.4, 12.8, 15.0, "steel") + round_part(8, 8, 1.8, 15.0, 15.6, "steel")
    return out


# ------------------------------------------------------------------ the thrust test stand
def thrust_stand():
    """The motor thrust test rig on its steel table, its user looking north: the table, the rig's base plate, the load
    cell beam, the motor and a prop, the polycarbonate shield round it, the logger laptop showing the thrust curve."""
    t = 12.0
    out = [box((0.6, t - 0.8, 1.0), (15.4, t, 15.0), mat("steel"))]
    for x0 in (1.0, 14.2):
        for z0 in (1.4, 13.6):
            out.append(box((x0, 0, z0), (x0 + 0.8, t - 0.8, z0 + 0.8), mat("steel")))
    out += [box((2.0, t, 2.0), (10.0, t + 0.4, 9.0), mat("alu")), box((3.0, t + 0.4, 5.0), (5.0, t + 1.6, 6.0), mat("alu")),
            box((4.4, t + 1.0, 5.1), (9.0, t + 1.6, 5.9), mat("steel_blue")), box((5.2, t + 1.6, 5.2), (5.8, t + 1.8, 5.8), mat("bin_yellow"))]
    out += round_part(8.0, 5.5, 0.9, t + 1.6, t + 2.8, "black") + round_part(8.0, 5.5, 0.25, t + 2.8, t + 3.3, "chrome")
    out += [box((4.5, t + 3.0, 5.35), (11.5, t + 3.1, 5.65), mat("pla_black"), {"angle": 22.5, "axis": "y", "origin": [8, t + 3, 5.5]})]
    out += [box((1.6, t, 1.4), (10.4, t + 7.0, 1.6), mat("shield")), box((1.6, t, 9.4), (10.4, t + 7.0, 9.6), mat("shield")),
            box((1.4, t, 1.6), (1.6, t + 7.0, 9.4), mat("shield")), box((10.4, t, 1.6), (10.6, t + 7.0, 9.4), mat("shield"))]
    out += laptop_at(9.0, 10.4, t, "lk_thrust", thrust_graph, w=6.0, d=4.0, lid=3.8, kind="black")
    return out


# ------------------------------------------------------------------ the parts rack
def parts_rack(half, v):
    """Steel shelving (two high), its user looking north: the uprights, four shelves, the labelled bins, boxes, props in
    their bags, motors in their boxes; v picks the shelves."""
    rng = random.Random(3400 + v * 7 + (half == "upper"))
    out = [box((0.2, 0, 1.0), (1.0, 16, 1.8), mat("steel")), box((15.0, 0, 1.0), (15.8, 16, 1.8), mat("steel")),
           box((0.2, 0, 14.0), (1.0, 16, 14.8), mat("steel")), box((15.0, 0, 14.0), (15.8, 16, 14.8), mat("steel"))]
    levels = (0.8, 8.6) if half == "lower" else (0.4, 8.2, 15.4)
    for k, y in enumerate(levels):
        out.append(box((1.0, y, 1.0), (15.0, y + 0.5, 14.8), mat("steel")))
        if y > 15:
            break
        y0 = y + 0.5
        room = (levels[k + 1] if k + 1 < len(levels) else 16.0) - y0 - 0.3
        x = 1.3
        while x < 14.4:
            kind = rng.choice(("bins", "bins", "box", "props", "motors"))
            if kind == "bins" and x < 11.0:
                c = rng.choice(("bin_blue", "bin_red", "bin_yellow"))
                for j in range(3):
                    if x + 3.0 > 14.8:
                        break
                    h = min(room, 4.4)
                    lab = rng.choice(("M3 SCREWS", "M2 NUTS", "ESC 40A", "MOTOR 2306", "PROP 7IN", "XT60", "CABLE TIES", "STANDOFFS", "GPS", "RX UNITS"))
                    out += [box((x, y0, 3.0), (x + 3.0, y0 + h, 14.0), {**mat(c), "up": {"texture": "#black"}}),
                            box((x + 0.2, y0 + h * 0.45, 14.0), (x + 2.8, y0 + h * 0.75, 14.15), faces("white", south=A.region(f"lk_bin_{lab}", 2.6, round(h * 0.3, 2), label(lab))))]
                    x += 3.1
            elif kind == "box" and x < 10.4:
                w = rng.uniform(3.6, 4.4)
                h = min(room, rng.uniform(2.8, 4.6))
                out += [box((x, y0, 2.4), (x + w, y0 + h, 13.6), mat("cardboard")), box((x + 0.6, y0 + h * 0.5, 13.6), (x + 2.6, y0 + h * 0.8, 13.7), mat("white"))]
                x += w + 0.3
            elif kind == "props" and x < 11.6:
                for j in range(2):
                    out.append(box((x + j * 1.4, y0, 4.0), (x + j * 1.4 + 1.2, y0 + min(room, 5.0), 13.0), {**mat("bag"), "south": {"texture": "#bag"}}))
                    out.append(box((x + j * 1.4 + 0.4, y0 + 0.2, 6.0), (x + j * 1.4 + 0.8, y0 + min(room, 5.0) - 0.4, 11.0), mat("pla_black")))
                x += 3.0
            elif kind == "motors" and x < 11.4:
                for j in range(3):
                    out += [box((x + j * 1.1, y0, 6.0), (x + j * 1.1 + 1.0, y0 + 1.0, 12.0), mat("white")),
                            box((x + j * 1.1, y0 + 1.0, 6.0), (x + j * 1.1 + 1.0, y0 + 2.0, 12.0), mat("black"))]
                x += 3.6
            else:
                x += 1.0
    return out


RACKS = 4


# ------------------------------------------------------------------ the plotter, the plan chest, the whiteboard, the cases
def plotter():
    """Built whole along x 0..32, its user looking north: the large-format printer on its stand, its roll, its panel, a
    drawing feeding out the front into the basket."""
    out = [box((1.0, 9.0, 4.0), (31.0, 13.4, 12.0), mat("pla_black")), box((1.4, 13.4, 4.6), (30.6, 14.0, 11.4), mat("pla_grey")),
           box((25.0, 13.6, 8.6), (29.0, 14.2, 11.2), faces("pla_black", up=SCR.face("lk_plot", 4.0, 2.6, printer_screen)))]
    for x0 in (2.0, 28.8):
        out += [box((x0, 0.8, 7.2), (x0 + 1.2, 9.0, 8.8), mat("pla_black")), box((x0 - 0.6, 0, 4.0), (x0 + 1.8, 0.8, 12.0), mat("pla_black"))]
    out += [box((3.2, 4.0, 7.6), (28.8, 4.4, 8.4), mat("pla_black"))]
    out += [box((3.0, 9.6, 12.0), (29.0, 9.8, 15.0), faces("paper", up=A.region("lk_plotdraw", 26.0, 3.0, blueprint))),
            box((3.0, 4.8, 14.8), (29.0, 9.6, 15.0), faces("paper", south=A.region("lk_plotdraw2", 26.0, 4.8, blueprint))),
            box((3.0, 4.4, 12.0), (29.0, 4.8, 15.0), mat("paper"))]
    return out


def plan_chest():
    """Built whole along x 0..32: the flat file of wide shallow drawers (their pulls and label holders), drawings on top."""
    w = "cab_grey"
    out = [box((0.6, 0, 2.0), (31.4, 1.0, 14.0), mat("black")), box((0.4, 1.0, 1.2), (31.6, 11.6, 14.6), mat(w)),
           box((0.2, 11.6, 1.0), (31.8, 12.2, 15.0), mat(w))]
    for k in range(6):
        y0 = 1.4 + k * 1.7
        out += [box((0.8, y0, 14.6), (31.2, y0 + 1.5, 14.9), mat(w)), box((10.0, y0 + 0.5, 14.9), (22.0, y0 + 0.9, 15.2), mat("chrome")),
                box((24.0, y0 + 0.3, 14.9), (27.0, y0 + 1.2, 15.0), faces("white", south=A.region(f"lk_pc_{k}", 3.0, 0.9, label(f"T{k + 1} FRAMES"))))]
    out += [box((2.0, 12.2, 3.0), (20.0, 12.4, 13.0), faces("paper", up=A.region("lk_pcdraw", 18.0, 10.0, blueprint))),
            box((21.0, 12.2, 6.0), (30.0, 13.2, 7.0), mat("paper")), box((21.0, 12.2, 8.0), (30.0, 13.0, 8.8), mat("bk_blue"))]
    return out


def whiteboard(half):
    """The rolling whiteboard, 2 x 1 and two high, built whole along x 0..32, its face to the south (where its viewers
    stand): the frame on its legs and castors, the board (a drone sketched on it), the marker tray and markers."""
    if half == "lower":
        out = []
        for x0 in (1.0, 30.0):
            out += [box((x0, 1.2, 7.4), (x0 + 1.0, 16, 8.6), mat("alu")), box((x0 - 1.0, 0.9, 4.0), (x0 + 2.0, 1.4, 12.0), mat("alu")),
                    box((x0 - 0.6, 0, 4.4), (x0 + 0.4, 0.9, 5.4), mat("black")), box((x0 + 0.6, 0, 10.6), (x0 + 1.6, 0.9, 11.6), mat("black"))]
        out += [box((2.0, 8.0, 7.6), (30.0, 8.6, 8.4), mat("alu"))]
        return out
    out = [box((1.0, 0, 7.4), (2.0, 13.0, 8.6), mat("alu")), box((30.0, 0, 7.4), (31.0, 13.0, 8.6), mat("alu")),
           box((2.0, 12.4, 7.4), (30.0, 13.0, 8.6), mat("alu")), box((2.0, 0, 7.4), (30.0, 0.6, 8.6), mat("alu")),
           box((2.0, 0.6, 7.7), (30.0, 12.4, 8.3), faces("whiteboard", south=A.region("lk_sketch", 28.0, 11.8, sketch))),
           box((6.0, 0.6, 8.6), (26.0, 0.8, 9.6), mat("alu")), box((6.0, 0.8, 9.4), (26.0, 1.2, 9.6), mat("alu"))]
    for k, c in enumerate(("bin_blue", "red", "black")):
        out.append(box((8.0 + k * 2.2, 0.8, 8.9), (9.8 + k * 2.2, 1.2, 9.3), mat(c)))
    return out


def flight_case(v):
    """Hard cases for the drones, stacked (v: one big case; a big and a small; two the same), their latches, handles,
    corner guards."""
    def case(x0, z0, y0, w, d, h, c):
        out = [box((x0, y0, z0), (x0 + w, y0 + h, z0 + d), {**mat(c)}),
               box((x0 - 0.1, y0 + h * 0.55, z0 - 0.1), (x0 + w + 0.1, y0 + h * 0.6, z0 + d + 0.1), mat("black"))]
        for lx in (x0 + w * 0.25, x0 + w * 0.75):
            out.append(box((lx - 0.5, y0 + h * 0.45, z0 + d), (lx + 0.5, y0 + h * 0.7, z0 + d + 0.2), mat("chrome")))
        out += [box((x0 + w / 2 - 1.2, y0 + h, z0 + d / 2 - 0.3), (x0 + w / 2 + 1.2, y0 + h + 0.4, z0 + d / 2 + 0.3), mat("black"))]
        return out
    if v == 0:
        return case(1.0, 2.0, 0, 14.0, 12.0, 5.0, "case_black")
    if v == 1:
        return case(1.0, 2.0, 0, 14.0, 12.0, 5.0, "case_black") + case(3.0, 4.0, 5.4, 9.0, 8.0, 3.6, "case_orange")
    return case(1.0, 2.0, 0, 14.0, 12.0, 4.2, "case_orange") + case(1.0, 2.0, 4.6, 14.0, 12.0, 4.2, "case_orange")


# ======================================================================== take 2 (Justin: the cage's net looked pasted
# on, the bench wood poor, the tools and toolbox poor and the toolbox tiny; "make things look more realistic"):
# everything at real size (a pixel is 6.25 cm), the tools shaped, the net and the floor modelled.

def butcher():
    """The bench top: beech butcher block, seven strips along the bench (a row of benches reads as one top), each its
    own tone with its own long grain, the glue lines between; the strips symmetric so the two ends match."""
    n = 256
    widths = [34, 36, 38, 40, 38, 36, 34]
    rng = np.random.default_rng(3240)
    out = np.zeros((n, n, 3), float)
    y = 0
    for k, w in enumerate(widths):
        tone = np.array((196, 156, 110), float) * (1 + rng.uniform(-0.06, 0.06)) * np.array((1, 1 + rng.uniform(-0.02, 0.02), 1 + rng.uniform(-0.03, 0.03)))
        strip = np.array(MK.wood(tuple(tone), 3241 + k, lines=30, depth=(0.02, 0.06)).convert("RGB"), float)
        out[y:y + w] = strip[y:y + w]
        out[y] *= 0.86
        y += w
    return TK._img(out)


def beech_end():
    """The butcher block's end grain: the strips' ends side by side (their widths as the top's), each stippled with its
    pores and the arcs of its growth rings."""
    n = 256
    widths = [34, 36, 38, 40, 38, 36, 34]
    rng = np.random.default_rng(3250)
    img = Image.new("RGB", (n, n))
    d = ImageDraw.Draw(img)
    x = 0
    for k, w in enumerate(widths):
        c = tuple(int(v * (0.9 + rng.uniform(-0.05, 0.05))) for v in (186, 146, 100))
        d.rectangle((x, 0, x + w - 1, n), fill=c)
        for r in range(6, 140, 9):
            cy = rng.uniform(-60, 320)
            d.arc((x + w / 2 - r, cy - r, x + w / 2 + r, cy + r), 0, 360, fill=tuple(int(v * 0.88) for v in c), width=1)
        d.line((x, 0, x, n), fill=tuple(int(v * 0.8) for v in c))
        x += w
    a = np.array(img, float) + TK._noise(3251, 3)[..., None]
    return TK._img(a)


def foam_tile(tone, seed):
    """EVA foam tile: a fine pebbled surface."""
    a = np.stack([np.full((256, 256), c, float) for c in tone], -1) + TK._noise(seed, 2.5)[..., None]
    pebble = np.random.default_rng(seed + 1).random((256, 256)) < 0.06
    a[pebble] *= 0.9
    return TK._img(a)


def cutting_mat(img):
    """A self-healing cutting mat: green, its grid (a line every centimetre, a heavier one every five), the rulers."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(36, 102, 74))
    cm = TX / 6.25
    for k in range(0, int(W / cm) + 1):
        d.line((k * cm, 0, k * cm, H), fill=(70, 150, 110) if k % 5 else (190, 220, 200), width=1)
    for k in range(0, int(H / cm) + 1):
        d.line((0, k * cm, W, k * cm), fill=(70, 150, 110) if k % 5 else (190, 220, 200), width=1)
    return img


def socket_strip(img):
    """The power rail along the bench's back: grey, its sockets each with its switch and light."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(70, 74, 80))
    k = TX
    for i in range(int(W / (2.6 * k))):
        x0 = 0.6 * k + i * 2.6 * k
        d.rounded_rectangle((x0, 0.15 * k, x0 + 1.4 * k, H - 0.15 * k), radius=3, fill=(232, 232, 228))
        d.rectangle((x0 + 0.45 * k, 0.35 * k, x0 + 0.55 * k, 0.6 * k), fill=(30, 30, 30))
        d.rectangle((x0 + 0.85 * k, 0.35 * k, x0 + 0.95 * k, 0.6 * k), fill=(30, 30, 30))
        d.rectangle((x0 + 1.55 * k, 0.25 * k, x0 + 2.0 * k, 0.6 * k), fill=(200, 40, 36))
    return img


def charger_face(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(20, 22, 26))
    d.rectangle((W * 0.06, H * 0.12, W * 0.62, H * 0.88), fill=(16, 40, 60))
    f = ImageFont.truetype(MONO, max(6, round(H * 0.24)))
    # live: the charge current, the pack's voltage and how full it is (a charge of 40 minutes, then it holds)
    TK.live_part(img, (W * 0.09, H * 0.2), "6S 4.2A", "4.2A", f, (150, 220, 255), "la", bg=(16, 40, 60), src="value", base=4.2, spread=0.08, dp=1, period=20, unit="A")
    TK.live_part(img, (W * 0.09, H * 0.52), "24.6V 78%", "24.6V", f, (150, 220, 255), "la", bg=(16, 40, 60), src="value", base=24.6, spread=0.12, dp=1, period=60, unit="V")
    TK.live_part(img, (W * 0.09, H * 0.52), "24.6V 78%", "78%", f, (150, 220, 255), "la", bg=(16, 40, 60), widest="100%", again=True, src="value", base=40,
                 rate=0.025, cycle=3000, max=100, unit="%")
    for k in range(3):
        d.ellipse((W * (0.7 + k * 0.09), H * 0.4, W * (0.76 + k * 0.09), H * 0.6), fill=(60, 64, 70))
    return img


def sign_plate(lines, bg=(250, 250, 246), band=(200, 40, 36)):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=bg)
        d.rectangle((0, 0, W, H * 0.3), fill=band)
        d.text((W / 2, H * 0.15), lines[0], font=_fit(d, lines[0], FONT, round(H * 0.2), W - 8), fill=(255, 255, 255), anchor="mm")
        y = H * 0.45
        for t in lines[1:]:
            d.text((W / 2, y), t, font=_fit(d, t, FONT, round(H * 0.13), W - 10), fill=(30, 30, 34), anchor="mm")
            y += H * 0.18
        return img
    return draw


def swap_xy(elements):
    """Turns parts built standing up (axis y) to lie along x: x and y swapped."""
    names = {"up": "east", "down": "west", "east": "up", "west": "down", "north": "north", "south": "south"}
    out = []
    for e in elements:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        out.append(box((y0, x0, z0), (y1, x1, z1), {names[k]: v for k, v in e["faces"].items()}))
    return out


def rod_x(x0, x1, cy, cz, r, tex, n=3):
    """A round rod lying along x (centre cy, cz)."""
    return swap_xy(round_part(cy, cz, r, x0, x1, tex, n=n))


# ------------------------------------------------------------------ the hand tools (on a pegboard: its face at z 15.2,
# the tools in front of it toward -z; z1 their back, against the board's hooks)
def peg_hook(x, y, out=1.0):
    """A J hook: its shank out of the board, the tip turned up."""
    return [box((x - 0.07, y - 0.07, 15.2 - out), (x + 0.07, y + 0.07, 15.2), mat("chrome")),
            box((x - 0.07, y + 0.07, 15.2 - out), (x + 0.07, y + 0.4, 15.2 - out + 0.14), mat("chrome"))]


def wrench(x, y, ln, z=14.45):
    """A combination wrench hung by its ring end at (x, y) (the hook through the ring): the ring, the shank, the open
    end at the bottom."""
    t = 0.12
    rw, hole = 0.4 + ln * 0.06, 0.16 + ln * 0.03
    out = [box((x - rw, y - rw, z), (x + rw, y - hole, z + t), mat("chrome")), box((x - rw, y + hole, z), (x + rw, y + rw, z + t), mat("chrome")),
           box((x - rw, y - hole, z), (x - hole, y + hole, z + t), mat("chrome")), box((x + hole, y - hole, z), (x + rw, y + hole, z + t), mat("chrome"))]
    sw = 0.12 + ln * 0.02
    out.append(box((x - sw, y - ln + rw, z + 0.01), (x + sw, y - rw, z + t - 0.01), mat("chrome")))
    hw, gap = rw * 1.05, hole * 1.1
    yb = y - ln
    out += [box((x - hw, yb + 0.25, z), (x + hw, yb + rw + 0.35, z + t), mat("chrome")),
            box((x - hw, yb, z), (x - gap, yb + 0.25, z + t), mat("chrome")), box((x + gap, yb, z), (x + hw, yb + 0.25, z + t), mat("chrome"))]
    return peg_hook(x, y - hole + 0.07, 0.9) + out


def screwdriver(x, top, colour, shank, z=14.6, tip="flat"):
    """A screwdriver standing in its holder (the holder's top at `top`): the handle in two colours with its grip ribs,
    the cap, the ferrule, the shank down through the holder, the tip."""
    c1, c2 = colour
    out = [box((x - 0.26, top + 0.2, z - 0.26), (x + 0.26, top + 1.6, z + 0.26), mat(c1)),
           box((x - 0.29, top + 0.45, z - 0.29), (x + 0.29, top + 1.25, z + 0.29), mat(c2)),
           box((x - 0.2, top + 1.6, z - 0.2), (x + 0.2, top + 1.8, z + 0.2), mat(c1)),
           box((x - 0.14, top, z - 0.14), (x + 0.14, top + 0.2, z + 0.14), mat("chrome")),
           box((x - 0.06, top - shank, z - 0.06), (x + 0.06, top, z + 0.06), mat("chrome"))]
    if tip == "flat":
        out.append(box((x - 0.11, top - shank - 0.25, z - 0.03), (x + 0.11, top - shank, z + 0.03), mat("chrome")))
    return out


def driver_holder(x0, x1, y):
    return [box((x0, y - 0.2, 14.0), (x1, y, 15.2), mat("steel")), box((x0, y - 1.0, 15.0), (x0 + 0.2, y, 15.2), mat("steel")),
            box((x1 - 0.2, y - 1.0, 15.0), (x1, y, 15.2), mat("steel"))]


def pliers(x, y, grip="cab_red", ln=3.2, z=14.45):
    """Pliers hung jaws down from a hook between their handles: the two handles in their grips, the pivot, the jaws."""
    out = peg_hook(x, y + 0.1, 0.95)
    t = 0.18
    for s in (-1, 1):
        hx0, hx1 = (x - 0.38, x - 0.08) if s < 0 else (x + 0.08, x + 0.38)
        out += [box((hx0, y - ln * 0.62, z), (hx1, y + 0.1, z + t), mat(grip)),
                box((hx0 + 0.04, y - ln * 0.68, z + 0.02), (hx1 - 0.04, y - ln * 0.62, z + t - 0.02), mat("steel"))]
    py = y - ln * 0.68
    out += [box((x - 0.32, py - 0.5, z - 0.02), (x + 0.32, py, z + t + 0.02), mat("steel")),
            box((x - 0.18, py - 0.36, z + t + 0.02), (x + 0.18, py - 0.12, z + t + 0.06), mat("chrome")),
            box((x - 0.26, py - 0.95, z), (x + 0.26, py - 0.5, z + t), mat("steel")),
            box((x - 0.17, py - 1.3, z), (x + 0.17, py - 0.95, z + t), mat("steel"))]
    return out


def hammer(x, y, z=14.3):
    """A claw hammer hung head up on two hooks: the head (its face, the neck, the claw's two prongs), the hickory handle
    and its rubber grip."""
    out = peg_hook(x - 0.55, y - 0.35, 1.1) + peg_hook(x + 0.55, y - 0.35, 1.1)
    out += [box((x - 0.3, y - 0.05, z - 0.3), (x + 0.3, y + 0.55, z + 0.3), mat("steel")),
            box((x + 0.3, y + 0.05, z - 0.26), (x + 0.75, y + 0.45, z + 0.26), mat("steel")),
            box((x + 0.75, y - 0.02, z - 0.32), (x + 1.0, y + 0.52, z + 0.32), mat("steel")),
            box((x - 1.2, y + 0.1, z - 0.24), (x - 0.3, y + 0.45, z - 0.06), mat("steel")),
            box((x - 1.2, y + 0.1, z + 0.06), (x - 0.3, y + 0.45, z + 0.24), mat("steel")),
            box((x - 0.2, y - 3.6, z - 0.16), (x + 0.2, y - 0.05, z + 0.16), mat("hickory")),
            box((x - 0.25, y - 4.6, z - 0.2), (x + 0.25, y - 2.8, z + 0.2), mat("grip_black"))]
    return out


def tape(x, y, z=14.3):
    """A tape measure hung by its clip: the yellow case in its black bumper, the tape's mouth and its hook."""
    out = peg_hook(x, y + 0.55, 1.0)
    out += [box((x - 0.55, y - 0.55, z - 0.28), (x + 0.55, y + 0.55, z + 0.28), mat("bin_yellow")),
            box((x - 0.6, y - 0.6, z - 0.12), (x + 0.6, y + 0.6, z + 0.12), mat("grip_black")),
            box((x - 0.18, y - 0.45, z + 0.28), (x + 0.18, y + 0.62, z + 0.36), mat("steel")),
            box((x + 0.55, y - 0.4, z - 0.15), (x + 0.75, y - 0.15, z + 0.15), mat("grip_black")),
            box((x + 0.75, y - 0.38, z - 0.12), (x + 0.82, y - 0.05, z + 0.12), mat("steel"))]
    return out


def hex_keys(x, y, z=14.4):
    """A set of hex keys in their holder on a hook: the red holder, the keys' long legs down, their short legs out."""
    out = peg_hook(x + 0.9, y + 0.25, 1.0) + [box((x, y - 0.22, z - 0.22), (x + 1.8, y + 0.22, z + 0.22), mat("cab_red"))]
    for k in range(7):
        s = 0.05 + k * 0.012
        ln = 0.9 + k * 0.22
        kx = x + 0.18 + k * 0.24
        out += [box((kx - s, y - 0.22 - ln, z - s), (kx + s, y - 0.22, z + s), mat("black")),
                box((kx - s, y - 0.22 - ln - s * 2, z - 0.3 - k * 0.04), (kx + s, y - 0.22 - ln, z + s), mat("black"))]
    return out


def adj_wrench(x, y, z=14.45):
    """An adjustable wrench hung by the hole in its handle: the handle, the head with its fixed jaw, the moving jaw,
    the worm."""
    out = peg_hook(x, y - 0.1, 0.9)
    t = 0.16
    out += [box((x - 0.24, y - 2.6, z), (x + 0.24, y + 0.15, z + t), mat("chrome")),
            box((x - 0.5, y - 3.3, z), (x + 0.4, y - 2.6, z + t), mat("chrome")),
            box((x - 0.5, y - 3.75, z), (x - 0.2, y - 3.3, z + t), mat("chrome")),
            box((x + 0.1, y - 3.7, z), (x + 0.5, y - 3.35, z + t), mat("chrome"))]
    out += swap_xy(round_part(y - 2.8, z + 0.08, 0.14, x - 0.2, x + 0.2, "steel"))
    return out


def knife(x, y, z=14.5):
    out = peg_hook(x, y + 0.1, 0.9)
    out += [box((x - 0.22, y - 1.8, z), (x + 0.22, y + 0.2, z + 0.25), mat("bin_yellow")),
            box((x - 0.24, y - 1.1, z - 0.02), (x + 0.24, y + 0.2, z + 0.27), mat("grip_black")),
            box((x - 0.08, y - 2.2, z + 0.1), (x + 0.12, y - 1.8, z + 0.14), mat("chrome"))]
    return out


def peg_bin(x0, y0, colour, z=14.0):
    """A small parts bin hung on the board: its open front lip, its sides, and what is in it."""
    w, h, d = 1.8, 1.0, 1.2
    out = [box((x0, y0, z), (x0 + w, y0 + 0.12, z + d), mat(colour)),
           box((x0, y0, z), (x0 + 0.12, y0 + h, z + d), mat(colour)), box((x0 + w - 0.12, y0, z), (x0 + w, y0 + h, z + d), mat(colour)),
           box((x0 + 0.12, y0, z), (x0 + w - 0.12, y0 + h * 0.55, z + 0.12), mat(colour)),
           box((x0 + 0.12, y0, z + d - 0.12), (x0 + w - 0.12, y0 + h, z + d), mat(colour)),
           box((x0 + 0.12, y0 + 0.12, z + 0.12), (x0 + w - 0.12, y0 + 0.45, z + d - 0.12), mat("chrome"))]
    return out


def can_shelf(x0, y, n, rng):
    out = [box((x0, y - 0.3, 13.0), (x0 + n * 1.3 + 0.4, y, 15.2), mat("steel")), box((x0, y, 13.0), (x0 + n * 1.3 + 0.4, y + 0.6, 13.12), mat("steel"))]
    for k in range(n):
        c = rng.choice(("cab_red", "bin_blue", "bin_yellow", "white"))
        cx = x0 + 0.85 + k * 1.3
        out += round_part(cx, 14.1, 0.52, y, y + 2.6, c, n=3) + round_part(cx, 14.1, 0.36, y + 2.6, y + 3.0, "chrome", n=3) + \
            round_part(cx, 14.1, 0.18, y + 3.0, y + 3.3, "black", n=3)
    return out


PEG_LAYOUTS = [
    lambda r: ([w for k in range(5) for w in wrench(1.0 + k * 1.45, 13.6, 2.4 + k * 0.4)] + driver_holder(8.0, 14.6, 10.6)
               + [p for k, c in enumerate((("cab_red", "black"), ("bin_yellow", "black"), ("bin_blue", "black"), ("black", "cab_red"), ("cab_red", "black")))
                  for p in screwdriver(8.7 + k * 1.25, 10.6, c, 2.0 + (k % 3) * 0.5)]
               + pliers(1.8, 7.6) + pliers(3.4, 7.6, "bin_blue") + hammer(6.4, 8.6) + tape(11.0, 5.6) + hex_keys(12.6, 6.2)),
    lambda r: (can_shelf(1.0, 3.0, 4, r) + adj_wrench(1.6, 13.4) + pliers(3.2, 13.6) + pliers(4.6, 13.6, "bin_yellow")
               + peg_bin(7.0, 11.4, "bin_blue") + peg_bin(9.2, 11.4, "bin_red") + peg_bin(11.4, 11.4, "bin_blue") + peg_bin(13.6, 11.4, "bin_yellow")
               + knife(8.0, 8.0) + tape(10.2, 7.6) + hammer(13.2, 8.4)),
    lambda r: (hammer(2.2, 13.0) + [w for k in range(4) for w in wrench(4.6 + k * 1.45, 13.6, 2.6 + k * 0.4)] + adj_wrench(11.2, 13.4)
               + driver_holder(1.2, 7.2, 7.6) + [p for k, c in enumerate((("bin_yellow", "black"), ("cab_red", "black"), ("bin_blue", "black"), ("black", "bin_yellow")))
                                                  for p in screwdriver(1.9 + k * 1.3, 7.6, c, 2.2 + (k % 2) * 0.6)]
               + hex_keys(9.0, 7.0) + tape(13.4, 7.2) + can_shelf(10.0, 2.2, 3, r)),
    lambda r: (peg_bin(1.0, 12.2, "bin_red") + peg_bin(3.2, 12.2, "bin_red") + peg_bin(1.0, 10.4, "bin_blue") + peg_bin(3.2, 10.4, "bin_blue")
               + hex_keys(6.4, 13.0) + pliers(9.4, 13.6) + pliers(10.9, 13.6, "black") + knife(12.6, 13.4) + adj_wrench(14.2, 13.4)
               + driver_holder(6.0, 12.6, 8.0) + [p for k, c in enumerate((("cab_red", "black"), ("cab_red", "black"), ("bin_blue", "black"), ("bin_yellow", "black"), ("black", "cab_red")))
                                                   for p in screwdriver(6.7 + k * 1.25, 8.0, c, 1.8 + (k % 3) * 0.6)]
               + tape(2.2, 6.0) + hammer(14.0, 6.6))]


def pegboard(v):
    """A pegboard on the wall over a bench, its face to the north (the wall to the south): the board on its battens, the
    hooks and the tools on them at their real sizes (combination wrenches, screwdrivers in their holder, pliers, a claw
    hammer, a tape measure, hex keys, an adjustable wrench, a knife, bins, a shelf of cans); every layout its own."""
    rng = random.Random(3300 + v)
    out = [box((0.2, 0.4, 15.2), (15.8, 15.6, 15.6), {**mat("pegboard"), "south": {"texture": "#walnut"}}),
           box((0.2, 0.0, 15.6), (15.8, 0.4, 16.0), mat("walnut")), box((0.2, 15.6, 15.6), (15.8, 16.0, 16.0), mat("walnut"))]
    return out + PEG_LAYOUTS[v % len(PEG_LAYOUTS)](rng)


# ------------------------------------------------------------------ the workbench (its top at 90 cm)
BENCH_TOP = 14.4


def workbench_body():
    """One block of the bench, its user looking north (the wall behind it to the north): the beech butcher block top, the
    power rail along its back, the steel frame (square tube legs on levelling feet, the aprons, the rails), the lower
    shelf, the two drawers hung under the top (their pulls and label holders)."""
    t = BENCH_TOP
    out = [box((0, t - 1.2, 0.6), (16, t, 15.4), {"up": {"texture": "#beech"}, "down": {"texture": "#steel"}, "north": {"texture": "#beech_side"},
                                                   "south": {"texture": "#beech_side"}, "east": {"texture": "#beech_end"}, "west": {"texture": "#beech_end"}}),
           box((0, t, 0.6), (16, t + 1.1, 1.5), faces("cab_grey", south=A.region("lk_sockets", 16.0, 1.1, socket_strip))),
           box((0.3, 0.3, 1.2), (1.3, t - 1.2, 2.2), mat("steel_blue")), box((0.3, 0.3, 13.8), (1.3, t - 1.2, 14.8), mat("steel_blue")),
           box((0.2, 0, 1.1), (1.4, 0.3, 2.3), mat("grip_black")), box((0.2, 0, 13.7), (1.4, 0.3, 14.9), mat("grip_black")),
           box((1.3, t - 2.2, 1.2), (16, t - 1.2, 1.8), mat("steel_blue", skip=("up",))),
           box((1.3, t - 2.2, 14.2), (16, t - 1.2, 14.8), mat("steel_blue", skip=("up",))),
           box((0.3, 2.4, 2.2), (1.3, 3.2, 13.8), mat("steel_blue")),
           box((1.3, 2.4, 1.6), (16, 2.8, 14.6), {"up": {"texture": "#beech"}, "down": {"texture": "#beech_side"}, "north": {"texture": "#beech_side"},
                                                   "south": {"texture": "#beech_side"}, "east": {"texture": "#beech_end"}, "west": {"texture": "#beech_end"}}),
           box((9.0, 8.0, 2.0), (15.4, t - 2.2, 14.6), mat("steel_blue")),
           box((9.2, 10.4, 14.6), (15.2, t - 2.4, 15.0), mat("cab_grey")), box((9.2, 8.2, 14.6), (15.2, 10.2, 15.0), mat("cab_grey"))]
    for yc in (11.1, 9.2):
        out += [box((10.6, yc - 0.15, 15.3), (13.8, yc + 0.15, 15.6), mat("chrome")),
                box((10.8, yc - 0.15, 15.0), (11.1, yc + 0.15, 15.3), mat("chrome")), box((13.3, yc - 0.15, 15.0), (13.6, yc + 0.15, 15.3), mat("chrome")),
                box((11.6, yc + 0.3, 15.0), (12.8, yc + 0.7, 15.05), mat("white"))]
    return out


def vise(x0, z0, y0):
    """A bench vise bolted at the front edge (its jaws over the edge): the swivel base, the body and its anvil, the
    fixed jaw and the sliding jaw with their serrated plates, the slide, the screw and its T-handle with knobs."""
    b = "steel_blue"
    out = round_part(x0 + 1.6, z0 + 1.6, 1.5, y0, y0 + 0.4, b, n=4)
    out += [box((x0 + 0.5, y0 + 0.4, z0 + 0.4), (x0 + 2.7, y0 + 1.4, z0 + 3.2), mat(b)),
            box((x0 + 0.7, y0 + 1.4, z0 + 0.3), (x0 + 2.5, y0 + 1.8, z0 + 1.3), mat(b)),
            box((x0 + 0.2, y0 + 1.4, z0 + 1.3), (x0 + 3.0, y0 + 2.6, z0 + 2.0), mat(b)),
            box((x0 + 0.2, y0 + 1.7, z0 + 2.0), (x0 + 3.0, y0 + 2.5, z0 + 2.08), mat("steel")),
            box((x0 + 1.1, y0 + 1.0, z0 + 2.0), (x0 + 2.1, y0 + 1.6, z0 + 4.2), mat("chrome")),
            box((x0 + 0.2, y0 + 1.4, z0 + 3.2), (x0 + 3.0, y0 + 2.6, z0 + 3.9), mat(b)),
            box((x0 + 0.2, y0 + 1.7, z0 + 3.12), (x0 + 3.0, y0 + 2.5, z0 + 3.2), mat("steel"))]
    out += swap_yz(round_part(x0 + 1.6, y0 + 1.15, 0.16, z0 + 3.9, z0 + 4.86, "chrome"))
    out += rod_x(x0 - 0.4, x0 + 3.6, y0 + 1.15, z0 + 5.0, 0.1, "chrome")
    out += [box((x0 - 0.6, y0 + 0.95, z0 + 4.8), (x0 - 0.3, y0 + 1.35, z0 + 5.2), mat("grip_black")),
            box((x0 + 3.5, y0 + 0.95, z0 + 4.8), (x0 + 3.8, y0 + 1.35, z0 + 5.2), mat("grip_black"))]
    return out


def toolbox(x0, z0, y0, colour="cab_red"):
    """A steel cantilever toolbox (50 by 25 by 23 cm): the body, the lid over its seam, the black end caps, the two
    latches, the folding handle on its pivots."""
    w, d, h = 8.0, 4.0, 3.7
    c = colour
    out = [box((x0, y0, z0), (x0 + w, y0 + h - 1.0, z0 + d), mat(c)),
           box((x0 + 0.06, y0 + h - 1.0, z0 + 0.06), (x0 + w - 0.06, y0 + h - 0.9, z0 + d - 0.06), mat("black")),
           box((x0, y0 + h - 0.9, z0), (x0 + w, y0 + h, z0 + d), mat(c)),
           box((x0 - 0.12, y0, z0 - 0.12), (x0 + 0.5, y0 + h, z0 + 0.5), mat("grip_black")), box((x0 + w - 0.5, y0, z0 - 0.12), (x0 + w + 0.12, y0 + h, z0 + 0.5), mat("grip_black")),
           box((x0 - 0.12, y0, z0 + d - 0.5), (x0 + 0.5, y0 + h, z0 + d + 0.12), mat("grip_black")), box((x0 + w - 0.5, y0, z0 + d - 0.5), (x0 + w + 0.12, y0 + h, z0 + d + 0.12), mat("grip_black"))]
    for lx in (x0 + 1.6, x0 + w - 2.4):
        out += [box((lx, y0 + h - 1.6, z0 + d), (lx + 0.8, y0 + h - 0.6, z0 + d + 0.15), mat("chrome")),
                box((lx + 0.2, y0 + h - 1.9, z0 + d), (lx + 0.6, y0 + h - 1.6, z0 + d + 0.1), mat("chrome"))]
    out += [box((x0 + 2.2, y0 + h, z0 + 1.7), (x0 + 2.7, y0 + h + 0.3, z0 + 2.3), mat("black")), box((x0 + w - 2.7, y0 + h, z0 + 1.7), (x0 + w - 2.2, y0 + h + 0.3, z0 + 2.3), mat("black")),
            box((x0 + 2.4, y0 + h + 0.3, z0 + 1.8), (x0 + w - 2.4, y0 + h + 0.75, z0 + 2.2), mat("grip_black"))]
    return out


def drill(x0, z0, y0):
    """A cordless drill lying on its side: the motor body, the gearbox and its clutch ring, the keyless chuck and its
    bit, the handle with the trigger, the battery pack."""
    out = rod_x(x0 + 1.0, x0 + 3.4, y0 + 0.62, z0 + 0.7, 0.62, "bin_yellow", n=4)
    out += rod_x(x0 + 3.4, x0 + 3.9, y0 + 0.62, z0 + 0.7, 0.5, "grip_black", n=4)
    out += rod_x(x0 + 3.9, x0 + 4.6, y0 + 0.62, z0 + 0.7, 0.36, "grip_black", n=3)
    out += rod_x(x0 + 4.6, x0 + 5.0, y0 + 0.62, z0 + 0.7, 0.22, "chrome", n=3)
    out += [box((x0 + 5.0, y0 + 0.56, z0 + 0.64), (x0 + 6.3, y0 + 0.68, z0 + 0.76), mat("chrome")),
            box((x0 + 1.4, y0 + 0.05, z0 + 1.2), (x0 + 2.4, y0 + 0.95, z0 + 3.6), mat("grip_black")),
            box((x0 + 2.4, y0 + 0.25, z0 + 1.4), (x0 + 2.7, y0 + 0.75, z0 + 2.0), mat("bin_yellow")),
            box((x0 + 0.8, y0, z0 + 3.6), (x0 + 3.0, y0 + 1.2, z0 + 4.8), mat("grip_black")),
            box((x0 + 1.0, y0 + 0.2, z0 + 4.8), (x0 + 2.8, y0 + 1.0, z0 + 4.9), mat("bin_yellow"))]
    return out


def bench_grinder(x0, z0, y0):
    """A 15 cm bench grinder: its cast base, the motor between the two wheels, the wheel guards, the tool rests, the eye
    shields, the switch."""
    out = [box((x0 + 1.8, y0, z0 + 0.6), (x0 + 5.0, y0 + 0.6, z0 + 3.0), mat("cab_grey")),
           box((x0 + 2.6, y0 + 0.6, z0 + 1.2), (x0 + 4.2, y0 + 1.2, z0 + 2.4), mat("cab_grey"))]
    out += rod_x(x0 + 2.0, x0 + 4.8, y0 + 2.2, z0 + 1.8, 1.05, "cab_grey", n=4)
    for wx0, wx1, s in ((x0 + 0.6, x0 + 1.5, -1), (x0 + 5.3, x0 + 6.2, 1)):
        out += rod_x(wx0, wx1, y0 + 2.2, z0 + 1.8, 1.25, "pla_grey", n=5)
        gx0, gx1 = wx0 - 0.15, wx1 + 0.15
        out += [box((gx0, y0 + 3.45, z0 + 0.4), (gx1, y0 + 3.65, z0 + 3.0), mat("cab_grey")),
                box((gx0, y0 + 2.2, z0 + 0.3), (gx1, y0 + 3.6, z0 + 0.5), mat("cab_grey")),
                box((gx0, y0 + 0.8, z0 + 0.4), (gx1, y0 + 2.2, z0 + 0.6), mat("cab_grey")),
                box((gx0, y0 + 1.5, z0 + 3.0), (gx1, y0 + 1.7, z0 + 3.9), mat("steel")),
                box((gx0, y0 + 3.3, z0 + 2.8), (gx1, y0 + 4.3, z0 + 2.9), mat("shield"))]
        out.append(box((min(gx1, x0 + 2.0) - 0.1 if s < 0 else x0 + 4.8, y0 + 2.0, z0 + 1.6), (x0 + 2.0 if s < 0 else max(gx0, x0 + 4.8) + 0.1, y0 + 2.4, z0 + 2.0), mat("chrome")))
    out += [box((x0 + 3.1, y0 + 0.7, z0 + 2.4), (x0 + 3.7, y0 + 1.1, z0 + 2.55), mat("bin_yellow"))]
    return out


def charger(x0, z0, y0):
    """The LiPo balance charger on a fire-proof tile: its case, its screen and keys, two batteries charging on their
    leads."""
    out = [box((x0 - 0.4, y0, z0 - 0.4), (x0 + 6.4, y0 + 0.25, z0 + 4.4), mat("pla_grey")),
           box((x0, y0 + 0.25, z0), (x0 + 3.0, y0 + 1.5, z0 + 2.4), faces("grip_black", south=SCR.face("lk_charger", 3.0, 1.25, charger_face)))]
    for k in range(2):
        bz = z0 + 0.2 + k * 1.9
        out += [box((x0 + 3.6, y0 + 0.25, bz), (x0 + 6.0, y0 + 1.15, bz + 1.4), mat("grip_black")),
                box((x0 + 3.65, y0 + 0.3, bz + 1.4), (x0 + 5.95, y0 + 1.1, bz + 1.45), faces("grip_black", south=A.region("lk_batt", 2.3, 0.8, label("6S 1300", (30, 30, 34), (240, 160, 40))))),
                box((x0 + 3.0, y0 + 0.5, bz + 0.5), (x0 + 3.6, y0 + 0.62, bz + 0.62), mat("red")), box((x0 + 3.0, y0 + 0.66, bz + 0.6), (x0 + 3.6, y0 + 0.78, bz + 0.72), mat("black"))]
    return out


def heat_gun(x0, z0, y0):
    """A heat gun standing on its back end: the body, the nozzle up, the handle, the stand."""
    out = [box((x0, y0, z0), (x0 + 2.2, y0 + 0.3, z0 + 1.8), mat("black"))]
    out += round_part(x0 + 1.1, z0 + 0.9, 0.62, y0 + 0.3, y0 + 3.0, "cab_red", n=4)
    out += round_part(x0 + 1.1, z0 + 0.9, 0.4, y0 + 3.0, y0 + 4.0, "chrome", n=3)
    out += [box((x0 + 0.85, y0 + 0.5, z0 + 1.5), (x0 + 1.35, y0 + 2.4, z0 + 2.6), mat("cab_red")),
            box((x0 + 0.9, y0 + 0.4, z0 + 2.6), (x0 + 1.3, y0 + 0.9, z0 + 3.4), mat("grip_black"))]
    return out


def hammer_lying(x0, z0, y0):
    return [box((x0, y0, z0 - 0.3), (x0 + 0.6, y0 + 0.6, z0 + 0.3), mat("steel")),
            box((x0 - 1.0, y0 + 0.1, z0 - 0.24), (x0, y0 + 0.4, z0 + 0.24), mat("steel")),
            box((x0 + 0.6, y0 + 0.1, z0 - 0.2), (x0 + 4.0, y0 + 0.42, z0 + 0.2), mat("hickory")),
            box((x0 + 2.8, y0 + 0.06, z0 - 0.25), (x0 + 4.2, y0 + 0.48, z0 + 0.25), mat("grip_black"))]


def tape_lying(x0, z0, y0):
    return [box((x0, y0, z0), (x0 + 1.1, y0 + 0.55, z0 + 1.1), mat("bin_yellow")), box((x0 - 0.05, y0 + 0.15, z0 - 0.05), (x0 + 1.15, y0 + 0.4, z0 + 1.15), mat("grip_black")),
            box((x0 + 1.1, y0 + 0.05, z0 + 0.3), (x0 + 1.4, y0 + 0.15, z0 + 0.8), mat("bin_yellow"))]


def driver_lying(x0, z0, y0, c=("cab_red", "black")):
    return [box((x0, y0, z0 - 0.26), (x0 + 1.4, y0 + 0.52, z0 + 0.26), mat(c[0])), box((x0 + 0.25, y0 - 0.03, z0 - 0.29), (x0 + 1.05, y0 + 0.55, z0 + 0.29), mat(c[1])),
            box((x0 + 1.4, y0 + 0.12, z0 - 0.14), (x0 + 1.6, y0 + 0.4, z0 + 0.14), mat("chrome")),
            box((x0 + 1.6, y0 + 0.2, z0 - 0.06), (x0 + 3.6, y0 + 0.32, z0 + 0.06), mat("chrome"))]


def prop(cx, cz, y0, angle, ln=2.8, colour="pla_black"):
    """A two-bladed prop lying flat: the hub, each blade wide at the root and narrowing to its tip."""
    r = {"angle": angle, "axis": "y", "origin": [cx, y0, cz]} if angle else None
    out = round_part(cx, cz, 0.22, y0, y0 + 0.2, "black")
    for s in (-1, 1):
        a, b = (cx + 0.2, cx + ln * 0.55) if s > 0 else (cx - ln * 0.55, cx - 0.2)
        c, d = (cx + ln * 0.55, cx + ln) if s > 0 else (cx - ln, cx - ln * 0.55)
        out += [box((a, y0 + 0.02, cz - 0.24), (b, y0 + 0.12, cz + 0.24), mat(colour), r),
                box((c, y0 + 0.02, cz - 0.15), (d, y0 + 0.1, cz + 0.15), mat(colour), r)]
    return out


def frame7(cx, cz, y0, motors=2):
    """A 7 inch quad frame being built: the carbon bottom plate and four arms in an X, the standoffs, the flight stack
    in the middle, motors on `motors` of the arms (their bells and bases), a prop on the first."""
    out = [box((cx - 0.8, y0, cz - 1.0), (cx + 0.8, y0 + 0.2, cz + 1.0), mat("carbon"))]
    for ang in (45, -45):
        out.append(box((cx - 3.2, y0, cz - 0.25), (cx + 3.2, y0 + 0.2, cz + 0.25), mat("carbon"), {"angle": ang, "axis": "y", "origin": [cx, y0, cz]}))
    for dx, dz in ((0.6, 0.8), (-0.6, 0.8), (0.6, -0.8), (-0.6, -0.8)):
        out.append(box((cx + dx - 0.08, y0 + 0.2, cz + dz - 0.08), (cx + dx + 0.08, y0 + 1.2, cz + dz + 0.08), mat("alu")))
    out += [box((cx - 0.5, y0 + 0.2, cz - 0.5), (cx + 0.5, y0 + 0.35, cz + 0.5), mat("green_pcb")),
            box((cx - 0.5, y0 + 0.55, cz - 0.5), (cx + 0.5, y0 + 0.7, cz + 0.5), mat("green_pcb")),
            box((cx - 0.2, y0 + 0.7, cz - 0.2), (cx + 0.2, y0 + 0.82, cz + 0.2), mat("black"))]
    for k, (dx, dz) in enumerate(((2.12, 2.12), (-2.12, 2.12), (2.12, -2.12), (-2.12, -2.12))[:motors]):
        out += round_part(cx + dx, cz + dz, 0.45, y0 + 0.2, y0 + 0.45, "black", n=3) + round_part(cx + dx, cz + dz, 0.42, y0 + 0.45, y0 + 0.95, "steel", n=3)
        if k == 0:
            out += prop(cx + dx, cz + dz, y0 + 0.95, 22.5)
    return out


def build_stand(cx, cz, y0):
    """A drone build stand (a turntable on its weighted base) with a 7 inch frame on it, half built."""
    out = round_part(cx, cz, 1.4, y0, y0 + 0.3, "steel", n=4) + round_part(cx, cz, 0.3, y0 + 0.3, y0 + 0.9, "chrome")
    out += round_part(cx, cz, 1.2, y0 + 0.9, y0 + 1.05, "alu", n=4)
    out += frame7(cx, cz, y0 + 1.05)
    return out


def bench_top(v):
    """The things on a bench (variant v), all at their real sizes: a build at the cutting mat on its stand with the parts
    tray and the charger; the toolbox and the vise, a hammer and a tape; the batteries charging and the heat gun;
    the laptop, the prop balancer and the drill; the bench grinder and a vise; a clean bench with the cutting mat."""
    t = BENCH_TOP
    out = []
    mat_pic = lambda key, w, d: A.region(key, w, d, cutting_mat)
    if v == 0:
        out += [box((2.0, t, 6.0), (11.6, t + 0.1, 12.0), faces("grip_black", up=mat_pic("lk_cmat_96", 9.6, 6.0)))]
        out += build_stand(6.8, 9.0, t + 0.1) + tray(12.4, 7.0, 2.4, 1.6, t) + charger(9.0, 1.8, t) + driver_lying(2.6, 13.2, t)
    elif v == 1:
        out += toolbox(1.0, 1.8, t) + vise(11.6, 11.0, t) + hammer_lying(3.4, 12.6, t) + tape_lying(7.0, 9.0, t) + driver_lying(1.2, 9.6, t, ("bin_yellow", "black"))
    elif v == 2:
        out += charger(1.0, 2.2, t) + heat_gun(11.6, 3.0, t) + lipo_bag(8.0, 9.6, t)
        out += [box((2.0, t, 10.4), (2.3 + k * 0.6, t + 0.2, 10.6 + k * 0.4), mat(("black", "red", "bin_blue")[k])) for k in range(3)]
    elif v == 3:
        out += laptop_at(1.4, 3.6, t, "lk_lap_cad", cad_screen("quad"))
        out += [box((9.6, t, 4.0), (12.8, t + 0.5, 7.0), mat("black")), box((11.0, t + 0.5, 5.3), (11.4, t + 3.0, 5.7), mat("chrome")),
                box((8.0, t + 3.0, 5.35), (14.4, t + 3.2, 5.65), mat("pla_black"), {"angle": 22.5, "axis": "y", "origin": [11.2, t + 3.0, 5.5]})]
        out += drill(7.0, 10.0, t)
        for k in range(3):
            out += prop(3.4 + k * 0.6, 11.4 + k * 0.4, t + k * 0.2, (-22.5, 0.0, 22.5)[k], colour=("pla_black", "pla_orange", "pla_black")[k])
    elif v == 4:
        out += bench_grinder(1.0, 2.0, t) + vise(11.6, 11.0, t) + toolbox(8.6, 1.8, t, "cab_grey") + frame7(5.0, 10.4, t, motors=4)
    else:
        out += [box((3.0, t, 5.0), (12.6, t + 0.1, 11.0), faces("grip_black", up=mat_pic("lk_cmat_96", 9.6, 6.0)))]
        out += [box((4.0, t + 0.1, 6.0), (9.0, t + 0.2, 9.6), faces("paper", up=A.region("lk_print_1", 5.0, 3.6, blueprint)))] + driver_lying(10.0, 7.0, t + 0.1)
        out += tape_lying(13.2, 12.0, t)
    return out


BENCHES = 6


def lipo_bag(x0, z0, y0):
    """A LiPo safety bag with a battery in it: the red fireproof bag, its black seams and velcro flap, the leads out."""
    return [box((x0, y0, z0), (x0 + 2.4, y0 + 0.9, z0 + 1.6), mat("cab_red")),
            box((x0 - 0.04, y0 + 0.4, z0 - 0.04), (x0 + 2.44, y0 + 0.48, z0 + 1.64), mat("grip_black")),
            box((x0 + 0.1, y0 + 0.9, z0 + 0.2), (x0 + 2.3, y0 + 0.96, z0 + 1.0), mat("grip_black")),
            box((x0 + 2.4, y0 + 0.2, z0 + 0.5), (x0 + 3.2, y0 + 0.32, z0 + 0.62), mat("red")), box((x0 + 2.4, y0 + 0.2, z0 + 0.9), (x0 + 3.2, y0 + 0.32, z0 + 1.02), mat("black"))]


# ------------------------------------------------------------------ the test cage (modelled: the mesh cords, the frame,
# the foam tiles and their seams, the pad)
MESH = [k + 1.0 for k in range(0, 16, 2)]           # the cords every 2 pixels (12.5 cm), at the odd pixels: they tile


def net_side():
    """A side's net, on the block's north side (z 0): the vertical cords behind, the horizontal ones in front."""
    c = "grip_black"
    out = [box((x - 0.05, 0, 0.36), (x + 0.05, 16, 0.46), mat(c, skip=("up", "down"))) for x in MESH]
    out += [box((0, y - 0.05, 0.46), (16, y + 0.05, 0.56), mat(c, skip=("east", "west"))) for y in MESH]
    return out


def net_roof():
    c = "grip_black"
    out = [box((0, 15.5, z - 0.05), (16, 15.6, z + 0.05), mat(c, skip=("east", "west"))) for z in MESH]
    out += [box((x - 0.05, 15.6, 0), (x + 0.05, 15.7, 16), mat(c, skip=("north", "south"))) for x in MESH]
    return out


def foam_floor(pad=False):
    """The cage's floor: four interlocking EVA foam tiles (two tones, checkered), the black underlay in their seams, the
    tabs of the jigsaw edges across them; or the landing pad: a raised safety-orange mat with its white H and ring."""
    out = [box((0, 0, 0), (16, 0.1, 16), mat("grip_black"))]
    for i in range(2):
        for j in range(2):
            tone = "foam_a" if (i + j) % 2 == 0 else "foam_b"
            out.append(box((0.1 + i * 8, 0.1, 0.1 + j * 8), (7.9 + i * 8, 0.75, 7.9 + j * 8), mat(tone)))
    for k in (2.0, 6.0, 10.0, 14.0):                 # the jigsaw tabs across the seams
        out += [box((7.8, 0.1, k - 0.5), (8.2, 0.74, k + 0.5), mat("foam_a" if k < 8 else "foam_b")),
                box((k - 0.5, 0.1, 7.8), (k + 0.5, 0.74, 8.2), mat("foam_b" if k < 8 else "foam_a")),
                box((-0.1, 0.1, k - 0.5), (0.1, 0.74, k + 0.5), mat("foam_a" if k < 8 else "foam_b")),
                box((k - 0.5, 0.1, -0.1), (k + 0.5, 0.74, 0.1), mat("foam_a" if k < 8 else "foam_b"))]
    if pad:
        out += [box((1.0, 0.75, 1.0), (15.0, 1.0, 15.0), mat("pad_orange")),
                box((2.0, 1.0, 2.0), (14.0, 1.06, 2.6), mat("white")), box((2.0, 1.0, 13.4), (14.0, 1.06, 14.0), mat("white")),
                box((2.0, 1.0, 2.6), (2.6, 1.06, 13.4), mat("white")), box((13.4, 1.0, 2.6), (14.0, 1.06, 13.4), mat("white")),
                box((5.2, 1.0, 4.6), (6.4, 1.06, 11.4), mat("white")), box((9.6, 1.0, 4.6), (10.8, 1.06, 11.4), mat("white")),
                box((6.4, 1.0, 7.4), (9.6, 1.06, 8.6), mat("white"))]
    return out


def cage_parts():
    """The flight test cage's pieces, one block of it at a time (a multipart blockstate puts them together): the net on a
    side (its rails at the cage's foot and head), the net over it, a corner post, the door (its frame, mesh, hinges,
    lever, the sign; the stack light over it), the foam floor, the pad, the drone parked on it."""
    # (the rails stand 0.2 proud of the block's side, over the floor tiles' tabs, and clear the cords' ends: no face of
    # theirs lies flush with the floor's, a post's or a cord's)
    P = {"net_north": net_side(), "foot_north": [box((0, 0, -0.2), (16, 1.2, 1.0), mat("alu"))],
         "head_north": [box((0, 14.8, -0.2), (16, 16.0, 1.0), mat("alu"))], "roof": net_roof(),
         "post": [box((0, 0, 0), (1.4, 16, 1.4), mat("alu")), box((0.2, 0, 0.2), (1.2, 0.2, 1.2), mat("grip_black"))]}
    door = [box((0.6, 0, -0.1), (1.6, 16, 0.9), mat("alu")), box((14.4, 0, -0.1), (15.4, 16, 0.9), mat("alu"))]
    door += [box(f, t, mat(c, skip=sk)) for f, t, c, sk in
             [((x - 0.05, 0, 0.36), (x + 0.05, 16, 0.46), "grip_black", ("up", "down")) for x in MESH if 1.6 < x < 14.4] +
             [((1.6, y - 0.05, 0.46), (14.4, y + 0.05, 0.56), "grip_black", ("east", "west")) for y in MESH]]
    P["door_lower"] = door + [box((1.6, 0.0, -0.1), (14.4, 1.0, 0.9), mat("alu")),
                              box((0.0, 3.0, 0.0), (0.6, 4.2, 0.8), mat("chrome"))]
    P["door_upper"] = door + [box((1.6, 15.0, -0.1), (14.4, 16.0, 0.9), mat("alu")), box((1.6, 0.0, -0.1), (14.4, 0.8, 0.9), mat("alu")),
                              box((0.0, 10.0, 0.0), (0.6, 11.2, 0.8), mat("chrome")),
                              box((12.6, 0.6, -0.7), (13.0, 1.0, -0.1), mat("grip_black")), box((12.6, 0.6, -0.9), (14.0, 1.0, -0.7), mat("grip_black")),
                              box((4.0, 2.4, -0.2), (12.0, 7.4, -0.1), faces("white", north=A.region("lk_cage_sign", 8.0, 5.0,
                                  sign_plate(["FLIGHT TEST AREA", "DOOR SHUT BEFORE ARMING", "EYE PROTECTION", "ONE PILOT AT A TIME"])))),
                              box((1.2, 16.0, 0.0), (2.4, 16.4, 1.2), mat("grip_black"))]
    for k, c in enumerate(("green", "amber", "red")):
        P["door_upper"].append({**box((1.3, 16.4 + k * 0.7, 0.1), (2.3, 17.1 + k * 0.7, 1.1), mat(c if k == 0 else c + "_off")), "shade": False})
    P["door_upper"].append(box((1.25, 18.5, 0.05), (2.35, 18.8, 1.15), mat("grip_black")))
    P["floor"] = foam_floor()
    P["pad"] = foam_floor(pad=True)
    P["drone"] = parked_drone(8.0, 8.0, 1.0)
    return P


# ------------------------------------------------------------------ the 3D printer farm (enclosed printers)
def printer3d(x0, z0, y0, part, ams=False):
    """An enclosed 3D printer (40 cm cube) at x0, z0, y0, its front to the south: the black body and its glass door and
    lid, inside the bed and the part printing on it, the gantry and the toolhead; the screen and the status strip on
    its front; with ams the filament unit on top, its four spools under a smoked lid."""
    w, d, h = 6.4, 6.4, 6.6
    x1, z1, y1 = x0 + w, z0 + d, y0 + h
    k = "pla_black"
    out = [box((x0, y0, z0), (x1, y0 + 1.0, z1), mat(k)),
           box((x0, y0 + 1.0, z0), (x1, y1, z0 + 0.3), mat(k)),
           box((x0, y0 + 1.0, z0 + 0.3), (x0 + 0.3, y1, z1), mat(k)), box((x1 - 0.3, y0 + 1.0, z0 + 0.3), (x1, y1, z1), mat(k)),
           box((x0 + 0.3, y1 - 0.4, z0 + 0.3), (x1 - 0.3, y1, z0 + 0.8), mat(k)), box((x0 + 0.3, y1 - 0.4, z1 - 0.5), (x1 - 0.3, y1, z1), mat(k)),
           box((x0 + 0.3, y1 - 0.2, z0 + 0.8), (x1 - 0.3, y1 - 0.1, z1 - 0.5), mat("glass")),
           box((x0 + 0.3, y0 + 1.0, z1 - 0.12), (x1 - 0.3, y1 - 0.4, z1 - 0.04), mat("glass")),
           box((x1 - 0.9, y0 + 2.4, z1 - 0.04), (x1 - 0.6, y0 + 4.6, z1 + 0.2), mat("chrome")),
           {**box((x0 + 0.6, y0 + 0.25, z1 - 0.02), (x0 + 3.0, y0 + 0.85, z1 + 0.06), faces("black", south=SCR.face("lk_3dp", 2.4, 0.6, printer_screen))), "shade": False},
           {**box((x0 + 0.3, y1 - 0.55, z1 - 0.5), (x1 - 0.3, y1 - 0.45, z1 - 0.3), mat("light")), "shade": False},
           box((x0 + 0.9, y0 + 1.6, z0 + 0.9), (x1 - 0.9, y0 + 1.85, z1 - 0.9), mat("steel")),
           box((x0 + 0.3, y0 + 4.6, z0 + 3.0), (x1 - 0.3, y0 + 4.85, z0 + 3.4), mat("carbon")),
           box((x0 + 2.6, y0 + 4.0, z0 + 2.6), (x0 + 3.8, y0 + 5.2, z0 + 3.8), mat(k)),
           box((x0 + 2.65, y0 + 4.4, z0 + 3.8), (x0 + 3.75, y0 + 5.0, z0 + 3.85), mat("pla_orange")),
           box((x0 + 3.05, y0 + 3.7, z0 + 3.05), (x0 + 3.35, y0 + 4.0, z0 + 3.35), mat("copper"))]
    b = y0 + 1.85
    if part == "arm":
        out.append(box((x0 + 1.4, b, z0 + 2.8), (x1 - 1.4, b + 0.5, z0 + 3.6), mat("pla_orange")))
    elif part == "shell":
        out += round_part(x0 + 3.2, z0 + 3.2, 1.3, b, b + 1.1, "pla_grey", n=4)
    else:
        out += [box((x0 + 1.6, b, z0 + 1.6), (x0 + 2.4, b + 1.4, z0 + 2.4), mat("pla_orange")), box((x0 + 3.9, b, z0 + 3.9), (x0 + 4.7, b + 1.4, z0 + 4.7), mat("pla_orange")),
                box((x0 + 3.9, b, z0 + 1.6), (x0 + 4.7, b + 0.7, z0 + 2.4), mat("pla_orange"))]
    if ams:
        out += [box((x0, y1, z0 + 0.4), (x1, y1 + 1.6, z1 - 0.4), mat(k)),
                box((x0 + 0.3, y1 + 1.6, z0 + 0.7), (x1 - 0.3, y1 + 1.7, z1 - 0.7), mat("glass"))]
        for s, c in enumerate(("spool_blue", "pla_orange", "white", "pla_black")):
            out += swap_yz(round_part(x0 + 0.95 + s * 1.5, y1 + 1.1, 0.7, z0 + 1.2, z1 - 1.2, c, n=4))
    return out


def printer_farm(half):
    """Two blocks high: a steel rack of three shelves, two enclosed printers on each (the top ones with their filament
    units), boxes of filament on the top shelf."""
    out = [box((0.2, 0, 1.0), (1.0, 16, 1.8), mat("steel")), box((15.0, 0, 1.0), (15.8, 16, 1.8), mat("steel")),
           box((0.2, 0, 14.2), (1.0, 16, 15.0), mat("steel")), box((15.0, 0, 14.2), (15.8, 16, 15.0), mat("steel"))]
    if half == "lower":
        out += [box((1.0, 0.6, 1.0), (15.0, 1.2, 15.0), mat("steel")), box((1.0, 8.2, 1.0), (15.0, 8.8, 15.0), mat("steel"))]
        out += printer3d(1.3, 4.6, 1.2, "arm") + printer3d(8.3, 4.6, 1.2, "parts")
        out += printer3d(1.3, 4.6, 8.8, "shell") + printer3d(8.3, 4.6, 8.8, "arm")
    else:
        out += [box((1.0, 0.0, 1.0), (15.0, 0.6, 15.0), mat("steel")), box((1.0, 15.2, 1.0), (15.0, 15.8, 15.0), mat("steel"))]
        out += printer3d(1.3, 4.6, 0.6, "parts", ams=True) + printer3d(8.3, 4.6, 0.6, "shell", ams=True)
        out += [box((1.6, 15.8, 3.0), (7.4, 19.4, 12.0), mat("cardboard")), box((8.0, 15.8, 4.0), (13.2, 18.6, 12.0), mat("cardboard"))]
    return out


# ------------------------------------------------------------------ the electronics bench (real sizes)
def eb_top(v):
    """What a place on the electronics bench has (v), at real sizes: the soldering station, its stand and the fume
    extractor, a board under repair; the oscilloscope and the bench supply on the shelf, a board wired to them; the
    magnifier lamp over a board and the parts cabinet; a multimeter and its probes, a laptop."""
    t = EB_TOP
    s = t + 7.0
    out = []
    if v == 0:
        out += [box((1.6, t + 0.1, 7.6), (4.2, t + 1.7, 10.4), mat("grip_black")),
                box((1.8, t + 0.5, 10.4), (4.0, t + 1.4, 10.5), faces("black", south=SCR.face("lk_solder", 2.2, 0.9, label("350°C", (12, 8, 6), (255, 70, 40), live={"src": "value", "base": 350, "spread": 3, "period": 8, "unit": "°C"})))),
                box((4.8, t + 0.1, 8.2), (6.6, t + 0.4, 10.0), mat("grip_black")), box((5.1, t + 0.4, 8.5), (6.3, t + 0.6, 9.7), mat("bin_yellow"))]
        out += swap_yz(round_part(5.7, t + 1.3, 0.32, 8.4, 10.2, "chrome", n=3))
        out += [box((5.5, t + 1.1, 10.2), (5.9, t + 1.5, 11.6), mat("bin_blue")),
                box((9.6, t + 0.1, 6.0), (12.8, t + 2.8, 8.6), mat("cab_grey")), box((9.9, t + 0.4, 8.6), (12.5, t + 2.5, 8.7), mat("steel")),
                box((7.0, t + 0.1, 10.0), (9.4, t + 0.3, 12.0), mat("green_pcb")), box((7.8, t + 0.3, 10.6), (8.6, t + 0.5, 11.4), mat("black"))]
        out += [box((1.0, s, 2.0), (5.0, s + 1.4, 5.0), mat("cardboard")), box((8.4, s, 1.8), (13.2, s + 4.8, 4.2), mat("cab_grey"))]
        for k in range(4):
            for j in range(4):
                out.append(box((8.6 + j * 1.15, s + 0.3 + k * 1.1, 4.2), (9.6 + j * 1.15, s + 1.1 + k * 1.1, 4.4), mat(("bin_blue", "bin_red", "bin_yellow", "bin_blue")[k])))
    elif v == 1:
        out += [box((1.0, s, 2.2), (6.6, s + 3.0, 4.8), mat("cab_grey")),
                {**box((1.4, s + 0.6, 4.8), (4.6, s + 2.7, 4.9), faces("black", south=SCR.face("lk_scope", 3.2, 2.1, scope_screen))), "shade": False}]
        for k in range(3):
            out += knob_s(5.6, s + 2.3 - k * 0.75, 0.25, 4.8)
        out += [box((8.4, s, 1.0), (11.8, s + 2.4, 5.0), mat("cab_grey")),
                {**box((8.7, s + 1.2, 5.0), (11.5, s + 2.1, 5.1), faces("black", south=SCR.face("lk_psu", 2.8, 0.9, psu_screen))), "shade": False},
                box((8.9, s + 0.3, 5.0), (9.3, s + 0.7, 5.3), mat("red")), box((9.7, s + 0.3, 5.0), (10.1, s + 0.7, 5.3), mat("black"))]
        out += [box((5.4, t + 0.1, 9.0), (7.6, t + 0.3, 11.0), mat("green_pcb")), box((6.1, t + 0.3, 9.7), (6.9, t + 0.5, 10.5), mat("black")),
                box((7.6, t + 0.1, 9.6), (9.3, t + 0.22, 9.72), mat("red")), box((9.2, t + 0.22, 5.2), (9.32, t + 0.34, 9.72), mat("red")),
                box((7.6, t + 0.1, 10.1), (9.9, t + 0.22, 10.22), mat("black")), box((9.8, t + 0.22, 5.2), (9.92, t + 0.34, 10.22), mat("black"))]
    elif v == 2:
        out += [box((2.0, t + 0.1, 2.6), (4.0, t + 0.5, 4.6), mat("grip_black"))]
        out += round_part(3.0, 3.6, 0.2, t + 0.5, t + 5.0, "chrome")
        arm = {"angle": 22.5, "axis": "x", "origin": [3.0, t + 5.0, 3.6]}
        out += [box((2.8, t + 4.8, 3.4), (3.2, t + 5.2, 9.0), mat("chrome"), arm)]
        ring = round_ring(3.0, 9.6, 1.5, 0.32, t + 2.2, t + 2.7, "white", 0.0, "white", n=6)
        out += [e for e in ring if e["to"][1] - e["from"][1] > 0.1]
        out += [box((1.8, t + 2.4, 8.4), (4.2, t + 2.5, 10.8), mat("shield"))]
        out += [box((2.0, t + 0.1, 8.8), (4.0, t + 0.3, 10.4), mat("green_pcb")), box((2.7, t + 0.3, 9.3), (3.3, t + 0.45, 9.9), mat("black"))]
        out += [box((9.0, t + 0.1, 3.0), (13.8, t + 4.9, 5.4), mat("cab_grey"))]
        for k in range(5):
            for j in range(4):
                out += [box((9.2 + j * 1.15, t + 0.35 + k * 0.9, 5.4), (10.25 + j * 1.15, t + 1.1 + k * 0.9, 5.55), mat("shield")),
                        box((9.45 + j * 1.15, t + 0.5 + k * 0.9, 5.2), (10.0 + j * 1.15, t + 0.9 + k * 0.9, 5.4), mat(("bin_red", "bin_blue", "bin_yellow", "green_pcb")[(k + j) % 4]))]
    else:
        out += [box((3.0, t + 0.1, 8.0), (4.6, t + 0.6, 10.9), mat("bin_yellow")),
                box((3.15, t + 0.6, 8.2), (4.45, t + 0.65, 9.2), faces("black", up=SCR.face("lk_meter", 1.3, 1.0, label("12.43", (150, 170, 150), (20, 30, 20), live={"src": "value", "base": 12.43, "spread": 0.04, "dp": 2, "period": 5})))),
                box((3.3, t + 0.1, 10.9), (3.42, t + 0.22, 13.4), mat("red")), box((4.18, t + 0.1, 10.9), (4.3, t + 0.22, 13.4), mat("black"))]
        out += laptop_at(8.2, 5.4, t + 0.1, "lk_lap_fc", cad_screen("board"), w=6.0, d=4.0, lid=3.8, kind="silver")
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **MATERIALS}
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = TK.Atlas(1024, TX, "atlas")
    SCR = TK.Atlas(2048, 4 * TX, "screens")
    TK.SCR = SCR
    MK.A, MK.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("lk_atlas"), "screens": ref("lk_screens"), "particle": ref("lk_beech")})

    def model(name, elements):
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, obj):
        (bs_dir / f"{name}.json").write_text(json.dumps(obj, indent=1) + "\n")

    def faced(model_name, extra=""):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": y} if y else {})} for f, y in FACINGS.items()}

    def row_piece(name, body, ends, tops):
        model(name, body)
        model(f"{name}_left", ends(1.0))
        model(f"{name}_right", ends(15.0))
        parts = []
        for f, y in FACINGS.items():
            yy = {"y": y} if y else {}
            parts += [{"when": {"facing": f}, "apply": {"model": ref(name), **yy}},
                      {"when": {"facing": f, "left": "false"}, "apply": {"model": ref(f"{name}_left"), **yy}},
                      {"when": {"facing": f, "right": "false"}, "apply": {"model": ref(f"{name}_right"), **yy}}]
            for v, els in enumerate(tops):
                if els:
                    parts.append({"when": {"facing": f, "variant": str(v)}, "apply": {"model": ref(f"{name}_top_{v}"), **yy}})
        for v, els in enumerate(tops):
            if els:
                model(f"{name}_top_{v}", els)
        states(name, {"multipart": parts})

    # the workbench: its frame at both edges of every block (a row reads as one bench), its things a variant
    model("lab_workbench", workbench_body())
    v = {}
    tops = [bench_top(k) for k in range(BENCHES)]
    parts = []
    for f, y in FACINGS.items():
        yy = {"y": y} if y else {}
        parts.append({"when": {"facing": f}, "apply": {"model": ref("lab_workbench"), **yy}})
        for k in range(BENCHES):
            parts.append({"when": {"facing": f, "variant": str(k)}, "apply": {"model": ref(f"lab_workbench_top_{k}"), **yy}})
    for k, els in enumerate(tops):
        model(f"lab_workbench_top_{k}", els)
    states("lab_workbench", {"multipart": parts})
    v = {}
    for k in range(4):
        model(f"lab_pegboard_{k}", pegboard(k))
        v.update(faced(f"lab_pegboard_{k}", f",variant={k}"))
    states("lab_pegboard", {"variants": v})
    v = {}
    for colour in ("red", "grey"):
        model(f"lab_tool_cabinet_{colour}", tool_cabinet(colour))
        v.update(faced(f"lab_tool_cabinet_{colour}", f",colour={colour}"))
    states("lab_tool_cabinet", {"variants": v})
    row_piece("cad_desk", cad_body(), cad_end, [cad_top(k) for k in range(len(CADS) + 2)])
    row_piece("electronics_bench", eb_body(), cad_end, [eb_top(k) for k in range(EBS)])
    v = {}
    for k, sw in enumerate((0.0, 22.5, -22.5)):
        model(f"task_chair_{k}", task_chair(sw))
        v.update(faced(f"task_chair_{k}", f",variant={k}"))
    states("task_chair", {"variants": v})
    model("bench_stool", bench_stool())
    states("bench_stool", {"variants": {"": {"model": ref("bench_stool")}}})
    # the test cage: one block of it, its sides' nets, the net over it, its posts, the door, the floor (multipart)
    P = cage_parts()
    for k, els in P.items():
        model(f"test_cage_{k}", els)
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    parts = []
    for side, y in rot.items():
        yy = {"y": y} if y else {}
        parts += [{"when": {side: "net"}, "apply": {"model": ref("test_cage_net_north"), **yy}},
                  {"when": {side: "door", "part": "bottom"}, "apply": {"model": ref("test_cage_door_lower"), **yy}},
                  {"when": {side: "door", "part": "middle"}, "apply": {"model": ref("test_cage_door_upper"), **yy}},
                  {"when": {side: "net|door", "part": "bottom"}, "apply": {"model": ref("test_cage_foot_north"), **yy}},
                  {"when": {side: "net|door", "part": "top"}, "apply": {"model": ref("test_cage_head_north"), **yy}}]
    for corner, y in (("north_west", 0), ("north_east", 90), ("south_east", 180), ("south_west", 270)):
        a, b = corner.split("_")
        parts.append({"when": {"AND": [{a: "net|door"}, {b: "net|door"}]}, "apply": {"model": ref("test_cage_post"), **({"y": y} if y else {})}})
    parts += [{"when": {"part": "top"}, "apply": {"model": ref("test_cage_roof")}},
              {"when": {"floor": "foam"}, "apply": {"model": ref("test_cage_floor")}},
              {"when": {"floor": "pad|drone"}, "apply": {"model": ref("test_cage_pad")}},
              {"when": {"floor": "drone"}, "apply": {"model": ref("test_cage_drone")}}]
    states("test_cage", {"multipart": parts})
    for name, build in (("printer_farm", printer_farm), ("lipo_cabinet", lipo_cabinet)):
        v = {}
        for half in ("lower", "upper"):
            model(f"{name}_{half}", build(half))
            v.update(faced(f"{name}_{half}", f",half={half}"))
        states(name, {"variants": v})
    model("thrust_test_stand", thrust_stand())
    states("thrust_test_stand", {"variants": faced("thrust_test_stand")})
    v = {}
    for k in range(RACKS):
        for half in ("lower", "upper"):
            model(f"parts_rack_{half}_{k}", parts_rack(half, k))
            v.update(faced(f"parts_rack_{half}_{k}", f",half={half},variant={k}"))
    states("parts_rack", {"variants": v})
    for name, build, n in (("lab_plotter", plotter, 2), ("plan_chest", plan_chest, 2)):
        v = {}
        for p, els in enumerate(cut_parts(build(), n)):
            model(f"{name}_{p}", els)
            v.update(faced(f"{name}_{p}", f",part={p}"))
        states(name, {"variants": v})
    v = {}
    for half in ("lower", "upper"):
        for p, els in enumerate(cut_parts(whiteboard(half), 2)):
            model(f"rolling_whiteboard_{half}_{p}", els)
            v.update(faced(f"rolling_whiteboard_{half}_{p}", f",half={half},part={p}"))
    states("rolling_whiteboard", {"variants": v})
    v = {}
    for k in range(3):
        model(f"flight_case_{k}", flight_case(k))
        v.update(faced(f"flight_case_{k}", f",variant={k}"))
    states("flight_case", {"variants": v})
    A.img.save(tex_dir / "lk_atlas.png")
    SCR.img.save(tex_dir / "lk_screens.png")
    return A.y + A.row, SCR.y + SCR.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
