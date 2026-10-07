"""Round 6 of the plant kit: things you look at up close.

- Books and binders as real objects: every book its own box on the shelf with its spine (title and all) drawn on
  a shared spine sheet (pi_spines, 256 px, kept sharp so the titles read). Nuclear plant reading: reactor physics,
  tech specs, procedures, logs.
- Paperwork you can read: procedures, a radiation work permit, a shift log, a surveillance data sheet, a tech spec
  page, a P&ID (pi_doc_*, 128 px, kept sharp).
- A mug that is a mug (hollow, coffee in it; desks with one give off steam) and a pen cup with pens and pencils.
- The floor-standing office copier, built after the big A3 office machines (Samsung MultiXpress style): paper
  drawers, the engine, the output bay, the scanner, the document feeder, the tilting touch screen.
- Tall (two-block) filing cabinets that join up, and a drawer pulled out just a few pixels showing a couple of
  files.
"""
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFont

S = 128
ATLAS = 1024         # the spine sheet (48 px a model pixel: the titles read)
DOCS = 12           # the kinds of paperwork (pi_doc_0 ... )
SHARP = set()        # textures kept at their full size (no stylizing): text must read


def _font(size):
    return ImageFont.load_default(size=size)


# --------------------------------------------------------------------------------------------- the spine sheet
BOOKS = [  # title, colour, width, height (model pixels)
    ("REACTOR PHYSICS", (120, 30, 34), 1.2, 6.4), ("PWR SYSTEMS MANUAL", (30, 50, 100), 1.4, 6.6),
    ("HEALTH PHYSICS", (40, 90, 60), 1.0, 5.8), ("THERMAL HYDRAULICS", (20, 20, 24), 1.3, 6.2),
    ("RADIATION PROTECTION", (200, 160, 40), 1.1, 6.0), ("NUCLEAR SAFETY ANALYSIS", (90, 30, 90), 1.5, 6.6),
    ("PLANT CHEMISTRY", (30, 110, 140), 0.9, 5.4), ("STEAM TURBINES", (110, 70, 40), 1.2, 6.3),
    ("ELECTRICAL SYSTEMS", (60, 60, 64), 1.0, 5.9), ("INSTRUMENTATION & CONTROL", (150, 40, 40), 1.4, 6.5),
    ("FUEL HANDLING", (40, 70, 130), 0.9, 5.6),
]
BINDERS = [  # label code, title, colour, width, height
    ("EOP", "EMERGENCY OPERATING PROCEDURES", (200, 40, 40), 2.3, 6.6),
    ("TS", "TECHNICAL SPECIFICATIONS", (30, 60, 140), 2.4, 6.6),
    ("OP-1", "NORMAL OPERATING PROCEDURES", (30, 30, 34), 2.2, 6.5),
    ("SURV", "SURVEILLANCE TESTS 2026", (40, 120, 70), 2.2, 6.5),
    ("RP", "RADIATION WORK PERMITS", (220, 180, 40), 2.1, 6.4),
]
DESK_BINDERS = [  # label code, colour (narrow binders standing on desks)
    ("AOP", (200, 40, 40)), ("PM", (30, 60, 140)), ("LOG", (30, 30, 34)), ("CHEM", (40, 120, 70)),
    ("QA", (220, 180, 40)), ("TRN", (120, 120, 126)),
]
DESK_BINDER_W, DESK_BINDER_H = 1.1, 5.6


def _shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


K = 48  # spine sheet pixels per model pixel


