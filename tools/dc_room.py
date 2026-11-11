"""The control building's DC equipment rooms brought up to the detail standard (older rooms round 3; Justin, 15 October
2026: the switchboard "Real 3D, breakers work", working "Charger switches", "Inverter bypass switch", "Annunciator
buttons", "Log desk laptop"; the room "Lights and safety", "One-line and signs", "Desk, phone, drawings", "Mats and
work gear"; the east wall "DC distribution panels").

- The 125 VDC distribution switchboard (dc_switchboard, two sections): its meters in black cases, their faces set
  back behind glass; the ground detector's lamps in chrome bezels; the main breaker 72-1A through its door's cut-out;
  twelve feeder breakers on the second section's dead front, each with its handle (live: up ON, down OFF) and its
  engraved tag. A click on a handle switches it (nuclear/DcSwitchboard.java); what a feeder feeds goes dead with it.
- The 125 VDC distribution panels (dc_dist_panel): built as the vital panels (plant_panels vital_panel, VP_TEXTS),
  fed from the switchboard's feeders 03 and 04.
- The log desk's laptop (dc_desk_laptop): the room's battery, chargers, inverters, switchboard and panels, live.
- The room's fittings: the framed 125 VDC one-line diagram, the train signs, the DANGER sign, the battery service
  board (hydrometer, thermometer, gloves, face shield, apron), the battery test cart.

Model pixels for the piece facing north (its back to the north wall, z 0; its front out at +z), x across to the right
as you face it, y up from the floor.
"""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR

U = PP.U
D = PP.SC_DOOR                  # the doors' and dead front's face
BLK = "pi_pnl_black"
GLASS = PP.SC_GLASS
WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)

# ------------------------------------------------------------------------------------------------ the switchboard

SB_KEY = "dc_switchboard"
SB_METER = (6.0, 22.0, 27.2)                       # a meter's width, bottom, top (as the chargers')
SB_METERS = [(1.4, "BUS VOLTS", (0, 150), "V", "dc_v"), (8.6, "BATTERY AMPS", (0, 200), "A", "dc_a")]
SB_GD = (1.4, 16.4, 14.6, 20.8)                    # the ground detector's plate
SB_LAMP_Y = 18.1
SB_LAMPS = [(3.4, "+ BUS GND", (240, 240, 230), "gnd_lamp", "pi_lens_white"),
            (8.0, "BATT DISCH", (240, 170, 30), "sb_disch", "pi_lens_amber"),
            (12.6, "- BUS GND", (240, 240, 230), "gnd_lamp", "pi_lens_white")]
SB_CUT = (2.6, 4.9, 13.4, 14.9)                    # the main breaker's cut-out in the door
SB_MAIN = (3.4, 5.6, 12.6, 14.2)                   # the main breaker's moulded front
SB_MAIN_X, SB_MAIN_Y = 8.0, 10.6                   # its handle's slot (middle)
SB_FEEDER_X = (20.0, 27.6)                         # the feeders' two columns (their handles' x)
SB_FEEDER_Y = [26.0 - 3.55 * k for k in range(6)]  # their rows (a breaker's handle slot's foot)
SB_BODY = (2.3, -0.6, 1.3)                         # a feeder breaker's body: half width, foot and head from its row


def sb_feeders(label):
    """The switchboard's twelve feeders (72-<label>-01 .. 12): odd in the left column, top down."""
    t = label[-1]
    return [f"INVERTER {label}-1", f"INVERTER {label}-2", f"DC DIST PNL {label}-1", f"DC DIST PNL {label}-2",
            f"480V LC {label} CONTROL", f"4160V SWGR {label} CTL", f"EDG {label} CONTROL",
            "TURB AUX OIL PUMP" if t == "A" else "MD AFW PUMP CTL", f"EMERG LIGHTING {label}",
            "AFW TURB CONTROL" if t == "A" else "DIESEL FUEL XFER", f"COMPUTER UPS {label}", "SPARE"]


