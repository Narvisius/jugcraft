"""Original textures for the fifth batch of Halloween decorations, the harvest party (requires Pillow): the Bobbing
for Apples Tub and its apples, the Pumpkin Crate and its label, the Hay Bale Seat, the Autumn Wreath (leaves, corn and
four colours of mums) and the red, orange and yellow Leaf Piles.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang texture is
read, traced or recoloured. Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a
short palette in small clumps, never a random colour at every pixel, wood as planks and straw in streaks. Block
textures are 16x16 and fully opaque.
"""
import math
import random

from crop_textures import Canvas, rgb
from halloween_textures import shade
import block_style as bs

STAVE = [rgb("6a4628"), rgb("7c5432"), rgb("8c623c"), rgb("9c7046")]
BAND = [rgb("2c2c32"), rgb("3a3a42"), rgb("4a4a54"), rgb("62626c")]
WATER = [rgb("2c5a9a"), rgb("3a6cb0"), rgb("4c80c4"), rgb("8ab8e8")]
APPLE = [rgb("7a1010"), rgb("a01818"), rgb("c42a20"), rgb("e05a3c")]
STEM = [rgb("3e2a14"), rgb("5a3e1e")]
CRATE = [rgb("a07a4a"), rgb("b48c58"), rgb("c49c66"), rgb("d2ac76")]
POST = [rgb("6a4a2a"), rgb("7a5634"), rgb("8a643e")]
PUMPKIN_PAINT = [rgb("c85a10"), rgb("e8761c")]
STRAW = [rgb("9a7a2a"), rgb("b8963a"), rgb("d0ae4e"), rgb("e4c66a"), rgb("f0d888")]
TWINE = [rgb("6a5030"), rgb("8a6a40"), rgb("a4845a")]
WREATH_LEAVES = [rgb("6a2a10"), rgb("9a3a14"), rgb("c4561a"), rgb("d8822a"), rgb("8a6a1c"), rgb("5a3a14")]
KERNELS = [rgb("7a1e1e"), rgb("c49a2a"), rgb("5a2a6a"), rgb("e0c870"), rgb("a0401a"), rgb("3a2a4a")]
HUSK = [rgb("b8a070"), rgb("cfb888"), rgb("e0cca0")]
MUMS = {"yellow": [rgb("a07a10"), rgb("d0a81c"), rgb("f0d040"), rgb("fff08a")],
        "orange": [rgb("a04a0a"), rgb("d06a14"), rgb("f08c2a"), rgb("ffbc6a")],
        "red": [rgb("6a1010"), rgb("9a1c1c"), rgb("c43030"), rgb("e86a5a")],
        "purple": [rgb("4a1a5a"), rgb("6a2a84"), rgb("8a44aa"), rgb("b884d4")]}
PILES = {"red": [rgb("4a1a10"), rgb("7a1e14"), rgb("a02a18"), rgb("c43c1e"), rgb("8a5a1c"), rgb("d0602a")],
         "orange": [rgb("5a2a10"), rgb("9a4a14"), rgb("c86a1a"), rgb("e08a26"), rgb("8a6a1c"), rgb("f0a840")],
         "yellow": [rgb("5a4a14"), rgb("9a7a18"), rgb("c8a020"), rgb("e4c03a"), rgb("8a6a1c"), rgb("f4dc6a")]}


# ---------------------------------------------------------------- the bobbing tub

def staves():
    """Upright plank staves, four pixels wide, with dark seams and grain."""
    c = Canvas()
    bs.planks(STAVE, 15101, vertical=True, joint=False)(c)
    return c.img


def band():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BAND[:3], 15111, [2, 3, 1])
    for x in range(0, 16, 5):
        c.px(x + 2, 7, BAND[3])
        c.px(x + 2, 8, BAND[0])
    return c.img


def water():
    c = Canvas()
    rng = random.Random(15121)
    for y in range(16):
        for x in range(16):
            ripple = (math.sin(x * 0.9 + y * 0.5) + math.sin(y * 1.1 - x * 0.3)) / 4 + 0.5
            c.px(x, y, WATER[min(2, int(ripple * 3))])
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        c.px(x, y, WATER[3])
    return c.img


def apple():
    """Glossy red skin: darker low down, a bright highlight high on one side, a few pale freckles."""
    c = Canvas()
    grain = bs.wobble(15131, 1)
    for y in range(16):
        for x in range(16):
            tone = 2 if y < 6 else 1 if y < 12 else 0
            c.px(x, y, APPLE[max(0, tone + min(0, int(grain(x, y))))])
    c.rect(3, 2, 6, 4, APPLE[3])
    for x, y in ((10, 3), (13, 8), (6, 9), (11, 13)):
        c.px(x, y, APPLE[3])
    return c.img


def stem():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, STEM, 15141)
    return c.img


# ---------------------------------------------------------------- the pumpkin crate

def crate_wood():
    """Pale rough-sawn slats running across, with grain and a couple of nail heads."""
    c = Canvas()
    bs.planks(CRATE, 15201, boards=2, joint=False)(c)
    for x in (1, 14):
        for y in (4, 11):
            c.px(x, y, POST[0])
    return c.img


def crate_post():
    c = Canvas()
    bs.planks(POST, 15211, boards=2, vertical=True)(c)
    return c.img


