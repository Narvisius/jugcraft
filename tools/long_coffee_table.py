"""The Long Coffee Table: the Tri-Pillar lobby lounge's long coffee table (work list item 3, approved by Justin on 22 October
2026 from the mockup sheet with the ticker runner, the nuclear plant's mugs on it and the words "Drone Tower brought to
you by Narv-Tech." running along it; see claude/jugcraft-tripillar-coffee-table.md).

One table instead of the three small glass ones: a 4.0 x 1.1 slab of solid iroko (the planter bench's timber), four
boards glued up, 50 mm thick with a knife edge (22 mm of edge, bevelled 30 mm back underneath); a runner routed down its
middle, 3.39 long and 0.30 wide, lined with stainless steel, holding smoked glass over an amber LED dot board in two
lines, each reading to the sofa on its side (client/LongCoffeeTableRenderer runs the words along them, client/TickerRail);
two graphite powder-coated sled frames on felt glides and a spine between them; an amber LED line under each long edge
washing the floor softly. On it: two of the plant's mugs (tools/plant_office4.mug, coffee in them), three magazines, a
tablet showing the fleet and a stand for a 1:6 model of the T1 drone (the renderer draws the drone on it).

One item places it whole (tower/LongCoffeeTableBlock.java): the master is the cell south-east of the table's middle, its
other 7 cells are Long Coffee Table Parts (no item) that know their offset from it. The master's `facing` turns the
table about the master cell's middle (facing north, its length runs east-west).

Frame of the mesh: the table's middle at the origin (the master cell's north-west corner), the floor at y 0, its length
along x. Quad kinds: 0 shaded by the world, 1 full bright, 2 glass (translucent, shaded), 3 glow (translucent, full
bright: the floor wash). Live faces: the two ticker lines (kind 7; id 0 the north line, 1 the south; corners tl tr br bl
as the reader on that side sees them, the band's top edge tl-tr towards the middle).
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"

ID = "long_coffee_table"
PART_ID = "long_coffee_table_part"
NAME = "Long Coffee Table"
MESH = ASSETS / "tower_mesh/long_coffee_table.bin.gz"
TEXTURE = ASSETS / "textures/entity/long_coffee_table.png"

HX, HZ = 2.00, 0.55          # half length, half depth
TOP = 0.400                  # the top's surface
EDGE = 0.022                 # the visible edge
BEV = 0.030                  # how far the bevel runs in underneath
UNDER = 0.350                # the slab's underside
PITCH = 0.015                # the LED pitch, both ways
COLUMNS = 226                # LED columns a line
ROWS = 9                     # a line's rows (the glyph the middle seven)
RX, RZ = COLUMNS * PITCH / 2, 0.150     # the runner's opening (half sizes)
GAP = 0.008                  # from the middle to each line's top edge
GLASS_Y = 0.392              # the runner's glass
DOT_Y = 0.3875               # the lit dots (client/LongCoffeeTableRenderer) under it
PCB_Y = 0.385                # the LED board (the pocket's floor)
SLED_X = 1.40                # the sleds' middles
SLED_Z = 0.45                # their outer half depth
TUBE_W, TUBE_T = 0.050, 0.030
BOARDS = 4
# the T1 model's stand: its middle, and the height its drone stands at (client/LongCoffeeTableRenderer)
MODEL_X, MODEL_Z, MODEL_Y, MODEL_YAW = 0.10, 0.33, TOP + 0.075, 35.0
CELLS_X, CELLS_Z = 4, 2      # cells along and across
TICKER = 7                   # the live kind (client/ConsoleMesh)
SOLID, LIT, GLASS, GLOW = 0, 1, 2, 3
Y = np.array([0, 1.0, 0])
ATLAS = (2048, 1024)
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


# ------------------------------------------------------------------ textures

def _board(w, h, seed, tone):
    """One iroko board, the grain along u: interlocked ribbon stripe, soft growth lines, pores."""
    rnd = np.random.default_rng(seed)
    x = np.arange(w)[None, :] / w
    y = np.arange(h)[:, None] / h
    wav = (0.06 * np.sin(x * 2 * math.pi * rnd.uniform(0.6, 1.4) + rnd.uniform(0, 6))
           + 0.025 * np.sin(x * 2 * math.pi * rnd.uniform(3, 5) + rnd.uniform(0, 6)))
    g = y + wav
    lines = (np.sin(g * 2 * math.pi * rnd.uniform(7, 11)) * 0.5 + 0.5) ** 3
    fine = np.sin(g * 2 * math.pi * rnd.uniform(38, 52) + rnd.uniform(0, 6)) * 0.5 + 0.5
    ribbon = np.sin(x * 2 * math.pi * rnd.uniform(9, 14) + y * rnd.uniform(1, 3)) * 0.5 + 0.5
    low = np.repeat(rnd.uniform(-1, 1, (h // 4 + 2, 1)), 4, 0)[:h]
    k = 1 - 0.10 * lines - 0.04 * fine + 0.05 * ribbon + 0.03 * low
    a = np.ones((h, w, 4), np.float32)
    for c in range(3):
        a[..., c] = tone[c] * k
    n = int(w * h / 90)
    px, py, ln = rnd.integers(0, w, n), rnd.integers(0, h, n), rnd.integers(2, 5, n)
    for i in range(n):
        a[py[i], px[i]:px[i] + ln[i], :3] *= 0.86
    a[..., :3] *= (1 + rnd.uniform(-0.015, 0.015, (h, w)))[..., None]
    a[..., 3] = 255
    return np.clip(a, 0, 255)


def _top():
    """The whole top, four boards across (256 px a metre), each its own tone and figure; faint glue lines."""
    w, bh = 1024, 70
    tones = [(128, 84, 54), (121, 79, 51), (134, 89, 57), (125, 82, 53)]
    out = np.concatenate([_board(w, bh, 40 + i, tones[i]) for i in range(BOARDS)], 0)
    for i in range(1, BOARDS):
        out[i * bh - 1:i * bh + 1, :, :3] *= 0.9
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def _end():
    rnd = np.random.default_rng(7)
    a = np.ones((64, 64, 4), np.float32)
    y = np.arange(64)[:, None] / 64
    rings = (np.sin((y + 0.02 * np.sin(np.arange(64)[None, :] / 9)) * 2 * math.pi * 6) * 0.5 + 0.5) ** 2
    for c, base in enumerate((98, 62, 40)):
        a[..., c] = base * (1 - 0.12 * rings + rnd.uniform(-0.05, 0.05, (64, 64)))
    a[..., 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _wash():
    """The under-glow on the floor: a soft amber band along a long edge, fading out at the ends."""
    w, h = 128, 64
    a = np.zeros((h, w, 4), np.float32)
    v = (np.arange(h) + 0.5) / h
    u = (np.arange(w) + 0.5) / w
    across = np.exp(-((v - 0.5) / 0.26) ** 2)
    along = np.clip(np.minimum(u, 1 - u) / 0.12, 0, 1) ** 1.5
    a[..., 0], a[..., 1], a[..., 2] = 255, 163, 66
    a[..., 3] = 255 * 0.30 * across[:, None] * along[None, :]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def line_dot(line, column, row):
    """Where the LED at (column, glyph row 0-6) of a line (0 north, 1 south) is on the table: x0, z0, x1, z1 (as
    client/TickerRail draws its dots: 64% of the pitch, in the middle seven of the nine rows)."""
    s0, s1 = (column + 0.18) * PITCH, (column + 0.82) * PITCH
    d0, d1 = (row + 1) * PITCH + 0.18 * PITCH, (row + 2) * PITCH - 0.18 * PITCH
    if line == 0:
        xa, xb, za, zb = RX - s0, RX - s1, -GAP - d0, -GAP - d1
    else:
        xa, xb, za, zb = -RX + s0, -RX + s1, GAP + d0, GAP + d1
    return min(xa, xb), min(za, zb), max(xa, xb), max(za, zb)


DOTS_PX = 4  # pixels a pitch on the LED board's tile


def _dots():
    """The LED board under the glass: black, every LED of both lines a faint dot where its lit dot is drawn."""
    w = COLUMNS * DOTS_PX
    h = int(round(2 * RZ / PITCH * DOTS_PX))
    a = np.zeros((h, w, 4), np.uint8)
    a[..., :3] = 8
    a[..., 3] = 255
    sx, sz = w / (2 * RX), h / (2 * RZ)
    for line in (0, 1):
        for c in range(COLUMNS):
            for r in range(7):
                x0, z0, x1, z1 = line_dot(line, c, r)
                i0, i1 = int(round((x0 + RX) * sx)), int(round((x1 + RX) * sx))
                j0, j1 = int(round((z0 + RZ) * sz)), int(round((z1 + RZ) * sz))
                a[j0:max(j1, j0 + 1), i0:max(i1, i0 + 1), :3] = (30, 24, 18)
    return Image.fromarray(a, "RGBA")


def _cover(title, sub, seed, accent):
    """A magazine cover (invented titles)."""
    w, h = 105, 140
    rnd = np.random.default_rng(seed)
    im = Image.new("RGBA", (w, h), (22, 24, 28, 255))
    d = ImageDraw.Draw(im)
    for y in range(35, 125):
        t = (y - 35) / 90
        d.line([(0, y), (w, y)], fill=tuple(int(a * (1 - t) + b * t) for a, b in zip((40, 52, 86), (196, 120, 64))) + (255,))
    for i in range(10):
        x0 = int(rnd.integers(-5, w))
        bw, bh = int(rnd.integers(6, 17)), int(rnd.integers(10, 45))
        d.rectangle((x0, 125 - bh, x0 + bw, 125), fill=(18, 18, 22, 255))
    cx, cy = int(rnd.integers(35, 70)), int(rnd.integers(55, 75))
    d.rectangle((cx - 9, cy - 1, cx + 9, cy + 1), fill=(16, 16, 18, 255))
    for sx in (-11, 11):
        d.ellipse((cx + sx - 5, cy - 3, cx + sx + 5, cy - 1), outline=(16, 16, 18, 255))
    d.rectangle((0, 125, w, h), fill=(16, 16, 18, 255))
    d.text((6, 3), title, font=ImageFont.truetype(FONT_B, 22), fill=accent + (255,))
    d.text((7, 28), sub, font=ImageFont.truetype(FONT_R, 6), fill=(230, 230, 230, 255))
    d.text((6, 128), "THE FLEET ISSUE", font=ImageFont.truetype(FONT_B, 6), fill=(240, 240, 240, 255))
    return im


def _screen():
    w, h = 245, 170
    im = Image.new("RGBA", (w, h), (12, 13, 16, 255))
    d = ImageDraw.Draw(im)
    fb = lambda s: ImageFont.truetype(FONT_B, s)  # noqa: E731
    fr = lambda s: ImageFont.truetype(FONT_R, s)  # noqa: E731
    d.rectangle((0, 0, w, 18), fill=(26, 28, 34, 255))
    d.text((8, 9), "TRI-PILLAR  ·  FLEET", font=fb(10), fill=(255, 180, 80, 255), anchor="lm")
    d.text((w - 8, 9), "6 OUT", font=fb(10), fill=(230, 230, 230, 255), anchor="rm")
    d.rectangle((8, 26, 128, 160), fill=(18, 22, 26, 255), outline=(50, 56, 64, 255))
    for i in range(6):
        d.line([(8, 26 + i * 27), (128, 26 + i * 27)], fill=(30, 36, 42, 255))
        d.line([(8 + i * 24, 26), (8 + i * 24, 160)], fill=(30, 36, 42, 255))
    d.polygon([(68, 80), (80, 93), (68, 106), (56, 93)], outline=(255, 180, 80, 255))
    for (x, y) in ((40, 50), (100, 70), (90, 130), (30, 120), (112, 40), (55, 140)):
        d.line([(68, 93), (x, y)], fill=(60, 90, 130, 255))
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=(110, 180, 255, 255))
    rows = [("T1 FAN-WING", "DOCKED"), ("T2 QUAD CARRIER", "OUT"), ("T4 FAN-WING RUNNER", "OUT"), ("T3 HEAVY QUAD", "CHARGING"),
            ("T1 FAN-WING", "OUT"), ("T2 QUAD CARRIER", "DOCKED")]
    for i, (a, b) in enumerate(rows):
        y = 30 + i * 22
        d.rectangle((136, y, w - 8, y + 18), fill=(22, 24, 30, 255))
        d.text((141, y + 9), a, font=fr(8), fill=(220, 220, 220, 255), anchor="lm")
        d.text((w - 12, y + 9), b, font=fb(7), fill=(255, 180, 80, 255) if b == "OUT" else (150, 200, 150, 255), anchor="rm")
    return im


def _tint(rgb, alpha, size=16):
    return Image.new("RGBA", (size, size), tuple(rgb) + (int(round(alpha * 255)),))


def _plant(name):
    """A texture of the nuclear plant's (its mug and the coffee in it)."""
    return Image.open(ASSETS / f"textures/block/{name}.png").convert("RGBA")


