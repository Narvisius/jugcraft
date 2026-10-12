"""Original 16x16 textures for the Agriculture branch (requires Pillow).

Called from generate_textures.py. Every pixel is drawn here by code from fixed seeds; no
Mojang texture is read, traced or recolored.

Crop textures are drawn for Minecraft's "crop" model: four upright planes, each showing the
whole 16x16 texture, with row 15 at the soil. Tall crops stack one texture per block, and each
block's planes start one pixel lower than the block (as vanilla crops do on 15-pixel farmland),
so the stalk continues seamlessly from one block into the next.
"""
import math
import random

from PIL import Image


def rgb(hex_color):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))


# Leaf greens, darkest to lightest.
LEAF = [rgb("24461c"), rgb("2f5d22"), rgb("3f7a2a"), rgb("539536"), rgb("6fb044"), rgb("97c95e")]
# Drying leaves at harvest time.
DRY = [rgb("6b5328"), rgb("8c6f38"), rgb("ad9048"), rgb("c9ae62"), rgb("dfc987")]
STALK = [rgb("35591f"), rgb("4c7a2b"), rgb("679a3a"), rgb("86b24c")]
HUSK = [rgb("6f8a35"), rgb("93ab4b"), rgb("b5c56a"), rgb("d3dc95")]
KERNEL = [rgb("b9791a"), rgb("dfa42a"), rgb("f2c64a"), rgb("fbe27e")]
SILK = [rgb("6e3419"), rgb("955029"), rgb("bf7a44")]
TASSEL = [rgb("7d6231"), rgb("a88945"), rgb("cfb168"), rgb("ead394")]
HUSK6 = [rgb("4f6428"), rgb("657e30"), rgb("86a044"), rgb("9db457"), rgb("bccb72"), rgb("dbe2a2")]
KERNEL6 = [rgb("8f5a10"), rgb("b27416"), rgb("d69722"), rgb("eab43a"), rgb("f8cf52"), rgb("fde88c")]
SUN_LEAF = [rgb("1f3f17"), rgb("2a5320"), rgb("386b28"), rgb("4a8433"), rgb("5f9c40"), rgb("7fb85a")]
PETAL = [rgb("c07a0c"), rgb("e4a114"), rgb("f7c52a"), rgb("ffe066")]
DISC = [rgb("2b1a0e"), rgb("41291a"), rgb("5c3d22"), rgb("7a5530")]
FLAX_BLUE = [rgb("3d5fb0"), rgb("5c84d6"), rgb("8fb2ee"), rgb("c8dcff")]
BLUE_GREEN = [rgb("2c5037"), rgb("3c6b47"), rgb("548a5c"), rgb("76a978")]
VINE = [rgb("5a2f45"), rgb("7a4160"), rgb("9b5a7c")]
TUBER = [rgb("6e2d2a"), rgb("924239"), rgb("b45e4a"), rgb("d0806a")]
FLESH = [rgb("c2561a"), rgb("e27a26"), rgb("f39c3e"), rgb("fbbf6a")]
POD = [rgb("476f25"), rgb("5f8c30"), rgb("7eaa45"), rgb("a4c867")]
POD_DRY = [rgb("8a7444"), rgb("ab925a"), rgb("c9b27a")]
BLOSSOM = [rgb("b9a3d6"), rgb("e7dcf5"), rgb("ffffff")]
BEAN = [rgb("5c3a22"), rgb("8a6040"), rgb("c49a6c"), rgb("e2c49c")]
WOOD = [rgb("4a3219"), rgb("6b4a26"), rgb("8d6535"), rgb("a8804a")]
FLINT = [rgb("1e1e22"), rgb("35353b"), rgb("4f4f57"), rgb("737380"), rgb("9a9aa6")]
BRONZE = [rgb("58361a"), rgb("8a5c2a"), rgb("b88240"), rgb("deac62"), rgb("f6d696")]
SEED_BLACK = [rgb("1a1a1c"), rgb("2e2e33")]
SEED_STRIPE = [rgb("d9d6cc"), rgb("f2efe6")]
FLAX_SEED = [rgb("5a3416"), rgb("80502a"), rgb("a8743f"), rgb("d2a36a")]
PAPER_RED = rgb("c42f2f")
PAPER_WHITE = rgb("f4efe6")
POP = [rgb("d9c79a"), rgb("f3e8c8"), rgb("fffaf0")]


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def px(self, x, y, color):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < 16 and 0 <= y < 16:
            self.img.putpixel((x, y), color + (255,))

    def get(self, x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            pixel = self.img.getpixel((x, y))
            return pixel if pixel[3] else None
        return None

    def empty(self, x, y):
        return 0 <= x < 16 and 0 <= y < 16 and self.img.getpixel((x, y))[3] == 0

    def line(self, x0, y0, x1, y1, color):
        steps = max(abs(x1 - x0), abs(y1 - y0)) * 2 + 1
        for i in range(int(steps) + 1):
            t = i / steps
            self.px(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, color)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, color)


