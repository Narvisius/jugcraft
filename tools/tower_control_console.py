"""The Tower Control Console, the Console Chair and the Raised Floor: the Tri-Pillar's master console redone (work list
item 6, approved by Justin on 22 October 2026 from the second, "more realistic" mockup sheet; see
claude/jugcraft-tripillar-master-console.md).

A curved three-position console north of the Holo Command Table, built the way control-room furniture is built: a light
laminate worksurface with a black soft edge; a technology riser at the back carrying the monitor rail, inset clock and
annunciator modules, a task light under its lip and an amber accent line; vented equipment-bay doors towards the table
with backlit position signs (FLEET + JOBS, TOWER CONTROL, POWER + SUPPLY); equipment pedestals in the knee space,
footrests, light end panels. Each position has three 27-inch monitors on articulated arms, a keyboard and mouse, a
voice-loop touch panel, a desk phone (the side positions) or the TOWER CONTROL switch box (guarded UPGRADE ARM and
RECALL ALL, the MODE knob, eight annunciator lamps), a notepad, a mug and a headset on a hook. The console stands on a
carpeted raised floor whose curved front step (aluminium nosing, amber step light) follows the table.

One item places the console (tower/TowerControlConsoleBlock.java): its parts are the cells of the floor band it stands
on ({@link footprint}), part 0 the middle cell under the TOWER CONTROL position, holding the pinned depot display.
Nothing is drawn by block models: client/TowerControlConsoleRenderer draws this mesh (shaded per corner: ambient
occlusion baked from the mesh, the task lights' pools on the desk) and the live parts named in it: nine monitor pages,
three loop panels, three clocks, three annunciator modules, the switch box's lamps, two phone displays; and, where the
band's floor ends at a cell edge with nothing to carry it on, the floor's step edge.

The Console Chair (tower/ConsoleChairBlock.java) is a separate block: 16 rotations, drawn from its own mesh by
client/ConsoleChairRenderer, sit-able; set on a Raised Floor it takes the floor's place (and draws the floor tile). The
Raised Floor (tower/RaisedFloorBlock.java) is a 3 px carpeted block with the nosing and step light on any side that
steps down.

Frame of the console's mesh: the master cell's corner at the origin, the hall floor at y 0, the console facing north
(its operators sit to the north and look south, over the monitors, to the table); the arc's axis (the table's in the
tower) at (0.5, 9.5). Angles are measured from east towards south, as in the tower's tools (north is 270 degrees).
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
MESH = ASSETS / "tower_mesh/tower_control_console.bin.gz"
CHAIR_MESH = ASSETS / "tower_mesh/console_chair.bin.gz"
TEXTURE = ASSETS / "textures/entity/tower_control_console.png"
ID, NAME = "tower_control_console", "Tower Control Console"
CHAIR_ID, CHAIR_NAME = "console_chair", "Console Chair"
FLOOR_ID, FLOOR_NAME = "raised_floor", "Raised Floor"

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"

# ------------------------------------------------------------------ dimensions (blocks)

AX, AZ = 0.5, 9.5                 # the arc's axis in the master cell's frame
A0, A1 = math.radians(240), math.radians(300)
POS_SPAN = (A1 - A0) / 3
PLATFORM = 3 / 16                 # the raised floor's top (the Raised Floor block is 3 px)
YD = PLATFORM
WS = YD + 0.76                    # worksurface top
WT = 0.04                         # worksurface thickness
RB = 8.72                         # the console's back (towards the table)
RM = RB + 0.42                    # the rear bay's operator face (modesty panel)
RE = 9.86                         # the operator edge
BN = 0.035                        # the soft edge's radius
DR0 = 8.25                        # the floor's curved front step
NOSE = 0.035                      # the nosing's depth
BAND_R = 10.0                     # the band of floor the console's parts cover reaches this far out
HALF_W = 5.5                      # and is this wide each side of the axis (the north nave's walls, in the tower)
POSITIONS = [(-1, "FLEET + JOBS", "legend_w"), (0, "TOWER CONTROL", "legend_c"), (1, "POWER + SUPPLY", "legend_e")]
Y = np.array([0, 1.0, 0])

# live faces (the renderer's kinds; keep in sync with client/TowerControlConsoleMesh)
MONITOR, KEYSET, CLOCK, ANNUNCIATOR, LAMP, LCD = 0, 1, 2, 3, 4, 5
# monitor pages (client/TowerControlConsoleRenderer.PAGES)
PAGES = ["FLEET", "SITE", "JOBS", "TOWER", "ALERTS", "BAYS", "POWER", "SUPPLY", "LINK"]
SCREEN_PAGES = {-1: ["FLEET", "SITE", "JOBS"], 0: ["TOWER", "ALERTS", "BAYS"], 1: ["POWER", "SUPPLY", "LINK"]}
LAMPS = ["LOW PWR", "PICKUP", "PACKAGER", "FLEET FULL", "BUILDING", "LINK", "EXCH E", "EXCH C"]

SOLID, LIT = 0, 1


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def P(r, y, a):
    return np.array([AX + r * math.cos(a), y, AZ + r * math.sin(a)])


def O(a):
    return np.array([math.cos(a), 0, math.sin(a)])


def T(a):
    return np.array([-math.sin(a), 0, math.cos(a)])


def mid(k):
    return A0 + POS_SPAN * (k + 1.5)


# ------------------------------------------------------------------ footprint

def cell_r(dx, dz):
    """The nearest and farthest distance of cell (dx, dz) from the axis."""
    x0, x1, z0, z1 = dx - AX, dx + 1 - AX, dz - AZ, dz + 1 - AZ
    nx = min(max(0.0, x0), x1)
    nz = min(max(0.0, z0), z1)
    fx = max(abs(x0), abs(x1))
    fz = max(abs(z0), abs(z1))
    return math.hypot(nx, nz), math.hypot(fx, fz)


def footprint():
    """The cells of the floor band the console stands on, as (dx, dz) from the master cell (facing north): every cell
    north of the axis, within HALF_W of it east and west, that the band DR0..BAND_R touches. The master first, then row
    by row from the north-west (tower/TowerControlConsoleBlock.FOOTPRINT builds the same list)."""
    out = [(0, 0)]
    for dz in range(-3, 10):
        for dx in range(-6, 7):
            if (dx, dz) == (0, 0):
                continue
            if dx < AX - HALF_W - 1e-9 or dx + 1 > AX + HALF_W + 1e-9 or dz + 1 > AZ:
                continue
            near, far = cell_r(dx, dz)
            if near < BAND_R - 1e-6 and far > DR0 + 1e-6:
                out.append((dx, dz))
    return out


# ------------------------------------------------------------------ textures: one sheet

ATLAS_W, ATLAS_H = 2048, 1024
NOISE_TILE = 64


def _noise(base, amount, seed, streak=None, size=NOISE_TILE):
    rnd = np.random.default_rng(seed)
    n = rnd.uniform(-1, 1, (size, size)) * amount
    if streak == "h":
        n = n * 0.35 + rnd.uniform(-1, 1, (size, 1)) * amount * 0.9
    a = np.zeros((size, size, 4), np.uint8)
    for i in range(3):
        a[..., i] = np.clip(base[i] + n, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _solid(rgb, size=16):
    return Image.new("RGBA", (size, size), tuple(rgb) + (255,))


def _door(w, h, base, led, seed):
    rnd = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rnd.uniform(-4, 4, (h, w))
    for i in range(3):
        a[..., i] = np.clip(base[i] + n, 0, 255)
    a[..., 3] = 255
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, w - 1, h - 1), outline=(36, 38, 42, 255))
    d.line((1, 1, w - 2, 1), fill=tuple(min(255, c + 22) for c in base) + (255,))
    for y in range(int(h * 0.18), int(h * 0.82), 4):
        for x in range(int(w * 0.16) + (y // 4 % 2) * 2, int(w * 0.84), 4):
            d.rectangle((x, y, x + 1, y + 1), fill=(30, 32, 35, 255))
    d.rounded_rectangle((w - 13, h // 2 - 12, w - 8, h // 2 + 12), radius=2, fill=(44, 46, 50, 255))
    d.line((w - 12, h // 2 - 11, w - 12, h // 2 + 11), fill=(150, 154, 160, 255))
    d.rectangle((6, 6, 8, 8), fill=led + (255,))
    return img


def _carpet(size=64, seed=11):
    """A charcoal carpet tile (half a block): fine loop pile, darker seams on two edges (the next tile has the others)."""
    rnd = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.float32)
    n = rnd.normal(0, 5.5, (size, size))
    rows = np.sin(np.arange(size) * 2.2)[:, None] * 1.5
    for i, b in enumerate((50, 54, 62)):
        a[..., i] = np.clip(b + n + rows, 0, 255)
    a[0, :, :3] *= 0.72
    a[:, 0, :3] *= 0.72
    a[..., 3] = 255
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def _riser_back():
    t = np.asarray(_noise((56, 59, 64), 2.5, 55)).astype(np.float32)
    t[:, 0, :3] *= 0.5
    t[:, 1, :3] = np.clip(t[:, 1, :3] * 1.25, 0, 255)
    return Image.fromarray(t.astype(np.uint8), "RGBA")


def _mesh_weave(size=64):
    img = Image.new("RGBA", (size, size), (30, 31, 35, 255))
    d = ImageDraw.Draw(img)
    for i in range(-size, size * 2, 4):
        d.line((i, 0, i + size, size), fill=(54, 56, 61, 255))
        d.line((i, size, i + size, 0), fill=(46, 48, 53, 255))
    return img


def _keyboard(w=354, h=118):
    img = Image.new("RGBA", (w, h), (44, 46, 50, 255))
    d = ImageDraw.Draw(img)
    u = 15.0

    def key(x, y, kw=1.0, kh=1.0):
        x0, y0 = 8 + x * u, 10 + y * u * 1.02
        d.rounded_rectangle((x0 + 0.8, y0 + 0.8, x0 + kw * u - 0.8, y0 + kh * u - 0.8), radius=2, fill=(24, 25, 28, 255))
        d.line((x0 + 3, y0 + 3, x0 + 5, y0 + 3), fill=(150, 152, 156, 255))
    key(0, 0)
    for i in range(12):
        key(2 + i + (i // 4) * 0.5, 0)
    rows = [[1] * 13 + [2], [1.5] + [1] * 12 + [1.5], [1.75] + [1] * 11 + [2.25], [2.25] + [1] * 10 + [2.75],
            [1.25, 1.25, 1.25, 6.25, 1.25, 1.25, 1.25, 1.25]]
    for r, row in enumerate(rows):
        x = 0
        for kw in row:
            key(x, 1.5 + r, kw)
            x += kw
    for c in range(3):
        for r in range(2):
            key(15.25 + c, 1.5 + r)
    key(16.25, 4.5)
    for c in range(3):
        key(15.25 + c, 5.5)
    for c in range(4):
        for r in range(5):
            if (c == 3 and r in (2, 4)) or (c in (0, 1) and r == 4):
                continue
            key(18.5 + c, 1.5 + r, 1, 2 if c == 3 and r in (1, 3) else 1)
    key(18.5, 5.5, 2)
    return img


def _phone_top(w=190, h=200):
    img = Image.new("RGBA", (w, h), (40, 42, 46, 255))
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype(FB, 9)
    for r in range(4):
        for c in range(3):
            x0, y0 = 70 + c * 34, 92 + r * 25
            d.rounded_rectangle((x0, y0, x0 + 28, y0 + 19), radius=4, fill=(64, 66, 70, 255))
            d.text((x0 + 14, y0 + 10), "123456789*0#"[r * 3 + c], font=f, fill=(220, 220, 224, 255), anchor="mm")
    for r in range(6):
        d.rounded_rectangle((164, 20 + r * 12, 182, 28 + r * 12), radius=2, fill=(70, 72, 76, 255))
    d.rounded_rectangle((70, 190, 172, 196), radius=2, fill=(28, 29, 31, 255))
    return img


def _keyset(title):
    w, h = 270, 180
    img = Image.new("RGBA", (w, h), (10, 14, 18, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, w, 20), fill=(22, 28, 34, 255))
    d.text((8, 10), title, font=ImageFont.truetype(FB, 11), fill=(255, 226, 180, 255), anchor="lm")
    d.text((w - 8, 10), "TALK FD", font=ImageFont.truetype(FB, 10), fill=(90, 230, 120, 255), anchor="rm")
    names = ["FD", "TWR", "FLT", "PWR", "SUP", "JOBS", "BAY A", "BAY B", "BAY C", "YARD", "EXCH", "ALL"]
    f = ImageFont.truetype(FB, 10)
    for i, n in enumerate(names):
        x0, y0 = 8 + (i % 4) * 65, 28 + (i // 4) * 50
        talk, mon = i == 0, i in (2, 4, 9)
        fill = (60, 170, 90) if talk else (150, 110, 40) if mon else (34, 40, 46)
        d.rounded_rectangle((x0, y0, x0 + 58, y0 + 42), radius=5, fill=fill + (255,), outline=(70, 80, 88, 255))
        d.text((x0 + 29, y0 + 15), n, font=f, fill=(10, 14, 12, 255) if (talk or mon) else (200, 206, 212, 255), anchor="mm")
        d.text((x0 + 29, y0 + 31), "TALK" if talk else "MON" if mon else "", font=ImageFont.truetype(FB, 8),
               fill=(10, 14, 12, 255), anchor="mm")
    return img


KEYSET_TALK = (8 / 270, 28 / 180, 66 / 270, 70 / 180)        # the FD TALK key on the panel (fractions), pulsed live


def _switch_top(w=480, h=300):
    img = Image.new("RGBA", (w, h), (52, 56, 61, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, w - 1, h - 1), outline=(30, 32, 35, 255), width=3)
    for (x, y) in ((10, 10), (w - 14, 10), (10, h - 14), (w - 14, h - 14)):
        d.ellipse((x, y, x + 5, y + 5), fill=(110, 114, 120, 255))
    f = ImageFont.truetype(FB, 15)
    d.text((w / 2, 22), "TOWER CONTROL", font=ImageFont.truetype(FB, 17), fill=(255, 196, 120, 255), anchor="mm")
    d.line((20, 36, w - 20, 36), fill=(90, 94, 100, 255), width=2)
    for i, n in enumerate(["UPGRADE ARM", "RECALL ALL", "MODE"]):
        x = 85 + i * 155
        d.text((x, 272), n, font=f, fill=(230, 232, 235, 255), anchor="mm")
        if i < 2:
            d.rectangle((x - 46, 168, x + 46, 252), outline=(220, 200, 60, 255), width=3)
            for k in range(0, 92, 12):
                d.line((x - 46 + k, 252, x - 46 + k + 6, 246), fill=(220, 200, 60, 255), width=2)
    d.text((385, 168), "AUTO", font=ImageFont.truetype(FB, 12), fill=(210, 212, 216, 255), anchor="mm")
    d.text((440, 205), "MAN", font=ImageFont.truetype(FB, 12), fill=(210, 212, 216, 255), anchor="mm")
    d.text((330, 205), "HOLD", font=ImageFont.truetype(FB, 12), fill=(210, 212, 216, 255), anchor="mm")
    return img


def _paper():
    img = Image.new("RGBA", (120, 168), (238, 236, 226, 255))
    d = ImageDraw.Draw(img)
    for y in range(22, 168, 9):
        d.line((6, y, 114, y), fill=(170, 190, 214, 255))
    d.line((18, 0, 18, 168), fill=(220, 150, 150, 255))
    rnd = np.random.default_rng(5)
    for row in range(7):
        y = 20 + row * 9
        x = 22
        end = 22 + rnd.uniform(40, 90)
        while x < end:
            ww = rnd.uniform(6, 16)
            d.line((x, y - 1, x + ww, y - 2), fill=(40, 50, 90, 255))
            x += ww + 4
    return img


def _legend(text):
    img = Image.new("RGBA", (704, 64), (14, 15, 17, 255))
    ImageDraw.Draw(img).text((352, 32), text, font=ImageFont.truetype(FB, 38), fill=(255, 190, 100, 255), anchor="mm")
    return img


def _screen_glass():
    return _solid((7, 11, 15), 32)


def _lcd():
    return _solid((150, 176, 186), 32)


def _ann_face():
    """The annunciator module's face: six dark lamp windows (the renderer lights them)."""
    w, h = 220, 80
    img = Image.new("RGBA", (w, h), (20, 22, 24, 255))
    d = ImageDraw.Draw(img)
    for i in range(6):
        x0, y0 = 6 + (i % 3) * 71, 6 + (i // 3) * 36
        d.rectangle((x0, y0, x0 + 66, y0 + 31), fill=(34, 34, 34, 255), outline=(8, 8, 8, 255))
    return img


def tiles():
    t = {
        "ws_top": _noise((172, 175, 177), 2.5, 31),
        "ws_under": _noise((112, 115, 118), 3, 32),
        "bull": _noise((22, 23, 25), 2, 33),
        "cab": _noise((78, 82, 88), 3, 34),
        "riser": _noise((54, 57, 62), 2.5, 35),
        "riser_back": _riser_back(),
        "door": _door(96, 96, (86, 90, 97), (80, 230, 120), 3),
        "door_ped": _door(96, 128, (80, 84, 90), (255, 170, 60), 9),
        "kick": _noise((17, 18, 19), 1.5, 36),
        "alu": _noise((176, 181, 188), 9, 4, "h"),
        "alu_dark": _noise((98, 103, 110), 6, 5, "h"),
        "task": _solid((255, 246, 226)),
        "amber": _solid((255, 168, 58)),
        "carpet": _carpet(),
        "fascia": _noise((46, 48, 52), 2, 37),
        "endpanel": _noise((146, 151, 157), 4, 38, "h"),
        "edge": _noise((20, 21, 23), 1, 56),
        "fabric": _noise((44, 45, 49), 7, 39),
        "mesh": _mesh_weave(),
        "nylon": _noise((38, 40, 44), 3, 40),
        "pu": _noise((27, 28, 30), 3, 41),
        "chrome": _noise((190, 194, 200), 10, 42, "h"),
        "mon": _noise((16, 17, 19), 1.5, 43),
        "mon_back": _noise((32, 33, 36), 2, 44),
        "arm": _noise((70, 73, 78), 4, 45, "h"),
        "kb_body": _noise((40, 42, 45), 2, 46),
        "kb_top": _keyboard(),
        "mouse": _noise((22, 23, 25), 2, 47),
        "pad": _noise((28, 30, 33), 3, 48),
        "phone": _noise((36, 38, 42), 2, 49),
        "phone_top": _phone_top(),
        "lcd": _lcd(),
        "keyset": _keyset("LOOPS"),
        "keyset_b": _keyset("LOOPS  B"),
        "switch_top": _switch_top(),
        "guard": _noise((178, 32, 28), 6, 50),
        "toggle": _solid((205, 208, 214)),
        "knob": _noise((24, 25, 27), 2, 51),
        "paper": _paper(),
        "pen": _solid((26, 40, 110)),
        "mug": _noise((232, 232, 228), 3, 52),
        "coffee": _solid((52, 32, 20)),
        "hs": _noise((26, 27, 29), 2, 53),
        "legend_c": _legend("TOWER CONTROL"), "legend_w": _legend("FLEET + JOBS"), "legend_e": _legend("POWER + SUPPLY"),
        "clock": _solid((12, 12, 14), 32),
        "ann": _ann_face(),
        "module": _noise((34, 36, 40), 2, 54),
        "screen": _screen_glass(),
        "white": _solid((255, 255, 255)),
    }
    return t


class Atlas:
    """Tiles packed on shelves; uv(name, u, v) maps 0..1 within a tile (half a texel in from its edges)."""

    def __init__(self, t):
        self.img = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
        self.box = {}
        x = y = shelf = 0
        for name, im in sorted(t.items(), key=lambda kv: -kv[1].height):
            if x + im.width > ATLAS_W:
                x, y, shelf = 0, y + shelf + 2, 0
            if y + im.height > ATLAS_H:
                raise ValueError("atlas full")
            self.img.paste(im, (x, y))
            self.box[name] = (x, y, im.width, im.height)
            x += im.width + 2
            shelf = max(shelf, im.height)

    def uv(self, name, u, v):
        x, y, w, h = self.box[name]
        u = min(max(u, 0.0), 1.0)
        v = min(max(v, 0.0), 1.0)
        return ((x + 0.5 + u * (w - 1)) / ATLAS_W, (y + 0.5 + v * (h - 1)) / ATLAS_H)


NOISE = {"ws_top", "ws_under", "bull", "cab", "riser", "kick", "alu", "alu_dark", "fascia", "endpanel", "edge", "fabric",
         "mesh", "nylon", "pu", "chrome", "mon", "mon_back", "arm", "kb_body", "mouse", "pad", "phone", "guard", "knob",
         "mug", "hs", "module"}


# ------------------------------------------------------------------ the mesh

class Mesh:
    """Quads as (kind, corners tl/tr/br/bl as seen from the front, material, uv fractions within the tile, ao?). Noise
    materials take a patch the quad's size at a varying offset (they never stretch or wrap); the others take the
    fractions given (default: the whole tile)."""

    def __init__(self):
        self.quads = []
        self.live = []          # (kind, id, [tl, tr, br, bl])

    def quad(self, pts, mat, uvf=None, lit=False, ao=True):
        P_ = np.asarray(pts, float)
        n = np.zeros(3)
        for i in range(4):
            a, b = P_[i], P_[(i + 1) % 4]
            n += np.array([(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])])
        n = -n
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            return
        self.quads.append(dict(kind=LIT if lit else SOLID, P=P_, mat=mat, uvf=uvf, n=n / ln, ao=ao and not lit))

    def oquad(self, pts, want, mat, uvf=None, lit=False, ao=True):
        p = np.asarray(pts, float)
        n = np.cross(p[3] - p[0], p[1] - p[0])
        if np.linalg.norm(n) < 1e-12:
            n = np.cross(p[2] - p[0], p[1] - p[0])
        if n @ np.asarray(want, float) < 0:
            p = p[[1, 0, 3, 2]]
            if uvf is not None:
                uvf = [uvf[1], uvf[0], uvf[3], uvf[2]]
        self.quad(p, mat, uvf, lit, ao)

    def obox(self, c, ax, ay, hx, hy, hz, mat, faces="nsewud", lit=False):
        c = np.asarray(c, float)
        ax, ay = unit(ax), unit(ay)
        az = np.cross(ax, ay)
        X, Y_, Z = ax * hx, ay * hy, az * hz

        def Q(sx, sy, sz):
            return c + X * sx + Y_ * sy + Z * sz
        F = {"u": [Q(-1, 1, -1), Q(1, 1, -1), Q(1, 1, 1), Q(-1, 1, 1)],
             "d": [Q(-1, -1, 1), Q(1, -1, 1), Q(1, -1, -1), Q(-1, -1, -1)],
             "n": [Q(1, 1, -1), Q(-1, 1, -1), Q(-1, -1, -1), Q(1, -1, -1)],
             "s": [Q(-1, 1, 1), Q(1, 1, 1), Q(1, -1, 1), Q(-1, -1, 1)],
             "e": [Q(1, 1, 1), Q(1, 1, -1), Q(1, -1, -1), Q(1, -1, 1)],
             "w": [Q(-1, 1, -1), Q(-1, 1, 1), Q(-1, -1, 1), Q(-1, -1, -1)]}
        for f in faces:
            self.quad(F[f], mat, lit=lit)

    def cylinder(self, a, b, r, mat, n=12):
        a, b = np.asarray(a, float), np.asarray(b, float)
        d = b - a
        L = np.linalg.norm(d)
        if L < 1e-9:
            return
        w = d / L
        u = unit(np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0)))
        v = np.cross(w, u)
        ring = [(u * math.cos(2 * math.pi * i / n) + v * math.sin(2 * math.pi * i / n)) * r for i in range(n)]
        for i in range(n):
            p, q = ring[i], ring[(i + 1) % n]
            self.quad([b + q, b + p, a + p, a + q], mat)
            self.quad([b, b, b + q, b + p], mat)
            self.quad([a, a, a + p, a + q], mat)

    def drum(self, cx, cz, r, y0, y1, mat, n=16, outward=True):
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            p0 = (cx + r * math.cos(t0), cz + r * math.sin(t0))
            p1 = (cx + r * math.cos(t1), cz + r * math.sin(t1))
            if outward:
                pts = [(p1[0], y1, p1[1]), (p0[0], y1, p0[1]), (p0[0], y0, p0[1]), (p1[0], y0, p1[1])]
            else:
                pts = [(p0[0], y1, p0[1]), (p1[0], y1, p1[1]), (p1[0], y0, p1[1]), (p0[0], y0, p0[1])]
            self.quad(pts, mat)

    def ring(self, cx, cz, r0, r1, y, mat, n=16):
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            c0, s0, c1, s1 = math.cos(t0), math.sin(t0), math.cos(t1), math.sin(t1)
            pts = [(cx + r1 * c0, y, cz + r1 * s0), (cx + r1 * c1, y, cz + r1 * s1), (cx + r0 * c1, y, cz + r0 * s1),
                   (cx + r0 * c0, y, cz + r0 * s0)]
            self.oquad(pts, Y, mat)

    def disc(self, cx, cz, r, y, mat, n=16):
        c = np.array([cx, y, cz])
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            p0 = c + r * np.array([math.cos(t0), 0, math.sin(t0)])
            p1 = c + r * np.array([math.cos(t1), 0, math.sin(t1)])
            self.oquad([c, p0, p1, p1], Y, mat)