def tiles():
    return {
        "top": _top(), "end": _end(), "dots": _dots(), "wash": _wash(), "screen": _screen(),
        "powder": TCC._noise((36, 38, 42), 2.2, 61, size=64), "steel": TCC._noise((186, 190, 196), 9, 62, "h", size=64),
        "glass": _tint((18, 14, 10), 0.52), "amber": TCC._solid((255, 172, 70)), "felt": TCC._solid((26, 26, 28)),
        "mug": _plant("pi_mug_plain"), "coffee": _plant("pi_coffee"),
        "pages": TCC._noise((232, 228, 218), 6, 63, "h", size=32), "tab_back": TCC._noise((64, 66, 72), 3, 64, "h", size=32),
        "tab_bezel": TCC._solid((14, 14, 16)), "stand": TCC._noise((24, 25, 28), 1.5, 65, size=32),
        "cover0": _cover("VECTOR", "LONG-RANGE CARRIERS TESTED", 3, (255, 182, 80)),
        "cover1": _cover("AIRFRAME", "WHAT THE TIER 9 CAN LIFT", 8, (236, 236, 236)),
        "cover2": _cover("LIFT", "INSIDE THE NEW TOWERS", 15, (120, 200, 255)),
    }


NOISE = {"powder", "steel", "pages", "tab_back", "stand"}


