"""The tower's first aid room and the crew rest room (work list item 22 part B, room 4), to the standard Justin set on
the lab (take 2): everything at its real size (a pixel is 6.25 cm), modelled rather than painted on, the screens live
in game, materials at one density and seamless, things varied.

First aid: exam_couch (2 long), privacy_screen, iv_pole, oxygen_trolley, crash_cart, scrub_sink
(2 x 1, two high), med_fridge, medicine_cupboard, first_aid_box, eyewash_bottles, spine_board, wheelchair, med_desk
(2 x 1), pedal_bin, dressing_trolley, diagnostic_set, column_scale (two high), step_stool, med_poster. Crew rest: recliner, crew_side_table, crew_sofa (joins), crew_coffee_table (2 x 1), wall_tv
(2 x 1), kitchenette (3 x 1, two high), crew_fridge, crew_locker, crew_bunk (2 long, two high), water_dispenser.
Pieces taller than a block are built whole and cut into halves (half=lower|upper); pieces longer than a block into
parts (part=0..).
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
import lab_kit as LK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, ORANGE, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None
SCR = None
MONO = MK.MONO
swap_xy, rod_x = LK.swap_xy, LK.rod_x


# ------------------------------------------------------------------ materials
def tiles(img_size=256):
    """Subway tiles 25 by 12.5 cm (4 by 2 pixels), their rows offset by half a tile, the grout lines."""
    n = img_size
    a = np.stack([np.full((n, n), c, float) for c in (240, 242, 240)], -1) + TK._noise(3401, 1.5)[..., None]
    for r in range(8):
        y0 = r * 32
        a[y0:y0 + 2] = (176, 178, 176)
        off = 32 if r % 2 else 0
        for k in range(5):
            x = (off + k * 64) % n
            a[y0:y0 + 32, x:x + 2] = (176, 178, 176)
    return TK._img(a)


def coil(base=(34, 34, 38)):
    """A coiled cord: dark ridges a quarter pixel apart across it (the cord is hung up and down)."""
    a = np.stack([np.full((256, 256), c, float) for c in base], -1) + TK._noise(3441, 1.0)[..., None]
    a[(np.arange(256) % 4) < 2] *= 1.9
    return TK._img(a)


def quilt(base, seed):
    """A quilted mattress cover: a diamond stitch every 2 pixels."""
    y, x = np.mgrid[0:256, 0:256]
    st = ((np.abs(((x + y) % 32) - 16) < 1) | (np.abs(((x - y) % 32) - 16) < 1))
    a = np.stack([np.full((256, 256), c, float) for c in base], -1) + TK._noise(seed, 1.5)[..., None]
    a[st] *= 0.9
    return TK._img(a)


def speckle(base, seed):
    """A stone-effect worktop: grey with light and dark speckles."""
    a = np.stack([np.full((256, 256), c, float) for c in base], -1) + TK._noise(seed, 2)[..., None]
    rng = np.random.default_rng(seed + 1)
    a[rng.random((256, 256)) < 0.05] *= 0.8
    a[rng.random((256, 256)) < 0.04] *= 1.15
    return TK._img(a)


MATERIALS = {
    "rk_vinyl": lambda: MK.leather((44, 92, 150), 3410, grain=1.2), "rk_wsteel": lambda: TK.solid((234, 236, 238), 3411, 1.2),
    "rk_screen_fab": lambda: MK.weave((150, 194, 196), 3412, 1, 4), "rk_stainless": lambda: TK.brushed((168, 172, 176), 3413, 3), "rk_couch_paper": lambda: MK.weave((226, 226, 220), 3437, 1, 2), "rk_ceramic": lambda: TK.solid((200, 204, 208), 3438, 1.0),
    "rk_ceramic_mid": lambda: TK.solid((182, 188, 192), 3439, 1.0), "rk_ceramic_deep": lambda: TK.solid((160, 166, 172), 3440, 1.0),
    "rk_coil": lambda: coil(), "rk_boot": lambda: MK.leather((120, 76, 42), 3442, grain=1.6), "rk_boot_dark": lambda: MK.leather((70, 44, 28), 3443, grain=1.4),
    "rk_worktop": lambda: speckle((150, 152, 154), 3414), "rk_tile": lambda: tiles(), "rk_mattress": lambda: quilt((236, 236, 232), 3415),
    "rk_sheet": lambda: MK.weave((244, 244, 240), 3416, 1, 2), "rk_blanket": lambda: MK.weave((38, 54, 92), 3417, 2, 6),
    "rk_blanket_red": lambda: MK.weave((150, 44, 40), 3418, 2, 6), "rk_locker": lambda: TK.solid((82, 98, 118), 3419, 1.4),
    "rk_sofa": lambda: MK.weave((92, 100, 110), 3420, 2, 6), "rk_recliner": lambda: MK.leather((104, 64, 40), 3421, grain=1.6),
    "rk_fridge": lambda: TK.solid((242, 242, 240), 3422, 1.0), "rk_cyl": lambda: TK.solid((40, 120, 72), 3423, 1.5),
    "rk_bin_yellow": lambda: TK.solid((238, 198, 28), 3424, 1.5), "rk_aid_green": lambda: TK.solid((28, 136, 70), 3425, 1.5),
    "rk_board_orange": lambda: TK.solid((236, 108, 30), 3426, 1.5), "rk_glove_blue": lambda: TK.solid((60, 110, 200), 3427, 1.5),
    "rk_glove_purple": lambda: TK.solid((120, 70, 170), 3428, 1.5), "rk_glove_green": lambda: TK.solid((60, 160, 90), 3429, 1.5),
    "rk_saline": lambda: MK.rgba((236, 240, 244), 110, 3430), "rk_water": lambda: MK.rgba((120, 176, 228), 130, 3431),
    "rk_curtain": lambda: MK.weave((70, 82, 104), 3432, 1, 5), "rk_pillow": lambda: MK.weave((246, 246, 244), 3433, 1, 2),
    "rk_cart_red": lambda: TK.solid((186, 32, 30), 3434, 1.4), "rk_sling": lambda: MK.weave((36, 36, 40), 3435, 1, 4),
    "rk_lampshade": lambda: MK.weave((236, 226, 200), 3436, 1, 3)}


# ------------------------------------------------------------------ pictures
_fit = MK._text_fit


def weather(img):
    """The crew rest TV: the tower's weather (the land round the tower, the river, a band of rain drifting over it, the
    wind), the header and the ticker."""
    W, H = img.size
    rng = np.random.default_rng(3440)
    y, x = np.mgrid[0:H, 0:W]
    a = np.zeros((H, W, 3), float)
    a[:] = (70, 112, 64)
    n = sum(np.sin(x / W * k * 6.3 + rng.uniform(0, 6)) * np.cos(y / H * k * 5.1 + rng.uniform(0, 6)) / k for k in range(1, 6))
    a += (n * 14)[..., None]
    river = np.abs(y - (H * 0.6 + np.sin(x / W * 7) * H * 0.12)) < H * 0.02
    a[river] = (60, 110, 170)
    rain = np.clip(1 - (((x - W * 0.62) / (W * 0.25)) ** 2 + ((y - H * 0.42) / (H * 0.22)) ** 2), 0, 1)
    rain *= (0.6 + 0.4 * np.sin(x / 9.0) * np.cos(y / 7.0))
    col = np.where((rain > 0.5)[..., None], np.array([240, 220, 60], float), np.array([60, 200, 90], float))
    a = a * (1 - (rain > 0.15)[..., None] * 0.7) + col * (rain > 0.15)[..., None] * 0.7
    img.paste(TK._img(a), (0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H * 0.12), fill=(16, 30, 60))
    d.text((W * 0.02, H * 0.06), "TOWER WEATHER", font=ImageFont.truetype(FONT, round(H * 0.07)), fill=(255, 255, 255), anchor="lm")
    d.text((W * 0.98, H * 0.06), "14°C  WIND SW 12 km/h", font=ImageFont.truetype(FONT, round(H * 0.06)), fill=(255, 220, 120), anchor="rm")
    cx, cy = W * 0.4, H * 0.55
    d.polygon([(cx, cy - H * 0.06), (cx - H * 0.03, cy + H * 0.02), (cx + H * 0.03, cy + H * 0.02)], fill=(232, 150, 40))
    d.rectangle((0, H * 0.9, W, H), fill=(16, 30, 60))
    d.text((W * 0.02, H * 0.95), "FLIGHTS NORMAL  ·  RAIN BAND EAST BY 16:00  ·  T4 WING FLIGHTS HELD IN RAIN", font=ImageFont.truetype(FONT, round(H * 0.045)), fill=(236, 236, 240), anchor="lm")
    return img


def patient_log(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(10, 10, 12))
    b = round(W * 0.025)
    d.rectangle((b, b, W - b, H - round(W * 0.05)), fill=(236, 240, 244))
    d.rectangle((b, b, W - b, b + H * 0.1), fill=(28, 120, 70))
    d.text((b + W * 0.02, b + H * 0.05), "FIRST AID LOG", font=ImageFont.truetype(FONT, round(H * 0.06)), fill=(255, 255, 255), anchor="lm")
    rows = [("08:12", "REYES", "Cut, left hand. Dressed."), ("10:40", "BRANDT", "Headache. Water, rest."), ("13:05", "SILVA", "Eye: dust. Eye wash."),
            ("14:20", "PETROV", "Burn (soldering). Cooled."), ("", "", "")]
    f = ImageFont.truetype(FONT_R, round(H * 0.045))
    for k, (t, n, e) in enumerate(rows):
        yy = b + H * (0.16 + k * 0.1)
        d.text((b + W * 0.03, yy), t, font=f, fill=(40, 40, 50))
        d.text((b + W * 0.15, yy), n, font=f, fill=(40, 40, 50))
        d.text((b + W * 0.32, yy), e, font=f, fill=(40, 40, 50))
        d.line((b + W * 0.02, yy + H * 0.075, W - b - W * 0.02, yy + H * 0.075), fill=(200, 206, 212))
    return img


def sign(lines, band=(28, 136, 70), icon=None):
    """A sign plate: the coloured band with its title, the lines under it, an icon drawn simply (a cross, an eye, hands,
    a flame crossed out, the biohazard)."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 246))
        d.rectangle((0, 0, W, H * 0.34), fill=band)
        d.text((W / 2, H * 0.17), lines[0], font=_fit(d, lines[0], FONT, round(H * 0.2), W - 8), fill=(255, 255, 255), anchor="mm")
        y = H * 0.48
        for t in lines[1:]:
            d.text((W / 2, y), t, font=_fit(d, t, FONT, round(H * 0.13), W - 10), fill=(30, 30, 34), anchor="mm")
            y += H * 0.17
        return img
    return draw


def label_plate(text, bg=(244, 242, 232), fg=(30, 30, 34), live=None):
    return LK.label(text, bg, fg, live=live)


def game_lid(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(170, 40, 36))
    d.rectangle((W * 0.08, H * 0.1, W * 0.92, H * 0.9), outline=(250, 220, 120), width=2)
    d.text((W / 2, H * 0.4), "SKY ROUTES", font=_fit(d, "SKY ROUTES", FONT, round(H * 0.2), W * 0.8), fill=(250, 230, 140), anchor="mm")
    d.text((W / 2, H * 0.66), "the board game", font=_fit(d, "the board game", FONT_R, round(H * 0.1), W * 0.8), fill=(250, 240, 220), anchor="mm")
    return img


def gauge(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(40, 42, 46))
    d.ellipse((W * 0.06, H * 0.06, W * 0.94, H * 0.94), fill=(246, 246, 242))
    for k in range(11):
        a = math.radians(225 - k * 27)
        d.line((W / 2 + math.cos(a) * W * 0.36, H / 2 - math.sin(a) * H * 0.36, W / 2 + math.cos(a) * W * 0.42, H / 2 - math.sin(a) * H * 0.42), fill=(30, 30, 30), width=1)
    a = math.radians(225 - 7.5 * 27)
    d.line((W / 2, H / 2, W / 2 + math.cos(a) * W * 0.34, H / 2 - math.sin(a) * H * 0.34), fill=(200, 30, 30), width=2)
    d.arc((W * 0.14, H * 0.14, W * 0.86, H * 0.86), -45 - 30, -45, fill=(40, 170, 70), width=3)
    return img


def name_card(name):
    return LK.label(name, (246, 244, 236), (30, 30, 34))


# ------------------------------------------------------------------ cutting tall and long pieces
def cut_y(elements, n=2):
    """Cuts a model built n blocks tall into its n half models (each moved to 0..16): an element across a block's top is
    cut there, the faces on the cut left out; its default UVs (read by position) carry on."""
    out = [[] for _ in range(n)]
    for e in elements:
        y0, y1 = e["from"][1], e["to"][1]
        for p in range(n):
            a, b = max(y0, p * 16), min(y1, (p + 1) * 16)
            if b - a < 1e-6:
                continue
            q = json.loads(json.dumps(e))
            q["from"][1], q["to"][1] = round(a - p * 16, 4), round(b - p * 16, 4)
            if "rotation" in q:
                q["rotation"]["origin"][1] = round(q["rotation"]["origin"][1] - p * 16, 4)
            fc = q["faces"]
            for k in ("north", "south", "east", "west"):
                uv = fc.get(k, {}).get("uv")
                if uv and (a > y0 + 1e-6 or b < y1 - 1e-6):
                    sv = (uv[3] - uv[1]) / (y1 - y0)
                    fc[k]["uv"] = [uv[0], round(uv[1] + (y1 - b) * sv, 5), uv[2], round(uv[1] + (y1 - a) * sv, 5)]
            if a > y0 + 1e-6:
                fc.pop("down", None)
            if b < y1 - 1e-6:
                fc.pop("up", None)
            out[p].append(q)
    return out


def cut_z(elements, n=2):
    """Cuts a model built n blocks long along z into its parts (each moved to 0..16), as cut_y."""
    out = [[] for _ in range(n)]
    for e in elements:
        z0, z1 = e["from"][2], e["to"][2]
        for p in range(n):
            a, b = max(z0, p * 16), min(z1, (p + 1) * 16)
            if b - a < 1e-6:
                continue
            q = json.loads(json.dumps(e))
            q["from"][2], q["to"][2] = round(a - p * 16, 4), round(b - p * 16, 4)
            if "rotation" in q:
                q["rotation"]["origin"][2] = round(q["rotation"]["origin"][2] - p * 16, 4)
            fc = q["faces"]
            assert not any(fc.get(k, {}).get("uv") for k in ("east", "west", "up", "down")) or (a == z0 and b == z1), "a picture across a cut"
            if a > z0 + 1e-6:
                fc.pop("north", None)
            if b < z1 - 1e-6:
                fc.pop("south", None)
            out[p].append(q)
    return out


def star_base():
    """A five-legged rolling base (as the office chairs'), its hub at 8, 8."""
    return MK.exec_chair(0.0)[:11]


# ------------------------------------------------------------------ first aid: the couch, the screen, the stands
def iv_pole():
    """The IV pole: its base, the chrome pole, the four hooks at its top, a saline bag on one, its drip chamber and the
    line coiled over a hook."""
    out = star_base() + round_part(8, 8, 0.28, 2.0, 30.0, "chrome")
    out += [box((4.4, 29.6, 7.85), (11.6, 29.9, 8.15), mat("chrome")), box((7.85, 29.6, 4.4), (8.15, 29.9, 11.6), mat("chrome"))]
    for (x, z) in ((4.4, 8.0), (11.6, 8.0), (8.0, 4.4), (8.0, 11.6)):
        out.append(box((x - 0.15, 29.9, z - 0.15), (x + 0.15, 30.6, z + 0.15), mat("chrome")))
    out += [box((3.4, 25.6, 7.5), (5.4, 29.4, 8.5), mat("saline")), box((4.2, 29.4, 7.9), (4.6, 29.9, 8.1), mat("chrome")),
            box((3.9, 26.6, 7.48), (4.9, 28.0, 7.5), mat("white")),
            box((4.25, 24.2, 7.85), (4.55, 25.6, 8.15), mat("saline")), box((4.35, 18.0, 7.95), (4.45, 24.2, 8.05), mat("saline"))]
    return out


