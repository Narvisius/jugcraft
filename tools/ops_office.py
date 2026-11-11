"""The operations office (round 26, #13; Justin: "Work control + tagout", live "Status screen + out-of-service board",
fittings "Radio charging rack", "PPE and flashlights", "Shift schedule whiteboard", "Procedure shelves + printer"). The
second floor's operations office (x 31..38, z 22..28) as a real plant's work control centre: where the work control
senior reactor operator plans the day's work and the tagout coordinator writes and hangs the clearances.

- the work control consoles: desks three blocks wide, two positions each (the position's name on a plate on the
  divider, lettered live), each with two monitors showing its own live data; the tagout console's right position has
  the tag printer, the blank tags and the padlocks;
- the equipment out-of-service board (a wall display three blocks across): every breaker, starter and fan in the plant
  that is open or switched off, with its train, its tags, how long it has been out and whether a technical
  specification action (LCO) is running on it, read from the plant;
- the plant status screen (the administration building's), the wall clock;
- the shift schedule whiteboard: the crews' rotation and the plan of the day (the crew on shift and its names live);
- the radio charging rack and the PPE shelf (hard hats, glasses, ear defenders, flashlights on their chargers; the
  chargers' lamps live), the locked valve key locker, the clearance tag rack and the group lockbox;
- the procedure shelves (the TSC's), the copier (the administration building's), filing cabinets.

What moves is the game's (client/EdgTelemetry.java ops; nuclear/OpsOffice.java). Models in model pixels for the piece
facing north: x across, y up, z from its back (z 0) out to its front, where its user is (+z)."""
import math

from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP
import relay_room as RR
import tsc_room as T

K = 512

# ------------------------------------------------------------------------------------------------ the consoles
CON_KEY = "ops_console"
POSITIONS = [("WORK CONTROL SRO", "WORK CONTROL CLERK"), ("TAGOUT COORDINATOR", "CLEARANCE WRITER")]
DESK_Y = T.DESK_Y
BLACK = T.BLACK
SEATS = [{"mon": 10.0, "kbd": 5.0, "phone": 16.6, "binder": 1.6, "clip": 12.6, "tower": 16.0},
         {"mon": 38.0, "kbd": 33.0, "phone": 27.6, "binder": 43.6, "clip": 28.2, "tower": 28.6}]


def _tag_printer(x0, z0):
    """A thermal tag printer: its body, the sloped top with its display and keys, a printed red tag curling out of
    its front slot."""
    out = [(x0, DESK_Y, z0, x0 + 4.4, DESK_Y + 2.2, z0 + 3.2, {"*": "pi_ops_printer_side", "up": "pi_ops_printer_top!", "south": "pi_ops_printer_front!"}),
           (x0 + 0.3, DESK_Y + 2.2, z0 + 0.2, x0 + 4.1, DESK_Y + 2.6, z0 + 1.6, "pi_ops_printer_side")]
    out += [(x0 + 0.9, DESK_Y + 0.9, z0 + 3.2, x0 + 3.5, DESK_Y + 1.0, z0 + 4.6, {"*": "pi_ops_tag_red", "up": "pi_ops_tag_danger!"})]
    return out


def _tag_tray(x0, z0):
    """A tray of blank clearance tags (red DANGER, yellow CAUTION), each a stack."""
    out = [(x0, DESK_Y, z0, x0 + 4.6, DESK_Y + 0.6, z0 + 3.0, {"*": "pi_tsc_body", "up": "pi_tsc_body"})]
    out += [(x0 + 0.3, DESK_Y + 0.6, z0 + 0.3, x0 + 2.2, DESK_Y + 1.2, z0 + 2.7, {"*": "pi_ops_tag_red", "up": "pi_ops_tag_danger!"}),
            (x0 + 2.4, DESK_Y + 0.6, z0 + 0.3, x0 + 4.3, DESK_Y + 1.0, z0 + 2.7, {"*": "pi_ops_tag_yellow", "up": "pi_ops_tag_caution!"})]
    return out


def _padlock(cx, z0, body="pi_ops_lock_red", y=DESK_Y):
    """A safety padlock lying on the desk: its body and the shackle's two legs and top."""
    return [(cx - 0.6, y, z0, cx + 0.6, y + 0.5, z0 + 1.3, body),
            (cx - 0.5, y, z0 - 0.9, cx - 0.3, y + 0.25, z0, "pi_chrome"), (cx + 0.3, y, z0 - 0.9, cx + 0.5, y + 0.25, z0, "pi_chrome"),
            (cx - 0.5, y, z0 - 1.1, cx + 0.5, y + 0.25, z0 - 0.9, "pi_chrome")]


def ops_console_boxes():
    W = 48
    out = T._desk(W, [24.0])
    for k, p in enumerate(SEATS):
        px = p["mon"]
        out += T._stand(px, 5.4) + T._monitor(px - T.MON_GAP) + T._monitor(px + T.MON_GAP)
        out += T._keyboard(p["kbd"], 9.4) + T._phone(p["phone"], 6.4) + T._tower(p["tower"]) + T._plate(px)
        if k == 0:
            out += T._binder(p["binder"], 10.2, "pi_ops_binder") + [b if b[6] != {"*": "pi_tsc_hardboard", "up": "pi_tsc_log!"} else
                                                                     b[:6] + ({"*": "pi_tsc_hardboard", "up": "pi_ops_log!"},)
                                                                     for b in T._clipboard(p["clip"], 12.4)]
        else:
            out += _tag_printer(42.4, 5.8) + _tag_tray(42.8, 12.2) + _padlock(29.0, 13.0) + _padlock(31.0, 13.4, "pi_ops_lock_red")
    return out


SCREEN_LINES = T.SCREEN_LINES


def ops_console(label=None):
    f = PP.Front(3, 790)
    for k, p in enumerate(SEATS):
        px = p["mon"]
        f.text(px, (T.PLATE[0] + T.PLATE[1]) / 2, T.PLATE[3] + 0.01, 1.15, (246, 246, 240), f"ops_name{k}", "center")
        for m, dx in enumerate((-T.MON_GAP, T.MON_GAP)):
            T._screen_live(f, px + dx, f"ops_p{k}_m{m}")
    return f


