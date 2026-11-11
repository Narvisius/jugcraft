"""The main control room brought up to the detail standard (older rooms round 5; Justin: "Control room furniture"). The
round 23 room kept its boards (rebuilt live since: #4a-#4e) but its furniture was the first the plant had: black desks
with painted screens, bookcases, the admin building's executive desks in the shift supervisor's office. As a real
Westinghouse plant's control room has them:

- the operators' consoles (cr_console, three blocks wide, one position each: REACTOR OPERATOR, BALANCE OF PLANT,
  CONTROL ROOM SUPERVISOR, lettered live on the plate over the screens): a black steel console with drawer pedestals,
  its rubber-edged worktop, a monitor bridge with three of the plant process computer's screens (live: each reads the
  control room's display wall, nuclear/ControlRoomConsoles.java; a click steps a screen to its next page), the keyboard,
  the phone set, the plant page microphone, the emergency procedure open in its holder, the operator's log, a headset;
- the shift manager's desk in his office (ss_desk): the same build with two screens (plant status, alarms), the ENS
  phone (red) beside his own, the emergency plan's classification binder, the shift log, a desk lamp;
- the procedure racks along the back wall (cr_procedure_rack, eight sections: EOPs, function restoration, AOPs, alarm
  responses, operating procedures, technical specifications, the emergency plan, surveillances), every binder's spine
  numbered, each rack's header naming its section;
- the room's fittings: the SCBA cabinet (the operators' breathing air), the emergency action level chart in the shift
  manager's office, the At The Controls area's line across the floor and its sign over it; the master clocks, smoke
  detectors, emergency lights, an extinguisher, the drawing rack and the alarm printer (pieces the plant has).

Models in model pixels for the piece facing north: x across, y up, z from its back (z 0) out to its front, where its user
is (+z)."""
from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR
import tsc_room as TR

U = PP.U
INK = (24, 24, 26, 255)
WHITE = (246, 246, 240, 255)

# ------------------------------------------------------------------------------------------------ the positions
CC_KEY, SS_KEY, PR_KEY = "cr_console", "ss_desk", "cr_procedure_rack"
POSITIONS = ["REACTOR OPERATOR", "BALANCE OF PLANT", "CONTROL ROOM SUPERVISOR", "TURBINE CONTROL"]
# Each position's screens' pages (nuclear/ControlRoomConsoles.java PAGES): a click on a screen steps it to the next.
PAGES = [["REACTOR", "RCS", "NIS", "RODS"], ["SECONDARY", "TURBINE GEN", "ELECTRICAL", "AUX FEED"],
         ["ALARMS", "CSF STATUS", "PLANT STATUS", "SAFETY INJ"],
         # (Turbine hall part 3: the turbine building's control room, tools/tcr_room.py.)
         ["TURBINE GEN", "TSI", "EHC", "TURBINE OIL"]]
SS_PAGES = ["PLANT STATUS", "ALARMS", "CSF STATUS"]

# ------------------------------------------------------------------------------------------------ the console
DESK_Y = 12.2               # the worktop's top
BODY, TOP, EDGE, ALUM = "pi_cr_body", "pi_cr_top", "pi_cap_black", "pi_cr_alum"
BLACK = "pi_cap_black"
BRIDGE = (0.4, 4.4, 13.6)   # the monitor bridge (a shelf across the back): z0, z1, its top
MON_HALF = 5.5              # a monitor's half width (27-inch screens: 11 px)
MON_Y = (14.2, 21.6)        # its case's bottom and top
SCREEN_Z = 3.7              # the screens' faces
CON_MON = [12.6, 24.0, 35.4]
SS_MON = [17.4, 30.6]
PLATE = (16.0, 32.0, 22.4, 24.0, 2.7, 3.3)   # the position plate over the screens: x0, x1, y0, y1, z0, z1
SCREEN_LINES = 7
LINE_STEP = 0.8


def _console_body(W=48, pedestals=True):
    """The steel console: its two drawer pedestals, the modesty panel, the worktop and its rubber front edge, the
    monitor bridge along its back, the kick plate."""
    y0 = DESK_Y - 0.8
    out = [(0.2, y0, BRIDGE[1], W - 0.2, DESK_Y, 15.2, {"*": ALUM, "up": TOP, "down": BODY}),
           # The rubber bumper standing a hair over the worktop along its front (the operator's arms rest on it).
           (0.2, y0 - 0.2, 15.2, W - 0.2, DESK_Y + 0.2, 15.9, EDGE),
           (10.6, 1.5, 3.0, W - 10.6, y0, 3.8, BODY)]
    for x0, x1 in ((0.4, 10.6), (W - 10.6, W - 0.4)):
        out += [(x0, 0.6, 3.0, x1, y0, 14.8, BODY), (x0 + 0.3, 0.0, 3.4, x1 - 0.3, 0.6, 14.2, BLACK)]
        if pedestals:
            # Three drawers, their fronts a hair proud, a pull on each.
            for k, (a, b) in enumerate(((0.9, 4.2), (4.4, 7.7), (7.9, y0 - 0.3))):
                out.append((x0 + 0.4, a, 14.8, x1 - 0.4, b, 15.0, BODY))
                cx = (x0 + x1) / 2
                out.append((cx - 1.6, b - 0.9, 15.0, cx + 1.6, b - 0.5, 15.35, ALUM))
    # The monitor bridge: a shelf on the worktop's back, its top in aluminium, a cable slot along its foot.
    z0, z1, top = BRIDGE
    out += [(0.2, DESK_Y, z0, W - 0.2, top, z1, {"*": BODY, "up": ALUM}),
            (0.2, y0, z0, W - 0.2, DESK_Y, BRIDGE[1], BODY)]
    return out