def ribbon(c, points, widths, palette, midrib=True, overwrite=True):
    """Fills a smooth ribbon (a leaf, husk or petal) around a centreline.

    points: centreline samples [(x, y), ...]; widths: half-width at each sample. Pixels above the
    centreline get the lit shade, pixels below the shaded one, the edges the darkest, and a light
    midrib runs along the widest part."""
    samples = []
    for i in range(len(points) - 1):
        (x0, y0), (x1, y1) = points[i], points[i + 1]
        w0, w1 = widths[i], widths[i + 1]
        steps = 8
        for k in range(steps):
            f = k / steps
            samples.append((x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, w0 + (w1 - w0) * f,
                            (i + f) / (len(points) - 1), x1 - x0, y1 - y0))
    samples.append((*points[-1], widths[-1], 1.0, 1.0, 0.0))
    for py in range(16):
        for px in range(16):
            cx, cy = px + 0.5, py + 0.5
            best = None
            for sx, sy, w, t, dx, dy in samples:
                d = math.hypot(cx - sx, cy - sy)
                if d <= w + 0.3 and (best is None or d - w < best[0] - best[2]):
                    best = (d, t, w, (cx - sx) * dy - (cy - sy) * dx, dx)
            if best is None or (not overwrite and not c.empty(px, py)):
                continue
            d, t, w, side, dx = best
            # side < 0: above the centreline for a leaf pointing right (flip for left-pointing).
            above = side * (1 if dx >= 0 else -1) < 0
            if midrib and d < 0.45 and 0.08 < t < 0.75 and w > 0.9:
                shade = palette[-1]
            elif d > w - 0.35 and not above:
                shade = palette[1]
            elif above:
                shade = palette[4] if t < 0.8 else palette[3]
            else:
                shade = palette[3] if t < 0.8 else palette[2]
            c.px(px, py, shade)


def curve(x0, y0, direction, length, rise, droop, n=10):
    """Centreline of an arching leaf: up and out from (x0, y0), then drooping to its tip."""
    pts = []
    for i in range(n + 1):
        t = i / n
        pts.append((x0 + direction * length * t, y0 - 4 * rise * t * (1 - t) + droop * t * t))
    return pts


def taper(n, base, tip=0.35, n_points=10):
    return [base + (tip - base) * (i / n_points) ** 1.3 for i in range(n_points + 1)]


# ---------------------------------------------------------------- plant parts

def stalk(c, x, top, bottom=15, palette=STALK, nodes=()):
    """A two-pixel stalk: shaded left, lit right, lighter bands at the leaf nodes."""
    for y in range(top, bottom + 1):
        c.px(x, y, palette[1])
        c.px(x + 1, y, palette[2])
    for y in nodes:
        if top <= y <= bottom:
            c.px(x, y, palette[2])
            c.px(x + 1, y, palette[3])


def leaf(c, x0, y0, direction, length, rise, droop, palette=LEAF, width=2):
    """A corn-style strap leaf from the stalk at (x0, y0): arches up by `rise`, droops by `droop`."""
    pts = curve(x0 + 0.5 + direction * 0.5, y0 + 0.5, direction, length, rise, droop)
    ribbon(c, pts, taper(len(pts), 0.55 + 0.45 * width, 0.3), palette)


def ear(c, x, y, direction, ripe=True):
    """An ear of corn leaning out from the stalk at (x, y). Ripe: the husk is peeled back at the
    top, showing golden kernels; unripe: a green husk with a tuft of silk."""
    pts = [(x + direction * 0.4 * i, y - i * 1.1) for i in range(7)]
    ribbon(c, pts, [0.5, 1.2, 1.5, 1.6, 1.4, 1.0, 0.5], HUSK6)
    if ripe:
        ribbon(c, pts[2:], [0.8, 1.2, 1.2, 0.9, 0.5], KERNEL6, midrib=False)
        # Husk flaps folded back on both sides of the kernels.
        bx, by = pts[2]
        c.px(bx - direction * 2, by + 1, HUSK6[5])
        c.px(bx + direction * 2, by, HUSK6[4])
        tx, ty = pts[-1]
        c.px(tx + direction, ty - 1, SILK[1])
        c.px(tx + direction * 1.5, ty - 2, SILK[0])
    else:
        tx, ty = pts[-1]
        c.px(tx, ty - 1, SILK[2])
        c.px(tx + direction, ty - 1, SILK[1])
        c.px(tx + direction, ty - 2, SILK[2])
        c.px(tx + direction * 2, ty - 2, SILK[1])


