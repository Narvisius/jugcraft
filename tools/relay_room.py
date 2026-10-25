"""The control building's relay room (round 26, #10; Justin: "Real cabinet types", "Unique labels", "Overhead trays and
cable entry", "Room extras", "Yes, read the reactor"): the cabinets a Westinghouse plant's relay room holds, each one
block wide and two high, their fronts drawn at 512 px a block (plant_panels' kit) with what stands out of them modelled:

- the 7300 process protection racks, four channels of three racks each, colour-coded by channel (I red, II white, III
  blue, IV yellow): two indicators and four bistable status lamps on the test panel, the card cages behind the glass;
- the solid state protection system, its two trains' input, logic and output bays: a matrix of legend windows on each
  bay's test panel (lit tripped), its universal logic cards behind the glass;
- the nuclear instrumentation system: drawers for the source range, intermediate range and power range channels and the
  comparator and rate drawers, each with its meter and lamps;
- the protective relay panels (plant_panels' relay cabinet whole over two blocks, its nameplate lettered live with its
  own name: RELAY_LABELS, one drawing for them all).

What moves on them reads the nearest reactor (client/EdgTelemetry.java relayRoom; nuclear/LivePanelBlockEntity.java).
Each kind's live parts are the same on every variant (plant_panels.layout() takes the first's): the variants differ in
what is drawn. Also the room's plain pieces: the row signs, the smoke detectors, the drawing rack, the sequence of
events recorder's printer (its trays and cables: tools/plant_office2.py, the control cable tray).

Models in model pixels for the piece facing north: x across, y up from the floor (0..32 over its two blocks), its front
+z (the doors' face at z 15.2)."""
import math
from PIL import Image, ImageDraw

import plant_panels as PP

U, INK = PP.U, PP.INK
WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
DOOR = 15.2                                # the doors' face
CHANNELS = ["I", "II", "III", "IV"]
CHANNEL_COLOURS = [((196, 36, 30, 255), WHITE), ((242, 242, 236, 255), BLACK), ((40, 84, 176, 255), WHITE), ((240, 196, 36, 255), BLACK)]
TRAIN_COLOURS = [((226, 120, 28, 255), BLACK), ((36, 128, 76, 255), WHITE)]
RED, AMBER, GREEN = [230, 40, 30], [240, 170, 30], [40, 180, 70]

# ---------------------------------------------------------------------------------------------------- the names
PROTECTION_RACKS = [(c, r) for c in range(4) for r in range(3)]
PROTECTION_LABELS = [f"CH {CHANNELS[c]} RACK {r + 1}" for c, r in PROTECTION_RACKS]
SSPS_BAYS = [(t, b) for t in range(2) for b in range(3)]
SSPS_BAY_NAMES = ["INPUT BAY", "LOGIC BAY", "OUTPUT BAY"]
SSPS_LABELS = [f"TRAIN {'AB'[t]} {SSPS_BAY_NAMES[b]}" for t, b in SSPS_BAYS]
# The nuclear instruments: each rack's group, its two drawers (tag, title, scale, unit) and their three lamps (the
# first always green, the second red, the third amber: the live parts are the same on every rack).
NIS_RACKS = [
    ("SOURCE RANGE", [("N31", "SOURCE RANGE", "SR", "CPS"), ("N32", "SOURCE RANGE", "SR", "CPS")], ["HV ON", "HIGH FLUX", "BLOCKED"]),
    ("INTERMEDIATE RANGE", [("N35", "INTERMEDIATE RANGE", "IR", "AMPS"), ("N36", "INTERMEDIATE RANGE", "IR", "AMPS")],
     ["INST POWER", "HIGH FLUX", "ROD STOP"]),
    ("POWER RANGE", [("N41", "POWER RANGE", "PR", "% PWR"), ("N42", "POWER RANGE", "PR", "% PWR")], ["INST POWER", "HIGH FLUX", "ROD STOP"]),
    ("POWER RANGE", [("N43", "POWER RANGE", "PR", "% PWR"), ("N44", "POWER RANGE", "PR", "% PWR")], ["INST POWER", "HIGH FLUX", "ROD STOP"]),
    ("COMPARATOR AND RATE", [("NC41", "CHANNEL DEVIATION", "DEV", "%"), ("NM41", "STARTUP RATE", "SUR", "DPM")],
     ["INST POWER", "DEVIATION", "POS RATE"]),
]
NIS_LABELS = [" ".join(d[0] for d in r[1]) for r in NIS_RACKS]
# The protective relay panels' names (rows A to D as tools/medium_plant.py relay_room lays them; nuclear/RelayRoom.java
# has the same list for their nameplates).
RELAY_LABELS = ["RCP 1A 1B MOTORS", "RCP 1C 1D MOTORS",
                "MAIN GENERATOR", "MAIN TRANSFORMER", "UNIT AUX XFMR",
                "4160V BUS 1A", "4160V BUS 1B", "4160V BUS 1C", "4160V BUS 1D", "EDG 1A", "EDG 1B",
                "480V LC 1A", "480V LC 1B", "STARTUP XFMR", "345KV BREAKERS", "MAIN STEAM ISOL", "FEEDWATER ISOL",
                "SI PUMPS", "RHR PUMPS", "CONT SPRAY PUMPS", "CHARGING PUMPS", "CCW PUMPS", "SERVICE WATER",
                "AFW PUMPS", "CONDENSATE PUMPS", "FEEDWATER PUMPS", "CIRC WATER PUMPS", "DIESEL SEQ A", "DIESEL SEQ B"]

