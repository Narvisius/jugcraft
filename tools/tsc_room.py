"""The Technical Support Center (round 26, #12; Justin: "Enlarge into the kitchen", live: "Emergency class board", "Plant
data video wall", "Desk screens and clocks"; "New call-centre consoles"; "Status boards", "Communications corner",
"Records and procedures", "Frisker at the door"). The second floor's TSC laid out as a real plant's (NUREG-0696: room for
the licensee's 20 and the NRC's 5, a private NRC room, plant data displays, records, communications; a plant's emergency
plan: the Site Emergency Manager and the coordinators by discipline, the ENS communicator, the status boards):

- the TSC consoles: call-centre desks three blocks wide, each two positions (the position's name on a plate on the
  divider, lettered live), each position with two monitors showing its own live data, a keyboard, a phone, a headset, its
  position binder and log; the command desk (the Site Emergency Manager and the log keeper) is one of them;
- the communications console: the ENS (to the NRC), HPN and offsite (state and county) communicators, each with a phone
  set and a monitor, the radio base station;
- the video wall (three displays, two blocks square each, on the front wall): the safety parameter display (critical
  safety functions and the plant's parameters), radiological and meteorological data, electrical status and the event
  timeline; over it the emergency classification board (NO EMERGENCY to GENERAL EMERGENCY, lit as the plant stands) with
  the local time and the event clock;
- the status boards (whiteboards: staffing, priorities, the sequence of events and protective action recommendations);
- the area radiation monitor and the frisking station inside the door, the accountability card reader outside it;
- records: the flat file of drawings and the procedure shelves.

What moves is the game's (client/EdgTelemetry.java tsc; nuclear/TscRoom.java). Models in model pixels for the piece
facing north: x across, y up, z from its back (z 0) out to its front, where its user is (+z)."""
import math

from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP
import relay_room as RR

K = 512

# ------------------------------------------------------------------------------------------------ the positions
# Each console's two positions (its variant), as on the plates on its divider and its screens.
POSITIONS = [("OPS COORDINATOR", "OPS RECORDER"), ("RAD COORDINATOR", "DOSE ASSESSMENT"), ("ENGINEERING COORD", "REACTOR ENGINEER"),
             ("ELECTRICAL ENGINEER", "MECHANICAL ENGINEER"), ("MAINTENANCE COORD", "MAINT PLANNER"), ("ADMIN COORDINATOR", "SECURITY COORD"),
             ("SITE EMERGENCY MGR", "TSC LOG KEEPER")]
COMMS = ("ENS COMMUNICATOR", "HPN COMMUNICATOR", "OFFSITE COMMUNICATOR")

# ------------------------------------------------------------------------------------------------ the console
TC_KEY, CC_KEY = "tsc_console", "tsc_comms_console"
DESK_Y = 12.2               # the worktop's top
TOP_T = 0.8                 # its thickness
DIV_Z = (1.0, 2.0)          # the back divider's thickness (the worktop runs from its face)
DIV_TOP = 20.8              # the back divider's top (its cap over it)
WING_TOP, WING_Z = 19.0, 9.0
PLATE = (21.2, 24.0, 1.15, 1.85)     # the position plates on the divider's cap: y0, y1, z0, z1
SCREEN_Z = 5.65             # the monitors' screens' faces
TOP, BODY, EDGE, FABRIC, TRIM = "pi_tsc_top", "pi_tsc_body", "pi_tsc_edge", "pi_tsc_fabric", "pi_tsc_trim"
BLACK = "pi_cap_black"


def _desk(W, dividers):
    """The desk three blocks wide: the worktop on its end panels and middle leg, the modesty panel, the cable trough,
    the fabric divider along its back with its aluminium cap, its end wings, the dividers between positions."""
    y0 = DESK_Y - TOP_T
    out = [(0.2, y0, DIV_Z[1], W - 0.2, DESK_Y, 15.6, {"*": EDGE, "up": TOP, "down": BODY}),
           (0.2, 0, 2.6, 1.2, y0, 15.2, BODY), (W - 1.2, 0, 2.6, W - 0.2, y0, 15.2, BODY),
           (1.2, 1.5, DIV_Z[1], W - 1.2, y0, 2.8, BODY), (1.2, y0 - 1.4, 2.8, W - 1.2, y0, 4.6, {"*": BODY, "up": None})]
    out[1] = (0.2, 0.3, 2.6, 1.2, y0, 15.2, BODY)
    out[2] = (W - 1.2, 0.3, 2.6, W - 0.2, y0, 15.2, BODY)
    out.append((W / 2 - 0.5, 0.3, 3.0, W / 2 + 0.5, y0, 14.0, BODY))
    # Levelling glides under the end panels and the middle leg.
    for x in (0.7, W / 2, W - 0.7):
        for z in (3.4, 14.0 if x == W / 2 else 14.4):
            out.append((x - 0.4, 0.0, z - 0.4, x + 0.4, 0.3, z + 0.4, BLACK))
    # The back divider (fabric in an aluminium frame) and its cap; the end wings; between the positions.
    out += [(0.2, DESK_Y, DIV_Z[0], W - 0.2, DIV_TOP, DIV_Z[1], {"*": TRIM, "north": FABRIC, "south": FABRIC}),
            (0.1, DIV_TOP, DIV_Z[0] - 0.1, W - 0.1, DIV_TOP + 0.4, DIV_Z[1] + 0.1, TRIM)]
    for x in [0.2, W - 1.0] + [d - 0.4 for d in dividers]:
        top = WING_TOP
        out += [(x, DESK_Y, DIV_Z[1], x + 0.8, top, WING_Z, {"*": TRIM, "west": FABRIC, "east": FABRIC}),
                (x - 0.1, top, DIV_Z[1], x + 0.9, top + 0.4, WING_Z + 0.1, TRIM)]
    return out


MON_HALF = 4.3       # a monitor's half width (a 24-inch screen: 8.6 px)


def _monitor(cx, tex="pi_tsc_screen!"):
    """A 24-inch monitor on the stand's arm: its black case, the screen a hair proud of it."""
    h = MON_HALF
    return [(cx - h, 14.4, 5.0, cx + h, 20.2, SCREEN_Z - 0.05, {"*": "pi_tsc_case", "south": "pi_tsc_case"}),
            (cx - h + 0.25, 14.65, SCREEN_Z - 0.05, cx + h - 0.25, 19.95, SCREEN_Z, {"*": "pi_tsc_case", "south": tex}),
            (cx - 0.6, 15.6, 4.6, cx + 0.6, 17.6, 5.0, BLACK)]


def _stand(px, half):
    """A dual (or single) monitor stand: its weighted base, the pole, the cross arm behind the screens."""
    return [(px - 2.4, DESK_Y, 3.2, px + 2.4, DESK_Y + 0.25, 6.2, "pi_steel_dark"),
            (px - 0.35, DESK_Y + 0.25, 4.0, px + 0.35, 18.0, 4.6, "pi_chrome"),
            (px - half, 16.3, 4.0, px + half, 16.9, 4.6, "pi_chrome")]


def _keyboard(x0, z0):
    return [(x0, DESK_Y, z0, x0 + 7.0, DESK_Y + 0.45, z0 + 2.3, {"*": BLACK, "up": "pi_tsc_keyboard!"}),
            (x0 + 7.8, DESK_Y, z0 - 0.2, x0 + 10.0, DESK_Y + 0.05, z0 + 2.4, "pi_pnl_black"),
            (x0 + 8.4, DESK_Y + 0.05, z0 + 0.4, x0 + 9.4, DESK_Y + 0.45, z0 + 1.9, BLACK)]


def _phone(x0, z0, handset=BLACK, lcd=True):
    """A desk phone: its sloped body (in two steps), the keys and display on its top, the handset in its cradle."""
    out = [(x0, DESK_Y, z0, x0 + 3.8, DESK_Y + 0.55, z0 + 3.0, {"*": "pi_tsc_phone_side", "up": "pi_tsc_phone!"}),
           (x0, DESK_Y + 0.55, z0, x0 + 3.8, DESK_Y + 0.95, z0 + 1.2, {"*": "pi_tsc_phone_side", "up": "pi_tsc_phone_side"}),
           (x0 + 0.15, DESK_Y + 0.95, z0 + 0.1, x0 + 1.1, DESK_Y + 1.5, z0 + 2.9, handset),
           (x0 + 0.05, DESK_Y + 0.55, z0 + 2.3, x0 + 1.2, DESK_Y + 1.1, z0 + 2.95, handset)]
    return out


