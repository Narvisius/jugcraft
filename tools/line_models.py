"""Power lines round 2 in game (Justin, 20 October 2026, "Power lines round 2": build the approved redesign,
claude/jugcraft-power-lines-redesign.md): the model exporter. It runs the approved support generators in
tools/line_supports.py and writes what client/LineRenderer.java and powerline/LineGeometry.java read:

- every support's tangent and angle (dead-end) body, and each family's strain assembly or wooden dead-end arm (the
  game turns one to each span), as quads in the game's frame (the generators' x mirrored, which is why their plates
  read the right way round, as the plant's equipment does), in three levels of detail: everything near; small parts
  left out and each face one flat colour (its texture's average) from 48 blocks; only the main members from 128, so a
  distant tower neither sparkles nor costs what a close one does. assets/jugcraft/power_lines/models/<name>.bin;
- one texture for all of them, textures/entity/line_models.png: the equipment sheet's and the line sheet's cells the
  generators draw in, the legends (plates, flags), a white cell the flat faces are tinted from, and the plate letters
  the game sets each structure's own number in;
- supports.json: per support its conductor points (sub-conductors in phases), shield-wire points, the angle body's
  hinges and its assembly's conductor ends and jumper pads, the legs (for the collision frame), the number plates, the
  wire sizes and the texture regions the game draws wires, jumpers, pilot strings and spacers with.

A support's frame (line_supports.py): its anchor at x = z = 0 on the ground, the line along z. In game, local +z faces
the line's mean travel direction (LineGeometry.yaw) and the frame turns about the anchor.
"""
import json
import math
import struct
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import control_boards as CB
import line_supports as LS
import nuclear as N
import site_bake as SB
import transformer_yard as TY

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft"
OUT = ASSETS / "power_lines"
TEXTURE = ASSETS / "textures" / "entity" / "line_models.png"
NUMBER = "NUMBER"                      # the plate text the game replaces with the structure's own number
SCALE = 256                            # model positions: int16 in 1/256 block (range +-128 blocks)
ATLAS_W = 2048
LOD_NEAR, LOD_MID = 48, 128            # blocks (LineRenderer.LOD_*)
GLYPHS = "0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZkh+/. "      # (kh: the pedestal meter's kWh)
GLYPH_PX = 48                          # font size the plate letters are drawn at
GLYPH_CELL = 64                        # their cells' height

# ---------------------------------------------------------------- what is exported

# Support key: its tangent and angle bodies, sub-conductors a phase, the plate text ({n}: the number), the wires.
SUPPORTS = {
    "wooden_pole": dict(t=lambda: LS.wooden_pole(NUMBER), a=lambda: LS.wood_angle_pole(1, NUMBER), n=1, plate="P-{n4}",
                        wire=dict(r=0.008, sag=0.03, shield_r=0.0, spacer=None)),
    "double_arm_pole": dict(t=lambda: LS.double_arm_pole(NUMBER), a=lambda: LS.wood_angle_pole(2, NUMBER), n=1, plate="P-{n4}",
                            wire=dict(r=0.009, sag=0.03, shield_r=0.0, spacer=None)),
    "spun_concrete_pole": dict(t=lambda: LS.spun_concrete_pole(NUMBER), a=lambda: LS.wood_angle_pole(3, NUMBER), n=1, plate="C-{n4}",
                               wire=dict(r=0.01, sag=0.03, shield_r=0.0, spacer=None)),
    "steel_monopole": dict(t=lambda: LS.steel_monopole(NUMBER), a=lambda: LS.steel_monopole(NUMBER, angle=True), n=1,
                           plate="TR-138  STR {n}", wire=dict(r=0.012, sag=0.03, shield_r=0.006, spacer=None)),
    "double_circuit_monopole": dict(t=lambda: LS.double_circuit_monopole(NUMBER), a=lambda: LS.double_circuit_monopole(NUMBER, angle=True),
                                    n=1, plate="TR-230  STR {n}", wire=dict(r=0.013, sag=0.03, shield_r=0.006, spacer=None)),
    "compact_monopole": dict(t=lambda: LS.compact_monopole(NUMBER), a=lambda: LS.compact_monopole(NUMBER, angle=True), n=2,
                             plate="TR-345  STR {n}", wire=dict(r=0.014, sag=0.03, shield_r=0.006, spacer="rod")),
    "lattice_345": dict(t=lambda: LS.lattice_345(NUMBER), a=lambda: LS.lattice_345_angle(NUMBER), n=2, plate="345 kV  TOWER {n}",
                        wire=dict(r=0.015, sag=0.03, shield_r=0.006, spacer="rod")),
    "lattice_500": dict(t=lambda: LS.lattice_500(NUMBER), a=lambda: LS.lattice_500_angle(NUMBER), n=3, plate="500 kV  TOWER {n}",
                        wire=dict(r=0.015, sag=0.03, shield_r=0.006, spacer="rod")),
    "lattice_500r": dict(t=lambda: LS.lattice_500(NUMBER, "recond"), a=lambda: LS.lattice_500_angle(NUMBER, "recond"), n=3,
                         plate="500 kV  TOWER {n}", wire=dict(r=0.015, sag=0.018, shield_r=0.006, spacer="damper")),
    "lattice_500m": dict(t=lambda: LS.lattice_500(NUMBER, "monitored"), a="lattice_500r", n=3,
                         plate="500 kV  TOWER {n}", wire=dict(r=0.015, sag=0.018, shield_r=0.006, spacer="damper")),
    "uhv_765": dict(t=lambda: LS.uhv_765(NUMBER), a=lambda: LS.uhv_765_angle(NUMBER), n=6, plate="765 kV  TOWER {n}",
                    wire=dict(r=0.016, sag=0.03, shield_r=0.007, spacer="rod")),
    "uhvdc": dict(t=lambda: LS.uhvdc_tower(NUMBER), a=lambda: LS.uhvdc_angle(NUMBER), n=8, plate="UHVDC  TOWER {n}",
                  wire=dict(r=0.018, sag=0.03, shield_r=0.008, spacer="rod")),
}