# What each protection rack shows, by rack: its two indicators (title, unit, low and high end of the scale) and its
# four bistables.
PROTECTION_RACK_KINDS = [
    ([("TAVG", "DEG F", 530, 630), ("DELTA T", "% PWR", 0, 150)], ["OT DELTA T", "OP DELTA T", "TAVG LO-LO", "HIGH FLUX"]),
    ([("PZR PRESSURE", "PSIG", 1700, 2500), ("PZR LEVEL", "%", 0, 100)], ["PZR P LO", "PZR P HI", "PZR LVL HI", "PZR P LO SI"]),
    ([("SG LEVEL NR", "%", 0, 100), ("RCS FLOW", "%", 0, 120)], ["SG LVL LO-LO", "SG LVL HI-HI", "RCS FLOW LO", "RCP UNDERFREQ"]),
]
# The SSPS bays' windows (4 by 4, row by row from the top: two lines each), their left lamp's name (amber; the right
# one is POWER ON, green) and the heading over the windows.
SSPS_INPUTS = ["PZR P LO", "OT DELTA T", "SG LO-LO", "FLOW LO"]
SSPS_WINDOWS = [
    ([f"{fn}|CH {ch}" for fn in SSPS_INPUTS for ch in CHANNELS], "INPUT ERROR", "INPUT RELAYS  -  LIT TRIPPED"),
    (["P-4|RTB OPEN", "P-6|IR ABOVE", "P-7|AT POWER", "P-8|ABOVE 48%", "P-10|PR ABOVE", "P-11|PZR P", "P-12|TAVG LO-LO", "P-13|TURB IMP",
      "REACTOR|TRIP", "SAFETY|INJECTION", "FW|ISOLATION", "STM LINE|ISOLATION", "CONT|ISOLATION", "CONT|SPRAY", "AFW|START",
      "TURBINE|TRIP"], "GENERAL WARNING", "PERMISSIVES AND ACTUATION LOGIC"),
    (["K601|RTB UV", "K602|SI", "K603|SI", "K604|FW ISOL", "K605|FWP TRIP", "K606|MSIV", "K607|CONT ISO A", "K608|CONT ISO B",
      "K609|CS PUMPS", "K610|AFW MDP", "K611|AFW TDP", "K612|TURB TRIP", "K613|CR VENT", "K614|EDG START", "K615|RTB SHUNT",
      "K616|SPARE"], "OUTPUT TEST", "MASTER AND SLAVE RELAYS  -  LIT ENERGIZED"),
]

# --------------------------------------------------------------------------------------------------- the shell
# Every cabinet's sides, back, plinth and cap, as the relay cabinet's (plant_panels relay_cabinet_boxes), over both
# blocks; its control cables come down into the middle of its cap (tools/plant_office2.py control_cable_entry).


def _shell():
    # (The sides and back between the plinth and the cap, so no two of their ends lie in one plane: tools/zfight.py.)
    return [(1.0, 1.2, 2.0, 1.6, 31.4, 14.6, "pi_cabinet"), (14.4, 1.2, 2.0, 15.0, 31.4, 14.6, "pi_cabinet"),
            (1.6, 1.2, 2.0, 14.4, 31.4, 2.6, "pi_cabinet"),
            (0.8, 0, 1.8, 15.2, 1.2, 15.2, "pi_cabinet_dark"), (0.8, 31.4, 1.8, 15.2, 32, 15.4, "pi_cabinet_dark")]


def _door(F, window):
    """A door at DOOR, its window (x0, y0, x1, y1) in it: the stiles and rails (the drawing on their faces), the glass
    behind a black gasket, the hinges, the handle and its lock."""
    wx0, wy0, wx1, wy1 = window
    front = {"*": "pi_cabinet", "south": F}
    gz = DOOR - 0.4
    return [(1.0, 1.2, 14.6, wx0, 31.4, DOOR, front), (wx1, 1.2, 14.6, 15.0, 31.4, DOOR, front),
            (wx0, 1.2, 14.6, wx1, wy0, DOOR, front), (wx0, wy1, 14.6, wx1, 31.4, DOOR, front),
            (wx0, wy0, gz, wx1, wy1, gz + 0.1, {"south": PP.RC_GLASS, "north": PP.RC_GLASS}),
            (wx0, wy0, gz + 0.1, wx0 + 0.2, wy1, DOOR - 0.1, "pi_pnl_black"), (wx1 - 0.2, wy0, gz + 0.1, wx1, wy1, DOOR - 0.1, "pi_pnl_black"),
            (wx0 + 0.2, wy0, gz + 0.1, wx1 - 0.2, wy0 + 0.2, DOOR - 0.1, "pi_pnl_black"),
            (wx0 + 0.2, wy1 - 0.2, gz + 0.1, wx1 - 0.2, wy1, DOOR - 0.1, "pi_pnl_black"),
            (0.6, 5.0, 14.7, 1.0, 7.0, DOOR - 0.1, "pi_chrome"), (0.6, 25.0, 14.7, 1.0, 27.0, DOOR - 0.1, "pi_chrome"),
            (14.05, 12.8, DOOR, 14.85, 17.6, DOOR + 0.15, "pi_pnl_black"), (14.2, 13.4, DOOR + 0.15, 14.7, 16.8, DOOR + 0.6, "pi_chrome"),
            (14.3, 17.0, DOOR + 0.15, 14.6, 17.35, DOOR + 0.3, "pi_brass")]


def _meter_case(x0, y0, x1, y1, face, D=DOOR):
    """A switchboard meter's black case standing out of the panel, its face set back in it behind a glass."""
    blk = "pi_pnl_black"
    xm = (x0 + x1) / 2
    return [(x0, y0, D, x0 + 0.35, y1, D + 0.6, blk), (x1 - 0.35, y0, D, x1, y1, D + 0.6, blk),
            (x0 + 0.35, y0, D, x1 - 0.35, y0 + 0.35, D + 0.6, blk), (x0 + 0.35, y1 - 0.35, D, x1 - 0.35, y1, D + 0.6, blk),
            (x0 + 0.35, y0 + 0.35, D, x1 - 0.35, y1 - 0.35, D + 0.12, face),
            (x0 + 0.35, y0 + 0.35, D + 0.48, x1 - 0.35, y1 - 0.35, D + 0.52, {"south": PP.SC_GLASS, "north": PP.SC_GLASS}),
            (xm - 0.22, y0 + 0.05, D + 0.6, xm + 0.22, y0 + 0.3, D + 0.72, "pi_cap_black")]


def _lamp(x, y, lens, D=DOOR, r=0.5):
    """A pilot light: its chrome ring and the lens in it (lit live: the lamp's disc at the lens's face, D + 0.48)."""
    return PP._sc_round(x, y, D, D + 0.28, r + 0.18, "pi_chrome") + PP._sc_round(x, y, D + 0.28, D + 0.48, r, lens)