class Atlas:
    """The table's tiles on one sheet (as tools/drone_display_case.Atlas)."""

    def __init__(self, t):
        self.img = Image.new("RGBA", ATLAS, (0, 0, 0, 0))
        self.box = {}
        x = y = shelf = 0
        for name, im in sorted(t.items(), key=lambda kv: (-kv[1].height, kv[0])):
            if x + im.width > ATLAS[0]:
                x, y, shelf = 0, y + shelf + 2, 0
            if y + im.height > ATLAS[1]:
                raise ValueError("atlas full")
            self.img.paste(im, (x, y))
            self.box[name] = (x, y, im.width, im.height)
            x += im.width + 2
            shelf = max(shelf, im.height)

    def uv(self, name, u, v):
        x, y, w, h = self.box[name]
        u = min(max(u, 0.0), 1.0)
        v = min(max(v, 0.0), 1.0)
        return ((x + 0.5 + u * (w - 1)) / ATLAS[0], (y + 0.5 + v * (h - 1)) / ATLAS[1])


# ------------------------------------------------------------------ the mesh

def kind(m, first, k):
    """Sets the kind of the quads added since `first` and leaves them out of the baked shading (but solid)."""
    for q in m.quads[first:]:
        q["kind"] = k
        if k != SOLID:
            q["ao"] = False