def _headset(cx, z):
    """A headset on its stand: the base, the post, the band over it, the ear cups, the microphone boom."""
    return [(cx - 0.8, DESK_Y, z - 0.8, cx + 0.8, DESK_Y + 0.2, z + 0.8, BLACK),
            (cx - 0.2, DESK_Y + 0.2, z - 0.2, cx + 0.2, DESK_Y + 3.0, z + 0.2, "pi_steel_dark"),
            (cx - 1.2, DESK_Y + 3.0, z - 0.4, cx + 1.2, DESK_Y + 3.35, z + 0.4, BLACK),
            (cx - 1.45, DESK_Y + 1.7, z - 0.65, cx - 1.05, DESK_Y + 3.0, z + 0.65, "pi_tsc_cushion"),
            (cx + 1.05, DESK_Y + 1.7, z - 0.65, cx + 1.45, DESK_Y + 3.0, z + 0.65, "pi_tsc_cushion"),
            (cx - 1.4, DESK_Y + 1.9, z + 0.65, cx - 1.2, DESK_Y + 2.1, z + 2.2, BLACK)]


def _binder(x0, z0, tex="pi_tsc_binder"):
    return [(x0, DESK_Y, z0, x0 + 3.6, DESK_Y + 0.7, z0 + 4.6, {"*": "pi_tsc_binder_side", "up": tex + "!", "west": "pi_tsc_binder_spine!"})]


def _clipboard(x0, z0):
    return [(x0, DESK_Y, z0, x0 + 3.2, DESK_Y + 0.12, z0 + 3.0, {"*": "pi_tsc_hardboard", "up": "pi_tsc_log!"}),
            (x0 + 1.0, DESK_Y + 0.12, z0 + 0.15, x0 + 2.2, DESK_Y + 0.35, z0 + 0.55, "pi_chrome"),
            (x0 + 2.4, DESK_Y + 0.12, z0 + 1.0, x0 + 2.55, DESK_Y + 0.27, z0 + 2.6, "pi_pen_blue")]


def _tower(x0):
    """The position's computer under the desk: its case, its front with the power button lit."""
    return [(x0, 0.3, 4.6, x0 + 3.4, 8.6, 12.8, {"*": "pi_tsc_body", "south": "pi_tsc_pc!"})]


def _plate(px, half=7.4):
    """A position plate on the divider's cap, facing the room behind the position (its name lettered live)."""
    y0, y1, z0, z1 = PLATE
    return [(px - half, y0, z0, px + half, y1, z1, {"*": "pi_tsc_plate", "north": "pi_tsc_plate"})]


# Each position's things across the desk (its monitors' centre, its keyboard's left end, the phone, the headset, its
# binder, its log, its computer under the desk): the left position's, the right's mirrored round the middle divider.
SEATS = [{"mon": 10.0, "kbd": 5.0, "phone": 16.6, "headset": 22.0, "binder": 1.6, "clip": 12.6, "tower": 16.0},
         {"mon": 38.0, "kbd": 33.0, "phone": 27.6, "headset": 26.0, "binder": 43.6, "clip": 28.2, "tower": 28.6}]
MON_GAP = 4.5        # each of a position's two monitors' centre from the pair's


def tsc_console_boxes():
    W = 48
    out = _desk(W, [24.0])
    for p in SEATS:
        px = p["mon"]
        out += _stand(px, 5.4) + _monitor(px - MON_GAP) + _monitor(px + MON_GAP)
        out += _keyboard(p["kbd"], 9.4) + _phone(p["phone"], 6.4) + _headset(p["headset"], 7.4)
        out += _binder(p["binder"], 10.2) + _clipboard(p["clip"], 12.4) + _tower(p["tower"])
        out += _plate(px)
    return out


def tsc_comms_boxes():
    """The communications console: three positions, each one monitor; the ENS phone (red), the HPN phone, the offsite
    notification phone with its ringdown line; the radio base station with its desk microphone in the middle."""
    W = 48
    out = _desk(W, [16.0, 32.0])
    for k, px in enumerate((8.0, 24.0, 40.0)):
        out += _stand(px, 1.0) + _monitor(px)
        out += _keyboard(px - 5.0, 9.6)
        handset = ("pi_cap_red", BLACK, "pi_tsc_beige")[k]
        out += _phone(px - 7.2, 12.2, handset) if k == 1 else _phone(px + 3.4, 6.4, handset)
        if k == 2:
            out += _phone(px - 7.4, 6.2, BLACK)
        out += _clipboard(px - 7.0, 12.4) if k != 1 else []
        out += _plate(px, 7.0)
    # The radio base station on the middle position, its desk microphone on a gooseneck.
    x0 = 24.0 + 4.6
    out += [(x0, DESK_Y, 5.6, x0 + 3.0, DESK_Y + 2.0, 9.4, {"*": "pi_tsc_body", "south": "pi_tsc_radio!"}),
            (26.4, DESK_Y, 12.4, 28.0, DESK_Y + 0.3, 14.0, BLACK),
            (27.0, DESK_Y + 0.3, 13.0, 27.4, DESK_Y + 3.4, 13.4, "pi_steel_dark"),
            (26.8, DESK_Y + 3.4, 12.8, 27.6, DESK_Y + 4.3, 13.6, BLACK)]
    out += _tower(2.0) + _tower(42.6)
    return out


NRC_KEY = "tsc_nrc_desk"
NRC_MON = (12.0, 36.0)      # the NRC desk's two monitors' centres


def tsc_nrc_desk_boxes():
    """The NRC site team's desk in its glass room: a glass top on a steel frame three blocks long, two monitors (live),
    a phone at each end, the conference speakerphone in the middle, keyboards, a binder and a log."""
    W = 48
    top = {"*": "pi_glass_edge", "up": "pi_glass_top", "down": "pi_glass_top"}
    out = [(0.2, DESK_Y - 0.5, 1.0, W - 0.2, DESK_Y, 15.0, top),
           (0.6, DESK_Y - 1.2, 1.5, W - 0.6, DESK_Y - 0.5, 2.5, "pi_steel_dark"), (0.6, DESK_Y - 1.2, 13.5, W - 0.6, DESK_Y - 0.5, 14.5, "pi_steel_dark")]
    for x in (0.6, W / 2 - 0.5, W - 1.6):
        for z in (1.5, 13.5):
            out.append((x, 0, z, x + 1.0, DESK_Y - 1.2, z + 1.0, "pi_steel_dark"))
    for px in NRC_MON:
        out += _stand(px, 1.0) + _monitor(px) + _keyboard(px - 3.5, 9.4)
    out += _phone(2.0, 6.4) + _phone(W - 5.8, 6.4, "pi_cap_red")
    out += _clipboard(2.2, 11.0) + _binder(W - 5.0, 10.0)
    # The speakerphone: its three-legged body, the grille and keys on its top.
    out += [(21.2, DESK_Y, 5.2, 26.8, DESK_Y + 0.7, 10.8, {"*": "pi_tsc_phone_side", "up": "pi_tsc_speaker!"}),
            (22.4, DESK_Y + 0.7, 6.4, 25.6, DESK_Y + 0.9, 9.6, {"*": "pi_tsc_phone_side", "up": "pi_tsc_speaker_top!"})]
    out += _clipboard(22.4, 11.6)
    return out


# The live parts of a console (client/LivePanelRenderer.java): the plates' names, each screen's title and lines.
SCREEN_LINES = 7
LINE_STEP = 0.7


def _screen_live(f, cx, stem):
    """A monitor's live text: its title in the title bar; each line a name on the left and its value on the right (the
    game's font is proportional: the values line up on the right)."""
    top = 19.95 - 0.45
    x = cx - MON_HALF + 0.45
    xr = cx + MON_HALF - 0.45
    f.text(x, top, SCREEN_Z + 0.01, 0.46, (250, 250, 250), f"{stem}_t")
    for i in range(SCREEN_LINES - 1):
        y = top - 0.8 - i * LINE_STEP
        f.text(x, y, SCREEN_Z + 0.01, 0.42, (170, 230, 255), f"{stem}_l{i}")
        f.text(xr, y, SCREEN_Z + 0.01, 0.42, (240, 240, 240), f"{stem}_r{i}", "right")


def tsc_console(label=None):
    f = PP.Front(3, 700)
    for k, p in enumerate(SEATS):
        px = p["mon"]
        f.text(px, (PLATE[0] + PLATE[1]) / 2, PLATE[3] + 0.01, 1.15, (246, 246, 240), f"ts_name{k}", "center")
        for m, dx in enumerate((-MON_GAP, MON_GAP)):
            _screen_live(f, px + dx, f"ts_p{k}_m{m}")
    return f


def tsc_nrc_desk():
    f = PP.Front(3, 702)
    for k, px in enumerate(NRC_MON):
        _screen_live(f, px, f"ts_n{k}")
    return f


