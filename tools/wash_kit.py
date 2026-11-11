"""The restrooms by the stairs (work list item 22 part B, room 12), to the standard Justin set on the lab: everything at
its real size (a pixel is 6.25 cm), modelled rather than painted on, the displays live in game, varied. Every
same-facing pair of faces in different materials stands at least 0.1 px apart as built, so model_writer has nothing to
nudge.

Pieces (front to model north, back on the wall at z 16):
- vanity_counter (3 x 1): the women's room's quartz counter with three undermount basins, sensor taps and soap
  dispensers, the traps, stops and brackets under it; vanity_mirror (3 x 1, on the wall over it).
- sanitary_bin: the small lidded bin in a stall.
- mop_sink (1 x 1 x 2): the custodian's floor basin with its bumper, the stainless splash panels and the service faucet
  with its vacuum breaker and hose; mop_rack (1 x 1 x 2, wall): the rail, a mop, a broom and a lobby dustpan.
- chemical_dispenser (1 x 1 x 2, wall): the dilution station, its four products on the shelf under it, their tubes.
- janitor_cart (2 x 1): the cart, its vinyl bag, its sprays and rolls, the mop bucket and wringer docked at its end.
- supply_shelf (2 x 1 x 2, two variants): two wire shelving units stocked with the restrooms' paper, soap and liners.
- water_heater (1 x 1 x 2): the restrooms' 190 l electric water heater in its pan, the pipes, valves, expansion tank,
  the relief valve's discharge, the disconnect.
- wet_floor_sign: the yellow A-frame, open.
- floor_scrubber: the walk-behind scrubber parked on charge.
- bottle_filler (1 x 1 x 2, wall): the bi-level fountain with the bottle filler over it; its counter live in game.
With them the plant's restroom fittings: the stalls (the toilet gets a partition side so stalls can stand side by
side), the sinks, mirror, dryer, bins, the baby changer, signs.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

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
ring = CK.ring
cable, cut_y = RK.cable, RK.cut_y

MATERIALS = {
    "wr_quartz": lambda: TK.solid((214, 212, 206), 5101, 2.2), "wr_terrazzo": lambda: TK.solid((190, 190, 184), 5102, 7.0),
    "wr_mop": lambda: TK.solid((222, 216, 200), 5103, 9.0), "wr_bucket": lambda: TK.solid((232, 186, 28), 5104, 2.0),
    "wr_cart": lambda: TK.solid((66, 70, 76), 5105, 2.0), "wr_vinyl": lambda: TK.solid((226, 190, 40), 5106, 3.5),
    "wr_jug_blue": lambda: TK.solid((60, 120, 210), 5107, 1.5), "wr_jug_green": lambda: TK.solid((70, 170, 80), 5108, 1.5),
    "wr_jug_red": lambda: TK.solid((200, 60, 60), 5109, 1.5), "wr_jug_purple": lambda: TK.solid((130, 80, 170), 5110, 1.5),
    "wr_enamel": lambda: TK.solid((238, 238, 236), 5111, 1.5), "wr_exp": lambda: TK.solid((58, 104, 180), 5112, 2.0),
    "wr_hose": lambda: TK.solid((48, 120, 62), 5113, 2.0), "wr_scrub": lambda: TK.solid((214, 172, 36), 5114, 2.0),
    "wr_scrub_grey": lambda: TK.solid((92, 96, 102), 5115, 2.0), "wr_handle_yel": lambda: TK.solid((230, 196, 40), 5116, 1.5),
    "wr_bristle": lambda: TK.solid((60, 58, 56), 5117, 6.0), "wr_dust": lambda: TK.solid((44, 46, 50), 5118, 2.0),
}

CT = 13.6          # the vanity's top (0.85 m)


# ------------------------------------------------------------------ pictures
def mirror_pic(img):
    """A mirror: the tiled room behind you, soft and grey-blue, a highlight across it."""
    W, H = img.size
    a = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(a)
    for y in range(H):
        t = y / H
        c = (int(182 + 30 * t), int(192 + 26 * t), int(198 + 20 * t))
        d.line((0, y, W, y), fill=c)
    for k in range(0, W, max(4, W // 18)):                 # the tile wall's joints, far off and soft
        d.line((k, int(H * 0.55), k, H), fill=(200, 208, 212))
    for y in range(int(H * 0.55), H, max(4, H // 8)):
        d.line((0, y, W, y), fill=(200, 208, 212))
    d.rectangle((0, int(H * 0.5), W, int(H * 0.53)), fill=(120, 140, 170))          # the blue band
    for k in range(3):
        x0 = int(W * (0.12 + k * 0.08))
        d.polygon([(x0, 0), (x0 + W * 0.05, 0), (x0 + W * 0.05 - H * 0.6, H), (x0 - H * 0.6, H)], fill=(222, 228, 232))
    img.paste(a, (0, 0))
    return img


def label(title, sub, band, ink=(30, 30, 34), bg=(246, 244, 238)):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=bg)
        d.rectangle((0, 0, W, H * 0.28), fill=band)
        d.text((W * 0.5, H * 0.14), title, font=_fit(d, title, FONT, round(H * 0.2), W * 0.9), fill=(255, 255, 255), anchor="mm")
        for k, s in enumerate(sub):
            d.text((W * 0.5, H * (0.45 + k * 0.2)), s, font=_fit(d, s, FONT_R, round(H * 0.15), W * 0.9), fill=ink, anchor="mm")
        return img
    return draw


def case_print(brand, line1, line2, colour):
    """A carton's side: the maker's band, the product, the count; brown board round it."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(178, 138, 92))
        d.rectangle((W * 0.06, H * 0.12, W * 0.94, H * 0.88), fill=(236, 230, 214))
        d.rectangle((W * 0.06, H * 0.12, W * 0.94, H * 0.34), fill=colour)
        d.text((W * 0.5, H * 0.23), brand, font=_fit(d, brand, FONT, round(H * 0.16), W * 0.8), fill=(255, 255, 255), anchor="mm")
        d.text((W * 0.5, H * 0.52), line1, font=_fit(d, line1, FONT, round(H * 0.15), W * 0.82), fill=(30, 30, 34), anchor="mm")
        d.text((W * 0.5, H * 0.72), line2, font=_fit(d, line2, FONT_R, round(H * 0.12), W * 0.82), fill=(70, 70, 76), anchor="mm")
        return img
    return draw