def tassel(c, x, top, palette):
    """Branched tassel on top of the stalk: a central spike and drooping side branches."""
    for y in range(top, top + 6):
        c.px(x, y, palette[2])
        c.px(x + 1, y, palette[3] if y % 2 else palette[2])
    for side, (bx, by) in ((-1, (x, top + 2)), (1, (x + 1, top + 2)), (-1, (x, top + 4)), (1, (x + 1, top + 4))):
        for i in range(1, 4):
            c.px(bx + side * i, by - 1 + i * 0.6, palette[1 + (i % 2)])
    c.px(x, top - 1, palette[3])


# ---------------------------------------------------------------- corn

# ---------------------------------------------------------------- sunflower

SUN_STEM = [rgb("3e5f22"), rgb("567d2e"), rgb("6f973d"), rgb("8db255")]


def broad_leaf(c, x, y, direction, size, palette=SUN_LEAF):
    """A broad, pointed leaf on a short stalk, drooping a little (sunflowers, beans)."""
    n = 6
    length = 2.2 + size * 1.6
    pts = [(x + 0.5 + direction * length * i / n, y + 0.5 + 0.9 * size * (i / n) ** 2) for i in range(n + 1)]
    ribbon(c, pts, [0.4, 0.4 + 0.45 * size, 0.55 + 0.6 * size, 0.5 + 0.55 * size, 0.35 + 0.4 * size, 0.3 + 0.2 * size, 0.3],
           palette)


def sunflower_stem_part(c, top, bottom=15):
    stalk(c, 7, top, bottom, SUN_STEM)


def sunflower_sprout():
    c = Canvas()
    sunflower_stem_part(c, 11)
    broad_leaf(c, 7, 10, -1, 1)
    broad_leaf(c, 8, 10, 1, 1)
    return c.img


def sunflower_seedling():
    c = Canvas()
    sunflower_stem_part(c, 7)
    broad_leaf(c, 7, 11, -1, 2)
    broad_leaf(c, 8, 11, 1, 2)
    broad_leaf(c, 7, 6, -1, 1)
    broad_leaf(c, 8, 6, 1, 1)
    return c.img


def sunflower_young():
    c = Canvas()
    sunflower_stem_part(c, 2)
    broad_leaf(c, 7, 11, -1, 3)
    broad_leaf(c, 8, 7, 1, 3)
    broad_leaf(c, 7, 3, -1, 2)
    broad_leaf(c, 8, 1, 1, 1)
    return c.img


def sunflower_stem():
    c = Canvas()
    sunflower_stem_part(c, 0)
    broad_leaf(c, 7, 11, -1, 3)
    broad_leaf(c, 8, 6, 1, 3)
    broad_leaf(c, 7, 1, -1, 3)
    return c.img


def sunflower_young_top():
    c = Canvas()
    sunflower_stem_part(c, 8)
    broad_leaf(c, 8, 11, 1, 2)
    broad_leaf(c, 7, 8, -1, 2)
    return c.img


def sunflower_leafy_top():
    c = Canvas()
    sunflower_stem_part(c, 3)
    broad_leaf(c, 8, 11, 1, 3)
    broad_leaf(c, 7, 7, -1, 3)
    broad_leaf(c, 8, 3, 1, 1)
    broad_leaf(c, 7, 3, -1, 1)
    return c.img


def flower_head(c, cx, cy, radius, petals, bloom):
    """A sunflower head facing the viewer: seed disc with a spiral glint, ringed by petals."""
    rng = random.Random(1000 + radius * 10 + petals)
    if petals:
        for i in range(petals):
            angle = 2 * math.pi * i / petals + 0.2
            for r in range(radius, radius + (3 if bloom else 1) + 1):
                x = cx + math.cos(angle) * r
                y = cy + math.sin(angle) * r * 0.95
                c.px(x, y, PETAL[3 if r < radius + 2 else 2] if bloom else PETAL[1])
        # Fill gaps between petals near the disc with deeper yellow.
        for dy in range(-radius - 1, radius + 2):
            for dx in range(-radius - 1, radius + 2):
                d = math.hypot(dx, dy)
                if radius <= d < radius + 1.2 and c.empty(int(round(cx + dx)), int(round(cy + dy))):
                    c.px(cx + dx, cy + dy, PETAL[1])
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            d = math.hypot(dx, dy)
            if d <= radius - 0.2:
                shade = 0 if d > radius - 1.2 else rng.choice([1, 1, 2, 2, 3])
                c.px(cx + dx, cy + dy, DISC[shade])


def sunflower_bud():
    c = Canvas()
    sunflower_stem_part(c, 5)
    broad_leaf(c, 8, 12, 1, 3)
    broad_leaf(c, 7, 8, -1, 3)
    # A closed green bud wrapped in pointed bracts.
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            if abs(dx) + abs(dy) <= 3:
                c.px(7.5 + dx, 3 + dy, SUN_STEM[3] if dy < 0 else SUN_STEM[2])
    for x, y in ((5, 1), (10, 1), (7, 0), (8, 0), (4, 3), (11, 3)):
        c.px(x, y, LEAF[4])
    return c.img


