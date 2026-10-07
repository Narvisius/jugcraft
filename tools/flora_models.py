"""The graveyard flora, plant by plant (tools/flora_art.py has the pieces and the rules). Each builder returns a Sculpt
whose `models` maps a model name to its elements and whose atlas is the plant's texture. Tall plants are built in one
frame two blocks high (y 0..32) and cut into their lower and upper blocks by where each element starts.

The look follows hand-built plants: a few bent stems of small boxes carrying flat, cut-out leaves and petals at angles,
and the flowers themselves as little boxes (bells, trumpets, berries, cups) where a flat picture would read thin.
"""
import copy
import math
import random

from PIL import Image

import model_writer

from flora_art import (ANTHER, ASPHODEL, ASPHODEL_VEIN, BERRY, CALYX, CAPSULE, DRY, DUSK_LEAF, FERN, FERN_RIB, FINGER,
                       FINGER_TIP, FOX, FOX_SPOT, GHOST, GHOST_FLECK, HEART_PINK, HEART_WHITE, IVY, LEAF, LILY,
                       LILY_THROAT, MANDRAKE_FLOWER, MANDRAKE_LEAF, MOSS, NIGHT_PURPLE, ROOT, ROOT_DARK, ROSE, SHROUD,
                       SIDES4, SNOW_GREEN, SNOW_WHITE, SOIL, SPIDER_RED, SPORE, STEM, Px, Sculpt, TEXELS, blades, column,
                       cube, frond, ivy_leaf, ivy_sheet, leaf, leaf_flat, leaf_out, pal, plane_xy, plane_xz, plane_zy,
                       rgb, root_face, rotation, segment, shade, solid, star, strip, upright, wisps)
import flora_art as fa


# ---------------------------------------------------------------- painters only these plants use

def spider_head(seed=1):
    """A spider lily's head from the side: recurved, wavy red petals curling down either side of the stem's top and
    long stamens sweeping up and out past them."""
    def paint(p):
        rng = random.Random(seed)
        cx, cy = (p.w - 1) / 2, p.h - 7.5
        for side in (-1, 1):
            for length, rise, drop, k0 in ((8.0, 0.6, 3.6, 3), (6.0, 1.4, 2.6, 4)):
                for i in range(40):
                    t = i / 39
                    x = cx + side * t * length
                    y = cy - rise * math.sin(t * math.pi) * 1.2 + drop * t * t
                    wave = math.sin(t * 14 + seed) * 0.5
                    width = 1.6 * (1 - t * 0.6)
                    for w in range(-1, 2):
                        if abs(w) <= width:
                            k = k0 - (1 if w > 0 else 0) + (1 if t > 0.7 else 0)
                            p.put(x, y + w + wave, shade(SPIDER_RED, k))
            for s in range(3):
                reach = 5.5 + s * 1.4
                height = 5.0 + s * 0.9 + rng.random()
                for i in range(30):
                    t = i / 29
                    x = cx + side * (t * reach * 0.55 + t ** 3 * reach * 0.45)
                    y = cy - t * height + t ** 3 * 1.5
                    p.put(x, y, SPIDER_RED[3])
                p.put(cx + side * reach, cy - height + 1.5, rgb("e8b04a"))
        for dx in (-1, 0, 1):
            p.put(cx + dx, cy + 1, SPIDER_RED[1])
    return paint


def spider_top(seed=1):
    """A spider lily's head from above: a ring of narrow wavy petals and the stamens between them."""
    def paint(p):
        star(12, SPIDER_RED, centre=[SPIDER_RED[1], SPIDER_RED[2]], inner=0.1, width=0.36, wavy=0.35, seed=seed, twist=0.8)(p)
        rng = random.Random(seed + 1)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for i in range(10):
            a = i / 10 * math.tau + 0.3
            r = p.w / 2 - 0.5
            p.put(cx + math.cos(a) * r, cy + math.sin(a) * r, rgb("e8b04a"))
            for j in range(3, int(r)):
                if rng.random() < 0.7:
                    p.put(cx + math.cos(a) * j, cy + math.sin(a) * j, SPIDER_RED[3])
    return paint