def _monitor(cx, tex="pi_cr_screen!"):
    """A 27-inch monitor on its foot on the bridge: the black case, the screen a tenth proud of it."""
    h = MON_HALF
    z0 = SCREEN_Z - 1.1
    return [(cx - h, MON_Y[0], z0, cx + h, MON_Y[1], SCREEN_Z - 0.1, "pi_tsc_case"),
            (cx - h + 0.3, MON_Y[0] + 0.3, SCREEN_Z - 0.1, cx + h - 0.3, MON_Y[1] - 0.3, SCREEN_Z, {"*": "pi_tsc_case", "south": tex}),
            (cx - 1.8, BRIDGE[2], 1.0, cx + 1.8, BRIDGE[2] + 0.25, 3.6, "pi_steel_dark"),
            (cx - 0.5, BRIDGE[2] + 0.25, 1.6, cx + 0.5, MON_Y[0] + 1.0, z0, "pi_steel_dark"),
            # The power light under the screen's right corner.
            (cx + h - 1.2, MON_Y[0] + 0.08, SCREEN_Z - 0.1, cx + h - 0.8, MON_Y[0] + 0.22, SCREEN_Z, "pi_cr_led!")]


def _page_mic(x, z):
    """The plant page station: its base with the PAGE and ALL CALL keys, the gooseneck, the microphone head."""
    return [(x - 1.4, DESK_Y, z - 1.2, x + 1.4, DESK_Y + 0.6, z + 1.2, {"*": BLACK, "up": "pi_cr_page!"}),
            (x - 0.2, DESK_Y + 0.6, z - 1.0, x + 0.2, DESK_Y + 3.2, z - 0.6, "pi_steel_dark"),
            (x - 0.2, DESK_Y + 2.9, z - 0.6, x + 0.2, DESK_Y + 3.3, z + 0.6, "pi_steel_dark"),
            (x - 0.45, DESK_Y + 2.5, z + 0.6, x + 0.45, DESK_Y + 3.6, z + 1.5, {"*": BLACK, "south": "pi_cr_grille"})]


def _procedure_holder(x0, z0, tex="pi_cr_eop"):
    """The emergency procedure open in its holder: the sloped stand, the binder's two pages, its rings."""
    return [(x0, DESK_Y, z0, x0 + 9.0, DESK_Y + 0.3, z0 + 5.8, "pi_steel_dark"),
            (x0 + 0.3, DESK_Y + 0.3, z0 + 0.2, x0 + 8.7, DESK_Y + 0.55, z0 + 5.6, {"*": "pi_cr_binder_0", "up": (tex, PAGE_UV)}),
            (x0 + 4.35, DESK_Y + 0.55, z0 + 1.0, x0 + 4.65, DESK_Y + 0.85, z0 + 4.8, "pi_chrome")]


def _logbook(x0, z0, tex="pi_cr_log"):
    """The operator's log open on the desk (a bound book: its narrative entries, the shift's), the pen on it."""
    return [(x0, DESK_Y, z0, x0 + 5.6, DESK_Y + 0.4, z0 + 4.0, {"*": "pi_cr_logcover", "up": (tex, LOG_UV)}),
            (x0 + 4.4, DESK_Y + 0.4, z0 + 0.6, x0 + 4.55, DESK_Y + 0.55, z0 + 3.2, "pi_pen_blue")]


def _plate():
    x0, x1, y0, y1, z0, z1 = PLATE
    return [(x0, y0, z0, x1, y1, z1, {"*": ALUM, "south": "pi_cr_plate"}),
            # Its two posts standing on the middle screen's case.
            (x0 + 3.6, MON_Y[1], z0 + 0.1, x0 + 4.1, y0, z1 - 0.1, ALUM), (x1 - 4.1, MON_Y[1], z0 + 0.1, x1 - 3.6, y0, z1 - 0.1, ALUM)]


def cr_console_boxes():
    W = 48
    out = _console_body(W)
    for cx in CON_MON:
        out += _monitor(cx)
    out += _plate()
    out += TR._keyboard(19.6, 10.4)
    out += TR._phone(3.6, 5.6)
    out += _page_mic(9.6, 7.2)
    out += _logbook(9.8, 9.6)
    out += _procedure_holder(36.4, 6.2)
    out += TR._headset(31.8, 6.6)
    # The process computer's workstation under the desk, beside the right pedestal.
    out += [(33.6, 0.3, 4.4, 37.0, 8.6, 12.6, {"*": "pi_tsc_body", "south": "pi_tsc_pc!"})]
    return out


