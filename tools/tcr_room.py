"""The turbine building's control room (turbine hall part 3): the turbine-generator's own electronics and its local
operator's station, under the operating floor at the hall's east end. Built as the diesel rooms' panels
(tools/diesel_panels.py): each piece a cabinet two blocks high, its front drawn at 512 px a block, every part that
stands out of it a real box, what moves the game's (client/LivePanelRenderer.java, live_panels.json).

Along the south wall, east to west:
- tb_tcs_power: the turbine control system's power distribution (its two DC feeds' meters, the feed and ground fault
  lamps, through a window its breakers and power supplies);
- tb_tcs_control: the control system's three redundant controllers R, S and T behind the door's window (each one's
  status display and lamps live), its network switches under them;
- tb_tcs_io (I/O CAB 1, I/O CAB 2): the I/O cabinets, their terminal boards and I/O packs (each pack's lamps live)
  either side of the wireway;
- tb_tsi_rack: the turbine supervisory instruments, the bearing vibration bar graph in the door (live), the monitor rack
  through its window (its modules' lamps live);
- tb_excitation (two wide): the generator's excitation, the regulator's display and lamps, field amps and volts, its
  switches; the thyristor bridges and the field breaker behind the windows of its power section;
- tb_gen_protection (SYSTEM A, SYSTEM B): the generator's two protection systems, each two multifunction relays (their
  displays and target lamps live), their test switches, the 86G lockout relay.

The turbine control console (tools/cr_room.py cr_console, its new TURBINE CONTROL position: three screens, TURBINE GEN,
TSI, EHC, TURBINE OIL) stands in the middle facing the line-up.

Frames as the panels': x right as you face it, y up, z out of the wall toward you (model pixels; the front "south")."""
import math

import plant_panels as PP
from plant_panels import Front, INK, _sc_round
import diesel_panels as DP
from diesel_panels import D, BLK, _fit

WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
GREEN, AMBER, RED, LWHITE = (60, 230, 90), (240, 170, 30), (230, 50, 40), (240, 240, 230)
GREEN_L = (40, 180, 70)                       # a pilot lamp's green (diesel_panels.LENSES)
SCREEN_BG, SCREEN_TEXT = (10, 16, 24, 255), (236, 240, 246)
BACK, FRONT_Z = DP.CARD_Z                     # behind a window: the back plate and the modules' faces

POWER, CONTROL, IO, TSI, EXCITATION, PROTECTION = ("tb_tcs_power", "tb_tcs_control", "tb_tcs_io", "tb_tsi_rack", "tb_excitation",
                                                   "tb_gen_protection")
IO_LABELS = ["I/O CAB 1", "I/O CAB 2"]
GP_LABELS = ["SYSTEM A", "SYSTEM B"]