def sb_feeder(k):
    """Feeder k (0..11): its handle's middle (x, the slot's foot y)."""
    return SB_FEEDER_X[k % 2], SB_FEEDER_Y[k // 2]


def sb_handles():
    """Each breaker's handle, ON and OFF (x0, y0, z0, x1, y1, z1): the main first, then the feeders."""
    out = [((SB_MAIN_X - 0.7, SB_MAIN_Y + 0.3, D + 0.85, SB_MAIN_X + 0.7, SB_MAIN_Y + 1.8, D + 1.75),
            (SB_MAIN_X - 0.7, SB_MAIN_Y - 1.8, D + 0.85, SB_MAIN_X + 0.7, SB_MAIN_Y - 0.3, D + 1.75))]
    for k in range(12):
        x, y = sb_feeder(k)
        out.append(((x - 0.45, y + 0.55, D + 0.6, x + 0.45, y + 1.15, D + 1.15), (x - 0.45, y - 0.45, D + 0.6, x + 0.45, y + 0.15, D + 1.15)))
    return out


def sb_hits():
    """Where a click works a breaker (x0, y0, x1, y1 on the front, model pixels): the main, then the feeders
    (nuclear/DcSwitchboard.java HITS)."""
    out = [(SB_MAIN[0], SB_MAIN[1], SB_MAIN[2], SB_MAIN[3])]
    hw, lo, hi = SB_BODY
    for k in range(12):
        x, y = sb_feeder(k)
        out.append((x - hw, y + lo, x + hw, y + hi))
    return out


def _label_danger(f, x0, y0, x1, y1, line1, line2):
    """A DANGER label: its black head with the red oval, two lines under it."""
    f.R(x0, y0, x1, y1, fill=(250, 250, 246, 255), outline=(20, 20, 22, 255), width=2)
    f.R(x0 + 0.1, y1 - 0.95, x1 - 0.1, y1 - 0.1, fill=(20, 20, 22, 255))
    f.E((x0 + x1) / 2 - 1.0, y1 - 0.52, 0.32, fill=(206, 30, 32, 255))
    f.R((x0 + x1) / 2 - 1.0, y1 - 0.84, (x0 + x1) / 2 + 1.0, y1 - 0.2, fill=(206, 30, 32, 255))
    f.E((x0 + x1) / 2 + 1.0, y1 - 0.52, 0.32, fill=(206, 30, 32, 255))
    f.T((x0 + x1) / 2, y1 - 0.52, "DANGER", 7, fill=(250, 250, 246, 255), bold=True)
    RR._fit(f, (x0 + x1) / 2, y0 + 1.15, line1, 7.5, x1 - x0 - 0.4)
    RR._fit(f, (x0 + x1) / 2, y0 + 0.5, line2, 6, x1 - x0 - 0.4)


def _label_arc(f, x0, y0, x1, y1, lines):
    """An arc flash label (NFPA 70E): its orange WARNING head, the lines under it."""
    f.R(x0, y0, x1, y1, fill=(250, 250, 246, 255), outline=(20, 20, 22, 255), width=2)
    f.R(x0 + 0.1, y1 - 0.85, x1 - 0.1, y1 - 0.1, fill=(246, 130, 30, 255))
    f.T((x0 + x1) / 2, y1 - 0.47, "WARNING", 7, fill=(20, 20, 22, 255), bold=True)
    for k, s in enumerate(lines):
        RR._fit(f, (x0 + x1) / 2, y1 - 1.25 - k * 0.5, s, 6, x1 - x0 - 0.4)


def switchboard(label="1A"):
    """The switchboard's front, drawn (the cases, glass, lamps, breakers and handles are real: switchboard_boxes)."""
    f = PP.Front(2, 591)
    T = f.T
    t = label[-1]
    # The first section's door (its seam, the gasket line) and the second's dead front (its edge, the screws).
    f.R(0.6, 1.4, 15.8, 30.2, outline=(130, 132, 128, 255), width=3)
    f.R(16.2, 1.4, 31.4, 30.2, outline=(130, 132, 128, 255), width=3)
    for x in (16.8, 30.8):
        for y in (2.0, 16.0, 29.6):
            f.E(x, y, 0.16, fill=(150, 152, 148, 255))
            f.line([(x - 0.1, y), (x + 0.1, y)], (60, 60, 62, 255), 1)
    # The nameplates.
    for x0, x1, a, b in ((2.0, 14.0, f"125 VDC SWITCHBOARD {label}", f"SECTION 1  INCOMING  CLASS 1E TRAIN {t}"),
                         (18.0, 30.0, f"SWITCHBOARD {label}  SECTION 2", f"FEEDERS 72-{label}-01 TO 12   250 VDC")):
        f.R(x0, 28.3, x1, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
        RR._fit(f, (x0 + x1) / 2, 29.4, a, 11, x1 - x0 - 0.4, fill=WHITE)
        RR._fit(f, (x0 + x1) / 2, 28.75, b, 7, x1 - x0 - 0.4, fill=WHITE)
    # The meters' faces (the case and glass are real), their needles live.
    w, y0, y1 = SB_METER
    for x0, name, scale, unit, ch in SB_METERS:
        f.meter(x0, y0, x0 + w, y1, name, scale, 0, unit, live=ch)
        f.live[-1]["z"] = D + 0.3
    # The ground detector's plate, its lamps' legends.
    gx0, gy0, gx1, gy1 = SB_GD
    f.R(gx0, gy0, gx1, gy1, fill=(52, 54, 58, 255), outline=(150, 152, 148, 255), width=2)
    T((gx0 + gx1) / 2, gy1 - 0.6, "GROUND DETECTOR", 7, fill=WHITE, bold=True)
    for x, name, colour, ch, _ in SB_LAMPS:
        # (Lit at the lens's face: the lens stands D + 0.12 out, on the plate.)
        RR._live_lamp(f, x, SB_LAMP_Y, 0.5, colour, ch, D=D + 0.12)
        T(x, SB_LAMP_Y - 1.2, name, 6, fill=WHITE, bold=True)
    # The main breaker's engraved tag over its cut-out, its moulded front: the rating, ON over the handle, OFF under it.
    f.tag(8.0, 15.65, f"72-{label}  MAIN  FROM BATTERY {label}", size=7, w=10.4)
    cx0, cy0, cx1, cy1 = SB_CUT
    f.R(cx0, cy0, cx1, cy1, fill=(70, 72, 76, 255))
    mx0, my0, mx1, my1 = SB_MAIN
    f.R(mx0, my0, mx1, my1, fill=(46, 48, 52, 255), outline=(20, 20, 22, 255), width=2)
    f.R(SB_MAIN_X - 0.9, SB_MAIN_Y - 2.0, SB_MAIN_X + 0.9, SB_MAIN_Y + 2.0, fill=(26, 26, 28, 255))
    T(SB_MAIN_X, SB_MAIN_Y + 2.45, "ON", 8, fill=WHITE, bold=True)
    T(SB_MAIN_X, SB_MAIN_Y - 2.45, "OFF", 8, fill=WHITE, bold=True)
    T(4.9, 13.4, "DC", 7, fill=WHITE, bold=True)
    for k, s in enumerate(("1200 A", "250 VDC", "2 POLE", "65 kAIC")):
        T(4.9, 12.4 - k * 0.75, s, 6, fill=(206, 208, 210, 255), bold=True)
    T(11.1, 12.9, "PUSH", 6, fill=WHITE, bold=True)
    T(11.1, 12.35, "TO TRIP", 6, fill=WHITE, bold=True)
    f.E(11.1, 11.3, 0.45, fill=(200, 40, 34, 255))
    for k, s in enumerate(("TRIP UNIT", "LT 1000 A", "ST 6X", "INST 10X")):
        T(11.1, 9.6 - k * 0.7, s, 6, fill=(206, 208, 210, 255), bold=True)
    # The feeders: each breaker's moulded front (ON and OFF by its handle, its rating), its engraved tag under it.
    hw, lo, hi = SB_BODY
    for k, name in enumerate(sb_feeders(label)):
        x, y = sb_feeder(k)
        f.R(x - hw, y + lo, x + hw, y + hi, fill=(46, 48, 52, 255), outline=(20, 20, 22, 255), width=2)
        f.R(x - 0.55, y - 0.5, x + 0.55, y + 1.2, fill=(26, 26, 28, 255))
        T(x - 1.2, y + 0.85, "ON", 6, fill=WHITE, bold=True)
        T(x - 1.2, y - 0.2, "OFF", 6, fill=WHITE, bold=True)
        T(x + 1.45, y + 0.85, "100 A" if name != "SPARE" else "50 A", 6, fill=(206, 208, 210, 255), bold=True)
        T(x + 1.45, y - 0.2, "250 V", 6, fill=(206, 208, 210, 255), bold=True)
        f.tag(x, y - 1.2, f"{k + 1:02d}  {name}", size=6.5, w=6.8)
    # The labels at the first section's foot, the second's louvred inlet.
    _label_danger(f, 1.2, 1.9, 7.8, 4.6, "125 VOLTS DC", "AUTHORIZED PERSONNEL ONLY")
    _label_arc(f, 8.4, 1.9, 15.0, 4.6, ["ARC FLASH AND SHOCK HAZARD", "PPE CAT 1   18 IN BOUNDARY", "SWBD 1" + label[-1] + "  NFPA 70E"])
    f.R(17.6, 1.8, 30.4, 4.6, fill=(22, 22, 24, 255))
    return f


def switchboard_boxes():
    """The whole switchboard (both blocks wide, two high): plinth, body, the first section's door with its meters,
    lamps, main breaker, hinges and handle, the second's dead front with its feeders, the cap, the cables' entries."""
    F = f"pi_{SB_KEY}_f"
    face = {"*": BLK, "south": F}
    b = [(0.8, 0, 2.6, 31.2, 1.2, 14.0, "pi_toe_kick"),
         (0.4, 1.2, 1.6, 31.6, 30.4, 14.6, "pi_cabinet"),
         (0.6, 1.4, 14.6, 15.8, 30.2, D, {"*": "pi_cabinet", "south": F}),
         (16.2, 1.4, 14.6, 31.4, 30.2, D, {"*": "pi_cabinet", "south": F}),
         (0.2, 30.4, 1.4, 31.8, 31.2, 15.2, "pi_cabinet_dark")]
    # The cables: the battery's down through a gland plate on the first section's top, the chargers' and inverters'
    # through the hub on the second's (round 26, 8f: up to the tray).
    b += [(4.0, 31.2, 4.0, 12.0, 31.5, 12.0, "pi_steel_dark")]
    b += PP._top_hub(24.0, 31.2)
    # The nameplates.
    for x0, x1 in ((2.0, 14.0), (18.0, 30.0)):
        b.append((x0, 28.3, D, x1, 29.9, D + 0.12, face))
    # The meters: black cases out of the door, their faces set back inside, the glass in front, the zero adjusters.
    w, y0, y1 = SB_METER
    for x0, *_ in SB_METERS:
        b += PP._meter_case(x0, y0, x0 + w, y1, face)
    # The ground detector's plate and its lamps (a chrome bezel, the lens).
    gx0, gy0, gx1, gy1 = SB_GD
    b.append((gx0, gy0, D, gx1, gy1, D + 0.12, face))
    for x, _, _, _, lens in SB_LAMPS:
        b += RR._lamp(x, SB_LAMP_Y, lens, D=D + 0.12, r=0.5)
    # The main breaker's tag, its cut-out's frame, the breaker's front in it, the handle's slot (the handle the game's).
    b.append((2.8, 15.27, D, 13.2, 16.03, D + 0.1, face))
    cx0, cy0, cx1, cy1 = SB_CUT
    b += [(cx0, cy0, D, cx0 + 0.4, cy1, D + 0.25, "pi_cabinet_dark"), (cx1 - 0.4, cy0, D, cx1, cy1, D + 0.25, "pi_cabinet_dark"),
          (cx0 + 0.4, cy0, D, cx1 - 0.4, cy0 + 0.4, D + 0.25, "pi_cabinet_dark"), (cx0 + 0.4, cy1 - 0.4, D, cx1 - 0.4, cy1, D + 0.25, "pi_cabinet_dark")]
    mx0, my0, mx1, my1 = SB_MAIN
    b.append((mx0, my0, D - 0.4, mx1, my1, D + 0.75, face))
    b.append((SB_MAIN_X - 0.9, SB_MAIN_Y - 2.0, D + 0.75, SB_MAIN_X + 0.9, SB_MAIN_Y + 2.0, D + 0.85, face))
    b += PP._sc_round(11.1, 11.3, D + 0.75, D + 0.95, 0.45, "pi_pnl_red")
    # The door's hinges and its locking handle.
    for y in (4.0, 26.0):
        b.append((0.15, y, 14.5, 0.6, y + 2.0, 15.25, "pi_chrome"))
    b += [(14.6, 9.0, D, 15.4, 14.0, D + 0.15, BLK), (14.8, 9.6, D + 0.15, 15.2, 13.2, D + 0.5, "pi_chrome"),
          (14.85, 9.15, D + 0.15, 15.15, 9.45, D + 0.3, "pi_brass")]
    # The labels a hair out of the door.
    b += [(1.2, 1.9, D, 7.8, 4.6, D + 0.1, face), (8.4, 1.9, D, 15.0, 4.6, D + 0.1, face)]
    # The feeders: each breaker's moulded front out of the dead front, its handle's slot; its tag.
    hw, lo, hi = SB_BODY
    for k in range(12):
        x, y = sb_feeder(k)
        b.append((x - hw, y + lo, D, x + hw, y + hi, D + 0.5, face))
        b.append((x - 0.55, y - 0.5, D + 0.5, x + 0.55, y + 1.2, D + 0.6, face))
        b.append((x - 3.4, y - 1.58, D, x + 3.4, y - 0.82, D + 0.1, face))
    # The second section's louvred inlet: its frame, the slats.
    b += [(17.6, 1.6, D, 30.4, 1.8, D + 0.3, "pi_cabinet_dark"), (17.6, 4.6, D, 30.4, 4.8, D + 0.3, "pi_cabinet_dark"),
          (17.4, 1.6, D, 17.6, 4.8, D + 0.3, "pi_cabinet_dark"), (30.4, 1.6, D, 30.6, 4.8, D + 0.3, "pi_cabinet_dark")]
    for k in range(5):
        yc = 2.15 + k * 0.55
        b.append((17.6, yc - 0.06, D - 0.1, 30.4, yc + 0.06, D + 0.4, "pi_cabinet_dark",
                  {"rotation": {"angle": -22.5, "axis": "x", "origin": [24.0, yc, D + 0.15]}}))
    return b


def switchboard_front(label="1A"):
    """The drawing with its handles: live toggles (sb_0 the main, sb_1 .. sb_12 the feeders: lit, up ON)."""
    f = switchboard(label)
    for i, (on, off) in enumerate(sb_handles()):
        f.live.append({"t": "toggle", "on": [round(v, 3) for v in on], "off": [round(v, 3) for v in off],
                       "colour": [150, 152, 156], "ch": f"sb_{i}"})
    return f


# ------------------------------------------------------------------------------------------------ the distribution panels

# The DC rooms' 125 VDC distribution panels (older rooms round 3; Justin: "DC distribution panels"): built as the vital
# panels (plant_panels vital_panel, VP_TEXTS: the same enclosure, door, dead front and breakers, their handles live),
# hung on the side wall where the relay cabinets stood, each fed from its switchboard's feeder 03 or 04.
DP_KEY = "dc_dist_panel"
DP_LABELS = ["1A-1", "1A-2", "1B-1", "1B-2"]
DP_PANELS = {"1A-1": [(19, 4, "east")], "1A-2": [(19, 5, "east")], "1B-1": [(19, 22, "west")], "1B-2": [(19, 23, "west")]}


def dp_circuits(label):
    """The panel's twelve circuits: (name's two lines)."""
    t = label[1]
    if label.endswith("-1"):
        return [("MCR BOARDS", f"TRAIN {t}"), ("RX TRIP SWGR", f"CONTROL {t}"), ("SSPS LOGIC", f"TRAIN {t}"),
                ("RELAY ROOM", "CAB 1-3" if t == "A" else "CAB 7-9"), ("RELAY ROOM", "CAB 4-6" if t == "A" else "CAB 10-12"),
                ("REMOTE SHUTDOWN", f"PANEL {t}"), ("ESF LOAD", f"SEQUENCER {t}"), ("ANNUNCIATOR", f"TRAIN {t}"),
                ("CR HVAC", f"CONTROL {t}"), ("FIRE DETECTION", f"PANEL {t}"), ("SPARE", ""), ("SPARE", "")]
    return [("AFW VALVES", f"TRAIN {t}"), ("MSIV SOLENOIDS", f"TRAIN {t}"), ("PORV BLOCK", f"VALVES {t}"), ("CCW VALVES", f"TRAIN {t}"),
            ("SERVICE WATER", f"VALVES {t}"), ("CONT ISOLATION", f"VALVES {t}"), (f"EDG 1{t} FIELD", "FLASH"),
            ("TURB BLDG EMERG", "LIGHTING"), ("SECURITY", "SYSTEM"), ("PLANT PAGE", "SYSTEM"), ("SPARE", ""), ("SPARE", "")]


def dp_short(label):
    t = label[1]
    if label.endswith("-1"):
        return [f"MCR BDS {t}", f"RTS CTL {t}", f"SSPS {t}", "RLY 1-3" if t == "A" else "RLY 7-9", "RLY 4-6" if t == "A" else "RLY 10-12",
                f"RSP {t}", f"ESF SEQ {t}", f"ANN {t}", f"CR HVAC {t}", f"FIRE DET {t}", "SPARE", "SPARE"]
    return [f"AFW VLV {t}", f"MSIV SOL {t}", f"PORV BLK {t}", f"CCW VLV {t}", f"SW VLV {t}", f"CIV {t}", f"EDG FLD {t}",
            "TB EM LTG", "SECURITY", "PAGE", "SPARE", "SPARE"]


def dp_texts(label):
    t = label[1]
    feeder = "03" if label.endswith("-1") else "04"
    short = dp_short(label)
    return {"title": f"125 VDC DIST PANEL {label}", "sub": f"125 VDC 2 W  TRAIN {t}  FROM 72-1{t}-{feeder}",
            "head": f"DC PANEL {label}   TRAIN {t}", "main": "MAIN  100 A  2 POLE  250 VDC", "main_short": ("100 A 2P", "250 VDC"),
            "warn": "125 VDC  KEEP CLOSED", "circuits": dp_circuits(label), "short": short,
            "rating": ["15 A" if name == "SPARE" else "30 A" if k in (1, 6) else "20 A" for k, name in enumerate(short)]}


# ------------------------------------------------------------------------------------------------ the laptop

# The DC rooms' log desk laptop (dc_desk_laptop; Justin: "Log desk laptop"): the switchgear room's (switchgear_room
# laptop) with its own lines (client/EdgTelemetry.java dcl: the nearest switchboard's room, nuclear/DcRoom.java).
LT_KEY = "dc_desk_laptop"


def laptop_front(label=None):
    """Its screen's live lines: the title (the room's bus) and its volts; a line a piece (the battery, the chargers,
    the inverters, the main and feeders, the distribution panels): its name, its state in its colour, its amps; the
    ground detector's line at the foot."""
    f = PP.Front(1, 712)
    z = SGR.LT_Z + 0.02
    f.text(SGR.LT_X[0], SGR.LT_TITLE, z, 0.40, SGR.LT_WHITE, "dcl_t", "left")
    f.text(SGR.LT_X[2], SGR.LT_TITLE, z, 0.40, SGR.LT_WHITE, "dcl_v", "right")
    for k in range(SGR.LT_ROWS):
        ry = SGR.LT_ROW0 - k * SGR.LT_STEP
        f.text(SGR.LT_X[0], ry, z, SGR.LT_SIZE, SGR.LT_NAME, f"dcl_n{k}", "left")
        for ch, rgb in (("g", SGR.LT_GREEN), ("y", SGR.LT_AMBER), ("w", SGR.LT_WHITE)):
            f.text(SGR.LT_X[1], ry, z, SGR.LT_SIZE, rgb, f"dcl_{ch}{k}", "left")
        f.text(SGR.LT_X[2], ry, z, SGR.LT_SIZE, SGR.LT_WHITE, f"dcl_a{k}", "right")
    f.text(SGR.LT_X[0], SGR.LT_FOOT, z, SGR.LT_SIZE, SGR.LT_NAME, "dcl_f", "left")
    f.text(SGR.LT_X[2], SGR.LT_FOOT, z, SGR.LT_SIZE, SGR.LT_AMBER, "dcl_x", "right")
    return f


# ------------------------------------------------------------------------------------------------ the one-line diagram

OL_W, OL_H, OL_PX = 3, 2, 512


def _t(d, xy, s, size, fill, anchor="mm", bold=True, width=None):
    SGR._text(d, xy, s, size, fill, anchor, bold, width)


def one_line_image():
    """The 125 VDC Class 1E system's one-line diagram (512 px a block, 3 x 2): for each train its battery's two
    strings, the chargers from the MCC, the switchboard's main and bus, its twelve feeders to their loads (the inverters
    on to their vital panels, their regulating transformers' bypass from the MCC; the two distribution panels); the
    legend and the title block."""
    W, H = OL_W * OL_PX, OL_H * OL_PX
    img = Image.new("RGBA", (W, H), (250, 250, 246, 255))
    d = ImageDraw.Draw(img)
    ink, red, grey = (20, 20, 24), (180, 30, 28), (110, 112, 116)
    d.rectangle([14, 14, W - 15, H - 15], outline=ink, width=3)
    _t(d, (W / 2, 50), "UNIT 1   125 VDC CLASS 1E POWER SYSTEM   ONE LINE DIAGRAM", 34, ink)

    def brk(x, y, s=12, closed=True):
        d.rectangle([x - s, y - s, x + s, y + s], fill=(red if closed else (250, 250, 246)), outline=ink, width=3)

    def battery(x, y, name):
        for k in range(3):
            d.line([(x - 26, y + k * 14), (x + 26, y + k * 14)], fill=ink, width=5)
            d.line([(x - 14, y + 7 + k * 14), (x + 14, y + 7 + k * 14)], fill=ink, width=3)
        _t(d, (x, y + 52), name, 14, ink)

    for k, (t, colour) in enumerate((("A", (226, 116, 30)), ("B", (40, 140, 70)))):
        x0 = 44 + k * 750
        label = f"1{t}"
        # The battery's strings into the bus through the main; the chargers from the MCC.
        bx = x0 + 60
        battery(bx, 120, "STR 1")
        battery(bx + 120, 120, "STR 2")
        _t(d, (bx + 60, 90), f"BATTERY {label}  60 CELLS", 13, ink)
        d.line([(bx, 120), (bx, 104), (bx + 120, 104), (bx + 120, 120)], fill=ink, width=3)
        d.line([(bx + 60, 104), (bx + 60, 236)], fill=ink, width=4)
        brk(bx + 60, 206)
        _t(d, (bx + 80, 206), f"72-{label}  MAIN 1200 A", 14, ink, "lm")
        for j, cx in enumerate((x0 + 380, x0 + 520)):
            d.line([(cx, 92), (cx, 122)], fill=ink, width=3)
            d.line([(cx, 170), (cx, 236)], fill=ink, width=3)
            _t(d, (cx, 80), f"MCC {label} CHGR {j + 1}", 13, ink)
            d.rectangle([cx - 30, 122, cx + 30, 170], outline=ink, width=3)
            _t(d, (cx, 138), "AC/DC", 12, ink)
            _t(d, (cx, 156), f"{label}-{j + 1}", 12, ink)
            brk(cx, 206)
        _t(d, (x0 + 610, 186), "CHARGERS 150 A", 12, grey, "lm")
        # The bus, its name and the train's band under its west end.
        d.line([(x0, 236), (x0 + 692, 236)], fill=ink, width=9)
        _t(d, (x0, 262), f"125 VDC BUS {label}", 18, ink, "lm")
        _t(d, (x0, 284), f"SWITCHBOARD {label}", 13, ink, "lm", bold=False)
        d.rectangle([x0, 298, x0 + 112, 310], fill=colour)
        # The feeders: 05 .. 12 straight down to their loads; 01 .. 04 on down past them to the inverters and panels.
        fx = [x0 + 150 + 49 * i for i in range(12)]
        for i, name in enumerate(sb_feeders(label)):
            d.line([(fx[i], 236), (fx[i], 520 if i >= 4 else 640 if i == 3 else 650)], fill=ink, width=3)
            brk(fx[i], 300, s=10, closed=name != "SPARE")
            _t(d, (fx[i] + 2, 326), f"-{i + 1:02d}", 11, ink, bold=False)
            if i >= 4:
                d.polygon([(fx[i] - 8, 520), (fx[i] + 8, 520), (fx[i], 535)], fill=ink)
                for j, word in enumerate(name.split(" ")):
                    _t(d, (fx[i], 552 + j * 16), word, 11, ink, width=46)
        # The inverters, each on to its vital panel, its regulating transformer's bypass from the MCC beside it.
        for j in range(2):
            ix = x0 + 40 + j * 130
            d.line([(fx[j], 650), (ix, 650), (ix, 660)], fill=ink, width=3)
            d.rectangle([ix - 34, 660, ix + 34, 706], outline=ink, width=3)
            _t(d, (ix, 675), "DC/AC", 12, ink)
            _t(d, (ix, 692), f"INV {label}-{j + 1}", 12, ink)
            d.line([(ix, 706), (ix, 752)], fill=ink, width=3)
            d.rectangle([ix - 40, 752, ix + 40, 784], outline=ink, width=3)
            _t(d, (ix, 768), f"VITAL {label}-{j + 1}", 12, ink)
            tx = ix + 58
            d.line([(tx, 700), (tx, 712)], fill=grey, width=2)
            d.ellipse([tx - 12, 712, tx + 12, 736], outline=grey, width=3)
            d.ellipse([tx - 12, 730, tx + 12, 754], outline=grey, width=3)
            d.line([(tx, 754), (tx, 768), (ix + 40, 768)], fill=grey, width=2)
            _t(d, (tx - 8, 694), f"REG XF {j + 1}", 10, grey, "lm")
        # The distribution panels and their branch circuits.
        for j, (px, y) in enumerate(((x0 + 340, 650), (x0 + 490, 640))):
            i = j + 2
            d.line([(fx[i], y), (px, y), (px, 660)], fill=ink, width=3)
            d.rectangle([px - 54, 660, px + 54, 706], outline=ink, width=3)
            _t(d, (px, 675), "DC DIST PNL", 12, ink)
            _t(d, (px, 692), f"{label}-{j + 1}", 12, ink)
            for c in range(6):
                cx = px - 45 + c * 18
                d.line([(cx, 706), (cx, 722)], fill=ink, width=2)
        _t(d, (x0 + 420, 760), f"GROUND DETECTOR / BATTERY MONITOR {label} ON BUS {label}", 12, grey)
        _t(d, (x0 + 420, 780), f"DC EQUIPMENT ROOM {label}   BATTERY ROOM {label}", 12, grey)
    # The legend and the title block.
    lx, ly = 70, 846
    d.rectangle([lx - 10, ly - 20, lx + 560, ly + 136], outline=ink, width=2)
    _t(d, (lx, ly), "LEGEND", 16, ink, "lm")
    d.rectangle([lx, ly + 22, lx + 24, ly + 46], fill=red, outline=ink, width=2)
    _t(d, (lx + 36, ly + 34), "BREAKER, NORMALLY CLOSED", 14, ink, "lm", bold=False)
    d.rectangle([lx, ly + 58, lx + 24, ly + 82], fill=(250, 250, 246), outline=ink, width=2)
    _t(d, (lx + 36, ly + 70), "BREAKER, OPEN (SPARE)", 14, ink, "lm", bold=False)
    for k in range(2):
        d.line([(lx + 300, ly + 28 + k * 14), (lx + 340, ly + 28 + k * 14)], fill=ink, width=5)
        d.line([(lx + 310, ly + 35 + k * 14), (lx + 330, ly + 35 + k * 14)], fill=ink, width=3)
    _t(d, (lx + 352, ly + 40), "BATTERY (60 CELLS)", 14, ink, "lm", bold=False)
    d.rectangle([lx + 300, ly + 80, lx + 340, ly + 110], outline=ink, width=3)
    _t(d, (lx + 352, ly + 95), "CHARGER / INVERTER", 14, ink, "lm", bold=False)
    tx0, ty0 = W - 640, H - 170
    d.rectangle([tx0, ty0, W - 30, H - 30], outline=ink, width=3)
    d.line([(tx0, ty0 + 50), (W - 30, ty0 + 50)], fill=ink, width=2)
    _t(d, (tx0 + 300, ty0 + 25), "125 VDC CLASS 1E POWER  ONE LINE", 20, ink)
    for k, (a, b) in enumerate((("DWG", "E-0021 SH 1"), ("REV", "4"), ("DATE", "2026-03-12"), ("STATUS", "CONTROLLED COPY"))):
        _t(d, (tx0 + 20 + (k % 2) * 300, ty0 + 76 + (k // 2) * 34), f"{a}  {b}", 16, ink, "lm")
    return img


def one_line_part(c, r):
    """Its block (c, r) of the framed drawing (the switchgear room's frame: switchgear_room.one_line_part)."""
    return [(b[:6] + ({"*": "pi_sgr_frame", "south": f"pi_dc_oneline_{r * OL_W + c}!"},) if isinstance(b[6], dict) else b)
            for b in SGR.one_line_part(c, r)]


def one_line_textures():
    img = one_line_image()
    out = {}
    for p in range(OL_W * OL_H):
        c, r = p % OL_W, p // OL_W
        x0, y0 = c * OL_PX, (OL_H - 1 - r) * OL_PX
        out[f"pi_dc_oneline_{p}"] = (lambda x0=x0, y0=y0: img.crop((x0, y0, x0 + OL_PX, y0 + OL_PX)))
    return out


# ------------------------------------------------------------------------------------------------ the work gear

# The battery service board (Justin: "Mats and work gear"): a white pegboard two blocks wide on the wall at eye level,
# its header, and on its hooks what a cell's monthly checks take: the hydrometer, the digital thermometer, the rubber
# gloves, the face shield, the acid apron, the cell lifting strap; the spill neutraliser on its shelf.
SV_Y0, SV_Y1 = 1.0, 15.4                 # the board (on the block over the floor's: y 16 .. 32 of the wall)
SV_Z = 0.6                               # its face


def _hook(x, y, out):
    out += [(x - 0.12, y - 0.12, SV_Z, x + 0.12, y + 0.12, SV_Z + 0.9, "pi_chrome")]


def service_part(c, r):
    """Block c (0 left, 1 right as you face it) of the board: its share of the frame and pegboard, what hangs there."""
    # (The right block's board and rails stop inside its end strip, so only the strip meets the block's edge: nothing
    # of it coplanar there for tools/zfight.py to push out past the piece.)
    x1 = 15.4 if c else 16.0
    out = [(0.0 if c else 0.6, SV_Y0, 0.0, x1, SV_Y1, SV_Z, {"*": "pi_dc_peg_edge", "south": f"pi_dc_pegboard_{c}!"})]
    if c == 0:
        out.append((0.3, SV_Y0 - 0.3, 0.0, 0.6, SV_Y1 + 0.3, SV_Z + 0.4, "pi_dc_peg_edge"))
    else:
        out.append((15.4, SV_Y0 - 0.3, 0.0, 16.0, SV_Y1 + 0.3, SV_Z + 0.4, "pi_dc_peg_edge"))
    out.append((0.0 if c else 0.3, SV_Y1, 0.0, x1, SV_Y1 + 0.3, SV_Z + 0.4, "pi_dc_peg_edge"))
    out.append((0.0 if c else 0.3, SV_Y0 - 0.3, 0.0, x1, SV_Y0, SV_Z + 0.4, "pi_dc_peg_edge"))
    z = SV_Z
    if c == 0:
        # The hydrometer: its rubber bulb, the glass barrel (the float in it), the nozzle; on two hooks.
        _hook(3.0, 11.8, out)
        _hook(3.0, 5.4, out)
        out += [(2.55, 11.0, z + 0.9, 3.45, 13.2, z + 1.8, "pi_cap_black"),
                (2.65, 5.8, z + 0.95, 3.35, 11.0, z + 1.65, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"}),
                (2.9, 7.0, z + 1.15, 3.1, 10.2, z + 1.45, "pi_pnl_white"),
                (2.92, 4.2, z + 1.2, 3.08, 5.8, z + 1.4, "pi_cap_black")]
        # The digital thermometer (yellow), its probe.
        _hook(6.4, 11.4, out)
        out += [(5.7, 8.6, z + 0.9, 7.1, 11.2, z + 1.5, {"*": "pi_dc_yellow", "south": "pi_dc_thermo!"}),
                (6.32, 5.0, z + 1.1, 6.48, 8.6, z + 1.3, "pi_chrome")]
        # The rubber gloves, a pair (black, cuffs up), on one hook.
        _hook(10.0, 12.2, out)
        for dx in (-0.8, 0.3):
            out += [(9.6 + dx, 8.0, z + 0.8, 10.6 + dx, 12.0, z + 1.2, "pi_cap_black"),
                    (9.75 + dx, 6.4, z + 0.85, 10.45 + dx, 8.0, z + 1.15, "pi_cap_black")]
        # The cell lifting strap, coiled on its hook.
        _hook(13.8, 12.0, out)
        out += [(13.0, 7.6, z + 0.8, 14.6, 11.8, z + 1.0, "pi_dc_strap"), (13.3, 7.9, z + 1.0, 14.3, 11.5, z + 1.05, "pi_dc_peg_edge")]
    else:
        # The face shield: its headgear (black) and the clear visor under it.
        _hook(3.0, 12.4, out)
        out += [(1.6, 11.4, z + 0.8, 4.4, 12.2, z + 2.0, "pi_cap_black"),
                (1.4, 7.4, z + 1.6, 4.6, 11.4, z + 1.8, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"})]
        # The acid apron (yellow rubber) on its hook.
        _hook(8.0, 13.0, out)
        out += [(6.0, 3.6, z + 0.7, 10.0, 12.6, z + 1.0, {"*": "pi_dc_yellow", "south": "pi_dc_apron!"})]
        # The shelf with the spill neutraliser (its tub) and the distilled water jug.
        out += [(11.0, 3.0, z, 15.4, 3.3, z + 3.2, "pi_dc_peg_edge"), (11.0, 3.3, z + 2.9, 15.4, 4.4, z + 3.2, "pi_dc_peg_edge")]
        out += [(11.4, 3.3, z + 0.6, 13.0, 6.1, z + 2.6, {"*": "pi_dc_tub", "south": "pi_dc_tub_label!"}),
                (13.4, 3.3, z + 0.8, 15.0, 7.0, z + 2.4, {"*": "pi_dc_jug", "south": "pi_dc_jug_label!"}),
                (13.9, 7.0, z + 1.4, 14.5, 7.6, z + 1.9, "pi_dc_jug")]
    return out


# The battery test cart: a two-shelf steel cart on casters with its push handle, the capacity tester (load bank) on
# its top shelf with its readout and controls, its leads coiled, the clamp meter beside it; cable reels below.
def test_cart():
    out = []
    # Casters and the frame's posts, the two shelves (lipped), the push handle at its back.
    for x in (2.0, 14.0):
        for z in (3.0, 13.0):
            out += [(x - 0.6, 0.0, z - 0.6, x + 0.6, 1.2, z + 0.6, "pi_cap_black"), (x - 0.35, 1.2, z - 0.35, x + 0.35, 12.6, z + 0.35, "pi_steel_dark")]
    for y in (3.0, 11.0):
        out += [(1.4, y, 2.4, 14.6, y + 0.4, 13.6, "pi_dc_cart"), (1.4, y + 0.4, 2.4, 14.6, y + 1.0, 2.7, "pi_dc_cart"),
                (1.4, y + 0.4, 13.3, 14.6, y + 1.0, 13.6, "pi_dc_cart")]
    out += [(1.6, 12.6, 2.6, 2.4, 16.0, 3.4, "pi_steel_dark"), (13.6, 12.6, 2.6, 14.4, 16.0, 3.4, "pi_steel_dark"),
            (1.6, 16.0, 2.6, 14.4, 16.8, 3.4, "pi_cap_black")]
    # The load bank on the top shelf: its case, the front panel (readout, controls), the side vents, the leads.
    out += [(2.6, 11.4, 4.0, 11.4, 15.6, 12.4, {"*": "pi_dc_tester_side", "south": "pi_dc_tester!", "up": "pi_dc_tester_top"})]
    out += PP._sc_round(9.4, 13.2, 12.4, 12.7, 0.6, "pi_cap_black") + PP._sc_round(10.6, 14.4, 12.4, 12.65, 0.42, "pi_pnl_red")
    out += [(11.4, 11.4, 6.0, 13.2, 12.4, 10.0, "pi_pnl_red"), (11.6, 12.4, 6.4, 13.0, 12.8, 9.6, "pi_cap_black")]
    # The clamp meter.
    out += [(12.0, 11.4, 10.6, 13.8, 12.0, 13.2, {"*": "pi_dc_yellow", "up": "pi_dc_clamp!"}), (12.3, 11.4, 13.2, 13.5, 11.9, 13.8, "pi_cap_black")]
    # The cable reels on the bottom shelf.
    for x in (4.6, 10.4):
        # (Flanges 0.8 thick: round_y insets its layers 0.3 top and bottom.)
        out += PP.round_y(x, 8.0, 2.3, 3.4, 4.2, "pi_pnl_red") + PP.round_y(x, 8.0, 1.6, 4.2, 5.6, "pi_dc_reel") + \
            PP.round_y(x, 8.0, 2.3, 5.6, 6.4, "pi_pnl_red")
    return out


# ------------------------------------------------------------------------------------------------ the textures

def _img(w, h, rgb):
    return Image.new("RGBA", (w, h), tuple(rgb) + (255,))


def pegboard_tex(c):
    """The pegboard's half c (white, a hole every inch; the header across both halves, drawn whole and cut)."""
    img = _img(1024, 512, (236, 236, 230))
    d = ImageDraw.Draw(img)
    step = 32
    for y in range(16, 512, step):
        for x in range(16, 1024, step):
            d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(140, 140, 136))
    d.rectangle([40, 18, 983, 96], fill=(20, 20, 22))
    SGR._text(d, (512, 46), "BATTERY SERVICE EQUIPMENT", 40, (250, 250, 246))
    SGR._text(d, (512, 80), "ACID PPE REQUIRED:  GOGGLES OR FACE SHIELD, RUBBER GLOVES, APRON", 20, (246, 200, 40))
    return img.crop((c * 512, 0, c * 512 + 512, 512))


def thermo_tex():
    img = _img(64, 128, (240, 200, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 10, 56, 50], fill=(30, 40, 34))
    SGR._text(d, (32, 30), "77.2", 16, (140, 230, 160))
    for k in range(2):
        d.ellipse([16 + k * 22, 70, 28 + k * 22, 82], fill=(40, 40, 42))
    return img


def apron_tex():
    img = _img(128, 256, (232, 190, 40))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 118, 40], outline=(150, 120, 20), width=3)
    SGR._text(d, (64, 26), "ACID", 18, (40, 40, 42))
    return img


def tub_tex():
    img = _img(128, 128, (236, 236, 230))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 30, 127, 98], fill=(30, 110, 60))
    SGR._text(d, (64, 52), "ACID SPILL", 18, (250, 250, 246))
    SGR._text(d, (64, 78), "NEUTRALIZER", 15, (250, 250, 246))
    return img


def jug_tex():
    img = _img(128, 128, (226, 232, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 40, 118, 90], fill=(250, 250, 246), outline=(40, 80, 160), width=3)
    SGR._text(d, (64, 56), "DISTILLED", 15, (40, 80, 160))
    SGR._text(d, (64, 76), "WATER", 15, (40, 80, 160))
    return img


def tester_tex():
    """The capacity tester's front: its name, the readout, the controls' legends, the leads' posts."""
    img = _img(256, 128, (70, 74, 80))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 251, 123], outline=(30, 30, 32), width=3)
    SGR._text(d, (72, 16), "BATTERY CAPACITY TESTER", 11, (236, 236, 230), width=136)
    d.rectangle([12, 30, 130, 70], fill=(20, 30, 24))
    SGR._text(d, (71, 42), "125.8 V   0.0 A", 13, (140, 230, 160))
    SGR._text(d, (71, 60), "READY  60 CELLS", 11, (140, 230, 160))
    for k, s in enumerate(("START", "STOP", "MENU")):
        d.rectangle([14 + k * 40, 82, 46 + k * 40, 100], fill=(30, 30, 32))
        SGR._text(d, (30 + k * 40, 110), s, 10, (236, 236, 230))
    SGR._text(d, (232, 62), "E-STOP", 10, (236, 236, 230))
    SGR._text(d, (196, 106), "LOAD ADJ", 10, (236, 236, 230))
    return img


def clamp_tex():
    img = _img(64, 128, (240, 200, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 40, 54, 70], fill=(30, 40, 34))
    SGR._text(d, (32, 55), "62.4", 14, (140, 230, 160))
    d.ellipse([22, 84, 42, 104], fill=(40, 40, 42))
    return img


def sign_tex(kind):
    """The rooms' signs: each train's row sign at the row's end, the DANGER sign by the switchboard."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    if kind.startswith("sign_dc_train_"):
        t = kind[-1].upper()
        band = {"A": (226, 116, 30), "B": (40, 140, 70)}[t]
        d.rectangle([0, 0, W - 1, H - 1], fill=(24, 24, 26))
        d.rectangle([14, 14, W - 15, H - 15], outline=(236, 236, 230), width=6)
        SGR._text(d, (W / 2, 110), "125 VDC", 96, (250, 250, 246))
        SGR._text(d, (W / 2, 210), f"TRAIN {t}", 72, (250, 250, 246))
        d.rectangle([30, 270, W - 31, 360], fill=band)
        SGR._text(d, (W / 2, 315), "CLASS 1E", 48, (20, 20, 22) if t == "A" else (250, 250, 246), width=W - 80)
        SGR._text(d, (W / 2, 410), f"DC EQUIPMENT ROOM 1{t}", 30, (236, 236, 230), width=W - 60)
        SGR._text(d, (W / 2, 455), f"BATTERY 1{t}  SWITCHBOARD 1{t}", 24, (200, 200, 196), width=W - 60)
    else:
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246), outline=(20, 20, 22), width=8)
        d.rectangle([8, 8, W - 9, 150], fill=(20, 20, 22))
        d.rounded_rectangle([60, 30, W - 61, 128], radius=48, fill=(206, 30, 32))
        SGR._text(d, (W / 2, 80), "DANGER", 68, (250, 250, 246))
        for k, s in enumerate(("125 VOLTS DC", "ARC FLASH HAZARD", "BATTERIES CANNOT", "BE SWITCHED OFF")):
            SGR._text(d, (W / 2, 200 + k * 76), s, 50 if k == 0 else 40, (20, 20, 22), width=W - 50)
    return img


SIGNS = {"sign_dc_train_a": "125 VDC Train A Sign", "sign_dc_train_b": "125 VDC Train B Sign", "sign_dc_danger": "125 VDC Danger Sign"}


def makers():
    solid = PP._solid
    out = {
        "pi_dc_pegboard_0": lambda: pegboard_tex(0), "pi_dc_pegboard_1": lambda: pegboard_tex(1),
        "pi_dc_peg_edge": lambda: solid((214, 214, 208), 760, 0.03), "pi_dc_glass": lambda: _glass(),
        "pi_dc_yellow": lambda: solid((236, 196, 36), 761, 0.03), "pi_dc_thermo": thermo_tex, "pi_dc_strap": lambda: solid((200, 60, 40), 762, 0.04),
        "pi_dc_apron": apron_tex, "pi_dc_tub": lambda: solid((236, 236, 230), 763, 0.03), "pi_dc_tub_label": tub_tex,
        "pi_dc_jug": lambda: solid((214, 226, 232), 764, 0.02), "pi_dc_jug_label": jug_tex,
        "pi_dc_cart": lambda: solid((60, 84, 140), 765, 0.05), "pi_dc_tester": tester_tex, "pi_dc_tester_side": lambda: solid((70, 74, 80), 766, 0.04),
        "pi_dc_tester_top": lambda: solid((80, 84, 90), 767, 0.04), "pi_dc_clamp": clamp_tex, "pi_dc_reel": lambda: solid((30, 30, 32), 768, 0.04),
    }
    return {k: SGR._square(v) for k, v in out.items()}


def _glass():
    img = _img(16, 16, (200, 220, 230))
    img.putalpha(110)
    return img


SHARP = {"pi_dc_pegboard_0", "pi_dc_pegboard_1", "pi_dc_thermo", "pi_dc_apron", "pi_dc_tub_label", "pi_dc_jug_label", "pi_dc_tester",
         "pi_dc_clamp"}
TRANSLUCENT = {"pi_dc_glass"}


# ------------------------------------------------------------------------------------------------ registering

PANELS = {
    DP_KEY: {"display": "125 VDC Distribution Panel", "width": 1, "item3d": True, "labels": DP_LABELS, "door": True},
    LT_KEY: {"display": "DC Room Log Desk (Laptop)", "width": 1, "rows": 1, "item3d": True},
}
MULTI = {
    "dc_one_line": {"display": "125 VDC One-Line Diagram", "multi": (OL_W, OL_H, "frame"), "parts": one_line_part, "item3d": True},
    "battery_service_board": {"display": "Battery Service Board", "multi": (2, 1, "frame"), "parts": service_part, "item3d": True},
}
PIECES = {"battery_test_cart": {"display": "Battery Test Cart", "boxes": test_cart()}}
CRAFTING = {
    DP_KEY: (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:paper", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 1),
    LT_KEY: (["GCG", "SSS", "P P"], {"G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "S": "minecraft:smooth_stone_slab",
                                     "P": "#c:plates/iron"}, 1),
    "dc_one_line": (["FPF", "PIP", "FPF"], {"F": "minecraft:stick", "P": "minecraft:paper", "I": "minecraft:black_dye"}, 1),
    "battery_service_board": (["PSP", "GIG"], {"P": "minecraft:paper", "S": "minecraft:stick", "G": "minecraft:glass_bottle", "I": "minecraft:leather"}, 1),
    "battery_test_cart": (["PCP", "I I", "W W"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "I": "minecraft:iron_ingot",
                                                   "W": "minecraft:iron_nugget"}, 1),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": {"a": "minecraft:orange_dye", "b": "minecraft:green_dye", "r": "minecraft:red_dye"}[
        "r" if k == "sign_dc_danger" else k[-1]], "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.SEE_THROUGH.add("pi_dc_glass")
    PP.VP_TEXTS[DP_KEY] = dp_texts
    PP.DRAWINGS[DP_KEY] = lambda label="1A-1": PP.vital_panel(label, DP_KEY)
    PP.DRAWINGS[SB_KEY] = switchboard_front
    PP.CABINETS[SB_KEY] = switchboard_boxes
    PP.DRAWINGS[LT_KEY] = laptop_front
    PP.CABINETS[LT_KEY] = lambda: SGR.desk(2)
    PP.RISERS.update(PIECES)
    PP.RISERS.update(MULTI)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.EXTRA_MAKERS.append(one_line_textures)
    PP.SHARP |= SHARP | {f"pi_dc_oneline_{p}" for p in range(OL_W * OL_H)} | {f"pi_{LT_KEY}_f_{p}" for p in range(2)}
    PP.SHARP |= {f"pi_{PP.texture_key(DP_KEY, v)}_{k}_{r}" for v in range(len(DP_LABELS)) for k in "fio" for r in range(2)}
    PP.SHARP |= {f"pi_{PP.texture_key(SB_KEY, v)}_f_{p}" for v in range(2) for p in range(4)}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}      # (kept at full size so the writing reads)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
