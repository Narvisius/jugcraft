"""The control room's ventilation plant (round 26, #11; Justin: "Control Room HVAC Equipment Room", all four kinds of
machine, "Switch to emergency mode", "Real ducts with dampers"): its machines as working equipment drawn from their
own parts, the way tools/control_boards.py draws the motor control centres (operations replayed by
client/ControlBoards.java with the equipment sheet's materials), every door, latch, hinge, gauge, valve and nameplate
real geometry:

- the control room emergency filtration units (CREFS) A and B: a stainless housing on a skid, its stages behind their
  own access doors (moisture separator, electric heater, prefilter and HEPA, the charcoal adsorber with its fill ports,
  test canisters and water spray header, the second HEPA), the differential pressure gauges' board, and the booster fan
  (centrifugal, direct drive) with its local control station;
- the control room air handling units A and B: mixing box with its outside and return air dampers and their
  actuators, filters, the chilled water cooling coil (its supply and return piped out of the side through valves, the
  condensate trap), the supply fan's section and the discharge out of the end;
- the control room chillers A and B: evaporator (insulated) and condenser shells, water boxes and nozzles, the screw
  compressor and its motor, the unit's control panel and its display, relief valves and gauges;
- the chilled water pumps A and B: end-suction pumps on a base on a housekeeping pad, coupling guard, motor, the
  suction and discharge valves and gauges.

Every unit is labelled by train (the anchor's variant: "v0" A, "v1" B). Frame as the boards': x to the right
(blocks), y up, z away from whoever placed it. What lights and moves is the game's (live: the runtime lamps, gauge
needles and displays), as the motor control centre's."""
import math

from PIL import Image, ImageDraw

import control_boards as C
from control_boards import sw, pic, named, _r, LENS

GASKET = sw("black")
DARK = sw("dgrey")
MID = sw("mgrey")
# The train colours (as the relay room's SSPS bands): A orange, B green.
TRAINS = ("A", "B")


# ------------------------------------------------------------------------------------------------ lettered cells
# Each unit's title plate (two to a cell: train A in the top half, B in the bottom, three lines each), stage and part
# labels (16 to a cell), and the placards and gauge faces (four quarters).
TITLE_CELLS = {
    "hvac_titles_efu": [("CONTROL ROOM EMERGENCY", "FILTRATION UNIT A", "CRF-1A   2000 CFM"), ("CONTROL ROOM EMERGENCY", "FILTRATION UNIT B", "CRF-1B   2000 CFM")],
    "hvac_titles_ahu": [("CONTROL ROOM", "AIR HANDLING UNIT A", "AHU-1A   8000 CFM"), ("CONTROL ROOM", "AIR HANDLING UNIT B", "AHU-1B   8000 CFM")],
    "hvac_titles_chiller": [("CONTROL ROOM", "WATER CHILLER A", "CH-1A   60 TONS"), ("CONTROL ROOM", "WATER CHILLER B", "CH-1B   60 TONS")],
    "hvac_titles_pump": [("CHILLED WATER", "PUMP A", "CHP-1A   150 GPM"), ("CHILLED WATER", "PUMP B", "CHP-1B   150 GPM")],
}
TITLE_COLOURS = [((226, 120, 28), (20, 20, 22)), ((36, 128, 76), (244, 244, 240))]
C.NAMEPLATES.update({
    "nameplates5": ["MOIST SEP", "HEATER", "PREFILTER", "HEPA", "CHARCOAL", "POST HEPA", "BOOSTER FAN", "TEST PORT",
                    "MIXING BOX", "FILTERS", "COOL COIL", "ACCESS", "SUPPLY FAN", "DISCHARGE", "OA DAMPER", "RA DAMPER"],
    "nameplates6": ["EVAPORATOR", "CONDENSER", "COMPRESSOR", "CHW SUPPLY", "CHW RETURN", "SW IN", "SW OUT", "OIL SEP",
                    "SUCTION", "DISCH", "PREFLTR DP", "HEPA DP", "CHAR DP", "POST DP", "FILTER DP", "SPRAY"],
})
C.PLATE_COLOURS.update({("nameplates6", 15): "red"})
PLACARDS = "hvac_placards"
# The placards' cell: DANGER 480 V (0), CAUTION ROTATING EQUIPMENT / STARTS AUTOMATICALLY (1), a differential
# pressure gauge's face (2: inches of water, no needle: the game's), a pressure and temperature gauge's (3).
GAUGE_DP, GAUGE_P = 2, 3


def _font(size):
    from PIL import ImageFont
    return ImageFont.truetype(C._LETTER_FONT, size)


def _fit(d, cx, cy, w, legend, ink, size):
    while size > 6 and d.textlength(legend, font=_font(size)) > w:
        size -= 1
    d.text((cx, cy), legend, font=_font(size), fill=ink, anchor="mm")


def _title_cell(cell):
    def make(seed, size=C.LETTERED):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
        d = ImageDraw.Draw(img)
        h = size // 2
        for k, lines in enumerate(TITLE_CELLS[cell]):
            bg, ink = TITLE_COLOURS[k]
            y0 = k * h
            d.rectangle([0, y0, size - 1, y0 + h - 1], fill=(236, 236, 230, 255))
            d.rectangle([0, y0, size - 1, y0 + 13], fill=bg + (255,))
            d.rectangle([0, y0, size - 1, y0 + h - 1], outline=(30, 30, 32, 255))
            _fit(d, size / 2, y0 + 7, size - 6, lines[0], ink + (255,), 11)
            _fit(d, size / 2, y0 + 29, size - 6, lines[1], (20, 20, 22, 255), 17)
            _fit(d, size / 2, y0 + 50, size - 8, lines[2], (20, 20, 22, 255), 12)
        return img
    return make