def plate(f, b, face, x0, y0, x1, y1, title, sub=None):
    """An engraved nameplate (black, white letters): the cabinet's name and its second line."""
    f.R(x0, y0, x1, y1, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    cx = (x0 + x1) / 2
    if sub:
        _fit(f, cx, y1 - 0.64, title, 10, x1 - x0 - 0.6, fill=WHITE)
        _fit(f, cx, y0 + 0.62, sub, 7.5, x1 - x0 - 0.6, fill=WHITE)
    else:
        _fit(f, cx, (y0 + y1) / 2, title, 10, x1 - x0 - 0.6, fill=WHITE)
    b.append((x0, y0, D, x1, y1, D + 0.12, face))


def bezel(b, face, x0, y0, x1, y1, rim=0.4, depth=0.4):
    """A display's black bezel out of the door, the screen (the drawing) set in it."""
    b += [(x0, y0, D, x0 + rim, y1, D + depth, BLK), (x1 - rim, y0, D, x1, y1, D + depth, BLK),
          (x0 + rim, y0, D, x1 - rim, y0 + rim, D + depth, BLK), (x0 + rim, y1 - rim, D, x1 - rim, y1, D + depth, BLK),
          (x0 + rim, y0 + rim, D, x1 - rim, y1 - rim, D + 0.1, face)]


def led(f, x, y, colour, ch, z):
    """A status light on a module (a small one: drawn lit by the game)."""
    f.E(x, y, 0.17, fill=(30, 34, 32, 255))
    f.live.append({"t": "lamp", "x": x, "y": y, "r": 0.13, "z": z, "colour": list(colour), "ch": ch})


def back_plate(f, b, F, win):
    """What a window shows first: the cabinet's mounting plate behind it, its rails top and bottom."""
    wx0, wy0, wx1, wy1 = win
    f.R(wx0, wy0, wx1, wy1, fill=(186, 188, 184, 255))
    b.append((wx0, wy0, BACK - 0.3, wx1, wy1, BACK, {"*": "pi_cabinet_dark", "south": F}))


# ------------------------------------------------------------------------------------------------ TCS power

PW_METERS = [(1.6, "DC FEED A", (0, 150), "VDC", "tcp_dc_a"), (8.8, "DC FEED B", (0, 150), "VDC", "tcp_dc_b")]
PW_METER = (5.6, 20.0, 24.6)                  # a meter's width, bottom, top
PW_LAMPS = [(2.6, "FEED A", GREEN_L, "tcp_a"), (5.9, "FEED B", GREEN_L, "tcp_b"), (9.2, "120V UPS", GREEN_L, "tcp_ups"),
            (12.5, "GND FAULT", AMBER, "tcp_gnd")]
PW_LAMP_Y = 17.2
PW_WINDOW = (1.6, 4.4, 14.4, 14.2)
PW_BREAKERS = 10


def tcs_power(name=POWER):
    f = Front(1, 931)
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, windows=[PW_WINDOW])
    DP.door_seam(f)
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "TCS POWER DISTRIBUTION", "125 VDC  A / B    120 VAC UPS")
    w, y0, y1 = PW_METER
    for x0, label, scale, unit, ch in PW_METERS:
        DP.meter(f, b, face, x0, y0, x0 + w, y1, label, scale, unit, ch)
    for x, label, colour, ch in PW_LAMPS:
        DP.lamp(f, b, x, PW_LAMP_Y, colour, ch, label)
    # Through the window: the DIN rail of feeder breakers (each its handle up, its number), the two power supplies.
    wx0, wy0, wx1, wy1 = PW_WINDOW
    back_plate(f, b, F, PW_WINDOW)
    ry = 10.6
    f.R(wx0 + 0.3, ry - 0.3, wx1 - 0.3, ry + 0.3, fill=(170, 172, 176, 255))
    b.append((wx0 + 0.3, ry - 0.3, BACK, wx1 - 0.3, ry + 0.3, BACK + 0.25, "pi_chrome"))
    pitch = (wx1 - wx0 - 0.8) / PW_BREAKERS
    for k in range(PW_BREAKERS):
        x0 = wx0 + 0.4 + k * pitch + 0.04
        x1 = x0 + pitch - 0.08
        f.R(x0, ry - 1.5, x1, ry + 1.5, fill=(232, 232, 226, 255))
        f.T((x0 + x1) / 2, ry - 1.0, str(k + 1), 5.5, fill=INK, bold=True)
        f.R(x0 + 0.15, ry + 0.95, x1 - 0.15, ry + 1.35, fill=(40, 160, 70, 255))
        b += [(x0, ry - 1.5, BACK + 0.25, x1, ry + 1.5, FRONT_Z - 0.6, {"*": "pi_pnl_white", "south": F}),
              (x0 + 0.25, ry + 0.1, FRONT_Z - 0.6, x1 - 0.25, ry + 0.75, FRONT_Z, BLK)]
    for k, x0 in enumerate((wx0 + 0.5, wx0 + 6.6)):
        x1 = x0 + 5.6
        f.R(x0, 5.0, x1, 8.4, fill=(70, 74, 80, 255))
        f.T((x0 + x1) / 2, 7.6, f"PS {'AB'[k]}  24 VDC", 6, fill=WHITE, bold=True)
        for j in range(4):
            f.E(x0 + 0.8 + j * 1.2, 6.0, 0.28, fill=(60, 62, 66, 255))
        led(f, x1 - 0.6, 5.8, GREEN, f"tcp_ps{k}", FRONT_Z - 0.4 + 0.02)
        b.append((x0, 5.0, BACK, x1, 8.4, FRONT_Z - 0.4, {"*": "pi_cabinet_dark", "south": F}))
    return f, b


# ------------------------------------------------------------------------------------------------ TCS controllers

CT_WINDOW = (1.6, 12.4, 14.4, 26.6)
NET_WINDOW = (1.6, 4.4, 14.4, 10.8)
CONTROLLERS = ["R", "S", "T"]
CT_LEDS = [("ONLINE", GREEN), ("CONTROL", GREEN), ("DIAG", AMBER), ("FAULT", RED)]


