"""Original textures for ghost hunting (fall additions 9) (requires Pillow): the Spirit Lantern (a brass lantern, a pale
green flame behind amethyst glass), Ectoplasm (a corked bottle of glowing green ooze), and the soft, pale stuff the
client's RestlessSpiritRenderer shapes a spirit from (tinted and made see-through as it is drawn).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The items are see-through round their shapes.
"""
from agriculture import GHOSTS
from crop_textures import Canvas, rgb
import block_style as bs
from decor13_textures import icon

LANTERN = [
    "................",
    ".......rr.......",
    "......r..r......",
    ".......rr.......",
    ".....BBBBBB.....",
    "....BBBBBBBB....",
    "....BgggggGB....",
    "....BggffggB....",
    "....BgfFFfgB....",
    "....BgfFFfgB....",
    "....BggffggB....",
    "....BgggggGB....",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
    "......rrrr......",
    "................",
]
ECTOPLASM = [
    "................",
    "................",
    "......cccc......",
    "......cccc......",
    ".......ww.......",
    ".......ww.......",
    "......wwww......",
    ".....weeeew.....",
    "....weeEeeew....",
    "....weEeeeew....",
    "....weeeeEew....",
    "....weeeeeew....",
    ".....weeeew.....",
    "......wwww......",
    "................",
    "................",
]


def lantern():
    return icon(LANTERN, {"r": rgb("6a4a1a"), "B": rgb("c8963a"), "g": rgb("b8a8d8"), "G": rgb("d8ccf0"),
                          "f": rgb("6aff9a"), "F": rgb("d8ffe8")})


def ectoplasm():
    return icon(ECTOPLASM, {"c": rgb("9a7a4a"), "w": rgb("d8e8f0"), "e": rgb("5aff8a"), "E": rgb("c8ffd8")})


def spirit():
    """Pale, wispy stuff: near white, with faint streaks running down (tinted and made see-through by the renderer)."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("e2ecf8"), rgb("eef4fc"), rgb("fafcff")], 33101, [2, 3, 2])
    for x in (2, 7, 12):
        for y in range(16):
            if (y + x) % 5:
                c.px(x, y, rgb("d6e2f2"))
    return c.img


def ghost_textures():
    return {("item", GHOSTS["lantern"]): lantern(), ("item", GHOSTS["ectoplasm"]): ectoplasm(), ("entity", GHOSTS["entity"]): spirit()}
