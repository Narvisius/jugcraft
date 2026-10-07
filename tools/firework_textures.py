"""Original textures for spooky fireworks (fall additions 5) (requires Pillow): each spooky firework's item (an upright
paper rocket on its stick, banded in its picture's colours), the Show Launcher's stained crate with iron corners, its
painted mortar tubes and their dark mouths, its brass dial at each mode (one pip, a row of three, three rows of three),
the paper of a loaded rocket for the client's ShowLauncherRenderer, and the soft white star of a spark (tinted by its
colour as it is drawn).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. Block textures are 16x16 and opaque; the items and the spark are see-through round their
shapes.
"""
import math

from PIL import Image

from agriculture import FIREWORKS
from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put
from decor13_textures import icon

ROCKET = [
    "................",
    ".......nn.......",
    "......nnnn......",
    ".....nnnnnn.....",
    ".....kkkkkk.....",
    ".....mmmmhm.....",
    ".....ssssss.....",
    ".....mmmmhm.....",
    ".....mmmmhm.....",
    ".....ssssss.....",
    ".....mmmmhm.....",
    ".....kkkkkk.....",
    ".......tt.......",
    ".......tt.......",
    ".......tt.......",
    ".......ff.......",
]
# Each picture's rocket: main, highlight, stripe, nose and rim colours.
ROCKET_COLOURS = {
    "bat": ("7e44c0", "b07ae8", "2a1a3a", "2a1a3a", "1a1020"),
    "pumpkin": ("ff8a1c", "ffb860", "2a1a10", "58c83c", "5a2a08"),
    "ghost": ("f2f4ff", "ffffff", "a8c8ff", "a8c8ff", "6a7890"),
    "skull": ("ede3c4", "fff8e8", "3a3430", "3a3430", "5a5040"),
}
STICK = rgb("8a6234")
FUSE = rgb("ffd040")
STAIN = [rgb("2a1a10"), rgb("3a2416"), rgb("4a2e1c"), rgb("5a3a22")]
IRON = [rgb("3a3a40"), rgb("5a5a62"), rgb("8a8a94")]
ORANGE = [rgb("d8641a"), rgb("ee7a22"), rgb("ff9030")]
BLACK = [rgb("141418"), rgb("1e1e24")]
BRASS = [rgb("8a6a20"), rgb("b08a30"), rgb("d0aa48")]
PIP = rgb("c8281e")


def rocket(shape):
    main, shine, stripe, nose, rim = (rgb(c) for c in ROCKET_COLOURS[shape])
    return icon(ROCKET, {"m": main, "h": shine, "s": stripe, "n": nose, "k": rim, "t": STICK, "f": FUSE})


def crate():
    """Dark stained planks laid across, with iron corner straps and rivets."""
    c = Canvas()
    bs.planks(STAIN, 28101)(c)
    for x in (0, 1, 14, 15):
        for y in range(16):
            c.px(x, y, IRON[1] if x in (1, 14) else IRON[0])
    for y in (2, 7, 12):
        c.px(1, y, IRON[2])
        c.px(14, y, IRON[2])
    return c.img


def crate_top():
    c = Canvas()
    bs.planks(STAIN, 28102, vertical=True)(c)
    return c.img


def tube():
    """A mortar tube painted in orange and black bands (stretched round a narrow tube, so the bands run across)."""
    c = Canvas()
    paints = {id(BLACK): bs.surface(BLACK, 28103, [2, 3, 1][:len(BLACK)], spread=0.6),
              id(ORANGE): bs.surface(ORANGE, 28104, [2, 3, 1][:len(ORANGE)], spread=0.6)}
    for y in range(16):
        band = BLACK if (y // 4) % 2 else ORANGE
        for x in range(16):
            c.px(x, y, paints[id(band)](x, y))
    for x in range(0, 16, 4):
        c.px(x, 1, ORANGE[2])
        c.px(x + 2, 9, ORANGE[2])
    return c.img


def tube_top():
    """A tube's mouth from above: a dark bore inside an orange-painted rim."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c.px(x, y, rgb("0a0a0c") if d < 4.5 else (ORANGE[2] if d < 6.0 else ORANGE[0]) if d < 7.2 else BLACK[0])
    return c.img


def dial(mode):
    """A brass plate with a dark border and the mode's pips in red: one, a row of three, or three rows of three."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BRASS, 28104, [1, 3, 2])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            c.px(x, y, BRASS[0])
    rows = {"sequence": [(7, 7)], "volley": [(3, 7), (7, 7), (11, 7)],
            "finale": [(x, y) for y in (3, 7, 11) for x in (3, 7, 11)]}[mode]
    for x, y in rows:
        c.rect(x, y, x + 1, y + 1, PIP)
    return c.img


def paper():
    """A rocket's paper, pale, with faint wraps (tinted by the renderer)."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("e8e8e8"), rgb("f2f2f2"), rgb("ffffff")], 28105, [1, 2, 3], spread=0.6)
    for y in (3, 8, 13):
        for x in range(16):
            c.px(x, y, rgb("d0d0d0"))
    return c.img


def spark():
    """A soft four-pointed white star, bright at its heart and fading out (tinted by its colour as it is drawn)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            glow = max(0.0, 1.0 - math.hypot(dx, dy) / 6.0)
            ray = max(0.0, 1.0 - min(dx, dy) / 1.2) * max(0.0, 1.0 - max(dx, dy) / 8.0)
            alpha = min(1.0, glow * 1.3 + ray * 0.8)
            if alpha > 0.04:
                put(img, x, y, (255, 255, 255), int(alpha * 255))
    return img


def firework_textures():
    out = {("item", info["item"]): rocket(shape) for shape, info in FIREWORKS["shapes"].items()}
    out.update({("block", "show_launcher_crate"): crate(), ("block", "show_launcher_crate_top"): crate_top(),
                ("block", "show_launcher_tube"): tube(), ("block", "show_launcher_tube_top"): tube_top(),
                ("entity", "show_launcher_rocket"): paper(), ("particle", FIREWORKS["particle"]): spark()})
    for mode in ("sequence", "volley", "finale"):
        out[("block", f"show_launcher_dial_{mode}")] = dial(mode)
    return out