def sunflower_opening():
    c = Canvas()
    sunflower_stem_part(c, 7)
    broad_leaf(c, 8, 12, 1, 3)
    broad_leaf(c, 7, 9, -1, 3)
    flower_head(c, 7.5, 4, 2, 10, False)
    for x, y in ((3, 4), (12, 4), (7, 0), (8, 8)):
        c.px(x, y, LEAF[3])
    return c.img


def sunflower_bloom():
    c = Canvas()
    sunflower_stem_part(c, 10)
    broad_leaf(c, 8, 13, 1, 3)
    broad_leaf(c, 7, 11, -1, 2)
    flower_head(c, 7.5, 5.5, 3, 14, True)
    return c.img


def sunflower_wild():
    """A wild sunflower: branching stems, several small heads."""
    c = Canvas()
    for y in range(6, 16):
        c.px(7, y, SUN_STEM[1])
        c.px(8, y, SUN_STEM[2])
    c.line(7, 10, 3, 6, SUN_STEM[1])
    c.line(8, 9, 12, 5, SUN_STEM[2])
    broad_leaf(c, 7, 12, -1, 2)
    broad_leaf(c, 8, 11, 1, 2)
    flower_head(c, 3, 4, 1, 8, True)
    flower_head(c, 12, 3, 1, 8, True)
    flower_head(c, 7.5, 3, 1, 8, True)
    return c.img


# ---------------------------------------------------------------- beans

def trifoliate(c, x, y, size, palette=LEAF):
    """A bean leaf: three rounded leaflets."""
    for lx, ly in ((x - size, y), (x + size, y), (x, y - size)):
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) < 2 or size > 1:
                    c.px(lx + dx, ly + dy, palette[4] if dy < 0 else palette[3])
        c.px(lx, ly, palette[2])


