"""High-detail art for the batch 27 high-tech gear (docs/features/gear-and-plastic.md): the power katana, the power
bow, scuba gear and free runners.

- Icons are 32x32 instead of 16x16.
- Worn layers are 128x64 instead of 64x32. Minecraft scales armor UVs to the texture's size, so the doubled layer
  maps onto the same model with twice the detail.
- The katana's blade is animated: a pulse of light runs along the energy edge.

All original. The art is drawn procedurally in sword space (along and across the blade's axis), so the shapes stay
crisp at any angle without being traced from anything. The katana's general shape follows an image the owner
shared: a wrapped grip, a guard, a collar and a two-tone blade. Nothing is copied from it; the colours, energy edge
and details are our own.
"""
import math

from PIL import Image

# Palette (our own picks): the mod's electric cyan, gunmetal, and a charcoal grip wrap.
WHITE = (240, 252, 255)
CYAN_CORE = (150, 244, 255)
CYAN = (60, 210, 240)
CYAN_DEEP = (24, 128, 196)
VIOLET = (110, 70, 200)
INDIGO = (30, 34, 58)
INDIGO_HI = (58, 66, 104)
STEEL_HI = (214, 222, 232)
STEEL = (150, 160, 174)
STEEL_MID = (104, 112, 126)
STEEL_DARK = (58, 62, 74)
GUN = (36, 38, 46)
WRAP = (28, 28, 34)
WRAP_HI = (64, 66, 78)
GOLD = (226, 184, 72)
GOLD_DARK = (150, 108, 36)
BLACK = (14, 14, 18)
# The glow fringe along the edge: opaque since 5 October 2026 (it was a translucent cyan, see-through on the blade in
# the hand), in the blade's bright glow tone, so the white-hot edge still reads as glowing rather than dark-rimmed.
GLOW = CYAN


def _rgba(c, a=255):
    return tuple(c) + (a,)


def _mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


# ---------------------------------------------------------------- the power katana
# Drawn on the 45-degree diagonal from the bottom-left (tip) to the top-right (pommel). Pixel art at 45 degrees stays
# crisp only in whole diagonal rows, so the sword is laid out in pixel coordinates:
# - t, along the sword, from (x - y + 31) / 2: one per diagonal step, tip at 0 and pommel at 31;
# - k, across it, from x + y - 31: each k is one diagonal row, and positive k is the cutting edge (lower right).
KATANA_FRAMES = 8
BLADE_END = 20.5   # two thirds of the sword is blade
COLLAR_END = 21.5
GUARD_END = 22.5
GRIP_END = 29.5
POMMEL_END = 31.0


# The blade's energy colours: the power katana's cyan, and the Ronin katana's crimson (batch 28).
KATANA_COLOURS = {
    "cyan": {"WHITE": WHITE, "CYAN_CORE": CYAN_CORE, "CYAN": CYAN, "CYAN_DEEP": CYAN_DEEP, "INDIGO": INDIGO,
             "INDIGO_HI": INDIGO_HI, "VIOLET": VIOLET, "GLOW": GLOW},
    "crimson": {"WHITE": (255, 242, 238), "CYAN_CORE": (255, 176, 164), "CYAN": (244, 56, 52), "CYAN_DEEP": (160, 18, 26),
                "INDIGO": (34, 14, 18), "INDIGO_HI": (74, 24, 30), "VIOLET": (226, 230, 236),
                "GLOW": (244, 56, 52)},
}