def box(m, lo, hi, mat, faces="nsewud", lit=False):
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    m.obox((lo + hi) / 2, (1, 0, 0), (0, 1, 0), *((hi - lo) / 2), mat, faces=faces, lit=lit)


def top_f(x, z):
    """Where (x, z) on the top falls on its tile (fractions)."""
    return ((x + HX) / (2 * HX), (z + HZ) / (2 * HZ))


def slab(m):
    bz = [-HZ + i * 2 * HZ / BOARDS for i in range(BOARDS + 1)]
    xs = [-HX, -RX, RX, HX]
    zs = sorted(set([-HZ, -RZ, RZ, HZ] + bz))
    # the top, split at the boards' joints and round the runner's opening
    for i in range(len(xs) - 1):
        for j in range(len(zs) - 1):
            x0, x1, z0, z1 = xs[i], xs[i + 1], zs[j], zs[j + 1]
            if -RX <= x0 and x1 <= RX and -RZ <= z0 and z1 <= RZ:
                continue
            pts = [(x0, TOP, z0), (x1, TOP, z0), (x1, TOP, z1), (x0, TOP, z1)]
            m.oquad(pts, Y, "top", [top_f(p[0], p[2]) for p in pts])
    # the long edges (side grain, from the outer boards' figure), their bevels; the ends (end grain)
    for sz in (-1, 1):
        z, zb = sz * HZ, sz * (HZ - BEV)
        v = 0.012 if sz < 0 else 0.988
        m.oquad([(-HX, TOP, z), (HX, TOP, z), (HX, TOP - EDGE, z), (-HX, TOP - EDGE, z)], (0, 0, sz), "top",
                [(0, v), (1, v), (1, v), (0, v)])
        m.oquad([(-HX, TOP - EDGE, z), (HX, TOP - EDGE, z), (HX - BEV, UNDER, zb), (-HX + BEV, UNDER, zb)], (0, -1, sz), "top",
                [(0, v), (1, v), (1, v), (0, v)])
    for sx in (-1, 1):
        x, xb = sx * HX, sx * (HX - BEV)
        m.oquad([(x, TOP, -HZ), (x, TOP, HZ), (x, TOP - EDGE, HZ), (x, TOP - EDGE, -HZ)], (sx, 0, 0), "end",
                [(0, 0), (1, 0), (1, 0.1), (0, 0.1)])
        m.oquad([(x, TOP - EDGE, -HZ), (x, TOP - EDGE, HZ), (xb, UNDER, HZ - BEV), (xb, UNDER, -HZ + BEV)], (sx, -1, 0), "end",
                [(0, 0.3), (1, 0.3), (1, 0.4), (0, 0.4)])
    pts = [(-HX + BEV, UNDER, -HZ + BEV), (HX - BEV, UNDER, -HZ + BEV), (HX - BEV, UNDER, HZ - BEV), (-HX + BEV, UNDER, HZ - BEV)]
    m.oquad(pts, -Y, "top", [top_f(p[0], p[2]) for p in pts])
    # the runner's pocket: stainless walls down to the glass, graphite below it to the LED board
    walls = (((-RX, -RZ), (RX, -RZ), (0, 0, 1)), ((RX, RZ), (-RX, RZ), (0, 0, -1)),
             ((RX, -RZ), (RX, RZ), (-1, 0, 0)), ((-RX, RZ), (-RX, -RZ), (1, 0, 0)))
    for (x0, z0), (x1, z1), want in walls:
        m.oquad([(x0, TOP, z0), (x1, TOP, z1), (x1, GLASS_Y, z1), (x0, GLASS_Y, z0)], want, "steel")
        m.oquad([(x0, GLASS_Y, z0), (x1, GLASS_Y, z1), (x1, PCB_Y, z1), (x0, PCB_Y, z0)], want, "powder")
    first = len(m.quads)
    m.oquad([(-RX, PCB_Y, -RZ), (RX, PCB_Y, -RZ), (RX, PCB_Y, RZ), (-RX, PCB_Y, RZ)], Y, "dots",
            [(0, 0), (1, 0), (1, 1), (0, 1)], ao=False)
    # the two lines (live): tl tr br bl as the reader on that side sees them, the top edge towards the middle
    north = [(RX, DOT_Y, -GAP), (-RX, DOT_Y, -GAP), (-RX, DOT_Y, -GAP - ROWS * PITCH), (RX, DOT_Y, -GAP - ROWS * PITCH)]
    south = [(-RX, DOT_Y, GAP), (RX, DOT_Y, GAP), (RX, DOT_Y, GAP + ROWS * PITCH), (-RX, DOT_Y, GAP + ROWS * PITCH)]
    m.live.append((TICKER, 0, north))
    m.live.append((TICKER, 1, south))
    first = len(m.quads)
    m.quad([(-RX, GLASS_Y, -RZ), (RX, GLASS_Y, -RZ), (RX, GLASS_Y, RZ), (-RX, GLASS_Y, RZ)], "glass", ao=False)
    kind(m, first, GLASS)


