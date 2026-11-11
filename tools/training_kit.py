"""The tower's training furniture (work list item 22 part B, room 1: the briefing room, take 3; Justin: "everything's kind
of copy pasted. The laptops look kind of low quality ... I don't like it when it looks like textures are stretched over
an area ... I like the long tables. Add some variants to it and then increase the detail a bit").

- training_desk: a training table that joins up in rows (left/right: the T-legs where a row ends), its top one of
  the variants below. Every seat's place has its gooseneck microphone (the push-to-talk key, the ring lit red while it
  is live) and its own things: a laptop (silver, graphite or black; open on one of the tower's desk pages, or shut), a
  tablet, a notepad and pen, a mouse, a phone, a mug or a bottle, the name tent card. The places between the seats
  have the coffee jug and cups on a tray, briefing packs, or nothing.
- training_chair: a sled-base training chair, mesh back, padded seat, arms; variants turned a little to the left or
  right or pushed back from the desk, so no two rows look alike.

Every texture is drawn at its face's own proportions at one density (TX texels a model pixel: 256 a block), so nothing
is stretched: plain materials read by position (Minecraft's default UV), the pictures (screens, keyboards, papers,
cards, logos) from regions of one atlas sized to their faces. Round things are stepped slabs that do not overlap, and
no two faces share a plane (no z-fighting).

Model pixels, the pieces facing north (the user looks north: the desk's far edge at z 0, its user's side at z 16; the
chair's sitter faces north).
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as MW

ROOT = Path(__file__).resolve().parent.parent
TX = 16                         # texels a model pixel
ATLAS = 1024                    # the picture atlas's size (texels): 64 a uv unit
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
PAGES = None                    # a folder of the desks' page pictures (<role>_<page>.png) for the screens, if given
SCR = None                      # the screens' atlas (write() makes it)
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ORANGE = (232, 150, 40)


# ------------------------------------------------------------------ materials (256 x 256: one block at TX)
def _noise(seed, sigma, size=256):
    return np.random.default_rng(seed).normal(0, sigma, (size, size))


def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB")


def _periodic(n, seed, kmax, power=0.9):
    """Smooth noise along one axis that wraps round (a sum of whole-period waves), scaled to -1..1."""
    rng = np.random.default_rng(seed)
    t = np.arange(n) / n
    out = sum(rng.normal() / k ** power * np.sin(math.tau * k * t + rng.uniform(0, math.tau)) for k in range(1, kmax + 1))
    return out / np.abs(out).max()


def oak():
    """Light oak laminate: straight grain the length of the desk (along x), the lines and their shading the same at
    both edges of the block either way, so a row of desks reads as one top with no seams; calm, no speckle."""
    n = 256
    rng = np.random.default_rng(2201)
    rows = _periodic(n, 2202, 40)                                   # the broad light and dark bands down the rows
    shade = 1 + 0.045 * rows[:, None] * np.ones((1, n))
    # the fine grain lines: each its own darkness, fading in and out along the length (whole periods: seamless)
    for k in range(46):
        y = rng.uniform(0, n)
        fade = 0.5 + 0.5 * np.sin(math.tau * rng.integers(1, 3) * np.arange(n) / n + rng.uniform(0, math.tau))
        w = rng.uniform(0.6, 1.6)
        d = np.abs(((np.arange(n) - y + n / 2) % n) - n / 2)[:, None]
        shade -= rng.uniform(0.04, 0.09) * np.clip(1 - d / w, 0, 1) * (0.35 + 0.65 * fade[None, :])
    # the pores: short dashes along the grain, wrapping round
    for _ in range(140):
        y, x0, ln = rng.integers(0, n), rng.integers(0, n), rng.integers(4, 13)
        shade[y, (x0 + np.arange(ln)) % n] -= rng.uniform(0.03, 0.06)
    base = np.array((198, 170, 134), float)
    a = base[None, None, :] * shade[..., None] + np.random.default_rng(2203).normal(0, 0.8, (n, n))[..., None]
    return _img(a)


def brushed(rgb, seed, sigma=2.5):
    """A metal brushed along x: fine streaks the length of the block."""
    a = np.stack([np.full((256, 256), c, float) for c in rgb], -1)
    rng = np.random.default_rng(seed)
    a += rng.normal(0, sigma * 1.4, (256, 1))[..., None] + rng.normal(0, sigma * 0.6, (256, 256))[..., None]
    return _img(a)


def solid(rgb, seed, sigma=2.0):
    return _img(np.stack([np.full((256, 256), c, float) for c in rgb], -1) + _noise(seed, sigma)[..., None])


def perforated():
    """Dark powder-coated steel, perforated: holes 2 texels on a 5-texel staggered grid."""
    a = np.stack([np.full((256, 256), c, float) for c in (58, 60, 64)], -1) + _noise(2203, 2)[..., None]
    for yy in range(0, 256, 5):
        off = 2 if (yy // 5) % 2 else 0
        for xx in range(off, 256, 5):
            a[yy:yy + 2, xx:xx + 2] = (18, 18, 20)
    return _img(a)


def fabric():
    """The seat's charcoal woven fabric: a fine basket weave."""
    y, x = np.mgrid[0:256, 0:256]
    w = ((x // 2 + y // 2) % 2) * 6 - 3
    return _img(np.stack([np.full((256, 256), c, float) for c in (56, 58, 64)], -1) + w[..., None] + _noise(2204, 3)[..., None])


def mesh():
    """The back's mesh: a dark diamond weave over a darker gap."""
    y, x = np.mgrid[0:256, 0:256]
    d = (np.abs(((x + y) % 6) - 3) < 1) | (np.abs(((x - y) % 6) - 3) < 1)
    return _img(np.where(d[..., None], np.array([60, 62, 68], float), np.array([22, 23, 26], float)) + _noise(2205, 2)[..., None])


MATERIALS = {
    "td_oak": oak, "td_edge": lambda: solid((50, 52, 56), 2206), "td_perf": perforated,
    "td_steel": lambda: solid((40, 42, 46), 2207, 2.5), "td_alu": lambda: brushed((182, 186, 192), 2208),
    "td_graphite": lambda: brushed((78, 82, 88), 2209), "td_black": lambda: solid((26, 27, 29), 2210, 1.5),
    "td_chrome": lambda: brushed((166, 170, 176), 2211, 4.5), "td_fabric": fabric, "td_mesh": mesh,
    "td_white": lambda: solid((238, 238, 234), 2212), "td_rubber": lambda: solid((30, 30, 32), 2213, 3),
    "td_red": lambda: solid((220, 38, 30), 2214), "td_red_off": lambda: solid((64, 60, 60), 2215),
    "td_navy": lambda: solid((30, 46, 82), 2216), "td_coffee": lambda: solid((58, 34, 18), 2217, 3),
    "td_mug_red": lambda: solid((172, 40, 36), 2218), "td_mug_grey": lambda: solid((70, 72, 78), 2219),
    "td_orange": lambda: solid(ORANGE, 2220), "td_teal": lambda: solid((30, 120, 120), 2221),
    "td_paper": lambda: solid((246, 244, 236), 2222, 1.5), "td_amber": lambda: solid((255, 176, 64), 2224, 1.0),
    "td_grille": lambda: grille(), "td_green": lambda: solid((70, 230, 110), 2226, 1.0),
    "td_cloth": lambda: cloth()}


def cloth():
    """A speaker's grille cloth: a fine dark weave."""
    y, x = np.mgrid[0:256, 0:256]
    w = ((x % 2) ^ (y % 2)) * 5 - 2.5
    return _img(np.stack([np.full((256, 256), c, float) for c in (34, 35, 38)], -1) + w[..., None] + _noise(2227, 1.5)[..., None])


def grille():
    """A ceiling speaker's grille: white powder-coated steel, fine round holes on a 4-texel grid."""
    a = np.stack([np.full((256, 256), c, float) for c in (232, 232, 228)], -1) + _noise(2225, 1.5)[..., None]
    for yy in range(1, 256, 4):
        for xx in range(1, 256, 4):
            a[yy:yy + 2, xx:xx + 2] = (120, 122, 124)
    return _img(a)


# ------------------------------------------------------------------ live pictures
# What part of a picture the game draws live over the picture (B2: a laptop's page, a clock's figures, a counter...):
# a drawing function marks it with live(); the atlas keeps it by the picture's key, and tools/room_kit.py finds the
# faces that show it and writes where they are into the room kits' manifest (client/RoomKitDisplayRenderer draws them).
_DRAWING = []                    # the (atlas, key) of the picture being drawn
ATLASES = []                     # every atlas made (tools/room_kit.py: each kit's own, by their keys)
# live parts of whole textures that are not in an atlas (a simulator's screens): texture ("ns:block/name") -> its parts,
# each as live() makes them (rect: fractions of the texture)
TEXTURE_LIVE = {}


def live(img, kind, rect, **params):
    """Marks rect (x0, y0, x1, y1 in the picture's texels) of the picture being drawn as drawn live in game: kind and its
    parameters (a page's role and number; a clock's figures...); a picture may have several. Outside an atlas's drawing
    it does nothing."""
    if _DRAWING:
        atlas, key = _DRAWING[-1]
        W, H = img.size
        atlas.live.setdefault(key, []).append({"kind": kind, "rect": [round(rect[0] / W, 5), round(rect[1] / H, 5), round(rect[2] / W, 5),
                                                                     round(rect[3] / H, 5)], **params})


def live_text(img, xy, s, font, fill, anchor="mm", bg=None, widest=None, pad=0.18, bake=True, **params):
    """A readout drawn live in game (client/LiveFaces.text): s drawn here at xy (what it shows when the picture is made;
    not drawn, bake=False, where it sits on a picture with no plain ground of its own and the game draws it alone), and
    its box marked live, wide enough for `widest` (its longest reading), padded by `pad` of its height (a third of it
    across), on bg
    (the ground the game paints first, over the baked reading). params: src (time, time12, dtime, value, timer) and
    its own (tools: base, spread, period, dp, pre, unit...)."""
    d = ImageDraw.Draw(img)
    if bake:
        d.text(xy, s, font=font, fill=fill, anchor=anchor)
    boxes = [d.textbbox(xy, t, font=font, anchor=anchor) for t in {s, widest or s}]
    x0, y0, x1, y1 = min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes)
    h = y1 - y0
    p, px = h * pad, h * pad / 3                # (less across: the next letters are close)
    W, H = img.size
    rect = (max(0, x0 - px), max(0, y0 - p), min(W, x1 + px), min(H, y1 + p))
    extra = {"bg": [int(c) for c in bg[:3]]} if bg is not None else {}
    live(img, "text", rect, fg=[int(c) for c in fill[:3]], align={"l": 0, "m": 1, "r": 2}[anchor[0]],
         cap=round(h / (rect[3] - rect[1]), 3), **extra, **params)


def live_part(img, xy, full, part, font, fill, anchor="mm", bg=None, widest=None, again=False, **params):
    """full drawn at xy, and the part of it that changes (a clock's time, a timer, a figure) marked live as live_text
    (again: another part of a text already drawn, so it is not drawn twice)."""
    d = ImageDraw.Draw(img)
    if not again:
        d.text(xy, full, font=font, fill=fill, anchor=anchor)
    x0 = d.textbbox(xy, full, font=font, anchor=anchor)[0]
    i = full.index(part)
    x = x0 + d.textlength(full[:i], font=font) if i else x0
    live_text(img, (x, xy[1]), part, font, fill, "l" + anchor[1], bg=bg, widest=widest, bake=False, **params)


def turned180(draw):
    """draw, its picture turned upside down (a phone facing the other way across a desk), its live parts with it."""
    def turned(img):
        out = draw(img).rotate(180)
        if _DRAWING:
            atlas, key = _DRAWING[-1]
            for info in atlas.live.get(key, []):
                x0, y0, x1, y1 = info["rect"]
                info["rect"] = [round(1 - x1, 5), round(1 - y1, 5), round(1 - x0, 5), round(1 - y0, 5)]
                info["turn"] = (info.get("turn", 0) + 180) % 360
        return out
    return turned


def drawn(atlas, key, draw, size):
    """draw(image of size) for atlas's picture key, its live parts kept by the atlas."""
    _DRAWING.append((atlas, key))
    try:
        return draw(Image.new("RGB", size))
    finally:
        _DRAWING.pop()


# ------------------------------------------------------------------ the picture atlas
class Atlas:
    """One size x size texture: pictures drawn at tx texels a pixel in regions sized to the faces that show them. The
    pictures atlas at the materials' density (TX); the screens' at four times it (crisp pages and digits)."""

    def __init__(self, size=ATLAS, tx=TX, key="atlas"):
        self.size, self.tx, self.key = size, tx, key
        self.img = Image.new("RGB", (size, size), (30, 30, 34))
        self.x = self.y = self.row = 0
        self.regions = {}
        self.live = {}                # key -> what of its picture is live (live())
        ATLASES.append(self)

    def face(self, key, w, h, draw):
        return {"texture": f"#{self.key}", "uv": self.region(key, w, h, draw)}

    def region(self, key, w, h, draw):
        """The uv of a w x h (model pixels) region for `key`, drawn (once) by draw(image of that size)."""
        if key not in self.regions:
            tw, th = round(w * self.tx), round(h * self.tx)
            if self.x + tw > self.size:
                self.x, self.y, self.row = 0, self.y + self.row + 2, 0
            assert self.y + th <= self.size, ("atlas full", key)
            sub = drawn(self, key, draw, (tw, th))
            self.img.paste(sub, (self.x, self.y))
            k = 16 / self.size
            self.regions[key] = [round(self.x * k, 5), round(self.y * k, 5), round((self.x + tw) * k, 5), round((self.y + th) * k, 5)]
            self.x += tw + 2
            self.row = max(self.row, th)
        return self.regions[key]


BODY = {"silver": (182, 186, 192), "graphite": (78, 82, 88), "black": (32, 33, 36)}


def _brushed_fill(img, rgb, seed):
    W, H = img.size
    rng = np.random.default_rng(seed)
    a = np.stack([np.full((H, W), c, float) for c in rgb], -1)
    a += rng.normal(0, 3.5, (H, 1))[..., None] + rng.normal(0, 1.5, (H, W))[..., None]
    img.paste(_img(a), (0, 0))
    return ImageDraw.Draw(img)


def deck(kind):
    """A laptop's deck from above, its hinge at the top: the keyboard in its well, the trackpad, the speaker grilles."""
    def draw(img):
        W, H = img.size
        d = _brushed_fill(img, BODY[kind], 31)
        dark = kind != "silver"
        well = (14, 8, W - 14, round(H * 0.58))
        d.rectangle(well, fill=(20, 21, 23) if dark else (58, 60, 64))
        kh = (well[3] - well[1] - 4) / 5.0
        key = (44, 46, 50) if dark else (28, 29, 32)
        legend = (120, 124, 130)
        for r, n in enumerate((14, 14, 13, 12)):
            y0 = well[1] + 2 + r * kh
            kw = (well[2] - well[0] - 4) / n
            for k in range(n):
                x0 = well[0] + 2 + k * kw
                d.rectangle((x0 + 1, y0 + 1, x0 + kw - 1, y0 + kh - 1), fill=key)
                d.point((x0 + 3, y0 + 3), fill=legend)
        y0 = well[1] + 2 + 4 * kh
        span = well[2] - well[0]
        for a, b in ((0.0, 0.14), (0.15, 0.29), (0.30, 0.70), (0.71, 0.79)):        # the modifiers, the space bar
            d.rectangle((well[0] + 2 + span * a + 1, y0 + 1, well[0] + span * b - 1, y0 + kh - 1), fill=key)
        x4 = well[0] + span * 0.80
        aw = (well[2] - 2 - x4) / 3
        for k in range(3):                                                         # the arrow keys
            d.rectangle((x4 + k * aw + 1, y0 + kh / 2 + 1, x4 + (k + 1) * aw - 1, y0 + kh - 1), fill=key)
        d.rectangle((x4 + aw + 1, y0 + 1, x4 + 2 * aw - 1, y0 + kh / 2 - 1), fill=key)
        for gx in (5, W - 9):                                                      # the speaker grilles
            for gy in range(well[1] + 2, well[3] - 2, 3):
                d.point((gx, gy), fill=(36, 36, 40))
                d.point((gx + 2, gy + 1), fill=(36, 36, 40))
        pw, ph = W * 0.36, H * 0.30                                                # the trackpad
        px0, py0 = (W - pw) / 2, H - ph - 5
        d.rectangle((px0, py0, px0 + pw, py0 + ph), fill=tuple(min(255, c + 8) for c in BODY[kind]),
                    outline=tuple(max(0, c - 26) for c in BODY[kind]))
        d.rectangle((0, 0, W, 3), fill=(24, 24, 26))                               # the hinge
        return img
    return draw


def lid_back(kind):
    def draw(img):
        W, H = img.size
        d = _brushed_fill(img, BODY[kind], 32)
        cx, cy = W / 2, H * 0.48
        mark = (150, 154, 160) if kind == "silver" else (122, 126, 132)
        for dx in (-7, 0, 7):
            d.rectangle((cx + dx - 2, cy - 9, cx + dx + 2, cy + 9), fill=mark)
        return img
    return draw


def _page_into(img, page, inner):
    iw, ih = inner[2] - inner[0], inner[3] - inner[1]
    p = Path(PAGES) / f"{page}.png" if PAGES else None
    if p and p.exists():
        q = Image.open(p).convert("RGB")
        k = min(iw / q.width, ih / q.height)
        q = q.resize((max(1, round(q.width * k)), max(1, round(q.height * k))), Image.LANCZOS)
        x0, y0 = inner[0] + (iw - q.width) // 2, inner[1] + (ih - q.height) // 2
        img.paste(q, (x0, y0))
        role, number = page.split("_")
        live(img, "page", (x0, y0, x0 + q.width, y0 + q.height), role=int(role), page=int(number))
    else:
        d = ImageDraw.Draw(img)
        rng = random.Random(page)
        d.rectangle((inner[0], inner[1], inner[2], inner[1] + 6), fill=(46, 36, 18))
        for i in range(7):
            y = inner[1] + 10 + i * (ih - 12) / 7
            d.rectangle((inner[0] + 4, y, inner[0] + 4 + rng.randint(20, iw - 10), y + 2), fill=(120, 160, 170))


def screen(page):
    """A laptop's screen: the black glass bezel, the camera, the tower's desk page fitted in at its own aspect."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 10, 12))
        k = W / (6.8 * TX)                                                       # texels per material texel
        inner = (round(5 * k), round(6 * k), W - round(5 * k), H - round(9 * k))
        d.rectangle(inner, fill=(9, 13, 15))
        _page_into(img, page, inner)
        d.ellipse((W / 2 - 2 * k, 1.5 * k, W / 2 + 2 * k, 3.5 * k), fill=(40, 48, 56))
        return img
    return draw


def tablet_face(page):
    def draw(img):
        W, H = img.size
        ImageDraw.Draw(img).rectangle((0, 0, W, H), fill=(14, 14, 16))
        b = round(W / (6.4 * TX) * 6)
        _page_into(img, page, (b, b, W - b, H - b))
        return img
    return draw


def paper(seed):
    """A notepad's top sheet: the binding, the ruling and margin, a few lines of handwriting."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 244, 236))
        d.rectangle((0, 0, W, 6), fill=(30, 46, 82))
        for y in range(16, H - 2, 7):
            d.line((2, y, W - 3, y), fill=(178, 196, 222))
        d.line((10, 7, 10, H), fill=(226, 156, 156))
        rng = random.Random(seed)
        y = 14
        for _ in range(rng.randint(2, 6)):
            x = 13
            while x < W - 8:
                n = rng.randint(4, 12)
                if x + n > W - 4:
                    break
                d.line((x, y, x + n, y), fill=(40, 50, 96))
                x += n + rng.randint(2, 4)
            y += 7
        return img
    return draw


def tent(name, role):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(242, 242, 238))
        d.rectangle((0, H - 3, W, H), fill=ORANGE)
        size = 10
        while size > 5 and d.textlength(name, font=ImageFont.truetype(FONT, size)) > W - 6:
            size -= 1
        d.text((W / 2, H * 0.38), name, font=ImageFont.truetype(FONT, size), fill=(26, 28, 34), anchor="mm")
        d.text((W / 2, H * 0.76), role, font=ImageFont.truetype(FONT_R, 6), fill=(96, 98, 104), anchor="mm")
        return img
    return draw