def oxygen_trolley():
    """The oxygen trolley: its two wheels and base plate, the frame and handle, the cylinder (green, its white shoulder,
    the valve), the regulator with its gauge, the mask and tubing hung from the handle."""
    out = rod_x(3.6, 4.4, 1.4, 4.6, 1.4, "grip_black", n=4) + rod_x(11.6, 12.4, 1.4, 4.6, 1.4, "grip_black", n=4)
    out += rod_x(4.4, 11.6, 1.4, 4.6, 0.2, "chrome")
    out += [box((5.0, 1.0, 5.6), (11.0, 1.4, 10.6), mat("steel")),
            box((5.0, 1.0, 9.8), (5.6, 18.0, 10.4), mat("steel")), box((10.4, 1.0, 9.8), (11.0, 18.0, 10.4), mat("steel")),
            box((5.0, 18.0, 9.8), (11.0, 18.6, 10.4), mat("steel")), box((5.6, 8.0, 9.8), (10.4, 8.4, 10.4), mat("steel")),
            box((6.0, 17.6, 10.4), (10.0, 18.6, 11.0), mat("grip_black"))]
    out += round_part(8, 7.6, 1.0, 1.4, 12.0, "cyl", n=4) + round_part(8, 7.6, 0.85, 12.0, 13.0, "white", n=4)
    out += round_part(8, 7.6, 0.35, 13.0, 14.0, "chrome") + [box((7.4, 14.0, 7.0), (8.6, 15.2, 8.2), mat("chrome"))]
    out += [box((7.6, 14.2, 6.0), (8.4, 15.0, 7.0), mat("chrome")),
            box((7.4, 14.2, 5.7), (8.6, 15.4, 6.0), faces("steel", north=A.region("rk_gauge", 1.2, 1.2, gauge))),
            box((5.4, 6.0, 8.4), (7.4, 9.0, 8.6), faces("white", north=A.region("rk_o2", 2.0, 3.0, sign(["OXYGEN", "NO OIL", "NO FLAMES"], (40, 120, 72)))))]
    out += [box((9.8, 15.6, 10.6), (10.0, 18.0, 10.8), mat("saline")), box((9.4, 14.4, 10.5), (10.6, 15.6, 11.2), mat("saline")),
            box((9.6, 14.0, 10.7), (10.4, 14.4, 11.0), mat("grip_black"))]
    return out


# ------------------------------------------------------------------ the crash cart
# ------------------------------------------------------------------ the sink, the fridge, the cupboards
def med_fridge():
    """The medicines fridge (60 cm wide, 85 cm tall) under the counter line, its front to the north: the white body, the
    glass door in its frame and its handle and lock, the shelves behind it with boxes and vials, the temperature display,
    the MEDICINES ONLY label."""
    out = [box((3.2, 0, 2.0), (12.8, 0.8, 14.0), mat("black")),
           box((3.0, 0.8, 1.4), (13.0, 13.6, 14.4), mat("fridge")),
           box((3.0, 13.6, 1.4), (13.0, 14.0, 14.4), mat("fridge"))]
    out += [box((3.0, 0.8, 1.0), (3.6, 13.6, 1.4), mat("fridge")), box((12.4, 0.8, 1.0), (13.0, 13.6, 1.4), mat("fridge")),
            box((3.6, 0.8, 1.0), (12.4, 1.6, 1.4), mat("fridge")), box((3.6, 12.2, 1.0), (12.4, 13.6, 1.4), mat("fridge")),
            box((3.6, 1.6, 1.15), (12.4, 12.2, 1.25), mat("glass")),
            box((11.4, 4.0, 0.6), (11.8, 10.0, 1.0), mat("chrome")), box((4.0, 12.6, 0.9), (6.6, 13.3, 1.0), faces("black", north=SCR.face("rk_ftemp", 2.6, 0.7, label_plate("4.2°C", (12, 8, 6), (255, 70, 40), live={"src": "value", "base": 4.2, "spread": 0.35, "dp": 1, "period": 150, "unit": "°C"})))),
            box((7.0, 12.5, 0.95), (11.6, 13.4, 1.0), faces("white", north=A.region("rk_fridge_lab", 4.6, 0.9, label_plate("MEDICINES ONLY  2-8°C"))))]
    for y0 in (4.6, 8.4):
        out.append(box((3.6, y0, 1.6), (12.4, y0 + 0.2, 14.0), mat("chrome")))
    rng = random.Random(3460)
    for y0 in (1.6, 4.8, 8.6):
        x = 4.0
        while x < 11.6:
            w = rng.uniform(1.2, 2.4)
            c = rng.choice(("white", "glove_blue", "glove_green", "red"))
            out.append(box((x, y0, 3.0), (min(x + w, 12.2), y0 + rng.uniform(1.4, 2.8), 9.0), mat(c)))
            x += w + 0.2
    return out


def first_aid_box():
    """The wall first aid box (40 by 34 by 15 cm), its front to the north: the green case, the white cross raised on its
    lid, the hinge and the two latches, the carry handle, its label."""
    g = "aid_green"
    out = [box((1.6, 3.0, 13.6), (14.4, 13.0, 16.0), mat(g)),
           box((1.6, 3.0, 13.2), (14.4, 13.0, 13.6), mat(g)),
           box((6.8, 6.0, 13.0), (9.2, 11.4, 13.2), mat("white")), box((5.4, 7.6, 13.0), (10.6, 9.8, 13.2), mat("white")),
           box((1.6, 12.6, 12.9), (14.4, 13.0, 13.2), mat("grip_black")),
           box((3.0, 4.6, 12.9), (4.0, 5.6, 13.2), mat("chrome")), box((12.0, 4.6, 13.0), (13.0, 5.6, 13.2), mat("chrome")),
           box((6.0, 13.0, 14.2), (10.0, 13.6, 14.8), mat("grip_black")), box((5.8, 13.0, 14.0), (6.2, 13.8, 15.0), mat("grip_black")),
           box((9.8, 13.0, 14.0), (10.2, 13.8, 15.0), mat("grip_black")),
           box((5.0, 3.8, 13.1), (11.0, 5.4, 13.2), faces("white", north=A.region("rk_fa_label", 6.0, 1.6, label_plate("FIRST AID", (28, 136, 70), (255, 255, 255)))))]
    return out


def eyewash_station():
    """An eye wash station on the wall, its front to the north: the green backboard and its sign, two bottles of sterile
    eye wash in their holder (white bottles, green caps), the little mirror."""
    out = [box((2.0, 1.0, 15.4), (14.0, 15.0, 16.0), mat("aid_green")),
           box((3.0, 9.6, 15.3), (13.0, 14.4, 15.4), faces("white", north=A.region("rk_eye", 10.0, 4.8, sign(["EYE WASH", "rinse for 15 minutes", "then call first aid"])))),
           box((3.0, 1.6, 12.8), (13.0, 2.2, 15.4), mat("aid_green")), box((3.0, 2.2, 12.6), (13.0, 4.0, 13.0), mat("glass"))]
    for cx in (5.4, 10.6):
        out += round_part(cx, 14.0, 1.1, 2.2, 7.6, "white", n=4) + round_part(cx, 14.0, 0.5, 7.6, 8.6, "aid_green", n=3)
    out += [box((7.2, 5.0, 15.2), (8.8, 8.6, 15.4), mat("chrome"))]
    return out


def spine_board():
    """The spine board on its wall bracket, its face to the north (the wall to the south; built 0..32 tall): the orange
    board (1.83 m by 41 cm) with the handholds cut along its sides, its three straps and buckles, the head blocks."""
    o = "board_orange"
    z0, z1 = 13.6, 14.2
    out = [box((5.0, 2.0, z0), (11.0, 31.0, z1), mat(o))]
    for k in range(7):
        y = 3.0 + k * 4.0
        out += [box((3.4, y, z0), (5.0, y + 1.0, z1), mat(o)), box((11.0, y, z0), (12.6, y + 1.0, z1), mat(o))]
    out += [box((3.4, 2.0, z0), (4.0, 31.0, z1), mat(o)), box((12.0, 2.0, z0), (12.6, 31.0, z1), mat(o))]
    for y in (8.0, 16.0, 24.0):
        out += [box((3.3, y, z0 - 0.15), (12.7, y + 0.9, z0), mat("grip_black")), box((7.4, y - 0.1, z0 - 0.3), (8.6, y + 1.0, z0 - 0.15), mat("chrome"))]
    out += [box((4.6, 26.0, z0 - 1.6), (6.6, 29.4, z0), mat("glove_blue")), box((9.4, 26.0, z0 - 1.6), (11.4, 29.4, z0), mat("glove_blue")),
            box((3.6, 6.0, z1), (12.4, 6.8, 16.0), mat("chrome")), box((3.6, 6.8, z1), (4.2, 8.2, 14.6), mat("chrome")),
            box((11.8, 6.8, z1), (12.4, 8.2, 14.6), mat("chrome")), box((3.6, 27.0, z1), (12.4, 27.8, 16.0), mat("chrome"))]
    return out


# ------------------------------------------------------------------ the wheelchair
def wheelchair():
    """A wheelchair, its sitter looking north: the chrome frame, the big rear wheels (their tyres, spokes' hub, push rims),
    the front castors on their forks, the black sling seat and back, the arm pads, the push handles, the footrests."""
    out = []
    for x0, s in ((2.0, -1), (14.0, 1)):
        rim = round_ring(9.6, 11.0, 4.2, 0.45, 0.0, 0.6, "grip_black", 0.0, "grip_black", n=8)
        ring = [e for e in rim if e["to"][1] - e["from"][1] > 0.1]
        for e in swap_xy(ring):
            e["from"][0], e["to"][0] = round(x0 + e["from"][0] - 0.3, 4), round(x0 + e["to"][0] - 0.3, 4)
            out.append(e)
        push = [e for e in round_ring(9.6, 11.0, 3.6, 0.22, 0.0, 0.25, "chrome", 0.0, "chrome", n=8) if e["to"][1] - e["from"][1] > 0.05]
        for e in swap_xy(push):
            off = x0 + (0.5 if s > 0 else -0.75)
            e["from"][0], e["to"][0] = round(off + e["from"][0], 4), round(off + e["to"][0], 4)
            out.append(e)
        out += rod_x(x0 - 0.4, x0 + 0.4, 4.2, 11.0, 0.5, "chrome")
    for x0 in (3.0, 12.4):
        out += [box((x0, 4.2, 3.0), (x0 + 0.6, 4.8, 12.0), mat("chrome")),
                box((x0, 4.8, 11.4), (x0 + 0.6, 15.4, 12.0), mat("chrome")), box((x0, 15.0, 11.4), (x0 + 0.6, 15.4, 13.4), mat("chrome")),
                box((x0 - 0.05, 15.0, 12.8), (x0 + 0.65, 15.45, 13.6), mat("grip_black")),
                box((x0, 4.8, 3.0), (x0 + 0.6, 9.6, 3.6), mat("chrome")), box((x0, 9.6, 3.0), (x0 + 0.6, 10.2, 11.4), mat("chrome")),
                box((x0 - 0.1, 10.2, 5.0), (x0 + 0.7, 10.6, 10.0), mat("grip_black")),
                box((x0, 1.4, 2.6), (x0 + 0.6, 4.2, 3.2), mat("chrome")), box((x0 + 0.05, 1.0, 1.6), (x0 + 0.55, 1.6, 2.6), mat("chrome")),
                box((x0 - 0.6 if x0 < 8 else x0, 1.0, 0.4), (x0 + 0.6 if x0 < 8 else x0 + 1.2, 1.3, 2.0), mat("grip_black"))]
        out += rod_x(x0 + 0.0, x0 + 0.6, 1.0, 3.0, 1.0, "grip_black", n=4)
    out += [box((3.6, 7.6, 3.4), (12.4, 7.9, 11.4), mat("sling")), box((3.6, 8.2, 11.6), (12.4, 14.8, 11.9), mat("sling"))]
    return out


# ------------------------------------------------------------------ the first aider's desk
def pedal_bin(colour):
    """A pedal bin (30 by 40 cm, 60 tall), its front to the north: the body with its rounded corners, the lid, the pedal,
    its label (yellow: clinical waste; black: general waste)."""
    c = {"yellow": "bin_yellow", "black": "grip_black"}[colour]
    out = [box((5.2, 0.6, 5.4), (10.8, 9.0, 10.6), mat(c)), box((5.0, 0.6, 5.6), (11.0, 9.0, 10.4), mat(c)),
           box((4.9, 9.0, 5.3), (11.1, 9.8, 10.7), mat(c)), box((6.0, 9.8, 9.8), (10.0, 10.1, 10.6), mat(c)),
           box((5.4, 0, 5.8), (10.6, 0.6, 10.2), mat("grip_black")), box((6.6, 0.2, 4.4), (9.4, 0.6, 5.4), mat("chrome")),
           box((6.0, 4.6, 5.3), (10.0, 7.0, 5.4), faces("white", north=A.region(f"rk_bin_{colour}", 4.0, 2.4,
               sign(["CLINICAL", "WASTE"], (200, 160, 20)) if colour == "yellow" else sign(["GENERAL", "WASTE"], (40, 40, 44)))))]
    return out


# ------------------------------------------------------------------ crew rest: the recliner, the side table, the sofa
def recliner(reclined=False):
    """A leather recliner, its sitter looking north: the base, the seat cushion, the deep padded arms, the high back with
    its pillow top; reclined, the back leans back and the footrest is out on its scissor arms."""
    L = "recliner"
    out = [box((1.4, 0, 2.6), (14.6, 1.0, 14.4), mat("grip_black")),
           box((1.0, 1.0, 1.4), (4.0, 10.6, 15.2), mat(L)), box((12.0, 1.0, 1.4), (15.0, 10.6, 15.2), mat(L)),
           box((1.2, 10.6, 1.6), (3.8, 11.4, 15.0), mat(L)), box((12.2, 10.6, 1.6), (14.8, 11.4, 15.0), mat(L)),
           box((4.0, 1.0, 1.8), (12.0, 5.4, 14.0), mat(L)), box((4.0, 5.4, 1.6), (12.0, 7.8, 12.0), mat(L))]
    back = [box((4.0, 5.4, 12.0), (12.0, 21.0, 15.0), mat(L)), box((4.2, 7.8, 11.0), (11.8, 15.6, 12.0), mat(L)),
            box((4.2, 15.8, 11.2), (11.8, 20.4, 12.0), mat(L)), box((4.1, 21.0, 11.8), (11.9, 22.2, 14.8), mat(L))]
    if reclined:
        for e in back:
            e["rotation"] = {"angle": 22.5, "axis": "x", "origin": [8.0, 5.4, 15.2]}
        out += back
        foot = [box((4.0, 4.2, -4.8), (12.0, 6.6, 1.6), mat(L))]
        out += foot + [box((5.0, 3.0, -1.0), (5.6, 4.2, 1.8), mat("chrome")), box((10.4, 3.0, -1.0), (11.0, 4.2, 1.8), mat("chrome"))]
    else:
        out += back + [box((4.0, 1.0, 1.4), (12.0, 5.2, 1.8), mat(L))]
    return out


