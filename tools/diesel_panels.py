"""The diesel generator rooms' panels rebuilt (round 31, part 2; Justin: "Diesel Part 2: panels"): the seven pieces
along each room's south wall in real 3D, to the detail standard, with the engine and breaker controls working.

West to east: the station battery for the diesel (two blocks: a two-step rack of clear-jar cells), its battery
charger, the load sequencer, the generator protection relays, the excitation and voltage regulator, the generator
control panel (two blocks) and the engine control panel. Each is a LivePanelBlock two blocks high, labelled by train
(EDG 1A, EDG 1B: one drawing, the name lettered live). Built like the DC rooms' cabinets (plant_panels station_charger):
the front drawn whole at 512 px a block and cut into the blocks, every part that stands out of it a real box (meter
cases with the face set back behind glass and a zero adjuster, lamps in chrome bezels, push buttons, selector
switches, the breaker's pistol grip, the emergency stop's mushroom, relays in their drawout cases with test switches,
the lockout relay, doors with hinges and a T-handle, windows onto card racks).

What moves is client/LivePanelRenderer.java's, laid out in live_panels.json: needles, lamps, windows, displays, and
the controls that turn or push in ("part": a few boxes turned about their hub or pushed in by a channel): the MODE
switch (OFF, LOCAL, AUTO), the emergency stop (latched in), the breaker control switch (TRIP, CLOSE, springing back),
the 86G lockout's handle (RESET, TRIP). Clicking them works (nuclear/DieselPanels.java).

Justin's answers (round 31): controls "Engine and breaker" (START, STOP, EMERG STOP, MODE, the breaker's TRIP/CLOSE,
the 86G lockout); synchroscope "Emergency start only"; a LOCAL start's breaker "Closes itself" (as an emergency start's
does, five seconds in); the 86G trips from "Handle and relay tests" (its own handle, or a protection relay's test
switch: the relay's target drops); the emergency stop "Latches in" (pull it out to reset).
"""
import math

import plant_panels as PP
from plant_panels import Front, INK, U, _sc_round

D = PP.SC_DOOR                    # a door's (or a panel sheet's) face
BLK = "pi_pnl_black"
GLASS = PP.SC_GLASS
WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
PLATE = (204, 206, 208, 255)       # a switch's satin aluminium escutcheon
ORANGE = (255, 150, 40)
LABELS = ["EDG 1A", "EDG 1B"]

ENGINE, GENERATOR, AVR, PROTECTION, SEQUENCER, CHARGER, BATTERY = (
    "edg_engine_panel", "edg_generator_panel", "edg_voltage_regulator", "edg_protection_panel", "edg_load_sequencer",
    "edg_battery_charger", "edg_battery_rack")


def _fit(f, cx, cy, text, size, max_w, fill=INK, anchor="mm"):
    """Bold condensed lettering brought down half a size at a time (no smaller than 6) until it fits max_w."""
    while size > 6 and f.d.textlength(text, font=PP.cfont(size, True)) > max_w * U:
        size -= 0.5
    f.T(cx, cy, text, size, fill=fill, bold=True, anchor=anchor)


# ------------------------------------------------------------------------------------------------ parts of a panel

def nameplate(f, b, x0, y0, x1, y1, title, sub=None, face=None):
    """The engraved nameplate: the title, a second line if any, and the train's name lettered live under them."""
    f.R(x0, y0, x1, y1, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    cx = (x0 + x1) / 2
    lines = [title] + ([sub] if sub else [])
    top = y1 - 0.5
    _fit(f, cx, top, title, 10.5, x1 - x0 - 0.6, fill=WHITE)
    if sub:
        _fit(f, cx, top - 0.68, sub, 7, x1 - x0 - 0.6, fill=WHITE)
    f.text(cx, y0 + 0.42, D + 0.14, 0.5, (238, 238, 232), "label", "center")
    b.append((x0, y0, D, x1, y1, D + 0.12, face))
    return lines


def case(x0, y0, x1, y1, face, depth=0.7, rim=0.4, zero=True):
    """A black instrument case out of the door: its rim, the face set back inside it (the drawing), the glass in front
    and the zero adjuster under it."""
    xm = (x0 + x1) / 2
    b = [(x0, y0, D, x0 + rim, y1, D + depth, BLK), (x1 - rim, y0, D, x1, y1, D + depth, BLK),
         (x0 + rim, y0, D, x1 - rim, y0 + rim, D + depth, BLK), (x0 + rim, y1 - rim, D, x1 - rim, y1, D + depth, BLK),
         (x0 + rim, y0 + rim, D, x1 - rim, y1 - rim, D + 0.15, face),
         (x0 + rim, y0 + rim, D + depth - 0.15, x1 - rim, y1 - rim, D + depth - 0.1, {"south": GLASS, "north": GLASS})]
    if zero:
        b.append((xm - 0.25, y0 + 0.06, D + depth, xm + 0.25, y0 + rim - 0.06, D + depth + 0.15, "pi_cap_black"))
    return b


def gauge(f, b, face, cx, cy, r, label, scale, unit, ch, red=None, ticks=(0, 5, 10), rim=0.35):
    """A round gauge in a square black case: white face, a 270-degree scale (its numbers at ticks: tenths of it), the
    red band, the unit; the needle live from channel ch. label: one or two lines under the case."""
    h = r + rim + 0.05
    f.R(cx - h, cy - h, cx + h, cy + h, fill=(30, 30, 32, 255))
    f.E(cx, cy, r + 0.08, fill=(86, 88, 92, 255))
    f.E(cx, cy, r, fill=(246, 246, 240, 255))
    lo, hi = scale
    start, sweep = 225, 270
    ang = lambda v: math.radians(start - sweep * (v - lo) / (hi - lo))
    if red:
        a0, a1 = ang(red[0]), ang(red[1])
        for k in range(40):
            a = a0 + (a1 - a0) * k / 39
            f.E(cx + math.cos(a) * r * 0.86, cy + math.sin(a) * r * 0.86, r * 0.065, fill=(200, 40, 34, 255))
    small = r < 1.6
    n = 20 if small else 50
    for k in range(n + 1):
        a = math.radians(start - sweep * k / n)
        major = k % 5 == 0
        r0 = r * (0.76 if major else 0.83)
        f.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r * 0.93, cy + math.sin(a) * r * 0.93)],
               INK, (2 if small else 3) if major else 1)
    for k in ticks:
        a = math.radians(start - sweep * k / 10)
        v = lo + (hi - lo) * k / 10
        f.T(cx + math.cos(a) * r * (0.5 if small else 0.55), cy + math.sin(a) * r * (0.5 if small else 0.55), f"{v:g}", 6 if small else 8, bold=True)
    # (A small gauge's unit over its hub, out of the way of its end numbers.)
    f.T(cx, cy + r * 0.36 if r < 1.6 else cy - r * 0.74, unit, 6.5 if r < 1.6 else 8, bold=True)
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": r, "z": D + 0.3, "lo": lo, "hi": hi, "start": start, "sweep": sweep, "ch": ch})
    lines = label.split("|")
    for k, line in enumerate(lines):
        _fit(f, cx, cy - h - 0.42 - k * 0.55, line, 7, 2 * h + 0.4)
    b += case(cx - h, cy - h, cx + h, cy + h, face, rim=rim)


def meter(f, b, face, x0, y0, x1, y1, label, scale, unit, ch, ticks=None):
    """A square switchboard meter: black case, white quarter-arc scale, the needle live, its legend under it."""
    f.R(x0, y0, x1, y1, fill=(28, 28, 30, 255))
    f.R(x0 + 0.4, y0 + 0.4, x1 - 0.4, y1 - 0.4, fill=(244, 244, 238, 255))
    cx, cy, r = x0 + 0.75, y0 + 0.75, min(x1 - x0, y1 - y0) - 1.45
    lo, hi = scale
    for k in range(41):
        a = math.radians(90 - 90 * k / 40)
        major = k % 10 == 0
        r0 = r * (0.86 if major else 0.93 if k % 2 else 0.9)
        f.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r, cy + math.sin(a) * r)], INK, 3 if major else 1)
    marks = ticks or [lo, (lo + hi) / 2, hi]
    for k, v in enumerate(marks):
        a = math.radians(90 - 90 * k / (len(marks) - 1))
        dx = 0.4 if k == 0 else 0
        dy = 0.25 if k == len(marks) - 1 else 0
        f.T(cx + math.cos(a) * r * 0.7 + dx, cy + math.sin(a) * r * 0.7 + dy, f"{v:g}", 7, bold=True)
    f.T(x1 - 0.7, y1 - 0.95, unit, 8, bold=True, anchor="rm")
    f.E(cx, cy, 0.18, fill=(34, 34, 34, 255))
    f.live.append({"t": "meter", "x": cx, "y": cy, "r": r * 0.97, "z": D + 0.3, "lo": lo, "hi": hi, "ch": ch})
    _fit(f, (x0 + x1) / 2, y0 - 0.5, label, 7.5, x1 - x0 + 0.6)
    b += case(x0, y0, x1, y1, face)


