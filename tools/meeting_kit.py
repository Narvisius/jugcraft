"""The meeting room (work list item 22 part B, room 11), to the standard Justin set on the lab: everything at its real
size (a pixel is 6.25 cm), modelled rather than painted on, the screens live in game, varied. Every same-facing pair
of faces in different materials stands at least 0.1 px apart as built (the depth buffer's rule), so model_writer has
nothing to nudge.

Pieces: boardroom_table (2 x 5: the walnut boat-shaped table, its two steel pedestals, the power modules, the
conference phone and its microphones, each place's things), vc_camera (on a wall: the PTZ camera on its bracket),
refreshment_credenza (3 x 1: the walnut sideboard and the coffee, cups, water, biscuits and fruit on it), logo_wall (3 x
2, on a wall: walnut slats, the tower's mark and name in brushed brass letters), pendant_light (1 x 5, hung from the
ceiling over the table). With them the briefing room's AV credenza, the manager's chairs, the offices' whiteboard, the
simulator room's acoustic panels, the wall screen, the clock, plants.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
import lab_kit as LK
import rest_kit as RK
import planning_kit as PK
import office_kit as OK
import class_kit as CK
from training_kit import box, mat, faces, round_part, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = SCR = PRT = None
_fit = MK._text_fit
ring, mirror_x = CK.ring, CK.mirror_x
cable, cut_z = RK.cable, RK.cut_z

MATERIALS = {
    "mt_felt_black": lambda: MK.weave((30, 30, 32), 4201, 1, 3), "mt_biscuit": lambda: TK.solid((206, 160, 96), 4202, 3.0),
    "mt_coffee_pot": lambda: TK.brushed((180, 184, 190), 4203, 2.0), "mt_brass": lambda: TK.brushed((190, 156, 84), 4204, 2.0),
    "mt_napkin": lambda: TK.solid((236, 226, 206), 4205, 1.0), "mt_tray": lambda: TK.solid((40, 40, 44), 4206, 1.0),
    "mt_orchid": lambda: TK.solid((236, 220, 240), 4207, 2.0), "mt_leaf": lambda: TK.solid((50, 110, 50), 4208, 2.5),
}


# ------------------------------------------------------------------ pictures
def phone_screen(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(14, 20, 28))
    # live: the call's length
    TK.live_part(img, (W * 0.5, H * 0.32), "IN CALL  00:42:10", "00:42:10", _fit(d, "IN CALL  00:42:10", FONT, round(H * 0.3), W * 0.9),
                 (140, 255, 170), "mm", bg=(14, 20, 28), src="timer", fmt="hms", period=5400)
    d.text((W * 0.5, H * 0.74), "HANGAR OPS + 2", font=_fit(d, "HANGAR OPS + 2", FONT_R, round(H * 0.26), W * 0.9), fill=(200, 210, 220), anchor="mm")
    return img


def module_top(img):
    """A power module's lid: its outline, the push mark, the sockets' icons."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(170, 174, 180))
    d.rectangle((2, 2, W - 3, H - 3), outline=(110, 114, 120), width=2)
    for k, t in enumerate(("230V", "USB", "HDMI")):
        d.text((W * (0.2 + k * 0.3), H * 0.5), t, font=_fit(d, "HDMI", FONT, round(H * 0.3), W * 0.24), fill=(60, 64, 70), anchor="mm")
    return img