def _placards(seed, size=C.LETTERED):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    h = size // 2
    ink, white = (20, 20, 22, 255), (246, 244, 238, 255)
    # DANGER 480 VOLTS.
    d.rectangle([0, 0, h - 1, h - 1], fill=white, outline=ink)
    d.rectangle([2, 2, h - 3, 20], fill=(188, 32, 28, 255))
    _fit(d, h / 2, 11, h - 6, "DANGER", white, 15)
    _fit(d, h / 2, 34, h - 6, "480 VOLTS", ink, 14)
    _fit(d, h / 2, 51, h - 6, "KEEP OUT", ink, 11)
    # CAUTION: starts automatically.
    d.rectangle([h, 0, size - 1, h - 1], fill=white, outline=ink)
    d.rectangle([h + 2, 2, size - 3, 20], fill=(236, 196, 36, 255))
    _fit(d, h + h / 2, 11, h - 6, "CAUTION", ink, 15)
    _fit(d, h + h / 2, 32, h - 6, "STARTS", ink, 12)
    _fit(d, h + h / 2, 48, h - 8, "AUTOMATICALLY", ink, 11)
    # The gauges' faces: white dials, their scales (270 degrees, from the lower left round to the lower right), the
    # major figures inside the scale, the units under the hub; no needles (the game's).
    for q, unit, figures in ((2, "IN W.C.", ("0", "2", "4")), (3, "PSI", ("0", "50", "100"))):
        x0, y0 = (q % 2) * h, h
        cx, cy, r = x0 + h / 2, y0 + h / 2, h / 2 - 2
        d.rectangle([x0, y0, x0 + h - 1, y0 + h - 1], fill=(30, 30, 32, 255))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(242, 242, 236, 255), outline=(150, 150, 146, 255), width=2)
        for k in range(21):
            a = math.radians(225 - 13.5 * k)
            r0 = r - (7 if k % 10 == 0 else 5 if k % 5 == 0 else 3.5)
            d.line([(cx + r0 * math.cos(a), cy - r0 * math.sin(a)), (cx + (r - 2.5) * math.cos(a), cy - (r - 2.5) * math.sin(a))],
                   fill=ink, width=2 if k % 5 == 0 else 1)
        for k, fig in enumerate(figures):
            a = math.radians(225 - 135 * k)
            d.text((cx + (r - 13) * math.cos(a), cy - (r - 13) * math.sin(a)), fig, font=_font(9), fill=ink, anchor="mm")
        d.text((cx, cy + r - 7), unit, font=_font(7), fill=(150, 30, 26, 255), anchor="mm")
        d.ellipse([cx - 2.5, cy - 2.5, cx + 2.5, cy + 2.5], fill=(30, 30, 32, 255))
    return img


def title_plate(kind, train):
    """Train A's or B's title plate of a unit kind (its half of the kind's cell)."""
    return [f"hvac_titles_{kind}", 0.004, train * 0.5 + 0.004, 0.996, train * 0.5 + 0.496]


def placard(q):
    return C.quarter(PLACARDS, q)


C.MAKERS.update({cell: _title_cell(cell) for cell in TITLE_CELLS})
C.MAKERS.update({"nameplates5": C._nameplate_cell("nameplates5"), "nameplates6": C._nameplate_cell("nameplates6"), PLACARDS: _placards})
C.LETTERED_CELLS |= set(TITLE_CELLS) | {"nameplates5", "nameplates6", PLACARDS}
C.COLOURS.update({cell: 48 for cell in TITLE_CELLS})
C.COLOURS.update({"nameplates5": 32, "nameplates6": 32, PLACARDS: 48})
MATERIALS = list(TITLE_CELLS) + ["nameplates5", "nameplates6", PLACARDS]


# ------------------------------------------------------------------------------------------------ building blocks

def side_box(b, side, xf, d, y0, y1, z0, z1, face, edge=GASKET, lst=None):
    """A part standing d out of a side (side -1: the left face at x = xf, out toward -x; +1 the right), its outer face
    face and its edges edge."""
    if side < 0:
        b.box(xf - d, y0, z0, xf, y1, z1, [edge, edge, face, None, edge, edge], lst)
    else:
        b.box(xf, y0, z0, xf + d, y1, z1, [edge, edge, None, face, edge, edge], lst)


def end_box(b, end, zf, d, x0, x1, y0, y1, face, edge=GASKET, lst=None):
    """A part standing d out of an end (end -1: the front face at z = zf, out toward -z; +1 the back)."""
    if end < 0:
        b.box(x0, y0, zf - d, x1, y1, zf, [face, None, edge, edge, edge, edge], lst)
    else:
        b.box(x0, y0, zf, x1, y1, zf + d, [None, face, edge, edge, edge, edge], lst)


def side_plate(b, side, xf, z, y, w, h, mat, d=0.008):
    side_box(b, side, xf, d, y - h / 2, y + h / 2, z - w / 2, z + w / 2, mat, sw("white"))


def door(b, side, xf, z0, z1, y0, y1, mat, label=None, window=False, handles=2, hinge_far=True):
    """An access door on a side: the door (its edges a black gasket), hinges along one edge, cam latches along the
    other, a wired-glass viewport if window, its stage's label plate over it."""
    d = 0.016
    side_box(b, side, xf, d, y0, y1, z0, z1, mat)
    xo = xf + side * d
    zh, zl = (z1 - 0.03, z0 + 0.06) if hinge_far else (z0 + 0.03, z1 - 0.06)
    for hy in (y0 + 0.18, y1 - 0.18):
        b.cyl((xo + side * 0.018, hy - 0.07, zh), (xo + side * 0.018, hy + 0.07, zh), 0.016, DARK, MID, 8)
    span = y1 - y0
    for k in range(handles):
        hy = y0 + span * (k + 1) / (handles + 1)
        # The latch: its round boss on the door and the T-handle out of it.
        b.cyl((xo, hy, zl), (xo + side * 0.025, hy, zl), 0.02, DARK, DARK, 8)
        side_box(b, side, xo + side * 0.025, 0.02, hy - 0.05, hy + 0.05, zl - 0.012, zl + 0.012, sw("black"), sw("black"))
    if window:
        wz0, wz1 = (z0 + z1) / 2 - 0.12, (z0 + z1) / 2 + 0.12
        wy0, wy1 = y1 - 0.42, y1 - 0.18
        side_box(b, side, xo, 0.008, wy0, wy1, wz0, wz1, pic("glass"), sw("black"))
    if label:
        side_plate(b, side, xf, (z0 + z1) / 2, y1 + 0.075, min(0.5, z1 - z0 - 0.08), 0.1, named(label))


def side_quad(b, side, x, z, y, hw, hh, mat, lst=None):
    """A quad on a side (seen from outside: -x for the left, +x for the right), centred at (z, y), hw by hh half-sizes;
    upright as the boxes' own side faces are."""
    if side < 0:
        b.quad((x, y + hh, z + hw), (x, y - hh, z + hw), (x, y - hh, z - hw), (x, y + hh, z - hw), mat, lst)
    else:
        b.quad((x, y + hh, z - hw), (x, y - hh, z - hw), (x, y - hh, z + hw), (x, y + hh, z + hw), mat, lst)


def gauge(b, side, xf, z, y, r=0.085, kind=GAUGE_DP, live=None, name=None):
    """A round gauge on a side: its black case standing out of the surface at xf, the white dial behind its glass (a
    face inscribed in the case's front), the needle the game's (live: its centre, length and the dial's right, the
    sweep 225 degrees round to -45 from empty to full)."""
    xo = xf + side * 0.05
    b.cyl((xf, y, z), (xo, y, z), r, sw("black"), sw("black"), 12)
    s = r * 0.7
    xq = xo + side * 0.004
    side_quad(b, side, xq, z, y, s, s, placard(kind))
    if live is not None:
        live.append({"name": name, "c": _r(xq + side * 0.003, y, z), "len": _r(s * 0.8)[0], "u": [0, 0, side], "side": side})
    return xo


# The colour a round part's end is drawn in where its material would repeat round the cap (client discs map the
# material's middle onto every wedge): a plain swatch near the material's own.
CAPS = {"paint_grey": "mgrey", "paint_blue": "blue", "paint_green": "green", "paint_yellow": "yellow", "paint_white": "white",
        "stainless": "enamel", "stainless_dark": "mgrey", "rubber": "black", "insulation": "white", "flange": "mgrey",
        "pipe_red": "red", "pipe_green": "green"}