def _lamp(x, z):
    """A desk lamp: its weighted base, the two-part arm, the shade over the desk."""
    return [(x - 1.2, DESK_Y, z - 1.2, x + 1.2, DESK_Y + 0.4, z + 1.2, BLACK),
            (x - 0.2, DESK_Y + 0.4, z - 0.2, x + 0.2, DESK_Y + 5.2, z + 0.2, "pi_chrome"),
            (x - 0.2, DESK_Y + 5.0, z - 0.2, x + 0.2, DESK_Y + 5.4, z + 3.4, "pi_chrome"),
            (x - 1.3, DESK_Y + 3.8, z + 3.0, x + 1.3, DESK_Y + 5.4, z + 5.4, {"*": "pi_cr_shade", "down": "pi_cr_bulb"})]


def ss_desk_boxes():
    W = 48
    out = _console_body(W)
    for cx in SS_MON:
        out += _monitor(cx)
    out += TR._keyboard(19.6, 10.4)
    # The ENS phone (red: the NRC's line) beside his own; the emergency plan's classification binder; the shift log.
    out += TR._phone(1.8, 5.6, "pi_cap_red")
    out += TR._phone(6.6, 5.8)
    out += [(37.0, DESK_Y, 6.0, 41.8, DESK_Y + 0.9, 11.8, {"*": "pi_cr_binder_6", "up": ("pi_cr_ep", EP_UV)})]
    out += _logbook(10.2, 9.6, "pi_cr_sslog")
    out += _lamp(44.6, 6.0)
    out += [(33.6, 0.3, 4.4, 37.0, 8.6, 12.6, {"*": "pi_tsc_body", "south": "pi_tsc_pc!"})]
    return out


def _screen_live(f, cx, stem):
    """A screen's live text: its page's title on the title bar; each line a name on the left, its value on the right
    (each value's colour the game's: white, amber or red)."""
    top = MON_Y[1] - 0.3 - 0.45
    x, xr = cx - MON_HALF + 0.75, cx + MON_HALF - 0.75
    f.text(x, top, SCREEN_Z + 0.01, 0.5, (236, 240, 246), f"{stem}_t")
    f.text(xr, top, SCREEN_Z + 0.01, 0.42, (120, 198, 238), f"{stem}_c", "right")
    for i in range(SCREEN_LINES):
        y = top - 0.95 - i * LINE_STEP
        f.text(x, y, SCREEN_Z + 0.01, 0.46, (168, 176, 188), f"{stem}_l{i}")
        f.text(xr, y, SCREEN_Z + 0.01, 0.46, (236, 240, 246), f"{stem}_r{i}", "right")


def cr_console(label=None):
    f = PP.Front(3, 900)
    x0, x1, y0, y1, z0, z1 = PLATE
    f.text((x0 + x1) / 2, (y0 + y1) / 2 - 0.42, z1 + 0.01, 0.84, (24, 24, 26), "crc_name", "center")
    for m, cx in enumerate(CON_MON):
        _screen_live(f, cx, f"crc_m{m}")
    return f


def ss_desk(label=None):
    f = PP.Front(3, 901)
    for m, cx in enumerate(SS_MON):
        _screen_live(f, cx, f"ssd_m{m}")
    return f


# ------------------------------------------------------------------------------------------------ the procedure racks
SECTIONS = [
    ("EMERGENCY OPERATING", "PROCEDURES", "EOP", ["E-0", "E-1", "E-2", "E-3", "ES-0.1", "ES-0.2", "ES-0.3", "ES-0.4", "ES-1.1", "ES-1.2", "ES-1.3",
                                                 "ES-3.1", "ES-3.2", "ES-3.3", "ECA-0.0", "ECA-0.1", "ECA-0.2", "ECA-1.1", "ECA-2.1", "ECA-3.1"]),
    ("FUNCTION RESTORATION", "PROCEDURES", "FR", ["F-0", "FR-S.1", "FR-S.2", "FR-C.1", "FR-C.2", "FR-C.3", "FR-H.1", "FR-H.2", "FR-H.3", "FR-H.4",
                                                  "FR-H.5", "FR-P.1", "FR-P.2", "FR-Z.1", "FR-Z.2", "FR-Z.3", "FR-I.1", "FR-I.2", "FR-I.3", "BKGD"]),
    ("ABNORMAL OPERATING", "PROCEDURES", "AOP", [f"AOP-{k}" for k in range(1, 21)]),
    ("ALARM RESPONSE", "PROCEDURES", "ARP", [f"{b}-{k}" for b in ("ESF", "RCS", "SEC", "TRB", "ELE") for k in range(1, 5)]),
    ("GENERAL & SYSTEM", "OPERATING PROCS", "OP", [f"GOP-{k}" for k in range(1, 7)] + [f"OP-{k}" for k in (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)]),
    ("TECHNICAL", "SPECIFICATIONS", "TS", ["TS 1.0", "TS 2.0", "TS 3.0", "TS 3.1", "TS 3.2", "TS 3.3", "TS 3.4", "TS 3.5", "TS 3.6", "TS 3.7",
                                           "TS 3.8", "TS 3.9", "TS 5.0", "BASES", "BASES", "TRM", "TRM", "ODCM", "COLR", "LCO LOG"]),
    ("EMERGENCY PLAN", "IMPLEMENTING PROCS", "EPIP", [f"EPIP-{k:02d}" for k in range(1, 15)] + ["E-PLAN", "EAL", "EAL", "PHONE", "FORMS", "KI"]),
    ("SURVEILLANCE", "PROCEDURES", "OST", [f"OST-{k}" for k in (101, 102, 105, 110, 115, 201, 202, 210, 301, 302, 305, 401, 402, 405, 501,
                                                             502, 601, 602, 701, 702)]),
]
PR_LABELS = [s[2] for s in SECTIONS]
# Each section's binders' colour (their covers) and its spine label's band.
BINDER_RGB = [(236, 236, 230), (232, 140, 40), (240, 204, 40), (60, 110, 190), (60, 140, 80), (40, 42, 46), (190, 40, 36), (150, 154, 160)]
SHELVES = [0.4, 7.6, 14.8, 22.0]      # the shelves' undersides (each 0.5 thick)
BINDER_W, BINDER_GAP, BINDER_X0 = 2.56, 0.06, 1.5
BINDER_H = [6.4, 6.4, 6.4, 6.4]
PR_F = f"pi_{PR_KEY}_f"


