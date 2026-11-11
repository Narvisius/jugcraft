"""The low filing cabinet as real 3D (Justin's walk through, 25 October 2026: "the small drawers to the left of the gun
locker. Replace that with a more realistic version so that it overwrites any other build with that particular drawer").

A three-drawer lateral filing cabinet, 75 cm wide, 53 cm deep and 94 cm high, its back to the wall, for a block facing
north (its drawers on the south side): the light grey powder-coated carcass (sides, top, back) on a recessed black
plinth; each drawer front its own panel standing proud with the dark gaps between them; on each a bar pull on its two
posts and a chrome card holder with the drawer's label; the lock in the top drawer. Open, the top drawer pulled out a
quarter of a metre: its sides and floor run back into the cabinet, the suspension files hang in it, the dark inside
behind. Every face at least 0.1 px from any other in its plane (no z-fighting); 1 px is 6.25 cm.
"""
from __future__ import annotations

from PIL import Image, ImageDraw, ImageFont

import nuclear_materials as M
import plant_office3 as O3

PAINT, DARK, CHROME, PAPER = "pi_filing_paint", "pi_pnl_black", "pi_chrome", "pi_filing_labels"
X0, X1 = 2.0, 14.0            # the carcass across
ZB, ZF = 2.0, 10.5            # its back (against the wall) and the front of its sides
T = 0.3                       # sheet thickness
TOP = 15.0                    # its top
FRONT = (ZF, ZF + 0.45)       # a drawer front's back and front
DRAWERS = ((10.2, 14.7), (5.5, 10.1), (0.8, 5.4))   # the drawers' fronts, top first
LABELS = ("A - H", "I - Q", "R - Z", "PERMITS", "SHIFT LOGS", "SPARES")   # the label sheet (pi_filing_labels), row by row
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def _label(i):
    """Label i of the sheet: two across, four down, each card 3 to 1 (8 by 8 / 3 in uv) at the top of its cell."""
    c, r = i % 2, i // 2
    return (PAPER, [c * 8, r * 4, c * 8 + 8, r * 4 + 8 / 3])


def _carcass():
    return [(X0, 0.6, ZB, X0 + T, TOP - 0.3, ZF, PAINT), (X1 - T, 0.6, ZB, X1, TOP - 0.3, ZF, PAINT),
            (X0 - 0.1, TOP - 0.3, ZB - 0.1, X1 + 0.1, TOP, ZF + 0.55, PAINT),            # the top, its lip over the fronts
            (X0 + T, 0.6, ZB, X1 - T, TOP - 0.3, ZB + T, PAINT),                         # the back
            (X0 + T, 0.6, ZB + T, X1 - T, 0.7, ZF - 0.05, PAINT),                        # the floor
            (X0 + 0.4, 0, ZB + 0.4, X1 - 0.4, 0.6, ZF - 0.6, DARK),                      # the plinth, set back
            (X0 + T, 0.7, ZF - 0.2, X1 - T, TOP - 0.3, ZF - 0.1, DARK)]                  # the dark behind the gaps


def _front(y0, y1, z0, z1, label, lock):
    """One drawer front and its fittings, its front at z1."""
    xc = (X0 + X1) / 2
    out = [(X0 + T + 0.1, y0, z0, X1 - T - 0.1, y1, z1, PAINT)]
    hy = y1 - 1.6
    out += [(xc - 2.6, hy, z1 + 0.3, xc + 2.6, hy + 0.35, z1 + 0.5, CHROME),             # the bar
            (xc - 2.4, hy, z1, xc - 2.1, hy + 0.35, z1 + 0.3, CHROME), (xc + 2.1, hy, z1, xc + 2.4, hy + 0.35, z1 + 0.3, CHROME),
            (xc - 1.3, hy - 1.4, z1, xc + 1.3, hy - 0.4, z1 + 0.12, CHROME),             # the card holder
            (xc - 1.15, hy - 1.28, z1 + 0.12, xc + 1.15, hy - 0.52, z1 + 0.18, {"*": "pi_paper", "south": _label(label)})]
    if lock:
        out += [(X1 - 1.6, y1 - 1.0, z1, X1 - 1.0, y1 - 0.4, z1 + 0.25, CHROME),
                (X1 - 1.45, y1 - 0.85, z1 + 0.25, X1 - 1.15, y1 - 0.55, z1 + 0.32, DARK)]
    return out


def closed(labels=(0, 1, 2)):
    out = _carcass()
    for k, (y0, y1) in enumerate(DRAWERS):
        out += _front(y0, y1, *FRONT, labels[k], k == 0)
    return out


def opened(labels=(3, 4, 5), out_by=4.0):
    """The same with its top drawer pulled out out_by px, its files hanging in it."""
    out = _carcass()
    for k, (y0, y1) in enumerate(DRAWERS):
        dz = out_by if k == 0 else 0.0
        out += _front(y0, y1, FRONT[0] + dz, FRONT[1] + dz, labels[k], k == 0)
        if k == 0:
            # the drawer's sides and floor back into the cabinet; the suspension files on their rails
            fz0, fz1 = ZB + 1.5, FRONT[0] + dz
            out += [(X0 + T + 0.3, y0 + 0.2, fz0, X0 + T + 0.45, y1 - 1.2, fz1, PAINT),
                    (X1 - T - 0.45, y0 + 0.2, fz0, X1 - T - 0.3, y1 - 1.2, fz1, PAINT),
                    (X0 + T + 0.45, y0 + 0.2, fz0, X1 - T - 0.45, y0 + 0.35, fz1, PAINT)]
            cols = ("pi_folder_green", "pi_folder_manila", "pi_folder_green", "pi_folder_blue", "pi_folder_green",
                    "pi_folder_manila", "pi_folder_green", "pi_folder_green")
            for j, c in enumerate(cols):
                z = ZF - 0.6 + j * 0.5 + dz - 4.0
                h = 3.2 - (j % 3) * 0.15
                out.append((X0 + T + 0.6, y0 + 0.35, z, X1 - T - 0.6, y0 + 0.35 + h, z + 0.12, {"*": c, "up": "pi_folder_tab"}))
                tx = X0 + 1.2 + (j * 2.3) % 8.0
                out.append((tx, y0 + 0.35 + h, z, tx + 1.4, y0 + 0.75 + h, z + 0.08, "pi_paper"))
    return out


# ---------------------------------------------------------------------------------------------- textures

def _labels():
    """The drawer labels (pi_filing_labels, 128 px, the most a texture may be; kept sharp so they read): printed cards,
    64 by 21 px each, two to a row."""
    img = Image.new("RGBA", (128, 128), (244, 242, 234, 255))
    d = ImageDraw.Draw(img)
    for i, text in enumerate(LABELS):
        c, r = i % 2, i // 2
        x0, y0 = c * 64, r * 32
        d.rectangle((x0, y0, x0 + 63, y0 + 20), fill=(244, 242, 234, 255), outline=(200, 196, 186, 255))
        size = 15
        while d.textlength(text, font=ImageFont.truetype(FONT, size)) > 56:
            size -= 1
        d.text((x0 + 32, y0 + 11), text, font=ImageFont.truetype(FONT, size), fill=(30, 34, 44, 255), anchor="mm")
    return img


def makers():
    """The filing cabinet's textures: its powder-coated paint, the labels, the green suspension files."""
    return {PAINT: lambda: M.painted((184, 186, 182), 6050, chip_count=0, drip_count=0, dirt=0.05),
            PAPER: _labels, "pi_folder_green": lambda: O3._folder((74, 118, 78))}


SHARP = {PAPER}