def meet_page(kind):
    """A meeting laptop's screen (live in game): the meeting's agenda, or the deck being presented."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        ink, blue, green = (24, 30, 44), (30, 90, 200), (30, 150, 70)
        d.rectangle((0, 0, W, H), fill=(248, 248, 246))
        d.rectangle((0, 0, W, H * 0.11), fill=(34, 40, 48))
        if kind == "agenda":
            d.text((W * 0.02, H * 0.055), "OPS REVIEW  WEEK 43", font=_fit(d, "OPS REVIEW  WEEK 43", FONT, round(H * 0.06), W * 0.6), fill=(255, 255, 255), anchor="lm")
            d.text((W * 0.98, H * 0.055), "MINUTES ON", font=_fit(d, "MINUTES ON", FONT_R, round(H * 0.055), W * 0.3), fill=(150, 220, 160), anchor="rm")
            d.text((W * 0.04, H * 0.2), "AGENDA", font=_fit(d, "AGENDA", FONT, round(H * 0.085), W * 0.5), fill=ink, anchor="lm")
            items = (("Tower: tier 3, small hangars", "42%"), ("Deliveries this month", "NOW"), ("Pilot training: 16 booked", ""),
                     ("Simulator hours", ""), ("Safety: no incidents", ""), ("Any other business", ""))
            for k, (t, r) in enumerate(items):
                y = H * (0.32 + k * 0.105)
                if k == 1:
                    d.rectangle((W * 0.03, y - H * 0.045, W * 0.97, y + H * 0.045), fill=(222, 236, 255))
                d.text((W * 0.05, y), str(k + 1), font=_fit(d, "6", FONT, round(H * 0.055), W * 0.05), fill=blue, anchor="lm")
                d.text((W * 0.12, y), t, font=_fit(d, t, FONT_R, round(H * 0.055), W * 0.66), fill=ink if k > 0 else (110, 116, 128), anchor="lm")
                if r:
                    d.text((W * 0.95, y), r, font=_fit(d, r, FONT, round(H * 0.05), W * 0.14), fill=green if k == 1 else (150, 110, 20), anchor="rm")
        else:                                   # the deck: the slide being presented
            d.text((W * 0.02, H * 0.055), "DELIVERIES", font=_fit(d, "DELIVERIES", FONT, round(H * 0.06), W * 0.5), fill=(255, 255, 255), anchor="lm")
            d.text((W * 0.98, H * 0.055), "SLIDE 4 OF 12", font=_fit(d, "SLIDE 4 OF 12", FONT_R, round(H * 0.055), W * 0.3), fill=(220, 226, 236), anchor="rm")
            d.text((W * 0.04, H * 0.2), "DELIVERIES BY MONTH", font=_fit(d, "DELIVERIES BY MONTH", FONT, round(H * 0.085), W * 0.92), fill=ink, anchor="lm")
            x0, x1, y0, y1 = W * 0.1, W * 0.95, H * 0.84, H * 0.34
            d.line((x0, y1, x0, y0, x1, y0), fill=(120, 126, 136), width=2)
            vals = (38, 52, 61, 70, 84, 97)
            bw = (x1 - x0) / len(vals)
            for k, (m, v) in enumerate(zip(("MAY", "JUN", "JUL", "AUG", "SEP", "OCT"), vals)):
                xa = x0 + bw * (k + 0.2)
                ya = y0 - (y0 - y1) * v / 100
                d.rectangle((xa, ya, xa + bw * 0.6, y0 - 1), fill=green if k == 5 else (70, 120, 200))
                d.text((xa + bw * 0.3, y0 + H * 0.05), m, font=_fit(d, "MAY", FONT_R, round(H * 0.045), bw * 0.8), fill=ink, anchor="mm")
                d.text((xa + bw * 0.3, ya - H * 0.035), str(v), font=_fit(d, "97", FONT, round(H * 0.045), bw * 0.8), fill=ink, anchor="mm")
            yt = y0 - (y0 - y1) * 0.8
            for xa in range(int(x0), int(x1), 8):
                d.line((xa, yt, min(xa + 4, x1), yt), fill=(200, 60, 40), width=2)
            d.text((x1, yt - H * 0.03), "TARGET 80", font=_fit(d, "TARGET 80", FONT_R, round(H * 0.04), W * 0.2), fill=(200, 60, 40), anchor="rm")
        return img
    return draw


def pot_label(text, band):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=band)
        d.text((W / 2, H / 2), text, font=_fit(d, text, FONT, round(H * 0.6), W * 0.86), fill=(255, 255, 255), anchor="mm")
        return img
    return draw


# ------------------------------------------------------------------ small things on tables
def glass(cx, cz, y0, water=True):
    """A tumbler: its glass wall, the water in it."""
    return ring(cx, cz, 0.45, 0.06, y0, y0 + 1.5, "glass", 1.1 if water else 0.15, "water" if water else "glass")


def carafe(cx, cz, y0):
    """A water carafe on its coaster: the coaster, the glass body, the water, the neck."""
    return (round_part(cx, cz, 0.85, y0, y0 + 0.12, "walnut", n=3) + ring(cx, cz, 0.7, 0.06, y0 + 0.12, y0 + 2.6, "glass", 1.8, "water")
            + ring(cx, cz, 0.42, 0.06, y0 + 2.6, y0 + 3.6, "glass", 0.0, "glass"))


def cup_saucer(cx, cz, y0, colour="ceramic"):
    """A coffee cup on its saucer: the saucer, the cup, the coffee, the handle."""
    out = round_part(cx, cz, 0.8, y0, y0 + 0.12, colour, n=3) + ring(cx, cz, 0.5, 0.08, y0 + 0.12, y0 + 1.0, colour, 0.5, "coffee")
    out += [box((cx + 0.5, y0 + 0.4, cz - 0.07), (cx + 0.8, y0 + 0.8, cz + 0.07), mat(colour))]
    return out


def notepad(x0, z0, y0, seed, w=2.4, d=3.4):
    return [box((x0, y0, z0), (x0 + w, y0 + 0.2, z0 + d), faces("paper", up=PRT.face(f"mt_pad_{seed}", w, d, TK.paper(seed))))] + \
        [box((x0 + w + 0.3, y0, z0 + 0.5), (x0 + w + 0.48, y0 + 0.18, z0 + d - 0.4), mat("navy"))]


def phone_flat(x0, z0, y0):
    return [box((x0, y0, z0), (x0 + 1.1, y0 + 0.14, z0 + 2.3), mat("black")),
            box((x0 + 0.08, y0 + 0.14, z0 + 0.1), (x0 + 1.02, y0 + 0.24, z0 + 2.2), mat("glassblack"))]


# ------------------------------------------------------------------ the boardroom table
TT = 12.0             # the table's top (75 cm)


def boardroom_table():
    """The boardroom table (built along x 0..32 and z 0..80: 1.4 by 4.8 m, its head to the north toward the screen):
    the walnut top, boat-shaped (widest in the middle, its ends rounded off), its dark edge; two brushed steel
    pedestals on their black foot plates, the beam between under the top; two power modules let into the top, the
    conference phone in the middle (its lit ring, its screen live) and two microphones on the table; at each of the
    ten places down the sides: a notepad and pen, a glass of water, here a laptop (live), there
    a coffee or a phone; two carafes of water on their coasters."""
    top = {**mat("walnut_dark"), "up": {"texture": "#walnut"}}
    out = []
    for za, zb, xa, xb in ((1.6, 4.0, 7.0, 25.0), (4.0, 8.0, 5.6, 26.4), (8.0, 72.0, 4.8, 27.2), (72.0, 76.0, 5.6, 26.4), (76.0, 78.4, 7.0, 25.0)):
        out.append(box((xa, TT - 0.6, za), (xb, TT, zb), dict(top)))
    for zc in (20.0, 60.0):
        out += [box((9.0, 0.5, zc - 0.6), (23.0, TT - 0.6, zc + 0.6), mat("steel")),
                box((7.0, 0.0, zc - 3.6), (25.0, 0.5, zc + 3.6), mat("anod_black"))]
    out += [box((14.0, TT - 1.6, 20.6), (18.0, TT - 0.6, 59.4), mat("anod_black"))]
    # the power modules (raised a little out of the top), the phone, the microphones
    for zc in (24.0, 56.0):
        out.append(box((13.6, TT, zc - 1.6), (18.4, TT + 0.15, zc + 1.6), faces("alu", up=PRT.face("mt_module", 4.8, 3.2, module_top))))
    out += round_part(16.0, 40.0, 2.2, TT, TT + 0.4, "anod_black", n=5)
    out += [{**e, "shade": False} for e in ring(16.0, 40.0, 1.9, 0.15, TT + 0.4, TT + 0.5, "led_green", n=5)]
    out += round_part(16.0, 40.0, 1.0, TT + 0.4, TT + 0.6, "graphite", n=3)
    out += [box((15.2, TT + 0.6, 39.5), (16.8, TT + 0.7, 40.5), faces("black", up=SCR.face("mt_phone", 1.6, 1.0, phone_screen)))]
    for zc in (30.0, 50.0):
        out += round_part(16.0, zc, 0.6, TT, TT + 0.15, "anod_black", n=3) + [{**e, "shade": False} for e in round_part(16.0, zc, 0.15, TT + 0.15, TT + 0.25, "led_green", n=3)]
    out += carafe(16.0, 12.0, TT) + carafe(16.0, 68.0, TT)
    # the places: five down each side (their sitters at x 0 and x 32)
    things = ["laptop", "coffee", "", "phone", "", "", "laptop", "", "coffee", ""]
    for k in range(10):
        side, zi = k // 5, k % 5
        zc = 8.0 + zi * 16.0
        what = things[k]
        if side == 0:              # the west side: its sitter to the west, looking east
            out += notepad(5.6, zc - 3.6, TT, 4600 + k) + glass(10.4, zc - 4.6, TT)
            if what == "laptop":
                out += _laptop_east(6.0, zc + 0.6, TT, f"mt_lap_{k}")
            elif what == "coffee":
                out += cup_saucer(10.6, zc + 3.4, TT)
            elif what == "phone":
                out += phone_flat(9.0, zc + 1.2, TT)
        else:                      # the east side: looking west
            out += notepad(24.0, zc + 0.2, TT, 4600 + k) + glass(21.6, zc + 4.6, TT)
            if what == "laptop":
                out += _laptop_west(21.6, zc - 3.6, TT, f"mt_lap_{k}")
            elif what == "coffee":
                out += cup_saucer(21.4, zc - 3.4, TT)
            elif what == "phone":
                out += phone_flat(21.9, zc - 3.5, TT)
    return out


def _laptop_east(x0, z0, y0, key):
    """A laptop open toward a sitter on the west, looking east (the lid on its east side, leaning back)."""
    w, d, lid = 3.6, 5.4, 3.4                  # (its depth along x, its width along z)
    draw = meet_page("agenda")
    out = [box((x0, y0, z0), (x0 + w, y0 + 0.35, z0 + d), faces("alu", up=A.region("mt_deck", w, d, _deck_turned)))]
    out.append(box((x0 + w - 0.3, y0 + 0.35, z0 + 0.1), (x0 + w, y0 + 0.35 + lid, z0 + d - 0.1),
                   {**mat("alu"), "west": SCR.face(key, d - 0.2, lid, draw)}, {"angle": -22.5, "axis": "z", "origin": [x0 + w - 0.3, y0 + 0.35, z0 + d / 2]}))
    return out


def _laptop_west(x0, z0, y0, key):
    w, d, lid = 3.6, 5.4, 3.4
    draw = meet_page("slides")
    out = [box((x0, y0, z0), (x0 + w, y0 + 0.35, z0 + d), faces("graphite", up=A.region("mt_deck2", w, d, _deck_turned)))]
    out.append(box((x0, y0 + 0.35, z0 + 0.1), (x0 + 0.3, y0 + 0.35 + lid, z0 + d - 0.1),
                   {**mat("graphite"), "east": SCR.face(key, d - 0.2, lid, draw)}, {"angle": 22.5, "axis": "z", "origin": [x0 + 0.3, y0 + 0.35, z0 + d / 2]}))
    return out


def _deck_turned(img):
    W, H = img.size
    sub = TK.deck("silver")(Image.new("RGB", (H, W))).rotate(90, expand=True)
    img.paste(sub.resize((W, H)), (0, 0))
    return img


# ------------------------------------------------------------------ the camera
def vc_camera():
    """The video call camera on the wall (its front to the north): the wall plate and shelf, the camera's round base,
    its yoke, the head with its lens and the red light that shows it is live."""
    out = [box((5.0, 3.0, 15.6), (11.0, 9.0, 16.0), mat("anod_black")), box((4.4, 3.0, 10.6), (11.6, 3.5, 15.6), mat("anod_black"))]
    out += round_part(8.0, 13.0, 1.6, 3.5, 4.2, "graphite", n=4)
    out += [box((5.9, 4.2, 12.4), (6.4, 7.6, 13.6), mat("graphite")), box((9.6, 4.2, 12.4), (10.1, 7.6, 13.6), mat("graphite")),
            box((6.4, 4.8, 11.0), (9.6, 7.4, 14.6), mat("anod_black"))]
    out += swap_yz(round_part(8.0, 6.1, 1.0, 10.9, 11.0, "lens", n=4))
    out += swap_yz(round_part(8.0, 6.1, 0.7, 10.8, 10.9, "glass", n=3))
    out += [{**box((9.0, 7.0, 10.9), (9.3, 7.2, 11.0), mat("red")), "shade": False}]
    return out


# ------------------------------------------------------------------ the sideboard
CT = 13.4


def refreshment_credenza():
    """The walnut sideboard by the wall (2.9 m long; built along x 0..48, its doors to the north): the plinth, the
    carcass, four doors with brass pulls, the top; on it two pump pots (COFFEE and DECAF on their bands), the saucers
    and the cups upside down beside them, the sugar and the spoons, a carafe of water and glasses on a tray, a plate of
    biscuits, a bowl of fruit, napkins, a white orchid in its pot."""
    out = [box((1.2, 0, 7.4), (46.8, 0.8, 15.6), mat("black")), box((0.8, 0.8, 7.0), (47.2, CT - 0.4, 16.0), mat("walnut")),
           box((0.4, CT - 0.4, 6.6), (47.6, CT, 16.0), {**mat("walnut_dark"), "up": {"texture": "#walnut"}})]
    for k in range(4):
        x0 = 1.0 + k * 11.55
        out += [box((x0, 1.0, 6.85), (x0 + 11.35, CT - 0.6, 7.0), mat("walnut")),
                box((x0 + (10.4 if k % 2 == 0 else 0.6), 4.6, 6.55), (x0 + (10.75 if k % 2 == 0 else 0.95), 9.0, 6.85), mat("brass"))]
    y = CT
    # the coffee
    for k, (x, text, band) in enumerate(((4.0, "COFFEE", (60, 40, 30)), (7.0, "DECAF", (200, 90, 30)))):
        out += round_part(x, 11.0, 1.1, y, y + 3.6, "coffee_pot", n=4)
        out += [box((x - 0.9, y + 1.6, 9.7), (x + 0.9, y + 2.2, 9.9), faces("coffee_pot", north=SCR.face(f"mt_pot_{k}", 1.8, 0.6, pot_label(text, band))))]
        out += round_part(x, 11.0, 0.95, y + 3.6, y + 4.4, "black", n=4) + round_part(x, 11.0, 0.4, y + 4.4, y + 4.9, "black", n=3)
        out += [box((x - 0.15, y + 3.9, 9.3), (x + 0.15, y + 4.2, 9.9), mat("black"))]
    for k in range(4):
        out += round_part(11.0, 11.0, 1.0, y + k * 0.12, y + (k + 1) * 0.12, "ceramic", n=3)
    for k in range(6):
        cx, cz = 13.6 + (k % 3) * 1.3, 9.9 + (k // 3) * 1.6
        out += round_part(cx, cz, 0.5, y, y + 0.9, "ceramic", n=3)
    out += round_part(17.8, 13.8, 0.55, y, y + 1.1, "glass", n=3) + round_part(17.8, 13.8, 0.45, y + 1.1, y + 1.3, "steel", n=3)
    out += ring(19.6, 13.8, 0.5, 0.08, y, y + 1.0, "ceramic", 0.2, "ceramic")
    for k in range(3):
        out.append(box((19.4 + k * 0.15, y + 0.2, 13.6), (19.48 + k * 0.15, y + 1.8, 13.68 + k * 0.1), mat("steel")))
    # the water and the glasses on their tray
    out += [box((21.2, y, 9.0), (27.4, y + 0.2, 14.4), mat("tray"))]
    out += carafe(22.6, 11.6, y + 0.2)
    for k in range(6):
        out += round_part(24.2 + (k % 3) * 1.1, 10.0 + (k // 3) * 2.6, 0.45, y + 0.2, y + 1.7, "glass", n=3)
    # biscuits, fruit, napkins, the orchid
    out += round_part(30.4, 11.4, 1.5, y, y + 0.12, "ceramic", n=4)
    for k in range(6):
        a = k * math.pi / 3
        out += round_part(30.4 + 0.85 * math.cos(a), 11.4 + 0.85 * math.sin(a), 0.36, y + 0.12, y + 0.24, "biscuit", n=3)
    out += ring(35.6, 11.6, 1.6, 0.15, y, y + 0.9, "ceramic", 0.25, "ceramic", n=5)
    for k, (dx, dz, c) in enumerate(((-0.5, -0.5, "apple"), (0.5, -0.5, "fruit_orange"), (0.5, 0.5, "apple"), (-0.5, 0.5, "fruit_orange"))):
        out += round_part(35.6 + dx, 11.6 + dz, 0.42, y + 0.25, y + 1.1, c, n=3)
    out += [box((34.6, y + 1.1, 11.4), (37.0, y + 1.5, 11.8), mat("banana"), {"angle": 22.5, "axis": "y", "origin": [35.8, y + 1.1, 11.6]})]
    for k in range(4):
        out.append(box((39.2 + k * 0.05, y + k * 0.1, 10.0 + k * 0.05), (41.6 - k * 0.05, y + (k + 1) * 0.1, 12.4 - k * 0.05), mat("napkin")))
    out += round_part(44.6, 11.6, 0.9, y, y + 1.6, "ceramic", n=4) + round_part(44.6, 11.6, 0.75, y + 1.6, y + 1.7, "soil", n=3)
    out += [box((44.5, y + 1.7, 11.5), (44.7, y + 6.2, 11.7), mat("leaf")),
            box((43.6, y + 1.7, 11.2), (45.6, y + 1.9, 12.0), mat("leaf"), {"angle": 22.5, "axis": "x", "origin": [44.6, y + 1.8, 11.6]})]
    for k, (dx, dy) in enumerate(((-0.6, 5.2), (0.0, 5.8), (0.6, 5.0), (0.9, 4.4))):
        out.append(box((44.6 + dx - 0.35, y + dy, 11.25), (44.6 + dx + 0.35, y + dy + 0.6, 11.55), mat("orchid")))
    return out


# ------------------------------------------------------------------ the logo wall
def _letters(text, x0, y0, dot, z0, z1, tex):
    """Raised letters from the 5 x 7 font: each glyph's runs of dots as boxes (as seen from the front: x from the
    viewer's left)."""
    import dotfont
    out = []
    x = x0
    for ch in text:
        g = dotfont.G.get(ch, dotfont.G[" "])
        for r, row in enumerate(g):
            k = 0
            while k < 5:
                if row[k] == "#":
                    j = k
                    while j < 5 and row[j] == "#":
                        j += 1
                    out.append(box((x + k * dot, y0 + (6 - r) * dot, z0), (x + j * dot, y0 + (7 - r) * dot, z1), mat(tex)))
                    k = j
                else:
                    k += 1
        x += 6 * dot
    return out