def procedure_rack(label=PR_LABELS[0]):
    """A rack's lettering (pi_cr_procedure_rack_f, 1 block by 2): the header card over it naming its section, every
    binder's spine (its cover's colour, the white label with its number, the section's band at its foot), each shelf's
    edge with its range."""
    s = PR_LABELS.index(label)
    line1, line2, short, numbers = SECTIONS[s]
    f = PP.Front(1, 910 + s)
    f.R(0, 0, 16, 32, fill=(70, 72, 76, 255))
    # The header across its top: white, the section's two lines, its colour band.
    f.R(0.4, 29.0, 15.6, 31.6, fill=WHITE, outline=(60, 60, 62, 255), width=2)
    f.R(0.6, 29.15, 15.4, 29.45, fill=BINDER_RGB[s] + (255,))
    RR._fit(f, 8.0, 30.95, line1, 7.5, 14.4, fill=INK)
    RR._fit(f, 8.0, 30.05, line2, 7.5, 14.4, fill=INK)
    rgb = BINDER_RGB[s]
    dark = sum(rgb) < 300
    k = 0
    for r, y in enumerate(SHELVES):
        for i in range(5):
            x0 = BINDER_X0 + i * (BINDER_W + BINDER_GAP)
            y0, y1 = y + 0.5, y + 0.5 + BINDER_H[r]
            f.R(x0, y0, x0 + BINDER_W, y1, fill=rgb + (255,))
            f.R(x0 + 0.25, y1 - 2.2, x0 + BINDER_W - 0.25, y1 - 0.5, fill=WHITE, outline=(120, 120, 120, 255), width=1)
            RR._fit(f, x0 + BINDER_W / 2, y1 - 1.35, numbers[k], 7, BINDER_W - 0.6, fill=INK)
            f.R(x0, y0 + 0.5, x0 + BINDER_W, y0 + 1.1, fill=((230, 230, 224) if dark else (36, 36, 40)) + (255,))
            # The finger hole near the spine's foot.
            f.E(x0 + BINDER_W / 2, y0 + 2.2, 0.35, fill=(40, 40, 42, 255) if not dark else (20, 20, 22, 255))
            k += 1
        # The shelf's edge: its section and range on a label holder.
        f.R(0.8, y - 0.4, 15.2, y + 0.5, fill=(198, 200, 204, 255))
        first, last = numbers[r * 5], numbers[r * 5 + 4]
        f.R(4.0, y - 0.32, 12.0, y + 0.42, fill=WHITE)
        RR._fit(f, 8.0, y + 0.05, f"{first} - {last}", 6.5, 7.6, fill=INK)
    return f


def procedure_rack_boxes(open_=False):
    """The rack: a steel shelving unit two blocks high against the wall (its back and closed ends), four shelves with
    lipped edges, five binders standing on each, the header across its top."""
    F = PR_F
    out = [(0.4, 0.0, 0.6, 15.6, 31.6, 1.0, "pi_cr_rack"),
           (0.4, 0.0, 1.0, 1.0, 31.6, 13.8, "pi_cr_rack"), (15.0, 0.0, 1.0, 15.6, 31.6, 13.8, "pi_cr_rack"),
           (0.4, 31.2, 1.0, 15.6, 31.6, 13.8, "pi_cr_rack")]
    for r, y in enumerate(SHELVES):
        out.append((1.0, y, 1.0, 15.0, y + 0.5, 13.6, "pi_cr_rack"))
        out.append((0.8, y - 0.4, 13.6, 15.2, y + 0.5, 13.9, {"*": "pi_cr_rack", "south": F}))
        for i in range(5):
            x0 = BINDER_X0 + i * (BINDER_W + BINDER_GAP)
            out.append((x0, y + 0.5, 2.6, x0 + BINDER_W, y + 0.5 + BINDER_H[r], 13.0, {"*": "pi_cr_binder", "south": F}))
    # The header across the front of its top.
    out += [(0.4, 29.0, 13.8, 15.6, 31.6, 14.1, {"*": "pi_cr_rack", "south": F}), (1.0, 29.0, 1.0, 15.0, 29.4, 13.8, "pi_cr_rack")]
    return out