def tcs_control(name=CONTROL):
    f = Front(1, 932)
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, windows=[NET_WINDOW, CT_WINDOW])
    DP.door_seam(f)
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "TURBINE CONTROL SYSTEM", "TMR CONTROLLERS  R  S  T")
    wx0, wy0, wx1, wy1 = CT_WINDOW
    back_plate(f, b, F, CT_WINDOW)
    b += [(wx0 + 0.05, wy0 + 0.25, BACK, wx1 - 0.05, wy0 + 0.55, FRONT_Z - 0.4, "pi_chrome"),
          (wx0 + 0.05, wy1 - 0.55, BACK, wx1 - 0.05, wy1 - 0.25, FRONT_Z - 0.4, "pi_chrome")]
    pitch = (wx1 - wx0 - 0.6) / 3
    for i, n in enumerate(CONTROLLERS):
        x0 = wx0 + 0.3 + i * pitch + 0.12
        x1 = x0 + pitch - 0.24
        y0, y1 = wy0 + 0.7, wy1 - 0.7
        fz = FRONT_Z
        f.R(x0, y0, x1, y1, fill=(52, 56, 64, 255))
        f.R(x0 + 0.2, y1 - 1.1, x1 - 0.2, y1 - 0.3, fill=(222, 222, 216, 255))
        _fit(f, (x0 + x1) / 2, y1 - 0.7, f"CONTROLLER {n}", 6.5, x1 - x0 - 0.5)
        dx0, dy0, dx1, dy1 = x0 + 0.3, y1 - 3.5, x1 - 0.3, y1 - 1.5
        f.R(dx0, dy0, dx1, dy1, fill=(12, 30, 20, 255))
        f.text(dx0 + 0.2, (dy0 + dy1) / 2 + 0.42, fz + 0.02, 0.4, (110, 240, 140), f"tcc_{n.lower()}0")
        f.text(dx0 + 0.2, (dy0 + dy1) / 2 - 0.42, fz + 0.02, 0.4, (110, 240, 140), f"tcc_{n.lower()}1")
        for k, (label, colour) in enumerate(CT_LEDS):
            y = dy0 - 0.85 - k * 0.9
            led(f, x0 + 0.6, y, colour, f"tcc_{n.lower()}_l{k}", fz + 0.02)
            f.T(x0 + 1.05, y, label, 5.5, fill=WHITE, bold=True, anchor="lm")
        # Its two network ports (the cables out of them, yellow and blue, down out of sight), its serial port.
        for k, rgb in enumerate(((236, 196, 30), (40, 110, 200))):
            px = x0 + 0.5 + k * 1.5
            f.R(px, y0 + 0.5, px + 1.0, y0 + 1.3, fill=(20, 20, 22, 255))
            b += [(px + 0.1, y0 + 0.6, fz, px + 0.9, y0 + 1.2, fz + 0.5, "pi_pnl_yellow" if k == 0 else "pi_shop_blue"),
                  (px + 0.3, wy0 + 0.6, fz + 0.2, px + 0.7, y0 + 0.6, fz + 0.5, "pi_pnl_yellow" if k == 0 else "pi_shop_blue")]
        f.R(x1 - 1.3, y0 + 0.5, x1 - 0.4, y0 + 1.3, fill=(70, 72, 76, 255))
        b.append((x0, y0, BACK, x1, y1, fz, {"*": "pi_sb_frame", "south": F}))
        b += [(x0 + 0.1, y1, fz - 0.4, x1 - 0.1, y1 + 0.35, fz + 0.3, BLK), (x0 + 0.1, y0 - 0.35, fz - 0.4, x1 - 0.1, y0, fz + 0.3, BLK)]
    # The network switches under them (two, a row of ports each, their link lights live) and the fibre patch panel.
    wx0, wy0, wx1, wy1 = NET_WINDOW
    back_plate(f, b, F, NET_WINDOW)
    for r, y in enumerate((8.2, 6.0)):
        f.R(wx0 + 0.4, y - 0.9, wx1 - 0.4, y + 0.9, fill=(46, 50, 56, 255))
        f.T(wx0 + 0.7, y + 0.5, f"NET {'AB'[r]}", 5.5, fill=WHITE, bold=True, anchor="lm")
        for k in range(8):
            px = wx0 + 3.0 + k * 1.3
            f.R(px, y - 0.55, px + 0.9, y + 0.15, fill=(16, 16, 18, 255))
            led(f, px + 0.45, y + 0.5, GREEN, f"tcn_{r}_{k}", FRONT_Z - 0.6 + 0.02)
        b.append((wx0 + 0.4, y - 0.9, BACK, wx1 - 0.4, y + 0.9, FRONT_Z - 0.6, {"*": "pi_cabinet_dark", "south": F}))
    return f, b


# ------------------------------------------------------------------------------------------------ TCS I/O

IO_WINDOW = (1.6, 4.0, 14.4, 26.8)
IO_COLS = ((2.1, 6.9), (9.1, 13.9))           # the terminal boards' columns (x)
IO_DUCT = (7.3, 8.7)                          # the wireway between them
IO_ROWS = 5
IO_PACKS = [["DI", "AI", "TC", "SVO", "SPD"], ["DO", "AO", "RTD", "TRP", "PWR"]]
IO_PACKS2 = [["DI", "DI", "AI", "AI", "DO"], ["DO", "RLY", "RLY", "AI", "PWR"]]