def sweep(m, prof, a0, a1, segs, n):
    """A profile in (r, y), anticlockwise round the solid (r to the right, y up), swept round the axis from a0 to a1 in
    n steps. segs[j] (the face between prof[j] and prof[j+1]): dict(mat, lit, sub=profile step, rep=texture repeats
    round the arc for fitted materials, fitv=True) or None."""
    for j in range(len(prof) - 1):
        sg = segs[j]
        if sg is None:
            continue
        (r0, y0), (r1, y1) = prof[j], prof[j + 1]
        L = math.hypot(r1 - r0, y1 - y0)
        if L < 1e-6:
            continue
        k = max(1, int(math.ceil(L / sg.get("sub", 0.12))))
        nr, ny = (y1 - y0) / L, -(r1 - r0) / L
        rep = sg.get("rep")
        for i in range(n):
            t0, t1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
            want = nr * O((t0 + t1) / 2) + ny * Y
            if rep:
                per = n // rep
                u0, u1 = (i % per) / per, (i % per + 1) / per
            else:
                u0, u1 = 0, 1
            for s in range(k):
                f0, f1 = s / k, (s + 1) / k
                ra, ya = r0 + (r1 - r0) * f0, y0 + (y1 - y0) * f0
                rb, yb = r0 + (r1 - r0) * f1, y0 + (y1 - y0) * f1
                pts = [P(ra, ya, t0), P(ra, ya, t1), P(rb, yb, t1), P(rb, yb, t0)]
                uvf = [(u0, f0), (u1, f0), (u1, f1), (u0, f1)] if rep else None
                m.oquad(pts, want, sg["mat"], uvf, lit=sg.get("lit", False))


