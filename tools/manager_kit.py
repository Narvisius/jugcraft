"""The tower manager's office furniture (work list item 22 part B, room 2), built to room 1's standard (Justin on the
briefing room: everything modelled, nothing painted on that should stand out; screens sharp; materials that tile
without seams; things varied; cups open):

- exec_desk (3 x 1): a walnut executive desk, its pedestal of three drawers at the user's left, a panel leg at the
  right, the modesty panel with its steel strip to the visitors; on it the two monitors on their arm (live pages),
  the keyboard and mouse on the leather desk pad, the desk phone, the lamp, the pen cup, a photo, the in-tray, a mug,
  the brass nameplate facing the visitors.
- exec_chair: a high-back leather chair on a five-star base (casters), its seat and back in stitched pads, the
  headrest, the arms; a variant swivelled.
- visitor_chair: an upholstered side chair with walnut arms and legs; variants turned in toward the desk.
- office_bookcase (two blocks high): walnut, the lower cupboard (two doors), open shelves above filled with books
  (their spines titled), binders, lying stacks, bookends, a trophy, a model drone, a little plant, photos; every
  shelf its own (variants).
- leather_sofa (joins up; an end on its own is an armchair): cognac leather, its seat and back cushions, the arms,
  steel feet.
- lounge_coffee_table (2 x 1): walnut on a black steel frame, the shelf under it; magazines, a bowl, a glass on a coaster.
- bistro_table: a round walnut table on its pedestal; a notepad on it.
- exec_credenza (3 x 1): walnut, doors either end (brass pulls), binders in the open middle; the printer, a photo and
  a plant on it.
- framed_certificate (on the wall): the tower operator's certificate, the safety award plaque, the millionth
  delivery; their frames and mounts modelled.
- coat_stand (two blocks high), floor_lamp (two blocks high), waste_basket.

Materials and pictures as tools/training_kit.py: one density (16 texels a pixel), pictures from an atlas sized to
their faces, screens at four times that; round things as stepped slabs (no overlaps, no shared planes).
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
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, ORANGE, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None            # the pictures atlas (write() makes it)
SCR = None          # the screens atlas
MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SERIF_R = "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"


# ------------------------------------------------------------------ materials
def wood(base, seed, lines=46, depth=(0.05, 0.11)):
    """A wood veneer, straight grain along x, seamless both ways (as training_kit.oak)."""
    n = 256
    rng = np.random.default_rng(seed)
    shade = 1 + 0.05 * TK._periodic(n, seed + 1, 40)[:, None] * np.ones((1, n))
    for _ in range(lines):
        y = rng.uniform(0, n)
        fade = 0.5 + 0.5 * np.sin(math.tau * rng.integers(1, 3) * np.arange(n) / n + rng.uniform(0, math.tau))
        w = rng.uniform(0.6, 1.8)
        d = np.abs(((np.arange(n) - y + n / 2) % n) - n / 2)[:, None]
        shade -= rng.uniform(*depth) * np.clip(1 - d / w, 0, 1) * (0.35 + 0.65 * fade[None, :])
    for _ in range(160):
        y, x0, ln = rng.integers(0, n), rng.integers(0, n), rng.integers(4, 13)
        shade[y, (x0 + np.arange(ln)) % n] -= rng.uniform(0.04, 0.08)
    a = np.array(base, float)[None, None, :] * shade[..., None] + np.random.default_rng(seed + 2).normal(0, 0.8, (n, n))[..., None]
    return TK._img(a)


def leather(base, seed, grain=2.2):
    """Leather: a soft mottle (whole-period waves: seamless) and a fine pebble grain."""
    n = 256
    t = np.arange(n) / n
    rng = np.random.default_rng(seed)
    m = sum(rng.normal() * np.sin(math.tau * (kx * t[None, :] + ky * t[:, None]) + rng.uniform(0, math.tau))
            for kx, ky in ((1, 2), (2, 1), (3, 3), (1, 4), (4, 1), (2, 5)))
    m = m / np.abs(m).max()
    a = np.array(base, float)[None, None, :] * (1 + 0.05 * m)[..., None]
    a += rng.normal(0, grain, (n, n))[..., None]
    return TK._img(a)


def weave(base, seed, step=2, amp=5):
    y, x = np.mgrid[0:256, 0:256]
    w = (((x // step) + (y // step)) % 2) * amp - amp / 2
    return TK._img(np.array(base, float)[None, None, :] + w[..., None] + TK._noise(seed, 1.5)[..., None])


def rgba(rgb, alpha, seed):
    """A translucent material (glass, whisky, ice, smoke)."""
    a = np.concatenate([np.stack([np.full((256, 256), c, float) for c in rgb], -1) + TK._noise(seed, 1.5)[..., None],
                        np.full((256, 256, 1), alpha, float)], -1)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def cigar():
    """A cigar's wrapper leaf: brown, a fine vein along it."""
    a = np.array(wood((104, 66, 38), 3147, lines=30, depth=(0.03, 0.08)).convert("RGB"), float)
    return TK._img(a)