def tcs_io(label=IO_LABELS[0], name=IO):
    f = Front(1, 933 + IO_LABELS.index(label))
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, windows=[IO_WINDOW])
    DP.door_seam(f)
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "TURBINE CONTROL SYSTEM", label)
    wx0, wy0, wx1, wy1 = IO_WINDOW
    back_plate(f, b, F, IO_WINDOW)
    packs = IO_PACKS if label == IO_LABELS[0] else IO_PACKS2
    # The wireway (slotted grey duct with its cover) down the middle.
    d0, d1 = IO_DUCT
    f.R(d0, wy0 + 0.2, d1, wy1 - 0.2, fill=(150, 152, 150, 255))
    for k in range(int((wy1 - wy0) / 0.8)):
        f.R(d0, wy0 + 0.4 + k * 0.8, d0 + 0.25, wy0 + 0.6 + k * 0.8, fill=(110, 112, 110, 255))
        f.R(d1 - 0.25, wy0 + 0.4 + k * 0.8, d1, wy0 + 0.6 + k * 0.8, fill=(110, 112, 110, 255))
    b.append((d0, wy0 + 0.2, BACK, d1, wy1 - 0.2, FRONT_Z - 0.2, {"*": "pi_cabinet", "south": F}))
    h = (wy1 - wy0 - 0.4) / IO_ROWS
    for c, (x0, x1) in enumerate(IO_COLS):
        for r in range(IO_ROWS):
            y1 = wy1 - 0.2 - r * h - 0.15
            y0 = y1 - h + 0.3
            # The terminal board (its terminal strip along the side away from the duct, the screws), the I/O pack on it
            # (its name, three lights: power, attention, network).
            f.R(x0, y0, x1, y1, fill=(60, 120, 80, 255))
            tx = (x0 + 0.15, x0 + 1.05) if c == 0 else (x1 - 1.05, x1 - 0.15)
            f.R(tx[0], y0 + 0.15, tx[1], y1 - 0.15, fill=(222, 222, 214, 255))
            for k in range(int((y1 - y0 - 0.3) / 0.42)):
                f.E((tx[0] + tx[1]) / 2, y0 + 0.36 + k * 0.42, 0.11, fill=(120, 122, 126, 255))
            b.append((x0, y0, BACK, x1, y1, BACK + 0.3, {"*": "pi_cabinet_dark", "south": F}))
            b.append((tx[0], y0 + 0.15, BACK + 0.3, tx[1], y1 - 0.15, BACK + 0.9, {"*": "pi_pnl_white", "south": F}))
            px0, px1 = (x0 + 1.4, x1 - 0.3) if c == 0 else (x0 + 0.3, x1 - 1.4)
            py0, py1 = y0 + 0.5, y1 - 0.5
            f.R(px0, py0, px1, py1, fill=(64, 66, 72, 255))
            f.R(px0 + 0.2, py1 - 0.9, px1 - 0.2, py1 - 0.2, fill=(222, 222, 216, 255))
            _fit(f, (px0 + px1) / 2, py1 - 0.55, packs[c][r], 6, px1 - px0 - 0.5)
            for k, colour in enumerate((GREEN, RED, AMBER)):
                led(f, px0 + 0.55 + k * 0.85, py0 + 0.6, colour, f"tio_{c}{r}_{k}", FRONT_Z - 0.3 + 0.02)
            b.append((px0, py0, BACK + 0.3, px1, py1, FRONT_Z - 0.3, {"*": "pi_sb_frame", "south": F}))
            # The field wires from the terminal strip into the duct (a bundle along the board's edge toward it).
            ex = x1 if c == 0 else x0
            f.R(min(ex, d0 if c == 0 else d1), (y0 + y1) / 2 - 0.12, max(ex, d0 if c == 0 else d1), (y0 + y1) / 2 + 0.12, fill=(40, 70, 160, 255))
    return f, b


# ------------------------------------------------------------------------------------------------ TSI

TSI_SCREEN = (1.4, 16.0, 14.6, 26.8)
TSI_WINDOW = (1.6, 4.4, 14.4, 14.6)
TSI_BARS = [("1", "tsi_b1"), ("2", "tsi_b2"), ("3", "tsi_b3"), ("4", "tsi_b4"), ("5", "tsi_b5"), ("6", "tsi_b6"), ("7", "tsi_b7"),
            ("8", "tsi_b8"), ("TH", "tsi_thrust"), ("EC", "tsi_ecc")]
TSI_ALERT, TSI_DANGER = 0.5, 0.7              # of full scale (10 mils): ALERT 5, DANGER 7
TSI_CARDS = ["PS1", "PS2", "RIM", "KPH", "1-4", "5-8", "THR", "DE", "ECC", "RLY"]