def tsc_comms_console():
    f = PP.Front(3, 701)
    for k, px in enumerate((8.0, 24.0, 40.0)):
        f.text(px, (PLATE[0] + PLATE[1]) / 2, PLATE[3] + 0.01, 1.05, (246, 246, 240), f"ts_cname{k}", "center")
        _screen_live(f, px, f"ts_c{k}")
    return f


# ------------------------------------------------------------------------------------------------ the video wall
VW_KEY, CB_KEY, SB_KEY, AM_KEY, FR_KEY = "tsc_video_wall", "tsc_class_board", "tsc_status_board", "tsc_area_monitor", "tsc_frisker"
VW_SCREEN = (0.9, 2.8, 31.1, 29.2)
VW_Z = 2.4
VW_LABELS = ["SPDS", "RAD MET", "ELEC EVENTS"]
NAVY, PANEL_BG = (12, 20, 38, 255), (20, 30, 52, 255)
CYAN, WHITE_T, GREY_T = (120, 210, 255, 255), (238, 240, 244, 255), (150, 160, 178, 255)
CSF = ["SUBCRITICALITY", "CORE COOLING", "HEAT SINK", "RCS INTEGRITY", "CONTAINMENT", "RCS INVENTORY"]
SPDS_ROWS = [("RX POWER", "%"), ("TAVG", "DEG F"), ("PZR PRESSURE", "PSIG"), ("PZR LEVEL", "%"), ("SG NR LEVEL", "%"), ("RCS FLOW", "%"),
             ("RODS INSERTED", "%"), ("CORE EXIT TC", "DEG F")]
RAD_ROWS = [("CR INTAKE A", "CPM"), ("CR INTAKE B", "CPM"), ("CR HVAC MODE", ""), ("TSC AREA", "MR/HR"), ("PLANT VENT", ""),
            ("WIND SPEED", "MPH"), ("WIND FROM", "DEG"), ("STABILITY", "CLASS")]
ELEC_ROWS = [("MAIN GENERATOR", "MWE"), ("MAIN XFMR", ""), ("480V LC 1A", ""), ("480V LC 1B", ""), ("EDG 1A", ""), ("EDG 1B", "")]
SOE_ROWS = 6


def _vw_frame(f, title, v):
    x0, y0, x1, y1 = VW_SCREEN
    f.R(0, 0, 32, 32, fill=(30, 30, 32, 255))
    f.R(x0, y0, x1, y1, fill=NAVY)
    f.R(x0, y1 - 2.0, x1, y1, fill=(28, 70, 140, 255))
    f.T(x0 + 0.6, y1 - 1.0, "UNIT 1", 10, fill=WHITE_T, bold=True, anchor="lm")
    RR._fit(f, 16.0, y1 - 1.0, title, 11, 18.0, fill=WHITE_T)
    f.text(x1 - 0.6, y1 - 1.0, VW_Z + 0.02, 0.9, (238, 240, 244), "ts_clock", "right")
    f.R(x0, y0, x1, y0 + 1.4, fill=(16, 24, 44, 255))
    f.T(x0 + 0.6, y0 + 0.7, f"PLANT COMPUTER  TSC-VW{v + 1}", 8, fill=GREY_T, anchor="lm")


def _table(f, x0, x1, y_top, rows, stem, step=1.75):
    """A table of a parameter's name, its live value (right-aligned before its unit) and its unit."""
    for i, (name, unit) in enumerate(rows):
        y = y_top - i * step
        f.R(x0, y - step / 2 + 0.1, x1, y + step / 2 - 0.1, fill=PANEL_BG)
        f.T(x0 + 0.4, y, name, 10, fill=GREY_T, bold=True, anchor="lm")
        if unit:
            f.T(x1 - 0.4, y, unit, 8, fill=GREY_T, bold=True, anchor="rm")
        f.text(x1 - (3.6 if unit else 0.4), y, VW_Z + 0.02, 0.85, (120, 255, 150), f"{stem}{i}", "right")


def tsc_video_wall(label="SPDS"):
    v = VW_LABELS.index(label)
    f = PP.Front(2, 710 + v)
    x0, y0, x1, y1 = VW_SCREEN
    if v == 0:
        _vw_frame(f, "SAFETY PARAMETER DISPLAY", v)
        # The critical safety functions' status tree (green satisfied, red challenged: live), the parameters.
        f.T(x0 + 6.6, y1 - 3.0, "CRITICAL SAFETY FUNCTIONS", 9, fill=CYAN, bold=True)
        for i, name in enumerate(CSF):
            yt = y1 - 4.6 - i * 2.6
            f.R(x0 + 0.6, yt - 1.1, x0 + 12.6, yt + 1.1, fill=(40, 48, 66, 255), outline=(90, 100, 120, 255), width=2)
            RR._fit(f, x0 + 6.6, yt, name, 11, 11.4, fill=WHITE_T)
            f.glow(x0 + 0.7, yt - 1.0, x0 + 12.5, yt + 1.0, VW_Z + 0.01, (40, 200, 90), f"ts_csf{i}", 0.55, (230, 50, 40))
        _table(f, x0 + 13.4, x1 - 0.6, y1 - 3.6, SPDS_ROWS, "ts_spds", 1.85)
        f.R(x0 + 13.4, y0 + 1.8, x1 - 0.6, y0 + 4.0, fill=(40, 48, 66, 255))
        f.glow(x0 + 13.5, y0 + 1.9, x1 - 0.7, y0 + 3.9, VW_Z + 0.01, (40, 200, 90), "ts_rx_band", 0.6, (230, 50, 40))
        f.text((x0 + 13.4 + x1 - 0.6) / 2, y0 + 2.9, VW_Z + 0.03, 1.0, (250, 250, 250), "ts_rx_state", "center")
    elif v == 1:
        _vw_frame(f, "RADIOLOGICAL AND MET DATA", v)
        _table(f, x0 + 0.6, x0 + 15.4, y1 - 3.4, RAD_ROWS, "ts_rad", 1.85)
        # The site's sixteen sectors round the plant (the downwind sector live).
        cx, cy, r = x0 + 22.8, y0 + 13.8, 5.4
        f.E(cx, cy, r, fill=(24, 40, 30, 255), outline=(90, 140, 100, 255), width=2)
        for k in range(16):
            a = math.radians(90 - (k * 22.5 - 11.25))
            f.line([(cx, cy), (cx + math.cos(a) * r, cy + math.sin(a) * r)], (60, 100, 70, 255), 1)
        for k, name in enumerate(("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")):
            a = math.radians(90 - k * 22.5)
            f.T(cx + math.cos(a) * (r + 1.05), cy + math.sin(a) * (r + 1.05), name, 8 if len(name) < 3 else 7, fill=GREY_T, bold=True)
        for rr in (1.7, 3.6):
            f.E(cx, cy, rr, outline=(60, 100, 70, 255), width=1)
        f.E(cx, cy, 0.5, fill=(240, 200, 40, 255))
        f.T(cx, y0 + 6.2, "2 MI / 5 MI / 10 MI", 8, fill=GREY_T)
        f.T(cx, y1 - 3.0, "EMERGENCY PLANNING ZONE", 9, fill=CYAN, bold=True)
        f.text(cx, y0 + 3.4, VW_Z + 0.02, 0.9, (255, 210, 80), "ts_downwind", "center")
        f.R(x0 + 0.6, y0 + 1.8, x0 + 15.4, y0 + 4.6, fill=(40, 48, 66, 255))
        f.glow(x0 + 0.7, y0 + 1.9, x0 + 15.3, y0 + 4.5, VW_Z + 0.01, (40, 200, 90), "ts_release_band", 0.6, (230, 50, 40))
        f.text(x0 + 8.0, y0 + 3.2, VW_Z + 0.03, 0.95, (250, 250, 250), "ts_release", "center")
    else:
        _vw_frame(f, "ELECTRICAL AND EVENTS", v)
        _table(f, x0 + 0.6, x0 + 14.2, y1 - 3.4, ELEC_ROWS, "ts_elec", 1.85)
        f.T(x0 + 22.6, y1 - 3.0, "EVENT TIMELINE", 9, fill=CYAN, bold=True)
        for i in range(SOE_ROWS):
            y = y1 - 4.8 - i * 1.9
            f.R(x0 + 15.0, y - 0.85, x1 - 0.6, y + 0.85, fill=PANEL_BG)
            f.text(x0 + 15.4, y, VW_Z + 0.02, 0.75, (255, 210, 80), f"ts_soe_t{i}")
            f.text(x0 + 19.0, y, VW_Z + 0.02, 0.62, (238, 240, 244), f"ts_soe_e{i}")
        f.R(x0 + 0.6, y0 + 1.8, x0 + 14.2, y0 + 5.6, fill=(40, 48, 66, 255))
        f.T(x0 + 7.4, y0 + 4.7, "EMERGENCY CLASS", 9, fill=GREY_T, bold=True)
        f.text(x0 + 7.4, y0 + 2.9, VW_Z + 0.03, 0.85, (255, 255, 255), "ts_class", "center")
    return f