def plate(m, c, right, up, w, h, depth, face, body, lit=False, live=None):
    """A plate whose front (right x up) carries `face` (or, with live=(kind, id), a live face the renderer draws); its
    body behind it has no front face (no z-fighting)."""
    r, u = unit(right), unit(up)
    n = np.cross(r, u)
    c = np.asarray(c, float)
    m.obox(c, r, u, w / 2, h / 2, depth / 2, body, faces="nweud")
    f = c + n * depth / 2
    corners = [f - r * w / 2 + u * h / 2, f + r * w / 2 + u * h / 2, f + r * w / 2 - u * h / 2, f - r * w / 2 - u * h / 2]
    if face is not None:
        m.quad(corners, face, lit=lit)
    if live is not None:
        m.live.append((live[0], live[1], corners))
    return corners


def rbox(m, c, ax, ay, hx, hy, hz, mat, b=0.02):
    """A box with chamfered edges and corners."""
    c = np.asarray(c, float)
    ax, ay = unit(ax), unit(ay)
    az = np.cross(ax, ay)
    b = min(b, hx * 0.9, hy * 0.9, hz * 0.9)
    H = np.array([hx, hy, hz])
    A = [ax, ay, az]

    def W(p):
        return c + ax * p[0] + ay * p[1] + az * p[2]
    for axis in range(3):
        o1, o2 = [i for i in range(3) if i != axis]
        for s in (-1, 1):
            pts = []
            for (s1, s2) in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
                p = np.zeros(3)
                p[axis], p[o1], p[o2] = s * H[axis], s1 * (H[o1] - b), s2 * (H[o2] - b)
                pts.append(W(p))
            m.oquad(pts, A[axis] * s, mat)
    for axis in range(3):
        o1, o2 = [i for i in range(3) if i != axis]
        for s1 in (-1, 1):
            for s2 in (-1, 1):
                pts = []
                for (e, which) in ((-1, 0), (1, 0), (1, 1), (-1, 1)):
                    p = np.zeros(3)
                    p[axis] = e * (H[axis] - b)
                    if which == 0:
                        p[o1], p[o2] = s1 * H[o1], s2 * (H[o2] - b)
                    else:
                        p[o1], p[o2] = s1 * (H[o1] - b), s2 * H[o2]
                    pts.append(W(p))
                m.oquad(pts, A[o1] * s1 + A[o2] * s2, mat)
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                s = np.array([sx, sy, sz])
                pa = s * (H - b)
                q = []
                for i in range(3):
                    p = pa.copy()
                    p[i] = s[i] * H[i]
                    q.append(W(p))
                m.oquad([q[0], q[1], q[2], q[2]], ax * sx + ay * sy + az * sz, mat)


