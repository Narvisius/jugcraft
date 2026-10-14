"""Textures for the Archive of Last Names (tools/crypt_archive.py, docs/features/crypt-archive.md): its four lair-only
blocks and the Writ of Exhumation's icon (its map, tools/item_icons/exhumation_writ.txt). Painted here by code, 16 x 16,
in the Witching Season's clean style: a few flat tones a material, crisp edges, the patterns (a tablet's bevel, a rune,
a seal's rim) laid out whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang
texture is read, traced or copied.

- the name-stone: a tablet of pale limestone, its rune carved large over two lines of lettering. Dark, the carving is
  shadowed; lit, it burns with soul-fire; erased, rune and lettering are scraped away to rough stone. The three runes are
  three shapes (the Sexton's spade, the Crowned's crown, the Scribe's quill), so a name is told by its shape, not its
  colour alone;
- the grave seal: a disc of dark red wax with the same rune pressed into it, on a riveted iron plate; broken, the wax is
  split across and a piece gone;
- the Lectern of Last Names: a foot of crypt stone, a column of bone, a desk of black wood and the archive's book open
  on it;
- the phylactery: an urn of bone banded with gold, soul-fire showing through its three windows; cracked, the windows are
  dark and a crack runs down it.

The models' faces (tools/lair_data.py) take the textures at a texel a pixel: the tablet's face shows 1 to 15 across and
down, the seal's 2 to 14, the urn's body 4 to 12 across and 8 to 15 down its side texture.
"""
from PIL import Image

import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


LIMESTONE = pal("55555c", "6c6c74", "86868e", "a0a0a8", "bcbcc2", "d6d6da")    # the tablet, a joint darkest
GROOVE = pal("26262c", "3a3a42")                                                  # carving in shadow
SOUL = pal("123a4c", "1f6280", "2f93b4", "58c6e2", "a2ecf8", "e8fdff")          # soul-fire: deep to white-hot
WAX = pal("2a080a", "481014", "6a181c", "8e2428", "b23a38", "cc5a50")
IRON = pal("1a1c20", "282c30", "383d42", "4a5056", "626a70")
BONE = pal("6e6656", "8c8470", "aaa28a", "c8c0a6", "e2dbc4", "f6f1e2")
GOLD = pal("4a3006", "7a5410", "a87a1e", "d4a632", "eecc62", "fbeaa8")
WOOD = pal("140e0b", "21170f", "302217", "433022", "5a4230")                   # the desk's black wood
CRYPT = pal("3a3a3e", "4a4a50", "5c5c62", "707078")                             # the lectern's foot
PAGE = pal("8a7a5a", "b4a47e", "d6c8a2", "eadfbe", "f6efda")
INK = pal("1a1414", "2e2626")

# The runes, carved large on a name-stone (7 across, 8 down) and pressed small into a seal (5 by 5).
RUNES = {
    "spade": ["...#...",
              "..###..",
              "...#...",
              "...#...",
              ".#####.",
              ".#####.",
              "..###..",
              "...#..."],
    "crown": ["#..#..#",
              "#..#..#",
              "##.#.##",
              "#######",
              "#######",
              ".......",
              "#######",
              "......."],
    "quill": [".....##",
              "....###",
              "...###.",
              "..###..",
              ".###...",
              ".##....",
              "#......",
              "......."],
}
SMALL_RUNES = {
    "spade": ["..#..",
              "..#..",
              ".###.",
              ".###.",
              "..#.."],
    "crown": ["#.#.#",
              "#####",
              "#####",
              ".....",
              "#####"],
    "quill": ["...##",
              "..##.",
              ".##..",
              "##...",
              "#...."],
}
RUNE_AT = (4, 2)                   # the large rune's top-left corner on the tablet
LETTERS = [(11, "-.--.---.-"), (13, ".--.-.--.")]   # the lettering's rows: a stroke where '-', starting at x 3


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def tablet():
    """Pale limestone, its bevel lit on the top and left and shaded on the others, a few darker flecks of weather."""
    img = new(LIMESTONE[3])
    for i in range(16):
        img.putpixel((i, 0), LIMESTONE[0])
        img.putpixel((0, i), LIMESTONE[0])
        img.putpixel((i, 15), LIMESTONE[0])
        img.putpixel((15, i), LIMESTONE[0])
    for i in range(1, 15):
        img.putpixel((i, 1), LIMESTONE[5])
        img.putpixel((1, i), LIMESTONE[4])
        img.putpixel((i, 14), LIMESTONE[1])
        img.putpixel((14, i), LIMESTONE[2])
    for x, y in ((3, 4), (12, 3), (11, 9), (4, 10), (9, 12)):
        img.putpixel((x, y), LIMESTONE[2])
    return img


def stone_edge():
    """A name-stone's edges: plain limestone, a joint along the middle."""
    img = new(LIMESTONE[2])
    for i in range(16):
        img.putpixel((i, 7), LIMESTONE[1])
        img.putpixel((i, 0), LIMESTONE[3])
    return img