# ------------------------------------------------------------------------------------------------ out-of-service board
OOS_KEY = "ops_oos_board"
OOS_SCREEN = (1.0, 2.6, 47.0, 29.6)
OOS_Z = 2.4
OOS_ROWS = 9
OOS_TOP, OOS_STEP = 22.6, 2.08
# The columns: heading, its x and how its live text sits there.
OOS_COLS = [("NO.", 1.9, "left", "n"), ("EQUIPMENT", 7.8, "left", "e"), ("TRAIN", 28.4, "center", "tr"), ("TAGS", 33.0, "center", "tg"),
            ("STATUS", 38.6, "center", "s"), ("OUT FOR", 46.2, "right", "t")]
NAVY, PANEL_BG = T.NAVY, T.PANEL_BG
CYAN, WHITE_T, GREY_T = T.CYAN, T.WHITE_T, T.GREY_T


def ops_oos_board(label=None):
    f = PP.Front(3, 791)
    x0, y0, x1, y1 = OOS_SCREEN
    f.R(0, 0, 48, 32, fill=(30, 30, 32, 255))
    f.R(x0, y0, x1, y1, fill=NAVY)
    # The title bar: the unit, what it is, the clock (live).
    f.R(x0, y1 - 2.2, x1, y1, fill=(150, 28, 30, 255))
    f.T(x0 + 0.6, y1 - 1.1, "UNIT 1", 11, fill=WHITE_T, bold=True, anchor="lm")
    RR._fit(f, 22.0, y1 - 1.1, "EQUIPMENT OUT OF SERVICE", 15, 26.0, fill=WHITE_T)
    f.text(x1 - 0.6, y1 - 1.1, OOS_Z + 0.02, 1.0, (238, 240, 244), "ops_clock", "right")
    # The line under it: the plant's mode (live) and the system it comes from.
    f.R(x0, y1 - 3.8, x1, y1 - 2.2, fill=(20, 30, 52, 255))
    f.T(x0 + 0.6, y1 - 3.0, "WCC-1  WORK CONTROL CENTER  -  EOOS / LCO TRACKING", 9, fill=GREY_T, bold=True, anchor="lm")
    f.T(34.0, y1 - 3.0, "PLANT MODE", 9, fill=GREY_T, bold=True, anchor="rm")
    f.text(34.6, y1 - 3.0, OOS_Z + 0.02, 0.78, (120, 255, 150), "ops_mode")
    # The column headings, the rows (an LCO's row lit amber, live).
    hy = y1 - 5.0
    for name, x, align, _ in OOS_COLS:
        anchor = {"left": "lm", "center": "mm", "right": "rm"}[align]
        f.T(x, hy, name, 9, fill=CYAN, bold=True, anchor=anchor)
    f.line([(x0 + 0.5, hy - 0.9), (x1 - 0.5, hy - 0.9)], (60, 80, 120, 255), 2)
    for i in range(OOS_ROWS):
        y = OOS_TOP - i * OOS_STEP
        f.R(x0 + 0.5, y - OOS_STEP / 2 + 0.12, x1 - 0.5, y + OOS_STEP / 2 - 0.12, fill=PANEL_BG)
        f.glow(x0 + 0.5, y - OOS_STEP / 2 + 0.12, x1 - 0.5, y + OOS_STEP / 2 - 0.12, OOS_Z + 0.01, (150, 100, 10), f"ops_oos_lco{i}", 0.55)
        for _, x, align, stem in OOS_COLS:
            colour = (255, 210, 80) if stem in ("n", "t") else (238, 240, 244)
            f.text(x, y, OOS_Z + 0.02, 0.8, colour, f"ops_oos_{stem}{i}", align)
    # The footer: the counts (live) and the system's name.
    f.R(x0, y0, x1, y0 + 2.2, fill=(16, 24, 44, 255))
    for k, (name, x) in enumerate((("OUT OF SERVICE", 2.0), ("LCO ACTIONS", 17.0), ("CLEARANCES HUNG", 31.0))):
        f.T(x, y0 + 1.1, name, 9, fill=GREY_T, bold=True, anchor="lm")
        f.text(x + 9.6 + (1.2 if k == 2 else 0), y0 + 1.1, OOS_Z + 0.02, 0.85, (255, 210, 80), f"ops_count{k}")
    return f


def ops_oos_board_boxes():
    x0, y0, x1, y1 = OOS_SCREEN
    F = f"pi_{OOS_KEY}_f"
    return [(8.0, 8.0, 0.1, 40.0, 24.0, 0.9, "pi_steel_dark"), (14.0, 4.0, 0.1, 18.0, 28.0, 0.9, "pi_steel_dark"),
            (30.0, 4.0, 0.1, 34.0, 28.0, 0.9, "pi_steel_dark"),
            (0.3, 1.8, 0.9, 47.7, 30.4, OOS_Z - 0.1, {"*": "pi_tsc_case", "south": "pi_tsc_case"}),
            (x0, y0, OOS_Z - 0.1, x1, y1, OOS_Z, {"*": "pi_tsc_case", "south": F}),
            (23.0, 1.9, OOS_Z - 0.1, 25.0, 2.3, OOS_Z - 0.02, "pi_chrome")]


# ------------------------------------------------------------------------------------------------ the schedule board
SB_KEY = "ops_schedule_board"
SB_FACE = (1.0, 1.8, 47.0, 30.4)
SB_Z = 1.0
CREWS = "ABCDEF"
DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
# The crews' week (D days, N nights, O off, T training): each day one crew on days and one on nights.
ROTATION = ["DDOOTOO", "NNDDOOO", "OONNDDO", "OOOONNO", "TOOTOOD", "OTOOOTN"]    # nuclear/OpsOffice.java ROTATION
POD_ROWS = 6     # the plan of the day's lines (live)