def tsc_video_wall_boxes():
    x0, y0, x1, y1 = VW_SCREEN
    F = f"pi_{VW_KEY}_f"
    return [(6.0, 8.0, 0.1, 26.0, 24.0, 0.9, "pi_steel_dark"), (11.0, 4.0, 0.1, 21.0, 28.0, 0.9, "pi_steel_dark"),
            (0.3, 2.2, 0.9, 31.7, 29.8, VW_Z - 0.1, {"*": "pi_tsc_case", "south": "pi_tsc_case"}),
            (x0, y0, VW_Z - 0.1, x1, y1, VW_Z, {"*": "pi_tsc_case", "south": F}),
            (17.2, 2.3, VW_Z - 0.1, 19.6, 2.7, VW_Z - 0.02, "pi_chrome")]


# ------------------------------------------------------------------------------------------------ the class board
CLASSES = [("NO", "EMERGENCY"), ("UNUSUAL", "EVENT"), ("ALERT",), ("SITE AREA", "EMERGENCY"), ("GENERAL", "EMERGENCY")]
CLASS_COLOURS = [(60, 200, 90), (240, 240, 236), (255, 200, 40), (255, 130, 30), (235, 40, 36)]
CB_WIN = [(2.0 + i * 8.9, 13.6, 2.0 + i * 8.9 + 8.3, 24.6) for i in range(5)]
CB_D = 2.4
CB_CLOCKS = [(3.0, 3.4, 15.0, 9.4, "LOCAL TIME", "ts_clock"), (17.0, 3.4, 31.0, 9.4, "EVENT TIME", "ts_event"),
             (33.0, 3.4, 45.0, 9.4, "DECLARED AT", "ts_declared")]


def tsc_class_board():
    f = PP.Front(3, 720)
    f.R(0.6, 1.0, 47.4, 31.0, fill=(36, 38, 42, 255))
    f.R(1.2, 26.0, 46.8, 30.4, fill=(150, 28, 30, 255))
    RR._fit(f, 24.0, 28.8, "EMERGENCY CLASSIFICATION", 20, 44.0, fill=(250, 250, 246, 255))
    RR._fit(f, 24.0, 26.9, "UNIT 1   EPIP-01 EMERGENCY ACTION LEVELS", 10, 44.0, fill=(250, 220, 210, 255))
    for (x0, y0, x1, y1), lines, colour in zip(CB_WIN, CLASSES, CLASS_COLOURS):
        f.R(x0, y0, x1, y1, fill=(214, 214, 206, 255))
        for i, w in enumerate(lines):
            RR._fit(f, (x0 + x1) / 2, (y0 + y1) / 2 + (len(lines) - 1) * 1.05 - i * 2.1, w, 14, x1 - x0 - 0.6)
    for i, (x0, y0, x1, y1) in enumerate(CB_WIN):
        c = CLASS_COLOURS[i]
        f.glow(x0, y0, x1, y1, CB_D + 0.35, c, f"ts_cls{i}", 0.45)
    for x0, y0, x1, y1, title, ch in CB_CLOCKS:
        f.R(x0, y0, x1, y1, fill=(16, 16, 18, 255))
        f.R(x0 + 0.35, y0 + 0.35, x1 - 0.35, y1 - 2.0, fill=(30, 6, 6, 255))
        RR._fit(f, (x0 + x1) / 2, y1 - 1.0, title, 10, x1 - x0 - 0.6, fill=(230, 230, 226, 255))
        f.text((x0 + x1) / 2, (y0 + 0.35 + y1 - 2.0) / 2, CB_D + 0.25, 2.2, (255, 60, 40), ch, "center")
    return f


def tsc_class_board_boxes():
    F = f"pi_{CB_KEY}_f"
    face = {"*": "pi_pnl_black", "south": F}
    b = [(0.6, 1.0, 0.1, 47.4, 31.0, CB_D, {"*": "pi_tsc_body", "south": F})]
    # Each window set back in its frame (the lit glass the game's); the clocks' bezels.
    for x0, y0, x1, y1 in CB_WIN:
        b += [(x0 - 0.3, y0 - 0.3, CB_D, x0, y1 + 0.3, CB_D + 0.4, face), (x1, y0 - 0.3, CB_D, x1 + 0.3, y1 + 0.3, CB_D + 0.4, face),
              (x0, y0 - 0.3, CB_D, x1, y0, CB_D + 0.4, face), (x0, y1, CB_D, x1, y1 + 0.3, CB_D + 0.4, face)]
    for x0, y0, x1, y1, *_ in CB_CLOCKS:
        b += [(x0, y0, CB_D, x0 + 0.35, y1, CB_D + 0.2, face), (x1 - 0.35, y0, CB_D, x1, y1, CB_D + 0.2, face),
              (x0 + 0.35, y0, CB_D, x1 - 0.35, y0 + 0.35, CB_D + 0.2, face)]
    # Its fixings to the wall.
    for x in (2.0, 46.0):
        for y in (2.0, 30.0):
            b.append((x - 0.35, y - 0.35, CB_D, x + 0.35, y + 0.35, CB_D + 0.12, "pi_steel"))
    return b


# ------------------------------------------------------------------------------------------------ the status boards
SB_LABELS = ["STAFFING", "PRIORITIES", "EVENTS", "NRC"]
SB_FACE = (1.0, 1.8, 47.0, 30.4)
SB_Z = 1.0
# The positions on the staffing board, the T-cards against them the players in the TSC (live: in the order they came in).
STAFF = ["SITE EMERGENCY MGR", "OPS COORDINATOR", "RAD COORDINATOR", "ENGINEERING COORD", "MAINTENANCE COORD", "ENS COMMUNICATOR",
         "SECURITY COORD", "ADMIN COORDINATOR"]
NRC_FIELDS = ["CLASSIFICATION", "DECLARED", "ENS CALL BY", "EVENT TIME"]
PRI_ROWS = 7     # the priorities board's lines (live: worked out from the plant, the class and its deadlines)


