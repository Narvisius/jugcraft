"""Original textures for the preserves pantry (fall additions 3) (requires Pillow): the Canning Kettle's speckled blue
enamel, its dark inside and iron jar rack; the Pantry Shelf's oak and beadboard back; for the renderers, the water in the
kettle, a jar's glass (pale, tinted by what is in it as it is drawn), its tin lid and the red gingham cloth of a sealed
jar; and the items: an empty Mason Jar, cider vinegar, and each preserve's jar, plain and sealed with its cloth cap.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. Block textures are 16x16 and opaque; the items are see-through round their shapes.
"""
from agriculture import PANTRY
from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put
from decor13_textures import bottle, icon

ENAMEL = [rgb("1e3a6a"), rgb("2a4e8a"), rgb("3462a2")]
SPECK = rgb("e8eef4")
OAK = [rgb("5a3a1c"), rgb("74502a"), rgb("8a6234"), rgb("a2763e")]
GLASS = [rgb("8aa8a0"), rgb("c8dcd6"), rgb("eef6f2")]
TIN = [rgb("6a6a70"), rgb("9a9aa2"), rgb("c8c8d0")]


def enamel():
    """Graniteware: deep blue enamel flecked with white, the flecks set evenly apart as on vanilla diorite."""
    c = Canvas()
    bs.speckled(ENAMEL, [SPECK], 27101, density=0.07)(c)
    return c.img


def enamel_inside():
    c = Canvas()
    bs.speckled([rgb("16243a"), rgb("1e2e48"), rgb("263856")], [rgb("8a96a8")], 27103, density=0.03)(c)
    return c.img


def rack():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, TIN[:2], 27105, [2, 1])
    for x in range(0, 16, 4):
        for y in range(16):
            c.px(x, y, TIN[2])
    return c.img


def shelf_wood():
    """Honey oak boards."""
    c = Canvas()
    bs.planks(OAK, 27106)(c)
    return c.img


def beadboard():
    """The cupboard's back: narrow upright boards with a bead between them."""
    c = Canvas()
    bs.planks(OAK, 27107, vertical=True, joint=False)(c)
    return c.img


def water():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("3a6aaa"), rgb("4a7cbc"), rgb("5a8ece")], 27108, [2, 3, 2])
    for x, y in ((3, 4), (4, 4), (10, 9), (11, 9), (6, 13)):
        c.px(x, y, rgb("a8c8ec"))
    return c.img


def jar_glass():
    """A jar's side, pale so it takes the colour of what is in it: a highlight down one edge and the moulded rings."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, GLASS[2] if x in (2, 3) else GLASS[1])
    for y in (1, 14):
        for x in range(16):
            c.px(x, y, GLASS[0])
    return c.img


def lid():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, TIN, 27109, [1, 2, 1])
    return c.img


def gingham():
    """Red-and-white check, for the cloth tied over a sealed jar."""
    c = Canvas()
    red, pink, white = rgb("b81e24"), rgb("e48a8a"), rgb("f6f0ea")
    for y in range(16):
        for x in range(16):
            a, b = (x // 2) % 2, (y // 2) % 2
            c.px(x, y, red if a and b else white if not a and not b else pink)
    return c.img


JAR = [
    "................",
    "................",
    "................",
    "................",
    ".....LLLLLL.....",
    ".....TTTTTT.....",
    "....GCCCCCCG....",
    "....GHCCCCCG....",
    "....GHCCCCCG....",
    "....GHCCDCCG....",
    "....GCCCCCCG....",
    "....GCCCCDCG....",
    "....GCCCCCCG....",
    ".....GGGGGG.....",
    "................",
    "................"]

CLOTH = [
    "................",
    "................",
    "....RWRWRWRW....",
    "...WRWRWRWRWR...",
    "....RWRWRWRW....",
    ".....SSSSSS.....",
    "....R.......W...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"]


def darker(color, by=0.65):
    return tuple(int(v * by) for v in color)


def jar_item(color=None, sealed=False):
    """A Mason Jar: empty glass, or filled with `color` (a few darker bits: fruit, seeds or slices), its tin lid, and once
    sealed a red gingham cloth tied over the lid with string."""
    fill = color or GLASS[1]
    img = icon(JAR, {"L": TIN[1], "T": TIN[2], "G": GLASS[0], "H": GLASS[2] if color is None else tuple(min(255, v + 60) for v in fill),
                     "C": fill, "D": darker(fill) if color else GLASS[1]})
    if sealed:
        for y, row in enumerate(CLOTH):
            for x, ch in enumerate(row):
                colour = {"R": rgb("b81e24"), "W": rgb("f6f0ea"), "S": rgb("d8c8a0")}.get(ch)
                if colour:
                    put(img, x, y, colour)
    return img


def vinegar():
    """Cider vinegar: a pale, cloudy amber with a wisp of the mother in it."""
    img = bottle(rgb("d8b060"), rgb("8a6a3a"), rgb("f0d898"))
    for x, y in ((6, 9), (7, 10), (8, 10)):
        put(img, x, y, rgb("b08a46"))
    return img


def pantry_textures():
    """(kind, name) -> image for every texture of the preserves pantry."""
    out = {
        ("block", "canning_kettle"): enamel(),
        ("block", "canning_kettle_inside"): enamel_inside(),
        ("block", "canning_kettle_rack"): rack(),
        ("block", "pantry_shelf_wood"): shelf_wood(),
        ("block", "pantry_shelf_back"): beadboard(),
        ("entity", "kettle_water"): water(),
        ("entity", "jar_glass"): jar_glass(),
        ("entity", "jar_lid"): lid(),
        ("entity", "jar_cloth"): gingham(),
        ("item", PANTRY["jar"]): jar_item(),
        ("item", PANTRY["vinegar"]): vinegar(),
    }
    for preserve, info in PANTRY["preserves"].items():
        color = ((info["color"] >> 16) & 0xFF, (info["color"] >> 8) & 0xFF, info["color"] & 0xFF)
        out[("item", preserve)] = jar_item(color)
        out[("item", f"{preserve}_sealed")] = jar_item(color, sealed=True)
    return out