def ops_schedule_board(label=None):
    f = PP.Front(3, 792)
    x0, y0, x1, y1 = SB_FACE
    ink, blue, red, green = (24, 26, 30, 255), (30, 50, 140, 255), (170, 30, 30, 255), (30, 110, 60, 255)
    f.R(0, 0, 48, 32, fill=(190, 194, 198, 255))
    f.R(x0, y0, x1, y1, fill=(246, 247, 244, 255))
    f.R(x0, y1 - 2.4, x1, y1, fill=(30, 70, 140, 255))
    RR._fit(f, 24.0, y1 - 1.2, "OPERATIONS  -  SHIFT SCHEDULE", 16, 42.0, fill=(250, 250, 246, 255))
    # The rotation: a grid ruled in black tape, the crews down its side, the days along its top; each shift a marker
    # letter (days blue, nights red, training green).
    gx0, gy1, cw, rh = x0 + 1.0, y1 - 3.4, 2.6, 2.7
    f.T(gx0 + 1.4, gy1 - 1.0, "CREW", 9, fill=ink, bold=True)
    for c, day in enumerate(DAYS):
        f.T(gx0 + 2.8 + c * cw + cw / 2, gy1 - 1.0, day, 9, fill=ink, bold=True)
    for r, crew in enumerate(CREWS):
        y = gy1 - 2.0 - r * rh - rh / 2
        f.T(gx0 + 1.4, y, crew, 14, fill=ink, bold=True)
        for c in range(7):
            s = ROTATION[r][c]
            colour = {"D": blue, "N": red, "T": green, "O": (150, 150, 150, 255)}[s]
            f.T(gx0 + 2.8 + c * cw + cw / 2, y, s, 14, fill=colour, bold=True, condensed=False)
    gx1 = gx0 + 2.8 + 7 * cw
    for r in range(len(CREWS) + 1):
        y = gy1 - 2.0 - r * rh
        f.line([(gx0, y), (gx1, y)], ink, 2)
    for c in range(8):
        x = gx0 + 2.8 + c * cw
        f.line([(x, gy1 - 2.0), (x, gy1 - 2.0 - len(CREWS) * rh)], ink, 2)
    f.line([(gx0, gy1 - 2.0), (gx0, gy1 - 2.0 - len(CREWS) * rh)], ink, 2)
    ky = gy1 - 2.0 - len(CREWS) * rh - 1.3
    for k, (s, name, colour) in enumerate((("D", "DAYS 0600-1800", blue), ("N", "NIGHTS 1800-0600", red), ("T", "TRAINING", green),
                                            ("O", "OFF", (150, 150, 150, 255)))):
        f.T(gx0 + 0.6 + (k % 2) * 11.0, ky - (k // 2) * 1.6, f"{s} = {name}", 8, fill=colour, bold=True, anchor="lm")
    # The plan of the day (live: the day, the crew on shift and its names, the plant's work).
    px = gx1 + 1.4
    f.line([(px - 0.7, y0 + 0.4), (px - 0.7, y1 - 2.6)], ink, 3)
    f.T(px + 0.2, y1 - 3.6, "PLAN OF THE DAY", 12, fill=red, bold=True, anchor="lm")
    for i in range(POD_ROWS):
        f.text(px + 0.2, y1 - 6.0 - i * 2.9, SB_Z + 0.06, 1.0, (30, 40, 110) if i else (170, 30, 30), f"ops_pod{i}")
    f.T(px + 0.2, y0 + 1.6, "PRE-JOB BRIEFS 0630 / 1830", 8, fill=ink, bold=True, anchor="lm")
    return f


def ops_schedule_board_boxes():
    """The whiteboard: its aluminium frame, the marker tray under it with its markers and eraser."""
    F = f"pi_{SB_KEY}_f"
    x0, y0, x1, y1 = SB_FACE
    trim = T.TRIM
    b = [(0.4, 1.0, 0.1, 47.6, 31.0, 0.6, {"*": trim, "south": F}), (x0, y0, 0.6, x1, y1, SB_Z, {"*": trim, "south": F})]
    b += [(0.4, y1, 0.6, 47.6, 31.0, SB_Z + 0.4, trim), (0.4, 1.0, 0.6, 47.6, y0, SB_Z + 0.4, trim),
          (0.4, y0, 0.6, x0, y1, SB_Z + 0.4, trim), (x1, y0, 0.6, 47.6, y1, SB_Z + 0.4, trim),
          (8.0, 1.0, SB_Z + 0.4, 40.0, 1.4, 3.0, trim), (8.0, 1.4, 2.6, 40.0, 2.0, 3.0, trim)]
    for x, tex in ((10.0, "pi_marker_red"), (13.0, "pi_marker_blue"), (16.0, "pi_tsc_marker_black")):
        b.append((x, 1.4, 1.8, x + 2.4, 1.8, 2.3, tex))
    b.append((30.0, 1.4, 1.5, 34.0, 2.6, 2.5, {"*": "pi_cap_black", "up": "pi_eraser"}))
    return b


# ------------------------------------------------------------------------------------------------ the clock
CL_KEY = "ops_wall_clock"
CL_C, CL_R, CL_Z = (8.0, 8.0), 6.2, 2.0


def ops_wall_clock(label=None):
    f = PP.Front(1, 793)
    cx, cy = CL_C
    f.R(0, 0, 16, 32, fill=(214, 216, 210, 255))
    f.E(cx, cy, CL_R, fill=(250, 250, 246, 255))
    for k in range(60):
        a = math.radians(90 - 6 * k)
        r0 = CL_R * (0.84 if k % 5 == 0 else 0.92)
        f.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * CL_R * 0.97, cy + math.sin(a) * CL_R * 0.97)],
               (24, 24, 26, 255), 4 if k % 5 == 0 else 2)
    for h in range(1, 13):
        a = math.radians(90 - 30 * h)
        f.T(cx + math.cos(a) * CL_R * 0.68, cy + math.sin(a) * CL_R * 0.68, str(h), 11, fill=(24, 24, 26, 255), bold=True, condensed=False)
    f.T(cx, cy - CL_R * 0.36, "UNIT 1 MASTER CLOCK", 6, fill=(110, 110, 112, 255), bold=True)
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": CL_R * 0.66, "z": CL_Z + 0.06, "lo": 0, "hi": 12, "start": 90, "sweep": 360, "ch": "ops_hour"})
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": CL_R * 0.98, "z": CL_Z + 0.08, "lo": 0, "hi": 60, "start": 90, "sweep": 360, "ch": "ops_min"})
    return f


def ops_wall_clock_boxes():
    """The master clock: its round chrome case on the wall, the face (numbers and ticks drawn) set in it, the hands'
    boss (the hands the game's)."""
    F = f"pi_{CL_KEY}_f"
    cx, cy = CL_C
    out = PP._sc_round(cx, cy, 0.0, CL_Z - 0.3, CL_R + 0.6, "pi_chrome")
    out += PP._sc_round(cx, cy, CL_Z - 0.3, CL_Z, CL_R, {"*": "pi_chrome", "south": F})
    out.append((cx - 0.45, cy - 0.45, CL_Z, cx + 0.45, cy + 0.45, CL_Z + 0.04, "pi_cap_black"))
    return out


