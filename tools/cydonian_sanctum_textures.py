"""Textures for the Cydonian Sanctum's lair-only blocks (tools/cydonian_sanctum.py, docs/features/cydonian-sanctum.md):
the Red Mist, the star stones (rust-red rock carved with the Ancient Altar's ring and red star), the star dials (a disc of
night sky in a bronze ring, one face for each constellation), the Star Door (red ashlar, three constellations carved in
it, dark or burning) and the dust vent; and the Altar Keystone's icon (its map, tools/item_icons/altar_keystone.txt).

All but the dust vent are painted here by code, 16 x 16, in the manner of the Ancient Altar's (tools/adam_ondi_ahman_
textures.py): flat tones, light from the top left, the patterns laid out whole. The dust vent is the owner's sulfur vent
from the supplied library (art/owner-library/originals/Blocks/Guns/block/sulfur_vent.png), copied unchanged: its rust-red
rock and dark mouth are Mars's already. No Mojang texture is read, traced or copied. Called from tools/lair_textures.py.
"""
import math
from pathlib import Path

from PIL import Image

from adam_ondi_ahman_textures import RING_RADIUS, star_cells
from cydonian_sanctum import CONSTELLATIONS, DOOR_FIGURES
import item_icons
from lair_layout import hash01

ROOT = Path(__file__).resolve().parents[1]
VENT_SOURCE = ROOT / "art" / "owner-library" / "originals" / "Blocks" / "Guns" / "block" / "sulfur_vent.png"


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


ROCK = pal("4a1e14", "5e2818", "74321e", "8a3e24", "a04c2c", "b85e36")     # rust-red rock, darkest to lightest
GROOVE = pal("240c08", "34120c")                                             # carving in shadow
STAR_RED = pal("8a1c10", "b8301a", "de4a26", "f2703a")                       # the carved star's red, brighter than the rock
SKY = pal("120a10", "1c1018", "281620")                                      # a dial's night sky
BRONZE = pal("5a3a1a", "7e5428", "a8743a", "cc9a52")                         # a dial's ring
STARLIGHT = pal("8a7a5a", "d8c890", "fff4c8")                                # a star: dim, bright, brightest
STARLINE = (70, 54, 74, 255)                                                 # the faint line between a dial's stars
EMBER = pal("7a2010", "c84a1c", "ff8a3a", "ffd27a")                          # a lit figure: line, glow, star, core
MIST = [(150, 52, 36, 70), (178, 66, 42, 100), (206, 86, 52, 130), (232, 112, 70, 165)]

# The constellations, as stars (x, y) on a 16 x 16 face and the lines between them (pairs of star indices). The first
# star of each is its brightest.
STARS = {
    "plough": ([(12, 7), (3, 4), (5, 5), (7, 6), (9, 7), (9, 10), (12, 10)],
               [(1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 0), (0, 4)]),
    "hunter": ([(5, 4), (8, 2), (11, 4), (6, 8), (8, 8), (10, 8), (5, 12), (11, 12)],
               [(1, 0), (1, 2), (0, 3), (2, 5), (3, 4), (4, 5), (3, 6), (5, 7)]),
    "lyre": ([(5, 3), (7, 6), (10, 6), (9, 10), (6, 10)],
             [(0, 1), (1, 2), (2, 3), (3, 4), (4, 1)]),
    "swan": ([(8, 2), (8, 6), (8, 13), (4, 8), (12, 5)],
             [(0, 1), (1, 2), (3, 1), (1, 4)]),
    "scorpion": ([(4, 3), (5, 5), (6, 7), (7, 9), (9, 11), (11, 12), (12, 10), (11, 8)],
                 [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7)]),
    "crown": ([(8, 11), (3, 6), (4, 8), (6, 10), (10, 10), (12, 8), (13, 6)],
              [(1, 2), (2, 3), (3, 0), (0, 4), (4, 5), (5, 6)]),
}


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def line_cells(a, b):
    """The cells on the line from a to b (Bresenham), both ends included."""
    (x0, y0), (x1, y1) = a, b
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    out = []
    while True:
        out.append((x0, y0))
        if (x0, y0) == (x1, y1):
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def rock(seed):
    """Rust-red rock: mottled, lighter towards the top left, a few dark pits and pale flecks."""
    img = new()
    for y in range(16):
        for x in range(16):
            shade = 2 + (1 if hash01(x, y, seed) > 0.6 else 0) - (1 if hash01(x, y, seed + 1) > 0.78 else 0)
            if x + y < 6:
                shade += 1
            elif x + y > 25:
                shade -= 1
            img.putpixel((x, y), ROCK[max(0, min(5, shade))])
    for x, y in ((3, 11), (12, 4), (9, 13), (5, 6)):
        img.putpixel(((x + seed) % 16, y), ROCK[0])
    for x, y in ((2, 3), (10, 9), (14, 1)):
        img.putpixel(((x + seed) % 16, y), ROCK[5])
    return img


def star_stone():
    """A star stone's face: rust-red rock carved with the Ancient Altar's ring and its red star, the grooves shadowed."""
    img = rock(61)
    star = star_cells()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if RING_RADIUS[0] <= d <= RING_RADIUS[1] and (x, y) not in star:
                img.putpixel((x, y), GROOVE[0] if y >= 8 or x >= 8 else GROOVE[1])
    for x, y in star:
        worn = hash01(x, y, 67)
        img.putpixel((x, y), STAR_RED[1] if worn < 0.25 else STAR_RED[2] if worn < 0.8 else STAR_RED[3])
        if (x, y + 1) not in star and y + 1 < 16:
            img.putpixel((x, y + 1), STAR_RED[0])
    return img