def tsc_status_board(label="STAFFING"):
    v = SB_LABELS.index(label)
    f = PP.Front(3, 730 + v)
    x0, y0, x1, y1 = SB_FACE
    f.R(0, 0, 48, 32, fill=(190, 194, 198, 255))
    f.R(x0, y0, x1, y1, fill=(246, 247, 244, 255))
    ink, red = (24, 26, 30, 255), (170, 30, 30, 255)
    if v == 0:
        f.R(x0, y1 - 2.4, x1, y1, fill=(30, 70, 140, 255))
        RR._fit(f, 24.0, y1 - 1.2, "TSC STAFFING", 16, 40.0, fill=(250, 250, 246, 255))
        for name, x in (("POSITION", x0 + 1.0), ("NAME", x0 + 21.0), ("IN", x0 + 37.0)):
            f.T(x, y1 - 3.4, name, 10, fill=ink, bold=True, anchor="lm")
        for i, pos in enumerate(STAFF):
            y = y1 - 5.0 - i * 2.9
            f.line([(x0 + 0.6, y - 1.45), (x1 - 0.6, y - 1.45)], (150, 150, 150, 255), 1)
            f.T(x0 + 1.0, y, pos, 11, fill=ink, bold=True, anchor="lm")
            f.R(x0 + 20.4, y - 1.05, x0 + 34.6, y + 1.05, fill=(250, 236, 120, 255), outline=(150, 130, 40, 255), width=2)
            f.text(x0 + 21.0, y, SB_Z + 0.06, 0.8, (24, 26, 30), f"ts_staff{i}")
            f.text(x0 + 37.0, y, SB_Z + 0.06, 0.8, (30, 40, 110), f"ts_in{i}")
    elif v == 1:
        f.R(x0, y1 - 2.4, x1, y1, fill=(150, 28, 30, 255))
        RR._fit(f, 24.0, y1 - 1.2, "TSC PRIORITIES", 16, 40.0, fill=(250, 250, 246, 255))
        for i in range(PRI_ROWS):
            f.text(x0 + 1.4, y1 - 4.6 - i * 3.3, SB_Z + 0.06, 1.1, (170, 30, 30) if i == 0 else (20, 60, 140), f"ts_pri{i}")
        f.text(x1 - 1.4, y0 + 2.0, SB_Z + 0.06, 1.0, (170, 30, 30), "ts_brief", "right")
    elif v == 3:
        # The NRC site team's board in their room: the class and its clocks, the team (their names the plant's roster),
        # what they follow up (the plant's own conditions).
        f.R(x0, y1 - 2.4, x1, y1, fill=(60, 70, 90, 255))
        RR._fit(f, 24.0, y1 - 1.2, "NRC SITE TEAM", 16, 40.0, fill=(250, 250, 246, 255))
        f.line([(24.0, y0 + 0.2), (24.0, y1 - 2.4)], ink, 3)
        for i, name in enumerate(NRC_FIELDS):
            y = y1 - 4.4 - i * 5.6
            f.T(x0 + 1.0, y, name, 10, fill=ink, bold=True, anchor="lm")
            f.text(x0 + 1.0, y - 2.2, SB_Z + 0.06, 1.2, (170, 30, 30) if i == 0 else (30, 40, 110), f"ts_nrc{i}")
        f.T(25.0, y1 - 4.4, "TEAM", 10, fill=ink, bold=True, anchor="lm")
        for i in range(2):
            f.text(25.0, y1 - 6.6 - i * 2.4, SB_Z + 0.06, 1.0, (30, 40, 110), f"ts_nrc_t{i}")
        f.T(25.0, y1 - 12.6, "FOLLOW UP", 10, fill=ink, bold=True, anchor="lm")
        for i in range(4):
            f.text(25.0, y1 - 14.8 - i * 2.6, SB_Z + 0.06, 1.0, (30, 40, 110), f"ts_nrc_f{i}")
    else:
        f.R(x0, y1 - 2.4, x1, y1, fill=(30, 110, 60, 255))
        RR._fit(f, 15.0, y1 - 1.2, "SEQUENCE OF EVENTS", 15, 26.0, fill=(250, 250, 246, 255))
        RR._fit(f, 38.0, y1 - 1.2, "PROTECTIVE ACTIONS", 14, 15.6, fill=(250, 250, 246, 255))
        f.line([(30.0, y0 + 0.2), (30.0, y1 - 2.4)], ink, 3)
        f.T(x0 + 1.0, y1 - 3.4, "TIME", 10, fill=ink, bold=True, anchor="lm")
        f.T(x0 + 7.8, y1 - 3.4, "EVENT", 10, fill=ink, bold=True, anchor="lm")
        for i in range(SOE_ROWS):
            y = y1 - 5.4 - i * 3.6
            f.line([(x0 + 0.6, y - 1.8), (29.6, y - 1.8)], (170, 170, 170, 255), 1)
            f.text(x0 + 1.0, y, SB_Z + 0.06, 1.1, (30, 40, 110), f"ts_soe_t{i}")
            f.text(x0 + 7.8, y, SB_Z + 0.06, 1.0, (24, 26, 30), f"ts_soe_e{i}")
        f.T(38.5, y1 - 4.2, "PAR TO STATE / COUNTY", 10, fill=ink, bold=True)
        f.text(38.0, y1 - 7.6, SB_Z + 0.06, 1.3, (170, 30, 30), "ts_par0", "center")
        f.text(38.0, y1 - 10.0, SB_Z + 0.06, 1.1, (30, 40, 110), "ts_par1", "center")
        f.text(38.0, y1 - 12.2, SB_Z + 0.06, 1.1, (30, 40, 110), "ts_par2", "center")
        f.T(38.5, y0 + 9.0, "WIND FROM", 10, fill=ink, bold=True)
        f.text(38.0, y0 + 6.6, SB_Z + 0.06, 1.3, (30, 40, 110), "ts_wind", "center")
    return f


def tsc_status_board_boxes(v=0):
    F = f"pi_{SB_KEY}_f"
    x0, y0, x1, y1 = SB_FACE
    trim = TRIM
    b = [(0.4, 1.0, 0.1, 47.6, 31.0, 0.6, {"*": trim, "south": F}), (x0, y0, 0.6, x1, y1, SB_Z, {"*": trim, "south": F})]
    # The frame round the board, the marker tray under it with its markers and eraser.
    b += [(0.4, y1, 0.6, 47.6, 31.0, SB_Z + 0.4, trim), (0.4, 1.0, 0.6, 47.6, y0, SB_Z + 0.4, trim),
          (0.4, y0, 0.6, x0, y1, SB_Z + 0.4, trim), (x1, y0, 0.6, 47.6, y1, SB_Z + 0.4, trim),
          (8.0, 1.0, SB_Z + 0.4, 40.0, 1.4, 3.0, trim), (8.0, 1.4, 2.6, 40.0, 2.0, 3.0, trim)]
    for x, tex in ((10.0, "pi_marker_red"), (13.0, "pi_marker_blue"), (16.0, "pi_tsc_marker_black")):
        b.append((x, 1.4, 1.8, x + 2.4, 1.8, 2.3, tex))
    b.append((30.0, 1.4, 1.5, 34.0, 2.6, 2.5, {"*": "pi_cap_black", "up": "pi_eraser"}))
    return b


# ------------------------------------------------------------------------------------------------ area monitor, frisker
AM_BOX = (2.4, 2.0, 13.6, 28.0)
AM_D = 3.4
AM_METER = (3.4, 15.6, 12.6, 23.2)
AM_DISPLAY = (4.0, 11.6, 12.0, 13.8)
AM_LAMPS = [(4.6, "NORMAL", RR.GREEN, "ts_am_ok", "pi_lens_green"), (8.0, "ALERT", RR.AMBER, "ts_am_alert", "pi_lens_amber"),
            (11.4, "HIGH", RR.RED, "ts_am_high", "pi_lens_red")]
AM_LAMP_Y = 8.0


def tsc_area_monitor():
    f = PP.Front(1, 740)
    D = AM_D
    x0, y0, x1, y1 = AM_BOX
    f.R(x0, y0, x1, y1, outline=(90, 92, 90, 255), width=3)
    f.R(x0, y1 - 3.0, x1, y1, fill=(236, 196, 36, 255), outline=(40, 40, 42, 255), width=2)
    RR._fit(f, 8.0, y1 - 0.95, "AREA RADIATION MONITOR", 9, 10.6)
    RR._fit(f, 8.0, y1 - 2.15, "TSC  RM-TSC-1", 8, 10.6)
    mx0, my0, mx1, my1 = AM_METER
    RR._quarter_meter(f, mx0, my0, mx1, my1, None, "MR/HR", [("0.01", 0.0), ("1", 0.4), ("100", 0.8), ("1000", 1.0)], "ts_am_m", D + 0.2)
    dx0, dy0, dx1, dy1 = AM_DISPLAY
    f.R(dx0, dy0, dx1, dy1, fill=(30, 30, 32, 255))
    f.R(dx0 + 0.25, dy0 + 0.25, dx1 - 0.25, dy1 - 0.25, fill=(28, 6, 6, 255))
    f.text(dx1 - 0.5, (dy0 + dy1) / 2, D + 0.2, 1.2, (255, 70, 50), "ts_am_v", "right")
    RR._fit(f, 8.0, dy0 - 0.5, "DOSE RATE  MR/HR", 7, 8.0)
    for x, name, colour, ch, _ in AM_LAMPS:
        RR._live_lamp(f, x, AM_LAMP_Y, 0.5, colour, ch, D)
        RR._fit(f, x, AM_LAMP_Y - 1.3, name, 7, 3.2)
    RR._fit(f, 8.0, 4.8, "ALERT 2.5  HIGH 15 MR/HR", 7, 10.8)
    RR._fit(f, 8.0, 3.6, "HIGH: TSC HABITABILITY", 7, 10.8)
    return f


def tsc_area_monitor_boxes():
    F = f"pi_{AM_KEY}_f"
    D = AM_D
    x0, y0, x1, y1 = AM_BOX
    face = {"*": "pi_pnl_black", "south": F}
    b = [(x0, y0, 0.2, x1, y1, D, {"*": "pi_cabinet", "south": F})]
    b += RR._meter_case(*AM_METER, face, D=D)
    dx0, dy0, dx1, dy1 = AM_DISPLAY
    b += [(dx0, dy0, D, dx0 + 0.25, dy1, D + 0.3, "pi_pnl_black"), (dx1 - 0.25, dy0, D, dx1, dy1, D + 0.3, "pi_pnl_black"),
          (dx0 + 0.25, dy0, D, dx1 - 0.25, dy0 + 0.25, D + 0.3, "pi_pnl_black"), (dx0 + 0.25, dy1 - 0.25, D, dx1 - 0.25, dy1, D + 0.3, "pi_pnl_black")]
    for x, _, _, _, lens in AM_LAMPS:
        b += RR._lamp(x, AM_LAMP_Y, lens, D=D, r=0.5)
    # The detector under it, its alarm horn and strobe on top, the conduit up out of it.
    b += [(5.6, 0.4, 1.0, 10.4, y0, 3.2, "pi_steel_dark")] + PP.round_y(8.0, 2.1, 1.0, -1.4, 0.4, "pi_pnl_yellow")
    b += [(4.4, y1, 0.6, 7.6, y1 + 1.6, 3.0, "pi_cap_black")] + PP.round_y(10.2, 1.8, 1.2, y1, y1 + 2.2, "pi_amber_glass")
    b += [(11.6, y1, 1.2, 12.6, 32.0, 2.2, "pi_chrome")]
    return b