# ------------------------------------------------------------------------------------------------ the room's pieces
def scba_part(c, r):
    """The SCBA cabinet (two blocks high, against the wall): its red steel case, the clear door in its frame with the
    hinges and latch, two breathing apparatus inside (each its harness on a backplate, the yellow cylinder, the mask
    hung over it), its sign on the top."""
    out = []
    if r == 0:
        out += [(1.0, 0.0, 1.0, 15.0, 1.0, 9.0, "pi_cr_scba_red"), (1.0, 1.0, 1.0, 15.0, 16.0, 1.6, "pi_cr_scba_red"),
                (1.0, 1.0, 1.6, 1.8, 16.0, 9.0, "pi_cr_scba_red"), (14.2, 1.0, 1.6, 15.0, 16.0, 9.0, "pi_cr_scba_red"),
                (1.8, 1.0, 1.6, 14.2, 1.6, 8.6, "pi_cr_scba_red"),
                (1.8, 1.6, 8.6, 14.2, 16.0, 9.0, {"*": "pi_cr_glass", "south": "pi_cr_glass"})]
        for cx in (5.0, 11.0):
            # The cylinder (upright, its valve at the foot) on the backplate, the harness straps.
            out += [(cx - 1.6, 2.4, 2.2, cx + 1.6, 14.0, 5.4, "pi_cr_cylinder"), (cx - 2.4, 4.0, 1.6, cx + 2.4, 16.0, 2.2, BLACK),
                    (cx - 0.5, 1.6, 3.2, cx + 0.5, 2.4, 4.4, "pi_chrome"), (cx - 2.6, 10.0, 2.2, cx - 2.0, 16.0, 6.2, BLACK),
                    (cx + 2.0, 10.0, 2.2, cx + 2.6, 16.0, 6.2, BLACK)]
        out += [(14.6, 6.0, 9.0, 15.0, 9.0, 9.4, "pi_chrome")]
    else:
        out += [(1.0, 0.0, 1.0, 15.0, 13.0, 1.6, "pi_cr_scba_red"), (1.0, 0.0, 1.6, 1.8, 13.0, 9.0, "pi_cr_scba_red"),
                (14.2, 0.0, 1.6, 15.0, 13.0, 9.0, "pi_cr_scba_red"), (1.0, 12.2, 1.6, 15.0, 13.0, 9.0, "pi_cr_scba_red"),
                (1.8, 0.0, 8.6, 14.2, 12.2, 9.0, {"*": "pi_cr_glass", "south": "pi_cr_glass"}),
                # Its sign on the cabinet's top front.
                (1.4, 13.0, 7.8, 14.6, 15.6, 8.4, {"*": "pi_cr_scba_red", "south": ("pi_cr_scba_sign", SCBA_UV)})]
        for cx in (5.0, 11.0):
            # The harness's shoulder straps over the top, the mask in its bag hung from a hook.
            out += [(cx - 2.4, 0.0, 1.6, cx + 2.4, 3.0, 2.2, BLACK), (cx - 2.6, 0.0, 2.2, cx - 2.0, 4.0, 5.0, BLACK),
                    (cx + 2.0, 0.0, 2.2, cx + 2.6, 4.0, 5.0, BLACK), (cx - 0.3, 9.0, 1.6, cx + 0.3, 9.6, 3.0, "pi_chrome"),
                    (cx - 2.0, 4.6, 2.6, cx + 2.0, 9.2, 6.4, "pi_cr_maskbag")]
        out += [(14.6, 0.0, 9.0, 15.0, 3.0, 9.4, "pi_chrome")]
    # The hinges on the door's left edge.
    out += [(1.2, 2.0 if r == 0 else 6.0, 9.0, 1.8, 3.6 if r == 0 else 7.6, 9.4, "pi_chrome")]
    return out


def eal_part(c, r):
    """The emergency action level chart on the shift manager's wall (two blocks across): its frame and the chart."""
    tex = (f"pi_cr_eal_{c}", [0, 0, 16, 14])
    # (Kept a little in from the piece's outer ends: the model writer's nudges would take a face at the edge out past
    # the item model's limits.)
    x0, x1 = (0.4, 16.0) if c == 0 else (0.0, 15.6)
    out = [(x0, 1.0, 0.0, x1, 15.0, 0.6, {"*": "pi_cr_frame", "south": tex})]
    # The frame's moulding round the chart (on its outer edges only).
    out += [(x0, 0.6, 0.0, x1, 1.0, 0.9, "pi_cr_frame"), (x0, 15.0, 0.0, x1, 15.4, 0.9, "pi_cr_frame")]
    if c == 0:
        out.append((x0, 0.6, 0.0, x0 + 0.4, 15.4, 0.9, "pi_cr_frame"))
    else:
        out.append((x1 - 0.4, 0.6, 0.0, x1, 15.4, 0.9, "pi_cr_frame"))
    return out


ATC_UV = [0, 0, 16, 7.56]


def atc_sign():
    """The At The Controls area's sign at each end of its line: a weighted base, the post, the plate on its top lettered
    both sides."""
    face = ("pi_cr_atc", ATC_UV)
    return [(4.6, 0.0, 4.6, 11.4, 0.6, 11.4, BLACK), (5.4, 0.6, 5.4, 10.6, 0.9, 10.6, "pi_steel_dark"),
            (7.6, 0.9, 7.6, 8.4, 9.0, 8.4, "pi_chrome"),
            (1.0, 9.0, 7.4, 15.0, 15.6, 8.6, {"*": "pi_cr_atc_edge", "south": face, "north": face})]