def sleds(m):
    for sx in (-1, 1):
        c = sx * SLED_X
        x0, x1 = c - TUBE_W / 2, c + TUBE_W / 2
        box(m, (x0, 0.006, -SLED_Z), (x1, 0.006 + TUBE_T, SLED_Z), "powder")
        box(m, (x0, UNDER - TUBE_T, -SLED_Z), (x1, UNDER - 0.0005, SLED_Z), "powder", faces="nsewd")
        for sz in (-1, 1):
            za, zb = sorted((sz * SLED_Z, sz * (SLED_Z - TUBE_T)))
            box(m, (x0, 0.006 + TUBE_T, za), (x1, UNDER - TUBE_T, zb), "powder", faces="nsew")
            box(m, (x0 + 0.004, 0.0005, sz * 0.40 - 0.02), (x1 - 0.004, 0.006, sz * 0.40 + 0.02), "felt", faces="nsewd")
    box(m, (-SLED_X + TUBE_W / 2, UNDER - 0.024, -0.05), (SLED_X - TUBE_W / 2, UNDER - 0.0005, 0.05), "powder", faces="nsewd")
    # the amber LED lines under the long edges, facing down
    for sz in (-1, 1):
        z0, z1 = sorted((sz * 0.49, sz * 0.502))
        m.oquad([(-HX + 0.08, UNDER - 0.0015, z0), (HX - 0.08, UNDER - 0.0015, z0), (HX - 0.08, UNDER - 0.0015, z1),
                 (-HX + 0.08, UNDER - 0.0015, z1)], -Y, "amber", lit=True, ao=False)
    first = len(m.quads)
    for sz in (-1, 1):
        zc = sz * 0.46
        m.oquad([(-HX + 0.05, 0.004, zc - 0.26), (HX - 0.05, 0.004, zc - 0.26), (HX - 0.05, 0.004, zc + 0.26), (-HX + 0.05, 0.004, zc + 0.26)],
                Y, "wash", [(0, 0), (1, 0), (1, 1), (0, 1)], lit=True, ao=False)
    kind(m, first, GLOW)