def carve(img, cells, glow):
    """Carves `cells` into the tablet: dark, a shadowed groove with its lower edge caught by the light; lit, soul-fire in
    the groove, white-hot in its middle and deep blue at its edges."""
    for x, y in cells:
        if glow == "lit":
            inner = all((x + dx, y + dy) in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            img.putpixel((x, y), SOUL[5] if inner else SOUL[3])
        else:
            img.putpixel((x, y), GROOVE[0])
            if (x, y + 1) not in cells:
                img.putpixel((x, y + 1), LIMESTONE[5])
    if glow == "lit":
        # A faint glow round the carving.
        for x, y in cells:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if n not in cells and 2 <= n[0] <= 13 and 2 <= n[1] <= 13:
                    img.putpixel(n, SOUL[1])


def rune_cells(rune):
    x0, y0 = RUNE_AT
    return {(x0 + x, y0 + y) for y, row in enumerate(RUNES[rune]) for x, c in enumerate(row) if c == "#"}


def letter_cells():
    return {(3 + i, y) for y, row in LETTERS for i, c in enumerate(row) if c == "-"}


def name_stone(rune, glow):
    """A name-stone's face: the rune and its lettering, dark, lit or erased."""
    img = tablet()
    if glow == "erased":
        # Scraped: rough strokes across where the rune and the lettering were, paler and darker by turns.
        for i in range(-2, 12):
            for t in range(0, 4):
                x, y = 2 + i + t, 3 + t * 3 + (i % 3)
                if 2 <= x <= 13 and 2 <= y <= 13:
                    img.putpixel((x, y), LIMESTONE[4] if (i + t) % 2 else LIMESTONE[2])
        for x, y in ((5, 5), (8, 7), (10, 4), (6, 11), (9, 12), (4, 8)):
            img.putpixel((x, y), GROOVE[1])
        return img
    carve(img, rune_cells(rune), glow)
    carve(img, letter_cells(), glow)
    return img


def plate():
    """The seal's iron plate: a bevel, a rivet in each corner."""
    img = new(IRON[2])
    for i in range(16):
        img.putpixel((i, 0), IRON[3])
        img.putpixel((0, i), IRON[3])
        img.putpixel((i, 15), IRON[0])
        img.putpixel((15, i), IRON[1])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), IRON[4])
        img.putpixel((x + 1, y + 1), IRON[0])
    return img


SEAL_CENTRE = (7.5, 7.5)
SEAL_RADIUS = 4.6


def grave_seal(rune, broken):
    """The plate with a disc of red wax on it, its rim raised, the rune pressed small into its middle; broken, split from
    the top right to the bottom left with the lower right piece gone, the plate showing."""
    img = plate()
    cx, cy = SEAL_CENTRE
    gone = set()
    for y in range(16):
        for x in range(16):
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if r > SEAL_RADIUS:
                continue
            if broken and x - cx > 0.5 and y - cy > -0.5 and (x - cx) + (y - cy) > 1.5:
                gone.add((x, y))
                continue
            if r > SEAL_RADIUS - 1.0:
                img.putpixel((x, y), WAX[4] if x + y < 15 else WAX[2])
            else:
                img.putpixel((x, y), WAX[3])
    for y, row in enumerate(SMALL_RUNES[rune]):
        for x, c in enumerate(row):
            p = (5 + x, 5 + y)
            if c == "#" and p not in gone:
                img.putpixel(p, WAX[1])
    if broken:
        # The crack across what is left, and the plate's shadow where the piece came away.
        for i in range(-4, 5):
            p = (int(cx + i * 0.6 + 0.5), int(cy - i + 0.5))
            if p not in gone and 0 <= p[0] < 16 and 0 <= p[1] < 16:
                img.putpixel(p, WAX[0])
        for x, y in gone:
            img.putpixel((x, y), IRON[1])
    return img


def lectern_base():
    img = new(CRYPT[2])
    for i in range(16):
        img.putpixel((i, 0), CRYPT[3])
        img.putpixel((i, 15), CRYPT[0])
    for x in (0, 5, 10, 15):
        for y in range(16):
            if y % 8 == (4 if x % 10 else 0):
                img.putpixel((x, y), CRYPT[1])
    return img


def lectern_column():
    """A column of bone: knuckled bands, a shaft between them, lit on its left."""
    img = new(BONE[3])
    for y in range(16):
        img.putpixel((0, y), BONE[5])
        img.putpixel((1, y), BONE[4])
        img.putpixel((14, y), BONE[2])
        img.putpixel((15, y), BONE[1])
    for y in (2, 3, 8, 9, 14):
        for x in range(16):
            img.putpixel((x, y), BONE[4] if y % 2 == 0 else BONE[2])
    return img