LENSES = {(40, 180, 70): "pi_lens_green", (240, 240, 230): "pi_lens_white", (240, 170, 30): "pi_lens_amber", (230, 50, 40): "pi_lens_red"}


def lamp(f, b, x, y, colour, ch, label=None, r=0.45, label_size=6.5):
    """A pilot lamp: the chrome bezel, the lens (lit live from ch), its legend under it. (r at least 0.42: a smaller
    round part is mangled by tools/zfight.py.)"""
    r = max(0.42, r)
    b += _sc_round(x, y, D, D + 0.3, r + 0.22, "pi_chrome") + _sc_round(x, y, D + 0.3, D + 0.5, r, LENSES[colour])
    f.live.append({"t": "lamp", "x": x, "y": y, "r": r, "z": D + 0.52, "colour": list(colour), "ch": ch})
    if label:
        for k, line in enumerate(label.split("|")):
            _fit(f, x, y - r - 0.62 - k * 0.5, line, label_size, 3.0)


def button(f, b, face, x, y, colour_tex, legend, r=0.6, plate=True):
    """A push button: its chrome ring, the coloured head standing out of it, its engraved legend plate under it."""
    b += _sc_round(x, y, D, D + 0.25, r + 0.25, "pi_chrome") + _sc_round(x, y, D + 0.25, D + 0.62, r, colour_tex)
    if plate:
        w = max(1.8, 2 * r + 0.8)
        f.R(x - w / 2, y - r - 1.05, x + w / 2, y - r - 0.4, fill=BLACK)
        _fit(f, x, y - r - 0.72, legend, 7, w - 0.2, fill=WHITE)
        b.append((x - w / 2, y - r - 1.05, D, x + w / 2, y - r - 0.4, D + 0.1, face))


def selector_plate(f, b, face, cx, cy, w, h, positions, name, arc=45, radius=1.25, size=6.5, drop=0.0, mark=1.0, beside=False):
    """A switch's engraved plate about its hub (cx, cy) (the plate's middle drop below it): its positions round the
    top (at arc degrees either side of up), its name along the bottom; a mark at each position (mark: their scale, a
    small knob's smaller)."""
    py = cy - drop
    f.R(cx - w / 2, py - h / 2, cx + w / 2, py + h / 2, fill=PLATE, outline=(120, 122, 124, 255), width=2)
    n = len(positions)
    for i, pos in enumerate(positions):
        if not pos:
            continue
        a = math.radians(90 + arc - 2 * arc * i / max(1, n - 1)) if n > 1 else math.radians(90)
        f.line([(cx + math.cos(a) * 0.8 * mark, cy + math.sin(a) * 0.8 * mark), (cx + math.cos(a) * 0.95 * mark, cy + math.sin(a) * 0.95 * mark)], INK, 3)
        side = abs(math.cos(a)) > 0.3
        if side and beside:
            # Beside the hub, out of the way of a long handle turned to it (a breaker's pistol grip).
            f.T(cx + math.copysign(w * 0.3, math.cos(a)), cy - 0.75, pos, size, fill=INK, bold=True)
            continue
        f.T(cx + math.cos(a) * radius * (1.45 if side else 1.0), cy + math.sin(a) * radius * (0.9 if side else 1.0) + 0.15, pos, size,
            fill=INK, bold=True)
    if name:
        _fit(f, cx, py - h / 2 + 0.38, name, 6.5, w - 0.3, fill=INK)
    b.append((cx - w / 2, py - h / 2, D, cx + w / 2, py + h / 2, D + 0.12, {**face, "*": "pi_chrome"}))


def wing_knob(cx, cy, z, scale=1.0):
    """A selector's black wing knob (its white pointer up), standing out from z: as boxes about (cx, cy)."""
    k = scale
    return ([(x0, y0, z0, x1, y1, z1, BLK) for x0, y0, z0, x1, y1, z1, _ in _sc_round(cx, cy, z, z + 0.3, max(0.42, 0.62 * k), BLK)]
            + [(cx - 0.22 * k, cy - 0.95 * k, z + 0.3, cx + 0.22 * k, cy + 0.95 * k, z + 0.85, BLK),
               (cx - 0.07, cy + 0.3 * k, z + 0.85, cx + 0.07, cy + 0.88 * k, z + 0.9, "pi_pnl_white")])


def pistol_grip(cx, cy, z, scale=1.0):
    """A control switch's pistol grip (pointing up at rest): the hub, the long grip, its rounded end, the short tail."""
    s = scale
    return ([(x0, y0, z0, x1, y1, z1, BLK) for x0, y0, z0, x1, y1, z1, _ in _sc_round(cx, cy, z, z + 0.3, 0.62 * s, BLK)]
            + [(cx - 0.34 * s, cy - 0.55 * s, z + 0.3, cx + 0.34 * s, cy + 1.25 * s, z + 0.95, BLK),
               (cx - 0.25 * s, cy + 1.25 * s, z + 0.35, cx + 0.25 * s, cy + 1.55 * s, z + 0.9, BLK),
               (cx - 0.08 * s, cy + 0.5 * s, z + 0.95, cx + 0.08 * s, cy + 1.2 * s, z + 1.0, "pi_pnl_white")])


def turned(boxes, cx, cy, angle):
    """Boxes turned about (cx, cy) on the panel's face (a model's own rotation, for a still part)."""
    if not angle:
        return list(boxes)
    return [tuple(bx[:7]) + ({"rotation": {"angle": angle, "axis": "z", "origin": [cx, cy, D]}},) for bx in boxes]


# The colours a moving part's boxes are drawn in (client/LivePanelRenderer.java: flat, shaded like a block).
PART_COLOURS = {BLK: (32, 32, 34), "pi_pnl_white": (236, 236, 230), "pi_pnl_red": (196, 38, 32), "pi_chrome": (196, 200, 204),
                "pi_pnl_yellow": (236, 196, 30)}


def part(f, ch, cx, cy, boxes, angles=None, push=None):
    """A control that moves (live): its boxes turned about (cx, cy) by angles[channel value] (degrees, anticlockwise as
    you face it), or pushed in by push[value] (model pixels)."""
    f.live.append({"t": "part", "x": cx, "y": cy, "ch": ch, "angles": angles or [], "push": push or [],
                   "boxes": [list(bx[:6]) + [list(PART_COLOURS[bx[6]])] for bx in boxes]})


def ann_grid(ax, ay, cols, rows, ww, wh, face):
    """An annunciator's black grid out of the door, its windows set back in it (the outer frame outside them)."""
    blk = BLK
    gx0, gy0, gx1, gy1 = ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3
    b = [(gx0, gy0, D, gx1, gy1, D + 0.25, face)]
    for c in range(cols + 1):
        x = ax + c * ww
        xa, xb = (x - 0.3, x + 0.12) if c == 0 else (x - 0.12, x + 0.3) if c == cols else (x - 0.12, x + 0.12)
        b.append((xa, gy0, D + 0.25, xb, gy1, D + 0.5, blk))
    for r in range(rows + 1):
        y = ay + r * wh
        ya, yb = (y - 0.3, y + 0.12) if r == 0 else (y - 0.12, y + 0.3) if r == rows else (y - 0.12, y + 0.12)
        for c in range(cols):
            xa = ax + c * ww + 0.12
            xb = ax + (c + 1) * ww - 0.12
            b.append((xa, ya, D + 0.25, xb, yb, D + 0.5, blk))
    return b


def louvre(b, x0, x1, F_door=None):
    """A louvred air inlet low in the door: its frame and four slats leaning out."""
    b += [(x0, 1.4, D, x1, 1.6, D + 0.3, "pi_cabinet_dark"), (x0, 3.6, D, x1, 3.8, D + 0.3, "pi_cabinet_dark"),
          (x0, 1.6, D, x0 + 0.2, 3.6, D + 0.3, "pi_cabinet_dark"), (x1 - 0.2, 1.6, D, x1, 3.6, D + 0.3, "pi_cabinet_dark")]
    for k in range(4):
        yc = 1.95 + k * 0.5
        b.append((x0 + 0.2, yc - 0.06, D - 0.1, x1 - 0.2, yc + 0.06, D + 0.4, "pi_cabinet_dark",
                  {"rotation": {"angle": -22.5, "axis": "x", "origin": [(x0 + x1) / 2, yc, D + 0.15]}}))


