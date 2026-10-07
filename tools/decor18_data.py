"""Generated data for Halloween decorations batch 18, the Crypt and the Ossuary (tools/decor18.py): sculpted models on the
toolkit of tools/flora_art.py (each block one texture painted here, 128 x 128 for the detailed props), blockstates, items,
names, loot and tags; and the quads the client draws the moving parts from (assets/jugcraft/decor18_quads.json): the
sarcophagi's lids, the colossal skull's jaw and the gargoyle sentinel's head.

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import math
import random

from PIL import Image

import cute_art as ca
import decor15_data as d15d
import decor17_data as d17d
import decor18 as d18
import flora_art as fa
from flora_art import SIDES4, Px, Sculpt, cube, pal, rotation, shade, solid, strip
from decor6_data import quads

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL6 = ("north", "south", "east", "west", "up", "down")

EBONY = pal("0b0909", "130f0e", "1b1513", "241c18", "2e241e", "3a2d25", "4a3a2e")
IRON = d17d.IRON
BRASS = d17d.BRASS
VELVET = d17d.VELVET
RED = pal("2a0507", "420a0d", "5e1014", "7a181b", "962622", "b03a30")
GOLD = pal("6a4a14", "9a7220", "c49a34", "e4c25a", "f8e294")
BONE = d17d.BONE
SOCKET = d17d.SOCKET
HORN = d17d.HORN
STONES = {"stone_brick": pal("2e2e2d", "3f3f3d", "51514e", "64645f", "777771", "8b8b84", "a0a098"),
          "deepslate": pal("15151a", "1e1e24", "28282f", "33333b", "3f3f48", "4c4c56", "5c5c66"),
          "blackstone": pal("0f0c0f", "171217", "201920", "2a2129", "352a34", "433541", "52424f")}
LICHEN = pal("4e5a2c", "6a7838", "8a9848")
GARGOYLE = pal("23241f", "31332c", "41433a", "53564b", "676a5d", "7c7f70", "929582")
COPPER = pal("1f4a40", "2c6456", "3e7e6c", "58a08a", "7cc0a8")
WOOD = pal("1c120c", "2a1b12", "3a2619", "4c3221", "5e3f2a", "724e34")
EARTH = pal("1e160f", "2c2016", "3c2c1e", "4e3a28")
GLASS = d17d.GLASS
BOOKS = [pal("3a0c0c", "5a1414", "7a2020"), pal("0e2a1a", "184228", "245a38"), pal("101a3a", "1a2a5a", "283e7a"),
         pal("2a1a0c", "422a14", "5a3a1e"), pal("181418", "262026", "342c34"), pal("3a2a0a", "5a4212", "7a5c1c")]


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def faces(uv, sides=ALL6, **over):
    """A box's faces: every side in `sides` drawn with `uv`, and any named one with its own."""
    out = {s: uv for s in sides}
    out.update(over)
    return out


def mirror_x(elements):
    """`elements` mirrored across the block's middle (x -> 16 - x), their turns reversed and their faces swapped."""
    out = []
    for e in elements:
        c = dict(e)
        c["from"] = [round(16 - e["to"][0], 4), e["from"][1], e["from"][2]]
        c["to"] = [round(16 - e["from"][0], 4), e["to"][1], e["to"][2]]
        f = dict(e["faces"])
        if "east" in f or "west" in f:
            east, west = f.pop("east", None), f.pop("west", None)
            if west is not None:
                f["east"] = west
            if east is not None:
                f["west"] = east
        c["faces"] = f
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [round(16 - r["origin"][0], 4), r["origin"][1], r["origin"][2]]
            if r["axis"] in ("y", "z"):
                r["angle"] = -r["angle"]
            c["rotation"] = r
        out.append(c)
    return out


def shifted(elements, dx=0.0, dy=0.0, dz=0.0):
    return fa.transformed(elements, 1.0, (dx, dy, dz), (0, 0, 0))


# ---------------------------------------------------------------- painters

def noise(palette, seed=1, base=None, spread=(0, 0, 0, -1, 1)):
    return d17d.noise(palette, seed, base, spread)


def ebony(seed=1, panel=False, grain=True):
    return d17d.wood(EBONY, seed, grain=grain, panel=panel)


def lining(seed=1, palette=VELVET, border=2):
    """Tufted velvet lining seen from above: diamond tufts with a button at each crossing, a dark wood rim round it."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                u, v = (x + y) % 6, (x - y) % 6
                k = 2 + (1 if u in (2, 3) or v in (2, 3) else 0) - (1 if u == 0 or v == 0 else 0) + (0 if fa.QUIET else rng.choice((0, 0, -1)))
                p.put(x, y, shade(palette, k))
                if u == 0 and v == 0:
                    p.put(x, y, BRASS[3])
        for y in range(p.h):
            for x in range(p.w):
                if x < border or y < border or x >= p.w - border or y >= p.h - border:
                    p.put(x, y, shade(EBONY, 4 if x == 0 or y == 0 else 2))
    return paint


def velvet(palette=RED, seed=1, tufted=True):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                u, v = (x + y) % 5, (x - y) % 5
                k = 2 + (1 if tufted and (u == 2 or v == 2) else 0) + (0 if fa.QUIET else rng.choice((0, 0, -1)))
                p.put(x, y, shade(palette, k))
                if tufted and u == 0 and v == 0:
                    p.put(x, y, GOLD[3])
    return paint


def iron_plate(seed=1, rivets=True):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 3 + (1 if y == 0 or x == 0 else 0) - (1 if y == p.h - 1 or x == p.w - 1 else 0) + (0 if fa.QUIET else rng.choice((0, 0, -1)))
                p.put(x, y, shade(IRON, k))
        if rivets and p.w > 3 and p.h > 3:
            for cx, cy in ((1, 1), (p.w - 2, 1), (1, p.h - 2), (p.w - 2, p.h - 2)):
                p.put(cx, cy, IRON[6])
    return paint


def canal(palette=BONE):
    """A vertebra's end, opaque (5 October 2026: its old ring left the canal and corners see-through): bone, its rim lit
    along the top and shaded along the bottom, the spinal canal a dark round recess in the middle with its lower wall
    catching a little light, and the square corners filled with a step darker bone."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if d > 1.0:
                    c = palette[2]
                elif d >= 0.55:
                    c = palette[4] if y < cy else palette[3]
                elif d >= 0.45:
                    c = ca.SOCKET[2]
                else:
                    c = ca.SOCKET[1] if y > cy + p.h * 0.12 else ca.SOCKET[0]
                p.put(x, y, c)
    return paint


def ring(palette=IRON):
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if 0.55 <= d <= 1.0:
                    p.put(x, y, palette[5] if y < cy else palette[3])
    return paint


