"""Power line supports, redesigned (round 2, claude/jugcraft-power-lines-redesign.md): each support drawn as board
operations (tools/control_boards.py's format) at the plant's detail standard, in the materials of its own sheet
(textures/entity/line_supports.png, 128 px cells, named "ls:<name>"), plus the yard's legend atlas for tags and plates.
client/LineRenderer.java replays them: the body as it stands, the head turned to the line. What is previewed is what
the game draws.

Frame: the support's anchor at x = z = 0 on the ground (y 0), the line along z, crossarms along x."""
import math

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M
import transformer_yard as TY
from control_boards import Board, sw
from transformer_yard import ATLAS, cyl, rod, bar, path, ring, tag, face_quad, flag_img, flange, _rnd, _v

LS_CELL = 128
LS_MATERIALS = ["pole_wood", "crossarm", "galv", "galv_dark", "porcelain_grey", "porcelain_brown", "polymer", "aluminium",
                "copper_wire", "ground_molding", "concrete_spun", "pole_tag_blank", "bolt_head", "creosote_butt",
                "composite_white", "cryo_jacket", "sc_glow", "aviation_red", "enclosure_grey", "meter_glass", "transformer_grey",
                "lcd", "padmount_green", "rubber_black", "gravel", "stainless", "guard_yellow",
                "jar_clear", "electrolyte", "plate_pos", "plate_neg", "lead"]
# Drawn in the translucent pass (clear battery jars and the electrolyte in them); the rest are opaque.
LS_TRANSLUCENT = {"jar_clear", "electrolyte"}


def ls(name):
    assert name in LS_MATERIALS, name
    return "ls:" + name


# ---------------------------------------------------------------- the sheet

def _wood(seed, base=(86, 66, 48), weathered=(118, 104, 88), grain="v", checks=7, knots=2):
    """Creosote-treated pole wood weathered silver-brown: mottled, streaked along the grain, long dark checks
    (drying cracks), a knot or two; tiles on itself."""
    s = LS_CELL
    n = M.fbm(s, seed, 4, 48)
    a = np.zeros((s, s, 3), np.float32)
    for i in range(3):
        a[..., i] = base[i] + (weathered[i] - base[i]) * np.clip(0.5 + n * 1.6, 0, 1)
    r = np.random.default_rng(seed)
    streak = r.normal(0, 7, s)
    streak = np.convolve(np.tile(streak, 3), np.ones(3) / 3, "same")[s:2 * s]
    a += (streak[None, :, None] if grain == "v" else streak[:, None, None])
    a += r.normal(0, 3, (s, s, 1))
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(img)
    for _ in range(checks):
        if grain == "v":
            x, y0 = r.integers(4, s - 4), r.integers(0, s)
            L = r.integers(24, 90)
            pts = [((x + (r.random() < 0.15) * r.integers(-1, 2)) % s, (y0 + t) % s) for t in range(0, L, 4)]
        else:
            y, x0 = r.integers(4, s - 4), r.integers(0, s)
            L = r.integers(24, 90)
            pts = [((x0 + t) % s, (y + (r.random() < 0.15) * r.integers(-1, 2)) % s) for t in range(0, L, 4)]
        for p, q in zip(pts, pts[1:]):
            if abs(p[0] - q[0]) < 8 and abs(p[1] - q[1]) < 8:
                d.line([p, q], fill=(52, 40, 30, 255), width=1)
    for _ in range(knots):
        cx, cy = r.integers(10, s - 10), r.integers(10, s - 10)
        e = [cx - 2.5, cy - 4.5, cx + 2.5, cy + 4.5] if grain == "v" else [cx - 4.5, cy - 2.5, cx + 4.5, cy + 2.5]
        d.ellipse([e[0] - 2, e[1] - 2, e[2] + 2, e[3] + 2], fill=tuple(int(v * 0.85) for v in base) + (255,))
        d.ellipse(e, fill=(58, 44, 32, 255))
    return img