def shell(F, width=1, door=True, windows=(), handle=True):
    """A cabinet two blocks high: plinth, body, its front (a door with hinges on the left and the T-handle on the right,
    or a fixed panel sheet with its screws), cap. windows: openings in the door (x0, y0, x1, y1), each with its glass
    behind a black gasket (what is behind them is the piece's own)."""
    W = 16 * width
    b = [(0.8, 0, 2.6, W - 0.8, 1.2, 14.0, "pi_toe_kick"),
         (0.2, 30.4, 1.4, W - 0.2, 31.2, 15.2, "pi_cabinet_dark")]
    # The body: solid, but hollow behind a window (what shows through it stands in the hollow).
    bys = [1.2]
    for wx0, wy0, wx1, wy1 in sorted(windows, key=lambda w: w[1]):
        bys += [wy0, wy1]
    bys.append(30.4)
    for k in range(0, len(bys), 2):
        b.append((0.4, bys[k], 1.6, W - 0.4, bys[k + 1], 14.6, "pi_cabinet"))
    for wx0, wy0, wx1, wy1 in windows:
        b += [(0.4, wy0, 1.6, wx0, wy1, 14.6, "pi_cabinet"), (wx1, wy0, 1.6, W - 0.4, wy1, 14.6, "pi_cabinet"),
              (wx0, wy0, 1.6, wx1, wy1, CARD_Z[0] - 0.3, "pi_cabinet")]
    front = {"*": "pi_cabinet", "south": F}
    x0, y0, x1, y1 = (0.6, 1.4, W - 0.6, 30.2)
    # The door (or sheet) round its windows: cut into strips so each window is an opening.
    cuts = sorted(windows, key=lambda w: w[1])
    ys = [y0]
    for wx0, wy0, wx1, wy1 in cuts:
        ys += [wy0, wy1]
    ys.append(y1)
    for k in range(0, len(ys), 2):
        if ys[k + 1] - ys[k] > 1e-6:
            b.append((x0, ys[k], 14.6, x1, ys[k + 1], D, front))
    for wx0, wy0, wx1, wy1 in cuts:
        b += [(x0, wy0, 14.6, wx0, wy1, D, front), (wx1, wy0, 14.6, x1, wy1, D, front),
              (wx0, wy0, D - 0.3, wx1, wy1, D - 0.2, {"south": PP.RC_GLASS, "north": PP.RC_GLASS}),
              (wx0, wy0, D - 0.2, wx0 + 0.2, wy1, D + 0.1, BLK), (wx1 - 0.2, wy0, D - 0.2, wx1, wy1, D + 0.1, BLK),
              (wx0 + 0.2, wy0, D - 0.2, wx1 - 0.2, wy0 + 0.2, D + 0.1, BLK), (wx0 + 0.2, wy1 - 0.2, D - 0.2, wx1 - 0.2, wy1, D + 0.1, BLK)]
    if door:
        for y in (4.0, 26.0):
            b.append((0.15, y, 14.5, 0.6, y + 2.0, 15.25, "pi_chrome"))
        if handle:
            hx = W - 2.3
            b += [(hx, 12.4, D, hx + 1.0, 17.4, D + 0.15, BLK), (hx + 0.3, 13.0, D + 0.15, hx + 0.7, 16.6, D + 0.5, "pi_chrome"),
                  (hx + 0.15, 15.8, D + 0.5, hx + 0.85, 16.4, D + 0.75, "pi_chrome"), (hx + 0.35, 12.7, D + 0.15, hx + 0.65, 12.95, D + 0.3, "pi_brass")]
    else:
        # A fixed panel: its screws round the edge.
        for sy in (2.0 if not windows else 4.4, 29.8):
            for sx in [1.0] + [W / 2] * (width > 1 and sy < 29) + [W - 1.0]:
                b += _sc_round(sx, sy, D, D + 0.12, 0.42, "pi_chrome")
    return b


def door_seam(f, W=16):
    """A door's edge and its gasket line, drawn."""
    f.R(0.6, 1.4, W - 0.6, 30.2, outline=(130, 132, 128, 255), width=3)


# ------------------------------------------------------------------------------------------------ the engine panel

# The local engine control panel (the engine end of the row): engine speed and the starting air big, the lube oil,
# jacket water and fuel oil gauges small (psig and degrees F, as the set's gauge board), the hour meter, a 12-window
# annunciator, READY and RUNNING lamps with LAMP TEST, the MODE switch (OFF, LOCAL, AUTO), START and STOP, and the
# emergency stop's mushroom on its yellow plate.
EP_BIG = [(4.4, 24.85, 2.0, "ENGINE SPEED", (0, 800), "RPM", "rpm", (590, 800)),
          (11.6, 24.85, 2.0, "STARTING AIR", (0, 400), "PSIG", "air", (0, 150))]
EP_SMALL = [(2.45, 19.15, 1.15, "LUBE OIL|PRESS", (0, 100), "PSIG", "lube", (0, 25)),
            (5.8, 19.15, 1.15, "LUBE OIL|TEMP", (0, 250), "°F", "lube_t", (210, 250)),
            (9.15, 19.15, 1.15, "JKT WATER|TEMP", (0, 250), "°F", "jw", (200, 250)),
            (12.5, 19.15, 1.15, "FUEL OIL|PRESS", (0, 100), "PSIG", "fuel_p", (0, 20))]
EP_ANN = (1.4, 10.75, 4, 3, 2.9, 1.5)
EP_WINDOWS = [("LOW LUBE|OIL PRESS", "ann_low_lube"), ("HIGH LUBE|OIL TEMP", "ann_lube_temp"), ("HIGH JKT|WATER TEMP", "ann_high_jw"),
              ("ENGINE|OVERSPEED", "ann_overspeed"), ("FAIL TO|START", "ann_fail_start"), ("LOW START|AIR PRESS", "ann_low_air"),
              ("LOW DAY|TANK LEVEL", "ann_low_day_tank"), ("BATTERY|CHGR FAIL", "ann_charger_fail"), ("GEN BKR|TRIPPED", "ann_bkr_trip"),
              ("86G|LOCKOUT", "ann_lockout"), ("EMERG|STOP", "ann_estop"), ("NOT IN|AUTO", "ann_not_auto")]
EP_HOURS = (1.4, 8.05, 6.4, 9.55)            # the hour meter's case
EP_LAMPS = [(8.3, "READY", (240, 240, 230), "lamp_ready"), (10.5, "RUNNING", (230, 50, 40), "lamp_run")]
EP_LAMP_Y = 8.8
EP_TEST = (12.7, 8.8)                        # LAMP TEST
EP_MODE = (2.95, 5.45)                       # the MODE switch's hub
EP_START, EP_STOP = (6.55, 5.95), (8.95, 5.95)
EP_ESTOP = (12.15, 5.6)
MODE_ANGLES = [45, 0, -45]                   # OFF, LOCAL, AUTO
ESTOP_PUSH = [0, -0.32]


