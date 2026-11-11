"""The automated warehouse, part W4 (liquids; claude/jugcraft-warehouse-plan.md): liquids stored in drums on pallets.

How it works: liquids piped into the Liquids In wall panel (four flanged inlets, a tanker coupling, a funnel to tip
buckets into) go into the Drum Filling Station behind it. The station takes an empty pallet from its magazine, its
gantry sets four empty drums on it from the drum magazine, fills each through its lance on the weigh scale, caps it
and ring-straps the four; a pallet drone takes the pallet to the racks. The drum is the one the liquid needs
(nuclear/TankProducts kinds): a blue steel drum for a general liquid, a red one (earthed while filling) for a
flammable one, a natural (white) HDPE drum for a corrosive one, a stainless liquid dewar for a cryogenic one. Asked
for a liquid, a pallet drone brings drums to the Drum Emptying Station by Ready Storage: on its bunded platform the
drum pump on its jib empties what the order needs into the holding tank, and the Liquids Out wall panel gives it out:
by pipe from its OUT flange, or at its filling point into a bucket. Emptied drums go back to the filling station's
magazine.

Every piece is drawn as tools/warehouse.py draws them: front to +z (toward its user in the aisle), x across."""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
import warehouse as W
from warehouse import box, round_x, round_y, round_z, rot_x, rot_y, rot_z, shifted

YELLOW, GRAPHITE, STEEL, CHROME, BLACK, HAZARD = W.YELLOW, W.GRAPHITE, W.STEEL, W.CHROME, W.BLACK, W.HAZARD
MACHINE, MACHINE_DARK, STAINLESS, RUBBER, GUARD = W.MACHINE, W.MACHINE_DARK, W.STAINLESS, W.RUBBER, W.GUARD

# The drums: kind -> (side, top, label). A drum is 3.3 px round (its middle in a quarter of the pallet) and 9.6 high.
DRUMS = {"steel": ("pi_wh_drum", "pi_wh_drum_top", "pi_wh_drum_label"),
         "red": ("pi_wh4_drum_red", "pi_wh4_drum_red_top", "pi_wh4_drum_red_label"),
         "poly": ("pi_wh4_poly", "pi_wh4_poly_top", "pi_wh4_poly_label"),
         "dewar": ("pi_wh4_dewar", "pi_wh4_dewar_top", "pi_wh4_dewar_label")}
# Which drum a liquid goes in (TankProducts.Kind).
DRUM_OF = {"general": "steel", "flammable": "red", "corrosive": "poly", "cryogenic": "dewar"}
DRUM_R, DRUM_H, DEWAR_H = 3.3, 9.6, 9.0
SPOTS = ((4.6, 4.6), (11.4, 4.6), (4.6, 11.4), (11.4, 11.4))
HOSE, GRATING, BUND = "pi_wh4_hose", "pi_wh4_grating", "pi_wh4_bund"
TANK_R = 6.4


# ================================================================================================ textures
def _diamond(d, cx, cy, s, fill, edge=(20, 20, 22), text="", sub="", tcol=(20, 20, 22), symbol=None):
    """A transport hazard label (a square set on its point): its colours, the symbol, the class number at the bottom."""
    pts = [(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)]
    if isinstance(fill, tuple) and len(fill) == 2 and isinstance(fill[0], tuple):
        top, bot = fill
        d.polygon(pts, fill=top)
        d.polygon([(cx - s, cy), (cx + s, cy), (cx, cy + s)], fill=bot)
    else:
        d.polygon(pts, fill=fill)
    d.polygon([(cx, cy - s + 3), (cx + s - 3, cy), (cx, cy + s - 3), (cx - s + 3, cy)], outline=edge, width=2)
    if symbol == "flame":
        d.polygon([(cx, cy - s * 0.62), (cx + s * 0.22, cy - s * 0.18), (cx + s * 0.12, cy + s * 0.08), (cx - s * 0.12, cy + s * 0.08),
                   (cx - s * 0.22, cy - s * 0.2)], fill=tcol)
        d.rectangle([cx - s * 0.3, cy + s * 0.12, cx + s * 0.3, cy + s * 0.18], fill=tcol)
    elif symbol == "corrosive":
        d.rectangle([cx - s * 0.42, cy - s * 0.42, cx - s * 0.12, cy - s * 0.32], fill=tcol)
        d.rectangle([cx + s * 0.12, cy - s * 0.42, cx + s * 0.42, cy - s * 0.32], fill=tcol)
        d.line([(cx - s * 0.3, cy - s * 0.3), (cx - s * 0.3, cy - s * 0.1)], fill=tcol, width=2)
        d.line([(cx + s * 0.3, cy - s * 0.3), (cx + s * 0.3, cy - s * 0.1)], fill=tcol, width=2)
        d.rectangle([cx - s * 0.5, cy - s * 0.02, cx + s * 0.5, cy + s * 0.06], fill=tcol)
    elif symbol == "oxidizer":
        d.ellipse([cx - s * 0.2, cy - s * 0.25, cx + s * 0.2, cy + s * 0.05], outline=tcol, width=2)
        d.polygon([(cx, cy - s * 0.6), (cx + s * 0.12, cy - s * 0.3), (cx - s * 0.12, cy - s * 0.3)], fill=tcol)
    elif symbol == "cylinder":
        d.rounded_rectangle([cx - s * 0.12, cy - s * 0.55, cx + s * 0.12, cy + s * 0.05], radius=3, fill=tcol)
    if text:
        W._t(d, (cx, cy + s * 0.55), text, max(8, int(s * 0.32)), tcol)
    if sub:
        W._t(d, (cx, cy + s * 0.22), sub, max(6, int(s * 0.16)), tcol, bold=False)


def _paint(colour, seed, hoops=True, ribs=False):
    img = PP._solid(colour, seed, 0.06).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 42) for c in colour) + (255,)
    light = tuple(min(255, c + 26) for c in colour) + (255,)
    if hoops:
        for y in (0, 40, 84, 124):
            d.rectangle([0, y, 127, y + 4], fill=dark)
            d.line([(0, y + 1), (127, y + 1)], fill=light)
    if ribs:
        for y in (30, 94):
            d.rectangle([0, y, 127, y + 7], fill=dark)
            d.line([(0, y + 2), (127, y + 2)], fill=light, width=2)
        d.rectangle([0, 0, 127, 5], fill=dark)
        d.rectangle([0, 122, 127, 127], fill=dark)
    return img


def _drum_red(label=False):
    img = _paint((168, 30, 26), 4201)
    if label:
        d = ImageDraw.Draw(img)
        d.rectangle([14, 50, 72, 78], fill=(246, 246, 240, 255))
        W._barcode(d, 18, 62, 68, 75, 4202)
        W._t(d, (43, 56), "UN1203", 9, (20, 20, 22))
        _diamond(d, 98, 64, 20, (214, 40, 34), edge=(250, 250, 250), text="3", symbol="flame", tcol=(250, 250, 250))
    return img


def _poly(label=False):
    img = _paint((222, 218, 200), 4211, hoops=False, ribs=True)
    if label:
        d = ImageDraw.Draw(img)
        d.rectangle([14, 50, 72, 80], fill=(246, 246, 240, 255), outline=(120, 116, 100, 255))
        W._barcode(d, 18, 64, 68, 77, 4212)
        W._t(d, (43, 57), "UN1830", 9, (20, 20, 22))
        _diamond(d, 98, 64, 20, ((250, 250, 250), (20, 20, 22)), edge=(20, 20, 22), text="8", symbol="corrosive", tcol=(250, 250, 250))
    return img


def _dewar(label=False):
    import nuclear_materials as M
    img = M.brushed((184, 188, 190), 4221, "v", dirt=0.04).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, 127)], fill=(150, 154, 158, 255), width=2)            # the weld seam
    for y in (6, 120):
        d.rectangle([0, y, 127, y + 3], fill=(156, 160, 164, 255))
    if label:
        d.rectangle([12, 44, 66, 86], fill=(246, 246, 240, 255))
        W._t(d, (39, 54), "LIQUID", 9, (20, 20, 22))
        W._t(d, (39, 66), "OXYGEN", 9, (20, 20, 22))
        W._t(d, (39, 78), "UN1073", 8, (20, 20, 22), bold=False)
        _diamond(d, 88, 52, 14, (30, 140, 70), edge=(250, 250, 250), text="2", symbol="cylinder", tcol=(250, 250, 250))
        _diamond(d, 108, 82, 14, (250, 210, 30), text="5.1", symbol="oxidizer")
    return img