def logo_wall():
    """The feature wall (3 x 2 on a wall, 2.8 by 1.8 m, its front to the north): the black felt behind, thirty walnut
    slats, and standing off them in brushed brass the tower's mark (its three pillars and the ring) and TRI-PILLAR,
    DRONE OPERATIONS under it."""
    out = [box((1.6, 1.6, 15.4), (46.4, 30.4, 16.0), mat("felt_black"))]
    for k in range(30):
        x = 1.84 + k * 1.48
        out.append(box((x, 1.6, 14.6), (x + 0.96, 30.4, 15.4), mat("walnut")))
    front = []
    # the mark: three pillars and the ring round them
    for k in range(3):
        x = 21.0 + k * 2.2
        front.append(box((x, 18.6, 13.8), (x + 1.4, 27.6, 14.3), mat("brass")))
    front += [box((19.6, 23.2, 13.3), (28.4, 23.8, 13.8), mat("brass"))]
    front += _letters("TRI-PILLAR", 24.0 - 10 * 6 * 0.62 / 2, 12.4, 0.62, 13.8, 14.3, "brass")
    front += _letters("DRONE OPERATIONS", 24.0 - 16 * 6 * 0.32 / 2, 8.6, 0.32, 13.8, 14.3, "brass")
    return out + mirror_x(front, 48.0)


