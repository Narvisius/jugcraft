"""Original textures for the harvest feast (fall additions 7) (requires Pillow): the Harvest Feast Table's honey-oak
top, its darker trestle wood, its orange runner with a border of little leaves, and the cream plate the client's
FeastTableRenderer sets each dish on.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. Wood is drawn as planks and cloth in soft clumps, in the manner of the vanilla blocks
(tools/block_style.py). All are 16x16 and opaque.
"""
from crop_textures import Canvas, rgb
import block_style as bs

OAK = [rgb("8a5e2c"), rgb("a2723a"), rgb("b8864a"), rgb("c89a5a")]
DARK = [rgb("4a2e16"), rgb("5c3a1e"), rgb("6e4626")]
RUNNER = [rgb("c8561a"), rgb("d8661e"), rgb("e47424")]


def top():
    """Planks laid along the table, their joins dark."""
    c = Canvas()
    bs.planks(OAK, 30101)(c)
    return c.img


def wood():
    c = Canvas()
    bs.planks(DARK, 30102, boards=2, vertical=True)(c)
    return c.img


def runner():
    """An orange cloth runner with a gold border and a row of little red and yellow leaves."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, RUNNER, 30103, [2, 3, 1], spread=0.6)
    for x in range(16):
        c.px(x, 0, rgb("e8b040"))
        c.px(x, 15, rgb("e8b040"))
    for x in range(1, 16, 4):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            c.px(x + dx, 6 + dy, rgb("a8241a") if (x // 4) % 2 else rgb("f0c040"))
    return c.img


def plate():
    """A cream plate with a rim and a faint glaze."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("ece4d0"), rgb("f4eedc"), rgb("faf6ea")], 30104, [1, 2, 3])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            c.px(x, y, rgb("c8b890"))
    return c.img


def feast_textures():
    return {("block", "feast_table_top"): top(), ("block", "feast_table_wood"): wood(), ("block", "feast_table_runner"): runner(),
            ("entity", "feast_plate"): plate()}