def bar(m, a, b, w, h, up, mat):
    a, b = np.asarray(a, float), np.asarray(b, float)
    d = b - a
    L = np.linalg.norm(d)
    ax = d / L
    ay = np.asarray(up, float) - ax * (np.asarray(up, float) @ ax)
    m.obox((a + b) / 2, ax, ay, L / 2, h / 2, w / 2, mat)


def L_(f, s, h, k):
    """A point on the desk of position k: f in from the operator edge, s to the operator's right, h over the top; and
    the frame there (in = towards the table, right, up)."""
    r = RE - f
    a = mid(k) - s / r
    return P(r, WS + h, a), -O(a), -T(a), Y.copy()


# ------------------------------------------------------------------ the floor band

def clip_circle(poly, R):
    """Sutherland-Hodgman: the part of a polygon (x, z points relative to the axis) with r >= R."""
    def inside(p):
        return math.hypot(p[0], p[1]) >= R - 1e-12

    def cut(p, q):
        d = (q[0] - p[0], q[1] - p[1])
        a = d[0] ** 2 + d[1] ** 2
        b = 2 * (p[0] * d[0] + p[1] * d[1])
        c = p[0] ** 2 + p[1] ** 2 - R * R
        disc = max(0.0, b * b - 4 * a * c)
        for t in ((-b + math.sqrt(disc)) / (2 * a), (-b - math.sqrt(disc)) / (2 * a)):
            if -1e-9 <= t <= 1 + 1e-9:
                return (p[0] + d[0] * t, p[1] + d[1] * t)
        return q
    out = []
    for i in range(len(poly)):
        p, q = poly[i], poly[(i + 1) % len(poly)]
        if inside(q):
            if not inside(p):
                out.append(cut(p, q))
            out.append(q)
        elif inside(p):
            out.append(cut(p, q))
    return out


def floor_band(m, cells):
    """The carpet over the console's cells (half-block tiles, each in eighths for the corner shading), clipped to the
    curved step; the step itself (nosing, step light, fascia) along the arc between the band's sides."""
    r_in = DR0 + NOSE
    sub = 4
    for (dx, dz) in cells:
        for qx in (0, 0.5):
            for qz in (0, 0.5):
                for i in range(sub):
                    for j in range(sub):
                        x0 = dx + qx + 0.5 * i / sub
                        z0 = dz + qz + 0.5 * j / sub
                        x1, z1 = x0 + 0.5 / sub, z0 + 0.5 / sub
                        corners = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
                        rel = [(x - AX, z - AZ) for x, z in corners]
                        rs = [math.hypot(*p) for p in rel]
                        if max(rs) < r_in:
                            continue
                        angs = [math.atan2(p[1], p[0]) % (2 * math.pi) for p in rel]
                        if min(rs) > RB + 0.05 and max(rs) < RM - 0.01 and min(angs) > A0 + 0.004 and max(angs) < A1 - 0.004:
                            continue     # under the rear bay
                        poly = rel if min(rs) >= r_in else clip_circle(rel, r_in)
                        if len(poly) < 3:
                            continue
                        pts = [np.array([p[0] + AX, YD, p[1] + AZ]) for p in poly]
                        uv = [((p[0] - dx - qx) / 0.5, (p[2] - dz - qz) / 0.5) for p in pts]
                        for t in range(1, len(pts) - 1, 2):
                            idx = [0, t, t + 1, min(t + 2, len(pts) - 1)]
                            if t + 2 > len(pts) - 1:
                                idx[3] = t + 1
                            m.oquad([pts[q] for q in idx], Y, "carpet", [uv[q] for q in idx])
    aw = math.pi + math.acos(HALF_W / DR0)
    ae = 2 * math.pi - math.acos(HALF_W / DR0)
    prof = [(DR0 + NOSE, YD), (DR0, YD), (DR0, YD - 0.035), (DR0 + 0.012, YD - 0.035), (DR0 + 0.012, YD - 0.062),
            (DR0, YD - 0.062), (DR0, 0.0)]
    segs = [dict(mat="alu", sub=1), dict(mat="alu", sub=1), dict(mat="alu_dark", sub=1), dict(mat="amber", lit=True, sub=1),
            dict(mat="fascia", sub=1), dict(mat="fascia", sub=0.07)]
    sweep(m, prof, aw, ae, segs, 96)


def band_edges(cells):
    """Where the band's floor reaches a cell edge with no console cell beyond it: (dx, dz, side, t0, t1). side 0 north,
    1 east, 2 south, 3 west (facing north); t runs along the edge west to east or north to south. The renderer draws
    the step there unless the cell beyond carries the floor on (a Raised Floor, a chair on one) or is a wall."""
    s = set(cells)
    out = []
    r_in = DR0 + NOSE
    for (dx, dz) in cells:
        for side, (nx, nz) in enumerate(((0, -1), (1, 0), (0, 1), (-1, 0))):
            if (dx + nx, dz + nz) in s:
                continue
            if side == 0:
                a, b = (dx, dz), (dx + 1, dz)
            elif side == 1:
                a, b = (dx + 1, dz), (dx + 1, dz + 1)
            elif side == 2:
                a, b = (dx, dz + 1), (dx + 1, dz + 1)
            else:
                a, b = (dx, dz), (dx, dz + 1)
            ts = [t for t in np.linspace(0, 1, 401) if math.hypot(a[0] + (b[0] - a[0]) * t - AX, a[1] + (b[1] - a[1]) * t - AZ) >= r_in]
            if not ts:
                continue
            out.append((dx, dz, side, float(min(ts)), float(max(ts))))
    return out


# ------------------------------------------------------------------ the console