def _lid(colour, poly=False, dewar=False):
    import nuclear_materials as M
    if dewar:
        img = M.brushed((184, 188, 190), 4231, "h", dirt=0.04).convert("RGBA").resize((128, 128))
        d = ImageDraw.Draw(img)
        d.ellipse([10, 10, 117, 117], outline=(140, 144, 148, 255), width=5)
        return img
    img = PP._solid(colour, 4232 + poly, 0.06).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 42) for c in colour) + (255,)
    d.ellipse([4, 4, 123, 123], outline=dark, width=6 if not poly else 9)
    for cx, r_ in ((40, 10), (90, 7)):
        d.ellipse([cx - r_, 60 - r_, cx + r_, 60 + r_], fill=(150, 152, 156, 255) if not poly else dark, outline=dark, width=2)
    return img


def _grating():
    """Yellow steel grating: bearing bars and cross bars over the dark of the sump under them."""
    img = Image.new("RGBA", (128, 128), (36, 34, 30, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 128, 10):
        d.rectangle([x, 0, x + 3, 127], fill=(220, 176, 30, 255))
        d.line([(x, 0), (x, 127)], fill=(248, 210, 70, 255))
    for y in range(0, 128, 32):
        d.rectangle([0, y, 127, y + 2], fill=(200, 160, 26, 255))
    return img


def _bund():
    img = PP._solid((226, 180, 30), 4241, 0.06).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 6], fill=(190, 150, 20, 255))
    d.rectangle([10, 40, 118, 88], fill=(250, 250, 244, 255))
    W._t(d, (64, 54), "SPILL", 16, (20, 20, 22))
    W._t(d, (64, 74), "CONTAINMENT", 13, (20, 20, 22))
    return img


def _hose():
    img = PP._solid((30, 30, 32), 4251, 0.05).convert("RGBA").resize((64, 64))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.line([(0, y), (63, y)], fill=(44, 44, 46, 255), width=2)
    return img


def _screen_page(title, tag, rows, accent=(36, 120, 90)):
    img = Image.new("RGB", (256, 192), (10, 16, 24))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 30], fill=accent)
    W._t(d, (10, 15), title, 15, (240, 244, 250), anchor="lm")
    W._t(d, (246, 15), tag, 11, (210, 240, 230), anchor="rm")
    for k, (a, b, col) in enumerate(rows):
        y = 50 + k * 30
        W._t(d, (12, y), a, 13, (160, 176, 190), anchor="lm", bold=False)
        W._t(d, (244, y), b, 13, col, anchor="rm")
    return img.resize((128, 96)).convert("RGBA").resize((128, 128))


def _hmi_fill():
    return _screen_page("DRUM FILLING", "DF-1", (("Liquid", "DIESEL", (236, 240, 246)), ("Drum", "3 OF 4  RED", (255, 190, 60)),
                                                  ("Filled", "14.5 / 20 B", (110, 210, 240)), ("Earth", "OK", (100, 230, 140)),
                                                  ("Magazine", "13 DRUMS", (236, 240, 246))))


def _hmi_empty():
    return _screen_page("DRUM EMPTYING", "DE-1", (("Order", "#1061  WATER", (236, 240, 246)), ("Drum", "2 OF 4", (255, 190, 60)),
                                                   ("Pumped", "33 / 80 B", (110, 210, 240)), ("Flow", "2.0 B/s", (110, 210, 240)),
                                                   ("Empties to", "DF-1", (100, 230, 140))), accent=(30, 76, 150))


def _indicator():
    """The weigh scale's indicator: its digits (the drum's fill, live in the game)."""
    img = Image.new("RGBA", (128, 64), (22, 26, 24, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 8, 121, 44], fill=(26, 50, 34, 255))
    W._t(d, (64, 26), "14.5 B", 26, (120, 250, 150))
    W._t(d, (64, 54), "NET   STABLE", 9, (170, 180, 176), bold=False)
    return img.resize((128, 128))


def _sight(level=0.6, colour=(196, 150, 50)):
    """A level gauge's glass: the liquid up to its level, the scale's ticks beside it (live in the game)."""
    img = Image.new("RGBA", (32, 128), (210, 220, 224, 255))
    d = ImageDraw.Draw(img)
    top = int(128 * (1 - level))
    d.rectangle([8, top, 23, 127], fill=colour + (255,))
    d.line([(8, top), (23, top)], fill=tuple(min(255, c + 50) for c in colour) + (255,), width=2)
    for y in range(0, 128, 16):
        d.line([(0, y), (6, y)], fill=(40, 40, 44, 255), width=2)
    return img.resize((128, 128))


def _plate(text, sub="", bg=(246, 246, 240), fg=(20, 20, 22)):
    img = Image.new("RGBA", (128, 48), bg + (255,))
    d = ImageDraw.Draw(img)
    W._t(d, (64, 20 if sub else 24), text, 20, fg)
    if sub:
        W._t(d, (64, 39), sub, 10, fg, bold=False)
    return img.resize((128, 128))


def _sign(top, sub, colour):
    img = Image.new("RGBA", (256, 64), colour + (255,))
    d = ImageDraw.Draw(img)
    W._t(d, (128, 24), top, 28, (250, 250, 250))
    W._t(d, (128, 50), sub, 12, (230, 236, 244), bold=False)
    return img.resize((128, 128))


def _panel_page(title, rows, accent):
    return _screen_page(title, "", rows, accent)


def _coupling():
    """The tanker coupling's dust cap seen end on: the cam arms, the cap's face."""
    img = PP._solid((176, 150, 60), 4261, 0.05).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.ellipse([16, 16, 111, 111], outline=(120, 100, 40, 255), width=8)
    d.ellipse([46, 46, 81, 81], fill=(120, 100, 40, 255))
    return img


def _eyewash():
    img = Image.new("RGBA", (128, 128), (20, 140, 70, 255))
    d = ImageDraw.Draw(img)
    W._t(d, (64, 40), "EMERGENCY", 15, (250, 250, 250))
    W._t(d, (64, 64), "EYEWASH", 18, (250, 250, 250))
    d.ellipse([46, 82, 82, 112], outline=(250, 250, 250, 255), width=4)
    return img


def _spill_kit():
    img = PP._solid((226, 180, 30), 4271, 0.05).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 20, 119, 70], fill=(20, 20, 22, 255))
    W._t(d, (64, 36), "SPILL KIT", 20, (240, 200, 40))
    W._t(d, (64, 58), "CHEMICAL", 12, (240, 240, 240), bold=False)
    return img


def _earth():
    img = Image.new("RGBA", (128, 64), (20, 120, 60, 255))
    d = ImageDraw.Draw(img)
    W._t(d, (64, 20), "EARTH", 18, (250, 250, 250))
    W._t(d, (64, 46), "CLAMP FIRST", 11, (230, 240, 230), bold=False)
    return img.resize((128, 128))


def _funnel():
    """The funnel's grate seen from above (buckets are tipped through it)."""
    img = Image.new("RGBA", (128, 128), (26, 28, 30, 255))
    d = ImageDraw.Draw(img)
    for x in range(4, 128, 12):
        d.rectangle([x, 0, x + 4, 127], fill=(176, 180, 184, 255))
    d.rectangle([0, 0, 127, 127], outline=(150, 154, 158, 255), width=6)
    return img