def atc_line():
    """The At The Controls area's line across the carpet: a red band edged white on the floor."""
    return [(0.0, 0.0, 6.8, 16.0, 0.12, 9.2, "pi_cr_atc_red"), (0.0, 0.0, 6.6, 16.0, 0.1, 6.8, "pi_cr_atc_white"),
            (0.0, 0.0, 9.2, 16.0, 0.1, 9.4, "pi_cr_atc_white")]


PIECES = {"cr_atc_sign": {"display": "At The Controls Sign", "boxes": atc_sign()},
          "cr_atc_line": {"display": "At The Controls Floor Line", "boxes": atc_line()}}
MULTI = {"cr_scba_cabinet": {"display": "SCBA Cabinet", "multi": (1, 2, "frame"), "parts": scba_part, "item3d": True},
         "cr_eal_chart": {"display": "Emergency Action Level Chart", "multi": (2, 1, "frame"), "parts": eal_part, "item3d": True}}


# ------------------------------------------------------------------------------------------------ textures
K = 256


def _f(size, bold=True):
    return PP.rfont(size, bold)


PAGE_UV = [0, 0, 16, 10.25]


def _page_tex(title, sub, steps, accent=(170, 30, 30)):
    """An open binder's two pages (its top 512 by 328: the spread's shape): the procedure's number and title on the left
    page's head, its numbered steps (the action, the expected response under it) down both."""
    img = Image.new("RGBA", (512, 512), (246, 246, 238, 255))
    d = ImageDraw.Draw(img)
    H = 328
    d.rectangle([254, 0, 258, H - 1], fill=(200, 200, 192, 255))
    d.rectangle([0, 0, 511, H - 1], outline=(150, 150, 140, 255), width=4)
    d.rectangle([12, 10, 244, 54], fill=accent + (255,))
    SGR._text(d, (128, 25), title, 26, (250, 250, 246), width=220)
    SGR._text(d, (128, 45), sub, 13, (250, 250, 246), width=224)
    y = 62
    for k, (a, b) in enumerate(steps):
        side = 0 if k < 7 else 1
        if k == 7:
            y = 14
        x0 = 14 + side * 256
        d.text((x0, y), f"{k + 1}.", font=_f(15), fill=(20, 20, 22))
        d.text((x0 + 26, y), a, font=_f(14), fill=(20, 20, 22))
        d.text((x0 + 26, y + 16), b, font=_f(12, False), fill=(70, 70, 80))
        y += 37 if side == 0 else 44
    return img


EOP_STEPS = [("VERIFY REACTOR TRIP", "RX TRIP BKRS OPEN"), ("VERIFY TURBINE TRIP", "STOP VALVES CLOSED"),
             ("VERIFY POWER TO AC BUSES", "4160 V 1A, 1B ENERGIZED"), ("CHECK IF SI ACTUATED", "SI LIGHTS LIT"),
             ("VERIFY FW ISOLATION", "MFW PUMPS TRIPPED"), ("VERIFY AFW PUMPS", "MD AFW RUNNING"),
             ("VERIFY CCW PUMPS", "TWO RUNNING"), ("CHECK RCS TEMPS", "TAVG 547 F STABLE"),
             ("CHECK PZR PORVS", "CLOSED"), ("CHECK RCPS", "RUNNING IF SUBCOOLED"),
             ("CHECK SG PRESSURES", "NO FAULTED SG"), ("CHECK SG TUBES", "NO RADIATION"),
             ("CHECK RCS INTACT", "CNT PRESS NORMAL"), ("GO TO E-1 OR ES-0.1", "AS DIRECTED")]


LOG_UV = [0, 0, 16, 11.43]


def _log_tex(title):
    """An open log book (its top 256 by 183: the book's shape): the shift at its head, the entries (times down the left,
    the narrative lines)."""
    img = Image.new("RGBA", (256, 256), (244, 242, 230, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([126, 0, 130, 182], fill=(190, 186, 170, 255))
    d.text((10, 6), title, font=_f(16), fill=(20, 40, 120))
    for page in (0, 1):
        x0 = 8 + page * 128
        for k in range(7):
            y = 28 + k * 21
            d.line([(x0, y + 18), (x0 + 112, y + 18)], fill=(170, 190, 210), width=1)
            if page == 0 or k < 4:
                d.text((x0, y), f"{(6 + k // 2 + page * 4):02d}{(k * 7 % 6) * 10:02d}", font=_f(12, False), fill=(30, 30, 120))
                d.line([(x0 + 38, y + 10), (x0 + 50 + (k * 37 % 60), y + 10)], fill=(40, 40, 140), width=2)
    return img


def _screen():
    """A plant process computer screen's furniture (its frame, title band, faint rules; the words the game's)."""
    img = Image.new("RGBA", (320, 224), (8, 10, 16, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 319, 24], fill=(34, 40, 52, 255))
    d.rectangle([3, 28, 316, 220], outline=(32, 40, 56, 255), width=2)
    for y in range(52, 220, 23):
        d.line([(8, y), (311, y)], fill=(18, 22, 32, 255), width=1)
    return img.resize((128, 128), Image.LANCZOS)


EP_UV = [0, 0, 13.25, 16]


def _ep_cover():
    """The emergency plan's classification binder's cover (its left 424 by 512: the binder's shape)."""
    img = Image.new("RGBA", (512, 512), (150, 30, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 24, 400, 488], outline=(250, 250, 246), width=6)
    SGR._text(d, (212, 90), "EMERGENCY PLAN", 40, (250, 250, 246), width=350)
    d.rectangle([60, 140, 364, 300], fill=(250, 250, 246))
    SGR._text(d, (212, 190), "EPIP-01", 56, (150, 30, 30), width=290)
    SGR._text(d, (212, 260), "EMERGENCY CLASSIFICATION", 22, (24, 24, 26), width=290)
    SGR._text(d, (212, 380), "SHIFT MANAGER", 30, (250, 250, 246), width=350)
    SGR._text(d, (212, 440), "CONTROLLED COPY", 24, (250, 220, 120), width=350)
    return img


def _plate_tex():
    img = M.painted((214, 216, 214), 920, seams=(), chip_count=0, drip_count=0, dirt=0.02)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, img.width - 1, img.height - 1], outline=(150, 152, 150), width=3)
    return img