def ashlar(seed):
    """Red ashlar: courses of cut stone four high, the joints shadowed below and lit above, staggered."""
    img = rock(seed)
    for y in range(16):
        for x in range(16):
            course = y // 4
            joint_x = (x + (4 if course % 2 else 0)) % 8 == 0
            if y % 4 == 3 or joint_x:
                img.putpixel((x, y), GROOVE[1])
            elif y % 4 == 0:
                img.putpixel((x, y), ROCK[4] if hash01(x, y, seed + 5) > 0.3 else ROCK[3])
    return img


def end():
    return rock(71)


def dial_face(stars):
    """A star dial's face: a disc of night sky in a bronze ring, the constellation's stars on it and faint lines between."""
    img = ashlar(73)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.2:
                img.putpixel((x, y), SKY[1] if hash01(x, y, 79) > 0.2 else SKY[2])
            elif d <= 7.6:
                lit = (x + y) < 15
                img.putpixel((x, y), BRONZE[3 if lit and d < 7.0 else 2 if lit else 1 if d < 7.0 else 0])
    points, lines = STARS[stars]
    for a, b in lines:
        for x, y in line_cells(points[a], points[b])[1:-1]:
            if math.hypot(x - 7.5, y - 7.5) <= 6.2:
                img.putpixel((x, y), STARLINE)
    for i, (x, y) in enumerate(points):
        img.putpixel((x, y), STARLIGHT[2] if i == 0 else STARLIGHT[1])
    return img


def dial_side():
    """A dial's sides: cut red stone with a bronze band round it."""
    img = ashlar(83)
    for x in range(16):
        img.putpixel((x, 1), BRONZE[2])
        img.putpixel((x, 14), BRONZE[1])
    return img


def dial_top():
    return ashlar(89)


def panel(seed):
    """A smooth panel sunk into the stone: a dark bevel round it (lit along its lower right edge), flat rock inside."""
    img = rock(seed)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                continue
            if x == 1 or y == 1:
                img.putpixel((x, y), GROOVE[0])
            elif x == 14 or y == 14:
                img.putpixel((x, y), ROCK[4])
            else:
                img.putpixel((x, y), ROCK[2] if hash01(x, y, seed + 3) > 0.15 else ROCK[1])
    return img


def door_figure(stars, lit):
    """A figure of the Star Door: a sunk panel with the constellation cut into it, its stars pale in their pits; lit, its
    stars burn and its lines glow."""
    img = panel(97)
    points, lines = STARS[stars]
    for a, b in lines:
        for x, y in line_cells(points[a], points[b])[1:-1]:
            img.putpixel((x, y), EMBER[1] if lit else GROOVE[1])
    for i, (x, y) in enumerate(points):
        if lit:
            img.putpixel((x, y), EMBER[3] if i == 0 else EMBER[2])
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[:3] not in (EMBER[2][:3], EMBER[3][:3]):
                    img.putpixel((nx, ny), EMBER[0] if i else EMBER[1])
        else:
            img.putpixel((x, y), ROCK[5])
            if y + 1 < 16:
                img.putpixel((x, y + 1), GROOVE[0])
    return img


def red_mist():
    """The Red Mist: soft see-through bands of red dust drifting across, densest at the middle."""
    img = new()
    for y in range(16):
        band = 0.5 + 0.5 * math.sin(y * 0.9 + 1.1)
        for x in range(16):
            wave = 0.5 + 0.5 * math.sin(x * 0.55 + y * 0.35 + hash01(x, y, 101) * 0.4)
            v = band * 0.55 + wave * 0.45
            idx = 0 if v < 0.35 else 1 if v < 0.6 else 2 if v < 0.8 else 3
            img.putpixel((x, y), MIST[idx])
    return img


def dust_vent():
    """The owner's sulfur vent, unchanged (its model takes its faces from the 32 x 32 sheet)."""
    return Image.open(VENT_SOURCE).convert("RGBA")


def cydonian_sanctum_textures():
    out = {
        ("block", "red_mist"): red_mist(),
        ("block", "star_stone"): star_stone(),
        ("block", "star_stone_end"): end(),
        ("block", "star_dial_side"): dial_side(),
        ("block", "star_dial_top"): dial_top(),
        ("block", "star_door"): ashlar(97),
        ("block", "dust_vent"): dust_vent(),
        ("item", "altar_keystone"): item_icons.draw("altar_keystone"),
    }
    for stars in CONSTELLATIONS:
        out[("block", f"star_dial_{stars}")] = dial_face(stars)
    for figure in DOOR_FIGURES:
        out[("block", f"star_door_{figure}")] = door_figure(figure, False)
        out[("block", f"star_door_{figure}_lit")] = door_figure(figure, True)
    return out


if __name__ == "__main__":
    import sys
    sheet = cydonian_sanctum_textures()
    keys = sorted(sheet)
    scale = 8
    image = Image.new("RGBA", (len(keys) * (16 * scale + 8), 32 * scale), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((sheet[key].width * scale, sheet[key].height * scale), Image.NEAREST)
        image.paste(tile, (i * (16 * scale + 8), 0), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "cydonian_sanctum_textures.png")
    print(len(keys), "textures")