def wet_print(img):
    """The A-frame's face: CAUTION in its black band, the slipping man in his triangle, WET FLOOR."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(240, 196, 30))
    d.rectangle((0, H * 0.06, W, H * 0.2), fill=(20, 20, 22))
    d.text((W * 0.5, H * 0.13), "CAUTION", font=_fit(d, "CAUTION", FONT, round(H * 0.1), W * 0.86), fill=(240, 196, 30), anchor="mm")
    cx, cy, r = W * 0.5, H * 0.46, W * 0.36
    d.polygon([(cx, cy - r), (cx + r, cy + r * 0.75), (cx - r, cy + r * 0.75)], outline=(20, 20, 22), width=max(2, W // 24))
    # the man, slipping
    d.ellipse((cx - W * 0.05 + W * 0.06, cy - r * 0.38, cx + W * 0.05 + W * 0.06, cy - r * 0.38 + W * 0.1), fill=(20, 20, 22))
    lw = max(2, W // 20)
    d.line((cx + W * 0.04, cy - r * 0.2, cx - W * 0.04, cy + r * 0.25), fill=(20, 20, 22), width=lw)
    d.line((cx - W * 0.04, cy + r * 0.25, cx - W * 0.16, cy + r * 0.55), fill=(20, 20, 22), width=lw)
    d.line((cx - W * 0.04, cy + r * 0.25, cx + W * 0.14, cy + r * 0.5), fill=(20, 20, 22), width=lw)
    d.line((cx + W * 0.02, cy - r * 0.05, cx + W * 0.17, cy - r * 0.2), fill=(20, 20, 22), width=lw)
    d.line((cx + W * 0.02, cy - r * 0.05, cx - W * 0.14, cy + r * 0.02), fill=(20, 20, 22), width=lw)
    for k, t in enumerate(("WET", "FLOOR")):
        d.text((W * 0.5, H * (0.74 + k * 0.12)), t, font=_fit(d, "FLOOR", FONT, round(H * 0.1), W * 0.8), fill=(20, 20, 22), anchor="mm")
    return img


def dispenser_front(img):
    """The dilution station's door: the maker's band, four products in their colours with their numbers."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(214, 216, 220))
    d.rectangle((0, 0, W, H * 0.2), fill=(40, 70, 130))
    d.text((W * 0.5, H * 0.1), "DILUTION CENTER", font=_fit(d, "DILUTION CENTER", FONT, round(H * 0.12), W * 0.9), fill=(255, 255, 255), anchor="mm")
    for k, (t, c) in enumerate((("1 GLASS", (60, 120, 210)), ("2 FLOOR", (70, 170, 80)), ("3 RESTROOM", (200, 60, 60)), ("4 DISINFECT", (130, 80, 170)))):
        x0 = W * (0.04 + k * 0.24)
        d.rectangle((x0, H * 0.3, x0 + W * 0.2, H * 0.52), fill=c)
        d.text((x0 + W * 0.1, H * 0.62), t.split()[1], font=_fit(d, "DISINFECT", FONT, round(H * 0.08), W * 0.21), fill=(30, 30, 34), anchor="mm")
        d.text((x0 + W * 0.1, H * 0.41), t.split()[0], font=_fit(d, "4", FONT, round(H * 0.14), W * 0.12), fill=(255, 255, 255), anchor="mm")
    d.text((W * 0.5, H * 0.86), "PRESS AND HOLD TO FILL", font=_fit(d, "PRESS AND HOLD TO FILL", FONT_R, round(H * 0.08), W * 0.9), fill=(60, 60, 66), anchor="mm")
    return img


def heater_label(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(244, 242, 236))
    d.rectangle((0, 0, W, H * 0.18), fill=(30, 60, 120))
    d.text((W * 0.5, H * 0.09), "ELECTRIC WATER HEATER", font=_fit(d, "ELECTRIC WATER HEATER", FONT, round(H * 0.11), W * 0.92), fill=(255, 255, 255), anchor="mm")
    for k, t in enumerate(("190 L / 50 GAL", "2 x 4500 W  240 V", "SET 49 C / 120 F", "CHECK RELIEF VALVE YEARLY")):
        d.text((W * 0.08, H * (0.3 + k * 0.17)), t, font=_fit(d, t, FONT_R, round(H * 0.11), W * 0.84), fill=(30, 30, 34), anchor="lm")
    return img