def _rotated_text(img, text, size, colour, box, bold=False):
    """Text running up a spine (bottom to top), fitted into box (x0, y0, x1, y1): shrunk to fit its length."""
    x0, y0, x1, y1 = box
    f = _font(size)
    probe = ImageDraw.Draw(img)
    while size > 6 and (probe.textlength(text, font=f) > (y1 - y0) or size > (x1 - x0) - 2):
        size -= 1
        f = _font(size)
    tw = int(probe.textlength(text, font=f)) + 4
    t = Image.new("RGBA", (tw, size + 6), (0, 0, 0, 0))
    td = ImageDraw.Draw(t)
    td.text((2, 1), text, font=f, fill=colour)
    if bold:
        td.text((3, 1), text, font=f, fill=colour)
    t = t.rotate(90, expand=True)
    img.alpha_composite(t, (x0 + (x1 - x0 - t.size[0]) // 2, y0 + (y1 - y0 - t.size[1]) // 2))


def _book_spine(img, x0, y0, w, h, title, colour, r):
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=colour + (255,))
    d.rectangle([x0, y0, x0 + 2, y0 + h - 1], fill=_shade(colour, 0.7) + (255,))
    d.rectangle([x0 + w - 3, y0, x0 + w - 1, y0 + h - 1], fill=_shade(colour, 0.7) + (255,))
    gold = (226, 196, 120, 255) if sum(colour) < 400 else (30, 30, 34, 255)
    for yy in (y0 + 15, y0 + 21, y0 + h - 51, y0 + h - 45):
        d.line([(x0 + 7, yy), (x0 + w - 8, yy)], fill=gold, width=3)
    _rotated_text(img, title, 30, gold, (x0 + 6, y0 + 33, x0 + w - 6, y0 + h - 60), bold=True)
    d.rectangle([x0 + 9, y0 + h - 36, x0 + w - 10, y0 + h - 18], fill=_shade(colour, 0.55) + (255,))


def _binder_spine(img, x0, y0, w, h, code, title, colour):
    """A lever-arch binder spine: the clear label pocket down most of it, the code in big letters across the top
    of the label (or up it on a thin binder), the title running up the label, the finger hole at the bottom."""
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=colour + (255,))
    d.rectangle([x0, y0, x0 + w - 1, y0 + 6], fill=_shade(colour, 0.75) + (255,))
    d.rectangle([x0, y0 + h - 7, x0 + w - 1, y0 + h - 1], fill=_shade(colour, 0.75) + (255,))
    d.rectangle([x0, y0, x0 + 2, y0 + h - 1], fill=_shade(colour, 0.8) + (255,))
    lx0, lx1 = x0 + 8, x0 + w - 9
    ly0, ly1 = y0 + 20, y0 + h - 100
    d.rectangle([lx0, ly0, lx1, ly1], fill=(248, 248, 242, 255), outline=_shade(colour, 0.55) + (255,), width=2)
    ink = (20, 20, 24, 255)
    if lx1 - lx0 > 70:
        # Wide: the code across the top in big letters, a rule, then the title word by word, one word a line,
        # the size chosen so the longest word fits the pocket and all the lines fit under the code.
        size = 40
        f = _font(size)
        while d.textlength(code, font=f) > lx1 - lx0 - 10:
            size -= 1
            f = _font(size)
        d.text((lx0 + (lx1 - lx0 - d.textlength(code, font=f)) / 2, ly0 + 8), code, font=f, fill=ink)
        d.line([(lx0 + 8, ly0 + 60), (lx1 - 8, ly0 + 60)], fill=_shade(colour, 0.7) + (255,), width=4)
        if title:
            words = title.split()
            room_w, room_h = lx1 - lx0 - 10, ly1 - ly0 - 78
            size = 24
            while size > 7:
                f = _font(size)
                if max(d.textlength(wd, font=f) for wd in words) <= room_w and len(words) * (size + 6) <= room_h:
                    break
                size -= 1
            f = _font(size)
            for k, wd in enumerate(words):
                d.text((lx0 + (lx1 - lx0 - d.textlength(wd, font=f)) / 2, ly0 + 74 + k * (size + 6)), wd, font=f, fill=(30, 30, 36, 255))
    else:
        # Thin: everything runs up the spine.
        text = code if not title else code + "  " + title
        _rotated_text(img, text, 34, ink, (lx0 + 2, ly0 + 8, lx1 - 2, ly1 - 8), bold=True)
    # The steel ring of the finger hole.
    cx, cy = x0 + w // 2, y0 + h - 60
    rr = min(15, w // 2 - 9)
    d.ellipse([cx - rr - 2, cy - rr - 2, cx + rr + 2, cy + rr + 2], fill=(190, 192, 196, 255))
    d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(14, 14, 16, 255))


def spine_sheet():
    """The spine sheet and where each spine is on it (uv in model pixels: u0, v0, u1, v1)."""
    img = Image.new("RGBA", (ATLAS, ATLAS), (60, 50, 40, 255))
    uv = {}
    r = random.Random(1201)

    def place(key, w, h, draw):
        nonlocal x, y, row_h
        pw, ph = int(w * K), int(h * K)
        if x + pw > ATLAS:
            x, y, row_h = 0, y + row_h, 0
        draw(x, y, pw, ph)
        uv[key] = [x / K * 16 / (ATLAS / K) * (ATLAS / K) / 16, y / K, (x + pw) / K, (y + ph) / K]
        uv[key] = [v * 16 / (ATLAS / K) for v in (x / K, y / K, (x + pw) / K, (y + ph) / K)]
        x += pw
        row_h = max(row_h, ph)

    x = y = row_h = 0
    for title, colour, w, h in BOOKS:
        place(title, w, h, lambda X, Y, W, H, t=title, c=colour: _book_spine(img, X, Y, W, H, t, c, r))
    x, y, row_h = 0, y + row_h, 0
    for code, title, colour, w, h in BINDERS:
        place(code, w, h, lambda X, Y, W, H, c=code, t=title, col=colour: _binder_spine(img, X, Y, W, H, c, t, col))
    x, y, row_h = 0, y + row_h, 0
    for code, colour in DESK_BINDERS:
        place("desk:" + code, DESK_BINDER_W, DESK_BINDER_H,
              lambda X, Y, W, H, c=code, col=colour: _binder_spine(img, X, Y, W, H, c, None, col))
    return img, uv


_SHEET, SPINE_UV = spine_sheet()


def _side_uv(uv):
    """A thin strip of the spine's middle colour: the covers seen from the side."""
    u0, v0, u1, v1 = uv
    return [u0 + 0.12, v0 + 0.06, u0 + 0.16, v0 + 0.1]


GAP = 0.07   # a hair between neighbours, so each book or binder reads as its own from above


def book(x, y0, z_front, depth, title, w, h):
    uv = SPINE_UV[title]
    side = ("pi_spines", _side_uv(uv))
    return (x + GAP, y0, z_front - depth, x + w - GAP, y0 + h, z_front,
            {"*": side, "south": ("pi_spines", uv), "up": "pi_page_edge", "down": "pi_page_edge", "north": side})


def binder(x, y0, z_front, depth, key, w, h):
    """A lever-arch binder: from above you see its coloured covers either side of the paper inside (a thin strip of
    pages on top between the covers)."""
    uv = SPINE_UV[key]
    side = ("pi_spines", _side_uv(uv))
    return [(x + GAP, y0, z_front - depth, x + w - GAP, y0 + h, z_front,
             {"*": side, "south": ("pi_spines", uv), "up": side, "down": side, "north": side}),
            (x + GAP + 0.18, y0 + h - 0.6, z_front - depth + 0.25, x + w - GAP - 0.18, y0 + h + 0.03, z_front - 0.35,
             {"*": "pi_page_edge", "south": side, "north": side})]


# The bottom shelf's floor, part of the bookcase body (so it is there in the upper block of a stack too).
SHELF_FLOOR = (2, 0, 2, 14, 0.6, 15, "pi_veneer")