def tsi_rack(name=TSI):
    f = Front(1, 934)
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, windows=[TSI_WINDOW])
    DP.door_seam(f)
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "TURBINE SUPERVISORY", "INSTRUMENTATION   TSI")
    x0, y0, x1, y1 = TSI_SCREEN
    f.R(x0, y0, x1, y1, fill=(26, 28, 30, 255))
    sx0, sy0, sx1, sy1 = x0 + 0.4, y0 + 0.4, x1 - 0.4, y1 - 0.4
    f.R(sx0, sy0, sx1, sy1, fill=SCREEN_BG)
    f.R(sx0, sy1 - 1.0, sx1, sy1, fill=(30, 60, 110, 255))
    f.T(sx0 + 0.3, sy1 - 0.5, "VIBRATION  MILS P-P", 6, fill=WHITE, bold=True, anchor="lm")
    # (Its screen's face stands out to the bezel's front once tools/zfight.py has moved it: D + 0.4. The live parts in front.)
    lz = D + 0.46
    f.text(sx1 - 0.3, sy1 - 0.5, lz, 0.5, SCREEN_TEXT, "tsi_rpm", "right")
    by0, by1 = sy0 + 1.0, sy1 - 1.5
    n = len(TSI_BARS)
    pitch = (sx1 - sx0 - 0.6) / n
    for k, (label, ch) in enumerate(TSI_BARS):
        bx0 = sx0 + 0.3 + k * pitch + 0.12
        bx1 = bx0 + pitch - 0.24
        f.R(bx0, by0, bx1, by1, fill=(24, 34, 46, 255))
        f.live.append({"t": "bar", "x0": round(bx0, 4), "y0": round(by0, 3), "x1": round(bx1, 4), "y1": round(by1, 3), "z": lz, "lo": 0.0,
                       "hi": 1.0, "warn": TSI_ALERT, "colour": [255, 190, 60], "colour2": [90, 220, 120], "ch": ch})
        # (client/LivePanelRenderer.java draws a bar in its second colour below its warn level: green under ALERT.)
        f.T((bx0 + bx1) / 2, sy0 + 0.5, label, 5.5, fill=(170, 180, 196, 255), bold=True)
    for frac, rgb in ((TSI_ALERT, (240, 170, 30, 255)), (TSI_DANGER, (230, 50, 40, 255))):
        y = by0 + frac * (by1 - by0)
        f.R(sx0 + 0.3, y - 0.04, sx1 - 0.3, y + 0.04, fill=rgb)
    bezel(b, face, x0, y0, x1, y1)
    DP.card_rack(f, b, face, TSI_WINDOW, TSI_CARDS, name)
    return f, b


# ------------------------------------------------------------------------------------------------ excitation

EX_SCREEN = (1.6, 19.8, 14.4, 26.8)
EX_LINES = 5
EX_LAMPS = [(2.6, "CH A", GREEN_L, "exc_cha"), (5.3, "CH B", LWHITE, "exc_chb"), (8.0, "PSS", LWHITE, "exc_pss"),
            (10.7, "41 CLOSED", RED, "exc_41"), (13.4, "ALARM", AMBER, "exc_alarm")]
EX_LAMP_Y = 18.0
EX_METERS = [(1.6, "FIELD AMPS", (0, 6), "KA", "exc_fa"), (8.8, "FIELD VOLTS", (0, 600), "V", "exc_fv")]
EX_METER = (5.6, 9.8, 14.4)
EX_POWER = (17.6, 11.4, 30.4, 25.0)          # the power section's window onto the thyristor bridges
EX_BREAKER = (17.6, 4.4, 30.4, 10.0)          # its window onto the field breaker