def body(m):
    n = 72
    # rear bay: vented doors to the table (twelve), kick plate, the modesty panel towards the operators
    prof = [(RB + 0.04, YD), (RM, YD), (RM, WS - WT), (RB, WS - WT), (RB, YD + 0.08), (RB + 0.04, YD + 0.08), (RB + 0.04, YD)]
    sweep(m, prof, A0, A1, [None, dict(mat="cab"), None, dict(mat="door", rep=12, sub=1), dict(mat="kick"), dict(mat="kick")], n)
    # the worksurface and its soft edge
    sweep(m, [(RB - 0.012, WS - WT), (RE - 0.02, WS - WT), (RE - 0.02, WS), (RB - 0.012, WS), (RB - 0.012, WS - WT)], A0, A1,
          [dict(mat="ws_under", sub=0.3), None, dict(mat="ws_top", sub=0.1), dict(mat="alu")], n)
    bull = [(RE - 0.02, WS - 0.07), (RE, WS - 0.07)]
    for i in range(7):
        ph = -math.pi / 2 + math.pi * (i + 1) / 7
        bull.append((RE + BN * math.cos(ph), WS - BN + BN * math.sin(ph)))
    bull += [(RE - 0.02, WS), (RE - 0.02, WS - 0.07)]
    sweep(m, bull, A0, A1, [dict(mat="bull", sub=1)] * (len(bull) - 1), n)
    # the technology riser: back panel (seams, accent line), top, sloped face, lip, task light under the lip
    rp = [(RB, WS), (RB + 0.29, WS), (RB + 0.29, WS + 0.05), (RB + 0.33, WS + 0.05), (RB + 0.33, WS + 0.07),
          (RB + 0.18, WS + 0.22), (RB, WS + 0.22), (RB, WS + 0.205), (RB, WS + 0.195), (RB, WS)]
    sweep(m, rp, A0, A1, [None, dict(mat="riser"), dict(mat="task", lit=True), dict(mat="alu"), dict(mat="riser", sub=0.25),
                          dict(mat="riser", sub=0.2), dict(mat="alu"), dict(mat="amber", lit=True),
                          dict(mat="riser_back", rep=6, sub=1)], n)
    sweep(m, [(RB + 0.06, WS + 0.22), (RB + 0.10, WS + 0.22), (RB + 0.10, WS + 0.245), (RB + 0.06, WS + 0.245), (RB + 0.06, WS + 0.22)],
          A0 + 0.004, A1 - 0.004, [None, dict(mat="alu"), dict(mat="alu"), dict(mat="alu")], 36)
    # equipment pedestals in the knee space: both ends and between the positions
    dth = 0.58 / (RE - 0.1)
    for edge in range(4):
        a = A0 + POS_SPAN * edge
        lo, hi = (a, a + dth) if edge == 0 else (a - dth, a) if edge == 3 else (a - dth / 2, a + dth / 2)
        pp = [(RM, YD), (RE - 0.14, YD), (RE - 0.14, YD + 0.07), (RE - 0.10, YD + 0.07), (RE - 0.10, WS - WT)]
        sweep(m, pp, lo, hi, [None, dict(mat="kick"), dict(mat="kick"), dict(mat="door_ped", rep=1, sub=1)], 4)
        for ang, sign in ((lo, -1), (hi, 1)):
            if (edge == 0 and sign < 0) or (edge == 3 and sign > 0):
                continue
            for (r0, y0, r1, y1) in ((RM, YD + 0.07, RE - 0.10, WS - WT), (RM, YD, RE - 0.14, YD + 0.07)):
                m.oquad([P(r0, y1, ang), P(r1, y1, ang), P(r1, y0, ang), P(r0, y0, ang)], sign * T(ang), "cab")
    # footrests between the pedestals
    for k in range(3):
        lo = A0 + POS_SPAN * k + (dth if k == 0 else dth / 2) + 0.01
        hi = A0 + POS_SPAN * (k + 1) - (dth if k == 2 else dth / 2) - 0.01
        fr = [(RM + 0.14, YD + 0.05), (RM + 0.40, YD + 0.13), (RM + 0.395, YD + 0.147), (RM + 0.135, YD + 0.067), (RM + 0.14, YD + 0.05)]
        sweep(m, fr, lo, hi, [dict(mat="kick"), dict(mat="kick"), dict(mat="pad"), dict(mat="kick")], 12)
        for ang in (lo, hi):
            bar(m, P(RM, YD + 0.07, ang), P(RM + 0.27, YD + 0.07, ang), 0.02, 0.025, Y, "kick")
    # end panels: light laminate with a black edge band, a little proud of the whole section
    outline = [(RB - 0.022, YD), (RE + 0.045, YD), (RE + 0.045, WS + 0.006), (RB + 0.345, WS + 0.006), (RB + 0.345, WS + 0.08),
               (RB + 0.185, WS + 0.24), (RB - 0.022, WS + 0.24)]
    parts = [[(RB - 0.022, YD), (RE + 0.045, YD), (RE + 0.045, WS + 0.006), (RB - 0.022, WS + 0.006)],
             [(RB - 0.022, WS + 0.006), (RB + 0.345, WS + 0.006), (RB + 0.345, WS + 0.08), (RB - 0.022, WS + 0.08)],
             [(RB - 0.022, WS + 0.08), (RB + 0.345, WS + 0.08), (RB + 0.185, WS + 0.24), (RB - 0.022, WS + 0.24)]]
    th = 0.035
    for a, sign in ((A0, -1), (A1, 1)):
        off = T(a) * th * sign
        for part in parts:
            # split the big lower panel so the corner shading has points to work with
            pts = [P(r, y, a) + off for (r, y) in part]
            for i in range(4):
                for j in range(2):
                    def lerp(u, v):
                        top = pts[0] + (pts[1] - pts[0]) * u
                        bot = pts[3] + (pts[2] - pts[3]) * u
                        return top + (bot - top) * v
                    q = [lerp(i / 4, j / 2), lerp((i + 1) / 4, j / 2), lerp((i + 1) / 4, (j + 1) / 2), lerp(i / 4, (j + 1) / 2)]
                    m.oquad(q, T(a) * sign, "endpanel")
        for i in range(len(outline)):
            (r0, y0), (r1, y1) = outline[i], outline[(i + 1) % len(outline)]
            Ls = math.hypot(r1 - r0, y1 - y0)
            nr, ny = (y1 - y0) / Ls, -(r1 - r0) / Ls
            m.oquad([P(r0, y0, a), P(r1, y1, a), P(r1, y1, a) + off, P(r0, y0, a) + off], nr * O(a) + ny * Y, "edge")


def monitor(m, c, right, up, page, arm_from):
    """A 27-inch monitor (thin bezel, a chin) on an articulated arm from the riser's rail; its glass is a live face."""
    w, h = 0.597, 0.336
    r, u = unit(right), unit(up)
    n = np.cross(r, u)
    c = np.asarray(c, float)
    W_, Ht, Hb = w / 2 + 0.008, h / 2 + 0.008, h / 2 + 0.018
    hc = c + u * (Ht - Hb) / 2
    m.obox(hc - n * 0.009, r, u, W_, (Ht + Hb) / 2, 0.009, "mon", faces="nweud")
    glass = [c - r * w / 2 + u * h / 2, c + r * w / 2 + u * h / 2, c + r * w / 2 - u * h / 2, c - r * w / 2 - u * h / 2]
    m.quad(glass, "screen", lit=True)
    m.live.append((MONITOR, PAGES.index(page), glass))
    for (x0, y0, x1, y1) in ((-W_, Ht, W_, h / 2), (-W_, -h / 2, W_, -Hb), (-W_, h / 2, -w / 2, -h / 2), (w / 2, h / 2, W_, -h / 2)):
        m.quad([c + r * x0 + u * y0, c + r * x1 + u * y0, c + r * x1 + u * y1, c + r * x0 + u * y1], "mon")
    back = c - n * 0.018 - u * 0.01
    rbox(m, back - n * 0.014, r, u, 0.21, 0.13, 0.016, "mon_back", 0.012)
    head = back - n * 0.04
    m.obox(head, r, u, 0.04, 0.04, 0.012, "arm")
    base = np.asarray(arm_from, float)
    m.obox(base + Y * 0.03, r, Y, 0.03, 0.035, 0.03, "arm")
    post_top = base + Y * 0.07
    m.cylinder(base + Y * 0.06, post_top + Y * 0.02, 0.016, "arm", n=10)
    elbow = (post_top + head) / 2 + Y * 0.025
    m.cylinder(elbow - Y * 0.02, elbow + Y * 0.02, 0.02, "arm", n=10)
    bar(m, post_top + Y * 0.01, elbow, 0.03, 0.022, Y, "arm")
    bar(m, elbow, head - n * 0.012, 0.03, 0.022, Y, "arm")
    for dx in (-0.03, 0.03):
        c0 = back - n * 0.02 - u * 0.09 + r * dx
        g = base - Y * 0.024 + (c0 - base) * np.array([1, 0, 1]) * 0.25 + r * dx * 0.5
        mid_ = (c0 + g) / 2 - Y * 0.05
        m.cylinder(c0, mid_, 0.0045, "kick", n=6)
        m.cylinder(mid_, g, 0.0045, "kick", n=6)


def screens(m, k):
    a = mid(k)
    o, t = O(a), T(a)
    R = -t
    tilt = math.radians(9)
    cen = P(RB + 0.44, WS + 0.32, a)
    gap, toe = 0.012, math.radians(24)
    Wm = 0.597 / 2 + 0.008
    R2 = R * math.cos(toe) + o * math.sin(toe)
    R1 = R * math.cos(toe) - o * math.sin(toe)
    sets = [(cen - R * (Wm + gap / 2) - R1 * (Wm + gap / 2), R1), (cen, R), (cen + R * (Wm + gap / 2) + R2 * (Wm + gap / 2), R2)]
    for i, (c, r) in enumerate(sets):
        nn = np.cross(r, Y)
        u = Y * math.cos(tilt) - nn * math.sin(tilt)
        ang = math.atan2(c[2] - AZ, c[0] - AX)
        monitor(m, c, r, u, SCREEN_PAGES[k][i], P(RB + 0.08, WS + 0.245, ang))


def riser_modules(m, k):
    a = mid(k)
    rs, ys = RB + 0.255, WS + 0.145
    for s, kind in ((-1.22, CLOCK), (1.22, ANNUNCIATOR)):
        ang = a - s / rs
        right = -T(ang)
        up = unit(-O(ang) * 0.15 + Y * 0.15)
        n = np.cross(right, up)
        plate(m, P(rs, ys, ang) + n * 0.008, right, up, 0.36, 0.13, 0.016, "clock" if kind == CLOCK else "ann", "module", lit=True,
              live=(kind, k + 1))