def lectern_desk():
    """Black wood, its grain in long strokes, edged with a band of gold."""
    img = new(WOOD[2])
    for y in range(16):
        for x in range(16):
            if (x * 3 + y * 7) % 11 == 0:
                img.putpixel((x, y), WOOD[1])
            elif (x * 5 + y) % 13 == 0:
                img.putpixel((x, y), WOOD[3])
    for i in range(16):
        img.putpixel((i, 0), GOLD[3])
        img.putpixel((i, 15), GOLD[1])
        img.putpixel((0, i), GOLD[2])
        img.putpixel((15, i), GOLD[2])
    return img


def lectern_book():
    """The archive's book open from above: two pages of names in close lines, the gutter dark between them, a ribbon of
    soul-blue across the right page."""
    img = new(PAGE[3])
    for y in range(16):
        img.putpixel((7, y), PAGE[1])
        img.putpixel((8, y), PAGE[2])
        img.putpixel((0, y), PAGE[1])
        img.putpixel((15, y), PAGE[1])
    for y in range(2, 14, 2):
        for x in list(range(2, 6)) + list(range(10, 14)):
            if (x + y) % 5:
                img.putpixel((x, y), INK[1] if (x * y) % 3 else INK[0])
    for y in range(16):
        img.putpixel((11, y), SOUL[2] if y % 3 else SOUL[1])
    return img


def lectern_pages():
    """The book's edges: the stacked pages."""
    img = new(PAGE[4])
    for y in range(0, 16, 2):
        for x in range(16):
            img.putpixel((x, y), PAGE[2])
    return img


def phylactery_side(cracked):
    """The urn's body (4 to 12 across, 8 to 15 down): bone, banded with gold at its top and foot, three narrow windows of
    soul-fire between the bands; cracked, the windows dark and a crack running down it."""
    img = new(BONE[3])
    for x in range(16):
        img.putpixel((x, 8), GOLD[4])
        img.putpixel((x, 9), GOLD[2])
        img.putpixel((x, 14), GOLD[3])
    for y in range(10, 14):
        img.putpixel((4, y), BONE[5])
        img.putpixel((11, y), BONE[1])
    for wx in (5, 7, 9):
        for y in (10, 11, 12):
            if cracked:
                img.putpixel((wx + 1, y), GROOVE[0])
            else:
                img.putpixel((wx + 1, y), SOUL[5] if y == 11 else SOUL[3])
    if cracked:
        for x, y in ((8, 8), (8, 9), (7, 10), (7, 11), (6, 12), (7, 13), (6, 14)):
            img.putpixel((x, y), GROOVE[0])
    return img


def phylactery_top():
    """The urn's shoulder from above: bone, a gold ring round its neck."""
    img = new(BONE[3])
    for y in range(4, 12):
        for x in range(4, 12):
            if x in (4, 11) or y in (4, 11):
                img.putpixel((x, y), BONE[2])
    for y in range(6, 10):
        for x in range(6, 10):
            img.putpixel((x, y), GOLD[3] if (x, y) not in ((6, 6), (9, 9)) else GOLD[5])
    return img


def phylactery_gold():
    img = new(GOLD[3])
    for i in range(16):
        img.putpixel((i, 0), GOLD[5])
        img.putpixel((i, 15), GOLD[1])
    return img


def phylactery_bone():
    img = new(BONE[4])
    for i in range(16):
        img.putpixel((0, i), BONE[5])
        img.putpixel((15, i), BONE[2])
    return img


def crypt_archive_textures():
    out = {
        ("block", "name_stone_edge"): stone_edge(),
        ("block", "grave_seal_plate"): plate(),
        ("block", "archive_lectern_base"): lectern_base(),
        ("block", "archive_lectern_column"): lectern_column(),
        ("block", "archive_lectern_desk"): lectern_desk(),
        ("block", "archive_lectern_book"): lectern_book(),
        ("block", "archive_lectern_pages"): lectern_pages(),
        ("block", "phylactery_side"): phylactery_side(False),
        ("block", "phylactery_cracked"): phylactery_side(True),
        ("block", "phylactery_top"): phylactery_top(),
        ("block", "phylactery_gold"): phylactery_gold(),
        ("block", "phylactery_bone"): phylactery_bone(),
        ("item", "exhumation_writ"): item_icons.draw("exhumation_writ"),
    }
    for rune in RUNES:
        for glow in ("dark", "lit", "erased"):
            out[("block", f"name_stone_{rune}_{glow}")] = name_stone(rune, glow)
        out[("block", f"grave_seal_{rune}")] = grave_seal(rune, False)
        out[("block", f"grave_seal_{rune}_broken")] = grave_seal(rune, True)
    return out


if __name__ == "__main__":
    import sys
    sheet = crypt_archive_textures()
    keys = sorted(sheet)
    scale, cols = 8, 6
    rows = (len(keys) + cols - 1) // cols
    image = Image.new("RGBA", (cols * (16 * scale + 8), rows * (16 * scale + 8)), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, ((i % cols) * (16 * scale + 8), (i // cols) * (16 * scale + 8)), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "crypt_archive_textures.png")
    print(len(keys), "textures")