def cover_print(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(232, 232, 230))
    d.rectangle((1, 1, W - 2, H - 2), outline=(180, 180, 178), width=max(1, W // 40))
    d.text((W * 0.5, H * 0.5), "DANGER 240 V", font=_fit(d, "DANGER 240 V", FONT, round(H * 0.3), W * 0.8), fill=(180, 40, 30), anchor="mm")
    return img


def filler_screen(count):
    """The bottle filler's counter (live in game: one more bottle each fill) and its filter light."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(18, 24, 22))
        n = f"{count:06d}"
        d.text((W * 0.5, H * 0.36), n, font=_fit(d, "000000", FONT, round(H * 0.42), W * 0.86), fill=(150, 255, 170), anchor="mm")
        d.text((W * 0.5, H * 0.8), "BOTTLES SAVED", font=_fit(d, "BOTTLES SAVED", FONT_R, round(H * 0.22), W * 0.86), fill=(150, 220, 170), anchor="mm")
        TK.live(img, "counter", (W * 0.05, H * 0.1, W * 0.95, H * 0.62), base=count, digits=6, fg=[150, 255, 170], bg=[18, 24, 22])
        return img
    return draw


def scrub_panel(img):
    """The scrubber's dashboard: the key, the battery bar (charging), the brush and water buttons."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(40, 42, 46))
    for k in range(5):
        d.rectangle((W * (0.08 + k * 0.09), H * 0.2, W * (0.15 + k * 0.09), H * 0.45), fill=(90, 220, 110) if k < 4 else (60, 70, 64))
    d.text((W * 0.3, H * 0.72), "CHARGING", font=_fit(d, "CHARGING", FONT, round(H * 0.24), W * 0.44), fill=(150, 230, 160), anchor="mm")
    # live: the battery bars fill as it charges (an hour of the tower's time, then CHARGED)
    TK.live(img, "charge", (0, 0, W * 0.56, H), bg=[40, 42, 46], on=[90, 220, 110], off=[60, 70, 64], fg=[150, 230, 160],
            bars=[[round((0.08 + k * 0.09) / 0.56, 5), 0.2, round(0.07 / 0.56, 5), 0.25] for k in range(5)], text=[round(0.3 / 0.56, 5), 0.72, 0.24])
    for k, c in enumerate(((60, 120, 210), (230, 196, 40), (200, 60, 60))):
        d.ellipse((W * (0.62 + k * 0.12), H * 0.3, W * (0.71 + k * 0.12), H * 0.7), fill=c)
    return img


def jug_label(name, colour):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(244, 244, 240))
        d.rectangle((0, 0, W, H * 0.3), fill=colour)
        d.text((W * 0.5, H * 0.15), name, font=_fit(d, name, FONT, round(H * 0.2), W * 0.9), fill=(255, 255, 255), anchor="mm")
        d.text((W * 0.5, H * 0.55), "CONCENTRATE", font=_fit(d, "CONCENTRATE", FONT_R, round(H * 0.14), W * 0.9), fill=(40, 40, 44), anchor="mm")
        d.text((W * 0.5, H * 0.78), "3.78 L", font=_fit(d, "3.78 L", FONT_R, round(H * 0.14), W * 0.6), fill=(40, 40, 44), anchor="mm")
        return img
    return draw


def wheel_x(xc, yc, zc, r, w, tex):
    """A wheel turning about x (it rolls along z): round_part's slabs built in the y-x plane and turned."""
    names = {"up": "east", "down": "west", "east": "up", "west": "down", "north": "north", "south": "south"}
    out = []
    for e in round_part(yc, zc, r, xc - w / 2, xc + w / 2, tex, n=3):
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        out.append(box((y0, x0, z0), (y1, x1, z1), {names[k]: v for k, v in e["faces"].items()}))
    return out


# ------------------------------------------------------------------ the vanity
BASINS = (8.0, 24.0, 40.0)


def vanity_counter():
    """The women's room's vanity (3 m long, 0.55 m deep, its top at 0.85 m; built along x 0..48, its front to the
    north): the quartz top cut round three undermount basins, its front apron and backsplash; at each basin the sensor
    tap and the soap dispenser on the deck, under it the tailpiece and trap into the wall, the angle stops and braided
    hoses; two steel brackets holding it off the wall."""
    out = []
    z0, z1 = 7.2, 15.8
    bx0, bx1, bz0, bz1 = 3.6, 3.6, 9.0, 13.6         # a basin's opening: x c - 3.6 .. c + 3.6, z 9.0 .. 13.6
    top = {**mat("quartz")}
    out.append(box((0.4, CT - 0.8, z0), (47.6, CT, bz0), top))                         # the top's front band
    out.append(box((0.4, CT - 0.8, bz1), (47.6, CT, z1), top))                         # its back band
    edges = [0.4] + [v for c in BASINS for v in (c - bx0, c + bx1)] + [47.6]
    for a, b in zip(edges[::2], edges[1::2]):
        out.append(box((a, CT - 0.8, bz0), (b, CT, bz1), top))                        # between the basins
    out.append(box((0.4, CT - 3.0, z0), (47.6, CT - 0.8, z0 + 0.5), mat("quartz")))    # the apron
    out.append(box((0.4, CT, z1 - 0.5), (47.6, CT + 1.6, z1), mat("quartz")))          # the backsplash
    for k, c in enumerate(BASINS):
        # the bowl: its floor, four walls (inside the opening), the drain
        out.append(box((c - 3.4, CT - 3.0, bz0 + 0.2), (c + 3.4, CT - 2.8, bz1 - 0.2), mat("ceramic")))
        for (a, b) in (((c - 3.6, bz0), (c - 3.4, bz1)), ((c + 3.4, bz0), (c + 3.6, bz1))):
            out.append(box((a[0], CT - 3.0, a[1]), (b[0], CT - 0.1, b[1]), mat("ceramic")))
        for (za, zb) in ((bz0, bz0 + 0.2), (bz1 - 0.2, bz1)):
            out.append(box((c - 3.4, CT - 3.0, za), (c + 3.4, CT - 0.1, zb), mat("ceramic")))
        out += round_part(c, 11.3, 0.5, CT - 2.8, CT - 2.7, "chrome", n=3)
        # the tap: the body, the spout over the bowl, its aerator, the sensor's eye
        out += round_part(c, 14.6, 0.55, CT, CT + 3.0, "chrome", n=3)
        out.append(box((c - 0.36, CT + 2.4, 11.6), (c + 0.36, CT + 3.0, 14.6), mat("chrome")))
        out.append(box((c - 0.3, CT + 2.3, 11.7), (c + 0.3, CT + 2.4, 12.3), mat("black")))
        out.append(box((c - 0.2, CT + 1.4, 13.95), (c + 0.2, CT + 1.9, 14.05), mat("black")))
        # the soap dispenser beside it
        sx = c + 4.8
        out += round_part(sx, 14.6, 0.45, CT, CT + 2.2, "chrome", n=3)
        out.append(box((sx - 0.25, CT + 1.8, 12.9), (sx + 0.25, CT + 2.2, 14.6), mat("chrome")))
        out.append(box((sx - 0.5, CT + 2.2, 14.0), (sx + 0.5, CT + 2.6, 15.2), mat("chrome")))
        # under: the tailpiece, the trap's bend and its arm into the wall's escutcheon
        out += round_part(c, 11.3, 0.4, CT - 5.8, CT - 3.0, "chrome", n=3)
        out.append(box((c - 0.4, CT - 6.6, 10.9), (c + 0.4, CT - 5.8, 12.6), mat("chrome")))
        out.append(box((c - 0.4, CT - 6.0, 12.6), (c + 0.4, CT - 5.2, 15.7), mat("chrome")))
        out.append(box((c - 0.9, CT - 6.3, 15.7), (c + 0.9, CT - 4.9, 15.9), mat("chrome")))
        # the angle stops either side and their braided hoses up to the tap
        for sd in (-1.6, 1.6):
            out.append(box((c + sd - 0.35, CT - 4.6, 15.0), (c + sd + 0.35, CT - 3.9, 15.9), mat("chrome")))
            out.append(box((c + sd - 0.12, CT - 3.9, 15.0), (c + sd + 0.12, CT - 0.8, 15.24), mat("steel")))
    for x in (16.0, 32.0):                                                              # the brackets
        out.append(box((x - 0.4, CT - 6.0, 15.4), (x + 0.4, CT - 0.8, 15.9), mat("steel")))
        out.append(box((x - 0.4, CT - 1.4, 8.0), (x + 0.4, CT - 0.8, 15.4), mat("steel")))
    return out


def vanity_mirror():
    """The mirror over the vanity (2.9 m by 0.85 m, from 1.05 m up; its own block is the wall's second): the glass on a
    thin brushed steel frame, standing off the wall."""
    uv = A.region("wr_mirror", 46.0, 13.4, mirror_pic)
    return [box((0.8, 0.8, 15.6), (47.2, 14.6, 15.95), mat("chrome")),
            box((1.0, 1.0, 15.3), (47.0, 14.4, 15.6), faces("glass", north=uv))]


# ------------------------------------------------------------------ in the stalls
def sanitary_bin():
    """The stall's bin: a small white bin with its hinged lid, against the partition by the toilet."""
    out = [box((1.6, 0, 1.2), (4.8, 4.4, 5.6), mat("white"))]
    out.append(box((1.5, 4.4, 1.1), (4.9, 4.9, 5.7), mat("white")))
    out.append(box((2.6, 4.9, 1.1), (3.8, 5.1, 1.5), mat("graphite")))
    return out


# ------------------------------------------------------------------ the custodian's closet
def mop_sink():
    """The floor basin in the corner (0.7 m square, its curb 25 cm high; 1 x 1, two blocks high): the terrazzo basin,
    the black bumper on its front edge, the drain's grate; on the wall the stainless splash panels and the service
    faucet at 1.1 m with its two handles, the vacuum breaker on its spout, the hose to the basin, the bucket hook."""
    out = []
    x0, x1, z0, z1, h = 2.4, 13.6, 4.8, 15.9, 4.0
    out.append(box((x0, 0, z0), (x1, 0.6, z1), mat("terrazzo")))
    for (a, b) in (((x0, z0), (x1, z0 + 0.9)), ((x0, z1 - 0.9), (x1, z1)), ((x0, z0 + 0.9), (x0 + 0.9, z1 - 0.9)), ((x1 - 0.9, z0 + 0.9), (x1, z1 - 0.9))):
        out.append(box((a[0], 0.6, a[1]), (b[0], h, b[1]), mat("terrazzo")))
    out.append(box((x0 - 0.1, h - 1.2, z0 - 0.4), (x1 + 0.1, h + 0.2, z0), mat("rubber")))
    out += round_part(8.0, 10.4, 1.0, 0.6, 0.7, "chrome", n=3)
    # the splash panels on the walls behind and beside it
    out.append(box((x0, h, 15.6), (x1, 22.0, 15.9), mat("stainless")))
    # the faucet: the body across, two handles, the spout down, the breaker, the hook
    fy = 18.0
    out.append(box((4.6, fy - 0.5, 14.6), (11.4, fy + 0.5, 15.6), mat("chrome")))
    for x, cap in ((5.0, "red"), (11.0, "steel_blue")):
        out += swap_yz(round_part(x, fy, 0.6, 13.4, 14.6, "chrome", n=3))
        out.append(box((x - 0.9, fy - 0.2, 13.0), (x + 0.9, fy + 0.2, 13.4), mat(cap)))
    out += round_part(8.0, 14.2, 0.45, fy - 3.8, fy - 0.5, "chrome", n=3)
    out += round_part(8.0, 14.2, 0.75, fy - 2.2, fy - 1.4, "chrome", n=3)
    out.append(box((7.6, fy - 4.4, 13.8), (8.4, fy - 3.8, 14.6), mat("brass")))
    out.append(box((5.6, fy - 6.2, 15.0), (10.4, fy - 5.8, 15.6), mat("steel")))
    out += cable([(8.0, fy - 4.4, 14.2), (8.0, 9.0, 14.2), (8.0, 9.0, 11.0), (5.0, 9.0, 11.0), (5.0, 2.0, 11.0)], 0.35, "hose")
    return out


def mop_rack():
    """The rail on the wall at 1.4 m (two blocks high) and what hangs on it: a wet mop (its yellow handle, the clamp, the
    cotton head hanging to the floor), a push broom's head up and a lobby dustpan."""
    out = [box((1.0, 22.4, 14.6), (15.0, 23.4, 15.9), mat("alu"))]
    for x in (3.6, 8.0, 12.4):
        out.append(box((x - 0.8, 21.6, 13.6), (x + 0.8, 23.6, 14.6), mat("black")))
    # the mop: its handle through the clamp, the head's band, the strands
    out += round_part(3.6, 14.1, 0.35, 6.4, 30.4, "handle_yel", n=3)
    out.append(box((2.2, 5.6, 13.2), (5.0, 6.4, 15.0), mat("graphite")))
    for k in range(5):
        x = 1.8 + k * 0.75
        out.append(box((x, 0.4 + (k % 2) * 0.4, 12.8 + (k % 3) * 0.3), (x + 0.65, 5.6, 14.6 - (k % 2) * 0.3), mat("mop")))
    # the broom, its head up: the handle, the block, the bristles
    out += round_part(8.0, 14.1, 0.35, 2.8, 30.0, "oak", n=3)
    out.append(box((4.8, 30.0, 12.8), (11.2, 31.2, 15.4), mat("oak")))
    out.append(box((5.0, 31.2, 13.0), (11.0, 31.9, 15.2), mat("bristle")))
    # the lobby dustpan: the handle, the pan
    out += round_part(12.4, 14.1, 0.3, 3.0, 30.0, "black", n=3)
    out.append(box((10.4, 0.4, 12.6), (14.4, 3.0, 15.4), mat("dust")))
    return out


JUGS = (("GLASS", "jug_blue", (60, 120, 210)), ("FLOOR", "jug_green", (70, 170, 80)), ("RESTROOM", "jug_red", (200, 60, 60)),
        ("DISINFECT", "jug_purple", (130, 80, 170)))


def chemical_dispenser():
    """The dilution station on the wall (two blocks high): the dispenser at 1.15 m with its four buttons and two
    filling hoses; under it on a wire shelf the four 3.8 l jugs of concentrate, each with its feed tube up."""
    out = []
    out.append(box((2.4, 18.4, 12.6), (13.6, 25.6, 15.9), faces("pla_grey", north=PRT.face("wr_disp", 11.2, 7.2, dispenser_front))))
    for k in range(4):
        x = 11.5 - k * 2.7                       # (the viewer's left is +x: 1 GLASS on the left)
        out.append(box((x, 19.6, 12.3), (x + 1.4, 20.6, 12.6), mat(JUGS[k][1])))
    for x, y1 in ((5.0, 12.0), (11.0, 14.0)):
        out += cable([(x, 18.4, 13.6), (x, y1, 13.6)], 0.25, "black")
        out += round_part(x, 13.6, 0.4, y1 - 1.0, y1, "black", n=3)
    out.append(box((1.0, 5.6, 8.4), (15.0, 6.0, 15.9), mat("wire")))
    for x in (1.4, 14.2):
        out.append(box((x, 2.6, 15.0), (x + 0.4, 5.6, 15.9), mat("wire")))
    for k, (name, tex, col) in enumerate(JUGS):
        x0 = 11.8 - k * 3.4
        out.append(box((x0, 6.0, 9.4), (x0 + 2.8, 10.4, 12.2), faces(tex, north=PRT.face(f"wr_jug_{k}", 2.8, 2.6, jug_label(name, col)))))
        out.append(box((x0 + 0.9, 10.4, 10.2), (x0 + 1.9, 11.0, 11.2), mat("black")))
        out.append(box((x0 + 2.0, 9.0, 10.6), (x0 + 2.6, 10.4, 11.0), mat(tex)))
        tx = x0 + 1.4
        out += cable([(tx, 11.0, 10.7), (tx, 13.0, 10.7), (tx, 13.0, 13.4), (tx, 18.4, 13.4)], 0.12, "white")
    return out


def janitor_cart():
    """The cleaning cart (1.2 m by 0.55 m, 1 m high; built along x 0..32, its handle end east): the grey frame on
    four casters, its two shelves and top tray, the yellow vinyl bag at its west end; on top the sprays, a roll of
    liners, the toilet brush; on the shelf the paper rolls; docked at its east end the mop bucket and wringer."""
    out = []
    x0, x1, z0, z1 = 6.0, 22.8, 3.6, 12.4
    for (x, z) in ((x0 + 0.8, z0 + 0.8), (x1 - 0.8, z0 + 0.8), (x0 + 0.8, z1 - 0.8), (x1 - 0.8, z1 - 0.8)):
        out += swap_yz(round_part(x, 0.8, 0.8, z - 0.3, z + 0.3, "black", n=3))
        out.append(box((x - 0.5, 1.6, z - 0.5), (x + 0.5, 2.2, z + 0.5), mat("steel")))
    out.append(box((x0, 2.2, z0), (x1, 3.0, z1), mat("cart")))
    out.append(box((x0, 7.6, z0), (x1, 8.2, z1), mat("cart")))
    out.append(box((x0, 14.4, z0), (x1, 15.0, z1), mat("cart")))
    for z in (z0, z1 - 0.6):
        out.append(box((x0, 15.0, z), (x1, 15.6, z + 0.6), mat("cart")))
    for (x, z) in ((x0, z0), (x1 - 0.8, z0), (x0, z1 - 0.8), (x1 - 0.8, z1 - 0.8)):
        out.append(box((x, 3.0, z), (x + 0.8, 14.4, z + 0.8), mat("cart")))
    # the bag at the west end, on its hoop
    out.append(box((0.6, 3.0, z0 + 0.6), (x0, 14.0, z1 - 0.6), mat("vinyl")))
    out.append(box((0.4, 14.0, z0 + 0.4), (x0, 14.6, z1 - 0.4), mat("cart")))
    # on the top tray: three sprays, the liners, the brush in its holder
    for k, tex in enumerate(("jug_blue", "jug_green", "jug_red")):
        cx = 9.0 + k * 2.0
        out += round_part(cx, 6.4, 0.6, 15.0, 18.0, tex, n=3)
        out.append(box((cx - 0.4, 18.0, 5.8), (cx + 0.4, 19.2, 7.0), mat("white")))
        out.append(box((cx - 0.15, 18.4, 5.3), (cx + 0.15, 18.8, 5.8), mat("white")))
    out += swap_yz(round_part(16.4, 15.9, 0.9, 5.2, 9.6, "black", n=3))
    out += round_part(20.0, 9.6, 0.9, 15.0, 17.6, "white", n=3)
    out += round_part(20.0, 9.6, 0.2, 17.6, 22.0, "white", n=3)
    # on the shelf: paper rolls
    for k in range(4):
        out += round_part(8.4 + k * 2.6, 8.0, 1.1, 8.2, 11.4, "paper", n=3)
    # the bucket and wringer docked at the east end
    bx0, bx1 = 23.4, 31.2
    for (x, z) in ((bx0 + 0.8, z0 + 0.8), (bx1 - 0.8, z0 + 0.8), (bx0 + 0.8, z1 - 0.8), (bx1 - 0.8, z1 - 0.8)):
        out += swap_yz(round_part(x, 0.6, 0.6, z - 0.25, z + 0.25, "black", n=3))
    out.append(box((bx0, 1.2, z0), (bx1, 7.4, z1), mat("bucket")))
    out.append(box((bx0 + 0.6, 7.4, z0 + 0.8), (bx1 - 0.6, 10.6, z1 - 0.8), mat("cart")))
    out.append(box((bx0 + 2.4, 10.6, z0 + 3.4), (bx0 + 3.2, 17.6, z0 + 5.4), mat("cart")))
    out.append(box((bx0 + 1.6, 17.6, z0 + 2.8), (bx0 + 4.0, 18.4, z0 + 6.0), mat("black")))
    return out


SHELF_LOAD = {
    0: [("case", "TORK", "JUMBO TISSUE", "12 ROLLS", (40, 90, 160)), ("rolls",), ("towels",), ("case", "TORK", "HAND TOWEL", "6 ROLLS", (40, 90, 160))],
    1: [("case", "GOJO", "FOAM SOAP", "2 x 1200 ML", (200, 70, 50)), ("gloves",), ("liners",), ("case", "SANI", "SEAT COVERS", "20 PACKS", (60, 140, 90))],
}


def supply_shelf(v):
    """Two wire shelving units (0.9 m wide, 0.46 m deep, 1.83 m high each; two blocks wide and high): chrome posts, five
    wire decks; on them the restrooms' paper, soap and liners, cartons labelled."""
    out = []
    ys = (1.2, 8.6, 16.0, 23.4, 29.0)
    z0, z1 = 8.4, 15.8
    for u, ux in enumerate((0.8, 16.8)):
        ux1 = ux + 14.4
        for x in (ux, ux1 - 0.6):
            for z in (z0, z1 - 0.6):
                out.append(box((x, 0, z), (x + 0.6, 29.4, z + 0.6), mat("chrome")))
        for y in ys:
            out.append(box((ux + 0.6, y, z0 + 0.1), (ux1 - 0.6, y + 0.3, z1 - 0.1), mat("wire")))
        load = SHELF_LOAD[(v + u) % 2]
        for lvl, item in zip(ys, load):
            y = lvl + 0.3
            if item[0] == "case":
                _, brand, l1, l2, col = item
                key = f"wr_case_{brand}_{l1}".replace(" ", "_")
                out.append(box((ux + 1.0, y, z0 + 0.6), (ux + 7.2, y + 4.8, z1 - 0.8), faces("cardboard", north=PRT.face(key, 6.2, 4.8, case_print(brand, l1, l2, col)))))
                out.append(box((ux + 7.6, y, z0 + 1.4), (ux1 - 1.0, y + 4.0, z1 - 0.8), faces("cardboard", north=PRT.face(key + "_b", 5.8, 4.0, case_print(brand, l1, l2, col)))))
            elif item[0] == "rolls":
                for k in range(5):
                    for j in range(2):
                        out += round_part(ux + 1.8 + k * 2.6, z0 + 2.2 + j * 3.2, 1.1, y, y + 2.0, "paper", n=3)
                for k in range(4):
                    out += round_part(ux + 3.1 + k * 2.6, z0 + 3.8, 1.1, y + 2.0, y + 4.0, "paper", n=3)
            elif item[0] == "towels":
                for k in range(4):
                    out += round_part(ux + 2.0 + k * 3.4, z0 + 3.8, 1.5, y, y + 5.0, "paper", n=4)
            elif item[0] == "gloves":
                for k, tex in enumerate(("glove_blue", "glove_blue", "glove_purple", "glove_green")):
                    out.append(box((ux + 1.0 + k * 3.2, y, z0 + 1.0), (ux + 3.8 + k * 3.2, y + 1.6, z1 - 1.4), mat(tex)))
                    out.append(box((ux + 1.0 + k * 3.2, y + 1.6, z0 + 1.0), (ux + 3.8 + k * 3.2, y + 3.2, z1 - 1.4), mat(tex)))
            elif item[0] == "liners":
                for k in range(3):
                    out += swap_yz(round_part(ux + 2.6 + k * 4.4, y + 1.6, 1.6, z0 + 1.2, z1 - 1.2, "black", n=3))
    return out


def water_heater():
    """The restrooms' water heater (190 l, 0.57 m across, 1.5 m high, in its drain pan; two blocks high): the white
    jacket, its thermostat covers and rating label, the cold in and hot out up to the wall with their valves, the
    expansion tank on the cold, the relief valve and its discharge down to the pan, the drain valve, the seismic
    straps, the disconnect switch on the wall and its conduit to the top."""
    out = []
    cx, cz, r = 7.4, 9.6, 4.6
    out += ring(cx, cz, r + 0.9, 0.3, 0.0, 1.0, "steel", 0.2, "steel")
    out += round_part(cx, cz, r - 0.4, 0.2, 1.4, "black", n=4)
    out += round_part(cx, cz, r, 1.4, 23.4, "enamel", n=5)
    out += round_part(cx, cz, r - 0.3, 23.4, 24.0, "graphite", n=5)
    front = cz - r
    for y0, h, key, draw in ((5.0, 2.6, "wr_cover0", cover_print), (15.0, 2.6, "wr_cover1", cover_print)):
        out.append(box((cx - 1.8, y0, front - 0.2), (cx + 1.8, y0 + h, front + 0.6), faces("enamel", north=PRT.face(key, 3.6, h, draw))))
    out.append(box((cx - 2.4, 9.0, front - 0.1), (cx + 2.4, 13.4, front + 0.6), faces("enamel", north=PRT.face("wr_heater", 4.8, 4.4, heater_label))))
    out.append(box((cx - 0.4, 2.0, front - 0.8), (cx + 0.4, 2.8, front + 0.6), mat("brass")))
    out.append(box((cx - 0.6, 2.1, front - 1.2), (cx + 0.6, 2.7, front - 0.8), mat("black")))
    # the seismic straps round it and into the wall
    for y in (7.0, 19.0):
        out += ring(cx, cz, r + 0.1, 0.1, y, y + 0.6, "steel", 0.0, None)
        for sx in (cx - r + 0.8, cx + r - 0.8):
            out.append(box((sx - 0.3, y, cz + 2.6), (sx + 0.3, y + 0.6, 15.9), mat("steel")))
    # cold in (left as you face it) and hot out: up from the top, back into the wall at 1.9 m
    for x, tag, tex in ((cx + 2.2, "steel_blue", "copper"), (cx - 2.2, "red", "copper")):
        out += round_part(x, cz, 0.45, 24.0, 30.0, tex, n=3)
        out.append(box((x - 0.45, 29.4, cz), (x + 0.45, 30.3, 15.9), mat(tex)))
        out += round_part(x, cz, 0.7, 26.2, 27.4, "brass", n=3)
        out.append(box((x - 0.15, 27.4, cz - 2.2), (x + 0.15, 27.8, cz), mat(tag)))
    # the expansion tank on the cold line
    ex = cx + 2.2 + 3.2
    out.append(box((cx + 2.65, 28.0, cz - 0.3), (ex, 28.6, cz + 0.3), mat("copper")))
    out += round_part(ex, cz, 1.5, 23.0, 28.6, "exp", n=4)
    out += round_part(ex, cz, 0.5, 22.4, 23.0, "brass", n=3)
    # the relief valve on the side near the top, its discharge down to the pan
    vx = cx - r - 0.6
    out.append(box((vx - 0.2, 20.6, cz - 0.5), (cx - r + 0.4, 21.6, cz + 0.5), mat("brass")))
    out.append(box((vx - 0.8, 21.6, cz - 0.3), (vx + 0.2, 22.2, cz + 0.3), mat("brass")))
    out += round_part(vx - 0.3, cz, 0.45, 2.2, 20.6, "copper", n=3)
    # the disconnect on the wall and its conduit to the top
    out.append(box((12.2, 20.0, 14.2), (15.4, 24.6, 15.9), faces("graphite", north=PRT.face("wr_disc", 3.2, 4.6, label("DISCONNECT", ("WATER HEATER", "240 V 30 A"), (180, 40, 30))))))
    out.append(box((13.4, 21.4, 13.8), (14.2, 23.2, 14.2), mat("black")))
    out += cable([(cx, 24.0, cz + 1.6), (cx, 25.2, cz + 1.6), (cx, 25.2, 13.0), (13.8, 25.2, 13.0), (13.8, 24.6, 13.0)], 0.3, "steel")
    return out


def wet_floor_sign():
    """The yellow A-frame (0.3 m wide, 0.65 m high), open: its two leaves leaning together at the handle, printed both
    sides."""
    uv = PRT.face("wr_wet", 5.0, 10.0, wet_print)
    out = []
    for ang, z in ((22.5, 8.2), (-22.5, 7.8)):
        out.append(box((5.5, 0.0, z - 0.2), (10.5, 10.0, z + 0.2), {**mat("bucket"), "north": uv, "south": uv},
                       {"angle": ang, "axis": "x", "origin": [8.0, 10.0, 8.0]}))
    out.append(box((6.4, 9.6, 7.5), (9.6, 11.0, 8.5), mat("bucket")))
    return out


def floor_scrubber():
    """The walk-behind scrubber parked on charge (0.6 m wide, 1.1 m long, 1 m to its handle; built front to the north):
    the brush deck, the yellow solution tank under the grey recovery tank, the squeegee at the back, the two wheels,
    the handle and the dashboard (live in game: the battery's bars as it charges), its cord to the wall."""
    out = []
    out.append(box((2.4, 1.4, 1.0), (13.6, 2.8, 6.0), mat("scrub_grey")))
    out += round_part(8.0, 3.6, 2.4, 0.4, 1.4, "black", n=4)
    out.append(box((3.0, 2.0, 2.0), (13.0, 9.0, 13.4), mat("scrub")))
    out.append(box((2.8, 9.0, 1.8), (13.2, 11.6, 13.6), mat("scrub_grey")))
    out.append(box((4.0, 11.6, 3.0), (12.0, 12.0, 11.0), mat("scrub_grey")))
    for x in (2.2, 13.8):
        out += wheel_x(x, 2.0, 12.0, 2.0, 1.2, "black")
    out.append(box((1.6, 0.3, 14.4), (14.4, 1.6, 15.0), mat("black")))
    out.append(box((2.0, 1.6, 14.2), (14.0, 2.4, 15.2), mat("scrub_grey")))
    for x in (3.8, 12.2):
        out.append(box((x - 0.4, 9.0, 13.6), (x + 0.4, 15.2, 14.4), mat("scrub_grey")))
    out.append(box((3.0, 15.2, 13.4), (13.0, 16.0, 14.6), mat("grip_black")))
    out.append(box((5.2, 13.0, 12.6), (10.8, 15.0, 13.4), faces("scrub_grey", north=SCR.face("wr_scrub_panel", 5.6, 2.0, scrub_panel))))
    out += cable([(12.0, 10.0, 13.6), (12.0, 10.0, 15.9)], 0.25, "black")
    return out


def bottle_filler():
    """The bi-level drinking fountain with the bottle filler over it (two blocks high, on the wall): the low basin
    (for a wheelchair) and the high one, each with its bubbler and front push bar, the chiller's cabinet under the high
    one; over them the filler's recess with its spout and sensor, the counter (live in game) and the filter light."""
    out = []
    for x0, x1, y in ((0.6, 7.6, 12.4), (8.4, 15.4, 15.6)):
        out.append(box((x0, y - 1.6, 7.8), (x1, y, 15.9), mat("stainless")))
        out.append(box((x0 + 0.6, y, 8.4), (x1 - 0.6, y + 0.6, 8.9), mat("stainless")))
        out.append(box((x0 + 0.6, y, 15.0), (x1 - 0.6, y + 0.6, 15.5), mat("stainless")))
        for xa in (x0 + 0.1, x1 - 0.6):
            out.append(box((xa, y, 8.4), (xa + 0.5, y + 0.6, 15.5), mat("stainless")))
        cx = (x0 + x1) / 2
        out += round_part(cx, 13.4, 0.4, y, y + 1.0, "chrome", n=3)
        out.append(box((cx - 0.3, y + 0.8, 12.4), (cx + 0.3, y + 1.1, 13.6), mat("chrome")))
        out.append(box((x0 + 1.0, y - 1.4, 7.4), (x1 - 1.0, y - 0.6, 7.8), mat("black")))
        out += round_part(cx, 11.6, 0.6, y, y + 0.12, "chrome", n=3)
    out.append(box((8.4, 0.6, 9.4), (15.4, 14.0, 15.9), mat("stainless")))
    out.append(box((9.4, 2.0, 9.3), (14.4, 4.6, 9.4), mat("grille")))
    # the filler: its case, the recess, the spout, the sensor, the counter and the filter light
    fx0, fx1, fy0, fy1 = 3.6, 12.4, 18.2, 30.4
    out.append(box((fx0, fy0, 11.6), (fx1, fy0 + 1.4, 15.9), mat("stainless")))
    out.append(box((fx0, fy1 - 4.2, 11.6), (fx1, fy1, 15.9), mat("stainless")))
    for xa, xb in ((fx0, fx0 + 1.0), (fx1 - 1.0, fx1)):
        out.append(box((xa, fy0 + 1.4, 11.6), (xb, fy1 - 4.2, 15.9), mat("stainless")))
    out.append(box((fx0 + 1.0, fy0 + 1.4, 14.6), (fx1 - 1.0, fy1 - 4.2, 15.9), mat("graphite")))
    out.append(box((fx0 + 1.0, fy0 + 1.4, 12.0), (fx1 - 1.0, fy0 + 1.6, 14.6), mat("grille")))
    out += round_part(8.0, 13.0, 0.5, fy1 - 4.8, fy1 - 4.2, "chrome", n=3)
    out.append(box((9.6, fy1 - 4.5, 14.4), (10.4, fy1 - 4.2, 14.6), mat("black")))
    out.append(box((fx0 + 1.2, fy1 - 3.2, 11.5), (fx1 - 1.2, fy1 - 0.8, 11.6), {**mat("black"), "north": SCR.face("wr_filler", 6.4, 2.4, filler_screen(12486))}))
    out += [{**e, "shade": False} for e in [box((fx1 - 1.0, fy1 - 3.9, 11.5), (fx1 - 0.6, fy1 - 3.5, 11.6), mat("led_green"))]]
    return out


# ------------------------------------------------------------------ write
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, PRT
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **OK.MATERIALS, **CK.MATERIALS, **MATERIALS}
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(512, TX, "atlas")
    SCR = PK.ShelfAtlas(512, 3 * TX, "screens")
    PRT = PK.ShelfAtlas(1024, 2 * TX, "prints")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK, OK):
        m.A, m.SCR = A, SCR
    CK.A, CK.SCR, CK.PRT = A, SCR, PRT
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("wr_atlas"), "screens": ref("wr_screens"), "prints": ref("wr_prints"), "particle": ref("wr_quartz")})

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

    def wide(name, els, n, extra=""):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}{extra.replace(',', '_').replace('=', '')}_{p}", part, True)
            v.update(faced(f"{name}{extra.replace(',', '_').replace('=', '')}_{p}", f"{extra},part={p}"))
        return v

    def tall(name, els, extra=""):
        v = {}
        tag = extra.replace(',', '_').replace('=', '')
        for h, half in zip(("lower", "upper"), cut_y(prep(els), 2)):
            model(f"{name}{tag}_{h}", half, True)
            v.update(faced(f"{name}{tag}_{h}", f"{extra},half={h}"))
        return v

    states("vanity_counter", wide("vanity_counter", vanity_counter(), 3))
    states("vanity_mirror", wide("vanity_mirror", vanity_mirror(), 3))
    model("sanitary_bin", sanitary_bin())
    states("sanitary_bin", faced("sanitary_bin"))
    for name, fn in (("mop_sink", mop_sink), ("mop_rack", mop_rack), ("chemical_dispenser", chemical_dispenser),
                     ("water_heater", water_heater), ("bottle_filler", bottle_filler)):
        states(name, tall(name, fn()))
    states("janitor_cart", wide("janitor_cart", janitor_cart(), 2))
    v = {}
    for var in (0, 1):
        for h, half in zip(("lower", "upper"), cut_y(prep(supply_shelf(var)), 2)):
            for p, part in enumerate(cut_parts(half, 2)):
                model(f"supply_shelf_{var}_{h}_{p}", part, True)
                v.update(faced(f"supply_shelf_{var}_{h}_{p}", f",half={h},part={p},variant={var}"))
    states("supply_shelf", v)
    for name, fn in (("wet_floor_sign", wet_floor_sign), ("floor_scrubber", floor_scrubber)):
        model(name, fn())
        states(name, faced(name))
    A.img.save(tex_dir / "wr_atlas.png")
    SCR.img.save(tex_dir / "wr_screens.png")
    PRT.img.save(tex_dir / "wr_prints.png")
    return [(a.y + a.row, a.size) for a in (A, SCR, PRT)]


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
