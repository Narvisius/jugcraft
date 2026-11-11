"""The Drone Display Case: the Tri-Pillar lobby's museum vitrines for a drone (work list item 1, approved by Justin on
22 October 2026 from the mockup sheet, without the placard; see claude/jugcraft-tripillar-display-case.md).

Built the way gallery showcases are: a graphite powder-coated plinth 0.62 high on a shadow-gap kick (an amber LED line
in it washing the floor), three access panels a side (climate vents at the sides, a key lock at the back), a brushed
stainless edge band and the glass standing in a gasketed channel round a charcoal fabric deck; a frameless hood of
low-iron glass 4.8 x 4.8 x 2.45 (green edges at its corners, fine black silicone joints); a light hood 0.30 high on top,
stainless bands round it, two linear LED diffusers and four spots under it, and between its bands the ticker rail: one
LED dot band (187 columns by 9 rows a side) running round all four sides, on which client/DroneDisplayCaseRenderer runs
the drone's name in four evenly spaced spots (client/TickerRail). Inside, the turntable with its light ring; the drone
(client/DroneModel) stands on it on its gear and turns once a minute. 5 x 5 cells, 3.37 high (four cells).

One item places it whole (tower/DroneDisplayCaseBlock.java): the master is the middle cell at the bottom, its other 99
cells are Drone Display Case Parts (no item) that know their offset from it. The master's `tier` is the drone shown
(using the case with an empty hand shows the next); its `facing` is the front (the back has the lock).

Frame of the mesh: the master cell's corner at the origin, the case's middle over (0.5, 0.5), the floor at y 0, the
front towards north (-z). Quad kinds: 0 shaded by the world, 1 full bright, 2 glass (translucent, shaded), 3 glow
(translucent, full bright: the floor wash). Live faces: the four ticker band faces (kind 7, ids 0-3 in the rail's
order, corners tl tr br bl as a reader in front of each sees them).
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

ID = "drone_display_case"
PART_ID = "drone_display_case_part"
NAME = "Drone Display Case"
MESH = ASSETS / "tower_mesh/drone_display_case.bin.gz"
TEXTURE = ASSETS / "textures/entity/drone_display_case.png"

CX = CZ = 0.5            # the case's middle (the master cell's middle)
HALF = 2.45              # the plinth's half width (5 cells less a reveal)
PY = 0.62                # the plinth's top
KICK = 0.09              # the shadow-gap kick's height
KI = 0.07                # and how far it is set in
GW = 2.40                # the glass's outer face (half width)
GT = 0.025               # glass thickness
GH = 2.45                # the glass's height
HOOD = 0.30              # the light hood's height
TT_R = 1.78              # turntable radius
TT_H = 0.09
HY0, HY1 = PY + GH, PY + GH + HOOD
RAIL_Y0, RAIL_Y1 = HY0 + 0.035, HY1 - 0.03     # the ticker band between the hood's bands
RAIL_HALF = HALF + 0.0015
RAIL_COLS, RAIL_ROWS = 187, 9                   # dot columns a side, rows (7 of the glyph, a dark row above and below)
CELLS = 5                # cells across
HEIGHT = 4               # cells high
SOLID, LIT, GLASS, GLOW = 0, 1, 2, 3
TICKER = 7               # the live kind (client/ConsoleMesh)
Y = np.array([0, 1.0, 0])
ATLAS = (1024, 512)


# ------------------------------------------------------------------ textures

def _panels(lock=False, vent=False):
    """A plinth side: graphite powder coat, the seams between its three access panels, a lock or a vent band."""
    W, H = 500, 64
    a = np.asarray(TCC._noise((38, 41, 45), 2.2, 11 + lock * 2 + vent, size=64).resize((W, H))).copy()
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    for i in (1, 2):
        x = int(W * i / 3)
        d.line((x, 0, x, H), fill=(14, 15, 17, 255), width=2)
        d.line((x + 2, 0, x + 2, H), fill=(58, 61, 66, 255), width=1)
    if vent:
        for i in range(3):
            x0 = int(W * (i + 0.5) / 3) - 40
            for k in range(14):
                d.rectangle((x0 + k * 6, 44, x0 + k * 6 + 2, 52), fill=(10, 10, 12, 255))
    if lock:
        x = W // 2
        d.ellipse((x - 5, 18, x + 5, 28), fill=(170, 172, 176, 255), outline=(90, 92, 96, 255))
        d.line((x, 20, x, 26), fill=(40, 40, 44, 255), width=2)
    return img


def _fabric():
    rnd = np.random.default_rng(31)
    a = np.zeros((64, 64, 4), np.uint8)
    n = rnd.uniform(-1, 1, (64, 64)) * 3
    weave = (np.add.outer(np.arange(64) % 2, np.arange(64) % 2) % 2) * 2
    for i, b in enumerate((44, 45, 47)):
        a[..., i] = np.clip(b + n + weave, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _wash():
    """The kick's light on the floor: amber, fading out from the plinth (v 0 at the kick)."""
    a = np.zeros((16, 16, 4), np.uint8)
    for v in range(16):
        k = (1 - v / 15) ** 2.0
        a[v, :, :3] = (255, 163, 66)
        a[v, :, 3] = int(round(255 * 0.75 * k))
    return Image.fromarray(a, "RGBA")