def w4_makers():
    import nuclear_materials as M
    out = {
        "pi_wh4_drum_red": _drum_red, "pi_wh4_drum_red_label": lambda: _drum_red(True), "pi_wh4_drum_red_top": lambda: _lid((168, 30, 26)),
        "pi_wh4_poly": _poly, "pi_wh4_poly_label": lambda: _poly(True), "pi_wh4_poly_top": lambda: _lid((222, 218, 200), poly=True),
        "pi_wh4_dewar": _dewar, "pi_wh4_dewar_label": lambda: _dewar(True), "pi_wh4_dewar_top": lambda: _lid(None, dewar=True),
        GRATING: _grating, BUND: _bund, HOSE: _hose,
        "pi_wh4_hmi_fill": _hmi_fill, "pi_wh4_hmi_empty": _hmi_empty, "pi_wh4_indicator": _indicator,
        "pi_wh4_sight": _sight, "pi_wh4_sight_water": lambda: _sight(0.45, (60, 120, 200)),
        "pi_wh4_lbl_liq1": lambda: _plate("LIQ 1"), "pi_wh4_lbl_liq2": lambda: _plate("LIQ 2"), "pi_wh4_lbl_liq3": lambda: _plate("LIQ 3"),
        "pi_wh4_lbl_liq4": lambda: _plate("LIQ 4"), "pi_wh4_lbl_out": lambda: _plate("OUT", "LIQUIDS"),
        "pi_wh4_lbl_tanker": lambda: _plate("TANKER", "DRY-BREAK 3\""), "pi_wh4_lbl_fill": lambda: _plate("FILL HERE", "BUCKETS"),
        "pi_wh4_lbl_tip": lambda: _plate("TIP BUCKETS", "HERE"),
        "pi_wh4_sign_in": lambda: _sign("LIQUIDS IN", "DRUM FILLING", (30, 120, 90)),
        "pi_wh4_sign_out": lambda: _sign("LIQUIDS OUT", "DRUM EMPTYING", (30, 76, 150)),
        "pi_wh4_scr_in": lambda: _panel_page("LIQUIDS IN", (("LIQ 1", "DIESEL 4 B/s", (100, 230, 140)), ("LIQ 2", "WATER WAITS", (255, 190, 60)),
                                                            ("LIQ 3", "NO PIPE", (120, 140, 160)), ("To drums", "DIESEL", (236, 240, 246)),
                                                            ("Tank", "28 / 80 B", (110, 210, 240))), (30, 120, 90)),
        "pi_wh4_scr_out": lambda: _panel_page("LIQUIDS OUT", (("Your orders", "3", (236, 240, 246)), ("#1061 WATER", "33 / 80 B", (100, 230, 140)),
                                                              ("Fill point", "HOLD A BUCKET", (255, 190, 60)), ("OUT pipe", "STREAM WATER", (110, 210, 240)),
                                                              ("Tank", "33 / 80 B", (110, 210, 240))), (30, 76, 150)),
        "pi_wh4_coupling": _coupling, "pi_wh4_eyewash": _eyewash, "pi_wh4_spill_kit": _spill_kit, "pi_wh4_earth": _earth,
        "pi_wh4_funnel": _funnel, "pi_wh4_copper": lambda: M.brushed((190, 120, 70), 4281, "h", dirt=0.05),
    }
    return out


W4_SHARP = {"pi_wh4_drum_red_label", "pi_wh4_poly_label", "pi_wh4_dewar_label", "pi_wh4_hmi_fill", "pi_wh4_hmi_empty",
            "pi_wh4_indicator", "pi_wh4_sight", "pi_wh4_sight_water", "pi_wh4_lbl_liq1", "pi_wh4_lbl_liq2", "pi_wh4_lbl_liq3",
            "pi_wh4_lbl_liq4", "pi_wh4_lbl_out", "pi_wh4_lbl_tanker", "pi_wh4_lbl_fill", "pi_wh4_lbl_tip", "pi_wh4_sign_in",
            "pi_wh4_sign_out", "pi_wh4_scr_in", "pi_wh4_scr_out", "pi_wh4_eyewash", "pi_wh4_spill_kit", "pi_wh4_earth", GRATING, BUND}
W4_SEE_THROUGH = set()


# ================================================================================================ the drums
def drum(kind, cx, cz, y, label=True):
    """One drum standing at (cx, cz) on y: a steel drum (blue, or red for a flammable liquid: its rolling hoops proud,
    its two bungs), an HDPE drum (its moulded ribs, the bungs in the lid) or a stainless liquid dewar (its shoulder,
    the guard ring on four posts round the valves and the pressure gauge). Its label faces +z."""
    side, top, lab = DRUMS[kind]
    if kind == "dewar":
        h = DEWAR_H
        out = W.drum_y(cx, cz, 3.1, y, y + h - 1.0, side, top, lab if label else None)
        out += round_y(cx, cz, 2.5, y + h - 1.0, y + h - 0.4, side) + round_y(cx, cz, 0.9, y + h - 0.4, y + h + 0.6, STEEL)
        # The guard ring's four posts and its two bars, the liquid and vent valves' handwheels, the gauge.
        for dx, dz in ((-2.1, -2.1), (2.1, -2.1), (-2.1, 2.1), (2.1, 2.1)):
            out.append(box(cx + dx - 0.3, y + h - 1.0, cz + dz - 0.3, cx + dx + 0.3, y + h + 2.0, cz + dz + 0.3, STAINLESS))
        out += [box(cx - 2.4, y + h + 1.6, cz - 2.4, cx + 2.4, y + h + 2.0, cz - 1.8, STAINLESS),
                box(cx - 2.4, y + h + 1.6, cz + 1.8, cx + 2.4, y + h + 2.0, cz + 2.4, STAINLESS)]
        out += round_y(cx - 1.0, cz, 0.7, y + h + 0.6, y + h + 0.9, "pi_wh_lamp_blue") + round_y(cx + 1.0, cz - 0.4, 0.6, y + h + 0.6, y + h + 0.9, W.BLACK)
        out += [box(cx + 0.2, y + h + 0.6, cz + 0.9, cx + 1.4, y + h + 1.8, cz + 1.2, CHROME)]
        return out
    out = W.drum_y(cx, cz, DRUM_R, y, y + DRUM_H, side, top, lab if label else None)
    hoop = side
    for y0 in ((3.0, 6.0) if kind != "poly" else (2.8, 6.2)):
        out += round_y(cx, cz, DRUM_R + 0.12, y + y0, y + y0 + 0.75, hoop)
    # The bungs, standing proud of the lid.
    out += [box(cx - 1.9, y + DRUM_H, cz - 0.5, cx - 0.9, y + DRUM_H + 0.3, cz + 0.5, STEEL if kind != "poly" else side),
            box(cx + 1.0, y + DRUM_H, cz - 0.35, cx + 1.7, y + DRUM_H + 0.25, cz + 0.35, STEEL if kind != "poly" else side)]
    if kind == "red":
        out += [box(cx - 0.4, y + 0.8, cz - DRUM_R - 0.3, cx + 0.4, y + 1.6, cz - DRUM_R + 0.2, CHROME)]      # the earthing boss
    return out


def drum_load(kind, n=4, y=W.PALLET_H):
    """n drums of a kind on a pallet (y its top), ring-strapped together."""
    out = []
    for k, (cx, cz) in enumerate(SPOTS[:max(1, min(4, n))]):
        out += drum(kind, cx, cz, y, label=cz > 8)
    h = DEWAR_H if kind == "dewar" else DRUM_H
    if n >= 2:
        out += W._straps(y + 1.0, y + h - 1.2, 1, ring=True)
    return out


def drum_item(kind):
    """An empty drum's item: the drum alone."""
    return drum(kind, 8.0, 8.0, 0.0)


# ================================================================================================ the drum filling station
FILL_DECK = 1.6         # the pallet's underside on the scale's chain strands
RAIL_Y = 44.0           # the gantry's rails' underside
HEAD_Y = RAIL_Y - 2.4   # the head's frame: the bridge's underside (the trolley runs under it)
MAG_Y = 1.8             # the empty drums stand on the magazine's floor rollers here
# The magazine's eight places (x, z), the one nearest the fill bay first; any kind of drum in any of them.
MAG_SPOTS = ((11.4, 27.0), (11.4, 19.8), (11.4, 12.2), (11.4, 4.8), (4.6, 27.0), (4.6, 19.8), (4.6, 12.2), (4.6, 4.8))
# The gantry robot's Z axis: the tool plate's underside hangs DROP_MIN (carrying, raised) to DROP_MAX under the
# head's frame; the inner mast is MAST long, the sleeve on the trolley SLEEVE.
DROP_MIN, DROP_MAX, MAST, SLEEVE = 16.0, 31.0, 20.0, 14.0
LANCE = 9.4             # how far the lance reaches down out of the tool plate
PARK = (24.0, 8.0)      # where the head waits (over the pallet magazine)