def _katana_pixel(t, k, pulse, c):
    if t < 0.5 or t > POMMEL_END:
        return None
    if t <= BLADE_END:
        # Kissaki: the tip narrows to the edge row.
        lowest = -1 if t > 3.5 else (0 if t > 2.5 else (1 if t > 1.5 else 2))
        if k == 3 and t > 1.5:
            return c["GLOW"]
        if not lowest <= k <= 2:
            return None
        glow = max(0.0, 1.0 - abs(t - pulse) / 2.5)
        # Across the blade, dark spine to white-hot edge, so the rows blend instead of striping.
        if k == -1:
            return c["INDIGO"]  # the dark spine
        if k == 0:
            if t > BLADE_END - 8 and int(t) % 3 == 1:
                return _mix(c["CYAN"], c["WHITE"], glow)  # circuit nodes running out from the collar
            return _mix(c["INDIGO_HI"], c["CYAN_DEEP"], glow)
        if k == 1:
            return _mix(c["CYAN"], c["CYAN_CORE"], glow)
        return _mix(c["CYAN_CORE"], c["WHITE"], 0.4 + 0.6 * glow)  # the cutting edge
    if t <= COLLAR_END:
        return (GOLD if k <= 0 else GOLD_DARK) if -1 <= k <= 2 else None  # habaki
    if t <= GUARD_END:
        if not -3 <= k <= 4:
            return None
        if k in (-3, 4):
            return STEEL_DARK
        return c["CYAN"] if k in (-2, 3) else STEEL_MID  # tsuba with a cyan power ring
    if t <= GRIP_END:
        if not -1 <= k <= 2:
            return None
        i = int(t - GUARD_END)
        if (i + k) % 3 == 0:
            return c["CYAN_CORE"] if k in (0, 1) else c["CYAN"]  # cell windows between the wraps
        return WRAP_HI if (i - k) % 3 == 0 else WRAP
    if not -1 <= k <= 2:
        return None
    if k in (0, 1) and t < POMMEL_END - 0.5:
        return c["VIOLET"]  # status light in the pommel cap
    return STEEL_HI if k <= 0 else STEEL


def katana_frame(pulse, colours=None):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            colour = _katana_pixel((x - y + 31) / 2, x + y - 31, pulse, colours or KATANA_COLOURS["cyan"])
            if colour is not None:
                img.putpixel((x, y), colour if len(colour) == 4 else _rgba(colour))
    # An opaque rim (5 October 2026: a see-through rim was the transparency the owner saw on held weapons).
    return _outline(img, BLACK)


def katana(colours=None):
    """The animated icon: KATANA_FRAMES frames of 32x32 in a vertical strip, a light running down to the tip."""
    strip = Image.new("RGBA", (32, 32 * KATANA_FRAMES), (0, 0, 0, 0))
    for i in range(KATANA_FRAMES):
        pulse = BLADE_END + 2 - (BLADE_END + 5) * i / KATANA_FRAMES
        strip.paste(katana_frame(pulse, colours), (0, 32 * i))
    return strip


def _outline(img, colour, alpha=255):
    """A one-pixel dark rim around the shape, so it reads against any background."""
    out = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if img.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and img.getpixel((nx, ny))[3] == 255:
                    out.putpixel((x, y), _rgba(colour, alpha))
                    break
    return out


# ---------------------------------------------------------------- worn layers at double resolution (128x64)
BOXES = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}
RUBBER = (34, 36, 42)
RUBBER_HI = (62, 66, 76)
GLASS = (96, 196, 220)
GLASS_HI = (200, 244, 252)
GLASS_DARK = (44, 120, 150)
YELLOW = (236, 188, 40)
YELLOW_HI = (252, 226, 120)
YELLOW_DARK = (176, 126, 22)
WEB = (44, 48, 60)
WEB_HI = (78, 84, 100)
SHELL = (226, 230, 236)
SHELL_MID = (178, 186, 198)
SHELL_DARK = (120, 128, 142)


def _faces2x(box):
    """The six faces of a box on the doubled sheet, as name -> (left, top, width, height)."""
    u, v, w, h, d = BOXES[box]
    rects = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
             "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
    return {name: tuple(2 * n for n in rect) for name, rect in rects.items()}


class Face:
    """Paints one face in its own coordinates (0, 0 at its top left)."""

    def __init__(self, img, rect):
        self.img, (self.left, self.top, self.w, self.h) = img, rect

    def px(self, x, y, colour):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((self.left + x, self.top + y), colour if len(colour) == 4 else _rgba(colour))

    def rect(self, x0, y0, x1, y1, colour):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, colour)

    def hline(self, x0, x1, y, colour):
        self.rect(x0, y, x1, y, colour)