def pipe(b, a, c, r, mat, cap=None, sides=10, both=None):
    """A round part from a to c; both: capped at both ends (drawn as two halves from the middle out, each capped at
    its own end), as anything bigger than a small pipe is, so it never shows open from either end."""
    if cap is None or cap == mat:
        cap = sw(CAPS[mat]) if isinstance(mat, str) and mat in CAPS and r > 0.06 else mat
    if both is None:
        both = r > 0.06
    if both:
        m = tuple((p + q) / 2 for p, q in zip(a, c))
        b.cyl(m, a, r, mat, cap, sides)
        b.cyl(m, c, r, mat, cap, sides)
    else:
        b.cyl(a, c, r, mat, cap, sides)


def flange(b, at, axis, r, t=0.035):
    """A pipe flange: a disc t thick at at, across axis ("x", "y" or "z")."""
    x, y, z = at
    d = {"x": (t / 2, 0, 0), "y": (0, t / 2, 0), "z": (0, 0, t / 2)}[axis]
    b.cyl((x - d[0], y - d[1], z - d[2]), (x + d[0], y + d[1], z + d[2]), r * 1.5, "flange", sw("mgrey"), 12)


def handwheel(b, at, axis, r=0.09):
    """A valve's yoke and handwheel at the end of its stem (pointing along axis)."""
    x, y, z = at
    e = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[axis]
    stem_end = (x + e[0] * 0.12, y + e[1] * 0.12, z + e[2] * 0.12)
    b.cyl(at, stem_end, 0.012, "stainless", "stainless", 6)
    wheel0 = stem_end
    wheel1 = (stem_end[0] + e[0] * 0.02, stem_end[1] + e[1] * 0.02, stem_end[2] + e[2] * 0.02)
    b.cyl(wheel0, wheel1, r, sw("red"), sw("red"), 12)


def base_rails(b, x0, x1, z0, z1, h=0.22, mat=None):
    """A unit's skid: two channel rails along z under its sides and cross members between them."""
    mat = mat or DARK
    for rx in (x0, x1 - 0.2):
        b.box(rx, 0, z0, rx + 0.2, h, z1, mat)
    for cz in (z0 + 0.1, (z0 + z1) / 2, z1 - 0.25):
        b.box(x0 + 0.2, 0.04, cz, x1 - 0.2, h - 0.04, cz + 0.15, mat)


def station(b, side, xf, z, y, live, name="station"):
    """A local control station on a side: its grey box, the RUNNING (red) and STOPPED (green) lamps over the HAND-OFF-AUTO
    selector (at AUTO), its plate; the lamps the game's (live: red then green, the red to the viewer's right as the
    plant's switches have it)."""
    side_box(b, side, xf, 0.12, y - 0.2, y + 0.2, z - 0.14, z + 0.14, MID, DARK)
    xo = xf + side * 0.12
    lamps = []
    for dz in (side * 0.06, -side * 0.06):
        b.cyl((xo, y + 0.1, z + dz), (xo + side * 0.012, y + 0.1, z + dz), 0.03, DARK, DARK, 8)
        lamps.append(_r(xo + side * 0.016, y + 0.1, z + dz))
    b.cyl((xo, y - 0.05, z), (xo + side * 0.02, y - 0.05, z), 0.04, sw("black"), sw("dgrey"), 10)
    side_box(b, side, xo + side * 0.02, 0.025, y - 0.065, y - 0.035, z - 0.012, z + 0.035, sw("white"), sw("white"))
    side_plate(b, side, xo, z, y - 0.155, 0.22, 0.05, named("AUTO"), 0.006)
    live.setdefault("stations", []).append({"name": name, "lamps": lamps, "r": 0.024, "side": side})


def _mats(live):
    """The materials the client draws a unit's live parts in (client/NuclearRenderer.java hvacLive): the needles, the
    station lamps' lenses lit and unlit, the chiller display's glass."""
    live["mats"] = {"needle": sw("black"), "red": C.LENS["red"], "green": C.LENS["green"], "off_red": C.LENS["off_red"],
                    "off_green": C.LENS["off_green"], "glass": sw("black")}
    return live


def trained(b, f):
    """Draws f(train) into each train's state ("v0" A, "v1" B): the parts that differ by train (their names)."""
    for t in range(2):
        s = b.state(f"v{t}")
        keep = b.still
        b.still = s["still"]
        try:
            f(t)
        finally:
            b.still = keep


def collar(b, x0, x1, y0, y1, z0, z1, mat="stainless", lip=0.03):
    """A rectangular duct collar along z (x0..x1, y0..y1) from z0 to z1, its angle flange at z0 (lip proud all round)."""
    b.box(x0, y0, z0 + 0.04, x1, y1, z1, [None, None, mat, mat, mat, mat])
    b.box(x0 - lip, y0 - lip, z0, x1 + lip, y1 + lip, z0 + 0.04, "stainless_dark")


def collar_up(b, x0, x1, z0, z1, y0, y1, mat="stainless", lip=0.03):
    """A collar up out of a top (x0..x1, z0..z1) from y0 to y1, its flange at the top (where the room's riser meets it)."""
    b.box(x0, y0, z0, x1, y1 - 0.04, z1, [mat, mat, mat, mat, None, None])
    b.box(x0 - lip, y1 - 0.04, z0 - lip, x1 + lip, y1, z1 + lip, "stainless_dark")


# Where the room's ducts meet a unit (tools/hvac_room.py D0 to D1): a duct is 13 px square, centred in its block.
DUCT_W, DUCT_H = 13 / 16, 13 / 16


def duct_face(cx, cy):
    """The duct's opening (x0, x1, y0, y1) about a block's middle (cx, cy), along z."""
    return cx - DUCT_W / 2, cx + DUCT_W / 2, cy - DUCT_H / 2, cy + DUCT_H / 2


def duct_top(cx, cz):
    """The duct's opening (x0, x1, z0, z1) about a block's middle, up out of a top (a riser, 13 px square)."""
    return cx - DUCT_W / 2, cx + DUCT_W / 2, cz - DUCT_H / 2, cz + DUCT_H / 2


# ------------------------------------------------------------------------------------------------ the filtration unit

EFU_X = (0.35, 2.65)
EFU_Y = (0.22, 2.42)
EFU_Z = (0.35, 5.0)
EFU_STAGES = [(0.35, 0.9, "MOIST SEP"), (0.9, 1.5, "HEATER"), (1.5, 2.05, "PREFILTER"), (2.05, 2.8, "HEPA"), (2.8, 4.2, "CHARCOAL"),
              (4.2, 5.0, "POST HEPA")]
EFU_GAUGES = {"PREFILTER": "PREFLTR DP", "HEPA": "HEPA DP", "CHARCOAL": "CHAR DP", "POST HEPA": "POST DP"}
EFU_DOOR_Y = (0.36, 1.62)
# The booster fan: its scroll about an axis along z, the outlet straight up (the room's riser over local block (1, 3, 5)).
EFU_FAN = (1.0625, 1.3, 0.875, 5.125, 5.875)   # axis x, y, radius, scroll z0, z1


