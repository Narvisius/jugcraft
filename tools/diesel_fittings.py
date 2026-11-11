"""The diesel generator rooms' fittings (round 31 part 3; Justin: "Diesel Part 3"): the CO2 system's manual release
station and its horns and strobes, the room's exhaust fans, the monorail hoist's pendant station, the CO2 discharge
piping and its nozzles, and the CO2 and hearing protection signs. Built like the diesel panels (tools/diesel_panels.py):
a front drawn at 512 px a block, every part that stands out of it a real box, what moves the game's
(client/LivePanelRenderer.java, live_panels.json; nuclear/Co2System.java).

Justin's answers (round 31 part 3): the CO2 "Discharge that hurts" (pull the release: 30 seconds of horns and strobes,
then the room fogs, the engine shuts down and anyone inside suffocates until they get out; reset at the station), the
hoist's "Trolley moves" (the pendant drives it along the beam and the hook up and down).

Frames as the panels': x right as you face it, y up, z out of the wall toward you (model pixels; the front "south")."""
import math

import plant_panels as PP
from plant_panels import Front, INK, _sc_round, _oct_prism_z, _oct_ring_z
import diesel_panels as DP
from diesel_panels import _fit

LABELS = DP.LABELS
WHITE, BLACK, RED = (238, 238, 232, 255), (24, 24, 26, 255), (196, 36, 30, 255)
YELLOW = (236, 190, 30, 255)
BLK = "pi_pnl_black"

STATION, HORN, FAN, PENDANT = "edg_co2_station", "edg_horn_strobe", "edg_room_fan", "edg_hoist_pendant"


def screws(b, pts, z, tex="pi_steel"):
    for x, y in pts:
        b += _sc_round(x, y, z, z + 0.12, 0.42, tex)


def conduit_up(b, x, y0, z0=1.0, z1=2.2, top=31.4):
    """A conduit from a box's top up the wall and into it (its fitting on the box, the elbow at the wall)."""
    bend = max(y0 + 0.6, top - 1.2)
    if bend - y0 > 0.2:
        b.append((x - 0.6, y0, z0, x + 0.6, bend, z1, "pi_chrome"))
    b += [(x - 0.6, bend, 0.0, x + 0.6, top, z1, "pi_chrome"), (x - 0.85, y0, z0 - 0.25, x + 0.85, y0 + 0.6, z1 + 0.25, "pi_steel_dark"),
          (x - 0.9, bend - 0.25, 0.0, x + 0.9, top + 0.25, 0.3, "pi_steel_dark")]


# ------------------------------------------------------------------------------------------------ the release station

# The CO2 system's manual release station (on the wall by the room's door, a block and a half up): a red enclosure,
# its header, the countdown (DISCHARGE IN, seconds: live), POWER, ALARM and DISCHARGED lamps (live), the PULL TO
# RELEASE T-handle in its recess (pulled out: live), ABORT (hold it in the countdown) and RESET (after a discharge:
# the room's purge).
CS_BOX = (2.5, 6.6, 13.5, 30.0)
CS_D = 3.6                                   # its face
CS_DISPLAY = (4.6, 22.2, 11.4, 25.4)
CS_LAMPS = [(4.9, "POWER", (240, 240, 230), "co2_power"), (8.0, "ALARM", (240, 170, 30), "co2_alarm"),
            (11.1, "DISCH", (230, 50, 40), "co2_dis")]
CS_LAMP_Y = 19.3
CS_RECESS = (4.2, 11.4, 11.8, 16.0)
CS_HANDLE = (8.0, 13.7)                      # the T-handle's grip middle
CS_PULL = [0, 1.6]                           # in, pulled out (model px out of the face)
CS_ABORT, CS_RESET = (5.3, 9.0), (10.7, 9.0)