def folder_cover(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 46, 82))
    d.rectangle((0, H - 12, W, H), fill=(24, 36, 66))
    cx, cy = W / 2, H * 0.4
    for dx in (-9, 0, 9):
        d.rectangle((cx + dx - 2.5, cy - 15, cx + dx + 2.5, cy + 11), fill=ORANGE)
    d.text((cx, cy + 24), "MISSION BRIEF", font=ImageFont.truetype(FONT, 9), fill=(230, 232, 236), anchor="mm")
    d.text((cx, cy + 36), "RESTRICTED", font=ImageFont.truetype(FONT_R, 7), fill=(210, 128, 118), anchor="mm")
    return img


def logo(rgb):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=rgb)
        cx, cy = W / 2, H / 2
        for dx in (-5, 0, 5):
            d.rectangle((cx + dx - 1.5, cy - 8, cx + dx + 1.5, cy + 6), fill=ORANGE)
        return img
    return draw


def socket_plate(img):
    W, H = img.size
    d = _brushed_fill(img, BODY["graphite"], 33)
    d.rectangle((3, 3, W - 4, H - 4), fill=(22, 22, 24))
    for k in range(2):
        cx = 12 + k * 15
        d.rectangle((cx - 5, 6, cx + 5, H - 7), fill=(38, 38, 42))
        d.rectangle((cx - 2, 9, cx - 1, 13), fill=(8, 8, 8))
        d.rectangle((cx + 1, 9, cx + 2, 13), fill=(8, 8, 8))
    for k in range(2):
        d.rectangle((W - 18 + k * 7, 9, W - 14 + k * 7, 13), fill=(70, 120, 200) if k else (10, 10, 10))
    return img