def excitation(name=EXCITATION):
    f = Front(2, 935)
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, width=2, windows=[EX_BREAKER, EX_POWER])
    DP.door_seam(f, 16)
    f.R(16.6, 1.4, 31.4, 30.2, outline=(130, 132, 128, 255), width=3)
    # (The power section's own door: its hinges, its handle.)
    for y in (4.0, 26.0):
        b.append((16.15, y, 14.5, 16.6, y + 2.0, 15.25, "pi_chrome"))
    hx = 14.1
    b += [(hx, 12.4, D, hx + 1.0, 17.4, D + 0.15, BLK), (hx + 0.3, 13.0, D + 0.15, hx + 0.7, 16.6, D + 0.5, "pi_chrome")]
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "GENERATOR EXCITATION", "VOLTAGE REGULATOR  CH A / B")
    x0, y0, x1, y1 = EX_SCREEN
    f.R(x0, y0, x1, y1, fill=(26, 28, 30, 255))
    sx0, sy0, sx1, sy1 = x0 + 0.4, y0 + 0.4, x1 - 0.4, y1 - 0.4
    f.R(sx0, sy0, sx1, sy1, fill=SCREEN_BG)
    f.R(sx0, sy1 - 1.0, sx1, sy1, fill=(30, 60, 110, 255))
    f.text(sx0 + 0.3, sy1 - 0.5, D + 0.16, 0.5, SCREEN_TEXT, "exc_t")
    for i in range(EX_LINES):
        y = sy1 - 1.65 - i * 0.98
        f.text(sx0 + 0.3, y, D + 0.16, 0.5, (168, 176, 188), f"exc_l{i}")
        f.text(sx1 - 0.3, y, D + 0.16, 0.5, SCREEN_TEXT, f"exc_r{i}", "right")
    bezel(b, face, x0, y0, x1, y1)
    for x, label, colour, ch in EX_LAMPS:
        DP.lamp(f, b, x, EX_LAMP_Y, colour, ch, label, label_size=6)
    w, my0, my1 = EX_METER
    for mx0, label, scale, unit, ch in EX_METERS:
        DP.meter(f, b, face, mx0, my0, mx0 + w, my1, label, scale, unit, ch)
    for cx, positions, nm in ((3.2, ["AUTO", "MAN"], "REGULATOR"), (8.0, ["LOWER", "", "RAISE"], "VOLT ADJUST"), (12.8, ["A", "B"], "CHANNEL")):
        DP.selector_plate(f, b, face, cx, 5.6, 3.6, 3.0, positions, nm, arc=40, radius=0.95, size=6, drop=0.2, mark=0.75)
        b += DP.turned(DP.wing_knob(cx, 5.6, D + 0.12, 0.7), cx, 5.6, 40 if nm == "REGULATOR" else -40 if nm == "CHANNEL" else 0)
    DP.louvre(b, 1.8, 14.2)
    # The power section: its warning label; the three thyristor bridges through the upper window (each a stack of
    # finned heat sinks, the thyristors' discs clamped between them, a fuse with its blown-fuse flag); through the lower
    # window the field breaker 41 (its closing spring and the OPEN / CLOSED flag live).
    f.R(18.4, 26.0, 29.6, 29.4, fill=(250, 250, 246, 255), outline=(20, 20, 22, 255), width=2)
    f.R(18.6, 28.0, 29.4, 29.2, fill=(200, 30, 28, 255))
    f.T(24.0, 28.6, "DANGER", 9, fill=WHITE, bold=True)
    _fit(f, 24.0, 27.35, "HIGH VOLTAGE  700 VDC", 7, 10.6, fill=INK)
    _fit(f, 24.0, 26.55, "FIELD CIRCUIT", 6, 10.6, fill=INK)
    b.append((18.4, 26.0, D, 29.6, 29.4, D + 0.1, face))
    wx0, wy0, wx1, wy1 = EX_POWER
    back_plate(f, b, F, EX_POWER)
    pitch = (wx1 - wx0 - 0.6) / 3
    for i in range(3):
        x0 = wx0 + 0.3 + i * pitch + 0.25
        x1 = x0 + pitch - 0.5
        cx = (x0 + x1) / 2
        f.R(x0, wy1 - 1.4, x1, wy1 - 0.5, fill=(222, 222, 216, 255))
        _fit(f, cx, wy1 - 0.95, f"BRIDGE {i + 1}", 6, x1 - x0 - 0.3)
        # The heat sink stack: its base, the fins standing out toward the window, the thyristor discs (dark) at three
        # heights, the pulse transformer card above.
        b.append((x0, wy0 + 1.6, BACK, x1, wy1 - 2.0, BACK + 0.6, "pi_cr_alum"))
        for k in range(7):
            fx = x0 + 0.2 + k * (x1 - x0 - 0.4) / 6
            b.append((fx - 0.08, wy0 + 1.8, BACK + 0.6, fx + 0.08, wy1 - 2.2, FRONT_Z - 0.6, "pi_cr_alum"))
        for y in (wy0 + 3.4, wy0 + 6.4, wy0 + 9.4):
            b.append((x0 - 0.15, y - 0.45, BACK + 0.6, x1 + 0.15, y + 0.45, FRONT_Z - 0.4, BLK))
        f.R(x0, wy0 + 0.3, x1, wy0 + 1.4, fill=(232, 230, 220, 255))
        f.R(x0 + 0.4, wy0 + 0.6, x0 + 1.0, wy0 + 1.1, fill=(40, 40, 42, 255))
        b.append((x0, wy0 + 0.3, BACK, x1, wy0 + 1.4, BACK + 0.9, {"*": "pi_pnl_white", "south": F}))
    wx0, wy0, wx1, wy1 = EX_BREAKER
    back_plate(f, b, F, EX_BREAKER)
    f.R(wx0 + 1.2, wy0 + 0.3, wx1 - 1.2, wy1 - 0.3, fill=(34, 36, 40, 255))
    f.T(24.0, wy1 - 1.35, "FIELD BREAKER 41", 7, fill=WHITE, bold=True)
    f.T(22.2, wy0 + 0.95, "POSITION", 5.5, fill=WHITE, bold=True)
    f.T(25.8, wy0 + 0.95, "SPRING", 5.5, fill=WHITE, bold=True)
    f.R(21.0, wy0 + 1.5, 23.4, wy0 + 2.7, fill=(20, 20, 22, 255))
    f.live.append({"t": "window", "x0": 21.1, "y0": wy0 + 1.6, "x1": 23.3, "y1": wy0 + 2.6, "z": FRONT_Z + 0.02, "colour": [230, 50, 40],
                   "colour2": [60, 200, 90], "ch": "exc_41_flag", "alpha": 1.0})
    f.R(24.6, wy0 + 1.5, 27.0, wy0 + 2.7, fill=(236, 196, 30, 255))
    f.T(25.8, wy0 + 2.1, "CHARGED", 5.5, fill=INK, bold=True)
    b.append((wx0 + 1.2, wy0 + 0.3, BACK, wx1 - 1.2, wy1 - 0.3, FRONT_Z, {"*": BLK, "south": F}))
    # The power section's fans on the cabinet's top.
    for fx in (19.5, 28.5):
        b += [(fx - 2.0, 31.2, 4.0, fx + 2.0, 32.0, 12.0, "pi_cabinet_dark")]
    return f, b