# ------------------------------------------------------------------ what is on it

# The nuclear plant's mug (tools/plant_office4.mug, on its meeting and cafe tables and desks: Justin's pick), in its
# pixels (a sixteenth of a block) from its corner, the handle on its +x side: four walls on a bottom, coffee standing
# in it (the plant's coffee, its crema ring on top), a square handle. Faces where boxes meet are left out.
MUG_W, MUG_H, MUG_T = 2.4, 2.8, 0.3
MUG_BOXES = [
    ((0, 0, 0), (MUG_W, 0.3, MUG_W), "mug", "d"),
    ((0, 0, 0), (MUG_T, MUG_H, MUG_W), "mug", "nsweu"),
    ((MUG_W - MUG_T, 0, 0), (MUG_W, MUG_H, MUG_W), "mug", "nsweu"),
    ((MUG_T, 0, 0), (MUG_W - MUG_T, MUG_H, MUG_T), "mug", "nsu"),
    ((MUG_T, 0, MUG_W - MUG_T), (MUG_W - MUG_T, MUG_H, MUG_W), "mug", "nsu"),
    ((MUG_W, 0.7, 0.9), (MUG_W + 0.9, 0.95, 1.5), "mug", "nseud"),
    ((MUG_W + 0.65, 0.95, 0.9), (MUG_W + 0.9, 1.95, 1.5), "mug", "nsew"),
    ((MUG_W, 1.95, 0.9), (MUG_W + 0.9, 2.2, 1.5), "mug", "nseud"),
]
COFFEE = ((MUG_T + 0.02, MUG_H - 0.5, MUG_T + 0.02), (MUG_W - MUG_T - 0.02, MUG_W - MUG_T - 0.02))