def shelf_books(x0, x1, y0, z_front):
    """The upper shelf: the books side by side, front edges not quite in line, one leaning (a gap at the end)."""
    out, x = [], x0
    r = random.Random(1202)
    for title, colour, w, h in BOOKS:
        if x + w > x1 - 0.6:
            break
        out.append(book(x, y0, z_front - r.uniform(0, 0.6), r.uniform(8.5, 10), title, w, h))
        x += w
    return out


def shelf_binders(x0, x1, y0, z_front):
    out, x = [], x0
    for code, title, colour, w, h in BINDERS:
        if x + w > x1:
            break
        out += binder(x, y0, z_front, 10, code, w, h)
        x += w
    return out


# --------------------------------------------------------------------------------------------- desk things

def mug(x, z, y0, steam=False):
    """A mug: four walls, a bottom, coffee standing in it, a handle."""
    w, h, t = 2.4, 2.8, 0.3
    return [(x, y0, z, x + w, y0 + 0.3, z + w, "pi_mug"),
            (x, y0, z, x + t, y0 + h, z + w, "pi_mug"), (x + w - t, y0, z, x + w, y0 + h, z + w, "pi_mug"),
            (x + t, y0, z, x + w - t, y0 + h, z + t, "pi_mug"), (x + t, y0, z + w - t, x + w - t, y0 + h, z + w, "pi_mug"),
            (x + t + 0.02, y0 + 0.3, z + t + 0.02, x + w - t - 0.02, y0 + h - 0.5, z + w - t - 0.02, {"*": "pi_coffee", "up": "pi_coffee"}),
            (x + w, y0 + 0.7, z + 0.9, x + w + 0.9, y0 + 0.95, z + 1.5, "pi_mug"),
            (x + w + 0.65, y0 + 0.7, z + 0.9, x + w + 0.9, y0 + 2.2, z + 1.5, "pi_mug"),
            (x + w, y0 + 1.95, z + 0.9, x + w + 0.9, y0 + 2.2, z + 1.5, "pi_mug")]


MUG_CENTRE = 1.2  # from the mug's corner to its middle


def pen_cup(x, z, y0):
    """A mesh pen cup (hollow) with two pens, a pencil and a highlighter standing in it, leaning."""
    w, h, t = 2.0, 2.6, 0.25
    out = [(x, y0, z, x + w, y0 + 0.25, z + w, "pi_pen_cup"),
           (x, y0, z, x + t, y0 + h, z + w, "pi_pen_cup"), (x + w - t, y0, z, x + w, y0 + h, z + w, "pi_pen_cup"),
           (x + t, y0, z, x + w - t, y0 + h, z + t, "pi_pen_cup"), (x + t, y0, z + w - t, x + w - t, y0 + h, z + w, "pi_pen_cup")]
    cx, cz = x + w / 2, z + w / 2
    for dx, dz, tex, length, axis, angle in ((-0.35, -0.3, "pi_pen_blue", 4.4, "z", 22.5), (0.3, 0.2, "pi_pencil", 4.8, "x", -22.5),
                                             (0.2, -0.35, "pi_pen_black", 4.2, "x", 22.5), (-0.3, 0.35, "pi_highlighter", 3.6, "z", -22.5)):
        px, pz = cx + dx, cz + dz
        thick = 0.35 if tex != "pi_highlighter" else 0.55
        out.append((px - thick / 2, y0 + 0.25, pz - thick / 2, px + thick / 2, y0 + 0.25 + length, pz + thick / 2, tex,
                    {"rotation": {"angle": angle, "axis": axis, "origin": [px, y0 + 0.25, pz]}}))
    return out


# --------------------------------------------------------------------------------------------- the copier

# Drawn as one machine over 2 x 2 blocks (32 x 32 model pixels; it rises 7 into the upper row). The front faces
# show the front elevation (pi_copier_f_<part>) and the tops the plan (pi_copier_t_<column>), both drawn whole and
# cut into blocks, so the drawers and panels run across the block line.
F, T = "pi_copier_f", "pi_copier_t"
COPIER = [
    (1.6, 0, 2.6, 30.4, 1, 14.4, "pi_toe_kick"),
    # The main unit: four paper drawers and the engine.
    (1.2, 1, 2, 21, 17, 15, {"*": "pi_copier_dark", "south": F}),
    # The output bay: the back wall and the right post, copies lying in it.
    (1.2, 17, 2, 21, 19, 7.5, {"*": "pi_copier_dark", "south": F}),
    (17.5, 17, 7.5, 21, 19, 15, {"*": "pi_copier_light", "south": F}),
    (2.2, 17, 8, 13.5, 17.5, 14, {"*": "pi_paper", "up": T}),
    # The scanner with the document feeder on it, an original waiting in the feeder.
    (1.2, 19, 2, 21, 20.6, 15, {"*": "pi_copier_light", "south": F, "up": T}),
    (1.6, 20.6, 2.4, 17, 22.4, 13, {"*": "pi_copier_dark", "south": F, "up": T}),
    (3, 22.4, 4.5, 14.5, 22.7, 11.5, {"*": "pi_paper", "up": T}),
    # The control panel along the front of the scanner, its touch screen on top.
    (12.5, 19.4, 13.2, 20.8, 20.6, 16, {"*": "pi_copier_dark", "south": F, "up": T}),
    # The finisher on the right: its front, the two output trays sticking out of its side with copies on them.
    (21.2, 1, 3, 30.8, 17.6, 14.6, {"*": "pi_copier_light", "south": F, "up": T}),
    (30.8, 9, 4, 32, 9.6, 13.6, "pi_copier_light"), (30.8, 13, 4, 32, 13.6, 13.6, "pi_copier_light"),
    (30.8, 9.6, 4.6, 31.8, 9.9, 13, "pi_paper"), (30.8, 13.6, 4.6, 31.8, 13.9, 13, "pi_paper"),
]