# The parts the game draws itself (wires, jumpers, pilot strings, spacers, line-end tails): their materials.
REGIONS = {"aluminium": "ls:aluminium", "galv": "ls:galv", "polymer": "ls:polymer", "galv_dark": "ls:galv_dark",
           "creosote_butt": "ls:creosote_butt", "guard_yellow": "ls:guard_yellow",
           # part 2: the leads, the service drop's insulation, the superconducting cable, its sleeves and glow
           "copper_wire": "ls:copper_wire", "ground_molding": "ls:ground_molding", "cryo_jacket": "ls:cryo_jacket",
           "composite_white": "ls:composite_white", "sc_glow": "ls:sc_glow"}


# ---------------------------------------------------------------- recording what the generators build

class Recorder:
    """Notes what lattice_body is given (the legs: base and waist half-widths, waist height), for the collision frame."""

    def __init__(self):
        self.legs = None
        self._orig = LS.lattice_body

    def __enter__(self):
        def rec(b, B, Wd, wy, *a, **k):
            if self.legs is None:
                self.legs = dict(base=float(B), waist=float(Wd), waist_y=float(wy))
            return self._orig(b, B, Wd, wy, *a, **k)
        LS.lattice_body = rec
        return self

    def __exit__(self, *exc):
        LS.lattice_body = self._orig


def _is_number(mat):
    return isinstance(mat, list) and len(mat) == 1 and isinstance(mat[0], str) and mat[0].startswith("@") and NUMBER in mat[0]


def _box_face(op, i):
    """The corners (tl, bl, br, tr) of face i of a 'b' or 'o' op, as site_bake.Tess.box6 makes them."""
    if op[0] == "b":
        x0, y0, z0, x1, y1, z1 = op[1:7]
        p = lambda a, b_, c: np.array((x1 if a else x0, y1 if b_ else y0, z1 if c else z0), float)
    else:
        c = np.array(op[1:4], float)
        u = np.array(op[4:7], float)
        w0 = np.array(op[7:10], float)
        hu, hv, hw = op[10:13]
        w = np.cross(u, w0)
        p = lambda a, b_, kk: c + u * (hu * (2 * a - 1)) + w0 * (hv * (2 * b_ - 1)) + w * (hw * (2 * kk - 1))
    faces = [(p(0, 1, 0), p(0, 0, 0), p(1, 0, 0), p(1, 1, 0)), (p(1, 1, 1), p(1, 0, 1), p(0, 0, 1), p(0, 1, 1)),
             (p(0, 1, 1), p(0, 0, 1), p(0, 0, 0), p(0, 1, 0)), (p(1, 1, 0), p(1, 0, 0), p(1, 0, 1), p(1, 1, 1)),
             (p(0, 1, 1), p(0, 1, 0), p(1, 1, 0), p(1, 1, 1)), (p(0, 0, 0), p(0, 0, 1), p(1, 0, 1), p(1, 0, 0))]
    return faces[i]


def take_plates(b):
    """The number plates (the tag carrying NUMBER): each one's corners in the generator's frame, its picture swapped
    for a blank tag of the same size (the game sets the letters on it)."""
    plates = []

    def blank(w, h):
        return TY.ATLAS.mat(TY.ATLAS.add(f"tag:blank:{w:.3f}:{h:.3f}", TY.tag_img("", w, h)))

    for lst in (b.still, b.lit):
        for op in lst:
            if op[0] == "q" and _is_number(op[13]):
                corners = [np.array(op[i:i + 3], float) for i in (1, 4, 7, 10)]
            elif op[0] in ("b", "o"):
                faces = op[7] if op[0] == "b" else op[13]
                if not isinstance(faces, list) or len(faces) != 6 or not any(_is_number(f) for f in faces):
                    continue
                i = next(k for k, f in enumerate(faces) if _is_number(f))
                corners = list(_box_face(op, i))
            else:
                continue
            tl, bl, br, tr = corners
            w, h = float(np.linalg.norm(tr - tl)), float(np.linalg.norm(bl - tl))
            mat = blank(w, h)
            if op[0] == "q":
                op[13] = mat
            else:
                faces[i] = mat
            plates.append(dict(corners=[mirror(p) for p in corners], w=round(w, 4), h=round(h, 4)))
    return plates


def mirror(p):
    """A point from the generators' frame to the game's (x mirrored)."""
    return [round(-float(p[0]), 5), round(float(p[1]), 5), round(float(p[2]), 5)]


# ---------------------------------------------------------------- levels of detail

def _extent(op):
    k = op[0]
    if k == "b":
        return max(abs(op[4] - op[1]), abs(op[5] - op[2]), abs(op[6] - op[3]))
    if k == "o":
        return 2 * max(op[10], op[11], op[12])
    if k == "q":
        P = [np.array(op[i:i + 3], float) for i in (1, 4, 7, 10)]
        return max(np.linalg.norm(P[1] - P[0]), np.linalg.norm(P[3] - P[0]))
    return None