def co2_station():
    f = Front(1, 611)
    F = f"pi_{STATION}_f"
    x0, y0, x1, y1 = CS_BOX
    f.R(x0, y0, x1, y1, fill=RED)
    f.T(8.0, 29.0, "CO2 SYSTEM", 10, fill=WHITE, bold=True)
    f.T(8.0, 27.95, "MANUAL RELEASE", 7.5, fill=WHITE, bold=True)
    f.text(8.0, 26.85, CS_D + 0.14, 0.5, (250, 250, 246), "label", "center")
    dx0, dy0, dx1, dy1 = CS_DISPLAY
    f.R(dx0, dy0, dx1, dy1, fill=(30, 30, 32, 255))
    f.R(dx0 + 0.35, dy0 + 0.35, dx1 - 0.35, dy1 - 0.35, fill=(28, 6, 6, 255))
    f.T(dx1 - 0.6, dy0 + 0.95, "SEC", 6, fill=(240, 90, 70, 255), bold=True, anchor="rm")
    f.text(dx1 - 2.0, (dy0 + dy1) / 2 + 0.15, CS_D + 0.2, 1.5, (255, 70, 50), "co2_count", "right")
    _fit(f, 8.0, dy0 - 0.62, "DISCHARGE IN", 6.5, 7.0, fill=WHITE)
    for x, label, colour, ch in CS_LAMPS:
        f.live.append({"t": "lamp", "x": x, "y": CS_LAMP_Y, "r": 0.5, "z": CS_D + 0.52, "colour": list(colour), "ch": ch})
        _fit(f, x, CS_LAMP_Y - 1.25, label, 6, 2.9, fill=WHITE)
    rx0, ry0, rx1, ry1 = CS_RECESS
    _fit(f, 8.0, ry1 + 0.62, "PULL TO RELEASE", 7, 8.6, fill=WHITE)
    f.R(rx0, ry0, rx1, ry1, fill=(58, 12, 10, 255), outline=(250, 250, 246, 255), width=3)
    for (x, y), legend, sub in ((CS_ABORT, "ABORT", "HOLD"), (CS_RESET, "RESET", None)):
        _fit(f, x, y - 1.55, legend, 6.5, 4.0, fill=WHITE)
        if sub:
            _fit(f, x, y - 2.15, sub, 5.5, 4.0, fill=WHITE)
    # The T-handle (live: pulled out).
    hx, hy = CS_HANDLE
    grip = [(hx - 2.6, hy - 0.5, CS_D + 0.6, hx + 2.6, hy + 0.5, CS_D + 1.4, "pi_pnl_white"),
            (hx - 0.4, hy - 0.4, CS_D - 0.3, hx + 0.4, hy + 0.4, CS_D + 0.6, "pi_chrome")]
    DP.part(f, "co2_pull", hx, hy, grip, push=CS_PULL)
    return f


def co2_station_boxes():
    F = f"pi_{STATION}_f"
    x0, y0, x1, y1 = CS_BOX
    face = {"*": "pi_pnl_red", "south": F}
    b = [(x0, y0, 0.2, x1, y1, CS_D, face), (x0 + 0.4, y1, 0.4, x1 - 0.4, y1 + 0.3, CS_D - 0.3, "pi_pnl_red")]
    dx0, dy0, dx1, dy1 = CS_DISPLAY
    b += [(dx0, dy0, CS_D, dx0 + 0.35, dy1, CS_D + 0.3, BLK), (dx1 - 0.35, dy0, CS_D, dx1, dy1, CS_D + 0.3, BLK),
          (dx0 + 0.35, dy0, CS_D, dx1 - 0.35, dy0 + 0.35, CS_D + 0.3, BLK), (dx0 + 0.35, dy1 - 0.35, CS_D, dx1 - 0.35, dy1, CS_D + 0.3, BLK)]
    for x, _, colour, _ in CS_LAMPS:
        b += _sc_round(x, CS_LAMP_Y, CS_D, CS_D + 0.3, 0.72, "pi_chrome") + _sc_round(x, CS_LAMP_Y, CS_D + 0.3, CS_D + 0.5, 0.5, DP.LENSES[colour])
    # The recess's lip round it (the handle sits in it).
    rx0, ry0, rx1, ry1 = CS_RECESS
    b += [(rx0, ry0, CS_D, rx1, ry0 + 0.3, CS_D + 0.25, "pi_chrome"), (rx0, ry1 - 0.3, CS_D, rx1, ry1, CS_D + 0.25, "pi_chrome"),
          (rx0, ry0 + 0.3, CS_D, rx0 + 0.3, ry1 - 0.3, CS_D + 0.25, "pi_chrome"), (rx1 - 0.3, ry0 + 0.3, CS_D, rx1, ry1 - 0.3, CS_D + 0.25, "pi_chrome")]
    # ABORT (yellow) and RESET (black): their guards, the heads.
    for (x, y), tex in ((CS_ABORT, "pi_pnl_yellow"), (CS_RESET, BLK)):
        b += _sc_round(x, y, CS_D, CS_D + 0.3, 1.05, "pi_chrome") + _sc_round(x, y, CS_D + 0.3, CS_D + 0.75, 0.8, tex)
    screws(b, ((x0 + 0.7, y0 + 0.7), (x1 - 0.7, y0 + 0.7), (x0 + 0.7, y1 - 3.6), (x1 - 0.7, y1 - 3.6)), CS_D)
    conduit_up(b, 8.0, y1 + 0.3)
    return b