# ------------------------------------------------------------------------------------------------ generator protection

GP_RELAYS = [("GEN PROTECTION RELAY", ["TRIP", "87G", "40", "32", "21", "24", "46", "64G"], 26.6),
             ("XFMR DIFF / BACKUP RELAY", ["TRIP", "87T", "87U", "51N", "50BF", "78", "59N", "27TN"], 19.0)]
GP_H = 6.6                                    # a relay's face, top to bottom
GP_LOR = (8.0, 5.6)
GP_LAMPS = [(3.0, "125 VDC", LWHITE, "gp_dc"), (13.0, "TRIP CKT", LWHITE, "gp_trip_ckt")]


def gen_protection(label=GP_LABELS[0], name=PROTECTION):
    f = Front(1, 937 + GP_LABELS.index(label))
    F = f"pi_{name}_f"
    face = {"*": BLK, "south": F}
    b = DP.shell(F, door=False)
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    plate(f, b, face, 1.6, 27.75, 14.4, 30.0, "GENERATOR PROTECTION", label)
    for r, (title, targets, top) in enumerate(GP_RELAYS):
        x0, x1, y1 = 1.0, 15.0, top
        y0 = y1 - GP_H
        # The relay's face: its name, the display (live, two lines), the targets (two columns, each its lamp), the
        # TARGET RESET button and the four navigation keys; the test switch under it.
        f.R(x0, y0, x1, y1, fill=(58, 62, 70, 255))
        f.R(x0 + 0.3, y1 - 0.95, x1 - 0.3, y1 - 0.25, fill=(222, 222, 216, 255))
        _fit(f, (x0 + x1) / 2, y1 - 0.6, title, 6.5, x1 - x0 - 0.8)
        lx0, ly0, lx1, ly1 = x0 + 0.5, y1 - 3.4, x0 + 7.2, y1 - 1.3
        f.R(lx0, ly0, lx1, ly1, fill=(140, 170, 90, 255))
        f.text(lx0 + 0.25, (ly0 + ly1) / 2 + 0.45, D + 0.62, 0.55, (24, 30, 20), f"gp_r{r}0")
        f.text(lx0 + 0.25, (ly0 + ly1) / 2 - 0.45, D + 0.62, 0.55, (24, 30, 20), f"gp_r{r}1")
        for k, name_ in enumerate(targets):
            c, row = k // 4, k % 4
            tx = x0 + 8.0 + c * 3.3
            ty = y1 - 1.6 - row * 0.95
            led(f, tx, ty, RED, f"gp_t{r}{k}", D + 0.62)
            f.T(tx + 0.4, ty, name_, 5.5, fill=WHITE, bold=True, anchor="lm")
        f.T(x0 + 1.4, y0 + 1.6, "TARGET", 5.5, fill=WHITE, bold=True)
        f.T(x0 + 1.4, y0 + 1.1, "RESET", 5.5, fill=WHITE, bold=True)
        b.append((x0, y0, D, x1, y1, D + 0.6, {"*": BLK, "south": F}))
        b += _sc_round(x0 + 3.2, y0 + 1.35, D + 0.6, D + 0.9, 0.48, "pi_pnl_white")
        for k in range(4):
            kx = x0 + 5.6 + k * 1.3
            b.append((kx - 0.45, y0 + 1.0, D + 0.6, kx + 0.45, y0 + 1.7, D + 0.8, "pi_pnl_white"))
        # The test switch: its black base, its nameplate, ten levers (the trip circuits' red).
        ty0, ty1 = y0 - 1.05, y0 - 0.15
        f.R(1.1, ty0, 14.9, ty1, fill=(36, 38, 42, 255))
        f.R(1.2, ty0 + 0.12, 3.0, ty1 - 0.12, fill=(236, 236, 230, 255))
        _fit(f, 2.1, (ty0 + ty1) / 2, f"TS-{r + 1}", 5.5, 1.6)
        b.append((1.1, ty0, D, 14.9, ty1, D + 0.35, face))
        for k in range(10):
            xs = 3.6 + k * 1.1
            b += [(xs - 0.1, ty0 + 0.15, D + 0.35, xs + 0.1, ty1 - 0.15, D + 0.5, "pi_chrome"),
                  (xs - 0.17, ty1 - 0.5, D + 0.5, xs + 0.17, ty1 - 0.12, D + 0.85, "pi_pnl_red" if k in (0, 9) else BLK)]
    # The 86G lockout relay: its plate (RESET, TRIP), the handle (live), its target; the 125 VDC and TRIP CKT lamps.
    lx, ly = GP_LOR
    f.R(lx - 1.9, ly - 2.05, lx + 1.9, ly + 1.95, fill=BLACK)
    f.E(lx, ly, 1.25, fill=(222, 222, 216, 255))
    f.E(lx, ly, 0.85, fill=(200, 200, 196, 255))
    for a, col in ((135, INK), (45, (190, 30, 26, 255))):
        aa = math.radians(a)
        f.line([(lx + math.cos(aa) * 0.9, ly + math.sin(aa) * 0.9), (lx + math.cos(aa) * 1.22, ly + math.sin(aa) * 1.22)], col, 4)
    f.T(lx - 1.1, ly + 1.55, "RESET", 6, fill=WHITE, bold=True)
    f.T(lx + 1.2, ly + 1.55, "TRIP", 6, fill=(240, 90, 70, 255), bold=True)
    _fit(f, lx, ly - 1.72, f"86G-{label[-1]} LOCKOUT", 6.5, 3.6, fill=WHITE)
    f.R(lx + 1.3, ly - 1.22, lx + 1.84, ly - 0.84, fill=(20, 20, 22, 255))
    f.glow(lx + 1.34, ly - 1.18, lx + 1.8, ly - 0.88, D + 0.13, DP.ORANGE, "gp_lor_target", alpha=1.0)
    b.append((lx - 1.9, ly - 2.05, D, lx + 1.9, ly + 1.95, D + 0.12, face))
    b += [bx[:6] + ({"*": "pi_chrome", "south": F},) for bx in _sc_round(lx, ly, D + 0.12, D + 0.3, 1.25, None)]
    DP.part(f, "gp_lor", lx, ly, DP.pistol_grip(lx, ly, D + 0.3, 1.15), angles=[45, -45])
    for x, lbl, colour, ch in GP_LAMPS:
        DP.lamp(f, b, x, ly + 0.4, colour, ch, lbl)
    return f, b