def bell_side(palette, tip=None, seed=1, spots=None):
    """The side of a hanging bell: tepals meeting at the top, flaring open at the bottom (a slit between them), with
    an optional coloured tip and spots."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                t = y / max(1, p.h - 1)
                k = n - 2 - (1 if x >= p.w * 0.6 else 0) + (1 if t < 0.3 else 0) + (0 if fa.QUIET else rng.choice((0, 0, 0, -1)))
                p.put(x, y, shade(palette, k))
        if p.w >= 3:
            for y in range(p.h // 2, p.h):
                p.put(p.w // 2, y, palette[1])
        if tip:
            for x in range(p.w):
                p.put(x, p.h - 1, tip)
        if spots:
            for _ in range(max(1, p.w * p.h // 6)):
                p.put(rng.randrange(p.w), rng.randrange(p.h // 2, p.h), rng.choice(spots))
    return paint


def bell_mouth(palette, inner, centre=None, seed=1):
    """The open end of a bell seen straight in: its rim, the darker inside and a heart of stamens or green marks."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                edge = x in (0, p.w - 1) or y in (0, p.h - 1)
                p.put(x, y, palette[-2] if edge else inner[0])
        if centre:
            for dx in range(p.w // 3, p.w - p.w // 3):
                for dy in range(p.h // 3, p.h - p.h // 3):
                    p.put(dx, dy, centre)
    return paint


def bell_picture(palette, lobes=3, tip=None, inner=None, seed=1, spots=None, flare=1.0):
    """A hanging bell from the side: a narrow neck at the top widening to a mouth of `lobes` points at the bottom,
    shaded in bands, with coloured lobe tips (`tip`), the dark inside showing between them (`inner`) and spots."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        cx = (p.w - 1) / 2
        for y in range(p.h):
            t = y / max(1, p.h - 1)
            half = (0.25 + 0.75 * math.sin(min(1.0, t * 1.15) * math.pi / 2) ** 1.4 * flare) * p.w / 2
            for x in range(p.w):
                d = x - cx
                if abs(d) > half:
                    continue
                if t > 0.8 and lobes:
                    # The mouth's points: a notch between each pair of lobes.
                    phase = (d / max(0.5, half) + 1) / 2 * lobes
                    if abs(phase - round(phase)) < 0.22 and 0 < round(phase) < lobes and t > 0.88:
                        if inner:
                            p.put(x, y, inner)
                        continue
                k = n - 2 - (1 if d > half * 0.35 else 0) + (1 if d < -half * 0.4 else 0) - (1 if t > 0.85 else 0)
                k += 0 if fa.QUIET else rng.choice((0, 0, 0, -1))
                c = shade(palette, k)
                if tip and t > 0.88:
                    c = tip
                p.put(x, y, c)
        if spots:
            for _ in range(max(1, p.w * p.h // 12)):
                x, y = rng.randrange(p.w), rng.randrange(p.h // 3, p.h)
                if p.get(x, y):
                    p.put(x, y, rng.choice(spots))
        p.outline(palette[0])
    return paint


def crossed_bell(sc, tip, key, painter, width, height, core=None, light=None, ovary=None, density=4):
    """A bell hanging from `tip`: two crossed pictures of it (so it reads from every side) round a small core box."""
    x, y, z = tip
    out = []
    top = y
    if ovary:
        o = sc.piece("ovary", 2, 2, solid(ovary, 2))
        out.append(cube((x - 0.45, y - 0.9, z - 0.45), (x + 0.45, y, z + 0.45), {s: o for s in SIDES4 + ("down",)}, light=light))
        top = y - 0.9
    uv = sc.piece(key, max(3, round(width * density)), max(3, round(height * density)), painter)
    out.append(plane_xy(x - width / 2, x + width / 2, top - height, top, z, uv, light=light))
    out.append(plane_zy(z - width / 2, z + width / 2, top - height, top, x, uv, light=light))
    if core:
        c = sc.piece(f"{key}_core", 2, 3, solid(core, 3))
        out.append(cube((x - width * 0.22, top - height * 0.75, z - width * 0.22), (x + width * 0.22, top, z + width * 0.22),
                        {s: c for s in SIDES4 + ("down",)}, light=light))
    return out


def heart(seed=1):
    """A bleeding heart flower hanging from its stalk: a pink heart with the white inner petals dropping below it."""
    def paint(p):
        cx = (p.w - 1) / 2
        top = 2
        for y in range(top):
            p.put(cx, y, STEM[2])
        body = p.h - top - 2
        for y in range(body):
            t = y / max(1, body - 1)
            half = (p.w / 2) * (0.75 + 0.25 * math.sin(t * math.pi * 0.9)) * (1 - max(0, t - 0.55) / 0.45 * 0.95)
            for x in range(p.w):
                d = x - cx
                lobe = not (t < 0.18 and abs(d) < 0.8)
                if abs(d) <= half and lobe:
                    k = 3 if d < 0 else 2
                    if abs(d) > half - 1:
                        k -= 1
                    p.put(x, top + y, shade(HEART_PINK, k))
        for y in range(top + body - 1, p.h):
            p.put(cx, y, HEART_WHITE[1])
            if y == p.h - 1:
                p.put(cx - 1, y, HEART_WHITE[0])
                p.put(cx + 1, y, HEART_WHITE[0])
    return paint


def rose_core(top=False, seed=1):
    """The packed petals of a rose: on its sides overlapping petal edges, on top a dark spiral."""
    def paint(p):
        rng = random.Random(seed)
        n = len(ROSE)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                if top:
                    r = math.hypot(x - cx, y - cy)
                    a = math.atan2(y - cy, x - cx)
                    spiral = (a / math.tau * 3 + r * 0.55) % 1
                    k = 1 + int(r) % 2 + (2 if spiral < 0.25 else 0)
                else:
                    band = (y + (x // 3) * 2) % 4
                    k = 2 + (2 if band == 0 else 0) + (1 if y < 2 else 0)
                p.put(x, y, shade(ROSE, k + (0 if fa.QUIET else rng.choice((0, 0, -1)))))
    return paint


def lily_mouth(seed=1):
    """A lily's trumpet seen into: six white tepals round a green throat, with orange anthers."""
    def paint(p):
        star(6, LILY, centre=LILY_THROAT, inner=0.2, width=0.62, seed=seed, centre_r=0.28)(p)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for dx, dy in ((-1, -1), (1, -1), (0, 1)):
            p.put(cx + dx, cy + dy, ANTHER[1])
    return paint


def strap_arch(palette, seed=1, count=3):
    """Strap leaves rising from the bottom and arching over to one side or the other near their tips."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for i in range(count):
            x = p.w / 2 + (i - (count - 1) / 2) * 1.6
            side = -1 if i % 2 == 0 else 1
            for y in range(p.h - 1, -1, -1):
                t = (p.h - 1 - y) / max(1, p.h - 1)
                xx = x + side * (t ** 2.2) * p.w * 0.38
                for w in (0, 1):
                    k = n - 2 - w + (1 if t > 0.7 else 0)
                    p.put(xx + w * side, y, shade(palette, k))
    return paint


def fern_pair(seed=1):
    """Two fronds of a painted fern arching out from the middle, one each way."""
    def paint(p):
        half_w = p.w // 2
        left = Px(p.img.crop((0, 0, half_w, p.h)))
        frond(FERN, FERN_RIB, seed, curve=-0.8, pinnae=10, droop=0.5)(left)
        p.img.alpha_composite(left.img, (0, 0))
        right = Px(p.img.crop((half_w, 0, p.w, p.h)).transpose(0))
        frond(FERN, FERN_RIB, seed + 1, curve=-0.8, pinnae=10, droop=0.5)(right)
        p.img.alpha_composite(right.img.transpose(0), (half_w, 0))
    return paint


def moss_top(seed=1):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 2 + (1 if fa.QUIET else rng.choice((0, 0, 1, 1, -1, 2)))
                p.put(x, y, shade(MOSS, k))
    return paint


def spores(seed=1):
    """Moss sporophytes: thin red stalks, each with a little brown capsule."""
    def paint(p):
        rng = random.Random(seed)
        for i in range(4):
            x = 1 + i * (p.w - 2) / 3 + rng.choice((-0.5, 0, 0.5))
            height = p.h - 2 - rng.randrange(0, 3)
            for y in range(p.h - 1, p.h - 1 - height, -1):
                p.put(x, y, rng.choice(SPORE[:2]))
            top = p.h - 1 - height
            p.put(x, top, CAPSULE[1])
            p.put(x, top - 1, CAPSULE[2])
            p.put(x + 1, top, CAPSULE[0])
    return paint


def finger_side(tip=False, seed=1):
    """Dead man's fingers: black, wrinkled skin (pale at the tip of the top piece)."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 2 + (0 if fa.QUIET else rng.choice((0, 0, 1, -1))) - (1 if x == p.w - 1 else 0) + (1 if x == 0 else 0)
                if rng.random() < 0.12:
                    k = 0
                c = shade(FINGER, k)
                if tip and y < 2:
                    c = FINGER_TIP[1 if y else 2] if rng.random() < 0.85 else FINGER[3]
                p.put(x, y, c)
    return paint


def root_side(seed=1):
    def paint(p):
        strip(ROOT, seed, knots=3)(p)
        rng = random.Random(seed)
        for _ in range(p.w * p.h // 8):
            y = rng.randrange(p.h)
            x = rng.randrange(p.w)
            p.put(x, y, ROOT[1])
    return paint


def root_top(seed=1):
    def paint(p):
        solid(ROOT, seed)(p)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            p.put(cx + dx - 0.5, cy + dy - 0.5, MANDRAKE_LEAF[1])
    return paint


def peek(seed=1):
    """The top of a mandrake's head just out of the soil: the eyes alone show."""
    def paint(p):
        root_side(seed)(p)
        ey = p.h * 0.55
        for x in (p.w * 0.28, p.w * 0.66):
            for dx in (0, 1):
                p.put(x + dx, ey, ROOT_DARK[0])
                p.put(x + dx, ey + 1, ROOT_DARK[0])
    return paint


# ---------------------------------------------------------------- shared builders

def berry(sc, centre, size=1.6):
    """A glossy black berry sitting in its green, star-shaped calyx."""
    x, y, z = centre
    h = size / 2
    side = sc.piece("berry", 3, 3, solid(BERRY, 4, glossy=BERRY[4]))
    cal = sc.piece("calyx", 7, 7, star(5, CALYX, inner=0.2, width=0.55, seed=9))
    return [cube((x - h, y, z - h), (x + h, y + size, z + h), {s: side for s in SIDES4 + ("up", "down")}),
            plane_xz(x - 1.6, x + 1.6, z - 1.6, z + 1.6, y + 0.15, cal)]


def hanging_bell(sc, tip, key, palette, size=(2.0, 2.4), mouth=None, light=None, inner=None, centre=None, ovary=None,
                 spots=None):
    """A bell hanging from a stem's tip: a short stalk, an optional green ovary, then the bell, mouth down."""
    x, y, z = tip
    w, h = size
    side = sc.piece(f"{key}_side", max(2, round(w * TEXELS)), max(2, round(h * TEXELS)), bell_side(palette, mouth, seed=len(key), spots=spots))
    mouth_uv = sc.piece(f"{key}_mouth", max(2, round(w * TEXELS)), max(2, round(w * TEXELS)),
                        bell_mouth(palette, inner or [palette[0]], centre))
    top_uv = sc.piece(f"{key}_top", 2, 2, solid(palette[1:], 3))
    out = []
    y_top = y
    if ovary:
        o = sc.piece("ovary", 2, 2, solid(ovary, 2))
        out.append(cube((x - 0.5, y - 1.0, z - 0.5), (x + 0.5, y, z + 0.5), {s: o for s in SIDES4 + ("down",)}, light=light))
        y_top = y - 1.0
    out.append(cube((x - w / 2, y_top - h, z - w / 2), (x + w / 2, y_top, z + w / 2),
                    {**{s: side for s in SIDES4}, "down": mouth_uv, "up": top_uv}, light=light))
    return out


def split_tall(elements):
    """Cuts a two-block frame into (lower, upper): each element goes to the block it starts in."""
    lower, upper = [], []
    for e in elements:
        start = min(e["from"][1], e["to"][1])
        if start < 16:
            lower.append(e)
        else:
            copy = dict(e, **{"from": [e["from"][0], e["from"][1] - 16, e["from"][2]], "to": [e["to"][0], e["to"][1] - 16, e["to"][2]]})
            if "rotation" in e:
                o = e["rotation"]["origin"]
                copy["rotation"] = dict(e["rotation"], origin=[o[0], o[1] - 16, o[2]])
            upper.append(copy)
    return lower, upper


def tall_models(sc, elements):
    # The halves are drawn one above the other: separate the whole frame first, so no face of one half shares a plane
    # with a face of the other (each half's own pass at writing cannot see the other's).
    elements = copy.deepcopy(elements)
    model_writer.separate_coplanar(elements)
    lower, upper = split_tall(elements)
    sc.models[f"{sc.name}_bottom"] = lower
    sc.models[f"{sc.name}_top"] = upper
    sc.models[f"{sc.name}_item"] = elements


# ---------------------------------------------------------------- small flowers

def spider_lily():
    sc = Sculpt("spider_lily", 11)
    stem = sc.piece("stem", 2, 22, strip(STEM, 3))
    els = []
    e, top = segment((8, 0, 8), 4.5, width=1.0, uv=stem)
    els.append(e)
    e, top = segment(top, 6.2, "x", 0, width=0.9, uv=stem)
    els.append(e)
    cx, cy, cz = top
    for yaw in (0, 45, 90, 135):
        els.append(upright(sc, "head", spider_head(5), (cx, cz), 9, cy - 3.5, cy + 3.5, yaw=yaw))
    els.append(plane_xz(cx - 4.5, cx + 4.5, cz - 4.5, cz + 4.5, cy + 0.2, sc.piece("top", 18, 18, spider_top(7))))
    knob = sc.piece("knob", 3, 3, solid(SPIDER_RED[1:4], 5))
    els.append(cube((cx - 0.8, cy - 0.6, cz - 0.8), (cx + 0.8, cy + 0.5, cz + 0.8), {s: knob for s in SIDES4 + ("up",)}))
    sc.models["spider_lily"] = els
    return sc


def snowdrop():
    sc = Sculpt("snowdrop", 12)
    stem = sc.piece("stem", 2, 16, strip(pal("3a5e2c", "4e7a3a", "679648", "84b05c"), 4))
    blade = pal("2e4a34", "3f6246", "56805c", "6f9a72", "8cb48c")
    els = []
    for i, yaw in enumerate((0, 90, 45)):
        els.append(upright(sc, f"leaf{i % 2}", leaf(blade, "strap", seed=20 + i, rib=rgb("b8d0b0")), (8, 8), 1.6, 0, 7.5 + i,
                           yaw=yaw, lean=(-22.5, 22.5, 0)[i]))
    for x, z, height, axis, angle in ((6.6, 8.2, 6.0, "z", 45), (9.6, 7.4, 7.0, "z", -45), (8.2, 9.8, 5.0, "x", 45)):
        e, top = segment((x, 0, z), height, width=0.8, uv=stem)
        els.append(e)
        e, tip = segment(top, 2.0, axis, angle, width=0.7, uv=stem)
        els.append(e)
        els += crossed_bell(sc, tip, "drop", bell_picture(SNOW_WHITE, 3, inner=SNOW_GREEN[1], seed=4, flare=1.1), 2.6, 3.0,
                            core=SNOW_WHITE[2:], ovary=SNOW_GREEN)
    sc.models["snowdrop"] = els
    return sc


def deadly_nightshade():
    sc = Sculpt("deadly_nightshade", 13)
    stem = sc.piece("stem", 2, 18, strip(pal("2e3a22", "3e4e2c", "566a3c", "6e8650"), 5))
    big = leaf(DUSK_LEAF, "oval", seed=31, wavy=0.3)
    small = leaf(DUSK_LEAF, "oval", seed=32)
    els = []
    e, top_main = segment((8, 0, 8), 9.5, width=1.2, uv=stem)
    els.append(e)
    e, tip1 = segment((8, 4.5, 8), 4.2, "z", -45, width=0.9, uv=stem)
    els.append(e)
    e, tip2 = segment((8, 5.5, 8), 3.6, "x", -45, width=0.9, uv=stem)
    els.append(e)
    e, tip3 = segment((8, 6.8, 8), 3.0, "z", 22.5, width=0.8, uv=stem)
    els.append(e)
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "big", big, (8, 2.4, 8), d, 5.2, 3.4, 22.5))
    for d in ("ne", "sw"):
        els.append(leaf_flat(sc, "small", small, (8, 5.0, 8), d, 4.0, 2.6))
    els.append(leaf_out(sc, "small", small, (8, 8.6, 8), "north", 3.0, 2.0, 45))
    for tip in (tip1, tip3):
        els += crossed_bell(sc, tip, "bell", bell_picture(NIGHT_PURPLE, 5, tip=NIGHT_PURPLE[2], inner=NIGHT_PURPLE[0], seed=6), 2.4, 2.6,
                            core=NIGHT_PURPLE[1:3])
    els += berry(sc, (tip2[0], tip2[1], tip2[2]), 1.3)
    els += berry(sc, (top_main[0], top_main[1], top_main[2]), 1.4)
    e, tip4 = segment((8, 3.6, 8), 2.6, "x", 45, width=0.7, uv=stem)
    els.append(e)
    els += berry(sc, tip4, 1.2)
    for d in ("north", "east"):
        els.append(leaf_out(sc, "small", small, (8, 7.2, 8), d, 3.4, 2.2, 22.5))
    els.append(leaf_out(sc, "small", small, (tip1[0], tip1[1] - 0.3, tip1[2]), "east", 2.6, 1.8, 22.5))
    els.append(leaf_out(sc, "small", small, (tip2[0], tip2[1] - 0.3, tip2[2]), "north", 2.6, 1.8, 22.5))
    sc.models["deadly_nightshade"] = els
    return sc


def bleeding_heart():
    sc = Sculpt("bleeding_heart", 14)
    stem = sc.piece("stem", 2, 18, strip(pal("3e3a26", "56502e", "6e6a3a", "8a8650"), 6))
    els = []
    lobed = frond(pal("1f3a24", "2d5032", "3e6a42", "538458", "6c9c6e"), pal("3a4a2a", "4e6236", "667a44"), 41, pinnae=5)
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "frond", lobed, (8, 0.5, 8), d, 5.5, 4.4, 45))
    # The east arch: up, over, along and down, with four hearts beneath it.
    e, top = segment((6.6, 0, 8), 8.4, width=0.6, uv=stem)
    els.append(e)
    e, top = segment(top, 3.5, "z", -45, width=0.55, uv=stem)
    els.append(e)
    y = top[1]
    els.append(cube((top[0] - 0.3, y - 0.3, 7.7), (13.0, y + 0.3, 8.3), {s: stem for s in ("north", "south", "up", "down", "east")}))
    els.append(cube((12.7, y - 2.4, 7.7), (13.3, y + 0.2, 8.3), {s: stem for s in SIDES4 + ("down",)}))
    hearts = [(9.4, 3.4), (10.9, 3.0), (12.2, 2.6), (13.0, 2.0)]
    for i, (hx, size) in enumerate(hearts):
        piece = sc.piece(f"heart{size}", round(size * TEXELS) + (round(size * TEXELS) + 1) % 2, round((size + 1.2) * TEXELS), heart(i))
        hy = y - 0.3 - (2.2 if i == 3 else 0)
        els.append(plane_xy(hx - size / 2, hx + size / 2, hy - size - 1.2, hy, 8.0, piece))
        els.append(plane_zy(8 - size / 2, 8 + size / 2, hy - size - 1.2, hy, hx, piece))
    # A shorter arch toward the north.
    e, top = segment((8.8, 0, 9.0), 7.0, width=0.55, uv=stem)
    els.append(e)
    e, top = segment(top, 3.0, "x", -45, width=0.5, uv=stem)
    els.append(e)
    y2 = top[1]
    els.append(cube((8.5, y2 - 0.3, 3.4), (9.1, y2 + 0.3, top[2] + 0.3), {s: stem for s in ("east", "west", "up", "down", "north")}))
    for i, (hz, size) in enumerate(((6.0, 3.0), (4.6, 2.6), (3.6, 2.0))):
        piece = sc.piece(f"heart{size}", round(size * TEXELS) + (round(size * TEXELS) + 1) % 2, round((size + 1.2) * TEXELS), heart(i))
        els.append(plane_zy(hz - size / 2, hz + size / 2, y2 - 0.35 - size - 1.2, y2 - 0.35, 8.8, piece))
        els.append(plane_xy(8.8 - size / 2, 8.8 + size / 2, y2 - 0.35 - size - 1.2, y2 - 0.35, hz, piece))
    sc.models["bleeding_heart"] = els
    return sc