def _dots():
    """The ticker band: black, the unlit LEDs as faint dots, 4 pixels a dot, a whole side (187 x 9)."""
    a = np.zeros((RAIL_ROWS * 4, RAIL_COLS * 4, 4), np.uint8)
    a[..., :3] = 10
    a[..., 3] = 255
    for r in range(RAIL_ROWS):
        for c in range(RAIL_COLS):
            a[r * 4 + 1:r * 4 + 3, c * 4 + 1:c * 4 + 3, :3] = 30
    return Image.fromarray(a, "RGBA")


def _tint(rgb, alpha, size=16):
    return Image.new("RGBA", (size, size), tuple(rgb) + (int(round(alpha * 255)),))


def tiles():
    return {
        "side": _panels(vent=True), "side_back": _panels(lock=True), "side_plain": _panels(),
        "steel": TCC._noise((188, 192, 198), 9, 21, "h", size=64), "steel_dark": TCC._noise((120, 124, 130), 9, 22, "h", size=64),
        "kick": TCC._noise((12, 12, 14), 1, 23, size=32), "amber": TCC._solid((255, 172, 70)), "fabric": _fabric(),
        "gasket": TCC._solid((16, 16, 18)), "glass": _tint((214, 232, 232), 0.09), "glass_edge": _tint((96, 150, 138), 0.75),
        "silicone": TCC._solid((20, 22, 22)), "diffuser": TCC._solid((255, 246, 228)), "spot": TCC._solid((255, 240, 210)),
        "spot_ring": TCC._noise((90, 94, 100), 9, 23, "h", size=32), "tt_side": TCC._noise((70, 74, 80), 9, 24, "h", size=64),
        "tt_top": TCC._noise((32, 34, 37), 2, 42, size=64), "tt_ring": TCC._solid((255, 238, 206)), "wash": _wash(),
        "dots": _dots(),
    }


NOISE = {"steel", "steel_dark", "kick", "spot_ring", "tt_side", "tt_top"}


class Atlas:
    """The case's tiles on one sheet (as tower_control_console.Atlas)."""

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

def sides(half, y0, y1):
    """The four upright faces of a square round the middle, facing out; corners tl tr br bl as seen from outside."""
    h = half
    return {
        "n": [(CX + h, y1, CZ - h), (CX - h, y1, CZ - h), (CX - h, y0, CZ - h), (CX + h, y0, CZ - h)],
        "s": [(CX - h, y1, CZ + h), (CX + h, y1, CZ + h), (CX + h, y0, CZ + h), (CX - h, y0, CZ + h)],
        "e": [(CX + h, y1, CZ + h), (CX + h, y1, CZ - h), (CX + h, y0, CZ - h), (CX + h, y0, CZ + h)],
        "w": [(CX - h, y1, CZ - h), (CX - h, y1, CZ + h), (CX - h, y0, CZ + h), (CX - h, y0, CZ - h)],
    }


