"""The plant overview screen's hardware (round 34; Justin, 15 October 2026: "Overview screens live", mockup approved):
a slim large-format LCD three blocks across and two high on a wall mount, and the surface raceway that brings its
power down the wall to a duplex receptacle over the floor. Its picture is drawn live over the black glass
(tools/overview_screen.py, client/OverviewScreenRenderer.java).

Whole-screen model pixels: x across from its left as you face it (0..48), y up (0..32), z out from the wall."""
import math

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M

S = M.CELL
PANEL_EDGE, BEZEL, BEZEL_BOTTOM = 0.25, 0.32, 0.5
GLASS_PX = 1.9                  # the glass's face, off the wall
HIT_PX = 1.95                   # the hitbox's depth (to the bezel's face: a click lands on the picture)

# The wall mount behind it, the back shell, the body, the bezel standing a little proud of the glass, the glass
# (black: the picture is drawn over it), and on the bottom bezel the maker's badge, the remote's window and the
# power light (lit).
HARDWARE = [
    (15, 9, 0, 33, 23, 0.5, "pi_ovs_mount"),
    (0.7, 0.7, 0.5, 47.3, 31.3, 1.15, "pi_ovs_back"),
    (PANEL_EDGE, PANEL_EDGE, 1.15, 48 - PANEL_EDGE, 32 - PANEL_EDGE, 1.85, "pi_ovs_bezel"),
    (PANEL_EDGE, 32 - PANEL_EDGE - BEZEL, 1.85, 48 - PANEL_EDGE, 32 - PANEL_EDGE, 1.95, "pi_ovs_bezel"),
    (PANEL_EDGE, PANEL_EDGE, 1.85, 48 - PANEL_EDGE, PANEL_EDGE + BEZEL_BOTTOM, 1.95, "pi_ovs_bezel"),
    (PANEL_EDGE, PANEL_EDGE + BEZEL_BOTTOM, 1.85, PANEL_EDGE + BEZEL, 32 - PANEL_EDGE - BEZEL, 1.95, "pi_ovs_bezel"),
    (48 - PANEL_EDGE - BEZEL, PANEL_EDGE + BEZEL_BOTTOM, 1.85, 48 - PANEL_EDGE, 32 - PANEL_EDGE - BEZEL, 1.95, "pi_ovs_bezel"),
    (PANEL_EDGE + BEZEL, PANEL_EDGE + BEZEL_BOTTOM, 1.85, 48 - PANEL_EDGE - BEZEL, 32 - PANEL_EDGE - BEZEL, GLASS_PX, "glass"),
    (22.6, 0.36, 1.95, 25.4, 0.64, 1.97, "pi_ovs_badge"),
    (44.2, 0.38, 1.95, 45.2, 0.62, 1.97, "pi_ovs_ir"),
    (46.6, 0.42, 1.95, 46.9, 0.58, 1.98, "led"),
]


def part_boxes(c, r, glass=None):
    """Block (c, r)'s share of the display, in its own coordinates (its front, the picture's side, south); glass: a
    texture for its share of the glass instead of the black glass (a mockup's picture)."""
    out = []
    for x0, y0, z0, x1, y1, z1, tex in HARDWARE:
        a0, a1 = max(x0, 16 * c), min(x1, 16 * c + 16)
        b0, b1 = max(y0, 16 * r), min(y1, 16 * r + 16)
        if a1 - a0 < 0.01 or b1 - b0 < 0.01:
            continue
        if tex == "glass":
            # The glass's front (its UV by position: the blocks' shares of a picture meet), its edges the bezel's.
            tex = {"*": "pi_ovs_bezel", "south": glass or "pi_ovs_glass"}
        elif tex == "led":
            tex = {"*": "pi_ovs_led", "south": "pi_ovs_led"}
        out.append((round(a0 - 16 * c, 4), round(b0 - 16 * r, 4), z0, round(a1 - 16 * c, 4), round(b1 - 16 * r, 4), z1, tex))
    return out


# The raceway: a painted steel channel down the wall from behind the display's middle to a duplex receptacle in its
# box over the floor (its cover's screw, the outlets' slots and ground holes on its texture).
RACEWAY = [
    (7.0, 4.6, 0, 9.0, 16, 0.8, "pi_raceway"),
    (7.3, 4.6, 0.8, 8.7, 16, 0.84, "pi_raceway_cover"),
    (5.6, 0.8, 0, 10.4, 4.6, 1.3, {"*": "pi_raceway", "south": "pi_raceway_outlet!"}),
]