def ghost_pipe():
    sc = Sculpt("ghost_pipe", 15)
    stem = sc.piece("stem", 2, 14, strip(GHOST, 7, light=True))
    els = []
    glow = 9
    pipe = bell_picture(GHOST, 5, inner=pal("8a8490")[0], seed=8, spots=GHOST_FLECK[1:], flare=0.9)
    for x, z, height, axis, angle in ((6.4, 6.6, 6.6, "z", 45), (9.8, 7.0, 7.6, "z", -45), (7.6, 10.2, 5.6, "x", 45),
                                      (10.4, 10.4, 4.6, "x", -45), (8.4, 8.6, 8.2, "x", -45)):
        e, top = segment((x, 0, z), height, width=0.9, uv=stem, light=glow)
        els.append(e)
        e, tip = segment(top, 1.8, axis, angle, width=0.8, uv=stem, light=glow)
        els.append(e)
        els += crossed_bell(sc, tip, "pipe", pipe, 2.6, 3.2, core=GHOST[1:4], light=glow)
        els.append(upright(sc, "scale", leaf(GHOST, "lance", seed=4, vein=False), (x + 0.5, z), 1.0, height * 0.4, height * 0.4 + 1.4,
                           light=glow))
    sc.models["ghost_pipe"] = els
    return sc