# ------------------------------------------------------------------------------------------------ registering

PANELS = {POWER: {"display": "TCS Power Distribution Cabinet", "width": 1},
          CONTROL: {"display": "Turbine Control System Controller Cabinet", "width": 1},
          IO: {"display": "Turbine Control System I/O Cabinet", "width": 1, "labels": IO_LABELS},
          TSI: {"display": "Turbine Supervisory Instrument Rack", "width": 1},
          EXCITATION: {"display": "Generator Excitation Cabinet", "width": 2},
          PROTECTION: {"display": "Generator Protection Panel", "width": 1, "labels": GP_LABELS}}
BUILDERS = {POWER: tcs_power, CONTROL: tcs_control, IO: tcs_io, TSI: tsi_rack, EXCITATION: excitation, PROTECTION: gen_protection}
_P = "#c:plates/steel"
CRAFTING = {
    POWER: (["PRP", "PCP", "PPP"], {"P": _P, "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, 1),
    CONTROL: (["PGP", "PCP", "PRP"], {"P": _P, "G": "minecraft:glass_pane", "C": "minecraft:comparator", "R": "minecraft:redstone"}, 1),
    IO: (["PGP", "PRP", "PCP"], {"P": _P, "G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, 2),
    TSI: (["PGP", "PQP", "PRP"], {"P": _P, "G": "minecraft:glass_pane", "Q": "minecraft:quartz", "R": "minecraft:redstone"}, 1),
    EXCITATION: (["PGP", "CRC", "PPP"], {"P": _P, "G": "minecraft:glass_pane", "C": "minecraft:copper_ingot", "R": "minecraft:redstone_block"}, 1),
    PROTECTION: (["PRP", "PCP", "PLP"], {"P": _P, "R": "minecraft:redstone", "C": "minecraft:comparator", "L": "minecraft:lever"}, 2),
}
_BUILT = {}


def built(key, label=None):
    labels = PANELS[key].get("labels")
    label = label or (labels[0] if labels else None)
    if (key, label) not in _BUILT:
        _BUILT[(key, label)] = BUILDERS[key](label) if labels else BUILDERS[key]()
    return _BUILT[(key, label)]


def register():
    for key, info in PANELS.items():
        info.update({"multi": (info["width"], 2, "panel"), "panel": key, "item3d": True})
        PP.PANELS[key] = info
        PP.DRAWINGS[key] = lambda label=None, key=key: built(key, label)[0]
        PP.CABINETS[key] = lambda key=key: built(key)[1]
    DP.BUILDERS.update({k: (lambda k=k: built(k)) for k in BUILDERS})
    PP.CRAFTING.update(CRAFTING)
    for key, info in PANELS.items():
        for v in range(len(info.get("labels", [None]))):
            PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for p in range(2 * info["width"])}


register()