def lod_ops(ops, level):
    """The ops a level draws: level 1 leaves out bolts, nuts, staples and the thinnest wires and makes round parts
    coarser; level 2 keeps only what reads from 128 blocks (legs, members, arms, poles, big plates)."""
    if level == 0:
        return ops
    out = []
    for op in ops:
        k = op[0]
        if k == "c":
            a, c = np.array(op[1:4], float), np.array(op[4:7], float)
            L, r, sides = float(np.linalg.norm(c - a)), float(op[7]), int(op[8])
            if level == 1:
                if r < 0.006 or (r < 0.013 and L < 1.0) or (L < 0.02 and r < 0.03):
                    continue
                sides = max(4, min(SB.sides_for(r, sides), 8))
            else:
                if r < 0.02 or (r < 0.045 and L < 2.0) or L < 0.05:
                    continue
                sides = 6 if r > 0.25 else 4
            op = list(op)
            op[8] = sides
            out.append(op)
        else:
            e = _extent(op)
            if level == 1 and e < (0.06 if k != "q" else 0.1):
                continue
            if level == 2 and e < (0.3 if k != "q" else 0.5):
                continue
            out.append(op)
    return out


class FlatTess(SB.Tess):
    """Tessellation for the flat-colour levels: a face is one quad (no texture to repeat), a cylinder one row."""

    def tiled(self, tl, bl, br, tr, n, reg, lit, tile=1.0):
        tex, U0, V0, U1, V1, _ = reg
        self.quad(tl, bl, br, tr, n, tex, U0, V0, U1, V1, lit)

    def revolve(self, a, b, profile, sides, reg, lit):
        a, b = np.asarray(a, float), np.asarray(b, float)
        axis = b - a
        axis = axis / np.linalg.norm(axis)
        ref = np.array([0.0, 0.0, 1.0]) if abs(axis[1]) > 0.9 else np.array([0.0, 1.0, 0.0])
        n1 = np.cross(axis, ref)
        n1 /= np.linalg.norm(n1)
        n2 = np.cross(n1, axis)
        n2 /= np.linalg.norm(n2)
        tex, U0, V0, U1, V1, _ = reg
        for i in range(len(profile) - 1):
            d0, d1, r0, r1 = profile[i][0], profile[i + 1][0], profile[i][1], profile[i + 1][1]
            for k in range(sides):
                g0, g1 = 2 * math.pi * k / sides, 2 * math.pi * (k + 1) / sides
                dir0 = n1 * math.cos(g0) + n2 * math.sin(g0)
                dir1 = n1 * math.cos(g1) + n2 * math.sin(g1)
                p00, p01 = a + axis * d0 + dir0 * r0, a + axis * d1 + dir0 * r1
                p10, p11 = a + axis * d0 + dir1 * r0, a + axis * d1 + dir1 * r1
                n = dir0 + dir1
                n = n / np.linalg.norm(n)
                self.quad(p01, p00, p10, p11, n, tex, U0, V0, U1, V1, lit)


def tessellate(ops, level):
    """Polygons (pts, uvs, normal, texture, lit, blink) of a model's ops at a level."""
    t = SB.Tess() if level == 0 else FlatTess()
    out = []
    for name, lit, blink in (("still", False, False), ("lit", True, False), ("blink", True, True)):
        t.polys = []
        for op in lod_ops(ops.get(name, []), level):
            if level > 0 and op[0] == "c":
                # a flat level's cylinders: no small-radius side reduction from site_bake (sides already chosen)
                a, b = np.array(op[1:4], float), np.array(op[4:7], float)
                L = np.linalg.norm(b - a)
                if L < 1e-9:
                    continue
                t.revolve(a, b, [(0.0, op[7]), (L, op[7])], int(op[8]), SB.region(op[9]), lit)
                if op[10] is not None:
                    t.disc(b, (b - a) / L, op[7], int(op[8]), SB.region(op[10]), lit)
            else:
                t.op(op, lit)
        out += [(p[0], p[1], p[2], p[3], p[4], blink) for p in t.polys]
    return out


# ---------------------------------------------------------------- textures

class Textures:
    """The images polygons name: the equipment sheet's cells, the line sheet's ("ls:..."), legends (by atlas key)."""

    def __init__(self):
        sheet = Image.open(ASSETS / "textures" / "entity" / "nuclear_equipment.png").convert("RGBA")
        self.eq = {}
        for name in N.EQ_MATERIALS:
            c, r = N.eq_slot(name)
            self.eq[name] = sheet.crop((c * N.EQ_CELL, r * N.EQ_CELL, (c + 1) * N.EQ_CELL, (r + 1) * N.EQ_CELL))
        ls = LS.draw_sheet()
        for name in LS.LS_MATERIALS:
            i = LS.LS_MATERIALS.index(name)
            c, r = i % 8, i // 8
            self.eq["ls:" + name] = ls.crop((c * LS.LS_CELL, r * LS.LS_CELL, (c + 1) * LS.LS_CELL, (r + 1) * LS.LS_CELL))
        self.legend = {}                  # legend key -> image
        self.used = {}                    # texture name -> image

    def legend_of(self, u, v):
        """The legend (atlas key) a point of the packed legend atlas lies in."""
        x, y = u * TY.ATLAS_W, v * TY.ATLAS.h
        for key, (ax, ay, w, h) in TY.ATLAS.at.items():
            if ax - 0.6 <= x <= ax + w + 0.6 and ay - 0.6 <= y <= ay + h + 0.6:
                return key, ax, ay, w, h
        raise ValueError((u, v))

    def localise(self, polys):
        """Polygons with legend-atlas UVs re-expressed in their own legend's image (the atlas is packed again for
        every model, so its coordinates do not last)."""
        out = []
        img_of = dict(TY.ATLAS.items)
        for pts, uvs, n, tex, lit, blink in polys:
            if tex == TY.LEGENDS:
                c = uvs.mean(0)
                key, ax, ay, w, h = self.legend_of(c[0], c[1])
                uvs = np.stack([(uvs[:, 0] * TY.ATLAS_W - ax) / w, (uvs[:, 1] * TY.ATLAS.h - ay) / h], 1).clip(0, 1)
                tex = "leg:" + key
                self.used[tex] = img_of[key].convert("RGBA")
            else:
                if tex not in self.eq:
                    raise SystemExit(f"unknown texture {tex}")
                self.used[tex] = self.eq[tex]
            out.append((pts, uvs, n, tex, lit, blink))
        return out