def _magazine(stock=0):
    """The empty drum magazine (the left column): a yellow frame round a floor of rollers with eight places marked,
    a guard on its outside; stock: drums drawn standing in the first places (the game draws them live)."""
    out = []
    for x0 in (0.4, 14.6):
        for z0 in (0.4, 30.6):
            out.append(box(x0, 0.0, z0, x0 + 1.0, 14.0, z0 + 1.0, YELLOW))
            out.append(box(x0 - 0.2 if x0 > 1 else x0, 0.0, z0 - 0.2 if z0 > 1 else z0, x0 + 1.2, 0.3, z0 + 1.2 if z0 < 1 else z0 + 1.0, STEEL))
    out += [box(1.4, MAG_Y - 0.6, 0.6, 14.6, MAG_Y, 30.4, {"*": GRAPHITE, "up": "pi_wh_rollers"}),
            box(0.4, MAG_Y - 1.4, 0.4, 15.6, MAG_Y - 0.6, 1.4, YELLOW), box(0.4, MAG_Y - 1.4, 30.6, 15.6, MAG_Y - 0.6, 31.6, YELLOW)]
    for z in (8.5, 16.0, 23.4):
        out.append(box(1.4, MAG_Y, z - 0.2, 14.6, MAG_Y + 1.2, z + 0.2, YELLOW))          # the places' dividers
    out += [box(0.0, 2.0, 1.4, 0.3, 14.0, 30.6, {"east": GUARD, "west": GUARD}), box(1.4, 13.0, 0.4, 14.6, 14.0, 1.4, YELLOW),
            box(1.4, 13.0, 30.6, 14.6, 14.0, 31.6, YELLOW)]
    for k, (cx, cz) in enumerate(MAG_SPOTS[:stock]):
        out += drum(("steel", "red", "poly", "dewar")[k % 4], cx, cz, MAG_Y, label=False)
    return out


def gantry_bridge():
    """The gantry's bridge (its own frame: x 0 its middle, along z across the rails), with its end trucks."""
    return [box(-2.0, RAIL_Y - 2.4, 0.4, 2.0, RAIL_Y, 31.6, YELLOW),
            box(-2.6, RAIL_Y - 0.4, 0.0, 2.6, RAIL_Y + 2.4, 2.8, MACHINE), box(-2.6, RAIL_Y - 0.4, 29.2, 2.6, RAIL_Y + 2.4, 32.0, MACHINE)]


def head_trolley():
    """The head's trolley under the bridge (its frame: the Z axis at (0, 0), y 0 the bridge's underside): the
    trolley, its drive motor, the Z axis's sleeve the mast slides in."""
    return [box(-2.6, -3.0, -2.6, 2.6, 0.0, 2.6, MACHINE), box(1.6, -6.0, -1.0, 3.4, -2.0, 1.0, GRAPHITE),
            box(-1.6, -SLEEVE, -1.6, 1.6, -3.0, 1.6, STAINLESS), box(-1.8, -SLEEVE - 0.6, -1.8, 1.8, -SLEEVE, 1.8, GRAPHITE)]


def head_mast():
    """The Z axis's mast and its tool (its frame: y 0 the vacuum cups' faces): the inner mast, the filling valve
    on the tool plate, the plate's square frame and its two vacuum cups (front and back of the lance: they lift a
    drum by its lid, a dewar by its guard ring's bars), the lance's guide under the valve."""
    out = [box(-1.0, 1.2, -1.0, 1.0, 1.2 + MAST, 1.0, CHROME), box(-1.3, 1.2, -1.3, 1.3, 3.6, 1.3, STAINLESS)]
    out += [box(-2.6, 0.6, 1.8, 2.6, 1.2, 2.6, GRAPHITE), box(-2.6, 0.6, -2.6, 2.6, 1.2, -1.8, GRAPHITE),
            box(1.6, 0.6, -1.8, 2.2, 1.2, 1.8, GRAPHITE), box(-2.2, 0.6, -1.8, -1.6, 1.2, 1.8, GRAPHITE)]
    for z in (-2.2, 2.2):
        out += round_y(0.0, z, 0.7, 0.0, 0.6, RUBBER)
    out += round_y(0.0, 0.0, 0.8, 0.6, 1.2, STEEL)
    return out


def head_lance():
    """The filling lance (its frame: y 0 the tool plate's underside, reaching down LANCE; the game slides it up
    into the mast when it isn't filling)."""
    return round_y(0.0, 0.0, 0.45, -LANCE, 0.6, CHROME)


def filler_head(lance=0.0, drop=None):
    """The whole head at a pose (its frame as head_trolley's): the tool plate drop under it, the lance out lance."""
    d = DROP_MIN if drop is None else drop
    return head_trolley() + shifted(head_mast(), 0.0, -d, 0.0) + shifted(head_lance(), 0.0, -d + LANCE * (1.0 - lance), 0.0)


def gantry(bx=None):
    """The gantry over the station: its two rails along x on four posts, the cable carrier along the back rail; the
    bridge at bx (the game draws it moving)."""
    out = []
    for z0 in (0.4, 29.6):
        out += [box(0.4, RAIL_Y, z0, 47.6, RAIL_Y + 1.6, z0 + 2.0, YELLOW), box(0.4, RAIL_Y + 1.6, z0 + 0.6, 47.6, RAIL_Y + 2.0, z0 + 1.4, STEEL)]
    for x0 in (0.4, 46.4):
        for z0 in (0.4, 30.0):
            out += [box(x0, 0.0 if x0 > 1 else 14.0, z0, x0 + 1.2, RAIL_Y, z0 + 1.2, YELLOW)]
            if x0 > 1:
                out.append(box(46.2, 0.0, z0 - 0.2 if z0 > 1 else z0, 47.8, 0.3, z0 + 1.4 if z0 < 1 else z0 + 1.2, STEEL))
    out += [box(1.0, RAIL_Y + 2.0, 2.4, 46.6, RAIL_Y + 3.2, 4.0, {"*": GRAPHITE, "up": "pi_wh_strap"})]     # the cable carrier's track
    if bx is not None:
        out += shifted(gantry_bridge(), bx, 0.0, 0.0)
    return out


def _tank(cx, cz, y0, y1, sight, inlet=True):
    """A stainless vertical tank (dished head, three legs, the manway, the vent with its flame arrester) with its
    level gauge's glass on the front (sight: its texture, live in the game)."""
    out = round_y(cx, cz, TANK_R, y0, y1, STAINLESS)
    out += round_y(cx, cz, TANK_R - 1.2, y1, y1 + 1.2, STAINLESS) + round_y(cx, cz, TANK_R - 2.8, y1 + 1.2, y1 + 2.0, STAINLESS)
    out += round_y(cx, cz, 2.2, y1 + 2.0, y1 + 2.8, STEEL) + round_y(cx + 2.4, cz - 2.0, 0.6, y1 + 1.2, y1 + 4.0, STEEL)
    out += [box(cx + 1.6, y1 + 4.0, cz - 2.8, cx + 3.2, y1 + 5.2, cz - 1.2, GRAPHITE)]
    for a in (90, 210, 330):
        r = math.radians(a)
        lx, lz = cx + (TANK_R - 1.0) * math.cos(r), cz + (TANK_R - 1.0) * math.sin(r)
        out += [box(lx - 0.6, 0.0, lz - 0.6, lx + 0.6, y0, lz + 0.6, STEEL), box(lx - 1.0, 0.0, lz - 1.0, lx + 1.0, 0.3, lz + 1.0, STEEL)]
    # The level gauge: its glass in a steel channel, its two valves top and bottom.
    zf = cz + TANK_R + 0.2
    out += [box(cx - 1.0, y0 + 2.0, zf, cx + 1.0, y1 - 2.0, zf + 0.6, STEEL),
            box(cx - 0.6, y0 + 2.4, zf + 0.6, cx + 0.6, y1 - 2.4, zf + 0.8, {"*": STEEL, "south": sight + "!"})]
    for y in (y0 + 1.6, y1 - 2.4):
        out += [box(cx - 1.4, y, zf - 1.0, cx + 1.4, y + 0.8, zf + 1.2, CHROME)]
    return out