def band():
    y, x = np.mgrid[0:256, 0:256]
    a = np.where((((x // 4) % 4) == 0)[..., None], np.array([214, 172, 64], float), np.array([150, 30, 34], float))
    return TK._img(a + TK._noise(3148, 2)[..., None])


def snake():
    """A snake plant's leaf: dark green crossed by paler wavy bands (whole periods: seamless)."""
    n = 256
    y, x = np.mgrid[0:n, 0:n]
    band = np.sin(math.tau * (y / n * 16 + 0.06 * np.sin(math.tau * x / n * 4)))
    a = np.where((band > 0.55)[..., None], np.array([92, 140, 84], float), np.array([38, 86, 48], float))
    return TK._img(a + TK._noise(3119, 2)[..., None])


def snake_plant(cx, cz, y0, scale=1.0):
    """A snake plant's leaves standing up out of its pot (cx, cz at the soil, y0): tall, flat, each leaning its own way."""
    out = []
    leaves = ((-0.7, 0.0, 6.0, "z", -22.5, 1.0), (0.7, 0.2, 7.0, "z", 22.5, 1.1), (0.0, -0.7, 6.6, "x", -22.5, 0.9),
              (0.1, 0.7, 5.6, "x", 22.5, 1.0), (0.0, 0.0, 7.8, "z", 0.0, 1.2), (-0.4, 0.4, 5.0, "x", 22.5, 0.8),
              (0.45, -0.35, 5.4, "z", 22.5, 0.8))
    for dx, dz, h, ax, ang, w in leaves:
        dx, dz, h, w = dx * scale, dz * scale, h * scale, w * scale
        if ax == "z":
            f, t = (cx + dx - w / 2, y0, cz + dz - 0.14), (cx + dx + w / 2, y0 + h, cz + dz + 0.14)
        else:
            f, t = (cx + dx - 0.14, y0, cz + dz - w / 2), (cx + dx + 0.14, y0 + h, cz + dz + w / 2)
        r = {"angle": ang, "axis": ax, "origin": [cx + dx, y0, cz + dz]} if ang else None
        out.append(box(f, t, mat("snake"), r))
    return out


BOOK_COLOURS = {"bk_navy": (34, 50, 92), "bk_red": (128, 34, 32), "bk_green": (34, 84, 56), "bk_black": (32, 32, 34),
                "bk_tan": (176, 140, 92), "bk_cream": (224, 214, 188), "bk_grey": (100, 104, 110), "bk_teal": (30, 104, 112),
                "bk_orange": (206, 120, 40), "bk_maroon": (92, 30, 44), "bk_blue": (52, 96, 160), "bk_white": (230, 230, 226)}

MATERIALS = {
    "mg_walnut": lambda: wood((104, 68, 44), 3101), "mg_walnut_dark": lambda: wood((72, 46, 30), 3102),
    "mg_leather": lambda: leather((128, 74, 42), 3103), "mg_leather_dark": lambda: leather((38, 35, 34), 3104, grain=1.0),
    "mg_brass": lambda: TK.brushed((198, 162, 88), 3105, 3), "mg_upholstery": lambda: weave((70, 78, 96), 3106),
    "mg_linen": lambda: weave((232, 224, 206), 3107, 1, 4), "mg_basket": lambda: weave((34, 34, 36), 3108, 2, 8),
    "mg_light": lambda: TK.solid((255, 240, 210), 3109, 0.8), "mg_leaf": lambda: weave((52, 116, 60), 3110, 3, 10),
    "mg_soil": lambda: TK.solid((60, 44, 32), 3111, 4), "mg_pot": lambda: TK.solid((222, 220, 214), 3112, 1.5),
    "mg_printer": lambda: TK.solid((214, 216, 218), 3113, 1.2), "mg_navy_cloth": lambda: weave((30, 40, 66), 3114, 1, 5),
    "mg_water": lambda: TK.solid((150, 196, 214), 3115, 1.0), "mg_snake": lambda: snake(),
    "mg_onyx": lambda: TK.solid((22, 22, 24), 3140, 1.5), "mg_ash": lambda: TK.solid((150, 148, 144), 3141, 9),
    "mg_ember": lambda: TK.solid((255, 120, 40), 3142, 6), "mg_cigar": lambda: cigar(),
    "mg_cigar_band": lambda: band(), "mg_glass": lambda: rgba((206, 224, 232), 70, 3143),
    "mg_whisky": lambda: rgba((176, 98, 30), 200, 3144), "mg_ice": lambda: rgba((236, 244, 248), 150, 3145),
    "mg_smoke": lambda: rgba((206, 206, 210), 40, 3146), "mg_gold": lambda: TK.brushed((214, 172, 64), 3116, 3),
    "mg_frame": lambda: TK.solid((28, 28, 30), 3117, 1.2), "mg_mat": lambda: TK.solid((240, 238, 230), 3118, 1.0),
    **{f"mg_{k}": (lambda c=c, s=3120 + i: TK.solid(c, s, 2.0)) for i, (k, c) in enumerate(BOOK_COLOURS.items())}}


# ------------------------------------------------------------------ pictures
def _text_fit(d, text, font, size, width):
    while size > 4 and d.textlength(text, font=ImageFont.truetype(font, size)) > width:
        size -= 1
    return ImageFont.truetype(font, size)


TITLES = ["AIRSPACE", "ROTORCRAFT", "FLIGHT LAW", "LOGISTICS", "WEATHER", "NAVIGATION", "BATTERIES", "SAFETY",
          "OPERATIONS", "MAINTENANCE", "AERODYNAMICS", "PAYLOADS", "RADIO", "MAPS", "TRAINING", "FLEET", "SCHEDULING",
          "ELECTRICS", "MOTORS", "STANDARDS", "ROUTES", "RECORDS", "PLANNING", "EMERGENCIES"]


def spine(colour, seed, binder=False):
    """A book's spine (its title down it, the bands top and bottom), or a binder's (its label, its finger hole)."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        rgb = BOOK_COLOURS[colour]
        d.rectangle((0, 0, W, H), fill=rgb)
        rng = random.Random(seed)
        light = sum(rgb) > 450
        ink = (40, 36, 30) if light else (226, 214, 180)
        if binder:
            d.rectangle((3, H * 0.12, W - 4, H * 0.42), fill=(240, 238, 230))
            f = _text_fit(d, TITLES[seed % len(TITLES)], FONT, 6, H * 0.28)
            label = Image.new("RGBA", (round(H * 0.3), max(4, W - 8)), (0, 0, 0, 0))
            ImageDraw.Draw(label).text((label.width / 2, label.height / 2), TITLES[seed % len(TITLES)], font=f, fill=(30, 30, 40), anchor="mm")
            img.paste(label.rotate(90, expand=True), (4, round(H * 0.12)), label.rotate(90, expand=True))
            d.ellipse((W / 2 - W * 0.22, H * 0.78 - W * 0.22, W / 2 + W * 0.22, H * 0.78 + W * 0.22), fill=tuple(max(0, c - 40) for c in rgb))
            return img
        for y in (H * 0.06, H * 0.9):
            d.rectangle((0, y, W, y + 2), fill=ink)
        title = TITLES[seed % len(TITLES)]
        f = _text_fit(d, title, SERIF if rng.random() < 0.5 else FONT, max(5, W - 4), H * 0.7)
        label = Image.new("RGBA", (round(H * 0.75), W), (0, 0, 0, 0))
        ImageDraw.Draw(label).text((label.width / 2, label.height / 2), title, font=f, fill=ink, anchor="mm")
        rot = label.rotate(-90, expand=True)
        img.paste(rot, (0, round(H * 0.12)), rot)
        return img
    return draw


def keyboard_top(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 31, 34))
    k = W / 10.0
    rows = 5
    kh = (H - 2 * k * 0.25) / rows
    for r in range(rows):
        y0 = k * 0.25 + r * kh
        n = 15 if r < 4 else 0
        if n:
            kw = (W * 0.78 - k * 0.5) / n
            for c in range(n):
                x0 = k * 0.25 + c * kw
                d.rectangle((x0 + 1, y0 + 1, x0 + kw - 2, y0 + kh - 2), fill=(52, 54, 58))
        else:
            d.rectangle((k * 0.25 + W * 0.18, y0 + 1, W * 0.55, y0 + kh - 2), fill=(52, 54, 58))
        for c in range(4):                                                    # the number pad
            x0 = W * 0.8 + c * (W * 0.19 / 4)
            d.rectangle((x0 + 1, y0 + 1, x0 + W * 0.19 / 4 - 2, y0 + kh - 2), fill=(52, 54, 58))
    return img


def monitor_screen(page):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 10, 12))
        b = round(W * 0.025)
        inner = (b, b, W - b, H - round(W * 0.05))
        d.rectangle(inner, fill=(9, 13, 15))
        TK._page_into(img, page, inner)
        d.rectangle((W / 2 - W * 0.04, H - W * 0.035, W / 2 + W * 0.04, H - W * 0.02), fill=(60, 62, 66))
        return img
    return draw


def phone_face(img):
    """The desk phone's face: its display (the line, the time), the keypad, the line keys, the speaker grille."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(28, 29, 32))
    k = W / 5.2
    d.rectangle((0.5 * k, 0.3 * k, 3.6 * k, 1.5 * k), fill=(150, 176, 160), outline=(14, 14, 16), width=3)
    f = ImageFont.truetype(MONO, round(0.26 * k))
    d.text((0.65 * k, 0.55 * k), "TOWER MGR  x200", font=f, fill=(20, 30, 24))
    # live: the day and the tower's time
    TK.live_text(img, (0.65 * k, 0.95 * k), "THU 14:32", f, (20, 30, 24), "la", bg=(150, 176, 160), widest="WED 00:00", src="dtime")
    for r in range(4):
        for c in range(3):
            x0, y0 = 0.6 * k + c * 0.95 * k, 1.85 * k + r * 0.6 * k
            d.rounded_rectangle((x0, y0, x0 + 0.75 * k, y0 + 0.45 * k), radius=0.08 * k, fill=(60, 62, 66))
            d.text((x0 + 0.375 * k, y0 + 0.225 * k), "123456789*0#"[r * 3 + c], font=ImageFont.truetype(FONT, round(0.25 * k)),
                   fill=(220, 222, 226), anchor="mm")
    for r in range(5):
        y0 = 0.4 * k + r * 0.55 * k
        d.rounded_rectangle((3.95 * k, y0, 4.85 * k, y0 + 0.4 * k), radius=0.06 * k, fill=(60, 62, 66))
        d.ellipse((4.62 * k, y0 + 0.12 * k, 4.76 * k, y0 + 0.26 * k), fill=(70, 220, 110) if r == 0 else (36, 38, 40))
    for x in range(round(3.9 * k), round(4.9 * k), max(2, round(0.12 * k))):
        for y in range(round(3.4 * k), round(4.1 * k), max(2, round(0.12 * k))):
            d.point((x, y), fill=(12, 12, 14))
    return img


def nameplate(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    a = np.array(TK.brushed((200, 164, 90), 3130, 3).resize((W, H)))
    img.paste(Image.fromarray(a), (0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, W - 2, H - 2), outline=(150, 118, 56))
    d.text((W / 2, H * 0.52), "TOWER MANAGER", font=_text_fit(d, "TOWER MANAGER", SERIF, 11, W - 10), fill=(60, 44, 18), anchor="mm")
    return img


def photo(kind):
    """A photo in a frame: the tower at dusk, or the drone fleet on the pad."""
    def draw(img):
        W, H = img.size
        a = np.zeros((H, W, 3), float)
        yy = np.arange(H)[:, None] / H
        if kind == "dusk":
            top, bot = np.array([46, 58, 112]), np.array([240, 150, 86])
            a[:] = top * (1 - yy[..., None]) + bot * yy[..., None]
            img.paste(TK._img(a), (0, 0))
            d = ImageDraw.Draw(img)
            d.rectangle((0, H * 0.78, W, H), fill=(30, 34, 30))
            cx = W * 0.55
            d.polygon([(cx - W * 0.1, H * 0.8), (cx - W * 0.05, H * 0.2), (cx + W * 0.05, H * 0.2), (cx + W * 0.1, H * 0.8)], fill=(24, 26, 32))
            for k in range(5):
                d.rectangle((cx - W * 0.03, H * (0.3 + k * 0.1), cx + W * 0.03, H * (0.3 + k * 0.1) + 2), fill=(255, 210, 120))
        else:
            a[:] = np.array([120, 170, 220]) * (1 - yy[..., None]) + np.array([180, 210, 235]) * yy[..., None]
            img.paste(TK._img(a), (0, 0))
            d = ImageDraw.Draw(img)
            d.rectangle((0, H * 0.62, W, H), fill=(90, 92, 96))
            for k in range(3):
                x = W * (0.2 + 0.3 * k)
                d.rectangle((x - W * 0.08, H * 0.6, x + W * 0.08, H * 0.66), fill=(40, 42, 46))
                d.rectangle((x - W * 0.12, H * 0.57, x + W * 0.12, H * 0.59), fill=(232, 150, 40))
        return img
    return draw


def magazine(title, colour):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=colour)
        d.rectangle((0, H * 0.25, W, H * 0.8), fill=tuple(min(255, c + 50) for c in colour))
        d.text((W / 2, H * 0.12), title, font=_text_fit(d, title, FONT, 14, W - 8), fill=(250, 250, 248), anchor="mm")
        d.polygon([(W * 0.2, H * 0.7), (W * 0.5, H * 0.35), (W * 0.8, H * 0.7)], fill=(40, 42, 46))
        for k in range(3):
            d.rectangle((W * 0.1, H * (0.84 + k * 0.05), W * (0.5 + 0.1 * k), H * (0.84 + k * 0.05) + 2), fill=(250, 250, 248))
        return img
    return draw


def certificate(kind):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 242, 228))
        d.rectangle((6, 6, W - 7, H - 7), outline=(150, 110, 40), width=3)
        d.rectangle((11, 11, W - 12, H - 12), outline=(190, 150, 70), width=1)
        if kind == "operator":
            lines = [("CERTIFICATE", SERIF, 15, (40, 36, 30)), ("OF OPERATION", SERIF_R, 9, (60, 54, 44)), ("DRONE TOWER", FONT, 10, (180, 100, 30)),
                     ("is certified to operate", SERIF_R, 7, (70, 64, 54)), ("a Tier 1 to Tier 9 fleet", SERIF_R, 7, (70, 64, 54))]
        else:
            lines = [("ONE MILLION", SERIF, 14, (40, 36, 30)), ("DELIVERIES", SERIF, 12, (40, 36, 30)), ("DRONE TOWER", FONT, 9, (180, 100, 30)),
                     ("in recognition of the crew", SERIF_R, 7, (70, 64, 54)), ("and the fleet", SERIF_R, 7, (70, 64, 54))]
        y = H * 0.16
        for t, f, s, c in lines:
            fnt = _text_fit(d, t, f, s, W - 30)
            d.text((W / 2, y), t, font=fnt, fill=c, anchor="mm")
            y += fnt.size + 6
        d.ellipse((W * 0.72, H * 0.68, W * 0.86, H * 0.68 + W * 0.14), fill=(200, 40, 36))
        for k in range(2):
            d.line((W * 0.18, H * (0.78 + 0.07 * k), W * 0.5, H * (0.78 + 0.07 * k)), fill=(60, 60, 70), width=1)
        return img
    return draw


def plaque_plate(img):
    W, H = img.size
    img.paste(TK.brushed((200, 164, 90), 3131, 3).resize((W, H)), (0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, W - 2, H - 2), outline=(150, 118, 56))
    for t, y, s in (("SAFETY AWARD", 0.25, 10), ("A YEAR WITHOUT", 0.52, 7), ("A LOST DRONE", 0.72, 7)):
        d.text((W / 2, H * y), t, font=_text_fit(d, t, SERIF, s, W - 8), fill=(60, 44, 18), anchor="mm")
    return img


def printer_panel(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(40, 42, 46))
    k = W / 3.0
    d.rectangle((0.2 * k, 0.2 * k, 1.9 * k, H - 0.2 * k), fill=(30, 60, 90))
    d.text((0.3 * k, 0.45 * k), "READY", font=ImageFont.truetype(MONO, round(0.28 * k)), fill=(200, 230, 255))
    return img


def page_sheet(seed):
    """A printed page (its heading, a table of figures)."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 246, 242))
        rng = random.Random(seed)
        d.rectangle((W * 0.08, H * 0.06, W * 0.6, H * 0.1), fill=(40, 40, 50))
        for k in range(14):
            y = H * (0.16 + k * 0.055)
            d.rectangle((W * 0.08, y, W * rng.uniform(0.5, 0.9), y + max(1, H * 0.012)), fill=(110, 110, 120))
        return img
    return draw


# ------------------------------------------------------------------ the executive desk
TOP = 12.6


def exec_desk():
    """Built whole along x 0..48 (its user's left at x 0), the user at the south looking north, the visitors north."""
    w = "walnut"
    out = [box((0.5, 11.6, 1.0), (47.5, TOP, 15.2), {**mat(w), "down": {"texture": "#black"}}),
           # the pedestal of drawers at the user's left, on its plinth
           box((1.4, 0, 2.4), (13.6, 0.4, 14.0), mat("black")),
           box((1.0, 0.4, 2.0), (14.0, 11.6, 14.4), mat(w, skip=("up",))),
           # the panel leg at the right, the modesty panel and its steel strip to the visitors
           box((45.0, 0, 1.4), (46.8, 11.6, 14.6), mat(w, skip=("up",))),
           box((14.0, 3.0, 1.4), (45.0, 11.6, 2.0), mat(w, skip=("up",))),
           box((14.0, 9.6, 1.1), (45.0, 10.2, 1.4), mat("chrome"))]
    for y0, y1 in ((8.0, 11.2), (4.4, 7.6), (0.8, 4.0)):                     # the drawers and their bar pulls
        out.append(box((1.4, y0, 14.4), (13.6, y1, 14.8), mat(w)))
        ym = (y0 + y1) / 2
        out += [box((5.0, ym - 0.2, 15.2), (10.0, ym + 0.2, 15.6), mat("chrome")),
                box((5.4, ym - 0.2, 14.8), (5.8, ym + 0.2, 15.2), mat("chrome")), box((9.2, ym - 0.2, 14.8), (9.6, ym + 0.2, 15.2), mat("chrome"))]
    # the leather desk pad, the keyboard and mouse on it
    out += [box((17.0, TOP, 8.4), (31.0, TOP + 0.12, 14.6), mat("leather_dark")),
            box((19.0, TOP + 0.12, 9.6), (29.0, TOP + 0.5, 12.6), faces("black", up=A.region("keyboard", 10.0, 3.0, keyboard_top))),
            box((29.6, TOP + 0.12, 10.2), (30.9, TOP + 0.6, 12.2), mat("black"))]
    # the two monitors on their arm (the screens live in game)
    out += [box((22.8, TOP, 1.6), (25.2, TOP + 0.4, 3.4), mat("graphite")),
            box((23.7, TOP + 0.4, 2.2), (24.3, 19.0, 2.8), mat("graphite")),
            box((16.0, 18.0, 2.32), (32.0, 18.6, 2.68), mat("graphite"))]
    for x0, page in ((14.4, "6_0"), (24.1, "4_0")):
        out += [box((x0 + 2.6, 15.8, 2.8), (x0 + 6.9, 20.2, 3.6), mat("graphite")),
                {**box((x0, 15.4, 3.6), (x0 + 9.5, 21.2, 4.1), faces("black", south=SCR.face(f"mon_{page}", 9.5, 5.8, monitor_screen(page)))), "shade": False}]
    # the phone (its handset in its cradle, its face tilted to the user)
    out += [box((36.0, TOP, 4.0), (41.6, TOP + 0.6, 9.0), mat("black")),
            box((37.6, TOP + 0.4, 4.4), (41.4, TOP + 0.9, 8.8), faces("black", up=SCR.face("phone", 3.8, 4.4, phone_face)),
                {"angle": 22.5, "axis": "x", "origin": [39.5, TOP + 0.6, 6.6]}),
            box((36.1, TOP + 0.6, 4.2), (37.4, TOP + 1.2, 8.8), mat("black")),
            box((35.9, TOP + 1.2, 4.0), (37.6, TOP + 1.7, 5.4), mat("black")), box((35.9, TOP + 1.2, 7.6), (37.6, TOP + 1.7, 9.0), mat("black"))]
    # the nameplate to the visitors
    out += [box((21.0, TOP, 1.4), (27.0, TOP + 1.2, 2.4), mat("walnut_dark")),
            box((21.3, TOP + 0.15, 1.25), (26.7, TOP + 1.05, 1.4), faces("brass", north=A.region("nameplate", 5.4, 0.9, nameplate)))]
    # the lamp: its base, its arm up and over toward the user, its head lit underneath
    out += round_part(4.5, 4.0, 1.2, TOP, TOP + 0.4, "black")
    bend = {"angle": 22.5, "axis": "x", "origin": [4.5, TOP + 5.0, 4.0]}
    out += [box((4.3, TOP + 0.4, 3.8), (4.7, TOP + 5.0, 4.2), mat("black")),
            box((4.25, TOP + 4.8, 3.9), (4.75, TOP + 5.2, 9.2), mat("black"), bend),
            {**box((3.0, TOP + 4.5, 8.4), (6.0, TOP + 5.1, 10.8), {**mat("black"), "down": {"texture": "#light"}}, bend), "shade": False}]
    # the pen cup and its pens, the photo (to the user), the in-tray, a mug
    out += round_ring(10.0, 4.0, 0.8, 0.12, TOP, TOP + 2.0, "black", 0.3, "black", n=4)
    out += [box((9.8, TOP + 0.3, 3.8), (10.0, TOP + 2.8, 4.0), mat("navy"), {"angle": 22.5, "axis": "z", "origin": [9.9, TOP + 0.3, 3.9]}),
            box((10.1, TOP + 0.3, 4.1), (10.3, TOP + 2.6, 4.3), mat("black"), {"angle": -22.5, "axis": "z", "origin": [10.2, TOP + 0.3, 4.2]}),
            box((9.9, TOP + 0.3, 4.2), (10.1, TOP + 2.9, 4.4), mat("red"))]
    lean = {"angle": -22.5, "axis": "x", "origin": [8.0, TOP, 6.6]}
    out += [box((6.2, TOP, 6.2), (9.8, TOP + 3.0, 6.6), mat("walnut_dark"), lean),
            box((6.5, TOP + 0.3, 6.6), (9.5, TOP + 2.7, 6.7), {"south": {"texture": "#atlas", "uv": A.region("photo_dusk", 3.0, 2.4, photo("dusk"))}}, lean),
            box((7.8, TOP, 5.1), (8.2, TOP + 1.8, 5.45), mat("walnut_dark"))]
    out += [box((34.6, TOP, 10.0), (41.0, TOP + 0.2, 14.8), mat("black")),
            box((34.6, TOP + 0.2, 10.0), (41.0, TOP + 0.9, 10.2), mat("black")), box((34.6, TOP + 0.2, 14.6), (41.0, TOP + 0.9, 14.8), mat("black")),
            box((34.6, TOP + 0.2, 10.2), (34.8, TOP + 0.9, 14.6), mat("black")), box((40.8, TOP + 0.2, 10.2), (41.0, TOP + 0.9, 14.6), mat("black")),
            box((35.2, TOP + 0.2, 10.6), (40.4, TOP + 0.55, 14.2), faces("paper", up=A.region("sheet_0", 5.2, 3.6, page_sheet(0))))]
    out += round_ring(43.6, 10.6, 1.1, 0.2, TOP, TOP + 2.4, "white", 1.9, "coffee",
                      A.region("mug_mgr", 2.2, 2.4, TK.logo((238, 238, 234))))
    out += [box((44.7, TOP + 0.6, 10.35), (45.3, TOP + 0.9, 10.85), mat("white")), box((45.0, TOP + 0.9, 10.35), (45.3, TOP + 1.6, 10.85), mat("white")),
            box((44.7, TOP + 1.6, 10.35), (45.3, TOP + 1.9, 10.85), mat("white"))]
    return out


# ------------------------------------------------------------------ the chairs
def exec_chair(swivel=0.0):
    """The high-back leather chair, its sitter looking north: the five-star base on its casters (the base stays as it
    is; a swivelled chair turns above it), the gas lift, the seat, the back shaped in to the waist and padded in
    stitched bands to its head, the loop arms (chrome, leather pads)."""
    out = [box((7.2, 0.9, 7.2), (8.8, 2.0, 8.8), mat("chrome"))]
    legs = [((7.5, 1.2, 1.8), (8.5, 2.0, 8.0), 0.0, (8.0, 2.2)), ((8.0, 1.2, 7.5), (14.2, 2.0, 8.5), 22.5, (13.8, 8.0)),
            ((7.5, 1.2, 8.0), (8.5, 2.0, 14.2), 45.0, (8.0, 13.8)), ((7.5, 1.2, 8.0), (8.5, 2.0, 14.2), -45.0, (8.0, 13.8)),
            ((1.8, 1.2, 7.5), (8.0, 2.0, 8.5), -22.5, (2.2, 8.0))]
    for f, t, ang, (ex, ez) in legs:
        rot = {"angle": ang, "axis": "y", "origin": [8, 1.2, 8]} if ang else None
        out.append(box(f, t, mat("chrome"), rot))
        out.append(box((ex - 0.5, 0, ez - 0.5), (ex + 0.5, 1.2, ez + 0.5), mat("black"), rot))
    L = "leather_dark"
    top = [box((7.2, 2.0, 7.2), (8.8, 3.6, 8.8), mat("black")), box((7.5, 3.6, 7.5), (8.5, 6.2, 8.5), mat("chrome")),
           box((5.0, 6.2, 5.0), (11.0, 6.8, 11.0), mat("black")),
           box((2.6, 6.8, 1.8), (13.4, 8.4, 12.6), {**mat(L), "down": {"texture": "#black"}}),
           box((3.0, 8.4, 2.2), (13.0, 9.2, 12.2), mat(L)),
           box((7.0, 6.8, 12.6), (9.0, 10.0, 14.0), mat("black")),
           # the back: its shell shaped in at the waist, the stitched bands on its face, the head band
           box((3.0, 9.4, 12.6), (13.0, 13.6, 13.8), mat(L)), box((2.7, 13.6, 12.6), (13.3, 18.0, 13.8), mat(L)),
           box((3.4, 18.0, 12.6), (12.6, 20.8, 13.8), mat(L))]
    for y0, y1, x0, x1 in ((9.8, 11.6, 3.4, 12.6), (11.8, 13.6, 3.4, 12.6), (13.8, 15.8, 3.1, 12.9), (16.0, 17.8, 3.1, 12.9), (18.2, 20.4, 3.8, 12.2)):
        top.append(box((x0, y0, 12.0), (x1, y1, 12.6), mat(L)))
    for x0 in (1.8, 13.4):                                                   # the loop arms
        top += [box((x0, 7.4, 4.6), (x0 + 0.8, 11.2, 5.4), mat("chrome")), box((x0, 7.4, 9.6), (x0 + 0.8, 11.2, 10.4), mat("chrome")),
                box((x0, 6.8, 4.6), (x0 + 0.8, 7.4, 10.4), mat("chrome")),
                box((x0 - 0.3, 11.2, 4.2), (x0 + 1.1, 11.9, 10.8), mat(L))]
    if swivel:
        for e in top:
            e["rotation"] = {"angle": swivel, "axis": "y", "origin": [8, 0, 8]}
    return out + top


def visitor_chair(turn=0.0):
    """The upholstered visitor's chair, its sitter looking north: walnut legs and arms, the padded seat and back."""
    out = []
    for x0 in (2.4, 12.6):
        for z0 in (2.6, 12.0):
            out.append(box((x0, 0, z0), (x0 + 1.0, 7.0, z0 + 1.0), mat("walnut")))
        out += [box((x0, 7.0, 2.6), (x0 + 1.0, 11.0, 3.6), mat("walnut")), box((x0 - 0.2, 11.0, 2.4), (x0 + 1.2, 11.6, 13.2), mat("walnut")),
                box((x0, 7.0, 12.0), (x0 + 1.0, 11.0, 13.0), mat("walnut"))]
    out += [box((3.4, 6.2, 2.6), (12.6, 7.0, 13.0), mat("walnut")),
            box((3.4, 7.0, 2.4), (12.6, 8.8, 12.4), mat("upholstery")),
            box((3.4, 7.0, 12.4), (12.6, 18.4, 13.8), mat("upholstery")),
            box((3.0, 18.4, 12.2), (13.0, 19.0, 14.0), mat("walnut")),
            box((3.0, 11.0, 13.0), (3.4, 18.4, 13.8), mat("walnut")), box((12.6, 11.0, 13.0), (13.0, 18.4, 13.8), mat("walnut"))]
    if turn:
        for e in out:
            e["rotation"] = {"angle": turn, "axis": "y", "origin": [8, 0, 8]}
    return out


# ------------------------------------------------------------------ the bookcase
def _books(x0, x1, y0, y1, z_front, rng, tag):
    """Fills a shelf (x x0..x1, from y0, room to y1) with books, binders, a lying stack, a bookend, a thing."""
    out = []
    x = x0 + rng.uniform(0.1, 0.6)
    room = y1 - y0
    things = ["books"] * 5 + ["binders", "stack", "lean", "trophy", "drone", "plant", "photo"]
    while x < x1 - 0.8:
        kind = rng.choice(things)
        if kind in ("books", "lean"):
            n = rng.randint(3, 7)
            for k in range(n):
                t = rng.choice((0.6, 0.8, 1.0, 1.2))
                h = round(room * rng.uniform(0.62, 0.92) * 2) / 2
                if x + t > x1 - 0.1:
                    break
                c = rng.choice(list(BOOK_COLOURS))
                d = rng.uniform(5.5, 7.5)
                key = f"sp_{c}_{t}_{h}_{rng.randint(0, 5)}"
                sp = A.region(key, t, h, spine(c, zlib.crc32(key.encode()) % 997))
                out.append(box((x, y0, z_front - d), (x + t, y0 + h, z_front), {**mat(c), "up": {"texture": "#paper"}, "south": {"texture": "#atlas", "uv": sp}}))
                x += t + (0.05 if rng.random() < 0.8 else 0.25)
            if kind == "lean" and x < x1 - 2.0:
                c = rng.choice(list(BOOK_COLOURS))
                h = round(room * 0.8 * 2) / 2
                key = f"sp_{c}_1.0_{h}_l"
                sp = A.region(key, 1.0, h, spine(c, zlib.crc32(key.encode()) % 997))
                out.append(box((x + 0.2, y0, z_front - 6.5), (x + 1.2, y0 + h, z_front), {**mat(c), "up": {"texture": "#paper"}, "south": {"texture": "#atlas", "uv": sp}},
                               {"angle": -22.5, "axis": "z", "origin": [x + 0.2, y0, z_front - 3]}))
                x += 1.2 + h * 0.4
            x += rng.uniform(0.3, 0.9)
        elif kind == "binders":
            for k in range(rng.randint(2, 4)):
                if x + 1.6 > x1 - 0.1 or room < 7:
                    break
                c = rng.choice(("bk_navy", "bk_black", "bk_grey", "bk_blue", "bk_white"))
                h = min(room - 0.4, 7.0)
                key = f"bn_{c}_{h}_{k % 3}"
                sp = A.region(key, 1.6, h, spine(c, zlib.crc32(key.encode()) % 997, binder=True))
                out.append(box((x, y0, z_front - 7.6), (x + 1.6, y0 + h, z_front), {**mat(c), "up": {"texture": "#" + c}, "south": {"texture": "#atlas", "uv": sp}}))
                x += 1.65
            x += 0.4
        elif kind == "stack" and x < x1 - 4.5:
            yy = y0
            for k in range(rng.randint(2, 4)):
                c = rng.choice(list(BOOK_COLOURS))
                th = rng.choice((0.5, 0.7))
                w = rng.uniform(3.6, 4.4)
                out.append(box((x + rng.uniform(0, 0.3), yy, z_front - 5.6), (x + w, yy + th, z_front - rng.uniform(0, 0.4)), {**mat(c), "south": {"texture": "#paper"}}))
                yy += th
            x += 4.8
        elif kind == "trophy" and x < x1 - 2.6 and room > 5:
            cx = x + 1.2
            out += [box((cx - 1.0, y0, z_front - 3.6), (cx + 1.0, y0 + 1.0, z_front - 1.6), mat("black"))]
            out += round_part(cx, z_front - 2.6, 0.3, y0 + 1.0, y0 + 2.2, "gold")
            out += round_ring(cx, z_front - 2.6, 0.9, 0.12, y0 + 2.2, y0 + 3.8, "gold", 0.3, "gold", n=4)
            x += 2.8
        elif kind == "drone" and x < x1 - 5.0 and room > 4:
            out += model_drone(x + 2.5, z_front - 3.2, y0)
            x += 5.2
        elif kind == "plant" and x < x1 - 2.6 and room > 4:
            cx, cz = x + 1.2, z_front - 3.0
            out += round_ring(cx, cz, 1.0, 0.15, y0, y0 + 1.6, "pot", 1.3, "soil", n=4)
            out += snake_plant(cx, cz, y0 + 1.3, scale=min(0.55, (room - 1.8) / 8.0))
            x += 2.6
        elif kind == "photo" and x < x1 - 3.0 and room > 4:
            pk = "photo_fleet" if rng.random() < 0.5 else "photo_dusk"
            lean = {"angle": -22.5, "axis": "x", "origin": [x + 1.4, y0, z_front - 1.6]}
            out += [box((x, y0, z_front - 2.0), (x + 2.8, y0 + 2.4, z_front - 1.6), mat("frame"), lean),
                    box((x + 0.25, y0 + 0.25, z_front - 1.6), (x + 2.55, y0 + 2.15, z_front - 1.5), {"south": {"texture": "#atlas", "uv": A.region(pk + "_s", 2.3, 1.9, photo(pk[6:]))}}, lean),
                    box((x + 1.2, y0, z_front - 2.95), (x + 1.6, y0 + 1.5, z_front - 2.6), mat("frame"))]
            x += 3.2
        else:
            x += 0.5
        if rng.random() < 0.15 and x < x1 - 1.2:                       # a bookend
            out += [box((x, y0, z_front - 4.0), (x + 0.2, y0 + 3.0, z_front - 1.0), mat("black")), box((x, y0, z_front - 4.0), (x + 1.6, y0 + 0.12, z_front - 1.0), mat("black"))]
            x += 0.5
    return out


def model_drone(cx, cz, y0):
    """A scale model of the T1 courier quad on its display base (the tower's mark in brass on its front): the body, its
    top shell and stripe, the four arms, the motors, two-bladed props, the camera under its nose, the landing legs and
    skids, the navigation lights; held up on a brass rod. Its nose to the south (the shelf's front)."""
    out = [box((cx - 1.8, y0, cz - 1.3), (cx + 1.8, y0 + 0.5, cz + 1.3), mat("walnut_dark")),
           box((cx - 0.9, y0 + 0.14, cz + 1.3), (cx + 0.9, y0 + 0.36, cz + 1.42), mat("brass")),
           box((cx - 0.1, y0 + 0.5, cz - 0.1), (cx + 0.1, y0 + 1.75, cz + 0.1), mat("brass"))]
    b = y0 + 1.75
    out += [box((cx - 0.7, b, cz - 0.9), (cx + 0.7, b + 0.5, cz + 0.9), mat("black")),
            box((cx - 0.5, b + 0.5, cz - 0.7), (cx + 0.5, b + 0.72, cz + 0.6), mat("graphite")),
            box((cx - 0.12, b + 0.72, cz - 0.6), (cx + 0.12, b + 0.84, cz + 0.5), mat("orange")),
            box((cx - 0.25, b - 0.3, cz + 0.55), (cx + 0.25, b, cz + 0.95), mat("black")),
            box((cx - 0.12, b - 0.24, cz + 0.95), (cx + 0.12, b - 0.08, cz + 1.05), mat("graphite"))]
    for ang in (45, -45):                                                    # the arms, one box a diagonal
        out.append(box((cx - 2.1, b + 0.18, cz - 0.12), (cx + 2.1, b + 0.38, cz + 0.12), mat("graphite"),
                       {"angle": ang, "axis": "y", "origin": [cx, b, cz]}))
    for k, (dx, dz) in enumerate(((1.48, 1.48), (-1.48, 1.48), (1.48, -1.48), (-1.48, -1.48))):
        mx, mz = cx + dx, cz + dz
        out += round_part(mx, mz, 0.28, b + 0.12, b + 0.62, "black")
        out.append(box((mx - 0.8, b + 0.66, mz - 0.09), (mx + 0.8, b + 0.72, mz + 0.09), mat("graphite"),
                       {"angle": (22.5, -45, 45, -22.5)[k], "axis": "y", "origin": [mx, b, mz]}))
        if dz > 0:                                                           # the navigation lights at the front
            out.append({**box((mx - 0.08, b + 0.2, mz + 0.28), (mx + 0.08, b + 0.34, mz + 0.4), mat("red" if dx < 0 else "green")), "shade": False})
    for sx in (-0.55, 0.45):
        out += [box((cx + sx, b - 0.45, cz - 0.55), (cx + sx + 0.1, b, cz - 0.45), mat("black")),
                box((cx + sx, b - 0.45, cz + 0.35), (cx + sx + 0.1, b, cz + 0.45), mat("black")),
                box((cx + sx - 0.02, b - 0.55, cz - 0.9), (cx + sx + 0.12, b - 0.45, cz + 0.8), mat("black"))]
    return out


def bookcase(half, v):
    """One block of the two-high walnut bookcase, its user looking north at its front (the wall behind it to the north):
    the lower block its plinth, cupboard (two doors, knobs) and a short shelf of books above; the upper block two
    shelves of books and things, the top. v picks what is on the shelves."""
    w = "walnut"
    rng = random.Random(4100 + v * 17 + (half == "upper") * 5)
    out = [box((0.4, 0, 1.0), (1.2, 16, 14.6), mat(w, skip=("up", "down") if half == "lower" else ("down",))),
           box((14.8, 0, 1.0), (15.6, 16, 14.6), mat(w, skip=("up", "down") if half == "lower" else ("down",))),
           box((1.2, 0, 1.0), (14.8, 16, 1.6), mat(w))]
    if half == "lower":
        out[0]["faces"]["down"] = out[1]["faces"]["down"] = {"texture": "#walnut"}
        out += [box((1.2, 0, 2.0), (14.8, 1.0, 14.2), mat("black")), box((1.2, 1.0, 1.6), (14.8, 1.6, 14.6), mat(w)),
                box((1.2, 9.6, 1.6), (14.8, 10.2, 14.6), mat(w))]
        for x0, x1, kx in ((1.4, 7.9, 7.2), (8.1, 14.6, 8.8)):
            out += [box((x0, 1.8, 14.2), (x1, 9.4, 14.8), mat(w)), box((kx - 0.25, 5.2, 14.8), (kx + 0.25, 6.0, 15.3), mat("brass"))]
        out += _books(1.2, 14.8, 10.2, 16.0, 14.4, rng, "l")
    else:
        out += [box((1.2, 0, 1.6), (14.8, 0.6, 14.6), mat(w)), box((1.2, 7.8, 1.6), (14.8, 8.4, 14.6), mat(w)),
                box((0.2, 15.2, 0.6), (15.8, 16.0, 15.0), mat(w))]
        out += _books(1.2, 14.8, 0.6, 7.8, 14.4, rng, "u0") + _books(1.2, 14.8, 8.4, 15.2, 14.4, rng, "u1")
    return out


BOOKCASES = 6


# ------------------------------------------------------------------ the sofa
def sofa(left, right):
    """One seat of the leather sofa, its sitter looking north (its back to the south); left/right: an arm at that end
    (the user's left is west). The cushions part a little at every seat."""
    L = "leather"
    xa = 3.2 if left else 0.2
    xb = 12.8 if right else 15.8
    out = [box((0, 1.2, 1.4), (16, 5.0, 15.6), {**mat(L), "down": {"texture": "#black"}}),
           box((xa, 5.0, 2.0), (xb, 7.6, 13.0), mat(L)),
           box((0, 5.0, 13.4), (16, 12.4, 15.6), mat(L)),
           box((xa + 0.2, 7.6, 10.2), (xb - 0.2, 15.0, 12.8), mat(L), {"angle": 22.5, "axis": "x", "origin": [8, 7.6, 12.8]})]
    for on, x0, x1 in ((left, 0.0, 3.0), (right, 13.0, 16.0)):
        if on:
            out += [box((x0, 5.0, 1.4), (x1, 10.4, 15.6), mat(L)), box((x0 + 0.2, 10.4, 1.6), (x1 - 0.2, 11.0, 15.4), mat(L)),
                    box((x0 + 0.8, 0, 2.0), (x0 + 2.2, 1.2, 3.4), mat("chrome")), box((x0 + 0.8, 0, 13.6), (x0 + 2.2, 1.2, 15.0), mat("chrome"))]
    if not left and not right:
        out.append(box((7.2, 0, 7.2), (8.8, 1.2, 8.8), mat("black")))
    for k in out:
        if k["from"][0] == 0 and not left:
            k["faces"].pop("west", None)
        if k["to"][0] == 16 and not right:
            k["faces"].pop("east", None)
    return out


# ------------------------------------------------------------------ tables
def coffee_table():
    """Built whole along x 0..32: walnut top on a black steel frame, the shelf; magazines, the onyx ashtray with a lit cigar
    resting in it (its smoke rises in game: particles from CIGAR_TIP), a whisky on the rocks on a coaster."""
    w = "walnut"
    out = [box((0.6, 6.4, 2.6), (31.4, 7.2, 13.4), mat(w)), box((1.2, 1.6, 3.2), (30.8, 2.0, 12.8), mat(w))]
    for x0 in (1.0, 30.0):
        for z0 in (3.0, 12.0):
            out.append(box((x0, 0, z0), (x0 + 1.0, 6.4, z0 + 1.0), mat("steel")))
    out += [box((2.0, 6.0, 3.0), (30.0, 6.4, 3.6), mat("steel")), box((2.0, 6.0, 12.4), (30.0, 6.4, 13.0), mat("steel"))]
    mags = [("DRONE OPS", (30, 70, 140)), ("SKYWAYS", (170, 50, 40))]
    out += [box((5.0, 7.2, 5.0), (10.6, 7.4, 12.2), faces("paper", up=A.region("mag0", 5.6, 7.2, magazine(*mags[0])))),
            box((6.0, 7.4, 4.6), (11.6, 7.6, 11.8), faces("paper", up=A.region("mag1", 5.6, 7.2, magazine(*mags[1]))),
                {"angle": 22.5, "axis": "y", "origin": [8.8, 7.4, 8.2]})]
    out += round_ring(20.0, 8.0, 1.9, 0.35, 7.2, 8.0, "onyx", 0.3, "ash", n=6)
    cz = 8.0
    out += [box((19.2, 7.95, cz - 0.25), (20.0, 8.45, cz + 0.25), mat("ash")),                  # the ash
            {**box((20.0, 7.97, cz - 0.24), (20.14, 8.43, cz + 0.24), mat("ember")), "shade": False},
            box((20.14, 7.95, cz - 0.27), (24.4, 8.49, cz + 0.27), mat("cigar")),
            box((22.6, 7.9, cz - 0.32), (23.2, 8.54, cz + 0.32), mat("cigar_band")),
            box((24.4, 8.02, cz - 0.2), (24.7, 8.42, cz + 0.2), mat("cigar"))]
    out += round_part(26.8, 6.0, 1.2, 7.2, 7.4, "black")
    out += round_ring(26.8, 6.0, 0.95, 0.12, 7.4, 9.3, "glass", 0.95, "whisky", n=4)
    out += [box((26.35, 8.05, 5.6), (26.9, 8.6, 6.15), mat("ice")),
            box((26.85, 8.1, 5.9), (27.35, 8.6, 6.4), mat("ice"), {"angle": 22.5, "axis": "y", "origin": [27.1, 8.1, 6.15]})]
    out += [box((4.0, 2.0, 5.0), (12.0, 2.8, 11.0), mat("bk_navy")), box((4.4, 2.8, 5.4), (11.2, 3.4, 10.4), mat("bk_tan"))]
    return out


CIGAR_TIP = (19.2, 8.2, 8.0)          # (pixels, the coffee table built whole: the smoke's source)


def cigar_smoke():
    """The cigar's smoke as the mock draws it (in game: a thin wisp of smoke particles from the tip), in the block over
    the coffee table's part 1, its pixels the table's: a column of pale wisps rising and drifting."""
    x, y, z = CIGAR_TIP[0] - 16, CIGAR_TIP[1] - 16, CIGAR_TIP[2]
    out = []
    for k in range(9):
        h = y + 0.3 + k * 1.25
        dx = 0.5 * math.sin(k * 0.9)
        dz = 0.35 * math.cos(k * 0.7)
        w = 0.25 + k * 0.09
        out.append(box((x + dx - w, h, z + dz - w), (x + dx + w, h + 1.1, z + dz + w), mat("smoke")))
    return out


def display_plinth():
    """A walnut plinth for the model drone (its stand and drone on top), its brass plaque to the north (the viewer)."""
    return [box((1.0, 0, 1.0), (15.0, 0.8, 15.0), mat("black")),
            box((1.6, 0.8, 1.6), (14.4, 14.6, 14.4), mat("walnut")),
            box((1.5, 13.4, 1.5), (14.5, 13.7, 14.5), mat("brass")),
            box((0.8, 14.6, 0.8), (15.2, 16.0, 15.2), mat("walnut")),
            box((4.6, 9.6, 1.4), (11.4, 12.2, 1.6), faces("brass", north=A.region("plinth_plate", 6.8, 2.6, plinth_plate)))]


def plinth_plate(img):
    W, H = img.size
    img.paste(TK.brushed((200, 164, 90), 3132, 3).resize((W, H)), (0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, W - 2, H - 2), outline=(150, 118, 56))
    for t, y, sz, f in (("T1 COURIER QUAD", 0.34, 11, SERIF), ("THE TOWER'S FIRST DRONE", 0.7, 7, SERIF_R)):
        d.text((W / 2, H * y), t, font=_text_fit(d, t, f, sz, W - 10), fill=(60, 44, 18), anchor="mm")
    return img


def cafe_table():
    out = round_part(8, 8, 6.6, 11.6, 12.4, "walnut", n=8) + round_part(8, 8, 0.8, 1.0, 11.6, "black", n=3) + round_part(8, 8, 4.0, 0, 1.0, "black", n=5)
    out += [box((5.0, 12.4, 6.0), (8.6, 12.7, 11.0), faces("paper", up=A.region("pad_cafe", 3.6, 5.0, TK.paper(77)))),
            box((7.8, 12.7, 6.6), (8.1, 13.0, 10.2), mat("black"), {"angle": 22.5, "axis": "y", "origin": [7.95, 12.7, 8.4]})]
    return out


# ------------------------------------------------------------------ the credenza behind the desk
def printer():
    """The office printer on the credenza (its x 2..12 there): the body, the output slot with a sheet in it, the panel."""
    p = "printer"
    return [box((2.0, 11.6, 3.4), (12.0, 13.0, 13.2), mat(p)), box((2.0, 13.0, 3.4), (12.0, 13.6, 6.0), mat(p)),
            box((2.0, 13.6, 3.4), (12.0, 15.6, 13.2), mat(p)),
            box((3.0, 13.05, 6.0), (11.0, 13.25, 12.8), faces("paper", up=A.region("sheet_1", 8.0, 6.8, page_sheet(1)))),
            box((8.4, 15.6, 10.6), (11.6, 16.0, 12.8), faces("graphite", up=SCR.face("printer", 3.2, 2.2, printer_panel))),
            box((2.6, 11.6, 13.2), (11.4, 12.6, 13.6), mat("graphite"))]


def exec_credenza():
    """Built whole along x 0..48, its user looking north at its front: walnut, doors either end with brass pulls,
    binders in the open middle; on it the printer, a photo and a plant."""
    w = "walnut"
    out = [box((1.0, 0, 2.4), (47.0, 0.8, 14.0), mat("black")),
           box((0, 10.8, 1.2), (48, 11.6, 15.4), mat(w)),
           box((0.4, 0.8, 1.6), (1.4, 10.8, 15.0), mat(w)), box((46.6, 0.8, 1.6), (47.6, 10.8, 15.0), mat(w)),
           box((1.4, 0.8, 1.6), (46.6, 10.8, 2.2), mat(w)), box((1.4, 0.8, 2.2), (46.6, 1.6, 15.0), mat(w)),
           box((15.4, 1.6, 2.2), (16.6, 10.8, 15.0), mat(w)), box((31.4, 1.6, 2.2), (32.6, 10.8, 15.0), mat(w)),
           box((16.6, 6.0, 2.2), (31.4, 6.6, 15.0), mat(w))]
    for x0, x1 in ((1.6, 8.3), (8.5, 15.2), (32.8, 39.5), (39.7, 46.4)):
        out.append(box((x0, 1.8, 14.6), (x1, 10.6, 15.2), mat(w)))
        px = x1 - 0.8 if x0 in (1.6, 32.8) else x0 + 0.8
        out.append(box((px - 0.2, 4.0, 15.2), (px + 0.2, 8.4, 15.6), mat("brass")))
    out += _books(16.6, 31.4, 1.6, 6.0, 14.6, random.Random(4201), "c0") + _books(16.6, 31.4, 6.6, 10.8, 14.6, random.Random(4202), "c1")
    out += printer()
    lean = {"angle": -22.5, "axis": "x", "origin": [24.0, 11.6, 8.0]}
    out += [box((21.8, 11.6, 7.6), (26.2, 15.0, 8.0), mat("frame"), lean),
            box((22.1, 11.9, 8.0), (25.9, 14.7, 8.1), {"south": {"texture": "#atlas", "uv": A.region("photo_fleet", 3.8, 2.8, photo("fleet"))}}, lean),
            box((23.8, 11.6, 6.45), (24.2, 13.6, 6.8), mat("frame"))]
    cx, cz = 42.0, 8.0
    out += round_ring(cx, cz, 2.0, 0.25, 11.6, 14.4, "pot", 2.4, "soil", n=5)
    out += snake_plant(cx, cz, 14.0)
    return out


# ------------------------------------------------------------------ on the walls
def framed(kind):
    """A frame on the wall, its face to the north (the wall to the south): the moulding, the mat, the certificate under
    its glass; or the award plaque (a walnut board, its brass plate)."""
    if kind == "plaque":
        return [box((4.0, 3.0, 15.2), (12.0, 13.0, 16.0), mat("walnut_dark")),
                box((4.8, 4.0, 15.0), (11.2, 12.0, 15.2), faces("brass", north=A.region("plaque", 6.4, 8.0, plaque_plate)))]
    f = "frame"
    return [box((1.5, 2.5, 15.0), (14.5, 3.5, 16.0), mat(f)), box((1.5, 12.5, 15.0), (14.5, 13.5, 16.0), mat(f)),
            box((1.5, 3.5, 15.0), (2.5, 12.5, 16.0), mat(f)), box((13.5, 3.5, 15.0), (14.5, 12.5, 16.0), mat(f)),
            box((2.5, 3.5, 15.4), (13.5, 12.5, 16.0), faces("mat", north=A.region(f"cert_{kind}", 11.0, 9.0, certificate(kind))))]


CERTS = ["operator", "plaque", "million"]


def coat_stand(half):
    """The coat stand (two blocks high): its round foot, the walnut pole, the hooks; a navy jacket on one."""
    if half == "lower":
        return round_part(8, 8, 3.2, 0, 0.6, "black", n=5) + round_part(8, 8, 0.5, 0.6, 16.0, "walnut")
    out = round_part(8, 8, 0.5, 0, 12.0, "walnut") + round_part(8, 8, 0.7, 12.0, 12.6, "walnut")
    for ang in (0.0, 90.0, 180.0, 270.0):
        dx, dz = math.sin(math.radians(ang)), -math.cos(math.radians(ang))
        hx, hz = 8 + dx * 1.2, 8 + dz * 1.2
        out.append(box((hx - 0.2, 9.0, hz - 0.2), (hx + 0.2, 10.6, hz + 0.2), mat("chrome")))
    # the jacket hung from the south hook
    out += [box((5.0, 5.0, 9.4), (11.0, 9.0, 11.2), mat("navy_cloth")), box((6.6, 9.0, 9.2), (9.4, 9.6, 10.4), mat("navy_cloth"))]
    return out + [box((5.0, -8.0, 9.6), (11.0, 5.0, 11.0), mat("navy_cloth")),
                  box((4.2, -6.0, 9.7), (5.0, 7.2, 10.9), mat("navy_cloth")), box((11.0, -6.0, 9.7), (11.8, 7.2, 10.9), mat("navy_cloth"))]


def floor_lamp(half):
    """The floor lamp (two blocks high): four walnut legs splayed, the stem, the linen drum shade lit inside."""
    if half == "lower":
        out = []
        for ax, ang, f, t in (("x", 22.5, (7.6, 0, 7.6), (8.4, 16, 8.4)), ("x", -22.5, (7.6, 0, 7.6), (8.4, 16, 8.4)),
                              ("z", 22.5, (7.6, 0, 7.6), (8.4, 16, 8.4)), ("z", -22.5, (7.6, 0, 7.6), (8.4, 16, 8.4))):
            out.append(box(f, t, mat("walnut"), {"angle": ang, "axis": ax, "origin": [8, 16, 8]}))
        return out
    out = round_part(8, 8, 0.28, 0, 6.0, "black")
    ring = round_ring(8, 8, 4.4, 0.25, 6.0, 12.4, "linen", 0.0, "linen", n=7)
    ring = [e for e in ring if e["to"][1] - e["from"][1] > 0.1]
    for e in ring:
        e["shade"] = False
    out += ring + [{**box((7.0, 10.6, 7.0), (9.0, 11.2, 9.0), {**mat("light")}), "shade": False}]
    return out


def waste_basket():
    """The waste basket: a round mesh basket, two crumpled sheets in the bottom of it (well inside its wall)."""
    return round_ring(8, 8, 2.4, 0.2, 0, 5.2, "basket", 0.3, "basket", n=6) + [
        box((6.9, 0.3, 6.7), (8.5, 1.7, 8.3), mat("paper"), {"angle": 22.5, "axis": "y", "origin": [7.7, 0.3, 7.5]}),
        box((7.8, 0.3, 7.9), (9.2, 1.5, 9.3), mat("paper"), {"angle": -22.5, "axis": "y", "origin": [8.5, 0.3, 8.6]})]


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None, mock=False):
    """Writes the textures, models and blockstates under `assets` (training_kit's materials too)."""
    global A, SCR
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    for name, make in {**TK.MATERIALS, **MATERIALS}.items():
        make().save(tex_dir / f"{name}.png")
    A = TK.Atlas(1024, TX, "atlas")
    SCR = TK.Atlas(1024, 4 * TX, "screens")
    TK.SCR = SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in {**TK.MATERIALS, **MATERIALS}}
    T.update({"atlas": ref("mg_atlas"), "screens": ref("mg_screens"), "particle": ref("mg_walnut")})

    def model(name, elements):
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, variants):
        (bs_dir / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")

    def faced(name, model_name, extra=""):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": y} if y else {})} for f, y in FACINGS.items()}

    for name, build in (("exec_desk", exec_desk), ("exec_credenza", exec_credenza), ("lounge_coffee_table", coffee_table)):
        parts = cut_parts(build(), 3 if name != "lounge_coffee_table" else 2)
        v = {}
        for p, els in enumerate(parts):
            model(f"{name}_{p}", els)
            v.update(faced(name, f"{name}_{p}", f",part={p}"))
        states(name, v)
    v = {}
    for k, sw in enumerate((0.0, 22.5, -22.5)):
        model(f"exec_chair_{k}", exec_chair(sw))
        v.update(faced("exec_chair", f"exec_chair_{k}", f",variant={k}"))
    states("exec_chair", v)
    v = {}
    for k, t in enumerate((0.0, 22.5, -22.5)):
        model(f"visitor_chair_{k}", visitor_chair(t))
        v.update(faced("visitor_chair", f"visitor_chair_{k}", f",variant={k}"))
    states("visitor_chair", v)
    v = {}
    for k in range(BOOKCASES):
        for half in ("lower", "upper"):
            model(f"office_bookcase_{half}_{k}", bookcase(half, k))
            v.update(faced("office_bookcase", f"office_bookcase_{half}_{k}", f",half={half},variant={k}"))
    states("office_bookcase", v)
    v = {}
    for left in (False, True):
        for right in (False, True):
            n = f"leather_sofa_{'l' if left else ''}{'r' if right else ''}" if (left or right) else "leather_sofa"
            model(n, sofa(left, right))
            v.update(faced("leather_sofa", n, f",left={'false' if left else 'true'},right={'false' if right else 'true'}"))
    states("leather_sofa", v)
    model("bistro_table", cafe_table())
    states("bistro_table", {"": {"model": ref("bistro_table")}})
    v = {}
    for k, kind in enumerate(CERTS):
        model(f"framed_certificate_{k}", framed(kind))
        v.update(faced("framed_certificate", f"framed_certificate_{k}", f",variant={k}"))
    states("framed_certificate", v)
    for name, build in (("coat_stand", coat_stand), ("floor_lamp", floor_lamp)):
        v = {}
        for half in ("lower", "upper"):
            model(f"{name}_{half}", build(half))
            v[f"half={half}"] = {"model": ref(f"{name}_{half}")}
        states(name, v)
    model("waste_basket", waste_basket())
    states("waste_basket", {"": {"model": ref("waste_basket")}})
    model("display_plinth", display_plinth())
    states("display_plinth", faced("display_plinth", "display_plinth"))
    if mock:
        model("cigar_smoke", cigar_smoke())
        states("cigar_smoke", faced("cigar_smoke", "cigar_smoke"))
    A.img.save(tex_dir / "mg_atlas.png")
    SCR.img.save(tex_dir / "mg_screens.png")
    return A.y + A.row, SCR.y + SCR.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