def filtration_unit(seed=211):
    """A control room emergency filtration unit (CREFS), 3 wide, 7 deep, 3 tall: the outside air in through the collar
    on its front end (from the intake's emergency damper), through its stages along it, the booster fan at its far end
    blowing up out of its top (local block 1, 5) into the filtered air duct to the air handler."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": []}
    st, dk = "stainless", "stainless_dark"
    x0, x1 = EFU_X
    y0, y1 = EFU_Y
    z0, z1 = EFU_Z
    base_rails(b, 0.25, 2.75, 0.15, 6.9)
    # The housing: welded stainless, its stages' seams standing out round it.
    b.box(x0, y0, z0, x1, y1, z1, [st, st, st, st, st, None])
    b.core = len(b.still)
    for zs in [s[0] for s in EFU_STAGES[1:]]:
        b.box(x0 - 0.02, y0 + 0.01, zs - 0.03, x0, y1 - 0.01, zs + 0.03, dk)
        b.box(x1, y0 + 0.01, zs - 0.03, x1 + 0.02, y1 - 0.01, zs + 0.03, dk)
        b.box(x0 + 0.01, y1, zs - 0.03, x1 - 0.01, y1 + 0.02, zs + 0.03, dk)
    # The stages' access doors on the operating side (left), their names on them; over each filter its differential
    # pressure gauge (the needles the game's) and its plate.
    dy0, dy1 = EFU_DOOR_Y
    for za, zb, label in EFU_STAGES:
        if label == "HEATER":
            continue
        door(b, -1, x0, za + 0.06, zb - 0.06, dy0, dy1, dk, None, window=label in ("PREFILTER", "HEPA", "POST HEPA"),
             handles=3 if zb - za > 1 else 2)
        side_plate(b, -1, x0 - 0.016, (za + zb) / 2, dy1 - 0.1, min(0.44, zb - za - 0.2), 0.09, named(label), 0.006)
        if label in EFU_GAUGES:
            gz = (za + zb) / 2
            gauge(b, -1, x0, gz, 1.97, 0.11, GAUGE_DP, live["gauges"], EFU_GAUGES[label])
            side_plate(b, -1, x0, gz, 1.75, 0.34, 0.07, named(EFU_GAUGES[label]), 0.006)
            # The gauge's two sensing lines into the housing either side of the filter.
            for dz in (-0.06, 0.06):
                b.box(x0 - 0.03, 1.86, gz + dz - 0.008, x0 - 0.014, 1.93, gz + dz + 0.008, sw("mgrey"))
    # The heater's section: its name, the terminal box (480 V), the conduit down into the floor.
    za, zb, _ = EFU_STAGES[1]
    hz = (za + zb) / 2
    side_plate(b, -1, x0, hz, 2.2, 0.44, 0.09, named("HEATER"))
    side_box(b, -1, x0, 0.16, 0.9, 1.7, za + 0.08, zb - 0.08, "paint_grey", DARK)
    side_box(b, -1, x0 - 0.16, 0.004, 1.42, 1.64, za + 0.12, zb - 0.12, placard(0), sw("white"))
    for k in range(4):
        b.box(x0 - 0.17, 0.98 + k * 0.08, za + 0.14, x0 - 0.16, 1.02 + k * 0.08, zb - 0.14, sw("dgrey"))
    pipe(b, (x0 - 0.08, 0.0, hz), (x0 - 0.08, 0.9, hz), 0.045, "stainless")
    b.cyl((x0 - 0.08, 0.0, hz), (x0 - 0.08, 0.03, hz), 0.08, sw("red"), sw("red"), 8)
    # The charcoal stage: the fill ports on top, the water spray (deluge) header over it, the test canisters along its
    # right side.
    za, zb, _ = EFU_STAGES[4]
    for fx in (0.9, 2.1):
        pipe(b, (fx, y1, za + 0.45), (fx, y1 + 0.16, za + 0.45), 0.09, st)
        pipe(b, (fx, y1 + 0.16, za + 0.45), (fx, y1 + 0.22, za + 0.45), 0.115, dk)
    hz = za + 0.15
    pipe(b, (0.55, y1 + 0.32, hz), (2.78, y1 + 0.32, hz), 0.04, "pipe_red")
    for nx in (0.9, 1.5, 2.1):
        pipe(b, (nx, y1 + 0.02, hz), (nx, y1 + 0.28, hz), 0.025, "pipe_red")
    pipe(b, (2.78, y1 + 0.32, hz), (2.78, y1 + 0.32, za + 0.9), 0.04, "pipe_red")
    pipe(b, (2.78, y1 + 0.32, za + 0.9), (2.78, 0.0, za + 0.9), 0.04, "pipe_red")
    handwheel(b, (2.82, 1.3, za + 0.9), "x", 0.07)
    side_plate(b, 1, x1, za + 0.45, y1 - 0.14, 0.4, 0.09, named("SPRAY"))
    for k in range(4):
        cz = za + 0.2 + k * 0.3
        pipe(b, (x1, 0.6, cz), (x1 + 0.1, 0.6, cz), 0.05, st)
        pipe(b, (x1 + 0.1, 0.6, cz), (x1 + 0.13, 0.6, cz), 0.065, dk)
    side_plate(b, 1, x1, za + 0.65, 0.86, 0.44, 0.09, named("TEST PORT"))
    # The aerosol (DOP) test ports either side of each filter bank on the right, capped red.
    for tz in (1.6, 2.0, 2.15, 2.7, 4.3, 4.9):
        pipe(b, (x1, 1.95, tz), (x1 + 0.08, 1.95, tz), 0.03, st)
        pipe(b, (x1 + 0.08, 1.95, tz), (x1 + 0.11, 1.95, tz), 0.04, sw("red"))
    # The inlet: the collar out of the front end (the intake's damper and duct meet it at the block's face), its
    # flange; the title plate under it.
    ix0, ix1, iy0, iy1 = duct_face(1.5, 1.5)
    collar(b, ix0, ix1, iy0, iy1, 0.0, z0)
    trained(b, lambda t: end_box(b, -1, z0, 0.008, 0.75, 2.25, 0.3, 1.05, title_plate("efu", t), sw("white")))
    # The fan: the housing's outlet ring and a flexible connector into the fan's inlet bell, the scroll (axis along the
    # unit), the outlet straight up out of its top to the block's face, the direct-drive motor behind on its pedestal,
    # the inertia base and its springs.
    ax, ay, R, sz0, sz1 = EFU_FAN
    pipe(b, (ax, ay, z1), (ax, ay, z1 + 0.04), 0.46, "flange", "flange", 14)
    pipe(b, (ax, ay, z1 + 0.04), (ax, ay, sz0), 0.4, sw("black"), sw("black"), 14)
    b.box(0.3, 0.22, z1 + 0.1, 2.1, 0.36, 6.95, DARK)
    for sx in (0.4, 1.95):
        for spz in (5.2, 6.8):
            b.cyl((sx, 0.22, spz), (sx, 0.36, spz), 0.05, "paint_yellow", DARK, 8)
    pipe(b, (ax, ay, sz0), (ax, ay, sz1), R, "paint_grey", "paint_grey", 16)
    ox0, ox1, oz0, oz1 = duct_top(1.5, 5.5)
    b.box(ax, ay, oz0, ox1, 2.96, oz1, ["paint_grey", "paint_grey", "paint_grey", "paint_grey", None, None])
    b.box(ox0 - 0.03, 2.96, oz0 - 0.03, ox1 + 0.03, 3.0, oz1 + 0.03, "stainless_dark")
    for leg_z in (sz0 + 0.1, sz1 - 0.1):
        b.box(ax - 0.5, 0.36, leg_z - 0.04, ax + 0.5, ay - R * 0.7, leg_z + 0.04, DARK)
    pipe(b, (ax, ay, sz1), (ax, ay, 5.98), 0.06, "stainless", "stainless", 8)
    pipe(b, (ax, ay, 5.98), (ax, ay, 6.85), 0.3, "paint_blue", "paint_blue", 14)
    for fz in (6.1, 6.3, 6.5, 6.7):
        pipe(b, (ax, ay, fz), (ax, ay, fz + 0.03), 0.32, "paint_blue", "paint_blue", 14)
    b.box(ax - 0.24, 0.36, 6.05, ax + 0.24, ay - 0.27, 6.8, DARK)
    b.box(ax + 0.25, ay - 0.1, 6.25, ax + 0.45, ay + 0.15, 6.55, "paint_blue")
    pipe(b, (ax + 0.35, ay - 0.1, 6.4), (ax + 0.35, 0.36, 6.4), 0.035, DARK)
    # Its name and warning on the outlet's left face (toward the operating side).
    side_plate(b, -1, ax, 5.5, 2.82, 0.5, 0.09, named("BOOSTER FAN"))
    side_box(b, -1, ax, 0.004, 2.25, 2.65, 5.3, 5.7, placard(1), sw("white"))
    # The fan's local control station on its stand by the motor.
    b.box(0.08, 0.03, 6.55, 0.16, 1.22, 6.63, DARK)
    b.box(0.0, 0.0, 6.45, 0.24, 0.03, 6.73, DARK)
    station(b, -1, 0.16, 6.59, 1.42, live, "efu")
    b.live = _mats(live)
    return b


# ------------------------------------------------------------------------------------------------ the air handler

AHU_X = (0.3, 2.3)
AHU_Y = (0.25, 2.55)
AHU_Z = (0.1, 6.85)
AHU_SECTIONS = [(0.1, 1.3, "MIXING BOX"), (1.3, 2.6, "FILTERS"), (2.6, 4.9, "COOL COIL"), (4.9, 6.4, "SUPPLY FAN"), (6.4, 6.85, "DISCHARGE")]
# The chilled water: its supply up out of the top at local (2.5, 3.5), its return at (2.5, 4.5) (the room's pipes meet
# them, tools/hvac_room.py), insulated.
AHU_CHW = ((2.5, 3.5, 0.75), (2.5, 4.5, 2.05))     # x, z of the riser; the height its stub leaves the coil
CHW_R = 0.09


def valve_lever(b, at, side, along="z"):
    """A butterfly valve's notched quadrant and lever, on a vertical pipe at at, the lever out toward side (x)."""
    x, y, z = at
    b.box(x - 0.11, y - 0.05, z - 0.11, x + 0.11, y + 0.05, z + 0.11, "paint_blue")
    xa, xb = sorted((x + side * 0.11, x + side * 0.16))
    b.box(xa, y - 0.02, z - 0.04, xb, y + 0.02, z + 0.04, DARK)
    xa, xb = sorted((x + side * 0.16, x + side * 0.42))
    b.box(xa, y + 0.0, z - 0.015, xb, y + 0.03, z + 0.015, sw("red"))