def drum_filler(stock=6, bx=None, head=None, lance=0.0, drop=None, sight="pi_wh4_sight", pallets=4):
    """The Drum Filling Station (three wide, three high, two deep; its back against the wall behind which the
    Liquids In panel stands, its front to the lanes): on the left the empty drum magazine; in the middle at the back
    the empty pallet magazine, at the front the fill bay (the weigh scale with its chain strands; the pallet drone
    takes a full pallet from here); on the right the buffer tank (its level gauge) with its pump, the inlet header and
    the control cabinet (HMI, the scale's indicator, the earthing reel, stack light, E-stop). Over all of it the
    gantry robot: its Z axis's vacuum tool lifts empty drums from the magazine onto the pallet, its lance fills them
    through the bung (bottom up). stock, pallets: drums and pallets drawn in the magazines; bx, head (x, z), drop,
    lance: the gantry's pose (none: the station as its model, the game drawing all that moves)."""
    out = _magazine(stock)
    # ---- the middle: the pallet magazine at the back (low: the drums go over it).
    for x0 in (16.6, 30.4):
        for z0 in (0.4, 14.6):
            out.append(box(x0, 0.0, z0, x0 + 1.0, 12.0, z0 + 1.0, YELLOW))
    out += [box(17.6, 10.8, 0.4, 30.4, 12.0, 1.4, YELLOW), box(17.6, 10.8, 14.6, 30.4, 12.0, 15.6, YELLOW)]
    for x0 in (18.2, 28.2):
        out += [box(x0, 0.0, 0.8, x0 + 1.6, 1.3, 16.0, {"*": GRAPHITE, "up": "pi_wh_strap"})]
    for k in range(pallets):
        out += shifted(W.pallet(), 16.0, 1.3 + k * (W.PALLET_H + 0.02), 0.0)
    # ---- the fill bay: the weigh scale (its platform on four load cells), its chain strands, the drip edge.
    out += [box(16.8, 0.4, 16.8, 31.2, 1.0, 31.2, {"*": STEEL, "up": "pi_wh_deck"})]
    for x, z in ((17.6, 17.6), (30.4, 17.6), (17.6, 30.4), (30.4, 30.4)):
        out += round_y(x, z, 0.6, 0.0, 0.4, CHROME)
    for x0 in (18.2, 28.2):
        out += [box(x0, 1.0, 16.4, x0 + 1.6, FILL_DECK, 31.2, {"*": GRAPHITE, "up": "pi_wh_strap"})]
    out += [box(16.4, 0.0, 16.4, 16.8, 1.4, 31.6, YELLOW), box(31.2, 0.0, 16.4, 31.6, 1.4, 31.6, YELLOW)]
    # The light curtain's post at the bay's open front right corner (the magazine's post is the left one), clear of
    # the pallet's sides.
    out += [box(32.4, 0.0, 30.8, 33.4, 18.0, 31.8, {"*": BLACK, "west": GRAPHITE})]
    # ---- the right: the buffer tank, its pump and the inlet header (two flanged inlets through the back).
    out += _tank(40.0, 8.0, 4.0, 36.0, sight)
    out += [box(33.6, 0.0, 12.4, 38.4, 0.4, 16.0, STEEL)]
    out += round_x(3.2, 14.2, 1.8, 34.0, 37.0, "pi_wh_spk_red") + round_z(38.0, 2.6, 1.4, 13.0, 15.4, STEEL)
    out += [box(37.6, 2.6, 13.6, 38.4, 43.0, 14.4, STAINLESS)]                         # the riser to the gantry's hose
    out += round_x(10.0, 1.6, 0.9, 32.0, 47.0, STAINLESS)                             # the header along the back
    for x in (36.0, 44.0):
        out += round_z(x, 10.0, 2.2, 0.0, 0.6, GRAPHITE) + round_z(x, 10.0, 1.2, 0.6, 1.6, STEEL)
        out += round_y(x, 3.4, 0.6, 10.9, 13.4, STEEL) + round_y(x, 3.4, 1.6, 13.4, 13.7, "pi_wh_spk_red")   # its valve's handwheel
    out += round_y(41.0, 4.0, 0.7, 10.0, 36.4, STAINLESS)                              # up into the tank's top
    # ---- the control cabinet at the front right.
    out += [box(33.2, 0.0, 18.0, 46.0, 30.0, 30.0, MACHINE), box(33.0, 30.0, 17.8, 46.2, 30.8, 30.2, MACHINE_DARK)]
    out += [box(39.5, 1.0, 30.0, 39.7, 29.0, 30.1, MACHINE_DARK)]
    out += W.screen_unit(34.6, 19.4, 44.6, 27.0, 30.0, "pi_wh4_hmi_fill", hood=False)
    out += [box(35.0, 12.4, 30.0, 41.0, 16.6, 30.6, {"*": GRAPHITE, "south": "pi_wh4_indicator!"})]
    out += [box(42.4, 13.0, 30.0, 44.6, 15.2, 30.6, YELLOW)] + round_z(43.5, 14.1, 0.65, 30.6, 31.4, "pi_wh_estop")
    out += [box(36.0, 4.0, 30.0, 37.0, 9.0, 30.4, CHROME), box(41.0, 4.0, 30.0, 42.0, 9.0, 30.4, CHROME)]
    # The stack light on the cabinet; the earthing reel on its side, its lamp and plate.
    out += round_y(44.6, 20.0, 0.5, 30.8, 31.6, GRAPHITE)
    for k, lamp in enumerate(("pi_wh_lamp_green", "pi_wh_lamp_amber", "pi_wh_lamp_red")):
        out += PP.round_y(44.6, 20.0, 0.75, 31.6 + k * 1.3, 32.8 + k * 1.3, lamp)
    out += round_x(22.0, 24.0, 2.4, 32.0, 33.2, "pi_wh_lamp_green") + round_x(22.0, 24.0, 1.4, 31.6, 33.2, GRAPHITE)
    out += [box(32.6, 15.0, 22.0, 33.2, 18.0, 26.0, {"*": GRAPHITE, "west": "pi_wh4_earth!"})]
    out += gantry(bx)
    if head is not None:
        hx, hz = head
        out += shifted(filler_head(lance, drop), hx, HEAD_Y, hz)
    return out


# ================================================================================================ the drum emptying station
EMPTY_DECK = 2.0        # the grating's top: a pallet set on the platform stands here
JIB = (24.0, 7.0, 38.0)  # the jib's pillar (x, z) and its arm's underside (y)


def drum_pump(dip=1.0):
    """The air-operated drum pump (its own frame: the dip tube's axis at (0, 0), y 0 at the drum's top): the motor
    with its muffler and the air line, the outlet spout and its hose, the dip tube down through the bung (dip 1 at
    the drum's bottom), the bung adaptor."""
    lift = 10.0 * (1.0 - dip)
    out = round_y(0.0, 0.0, 0.45, -9.0 + lift, 4.0 + lift, STAINLESS)
    out += round_y(0.0, 0.0, 0.9, 0.0 + lift, 0.6 + lift, GRAPHITE)                     # the bung adaptor
    out += round_y(0.0, 0.0, 1.0, 4.0 + lift, 5.4 + lift, STAINLESS)
    out += round_y(0.0, 0.0, 1.3, 5.4 + lift, 8.6 + lift, "pi_wh_spk_red") + round_y(0.0, 0.0, 0.9, 8.6 + lift, 9.6 + lift, GRAPHITE)
    out += [box(1.2, 6.2 + lift, -0.5, 2.6, 7.4 + lift, 0.5, CHROME)]                    # the muffler
    out += [box(-0.5, 4.6 + lift, 0.8, 0.5, 5.2 + lift, 3.4, STAINLESS), box(-0.6, 3.4 + lift, 2.8, 0.6, 5.2 + lift, 4.0, HOSE)]
    return out


