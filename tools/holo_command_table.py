"""The Holo Command Table: the Tri-Pillar's giant round hologram table (work list item 5, approved by Justin on
22 October 2026 from the mockup sheet claude/jugcraft-tripillar-holo-table.md).

Eleven blocks across at standing height, in the middle of the hologram rotunda: a graphite top on a recessed drum with
a lit toe kick, a brushed aluminium edge with an amber LED line and a chamfer, a smoked-glass projection field the
size of the tower hologram's footprint (8.8 across; the control building is 8.7 at 1:12) under it, 36 projector lenses
round the field, and five touch stations set into the rim on the south half (the master console keeps the north side).

One item places the whole table (tower/HoloCommandTableBlock.java): its parts are the cells the round top stands on,
part 0 the middle cell (the Tower Core's cell + 1 in the tower), the only one with a block entity. Nothing is drawn by
the block models: client/HoloCommandTableRenderer draws this mesh (assets/jugcraft/tower_mesh/holo_command_table.bin.gz,
textured from textures/entity/holo_command_table.png) and the live parts over it: the site plan in the field, the
radar sweep, the drones, the five screens and their lamps, and the tower hologram (client/HoloMapRenderer).

Run it to write the mesh, the texture, the item icon and the block's data files (blockstate, models, item, loot table,
recipe, lang, tags). Real-world basis: a CIC plotting table / command-and-control "situation table" (a backlit glass
plot surface in a standing-height console with operator positions round its rim)."""
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
MESH = ASSETS / "tower_mesh/holo_command_table.bin.gz"
TEXTURE = ASSETS / "textures/entity/holo_command_table.png"
ICON = ASSETS / "textures/item/holo_command_table.png"
ID = "holo_command_table"
NAME = "Holo Command Table"

# Part-local frame: the middle cell's corner at the origin, the floor at y 0, the table's axis at (0.5, 0.5).
CX, CZ = 0.5, 0.5
TABLE_R = 5.5               # 11 across
TOP = 1.0                   # standing height
SLAB = TOP - 0.20           # the top's underside
BASE_R = 4.7                # the drum
KICK_R = 4.55               # the toe kick
FIELD_R = 4.4               # the projection field
BEZEL_R = 4.56
FIELD_Y = TOP - 0.03
EDGE_CHAMFER = 0.06
STATIONS = [30, 60, 90, 120, 150]     # degrees from east towards south (screens: POWER, SUPPLY, FLEET, JOBS, TOWER)
STATION_R = 5.02
TILT = math.radians(14)
SEGMENTS = 160

# kinds of quad in the mesh
SOLID, LIT, GLOW = 0, 1, 2

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def footprint():
    """The cells the table stands on, as (dx, dz) from the middle cell: the middle first, then row by row from the
    north-west (tower/HoloCommandTableBlock.FOOTPRINT builds the same list)."""
    out = [(0, 0)]
    for dz in range(-6, 7):
        for dx in range(-6, 7):
            if (dx, dz) == (0, 0):
                continue
            nx = min(max(CX, dx), dx + 1)
            nz = min(max(CZ, dz), dz + 1)
            if math.hypot(nx - CX, nz - CZ) < TABLE_R - 1e-6:
                out.append((dx, dz))
    return out


# ------------------------------------------------------------------ the texture: one sheet of tiles

ATLAS_W, ATLAS_H = 1024, 512
FIELD_TILE = (0, 0, 512, 512)                 # the field's glass (the live plan is drawn over it)
TILE = 64
TILES = ["graphite_top", "graphite", "drum", "alu", "alu_dark", "black", "lens", "amber", "wash", "white", "screen", "rib"]


