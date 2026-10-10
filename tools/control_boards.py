"""The main control room's boards (round 23; Justin, 8 October 2026: "a realistic reactor control board and varied
boards", the reactor console "in line with the boards, as a real control board"): one benchboard section per plant
system, each laid out from its own parts the way a real Westinghouse four-loop control room's are, so no two read
the same. Every switch, meter, recorder, controller, lamp and annunciator window is real geometry standing off the
board (nothing is painted on), in the equipment sheet's materials (tools/nuclear.py EQ_MATERIALS).

A board is a list of drawing operations, written to assets/jugcraft/equipment/control_boards.json and replayed by the
client (client/ControlBoards.java) into the equipment renderer's meshes, so what is previewed here is what the game
draws. The frame is the equipment's own: x to the right (blocks), y up, z away from the operators, the desk's front
edge at z 0.12 and the vertical board's face at z 1.2. Operations:

    ["b", x0, y0, z0, x1, y1, z1, faces]                       an axis-aligned box
    ["o", cx, cy, cz, ux, uy, uz, vx, vy, vz, hu, hv, hw, faces]  a box turned to the axes u (right) and v (up);
                                                                   its front is toward -(u x v)... see _obox
    ["q", x, y, z (top-left), bottom-left, bottom-right, top-right, material]    one quad, seen from its front
    ["c", ax, ay, az, bx, by, bz, r, sides, material, cap]       a cylinder (a knob, a pushbutton), cap at b

faces is one material for every face, or six: [front (-z), back, left (-x), right, top, bottom], null for a face
that is never seen (against the board), so no face is ever drawn twice in one plane. A material is a sheet material's
name (a surface: repeated every block across a face, as the renderer's own boxes are), or [name, u0, v0, u1, v1] for
part of its cell (fractions of the cell), stretched over the face (a lens, a screen, a patch of colour).

Each board has three lists: still (lit like the room), lit (full bright: lamps, screens, lit windows) and blink
(alarm windows that flash now and then: quads with two materials; the first is drawn with the still parts a hair
deeper in the board, BLINK_DEPTH, and the second lit in front of it while it flashes). core: how many of the still
operations the far-away version draws (the board's body).
"""
import json
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M

# ---------------------------------------------------------------- the boards, their names and sizes (wide, deep, tall)

BOARDS = {
    "esf_control_board": ("Safety Systems Control Board", 6, 2, 3),
    "rcs_control_board": ("Reactor Coolant Control Board", 6, 2, 3),
    "secondary_control_board": ("Steam and Feedwater Control Board", 6, 2, 3),
    "turbine_control_board": ("Turbine Generator Control Board", 6, 2, 3),
    "electrical_control_board": ("Electrical Control Board", 6, 2, 3),
    "radiation_control_board": ("Radiation and Ventilation Control Board", 6, 2, 3),
}
CONSOLE = "reactor_control_console"     # the working one (a board three wide; the screens and lamps are live)
BLINK_DEPTH = 0.004                     # how far behind a flashing window its unlit face is drawn

# The new materials on the equipment sheet (row 7).
NEW_MATERIALS = ["lamp_white", "lamp_amber", "annunciator", "meter", "dial", "chart", "drpi", "swatches"]
# Row 8 (round 24, the remote shutdown panel; Justin: "make it look more realistic with detail"): engraved nameplates
# with real legends in a 3 x 5 pixel font, the panel's title plates, digital indicators, placards and procedure binders.
TEXT_MATERIALS = ["nameplates", "nameplates2", "nameplates_inst", "titles", "digital", "placards", "binders", "nameplates3"]

# The nameplates' legends: each cell holds 16 plates (2 across, 8 down, 32 x 8 pixels), read left to right.
NAMEPLATES = {
    "nameplates": ["AFW PP A", "AFW PP B", "TDAFW PP", "CHG PP A", "CHG PP B", "PZR HT A", "PZR HT B", "PORV A",
                   "PORV B", "BLOCK A", "BLOCK B", "RHR PP A", "RHR PP B", "CCW PP A", "CCW PP B", "SW PP A"],
    "nameplates2": ["FCV 1", "FCV 2", "FCV 3", "FCV 4", "SG1 ARV", "SG2 ARV", "SG3 ARV", "SG4 ARV",
                    "SW PP B", "BA PP A", "BA PP B", "LTDN ISO", "EDG A", "EDG B", "MSIV", "STM SPLY"],
    "nameplates_inst": ["PZR PRES", "PZR LVL", "RCS TH", "RCS TC", "SG1 LVL", "SG2 LVL", "SG3 LVL", "SG4 LVL",
                        "AFW FLOW", "CST LVL", "SR FLUX", "SUR", "TRIP A", "TRIP B", "SG PRES", "BAT LVL"],
    # The 480 V load centre (round 25): its breakers' feeders, its meters, its warnings.
    "nameplates3": ["MAIN BKR", "XFMR", "MCC 1", "MCC 2", "MCC 3", "MCC 4", "CHARGER", "INVERTER",
                    "HVAC", "LIGHTING", "SPARE", "LOAD CTR", "CHARGE", "BUS V", "480 V", "4160 V"],
}
# The red and yellow plates among the instruments' (a reactor trip's, a warning's).
PLATE_COLOURS = {("nameplates_inst", 12): "red", ("nameplates_inst", 13): "red", ("nameplates3", 14): "red", ("nameplates3", 15): "red"}
# Title plates: eight rows of 64 x 8 (a plate may take two rows: the panel's own title).
TITLES = [("REMOTE SHUTDOWN", "red"), ("PANEL - UNIT 1", "red"), ("TRAIN A", "orange"), ("TRAIN B", "blue"),
          ("AUX FEEDWATER", "white"), ("REACTOR COOLANT", "white"), ("CONTROL TRANSFER", "yellow"), ("INDICATION", "white")]
# Digital indicators' readings (16, as the nameplates): red LED digits.
READINGS = ["2235", "54.8", "557.2", "548.1", "44.0", "46.2", "45.1", "43.8", "412", "92.5", "1.2E2", "-0.03",
            "1005", "998", "62.4", "128.6"]

# Swatch cell: 4 x 4 patches. Mimic colours, plain colours, and label strips (row 3).
SWATCH = {
    "red": (0, 0), "orange": (1, 0), "yellow": (2, 0), "green": (3, 0),
    "blue": (0, 1), "lblue": (1, 1), "purple": (2, 1), "white": (3, 1),
    "black": (0, 2), "dgrey": (1, 2), "mgrey": (2, 2), "enamel": (3, 2),
    "label": (0, 3), "label_white": (1, 3), "label_red": (2, 3), "label_yellow": (3, 3),
}
SWATCH_RGB = {
    "red": (196, 40, 34), "orange": (230, 118, 28), "yellow": (232, 196, 40), "green": (36, 146, 66),
    "blue": (36, 86, 186), "lblue": (104, 166, 218), "purple": (124, 66, 156), "white": (234, 234, 228),
    "black": (20, 20, 22), "dgrey": (58, 60, 64), "mgrey": (118, 120, 122), "enamel": (186, 190, 186),
}


def sw(name):
    c, r = SWATCH[name]
    e = 0.06
    return ["swatches", (c + e) / 4, (r + e) / 4, (c + 1 - e) / 4, (r + 1 - e) / 4]


def pic(mat):
    """A picture material (a lamp's lens, a screen): the whole cell stretched over the face, never repeated."""
    return [mat, 0.01, 0.01, 0.99, 0.99]


def quarter(mat, i):
    """One of a cell's four quarters (0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right)."""
    c, r = i % 2, i // 2
    e = 0.02
    return [mat, (c + e) / 2, (r + e) / 2, (c + 1 - e) / 2, (r + 1 - e) / 2]


def strip(mat, i, n=4):
    """One of a cell's n vertical strips."""
    e = 0.04
    return [mat, (i + e) / n, 0.02, (i + 1 - e) / n, 0.98]


def plate(cell, i):
    """Nameplate i of a nameplate cell (or a digital indicator's reading): 2 across, 8 down."""
    c, r = i % 2, i // 2
    return [cell, c / 2 + 0.004, r / 8 + 0.004, (c + 1) / 2 - 0.004, (r + 1) / 8 - 0.004]


def title(i, rows=1):
    """Title plate i (a row of the titles cell; two rows for a two-line plate)."""
    return ["titles", 0.004, i / 8 + 0.004, 0.996, (i + rows) / 8 - 0.004]


def named(legend):
    """The nameplate with this legend, from whichever cell holds it."""
    for cell, legends in NAMEPLATES.items():
        if legend in legends:
            return plate(cell, legends.index(legend))
    raise KeyError(legend)


# ---------------------------------------------------------------- the board's profile

DESK_FRONT = (0.86, 0.12)     # (y, z) of the desk's front edge
DESK_BACK = (1.14, 1.2)       # where the desk meets the vertical board
FACE = 1.2                    # the vertical board's face (z)
TOP = 2.95                    # the top of the vertical board, under the hood
TRIM = "gunmetal"
# Rows on the vertical board (y of their middles) and on the desk (s: 0 at its front edge, 1 at the back).
ROW1, ROW2, ROW3 = 2.36, 1.95, 1.47
LABEL1, LABEL2 = 2.175, 1.735
# The reactor console's live parts (client/NuclearRenderer.java consoleLamps draws them in these openings).
SCREENS = [(0.15 + i * 0.95, 1.35, 0.15 + i * 0.95 + 0.85, 1.85) for i in range(3)]   # x0, y0, x1, y1 at FACE - 0.04
SCREEN_Z = FACE - 0.04
LAMP_STRIP = (0.2, 1.94, 2.84, 2.02)       # eight lamps in it, at FACE - 0.02
LAMP_Z = FACE - 0.02
# The remote shutdown panel's live status screen (x0, y0, x1, y1 at RSP_SCREEN_Z; client/NuclearRenderer.java).
RSP_SCREEN = (2.62, 1.75, 3.38, 2.45)
RSP_SCREEN_Z = 0.3 - 0.04
# Its status lamps (x, y of each lens's middle, at RSP_LAMP_Z): control room in control (white), this panel in
# control (amber), reactor tripped (red).
RSP_LAMPS = [(2.75, 1.64), (3.0, 1.64), (3.25, 1.64)]
RSP_LAMP = 0.05
RSP_LAMP_Z = 0.3 - 0.016


def body():
    return sw("enamel")