JIB_ARM = 24.0          # the jib's arm's length (its balancer's trolley runs out to JIB_ARM - 1.8)


def jib(arm=JIB_ARM, balancer=True):
    """The jib crane's arm (its own frame: the pillar's axis at (0, 0), y 0 at the arm's underside; the arm along +z
    to arm): the I-beam, its pivot collar, the trolley's balancer at the end (the game runs it along the arm)."""
    out = [box(-0.8, 0.0, -1.6, 0.8, 2.2, arm, YELLOW), box(-1.6, 0.0, -1.6, 1.6, 0.4, arm, YELLOW), box(-1.6, 1.8, -1.6, 1.6, 2.2, arm, YELLOW)]
    out += round_y(0.0, 0.0, 1.8, -1.0, 3.0, YELLOW)
    if balancer:
        out += shifted(jib_balancer(), 0.0, 0.0, arm - 1.8)
    return out


def jib_balancer():
    """The balancer on its trolley under the arm (its frame: the cable's line at (0, 0), y 0 the arm's underside)."""
    return [box(-1.2, -2.6, -1.2, 1.2, 0.0, 1.2, GRAPHITE), box(-1.6, -0.4, -1.6, 1.6, 0.0, 1.6, STEEL)]


def drum_emptier(sight="pi_wh4_sight"):
    """The Drum Emptying Station (three wide, three high, two deep; its back against the wall behind which the Liquids
    Out panel stands, its front to the lanes, by Ready Storage): in the middle the bunded platform (a yellow spill
    pallet under a grating) where a pallet drone sets a pallet of drums, the jib crane's pillar behind it (the arm
    and the drum pump on it are drawn by the game, swinging from drum to drum); on the right the holding tank (its
    level gauge), its transfer pump to the OUT flange through the back, the control box (HMI, stack light, E-stop);
    on the left the emergency eyewash and the chemical spill kit."""
    out = []
    # ---- the bunded platform: the spill pallet (its sump), the grating, the roller-less deck the pallet stands on.
    out += [box(16.4, 0.0, 16.4, 31.6, EMPTY_DECK - 0.2, 31.6, {"*": BUND, "up": GRATING, "down": YELLOW})]
    out += [box(16.0, 0.0, 16.0, 32.0, 0.4, 32.0, YELLOW)]
    for x0 in (14.6, 32.4):
        out += [box(x0, 0.0, 30.8, x0 + 1.0, 18.0, 31.8, {"*": BLACK, "south": GRAPHITE})]
    # ---- the jib crane's pillar at the back of the middle (its base plate and anchor bolts).
    out += [box(20.0, 0.0, JIB[1] - 4.0, 28.0, 0.6, JIB[1] + 4.0, YELLOW)]
    for x, z in ((21.0, JIB[1] - 3.0), (27.0, JIB[1] - 3.0), (21.0, JIB[1] + 3.0), (27.0, JIB[1] + 3.0)):
        out += round_y(x, z, 0.4, 0.6, 1.2, STEEL)
    out += round_y(JIB[0], JIB[1], 1.6, 0.6, JIB[2] - 1.0, YELLOW)
    # ---- the right: the holding tank, its outlet and pump out through the back, the control box at the front.
    out += _tank(40.0, 8.0, 4.0, 36.0, sight)
    out += round_y(40.0, 8.0, 0.8, 1.0, 4.0, STAINLESS) + round_x(1.4, 8.0, 0.8, 34.0, 40.0, STAINLESS)
    out += round_x(3.0, 4.6, 1.6, 33.0, 36.0, "pi_wh_spk_red") + round_z(37.0, 3.0, 1.3, 2.6, 5.4, STEEL)
    out += round_z(37.0, 3.0, 0.8, 0.0, 2.6, STAINLESS) + round_z(37.0, 3.0, 2.0, 0.0, 0.5, GRAPHITE)
    out += [box(33.2, 0.0, 20.0, 46.8, 26.0, 30.0, MACHINE), box(33.0, 26.0, 19.8, 47.0, 26.8, 30.2, MACHINE_DARK)]
    out += W.screen_unit(34.8, 16.0, 44.8, 23.6, 30.0, "pi_wh4_hmi_empty", hood=False)
    out += [box(42.4, 9.4, 30.0, 44.6, 11.6, 30.6, YELLOW)] + round_z(43.5, 10.5, 0.65, 30.6, 31.4, "pi_wh_estop")
    out += [box(36.0, 3.0, 30.0, 37.0, 8.0, 30.4, CHROME)]
    out += round_y(45.0, 22.0, 0.5, 26.8, 27.6, GRAPHITE)
    for k, lamp in enumerate(("pi_wh_lamp_green", "pi_wh_lamp_amber", "pi_wh_lamp_red")):
        out += PP.round_y(45.0, 22.0, 0.75, 27.6 + k * 1.3, 28.8 + k * 1.3, lamp)
    # The hose from the pump's swivel on the jib pillar to the tank's inlet (along the pillar and over to the tank).
    out += [box(25.6, 30.0, JIB[1] - 0.6, 26.4, 36.0, JIB[1] + 0.2, HOSE), box(26.4, 35.2, JIB[1] - 0.6, 36.0, 36.0, JIB[1] + 0.2, HOSE),
            box(35.2, 34.4, JIB[1] - 0.6, 36.0, 36.4, JIB[1] + 0.2, STAINLESS)]
    # ---- the left: the eyewash (its yellow pedestal at the front, the bowl, the push flag; its green sign on the wall
    # behind) and the spill kit against the wall, clear of the eyewash.
    out += round_y(14.0, 25.0, 0.7, 0.0, 14.0, YELLOW) + [box(12.0, 0.0, 23.0, 16.0, 0.4, 27.0, YELLOW)]
    out += round_y(14.0, 25.0, 2.6, 14.0, 15.2, YELLOW) + round_y(14.0, 25.0, 2.0, 15.2, 15.6, STEEL)
    out += [box(8.4, 12.4, 24.6, 11.4, 13.0, 25.4, YELLOW), box(8.4, 10.4, 24.6, 9.0, 13.0, 25.4, YELLOW)]
    out += [box(7.0, 18.0, 0.0, 15.0, 26.0, 0.6, {"*": STEEL, "south": "pi_wh4_eyewash!"})]
    out += [box(0.6, 0.0, 2.0, 11.6, 10.0, 11.0, {"*": YELLOW, "south": "pi_wh4_spill_kit!"}), box(0.2, 10.0, 1.6, 12.0, 11.0, 11.4, YELLOW)]
    out += [box(4.1, 10.2, 11.4, 8.1, 10.8, 12.2, BLACK)]
    return out