def copier_part(c, r):
    """Block (c, r)'s share of the copier, its front and top textures named for that block."""
    out = []
    for b in COPIER:
        x0, x1 = max(b[0], c * 16), min(b[3], c * 16 + 16)
        y0, y1 = max(b[1], r * 16), min(b[4], r * 16 + 16)
        if x1 - x0 < 0.01 or y1 - y0 < 0.01:
            continue
        t = b[6]
        if isinstance(t, dict):
            t = {k: (f"{F}_{r * 2 + c}" if v == F else f"{T}_{c}" if v == T else v) for k, v in t.items()}
        out.append((x0 - c * 16, y0 - r * 16, b[2], x1 - c * 16, y1 - r * 16, b[5], t))
    return out


# --------------------------------------------------------------------------------------------- filing cabinets

TALL_CABINET = {"display": "Tall Filing Cabinet", "stacks": True, "group": "tall_filing", "boxes": [
    (2, 0, 2, 14, 16, 15, {"*": "pi_filing_side", "south": "pi_filing_two!"})],
    "cap": [(1.8, 15.6, 1.8, 14.2, 16.4, 15.2, "pi_filing_side")],
    "plinth": [(2.3, 0, 14.9, 13.7, 0.8, 15.1, "pi_toe_kick")]}

# The same with its top drawer pulled out three pixels: two files stand at the front of it.
TALL_CABINET_OPEN = {"display": "Tall Filing Cabinet (drawer open)", "stacks": True, "group": "tall_filing", "boxes": [
    (2, 0, 2, 14, 8, 15, {"*": "pi_filing_side", "south": "pi_filing_one!"}),
    (2, 8, 2, 2.4, 16, 15, "pi_filing_side"), (13.6, 8, 2, 14, 16, 15, "pi_filing_side"),
    (2.4, 15.6, 2, 13.6, 16, 15, "pi_filing_side"), (2.4, 8, 2, 13.6, 15.6, 2.4, "pi_cabinet_dark"),
    # The drawer: its front 3 px proud, its sides and bottom running back into the cabinet, files standing in it.
    (2.5, 8.3, 17.4, 13.5, 15.5, 18, {"*": "pi_filing_side", "south": "pi_filing_one!"}),
    (2.5, 8.3, 4, 2.9, 14.8, 17.4, "pi_filing_side"), (13.1, 8.3, 4, 13.5, 14.8, 17.4, "pi_filing_side"),
    (2.9, 8.3, 4, 13.1, 8.7, 17.4, "pi_cabinet_dark"),
    (3.1, 8.7, 16.4, 12.9, 14.4, 16.7, {"*": "pi_folder_manila", "up": "pi_folder_tab"}),
    (3.1, 8.7, 15.5, 12.9, 14.0, 15.8, {"*": "pi_folder_blue", "up": "pi_folder_tab"}),
    (3.1, 8.7, 14.6, 12.9, 13.8, 14.9, {"*": "pi_folder_manila", "up": "pi_folder_tab"})],
    "cap": TALL_CABINET["cap"], "plinth": TALL_CABINET["plinth"]}

# The low cabinet by a desk with its top drawer out a little.
LOW_CABINET_OPEN = [
    (2, 0, 2, 14, 9.6, 15, {"*": "pi_filing_side", "south": "pi_filing_low!"}),
    (2, 9.6, 2, 2.4, 15, 15, "pi_filing_side"), (13.6, 9.6, 2, 14, 15, 15, "pi_filing_side"),
    (1.8, 15, 1.8, 14.2, 15.4, 15.2, "pi_filing_side"), (2.4, 9.6, 2, 13.6, 15, 2.4, "pi_cabinet_dark"),
    (2.5, 9.9, 17.4, 13.5, 14.7, 18, {"*": "pi_filing_side", "south": "pi_drawer_front!"}),
    (2.5, 9.9, 4, 2.9, 14.2, 17.4, "pi_filing_side"), (13.1, 9.9, 4, 13.5, 14.2, 17.4, "pi_filing_side"),
    (2.9, 9.9, 4, 13.1, 10.3, 17.4, "pi_cabinet_dark"),
    (3.1, 10.3, 16.4, 12.9, 14.0, 16.7, {"*": "pi_folder_manila", "up": "pi_folder_tab"}),
    (3.1, 10.3, 15.5, 12.9, 13.7, 15.8, {"*": "pi_folder_blue", "up": "pi_folder_tab"})]


# --------------------------------------------------------------------------------------------- textures

