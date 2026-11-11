"""The reactor vessel and its integrated head package (reactor building rebuild, machine 1; Justin, 19 October 2026:
"Using this re make it. Go machine by machine.").

Built like a real Westinghouse four-loop reactor vessel with an integrated head assembly (Westinghouse / Framatome
IHA; patents US6618460 and US7139359), as board operations (tools/control_boards.py) in the equipment sheet's
materials, with engraved aluminium tags and painted stencils on its own legend texture (LEGEND, 512 px a block):

- The vessel: hemispherical bottom head, shell in reflective metal insulation, the nozzle belt with four outlet (hot
  leg) and four inlet (cold leg) nozzles and their safe ends, the vessel flange.
- The refuelling cavity seal ring between the flange and the cavity floor.
- The closure head: its flange with 54 studs, nuts and spherical washers, the insulated dome, three lifting lugs.
- The head package on it, which lifts off with the head as one unit:
  - a three-segment ring beam on saddles, six support columns tied at three heights;
  - the shroud in three tiers: bottom (two access doors, hinged, handles), middle (two air inlets with louvres on
    opposite sides), upper (three cable connector panels: CRDM power and rod position indication);
  - inside, the 53 control rod drive mechanisms in the real core pattern (latch housing, operating coil stack, rod
    position indicator coil stack, seismic cap plate), seen through an open access door;
  - the integral missile shield (steel plate) and over it the upper air plenum with the three CRDM cooling fans (2 of
    3 run), their motors, guards and the local fan control box (RUN lamps, the shroud exhaust temperature dial);
  - the lift rig: three lift rods from the head's lugs up past the shroud to the lift tripod and its shackle (the
    polar crane's hook takes the shackle);
  - three seismic restraints from the seismic ring beam to the cavity walls, and the cable bridge carrying the CRDM
    and rod position cables from the connector panels to the cavity wall; both are let go before a lift.

Frame: the equipment's own (x right, y up, z away; tools/control_boards.py), 7 x 7 with the vessel's axis at
(3.5, 3.5). y 0 is the bottom of the vessel; the cavity floor (the seal ring's top) is at CAVITY_FLOOR. The restraints
and the cable bridge reach out to the cavity walls, CAVITY_HALF from the axis.

Live parts (drawn by the client from LIVE, by the reactor's state): the fan rotors turn and their RUN lamps light when
the core is hot; the shroud exhaust dial reads the head air temperature; the connector panels' POWER lamps light while
the cables are connected. Refuelling (the head on the crane) lets go of the restraints, swings the cable bridge up and
unplugs the cables; the package is drawn lifted.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import control_boards as CB
from control_boards import Board, sw, pic, quarter

C = 3.5                     # the vessel's axis (x, z)
R_SHELL = 2.45              # outside of the vessel's insulation
R_FLANGE = 2.85             # vessel and head flanges
R_STUD = 2.62               # the stud circle
N_STUDS = 54
CAVITY_FLOOR = 12.0         # the seal ring's top: the refuelling cavity's floor (the medium plant's site y 7)
FLANGE_TOP = 11.95          # the vessel flange's mating face, just under the cavity floor
HEAD_FLANGE = (FLANGE_TOP, FLANGE_TOP + 0.55)
DOME_TOP = HEAD_FLANGE[1] + 1.3
R_SHROUD = 2.3
SHROUD = (13.35, 15.1, 17.4, 19.4)    # bottom of the shroud, the joints between its tiers, its top
SHIELD = (19.4, 19.55)                # the missile shield plate
PLENUM = (19.55, 20.35)               # the upper air plenum
TRIPOD_APEX = 23.7
CAVITY_HALF = 6.5
NOZZLE_Y = 9.6
# The four loops' nozzle azimuths (radians; x = cos, z = sin; -z is north), set by where the steam generators stand: one
# at each corner of the cavity, north and south of it, SG_XZ (machine 7). Each hot leg runs straight to its steam
# generator; the cold legs come from the pumps: loops 1, 3 and 4 outboard, loop 2 between steam generators 1 and 2
# (the internals' bay is where its pump would otherwise go).
SG_XZ = [(C + 6.0, C + 12.8), (C - 6.0, C + 12.8), (C - 6.0, C - 12.8), (C + 6.0, C - 12.8)]
LOOP_AZ = [(math.atan2(z - C, x - C), math.radians(c)) for (x, z), c in zip(SG_XZ, (30.0, 90.0, 210.0, 330.0))]

LEGEND = "rv_legends"                 # this piece's legend texture (512 px a block, 2048 wide)
PX = 512


# ---------------------------------------------------------------- legends: engraved aluminium tags and painted stencils

def _font(size, bold=True):
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed%s.ttf" % ("-Bold" if bold else ""), size)


TAGS = [
    # (text, width in blocks, height in blocks)
    ("RV-1  REACTOR VESSEL CLOSURE HEAD", 1.3, 0.09),
    ("CRDM CLG FAN 1A", 0.62, 0.075),
    ("CRDM CLG FAN 1B", 0.62, 0.075),
    ("CRDM CLG FAN 1C", 0.62, 0.075),
    ("CRDM / RPI CABLE PANEL A", 0.86, 0.075),
    ("CRDM / RPI CABLE PANEL B", 0.86, 0.075),
    ("CRDM / RPI CABLE PANEL C", 0.86, 0.075),
    ("IHA ACCESS DOOR 1", 0.66, 0.07),
    ("IHA ACCESS DOOR 2", 0.66, 0.07),
    ("CRDM CLG FAN CONTROL", 0.7, 0.07),
    ("SHROUD EXH TEMP  °F", 0.62, 0.06),
    ("FAN 1A", 0.2, 0.05), ("FAN 1B", 0.2, 0.05), ("FAN 1C", 0.2, 0.05),
    ("LIFT LUG 1", 0.36, 0.06), ("LIFT LUG 2", 0.36, 0.06), ("LIFT LUG 3", 0.36, 0.06),
    ("SEISMIC RESTRAINT SR-1", 0.8, 0.06), ("SEISMIC RESTRAINT SR-2", 0.8, 0.06), ("SEISMIC RESTRAINT SR-3", 0.8, 0.06),
    ("RCS LOOP 1 HOT LEG", 0.66, 0.07), ("RCS LOOP 1 COLD LEG", 0.66, 0.07),
    ("RCS LOOP 2 HOT LEG", 0.66, 0.07), ("RCS LOOP 2 COLD LEG", 0.66, 0.07),
    ("RCS LOOP 3 HOT LEG", 0.66, 0.07), ("RCS LOOP 3 COLD LEG", 0.66, 0.07),
    ("RCS LOOP 4 HOT LEG", 0.66, 0.07), ("RCS LOOP 4 COLD LEG", 0.66, 0.07),
    # the polar crane (tools/polar_crane.py)
    ("BRIDGE DRIVE 1", 0.56, 0.08), ("BRIDGE DRIVE 2", 0.56, 0.08), ("MAIN HOIST 300 T", 0.66, 0.08),
    ("AUX HOIST 25 T", 0.56, 0.07), ("TROLLEY DRIVE", 0.46, 0.07), ("POLAR CRANE PC-1 CAB", 0.86, 0.08),
    # the refuelling machine (tools/refuelling_machine.py)
    ("REFUELING MACHINE RM-1", 0.9, 0.08), ("BRIDGE DRIVE", 0.46, 0.07), ("MAST HOIST", 0.34, 0.07),
    ("AIR HOSE REEL", 0.46, 0.06), ("CABLE REEL", 0.4, 0.06), ("LOAD CELL", 0.3, 0.05), ("RM-1 MAST ELEC CAB", 0.62, 0.07),
    ("BRIDGE  COL", 0.24, 0.045), ("TROLLEY  ROW", 0.24, 0.045), ("MAST  Z  IN", 0.24, 0.045), ("LOAD  LB", 0.24, 0.045),
    ("GRIPPER ENGAGED", 0.24, 0.04), ("GRIPPER RELEASED", 0.24, 0.04), ("OVERLOAD", 0.2, 0.04), ("SLACK CABLE", 0.22, 0.04),
    ("MAST CAMERA", 0.26, 0.04), ("HOIST", 0.14, 0.04), ("BRIDGE", 0.14, 0.04), ("TROLLEY", 0.16, 0.04),
    ("GRIPPER", 0.16, 0.04), ("E-STOP", 0.16, 0.04), ("ROTATION LOCK", 0.42, 0.06),
    ("BR DRIVE 1", 0.28, 0.055), ("BR DRIVE 2", 0.28, 0.055), ("BRIDGE CABLE REEL", 0.46, 0.06), ("RM-1 BRIDGE DRIVE PANEL", 0.56, 0.06),
    ("RM-1 POWER FEED", 0.36, 0.05), ("AIR COMPRESSOR", 0.3, 0.045), ("AIR RECEIVER", 0.26, 0.045), ("ACCESS DOOR", 0.3, 0.05),
    ("TR DRIVE", 0.2, 0.045),
    # the refuelling floor's stands, racks and the stud tensioner (tools/refuel_floor.py)
    ("RV HEAD STORAGE STAND", 0.7, 0.07), ("UPPER INTERNALS STAND", 0.7, 0.07), ("UPPER INTERNALS LIFTING RIG", 0.8, 0.07),
    ("STUD RACK A", 0.36, 0.06), ("STUD RACK B", 0.36, 0.06), ("NUTS AND WASHERS", 0.5, 0.06), ("STUD TENSIONER CONSOLE", 0.66, 0.06),
    ("TENSIONER 1", 0.26, 0.05), ("TENSIONER 2", 0.26, 0.05), ("TENSIONER 3", 0.26, 0.05), ("TENSIONER 4", 0.26, 0.05),
    ("STROKE 1", 0.14, 0.035), ("STROKE 2", 0.14, 0.035), ("STROKE 3", 0.14, 0.035), ("STROKE 4", 0.14, 0.035),
    ("PUMP", 0.12, 0.035), ("VENT", 0.12, 0.035), ("PRESS  PSI", 0.2, 0.04), ("TENSIONER STORAGE", 0.5, 0.06), ("GUIDE STUD", 0.24, 0.05),
    ("MANIFOLD", 0.22, 0.045),
    # the SRO station and fittings (tools/refuel_desk.py)
    ("REFUELING SRO", 0.5, 0.07), ("MOVE NO", 0.16, 0.04), ("STEP", 0.12, 0.04), ("MAST CAMERA RM-1", 0.36, 0.04),
    ("RM-ARM-10 DETECTOR", 0.42, 0.05), ("PC BIN", 0.2, 0.05), ("LAUNDRY", 0.22, 0.05), ("RAD WASTE", 0.24, 0.05),
    ("FRISKER", 0.2, 0.04), ("FME LOG", 0.2, 0.05), ("LIFE RING", 0.26, 0.05), ("PAGE  PARTY", 0.24, 0.04),
    ("FUEL HANDLING PROCEDURES", 0.5, 0.045), ("SOUND POWERED PHONE", 0.4, 0.04),
    # the fuel transfer system (tools/fuel_transfer.py)
    ("FUEL TRANSFER SYSTEM", 0.62, 0.07), ("REACTOR SIDE CONSOLE", 0.5, 0.05), ("UPENDER HPU", 0.4, 0.06),
    ("HYD PRESS  PSIG", 0.24, 0.04), ("TRANSFER TUBE BLIND FLANGE", 0.8, 0.07), ("CAR POS  FT", 0.24, 0.045),
    ("CAR AT REACTOR", 0.22, 0.04), ("CAR IN TRANSIT", 0.22, 0.04), ("CAR AT POOL", 0.22, 0.04), ("UPENDER UP", 0.22, 0.04),
    ("UPENDER DOWN", 0.22, 0.04), ("RM MAST UP", 0.22, 0.04), ("TUBE VALVE OPEN", 0.22, 0.04), ("HPU RUNNING", 0.22, 0.04),
    ("UPENDER", 0.14, 0.04), ("CAR", 0.1, 0.04), ("HPU START", 0.15, 0.04), ("HPU STOP", 0.15, 0.04), ("PERMISSIVE", 0.15, 0.04),
    ("PHONE  FUEL BLDG", 0.3, 0.04), ("UPENDER CYL A", 0.36, 0.05), ("UPENDER CYL B", 0.36, 0.05),
    ("UPENDER LIMIT SWITCHES", 0.6, 0.06), ("CONVEYOR CAR", 0.4, 0.06), ("FUEL CONTAINER", 0.4, 0.06),
    ("HPU MOTOR", 0.26, 0.05), ("RESERVOIR", 0.26, 0.05), ("ACCUMULATOR", 0.3, 0.05), ("FTS JB-1", 0.22, 0.05),
    # the steam generators (tools/steam_generator.py)
    ("STEAM GENERATOR SG-1", 0.74, 0.08), ("STEAM GENERATOR SG-2", 0.74, 0.08), ("STEAM GENERATOR SG-3", 0.74, 0.08),
    ("STEAM GENERATOR SG-4", 0.74, 0.08), ("SG-1 ENCLOSURE", 0.42, 0.06), ("SG-2 ENCLOSURE", 0.42, 0.06),
    ("SG-3 ENCLOSURE", 0.42, 0.06), ("SG-4 ENCLOSURE", 0.42, 0.06),
    ("PRIMARY MANWAY  HOT LEG", 0.62, 0.06), ("PRIMARY MANWAY  COLD LEG", 0.64, 0.06),
    ("SECONDARY MANWAY A", 0.5, 0.06), ("SECONDARY MANWAY B", 0.5, 0.06), ("HANDHOLE A", 0.3, 0.05), ("HANDHOLE B", 0.3, 0.05),
    ("BLOWDOWN A", 0.3, 0.05), ("BLOWDOWN B", 0.3, 0.05), ("NR LEVEL TAP", 0.32, 0.05), ("WR LEVEL TAP", 0.32, 0.05),
    ("CONDENSATE POT", 0.32, 0.045), ("FEEDWATER", 0.3, 0.06), ("MAIN STEAM", 0.32, 0.06),
    ("SNUBBER A", 0.26, 0.05), ("SNUBBER B", 0.26, 0.05), ("SNUBBER C", 0.26, 0.05), ("SNUBBER D", 0.26, 0.05),
    ("SG SUPPORT COLUMN A", 0.52, 0.06), ("SG SUPPORT COLUMN B", 0.52, 0.06), ("SG SUPPORT COLUMN C", 0.52, 0.06),
    ("SG SUPPORT COLUMN D", 0.52, 0.06), ("LOWER LATERAL SUPPORT", 0.58, 0.06), ("UPPER LATERAL SUPPORT", 0.58, 0.06),
    ("THERMAL MOVEMENT", 0.34, 0.045), ("STM PRESS  PSIG", 0.24, 0.04), ("LIFTING TRUNNION", 0.4, 0.05), ("SEISMIC KEY", 0.26, 0.05),
    ("ROOT VALVE", 0.2, 0.035),
    # the reactor coolant pumps (tools/coolant_pump.py)
    ("REACTOR COOLANT PUMP RCP-1", 0.82, 0.075), ("REACTOR COOLANT PUMP RCP-2", 0.82, 0.075),
    ("REACTOR COOLANT PUMP RCP-3", 0.82, 0.075), ("REACTOR COOLANT PUMP RCP-4", 0.82, 0.075),
    ("MOTOR TERMINAL BOX  12.5 KV", 0.62, 0.055), ("UPPER OIL COOLER", 0.4, 0.055), ("OIL LIFT PUMP", 0.34, 0.055),
    ("UPPER BRG OIL  °F", 0.28, 0.04), ("SEAL STANDPIPE", 0.36, 0.05), ("NO 1 SEAL LEAKOFF", 0.38, 0.045),
    ("NO 2 SEAL LEAKOFF", 0.38, 0.045), ("NO 3 SEAL LEAKOFF", 0.38, 0.045), ("SEAL INJECTION", 0.32, 0.045),
    ("CCW SUPPLY", 0.24, 0.045), ("CCW RETURN", 0.24, 0.045), ("VIBRATION PROBES", 0.36, 0.045),
    ("RCP SUPPORT COLUMN A", 0.56, 0.06), ("RCP SUPPORT COLUMN B", 0.56, 0.06), ("RCP SUPPORT COLUMN C", 0.56, 0.06),
    ("TIE BAR 1", 0.26, 0.05), ("TIE BAR 2", 0.26, 0.05), ("TIE BAR 3", 0.26, 0.05), ("OIL DRAIN", 0.22, 0.045),
    ("OIL LEVEL", 0.2, 0.04), ("FLYWHEEL INSPECTION", 0.4, 0.045), ("OIL LIFT PRESS  PSIG", 0.3, 0.04),
    # the pressurizer (tools/pressurizer.py)
    ("PRESSURIZER", 0.5, 0.08), ("PRESSURIZER ENCLOSURE", 0.56, 0.06), ("PSV-8010A", 0.26, 0.05), ("PSV-8010B", 0.26, 0.05),
    ("PSV-8010C", 0.26, 0.05), ("PORV PCV-455A", 0.34, 0.05), ("PORV PCV-456", 0.32, 0.05), ("BLOCK MOV-8000A", 0.38, 0.05),
    ("BLOCK MOV-8000B", 0.38, 0.05), ("SPRAY PCV-455B", 0.36, 0.05), ("SPRAY PCV-455C", 0.36, 0.05), ("PZR MANWAY", 0.3, 0.05),
    ("HEATER JUNCTION BOX", 0.44, 0.045), ("SURGE LINE", 0.28, 0.055), ("RELIEF TO PRT", 0.32, 0.055), ("PZR PRESS  PSIG", 0.26, 0.04),
    ("PZR LEVEL TAP", 0.32, 0.045), ("PZR TEMP", 0.2, 0.045), ("SPRAY LINE", 0.26, 0.05),
    # the loop piping (tools/loop_piping.py)
    ("RCS LOOP 1 CROSSOVER", 0.7, 0.07), ("RCS LOOP 2 CROSSOVER", 0.7, 0.07), ("RCS LOOP 3 CROSSOVER", 0.7, 0.07),
    ("RCS LOOP 4 CROSSOVER", 0.7, 0.07), ("PIPE WHIP RESTRAINT", 0.5, 0.055), ("HOT LEG RTD", 0.28, 0.045),
    ("COLD LEG RTD", 0.3, 0.045), ("LOOP DRAIN", 0.26, 0.05), ("ELBOW FLOW TAPS", 0.36, 0.045), ("CHARGING", 0.24, 0.05),
    ("LETDOWN", 0.22, 0.05), ("RHR SUCTION", 0.3, 0.05), ("ACCUMULATOR INJECTION", 0.52, 0.05), ("PZR SPRAY  LOOP 1", 0.4, 0.05),
    ("PZR SPRAY  LOOP 2", 0.4, 0.05), ("PZR SURGE LINE", 0.38, 0.055), ("SPRING HANGER", 0.32, 0.045),
    ("CROSSOVER SUPPORT", 0.42, 0.05),
]
STENCILS = [
    # painted cut-out letters on the shroud (no plate behind them)
    ("REACTOR HEAD", 2.2, 0.32),
    ("UNIT 1", 1.1, 0.32),
    ("POWER", 0.16, 0.035), ("RPI", 0.12, 0.035),
    ("300 / 25 TON", 3.4, 0.62), ("PC-1", 1.4, 0.62),
    ("SG-1", 1.4, 0.62), ("SG-2", 1.4, 0.62), ("SG-3", 1.4, 0.62), ("SG-4", 1.4, 0.62),
    ("RCP-1", 0.8, 0.32), ("RCP-2", 0.8, 0.32), ("RCP-3", 0.8, 0.32), ("RCP-4", 0.8, 0.32),
    ("PZR", 1.1, 0.62),
    ("REFUELING MACHINE", 3.7, 0.4), ("RM-1", 1.0, 0.4),
    ("TRANSFER TUBE", 2.4, 0.3), ("UPENDER PIT", 2.2, 0.3), ("UPPER INTERNALS", 3.0, 0.3),
]
# LED characters for live displays (the client draws a reading a character at a time), each LED_CHAR wide, in red,
# clear between them: digits, the point, "t", the minus, and the core's row letters (A to R without I, O and Q).
LED_CHARS = "0123456789.t-ABCDEFGHJKLMNPR"
LED_CHAR = (0.11, 0.2)
DIALS = [
    # dial faces with no needle (the client draws the live needle): (name, size in blocks, low, high, unit)
    ("DIAL_EXH", 0.14, 100, 200, "\u00b0F"),
    ("DIAL_HYD", 0.12, 0, 3000, "PSI"),
    ("DIAL_STEAM", 0.12, 0, 1200, "PSIG"),
    ("DIAL_OILT", 0.12, 0, 250, "°F"),
    ("DIAL_LIFT", 0.12, 0, 1000, "PSIG"),
    ("DIAL_PZR", 0.12, 0, 3000, "PSIG"),
]
# Pictures drawn for the refuelling floor (tools/refuelling_machine.py): (name, width, height in blocks).
CORE_PITCH = 0.215                     # a fuel assembly's pitch in the core (17x17: 8.43 in)
CORE_ROWS = "ABCDEFGHJKLMNPR"          # the core's rows (trolley direction); columns 1 to 15 (bridge direction)
PICTURES = [
    ("FUEL_SIDE", 0.214, 1.0),         # a metre of a 17x17 assembly's side: 17 rods, a grid strap
    ("NOZZLE_TOP", 0.214, 0.214),      # an assembly's top nozzle seen from above
    ("TV_CORE", 0.44, 0.33),           # the mast camera's picture on the console (live: the client shows the cell)
    ("SCALE_COL", 3.3, 0.14),          # the bridge's position strip on the cavity wall: core columns 1 to 15
    ("SCALE_ROW", 3.3, 0.14),          # the trolley's position strip on the bridge girder: core rows A to R
] + [("STUD_NOS_%d" % k, 2.88, 0.06) for k in range(1, 7)] + [  # the stud racks' slot numbers, nine a strip (1-54)
    # the refuelling floor's SRO station, radiation protection fittings and signs (tools/refuel_desk.py)
    ("SIGN_CONTAM", 0.5, 0.36),        # CAUTION CONTAMINATED AREA (magenta trefoil on yellow)
    ("SIGN_RAD", 0.5, 0.36),           # CAUTION RADIATION AREA
    ("SIGN_FME", 0.5, 0.36),           # FME ZONE
    ("SIGN_FRISK", 0.36, 0.5),         # the frisking steps
    ("STEP_OFF", 0.9, 0.6),            # the step-off pad's mat
    ("CORE_MAP", 1.5, 1.5),            # the SRO's core map board (live cell lamps over it)
    ("ARM_FACE", 0.4, 0.3),            # the area radiation monitor's local panel face
    ("LOG_PAGES", 0.36, 0.26),         # an open log book's pages
    ("LCD_DIGITS", 0.11 * 11, 0.2),    # grey LCD characters "0123456789." (the frisker's display)
    # the steam generators (tools/steam_generator.py)
    ("SIGN_HRA", 0.5, 0.36),           # CAUTION HIGH RADIATION AREA (the enclosures' doors)
    ("MOVE_SCALE", 0.2, 0.06),         # the thermal movement indicator's scale, 0 to 2 in
    ("SG_PLATE", 0.42, 0.3),           # the steam generator's data plate
    ("RCP_PLATE", 0.42, 0.3),          # the reactor coolant pump motor's data plate
    ("PZR_PLATE", 0.42, 0.3),          # the pressurizer's data plate
]
LCD_CHARS = "0123456789."
STUD_PITCH = 0.32


def _picture(name, w, h):
    """One of PICTURES as an image w x h pixels."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if name == "FUEL_SIDE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(46, 50, 54, 255))
        pitch = w / 17
        for k in range(17):
            x0, x1 = int(k * pitch + 1), int((k + 1) * pitch - 1)
            for i in range(x0, x1 + 1):
                t = (i - x0) / max(1, x1 - x0)
                g = int(150 + 70 * math.sin(t * math.pi))
                d.line([(i, 0), (i, h - 1)], fill=(g, g + 2, g - 4, 255))
        for gy in (0.25, 0.75):
            y0, y1 = int(h * gy - 12), int(h * gy + 12)
            d.rectangle([0, y0, w - 1, y1], fill=(168, 164, 150, 255))
            d.line([(0, y0), (w, y0)], fill=(96, 94, 88, 255), width=2)
            d.line([(0, y1), (w, y1)], fill=(96, 94, 88, 255), width=2)
            for k in range(17):
                cx = int((k + 0.5) * pitch)
                d.ellipse([cx - 2, (y0 + y1) // 2 - 3, cx + 2, (y0 + y1) // 2 + 3], fill=(120, 118, 110, 255))
        return img
    if name == "NOZZLE_TOP":
        d.rectangle([0, 0, w - 1, h - 1], fill=(170, 174, 176, 255), outline=(90, 94, 98, 255), width=3)
        d.rectangle([12, 12, w - 13, h - 13], fill=(128, 132, 136, 255))
        for i in range(5):
            for j in range(5):
                if (i, j) in ((0, 0), (4, 4), (2, 2)):
                    continue
                cx, cy = 20 + i * (w - 40) / 4, 20 + j * (h - 40) / 4
                d.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=(40, 42, 44, 255))
        for cx, cy in ((20, 20), (w - 20, h - 20)):         # the alignment (guide pin) holes, diagonal
            d.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(20, 20, 22, 255), outline=(200, 204, 206, 255), width=2)
        for cx, cy in ((w - 22, 22), (22, h - 22)):          # hold-down spring clamps
            d.rectangle([cx - 10, cy - 10, cx + 10, cy + 10], fill=(150, 152, 150, 255), outline=(70, 72, 74, 255), width=2)
        f = _font(13)
        d.text((w // 2 - 14, h // 2 - 8), "J47", font=f, fill=(50, 52, 56, 255))
        return img
    if name == "TV_CORE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(26, 34, 32, 255))
        cell = w / 6.2
        ox, oy = w / 2 - cell * 3, h / 2 - cell * 2.5
        for i in range(6):
            for j in range(5):
                x0, y0 = ox + i * cell + 2, oy + j * cell + 2
                if (i, j) == (3, 2):
                    d.rectangle([x0, y0, x0 + cell - 4, y0 + cell - 4], fill=(40, 90, 110, 255))   # the empty cell: water, glow
                    continue
                d.rectangle([x0, y0, x0 + cell - 4, y0 + cell - 4], fill=(116, 140, 128, 255))
                d.rectangle([x0 + 5, y0 + 5, x0 + cell - 9, y0 + cell - 9], fill=(86, 108, 98, 255))
                for a, b2 in ((7, 7), (cell - 11, cell - 11)):
                    d.ellipse([x0 + a - 2, y0 + b2 - 2, x0 + a + 3, y0 + b2 + 3], fill=(20, 28, 26, 255))
        cx, cy = ox + 3.5 * cell, oy + 2.5 * cell
        d.line([(cx - 18, cy), (cx + 18, cy)], fill=(230, 240, 236, 255), width=2)
        d.line([(cx, cy - 18), (cx, cy + 18)], fill=(230, 240, 236, 255), width=2)
        f = _font(18)
        d.text((8, 6), "K-11", font=f, fill=(236, 244, 240, 255))
        f2 = _font(12, bold=False)
        d.text((8, h - 20), "RM-1 MAST CAM", font=f2, fill=(200, 214, 208, 255))
        # scan lines
        for y in range(0, h, 3):
            d.line([(0, y), (w, y)], fill=(0, 0, 0, 40))
        return img
    if name in ("SCALE_COL", "SCALE_ROW"):
        d.rectangle([0, 0, w - 1, h - 1], fill=(222, 182, 40, 255))
        d.rectangle([0, 0, w - 1, h - 1], outline=(30, 30, 32, 255), width=3)
        labels = [str(k) for k in range(1, 16)] if name == "SCALE_COL" else list(CORE_ROWS)
        p = CORE_PITCH * PX
        f = _font(int(h * 0.5))
        for k, lab in enumerate(labels):
            cx = w / 2 + (k - 7) * p
            for e in (cx - p / 2, cx + p / 2):
                d.line([(e, 0), (e, h * 0.35)], fill=(24, 24, 26, 255), width=3)
            d.line([(cx, h * 0.62), (cx, h - 1)], fill=(24, 24, 26, 255), width=4)
            bb = d.textbbox((0, 0), lab, font=f)
            d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], h * 0.08 - bb[1]), lab, font=f, fill=(24, 24, 26, 255))
        return img
    YEL, MAG, INK = (250, 210, 20, 255), (160, 30, 130, 255), (24, 24, 26, 255)

    def trefoil(cx, cy, r, col):
        d.ellipse([cx - r * 0.2, cy - r * 0.2, cx + r * 0.2, cy + r * 0.2], fill=col)
        for k in range(3):
            a0 = -90 + 120 * k - 30
            d.pieslice([cx - r, cy - r, cx + r, cy + r], a0, a0 + 60, fill=col)
        d.ellipse([cx - r * 0.3, cy - r * 0.3, cx + r * 0.3, cy + r * 0.3], fill=YEL)
        d.ellipse([cx - r * 0.2, cy - r * 0.2, cx + r * 0.2, cy + r * 0.2], fill=col)

    def lines(texts, y0, size, col=INK):
        f = _font(size)
        y = y0
        for t in texts:
            bb = d.textbbox((0, 0), t, font=f)
            d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], y - bb[1]), t, font=f, fill=col)
            y += (bb[3] - bb[1]) + size * 0.35
    if name in ("SIGN_CONTAM", "SIGN_RAD"):
        d.rectangle([0, 0, w - 1, h - 1], fill=YEL, outline=MAG, width=6)
        d.rectangle([0, 0, w - 1, int(h * 0.26)], fill=MAG)
        f = _font(int(h * 0.17))
        bb = d.textbbox((0, 0), "CAUTION", font=f)
        d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], h * 0.13 - (bb[3] - bb[1]) / 2 - bb[1]), "CAUTION", font=f, fill=YEL)
        trefoil(w * 0.19, h * 0.62, h * 0.24, MAG)
        f2 = _font(int(h * 0.105))
        words = ["CONTAMINATED", "AREA"] if name == "SIGN_CONTAM" else ["RADIATION", "AREA"]
        y = h * 0.4
        for t in words:
            bb = d.textbbox((0, 0), t, font=f2)
            d.text((w * 0.67 - (bb[2] - bb[0]) / 2 - bb[0], y - bb[1]), t, font=f2, fill=MAG)
            y += h * 0.19
        return img
    if name == "SIGN_HRA":
        d.rectangle([0, 0, w - 1, h - 1], fill=YEL, outline=MAG, width=6)
        d.rectangle([0, 0, w - 1, int(h * 0.26)], fill=MAG)
        f = _font(int(h * 0.17))
        bb = d.textbbox((0, 0), "CAUTION", font=f)
        d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], h * 0.13 - (bb[3] - bb[1]) / 2 - bb[1]), "CAUTION", font=f, fill=YEL)
        trefoil(w * 0.2, h * 0.6, h * 0.19, MAG)
        f2 = _font(int(h * 0.095))
        y = h * 0.36
        for t in ("HIGH", "RADIATION", "AREA"):
            bb = d.textbbox((0, 0), t, font=f2)
            d.text((w * 0.65 - (bb[2] - bb[0]) / 2 - bb[0], y - bb[1]), t, font=f2, fill=MAG)
            y += h * 0.14
        f3 = _font(int(h * 0.06))
        t = "LOCKED  -  RP KEY CONTROL"
        bb = d.textbbox((0, 0), t, font=f3)
        d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], h * 0.86 - bb[1]), t, font=f3, fill=MAG)
        return img
    if name == "MOVE_SCALE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(206, 208, 204, 255), outline=(70, 72, 76, 255), width=3)
        x0, x1 = w * 0.08, w * 0.92
        f = _font(int(h * 0.26))
        for k in range(9):
            x = x0 + (x1 - x0) * k / 8
            d.line([(x, h * 0.08), (x, h * (0.42 if k % 4 == 0 else 0.28))], fill=INK, width=3)
            if k % 4 == 0:
                t = str(k // 4)
                bb = d.textbbox((0, 0), t, font=f)
                d.text((x - (bb[2] - bb[0]) / 2 - bb[0], h * 0.46 - bb[1]), t, font=f, fill=INK)
        for t, k, col in (("COLD", 0, (30, 80, 170, 255)), ("HOT", 6, (190, 40, 30, 255))):
            x = x0 + (x1 - x0) * k / 8
            bb = d.textbbox((0, 0), t, font=f)
            d.text((min(w - (bb[2] - bb[0]) - 4, max(4, x - (bb[2] - bb[0]) / 2)) - bb[0], h * 0.7 - bb[1]), t, font=f, fill=col)
        x = x0 + (x1 - x0) * 6 / 8
        d.line([(x, h * 0.08), (x, h * 0.42)], fill=(190, 40, 30, 255), width=5)
        return img
    if name == "SG_PLATE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(200, 202, 196, 255), outline=(60, 62, 66, 255), width=5)
        lines(["STEAM GENERATOR"], h * 0.07, int(h * 0.12))
        lines(["U-TUBE  3388 TUBES  7/8 IN OD", "PRIMARY  2485 PSIG  650 F", "SECONDARY  1185 PSIG  600 F",
               "HEIGHT 67 FT 9 IN", "ASME III CLASS 1   NB 4471"], h * 0.3, int(h * 0.064))
        return img
    if name == "PZR_PLATE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(200, 202, 196, 255), outline=(60, 62, 66, 255), width=5)
        lines(["PRESSURIZER"], h * 0.07, int(h * 0.12))
        lines(["VOLUME 1800 CU FT", "DESIGN 2485 PSIG  680 F", "78 HEATERS  1800 KW", "3 SAFETY VALVES  6 IN",
               "ASME III CLASS 1   NB 4502"], h * 0.3, int(h * 0.064))
        return img
    if name == "RCP_PLATE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(200, 202, 196, 255), outline=(60, 62, 66, 255), width=5)
        lines(["RCP MOTOR"], h * 0.07, int(h * 0.12))
        lines(["6000 HP  12500 V  3 PH  60 HZ", "1189 RPM  6 POLE  CLASS B", "DRIP PROOF  AIR COOLED",
               "FLYWHEEL 13200 LB", "THRUST BRG  DOUBLE KINGSBURY"], h * 0.3, int(h * 0.064))
        return img
    if name == "SIGN_FME":
        d.rectangle([0, 0, w - 1, h - 1], fill=(30, 90, 170, 255), outline=(240, 240, 240, 255), width=6)
        lines(["FME ZONE"], h * 0.12, int(h * 0.2), (250, 250, 250, 255))
        lines(["FOREIGN MATERIAL", "EXCLUSION AREA", "LOG ALL ITEMS IN AND OUT"], h * 0.5, int(h * 0.085), (250, 250, 250, 255))
        return img
    if name == "SIGN_FRISK":
        d.rectangle([0, 0, w - 1, h - 1], fill=YEL, outline=MAG, width=5)
        d.rectangle([0, 0, w - 1, int(h * 0.16)], fill=MAG)
        f = _font(int(h * 0.09))
        bb = d.textbbox((0, 0), "FRISK HERE", font=f)
        d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], h * 0.08 - (bb[3] - bb[1]) / 2 - bb[1]), "FRISK HERE", font=f, fill=YEL)
        f2 = _font(int(h * 0.05))
        y = h * 0.22
        for t in ("1  HANDS FIRST", "2  HEAD AND NECK", "3  FRONT, THEN BACK", "4  FEET LAST", "", "PROBE 1/2 IN FROM SKIN", "2 IN PER SECOND", "", "ALARM OR > 100 CPM", "ABOVE BACKGROUND:", "STOP - CALL RP"):
            d.text((w * 0.08, y), t, font=f2, fill=MAG if t.startswith(("ALARM", "ABOVE", "STOP")) else INK)
            y += h * 0.068
        return img
    if name == "STEP_OFF":
        d.rectangle([0, 0, w - 1, h - 1], fill=YEL)
        for k in range(0, w + h, 46):
            d.polygon([(k, 0), (k + 22, 0), (k + 22 - h, h), (k - h, h)], fill=MAG)
        d.rectangle([int(w * 0.12), int(h * 0.2), int(w * 0.88), int(h * 0.8)], fill=YEL)
        lines(["STEP OFF PAD"], h * 0.3, int(h * 0.14), MAG)
        lines(["REMOVE PCs BEFORE STEPPING HERE"], h * 0.58, int(h * 0.06), INK)
        return img
    if name == "CORE_MAP":
        d.rectangle([0, 0, w - 1, h - 1], fill=(236, 236, 230, 255), outline=(40, 40, 44, 255), width=8)
        f = _font(int(h * 0.04))
        lines(["UNIT 1  CORE MAP"], h * 0.015, int(h * 0.035))
        p = (w * 0.86) / 15
        ox, oy = w * 0.09, h * 0.09
        rng = [(5, 11), (3, 13), (2, 14), (2, 14), (1, 15), (1, 15), (1, 15), (1, 15), (1, 15), (1, 15), (1, 15), (2, 14), (2, 14), (3, 13), (5, 11)]
        for i, row in enumerate(CORE_ROWS):
            bb = d.textbbox((0, 0), row, font=f)
            d.text((ox - p * 0.6 - (bb[2] - bb[0]) / 2, oy + (i + 0.5) * p - (bb[3] - bb[1]) / 2 - bb[1]), row, font=f, fill=INK)
            for j in range(1, 16):
                x0, y0 = ox + (j - 1) * p, oy + i * p
                if rng[i][0] <= j <= rng[i][1]:
                    d.rectangle([x0 + 2, y0 + 2, x0 + p - 3, y0 + p - 3], fill=(206, 210, 206, 255), outline=(60, 62, 66, 255), width=2)
        for j in range(1, 16):
            lab = str(j)
            bb = d.textbbox((0, 0), lab, font=f)
            d.text((ox + (j - 0.5) * p - (bb[2] - bb[0]) / 2 - bb[0], oy + 15 * p + p * 0.15 - bb[1]), lab, font=f, fill=INK)
        return img
    if name == "ARM_FACE":
        d.rectangle([0, 0, w - 1, h - 1], fill=(200, 202, 196, 255), outline=(60, 62, 66, 255), width=4)
        lines(["AREA RADIATION MONITOR", "RM-ARM-10  REFUELING FLOOR"], h * 0.05, int(h * 0.075))
        f = _font(int(h * 0.06))
        d.rectangle([int(w * 0.12), int(h * 0.36), int(w * 0.76), int(h * 0.66)], fill=(16, 16, 18, 255))
        d.rectangle([int(w * 0.76), int(h * 0.36), int(w * 0.85), int(h * 0.66)], fill=(200, 202, 196, 255))
        for t, x, y in (("mR/hr", w * 0.78, h * 0.47), ("HIGH ALARM 15", w * 0.12, h * 0.72), ("ALERT 5", w * 0.62, h * 0.72), ("FAIL", w * 0.12, h * 0.85), ("RESET", w * 0.62, h * 0.85)):
            d.text((x, y), t, font=f, fill=INK)
        return img
    if name == "LOG_PAGES":
        d.rectangle([0, 0, w - 1, h - 1], fill=(244, 242, 232, 255), outline=(150, 146, 136, 255), width=2)
        d.line([(w / 2, 0), (w / 2, h)], fill=(150, 146, 136, 255), width=3)
        for k in range(1, 14):
            y = h * k / 14
            d.line([(8, y), (w / 2 - 8, y)], fill=(170, 186, 210, 255), width=1)
            d.line([(w / 2 + 8, y), (w - 8, y)], fill=(170, 186, 210, 255), width=1)
            if k < 10:
                d.line([(12, y - 3), (12 + (k * 37) % 70 + 40, y - 3)], fill=(40, 50, 90, 255), width=2)
                d.line([(w / 2 + 12, y - 3), (w / 2 + 12 + (k * 53) % 60 + 30, y - 3)], fill=(40, 50, 90, 255), width=2)
        return img
    if name == "LCD_DIGITS":
        d.rectangle([0, 0, w - 1, h - 1], fill=(150, 162, 140, 255))
        cw = w // len(LCD_CHARS)
        f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", int(h * 0.9))
        b0 = d.textbbox((0, 0), "0", font=f)
        ty = (h - (b0[3] - b0[1])) // 2 - b0[1]
        for k, ch in enumerate(LCD_CHARS):
            bb = d.textbbox((0, 0), ch, font=f)
            d.text((k * cw + (cw - (bb[2] - bb[0])) // 2 - bb[0], ty), ch, font=f, fill=(28, 32, 26, 255))
        return img
    if name.startswith("STUD_NOS_"):
        k = int(name.split("_")[-1])
        d.rectangle([0, 0, w - 1, h - 1], fill=(176, 180, 184, 255), outline=(100, 104, 108, 255), width=2)
        f = _font(int(h * 0.7))
        for j in range(9):
            lab = str((k - 1) * 9 + j + 1)
            cx = (j + 0.5) * STUD_PITCH * PX
            bb = d.textbbox((0, 0), lab, font=f)
            d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], (h - (bb[3] - bb[1])) / 2 - bb[1]), lab, font=f, fill=(34, 36, 40, 255))
        return img
    raise KeyError(name)