# ------------------------------------------------------------------ boxes
def box(f, t, faces, rot=None):
    e = {"from": [round(v, 4) for v in f], "to": [round(v, 4) for v in t], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def mat(tex, skip=()):
    """A box's faces all of one material, read by position (no stretch)."""
    return {k: {"texture": f"#{tex}"} for k in ("north", "south", "east", "west", "up", "down") if k not in skip}


def faces(base, **pics):
    """Faces of `base` material, with pictures {face: uv from the pictures atlas, or a face from Atlas.face}."""
    out = mat(base)
    for k, uv in pics.items():
        out[k] = uv if isinstance(uv, dict) else {"texture": "#atlas", "uv": uv}
    return out


def round_part(cx, cz, r, y0, y1, tex, pic=None, rot=None, n=3):
    """A round part standing up as slabs side by side, none overlapping (no shared planes): a wide middle one and n - 1
    narrower each side of it north and south, stepping in to the circle. `pic` (the uv of a 2r wide picture) is laid on
    it from the south and from the north as a print would be: each slab's face shows the part of the picture in front
    of it (its own width of it, so nothing stretches)."""
    if n == 3:
        bands = ((0.42, 1.0), (0.80, 0.82), (1.0, 0.48))         # (outer z edge, half width) as fractions of r
    else:
        edges = [(k + 0.5) / (n - 0.5) for k in range(n)]
        bands = [(e, math.sqrt(max(0.0, 1 - ((edges[k - 1] + e) / 2) ** 2)) if k else 1.0) for k, e in enumerate(edges)]
    out = []
    z_in = 0.0
    for i, (zo, hw) in enumerate(bands):
        for za, zb in ([(cz - zo * r, cz + zo * r)] if i == 0 else [(cz + z_in * r, cz + zo * r), (cz - zo * r, cz - z_in * r)]):
            fc = mat(tex)
            if pic:
                u0, v0, u1, v1 = pic
                su = (u1 - u0) / (2 * r)
                a, b = (1 - hw) * r, (1 + hw) * r                     # the slab's x span from the part's west edge
                if i == 0 or zb > cz:
                    fc["south"] = {"texture": "#atlas", "uv": [round(u0 + a * su, 5), v0, round(u0 + b * su, 5), v1]}
                if i == 0 or za < cz:
                    fc["north"] = {"texture": "#atlas", "uv": [round(u0 + a * su, 5), v0, round(u0 + b * su, 5), v1]}
            out.append(box((cx - hw * r, y0, za), (cx + hw * r, y1, zb), fc, rot))
        z_in = zo
    return out


# ------------------------------------------------------------------ the desk
TOP_Y = 12.6


def desk_body(A):
    plate = A.region("sockets", 3.4, 1.4, socket_plate)
    return [box((0, 11.6, 0.8), (16, TOP_Y, 15.2), {"up": {"texture": "#oak"}, "down": {"texture": "#steel"},
                                                     "north": {"texture": "#edge"}, "south": {"texture": "#edge"},
                                                     "east": {"texture": "#edge"}, "west": {"texture": "#edge"}}),
            # the perforated modesty panel under its far edge, the apron rail under its front
            box((0, 3.6, 1.6), (16, 11.6, 2.1), mat("perf", skip=("up",))),
            box((0, 10.6, 13.8), (16, 11.6, 14.4), mat("steel", skip=("up",))),
            # the flip-up power module let into the top at the back: its frame and its sockets
            box((6.3, TOP_Y, 1.4), (9.7, TOP_Y + 0.3, 2.8), faces("graphite", up=plate))]


def desk_end(x):
    """The T-leg at a row's end: its post, its foot (rubber pads), the rail under the top."""
    return [box((x - 0.8, 0.9, 7.2), (x + 0.8, 10.8, 8.8), mat("steel")),
            box((x - 1.0, 0.3, 1.4), (x + 1.0, 0.9, 14.6), mat("steel")),
            box((x - 0.9, 0, 1.6), (x + 0.9, 0.3, 2.6), mat("rubber")),
            box((x - 0.9, 0, 13.4), (x + 0.9, 0.3, 14.4), mat("rubber")),
            box((x - 0.8, 10.8, 1.4), (x + 0.8, 11.6, 14.6), mat("steel", skip=("up",)))]


def mic(live=False, x=0.9, z=2.0):
    """The gooseneck microphone: its base with the push-to-talk key and the ring, the neck, the capsule."""
    ring = "red" if live else "red_off"
    out = [box((x, TOP_Y, z), (x + 3.0, TOP_Y + 0.7, z + 2.4), mat("black")),
           box((x + 0.5, TOP_Y + 0.7, z + 1.3), (x + 2.5, TOP_Y + 0.9, z + 2.1), mat("graphite")),
           box((x + 2.2, TOP_Y + 0.7, z + 0.4), (x + 2.7, TOP_Y + 0.8, z + 0.9), mat(ring))]
    nx, nz = x + 1.5, z + 0.7
    bend = {"angle": 22.5, "axis": "x", "origin": [nx, TOP_Y + 4.2, nz]}
    out += [box((nx - 0.2, TOP_Y + 0.7, nz - 0.2), (nx + 0.2, TOP_Y + 4.0, nz + 0.2), mat("black")),
            # the neck's bend over toward the user, the capsule and its ring at its end
            box((nx - 0.3, TOP_Y + 4.0, nz - 0.2), (nx + 0.3, TOP_Y + 4.4, nz + 2.4), mat("black"), bend),
            box((nx - 0.45, TOP_Y + 3.8, nz + 2.4), (nx + 0.45, TOP_Y + 4.6, nz + 3.5), mat("rubber"), bend),
            box((nx - 0.5, TOP_Y + 3.75, nz + 3.5), (nx + 0.5, TOP_Y + 4.65, nz + 3.7), mat(ring), bend)]
    return out


def laptop(A, kind, page, shut=False, x0=4.6, z0=5.0, w=7.0, d=4.8, lid=4.6):
    """A laptop open toward its user: the base (its deck the keyboard, trackpad and grilles), the lid leaning back on its
    hinge, the screen on its inside, the maker's mark on its back; or shut, the lid on the base."""
    body = {"silver": "alu", "graphite": "graphite", "black": "black"}[kind]
    if shut:
        return [box((x0, TOP_Y, z0), (x0 + w, TOP_Y + 0.7, z0 + d), faces(body, up=A.region(f"shut_{kind}", w, d, lid_back(kind))))]
    out = [box((x0, TOP_Y, z0), (x0 + w, TOP_Y + 0.4, z0 + d), faces(body, up=A.region(f"deck_{kind}", w, d, deck(kind))))]
    scr = SCR.face(f"screen_{page}", w - 0.2, lid, screen(page))
    back = A.region(f"lid_{kind}", w - 0.2, lid, lid_back(kind))
    out.append(box((x0 + 0.1, TOP_Y + 0.4, z0), (x0 + w - 0.1, TOP_Y + 0.4 + lid, z0 + 0.3), faces(body, south=scr, north=back),
                   {"angle": -22.5, "axis": "x", "origin": [x0 + w / 2, TOP_Y + 0.4, z0 + 0.3]}))
    return out


def tablet(A, page, x0=5.2, z0=5.6):
    """A tablet in its folio, propped toward its user on the folio's fold."""
    s = SCR.face(f"tab_{page}", 6.4, 4.4, tablet_face(page))
    return [box((x0, TOP_Y + 0.3, z0 + 4.4), (x0 + 6.4, TOP_Y + 4.7, z0 + 4.7), faces("navy", south=s),
                {"angle": -45, "axis": "x", "origin": [x0 + 3.2, TOP_Y + 0.3, z0 + 4.6]}),
            box((x0, TOP_Y, z0 + 1.6), (x0 + 6.4, TOP_Y + 0.3, z0 + 5.0), mat("navy"))]


def notepad(A, seed, x0=12.0, z0=7.2, w=3.6, d=5.0, pen=True):
    out = [box((x0, TOP_Y, z0), (x0 + w, TOP_Y + 0.3, z0 + d), faces("paper", up=A.region(f"pad_{seed}", w, d, paper(seed))))]
    if pen:
        out.append(box((x0 + w - 0.9, TOP_Y + 0.3, z0 + 0.6), (x0 + w - 0.6, TOP_Y + 0.6, z0 + d - 0.8), mat("black"),
                       {"angle": 22.5, "axis": "y", "origin": [x0 + w - 0.75, TOP_Y + 0.3, z0 + d / 2]}))
    return out


def mouse(x0=12.8, z0=8.4):
    return [box((x0, TOP_Y, z0), (x0 + 1.4, TOP_Y + 0.5, z0 + 2.2), mat("black")),
            box((x0 + 0.2, TOP_Y + 0.5, z0 + 0.2), (x0 + 1.2, TOP_Y + 0.7, z0 + 1.6), mat("graphite"))]


def phone(x0=12.4, z0=12.2):
    return [box((x0, TOP_Y, z0), (x0 + 1.6, TOP_Y + 0.2, z0 + 3.0), mat("black"),
                {"angle": -22.5, "axis": "y", "origin": [x0 + 0.8, TOP_Y, z0 + 1.5]})]


MUG_RGB = {"white": (238, 238, 234), "navy": (30, 46, 82), "mug_red": (172, 40, 36), "mug_grey": (70, 72, 78)}


def round_ring(cx, cz, r, wall, y0, y1, tex, floor_h, floor_tex, pic=None, n=5):
    """A round vessel open at the top (a cup, a mug): its wall a ring of slabs side by side (none overlapping), its
    floor `floor_h` up inside it showing floor_tex (the cup's own inside, or the coffee). `pic` (the uv of a 2r wide
    picture) is laid on its outside from the south and from the north as a print would be."""
    edges = [(k + 0.5) / (n - 0.5) for k in range(n)]
    ri = (r - wall) / r
    out = []
    z_in = 0.0
    for i, zo in enumerate(edges):
        mid = (z_in + zo) / 2 if i else 0.0
        hw = math.sqrt(max(0.0, 1 - mid ** 2)) if i else 1.0
        hi = math.sqrt(ri ** 2 - zo ** 2) if zo < ri else 0.0          # the opening's half width (at the band's edge)
        for za, zb in ([(cz - zo * r, cz + zo * r)] if i == 0 else [(cz + z_in * r, cz + zo * r), (cz - zo * r, cz - z_in * r)]):
            spans = [(-hw, -hi), (hi, hw)] if hi > 0.05 else [(-hw, hw)]
            for a, b in spans:
                fc = mat(tex)
                if pic:
                    u0, v0, u1, v1 = pic
                    su = (u1 - u0) / (2 * r)
                    if i == 0 or zb > cz:
                        fc["south"] = {"texture": "#atlas", "uv": [round(u0 + (1 + a) * r * su, 5), v0, round(u0 + (1 + b) * r * su, 5), v1]}
                    if i == 0 or za < cz:
                        fc["north"] = {"texture": "#atlas", "uv": [round(u0 + (1 - b) * r * su, 5), v0, round(u0 + (1 - a) * r * su, 5), v1]}
                out.append(box((cx + a * r, y0, za), (cx + b * r, y1, zb), fc))
            if hi > 0.05:
                out.append(box((cx - hi * r, y0, za), (cx + hi * r, y0 + floor_h, zb), {**mat(tex), "up": {"texture": f"#{floor_tex}"}}))
        z_in = zo
    return out


def mug(A, colour, cx=13.8, cz=4.2):
    """A mug: its body (the tower's mark on its front and back, if it has one), open at the top with the coffee a little
    down inside it, its handle."""
    pic = A.region(f"mug_{colour}", 2.2, 2.4, logo(MUG_RGB[colour])) if colour in ("white", "navy", "mug_grey") else None
    out = round_ring(cx, cz, 1.1, 0.2, TOP_Y, TOP_Y + 2.4, colour, 1.9, "coffee", pic)
    out += [box((cx + 1.1, TOP_Y + 0.6, cz - 0.25), (cx + 1.7, TOP_Y + 0.9, cz + 0.25), mat(colour)),
            box((cx + 1.4, TOP_Y + 0.9, cz - 0.25), (cx + 1.7, TOP_Y + 1.6, cz + 0.25), mat(colour)),
            box((cx + 1.1, TOP_Y + 1.6, cz - 0.25), (cx + 1.7, TOP_Y + 1.9, cz + 0.25), mat(colour))]
    return out


def _well(e, frac, depth, tex):
    """Splits a round part's middle slab in three along x and sinks the middle third's top by `depth` (a mug's coffee
    inside its rim), the north and south pictures (if any) shared between the three as they lay on the whole."""
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    cx, hw = (x0 + x1) / 2, (x1 - x0) / 2
    xs = [x0, cx - frac * hw, cx + frac * hw, x1]
    out = []
    for k in range(3):
        fc = json.loads(json.dumps(e["faces"]))
        for side in ("north", "south"):
            uv = fc[side].get("uv")
            if uv:
                su = (uv[2] - uv[0]) / (x1 - x0)
                a, b = (xs[k] - x0, xs[k + 1] - x0) if side == "south" else (x1 - xs[k + 1], x1 - xs[k])
                fc[side]["uv"] = [round(uv[0] + a * su, 5), uv[1], round(uv[0] + b * su, 5), uv[3]]
        top = y1 - depth if k == 1 else y1
        if k == 1:
            fc["up"] = {"texture": f"#{tex}"}
            for side in ("north", "south"):
                if "uv" in fc[side]:
                    u = fc[side]["uv"]
                    fc[side]["uv"] = [u[0], round(u[1] + depth / (y1 - y0) * (u[3] - u[1]), 5), u[2], u[3]]
        out.append(box((xs[k], y0, z0), (xs[k + 1], top, z1), fc, e.get("rotation")))
    return out


def bottle(colour, cx=13.8, cz=4.0):
    """A reusable steel bottle: its body, its cap and the cap's loop."""
    return (round_part(cx, cz, 0.7, TOP_Y, TOP_Y + 3.6, colour) + round_part(cx, cz, 0.45, TOP_Y + 3.6, TOP_Y + 4.3, "black")
            + [box((cx - 0.12, TOP_Y + 4.3, cz - 0.6), (cx + 0.12, TOP_Y + 4.6, cz + 0.6), mat("black"))])


def name_card(A, name, role, x0=9.6, zc=1.3):
    """The name tent card on the far edge, its name on both faces (to the front of the room and to its user)."""
    c = A.region(f"card_{name}", 4.6, 1.7, tent(name, role))
    side = {"east": {"texture": "#white"}, "west": {"texture": "#white"}, "up": {"texture": "#white"}, "down": {"texture": "#white"}}
    return [box((x0, TOP_Y, zc - 0.7), (x0 + 4.6, TOP_Y + 1.7, zc - 0.6), {**side, "north": {"texture": "#atlas", "uv": c}},
                {"angle": 22.5, "axis": "x", "origin": [x0, TOP_Y, zc - 0.65]}),
            box((x0, TOP_Y, zc + 0.6), (x0 + 4.6, TOP_Y + 1.7, zc + 0.7), {**side, "south": {"texture": "#atlas", "uv": c}},
                {"angle": -22.5, "axis": "x", "origin": [x0, TOP_Y, zc + 0.65]})]


def folder(A, x0=2.0, z0=6.0, turn=0.0):
    """A briefing pack: the navy folder, the tower's mark and MISSION BRIEF on its cover, the papers' edges in it."""
    r = {"angle": turn, "axis": "y", "origin": [x0 + 2.7, TOP_Y, z0 + 3.7]} if turn else None
    return [box((x0, TOP_Y, z0), (x0 + 5.4, TOP_Y + 0.5, z0 + 7.4),
                {**mat("paper"), "up": {"texture": "#atlas", "uv": A.region("folder", 5.4, 7.4, folder_cover)},
                 "north": {"texture": "#navy"}, "west": {"texture": "#navy"}}, r)]


def jug_tray():
    """Between the seats: the coffee jug (a steel vacuum jug, its pump top) and white cups, open and empty, on a tray."""
    out = [box((3.0, TOP_Y, 3.6), (13.0, TOP_Y + 0.3, 11.6), mat("black"))]
    t = TOP_Y + 0.3
    out += round_part(6.4, 7.4, 1.6, t, t + 5.0, "chrome", n=4) + round_part(6.4, 7.4, 1.1, t + 5.0, t + 5.8, "black")
    out += [box((5.9, t + 5.8, 6.9), (6.9, t + 6.2, 7.9), mat("black")),             # the pump's push
            box((6.1, t + 3.9, 5.2), (6.7, t + 4.5, 5.8), mat("black")),             # the spout
            box((8.0, t + 1.0, 7.1), (8.6, t + 4.6, 7.7), mat("black"))]             # the handle
    for cx, cz in ((10.4, 5.4), (11.2, 8.4), (9.6, 10.0)):
        out += round_ring(cx, cz, 0.85, 0.15, t, t + 1.8, "white", 0.25, "white", n=4)
    return out


# the people at the seats (their name cards): names and their jobs
CREW = [("REYES", "PILOT"), ("OKAFOR", "PILOT"), ("LINDQVIST", "FLIGHT OPS"), ("NAKAMURA", "PILOT"), ("BRANDT", "LOADMASTER"),
        ("SILVA", "DISPATCH"), ("MORROW", "PILOT"), ("HADDAD", "AIRSPACE"), ("PETROV", "MAINTENANCE"), ("ADEYEMI", "PILOT"),
        ("KOWALSKI", "PILOT"), ("DUBOIS", "FLIGHT OPS")]
# a seat's place: (its main thing, its right-hand thing, its drink); the mic live on one (someone speaking)
SEATS = [(("laptop", "silver", "4_0"), "notepad", ("mug", "white")),
         (("laptop", "graphite", "1_0"), "mouse", ("bottle", "navy")),
         (("tablet", "2_0"), "notepad", ("mug", "navy")),
         (("notepad",), "phone", ("bottle", "chrome")),
         (("laptop", "black", "9_0"), "phone", ("mug", "mug_red")),
         (("laptop", "silver", "0_0"), None, ("bottle", "orange")),
         (("shut", "graphite"), "notepad", ("mug", "mug_grey")),
         (("tablet", "5_0"), "phone", ("bottle", "teal")),
         (("laptop", "graphite", "3_0"), "notepad", ("mug", "white")),
         (("laptop", "silver", "2_1"), "mouse", ("bottle", "navy")),
         (("laptop", "silver", "9_1"), "notepad", ("mug", "navy")),
         (("laptop", "black", "5_0"), "notepad", None)]
LIVE = 4
JUG, PACKS, EMPTY, PACK = 12, 13, 14, 15          # the variants between the seats


def tops(A):
    out = []
    for v, (main, right, drink) in enumerate(SEATS):
        items = mic(live=v == LIVE)
        if main[0] == "laptop":
            items += laptop(A, main[1], main[2])
        elif main[0] == "shut":
            items += laptop(A, main[1], None, shut=True)
        elif main[0] == "tablet":
            items += tablet(A, main[1])
        elif main[0] == "notepad":
            items += notepad(A, 100 + v, x0=5.6, z0=6.4, w=4.4, d=6.0)
        else:
            items += folder(A, x0=4.8, z0=5.4, turn=22.5)
        if right == "notepad":
            items += notepad(A, v)
        elif right == "mouse":
            items += mouse()
        elif right == "phone":
            items += phone()
        if drink:
            items += mug(A, drink[1]) if drink[0] == "mug" else bottle(drink[1])
        items += name_card(A, *CREW[v])
        out.append(items)
    out += [jug_tray(), folder(A, x0=1.8, z0=5.4) + folder(A, x0=8.6, z0=6.0, turn=-22.5), [], folder(A, x0=5.2, z0=5.0, turn=22.5)]
    return out


# ------------------------------------------------------------------ the chair
def chair(turn=0.0, back=0.0):
    """A sled-base training chair: the chrome sleds and uprights, the padded seat, the mesh back in its frame, the arms;
    turned `turn` degrees (22.5 steps) and pushed `back` pixels from its desk."""
    z = back
    out = []
    for x in (2.6, 12.8):
        out += [box((x, 0, 1.6 + z), (x + 0.6, 0.6, 14.2 + z), mat("chrome")),                   # the sled on the floor
                box((x, 0.6, 12.8 + z), (x + 0.6, 6.4, 13.4 + z), mat("chrome")),                # its upright at the back
                box((x, 0.6, 2.2 + z), (x + 0.6, 6.4, 2.8 + z), mat("chrome")),                  # and at the front
                box((x, 6.4, 2.2 + z), (x + 0.6, 7.0, 13.4 + z), mat("chrome")),                 # the rail under the seat
                box((x, 7.0, 8.6 + z), (x + 0.6, 11.2, 9.2 + z), mat("chrome")),                 # the arm's post
                box((x - 0.3, 11.2, 5.0 + z), (x + 0.9, 11.8, 10.6 + z), mat("black")),          # the arm pad
                box((x, 7.0, 12.8 + z), (x + 0.6, 18.0, 13.4 + z), mat("chrome"))]               # the back's tube, on up
    out += [box((3.2, 7.0, 2.6 + z), (12.8, 8.6, 12.6 + z), {**mat("fabric"), "down": {"texture": "#black"}}),
            box((3.6, 8.6, 3.0 + z), (12.4, 9.0, 12.2 + z), mat("fabric")),                      # the cushion's crown
            box((3.2, 10.2, 12.9 + z), (12.8, 17.4, 13.3 + z), {"north": {"texture": "#mesh"}, "south": {"texture": "#mesh"}}),
            box((3.2, 17.4, 12.7 + z), (12.8, 18.0, 13.5 + z), mat("chrome")),
            box((3.2, 9.6, 12.7 + z), (12.8, 10.2, 13.5 + z), mat("chrome"))]
    if turn:
        for e in out:
            e["rotation"] = {"angle": turn, "axis": "y", "origin": [8, 0, 8 + back]}
    return out


CHAIRS = [(0, 0), (22.5, 0), (-22.5, 0), (0, 2.0), (22.5, 1.5), (-22.5, 2.5)]


# ------------------------------------------------------------------ the lectern
def lectern_front(img):
    """The lectern's front to the room: an oak panel in a graphite frame, BRIEFING and DRONE OPERATIONS lettered on it
    (the tower's mark over them and the amber light at its foot are modelled)."""
    W, H = img.size
    _brushed_fill(img, BODY["graphite"], 34)
    inner = (10, 10, W - 10, H - 26)
    img.paste(oak().crop((0, 0, inner[2] - inner[0], inner[3] - inner[1])), inner[:2])
    d = ImageDraw.Draw(img)
    cx, cy = W / 2, H * 0.36
    d.text((cx, cy + 46), "BRIEFING", font=ImageFont.truetype(FONT, 17), fill=(44, 36, 28), anchor="mm")
    d.text((cx, cy + 66), "DRONE OPERATIONS", font=ImageFont.truetype(FONT_R, 10), fill=(70, 58, 44), anchor="mm")
    return img


def lectern_panel(page):
    """The touch panel let into the lectern's top: its bezel and the page the presenter works from."""
    def draw(img):
        W, H = img.size
        ImageDraw.Draw(img).rectangle((0, 0, W, H), fill=(12, 12, 14))
        b = round(W / (8.0 * TX) * 5)
        _page_into(img, page, (b, b, W - b, H - b))
        return img
    return draw


def lectern(A):
    """The briefing lectern, its presenter looking north (the room to the north): the plinth; the front panel to the room
    (oak in graphite, BRIEFING lettered on it, the tower's mark standing proud over it, the amber light at its foot);
    the body behind it; the top sloping down to the presenter, the touch panel set in it and the paper stop on its low
    edge, the cheeks under it either side; the presenter's shelf with a glass of water and the clicker; the gooseneck
    microphone on the top's high corner."""
    front = A.region("lectern_front", 10.8, 16.2, lectern_front)
    tilt = {"angle": 22.5, "axis": "x", "origin": [8, 15.2, 7.4]}
    out = [box((2.0, 0, 2.2), (14.0, 0.8, 12.8), mat("graphite")),
           box((2.6, 0.8, 2.6), (13.4, 17.0, 3.0), faces("graphite", north=front)),
           box((2.6, 0.8, 3.0), (13.4, 15.2, 7.4), mat("graphite")),
           box((2.6, 0.8, 7.4), (13.4, 13.0, 12.0), mat("graphite")),
           box((2.2, 15.2, 2.4), (13.8, 16.0, 12.4), mat("graphite"), tilt),
           box((4.0, 16.0, 4.6), (12.0, 16.15, 9.6), faces("black", up=SCR.face("lectern_panel", 8.0, 5.0, lectern_panel("4_0"))), tilt),
           box((2.2, 16.0, 11.6), (13.8, 16.5, 12.4), mat("black"), tilt),
           box((2.2, 12.8, 3.2), (2.5, 15.2, 12.4), mat("graphite"), tilt),
           box((13.5, 12.8, 3.2), (13.8, 15.2, 12.4), mat("graphite"), tilt),
           box((3.2, 11.0, 12.0), (12.8, 11.4, 14.4), mat("graphite")),
           box((3.6, 11.4, 12.6), (4.8, 11.6, 13.2), mat("black"))]
    # the tower's mark (three bars) standing proud of the front, the light along its foot
    for dx in (-1.0, 0.0, 1.0):
        out.append(box((8 + dx - 0.32, 9.8, 2.3), (8 + dx + 0.32, 13.05, 2.6), mat("orange")))
    out.append({**box((3.2, 1.5, 2.4), (12.8, 1.85, 2.6), mat("amber")), "shade": False})
    out += round_ring(11.2, 13.4, 0.6, 0.12, 11.4, 13.0, "white", 1.0, "white", n=4)
    nx, nz = 3.6, 3.6
    bend = {"angle": 22.5, "axis": "x", "origin": [nx, 21.4, nz]}
    out += [box((nx - 0.6, 17.2, nz - 0.6), (nx + 0.6, 17.9, nz + 0.6), mat("black")),
            box((nx - 0.2, 17.9, nz - 0.2), (nx + 0.2, 21.2, nz + 0.2), mat("black")),
            box((nx - 0.3, 21.2, nz - 0.2), (nx + 0.3, 21.6, nz + 3.0), mat("black"), bend),
            box((nx - 0.45, 21.0, nz + 3.0), (nx + 0.45, 21.8, nz + 4.1), mat("rubber"), bend),
            box((nx - 0.5, 20.95, nz + 4.1), (nx + 0.5, 21.85, nz + 4.3), mat("red"), bend)]
    return out


# ------------------------------------------------------------------ the ceiling speaker
def ceiling_speaker():
    """A ceiling tile with the PA speaker let into it: the round grille just under the tile, its rim."""
    return ([box((0, 0, 0), (16, 16, 16), {k: {"texture": "#ceiling", "cullface": k} for k in ("north", "south", "east", "west", "up")}
                 | {"down": {"texture": "#ceiling"}})]
            + round_part(8, 8, 4.4, -0.4, 0, "white", n=11) + round_part(8, 8, 3.9, -0.5, -0.4, "grille", n=11))


# ------------------------------------------------------------------ the AV credenza (3 blocks)
def swap_yz(elements):
    """Turns parts built standing up (round_part: their axis y) to face south (their axis z): y and z swapped."""
    names = {"up": "south", "down": "north", "south": "up", "north": "down", "east": "east", "west": "west"}
    out = []
    for e in elements:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        out.append(box((x0, z0, y0), (x1, z1, y1), {names[k]: v for k, v in e["faces"].items()}))
    return out


def knob(cx, cy, r, z0, z1, tex="graphite"):
    """A round knob on a front panel, facing south."""
    return swap_yz(round_part(cx, cy, r, z0, z1, tex, n=3))


def receiver_face(img):
    """The AV receiver's front: black brushed metal, the display window (the input and the volume in its blue-green
    figures), the tower's mark small at its left (its knobs and keys are modelled)."""
    W, H = img.size
    d = _brushed_fill(img, (30, 31, 34), 36)
    k = W / 12.8
    d.rectangle((3.4 * k, 0.5 * k, 8.6 * k, 1.6 * k), fill=(8, 12, 14), outline=(50, 52, 56))
    f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", round(0.42 * k))
    d.text((3.65 * k, 1.05 * k), "HDMI 1", font=f, fill=(90, 220, 230), anchor="lm")
    d.text((8.35 * k, 1.05 * k), "-22.0 dB", font=f, fill=(90, 220, 230), anchor="rm")
    for dx in (-0.25, 0, 0.25):
        d.rectangle(((1.2 + dx) * k - 0.07 * k, 0.6 * k, (1.2 + dx) * k + 0.07 * k, 1.4 * k), fill=ORANGE)
    return img


def control_face(img):
    """The room's control panel: its bezel, BRIEFING ROOM, the buttons (present, lights, volume, mute)."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(12, 12, 14))
    k = W / 4.2
    d.rectangle((0.15 * k, 0.15 * k, W - 0.15 * k, H - 0.15 * k), fill=(16, 22, 28))
    d.rectangle((0.15 * k, 0.15 * k, W - 0.15 * k, 0.55 * k), fill=(46, 36, 18))
    f = ImageFont.truetype(FONT, round(0.26 * k))
    d.text((0.3 * k, 0.35 * k), "BRIEFING ROOM", font=f, fill=ORANGE, anchor="lm")
    labels = [("PRESENT", (40, 130, 70)), ("LIGHTS 70%", (52, 58, 66)), ("VOLUME", (52, 58, 66)), ("MIC MUTE", (150, 46, 40))]
    for n, (t, c) in enumerate(labels):
        x0 = 0.3 * k + (n % 2) * 1.85 * k
        y0 = 0.8 * k + (n // 2) * 0.95 * k
        d.rounded_rectangle((x0, y0, x0 + 1.7 * k, y0 + 0.8 * k), radius=0.1 * k, fill=c)
        d.text((x0 + 0.85 * k, y0 + 0.4 * k), t, font=ImageFont.truetype(FONT, round(0.2 * k)), fill=(236, 238, 240), anchor="mm")
    return img


def credenza(A):
    """The AV credenza under the big screen, 3 blocks long (built whole in pixels 0..48 along x, then cut into its parts),
    its user looking north at its front (the wall behind it to the north): the graphite carcass on its black plinth, the
    oak top, the louvred doors left and right (their frames, eight slats each, the bar pulls), the middle bay open
    above (the AV receiver with its display, knobs and keys, the switcher with its lights, on a shelf) and a drawer
    below; on the top the soundbar (cloth front, end caps, keys and the lit strip) and the room's control panel."""
    g, blk = "graphite", "black"
    inside = lambda *show: {**mat(g), **{k: {"texture": "#black"} for k in show}}
    out = [box((1.0, 0, 2.4), (47.0, 0.8, 14.0), mat(blk)),
           box((0.4, 0.8, 1.0), (1.4, 10.4, 15.0), inside("east")),
           box((46.6, 0.8, 1.0), (47.6, 10.4, 15.0), inside("west")),
           box((1.4, 0.8, 1.0), (46.6, 10.4, 1.6), inside("south")),
           box((1.4, 0.8, 1.6), (46.6, 1.6, 15.0), inside("up")),
           box((15.4, 1.6, 1.6), (16.6, 10.4, 15.0), inside("east", "west")),
           box((31.4, 1.6, 1.6), (32.6, 10.4, 15.0), inside("east", "west")),
           box((0, 10.4, 0.6), (48, 11.2, 15.6), {**mat("edge"), "up": {"texture": "#oak"}, "down": {"texture": "#black"}})]

    def door(a, b, pull):
        els = [box((a + 0.1, 1.7, 14.4), (a + 1.1, 10.3, 15.2), mat(g)), box((b - 1.1, 1.7, 14.4), (b - 0.1, 10.3, 15.2), mat(g)),
               box((a + 1.1, 1.7, 14.4), (b - 1.1, 2.7, 15.2), mat(g)), box((a + 1.1, 9.3, 14.4), (b - 1.1, 10.3, 15.2), mat(g))]
        for k in range(8):
            yc = 3.15 + k * 0.85
            els.append(box((a + 1.1, yc - 0.4, 14.6), (b - 1.1, yc + 0.4, 14.8), mat(g), {"angle": -45, "axis": "x", "origin": [a, yc, 14.7]}))
        px = b - 2.2 if pull == "right" else a + 2.2
        els += [box((px - 0.2, 4.6, 15.6), (px + 0.2, 7.6, 16.0), mat("chrome")),
                box((px - 0.2, 4.9, 15.2), (px + 0.2, 5.3, 15.6), mat("chrome")), box((px - 0.2, 6.9, 15.2), (px + 0.2, 7.3, 15.6), mat("chrome"))]
        return els
    out += door(1.4, 15.4, "right") + door(32.6, 46.6, "left")
    # the middle bay: the shelf, the receiver and the switcher above it, the drawer below
    out += [box((16.6, 5.2, 1.6), (31.4, 5.6, 15.0), inside("up")),
            box((17.6, 5.6, 3.0), (30.4, 7.8, 14.0), faces(blk, south=SCR.face("receiver", 12.8, 2.2, receiver_face))),
            box((18.4, 8.2, 4.0), (29.6, 9.4, 14.0), mat(g)),
            box((18.0, 7.8, 4.4), (18.8, 8.2, 13.2), mat("rubber")), box((29.2, 7.8, 4.4), (30.0, 8.2, 13.2), mat("rubber"))]
    out += knob(28.6, 6.7, 0.75, 14.0, 14.5) + knob(26.6, 6.7, 0.4, 14.0, 14.35)
    for k in range(4):                                                          # the receiver's keys
        out.append(box((18.6 + k * 0.7, 6.1, 14.0), (19.1 + k * 0.7, 6.4, 14.15), mat("graphite")))
    for k in range(6):                                                          # the switcher's input lights
        out.append({**box((19.2 + k * 1.1, 8.6, 14.0), (19.7 + k * 1.1, 8.95, 14.12), mat("green" if k == 0 else "amber" if k == 3 else "black")),
                    "shade": False})
    out += [box((16.9, 1.8, 14.4), (31.1, 5.0, 15.2), mat(g)),
            box((21.0, 3.2, 15.6), (27.0, 3.6, 16.0), mat("chrome")),
            box((21.4, 3.2, 15.2), (21.8, 3.6, 15.6), mat("chrome")), box((26.2, 3.2, 15.2), (26.6, 3.6, 15.6), mat("chrome"))]
    # the soundbar on the top, centred under the screen
    out += [box((9.0, 11.2, 10.0), (10.0, 11.4, 11.6), mat("rubber")), box((38.0, 11.2, 10.0), (39.0, 11.4, 11.6), mat("rubber")),
            box((8.0, 11.4, 9.2), (40.0, 13.2, 12.4), {**mat(blk), "south": {"texture": "#cloth"}}),
            box((7.6, 11.4, 9.0), (8.0, 13.2, 12.6), mat(g)), box((40.0, 11.4, 9.0), (40.4, 13.2, 12.6), mat(g)),
            {**box((22.5, 13.2, 11.8), (25.5, 13.32, 12.1), mat("amber")), "shade": False}]
    for k in range(3):
        out.append(box((35.4 + k * 0.9, 13.2, 10.2), (36.0 + k * 0.9, 13.35, 10.8), mat("graphite")))
    # the room's control panel on its stand (its left end)
    out += [box((2.0, 11.2, 4.0), (6.2, 11.6, 7.4), mat(g)),
            box((3.6, 11.6, 5.4), (4.6, 12.8, 6.4), mat(g)),
            box((2.0, 11.6, 6.6), (6.2, 14.4, 6.9), faces(blk, south=SCR.face("control", 4.2, 2.8, control_face)),
                {"angle": -22.5, "axis": "x", "origin": [4.1, 11.6, 6.9]})]
    return out


def cut_parts(elements, n=3, w=16):
    """Cuts a model built n blocks long along x into its n part models (each moved to 0..16): an element across a
    part's edge is cut there, the faces on the cut left out (they face each other inside); its UVs carry on (read by
    position, or a picture's share of its width)."""
    parts = [[] for _ in range(n)]
    for e in elements:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        for p in range(n):
            a, b = max(x0, p * w), min(x1, (p + 1) * w)
            if b - a < 1e-6:
                continue
            q = json.loads(json.dumps(e))
            q["from"][0], q["to"][0] = round(a - p * w, 4), round(b - p * w, 4)
            if "rotation" in q:
                q["rotation"]["origin"][0] = round(q["rotation"]["origin"][0] - p * w, 4)
            fc = q["faces"]
            if a > x0 + 1e-6:
                fc.pop("west", None)
            if b < x1 - 1e-6:
                fc.pop("east", None)
            for k in ("north", "south", "up", "down"):
                uv = fc.get(k, {}).get("uv")
                if uv and (a > x0 + 1e-6 or b < x1 - 1e-6):
                    su = (uv[2] - uv[0]) / (x1 - x0)
                    lo, hi = (a - x0, b - x0) if k != "north" else (x1 - b, x1 - a)
                    fc[k]["uv"] = [round(uv[0] + lo * su, 5), uv[1], round(uv[0] + hi * su, 5), uv[3]]
            parts[p].append(q)
    return parts


# ------------------------------------------------------------------ the digital wall clock
def seven_seg(d, x, y, w, h, digit, on, off):
    """Draws one seven-segment digit (its unlit segments dim, as on a real LED clock)."""
    segs = {"a": (0, 0, 1, 0), "b": (1, 0, 1, 1), "c": (1, 1, 1, 2), "d": (0, 2, 1, 2), "e": (0, 1, 0, 2), "f": (0, 0, 0, 1), "g": (0, 1, 1, 1)}
    lit = {"0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd", "6": "afgedc", "7": "abc", "8": "abcdefg", "9": "abcdfg"}[digit]
    t = w * 0.18
    for name, (u0, v0, u1, v1) in segs.items():
        x0, y0, x1, y1 = x + u0 * w, y + v0 * h / 2, x + u1 * w, y + v1 * h / 2
        if u0 == u1:                                                            # a vertical segment
            poly = [(x0, y0 + t * 0.6), (x0 + t / 2, y0 + t * 1.1), (x0 + t / 2, y1 - t * 1.1), (x0, y1 - t * 0.6),
                    (x0 - t / 2, y1 - t * 1.1), (x0 - t / 2, y0 + t * 1.1)]
        else:
            poly = [(x0 + t * 0.6, y0), (x0 + t * 1.1, y0 - t / 2), (x1 - t * 1.1, y0 - t / 2), (x1 - t * 0.6, y0),
                    (x1 - t * 1.1, y0 + t / 2), (x0 + t * 1.1, y0 + t / 2)]
        d.polygon(poly, fill=on if name in lit else off)


def live_clock(img, xs, y, dw, dh, colon, on, off, bg, hours=0):
    """Marks a seven-segment clock's four figures (at xs, y, each dw x dh, as seven_seg draws them) and its colon live:
    the game's time of day (plus hours), drawn as they are."""
    t = dw * 0.18
    x0, x1 = xs[0] - t, xs[-1] + dw + t
    y0, y1 = y - t, y + dh + t
    W, H = x1 - x0, y1 - y0
    live(img, "seg_clock", (x0, y0, x1, y1), hours=hours, on=list(on), off=list(off), bg=list(bg),
         digits=[[round((x - x0) / W, 5), round((y - y0) / H, 5), round(dw / W, 5), round(dh / H, 5)] for x in xs],
         colon=[round((colon[0] - x0) / W, 5), round((colon[1] - x0) / W, 5), round((y + dh * 0.32 - y0) / H, 5), round((y + dh * 0.68 - y0) / H, 5)],
         thick=round(t / W, 5))


def clock_face(time="14:32"):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 6, 6))
        on, off = (255, 58, 36), (44, 14, 12)
        k = W / 12.8
        dw, dh = 1.75 * k, 3.0 * k
        xs = [1.0, 3.35, 6.95, 9.3]
        y = 0.55 * k
        for n, ch in enumerate(time.replace(":", "")):
            seven_seg(d, xs[n] * k + 0.3 * k, y, dw, dh, ch, on, off)
        for cy in (y + dh * 0.32, y + dh * 0.68):
            d.ellipse((6.05 * k, cy - 0.17 * k, 6.39 * k, cy + 0.17 * k), fill=on)
        live_clock(img, [x * k + 0.3 * k for x in xs], y, dw, dh, (6.05 * k, 6.39 * k), on, off, (10, 6, 6))
        d.text((W / 2, H - 0.42 * k), "TOWER TIME", font=ImageFont.truetype(FONT, round(0.42 * k)), fill=(150, 60, 44), anchor="mm")
        return img
    return draw