def topface(m, c, ax, ay, hx, hy, hz, mat, body_mat):
    c = np.asarray(c, float)
    ax, ay = unit(ax), unit(ay)
    az = np.cross(ax, ay)
    m.obox(c, ax, ay, hx, hy, hz, body_mat, faces="nsewd")
    t = c + ay * hy
    m.quad([t - ax * hx - az * hz, t + ax * hx - az * hz, t + ax * hx + az * hz, t - ax * hx + az * hz], mat)


def keyboard(m, k):
    c, fin, right, up = L_(0.27, -0.02, 0.0, k)
    tl = math.radians(4)
    topface(m, c + Y * 0.013, right, up * math.cos(tl) - fin * math.sin(tl), 0.22, 0.011, 0.073, "kb_top", "kb_body")
    p, fin2, r2, _ = L_(0.26, 0.40, 0.0, k)
    m.obox(p + Y * 0.0015, r2, Y, 0.125, 0.0015, 0.105, "pad", faces="nsewu")
    rbox(m, p + Y * 0.019 + fin2 * 0.01 + r2 * 0.01, r2 * math.cos(0.1) + fin2 * math.sin(0.1), Y, 0.031, 0.017, 0.056, "mouse", 0.016)


def wedge(m, k, f, s, w, d, hb, hf, top, body_mat):
    c, fin, right, up = L_(f, s, 0.0, k)
    near, far = c - fin * d / 2, c + fin * d / 2
    pts = {}
    for sx in (-1, 1):
        for sz, base_pt, hh in ((-1, near, hf), (1, far, hb)):
            pts[(sx, sz, 0)] = base_pt + right * sx * w / 2 + Y * 0.002
            pts[(sx, sz, 1)] = base_pt + right * sx * w / 2 + Y * hh

    def q(*ks):
        return [pts[x] for x in ks]
    m.oquad(q((-1, -1, 1), (1, -1, 1), (1, -1, 0), (-1, -1, 0)), -fin, body_mat)
    m.oquad(q((1, 1, 1), (-1, 1, 1), (-1, 1, 0), (1, 1, 0)), fin, body_mat)
    m.oquad(q((-1, 1, 1), (-1, -1, 1), (-1, -1, 0), (-1, 1, 0)), -right, body_mat)
    m.oquad(q((1, -1, 1), (1, 1, 1), (1, 1, 0), (1, -1, 0)), right, body_mat)
    tl, tr, br, bl = pts[(-1, 1, 1)], pts[(1, 1, 1)], pts[(1, -1, 1)], pts[(-1, -1, 1)]
    nrm = unit(np.cross(bl - tl, tr - tl))
    m.quad([tl, tr, br, bl], top)
    return (tl + tr + br + bl) / 4, right, unit(tl - bl), nrm


def phone(m, k, s):
    c, right, ups, n = wedge(m, k, 0.31, s, 0.20, 0.21, 0.075, 0.032, "phone_top", "phone")
    plate(m, c + ups * 0.045 + right * 0.025 + n * 0.003, right, ups, 0.11, 0.05, 0.006, "lcd", "phone", lit=True, live=(LCD, k + 1))
    rbox(m, c - right * 0.068 + n * 0.022, ups, n, 0.10, 0.018, 0.026, "phone", 0.014)


def keyset(m, k, s, tex):
    c, fin, right, up = L_(0.30, s, 0.0, k)
    m.obox(c + Y * 0.006, right, Y, 0.12, 0.006, 0.09, "phone", faces="nsewu")
    lean = math.radians(38)
    u = Y * math.cos(lean) + fin * math.sin(lean)
    centre = c + Y * 0.115 + fin * 0.035
    plate(m, centre, right, u, 0.27, 0.18, 0.02, tex, "phone", lit=True, live=(KEYSET, k + 1))
    bar(m, c + Y * 0.01 + fin * 0.05, centre - np.cross(right, u) * 0.012, 0.06, 0.02, fin, "arm")