def engine_panel():
    f = Front(1, 401)
    F = f"pi_{ENGINE}_f"
    face = {"*": BLK, "south": F}
    b = shell(F)
    door_seam(f)
    nameplate(f, b, 2.0, 27.75, 14.0, 30.0, "DIESEL ENGINE CONTROL", "EMERGENCY DIESEL GENERATOR", face)
    for cx, cy, r, label, scale, unit, ch, red in EP_BIG:
        gauge(f, b, face, cx, cy, r, label, scale, unit, ch, red)
    for cx, cy, r, label, scale, unit, ch, red in EP_SMALL:
        gauge(f, b, face, cx, cy, r, label, scale, unit, ch, red, ticks=(0, 10), rim=0.3)
    # The annunciator: its windows' legends (lit live), in its black grid.
    ax, ay, cols, rows, ww, wh = EP_ANN
    f.R(ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3, fill=(30, 30, 32, 255))
    for i, (text, ch) in enumerate(EP_WINDOWS):
        c, r = i % cols, i // cols
        wx0, wy1 = ax + c * ww + 0.12, ay + (rows - r) * wh - 0.12
        wx1, wy0 = wx0 + ww - 0.24, wy1 - wh + 0.24
        f.R(wx0, wy0, wx1, wy1, fill=(232, 232, 222, 255))
        words = text.split("|")
        for k, word in enumerate(words):
            _fit(f, (wx0 + wx1) / 2, (wy0 + wy1) / 2 + (len(words) - 1) * 0.27 - k * 0.54, word, 6, wx1 - wx0 - 0.1)
        f.live.append({"t": "window", "x0": wx0, "y0": wy0, "x1": wx1, "y1": wy1, "z": D + 0.33, "colour": [255, 200, 80], "ch": ch,
                       "alpha": 1.0, "label": words})
    b += ann_grid(*EP_ANN, face)
    # The hour meter: its case, six wheels behind a window (the tenths on red), live.
    hx0, hy0, hx1, hy1 = EP_HOURS
    f.R(hx0, hy0, hx1, hy1, fill=(28, 28, 30, 255))
    for k in range(6):
        x = hx0 + 0.55 + k * 0.66
        f.R(x, hy0 + 0.35, x + 0.56, hy1 - 0.35, fill=(240, 240, 236, 255) if k < 5 else (200, 40, 34, 255))
        f.text(x + 0.28, (hy0 + hy1) / 2, D + 0.2, 0.55, INK if k < 5 else (255, 255, 255), f"hours{k}", "center")
    _fit(f, (hx0 + hx1) / 2, hy0 - 0.42, "HOURS RUN", 6.5, 4.0)
    b += case(hx0, hy0, hx1, hy1, face, depth=0.45, rim=0.3, zero=False)
    for x, label, colour, ch in EP_LAMPS:
        lamp(f, b, x, EP_LAMP_Y, colour, ch, label)
    tx, ty = EP_TEST
    b += _sc_round(tx, ty, D, D + 0.2, 0.62, "pi_chrome") + _sc_round(tx, ty, D + 0.2, D + 0.5, 0.42, BLK)
    _fit(f, tx, ty - 1.05, "LAMP|TEST".replace("|", " "), 6, 2.2)
    # MODE (still at its rest here: the knob is live), START and STOP, the emergency stop.
    mx, my = EP_MODE
    selector_plate(f, b, face, mx, my, 3.5, 3.3, ["OFF", "LOCAL", "AUTO"], "MODE", radius=1.12)
    part(f, "mode", mx, my, wing_knob(mx, my, D + 0.12), angles=MODE_ANGLES)
    button(f, b, face, *EP_START, "pi_pnl_green", "START")
    button(f, b, face, *EP_STOP, "pi_pnl_red", "STOP")
    ex, ey = EP_ESTOP
    f.R(ex - 1.6, ey - 1.75, ex + 1.6, ey + 1.75, fill=(236, 196, 30, 255), outline=(30, 30, 32, 255), width=2)
    f.E(ex, ey, 1.2, fill=(30, 30, 32, 255))
    _fit(f, ex, ey + 1.43, "EMERGENCY", 6, 3.0)
    _fit(f, ex, ey - 1.43, "STOP", 6.5, 3.0)
    b.append((ex - 1.6, ey - 1.75, D, ex + 1.6, ey + 1.75, D + 0.1, {"*": "pi_pnl_yellow", "south": F}))
    # Its shroud (a black collar) and the mushroom on its stem (live: pushed in, it latches).
    b += _sc_round(ex, ey, D + 0.1, D + 0.55, 1.08, BLK)
    part(f, "estop", ex, ey, [(x0, y0, z0, x1, y1, z1, "pi_pnl_red") for x0, y0, z0, x1, y1, z1, _ in
                              _sc_round(ex, ey, D + 0.55, D + 0.95, 0.42, "pi_pnl_red") + _sc_round(ex, ey, D + 0.95, D + 1.45, 0.92, "pi_pnl_red")],
         push=ESTOP_PUSH)
    louvre(b, 1.8, 14.2)
    return f, b


# ------------------------------------------------------------------------------------------------ the generator panel

# The generator control panel (two blocks, a fixed front): the switchboard meters (volts, amps, kilowatts, kilovars;
# frequency and power factor), the synchroscope between its two SYNC lamps (it turns while the engine runs up and
# locks at 12 when the breaker closes: Justin's "Emergency start only"), the mimic of the generator onto its breaker
# 52G onto 4160 V bus 1A, the breaker's control switch (a pistol grip: TRIP left, CLOSE right, springing back to the
# middle) with its flag and OPEN and CLOSED lamps, voltage and speed adjust, the meter selector switches, SYNC,
# EXCITATION and GOVERNOR MODE.
GP_METERS = [(1.2, "AC VOLTS", (0, 5), "kV", "kv", None), (8.8, "AC AMPS", (0, 1500), "A", "amps", None),
             (17.0, "KILOWATTS", (0, 10000), "kW", "kw", [0, 5000, 10000]), (24.6, "KILOVARS", (-5000, 5000), "kVAR", "kvar", [-5000, 0, 5000])]
GP_METER = (6.2, 21.3, 26.1)
GP_ROW2 = [(1.2, "FREQUENCY", (55, 65), "Hz", "hz", None), (24.6, "POWER FACTOR", (0, 1), "PF", "pf", None)]
GP_ROW2_Y = (14.5, 19.3)
GP_SYNC = (16.0, 16.35, 3.1)                 # the synchroscope: centre, its case's half width
GP_SYNC_LAMPS = (10.5, 21.5)
GP_MIMIC = (1.2, 7.1, 11.0, 12.0)
GP_BKR = (16.0, 9.35)                        # the breaker control switch's hub
GP_BKR_LAMPS = [(12.75, "OPEN", (40, 180, 70), "bkr_open"), (19.25, "CLOSED", (230, 50, 40), "bkr_closed")]
GP_ADJUST = [(23.4, 9.35, "VOLTAGE", "ADJUST"), (28.2, 9.35, "SPEED", "ADJUST")]
GP_KNOBS = [(3.0, 5.25, ["AB", "BC", "CA"], "VOLTMETER", 0), (7.2, 5.25, ["A", "B", "C"], "AMMETER", 0),
            (11.6, 5.25, ["OFF", "ON"], "SYNC", 0), (20.4, 5.25, ["OFF", "ON"], "EXCITATION", 1), (24.6, 5.25, ["DROOP", "ISOCH"], "GOVERNOR", 1),
            (28.8, 5.25, ["OFF", "ON"], "SPACE HTR", 0)]
BKR_ANGLES = [0, 45, -45]                    # at rest, TRIP (held), CLOSE (held)