def digital_clock():
    """The digital wall clock, its face to the north (on a wall to the south): the graphite case, the lip round its smoked
    face, the red figures (the tower's time: live in game)."""
    return [box((1.0, 5.0, 13.6), (15.0, 11.0, 16.0), mat("graphite")),
            box((1.0, 10.4, 13.2), (15.0, 11.0, 13.6), mat("graphite")), box((1.0, 5.0, 13.2), (15.0, 5.6, 13.6), mat("graphite")),
            box((1.0, 5.6, 13.2), (1.6, 10.4, 13.6), mat("graphite")), box((14.4, 5.6, 13.2), (15.0, 10.4, 13.6), mat("graphite")),
            {**box((1.6, 5.6, 13.4), (14.4, 10.4, 13.6), {"north": SCR.face("clock", 12.8, 4.8, clock_face())}), "shade": False}]


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    """Writes the textures, models and blockstates under `assets` (the assets folder of namespace ns). Returns the
    counts of the desk's top variants and the chair's, and the atlas's rows used."""
    global PAGES
    PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    for name, make in MATERIALS.items():
        make().save(tex_dir / f"{name}.png")
    global SCR
    A = Atlas()
    SCR = Atlas(2048, 4 * TX, "screens")
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in MATERIALS}
    T.update({"atlas": ref("td_atlas"), "screens": ref("td_screens"), "particle": ref("td_oak")})

    def model(name, elements, textures=T):
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": textures, "elements": elements}, indent=1) + "\n")
    model("training_desk", desk_body(A))
    model("training_desk_left", desk_end(1.0))
    model("training_desk_right", desk_end(15.0))
    top = tops(A)
    for v, els in enumerate(top):
        if els:
            model(f"training_desk_top_{v}", els)
    A.img.save(tex_dir / "td_atlas.png")
    parts = []
    for f, y in FACINGS.items():
        yy = {"y": y} if y else {}
        parts.append({"when": {"facing": f}, "apply": {"model": ref("training_desk"), **yy}})
        parts.append({"when": {"facing": f, "left": "false"}, "apply": {"model": ref("training_desk_left"), **yy}})
        parts.append({"when": {"facing": f, "right": "false"}, "apply": {"model": ref("training_desk_right"), **yy}})
        for v, els in enumerate(top):
            if els:
                parts.append({"when": {"facing": f, "variant": str(v)}, "apply": {"model": ref(f"training_desk_top_{v}"), **yy}})
    (bs_dir / "training_desk.json").write_text(json.dumps({"multipart": parts}, indent=1) + "\n")
    variants = {}
    for v, (turn, back) in enumerate(CHAIRS):
        model(f"training_chair_{v}", chair(turn, back))
        for f, y in FACINGS.items():
            variants[f"facing={f},variant={v}"] = {"model": ref(f"training_chair_{v}"), **({"y": y} if y else {})}
    (bs_dir / "training_chair.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    model("briefing_lectern", lectern(A))
    (bs_dir / "briefing_lectern.json").write_text(json.dumps({"variants": {
        f"facing={f}": {"model": ref("briefing_lectern"), **({"y": y} if y else {})} for f, y in FACINGS.items()}}, indent=1) + "\n")
    model("ceiling_speaker", ceiling_speaker(), {**T, "ceiling": "jugcraft:block/ceiling_tile"})
    (bs_dir / "ceiling_speaker.json").write_text(json.dumps({"variants": {"": {"model": ref("ceiling_speaker")}}}, indent=1) + "\n")
    variants = {}
    for part, els in enumerate(cut_parts(credenza(A))):
        model(f"av_credenza_{part}", els)
        for f, y in FACINGS.items():
            variants[f"facing={f},part={part}"] = {"model": ref(f"av_credenza_{part}"), **({"y": y} if y else {})}
    (bs_dir / "av_credenza.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    model("digital_wall_clock", digital_clock())
    (bs_dir / "digital_wall_clock.json").write_text(json.dumps({"variants": {
        f"facing={f}": {"model": ref("digital_wall_clock"), **({"y": y} if y else {})} for f, y in FACINGS.items()}}, indent=1) + "\n")
    A.img.save(tex_dir / "td_atlas.png")
    SCR.img.save(tex_dir / "td_screens.png")
    return len(top), len(CHAIRS), A.y + A.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
