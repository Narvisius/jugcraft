"""Original textures for the corn maze (fall additions 8) (requires Pillow): the gate's weathered posts, its green
pennant with a white arrow, and the finish post's chequered flag. Maze corn uses ripe corn's own textures, and the
posts' hay wraps vanilla's hay bale (referenced by the model, not copied).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. All are 16x16 and opaque.
"""
from crop_textures import Canvas, rgb
import block_style as bs

WOOD = [rgb("5a4630"), rgb("6e563a"), rgb("82684a")]


def post():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WOOD, 31101, [2, 3, 1])
    for x in (3, 9, 13):
        for y in range(16):
            c.px(x, y, WOOD[0])
    return c.img


def pennant():
    """A green pennant with a white arrow pointing on."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("2a8a3a"), rgb("34a046"), rgb("3cb050")], 31102, [1, 3, 2])
    for y in range(4, 12):
        for x in range(4, 9):
            c.px(x, y, rgb("f4f4ec"))
    for i in range(5):
        for y in range(3 + i, 13 - i):
            c.px(9 + i, y, rgb("f4f4ec"))
    return c.img


def chequered():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("f4f4ec") if (x // 4 + y // 4) % 2 == 0 else rgb("1a1a1e"))
    return c.img


def maze_textures():
    return {("block", "corn_maze_post"): post(), ("block", "corn_maze_pennant"): pennant(), ("block", "corn_maze_chequered"): chequered()}