def _live_lamp(f, x, y, r, colour, ch, D=DOOR):
    f.E(x, y, r + 0.18, fill=(40, 40, 42, 255))
    f.E(x, y, r, fill=tuple(int(c * 0.35) for c in colour) + (255,))
    f.live.append({"t": "lamp", "x": x, "y": y, "r": r, "z": D + 0.48, "colour": list(colour), "ch": ch})


def _fit(f, cx, cy, text, size, max_w, **kw):
    """Lettering brought down half a size at a time (no smaller than 6) until it fits max_w model pixels."""
    while size > 6 and f.d.textlength(text, font=PP.cfont(size, True)) > max_w * U:
        size -= 0.5
    f.T(cx, cy, text, size, bold=True, **kw)


def _band(f, colours, line1, line2, y0=29.6, y1=31.2):
    """The coloured band across the top: the cabinet's name in two lines."""
    bg, fg = colours
    f.R(1.6, y0, 14.4, y1, fill=bg, outline=(60, 60, 62, 255), width=2)
    _fit(f, 8.0, y1 - 0.5, line1, 9, 12.4, fill=fg)
    _fit(f, 8.0, y0 + 0.45, line2, 7, 12.4, fill=fg)


def _quarter_meter(f, x0, y0, x1, y1, title, unit, ticks, live, z):
    """A switchboard meter (white face, quarter arc, the needle live from 0 to 100 of its scale): ticks are (label,
    fraction of the scale) at its major marks."""
    f.R(x0, y0, x1, y1, fill=(28, 28, 30, 255))
    f.R(x0 + 0.35, y0 + 0.35, x1 - 0.35, y1 - 0.35, fill=(244, 244, 238, 255))
    cx, cy, r = x0 + 0.65, y0 + 0.65, min(x1 - x0, y1 - y0) - 1.3
    for k in range(21):
        a = math.radians(90 - 90 * k / 20)
        r0 = r * (0.86 if k % 5 == 0 else 0.92)
        f.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r, cy + math.sin(a) * r)], INK, 2)
    for label, frac in ticks:
        a = math.radians(90 - 90 * frac)
        if frac == 0:
            # The scale's low end, at the top over the pivot: lettered from there to the right, clear of the case.
            f.T(cx + 0.2, cy + r * 0.68, label, 7, bold=True, anchor="lm")
        else:
            f.T(cx + math.cos(a) * r * 0.68, cy + math.sin(a) * r * 0.68 + (0.3 if frac == 1 else 0), label, 7, bold=True)
    f.E(cx, cy, 0.16, fill=(34, 34, 34, 255))
    f.T(x1 - 0.55, y1 - 0.85, unit, 7, bold=True, anchor="rm")
    if title:
        f.T((x0 + x1) / 2, y0 - 0.5, title, 7, bold=True)
    f.live.append({"t": "meter", "x": cx, "y": cy, "r": r * 0.95, "z": z, "lo": 0, "hi": 100, "ch": live})


def _ticks(lo, hi):
    return [(f"{lo:g}", 0.0), (f"{(lo + hi) / 2:g}", 0.5), (f"{hi:g}", 1.0)]


# ------------------------------------------------------------------------------------------- the card cages
# Behind each glass door's lower window: two card frames on the cabinet's mounting plate, each a row of printed circuit
# cards standing out of it, every card's front panel with its name strip and test points, its ejector and status light.
CAGE_WINDOW = (2.1, 2.6, 13.9, 14.6)
CAGE_FRAMES = [(8.8, 14.0), (2.9, 8.1)]
CAGE_Z = 9.0


def _cards(n):
    pitch = (13.3 - 2.7) / n
    return [(2.7 + k * pitch + 0.2, 2.7 + (k + 1) * pitch - 0.2) for k in range(n)]


def _card_cage(F, n=11, led="pi_pnl_green"):
    b = [(1.6, 1.2, CAGE_Z - 0.6, 14.4, 15.6, CAGE_Z, {"*": "pi_cabinet", "south": F})]
    for y0, y1 in CAGE_FRAMES:
        b += [(2.3, y0, CAGE_Z, 13.7, y0 + 0.4, CAGE_Z + 3.4, "pi_pnl_black"), (2.3, y1 - 0.4, CAGE_Z, 13.7, y1, CAGE_Z + 3.4, "pi_pnl_black"),
              (2.3, y0 + 0.4, CAGE_Z, 2.7, y1 - 0.4, CAGE_Z + 3.4, "pi_pnl_black"), (13.3, y0 + 0.4, CAGE_Z, 13.7, y1 - 0.4, CAGE_Z + 3.4, "pi_pnl_black")]
        for k, (x0, x1) in enumerate(_cards(n)):
            xm = (x0 + x1) / 2
            b += [(x0, y0 + 0.45, CAGE_Z, x1, y1 - 0.45, CAGE_Z + 3.0, {"*": "pi_relay_card", "south": F}),
                  (x0 + 0.04, y1 - 1.25, CAGE_Z + 3.0, x1 - 0.04, y1 - 0.6, CAGE_Z + 3.35, "pi_cab_white"),
                  (xm - 0.12, y0 + 1.5, CAGE_Z + 3.0, xm + 0.12, y0 + 1.74, CAGE_Z + 3.15, led if k % 5 else "pi_pnl_yellow")]
    return b


def _cage_drawing(f, n=11):
    """The card cages as the glass shows them: the mounting plate, each card's front panel."""
    f.R(1.6, 1.2, 14.4, 15.6, fill=(212, 214, 210, 255))
    for y0, y1 in CAGE_FRAMES:
        f.R(2.3, y0, 13.7, y1, fill=(30, 30, 32, 255))
        for x0, x1 in _cards(n):
            xm = (x0 + x1) / 2
            f.R(x0, y0 + 0.45, x1, y1 - 0.45, fill=(52, 54, 58, 255))
            f.R(x0 + 0.06, y0 + 2.3, x1 - 0.06, y0 + 2.75, fill=(236, 236, 230, 255))
            for ty in (y0 + 3.2, y0 + 3.6):
                f.E(xm, ty, 0.09, fill=(206, 166, 64, 255))