def side_table():
    """A crew rest side table (50 cm square, 55 cm tall): the oak top and shelf on their legs, the reading lamp (its
    base, stem and lit linen shade), a book, a mug."""
    out = [box((3.0, 8.2, 3.0), (13.0, 8.8, 13.0), mat("oak")), box((3.6, 2.6, 3.6), (12.4, 3.0, 12.4), mat("oak"))]
    for x0 in (3.2, 12.0):
        for z0 in (3.2, 12.0):
            out.append(box((x0, 0, z0), (x0 + 0.8, 8.2, z0 + 0.8), mat("oak")))
    out += round_part(6.0, 6.0, 1.2, 8.8, 9.3, "graphite", n=4) + round_part(6.0, 6.0, 0.22, 9.3, 15.6, "graphite")
    shade = round_ring(6.0, 6.0, 2.6, 0.2, 14.6, 18.2, "lampshade", 0.0, "lampshade", n=6)
    for e in shade:
        if e["to"][1] - e["from"][1] > 0.1:
            e["shade"] = False
            out.append(e)
    out.append({**box((5.4, 15.4, 5.4), (6.6, 16.0, 6.6), mat("light")), "shade": False})
    out += [box((8.4, 8.8, 7.6), (12.0, 9.5, 12.2), mat("bk_red")), box((8.5, 8.85, 7.7), (11.9, 9.45, 12.1), mat("paper"))]
    out += MK.round_ring(5.6, 11.0, 0.7, 0.12, 8.8, 10.2, "white", 1.0, "coffee", n=4)
    return out


def crew_sofa(left, right):
    """The crew rest sofa: the manager's sofa's shape in a grey woven fabric."""
    out = json.loads(json.dumps(MK.sofa(left, right)))
    for e in out:
        for f in e["faces"].values():
            if f.get("texture") == "#leather":
                f["texture"] = "#sofa"
    return out


def crew_coffee_table():
    """Built whole along x 0..32: oak on black legs, a lower shelf (magazines), on it the board game in its box, two
    mugs, the TV remote, a deck of cards."""
    out = [box((0.8, 6.2, 3.0), (31.2, 7.0, 13.0), mat("oak")), box((1.6, 1.6, 3.6), (30.4, 2.0, 12.4), mat("oak"))]
    for x0 in (1.2, 30.0):
        for z0 in (3.4, 11.8):
            out.append(box((x0, 0, z0), (x0 + 0.8, 6.2, z0 + 0.8), mat("grip_black")))
    out += [box((4.0, 7.0, 5.0), (12.0, 8.0, 11.0), faces("bk_red", up=A.region("rk_game", 8.0, 6.0, game_lid)))]
    out += MK.round_ring(17.0, 6.0, 0.7, 0.12, 7.0, 8.4, "navy", 1.0, "coffee", n=4) + MK.round_ring(20.4, 9.4, 0.7, 0.12, 7.0, 8.4, "white", 1.0, "coffee", n=4)
    out += [box((24.0, 7.0, 6.0), (24.8, 7.35, 9.0), mat("grip_black"), {"angle": 22.5, "axis": "y", "origin": [24.4, 7.0, 7.5]}),
            box((26.4, 7.0, 9.0), (28.0, 7.3, 11.2), mat("white")),
            box((5.0, 2.0, 5.0), (11.0, 2.3, 11.0), faces("paper", up=A.region("rk_mag", 6.0, 6.0, MK.magazine("PILOT", (40, 90, 160)))))]
    return out


def wall_tv():
    """The crew rest TV on the wall (a 55 inch, 1.22 m by 70 cm; built whole along x 0..32, its face to the north, the
    wall to the south): the slim bezel and its screen (the tower's weather, live in game), the bracket behind."""
    return [box((6.0, 2.0, 15.0), (26.0, 13.6, 15.6), mat("black")),
            {**box((6.3, 2.4, 14.95), (25.7, 13.3, 15.0), faces("black", north=SCR.face("rk_tv", 19.4, 10.9, weather))), "shade": False},
            box((13.0, 5.0, 15.6), (19.0, 10.0, 16.0), mat("steel")),
            box((15.6, 2.0, 14.9), (16.4, 2.3, 15.0), mat("white"))]