# ------------------------------------------------------------------------------------------------ radio charging rack
RD_KEY = "ops_radio_rack"
RADIOS = 6
RD_X = [1.6 + i * 2.2 for i in range(RADIOS)]      # each pocket's left edge
RD_BASE = (0.8, 2.0, 15.2, 5.4)                     # the charger base's front: x0, y0, x1, y1
RD_Z = 6.0                                         # the base's front face


def ops_radio_rack(label=None):
    f = PP.Front(1, 794)
    x0, y0, x1, y1 = RD_BASE
    f.R(0, 0, 16, 32, fill=(30, 30, 32, 255))
    f.R(x0, y0, x1, y1, fill=(26, 26, 28, 255))
    RR._fit(f, 8.0, y0 + 0.6, "OPS RADIOS 1-6", 7, 13.0, fill=(220, 220, 214, 255))
    for i, x in enumerate(RD_X):
        cx = x + 1.0
        f.T(cx, y1 - 0.75, str(i + 1), 7, fill=(200, 200, 196, 255), bold=True)
        f.R(cx - 0.42, y0 + 1.2, cx + 0.42, y0 + 1.84, fill=(40, 10, 10, 255))
        f.glow(cx - 0.42, y0 + 1.2, cx + 0.42, y0 + 1.84, RD_Z + 0.02, (255, 50, 30), f"ops_radio{i}", 1.0, (60, 255, 90))
    # The rack's wall plate: its label.
    f.R(1.0, 14.0, 15.0, 15.6, fill=(236, 236, 230, 255))
    RR._fit(f, 8.0, 14.8, "RETURN RADIOS TO CHARGER", 7, 13.4, fill=(24, 24, 26, 255))
    return f


def ops_radio_rack_boxes():
    F = f"pi_{RD_KEY}_f"
    x0, y0, x1, y1 = RD_BASE
    out = [(0.6, 1.4, 0.0, 15.4, 15.8, 0.6, {"*": "pi_steel_dark", "south": F}),
           (x0, y0, 0.6, x1, y1, RD_Z, {"*": "pi_cap_black", "south": F})]
    for i, x in enumerate(RD_X):
        # The radio standing in its pocket: its body, the battery on its back, the antenna, the volume and channel
        # knobs, the push-to-talk bar on its side, its face (speaker grille, display, keys).
        out += [(x + 0.1, y1, 1.6, x + 1.9, y1 + 6.4, 4.4, {"*": "pi_ops_radio_side", "south": "pi_ops_radio!"}),
                (x + 0.2, y1, 1.2, x + 1.8, y1 + 5.0, 1.6, "pi_cap_black"),
                (x + 1.15, y1 + 6.4, 2.6, x + 1.55, y1 + 8.2, 3.0, "pi_cap_black"),
                (x + 1.05, y1 + 8.2, 2.5, x + 1.65, y1 + 8.6, 3.1, "pi_cap_black"),
                (x + 0.35, y1 + 6.4, 2.4, x + 0.85, y1 + 7.1, 3.2, "pi_ops_knob"),
                (x + 0.0, y1 + 3.0, 2.2, x + 0.1, y1 + 5.0, 3.6, "pi_cap_black")]
    return out


# ------------------------------------------------------------------------------------------------ PPE shelf
PPE_KEY = "ops_ppe_shelf"
SHELF_Y, SHELF_D = 7.6, 9.0
LIGHTS = 4
LT_X = [2.6 + i * 3.6 for i in range(LIGHTS)]      # each flashlight charger's centre (the shelf's right block)
LT_Z = 2.2                                        # the chargers' fronts


def ops_ppe_shelf(label=None):
    f = PP.Front(2, 795)
    f.R(0, 0, 32, 32, fill=(214, 216, 210, 255))
    # The label strip on the shelf's edge, the flashlights' sign, the chargers' fronts (their lamps live).
    f.R(1.0, SHELF_Y - 0.9, 31.0, SHELF_Y, fill=(236, 236, 230, 255))
    RR._fit(f, 8.0, SHELF_Y - 0.45, "HARD HATS  -  GLASSES  -  EAR DEFENDERS", 7, 14.0, fill=(24, 24, 26, 255))
    RR._fit(f, 24.0, SHELF_Y - 0.45, "OPERATOR PPE  -  SIGN OUT IN LOG", 7, 14.0, fill=(24, 24, 26, 255))
    for i in range(LIGHTS):
        cx = 16.0 + LT_X[i]
        f.R(cx - 1.4, 1.0, cx + 1.4, 5.4, fill=(36, 36, 40, 255))
        f.R(cx - 0.4, 1.5, cx + 0.4, 2.1, fill=(40, 10, 10, 255))
        f.glow(cx - 0.4, 1.5, cx + 0.4, 2.1, LT_Z + 0.02, (255, 50, 30), f"ops_light{i}", 1.0, (60, 255, 90))
    f.R(2.0, 1.0, 14.6, 5.4, fill=(250, 250, 246, 255), outline=(30, 70, 140, 255), width=4)
    RR._fit(f, 8.3, 4.2, "FLASHLIGHTS", 9, 11.6, fill=(30, 70, 140, 255))
    RR._fit(f, 8.3, 2.8, "RETURN TO CHARGER", 7, 11.6, fill=(24, 24, 26, 255))
    RR._fit(f, 8.3, 1.8, "GREEN = READY", 7, 11.6, fill=(30, 110, 60, 255))
    return f


def _hard_hat(cx, cz, y, sticker):
    """A hard hat on the shelf: the brim, the shell in three steps and its ridge, the sticker on its front."""
    shell = {"*": "pi_ops_hat", "south": sticker}
    return [(cx - 3.0, y, cz - 3.2, cx + 3.0, y + 0.4, cz + 3.4, "pi_ops_hat"),
            (cx - 2.4, y + 0.4, cz - 2.6, cx + 2.4, y + 1.8, cz + 2.6, shell),
            (cx - 2.0, y + 1.8, cz - 2.2, cx + 2.0, y + 2.7, cz + 2.2, "pi_ops_hat"),
            (cx - 1.4, y + 2.7, cz - 1.6, cx + 1.4, y + 3.3, cz + 1.6, "pi_ops_hat"),
            (cx - 0.35, y + 2.7, cz - 2.3, cx + 0.35, y + 3.5, cz + 2.3, "pi_ops_hat")]