def _page_keys():
    img = Image.new("RGBA", (128, 128), (30, 30, 32, 255))
    d = ImageDraw.Draw(img)
    for k, (word, col) in enumerate((("PAGE", (220, 220, 214)), ("ALL CALL", (210, 60, 50)), ("PARTY", (220, 220, 214)))):
        x0 = 10 + k * 38
        d.rectangle([x0, 50, x0 + 32, 78], fill=col)
        SGR._text(d, (x0 + 16, 92), word, 11, (230, 230, 226), width=36)
    return img


SCBA_UV = [0, 0, 16, 3.15]


def _scba_sign():
    """The SCBA cabinet's sign (its top 512 by 101: the sign's shape)."""
    img = Image.new("RGBA", (512, 512), (200, 30, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 507, 96], outline=(250, 250, 246), width=4)
    SGR._text(d, (128, 50), "SCBA", 64, (250, 250, 246))
    SGR._text(d, (360, 34), "EMERGENCY USE ONLY", 26, (250, 250, 246), width=270)
    SGR._text(d, (360, 70), "CONTROL ROOM CREW", 22, (250, 250, 246), width=270)
    return img


def _atc():
    """The At The Controls sign (its top 512 by 242: the plate's shape)."""
    img = Image.new("RGBA", (512, 512), (178, 30, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 14, 497, 227], fill=(250, 250, 246))
    d.rectangle([24, 24, 487, 217], outline=(178, 30, 30), width=6)
    SGR._text(d, (256, 74), "AT THE", 52, (178, 30, 30), width=440)
    SGR._text(d, (256, 128), "CONTROLS", 60, (178, 30, 30), width=440)
    SGR._text(d, (256, 184), "LICENSED OPERATORS ONLY", 28, (24, 24, 26), width=440)
    return img


EAL_ROWS = [("R", "ABNORMAL RAD LEVELS / EFFLUENT"), ("F", "FISSION PRODUCT BARRIERS"), ("H", "HAZARDS AND JUDGMENT"),
            ("S", "SYSTEM MALFUNCTION")]
EAL_COLS = [("UNUSUAL EVENT", (60, 140, 80)), ("ALERT", (230, 190, 40)), ("SITE AREA", (232, 130, 40)), ("GENERAL", (200, 40, 36))]