# ---------------------------------------------------------------- tall flowers

def rose_bloom(sc, centre, size, bud=False):
    """A black rose: a cup of packed petals, four outer petals curling out round it and green sepals beneath."""
    x, y, z = centre
    c = size / 3
    side = sc.piece("rose_side", 6, 4, rose_core(False, 3))
    top = sc.piece("rose_top", 6, 6, rose_core(True, 4))
    sep = sc.piece("sepals", 9, 9, star(5, CALYX, inner=0.15, width=0.45, seed=6))
    out = [plane_xz(x - size * 0.55, x + size * 0.55, z - size * 0.55, z + size * 0.55, y + 0.1, sep)]
    if bud:
        out.append(cube((x - c * 0.8, y, z - c * 0.8), (x + c * 0.8, y + size * 0.6, z + c * 0.8),
                        {**{s: side for s in SIDES4}, "up": top}))
        return out
    out.append(cube((x - c, y + 0.2, z - c), (x + c, y + size * 0.5, z + c), {**{s: side for s in SIDES4}, "up": top}))
    petal = leaf(ROSE, "round", seed=8, vein=False)
    pw, ph = size * 0.85, size * 0.6
    uv = sc.piece("rose_petal", max(4, round(pw * TEXELS)), max(3, round(ph * TEXELS)), petal)
    out.append(plane_xy(x - pw / 2, x + pw / 2, y + 0.2, y + 0.2 + ph, z + c, uv, rotation((x, y + 0.2, z + c), "x", 22.5)))
    out.append(plane_xy(x - pw / 2, x + pw / 2, y + 0.2, y + 0.2 + ph, z - c, uv, rotation((x, y + 0.2, z - c), "x", -22.5)))
    out.append(plane_zy(z - pw / 2, z + pw / 2, y + 0.2, y + 0.2 + ph, x + c, uv, rotation((x + c, y + 0.2, z), "z", -22.5)))
    out.append(plane_zy(z - pw / 2, z + pw / 2, y + 0.2, y + 0.2 + ph, x - c, uv, rotation((x - c, y + 0.2, z), "z", 22.5)))
    return out