FR_TABLE = 12.0
FR_METER = (3.4, 12.0, 11.4, 18.6)      # the ratemeter's face (x0, y0, x1, y1), on its front at FR_Z
FR_Z = 11.2
FR_SIGN = (1.6, 20.2, 14.4, 29.6)


def tsc_frisker():
    f = PP.Front(1, 750)
    f.R(0, 0, 16, 32, fill=(214, 216, 210, 255))
    x0, y0, x1, y1 = FR_METER
    f.R(x0, y0, x1, y1, fill=(232, 196, 40, 255))
    RR._quarter_meter(f, x0 + 0.4, y0 + 0.5, x0 + 5.2, y1 - 0.5, None, "CPM", [("0", 0.0), ("500", 0.5), ("1K", 1.0)], "ts_fr_m", FR_Z + 0.05)
    RR._fit(f, x0 + 6.4, y1 - 1.0, "X1", 7, 2.0)
    RR._fit(f, x0 + 6.4, y1 - 2.4, "X10", 7, 2.0)
    RR._fit(f, x0 + 6.4, y0 + 1.2, "AUD", 6, 2.0)
    sx0, sy0, sx1, sy1 = FR_SIGN
    f.R(sx0, sy0, sx1, sy1, fill=(250, 250, 246, 255), outline=(150, 28, 30, 255), width=4)
    f.R(sx0, sy1 - 2.6, sx1, sy1, fill=(150, 28, 30, 255))
    RR._fit(f, 8.0, sy1 - 1.3, "FRISK HERE", 13, 12.0, fill=(250, 250, 246, 255))
    for i, line in enumerate(("BEFORE ENTERING THE TSC", "HANDS, FEET, THEN BODY", "PROBE SLOW: 2 IN A SEC", "OVER 100 CPM: CALL RP")):
        RR._fit(f, 8.0, sy1 - 3.9 - i * 1.45, line, 8, 12.4, fill=(24, 24, 26, 255))
    return f


def tsc_frisker_boxes():
    """A frisking station: the small table on its legs, the ratemeter on it (its meter the game's), its speaker, the
    pancake probe in its holder on its coiled cable, the sign on the wall over it."""
    F = f"pi_{FR_KEY}_f"
    out = [(1.0, FR_TABLE - 0.6, 3.0, 15.0, FR_TABLE, 13.0, {"*": EDGE, "up": TOP, "down": BODY})]
    for x in (1.6, 13.6):
        for z in (3.6, 11.6):
            out.append((x, 0.3, z, x + 0.8, FR_TABLE - 0.6, z + 0.8, "pi_steel_dark"))
            out.append((x - 0.1, 0, z - 0.1, x + 0.9, 0.3, z + 0.9, BLACK))
    out.append((1.6, 3.0, 3.6, 14.4, 3.5, 12.4, "pi_steel_dark"))
    x0, y0, x1, y1 = FR_METER
    out += [(x0, FR_TABLE, 6.0, x1, y1, FR_Z, {"*": "pi_pnl_yellow", "south": F}),
            (x0 + 1.0, y1, 7.4, x1 - 1.0, y1 + 0.4, 9.4, BLACK)]
    out += PP._sc_round(x0 + 6.4, y1 - 1.7, FR_Z, FR_Z + 0.3, 0.45, "pi_cap_black")
    # The probe's holder clipped to the meter's side, the pancake probe in it, its cable down and along the table.
    out += [(x1, 13.0, 7.0, x1 + 0.6, 17.0, 10.0, "pi_steel_dark"), (x1 + 0.6, 13.4, 7.4, x1 + 2.2, 16.6, 9.6, BLACK),
            (x1 + 1.0, 12.2, 8.2, x1 + 1.6, 13.4, 8.8, BLACK), (x1 + 1.0, FR_TABLE, 6.4, x1 + 1.6, 12.2, 8.8, BLACK),
            (x0 + 1.0, FR_TABLE, 5.4, x1 + 1.6, FR_TABLE + 0.4, 6.0, BLACK)]
    sx0, sy0, sx1, sy1 = FR_SIGN
    out.append((sx0, sy0, 0.1, sx1, sy1, 0.4, {"*": "pi_pnl_white", "south": F}))
    for x, y in ((sx0 + 0.6, sy0 + 0.6), (sx1 - 0.6, sy0 + 0.6), (sx0 + 0.6, sy1 - 0.6), (sx1 - 0.6, sy1 - 0.6)):
        out.append((x - 0.2, y - 0.2, 0.4, x + 0.2, y + 0.2, 0.5, "pi_steel"))
    # A box of shoe covers and a step-off pad on the table.
    out += [(1.6, FR_TABLE, 9.0, 4.6, FR_TABLE + 1.6, 12.4, {"*": "pi_carton", "up": "pi_carton"})]
    return out


# ------------------------------------------------------------------------------------------------ the plain pieces
SPINES = [("EPIP-01", "CLASSIFICATION"), ("EPIP-02", "NOTIFICATIONS"), ("EPIP-03", "TSC ACTIVATION"), ("EPIP-04", "DOSE ASSESSMENT"),
          ("EPIP-05", "PROTECTIVE ACTIONS"), ("EPIP-06", "OSC TEAMS"), ("EPIP-07", "CORE DAMAGE"), ("EPIP-08", "RECOVERY"),
          ("TECH SPECS", "VOL 1"), ("TECH SPECS", "VOL 2"), ("FSAR", "CH 15"), ("EOP", "E-0 TO ES-1.3"), ("EOP", "FR-C TO FR-S"),
          ("SAMG", "SAG-1 TO 8"), ("ODCM", "REV 24"), ("E-PLAN", "REV 31")]
SPINE_COLOURS = [(170, 30, 30), (30, 70, 140), (30, 110, 60), (230, 190, 40), (90, 40, 120), (200, 90, 30), (40, 40, 44), (120, 120, 126)]
FILES = ["P&IDs  M-100 TO M-199", "ELECTRICAL ONE-LINES", "I&C LOGIC DIAGRAMS", "GENERAL ARRANGEMENT", "PLANT SITE / EPZ MAPS"]


def procedure_shelf():
    """Steel shelving with the procedures in binders: two shelves to the block, each a row of binders of the plant's
    emergency plan, procedures and technical specifications, their spines labelled; its ends closed."""
    out = []
    for x in (0.6, 14.6):
        for z in (2.0, 13.2):
            out.append((x, 0, z, x + 0.8, 16, z + 0.8, "pi_tsc_body"))
        # Its closed ends (the binders' covers not seen from the side).
        out.append((x + 0.1, 0.2, z - 10.4, x + 0.7, 15.4, z, "pi_tsc_body"))
    for y in (0.4, 8.2):
        out.append((0.6, y, 2.0, 15.4, y + 0.5, 14.0, {"*": "pi_tsc_body", "up": "pi_tsc_body"}))
        x = 1.6
        k = 0
        while x < 14.2:
            w = 1.6 if k % 3 else 2.0
            n = (k + (0 if y < 1 else 7)) % len(SPINES)
            h = 6.6 if k % 4 else 6.2
            out.append((x, y + 0.5, 3.4, x + w, y + 0.5 + h, 13.2, {"*": f"pi_tsc_binder_{n % len(SPINE_COLOURS)}", "south": spine(n)}))
            x += w + 0.06
            k += 1
    out.append((0.6, 15.4, 2.0, 15.4, 16.0, 14.0, {"*": "pi_tsc_body", "up": "pi_tsc_body"}))
    return out