# ================================================================================================ the wall panels
def liquids_in_panel():
    """The Liquids In wall panel (three wide, two high, in the outer wall behind the Drum Filling Station, its face
    outside, +z): four flanged inlets (LIQ 1 to LIQ 4) with their flow lamps; in the middle the tanker coupling (a 3"
    dry-break coupling with its dust cap on a chain) low down, over it the funnel buckets are tipped into (its grate,
    the drip lip), the screen; the LIQUIDS IN sign under the canopy, the wall light."""
    face = {"*": STEEL, "south": "pi_wh_panel_face", "north": STEEL}
    out = [box(0.0, 0.0, 0.0, 48.0, 32.0, 1.0, face)]
    out += [box(0.0, 0.0, 1.0, 1.0, 30.0, 2.0, GRAPHITE), box(47.0, 0.0, 1.0, 48.0, 30.0, 2.0, GRAPHITE),
            box(1.0, 0.0, 1.0, 47.0, 0.8, 2.0, GRAPHITE), box(16.0, 0.8, 1.0, 16.8, 30.0, 1.6, GRAPHITE), box(31.2, 0.8, 1.0, 32.0, 30.0, 1.6, GRAPHITE)]
    out += [box(0.0, 30.0, 0.0, 48.0, 31.0, 2.0, GRAPHITE), box(0.0, 31.0, 0.0, 48.0, 31.6, 9.0, {"*": GRAPHITE, "up": STEEL}),
            box(0.0, 30.4, 9.0, 48.0, 31.6, 9.8, STEEL)]
    for x in (1.0, 15.6, 31.4, 46.0):
        out.append(box(x, 26.0, 1.0, x + 1.0, 31.0, 2.0, GRAPHITE))
        out.append(box(x, 27.2, 2.0, x + 1.0, 28.0, 8.0, GRAPHITE, rot_x(x, 27.6, 2.0, 22.5)))
    out += [box(21.0, 29.4, 2.4, 27.0, 31.0, 6.0, GRAPHITE), box(21.4, 29.2, 2.8, 26.6, 29.4, 5.6, "pi_wh_wallpack")]
    out += [box(17.0, 26.4, 1.6, 31.0, 29.6, 1.9, {"*": STEEL, "south": "pi_wh4_sign_in!"})]
    for (cx, cy), label in (((8.0, 8.0), "pi_wh4_lbl_liq1"), ((40.0, 8.0), "pi_wh4_lbl_liq2"), ((8.0, 20.0), "pi_wh4_lbl_liq3"),
                            ((40.0, 20.0), "pi_wh4_lbl_liq4")):
        out += W._flange(cx, cy, 1.0, label, "pi_wh_lamp_green")
    # The tanker coupling: its pipe stub and valve (the lever), the coupling's cam arms, the dust cap on its chain.
    out += round_z(24.0, 5.0, 1.8, 1.0, 3.0, STEEL) + round_z(24.0, 5.0, 2.4, 3.0, 4.4, "pi_wh_spk_red")
    out += [box(26.0, 6.2, 3.2, 31.0, 6.8, 3.8, "pi_wh_spk_red")]
    out += round_z(24.0, 5.0, 2.0, 4.4, 6.0, "pi_wh4_copper") + round_z(24.0, 5.0, 1.8, 6.0, 7.0, "pi_wh4_copper")
    out += [box(22.0, 5.0, 6.8, 26.0, 5.4, 7.2, {"*": "pi_wh4_copper", "south": "pi_wh4_coupling!"})]
    out += [box(25.6, 2.2, 6.4, 26.0, 5.0, 6.8, CHAIN_())]
    out += [box(19.0, 9.2, 1.0, 29.0, 10.6, 1.4, {"*": STEEL, "south": "pi_wh4_lbl_tanker!"})]
    # The funnel: its stainless hopper out from the wall, the grate on top, the drip lip, its label.
    out += [box(19.6, 12.0, 1.0, 28.4, 13.0, 6.6, STAINLESS), box(19.0, 13.0, 1.0, 29.0, 16.0, 7.4, STAINLESS),
            box(19.2, 16.0, 1.2, 28.8, 16.2, 7.2, {"*": STAINLESS, "up": "pi_wh4_funnel"})]
    out += [box(19.0, 17.0, 1.0, 29.0, 18.2, 1.4, {"*": STEEL, "south": "pi_wh4_lbl_tip!"})]
    out += W.screen_unit(19.4, 20.2, 28.6, 25.0, 1.0, "pi_wh4_scr_in", hood=False)
    return W._in_wall(out)


def CHAIN_():
    return "pi_wh_chain"


def liquids_out_panel(bucket=True):
    """The Liquids Out wall panel (three wide, two high, in the outer wall behind the Drum Emptying Station, its face
    outside, +z): the OUT flange (orders and streams sent to pipes); in the middle the filling point: a recess with
    the dispensing nozzle on its swivel hose and holster, a grated drip tray (a bucket held under the nozzle fills
    with your order), its FILL HERE label, the screen (your liquid orders); the sight flow glass; the LIQUIDS OUT sign
    under the canopy, the wall light."""
    face = {"*": STEEL, "south": "pi_wh_panel_face", "north": STEEL}
    hole = (17.0, 3.0, 31.0, 17.0)
    out = [box(pa[0], pb[0], 0.0, pa[1], pb[1], 1.0, face) for pa, pb in W._minus(0.0, 48.0, 0.0, 32.0, hole)]
    out += [box(0.0, 0.0, 1.0, 1.0, 30.0, 2.0, GRAPHITE), box(47.0, 0.0, 1.0, 48.0, 30.0, 2.0, GRAPHITE),
            box(1.0, 0.0, 1.0, 47.0, 0.8, 2.0, GRAPHITE), box(16.0, 0.8, 1.0, 16.8, 30.0, 1.6, GRAPHITE), box(31.2, 0.8, 1.0, 32.0, 30.0, 1.6, GRAPHITE)]
    out += [box(0.0, 30.0, 0.0, 48.0, 31.0, 2.0, GRAPHITE), box(0.0, 31.0, 0.0, 48.0, 31.6, 9.0, {"*": GRAPHITE, "up": STEEL}),
            box(0.0, 30.4, 9.0, 48.0, 31.6, 9.8, STEEL)]
    for x in (1.0, 15.6, 31.4, 46.0):
        out.append(box(x, 26.0, 1.0, x + 1.0, 31.0, 2.0, GRAPHITE))
        out.append(box(x, 27.2, 2.0, x + 1.0, 28.0, 8.0, GRAPHITE, rot_x(x, 27.6, 2.0, 22.5)))
    out += [box(21.0, 29.4, 2.4, 27.0, 31.0, 6.0, GRAPHITE), box(21.4, 29.2, 2.8, 26.6, 29.4, 5.6, "pi_wh_wallpack")]
    out += [box(17.0, 26.4, 1.6, 31.0, 29.6, 1.9, {"*": STEEL, "south": "pi_wh4_sign_out!"})]
    out += W._flange(8.0, 10.0, 1.0, "pi_wh4_lbl_out", "pi_wh_lamp_green")
    # The filling point's recess: its stainless lining, the drip tray's grating, the nozzle's holster and hose.
    out += [box(17.0, 3.0, -5.0, 31.0, 3.6, 1.0, STAINLESS), box(17.0, 16.4, -5.0, 31.0, 17.0, 1.0, STAINLESS),
            box(17.0, 3.6, -5.0, 17.6, 16.4, 1.0, STAINLESS), box(30.4, 3.6, -5.0, 31.0, 16.4, 1.0, STAINLESS),
            box(17.6, 3.6, -5.0, 30.4, 16.4, -4.6, STAINLESS)]
    out += [box(17.6, 3.6, -4.6, 30.4, 4.4, 2.4, {"*": STAINLESS, "up": GRATING})]
    out += round_z(24.0, 14.6, 0.9, -4.6, -2.4, CHROME) + [box(23.4, 10.0, -2.8, 24.6, 14.6, -2.0, HOSE)]
    out += [box(22.8, 8.0, -3.0, 25.2, 10.0, -1.4, "pi_wh_spk_red"), box(23.6, 6.6, -2.6, 24.4, 8.0, -1.8, CHROME)]
    if bucket:
        out += round_y(24.0, -1.2, 2.2, 4.4, 8.6, CHROME)
    out += [box(17.6, 18.0, 1.0, 30.4, 19.2, 1.4, {"*": STEEL, "south": "pi_wh4_lbl_fill!"})]
    out += W.screen_unit(34.0, 17.0, 44.0, 23.4, 1.0, "pi_wh4_scr_out", hood=False)
    # The sight flow glass on the OUT line, under the flange.
    out += round_z(8.0, 4.0, 1.6, 1.0, 2.2, STEEL) + round_z(8.0, 4.0, 1.1, 2.2, 2.6, "pi_wh4_sight_water")
    return W._in_wall(out, hole=(17.0, 3.0, 31.0, 17.0, -5.0))