def generator_panel():
    f = Front(2, 402)
    F = f"pi_{GENERATOR}_f"
    face = {"*": BLK, "south": F}
    b = shell(F, 2, door=False)
    f.R(0.6, 1.4, 31.4, 30.2, outline=(130, 132, 128, 255), width=3)
    nameplate(f, b, 8.0, 27.3, 24.0, 30.0, "GENERATOR CONTROL", "4160 V   3 PH   60 HZ   9000 KW   0.8 PF   514 RPM", face)
    w, y0, y1 = GP_METER
    for x0, label, scale, unit, ch, ticks in GP_METERS:
        meter(f, b, face, x0, y0, x0 + w, y1, label, scale, unit, ch, ticks)
    for x0, label, scale, unit, ch, ticks in GP_ROW2:
        meter(f, b, face, x0, GP_ROW2_Y[0], x0 + w, GP_ROW2_Y[1], label, scale, unit, ch, ticks)
    # The synchroscope: a round face, the 12 o'clock mark red, SLOW and FAST; its pointer live (a whole turn is 360).
    cx, cy, h = GP_SYNC
    r = h - 0.55
    f.R(cx - h, cy - h, cx + h, cy + h, fill=(28, 28, 30, 255))
    f.E(cx, cy, r + 0.08, fill=(86, 88, 92, 255))
    f.E(cx, cy, r, fill=(246, 246, 240, 255))
    for k in range(36):
        a = math.radians(90 - 10 * k)
        major = k % 9 == 0
        f.line([(cx + math.cos(a) * r * (0.78 if major else 0.85), cy + math.sin(a) * r * (0.78 if major else 0.85)),
                (cx + math.cos(a) * r * 0.94, cy + math.sin(a) * r * 0.94)], INK, 3 if major else 1)
    f.R(cx - 0.22, cy + r * 0.6, cx + 0.22, cy + r * 0.94, fill=(200, 40, 34, 255))
    f.T(cx - 1.15, cy + 0.15, "SLOW", 7, bold=True)
    f.T(cx + 1.15, cy + 0.15, "FAST", 7, bold=True)
    for k, a in enumerate((150, 30)):
        aa = math.radians(a)
        f.line([(cx + math.cos(aa) * r * 0.55, cy - 0.65), (cx + math.cos(aa) * r * 0.55 + (-0.35 if k == 0 else 0.35), cy - 0.95)], INK, 2)
    f.T(cx, cy - r * 0.5, "SYNCHROSCOPE", 6.5, bold=True)
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": r * 1.1, "z": D + 0.3, "lo": 0, "hi": 360, "start": 90, "sweep": 360, "ch": "sync"})
    _fit(f, cx, cy - h - 0.45, "INCOMING  -  RUNNING", 6.5, 2 * h)
    b += case(cx - h, cy - h, cx + h, cy + h, face, rim=0.45)
    for x in GP_SYNC_LAMPS:
        lamp(f, b, x, cy, (240, 240, 230), "sync_lamp", "SYNC")
    # The mimic: the generator, its breaker (lit red closed, green open), the bus.
    mx0, my0, mx1, my1 = GP_MIMIC
    f.R(mx0, my0, mx1, my1, fill=(52, 54, 58, 255))
    LINE, mim = (170, 172, 168, 255), (60, 110, 200, 255)
    gx, gy = mx0 + 1.6, (my0 + my1) / 2
    f.E(gx, gy, 0.9, outline=mim, width=6)
    f.T(gx, gy, "G", 11, fill=mim, bold=True)
    f.R(gx + 0.9, gy - 0.1, 5.6, gy + 0.1, fill=mim)
    f.R(7.2, gy - 0.1, 8.9, gy + 0.1, fill=mim)
    f.R(8.9, my0 + 0.5, 9.15, my1 - 0.5, fill=mim)
    f.R(5.6, gy - 0.7, 7.2, gy + 0.7, fill=(40, 160, 70, 255), outline=(20, 20, 20, 255), width=2)
    f.T(6.4, gy + 1.15, "52G", 7, fill=WHITE, bold=True)
    f.T(9.95, my1 - 0.65, "SAFETY", 6, fill=WHITE, bold=True)
    f.T(9.95, my1 - 1.15, "BUS", 6, fill=WHITE, bold=True)
    f.T(gx, my0 + 0.45, "4160 V", 6.5, fill=WHITE, bold=True)
    f.glow(5.7, gy - 0.6, 7.1, gy + 0.6, D + 0.4, (200, 40, 34), "bkr_closed", alpha=0.95)
    b.append((mx0, my0, D, mx1, my1, D + 0.15, face))
    b.append((5.6, gy - 0.7, D + 0.15, 7.2, gy + 0.7, D + 0.35, face))
    # The breaker's control switch: its plate (TRIP, CLOSE, the flag's window), the pistol grip (live).
    bx, by = GP_BKR
    selector_plate(f, b, face, bx, by, 4.2, 4.3, ["TRIP", "", "CLOSE"], "GEN BKR 52G", radius=1.25, drop=0.25, beside=True)
    f.R(bx - 0.5, by - 1.35, bx + 0.5, by - 0.85, fill=(20, 20, 22, 255))
    f.glow(bx - 0.44, by - 1.3, bx + 0.44, by - 0.9, D + 0.13, (230, 50, 40), "bkr_flag", alpha=1.0, colour2=(40, 170, 70))
    part(f, "bkr_sw", bx, by, pistol_grip(bx, by, D + 0.12), angles=BKR_ANGLES)
    for x, label, colour, ch in GP_BKR_LAMPS:
        lamp(f, b, x, by + 0.6, colour, ch, label, r=0.5)
    # Voltage and speed adjust: spring-return RAISE / LOWER switches (at rest).
    for x, y, l1, l2 in GP_ADJUST:
        selector_plate(f, b, face, x, y, 3.6, 4.1, ["LOWER", "", "RAISE"], f"{l1} {l2}", radius=1.35)
        b += wing_knob(x, y, D + 0.12)
    for x, y, positions, name, at in GP_KNOBS:
        n = len(positions)
        arc = 40
        selector_plate(f, b, face, x, y, 3.6, 3.0, positions, name, arc=arc, radius=0.95, size=6, drop=0.2, mark=0.75)
        a = arc - 2 * arc * at / max(1, n - 1)
        b += turned(wing_knob(x, y, D + 0.12, 0.7), x, y, a)
    for x0, x1 in ((1.8, 14.2), (17.8, 30.2)):
        louvre(b, x0, x1)
    return f, b


# ------------------------------------------------------------------------------------------------ the voltage regulator

# The excitation system's cabinet: the regulator's display (live), POWER, AUTO, MANUAL and FAULT lamps, the field's
# volts and amps, the AUTO / MANUAL transfer switch, the voltage setpoint switch, the FIELD FLASH lamp, and through a
# window the regulator's card rack (each card's front, its ejectors and status lights, live).
AV_SCREEN = (1.6, 22.9, 14.4, 26.9)
AV_LAMPS = [(2.8, "POWER", (40, 180, 70), "avr_power"), (6.1, "AUTO", (40, 180, 70), "avr_auto"),
            (9.4, "MANUAL", (240, 170, 30), "avr_manual"), (12.7, "FAULT", (230, 50, 40), "avr_fault")]
AV_LAMP_Y = 21.1
AV_METERS = [(1.4, "FIELD VOLTS", (0, 250), "V", "field_v"), (7.8, "FIELD AMPS", (0, 150), "A", "field_a")]
AV_METER = (5.6, 14.3, 18.9)
AV_WINDOW = (2.0, 4.3, 14.0, 9.5)
AV_CARDS = ["PS", "REG", "LIMIT", "SCR A", "SCR B", "I/O"]
CARD_Z = (10.6, 13.6)                         # a card's front: behind the glass (z), its face


def card_rack(f, b, face, win, cards, prefix):
    """A card rack seen through a door's window: the rack's back and rails, each card's front panel (its name, its
    status light live, the ejector handles top and bottom)."""
    wx0, wy0, wx1, wy1 = win
    back = CARD_Z[0]
    fz = CARD_Z[1]
    f.R(wx0, wy0, wx1, wy1, fill=(40, 42, 46, 255))
    n = len(cards)
    pitch = (wx1 - wx0 - 0.6) / n
    b.append((wx0, wy0, back - 0.3, wx1, wy1, back, {"*": "pi_cabinet_dark", "south": f"pi_{prefix}_f"}))
    b += [(wx0 + 0.05, wy0 + 0.25, back, wx1 - 0.05, wy0 + 0.55, fz - 0.4, "pi_chrome"),
          (wx0 + 0.05, wy1 - 0.55, back, wx1 - 0.05, wy1 - 0.25, fz - 0.4, "pi_chrome")]
    for i, name in enumerate(cards):
        x0 = wx0 + 0.3 + i * pitch + 0.05
        x1 = x0 + pitch - 0.1
        cy0, cy1 = wy0 + 0.6, wy1 - 0.6
        f.R(x0, cy0, x1, cy1, fill=(58, 62, 70, 255))
        f.R(x0 + 0.1, cy1 - 0.9, x1 - 0.1, cy1 - 0.25, fill=(222, 222, 216, 255))
        _fit(f, (x0 + x1) / 2, cy1 - 0.57, name, 6, x1 - x0 - 0.2)
        f.R(x0 + 0.25, cy0 + 0.9, x1 - 0.25, cy0 + 1.0, fill=(150, 152, 150, 255))
        for k in range(3):
            f.E((x0 + x1) / 2, cy1 - 1.5 - k * 0.6, 0.11, fill=(30, 50, 34, 255))
        b.append((x0, cy0, back, x1, cy1, fz, {"*": "pi_sb_frame", "south": f"pi_{prefix}_f"}))
        b += [(x0 + 0.1, cy1 - 0.1, fz, x1 - 0.1, cy1 + 0.25, fz + 0.35, "pi_cap_red" if i % 3 == 0 else BLK),
              (x0 + 0.1, cy0 - 0.25, fz, x1 - 0.1, cy0 + 0.1, fz + 0.35, "pi_cap_red" if i % 3 == 0 else BLK)]
        for k, colour in enumerate(((60, 230, 90), (60, 230, 90), (240, 170, 30))):
            f.live.append({"t": "lamp", "x": (x0 + x1) / 2, "y": cy1 - 1.5 - k * 0.6, "r": 0.12, "z": fz + 0.02, "colour": list(colour),
                           "ch": f"{prefix}_led{i}_{k}"})