# ------------------------------------------------------------------ the kitchenette and the fridge
def kitchenette():
    """The crew rest kitchenette (3 x 1, two high, built whole along x 0..48, its user looking north at the wall): base
    cabinets with doors and a drawer stack on a plinth, the worktop, the sink and its tap, the espresso machine, the
    kettle, the mug tree, the fruit bowl, the microwave (its clock live); the tiled splashback and the wall cabinets with
    their handles, the light under them."""
    out = [box((0.8, 0, 1.4), (47.2, 0.9, 14.0), mat("black")),
           box((0.2, 0.9, 0.6), (47.8, 9.6, 14.8), mat("white")),
           box((0.2, 9.6, 0.6), (20.0, 13.8, 14.8), mat("white")), box((30.0, 9.6, 0.6), (47.8, 13.8, 14.8), mat("white")),
           box((20.0, 9.6, 0.6), (30.0, 13.8, 3.0), mat("white")), box((20.0, 9.6, 12.0), (30.0, 13.8, 14.8), mat("white"))]
    for x0, x1, kx in ((0.4, 8.1, 7.4), (8.3, 16.0, 9.0), (32.2, 39.9, 39.2), (40.1, 47.6, 40.8)):
        out += [box((x0, 1.1, 14.8), (x1, 13.6, 15.1), mat("white")), box((kx - 0.15, 9.6, 15.1), (kx + 0.15, 12.6, 15.4), mat("chrome"))]
    for y0, y1 in ((10.4, 13.6), (6.0, 10.2), (1.1, 5.8)):
        out += [box((16.2, y0, 14.8), (32.0, y1, 15.1), mat("white")), box((22.0, y1 - 1.0, 15.1), (26.2, y1 - 0.7, 15.4), mat("chrome"))]
    wt = "oak"
    out += [box((0, 13.8, 0), (48, 14.6, 3.0), mat(wt)), box((0, 13.8, 12.0), (48, 14.6, 15.6), mat(wt)),
            box((0, 13.8, 3.0), (20.0, 14.6, 12.0), mat(wt)), box((30.0, 13.8, 3.0), (48, 14.6, 12.0), mat(wt))]
    ss = "stainless"
    out += [box((20.0, 10.0, 3.0), (30.0, 10.4, 12.0), mat(ss)), box((20.0, 10.4, 3.0), (20.4, 13.8, 12.0), mat(ss)),
            box((29.6, 10.4, 3.0), (30.0, 13.8, 12.0), mat(ss)), box((20.4, 10.4, 3.0), (29.6, 13.8, 3.4), mat(ss)),
            box((20.4, 10.4, 11.6), (29.6, 13.8, 12.0), mat(ss))]
    out += round_part(25.0, 1.6, 0.6, 14.6, 17.2, "chrome", n=3)
    out += [box((24.7, 17.2, 1.3), (25.3, 17.8, 6.0), mat("chrome")), box((24.75, 16.2, 5.6), (25.25, 17.2, 6.0), mat("chrome")),
            box((25.6, 17.6, 1.4), (26.0, 18.0, 3.6), mat("chrome"))]
    # the espresso machine, its cups on top; the kettle; the mug tree; the fruit bowl; the microwave
    out += [box((2.0, 14.6, 2.0), (7.6, 21.8, 8.4), mat("stainless")), box((2.4, 18.8, 8.4), (7.2, 19.6, 9.6), mat("black")),
            box((3.8, 14.6, 8.4), (5.8, 15.2, 10.0), mat("black")), box((4.4, 18.0, 8.4), (5.2, 18.8, 9.2), mat("chrome")),
            box((3.0, 20.0, 8.38), (6.6, 21.2, 8.42), faces("black", south=SCR.face("rk_esp", 3.6, 1.2, label_plate("READY", (14, 18, 24), (120, 220, 255)))))]
    out += MK.round_ring(4.8, 9.2, 0.55, 0.1, 15.2, 16.2, "white", 0.2, "white", n=4)
    out += round_part(11.0, 5.0, 1.2, 14.6, 15.0, "black", n=4) + round_part(11.0, 5.0, 1.05, 15.0, 18.4, "stainless", n=4)
    out += [box((10.6, 18.4, 4.6), (11.4, 18.8, 5.4), mat("black")), box((12.0, 15.6, 4.7), (12.9, 18.0, 5.3), mat("black")),
            box((9.4, 17.2, 4.8), (10.0, 17.8, 5.2), mat("stainless"))]
    out += round_part(15.0, 9.0, 1.0, 14.6, 14.9, "graphite", n=4) + round_part(15.0, 9.0, 0.2, 14.9, 20.0, "graphite")
    for k, (dx, dz, c) in enumerate(((1.0, 0.0, "navy"), (-1.0, 0.0, "mug_red"), (0.0, 1.0, "white"))):
        out += [box((15.0 + dx * 0.2 - 0.1, 18.6 - k * 0.8, 9.0 + dz * 0.2 - 0.1), (15.0 + dx * 1.0 + 0.1, 18.8 - k * 0.8, 9.0 + dz * 1.0 + 0.1), mat("graphite"))]
        out += MK.round_ring(15.0 + dx * 1.4, 9.0 + dz * 1.4, 0.55, 0.1, 17.0 - k * 0.8, 18.4 - k * 0.8, c, 0.2, c, n=4)
    out += MK.round_ring(35.0, 9.0, 2.0, 0.25, 14.6, 15.6, "white", 0.3, "white", n=5)
    for (dx, dz, c) in ((-0.6, -0.4, "red"), (0.6, 0.3, "bin_yellow"), (0.0, 0.6, "glove_green")):
        out += round_part(35.0 + dx, 9.0 + dz, 0.6, 14.9, 16.0, c, n=3)
    out += [box((38.4, 14.6, 1.0), (47.0, 19.8, 7.6), mat("white")), box((38.8, 15.0, 7.6), (44.6, 19.4, 7.7), mat("glass")),
            box((44.8, 15.0, 7.6), (46.6, 19.4, 7.7), mat("graphite")),
            box((45.0, 18.2, 7.7), (46.4, 18.9, 7.75), faces("black", south=SCR.face("rk_mw", 1.4, 0.7, label_plate("14:32", (6, 10, 8), (80, 230, 120), live={"src": "time"})))),
            box((44.0, 15.6, 7.7), (44.4, 19.0, 8.0), mat("chrome"))]
    # the splashback and the wall cabinets (their bottoms at 1.45 m)
    out += [box((0, 14.6, 0), (48, 23.2, 0.3), {**mat("tile"), "down": {"texture": "#tile"}})]
    out += [box((0, 23.2, 0), (48, 31.8, 5.6), mat("white"))]
    for x0 in (0.2, 8.2, 16.2, 24.2, 32.2, 40.2):
        out += [box((x0, 23.4, 5.6), (x0 + 7.6, 31.6, 5.9), mat("white")),
                box((x0 + (6.6 if (x0 // 8) % 2 == 0 else 0.7), 23.8, 5.9), (x0 + (6.9 if (x0 // 8) % 2 == 0 else 1.0), 26.8, 6.2), mat("chrome"))]
    out.append({**box((1.0, 23.0, 3.6), (47.0, 23.2, 4.6), mat("light")), "shade": False})
    return out


def crew_fridge():
    """A tall fridge-freezer (60 cm wide, 1.8 m), its front to the north: the white body, the freezer door below and the
    fridge door above, their long handles, papers and magnets on its door."""
    f = "fridge"
    out = [box((2.4, 0.6, 1.6), (13.6, 28.8, 14.4), mat(f)), box((2.8, 0, 2.0), (13.2, 0.6, 14.0), mat("black")),
           box((2.4, 0.6, 1.0), (13.6, 10.8, 1.6), mat(f)), box((2.4, 11.0, 1.0), (13.6, 28.8, 1.6), mat(f)),
           box((2.4, 10.8, 1.2), (13.6, 11.0, 1.6), mat("graphite"))]
    for y0, y1 in ((4.0, 10.0), (12.0, 20.0)):
        out += [box((11.6, y0, 0.4), (12.2, y1, 0.7), mat("chrome")), box((11.6, y0, 0.7), (12.2, y0 + 0.4, 1.0), mat("chrome")),
                box((11.6, y1 - 0.4, 0.7), (12.2, y1, 1.0), mat("chrome"))]
    out += [box((4.0, 20.0, 0.9), (7.4, 24.6, 1.0), faces("paper", north=A.region("rk_rota", 3.4, 4.6, TK.paper(911)))),
            box((5.4, 24.2, 0.8), (6.0, 24.8, 0.9), mat("red")), box((8.0, 17.0, 0.9), (10.6, 20.0, 1.0), faces("paper", north=A.region("rk_photo_f", 2.6, 3.0, MK.photo("fleet")))),
            box((9.0, 19.6, 0.8), (9.6, 20.2, 0.9), mat("glove_blue"))]
    return out


# ------------------------------------------------------------------ the lockers
CREW_NAMES = ["REYES", "OKAFOR", "LINDQVIST", "NAKAMURA", "BRANDT", "SILVA", "MORROW", "HADDAD", "PETROV", "ADEYEMI", "KOWALSKI", "DUBOIS"]


LOCKERS = 4


# ------------------------------------------------------------------ the bunk
# ------------------------------------------------------------------ the water dispenser
def water_dispenser():
    """A bottled water cooler (35 cm wide, 1 m tall, its 19 litre bottle on top), its front to the north: the grey and
    white cabinet, the blue and red taps in their recess, the drip tray, the cup dispenser on its side, the bottle."""
    out = [box((2.4, 0, 2.6), (13.6, 0.8, 13.4), mat("graphite")), box((2.6, 0.8, 2.8), (13.4, 16.0, 13.2), mat("white")),
           box((2.6, 9.0, 2.6), (13.4, 13.4, 2.8), mat("graphite")),
           box((4.0, 9.2, 2.4), (12.0, 13.2, 2.6), mat("graphite")),
           box((5.0, 11.6, 1.8), (6.2, 12.6, 2.6), mat("glove_blue")), box((9.8, 11.6, 1.8), (11.0, 12.6, 2.6), mat("red")),
           box((4.4, 8.8, 1.4), (11.6, 9.2, 2.8), mat("grip_black")), box((4.6, 9.2, 1.6), (11.4, 9.3, 2.6), mat("chrome")),
           box((2.6, 16.0, 2.8), (13.4, 16.6, 13.2), mat("graphite"))]
    out += round_part(14.4, 8.0, 0.9, 5.0, 12.0, "white", n=3) + [box((13.4, 10.0, 7.6), (13.6, 10.6, 8.4), mat("chrome"))]
    out += round_part(8.0, 8.0, 0.6, 16.6, 17.8, "water", n=3) + round_part(8.0, 8.0, 3.4, 17.8, 25.6, "water", n=6)
    out += round_part(8.0, 8.0, 2.2, 25.6, 26.4, "water", n=5)
    return out


# ======================================================================== take 2 (Justin on take 1: the monitor without
# its cuff, hose, SpO2 sensor and leads; the divider wants three panels; the sink "needs work"; the medical office
# subpar; the bunks, the bag on the lockers, the boots, the crash cart's top; its defibrillator off until used)

def cable(points, r=0.08, tex="grip_black"):
    """A cable or hose along a path of points (pixels): each leg straight along an axis, or at 22.5 or 45 degrees in a
    plane of two axes."""
    out = []
    for (x0, y0, z0), (x1, y1, z1) in zip(points, points[1:]):
        d = (x1 - x0, y1 - y0, z1 - z0)
        moving = [i for i in range(3) if abs(d[i]) > 1e-6]
        if len(moving) == 1:
            lo = [min(a, b) - r for a, b in zip((x0, y0, z0), (x1, y1, z1))]
            hi = [max(a, b) + r for a, b in zip((x0, y0, z0), (x1, y1, z1))]
            out.append(box(lo, hi, mat(tex)))
            continue
        assert len(moving) == 2, (points, d)
        a, b = moving
        ang = math.degrees(math.atan2(d[b], d[a]))
        ln = math.hypot(d[a], d[b])
        mid = [(p + q) / 2 for p, q in zip((x0, y0, z0), (x1, y1, z1))]
        lo, hi = [m - r for m in mid], [m + r for m in mid]
        lo[a], hi[a] = mid[a] - ln / 2, mid[a] + ln / 2
        if (a, b) == (0, 1):
            axis, angle = "z", ang
        elif (a, b) == (1, 2):
            axis, angle = "x", ang
        else:
            axis, angle = "y", -ang
        while angle > 90:
            angle -= 180
        while angle < -90:
            angle += 180
        snap = min((-45, -22.5, 0, 22.5, 45), key=lambda s: abs(s - angle))
        assert abs(snap - angle) < 1.0, (points, angle)
        out.append(box(lo, hi, mat(tex), {"angle": snap, "axis": axis, "origin": [round(v, 4) for v in mid]}))
    return out


def loop_xy(cx, cy, z, r, w=0.16, tex="grip_black"):
    """A coil of cable hanging flat against something, in the xy plane at z (a ring of segments)."""
    ring = [e for e in round_ring(cx, cy, r, w, z - 0.08, z + 0.08, tex, 0.0, tex, n=5) if e["to"][1] - e["from"][1] > 0.1]
    return swap_yz(ring)


def screen_off(img):
    """A screen switched off: dark glass, a faint sheen across it."""
    W, H = img.size
    a = np.zeros((H, W, 3), float)
    y, x = np.mgrid[0:H, 0:W]
    a[:] = 14
    a += (np.clip(1 - np.abs((x / W + y / H) - 0.7) * 4, 0, 1) * 16)[..., None]
    img.paste(TK._img(a), (0, 0))
    return img


def poster(kind):
    """The first aid room's posters: CPR in four steps, the recovery position and bleeding, hand hygiene's five
    moments; drawn as printed posters are (icons and text), in clip frames."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(252, 252, 248))
        band = {"cpr": (200, 40, 36), "aid": (28, 136, 70), "hands": (30, 90, 170)}[kind]
        title = {"cpr": "CPR", "aid": "FIRST AID", "hands": "HAND HYGIENE"}[kind]
        d.rectangle((0, 0, W, H * 0.14), fill=band)
        d.text((W / 2, H * 0.07), title, font=_fit(d, title, FONT, round(H * 0.08), W - 10), fill=(255, 255, 255), anchor="mm")
        steps = {"cpr": ["CHECK: safe? responsive?", "CALL for help, AED", "PUSH hard and fast 30x", "BREATHE 2, repeat"],
                 "aid": ["Unresponsive, breathing:", "recovery position", "Bleeding: press firmly,", "raise, call first aid"],
                 "hands": ["Before touching a patient", "Before a clean task", "After body fluid risk", "After touching a patient"]}[kind]
        for k, t in enumerate(steps):
            y0 = H * (0.18 + k * 0.2)
            d.rounded_rectangle((W * 0.06, y0, W * 0.3, y0 + H * 0.16), radius=6, outline=band, width=3)
            cx, cy = W * 0.18, y0 + H * 0.08
            if kind == "cpr":
                d.ellipse((cx - W * 0.05, cy - H * 0.05, cx + W * 0.05, cy + H * 0.05), outline=(40, 40, 46), width=2)
                d.line((cx - W * 0.08, cy + H * 0.06, cx + W * 0.08, cy + H * 0.06), fill=(40, 40, 46), width=3)
            elif kind == "aid":
                d.line((cx - W * 0.08, cy + H * 0.03, cx + W * 0.06, cy + H * 0.03), fill=(40, 40, 46), width=4)
                d.ellipse((cx + W * 0.05, cy - H * 0.01, cx + W * 0.09, cy + H * 0.05), fill=(40, 40, 46))
            else:
                d.polygon([(cx - W * 0.05, cy + H * 0.05), (cx - W * 0.02, cy - H * 0.05), (cx + W * 0.02, cy - H * 0.05), (cx + W * 0.05, cy + H * 0.05)], outline=(40, 40, 46))
            d.text((W * 0.04 + W * 0.3, y0 + H * 0.08), f"{k + 1}  {t}", font=_fit(d, f"{k + 1}  {t}", FONT, round(H * 0.045), W * 0.6), fill=(30, 30, 34), anchor="lm")
        return img
    return draw


def med_poster(kind):
    """A poster in its aluminium clip frame on the wall, its face to the north (the wall to the south)."""
    return [box((3.0, 1.0, 15.6), (13.0, 15.0, 16.0), mat("chrome")),
            box((3.3, 1.3, 15.5), (12.7, 14.7, 15.6), faces("white", north=A.region(f"rk_poster_{kind}", 9.4, 13.4, poster(kind)))),
            box((3.0, 1.0, 15.4), (13.0, 1.4, 15.6), mat("chrome")), box((3.0, 14.6, 15.4), (13.0, 15.0, 15.6), mat("chrome"))]


POSTERS = ["cpr", "aid", "hands"]


def drug_box(name, colour):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(248, 248, 244))
        d.rectangle((0, 0, W, H * 0.3), fill=colour)
        d.text((W / 2, H * 0.62), name, font=_fit(d, name, FONT, round(H * 0.28), W - 4), fill=(30, 30, 34), anchor="mm")
        return img
    return draw


DRUGS = [("PARACETAMOL", (40, 90, 170)), ("IBUPROFEN", (200, 60, 40)), ("ANTISEPTIC", (40, 140, 90)), ("BANDAGES", (230, 150, 40)),
         ("PLASTERS", (180, 60, 150)), ("SALINE", (60, 160, 200)), ("ANTIHISTAMINE", (120, 90, 170)), ("BURN GEL", (220, 90, 40))]


def bevel_box(f, t, tex, b=0.3, top=True):
    """A cushion with its edges rounded off: its core, the top slightly smaller (the radius), the sides inset at the top."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    out = [box((x0, y0, z0), (x1, y1 - b, z1), mat(tex, skip=("up",))),
           box((x0 + b, y1 - b, z0 + b), (x1 - b, y1, z1 - b), mat(tex, skip=("down",)))]
    out += [box((x0, y1 - b, z0 + b), (x0 + b, y1 - b * 0.4, z1 - b), mat(tex, skip=("down",))), box((x1 - b, y1 - b, z0 + b), (x1, y1 - b * 0.4, z1 - b), mat(tex, skip=("down",))),
            box((x0 + b, y1 - b, z0), (x1 - b, y1 - b * 0.4, z0 + b), mat(tex, skip=("down",))), box((x0 + b, y1 - b, z1 - b), (x1 - b, y1 - b * 0.4, z1), mat(tex, skip=("down",)))]
    return out


# ------------------------------------------------------------------ the couch (take 2)
def exam_couch(raised=True):
    """The examination couch, 1.9 m by 67 cm, its top at 72 cm, its feet to the south (+z; built 0..32 along z): the white
    steel frame on four legs (two on locking castors, two on glides), its rails, the shelf under it with a folded
    blanket, the chrome side rails, the blue vinyl top in two padded sections with their rounded edges and the piping
    between (the head raised on its gas strut, or flat with a pillow), the paper roll on its holder and its sheet."""
    w = "wsteel"
    out = []
    for x0 in (3.0, 12.1):
        for z0, caster in ((2.0, True), (29.1, False)):
            out.append(box((x0, 1.6 if caster else 0.4, z0), (x0 + 0.9, 9.6, z0 + 0.9), mat(w)))
            if caster:
                out += [box((x0 - 0.1, 1.2, z0 - 0.1), (x0 + 1.0, 1.6, z0 + 1.0), mat("chrome"))]
                out += rod_x(x0 + 0.1, x0 + 0.8, 0.6, z0 + 0.45, 0.6, "grip_black", n=3)
                out.append(box((x0 + 0.95, 1.0, z0 + 0.1), (x0 + 1.25, 1.3, z0 + 0.8), mat("red")))
            else:
                out.append(box((x0 - 0.05, 0, z0 - 0.05), (x0 + 0.95, 0.4, z0 + 0.95), mat("grip_black")))
        out.append(box((x0, 2.6, 2.9), (x0 + 0.9, 3.2, 29.1), mat(w)))
        out.append(box((x0, 9.0, 2.9), (x0 + 0.9, 9.6, 29.1), mat(w)))
    out += [box((3.9, 2.6, 2.0), (12.1, 3.2, 2.9), mat(w)), box((3.9, 2.6, 29.1), (12.1, 3.2, 30.0), mat(w)),
            box((3.9, 3.2, 4.0), (12.1, 3.5, 28.0), mat(w))]
    out += bevel_box((5.0, 3.5, 18.0), (11.0, 4.7, 24.0), "blanket", b=0.25)
    out += [box((3.0, 9.6, 1.6), (13.0, 10.4, 30.4), mat(w))]
    for x0 in (2.2, 13.4):
        out += [box((x0, 10.6, 9.0), (x0 + 0.4, 11.0, 28.0), mat("chrome")),
                box((x0 + (0.4 if x0 < 8 else -0.4), 10.0, 10.0), (x0 + (0.8 if x0 < 8 else 0.0), 10.6, 10.6), mat("chrome")),
                box((x0 + (0.4 if x0 < 8 else -0.4), 10.0, 26.4), (x0 + (0.8 if x0 < 8 else 0.0), 10.6, 27.0), mat("chrome"))]
    top = 11.8
    out += bevel_box((2.6, 10.4, 8.4), (13.4, top, 30.4), "vinyl", b=0.35)
    out.append(box((2.8, 10.4, 8.0), (13.2, 11.4, 8.4), mat("grip_black")))
    head = bevel_box((2.6, 10.4, 1.6), (13.4, top, 8.0), "vinyl", b=0.35)
    sheet_head = [box((4.0, top, 1.6), (12.0, top + 0.1, 8.0), mat("couch_paper"))]
    if raised:
        r = {"angle": 22.5, "axis": "x", "origin": [8.0, 10.4, 8.0]}
        for e in head + sheet_head:
            e["rotation"] = r
        out += head + sheet_head
        out += [box((7.6, 9.8, 4.4), (8.4, 10.4, 7.6), mat("chrome"), {"angle": 22.5, "axis": "x", "origin": [8.0, 10.1, 7.6]}),
                box((7.7, 10.0, 3.0), (8.3, 10.8, 4.6), mat("grip_black"), {"angle": 22.5, "axis": "x", "origin": [8.0, 10.1, 7.6]})]
    else:
        out += head + sheet_head + bevel_box((4.4, top + 0.1, 2.2), (11.6, top + 1.1, 6.0), "pillow", b=0.3)
    out.append(box((4.0, top, 8.4), (12.0, top + 0.1, 22.0), mat("couch_paper")))
    out += [box((3.2, 10.4, 0.2), (3.6, 13.0, 0.6), mat("chrome")), box((12.4, 10.4, 0.2), (12.8, 13.0, 0.6), mat("chrome"))]
    out += rod_x(3.6, 12.4, 13.0, 0.4, 0.75, "couch_paper", n=4) + rod_x(3.2, 12.8, 13.0, 0.4, 0.15, "chrome")
    return out


def step_stool():
    """A two-step stool for the couch: the chrome frame, its two treads with their black rubber mats, the feet."""
    out = []
    for x0 in (3.0, 12.4):
        out += [box((x0, 0, 2.0), (x0 + 0.6, 7.4, 2.6), mat("chrome")), box((x0, 0, 12.4), (x0 + 0.6, 3.8, 13.0), mat("chrome")),
                box((x0, 3.4, 2.6), (x0 + 0.6, 3.8, 12.4), mat("chrome")),
                box((x0 - 0.1, 0, 1.9), (x0 + 0.7, 0.3, 2.7), mat("grip_black")), box((x0 - 0.1, 0, 12.3), (x0 + 0.7, 0.3, 13.1), mat("grip_black"))]
    out += [box((3.0, 3.8, 7.6), (13.0, 4.2, 13.0), mat("chrome")), box((3.2, 4.2, 7.8), (12.8, 4.4, 12.8), mat("grip_black")),
            box((3.0, 7.4, 2.0), (13.0, 7.8, 7.4), mat("chrome")), box((3.2, 7.8, 2.2), (12.8, 8.0, 7.2), mat("grip_black"))]
    return out


def privacy_screen():
    """A folding hospital screen, three panels in a zigzag (1.7 m tall), each 50 cm: their steel tube frames, the fabric,
    the hinges between them, the castors at their feet."""
    out = []

    def panel(x0, x1, rot):
        els = [box((x0, 1.0, 7.75), (x0 + 0.45, 27.0, 8.25), mat("wsteel")), box((x1 - 0.45, 1.0, 7.75), (x1, 27.0, 8.25), mat("wsteel")),
               box((x0 + 0.45, 26.55, 7.75), (x1 - 0.45, 27.0, 8.25), mat("wsteel")), box((x0 + 0.45, 3.0, 7.75), (x1 - 0.45, 3.45, 8.25), mat("wsteel")),
               box((x0 + 0.45, 3.45, 7.9), (x1 - 0.45, 26.55, 8.1), mat("screen_fab"))]
        for cx in (x0 + 0.22, x1 - 0.22):
            els += [box((cx - 0.3, 0.45, 7.7), (cx + 0.3, 1.0, 8.3), mat("chrome")), box((cx - 0.22, 0, 7.78), (cx + 0.22, 0.45, 8.22), mat("grip_black"))]
        for y in (6.0, 22.0):
            els.append(box((x1 - 0.15 if rot and rot["origin"][0] > x0 else x0 - 0.15, y, 7.7), (x1 + 0.15 if rot and rot["origin"][0] > x0 else x0 + 0.15, y + 1.0, 8.3), mat("chrome")))
        if rot:
            for e in els:
                e["rotation"] = rot
        return els
    out += panel(5.6, 10.4, None)
    out += panel(0.8, 5.6, {"angle": -45.0, "axis": "y", "origin": [5.6, 0, 8.0]})
    out += panel(10.4, 15.2, {"angle": -45.0, "axis": "y", "origin": [10.4, 0, 8.0]})
    return out


# ------------------------------------------------------------------ the crash cart (take 2)
def defib():
    """The monitor-defibrillator on the crash cart (40 by 25 by 29 cm), its front to the north, switched off until it is
    used: the charcoal case, its dark screen, the shock and analyse keys, the energy knob, the printer's slot, the carry
    handle, the two paddles in their cradles on its east side, the coiled cable."""
    g = "graphite"
    x0, x1, y0, y1, z0, z1 = 2.4, 8.8, 15.0, 18.6, 5.0, 9.8
    out = [box((x0 + 0.2, y0, z0), (x1 - 0.2, y1, z1), mat(g)), box((x0, y0 + 0.2, z0 + 0.2), (x0 + 0.2, y1 - 0.2, z1 - 0.2), mat(g)),
           box((x1 - 0.2, y0 + 0.2, z0 + 0.2), (x1, y1 - 0.2, z1 - 0.2), mat(g)),
           box((x0 + 0.4, y0 + 0.4, z0 - 0.15), (x1 - 0.4, y1 - 0.4, z0), mat("grip_black")),
           box((x0 + 0.6, y0 + 0.9, z0 - 0.2), (x0 + 3.8, y1 - 0.6, z0 - 0.15), faces("black", north=SCR.face("rk_defib_off", 3.2, 2.1, screen_off)))]
    out += [box((x0 + 4.2, y0 + 2.2, z0 - 0.3), (x0 + 5.0, y0 + 2.9, z0 - 0.15), mat("bin_yellow")),
            box((x0 + 4.2, y0 + 1.2, z0 - 0.3), (x0 + 5.0, y0 + 1.9, z0 - 0.15), mat("red_off")),
            box((x0 + 5.3, y0 + 2.2, z0 - 0.25), (x0 + 5.8, y0 + 2.6, z0 - 0.15), mat("white")),
            box((x0 + 5.3, y0 + 1.5, z0 - 0.25), (x0 + 5.8, y0 + 1.9, z0 - 0.15), mat("white"))]
    out += swap_yz(round_part(x0 + 5.5, y0 + 3.1, 0.3, z0 - 0.45, z0 - 0.15, "grip_black", n=3))
    out += [box((x0 + 1.0, y1, z0 + 0.4), (x1 - 1.0, y1 + 0.1, z0 + 0.8), mat("grip_black")),
            box((x0 + 1.2, y1, z0 + 2.2), (x0 + 1.6, y1 + 1.0, z0 + 2.8), mat("grip_black")), box((x1 - 1.6, y1, z0 + 2.2), (x1 - 1.2, y1 + 1.0, z0 + 2.8), mat("grip_black")),
            box((x0 + 1.2, y1 + 1.0, z0 + 2.2), (x1 - 1.2, y1 + 1.4, z0 + 2.8), mat("grip_black"))]
    for k, z in enumerate((5.4, 7.6)):
        out += [box((x1, y0 + 0.4, z), (x1 + 0.4, y0 + 3.0, z + 1.8), mat("grip_black")),
                box((x1 + 0.4, y0 + 0.6, z + 0.1), (x1 + 0.8, y0 + 2.8, z + 1.7), mat("bin_yellow")),
                box((x1 + 0.8, y0 + 1.2, z + 0.5), (x1 + 1.8, y0 + 2.2, z + 1.3), mat("pla_grey")),
                box((x1 + 1.8, y0 + 1.5, z + 0.75), (x1 + 2.4, y0 + 1.9, z + 1.05), mat("grip_black"))]
    out += loop_xy(x1 + 2.1, y0 + 1.0, 10.2, 0.9, 0.14)
    return out


def bvm():
    """A bag valve mask in its clear bag: the bag, the green squeeze bag inside, the mask, the reservoir folded."""
    out = [box((9.2, 15.0, 4.6), (14.2, 17.4, 7.2), mat("saline"))]
    out += rod_x(9.8, 12.6, 16.1, 5.9, 0.95, "glove_green", n=4) + rod_x(12.6, 13.2, 16.1, 5.9, 0.4, "pla_grey", n=3)
    out += [box((13.2, 15.4, 5.2), (13.9, 16.8, 6.6), mat("white"))]
    return out


def gloves_box(x0, z0, y0, colour, w=3.0, d=1.6, h=1.4):
    """A box of gloves with one pulled half out of its slot."""
    return [box((x0, y0, z0), (x0 + w, y0 + h, z0 + d), mat(colour)), box((x0 + 0.6, y0 + h, z0 + 0.5), (x0 + w - 0.6, y0 + h + 0.02, z0 + d - 0.5), mat("white")),
            box((x0 + w / 2 - 0.4, y0 + h, z0 + 0.6), (x0 + w / 2 + 0.4, y0 + h + 0.9, z0 + d - 0.6), mat("glove_" + colour.split("_")[-1] if colour.startswith("glove") else "white"),
                {"angle": 22.5, "axis": "z", "origin": [x0 + w / 2, y0 + h, z0 + d / 2]})]


def crash_cart():
    """The emergency trolley (75 cm wide, its top at 95 cm), its front to the north: the red body on four castors, five
    drawers (pulls, labels), the grey top in its raised rail; on it the monitor-defibrillator (off until it is used),
    the bag valve mask in its bag, a box of gloves; the sharps bin on its bracket, the push handle with the checklist
    clipboard hung on it, the CPR board on its back."""
    r = "cart_red"
    out = [box((2.0, 1.4, 4.0), (14.0, 14.4, 12.4), mat(r)),
           box((1.6, 14.4, 3.6), (14.4, 15.0, 12.8), mat("worktop")),
           box((1.6, 15.0, 3.6), (14.4, 15.6, 4.0), mat("worktop")), box((1.6, 15.0, 12.4), (14.4, 15.6, 12.8), mat("worktop")),
           box((1.6, 15.0, 4.0), (2.0, 15.6, 12.4), mat("worktop")), box((14.0, 15.0, 4.0), (14.4, 15.6, 12.4), mat("worktop"))]
    for k, (y0, y1, c) in enumerate(((12.0, 14.0, "red"), (9.8, 11.8, "bin_yellow"), (7.6, 9.6, "glove_blue"), (5.0, 7.4, "glove_green"), (1.8, 4.8, "white"))):
        yc = y0 + (y1 - y0) / 2
        out += [box((2.2, y0, 3.7), (13.8, y1, 4.0), mat(r)),
                box((4.6, yc - 0.15, 3.3), (11.4, yc + 0.15, 3.5), mat("chrome")),
                box((5.0, yc - 0.15, 3.5), (5.3, yc + 0.15, 3.7), mat("chrome")), box((10.7, yc - 0.15, 3.5), (11.0, yc + 0.15, 3.7), mat("chrome")),
                box((2.5, y1 - 0.5, 3.64), (3.5, y1 - 0.2, 3.7), mat(c))]
    for x0 in (2.2, 12.6):
        for z0 in (4.2, 11.0):
            out += [box((x0, 1.0, z0), (x0 + 1.2, 1.4, z0 + 1.2), mat("chrome")), box((x0 + 0.3, 0.5, z0 + 0.2), (x0 + 0.9, 1.0, z0 + 1.0), mat("chrome"))]
            out += rod_x(x0 + 0.25, x0 + 0.95, 0.5, z0 + 0.6, 0.5, "grip_black", n=3)
    out += defib() + bvm() + gloves_box(10.2, 8.6, 15.0, "glove_purple")
    out += [box((14.0, 9.0, 6.0), (14.4, 9.4, 10.0), mat("chrome")),
            box((14.4, 8.4, 6.2), (17.4, 13.2, 9.8), mat("bin_yellow")), box((14.3, 13.2, 6.1), (17.5, 13.9, 9.9), mat("red")),
            box((15.2, 13.9, 7.4), (16.6, 14.0, 8.6), mat("grip_black")),
            box((14.6, 10.0, 9.8), (17.2, 12.0, 9.9), faces("white", south=A.region("rk_sharps", 2.6, 2.0, sign(["SHARPS", "ONLY"], (200, 40, 36))))),
            box((0.6, 12.0, 5.0), (1.0, 13.6, 5.4), mat("chrome")), box((0.6, 12.0, 11.0), (1.0, 13.6, 11.4), mat("chrome")),
            box((0.2, 13.2, 5.0), (0.6, 13.6, 11.4), mat("chrome")),
            box((-0.1, 8.4, 6.4), (0.1, 13.2, 10.0), mat("cardboard")), box((-0.15, 8.6, 6.6), (-0.1, 12.6, 9.8), faces("paper", west=A.region("rk_check", 3.2, 4.0, TK.paper(921)))),
            box((-0.3, 12.6, 7.6), (0.1, 13.1, 8.8), mat("chrome")),
            box((3.0, 3.0, 12.4), (13.0, 14.0, 13.0), mat("grip_black")),
            box((4.0, 9.0, 3.6), (12.0, 10.6, 3.7), faces("white", north=A.region("rk_cart", 8.0, 1.6, label_plate("EMERGENCY TROLLEY", (246, 244, 236), (180, 30, 30)))))]
    return out


# ------------------------------------------------------------------ the hand-wash station (take 2)
def dispenser(x0, y0, z0, pad, label, key):
    """A wall soap or hand-rub dispenser (11 by 25 by 10 cm): the white body with its rounded top and front, the level
    window, the push pad at the front, the nozzle under it, its label."""
    w, h, d = 1.8, 4.0, 1.6
    out = [box((x0, y0 + 0.4, z0), (x0 + w, y0 + h - 0.3, z0 + d), mat("wsteel")), box((x0 + 0.15, y0 + h - 0.3, z0), (x0 + w - 0.15, y0 + h, z0 + d - 0.15), mat("wsteel")),
           box((x0 + 0.15, y0 + 0.4, z0 + d), (x0 + w - 0.15, y0 + h - 0.4, z0 + d + 0.12), mat("wsteel")),
           box((x0 + 0.5, y0 + 2.0, z0 + d + 0.12), (x0 + w - 0.5, y0 + 3.2, z0 + d + 0.14), mat("saline")),
           box((x0 + 0.2, y0 + 0.8, z0 + d + 0.12), (x0 + w - 0.2, y0 + 1.8, z0 + d + 0.35), mat(pad)),
           box((x0 + 0.2, y0, z0), (x0 + w - 0.2, y0 + 0.4, z0 + d - 0.2), mat("pla_grey")),
           box((x0 + 0.75, y0 - 0.2, z0 + d - 0.8), (x0 + 1.05, y0, z0 + d - 0.5), mat("pla_grey")),
           box((x0 + 0.25, y0 + 3.3, z0 + d + 0.12), (x0 + w - 0.25, y0 + 3.6, z0 + d + 0.14), faces("white", south=A.region(key, 1.3, 0.3, label_plate(label))))]
    return out


def towel_dispenser(x0, y0, z0):
    """A paper towel dispenser (28 by 36 by 13 cm): the white case, its smoked window, the lock, the slot underneath with
    a towel hanging out of it."""
    w, h, d = 4.4, 5.6, 2.1
    return [box((x0, y0 + 0.4, z0), (x0 + w, y0 + h, z0 + d), mat("wsteel")), box((x0 + 0.2, y0, z0), (x0 + w - 0.2, y0 + 0.4, z0 + d - 0.6), mat("wsteel")),
            box((x0 + 0.8, y0 + 2.4, z0 + d), (x0 + w - 0.8, y0 + 4.6, z0 + d + 0.05), mat("glass")),
            box((x0 + 1.0, y0 + 2.6, z0 + d - 0.6), (x0 + w - 1.0, y0 + 4.4, z0 + d - 0.2), mat("paper")),
            box((x0 + w / 2 - 0.3, y0 + 4.9, z0 + d), (x0 + w / 2 + 0.3, y0 + 5.3, z0 + d + 0.08), mat("pla_grey")),
            box((x0 + 0.9, y0 - 1.4, z0 + d - 0.75), (x0 + w - 0.9, y0 + 0.4, z0 + d - 0.65), mat("paper"))]


def glove_rack(x0, y0, z0):
    """A wall glove rack: the chrome frame holding three boxes of gloves on their sides (S, M, L), a glove poking out of
    each box's opening."""
    out = [box((x0, y0, z0), (x0 + 11.0, y0 + 0.2, z0 + 2.0), mat("chrome")), box((x0, y0 + 0.2, z0), (x0 + 0.2, y0 + 5.0, z0 + 2.0), mat("chrome")),
           box((x0 + 10.8, y0 + 0.2, z0), (x0 + 11.0, y0 + 5.0, z0 + 2.0), mat("chrome")),
           box((x0, y0 + 0.9, z0 + 1.8), (x0 + 11.0, y0 + 1.1, z0 + 2.0), mat("chrome"))]
    for k, (c, size) in enumerate((("glove_blue", "S"), ("glove_purple", "M"), ("glove_green", "L"))):
        bx = x0 + 0.3 + k * 3.5
        out += [box((bx, y0 + 0.2, z0), (bx + 3.4, y0 + 4.8, z0 + 1.6), mat(c)),
                box((bx + 0.7, y0 + 1.6, z0 + 1.6), (bx + 2.7, y0 + 3.4, z0 + 1.62), mat("white")),
                box((bx + 1.2, y0 + 1.8, z0 + 1.62), (bx + 2.2, y0 + 2.8, z0 + 2.3), mat(c)),
                box((bx + 0.4, y0 + 3.8, z0 + 1.6), (bx + 1.4, y0 + 4.5, z0 + 1.65), faces("white", south=A.region(f"rk_gl_{size}", 1.0, 0.7, label_plate(size))))]
    return out


def tap(x, z, y0):
    """A clinical mixer tap: the round body, its rise, a 45 degree bend, the reach over the basin, the spout turned down,
    the long elbow lever to the side."""
    out = round_part(x, z, 0.62, y0, y0 + 0.3, "chrome", n=3) + round_part(x, z, 0.5, y0 + 0.3, y0 + 1.8, "chrome", n=3)
    out += round_part(x, z, 0.3, y0 + 1.8, y0 + 5.6, "chrome", n=3)
    out += cable([(x, y0 + 5.6, z), (x, y0 + 6.4, z + 0.8), (x, y0 + 6.4, z + 3.6), (x, y0 + 5.6, z + 4.4), (x, y0 + 4.9, z + 4.4)], 0.28, "chrome")
    out += round_part(x, z + 4.4, 0.34, y0 + 4.5, y0 + 4.9, "steel", n=3)
    out += [box((x + 0.5, y0 + 1.0, z - 0.2), (x + 4.6, y0 + 1.3, z + 0.2), mat("chrome")), box((x + 4.3, y0 + 1.0, z - 0.25), (x + 4.7, y0 + 1.7, z + 0.25), mat("chrome")),
            box((x - 0.18, y0 + 1.15, z + 0.5), (x + 0.18, y0 + 1.5, z + 0.58), mat("red")), box((x - 0.18, y0 + 0.65, z + 0.5), (x + 0.18, y0 + 1.0, z + 0.58), mat("glove_blue"))]
    return out


def scrub_sink():
    """The hand-wash station (2 x 1, two high, built whole along x 0..32, its user looking north at the wall at z 0): the
    white cabinets on their recessed kick plate (a door with its finger pull, a stack of drawers), the solid-surface
    worktop with its upstand and front edge, the white ceramic basin set into it (its rounded bowl, the drain and the
    overflow), the clinical mixer tap and its elbow lever; a disinfectant spray and tissues on the worktop; on the tiled
    wall the soap and hand-rub dispensers, the paper towels, the glove rack (S, M, L), the sharps bin on its bracket and
    the hand hygiene poster."""
    out = [box((0.8, 0, 1.4), (31.2, 0.9, 13.8), mat("grip_black")),
           box((0.2, 0.9, 0.6), (31.8, 8.0, 14.6), mat("white")),
           box((0.2, 8.0, 0.6), (3.6, 13.8, 14.6), mat("white")), box((13.8, 8.0, 0.6), (31.8, 13.8, 14.6), mat("white")),
           box((3.6, 8.0, 0.6), (13.8, 13.8, 2.8), mat("white")), box((3.6, 8.0, 12.6), (13.8, 13.8, 14.6), mat("white"))]
    out += [box((0.4, 1.1, 14.6), (15.8, 13.4, 14.9), mat("white")), box((12.6, 12.6, 14.9), (15.4, 12.9, 15.2), mat("chrome"))]
    for k, (y0, y1) in enumerate(((9.4, 13.4), (5.2, 9.2), (1.1, 5.0))):
        out += [box((16.2, y0, 14.6), (31.6, y1, 14.9), mat("white")), box((21.4, y1 - 0.9, 14.9), (26.4, y1 - 0.6, 15.2), mat("chrome")),
                box((22.8, y1 - 2.0, 14.9), (25.0, y1 - 1.5, 14.95), mat("pla_grey"))]
    wt = "worktop"
    out += [box((0, 13.8, 0.3), (32, 14.6, 2.8), mat(wt)), box((0, 13.8, 12.6), (32, 14.6, 15.4), mat(wt)),
            box((0, 13.8, 2.8), (3.6, 14.6, 12.6), mat(wt)), box((13.8, 13.8, 2.8), (32, 14.6, 12.6), mat(wt)),
            box((0, 14.6, 0.3), (32, 15.6, 0.7), mat(wt)), box((0, 13.6, 15.4), (32, 14.6, 15.6), mat(wt))]
    # the ceramic basin: its stepped rounded bowl under the opening (x 3.6..13.8, z 2.8..12.6)
    cer = "ceramic"
    for k, (inset, ytop) in enumerate(((0.0, 14.6), (0.7, 11.6), (1.6, 10.4), (2.6, 9.6))):
        x0, x1, z0, z1 = 3.6 + inset, 13.8 - inset, 2.8 + inset, 12.6 - inset
        cer = ("ceramic", "ceramic_mid", "ceramic_mid", "ceramic_deep")[k]
        if k == 0:
            out += [box((x0, 11.6, z0), (x0 + 0.7, 14.6, z1), mat(cer)), box((x1 - 0.7, 11.6, z0), (x1, 14.6, z1), mat(cer)),
                    box((x0 + 0.7, 11.6, z0), (x1 - 0.7, 14.6, z0 + 0.7), mat(cer)), box((x0 + 0.7, 11.6, z1 - 0.7), (x1 - 0.7, 14.6, z1), mat(cer))]
        else:
            nx0, nx1, nz0, nz1 = 3.6 + inset + 0.9, 13.8 - inset - 0.9, 2.8 + inset + 0.9, 12.6 - inset - 0.9
            out += [box((x0, ytop - 1.2, z0), (nx0, ytop, z1), mat(cer)), box((nx1, ytop - 1.2, z0), (x1, ytop, z1), mat(cer)),
                    box((nx0, ytop - 1.2, z0), (nx1, ytop, nz0), mat(cer)), box((nx0, ytop - 1.2, nz1), (nx1, ytop, z1), mat(cer))]
    out += [box((7.1, 8.4, 6.3), (10.3, 9.6, 9.1), mat("ceramic_deep"))]
    out += round_part(8.7, 7.7, 0.6, 9.6, 9.7, "steel", n=3) + round_part(8.7, 7.7, 0.3, 9.7, 9.72, "grip_black", n=3)
    out += [box((8.0, 13.0, 3.45), (9.4, 13.4, 3.5), mat("grip_black"))]
    out += tap(8.7, 1.4, 14.6)
    out += [box((17.0, 14.6, 4.0), (18.4, 17.8, 5.2), mat("white")), box((17.2, 17.8, 4.2), (18.2, 18.6, 5.0), mat("red")),
            box((17.5, 18.0, 5.0), (17.9, 18.4, 5.6), mat("red")), box((17.1, 15.6, 5.2), (18.3, 17.0, 5.22), faces("white", south=A.region("rk_spray", 1.2, 1.4, label_plate("DISINFECT"))))]
    out += [box((20.0, 14.6, 6.0), (23.6, 16.2, 8.4), mat("glove_blue")), box((21.2, 16.2, 6.8), (22.4, 16.8, 7.6), mat("white"))]
    # the wall over it
    out += [box((0, 15.6, 0), (32, 30.0, 0.3), {**mat("tile"), "down": {"texture": "#tile"}})]
    out += dispenser(2.2, 21.4, 0.3, "pla_grey", "SOAP", "rk_lab_soap") + dispenser(11.6, 21.4, 0.3, "glove_blue", "HAND RUB", "rk_lab_rub")
    out += towel_dispenser(14.6, 23.2, 0.3)
    out += glove_rack(15.0, 16.0, 0.3)
    out += [box((28.4, 16.8, 0.3), (30.8, 20.4, 0.6), mat("chrome")), box((28.0, 17.0, 0.6), (31.2, 22.2, 3.4), mat("bin_yellow")),
            box((27.9, 22.2, 0.5), (31.3, 22.8, 3.5), mat("red")), box((28.8, 22.8, 1.4), (30.4, 22.9, 2.6), mat("grip_black")),
            box((28.3, 18.4, 3.4), (30.9, 20.6, 3.45), faces("white", south=A.region("rk_sharps2", 2.6, 2.2, sign(["SHARPS", "ONLY"], (200, 40, 36)))))]
    out += [box((20.4, 22.0, 0.3), (23.8, 28.4, 0.4), faces("white", south=A.region("rk_poster_small", 3.4, 6.4, poster("hands"))))]
    return out


# ------------------------------------------------------------------ the medicine cupboard (take 2)
def medicine_cupboard():
    """A wall cupboard for medicines (75 by 56 cm), its front to the north (the wall to the south): the white case and its
    cornice, two glass doors in chrome frames with their lock, three shelves of labelled boxes and bottles."""
    out = [box((2.0, 2.6, 11.4), (14.0, 12.4, 16.0), mat("white")), box((1.8, 12.4, 11.2), (14.2, 13.0, 16.0), mat("white")),
           box((2.0, 2.2, 11.6), (14.0, 2.6, 16.0), mat("white"))]
    for x0, x1 in ((2.0, 8.0), (8.0, 14.0)):
        out += [box((x0, 2.6, 11.0), (x0 + 0.35, 12.4, 11.4), mat("chrome")), box((x1 - 0.35, 2.6, 11.0), (x1, 12.4, 11.4), mat("chrome")),
                box((x0 + 0.35, 2.6, 11.0), (x1 - 0.35, 2.95, 11.4), mat("chrome")), box((x0 + 0.35, 12.05, 11.0), (x1 - 0.35, 12.4, 11.4), mat("chrome")),
                box((x0 + 0.35, 2.95, 11.15), (x1 - 0.35, 12.05, 11.25), mat("glass"))]
    out += [box((7.6, 6.8, 10.7), (7.9, 8.4, 11.0), mat("chrome")), box((8.1, 6.8, 10.7), (8.4, 8.4, 11.0), mat("chrome")),
            box((7.4, 12.5, 11.1), (8.6, 12.9, 11.2), mat("aid_green")), box((7.85, 12.42, 11.1), (8.15, 12.98, 11.2), mat("aid_green"))]
    rng = random.Random(3470)
    for y0 in (2.95, 6.1, 9.2):
        out.append(box((2.4, y0 - 0.2, 11.6), (13.6, y0, 15.8), mat("white")))
        x = 2.6
        while x < 13.0:
            kind = rng.choice(("box", "box", "box", "bottle", "tape"))
            room = 2.9
            if kind == "box":
                name, col = rng.choice(DRUGS)
                w = rng.choice((1.4, 1.8, 2.2))
                h = round(rng.uniform(1.6, room - 0.2), 1)
                if x + w > 13.4:
                    break
                out.append(box((x, y0, 12.2), (x + w, y0 + h, 15.0), faces("white", north=A.region(f"rk_drug_{name}_{w}_{h}", w, h, drug_box(name, col)))))
                x += w + 0.15
            elif kind == "bottle":
                c = rng.choice(("white", "pla_orange", "glove_blue"))
                out += round_part(x + 0.5, 13.6, 0.5, y0, y0 + 1.9, c, n=3) + round_part(x + 0.5, 13.6, 0.3, y0 + 1.9, y0 + 2.4, "white", n=3)
                x += 1.2
            else:
                out += swap_yz(round_part(x + 0.6, y0 + 0.6, 0.6, 12.6, 13.6, "white", n=3))
                x += 1.4
    return out


# ------------------------------------------------------------------ the medical office's added pieces (take 2)
def castor(x0, z0):
    """A castor under a trolley's leg: its plate, the fork, the wheel."""
    return [box((x0, 1.0, z0), (x0 + 1.2, 1.4, z0 + 1.2), mat("chrome")), box((x0 + 0.3, 0.5, z0 + 0.2), (x0 + 0.9, 1.0, z0 + 1.0), mat("chrome"))] + \
        rod_x(x0 + 0.25, x0 + 0.95, 0.5, z0 + 0.6, 0.5, "grip_black", n=3)


def lipped_tray(x0, x1, z0, z1, y0, base=0.3, lip=0.6, wall=0.2, tex="stainless"):
    """A flat tray with its raised lip all round."""
    return [box((x0, y0, z0), (x1, y0 + base, z1), mat(tex)),
            box((x0, y0 + base, z0), (x1, y0 + base + lip, z0 + wall), mat(tex)), box((x0, y0 + base, z1 - wall), (x1, y0 + base + lip, z1), mat(tex)),
            box((x0, y0 + base, z0 + wall), (x0 + wall, y0 + base + lip, z1 - wall), mat(tex)), box((x1 - wall, y0 + base, z0 + wall), (x1, y0 + base + lip, z1 - wall), mat(tex))]


def dressing_pack_pic(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(238, 242, 246))
    d.rectangle((0, 0, W, H * 0.24), fill=(40, 90, 170))
    d.text((W / 2, H * 0.12), "STERILE", font=_fit(d, "STERILE", FONT, round(H * 0.17), W - 6), fill=(255, 255, 255), anchor="mm")
    for k, t in enumerate(("DRESSING PACK", "single use only", "do not use if opened")):
        d.text((W / 2, H * (0.4 + k * 0.15)), t, font=_fit(d, t, FONT if k == 0 else FONT_R, round(H * 0.11), W - 8), fill=(30, 40, 60), anchor="mm")
    d.polygon([(W * 0.12, H * 0.92), (W * 0.2, H * 0.86), (W * 0.2, H * 0.98)], fill=(40, 90, 170))
    d.text((W * 0.24, H * 0.92), "peel here", font=_fit(d, "peel here", FONT_R, round(H * 0.08), W * 0.5), fill=(40, 90, 170), anchor="lm")
    return img


def dressing_trolley():
    """A stainless dressing trolley (62 by 42 cm, 95 cm to its handle), its long side to the north: four round legs on
    castors, the lower shelf and the top tray in their lips, the push handle at the east end; on the top a sterile dressing
    pack, an instrument tray with scissors and forceps, a gallipot, saline pods, a roll of tape and a pack of swabs;
    below, a box of gloves, a box of dressings, an apron roll and a skin wash bottle."""
    s = "stainless"
    out = []
    for cx in (3.6, 12.4):
        for cz in (5.2, 10.8):
            out += castor(cx - 0.6, cz - 0.6) + round_part(cx, cz, 0.3, 1.4, 13.4, s, n=3)
    out += lipped_tray(3.0, 13.0, 4.6, 11.4, 4.0, base=0.3, lip=0.5) + lipped_tray(3.0, 13.0, 4.6, 11.4, 13.4, base=0.3, lip=0.6)
    out += round_part(13.7, 5.6, 0.22, 12.8, 15.4, "chrome", n=3) + round_part(13.7, 10.4, 0.22, 12.8, 15.4, "chrome", n=3)
    out += swap_yz(round_part(13.7, 15.4, 0.24, 5.3, 10.7, "chrome", n=3))
    out += [box((13.0, 13.0, 5.4), (13.5, 13.4, 5.8), mat("chrome")), box((13.0, 13.0, 10.2), (13.5, 13.4, 10.6), mat("chrome"))]
    t = 13.7
    out.append(box((3.6, t, 5.2), (7.6, t + 0.5, 8.6), faces("paper", up=SCR.face("rk_dpack", 4.0, 3.4, dressing_pack_pic))))
    out += lipped_tray(8.0, 12.4, 5.2, 8.4, t, base=0.15, lip=0.35, wall=0.15)
    tt = t + 0.15
    out += [box((8.6, tt, 6.2), (10.4, tt + 0.12, 6.44), mat("chrome")),
            box((8.6, tt + 0.12, 6.2), (10.4, tt + 0.24, 6.44), mat("chrome"), {"angle": -22.5, "axis": "y", "origin": [10.4, tt, 6.32]}),
            box((10.4, tt, 6.1), (10.9, tt + 0.24, 6.54), mat("chrome"))]
    for rz in (5.6, 6.5):
        out += [box((10.9, tt, rz), (11.7, tt + 0.16, rz + 0.12), mat("grip_black")), box((10.9, tt, rz + 0.48), (11.7, tt + 0.16, rz + 0.6), mat("grip_black")),
                box((10.9, tt, rz + 0.12), (11.02, tt + 0.16, rz + 0.48), mat("grip_black")), box((11.58, tt, rz + 0.12), (11.7, tt + 0.16, rz + 0.48), mat("grip_black"))]
    out += [box((8.6, tt, 7.3), (11.8, tt + 0.12, 7.42), mat("chrome")), box((8.6, tt, 7.6), (11.8, tt + 0.12, 7.72), mat("chrome")),
            box((11.6, tt, 7.42), (11.8, tt + 0.12, 7.6), mat("chrome"))]
    out += round_ring(4.8, 9.9, 0.9, 0.12, t, t + 0.8, s, 0.15, s, n=4)
    for q in range(3):
        out += [box((6.3 + q * 0.6, t, 9.0), (6.8 + q * 0.6, t + 0.4, 10.6), mat("white")), box((6.4 + q * 0.6, t, 10.6), (6.7 + q * 0.6, t + 0.3, 11.0), mat("white"))]
    out += round_ring(9.0, 9.9, 0.7, 0.3, t, t + 0.6, "white", 0.05, s, n=4)
    out += [box((10.2, t, 9.0), (12.6, t + 0.5, 10.9), faces("paper", up=SCR.face("rk_swabs", 2.4, 1.9, label_plate("GAUZE SWABS", (240, 244, 248), (40, 90, 170)))))]
    b = 4.3
    out += gloves_box(4.1, 5.6, b, "glove_blue")
    out.append(box((7.0, b, 5.2), (10.6, b + 2.3, 8.0), faces("cardboard", north=A.region("rk_dbox", 3.6, 2.3, label_plate("DRESSINGS 10x10", (236, 230, 214), (40, 60, 120))))))
    out += rod_x(4.2, 7.2, b + 0.9, 9.5, 0.9, "white", n=4)
    out += round_part(11.2, 9.8, 0.6, b, b + 2.6, "glove_blue", n=3) + round_part(11.2, 9.8, 0.3, b + 2.6, b + 3.2, "white", n=3)
    return out


def diagnostic_set():
    """A wall diagnostic set (50 by 28 cm, at 1.2 to 1.45 m), its front to the north (the wall to the south): the white
    transformer plate and its grey base with the power light, the two handle cradles, the otoscope (its head, speculum and
    lens) and the ophthalmoscope (its disc head, the sight hole, the lens wheel) standing in them, their coiled cords
    hanging in loops back to the base, the specula dispenser beside them."""
    out = [box((3.0, 3.0, 15.2), (11.4, 7.4, 16.0), mat("white")), box((3.0, 3.0, 14.4), (11.4, 4.0, 15.2), mat("pla_grey")),
           box((10.4, 3.4, 14.36), (10.7, 3.6, 14.4), mat("glove_green"))]
    for hx in (5.0, 8.0):
        out += [box((hx - 0.5, 5.4, 14.0), (hx + 0.5, 6.6, 15.2), mat("graphite")),
                box((hx - 0.62, 5.6, 13.0), (hx + 0.62, 6.4, 13.12), mat("graphite")),
                box((hx - 0.62, 5.6, 13.12), (hx - 0.5, 6.4, 14.0), mat("graphite")), box((hx + 0.5, 5.6, 13.12), (hx + 0.62, 6.4, 14.0), mat("graphite"))]
        out += round_part(hx, 13.56, 0.42, 4.6, 7.6, "grip_black", n=3) + round_part(hx, 13.56, 0.46, 7.6, 7.8, "chrome", n=3)
        out += cable([(hx, 4.6, 13.56), (hx, 1.4, 13.56), (hx + 0.6, 0.8, 13.56), (hx + 0.9, 0.8, 13.56), (hx + 1.5, 1.4, 13.56), (hx + 1.5, 2.4, 13.56),
                      (hx + 1.5, 2.4, 14.8), (hx + 1.5, 3.0, 14.8)], 0.17, "coil")
    out += round_part(5.0, 13.56, 0.5, 7.8, 8.9, "grip_black", n=3)
    out += swap_yz(round_part(5.0, 8.4, 0.34, 12.5, 13.1, "pla_grey", n=3)) + swap_yz(round_part(5.0, 8.4, 0.2, 11.9, 12.5, "pla_grey", n=3))
    out += swap_yz(round_part(5.0, 8.4, 0.38, 14.06, 14.2, "chrome", n=3))
    out += [box((7.5, 7.8, 13.3), (8.5, 9.6, 13.8), mat("grip_black")), box((7.65, 9.6, 13.3), (8.35, 9.9, 13.8), mat("grip_black")),
            box((7.88, 9.0, 13.26), (8.12, 9.24, 13.3), mat("black")), box((8.5, 8.3, 13.36), (8.62, 9.2, 13.74), mat("graphite"))]
    out += [box((9.6, 3.6, 13.8), (11.0, 7.0, 15.2), mat("white")), box((9.75, 4.4, 13.76), (10.85, 6.6, 13.8), mat("glass")),
            box((9.9, 4.6, 13.9), (10.7, 6.4, 14.4), mat("pla_grey")), box((9.8, 3.6, 13.76), (10.8, 3.8, 13.8), mat("grip_black"))]
    return out


def ruler_pic(top_cm, bottom_cm):
    """A height rod's scale: a tick each centimetre, longer at 5, numbered at 10."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 246, 242))
        f = ImageFont.truetype(FONT, max(8, round(W * 0.3)))
        span = top_cm - bottom_cm
        for c in range(math.ceil(bottom_cm), math.floor(top_cm) + 1):
            y = (top_cm - c) / span * H
            ln = W * (0.55 if c % 10 == 0 else 0.4 if c % 5 == 0 else 0.22)
            d.line((0, y, ln, y), fill=(30, 30, 34), width=max(1, round(W / 40)))
            if c % 10 == 0 and W * 0.4 < y < H - 4:
                d.text((W * 0.97, y - 2), str(c), font=f, fill=(30, 30, 34), anchor="rb")
        return img
    return draw


def beam_pic(maxv, step, label_every):
    """A scale beam's graduations: a notch each step, numbered."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(216, 220, 224))
        f = ImageFont.truetype(FONT, max(8, round(H * 0.4)))
        n = round(maxv / step)
        for k in range(n + 1):
            x = W * 0.04 + k * W * 0.92 / n
            v = k * step
            major = abs(v / label_every - round(v / label_every)) < 1e-6
            d.line((x, H * 0.55, x, H * (0.05 if major else 0.3)), fill=(30, 30, 34), width=max(1, round(H / 30)))
            if major:
                d.text((x, H * 0.8), f"{v:g}", font=f, fill=(30, 30, 34), anchor="mm")
        return img
    return draw


def column_scale():
    """A column scale with its height rod (to 1.9 m), its front to the north (the wall to the south): the low platform
    and its black rubber mat, the white column rising from its back, the beam head (the two graduated beams with their
    sliding poises, the balance pointer in its loop), the height rod above it with its markings, the head plate folded
    out on its slider."""
    out = [box((4.6, 0, 2.6), (11.4, 0.7, 13.6), mat("white")), box((5.0, 0.7, 3.0), (11.0, 0.85, 11.0), mat("grip_black")),
           box((7.0, 0.7, 11.6), (9.0, 16.0, 13.4), mat("white")), box((7.2, 2.0, 11.56), (8.8, 2.4, 11.6), mat("pla_grey")),
           box((3.4, 16.0, 11.2), (12.6, 18.8, 13.8), mat("white"))]
    for y0, key, pic in ((16.5, "rk_beam_lo", beam_pic(10, 0.1, 1)), (17.7, "rk_beam_hi", beam_pic(200, 10, 50))):
        out.append(box((3.8, y0, 10.8), (12.2, y0 + 0.7, 11.2), faces("chrome", north=SCR.face(key, 8.4, 0.7, pic))))
    out += [box((5.9, 16.4, 10.4), (6.7, 17.3, 10.8), mat("steel")), box((5.95, 17.3, 10.6), (6.65, 17.5, 10.8), mat("steel")),
            box((8.5, 17.6, 10.4), (9.5, 18.5, 10.8), mat("steel")), box((8.55, 18.5, 10.6), (9.45, 18.7, 10.8), mat("steel")),
            box((12.6, 17.2, 11.8), (13.5, 17.36, 12.0), mat("black")),
            box((12.6, 16.6, 11.7), (13.8, 16.7, 12.1), mat("pla_grey")), box((12.6, 17.9, 11.7), (13.8, 18.0, 12.1), mat("pla_grey")),
            box((13.7, 16.7, 11.7), (13.8, 17.9, 12.1), mat("pla_grey"))]
    out.append(box((7.6, 18.8, 12.0), (8.4, 30.0, 12.8), faces("white", north=SCR.face("rk_rod", 0.8, 11.2, ruler_pic(30.0 * 6.25, 18.8 * 6.25)))))
    out += [box((7.5, 30.0, 11.9), (8.5, 30.3, 12.9), mat("pla_grey")),
            box((7.4, 27.0, 12.8), (8.6, 28.2, 13.0), mat("pla_grey")), box((7.4, 27.0, 11.8), (7.6, 28.2, 12.8), mat("pla_grey")),
            box((8.4, 27.0, 11.8), (8.6, 28.2, 12.8), mat("pla_grey")), box((7.6, 27.7, 11.8), (8.4, 28.2, 12.0), mat("pla_grey")),
            box((7.5, 27.8, 7.4), (8.5, 28.1, 11.8), mat("white"))]
    return out


# ------------------------------------------------------------------ the desk (take 2)
def med_desk():
    """The first aider's desk (2 x 1, built whole along x 0..32, its user looking north): the white top on its panel legs,
    the modesty panel, the drawers; the monitor (the first aid log, live) and its stand, keyboard and mouse; the blood
    pressure monitor and its cuff; the ear thermometer in its base; a box of gloves; the logbook open; a document tray
    with papers; the stapler; a binder; the desk phone; a sanitiser bottle; the pen pot."""
    t = 12.0
    out = [box((0, t - 0.6, 0.8), (32, t, 15.4), mat("white")),
           box((0.4, 0, 1.6), (1.2, t - 0.6, 14.6), mat("white")), box((30.8, 0, 1.6), (31.6, t - 0.6, 14.6), mat("white")),
           box((1.2, 4.0, 1.6), (30.8, t - 0.6, 2.0), mat("white")),
           box((24.0, 0.4, 2.0), (30.8, t - 0.6, 14.4), mat("white"))]
    for y0, y1 in ((7.6, 11.2), (4.0, 7.4), (0.6, 3.8)):
        out += [box((24.2, y0, 14.4), (30.6, y1, 14.7), mat("white")), box((25.8, y1 - 0.9, 14.7), (29.0, y1 - 0.6, 15.0), mat("chrome"))]
    out += [box((12.8, t, 1.6), (15.2, t + 0.4, 3.6), mat("graphite")), box((13.7, t + 0.4, 2.2), (14.3, t + 3.0, 2.8), mat("graphite")),
            {**box((9.4, t + 2.4, 2.8), (18.6, t + 8.2, 3.2), faces("black", south=SCR.face("rk_log", 9.2, 5.8, patient_log))), "shade": False},
            box((10.0, t, 9.0), (18.0, t + 0.4, 11.6), faces("black", up=A.region("rk_kbd", 8.0, 2.6, MK.keyboard_top))),
            box((19.0, t, 9.6), (20.2, t + 0.5, 11.4), mat("black"))]
    out += [box((2.0, t, 3.0), (6.0, t + 1.6, 6.0), mat("white")),
            box((2.4, t + 0.6, 2.9), (4.4, t + 1.4, 3.0), faces("black", north=SCR.face("rk_bp", 2.0, 0.8, label_plate("118/76  72", (20, 30, 40), (150, 220, 255), live={"part": "72", "src": "value", "base": 72, "spread": 5, "period": 12})))),
            box((4.6, t + 0.9, 3.4), (5.6, t + 1.4, 4.0), mat("white"))]
    out += bevel_box((2.0, t, 6.4), (6.0, t + 0.9, 8.8), "navy", b=0.2)
    out += cable([(5.6, t + 0.4, 6.0), (5.6, t + 0.4, 6.6)], 0.1, "pla_grey")
    out += [box((2.0, t, 10.0), (5.4, t + 0.3, 14.4), faces("paper", up=A.region("rk_logbook_l", 3.4, 4.4, TK.paper(901)))),
            box((5.4, t, 10.0), (8.8, t + 0.3, 14.4), faces("paper", up=A.region("rk_logbook_r", 3.4, 4.4, TK.paper(902)))),
            box((5.3, t - 0.05, 9.9), (5.5, t + 0.35, 14.5), mat("navy"))]
    out += gloves_box(20.6, 3.0, t, "glove_blue", w=3.0, d=1.6, h=1.4)
    out += [box((25.6, t, 4.0), (30.0, t + 0.6, 8.0), mat("black")),
            box((26.6, t + 0.4, 4.2), (29.8, t + 0.8, 7.8), faces("black", up=SCR.face("rk_phone", 3.2, 3.6, MK.phone_face)),
                {"angle": 22.5, "axis": "x", "origin": [28.2, t + 0.6, 6.0]})]
    out += round_part(22.4, 7.4, 0.5, t, t + 2.0, "white", n=3) + round_part(22.4, 7.4, 0.25, t + 2.0, t + 2.6, "glove_blue", n=3)
    out += round_ring(28.8, 11.4, 0.6, 0.1, t, t + 1.4, "black", 0.2, "black", n=4)
    out += [box((28.6, t + 0.2, 11.2), (28.75, t + 2.2, 11.35), mat("glove_blue")), box((28.9, t + 0.2, 11.4), (29.05, t + 2.0, 11.55), mat("red"))]
    # the ear thermometer on its base, the document tray, the stapler, a binder standing
    out += [box((7.0, t, 3.4), (8.6, t + 0.5, 5.0), mat("white")), box((7.4, t + 0.5, 3.8), (8.2, t + 2.2, 4.6), mat("white")),
            box((7.55, t + 2.2, 3.4), (8.05, t + 2.6, 3.9), mat("glove_blue"))]
    out += [box((20.6, t, 9.4), (24.2, t + 0.15, 13.8), mat("grip_black")), box((20.6, t + 0.15, 9.4), (24.2, t + 0.8, 9.55), mat("grip_black")),
            box((20.6, t + 0.15, 13.65), (24.2, t + 0.8, 13.8), mat("grip_black")),
            box((20.8, t + 0.15, 9.6), (24.0, t + 0.55, 13.6), faces("paper", up=A.region("rk_tray_sheet", 3.2, 4.0, page_sheet_simple)))]
    out += [box((25.4, t, 9.6), (27.8, t + 0.45, 10.4), mat("grip_black")), box((25.4, t + 0.45, 9.6), (27.4, t + 0.75, 10.4), mat("chrome"))]
    out += [box((1.6, t, 1.2), (2.6, t + 4.2, 2.8), mat("aid_green")), box((1.55, t + 1.4, 1.4), (1.6, t + 3.0, 2.6), mat("white"))]
    return out


def page_sheet_simple(img):
    return MK.page_sheet(931)(img)


# ------------------------------------------------------------------ the lockers' bag, the boots, the bunk (take 2)
def duffel(x0, z0, y0, colour="navy"):
    """A kit bag lying on its side (60 cm long, 30 across): the round body (its bottom panel black), the end panels, the
    zip along its top, the two carry handles, the shoulder strap lying over it, the tower's patch."""
    cx, cy, cz, r, ln = x0 + 4.8, y0 + 2.2, z0 + 2.2, 2.2, 9.6
    out = rod_x(x0 + 0.3, x0 + ln - 0.3, cy, cz, r, colour, n=6)
    out += rod_x(x0, x0 + 0.3, cy, cz, r - 0.2, "grip_black", n=5) + rod_x(x0 + ln - 0.3, x0 + ln, cy, cz, r - 0.2, "grip_black", n=5)
    out += [box((x0 + 0.6, y0, cz - 1.6), (x0 + ln - 0.6, y0 + 0.4, cz + 1.6), mat("grip_black")),
            box((x0 + 0.8, cy + r - 0.02, cz - 0.15), (x0 + ln - 0.8, cy + r + 0.08, cz + 0.15), mat("pla_grey"))]
    for hx in (x0 + 3.0, x0 + 6.6):
        out += [box((hx - 0.3, cy + r - 0.1, cz - 0.9), (hx + 0.3, cy + r + 0.9, cz - 0.6), mat("grip_black")),
                box((hx - 0.3, cy + r - 0.1, cz + 0.6), (hx + 0.3, cy + r + 0.9, cz + 0.9), mat("grip_black")),
                box((hx - 0.3, cy + r + 0.6, cz - 0.6), (hx + 0.3, cy + r + 0.9, cz + 0.6), mat("grip_black"))]
    out += [box((x0 + 1.4, cy + r - 0.4, cz + 0.9), (x0 + 1.9, cy + r + 0.05, cz + r), mat("grip_black"), {"angle": -22.5, "axis": "x", "origin": [x0 + 1.65, cy + r, cz + 0.9]}),
            box((x0 + 7.8, cy + r - 0.4, cz + 0.9), (x0 + 8.3, cy + r + 0.05, cz + r), mat("grip_black"), {"angle": -22.5, "axis": "x", "origin": [x0 + 8.05, cy + r, cz + 0.9]}),
            box((x0 + 3.6, cy - 0.5, cz - r - 0.02), (x0 + 6.0, cy + 0.9, cz - r + 0.12), faces(colour, north=A.region("rk_patch", 2.4, 1.4, label_plate("TOWER CREW", (30, 46, 82), (232, 150, 40)))))]
    return out


def boot(x0, z0, y0, turn=0.0):
    """A work boot (28 cm), its toe to the north: the lugged sole (its tread blocks, the raised heel), the rounded toe cap,
    the leather upper rising to the padded collar, the tongue between the quarters, the laces across it from the eyelets
    up to the speed hooks, the darker heel counter, the pull tab."""
    s, u, dk = "grip_black", "boot", "boot_dark"
    out = [box((x0 + 0.3, y0 + 0.1, z0), (x0 + 1.5, y0 + 0.4, z0 + 0.35), mat(s)), box((x0, y0 + 0.1, z0 + 0.35), (x0 + 1.8, y0 + 0.4, z0 + 3.0), mat(s)),
           box((x0 + 0.05, y0, z0 + 3.0), (x0 + 1.75, y0 + 0.6, z0 + 4.4), mat(s))]
    for k in range(5):
        out.append(box((x0 + 0.1, y0, z0 + 0.4 + k * 0.48), (x0 + 1.7, y0 + 0.1, z0 + 0.7 + k * 0.48), mat(s)))
    out += [box((x0 + 0.3, y0 + 0.4, z0 + 0.05), (x0 + 1.5, y0 + 0.95, z0 + 0.4), mat(dk)), box((x0 + 0.12, y0 + 0.4, z0 + 0.4), (x0 + 1.68, y0 + 1.2, z0 + 1.5), mat(dk)),
            box((x0 + 0.3, y0 + 1.2, z0 + 0.55), (x0 + 1.5, y0 + 1.35, z0 + 1.5), mat(dk)),
            box((x0 + 0.12, y0 + 0.4, z0 + 1.5), (x0 + 1.68, y0 + 1.8, z0 + 2.0), mat(u)),
            box((x0 + 0.12, y0 + 0.4, z0 + 2.0), (x0 + 1.68, y0 + 2.8, z0 + 3.9), mat(u)),
            box((x0 + 0.1, y0 + 0.6, z0 + 3.9), (x0 + 1.7, y0 + 2.8, z0 + 4.4), mat(dk)),
            box((x0 + 0.5, y0 + 1.8, z0 + 1.9), (x0 + 1.3, y0 + 3.3, z0 + 2.0), mat(u)),
            box((x0 + 0.08, y0 + 2.8, z0 + 2.0), (x0 + 1.72, y0 + 3.15, z0 + 4.45), mat(dk)),
            box((x0 + 0.7, y0 + 2.5, z0 + 4.45), (x0 + 1.1, y0 + 3.4, z0 + 4.6), mat(s))]
    for k in range(4):
        y = y0 + 1.9 + k * 0.32
        out += [box((x0 + 0.5, y, z0 + 1.82), (x0 + 1.3, y + 0.1, z0 + 1.9), mat(s)),
                box((x0 + 0.3, y, z0 + 1.92), (x0 + 0.5, y + 0.1, z0 + 2.0), mat(s)), box((x0 + 1.3, y, z0 + 1.92), (x0 + 1.5, y + 0.1, z0 + 2.0), mat(s)),
                box((x0 + 0.22, y - 0.03, z0 + 1.94), (x0 + 0.3, y + 0.13, z0 + 2.0), mat("chrome")), box((x0 + 1.5, y - 0.03, z0 + 1.94), (x0 + 1.58, y + 0.13, z0 + 2.0), mat("chrome"))]
    out += [box((x0 + 0.5, y0 + 1.8, z0 + 1.5), (x0 + 1.3, y0 + 1.88, z0 + 1.6), mat(s))]
    if turn:
        for e in out:
            e["rotation"] = {"angle": turn, "axis": "y", "origin": [x0 + 0.9, y0, z0 + 2.2]}
    return out


def crew_locker(v):
    """Two full-height steel lockers (each 47 cm wide, 1.8 m), their fronts to the north: the body on its plinth, each
    door with its louvres top and bottom, the handle, a padlock on some, the number and the name card; on v 2 the
    right-hand door stands open (its shelf, a jacket hung inside, boots in the bottom); a kit bag on top of v 1."""
    s = "locker"
    out = [box((0.2, 0, 2.0), (15.8, 1.0, 15.6), mat("black")),
           box((0.2, 1.0, 15.2), (15.8, 29.0, 15.8), mat(s)), box((0.2, 1.0, 1.4), (0.6, 29.0, 15.2), mat(s)), box((15.4, 1.0, 1.4), (15.8, 29.0, 15.2), mat(s)),
           box((7.8, 1.0, 1.4), (8.2, 29.0, 15.2), mat(s)), box((0.6, 1.0, 1.4), (15.4, 1.4, 15.2), mat(s)), box((0.6, 28.6, 1.4), (15.4, 29.0, 15.2), mat(s))]
    for k, x0 in enumerate((0.3, 8.1)):
        x1 = x0 + 7.6
        n = v * 2 + k
        door = [box((x0, 1.2, 1.0), (x1, 28.8, 1.4), mat(s))]
        for y in (3.0, 3.8, 4.6, 24.8, 25.6, 26.4):
            door.append(box((x0 + 1.6, y, 0.8), (x1 - 1.6, y + 0.4, 1.0), mat("graphite")))
        hx = x1 - 1.2 if k == 0 else x0 + 0.8
        door += [box((hx, 14.0, 0.6), (hx + 0.4, 16.6, 1.0), mat("chrome")),
                 box((x0 + 2.0, 19.4, 0.9), (x1 - 2.0, 20.6, 1.0), faces("white", north=A.region(f"rk_lname_{n}", 3.6, 1.2, name_card(CREW_NAMES[n % len(CREW_NAMES)])))),
                 box((x0 + 3.2, 21.2, 0.9), (x1 - 3.2, 22.2, 1.0), faces("chrome", north=A.region(f"rk_lnum_{n}", 1.2, 1.0, label_plate(str(11 + n), (196, 200, 204), (30, 30, 34)))))]
        if (n % 3) == 1:
            door += [box((hx - 0.1, 12.6, 0.3), (hx + 0.5, 13.6, 0.6), mat("bin_yellow")), box((hx + 0.05, 13.6, 0.4), (hx + 0.35, 14.2, 0.5), mat("chrome"))]
        if v == 2 and k == 1:
            for e in door:
                e["rotation"] = {"angle": -45.0, "axis": "y", "origin": [x1, 0, 1.2]}
            out += door + [box((x0 + 0.4, 22.0, 1.6), (x1 - 0.4, 22.4, 15.4), mat("graphite")),
                           box((x0 + 1.0, 9.0, 6.0), (x1 - 1.0, 21.4, 11.0), mat("navy")), box((x0 + 1.6, 18.6, 5.8), (x1 - 1.6, 21.4, 6.0), mat("navy")),
                           box((x0 + 3.6, 21.4, 8.2), (x0 + 4.0, 22.0, 8.8), mat("chrome"))]
            out += boot(x0 + 1.4, 3.0, 1.0) + boot(x0 + 3.8, 3.2, 1.0)
        else:
            out += door
    if v == 1:
        out += duffel(3.0, 4.6, 29.0)
    return out


def crew_bunk():
    """A steel bunk (2 m by 90 cm, built whole along x 0..32 with its head at x 0 and its open side to the north, two high):
    the four posts and their caps, the head and foot panels, each bed's frame and its slats, the quilted mattresses with
    their rounded edges, fitted sheets, the pillows, the duvets (the lower turned down over its sheet and hanging over the
    open side, the upper made, a blanket folded at its foot), the upper bed's guard rail, the ladder hooked over it at the
    foot, a shelf with a reading light at each head, the privacy curtains on their rails gathered at the foot, a pair of
    boots under the lower bed."""
    p = "steel"
    out = []
    for x0 in (0.4, 30.4):
        for z0 in (1.0, 14.0):
            out += [box((x0, 0, z0), (x0 + 1.2, 30.0, z0 + 1.2), mat(p)), box((x0 + 0.1, 30.0, z0 + 0.1), (x0 + 1.1, 30.4, z0 + 1.1), mat("grip_black")),
                    box((x0 - 0.05, 0, z0 - 0.05), (x0 + 1.25, 0.3, z0 + 1.25), mat("grip_black"))]
        for y0, y1 in ((9.6, 13.6), (25.6, 29.4)):
            out.append(box((x0 + 0.2, y0, 2.2), (x0 + 1.0, y1, 14.0), mat("locker")))
    for y0 in (4.4, 20.4):
        out += [box((1.6, y0, 1.1), (30.4, y0 + 1.0, 2.1), mat(p)), box((1.6, y0, 14.1), (30.4, y0 + 1.0, 15.1), mat(p)),
                box((1.6, y0, 2.1), (2.0, y0 + 1.0, 14.1), mat(p)), box((30.0, y0, 2.1), (30.4, y0 + 1.0, 14.1), mat(p))]
        for k in range(9):
            x = 3.0 + k * 3.2
            out.append(box((x, y0 + 0.3, 2.1), (x + 1.4, y0 + 0.6, 14.1), mat("oak")))
        m = y0 + 0.6
        out += bevel_box((2.0, m, 2.2), (30.0, m + 2.4, 14.0), "mattress", b=0.4)
        out.append(box((2.2, m + 2.4, 2.4), (29.8, m + 2.5, 13.8), mat("sheet")))
        out += bevel_box((2.8, m + 2.5, 4.2), (7.8, m + 3.9, 11.8), "pillow", b=0.4)
    # the lower duvet turned down, hanging over the open side; the upper made, a blanket folded at its foot
    lm = 4.4 + 0.6 + 2.5
    out += bevel_box((10.0, lm, 2.0), (30.0, lm + 0.9, 14.0), "blanket", b=0.3)
    out += [box((10.0, lm - 2.4, 1.6), (30.0, lm + 0.5, 2.0), mat("blanket")), box((8.4, lm, 2.2), (10.0, lm + 0.6, 13.8), mat("sheet"))]
    um = 20.4 + 0.6 + 2.5
    out += bevel_box((8.4, um, 2.2), (30.0, um + 0.8, 13.8), "blanket_red", b=0.3)
    out += bevel_box((24.0, um + 0.8, 3.0), (29.6, um + 1.8, 13.0), "sheet", b=0.25)
    # the guard rail, the ladder at the foot hooked over the rail
    out += [box((1.6, 26.4, 1.1), (24.0, 27.0, 1.7), mat(p)), box((1.6, 24.6, 1.1), (24.0, 25.0, 1.7), mat(p)),
            box((8.0, 21.4, 1.1), (8.6, 26.4, 1.7), mat(p)), box((16.0, 21.4, 1.1), (16.6, 26.4, 1.7), mat(p)), box((23.4, 21.4, 1.1), (24.0, 26.4, 1.7), mat(p))]
    for lx in (25.0, 29.0):
        out += [box((lx, 0, 0.0), (lx + 0.6, 26.0, 0.6), mat(p)), box((lx, 26.0, 0.0), (lx + 0.6, 26.4, 1.6), mat(p)),
                box((lx, 0, -0.1), (lx + 0.6, 0.3, 0.7), mat("grip_black"))]
    for y in (5.4, 10.0, 14.6, 19.2):
        out.append(box((25.6, y, 0.1), (29.0, y + 0.5, 0.5), mat(p)))
    # the head shelves and their reading lights
    for y0 in (14.6, 30.0):
        out += [box((1.6, y0 - 1.6, 9.0), (4.0, y0 - 1.4, 14.0), mat("oak")), box((2.6, y0 - 1.4, 12.4), (3.4, y0 - 0.2, 13.2), mat("graphite")),
                {**box((2.4, y0 - 0.9, 11.2), (3.6, y0 - 0.5, 12.4), mat("light")), "shade": False}]
    # the curtains: a rail along each bed's open side, the curtain gathered at the foot in its folds
    for y0, y1 in ((5.4, 19.4), (21.4, 29.6)):
        out.append(box((1.6, y1, -0.4), (30.4, y1 + 0.25, -0.15), mat("chrome")))
        for k in range(6):
            dz = 0.25 if k % 2 else 0.0
            out.append(box((21.6 + k * 0.55, y0 + 0.4, -0.75 + dz), (22.15 + k * 0.55, y1, -0.35 + dz), mat("curtain")))
        for k in range(4):
            out.append(box((2.0 + k * 5.0, y1 - 0.25, -0.38), (2.3 + k * 5.0, y1, -0.15), mat("chrome")))
    out += boot(4.0, 3.0, 0.0) + boot(6.2, 3.2, 0.0, 22.5)
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **MATERIALS}
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = TK.Atlas(1024, TX, "atlas")
    SCR = TK.Atlas(2048, 4 * TX, "screens")
    TK.SCR = SCR
    MK.A, MK.SCR = A, SCR
    LK.A, LK.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("rk_atlas"), "screens": ref("rk_screens"), "particle": ref("rk_wsteel")})

    def model(name, elements, prepared=False):
        if not prepared:
            prep(elements)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, variants):
        (bs_dir / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")

    def prep(elements):
        """Closes a whole piece's open faces and separates its coplanar faces before it is cut into its blocks, so
        every block's share moves the same."""
        for e in elements:
            for i in range(3):
                if e["from"][i] > e["to"][i]:
                    e["from"][i], e["to"][i] = e["to"][i], e["from"][i]
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        return elements

    def faced(model_name, extra="", off=0):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": (y - off) % 360} if (y - off) % 360 else {})} for f, y in FACINGS.items()}

    def single(name, els):
        model(name, els)
        states(name, faced(name))

    def tall(name, els, extra="", key=None):
        v = {}
        for h, part in zip(("lower", "upper"), cut_y(prep(els), 2)):
            model(f"{key or name}_{h}", part, True)
            v.update(faced(f"{key or name}_{h}", f",half={h}{extra}"))
        return v

    def wide(name, els, n, extra=""):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}{extra}"))
        return v

    # first aid
    v = {}
    for k, raised in enumerate((True, False)):
        for p, part in enumerate(cut_z(prep(exam_couch(raised)), 2)):
            model(f"exam_couch_{k}_{p}", part, True)
            v.update(faced(f"exam_couch_{k}_{p}", f",part={p},variant={k}", off=180))
    states("exam_couch", v)
    states("privacy_screen", tall("privacy_screen", privacy_screen()))
    states("iv_pole", tall("iv_pole", iv_pole()))
    states("spine_board", tall("spine_board", spine_board()))
    single("oxygen_trolley", oxygen_trolley())
    single("crash_cart", crash_cart())
    v = {}
    for h, half in zip(("lower", "upper"), cut_y(prep(scrub_sink()), 2)):
        for p, part in enumerate(cut_parts(half, 2)):
            model(f"scrub_sink_{h}_{p}", part, True)
            v.update(faced(f"scrub_sink_{h}_{p}", f",half={h},part={p}"))
    states("scrub_sink", v)
    single("med_fridge", med_fridge())
    single("medicine_cupboard", medicine_cupboard())
    single("first_aid_box", first_aid_box())
    single("eyewash_bottles", eyewash_station())
    single("wheelchair", wheelchair())
    single("step_stool", step_stool())
    v = {}
    for k, kind in enumerate(POSTERS):
        model(f"med_poster_{k}", med_poster(kind))
        v.update(faced(f"med_poster_{k}", f",variant={k}"))
    states("med_poster", v)
    states("med_desk", wide("med_desk", med_desk(), 2))
    single("dressing_trolley", dressing_trolley())
    single("diagnostic_set", diagnostic_set())
    states("column_scale", tall("column_scale", column_scale()))
    v = {}
    for colour in ("yellow", "black"):
        model(f"pedal_bin_{colour}", pedal_bin(colour))
        v.update(faced(f"pedal_bin_{colour}", f",colour={colour}"))
    states("pedal_bin", v)
    # crew rest
    v = {}
    for k, rec in enumerate((False, True)):
        model(f"recliner_{k}", recliner(rec))
        v.update(faced(f"recliner_{k}", f",variant={k}"))
    states("recliner", v)
    single("crew_side_table", side_table())
    v = {}
    for left in (False, True):
        for right in (False, True):
            n = f"crew_sofa_{'l' if left else ''}{'r' if right else ''}" if (left or right) else "crew_sofa"
            model(n, crew_sofa(left, right))
            v.update(faced(n, f",left={'false' if left else 'true'},right={'false' if right else 'true'}"))
    states("crew_sofa", v)
    states("crew_coffee_table", wide("crew_coffee_table", crew_coffee_table(), 2))
    states("wall_tv", wide("wall_tv", wall_tv(), 2))
    v = {}
    for h, half in zip(("lower", "upper"), cut_y(prep(kitchenette()), 2)):
        for p, part in enumerate(cut_parts(half, 3)):
            model(f"kitchenette_{h}_{p}", part, True)
            v.update(faced(f"kitchenette_{h}_{p}", f",half={h},part={p}"))
    states("kitchenette", v)
    states("crew_fridge", tall("crew_fridge", crew_fridge()))
    v = {}
    for k in range(LOCKERS):
        v.update(tall("crew_locker", crew_locker(k), f",variant={k}", key=f"crew_locker_{k}"))
    states("crew_locker", v)
    v = {}
    for h, half in zip(("lower", "upper"), cut_y(prep(crew_bunk()), 2)):
        for p, part in enumerate(cut_parts(half, 2)):
            model(f"crew_bunk_{h}_{p}", part, True)
            v.update(faced(f"crew_bunk_{h}_{p}", f",half={h},part={p}"))
    states("crew_bunk", v)
    states("water_dispenser", tall("water_dispenser", water_dispenser()))
    A.img.save(tex_dir / "rk_atlas.png")
    SCR.img.save(tex_dir / "rk_screens.png")
    return A.y + A.row, SCR.y + SCR.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