def switch_box(m, k, s):
    c, right, ups, n = wedge(m, k, 0.31, s, 0.48, 0.27, 0.12, 0.045, "switch_top", "phone")
    L_up = math.hypot(0.27, 0.075)

    def at(x, v):
        return c + right * ((x / 480 - 0.5) * 0.48) + ups * ((0.5 - v / 300) * L_up)
    for i, name in enumerate(LAMPS):
        x = 60 + (i % 4) * 120
        v = 70 + (i // 4) * 50
        plate(m, at(x, v) + n * 0.005, right, ups, 0.085, 0.042, 0.01, None, "edge", live=(LAMP, i))
    for i in range(2):
        base = at(85 + i * 155, 210)
        m.obox(base + n * 0.003, right, ups, 0.03, 0.03, 0.003, "alu_dark", faces="nsewu")
        m.cylinder(base + n * 0.004, base + n * 0.03 + ups * 0.01, 0.005, "toggle", n=8)
        rbox(m, base + n * 0.024, right, ups, 0.03, 0.036, 0.021, "guard", 0.006)
    kb = at(395, 210)
    m.cylinder(kb + n * 0.002, kb + n * 0.03, 0.026, "knob", n=16)
    m.obox(kb + n * 0.031 + ups * 0.013, right, ups, 0.003, 0.013, 0.001, "toggle", faces="u")


def mug(m, k, f, s):
    c, fin, right, up = L_(f, s, 0.0, k)
    x, z = c[0], c[2]
    m.drum(x, z, 0.042, WS + 0.001, WS + 0.10, "mug", n=14)
    m.drum(x, z, 0.036, WS + 0.02, WS + 0.10, "mug", n=14, outward=False)
    m.ring(x, z, 0.036, 0.042, WS + 0.10, "mug", n=14)
    m.disc(x, z, 0.036, WS + 0.075, "coffee", n=14)
    hnd = c + right * 0.05
    bar(m, hnd + Y * 0.08 - right * 0.008, hnd + Y * 0.08 + right * 0.02, 0.012, 0.01, Y, "mug")
    bar(m, hnd + Y * 0.025 - right * 0.008, hnd + Y * 0.025 + right * 0.02, 0.012, 0.01, Y, "mug")
    bar(m, hnd + Y * 0.022 + right * 0.02, hnd + Y * 0.083 + right * 0.02, 0.012, 0.01, right, "mug")


def notepad(m, k, f, s, rot):
    c, fin, right, up = L_(f, s, 0.0, k)
    r = right * math.cos(rot) + fin * math.sin(rot)
    m.obox(c + Y * 0.004, r, Y, 0.075, 0.004, 0.105, "mug", faces="nsew")
    t = c + Y * 0.0081
    zz = np.cross(r, Y)
    m.quad([t - r * 0.075 - zz * 0.105, t + r * 0.075 - zz * 0.105, t + r * 0.075 + zz * 0.105, t - r * 0.075 + zz * 0.105], "paper")
    p = t + Y * 0.005 + r * 0.11
    m.cylinder(p - zz * 0.07 + r * 0.01, p + zz * 0.07 - r * 0.01, 0.005, "pen", n=6)


def headset(m, k, s):
    a = mid(k) - s / RE
    o, t = O(a), T(a)
    hook0, hook1 = P(RE - 0.06, WS - 0.075, a), P(RE + 0.05, WS - 0.075, a)
    bar(m, hook0, hook1, 0.03, 0.012, Y, "alu")
    bar(m, hook1 - Y * 0.006, hook1 + Y * 0.02, 0.03, 0.01, o, "alu")
    centre = P(RE + 0.035, WS - 0.075 - 0.082, a)
    R = 0.082
    prev = None
    for i in range(11):
        th = math.radians(-25 + i * (230 / 10))
        p = centre + t * R * math.cos(th) + Y * R * math.sin(th)
        if prev is not None:
            bar(m, prev, p, 0.022, 0.008, p - centre, "hs")
        prev = p
    for th in (-25, 205):
        th = math.radians(th)
        p = centre + t * (R + 0.004) * math.cos(th) + Y * (R + 0.004) * math.sin(th) - Y * 0.035
        side = 1 if math.cos(th) > 0 else -1
        m.cylinder(p - t * side * 0.012, p + t * side * 0.016, 0.036, "hs", n=12)
        if side < 0:
            m.cylinder(p - Y * 0.01 + o * 0.01, p - Y * 0.045 + o * 0.11, 0.004, "hs", n=6)


def console():
    m = Mesh()
    cells = footprint()
    floor_band(m, cells)
    body(m)
    for k, name, sign in POSITIONS:
        a = mid(k)
        screens(m, k)
        riser_modules(m, k)
        keyboard(m, k)
        plate(m, P(RB, WS + 0.11, a) - O(a) * 0.006, T(a), Y, 1.10, 0.10, 0.012, sign, "edge", lit=True)
        if k == 0:
            keyset(m, k, -1.0, "keyset")
            switch_box(m, k, 1.0)
            mug(m, k, 0.40, -1.36)
            notepad(m, k, 0.30, -0.42, 0.18)
            headset(m, k, -1.30)
        else:
            keyset(m, k, -1.0 if k == 1 else 1.0, "keyset_b")
            phone(m, k, 0.97 if k == 1 else -0.97)
            mug(m, k, 0.42, -1.38 if k == 1 else 1.38)
            notepad(m, k, 0.30, -0.44 if k == 1 else 0.44, -0.15 if k == 1 else 0.2)
            headset(m, k, 1.22 if k == 1 else -1.22)
    return m


# ------------------------------------------------------------------ the chair (its own frame: axis at the origin, facing +z)

CHAIR_SEAT = 0.497          # the cushion's top over the chair's floor
CHAIR_PUSH = 0.40           # drawn this far forward of its cell's centre (pulled up to the desk)


def chair():
    m = Mesh()
    f = np.array([0, 0, 1.0])
    lat = np.cross(Y, f)
    base = np.zeros(3)
    for i in range(5):
        ph = 2 * math.pi * i / 5 + 0.31
        d = f * math.cos(ph) + lat * math.sin(ph)
        hub, tip = base + d * 0.04 + Y * 0.105, base + d * 0.31 + Y * 0.072
        bar(m, hub, tip, 0.042, 0.03, Y, "nylon")
        side = np.cross(Y, d)
        rbox(m, tip + d * 0.022 - Y * 0.026, d, Y, 0.032, 0.02, 0.03, "nylon", 0.01)
        for sgn in (-1, 1):
            wc = tip + d * 0.022 - Y * 0.044 + side * sgn * 0.0165
            m.cylinder(wc - side * 0.0075, wc + side * 0.0075, 0.028, "pu", n=10)
    m.cylinder(base + Y * 0.085, base + Y * 0.13, 0.05, "nylon", n=12)
    m.cylinder(base + Y * 0.13, base + Y * 0.30, 0.031, "pu", n=12)
    m.cylinder(base + Y * 0.30, base + Y * 0.385, 0.017, "chrome", n=10)
    m.obox(base + Y * 0.40, lat, Y, 0.12, 0.02, 0.14, "nylon")
    rbox(m, base + Y * 0.455 + f * 0.02, lat, Y, 0.255, 0.042, 0.245, "fabric", 0.035)
    rec = math.radians(9)
    bup = Y * math.cos(rec) - f * math.sin(rec)
    bc = base - f * 0.27 + Y * 0.86
    bar(m, base + Y * 0.40 - f * 0.12, base + Y * 0.42 - f * 0.29, 0.06, 0.03, Y, "nylon")
    bar(m, base + Y * 0.42 - f * 0.29, bc - bup * 0.28 - f * 0.03, 0.06, 0.03, f, "nylon")
    rbox(m, bc, lat, bup, 0.16, 0.31, 0.035, "mesh", 0.02)
    wing = math.radians(20)
    for sgn in (-1, 1):
        wx = lat * sgn * math.cos(wing) + f * math.sin(wing)
        wc = bc + lat * sgn * (0.16 + 0.055 * math.cos(wing)) + f * (0.055 * math.sin(wing))
        rbox(m, wc, wx * sgn, bup, 0.06, 0.29, 0.03, "mesh", 0.018)
    hc = bc + bup * 0.43 + f * 0.01
    for sgn in (-1, 1):
        m.cylinder(bc + bup * 0.28 + lat * sgn * 0.07 - f * 0.01, hc - bup * 0.04 + lat * sgn * 0.07 - f * 0.01, 0.008, "chrome", n=8)
    rbox(m, hc, lat, bup, 0.14, 0.075, 0.04, "fabric", 0.03)
    for sgn in (-1, 1):
        sd = lat * sgn
        bar(m, base + Y * 0.41 + sd * 0.10, base + Y * 0.41 + sd * 0.275, 0.05, 0.02, Y, "nylon")
        m.obox(base + Y * 0.53 + sd * 0.275 - f * 0.01, sd, Y, 0.016, 0.12, 0.03, "nylon")
        rbox(m, base + Y * 0.665 + sd * 0.275 + f * 0.03, f, Y, 0.13, 0.02, 0.042, "pu", 0.016)
    return m


# ------------------------------------------------------------------ corner shading (ambient occlusion)

def bake(m, floor_y=0.0, res=1 / 32, reach=0.7, rays=20, strength=0.85, light=None):
    """Each corner of every shaded quad: how open the space over it is (rays through a voxel grid of the mesh and the
    floor), times `light` (task lights). Kept as quad['shade'] (four values, 0..1)."""
    pts = np.concatenate([q["P"] for q in m.quads])
    lo = pts.min(0) - 1.0
    hi = pts.max(0) + 1.0
    dims = np.ceil((hi - lo) / res).astype(int) + 1
    occ = np.zeros(dims, bool)
    for q in m.quads:
        P_ = q["P"]
        e1, e2 = P_[1] - P_[0], P_[3] - P_[0]
        nu = max(2, int(np.linalg.norm(e1) / (res * 0.5)) + 2)
        nv = max(2, int(np.linalg.norm(e2) / (res * 0.5)) + 2)
        S_, T_ = np.meshgrid(np.linspace(0, 1, nu), np.linspace(0, 1, nv), indexing="ij")
        A = ((1 - S_) * (1 - T_))[..., None] * P_[0] + (S_ * (1 - T_))[..., None] * P_[1] + (S_ * T_)[..., None] * P_[2] \
            + ((1 - S_) * T_)[..., None] * P_[3]
        idx = np.clip(np.floor((A.reshape(-1, 3) - lo) / res).astype(int), 0, dims - 1)
        occ[idx[:, 0], idx[:, 1], idx[:, 2]] = True
    fy = int(np.floor((floor_y - lo[1]) / res))
    if fy > 0:
        occ[:, :min(fy, dims[1]), :] = True
    k = np.arange(rays) + 0.5
    rr = np.sqrt(k / rays)
    th = k * math.pi * (3 - math.sqrt(5))
    local = np.stack([rr * np.cos(th), rr * np.sin(th), np.sqrt(1 - rr * rr)], 1)
    steps = np.arange(res * 1.2, reach, res * 0.9)
    todo = [i for i, q in enumerate(m.quads) if q["ao"]]
    for q in m.quads:
        q["shade"] = [1.0] * 4
    if not todo:
        return
    corners = np.concatenate([m.quads[i]["P"] for i in todo])
    normals = np.concatenate([np.repeat(m.quads[i]["n"][None], 4, 0) for i in todo])
    key = np.concatenate([np.round(corners / 1e-3), np.round(normals * 50)], 1).astype(np.int64)
    uk, inv = np.unique(key, axis=0, return_inverse=True)
    inv = inv.reshape(-1)
    up_p = np.zeros((len(uk), 3))
    up_n = np.zeros((len(uk), 3))
    up_p[inv] = corners
    up_n[inv] = normals
    ao = np.ones(len(uk))
    B = 4000
    for b0 in range(0, len(uk), B):
        p, n = up_p[b0:b0 + B], up_n[b0:b0 + B]
        a = np.where(np.abs(n[:, 1:2]) < 0.9, np.array([[0, 1.0, 0]]), np.array([[1.0, 0, 0]]))
        u = np.cross(n, a)
        u /= np.linalg.norm(u, axis=1, keepdims=True)
        v = np.cross(n, u)
        dirs = local[None, :, 0:1] * u[:, None] + local[None, :, 1:2] * v[:, None] + local[None, :, 2:3] * n[:, None]
        start = p + n * res * 1.3
        hit = np.zeros((len(p), rays), bool)
        dist = np.full((len(p), rays), reach)
        for s in steps:
            idx = np.floor((start[:, None] + dirs * s - lo) / res).astype(int)
            inb = np.all((idx >= 0) & (idx < dims), axis=2)
            idx = np.clip(idx, 0, dims - 1)
            h = occ[idx[..., 0], idx[..., 1], idx[..., 2]] & inb & ~hit
            dist[h] = s
            hit |= h
        w = np.where(hit, 1 - 0.55 * (dist / reach), 0.0)
        ao[b0:b0 + B] = 1 - strength * w.mean(1)
    ao = np.clip(ao, 0.25, 1.0)
    per = ao[inv].reshape(-1, 4)
    for j, i in enumerate(todo):
        q = m.quads[i]
        s = per[j] * 0.88
        if light is not None:
            s = s * np.array([light(q["P"][c], q["n"]) for c in range(4)])
        q["shade"] = [float(min(1.0, v)) for v in s]


def task_light(p, n):
    """The riser's task light on the desk in front of it."""
    if n[1] < 0.9 or abs(p[1] - WS) > 0.004:
        return 1.0
    r = math.hypot(p[0] - AX, p[2] - AZ)
    if r < RB + 0.29 or r > RE + 0.1:
        return 1.0
    return 1.0 + 0.30 * math.exp(-(r - (RB + 0.29)) / 0.28)


# ------------------------------------------------------------------ writing

MAGIC = 0x4A54434D  # "JTCM"
_seed = [0]


def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE and q["uvf"] is None:
        P_ = q["P"]
        x, y, w, h = atlas.box[mat]
        span_w = np.linalg.norm(P_[1] - P_[0]) * 2.0
        span_h = np.linalg.norm(P_[3] - P_[0]) * 2.0
        sw, sh = min(span_w, 0.9), min(span_h, 0.9)
        _seed[0] = (_seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        ou = (_seed[0] % 997) / 997 * (1 - sw)
        ov = ((_seed[0] // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = q["uvf"] or [(0, 0), (1, 0), (1, 1), (0, 1)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path, edges=()):
    """Quads (kind, then four vertices counter-clockwise as Minecraft draws front faces: x y z, normal as bytes, u v,
    shade 0-255), the live faces (kind, id, corners tl tr br bl) and the band's step edges (cell, side, t0, t1)."""
    path.parent.mkdir(parents=True, exist_ok=True)
    buf = bytearray(struct.pack(">iii", MAGIC, 1, len(m.quads)))
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
    for kind, ident, corners in m.live:
        buf += struct.pack(">bb", kind, ident)
        for p in corners:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">i", len(edges))
    for dx, dz, side, t0, t1 in edges:
        buf += struct.pack(">bbbff", dx, dz, side, t0, t1)
    # the tiles the renderer draws the step edges from: nosing, step light, fascia (u0 v0 u1 v1)
    for mat in ("alu", "amber", "fascia"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def _json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def _loot(id_, extra=None):
    """A block loot table in 26.x's form (one "condition" a pool)."""
    entries = [{"type": "minecraft:item", "name": f"jugcraft:{id_}"}]
    pools = [{"rolls": 1, "entries": entries, "condition": {"type": "minecraft:survives_explosion"}}]
    if extra:
        pools.append(extra)
    return {"type": "minecraft:block", "pools": pools, "random_sequence": f"jugcraft:blocks/{id_}"}


def _recipe(pattern, key, result, count=1):
    """A tower piece's recipe: its 3 x 3 made on the Advanced Workbench (tools/advanced_crafting.py widen: the same
    ingredients and cost a block, bigger than 3 x 3); the advanced kit's own 6 x 6s are written over it by apply()."""
    import advanced_crafting
    recipe = {"fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                         {"condition": "jugcraft:feature_enabled", "feature": "drones"}],
              "type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key,
              "result": {"id": f"jugcraft:{result}", "count": count}}
    if result in advanced_crafting.RECIPES or result in advanced_crafting.CONVERT_EXCLUDE:
        return recipe
    return advanced_crafting.widen(recipe)


def floor_textures():
    """The Raised Floor's block textures: the carpet (four half-block tiles), its side (fascia), the nosing and the
    step light."""
    tex = ASSETS / "textures/block"
    t = _carpet(32, 11)
    img = Image.new("RGBA", (64, 64))
    for i in range(2):
        for j in range(2):
            img.paste(t, (i * 32, j * 32))
    img.save(tex / f"{FLOOR_ID}.png")
    _noise((46, 48, 52), 2, 37, size=16).save(tex / f"{FLOOR_ID}_side.png")
    _noise((176, 181, 188), 9, 4, "h", size=16).save(tex / f"{FLOOR_ID}_nosing.png")
    _solid((255, 168, 58)).save(tex / f"{FLOOR_ID}_light.png")


def write_data(cells):
    parts = len(cells)
    # the console: every part an empty model; the renderer draws it
    _json(ASSETS / "blockstates" / f"{ID}.json", {"variants": {f"facing={f},part={p}": {"model": f"jugcraft:block/{ID}"}
                                                                for f in ("north", "east", "south", "west") for p in range(parts)}})
    _json(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/steel_armor_plate"}, "elements": []})
    for i in (ID, CHAIR_ID):
        _json(ASSETS / "models/item" / f"{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{i}"}})
        _json(ASSETS / "items" / f"{i}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{i}"}})
    # the chair: the floor tile under it when it stands on a raised floor; the renderer draws the chair
    _json(ASSETS / "models/block" / f"{CHAIR_ID}.json", {"textures": {"particle": f"jugcraft:block/{FLOOR_ID}"}, "elements": []})
    _json(ASSETS / "blockstates" / f"{CHAIR_ID}.json", {"variants": {
        f"platform={p},rotation={r}": {"model": f"jugcraft:block/{FLOOR_ID}" if p == "true" else f"jugcraft:block/{CHAIR_ID}"}
        for p in ("false", "true") for r in range(16)}})
    # the raised floor: the carpet slab, and the nosing and step light on each side that steps down
    floor_textures()
    _json(ASSETS / "models/block" / f"{FLOOR_ID}.json", {
        "textures": {"particle": f"jugcraft:block/{FLOOR_ID}", "top": f"jugcraft:block/{FLOOR_ID}", "side": f"jugcraft:block/{FLOOR_ID}_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"}}}]})
    _json(ASSETS / "models/block" / f"{FLOOR_ID}_edge.json", {
        "textures": {"particle": f"jugcraft:block/{FLOOR_ID}", "side": f"jugcraft:block/{FLOOR_ID}_side",
                     "nosing": f"jugcraft:block/{FLOOR_ID}_nosing", "light": f"jugcraft:block/{FLOOR_ID}_light"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 1.6, 0], "faces": {"north": {"uv": [0, 13, 16, 14.6], "texture": "#side"}}},
            {"from": [0, 1.6, 0.2], "to": [16, 2.45, 0.2], "light_emission": 15,
             "faces": {"north": {"uv": [0, 6, 16, 7], "texture": "#light"}}},
            {"from": [0, 1.6, 0], "to": [16, 1.6, 0.2], "faces": {"up": {"uv": [0, 0, 16, 0.2], "texture": "#side"}}},
            {"from": [0, 2.45, 0], "to": [16, 3.1, 0.56], "faces": {
                "north": {"uv": [0, 4, 16, 4.65], "texture": "#nosing"}, "south": {"uv": [0, 4, 16, 4.65], "texture": "#nosing"},
                "up": {"uv": [0, 0, 16, 0.56], "texture": "#nosing"}, "down": {"uv": [0, 0, 16, 0.56], "texture": "#nosing"},
                "east": {"uv": [0, 4, 0.56, 4.65], "texture": "#nosing"}, "west": {"uv": [0, 4, 0.56, 4.65], "texture": "#nosing"}}}]})
    multipart = [{"apply": {"model": f"jugcraft:block/{FLOOR_ID}"}}]
    for side, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        part = {"model": f"jugcraft:block/{FLOOR_ID}_edge"}
        if rot:
            part["y"] = rot
        multipart.append({"when": {f"edge_{side}": "true"}, "apply": part})
    _json(ASSETS / "blockstates" / f"{FLOOR_ID}.json", {"multipart": multipart})
    _json(ASSETS / "models/item" / f"{FLOOR_ID}.json", {"parent": f"jugcraft:block/{FLOOR_ID}"})
    _json(ASSETS / "items" / f"{FLOOR_ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{FLOOR_ID}"}})
    # loot: the console and the floor drop themselves; the chair also gives back the floor tile it took
    _json(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", _loot(ID))
    _json(DATA / "jugcraft/loot_table/blocks" / f"{FLOOR_ID}.json", _loot(FLOOR_ID))
    _json(DATA / "jugcraft/loot_table/blocks" / f"{CHAIR_ID}.json", _loot(CHAIR_ID, {
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{FLOOR_ID}"}],
        "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:survives_explosion"},
            {"type": "minecraft:match_block", "blocks": f"jugcraft:{CHAIR_ID}", "state": {"platform": "true"}}]}}))
    _json(DATA / "jugcraft/recipe" / f"{ID}.json", _recipe(["GGG", "CMC", "SPS"], {
        "G": "jugcraft:blast_glass", "C": "jugcraft:advanced_circuit", "M": "jugcraft:tower_master_console",
        "S": "#c:plates/steel", "P": "jugcraft:processor"}, ID))
    import advanced_crafting            # made on the Advanced Crafting Table: its 6 x 6 recipe over the 3 x 3
    advanced_crafting.apply(DATA / "jugcraft/recipe", [ID])
    _json(DATA / "jugcraft/recipe" / f"{CHAIR_ID}.json", _recipe([" W ", "WWW", "SIS"], {
        "W": "minecraft:black_wool", "S": "#c:ingots/steel", "I": "#c:ingots/iron"}, CHAIR_ID))
    _json(DATA / "jugcraft/recipe" / f"{FLOOR_ID}.json", _recipe(["CCC", "SSS"], {
        "C": "minecraft:gray_carpet", "S": "#c:plates/steel"}, FLOOR_ID, 6))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{CHAIR_ID}"] = CHAIR_NAME
    lang[f"block.jugcraft.{FLOOR_ID}"] = FLOOR_NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Tower Control Console needs a clear, level band 11 blocks wide, with headroom over the desk"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, CHAIR_ID, FLOOR_ID):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write_all():
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    cells = footprint()
    m = console()
    bake(m, light=task_light)
    edges = band_edges(cells)
    write_mesh(m, atlas, MESH, edges)
    c = chair()
    bake(c)
    write_mesh(c, atlas, CHAIR_MESH)
    write_data(cells)
    import item_icons
    for i in (ID, CHAIR_ID):
        item_icons.draw(i).save(ASSETS / f"textures/item/{i}.png")
    return m, c, cells, edges


if __name__ == "__main__":
    m, c, cells, edges = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(f"console: {len(m.quads)} quads ({kinds.count(SOLID)} shaded, {kinds.count(LIT)} lit), {len(m.live)} live faces, "
          f"{len(cells)} parts, {len(edges)} step edges; mesh {MESH.stat().st_size} bytes")
    print(f"chair: {len(c.quads)} quads; mesh {CHAIR_MESH.stat().st_size} bytes")
