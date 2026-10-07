"""Original textures for the Bat House (fall additions 13) (requires Pillow): weathered dark planks, the slatted front
with a bat painted on it, the guano piled on its tray, and the Bat Guano item. Planks and guano are painted in the
manner of the vanilla blocks (tools/block_style.py): grain and clumps, never per-pixel static.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured.
"""
import block_style as bs
from crop_textures import Canvas, rgb, outline

WOOD = [rgb("2e2218"), rgb("3e2e20"), rgb("4e3a28"), rgb("5e4632")]
GUANO = [rgb("2a2420"), rgb("4a4038"), rgb("8a8478"), rgb("d8d4c8")]


def planks(c, horizontal=True, seed=28101):
    """Weathered dark planks in the manner of vanilla planks: boards with grain streaks, seams and an end joint."""
    bs.planks(WOOD + [rgb("6e5440")], seed, boards=4, vertical=not horizontal)(c)


def wood():
    """Weathered dark planks, running across, a nail head in two of them."""
    c = Canvas()
    planks(c)
    c.px(2, 2, rgb("8a8a8a"))
    c.px(13, 7, rgb("8a8a8a"))
    return c.img


def slats():
    """The front: upright slats with dark gaps, and a small cute bat painted in pale paint."""
    c = Canvas()
    planks(c, horizontal=False)
    pale = rgb("d8ccb0")
    for x, y in ((5, 6), (6, 5), (7, 6), (8, 6), (9, 5), (10, 6), (7, 7), (8, 7), (4, 7), (11, 7), (6, 7), (9, 7)):
        c.px(x, y, pale)
    return c.img


def pile():
    """Guano: dark crumbly clumps like a dark gravel, flecked here and there with white."""
    c = Canvas()
    bs.dirt([rgb("221c19"), GUANO[0], rgb("362e29"), rgb("413832"), GUANO[1]], 28103, pebbles=[GUANO[3], GUANO[2]], count=6)(c)
    return c.img


def guano_item():
    """A small heap of guano: flat dark, lit along its top, with three pale flecks."""
    c = Canvas()
    for y in range(7, 14):
        half = (y - 6) * 0.9
        for x in range(int(8 - half), int(8 + half) + 1):
            c.px(x, y, GUANO[2] if y == 7 or x == int(8 - half) else GUANO[1] if y < 11 else GUANO[0])
    for x, y in ((6, 10), (9, 11), (8, 12)):
        c.px(x, y, GUANO[3])
    outline(c, rgb("15110e"))
    return c.img


def bat_textures():
    return {("block", "bat_house"): wood(), ("block", "bat_house_slats"): slats(), ("block", "bat_guano_pile"): pile(),
            ("item", "bat_guano"): guano_item()}