def padlock():
    """A heavy iron padlock: a rounded body, a keyhole, rivets."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                k = 4 - (1 if x > p.w * 0.6 else 0) - (1 if y > p.h * 0.75 else 0)
                p.put(x, y, shade(IRON, k))
        cx = p.w // 2
        p.put(cx, p.h * 0.4, SOCKET[0])
        p.put(cx, p.h * 0.4 + 1, SOCKET[0])
        p.put(cx, p.h * 0.4 + 2, SOCKET[1])
        p.put(1, 1, IRON[6])
        p.put(p.w - 2, 1, IRON[6])
    return paint


def stone(palette, seed=1, joints=False, lichen=0.0, tooled=True):
    """Dressed stone, clean: one flat tone with a lit top-left edge and a shaded bottom-right one; ashlar joints if
    `joints`; with `lichen`, neat tufts at fixed places along the bottom. (`seed` and `tooled` are kept for callers.)"""
    def paint(p):
        n = len(palette)
        ca.bevel(palette, n // 2)(p)
        if joints:
            course = max(4, p.h // 3)
            for y in range(0, p.h, course):
                for x in range(p.w):
                    p.put(x, y, palette[1])
                offset = (y // course) % 2 * (p.w // 3)
                for x in range(offset, p.w, max(6, p.w // 2)):
                    for yy in range(y, min(p.h, y + course)):
                        p.put(x, yy, palette[1])
        if lichen:
            step = max(5, int(1.0 / lichen))
            for x in range((seed * 5) % step, p.w, step):
                p.put(x, p.h - 2, LICHEN[1])
                p.put(x + 1, p.h - 2, LICHEN[2])
                p.put(x, p.h - 1, LICHEN[0])
                p.put(x + 1, p.h - 1, LICHEN[1])
    return paint


def arcade(palette, seed=1, arches=4):
    """A carved tomb panel: a frame, and blind pointed arches on little columns, each arch's back in shadow."""
    def paint(p):
        stone(palette, seed)(p)
        n = len(palette)
        w, h = p.w, p.h
        for x in range(w):
            p.put(x, 0, palette[n - 1])
            p.put(x, 1, palette[n - 2])
            p.put(x, h - 1, palette[1])
            p.put(x, h - 2, palette[2])
        span = (w - 4) / arches
        for i in range(arches):
            x0 = 2 + i * span
            x1 = x0 + span
            cx = (x0 + x1) / 2
            for y in range(3, h - 3):
                for x in range(int(x0) + 1, int(x1)):
                    rel = abs(x - cx) / (span / 2 - 1.2)
                    top = 3 + (h - 6) * 0.42 * (rel ** 0.6)
                    if y >= top and rel < 1:
                        k = 1 + (1 if x < cx else 0)
                        p.put(x, y, shade(palette, k))
            # The columns between the arches, lit on their left.
            for y in range(int(h * 0.42), h - 2):
                p.put(x0, y, palette[n - 2])
                p.put(x0 + 1, y, palette[n - 3])
        p.put(w - 2, h // 2, palette[n - 2])
    return paint


def quatrefoil(palette, seed=1):
    """A carved end panel: a frame and a quatrefoil roundel in its middle."""
    def paint(p):
        stone(palette, seed)(p)
        n = len(palette)
        w, h = p.w, p.h
        for x in range(w):
            p.put(x, 0, palette[n - 1])
            p.put(x, h - 1, palette[1])
        cx, cy = (w - 1) / 2, (h - 1) / 2
        r = min(w, h) * 0.32
        for y in range(h):
            for x in range(w):
                best = min(math.hypot(x - (cx + ox * r * 0.5), y - (cy + oy * r * 0.5)) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if best < r * 0.55:
                    p.put(x, y, shade(palette, 1 + (1 if x < cx else 0)))
                elif best < r * 0.7:
                    p.put(x, y, palette[n - 2])
    return paint


def relief(palette, seed=1, kind="plain"):
    """Stone carved in relief, lit from the upper left: folds (`folds`), mail rings (`mail`), a lion's mane (`mane`),
    feathers or scales (`scales`), or plain dressed stone."""
    def paint(p):
        stone(palette, seed, tooled=kind == "plain")(p)
        n = len(palette)
        if kind == "folds":
            for x in range(p.w):
                phase = math.sin(x * 0.9)
                for y in range(p.h):
                    k = n // 2 + (1 if phase > 0.4 else 0) - (1 if phase < -0.5 else 0)
                    p.put(x, y, shade(palette, k))
        elif kind == "mail":
            # Rows of rings: a dark line under each row, lit along its top.
            for y in range(p.h):
                for x in range(p.w):
                    k = n // 2 - (1 if y % 3 == 2 else 0) + (1 if y % 3 == 0 else 0)
                    p.put(x, y, shade(palette, k))
        elif kind == "mane":
            # Long locks: four texels wide, lit down their left edge, a dark parting between.
            for y in range(p.h):
                for x in range(p.w):
                    lock = (x + (y // 6) % 2 * 2) % 4
                    p.put(x, y, shade(palette, n // 2 + (1 if lock == 0 else 0) - (1 if lock == 3 else 0)))
        elif kind == "scales":
            # Big rounded scales in offset rows, each lit at its top with a dark lower curve.
            for y in range(p.h):
                for x in range(p.w):
                    row = y // 4
                    u = (x + row % 2 * 3) % 6 - 2.5
                    v = y % 4
                    edge = v >= 3 - (abs(u) > 1.6)
                    k = n // 2 + (1 if v == 0 and abs(u) < 2 else 0) - (1 if edge else 0)
                    p.put(x, y, shade(palette, k))
    return paint


def effigy_face(palette, kind="helm"):
    """A face carved on a recumbent effigy, seen from above, chin at the bottom: a great helm with its eye slit and
    breaths (`helm`), or a veiled lady's face, eyes shut (`lady`)."""
    def paint(p):
        stone(palette, 31 if kind == "helm" else 32)(p)
        n = len(palette)
        w, h = p.w, p.h
        if kind == "helm":
            for x in range(1, w - 1):
                p.put(x, h * 0.4, palette[0])
                p.put(x, h * 0.4 + 1, palette[n - 2])
            for y in range(int(h * 0.55), h - 1, 2):
                p.put(w * 0.3, y, palette[1])
                p.put(w * 0.7, y, palette[1])
            for y in range(h):
                p.put(w / 2, y, palette[n - 1] if y < h * 0.4 else palette[n - 2])
        else:
            for y in range(h):
                for x in range(w):
                    if x < 2 or x >= w - 2 or y < 2:
                        p.put(x, y, palette[n - 2] if (x + y) % 3 else palette[n - 3])
            for ex in (w * 0.36, w * 0.64):
                p.put(ex, h * 0.45, palette[1])
                p.put(ex + 0.6, h * 0.45, palette[1])
            p.put(w / 2, h * 0.6, palette[n - 1])
            p.put(w / 2, h * 0.62 + 1, palette[2])
            for x in range(int(w * 0.42), int(w * 0.6)):
                p.put(x, h * 0.78, palette[2])
    return paint


def skull_sockets(w, h):
    """Where a human skull's square eye sockets lie on its face picture `w` x `h` texels: [(u0, v0, u1, v1)], whole
    texels, a quarter of the face wide each, a little above the middle."""
    sw, sh = max(2, round(w * 0.25)), max(2, round(h * 0.25))
    v0 = round(h * 0.36)
    return [(round(w * ex - sw / 2), v0, round(w * ex - sw / 2) + sw, v0 + sh) for ex in (0.3, 0.7)]


def human_skull(palette=BONE, seed=1, glow=None):
    """A skull from the front, the Minecraft way (5 October 2026: round, glinting sockets looked goofy): a smooth dome lit
    along its top, two square dark sockets (skull_sockets) each over a lip of shaded bone, no nose holes, and a neat row
    of square teeth along the bottom. Lit (`glow`), each socket is filled flat with the glow: the whole socket when it is
    small (under four texels, as on a 12-texel face, where a rim would leave one lit texel), inside a one-texel dark rim
    when it is bigger."""
    def paint(p):
        ca.soft(palette, len(palette) // 2)(p)
        w, h = p.w, p.h
        for u0, v0, u1, v1 in skull_sockets(w, h):
            if glow and min(u1 - u0, v1 - v0) < 4:
                ca.square_socket(p, u0, v0, u1, v1, lip=palette[1], glow=glow)
                continue
            ca.square_socket(p, u0, v0, u1, v1, lip=palette[1])
            if glow:
                for y in range(v0 + 1, v1 - 1):
                    for x in range(u0 + 1, u1 - 1):
                        p.put(x, y, glow)
        rows = max(1, int(h * 0.14))
        ca.teeth(p, w * 0.26, w * 0.74, h - rows - 1, rows, palette[-1], ca.SOCKET[1], tooth=max(1, w // 8))
    return paint


def teeth(palette=BONE):
    """A neat row of square teeth, lit along the top, a dark gap every third texel."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                gap = x % 3 == 2
                p.put(x, y, ca.SOCKET[1] if gap else palette[-1] if y == 0 else palette[-2])
    return paint


def cracked_bone(seed=1, cracks=3, palette=BONE, moss=0.0):
    """Big bone, clean and smooth: warm cream lit along its top and shaded along its bottom; with `moss`, neat tufts of
    lichen at fixed places along its lower edge. (`seed` and `cracks` are kept for callers.)"""
    def paint(p):
        ca.soft(palette, len(palette) // 2, edge=1, top=0.2, bottom=0.2)(p)
        if moss:
            step = max(4, int(6 / max(0.05, moss) / 4))
            for x in range((seed * 3) % step, p.w, step):
                for dx in (0, 1, 2):
                    p.put(x + dx, p.h - 2, LICHEN[1])
                p.put(x + 1, p.h - 3, LICHEN[2])
                for dx in (0, 1, 2):
                    p.put(x + dx, p.h - 1, LICHEN[0])
    return paint


def vertebrae(seed=1, palette=BONE):
    """A column of vertebrae seen from the side: smooth rounded bodies, lit along one edge, with a warm brown disc
    between each."""
    def paint(p):
        n = len(palette)
        for y in range(p.h):
            disc = y % 5 == 4
            for x in range(p.w):
                k = n // 2 + (1 if x < p.w * 0.35 or y % 5 == 0 else 0) - (1 if x >= p.w * 0.75 else 0)
                p.put(x, y, palette[1] if disc else shade(palette, k))
    return paint


def ribs_texture(seed=1, palette=BONE):
    """A ribcage seen from the front: the sternum down the middle and smooth cream ribs curving off it, each two texels
    thick and lit along its top, soft dark plum between."""
    def paint(p):
        w, h = p.w, p.h
        for y in range(h):
            for x in range(w):
                p.put(x, y, ca.SOCKET[1])
        cx = (w - 1) / 2
        for i in range(max(3, h // 3)):
            y0 = 1 + i * 3
            for x in range(w):
                yy = y0 + abs(x - cx) * 0.25
                if yy < h:
                    p.put(x, yy, palette[4])
                    p.put(x, yy + 1, palette[3])
        for y in range(h):
            p.put(cx, y, palette[4])
            p.put(cx + 1, y, palette[3])
    return paint


def book_spines(seed=1):
    """Book spines side by side: leather in several colours, gilt bands and a title block on each."""
    def paint(p):
        rng = random.Random(seed)
        x = 0
        while x < p.w:
            width = rng.choice((3, 4, 4, 5))
            colour = rng.choice(BOOKS)
            for xx in range(x, min(p.w, x + width)):
                for y in range(p.h):
                    k = 1 + (1 if xx == x else 0) - (1 if xx == x + width - 1 else 0)
                    p.put(xx, y, shade(colour, k))
            for band in (2, p.h - 3):
                for xx in range(x, min(p.w, x + width - 1)):
                    p.put(xx, band, GOLD[3])
            for y in range(int(p.h * 0.35), int(p.h * 0.55)):
                for xx in range(x + 1, min(p.w, x + width - 2)):
                    p.put(xx, y, GOLD[2] if y % 2 else colour[0])
            x += width
    return paint


def book_pages():
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, (226, 214, 180) if y % 2 else (200, 186, 150))
    return paint


def membrane(palette=GARGOYLE, seed=1):
    """A bat's wing in stone: smooth webbing in one flat tone, the finger bones fanning out as clean lit ridges."""
    def paint(p):
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, palette[n // 2 - 1])
        for i in range(4):
            a = 0.2 + i * 0.38
            for s in range(int(max(p.w, p.h) * 1.3)):
                x, y = s * math.cos(a) * 0.8, s * math.sin(a) * 0.9
                p.put(x, y, palette[n - 2])
                p.put(x + 1, y, palette[n - 1])
        p.outline(palette[1])
    return paint


def grotesque(palette=GARGOYLE, seed=1, mouth=False):
    """A gargoyle's face, cute but grumpy: smooth stone, a heavy lit brow, two big round dark eyes each with a glint, no
    nostrils, and a wide mouth (shut, or open) with two little fangs."""
    def paint(p):
        ca.soft(palette, len(palette) // 2)(p)
        n = len(palette)
        w, h = p.w, p.h
        for x in range(1, w - 1):
            p.put(x, h * 0.28, palette[n - 1])
            p.put(x, h * 0.28 + 1, palette[n - 3])
        r = max(1.2, min(w * 0.13, h * 0.16))
        for ex in (0.3, 0.7):
            ca.eye(p, ex * w, h * 0.5, r, r)
        y = int(h * 0.78)
        x0, x1 = int(w * 0.24), int(w * 0.76)
        for x in range(x0, x1):
            p.put(x, y, ca.SOCKET[0])
            if mouth:
                p.put(x, y + 1, ca.SOCKET[1])
        for fx in (0.34, 0.66):
            p.put(fx * w, y, BONE[4])
            if mouth:
                p.put(fx * w, y + 1, BONE[3])
    return paint


def water(seed=1, alpha=150):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                streak = (x * 3 + y) % 7 < 2
                c = (150, 190, 220) if streak else (70, 110, 170)
                p.put(x, y, c, alpha + rng.randrange(-20, 20))
    return paint


def glow_dot(colour, alpha=230):
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if d <= 1.05:
                    c = tuple(min(255, int(v + (255 - v) * max(0.0, 0.6 - d))) for v in colour)
                    p.put(x, y, c, alpha)
    return paint


# ---------------------------------------------------------------- 6. the iron-bound coffin and the wardrobe

# The coffin's outline from above, head toward north: (x0, x1, z0, z1) boxes of each half.
COFFIN_HEAD = [(3.5, 12.5, 1.0, 3.0), (2.0, 14.0, 3.0, 5.5), (1.0, 15.0, 5.5, 16.0)]
COFFIN_FOOT = [(1.5, 14.5, 0.0, 6.0), (2.5, 13.5, 6.0, 11.0), (3.5, 12.5, 11.0, 15.0)]
COFFIN_BODY = 9.0
COFFIN_LID = 2.0
HINGE = {"origin": [15.2, 9.0, 8.0], "axis": "z", "angle": -45}


@fa.quietly
def iron_bound_coffin():
    """The Iron-Bound Coffin's halves, head toward north: a six-sided coffin of black wood, purple velvet inside, under a
    lid with a raised panel; riveted iron bands round body and lid, iron caps on its corners, ring handles on its sides,
    and on the foot end an iron hasp over a staple, with a heavy padlock through it when locked. Open, the lid swings up
    45 degrees on a hinge along its east edge."""
    sc = Sculpt(d18.COFFIN["block"], 181, 128)
    side = sc.piece("side", 48, 18, ebony(2))
    end = sc.piece("end", 16, 18, ebony(3))
    lid_top = sc.piece("lid_top", 48, 28, ebony(4, panel=True))
    lid_edge = sc.piece("lid_edge", 48, 4, ebony(5))
    panel = sc.piece("panel", 36, 20, ebony(6, panel=True))
    lining_ = sc.piece("lining", 40, 28, lining(7))
    under = sc.piece("under", 24, 24, noise(EBONY, 8, 1))
    band = sc.piece("band", 56, 4, d17d.band(9, 10))
    corner = sc.piece("corner", 6, 18, iron_plate(10))
    ring_ = sc.piece("ring", 8, 8, ring())
    hasp = sc.piece("hasp", 6, 10, iron_plate(11))
    lock = sc.piece("padlock", 8, 8, padlock())
    shackle = sc.piece("shackle", 6, 4, ring())
    staple = sc.piece("staple", 4, 4, iron_plate(12, rivets=False))

    def half(outline, bands, head):
        els = []
        for x0, x1, z0, z1 in outline:
            els.append(cube((x0, 0, z0), (x1, COFFIN_BODY, z1), {"east": side, "west": side, "north": end, "south": end,
                                                                "up": lining_, "down": under}))
        lid = []
        for x0, x1, z0, z1 in outline:
            lid.append(cube((x0 - 0.2, COFFIN_BODY, z0 - 0.2), (x1 + 0.2, COFFIN_BODY + COFFIN_LID, z1 + 0.2),
                            {"east": lid_edge, "west": lid_edge, "north": lid_edge, "south": lid_edge, "up": lid_top, "down": lining_}))
        # The raised panel along the lid, narrowing toward the feet.
        if head:
            lid.append(cube((3.0, COFFIN_BODY + COFFIN_LID, 6.5), (13.0, COFFIN_BODY + COFFIN_LID + 0.8, 16.0), faces(panel, ("up", "north", "south", "east", "west"))))
            lid.append(cube((4.2, COFFIN_BODY + COFFIN_LID, 3.0), (11.8, COFFIN_BODY + COFFIN_LID + 0.8, 6.5), faces(panel, ("up", "north", "east", "west"))))
        else:
            lid.append(cube((3.6, COFFIN_BODY + COFFIN_LID, 0.0), (12.4, COFFIN_BODY + COFFIN_LID + 0.8, 9.0), faces(panel, ("up", "north", "south", "east", "west"))))
            lid.append(cube((4.6, COFFIN_BODY + COFFIN_LID, 9.0), (11.4, COFFIN_BODY + COFFIN_LID + 0.8, 13.0), faces(panel, ("up", "south", "east", "west"))))
        # The bands stand 0.1 proud of the rim (the body's) and of the lid's underside (the lid's), so their ends never
        # lie flush with the velvet when the coffin is open.
        for z, (x0, x1) in bands:
            els.append(cube((x0 - 0.35, -0.01, z), (x1 + 0.35, COFFIN_BODY + 0.1, z + 1.0), faces(band, ("north", "south", "east", "west", "up"))))
            lid.append(cube((x0 - 0.55, COFFIN_BODY - 0.1, z), (x1 + 0.55, COFFIN_BODY + COFFIN_LID + 0.35, z + 1.0),
                            faces(band, ("north", "south", "east", "west", "up", "down"))))
        return els, lid

    head_body, head_lid = half(COFFIN_HEAD, [(4.0, (2.0, 14.0)), (12.0, (1.0, 15.0))], True)
    foot_body, foot_lid = half(COFFIN_FOOT, [(3.0, (1.5, 14.5)), (10.0, (2.5, 13.5))], False)
    # Iron caps on the corners of the head and foot ends, and ring handles on the sides.
    for x in (3.0, 11.6):
        head_body.append(cube((x, -0.01, 0.7), (x + 1.4, COFFIN_BODY + 0.1, 2.1), faces(corner, ("north", "east", "west", "south", "up"))))
        foot_body.append(cube((x, -0.01, 13.9), (x + 1.4, COFFIN_BODY + 0.1, 15.3), faces(corner, ("north", "east", "west", "south", "up"))))
    for x in (0.5, 15.0):
        head_body.append(cube((x, 3.5, 8.5), (x + 0.5, 6.5, 11.5), faces(ring_, ("east", "west"))))
        foot_body.append(cube((x + (0.5 if x < 8 else -0.5), 3.5, 1.0), (x + (1.0 if x < 8 else 0.0), 6.5, 4.0), faces(ring_, ("east", "west"))))
    # The hasp on the lid's foot edge (it moves with the lid), the staple it closes over, and the padlock.
    foot_lid.append(cube((6.9, 6.0, 15.1), (9.1, COFFIN_BODY + COFFIN_LID, 15.7), faces(hasp, ALL6)))
    foot_body.append(cube((7.5, 6.6, 15.0), (8.5, 7.6, 16.0), faces(staple, ALL6)))
    padlock_ = [cube((6.6, 2.6, 15.3), (9.4, 5.6, 16.4), faces(lock, ALL6)),
                cube((7.1, 5.6, 15.6), (8.9, 7.4, 16.1), faces(shackle, ("south", "north", "east", "west", "up")))]

    def opened(lid):
        return [dict(e, rotation=HINGE) if "rotation" not in e else e for e in lid]
    sc.models["iron_bound_coffin_head"] = head_body + head_lid
    sc.models["iron_bound_coffin_head_open"] = head_body + opened(head_lid)
    sc.models["iron_bound_coffin_foot"] = foot_body + foot_lid
    sc.models["iron_bound_coffin_foot_open"] = foot_body + opened(foot_lid)
    sc.models["iron_bound_coffin_foot_locked"] = foot_body + foot_lid + padlock_
    sc.models["iron_bound_coffin_foot_open_locked"] = foot_body + opened(foot_lid) + padlock_
    sc.models["item"] = shifted(head_body + head_lid, dz=-0.5) + shifted(foot_body + foot_lid + padlock_, dz=15.5)
    return sc


def key_icon(blank):
    """The Skeleton Key's (or a Key Blank's) item picture: an ornate bow with a skull in it, a long shank, and the
    bit cut in wards (left square on a blank)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    p = Px(img)
    metal = IRON if blank else pal("3a3a40", "55555e", "727280", "9090a0", "b4b4c4", "d4d4e0", "eeeef8")
    cx, cy = 9.5, 9.5
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, y - cy)
            if 4.2 <= d <= 7.2:
                p.put(x, y, metal[5] if (x - cx) + (y - cy) < 0 else metal[3])
    if not blank:
        # The little skull in the bow.
        for y in range(32):
            for x in range(32):
                if math.hypot(x - cx, (y - cy) * 1.1) < 3.2:
                    p.put(x, y, BONE[4])
        p.put(cx - 1, cy, SOCKET[0])
        p.put(cx + 1, cy, SOCKET[0])
        p.put(cx, cy + 2, SOCKET[1])
    for i in range(17):
        x, y = 14 + i * 0.8, 14 + i * 0.8
        for d in (-1, 0, 1):
            p.put(x + d * 0.6, y - d * 0.6, metal[5] if d < 0 else (metal[4] if d == 0 else metal[2]))
    # The bit at the end of the shank.
    for i in range(6):
        bx, by = 22 + i * 0.8, 22 + i * 0.8
        depth = 4 if blank else (3, 5, 2, 4, 5, 3)[i]
        for j in range(depth):
            p.put(bx + j * 0.7, by - j * 0.7 - 1, metal[4] if j < depth - 1 else metal[2])
    return img


# The wardrobe's outline from the front, bottom up: (y0, y1, x0, x1).
WARDROBE = [(0.0, 5.0, 3.0, 13.0), (5.0, 11.0, 2.0, 14.0), (11.0, 17.0, 1.2, 14.8), (17.0, 24.0, 0.5, 15.5),
            (24.0, 28.5, 2.0, 14.0), (28.5, 31.5, 3.5, 12.5)]
WARDROBE_FRONT = 1.6
WARDROBE_BACK = 14.6
# The armour on the mannequin is drawn at this scale, standing on the wardrobe's floor (pixels up).
WARDROBE_SCALE = 0.8
WARDROBE_FLOOR = 1.0


def coffin_wardrobe():
    """The Coffin Wardrobe: an upright six-sided coffin of black wood two blocks tall, lined in purple velvet, with a
    glazed door (a coffin-shaped frame, a glazing bar across the shoulders, iron strap hinges on its left, a brass
    knob on its right) and a carved skull on its crown. Inside on an iron stand is a skeleton mannequin built to the
    armour stand's frame (at WARDROBE_SCALE) so the armour the client hangs on it fits."""
    sc = Sculpt(d18.WARDROBE["block"], 182, 128)
    side = sc.piece("side", 16, 32, ebony(2))
    back = sc.piece("back", 32, 32, ebony(3))
    inner = sc.piece("inner", 30, 30, velvet(VELVET, 4))
    cap = sc.piece("cap", 30, 14, ebony(5))
    frame = sc.piece("frame", 4, 32, ebony(6))
    frame_h = sc.piece("frame_h", 30, 3, ebony(7))
    pane = sc.piece("pane", 28, 30, d17d.glass(56, rim=False))
    strap = sc.piece("strap", 8, 3, iron_plate(8))
    knob = sc.piece("knob", 3, 3, solid(BRASS[2:], 9))
    rod = sc.piece("rod", 2, 16, strip(IRON, 10, light=True))
    plate = sc.piece("plate", 12, 8, iron_plate(11))
    bone_ = sc.piece("bone", 4, 16, d17d.bone(12, 1))
    ribs = sc.piece("ribs", 12, 12, ribs_texture(13))
    spine = sc.piece("spine", 4, 20, vertebrae(14))
    pelvis = sc.piece("pelvis", 12, 6, cracked_bone(15, 1))
    skull_face = sc.piece("skull_face", 12, 12, human_skull(BONE, 16))
    skull_side = sc.piece("skull_side", 12, 12, d17d.bone(17, 1))
    crest = sc.piece("crest", 10, 8, human_skull(BONE, 18))
    els = []
    for y0, y1, x0, x1 in WARDROBE:
        # Walls round the outline, velvet inside, and the back.
        els.append(cube((x0, y0, WARDROBE_BACK - 1.2), (x1, y1, WARDROBE_BACK), {"south": back, "north": inner, "east": side, "west": side}))
        els.append(cube((x0, y0, WARDROBE_FRONT), (x0 + 1.0, y1, WARDROBE_BACK - 1.2), {"west": side, "east": inner, "north": frame}))
        els.append(cube((x1 - 1.0, y0, WARDROBE_FRONT), (x1, y1, WARDROBE_BACK - 1.2), {"east": side, "west": inner, "north": frame}))
    for i in range(len(WARDROBE) - 1):
        # Where the outline steps in or out, a ledge closes the gap.
        y = WARDROBE[i][1]
        a0, a1 = WARDROBE[i][2], WARDROBE[i][3]
        b0, b1 = WARDROBE[i + 1][2], WARDROBE[i + 1][3]
        lo, hi = min(a0, b0), max(a1, b1)
        # The ledge lies on the wider part's side of the step (above it if the part below is wider), so its outer end
        # and back never share a plane with the wider part's wall.
        ly0, ly1 = (y, y + 0.3) if a0 < b0 else (y - 0.3, y)
        els.append(cube((lo, ly0, WARDROBE_FRONT), (lo + abs(a0 - b0) + 1.0, ly1, WARDROBE_BACK), faces(cap, ("up", "down", "north", "west", "south"))))
        ly0, ly1 = (y, y + 0.3) if a1 > b1 else (y - 0.3, y)
        els.append(cube((hi - abs(a1 - b1) - 1.0, ly0, WARDROBE_FRONT), (hi, ly1, WARDROBE_BACK), faces(cap, ("up", "down", "north", "east", "south"))))
    els.append(cube((3.0, 0.0, WARDROBE_FRONT), (13.0, 1.0, WARDROBE_BACK), faces(cap, ("up", "down", "north", "east", "west"))))
    els.append(cube((3.5, 31.5, WARDROBE_FRONT - 0.2), (12.5, 32.0, WARDROBE_BACK), faces(cap, ALL6)))
    # The door: a frame following the outline, a glazing bar at the shoulders, the glass, hinges and a knob.
    for y0, y1, x0, x1 in WARDROBE:
        els.append(cube((x0, y0, WARDROBE_FRONT - 0.6), (x0 + 1.2, y1, WARDROBE_FRONT), faces(frame, ("north", "west", "east", "up", "down"))))
        els.append(cube((x1 - 1.2, y0, WARDROBE_FRONT - 0.6), (x1, y1, WARDROBE_FRONT), faces(frame, ("north", "west", "east", "up", "down"))))
        els.append(fa.plane_xy(x0 + 1.2, x1 - 1.2, y0, y1, WARDROBE_FRONT - 0.3, pane))
    els.append(cube((1.2, 16.6, WARDROBE_FRONT - 0.7), (14.8, 17.6, WARDROBE_FRONT), faces(frame_h, ("north", "up", "down"))))
    els.append(cube((3.0, 0.0, WARDROBE_FRONT - 0.6), (13.0, 1.6, WARDROBE_FRONT), faces(frame_h, ("north", "up"))))
    for y in (4.0, 26.0):
        els.append(cube((0.2, y, WARDROBE_FRONT - 0.8), (4.6, y + 1.2, WARDROBE_FRONT - 0.5), faces(strap, ("north", "up", "down", "west", "east", "south"))))
    els.append(cube((14.0, 14.5, WARDROBE_FRONT - 1.4), (15.1, 15.6, WARDROBE_FRONT - 0.5), faces(knob, ALL6)))
    # The crest: a little skull on the crown.
    els.append(cube((6.4, 28.6, WARDROBE_FRONT - 1.6), (9.6, 31.2, WARDROBE_FRONT - 0.2), {"north": crest, "east": skull_side, "west": skull_side,
                                                                                         "up": skull_side, "down": skull_side}))
    # The mannequin, at the armour stand's frame scaled: feet on a plate, legs, pelvis, spine, ribs, arms and the skull.
    s, f = WARDROBE_SCALE, WARDROBE_FLOOR
    zc = 8.0

    def at(px):
        return f + px * s
    els.append(cube((5.0, f - 0.2, zc - 2.5), (11.0, f + 0.3, zc + 2.5), faces(plate, ("up", "north", "east", "west"))))
    els.append(cube((7.6, f, zc + 1.2), (8.4, at(12.0), zc + 2.0), faces(rod, SIDES4)))
    for x in (6.2, 9.0):
        els.append(cube((x, at(0.5), zc - 0.5), (x + 0.8, at(6.0), zc + 0.5), faces(bone_, SIDES4)))
        els.append(cube((x - 0.1, at(6.0), zc - 0.6), (x + 0.9, at(6.8), zc + 0.6), faces(pelvis, ALL6)))
        els.append(cube((x, at(6.8), zc - 0.5), (x + 0.8, at(11.5), zc + 0.5), faces(bone_, SIDES4)))
        els.append(cube((x - 0.4, f, zc - 1.6), (x + 1.2, f + 0.8, zc + 0.6), faces(bone_, ("up", "north", "east", "west"))))
    els.append(cube((5.4, at(11.0), zc - 1.2), (10.6, at(13.4), zc + 1.0), faces(pelvis, ALL6)))
    els.append(cube((7.5, at(13.4), zc - 0.2), (8.5, at(24.0), zc + 0.8), faces(spine, SIDES4)))
    els.append(cube((5.2, at(15.5), zc - 1.6), (10.8, at(23.0), zc + 0.9), {"north": ribs, "south": ribs, "east": ribs, "west": ribs, "up": pelvis}))
    els.append(cube((4.6, at(22.6), zc - 0.6), (11.4, at(23.6), zc + 0.4), faces(bone_, ALL6)))
    for x in (3.4, 11.8):
        els.append(cube((x, at(14.0), zc - 0.5), (x + 0.8, at(23.0), zc + 0.5), faces(bone_, SIDES4)))
        els.append(cube((x - 0.1, at(11.5), zc - 0.6), (x + 0.9, at(14.0), zc + 0.6), faces(pelvis, ALL6)))
    els.append(cube((7.6, at(23.6), zc - 0.3), (8.4, at(24.4), zc + 0.5), faces(spine, SIDES4)))
    sk0, sk1 = at(24.4), at(31.4)
    els.append(cube((5.4, sk0 + 0.8, zc - 2.4), (10.6, sk1, zc + 2.4), {"north": skull_face, "east": skull_side, "west": skull_side,
                                                                      "up": skull_side, "south": skull_side, "down": skull_side}))
    els.append(cube((6.2, sk0, zc - 2.3), (9.8, sk0 + 0.8, zc - 0.4), faces(skull_side, ("north", "east", "west", "down"))))
    sc.models[d18.WARDROBE["block"]] = els
    return sc


# ---------------------------------------------------------------- 7. the stone sarcophagi

def sarcophagus(stone_):
    """A Stone Sarcophagus, head toward north, drawn whole from its head half (its foot half the next block south):
    a moulded plinth, a hollow tomb-chest with corner pilasters, blind arcading down each long side and a skull boss
    standing off its middle, a quatrefoil on each end, a cornice round its rim, and a skeleton laid out within. Its lids
    (one for each carving in d18.LIDS) are the client's (decor18_quads.json)."""
    palette = STONES[stone_]
    block = f"{stone_}_sarcophagus"
    sc = Sculpt(block, {"stone_brick": 183, "deepslate": 184, "blackstone": 185}[stone_], 128)
    plinth = sc.piece("plinth", 32, 6, stone(palette, 2, lichen=0.04 if stone_ == "stone_brick" else 0))
    top = sc.piece("top", 24, 24, stone(palette, 3))
    side = sc.piece("side", 48, 16, arcade(palette, 4))
    end = sc.piece("end", 20, 18, quatrefoil(palette, 5))
    pilaster = sc.piece("pilaster", 4, 18, strip(palette, 6, light=True))
    cornice = sc.piece("cornice", 40, 3, relief(palette, 7))
    inside = sc.piece("inside", 16, 20, noise([palette[0], palette[1], palette[2]], 8, 1))
    boss_face = sc.piece("boss_face", 10, 10, human_skull(palette[2:], 9))
    boss_side = sc.piece("boss_side", 8, 10, stone(palette, 10))
    bone_ = sc.piece("bone", 4, 16, d17d.bone(11, 1))
    ribs = sc.piece("ribs", 12, 12, ribs_texture(12))
    spine = sc.piece("spine", 4, 20, vertebrae(13))
    pelvis = sc.piece("pelvis", 12, 6, cracked_bone(14, 1))
    skull_face = sc.piece("skull_face", 12, 12, human_skull(BONE, 15))
    skull_side = sc.piece("skull_side", 12, 12, d17d.bone(16, 1))
    shroud = sc.piece("shroud", 12, 12, noise(pal("4a4236", "5e5444", "72664f", "877a5f"), 17, 2))
    els = [cube((0.5, 0.0, 0.5), (15.5, 1.8, 31.5), faces(plinth, ALL6, up=top)),
           cube((1.0, 1.8, 1.0), (15.0, 2.6, 31.0), faces(plinth, ALL6, up=top)),
           cube((1.6, 2.6, 1.6), (3.0, 10.2, 30.4), {"west": side, "east": inside, "north": end, "south": end, "up": top}),
           cube((13.0, 2.6, 1.6), (14.4, 10.2, 30.4), {"east": side, "west": inside, "north": end, "south": end, "up": top}),
           cube((3.0, 2.6, 1.6), (13.0, 10.2, 3.0), {"north": end, "south": inside, "up": top}),
           cube((3.0, 2.6, 29.0), (13.0, 10.2, 30.4), {"south": end, "north": inside, "up": top}),
           cube((3.0, 2.6, 3.0), (13.0, 3.4, 29.0), {"up": inside})]
    # The cornice round the rim.
    for lo, hi in (((1.1, 10.2, 1.1), (3.0, 11.0, 30.9)), ((13.0, 10.2, 1.1), (14.9, 11.0, 30.9)),
                   ((3.0, 10.2, 1.1), (13.0, 11.0, 3.0)), ((3.0, 10.2, 29.0), (13.0, 11.0, 30.9))):
        els.append(cube(lo, hi, faces(cornice, ALL6, down=inside)))
    for x, z in ((1.2, 1.2), (13.6, 1.2), (1.2, 29.6), (13.6, 29.6)):
        els.append(cube((x, 2.6, z), (x + 1.2, 10.2, z + 1.2), faces(pilaster, SIDES4)))
    # The skull bosses on the long sides.
    for x0, x1, face in ((0.6, 1.6, "west"), (14.4, 15.4, "east")):
        els.append(cube((x0, 4.4, 13.6), (x1, 8.6, 18.4), {face: boss_face, "north": boss_side, "south": boss_side, "up": boss_side, "down": boss_side}))
    # The skeleton laid out within, under the rags of its shroud.
    y = 3.4
    els += [cube((6.0, y, 3.8), (10.0, y + 3.4, 8.2), {"up": skull_face, "north": skull_side, "east": skull_side, "west": skull_side, "south": skull_side}),
            cube((6.6, y, 8.2), (9.4, y + 1.2, 9.6), faces(skull_side, ("up", "east", "west", "south"))),
            cube((7.5, y, 9.6), (8.5, y + 1.0, 19.0), faces(spine, ("up", "east", "west"))),
            cube((5.4, y, 10.0), (10.6, y + 2.6, 16.0), {"up": ribs, "east": ribs, "west": ribs, "north": ribs, "south": ribs}),
            cube((5.4, y, 17.0), (10.6, y + 1.4, 19.6), faces(pelvis, ("up", "north", "south", "east", "west"))),
            cube((5.8, y, 19.6), (6.8, y + 1.0, 24.4), faces(bone_, ("up", "east", "west"))),
            cube((9.2, y, 19.6), (10.2, y + 1.0, 24.4), faces(bone_, ("up", "east", "west"))),
            cube((5.9, y, 24.4), (6.7, y + 0.9, 28.4), faces(bone_, ("up", "east", "west", "south"))),
            cube((9.3, y, 24.4), (10.1, y + 0.9, 28.4), faces(bone_, ("up", "east", "west", "south"))),
            cube((4.2, y, 10.4), (5.0, y + 0.9, 15.6), faces(bone_, ("up", "east", "west"))),
            cube((11.0, y, 10.4), (11.8, y + 0.9, 15.6), faces(bone_, ("up", "east", "west"))),
            cube((4.4, y, 15.6), (5.2, y + 0.8, 19.8), faces(bone_, ("up", "east", "west", "south"))),
            cube((10.8, y, 15.6), (11.6, y + 0.8, 19.8), faces(bone_, ("up", "east", "west", "south"))),
            fa.plane_xz(4.0, 12.0, 21.0, 28.8, y + 1.3, shroud)]
    sc.models[block] = els
    for kind in d18.LIDS:
        sc.models[f"lid_{kind}"] = sarcophagus_lid(sc, palette, kind)
    sc.models["item"] = els + sc.models["lid_knight"]
    return sc


LID_Y = 11.0
LID_T = 1.8


def sarcophagus_lid(sc, palette, kind):
    """A sarcophagus's lid, shut on its chest (the renderer slides and tilts it when it opens): a heavy slab with a
    raised border, and on it `kind`: plain and coped (`plain`, as in the picture), a knight lying with his sword on his
    breast and a lion at his feet (`knight`), a lady with folded hands and a lily and a little dog at her feet (`lady`),
    or a skull and crossed bones (`skull`)."""
    slab = sc.piece("slab", 40, 12, stone(palette, 20))
    edge = sc.piece("edge", 40, 3, relief(palette, 21))
    border = sc.piece("border", 40, 2, strip(palette, 22, light=True, horizontal=True))
    y0, y1 = LID_Y, LID_Y + LID_T
    els = [cube((0.8, y0, 0.8), (15.2, y1, 31.2), faces(edge, SIDES4, up=slab, down=slab))]
    for lo, hi in (((0.8, y1, 0.8), (2.0, y1 + 0.6, 31.2)), ((14.0, y1, 0.8), (15.2, y1 + 0.6, 31.2)),
                   ((2.0, y1, 0.8), (14.0, y1 + 0.6, 2.0)), ((2.0, y1, 30.0), (14.0, y1 + 0.6, 31.2))):
        els.append(cube(lo, hi, faces(border, ALL6)))
    if kind == "plain":
        coping = sc.piece("coping", 20, 20, stone(palette, 23))
        els.append(cube((2.0, y1, 2.0), (8.2, y1 + 0.9, 30.0), faces(coping, ("up", "north", "south", "west")), rotation((8.0, y1, 16.0), "z", 22.5)))
        els.append(cube((7.8, y1, 2.0), (14.0, y1 + 0.9, 30.0), faces(coping, ("up", "north", "south", "east")), rotation((8.0, y1, 16.0), "z", -22.5)))
        els.append(cube((7.4, y1 + 1.6, 2.0), (8.6, y1 + 2.4, 30.0), faces(border, ALL6)))
        return els
    pillow = sc.piece("pillow", 12, 8, relief(palette, 24, "folds"))
    els.append(cube((4.8, y1, 2.4), (11.2, y1 + 1.4, 6.6), faces(pillow, ALL6)))
    for x in (4.5, 10.9):
        for z in (2.1, 6.0):
            els.append(cube((x, y1, z), (x + 0.6, y1 + 0.9, z + 0.6), faces(pillow, ALL6)))
    if kind == "knight":
        helm = sc.piece("helm", 8, 8, effigy_face(palette, "helm"))
        mail = sc.piece("mail", 16, 16, relief(palette, 25, "mail"))
        surcoat = sc.piece("surcoat", 12, 18, relief(palette, 26, "folds"))
        steel = sc.piece("steel", 4, 20, strip(palette, 27, light=True))
        mane = sc.piece("mane", 12, 10, relief(palette, 28, "mane"))
        els += [cube((6.2, y1 + 0.6, 3.6), (9.8, y1 + 3.6, 7.8), faces(mail, ALL6, up=helm)),
                cube((4.4, y1, 7.6), (11.6, y1 + 2.6, 18.0), faces(mail, ALL6, up=surcoat)),
                cube((4.8, y1, 18.0), (11.2, y1 + 2.0, 21.0), faces(surcoat, ALL6)),
                cube((5.0, y1, 21.0), (7.8, y1 + 1.8, 27.0), faces(mail, ALL6)),
                cube((8.2, y1, 21.0), (11.0, y1 + 1.8, 27.0), faces(mail, ALL6)),
                cube((5.2, y1, 27.0), (7.6, y1 + 2.6, 28.6), faces(steel, ALL6)),
                cube((8.4, y1, 27.0), (10.8, y1 + 2.6, 28.6), faces(steel, ALL6)),
                # The sword down his breast: blade, crossguard, grip and pommel, and his hands on the grip.
                cube((7.4, y1 + 2.6, 11.4), (8.6, y1 + 3.0, 25.0), faces(steel, ALL6)),
                cube((5.6, y1 + 2.6, 10.6), (10.4, y1 + 3.3, 11.6), faces(steel, ALL6)),
                cube((7.5, y1 + 2.6, 8.6), (8.5, y1 + 3.4, 10.6), faces(steel, ALL6)),
                cube((7.2, y1 + 2.6, 8.0), (8.8, y1 + 3.6, 8.8), faces(steel, ALL6)),
                cube((6.4, y1 + 2.6, 9.0), (9.6, y1 + 3.8, 10.4), faces(mail, ALL6)),
                # The lion at his feet.
                cube((5.0, y1, 28.6), (11.0, y1 + 2.4, 30.4), faces(mane, ALL6)),
                cube((9.0, y1 + 0.8, 28.2), (11.4, y1 + 3.4, 30.2), faces(mane, ALL6))]
    elif kind == "lady":
        face = sc.piece("face", 8, 8, effigy_face(palette, "lady"))
        gown = sc.piece("gown", 12, 22, relief(palette, 29, "folds"))
        veil = sc.piece("veil", 10, 10, relief(palette, 30, "folds"))
        lily = sc.piece("lily", 6, 6, relief(palette, 31))
        els += [cube((6.2, y1 + 0.6, 3.4), (9.8, y1 + 3.2, 7.4), faces(veil, ALL6, up=face)),
                cube((5.8, y1 + 0.4, 3.0), (10.2, y1 + 2.4, 8.0), faces(veil, ("east", "west", "north", "down"))),
                cube((4.8, y1, 7.4), (11.2, y1 + 2.4, 16.0), faces(gown, ALL6)),
                cube((4.4, y1, 16.0), (11.6, y1 + 2.1, 27.6), faces(gown, ALL6)),
                # Her hands together in prayer, the lily held in them and lying down her gown.
                cube((7.1, y1 + 2.4, 9.8), (8.9, y1 + 4.2, 11.6), faces(gown, ALL6)),
                cube((7.8, y1 + 2.4, 11.6), (8.2, y1 + 2.8, 18.0), faces(lily, ALL6)),
                cube((6.8, y1 + 2.2, 17.8), (9.2, y1 + 3.2, 20.4), faces(lily, ALL6)),
                # The little dog at her feet.
                cube((6.0, y1, 27.6), (10.0, y1 + 1.8, 30.0), faces(veil, ALL6)),
                cube((6.6, y1 + 1.0, 27.4), (8.4, y1 + 2.6, 29.0), faces(veil, ALL6))]
    else:
        face = sc.piece("relief_skull", 10, 10, human_skull(palette[2:], 32))
        bone_ = sc.piece("relief_bone", 4, 16, relief(palette, 33))
        knob = sc.piece("relief_knob", 4, 4, relief(palette, 34))
        els += [cube((5.2, y1, 8.4), (10.8, y1 + 2.8, 14.6), faces(bone_, ALL6, up=face))]
        for angle in (45, -45):
            r = rotation((8.0, y1, 20.0), "y", angle)
            els.append(cube((7.3, y1, 14.0), (8.7, y1 + 1.3, 26.0), faces(bone_, ALL6), r))
            for z in (13.4, 25.0):
                els.append(cube((6.9, y1, z), (9.1, y1 + 1.6, z + 1.6), faces(knob, ALL6), r))
    return els


# ---------------------------------------------------------------- 8. the ossuary parlour

def long_bone(lo, hi, shaft, knob, axis="y", rot=None, knob_size=0.7):
    """A long bone along `axis` between `lo` and `hi`, a knobbed end at each, turned by `rot`."""
    out = [cube(lo, hi, faces(shaft, ALL6), rot)]
    k = knob_size
    if axis == "y":
        for y0, y1 in ((lo[1] - 0.2, lo[1] + 1.4), (hi[1] - 1.4, hi[1] + 0.2)):
            out.append(cube((lo[0] - k, y0, lo[2] - k * 0.6), (hi[0] + k, y1, hi[2] + k * 0.6), faces(knob, ALL6), rot))
    elif axis == "z":
        for z0, z1 in ((lo[2] - 0.2, lo[2] + 1.4), (hi[2] - 1.4, hi[2] + 0.2)):
            out.append(cube((lo[0] - k, lo[1] - k * 0.5, z0), (hi[0] + k, hi[1] + k * 0.6, z1), faces(knob, ALL6), rot))
    else:
        for x0, x1 in ((lo[0] - 0.2, lo[0] + 1.4), (hi[0] - 1.4, hi[0] + 0.2)):
            out.append(cube((x0, lo[1] - k * 0.5, lo[2] - k), (x1, hi[1] + k * 0.6, hi[2] + k), faces(knob, ALL6), rot))
    return out


def skull_at(sc, x0, y0, z0, w, h, d, face="north", glow=None, prefix="skull"):
    """A human skull in the box from (x0, y0, z0), w x h x d, its face toward `face` (north or up), and its jaw."""
    front = sc.piece(f"{prefix}_face", 12, 12, human_skull(BONE, 41))
    side = sc.piece(f"{prefix}_side", 12, 12, d17d.bone(42, 1))
    out = [cube((x0, y0 + h * 0.18, z0), (x0 + w, y0 + h, z0 + d), {face: front, **{s: side for s in ALL6 if s != face}})]
    out.append(cube((x0 + w * 0.18, y0, z0 + 0.05), (x0 + w * 0.82, y0 + h * 0.2, z0 + d * 0.55), faces(side, ("north", "east", "west", "down"))))
    if glow:
        # A flat glowing square exactly over each socket (a north face's picture runs from x0 + w on its left), 0.1 in front.
        top, tall = y0 + h, h * 0.82
        for u0, v0, u1, v1 in skull_sockets(12, 12):
            out.append(cube((x0 + w * (1 - u1 / 12), top - tall * v1 / 12, z0 - 0.1), (x0 + w * (1 - u0 / 12), top - tall * v0 / 12, z0 - 0.1),
                            {"north": glow}, light=15))
    return out


def bone_throne():
    """The Bone Throne, facing north, drawn whole from its lower half: a seat on four femurs with crossed bones under
    its front, a tufted red velvet cushion, femur armrests on humerus posts ending in skulls, and behind a velvet back
    under a fan of ribs off a spine, crowned by a horned skull whose sockets glow red while someone sits at night."""
    sc = Sculpt(d18.THRONE["block"], 186, 128)
    shaft = sc.piece("shaft", 4, 16, d17d.bone(2, 1))
    knob = sc.piece("knob", 6, 6, cracked_bone(3, 0))
    seat = sc.piece("seat", 28, 28, cracked_bone(4, 2))
    cushion = sc.piece("cushion", 26, 26, velvet(RED, 5))
    cushion_side = sc.piece("cushion_side", 26, 4, velvet(RED, 6, tufted=False))
    back = sc.piece("back", 24, 32, velvet(RED, 7))
    spine = sc.piece("spine", 4, 24, vertebrae(8))
    rib = sc.piece("rib", 12, 3, strip(BONE, 9, light=True, horizontal=True))
    horn = sc.piece("horn", 10, 26, d17d.horn(10))
    eye = sc.piece("eye", 4, 4, ca.bevel(pal("b81c16", "dc2a20", "f4523a"), 1, light=0, dark=0))
    els = []
    for x in (2.2, 12.0):
        for z in (2.4, 12.2):
            els += long_bone((x, 0.0, z), (x + 1.8, 7.4, z + 1.8), shaft, knob)
    els += [cube((2.4, 6.4, 2.6), (13.6, 8.0, 14.0), faces(seat, ALL6)),
            cube((3.0, 8.0, 3.0), (13.0, 9.6, 13.4), faces(cushion_side, SIDES4, up=cushion))]
    for angle in (45, -45):
        els += [cube((3.0, 3.2, 2.4), (13.0, 4.4, 3.4), faces(shaft, ALL6), rotation((8.0, 3.8, 2.9), "z", angle))]
    # Armrests: humerus posts, femur rails, skulls at their ends.
    for x in (1.8, 12.6):
        els += long_bone((x + 0.1, 8.0, 3.2), (x + 1.5, 13.2, 4.6), shaft, knob, knob_size=0.4)
        els += long_bone((x, 13.0, 3.4), (x + 1.6, 14.4, 13.4), shaft, knob, axis="z", knob_size=0.4)
        els += skull_at(sc, x - 0.5, 13.6, 1.4, 2.6, 2.6, 2.6, prefix="arm_skull")
    # The back: velvet, a spine, ribs fanning off it, and the horned skull on top.
    els.append(cube((2.6, 9.6, 13.8), (13.4, 27.0, 14.8), faces(back, ALL6)))
    els.append(cube((7.2, 8.0, 13.0), (8.8, 28.0, 14.0), faces(spine, SIDES4)))
    for i, y in enumerate((11.0, 14.5, 18.0, 21.5, 25.0)):
        reach = 5.6 - abs(i - 2) * 0.6
        for sign in (-1, 1):
            if sign < 0:
                x0, x1 = 8.0 - reach, 7.2
            else:
                x0, x1 = 8.8, 8.0 + reach
            els.append(cube((x0, y, 12.9), (x1, y + 0.9, 13.6), faces(rib, ALL6), rotation((8.0, y, 13.2), "z", 22.5 * sign)))
    crest_dark = skull_at(sc, 5.4, 27.0, 11.6, 5.2, 5.0, 4.2, prefix="crest")
    crest_lit = skull_at(sc, 5.4, 27.0, 11.6, 5.2, 5.0, 4.2, glow=eye, prefix="crest")
    for sign in (-1, 1):
        def mx(a, b):
            return (a, b) if sign < 0 else (16 - b, 16 - a)
        x0, x1 = mx(3.4, 5.6)
        els.append(cube((x0, 29.6, 12.2), (x1, 31.6, 14.6), faces(horn, ALL6), rotation(((x0 + x1) / 2, 30.6, 13.4), "z", -22.5 * sign)))
        x0, x1 = mx(2.0, 3.8)
        els.append(cube((x0, 27.4, 12.0), (x1, 30.0, 14.4), faces(horn, ALL6), rotation(((x0 + x1) / 2, 28.7, 13.2), "z", 22.5 * sign)))
        x0, x1 = mx(1.6, 3.2)
        els.append(cube((x0, 25.6, 11.4), (x1, 27.8, 13.6), faces(horn, ALL6), rotation(((x0 + x1) / 2, 27.8, 12.6), "x", 22.5)))
    sc.models["bone_throne"] = els + crest_dark
    sc.models["bone_throne_lit"] = els + crest_lit
    return sc


# Where the Ribcage Bookcase's six places are (a chiseled bookshelf's: top row 0-2, then 3-5, each row from the
# viewer's left, which for a block facing north is its east side): x0, x1, y0 of each place, in pixels.
BOOK_PLACES = [(10.6, 14.6, 8.4), (6.0, 10.0, 8.4), (1.4, 5.4, 8.4), (10.6, 14.6, 1.6), (6.0, 10.0, 1.6), (1.4, 5.4, 1.6)]


@fa.quietly
def ribcage_bookcase():
    """The Ribcage Bookcase, facing north: a dark plank plinth and cap, a spine up the back and three pairs of ribs
    curving round the sides to the front, a femur and a board for the shelf between its two rows. Each place's books
    are a model of their own (`ribcage_bookcase_slot_N`), shown while the place holds a book."""
    sc = Sculpt(d18.BOOKCASE["block"], 187, 128)
    plank = sc.piece("plank", 30, 6, d17d.wood(WOOD, 2, panel=False))
    plank_top = sc.piece("plank_top", 30, 28, d17d.wood(WOOD, 3, panel=True))
    spine = sc.piece("spine", 4, 26, vertebrae(4))
    rib = sc.piece("rib", 14, 3, strip(BONE, 5, light=True, horizontal=True))
    shaft = sc.piece("shaft", 4, 16, d17d.bone(6, 1))
    knob = sc.piece("knob", 6, 6, cracked_bone(7, 0))
    shelf = sc.piece("shelf", 28, 20, d17d.wood(WOOD, 8, panel=False))
    back = sc.piece("back", 28, 26, noise(EBONY, 9, 2))
    spines = sc.piece("spines", 24, 12, book_spines(10))
    pages = sc.piece("pages", 12, 8, book_pages())
    els = [cube((0.5, 0.0, 1.0), (15.5, 1.6, 15.0), faces(plank, SIDES4, up=plank_top, down=plank_top)),
           cube((0.5, 14.6, 1.0), (15.5, 16.0, 15.0), faces(plank, SIDES4, up=plank_top, down=plank_top)),
           cube((1.0, 1.6, 14.0), (15.0, 14.6, 14.8), faces(back, ("north", "south"), east=plank, west=plank)),
           cube((6.8, 1.6, 12.6), (9.2, 14.6, 14.6), faces(spine, SIDES4)),
           cube((1.0, 7.6, 3.6), (15.0, 8.2, 13.0), faces(shelf, ALL6))]
    els += long_bone((1.2, 7.4, 2.8), (14.8, 8.4, 3.8), shaft, knob, axis="x", knob_size=0.5)
    for y in (4.0, 9.8, 12.6):
        for sign in (-1, 1):
            # Each rib: along the back from the spine, down the side, and round the front corner.
            parts = [((1.6, y, 13.0), (6.8, y + 1.0, 14.0), None),
                     ((0.8, y, 4.4), (1.8, y + 1.0, 13.6), None),
                     ((0.9, y, 2.4), (4.6, y + 1.0, 3.4), rotation((1.4, y, 3.4), "y", 45))]
            for lo, hi, r in parts:
                e = cube(lo, hi, faces(rib, ALL6), r)
                els.append(e if sign < 0 else mirror_x([e])[0])
    sc.models[d18.BOOKCASE["block"]] = els
    rng = random.Random(11)
    for slot, (x0, x1, y0) in enumerate(BOOK_PLACES):
        books = []
        x = x0
        while x < x1 - 0.6:
            width = min(x1 - x, rng.choice((1.1, 1.3, 1.5)))
            height = rng.choice((4.6, 5.0, 5.4, 5.8))
            colour = sc.piece(f"book_{slot}_{len(books)}", 6, 12, book_spines(20 + slot * 7 + len(books)))
            books.append(cube((x, y0, 4.2), (x + width, y0 + height, 12.6), {"north": colour, "south": colour, "up": pages, "east": pages, "west": pages}))
            x += width + 0.05
        sc.models[f"ribcage_bookcase_slot_{slot}"] = books
    sc.models["item"] = els + [e for slot in range(6) for e in sc.models[f"ribcage_bookcase_slot_{slot}"]]
    return sc


def skull_footstool():
    """The Skull Footstool, facing north: two femurs crossed on the floor, a big skull resting on them, and a tufted red
    velvet cushion on its crown with a gold tassel at each corner."""
    sc = Sculpt(d18.FOOTSTOOL["block"], 188, 64)
    shaft = sc.piece("shaft", 4, 16, d17d.bone(2, 1))
    knob = sc.piece("knob", 5, 5, cracked_bone(3, 0))
    cushion = sc.piece("cushion", 20, 20, velvet(RED, 4))
    cushion_side = sc.piece("cushion_side", 20, 3, velvet(RED, 5, tufted=False))
    tassel = sc.piece("tassel", 2, 3, solid(GOLD[1:], 6))
    els = []
    for angle in (45, -45):
        r = rotation((8.0, 0.8, 8.0), "y", angle)
        els += long_bone((7.2, 0.0, 1.6), (8.8, 1.6, 14.4), shaft, knob, axis="z", rot=r, knob_size=0.6)
    els += skull_at(sc, 4.6, 1.4, 4.2, 6.8, 5.8, 7.4)
    els.append(cube((3.6, 7.0, 3.6), (12.4, 8.4, 12.4), faces(cushion_side, SIDES4, up=cushion, down=cushion)))
    for x in (3.2, 12.2):
        for z in (3.2, 12.2):
            els.append(cube((x, 6.0, z), (x + 0.6, 7.2, z + 0.6), faces(tassel, ALL6)))
    sc.models[d18.FOOTSTOOL["block"]] = els
    return sc


def vertebra_floor_lamp():
    """The Vertebra Floor Lamp, facing north, drawn whole from its lower half: a pelvis for a foot, a column of nine
    vertebrae with their spines and wings, and at the top a skull for a shade whose sockets and nose glow amber when
    it is lit."""
    sc = Sculpt(d18.LAMP["block"], 189, 64)
    pelvis = sc.piece("pelvis", 20, 8, cracked_bone(2, 1))
    body = sc.piece("body", 6, 6, cracked_bone(3, 0))
    disc = sc.piece("disc", 6, 2, solid(SOCKET[1:] + [BONE[1]], 4))
    process = sc.piece("process", 4, 4, d17d.bone(5, 0))
    glow = sc.piece("glow", 4, 4, glow_dot((255, 190, 90)))
    els = [cube((3.0, 0.0, 4.6), (13.0, 1.6, 11.4), faces(pelvis, ALL6)),
           cube((3.4, 1.6, 6.0), (5.8, 4.4, 10.0), faces(pelvis, ALL6), rotation((4.6, 1.6, 8.0), "z", 22.5)),
           cube((10.2, 1.6, 6.0), (12.6, 4.4, 10.0), faces(pelvis, ALL6), rotation((11.4, 1.6, 8.0), "z", -22.5)),
           cube((6.4, 1.6, 6.4), (9.6, 3.6, 9.6), faces(pelvis, ALL6))]
    y = 3.6
    for i in range(9):
        h = 1.8
        els.append(cube((6.7, y, 6.7), (9.3, y + h, 9.3), faces(body, ALL6)))
        els.append(cube((5.4, y + 0.6, 7.4), (10.6, y + 1.4, 8.6), faces(process, ALL6)))
        els.append(cube((7.5, y + 0.4, 9.3), (8.5, y + 1.6, 10.8), faces(process, ALL6), rotation((8.0, y + 1.0, 9.3), "x", 22.5)))
        els.append(cube((6.9, y + h, 6.9), (9.1, y + h + 0.4, 9.1), faces(disc, SIDES4)))
        y += h + 0.4
    dark_face = sc.piece("shade_face", 12, 12, human_skull(BONE, 7))
    lit_face = sc.piece("shade_face_lit", 12, 12, human_skull(BONE, 7, glow=(255, 200, 110)))
    side = sc.piece("shade_side", 12, 12, cracked_bone(8, 1))
    for lit in (False, True):
        front = lit_face if lit else dark_face
        shade_ = [cube((4.4, y + 1.2, 4.4), (11.6, y + 7.6, 11.6), {"north": front, "east": side, "west": side, "south": side, "up": side,
                                                                    "down": side}, light=12 if lit else None),
                  cube((5.6, y, 4.6), (10.4, y + 1.4, 8.6), faces(side, ("north", "east", "west", "down")))]
        if lit:
            shade_.append(cube((6.6, y - 0.4, 6.6), (9.4, y + 1.2, 9.4), faces(glow, ALL6), light=15))
        sc.models["vertebra_floor_lamp_lit" if lit else "vertebra_floor_lamp"] = els + shade_
    return sc


# ---------------------------------------------------------------- 9. the buried colossus

def colossal_skull():
    """The Colossal Skull, facing north, drawn whole from its first block (its others to the right, up and back: x
    from -16, y to 32, z to 32): a giant's cracked cranium, its brow, big round sockets, cheekbones and the upper
    teeth. Its lower jaw and its sockets' glow are the client's (decor18_quads.json: `colossal_skull_jaw`)."""
    sc = Sculpt(d18.SKULL["block"], 190, 128)
    dome = sc.piece("dome", 48, 32, cracked_bone(2, 4, moss=0.15))
    side = sc.piece("side", 40, 32, cracked_bone(3, 3, moss=0.2))
    top = sc.piece("top", 40, 40, cracked_bone(4, 3))
    brow = sc.piece("brow", 44, 6, cracked_bone(5, 1))
    face = sc.piece("face", 20, 16, cracked_bone(6, 2))
    hollow = sc.piece("hollow", 16, 16, ca.bevel(ca.SOCKET, 1, light=0, dark=1))
    cheek = sc.piece("cheek", 16, 8, cracked_bone(8, 1))
    upper_teeth = sc.piece("upper_teeth", 28, 4, teeth())
    jaw = sc.piece("jaw", 28, 8, cracked_bone(9, 1))
    jaw_side = sc.piece("jaw_side", 16, 10, cracked_bone(10, 1))
    lower_teeth = sc.piece("lower_teeth", 24, 3, teeth())
    # The cranium's front is left out on purpose: the sockets look through it into the hollows lined by the face round
    # them and closed behind by the dark plate at z 8 (model_writer "_keep_open"; art_check's O1 allow-list names the
    # skull). Thin plates close its front beside the face, and the brow reaches back to the plate over the sockets.
    els = [dict(cube((-14.0, 8.0, 6.0), (14.0, 28.0, 29.0), faces(side, ("east", "west"), up=top, down=hollow, south=dome)),
                _keep_open=["north"]),
           cube((-14.0, 8.0, 5.9), (-12.0, 28.0, 6.0), faces(side, ALL6)),
           cube((12.0, 8.0, 5.9), (14.0, 28.0, 6.0), faces(side, ALL6)),
           cube((-12.0, 28.0, 8.0), (12.0, 31.0, 27.0), faces(top, ALL6)),
           cube((-15.0, 12.0, 9.0), (15.0, 25.0, 25.0), faces(side, ("east", "west", "north", "south", "up", "down"))),
           cube((-11.0, 6.0, 27.0), (11.0, 27.0, 31.0), faces(dome, ALL6)),
           cube((-13.0, 19.6, 3.0), (13.0, 24.0, 8.0), faces(brow, ALL6)),
           cube((-12.0, 24.0, 5.0), (12.0, 28.0, 8.0), faces(dome, ("north", "east", "west", "up"))),
           # The face round the sockets (no nose hole: the clean style keeps faces simple), and the hollows behind them.
           cube((-12.0, 11.0, 4.0), (-10.0, 19.6, 8.0), faces(face, ("north", "east", "west"))),
           cube((-2.6, 13.4, 4.0), (2.6, 19.6, 8.0), faces(face, ("north", "east", "west"))),
           cube((10.0, 11.0, 4.0), (12.0, 19.6, 8.0), faces(face, ("north", "east", "west"))),
           cube((-12.0, 8.0, 4.0), (-2.0, 11.6, 8.0), faces(cheek, ("north", "east", "west", "down"), up=hollow)),
           cube((2.0, 8.0, 4.0), (12.0, 11.6, 8.0), faces(cheek, ("north", "east", "west", "down"), up=hollow)),
           # The hollow closing the back of the sockets; ColossalSkullRenderer draws their night glow just in front of
           # its face (decor18.SKULL glow_z and hollow_z).
           cube((-11.0, 9.0, d18.SKULL["hollow_z"]), (11.0, 20.0, d18.SKULL["hollow_z"] + 1.0), faces(hollow, ("north",))),
           cube((-2.0, 8.0, 4.0), (2.0, 13.4, 8.0), faces(face, ("north", "east", "west"))),
           # The skull's front corners beside the brow and cheeks, closed.
           cube((-14.0, 8.0, 6.0), (-12.0, 28.0, 6.0), faces(side, ("north",))),
           cube((12.0, 8.0, 6.0), (14.0, 28.0, 6.0), faces(side, ("north",))),
           # Cheekbones out the sides, and the upper jaw with its teeth.
           cube((-15.6, 9.0, 7.0), (-12.0, 12.4, 16.0), faces(cheek, ALL6)),
           cube((12.0, 9.0, 7.0), (15.6, 12.4, 16.0), faces(cheek, ALL6)),
           cube((-8.0, 4.4, 4.4), (8.0, 8.0, 12.0), faces(face, ALL6)),
           cube((-7.2, 3.2, 4.2), (7.2, 4.4, 5.8), faces(upper_teeth, ALL6))]
    sc.models[d18.SKULL["block"]] = els
    # The jaw, hinged at its back corners (the renderer turns it down about x at y 8, z 15).
    sc.models["colossal_skull_jaw"] = [
        cube((-8.0, 0.4, 4.4), (8.0, 3.4, 9.0), faces(jaw, ALL6)),
        cube((-7.0, 3.4, 4.6), (7.0, 4.4, 6.0), faces(lower_teeth, ("north", "up", "east", "west"))),
        cube((-11.0, 0.4, 8.0), (-8.0, 3.4, 13.0), faces(jaw_side, ALL6)),
        cube((8.0, 0.4, 8.0), (11.0, 3.4, 13.0), faces(jaw_side, ALL6)),
        cube((-11.6, 2.0, 12.0), (-9.0, 9.0, 16.0), faces(jaw_side, ALL6)),
        cube((9.0, 2.0, 12.0), (11.6, 9.0, 16.0), faces(jaw_side, ALL6))]
    sc.models["item"] = els + sc.models["colossal_skull_jaw"]
    return sc


# Where the skull's sockets are, for the client's glow: {x0, y0, x1, y1} on a sheet facing north just in front of the hollow
# behind them (decor18.SKULL).
SKULL_SOCKETS = d18.SKULL["sockets"]


def colossal_rib():
    """A Colossal Rib, facing north, drawn whole from its upper half (y from -16): a giant's rib rising from the ground
    at the back of its block, curving forward over the block in front; joined, its tip runs on to meet its partner's
    over the middle of that block, and otherwise ends short and broken."""
    sc = Sculpt(d18.RIB["block"], 191, 128)
    bone_ = sc.piece("bone", 12, 40, cracked_bone(2, 3, moss=0.1))
    end = sc.piece("end", 8, 8, cracked_bone(3, 1))
    broken = sc.piece("broken", 8, 8, solid(SOCKET[1:] + [BONE[1], BONE[2]], 4))
    # The shaft stands on the centre line z 12; each piece above leans further north (a negative turn about x tips its
    # top north) and starts where the one below it ends: 22.5 degrees from (y 2, z 12) to (y 10.3, z 8.6), then 45 to
    # (y 14.1, z 4.1), then level. (The item draws it a block higher, so nothing may reach above 15.9.)
    els = [cube((6.2, -15.9, 10.0), (9.8, 4.0, 14.0), faces(bone_, SIDES4, down=end, up=end)),
           cube((6.3, 2.0, 10.2), (9.7, 11.4, 13.8), faces(bone_, SIDES4, up=end, down=end), rotation((8.0, 2.0, 12.0), "x", -22.5)),
           cube((6.4, 9.4, 6.9), (9.6, 15.8, 10.3), faces(bone_, SIDES4, up=end, down=end), rotation((8.0, 9.6, 8.6), "x", -45)),
           cube((6.5, 12.4, -2.0), (9.5, 15.6, 5.6), faces(bone_, ("east", "west", "up", "down"), south=end, north=end))]
    els.append(cube((5.2, -15.9, 9.0), (10.8, -15.0, 15.0), faces(end, ALL6)))
    sc.models["colossal_rib_joined"] = els + [cube((6.6, 12.5, -8.0), (9.4, 15.7, -1.6), faces(bone_, ("east", "west", "up", "down"), north=end)),
                                              cube((6.2, 12.1, -8.0), (9.8, 16.1, -6.6), faces(end, ALL6))]
    sc.models["colossal_rib"] = els + [cube((6.8, 12.8, -4.6), (9.2, 15.4, -1.6), faces(bone_, ("east", "west", "up", "down"), north=broken))]
    return sc


def colossal_vertebra():
    """A Colossal Vertebra along y: a round body (its rings at each end), the arch behind it with the long spine
    standing back, and a wing out each side."""
    sc = Sculpt(d18.VERTEBRA["block"], 192, 64)
    body = sc.piece("body", 16, 16, cracked_bone(2, 2))
    ring_ = sc.piece("rings", 14, 14, canal(BONE))
    wing = sc.piece("wing", 8, 10, cracked_bone(3, 1))
    # The round body: a box and a copy turned 45 degrees, its ends 0.1 inside the box's so no two caps share a plane.
    els = [cube((4.0, 0.2, 4.0), (12.0, 15.8, 12.0), faces(body, SIDES4, up=ring_, down=ring_)),
           cube((3.4, 0.3, 3.4), (12.6, 15.7, 12.6), faces(body, SIDES4, up=ring_, down=ring_), rotation((8.0, 8.0, 8.0), "y", 45)),
           cube((5.6, 3.0, 12.0), (10.4, 13.0, 14.0), faces(wing, ALL6)),
           cube((7.0, 4.0, 13.0), (9.0, 11.0, 16.0), faces(wing, ALL6)),
           cube((0.4, 5.0, 7.0), (4.0, 11.0, 10.0), faces(wing, ALL6)),
           cube((12.0, 5.0, 7.0), (15.6, 11.0, 10.0), faces(wing, ALL6))]
    sc.models[d18.VERTEBRA["block"]] = els
    return sc


def colossal_femur():
    """The Colossal Femur, head toward north, drawn whole from its head half: the ball of its hip end on its neck and
    the great knob beside it, the long shaft, and the two knuckles of the knee at its foot end, lying on the ground."""
    sc = Sculpt(d18.FEMUR["block"], 193, 128)
    shaft = sc.piece("shaft", 20, 48, cracked_bone(2, 4, moss=0.1))
    knob = sc.piece("knob", 16, 16, cracked_bone(3, 1))
    els = [cube((5.2, 0.0, 8.0), (10.8, 5.6, 26.0), faces(shaft, ALL6)),
           cube((5.8, 0.0, 8.1), (10.2, 5.6, 25.9), faces(shaft, ALL6), rotation((8.0, 2.8, 17.0), "z", 45)),
           cube((3.6, 0.0, 1.0), (9.4, 6.4, 6.6), faces(knob, ALL6)),
           cube((3.0, 0.6, 1.6), (10.0, 5.8, 6.0), faces(knob, ALL6), rotation((6.5, 3.2, 3.8), "y", 45)),
           cube((5.4, 0.4, 4.6), (9.4, 5.0, 10.0), faces(knob, ALL6), rotation((7.4, 2.7, 7.3), "y", 22.5)),
           cube((9.6, 0.0, 5.6), (13.0, 6.8, 10.4), faces(knob, ALL6)),
           cube((3.0, 0.0, 25.0), (7.6, 6.8, 31.0), faces(knob, ALL6)),
           cube((8.4, 0.0, 25.0), (13.0, 6.8, 31.0), faces(knob, ALL6)),
           cube((4.6, 0.0, 24.0), (11.4, 5.6, 27.0), faces(knob, ALL6))]
    sc.models[d18.FEMUR["block"]] = els
    sc.models["item"] = els
    return sc


# ---------------------------------------------------------------- 10. the gargoyles

def gargoyle_sentinel():
    """The Gargoyle Sentinel, facing north: a moulded pedestal with a carved die, and crouched on it a winged gargoyle,
    haunches down, forearms braced and its claws over the pedestal's front edge, wings folded up its back, a tail
    curled round. Its head is the client's (decor18_quads.json: `gargoyle_sentinel_head`, turned about y at x 8, z 7),
    and so is its eyes' glow."""
    sc = Sculpt(d18.SENTINEL["block"], 194, 128)
    base = sc.piece("base", 48, 6, stone(GARGOYLE, 2, lichen=0.1))
    die = sc.piece("die", 40, 14, quatrefoil(GARGOYLE, 3))
    top = sc.piece("top", 24, 24, stone(GARGOYLE, 4))
    hide = sc.piece("hide", 24, 24, relief(GARGOYLE, 5, "scales"))
    limb = sc.piece("limb", 8, 20, stone(GARGOYLE, 6))
    claw = sc.piece("claw", 6, 4, ca.bevel(GARGOYLE, 4))
    wing = sc.piece("wing", 24, 28, membrane(GARGOYLE, 8))
    face = sc.piece("face", 16, 14, grotesque(GARGOYLE, 9))
    head_side = sc.piece("head_side", 14, 14, relief(GARGOYLE, 10, "scales"))
    snout = sc.piece("snout", 12, 8, grotesque(GARGOYLE, 11, mouth=True))
    horn_ = sc.piece("horn", 6, 16, ca.bevel(GARGOYLE, 4, sides="lr"))
    # Opaque: the renderer draws the eyes cut out, which shows a half-transparent texel solid anyway.
    eye = sc.piece("eye", 4, 4, glow_dot((255, 60, 40), 255))
    els = [cube((2.0, 0.0, 2.0), (14.0, 1.4, 14.0), faces(base, SIDES4, up=top, down=top)),
           cube((3.0, 1.4, 3.0), (13.0, 5.0, 13.0), faces(die, SIDES4)),
           cube((2.6, 5.0, 2.6), (13.4, 6.0, 13.4), faces(base, SIDES4, up=top, down=top)),
           # Haunches and hind feet, the body leaning forward over them.
           cube((4.0, 6.0, 7.0), (12.0, 10.4, 12.8), faces(hide, ALL6)),
           cube((3.6, 6.0, 9.4), (6.2, 7.6, 13.2), faces(limb, ALL6)),
           cube((9.8, 6.0, 9.4), (12.4, 7.6, 13.2), faces(limb, ALL6)),
           cube((5.0, 8.6, 5.0), (11.0, 14.6, 10.6), faces(hide, ALL6), rotation((8.0, 9.0, 8.0), "x", 22.5)),
           # Forearms braced on the pedestal, claws over its edge.
           cube((4.2, 6.0, 3.6), (6.2, 11.0, 5.6), faces(limb, ALL6)),
           cube((9.8, 6.0, 3.6), (11.8, 11.0, 5.6), faces(limb, ALL6)),
           cube((4.0, 5.0, 2.2), (6.4, 6.2, 3.8), faces(claw, ALL6)),
           cube((9.6, 5.0, 2.2), (12.0, 6.2, 3.8), faces(claw, ALL6)),
           # Wings folded up its back, and the tail curled round its foot.
           cube((3.4, 9.0, 8.4), (5.0, 18.0, 14.6), faces(wing, ALL6), rotation((4.2, 9.0, 11.5), "z", 22.5)),
           cube((11.0, 9.0, 8.4), (12.6, 18.0, 14.6), faces(wing, ALL6), rotation((11.8, 9.0, 11.5), "z", -22.5)),
           cube((11.6, 6.0, 12.4), (13.4, 7.4, 14.4), faces(limb, ALL6)),
           cube((12.0, 6.0, 7.0), (13.4, 7.2, 12.4), faces(limb, ALL6)),
           cube((11.4, 6.0, 5.2), (13.0, 7.8, 7.2), faces(claw, ALL6))]
    sc.models[d18.SENTINEL["block"]] = els
    head = [cube((5.6, 13.4, 3.8), (10.4, 18.2, 8.6), faces(head_side, ALL6, north=face)),
            cube((6.4, 13.2, 2.2), (9.6, 15.8, 3.8), faces(head_side, ALL6, north=snout)),
            cube((5.4, 17.4, 6.2), (6.6, 20.0, 7.4), faces(horn_, ALL6), rotation((6.0, 17.4, 6.8), "z", 22.5)),
            cube((9.4, 17.4, 6.2), (10.6, 20.0, 7.4), faces(horn_, ALL6), rotation((10.0, 17.4, 6.8), "z", -22.5)),
            cube((4.4, 15.6, 6.0), (5.6, 17.6, 7.6), faces(horn_, ALL6)),
            cube((10.4, 15.6, 6.0), (11.6, 17.6, 7.6), faces(horn_, ALL6))]
    sc.models["gargoyle_sentinel_head"] = head
    sc.models["gargoyle_sentinel_eyes"] = [cube((6.4, 15.9, 3.74), (7.6, 16.7, 3.76), {"north": eye}),
                                           cube((8.4, 15.9, 3.74), (9.6, 16.7, 3.76), {"north": eye})]
    sc.models["item"] = els + head
    return sc


def gargoyle_rainspout():
    """The Gargoyle Rainspout, sticking out of a wall to the south toward north: a corbelled bracket on the wall, a
    long lead-lined stone gutter with the beast's spiny back over it and its forelegs clasping it, and at its end the
    grotesque head with its jaws wide, the water's outlet (the stream is the client's)."""
    sc = Sculpt(d18.RAINSPOUT["block"], 195, 128)
    plate = sc.piece("plate", 20, 14, stone(GARGOYLE, 2, lichen=0.12))
    corbel = sc.piece("corbel", 16, 8, relief(GARGOYLE, 3))
    channel = sc.piece("channel", 30, 6, stone(GARGOYLE, 4, lichen=0.1))
    lead = sc.piece("lead", 10, 28, ca.bevel(COPPER, 2, sides="lr"))
    back = sc.piece("back", 12, 28, relief(GARGOYLE, 6, "scales"))
    spine = sc.piece("spine", 4, 4, ca.bevel(GARGOYLE, 4))
    limb = sc.piece("limb", 6, 12, stone(GARGOYLE, 8))
    face = sc.piece("face", 14, 12, grotesque(GARGOYLE, 9, mouth=True))
    head_side = sc.piece("head_side", 12, 12, relief(GARGOYLE, 10, "scales"))
    jaw = sc.piece("jaw", 12, 6, grotesque(GARGOYLE, 11, mouth=True))
    gullet = sc.piece("gullet", 6, 6, ca.bevel(ca.SOCKET, 1, light=0))
    horn_ = sc.piece("horn", 6, 12, ca.bevel(GARGOYLE, 4, sides="lr"))
    els = [cube((3.6, 2.6, 14.0), (12.4, 13.4, 16.0), faces(plate, ALL6)),
           cube((5.0, 0.6, 13.0), (11.0, 3.6, 16.0), faces(corbel, ALL6), rotation((8.0, 3.6, 14.5), "x", -22.5)),
           # The gutter: its floor lined with lead, its walls, the beast's back and spines over it.
           cube((5.4, 6.0, 3.0), (10.6, 7.0, 14.0), faces(channel, ALL6, up=lead)),
           cube((5.0, 6.0, 3.0), (5.6, 9.0, 14.0), faces(channel, ALL6)),
           cube((10.4, 6.0, 3.0), (11.0, 9.0, 14.0), faces(channel, ALL6)),
           cube((5.2, 9.0, 4.0), (10.8, 10.6, 14.0), faces(back, ALL6))]
    for z in (5.0, 7.6, 10.2, 12.8):
        els.append(cube((7.4, 10.6, z), (8.6, 11.8, z + 1.0), faces(spine, ALL6), rotation((8.0, 10.6, z + 0.5), "x", -22.5)))
    for x in (4.2, 10.6):
        els += [cube((x, 5.0, 4.2), (x + 1.2, 9.4, 5.6), faces(limb, ALL6)),
                cube((x - 0.2, 4.4, 3.6), (x + 1.4, 5.6, 6.0), faces(limb, ALL6))]
    # The head at the gutter's end, its jaws wide.
    els += [cube((4.8, 7.6, -1.2), (11.2, 12.0, 4.0), faces(head_side, ALL6, north=face)),
            cube((5.4, 4.4, -1.6), (10.6, 6.0, 3.4), faces(head_side, ALL6, north=jaw), rotation((8.0, 6.0, 3.4), "x", 22.5)),
            cube((6.4, 6.0, -0.4), (9.6, 7.6, 3.0), faces(gullet, ALL6)),
            cube((4.6, 11.6, 1.4), (5.8, 14.0, 2.6), faces(horn_, ALL6), rotation((5.2, 11.6, 2.0), "z", 22.5)),
            cube((10.2, 11.6, 1.4), (11.4, 14.0, 2.6), faces(horn_, ALL6), rotation((10.8, 11.6, 2.0), "z", -22.5))]
    sc.models[d18.RAINSPOUT["block"]] = els
    return sc


# Where the rainspout's mouth is (the stream's top), in pixels for a spout facing north.
SPOUT_MOUTH = (8.0, 6.4, -1.0)


def chimera_finial():
    """The Chimera Finial, facing north: a square plinth, a neck and a stone ball, and crouched on the ball a winged
    chimera, a lion's head and mane, a goat's horns, a serpent for a tail; its wings folded, or spread wide in a storm."""
    sc = Sculpt(d18.FINIAL["block"], 196, 64)
    base = sc.piece("base", 24, 6, stone(GARGOYLE, 2, lichen=0.1))
    ball = sc.piece("ball", 16, 16, stone(GARGOYLE, 3, lichen=0.12))
    body = sc.piece("body", 16, 12, relief(GARGOYLE, 4, "scales"))
    mane = sc.piece("mane", 12, 12, relief(GARGOYLE, 5, "mane"))
    face = sc.piece("face", 8, 8, grotesque(GARGOYLE, 6))
    horn_ = sc.piece("horn", 4, 8, ca.bevel(GARGOYLE, 4, sides="lr"))
    wing = sc.piece("wing", 14, 14, membrane(GARGOYLE, 8))
    tail = sc.piece("tail", 4, 10, relief(GARGOYLE, 9, "scales"))
    els = [cube((3.6, 0.0, 3.6), (12.4, 1.6, 12.4), faces(base, ALL6)),
           cube((6.0, 1.6, 6.0), (10.0, 3.6, 10.0), faces(base, ALL6)),
           cube((4.6, 3.6, 4.6), (11.4, 9.4, 11.4), faces(ball, ALL6)),
           cube((4.0, 4.2, 4.0), (12.0, 8.8, 12.0), faces(ball, ALL6), rotation((8.0, 6.5, 8.0), "y", 45)),
           cube((5.6, 9.4, 6.4), (10.4, 12.4, 11.6), faces(body, ALL6)),
           cube((5.4, 11.0, 4.4), (10.6, 15.0, 7.4), faces(mane, ALL6)),
           cube((6.4, 11.4, 3.2), (9.6, 14.4, 4.6), faces(mane, ALL6, north=face)),
           cube((6.0, 14.6, 5.4), (7.0, 16.4, 6.4), faces(horn_, ALL6), rotation((6.5, 14.6, 5.9), "x", -22.5)),
           cube((9.0, 14.6, 5.4), (10.0, 16.4, 6.4), faces(horn_, ALL6), rotation((9.5, 14.6, 5.9), "x", -22.5)),
           cube((7.4, 10.4, 11.4), (8.6, 14.0, 12.6), faces(tail, ALL6), rotation((8.0, 10.4, 12.0), "x", 22.5))]
    storm = [cube((0.4, 12.6, 7.6), (5.6, 13.4, 11.6), faces(wing, ALL6), rotation((5.6, 13.0, 9.6), "z", -22.5)),
                cube((10.4, 12.6, 7.6), (15.6, 13.4, 11.6), faces(wing, ALL6), rotation((10.4, 13.0, 9.6), "z", 22.5)),
                cube((1.0, 13.4, 8.0), (5.6, 16.6, 8.6), faces(wing, ALL6), rotation((5.6, 13.4, 8.3), "z", -45)),
                cube((10.4, 13.4, 8.0), (15.0, 16.6, 8.6), faces(wing, ALL6), rotation((10.4, 13.4, 8.3), "z", 45))]
    folded = [cube((4.8, 10.0, 7.2), (5.6, 14.6, 12.0), faces(wing, ALL6), rotation((5.2, 10.0, 9.6), "z", 22.5)),
              cube((10.4, 10.0, 7.2), (11.2, 14.6, 12.0), faces(wing, ALL6), rotation((10.8, 10.0, 9.6), "z", -22.5))]
    sc.models["chimera_finial"] = els + folded
    sc.models["chimera_finial_storm"] = els + storm
    return sc


def stream_texture():
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    water(1)(Px(img))
    return img


def socket_glow_texture():
    """The colossal skull's sockets' glow: a flat pale-blue square, a little fainter in a one-texel rim, drawn exactly over
    each socket (skull glows are flat squares, never soft halos)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            rim = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (120, 200, 255, 150 if rim else 200))
    return img


# ---------------------------------------------------------------- building, quads and files

_built = {}


def build(name):
    """The Sculpt model `name` is drawn from: the lit, joined and storm models share their block's."""
    name = texture_of(name)
    if name not in _built:
        builders = {d18.COFFIN["block"]: iron_bound_coffin, d18.WARDROBE["block"]: coffin_wardrobe,
                    **{f"{s}_sarcophagus": (lambda s=s: sarcophagus(s)) for s in d18.STONES},
                    "bone_throne": bone_throne, d18.BOOKCASE["block"]: ribcage_bookcase, d18.FOOTSTOOL["block"]: skull_footstool,
                    "vertebra_floor_lamp": vertebra_floor_lamp, d18.SKULL["block"]: colossal_skull, "colossal_rib": colossal_rib,
                    d18.VERTEBRA["block"]: colossal_vertebra, d18.FEMUR["block"]: colossal_femur, d18.SENTINEL["block"]: gargoyle_sentinel,
                    d18.RAINSPOUT["block"]: gargoyle_rainspout, "chimera_finial": chimera_finial}
        _built[name] = builders[name]()
    return _built[name]


def decor18_quads():
    """The renderer's models: the sarcophagi's lids, the colossal skull's jaw, the sentinel's head and eyes."""
    out = {}
    for stone_ in d18.STONES:
        block = f"{stone_}_sarcophagus"
        sc = build(block)
        for kind in d18.LIDS:
            out[f"{block}_lid_{kind}"] = [dict(q, cutout=True) for q in quads(sc.models[f"lid_{kind}"], {"p": block})]
    sc = build(d18.SKULL["block"])
    out["colossal_skull_jaw"] = [dict(q, cutout=True) for q in quads(sc.models["colossal_skull_jaw"], {"p": d18.SKULL["block"]})]
    sc = build(d18.SENTINEL["block"])
    for name in ("gargoyle_sentinel_head", "gargoyle_sentinel_eyes"):
        out[name] = [dict(q, cutout=True) for q in quads(sc.models[name], {"p": d18.SENTINEL["block"]})]
    return out


TALL_ITEM = d17d.TALL_ITEM
SMALL_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
              "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
              "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
              "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [0.5, 0.5, 0.5]},
              "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
              "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]}}
LONG_ITEM = {"gui": {"rotation": [25, 135, 0], "translation": [0, 1, 0], "scale": [0.42, 0.42, 0.42]},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
             "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
             "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [0.5, 0.5, 0.5]},
             "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]}}


def texture_of(model):
    """The texture a model is drawn from: the lit and joined variants share their block's."""
    for suffix in ("_lit", "_joined", "_storm"):
        if model.endswith(suffix):
            return model[: -len(suffix)]
    return model


def _image_of(name):
    """The Sculpt texture a model's "#p" (block/<name>) is drawn from, for fa.closing_writer; None for another."""
    try:
        return build(name).atlas.img
    except KeyError:
        return None


def assets(root, write, lang):
    # Every block and item model is closed: no face a box leaves out shows a hole (docs/ART_DIRECTION.md).
    write = fa.closing_writer(write, _image_of)
    models = root / "models" / "block"
    states = root / "blockstates"
    items = root / "models" / "item"
    lang.update({f"block.{MOD}.{block}": display for block, display in d18.names().items()})
    lang[f"item.{MOD}.{d18.KEY['item']}"] = d18.KEY["display"]
    lang[f"item.{MOD}.{d18.KEY['blank']}"] = d18.KEY["blank_display"]
    lang.update(d18.MESSAGES)

    def model(name, source=None, elements=None):
        sc = build(source or name)
        write(models / f"{name}.json", fa.model(texture_of(source or name), elements if elements is not None else sc.models[name]))

    def particle(name, texture):
        write(models / f"{name}.json", {"textures": {"particle": rid(f"block/{texture}")}})

    def turned(model_name, facing, **extra):
        return {"model": rid(f"block/{model_name}"), **({"y": FACINGS[facing]} if FACINGS[facing] else {}), **extra}

    # 6. The coffin (bed-like halves: facing points from foot to head), the keys and the wardrobe.
    coffin = d18.COFFIN["block"]
    sc = build(coffin)
    for name in ("iron_bound_coffin_head", "iron_bound_coffin_head_open", "iron_bound_coffin_foot", "iron_bound_coffin_foot_open",
                 "iron_bound_coffin_foot_locked", "iron_bound_coffin_foot_open_locked"):
        write(models / f"{name}.json", fa.model(coffin, sc.models[name]))
    variants = {}
    for f in FACINGS:
        for part in ("head", "foot"):
            for opened in (False, True):
                for occupied in (False, True):
                    for locked in (False, True):
                        name = f"{coffin}_{part}{'_open' if opened else ''}{'_locked' if locked and part == 'foot' else ''}"
                        key = f"facing={f},locked={str(locked).lower()},occupied={str(occupied).lower()},open={str(opened).lower()},part={part}"
                        variants[key] = turned(name, f)
    write(states / f"{coffin}.json", {"variants": variants})
    write(items / f"{coffin}.json", fa.model(coffin, sc.models["item"], LONG_ITEM))
    for key in (d18.KEY["item"], d18.KEY["blank"]):
        write(items / f"{key}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{key}")}})

    wardrobe = d18.WARDROBE["block"]
    model(wardrobe)
    particle(f"{wardrobe}_upper", wardrobe)
    write(states / f"{wardrobe}.json", {"variants": {
        f"facing={f},half={h}": turned(wardrobe if h == "lower" else f"{wardrobe}_upper", f if h == "lower" else "north")
        for f in FACINGS for h in ("lower", "upper")}})
    write(items / f"{wardrobe}.json", fa.model(wardrobe, build(wardrobe).models[wardrobe], TALL_ITEM))

    # 7. The sarcophagi: the chest from the head half; the foot half only gives particles; the lid is the client's.
    for stone_ in d18.STONES:
        block = f"{stone_}_sarcophagus"
        sc = build(block)
        model(block)
        particle(f"{block}_foot", block)
        write(states / f"{block}.json", {"variants": {
            f"facing={f},lid={lid},open={str(o).lower()},part={part}": turned(block if part == "head" else f"{block}_foot", f)
            for f in FACINGS for lid in d18.LIDS for o in (False, True) for part in ("head", "foot")}})
        write(items / f"{block}.json", fa.model(block, sc.models["item"], LONG_ITEM))

    # 8. The parlour.
    throne = d18.THRONE["block"]
    model("bone_throne")
    model("bone_throne_lit")
    particle(f"{throne}_upper", throne)
    write(states / f"{throne}.json", {"variants": {
        f"facing={f},half={h},lit={str(lit).lower()}":
            turned((f"{throne}_lit" if lit else throne) if h == "lower" else f"{throne}_upper", f if h == "lower" else "north")
        for f in FACINGS for h in ("lower", "upper") for lit in (False, True)}})
    write(items / f"{throne}.json", fa.model(throne, build("bone_throne").models["bone_throne"], TALL_ITEM))
    case = d18.BOOKCASE["block"]
    sc = build(case)
    model(case)
    for slot in range(6):
        model(f"{case}_slot_{slot}", case, sc.models[f"{case}_slot_{slot}"])
    parts = [{"when": {"facing": f}, "apply": turned(case, f)} for f in FACINGS]
    parts += [{"when": {"facing": f, f"slot_{slot}_occupied": "true"}, "apply": turned(f"{case}_slot_{slot}", f)}
              for f in FACINGS for slot in range(6)]
    write(states / f"{case}.json", {"multipart": parts})
    write(items / f"{case}.json", fa.model(case, sc.models["item"], fa.PLANT_DISPLAY))
    stool = d18.FOOTSTOOL["block"]
    model(stool)
    write(states / f"{stool}.json", {"variants": {f"facing={f}": turned(stool, f) for f in FACINGS}})
    write(items / f"{stool}.json", {"parent": rid(f"block/{stool}"), "display": fa.PLANT_DISPLAY})
    lamp = d18.LAMP["block"]
    model("vertebra_floor_lamp")
    model("vertebra_floor_lamp_lit")
    particle(f"{lamp}_upper", lamp)
    write(states / f"{lamp}.json", {"variants": {
        f"facing={f},half={h},lit={str(lit).lower()},powered={str(p).lower()}":
            turned((f"{lamp}_lit" if lit else lamp) if h == "lower" else f"{lamp}_upper", f if h == "lower" else "north")
        for f in FACINGS for h in ("lower", "upper") for lit in (False, True) for p in (False, True)}})
    write(items / f"{lamp}.json", fa.model(lamp, build("vertebra_floor_lamp_lit").models["vertebra_floor_lamp_lit"], TALL_ITEM))

    # 9. The colossus.
    skull = d18.SKULL["block"]
    sc = build(skull)
    model(skull)
    particle(f"{skull}_part", skull)
    write(states / f"{skull}.json", {"variants": {
        f"facing={f},lit={str(lit).lower()},part={part},powered={str(p).lower()}": turned(skull if part == 0 else f"{skull}_part", f)
        for f in FACINGS for lit in (False, True) for part in range(8) for p in (False, True)}})
    write(items / f"{skull}.json", fa.model(skull, sc.models["item"], SMALL_ITEM))
    rib = d18.RIB["block"]
    model("colossal_rib")
    model("colossal_rib_joined")
    particle(f"{rib}_lower", rib)
    write(states / f"{rib}.json", {"variants": {
        f"facing={f},half={h},joined={str(j).lower()}":
            turned((f"{rib}_joined" if j else rib) if h == "upper" else f"{rib}_lower", f if h == "upper" else "north")
        for f in FACINGS for h in ("lower", "upper") for j in (False, True)}})
    write(items / f"{rib}.json", fa.model(rib, shifted(build("colossal_rib").models["colossal_rib"], dy=16.0), TALL_ITEM))
    vertebra = d18.VERTEBRA["block"]
    model(vertebra)
    write(states / f"{vertebra}.json", {"variants": {
        "axis=y": {"model": rid(f"block/{vertebra}")},
        "axis=z": {"model": rid(f"block/{vertebra}"), "x": 90},
        "axis=x": {"model": rid(f"block/{vertebra}"), "x": 90, "y": 90}}})
    write(items / f"{vertebra}.json", {"parent": rid(f"block/{vertebra}"), "display": fa.PLANT_DISPLAY})
    femur = d18.FEMUR["block"]
    model(femur)
    particle(f"{femur}_foot", femur)
    write(states / f"{femur}.json", {"variants": {
        f"facing={f},part={part}": turned(femur if part == "head" else f"{femur}_foot", f) for f in FACINGS for part in ("head", "foot")}})
    write(items / f"{femur}.json", fa.model(femur, build(femur).models["item"], LONG_ITEM))

    # 10. The gargoyles.
    sentinel = d18.SENTINEL["block"]
    sc = build(sentinel)
    model(sentinel)
    write(states / f"{sentinel}.json", {"variants": {f"facing={f},power={p}": turned(sentinel, f) for f in FACINGS for p in range(16)}})
    write(items / f"{sentinel}.json", fa.model(sentinel, sc.models["item"], fa.PLANT_DISPLAY))
    spout = d18.RAINSPOUT["block"]
    model(spout)
    write(states / f"{spout}.json", {"variants": {f"facing={f},pouring={str(p).lower()}": turned(spout, f) for f in FACINGS for p in (False, True)}})
    write(items / f"{spout}.json", {"parent": rid(f"block/{spout}"), "display": fa.PLANT_DISPLAY})
    finial = d18.FINIAL["block"]
    model("chimera_finial")
    model("chimera_finial_storm")
    write(states / f"{finial}.json", {"variants": {
        f"facing={f},weather={w}": turned("chimera_finial_storm" if w == "storm" else "chimera_finial", f)
        for f in FACINGS for w in ("clear", "rain", "storm")}})
    write(items / f"{finial}.json", {"parent": rid("block/chimera_finial_storm"), "display": fa.PLANT_DISPLAY})

    for item in d18.items():
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    write(root / "decor18_quads.json", decor18_quads())


def loot(out, write):
    from decor_data import self_drop
    for block in d18.blocks():
        half = {d18.COFFIN["block"]: ("part", "head"), d18.WARDROBE["block"]: ("half", "lower"), d18.THRONE["block"]: ("half", "lower"),
                d18.LAMP["block"]: ("half", "lower"), d18.RIB["block"]: ("half", "lower"), d18.FEMUR["block"]: ("part", "head"),
                d18.SKULL["block"]: ("part", "0"), **{s: ("part", "head") for s in d18.sarcophagi()}}.get(block)
        if half is None:
            write(out / f"{block}.json", self_drop(block))
            continue
        # Several blocks make one: the block named drops it.
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                {"type": "minecraft:match_block", "blocks": rid(block), "state": {half[0]: half[1]}}]},
            "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})


def tags(tags):
    tags.add("block", "minecraft:enchantment_power_provider", rid(d18.BOOKCASE["block"]))
    for block in d18.sarcophagi() + [d18.SENTINEL["block"], d18.RAINSPOUT["block"], d18.FINIAL["block"], d18.SKULL["block"],
                                     d18.RIB["block"], d18.VERTEBRA["block"], d18.FEMUR["block"]]:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in (d18.COFFIN["block"], d18.WARDROBE["block"], d18.BOOKCASE["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))


def textures():
    """(kind, name) -> image for each block's texture, the keys' item pictures and the client's stream and glow."""
    out = {}
    for model_name in ([d18.COFFIN["block"], d18.WARDROBE["block"]] + d18.sarcophagi()
                       + ["bone_throne", d18.BOOKCASE["block"], d18.FOOTSTOOL["block"], "vertebra_floor_lamp", d18.SKULL["block"],
                          "colossal_rib", d18.VERTEBRA["block"], d18.FEMUR["block"], d18.SENTINEL["block"], d18.RAINSPOUT["block"],
                          "chimera_finial"]):
        out[("block", texture_of(model_name))] = build(model_name).atlas.img
    out[("item", d18.KEY["item"])] = key_icon(False)
    out[("item", d18.KEY["blank"])] = key_icon(True)
    out[("entity", "crypt_stream")] = stream_texture()
    out[("entity", "crypt_socket_glow")] = socket_glow_texture()
    return out


def recipes(out, write, conditions):
    """The special recipe that copies a Skeleton Key onto Key Blanks (agriculture/KeyCopyingRecipe)."""
    write(out / f"{d18.KEY['recipe']}.json", {"fabric:load_conditions": conditions(), "type": f"{MOD}:{d18.KEY['recipe']}"})