def tile_box(name):
    i = TILES.index(name)
    return (512 + (i % 8) * TILE, (i // 8) * TILE, 512 + (i % 8) * TILE + TILE, (i // 8) * TILE + TILE)


def _noise(base, amount, seed, streak=None, seams=None):
    rnd = np.random.default_rng(seed)
    n = rnd.uniform(-1, 1, (TILE, TILE)) * amount
    if streak == "h":
        n = n * 0.35 + rnd.uniform(-1, 1, (TILE, 1)) * amount * 0.9
    a = np.zeros((TILE, TILE, 4), np.uint8)
    for i in range(3):
        a[..., i] = np.clip(base[i] + n, 0, 255)
    a[..., 3] = 255
    if seams:
        for s in seams:
            a[:, s, :3] = (a[:, s, :3] * 0.55).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def field_glass(size=512):
    """Smoked glass, a little lighter in the middle, with the plot grid: rings every half block, a ray every 15 degrees."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for r in range(size // 2 + 20, 0, -2):
        t = min(1.0, r / (size / 2))
        d.ellipse((size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r),
                  fill=(int(8 + 12 * (1 - t)), int(12 + 14 * (1 - t)), int(17 + 16 * (1 - t)), 255))
    grid = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g = ImageDraw.Draw(grid)
    k = size / (2 * FIELD_R)
    for ring in range(1, 9):
        r = ring * 0.5 * k
        g.ellipse((size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r), outline=(255, 170, 70, 46), width=1)
    for deg in range(0, 360, 15):
        a = math.radians(deg)
        g.line((size / 2, size / 2, size / 2 + math.cos(a) * size / 2, size / 2 + math.sin(a) * size / 2),
               fill=(255, 170, 70, 30), width=1)
    return Image.alpha_composite(img, grid)


def screen_tile():
    """A touch screen's glass: near black with a faint blue cast (the renderer writes on it)."""
    a = Image.new("RGBA", (TILE, TILE), (8, 12, 16, 255))
    d = ImageDraw.Draw(a)
    d.rectangle((0, 0, TILE, 6), fill=(22, 28, 34, 255))
    return a


def wash_tile():
    """The toe kick's light on the floor: amber fading outward (v runs out from the drum)."""
    a = np.zeros((TILE, TILE, 4), np.uint8)
    for v in range(TILE):
        k = (1 - v / (TILE - 1)) ** 1.8
        a[v, :, 0], a[v, :, 1], a[v, :, 2] = 255, 158, 56
        a[v, :, 3] = int(220 * k)
    return Image.fromarray(a, "RGBA")


def atlas():
    img = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    img.paste(field_glass(), FIELD_TILE[:2])
    tiles = {
        "graphite_top": _noise((34, 37, 41), 2.5, 2),
        "graphite": _noise((40, 43, 47), 3, 1),
        "drum": _noise((47, 50, 55), 3, 3, seams=[0, 1]),
        "alu": _noise((176, 181, 188), 9, 4, "h"),
        "alu_dark": _noise((98, 103, 110), 6, 5, "h"),
        "black": _noise((18, 19, 21), 1.5, 6),
        "lens": Image.new("RGBA", (TILE, TILE), (255, 196, 104, 255)),
        "amber": Image.new("RGBA", (TILE, TILE), (255, 168, 58, 255)),
        "wash": wash_tile(),
        "white": Image.new("RGBA", (TILE, TILE), (255, 255, 255, 255)),
        "screen": screen_tile(),
        "rib": _noise((70, 74, 80), 4, 7),
    }
    for name, t in tiles.items():
        img.paste(t, tile_box(name)[:2])
    return img


# ------------------------------------------------------------------ the mesh

class Mesh:
    """Quads as (kind, [4 corners], [4 uvs in 0..1 of the atlas], normal). Corners are given as seen from the front:
    top-left, top-right, bottom-right, bottom-left; they are written counter-clockwise (Minecraft's front faces)."""

    def __init__(self):
        self.quads = []
        self._seed = 0

    def _uv(self, mat, pts, scale):
        """Texture coordinates for a material: the field laid flat over its tile; any other tile sampled over a patch
        the quad's size (in blocks, times scale) at a varying offset, so no face stretches its tile or wraps it."""
        P = np.asarray(pts, float)
        if mat == "field":
            x0, z0, x1, z1 = FIELD_TILE
            return [((x0 + (p[0] - (CX - FIELD_R)) / (2 * FIELD_R) * (x1 - x0)) / ATLAS_W,
                     (z0 + (p[2] - (CZ - FIELD_R)) / (2 * FIELD_R) * (z1 - z0)) / ATLAS_H) for p in P]
        bx0, by0, bx1, by1 = tile_box(mat)
        w = np.linalg.norm(P[1] - P[0]) * scale
        h = np.linalg.norm(P[3] - P[0]) * scale
        w, h = min(w, 0.9), min(h, 0.9)
        self._seed = (self._seed * 1103515245 + 12345) & 0x7FFFFFFF
        ou = (self._seed % 997) / 997 * (1 - w)
        ov = ((self._seed // 997) % 991) / 991 * (1 - h)
        pad = 1.5 / TILE
        span = 1 - 2 * pad

        def U(u, v):
            return ((bx0 + (pad + (ou + u) * span) * TILE) / ATLAS_W, (by0 + (pad + (ov + v) * span) * TILE) / ATLAS_H)
        return [U(0, 0), U(w, 0), U(w, h), U(0, h)]

    def quad(self, pts, mat, kind=SOLID, scale=1.0, uvs=None):
        P = np.asarray(pts, float)
        n = np.zeros(3)
        for i in range(4):
            a, b = P[i], P[(i + 1) % 4]
            n += np.array([(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])])
        n = -n
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            return
        n /= ln
        if uvs is None:
            uvs = self._uv(mat, P, scale)
        self.quads.append((kind, P, uvs, n, mat))

    def box(self, lo, hi, mat, kind=SOLID, faces="nsewud"):
        x0, y0, z0 = lo
        x1, y1, z1 = hi
        F = {"u": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
             "d": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
             "n": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
             "s": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
             "e": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
             "w": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]}
        for f in faces:
            self.quad(F[f], mat, kind)

    def obox(self, c, ax, ay, hx, hy, hz, mat, kind=SOLID, faces="nsewud"):
        c = np.asarray(c, float)
        ax = np.asarray(ax, float) / np.linalg.norm(ax)
        ay = np.asarray(ay, float) / np.linalg.norm(ay)
        az = np.cross(ax, ay)
        X, Y, Z = ax * hx, ay * hy, az * hz

        def P(sx, sy, sz):
            return tuple(c + X * sx + Y * sy + Z * sz)
        F = {"u": [P(-1, 1, -1), P(1, 1, -1), P(1, 1, 1), P(-1, 1, 1)],
             "d": [P(-1, -1, 1), P(1, -1, 1), P(1, -1, -1), P(-1, -1, -1)],
             "n": [P(1, 1, -1), P(-1, 1, -1), P(-1, -1, -1), P(1, -1, -1)],
             "s": [P(-1, 1, 1), P(1, 1, 1), P(1, -1, 1), P(-1, -1, 1)],
             "e": [P(1, 1, 1), P(1, 1, -1), P(1, -1, -1), P(1, -1, 1)],
             "w": [P(-1, 1, -1), P(-1, 1, 1), P(-1, -1, 1), P(-1, -1, -1)]}
        for f in faces:
            self.quad(F[f], mat, kind)

    def drum(self, r, y0, y1, mat, kind=SOLID, outward=True, n=SEGMENTS, scale=1.0):
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            p0 = (CX + r * math.cos(t0), CZ + r * math.sin(t0))
            p1 = (CX + r * math.cos(t1), CZ + r * math.sin(t1))
            if outward:
                pts = [(p1[0], y1, p1[1]), (p0[0], y1, p0[1]), (p0[0], y0, p0[1]), (p1[0], y0, p1[1])]
            else:
                pts = [(p0[0], y1, p0[1]), (p1[0], y1, p1[1]), (p1[0], y0, p1[1]), (p0[0], y0, p0[1])]
            self.quad(pts, mat, kind, scale)

    def annulus(self, r0, r1, y, mat, kind=SOLID, up=True, n=SEGMENTS, scale=1.0, ring_uv=False):
        r0 = max(r0, 0.03)
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            c0, s0, c1, s1 = math.cos(t0), math.sin(t0), math.cos(t1), math.sin(t1)
            pi0, pi1 = (CX + r0 * c0, y, CZ + r0 * s0), (CX + r0 * c1, y, CZ + r0 * s1)
            po0, po1 = (CX + r1 * c0, y, CZ + r1 * s0), (CX + r1 * c1, y, CZ + r1 * s1)
            pts = [po0, po1, pi1, pi0] if up else [pi0, pi1, po1, po0]
            uvs = None
            if ring_uv:
                # v runs from the inner edge (0) out (1): the wash fades outward
                bx0, by0, bx1, by1 = tile_box(mat)
                pad = 1.5 / TILE

                def U(u, v):
                    return ((bx0 + (pad + u * (1 - 2 * pad)) * TILE) / ATLAS_W, (by0 + (pad + v * (1 - 2 * pad)) * TILE) / ATLAS_H)
                uvs = [U(0.2, 1), U(0.8, 1), U(0.8, 0), U(0.2, 0)] if up else [U(0.2, 0), U(0.8, 0), U(0.8, 1), U(0.2, 1)]
            self.quad(pts, mat, kind, scale, uvs)

    def cone(self, r0, y0, r1, y1, mat, kind=SOLID, n=SEGMENTS):
        """A conical strip from (r0, y0) to (r1, y1), facing up and out."""
        for i in range(n):
            t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n

            def p(r, y, t):
                return (CX + r * math.cos(t), y, CZ + r * math.sin(t))
            self.quad([p(r1, y1, t0), p(r1, y1, t1), p(r0, y0, t1), p(r0, y0, t0)], mat, kind)


def station_frame(deg):
    """A touch station's frame: its rim point, outward and along-the-rim directions, the screen's normal and the
    direction down its slope (towards the user)."""
    a = math.radians(deg)
    o = np.array([math.cos(a), 0, math.sin(a)])
    t = np.array([-math.sin(a), 0, math.cos(a)])
    up = np.array([0, 1.0, 0])
    nrm = up * math.cos(TILT) + o * math.sin(TILT)
    along = np.cross(nrm, t)
    along /= np.linalg.norm(along)
    if along @ o < 0:
        along = -along
    return o, t, up, nrm, along


SW, SD, SH = 0.98, 0.60, 0.05      # a station's housing: across, down the slope, half its thickness


def station_screen(deg):
    """The screen's corners (top-left, top-right, bottom-right, bottom-left as the user standing outside sees it) and
    its status lamp's centre."""
    o, t, up, nrm, along = station_frame(deg)
    c_rim = np.array([CX, TOP, CZ]) + o * STATION_R
    centre = c_rim + nrm * (SH + 0.02) + up * (SD / 2 * math.sin(TILT))
    sc = centre + nrm * (SH + 0.002)
    sw, sd = SW - 0.10, SD - 0.12
    tl = sc - along * (sd / 2) + t * (sw / 2)
    tr = sc - along * (sd / 2) - t * (sw / 2)
    br = sc + along * (sd / 2) - t * (sw / 2)
    bl = sc + along * (sd / 2) + t * (sw / 2)
    lamp = centre + nrm * (SH + 0.003) - along * (SD / 2 - 0.03) + t * (SW / 2 - 0.05)
    return [tl, tr, br, bl], lamp, centre


def station(m, deg):
    o, t, up, nrm, along = station_frame(deg)
    corners, lamp, centre = station_screen(deg)
    m.obox(centre, t, nrm, SW / 2, SH, SD / 2, "alu_dark")
    inner_top = centre - along * (SD / 2) - nrm * SH
    h_back = inner_top[1] - TOP
    if h_back > 0.01:
        bc = np.array([inner_top[0], TOP + h_back / 2, inner_top[2]])
        m.obox(bc, t, up, SW / 2, h_back / 2, 0.02, "alu_dark")
        for s in (-1, 1):
            sc = centre + t * s * (SW / 2 - 0.01) - nrm * SH - up * (h_back / 2) * 0.5
            m.obox(sc, t, up, 0.012, h_back / 2 * 0.5 + 0.01, SD / 2 * 0.95, "alu_dark")
    c_rim = np.array([CX, TOP, CZ]) + o * STATION_R
    pr = c_rim + o * (SD / 2 + 0.05) + up * 0.025
    m.obox(pr, t, up, SW / 2, 0.025, 0.04, "alu")


def table():
    m = Mesh()
    # the top: the rim, the chamfer, the edge band with its LED line, the inlay line, the underside
    m.annulus(BEZEL_R, TABLE_R - EDGE_CHAMFER, TOP, "graphite_top")
    m.cone(TABLE_R - EDGE_CHAMFER, TOP, TABLE_R, TOP - EDGE_CHAMFER, "alu")
    m.drum(TABLE_R, SLAB, TOP - EDGE_CHAMFER, "alu", scale=2.0)
    m.drum(TABLE_R + 0.003, TOP - 0.115, TOP - 0.095, "amber", LIT)
    m.annulus(TABLE_R - 0.16, TABLE_R - 0.145, TOP + 0.002, "amber", LIT)
    m.annulus(BASE_R, TABLE_R, SLAB, "graphite", up=False)
    # the drum and its toe kick, and the light the kick washes the floor with
    m.drum(BASE_R, 0.08, SLAB, "drum", scale=0.8)
    m.drum(KICK_R, 0.0, 0.08, "black")
    m.annulus(KICK_R, BASE_R, 0.08, "black", up=False)
    m.annulus(KICK_R, KICK_R + 0.5, 0.004, "wash", GLOW, ring_uv=True)
    # the projector bezel, a little proud of the top, and the glass field
    m.annulus(FIELD_R, BEZEL_R, TOP + 0.03, "alu")
    m.drum(BEZEL_R, TOP, TOP + 0.03, "alu")
    m.drum(FIELD_R, FIELD_Y, TOP + 0.03, "alu_dark", outward=False)
    m.annulus(0.0, FIELD_R, FIELD_Y, "field", LIT, n=96)
    # 36 projector lenses round the field
    for i in range(36):
        a = math.radians(i * 10 + 5)
        r = (FIELD_R + BEZEL_R) / 2
        x, z = CX + r * math.cos(a), CZ + r * math.sin(a)
        m.box((x - 0.035, TOP + 0.031, z - 0.035), (x + 0.035, TOP + 0.036, z + 0.035), "lens", LIT, faces="u")
    for deg in STATIONS:
        station(m, deg)
    return m


# ------------------------------------------------------------------ writing

MAGIC = 0x4A484354  # "JHCT"


def write_mesh(m, path=MESH):
    """quads (kind, then four vertices counter-clockwise: x y z, normal as bytes, u v), then the five screens (corners
    tl, tr, br, bl and the lamp) and the field's radius and height."""
    path.parent.mkdir(parents=True, exist_ok=True)
    buf = bytearray()
    buf += struct.pack(">ii", MAGIC, 1)
    buf += struct.pack(">i", len(m.quads))
    for kind, P, uvs, n, mat in m.quads:
        buf += struct.pack(">b", kind)
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in n]
        for k in (0, 3, 2, 1):       # tl, bl, br, tr: counter-clockwise seen from the front
            buf += struct.pack(">fffbbbff", P[k][0], P[k][1], P[k][2], nb[0], nb[1], nb[2], uvs[k][0], uvs[k][1])
    buf += struct.pack(">i", len(STATIONS))
    for deg in STATIONS:
        corners, lamp, _ = station_screen(deg)
        for p in corners + [lamp]:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">fff", FIELD_R, FIELD_Y, TOP)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def write_data():
    """The block's blockstate (every part shows an empty model: the renderer draws the table), its particle model, the
    item (a flat icon), loot table, recipe, lang and tags."""
    parts = len(footprint())
    (ASSETS / "blockstates").mkdir(parents=True, exist_ok=True)
    (ASSETS / "blockstates" / f"{ID}.json").write_text(json.dumps(
        {"variants": {f"part={p}": {"model": f"jugcraft:block/{ID}"} for p in range(parts)}}, indent=2) + "\n")
    (ASSETS / "models/block" / f"{ID}.json").write_text(json.dumps(
        {"textures": {"particle": "jugcraft:block/steel_armor_plate"}, "elements": []}, indent=2) + "\n")
    (ASSETS / "models/item" / f"{ID}.json").write_text(json.dumps(
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}}, indent=2) + "\n")
    (ASSETS / "items" / f"{ID}.json").write_text(json.dumps(
        {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}}, indent=2) + "\n")
    loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{ID}"}],
                                                  "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"jugcraft:blocks/{ID}"}
    (DATA / "jugcraft/loot_table/blocks" / f"{ID}.json").write_text(json.dumps(loot, indent=2) + "\n")
    # No recipe: a hologram, the Drone Tower's own (tower/Holograms, Justin 28 October 2026); apply() removes an old one.
    import advanced_crafting
    advanced_crafting.apply(DATA / "jugcraft/recipe", [ID])
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang["message.jugcraft.holo_command_table.blocked"] = "The Holo Command Table needs a clear, level space 11 blocks across"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    for tag in ("mineable/pickaxe",):
        p = DATA / "minecraft/tags/block" / f"{tag}.json"
        t = json.loads(p.read_text())
        if f"jugcraft:{ID}" not in t["values"]:
            t["values"].append(f"jugcraft:{ID}")
        p.write_text(json.dumps(t, indent=2) + "\n")