def voltage_regulator():
    f = Front(1, 403)
    F = f"pi_{AVR}_f"
    face = {"*": BLK, "south": F}
    b = shell(F, windows=[AV_WINDOW])
    door_seam(f)
    nameplate(f, b, 2.0, 27.75, 14.0, 30.0, "EXCITATION SYSTEM", "AUTOMATIC VOLTAGE REGULATOR", face)
    sx0, sy0, sx1, sy1 = AV_SCREEN
    f.R(sx0, sy0, sx1, sy1, fill=(26, 28, 30, 255))
    f.R(sx0 + 0.35, sy0 + 0.35, sx1 - 0.35, sy1 - 0.35, fill=(12, 30, 20, 255))
    f.text(sx0 + 0.75, sy1 - 1.2, D + 0.2, 0.62, (110, 240, 140), "avr1")
    f.text(sx0 + 0.75, sy0 + 1.2, D + 0.2, 0.62, (110, 240, 140), "avr2")
    b += [(sx0, sy0, D, sx0 + 0.35, sy1, D + 0.4, BLK), (sx1 - 0.35, sy0, D, sx1, sy1, D + 0.4, BLK),
          (sx0 + 0.35, sy0, D, sx1 - 0.35, sy0 + 0.35, D + 0.4, BLK), (sx0 + 0.35, sy1 - 0.35, D, sx1 - 0.35, sy1, D + 0.4, BLK),
          (sx0 + 0.35, sy0 + 0.35, D, sx1 - 0.35, sy1 - 0.35, D + 0.1, face)]
    for x, label, colour, ch in AV_LAMPS:
        lamp(f, b, x, AV_LAMP_Y, colour, ch, label)
    w, y0, y1 = AV_METER
    for x0, label, scale, unit, ch in AV_METERS:
        meter(f, b, face, x0, y0, x0 + w, y1, label, scale, unit, ch)
    selector_plate(f, b, face, 3.2, 11.5, 3.4, 3.0, ["AUTO", "MAN"], "REGULATOR", arc=40, radius=0.95, size=6, drop=0.2, mark=0.75)
    b += turned(wing_knob(3.2, 11.5, D + 0.12, 0.7), 3.2, 11.5, 40)
    selector_plate(f, b, face, 7.6, 11.5, 3.6, 3.0, ["LOWER", "", "RAISE"], "VOLT SETPOINT", arc=40, radius=0.95, size=6, drop=0.2, mark=0.75)
    b += wing_knob(7.6, 11.5, D + 0.12, 0.7)
    lamp(f, b, 11.4, 11.6, (240, 240, 230), "avr_flash", "FIELD|FLASH")
    card_rack(f, b, face, AV_WINDOW, AV_CARDS, AVR)
    louvre(b, 1.8, 14.2)
    return f, b


# ------------------------------------------------------------------------------------------------ the protection relays

# The generator protection relay panel (a fixed front): eight electromechanical relays in their drawout cases (black
# frame, clear cover, the relay inside: its nameplate, time dial, induction disc, target), each with its test switch
# under it; the 86G lockout relay with its handle and target, its 125 VDC and TRIP CIRCUIT lamps. Clicking a relay's
# test switch tests it: its target drops and it trips the 86G (Justin: "Handle and relay tests"); clicking the relay
# resets its target. The 86G's handle trips it or resets it.
PR_RELAYS = [("87G", "GEN DIFF"), ("51V", "VOLT RESTR O/C"), ("32", "REVERSE POWER"), ("40", "LOSS OF FIELD"),
             ("46", "NEG SEQUENCE"), ("81", "O/U FREQUENCY"), ("59", "OVERVOLTAGE"), ("64G", "STATOR GROUND")]
PR_COLS = (1.4, 8.8)                          # a relay case's left side, by column
PR_W, PR_H = 5.8, 3.7                         # a case's width and height
PR_TOPS = (27.3, 22.5, 17.7, 12.9)            # each row's case top
PR_TS = 0.85                                  # a test switch's height (under its case, 0.15 down)
PR_LOR = (8.0, 6.05)                          # the 86G's hub
PR_LAMPS = [(3.0, "125 VDC", (240, 240, 230), "pr_dc"), (13.0, "TRIP CKT", (240, 240, 230), "pr_trip_ckt")]
LOR_ANGLES = [45, -45]                        # RESET, TRIPPED


def relay_cells():
    """Each relay's case and its test switch: (x0, y0, x1, y1, ts_y0, ts_y1)."""
    out = []
    for i in range(len(PR_RELAYS)):
        c, r = i % 2, i // 2
        x0, y1 = PR_COLS[c], PR_TOPS[r]
        y0 = y1 - PR_H
        out.append((x0, y0, x0 + PR_W, y1, y0 - 0.15 - PR_TS, y0 - 0.15))
    return out


def protection_panel():
    f = Front(1, 404)
    F = f"pi_{PROTECTION}_f"
    face = {"*": BLK, "south": F}
    b = shell(F, door=False)
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    nameplate(f, b, 2.0, 27.75, 14.0, 30.0, "GENERATOR PROTECTION", "RELAYS   52G   86G", face)
    for i, ((num, name), (x0, y0, x1, y1, t0, t1)) in enumerate(zip(PR_RELAYS, relay_cells())):
        rim = 0.3
        ix0, iy0, ix1, iy1 = x0 + rim, y0 + rim, x1 - rim, y1 - rim
        fz = D + 0.25
        # The relay's face: its plate (number and name), the time dial, the disc's slot, the target's window.
        f.R(x0, y0, x1, y1, fill=(26, 26, 28, 255))
        f.R(ix0, iy0, ix1, iy1, fill=(214, 210, 192, 255))
        f.R(ix0 + 0.15, iy1 - 0.95, ix1 - 0.15, iy1 - 0.15, fill=BLACK)
        _fit(f, (ix0 + ix1) / 2, iy1 - 0.4, f"{num}  {name}", 6.5, ix1 - ix0 - 0.4, fill=WHITE)
        dx, dy = ix1 - 0.95, iy0 + 1.05
        f.E(dx, dy, 0.6, fill=(240, 240, 234, 255), outline=(60, 60, 60, 255), width=2)
        for k in range(11):
            a = math.radians(210 - 24 * k)
            f.line([(dx + math.cos(a) * 0.42, dy + math.sin(a) * 0.42), (dx + math.cos(a) * 0.56, dy + math.sin(a) * 0.56)], INK, 1)
        f.line([(dx, dy), (dx + math.cos(math.radians(210 - 24 * (2 + i % 5))) * 0.4, dy + math.sin(math.radians(210 - 24 * (2 + i % 5))) * 0.4)],
               (190, 30, 26, 255), 2)
        _fit(f, dx, dy - 0.82, "TIME DIAL", 5.5, 1.6)
        sx = (ix0 + ix1) / 2 - 0.5
        f.R(sx - 1.3, iy0 + 1.6, sx + 1.3, iy0 + 1.78, fill=(20, 20, 22, 255))
        tx0, ty0, tx1, ty1 = ix0 + 0.3, iy0 + 0.25, ix0 + 2.1, iy0 + 0.95
        f.R(tx0 - 0.1, ty0 - 0.1, tx1 + 0.1, ty1 + 0.1, fill=(30, 30, 32, 255))
        f.R(tx0, ty0, tx1, ty1, fill=(40, 40, 42, 255))
        f.T((tx0 + tx1) / 2, (ty0 + ty1) / 2, "TARGET", 6, fill=(150, 150, 146, 255), bold=True)
        f.live.append({"t": "window", "x0": tx0, "y0": ty0, "x1": tx1, "y1": ty1, "z": fz + 0.01, "colour": list(ORANGE), "ch": f"pr_target{i}",
                       "alpha": 1.0, "label": ["TRIP"]})
        # The case: its frame, the relay's face set back, the clear cover; the time dial's ring, the disc out of its
        # slot, the target's bezel; the cover's latches and the target reset rod.
        b += [(x0, y0, D, ix0, y1, D + 0.9, BLK), (ix1, y0, D, x1, y1, D + 0.9, BLK),
              (ix0, y0, D, ix1, iy0, D + 0.9, BLK), (ix0, iy1, D, ix1, y1, D + 0.9, BLK),
              (ix0, iy0, D, ix1, iy1, fz, face),
              (ix0, iy0, D + 0.78, ix1, iy1, D + 0.83, {"south": GLASS, "north": GLASS})]
        b += [bx[:6] + ({"*": "pi_chrome", "south": F},) for bx in _sc_round(dx, dy, fz, fz + 0.15, 0.6, None)]
        b += [(sx - 1.2, iy0 + 1.6, fz, sx + 1.2, iy0 + 1.72, fz + 0.32, "pi_chrome"),
              (sx - 0.9, iy0 + 1.72, fz, sx - 0.4, iy0 + 2.0, fz + 0.4, BLK), (sx - 0.9, iy0 + 1.3, fz, sx - 0.4, iy0 + 1.6, fz + 0.4, BLK),
              (tx0 - 0.1, ty0 - 0.1, fz, tx0, ty1 + 0.1, fz + 0.2, BLK), (tx1, ty0 - 0.1, fz, tx1 + 0.1, ty1 + 0.1, fz + 0.2, BLK),
              (x0 + 0.06, y1 - 0.75, D + 0.9, x0 + 0.24, y1 - 0.25, D + 1.0, "pi_chrome"),
              (x1 - 0.24, y1 - 0.75, D + 0.9, x1 - 0.06, y1 - 0.25, D + 1.0, "pi_chrome"),
              (x0 + 1.0, y0 + 0.06, D + 0.9, x0 + 1.3, y0 + 0.24, D + 1.15, "pi_chrome")]
        # The test switch: its black base, a nameplate on its left end, six levers (the two trip circuit's red).
        f.R(x0 + 0.1, t0, x1 - 0.1, t1, fill=(36, 38, 42, 255))
        f.R(x0 + 0.2, t0 + 0.12, x0 + 1.6, t1 - 0.12, fill=(236, 236, 230, 255))
        _fit(f, x0 + 0.9, (t0 + t1) / 2, f"TS-{num}", 5.5, 1.3)
        b.append((x0 + 0.1, t0, D, x1 - 0.1, t1, D + 0.35, face))
        for k in range(6):
            xs = x0 + 1.95 + k * 0.62
            b += [(xs - 0.1, t0 + 0.15, D + 0.35, xs + 0.1, t1 - 0.15, D + 0.5, "pi_chrome"),
                  (xs - 0.17, t1 - 0.5, D + 0.5, xs + 0.17, t1 - 0.12, D + 0.85, "pi_pnl_red" if k in (0, 5) else BLK)]
    # The 86G lockout relay: its plate (RESET, TRIP, the target), the handle (live).
    lx, ly = PR_LOR
    f.R(lx - 1.9, ly - 2.05, lx + 1.9, ly + 1.95, fill=BLACK)
    f.E(lx, ly, 1.25, fill=(222, 222, 216, 255))
    f.E(lx, ly, 0.85, fill=(200, 200, 196, 255))
    for k, (a, col) in enumerate(((135, INK), (45, (190, 30, 26, 255)))):
        aa = math.radians(a)
        f.line([(lx + math.cos(aa) * 0.9, ly + math.sin(aa) * 0.9), (lx + math.cos(aa) * 1.22, ly + math.sin(aa) * 1.22)], col, 4)
    f.T(lx - 1.1, ly + 1.55, "RESET", 6, fill=WHITE, bold=True)
    f.T(lx + 1.2, ly + 1.55, "TRIP", 6, fill=(240, 90, 70, 255), bold=True)
    _fit(f, lx, ly - 1.72, "86G LOCKOUT", 6.5, 3.6, fill=WHITE)
    # The target window right of the handle's boss (under it, the chrome boss would cover it).
    f.R(lx + 1.3, ly - 1.22, lx + 1.84, ly - 0.84, fill=(20, 20, 22, 255))
    f.glow(lx + 1.34, ly - 1.18, lx + 1.8, ly - 0.88, D + 0.13, ORANGE, "lor_target", alpha=1.0)
    b.append((lx - 1.9, ly - 2.05, D, lx + 1.9, ly + 1.95, D + 0.12, face))
    b += [bx[:6] + ({"*": "pi_chrome", "south": F},) for bx in _sc_round(lx, ly, D + 0.12, D + 0.3, 1.25, None)]
    part(f, "lor", lx, ly, pistol_grip(lx, ly, D + 0.3, 1.15), angles=LOR_ANGLES)
    for x, label, colour, ch in PR_LAMPS:
        lamp(f, b, x, ly + 0.4, colour, ch, label)
    louvre(b, 1.8, 14.2)
    return f, b