def _cutout(img, translucent=False):
    a = np.asarray(img.convert("RGBA")).copy()
    if not translucent:
        a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a)


def pack(images):
    """Shelf-packs the images (2 px apart, their edge pixels repeated into the gap against bleeding). Returns the
    atlas and each one's (x, y, w, h)."""
    at, x, y, row = {}, 0, 0, 0
    for name, img in sorted(images.items(), key=lambda kv: (-kv[1].height, kv[0])):
        if x + img.width + 2 > ATLAS_W:
            x, y, row = 0, y + row + 4, 0
        at[name] = (x + 2, y + 2, img.width, img.height)
        x += img.width + 4
        row = max(row, img.height)
    h = 1
    while h < y + row + 4:
        h *= 2
    atlas = Image.new("RGBA", (ATLAS_W, h), (0, 0, 0, 0))
    for name, img in images.items():
        ax, ay, w, hh = at[name]
        a = np.asarray(img.convert("RGBA"))
        padded = np.pad(a, ((2, 2), (2, 2), (0, 0)), mode="edge")
        atlas.paste(Image.fromarray(padded), (ax - 2, ay - 2))
    return atlas, at


def glyph_strip():
    """The plate letters (the tags' font, dark on clear), one cell each, and their advances (px at GLYPH_PX)."""
    f = TY._font(GLYPH_PX)
    cells = {}
    for ch in GLYPHS:
        tmp = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
        adv = tmp.textlength(ch, font=f)
        w = max(4, int(math.ceil(adv)) + 4)
        im = Image.new("RGBA", (w, GLYPH_CELL), (0, 0, 0, 0))
        if ch != " ":
            ImageDraw.Draw(im).text((2, GLYPH_CELL / 2), ch, font=f, fill=(36, 38, 42, 255), anchor="lm")
        cells[ch] = (im, adv)
    return cells


# ---------------------------------------------------------------- writing models

def avg_colour(img, uvs, cache):
    """The average colour of a polygon's patch of its texture (opaque pixels), and whether it is mostly see-through."""
    u0, v0 = uvs.min(0)
    u1, v1 = uvs.max(0)
    key = (id(img), round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3))
    if key not in cache:
        a = np.asarray(img).astype(np.float64)
        H, W = a.shape[:2]
        x0, x1 = int(min(W - 1, max(0, u0 * W))), int(min(W, max(1, math.ceil(u1 * W))))
        y0, y1 = int(min(H - 1, max(0, v0 * H))), int(min(H, max(1, math.ceil(v1 * H))))
        cell = a[y0:max(y1, y0 + 1), x0:max(x1, x0 + 1)].reshape(-1, 4)
        opaque = cell[cell[:, 3] > 127]
        if len(opaque) < 0.5 * len(cell):
            cache[key] = None
        else:
            cache[key] = tuple(int(round(v)) for v in opaque[:, :3].mean(0))
    return cache[key]


def model_bytes(levels, at, atlas_size, images, white):
    """A model's levels as the game reads them (big-endian): "JLM1", levels; per level its quads: flags (1 lit,
    2 blinking, 4 flat colour), the corners (int16 x 12, 1/256 block), the normal (int8 x 3), then UVs (uint16 x 8,
    the atlas in 1/65535) or the colour (RGB) for a flat level."""
    W, H = atlas_size
    out = bytearray(b"JLM1")
    out += struct.pack(">B", len(levels))
    cache = {}
    wx, wy, ww, wh = at[white]
    wu, wv = (wx + ww / 2) / W, (wy + wh / 2) / H
    for level, polys in enumerate(levels):
        quads = []
        for pts, uvs, n, tex, lit, blink in polys:
            if len(pts) != 4:
                continue
            # the game's frame: x mirrored; the corners keep their order (the mirror turns the generators' winding
            # into the game's, as the plant's sites do)
            P = np.array(pts, float) * [-1, 1, 1]
            nn = np.array(n, float) * [-1, 1, 1]
            flags = (1 if lit else 0) | (2 if blink else 0)
            if level == 0:
                x, y, w, h = at[tex]
                U = ((x + 0.5) + uvs[:, 0] * (w - 1)) / W
                V = ((y + 0.5) + uvs[:, 1] * (h - 1)) / H
                tail = struct.pack(">8H", *[int(round(min(1.0, max(0.0, c)) * 65535)) for uv in zip(U, V) for c in uv])
            else:
                col = avg_colour(images[tex], uvs, cache)
                if col is None:
                    continue
                flags |= 4
                tail = struct.pack(">3B", *col)
            q = np.round(P * SCALE).astype(int)
            if np.abs(q).max() > 32767:
                raise SystemExit("model too big for int16 positions")
            nq = np.clip(np.round(nn * 127), -127, 127).astype(int)
            quads.append(struct.pack(">B", flags) + struct.pack(">12h", *q.flatten()) + struct.pack(">3b", *nq) + tail)
        out += struct.pack(">I", len(quads))
        for qb in quads:
            out += qb
    return bytes(out), [sum(1 for _ in lv) for lv in levels]


# ---------------------------------------------------------------- the kits

def kit_board(seed):
    b = CB.Board(4, seed, face=0.0)
    b.live = []
    return b


