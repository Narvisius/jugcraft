"""The Curved Video Wall: the Tri-Pillar's video walls on the rotunda's cut pillar faces (work list item 7, approved by
Justin on 22 October 2026 from the mockup sheet; see claude/jugcraft-tripillar-video-walls.md).

A fine-pitch LED video wall built concave round an axis 14.62 blocks in front of it: 30 cabinet columns by 22 rows of
0.6 x 0.3375 cabinets (17.9 blocks along the curve, 7.4 high), no bezels, in a black powder-coated surround: a plinth of
vented controller doors (a green status LED on each) over a kick recess, bottom and top trims each with an amber LED
line, a bulkhead up to the top (9 blocks), end posts with an amber line closing back towards a wall behind it.

One item places it whole (tower/CurvedVideoWallBlock.java). It turns to any of 84 aims: each aim is a whole-block
vector from the master cell (the bottom of the middle column) to the axis cell, between 14.5 and 15.4 long (`aims`), so
in the tower the three walls stand exactly round the rotunda's axis (the core cell). Its other cells are Curved Video
Wall Parts (no item), each knowing its offset from the master (`footprint`); a part collides with the wall's body in its
cell on an eighth-of-a-block grid (`mask`). Nothing is drawn by block models: client/CurvedVideoWallRenderer draws this
mesh (round the axis, turned to the aim) and the board on its 30 facets (live faces), read from the depot it is pinned
to: OPERATIONS, FLEET BOARD or LOGISTICS (the master's `board`; using the wall with an empty hand turns to the next).

Frame of the mesh: the axis at the origin, the floor (the master cell's bottom) at y 0, the wall centred due south of
the axis (+z, 90 degrees; angles are measured from east towards south, as in the tower's tools).
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
ID, NAME = "curved_video_wall", "Curved Video Wall"
PART_ID = "curved_video_wall_part"
MESH = ASSETS / "tower_mesh/curved_video_wall.bin.gz"
META = ASSETS / "tower_mesh/curved_video_wall.json"
TEXTURE = ASSETS / "textures/entity/curved_video_wall.png"
TOWER_TEXTURE = ASSETS / "textures/entity/curved_video_wall_tower.png"
TIER_PICTURE = ASSETS / "textures/gui/tower_tripillar/tier_9.png"
BOARDS = ["operations", "fleet", "logistics"]

# ------------------------------------------------------------------ dimensions (blocks)

R_S = 14.62                 # the LED surface
R_F = 14.56                 # the surround's front
R_B = 14.95                 # the surround's back
R_POST = 15.6               # the end posts reach back to here (to the concrete, in the tower)
HALF = math.radians(35)
MID = math.radians(90)
A0, A1 = MID - HALF, MID + HALF
COLS, ROWS = 30, 22
CAB_H = 0.3375
Y_KICK = 0.06
Y_PLINTH = 0.9
Y0 = 1.0                    # the display's bottom
Y1 = Y0 + ROWS * CAB_H      # 8.425
Y_TRIM = Y1 + 0.125
TOP = 9.0
POST_W = 0.12
HEIGHT = 9                  # cells high
CANVAS = (2400, 1000)       # the board's pixels (client/CurvedVideoWallRenderer)
SPAN = 9                    # parts lie within this many cells of the master, east-west and north-south

FACET = 6                   # the live kind (client/ConsoleMesh)
SOLID, LIT = TCC.SOLID, TCC.LIT
Y = np.array([0, 1.0, 0])


def P(r, y, a):
    return np.array([r * math.cos(a), y, r * math.sin(a)])


def O(a):
    return np.array([math.cos(a), 0, math.sin(a)])


def T(a):
    return np.array([-math.sin(a), 0, math.cos(a)])


def col_angles(i):
    return A0 + (A1 - A0) * i / COLS, A0 + (A1 - A0) * (i + 1) / COLS


# ------------------------------------------------------------------ aims, footprint, collision

def aims():
    """Every aim, in order: (vx, vz) from the master cell to the axis cell, 14.5 to 15.4 long, by the angle of the
    vector (tower/CurvedVideoWallBlock.AIMS builds the same list)."""
    v = [(x, z) for x in range(-16, 17) for z in range(-16, 17) if 211 <= x * x + z * z <= 237]
    v.sort(key=lambda p: math.atan2(p[1], p[0]) % (2 * math.pi))
    return v


def aim_of(vx, vz):
    return aims().index((vx, vz))


def in_body(x, z, psi):
    """Whether (x, z) (from the axis) is in the wall's body: between its front and back, across its span and posts."""
    r = math.hypot(x, z)
    if r < R_F - 0.04 or r > R_B:
        return False
    d = math.atan2(z, x) - psi
    d = (d + math.pi) % (2 * math.pi) - math.pi
    return abs(d) <= HALF + POST_W / r