def _streaky(rgb, seed, amount, vertical=False):
    a = M.base(rgb, S)
    M.shade(a, M.streaks(S, seed, "v" if vertical else "h", count=160, length=(30, 110), amount=amount))
    M.shade(a, M.fbm(S, seed + 1, 3, 32) * 2)
    return M.to_img(a)


def _back():
    img = _streaky((36, 38, 42), 1411, 4)
    d = ImageDraw.Draw(img)
    for y in range(16, S - 8, 12):
        for x in range(10, S - 10, 14):
            d.rounded_rectangle([x, y, x + 9, y + 3], 1, fill=(18, 19, 22, 255))
    return img


def _glass():
    """Black glass, a faint sheen across it."""
    a = np.zeros((S, S, 4), np.uint8)
    yy, xx = np.mgrid[0:S, 0:S]
    sheen = np.clip(1 - np.abs((xx + yy) - S * 0.9) / (S * 0.35), 0, 1) * 7
    a[..., 0] = 8 + sheen
    a[..., 1] = 9 + sheen
    a[..., 2] = 11 + sheen
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _outlet():
    img = _streaky((228, 226, 220), 1417, 3)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(176, 174, 168, 255), width=2)
    for y in (18, 72):
        d.rounded_rectangle([34, y, 94, y + 38], 12, fill=(238, 236, 230, 255), outline=(168, 166, 160, 255), width=2)
        d.rectangle([49, y + 9, 54, y + 24], fill=(36, 36, 38, 255))
        d.rectangle([74, y + 9, 79, y + 26], fill=(36, 36, 38, 255))
        d.ellipse([59, y + 26, 69, y + 35], fill=(36, 36, 38, 255))
    d.ellipse([59, 59, 69, 69], fill=(150, 150, 146, 255))
    d.line([61, 64, 67, 64], fill=(90, 90, 88, 255), width=2)
    return img


def _raceway(cover=False):
    img = _streaky((216, 216, 210), 1419 + cover, 3, vertical=True)
    if not cover:
        d = ImageDraw.Draw(img)
        for x in (2, S - 3):
            d.line([x, 0, x, S], fill=(170, 170, 164, 255), width=2)
    return img


def makers():
    return {
        "pi_ovs_mount": lambda: M.brushed((58, 60, 64), 1410, "h", dirt=0.05),
        "pi_ovs_back": _back,
        "pi_ovs_bezel": lambda: _streaky((29, 30, 33), 1412, 3),
        "pi_ovs_glass": _glass,
        "pi_ovs_badge": lambda: _streaky((164, 168, 174), 1413, 8),
        "pi_ovs_ir": lambda: M.to_img(M.base((38, 12, 14), S)),
        "pi_ovs_led": lambda: M.to_img(M.base((96, 244, 136), S)),
        "pi_raceway": _raceway,
        "pi_raceway_cover": lambda: _raceway(True),
        "pi_raceway_outlet": _outlet,
    }


def icon():
    """The display's item icon (as the other screens': the picture's small version on a dark panel)."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 24, 124, 104], fill=(28, 30, 34, 255))
    d.rectangle([7, 27, 121, 100], fill=(14, 17, 22, 255))
    d.rectangle([7, 27, 121, 33], fill=(26, 33, 44, 255))
    for x0, y0, x1, y1, c in ((14, 44, 28, 58, (22, 60, 40)), (32, 42, 58, 54, (22, 60, 40)), (62, 42, 76, 56, (22, 60, 40)),
                              (16, 64, 30, 74, (46, 52, 64)), (36, 62, 48, 74, (46, 52, 64)), (52, 62, 76, 70, (46, 52, 64))):
        d.rectangle([x0, y0, x1, y1], fill=c + (255,), outline=(150, 158, 170, 255))
    for cx in (28, 56):
        d.ellipse([cx - 10, 78, cx + 10, 96], fill=(22, 60, 40, 255), outline=(150, 158, 170, 255))
    d.rectangle([84, 38, 117, 70], fill=(23, 28, 36, 255), outline=(56, 64, 78, 255))
    for k in range(5):
        d.rectangle([88, 42 + k * 5, 112, 44 + k * 5], fill=(110, 230, 140, 255) if k < 4 else (240, 176, 40, 255))
    d.line([(86, 92), (98, 92), (100, 82), (116, 82)], fill=(240, 150, 60, 255), width=2)
    d.rectangle([60, 104, 68, 120], fill=(216, 216, 210, 255))
    return M.stylize(img, 16)