def write_icon(atlas_img):
    """The item's icon: the table seen from above at three quarters, drawn from the mesh."""
    m = table()
    size = 64
    S = 4 * size
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    yaw, pitch = math.radians(30), math.radians(38)

    def proj(p):
        x, y, z = p[0] - CX, p[1] - 0.6, p[2] - CZ
        x, z = x * math.cos(yaw) - z * math.sin(yaw), x * math.sin(yaw) + z * math.cos(yaw)
        y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
        return (S / 2 + x * S / 12.4, S / 2 - y * S / 12.4 + S * 0.08), z
    A = np.asarray(atlas_img.convert("RGBA"))
    faces = []
    for kind, P, uvs, n, mat in m.quads:
        pts = [proj(p) for p in P]
        depth = sum(z for _, z in pts) / 4
        u = sum(uv[0] for uv in uvs) / 4 * ATLAS_W
        v = sum(uv[1] for uv in uvs) / 4 * ATLAS_H
        c = A[min(ATLAS_H - 1, int(v)), min(ATLAS_W - 1, int(u))]
        shade = 1.0 if kind else 0.55 + 0.45 * max(0.0, n[1]) + 0.15 * abs(n[0])
        col = tuple(int(min(255, c[i] * shade)) for i in range(3)) + (255 if kind != GLOW else 150,)
        faces.append((depth, [p for p, _ in pts], col))
    faces.sort(key=lambda f: f[0])
    for _, pts, col in faces:
        d.polygon(pts, fill=col)
    # the live plan's hexagon and pillars glowing in the field, as the table shows them
    plan = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pd = ImageDraw.Draw(plan)
    hr = 52 / 12
    hexp = [proj((CX + hr * math.cos(math.radians(60 * k)), FIELD_Y, CZ + hr * math.sin(math.radians(60 * k))))[0] for k in range(6)]
    pd.line(hexp + [hexp[0]], fill=(255, 186, 96, 255), width=6)
    for cx, cz in ((0, 17), (15, -8), (-15, -8)):
        pts = [proj((CX + (cx + sx * 9.5) / 12, FIELD_Y, CZ + (cz + sz * 9.5) / 12))[0] for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
        pd.polygon(pts, fill=(255, 170, 70, 150))
    glow = plan.filter(ImageFilter.GaussianBlur(5))
    img = Image.alpha_composite(img, glow)
    img = Image.alpha_composite(img, plan)
    img = img.resize((size, size), Image.LANCZOS)
    ICON.parent.mkdir(parents=True, exist_ok=True)
    img.save(ICON)


def write_all():
    img = atlas()
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEXTURE, optimize=True)
    m = table()
    write_mesh(m)
    write_data()
    write_icon(img)
    return m


if __name__ == "__main__":
    m = write_all()
    kinds = [q[0] for q in m.quads]
    print(f"{len(m.quads)} quads ({kinds.count(SOLID)} solid, {kinds.count(LIT)} lit, {kinds.count(GLOW)} glow); "
          f"{len(footprint())} parts; mesh {MESH.stat().st_size} bytes")