def _cylinder(face, x0, x1, y0, y1):
    """A yellow tank, lit from the left, with a rounded shoulder, steel bands and a valve."""
    for y in range(y0, y1 + 1):
        inset = 1 if y == y0 else 0
        for x in range(x0 + inset, x1 + 1 - inset):
            f = (x - x0) / max(1, x1 - x0)
            c = YELLOW_HI if f < 0.25 else (YELLOW if f < 0.7 else YELLOW_DARK)
            face.px(x, y, c)
    for band in (y0 + 5, y1 - 4):
        face.hline(x0, x1, band, STEEL_MID)
        face.px(x0, band, STEEL_HI)
    face.rect(x0 + 1, y0 - 2, x1 - 1, y0 - 1, STEEL)  # neck and valve
    face.px(x0 + 1, y0 - 2, STEEL_HI)


def scuba_layer():
    """Dive mask with a strap and a light module on the head; twin tanks on the back on a webbing harness with a
    chest buckle and a pressure gauge; a dive computer on the left forearm."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    head = _faces2x("head")
    # The mask: a silicone skirt and steel frame around two tempered panes, with a nose pocket.
    f = Face(img, head["front"])
    f.rect(1, 5, 14, 12, RUBBER)
    f.rect(2, 6, 13, 11, STEEL_MID)
    f.rect(3, 7, 6, 10, GLASS)
    f.rect(9, 7, 12, 10, GLASS)
    for pane in (3, 9):
        f.px(pane, 7, GLASS_HI)
        f.px(pane + 1, 7, GLASS_HI)
        f.px(pane, 8, GLASS_HI)
        f.hline(pane, pane + 3, 10, GLASS_DARK)
    f.rect(7, 7, 8, 9, STEEL_HI)       # bridge
    f.rect(6, 11, 9, 13, RUBBER_HI)     # nose pocket
    f.hline(7, 8, 13, RUBBER)
    f.hline(2, 13, 5, RUBBER_HI)
    f.px(7, 4, CYAN)                    # head-up display light
    f.px(8, 4, CYAN_CORE)
    for side in ("right", "left", "back"):
        s = Face(img, head[side])
        s.rect(0, 7, 15, 9, RUBBER)
        s.hline(0, 15, 7, RUBBER_HI)
    for side in ("right", "left"):
        s = Face(img, head[side])
        buckle = 13 if side == "right" else 1  # next to the front face
        s.rect(buckle, 6, buckle + 1, 10, STEEL_HI)
    back = Face(img, head["back"])
    back.rect(5, 6, 10, 10, GUN)        # the light module on the strap
    back.rect(6, 7, 9, 9, STEEL_DARK)
    back.hline(7, 8, 8, CYAN)

    body = _faces2x("body")
    b = Face(img, body["back"])
    _cylinder(b, 1, 6, 3, 21)
    _cylinder(b, 9, 14, 3, 21)
    b.rect(2, 0, 13, 1, STEEL_DARK)     # the manifold joining the valves
    b.hline(3, 12, 0, STEEL)
    b.rect(7, 2, 8, 4, STEEL_DARK)
    b.rect(0, 9, 15, 10, WEB)           # harness band across the tanks
    b.hline(0, 15, 9, WEB_HI)
    front = Face(img, body["front"])
    for x in (3, 11):                   # shoulder straps
        front.rect(x, 0, x + 1, 23, WEB)
        front.rect(x, 0, x, 23, WEB_HI)
    front.rect(3, 9, 12, 10, WEB)       # chest strap and buckle
    front.rect(6, 8, 9, 11, STEEL)
    front.rect(7, 9, 8, 10, CYAN)
    front.rect(1, 19, 14, 20, WEB)      # waist belt and buckle
    front.hline(1, 14, 19, WEB_HI)
    front.rect(7, 18, 8, 21, STEEL_HI)
    front.rect(11, 12, 14, 15, GUN)     # pressure gauge clipped to the strap
    front.rect(12, 13, 13, 14, SHELL)
    front.px(13, 13, CYAN_DEEP)
    for side in ("right", "left"):
        s = Face(img, body[side])
        s.rect(0, 9, 7, 10, WEB)
        s.rect(0, 19, 7, 20, WEB)
        s.hline(0, 7, 19, WEB_HI)
    top = Face(img, body["top"])
    for x in (3, 11):
        top.rect(x, 0, x + 1, 7, WEB)
    arm = _faces2x("arm")
    for side in ("front", "right", "left", "back"):
        a = Face(img, arm[side])
        a.rect(0, 15, 7, 18, RUBBER)    # the dive computer's strap
        a.hline(0, 7, 15, RUBBER_HI)
    a = Face(img, arm["front"])
    a.rect(1, 14, 6, 19, GUN)           # its screen
    a.rect(2, 15, 5, 18, CYAN_DEEP)
    a.hline(2, 4, 16, CYAN_CORE)
    a.px(2, 17, CYAN)
    return img


def runners_layer():
    """Free runners: white shell boots with a cyan light strip, black sprung soles and a coil at the heel."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    leg = _faces2x("leg")
    for side in ("front", "right", "left", "back"):
        f = Face(img, leg[side])
        f.rect(0, 13, 7, 19, SHELL)
        f.hline(0, 7, 13, SHELL_DARK)
        f.hline(0, 7, 14, SHELL_MID)
        f.hline(0, 7, 17, CYAN)          # light strip
        f.hline(0, 7, 18, CYAN_DEEP)
        f.rect(0, 20, 7, 23, RUBBER)     # sprung sole
        f.hline(0, 7, 20, RUBBER_HI)
        f.hline(0, 7, 23, BLACK)
    front = Face(img, leg["front"])
    front.rect(1, 19, 6, 21, SHELL_MID)  # toe cap
    front.hline(2, 5, 19, SHELL)
    front.rect(3, 15, 4, 16, STEEL_DARK)  # lace hatch
    back = Face(img, leg["back"])
    for y in (20, 22):                    # the heel coil
        back.hline(2, 5, y, STEEL_HI)
        back.hline(2, 5, y + 1, STEEL_MID)
    back.px(3, 15, CYAN_CORE)
    bottom = Face(img, leg["bottom"])
    bottom.rect(0, 0, 7, 7, RUBBER)
    for y in range(1, 7, 2):              # tread
        bottom.hline(1, 6, y, BLACK)
    return img