def _img(c, size=S):
    a = np.zeros((size, size, 4), np.uint8)
    a[..., :3] = c
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _doc(kind):
    """A page of plant paperwork at 256 px, readable up close: a controlled-document header with the plant's
    trefoil logo, the content, a red stamp, a signature, a barcode."""
    N = 256
    img = _img((248, 246, 238), N)
    d = ImageDraw.Draw(img)
    r = random.Random(1300 + kind)
    ink, grey, blue = (20, 20, 24, 255), (100, 100, 108, 255), (24, 60, 150, 255)
    title_f, head_f, body_f, small_f = _font(26), _font(17), _font(13), _font(11)

    def logo(cx, cy, rr, bg=(250, 210, 40, 255), fg=(30, 30, 34, 255)):
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=bg)
        for a_ in (90, 210, 330):
            d.pieslice([cx - rr + 2, cy - rr + 2, cx + rr - 2, cy + rr - 2], a_ - 30, a_ + 30, fill=fg)
        d.ellipse([cx - rr // 4, cy - rr // 4, cx + rr // 4, cy + rr // 4], fill=fg)

    def header(band, text, fg=(255, 255, 255, 255)):
        d.rectangle([0, 0, N, 34], fill=band)
        logo(18, 17, 12, bg=(255, 255, 255, 255), fg=band)
        size = 17
        f = _font(size)
        while d.textlength(text, font=f) > N - 44 and size > 9:
            size -= 1
            f = _font(size)
        d.text((36, 17 - size // 2 - 2), text, font=f, fill=fg)

    def lines(y0, y1, step=12, x0=14, x1=242):
        for y in range(y0, y1, step):
            d.line([(x0, y), (x0 + r.randint(int((x1 - x0) * 0.55), x1 - x0), y)], fill=(130, 130, 138, 255), width=2)

    def stamp(text, x, y, colour=(200, 30, 40, 255)):
        f = _font(16)
        w = int(d.textlength(text, font=f)) + 18
        t = Image.new("RGBA", (w, 36), (0, 0, 0, 0))
        td = ImageDraw.Draw(t)
        td.rectangle([1, 1, w - 2, 34], outline=colour, width=3)
        td.text((9, 8), text, font=f, fill=colour)
        t = t.rotate(r.uniform(-10, 10), expand=True)
        img.alpha_composite(t, (min(x, N - t.size[0] - 4), min(y, N - t.size[1] - 2)))

    def signature(x, y):
        pts = [(x + k * 6, y + r.randint(-6, 6)) for k in range(12)]
        d.line(pts, fill=blue, width=2)

    def barcode(x, y):
        for k in range(40):
            if r.random() < 0.6:
                d.rectangle([x + k * 2, y, x + k * 2, y + 16], fill=ink)

    if kind == 0:     # an operating procedure
        header((24, 56, 120, 255), "UNIT 1  OPERATING PROCEDURE")
        d.text((14, 44), "OP-1-0201", font=title_f, fill=ink)
        d.text((14, 76), "REACTOR STARTUP", font=head_f, fill=ink)
        d.text((14, 98), "Rev 14     Page 7 of 32", font=small_f, fill=grey)
        for k, step in enumerate(("5.3  Withdraw shutdown banks", "5.4  Verify RCS boron conc.", "5.5  Commence dilution",
                                  "5.6  Plot inverse count rate", "5.7  Declare criticality")):
            y = 120 + k * 20
            d.rectangle([14, y, 24, y + 10], outline=ink, width=2)
            if k < 3:
                d.line([(15, y + 5), (19, y + 10), (27, y - 3)], fill=blue, width=3)
            d.text((32, y - 2), step, font=body_f, fill=ink)
        stamp("CONTROLLED COPY", 118, 218)
        signature(14, 236)
    elif kind == 1:   # a radiation work permit
        header((250, 210, 40, 255), "RADIATION WORK PERMIT", fg=ink)
        logo(214, 70, 26, bg=(250, 210, 40, 255), fg=(150, 40, 150, 255))
        d.text((14, 44), "RWP 26-0417", font=title_f, fill=ink)
        for k, (a_, b_) in enumerate((("Area", "RCA - AUX BLDG 85'"), ("Dose rate", "15 mrem/h"), ("Stay time", "2.0 h"),
                                      ("Dosimetry", "TLD + EPD"), ("Dress", "Full PCs, hood"), ("Task", "Valve repack CV-210"))):
            y = 84 + k * 20
            d.text((14, y), a_, font=body_f, fill=grey)
            d.text((96, y), b_, font=body_f, fill=ink)
            d.line([(14, y + 17), (176, y + 17)], fill=(210, 210, 204, 255))
        stamp("HP APPROVED", 120, 212, (30, 120, 60, 255))
        signature(14, 226)
        barcode(14, 238)
    elif kind == 2:   # a shift turnover log
        header((60, 64, 72, 255), "MAIN CONTROL ROOM - SHIFT LOG")
        d.text((14, 42), "Night shift 23:00 - 07:00", font=body_f, fill=grey)
        for y in range(64, N - 6, 18):
            d.line([(8, y), (N - 8, y)], fill=(180, 190, 214, 255))
        d.line([(56, 60), (56, N - 8)], fill=(220, 120, 120, 255), width=2)
        for k, (t_, e_) in enumerate((("0015", "RX power 100%, steady"), ("0230", "Swap CCW pumps A->B"),
                                      ("0410", "Chem sample SG-B SAT"), ("0545", "Daily surveillances done"),
                                      ("0650", "Turnover to day shift"))):
            y = 68 + k * 18
            d.text((14, y), t_, font=body_f, fill=blue)
            d.text((62, y), e_, font=body_f, fill=blue)
        signature(150, 230)
    elif kind == 3:   # a surveillance data sheet
        header((30, 110, 70, 255), "SURVEILLANCE TEST DATA")
        d.text((14, 42), "ST-1-RCS-03  RCS leak rate", font=body_f, fill=ink)
        cols = (14, 74, 130, 186, 242)
        for k in range(6):
            y = 64 + k * 22
            d.rectangle([14, y, 242, y + 22], outline=(150, 150, 156, 255))
        for x in cols[1:-1]:
            d.line([(x, 64), (x, 196)], fill=(150, 150, 156, 255))
        for k, row in enumerate((("Time", "Tavg", "PZR %", "VCT %"), ("0800", "587.2", "52.1", "38.0"),
                                 ("0900", "587.1", "52.0", "37.6"), ("1000", "587.3", "52.1", "37.2"),
                                 ("1100", "587.2", "52.0", "36.9"))):
            for x, txt in zip(cols, row):
                d.text((x + 6, 68 + k * 22), txt, font=body_f, fill=ink if k == 0 else blue)
        d.text((14, 204), "Leak rate 0.08 gpm   RESULT:", font=body_f, fill=ink)
        stamp("SAT", 186, 196, (30, 130, 60, 255))
        signature(14, 232)
    elif kind == 4:   # a technical specification page
        header((90, 30, 40, 255), "TECHNICAL SPECIFICATIONS")
        d.text((14, 44), "3.4.1  RCS PRESSURE,", font=head_f, fill=ink)
        d.text((14, 66), "TEMPERATURE AND FLOW", font=head_f, fill=ink)
        d.text((14, 96), "LCO 3.4.1  Pressurizer pressure", font=body_f, fill=ink)
        d.text((14, 114), "shall be >= 2200 psig.", font=body_f, fill=ink)
        d.text((14, 138), "APPLICABILITY:  MODE 1", font=body_f, fill=ink)
        lines(166, 232)
        d.text((180, 240), "3.4-1", font=small_f, fill=grey)
    elif kind == 6:   # the water chemistry daily analysis sheet
        header((30, 110, 140, 255), "PRIMARY CHEMISTRY - DAILY")
        d.text((14, 42), "RCS sample point B   06:00", font=body_f, fill=grey)
        rows = (("Parameter", "Result", "Limit"), ("Boron", "1203 ppm", "-"), ("Lithium", "2.21 ppm", "2.0-2.5"),
                ("pH (300C)", "7.10", "6.9-7.4"), ("Chloride", "2.1 ppb", "< 50"), ("Diss. O2", "< 5 ppb", "< 100"),
                ("Diss. H2", "32 cc/kg", "25-50"), ("Gross act.", "1.8 uCi/g", "< 100"))
        for k, row in enumerate(rows):
            y = 62 + k * 19
            if k == 0:
                d.rectangle([12, y - 2, 244, y + 15], fill=(220, 236, 240, 255))
            for x, txt in zip((16, 104, 180), row):
                d.text((x, y), txt, font=body_f, fill=ink if k == 0 else blue if x == 104 else ink)
            d.line([(12, y + 16), (244, y + 16)], fill=(200, 204, 206, 255))
        stamp("WITHIN SPEC", 120, 218, (30, 130, 60, 255))
        signature(14, 236)
    elif kind == 7:   # a gamma spectroscopy result
        header((110, 40, 120, 255), "GAMMA SPECTROSCOPY")
        d.text((14, 42), "Sample RCS-B-0611   HPGe det. 2", font=body_f, fill=grey)
        d.line([(16, 160), (240, 160)], fill=ink, width=2)
        d.line([(16, 60), (16, 160)], fill=ink, width=2)
        pts = []
        for x in range(16, 241, 2):
            y = 156 - r.randint(0, 6) - (70 if 60 < x < 66 else 90 if 120 < x < 126 else 55 if 170 < x < 175 else 0)
            pts.append((x, y))
        d.line(pts, fill=(110, 40, 120, 255), width=2)
        for x, lbl in ((52, "I-131"), (110, "Cs-137"), (164, "Co-60")):
            d.text((x, 64), lbl, font=small_f, fill=(200, 40, 40, 255))
        d.text((14, 170), "Co-60   3.2E-3 uCi/g", font=body_f, fill=ink)
        d.text((14, 188), "Cs-137  4.1E-4 uCi/g", font=body_f, fill=ink)
        d.text((14, 206), "I-131   1.2E-4 uCi/g", font=body_f, fill=ink)
        signature(150, 236)
    elif kind == 8:   # the O&M budget variance report
        header((20, 70, 60, 255), "FY27 O&M BUDGET - VARIANCE")
        d.text((14, 42), "Period 9 (Sept)   $ thousands", font=body_f, fill=grey)
        rows = (("Dept", "Budget", "Actual", "Var"), ("Operations", "4,120", "4,052", "+68"),
                ("Maintenance", "6,880", "7,215", "-335"), ("Chemistry", "910", "874", "+36"),
                ("Rad Protection", "1,340", "1,362", "-22"), ("Engineering", "3,560", "3,401", "+159"),
                ("Training", "1,105", "1,098", "+7"), ("TOTAL", "17,915", "18,002", "-87"))
        cols = (14, 112, 160, 208)
        for k, row in enumerate(rows):
            y = 62 + k * 19
            if k in (0, len(rows) - 1):
                d.rectangle([12, y - 2, 244, y + 15], fill=(222, 236, 230, 255))
            for x, txt in zip(cols, row):
                neg = txt.startswith("-")
                d.text((x, y), txt, font=body_f, fill=(190, 30, 40, 255) if neg else ink)
            d.line([(12, y + 16), (244, y + 16)], fill=(200, 204, 206, 255))
        d.text((14, 220), "Maint. over: outage 1R27 scope", font=small_f, fill=grey)
        signature(170, 236)
    elif kind == 9:   # the quarterly plant performance summary, with a bar chart
        header((24, 56, 120, 255), "Q3 PLANT PERFORMANCE")
        for k, (a_, b_) in enumerate((("Capacity factor", "94.2 %"), ("Net generation", "2,486 GWh"),
                                      ("Forced loss rate", "0.8 %"), ("Cost / MWh", "$ 28.40"), ("Days w/o LTI", "412"))):
            y = 44 + k * 18
            d.text((14, y), a_, font=body_f, fill=ink)
            d.text((150, y), b_, font=body_f, fill=blue)
        d.line([(20, 236), (240, 236)], fill=ink, width=2)
        d.text((14, 136), "Monthly capacity factor", font=small_f, fill=grey)
        for k, (m, v) in enumerate((("Jul", 0.92), ("Aug", 0.96), ("Sep", 0.95), ("Q1", 0.88), ("Q2", 0.81))):
            x = 30 + k * 42
            top = 236 - int(v * 90)
            d.rectangle([x, top, x + 24, 235], fill=(40, 110, 190, 255) if k < 3 else (150, 170, 200, 255))
            d.text((x + 3, 240), m, font=small_f, fill=grey)
        d.line([(20, 236 - int(0.9 * 90)), (240, 236 - int(0.9 * 90))], fill=(200, 40, 40, 255), width=1)
        d.text((196, 236 - int(0.9 * 90) - 14), "goal", font=small_f, fill=(200, 40, 40, 255))
    elif kind == 10:  # the agenda for the meeting
        header((60, 64, 72, 255), "PLAN OF THE DAY MEETING")
        d.text((14, 42), "07:30   Conference room B", font=body_f, fill=grey)
        for k, item in enumerate(("1. Safety moment", "2. Plant status / LCOs", "3. Work week 40 schedule",
                                  "4. Outage 1R27 readiness", "5. Budget & staffing", "6. Open action items")):
            y = 66 + k * 22
            d.text((14, y), item, font=head_f if k == 0 else body_f, fill=ink)
            d.text((214, y), f"{5 + k * 5}m", font=small_f, fill=grey)
        d.text((14, 204), "Notes:", font=body_f, fill=ink)
        lines(222, 250, 10)
    elif kind == 11:  # the capital projects schedule
        header((110, 70, 20, 255), "CAPITAL PROJECTS 2027")
        d.text((14, 42), "Project            Cost $M", font=body_f, fill=grey)
        for k, (name, cost, a_, b_) in enumerate((("Turbine retrofit", "48.0", 10, 120), ("SG inspection", "6.5", 60, 100),
                                                  ("Cooling tower fill", "12.2", 90, 200), ("Digital I&C", "31.7", 30, 230),
                                                  ("Dry cask pad", "9.4", 150, 240))):
            y = 64 + k * 30
            d.text((14, y), name, font=body_f, fill=ink)
            d.text((160, y), cost, font=body_f, fill=blue)
            d.rectangle([14 + a_ * 0.9, y + 17, 14 + b_ * 0.9, y + 24], fill=(220, 150, 40, 255))
        stamp("BOARD REVIEW", 112, 216, (24, 60, 150, 255))
    else:             # a P&ID print
        d.rectangle([0, 0, N, N], fill=(234, 240, 246, 255))
        k = (24, 30, 40, 255)
        d.rectangle([20, 50, 70, 170], outline=k, width=3)
        d.text((24, 176), "SG-A", font=body_f, fill=k)
        d.line([(70, 90), (160, 90), (160, 180), (220, 180)], fill=k, width=3)
        d.polygon([(104, 80), (104, 100), (120, 90)], outline=k, width=2)
        d.polygon([(136, 80), (136, 100), (120, 90)], outline=k, width=2)
        d.ellipse([200, 164, 232, 196], outline=k, width=3)
        d.text((92, 62), "MS-101", font=body_f, fill=k)
        d.rectangle([150, 206, 248, 248], outline=k, width=2)
        d.text((156, 210), "DWG M-201", font=small_f, fill=k)
        d.text((156, 228), "SHEET 3  REV C", font=small_f, fill=k)
    return img


def _copier_drawings():
    """The copier's front elevation (32 x 32 model pixels at 16 px each) and plan (32 x 16), cut into blocks."""
    U = 16
    front = Image.new("RGBA", (32 * U, 32 * U), (44, 46, 50, 255))
    d = ImageDraw.Draw(front)

    def R(x0, y0, x1, y1, **kw):          # model coords (y up) to image
        d.rectangle([x0 * U, (32 - y1) * U, x1 * U - 1, (32 - y0) * U - 1], **kw)

    dark, edge, light = (44, 46, 50, 255), (26, 27, 30, 255), (198, 200, 204, 255)
    # Paper drawers: a panel line round each, a long handle recess, the paper size, a level gauge.
    for k, size in enumerate(("A3", "A4", "A4", "A4")):
        y0 = 1 + k * 3
        R(1.3, y0 + 0.1, 20.9, y0 + 2.9, fill=dark, outline=edge, width=3)
        R(8, y0 + 1.6, 14, y0 + 2.3, fill=(22, 22, 24, 255))
        R(2.2, y0 + 1.2, 4.2, y0 + 2.3, fill=(210, 212, 216, 255))
        d.text((2.4 * U, (32 - y0 - 2.25) * U), size, font=_font(14), fill=(20, 20, 24, 255))
        for g in range(4):
            R(17 + g * 0.6, y0 + 1.4, 17.4 + g * 0.6, y0 + 2.1, fill=(210, 212, 216, 255) if g < 3 - k % 2 else (80, 80, 84, 255))
    # The engine: a light band across the top with the model name, the front door, the toner window.
    R(1.3, 13.1, 20.9, 16.9, fill=dark, outline=edge, width=3)
    R(1.3, 16.0, 20.9, 16.9, fill=light)
    d.text((2 * U, (32 - 16.85) * U), "MFP 9800", font=_font(11), fill=(60, 62, 66, 255))
    R(9, 14.2, 13, 14.6, fill=(22, 22, 24, 255))
    for k, col in enumerate(((40, 200, 220), (220, 60, 160), (240, 210, 40), (30, 30, 30))):
        R(15.5 + k * 1.1, 14.0, 16.3 + k * 1.1, 15.2, fill=col + (255,))
    # The output bay back wall, the post, the scanner, the feeder, the panel front.
    R(1.3, 17, 17.5, 19, fill=(20, 21, 24, 255))
    R(17.5, 17, 21, 19, fill=light)
    R(1.2, 19, 21, 20.6, fill=light)
    R(1.6, 20.6, 17, 22.4, fill=dark)
    R(12.5, 19.4, 20.8, 20.6, fill=(30, 31, 34, 255))
    for k in range(6):
        R(13 + k * 1.2, 19.7, 13.8 + k * 1.2, 20.2, fill=(120, 124, 130, 255))
    # The finisher: light panels, a staple cartridge door, the tray slots.
    R(21.2, 1, 30.8, 17.6, fill=light)
    R(21.6, 1.5, 30.4, 8.5, outline=(150, 152, 156, 255), width=3)
    R(21.6, 9.5, 30.4, 16.8, outline=(150, 152, 156, 255), width=3)
    R(24.5, 5, 27.5, 5.6, fill=(120, 122, 126, 255))
    R(24.5, 13, 27.5, 13.6, fill=(120, 122, 126, 255))
    d.text((22 * U, (32 - 3) * U), "FINISHER", font=_font(11), fill=(110, 112, 116, 255))

    top = Image.new("RGBA", (32 * U, 16 * U), (198, 200, 204, 255))
    t = ImageDraw.Draw(top)

    def P(x0, z0, x1, z1, **kw):
        t.rectangle([x0 * U, z0 * U, x1 * U - 1, z1 * U - 1], **kw)

    P(1.6, 2.4, 17, 13, fill=(50, 52, 56, 255))                     # the feeder lid
    P(3, 4.5, 14.5, 11.5, fill=(244, 242, 234, 255))                 # the original in it
    for k in range(9):
        P(4, 5.5 + k * 0.6, 4 + (6 + (k * 7) % 5), 5.75 + k * 0.6, fill=(110, 110, 116, 255))
    P(2.2, 8, 13.5, 14, fill=(244, 242, 234, 255))                   # copies in the bay (seen from above)
    # The control panel: the touch screen with its tiles, the keypad.
    P(12.5, 13.2, 20.8, 16, fill=(30, 31, 34, 255))
    P(13, 13.5, 17.6, 15.7, fill=(232, 238, 246, 254))
    P(13, 13.5, 17.6, 14, fill=(30, 90, 170, 254))
    for k, col in enumerate(((40, 140, 220), (60, 180, 110), (240, 170, 40), (200, 70, 70))):
        P(13.3 + k * 1.1, 14.4, 14.1 + k * 1.1, 15.3, fill=col + (254,))
    for k in range(9):
        P(18.2 + (k % 3) * 0.8, 13.6 + (k // 3) * 0.7, 18.7 + (k % 3) * 0.8, 14.0 + (k // 3) * 0.7, fill=(150, 154, 160, 255))
    P(21.2, 3, 30.8, 14.6, fill=(60, 62, 66, 255))                   # the finisher top
    out = {}
    for r in range(2):
        for c in range(2):
            out[f"pi_copier_f_{r * 2 + c}"] = front.crop((c * 16 * U, (1 - r) * 16 * U, (c + 1) * 16 * U, (2 - r) * 16 * U))
    for c in range(2):
        out[f"pi_copier_t_{c}"] = top.crop((c * 16 * U, 0, (c + 1) * 16 * U, 16 * U))
    return out


_COPIER_TEX = _copier_drawings()


def _filing_front(drawers, low=False):
    img = _img((178, 180, 176))
    d = ImageDraw.Draw(img)
    hgt = S // drawers
    for k in range(drawers):
        y = k * hgt
        d.rectangle([4, y + 3, S - 5, y + hgt - 3], outline=(110, 112, 110, 255), width=2)
        d.rectangle([44, y + hgt // 3, 84, y + hgt // 3 + 6], fill=(90, 92, 94, 255))
        d.rectangle([54, y + hgt // 3 + 12, 74, y + hgt // 3 + 20], fill=(236, 236, 228, 255))
    return img


def _folder(colour):
    return _img(colour)


def _folder_tab():
    img = _img((214, 190, 130))
    d = ImageDraw.Draw(img)
    for x in (10, 60, 96):
        d.rectangle([x, 0, x + 24, S], fill=(236, 230, 210, 255))
    return img


def _page_edge():
    """The top edge of a book's pages: fine lines running front to back, a little shading at the sides."""
    img = _img((238, 232, 214))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 3):
        d.line([(x, 0), (x, S)], fill=(222, 214, 192, 255))
    return img


def _pages():
    img = _img((236, 230, 212))
    d = ImageDraw.Draw(img)
    for y in range(0, S, 4):
        d.line([(0, y), (S, y)], fill=(214, 206, 186, 255))
    return img


def _coffee():
    img = _img((60, 36, 20))
    d = ImageDraw.Draw(img)
    d.ellipse([20, 20, 108, 108], outline=(110, 74, 44, 255), width=6)
    return img


def _mug():
    img = _img((240, 240, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 50, S, 76], fill=(30, 80, 150, 255))
    return img


def makers():
    out = {f"pi_doc_{k}": (lambda k=k: _doc(k)) for k in range(DOCS)}
    out.update({
        "pi_page_edge": _page_edge,
        "pi_spines": lambda: _SHEET, "pi_pages": _pages, "pi_coffee": _coffee, "pi_mug": _mug,
        "pi_pen_blue": lambda: _img((40, 80, 200)), "pi_pen_black": lambda: _img((26, 26, 30)),
        "pi_pencil": lambda: _img((240, 196, 40)), "pi_highlighter": lambda: _img((250, 240, 60)),
        "pi_copier_dark": lambda: _img((44, 46, 50)), "pi_copier_light": lambda: _img((196, 198, 202)),
        **{k: (lambda v=v: v) for k, v in _COPIER_TEX.items()},
        "pi_filing_two": lambda: _filing_front(2), "pi_filing_one": lambda: _filing_front(1),
        "pi_filing_low": lambda: _filing_front(2, True),
        "pi_folder_manila": lambda: _folder((214, 190, 130)), "pi_folder_blue": lambda: _folder((90, 130, 190)),
        "pi_folder_tab": _folder_tab,
    })
    return out


SHARP = {f"pi_doc_{k}" for k in range(DOCS)} | {"pi_spines"} | set(_COPIER_TEX)