# ------------------------------------------------------------------ the pendant
def pendant_light():
    """The linear pendant over the table (built along z 0..80 in one block's column, hung at 2 m: its middle on the
    block's east edge, over the table's middle): the black aluminium body, the lit diffuser under it, the end caps,
    the two steel cables up to the ceiling and their round canopies there."""
    out = [box((15.2, 1.0, 3.2), (16.8, 2.0, 76.8), mat("anod_black")),
           {**box((15.4, 0.9, 3.4), (16.6, 1.0, 76.6), mat("ring")), "shade": False}]
    for zc in (12.0, 68.0):
        out += [box((15.96, 2.0, zc - 0.04), (16.04, 31.8, zc + 0.04), mat("chrome"))]
        out += round_part(16.0, zc, 0.8, 31.6, 32.0, "anod_black", n=3)
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, PRT
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    import lounge_kit as LG
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **OK.MATERIALS, **CK.MATERIALS, **MATERIALS}
    for k in ("lk2_steel_black", "lk2_glassblack", "lk2_apple", "lk2_fruit_orange", "lk2_banana"):
        mats[k] = LG.MATERIALS[k]
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(256, TX, "atlas")
    SCR = PK.ShelfAtlas(512, 3 * TX, "screens")
    PRT = PK.ShelfAtlas(512, 2 * TX, "prints")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK, OK):
        m.A, m.SCR = A, SCR
    CK.A, CK.SCR, CK.PRT = A, SCR, PRT
    ref = lambda n: f"{ns}:block/{n}"
    T = {(k[4:] if k.startswith("lk2_") else k[3:]): ref(k) for k in mats}
    T.update({"atlas": ref("mt_atlas"), "screens": ref("mt_screens"), "prints": ref("mt_prints"), "particle": ref("mg_walnut")})

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

    def faced(model_name, extra=""):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": y} if y else {})} for f, y in FACINGS.items()}

    def wide(name, els, n):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}"))
        return v

    v = {}
    for xi, col in enumerate(cut_parts(prep(boardroom_table()), 2)):
        for zi, part in enumerate(cut_z(col, 5)):
            p = zi * 2 + xi
            model(f"boardroom_table_{p}", part, True)
            v.update(faced(f"boardroom_table_{p}", f",part={p}"))
    states("boardroom_table", v)
    model("vc_camera", vc_camera())
    states("vc_camera", faced("vc_camera"))
    states("refreshment_credenza", wide("refreshment_credenza", refreshment_credenza(), 3))
    v = {}
    for h, half in zip(("lower", "upper"), RK.cut_y(prep(logo_wall()), 2)):
        for p, part in enumerate(cut_parts(half, 3)):
            model(f"logo_wall_{h}_{p}", part, True)
            v.update(faced(f"logo_wall_{h}_{p}", f",half={h},part={p}"))
    states("logo_wall", v)
    v = {}
    for p, part in enumerate(cut_z(prep(pendant_light()), 5)):
        model(f"pendant_light_{p}", part, True)
        v.update(faced(f"pendant_light_{p}", f",part={p}"))
    states("pendant_light", v)
    A.img.save(tex_dir / "mt_atlas.png")
    SCR.img.save(tex_dir / "mt_screens.png")
    PRT.img.save(tex_dir / "mt_prints.png")
    return [(a.y + a.row, a.size) for a in (A, SCR, PRT)]


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