def strain_kit(spec, seed):
    """One strain assembly hanging off the origin, pointing along +z: its board, conductor ends and jumper pads."""
    b = kit_board(seed)
    ends, pads = LS.kit_strain(b, (0.0, 0.0, 0.0), (0.0, 0.0, 1.0), **spec)
    return b, ends, pads


def dist_kit(tier, level, seed):
    """A wooden family's double dead-end arm at its level (0 top) square to a span along +z, with the span's down guy."""
    P = {1: LS.WOOD_POLE, 2: LS.DOUBLE_POLE, 3: LS.CONCRETE_POLE}[tier]
    top = P["top"]
    rad = lambda y: P["r0"] + (P["r1"] - P["r0"]) * min(1.0, max(0.0, y / top))
    arm_half = P.get("arm_half", 1.22) if tier < 3 else 1.3
    arm_mat = "crossarm" if tier < 3 else "galv"
    y = top - 0.65 - 0.8 * level
    b = kit_board(seed)
    d = (0.0, 0.0, 1.0)
    ends, pads = LS.kit_dist(b, (0, 0, 0), rad, y, d, arm_half=arm_half, arm_mat=arm_mat)
    f, up, s = LS._frame(d)
    LS.down_guy(b, np.array([0, y - 0.3, 0]) - f * (rad(y - 0.3) + 0.02), d)
    return b, ends, pads, y


def kit_key(spec):
    return json.dumps(spec, sort_keys=True)


# ---------------------------------------------------------------- main

def split_feet(ops, legs):
    """A lattice body's footings (the concrete piers and their ground straps, below 0.65 m at each corner) as four
    separate parts, so the game can set each on the ground under its leg (leg extensions on a slope). Corner order as
    LineGeometry.Layout.legCorners: (-x -z), (+x -z), (-x +z), (+x +z) in the game's frame."""
    B = legs["base"]
    body = {k: [] for k in ("still", "lit", "blink")}
    feet = [{"still": [], "lit": [], "blink": []} for _ in range(4)]

    def where(op):
        k = op[0]
        if k == "b":
            lo, hi = np.array(op[1:4], float), np.array(op[4:7], float)
        elif k == "o":
            c = np.array(op[1:4], float)
            r = max(op[10:13]) * 1.8
            lo, hi = c - r, c + r
        elif k == "q":
            P = np.array([op[i:i + 3] for i in (1, 4, 7, 10)], float)
            lo, hi = P.min(0), P.max(0)
        else:
            P = np.array([op[1:4], op[4:7]], float)
            lo, hi = P.min(0) - op[7], P.max(0) + op[7]
        return lo, hi

    for name in ("still", "lit", "blink"):
        for op in ops.get(name, []):
            lo, hi = where(op)
            c = (lo + hi) / 2
            gx = -c[0]                       # the game's x
            if hi[1] <= 0.65 and abs(abs(gx) - B) < 1.3 and abs(abs(c[2]) - B) < 1.3:
                k = (0 if gx < 0 else 1) + (0 if c[2] < 0 else 2)
                feet[k][name].append(op)
            else:
                body[name].append(op)
    return body, feet


def build_model(fn):
    """A generator's board: (ops with legends placed, metadata board, legs, plates)."""
    with Recorder() as rec:
        b = fn()
    plates = take_plates(b)
    ops = TY.finished(b)
    return ops, b, rec.legs, plates


def number_meta(plates, fmt):
    return [dict(p, text=fmt) for p in plates]


# ---------------------------------------------------------------- part 2: the superconducting line, the boxes, pole add-ons

SENT = 1.0e4           # a point that marks what the game draws itself (skipped while exporting)


class Patched:
    """Swaps attributes of line_supports while a model is built (to leave out what the game turns or draws itself)."""

    def __init__(self, **swaps):
        self.swaps, self.saved = swaps, {}

    def __enter__(self):
        for k, v in self.swaps.items():
            self.saved[k] = getattr(LS, k)
            setattr(LS, k, v)
        return self

    def __exit__(self, *exc):
        for k, v in self.saved.items():
            setattr(LS, k, v)


def _far(pts):
    return any(np.abs(np.asarray(p, float)).max() > 1000 for p in pts)


def moved(p, offset):
    """A point from the generators' frame to the game's, a box's offset added."""
    m = mirror(p)
    return [round(m[0] + offset[0], 5), round(m[1] + offset[1], 5), round(m[2] + offset[2], 5)]


def box_frames(ops, offset, half, depth):
    """A box's frame cells (from its anchor, before it turns): the fence ring round its yard (x -half..half, z 0..depth;
    the anchor's cell left out) and the cells its equipment fills up to 4 m (solid ops; wires and fence fabric not)."""
    cells = set()
    if half:
        for x in range(-half, half + 1):
            for z in range(0, depth + 1):
                if abs(x) == half or z in (0, depth):
                    cells.add((x, 0, z))
                    cells.add((x, 1, z))
    vox = SB.Voxels()
    for op in ops.get("still", []):
        if op[0] == "c" and op[7] < 0.06:
            continue
        if op[0] == "b" and max(op[2], op[5]) <= 0.2:
            continue                 # the yard's gravel, pads and footings at the ground: walked on, not into
        if op[0] in ("b", "o"):
            e = _extent(op)
            if e < 0.3:
                continue
        if op[0] == "q":
            continue
        vox.op(op, min_thick=0.015, min_radius=0.015)
    from collections import Counter
    count = Counter()
    res = SB.RES
    for (bx, by, bz), arr in vox.blocks.items():
        idx = np.argwhere(arr)
        if len(idx) == 0:
            continue
        pts = np.array([bx, by, bz], float) + (idx + 0.5) / res          # voxel centres, the generators' frame
        gx = -pts[:, 0] + offset[0]
        gz = pts[:, 2] + offset[2]
        gy = pts[:, 1] + offset[1]
        keep = (gy >= 0) & (gy < 4)
        for c in zip(np.floor(gx[keep] + 0.5).astype(int), np.floor(gy[keep]).astype(int), np.floor(gz[keep] + 0.5).astype(int)):
            count[c] += 1
    for c, n in count.items():
        if n >= 200:                 # a sliver (of 4096 voxels a block) is not worth a collision block
            cells.add(tuple(int(v) for v in c))
    cells.discard((0, 0, 0))
    return sorted([list(c) for c in cells])


