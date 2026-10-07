"""Original textures for the Halloween festivities (requires Pillow).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang
texture is read, traced or recoloured. All are 16x16. Block models take their UVs from each box's position
(tools/festivity_data.py), so faces are drawn where a box's face lands: the ghost's eyes, the skull's face
and the stand's rosette sit in the window their box's north face shows.
"""
import math
import random

from crop_textures import Canvas, rgb, outline
from halloween_textures import PLANK, shade
import block_style as bs

VELVET = [rgb("2e1240"), rgb("3e1a56"), rgb("4e246c"), rgb("5e3080")]
GOLD = [rgb("8a6416"), rgb("c09228"), rgb("e8c050"), rgb("fbe8a0")]
STONE = [rgb("8e8f8c"), rgb("a2a39f"), rgb("b2b3af"), rgb("c1c2be"), rgb("cfd0cc")]
LICHEN = [rgb("7e8a46"), rgb("98a256")]
SHEET = [rgb("cfd2d8"), rgb("e0e2e6"), rgb("eeeff2"), rgb("fafafb")]
BONE = [rgb("a89c78"), rgb("c4b894"), rgb("dcd2b0"), rgb("ece6cc")]
WAX = [rgb("d8d2c0"), rgb("ece8da"), rgb("f8f6ee")]
SOCKET = rgb("2a1e14")
WEB = (226, 228, 234)


def plain(palette, seed, weights=(1, 2, 3, 2, 1)):
    """A 16x16 of the palette's shades in small clumps, as on vanilla blocks, weighted towards the middle ones."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette, seed, list(weights[:len(palette)]), spread=0.7)
    return c


# ---------------------------------------------------------------- the Judging Stand

def stand_wood():
    c = Canvas()
    bs.planks(PLANK, 7201, vertical=True)(c)
    return c.img


def stand_cloth(hem=False):
    """Purple velvet with a soft diagonal sheen; the hem has a gold fringe along its lowest rows (v 4-5)."""
    nap = bs.grain(16, 16, 7202)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            tone = 2 if (x + y) % 6 < 3 else 1
            v = nap(x, y)
            tone += 1 if v > 0.7 else -1 if v < 0.3 else 0  # the nap, in soft clumps
            c.px(x, y, VELVET[max(0, min(3, tone))])
    if hem:
        for x in range(16):
            c.px(x, 4, GOLD[2 if x % 2 else 1])
            c.px(x, 5, GOLD[1] if x % 2 else GOLD[0])
    return c.img


def stand_rosette():
    """A gold rosette in the window u 6-10, v 1.5-7.5 (its box's north face); transparent elsewhere."""
    c = Canvas()
    for y in range(2, 6):
        for x in range(6, 10):
            d = math.hypot(x - 7.5, y - 3.5)
            if d <= 2.2:
                c.px(x, y, GOLD[3] if d < 0.9 else GOLD[2] if (x + y) % 2 else GOLD[1])
    for y in (6, 7):
        c.px(6, y, VELVET[3])
        c.px(9, y, VELVET[3])
        c.px(7, y, GOLD[1])
        c.px(8, y, GOLD[1])
    return c.img


# ---------------------------------------------------------------- gravestones and other decorations

def gravestone():
    """Pale weathered limestone, as vanilla stone, with a few spots of lichen, so an engraving stands out."""
    c = Canvas()
    bs.stone(STONE, 7204, cracks=2, spread=0.8)(c)
    for x, y in ((2, 3), (12, 6), (5, 12)):
        c.px(x, y, LICHEN[0])
        c.px(x + 1, y, LICHEN[1])
        c.px(x, y + 1, LICHEN[1])
    return c.img


def spun_cobweb():
    """A cobweb of thin threads: eight spokes from the middle and three rings strung between them."""
    c = Canvas()
    cx, cy = 7.5, 7.5
    spokes = [i * math.pi / 4 + 0.2 for i in range(8)]
    c.px(7, 7, WEB)
    c.px(8, 8, WEB)
    for a in spokes:
        for r in [i * 0.5 for i in range(4, 17)]:
            c.px(cx + math.cos(a) * r, cy + math.sin(a) * r, WEB)
    for ring in (3.2, 5.6):
        for i, a in enumerate(spokes):
            b = spokes[(i + 1) % 8] + (2 * math.pi if i == 7 else 0)
            x0, y0 = cx + math.cos(a) * ring, cy + math.sin(a) * ring
            x1, y1 = cx + math.cos(b) * ring, cy + math.sin(b) * ring
            c.line(x0, y0, x1, y1, WEB)
    return c.img


def ghost_sheet(face=False):
    """White cloth; its hem (v 14-15) is ragged. With a face: two black eyes and an open mouth in the head's
    window (u 5-11, v 3-8)."""
    c = plain(SHEET, 7206, weights=(1, 2, 3, 2))
    for x in range(16):
        if x % 3 == 0:
            c.img.putpixel((x, 15), (0, 0, 0, 0))
            if x % 6 == 0:
                c.img.putpixel((x, 14), (0, 0, 0, 0))
    if face:
        for x, y in ((6, 4), (7, 4), (6, 5), (7, 5), (9, 4), (10, 4), (9, 5), (10, 5)):
            c.px(x, y, rgb("141418"))
        c.px(8, 7, rgb("2a2a30"))
    return c.img


def ghost_string():
    c = Canvas()
    for y in range(16):
        c.px(7, y, rgb("d8d4c8"))
        c.px(8, y, rgb("b8b4a8"))
    return c.img


def skull_bone(face=False):
    """Old bone; with a face, the skull's eye sockets, nose and teeth in its north face's window (u 4-12, v 9-16)."""
    c = plain(BONE, 7207, weights=(1, 2, 3, 2))
    if face:
        for x, y in ((5, 10), (6, 10), (5, 11), (6, 11), (9, 10), (10, 10), (9, 11), (10, 11)):
            c.px(x, y, SOCKET)
        c.px(5, 12, shade(BONE[0], 0.8))
        c.px(10, 12, shade(BONE[0], 0.8))
        c.px(7, 13, SOCKET)
        c.px(8, 13, SOCKET)
        for x in range(5, 11):
            c.px(x, 15, BONE[3] if x % 2 else SOCKET)
    return c.img


def candle_wax():
    """White candle wax with a few drips running down."""
    rng = random.Random(7208)
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WAX[:2], 7208, [1, 4], spread=0.6)
    for x in range(0, 16, 3):
        length = rng.randrange(2, 6)
        for y in range(length):
            c.px(x, y, WAX[2])
    return c.img