def plan_file():
    """A flat file of the plant's drawings: five wide shallow drawers with their label cards and pulls, on its base, a
    roll of drawings on top."""
    out = [(0.4, 0, 1.0, 15.6, 1.0, 15.0, "pi_tsc_body"), (0.4, 1.0, 1.0, 15.6, 12.4, 14.6, {"*": "pi_tsc_body", "up": "pi_tsc_body"})]
    for i in range(5):
        y0 = 1.3 + i * 2.2
        out.append((0.8, y0, 14.6, 15.2, y0 + 2.0, 15.0, "pi_tsc_body"))
        out.append((5.0, y0 + 0.9, 15.0, 11.0, y0 + 1.8, 15.12, {"*": "pi_chrome", "south": label_card(i)}))
        out.append((6.0, y0 + 0.2, 15.0, 10.0, y0 + 0.6, 15.6, "pi_chrome"))
    out += [(0.4, 12.4, 1.0, 15.6, 12.8, 15.0, {"*": "pi_tsc_body", "up": TOP})]
    out += [(2.0, 12.8, 5.0, 14.0, 14.4, 7.0, "pi_tsc_roll"), (2.0, 13.1, 4.7, 14.0, 14.1, 7.3, "pi_tsc_roll"),
            (14.0, 12.9, 5.1, 14.1, 14.3, 6.9, "pi_tsc_roll_end")]
    return out


def card_reader():
    """The accountability card reader beside the TSC's door: its case on the wall, the reader face, its lit LED."""
    return [(5.6, 6.0, 0.0, 10.4, 13.0, 1.0, {"*": BLACK, "south": "pi_tsc_reader!"}),
            (6.2, 13.0, 0.2, 9.8, 13.4, 0.8, BLACK)]


# A strip atlas (4:1 strips, four a sheet) for the binders' spines and the flat file's label cards.
STRIPS = [f"spine_{k}" for k in range(len(SPINES))] + [f"card_{k}" for k in range(len(FILES))]
SHEETS = (len(STRIPS) + 3) // 4


def strip(name):
    i = STRIPS.index(name)
    sheet, row = divmod(i, 4)
    return (f"pi_tsc_strips_{sheet}", [0, row * 4, 16, row * 4 + 4])


def spine(n):
    """A binder's spine: the strip turned up the spine (a 1:4 label read from the bottom up is drawn on its side)."""
    tex, (u0, v0, u1, v1) = strip(f"spine_{n}")
    return (tex, [u0, v0, u1, v1], 90)


def label_card(i):
    return strip(f"card_{i}")


PIECES = {
    "tsc_procedure_shelf": {"display": "Procedure Shelf", "boxes": procedure_shelf()},
    "tsc_plan_file": {"display": "Drawing Flat File", "boxes": plan_file()},
    "tsc_card_reader": {"display": "Accountability Card Reader", "boxes": card_reader()},
}
SHAPES = {"tsc_procedure_shelf": (0.6, 0, 2.0, 15.4, 16, 14.8), "tsc_plan_file": (0.4, 0, 1.0, 15.6, 13.6, 15.6),
          "tsc_card_reader": (5.6, 6.0, 0.0, 10.4, 13.4, 1.0)}


# ------------------------------------------------------------------------------------------------ textures
def _top():
    return M.painted((206, 204, 198), 760, seams=(), chip_count=0, drip_count=0, dirt=0.06)


def _fabric():
    import numpy as np
    rng = np.random.default_rng(761)
    base = np.array((92, 104, 122), float)
    img = np.ones((128, 128, 3)) * base + rng.normal(0, 7, (128, 128))[..., None]
    for k in range(0, 128, 2):
        img[k, :, :] *= 0.94
    return Image.fromarray(np.clip(img, 0, 255).astype("uint8"), "RGB").convert("RGBA")


def _screen():
    """The monitors' screens' furniture (the application's window: its title bar, panes, faint rules; the text the
    game's)."""
    img = Image.new("RGBA", (320, 192), (10, 16, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 319, 22], fill=(26, 66, 132, 255))
    d.rectangle([4, 26, 315, 187], outline=(40, 60, 96, 255), width=2)
    for y in range(48, 188, 24):
        d.line([(8, y), (311, y)], fill=(22, 32, 54, 255), width=1)
    d.rectangle([296, 4, 314, 18], fill=(180, 50, 50, 255))
    return img


def _keyboard_top():
    img = Image.new("RGBA", (224, 76), (26, 26, 28, 255))
    d = ImageDraw.Draw(img)
    for row in range(5):
        x = 6 + row * 3
        for k in range(14):
            w = 12 if not (row == 4 and 4 <= k <= 8) else 12
            d.rounded_rectangle([x, 6 + row * 13, x + w, 6 + row * 13 + 10], 2, fill=(54, 54, 58, 255), outline=(14, 14, 16, 255))
            x += 14
    d.rounded_rectangle([60, 58, 140, 68], 2, fill=(54, 54, 58, 255))
    for k in range(4):
        for j in range(4):
            d.rounded_rectangle([170 + k * 13, 6 + j * 13, 180 + k * 13, 16 + j * 13], 2, fill=(54, 54, 58, 255))
    return img


def _phone_top():
    img = Image.new("RGBA", (152, 120), (36, 36, 40, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([44, 8, 144, 40], fill=(120, 150, 160, 255), outline=(20, 20, 22, 255), width=3)
    d.text((52, 12), "EXT 2140", fill=(20, 30, 34, 255), font=PP.rfont(13, True))
    d.text((52, 25), "TSC", fill=(20, 30, 34, 255), font=PP.rfont(12, True))
    for r in range(4):
        for c in range(3):
            d.rounded_rectangle([56 + c * 26, 48 + r * 17, 76 + c * 26, 60 + r * 17], 3, fill=(80, 80, 86, 255))
    for r in range(4):
        d.rounded_rectangle([136, 48 + r * 17, 148, 58 + r * 17], 2, fill=(70, 160, 70, 255) if r == 0 else (80, 80, 86, 255))
    return img


def _speaker():
    """The speakerphone's sloped skirt: its display and three keys."""
    img = Image.new("RGBA", (180, 180), (40, 40, 44, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([30, 18, 150, 50], fill=(120, 150, 160, 255), outline=(20, 20, 22, 255), width=3)
    for c in range(3):
        d.rounded_rectangle([30 + c * 44, 128, 62 + c * 44, 150], 4, fill=(70, 70, 76, 255))
    d.ellipse([84, 160, 96, 172], fill=(60, 200, 90, 255))
    return img


def _speaker_top():
    img = Image.new("RGBA", (128, 128), (30, 30, 34, 255))
    d = ImageDraw.Draw(img)
    for y in range(10, 120, 10):
        for x in range(10, 120, 10):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(14, 14, 16, 255))
    return img


def _pc_front():
    img = Image.new("RGBA", (64, 160), (30, 30, 34, 255))
    d = ImageDraw.Draw(img)
    for y in range(40, 150, 6):
        d.line([(10, y), (54, y)], fill=(18, 18, 20, 255), width=2)
    d.ellipse([26, 10, 38, 22], fill=(60, 140, 255, 255))
    d.rectangle([12, 28, 52, 32], fill=(50, 50, 54, 255))
    return img


def _binder_cover(colour=(170, 30, 30)):
    img = Image.new("RGBA", (180, 230), colour + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 30, 156, 120], fill=(246, 246, 240, 255), outline=(30, 30, 30, 255), width=3)
    d.text((90, 58), "EMERGENCY PLAN", fill=(24, 24, 26, 255), font=PP.rfont(18, True), anchor="mm")
    d.text((90, 84), "POSITION BINDER", fill=(24, 24, 26, 255), font=PP.rfont(16, True), anchor="mm")
    d.text((90, 106), "TSC", fill=(170, 30, 30, 255), font=PP.rfont(18, True), anchor="mm")
    return img


def _log_sheet():
    img = Image.new("RGBA", (200, 190), (130, 96, 60, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 22, 190, 186], fill=(250, 250, 246, 255))
    d.text((100, 34), "TSC POSITION LOG", fill=(24, 24, 26, 255), font=PP.rfont(16, True), anchor="mm")
    for k in range(9):
        y = 52 + k * 15
        d.line([(16, y), (184, y)], fill=(170, 170, 200, 255), width=1)
        if k < 6:
            d.text((20, y - 12), f"04{12 + k * 7:02d}", fill=(30, 40, 110, 255), font=PP.rfont(11, True))
            d.line([(58, y - 5), (60 + (k * 37) % 110 + 40, y - 6)], fill=(30, 40, 110, 255), width=2)
    return img


def _radio_face():
    img = Image.new("RGBA", (96, 128), (40, 40, 44, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 12, 86, 40], fill=(60, 150, 90, 255), outline=(10, 10, 10, 255), width=2)
    d.text((48, 26), "CH 2 OPS", fill=(10, 30, 16, 255), font=PP.rfont(14, True), anchor="mm")
    for r in range(3):
        for c in range(3):
            d.rounded_rectangle([14 + c * 24, 52 + r * 20, 32 + c * 24, 66 + r * 20], 3, fill=(80, 80, 86, 255))
    d.ellipse([60, 100, 84, 124], fill=(20, 20, 22, 255), outline=(90, 90, 96, 255), width=2)
    return img


def _reader():
    img = Image.new("RGBA", (96, 140), (24, 24, 26, 255))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([12, 14, 84, 96], 8, outline=(80, 80, 86, 255), width=3)
    for r in (16, 26, 36):
        d.arc([48 - r, 55 - r, 48 + r, 55 + r], 300, 60, fill=(120, 120, 128, 255), width=3)
    d.ellipse([40, 108, 56, 124], fill=(60, 220, 90, 255))
    d.text((48, 132), "BADGE", fill=(160, 160, 166, 255), font=PP.rfont(10, True), anchor="mm")
    return img


def _roll_end():
    img = Image.new("RGBA", (64, 64), (236, 236, 226, 255))
    d = ImageDraw.Draw(img)
    for r in range(4, 32, 4):
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], outline=(200, 200, 190, 255), width=1)
    return img