# ------------------------------------------------------------------------------------------------ the load sequencer

# The load sequencer: its screen (the steps it runs after the breaker closes, each lit as it starts and done), its
# STEP lamps, through a window its controller's cards (their lights live), MODE (NORMAL, TEST) and RESET.
SQ_SCREEN = (1.6, 19.3, 14.4, 27.0)
SQ_STEPS = [("1", "4160V SAFETY BUS", "0"), ("2", "SAFETY INJ PUMP", "5"), ("3", "RHR PUMP", "10"), ("4", "COMP COOL PUMP", "15"),
            ("5", "SERVICE WTR PUMP", "20"), ("6", "CONT SPRAY PUMP", "25"), ("7", "HVAC FANS", "30"), ("8", "BATT CHARGERS", "35")]
SQ_LAMP_Y = 17.35
SQ_WINDOW = (2.0, 7.0, 13.4, 14.6)
SQ_CARDS = ["PS", "CPU", "DI 1", "DI 2", "DO 1", "DO 2", "COMM"]


def load_sequencer():
    f = Front(1, 405)
    F = f"pi_{SEQUENCER}_f"
    face = {"*": BLK, "south": F}
    b = shell(F, windows=[SQ_WINDOW])
    door_seam(f)
    nameplate(f, b, 2.0, 27.75, 14.0, 30.0, "LOAD SEQUENCER", "4160 V SAFETY BUS LOADS", face)
    sx0, sy0, sx1, sy1 = SQ_SCREEN
    f.R(sx0, sy0, sx1, sy1, fill=(26, 28, 30, 255))
    f.R(sx0 + 0.4, sy0 + 0.4, sx1 - 0.4, sy1 - 0.4, fill=(232, 238, 244, 255))
    f.R(sx0 + 0.4, sy1 - 1.2, sx1 - 0.4, sy1 - 0.4, fill=(30, 80, 150, 255))
    f.text((sx0 + sx1) / 2, sy1 - 0.8, D + 0.2, 0.48, (255, 255, 255), "seq_head", "center")
    for i, (n, load, t) in enumerate(SQ_STEPS):
        y = sy1 - 1.75 - i * 0.74
        f.T(sx0 + 0.85, y, n, 7.5, bold=True, anchor="lm")
        f.T(sx0 + 1.65, y, load, 7.5, anchor="lm")
        f.T(sx1 - 0.85, y, f"T+{t}s", 7.5, bold=True, anchor="rm")
        f.glow(sx0 + 0.5, y - 0.34, sx1 - 0.5, y + 0.34, D + 0.2, (70, 200, 100), f"seq_row{i}", alpha=0.38, colour2=(250, 190, 50))
    b += [(sx0, sy0, D, sx0 + 0.4, sy1, D + 0.45, BLK), (sx1 - 0.4, sy0, D, sx1, sy1, D + 0.45, BLK),
          (sx0 + 0.4, sy0, D, sx1 - 0.4, sy0 + 0.4, D + 0.45, BLK), (sx0 + 0.4, sy1 - 0.4, D, sx1 - 0.4, sy1, D + 0.45, BLK),
          (sx0 + 0.4, sy0 + 0.4, D, sx1 - 0.4, sy1 - 0.4, D + 0.12, face)]
    for i in range(8):
        lamp(f, b, 2.0 + i * 1.56, SQ_LAMP_Y, (240, 240, 230), f"seq_lamp{i}", str(i + 1), r=0.42)
    _fit(f, 7.5, SQ_LAMP_Y - 1.45, "STEP", 6.5, 3.0)
    card_rack(f, b, face, SQ_WINDOW, SQ_CARDS, SEQUENCER)
    selector_plate(f, b, face, 4.0, 5.3, 3.8, 2.9, ["NORMAL", "TEST"], "MODE", arc=40, radius=0.9, size=6, drop=0.2, mark=0.75)
    b += turned(wing_knob(4.0, 5.3, D + 0.12, 0.7), 4.0, 5.3, 40)
    button(f, b, face, 10.2, 5.55, "pi_pnl_yellow", "RESET", r=0.5)
    louvre(b, 1.8, 14.2)
    return f, b


# ------------------------------------------------------------------------------------------------ the battery charger