def ops_ppe_shelf_boxes():
    F = f"pi_{PPE_KEY}_f"
    out = [(0.6, 1.0, 0.0, 31.4, 15.0, 0.2, "pi_steel_dark")]
    # The shelf on its two brackets, its label strip on its edge.
    out += [(1.0, SHELF_Y - 0.9, 0.2, 31.0, SHELF_Y, SHELF_D, {"*": "pi_tsc_body", "south": F, "up": "pi_tsc_body"})]
    for x in (1.0, 15.5, 31.1):
        out += [(x - 0.3, SHELF_Y - 4.0, 0.2, x + 0.3, SHELF_Y - 0.9, 1.0, "pi_tsc_body"),
                (x - 0.3, SHELF_Y - 1.6, 1.0, x + 0.3, SHELF_Y - 0.9, SHELF_D - 1.0, "pi_tsc_body")]
    y = SHELF_Y
    for k, cx in enumerate((4.2, 10.8)):
        out += _hard_hat(cx, 4.6, y, "pi_ops_hat_front!")
    # A box of safety glasses and a box of ear plugs; ear defenders hanging on hooks over the shelf.
    out += [(15.0, y, 1.0, 19.6, y + 3.0, 5.6, {"*": "pi_carton", "south": "pi_ops_glasses_box!"}),
            (20.4, y, 1.0, 23.6, y + 2.2, 4.6, {"*": "pi_carton", "south": "pi_ops_plugs_box!"})]
    for cx in (26.2, 29.4):
        out += [(cx - 0.2, y + 5.4, 0.2, cx + 0.2, y + 5.8, 2.4, "pi_chrome"),
                (cx - 1.2, y + 3.0, 1.6, cx + 1.2, y + 5.6, 2.2, "pi_cap_black"),
                (cx - 1.4, y + 1.4, 0.6, cx - 0.9, y + 3.4, 3.2, "pi_ops_muff"), (cx + 0.9, y + 1.4, 0.6, cx + 1.4, y + 3.4, 3.2, "pi_ops_muff"),
                (cx - 1.5, y + 1.6, 0.8, cx - 1.4, y + 3.2, 3.0, "pi_cap_black"), (cx + 1.4, y + 1.6, 0.8, cx + 1.5, y + 3.2, 3.0, "pi_cap_black")]
    # The flashlights' sign on the wall; the chargers under the shelf, each with its flashlight standing in it.
    out += [(2.0, 1.0, 0.2, 14.6, 5.4, 0.45, {"*": "pi_pnl_white", "south": F})]
    for i in range(LIGHTS):
        cx = 16.0 + LT_X[i]
        out += [(cx - 1.4, 1.0, 0.2, cx + 1.4, 5.4, LT_Z, {"*": "pi_cap_black", "south": F}),
                (cx - 0.7, 3.4, LT_Z, cx + 0.7, 6.2, LT_Z + 1.4, "pi_ops_torch"),
                (cx - 0.9, 5.4, LT_Z - 0.2, cx + 0.9, 6.6, LT_Z + 1.6, "pi_ops_torch_head"),
                (cx - 0.6, 6.6, LT_Z, cx + 0.6, 6.7, LT_Z + 1.4, "pi_amber_glass")]
    return out


# ------------------------------------------------------------------------------------------------ the plain pieces
def key_locker():
    """The locked valve key locker: a steel cabinet on the wall with a glass door (its frame, hinges, lock), the key
    board in it with its numbered hooks, a key on each but a few (signed out), each key's coloured tag by system."""
    out = [(1.0, 1.6, 0.0, 15.0, 14.6, 0.6, "pi_tsc_body"),
           (1.0, 1.6, 0.6, 1.6, 14.6, 3.0, "pi_tsc_body"), (14.4, 1.6, 0.6, 15.0, 14.6, 3.0, "pi_tsc_body"),
           (1.6, 1.6, 0.6, 14.4, 2.2, 3.0, "pi_tsc_body"), (1.6, 14.0, 0.6, 14.4, 14.6, 3.0, "pi_tsc_body"),
           (1.6, 2.2, 0.6, 14.4, 14.0, 0.7, "pi_ops_keyboard")]
    # The door: its frame round the glass, the hinges, the lock; the label over the glass.
    out += [(1.0, 1.6, 3.0, 15.0, 2.4, 3.4, "pi_tsc_body"), (1.0, 13.8, 3.0, 15.0, 14.6, 3.4, "pi_tsc_body"),
            (1.0, 2.4, 3.0, 1.8, 13.8, 3.4, "pi_tsc_body"), (14.2, 2.4, 3.0, 15.0, 13.8, 3.4, "pi_tsc_body"),
            (1.8, 2.4, 3.15, 14.2, 11.4, 3.25, "pi_relay_glass"),
            (1.8, 11.4, 3.15, 14.2, 13.8, 3.4, {"*": "pi_tsc_body", "south": strip("keys")}),
            (0.6, 3.6, 2.6, 1.0, 5.2, 3.4, "pi_chrome"), (0.6, 10.8, 2.6, 1.0, 12.4, 3.4, "pi_chrome")]
    out += PP._sc_round(13.4, 7.0, 3.4, 3.7, 0.5, "pi_chrome")
    out.append((13.3, 6.6, 3.7, 13.5, 7.4, 3.75, "pi_cap_black"))
    # The hooks and the keys on them: four rows of five.
    tags = ["pi_ops_ktag_red", "pi_ops_ktag_yellow", "pi_ops_ktag_blue", "pi_ops_ktag_green"]
    for r in range(4):
        y = 10.9 - r * 2.2
        for c in range(5):
            x = 3.0 + c * 2.5
            out.append((x - 0.15, y, 0.7, x + 0.15, y + 0.3, 1.6, "pi_chrome"))
            if (r * 5 + c) in (3, 11, 17):
                continue
            out += [(x - 0.25, y - 1.1, 1.2, x + 0.25, y + 0.1, 1.45, "pi_ops_brass"),
                    (x - 0.45, y - 1.9, 1.15, x + 0.45, y - 1.1, 1.3, tags[(r + c) % 4])]
    return out