# ---------------------------------------------------------------- 32x32 icons for the gear
def _icon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    return img, Face(img, (0, 0, 32, 32))


def scuba_mask_icon():
    img, f = _icon()
    f.rect(1, 13, 4, 16, RUBBER)            # strap stubs and buckles
    f.rect(27, 13, 30, 16, RUBBER)
    f.rect(4, 12, 5, 17, STEEL_HI)
    f.rect(26, 12, 27, 17, STEEL_HI)
    f.rect(5, 8, 26, 22, RUBBER)            # silicone skirt
    for x, y in ((5, 8), (26, 8), (5, 22), (26, 22)):
        f.px(x, y, (0, 0, 0, 0))
    f.hline(6, 25, 8, RUBBER_HI)
    f.rect(6, 9, 25, 20, STEEL_MID)         # steel frame
    f.hline(6, 25, 9, STEEL_HI)
    for x0 in (7, 17):                      # the two panes, glinting
        f.rect(x0, 10, x0 + 7, 18, GLASS)
        f.hline(x0, x0 + 7, 18, GLASS_DARK)
        f.rect(x0 + 6, 10, x0 + 7, 17, GLASS_DARK)
        for i in range(4):
            f.px(x0 + 1 + i, 14 - i, GLASS_HI)
        f.px(x0 + 1, 11, GLASS_HI)
        f.px(x0 + 2, 11, GLASS_HI)
    f.rect(15, 10, 16, 16, STEEL_HI)        # bridge
    f.rect(12, 19, 19, 24, RUBBER)          # nose pocket
    f.rect(13, 19, 18, 23, RUBBER_HI)
    f.hline(14, 17, 23, RUBBER)
    f.rect(14, 6, 17, 7, GUN)               # head-up display light
    f.hline(15, 16, 6, CYAN_CORE)
    return _outline(img, BLACK)   # opaque: no see-through rim (5 October 2026)