def _layout():
    """Where each legend sits on the 2048 px wide texture: (x, y, w, h) in pixels, packed in rows."""
    out = {}
    x = y = row_h = 0
    for text, w, h in TAGS + STENCILS + [(d[0], d[1], d[1]) for d in DIALS] + [("LED_DIGITS", LED_CHAR[0] * len(LED_CHARS), LED_CHAR[1])] + PICTURES:
        pw, ph = int(round(w * PX)), int(round(h * PX))
        if x + pw > 2048:
            x, y, row_h = 0, y + row_h + 4, 0
        out[text] = (x, y, pw, ph)
        x += pw + 4
        row_h = max(row_h, ph)
    return out, y + row_h


def legend_image():
    lay, height = _layout()
    img = Image.new("RGBA", (2048, ((height + 63) // 64) * 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stencil_names = {s[0] for s in STENCILS}
    dials = {d[0]: d for d in DIALS}
    pictures = {p[0] for p in PICTURES}
    for text, (x, y, w, h) in lay.items():
        if text in pictures:
            img.alpha_composite(_picture(text, w, h), (x, y))
            continue
        if text == "LED_DIGITS":
            cw = w // len(LED_CHARS)
            f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", int(h * 0.95))
            b0 = d.textbbox((0, 0), "0", font=f)
            ty = y + (h - (b0[3] - b0[1])) // 2 - b0[1]
            for k, ch in enumerate(LED_CHARS):
                bb = d.textbbox((0, 0), ch, font=f)
                tx = x + k * cw + (cw - (bb[2] - bb[0])) // 2 - bb[0]
                d.text((tx, ty), ch, font=f, fill=(255, 52, 36, 255))
            continue
        if text in dials:
            _, size, lo, hi, unit = dials[text]
            S = w * 4
            face = Image.new("RGBA", (S, S), (0, 0, 0, 0))
            fd = ImageDraw.Draw(face)
            fd.ellipse([2, 2, S - 3, S - 3], fill=(232, 230, 222, 255), outline=(30, 30, 32, 255), width=S // 40)
            cx = cy = S / 2
            rr = S * 0.42
            for k in range(11):
                a = math.radians(-135 + 27 * k) - math.pi / 2
                r0 = rr * (0.8 if k % 5 == 0 else 0.87)
                fd.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * rr, cy + math.sin(a) * rr)], fill=(20, 20, 22, 255),
                        width=max(2, S // 70))
                if k % 5 == 0:
                    f = _font(int(S * 0.11))
                    lab = str(lo + (hi - lo) * k // 10)
                    bb = fd.textbbox((0, 0), lab, font=f)
                    tx, ty = cx + math.cos(a) * rr * 0.58, cy + math.sin(a) * rr * 0.58
                    fd.text((tx - (bb[2] - bb[0]) / 2 - bb[0], ty - (bb[3] - bb[1]) / 2 - bb[1]), lab, font=f, fill=(20, 20, 22, 255))
            # the normal band (green) and the alarm band (red)
            for a0, a1, col in ((-135 + 27 * 3, -135 + 27 * 6, (40, 150, 70, 255)), (-135 + 27 * 8, 135, (196, 40, 34, 255))):
                fd.arc([cx - rr * 1.02, cy - rr * 1.02, cx + rr * 1.02, cy + rr * 1.02], a0 - 90, a1 - 90, fill=col, width=max(3, S // 30))
            f = _font(int(S * 0.1))
            bb = fd.textbbox((0, 0), unit, font=f)
            fd.text((cx - (bb[2] - bb[0]) / 2 - bb[0], cy + rr * 0.45), unit, font=f, fill=(20, 20, 22, 255))
            img.alpha_composite(face.resize((w, h), Image.LANCZOS), (x, y))
            continue
        if text in stencil_names:
            # Cut-out paint: black letters with the stencil's bridges (gaps), slightly worn edges.
            f = _font(int(h * 0.86))
            b = d.textbbox((0, 0), text, font=f)
            tw = b[2] - b[0]
            sx = min(1.0, (w - 8) / max(1, tw))
            tmp = Image.new("L", (tw + 4, h), 0)
            ImageDraw.Draw(tmp).text((2 - b[0], (h - (b[3] - b[1])) // 2 - b[1]), text, font=f, fill=255)
            tmp = tmp.resize((max(1, int((tw + 4) * sx)), h), Image.LANCZOS)
            # the bridges: thin horizontal gaps across each letter's middle
            td = ImageDraw.Draw(tmp)
            td.rectangle([0, int(h * 0.48), tmp.width, int(h * 0.48) + max(1, h // 22)], fill=0)
            paint = Image.new("RGBA", tmp.size, (28, 30, 32, 255))
            paint.putalpha(tmp.point(lambda v: 235 if v > 110 else 0))
            img.alpha_composite(paint, (x + (w - tmp.width) // 2, y))
            continue
        # Engraved aluminium tag: brushed metal, a darker border, dark engraved letters, a rivet at each end.
        for j in range(h):
            for i in range(0, w, 3):
                g = 176 + ((i * 7 + j * 13) % 11) - 5
                d.line([(x + i, y + j), (x + min(w - 1, i + 2), y + j)], fill=(g, g + 3, g + 5, 255))
        d.rectangle([x, y, x + w - 1, y + h - 1], outline=(112, 116, 120, 255), width=max(1, h // 18))
        rr = max(3, h // 7)
        for cx in (x + rr * 2 + 2, x + w - rr * 2 - 3):
            cy = y + h // 2
            d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(132, 136, 140, 255), outline=(84, 88, 92, 255))
            d.point([(cx - rr // 3, cy - rr // 3)], fill=(220, 224, 228, 255))
        f = _font(int(h * 0.62))
        b = d.textbbox((0, 0), text, font=f)
        tw = b[2] - b[0]
        room = w - rr * 8 - 8
        if tw > room:
            f = _font(max(8, int(h * 0.62 * room / tw)))
            b = d.textbbox((0, 0), text, font=f)
            tw = b[2] - b[0]
        tx, ty = x + (w - tw) // 2 - b[0], y + (h - (b[3] - b[1])) // 2 - b[1]
        d.text((tx + 1, ty + 1), text, font=f, fill=(214, 218, 222, 255))   # the engraving's lit lower edge
        d.text((tx, ty), text, font=f, fill=(38, 40, 44, 255))
    return img


def legend(text):
    """The material for a legend: its region of the legend texture."""
    lay, height = _layout()
    H = ((height + 63) // 64) * 64
    x, y, w, h = lay[text]
    return [LEGEND, (x + 0.5) / 2048, (y + 0.5) / H, (x + w - 0.5) / 2048, (y + h - 0.5) / H]


def lcd_char(ch):
    """The legend region of one grey LCD character."""
    m = legend("LCD_DIGITS")
    k = LCD_CHARS.index(ch)
    du = (m[3] - m[1]) / len(LCD_CHARS)
    return [m[0], m[1] + du * k, m[2], m[1] + du * (k + 1), m[4]]


def led_char(ch):
    """The legend region of one LED character."""
    m = legend("LED_DIGITS")
    k = LED_CHARS.index(ch)
    du = (m[3] - m[1]) / len(LED_CHARS)
    return [m[0], m[1] + du * k, m[2], m[1] + du * (k + 1), m[4]]


def legend_size(text):
    for t, w, h in TAGS + STENCILS + PICTURES:
        if t == text:
            return w, h
    for d in DIALS:
        if d[0] == text:
            return d[1], d[1]
    if text == "LED_DIGITS":
        return LED_CHAR[0] * len(LED_CHARS), LED_CHAR[1]
    raise KeyError(text)


# ---------------------------------------------------------------- geometry helpers on a Board

def P(r, a, y):
    return (C + r * math.cos(a), y, C + r * math.sin(a))


def tile_mat(name, u0, v0, u1, v1):
    """Part of a tiling material's cell (fractions), for curved faces cut into pieces of a block or less."""
    e = 0.002
    return [name, max(0.0, u0) + e, max(0.0, v0) + e, min(1.0, u1) - e, min(1.0, v1) - e]


class Head(Board):
    def __init__(self):
        super().__init__(7, 101)
        self.live = {"fans": [], "fan_lamps": [], "dial": None, "power_lamps": []}

    def cyl(self, a, b, r, mat, cap=None, sides=10, lst=None, both=None):
        """A cylinder; the client caps only its b end, so a thick one standing free (a drum, a motor, a wheel) also gets
        a cap at its a end (a second, paper-thin cylinder turned the other way)."""
        super().cyl(a, b, r, mat, cap, sides, lst)
        if both is None:
            both = r >= 0.06
        if both:
            d = [b[i] - a[i] for i in range(3)]
            L = math.sqrt(sum(v * v for v in d)) or 1.0
            e = [a[i] - d[i] / L * 0.002 for i in range(3)]
            c = cap if cap is not None else mat
            super().cyl(a, e, r, c, c, sides, lst)

    # A band of a surface of revolution between (r0, y0) and (r1, y1), angles a0..a1, outward; texture tiled about a
    # block a cell across, by the band's length up its face.
    def band(self, r0, y0, r1, y1, mat, a0=0.0, a1=2 * math.pi, per_block=1.0, lst=None, inward=False, stretch=False):
        rm = max(r0, r1, 0.05)
        length = math.hypot(r1 - r0, y1 - y0)
        n = max(8, int(round(abs(a1 - a0) * rm * 3 / per_block)))
        seg = abs(a1 - a0) * rm / n
        for i in range(n):
            aa, ab = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
            if inward:
                aa, ab = ab, aa
            tl, tr = P(r1, aa, y1), P(r1, ab, y1)
            bl, br = P(r0, aa, y0), P(r0, ab, y0)
            if stretch:
                m = mat
            else:
                u0 = (i * seg / per_block) % 1.0
                u1 = min(1.0, u0 + seg / per_block)
                m = tile_mat(mat, u0, 0.0, u1, min(1.0, length / per_block))
            if r1 < 1e-4:
                self.quad(tl, bl, br, tr, m, lst)
            else:
                self.quad(tl, bl, br, tr, m, lst)

    def tube(self, r, y0, y1, mat, a0=0.0, a1=2 * math.pi, inward=False):
        """A vertical cylinder wall, cut into bands a block high."""
        y = y0
        while y < y1 - 1e-6:
            yt = min(y1, math.floor(y + 1e-6) + 1.0)
            self.band(r, y, r, yt, mat, a0, a1, inward=inward)
            y = yt

    def ring_top(self, r_in, r_out, y, mat, a0=0.0, a1=2 * math.pi, down=False, n=None):
        """A flat annulus facing up (or down), its texture a block a cell (tiled by the pieces' sizes)."""
        n = n or max(12, int(round(abs(a1 - a0) * r_out * 3)))
        depth = r_out - r_in
        rows = max(1, math.ceil(depth - 1e-6))
        for j in range(rows):
            ri, ro = r_in + depth * j / rows, r_in + depth * (j + 1) / rows
            arc = abs(a1 - a0) * ro / n
            for i in range(n):
                aa, ab = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
                if down:
                    aa, ab = ab, aa
                o_a, o_b, i_a, i_b = P(ro, aa, y), P(ro, ab, y), P(ri, aa, y), P(ri, ab, y)
                if isinstance(mat, str):
                    u0 = (i * arc) % 1.0
                    m = tile_mat(mat, u0, 0.0, min(1.0, u0 + arc), min(1.0, ro - ri))
                else:
                    m = mat
                if ri < 1e-6:
                    self.quad(i_a, o_a, o_b, o_b, m)      # a triangle (its last corner repeated)
                else:
                    self.quad(i_a, o_a, o_b, i_b, m)

    def disc(self, cx, y, cz, r, mat, down=False, n=24):
        for i in range(n):
            aa, ab = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            if down:
                aa, ab = ab, aa
            c = (cx, y, cz)
            pa = (cx + r * math.cos(aa), y, cz + r * math.sin(aa))
            pb = (cx + r * math.cos(ab), y, cz + r * math.sin(ab))
            self.quad(c, pa, pb, pb, mat)

    def tag(self, text, centre, normal, up=(0, 1, 0), d=0.006):
        """An engraved tag standing d off a face: centre on the face, the face's outward normal."""
        w, h = legend_size(text)
        n = _unit(normal)
        u = _unit(_cross(up, n))          # right as seen from outside (frame mirrored: see the module notes)
        u = (-u[0], -u[1], -u[2])
        v = _unit(_cross(n, u))
        v = (-v[0], -v[1], -v[2]) if v[1] < 0 or (abs(v[1]) < 1e-9 and _dot(v, up) < 0) else v
        c = tuple(centre[i] + n[i] * d / 2 for i in range(3))
        edge = sw("dgrey")
        self.obox(c, u, v, w / 2, h / 2, d / 2, [legend(text), None, edge, edge, edge, edge])

    def stencil(self, text, centre, normal, up=(0, 1, 0), lift=0.004):
        """Painted cut-out letters on a face: one quad just off it, the letters' texture with clear between them."""
        w, h = legend_size(text)
        n = _unit(normal)
        u = _unit(_cross(up, n))
        u = (-u[0], -u[1], -u[2])
        v = _unit(_cross(n, u))
        if v[1] < 0:
            v = (-v[0], -v[1], -v[2])
        c = tuple(centre[i] + n[i] * lift for i in range(3))
        tl = tuple(c[i] - u[i] * w / 2 + v[i] * h / 2 for i in range(3))
        bl = tuple(c[i] - u[i] * w / 2 - v[i] * h / 2 for i in range(3))
        br = tuple(c[i] + u[i] * w / 2 - v[i] * h / 2 for i in range(3))
        tr = tuple(c[i] + u[i] * w / 2 + v[i] * h / 2 for i in range(3))
        self.quad(tl, bl, br, tr, legend(text))


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def _unit(a):
    l = math.sqrt(_dot(a, a)) or 1.0
    return tuple(x / l for x in a)


# ---------------------------------------------------------------- the pieces

def vessel(b):
    """The vessel below the cavity floor: bottom head, insulated shell, nozzle belt, flange; the seal ring."""
    # Bottom head (in insulation), a hemisphere from the shell down to y 0.
    steps = 6
    for k in range(steps):
        t0, t1 = k / steps, (k + 1) / steps
        r0, y0 = R_SHELL * math.sin(t0 * math.pi / 2), R_SHELL - R_SHELL * math.cos(t0 * math.pi / 2)
        r1, y1 = R_SHELL * math.sin(t1 * math.pi / 2), R_SHELL - R_SHELL * math.cos(t1 * math.pi / 2)
        b.band(r0, y0, r1, y1, "insulation")
    # Shell in reflective metal insulation up to the nozzle belt's insulation box, then the bare flange.
    b.tube(R_SHELL, R_SHELL, 11.2, "insulation")
    b.band(R_SHELL, 11.2, R_FLANGE, 11.45, "carbon_steel")
    b.tube(R_FLANGE, 11.45, FLANGE_TOP, "flange")
    # The nozzles: four outlet (hot leg, bigger) and four inlet (cold leg), either side of each loop's diagonal, with
    # their safe ends (stainless) and the leg's first length of pipe in insulation, ending at the shield wall.
    tags = []
    for loop, (hot, cold) in enumerate(LOOP_AZ):
        for kind, a, r_noz in (("HOT", hot, 0.42), ("COLD", cold, 0.37)):
            ux, uz = math.cos(a), math.sin(a)
            root = (C + ux * (R_SHELL - 0.1), NOZZLE_Y, C + uz * (R_SHELL - 0.1))
            boss = (C + ux * (R_SHELL + 0.25), NOZZLE_Y, C + uz * (R_SHELL + 0.25))
            safe = (C + ux * (R_SHELL + 0.55), NOZZLE_Y, C + uz * (R_SHELL + 0.55))
            end = (C + ux * 4.1, NOZZLE_Y, C + uz * 4.1)
            b.cyl(root, boss, r_noz + 0.12, "carbon_steel", "carbon_steel", 16)
            b.cyl(boss, safe, r_noz + 0.02, "stainless", "stainless", 16)
            b.cyl(safe, (safe[0] + ux * 0.06, NOZZLE_Y, safe[2] + uz * 0.06), r_noz + 0.06, "weld_band", "weld_band", 16)
            b.cyl((safe[0] + ux * 0.06, NOZZLE_Y, safe[2] + uz * 0.06), end, r_noz + 0.09, "insulation", "insulation", 16)
            tags.append((loop + 1, kind, a))
    return tags


def seal_ring(b):
    """The refuelling cavity seal ring: a stainless ring from the vessel flange out to the cavity floor's edge, its
    clamps round the inside."""
    b.ring_top(R_FLANGE, 3.45, CAVITY_FLOOR, "stainless")
    b.tube(3.45, CAVITY_FLOOR - 0.12, CAVITY_FLOOR, "stainless_dark")
    for k in range(12):
        a = 2 * math.pi * (k + 0.5) / 12
        c = P(R_FLANGE + 0.12, a, CAVITY_FLOOR + 0.035)
        b.obox(c, (math.cos(a), 0, math.sin(a)), (0, 1, 0), 0.1, 0.035, 0.07, "gunmetal")


def closure_head(b, studs=True):
    """The head's flange with its studs, nuts and spherical washers (studs=False: they are out, in their racks, and the
    flange shows its stud holes); the insulated dome; the lifting lugs."""
    y0, y1 = HEAD_FLANGE
    b.tube(R_FLANGE, y0, y1, "flange")
    b.ring_top(2.25, R_FLANGE, y1, "carbon_steel")
    if not studs:
        for k in range(N_STUDS):
            a = 2 * math.pi * k / N_STUDS
            x, _, z = P(R_STUD, a, 0)
            b.cyl((x, y1, z), (x, y1 + 0.002, z), 0.09, "black", "black", 10)
            b.cyl((x, y0, z), (x, y0 - 0.002, z), 0.09, "black", "black", 10)
    for k in (range(N_STUDS) if studs else ()):
        a = 2 * math.pi * k / N_STUDS
        x, _, z = P(R_STUD, a, 0)
        b.cyl((x, y1, z), (x, y1 + 0.05, z), 0.12, "stainless_dark", "stainless_dark", 10)       # spherical washer
        b.cyl((x, y1 + 0.05, z), (x, y1 + 0.27, z), 0.1, "gunmetal", "gunmetal", 6)                # the nut
        b.cyl((x, y1 + 0.27, z), (x, y1 + 0.33, z), 0.055, "carbon_steel", "carbon_steel", 8)      # stud end
    # The dome in its insulation (stainless-jacketed panels), up to the CRDM penetrations.
    steps = 6
    for k in range(steps):
        t0, t1 = k / steps, (k + 1) / steps
        r0, ya = 2.25 * math.cos(t0 * math.pi / 2 * 0.86), y1 + 1.3 * math.sin(t0 * math.pi / 2 * 0.86)
        r1, yb = 2.25 * math.cos(t1 * math.pi / 2 * 0.86), y1 + 1.3 * math.sin(t1 * math.pi / 2 * 0.86)
        b.band(r0, ya, r1, yb, "stainless_dark", stretch=False)
    b.disc(C, DOME_TOP - 0.02, C, 2.25 * math.cos(math.pi / 2 * 0.86), "stainless_dark")


LUGS = [math.radians(90), math.radians(210), math.radians(330)]


def head_package(b, lifted_cables=False):
    """The integrated head assembly on the head."""
    # Ring beam on its saddles (three segments, gaps at the lugs).
    for k in range(3):
        a0, a1 = LUGS[k] + 0.18, LUGS[(k + 1) % 3] - 0.18 + (2 * math.pi if k == 2 else 0)
        b.band(R_SHROUD + 0.25, 13.05, R_SHROUD + 0.25, 13.35, "gunmetal", a0, a1)
        b.band(R_SHROUD - 0.15, 13.35, R_SHROUD - 0.15, 13.05, "gunmetal", a0, a1)
        b.ring_top(R_SHROUD - 0.15, R_SHROUD + 0.25, 13.35, "gunmetal", a0, a1)
    for k in range(6):
        a = math.radians(30 + 60 * k)
        c = P(R_SHROUD + 0.05, a, 12.86)
        b.obox(c, (math.cos(a), 0, math.sin(a)), (0, 1, 0), 0.22, 0.19, 0.2, "gunmetal")
    # The shroud: three tiers with bolted flange rings between them.
    yb, j1, j2, yt = SHROUD
    door_a = [math.radians(240), math.radians(60)]    # access doors (bottom tier), between the columns
    inlet_a = [math.radians(170), math.radians(350)]  # air inlets (middle tier)
    door_w = math.radians(26)
    inlet_w = math.radians(30)
    # Bottom tier, round the two door openings.
    _wall_with_gaps(b, R_SHROUD, yb, j1, "stainless", [(a - door_w / 2, a + door_w / 2) for a in door_a])
    # Middle tier, round the two air inlets (louvre panels set back in the openings).
    _wall_with_gaps(b, R_SHROUD, j1, j2, "stainless", [(a - inlet_w / 2, a + inlet_w / 2) for a in inlet_a])
    for a in inlet_a:
        b.band(R_SHROUD - 0.08, j1 + 0.25, R_SHROUD - 0.08, j2 - 0.25, ["louvre", 0.02, 0.02, 0.98, 0.98], a - inlet_w / 2 + 0.02,
               a + inlet_w / 2 - 0.02, stretch=True)
        b.band(R_SHROUD, j1, R_SHROUD, j1 + 0.25, "stainless", a - inlet_w / 2, a + inlet_w / 2)
        b.band(R_SHROUD, j2 - 0.25, R_SHROUD, j2, "stainless", a - inlet_w / 2, a + inlet_w / 2)
        for s in (-1, 1):   # the opening's jambs
            aj = a + s * inlet_w / 2
            p0, p1 = P(R_SHROUD - 0.08, aj, j1 + 0.25), P(R_SHROUD, aj, j2 - 0.25)
            b.obox(((p0[0] + p1[0]) / 2, (j1 + j2) / 2, (p0[2] + p1[2]) / 2), (math.cos(aj), 0, math.sin(aj)), (0, 1, 0), 0.05,
                   (j2 - j1) / 2 - 0.25, 0.012, "stainless_dark")
    # Upper tier, plain (cable apertures behind the connector panels).
    b.tube(R_SHROUD, j2, yt, "stainless")
    # Joint rings, the top's seismic ring beam.
    for y in (j1, j2):
        b.band(R_SHROUD + 0.06, y - 0.05, R_SHROUD + 0.06, y + 0.05, "gunmetal")
        b.ring_top(R_SHROUD, R_SHROUD + 0.06, y + 0.05, "gunmetal")
        for k in range(24):
            a = 2 * math.pi * (k + 0.5) / 24
            x, _, z = P(R_SHROUD + 0.075, a, 0)
            b.cyl((x - math.cos(a) * 0.01, y, z - math.sin(a) * 0.01), (x + math.cos(a) * 0.02, y, z + math.sin(a) * 0.02), 0.022,
                  "carbon_steel", "carbon_steel", 6)
    b.band(R_SHROUD + 0.12, yt - 0.3, R_SHROUD + 0.12, yt, "paint_grey")
    b.ring_top(R_SHROUD, R_SHROUD + 0.12, yt, "paint_grey")
    b.band(R_SHROUD + 0.0, yt - 0.3, R_SHROUD + 0.12, yt - 0.3, "paint_grey")
    # Six support columns up the outside, tied at three heights.
    for k in range(6):
        a = math.radians(30 + 60 * k)
        c0, c1 = P(R_SHROUD + 0.13, a, 13.35), P(R_SHROUD + 0.13, a, yt - 0.3)
        b.obox(((c0[0] + c1[0]) / 2, (c0[1] + c1[1]) / 2, (c0[2] + c1[2]) / 2), (math.cos(a), 0, math.sin(a)), (0, 1, 0),
               0.1, (c1[1] - c0[1]) / 2, 0.1, "paint_grey")
    # Access doors (bottom tier): door 1 open (on its hinges, out from the opening), door 2 shut; frames, handles.
    for i, a in enumerate(door_a):
        _door(b, a, door_w, yb, j1, open_=(i == 0), number=i + 1)
    # The missile shield plate and the plenum on it, the fans, the fan control box.
    b.tube(R_SHROUD + 0.2, SHIELD[0], SHIELD[1], "carbon_steel")
    b.ring_top(0.0, R_SHROUD + 0.2, SHIELD[1], "carbon_steel")
    b.ring_top(R_SHROUD, R_SHROUD + 0.2, SHIELD[0], "carbon_steel", down=True)
    _plenum(b)
    _connector_panels(b)
    _lift_rig(b)
    _crdms(b)
    # Tags: the head's on the head flange, by door 2.
    a = math.radians(115)
    b.tag("RV-1  REACTOR VESSEL CLOSURE HEAD", P(R_FLANGE, a, (HEAD_FLANGE[0] + HEAD_FLANGE[1]) / 2), (math.cos(a), 0, math.sin(a)))
    for i, a in enumerate(door_a):
        b.tag("IHA ACCESS DOOR %d" % (i + 1), P(R_SHROUD + 0.13, a, j1 - 0.3), (math.cos(a), 0, math.sin(a)))
    # Stencils on the middle tier between the inlets.
    for a in (math.radians(120), math.radians(300)):
        b.stencil("REACTOR HEAD", P(R_SHROUD + 0.002, a, (j1 + j2) / 2 + 0.25), (math.cos(a), 0, math.sin(a)))
        b.stencil("UNIT 1", P(R_SHROUD + 0.002, a, (j1 + j2) / 2 - 0.3), (math.cos(a), 0, math.sin(a)))


def _wall_with_gaps(b, r, y0, y1, mat, gaps):
    gaps = sorted(gaps)
    spans = []
    a = gaps[-1][1] - 2 * math.pi
    for g0, g1 in gaps:
        spans.append((a, g0))
        a = g1
    for a0, a1 in spans:
        y = y0
        while y < y1 - 1e-6:
            yt = min(y1, math.floor(y + 1e-6) + 1.0)
            b.band(r, y, r, yt, mat, a0, a1)
            b.band(r - 0.03, yt, r - 0.03, y, "stainless_dark", a0, a1)    # the inside face, seen through the open door
            y = yt


def _door(b, a, w, y0, y1, open_, number):
    """An access door in the bottom tier: a curved plate on two hinges at its left edge (seen from outside), a lever
    handle at the right; open, it stands out square from the opening."""
    hinge_a = a - w / 2
    hx, _, hz = P(R_SHROUD + 0.03, hinge_a, 0)
    dy0, dy1 = y0 + 0.12, y1 - 0.12
    chord = 2 * R_SHROUD * math.sin(w / 2)
    t = (-math.sin(hinge_a), 0, math.cos(hinge_a))         # along the wall, toward the door's middle
    n = (math.cos(hinge_a), 0, math.sin(hinge_a))
    if open_:
        # swung out 100 degrees about the hinge (a vertical axis at the opening's edge)
        ang = math.radians(100)
        d = (t[0] * math.cos(ang) + n[0] * math.sin(ang), 0, t[2] * math.cos(ang) + n[2] * math.sin(ang))
        nn = (d[2], 0, -d[0])
    else:
        d = (math.cos(a + math.pi / 2), 0, math.sin(a + math.pi / 2))
        hx, _, hz = P(R_SHROUD + 0.03, a, 0)
        hx, hz = hx - d[0] * chord / 2, hz - d[2] * chord / 2
        nn = (math.cos(a), 0, math.sin(a))
    cx, cz = hx + d[0] * chord / 2, hz + d[2] * chord / 2
    b.obox((cx, (dy0 + dy1) / 2, cz), d, (0, 1, 0), chord / 2, (dy1 - dy0) / 2, 0.03, "stainless")
    # hinges
    for hy in (dy0 + 0.25, dy1 - 0.25):
        b.cyl((hx, hy - 0.12, hz), (hx, hy + 0.12, hz), 0.035, "gunmetal", "gunmetal", 8)
    # lever handle near the free edge
    fx, fz = hx + d[0] * (chord - 0.14), hz + d[2] * (chord - 0.14)
    side = 1 if not open_ else 1
    hc = (fx + nn[0] * 0.05 * side, (dy0 + dy1) / 2, fz + nn[2] * 0.05 * side)
    b.obox(hc, (0, 1, 0), nn, 0.11, 0.02, 0.02, "black")
    b.cyl((fx, (dy0 + dy1) / 2 + 0.09, fz), (hc[0], (dy0 + dy1) / 2 + 0.09, hc[2]), 0.02, "gunmetal", "gunmetal", 6)
    # the frame round the opening
    for s in (-1, 1):
        aj = a + s * w / 2
        p = P(R_SHROUD + 0.04, aj, 0)
        b.obox((p[0], (y0 + y1) / 2, p[2]), (math.cos(aj), 0, math.sin(aj)), (0, 1, 0), 0.04, (y1 - y0) / 2, 0.04, "stainless_dark")
    b.band(R_SHROUD + 0.04, y1 - 0.1, R_SHROUD + 0.04, y1, "stainless_dark", a - w / 2, a + w / 2)
    b.band(R_SHROUD + 0.04, y0, R_SHROUD + 0.04, y0 + 0.1, "stainless_dark", a - w / 2, a + w / 2)
    b.ring_top(R_SHROUD - 0.03, R_SHROUD + 0.04, y0 + 0.1, "stainless_dark", a - w / 2, a + w / 2)


FANS = [math.radians(90), math.radians(210), math.radians(330)]
# The access ladder from cable bridge A's deck up to the walkway: its rails stand on the deck LADDER_R out along the
# bridge, LADDER_T to the bridge's right; LADDER_A is where it meets the walkway (the railing's gap).
LADDER_R, LADDER_T = 2.6, (0.6, 1.02)
LADDER_A = math.pi + math.atan2(sum(LADDER_T) / 2, LADDER_R)
FAN_R = 0.5


def _plenum(b):
    """The upper air plenum on the missile shield: a squat drum, the three fan housings on it (vertical axial fans
    blowing up), their motors on spiders, the guards; a grating walkway round them with a handrail; the ladder down to
    the cavity floor; the fan control box."""
    y0, y1 = PLENUM
    b.tube(1.9, y0, y1, "paint_grey")
    b.ring_top(0.0, 1.9, y1, "paint_grey")
    # Walkway: grating annulus round the plenum on the shield plate's edge, toe plate and handrail.
    b.ring_top(1.9, R_SHROUD + 0.2, SHIELD[1] + 0.01, "grating")
    b.band(R_SHROUD + 0.2, SHIELD[1], R_SHROUD + 0.2, SHIELD[1] + 0.1, "paint_yellow")
    for k in range(18):
        a = 2 * math.pi * (k + 0.5) / 18
        if abs(_ang(a, LADDER_A)) < 0.2:
            continue   # the ladder's gap
        x, _, z = P(R_SHROUD + 0.15, a, 0)
        b.cyl((x, SHIELD[1], z), (x, SHIELD[1] + 1.07, z), 0.022, "paint_yellow", "paint_yellow", 6)
    gap0, gap1 = LADDER_A + 0.2, LADDER_A - 0.2 + 2 * math.pi
    for y, r in ((SHIELD[1] + 1.07, 0.024), (SHIELD[1] + 0.55, 0.02)):
        _arc_rail(b, R_SHROUD + 0.15, y, gap0, gap1, r)
    # Fans: vaneaxial fans standing on the plenum, blowing up: a grey housing with flanged ends and stiffening rings,
    # the motor (blue) on its spider inside, the rotor, a guard on top.
    for i, a in enumerate(FANS):
        fx, _, fz = P(1.15, a, 0)
        b.cyl((fx, y1, fz), (fx, y1 + 0.06, fz), FAN_R + 0.12, "gunmetal", "gunmetal", 20)        # base flange
        b.cyl((fx, y1 + 0.06, fz), (fx, y1 + 0.74, fz), FAN_R + 0.04, "paint_grey", "paint_grey", 20)
        for ry in (y1 + 0.3, y1 + 0.52):
            b.cyl((fx, ry, fz), (fx, ry + 0.04, fz), FAN_R + 0.07, "paint_grey", "paint_grey", 20)
        b.cyl((fx, y1 + 0.74, fz), (fx, y1 + 0.8, fz), FAN_R + 0.12, "gunmetal", "gunmetal", 20)  # outlet flange
        b.disc(fx, y1 + 0.83, fz, FAN_R + 0.1, "grating", n=20)
        b.cyl((fx, y1 + 0.8, fz), (fx, y1 + 0.83, fz), FAN_R + 0.1, "gunmetal", "gunmetal", 20)
        # motor on its spider, hub
        b.cyl((fx, y1 + 0.18, fz), (fx, y1 + 0.56, fz), 0.17, "paint_blue", "gunmetal", 12)
        for k in range(3):
            sa = a + k * 2 * math.pi / 3
            b.obox((fx + math.cos(sa) * 0.36, y1 + 0.36, fz + math.sin(sa) * 0.36), (math.cos(sa), 0, math.sin(sa)), (0, 1, 0), 0.19, 0.02,
                   0.02, "gunmetal")
        b.live["fans"].append([round(fx, 4), round(y1 + 0.66, 4), round(fz, 4), FAN_R - 0.03])
        # the rotor drawn still here (the client turns it): five pitched blades and the hub
        for k in range(5):
            sa = 2 * math.pi * k / 5 + i * 0.4
            u = (math.cos(sa), 0, math.sin(sa))
            tng = (-math.sin(sa), 0, math.cos(sa))
            pitch = math.radians(28)
            v = (tng[0] * math.cos(pitch), math.sin(pitch), tng[2] * math.cos(pitch))
            bc = (fx + u[0] * 0.3, y1 + 0.66, fz + u[2] * 0.3)
            b.obox(bc, u, v, 0.18, 0.065, 0.01, "gunmetal")
        b.cyl((fx, y1 + 0.6, fz), (fx, y1 + 0.71, fz), 0.09, "black", "black", 10)
        # its tag on the housing, facing out
        b.tag("CRDM CLG FAN 1%s" % "ABC"[i], P(1.15 + FAN_R + 0.04, a, y1 + 0.41), (math.cos(a), 0, math.sin(a)))


def fan_box(b, foot, n, t, deck):
    """The CRDM cooling fans' local control box on its post on cable bridge A's deck, facing across the deck (n): its
    tag, then for each fan its tag, RUN lamp and HAND-OFF-AUTO selector, left to right; under them the shroud exhaust
    temperature dial. Its conduit drops into the bridge's cable tray (the fans' supply runs with the drive cables)."""
    fx, fz = foot
    yc = deck + 1.3
    # the post and its base plate
    b.box(fx - 0.05, deck + 0.02, fz - 0.05, fx + 0.05, yc - 0.4, fz + 0.05, ["gunmetal", "gunmetal", "gunmetal", "gunmetal", "gunmetal", None])
    b.box(fx - 0.14, deck, fz - 0.14, fx + 0.14, deck + 0.02, fz + 0.14, "gunmetal")
    bx, bz = fx + n[0] * 0.04, fz + n[2] * 0.04
    b.obox((bx, yc, bz), t, (0, 1, 0), 0.42, 0.4, 0.09, "paint_grey")
    face = (bx + n[0] * 0.09, bz + n[2] * 0.09)
    b.tag("CRDM CLG FAN CONTROL", (face[0], yc + 0.31, face[1]), n)
    for k in range(3):
        off = -0.24 + k * 0.24
        px, pz = face[0] + t[0] * off, face[1] + t[2] * off
        b.tag("FAN 1%s" % "ABC"[k], (px, yc + 0.17, pz), n)
        b.cyl((px, yc + 0.05, pz), (px + n[0] * 0.025, yc + 0.05, pz + n[2] * 0.025), 0.038, "gunmetal", "gunmetal", 12)
        b.live["fan_lamps"].append([round(px + n[0] * 0.036, 4), round(yc + 0.05, 4), round(pz + n[2] * 0.036, 4)])
        on = k < 2
        b.cyl((px + n[0] * 0.025, yc + 0.05, pz + n[2] * 0.025), (px + n[0] * 0.035, yc + 0.05, pz + n[2] * 0.035), 0.026,
              "lamp_green" if on else "black", "lamp_green" if on else "rubber", 12, lst=b.lit if on else None)
        # HAND-OFF-AUTO selector under each lamp (pointer at AUTO)
        b.cyl((px, yc - 0.08, pz), (px + n[0] * 0.02, yc - 0.08, pz + n[2] * 0.02), 0.035, "black", "black", 10)
        r45 = (t[0] * 0.7071, 0.7071, t[2] * 0.7071)
        b.obox((px + n[0] * 0.03, yc - 0.08, pz + n[2] * 0.03), r45, n, 0.035, 0.012, 0.008, "paint_white")
    b.tag("SHROUD EXH TEMP  \u00b0F", (face[0], yc - 0.2, face[1]), n)
    b.obox((face[0] + n[0] * 0.025, yc - 0.31, face[1] + n[2] * 0.025), t, (0, 1, 0), 0.08, 0.08, 0.025,
           ["gunmetal", None, "gunmetal", "gunmetal", "gunmetal", "gunmetal"])
    b.obox((face[0] + n[0] * 0.051, yc - 0.31, face[1] + n[2] * 0.051), t, (0, 1, 0), 0.07, 0.07, 0.001,
           [legend("DIAL_EXH"), None, None, None, None, None])
    b.live["dial"] = [round(face[0] + n[0] * 0.056, 4), round(yc - 0.31, 4), round(face[1] + n[2] * 0.056, 4), list(n), 0.06, 100, 200]
    # conduit from the box's bottom down the post's side into the tray
    cx_, cz_ = bx - t[0] * 0.3, bz - t[2] * 0.3
    b.cyl((cx_, deck + 0.13, cz_), (cx_, yc - 0.4, cz_), 0.025, "gunmetal", "gunmetal", 6)


def _ang(a, b):
    return (a - b + math.pi) % (2 * math.pi) - math.pi


def _arc_rail(b, r, y, a0, a1, rr, n=36):
    for k in range(n):
        aa, ab = a0 + (a1 - a0) * k / n, a0 + (a1 - a0) * (k + 1) / n
        p, q = P(r, aa, y), P(r, ab, y)
        b.cyl(p, q, rr, "paint_yellow", "paint_yellow", 6)


PANELS = [math.radians(180), math.radians(300), math.radians(60)]


def _connector_panels(b):
    """Three cable connector panels on the upper tier: a box with a hinged cover, rows of round multi-pin connectors
    with their plugs and cable tails, a POWER and an RPI lamp; the cables gather into the tray on the cable bridge."""
    j2, yt = SHROUD[2], SHROUD[3]
    for i, a in enumerate(PANELS):
        n = (math.cos(a), 0, math.sin(a))
        t = (-math.sin(a), 0, math.cos(a))      # right, as seen from outside
        base = P(R_SHROUD, a, 0)
        cx, cz = base[0] + n[0] * 0.12, base[2] + n[2] * 0.12
        yc = (j2 + yt) / 2 - 0.05
        b.obox((cx, yc, cz), t, (0, 1, 0), 0.5, 0.55, 0.12, "paint_grey")
        fx, fz = cx + n[0] * 0.12, cz + n[2] * 0.12
        b.tag("CRDM / RPI CABLE PANEL %s" % "ABC"[i], (fx, yc + 0.45, fz), n)
        # connectors: 3 rows of 5, each a socket with its plug (black) and a cable tail down
        for row in range(3):
            for col in range(5):
                off = -0.36 + col * 0.18
                py = yc + 0.22 - row * 0.2
                px, pz = fx + t[0] * off, fz + t[2] * off
                b.cyl((px, py, pz), (px + n[0] * 0.03, py, pz + n[2] * 0.03), 0.055, "gunmetal", "gunmetal", 10)
                b.cyl((px + n[0] * 0.03, py, pz + n[2] * 0.03), (px + n[0] * 0.1, py, pz + n[2] * 0.1), 0.045, "black", "rubber", 10)
        # lamps (POWER, RPI) on the cover's lower edge, each with its stencilled word above
        for k, word in enumerate(("POWER", "RPI")):
            off = -0.25 + k * 0.5
            px, pz = fx + t[0] * off, fz + t[2] * off
            ly = yc - 0.43
            b.stencil(word, (px, ly + 0.06, pz), n)
            b.cyl((px, ly, pz), (px + n[0] * 0.02, ly, pz + n[2] * 0.02), 0.03, "gunmetal", "gunmetal", 10)
            b.cyl((px + n[0] * 0.02, ly, pz + n[2] * 0.02), (px + n[0] * 0.03, ly, pz + n[2] * 0.03), 0.02, "lamp_white", "lamp_white", 10,
                  lst=b.lit)
            b.live["power_lamps"].append([round(px + n[0] * 0.031, 4), round(ly, 4), round(pz + n[2] * 0.031, 4)])
        # each plug's cable drops to the trough under the panel; the trough runs out along the panel's cable bridge
        tb = yc - 0.62
        for col in range(5):
            off = -0.36 + col * 0.18
            for row in range(3):
                py = yc + 0.22 - row * 0.2
                px, pz = fx + t[0] * off + n[0] * (0.1 + row * 0.03), fz + t[2] * off + n[2] * (0.1 + row * 0.03)
                b.cyl((px, py, pz), (px, tb + 0.1, pz), 0.017, "black", "black", 6)
        trough_in = (cx + n[0] * 0.05, cz + n[2] * 0.05)
        b.obox((trough_in[0] + n[0] * 0.12, tb + 0.05, trough_in[1] + n[2] * 0.12), n, (0, 1, 0), 0.28, 0.06, 0.46, "gunmetal")
        b.obox((trough_in[0] + n[0] * 0.12, tb + 0.115, trough_in[1] + n[2] * 0.12), n, (0, 1, 0), 0.27, 0.005, 0.44, "rubber")


def cable_bridge(b, raised=False):
    """A cable bridge from each connector panel straight out to the cavity wall: a steel walkway (grating deck, side
    channels) carrying the cable tray, handrails both sides; hinged at the wall, it is swung up against the wall for a
    lift. The tray ends in a sleeve through the cavity wall (the cables go on to the containment's cable spreading)."""
    j2, yt = SHROUD[2], SHROUD[3]
    y = (j2 + yt) / 2 - 0.05 - 0.62
    for i, a in enumerate(PANELS):
        n = (math.cos(a), 0, math.sin(a))
        t = (-math.sin(a), 0, math.cos(a))      # right, as seen from outside
        hw = 1.1 if i == 0 else 0.5             # bridge A is also the walkway's access: wider, the ladder on it
        r0 = R_SHROUD + 0.62
        r1 = CAVITY_HALF / max(abs(n[0]), abs(n[2])) - 0.02
        L = r1 - 0.12 - r0
        if raised:
            hx, hz = C + n[0] * r1, C + n[2] * r1
            b.obox((hx - n[0] * 0.08, y + L / 2, hz - n[2] * 0.08), t, (0, 1, 0), hw, L / 2, 0.06, "paint_grey")
            continue
        mid = (C + n[0] * (r0 + L / 2), C + n[2] * (r0 + L / 2))
        # deck (grating) on two side channels
        b.obox((mid[0], y - 0.03, mid[1]), n, (0, 1, 0), L / 2, 0.03, hw, ["grating", "grating", "grating", "grating", "grating", "grating"])
        for s in (-1, 1):
            e = hw + 0.03
            sx, sz = mid[0] + t[0] * e * s, mid[1] + t[2] * e * s
            b.obox((sx, y - 0.1, sz), n, (0, 1, 0), L / 2, 0.12, 0.03, "paint_grey")
            # handrail posts and rails
            for k in range(4):
                rr = r0 + 0.12 + k * (L - 0.24) / 3
                px, pz = C + n[0] * rr + t[0] * e * s, C + n[2] * rr + t[2] * e * s
                b.cyl((px, y, pz), (px, y + 1.07, pz), 0.022, "paint_yellow", "paint_yellow", 6)
            for hy, rad in ((1.07, 0.026), (0.55, 0.02)):
                pa = (C + n[0] * (r0 + 0.08) + t[0] * e * s, y + hy, C + n[2] * (r0 + 0.08) + t[2] * e * s)
                pb = (C + n[0] * (r1 - 0.2) + t[0] * e * s, y + hy, C + n[2] * (r1 - 0.2) + t[2] * e * s)
                b.cyl(pa, pb, rad, "paint_yellow", "paint_yellow", 6)
        # the tray on the deck, full of cables, continuing the panel's trough
        b.obox((mid[0], y + 0.06, mid[1]), n, (0, 1, 0), L / 2, 0.06, 0.29, "gunmetal")
        b.obox((mid[0], y + 0.125, mid[1]), n, (0, 1, 0), L / 2 - 0.01, 0.005, 0.27, "rubber")
        # hinge bracket and wall sleeve
        wx, wz = C + n[0] * r1, C + n[2] * r1
        b.obox((wx - n[0] * 0.06, y - 0.15, wz - n[2] * 0.06), n, (0, 1, 0), 0.06, 0.2, hw + 0.05, "gunmetal")
        if i == 0:
            _access_ladder(b, a, y)
            # the fan control box on the deck's left, outside the tray, facing across the deck
            rr = r1 - 1.1
            foot = (C + n[0] * rr - t[0] * 0.62, C + n[2] * rr - t[2] * 0.62)
            fan_box(b, foot, t, (-n[0], 0, -n[2]), y)
        b.obox((wx - n[0] * 0.076, y + 0.25, wz - n[2] * 0.076), n, (0, 1, 0), 0.07, 0.19, 0.36, "carbon_steel")


def _access_ladder(b, a, y):
    """A fixed ladder on bridge A's deck up to the walkway: two rails, rungs every 0.3, the rails carried 1.07 above the
    walkway as handholds, two brackets back to the shield plate's edge."""
    n = (math.cos(a), 0, math.sin(a))
    t = (-math.sin(a), 0, math.cos(a))
    top = SHIELD[1] + 1.07
    rails = []
    for tt in LADDER_T:
        p = (C + n[0] * LADDER_R + t[0] * tt, C + n[2] * LADDER_R + t[2] * tt)
        rails.append(p)
        b.cyl((p[0], y, p[1]), (p[0], top, p[1]), 0.03, "paint_yellow", "paint_yellow", 8)
    k = 1
    while y + 0.3 * k < SHIELD[1] + 0.05:
        yy = y + 0.3 * k
        b.cyl((rails[0][0], yy, rails[0][1]), (rails[1][0], yy, rails[1][1]), 0.018, "gunmetal", "gunmetal", 6)
        k += 1
    for p in rails:
        for yy in (SHIELD[0] - 0.4, SHIELD[1] - 0.05):
            q = P(R_SHROUD + 0.16, math.atan2(p[1] - C, p[0] - C), yy)
            b.cyl((p[0], yy, p[1]), q, 0.02, "gunmetal", "gunmetal", 6)


def seismic_restraints(b, released=False):
    """Three struts from the top ring beam to the cavity walls (pins at both ends, a turnbuckle in the middle)."""
    yt = SHROUD[3]
    for i, a in enumerate((math.radians(0), math.radians(120), math.radians(240))):
        n = (math.cos(a), 0, math.sin(a))
        p0 = P(R_SHROUD + 0.12, a, yt - 0.15)
        # out to the wall (the cavity is square)
        far = CAVITY_HALF / max(abs(n[0]), abs(n[2])) - 0.05
        p1 = (C + n[0] * far, yt - 0.15, C + n[2] * far)
        if released:
            p1 = (C + n[0] * (R_SHROUD + 0.9), yt - 0.15 - 0.6, C + n[2] * (R_SHROUD + 0.9))
        b.cyl(p0, p1, 0.07, "paint_yellow", "paint_yellow", 10)
        mid = tuple((p0[k] + p1[k]) / 2 for k in range(3))
        b.cyl(tuple(mid[k] - n[k] * 0.2 for k in range(3)), tuple(mid[k] + n[k] * 0.2 for k in range(3)), 0.1, "gunmetal", "gunmetal", 8)
        # clevis at the shroud end
        b.obox(P(R_SHROUD + 0.2, a, yt - 0.15), n, (0, 1, 0), 0.1, 0.1, 0.06, "gunmetal")
        if not released:
            b.obox((p1[0] - n[0] * 0.04, yt - 0.15, p1[2] - n[2] * 0.04), n, (0, 1, 0), 0.04, 0.25, 0.2, "gunmetal")


def _lift_rig(b):
    """Lift rods from the three lugs (clevises on the head) up past the shroud to the tripod; tripod legs to the apex,
    the shackle the crane hook takes."""
    yt = SHROUD[3]
    top_y = PLENUM[1] + 0.55
    for i, a in enumerate(LUGS):
        lug = P(R_SHROUD + 0.42, a, 13.25)
        n = (math.cos(a), 0, math.sin(a))
        t = (-math.sin(a), 0, math.cos(a))      # right, as seen from outside
        # the lug on the head (through the ring beam's gap) and the rod's clevis
        b.obox(P(R_SHROUD + 0.3, a, 12.95), n, (0, 1, 0), 0.3, 0.32, 0.06, "carbon_steel")
        b.cyl((lug[0] - t[0] * 0.12, lug[1], lug[2] - t[2] * 0.12), (lug[0] + t[0] * 0.12, lug[1], lug[2] + t[2] * 0.12), 0.05, "gunmetal",
              "gunmetal", 8)
        b.obox((lug[0], lug[1] + 0.08, lug[2]), n, (0, 1, 0), 0.1, 0.16, 0.1, "paint_yellow")
        rod_top = P(R_SHROUD + 0.42, a, top_y)
        b.cyl((lug[0], lug[1] + 0.2, lug[2]), rod_top, 0.07, "paint_yellow", "paint_yellow", 10)
        # rod guides on the shroud's tie heights
        for y in (SHROUD[1], SHROUD[2], yt - 0.15):
            g = P(R_SHROUD + 0.27, a, y)
            b.obox(g, n, (0, 1, 0), 0.15, 0.05, 0.05, "gunmetal")
        b.tag("LIFT LUG %d" % (i + 1), P(R_SHROUD + 0.61, a, 12.95), n)
        # tripod leg from the rod top to the apex
        b.cyl(rod_top, (C, TRIPOD_APEX - 0.2, C), 0.075, "paint_yellow", "paint_yellow", 10)
        b.obox(rod_top, n, (0, 1, 0), 0.12, 0.1, 0.12, "gunmetal")
    # the tripod's spreader ring at the rod tops, the apex block and the shackle
    for k in range(3):
        p, q = P(R_SHROUD + 0.42, LUGS[k], top_y), P(R_SHROUD + 0.42, LUGS[(k + 1) % 3], top_y)
        b.cyl(p, q, 0.05, "paint_yellow", "paint_yellow", 8)
    b.box(C - 0.2, TRIPOD_APEX - 0.35, C - 0.2, C + 0.2, TRIPOD_APEX, C + 0.2, "paint_yellow")
    b.obox((C, TRIPOD_APEX + 0.2, C), (1, 0, 0), (0, 1, 0), 0.03, 0.18, 0.12, "gunmetal")
    b.cyl((C - 0.03, TRIPOD_APEX + 0.33, C - 0.14), (C - 0.03, TRIPOD_APEX + 0.33, C + 0.14), 0.04, "gunmetal", "gunmetal", 8)


def _crdm_positions():
    """53 drives on a square pitch (real four-loop pattern: the 53 control/shutdown bank locations of the 193)."""
    pts = []
    p = 0.3
    for i in range(-6, 7):
        for j in range(-6, 7):
            x, z = i * p, j * p
            if math.hypot(x, z) < 1.92 and (i + j) % 2 == 0:
                pts.append((x, z))
    pts.sort(key=lambda q: (math.hypot(*q), q))
    return pts[:53]


def _crdms(b):
    """Inside the shroud: each drive's pressure housing, latch housing, operating coil stack, rod position indicator
    coil stack and seismic cap plate; the seismic support panel at the top."""
    base_y = DOME_TOP - 0.15
    top = SHROUD[3] - 0.4
    for x, z in _crdm_positions():
        X, Z = C + x, C + z
        r = math.hypot(x, z)
        yb = HEAD_FLANGE[1] + 1.3 * math.sqrt(max(0.0, 1 - (r / 2.25) ** 2)) - 0.05
        b.cyl((X, yb, Z), (X, top, Z), 0.05, "stainless", "stainless", 8)
        b.cyl((X, yb + 0.5, Z), (X, yb + 1.3, Z), 0.11, "crdm", "gunmetal", 10)          # latch housing / coils
        b.cyl((X, yb + 1.9, Z), (X, top - 0.5, Z), 0.085, "copper", "gunmetal", 10)      # RPI coil stack
        b.box(X - 0.14, top - 0.06, Z - 0.14, X + 0.14, top, Z + 0.14, "gunmetal")     # seismic cap plate
    # the seismic support panel round the caps
    b.band(2.05, top - 0.06, 2.05, top, "gunmetal")


def build(state="running"):
    """The piece's operations. state: running (connected, fans on) or lifted (restraints let go, bridge up, cables off;
    the package drawn where it stands: the client lifts it with the crane)."""
    b = Head()
    vessel(b)
    seal_ring(b)
    b.core = len(b.still)
    closure_head(b)
    head_package(b)
    cable_bridge(b, raised=(state != "running"))
    seismic_restraints(b, released=(state != "running"))
    return b


def ops(state="running"):
    return build(state).ops()


def write_legends(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    legend_image().save(path, optimize=True)


if __name__ == "__main__":
    out = Path(__file__).resolve().parents[1] / "build" / "reactor_head"
    write_legends(out / "rv_legends.png")
    b = build()
    o = b.ops()
    print("still", len(o["still"]), "lit", len(o["lit"]), "overlaps", len(CB.overlaps(o)))