def _eal(c):
    """The EAL chart (two blocks across): the title; the recognition categories down its side, the four classes across,
    each cell its initiating conditions' numbers and a line of words."""
    W, H = 1024, 448
    img = Image.new("RGBA", (W, 512), (248, 248, 242, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, 56], fill=(24, 36, 70))
    SGR._text(d, (W // 2, 28), "EMERGENCY ACTION LEVEL MATRIX  -  HOT CONDITIONS (MODES 1-4)", 30, (250, 250, 246), width=W - 40)
    x0, cw = 200, (W - 210) // 4
    for k, (name, col) in enumerate(EAL_COLS):
        d.rectangle([x0 + k * cw, 62, x0 + (k + 1) * cw - 4, 96], fill=col)
        SGR._text(d, (x0 + k * cw + cw // 2, 79), name, 22, (20, 20, 22) if k == 1 else (250, 250, 246), width=cw - 12)
    rh = (H - 104) // 4
    for r, (letter, words) in enumerate(EAL_ROWS):
        y0 = 102 + r * rh
        d.rectangle([6, y0, 194, y0 + rh - 6], fill=(220, 224, 232))
        SGR._text(d, (40, y0 + rh // 2 - 4), letter, 46, (24, 36, 70))
        SGR._text(d, (126, y0 + rh // 2 - 4), words, 13, (24, 24, 26), width=120)
        for k, (name, col) in enumerate(EAL_COLS):
            cx0 = x0 + k * cw
            d.rectangle([cx0, y0, cx0 + cw - 4, y0 + rh - 6], outline=col, width=4)
            for j in range(3):
                code = f"{letter}{'UASG'[k]}{j + 1}"
                d.text((cx0 + 10, y0 + 6 + j * 25), code, font=_f(15), fill=(24, 24, 26))
                d.line([(cx0 + 56, y0 + 15 + j * 25), (cx0 + cw - 16 - (j * 23 % 40), y0 + 15 + j * 25)], fill=(110, 110, 120), width=3)
    half = img.crop((c * 512, 0, c * 512 + 512, 512))
    return half


def makers():
    solid = PP._solid
    out = {
        "pi_cr_body": lambda: solid((40, 42, 46), 930, 0.05), "pi_cr_top": lambda: solid((34, 34, 36), 931, 0.03),
        "pi_cr_alum": lambda: solid((176, 180, 184), 932, 0.03), "pi_cr_led": lambda: solid((90, 230, 120), 933, 0.0),
        "pi_cr_grille": lambda: solid((60, 62, 66), 934, 0.12), "pi_cr_logcover": lambda: solid((40, 60, 110), 935, 0.06),
        "pi_cr_shade": lambda: solid((30, 70, 50), 936, 0.04), "pi_cr_bulb": lambda: solid((255, 246, 210), 937, 0.0),
        "pi_cr_rack": lambda: solid((150, 154, 158), 938, 0.06), "pi_cr_binder": lambda: solid((210, 210, 206), 939, 0.05),
        "pi_cr_scba_red": lambda: solid((176, 30, 28), 940, 0.06), "pi_cr_cylinder": lambda: solid((226, 190, 40), 941, 0.05),
        "pi_cr_maskbag": lambda: solid((40, 40, 44), 942, 0.08), "pi_cr_frame": lambda: solid((60, 50, 40), 943, 0.08),
        "pi_cr_atc_edge": lambda: solid((178, 30, 30), 944, 0.02), "pi_cr_atc_red": lambda: solid((176, 32, 34), 945, 0.08),
        "pi_cr_atc_white": lambda: solid((230, 230, 224), 946, 0.06), "pi_cr_glass": lambda: PP._see_through((200, 220, 228), 50, 947),
        "pi_cr_screen": _screen, "pi_cr_plate": _plate_tex, "pi_cr_page": _page_keys, "pi_cr_scba_sign": _scba_sign, "pi_cr_atc": _atc,
        "pi_cr_eop": lambda: _page_tex("E-0", "REACTOR TRIP OR SAFETY INJECTION", EOP_STEPS),
        "pi_cr_ep": _ep_cover,
        "pi_cr_log": lambda: _log_tex("RO LOG  DAYS"), "pi_cr_sslog": lambda: _log_tex("SHIFT MANAGER LOG"),
        "pi_cr_eal_0": lambda: _eal(0), "pi_cr_eal_1": lambda: _eal(1),
    }
    for k, rgb in enumerate(BINDER_RGB):
        out[f"pi_cr_binder_{k}"] = (lambda rgb=rgb, k=k: solid(rgb, 950 + k, 0.04))
    return out


SHARP = {"pi_cr_eop", "pi_cr_ep", "pi_cr_log", "pi_cr_sslog", "pi_cr_eal_0", "pi_cr_eal_1", "pi_cr_atc", "pi_cr_scba_sign", "pi_cr_page",
         "pi_cr_screen"}
SEE_THROUGH = {"pi_cr_glass"}


# ------------------------------------------------------------------------------------------------ registration
# (Two rows: the screens' upper halves in the upper row, so a click anywhere on a screen lands: nuclear/CrConsoleBlock.)
PANELS = {CC_KEY: {"display": "Control Room Operator Console", "width": 3, "rows": 2, "item3d": True, "labels": POSITIONS, "shared": True},
          SS_KEY: {"display": "Shift Manager's Desk", "width": 3, "rows": 2, "item3d": True},
          PR_KEY: {"display": "Control Room Procedure Rack", "width": 1, "item3d": True, "labels": PR_LABELS}}
DRAWINGS = {CC_KEY: cr_console, SS_KEY: ss_desk, PR_KEY: procedure_rack}
BOXES = {CC_KEY: lambda open_=False: cr_console_boxes(), SS_KEY: lambda open_=False: ss_desk_boxes(), PR_KEY: procedure_rack_boxes}
DESK = {"P": "#c:plates/steel", "G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "B": "minecraft:black_dye"}
CRAFTING = {
    CC_KEY: (["GGG", "PCP", "PBP"], DESK, 1),
    SS_KEY: (["G G", "PCP", "PBP"], DESK, 1),
    PR_KEY: (["PBP", "PBP", "PBP"], {"P": "#c:plates/iron", "B": "minecraft:book"}, 1),
    "cr_scba_cabinet": (["RGR", "RIR", "RGR"], {"R": "minecraft:red_dye", "G": "minecraft:glass_pane", "I": "minecraft:iron_ingot"}, 1),
    "cr_eal_chart": (["SPS"], {"S": "minecraft:stick", "P": "minecraft:paper"}, 1),
    "cr_atc_sign": (["C C", "RWR"], {"C": "minecraft:chain", "R": "minecraft:red_dye", "W": "minecraft:white_dye"}, 1),
    "cr_atc_line": (["RWR"], {"R": "minecraft:red_carpet", "W": "minecraft:white_carpet"}, 6),
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.RISERS.update(MULTI)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.NOT_LIVE.add(PR_KEY)
    PP.SEE_THROUGH |= SEE_THROUGH
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(PP.variants(key))
                 for p in range(PANELS[key]["width"] * PANELS[key].get("rows", 2))}


register()