def scuba_tank_icon():
    img, f = _icon()
    for x0 in (6, 17):
        x1 = x0 + 8
        for y in range(7, 29):
            inset = 1 if y in (7, 28) else 0
            for x in range(x0 + inset, x1 + 1 - inset):
                r = (x - x0) / 8
                f.px(x, y, YELLOW_HI if r < 0.25 else (YELLOW if r < 0.7 else YELLOW_DARK))
        for band in (11, 24):
            f.hline(x0, x1, band, STEEL_MID)
            f.px(x0, band, STEEL_HI)
        f.rect(x0 + 3, 4, x0 + 5, 6, STEEL)  # valve necks
        f.px(x0 + 3, 4, STEEL_HI)
    f.rect(8, 2, 23, 3, STEEL_DARK)          # manifold
    f.hline(9, 22, 2, STEEL)
    f.rect(14, 1, 17, 4, GUN)                # its knob
    f.hline(15, 16, 1, STEEL_HI)
    f.rect(4, 16, 27, 18, WEB)               # harness band
    f.hline(4, 27, 16, WEB_HI)
    f.rect(14, 15, 17, 19, STEEL)            # band buckle, lit
    f.hline(15, 16, 17, CYAN)
    for y in range(4, 22):                   # the hose to the gauge
        f.px(28 if y < 12 else 29, y, RUBBER)
    f.rect(26, 22, 31, 27, GUN)              # pressure gauge
    f.rect(27, 23, 30, 26, SHELL)
    f.px(29, 24, CYAN_DEEP)
    f.px(28, 25, CYAN_DEEP)
    return _outline(img, BLACK)   # opaque: no see-through rim (5 October 2026)


def _runner(f, dx, dy, shade):
    """One boot in profile, toe to the right, at (dx, dy)."""
    def c(col):
        return _mix(col, (40, 44, 56), shade)
    f.rect(dx + 2, dy + 0, dx + 8, dy + 9, c(SHELL))          # shaft
    f.rect(dx + 2, dy + 0, dx + 8, dy + 1, c(SHELL_DARK))     # collar
    f.rect(dx + 2, dy + 6, dx + 15, dy + 11, c(SHELL))        # foot
    for x in range(9, 16):                                     # toe slope
        for y in range(6, 6 + max(0, x - 11)):
            f.px(dx + x, dy + y, (0, 0, 0, 0))
    f.rect(dx + 2, dy + 2, dx + 2, dy + 11, c(SHELL_MID))
    f.hline(dx + 2, dx + 15, dy + 9, c(CYAN))                  # light strip
    f.hline(dx + 2, dx + 15, dy + 10, c(CYAN_DEEP))
    f.rect(dx + 5, dy + 3, dx + 7, dy + 6, c(STEEL_DARK))      # lace hatch
    f.px(dx + 6, dy + 4, c(CYAN_CORE))
    f.rect(dx + 1, dy + 12, dx + 16, dy + 14, RUBBER)          # sprung sole
    f.hline(dx + 1, dx + 16, dy + 12, RUBBER_HI)
    f.hline(dx + 2, dx + 15, dy + 14, BLACK)
    for x in (dx + 2, dx + 4):                                 # heel coil
        f.rect(x, dy + 13, x, dy + 15, STEEL_HI)


def free_runners_icon():
    img, f = _icon()
    _runner(f, 1, 4, 0.35)    # the far boot, in shadow
    _runner(f, 12, 14, 0.0)
    return _outline(img, BLACK)   # opaque: no see-through rim (5 October 2026)