def mug(m, cx, cz, yaw):
    """The plant's mug on the top, its middle at (cx, cz), its handle turned `yaw` (radians) from +x."""
    ax = np.array([math.cos(yaw), 0, math.sin(yaw)])
    az = np.cross(ax, Y)
    base = np.array([cx, TOP + 0.0005, cz])

    def at(p):
        p = np.asarray(p, float) / 16 - np.array([MUG_W / 32, 0, MUG_W / 32])
        return base + ax * p[0] + Y * p[1] + az * p[2]
    for lo, hi, mat, faces in MUG_BOXES:
        lo, hi = np.array(lo, float), np.array(hi, float)
        c = at((lo + hi) / 2)
        h = (hi - lo) / 32
        m.obox(c, ax, Y, h[0], h[1], h[2], mat, faces=faces)
    (x0, y, z0), (x1, z1) = COFFEE
    m.oquad([at((x0, y, z0)), at((x1, y, z0)), at((x1, y, z1)), at((x0, y, z1))], Y, "coffee", [(0, 0), (1, 0), (1, 1), (0, 1)])


def flat_box(m, c, yaw, hx, hz, y0, y1, top, side, top_uv=None, lit_top=False):
    """A thin level box turned `yaw` about y (its top `top`, its sides `side`)."""
    ax = np.array([math.cos(yaw), 0, math.sin(yaw)])
    az = np.array([-math.sin(yaw), 0, math.cos(yaw)])
    c = np.array(c, float)

    def P(sx, y, sz):
        return c + ax * hx * sx + az * hz * sz + Y * y
    first = len(m.quads)
    m.oquad([P(-1, y1, -1), P(1, y1, -1), P(1, y1, 1), P(-1, y1, 1)], Y, top, top_uv or [(0, 0), (1, 0), (1, 1), (0, 1)], lit=lit_top)
    if lit_top:
        m.quads[first]["ao"] = False
    for (a, b, d) in (((-1, -1), (1, -1), -az), ((1, 1), (-1, 1), az), ((1, -1), (1, 1), ax), ((-1, 1), (-1, -1), -ax)):
        m.oquad([P(a[0], y1, a[1]), P(b[0], y1, b[1]), P(b[0], y0, b[1]), P(a[0], y0, a[1])], d, side)


def magazines(m, cx, cz, yaw):
    for i, (dy, dx, dz) in enumerate(((0.0, 0.0, 0.0), (0.12, 0.012, -0.008), (-0.07, -0.006, 0.010))):
        y0 = TOP + 0.0015 + i * 0.0055
        flat_box(m, (cx + dx, 0, cz + dz), yaw + dy, 0.105, 0.140, y0, y0 + 0.0050, f"cover{2 - i}", "pages",
                 [(1, 1), (0, 1), (0, 0), (1, 0)])