def tag_rack():
    """The clearance tag rack: a steel board on the wall, three rows of pegs, the DANGER (red) and CAUTION (yellow)
    tags of the clearances hung on them, the rack's heading plate."""
    out = [(1.0, 1.0, 0.0, 15.0, 15.0, 0.5, "pi_tsc_body"),
           (1.4, 12.6, 0.5, 14.6, 14.6, 0.7, {"*": "pi_tsc_body", "south": strip("tags")})]
    for r in range(3):
        y = 11.4 - r * 3.6
        for c in range(5):
            x = 2.6 + c * 2.7
            out.append((x - 0.15, y, 0.5, x + 0.15, y + 0.3, 2.0, "pi_chrome"))
            danger = (r * 5 + c) % 3 != 2
            n = 2 if (r + c) % 2 else 1
            for k in range(n):
                face = "pi_ops_tag_danger!" if danger else "pi_ops_tag_caution!"
                side = "pi_ops_tag_red" if danger else "pi_ops_tag_yellow"
                z = 0.9 + k * 0.35
                out.append((x - 0.95, y - 3.0, z, x + 0.95, y + 0.1, z + 0.12, {"*": side, "south": face}))
    return out


def lockbox():
    """The group lockbox: a steel box on the wall with a clear window (the isolation keys inside it), its hasp with
    the workers' padlocks on it, each colour a crew; its label."""
    out = [(2.0, 3.0, 0.0, 14.0, 13.0, 4.0, {"*": "pi_ops_lockbox", "south": "pi_ops_lockbox_front!"}),
           (3.8, 6.0, 4.0, 12.2, 10.0, 4.1, "pi_relay_glass")]
    # The hasp, standing out of the box under the window, and the padlocks hanging in its holes.
    out += [(4.0, 4.4, 4.0, 12.0, 5.6, 4.3, "pi_chrome"), (4.6, 4.8, 4.3, 11.4, 5.2, 5.4, "pi_chrome")]
    bodies = ["pi_ops_lock_red", "pi_ops_lock_blue", "pi_ops_lock_yellow", "pi_ops_lock_green", "pi_ops_lock_red"]
    for k, x in enumerate((5.2, 6.6, 8.0, 9.4, 10.8)):
        out += [(x - 0.3, 3.4, 4.9, x - 0.15, 5.0, 5.1, "pi_chrome"), (x + 0.15, 3.4, 4.9, x + 0.3, 5.0, 5.1, "pi_chrome"),
                (x - 0.55, 1.2, 4.6, x + 0.55, 3.4, 5.5, bodies[k])]
    return out


# The operations procedures on the office's shelves: general and abnormal operating procedures, the system procedures,
# the operations administrative procedures (clearances, work control, key control), technical specifications.
SPINES = [("GOP-1", "PLANT STARTUP"), ("GOP-2", "POWER OPERATION"), ("GOP-3", "PLANT SHUTDOWN"), ("AOP", "AOP-1 TO AOP-20"),
          ("SOP-RCS", "REACTOR COOLANT"), ("SOP-CVCS", "CHARGING / LETDOWN"), ("SOP-FW", "FEEDWATER"), ("SOP-EL", "ELECTRICAL"),
          ("OAP-07", "CLEARANCES"), ("OAP-12", "WORK CONTROL"), ("OAP-15", "KEY CONTROL"), ("OAP-21", "SHIFT TURNOVER"),
          ("TECH SPECS", "VOL 1"), ("TECH SPECS", "VOL 2"), ("STP", "MONTHLY"), ("STP", "QUARTERLY")]


def procedure_shelf():
    """Steel shelving with the operating procedures in binders: the TSC's shelf, its spines the office's own."""
    out, k = [], 0
    for b in T.procedure_shelf():
        t = b[6]
        if isinstance(t, dict) and isinstance(t.get("south"), tuple):
            t = dict(t, south=spine(k % len(SPINES)))
            k += 1
        out.append(b[:6] + (t,) + tuple(b[7:]))
    return out


def spine(n):
    sheet, row = divmod(n, 4)
    return (f"pi_ops_spines_{sheet}", [0, row * 4, 16, row * 4 + 4], 90)