class Board:
    def __init__(self, wide, seed, face=FACE):
        self.w = wide
        self.face = face          # the depth of the board's vertical face (z)
        self.still, self.lit, self.blink = [], [], []
        # Parts drawn one way or another by state (the remote shutdown panel's transfer switches: control at the control
        # room, "mcr", or at the panel, "rsp"): each state's own still and lit operations, drawn over the board's.
        self.states = {}
        self.r = random.Random(seed)

    # -- primitive operations

    def box(self, x0, y0, z0, x1, y1, z1, faces, lst=None):
        (self.still if lst is None else lst).append(["b", *_r(x0, y0, z0, x1, y1, z1), faces])

    def quad(self, tl, bl, br, tr, mat, lst=None):
        (self.still if lst is None else lst).append(["q", *_r(*tl, *bl, *br, *tr), mat])

    def front_quad(self, x0, y0, x1, y1, z, mat, lst):
        """A face toward the operators (seen from -z) at depth z."""
        self.quad((x0, y1, z), (x0, y0, z), (x1, y0, z), (x1, y1, z), mat, lst)

    def cyl(self, a, b, r, mat, cap=None, sides=10, lst=None):
        (self.still if lst is None else lst).append(["c", *_r(*a, *b, r), sides, mat, cap if cap is not None else mat])

    def obox(self, c, u, v, hu, hv, hw, faces, lst=None):
        (self.still if lst is None else lst).append(["o", *_r(*c, *u, *v, hu, hv, hw), faces])

    # -- the benchboard

    def profile(self, name=None):
        w = self.w
        yf, zf = DESK_FRONT
        yb, zb = DESK_BACK
        e = body()
        # The desk's body, the kick strip, the sloped top (with its sides) and its rolled front edge.
        self.box(0.04, 0.1, zf, w - 0.04, yf - 0.06, zb, [e, None, e, e, None, None])
        self.box(0.04, 0, zf + 0.06, w - 0.04, 0.1, zb, [sw("black"), None, sw("black"), sw("black"), None, None])
        self.quad((0.04, yb, zb), (0.04, yf, zf), (w - 0.04, yf, zf), (w - 0.04, yb, zb), e)
        self.quad((0.04, yb, zb), (0.04, yf - 0.06, zb), (0.04, yf - 0.06, zf), (0.04, yf, zf), e)
        self.quad((w - 0.04, yf, zf), (w - 0.04, yf - 0.06, zf), (w - 0.04, yf - 0.06, zb), (w - 0.04, yb, zb), e)
        self.box(0.04, yf - 0.06, zf - 0.04, w - 0.04, yf + 0.006, zf, [TRIM, None, TRIM, TRIM, TRIM, TRIM])
        # The vertical board and its hood, the section's name along the hood's edge.
        self.box(0.04, 0, self.face, w - 0.04, TOP, 1.96, [e, e, e, e, None, None])
        self.box(0.04, TOP, 0.98, w - 0.04, 3.0, 1.96, [TRIM, TRIM, TRIM, TRIM, TRIM, TRIM])
        if name:
            x0, x1 = w / 2 - 0.6, w / 2 + 0.6
            self.box(x0, TOP + 0.008, 0.972, x1, 3.0 - 0.008, 0.98, [sw("label")] * 5 + [sw("label")])
        # Far away the renderer draws only this much of the board (and its lit parts).
        self.core = len(self.still)

    def cabinet(self, sections):
        """A vertical switchboard one block deep (the remote shutdown panel): sections bolted side by side (sections:
        (x0, x1, doors) each, the seams between them dark lines up the face), its face at self.face, a writing ledge
        across the front at waist height, under it each section's doors (hinges, a latch handle, louvres), a black
        kick plate below and a cap on top."""
        w = self.w
        e = body()
        self.box(0.04, 0.1, self.face, w - 0.04, TOP, 0.98, [e, e, e, e, None, None])
        self.box(0.04, 0, self.face + 0.06, w - 0.04, 0.1, 0.98, [sw("black"), None, sw("black"), sw("black"), None, None])
        self.box(0.04, TOP, self.face - 0.08, w - 0.04, 3.0, 0.98, [TRIM, TRIM, TRIM, TRIM, TRIM, TRIM])
        self.box(0.06, 0.9, self.face - 0.28, w - 0.06, 0.95, self.face, [TRIM, None, TRIM, TRIM, TRIM, TRIM])
        self.core = len(self.still)
        for x0, x1, doors in sections:
            if x0 > 0.1:
                self.face_box(x0, (0.12 + TOP) / 2, 0.008, TOP - 0.14, 0.003, sw("black"), sw("black"))
            dw = (x1 - x0) / doors
            for k in range(doors):
                d0, d1 = x0 + k * dw + 0.03, x0 + (k + 1) * dw - 0.03
                mid = (d0 + d1) / 2
                self.face_box(mid, 0.84, d1 - d0, 0.012, 0.004, sw("dgrey"), sw("dgrey"))
                self.face_box(d1 - 0.07, 0.5, 0.025, 0.16, 0.03, sw("mgrey"), sw("mgrey"))
                self.cyl((d1 - 0.07, 0.6, self.face - 0.004), (d1 - 0.07, 0.6, self.face - 0.02), 0.016, sw("dgrey"), sw("black"), 8)
                for hy in (0.24, 0.7):
                    self.face_box(d0 + 0.02, hy, 0.018, 0.07, 0.014, sw("dgrey"), sw("dgrey"))
                for j in range(4):
                    self.face_box(mid - 0.08, 0.2 + j * 0.055, min(0.42, d1 - d0 - 0.3), 0.018, 0.006, sw("dgrey"), sw("dgrey"))

    def nameplate(self, x, y, mat, w=0.12, h=0.03, d=0.006):
        """An engraved nameplate (white lamacoid, black letters) on the face."""
        self.face_box(x, y, w, h, d, mat, sw("white"))

    def placard(self, x, y, w, h, mat):
        self.face_box(x, y, w, h, 0.003, mat, sw("white"))

    def zone(self, pts, colour="black"):
        """A demarcation line round a group of controls (a closed path of (x, y) points, turning square)."""
        self.mimic(pts, colour, 0.012)

    def digital_indicator(self, x, y, reading):
        """A digital panel meter: its black case and the lit reading in red LED digits."""
        self.face_box(x, y, 0.17, 0.07, 0.035, sw("black"), sw("dgrey"))
        self.front_quad(x - 0.07, y - 0.022, x + 0.07, y + 0.022, self.face - 0.039, plate("digital", reading), self.lit)

    def key_switch(self, x, y, legend):
        """A transfer switch: its plate, the key-lock barrel with the key in it, the lamps above, its nameplate below.
        By state: with control at the control room ("mcr") the key stands upright and the white lamp is lit; turned to
        the panel ("rsp") it lies across and the amber lamp is lit."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("mgrey"), sw("dgrey"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.04), 0.022, sw("dgrey"), sw("black"), 8)
        for dx in (-0.021, 0.021):
            self.lamp_rim(x + dx, y + 0.085, 0.026)
        for name, local in (("mcr", False), ("rsp", True)):
            s = self.state(name)
            hx, hy = (0.022, 0.006) if local else (0.006, 0.022)
            self.box(x - hx, y - hy, self.face - 0.07, x + hx, y + hy, self.face - 0.04, [sw("yellow")] * 5 + [None], s["still"])
            for dx, colour in ((-0.021, "off_white" if local else "white"), (0.021, "amber" if local else "off_amber")):
                h = 0.026 / 2 + 0.005
                self.front_quad(x + dx - h, y + 0.085 - h, x + dx + h, y + 0.085 + h, self.face - 0.012, LENS[colour],
                                s["still"] if colour.startswith("off") else s["lit"])
        self.nameplate(x, y - 0.08, named(legend))

    def lamp_rim(self, x, y, size=0.03, z=None, d=0.012):
        """An indicating lamp's rim alone (its lens drawn by state, or by the renderer)."""
        z = self.face if z is None else z
        rim = sw("dgrey")
        h = size / 2 + 0.005
        self.box(x - h, y - h, z - d, x + h, y + h, z, [None, None, rim, rim, rim, rim])

    def control_switch(self, x, y, legend, on, grip=True):
        """A control switch on the face: its brushed escutcheon, a black pistol grip (a pump, a heater) or a knob (a
        valve), the red and green lamps above (only the one for its state lit), its nameplate below."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("mgrey"), sw("dgrey"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.03), 0.022, sw("black"), sw("dgrey"), 8)
        if grip:
            turn = 0.35 if on else -0.35
            u = (math.cos(turn), math.sin(turn), 0)
            self.obox((x, y, self.face - 0.042), u, (-u[1], u[0], 0), 0.04, 0.013, 0.012, sw("black"))
        else:
            self.cyl((x, y, self.face - 0.03), (x, y, self.face - 0.05), 0.03, sw("black"), sw("dgrey"), 10)
        self.face_lamp(x - 0.021, y + 0.085, "red" if on else "off_red", 0.026)
        self.face_lamp(x + 0.021, y + 0.085, "off_green" if on else "green", 0.026)
        self.nameplate(x, y - 0.08, named(legend))

    def face_guarded(self, x, y, legend):
        """A reactor trip switch on a vertical panel: a big handle on its red plate under a hinged red guard, its red
        nameplate below."""
        self.face_box(x, y, 0.16, 0.16, 0.014, sw("red"))
        self.face_box(x, y, 0.1, 0.03, 0.045, sw("black"))
        for dx in (-0.075, 0.075):
            self.face_box(x + dx, y, 0.012, 0.14, 0.08, sw("red"), sw("red"))
        self.box(x - 0.074, y - 0.069, self.face - 0.079, x + 0.074, y - 0.057, self.face - 0.014, [sw("red")] * 6)
        self.nameplate(x, y - 0.115, named(legend), 0.14)

    def phone(self, x, y):
        """The plant telephone: its grey case, keypad and the black handset on its hook."""
        self.face_box(x, y, 0.13, 0.22, 0.04, sw("mgrey"), sw("dgrey"))
        self.face_box(x + 0.025, y - 0.04, 0.06, 0.08, 0.044, sw("black"), sw("black"))
        for i in range(3):
            for j in range(4):
                self.face_box(x + 0.007 + i * 0.018, y - 0.07 + j * 0.02, 0.012, 0.012, 0.05, sw("white"), sw("mgrey"))
        hx = x - 0.03
        self.face_box(hx, y + 0.01, 0.032, 0.17, 0.07, sw("black"), sw("black"))
        for dy in (0.075, -0.06):
            self.face_box(hx, y + 0.01 + dy, 0.048, 0.045, 0.082, sw("black"), sw("dgrey"))
        # The cord in coils from the handset's foot to the case.
        for k in range(5):
            self.face_box(hx - 0.004 + (k % 2) * 0.008, y - 0.11 - k * 0.018, 0.012, 0.012, 0.03, sw("black"), sw("black"))

    def jack_box(self, x, y):
        """A sound-powered phone station: a grey box with two headset jacks."""
        self.face_box(x, y, 0.1, 0.12, 0.03, sw("mgrey"), sw("dgrey"))
        for dy in (0.025, -0.025):
            self.cyl((x, y + dy, self.face - 0.03), (x, y + dy, self.face - 0.045), 0.014, sw("black"), sw("black"), 8)

    def open_binder(self, x0, x1):
        """The procedure, open on the ledge: its cover and the two pages (the procedure's steps)."""
        y = 0.95
        z0, z1 = self.face - 0.25, self.face - 0.03
        self.box(x0, y, z0, x1, y + 0.012, z1, [sw("blue"), sw("blue"), sw("blue"), sw("blue"), sw("blue"), None])
        mid = (x0 + x1) / 2
        for k, (a, c) in enumerate(((x0 + 0.012, mid - 0.004), (mid + 0.004, x1 - 0.012))):
            self.box(a, y + 0.012, z0 + 0.012, c, y + 0.026, z1 - 0.012, [sw("white"), sw("white"), sw("white"), sw("white"), None, None])
            yt = y + 0.026
            self.quad((a, yt, z1 - 0.012), (a, yt, z0 + 0.012), (c, yt, z0 + 0.012), (c, yt, z1 - 0.012),
                      ["binders", 0.504, k * 0.5 + 0.004, 0.996, k * 0.5 + 0.496])
        self.box(mid - 0.003, y + 0.012, z0 + 0.03, mid + 0.003, y + 0.034, z1 - 0.03, sw("mgrey"))

    def closed_binder(self, x0, x1, spine):
        """A binder lying shut on the ledge, its spine (with its label) toward the operators."""
        y0, y1 = 0.95, 0.988
        z0, z1 = self.face - 0.26, self.face - 0.03
        colour = ("red", "blue", "white", "black")[spine]
        self.box(x0, y0, z0, x1, y1, z1, [None, sw(colour), sw(colour), sw(colour), sw(colour), None])
        # The spine: the strip turned on its side (the texture's length along the binder).
        self.quad((x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), ["binders", spine / 8 + 0.004, 0.01, (spine + 1) / 8 - 0.004, 0.99])

    # -- placing things on the desk (s: 0 at its front edge, 1 at the back)

    def slope(self, s):
        yf, zf = DESK_FRONT
        yb, zb = DESK_BACK
        y, z = yf + (yb - yf) * s, zf + (zb - zf) * s
        t = np.array([0, yb - yf, zb - zf], float)
        t /= np.linalg.norm(t)
        n = np.array([0, t[2], -t[1]])   # the desk's up: up and toward the operators
        return np.array([0, y, z]), t, n

    def on_desk(self, x, s, h):
        """The point h above the desk at (x, s), and the desk's tangent (toward the back) and normal."""
        p, t, n = self.slope(s)
        return p + np.array([x, 0, 0]) + n * h, t, n

    def desk_block(self, x, s, wu, wt, h, faces, lst=None, h0=0.0):
        """A block lying on the desk (from h0 up to h): wu wide, wt along the slope; front toward the front edge."""
        c, t, n = self.on_desk(x, s, (h0 + h) / 2)
        self.obox(c, (1, 0, 0), tuple(n), wu / 2, (h - h0) / 2, wt / 2, faces, lst)

    # -- desk parts

    def switch(self, x, s, kind="j", collar=None, lamps=("red", "green"), label="label"):
        """A control switch: its escutcheon plate (a coloured collar for a safety train), the handle (a J handle,
        a pistol grip or a knob), the red and green position lamps above it and its engraved label below."""
        plate = sw(collar) if collar else sw("black")
        self.desk_block(x, s, 0.11, 0.11, 0.012, [plate] * 5 + [None])
        p, t, n = self.on_desk(x, s, 0.012)
        if kind == "knob":
            self.cyl(tuple(p), tuple(p + n * 0.045), 0.03, sw("black"), sw("dgrey"), 10)
        else:
            ln = 0.075 if kind == "j" else 0.06
            # The handle across the plate, and for a J handle its hooked end standing up on it.
            self.obox(tuple(p + n * 0.02), (1, 0, 0), tuple(n), ln / 2, 0.02, 0.012, sw("black"))
            if kind == "j":
                self.obox(tuple(p + n * 0.0535 + np.array([ln / 2 - 0.009, 0, 0])), (1, 0, 0), tuple(n), 0.009, 0.0105, 0.012,
                          [sw("black")] * 5 + [None])
        if lamps:
            for k, colour in enumerate(lamps):
                self.lamp_on_desk(x + (k - (len(lamps) - 1) / 2) * 0.042, s + 0.085, colour)
        if label:
            self.desk_block(x, s - 0.078, 0.1, 0.03, 0.006, [sw(label)] * 5 + [None])

    def lamp_on_desk(self, x, s, colour, size=0.026):
        c, t, n = self.on_desk(x, s, 0.012)
        rim = sw("dgrey")
        self.obox(tuple(c), (1, 0, 0), tuple(n), size / 2 + 0.006, 0.012, size / 2 + 0.006, [rim, rim, rim, rim, None, None])
        top = c + n * 0.012
        u = np.array([1, 0, 0])
        h = size / 2 + 0.006
        self.quad(tuple(top - u * h + t * h), tuple(top - u * h - t * h), tuple(top + u * h - t * h), tuple(top + u * h + t * h),
                  LENS[colour], self.lit if colour != "off" else None)

    def button(self, x, s, colour, lit=True, size=0.04):
        """A square illuminated push button (a lit lens on a dark collar)."""
        self.lamp_on_desk(x, s, colour if lit else "off", size)

    def guarded_switch(self, x, s, label="label_red"):
        """A reactor trip or safety injection switch: a big handle under a hinged red guard, a red label."""
        self.desk_block(x, s, 0.16, 0.16, 0.014, [sw("red")] * 5 + [None])
        p, t, n = self.on_desk(x, s, 0.014)
        self.obox(tuple(p + n * 0.03), (1, 0, 0), tuple(n), 0.05, 0.03, 0.016, [sw("black")] * 5 + [None])
        # The guard: two cheeks and the bar over the handle (its ends inside the cheeks).
        for dx in (-0.075, 0.075):
            self.obox(tuple(p + n * 0.04 + np.array([dx, 0, 0])), (1, 0, 0), tuple(n), 0.006, 0.04, 0.07, [sw("red")] * 5 + [None])
        self.obox(tuple(p + n * 0.072 + t * 0.03), (1, 0, 0), tuple(n), 0.074, 0.006, 0.034, sw("red"))
        self.desk_block(x, s - 0.12, 0.16, 0.035, 0.006, [sw(label)] * 5 + [None])

    def mimic_desk(self, pts, colour, width=0.022):
        """A mimic line on the desk ((x, s) points, turning square): runs across lie flat, runs up the desk stand a hair
        higher and carry the corners."""
        for (x0, s0), (x1, s1) in zip(pts, pts[1:]):
            if abs(s1 - s0) < 1e-6:
                xa, xb = min(x0, x1) - width / 2 + 0.004, max(x0, x1) + width / 2 - 0.004
                self.desk_block((xa + xb) / 2, s0, xb - xa, width, 0.004, [sw(colour)] * 5 + [None])
            else:
                k = (width / 2 - 0.002) / 1.116     # half the width, in s
                sa, sb = min(s0, s1) - k, max(s0, s1) + k
                self.desk_block(x0, (sa + sb) / 2, width - 0.004, (sb - sa) * 1.116, 0.006, [sw(colour)] * 5 + [None])

    # -- the vertical board's parts (x, y: the part's middle on the face)

    def face_box(self, x, y, wx, wy, d, front, sides=None, lst=None):
        """A part standing d out from the face: front material on its face, sides dark."""
        sides = sides or sw("black")
        self.box(x - wx / 2, y - wy / 2, self.face - d, x + wx / 2, y + wy / 2, self.face, [front, None, sides, sides, sides, sides], lst)

    def meter(self, x, y=ROW1, i=0, tall=0.3):
        """An edgewise meter: a narrow black case, its scale behind glass."""
        self.face_box(x, y, 0.1, tall, 0.07, strip("meter", i % 4))

    def meters(self, x0, n, y=ROW1, step=0.12, seed=0):
        for k in range(n):
            self.meter(x0 + k * step, y, seed + k)

    def dial(self, x, y=ROW1, i=0, size=0.22):
        """A square-cased round meter (megawatts, the synchroscope)."""
        self.face_box(x, y, size, size, 0.05, quarter("dial", i % 4))

    def recorder(self, x, y=ROW1, i=0, wide=0.3):
        """A strip chart recorder: its door frame and the chart behind the window (lit)."""
        h = 0.3
        z = self.face - 0.12
        self.face_box(x, y, wide, h, 0.12, None, sw("dgrey"))
        self.front_quad(x - wide / 2, y + h / 2 - 0.04, x + wide / 2, y + h / 2, z, sw("dgrey"), None)
        self.front_quad(x - wide / 2, y - h / 2, x + wide / 2, y - h / 2 + 0.03, z, sw("dgrey"), None)
        self.front_quad(x - wide / 2, y - h / 2 + 0.03, x - wide / 2 + 0.025, y + h / 2 - 0.04, z, sw("dgrey"), None)
        self.front_quad(x + wide / 2 - 0.025, y - h / 2 + 0.03, x + wide / 2, y + h / 2 - 0.04, z, sw("dgrey"), None)
        self.front_quad(x - wide / 2 + 0.025, y - h / 2 + 0.03, x + wide / 2 - 0.025, y + h / 2 - 0.04, z,
                        ["chart", (i % 2) * 0.5 + 0.02, 0.02, (i % 2) * 0.5 + 0.48, 0.98], self.lit)

    def controller(self, x, y=ROW3):
        """A manual/automatic controller station: its bar-graph display (lit), the auto and manual lamps, the raise and
        lower buttons."""
        z = self.face - 0.09
        self.face_box(x, y, 0.12, 0.32, 0.09, sw("dgrey"), sw("dgrey"))
        self.face_box(x, y + 0.07, 0.07, 0.13, 0.095, None)
        self.front_quad(x - 0.035, y + 0.005, x + 0.035, y + 0.135, self.face - 0.095, strip("meter", self.r.randint(0, 3)), self.lit)
        for k, colour in enumerate(("green", "amber")):
            self.face_lamp(x - 0.025 + k * 0.05, y - 0.055, colour, 0.03, z)
        for k in range(2):
            self.face_box(x - 0.025 + k * 0.05, y - 0.115, 0.035, 0.03, 0.1, sw("white"), sw("mgrey"))

    def controllers(self, x0, n, y=ROW3, step=0.17):
        for k in range(n):
            self.controller(x0 + k * step, y)

    def face_lamp(self, x, y, colour, size=0.03, z=None, d=0.012):
        """An indicating lamp: its rim and lens (lit, unless off)."""
        z = self.face if z is None else z
        rim = sw("dgrey")
        h = size / 2 + 0.005
        self.box(x - h, y - h, z - d, x + h, y + h, z, [None, None, rim, rim, rim, rim])
        self.front_quad(x - h, y - h, x + h, y + h, z - d, LENS[colour], None if colour.startswith("off") else self.lit)

    def face_switch(self, x, y=ROW3, lamps=("red", "green")):
        """A switch on the vertical board: its plate, a knob, its lamps above."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("black"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.05), 0.025, sw("black"), sw("dgrey"), 8)
        for k, colour in enumerate(lamps):
            self.face_lamp(x + (k - (len(lamps) - 1) / 2) * 0.042, y + 0.085, colour, 0.026)

    def bezel(self, x0, y0, x1, y1, d=0.05, b=0.02):
        """A black frame d out from the face round an opening x0..x1, y0..y1 (a screen's)."""
        self.box(x0 - b, y0 - b, self.face - d, x1 + b, y1 + b, self.face, [None, None, sw("black"), sw("black"), sw("black"), sw("black")])
        z = self.face - d
        self.front_quad(x0 - b, y1, x1 + b, y1 + b, z, sw("black"), None)
        self.front_quad(x0 - b, y0 - b, x1 + b, y0, z, sw("black"), None)
        self.front_quad(x0 - b, y0, x0, y1, z, sw("black"), None)
        self.front_quad(x1, y0, x1 + b, y1, z, sw("black"), None)
        return z

    def screen(self, x, y, wide, high, mat="screen", d=0.05):
        """A flat display in a black bezel (lit)."""
        z = self.bezel(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, d)
        self.front_quad(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, z, pic(mat), self.lit)

    def label(self, x0, x1, y, h=0.03, kind="label"):
        """An engraved label strip on the face."""
        self.face_box((x0 + x1) / 2, y, x1 - x0, h, 0.006, sw(kind), sw(kind))

    def mimic(self, pts, colour, width=0.026):
        """A mimic line on the face ((x, y) points, turning square): runs across stand just off the face, runs up and
        down a little further, carrying the corners."""
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            if abs(y1 - y0) < 1e-6:
                xa, xb = min(x0, x1) - width / 2 + 0.004, max(x0, x1) + width / 2 - 0.004
                self.face_box((xa + xb) / 2, y0, xb - xa, width, 0.008, sw(colour), sw(colour))
            else:
                ya, yb = min(y0, y1) - width / 2 + 0.002, max(y0, y1) + width / 2 - 0.002
                self.face_box(x0, (ya + yb) / 2, width - 0.004, yb - ya, 0.014, sw(colour), sw(colour))

    def pump(self, x, y, colour, r=0.05):
        """A pump on a mimic: a white-capped ring standing off the line, its colour in the middle."""
        self.cyl((x, y, self.face - 0.008), (x, y, self.face - 0.02), r, sw(colour), sw("white"), 12)
        self.cyl((x, y, self.face - 0.02), (x, y, self.face - 0.026), r * 0.55, sw(colour), sw(colour), 12)

    def valve(self, x, y, open_=True):
        """A valve on a mimic: its square with the position lamp (red open, green shut)."""
        self.face_box(x, y, 0.05, 0.05, 0.02, sw("black"))
        self.face_lamp(x, y, "red" if open_ else "green", 0.026, self.face - 0.02, 0.006)

    def vessel(self, x, y, wx, wy, colour):
        """A tank, heat exchanger or vessel on a mimic: a block outlined in white."""
        self.face_box(x, y, wx, wy, 0.022, sw("white"), sw(colour))
        self.face_box(x, y, wx - 0.03, wy - 0.03, 0.026, sw(colour), sw(colour))

    def lamp_grid(self, x0, y0, cols, rows, dx=0.07, dy=0.08, colours=("white",), seed=0, size=0.045):
        r = random.Random(seed)
        for i in range(cols):
            for j in range(rows):
                c = r.choice(colours)
                self.face_lamp(x0 + i * dx, y0 + j * dy, c, size)

    def annunciators(self, x0, x1, rows=3, cols=None, seed=0, alarms=2):
        """A band of annunciator windows across the top of a section: a black frame, the engraved windows, a few in
        alarm (amber steady, one red flashing now and then). Returns the band's bottom."""
        r = random.Random(seed)
        y_top = TOP - 0.02
        y_bot = y_top - rows * 0.085 - 0.02
        self.box(x0, y_bot, self.face - 0.03, x1, y_top, self.face, [None, None, sw("black"), sw("black"), sw("black"), sw("black")])
        cols = cols or max(1, int((x1 - x0 - 0.02) / 0.1))
        cw = (x1 - x0 - 0.02) / cols
        z = self.face - 0.03
        for q in ((x0, y_top - 0.01, x1, y_top), (x0, y_bot, x1, y_bot + 0.01), (x0, y_bot + 0.01, x0 + 0.01, y_top - 0.01),
                  (x1 - 0.01, y_bot + 0.01, x1, y_top - 0.01)):
            self.front_quad(*q, z, sw("black"), None)
        lit = set(r.sample(range(rows * cols), alarms))
        red = set(r.sample(sorted(lit), 1)) if lit else set()
        for i in range(cols):
            for j in range(rows):
                ax0, ax1 = x0 + 0.01 + i * cw, x0 + 0.01 + (i + 1) * cw
                ay1 = y_top - 0.01 - j * 0.085
                ay0 = ay1 - 0.085
                k = j * cols + i
                self.front_quad(ax0, ay0, ax0 + 0.006, ay1, z, sw("black"), None)
                self.front_quad(ax1 - 0.006, ay0, ax1, ay1, z, sw("black"), None)
                self.front_quad(ax0 + 0.006, ay1 - 0.006, ax1 - 0.006, ay1, z, sw("black"), None)
                self.front_quad(ax0 + 0.006, ay0, ax1 - 0.006, ay0 + 0.006, z, sw("black"), None)
                wx0, wy0, wx1, wy1 = ax0 + 0.006, ay0 + 0.006, ax1 - 0.006, ay1 - 0.006
                if k in red:
                    # Flashing: one quad, plain while off and lit red while it flashes (two materials).
                    self.blink.append(["q", *_r(wx0, wy1, z, wx0, wy0, z, wx1, wy0, z, wx1, wy1, z),
                                       quarter("annunciator", r.randint(0, 1)), quarter("annunciator", 3)])
                elif k in lit:
                    self.front_quad(wx0, wy0, wx1, wy1, z, quarter("annunciator", 2), self.lit)
                else:
                    self.front_quad(wx0, wy0, wx1, wy1, z, quarter("annunciator", r.randint(0, 1)), None)
        return y_bot

    def drpi(self, x, y, wide, high):
        """The digital rod position indicator: the reactor's rods as columns of lit position LEDs."""
        z = self.bezel(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, 0.04, 0.025)
        self.front_quad(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, z, pic("drpi"), self.lit)

    def ops(self):
        out = {"still": self.still, "lit": self.lit, "blink": self.blink, "core": getattr(self, "core", len(self.still))}
        if self.states:
            out["states"] = self.states
        if getattr(self, "live", None):
            out["live"] = self.live
        return out

    def state(self, name):
        return self.states.setdefault(name, {"still": [], "lit": []})


LENS = {"red": pic("lamp_red"), "green": pic("lamp_green"), "white": pic("lamp_white"), "amber": pic("lamp_amber"), "off": sw("dgrey"),
        # An unlit lamp of a colour: its lens in the room's light.
        "off_red": sw("red"), "off_green": sw("green"), "off_white": sw("enamel"), "off_amber": sw("orange")}


def _r(*v):
    return [round(float(a), 4) for a in v]


# ---------------------------------------------------------------- the sections

def esf_board(seed=11):
    """Safety systems (ESF): Train A on the left in orange, Train B on the right in blue (their switches in coloured
    collars), each with its flow meters and recorder, the safety injection, residual heat removal and containment
    spray pumps drawn from the refuelling water storage tank to the cold legs, its controllers and valve switches;
    the ESF status panel between the trains, and under it on the desk the manual safety injection switches."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=3, seed=seed)
    b.label(0.25, 2.65, y_ann - 0.04, 0.035, "label_yellow")
    b.label(3.35, 5.75, y_ann - 0.04, 0.035, "label_yellow")
    for colour, x in (("orange", 0.1), ("blue", 3.25)):
        b.meters(x + 0.15, 6, seed=1)
        b.recorder(x + 1.1, i=0)
        b.meters(x + 1.4, 6, seed=3)
        b.dial(x + 2.35, i=0)
        b.label(x + 0.08, x + 0.82, LABEL1)
        b.label(x + 1.33, x + 2.07, LABEL1)
        # The pumps from the tank, the valves out, the header to the cold legs, the RHR heat exchanger.
        b.vessel(x + 0.2, ROW2, 0.22, 0.3, colour)
        for k, (yy, px) in enumerate(((2.06, 0.55), (1.94, 0.75), (1.82, 0.95))):
            b.mimic([(x + 0.31, yy), (x + 2.5, yy)], colour)
            b.pump(x + px, yy, colour, 0.045)
            b.valve(x + 1.3, yy, True)
            b.valve(x + 1.62, yy, k != 2)
        b.vessel(x + 1.98, 1.94, 0.22, 0.07, colour)
        b.mimic([(x + 2.5, 2.06), (x + 2.5, 1.82)], colour)
        b.mimic([(x + 2.53, 1.94), (x + 2.62, 1.94)], colour)
        b.controllers(x + 0.2, 5)
        for k in range(6):
            b.face_switch(x + 1.15 + k * 0.25, 1.44)
        b.label(x + 0.1, x + 0.98, LABEL2)
        b.label(x + 1.05, x + 2.6, LABEL2)
        # The desk: two rows of switches in the train's collars, a mimic line along the front.
        for k in range(8):
            b.switch(x + 0.2 + k * 0.3, 0.74, "knob" if k % 3 == 1 else "j", colour)
            b.switch(x + 0.35 + k * 0.28, 0.46, "j", colour)
        b.mimic_desk([(x + 0.1, 0.24), (x + 2.55, 0.24)], colour)
    # The ESF status panel: the safety functions' equipment in white windows.
    b.label(2.82, 3.18, 2.58, 0.03, "label_yellow")
    b.lamp_grid(2.88, 1.36, 3, 14, 0.12, 0.085, ("white", "white", "white", "off"), seed=seed, size=0.07)
    b.guarded_switch(2.86, 0.5)
    b.guarded_switch(3.14, 0.5)
    for k in range(4):
        b.button(2.8 + k * 0.13, 0.86, "amber" if k % 2 else "white")
    return b


def rcs_board(seed=23):
    """Reactor coolant and chemical and volume control: the four loops round the vessel (red hot legs, blue cold
    legs, each with its steam generator and coolant pump) and the pressurizer on loop two; the coolant pumps'
    ammeters and seal flows, the pressurizer's controllers; on the right the charging and letdown mimic with the volume
    control tank, its recorders, meters and controllers and a screen; on the desk the four coolant pump switches, the
    pressurizer heater banks, relief valves, charging and letdown switches and the boric acid batch counter."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=3, seed=seed, alarms=3)
    b.label(0.2, 2.9, y_ann - 0.04, 0.035)
    b.label(3.1, 5.8, y_ann - 0.04, 0.035)
    # The loops.
    vx, vy = 1.5, 2.2
    b.vessel(vx, vy, 0.24, 0.5, "red")
    for k, (sx, sy) in enumerate(((0.45, 2.4), (2.55, 2.4), (0.45, 2.03), (2.55, 2.03))):
        left = sx < vx
        b.vessel(sx, sy, 0.2, 0.24, "lblue")
        edge_v, edge_s = vx + (-0.12 if left else 0.12), sx + (0.1 if left else -0.1)
        b.mimic([(edge_v, sy + 0.05), (edge_s, sy + 0.05)], "red")
        b.mimic([(edge_s, sy - 0.07), (edge_v, sy - 0.07)], "blue")
        b.pump((edge_v + edge_s) / 2, sy - 0.07, "blue", 0.04)
    b.vessel(2.1, 2.55, 0.12, 0.1, "red")     # the pressurizer
    b.meters(0.3, 4, 1.47, seed=2)
    b.meters(0.85, 4, 1.47, seed=5)
    b.label(0.22, 1.29, LABEL2 - 0.04)
    b.controllers(1.55, 4)
    b.dial(2.65, 1.47, 2)
    # Charging and letdown, the volume control tank.
    b.recorder(3.35, i=0)
    b.recorder(3.72, i=1)
    b.meters(4.05, 7, seed=1)
    b.recorder(5.48, i=0, wide=0.36)
    b.label(3.18, 5.8, LABEL1)
    b.vessel(3.4, ROW2, 0.2, 0.26, "green")
    b.mimic([(3.5, 2.02), (5.7, 2.02)], "green")
    b.mimic([(5.7, 1.88), (3.5, 1.88)], "lblue")
    for k in range(3):
        b.pump(3.85 + k * 0.25, 2.02, "green", 0.04)
    b.vessel(4.85, 1.95, 0.3, 0.08, "dgrey")    # the regenerative heat exchanger
    for k, x in enumerate((5.3, 5.5)):
        b.valve(x, 2.02, k == 0)
        b.valve(x - 1.2, 1.88, k == 1)
    b.mimic([(5.7, 2.02), (5.7, 1.88)], "green")
    b.controllers(3.3, 5)
    b.screen(4.55, 1.5, 0.5, 0.28)
    b.dial(5.45, 1.47, 0)
    b.label(3.18, 5.8, LABEL2)
    # The desk.
    for k in range(4):
        b.switch(0.35 + k * 0.33, 0.58, "j", None, ("red", "green", "white"))
        b.button(0.3 + k * 0.33, 0.86, "white")
        b.button(0.42 + k * 0.33, 0.86, "amber" if k == 1 else "white")
    b.mimic_desk([(0.2, 0.26), (1.45, 0.26)], "red")
    for k in range(5):
        b.switch(1.8 + k * 0.22, 0.74, "knob")
    for k in range(2):
        b.switch(1.9 + k * 0.4, 0.42, "j", "red")
        b.switch(2.1 + k * 0.4, 0.42, "knob")
    for k in range(7):
        b.switch(3.3 + k * 0.3, 0.46 if k % 2 else 0.72, "j")
    b.mimic_desk([(3.15, 0.22), (5.55, 0.22)], "green")
    b.desk_block(5.72, 0.6, 0.14, 0.14, 0.035, [strip("meter", 1), sw("dgrey"), sw("dgrey"), sw("dgrey"), sw("dgrey"), None])
    return b


def secondary_board(seed=37):
    """Steam generators, feedwater and main steam: for each steam generator four meters (narrow and wide range
    level, steam and feed flow) and a recorder, its mimic (steam out in yellow through the isolation valve and the
    atmospheric relief, feedwater in in green through the regulating valve and its bypass), its controllers and a
    meter; on the desk each generator's isolation, bypass and auxiliary feed switches and its push buttons."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=3, seed=seed, alarms=1)
    for sg in range(4):
        x = 0.1 + sg * 1.45
        b.label(x + 0.05, x + 1.38, y_ann - 0.04, 0.035)
        b.meters(x + 0.12, 4, seed=sg)
        b.recorder(x + 0.82, i=sg % 2, wide=0.32)
        b.meters(x + 1.13, 2, seed=sg + 2)
        b.label(x + 0.05, x + 1.38, LABEL1)
        b.vessel(x + 0.18, ROW2, 0.18, 0.3, "lblue")
        b.mimic([(x + 0.27, 2.04), (x + 1.4, 2.04)], "yellow")
        b.valve(x + 0.6, 2.04, True)
        b.valve(x + 0.92, 2.04, False)
        b.mimic([(x + 1.4, 1.86), (x + 0.27, 1.86)], "green")
        b.valve(x + 0.72, 1.86, True)
        b.valve(x + 1.04, 1.86, sg == 2)
        b.controllers(x + 0.22, 2)
        b.dial(x + 0.78, ROW3, sg % 3, 0.2)
        b.face_switch(x + 1.15, 1.44)
        b.label(x + 0.05, x + 1.38, LABEL2 - 0.04)
        for k, (kind, collar) in enumerate((("j", "yellow"), ("j", None), ("j", None), ("knob", None))):
            b.switch(x + 0.2 + k * 0.3, 0.5, kind, collar)
            b.button(x + 0.2 + k * 0.3, 0.82, "amber" if k == 3 else "white")
        b.mimic_desk([(x + 0.08, 0.22), (x + 1.32, 0.22)], "green")
    return b


def turbine_board(seed=41):
    """Turbine and generator: the generator's megawatts, megavars, volts and amps on big square meters and the
    synchroscope, recorders and meters; the turbine as a mimic (the stop and control valves on the main steam, the
    high-pressure turbine, the three low-pressure turbines, the generator on the shaft) and its status lamps; the
    speed and load screen and controllers; on the desk the electro-hydraulic control's lit push button matrix, the
    generator and field breaker switches, the turbine trip under its guard."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=3, seed=seed, alarms=2)
    b.label(0.2, 5.8, y_ann - 0.04, 0.035)
    for k in range(4):
        b.dial(0.32 + k * 0.33, ROW1, k % 3, 0.28)
    b.dial(1.78, ROW1, 1, 0.34)
    b.recorder(2.3, i=0, wide=0.32)
    b.recorder(2.68, i=1, wide=0.32)
    b.meters(3.05, 8, seed=0)
    b.recorder(4.38, i=0, wide=0.36)
    b.meters(4.75, 3, seed=2)
    b.dial(5.62, ROW1, 0)
    b.label(0.2, 2.85, LABEL1 - 0.02)
    b.label(3.0, 5.8, LABEL1)
    # The turbine.
    b.mimic([(0.18, 2.04), (0.8, 2.04)], "yellow")
    for k in range(4):
        b.valve(0.27 + k * 0.15, 2.04, k != 1)
    b.vessel(0.95, 1.95, 0.3, 0.2, "dgrey")
    for k in range(3):
        b.vessel(1.45 + k * 0.45, 1.95, 0.34, 0.24, "dgrey")
    b.vessel(2.85, 1.95, 0.42, 0.22, "purple")
    b.mimic([(1.1, 1.95), (1.28, 1.95)], "purple")
    b.mimic([(2.39, 1.95), (2.64, 1.95)], "purple")
    b.lamp_grid(3.35, 1.83, 9, 3, 0.07, 0.08, ("white", "white", "green", "off"), seed=seed)
    b.screen(1.0, 1.47, 0.62, 0.28, "display")
    b.controllers(1.55, 4)
    b.meters(2.35, 6, 1.47, seed=3)
    b.screen(3.75, 1.47, 0.5, 0.26, "screen")
    b.dial(4.35, 1.47, 2, 0.2)
    b.dial(4.65, 1.47, 0, 0.2)
    for k in range(3):
        b.face_switch(5.05 + k * 0.28, 1.44)
    b.label(0.2, 5.8, LABEL2 - 0.05)
    # The desk.
    for i in range(8):
        for j in range(3):
            b.button(0.3 + i * 0.11, 0.3 + j * 0.22, "white" if (i + j) % 3 else ("amber" if i % 2 else "green"))
    for k in range(4):
        b.switch(1.45 + k * 0.3, 0.55, "j", "red" if k == 0 else None)
    b.guarded_switch(2.75, 0.5)
    for k in range(6):
        b.switch(3.3 + k * 0.38, 0.46 if k % 2 else 0.72, "knob" if k % 3 == 1 else "j")
    b.mimic_desk([(3.15, 0.22), (5.5, 0.22)], "purple")
    return b


def electrical_board(seed=53):
    """Offsite power and the station busses: the switchyard bus over the main and auxiliary transformers, the two
    safety busses below with their feeders and breakers (the lamps show each breaker open or shut) and the diesel
    generators at their ends; voltmeters, ammeters and watt meters along the bottom with the synchronizing meter;
    on the desk the breaker switches set into a mimic of the busses, the diesels' start buttons and governor knobs."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=2, seed=seed, alarms=1)
    b.label(0.2, 5.8, y_ann - 0.04, 0.035)
    b.mimic([(0.2, 2.5), (5.8, 2.5)], "black", 0.04)
    for x in (1.0, 4.9):
        b.mimic([(x, 2.5), (x, 2.3)], "black")
        b.valve(x, 2.42, True)
        b.vessel(x, 2.24, 0.24, 0.12, "dgrey")
        b.mimic([(x, 2.18), (x, 2.0)], "black")
    for x0, x1, xs in ((0.2, 2.8, (0.4, 0.75, 1.35, 1.7, 2.05, 2.4)), (3.2, 5.8, (3.6, 3.95, 4.3, 4.6, 5.25, 5.6))):
        b.mimic([(x0, 1.96), (x1, 1.96)], "black", 0.04)
        for x in xs:
            b.mimic([(x, 1.96), (x, 1.68)], "black")
            b.valve(x, 1.84, (int(x * 10) % 3) != 1)
    for x, colour in ((2.65, "orange"), (3.35, "blue")):
        b.mimic([(x, 1.96), (x, 1.75)], "black")
        b.valve(x, 1.88, False)
        b.pump(x, 1.68, colour, 0.055)
    b.meters(0.25, 8, 1.42, seed=0)
    b.dial(1.35, 1.42, 0, 0.2)
    b.dial(1.65, 1.42, 2, 0.2)
    b.dial(3.0, 1.42, 1, 0.26)        # the synchronizing meter
    b.meters(3.4, 8, 1.42, seed=4)
    b.dial(4.5, 1.42, 0, 0.2)
    b.dial(4.8, 1.42, 2, 0.2)
    b.lamp_grid(5.2, 1.36, 6, 2, 0.1, 0.12, ("white", "amber", "white"), seed=seed)
    b.label(0.2, 2.8, 1.62)
    # The desk.
    for x0, x1, xs in ((0.2, 2.8, [0.35 + k * 0.36 for k in range(7)]), (3.2, 5.8, [3.35 + k * 0.36 for k in range(7)])):
        b.mimic_desk([(x0, 0.86), (x1, 0.86)], "black", 0.03)
        for x in xs:
            b.mimic_desk([(x, 0.86), (x, 0.62)], "black")
            b.switch(x, 0.47, "j")
    for k, colour in enumerate(("orange", "blue")):
        x = 0.4 + k * 3.0
        for j in range(4):
            b.button(x + j * 0.13, 0.2, ("green", "red", "white", "amber")[j])
        b.switch(x + 0.7, 0.2, "knob", colour, None, None)
        b.switch(x + 0.95, 0.2, "knob", colour, None, None)
    return b


def radiation_board(seed=67):
    """Radiation monitoring and ventilation: the radiation monitors' drawers on the left (a reading, its alarm and
    status lamps and its test and reset buttons each), with meters and a recorder below; on the right the control
    room and containment ventilation as a mimic of fans, dampers and filter units, the fire detection lamps, a screen
    and meters; on the desk the monitors' reset buttons and the fans' and dampers' switches."""
    b = Board(6, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 5.9, rows=2, seed=seed, alarms=2)
    b.label(0.2, 2.8, y_ann - 0.04, 0.035, "label_yellow")
    b.label(3.1, 5.8, y_ann - 0.04, 0.035)
    for i in range(4):
        for j, y in enumerate((2.42, 2.08, 1.74)):
            x = 0.45 + i * 0.68
            b.face_box(x, y, 0.62, 0.3, 0.05, sw("dgrey"), sw("dgrey"))
            b.screen(x - 0.1, y + 0.02, 0.26, 0.1, "screen", 0.065)
            b.face_lamp(x + 0.15, y + 0.06, "amber" if (i * 3 + j) % 5 == 2 else "off", 0.03, FACE - 0.05)
            b.face_lamp(x + 0.21, y + 0.06, "green", 0.03, FACE - 0.05)
            for k in range(2):
                b.face_box(x + 0.15 + k * 0.06, y - 0.07, 0.04, 0.03, 0.06, sw("white"), sw("mgrey"))
            b.face_box(x - 0.1, y - 0.09, 0.3, 0.025, 0.054, sw("label_white"), sw("label_white"))
    b.meters(0.25, 10, 1.38, step=0.115, seed=1)
    b.recorder(1.75 + 0.7, 1.4, 1, 0.34)
    b.mimic([(3.2, 2.38), (5.8, 2.38)], "lblue")
    b.mimic([(5.8, 2.02), (3.2, 2.02)], "lblue")
    b.mimic([(3.2, 2.38), (3.2, 2.02)], "lblue")
    for k in range(3):
        b.pump(3.55 + k * 0.8, 2.38, "lblue", 0.06)
        b.valve(3.9 + k * 0.8, 2.38, k != 1)
        b.vessel(3.6 + k * 0.8, 2.02, 0.22, 0.12, "dgrey")
        b.valve(3.95 + k * 0.8, 2.02, True)
    b.label(3.15, 5.8, LABEL2 + 0.06)
    b.lamp_grid(3.3, 1.38, 6, 3, 0.09, 0.09, ("white", "white", "red", "off"), seed=seed)
    b.screen(4.55, 1.5, 0.62, 0.3, "display")
    b.meters(5.15, 6, 1.47, step=0.115, seed=2)
    for k in range(10):
        b.button(0.3 + k * 0.24, 0.34, "white")
        b.button(0.3 + k * 0.24, 0.62, "amber" if k % 4 == 1 else "off", k % 4 == 1)
    for k in range(7):
        b.switch(3.3 + k * 0.36, 0.46 if k % 2 else 0.72, "j" if k % 2 else "knob")
    b.mimic_desk([(3.15, 0.22), (5.6, 0.22)], "lblue")
    return b


def reactor_console(seed=7):
    """The reactor control console (the working one, three wide, in line with the boards): the digital rod position
    indicator over the middle with the nuclear instruments' recorders either side, the three live screens and the
    status lamps (drawn by the renderer in the openings left for them, client/NuclearRenderer.java consoleLamps); on
    the desk the rod bank selector, the rods' in-hold-out lever, the rod speed and startup rate meters and the two
    reactor trip switches under their guards."""
    b = Board(3, seed)
    b.profile(True)
    y_ann = b.annunciators(0.1, 2.9, rows=2, seed=seed, alarms=0)
    b.label(0.25, 2.75, y_ann - 0.04, 0.035, "label_red")
    b.drpi(1.5, 2.35, 1.1, 0.42)
    b.recorder(0.45, 2.36, 0)
    b.recorder(2.55, 2.36, 1)
    for x0, y0, x1, y1 in SCREENS:
        b.bezel(x0, y0, x1, y1, FACE - SCREEN_Z, 0.025)
    # The lamp strip (the renderer's eight lamps sit in its openings).
    x0, y0, x1, y1 = LAMP_STRIP
    b.box(x0, y0, LAMP_Z, x1, y1, FACE, [None, None, sw("black"), sw("black"), sw("black"), sw("black")])
    step = (x1 - x0) / 8
    for i in range(8):
        lx0 = x0 + i * step
        b.front_quad(lx0, y0, lx0 + 0.04, y1, LAMP_Z, sw("black"), None)
        b.front_quad(lx0 + step - 0.04, y0, lx0 + step, y1, LAMP_Z, sw("black"), None)
    # The desk: rod control on the left, the trip switches on the right, the nuclear instruments' meters between.
    b.switch(0.4, 0.62, "knob", None, ("white", "white", "white"))
    p, t, n = b.on_desk(0.72, 0.5, 0.0)
    b.obox(tuple(p + n * 0.05), (1, 0, 0), tuple(n), 0.012, 0.05, 0.012, [sw("mgrey")] * 5 + [None])   # the lever
    b.cyl(tuple(p + n * 0.1), tuple(p + n * 0.13), 0.022, sw("black"), sw("black"), 8)
    b.desk_block(0.72, 0.5 - 0.11, 0.12, 0.04, 0.006, [sw("label")] * 5 + [None])
    for k in range(4):
        b.desk_block(1.08 + k * 0.13, 0.72, 0.1, 0.22, 0.06, [strip("meter", k)] + [sw("black")] * 4 + [None])
    for k in range(3):
        b.button(1.15 + k * 0.15, 0.3, "white")
        b.switch(0.25 + k * 0.3, 0.2, "knob", None, ("white",))
    b.guarded_switch(2.12, 0.45)
    b.guarded_switch(2.56, 0.45)
    return b


def remote_shutdown_panel(seed=89):
    """The remote shutdown panel (round 24; Justin: "make it look more realistic with detail"): a vertical
    switchboard to trip the reactor and hold it shut down from outside the control room, laid out as a real one is.
    Three sections bolted together: Train A on the left (orange), Train B on the right (blue), the reactor in the
    middle. Each train under its title plate: the indication (pressurizer pressure and level, the hot and cold leg
    temperatures on edgewise meters, the steam generators' levels and the tanks' on digital meters, the source range
    flux and startup rate dials, a steam pressure recorder); the control transfer switches (key locks that take each
    component away from the control room, white lamp while the control room has it, amber while this panel has it)
    in their yellow line; the auxiliary feedwater as a mimic (the condensate storage tank, the pumps, the flow control
    valves, the steam generators) with its switches under it; the reactor coolant system's switches (charging, boric
    acid, the pressurizer's heaters, relief and block valves, residual heat removal, cooling water) along the bottom.
    Every device has its engraved nameplate; the lamps show each component's state (red running or open, green
    stopped or shut). The middle: the panel's title, the reactor's live status screen and lamps (drawn by the renderer
    in the openings left for them), the two reactor trip switches under their guards, the plant telephone and the
    sound-powered phone jacks; on the ledge the evacuation procedure lies open. Under the ledge each section's doors,
    their caution and danger placards and the panel's number plate."""
    b = Board(6, seed, face=0.3)
    b.cabinet(((0.04, 2.5, 2), (2.5, 3.5, 1), (3.5, 5.96, 2)))
    trains = (
        # x0, x1, title, indication (meters, digital (legend, reading), dials, recorder), transfer switches (legend,
        # local), the feedwater pumps (legend, running), its valves, the coolant system's switches (legend, on, grip).
        (0.1, 2.44, 2, ("PZR PRES", "PZR LVL", "RCS TH", "RCS TC"),
         (("SG1 LVL", 4), ("SG2 LVL", 5), ("BAT LVL", 9), ("AFW FLOW", 8)), ("SR FLUX", "SUR"),
         ("AFW PP A", "TDAFW PP", "CHG PP A", "PZR HT A", "PORV A", "SG1 ARV"),
         (("AFW PP A", False), ("TDAFW PP", False)), (("FCV 1", True), ("FCV 2", True), ("SG1 ARV", False), ("SG2 ARV", False)),
         (("CHG PP A", True, True), ("BA PP A", False, True), ("PZR HT A", True, True), ("PORV A", False, False),
          ("BLOCK A", True, False), ("RHR PP A", False, True), ("CCW PP A", True, True), ("SW PP A", True, True))),
        (3.56, 5.9, 3, ("PZR PRES", "PZR LVL", "RCS TH", "RCS TC"),
         (("SG3 LVL", 6), ("SG4 LVL", 7), ("BAT LVL", 15), ("CST LVL", 1)), ("SR FLUX", "SUR"),
         ("AFW PP B", "CHG PP B", "PZR HT B", "PORV B", "SG3 ARV", "SG4 ARV"),
         (("AFW PP B", False), None), (("FCV 3", True), ("FCV 4", True), ("SG3 ARV", False), ("SG4 ARV", False)),
         (("CHG PP B", False, True), ("BA PP B", False, True), ("PZR HT B", False, True), ("PORV B", False, False),
          ("BLOCK B", True, False), ("RHR PP B", False, True), ("CCW PP B", True, True), ("SW PP B", False, True))),
    )
    inst = lambda legend: plate("nameplates_inst", NAMEPLATES["nameplates_inst"].index(legend))
    for k, (x0, x1, title_row, meters, digitals, dials, transfers, pumps, valves, coolant) in enumerate(trains):
        colour = ("orange", "blue")[k]
        mid = (x0 + x1) / 2
        b.nameplate(mid, 2.865, title(title_row), 0.68, 0.085, 0.008)
        # Indication.
        b.zone([(x0, 2.27), (x1, 2.27), (x1, 2.78), (x0, 2.78), (x0, 2.27)])
        b.nameplate(mid, 2.78, title(7), 0.46, 0.055, 0.016)
        for i, legend in enumerate(meters):
            x = x0 + 0.14 + i * 0.13
            b.meter(x, 2.53, i + 2 * k, 0.28)
            b.nameplate(x, 2.355, inst(legend))
        for i, (legend, reading) in enumerate(digitals):
            x, y = x0 + 0.78 + (i % 2) * 0.2, 2.6 - (i // 2) * 0.16
            b.digital_indicator(x, y, reading)
            b.nameplate(x, y - 0.058, inst(legend), 0.14)
        for i, legend in enumerate(dials):
            x = x0 + 1.3 + i * 0.26
            b.dial(x, 2.53, (0, 2)[i], 0.22)
            b.nameplate(x, 2.385, inst(legend), 0.14)
        b.recorder(x0 + 2.02, 2.53, k, 0.34)
        b.nameplate(x0 + 2.02, 2.35, inst("SG PRES"), 0.14)
        # The control transfer switches.
        b.zone([(x0, 1.89), (x1, 1.89), (x1, 2.21), (x0, 2.21), (x0, 1.89)], "yellow")
        b.nameplate(mid, 2.21, title(6), 0.6, 0.055, 0.016)
        for i, legend in enumerate(transfers):
            b.key_switch(x0 + 0.24 + i * 0.37, 2.03, legend)
        # Auxiliary feedwater: the tank, the pumps, the header, the valves to the steam generators.
        # (Its line and the coolant system's below share their sides; the line between them runs across.)
        b.zone([(x0, 0.99), (x1, 0.99), (x1, 1.81), (x0, 1.81), (x0, 0.99)])
        b.zone([(x0, 1.27), (x1, 1.27)])
        b.nameplate(mid, 1.81, title(4), 0.5, 0.055, 0.016)
        b.vessel(x0 + 0.2, 1.63, 0.18, 0.24, "lblue")
        hx = x0 + 0.98
        for (yy, px, pump) in ((1.70, x0 + 0.52, pumps[0]), (1.56, x0 + 0.78, pumps[1])):
            b.mimic([(x0 + 0.29 if pump else hx, yy), (x0 + 2.03, yy)], "green")
            if pump:
                b.pump(px, yy, "green", 0.045)
                b.control_switch(px, 1.4, pump[0], pump[1])
        b.mimic([(hx, 1.70), (hx, 1.56)], "green")
        for (yy, sx), (legend, opened) in zip(((1.70, x0 + 1.32), (1.56, x0 + 1.58)), valves[:2]):
            b.valve(sx, yy, opened)
            b.control_switch(sx, 1.4, legend, opened, grip=False)
        for i, (yy, (legend, opened)) in enumerate(zip((1.70, 1.56), valves[2:])):
            b.vessel(x0 + 2.1, yy, 0.14, 0.11, "lblue")
            b.control_switch(x0 + 1.88 + i * 0.26, 1.4, legend, opened, grip=False)
        # The reactor coolant system along the bottom.
        b.nameplate(mid, 1.27, title(5), 0.56, 0.05, 0.016)
        for i, (legend, on, grip) in enumerate(coolant):
            b.control_switch(x0 + 0.16 + i * 0.29, 1.11, legend, on, grip)
        # The caution placard on the section's first door.
        b.placard(x0 + 0.42, 0.7, 0.24, 0.12, ["placards", 0.01, 0.01, 0.99, 0.49])
    # The middle: the title, the reactor's status (live), its lamps, the trip switches, the telephones.
    b.nameplate(3.0, 2.78, title(0, 2), 0.8, 0.2, 0.012)
    b.bezel(*RSP_SCREEN, b.face - RSP_SCREEN_Z, 0.025)
    for (x, y) in RSP_LAMPS:
        rim = sw("dgrey")
        h = RSP_LAMP / 2 + 0.008
        b.box(x - h, y - h, RSP_LAMP_Z, x + h, y + h, b.face, [None, None, rim, rim, rim, rim])
    b.face_guarded(2.76, 1.42, "TRIP A")
    b.face_guarded(3.24, 1.42, "TRIP B")
    b.phone(3.0, 1.12)
    b.jack_box(2.68, 1.08)
    b.jack_box(3.32, 1.08)
    b.open_binder(2.66, 3.34)
    b.closed_binder(5.4, 5.78, 0)
    b.placard(3.0, 0.7, 0.1, 0.1, ["placards", 0.01, 0.51, 0.49, 0.99])
    b.placard(4.42, 0.7, 0.12, 0.12, ["placards", 0.51, 0.51, 0.99, 0.99])
    return b


# ---------------------------------------------------------------- the 480 V load centre (round 25)

# Its breakers, in the order the game numbers them: the main (incoming) breaker, then the feeders, two to a section
# (upper, lower) from the left. The upper feeders send their power up out of the top of their section, the lower ones
# down through the floor; the transformer section takes it in (client/NuclearRenderer.java, nuclear/Switchgear.java).
LC_FEEDERS = ["MCC 1", "MCC 2", "MCC 3", "MCC 4", "INVERTER", "CHARGER", "HVAC", "LIGHTING", "SPARE", "SPARE"]
LC_FACE = 0.12          # the doors' plane (z)
LC_TOP = 2.36           # the cabinets' top, under the cap


def load_center(seed=97):
    """A 480 V load centre as it stands in a plant's switchgear room (round 25; Justin: "realistic switchgear ... less
    simple and cartoony", and it must hold charge and work): a lineup of steel cubicles bolted together on a black
    sill, eight blocks long. On the left the station service transformer's section (4160 V to 480 V, dry type), its
    bolted covers louvred top and bottom, its temperature monitor and warnings; then the main breaker's section, its
    meters above (volts, amps, the bus's charge); then five feeder sections, each with two draw-out breakers behind
    hinged doors (the breaker's front showing through: open and close buttons, the open/closed flag, the spring's
    charge flag, the charging handle and the racking slot; red and green lamps above; a nameplate naming its feeder)
    and a pair of digital ammeters on top. Along the top, the rails and trolley of the breaker lifting device; at the
    back, the incoming bus duct and the feeders' conduits. The lamps, flags and meters are the game's (live)."""
    b = Board(8, seed, face=LC_FACE)
    e, dark, grey = body(), sw("dgrey"), sw("mgrey")
    # The body (its front dark: the seams between doors), the sill, the cap.
    b.box(0.04, 0.1, LC_FACE, 7.96, LC_TOP, 1.96, [dark, e, e, e, None, None])
    b.box(0.04, 0, 0.1, 7.96, 0.1, 1.96, [sw("black"), None, sw("black"), sw("black"), None, None])
    b.box(0.02, LC_TOP, 0.08, 7.98, 2.4, 1.98, [TRIM] * 6)
    b.core = len(b.still)
    live = {"breakers": [], "meters": []}

    def door(x0, x1, y0, y1):
        b.face_box((x0 + x1) / 2, (y0 + y1) / 2, x1 - x0, y1 - y0, 0.012, e, grey)

    def bolts(x0, x1, y0, y1):
        for bx in (x0 + 0.04, x1 - 0.04):
            for by in (y0 + 0.04, y1 - 0.04):
                b.face_box(bx, by, 0.022, 0.022, 0.018, dark, dark)

    def louvres(cx, y0, n, w):
        for k in range(n):
            b.face_box(cx, y0 + k * 0.055, w, 0.022, 0.016, dark, dark)

    def breaker(x0, x1, y0, y1, legend, big=False):
        """A breaker's door: hinges, latch, nameplate, the lamps' rims, and the breaker's front through its cut-out."""
        door(x0, x1, y0, y1)
        cx = (x0 + x1) / 2
        for hy in (y0 + 0.12, y1 - 0.12):
            b.face_box(x0 + 0.03, hy, 0.02, 0.07, 0.02, dark, dark)
        b.face_box(x1 - 0.07, (y0 + y1) / 2, 0.022, 0.13, 0.04, grey, dark)
        b.nameplate(cx, y1 - 0.06, plate("nameplates3", NAMEPLATES["nameplates3"].index(legend)), 0.3, 0.03, 0.02)
        lamps = [(cx - 0.045, y1 - 0.135), (cx + 0.045, y1 - 0.135)]
        for lx, ly in lamps:
            b.lamp_rim(lx, ly, 0.03, LC_FACE - 0.012)
        ew, eh = (0.5, 0.42) if big else (0.42, 0.34)
        ex, ey = cx - 0.05, (y0 + y1) / 2 - (0.02 if big else 0.06)
        ez = LC_FACE - 0.05
        b.face_box(ex, ey, ew, eh, 0.05, dark, sw("black"))
        # The open (green) and close (red) buttons, the charged flag, the racking slot, the charging handle.
        bx = ex - ew / 2 + 0.06
        b.cyl((bx, ey + 0.08, ez), (bx, ey + 0.08, ez - 0.016), 0.024, sw("black"), sw("green"), 10)
        b.cyl((bx, ey - 0.02, ez), (bx, ey - 0.02, ez - 0.016), 0.024, sw("black"), sw("red"), 10)
        b.face_box(ex + 0.02, ey - 0.04, 0.09, 0.035, 0.054, sw("yellow"), sw("black"))
        b.face_box(ex - 0.02, ey - eh / 2 + 0.05, 0.06, 0.03, 0.054, sw("black"), sw("black"))
        b.face_box(ex + ew / 2 - 0.06, ey - 0.02, 0.035, 0.2, 0.1, sw("black"), sw("black"))
        # Ventilation slots low on the door.
        for k in range(3):
            b.face_box(cx - 0.05, y0 + 0.07 + k * 0.045, ew * 0.8, 0.018, 0.018, dark, dark)
        # The open/closed flag's window: its frame here, the flag the game's.
        fx0, fy0, fx1, fy1 = ex - 0.04, ey + 0.05, ex + 0.07, ey + 0.11
        b.face_box((fx0 + fx1) / 2, (fy0 + fy1) / 2, fx1 - fx0 + 0.02, fy1 - fy0 + 0.02, 0.052, sw("black"), sw("black"))
        live["breakers"].append({"lamps": [list(_r(*l)) for l in lamps], "lamp": 0.03, "lamp_z": _r(LC_FACE - 0.024)[0],
                                 "flag": list(_r(fx0, fy0, fx1, fy1)), "flag_z": _r(LC_FACE - 0.056)[0]})

    def meter_live(x, y, kind):
        b.face_box(x, y, 0.17, 0.07, 0.035, sw("black"), dark)
        live["meters"].append({"kind": kind, "box": list(_r(x - 0.07, y - 0.022, x + 0.07, y + 0.022)), "z": _r(LC_FACE - 0.039)[0]})

    # The transformer's section.
    door(0.08, 1.92, 1.24, 2.33)
    door(0.08, 1.92, 0.14, 1.2)
    bolts(0.08, 1.92, 1.24, 2.33)
    bolts(0.08, 1.92, 0.14, 1.2)
    louvres(1.0, 1.82, 8, 1.4)
    louvres(1.0, 0.28, 9, 1.4)
    b.nameplate(0.62, 2.25, plate("nameplates3", 11), 0.4, 0.045, 0.02)
    b.nameplate(1.38, 2.25, plate("nameplates3", 1), 0.3, 0.035, 0.02)
    b.face_box(1.0, 1.58, 0.42, 0.24, 0.03, sw("mgrey"), dark)
    b.face_box(0.92, 1.62, 0.17, 0.07, 0.05, sw("black"), dark)
    b.front_quad(0.85, 1.598, 0.99, 1.642, LC_FACE - 0.054, plate("digital", 14), b.lit)
    for k, colour in enumerate(("green", "off_amber", "off_red")):
        b.lamp_rim(1.1 + k * 0.05 - 0.05, 1.51, 0.026, LC_FACE - 0.03)
        h = 0.026 / 2 + 0.005
        lx = 1.1 + k * 0.05 - 0.05
        b.front_quad(lx - h, 1.51 - h, lx + h, 1.51 + h, LC_FACE - 0.042, LENS[colour], None if colour.startswith("off") else b.lit)
    b.face_box(1.12, 1.64, 0.1, 0.035, 0.05, sw("black"), dark)
    b.nameplate(0.62, 1.4, plate("nameplates3", 15), 0.24, 0.035, 0.02)
    b.nameplate(1.38, 1.4, plate("nameplates3", 14), 0.24, 0.035, 0.02)
    # The main breaker's section: its meters, then the breaker.
    door(2.0, 2.96, 1.72, 2.33)
    meter_live(2.48, 2.2, "charge")
    b.nameplate(2.48, 2.13, plate("nameplates3", 12), 0.22, 0.03, 0.02)
    meter_live(2.48, 1.98, "volts")
    b.nameplate(2.48, 1.91, plate("nameplates3", 13), 0.22, 0.03, 0.02)
    b.dial(2.17, 2.06, 0, 0.2)
    b.dial(2.79, 2.06, 2, 0.2)
    # The voltmeter's phase selector switch.
    b.face_box(2.48, 1.8, 0.1, 0.1, 0.02, dark, sw("black"))
    b.cyl((2.48, 1.8, LC_FACE - 0.02), (2.48, 1.8, LC_FACE - 0.05), 0.026, sw("black"), sw("dgrey"), 8)
    breaker(2.0, 2.96, 0.14, 1.68, "MAIN BKR", big=True)
    # The feeders' sections.
    w = (7.96 - 2.98) / 5
    for k in range(5):
        x0, x1 = 2.98 + k * w + 0.02, 2.98 + (k + 1) * w - 0.02
        cx = (x0 + x1) / 2
        door(x0, x1, 1.96, 2.33)
        for dx, legend in ((-0.21, LC_FEEDERS[2 * k]), (0.21, LC_FEEDERS[2 * k + 1])):
            meter_live(cx + dx, 2.18, "amps")
            b.nameplate(cx + dx, 2.1, plate("nameplates3", NAMEPLATES["nameplates3"].index(legend)), 0.17, 0.026, 0.02)
        breaker(x0, x1, 1.16, 1.93, LC_FEEDERS[2 * k])
        breaker(x0, x1, 0.14, 1.13, LC_FEEDERS[2 * k + 1])
    # The breaker lifting device: its rails along the top, the trolley parked at the right end, its hoist out over the
    # front with the chain and hook.
    for z0 in (0.22, 0.72):
        b.box(0.12, 2.4, z0, 7.88, 2.45, z0 + 0.05, [sw("dgrey")] * 5 + [None])
    yellow = sw("yellow")
    b.box(6.55, 2.47, 0.16, 7.3, 2.6, 0.84, [yellow] * 6)
    for wx in (6.62, 7.18):
        for wz in (0.245, 0.745):
            b.box(wx - 0.035, 2.45, wz - 0.04, wx + 0.035, 2.47, wz + 0.04, [sw("black")] * 5 + [None])
    b.box(6.85, 2.5, -0.3, 7.0, 2.58, 0.16, [yellow, None, yellow, yellow, yellow, yellow])
    b.box(6.9, 2.38, -0.27, 6.95, 2.5, -0.22, [sw("black")] * 6)
    b.box(6.905, 2.08, -0.255, 6.945, 2.38, -0.235, [sw("mgrey")] * 6)
    b.box(6.89, 2.02, -0.26, 6.96, 2.08, -0.23, [sw("dgrey")] * 6)
    # At the back: the incoming bus duct over the transformer, the feeders' conduits up out of the top.
    b.box(0.3, 2.4, 1.1, 1.7, 2.92, 1.88, [dark] * 5 + [None])
    b.box(0.26, 2.92, 1.06, 1.74, 2.96, 1.92, [TRIM] * 6)
    for k in range(5):
        cx = 2.98 + (k + 0.5) * w
        for dx in (-0.12, 0.0, 0.12):
            b.cyl((cx + dx, 2.4, 1.5), (cx + dx, 2.99, 1.5), 0.04, sw("mgrey"), sw("dgrey"), 8)
    out = b
    out.live = live
    return out

SECTIONS = {"esf_control_board": esf_board, "rcs_control_board": rcs_board, "secondary_control_board": secondary_board,
            "turbine_control_board": turbine_board, "electrical_control_board": electrical_board,
            "radiation_control_board": radiation_board, CONSOLE: reactor_console,
            "remote_shutdown_panel": remote_shutdown_panel, "switchgear_lineup": load_center}


def all_ops():
    return {name: make().ops() for name, make in SECTIONS.items()}


def write(path):
    """The boards for the client (assets/jugcraft/control_boards.json.gz), gzipped (about a megabyte of JSON)."""
    import gzip
    path.parent.mkdir(parents=True, exist_ok=True)
    data = json.dumps(all_ops(), separators=(",", ":")).encode()
    with gzip.GzipFile(path, "wb", mtime=0) as f:
        f.write(data)


# ---------------------------------------------------------------- checking (tools/check_mod_data.py)

def _faces(op):
    """The quads an operation draws (corners top-left, bottom-left, bottom-right, top-right), as the client draws
    them; a flashing window counts once, at its lit face."""
    kind = op[0]
    out = []

    def box(corner, faces):
        if isinstance(faces, str) or (isinstance(faces, list) and len(faces) == 5 and isinstance(faces[1], (int, float))):
            faces = [faces] * 6
        P = corner
        spec = [(P(0, 1, 0), P(0, 0, 0), P(1, 0, 0), P(1, 1, 0)), (P(1, 1, 1), P(1, 0, 1), P(0, 0, 1), P(0, 1, 1)),
                (P(0, 1, 1), P(0, 0, 1), P(0, 0, 0), P(0, 1, 0)), (P(1, 1, 0), P(1, 0, 0), P(1, 0, 1), P(1, 1, 1)),
                (P(0, 1, 1), P(0, 1, 0), P(1, 1, 0), P(1, 1, 1)), (P(0, 0, 0), P(0, 0, 1), P(1, 0, 1), P(1, 0, 0))]
        for q, mat in zip(spec, faces):
            if mat is not None:
                out.append(tuple(np.array(p, float) for p in q))
    if kind == "b":
        x0, y0, z0, x1, y1, z1 = op[1:7]
        box(lambda a, b, c: (x1 if a else x0, y1 if b else y0, z1 if c else z0), op[7])
    elif kind == "o":
        c, u, v = np.array(op[1:4]), np.array(op[4:7]), np.array(op[7:10])
        hu, hv, hw = op[10:13]
        w = np.cross(u, v)
        box(lambda a, b, d: c + u * hu * (2 * a - 1) + v * hv * (2 * b - 1) + w * hw * (2 * d - 1), op[13])
    elif kind == "q":
        out.append(tuple(np.array(op[i:i + 3], float) for i in (1, 4, 7, 10)))
    return out


def overlaps(ops):
    """Faces of a board that lie in one plane, face the same way and overlap: they would flicker in game (Justin:
    never any z-fighting). Cylinders (knobs, pumps) are left out: their caps stand clear of everything by design. A
    board drawn by state is checked as it is drawn in each state."""
    if ops.get("states"):
        bad = []
        for name, s in ops["states"].items():
            for pair in _overlaps({"still": ops["still"] + s["still"], "lit": ops["lit"] + s["lit"], "blink": ops["blink"]}):
                if pair not in bad:
                    bad.append(pair)
        return bad
    return _overlaps(ops)


def _overlaps(ops):
    groups = {}
    for lst in ("still", "lit", "blink"):
        for i, op in enumerate(ops[lst]):
            for q in _faces(op):
                tl, bl, br, tr = q
                n = np.cross(tr - tl, bl - tl)
                ln = np.linalg.norm(n)
                if ln < 1e-12:
                    continue
                n = n / ln
                key = (round(n[0], 3), round(n[1], 3), round(n[2], 3), round(float(n @ tl), 3))
                groups.setdefault(key, []).append((q, (lst, i)))
    bad = []
    for key, fs in groups.items():
        if len(fs) < 2:
            continue
        n = np.array(key[:3])
        a = np.cross(n, [0, 1, 0] if abs(n[1]) < 0.9 else [1, 0, 0])
        a /= np.linalg.norm(a)
        b = np.cross(n, a)
        polys = [[(float(p @ a), float(p @ b)) for p in q] for q, _ in fs]
        rects = [(min(x for x, _ in pg), min(y for _, y in pg), max(x for x, _ in pg), max(y for _, y in pg)) for pg in polys]
        for i in range(len(fs)):
            for j in range(i + 1, len(fs)):
                if fs[i][1] == fs[j][1]:
                    continue
                r1, r2 = rects[i], rects[j]
                if min(r1[2], r2[2]) - max(r1[0], r2[0]) <= 1e-4 or min(r1[3], r2[3]) - max(r1[1], r2[1]) <= 1e-4:
                    continue
                if _area(_clip(polys[i], polys[j])) > 1e-6:
                    bad.append((fs[i][1], fs[j][1]))
    return bad


def _area(pg):
    return abs(sum(pg[i][0] * pg[(i + 1) % len(pg)][1] - pg[(i + 1) % len(pg)][0] * pg[i][1] for i in range(len(pg)))) / 2 if len(pg) > 2 else 0


def _clip(subject, clip):
    """Sutherland-Hodgman: the part of the quad subject inside the convex quad clip (either winding)."""
    sign = 1 if sum(clip[i][0] * clip[(i + 1) % 4][1] - clip[(i + 1) % 4][0] * clip[i][1] for i in range(4)) > 0 else -1
    out = subject
    for i in range(len(clip)):
        a, b = clip[i], clip[(i + 1) % len(clip)]
        inside = lambda p: sign * ((b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])) >= -1e-12
        inp, out = out, []
        for k in range(len(inp)):
            p, q = inp[k], inp[(k + 1) % len(inp)]
            if inside(q):
                if not inside(p):
                    out.append(_meet(p, q, a, b))
                out.append(q)
            elif inside(p):
                out.append(_meet(p, q, a, b))
        if not out:
            return []
    return out


def _meet(p, q, a, b):
    x1, y1, x2, y2, x3, y3, x4, y4 = *p, *q, *a, *b
    den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if abs(den) < 1e-15:
        return q
    t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
    return (x1 + t * (x2 - x1), y1 + t * (y2 - y1))


# ---------------------------------------------------------------- the new sheet materials (128 px, stylized to 64)

def _img(colour, size=M.CELL):
    return Image.new("RGBA", (size, size), colour + (255,))


def lamp_white(seed):
    return M.lamp((246, 240, 222), seed)


def lamp_amber(seed):
    return M.lamp((252, 168, 36), seed)


def annunciator(seed, size=M.CELL):
    """Four annunciator windows (quarters): two normal (frosted white, engraved black legend), one in alarm amber,
    one in alarm red."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    h = size // 2
    for q, (bg, ink) in enumerate((((222, 222, 214), (40, 40, 44)), ((214, 216, 210), (40, 40, 44)),
                                   ((252, 186, 60), (70, 40, 10)), ((236, 70, 52), (60, 10, 8)))):
        x0, y0 = (q % 2) * h, (q // 2) * h
        d.rectangle([x0, y0, x0 + h - 1, y0 + h - 1], fill=bg + ((254,) if q >= 2 else (255,)))
        for line in range(3):
            y = y0 + 14 + line * 14
            w = r.randint(20, h - 18)
            d.rectangle([x0 + (h - w) // 2, y, x0 + (h + w) // 2, y + 5], fill=ink + ((254,) if q >= 2 else (255,)))
    return img


def meter(seed, size=M.CELL):
    """Four edgewise meter scales (vertical strips): white scale, black ticks, a coloured band and the red pointer at
    different readings."""
    img = Image.new("RGBA", (size, size), (12, 12, 14, 255))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    w = size // 4
    for i in range(4):
        x0 = i * w
        d.rectangle([x0 + 3, 4, x0 + w - 4, size - 5], fill=(236, 236, 228, 255))
        band = r.choice([(40, 150, 70), (230, 160, 40), (200, 50, 40)])
        by = r.randint(10, size - 50)
        d.rectangle([x0 + w - 10, by, x0 + w - 6, by + 30], fill=band + (255,))
        for t in range(10, size - 8, 9):
            d.line([(x0 + 5, t), (x0 + (14 if (t // 9) % 2 else 10), t)], fill=(20, 20, 22, 255), width=2)
        py = r.randint(16, size - 20)
        d.rectangle([x0 + 4, py, x0 + w - 5, py + 3], fill=(210, 30, 28, 255))
    return img


def dial(seed, size=M.CELL):
    """Four square-cased round meters (quarters): megawatts, megavars, volts, and the synchroscope (black face)."""
    img = Image.new("RGBA", (size, size), (20, 20, 22, 255))
    d = ImageDraw.Draw(img)
    h = size // 2
    r = random.Random(seed)
    for q in range(4):
        x0, y0 = (q % 2) * h, (q // 2) * h
        cx, cy = x0 + h // 2, y0 + h // 2
        face, ink = ((236, 236, 228), (20, 20, 22)) if q != 1 else ((24, 26, 30), (230, 230, 220))
        d.rectangle([x0 + 2, y0 + 2, x0 + h - 3, y0 + h - 3], fill=(40, 42, 46, 255))
        d.ellipse([x0 + 6, y0 + 6, x0 + h - 7, y0 + h - 7], fill=face + (255,))
        for k in range(13):
            a = math.radians(-225 + k * 22.5) if q != 1 else math.radians(k * 30)
            r0, r1 = (h // 2 - 12, h // 2 - 8) if q != 1 else (h // 2 - 14, h // 2 - 8)
            d.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r1 * math.cos(a), cy + r1 * math.sin(a))], fill=ink + (255,), width=2)
        if q != 1:
            d.arc([x0 + 14, y0 + 14, x0 + h - 15, y0 + h - 15], -225 + 200, -225 + 270, fill=(200, 40, 34, 255), width=3)
        a = math.radians(r.randint(-200, -20)) if q != 1 else math.radians(-90)
        d.line([(cx, cy), (cx + (h // 2 - 12) * math.cos(a), cy + (h // 2 - 12) * math.sin(a))], fill=(210, 30, 28, 255) if q != 1 else (240, 240, 230, 255), width=3)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(30, 30, 32, 255))
    return img


def chart(seed, size=M.CELL):
    """Two strip charts side by side: pale green ruled paper with two pens' traces each."""
    img = Image.new("RGBA", (size, size), (226, 236, 222, 254))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    h = size // 2
    for half in range(2):
        x0 = half * h
        for x in range(x0 + 4, x0 + h, 8):
            d.line([(x, 0), (x, size)], fill=(150, 190, 160, 254), width=1)
        for y in range(0, size, 10):
            d.line([(x0, y), (x0 + h, y)], fill=(170, 200, 176, 254), width=1)
        for pen, colour in enumerate(((200, 40, 40), (40, 60, 180)) if half == 0 else ((30, 130, 50), (30, 30, 34))):
            x = x0 + r.randint(12, h - 12)
            pts = []
            for y in range(0, size + 1, 4):
                x = max(x0 + 4, min(x0 + h - 5, x + r.randint(-3, 3)))
                pts.append((x, y))
            d.line(pts, fill=colour + (254,), width=2)
        d.rectangle([x0, 0, x0 + 1, size], fill=(60, 60, 64, 254))
    return img


def drpi(seed, size=M.CELL):
    """The digital rod position indicator: black, the core's rods in columns of green LEDs (rods withdrawn light the
    full column, inserted banks only their bottom), a few red rod-bottom lamps, white bank labels."""
    img = Image.new("RGBA", (size, size), (14, 14, 16, 254))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    cols = 16
    for i in range(cols):
        x = 6 + i * 7.4
        bank = i // 4
        height = [1.0, 1.0, 0.78, 0.15][bank]
        for k in range(14):
            y = size - 18 - k * 7
            on = k / 14 < height
            d.rectangle([x, y, x + 3, y + 3], fill=((60, 230, 100) if on else (24, 50, 30)) + (254,))
        d.rectangle([x, size - 10, x + 3, size - 7], fill=((230, 50, 40) if bank == 3 and r.random() < 0.5 else (60, 20, 18)) + (254,))
    for k in range(4):
        d.rectangle([6 + k * 29.6, 4, 6 + k * 29.6 + 22, 9], fill=(220, 220, 214, 254))
    return img


def swatches(seed, size=M.CELL):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    q = size // 4
    for name, (c, r) in SWATCH.items():
        x0, y0 = c * q, r * q
        if name in SWATCH_RGB:
            d.rectangle([x0, y0, x0 + q - 1, y0 + q - 1], fill=SWATCH_RGB[name] + (255,))
        else:
            bg, ink = {"label": ((26, 26, 28), (230, 230, 226)), "label_white": ((232, 232, 226), (24, 24, 26)),
                       "label_red": ((190, 34, 30), (246, 240, 230)), "label_yellow": ((232, 196, 40), (24, 24, 26))}[name]
            d.rectangle([x0, y0, x0 + q - 1, y0 + q - 1], fill=bg + (255,))
            d.rectangle([x0 + 5, y0 + q // 2 - 3, x0 + q - 6, y0 + q // 2 + 2], fill=ink + (255,))
    return img


# ---------------------------------------------------------------- the lettered materials (row 8: drawn at 64 px)

# A 3 x 5 pixel font: engraved legends small enough for a nameplate a hand wide to read like a real one.
FONT = {
    "A": [".#.", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."],
    "C": [".##", "#..", "#..", "#..", ".##"], "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": [".##", "#..", "#.#", "#.#", ".##"], "H": ["#.#", "#.#", "###", "#.#", "#.#"],
    "I": ["###", ".#.", ".#.", ".#.", "###"], "J": ["..#", "..#", "..#", "#.#", ".#."],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "N": ["##.", "#.#", "#.#", "#.#", "#.#"],
    "O": [".#.", "#.#", "#.#", "#.#", ".#."], "P": ["##.", "#.#", "##.", "#..", "#.."],
    "Q": [".#.", "#.#", "#.#", "##.", ".##"], "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "S": [".##", "#..", ".#.", "..#", "##."], "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "U": ["#.#", "#.#", "#.#", "#.#", "###"], "V": ["#.#", "#.#", "#.#", "#.#", ".#."],
    "W": ["#.#", "#.#", "###", "###", "#.#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["##.", "..#", ".#.", "#..", "###"], "3": ["##.", "..#", ".#.", "..#", "##."],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "##.", "..#", "##."],
    "6": [".##", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "##."],
    "-": ["...", "...", "###", "...", "..."], "/": ["..#", "..#", ".#.", "#..", "#.."],
    "&": [".#.", "#.#", ".#.", "#.#", ".##"], ".": ["...", "...", "...", "...", "#.."],
    ":": ["...", "#..", "...", "#..", "..."], " ": ["...", "...", "...", "...", "..."],
}
INK = {"white": ((236, 236, 230), (22, 22, 24)), "red": ((188, 32, 28), (250, 244, 236)), "orange": ((226, 116, 30), (20, 20, 22)),
       "blue": ((34, 80, 170), (244, 244, 240)), "yellow": ((236, 196, 36), (20, 20, 22)), "black": ((24, 24, 26), (232, 232, 226))}


NARROW = {" ": 2, ".": 2, ":": 2}     # narrow characters' advance (pixels), the rest take 4 (3 and a space)


def text(d, x, y, s, ink, scale=1):
    """Draws s in the pixel font with its top-left at (x, y)."""
    for ch in s.upper():
        glyph = "".join(FONT.get(ch, FONT[" "]))
        for i, on in enumerate(glyph):
            if on == "#":
                px, py = x + (i % 3) * scale, y + (i // 3) * scale
                d.rectangle([px, py, px + scale - 1, py + scale - 1], fill=ink)
        x += NARROW.get(ch, 4) * scale


def text_width(s, scale=1):
    return (sum(NARROW.get(ch, 4) for ch in s.upper()) - 1) * scale


def _plate(d, x0, y0, w, h, legend, colour, alpha=255):
    bg, ink = INK[colour]
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=bg + (alpha,))
    edge = tuple(max(0, c - 40) for c in bg)
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=edge + (alpha,))
    text(d, x0 + (w - text_width(legend)) // 2, y0 + (h - 5) // 2, legend, ink + (alpha,))


def _nameplate_cell(cell):
    def make(seed, size=64):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
        d = ImageDraw.Draw(img)
        for i, legend in enumerate(NAMEPLATES[cell]):
            _plate(d, (i % 2) * 32, (i // 2) * 8, 32, 8, legend, PLATE_COLOURS.get((cell, i), "white"))
        return img
    return make


def titles(seed, size=64):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for i, (legend, colour) in enumerate(TITLES):
        bg, ink = INK[colour]
        d.rectangle([0, i * 8, 63, i * 8 + 7], fill=bg + (255,))
        # One line in the middle of its row; the panel's two-line title a little apart from each other.
        text(d, (64 - text_width(legend)) // 2, i * 8 + (2 if i == 0 else 1), legend, ink + (255,))
    return img


def digital(seed, size=64):
    """Digital indicators' windows: red LED digits over the dim unlit segments (every texel glows)."""
    img = Image.new("RGBA", (size, size), (16, 6, 6, 254))
    d = ImageDraw.Draw(img)
    for i, value in enumerate(READINGS):
        x0, y0 = (i % 2) * 32, (i // 2) * 8
        d.rectangle([x0, y0, x0 + 31, y0 + 7], fill=(18, 8, 8, 254))
        # The unlit segments of every digit behind the reading, which is right-aligned like a meter's.
        x = x0 + 30 - text_width(value)
        text(d, x, y0 + 1, "".join(c if c in NARROW else "8" for c in value), (62, 16, 14, 254))
        text(d, x, y0 + 1, value, (255, 58, 40, 254))
    return img


def placards(seed, size=64):
    """A caution placard (top), the panel's number plate (bottom left) and a voltage warning (bottom right)."""
    img = Image.new("RGBA", (size, size), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 31], fill=(244, 244, 238, 255), outline=(30, 30, 32, 255))
    d.rectangle([1, 1, 62, 10], fill=(236, 196, 36, 255))
    text(d, (64 - text_width("CAUTION")) // 2, 3, "CAUTION", (20, 20, 22, 255))
    for k, line in enumerate(("TRANSFER PER", "AOP-8 ONLY")):
        text(d, (64 - text_width(line)) // 2, 13 + k * 8, line, (20, 20, 22, 255))
    d.rectangle([0, 32, 31, 63], fill=(24, 24, 26, 255))
    d.rectangle([2, 34, 29, 61], outline=(200, 200, 196, 255))
    for k, line in enumerate(("PNL", "RSP-1")):
        text(d, (32 - text_width(line)) // 2, 40 + k * 9, line, (232, 232, 226, 255))
    d.rectangle([32, 32, 63, 63], fill=(244, 244, 238, 255), outline=(30, 30, 32, 255))
    d.rectangle([33, 33, 62, 42], fill=(188, 32, 28, 255))
    text(d, 32 + (32 - text_width("DANGER")) // 2, 35, "DANGER", (250, 244, 236, 255))
    for k, line in enumerate(("125 V", "DC")):
        text(d, 32 + (32 - text_width(line)) // 2, 46 + k * 8, line, (20, 20, 22, 255))
    return img


def binders(seed, size=64):
    """Procedure binders: four spines (left half, 8 x 64 each) and an open binder's two pages (right half, one above
    the other): the procedure's number, its steps and their check boxes."""
    img = Image.new("RGBA", (size, size), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for k, colour in enumerate(((168, 30, 28), (34, 70, 150), (230, 230, 224), (28, 28, 30))):
        x0 = k * 8
        d.rectangle([x0, 0, x0 + 7, 63], fill=colour + (255,))
        d.rectangle([x0 + 1, 8, x0 + 6, 30], fill=(244, 244, 238, 255))
        for y in range(10, 29, 3):
            d.line([(x0 + 2, y), (x0 + 2 + r.randint(1, 3), y)], fill=(40, 40, 44, 255))
        for y in (4, 56):
            d.rectangle([x0 + 2, y, x0 + 5, y + 2], fill=(150, 150, 154, 255))
    for page in range(2):
        y0 = page * 32
        d.rectangle([32, y0, 63, y0 + 31], fill=(246, 246, 240, 255))
        d.line([(32, y0), (32, y0 + 31)], fill=(170, 170, 166, 255))
        if page == 0:
            text(d, 35, y0 + 2, "AOP-8", (20, 20, 22, 255))
            d.line([(35, y0 + 8), (60, y0 + 8)], fill=(20, 20, 22, 255))
        for y in range(y0 + (11 if page == 0 else 3), y0 + 30, 3):
            d.rectangle([35, y, 36, y + 1], outline=(60, 60, 64, 255))
            d.line([(38, y + 1), (38 + r.randint(10, 22), y + 1)], fill=(80, 80, 86, 255))
    return img


MAKERS = {"lamp_white": lamp_white, "lamp_amber": lamp_amber, "annunciator": annunciator, "meter": meter,
          "dial": dial, "chart": chart, "drpi": drpi, "swatches": swatches,
          "nameplates": _nameplate_cell("nameplates"), "nameplates2": _nameplate_cell("nameplates2"),
          "nameplates_inst": _nameplate_cell("nameplates_inst"), "titles": titles, "digital": digital,
          "placards": placards, "binders": binders, "nameplates3": _nameplate_cell("nameplates3")}
# Colours kept when stylizing (the swatches keep every patch's colour exactly; the lettered cells are drawn at the
# sheet's own 64 px and keep theirs too).
COLOURS = {"swatches": 64, "annunciator": 24, "meter": 24, "dial": 24, "chart": 24, "drpi": 16, "lamp_white": 12, "lamp_amber": 12,
           "nameplates": 32, "nameplates2": 32, "nameplates_inst": 32, "titles": 32, "digital": 16, "placards": 32, "binders": 32, "nameplates3": 32}