# ================================================================================================ the game's pieces
# What tools/warehouse_data.py writes for part W4: the four pieces as their models (what moves, and the drums, pallets,
# levels and pages, the game draws: client/warehouse/FillerRenderer, EmptierRenderer, LiquidsPanelRenderer), the
# hidden parts it draws them with, the drums' rack loads, the drum items, and the numbers the Java works by.
PIECES_W4 = {
    "warehouse_drum_filler": ("Drum Filling Station", (3, 3, 2)),
    "warehouse_drum_emptier": ("Drum Emptying Station", (3, 3, 2)),
    "warehouse_liquids_in": ("Liquids In Panel", (3, 2, 1)),
    "warehouse_liquids_out": ("Liquids Out Panel", (3, 2, 1)),
}
DRUM_KINDS = ("steel", "red", "poly", "dewar")
# The drum items (empty drums: crafted, loaded into a Drum Filling Station; a full one dropped keeps its liquid).
DRUM_ITEMS = {"steel": ("steel_drum", "Steel Drum"), "red": ("flammables_drum", "Flammables Drum"),
              "poly": ("hdpe_drum", "HDPE Drum"), "dewar": ("liquid_dewar", "Liquid Dewar")}
# The level gauges' and the sight flow glass's liquid: a white column the game tints with the liquid's colour.
TINT = "pi_wh4_tint"
# The pages, digits and level glasses the game draws live: dark (or empty) in the models.
W4_DARK = {"pi_wh4_hmi_fill!": "pi_wh_scr_off!", "pi_wh4_hmi_empty!": "pi_wh_scr_off!", "pi_wh4_scr_in!": "pi_wh_scr_off!",
           "pi_wh4_scr_out!": "pi_wh_scr_off!", "pi_wh4_indicator!": "pi_wh4_indicator_off!", "pi_wh4_sight!": "pi_wh4_sight_empty!",
           "pi_wh4_sight_water": "pi_wh4_sight_empty"}


def piece_boxes_w4(name):
    if name == "warehouse_drum_filler":
        return drum_filler(stock=0, pallets=0)
    if name == "warehouse_drum_emptier":
        return drum_emptier()
    if name == "warehouse_liquids_in":
        return liquids_in_panel()
    if name == "warehouse_liquids_out":
        return liquids_out_panel(bucket=False)
    raise KeyError(name)


def grip_height(kind):
    """How high over the floor a drum's vacuum cups land (its lid; a dewar's guard ring's bars)."""
    return DEWAR_H + 2.0 if kind == "dewar" else DRUM_H


def level_column():
    """A level gauge's liquid: a unit column (16 high; the game scales it to the level), its face +z."""
    return [box(0.0, 0.0, 0.0, 1.2, 16.0, 0.05, {"south": TINT + "!", "north": TINT + "!"})]


def sight_disc():
    """The sight flow glass's liquid, seen through its window (its face +z, its middle at (0, 0))."""
    return round_z(0.0, 0.0, 1.0, 0.0, 0.05, TINT)


def unit_cable():
    """A cable, 16 high from its foot (the game stretches it)."""
    return [box(-0.15, 0.0, -0.15, 0.15, 16.0, 0.15, "pi_wh_chain")]


def parts_w4():
    """The hidden part models (warehouse_part) the game draws part W4's moving things with."""
    out = {
        "lq_bridge": gantry_bridge, "lq_trolley": head_trolley, "lq_mast": head_mast, "lq_lance": head_lance,
        "lq_jib": lambda: jib(balancer=False), "lq_balancer": jib_balancer, "lq_pump": lambda: drum_pump(1.0), "lq_cable": unit_cable,
        "lq_level": level_column, "lq_disc": sight_disc,
        "lq_reel_lamp": lambda: round_x(0.0, 0.0, 2.45, 0.0, 1.25, "pi_wh_lamp_green"),
    }
    for kind in DRUM_KINDS:
        out[f"drum_{kind}"] = (lambda k=kind: drum(k, 0.0, 0.0, 0.0))
    return out


def drum_load_boxes(kind, n, y=0.0):
    """A pallet with n drums of a kind on it standing at y (the floor, or a rack place's beams)."""
    return shifted(W.pallet() + drum_load(kind, n, W.PALLET_H), 0.0, y, 0.0)


def layout_w4():
    """assets/jugcraft/warehouse.json's part W4 sections (piece pixels; the pieces' fronts to +z)."""
    sight = lambda cx, cz, y0, y1: {"x": cx - 0.6, "y0": y0 + 2.4, "y1": y1 - 2.4, "z": cz + TANK_R + 1.0}
    return {
        "drums": {"kinds": list(DRUM_KINDS), "spots": [list(p) for p in SPOTS], "height": {k: (DEWAR_H if k == "dewar" else DRUM_H) for k in DRUM_KINDS},
                  "grip": {k: grip_height(k) for k in DRUM_KINDS}, "bung": {k: ([-1.0, 0.0] if k == "dewar" else [-1.4, 0.0]) for k in DRUM_KINDS},
                  "radius": DRUM_R},
        "drum_filler": {"mag_y": MAG_Y, "mag_spots": [list(p) for p in MAG_SPOTS], "fill_deck": FILL_DECK, "bay": [16.0, 16.0],
                        "pallets": {"x": 16.0, "z": 0.0, "y0": 1.3, "step": W.PALLET_H + 0.02, "shown": 4},
                        "rail_y": RAIL_Y, "head_y": HEAD_Y, "drop_min": DROP_MIN, "drop_max": DROP_MAX, "lance": LANCE, "park": list(PARK),
                        "hmi": {"x0": 34.6, "x1": 44.6, "y0": 19.4, "y1": 27.0, "z": 31.8},
                        "indicator": {"x0": 35.0, "x1": 41.0, "y0": 12.4, "y1": 16.6, "z": 30.6},
                        "sight": sight(40.0, 8.0, 4.0, 36.0), "stack": [44.6, 20.0, 31.6, 1.3], "reel_lamp": [31.95, 22.0, 24.0]},
        "drum_emptier": {"deck": EMPTY_DECK, "bay": [16.0, 16.0], "jib": list(JIB), "arm": JIB_ARM, "balancer_drop": 2.6,
                         "pump_top": 9.6, "pump_lift": 10.0, "park": [-35.0, 12.0],
                         "hmi": {"x0": 34.8, "x1": 44.8, "y0": 16.0, "y1": 23.6, "z": 31.8},
                         "sight": sight(40.0, 8.0, 4.0, 36.0), "stack": [45.0, 22.0, 27.6, 1.3]},
        "liquids_in": {"screen": {"x0": 19.4, "x1": 28.6, "y0": 20.2, "y1": 25.0, "z": W.WALL_FACE + 2.8},
                       "inlets": [[8.0, 8.0], [40.0, 8.0], [8.0, 20.0], [40.0, 20.0]], "lamp_dx": 4.3, "lamp_z": W.WALL_FACE + 1.35},
        "liquids_out": {"screen": {"x0": 34.0, "x1": 44.0, "y0": 17.0, "y1": 23.4, "z": W.WALL_FACE + 2.8},
                        "out": [8.0, 10.0], "lamp_dx": 4.3, "lamp_z": W.WALL_FACE + 1.35, "disc": [8.0, 4.0, W.WALL_FACE + 2.62]},
    }


def _tint():
    return Image.new("RGBA", (16, 16), (236, 240, 244, 255))


def _indicator_off():
    img = Image.new("RGBA", (128, 64), (22, 26, 24, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 8, 121, 44], fill=(14, 24, 18, 255))
    W._t(d, (64, 54), "NET   GROSS", 9, (120, 130, 126), bold=False)
    return img.resize((128, 128))


def _sight_empty():
    """A level gauge's glass, empty (the game draws the liquid in it): the glass, the scale's ticks."""
    img = Image.new("RGBA", (32, 128), (196, 206, 212, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 0, 23, 127], fill=(150, 166, 176, 255))
    d.line([(10, 0), (10, 127)], fill=(226, 236, 242, 255), width=2)
    for y in range(0, 128, 16):
        d.line([(0, y), (6, y)], fill=(40, 40, 44, 255), width=2)
    return img.resize((128, 128))


def game_makers():
    """The textures the game's models need besides the mockups' (w4_makers)."""
    return {TINT: _tint, "pi_wh4_indicator_off": _indicator_off, "pi_wh4_sight_empty": _sight_empty}


GAME_SHARP = {TINT, "pi_wh4_indicator_off", "pi_wh4_sight_empty"}