def bean_bush(c, height, flowers=False, pods=False):
    rng = random.Random(40 + height)
    base = 15
    for x in (5, 8, 10):
        c.line(7.5, base, x, base - height + 2, STALK[1])
    spots = [(3, base - height + 5), (7, base - height + 3), (11, base - height + 5),
             (5, base - height + 8), (10, base - height + 8), (7, base - height + 10)]
    for x, y in spots[: 2 + height // 3]:
        trifoliate(c, x, y, 1 if height < 8 else 2)
    if flowers:
        for x, y in ((4, base - height + 9), (9, base - height + 6), (12, base - height + 10)):
            c.px(x, y, BLOSSOM[1])
            c.px(x + 1, y, BLOSSOM[0])
    if pods:
        for i, (x, y) in enumerate(((4, base - 5), (7, base - 4), (10, base - 6), (12, base - 3), (2, base - 3))):
            palette = POD_DRY if i % 3 == 2 else POD
            for k in range(4):
                c.px(x + (k // 3), y + k, palette[1 + (k % 2)])
            c.px(x, y - 1, palette[0])
    return rng


def bean_stage(stage):
    c = Canvas()
    if stage == 0:
        c.line(7, 15, 7, 11, STALK[1])
        c.px(8, 12, STALK[2])
        broad_leaf(c, 7, 10, -1, 1)
        broad_leaf(c, 8, 10, 1, 1)
    else:
        bean_bush(c, (7, 11, 13)[stage - 1], flowers=stage == 2, pods=stage == 3)
    return c.img


# ---------------------------------------------------------------- sweet potato

def heart(c, x, y, size, palette=LEAF):
    for dy in range(0, size + 1):
        half = size - dy
        for dx in range(-half, half + 1):
            c.px(x + dx, y + dy, palette[4] if dy == 0 else palette[3 if dx < 0 else 2])
    c.px(x - size, y - 1, palette[3])
    c.px(x + size, y - 1, palette[3])
    c.px(x, y, palette[1])


def sweet_potato_stage(stage):
    c = Canvas()
    height = (4, 7, 9, 10)[stage]
    # Purple vines sprawling low over the soil.
    for x0, x1 in ((7, 2), (8, 13), (7, 5), (8, 11))[: 2 + stage // 2]:
        c.line(x0, 15, x1, 15 - height + 3, VINE[1])
    leaves = [(4, 15 - height + 2), (11, 15 - height + 1), (7, 15 - height), (2, 15 - height + 5),
              (13, 15 - height + 5), (6, 15 - height + 5), (9, 15 - height + 4)]
    for x, y in leaves[: 2 + stage * 2]:
        heart(c, x, y, 1 if stage == 0 else 2)
    if stage == 3:
        # Tuber shoulders showing at the soil line.
        for x in (4, 9, 12):
            c.px(x, 15, TUBER[2])
            c.px(x + 1, 15, TUBER[3])
            c.px(x, 14, TUBER[1])
    return c.img


# ---------------------------------------------------------------- flax

def flax_stage(stage):
    """Slender flax: a loose stand of single stems. Sky-blue flowers at stage 2, golden stems with
    round seed bolls when ripe."""
    c = Canvas()
    rng = random.Random(90 + stage)
    stems = [1.5, 3.5, 5.5, 7.5, 9.5, 11.5, 13.5] if stage else [3.5, 6.5, 9.5, 12.5]
    base_height = (4, 9, 13, 13)[stage]
    palette = DRY if stage == 3 else BLUE_GREEN
    for i, x in enumerate(stems):
        height = base_height - rng.choice([0, 1, 2, 2, 3])
        top = 15 - height
        sway = rng.choice([-1.2, -0.6, 0.6, 1.2])
        tip = None
        for y in range(top, 16):
            t = (15 - y) / max(1, height)
            px = x + sway * t * t
            c.px(px, y, palette[1 + (y % 2)])
            tip = (px, y) if tip is None else tip
            if stage >= 1 and (y - top) % 3 == 1 and y < 14:
                c.px(px + (1 if (i + y) % 2 else -1), y, palette[3])
        tx, ty = tip
        if stage == 2 and i % 2 == 0:
            c.px(tx, ty - 1, FLAX_BLUE[3])
            c.px(tx - 1, ty - 1, FLAX_BLUE[1])
            c.px(tx + 1, ty - 1, FLAX_BLUE[1])
            c.px(tx, ty - 2, FLAX_BLUE[2])
            c.px(tx, ty, FLAX_BLUE[0])
        elif stage == 2:
            c.px(tx, ty - 1, BLUE_GREEN[3])
        if stage == 3:
            c.px(tx, ty - 1, DRY[3])
            c.px(tx + 1, ty - 1, DRY[2])
            c.px(tx, ty - 2, DRY[4])
            c.px(tx + 1, ty - 2, DRY[3])
    return c.img


# ---------------------------------------------------------------- items

def outline(c, color):
    """Adds a dark outline around the drawn shape, like vanilla item sprites' shaded edges."""
    solid = [(x, y) for y in range(16) for x in range(16) if c.get(x, y)]
    for x, y in solid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if c.empty(x + dx, y + dy):
                c.px(x + dx, y + dy, color)


def corn_item(roasted=False):
    c = Canvas()
    kernel = [rgb("7a4a14"), rgb("b37a26"), rgb("d9a646"), rgb("eccb7a")] if roasted else KERNEL
    # The cob runs diagonally from lower left to upper right, kernels in a grid.
    for i in range(10):
        cx, cy = 4 + i * 0.85, 12 - i * 0.85
        for w in range(-1, 2):
            x, y = cx - w * 0.7, cy - w * 0.7
            shade = 2 if (i + w) % 2 else 3
            if w == 1:
                shade -= 1
            c.px(x, y, kernel[shade])
            c.px(x + 1, y, kernel[shade - 1 if w == -1 else shade])
    if roasted:
        for x, y in ((6, 9), (9, 6), (11, 5), (7, 11), (12, 3)):
            c.px(x, y, rgb("4a2c0e"))
    else:
        # Husk leaves peeled back at the base.
        for x, y in ((2, 13), (3, 14), (1, 12), (4, 15), (2, 11), (5, 14), (1, 14)):
            c.px(x, y, HUSK[1 + (x + y) % 3])
        c.px(0, 15, HUSK[0])
    outline(c, rgb("3a2a10") if roasted else rgb("5c4212"))
    return c.img


def seeds_item(palette, positions, size=(2, 1), stripe=None):
    c = Canvas()
    for x, y in positions:
        for dy in range(size[1] + 1):
            for dx in range(size[0]):
                c.px(x + dx, y + dy, palette[1] if dy else palette[0])
        c.px(x + size[0] - 1, y, palette[-1])
        if stripe:
            c.px(x, y + size[1], stripe)
    return c.img


def sunflower_seed_item(roasted=False):
    c = Canvas()
    dark = [rgb("3b2616"), rgb("5a3a20")] if roasted else SEED_BLACK
    stripe = [rgb("8f7350"), rgb("b39470")] if roasted else SEED_STRIPE
    for x, y in ((4, 4), (9, 3), (6, 9), (11, 8), (3, 11)):
        # A teardrop seed, pointed at the top, striped along its length.
        c.px(x + 1, y, dark[0])
        for dy in range(1, 4):
            c.px(x, y + dy, dark[1])
            c.px(x + 1, y + dy, stripe[dy % 2])
            c.px(x + 2, y + dy, dark[0])
        c.px(x + 1, y + 4, dark[0])
    return c.img


def beans_item():
    c = Canvas()
    for x, y in ((3, 5), (8, 4), (5, 9), (10, 9), (7, 12)):
        # A kidney bean with speckles (pinto colouring).
        c.rect(x, y, x + 3, y + 1, BEAN[2])
        c.px(x, y + 1, BEAN[1])
        c.px(x + 3, y + 1, BEAN[1])
        c.px(x + 1, y + 2, BEAN[1])
        c.px(x + 2, y + 2, BEAN[1])
        c.px(x + 1, y, BEAN[3])
        c.px(x + 2, y + 1, BEAN[0])
    return c.img


def sweet_potato_item(baked=False):
    c = Canvas()
    skin = [rgb("3f1a14"), rgb("5a261c"), rgb("773526"), rgb("924a34")] if baked else TUBER
    for i in range(11):
        t = i / 10
        cx, cy = 3 + i * 1.0, 12 - i * 0.8
        radius = 2.2 * math.sin(math.pi * (0.08 + t * 0.84))
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                if math.hypot(dx, dy) <= radius:
                    shade = 3 if dx + dy < -1 else (2 if dx + dy < 1 else 1)
                    c.px(cx + dx, cy + dy, skin[shade])
    if baked:
        # Split open along the top: orange flesh and a pat of steam-bright highlight.
        for i in range(2, 9):
            c.px(4 + i, 11 - i * 0.8, FLESH[2])
            c.px(4 + i, 10 - i * 0.8, FLESH[3] if i % 2 else FLESH[1])
    else:
        for x, y in ((6, 10), (9, 8), (11, 6)):
            c.px(x, y, TUBER[0])
    outline(c, rgb("2a0f0c"))
    return c.img


def flax_item():
    c = Canvas()
    # A sheaf of golden stems tied with a twist of fibre.
    for i, x in enumerate(range(4, 12)):
        lean = (x - 7.5) * 0.35
        for y in range(1, 15):
            t = abs(y - 9) / 8
            c.px(x + lean * t * (1 if y < 9 else -1), y, DRY[1 + (i + y) % 3])
    for x in range(4, 12):
        c.px(x, 9, rgb("e9e2cf"))
        c.px(x, 10, rgb("b9ae93"))
    for x in (3, 6, 9, 12):
        c.px(x, 0, DRY[3])
    return c.img


def flax_seeds_item():
    c = Canvas()
    for x, y in ((4, 5), (9, 4), (6, 9), (11, 9), (3, 11), (8, 12)):
        c.px(x, y, FLAX_SEED[2])
        c.px(x + 1, y, FLAX_SEED[3])
        c.px(x, y + 1, FLAX_SEED[1])
        c.px(x + 1, y + 1, FLAX_SEED[0])
    return c.img


def popcorn_item():
    c = Canvas()
    # A striped paper carton heaped with popped kernels.
    for y in range(7, 16):
        inset = (y - 7) // 4
        for x in range(3 + inset, 13 - inset):
            c.px(x, y, PAPER_RED if (x // 2) % 2 else PAPER_WHITE)
    rng = random.Random(55)
    for x, y in ((4, 5), (6, 4), (8, 3), (10, 4), (12, 5), (5, 6), (7, 5), (9, 5), (11, 6), (7, 2), (10, 2)):
        c.px(x, y, POP[2])
        c.px(x + 1, y, POP[1])
        c.px(x, y + 1, POP[rng.choice([0, 1])])
    outline(c, rgb("5c1a1a"))
    return c.img


def stew_item():
    c = Canvas()
    # A wooden bowl like vanilla's stews, filled with pumpkin broth, corn and beans.
    for y in range(8, 14):
        half = 6 - max(0, y - 10)
        for x in range(8 - half, 8 + half):
            c.px(x, y, WOOD[2] if x < 8 else WOOD[1])
    for x in range(2, 14):
        c.px(x, 8, WOOD[3])
    broth = [rgb("c6641c"), rgb("e08a2c"), rgb("f0a748")]
    for x in range(3, 13):
        c.px(x, 7, broth[1 + (x % 2)])
        c.px(x, 6, broth[0 + (x % 3 == 0)] if 4 <= x <= 11 else broth[1])
    for x, y in ((4, 6), (7, 7), (10, 6), (6, 5)):
        c.px(x, y, KERNEL[3])
    for x, y in ((5, 7), (9, 7), (11, 7), (8, 5)):
        c.px(x, y, BEAN[1])
    outline(c, rgb("2e1d0d"))
    return c.img


def sickle_item(blade):
    """A wooden handle at the lower left and a crescent blade hooking up and over to the right."""
    c = Canvas()
    for i in range(6):
        c.px(2 + i * 0.7, 15 - i, WOOD[2])
        c.px(3 + i * 0.7, 15 - i, WOOD[1])
    # Blade: an arc around (9, 8) from the handle's tip, over the top, ending in a point at the right.
    for i in range(26):
        a = math.radians(200 - i * 9.2)
        r_out, r_in = 5.6, 4.2 - 1.4 * (i / 25) ** 2
        for r in (r_out, (r_out + r_in) / 2, r_in):
            x = 9 + math.cos(a) * r
            y = 8 - math.sin(a) * r
            shade = blade[4] if r == r_out else (blade[3] if r != r_in else blade[2])
            if i > 22 and r != r_out:
                continue
            c.px(x, y, shade)
    c.px(6, 10, blade[1])
    outline(c, blade[0])
    return c.img


# ---------------------------------------------------------------- everything

def crop_textures():
    """(kind, name) -> image for every agriculture texture."""
    out = {
        ("block", "sunflower_sprout"): sunflower_sprout(),
        ("block", "sunflower_seedling"): sunflower_seedling(),
        ("block", "sunflower_young"): sunflower_young(),
        ("block", "sunflower_stem"): sunflower_stem(),
        ("block", "sunflower_young_top"): sunflower_young_top(),
        ("block", "sunflower_leafy_top"): sunflower_leafy_top(),
        ("block", "sunflower_bud"): sunflower_bud(),
        ("block", "sunflower_opening"): sunflower_opening(),
        ("block", "sunflower_bloom"): sunflower_bloom(),
        ("block", "sunflower_wild"): sunflower_wild(),
        ("item", "roasted_corn"): corn_item(roasted=True),
        ("item", "popcorn"): popcorn_item(),
        ("item", "sunflower_seeds"): sunflower_seed_item(),
        ("item", "roasted_sunflower_seeds"): sunflower_seed_item(roasted=True),
        ("item", "beans"): beans_item(),
        ("item", "sweet_potato"): sweet_potato_item(),
        ("item", "baked_sweet_potato"): sweet_potato_item(baked=True),
        ("item", "flax"): flax_item(),
        ("item", "flax_seeds"): flax_seeds_item(),
        ("item", "three_sisters_stew"): stew_item(),
        ("item", "flint_sickle"): sickle_item(FLINT),
        ("item", "bronze_sickle"): sickle_item(BRONZE),
    }
    for stage in range(4):
        out[("block", f"bean_stage{stage}")] = bean_stage(stage)
        out[("block", f"sweet_potato_stage{stage}")] = sweet_potato_stage(stage)
        out[("block", f"flax_stage{stage}")] = flax_stage(stage)
    from kitchen_textures import kitchen_textures  # the Kitchen Garden slice builds on the helpers above
    out.update(kitchen_textures())
    from festival_textures import festival_textures  # so do the festival crops
    out.update(festival_textures())
    from larch_textures import larch_textures  # and Alpine Spawn's larch
    out.update(larch_textures())
    from forest_textures import forest_textures  # and the biomes branch's seasonal-forest trees
    out.update(forest_textures())
    from wild_textures import wild_textures  # and its later batches' trees and plants
    out.update(wild_textures())
    from carving_textures import carving_textures  # and pumpkin carving
    out.update(carving_textures())
    from halloween_textures import halloween_textures  # and the Halloween harvest
    out.update(halloween_textures())
    from regatta_textures import regatta_textures  # and the pumpkin regatta and trick-or-treating
    out.update(regatta_textures())
    from festivity_textures import festivity_textures  # and the Halloween festivities
    out.update(festivity_textures())
    from night_textures import night_textures  # and Halloween nights
    out.update(night_textures())
    from decor_textures import decor_textures  # and the Halloween decorations
    out.update(decor_textures())
    from decor2_textures import decor2_textures  # and their second batch
    out.update(decor2_textures())
    from decor3_textures import decor3_textures  # and the graveyard
    out.update(decor3_textures())
    from decor4_textures import decor4_textures  # and the witch's cottage
    out.update(decor4_textures())
    from decor5_textures import decor5_textures  # and the harvest party
    out.update(decor5_textures())
    from decor6_textures import decor6_textures  # and the haunted house and yard
    out.update(decor6_textures())
    from decor7_textures import decor7_textures  # and the haunted house inside
    out.update(decor7_textures())
    from decor8_textures import decor8_textures  # and the mad scientist and monsters
    out.update(decor8_textures())
    from decor9_textures import decor9_textures  # and the yard and porch
    out.update(decor9_textures())
    from decor10_textures import decor10_textures  # and lighting and glow
    out.update(decor10_textures())
    from decor11_textures import decor11_textures  # and party games
    out.update(decor11_textures())
    from decor12_textures import decor12_textures  # and night events
    out.update(decor12_textures())
    from decor13_textures import decor13_textures  # and treats
    out.update(decor13_textures())
    from decor14_textures import decor14_textures  # and costumes
    out.update(decor14_textures())
    from chandlery_textures import chandlery_textures  # and the chandlery
    out.update(chandlery_textures())
    from cider_textures import cider_textures  # and the cider mill
    out.update(cider_textures())
    from pantry_textures import pantry_textures  # and the preserves pantry
    out.update(pantry_textures())
    from crow_textures import crow_textures  # and the crow
    out.update(crow_textures())
    from firework_textures import firework_textures  # and spooky fireworks
    out.update(firework_textures())
    from lantern_textures import lantern_textures  # and the sky lantern festival
    out.update(lantern_textures())
    from feast_textures import feast_textures  # and the harvest feast
    out.update(feast_textures())
    from maze_textures import maze_textures  # and the corn maze
    out.update(maze_textures())
    from ghost_textures import ghost_textures  # and ghost hunting
    out.update(ghost_textures())
    from face_paint_textures import face_paint_textures  # and face paint
    out.update(face_paint_textures())
    from candy_textures import candy_textures  # and the candy kitchen
    out.update(candy_textures())
    from foraging_textures import foraging_textures  # and autumn foraging
    out.update(foraging_textures())
    from bat_textures import bat_textures  # and the Bat House
    out.update(bat_textures())
    from hay_golem_textures import hay_golem_textures  # and the Hay Golem
    out.update(hay_golem_textures())
    from knitting_textures import knitting_textures  # and knitting
    out.update(knitting_textures())
    from pie_textures import pie_textures  # and pie baking
    out.update(pie_textures())
    from spirit_board_textures import spirit_board_textures  # and the Spirit Board
    out.update(spirit_board_textures())
    from turkey_textures import turkey_textures  # and wild turkeys
    out.update(turkey_textures())
    from theremin_textures import theremin_textures  # and the theremin
    out.update(theremin_textures())
    from broom_textures import broom_textures  # and the flying broomstick
    out.update(broom_textures())
    from werewolf_textures import werewolf_textures  # and full-moon werewolves
    out.update(werewolf_textures())
    from squirrel_textures import squirrel_textures  # and squirrels and acorns
    out.update(squirrel_textures())
    from pumpkling_textures import pumpkling_textures  # and the Pumpkling
    out.update(pumpkling_textures())
    from midway_textures import midway_textures  # and the fall fair midway
    out.update(midway_textures())
    from ferris_wheel_textures import ferris_wheel_textures  # and the Ferris wheel
    out.update(ferris_wheel_textures())
    from pinata_textures import pinata_textures  # and the piñata party
    out.update(pinata_textures())
    from hot_air_balloon_textures import hot_air_balloon_textures  # and the hot-air balloon fiesta
    out.update(hot_air_balloon_textures())
    from ofrenda_textures import ofrenda_textures  # and the ofrenda
    out.update(ofrenda_textures())
    from graveyard_textures import graveyard_textures  # and the graveyard pack
    out.update(graveyard_textures())
    from flora_data import flora_textures  # and the graveyard flora
    out.update(flora_textures())
    from decor15_data import textures as churchyard_textures  # and the churchyard's ornaments
    out.update(churchyard_textures())
    from decor16_data import textures as haunted_house_textures  # and the haunted house's props
    out.update(haunted_house_textures())
    from decor17_data import textures as witchs_workshop_textures  # and the witch's workshop
    out.update(witchs_workshop_textures())
    from decor18_data import textures as crypt_textures  # and the crypt and the ossuary
    out.update(crypt_textures())
    from decor19_data import textures as laboratory_textures  # and the laboratory, the larder and the dining room
    out.update(laboratory_textures())
    from decor20_data import textures as pumpkin_night_textures  # and Pumpkin Night
    out.update(pumpkin_night_textures())
    from lair_textures import lair_textures  # and the lairs' blocks, the Mourning Wreath and the Death Knell
    out.update(lair_textures())
    from vesperine_art import vesperine_textures  # and Vesperine, her skulls, thralls and scythe, and her loot
    out.update(vesperine_textures())
    from tatterlace_art import tatterlace_textures  # and Madame Tatterlace, her thimbles, spools, egg sacs and brood, and her loot
    out.update(tatterlace_textures())
    from yeti_king_art import yeti_king_textures  # and the Yeti King, his whelps, blocks of ice, icicles and spikes, and his loot
    out.update(yeti_king_textures())
    from cinder_tyrant_art import cinder_tyrant_textures  # and the Cinder Tyrant, his Cinderlings, gobs and cinders, and his loot
    out.update(cinder_tyrant_textures())
    from orchard_textures import orchard_textures  # and the orchards' fruit trees
    out.update(orchard_textures())
    from cake_art import cake_textures  # and the cakes, rebuilt from the owner's drawing of them
    out.update(cake_textures())
    from fruit_crop_textures import fruit_crop_textures  # and the fruit crops' bushes, fruit and seeds
    out.update(fruit_crop_textures())
    from pie_tart_art import pie_tart_textures  # and the owner's pies and tarts, rebuilt from their drawing of them
    out.update(pie_tart_textures())
    from milkshake_art import milkshake_textures  # and the owner's milkshakes, read off their drawing of them
    out.update(milkshake_textures())
    from vegetable_textures import vegetable_textures  # and the garden's vegetables, herbs and spices
    out.update(vegetable_textures())
    from herb_textures import herb_textures
    out.update(herb_textures())
    from spice_textures import spice_textures
    out.update(spice_textures())
    from wood_style import wood_textures  # and, last, every tree's woods and leaves, after the owner's paintings
    out.update(wood_textures())
    return out