def box(m, lo, hi, mat, faces="nsewud", lit=False):
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    m.obox((lo + hi) / 2, (1, 0, 0), (0, 1, 0), *((hi - lo) / 2), mat, faces=faces, lit=lit)


def flat(m, x0, z0, x1, z1, y, mat, up=True, lit=False, uvf=None):
    """A level quad over x0..x1, z0..z1 at y, facing up or down."""
    m.oquad([(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1)], Y if up else -Y, mat, uvf, lit=lit)


def disc_down(m, cx, cz, r, y, mat, n=16, lit=False):
    c = np.array([cx, y, cz])
    for i in range(n):
        t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        p0 = c + r * np.array([math.cos(t0), 0, math.sin(t0)])
        p1 = c + r * np.array([math.cos(t1), 0, math.sin(t1)])
        m.oquad([c, p0, p1, p1], -Y, mat, lit=lit)


def kind(m, first, k):
    """Sets the kind of the quads added since `first` (glass, glow) and leaves them out of the baked shading."""
    for q in m.quads[first:]:
        q["kind"] = k
        if k != SOLID:
            q["ao"] = False


def case():
    m = TCC.Mesh()
    # the kick (the shadow gap), its amber line, the underside of the plinth's overhang
    k = HALF - KI
    for pts in sides(k, 0.0, KICK).values():
        m.quad(pts, "kick")
    for pts in sides(k + 0.002, KICK - 0.03, KICK - 0.012).values():
        m.quad(pts, "amber", lit=True, ao=False)
    for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        # a trapezoid a side, from the kick out to the plinth's edge
        tx, tz = -sz, sx
        a = [(CX + sx * k + tx * s * k, CZ + sz * k + tz * s * k) for s in (-1, 1)]
        b = [(CX + sx * HALF + tx * s * HALF, CZ + sz * HALF + tz * s * HALF) for s in (1, -1)]
        m.oquad([(a[0][0], KICK, a[0][1]), (a[1][0], KICK, a[1][1]), (b[0][0], KICK, b[0][1]), (b[1][0], KICK, b[1][1])], -Y, "kick")
    # the wash on the floor round it: a mitred strip a side, fading out from the kick
    first = len(m.quads)
    for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        tx, tz = -sz, sx
        c = np.array([CX + sx * k, 0.004, CZ + sz * k])
        t = np.array([tx, 0, tz])
        o = np.array([sx, 0, sz]) * 0.55
        pts = [c - t * k, c + t * k, c + t * (k + 0.55) + o, c - t * (k + 0.55) + o]
        m.oquad(pts, Y, "wash", [(0, 0), (1, 0), (1, 1), (0, 1)], lit=True, ao=False)
    kind(m, first, GLOW)
    # the plinth's sides (the front plain, vents at the sides, the lock at the back), its stainless band
    tex = {"n": "side_plain", "s": "side_back", "e": "side", "w": "side"}
    for s, pts in sides(HALF, KICK, PY - 0.035).items():
        m.quad(pts, tex[s], [(0, 0), (1, 0), (1, 1), (0, 1)])
    for pts in sides(HALF, PY - 0.035, PY).values():
        m.quad(pts, "steel")
    # its top: the stainless rim out to the edge, the gasket in the glass channel, the fabric deck inside
    for a0, a1, mat in ((GW + 0.004, HALF, "steel"), (GW - GT - 0.006, GW + 0.004, "gasket")):
        for sgn in (1, -1):
            flat(m, CX - a1, CZ + sgn * a0, CX + a1, CZ + sgn * a1, PY, mat)
            flat(m, CX + sgn * a0, CZ - a0, CX + sgn * a1, CZ + a0, PY, mat)
    d = GW - GT - 0.006
    flat(m, CX - d, CZ - d, CX + d, CZ + d, PY, "fabric", uvf=[(0, 0), (1, 0), (1, 1), (0, 1)])
    # the turntable and its light ring (round, so it can stand still while the drone turns on it)
    m.drum(CX, CZ, TT_R, PY, PY + TT_H, "tt_side", n=72)
    m.drum(CX, CZ, TT_R + 0.004, PY + 0.006, PY + 0.022, "tt_ring", n=72)
    for q in m.quads[-72:]:
        q["kind"], q["ao"] = LIT, False
    m.ring(CX, CZ, 0.0, TT_R, PY + TT_H, "tt_top", n=72)
    m.ring(CX, CZ, TT_R - 0.05, TT_R - 0.035, PY + TT_H + 0.002, "steel_dark", n=72)
    # the glass hood: four panes (outer and inner faces), their edges where they meet, the silicone in the joints
    first = len(m.quads)
    gy0, gy1 = PY, PY + GH
    for half, outward in ((GW, True), (GW - GT, False)):
        for pts in sides(half, gy0, gy1).values():
            m.quad(pts if outward else [pts[1], pts[0], pts[3], pts[2]], "glass")
    for sx in (1, -1):
        for sz in (1, -1):
            x0, z0 = CX + sx * GW, CZ + sz * GW
            m.oquad([(x0, gy1, z0), (x0, gy1, z0 - sz * GT), (x0, gy0, z0 - sz * GT), (x0, gy0, z0)], (sx, 0, 0), "glass_edge")
            m.oquad([(x0, gy1, z0), (x0 - sx * GT, gy1, z0), (x0 - sx * GT, gy0, z0), (x0, gy0, z0)], (0, 0, sz), "glass_edge")
    kind(m, first, GLASS)
    for sx in (1, -1):
        for sz in (1, -1):
            x = CX + sx * (GW - 0.003)
            z = CZ + sz * (GW - 0.003)
            box(m, (x - 0.0015, gy0, z - 0.0015), (x + 0.0015, gy1, z + 0.0015), "silicone", faces="nsew")
    # the light hood: its stainless bands (the bottom one closing its underside, the top one its top), the ticker band
    # between them (live faces), two linear LED diffusers and four spots underneath
    box(m, (CX - HALF, HY0, CZ - HALF), (CX + HALF, HY0 + 0.035, CZ + HALF), "steel", faces="nsewd")
    box(m, (CX - HALF, HY1 - 0.03, CZ - HALF), (CX + HALF, HY1, CZ + HALF), "steel", faces="nsewu")
    rail = sides(RAIL_HALF, RAIL_Y0, RAIL_Y1)
    for i, s in enumerate(("s", "e", "n", "w")):
        m.quad(rail[s], "dots", [(0, 0), (1, 0), (1, 1), (0, 1)], ao=False)
        m.live.append((TICKER, i, rail[s]))
    for zz in (-0.95, 0.95):
        box(m, (CX - 1.9, HY0 - 0.012, CZ + zz - 0.07), (CX + 1.9, HY0, CZ + zz + 0.07), "diffuser", faces="d", lit=True)
        box(m, (CX - 1.94, HY0 - 0.016, CZ + zz - 0.09), (CX + 1.94, HY0 - 0.012, CZ + zz + 0.09), "spot_ring", faces="nsew")
    for sx in (1, -1):
        for sz in (1, -1):
            cx, cz = CX + sx * 1.65, CZ + sz * 1.65
            m.drum(cx, cz, 0.075, HY0 - 0.06, HY0, "spot_ring", n=16)
            disc_down(m, cx, cz, 0.055, HY0 - 0.06 - 0.002, "spot", lit=True)
            disc_down(m, cx, cz, 0.075, HY0 - 0.06, "spot_ring")
    return m


# ------------------------------------------------------------------ writing

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
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["GGG", "GLG", "PCP"], {
        "G": "jugcraft:framed_glass", "L": "minecraft:glowstone_dust", "C": "jugcraft:advanced_circuit", "P": "#c:plates/steel"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Drone Display Case needs a clear space 5 by 5 blocks and 4 high"
    lang[f"message.jugcraft.{ID}.tier"] = "Display case: %s"
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
    m = case()
    TCC.bake(m, floor_y=0.0, res=1 / 16, reach=0.6, rays=16)
    write_mesh(m, atlas, MESH)
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m


if __name__ == "__main__":
    m = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(len(m.quads), "quads", {k: kinds.count(k) for k in sorted(set(kinds))}, len(m.live), "ticker faces")