def crate_label():
    """The front slat, stencilled with an orange pumpkin and its stalk."""
    c = Canvas()
    c.img.paste(crate_wood(), (0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.5, (y - 8.5) / 4.0
            if dx * dx + dy * dy <= 1.0:
                rib = abs(x - 7.5) in (1.5, 4.5) and dx * dx + dy * dy < 0.8
                c.px(x, y, PUMPKIN_PAINT[0] if rib else PUMPKIN_PAINT[1])
    c.rect(7, 3, 8, 4, rgb("4a6a1c"))
    return c.img


# ---------------------------------------------------------------- the hay bale seat

def straw(seed, horizontal):
    """Packed straw, as a vanilla hay bale's side: streaks running one way, light and dark."""
    c = Canvas()
    bs.streaks(STRAW, seed, vertical=not horizontal, spread=0.9)(c)
    return c.img


def straw_ends():
    """The cut end of the bale: a soft straw surface dotted evenly with the dark ends of hollow stalks."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, STRAW[1:], 15311)
    for y in range(1, 16, 3):
        for x in range((y // 3) % 2 * 2 + 1, 16, 4):
            c.px(x, y, STRAW[0])
            c.px(x, y - 1, STRAW[4])
    return c.img


def twine():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, TWINE[(x + y) // 2 % 3])  # the twist, two pixels wide
    return c.img


# ---------------------------------------------------------------- the autumn wreath

def wreath_leaves():
    """Overlapping autumn leaves in three tones, rust to gold (a few browned), with dark gaps and bright midribs."""
    c = Canvas()
    rng = random.Random(15401)
    for y in range(16):
        for x in range(16):
            c.px(x, y, WREATH_LEAVES[5])
    for _ in range(22):
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        tone = rng.choice((1, 2, 2, 3, 3, 4))
        angle = rng.uniform(0, math.pi)
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                u = dx * math.cos(angle) + dy * math.sin(angle)
                v = -dx * math.sin(angle) + dy * math.cos(angle)
                if (u / 3.2) ** 2 + (v / 1.8) ** 2 <= 1.0:
                    c.px(x, y, shade(WREATH_LEAVES[tone], 1.15) if abs(v) < 0.4 else WREATH_LEAVES[tone])
    return c.img


def corn():
    """Ornamental corn: rows of plump kernels two pixels square, offset row to row, in red, gold, purple and cream;
    each kernel lit at its upper left and shaded at its lower right."""
    c = Canvas()
    rng = random.Random(15411)
    for row in range(8):
        for k in range(8):
            x, y = k * 2 + row % 2, row * 2
            kernel = KERNELS[rng.randrange(len(KERNELS))]
            c.px(x, y, shade(kernel, 1.2))
            c.px(x + 1, y, kernel)
            c.px(x, y + 1, kernel)
            c.px(x + 1, y + 1, shade(kernel, 0.7))
            if x + 1 == 16:
                c.px(0, y, kernel)
                c.px(0, y + 1, shade(kernel, 0.7))
    return c.img


def husk():
    c = Canvas()
    bs.streaks(HUSK, 15421, spread=0.8)(c)
    return c.img


def mum(colour):
    """A tight mum head: rings of petals round a darker heart."""
    palette = MUMS[colour]
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            ring = int(d * 1.4) % 3
            c.px(x, y, palette[0] if d < 1.6 else palette[1 + ring] if ring < 2 else palette[2])
    return c.img


# ---------------------------------------------------------------- the leaf piles

def pile(colour, top):
    """Heaped fallen leaves of one colour in two tones (with a few browned ones), each shaded along its lower edge,
    over a darker ground; opaque, the side a little darker."""
    palette = PILES[colour]
    c = Canvas()
    rng = random.Random(15501 + list(PILES).index(colour) * 7 + (0 if top else 3))
    for y in range(16):
        for x in range(16):
            c.px(x, y, palette[1])
    for _ in range(30):
        cx, cy = rng.uniform(-1, 17), rng.uniform(-1, 17)
        tone = rng.choice((2, 2, 3, 3, 3, 4))
        angle = rng.uniform(0, math.pi)
        length, width = rng.uniform(2.4, 3.6), rng.uniform(1.3, 1.9)

        def inside(x, y):
            dx, dy = x - cx, y - cy
            u = dx * math.cos(angle) + dy * math.sin(angle)
            v = -dx * math.sin(angle) + dy * math.cos(angle)
            return (u / length) ** 2 + (v / width) ** 2 <= 1.0
        for y in range(16):
            for x in range(16):
                if inside(x, y):
                    # Each leaf is shaded along its lower edge, so it reads against the leaves under it.
                    color = palette[tone] if inside(x, y + 1) else palette[1]
                    c.px(x, y, color if top else shade(color, 0.85))
    return c.img


def decor5_textures():
    """(kind, name) -> image for every texture of the fifth decorations batch."""
    out = {
        ("block", "bobbing_tub_wood"): staves(),
        ("block", "bobbing_tub_band"): band(),
        ("block", "bobbing_tub_water"): water(),
        ("block", "bobbing_tub_apple"): apple(),
        ("block", "bobbing_tub_stem"): stem(),
        ("block", "pumpkin_crate_wood"): crate_wood(),
        ("block", "pumpkin_crate_post"): crate_post(),
        ("block", "pumpkin_crate_label"): crate_label(),
        ("block", "hay_bale_seat_side"): straw(15301, True),
        ("block", "hay_bale_seat_top"): straw(15302, True),
        ("block", "hay_bale_seat_end"): straw_ends(),
        ("block", "hay_bale_seat_twine"): twine(),
        ("block", "autumn_wreath_leaves"): wreath_leaves(),
        ("block", "autumn_wreath_corn"): corn(),
        ("block", "autumn_wreath_husk"): husk(),
    }
    for colour in MUMS:
        out[("block", f"autumn_wreath_mum_{colour}")] = mum(colour)
    for colour in PILES:
        out[("block", f"{colour}_leaf_pile_top")] = pile(colour, True)
        out[("block", f"{colour}_leaf_pile_side")] = pile(colour, False)
    return out