def tablet(m, cx, cz, yaw):
    y = TOP + 0.0015
    flat_box(m, (cx, 0, cz), yaw, 0.1225, 0.085, y, y + 0.0070, "tab_bezel", "tab_back")
    flat_box(m, (cx, 0, cz), yaw, 0.1125, 0.075, y + 0.0070, y + 0.0072, "screen", "tab_bezel", [(1, 1), (0, 1), (0, 0), (1, 0)], lit_top=True)


def model_stand(m):
    """The T1 model's stand: a black base and a stainless post (the drone itself is drawn by the renderer)."""
    flat_box(m, (MODEL_X, 0, MODEL_Z), math.radians(MODEL_YAW), 0.07, 0.07, TOP + 0.0005, TOP + 0.018, "stand", "stand")
    m.cylinder((MODEL_X, TOP + 0.018, MODEL_Z), (MODEL_X, MODEL_Y, MODEL_Z), 0.004, "steel", n=8)


def table():
    m = TCC.Mesh()
    slab(m)
    sleds(m)
    mug(m, -0.55, -0.33, math.radians(200))
    mug(m, 0.95, 0.36, math.radians(-30))
    magazines(m, -1.25, 0.32, math.radians(6))
    tablet(m, 1.15, -0.33, math.radians(-12))
    model_stand(m)
    return m


# ------------------------------------------------------------------ writing

def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE and q["uvf"] is None:
        P_ = q["P"]
        sw = min(np.linalg.norm(P_[1] - P_[0]) * 2.0, 0.9)
        sh = min(np.linalg.norm(P_[3] - P_[0]) * 2.0, 0.9)
        TCC._seed[0] = (TCC._seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        s = TCC._seed[0]
        ou = (s % 997) / 997 * (1 - sw)
        ov = ((s // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = q["uvf"] or [(0.25, 0.25), (0.75, 0.25), (0.75, 0.75), (0.25, 0.75)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path):
    """The console's mesh format ("JTCM" 1, client/ConsoleMesh), with the glass and glow kinds; no step edges."""
    path.parent.mkdir(parents=True, exist_ok=True)
    TCC._seed[0] = 0
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(m.quads)))
    for q in m.quads:
        P_ = q["P"]
        uv = _uvs(atlas, q)
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in q["n"]]
        sh = q.get("shade", [1.0] * 4)
        buf += struct.pack(">b", q["kind"])
        for k in (0, 3, 2, 1):
            buf += struct.pack(">fffbbbffB", P_[k][0], P_[k][1], P_[k][2], nb[0], nb[1], nb[2], uv[k][0], uv[k][1],
                               int(round(255 * max(0.0, min(1.0, sh[k])))))
    buf += struct.pack(">i", len(m.live))
    for kind_, ident, corners in m.live:
        buf += struct.pack(">bb", kind_, ident)
        for p in corners:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">i", 0)
    for mat in ("steel", "amber", "steel"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def write_data():
    j = TCC._json
    model = f"jugcraft:block/{ID}"
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {f"facing={d}": {"model": model} for d in ("north", "east", "south", "west")}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": model}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["SSS", "GCG", "P P"], {
        "S": "minecraft:jungle_slab", "G": "minecraft:tinted_glass", "C": "jugcraft:advanced_circuit", "P": "#c:plates/steel"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Long Coffee Table needs a clear space 4 blocks by 2"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, PART_ID):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write_all():
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    m = table()
    TCC.bake(m, floor_y=0.0, res=1 / 32, reach=0.45, rays=16)
    write_mesh(m, atlas, MESH)
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m


if __name__ == "__main__":
    m = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(len(m.quads), "quads", {k: kinds.count(k) for k in sorted(set(kinds))}, len(m.live), "ticker lines")