def mask(vx, vz, dx, dz):
    """The body in the master's cell (dx, dz) as 64 bits: bit i + 8 j for the eighth (i east, j south)."""
    psi = math.atan2(-vz, -vx)
    cx, cz = dx - vx, dz - vz
    m = 0
    for j in range(8):
        for i in range(8):
            if in_body(cx + (i + 0.5) / 8 - 0.5, cz + (j + 0.5) / 8 - 0.5, psi):
                m |= 1 << (i + 8 * j)
    return m


def footprint(vx, vz):
    """The wall's cells for the aim (vx, vz), as (dx, dy, dz) from the master: the master first, then layer by layer,
    row by row (north to south, west to east). Every cell the body reaches into, all nine high."""
    cells = [(dx, dz) for dz in range(-SPAN, SPAN + 1) for dx in range(-SPAN, SPAN + 1) if mask(vx, vz, dx, dz)]
    assert (0, 0) in cells
    out = [(0, 0, 0)]
    for dy in range(HEIGHT):
        for (dx, dz) in cells:
            if (dx, dy, dz) != (0, 0, 0):
                out.append((dx, dy, dz))
    return out


# ------------------------------------------------------------------ textures

ATLAS = (256, 256)


def _cabinet():
    """One LED cabinet, dark: a fine pixel pitch, the module joints faintly, the cabinet's seam round it."""
    w, h = 96, 54
    a = np.zeros((h, w, 4), np.uint8)
    rnd = np.random.default_rng(71)
    base = np.array([9, 10, 12], float)
    n = rnd.uniform(-1.2, 1.2, (h, w))
    for c in range(3):
        a[..., c] = np.clip(base[c] + n, 0, 255)
    a[..., 3] = 255
    a[::2, ::2, :3] = np.clip(a[::2, ::2, :3].astype(int) + 4, 0, 255)      # the LED pitch, as a faint grain
    for x in (24, 48, 72):
        a[:, x, :3] = (14, 15, 18)
    a[27, :, :3] = (14, 15, 18)
    a[0, :, :3] = a[-1, :, :3] = (4, 4, 5)
    a[:, 0, :3] = a[:, -1, :3] = (4, 4, 5)
    return Image.fromarray(a, "RGBA")