def export_part2(add_model, meta, kit_names):
    """Part 2 (Justin, 20 October 2026: "Power lines part 2"): the superconducting pylon (the game bends its sleeves and
    hangs its cable), the four boxes (their strain assemblies, cryogenic sleeves and the pedestal's meter figures are
    the game's), and what a pole wired to a box carries: the pole-mount transformer for a Service Pedestal, the riser for
    a Pad-Mount Transformer."""
    boxes = meta.setdefault("boxes", {})

    # The superconducting pylon (its sleeves drawn by the game, bent to each turn).
    print("superconducting", file=sys.stderr)
    ops, b, legs, plates = build_model(lambda: LS.superconducting_pylon(NUMBER, sleeve=False))
    add_model("superconducting", ops)
    body = dict(model="superconducting", attach=[mirror(p) for p in b.attach], shield=[mirror(p) for p in b.messenger], legs=None,
                plates=plates)
    meta["supports"]["superconducting"] = {"n": 1, "plate": "SC-1  PYLON {n}", "tangent": body, "angle": dict(body, kind="tangent"),
                                           "wire": dict(r=0.08, sag=0.004, shield_r=0.01, spacer=None),
                                           "sleeve": dict(half=LS.SC["sleeve"], r=0.12)}

    # The boxes: their strain assemblies recorded (one per line in game, turned to it) and left out, with the jumpers
    # from them; the cryogenic sleeves left out.
    def recording(rec):
        def kit_strain(b_, hinge, d, k_index=0, **spec):
            rec["kits"].append(dict(hinge=np.array(hinge, float), spec=spec))
            n = spec.get("n", 1)
            return [np.array([SENT] * 3)] * n, [np.array([SENT] * 3)] * n

        orig_bezier = LS._bezier

        def bezier(p0, p1, p2, n):
            if _far([p0]) and rec["kits"] and "jumper" not in rec["kits"][-1]:
                rec["kits"][-1]["jumper"] = dict(control=np.array(p1, float), target=np.array(p2, float))
            return orig_bezier(p0, p1, p2, n)

        orig_path = LS.path

        def path(b_, pts, r, mat, sides=10):
            if _far(pts):
                return
            return orig_path(b_, pts, r, mat, sides)

        orig_cyl = LS.cyl

        def cyl(b_, a, c, r, mat, cap=None, sides=16, lst=None):
            if (abs(r - 0.13) < 1e-9 and mat == LS.ls("composite_white")) or (abs(r - 0.135) < 1e-9 and mat == LS.ls("sc_glow")):
                return               # a cryogenic termination's cable-entry sleeve: the game turns it to its line
            return orig_cyl(b_, a, c, r, mat, cap, sides, lst)
        return dict(kit_strain=kit_strain, _bezier=bezier, path=path, cyl=cyl)

    def canon_ops(ops):
        # (a legend's place in the atlas differs between two builds: compared by its material's name, not its UVs)
        import water_works as WW

        def strip(v):
            if isinstance(v, (list, tuple)):
                if len(v) == 5 and isinstance(v[0], str) and all(isinstance(x, (int, float)) for x in v[1:]):
                    return v[0]
                return [strip(x) for x in v]
            return v
        return [WW.canon(strip(o)) for o in ops]

    BOXES = {
        "service_pedestal": dict(fn=lambda: LS.service_pedestal(NUMBER), offset=(0.0, 0.0, 0.0), half=0, depth=0,
                                 plate="200 A  120/240 V  SVC {n4}-1"),
        "pad_mount_transformer": dict(fn=lambda: LS.pad_mount_transformer(NUMBER), offset=(0.5, 0.0, 0.5), half=0, depth=0,
                                      plate="PMT-{n4}  500 kVA"),
        "unit_substation": dict(fn=lambda: LS.unit_substation("SUB  NUMBER  UNIT SUBSTATION", cryo=False), offset=(0.0, 0.0, 5.0),
                                half=5, depth=10, plate="SUB {n}  UNIT SUBSTATION",
                                cryo=lambda: LS.unit_substation("SUB  NUMBER  UNIT SUBSTATION", cryo=True)),
        "converter_station": dict(fn=lambda: LS.converter_station("HVDC CONVERTER STATION NUMBER"), offset=(0.0, 0.0, 7.0), half=7,
                                  depth=14, plate="HVDC CONVERTER STATION {n}"),
    }
    for key, B in BOXES.items():
        print(key, file=sys.stderr)
        rec = {"kits": []}
        off = B["offset"]
        orig_add = TY.ATLAS.add

        def add(k_, img):
            if k_ == "lcd:meter":                # the meter's figures are set by the game: a blank display
                img = Image.new("RGBA", img.size, (138, 186, 140, 255))
            return orig_add(k_, img)
        TY.ATLAS.add = add
        try:
            with Patched(**recording(rec)):
                ops, b, legs, plates = build_model(B["fn"])
        finally:
            del TY.ATLAS.add
        entry = dict(model=key, plate=B["plate"], shield=[moved(p, off) for p in getattr(b, "shield", [])],
                     plates=[dict(p, corners=[[c[0] + off[0], c[1] + off[1], c[2] + off[2]] for c in p["corners"]]) for p in plates])
        if key == "service_pedestal":
            entry["attach"] = [moved(p, off) for p in b.attach]
        if rec["kits"]:
            spec = rec["kits"][0]["spec"]
            kk = kit_key(spec)
            if kk not in kit_names:
                kn = f"kit_strain_{len(kit_names)}"
                kit_names[kk] = kn
                kb, ends, pads = strain_kit(spec, 4200 + len(kit_names))
                add_model(kn, TY.finished(kb))
                meta["kits"][kn] = dict(model=kn, ends=[mirror(e) for e in ends], pads=[mirror(p) for p in pads], spec=spec)
            entry["kit"] = kit_names[kk]
            entry["hinges"] = [moved(k_["hinge"], off) for k_ in rec["kits"]]
            entry["jumpers"] = [dict(control=moved(k_["jumper"]["control"], off), target=moved(k_["jumper"]["target"], off))
                                for k_ in rec["kits"]]
        if "cryo" in B:
            rec2 = {"kits": []}
            with Patched(**recording(rec2)):
                full = TY.finished(B["cryo"]())
            # what the cryogenic terminations add: the vessels, the nitrogen header, their tag
            from collections import Counter
            base_c = Counter(canon_ops(ops["still"]) + ["lit:" + c for c in canon_ops(ops["lit"])])
            extra = {"still": [], "lit": [], "blink": []}
            for name in ("still", "lit"):
                for o, c in zip(full[name], canon_ops(full[name])):
                    k_ = c if name == "still" else "lit:" + c
                    if base_c[k_] > 0:
                        base_c[k_] -= 1
                    else:
                        extra[name].append(o)
            add_model(key + "_cryo", extra, offset=off)
            entry["cryo"] = dict(model=key + "_cryo", vessels=[moved((x, 1.6, -3.9), off) for x in (2.7, 3.9)])
        entry["frames"] = box_frames(ops, off, B["half"], B["depth"])
        add_model(key, ops, offset=off)
        boxes[key] = entry

    # The pedestal's meter: its display quad (the game sets kWh, volts and amps on it).
    ped = boxes["service_pedestal"]
    d = 0.13
    mc = np.array([0, 1.22, -d])
    c = mc + [0, 0.012, -0.172]
    w, h = 0.09, 0.037
    # face_quad toward -z: right = cross(n, up) in the generators' frame; in the game's the corners mirrored
    tl, bl, br, tr = c + [-w / 2, h / 2, 0], c + [-w / 2, -h / 2, 0], c + [w / 2, -h / 2, 0], c + [w / 2, h / 2, 0]
    ped["meter"] = dict(corners=[moved(p, (0, 0, 0)) for p in (tl, bl, br, tr)], w=w, h=h)

    # What a pole carries for a box: the pole-mount transformer (and its drop) for a Service Pedestal, the riser for a
    # Pad-Mount Transformer; each side of the pole (the game hangs it toward the box), for each pole it can be on.
    orig_radius = LS.pole_radius
    POLES = {"wooden_pole": lambda y: orig_radius(y), "double_arm_pole": lambda y: LS.DOUBLE_POLE["r0"] + (LS.DOUBLE_POLE["r1"] - LS.DOUBLE_POLE["r0"]) * min(1.0, max(0.0, y / LS.DOUBLE_POLE["top"])),
             "spun_concrete_pole": lambda y: LS.CONCRETE_POLE["r0"] + (LS.CONCRETE_POLE["r1"] - LS.CONCRETE_POLE["r0"]) * min(1.0, max(0.0, y / LS.CONCRETE_POLE["top"])),
             "steel_monopole": lambda y: LS.mono_radius(y, LS.MONO), "double_circuit_monopole": lambda y: LS.mono_radius(y, LS.MONO2),
             "compact_monopole": lambda y: LS.mono_radius(y, LS.MONO3)}
    for pole, rad in POLES.items():
        sup = meta["supports"][pole]
        for gside in (1, -1):
            s_ = -gside                  # the generators' side (their x mirrored)
            leads = []
            orig_path = LS.path

            def path(b_, pts, r, mat, sides=10, leads=leads):
                pts = [np.asarray(p, float) for p in pts]
                if _far(pts) or (abs(r - 0.006) < 1e-9 and pts[0][1] > 10.6 and mat == LS.ls("copper_wire") and len(pts) == 3):
                    leads.append(pts[-1])          # a lead from the line: the game draws it from the phase it takes
                    return
                return orig_path(b_, pts, r, mat, sides)
            if pole in ("wooden_pole", "double_arm_pole", "spun_concrete_pole"):
                with Patched(path=path, pole_radius=lambda y, r0=None, r1=None, top=None: rad(y)):
                    bx = kit_board(4300)
                    drop = LS.pole_transformer(bx, s_)
                name = f"{pole}_xfmr{'p' if gside > 0 else 'n'}"
                add_model(name, TY.finished(bx))
                sup.setdefault("xfmr", {})[str(gside)] = dict(model=name, drop=mirror(drop), lead=mirror(leads[0]) if leads else None)
            leads.clear()
            with Patched(path=path, pole_radius=lambda y, r0=None, r1=None, top=None: rad(y)):
                bx = kit_board(4310)
                cables = LS.pole_riser(bx, s_, [np.array([SENT] * 3)] * 3)
            name = f"{pole}_riser{'p' if gside > 0 else 'n'}"
            add_model(name, TY.finished(bx))
            sup.setdefault("riser", {})[str(gside)] = dict(model=name, leads=[mirror(p) for p in leads[:3]])
    LS.pole_radius = orig_radius