# ------------------------------------------------------------------------------------------------ the horn and strobe

# A CO2 horn and strobe high on the wall (its upper block; the conduit down the lower): a red box, the horn's round
# grille, the strobe's clear lens on top (flashing in the countdown and the discharge: live), CO2 ALARM under it.
HS_BOX = (3.4, 15.0, 12.6, 29.4)
HS_D = 2.8
HS_HORN = (8.0, 20.6, 3.3)
HS_LENS = (5.2, 24.8, 10.8, 28.4)


def horn_strobe():
    f = Front(1, 613)
    x0, y0, x1, y1 = HS_BOX
    f.R(x0, y0, x1, y1, fill=RED)
    hx, hy, hr = HS_HORN
    f.E(hx, hy, hr, fill=(40, 10, 8, 255))
    for k in range(5):
        r = hr * (0.92 - 0.18 * k)
        f.E(hx, hy, r, outline=(150, 30, 26, 255), width=4)
    _fit(f, 8.0, y0 + 0.75, "CO2 ALARM", 7, x1 - x0 - 0.8, fill=WHITE)
    lx0, ly0, lx1, ly1 = HS_LENS
    f.live.append({"t": "window", "x0": lx0 + 0.3, "y0": ly0 + 0.3, "x1": lx1 - 0.3, "y1": ly1 - 0.3, "z": HS_D + 1.45, "colour": [255, 255, 250],
                   "alpha": 1.0, "ch": "co2_strobe", "label": []})
    return f


def horn_strobe_boxes():
    F = f"pi_{HORN}_f"
    x0, y0, x1, y1 = HS_BOX
    face = {"*": "pi_pnl_red", "south": F}
    b = [(x0, y0, 0.2, x1, y1, HS_D, face)]
    hx, hy, hr = HS_HORN
    # The horn's bell: a ring standing out round its grille.
    b += _oct_ring_z(hx, hy, hr - 0.1, hr + 0.35, HS_D, HS_D + 0.9, "pi_pnl_red")
    b += _oct_prism_z(hx, hy, 0.8, HS_D, HS_D + 0.5, "pi_pnl_black")
    # The strobe: its base, the clear lens box (its face the game's when it flashes).
    lx0, ly0, lx1, ly1 = HS_LENS
    b += [(lx0 - 0.3, ly0 - 0.3, HS_D, lx1 + 0.3, ly1 + 0.3, HS_D + 0.3, "pi_pnl_white"),
          (lx0, ly0, HS_D + 0.3, lx1, ly1, HS_D + 1.4, "pi_lens_white")]
    screws(b, ((x0 + 0.6, y0 + 0.6), (x1 - 0.6, y0 + 0.6)), HS_D)
    # The conduit down the wall from its bottom.
    b += [(7.4, 0.0, 1.0, 8.6, y0, 2.2, "pi_chrome"), (7.15, y0 - 0.6, 0.75, 8.85, y0, 2.45, "pi_steel_dark")]
    return b


# ------------------------------------------------------------------------------------------------ the exhaust fan