def wick(lit):
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if lit:
                c.px(x, y, rgb("f8d860") if y < 6 else rgb("e88a20") if y < 11 else rgb("3a2a1a"))
            else:
                c.px(x, y, rgb("2a2420") if (x + y) % 3 else rgb("3a332c"))
    return c.img


# ---------------------------------------------------------------- spooky sweets

def glow_gum_item():
    """Three glowing green gumballs, a white highlight on each."""
    c = Canvas()
    balls = ((5, 10, 3.0, [rgb("4a9a20"), rgb("78d040"), rgb("b8f070")]),
             (10.5, 9.5, 2.8, [rgb("8a9a10"), rgb("c8e040"), rgb("f0fa8a")]),
             (7.5, 5, 2.6, [rgb("2a9a6a"), rgb("50d49a"), rgb("a0f8d0")]))
    for cx, cy, r, colors in balls:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    c.px(x, y, colors[2] if d < r * 0.35 else colors[1] if d < r * 0.75 else colors[0])
        c.px(cx - r * 0.4, cy - r * 0.4, rgb("ffffff"))
    outline(c, rgb("1a3a10"))
    return c.img


def ghost_taffy_item():
    """A white taffy twisted up in wax paper at both ends, a small ghost face on the sweet."""
    c = Canvas()
    for x in range(5, 11):
        for y in range(5, 11):
            c.px(x, y, SHEET[2] if (x + y) % 4 else SHEET[1])
    for i in range(4):
        for y in range(6 + (i > 1), 10 - (i > 1)):
            c.px(1 + i, y, rgb("d8d0b0") if (y + i) % 2 else rgb("ece6cc"))
            c.px(14 - i, y, rgb("d8d0b0") if (y + i) % 2 else rgb("ece6cc"))
    for x, y in ((7, 7), (9, 7), (8, 9)):
        c.px(x, y, rgb("202024"))
    outline(c, rgb("6a6458"))
    return c.img


def fizz_rocks_item():
    """A little heap of sugary crystal pebbles in pink, blue and white, lighter on top."""
    c = Canvas()
    colors = [[rgb("b0407c"), rgb("f07ab8")], [rgb("3268b4"), rgb("7ab4f4")], [rgb("a8a8b8"), rgb("f4f4fa")]]
    for y in range(6, 14):
        half = (y - 5) * 0.8
        for x in range(int(8 - half), int(8 + half) + 1):
            # Pebbles two pixels square, each one colour, lit at its upper left.
            dark, light = colors[(x // 2 + y // 2 * 2) % 3]
            c.px(x, y, light if x % 2 == 0 and y % 2 == 0 else dark)
    outline(c, rgb("3a2a40"))
    return c.img


def licorice_item():
    """A twisted black rope of licorice lying corner to corner, with a purple sheen on its turns."""
    c = Canvas()
    for i in range(24):
        t = i / 23
        x, y = 2 + t * 11.5, 13 - t * 11.5
        twist = (i // 2) % 2
        c.px(x, y, rgb("1a1420"))
        c.px(x + 1, y, rgb("3a2650") if twist else rgb("241a2e"))
        c.px(x, y - 1, rgb("6a4a8a") if twist else rgb("2e223a"))
    outline(c, rgb("0a080c"))
    return c.img


def festivity_textures():
    """(kind, name) -> image for every Halloween festivities texture."""
    return {
        ("block", "judging_stand_wood"): stand_wood(),
        ("block", "judging_stand_cloth"): stand_cloth(),
        ("block", "judging_stand_hem"): stand_cloth(hem=True),
        ("block", "judging_stand_rosette"): stand_rosette(),
        ("block", "gravestone"): gravestone(),
        ("block", "spun_cobweb"): spun_cobweb(),
        ("block", "hanging_ghost"): ghost_sheet(),
        ("block", "hanging_ghost_face"): ghost_sheet(face=True),
        ("block", "hanging_ghost_string"): ghost_string(),
        ("block", "candle_skull_bone"): skull_bone(),
        ("block", "candle_skull_face"): skull_bone(face=True),
        ("block", "candle_skull_wax"): candle_wax(),
        ("block", "candle_skull_wick"): wick(False),
        ("block", "candle_skull_wick_lit"): wick(True),
        ("item", "glow_gum"): glow_gum_item(),
        ("item", "ghost_taffy"): ghost_taffy_item(),
        ("item", "fizz_rocks"): fizz_rocks_item(),
        ("item", "witchs_licorice"): licorice_item(),
    }


if __name__ == "__main__":
    # Writes only these textures (the full set comes from generate_textures.py).
    from pathlib import Path
    root = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures"
    for (kind, name), image in festivity_textures().items():
        path = root / kind / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, optimize=True)