def main():
    TY._faces()
    tex = Textures()
    models = {}            # model name -> polys per level (localised)
    meta = {"supports": {}, "kits": {}, "lod": [LOD_NEAR, LOD_MID], "scale": SCALE}
    kit_names = {}

    def add_model(name, ops, offset=None):
        levels = [tex.localise(tessellate(ops, lv)) for lv in range(3)]
        if offset is not None:
            # (a box's anchor is its terminal cabinet: the model moved so the anchor's cell holds it; offset in the
            # game's frame, so the generators' x the other way)
            t = np.array([-offset[0], offset[1], offset[2]], float)
            levels = [[(np.asarray(pts, float) + t, uvs, n, tx_, lit, blink) for pts, uvs, n, tx_, lit, blink in lv] for lv in levels]
        models[name] = levels
        print(f"  {name}: {[len(l) for l in levels]} quads", file=sys.stderr)

    for key, S in SUPPORTS.items():
        print(key, file=sys.stderr)
        entry = {"n": S["n"], "wire": S["wire"], "plate": S["plate"]}
        # the tangent body
        ops, b, legs, plates = build_model(S["t"])
        if key == "lattice_345":
            T = TY.T345
            legs = dict(base=T["base"], waist=T["waist"], waist_y=T["waist_y"])
        if legs:
            ops, feet = split_feet(ops, legs)
            for k, f in enumerate(feet):
                add_model(f"{key}_foot{k}", f)
        add_model(key, ops)
        subs = [mirror(p) for p in b.attach]
        entry["tangent"] = dict(model=key, attach=subs, shield=[mirror(p) for p in getattr(b, "shield", [])], legs=legs,
                                plates=plates)
        # the angle body
        if isinstance(S["a"], str):
            entry["angle"] = meta["supports"][S["a"]]["angle"]
        else:
            ops, b, legs, plates = build_model(S["a"])
            name = key + "_angle"
            if legs:
                ops, feet = split_feet(ops, legs)
                for k, f in enumerate(feet):
                    add_model(f"{name}_foot{k}", f)
            add_model(name, ops)
            K = b.kit
            a = dict(model=name, shield=[mirror(p) for p in getattr(b, "shield", [])], legs=legs, plates=plates)
            if K["kind"] == "dist":
                tier = {"wooden_pole": 1, "double_arm_pole": 2, "spun_concrete_pole": 3}[key]
                a["kind"] = "dist"
                a["kits"] = []
                P = {1: LS.WOOD_POLE, 2: LS.DOUBLE_POLE, 3: LS.CONCRETE_POLE}[tier]
                a["pole"] = dict(r0=P["r0"], r1=P["r1"], top=P["top"])
                for level in range(3):
                    kb, ends, pads, y = dist_kit(tier, level, 4100 + tier * 10 + level)
                    kn = f"kit_dist{tier}_{level}"
                    add_model(kn, TY.finished(kb))
                    meta["kits"][kn] = dict(model=kn, ends=[mirror(e) for e in ends], pads=[mirror(p) for p in pads], y=round(y, 4))
                    a["kits"].append(kn)
            else:
                a["kind"] = "strain"
                spec = K["spec"]
                kk = kit_key(spec)
                if kk not in kit_names:
                    kn = f"kit_strain_{len(kit_names)}"
                    kit_names[kk] = kn
                    kb, ends, pads = strain_kit(spec, 4200 + len(kit_names))
                    add_model(kn, TY.finished(kb))
                    meta["kits"][kn] = dict(model=kn, ends=[mirror(e) for e in ends], pads=[mirror(p) for p in pads], spec=spec)
                a["kit"] = kit_names[kk]
                a["hinges"] = [mirror(h) for h in K["hinges"]]
                a["jr"] = K.get("jr", 0.016)
                a["arc"] = {str(i): [float(v[0]), float(v[1])] for i, v in K.get("arc", {}).items()}
            entry["angle"] = a
        meta["supports"][key] = entry
    export_part2(add_model, meta, kit_names)
    # the texture: every image used, the white cell, the plate letters
    for v in REGIONS.values():
        tex.used.setdefault(v, tex.eq[v])
    images = {name: _cutout(img) for name, img in tex.used.items()}
    images["white"] = Image.new("RGBA", (8, 8), (255, 255, 255, 255))
    glyphs = glyph_strip()
    for ch, (im, adv) in glyphs.items():
        images["glyph:" + ch] = im
    atlas, at = pack(images)
    W, H = atlas.size
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(TEXTURE)

    def region(name, inset=0.5):
        x, y, w, h = at[name]
        return [round((x + inset) / W, 6), round((y + inset) / H, 6), round((x + w - inset) / W, 6), round((y + h - inset) / H, 6)]

    meta["regions"] = {k: region(v) for k, v in REGIONS.items()}
    meta["colours"] = {k: list(avg_colour(images[v], np.array([[0.0, 0.0], [1.0, 1.0]]), {})) for k, v in REGIONS.items()}
    meta["regions"]["white"] = region("white", 2)
    meta["glyphs"] = {"px": GLYPH_PX, "cell": GLYPH_CELL, "chars": {ch: region("glyph:" + ch) + [round(adv, 3), glyphs[ch][0].width]
                                                                    for ch, (im, adv) in glyphs.items()}}
    (OUT / "models").mkdir(parents=True, exist_ok=True)
    total = 0
    counts = {}
    for name, levels in models.items():
        data, n = model_bytes(levels, at, (W, H), images, "white")
        counts[name] = n
        (OUT / "models" / f"{name}.bin").write_bytes(data)
        total += len(data)
    meta["models"] = counts
    (OUT / "supports.json").write_text(json.dumps(meta, indent=1))
    print(f"line models: {len(models)} models, {total / 1e6:.1f} MB, texture {W}x{H}", file=sys.stderr)


if __name__ == "__main__":
    main()