def air_handling_unit(seed=223):
    """A control room air handling unit, 3 wide, 7 deep, 3 tall: the control room's return air in through the damper on
    its front end (the duct up out of the floor meets it at local block (1, 1, 0)), the outside air (or, in emergency,
    the filtration unit's filtered air) down into the mixing box through the damper on its top (local block (1, 3, 0));
    the filters, the chilled water coil (its pipes up out of the top on the right), the supply fan; out of its far end
    (local block (1, 1, 6)) into the supply duct down to the control room."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": []}
    p = "paint_grey"
    x0, x1 = AHU_X
    y0, y1 = AHU_Y
    z0, z1 = AHU_Z
    base_rails(b, 0.2, 2.4, 0.1, 6.9, 0.25)
    b.box(x0, y0, z0, x1, y1, z1, [p, p, p, p, p, None])
    b.core = len(b.still)
    for zs in [s[0] for s in AHU_SECTIONS[1:]]:
        b.box(x0 - 0.02, y0 + 0.01, zs - 0.03, x0, y1 - 0.01, zs + 0.03, DARK)
        b.box(x1, y0 + 0.01, zs - 0.03, x1 + 0.02, y1 - 0.01, zs + 0.03, DARK)
        b.box(x0 + 0.01, y1, zs - 0.03, x1 - 0.01, y1 + 0.02, zs + 0.03, DARK)
    # The sections' names along the top of the operating side; doors on the filters, the coil and the fan.
    for za, zb, label in AHU_SECTIONS:
        if label == "DISCHARGE":
            continue
        side_plate(b, -1, x0, (za + zb) / 2, y1 - 0.12, min(0.5, zb - za - 0.16), 0.1, named(label))
        if label in ("FILTERS", "COOL COIL", "SUPPLY FAN"):
            door(b, -1, x0, za + 0.07, zb - 0.07, y0 + 0.12, 1.98, "paint_grey", None, window=label == "SUPPLY FAN",
                 handles=3 if zb - za > 1.6 else 2)
    # The title plate on the mixing box's side.
    trained(b, lambda t: side_box(b, -1, x0, 0.008, 0.9, 1.4, 0.2, 1.2, title_plate("ahu", t), sw("white")))
    # The filters' differential pressure gauge over their door (the needle the game's).
    za, zb, _ = AHU_SECTIONS[1]
    gauge(b, -1, x0, (za + zb) / 2 - 0.25, 2.16, 0.1, GAUGE_DP, live["gauges"], "FILTER DP")
    side_plate(b, -1, x0, (za + zb) / 2 + 0.2, 2.16, 0.32, 0.07, named("FILTER DP"), 0.006)
    # The return air: its damper on the front end (blades in the frame, the locking quadrant), the collar out to the
    # block's face where the room's duct meets it.
    rx0, rx1, ry0, ry1 = duct_face(1.5, 1.5)
    b.box(0.9, 0.95, z0 - 0.05, 2.1, 2.05, z0, [DARK, None, DARK, DARK, DARK, DARK])
    collar(b, rx0, rx1, ry0, ry1, 0.0, z0 - 0.05, "paint_grey")
    b.box(2.1, 1.35, z0 - 0.04, 2.24, 1.65, z0, DARK)
    b.box(2.16, 1.45, z0 - 0.07, 2.2, 1.6, z0 - 0.04, sw("red"))
    end_box(b, -1, z0, 0.008, 1.25, 1.75, 0.72, 0.84, named("RA DAMPER"), sw("white"))
    # The outside air: the damper on the mixing box's top, its frame and blades, the collar up to the block's top where
    # the room's riser meets it, the locking quadrant on its side.
    ox0, ox1, oz0, oz1 = duct_top(1.5, 0.5)
    b.box(0.95, y1, 0.15, 2.05, y1 + 0.12, 0.95, [DARK, DARK, DARK, DARK, None, None])
    b.box(0.97, y1 + 0.12, 0.17, 2.03, y1 + 0.14, 0.93, [None, None, None, None, "louvre", None])
    collar_up(b, ox0, ox1, oz0, oz1, y1 + 0.14, 3.0, "paint_grey")
    b.box(2.05, y1 + 0.02, 0.45, 2.18, y1 + 0.12, 0.65, DARK)
    b.box(2.12, y1 + 0.12, 0.5, 2.16, y1 + 0.26, 0.6, sw("red"))
    side_plate(b, -1, 0.95, 0.55, y1 + 0.06, 0.5, 0.07, named("OA DAMPER"), 0.006)
    # The coil's chilled water: each stub out of the right side, up as a riser out of the top; the supply's butterfly
    # valve, the return's two-way control valve (its actuator) and butterfly valve; their gauges (needles the game's),
    # thermometers; the condensate trap down to the floor drain.
    for k, (px, pz, sy) in enumerate(AHU_CHW):
        b.box(x1, sy - 0.15, pz - 0.15, x1 + 0.05, sy + 0.15, pz + 0.15, [DARK, DARK, None, DARK, DARK, DARK])
        pipe(b, (x1 + 0.05, sy, pz), (px - CHW_R, sy, pz), CHW_R, "insulation")
        pipe(b, (px, sy - CHW_R, pz), (px, 3.0, pz), CHW_R, "insulation")
        flange(b, (px, 2.94, pz), "y", CHW_R * 0.9, 0.06)
    sx, sz, sy = AHU_CHW[0]
    valve_lever(b, (sx, 1.45, sz), 1)
    gauge(b, 1, sx + CHW_R, sz, 1.9, 0.075, GAUGE_P, live["gauges"], "CHWS P")
    side_plate(b, 1, sx + CHW_R + 0.01, sz, 2.42, 0.28, 0.06, named("CHW SUPPLY"), 0.006)
    rx, rz, ry = AHU_CHW[1]
    b.box(rx - 0.12, 2.25, rz - 0.12, rx + 0.12, 2.42, rz + 0.12, "paint_green")
    b.box(rx - 0.1, 2.42, rz - 0.1, rx + 0.1, 2.72, rz + 0.1, "paint_blue")
    b.box(rx + 0.1, 2.5, rz - 0.04, rx + 0.14, 2.62, rz + 0.04, sw("black"))
    valve_lever(b, (rx, 2.82, rz), 1)
    side_plate(b, 1, rx + CHW_R + 0.01, rz, 1.7, 0.28, 0.06, named("CHW RETURN"), 0.006)
    gauge(b, 1, rx + CHW_R, rz, 1.32, 0.075, GAUGE_P, live["gauges"], "CHWR P")
    for (px, pz, _), ty in zip(AHU_CHW, (1.15, 2.05)):
        pipe(b, (px, ty, pz - CHW_R), (px, ty, pz - CHW_R - 0.06), 0.015, "stainless")
        b.box(px - 0.012, ty - 0.12, pz - CHW_R - 0.08, px + 0.012, ty + 0.12, pz - CHW_R - 0.06, sw("white"))
    pipe(b, (x1, 0.42, 4.2), (2.62, 0.42, 4.2), 0.03, "stainless")
    pipe(b, (2.62, 0.12, 4.2), (2.62, 0.44, 4.2), 0.03, "stainless")
    pipe(b, (2.62, 0.12, 4.2), (2.62, 0.12, 4.45), 0.03, "stainless")
    pipe(b, (2.62, 0.12, 4.45), (2.62, 0.3, 4.45), 0.03, "stainless")
    b.box(2.48, 0.0, 4.36, 2.76, 0.02, 4.64, sw("dgrey"))
    pipe(b, (2.62, 0.02, 4.5), (2.62, 0.3, 4.5), 0.035, "stainless")
    # The discharge: the plenum's collar out of the far end and its flexible connector, to the block's face (the room's
    # supply duct meets it).
    dx0, dx1, dy0_, dy1_ = duct_face(1.5, 1.5)
    b.box(dx0, dy0_, z1, dx1, dy1_, 6.96, [None, None, sw("black"), sw("black"), sw("black"), sw("black")])
    for fz in (z1 + 0.02, 6.96):
        b.box(dx0 - 0.03, dy0_ - 0.03, fz, dx1 + 0.03, dy1_ + 0.03, fz + 0.04, "stainless_dark")
    # The fan motor's disconnect switch (480 V) and the local control station on the fan section.
    za, zb, _ = AHU_SECTIONS[3]
    side_box(b, -1, x0, 0.14, 1.95, 2.35, zb - 0.02, zb + 0.32, "paint_grey", DARK)
    side_box(b, -1, x0 - 0.14, 0.004, 2.18, 2.32, zb + 0.02, zb + 0.28, placard(0), sw("white"))
    side_box(b, -1, x0 - 0.14, 0.05, 2.0, 2.12, zb + 0.08, zb + 0.13, sw("black"), sw("black"))
    station(b, -1, x0, za + 0.45, 1.3, live, "ahu")
    b.live = _mats(live)
    return b


# ------------------------------------------------------------------------------------------------ the chiller

CH_EVAP = (1.0, 0.85, 0.55)     # x, y of the axis, radius
CH_COND = (2.15, 0.85, 0.5)
CH_Z = (0.95, 4.45)
# The chilled water nozzles off the evaporator's front water box, up out of the top at local (0.5, 0.5) (return, in)
# and (1.5, 0.5) (supply, out); the service water in and out of the condenser through the floor.
CH_CHW = ((0.5, 0.75, "CHW RETURN"), (1.5, 0.95, "CHW SUPPLY"))     # x, the height it leaves the water box
CH_PANEL = (1.3, 3.15, 1.2, 2.5)     # z0, z1, y0, y1 of the control panel's door, on the left
CH_DISPLAY = (1.6, 2.65, 1.32, 1.78)     # its display (z0, z1, y0, y1): the game's readings


def chiller(seed=227):
    """A control room water chiller, 3 wide, 5 deep, 3 tall: the evaporator (black insulation) and the condenser (blue)
    side by side on the base, the water boxes on their ends (the chilled water out of the front and up; the service water
    up out of the floor into the front and out of the back down into it), the screw compressor and its motor on top,
    the oil separator, the control panel on the left with its display (the game's)."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": []}
    blue, ins = "paint_blue", "rubber"
    base_rails(b, 0.2, 2.8, 0.2, 4.8, 0.22)
    z0, z1 = CH_Z
    for (cx, cy, r), mat in ((CH_EVAP, ins), (CH_COND, blue)):
        pipe(b, (cx, cy, z0), (cx, cy, z1), r, mat, mat, 16)
        for za, zb in ((z0 - 0.22, z0 - 0.04), (z1 + 0.04, z1 + 0.22)):
            pipe(b, (cx, cy, za), (cx, cy, zb), r + 0.04, blue, blue, 16)
        for za, zb in ((z0 - 0.04, z0), (z1, z1 + 0.04)):
            pipe(b, (cx, cy, za), (cx, cy, zb), r + 0.08, "flange", "flange", 16)
        for sz in (z0 + 0.35, z1 - 0.35):
            b.box(cx - r * 0.7, 0.22, sz - 0.08, cx + r * 0.7, cy - r * 0.6, sz + 0.08, DARK)
    b.core = len(b.still)
    ex, ey, er = CH_EVAP
    kx, ky, kr = CH_COND
    # The chilled water: each nozzle forward out of the front water box, a flange, an elbow and up out of the top (the
    # room's pipes meet them), insulated.
    for nx, ny, legend in CH_CHW:
        pipe(b, (nx, ny, z0 - 0.22), (nx, ny, 0.5 + CHW_R), CHW_R, "insulation")
        flange(b, (nx, ny, z0 - 0.3), "z", CHW_R * 0.9)
        pipe(b, (nx, ny - CHW_R, 0.5), (nx, 3.0, 0.5), CHW_R, "insulation")
        flange(b, (nx, 2.94, 0.5), "y", CHW_R * 0.9, 0.06)
    side_plate(b, -1, 0.5 - CHW_R, 0.5, 2.0, 0.24, 0.06, named("CHW RETURN"), 0.006)
    gauge(b, -1, 0.5 - CHW_R, 0.5, 1.55, 0.075, GAUGE_P, live["gauges"], "EVAP IN")
    side_plate(b, 1, 1.5 + CHW_R, 0.5, 1.78, 0.24, 0.06, named("CHW SUPPLY"), 0.006)
    gauge(b, 1, 1.5 + CHW_R, 0.5, 1.5, 0.075, GAUGE_P, live["gauges"], "EVAP OUT")
    # The service water: up out of the floor into the condenser's front water box, out of its back one and down into the
    # floor, each through its floor sleeve and fire seal.
    for nz, ny, legend in ((0.5, 0.65, "SW IN"), (4.85, 1.05, "SW OUT")):
        sx = 2.5
        end = z0 - 0.22 if nz < 1 else z1 + 0.22
        pipe(b, (sx, ny, end), (sx, ny, nz + (0.08 if nz < 1 else -0.08)), 0.08, "pipe_green")
        pipe(b, (sx, 0.0, nz), (sx, ny + 0.08, nz), 0.08, "pipe_green")
        b.cyl((sx, 0.0, nz), (sx, 0.04, nz), 0.15, sw("red"), sw("red"), 10)
        b.cyl((sx, 0.04, nz), (sx, 0.1, nz), 0.11, DARK, DARK, 10)
        side_plate(b, 1, sx + 0.08, nz, 0.4, 0.24, 0.06, named(legend), 0.006)
    trained(b, lambda t: side_box(b, -1, 0.0, 0.008, 1.88, 2.42, CH_PANEL[0] + 0.25, CH_PANEL[0] + 1.33, title_plate("chiller", t), sw("white")))
    # The compressor on top: its casing on the shells' saddles, the motor, the discharge line over into the condenser,
    # the suction up out of the evaporator, the oil separator, relief valves and the condenser's sight glass.
    cx, cy = 1.55, 1.95
    pipe(b, (cx, cy, 1.15), (cx, cy, 3.1), 0.38, blue, blue, 14)
    pipe(b, (cx, cy, 3.1), (cx, cy, 4.2), 0.34, "paint_grey", "paint_grey", 14)
    for fz in (1.15, 3.1):
        pipe(b, (cx, cy, fz - 0.03), (cx, cy, fz + 0.03), 0.42, "flange", "flange", 14)
    b.box(cx - 0.3, 1.2, 1.4, cx + 0.3, cy - 0.33, 1.6, DARK)
    b.box(cx - 0.3, 1.2, 3.6, cx + 0.3, cy - 0.3, 3.8, DARK)
    b.box(cx - 0.15, cy + 0.33, 3.4, cx + 0.15, cy + 0.5, 3.9, "paint_grey")
    side_plate(b, -1, cx - 0.37, 2.4, cy, 0.5, 0.1, named("COMPRESSOR"))
    pipe(b, (ex + 0.15, ey + er - 0.02, 2.3), (ex + 0.15, 1.7, 2.3), 0.12, ins)
    pipe(b, (kx - 0.12, 1.62, 1.3), (kx - 0.12, ky + kr - 0.02, 1.3), 0.09, blue)
    pipe(b, (cx + 0.36, 1.62, 1.3), (kx - 0.12 + 0.09, 1.62, 1.3), 0.09, blue)
    pipe(b, (2.6, 1.3, 2.8), (2.6, 2.4, 2.8), 0.14, blue, blue, 12)
    side_plate(b, 1, 2.74, 2.8, 1.93, 0.28, 0.07, named("OIL SEP"), 0.006)
    for rz in (1.4, 3.9):
        pipe(b, (kx + 0.12, ky + kr - 0.04, rz), (kx + 0.12, ky + kr + 0.14, rz), 0.035, "flange")
        pipe(b, (kx + 0.12, ky + kr + 0.14, rz), (kx + 0.12, ky + kr + 0.22, rz), 0.05, sw("red"))
    b.cyl((kx + kr - 0.02, ky, 2.4), (kx + kr + 0.03, ky, 2.4), 0.06, sw("black"), sw("black"), 10)
    b.box(kx + kr + 0.03, ky - 0.035, 2.365, kx + kr + 0.034, ky + 0.035, 2.435, pic("glass"))
    side_plate(b, 1, kx + kr - 0.02, 2.3, 0.44, 0.6, 0.08, named("CONDENSER"), 0.006)
    side_plate(b, -1, ex - er + 0.02, 3.4, 0.86, 0.6, 0.08, named("EVAPORATOR"), 0.006)
    # The control panel on its legs on the left: its door, the title (the unit's name), the display (the game's), the
    # keypad, the emergency stop.
    za, zb, ya, yb = CH_PANEL
    for lz in (za + 0.08, zb - 0.08):
        b.box(0.06, 0.22, lz - 0.03, 0.12, ya, lz + 0.03, DARK)
    b.box(0.03, ya, za, 0.3, yb, zb, [sw("white"), sw("white"), None, None, sw("white"), sw("white")])
    side_box(b, -1, 0.03, 0.015, ya + 0.03, yb - 0.03, za + 0.03, zb - 0.03, sw("white"), DARK)
    dz0, dz1, dy0, dy1 = CH_DISPLAY
    side_box(b, -1, 0.015, 0.012, dy0 - 0.03, dy1 + 0.03, dz0 - 0.03, dz1 + 0.03, sw("black"), sw("black"))
    live["display"] = {"z0": dz0, "y0": dy0, "z1": dz1, "y1": dy1, "x": _r(0.002)[0], "side": -1}
    for i in range(4):
        for j in range(3):
            side_box(b, -1, 0.015, 0.012, dy0 + i * 0.11, dy0 + i * 0.11 + 0.08, zb - 0.42 + j * 0.11, zb - 0.34 + j * 0.11, DARK, sw("black"))
    b.cyl((0.015, yb - 0.25, zb - 0.22), (-0.03, yb - 0.25, zb - 0.22), 0.05, "paint_yellow", "paint_yellow", 10)
    b.cyl((-0.03, yb - 0.25, zb - 0.22), (-0.07, yb - 0.25, zb - 0.22), 0.07, sw("red"), sw("red"), 12)
    b.live = _mats(live)
    return b


# ------------------------------------------------------------------------------------------------ the pump

PUMP_AXIS = (0.5, 0.55)
PUMP_VOLUTE = (1.25, 1.75, 0.34)     # z0, z1, radius


def chw_pump(seed=229):
    """A chilled water pump, 1 wide, 3 deep, 2 tall, on its base on a housekeeping pad: the suction down from the
    return main into the top at local (0.5, 0.5) through its butterfly valve, an elbow into the eye of the end-suction
    pump; the discharge up out of the volute through the check valve and a butterfly valve, out of the top at (0.5, 1.5)
    toward the chiller; the bearing frame, the coupling guard, the motor."""
    b = C.Board(1, seed, face=0.0)
    live = {"gauges": []}
    green = "paint_green"
    b.box(0.03, 0, 0.03, 0.97, 0.12, 2.97, "concrete")
    b.box(0.12, 0.12, 0.62, 0.88, 0.2, 2.92, DARK)
    b.core = len(b.still)
    ax, ay = PUMP_AXIS
    vz0, vz1, vr = PUMP_VOLUTE
    # The volute, its feet; the bearing frame, the coupling guard, the motor (its feet, fins, terminal box and conduit).
    pipe(b, (ax, ay, vz0), (ax, ay, vz1), vr, green, green, 16)
    b.box(ax - 0.25, 0.2, vz0 + 0.05, ax + 0.25, ay - vr * 0.7, vz1 - 0.05, green)
    pipe(b, (ax, ay, vz1), (ax, ay, 2.0), 0.12, green, green, 12)
    b.box(ax - 0.1, 0.2, vz1 + 0.04, ax + 0.1, ay - 0.1, 1.96, green)
    b.box(ax - 0.15, ay - 0.15, 2.0, ax + 0.15, ay + 0.15, 2.22, "paint_yellow")
    pipe(b, (ax, ay, 2.22), (ax, ay, 2.86), 0.27, green, green, 14)
    for fz in (2.32, 2.45, 2.58, 2.71):
        pipe(b, (ax, ay, fz), (ax, ay, fz + 0.03), 0.29, green, green, 14)
    pipe(b, (ax, ay, 2.86), (ax, ay, 2.93), 0.21, green, green, 14)
    b.box(ax - 0.22, 0.2, 2.3, ax + 0.22, ay - 0.2, 2.82, green)
    b.box(ax + 0.27, ay - 0.08, 2.4, ax + 0.4, ay + 0.12, 2.65, green)
    pipe(b, (ax + 0.34, ay - 0.08, 2.52), (ax + 0.34, 0.12, 2.52), 0.03, DARK)
    side_box(b, 1, ax + 0.15, 0.004, ay - 0.12, ay + 0.12, 2.04, 2.18, placard(1), sw("white"))
    # The suction: down out of the top, its butterfly valve and gauge, the long-radius elbow into the pump's eye.
    sz = 0.5
    pipe(b, (ax, 2.0, sz), (ax, ay + 0.15, sz), CHW_R, "insulation")
    pipe(b, (ax, ay, sz + 0.12), (ax, ay, vz0), CHW_R, "insulation")
    b.box(ax - CHW_R, ay - CHW_R, sz - CHW_R, ax + CHW_R, ay + 0.15, sz + 0.12, "insulation")
    flange(b, (ax, ay, vz0 - 0.08), "z", CHW_R * 0.9)
    flange(b, (ax, 1.25, sz), "y", CHW_R * 0.9)
    valve_lever(b, (ax, 1.45, sz), -1)
    flange(b, (ax, 1.65, sz), "y", CHW_R * 0.9)
    flange(b, (ax, 1.94, sz), "y", CHW_R * 0.9, 0.06)
    gauge(b, -1, ax - CHW_R, sz, 1.05, 0.07, GAUGE_P, live["gauges"], "SUCTION")
    side_plate(b, -1, ax - CHW_R, sz, 0.85, 0.2, 0.05, named("SUCTION"), 0.006)
    # The discharge: up out of the volute's top, the check valve, the butterfly valve, its gauge, out of the top.
    dz = (vz0 + vz1) / 2
    pipe(b, (ax, ay + vr - 0.02, dz), (ax, 2.0, dz), 0.075, "insulation")
    b.box(ax - 0.11, 1.0, dz - 0.11, ax + 0.11, 1.2, dz + 0.11, green)
    b.box(ax - 0.03, 1.2, dz - 0.03, ax + 0.03, 1.24, dz + 0.03, DARK)
    valve_lever(b, (ax, 1.45, dz), -1)
    for fy in (0.98, 1.22, 1.38, 1.52):
        flange(b, (ax, fy, dz), "y", 0.075)
    flange(b, (ax, 1.94, dz), "y", 0.075, 0.06)
    gauge(b, -1, ax - 0.075, dz, 1.75, 0.07, GAUGE_P, live["gauges"], "DISCH")
    side_plate(b, -1, ax - 0.075, dz, 1.6, 0.2, 0.05, named("DISCH"), 0.006)
    # The title plate on its stand at the front of the pad; the local control station on its post behind the motor.
    trained(b, lambda t: b.box(0.1, 0.14, 0.06, 0.9, 0.54, 0.08, [title_plate("pump", t), None, sw("white"), sw("white"), sw("white"), sw("white")]))
    b.box(0.46, 0.12, 0.08, 0.54, 0.14, 0.2, DARK)
    b.box(0.89, 0.12, 2.6, 0.96, 1.1, 2.67, DARK)
    station(b, -1, 0.89, 2.64, 1.3, live, "pump")
    b.live = _mats(live)
    return b


# What each machine is (its name, its size: wide, deep, tall), as tools/nuclear.py's equipment table has it.
BOARDS = {"cr_filtration_unit": ("Control Room Emergency Filtration Unit", 3, 7, 3, filtration_unit),
          "cr_air_handling_unit": ("Control Room Air Handling Unit", 3, 7, 3, air_handling_unit),
          "cr_chiller": ("Control Room Water Chiller", 3, 5, 3, chiller),
          "chw_pump": ("Chilled Water Pump", 1, 3, 2, chw_pump)}
C.SECTIONS.update({k: v[4] for k, v in BOARDS.items()})