# ------------------------------------------------------------------------------------------ the protection racks
PR_KEY = "protection_rack"
PR_METERS = [(1.9, 22.4, 7.6, 28.0), (8.4, 22.4, 14.1, 28.0)]
PR_LAMP_Y, PR_LAMPS_X = 19.55, (3.0, 6.3, 9.7, 13.0)
PR_POWER = (2.4, 16.75)
PR_TEST = (12.9, 16.75)


def protection_rack(label="CH I RACK 1"):
    v = PROTECTION_LABELS.index(label)
    c, r = PROTECTION_RACKS[v]
    meters, bistables = PROTECTION_RACK_KINDS[r]
    f = PP.Front(1, 600 + v)
    f.R(1.0, 1.2, 15.0, 31.4, outline=(130, 132, 128, 255), width=3)
    f.R(CAGE_WINDOW[0] - 0.12, CAGE_WINDOW[1] - 0.12, CAGE_WINDOW[2] + 0.12, CAGE_WINDOW[3] + 0.12, outline=(150, 152, 148, 255), width=2)
    _band(f, CHANNEL_COLOURS[c], f"PROTECTION CHANNEL {CHANNELS[c]}", f"7300 PROCESS PROTECTION  -  RACK {r + 1}")
    # The test panel: its indicators, the bistables' lamps (lit tripped), power and the channel test switch.
    f.R(1.6, 15.8, 14.4, 29.2, fill=(206, 208, 202, 255))
    for (x0, y0, x1, y1), (title, unit, lo, hi), ch in zip(PR_METERS, meters, ("rr_m0", "rr_m1")):
        _quarter_meter(f, x0, y0, x1, y1, title, unit, _ticks(lo, hi), ch, DOOR + 0.2)
    f.T(8.0, PR_LAMP_Y + 1.3, "BISTABLES  -  LIT TRIPPED", 7, bold=True)
    for k, (x, name) in enumerate(zip(PR_LAMPS_X, bistables)):
        words = name.split(" ", 1) if len(name) > 8 else [name]
        for i, w in enumerate(words):
            f.T(x, PR_LAMP_Y - 1.05 - i * 0.5, w, 7, bold=True)
        _live_lamp(f, x, PR_LAMP_Y, 0.45, RED, f"rr_l{k}")
    _live_lamp(f, PR_POWER[0], PR_POWER[1], 0.42, GREEN, "rr_l4")
    f.T(PR_POWER[0] + 0.8, PR_POWER[1], "POWER", 7, bold=True, anchor="lm")
    f.T(PR_TEST[0] - 0.85, PR_TEST[1], "CH TEST", 7, bold=True, anchor="rm")
    f.R(PR_TEST[0] - 0.5, PR_TEST[1] - 0.7, PR_TEST[0] + 0.5, PR_TEST[1] + 0.7, fill=(186, 188, 190, 255))
    _cage_drawing(f)
    return f


def protection_rack_boxes():
    F = f"pi_{PR_KEY}_f"
    face = {"*": "pi_pnl_black", "south": F}
    b = _shell() + _door(F, CAGE_WINDOW) + _card_cage(F)
    b.append((1.6, 29.6, DOOR, 14.4, 31.2, DOOR + 0.12, face))
    for x0, y0, x1, y1 in PR_METERS:
        b += _meter_case(x0, y0, x1, y1, face)
    for x in PR_LAMPS_X:
        b += _lamp(x, PR_LAMP_Y, "pi_lens_red", r=0.45)
    b += _lamp(PR_POWER[0], PR_POWER[1], "pi_lens_green", r=0.42)
    # The channel test switch: its plate, the toggle down at NORMAL.
    tx, ty = PR_TEST
    b += [(tx - 0.5, ty - 0.7, DOOR, tx + 0.5, ty + 0.7, DOOR + 0.12, {"*": "pi_chrome", "south": F}),
          (tx - 0.12, ty - 0.5, DOOR + 0.12, tx + 0.12, ty, DOOR + 0.7, "pi_chrome"),
          (tx - 0.18, ty - 0.75, DOOR + 0.5, tx + 0.18, ty - 0.45, DOOR + 0.85, "pi_cap_black")]
    return b


# ---------------------------------------------------------------------------------------------- the SSPS bays
SS_KEY = "ssps_cabinet"
SS_GRID = (1.8, 19.45, 14.2, 27.6)                 # the window block's frame
SS_COLS = [(2.0 + k * 3.05, 2.0 + k * 3.05 + 2.85) for k in range(4)]
SS_ROWS = [(27.4 - (k + 1) * 2.0 + 0.25, 27.4 - k * 2.0) for k in range(4)]
SS_LAMPS = [(3.2, 17.6, AMBER, "pi_lens_amber"), (12.8, 17.6, GREEN, "pi_lens_green")]
SS_KNOB = (8.0, 17.35)