def _flat(colour, seed, amount=6, scale=24):
    s = LS_CELL
    n = M.fbm(s, seed, 3, scale)
    a = np.array(colour, np.float32)[None, None, :] + n[..., None] * amount * 2
    a += np.random.default_rng(seed).normal(0, 2, (s, s, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")


def _alpha(img, a):
    """A translucent cell: the colour kept, every pixel's alpha set to a (0-255)."""
    img = img.convert("RGBA")
    img.putalpha(a)
    return img


def _galv(seed, base=(168, 174, 180), dark=False):
    """Hot-dip galvanized steel: dull grey with spangle (crystal patches) and a little white rust."""
    s = LS_CELL
    img = _flat(base, seed, 5, 32)
    d = ImageDraw.Draw(img)
    r = np.random.default_rng(seed)
    for _ in range(70):
        x, y = r.integers(0, s), r.integers(0, s)
        w = r.integers(5, 16)
        c = int(r.integers(-14, 14))
        col = tuple(int(np.clip(v + c, 0, 255)) for v in base) + (255,)
        d.polygon([(x, y), (x + w, y + r.integers(-4, 5)), (x + w * 0.6, y + w), (x - 3, y + w * 0.7)], fill=col)
    if dark:
        a = np.asarray(img).astype(np.float32)
        a[..., :3] *= 0.62
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    return img


def _spun_concrete(seed):
    """Spun prestressed concrete: smooth, pale, fine aggregate speckle, faint streaks down the length; no form seams."""
    s = LS_CELL
    img = _flat((178, 178, 172), seed, 5, 32)
    a = np.asarray(img).astype(np.float32)
    r = np.random.default_rng(seed)
    streak = np.convolve(np.tile(r.normal(0, 3, s), 3), np.ones(5) / 5, "same")[s:2 * s]
    a[..., :3] += streak[None, :, None]
    for _ in range(260):
        x, y = r.integers(0, s), r.integers(0, s)
        a[y, x, :3] += r.choice([-26, -18, 14])
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def _gravel(seed):
    """Crushed-stone yard surfacing: grey stones of mixed size and shade, dark gaps between."""
    s = LS_CELL
    img = Image.new("RGBA", (s, s), (70, 70, 68, 255))
    d = ImageDraw.Draw(img)
    r = np.random.default_rng(seed)
    for _ in range(900):
        x, y = r.integers(0, s), r.integers(0, s)
        w = r.integers(2, 6)
        g = int(r.integers(110, 190))
        col = (g, g - int(r.integers(0, 6)), g - int(r.integers(0, 10)), 255)
        for dx in (0, -s, s):
            for dy in (0, -s, s):
                d.ellipse([x + dx, y + dy, x + dx + w, y + dy + w * 0.8], fill=col)
    return img


def draw_sheet():
    makers = {
        "pole_wood": lambda: _wood(301),
        "crossarm": lambda: _wood(302, base=(120, 100, 76), weathered=(150, 140, 126), grain="h", checks=5, knots=1),
        "galv": lambda: _galv(303), "galv_dark": lambda: _galv(304, (150, 156, 162), dark=True),
        "porcelain_grey": lambda: M.glaze((170, 174, 172), 305), "porcelain_brown": lambda: M.glaze((120, 66, 40), 306),
        "polymer": lambda: _flat((96, 100, 104), 307, 4, 16), "aluminium": lambda: M.brushed((196, 200, 204), 308, "v"),
        "copper_wire": lambda: _flat((176, 104, 60), 309, 8, 8), "ground_molding": lambda: _flat((40, 42, 44), 310, 3, 32),
        "concrete_spun": lambda: _spun_concrete(311), "pole_tag_blank": lambda: M.brushed((200, 204, 206), 312, "h"),
        "bolt_head": lambda: _galv(313, (140, 146, 150)), "creosote_butt": lambda: _wood(314, base=(42, 32, 24), weathered=(66, 50, 36)),
        "composite_white": lambda: _flat((224, 226, 224), 315, 2, 48), "cryo_jacket": lambda: M.brushed((64, 68, 74), 316, "h"),
        "sc_glow": lambda: _flat((96, 222, 255), 317, 5, 16), "aviation_red": lambda: _flat((236, 40, 34), 318, 4, 16),
        "enclosure_grey": lambda: _flat((142, 150, 144), 319, 3, 32), "meter_glass": lambda: _flat((196, 210, 214), 320, 3, 24),
        "transformer_grey": lambda: _flat((168, 174, 174), 321, 3, 32), "lcd": lambda: _flat((150, 196, 150), 322, 3, 16),
        "padmount_green": lambda: _flat((80, 96, 78), 323, 3, 32), "rubber_black": lambda: _flat((36, 36, 38), 324, 2, 16),
        "gravel": lambda: _gravel(325), "stainless": lambda: M.brushed((206, 210, 214), 326, "v"),
        "guard_yellow": lambda: _flat((226, 190, 40), 327, 4, 16),
        "jar_clear": lambda: _alpha(_flat((222, 232, 234), 328, 2, 32), 30),
        "electrolyte": lambda: _alpha(_flat((188, 212, 204), 329, 2, 32), 26),
        "plate_pos": lambda: _flat((88, 50, 34), 330, 6, 6), "plate_neg": lambda: _flat((84, 88, 94), 331, 6, 6),
        "lead": lambda: _flat((128, 132, 138), 332, 4, 12),
    }
    rows = (len(LS_MATERIALS) + 7) // 8
    sheet = Image.new("RGBA", (LS_CELL * 8, LS_CELL * rows), (0, 0, 0, 0))
    for i, name in enumerate(LS_MATERIALS):
        sheet.paste(makers[name]().convert("RGBA").resize((LS_CELL, LS_CELL)), ((i % 8) * LS_CELL, (i // 8) * LS_CELL))
    return sheet


def region(name, sheet_size):
    i = LS_MATERIALS.index(name)
    c, r = i % 8, i // 8
    W, H = sheet_size
    return [(c * LS_CELL + .5) / W, (r * LS_CELL + .5) / H, (c * LS_CELL + LS_CELL - .5) / W, (r * LS_CELL + LS_CELL - .5) / H]


# ---------------------------------------------------------------- shared parts

def tapered_pole(b, y0, y1, r0, r1, mat, segs=6, sides=16):
    """A round pole tapering from r0 at y0 to r1 at y1, in short cylinders (each a hair narrower than the last)."""
    for i in range(segs):
        ya, yb = y0 + (y1 - y0) * i / segs, y0 + (y1 - y0) * (i + 1) / segs
        r = r0 + (r1 - r0) * (i + 0.5) / segs
        cyl(b, (0, ya, 0), (0, yb, 0), r, mat, sides=sides)


def pole_radius(y, r0=0.155, r1=0.11, top=12.0):
    return r0 + (r1 - r0) * min(1.0, max(0.0, y / top))


def through_bolt(b, c, axis, length, r=0.012):
    """A through bolt with a square washer and nut at each end."""
    c, a = np.array(c, float), _v(axis)
    rod(b, c - a * length / 2, c + a * length / 2, r, ls("galv"), 6)
    for s in (-1, 1):
        e = c + a * s * length / 2
        cyl(b, e - a * s * 0.012, e, 0.035, ls("galv"), sides=4)
        cyl(b, e, e + a * s * 0.02, 0.02, ls("bolt_head"), sides=6)


def pin_insulator(b, base, h=0.2, mat="porcelain_grey"):
    """A pin-type insulator on its steel pin: three skirts, the top groove where the conductor is tied."""
    base = np.array(base, float)
    rod(b, base, base + [0, 0.1, 0], 0.016, ls("galv"), 6)
    y = base[1] + 0.08
    cyl(b, (base[0], y, base[2]), (base[0], y + h, base[2]), 0.04, ls(mat), sides=12)
    for k, (yy, rr) in enumerate(((0.02, 0.105), (0.08, 0.09), (0.13, 0.075))):
        cyl(b, (base[0], y + yy, base[2]), (base[0], y + yy + 0.025, base[2]), rr, ls(mat), sides=14)
    cyl(b, (base[0], y + h, base[2]), (base[0], y + h + 0.03, base[2]), 0.055, ls(mat), sides=12)
    return np.array([base[0], y + h - 0.02, base[2]])


def tie_and_conductor(b, top, stub=0.5, r=0.009, mat="aluminium"):
    """The conductor laid in the insulator's top groove, its tie wire wrapped round, stubs either way (the game's
    span joins on)."""
    top = np.array(top, float)
    rod(b, top + [0, 0, -stub], top + [0, 0, stub], r, ls(mat), 6)
    for dz in (-0.05, 0.0, 0.05):
        ring(b, top + [0, 0, dz], "z", r + 0.01, 0.008, ls("aluminium"), 8)


# ---------------------------------------------------------------- tier I: the wooden pole

WOOD_POLE = dict(top=12.0, arm_y=11.35, arm_half=1.22, pins=(-1.07, 1.07), r0=0.155, r1=0.11)


def wooden_pole(number="P-1427", transformer=None, riser=None):
    """Wood family tier I, a 12.47 kV three-phase distribution tangent pole (real basis: a Class 3, 40 ft southern pine
    pole, creosote-treated, set 6 ft deep, so 12 m stands above ground): an 8 ft crossarm through-bolted into a gain,
    two flat steel V-braces, porcelain pin insulators for the outer phases and a pole-top pin for the centre one, the
    conductors tied in; a bare copper ground wire down the pole in its plastic molding to a ground rod; the aluminium
    pole number tag and the treater's brand at eye height."""
    P = WOOD_POLE
    b = Board(3, 2101, face=0.0)
    b.live = []
    top = P["top"]
    # The butt's darker ground line (creosote bleeds), then the pole to its weathered top, a cap.
    tapered_pole(b, -0.4, 0.5, P["r0"] + 0.003, pole_radius(0.5), ls("creosote_butt"), segs=1)
    tapered_pole(b, 0.5, top, pole_radius(0.5), P["r1"], ls("pole_wood"), segs=24)
    cyl(b, (0, top, 0), (0, top + 0.03, 0), P["r1"] + 0.01, ls("galv_dark"), sides=16)
    # Crossarm in its gain (front, -z side), through bolt, two V braces down to a lag bolt below.
    ay = P["arm_y"]
    rp = pole_radius(ay)
    z0 = -rp - 0.115
    b.box(-P["arm_half"], ay - 0.045, z0, P["arm_half"], ay + 0.045, -rp + 0.005, ls("crossarm"))
    through_bolt(b, (0, ay, -rp / 2 - 0.06), "z", 2 * rp + 0.2)
    for s in (-1, 1):
        a0 = np.array([s * 0.72, ay - 0.045, (z0 - rp) / 2 + 0.0])
        a1 = np.array([0, ay - 0.62, -pole_radius(ay - 0.62) - 0.008])
        bar(b, a0, a1, 0.04, ls("galv"), h=0.006)
        cyl(b, a0 + [0, -0.005, 0], a0 + [0, 0.1, 0], 0.012, ls("galv"), sides=6)
    cyl(b, (0, ay - 0.62, -pole_radius(ay - 0.62) - 0.015), (0, ay - 0.62, -pole_radius(ay - 0.62) + 0.04), 0.012, ls("galv"), sides=6)
    cyl(b, (0, ay - 0.62, -pole_radius(ay - 0.62) - 0.035), (0, ay - 0.62, -pole_radius(ay - 0.62) - 0.015), 0.022, ls("bolt_head"), sides=6)
    # Pin insulators on the arm, the pole-top pin's bracket and insulator.
    tops = []
    zc = (z0 - rp) / 2 + 0.0
    zc = z0 + 0.0575
    for x in P["pins"]:
        tops.append(pin_insulator(b, (x, ay + 0.045, zc)))
    rt = P["r1"]
    b.box(-0.025, top - 0.55, -rt - 0.02, 0.025, top + 0.05, -rt, ls("galv"))                             # pole-top pin bracket
    through_bolt(b, (0, top - 0.42, 0), "z", 2 * rt + 0.08)
    through_bolt(b, (0, top - 0.12, 0), "z", 2 * rt + 0.08)
    b.box(-0.03, top + 0.03, -rt - 0.03, 0.03, top + 0.08, rt * 0.2, ls("galv"))
    tops.insert(1, pin_insulator(b, (0, top + 0.08, -rt * 0.5)))
    for t in tops:
        tie_and_conductor(b, t)
    b.attach = [list(_rnd(t)) for t in tops]
    # Ground wire: from the pole-top pin bracket down the back in its molding to the ground.
    gz = rt
    path(b, [(0, top - 0.3, -rt - 0.02), (0.06, top - 0.3, -rt - 0.02), (rt * 0.6, top - 0.3, rt * 0.8)], 0.004, ls("copper_wire"))
    for i in range(10):
        ya, yb = 0.0 + i * 1.12, 0.0 + (i + 1) * 1.12
        ra, rb_ = pole_radius(ya), pole_radius(yb)
        b.obox(tuple(_rnd([0, (ya + yb) / 2, (ra + rb_) / 2 + 0.008])), (0, 1, 0), (1, 0, 0), round((yb - ya) / 2, 4), 0.016, 0.009,
               ls("ground_molding"))
    for y in np.arange(0.5, 11.0, 1.2):
        cyl(b, (0, y, pole_radius(y) + 0.015), (0, y, pole_radius(y) + 0.03), 0.01, ls("bolt_head"), sides=6)      # staples
    # The number tag and the treater's brand (both small aluminium on the pole's front at eye height).
    if number:
        key = ATLAS.add(f"tag:{number}:pole", TY.tag_img(number, 0.16, 0.06))
        face_quad(b, (0, 1.75, -pole_radius(1.75) - 0.004), "-z", 0.16, 0.06, ATLAS.mat(key))
        for dx in (-0.065, 0.065):
            cyl(b, (dx, 1.75, -pole_radius(1.75) - 0.002), (dx, 1.75, -pole_radius(1.75) - 0.008), 0.006, ls("bolt_head"), sides=6)
        key2 = ATLAS.add("tag:brand", TY.tag_img("SYP  CR  40-3  2026", 0.2, 0.045))
        face_quad(b, (0, 3.0, -pole_radius(3.0) - 0.004), "-z", 0.2, 0.045, ATLAS.mat(key2))
    if transformer:
        b.drop = [list(_rnd(pole_transformer(b, transformer)))]
    if riser:
        b.riser = [list(_rnd(p)) for p in pole_riser(b, riser, tops)]
    return b


# ---------------------------------------------------------------- tier II: the double-arm pole

DOUBLE_POLE = dict(top=13.0, arm_y=12.3, arm_half=1.52, pins=(-1.37, 1.37), r0=0.17, r1=0.115)


def pin_insulator_25(b, base, mat="porcelain_grey"):
    """A 25 kV pin insulator: taller, four skirts."""
    base = np.array(base, float)
    rod(b, base, base + [0, 0.12, 0], 0.018, ls("galv"), 6)
    y = base[1] + 0.1
    h = 0.27
    cyl(b, (base[0], y, base[2]), (base[0], y + h, base[2]), 0.045, ls(mat), sides=12)
    for yy, rr in ((0.02, 0.13), (0.085, 0.115), (0.145, 0.1), (0.2, 0.085)):
        cyl(b, (base[0], y + yy, base[2]), (base[0], y + yy + 0.028, base[2]), rr, ls(mat), sides=16)
    cyl(b, (base[0], y + h, base[2]), (base[0], y + h + 0.035, base[2]), 0.062, ls(mat), sides=12)
    return np.array([base[0], y + h - 0.02, base[2]])


def double_arm_pole(number="P-2210"):
    """Wood family tier II, a 24.9 kV three-phase line on double crossarms (real basis: Class 2, 45 ft pole, 13 m up;
    two 10 ft arms, one on each face of the pole, through-bolted together with double-arm bolts and pipe spacers, each
    outer phase tied to a pin insulator on both arms for strength over a bigger conductor; the centre phase on a 25 kV
    pole-top pin). Ground wire in its molding, number tag and brand as on tier I."""
    P = DOUBLE_POLE
    b = Board(4, 2102, face=0.0)
    b.live = []
    top = P["top"]
    rad = lambda y: P["r0"] + (P["r1"] - P["r0"]) * min(1.0, max(0.0, y / top))
    tapered_pole(b, -0.4, 0.5, P["r0"] + 0.003, rad(0.5), ls("creosote_butt"), segs=1)
    tapered_pole(b, 0.5, top, rad(0.5), P["r1"], ls("pole_wood"), segs=26)
    cyl(b, (0, top, 0), (0, top + 0.03, 0), P["r1"] + 0.01, ls("galv_dark"), sides=16)
    ay = P["arm_y"]
    rp = rad(ay)
    zf0, zf1 = -rp - 0.115, -rp + 0.005                    # the front arm
    zb0, zb1 = rp - 0.005, rp + 0.115                      # the back arm
    for z0, z1 in ((zf0, zf1), (zb0, zb1)):
        b.box(-P["arm_half"], ay - 0.045, z0, P["arm_half"], ay + 0.045, z1, ls("crossarm"))
    # Double-arm bolts through both arms (and through the pole at the middle), pipe spacers between the arms.
    for x in (0.0, -0.75, 0.75, -1.15, 1.15):
        through_bolt(b, (x, ay, 0), "z", zb1 - zf0 + 0.08)
        if x:
            cyl(b, (x, ay, zf1), (x, ay, zb0), 0.024, ls("galv"), sides=8)
    # V braces on each arm down to a bolt through the pole.
    yb = ay - 0.7
    for zc, s_z in ((zf0, -1), (zb1, 1)):
        for s in (-1, 1):
            a0 = np.array([s * 0.78, ay - 0.045, zc + (0.0575 if s_z < 0 else -0.0575)])
            a1 = np.array([0, yb, s_z * (rad(yb) + 0.008)])
            bar(b, a0, a1, 0.04, ls("galv"), h=0.006)
    through_bolt(b, (0, yb, 0), "z", 2 * rad(yb) + 0.1)
    # Pin insulators on both arms for each outer phase, the conductor tied to both; the pole-top pin.
    tops = []
    for x in P["pins"]:
        t0 = pin_insulator_25(b, (x, ay + 0.045, (zf0 + zf1) / 2))
        t1 = pin_insulator_25(b, (x, ay + 0.045, (zb0 + zb1) / 2))
        mid = (t0 + t1) / 2
        rod(b, t0 + [0, 0, -0.5], t1 + [0, 0, 0.5], 0.011, ls("aluminium"), 6)
        for t in (t0, t1):
            for dz in (-0.05, 0.0, 0.05):
                ring(b, t + [0, 0, dz], "z", 0.021, 0.008, ls("aluminium"), 8)
        tops.append(mid)
    rt = P["r1"]
    b.box(-0.028, top - 0.6, -rt - 0.022, 0.028, top + 0.05, -rt, ls("galv"))
    for y in (top - 0.45, top - 0.13):
        through_bolt(b, (0, y, 0), "z", 2 * rt + 0.08)
    b.box(-0.032, top + 0.03, -rt - 0.032, 0.032, top + 0.09, rt * 0.2, ls("galv"))
    tc = pin_insulator_25(b, (0, top + 0.09, -rt * 0.5))
    tie_and_conductor(b, tc, r=0.011)
    tops.insert(1, tc)
    b.attach = [list(_rnd(t)) for t in tops]
    # Ground wire in its molding, staples; number tag and brand.
    path(b, [(0, top - 0.3, -rt - 0.022), (0.06, top - 0.3, -rt - 0.022), (rt * 0.6, top - 0.3, rt * 0.8)], 0.004, ls("copper_wire"))
    n = 11
    for i in range(n):
        ya, yb2 = i * (top - 1.2) / n, (i + 1) * (top - 1.2) / n
        ra, rb_ = rad(ya), rad(yb2)
        b.obox(tuple(_rnd([0, (ya + yb2) / 2, (ra + rb_) / 2 + 0.008])), (0, 1, 0), (1, 0, 0), round((yb2 - ya) / 2 - 0.002, 4), 0.016, 0.009,
               ls("ground_molding"))
    for y in np.arange(0.5, top - 1.2, 1.2):
        cyl(b, (0, y, rad(y) + 0.015), (0, y, rad(y) + 0.03), 0.01, ls("bolt_head"), sides=6)
    key = ATLAS.add(f"tag:{number}:pole", TY.tag_img(number, 0.16, 0.06))
    face_quad(b, (0, 1.75, -rad(1.75) - 0.004), "-z", 0.16, 0.06, ATLAS.mat(key))
    for dx in (-0.065, 0.065):
        cyl(b, (dx, 1.75, -rad(1.75) - 0.002), (dx, 1.75, -rad(1.75) - 0.008), 0.006, ls("bolt_head"), sides=6)
    key2 = ATLAS.add("tag:brand2", TY.tag_img("SYP  CR  45-2  2026", 0.2, 0.045))
    face_quad(b, (0, 3.0, -rad(3.0) - 0.004), "-z", 0.2, 0.045, ATLAS.mat(key2))
    return b


# ---------------------------------------------------------------- tier III: the spun-concrete pole

CONCRETE_POLE = dict(top=14.0, r0=0.2, r1=0.13, posts=((-1, 13.4), (1, 12.2), (-1, 11.0)), post_len=1.05)


def polymer_post(b, base, side, length, brace_to=None):
    """A horizontal polymer line post (fibreglass core, silicone sheds) out from a bracket at base toward side
    (+-x), its trunnion clamp at the tip; with brace_to, a polymer brace from the tip up to that point on the pole."""
    base = np.array(base, float)
    a = np.array([side, 0, 0.0])
    tip = base + a * length
    b.box(*_rnd(np.minimum(base - [0.04, 0.12, 0.08], base + [0.04, 0.12, 0.08])), *_rnd(np.maximum(base - [0.04, 0.12, 0.08], base + [0.04, 0.12, 0.08])),
          ls("galv"))
    cyl(b, base + a * 0.04, base + a * 0.12, 0.04, ls("galv"), sides=10)
    cyl(b, base + a * 0.12, tip - a * 0.1, 0.024, ls("polymer"), sides=10)
    n = int((length - 0.3) / 0.075)
    for i in range(n):
        c = base + a * (0.17 + i * 0.075)
        cyl(b, c, c + a * 0.012, 0.075 if i % 2 == 0 else 0.058, ls("polymer"), sides=14)
    cyl(b, tip - a * 0.1, tip, 0.035, ls("galv"), sides=10)
    clamp = tip + [0, 0.07, 0]
    b.box(*_rnd(clamp - [0.04, 0.05, 0.07]), *_rnd(clamp + [0.04, 0.03, 0.07]), ls("aluminium"))
    if brace_to is not None:
        bt = np.array(brace_to, float)
        d = (bt - tip) / np.linalg.norm(bt - tip)
        L = np.linalg.norm(bt - tip)
        cyl(b, tip + d * 0.05, bt - d * 0.08, 0.016, ls("polymer"), sides=8)
        for i in range(int((L - 0.3) / 0.09)):
            c = tip + d * (0.15 + i * 0.09)
            cyl(b, c, c + d * 0.01, 0.045, ls("polymer"), sides=12)
        b.box(*_rnd(bt - [0.035, 0.08, 0.06]), *_rnd(bt + [0.035, 0.08, 0.06]), ls("galv"))
    top = clamp + [0, 0.035, 0]
    rod(b, top + [0, 0, -0.5], top + [0, 0, 0.5], 0.011, ls("aluminium"), 6)
    return top


def spun_concrete_pole(number="C-3105"):
    """Wood family tier III, a 34.5 kV armless line on a spun prestressed-concrete pole (real basis: a centrifugally
    cast, tapered, hollow pole 14 m above ground; three braced polymer horizontal line posts staggered on alternate
    sides, each bolted through the pole's cast holes, a polymer brace from the tip up to the pole; the ground wire runs
    inside the hollow pole to a ground terminal at the base; a cast-in identification plate and number tag)."""
    P = CONCRETE_POLE
    b = Board(4, 2103, face=0.0)
    b.live = []
    top = P["top"]
    rad = lambda y: P["r0"] + (P["r1"] - P["r0"]) * min(1.0, max(0.0, y / top))
    tapered_pole(b, -0.4, top, rad(-0.4), P["r1"], ls("concrete_spun"), segs=28, sides=20)
    cyl(b, (0, top, 0), (0, top + 0.04, 0), P["r1"] + 0.005, ls("galv"), sides=20)                   # cap
    # Cast through-holes (the unused ones show as dark plugs up the pole every 0.3 m on two faces).
    for y in np.arange(9.8, top - 0.2, 0.3):
        for x_s in (-1, 1):
            cyl(b, (x_s * (rad(y) - 0.004), y, 0), (x_s * (rad(y) + 0.002), y, 0), 0.016, ls("ground_molding"), sides=8)
    tops = []
    for side, y in P["posts"]:
        r = rad(y)
        base = np.array([side * (r + 0.04), y, 0])
        through_bolt(b, (0, y + 0.06, 0), "x", 2 * r + 0.14)
        through_bolt(b, (0, y - 0.06, 0), "x", 2 * r + 0.14)
        tops.append(polymer_post(b, base, side, P["post_len"], brace_to=(side * (rad(y + 0.75) + 0.04), y + 0.75, 0)))
        through_bolt(b, (0, y + 0.75, 0), "x", 2 * rad(y + 0.75) + 0.12)
    tops.sort(key=lambda t: (t[0], -t[1]))
    b.attach = [list(_rnd(t)) for t in tops]
    # Ground terminal at the base, the lead into the grid; the cast plate and the number tag.
    b.box(-0.06, 0.45, -rad(0.5) - 0.03, 0.06, 0.6, -rad(0.5) + 0.01, ls("galv"))
    path(b, [(0, 0.45, -rad(0.5) - 0.02), (0, 0.02, -rad(0.5) - 0.02), (0, 0.02, -rad(0.5) - 0.5)], 0.008, ls("copper_wire"))
    key = ATLAS.add(f"tag:{number}:pole", TY.tag_img(number, 0.16, 0.06))
    face_quad(b, (0, 1.75, -rad(1.75) - 0.004), "-z", 0.16, 0.06, ATLAS.mat(key))
    key2 = ATLAS.add("tag:castplate", TY.tag_img("SPUN PSC  14-H5  2026", 0.22, 0.05))
    face_quad(b, (0, 3.0, -rad(3.0) - 0.004), "-z", 0.22, 0.05, ATLAS.mat(key2))
    return b


# ---------------------------------------------------------------- monopole family

MONO = dict(top=30.0, r0=0.5, r1=0.17, joints=(10.5, 20.5), arms=((-1, 25.0), (1, 21.0), (-1, 17.0)), arm_len=3.2)


def mono_radius(y, M_=None):
    M_ = M_ or MONO
    return M_["r0"] + (M_["r1"] - M_["r0"]) * min(1.0, max(0.0, y / M_["top"]))


def monopole_body(b, M_, plate="TR-138  STR 41", steps=True):
    """A tapered twelve-sided galvanized steel pole: concrete pier, base plate and anchor bolts, the shaft in slip-jointed
    sections, the top cap, step bolts from the guard height up, the ground lug and the structure number plate."""
    top = M_["top"]
    rad = lambda y: mono_radius(y, M_)
    r0 = M_["r0"]
    cyl(b, (0, -0.6, 0), (0, 0.45, 0), r0 + 0.35, ls("concrete_spun"), sides=24)
    cyl(b, (0, 0.45, 0), (0, 0.52, 0), r0 + 0.22, ls("galv"), sides=12)
    for k in range(12):
        t = 2 * math.pi * (k + 0.5) / 12
        p = np.array([math.cos(t) * (r0 + 0.13), 0.52, math.sin(t) * (r0 + 0.13)])
        cyl(b, p, p + [0, 0.12, 0], 0.03, ls("bolt_head"), sides=6)                                 # anchor bolt and nut
        cyl(b, p + [0, 0.12, 0], p + [0, 0.17, 0], 0.016, ls("galv"), sides=6)
    segs = 30
    for i in range(segs):
        ya, yb = 0.52 + (top - 0.52) * i / segs, 0.52 + (top - 0.52) * (i + 1) / segs
        cyl(b, (0, ya, 0), (0, yb, 0), rad((ya + yb) / 2), ls("galv"), sides=12)
    for yj in M_["joints"]:
        cyl(b, (0, yj - 0.6, 0), (0, yj, 0), rad(yj) + 0.012, ls("galv"), sides=12)                   # slip joint overlap
    cyl(b, (0, top, 0), (0, top + 0.06, 0), rad(top) + 0.02, ls("galv"), sides=12)
    if steps:
        for k, y in enumerate(np.arange(3.0, top - 0.6, 0.38)):
            t = math.radians(90 + (40 if k % 2 else -40))
            n = np.array([math.cos(t), 0, math.sin(t)])
            p = np.array([0, y, 0]) + n * rad(y)
            rod(b, p - n * 0.01, p + n * 0.2, 0.011, ls("galv"), 6)
            cyl(b, p + n * 0.18, p + n * 0.2, 0.02, ls("galv"), sides=6)
    # Ground lug and lead at the base, the number plate at eye height on the front.
    b.box(-0.06, 0.75, -rad(0.8) - 0.04, 0.06, 0.88, -rad(0.8) + 0.02, ls("galv"))
    path(b, [(0, 0.78, -rad(0.8) - 0.03), (0, 0.5, -r0 - 0.4), (0, 0.02, -r0 - 0.6)], 0.009, ls("copper_wire"))
    key = ATLAS.add(f"tag:{plate}:mono", TY.tag_img(plate, 0.34, 0.08))
    yp = 2.2
    face_quad(b, (0, yp, -rad(yp) - 0.006), "-z", 0.34, 0.08, ATLAS.mat(key))
    for dx in (-0.15, 0.15):
        cyl(b, (dx, yp, -rad(yp) - 0.002), (dx, yp, -rad(yp) - 0.01), 0.008, ls("bolt_head"), sides=6)


def davit_arm(b, M_, side, y, length, rise=0.45):
    """A tapered tubular davit arm bolted to a bracket on the pole: from the pole out along side (+-x), sweeping up a
    little; the vang plate at the tip. Returns the tip's hanging point."""
    rad = lambda yy: mono_radius(yy, M_)
    r = rad(y)
    p0 = np.array([side * (r + 0.02), y, 0])
    b.box(*_rnd([min(p0[0], p0[0] - side * 0.04), y - 0.35, -0.16]), *_rnd([max(p0[0], p0[0] - side * 0.04), y + 0.35, 0.16]), ls("galv"))  # bracket
    for dy in (-0.25, 0.25):
        for dz in (-0.1, 0.1):
            cyl(b, p0 + [side * 0.0, dy, dz], p0 + [side * 0.04, dy, dz], 0.018, ls("bolt_head"), sides=6)
    flange(b, p0 + [side * 0.06, 0, 0], "x", 0.17, 0.04, ls("galv"), 8)
    tip = p0 + np.array([side * length, rise, 0])
    pts = [p0 + [side * 0.08, 0, 0], p0 + (tip - p0) * 0.5 + [0, 0.05, 0], tip]
    for (a, c), rr in zip(zip(pts, pts[1:]), (0.12, 0.085)):
        cyl(b, a, c, rr, ls("galv"), sides=12)
    cyl(b, tip, tip + [side * 0.04, 0, 0], 0.07, ls("galv"), sides=12)
    b.box(*_rnd(tip + [-0.015, -0.2, -0.05]), *_rnd(tip + [0.015, -0.05, 0.05]), ls("galv"))          # vang plate
    return tip + [0, -0.2, 0]


def polymer_suspension(b, top, length=1.55, sheds=(0.07, 0.055), ring_r=0.18):
    """A polymer suspension insulator hanging from top: clevis, rod with silicone sheds, grading ring at the line end,
    the suspension clamp with armour rods on the conductor. Returns the conductor's point."""
    top = np.array(top, float)
    bot = top - [0, length, 0]
    cyl(b, top, top - [0, 0.12, 0], 0.03, ls("galv"), sides=8)
    cyl(b, top - [0, 0.12, 0], bot + [0, 0.12, 0], 0.022, ls("polymer"), sides=10)
    for i in range(int((length - 0.35) / 0.065)):
        y = top[1] - 0.18 - i * 0.065
        cyl(b, (top[0], y, top[2]), (top[0], y - 0.012, top[2]), sheds[i % 2], ls("polymer"), sides=14)
    cyl(b, bot + [0, 0.12, 0], bot, 0.03, ls("galv"), sides=8)
    if ring_r:
        ring(b, bot + [0, 0.1, 0], "y", ring_r, 0.035, ls("aluminium"), 16)
    clamp = bot - [0, 0.1, 0]
    b.box(*_rnd(clamp + [-0.05, -0.06, -0.12]), *_rnd(clamp + [0.05, 0.06, 0.12]), ls("aluminium"))
    cyl(b, clamp + [0, -0.02, -0.55], clamp + [0, -0.02, 0.55], 0.02, ls("aluminium"), sides=8)        # armour rods
    return clamp + [0, -0.02, 0]


def shield_peak(b, M_):
    """The shield wire's bracket on the pole top and its suspension clamp."""
    top = M_["top"]
    b.box(-0.04, top - 0.5, -mono_radius(top, M_) - 0.06, 0.04, top + 0.55, -mono_radius(top, M_) + 0.02, ls("galv"))
    p = np.array([0, top + 0.5, -mono_radius(top, M_) - 0.02])
    b.box(*_rnd(p + [-0.05, 0.05, -0.1]), *_rnd(p + [0.05, 0.12, 0.1]), ls("aluminium"))
    rod(b, p + [0, 0.1, -0.5], p + [0, 0.1, 0.5], 0.006, ls("galv"), 6)
    return p + [0, 0.1, 0]


def steel_monopole(number="TR-138  STR 41", angle=False):
    """Monopole family tier I, a 138 kV single-circuit line on a tapered twelve-sided galvanized steel pole 30 m above
    ground (real basis: utility tangent monopoles): on a drilled concrete pier with anchor bolts; three tubular davit
    arms staggered on alternate sides, each phase on a polymer suspension insulator with a grading ring and an
    armour-rodded suspension clamp; the shield wire on the pole top; step bolts from 3 m up; ground lug; structure
    number plate."""
    M_ = dict(MONO, r0=MONO["r0"] * 1.3, r1=MONO["r1"] * 1.25) if angle else MONO
    b = Board(4, 2201 + 10 * angle, face=0.0)
    b.live = []
    monopole_body(b, M_, number)
    tops, hinges = [], []
    for side, y in M_["arms"]:
        hang = davit_arm(b, M_, side, y, M_["arm_len"])
        if angle:
            hinges.append(hang + [0, 0.08, 0])
        else:
            tops.append(polymer_suspension(b, hang))
    if angle:
        b.kit = dict(kind="strain", hinges=hinges, spec=dict(ins="poly", L=1.6, n=1, ring_r=0.18))
        b.shield = [list(_rnd(shield_peak(b, M_)))]
        return b
    b.attach = [list(_rnd(t)) for t in tops]
    b.shield = [list(_rnd(shield_peak(b, M_)))]
    return b


MONO2 = dict(top=32.0, r0=0.62, r1=0.2, joints=(11.0, 22.0), levels=(30.0, 24.5, 19.0), arm_len=3.6)


def double_circuit_monopole(number="TR-230  STR 18", angle=False):
    """Monopole family tier II, a 230 kV double-circuit line (real basis: utility double-circuit tangent monopoles,
    32 m up): a heavier twelve-sided pole, six davit arms in three levels, one circuit each side, each phase on a
    longer polymer suspension insulator (230 kV) with a grading ring; shield wire on the pole top."""
    M_ = dict(MONO2, r0=MONO2["r0"] * 1.3, r1=MONO2["r1"] * 1.25) if angle else MONO2
    b = Board(4, 2202 + 10 * angle, face=0.0)
    b.live = []
    monopole_body(b, M_, number)
    tops, hinges = [], []
    for y in M_["levels"]:
        for side in (-1, 1):
            hang = davit_arm(b, M_, side, y, M_["arm_len"], rise=0.5)
            if angle:
                hinges.append(hang + [0, 0.08, 0])
            else:
                tops.append(polymer_suspension(b, hang, length=2.15, sheds=(0.08, 0.062), ring_r=0.22))
    if angle:
        b.kit = dict(kind="strain", hinges=hinges, spec=dict(ins="poly", L=2.15, n=1, ring_r=0.22, sheds=(0.08, 0.062)))
        b.shield = [list(_rnd(shield_peak(b, M_)))]
        return b
    b.attach = [list(_rnd(t)) for t in tops]
    b.shield = [list(_rnd(shield_peak(b, M_)))]
    return b


MONO3 = dict(top=36.0, r0=0.72, r1=0.24, joints=(12.0, 24.0), posts=((-1, 32.0), (1, 28.0), (-1, 24.0)), post_len=3.3,
             brace_rise=2.9, bundle=0.46)


def _hinge_bracket(b, p, side):
    """A gain plate on the pole face with two clevis lugs out toward side (+-x) and the hinge pin through them (along z)."""
    p = np.array(p, float)
    x0, x1 = sorted((p[0] - side * 0.03, p[0] + side * 0.02))
    b.box(*_rnd([x0, p[1] - 0.22, -0.13]), *_rnd([x1, p[1] + 0.22, 0.13]), ls("galv"))
    for dy in (-0.15, 0.15):
        for dz in (-0.08, 0.08):
            cyl(b, (x1 if side > 0 else x0, p[1] + dy, dz), ((x1 if side > 0 else x0) + side * 0.03, p[1] + dy, dz), 0.017, ls("bolt_head"), sides=6)
    lx0, lx1 = sorted((p[0] + side * 0.02, p[0] + side * 0.14))
    for zc in (-0.055, 0.055):
        b.box(*_rnd([lx0, p[1] - 0.06, zc - 0.012]), *_rnd([lx1, p[1] + 0.06, zc + 0.012]), ls("galv"))
    pin = np.array([p[0] + side * 0.1, p[1], 0])
    cyl(b, pin - [0, 0, 0.09], pin + [0, 0, 0.09], 0.016, ls("galv_dark"), sides=8)
    return pin


def braced_post(b, M_, side, y, length, rise, bundle):
    """A 345 kV braced line post (real basis: the horizontal polymer post in compression, hinged to a bracket on the pole,
    held up by a polymer suspension brace in tension to a second bracket above; grading rings at the line ends; a yoke at
    the tip carrying the two subconductors of the twin bundle in armour-rodded clamps). Returns the two clamp points."""
    rad = lambda yy: mono_radius(yy, M_)
    a = np.array([side, 0, 0.0])
    pin = _hinge_bracket(b, (side * rad(y), y, 0), side)
    tip = pin + a * length
    # The post: hinge end fitting, the fibreglass rod in alternating silicone sheds, the line end fitting and ring.
    cyl(b, pin, pin + a * 0.22, 0.05, ls("galv"), sides=10)
    flange(b, pin + a * 0.22, "x", 0.075, 0.025, ls("galv"), 8)
    cyl(b, pin + a * 0.245, tip - a * 0.24, 0.042, ls("polymer"), sides=10)
    n = int((length - 0.62) / 0.085)
    for i in range(n):
        c = pin + a * (0.33 + i * 0.085)
        cyl(b, c, c + a * 0.014, 0.125 if i % 2 == 0 else 0.098, ls("polymer"), sides=16)
    flange(b, tip - a * 0.24, "x", 0.075, 0.025, ls("galv"), 8)
    cyl(b, tip - a * 0.215, tip, 0.05, ls("galv"), sides=10)
    ring(b, tip - a * 0.36, "x", 0.24, 0.035, ls("aluminium"), 18)
    # The tip casting: a vertical plate, the brace's clevis on top, the yoke below.
    b.box(*_rnd(tip + [-0.02 * side - 0.0, -0.32, -0.05] if side > 0 else tip + [0.0, -0.32, -0.05]),
          *_rnd(tip + [0.0, 0.12, 0.05] if side > 0 else tip + [0.02, 0.12, 0.05]), ls("galv"))
    yoke_y = tip[1] - 0.32
    b.box(*_rnd([tip[0] - bundle / 2 - 0.05, yoke_y - 0.07, -0.03]), *_rnd([tip[0] + bundle / 2 + 0.05, yoke_y, 0.03]), ls("galv"))
    clamps = []
    for dx in (-bundle / 2, bundle / 2):
        c = np.array([tip[0] + dx, yoke_y - 0.07, 0])
        cyl(b, c, c - [0, 0.14, 0], 0.016, ls("galv"), sides=8)
        cl = c - [0, 0.2, 0]
        b.box(*_rnd(cl + [-0.045, -0.06, -0.12]), *_rnd(cl + [0.045, 0.06, 0.12]), ls("aluminium"))
        cyl(b, cl + [0, -0.02, -0.55], cl + [0, -0.02, 0.55], 0.022, ls("aluminium"), sides=8)
        clamps.append(cl + [0, -0.02, 0])
    # The brace: from the tip up to the upper bracket, a polymer suspension insulator with its own sheds and ring.
    top = _hinge_bracket(b, (side * rad(y + rise), y + rise, 0), side)
    lo = tip + [0, 0.1, 0]
    d = (top - lo) / np.linalg.norm(top - lo)
    L = np.linalg.norm(top - lo)
    cyl(b, lo, lo + d * 0.2, 0.032, ls("galv"), sides=8)
    cyl(b, lo + d * 0.2, top - d * 0.18, 0.024, ls("polymer"), sides=10)
    for i in range(int((L - 0.55) / 0.075)):
        c = lo + d * (0.28 + i * 0.075)
        cyl(b, c, c + d * 0.012, 0.085 if i % 2 == 0 else 0.066, ls("polymer"), sides=14)
    cyl(b, top - d * 0.18, top - d * 0.02, 0.032, ls("galv"), sides=8)
    return clamps


def compact_monopole(number="TR-345  STR 7", angle=False):
    """Monopole family tier III, a compact 345 kV single-circuit line (real basis: utility compact 345 kV lines on
    braced line posts, which narrow the right of way because the conductors cannot swing out): a heavy twelve-sided pole
    36 m up; three braced polymer line posts staggered on alternate sides 4 m apart; each phase a twin bundle on the
    post's tip yoke; the shield wire on the pole top."""
    M_ = dict(MONO3, r0=MONO3["r0"] * 1.25, r1=MONO3["r1"] * 1.25) if angle else MONO3
    b = Board(4, 2203 + 10 * angle, face=0.0)
    b.live = []
    monopole_body(b, M_, number)
    if angle:
        # Braced posts cannot take a dead-end's pull: the angle pole carries davit arms long enough for 345 kV.
        hinges = [davit_arm(b, M_, side, y, 5.2, rise=0.5) + [0, 0.08, 0] for side, y in M_["posts"]]
        b.kit = dict(kind="strain", hinges=hinges, spec=dict(ins="poly", L=3.0, n=2, rb=0.23, ring_r=0.26, sheds=(0.09, 0.07)))
        b.shield = [list(_rnd(shield_peak(b, M_)))]
        return b
    tops = []
    for side, y in M_["posts"]:
        tops += braced_post(b, M_, side, y, M_["post_len"], M_["brace_rise"], M_["bundle"])
    b.attach = [list(_rnd(t)) for t in tops]
    b.shield = [list(_rnd(shield_peak(b, M_)))]
    return b


# ---------------------------------------------------------------- lattice family

def section_truss(b, P, Q, ts, leg_w=0.16, brace_w=0.08, angle_legs=True, struts=True, seed=0):
    """A four-chord lattice from the section P (four corners in order round) to the section Q: the chords (galvanized
    angles when the run is mostly upright, square bars otherwise), X bracing on every face between the panel points ts
    (fractions 0..1; the two diagonals a hair apart so they never share a plane) and struts on the inner panel lines."""
    P, Q = [np.array(p, float) for p in P], [np.array(q, float) for q in Q]
    c = (sum(P) + sum(Q)) / 8
    cp = sum(P) / 4
    for i in range(4):
        if angle_legs:
            TY.angle_member(b, P[i], Q[i], 1 if P[i][0] > cp[0] else -1, 1 if P[i][2] > cp[2] else -1, leg_w)
        else:
            bar(b, P[i], Q[i], leg_w, TY.GALV)
    for f in range(4):
        A0, A1, B0, B1 = P[f], Q[f], P[(f + 1) % 4], Q[(f + 1) % 4]
        n = np.cross(B0 - A0, A1 - A0)
        n /= np.linalg.norm(n)
        if n @ ((A0 + B1) / 2 - c) < 0:
            n = -n
        o = 0.012 * ((f + seed) % 2) + (0 if angle_legs else 0.02)
        for k, (t0, t1) in enumerate(zip(ts, ts[1:])):
            a0, a1 = A0 + (A1 - A0) * t0, A0 + (A1 - A0) * t1
            b0, b1 = B0 + (B1 - B0) * t0, B0 + (B1 - B0) * t1
            bar(b, *shrink_(a0 + n * (0.02 + o), b1 + n * (0.02 + o), 0.06), brace_w, TY.GALV)
            bar(b, *shrink_(b0 - n * (0.03 + o), a1 - n * (0.03 + o), 0.06), brace_w, TY.GALV)
            if struts and 0 < k:
                bar(b, *shrink_(a0 - n * (0.065 + o), b0 - n * (0.065 + o), 0.2), brace_w * 1.2, TY.GALV, h=brace_w * 0.85)


def shrink_(a, c, d):
    return TY.shrink(a, c, d)


def perimeter(b, S, w=0.1, inset=0.2, dy=0.0, faces=(0, 1, 2, 3)):
    """Struts round a section's edges (pulled in at the corners)."""
    S = [np.array(s, float) for s in S]
    for i in faces:
        bar(b, *shrink_(S[i] + [0, dy, 0], S[(i + 1) % 4] + [0, dy, 0], inset), w, TY.GALV, h=w * 0.8)


def triple_yoke(b, c, a=0.46):
    """The yoke under a string and the three clamps of a triple bundle, the triangle apex down: a short beam with two
    clamps hanging from its ends and a strap from its middle down to the third. Returns the three sub-conductor points."""
    c = np.array(c, float)
    b.box(*_rnd(c + [-a / 2 - 0.07, -0.08, -0.03]), *_rnd(c + [a / 2 + 0.07, 0.0, 0.03]), ls("galv"))
    subs = []
    for dx, drop in ((-a / 2, 0.1), (a / 2, 0.1), (0.0, 0.5)):
        p = c + [dx, -0.08, 0]
        b.box(*_rnd(p + [-0.02, -drop, -0.015]), *_rnd(p + [0.02, 0, 0.015]), ls("galv"))
        cl = p - [0, drop + 0.06, 0]
        b.box(*_rnd(cl + [-0.045, -0.05, -0.1]), *_rnd(cl + [0.045, 0.05, 0.1]), TY.ALU)
        rod(b, cl + [0, -0.02, -0.55], cl + [0, -0.02, 0.55], 0.018, sw("mgrey"), 6)
        subs.append(cl + [0, -0.02, 0])
    return subs


T500 = dict(base=5.0, waist=1.4, waist_y=24.0, knee_y=30.0, top_y=38.0, beam_h=1.6, peak_y=43.0,
            col_out=(4.6, 3.8), col_in=(3.7, 2.9), col_z=0.9, arm_x=10.5, phase_x=10.0, discs=24, string=4.3)


def lattice_500(number="500 kV  TOWER 214", variant="plain"):
    """Lattice family tier II, a 500 kV single-circuit "cat-head" suspension tower (real basis: US 500 kV self-supporting
    delta towers): four angle legs on pier footings tapering from 10 m square to a 2.8 m waist at 24 m; above it two
    side columns open out to the knees at 30 m and close in to a top beam at 38 m, making the window; a tapered truss arm
    out from each knee carries an outer phase on a 24-disc I-string, the centre phase hangs in the window on a V-string
    from the top beam, every phase a triple bundle; two shield-wire peaks at 43 m. Anti-climbing guard, step bolts,
    number and danger plates, legs bonded to ground."""
    TY._faces()
    if "danger500" not in TY.FACES:
        TY.FACES["danger500"] = ATLAS.add("flag:danger500", flag_img("DANGER  500 kV", (196, 30, 30), (250, 250, 250), 0.34, 0.12))
    T = T500
    b = Board(10, {"plain": 2301, "recond": 2302, "monitored": 2303}[variant], face=0.0)
    b.live = []
    poly = variant != "plain"
    B, Wd, wy, ky, ty = T["base"], T["waist"], T["waist_y"], T["knee_y"], T["top_y"]
    half = lambda y: B - (B - Wd) * min(1.0, y / wy)
    lattice_body(b, B, Wd, wy, [0.35, 3.4, 6.5, 9.4, 12.0, 14.5, 16.8, 18.9, 20.8, 22.5, wy], "danger500", number)
    # The side columns: waist -> knee -> top, outer and inner chords.
    co, ci, cz = T["col_out"], T["col_in"], T["col_z"]
    knee_o, knee_i = 4.6, 3.7
    for sx in (-1, 1):
        W = [(sx * Wd, wy, -Wd), (sx * Wd, wy, Wd), (sx * 0.5, wy, Wd), (sx * 0.5, wy, -Wd)]
        K = [(sx * knee_o, ky, -cz), (sx * knee_o, ky, cz), (sx * knee_i, ky, cz), (sx * knee_i, ky, -cz)]
        U = [(sx * co[1], ty, -cz), (sx * co[1], ty, cz), (sx * ci[1], ty, cz), (sx * ci[1], ty, -cz)]
        section_truss(b, W, K, [0, 0.34, 0.67, 1.0], leg_w=0.18, seed=1)
        section_truss(b, K, U, [0, 0.25, 0.5, 0.75, 1.0], leg_w=0.16, seed=0)
        perimeter(b, K, w=0.11, dy=0.0)
        bar(b, *shrink_((sx * 0.5, wy - 0.08, -Wd), (sx * 0.5, wy - 0.08, Wd), 0.2), 0.12, TY.GALV, h=0.1)
        # The arm: a tapered box truss out from the knee, its bottom chords level, the hanger at the phase.
        ya = ky + 2.6
        xo_a = knee_o + (co[1] - knee_o) * (2.6 / (ty - ky))
        R = [(sx * (knee_o + 0.05), ky, -cz), (sx * (knee_o + 0.05), ky, cz), (sx * (xo_a + 0.05), ya, cz), (sx * (xo_a + 0.05), ya, -cz)]
        X = [(sx * T["arm_x"], ky, -0.28), (sx * T["arm_x"], ky, 0.28), (sx * T["arm_x"], ky + 0.5, 0.28), (sx * T["arm_x"], ky + 0.5, -0.28)]
        section_truss(b, R, X, [0, 0.2, 0.4, 0.6, 0.8, 1.0], leg_w=0.13, brace_w=0.07, angle_legs=False)
        bar(b, (sx * T["arm_x"], ky + 0.25, -0.3), (sx * T["arm_x"], ky + 0.25, 0.3), 0.08, TY.GALV)        # end post
        px = sx * T["phase_x"]
        zx = 0.28 + (cz - 0.28) * (T["arm_x"] - T["phase_x"]) / (T["arm_x"] - knee_o)
        bar(b, (px, ky - 0.07, -zx), (px, ky - 0.07, zx), 0.1, TY.GALV, h=0.08)                               # hanger beam
    # The top beam across the window and the shield-wire peaks.
    TB = lambda x0, x1: ([(x0, ty, -cz), (x0, ty, cz), (x0, ty + T["beam_h"], cz), (x0, ty + T["beam_h"], -cz)],
                         [(x1, ty, -cz), (x1, ty, cz), (x1, ty + T["beam_h"], cz), (x1, ty + T["beam_h"], -cz)])
    section_truss(b, *TB(-co[1] - 0.02, co[1] + 0.02), list(np.linspace(0, 1, 9)), leg_w=0.15, brace_w=0.075, angle_legs=False)
    for sx in (-1, 1):
        pk = np.array([sx * 3.35, T["peak_y"], 0])
        for xx in (co[1], ci[1]):
            for zz in (-cz, cz):
                bar(b, (sx * xx, ty + T["beam_h"] + 0.05, zz), pk + [sx * (0.05 if xx == co[1] else -0.05), -0.15, zz * 0.1], 0.1, TY.GALV,
                    h=0.1 if xx == co[1] else 0.085)
        for t in (0.35, 0.68):
            y = ty + T["beam_h"] + (T["peak_y"] - ty - T["beam_h"]) * t
            bar(b, (sx * 3.35, y, -cz * (1 - t) + 0.05), (sx * 3.35, y, cz * (1 - t) - 0.05), 0.05, TY.GALV)
        cyl(b, pk - [0, 0.15, 0], pk + [0, 0.1, 0], 0.08, TY.GALV, sides=8)
        b.box(*_rnd(pk + [-0.06, 0.1, -0.25]), *_rnd(pk + [0.06, 0.22, 0.25]), TY.ALU)
    b.shield = [list(_rnd([sx * 3.35, T["peak_y"] + 0.18, 0])) for sx in (-1, 1)]
    # The phases: outer I-strings from the hangers, the centre V-string from the top beam (glass discs as built; the
    # reconductored towers got polymer long-rods with the new conductor).
    string = (lambda top, bot: polymer_longrod(b, top, bot)) if poly else (lambda top, bot: TY.disc_string(b, top, bot, T["discs"]))
    subs = []
    for sx in (-1, 1):
        px = sx * T["phase_x"]
        string((px, ky - 0.12, 0), (px, ky - 0.12 - T["string"], 0))
        subs += triple_yoke(b, (px, ky - 0.12 - T["string"], 0))
    cy = ty - 0.05 - 4.45
    for sx in (-1, 1):
        string((sx * 2.4, ty - 0.05, 0), (sx * 0.12, cy + 0.05, 0))
    b.box(-0.3, cy - 0.04, -0.04, 0.3, cy + 0.05, 0.04, ls("galv"))
    subs += triple_yoke(b, (0, cy - 0.04, 0))
    if poly:
        for p in subs:                                                        # Stockbridge dampers both sides
            for s_ in (-1, 1):
                d_ = 1.6 if variant == "monitored" else 1.3
                stockbridge(b, np.array(p) + [0, -0.06, s_ * d_])         # dropped by the conductor's sag that close in
        # The reconductoring plate under the number plate.
        key = ATLAS.add("tag:recond500", TY.tag_img("ACCC 1020 kcmil  2026", 0.44, 0.06))
        face_quad(b, (half(1.75) - 0.7, 1.75, -half(1.75) - 0.154), "-z", 0.44, 0.06, ATLAS.mat(key))
        b.box(half(1.75) - 0.93, 1.7, -half(1.75) - 0.15, half(1.75) - 0.47, 1.8, -half(1.75) - 0.11, TY.GALV)
    if variant == "monitored":
        for k in range(0, len(subs), 3):                                      # a line sensor on each phase
            line_sensor(b, np.array(subs[k]) + [0, -0.03, 0.95])
        weather_mast(b, (0.0, ty + T["beam_h"], 0.0), T["col_z"])
    b.attach = [list(_rnd(s)) for s in subs]
    return b


def lattice_body(b, B, Wd, wy, levels, danger, number, leg_w=0.22, brace_w=0.09):
    """A lattice tower's square body: pier footings with ground straps, four angle legs tapering from half-width B at
    the ground to Wd at the waist (wy), X-braced panels at levels, struts and a plan diamond at the waist, the
    anti-climbing guard at 3.2 m, step bolts up one leg, the danger and number plates. Returns half(y)."""
    half = lambda y: B - (B - Wd) * min(1.0, y / wy)
    # Footings, stubs and ground straps; the body in tapering panels.
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = sx * B, sz * B
            b.box(x - 0.6, -0.6, z - 0.6, x + 0.6, 0.35, z + 0.6, ["concrete"] * 5 + [None])
            path(b, [(x - sx * 0.4, 0.6, z), (x - sx * 0.68, 0.6, z), (x - sx * 0.68, 0.36, z), (x - sx * 0.68, 0.02, z + sz * 0.7)],
                 0.016, TY.COPPER)
    corners = lambda y: [(-half(y), y, -half(y)), (half(y), y, -half(y)), (half(y), y, half(y)), (-half(y), y, half(y))]
    section_truss(b, corners(0.35), corners(wy), [(y - 0.35) / (wy - 0.35) for y in levels], leg_w=leg_w, brace_w=brace_w)
    perimeter(b, corners(wy), w=0.12, dy=-0.08)
    for f in range(4):                                                               # plan diamond at the waist
        a, c = TY.face_pts(f, Wd, wy)
        m = (np.array(a) + np.array(c)) / 2
        q0, q1 = TY.face_pts((f + 1) % 4, Wd, wy)
        bar(b, *shrink_(m + [0, -0.2 - 0.03 * (f % 2), 0], (np.array(q0) + np.array(q1)) / 2 + [0, -0.2 - 0.03 * (f % 2), 0], 0.1), 0.08, TY.GALV)
    # Anti-climbing guard at 3.2 m, step bolts up one leg, the plates.
    for f in range(4):
        a, c = TY.face_pts(f, half(3.2) + 0.08, 3.2)
        bar(b, *shrink_(a, c, 0.12 + 0.02 * (f % 2)), 0.06, TY.GALV, h=0.06 + 0.01 * (f % 2))
        a = np.array(a); c = np.array(c); n = TY.face_normal(f)
        for t in np.linspace(0.05, 0.95, 18):
            p = a + (c - a) * t
            rod(b, p + [0, 0.03, 0], p + n * 0.22 + [0, 0.25, 0], 0.008, TY.GALV, 4)
    for y in np.arange(3.8, wy, 0.4):
        p = np.array([half(y), y, -half(y)])
        rod(b, p + [0.1, 0, 0], p + [0.28, 0, 0], 0.012, TY.GALV, 6)
    face_quad(b, (half(2.3) - 0.7, 2.3, -half(2.3) - 0.16), "-z", 0.34, 0.12, ATLAS.mat(TY.FACES[danger]))
    b.box(half(2.3) - 0.89, 2.22, -half(2.3) - 0.15, half(2.3) - 0.51, 2.38, -half(2.3) - 0.11, TY.GALV)
    tag(b, (half(1.95) - 0.7, 1.95, -half(1.95) - 0.154), "-z", number, 0.44, 0.06)
    b.box(half(1.95) - 0.93, 1.9, -half(1.95) - 0.15, half(1.95) - 0.47, 2.0, -half(1.95) - 0.11, TY.GALV)
    return half


def polymer_longrod(b, top, bottom, sheds=(0.095, 0.072), ring_r=0.3, pitch=0.07, core=0.026, earth_r=0.15):
    """A 500 kV polymer long-rod insulator from top to bottom, any direction: forged end fittings, the fibreglass rod in
    alternating silicone sheds, corona ring at the line end and a smaller one at the earth end."""
    top, bottom = np.array(top, float), np.array(bottom, float)
    a = (bottom - top) / np.linalg.norm(bottom - top)
    L = np.linalg.norm(bottom - top)
    cyl(b, top, top + a * 0.18, 0.035, ls("galv"), sides=8)
    cyl(b, top + a * 0.18, bottom - a * 0.2, core, ls("polymer"), sides=10)
    for i in range(int((L - 0.6) / pitch)):
        c = top + a * (0.3 + i * pitch)
        cyl(b, c, c + a * 0.012, sheds[i % 2], ls("polymer"), sides=14)
    cyl(b, bottom - a * 0.2, bottom, 0.035, ls("galv"), sides=8)
    ring(b, top + a * 0.22, a, earth_r, 0.03, ls("aluminium"), 14)
    ring(b, bottom - a * 0.26, a, ring_r, 0.045, ls("aluminium"), 18)


def stockbridge(b, p):
    """A Stockbridge vibration damper on a conductor at p (line along z): its clamp and the messenger cable with a
    weight at each end."""
    p = np.array(p, float)
    b.box(*_rnd(p + [-0.03, -0.02, -0.04]), *_rnd(p + [0.03, 0.04, 0.04]), ls("aluminium"))
    m = p - [0, 0.12, 0]
    b.box(*_rnd(p + [-0.012, -0.12, -0.012]), *_rnd(p + [0.012, -0.02, 0.012]), ls("aluminium"))
    rod(b, m - [0, 0, 0.22], m + [0, 0, 0.22], 0.007, ls("galv"), 6)
    cyl(b, m - [0, 0, 0.18], m - [0, 0, 0.32], 0.035, ls("galv_dark"), sides=10)
    cyl(b, m + [0, 0, 0.16], m + [0, 0, 0.3], 0.03, ls("galv_dark"), sides=10)


def line_sensor(b, p):
    """A conductor-mounted line monitor (real basis: dynamic line rating sensors clamped round one sub-conductor,
    powered by the line's own current): a split housing round the conductor, a solar band, end clamps, an antenna stub."""
    p = np.array(p, float)
    cyl(b, p - [0, 0, 0.22], p - [0, 0, 0.3], 0.04, ls("galv"), sides=10)                        # (caps at the far end)
    cyl(b, p, p - [0, 0, 0.22], 0.1, ls("polymer"), sides=14)
    cyl(b, p, p + [0, 0, 0.22], 0.1, ls("polymer"), sides=14)
    cyl(b, p - [0, 0, 0.05], p + [0, 0, 0.05], 0.104, ls("galv_dark"), sides=14)
    cyl(b, p + [0, 0, 0.22], p + [0, 0, 0.3], 0.04, ls("galv"), sides=10)
    rod(b, p + [0, 0.1, 0.12], p + [0, 0.24, 0.12], 0.008, ls("galv_dark"), 6)


def weather_mast(b, base, cz):
    """The line-rating weather station on the top beam: a mast on a plate across the top chords, a crossarm with a cup
    anemometer and a wind vane, a louvred temperature shield, a pyranometer, the logger box with its solar panel and
    radio antenna."""
    base = np.array(base, float)
    b.box(*_rnd(base + [-0.15, 0.08, -cz - 0.05]), *_rnd(base + [0.15, 0.12, cz + 0.05]), ls("galv"))
    top = base + [0, 2.6, 0]
    cyl(b, base + [0, 0.12, 0], top, 0.045, ls("galv"), sides=10)
    arm = top - [0, 0.1, 0]
    rod(b, arm + [-0.7, 0, 0], arm + [0.7, 0, 0], 0.02, ls("galv"), 8)
    # cup anemometer at one end
    a = arm + [-0.7, 0, 0]
    cyl(b, a, a + [0, 0.25, 0], 0.025, ls("aluminium"), sides=8)
    hub = a + [0, 0.27, 0]
    for k in range(3):
        t = 2 * math.pi * k / 3
        d = np.array([math.cos(t), 0, math.sin(t)])
        rod(b, hub, hub + d * 0.16, 0.006, ls("aluminium"), 4)
        c = hub + d * 0.16
        cyl(b, c - np.cross(d, [0, 1, 0]) * 0.03, c + np.cross(d, [0, 1, 0]) * 0.03, 0.035, ls("polymer"), sides=10)
    # wind vane at the other end
    v = arm + [0.7, 0, 0]
    cyl(b, v, v + [0, 0.22, 0], 0.025, ls("aluminium"), sides=8)
    rod(b, v + [0, 0.24, -0.25], v + [0, 0.24, 0.2], 0.008, ls("aluminium"), 4)
    b.box(*_rnd(v + [-0.005, 0.18, 0.2]), *_rnd(v + [0.005, 0.32, 0.36]), ls("aluminium"))
    cyl(b, v + [0, 0.24, -0.3], v + [0, 0.24, -0.25], 0.03, ls("galv_dark"), sides=8)
    # louvred temperature shield and pyranometer on the mast
    sh = base + [0.12, 1.7, 0]
    b.box(*_rnd(sh + [0.0, -0.02, -0.03]), *_rnd(sh + [0.08, 0.02, 0.03]), ls("galv"))
    for i in range(6):
        y = sh[1] - 0.15 + i * 0.05
        cyl(b, (sh[0] + 0.17, y, 0), (sh[0] + 0.17, y + 0.015, 0), 0.09 - 0.004 * i, ls("aluminium"), sides=14)
    py = top + [0, 0.0, 0]
    cyl(b, py, py + [0, 0.04, 0], 0.07, ls("aluminium"), sides=12)
    cyl(b, py + [0, 0.04, 0], py + [0, 0.08, 0], 0.03, ls("porcelain_grey"), sides=10)
    # logger box, solar panel facing south (-z), antenna
    lb = base + [-0.05, 0.9, 0]
    b.box(*_rnd(lb + [-0.33, -0.22, -0.2]), *_rnd(lb + [-0.05, 0.22, 0.12]), ls("galv"))
    b.box(*_rnd(lb + [-0.31, -0.2, -0.215]), *_rnd(lb + [-0.07, 0.2, -0.2]), ls("galv"))                  # door
    for dy in (-0.15, 0.15):
        b.box(*_rnd(lb + [-0.05, dy - 0.02, -0.03]), *_rnd(lb + [0.006, dy + 0.02, 0.03]), ls("galv"))
    sp = base + [0, 1.25, -0.12]
    b.obox(tuple(_rnd(sp + [0, 0.12, -0.22])), (1, 0, 0), (0, 0.766, -0.643), 0.3, 0.22, 0.015, ls("galv_dark"))
    rod(b, sp, sp + [0, 0.12, -0.18], 0.012, ls("galv"), 6)
    rod(b, lb + [-0.2, 0.22, 0.05], lb + [-0.2, 0.9, 0.05], 0.006, ls("galv_dark"), 6)


# ---------------------------------------------------------------- UHV family

def hex_yoke(b, apex, r=0.457):
    """A six-conductor bundle's hexagonal yoke (see poly_yoke)."""
    return poly_yoke(b, apex, 6, r)


def poly_yoke(b, apex, n, r):
    """The apex plate under a V-string and an n-conductor bundle's polygonal yoke frame: a strap down to the frame,
    the frame's bars, a link and an armour-rodded clamp hanging below each corner. Returns the sub-conductor points in
    order round the polygon."""
    apex = np.array(apex, float)
    w = max(0.32, r * 0.7)
    b.box(*_rnd(apex + [-w, -0.05, -0.04]), *_rnd(apex + [w, 0.05, 0.04]), ls("galv"))
    y_top = apex[1] - 0.05 - 0.28
    b.box(*_rnd([apex[0] - 0.025, y_top, -0.02]), *_rnd([apex[0] + 0.025, apex[1] - 0.05, 0.02]), ls("galv"))
    top = max(math.sin(2 * math.pi * k / n) for k in range(n))
    hc = np.array([apex[0], y_top - 0.12 - r * top - 0.0, 0.0])
    subs = [hc + [r * math.cos(2 * math.pi / n * k), r * math.sin(2 * math.pi / n * k), 0] for k in range(n)]
    V = [s + [0, 0.12, 0] for s in subs]
    for k in range(n):
        bar(b, *shrink_(V[k], V[(k + 1) % n], 0.03), 0.05, ls("galv"), h=0.035 + 0.004 * (k % 2))
        cyl(b, V[k] + [0, 0, -0.03], V[k] + [0, 0, 0.03], 0.04, ls("galv"), sides=8)
        b.box(*_rnd(subs[k] + [-0.012, 0.03, -0.012]), *_rnd(subs[k] + [0.012, 0.085, 0.012]), ls("galv"))
        b.box(*_rnd(subs[k] + [-0.04, -0.04, -0.1]), *_rnd(subs[k] + [0.04, 0.03, 0.1]), ls("aluminium"))
        rod(b, subs[k] + [0, -0.005, -0.5], subs[k] + [0, -0.005, 0.5], 0.017, sw("mgrey"), 6)
    return [list(_rnd(s + [0, -0.005, 0])) for s in subs]


T765 = dict(base=6.5, waist=1.6, waist_y=30.0, arm_y=45.0, arm_h=2.5, col_top=(9.8, 8.6), col_z=1.2, arm_x=21.0,
            phase_x=(-15.0, 0.0, 15.0), v_half=3.2, v_drop=5.8, discs=32, peak_y=53.0)


def uhv_765(number="765 kV  TOWER 88"):
    """UHV family, a 765 kV single-circuit horizontal suspension tower (real basis: US 765 kV self-supporting towers):
    a broad body tapering from 13 m square to a 3.2 m waist at 30 m; two four-chord columns spread up to a 42 m box-truss
    crossarm at 45 m, making the window; every phase on a 32-disc V-string (6.5 m legs) so it cannot swing, the centre
    phase in the window, the outer two under the arms, 15 m apart; every phase a six-conductor bundle on a hexagonal
    yoke; two shield-wire peaks at 53 m. Anti-climbing guard, step bolts, plates, legs bonded to ground."""
    TY._faces()
    if "danger765" not in TY.FACES:
        TY.FACES["danger765"] = ATLAS.add("flag:danger765", flag_img("DANGER  765 kV", (196, 30, 30), (250, 250, 250), 0.34, 0.12))
    T = T765
    b = Board(10, 2401, face=0.0)
    b.live = []
    B, Wd, wy, ay, ah = T["base"], T["waist"], T["waist_y"], T["arm_y"], T["arm_h"]
    lattice_body(b, B, Wd, wy, [0.35, 3.6, 6.9, 10.0, 12.9, 15.6, 18.1, 20.4, 22.6, 24.6, 26.5, 28.3, wy], "danger765", number,
                 leg_w=0.26, brace_w=0.1)
    co, ci, cz = T["col_top"][0], T["col_top"][1], T["col_z"]
    for sx in (-1, 1):
        W = [(sx * Wd, wy, -Wd), (sx * Wd, wy, Wd), (sx * 0.6, wy, Wd), (sx * 0.6, wy, -Wd)]
        U = [(sx * co, ay, -cz), (sx * co, ay, cz), (sx * ci, ay, cz), (sx * ci, ay, -cz)]
        section_truss(b, W, U, [0, 0.17, 0.33, 0.5, 0.67, 0.83, 1.0], leg_w=0.2, seed=1)
        bar(b, *shrink_((sx * 0.6, wy - 0.08, -Wd), (sx * 0.6, wy - 0.08, Wd), 0.2), 0.12, TY.GALV, h=0.1)
    # The crossarm: a constant box truss between the columns, tapering arms out beyond them.
    sec = lambda x, y0, y1, z: [(x, y0, -z), (x, y0, z), (x, y1, z), (x, y1, -z)]
    section_truss(b, sec(-co - 0.02, ay, ay + ah, cz), sec(co + 0.02, ay, ay + ah, cz), list(np.linspace(0, 1, 11)),
                  leg_w=0.17, brace_w=0.08, angle_legs=False)
    for sx in (-1, 1):
        section_truss(b, sec(sx * (co + 0.06), ay, ay + ah, cz), sec(sx * T["arm_x"], ay, ay + 0.9, 0.55),
                      list(np.linspace(0, 1, 8)), leg_w=0.15, brace_w=0.075, angle_legs=False)
        bar(b, (sx * T["arm_x"], ay + 0.45, -0.57), (sx * T["arm_x"], ay + 0.45, 0.57), 0.08, TY.GALV)
    zarm = lambda x: cz if abs(x) <= co else cz + (0.55 - cz) * (abs(x) - co) / (T["arm_x"] - co)
    # Shield-wire peaks over the columns.
    for sx in (-1, 1):
        pk = np.array([sx * (co + ci) / 2, T["peak_y"], 0])
        for xx in (co, ci):
            for zz in (-cz, cz):
                bar(b, (sx * xx, ay + ah + 0.06, zz), pk + [sx * (0.06 if xx == co else -0.06), -0.15, zz * 0.08], 0.12, TY.GALV,
                    h=0.12 if xx == co else 0.1)
        for t in (0.25, 0.5, 0.75):
            y = ay + ah + (T["peak_y"] - ay - ah) * t
            bar(b, (pk[0], y, -cz * (1 - t) + 0.06), (pk[0], y, cz * (1 - t) - 0.06), 0.05, TY.GALV)
        cyl(b, pk - [0, 0.15, 0], pk + [0, 0.1, 0], 0.08, TY.GALV, sides=8)
        b.box(*_rnd(pk + [-0.06, 0.1, -0.25]), *_rnd(pk + [0.06, 0.22, 0.25]), TY.ALU)
    b.shield = [list(_rnd([sx * (co + ci) / 2, T["peak_y"] + 0.18, 0])) for sx in (-1, 1)]
    # The phases: each on a V-string from two hangers under the crossarm, a hexagonal six-bundle yoke.
    subs = []
    for px in T["phase_x"]:
        apex = np.array([px, ay - 0.12 - T["v_drop"], 0])
        for s_ in (-1, 1):
            hx = px + s_ * T["v_half"]
            z = zarm(hx) + 0.02
            bar(b, (hx, ay - 0.06, -z), (hx, ay - 0.06, z), 0.11, TY.GALV, h=0.09)
            TY.disc_string(b, (hx, ay - 0.12, 0), apex + [s_ * 0.15, 0.06, 0], T["discs"])
        subs += hex_yoke(b, apex)
    b.attach = subs
    return b


# ---------------------------------------------------------------- UHVDC family

TDC = dict(base=7.5, waist=2.0, waist_y=45.0, head=1.5, arm_y=62.0, arm_h=3.0, arm_x=15.5, pole_x=12.0, v_half=5.0,
           v_drop=12.0, peak_x=7.0, peak_y=71.0, bundle_r=0.72)


def uhvdc_tower(number="UHVDC  TOWER 1207", angle=False):
    """UHVDC family, a +-1,100 kV bipole suspension tower (real basis: the Changji-Guquan line's towers, the highest
    DC voltage in service): a 15 m square body tapering to a 4 m waist at 45 m, the head column on up to a crossarm at
    62 m; the two poles 24 m apart, each on a V of 13 m composite long-rod insulators with large DC grading rings, each
    pole an eight-conductor bundle on an octagonal yoke; two earth-wire peaks at 71 m; pole plates on the arms."""
    TY._faces()
    if "dangerdc" not in TY.FACES:
        TY.FACES["dangerdc"] = ATLAS.add("flag:dangerdc", flag_img("DANGER  1100 kV DC", (196, 30, 30), (250, 250, 250), 0.4, 0.12))
    T = TDC
    b = Board(10, 2501, face=0.0)
    b.live = []
    B, Wd, wy, hd, ay, ah = T["base"], T["waist"], T["waist_y"], T["head"], T["arm_y"], T["arm_h"]
    levels = [0.35, 3.8, 7.3, 10.6, 13.7, 16.7, 19.5, 22.2, 24.8, 27.3, 29.7, 32.0, 34.3, 36.5, 38.7, 40.9, 43.0, wy]
    lattice_body(b, B, Wd, wy, levels, "dangerdc", number, leg_w=0.3, brace_w=0.11)
    sq = lambda h, y: [(-h, y, -h), (h, y, -h), (h, y, h), (-h, y, h)]
    section_truss(b, sq(Wd, wy), sq(hd, ay), list(np.linspace(0, 1, 8)), leg_w=0.22, brace_w=0.09, seed=1)
    # The crossarm: a box over the head, tapered arms each side, the hangers, the pole plates.
    sec = lambda x, y0, y1, z: [(x, y0, -z), (x, y0, z), (x, y1, z), (x, y1, -z)]
    section_truss(b, sec(-hd - 0.02, ay, ay + ah, hd), sec(hd + 0.02, ay, ay + ah, hd), [0, 0.5, 1.0], leg_w=0.22, brace_w=0.09,
                  angle_legs=False)
    for sx in (-1, 1):
        section_truss(b, sec(sx * (hd + 0.06), ay, ay + ah, hd), sec(sx * T["arm_x"], ay, ay + 0.9, 0.55),
                      list(np.linspace(0, 1, 11)), leg_w=0.17, brace_w=0.08, angle_legs=False)
        bar(b, (sx * T["arm_x"], ay + 0.45, -0.57), (sx * T["arm_x"], ay + 0.45, 0.57), 0.08, TY.GALV)
    zarm = lambda x: hd + (0.55 - hd) * (abs(x) - hd) / (T["arm_x"] - hd)
    yarm = lambda x: ah + (0.9 - ah) * (abs(x) - hd) / (T["arm_x"] - hd)
    for sx, label in ((-1, "POLE 1  +"), (1, "POLE 2  -")):
        x = sx * (T["pole_x"] + 0.8)
        key = ATLAS.add(f"tag:dc:{label}", TY.tag_img(label, 0.5, 0.14))
        face_quad(b, (x, ay + yarm(x) / 2, -zarm(x) - 0.1), "-z", 0.5, 0.14, ATLAS.mat(key))
        b.box(x - 0.27, ay + yarm(x) / 2 - 0.09, -zarm(x) - 0.095, x + 0.27, ay + yarm(x) / 2 + 0.09, -zarm(x) - 0.05, TY.GALV)
    # Earth-wire peaks on the arms.
    for sx in (-1, 1):
        pk = np.array([sx * T["peak_x"], T["peak_y"], 0])
        for dx in (-0.6, 0.6):
            xx = sx * T["peak_x"] + dx
            yt = ay + yarm(xx) + 0.06
            for zz in (-1, 1):
                bar(b, (xx, yt, zz * zarm(xx)), pk + [dx * 0.1, -0.15, zz * 0.08], 0.13, TY.GALV, h=0.13 if dx > 0 else 0.11)
        for t in (0.2, 0.4, 0.6, 0.8):
            y0 = ay + yarm(pk[0])
            y = y0 + (T["peak_y"] - y0) * t
            zz = zarm(pk[0]) * (1 - t)
            bar(b, (pk[0], y, -zz + 0.07), (pk[0], y, zz - 0.07), 0.05, TY.GALV)
        cyl(b, pk - [0, 0.15, 0], pk + [0, 0.1, 0], 0.08, TY.GALV, sides=8)
        b.box(*_rnd(pk + [-0.06, 0.1, -0.25]), *_rnd(pk + [0.06, 0.22, 0.25]), TY.ALU)
    b.shield = [list(_rnd([sx * T["peak_x"], T["peak_y"] + 0.18, 0])) for sx in (-1, 1)]
    # The poles: V of composite long-rods from two hangers, the octagonal eight-bundle yoke.
    if angle:
        hinges = []
        for sx in (-1, 1):
            px = sx * T["pole_x"]
            z = zarm(px) + 0.02
            bar(b, (px, ay - 0.06, -z), (px, ay - 0.06, z), 0.14, TY.GALV, h=0.1)
            hinges.append(np.array([px, ay - 0.16, 0.0]))
        b.kit = dict(kind="strain", hinges=hinges, jr=0.02,
                     spec=dict(ins="poly", L=13.0, n=8, rb=0.72, double=True, gap=0.9, ring_r=0.6, sheds=(0.16, 0.12), pitch=0.09, core=0.04))
        return b
    subs = []
    for sx in (-1, 1):
        px = sx * T["pole_x"]
        apex = np.array([px, ay - 0.12 - T["v_drop"], 0])
        for s_ in (-1, 1):
            hx = px + s_ * T["v_half"]
            z = zarm(hx) + 0.02
            bar(b, (hx, ay - 0.06, -z), (hx, ay - 0.06, z), 0.12, TY.GALV, h=0.1)
            polymer_longrod(b, (hx, ay - 0.12, 0), apex + [s_ * 0.2, 0.06, 0], sheds=(0.16, 0.12), ring_r=0.6,
                            pitch=0.09, core=0.04, earth_r=0.3)
        subs += poly_yoke(b, apex, 8, T["bundle_r"])
    b.attach = subs
    return b



# ---------------------------------------------------------------- superconducting family

SC = dict(top=21.0, r0=0.42, r1=0.2, arm_x=2.6, cradle_y=20.0, horn=1.2, pod_y=8.5, sleeve=0.8)


def sc_radius(y):
    return SC["r0"] + (SC["r1"] - SC["r0"]) * min(1.0, max(0.0, y / SC["top"]))


def _bezier(p0, p1, p2, n):
    p0, p1, p2 = (np.array(p, float) for p in (p0, p1, p2))
    return [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, n)]


def cryo_sleeve(b, c, sleeve=True):
    """The cradle round a superconducting cable at c (line along z): a white clamshell sleeve whose ends droop a
    little with the cable, polymer-lined, a lit status ring at each mouth, the neck up to the arm."""
    c = np.array(c, float)
    L = SC["sleeve"]
    for s_ in ((-1, 1) if sleeve else ()):
        cyl(b, c, c + [0, 0, s_ * 0.3], 0.12, ls("composite_white"), sides=18)
        cyl(b, c + [0, 0, s_ * 0.3], c + [0, -0.012, s_ * (L - 0.06)], 0.118, ls("composite_white"), sides=18)
        cyl(b, c + [0, -0.012, s_ * (L - 0.06)], c + [0, -0.02, s_ * L], 0.124, ls("cryo_jacket"), sides=18)
        cyl(b, c + [0, -0.014, s_ * (L - 0.1)], c + [0, -0.015, s_ * (L - 0.07)], 0.122, ls("sc_glow"), sides=18, lst=b.lit)
    b.box(*_rnd(c + [-0.05, 0.1, -0.35]), *_rnd(c + [0.05, 0.3, 0.35]), ls("composite_white"))       # neck


def pod(b, c):
    """The booster cooling pod on the back of the pylon (real basis: the intermediate cooling stations long
    superconducting cables need): a white capsule with rounded ends, a lit status band, radiator fins on its back,
    two brackets to the pole. Returns its top point."""
    c = np.array(c, float)
    h, r = 1.1, 0.38
    cyl(b, c, c + [0, h - 0.2, 0], r, ls("composite_white"), sides=20)
    cyl(b, c, c - [0, h - 0.2, 0], r, ls("composite_white"), sides=20)
    for i, (dy, rr) in enumerate(((0.08, 0.34), (0.06, 0.27), (0.05, 0.16))):
        y0 = h - 0.2 + sum(d for d, _ in ((0.08, 0), (0.06, 0), (0.05, 0))[:i])
        cyl(b, c + [0, y0, 0], c + [0, y0 + dy, 0], rr, ls("composite_white"), sides=20)
        cyl(b, c - [0, y0, 0], c - [0, y0 + dy, 0], rr, ls("composite_white"), sides=20)
    cyl(b, c + [0, 0.12, 0], c + [0, 0.18, 0], r + 0.006, ls("sc_glow"), sides=20, lst=b.lit)
    for k in range(7):
        x = -0.24 + k * 0.08
        b.box(*_rnd(c + [x - 0.006, -0.6, r * 0.75]), *_rnd(c + [x + 0.006, 0.6, r * 0.75 + 0.14 - 0.03 * abs(k - 3) / 3]), ls("galv_dark"))
    for dy in (-0.5, 0.5):
        y = c[1] + dy
        b.box(*_rnd([-0.08, y - 0.06, sc_radius(y) - 0.02]), *_rnd([0.08, y + 0.06, c[2] - r + 0.02]), ls("composite_white"))
    return c + [0, h, 0]


def superconducting_pylon(number="SC-1  PYLON 12", sleeve=True):
    """Superconducting family: a slim white composite pylon carrying two superconducting cables (real basis: HTS
    triaxial cables, all three phases in one grounded cryostat, so no insulators; the T-pylon's clean lines). Each cable
    rests in a sleeve under a swept arm and hangs between pylons from a messenger wire on droppers, so it runs nearly
    level and its curve follows the line round turns (the head turns to bisect the angle); a booster cooling pod halfway
    up feeds both sleeves with nitrogen lines. Aviation light on top, number plate, ground terminal. The cable runs
    straight between sleeves; on a turn the sleeve bends round the angle, so the line follows turns without kinks."""
    T = SC
    b = Board(4, 2601, face=0.0)
    b.live = []
    top = T["top"]
    cyl(b, (0, -0.6, 0), (0, 0.3, 0), 0.85, ls("concrete_spun"), sides=24)
    cyl(b, (0, 0.3, 0), (0, 0.38, 0), 0.62, ls("galv"), sides=20)
    for k in range(10):
        t = 2 * math.pi * (k + 0.5) / 10
        p = np.array([math.cos(t) * 0.53, 0.38, math.sin(t) * 0.53])
        cyl(b, p, p + [0, 0.08, 0], 0.03, ls("bolt_head"), sides=6)
    segs = 24
    for i in range(segs):
        ya, yb = 0.38 + (top - 0.38) * i / segs, 0.38 + (top - 0.38) * (i + 1) / segs
        cyl(b, (0, ya, 0), (0, yb, 0), sc_radius((ya + yb) / 2), ls("composite_white"), sides=20)
    for yj in (7.0, 14.0):
        cyl(b, (0, yj - 0.03, 0), (0, yj + 0.03, 0), sc_radius(yj) + 0.008, ls("cryo_jacket"), sides=20)
    # The dome and the aviation light.
    for i, (dy, rr) in enumerate(((0.08, 0.19), (0.07, 0.16), (0.06, 0.11))):
        y0 = top + sum((0.08, 0.07, 0.06)[:i])
        cyl(b, (0, y0, 0), (0, y0 + dy, 0), rr, ls("composite_white"), sides=20)
    cyl(b, (0, top + 0.21, 0), (0, top + 0.31, 0), 0.06, ls("aviation_red"), sides=12, lst=b.lit)
    # The swept arms, the sleeves under them, the messenger horns.
    cradles, horns = [], []
    for side in (-1, 1):
        x = side * T["arm_x"]
        tip = np.array([x, T["cradle_y"] + 0.3, 0])
        pts = _bezier((0, top - 1.6, 0), (side * 0.25, top - 0.2, 0), tip + [0, 0.12, 0], 9)
        for k, (p, q) in enumerate(zip(pts, pts[1:])):
            cyl(b, p, q, 0.15 - 0.075 * k / 7, ls("composite_white"), sides=14)
        c = np.array([x, T["cradle_y"], 0])
        cryo_sleeve(b, c, sleeve)        # (on a turn the game bends the sleeve to the angle: sleeve=False, drawn with the line)
        cradles.append(c)
        cyl(b, tip + [0, 0.1, 0], tip + [0, T["horn"] - 0.1, 0], 0.055, ls("composite_white"), sides=12)
        hp = tip + [0, T["horn"], 0]
        b.box(*_rnd(hp + [-0.04, -0.1, -0.09]), *_rnd(hp + [0.04, 0.02, 0.09]), ls("aluminium"))
        horns.append(hp + [0, 0.02, 0])
    # The booster pod on the back, its nitrogen lines up the pole and along each arm into the sleeve.
    pc = np.array([0, T["pod_y"], sc_radius(T["pod_y"]) + 0.46])
    ptop = pod(b, pc)
    for side in (-1, 1):
        zr = lambda y: sc_radius(y) + 0.06
        x0 = side * 0.12
        line = [ptop + [x0, -0.05, -0.16], ptop + [x0, 0.25, -0.16], (x0, ptop[1] + 0.6, zr(ptop[1] + 0.6)),
                (x0, top - 2.2, zr(top - 2.2))]
        arm = _bezier((0, top - 1.6, 0), (side * 0.25, top - 0.2, 0), np.array([side * T["arm_x"], T["cradle_y"] + 0.42, 0]), 9)
        line += [p + [0, 0, 0.23] for p in arm[1:]]
        line += [(side * T["arm_x"], T["cradle_y"] + 0.11, 0.23)]
        path(b, line, 0.03, ls("cryo_jacket"), sides=8)
    # Plate at eye height, ground terminal and lead.
    key = ATLAS.add(f"tag:{number}:sc", TY.tag_img(number, 0.3, 0.07))
    face_quad(b, (0, 1.8, -sc_radius(1.8) - 0.006), "-z", 0.3, 0.07, ATLAS.mat(key))
    b.box(-0.06, 0.5, -sc_radius(0.5) - 0.04, 0.06, 0.62, -sc_radius(0.5) + 0.01, ls("galv"))
    path(b, [(0, 0.52, -sc_radius(0.5) - 0.03), (0, 0.3, -0.75), (0, 0.02, -0.9)], 0.009, ls("copper_wire"))
    b.attach = [list(_rnd(c)) for c in cradles]
    b.messenger = [list(_rnd(h)) for h in horns]
    return b



# ---------------------------------------------------------------- boxes: the service pedestal, and the pole transformer that feeds it

def pole_transformer(b, side, kva="25"):
    """On a wooden pole feeding a Service Pedestal (real basis: a 25 kVA single-phase pole-mount distribution
    transformer, 7.2 kV to 120/240 V): the grey tank hung on two brackets through-bolted to the pole, the HV bushing on
    the cover fed from the line through a fused cutout with its lightning arrester beside it, the three LV bushings on
    the side, their leads in a drip loop to the service drop's spool on a secondary rack; kVA stencilled on the tank,
    tank and arrester bonded to the pole's ground wire. side: +-1 (frame x). Returns the drop's point on the spool."""
    s = side
    yt0, yt1, rt = 8.45, 9.3, 0.27
    rp = pole_radius(8.9)
    cx = s * (rp + 0.06 + rt)
    c = np.array([cx, 0, 0])
    cyl(b, c + [0, yt0, 0], c + [0, yt1, 0], rt, ls("transformer_grey"), sides=20)
    cyl(b, c + [0, yt0, 0], c + [0, yt0 - 0.04, 0], rt - 0.03, ls("transformer_grey"), sides=20)
    cyl(b, c + [0, yt1, 0], c + [0, yt1 + 0.04, 0], rt + 0.015, ls("transformer_grey"), sides=20)       # cover rim
    cyl(b, c + [0, yt1 + 0.04, 0], c + [0, yt1 + 0.08, 0], rt - 0.05, ls("transformer_grey"), sides=20)
    for y in (yt1 - 0.12, yt0 + 0.15):                                                   # hangers, bolts through the pole
        x0, x1 = sorted((s * (pole_radius(y) - 0.01), s * (rp + 0.07)))
        b.box(*_rnd([x0, y - 0.05, -0.1]), *_rnd([x1, y + 0.05, 0.1]), ls("galv"))
        through_bolt(b, (0, y, -0.06), "x", 2 * pole_radius(y) + 0.16)
        through_bolt(b, (0, y, 0.06), "x", 2 * pole_radius(y) + 0.16)
    # HV bushing on the cover, LV bushings on the outer side.
    hb = c + [-s * 0.08, yt1 + 0.08, 0]
    for i in range(4):
        cyl(b, hb + [0, 0.03 + i * 0.045, 0], hb + [0, 0.045 + i * 0.045, 0], 0.055, ls("porcelain_grey"), sides=12)
    cyl(b, hb, hb + [0, 0.22, 0], 0.03, ls("porcelain_grey"), sides=10)
    cyl(b, hb + [0, 0.22, 0], hb + [0, 0.27, 0], 0.012, ls("copper_wire"), sides=6)
    lvs = []
    for dz in (-0.11, 0.0, 0.11):
        p = c + [s * (rt - 0.01), yt1 - 0.25, dz]
        cyl(b, p, p + [s * 0.07, 0, 0], 0.028, ls("porcelain_brown"), sides=10)
        cyl(b, p + [s * 0.07, 0, 0], p + [s * 0.1, 0, 0], 0.01, ls("copper_wire"), sides=6)
        lvs.append(p + [s * 0.1, 0, 0])
    im = TY.stencil_img(kva, 0.12)
    key = ATLAS.add(f"stencil:kva:{kva}", im)
    face_quad(b, c + [0, (yt0 + yt1) / 2, -rt - 0.003], "-z", im.width / TY.PXB, 0.12, ATLAS.mat(key))
    # Fused cutout and arrester on a bracket above, the line lead down to the cutout and on to the HV bushing.
    yc = 10.25
    rpc = pole_radius(yc)
    br0 = np.array([s * rpc, yc, 0])
    b.box(*_rnd([min(br0[0], br0[0] + s * 0.36), yc - 0.03, -0.03]), *_rnd([max(br0[0], br0[0] + s * 0.36), yc + 0.03, 0.03]), ls("galv"))
    through_bolt(b, (0, yc, 0), "x", 2 * rpc + 0.1)
    ct0, ct1 = br0 + [s * 0.36, -0.22, -0.08], br0 + [s * 0.42, 0.26, -0.08]
    cyl(b, (ct0 + ct1) / 2 + [0, 0, 0.0] - (ct1 - ct0) * 0.25, (ct0 + ct1) / 2 + (ct1 - ct0) * 0.25, 0.04, ls("porcelain_grey"), sides=10)
    b.box(*_rnd(br0 + [s * 0.32 if s > 0 else -0.4, -0.03, -0.12]), *_rnd(br0 + [0.4 if s > 0 else -0.32, 0.03, -0.04]), ls("galv"))
    fz = -0.15
    cyl(b, ct0 + [0, 0, fz + 0.08], ct1 + [0, 0, fz + 0.08], 0.022, ls("creosote_butt"), sides=8)        # fuse tube
    for p in (ct0, ct1):
        b.box(*_rnd(p + [-0.035, -0.03, -0.1]), *_rnd(p + [0.035, 0.03, -0.0]), ls("galv"))
    ar = br0 + [s * 0.36, 0, 0.1]
    cyl(b, ar + [0, 0.03, 0], ar + [0, 0.33, 0], 0.03, ls("polymer"), sides=10)
    for i in range(4):
        cyl(b, ar + [0, 0.08 + i * 0.06, 0], ar + [0, 0.09 + i * 0.06, 0], 0.05, ls("polymer"), sides=12)
    b.box(*_rnd(ar + [-0.04, -0.02, -0.04]), *_rnd(ar + [0.04, 0.03, 0.04]), ls("galv"))
    pin = np.array([s * WOOD_POLE["pins"][1], WOOD_POLE["arm_y"] + 0.045 + 0.2, -pole_radius(WOOD_POLE["arm_y"]) - 0.0575])
    path(b, [pin + [0, 0.03, 0.35], pin + [-s * 0.1, -0.2, 0.35], ct1 + [0, 0.05, -0.04]], 0.006, ls("copper_wire"))
    path(b, [ct1 + [0, 0.06, -0.06], ar + [0, 0.36, 0]], 0.005, ls("copper_wire"))
    path(b, [ct0 + [0, -0.03, -0.06], ct0 + [0, -0.4, -0.06], hb + [0, 0.27, 0]], 0.006, ls("copper_wire"))
    # Secondary rack with the drop's spool, LV leads in a drip loop to it; bonds to the ground wire on the back.
    ys = 7.75
    rs = pole_radius(ys)
    b.box(*_rnd([min(s * rs, s * (rs + 0.12)), ys - 0.12, -0.03]), *_rnd([max(s * rs, s * (rs + 0.12)), ys + 0.12, 0.03]), ls("galv"))
    sp = np.array([s * (rs + 0.1), ys, 0])
    cyl(b, sp + [0, -0.05, 0], sp + [0, 0.05, 0], 0.045, ls("porcelain_grey"), sides=12)
    through_bolt(b, (0, ys, 0), "x", 2 * rs + 0.18)
    drop = sp + [s * 0.05, 0, 0]
    for k, p in enumerate(lvs):
        path(b, [p, p + [s * 0.06, -0.08, 0], p + [s * 0.12, -0.6, 0], drop + [s * 0.08, 0.06 + 0.03 * k, (k - 1) * 0.03]], 0.008,
             ls("ground_molding"))
    gz = pole_radius(yt0) + 0.012
    path(b, [c + [-s * 0.15, yt0 - 0.02, rt * 0.55], (s * 0.04, yt0 - 0.25, gz)], 0.004, ls("copper_wire"))
    path(b, [ar + [0, 0.0, 0.04], (s * 0.04, yc - 0.3, pole_radius(yc - 0.3) + 0.012)], 0.004, ls("copper_wire"))
    return drop


def service_pedestal(number="SVC 1427-1"):
    """Box tier 1, the Service Pedestal (real basis: an overhead-fed 200 A meter pedestal at a farm or yard): a grey
    steel pedestal on a concrete pad, the meter socket with a smart meter (lit display) behind its glass, the lower
    door to the main breaker with hinges and a padlock hasp, louvred vents, an engraved tag; the service mast with
    a clevis and spool where the drop's neutral dead-ends, the weatherhead the conductors enter by a drip loop; the
    load conduit out to the mod's cables; ground rod and clamp."""
    b = Board(2, 2701, face=0.0)
    b.live = []
    b.box(-0.45, 0.0, -0.42, 0.45, 0.1, 0.42, ls("concrete_spun"))
    w, d, h = 0.18, 0.13, 1.55
    b.box(-w, 0.1, -d, w, h, d, ls("enclosure_grey"))
    b.box(-w - 0.02, h, -d - 0.025, w + 0.02, h + 0.05, d + 0.02, ls("enclosure_grey"))
    # Meter: socket ring, meter base, glass, the lit display on the glass front.
    mc = np.array([0, 1.22, -d])
    cyl(b, mc, mc + [0, 0, -0.03], 0.11, ls("aluminium"), sides=20)
    cyl(b, mc + [0, 0, -0.03], mc + [0, 0, -0.07], 0.095, ls("ground_molding"), sides=20)
    cyl(b, mc + [0, 0, -0.07], mc + [0, 0, -0.17], 0.088, ls("meter_glass"), sides=20)
    from PIL import Image as _I, ImageDraw as _D
    lcd = _I.new("RGBA", (220, 90), (138, 186, 140, 255))
    dr = _D.Draw(lcd)
    dr.text((110, 30), "004127 kWh", font=TY._font(26), fill=(28, 40, 30), anchor="mm")
    dr.text((110, 66), "240 V   38 A", font=TY._font(20), fill=(28, 40, 30), anchor="mm")
    key = ATLAS.add("lcd:meter", lcd)
    face_quad(b, mc + [0, 0.012, -0.172], "-z", 0.09, 0.037, ATLAS.mat(key), lst=b.lit)
    # Lower door: panel, hinges on the left, handle and hasp, the engraved tag.
    b.box(-w + 0.03, 0.25, -d - 0.006, w - 0.03, 0.98, -d, ls("enclosure_grey"))
    for y in (0.35, 0.88):
        cyl(b, (-w + 0.035, y - 0.04, -d - 0.012), (-w + 0.035, y + 0.04, -d - 0.012), 0.008, ls("galv"), sides=8)
    b.box(w - 0.08, 0.58, -d - 0.03, w - 0.055, 0.7, -d - 0.006, ls("galv"))
    b.box(w - 0.075, 0.5, -d - 0.02, w - 0.06, 0.55, -d - 0.006, ls("galv"))
    tag(b, (0, 1.04, -d), "-z", f"200 A  120/240 V  {number}", 0.24, 0.035)
    for sx in (-1, 1):                                                                    # louvred vents on the sides
        for i in range(5):
            y = 0.5 + i * 0.045
            x0, x1 = sorted((sx * w, sx * (w + 0.012)))
            b.box(x0, y, -0.07, x1, y + 0.02, 0.07, ls("enclosure_grey"))
    # The service mast: rigid conduit up through the cap, a kicker brace, the clevis and spool, the weatherhead.
    m0, m1 = 0.04, 3.95
    cyl(b, (0, h + 0.05, 0.04), (0, m1, 0.04), 0.032, ls("galv"), sides=12)
    cyl(b, (0, h + 0.05, 0.04), (0, h + 0.09, 0.04), 0.06, ls("galv"), sides=12)                     # hub
    bar(b, (0, 2.6, 0.075), (0, h + 0.06, d + 0.0), 0.03, ls("galv"), h=0.012)
    ys = 3.55
    b.box(-0.03, ys - 0.06, 0.07, 0.03, ys + 0.06, 0.18, ls("galv"))
    cyl(b, (0, ys - 0.045, 0.15), (0, ys + 0.045, 0.15), 0.04, ls("porcelain_grey"), sides=12)
    wh = np.array([0, m1, 0.04])
    cyl(b, wh, wh + [0, 0.05, 0], 0.045, ls("galv"), sides=12)
    b.box(*_rnd(wh + [-0.07, 0.05, -0.07]), *_rnd(wh + [0.07, 0.1, 0.11]), ls("galv"))
    drop = np.array([0, ys, 0.2])
    for k in range(3):
        o = (k - 1) * 0.03
        path(b, [wh + [o, 0.04, 0.09], wh + [o, -0.05, 0.16], drop + [o, 0.12 + 0.02 * k, 0.05], drop + [o, 0.02, 0.04]], 0.008,
             ls("ground_molding"))
    # Load conduit out of the side into the ground; ground rod and its clamp, the conductor into the pedestal.
    path(b, [(w, 0.3, 0.0), (w + 0.12, 0.3, 0.0), (w + 0.2, 0.18, 0.0), (w + 0.22, -0.2, 0.0)], 0.03, ls("ground_molding"))
    gr = np.array([-0.32, 0, -0.28])
    cyl(b, gr + [0, -0.3, 0], gr + [0, 0.18, 0], 0.008, ls("copper_wire"), sides=8)
    cyl(b, gr + [0, 0.12, 0], gr + [0, 0.16, 0], 0.02, ls("copper_wire"), sides=8)
    path(b, [gr + [0.02, 0.14, 0], (-w - 0.06, 0.16, -0.08), (-w, 0.2, -0.05)], 0.004, ls("copper_wire"))
    b.attach = [list(_rnd(drop))]
    return b



def pole_riser(b, side, tops):
    """On a wooden pole feeding a Pad-Mount Transformer (real basis: a three-phase underground riser): a fused cutout
    for each phase on an alley bracket, leads down to three cable terminations (stress-cone, silicone sheds) on a
    bracket below, an arrester at each termination, the three cables down the pole on stand-off brackets into a
    galvanized U-guard at 3 m and into the ground. side: +-1 (frame x). Returns the cables' points at the ground."""
    s = side
    zs = (-0.24, 0.0, 0.24)
    yc, yt = 10.85, 10.0
    rc, rt = pole_radius(yc), pole_radius(yt)
    # Alley bracket with the three cutouts, leads from the phases.
    b.box(*_rnd([min(s * rc, s * (rc + 0.39)), yc - 0.03, -0.035]), *_rnd([max(s * rc, s * (rc + 0.39)), yc + 0.03, 0.035]), ls("galv"))
    b.box(*_rnd([min(s * (rc + 0.36), s * (rc + 0.42)), yc - 0.036, -0.34]), *_rnd([max(s * (rc + 0.36), s * (rc + 0.42)), yc + 0.036, 0.34]), ls("galv"))
    through_bolt(b, (0, yc, 0), "x", 2 * rc + 0.1)
    ends = []
    for k, z in enumerate(zs):
        base = np.array([s * (rc + 0.39), yc, z])
        ct0, ct1 = base + [s * 0.02, -0.25, 0], base + [s * 0.08, 0.25, 0]
        cyl(b, base + [s * 0.035, -0.14, 0.0], base + [s * 0.065, 0.14, 0.0], 0.034, ls("porcelain_grey"), sides=10)
        cyl(b, ct0 + [s * 0.07, 0, 0], ct1 + [s * 0.07, 0, 0], 0.02, ls("creosote_butt"), sides=8)              # fuse tube
        for p in (ct0, ct1):
            b.box(*_rnd(p + [-0.03, -0.025, -0.035]), *_rnd(p + [0.03, 0.025, 0.035]), ls("galv"))
        t = np.array(tops[k], float)
        path(b, [t + [0, 0.03, 0.4 if z >= 0 else -0.4], t + [s * 0.2, -0.25, 0.4 if z >= 0 else -0.4], ct1 + [0, 0.05, 0]], 0.006, ls("copper_wire"))
        ends.append(ct0)
    # Termination bracket, terminations, arresters; leads from the cutouts.
    b.box(*_rnd([min(s * rt, s * (rt + 0.27)), yt - 0.04, -0.035]), *_rnd([max(s * rt, s * (rt + 0.27)), yt + 0.04, 0.035]), ls("galv"))
    b.box(*_rnd([min(s * (rt + 0.24), s * (rt + 0.3)), yt - 0.046, -0.4]), *_rnd([max(s * (rt + 0.24), s * (rt + 0.3)), yt + 0.046, 0.4]), ls("galv"))
    through_bolt(b, (0, yt, 0), "x", 2 * rt + 0.1)
    cables = []
    for k, z in enumerate(zs):
        tb = np.array([s * (rt + 0.27), yt + 0.04, z])
        cyl(b, tb, tb + [0, 0.42, 0], 0.035, ls("polymer"), sides=10)
        for i in range(5):
            y = 0.1 + i * 0.065
            cyl(b, tb + [0, y, 0], tb + [0, y + 0.012, 0], 0.07 if i % 2 == 0 else 0.056, ls("polymer"), sides=12)
        cyl(b, tb + [0, 0.42, 0], tb + [0, 0.48, 0], 0.014, ls("aluminium"), sides=6)                    # lug
        path(b, [ends[k] + [0, -0.03, 0], ends[k] + [0, -0.2, 0], tb + [0, 0.5, 0]], 0.006, ls("copper_wire"))
        cyl(b, tb - [0, 0.0, 0], tb - [0, 0.3, 0], 0.03, ls("rubber_black"), sides=10)                    # stress cone
        cables.append(tb - [0, 0.3, 0])
    for k, z in enumerate(zs[::2]):                                                         # arresters on the outer two
        ar = np.array([s * (rt + 0.27), yt + 0.04, z * 1.55])
        cyl(b, ar, ar + [0, 0.3, 0], 0.028, ls("polymer"), sides=10)
        for i in range(4):
            cyl(b, ar + [0, 0.06 + i * 0.06, 0], ar + [0, 0.07 + i * 0.06, 0], 0.048, ls("polymer"), sides=12)
        path(b, [ar + [0, 0.3, 0], cables[0 if z < 0 else 2] + [0, 0.78, 0]], 0.004, ls("copper_wire"))
    # The three cables down the pole on stand-offs into the U-guard, into the ground.
    yg = 3.0
    xs = lambda y: s * (pole_radius(y) + 0.07)
    for k, z in enumerate(zs):
        zz = (k - 1) * 0.035
        pts = [cables[k], cables[k] + [0, -0.4, 0], (xs(yt - 1.2), yt - 1.2, zz), (xs(yg + 0.3), yg + 0.3, zz), (xs(-0.3), -0.3, zz)]
        path(b, pts, 0.022, ls("rubber_black"), sides=8)
    for y in np.arange(yg + 0.8, yt - 1.2, 1.4):
        x0, x1 = sorted((s * (pole_radius(y) - 0.005), s * (pole_radius(y) + 0.12)))
        b.box(x0, y - 0.02, -0.07, x1, y + 0.02, 0.07, ls("galv"))
    gx0, gx1 = sorted((s * (pole_radius(1.5) - 0.0), s * (pole_radius(1.5) + 0.13)))
    for zz in (-0.075, 0.075):
        b.box(gx0, -0.2, zz - 0.006, gx1, yg, zz + 0.006, ls("galv"))
    gx0, gx1 = sorted((s * (pole_radius(1.5) + 0.124), s * (pole_radius(1.5) + 0.136)))
    b.box(gx0, -0.19, -0.075, gx1, yg - 0.01, 0.075, ls("galv"))
    for y in (0.4, 1.5, 2.6):
        through_bolt(b, (s * (pole_radius(y) + 0.13), y, 0), "x", 0.06)
    return [np.array([xs(-0.3), -0.3, (k - 1) * 0.035]) for k in range(3)]



def _gauge(b, c, lo, hi, label, unit, value, red=None):
    """A small round gauge on a -z face at c: bezel, printed face, needle at value."""
    c = np.array(c, float)
    cyl(b, c, c + [0, 0, -0.03], 0.055, ls("galv"), sides=16)
    key = ATLAS.add(f"dial:{label}:{lo}:{hi}", TY.dial_img(lo, hi, label, unit, red=red, r_m=0.045))
    face_quad(b, c + [0, 0, -0.031], "-z", 0.09, 0.09, ATLAS.mat(key))
    t = math.radians(225 - 270 * (value - lo) / (hi - lo))
    d = np.array([-math.cos(t), math.sin(t), 0])     # (mirrored frame: the viewer's right is -x on a -z face)
    bar(b, c + [0, 0, -0.034] - d * 0.006, c + [0, 0, -0.034] + d * 0.036, 0.004, ls("bolt_head"), h=0.002)


def pad_mount_transformer(number="PMT-4471", open_doors=False):
    """Box tier 2, the Pad-Mount Transformer (real basis: a three-phase, dead-front, loop-feed 500 kVA pad-mount,
    12.47 kV to 480Y/277 V, in pad-mount green): the tank with radiator fins at the back and sides and lifting lugs, the
    front cabinet with two doors (the LV door's handle, penta-head bolt and padlock hasp; the HV door opens only after
    it), a stencilled warning and engraved tag. Inside: on the HV side three load-break elbows on their bushing wells
    with hot-stick eyes, Bay-O-Net fuse handles above, fault indicators on the cables, the tap changer, the ground bus;
    on the LV side the X1-X3 and X0 bushings with spade terminals and the cables down, oil-level, temperature and
    pressure gauges, the pressure relief valve, drain valve and the nameplate. Fed by a buried cable from a riser pole."""
    b = Board(4, 2702, face=0.0)
    b.live = []
    G = ls("padmount_green")
    b.box(-1.0, 0.0, -0.95, 1.0, 0.15, 0.95, ls("concrete_spun"))
    b.box(-0.85, 0.15, 0.05, 0.85, 1.3, 0.8, G)                                                   # tank
    b.box(-0.86, 1.3, 0.04, 0.86, 1.33, 0.81, G)                                                  # cover
    for x in (-0.78, 0.78):
        for z in (0.12, 0.73):
            b.box(x - 0.02, 1.33, z - 0.05, x + 0.02, 1.43, z + 0.05, G)                              # lifting lugs
    for k in range(13):                                                                           # radiator fins
        x = -0.72 + k * 0.12
        b.box(x - 0.006, 0.3, 0.8, x + 0.006, 1.2, 0.92, G)
    for sx in (-1, 1):
        for k in range(7):
            z = 0.15 + k * 0.09
            x0, x1 = sorted((sx * 0.85, sx * 0.97))
            b.box(x0, 0.3, z - 0.006, x1, 1.2, z + 0.006, G)
    # The cabinet: sides, floor sill, roof; the back of the compartment is the tank's front wall.
    b.box(-0.85, 0.15, -0.62, -0.83, 1.38, 0.05, G)
    b.box(0.83, 0.15, -0.62, 0.85, 1.38, 0.05, G)
    b.box(-0.87, 1.38, -0.645, 0.87, 1.42, 0.06, G)
    b.box(-0.83, 1.325, 0.03, 0.83, 1.3795, 0.039, G)                                             # header above the tank wall
    b.box(-0.83, 0.15, -0.62, 0.83, 0.2, -0.6, G)
    b.box(-0.006, 0.2, -0.6, 0.006, 1.38, 0.05, ls("galv_dark"))                                   # barrier between HV and LV
    # Doors.
    def door(x_hinge, x_free, opened):
        s_ = 1 if x_free > x_hinge else -1
        if not opened:
            x0, x1 = sorted((x_hinge, x_free))
            b.box(x0, 0.2, -0.634, x1, 1.36, -0.62, G)
            for y in (0.4, 1.15):
                cyl(b, (x_hinge - s_ * 0.0, y - 0.05, -0.64), (x_hinge, y + 0.05, -0.64), 0.012, ls("galv_dark"), sides=8)
            return "closed"
        z0, z1 = -0.62 - abs(x_free - x_hinge), -0.62
        x0, x1 = sorted((x_hinge - s_ * 0.016, x_hinge - s_ * 0.002))
        b.box(x0, 0.2, z0, x1, 1.36, z1 - 0.01, G)
        for y in (0.4, 1.15):
            cyl(b, (x_hinge, y - 0.05, -0.636), (x_hinge, y + 0.05, -0.636), 0.012, ls("galv_dark"), sides=8)
        return "open"
    door(-0.845, -0.004, open_doors)
    door(0.845, 0.004, open_doors)
    if not open_doors:
        b.box(0.62, 0.66, -0.66, 0.66, 0.9, -0.634, ls("galv"))                                    # handle
        cyl(b, (0.64, 0.5, -0.634), (0.64, 0.5, -0.66), 0.02, ls("galv"), sides=5)                    # penta-head bolt
        b.box(0.7, 0.62, -0.65, 0.73, 0.68, -0.634, ls("galv"))                                    # padlock hasp
        im = TY.stencil_img("DANGER  HIGH VOLTAGE  KEEP OUT", 0.05, colour=(232, 196, 40))
        key = ATLAS.add("stencil:pmt:danger", im)
        face_quad(b, (-0.42, 1.12, -0.635), "-z", min(0.7, im.width / TY.PXB), 0.05, ATLAS.mat(key))
        tag(b, (0.42, 1.2, -0.634), "-z", f"{number}  500 kVA", 0.24, 0.04)
    # Inside, HV side: bushing wells, elbows with hot-stick eyes, fuses, fault indicators, tap changer, ground bus.
    for x in (-0.62, -0.42, -0.22):
        cyl(b, (x, 0.95, 0.05), (x, 0.95, -0.04), 0.05, ls("porcelain_brown"), sides=12)              # bushing well
        cyl(b, (x, 0.95, -0.04), (x, 0.95, -0.2), 0.045, ls("rubber_black"), sides=12)
        cyl(b, (x, 0.995, -0.2), (x, 0.42, -0.2), 0.04, ls("rubber_black"), sides=12)
        cyl(b, (x, 0.42, -0.2), (x, 0.2, -0.2), 0.026, ls("rubber_black"), sides=10)                  # cable into the pad
        ring(b, (x, 0.95, -0.26), "z", 0.026, 0.008, ls("galv"), 10)                                  # hot-stick eye
        cyl(b, (x, 0.95, -0.2), (x, 0.95, -0.235), 0.012, ls("galv"), sides=6)
        cyl(b, (x, 1.18, 0.05), (x, 1.18, -0.08), 0.03, ls("galv_dark"), sides=10)                    # Bay-O-Net holder
        ring(b, (x, 1.18, -0.1), "z", 0.025, 0.008, ls("aviation_red"), 10)
        cyl(b, (x, 0.62, -0.2), (x, 0.7, -0.2), 0.05, ls("polymer"), sides=10)                         # fault indicator
        path(b, [(x, 0.5, -0.24), (x, 0.3, -0.24), (x, 0.27, -0.06)], 0.004, ls("copper_wire"))        # elbow ground lead
    b.box(-0.8, 0.26, -0.07, -0.05, 0.29, -0.05, ls("copper_wire"))                                  # ground bus
    b.box(-0.78, 1.22, 0.03, -0.7, 1.32, 0.05, ls("galv_dark"))
    cyl(b, (-0.74, 1.27, 0.03), (-0.74, 1.27, -0.04), 0.012, ls("galv"), sides=6)
    b.box(-0.77, 1.255, -0.05, -0.71, 1.285, -0.035, ls("aviation_red"))                             # tap changer
    # LV side: bushings, spades, cables; gauges, relief and drain valves, nameplate.
    for x, nm in ((0.2, "X1"), (0.38, "X2"), (0.56, "X3"), (0.74, "X0")):
        y = 0.95 if nm != "X0" else 0.82
        cyl(b, (x, y, 0.05), (x, y, -0.1), 0.04, ls("porcelain_brown"), sides=12)
        cyl(b, (x, y, -0.1), (x, y, -0.13), 0.016, ls("copper_wire"), sides=8)
        b.box(x - 0.006, y - 0.12, -0.21, x + 0.006, y + 0.05, -0.13, ls("copper_wire"))              # spade
        cyl(b, (x, y - 0.12, -0.17), (x, 0.2, -0.17), 0.03, ls("rubber_black"), sides=10)
    path(b, [(0.74, 0.7, -0.17), (0.74, 0.32, -0.1), (0.1, 0.28, -0.06), (-0.05, 0.275, -0.06)], 0.005, ls("copper_wire"))
    _gauge(b, (0.28, 1.22, 0.05), -20, 120, "OIL TEMP", "C", 64, red=105)
    _gauge(b, (0.5, 1.22, 0.05), -10, 10, "PRESS", "PSI", 1.5)
    _gauge(b, (0.72, 1.22, 0.05), 0, 2, "LEVEL", "", 1.0)
    cyl(b, (0.12, 1.24, 0.05), (0.12, 1.24, -0.04), 0.02, ls("galv"), sides=8)                       # relief valve
    ring(b, (0.12, 1.24, -0.05), "z", 0.025, 0.007, ls("galv"), 10)
    cyl(b, (0.12, 0.4, 0.05), (0.12, 0.4, -0.05), 0.025, ls("galv"), sides=8)                        # drain valve
    b.box(0.1, 0.38, -0.08, 0.14, 0.42, -0.05, ls("galv"))
    tag(b, (0.45, 1.08, 0.05), "-z", "12470GY/7200 - 480Y/277 V  Z 5.6%", 0.3, 0.04)
    # Ground rod at the pad's corner, its conductor up into the cabinet.
    cyl(b, (-0.92, -0.3, -0.88), (-0.92, 0.2, -0.88), 0.008, ls("copper_wire"), sides=8)
    path(b, [(-0.92, 0.18, -0.88), (-0.84, 0.16, -0.7), (-0.8, 0.27, -0.06)], 0.004, ls("copper_wire"))
    b.attach = [[0.0, 0.0, 0.0]]
    return b



# ---------------------------------------------------------------- box tier 3: the unit substation

def chain_link_img(w_m=1.0, h_m=2.1):
    """Galvanized chain-link fabric, 50 mm diamonds, on transparency."""
    W, H = int(w_m * TY.PXB / 2), int(h_m * TY.PXB / 2)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    p = int(0.05 * TY.PXB / 2)
    for k in range(-H // p - 2, W // p + H // p + 2):
        x0 = k * p
        d.line([(x0, 0), (x0 + H, H)], fill=(176, 182, 186, 255), width=2)
        d.line([(x0, 0), (x0 - H, H)], fill=(150, 156, 160, 255), width=2)
    return im


def fence_run(b, a, c, gap=None, h=2.1):
    """A chain-link fence from a to c (ground points): line posts every 2 m, top rail, the fabric in 1 m panels (both
    faces), barbed-wire arms leaning out with three strands. gap: (t0, t1) along the run left open."""
    a, c = np.array(a, float), np.array(c, float)
    L = np.linalg.norm(c - a)
    u = (c - a) / L
    n = np.array([u[2], 0, -u[0]])                     # outward (runs go clockwise seen from above)
    key = ATLAS.add("chain_link", chain_link_img())
    for i in range(int(round(L))):
        t0, t1 = i, i + 1
        if gap and t0 >= gap[0] - 1e-6 and t1 <= gap[1] + 1e-6:
            continue
        p0, p1 = a + u * t0, a + u * t1
        for side in (1, -1):
            o = n * 0.004 * side
            tl, bl, br, tr = (p0 + o + [0, h, 0], p0 + o + [0, 0.05, 0], p1 + o + [0, 0.05, 0], p1 + o + [0, h, 0]) if side > 0 else \
                             (p1 + o + [0, h, 0], p1 + o + [0, 0.05, 0], p0 + o + [0, 0.05, 0], p0 + o + [0, h, 0])
            b.quad(tuple(_rnd(tl)), tuple(_rnd(bl)), tuple(_rnd(br)), tuple(_rnd(tr)), ATLAS.mat(key))
    for i in range(0, int(round(L)) + 1, 2):
        p = a + u * i
        if gap and gap[0] + 0.01 < i < gap[1] - 0.01:
            continue
        cyl(b, p + [0, -0.3, 0], p + [0, h + 0.05, 0], 0.03, ls("galv"), sides=10)
        cyl(b, p + [0, h + 0.05, 0], p + [0, h + 0.08, 0], 0.036, ls("galv"), sides=10)
        arm = p + [0, h + 0.08, 0] + n * 0.0
        rod(b, arm, arm + n * 0.3 + [0, 0.32, 0], 0.012, ls("galv"), 6)
    segs = [(0, L)] if not gap else [(0, gap[0]), (gap[1], L)]
    for s0, s1 in segs:
        if s1 - s0 < 0.05:
            continue
        rod(b, a + u * s0 + [0, h, 0] - n * 0.035, a + u * s1 + [0, h, 0] - n * 0.035, 0.02, ls("galv"), 8)
        for k in range(3):
            off = n * (0.1 + 0.09 * k) + [0, h + 0.19 + 0.1 * k, 0]
            rod(b, a + u * s0 + off, a + u * s1 + off, 0.004, ls("galv_dark"), 4)


def post_insulator(b, base, h=1.1, r=0.05):
    """A polymer station post insulator standing on base: flanges, core, alternating sheds."""
    base = np.array(base, float)
    flange(b, base + [0, 0.02, 0], "y", r + 0.04, 0.04, ls("galv"), 6)
    cyl(b, base + [0, 0.04, 0], base + [0, h - 0.04, 0], r * 0.6, ls("polymer"), sides=10)
    for i in range(int((h - 0.2) / 0.07)):
        y = 0.12 + i * 0.07
        cyl(b, base + [0, y, 0], base + [0, y + 0.012, 0], r + (0.04 if i % 2 == 0 else 0.022), ls("polymer"), sides=14)
    flange(b, base + [0, h - 0.02, 0], "y", r + 0.03, 0.04, ls("galv"), 6)
    return base + [0, h, 0]


def bushing_up(b, base, d, L, r=0.05):
    """A polymer bushing from base along d (unit), sheds, terminal stud; returns the stud's tip."""
    base, d = np.array(base, float), np.array(d, float) / np.linalg.norm(d)
    cyl(b, base, base + d * 0.12, r + 0.035, ls("galv"), sides=12)
    cyl(b, base + d * 0.12, base + d * (L - 0.06), r * 0.6, ls("polymer"), sides=10)
    for i in range(int((L - 0.3) / 0.07)):
        c = base + d * (0.2 + i * 0.07)
        cyl(b, c, c + d * 0.012, r + (0.04 if i % 2 == 0 else 0.022), ls("polymer"), sides=14)
    cyl(b, base + d * (L - 0.06), base + d * L, 0.03, ls("aluminium"), sides=8)
    cyl(b, base + d * L, base + d * (L + 0.08), 0.012, ls("copper_wire"), sides=6)
    return base + d * (L + 0.08)


US = dict(phase=(-2.2, 0.0, 2.2), gantry_z=3.8, col_x=3.6, beam_y=9.0, ds_z=2.2, bk_z=0.6, tx_z=-2.0)


def unit_substation(number="SUB 7  UNIT SUBSTATION", cryo=True, dirs=None, sc_dirs=None):
    """Box tier 3, the Unit Substation (real basis: a compact 138-13.8 kV distribution substation in a 10 x 10 m fenced
    yard): crushed-stone yard, chain-link fence with barbed wire and a double gate; a lattice H-frame dead-end gantry at
    the back where the incoming lines strain off on polymer insulators; a gang-operated three-pole disconnect with
    line-side surge arresters; a three-tank dead-tank breaker; a 20 MVA power transformer with radiators, fans and
    conservator; outdoor metal-clad switchgear on the left; the terminal cabinet in the front fence line where the mod's
    cables leave and nitrogen is piped in; with superconducting lines, two cryogenic termination vessels; yard light,
    ground risers, danger flags."""
    T = US
    b = Board(10, 2801, face=0.0)
    b.live = []
    TY._faces()
    if "danger_sub" not in TY.FACES:
        TY.FACES["danger_sub"] = ATLAS.add("flag:danger_sub", flag_img("DANGER  HIGH VOLTAGE  KEEP OUT", (196, 30, 30), (250, 250, 250), 0.6, 0.2))
    b.box(-5.0, -0.1, -5.0, 5.0, 0.05, 5.0, ls("gravel"))
    # Fence, clockwise from above (outward normals), the gate gap at the front left, the cabinet gap at the centre.
    fence_run(b, (-5, 0, -5), (-5, 0, 5))
    fence_run(b, (-5, 0, 5), (5, 0, 5))
    fence_run(b, (5, 0, 5), (5, 0, -5))
    fence_run(b, (5, 0, -5), (0.6, 0, -5))
    fence_run(b, (-0.6, 0, -5), (-1.5, 0, -5))
    for x in (-1.5, -4.5):                                                               # gate posts
        cyl(b, (x, -0.3, -5), (x, 2.3, -5), 0.05, ls("galv"), sides=12)
        cyl(b, (x, 2.3, -5), (x, 2.34, -5), 0.058, ls("galv"), sides=12)
    key = ATLAS.add("chain_link", chain_link_img())
    for x0, x1 in ((-4.45, -3.02), (-2.98, -1.55)):                                       # the two gate leaves
        for y in (0.12, 1.1, 2.08):
            rod(b, (x0, y, -5.0), (x1, y, -5.0), 0.02, ls("galv"), 8)
        for x in (x0, x1):
            rod(b, (x, 0.12, -5.0), (x, 2.08, -5.0), 0.02, ls("galv"), 8)
        for side in (1, -1):
            zz = -5.0 - 0.004 * side
            q = [(x0, 2.08, zz), (x0, 0.12, zz), (x1, 0.12, zz), (x1, 2.08, zz)] if side < 0 else [(x1, 2.08, zz), (x1, 0.12, zz), (x0, 0.12, zz), (x0, 2.08, zz)]
            b.quad(*q, ATLAS.mat(key))
    b.box(-3.04, 1.0, -5.06, -2.96, 1.12, -5.025, ls("galv"))                              # latch
    b.box(-3.03, 0.9, -5.08, -2.97, 0.98, -5.065, ls("bolt_head"))                          # padlock
    face_quad(b, (-3.0, 1.55, -5.03), "-z", 0.6, 0.2, ATLAS.mat(TY.FACES["danger_sub"]))
    b.box(-3.31, 1.44, -5.028, -2.69, 1.66, -5.022, ls("galv"))
    # The gantry: two lattice columns and a box-truss beam, the dead-end strings out over the back fence.
    gz, cx, by = T["gantry_z"], T["col_x"], T["beam_y"]
    for sx in (-1, 1):
        x = sx * cx
        b.box(x - 0.5, -0.4, gz - 0.5, x + 0.5, 0.25, gz + 0.5, ls("concrete_spun"))
        P = [(x - 0.3, 0.25, gz - 0.3), (x + 0.3, 0.25, gz - 0.3), (x + 0.3, 0.25, gz + 0.3), (x - 0.3, 0.25, gz + 0.3)]
        Q = [(x - 0.22, by, gz - 0.22), (x + 0.22, by, gz - 0.22), (x + 0.22, by, gz + 0.22), (x - 0.22, by, gz + 0.22)]
        section_truss(b, P, Q, list(np.linspace(0, 1, 13)), leg_w=0.1, brace_w=0.05)
        path(b, [(x + 0.32, 0.6, gz - 0.32), (x + 0.36, 0.3, gz - 0.36), (x + 0.4, 0.04, gz - 0.6)], 0.008, ls("copper_wire"))
    sec = lambda xx: [(xx, by, gz - 0.22), (xx, by, gz + 0.22), (xx, by + 0.7, gz + 0.22), (xx, by + 0.7, gz - 0.22)]
    section_truss(b, sec(-cx - 0.24), sec(cx + 0.24), list(np.linspace(0, 1, 13)), leg_w=0.1, brace_w=0.05, angle_legs=False)
    for sx in (-1, 1):                                                                      # shield-wire peaks
        x = sx * cx
        for dz, w_ in ((-0.2, 0.06), (0.2, 0.052)):
            bar(b, (x, by + 0.75, gz + dz), (x, by + 2.2, gz), w_, ls("galv"))
    b.shield = [list(_rnd([sx * cx, by + 2.25, gz])) for sx in (-1, 1)]
    attach = []
    for x in T["phase"]:
        bar(b, (x, by - 0.05, gz - 0.24), (x, by - 0.05, gz + 0.26), 0.06, ls("galv"))
        # one strain assembly per incoming line, turned to its span (the game turns it), jumpered to the disconnect
        for kk, d in enumerate(dirs or [np.array([0, 0, 1.0])]):
            ends, pads = kit_strain(b, (x, by - 0.05, gz + 0.26), d, ins="poly", L=2.2, n=1, ring_r=0.16, sheds=(0.085, 0.065), k_index=kk)
            attach.append(ends[0])
            p = np.array(pads[0])
            jumper = _bezier(p, np.array([x, by - 3.0, gz + 0.6]), np.array([x, 5.32, T["ds_z"] + 0.45]), 14)
            path(b, jumper, 0.014, ls("aluminium"), sides=6)
    b.attach = [list(_rnd(a)) for a in attach]
    # Disconnect: pipe columns and a beam, two posts and a blade per phase, interphase rod, crank and handle.
    dz = T["ds_z"]
    for sx in (-1, 1):
        x = sx * 2.95
        b.box(x - 0.3, -0.3, dz - 0.3, x + 0.3, 0.2, dz + 0.3, ls("concrete_spun"))
        cyl(b, (x, 0.2, dz), (x, 4.0, dz), 0.11, ls("galv"), sides=12)
        flange(b, (x, 0.22, dz), "y", 0.2, 0.04, ls("galv"), 6)
    b.box(-3.1, 4.0, dz - 0.6, 3.1, 4.12, dz + 0.6, ls("galv"))
    for x in T["phase"]:
        t1 = post_insulator(b, (x, 4.12, dz + 0.45), 1.1)
        t2 = post_insulator(b, (x, 4.12, dz - 0.45), 1.1)
        cyl(b, (x, 4.12, dz - 0.45), (x, 4.03, dz - 0.45), 0.04, ls("galv_dark"), sides=8)        # rotating base
        for t in (t1, t2):
            b.box(*_rnd(t + [-0.05, 0, -0.07]), *_rnd(t + [0.05, 0.08, 0.07]), ls("aluminium"))
        cyl(b, t2 + [0, 0.12, 0.04], t1 + [0, 0.12, -0.04], 0.025, ls("aluminium"), sides=8)       # blade
        b.box(*_rnd(t1 + [-0.06, 0.08, -0.02]), *_rnd(t1 + [0.06, 0.17, 0.08]), ls("copper_wire"))  # jaw
        # line-side arrester on a bracket off the beam's back edge
        ab = np.array([x + 0.55, 4.12, dz + 0.45])
        b.box(*_rnd(ab + [-0.08, 0.0, -0.08]), *_rnd(ab + [0.08, 0.02, 0.08]), ls("galv"))
        at = post_insulator(b, ab + [0, 0.02, 0], 0.85, 0.045)
        path(b, [at + [0, 0.02, 0], at + [-0.25, 0.25, 0], t1 + [0.0, 0.17, 0.03]], 0.006, ls("aluminium"))
        path(b, [ab + [0.06, -0.02, 0.06], ab + [0.06, -0.12, 0.62], (x + 0.61, 0.05, dz + 1.2)], 0.004, ls("copper_wire"))
    rod(b, (-2.2, 3.95, dz - 0.45), (2.95, 3.95, dz - 0.45), 0.02, ls("galv"), 8)               # interphase rod
    for x in T["phase"]:
        b.box(x - 0.03, 3.93, dz - 0.5, x + 0.03, 4.0, dz - 0.4, ls("galv"))
    rod(b, (2.95 + 0.13, 3.95, dz - 0.45), (2.95 + 0.13, 1.3, dz - 0.45), 0.02, ls("galv"), 8)
    b.box(2.95 + 0.08, 1.05, dz - 0.6, 2.95 + 0.3, 1.3, dz - 0.3, ls("galv_dark"))              # crank box
    rod(b, (2.95 + 0.3, 1.18, dz - 0.45), (2.95 + 0.48, 1.18, dz - 0.45), 0.015, ls("galv"), 6)
    b.box(2.95 + 0.47, 1.1, dz - 0.5, 2.95 + 0.52, 1.26, dz - 0.4, ls("aviation_red"))
    tag(b, (2.95 + 0.19, 1.36, dz - 0.6), "-z", "89-1", 0.12, 0.035)
    # Breaker: three tanks on a frame, two bushings each, the mechanism cabinet.
    bz = T["bk_z"]
    for x in (-2.6, 2.6):
        b.box(x - 0.08, 0.05, bz - 0.7, x + 0.08, 1.35, bz - 0.55, ls("galv"))
        b.box(x - 0.08, 0.05, bz + 0.55, x + 0.08, 1.35, bz + 0.7, ls("galv"))
    for zz in (bz - 0.62, bz + 0.62):
        b.box(-2.68, 1.35, zz - 0.07, 2.68, 1.45, zz + 0.07, ls("galv"))
    for x in T["phase"]:
        cyl(b, (x, 1.75, bz - 0.6), (x, 1.75, bz + 0.6), 0.3, ls("transformer_grey"), sides=18)
        for zs in (-1, 1):
            cyl(b, (x, 1.75, bz + zs * 0.6), (x, 1.75, bz + zs * 0.64), 0.32, ls("transformer_grey"), sides=18)
        for zz in (bz - 0.62, bz + 0.62):
            b.box(x - 0.2, 1.45, zz - 0.06, x + 0.2, 1.5, zz + 0.06, ls("galv"))
        tips = []
        for zs in (-1, 1):
            tips.append(bushing_up(b, (x, 1.98, bz + zs * 0.3), (0, 1, zs * 0.38), 1.9))
        path(b, [np.array([x, 4.12 + 1.1 + 0.17, dz - 0.45]), np.array([x, 4.4, (dz + tips[1][2]) / 2]), tips[1]], 0.012,
             ls("aluminium"))                                                               # disconnect to breaker
    b.box(2.85, 0.6, bz - 0.5, 3.6, 2.1, bz + 0.5, ls("transformer_grey"))                       # mechanism cabinet
    b.box(2.84, 0.7, bz - 0.4, 2.85, 2.0, bz + 0.4, ls("transformer_grey"))
    b.box(2.82, 1.2, bz + 0.25, 2.84, 1.45, bz + 0.3, ls("galv"))
    tag(b, (2.84, 1.8, bz), "-x", "52-1  145 kV  2000 A", 0.24, 0.04)
    path(b, [(2.6, 1.75, bz), (2.85, 1.75, bz)], 0.03, ls("galv"))
    # Transformer: plinth, tank, cover, HV bushings, radiators and fans, conservator, LV throat to the cable box.
    tz = T["tx_z"]
    b.box(-2.0, -0.2, tz - 1.1, 2.3, 0.3, tz + 1.1, ls("concrete_spun"))
    b.box(-1.4, 0.3, tz - 0.8, 1.4, 2.7, tz + 0.8, ls("transformer_grey"))
    b.box(-1.43, 2.7, tz - 0.83, 1.43, 2.78, tz + 0.83, ls("transformer_grey"))
    hv = []
    for x in (-0.8, 0.0, 0.8):
        hv.append(bushing_up(b, (x, 2.78, tz + 0.45), (0, 1, 0), 1.25))
    for x, h_ in zip(T["phase"], hv):
        src = np.array([x, 1.98, bz - 0.3]) + np.array([0, 1, -0.38]) / np.linalg.norm([0, 1, -0.38]) * 1.98
        path(b, [src, (src + h_) / 2 + [0, 0.45, 0], h_], 0.012, ls("aluminium"))
    for k in range(7):                                                                        # radiators on +x
        z = tz - 0.66 + k * 0.22
        b.box(1.4, 0.55, z - 0.07, 1.95, 2.35, z + 0.07, ls("transformer_grey"))
    for y in (0.6, 2.3):
        cyl(b, (1.41, y, tz - 0.75), (1.41, y, tz + 0.75), 0.06, ls("transformer_grey"), sides=10)
    for z in (tz - 0.45, tz + 0.45):                                                           # fans under the radiators
        cyl(b, (1.68, 0.5, z), (1.68, 0.36, z), 0.22, ls("galv_dark"), sides=16)
    cyl(b, (-1.1, 3.4, tz - 0.6), (-1.1, 3.4, tz + 0.6), 0.25, ls("transformer_grey"), sides=16)   # conservator
    for z in (tz - 0.4, tz + 0.4):
        b.box(-1.16, 2.78, z - 0.04, -1.04, 3.17, z + 0.04, ls("galv"))
    path(b, [(-1.1, 3.16, tz), (-0.9, 2.95, tz), (-0.9, 2.78, tz)], 0.03, ls("transformer_grey"))
    b.box(-0.96, 2.85, tz - 0.08, -0.84, 2.97, tz + 0.08, ls("galv_dark"))                    # Buchholz relay
    b.box(-1.9, 0.9, tz - 0.6, -1.4, 2.3, tz + 0.6, ls("transformer_grey"))                    # LV cable box
    tag(b, (0.6, 1.9, tz - 0.8), "-z", "T1  20 MVA  138-13.8 kV", 0.32, 0.045)
    b.box(-0.3, 0.9, tz - 0.95, 0.3, 1.7, tz - 0.8, ls("transformer_grey"))                    # control cabinet
    _gauge(b, (0.75, 2.3, tz - 0.8), 0, 120, "OIL", "C", 58, red=95)
    _gauge(b, (1.05, 2.3, tz - 0.8), 0, 140, "WINDING", "C", 71, red=110)
    path(b, [(-1.9, 1.0, tz), (-2.4, 0.4, tz), (-2.6, -0.05, tz)], 0.05, ls("rubber_black"))   # LV cables down to the trench
    # Switchgear on the left: an outdoor walk-in enclosure, cubicle doors facing the aisle, relays lit.
    b.box(-4.8, -0.2, -3.3, -3.3, 0.22, 1.9, ls("concrete_spun"))
    b.box(-4.7, 0.22, -3.2, -3.5, 2.45, 1.8, ls("enclosure_grey"))
    b.box(-4.75, 2.45, -3.25, -3.45, 2.52, 1.85, ls("enclosure_grey"))
    for i in range(4):
        z0 = -3.1 + i * 1.22
        b.box(-3.5, 0.3, z0, -3.49, 2.35, z0 + 1.15, ls("enclosure_grey"))
        b.box(-3.49, 1.0, z0 + 0.95, -3.47, 1.25, z0 + 1.0, ls("galv"))
        for j in range(3):
            b.box(-3.49, 1.9 - j * 0.14, z0 + 0.2, -3.485, 1.98 - j * 0.14, z0 + 0.42, ls("lcd"), lst=b.lit)
        tag(b, (-3.49, 2.15, z0 + 0.57), "x", f"52-{11 + i}  13.8 kV", 0.24, 0.035)
    # Terminal cabinet in the front fence line: the mod's cable outlet and the nitrogen inlet on its outer face.
    b.box(-0.6, -0.2, -5.3, 0.6, 0.2, -4.7, ls("concrete_spun"))
    b.box(-0.55, 0.2, -5.25, 0.55, 1.75, -4.75, ls("enclosure_grey"))
    b.box(-0.58, 1.75, -5.28, 0.58, 1.8, -4.72, ls("enclosure_grey"))
    b.box(-0.45, 0.35, -5.26, 0.45, 1.6, -5.25, ls("enclosure_grey"))
    cyl(b, (-0.3, 0.45, -5.25), (-0.3, 0.45, -5.45), 0.07, ls("rubber_black"), sides=12)       # cable outlet
    cyl(b, (0.3, 0.6, -5.25), (0.3, 0.6, -5.4), 0.05, ls("stainless"), sides=12)              # nitrogen inlet
    flange(b, (0.3, 0.6, -5.4), "z", 0.09, 0.03, ls("stainless"), 6)
    tag(b, (0.0, 1.45, -5.26), "-z", "CABLES OUT  /  N2 IN", 0.3, 0.04)
    path(b, [(-0.3, 0.35, -4.75), (-0.3, -0.05, -4.3), (-2.6, -0.05, -2.0)], 0.05, ls("rubber_black"))
    if cryo:
        # Two cryogenic termination vessels on legs, three phase bushings each, the nitrogen header from the cabinet.
        for i, x in enumerate((2.7, 3.9)):
            c = np.array([x, 0.0, -3.9])
            for dx, dz_ in ((-0.3, -0.3), (0.3, -0.3), (0.3, 0.3), (-0.3, 0.3)):
                cyl(b, c + [dx, 0.05, dz_], c + [dx, 0.5, dz_], 0.03, ls("galv"), sides=8)
            cyl(b, c + [0, 0.5, 0], c + [0, 2.1, 0], 0.42, ls("stainless"), sides=20)
            cyl(b, c + [0, 2.1, 0], c + [0, 2.18, 0], 0.46, ls("stainless"), sides=20)
            cyl(b, c + [0, 0.5, 0], c + [0, 0.44, 0], 0.38, ls("stainless"), sides=20)
            for k, dxx in enumerate((-0.22, 0.0, 0.22)):
                bushing_up(b, c + [dxx, 2.18, -0.15], (0, 1, 0), 0.9, r=0.04)
            # the cable-entry sleeve swivels on the vessel to face its line (the game turns it), out of the yard's half
            d = np.array((sc_dirs or [None, None])[i] if sc_dirs and sc_dirs[i] is not None else [0, 0, -1.0], float)
            d = d / np.linalg.norm(d)
            sl = c + [0, 1.6, 0] + d * 0.42
            cyl(b, c + [0, 1.6, 0] + d * 0.3, sl + d * 0.35, 0.13, ls("composite_white"), sides=16)
            cyl(b, sl + d * 0.3, sl + d * 0.35, 0.135, ls("sc_glow"), sides=16, lst=b.lit)
            cyl(b, c + [0, 1.45, 0], c + [0, 1.75, 0], 0.44, ls("galv_dark"), sides=20)           # the swivel collar
            b.attach.append(list(_rnd(sl + d * 0.35)))
            cyl(b, c + [0.25, 2.18, 0.2], c + [0.25, 2.6, 0.2], 0.03, ls("stainless"), sides=8)    # relief stack
            cyl(b, c + [0.25, 2.6, 0.2], c + [0.25, 2.66, 0.2], 0.05, ls("stainless"), sides=8)
            path(b, [c + [-0.42, 0.9, 0], c + [-0.7, 0.9, 0], c + [-0.7, 0.35, 0]], 0.04, ls("cryo_jacket"))
        path(b, [(0.3, 0.6, -4.75), (0.3, 0.35, -4.5), (2.0, 0.35, -4.5), (2.0, 0.35, -3.9), (3.2, 0.35, -3.9)], 0.04, ls("cryo_jacket"))
        tag(b, (3.3, 1.2, -3.9 - 0.42), "-z", "LN2  CRYO TERMINATION", 0.3, 0.04)
    # Yard light and ground risers.
    cyl(b, (4.5, -0.3, 4.5), (4.5, 6.0, 4.5), 0.06, ls("galv"), sides=10)
    rod(b, (4.5, 5.9, 4.5), (3.9, 6.05, 3.9), 0.03, ls("galv"), 6)
    b.box(3.72, 5.93, 3.72, 3.98, 6.05, 3.98, ls("galv_dark"))
    b.box(3.75, 5.92, 3.75, 3.95, 5.93, 3.95, ls("lcd"), lst=b.lit)
    return b



# ---------------------------------------------------------------- box tier 4: the HVDC converter station

def air_core_reactor(b, c, r=0.85, h=1.7):
    """A dry-type air-core smoothing reactor standing on c (the top of its insulators): the wound cylinder in banded
    sections with vertical spacer bars, the spider arms top and bottom, the terminals, a rain hood."""
    c = np.array(c, float)
    for i in range(4):
        y0 = 0.1 + i * (h - 0.2) / 4
        cyl(b, c + [0, y0, 0], c + [0, y0 + (h - 0.2) / 4 - 0.03, 0], r, ls("polymer"), sides=24)
        cyl(b, c + [0, y0 + (h - 0.2) / 4 - 0.03, 0], c + [0, y0 + (h - 0.2) / 4, 0], r + 0.008, ls("galv_dark"), sides=24)
    for k in range(12):
        t = 2 * math.pi * k / 12
        p = c + [math.cos(t) * (r + 0.012), 0, math.sin(t) * (r + 0.012)]
        b.box(*_rnd(p + [-0.012, 0.1, -0.012]), *_rnd(p + [0.012, h - 0.1, 0.012]), ls("polymer"))
    for y in (0.05, h - 0.05):
        for k in range(4):
            t = math.pi / 4 + math.pi / 2 * k
            rod(b, c + [0, y, 0], c + [math.cos(t) * (r + 0.05), y, math.sin(t) * (r + 0.05)], 0.035, ls("aluminium"), 8)
        cyl(b, c + [0, y - 0.05, 0], c + [0, y + 0.05, 0], 0.12, ls("aluminium"), sides=12)
    cyl(b, c + [0, h, 0], c + [0, h + 0.06, 0], r + 0.1, ls("polymer"), sides=24)                      # rain hood
    cyl(b, c + [0, h + 0.06, 0], c + [0, h + 0.12, 0], r * 0.5, ls("polymer"), sides=24)
    return c + [r + 0.05, h - 0.05, 0], c + [r + 0.05, 0.05, 0]


def converter_station(number="HVDC CONVERTER STATION 1", dirs=None):
    """Box tier 4, the HVDC Converter Station (real basis: an LCC bipole converter station, drawn compact): in a
    14 x 14 m fenced yard, a ribbed metal-clad valve hall with the thyristor valves inside (its doors, ventilation
    louvres); for each pole a converter transformer in front, its valve-side bushings through the hall wall and a
    firewall between them, the oil coolers; at the back, the DC wall bushings out to the air-core smoothing reactors on
    post insulators, DC filter capacitor stacks, and the DC line gantry where the two poles strain off; the valve
    cooling dry coolers on the side; the terminal cabinet in the front fence for the mod's cables."""
    b = Board(14, 2901, face=0.0)
    b.live = []
    TY._faces()
    if "danger_dc" not in TY.FACES:
        TY.FACES["danger_dc"] = ATLAS.add("flag:danger_dc", flag_img("DANGER  HIGH VOLTAGE DC  KEEP OUT", (196, 30, 30), (250, 250, 250), 0.6, 0.2))
    b.box(-7.0, -0.1, -7.0, 7.0, 0.05, 7.0, ls("gravel"))
    fence_run(b, (-7, 0, -7), (-7, 0, 7))
    fence_run(b, (-7, 0, 7), (7, 0, 7))
    fence_run(b, (7, 0, 7), (7, 0, -7))
    fence_run(b, (7, 0, -7), (0.6, 0, -7))
    fence_run(b, (-0.6, 0, -7), (-3.5, 0, -7))
    key = ATLAS.add("chain_link", chain_link_img())
    for x in (-3.5, -6.5):
        cyl(b, (x, -0.3, -7), (x, 2.3, -7), 0.05, ls("galv"), sides=12)
    for x0, x1 in ((-6.45, -5.02), (-4.98, -3.55)):
        for y in (0.12, 1.1, 2.08):
            rod(b, (x0, y, -7.0), (x1, y, -7.0), 0.02, ls("galv"), 8)
        for x in (x0, x1):
            rod(b, (x, 0.12, -7.0), (x, 2.08, -7.0), 0.02, ls("galv"), 8)
        for side in (1, -1):
            zz = -7.0 - 0.004 * side
            q = [(x0, 2.08, zz), (x0, 0.12, zz), (x1, 0.12, zz), (x1, 2.08, zz)] if side < 0 else [(x1, 2.08, zz), (x1, 0.12, zz), (x0, 0.12, zz), (x0, 2.08, zz)]
            b.quad(*q, ATLAS.mat(key))
    b.box(-5.04, 1.0, -7.06, -4.96, 1.12, -7.025, ls("galv"))
    face_quad(b, (-5.0, 1.55, -7.03), "-z", 0.6, 0.2, ATLAS.mat(TY.FACES["danger_dc"]))
    b.box(-5.31, 1.44, -7.028, -4.69, 1.66, -7.022, ls("galv"))
    # Valve hall: slab, ribbed cladding, roof, doors, louvres, the name.
    x0, x1, z0, z1, H = -4.0, 4.0, -1.0, 4.0, 7.5
    b.box(x0 - 0.2, -0.2, z0 - 0.2, x1 + 0.2, 0.25, z1 + 0.2, ls("concrete_spun"))
    b.box(x0, 0.25, z0, x1, H, z1, ls("enclosure_grey"))
    b.box(x0 - 0.12, H, z0 - 0.12, x1 + 0.12, H + 0.25, z1 + 0.12, ls("galv_dark"))
    for x in np.arange(x0 + 0.25, x1 - 0.1, 0.5):                                         # ribs, front and back
        for zf, d in ((z0, -1), (z1, 1)):
            zz = sorted((zf, zf + d * 0.04))
            b.box(x - 0.025, 0.3, zz[0], x + 0.025, H - 0.05, zz[1], ls("enclosure_grey"))
    for z in np.arange(z0 + 0.25, z1 - 0.1, 0.5):
        for xf, d in ((x0, -1), (x1, 1)):
            xx = sorted((xf, xf + d * 0.04))
            b.box(xx[0], 0.3, z - 0.025, xx[1], H - 0.05, z + 0.025, ls("enclosure_grey"))
    b.box(x1 + 0.04, 0.25, 0.1, x1 + 0.06, 2.45, 1.1, ls("galv_dark"))                          # personnel door (+x side)
    b.box(x1 + 0.06, 1.15, 0.95, x1 + 0.09, 1.3, 1.0, ls("galv"))
    for i in range(3):                                                                     # louvres high on +x side
        y = 5.6 + i * 0.3
        b.box(x1 + 0.04, y, 2.0, x1 + 0.14, y + 0.12, 3.6, ls("galv_dark"))
    tag(b, (x1 + 0.06, 2.8, 0.6), "x", "VALVE HALL  POLE 1 + / POLE 2 -", 0.5, 0.06)
    # Converter transformers, one per pole, valve-side bushings into the hall; firewall; coolers.
    b.box(-0.2, 0.25, -4.6, 0.2, 4.2, z0, ls("concrete_spun"))                                    # firewall
    poles = ((-1, "POLE 1  +"), (1, "POLE 2  -"))
    for sx, label in poles:
        cx = sx * 2.2
        b.box(cx - 1.5, -0.2, -4.6, cx + 1.5, 0.3, -1.25, ls("concrete_spun"))
        b.box(cx - 1.2, 0.3, -3.6, cx + 1.2, 3.2, -1.6, ls("transformer_grey"))
        b.box(cx - 1.23, 3.2, -3.63, cx + 1.23, 3.28, -1.57, ls("transformer_grey"))
        for k in range(3):                                                                 # valve-side bushings into the wall
            x = cx + (k - 1) * 0.6
            cyl(b, (x, 2.4, -1.6), (x, 2.4, z0 - 0.04), 0.12, ls("polymer"), sides=14)
            for i in range(3):
                zc = -1.55 + i * 0.12
                cyl(b, (x, 2.4, zc), (x, 2.4, zc + 0.015), 0.19, ls("polymer"), sides=16)
            cyl(b, (x, 2.4, z0 - 0.04), (x, 2.4, z0), 0.24, ls("galv"), sides=16)              # wall plate ring
        for k in range(3):                                                                 # line-side bushings on the cover
            x = cx + (k - 1) * 0.6
            bushing_up(b, (x, 3.28, -3.2), (0, 1, 0), 1.3, r=0.05)
        cyl(b, (cx - 0.9, 3.9, -3.4), (cx + 0.9, 3.9, -3.4), 0.25, ls("transformer_grey"), sides=16)   # conservator
        for x in (cx - 0.6, cx + 0.6):
            b.box(x - 0.04, 3.28, -3.44, x + 0.04, 3.67, -3.36, ls("galv"))
        for k in range(4):                                                                 # oil coolers in front
            x = cx - 0.9 + k * 0.6
            b.box(x - 0.25, 0.6, -4.35, x + 0.25, 2.8, -3.75, ls("galv_dark"))
            cyl(b, (x, 2.8, -4.05), (x, 2.86, -4.05), 0.22, ls("galv"), sides=14)
            path(b, [(x, 2.6, -3.75), (x, 2.6, -3.6)], 0.05, ls("transformer_grey"))
        tag(b, (cx, 2.0, -3.6), "-z", f"{label}  CONV TX  1100 kV DC", 0.36, 0.045)
        _gauge(b, (cx + 0.75, 2.8, -3.6), 0, 120, "OIL", "C", 61, red=95)
    # AC side: line-side bushings to the terminal cabinet by cable in the trench (the mod's cables leave there).
    b.box(-0.6, -0.2, -7.3, 0.6, 0.2, -6.7, ls("concrete_spun"))
    b.box(-0.55, 0.2, -7.25, 0.55, 1.75, -6.75, ls("enclosure_grey"))
    b.box(-0.58, 1.75, -7.28, 0.58, 1.8, -6.72, ls("enclosure_grey"))
    cyl(b, (0.0, 0.45, -7.25), (0.0, 0.45, -7.45), 0.08, ls("rubber_black"), sides=12)
    tag(b, (0.0, 1.45, -7.26), "-z", "AC  CABLES OUT", 0.3, 0.04)
    for sx, _ in poles:
        path(b, [(sx * 2.2 - sx * 1.2, 1.0, -2.6), (sx * 0.5, 0.4, -4.8), (sx * 0.2, 0.3, -6.75)], 0.06, ls("rubber_black"))
    # DC yard at the back: wall bushings, smoothing reactors, filter stacks, the line gantry.
    gz, gx, gy = 6.3, 6.2, 13.0
    attach = []
    for sx, label in poles:
        px = sx * 3.3
        wb0 = np.array([sx * 2.2, 5.6, z1])
        tip = bushing_up(b, wb0 + [0, 0, 0.0], (sx * 0.35, 0.2, 1.0), 2.2, r=0.09)               # DC wall bushing
        rc = np.array([px, 0.0, 5.5])
        for k in range(3):
            t = 2 * math.pi * k / 3 + 0.5
            base = rc + [math.cos(t) * 0.55, 0.12, math.sin(t) * 0.55]
            b.box(*_rnd(base + [-0.15, -0.32, -0.15]), *_rnd(base + [0.15, 0.0, 0.15]), ls("concrete_spun"))
            post_insulator(b, base, 2.0, 0.07)
        b.box(*_rnd(rc + [-0.7, 2.12, -0.08]), *_rnd(rc + [0.7, 2.19, 0.08]), ls("galv"))          # bearer on the posts
        top_t, bot_t = air_core_reactor(b, rc + [0, 2.19, 0], 0.85, 1.7)
        path(b, [tip, (tip + top_t) / 2 + [0, 0.6, 0], top_t + [sx * 0.0, 0.0, 0]], 0.025, ls("aluminium"))
        # DC filter: a capacitor stack on an insulated frame beside the reactor.
        fx = sx * 5.3
        for dx, dz_ in ((-0.35, -0.35), (0.35, -0.35), (0.35, 0.35), (-0.35, 0.35)):
            post_insulator(b, (fx + dx, 0.05, 4.4 + dz_), 0.9, 0.05)
        for i in range(8):
            y = 1.0 + i * 0.32
            b.box(fx - 0.42, y, 4.0, fx + 0.42, y + 0.04, 4.8, ls("galv"))
            for j in range(3):
                b.box(fx - 0.36 + j * 0.25, y + 0.04, 4.1, fx - 0.16 + j * 0.25, y + 0.28, 4.7, ls("stainless"))
        tag(b, (fx, 1.0 + 8 * 0.32 + 0.1, 4.0), "-z", f"DC FILTER  {label}", 0.3, 0.04)
        path(b, [bot_t + [0, -0.02, 0], (fx, 1.0 + 8 * 0.32 + 0.3, 4.4)], 0.02, ls("aluminium"))
        # The gantry's dead-end for this pole, its jumper down to the reactor's top terminal.
        for kk, d in enumerate(dirs or [np.array([0, 0, 1.0])]):    # turned to each line's span by the game
            ends, pads = kit_strain(b, (px, gy - 0.12, gz + 0.26), d, ins="poly", L=3.2, n=8, rb=0.72, double=True, gap=0.9,
                                    ring_r=0.45, sheds=(0.16, 0.12), pitch=0.09, core=0.04, k_index=kk)
            attach += ends                                   # the eight-conductor bundle dead-ended
            for p in pads:                                   # the jumpers gathered to the reactor's terminal
                p = np.array(p)
                path(b, _bezier(p, np.array([px + sx * 0.9, gy - 6.0, gz]) + (p - np.mean(pads, axis=0)) * 0.6, top_t + [0, 0.05, 0], 14),
                     0.016, ls("aluminium"))
        bar(b, (px, gy - 0.12, gz - 0.24), (px, gy - 0.12, gz + 0.3), 0.07, ls("galv"))
        b.box(px - 0.04, gy - 0.085, gz - 0.06, px + 0.04, gy - 0.06, gz + 0.06, ls("galv"))
        face_quad(b, (px, gy + 0.35, gz - 0.25), "-z", 0.5, 0.14, ATLAS.mat(ATLAS.add(f"tag:dc:{label}", TY.tag_img(label, 0.5, 0.14))))
    for sx in (-1, 1):
        x = sx * gx
        b.box(x - 0.55, -0.4, gz - 0.55, x + 0.55, 0.25, gz + 0.55, ls("concrete_spun"))
        P = [(x - 0.32, 0.25, gz - 0.32), (x + 0.32, 0.25, gz - 0.32), (x + 0.32, 0.25, gz + 0.32), (x - 0.32, 0.25, gz + 0.32)]
        Q = [(x - 0.22, gy, gz - 0.22), (x + 0.22, gy, gz - 0.22), (x + 0.22, gy, gz + 0.22), (x - 0.22, gy, gz + 0.22)]
        section_truss(b, P, Q, list(np.linspace(0, 1, 16)), leg_w=0.11, brace_w=0.055)
        path(b, [(x + 0.34, 0.6, gz - 0.34), (x + 0.38, 0.3, gz - 0.38), (x + 0.42, 0.04, gz - 0.6)], 0.008, ls("copper_wire"))
        for dz_, w_ in ((-0.2, 0.06), (0.2, 0.052)):
            bar(b, (x, gy + 0.85, gz + dz_), (x, gy + 2.6, gz), w_, ls("galv"))
    sec = lambda xx: [(xx, gy, gz - 0.24), (xx, gy, gz + 0.24), (xx, gy + 0.8, gz + 0.24), (xx, gy + 0.8, gz - 0.24)]
    section_truss(b, sec(-gx - 0.24), sec(gx + 0.24), list(np.linspace(0, 1, 17)), leg_w=0.11, brace_w=0.05, angle_legs=False)
    b.attach = [list(_rnd(a)) for a in attach]
    b.shield = [list(_rnd([sx * gx, gy + 2.65, gz])) for sx in (-1, 1)]
    # Valve cooling dry coolers on the -x side of the hall: a frame of fin coils, fans on top.
    for i in range(2):
        z = 0.2 + i * 1.8
        b.box(-6.3, 0.05, z, -4.6, 0.35, z + 1.6, ls("concrete_spun"))
        b.box(-6.2, 0.35, z + 0.05, -4.7, 1.9, z + 1.55, ls("galv_dark"))
        for zz in (z + 0.45, z + 1.15):
            cyl(b, (-5.45, 1.9, zz), (-5.45, 2.0, zz), 0.32, ls("galv"), sides=18)
            cyl(b, (-5.45, 2.0, zz), (-5.45, 2.04, zz), 0.05, ls("bolt_head"), sides=8)
        path(b, [(-4.7, 1.5, z + 0.8), (-4.3, 1.5, z + 0.8), (-4.04, 1.5, z + 0.8)], 0.06, ls("stainless"))
    tag(b, (-6.2, 1.2, 1.0), "-x", "VALVE COOLING", 0.3, 0.04)
    return b



# ================================================================ angles and dead-ends
#
# A support whose line turns by more than ANGLE_LIMIT, or that ends a line, is drawn as an angle (dead-end) structure:
# its body is turned to bisect the angle as usual, but each phase is strained off its own hinge point by a strain
# assembly that points along its own span (the game places the same assembly per connection, turned to that span),
# and a jumper loop joins the two assemblies of a phase. Wooden poles take a double dead-end crossarm per span instead,
# square to that span, and a down guy against each span's pull.

ANGLE_LIMIT = 15.0     # degrees of deviation a tangent support takes


def _frame(d):
    """Forward (the span, horizontal), up, side (to the forward's left seen from above, as cross(up, f))."""
    f = np.array([d[0], 0.0, d[2]], float)
    f /= np.linalg.norm(f)
    up = np.array([0.0, 1.0, 0.0])
    s = np.cross(up, f)
    return f, up, s


def bundle_offsets(n, r):
    """Sub-conductor offsets (side, up) of an n-conductor bundle of radius r (twin across, triple apex down,
    six and eight on a circle)."""
    if n == 1:
        return [(0.0, 0.0)]
    if n == 2:
        return [(-r, 0.0), (r, 0.0)]
    if n == 3:
        return [(-r, r * 0.5), (r, r * 0.5), (0.0, -r)]
    return [(r * math.cos(2 * math.pi * k / n), r * math.sin(2 * math.pi * k / n)) for k in range(n)]


def kit_strain(b, hinge, d, ins="poly", L=1.6, n=1, rb=0.23, double=False, gap=0.45, ring_r=0.18, discs=18,
               sheds=(0.075, 0.058), pitch=0.065, core=0.024, k_index=0):
    """A strain (dead-end) assembly hanging off hinge and pointing along d: shackle, a yoke plate when doubled, the
    insulator(s) (glass discs, polymer or composite long-rod), the line-end yoke, a compression dead-end per
    sub-conductor with its jumper pad. Returns (conductor ends, jumper pads), one per sub-conductor."""
    hinge = np.array(hinge, float)
    f, up, s = _frame(d)
    cyl(b, hinge, hinge + f * 0.16, 0.03, ls("galv"), sides=8)
    k = 0.19
    offs = [-gap / 2, gap / 2] if double else [0.0]
    if double:
        b.obox(tuple(_rnd(hinge + f * k)), tuple(_rnd(s)), tuple(_rnd(up)), round(gap / 2 + 0.07, 4), 0.06 + 0.004 * k_index, 0.012, ls("galv"))
    for o in offs:
        a0 = hinge + f * (k + 0.02) + s * o
        a1 = a0 + f * L
        if ins == "glass":
            TY.disc_string(b, a0, a1, discs)
        else:
            polymer_longrod(b, a0, a1, sheds=sheds, ring_r=ring_r, pitch=pitch, core=core, earth_r=max(0.1, ring_r * 0.5))
    pe = hinge + f * (k + L + 0.06)
    subs = bundle_offsets(n, rb)
    hs = max([abs(a) for a, _ in subs] + [gap / 2 if double else 0.0]) + 0.07
    hu = max(abs(c) for _, c in subs) + 0.07
    b.obox(tuple(_rnd(pe)), tuple(_rnd(s)), tuple(_rnd(up)), round(hs, 4), round(hu, 4), 0.014, ls("galv"))
    ends, pads = [], []
    for ox, oy in subs:
        q = pe + s * ox + up * oy + f * 0.015
        cyl(b, q, q + f * 0.42, 0.032, ls("aluminium"), sides=10)
        pad = q + f * 0.2 - up * 0.06
        b.obox(tuple(_rnd(pad)), tuple(_rnd(f)), tuple(_rnd(up)), 0.05, 0.03, 0.008, ls("aluminium"))
        ends.append(q + f * 0.45)
        pads.append(pad - up * 0.02)
    return ends, pads


def kit_dist(b, centre, rad_at, y, d, arm_half=1.22, phases=(-1.07, 0.45, 1.07), arm_mat="crossarm"):
    """A double dead-end crossarm square to the span d (real basis: distribution dead-end construction): an arm on each
    face of the pole, double-arm bolts and spacers, V-braces; a polymer dead-end insulator per phase off the front arm
    on an eye bolt, the dead-end shoe gripping the conductor. Returns (conductor ends, jumper pads)."""
    centre = np.array(centre, float)
    f, up, s = _frame(d)
    r = rad_at(y)
    c0 = centre + up * y
    for side in (1, -1):
        c = c0 + f * side * (r + 0.058)
        b.obox(tuple(_rnd(c)), tuple(_rnd(s)), tuple(_rnd(up)), round(arm_half, 4), 0.045, 0.0575, ls(arm_mat))
        for so in (-0.72, 0.72):
            a0 = c + s * so - up * 0.045
            a1 = c0 - up * 0.68 + f * side * (rad_at(y - 0.68) + 0.008)
            bar(b, a0, a1, 0.04, ls("galv"), h=0.006)
    for so in (0.0, -0.8, 0.8):
        p = c0 + s * so
        cyl(b, p - f * (r + 0.16), p + f * (r + 0.16), 0.009, ls("galv"), sides=6)
        if so:
            cyl(b, p - f * r, p + f * r, 0.022, ls("galv"), sides=8)
    cyl(b, c0 - up * 0.68 - f * (rad_at(y - 0.68) + 0.04), c0 - up * 0.68 + f * (rad_at(y - 0.68) + 0.04), 0.009, ls("galv"), sides=6)
    ends, pads = [], []
    for so in phases:
        e0 = c0 + s * so + f * (r + 0.116)
        cyl(b, e0, e0 + f * 0.07, 0.016, ls("galv"), sides=8)                            # eye nut
        a0, a1 = e0 + f * 0.09, e0 + f * 0.6
        cyl(b, a0, a1, 0.014, ls("polymer"), sides=8)
        for i in range(6):
            c = a0 + f * (0.08 + i * 0.07)
            cyl(b, c, c + f * 0.012, 0.048 if i % 2 == 0 else 0.038, ls("polymer"), sides=12)
        b.obox(tuple(_rnd(a1 + f * 0.11)), tuple(_rnd(f)), tuple(_rnd(up)), 0.11, 0.025, 0.02, ls("aluminium"))   # shoe
        ends.append(a1 + f * 0.25)
        pads.append(a1 + f * 0.12 - up * 0.03)
    return ends, pads


def _resample(pts, step=0.03):
    """A polyline as points about step apart (for clearance checks)."""
    out = []
    for p, q in zip(pts, pts[1:]):
        n = max(1, int(np.linalg.norm(q - p) / step))
        out += [p + (q - p) * (i / n) for i in range(n)]
    out.append(pts[-1])
    return np.array(out)


def _ribbon(A, B, centre):
    """A bundle's jumpers as a ribbon round one centre line (Justin: wires crossing over look terrible): each
    sub-conductor keeps its place across and up / down from the centre line all the way, as jumper spacers hold
    real bundle jumpers, so the loops run side by side and never cross. The ends are paired by their places (A's
    measured across its end of the centre line, B's across the other end), trying every rotation and mirror of the
    bundle. centre: the centre line's points from A's centroid to B's. Returns the curves in A's order."""
    C = [np.array(p, float) for p in centre]
    N = len(C)
    up = np.array([0, 1.0, 0])
    Ls, prev = [], None
    for i in range(N):
        t = C[min(i + 1, N - 1)] - C[max(i - 1, 0)]
        t[1] = 0
        if np.linalg.norm(t) < 1e-6:
            t = prev if prev is not None else np.array([1.0, 0, 0])
        t = t / np.linalg.norm(t)
        prev = t
        Ls.append(np.cross(up, t))
    ca, cb = C[0], C[-1]
    place = lambda p, c, L: np.array([float((p - c) @ L), float(p[1] - c[1])])
    pa = [place(a, ca, Ls[0]) for a in A]
    pb = [place(c, cb, Ls[-1]) for c in B]
    n = len(A)

    def build(order):
        curves = []
        for k in range(n):
            a, c = A[k], B[order[k]]
            oa, ob = pa[k], pb[order[k]]
            ra = (a - ca) - Ls[0] * oa[0] - up * oa[1]              # what is left (along the line) fades out
            rb = (c - cb) - Ls[-1] * ob[0] - up * ob[1]
            pts = []
            for i in range(N):
                t = i / (N - 1)
                o = oa + (ob - oa) * t
                pts.append(C[i] + Ls[i] * o[0] + up * o[1] + ra * (1 - t) + rb * t)
            curves.append(pts)
        return curves
    cands = []
    for shift in range(n):
        for rev in (False, True):
            order = [(shift + (-k if rev else k)) % n for k in range(n)]
            cost = sum(float(((pa[k] - pb[order[k]]) ** 2).sum()) for k in range(n))
            cands.append((cost, order))
    cands.sort(key=lambda c: c[0])
    best = None
    for cost, order in cands:                          # the best-matched order, unless its loops would touch
        curves = build(order)
        sep = min([_min_gap(curves[i], curves[j]) for i in range(n) for j in range(i + 1, n)] + [9.0])
        if sep >= 0.08:
            return curves
        if best is None or sep > best[0]:
            best = (sep, curves)
    return best[1]


def _min_gap(p, q):
    S, T = _resample([np.array(x) for x in p]), _resample([np.array(x) for x in q])
    d = np.sqrt(((S[:, None, :] - T[None, :, :]) ** 2).sum(-1))
    return float(d.min())


def jumper(b, pads_a, pads_b, r=0.012, drop=None, mat="aluminium"):
    """The jumper loops joining two strain assemblies of one phase, hanging below the line between them, the bundle's
    sub-conductors paired so that the loops never cross."""
    A = [np.array(p, float) for p in pads_a]
    B = [np.array(p, float) for p in pads_b]

    ca, cb = sum(A) / len(A), sum(B) / len(B)
    L = np.linalg.norm(cb - ca)
    sag = drop if drop is not None else 0.25 + 0.3 * L
    m = (ca + cb) / 2 - [0, sag, 0]
    for pts in _ribbon(A, B, _bezier(ca, m * 2 - (ca + cb) / 2, cb, 16)):
        path(b, pts, r, ls(mat), sides=6)


def jumper_arc(b, pads_a, pads_b, axis, outside, r=0.012, sag=0.4, min_r=0.0, mat="aluminium", hug=None):
    """Jumpers that go round a vertical axis (a pole, or the peak's mast) instead of through it: each sub-conductor's
    loop follows an arc in plan from its end on one side to its end on the other, the shorter way unless the ends are
    nearly opposite (then round the outside of the turn), sagging as it goes."""
    A = [np.array(p, float) for p in pads_a]
    B = [np.array(p, float) for p in pads_b]
    ax = np.array(axis, float)
    th_out = math.atan2(outside[2], outside[0])
    a, c = sum(A) / len(A), sum(B) / len(B)                 # the bundle's centre line round the axis
    ta, tc = math.atan2(a[2] - ax[2], a[0] - ax[0]), math.atan2(c[2] - ax[2], c[0] - ax[0])
    ra, rc = np.hypot(a[0] - ax[0], a[2] - ax[2]), np.hypot(c[0] - ax[0], c[2] - ax[2])
    dt = (tc - ta + math.pi) % (2 * math.pi) - math.pi
    if abs(abs(dt) - math.pi) < math.radians(10):
        mid = (th_out - ta + math.pi) % (2 * math.pi) - math.pi
        dt = dt if (mid > 0) == (dt > 0) else (dt - math.copysign(2 * math.pi, dt))
    spread = max([float(np.hypot(p[0] - a[0], p[2] - a[2])) for p in A] + [0.0])
    pts = []
    N = max(8, int(abs(dt) / 0.2))
    for i in range(N + 1):
        t = i / N
        th = ta + dt * t
        rr = ra + (rc - ra) * t
        if hug is not None:                                # drawn in close round the pole between its ends
            w = math.sin(math.pi * t) ** 0.5
            rr = rr * (1 - w) + (hug + spread) * w          # the bundle's outer sub-conductor at the hug
        rr = max(min_r + spread, rr) if min_r else rr
        y = a[1] + (c[1] - a[1]) * t - sag * math.sin(math.pi * t)
        pts.append(np.array([ax[0] + rr * math.cos(th), y, ax[2] + rr * math.sin(th)]))
    pts[0], pts[-1] = a, c
    for curve in _ribbon(A, B, pts):
        path(b, curve, r, ls(mat), sides=6)


def jumper_pilot(b, hinge, pads_a, pads_b, r=0.016, mat="aluminium"):
    """A long jumper held by a pilot string (real basis: jumper support insulators on dead-end towers, which stop a long
    jumper swinging into the tower): a polymer pilot insulator hangs from the hinge, a jumper yoke at its foot, and
    each sub-conductor runs from its dead-end down to the yoke and up to the other dead-end."""
    hinge = np.array(hinge, float)
    A = [np.array(p, float) for p in pads_a]
    B = [np.array(p, float) for p in pads_b]
    ca, cb = sum(A) / len(A), sum(B) / len(B)
    span = np.linalg.norm(ca - cb)
    drop = 1.0 + 0.18 * span
    mid = (ca + cb) / 2
    P = np.array([hinge[0] + (mid[0] - hinge[0]) * 0.35, hinge[1] - drop, hinge[2] + (mid[2] - hinge[2]) * 0.35])
    top = hinge - [0, 0.25, 0]
    u = (P - top) / np.linalg.norm(P - top)
    polymer_longrod(b, top, P - u * 0.12, sheds=(0.07, 0.055), ring_r=0.0001, pitch=0.08, core=0.022, earth_r=0.0001)
    t = cb - ca
    t[1] = 0
    t = t / (np.linalg.norm(t) + 1e-9)
    side = np.cross([0, 1.0, 0], t)
    offs = [(a - ca) for a in A]
    rb = max([np.linalg.norm(o) for o in offs] + [0.1])
    hs = max(abs(float(o @ side)) for o in offs) + 0.08
    hv = max(abs(float(o[1])) for o in offs) + 0.04
    b.obox(tuple(_rnd(P)), tuple(_rnd(side)), (0, 1, 0), round(hs, 4), round(hv, 4), 0.012, ls("galv"))
    centre = []
    for e0, e1 in ((ca, P), (P, cb)):
        m = (e0 + e1) / 2 - [0, 0.25 + 0.06 * np.linalg.norm(e1 - e0), 0]
        seg = _bezier(e0, m * 2 - (e0 + e1) / 2, e1, 12)
        centre += seg if not centre else seg[1:]
    for pts in _ribbon(A, B, centre):
        path(b, pts, r, ls(mat), sides=6)


def down_guy(b, att, d, ground=0.0, reach=7.0):
    """A down guy against a span's pull: the guy wire from att on the pole away from d to an anchor, a fibreglass guy
    strain insulator near the top, the yellow guy guard at the bottom, the anchor rod's eye at the ground."""
    att = np.array(att, float)
    f, up, s = _frame(d)
    anc = np.array([att[0], ground, att[2]]) - f * reach
    u = (anc - att) / np.linalg.norm(anc - att)
    g0 = att + u * 0.9
    g1 = g0 + u * 1.1
    rod(b, att, g0, 0.006, ls("galv"), 5)
    cyl(b, g0, g1, 0.014, ls("creosote_butt"), sides=8)
    rod(b, g1, anc, 0.006, ls("galv"), 5)
    L = np.linalg.norm(anc - att)
    cyl(b, anc - u * 2.4, anc - u * 0.1, 0.03, ls("guard_yellow"), sides=10)
    ring(b, anc, u, 0.04, 0.012, ls("galv"), 10)
    cyl(b, anc, anc + u * 0.6, 0.015, ls("galv"), sides=6)
    b.box(*_rnd(att - [0.03, 0.04, 0.03]), *_rnd(att + [0.03, 0.04, 0.03]), ls("galv"))


# ---------------------------------------------------------------- angle bodies

def wood_angle_pole(tier=1, number="P-1500"):
    """The wooden family's angle and dead-end pole: the tier's pole (Class 2-3 wood, or spun concrete for tier III)
    with its cap, ground wire and plates, but no tangent crossarm: each span gets its own double dead-end arm square
    to it (wood, or galvanized channel on the concrete pole) and a down guy. Hinges: the arm heights."""
    P = {1: WOOD_POLE, 2: DOUBLE_POLE, 3: CONCRETE_POLE}[tier]
    b = Board(4, 2110 + tier, face=0.0)
    b.live = []
    top = P["top"]
    rad = lambda y: P["r0"] + (P["r1"] - P["r0"]) * min(1.0, max(0.0, y / top))
    if tier < 3:
        tapered_pole(b, -0.4, 0.5, P["r0"] + 0.003, rad(0.5), ls("creosote_butt"), segs=1)
        tapered_pole(b, 0.5, top, rad(0.5), P["r1"], ls("pole_wood"), segs=26)
        cyl(b, (0, top, 0), (0, top + 0.03, 0), P["r1"] + 0.01, ls("galv_dark"), sides=16)
        ym = top - 0.65 - 0.8 * 2 - 0.95                   # the molding stops below the lowest dead-end arm
        n = 8
        for i in range(n):
            ya, yb2 = i * ym / n, (i + 1) * ym / n
            b.obox(tuple(_rnd([0, (ya + yb2) / 2, (rad(ya) + rad(yb2)) / 2 + 0.008])), (0, 1, 0), (1, 0, 0), round((yb2 - ya) / 2 - 0.002, 4),
                   0.016, 0.009, ls("ground_molding"))
        path(b, [(0, ym, rad(ym) + 0.012), (0, top - 0.2, rad(top - 0.2) + 0.006), (0, top + 0.03, 0)], 0.004, ls("copper_wire"))
        key2 = ATLAS.add(f"tag:brand{tier}", TY.tag_img("SYP  CR  45-1  2026" if tier == 2 else "SYP  CR  45-2  2026", 0.2, 0.045))
    else:
        tapered_pole(b, -0.4, top, rad(-0.4), P["r1"], ls("concrete_spun"), segs=28, sides=20)
        cyl(b, (0, top, 0), (0, top + 0.04, 0), P["r1"] + 0.005, ls("galv"), sides=20)
        b.box(-0.06, 0.45, -rad(0.5) - 0.03, 0.06, 0.6, -rad(0.5) + 0.01, ls("galv"))
        path(b, [(0, 0.45, -rad(0.5) - 0.02), (0, 0.02, -rad(0.5) - 0.02), (0, 0.02, -rad(0.5) - 0.5)], 0.008, ls("copper_wire"))
        key2 = ATLAS.add("tag:castplate16", TY.tag_img("SPUN PSC  16-H6  2026", 0.22, 0.05))
    key = ATLAS.add(f"tag:{number}:pole", TY.tag_img(number, 0.16, 0.06))
    face_quad(b, (0, 1.75, -rad(1.75) - 0.004), "-z", 0.16, 0.06, ATLAS.mat(key))
    face_quad(b, (0, 3.0, -rad(3.0) - 0.004), "-z", 0.2 if tier < 3 else 0.22, 0.045 if tier < 3 else 0.05, ATLAS.mat(key2))
    b.rad = rad
    b.kit = dict(kind="dist", arm_half=P.get("arm_half", 1.22) if tier < 3 else 1.3, arm_mat="crossarm" if tier < 3 else "galv",
                 ys=[top - 0.65 - 0.8 * k for k in range(3)])
    return b


def place_kits(b, dirs):
    """Hang a support's strain assemblies for the spans in dirs (frame-local horizontal directions, one per
    connection) and join each phase's assemblies with jumpers. Returns one list of phases (each a list of sub-
    conductor ends) per connection."""
    K = b.kit
    out, pads = [], []
    if K["kind"] == "dist":
        for k, d in enumerate(dirs):
            y = K["ys"][min(k, len(K["ys"]) - 1)]
            e, p = kit_dist(b, (0, 0, 0), b.rad, y, d, arm_half=K["arm_half"], arm_mat=K["arm_mat"])
            f, up, s = _frame(d)
            down_guy(b, np.array([0, y - 0.3, 0]) - f * (b.rad(y - 0.3) + 0.02), d)
            # phases across the span, left to right seen along it from this pole
            out.append([[x] for x in e])
            pads.append([[x] for x in p])
        # the same phase is the same side of travel: through the pole, left of travel in = left of travel out; a tap
        # (three or more spans) is jumpered from the first span's arm to each of the others
        for m in range(1, len(dirs)):
            A, B = _side_order(dirs[0], out[0], pads[0], incoming=True), _side_order(dirs[m], out[m], pads[m], incoming=False)
            outside = -(np.array(dirs[0], float) + np.array(dirs[m], float))
            if np.linalg.norm(outside) < 1e-6:
                outside = _frame(dirs[0])[2]
            # each phase's loop hugs the pole at its own radius, nearest phase innermost, so the loops nest round
            # the pole instead of crossing (Justin: wires crossing over look terrible)
            rad = lambda q: float(np.hypot(q[0][0], q[0][2]))
            pairs = list(zip(A, B))
            rank = sorted(range(len(pairs)), key=lambda i: rad(pairs[i][0][1]) + rad(pairs[i][1][1]))
            for k, i in enumerate(rank):
                (ea, pa), (eb, pb) = pairs[i]
                jumper_arc(b, pa, pb, (0, 0, 0), outside, r=0.008, sag=0.3 + 0.15 * (m - 1), min_r=0.32,
                           hug=0.42 + 0.08 * (m - 1) + 0.14 * k)
        return out
    for kk, d in enumerate(dirs):
        phases, ph_pads = [], []
        for h in K["hinges"]:
            e, p = kit_strain(b, h, d, k_index=kk, **K["spec"])
            phases.append(e)
            ph_pads.append(p)
        out.append(phases)
        pads.append(ph_pads)
    for m in range(1, len(dirs)):                         # each further span jumpered to the first, hinge by hinge
        outside = -(np.array(dirs[0], float) + np.array(dirs[m], float))
        if np.linalg.norm(outside) < 1e-6:
            outside = _frame(dirs[0])[2]
        for i, (pa, pb) in enumerate(zip(pads[0], pads[m])):   # the same hinge carries the same phase both ways
            if i in K.get("arc", {}):                       # round the peak's mast, at the clearance
                h = np.array(K["hinges"][i], float)
                jumper_arc(b, pa, pb, h, outside, r=K.get("jr", 0.016), sag=K["arc"][i][1], min_r=K["arc"][i][0])
            elif np.linalg.norm(sum(np.array(p) for p in pa) / len(pa) - sum(np.array(p) for p in pb) / len(pb)) > 4.0:
                jumper_pilot(b, K["hinges"][i], pa, pb, r=K.get("jr", 0.016))
            else:
                jumper(b, pa, pb, r=K.get("jr", 0.016))
    if len(dirs) == 1:
        for pa in pads[0]:                                 # a line end: the slack tails tied back under the hinge
            for p in pa:
                p = np.array(p)
                path(b, [p, p - [0, 0.5, 0], p - [0, 0.9, 0]], K.get("jr", 0.016), ls("aluminium"), sides=6)
    return out


def _side_order(d, ends, pads, incoming):
    """A wooden dead-end arm's phases ordered left to right across travel (incoming: travel runs against d)."""
    f, up, s = _frame(d)
    left = s if not incoming else -s
    key = lambda e: -float(np.dot(np.array(e[0][0]), left))
    return sorted(zip(ends, pads), key=key)



def angle_tower(spec, number, danger=None, seed=2350):
    """The lattice families' angle and dead-end tower (real basis: square-based single-circuit dead-end towers): a
    square body tapering to the crossarm; a box-truss crossarm whose ends carry the outer phases far enough out that,
    at a right-angle turn, their strings and jumpers stay clear of the body; the centre phase dead-ended on a peak above
    the crossarm, high enough that its jumper clears the arm; shield peaks on the arm. Hinges: the arm ends' hangers
    and the peak."""
    S = spec
    b = Board(10, seed, face=0.0)
    b.live = []
    TY._faces()
    key_d = danger or "danger500"
    if key_d not in TY.FACES:
        TY.FACES[key_d] = ATLAS.add(f"flag:{key_d}", flag_img(S["danger_text"], (196, 30, 30), (250, 250, 250), 0.34, 0.12))
    B, Wd, ay, ah = S["base"], S["top_half"], S["arm_y"], S["arm_h"]
    n = int(ay / 3.0)
    levels = [0.35] + [0.35 + (ay - 0.35) * (1 - (1 - i / n) ** 1.25) for i in range(1, n)] + [ay]
    lattice_body(b, B, Wd, ay, levels, key_d, number, leg_w=S.get("leg_w", 0.24), brace_w=0.1)
    sec = lambda x, y0, y1, z: [(x, y0, -z), (x, y0, z), (x, y1, z), (x, y1, -z)]
    section_truss(b, sec(-Wd - 0.02, ay, ay + ah, Wd), sec(Wd + 0.02, ay, ay + ah, Wd), [0, 0.5, 1.0], leg_w=0.22, brace_w=0.09,
                  angle_legs=False)
    X = S["arm_x"]
    for sx in (-1, 1):
        section_truss(b, sec(sx * (Wd + 0.06), ay, ay + ah, Wd), sec(sx * (X + 0.3), ay, ay + 0.8, 0.5),
                      list(np.linspace(0, 1, max(4, int(X / 1.4)))), leg_w=0.16, brace_w=0.075, angle_legs=False)
        bar(b, (sx * (X + 0.3), ay + 0.4, -0.52), (sx * (X + 0.3), ay + 0.4, 0.52), 0.08, TY.GALV)
    # the centre phase's peak: a four-chord mast from the crossarm's box up to its cap
    py = S["peak_y"]
    P = [(-Wd * 0.8, ay + ah, -Wd * 0.8), (Wd * 0.8, ay + ah, -Wd * 0.8), (Wd * 0.8, ay + ah, Wd * 0.8), (-Wd * 0.8, ay + ah, Wd * 0.8)]
    Q = [(-0.45, py, -0.45), (0.45, py, -0.45), (0.45, py, 0.45), (-0.45, py, 0.45)]
    section_truss(b, P, Q, list(np.linspace(0, 1, max(3, int((py - ay - ah) / 1.6) + 1))), leg_w=0.16, brace_w=0.075, seed=1)
    b.box(-0.55, py, -0.55, 0.55, py + 0.12, 0.55, TY.GALV)
    hinges = []
    for sx in (-1, 1):
        x = sx * X
        b.box(x - 0.09, ay - 0.16, -0.45, x + 0.09, ay - 0.02, 0.45, TY.GALV)               # hanger beam under the arm end
        hinges.append(np.array([x, ay - 0.2, 0.0]))
    hinges.insert(1, np.array([0.0, py - 0.05, 0.0]))
    b.box(-0.08, py - 0.05, -0.08, 0.08, py, 0.08, TY.GALV)
    # shield peaks on the arm
    sxp = S["shield_x"]
    for sx in (-1, 1):
        x = sx * sxp
        zz = 0.5 + (Wd - 0.5) * max(0.0, (X - sxp)) / max(1e-6, X - Wd)
        for dz, w_ in ((-zz, 0.12), (zz, 0.105)):
            bar(b, (x, ay + 0.8 + (ah - 0.8) * max(0.0, (X - sxp)) / max(1e-6, X - Wd), dz), (x, S["shield_y"], 0), w_, TY.GALV)
        cyl(b, (x, S["shield_y"], 0), (x, S["shield_y"] + 0.15, 0), 0.08, TY.GALV, sides=8)
        b.box(x - 0.06, S["shield_y"] + 0.15, -0.25, x + 0.06, S["shield_y"] + 0.27, 0.25, TY.ALU)
    b.shield = [list(_rnd([sx * sxp, S["shield_y"] + 0.27, 0])) for sx in (-1, 1)]
    b.kit = dict(kind="strain", hinges=hinges, spec=S["kit"], jr=0.016, arc={1: (S["clear"], S["jsag"])})
    return b


ANGLE_345 = dict(clear=2.6, jsag=1.0, base=4.6, top_half=1.6, arm_y=24.0, arm_h=1.6, arm_x=8.5, peak_y=31.0, shield_x=6.5, shield_y=31.5,
                 danger_text="DANGER  345 kV", kit=dict(ins="glass", L=3.3, n=2, rb=0.23, double=True, gap=0.45, discs=18))
ANGLE_500 = dict(clear=3.4, jsag=1.3, base=5.6, top_half=1.8, arm_y=30.0, arm_h=2.0, arm_x=10.5, peak_y=38.5, shield_x=8.0, shield_y=39.0,
                 danger_text="DANGER  500 kV", kit=dict(ins="glass", L=4.3, n=3, rb=0.46, double=True, gap=0.5, discs=24))
ANGLE_765 = dict(clear=5.0, jsag=1.8, base=7.2, top_half=2.0, arm_y=44.0, arm_h=2.5, arm_x=15.0, peak_y=55.0, shield_x=11.0, shield_y=55.5, leg_w=0.28,
                 danger_text="DANGER  765 kV", kit=dict(ins="glass", L=6.5, n=6, rb=0.457, double=True, gap=0.6, discs=32))


def lattice_345_angle(number="LINE 1  TOWER 9"):
    return angle_tower(ANGLE_345, number, "danger345a", 2351)


def lattice_500_angle(number="500 kV  TOWER 220", variant="plain"):
    spec = dict(ANGLE_500)
    if variant != "plain":
        spec["kit"] = dict(ins="poly", L=4.3, n=3, rb=0.46, double=True, gap=0.5, ring_r=0.3, sheds=(0.095, 0.072))
    return angle_tower(spec, number, "danger500", 2352 + (variant != "plain"))


def uhv_765_angle(number="765 kV  TOWER 95"):
    return angle_tower(ANGLE_765, number, "danger765", 2402)


def uhvdc_angle(number="UHVDC  TOWER 1300"):
    """The UHVDC angle tower: the bipole tower with its arms out to 18 m, so a pole strained back across a right-angle
    turn stays clear of the head; double composite strain strings, eight-conductor dead-end yokes."""
    T0 = dict(TDC)
    TDC.update(arm_x=21.5, pole_x=18.0, peak_x=9.0)
    try:
        b = uhvdc_tower(number, angle=True)
    finally:
        TDC.clear(); TDC.update(T0)
    return b


def lattice_345(number="LINE 1  TOWER 9"):
    """The 345 kV suspension tower (transformer_yard.tower_345) with its conductor and shield points."""
    b = TY.tower_345(False, number)
    T = TY.T345
    by = T["bridge_y"]
    b.attach = []
    for px in T["phase_x"]:
        y = by - (3.4 if px == 0.0 else 3.5)
        b.attach += [[px - 0.225, y, 0.0], [px + 0.225, y, 0.0]]
    b.shield = [[sx * 5.6, T["peak_y"] + 0.27, 0.0] for sx in (-1, 1)]
    return b
