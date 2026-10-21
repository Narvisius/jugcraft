"""The Ancient Altar's textures (tools/adam_ondi_ahman.py): a block of old grey stone, weathered and lichened, older than
the altars it lies on. Its top is carved with a ring and a red star in the ring, the red worn into the carving; its sides
have a carved band. 16 x 16, drawn here by code (docs/NATURAL_TEXTURES.md's manner: flat tones, light from the top
left); no Mojang texture is read, traced or copied.
"""
from PIL import Image

from lair_layout import hash01


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


STONE = pal("4a4842", "5c5a53", "6e6b63", "807d74", "939087", "a8a59b")    # old grey stone, darkest to lightest
GROOVE = pal("2c2a26", "3a3833")                                              # carving in shadow
RED = pal("5a1812", "7e2418", "a4341e", "c64a28")                             # the star's worn red
LICHEN = pal("5e6a3a", "76844a", "93a05c")                                    # lichen and moss

# The star, five-pointed, 9 x 9 (top-left at STAR_AT), and the ring round it (cells at RING_RADIUS from the middle).
STAR = ["....#....",
        "....#....",
        "...###...",
        "#########",
        ".#######.",
        "..#####..",
        "..##.##..",
        ".##...##.",
        ".#.....#."]
STAR_AT = (3, 3)
RING_RADIUS = (6.0, 7.2)
SALT = 1838


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def weathered(seed):
    """The stone: mottled greys, lighter towards the top left, a few dark pits and pale flecks of weather."""
    img = new()
    for y in range(16):
        for x in range(16):
            shade = 2 + (1 if hash01(x, y, seed) > 0.62 else 0) - (1 if hash01(x, y, seed + 1) > 0.78 else 0)
            if x + y < 6:
                shade += 1
            elif x + y > 25:
                shade -= 1
            img.putpixel((x, y), STONE[max(0, min(5, shade))])
    for x, y in ((3, 11), (12, 4), (9, 13), (5, 6), (13, 12)):
        img.putpixel(((x + seed) % 16, y), STONE[0])
    for x, y in ((2, 3), (10, 9), (14, 1)):
        img.putpixel(((x + seed) % 16, y), STONE[5])
    return img


def lichen(img, seed, rows):
    """Lichen creeping in from the lower edge, over the given rows."""
    for y in rows:
        for x in range(16):
            if hash01(x, y, seed + 7) < 0.18 + 0.08 * (y - rows[0]):
                img.putpixel((x, y), LICHEN[int(hash01(x, y, seed + 8) * len(LICHEN))])


def star_cells():
    x0, y0 = STAR_AT
    return {(x0 + x, y0 + y) for y, row in enumerate(STAR) for x, c in enumerate(row) if c == "#"}


def top():
    """The carved face: a ring, and inside it the red star, the red worn and the grooves shadowed below."""
    img = weathered(SALT)
    star = star_cells()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if RING_RADIUS[0] <= d <= RING_RADIUS[1] and (x, y) not in star:
                img.putpixel((x, y), GROOVE[0] if y >= 8 or x >= 8 else GROOVE[1])
    for x, y in star:
        worn = hash01(x, y, SALT + 3)
        img.putpixel((x, y), RED[1] if worn < 0.25 else RED[2] if worn < 0.8 else RED[3])
        if (x, y + 1) not in star and y + 1 < 16:
            img.putpixel((x, y + 1), RED[0])
    return img


def side():
    """A side: weathered stone, a carved band across the middle, lichen at the foot."""
    img = weathered(SALT + 11)
    for x in range(16):
        img.putpixel((x, 6), GROOVE[1])
        img.putpixel((x, 9), GROOVE[1])
        img.putpixel((x, 10), STONE[4])
        if x % 4 == 1:
            img.putpixel((x, 7), GROOVE[0])
            img.putpixel((x + 1, 8), GROOVE[0])
    lichen(img, SALT, range(13, 16))
    return img


def bottom():
    return weathered(SALT + 23)


def adam_ondi_ahman_textures():
    return {
        ("block", "ancient_altar_top"): top(),
        ("block", "ancient_altar_side"): side(),
        ("block", "ancient_altar_bottom"): bottom(),
    }


if __name__ == "__main__":
    import sys
    sheet = adam_ondi_ahman_textures()
    keys = sorted(sheet)
    scale = 16
    image = Image.new("RGBA", (len(keys) * (16 * scale + 8), 16 * scale), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, (i * (16 * scale + 8), 0), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "adam_ondi_ahman_textures.png")
    print(len(keys), "textures")