def _strip_img(name):
    img = Image.new("RGBA", (K, 128), (246, 246, 240, 255))
    d = ImageDraw.Draw(img)
    ink = (22, 22, 24, 255)
    kind, _, n = name.partition("_")
    n = int(n)
    if kind == "spine":
        top, sub = SPINES[n]
        colour = SPINE_COLOURS[n % len(SPINE_COLOURS)]
        d.rectangle([0, 0, K - 1, 127], fill=colour + (255,))
        d.rectangle([36, 14, K - 37, 113], fill=(250, 250, 246, 255))
        PP._fit_line(d, (K // 2, 44), top, 44, K - 100, ink)
        PP._fit_line(d, (K // 2, 88), sub, 34, K - 100, ink)
    else:
        d.rectangle([0, 0, K - 1, 127], outline=ink, width=6)
        PP._fit_line(d, (K // 2, 64), FILES[n], 56, K - 60, ink)
    return img


def _strips(sheet):
    img = Image.new("RGBA", (K, K), (240, 240, 234, 255))
    for row in range(4):
        i = sheet * 4 + row
        if i < len(STRIPS):
            img.paste(_strip_img(STRIPS[i]), (0, row * 128))
    return img


def makers():
    out = {"pi_tsc_top": _top, "pi_tsc_fabric": _fabric, "pi_tsc_screen": _screen, "pi_tsc_keyboard": _keyboard_top,
           "pi_tsc_phone": _phone_top, "pi_tsc_pc": _pc_front, "pi_tsc_binder": _binder_cover, "pi_tsc_log": _log_sheet,
           "pi_tsc_radio": _radio_face, "pi_tsc_reader": _reader, "pi_tsc_speaker": _speaker, "pi_tsc_speaker_top": _speaker_top,
           "pi_tsc_body": lambda: M.painted((58, 60, 64), 762, seams=(), chip_count=0, drip_count=0, dirt=0.05),
           "pi_tsc_edge": lambda: PP._solid((70, 72, 76), 763, 0.02), "pi_tsc_trim": lambda: M.brushed((196, 200, 204), 764, "h", dirt=0.04),
           "pi_tsc_case": lambda: PP._solid((22, 22, 24), 765, 0.01), "pi_tsc_plate": lambda: PP._solid((24, 44, 96), 766, 0.01),
           "pi_tsc_phone_side": lambda: PP._solid((40, 40, 44), 767, 0.01), "pi_tsc_cushion": lambda: PP._solid((30, 30, 32), 768, 0.02),
           "pi_tsc_beige": lambda: PP._solid((214, 204, 180), 769, 0.02), "pi_tsc_hardboard": lambda: PP._solid((130, 96, 60), 770, 0.04),
           "pi_tsc_binder_side": lambda: PP._solid((170, 30, 30), 771, 0.02), "pi_tsc_binder_spine": lambda: _binder_cover().rotate(90, expand=True),
           "pi_tsc_marker_black": lambda: PP._solid((30, 30, 32), 773, 0.0),
           "pi_tsc_roll": lambda: PP._solid((236, 236, 226), 774, 0.03), "pi_tsc_roll_end": _roll_end}
    # The drawn faces square (a block texture's size): each is mapped whole onto its face ("name!"), whatever its shape.
    for name in ("pi_tsc_screen", "pi_tsc_keyboard", "pi_tsc_phone", "pi_tsc_pc", "pi_tsc_binder", "pi_tsc_log", "pi_tsc_radio",
                 "pi_tsc_reader", "pi_tsc_speaker", "pi_tsc_speaker_top", "pi_tsc_binder_spine", "pi_tsc_roll_end"):
        out[name] = (lambda f=out[name]: f().resize((128, 128), Image.LANCZOS))
    for k, c in enumerate(SPINE_COLOURS):
        out[f"pi_tsc_binder_{k}"] = (lambda c=c, k=k: PP._solid(c, 780 + k, 0.03))
    out.update({f"pi_tsc_strips_{k}": (lambda k=k: _strips(k)) for k in range(SHEETS)})
    return out


SHARP = {f"pi_tsc_strips_{k}" for k in range(SHEETS)} | {"pi_tsc_screen", "pi_tsc_keyboard", "pi_tsc_phone", "pi_tsc_binder", "pi_tsc_log",
                                                         "pi_tsc_radio", "pi_tsc_reader", "pi_tsc_pc", "pi_tsc_binder_spine",
                                                         "pi_tsc_speaker", "pi_tsc_speaker_top"}


# ------------------------------------------------------------------------------------------------ registration
PANELS = {TC_KEY: {"display": "TSC Console", "width": 3, "rows": 1, "item3d": True, "labels": [" / ".join(p) for p in POSITIONS], "shared": True},
          CC_KEY: {"display": "TSC Communications Console", "width": 3, "rows": 1, "item3d": True},
          NRC_KEY: {"display": "NRC Site Team Desk", "width": 3, "rows": 1, "item3d": True},
          VW_KEY: {"display": "TSC Video Wall Display", "width": 2, "item3d": True, "labels": VW_LABELS},
          CB_KEY: {"display": "Emergency Classification Board", "width": 3, "item3d": True},
          SB_KEY: {"display": "TSC Status Board", "width": 3, "item3d": True, "labels": SB_LABELS},
          AM_KEY: {"display": "Area Radiation Monitor", "width": 1, "item3d": True},
          FR_KEY: {"display": "Frisking Station", "width": 1, "item3d": True}}
DRAWINGS = {TC_KEY: tsc_console, CC_KEY: tsc_comms_console, NRC_KEY: tsc_nrc_desk, VW_KEY: tsc_video_wall, CB_KEY: tsc_class_board, SB_KEY: tsc_status_board,
            AM_KEY: tsc_area_monitor, FR_KEY: tsc_frisker}
BOXES = {TC_KEY: tsc_console_boxes, CC_KEY: tsc_comms_boxes, NRC_KEY: tsc_nrc_desk_boxes, VW_KEY: tsc_video_wall_boxes, CB_KEY: tsc_class_board_boxes,
         SB_KEY: tsc_status_board_boxes, AM_KEY: tsc_area_monitor_boxes, FR_KEY: tsc_frisker_boxes}
DESK = {"P": "#c:plates/steel", "G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "W": "minecraft:gray_wool"}
CRAFTING = {
    TC_KEY: (["GCG", "WPW", "P P"], DESK, 1),
    CC_KEY: (["GCG", "WXW", "P P"], dict(DESK, X="minecraft:note_block"), 1),
    NRC_KEY: (["GCG", "QQQ", "P P"], dict({k: v for k, v in DESK.items() if k != "W"}, Q="minecraft:glass_pane"), 1),
    VW_KEY: (["GGG", "GCG", " P "], {k: v for k, v in DESK.items() if k != "W"}, 1),
    CB_KEY: (["LLL", "PCP"], {"L": "minecraft:redstone_lamp", "P": "#c:plates/steel", "C": "jugcraft:basic_circuit"}, 1),
    SB_KEY: (["QQQ", "QIQ"], {"Q": "minecraft:quartz_slab", "I": "minecraft:ink_sac"}, 1),
    AM_KEY: (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                     "X": "jugcraft:geiger_counter", "R": "minecraft:redstone"}, 1),
    FR_KEY: (["X", "S"], {"X": "jugcraft:geiger_counter", "S": "minecraft:smooth_stone_slab"}, 1),
    "tsc_procedure_shelf": (["PBP", "PBP", "PBP"], {"P": "#c:plates/iron", "B": "minecraft:book"}, 2),
    "tsc_plan_file": (["PPP", "PDP", "PPP"], {"P": "#c:plates/iron", "D": "minecraft:paper"}, 1),
    "tsc_card_reader": (["P", "C"], {"P": "#c:plates/iron", "C": "jugcraft:basic_circuit"}, 2),
}


def shape(name):
    return SHAPES[name]


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