def _spines(sheet):
    img = Image.new("RGBA", (K, K), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    ink = (22, 22, 24, 255)
    for row in range(4):
        n = sheet * 4 + row
        top, sub = SPINES[n]
        colour = T.SPINE_COLOURS[(n + 1) % len(T.SPINE_COLOURS)]
        y0 = row * 128
        d.rectangle([0, y0, K - 1, y0 + 127], fill=colour + (255,))
        d.rectangle([36, y0 + 14, K - 37, y0 + 113], fill=(250, 250, 246, 255))
        PP._fit_line(d, (K // 2, y0 + 44), top, 44, K - 100, ink)
        PP._fit_line(d, (K // 2, y0 + 88), sub, 34, K - 100, ink)
    return img


STRIPS = {"keys": "LOCKED VALVE KEYS", "tags": "CLEARANCE TAGS"}


def strip(name):
    return ("pi_ops_strips", [0, list(STRIPS).index(name) * 4, 16, list(STRIPS).index(name) * 4 + 4])


PIECES = {
    "ops_key_locker": {"display": "Locked Valve Key Locker", "boxes": key_locker()},
    "ops_tag_rack": {"display": "Clearance Tag Rack", "boxes": tag_rack()},
    "ops_lockbox": {"display": "Group Lockbox", "boxes": lockbox()},
    "ops_procedure_shelf": {"display": "Operations Procedure Shelf", "boxes": procedure_shelf()},
}
SHAPES = {"ops_key_locker": (1.0, 1.6, 0.0, 15.0, 14.6, 3.7), "ops_tag_rack": (1.0, 1.0, 0.0, 15.0, 15.0, 2.0),
          "ops_lockbox": (2.0, 1.2, 0.0, 14.0, 13.0, 5.5), "ops_procedure_shelf": (0.6, 0, 2.0, 15.4, 16, 14.8)}


# ------------------------------------------------------------------------------------------------ textures
def _tag(head, colour, lines):
    img = Image.new("RGBA", (96, 160), colour + (255,))
    d = ImageDraw.Draw(img)
    d.ellipse([40, 6, 56, 22], fill=(200, 200, 196, 255), outline=(90, 90, 92, 255), width=3)
    d.rectangle([8, 30, 88, 64], fill=(250, 250, 246, 255) if colour[0] > 200 and colour[1] > 150 else (24, 24, 26, 255))
    d.text((48, 47), head, fill=(24, 24, 26, 255) if colour[1] > 150 else (250, 250, 246, 255), font=PP.rfont(19, True), anchor="mm")
    for k, line in enumerate(lines):
        d.text((48, 82 + k * 18), line, fill=(250, 250, 246, 255) if colour[1] < 150 else (24, 24, 26, 255), font=PP.rfont(13, True), anchor="mm")
    d.line([(14, 140), (82, 140)], fill=(250, 250, 246, 255) if colour[1] < 150 else (24, 24, 26, 255), width=2)
    return img


def _printer_top():
    img = Image.new("RGBA", (128, 120), (60, 62, 66, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([12, 10, 116, 44], fill=(120, 150, 160, 255), outline=(20, 20, 22, 255), width=3)
    d.text((64, 27), "TAG 1042-07", fill=(20, 30, 34, 255), font=PP.rfont(14, True), anchor="mm")
    for c in range(4):
        d.rounded_rectangle([14 + c * 26, 60, 34 + c * 26, 76], 3, fill=(90, 90, 96, 255))
    d.rounded_rectangle([14, 86, 60, 104], 3, fill=(60, 160, 70, 255))
    d.text((37, 95), "PRINT", fill=(240, 240, 236, 255), font=PP.rfont(10, True), anchor="mm")
    return img


def _printer_front():
    img = Image.new("RGBA", (128, 64), (60, 62, 66, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 24, 108, 32], fill=(16, 16, 18, 255))
    d.text((64, 50), "TAGPRINT", fill=(200, 200, 196, 255), font=PP.rfont(12, True), anchor="mm")
    return img


def _radio_face():
    """A handheld radio's front: the speaker grille, its small display, the keypad."""
    img = Image.new("RGBA", (48, 192), (34, 34, 38, 255))
    d = ImageDraw.Draw(img)
    for y in range(12, 80, 6):
        for x in range(8, 42, 6):
            d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(14, 14, 16, 255))
    d.rectangle([6, 90, 42, 112], fill=(60, 150, 90, 255), outline=(10, 10, 10, 255), width=2)
    d.text((24, 101), "OPS", fill=(10, 30, 16, 255), font=PP.rfont(11, True), anchor="mm")
    for r in range(4):
        for c in range(3):
            d.rounded_rectangle([7 + c * 12, 122 + r * 16, 16 + c * 12, 132 + r * 16], 2, fill=(70, 70, 76, 255))
    return img


def _keyboard_board():
    """The key locker's board: pale grey perforated, each hook's number under it."""
    img = Image.new("RGBA", (256, 256), (206, 208, 204, 255))
    d = ImageDraw.Draw(img)
    for y in range(8, 256, 16):
        for x in range(8, 256, 16):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(150, 152, 150, 255))
    return img


def _carton_face(title, sub):
    img = Image.new("RGBA", (128, 96), (226, 214, 186, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 10, 120, 50], fill=(250, 250, 246, 255), outline=(30, 70, 140, 255), width=3)
    d.text((64, 30), title, fill=(30, 70, 140, 255), font=PP.rfont(16, True), anchor="mm")
    d.text((64, 70), sub, fill=(40, 40, 42, 255), font=PP.rfont(12, True), anchor="mm")
    return img


def _hat_front():
    img = Image.new("RGBA", (96, 64), (244, 244, 240, 255))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([20, 14, 76, 44], 6, fill=(30, 70, 140, 255))
    d.text((48, 29), "OPS", fill=(250, 250, 246, 255), font=PP.rfont(18, True), anchor="mm")
    return img


def _lockbox_front():
    img = Image.new("RGBA", (192, 160), (196, 40, 34, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([28, 48, 164, 112], fill=(60, 20, 18, 255))
    for k, x in enumerate(range(44, 160, 22)):
        d.rectangle([x, 60, x + 8, 92], fill=(196, 160, 70, 255))
    d.rectangle([14, 8, 178, 40], fill=(250, 250, 246, 255))
    PP._fit_line(d, (96, 24), "GROUP LOCKBOX  C-1042", 22, 150, (24, 24, 26, 255))
    return img


def _strips():
    img = Image.new("RGBA", (K, K), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    for row, text in enumerate(STRIPS.values()):
        y0 = row * 128
        d.rectangle([0, y0, K - 1, y0 + 127], fill=(24, 24, 26, 255))
        d.rectangle([10, y0 + 10, K - 11, y0 + 117], outline=(220, 220, 214, 255), width=4)
        PP._fit_line(d, (K // 2, y0 + 64), text, 60, K - 70, (240, 240, 234, 255))
    return img


def makers():
    out = {"pi_ops_tag_danger": lambda: _tag("DANGER", (196, 30, 28), ["DO NOT", "OPERATE"]),
           "pi_ops_tag_caution": lambda: _tag("CAUTION", (236, 196, 30), ["SEE", "CLEARANCE"]),
           "pi_ops_printer_top": _printer_top, "pi_ops_printer_front": _printer_front, "pi_ops_radio": _radio_face,
           "pi_ops_keyboard": _keyboard_board, "pi_ops_glasses_box": lambda: _carton_face("SAFETY", "GLASSES  ANSI Z87"),
           "pi_ops_plugs_box": lambda: _carton_face("EAR PLUGS", "NRR 32"), "pi_ops_hat_front": _hat_front,
           "pi_ops_lockbox_front": _lockbox_front, "pi_ops_strips": _strips, "pi_ops_log": _log_sheet,
           "pi_ops_spines_0": lambda: _spines(0), "pi_ops_spines_1": lambda: _spines(1), "pi_ops_spines_2": lambda: _spines(2),
           "pi_ops_spines_3": lambda: _spines(3),
           "pi_ops_binder": lambda: _binder("WORK CONTROL", "PLAN OF THE DAY"),
           "pi_ops_tag_red": lambda: PP._solid((196, 30, 28), 801, 0.02), "pi_ops_tag_yellow": lambda: PP._solid((236, 196, 30), 802, 0.02),
           "pi_ops_printer_side": lambda: PP._solid((60, 62, 66), 803, 0.02), "pi_ops_radio_side": lambda: PP._solid((34, 34, 38), 804, 0.02),
           "pi_ops_knob": lambda: PP._solid((90, 90, 96), 805, 0.01), "pi_ops_hat": lambda: PP._solid((244, 244, 240), 806, 0.03),
           "pi_ops_muff": lambda: PP._solid((236, 200, 30), 807, 0.03), "pi_ops_torch": lambda: PP._solid((40, 40, 44), 808, 0.02),
           "pi_ops_torch_head": lambda: PP._solid((236, 200, 30), 809, 0.02), "pi_ops_brass": lambda: PP._solid((196, 160, 70), 810, 0.03),
           "pi_ops_lockbox": lambda: PP._solid((196, 40, 34), 811, 0.03),
           "pi_ops_lock_red": lambda: PP._solid((200, 30, 28), 812, 0.02), "pi_ops_lock_blue": lambda: PP._solid((30, 70, 170), 813, 0.02),
           "pi_ops_lock_yellow": lambda: PP._solid((236, 196, 30), 814, 0.02), "pi_ops_lock_green": lambda: PP._solid((40, 140, 60), 815, 0.02),
           "pi_ops_ktag_red": lambda: PP._solid((200, 40, 34), 816, 0.02), "pi_ops_ktag_yellow": lambda: PP._solid((236, 200, 40), 817, 0.02),
           "pi_ops_ktag_blue": lambda: PP._solid((40, 90, 190), 818, 0.02), "pi_ops_ktag_green": lambda: PP._solid((50, 150, 70), 819, 0.02)}
    for name in ("pi_ops_tag_danger", "pi_ops_tag_caution", "pi_ops_printer_top", "pi_ops_printer_front", "pi_ops_radio",
                 "pi_ops_glasses_box", "pi_ops_plugs_box", "pi_ops_hat_front", "pi_ops_lockbox_front", "pi_ops_binder", "pi_ops_log"):
        out[name] = (lambda f=out[name]: f().resize((128, 128), Image.LANCZOS))
    return out


def _log_sheet():
    img = Image.new("RGBA", (200, 190), (130, 96, 60, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 22, 190, 186], fill=(250, 250, 246, 255))
    d.text((100, 34), "WORK CONTROL LOG", fill=(24, 24, 26, 255), font=PP.rfont(16, True), anchor="mm")
    for k in range(9):
        y = 52 + k * 15
        d.line([(16, y), (184, y)], fill=(170, 170, 200, 255), width=1)
        if k < 6:
            d.text((20, y - 12), f"0{6 + k}{(k * 17) % 60:02d}", fill=(30, 40, 110, 255), font=PP.rfont(11, True))
            d.line([(58, y - 5), (60 + (k * 41) % 110 + 40, y - 6)], fill=(30, 40, 110, 255), width=2)
    return img


def _binder(title, sub):
    img = Image.new("RGBA", (180, 230), (30, 70, 140, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 30, 156, 120], fill=(246, 246, 240, 255), outline=(30, 30, 30, 255), width=3)
    d.text((90, 58), title, fill=(24, 24, 26, 255), font=PP.rfont(18, True), anchor="mm")
    d.text((90, 84), sub, fill=(24, 24, 26, 255), font=PP.rfont(15, True), anchor="mm")
    d.text((90, 106), "OPS", fill=(30, 70, 140, 255), font=PP.rfont(18, True), anchor="mm")
    return img


SHARP = {"pi_ops_tag_danger", "pi_ops_tag_caution", "pi_ops_printer_top", "pi_ops_printer_front", "pi_ops_radio", "pi_ops_glasses_box",
         "pi_ops_plugs_box", "pi_ops_hat_front", "pi_ops_lockbox_front", "pi_ops_strips", "pi_ops_binder", "pi_ops_keyboard", "pi_ops_log",
         "pi_ops_spines_0", "pi_ops_spines_1", "pi_ops_spines_2", "pi_ops_spines_3"}


# ------------------------------------------------------------------------------------------------ registration
PANELS = {CON_KEY: {"display": "Work Control Console", "width": 3, "rows": 1, "item3d": True, "labels": [" / ".join(p) for p in POSITIONS],
                    "shared": True},
          OOS_KEY: {"display": "Equipment Out of Service Board", "width": 3, "item3d": True},
          SB_KEY: {"display": "Shift Schedule Board", "width": 3, "item3d": True},
          CL_KEY: {"display": "Master Clock", "width": 1, "rows": 1, "item3d": True},
          RD_KEY: {"display": "Radio Charging Rack", "width": 1, "rows": 1, "item3d": True},
          PPE_KEY: {"display": "PPE Shelf", "width": 2, "rows": 1, "item3d": True}}
DRAWINGS = {CON_KEY: ops_console, OOS_KEY: ops_oos_board, SB_KEY: ops_schedule_board, CL_KEY: ops_wall_clock, RD_KEY: ops_radio_rack,
            PPE_KEY: ops_ppe_shelf}
BOXES = {CON_KEY: ops_console_boxes, OOS_KEY: ops_oos_board_boxes, SB_KEY: ops_schedule_board_boxes, CL_KEY: ops_wall_clock_boxes,
         RD_KEY: ops_radio_rack_boxes, PPE_KEY: ops_ppe_shelf_boxes}
DESK = T.DESK
CRAFTING = {
    CON_KEY: (["GCG", "WPW", "P P"], dict(DESK, P="#c:plates/steel"), 1),
    OOS_KEY: (["GGG", "GCG", " P "], DESK, 1),
    SB_KEY: (["QQQ", "QIQ"], {"Q": "minecraft:quartz_slab", "I": "minecraft:blue_dye"}, 1),
    CL_KEY: (["PCP"], {"P": "#c:plates/iron", "C": "minecraft:clock"}, 1),
    RD_KEY: (["RRR", "PCP"], {"R": "jugcraft:basic_circuit", "P": "#c:plates/steel", "C": "minecraft:copper_ingot"}, 1),
    PPE_KEY: (["LLL", "P P"], {"L": "minecraft:leather_helmet", "P": "#c:plates/iron"}, 1),
    "ops_key_locker": (["PGP", "PIP", "PPP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "I": "minecraft:tripwire_hook"}, 1),
    "ops_tag_rack": (["PRP", "PYP"], {"P": "#c:plates/iron", "R": "minecraft:red_dye", "Y": "minecraft:yellow_dye"}, 2),
    "ops_lockbox": (["PGP", "PIP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "I": "minecraft:iron_ingot"}, 1),
    "ops_procedure_shelf": (["PBP", "PBP", "PBP"], {"P": "#c:plates/iron", "B": "minecraft:book"}, 2),
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(PP.variants(key)) for p in range(2 * PANELS[key]["width"])}


register()