# The room's exhaust fan, two wide and two high on the wall high up over its louvre (the wall's louvre blocks behind
# it): its square housing and the octagonal shroud, the motor on its spider, the wire guard, the blades (turning while
# the engine runs and in the CO2 purge: live), its nameplate (the train lettered live).
RF_C = (16.0, 16.6)
RF_SHROUD = (12.0, 12.7)
RF_D = 5.0


def room_fan():
    f = Front(2, 617)
    f.R(1.0, 1.0, 31.0, 31.0, fill=(150, 154, 152, 255))
    f.R(9.0, 1.4, 23.0, 3.4, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    f.T(13.6, 2.4, "EXHAUST FAN", 7, fill=WHITE, bold=True)
    f.text(19.6, 2.4, RF_D + 0.14, 0.42, (238, 238, 232), "label", "center")
    cx, cy = RF_C
    f.live.append({"t": "fan", "x": cx, "y": cy, "z": 1.6, "r": 11.6, "blades": 5, "width": 4.6, "colour": [140, 146, 150], "ch": "rf_spin"})
    return f


def room_fan_boxes():
    F = f"pi_{FAN}_f"
    cx, cy = RF_C
    a_in, a_out = RF_SHROUD
    D = RF_D
    H = "pi_cabinet_dark"
    face = {"*": H, "south": F}
    # The housing: its square frame round the shroud (its face the drawing), the dark behind (the louvre).
    b = [(1.0, cy + a_out, 0.2, 31.0, 31.0, D, face), (1.0, 1.0, 0.2, 31.0, cy - a_out, D, face)]
    # Round the octagon: half-pixel strips either side, each out to the octagon's edge at its middle.
    s = a_out * math.tan(math.radians(22.5))
    n = int(round(2 * a_out / 0.5))
    for k in range(n):
        ya, yb = cy - a_out + k * 0.5, cy - a_out + (k + 1) * 0.5
        dy = abs((ya + yb) / 2 - cy)
        ext = a_out if dy <= s else a_out - (dy - s)
        b += [(1.0, ya, 0.2, cx - ext, yb, D, face), (cx + ext, ya, 0.2, 31.0, yb, D, face)]
    b += _oct_ring_z(cx, cy, a_in, a_out, 0.6, D + 0.3, "pi_steel")
    b += _oct_prism_z(cx, cy, a_in, 0.2, 0.5, BLK)
    # The motor on its spider (four arms out to the shroud), its terminal box.
    b += _oct_prism_z(cx, cy, 2.6, 2.4, 6.4, "pi_cap_black")
    b += [(cx - 1.0, cy + 2.4, 3.2, cx + 1.0, cy + 3.6, 5.0, "pi_cap_black")]
    b += [(cx - a_in, cy - 0.3, 4.0, cx - 2.6, cy + 0.3, 4.6, "pi_steel_dark"), (cx + 2.6, cy - 0.3, 4.0, cx + a_in, cy + 0.3, 4.6, "pi_steel_dark"),
          (cx - 0.3, cy - a_in, 4.0, cx + 0.3, cy - 2.6, 4.6, "pi_steel_dark"), (cx - 0.3, cy + 3.6, 4.0, cx + 0.3, cy + a_in, 4.6, "pi_steel_dark")]
    # The wire guard: four rings and the spokes.
    for r in (3.2, 6.0, 8.8, 11.6):
        b += _oct_ring_z(cx, cy, r, r + 0.22, D + 0.4, D + 0.62, "pi_chrome")
    b += [(cx - 12.0, cy - 0.11, D + 0.35, cx + 12.0, cy + 0.11, D + 0.68, "pi_chrome"),
          (cx - 0.11, cy - 12.0, D + 0.35, cx + 0.11, cy + 12.0, D + 0.68, "pi_chrome")]
    # The nameplate.
    b.append((9.0, 1.4, D, 23.0, 3.4, D + 0.12, face))
    screws(b, ((2.0, 2.0), (30.0, 2.0), (2.0, 30.0), (30.0, 30.0)), D)
    return b


# ------------------------------------------------------------------------------------------------ the hoist pendant

# The monorail hoist's pendant station on the wall (the hoist's controls, parked): a yellow pendant hung in its
# holder, the cable up the wall in its conduit; the red STOP mushroom on top, UP, DOWN, EAST and WEST under it (each
# with its arrow), the POWER lamp (live). Clicking a button drives the hoist (nuclear/Monorail.java).
HP_BODY = (4.6, 6.0, 11.4, 23.0)
HP_D = 4.2
HP_STOP = (8.0, 20.6)
HP_BUTTONS = [("UP", 17.4, "up"), ("DOWN", 14.6, "down"), ("EAST", 11.6, "east"), ("WEST", 8.8, "west")]
HP_BX = 9.4                                    # the buttons' x (their legends left of them)


def hoist_pendant():
    f = Front(1, 619)
    x0, y0, x1, y1 = HP_BODY
    f.R(x0, y0, x1, y1, fill=YELLOW)
    f.R(3.0, 25.0, 13.0, 29.6, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    f.T(8.0, 28.7, "MONORAIL HOIST", 7.5, fill=WHITE, bold=True)
    f.T(8.0, 27.6, "5 TON  PENDANT", 6, fill=WHITE, bold=True)
    f.text(8.0, 26.1, 0.36, 0.42, (238, 238, 232), "label", "center")
    for legend, y, _ in HP_BUTTONS:
        f.T(x0 + 0.5, y, legend, 6, fill=INK, bold=True, anchor="lm")
        # Its head (black) and the arrow on it: up, down, east (right as you face the wall... the beam's east), west.
        f.E(HP_BX, y, 0.85, fill=(28, 28, 30, 255))
        a = {"UP": 90, "DOWN": -90, "EAST": 0, "WEST": 180}[legend]
        pts = []
        for da, r in ((0, 0.55), (140, 0.5), (220, 0.5)):
            t = math.radians(a + da)
            pts.append((HP_BX + math.cos(t) * r, y + math.sin(t) * r))
        f.d.polygon([f.px(px, py) for px, py in pts], fill=(240, 240, 234, 255))
    # The arrows on the buttons' heads are drawn on them (their faces: the drawing).
    f.live.append({"t": "lamp", "x": 6.2, "y": 22.0, "r": 0.42, "z": HP_D + 0.5, "colour": [240, 240, 230], "ch": "hoist_power"})
    return f


def hoist_pendant_boxes():
    F = f"pi_{PENDANT}_f"
    x0, y0, x1, y1 = HP_BODY
    face = {"*": "pi_pnl_yellow", "south": F}
    b = [(x0, y0, 1.2, x1, y1, HP_D, face)]
    # The holder on the wall it hangs in: a back plate, the cradle's two arms; the strain relief and the cable up.
    b += [(3.6, 21.0, 0.0, 12.4, 25.0, 0.4, "pi_steel_dark"), (x0 - 0.4, 21.6, 0.4, x0, 23.6, HP_D - 0.6, "pi_steel_dark"),
          (x1, 21.6, 0.4, x1 + 0.4, 23.6, HP_D - 0.6, "pi_steel_dark")]
    b += [(7.2, y1, 2.0, 8.8, y1 + 1.4, 3.4, BLK), (7.5, y1 + 1.4, 2.3, 8.5, 25.0, 3.1, BLK)]
    b.append((3.0, 25.0, 0.0, 13.0, 29.6, 0.36, {"*": BLK, "south": F}))
    conduit_up(b, 8.0, 29.6, 0.4, 1.6)
    # The STOP mushroom, the buttons (black, an arrow on each), the POWER lamp.
    sx, sy = HP_STOP
    b += _sc_round(sx, sy, HP_D, HP_D + 0.3, 1.2, "pi_pnl_yellow") + _sc_round(sx, sy, HP_D + 0.3, HP_D + 1.3, 0.55, BLK)
    b += _sc_round(sx, sy, HP_D + 1.3, HP_D + 1.9, 1.3, "pi_pnl_red")
    head = {"*": BLK, "south": F}
    for _, y, _ in HP_BUTTONS:
        b += _sc_round(HP_BX, y, HP_D, HP_D + 0.25, 1.05, BLK) + _sc_round(HP_BX, y, HP_D + 0.25, HP_D + 0.6, 0.8, head)
    b += _sc_round(6.2, 22.0, HP_D, HP_D + 0.3, 0.62, "pi_chrome") + _sc_round(6.2, 22.0, HP_D + 0.3, HP_D + 0.5, 0.42, "pi_lens_white")
    return b


# ------------------------------------------------------------------------------------------------ the CO2 piping

# The CO2 discharge piping (red, 2 inch: hung under the roof a block below it, its runs at y 11): straight, with a
# hanger, a nozzle (a tee down to a nipple and its brass radial nozzle), the end (its last nozzle, capped), up the wall
# (the riser from the cylinder bank), the elbows and the tee. Laid by tools/medium_plant.py as the HVAC pipes are
# (tools/hvac_room.py connections, pieces_for).
CR = 1.0                       # the pipe's radius
CY = 11.0                      # its runs' height in the block
RED_TEX = "pi_co2_red"
OPEN = {"z": {"*": RED_TEX, "north": None, "south": None}, "x": {"*": RED_TEX, "east": None, "west": None},
        "y": {"*": RED_TEX, "up": None, "down": None}}


def _run(axis, t0, t1, c=(8.0, CY)):
    import hvac_room as HR
    if axis == "y":
        return HR.round_ax("y", 8.0, 8.0, CR, t0, t1, OPEN["y"])
    return HR.round_ax(axis, c[0], c[1], CR, t0, t1, OPEN[axis])


def _ends(*ends):
    """The couplings where a piece's run meets the next block's ((axis, 0 or 16) each): rings a little proud of the pipe,
    out to the block's face (the run stops short inside them, so its open end is never seen)."""
    import hvac_room as HR
    out = []
    for axis, t in ends:
        t0 = 0.0 if t < 8 else 15.2
        c = (8.0, 8.0) if axis == "y" else (8.0, CY)
        out += HR.round_ax(axis, c[0], c[1], CR + 0.25, t0, t0 + 0.8, RED_TEX)
    return out


def _fitting(y=CY):
    a = CR + 0.35
    return [(8 - a, y - a, 8 - a, 8 + a, y + a, 8 + a, RED_TEX)]


IN = 7.4                        # a run's end inside a fitting (its inner boxes' too)
E0, E1 = 0.2, 15.8              # a run's ends inside its couplings


def _nozzle():
    """A tee down from the run at the block's middle: the nipple, the brass radial nozzle and its deflector."""
    import hvac_room as HR
    out = _fitting() + HR.round_ax("y", 8.0, 8.0, 0.55, 7.6, CY - CR - 0.3, "pi_steel")
    out += HR.round_ax("y", 8.0, 8.0, 0.95, 6.8, 7.6, "pi_brass") + HR.round_ax("y", 8.0, 8.0, 1.5, 5.9, 6.8, "pi_brass")
    return out


def _hanger():
    """A clevis hanger round the pipe, its rod up to the roof (the block's top)."""
    y0 = CY - CR
    return [(6.9, y0 - 0.6, 7.6, 9.1, y0 - 0.25, 8.4, "pi_steel_dark"), (6.6, y0 - 0.6, 7.6, 6.95, CY + CR + 0.5, 8.4, "pi_steel_dark"),
            (9.05, y0 - 0.6, 7.6, 9.4, CY + CR + 0.5, 8.4, "pi_steel_dark"), (6.6, CY + CR + 0.5, 7.6, 9.4, CY + CR + 0.85, 8.4, "pi_steel_dark"),
            (7.7, CY + CR + 0.85, 7.7, 8.3, 15.6, 8.3, "pi_steel"), (7.2, 15.6, 7.2, 8.8, 16.0, 8.8, "pi_steel_dark")]


PIPES = {
    "co2_pipe": {"display": "CO2 Pipe", "boxes": _run("z", E0, E1) + _ends(("z", 0), ("z", 16)), "conn": "ns"},
    "co2_pipe_hanger": {"display": "CO2 Pipe (Hanger)", "boxes": _run("z", E0, E1) + _hanger() + _ends(("z", 0), ("z", 16)), "conn": "ns"},
    "co2_nozzle": {"display": "CO2 Pipe (Nozzle)", "boxes": _run("z", E0, E1) + _nozzle() + _ends(("z", 0), ("z", 16)), "conn": "ns"},
    "co2_nozzle_end": {"display": "CO2 Pipe (End Nozzle)", "boxes": _run("z", E0, 8 + CR + 0.35) + _nozzle() + _ends(("z", 0))
                       + [(8 - CR - 0.45, CY - CR - 0.45, 8 + CR + 0.35, 8 + CR + 0.45, CY + CR + 0.45, 8 + CR + 0.75, RED_TEX)], "conn": "n"},
    "co2_riser": {"display": "CO2 Pipe Riser", "boxes": _run("y", E0, E1) + _ends(("y", 0), ("y", 16)), "conn": "ud"},
    "co2_elbow": {"display": "CO2 Pipe Elbow", "boxes": _run("z", E0, IN) + _run("x", 16 - IN, E1) + _fitting() + _ends(("z", 0), ("x", 16)), "conn": "ne"},
    "co2_elbow_down": {"display": "CO2 Pipe Elbow (Down)", "boxes": _run("z", E0, IN) + _run("y", E0, CY - 0.6) + _fitting()
                       + _ends(("z", 0), ("y", 0)), "conn": "nd"},
    "co2_tee": {"display": "CO2 Pipe Tee", "boxes": _run("z", E0, IN) + _run("z", 16 - IN, E1) + _run("x", 16 - IN, E1) + _fitting()
                + _ends(("z", 0), ("z", 16), ("x", 16)), "conn": "nse"},
}
FAMILY = {"ns": "co2_pipe", "ud": "co2_riser", "ne": "co2_elbow", "nd": "co2_elbow_down", "nse": "co2_tee", "n": "co2_nozzle_end"}


# ------------------------------------------------------------------------------------------------ the signs

SIGNS = {"sign_co2": "CO2 Warning Sign", "sign_hearing": "Hearing Protection Sign"}


def sign_image(kind, d, W, H, F, M, fit):
    """The two new signs' pictures (as tools/plant_panels.py _sign_image draws the others)."""
    BLACK_, WHITE_ = (20, 20, 22, 255), (250, 250, 246, 255)
    if kind == "sign_co2":
        ORANGE = (244, 120, 26, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE_, outline=BLACK_, width=10)
        d.rectangle([10, 10, W - 11, 132], fill=ORANGE)
        d.polygon([(34, 108), (74, 34), (114, 108)], fill=BLACK_)
        d.polygon([(50, 99), (74, 55), (98, 99)], fill=ORANGE)
        d.text((74, 88), "!", font=F(44), fill=BLACK_, anchor="mm")
        fit(d, (W // 2 + 50, 71), "WARNING", 64, W - 150, BLACK_)
        for y, line, size in ((180, "CARBON DIOXIDE", 54), (240, "GAS", 54), (310, "WHEN ALARM SOUNDS", 38), (360, "VACATE IMMEDIATELY", 38),
                              (430, "DO NOT ENTER UNTIL", 34), (472, "VENTILATED", 34)):
            fit(d, (W // 2, y), line, size, M, BLACK_)
    elif kind == "sign_hearing":
        BLUE = (20, 80, 170, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE_, outline=BLACK_, width=10)
        d.rectangle([10, 10, W - 11, 132], fill=BLUE)
        fit(d, (W // 2, 71), "NOTICE", 70, W - 60, WHITE_)
        # Ear defenders: the headband over, the two cups.
        cx, cy = W // 2, 236
        d.arc([cx - 92, cy - 96, cx + 92, cy + 80], 190, 350, fill=BLUE, width=18)
        for k in (-1, 1):
            d.rounded_rectangle([cx + k * 92 - 32, cy - 34, cx + k * 92 + 32, cy + 62], 18, fill=BLUE)
        fit(d, (W // 2, 372), "HEARING PROTECTION", 40, M, BLACK_)
        fit(d, (W // 2, 424), "REQUIRED", 52, M, BLACK_)
        fit(d, (W // 2, 474), "IN THIS AREA", 34, M, BLACK_)


# ------------------------------------------------------------------------------------------------ registering

PANELS = {STATION: {"display": "CO2 Manual Release Station", "width": 1, "item3d": True, "labels": LABELS, "shared": True},
          HORN: {"display": "CO2 Horn and Strobe", "width": 1, "item3d": True},
          FAN: {"display": "Diesel Room Exhaust Fan", "width": 2, "item3d": True, "labels": LABELS, "shared": True},
          PENDANT: {"display": "Monorail Hoist Pendant Station", "width": 1, "item3d": True, "labels": LABELS, "shared": True}}
BUILDERS = {STATION: (co2_station, co2_station_boxes), HORN: (horn_strobe, horn_strobe_boxes), FAN: (room_fan, room_fan_boxes),
            PENDANT: (hoist_pendant, hoist_pendant_boxes)}
CRAFTING = {
    STATION: (["PRP", "LCL", "PBP"], {"P": "#c:plates/steel", "R": "minecraft:red_dye", "L": "minecraft:redstone_lamp", "C": "jugcraft:basic_circuit",
                                     "B": "minecraft:stone_button"}, 1),
    HORN: (["PLP", "RNR"], {"P": "#c:plates/steel", "L": "minecraft:glass_pane", "R": "minecraft:red_dye", "N": "minecraft:note_block"}, 2),
    FAN: (["PIP", "IRI", "PIP"], {"P": "#c:plates/steel", "I": "minecraft:iron_bars", "R": "minecraft:redstone"}, 1),
    PENDANT: (["PYP", "BBB", "PCP"], {"P": "#c:plates/steel", "Y": "minecraft:yellow_dye", "B": "minecraft:stone_button", "C": "minecraft:string"}, 1),
    "co2_pipe": (["RRR", "III", "RRR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 8),
    "co2_pipe_hanger": (["N N", "RIR"], {"N": "minecraft:iron_nugget", "R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "co2_nozzle": (["RIR", " G "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "G": "minecraft:gold_nugget"}, 4),
    "co2_nozzle_end": (["RI ", " G "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "G": "minecraft:gold_nugget"}, 4),
    "co2_riser": (["RIR", "RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "co2_elbow": (["RI ", " IR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "co2_elbow_down": (["RIR", " I "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "co2_tee": (["III", "RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "sign_co2": (["POP", "PIP"], {"P": "minecraft:paper", "O": "minecraft:orange_dye", "I": "minecraft:iron_nugget"}, 2),
    "sign_hearing": (["PBP", "PIP"], {"P": "minecraft:paper", "B": "minecraft:blue_dye", "I": "minecraft:iron_nugget"}, 2),
}
_BUILT = {}


def built(key):
    if key not in _BUILT:
        draw, boxes = BUILDERS[key]
        _BUILT[key] = (draw(), boxes())
    return _BUILT[key]


def makers():
    import nuclear_materials as M
    return {RED_TEX: lambda: M.painted((178, 34, 30), 631, seams=(), chip_count=2, drip_count=0, dirt=0.15)}


def _signs():
    old = PP._sign_image

    def draw(kind):
        if kind not in SIGNS:
            return old(kind)
        from PIL import Image, ImageDraw
        W, H = 433, 512
        img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
        sign_image(kind, ImageDraw.Draw(img), W, H, lambda s: PP.rfont(s, True), W - 56, PP._fit_line)
        return img
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})
    PP.EXTRA_MAKERS.append(lambda: {f"pi_{k}": (lambda k=k: draw(k)) for k in SIGNS})
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], 2, "panel")
        info["panel"] = key
        PP.PANELS[key] = info
        PP.DRAWINGS[key] = lambda label=None, key=key: built(key)[0]
        PP.CABINETS[key] = lambda key=key: built(key)[1]
    DP.BUILDERS.update({k: (lambda k=k: built(k)) for k in BUILDERS})
    PP.RISERS.update({k: {kk: vv for kk, vv in v.items() if kk != "conn"} for k, v in PIPES.items()})
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= {f"pi_{key}_f_{p}" for key in PANELS for p in range(2 * PANELS[key]["width"])}
    _signs()


register()