def black_rose():
    sc = Sculpt("black_rose", 21)
    stem = sc.piece("stem", 2, 24, strip(pal("1a2214", "263220", "34442a", "465a36"), 8, knots=3))
    rose_leaf = leaf(pal("101c12", "18291a", "223824", "2f4a30", "40603e"), "oval", seed=50, wavy=0.25)
    els = []
    e, t = segment((8, 0, 8), 16, width=1.1, uv=stem)
    els.append(e)
    e, t1 = segment((8, 16, 8), 10.8, width=1.0, uv=stem)
    els.append(e)
    e, t = segment((8, 5, 8), 12, "z", 22.5, width=0.9, uv=stem)
    els.append(e)
    e, t2 = segment((t[0], 16, t[2]), 6.6, width=0.85, uv=stem)
    els.append(e)
    e, t = segment((8, 8, 8), 9, "x", 22.5, width=0.85, uv=stem)
    els.append(e)
    e, t3 = segment((t[0], 16.1, t[2]), 3.8, width=0.8, uv=stem)
    els.append(e)
    e, t = segment((8, 12, 8), 6, "z", -22.5, width=0.85, uv=stem)
    els.append(e)
    e, t4 = segment((t[0], 17.4, t[2]), 7.0, width=0.8, uv=stem)
    els.append(e)
    for y, dirs, length in ((2.6, ("north", "east"), 4.6), (6.8, ("west", "south"), 4.4), (10.6, ("north", "east"), 4.0),
                            (14.0, ("south", "west"), 3.8), (19.0, ("east",), 3.4), (22.0, ("north",), 3.2)):
        for d in dirs:
            els.append(leaf_out(sc, "leaf", rose_leaf, (8, y, 8), d, length, 2.8, 22.5))
    els.append(leaf_out(sc, "leaf", rose_leaf, (t2[0], 19.4, t2[2]), "west", 3.0, 2.4, 22.5))
    els.append(leaf_out(sc, "leaf", rose_leaf, (t4[0], 21.0, t4[2]), "east", 3.0, 2.4, 22.5))
    thorn = sc.piece("thorn", 1, 2, solid(pal("3a1418", "5a2028"), 2))
    for y, (dx, dz) in ((4.0, (0.55, 0)), (9.0, (0, 0.55)), (13.5, (-0.55, 0)), (20.5, (0, -0.55))):
        els.append(cube((8 + dx - 0.25, y, 8 + dz - 0.25), (8 + dx + 0.25 + abs(dx) * 0.6, y + 0.5, 8 + dz + 0.25 + abs(dz) * 0.6),
                        {s: thorn for s in SIDES4 + ("up",)}))
    els += rose_bloom(sc, t1, 5.2)
    els += rose_bloom(sc, t2, 4.4)
    els += rose_bloom(sc, t4, 4.0)
    els += rose_bloom(sc, t3, 2.6, bud=True)
    tall_models(sc, els)
    return sc


def foxglove():
    sc = Sculpt("foxglove", 22)
    stem = sc.piece("stem", 3, 24, strip(pal("3a4a30", "4c6040", "647c52", "7e9866"), 9))
    soft = pal("2c4030", "3c5640", "507050", "688a66", "84a680")
    big = leaf(soft, "oval", seed=60, wavy=0.4, rib=rgb("a8c0a0"))
    side = sc.piece("bell_side", 4, 6, bell_side(FOX, FOX[4], seed=7))
    mouth = sc.piece("bell_mouth", 4, 4, foxglove_mouth())
    bud = sc.piece("bud", 2, 3, solid(pal("6a7a4a", "8a8a5a", "b07a90"), 3))
    els = []
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "big", big, (8, 0.8, 8), d, 6.4, 4.4, 22.5))
    for d in ("ne", "se", "sw", "nw"):
        els.append(leaf_flat(sc, "mid", leaf(soft, "oval", seed=61, wavy=0.3), (8, 0.5, 8), d, 5.2, 3.4))
    e, t = segment((8, 0, 8), 16, width=1.5, uv=stem)
    els.append(e)
    e, t = segment((8, 16, 8), 13, width=1.2, uv=stem)
    els.append(e)
    e, t = segment((8, 29, 8), 2.2, width=0.7, uv=stem)
    els.append(e)
    for y, d, length in ((5.0, "east", 3.6), (5.0, "west", 3.4), (10.0, "north", 3.0), (10.0, "south", 3.2)):
        els.append(leaf_out(sc, "stemleaf", leaf(soft, "lance", seed=62), (8, y, 8), d, length, 1.9, 45))
    bells = ((12.4, "south"), (13.8, "east"), (15.2, "west"), (16.6, "south"), (18.0, "north"), (19.2, "east"),
             (20.6, "south"), (21.8, "west"), (23.0, "east"), (24.2, "south"), (25.2, "north"), (26.2, "west"))
    for i, (y, d) in enumerate(bells):
        length = 3.2 - i * 0.08
        girth = 2.0 - i * 0.05
        els.append(foxglove_bell(y, d, length, girth, side, mouth))
    for x, y, z in ((8, 27.4, 8.9), (7.1, 28.4, 8), (8.9, 29.2, 8), (8, 30.2, 7.2)):
        els.append(cube((x - 0.5, y, z - 0.5), (x + 0.5, y + 1.3, z + 0.5), {s: bud for s in SIDES4 + ("up", "down")}))
    tall_models(sc, els)
    return sc