def ssps_cabinet(label="TRAIN A INPUT BAY"):
    v = SSPS_LABELS.index(label)
    t, bay = SSPS_BAYS[v]
    windows, warn, header = SSPS_WINDOWS[bay]
    f = PP.Front(1, 620 + v)
    f.R(1.0, 1.2, 15.0, 31.4, outline=(130, 132, 128, 255), width=3)
    f.R(CAGE_WINDOW[0] - 0.12, CAGE_WINDOW[1] - 0.12, CAGE_WINDOW[2] + 0.12, CAGE_WINDOW[3] + 0.12, outline=(150, 152, 148, 255), width=2)
    _band(f, TRAIN_COLOURS[t], f"SSPS TRAIN {'AB'[t]}  -  {SSPS_BAY_NAMES[bay]}", "SOLID STATE PROTECTION SYSTEM")
    f.R(1.6, 15.8, 14.4, 29.2, fill=(206, 208, 202, 255))
    _fit(f, 8.0, 28.4, header, 7, 12.4)
    gx0, gy0, gx1, gy1 = SS_GRID
    f.R(gx0, gy0, gx1, gy1, fill=(30, 30, 32, 255))
    for k, text in enumerate(windows):
        x0, x1 = SS_COLS[k % 4]
        y0, y1 = SS_ROWS[k // 4]
        f.R(x0, y0, x1, y1, fill=(232, 232, 222, 255))
        lines = text.split("|")
        for i, w in enumerate(lines):
            _fit(f, (x0 + x1) / 2, (y0 + y1) / 2 + (len(lines) - 1) * 0.3 - i * 0.6, w, 7, x1 - x0 - 0.25)
        # Lit, the window glows amber over its legend (alpha: the legend shows through).
        f.live.append({"t": "window", "x0": x0, "y0": y0, "x1": x1, "y1": y1, "z": DOOR + 0.35, "colour": [255, 190, 70], "ch": f"rr_l{k}",
                       "alpha": 0.55})
    for (x, y, colour, _), name, ch in zip(SS_LAMPS, (warn, "POWER ON"), ("rr_l16", "rr_l17")):
        _live_lamp(f, x, y, 0.6, colour, ch)
        _fit(f, x, y - 1.3, name, 7, 4.6)
    kx, ky = SS_KNOB
    f.R(kx - 0.7, ky - 0.7, kx + 0.7, ky + 0.7, fill=(186, 188, 190, 255))
    f.T(kx - 1.0, ky + 1.05, "OFF", 6.5, bold=True)
    f.T(kx + 1.0, ky + 1.05, "TEST", 6.5, bold=True)
    f.T(kx, ky - 1.3, "LOGIC TEST", 7, bold=True)
    _cage_drawing(f, 12)
    return f


def ssps_cabinet_boxes():
    F = f"pi_{SS_KEY}_f"
    face = {"*": "pi_pnl_black", "south": F}
    b = _shell() + _door(F, CAGE_WINDOW) + _card_cage(F, 12, "pi_pnl_red")
    b.append((1.6, 29.6, DOOR, 14.4, 31.2, DOOR + 0.12, face))
    gx0, gy0, gx1, gy1 = SS_GRID
    b.append((gx0, gy0, DOOR, gx1, gy1, DOOR + 0.15, face))
    for k in range(16):
        x0, x1 = SS_COLS[k % 4]
        y0, y1 = SS_ROWS[k // 4]
        b.append((x0, y0, DOOR + 0.15, x1, y1, DOOR + 0.35, {"*": "pi_pnl_white", "south": F}))
    for x, y, _, lens in SS_LAMPS:
        b += _lamp(x, y, lens, r=0.6)
    # The logic test selector: its plate, the knob and its white pointer (up and left: OFF).
    kx, ky = SS_KNOB
    b += [(kx - 0.7, ky - 0.7, DOOR, kx + 0.7, ky + 0.7, DOOR + 0.12, {"*": "pi_chrome", "south": F})]
    b += PP._sc_round(kx, ky, DOOR + 0.12, DOOR + 0.55, 0.55, "pi_cap_black")
    b.append((kx - 0.08, ky, DOOR + 0.55, kx + 0.08, ky + 0.5, DOOR + 0.65, "pi_pnl_white", {"rotation": {"angle": 45, "axis": "z", "origin": [kx, ky, DOOR]}}))
    return b


# --------------------------------------------------------------------------------------- the nuclear instruments
# A rack of drawers with no door: two instrument drawers in the upper half (each its meter, three lamps, the level trip
# bypass switch, its handles), a high voltage power supply drawer and a cooling fan panel below.
NI_KEY = "nis_cabinet"
NI_DRAWERS = [(23.3, 29.5), (16.6, 22.8)]          # the instrument drawers (y0, y1)
NI_LOWER = [(9.4, 15.6), (2.0, 8.6)]
NI_FRONT = 15.0
NI_LAMPS = [GREEN, RED, AMBER]
NI_LENS = ["pi_lens_green", "pi_lens_red", "pi_lens_amber"]
NI_SCALES = {"SR": [("1", 0), ("1E3", 0.5), ("1E6", 1)], "IR": [("1E-11", 0), ("1E-7", 0.5), ("1E-3", 1)],
             "PR": [("0", 0), ("60", 0.5), ("120", 1)], "DEV": [("-10", 0), ("0", 0.5), ("+10", 1)],
             "SUR": [("-1", 0), ("2", 0.5), ("5", 1)]}


def _ni_meter(y0, y1):
    return 2.2, y0 + 0.6, 7.9, y1 - 1.35


def _ni_lamp_y(y1, i):
    return y1 - 1.75 - i * 1.25


def nis_cabinet(label="N31 N32"):
    v = NIS_LABELS.index(label)
    group, drawers, lamps = NIS_RACKS[v]
    f = PP.Front(1, 640 + v)
    _band(f, ((44, 46, 50, 255), WHITE), "NUCLEAR INSTRUMENTATION", group, y0=29.75, y1=31.3)
    for k, ((y0, y1), (tag, title, scale, unit)) in enumerate(zip(NI_DRAWERS, drawers)):
        f.R(1.4, y0, 14.6, y1, fill=(196, 198, 192, 255), outline=(110, 112, 110, 255), width=2)
        f.R(2.2, y1 - 1.0, 9.0, y1 - 0.25, fill=BLACK)
        _fit(f, 5.6, y1 - 0.62, f"{tag}  {title}", 7, 6.6, fill=WHITE)
        mx0, my0, mx1, my1 = _ni_meter(y0, y1)
        _quarter_meter(f, mx0, my0, mx1, my1, "", unit, NI_SCALES[scale], f"rr_m{k}", NI_FRONT + 0.2)
        for i, (name, colour) in enumerate(zip(lamps, NI_LAMPS)):
            ly = _ni_lamp_y(y1, i)
            _live_lamp(f, 9.6, ly, 0.42, colour, f"rr_l{k * 3 + i}", NI_FRONT)
            f.T(10.35, ly, name, 7, bold=True, anchor="lm")
        f.T(12.6, y0 + 0.75, "LVL TRIP BYPASS", 6.5, bold=True, anchor="rm")
    for (y0, y1), name in zip(NI_LOWER, ("HV POWER SUPPLY", "COOLING FANS")):
        f.R(1.4, y0, 14.6, y1, fill=(196, 198, 192, 255), outline=(110, 112, 110, 255), width=2)
        f.R(4.0, y1 - 1.1, 12.0, y1 - 0.35, fill=BLACK)
        f.T(8.0, y1 - 0.72, name, 7, fill=WHITE, bold=True)
        if name == "COOLING FANS":
            for k in range(6):
                f.R(3.0, y0 + 0.7 + k * 0.75, 13.0, y0 + 1.05 + k * 0.75, fill=(40, 40, 42, 255))
        else:
            f.T(4.5, y0 + 0.8, "ON", 7, bold=True)
            f.T(8.0, y0 + 0.8, "HV ADJ", 7, bold=True)
            f.T(11.5, y0 + 0.8, "FUSE", 7, bold=True)
    return f


def nis_cabinet_boxes():
    F = f"pi_{NI_KEY}_f"
    face = {"*": "pi_cabinet", "south": F}
    D = NI_FRONT
    b = _shell()
    # The rack's front frame (its stiles, the top under the band's plate).
    b += [(1.0, 1.2, 14.6, 1.4, 31.4, D, "pi_pnl_black"), (14.6, 1.2, 14.6, 15.0, 31.4, D, "pi_pnl_black"),
          (1.4, 29.5, 14.6, 14.6, 31.4, D, face), (1.6, 29.75, D, 14.4, 31.3, D + 0.12, {"*": "pi_pnl_black", "south": F})]
    for y0, y1 in NI_DRAWERS + NI_LOWER:
        # The drawer: its chassis back into the rack, its front panel, a handle each side.
        b += [(1.6, y0 + 0.1, 3.0, 14.4, y1 - 0.1, D - 0.3, "pi_cabinet_dark"), (1.4, y0, D - 0.3, 14.6, y1, D, face)]
        for hx in (1.6, 14.15):
            b += [(hx, y0 + 1.0, D, hx + 0.25, y0 + 1.4, D + 0.9, "pi_chrome"), (hx, y1 - 1.4, D, hx + 0.25, y1 - 1.0, D + 0.9, "pi_chrome"),
                  (hx, y0 + 1.0, D + 0.9, hx + 0.25, y1 - 1.0, D + 1.15, "pi_chrome")]
    for k, (y0, y1) in enumerate(NI_DRAWERS):
        b += _meter_case(*_ni_meter(y0, y1), face, D)
        for i in range(3):
            b += _lamp(9.6, _ni_lamp_y(y1, i), NI_LENS[i], D, r=0.42)
        # The level trip bypass toggle (down: normal).
        tx = 13.3
        b += [(tx - 0.35, y0 + 0.4, D, tx + 0.35, y0 + 1.1, D + 0.12, "pi_chrome"),
              (tx - 0.1, y0 + 0.45, D + 0.12, tx + 0.1, y0 + 0.75, D + 0.55, "pi_chrome")]
    # The power supply drawer: its switch (up: on), the high voltage adjuster, the fuse holder.
    y0 = NI_LOWER[0][0]
    b += [(4.15, y0 + 1.5, D, 4.85, y0 + 2.2, D + 0.12, "pi_chrome"), (4.4, y0 + 1.85, D + 0.12, 4.6, y0 + 2.7, D + 0.45, "pi_chrome")]
    b += PP._sc_round(8.0, y0 + 2.0, D, D + 0.4, 0.5, "pi_cap_black")
    b += PP._sc_round(11.5, y0 + 2.0, D, D + 0.5, 0.45, "pi_cap_black")
    return b


# --------------------------------------------------------------------------------------- the protective relays
RP_KEY = "relay_panel"
RP_PLATE = (2.0, 29.3, 14.0, 31.2)
RP_LABEL_Y, RP_LABEL_SIZE = 30.62, 0.62


def relay_panel(label=None):
    """The relay cabinet's door and what shows through it (plant_panels relay_cabinet_front), its nameplate wider and
    lettered live with the panel's name (RELAY_LABELS, by variant) over PROTECTIVE RELAYS."""
    f = PP.relay_cabinet_front(nameplate=False)
    x0, y0, x1, y1 = RP_PLATE
    f.R(x0, y0, x1, y1, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    f.T(8.0, y0 + 0.45, "PROTECTIVE RELAYS  -  125 VDC", 7, fill=WHITE, bold=True)
    f.text(8.0, RP_LABEL_Y, DOOR + 0.14, RP_LABEL_SIZE, (238, 238, 232), "label", "center")
    return f


def relay_panel_boxes():
    """The relay cabinet whole over its two blocks (its stack parts put back together), its drawing this panel's."""
    always, upper, lower, cap, plinth = PP.relay_cabinet_boxes()
    F = f"pi_{RP_KEY}_f"
    old = {f"{PP.RC_F}_0", f"{PP.RC_F}_1"}

    def retex(box, dy):
        t = box[6]
        if isinstance(t, dict):
            t = {k: (F if v in old else v) for k, v in t.items()}
        return (box[0], box[1] + dy, box[2], box[3], box[4] + dy, box[5], t) + tuple(box[7:])

    # The sides and back (each block's own in a stack) between the plinth and the cap here (tools/zfight.py).
    low = [(b[0], max(b[1], 1.2)) + tuple(b[2:]) for b in always]
    high = [b[:4] + (min(b[4], 15.4),) + tuple(b[5:]) for b in always]
    out = [retex(b, 0) for b in low + lower + plinth] + [retex(b, 16) for b in high + upper + cap]
    # The nameplate: wider than the stock cabinet's (its name lettered on it live).
    x0, y0, x1, y1 = RP_PLATE
    out = [b for b in out if not (abs(b[1] - 29.5) < 0.01 and abs(b[4] - 30.9) < 0.01)]
    out.append((x0, y0, DOOR, x1, y1, DOOR + 0.14, {"*": "pi_pnl_black", "south": F}))
    return out


# ------------------------------------------------------------------------------------------ the room's pieces
# The row signs: on the walls at each row's ends, a letter and what the row holds.
ROWS = {"a": "PROTECTION CH I - IV", "b": "SSPS  -  NIS", "c": "ELECTRICAL RELAYS", "d": "PUMP RELAYS"}
ROW_SIGNS = {f"sign_relay_row_{k}": f"Relay Room Row {k.upper()} Sign" for k in ROWS}
# The labels' pictures: drawn at their plate's shape in the top of a 512 px square, the UV the same band of it.
RACK_TAG_UV = [0, 0, 16, round(16 * 0.7 / 8.0, 4)]
PRINTER_UV = [0, 0, 16, round(16 * 4.2 / 13.0, 4)]


def _row_sign(row):
    """A row sign's picture (as plant_panels' signs: 433 by 512 px used of a 512 px square): ROW, the letter big, what
    the row holds."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    NAVY, WHITE_ = (26, 52, 104, 255), (250, 250, 246, 255)
    d.rectangle([0, 0, W - 1, H - 1], fill=NAVY, outline=WHITE_, width=10)
    d.rectangle([22, 22, W - 23, H - 23], outline=WHITE_, width=4)
    d.text((W // 2, 92), "ROW", font=PP.rfont(84, True), fill=WHITE_, anchor="mm")
    d.text((W // 2, 262), row.upper(), font=PP.rfont(250, True), fill=WHITE_, anchor="mm")
    PP._fit_line(d, (W // 2, 430), ROWS[row], 48, W - 70, WHITE_)
    return img


def smoke_detector_boxes():
    """A ceiling smoke detector (on the ceiling: the block's top): its base plate, the round head, the band of vent
    slots round it, the alarm LED and the test button under it."""
    out = [(5.3, 15.6, 5.3, 10.7, 16.0, 10.7, "pi_smoke_det")]
    out += PP.round_y(8, 8, 2.25, 14.2, 15.6, {"*": "pi_smoke_side", "up": "pi_smoke_det", "down": "pi_smoke_det"})
    out += [(9.6, 13.95, 7.8, 10.0, 14.2, 8.2, "pi_pnl_red"), (7.5, 13.9, 7.5, 8.5, 14.2, 8.5, "pi_smoke_vent")]
    return out


def drawing_rack_boxes():
    """A stick file of the room's drawings (facing north: its sheets hang across x, read from +z): two posts on feet,
    the rail across their tops with its label, six drawings hanging from it on their clamps (the plant's prints:
    pi_blueprint_0..5)."""
    out = []
    for x in (1.0, 14.0):
        out += [(x, 0, 2.0, x + 1.0, 0.5, 14.0, "pi_steel_dark"), (x + 0.2, 0.5, 7.6, x + 0.8, 14.4, 8.4, "pi_steel")]
    out += [(0.6, 14.4, 6.4, 15.4, 15.2, 9.6, "pi_steel_dark"),
            (4.0, 14.45, 9.6, 12.0, 15.15, 9.7, {"*": "pi_pnl_white", "south": ("pi_drawing_rack_tag", RACK_TAG_UV)})]
    for k in range(6):
        z = 6.7 + k * 0.48
        out += [(2.0, 13.7, z, 14.0, 14.4, z + 0.3, "pi_steel"),
                (2.2, 2.6 + (k % 3) * 0.5, z + 0.1, 13.8, 13.7, z + 0.15, {"*": "pi_paper", "south": f"pi_blueprint_{k}!", "north": f"pi_blueprint_{k}!"})]
    return out


def soe_printer_boxes():
    """The sequence of events recorder's line printer on its stand (facing north, its front +z): the stand's legs and
    shelf with the box of fanfold paper, the printer, the paper up out of its top."""
    leg = "pi_steel_dark"
    return [(1.0, 0, 2.0, 2.0, 9.0, 3.0, leg), (14.0, 0, 2.0, 15.0, 9.0, 3.0, leg), (1.0, 0, 13.0, 2.0, 9.0, 14.0, leg),
            (14.0, 0, 13.0, 15.0, 9.0, 14.0, leg), (1.0, 3.0, 2.0, 15.0, 3.5, 14.0, leg), (0.5, 9.0, 1.5, 15.5, 9.8, 14.5, leg),
            (2.5, 3.5, 3.5, 13.5, 7.0, 12.5, {"*": "pi_carton", "up": "pi_fanfold!"}),
            # The printer: body, the platen's cover, the control panel's strip along the front.
            (1.5, 9.8, 3.0, 14.5, 14.0, 13.5, {"*": "pi_printer_body", "south": ("pi_printer_front", PRINTER_UV)}),
            (2.5, 14.0, 4.0, 13.5, 14.6, 9.0, "pi_printer_body"),
            (3.0, 14.0, 9.0, 13.0, 14.3, 12.5, "pi_pnl_black"),
            # The paper: out of the cover's slot and up, printed.
            (3.2, 14.6, 5.0, 12.8, 18.0, 5.15, {"*": "pi_paper", "north": "pi_fanfold_print!", "south": "pi_fanfold_print!"})]


def _smoke_side():
    """The detector head's side: white, the band of vent slots round it where the head's sides map (y 14.7..15.1)."""
    img = Image.new("RGBA", (256, 256), (236, 236, 230, 255))
    d = ImageDraw.Draw(img)
    r0, r1 = round((16 - 15.1) * 16), round((16 - 14.7) * 16)
    for x in range(2, 256, 8):
        d.rectangle([x, r0, x + 4, r1], fill=(70, 72, 74, 255))
    return img


def _drawing_rack_tag():
    img = Image.new("RGBA", (512, 512), (236, 236, 230, 255))
    d = ImageDraw.Draw(img)
    h = round(512 * RACK_TAG_UV[3] / 16)
    d.rectangle([0, 0, 511, h - 1], outline=(30, 30, 32, 255), width=3)
    d.text((256, h / 2), "RELAY ROOM DRAWINGS", font=PP.rfont(h * 0.62, True), fill=INK, anchor="mm")
    return img


def _printer_front():
    """The printer's front (the band PRINTER_UV of the picture): the recorder's name, the panel's lamps and keys."""
    img = Image.new("RGBA", (512, 512), (214, 208, 190, 255))
    d = ImageDraw.Draw(img)
    h = round(512 * PRINTER_UV[3] / 16)
    d.rectangle([0, 0, 511, h - 1], outline=(120, 116, 104, 255), width=3)
    d.rectangle([16, 12, 250, 62], fill=(40, 40, 42, 255))
    PP._fit_line(d, (133, 37), "SOE RECORDER", 38, 214, (236, 236, 230, 255))
    PP._fit_line(d, (133, 88), "SEQUENCE OF EVENTS", 26, 230, INK)
    for k in range(4):
        d.rectangle([22 + k * 58, 112, 66 + k * 58, 142], fill=(70, 70, 72, 255))
    for k, (name, colour) in enumerate((("POWER", (60, 200, 90)), ("READY", (60, 200, 90)), ("PAPER", (220, 160, 40)))):
        x = 312 + k * 72
        d.ellipse([x - 11, 14, x + 11, 36], fill=colour + (255,), outline=(30, 30, 30, 255), width=2)
        d.text((x, 52), name, font=PP.rfont(17, True), fill=INK, anchor="mm")
    d.rectangle([280, 72, 494, 146], fill=(30, 30, 32, 255))
    return img


def _fanfold(printed):
    """Fanfold paper: green bars, the tractor holes down the edges; printed, the recorder's lines of events."""
    img = Image.new("RGBA", (256, 256), (244, 244, 236, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 256, 32):
        d.rectangle([16, y, 239, y + 15], fill=(206, 230, 206, 255))
    for y in range(6, 256, 16):
        d.ellipse([4, y, 10, y + 6], fill=(200, 200, 194, 255))
        d.ellipse([245, y, 251, y + 6], fill=(200, 200, 194, 255))
    if printed:
        for k, y in enumerate(range(6, 250, 8)):
            n = 60 + (k * 37) % 120
            d.rectangle([22, y, 22 + n, y + 3], fill=(60, 60, 64, 255))
            d.rectangle([30 + n, y, 30 + n + (k * 53) % 70, y + 3], fill=(60, 60, 64, 255))
    return img


def makers():
    out = {f"pi_{k}": (lambda k=k: _row_sign(k[-1])) for k in ROW_SIGNS}
    out.update({
        "pi_relay_card": lambda: PP._solid((38, 96, 62), 541, 0.04), "pi_smoke_det": lambda: PP._solid((236, 236, 230), 542, 0.02),
        "pi_smoke_vent": lambda: PP._solid((150, 150, 146), 543, 0.02), "pi_smoke_side": _smoke_side, "pi_printer_body": lambda: PP._solid((214, 208, 190), 544, 0.03),
        "pi_printer_front": _printer_front, "pi_fanfold": lambda: _fanfold(False), "pi_fanfold_print": lambda: _fanfold(True),
        "pi_drawing_rack_tag": _drawing_rack_tag,
    })
    return out


# ------------------------------------------------------------------------------------------- registering them
PANELS = {
    PR_KEY: {"display": "Process Protection Rack", "width": 1, "item3d": True, "labels": PROTECTION_LABELS},
    SS_KEY: {"display": "Solid State Protection System Cabinet", "width": 1, "item3d": True, "labels": SSPS_LABELS},
    NI_KEY: {"display": "Nuclear Instrumentation Drawer Rack", "width": 1, "item3d": True, "labels": NIS_LABELS},
    # One drawing for every relay panel: the name is the live part (plant_panels texture_key: "shared").
    RP_KEY: {"display": "Protective Relay Panel", "width": 1, "item3d": True, "labels": RELAY_LABELS, "shared": True},
}
DRAWINGS = {PR_KEY: protection_rack, SS_KEY: ssps_cabinet, NI_KEY: nis_cabinet, RP_KEY: relay_panel}
BOXES = {PR_KEY: protection_rack_boxes, SS_KEY: ssps_cabinet_boxes, NI_KEY: nis_cabinet_boxes, RP_KEY: relay_panel_boxes}
PIECES = {k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in ROW_SIGNS.items()}
PIECES.update({
    "smoke_detector": {"display": "Smoke Detector", "boxes": smoke_detector_boxes()},
    "drawing_rack": {"display": "Drawing Stick File", "boxes": drawing_rack_boxes()},
    "soe_printer": {"display": "Sequence of Events Recorder Printer", "boxes": soe_printer_boxes()},
})
CRAFTING = {
    PR_KEY: (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "X": "minecraft:comparator",
                                     "R": "minecraft:redstone"}, 1),
    SS_KEY: (["PGP", "XCX", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "X": "jugcraft:processor",
                                     "R": "minecraft:redstone"}, 1),
    NI_KEY: (["PMP", "CXC", "PRP"], {"P": "#c:plates/steel", "M": "minecraft:clock", "C": "jugcraft:basic_circuit", "X": "minecraft:observer",
                                     "R": "minecraft:redstone"}, 1),
    RP_KEY: (["PGP", "CRC", "PIP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone",
                                     "I": "minecraft:iron_ingot"}, 1),
    "smoke_detector": ([" W ", "WRW"], {"W": "minecraft:white_dye", "R": "minecraft:redstone"}, 2),
    "drawing_rack": (["SSS", "PPP", "S S"], {"S": "#c:plates/steel", "P": "minecraft:paper"}, 1),
    "soe_printer": (["PCP", "SXS", "S S"], {"P": "minecraft:paper", "C": "jugcraft:basic_circuit", "S": "#c:plates/steel", "X": "minecraft:dispenser"}, 1),
}
ROW_DYES = {"a": "blue", "b": "cyan", "c": "purple", "d": "light_blue"}
CRAFTING.update({k: (["PLP", " I "], {"P": "minecraft:paper", "L": f"minecraft:{ROW_DYES[k[-1]]}_dye", "I": "minecraft:iron_nugget"}, 2) for k in ROW_SIGNS})


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], 2, "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(PP.variants(key)) for p in range(2)}
    PP.SHARP |= {f"pi_{k}" for k in ROW_SIGNS} | {"pi_drawing_rack_tag", "pi_printer_front", "pi_smoke_side"}


register()