def battery_charger():
    """The diesel's own 125 VDC battery charger: the DC rooms' station charger (plant_panels), its nameplate the
    diesel's, lettered live."""
    f = PP.station_charger("")
    f.R(2.0, 28.3, 14.0, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    f.T(8.0, 29.4, "BATTERY CHARGER  125 VDC", 10.5, fill=WHITE, bold=True)
    f.text(8.0, 28.72, D + 0.14, 0.45, (238, 238, 232), "label", "center")
    old, new = f"pi_{PP.SC_KEY}_f", f"pi_{CHARGER}_f"
    boxes = [bx[:6] + (({k: (new if v == old else v) for k, v in bx[6].items()} if isinstance(bx[6], dict) else bx[6]),) + tuple(bx[7:])
             for bx in PP.station_charger_boxes()]
    return f, boxes


# ------------------------------------------------------------------------------------------------ the battery

# The diesel's 125 VDC battery (two blocks): a two-step rack like the station battery's (plant_panels.station_battery),
# seven clear-jar cells a step, the jumper between the steps at the left end, the main cables from the terminals at the
# right end up the back and into the wall behind through a sleeve, its nameplate (lettered live) on the lower step.
BT_CELLS = 7


def battery_rack():
    B = "pi_sb_frame"
    boxes = [(0.3, 0, 0.3, 31.7, 1.0, 15.7, {"*": "pi_sb_pan", "up": "pi_sb_pan_top"}),
             (0.3, 1.0, 15.3, 31.7, 2.0, 15.7, "pi_sb_pan"), (0.3, 1.0, 0.3, 31.7, 2.0, 0.7, "pi_sb_pan"),
             (0.3, 1.0, 0.7, 0.7, 2.0, 15.3, "pi_sb_pan"), (31.3, 1.0, 0.7, 31.7, 2.0, 15.3, "pi_sb_pan")]
    lz0, lz1, ly0, ly1 = PP.SB_LOW
    hz0, hz1, hy0, hy1 = PP.SB_HIGH
    for x0 in (0.8, 15.5, 30.2):
        x1 = x0 + 1.0
        for z0 in (hz0 + 0.2, hz1 - 1.0):
            boxes.append((x0, 1.0, z0, x1, hy0 - 1.2, z0 + 0.8, B))
        for z0 in (lz0 + 0.2, lz1 - 1.0):
            boxes.append((x0, 1.0, z0, x1, ly0 - 1.2, z0 + 0.8, B))
        boxes.append((x0, hy0 - 1.2, hz0 + 1.0, x1, hy0, hz1 - 1.0, B))
        boxes.append((x0, ly0 - 1.2, lz0 + 1.0, x1, ly0, lz1 - 1.0, B))
        boxes.append((x0, 6.4, hz1 - 0.2, x1, 7.2, lz0 + 0.2, B))
    for z0 in (hz0 + 0.2, hz1 - 1.0):
        boxes.append((0.8, hy0 - 1.2, z0, 31.2, hy0, z0 + 0.8, "pi_sb_rail"))
    for z0 in (lz0 + 0.2, lz1 - 1.0):
        boxes.append((0.8, ly0 - 1.2, z0, 31.2, ly0, z0 + 0.8, "pi_sb_rail"))
    for (z0, z1, y0, y1) in (PP.SB_LOW, PP.SB_HIGH):
        ry = y0 + 6.4
        for rz0, rz1 in ((z1 + 0.1, z1 + 0.6), (z0 - 0.6, z0 - 0.1)):
            boxes.append((0.8, ry, rz0, 31.2, ry + 0.6, rz1, "pi_sb_rail"))
            for px in (0.9, 15.7, 30.5):
                boxes.append((px, y0 - 1.2 if px != 15.7 else y0, rz0 + 0.05, px + 0.6, ry, rz1 - 0.05, B))
    x_first = 2.15
    for step in (PP.SB_LOW, PP.SB_HIGH):
        for k in range(BT_CELLS):
            boxes += [bx for bx in PP.sb_cell(x_first + k * PP.SB_PITCH, step, k, k < BT_CELLS - 1)
                      if bx[6] not in ("pi_sense_module", PP.SENSE) and not (bx[6] == "pi_pnl_green" and bx[4] - bx[1] < 0.2)]
    lt, ht = ly1 + PP.SB_COVER, hy1 + PP.SB_COVER
    lzc, hzc = (lz0 + lz1) / 2, (hz0 + hz1) / 2
    cab, red = "pi_sb_cable", "pi_sb_cable_red"
    px = x_first + 0.95
    boxes += [(1.0, lt + 0.4, lzc - 0.4, px + 0.4, lt + 1.2, lzc + 0.4, cab),
              (1.0, lt + 1.2, lzc - 0.4, 1.8, ht + 0.6, lzc + 0.4, cab),
              (1.0, ht + 0.6, hzc - 0.4, 1.8, ht + 1.4, lzc + 0.4, cab),
              (1.8, ht + 0.6, hzc - 0.4, px + 0.4, ht + 1.4, hzc + 0.4, cab)]
    last = x_first + (BT_CELLS - 1) * PP.SB_PITCH + PP.SB_W - 0.95
    # The terminals: the upper step's last post (positive), the lower step's (negative); the cables back to the wall,
    # up it and into it through a sleeve (to the charger, in the wall).
    boxes += [(last - 0.5, ht + 0.3, hzc - 0.5, last + 0.5, ht + 1.0, hzc + 0.5, "pi_sb_copper"),
              (last - 0.4, ht + 1.0, hzc - 0.4, last + 0.4, ht + 1.8, hzc + 0.4, red),
              (last - 0.4, ht + 1.0, 0.0, last + 0.4, ht + 1.8, hzc - 0.4, red),
              (last - 0.5, lt + 0.3, lzc - 0.5, last + 0.5, lt + 1.0, lzc + 0.5, "pi_sb_copper"),
              (last + 0.4, lt + 0.4, lzc - 0.4, 31.0, lt + 1.2, lzc + 0.4, cab),
              (30.2, lt + 1.2, lzc - 0.4, 31.0, ht + 2.6, lzc + 0.4, cab),
              (30.2, ht + 1.8, 0.0, 31.0, ht + 2.6, lzc - 0.4, cab)]
    # The wall sleeve the cables go into (its collar on the wall, fire-stopped).
    sx0, sx1 = last - 0.9, 31.4
    boxes += [(sx0, ht + 0.6, 0.0, sx1, ht + 0.8, 0.5, "pi_steel_dark"), (sx0, ht + 2.6, 0.0, sx1, ht + 2.8, 0.5, "pi_steel_dark"),
              (sx0, ht + 0.8, 0.0, sx0 + 0.2, ht + 2.6, 0.5, "pi_steel_dark"), (sx1 - 0.2, ht + 0.8, 0.0, sx1, ht + 2.6, 0.5, "pi_steel_dark"),
              (sx0 + 0.2, ht + 0.8, 0.0, sx1 - 0.2, ht + 2.6, 0.1, "pi_firestop")]
    # The nameplate on the lower step's front rail (lettered live).
    boxes.append((2.5, 2.0, lz1 + 0.02, 14.5, 3.4, lz1 + 0.14, {"*": "pi_pnl_white", "south": f"pi_{BATTERY}_f"}))
    return boxes


def battery_front():
    """The battery's drawing: only its nameplate shows (on the lower step's rail): engraved black on white, the name
    lettered live."""
    f = Front(2, 407)
    f.R(2.5, 2.0, 14.5, 3.4, fill=(236, 236, 230, 255), outline=(60, 60, 62, 255), width=2)
    f.T(8.5, 3.0, "125 VDC DIESEL BATTERY", 7.5, fill=INK, bold=True)
    f.text(8.5, 2.38, PP.SB_LOW[1] + 0.16, 0.4, (24, 24, 26), "label", "center")
    return f, battery_rack()


# ------------------------------------------------------------------------------------------------ registering

BUILDERS = {ENGINE: engine_panel, GENERATOR: generator_panel, AVR: voltage_regulator, PROTECTION: protection_panel,
            SEQUENCER: load_sequencer, CHARGER: battery_charger, BATTERY: battery_front}
_BUILT = {}


def built(key):
    if key not in _BUILT:
        _BUILT[key] = BUILDERS[key]()
    return _BUILT[key]


def register():
    """In place of plant_panels' old drawings of these pieces: labelled by train (one drawing, the name lettered live),
    in 3D in the inventory, the battery two wide."""
    for key in BUILDERS:
        info = PP.PANELS[key]
        info.update({"labels": LABELS, "shared": True, "item3d": True, "width": 2 if key in (GENERATOR, BATTERY) else 1})
        info["multi"] = (info["width"], 2, "panel")
        PP.DRAWINGS[key] = lambda label=None, key=key: built(key)[0]
        PP.CABINETS[key] = lambda key=key: built(key)[1]
    PP.SHARP |= {f"pi_{key}_f_{p}" for key in BUILDERS for p in range(2 * PP.PANELS[key]["width"])}


register()

# The diesel rooms' fittings (round 31 part 3: the CO2 release station, horns, fans, the hoist pendant, the CO2 piping).
import diesel_fittings  # noqa: E402,F401