def foxglove_mouth():
    """A foxglove bell seen into: pale lower lip with dark spots, darker throat."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                edge = x in (0, p.w - 1) or y == 0
                p.put(x, y, FOX[3] if edge else (FOX[5] if y >= p.h // 2 else FOX[1]))
        for x, y in ((1, 2), (2, 3), (1, 3), (2, 2)):
            if (x + y) % 2 == 0:
                p.put(x, y, FOX_SPOT[0])
    return paint


def foxglove_bell(y, direction, length, girth, side, mouth):
    """One foxglove flower: a tube off the stem pointing `direction`, drooping 22.5 degrees, its mouth outward."""
    g = girth / 2
    if direction in ("south", "north"):
        sign = 1 if direction == "south" else -1
        z0, z1 = (8.6, 8.6 + length) if sign > 0 else (7.4 - length, 7.4)
        faces = {"east": side, "west": side, "up": side, "down": side, direction: mouth}
        return cube((8 - g, y, z0), (8 + g, y + girth, z1), faces, rotation((8, y + g, 8.6 if sign > 0 else 7.4), "x", 22.5 * sign))
    sign = 1 if direction == "east" else -1
    x0, x1 = (8.6, 8.6 + length) if sign > 0 else (7.4 - length, 7.4)
    faces = {"north": side, "south": side, "up": side, "down": side, direction: mouth}
    return cube((x0, y, 8 - g), (x1, y + girth, 8 + g), faces, rotation((8.6 if sign > 0 else 7.4, y + g, 8), "z", -22.5 * sign))


def funeral_lily():
    sc = Sculpt("funeral_lily", 23)
    stem = sc.piece("stem", 2, 24, strip(pal("3e5e30", "52763e", "6c924e", "8aae64"), 10))
    lance = leaf(pal("2c4a26", "3c6232", "507c40", "689650", "86b066"), "lance", seed=70)
    tube = sc.piece("tube", 4, 9, lily_tube())
    mouth = sc.piece("mouth", 19, 19, lily_mouth(3))
    anther = sc.piece("anther", 1, 3, solid(ANTHER, 2))
    bud = sc.piece("bud", 3, 7, solid(pal("b8c8a0", "d6dcc4", "eef0e4"), 6))
    els = []
    e, t = segment((8, 0, 8), 16, width=1.25, uv=stem)
    els.append(e)
    e, t = segment((8, 16, 8), 9.2, width=1.1, uv=stem)
    els.append(e)
    for i, y in enumerate((1.5, 4.0, 6.5, 9.0, 11.5, 14.0, 17.0, 19.5)):
        dirs = ("east", "west") if i % 2 == 0 else ("north", "south")
        for d in dirs:
            els.append(leaf_out(sc, "lance", lance, (8, y, 8), d, 5.0 - i * 0.2, 2.1, 45, tex=(5, 12)))
    y = 24.5
    for d in ("south", "east", "west"):
        els += lily_trumpet(y, d, tube, mouth, anther)
    els.append(cube((7.4, y - 0.6, 4.0), (8.6, y + 0.6, 7.6), {s: bud for s in ("east", "west", "up", "down", "north")},
                    rotation((8, y, 7.6), "x", -22.5)))
    tall_models(sc, els)
    return sc


def lily_tube():
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                t = y / max(1, p.h - 1)
                c = LILY_THROAT[0] if t > 0.8 else (LILY[2] if x == p.w - 1 else LILY[3])
                p.put(x, y, c)
    return paint


def lily_trumpet(y, direction, tube, mouth, anther):
    """A lily trumpet pointing `direction`, drooping slightly, with the open flower across its end."""
    out = []
    if direction == "south":
        rot = rotation((8, y, 8.5), "x", 22.5)
        out.append(cube((7.0, y - 1.0, 8.5), (9.0, y + 1.0, 13.0), {s: tube for s in ("east", "west", "up", "down")}, rot))
        out.append(plane_xy(4.8, 11.2, y - 3.2, y + 3.2, 13.0, mouth, rot))
        out.append(cube((7.7, y - 0.35, 13.0), (8.3, y + 0.25, 14.4), {s: anther for s in ("east", "west", "up", "down", "south")}, rot))
        return out
    sign = 1 if direction == "east" else -1
    x0, x1 = (8.5, 13.0) if sign > 0 else (3.0, 7.5)
    rot = rotation((8.5 if sign > 0 else 7.5, y, 8), "z", -22.5 * sign)
    out.append(cube((x0, y - 1.0, 7.0), (x1, y + 1.0, 9.0), {s: tube for s in ("north", "south", "up", "down")}, rot))
    mx = 13.0 if sign > 0 else 3.0
    out.append(plane_zy(4.8, 11.2, y - 3.2, y + 3.2, mx, mouth, rot))
    ax0, ax1 = (mx, mx + 1.4) if sign > 0 else (mx - 1.4, mx)
    out.append(cube((ax0, y - 0.35, 7.7), (ax1, y + 0.25, 8.3), {s: anther for s in ("north", "south", "up", "down", direction)}, rot))
    return out


def asphodel():
    sc = Sculpt("asphodel", 24)
    stem = sc.piece("stem", 2, 24, strip(pal("4a5a3a", "5e7048", "788a5a", "94a670"), 11))
    blade = pal("2e4a2c", "3e6038", "527a48", "6a945a", "88ae72")
    flower = sc.piece("flower", 13, 13, asphodel_star())
    vein_flower = flower
    bud = sc.piece("bud", 2, 3, solid(pal("8a6a5a", "b08a7a", "d0b0a4"), 7))
    els = []
    for i, (yaw, lean) in enumerate(((0, 22.5), (0, -22.5), (90, 22.5), (90, -22.5), (45, 0), (135, 0))):
        els.append(upright(sc, f"strap{i % 2}", strap_arch(blade, 80 + i % 2), (8, 8), 4, 0, 10.5 - (i % 3), yaw=yaw, lean=lean))
    e, t = segment((8, 0, 8), 16, width=1.0, uv=stem)
    els.append(e)
    e, t = segment((8, 16, 8), 13.4, width=0.9, uv=stem)
    els.append(e)
    for h, d in ((18.6, "south"), (19.8, "east"), (21.0, "north"), (22.2, "west"), (23.3, "south"), (24.3, "east"),
                 (25.3, "north"), (26.2, "west"), (27.0, "south")):
        els.append(star_flower(h, d, 3.4, vein_flower))
    for x, y, z in ((8, 27.8, 8.6), (7.4, 28.6, 8), (8.6, 29.3, 8), (8, 30.0, 7.5)):
        els.append(cube((x - 0.45, y, z - 0.45), (x + 0.45, y + 1.1, z + 0.45), {s: bud for s in SIDES4 + ("up",)}))
    tall_models(sc, els)
    return sc


def asphodel_star():
    """An asphodel flower: six pale tepals, each with a brown midvein, round orange anthers."""
    def paint(p):
        star(6, ASPHODEL, centre=[ANTHER[1], ANTHER[2]], inner=0.1, width=0.62, seed=12, centre_r=0.16)(p)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for i in range(6):
            a = i / 6 * math.tau
            for r in range(2, int(p.w / 2) - 1):
                p.put(cx + math.cos(a) * r, cy + math.sin(a) * r, ASPHODEL_VEIN[r % 2])
    return paint


def star_flower(y, direction, size, uv):
    """A starry flower beside a stalk, facing up and out (`direction`)."""
    h = size / 2
    if direction == "south":
        return plane_xy(8 - h, 8 + h, y, y + size, 8.55, uv, rotation((8, y, 8.55), "x", -45))
    if direction == "north":
        return plane_xy(8 - h, 8 + h, y, y + size, 7.45, uv, rotation((8, y, 7.45), "x", 45))
    if direction == "east":
        return plane_zy(8 - h, 8 + h, y, y + size, 8.55, uv, rotation((8.55, y, 8), "z", 45))
    return plane_zy(8 - h, 8 + h, y, y + size, 7.45, uv, rotation((7.45, y, 8), "z", -45))


# ---------------------------------------------------------------- grasses and ferns

def tuft_planes(sc, keys, y0, y1, width=14, lean_keys=()):
    els = []
    for i, yaw in enumerate((0, 45, 90, 135)):
        key, painter, w_tex, h_tex = keys[i % len(keys)]
        els.append(upright(sc, key, painter, (8, 8), width, y0, y1, yaw=yaw, w_tex=w_tex, h_tex=h_tex))
    for key, painter, w_tex, h_tex, yaw, lean in lean_keys:
        els.append(upright(sc, key, painter, (8, 8), width * 0.7, y0, y1 * 0.8, yaw=yaw, lean=lean, w_tex=w_tex, h_tex=h_tex))
    return els


def withered_grass():
    sc = Sculpt("withered_grass", 31)
    a = ("a", blades(DRY, 13, seed=1, droop=0.45, broken=0.35, base_spread=0.85), 28, 20)
    b = ("b", blades(DRY, 11, seed=2, droop=0.5, broken=0.4, base_spread=0.85), 28, 20)
    c = ("c", blades(DRY, 9, seed=3, droop=0.8, broken=0.2), 20, 14)
    sc.models["withered_grass"] = tuft_planes(sc, [a, b], 0, 10, lean_keys=[(*c, 0, 22.5), (*c, 90, -22.5)])
    return sc


def cut_images(w, h, painter, cut):
    """Paints one tall picture and cuts it into its bottom `cut` rows and the rest: (lower, upper)."""
    img = Px(Image.new("RGBA", (w, h), (0, 0, 0, 0)))
    painter(img)
    return img.img.crop((0, h - cut, w, h)), img.img.crop((0, 0, w, h - cut))


def place(sc, key, picture):
    """Places an already painted picture as a piece."""
    return sc.piece(key, picture.width, picture.height, lambda p: p.img.alpha_composite(picture))


def cut_piece(sc, key, w, h, painter, cut):
    lower, upper = cut_images(w, h, painter, cut)
    return place(sc, f"{key}_lower", lower), place(sc, f"{key}_upper", upper)


def tall_withered_grass():
    sc = Sculpt("tall_withered_grass", 32)
    heads = pal("8a7a5a", "b0a07a", "d0c498")
    pictures = {key: cut_images(28, 56, blades(DRY, 18, seed=10 + i, heads=heads, droop=0.4, broken=0.2), 32) for i, key in enumerate("ab")}
    lowers = {key: place(sc, f"{key}_lower", pictures[key][0]) for key in "ab"}
    uppers = {key: place(sc, f"{key}_upper", pictures[key][1]) for key in "ab"}
    els = []
    for i, yaw in enumerate((0, 45, 90, 135)):
        key = "ab"[i % 2]
        els += [_upright_uv(lowers[key], 14, 0, 16, yaw), _upright_uv(uppers[key], 14, 16, 28, yaw)]
    tall_models(sc, els)
    return sc


def _upright_uv(uv, width, y0, y1, yaw, centre=(8, 8)):
    cx, cz = centre
    yaw = yaw % 180
    if 67.5 <= yaw < 157.5:
        rest = yaw - 90
        return plane_zy(cz - width / 2, cz + width / 2, y0, y1, cx, uv, rotation((cx, y0, cz), "y", rest) if rest else None)
    rest = yaw if yaw < 67.5 else yaw - 180
    return plane_xy(cx - width / 2, cx + width / 2, y0, y1, cz, uv, rotation((cx, y0, cz), "y", rest) if rest else None)


def ghost_fern():
    sc = Sculpt("ghost_fern", 33)
    els = []
    straight = frond(FERN, FERN_RIB, 3, pinnae=10)
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "frond", straight, (8, 0.4, 8), d, 7.6, 4.6, 45, tex=(13, 22)))
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "low", frond(FERN, FERN_RIB, 4, pinnae=8, droop=0.4), (8, 0.2, 8), d, 6.4, 4.0, 22.5, tex=(11, 18)))
    for yaw in (45, 135):
        els.append(upright(sc, "pair", fern_pair(5), (8, 8), 13, 0, 8.5, yaw=yaw, w_tex=26, h_tex=17))
    sc.models["ghost_fern"] = els
    return sc


def large_ghost_fern():
    sc = Sculpt("large_ghost_fern", 34)
    els = []
    straight = frond(FERN, FERN_RIB, 7, pinnae=12)
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "frond", straight, (8, 0.5, 8), d, 9.5, 5.2, 45, tex=(11, 20)))
    arc_lo, arc_up = cut_images(24, 40, fern_pair(8), 24)
    rise_lo, rise_up = cut_images(10, 40, frond(FERN, FERN_RIB, 9, pinnae=14), 24)
    lo, rlo = place(sc, "arc_lower", arc_lo), place(sc, "rise_lower", rise_lo)
    up, rup = place(sc, "arc_upper", arc_up), place(sc, "rise_upper", rise_up)
    for yaw in (45, 135):
        els += [_upright_uv(lo, 14, 0, 16, yaw), _upright_uv(up, 14, 16, 26.5, yaw)]
    lo, up = rlo, rup
    for yaw in (0, 90):
        els += [_upright_uv(lo, 5, 1, 16, yaw), _upright_uv(up, 5, 16, 27, yaw)]
    tall_models(sc, els)
    return sc


# ---------------------------------------------------------------- floor plants, moss, ivy

def dead_mans_fingers():
    sc = Sculpt("dead_mans_fingers", 41)
    side = sc.piece("side", 4, 8, finger_side(False, 1))
    tip_side = sc.piece("tip_side", 4, 6, finger_side(True, 2))
    tip = sc.piece("tip", 3, 3, solid(FINGER_TIP, 3))
    mound = sc.piece("mound", 10, 3, solid(SOIL, 4))
    mound_top = sc.piece("mound_top", 10, 10, solid(SOIL, 5, spots=FINGER[:2], spot_count=8))
    els = [cube((5.4, 0, 5.4), (10.6, 0.6, 10.6), {**{s: mound for s in SIDES4}, "up": mound_top})]
    for x, z, h, axis, angle, w in ((6.4, 6.6, 7.4, "z", 22.5, 1.4), (8.6, 7.2, 9.0, None, 0, 1.55), (10.0, 9.4, 6.4, "x", 22.5, 1.3),
                                     (7.0, 9.8, 5.6, "z", 22.5, 1.25), (9.6, 10.8, 3.8, "z", -45, 1.2)):
        rot = rotation((x, 0.3, z), axis, angle) if axis else None
        els.append(column(x, z, 0.3, h * 0.45, w, side, rot=rot, ends=()))
        els.append(column(x, z, h * 0.45, h * 0.62, w * 1.1, side, rot=rot, ends=()))
        els.append(column(x, z, h * 0.62, h, w * 0.86, tip_side, tip, rot=rot))
    sc.models["dead_mans_fingers"] = els
    return sc


GRAVE_MOSS_CLUMPS = ((4.5, 4.5), (11.5, 4.8), (11.2, 11.5), (4.8, 11.2))


def grave_moss():
    sc = Sculpt("grave_moss", 42)
    top = sc.piece("top", 10, 10, moss_top(1))
    top2 = sc.piece("top2", 8, 8, moss_top(2))
    side = sc.piece("side", 10, 3, strip(MOSS, 2, horizontal=True))
    spore = sc.piece("spores", 8, 10, spores(3))
    rng = random.Random(5)
    for n, (cx, cz) in enumerate(GRAVE_MOSS_CLUMPS, start=1):
        els = [cube((cx - 3.0, 0, cz - 2.6), (cx + 2.6, 0.8, cz + 3.0), {**{s: side for s in SIDES4}, "up": top}),
               cube((cx - 2.2, 0.8, cz - 2.0), (cx + 1.8, 1.6, cz + 2.2), {**{s: side for s in SIDES4}, "up": top2}),
               cube((cx - 1.2, 1.6, cz - 1.0), (cx + 1.0, 2.1, cz + 1.2), {**{s: side for s in SIDES4}, "up": top2}),
               cube((cx + 1.0, 0.8, cz - 2.4), (cx + 2.2, 1.3, cz - 1.0), {**{s: side for s in SIDES4}, "up": top2})]
        for dx, dz in ((-0.6, -0.4), (0.7, 0.6)):
            x, z = cx + dx, cz + dz
            els += [plane_xy(x - 1.0, x + 1.0, 1.4, 4.6 + rng.random(), z, spore), plane_zy(z - 1.0, z + 1.0, 1.4, 4.4, x, spore)]
        sc.models[f"grave_moss_{n}"] = els
    return sc


def shroud_moss():
    sc = Sculpt("shroud_moss", 43)
    base = []
    tip = []
    for key, seed in (("full_a", 1), ("full_b", 2)):
        sc.piece(key, 26, 32, wisps(SHROUD, seed, density=0.9))
    for i, yaw in enumerate((0, 45, 90, 135)):
        key = "ab"[i % 2]
        base.append(upright(sc, f"full_{key}", wisps(SHROUD, 1 + i % 2, density=0.9), (8, 8), 13, 0, 16, yaw=yaw, w_tex=26, h_tex=32))
        tip.append(upright(sc, f"tip_{key}", wisps(SHROUD, 3 + i % 2, density=0.75, taper=True), (8, 8), 13, 4, 16, yaw=yaw,
                           w_tex=26, h_tex=24))
    sc.models["shroud_moss"] = base
    sc.models["shroud_moss_tip"] = tip
    return sc


def creeping_ivy():
    sc = Sculpt("creeping_ivy", 44)
    sheet = sc.piece("sheet", 32, 32, ivy_sheet(5))

    def one_leaf(seed):
        def paint(p):
            ivy_leaf(p, (p.w - 1) / 2, (p.h - 1) / 2 + 0.5, p.w / 2 - 0.5, random.Random(seed))
        return paint
    els = [plane_xy(0, 16, 0, 16, 0.15, sheet)]
    for i, (x, y) in enumerate(((2.0, 10.0), (9.0, 12.0), (5.0, 4.0), (11.0, 6.0))):
        uv = sc.piece(f"leaf{i % 2}", 8, 8, one_leaf(i % 2 + 7))
        els.append(plane_xy(x, x + 4, y - 4, y, 0.3, uv, rotation((x + 2, y, 0.3), "x", -22.5 if i % 2 else -45)))
    sc.models["creeping_ivy"] = els
    return sc


# ---------------------------------------------------------------- the mandrake

def mandrake_rosette(sc, base, big, flat, flowers=0, head=None):
    """A mandrake's rosette of broad, crinkled leaves round `base` (four tilted, four lying flat), with a cluster of
    violet bells in the middle and the top of the root showing (`head`: "shoulder" or "peek")."""
    bx, by, bz = base
    crinkle = leaf(MANDRAKE_LEAF, "spoon", seed=90, wavy=0.7)
    els = []
    length, width, tilt = big
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "leaf", crinkle, (bx, by + 0.3, bz), d, length, width, tilt, tex=(8, 14)))
    if flat:
        fl, fw = flat
        for d in ("ne", "se", "sw", "nw"):
            els.append(leaf_flat(sc, "flat", leaf(MANDRAKE_LEAF, "spoon", seed=91, wavy=0.6), (bx, by + 0.15, bz), d, fl, fw, tex=(6, 10)))
    if head:
        side = sc.piece("root_side", 6, 4, root_side(2))
        top = sc.piece("root_top", 6, 6, root_top(3))
        face = sc.piece("peek", 7, 4, peek(4))
        if head == "shoulder":
            els.append(cube((bx - 1.5, by - 0.05, bz - 1.5), (bx + 1.5, by + 1.0, bz + 1.5), {**{s: side for s in SIDES4}, "up": top}))
        else:
            els.append(cube((bx - 1.8, by - 0.05, bz - 1.8), (bx + 1.8, by + 1.8, bz + 1.8),
                            {"north": side, "east": side, "west": side, "south": face, "up": top}))
    flower_side = sc.piece("flower_side", 3, 3, bell_side(MANDRAKE_FLOWER, MANDRAKE_FLOWER[3], seed=5))
    flower_top = sc.piece("flower_top", 4, 4, star(5, MANDRAKE_FLOWER, centre=[rgb("e0d070")], width=0.7, seed=6, centre_r=0.2))
    rng = random.Random(7)
    lift = 1.8 if head == "peek" else 1.0
    for i in range(flowers):
        a = i / max(1, flowers) * math.tau + 0.4
        x, z = bx + math.cos(a) * 0.9, bz + math.sin(a) * 0.9
        y = by + lift + rng.random() * 0.8
        els.append(cube((x - 0.7, y, z - 0.7), (x + 0.7, y + 1.3, z + 0.7), {**{s: flower_side for s in SIDES4}, "up": flower_top}))
    return els


def mandrake():
    """The mandrake crop's four looks, the wild mandrake and the root itself (an item, screaming)."""
    sc = Sculpt("mandrake", 51)
    soil = (8, -1, 8)
    sc.models["mandrake_stage0"] = mandrake_rosette(sc, soil, (2.6, 1.6, 45), None)
    sc.models["mandrake_stage1"] = mandrake_rosette(sc, soil, (4.2, 2.6, 45), (3.0, 2.0))
    sc.models["mandrake_stage2"] = mandrake_rosette(sc, soil, (6.0, 3.6, 22.5), (4.6, 3.0), head="shoulder")
    sc.models["mandrake_stage3"] = mandrake_rosette(sc, soil, (6.8, 4.0, 22.5), (5.2, 3.4), flowers=5, head="peek")
    sc.models["wild_mandrake"] = mandrake_rosette(sc, (8, 0, 8), (6.6, 4.0, 22.5), (5.0, 3.2), flowers=4, head="peek")
    side = sc.piece("root_side", 6, 4, root_side(2))
    top = sc.piece("root_top", 6, 6, root_top(3))
    face = sc.piece("face", 10, 12, root_face(5))
    limb = sc.piece("limb", 2, 6, strip(ROOT, 8))
    els = [cube((5.5, 3, 6), (10.5, 9, 10), {"south": face, "north": side, "east": side, "west": side, "up": top, "down": side}),
           cube((6, 9, 6.5), (10, 10, 9.5), {**{s: side for s in SIDES4}, "up": top}),
           cube((6.25, 2, 6.5), (9.75, 3, 9.5), {**{s: side for s in SIDES4}, "down": side})]
    for x, angle in ((6.9, -22.5), (9.1, 22.5)):
        els.append(column(x, 8, -0.4, 2.2, 1.1, limb, rot=rotation((x, 2.2, 8), "z", angle), ends=("down",)))
    els.append(cube((3.0, 6.0, 7.55), (5.6, 6.9, 8.45), {s: limb for s in ("north", "south", "up", "down", "west")},
                    rotation((5.6, 6.45, 8), "z", -22.5)))
    els.append(cube((10.4, 6.0, 7.55), (13.0, 6.9, 8.45), {s: limb for s in ("north", "south", "up", "down", "east")},
                    rotation((10.4, 6.45, 8), "z", 22.5)))
    crinkle = leaf(MANDRAKE_LEAF, "spoon", seed=90, wavy=0.7)
    for d in ("north", "south", "east", "west"):
        els.append(leaf_out(sc, "leaf", crinkle, (8, 10, 8), d, 4.4, 2.6, 45, tex=(8, 14)))
    flower_side = sc.piece("flower_side", 3, 3, bell_side(MANDRAKE_FLOWER, MANDRAKE_FLOWER[3], seed=5))
    flower_top = sc.piece("flower_top", 4, 4, star(5, MANDRAKE_FLOWER, centre=[rgb("e0d070")], width=0.7, seed=6, centre_r=0.2))
    for x, z in ((7.4, 7.6), (8.8, 8.6)):
        els.append(cube((x - 0.7, 10.6, z - 0.7), (x + 0.7, 11.9, z + 0.7), {**{s: flower_side for s in SIDES4}, "up": flower_top}))
    sc.models["mandrake_root"] = els
    return sc


BUILDERS = {
    "spider_lily": spider_lily, "snowdrop": snowdrop, "deadly_nightshade": deadly_nightshade, "bleeding_heart": bleeding_heart,
    "ghost_pipe": ghost_pipe, "black_rose": black_rose, "foxglove": foxglove, "funeral_lily": funeral_lily, "asphodel": asphodel,
    "withered_grass": withered_grass, "tall_withered_grass": tall_withered_grass, "ghost_fern": ghost_fern,
    "large_ghost_fern": large_ghost_fern, "dead_mans_fingers": dead_mans_fingers, "grave_moss": grave_moss,
    "shroud_moss": shroud_moss, "creeping_ivy": creeping_ivy, "mandrake": mandrake,
}

_built = {}


def build(name):
    """The plant's Sculpt (built once)."""
    if name not in _built:
        # The graveyard flora is painted in the clean style: no per-pixel random tones (tools/flora_art.py QUIET).
        quiet, fa.QUIET = fa.QUIET, True
        try:
            _built[name] = BUILDERS[name]()
        finally:
            fa.QUIET = quiet
    return _built[name]