def _vent():
    """A controller door in the plinth: perforated, a pull at the bottom; its status LED is a lit quad of its own."""
    img = Image.new("RGBA", (96, 128), (26, 27, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 95, 127), outline=(13, 13, 15, 255))
    d.rectangle((1, 1, 94, 126), outline=(34, 35, 39, 255))
    for y in range(18, 104, 5):
        for x in range(10 + (y // 5 % 2) * 2, 86, 5):
            d.rectangle((x, y, x + 1, y + 1), fill=(9, 9, 10, 255))
    d.rectangle((38, 112, 58, 115), fill=(48, 50, 55, 255))
    return img


def tiles():
    return {
        "cab": _cabinet(),
        "vent": _vent(),
        "frame": TCC._noise((22, 23, 25), 2, 61, size=64),
        "kick": TCC._noise((12, 12, 13), 1, 62, size=32),
        "bulk": TCC._noise((26, 27, 30), 2, 63, size=64),
        "back": TCC._noise((31, 32, 35), 3, 64, size=64),
        "amber": TCC._solid((255, 176, 74)),
        "led": TCC._solid((90, 230, 120)),
    }


NOISE = {"frame", "kick", "bulk", "back"}


class Atlas:
    """The wall's tiles on one small sheet (as tower_control_console.Atlas, sized to fit)."""

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

def band(m, r, ya, yb, mat, lit=False, n=COLS, a0=A0, a1=A1, outward=False, per_col=False):
    """A strip of the surround at radius r from ya to yb, facing the axis (or away from it), in n steps."""
    for i in range(n):
        t0, t1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        pts = [P(r, yb, t0), P(r, yb, t1), P(r, ya, t1), P(r, ya, t0)]
        if outward:
            pts = [pts[1], pts[0], pts[3], pts[2]]
        uvf = [(0, 0), (1, 0), (1, 1), (0, 1)] if per_col else None
        m.quad(pts, mat, uvf, lit=lit)


def ledge(m, y, r0, r1, up, mat, n=COLS, a0=A0, a1=A1):
    """A horizontal strip between radii r0 and r1 at height y, facing up or down."""
    for i in range(n):
        t0, t1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        m.oquad([P(r0, y, t0), P(r0, y, t1), P(r1, y, t1), P(r1, y, t0)], Y if up else -Y, mat)


def wall():
    m = TCC.Mesh()
    # the LED face: a quad a cabinet, a facet (live face) a column, concave towards the axis
    for i in range(COLS):
        t0, t1 = col_angles(i)
        m.live.append((FACET, i, [P(R_S, Y1, t0), P(R_S, Y1, t1), P(R_S, Y0, t1), P(R_S, Y0, t0)]))
        for j in range(ROWS):
            ya, yb = Y0 + j * CAB_H, Y0 + (j + 1) * CAB_H
            m.quad([P(R_S, yb, t0), P(R_S, yb, t1), P(R_S, ya, t1), P(R_S, ya, t0)], "cab", [(0, 0), (1, 0), (1, 1), (0, 1)], ao=False)
    e0, e1 = A0 - 0.004, A1 + 0.004
    # the plinth: a controller door under each column, over the kick recess
    band(m, R_F + 0.06, 0.0, Y_KICK, "kick", a0=e0, a1=e1)
    ledge(m, Y_KICK, R_F, R_F + 0.06, False, "kick", a0=e0, a1=e1)
    band(m, R_F, Y_KICK, Y_PLINTH, "vent", per_col=True)
    for i in range(COLS):
        t0, t1 = col_angles(i)
        a = t0 + (t1 - t0) * 0.1
        da = 0.035 / R_F
        y = Y_PLINTH - 0.12
        r = R_F - 0.004
        m.quad([P(r, y + 0.035, a), P(r, y + 0.035, a + da), P(r, y, a + da), P(r, y, a)], "led", lit=True)
    # the bottom trim with its amber line, the lip under the display
    band(m, R_F, Y_PLINTH, Y_PLINTH + 0.03, "frame", a0=e0, a1=e1)
    band(m, R_F, Y_PLINTH + 0.03, Y_PLINTH + 0.05, "amber", lit=True, a0=e0, a1=e1)
    band(m, R_F, Y_PLINTH + 0.05, Y0, "frame", a0=e0, a1=e1)
    ledge(m, Y0, R_F, R_S, True, "frame")
    # the top trim with its amber line, the lip over the display, the bulkhead to the top
    ledge(m, Y1, R_F, R_S, False, "frame")
    band(m, R_F, Y1, Y1 + 0.04, "frame", a0=e0, a1=e1)
    band(m, R_F, Y1 + 0.04, Y1 + 0.06, "amber", lit=True, a0=e0, a1=e1)
    band(m, R_F, Y1 + 0.06, Y_TRIM, "frame", a0=e0, a1=e1)
    band(m, R_F, Y_TRIM, TOP, "bulk", a0=e0, a1=e1)
    # its top and its back (seen when it stands free)
    ledge(m, TOP, R_F, R_B, True, "frame", a0=e0, a1=e1)
    band(m, R_B, 0.0, TOP, "back", a0=e0, a1=e1, outward=True)
    # the end posts, standing a little proud, an amber line down each face
    for a, sgn in ((A0, -1), (A1, 1)):
        t = T(a)
        ra, rb = R_F - 0.04, R_POST
        c = P((ra + rb) / 2, TOP / 2, a) + t * sgn * POST_W / 2
        m.obox(c, t, Y, POST_W / 2, TOP / 2, (rb - ra) / 2, "frame", faces="nsewu")
        c1 = P(ra - 0.004, (Y0 + Y1) / 2, a) + t * sgn * POST_W / 2
        h = (Y1 - Y0) / 2
        m.quad([c1 + t * 0.012 + Y * h, c1 - t * 0.012 + Y * h, c1 - t * 0.012 - Y * h, c1 + t * 0.012 - Y * h], "amber", lit=True, ao=False)
    return m


def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE and q["uvf"] is None:
        P_ = q["P"]
        span_w = np.linalg.norm(P_[1] - P_[0]) * 2.0
        span_h = np.linalg.norm(P_[3] - P_[0]) * 2.0
        sw, sh = min(span_w, 0.9), min(span_h, 0.9)
        TCC._seed[0] = (TCC._seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        s = TCC._seed[0]
        ou = (s % 997) / 997 * (1 - sw)
        ov = ((s // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = q["uvf"] or [(0, 0), (1, 0), (1, 1), (0, 1)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path):
    """The console's mesh format ("JTCM" 1, client/ConsoleMesh): quads, the 30 facets as live faces, no step edges."""
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
    for kind, ident, corners in m.live:
        buf += struct.pack(">bb", kind, ident)
        for p in corners:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">i", 0)
    for mat in ("frame", "amber", "frame"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


# ------------------------------------------------------------------ the OPERATIONS board's tower picture

def tower_picture():
    """The status screen's tier-9 picture as grey (the board tints it by tier: built, building, still to come), and the
    rows of the picture where each tier begins (TB), for client/CurvedVideoWallRenderer."""
    import tripillar_geometry as G
    src = Image.open(TIER_PICTURE).convert("RGBA")
    a = np.asarray(src).astype(np.float32)
    lum = (a[..., 0] * 0.3 + a[..., 1] * 0.5 + a[..., 2] * 0.2) / 255
    g = np.clip((0.35 + 0.65 * lum) * 255, 0, 255)
    out = np.zeros_like(a)
    out[..., 0] = out[..., 1] = out[..., 2] = g
    out[..., 3] = a[..., 3]
    TOWER_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(out.astype(np.uint8), "RGBA").save(TOWER_TEXTURE, optimize=True)
    top = G.TB[9] + 24
    blocks = 217                      # the picture's height in blocks of 2 px (TowerScreen.IMAGE_BLOCKS)
    rows = [round((blocks - tb * blocks / top) * 2, 2) for tb in G.TB] + [0.0]
    # the picture's drawn columns (where the tower is), so the board can sit it in its panel
    cols = np.where(a[..., 3].max(0) > 0)[0]
    return {"size": [src.width, src.height], "tier_rows": rows, "columns": [int(cols.min()), int(cols.max()) + 1]}


# ------------------------------------------------------------------ data files

def write_data():
    j = TCC._json
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["SSS", "SCS", "PTP"], {
        "S": "jugcraft:control_screen", "C": "jugcraft:advanced_circuit", "T": "jugcraft:processor", "P": "#c:plates/steel"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Curved Video Wall needs a clear curve 18 blocks along and 9 high, its axis 15 blocks in front"
    lang[f"message.jugcraft.{ID}.board"] = "Video wall board: %s"
    for b, name in zip(BOARDS, ("Operations", "Fleet Board", "Logistics")):
        lang[f"message.jugcraft.{ID}.board.{b}"] = name
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
    m = wall()
    TCC.bake(m, floor_y=0.0, res=1 / 16, reach=0.6, rays=16)
    write_mesh(m, atlas, MESH)
    meta = {"canvas": list(CANVAS), "tower": tower_picture()}
    TCC._json(META, meta)
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m, meta


if __name__ == "__main__":
    m, meta = write_all()
    print(len(m.quads), "quads,", len(m.live), "facets;", len(aims()), "aims;", meta["tower"])
