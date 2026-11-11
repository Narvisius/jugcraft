"""The diesel generator rooms' own plant (round 31 part 3; Justin: "Diesel Part 3"): the jacket water and lube oil
keep-warm skids, the jacket water expansion tank on its stand, the jacket water cooler, the neutral grounding resistor,
the generator's terminal and CT cubicle and the CO2 fire suppression cylinder bank, drawn from their own parts the way
tools/diesel_set.py draws the set and its starting air (board ops replayed by client/ControlBoards.java, their plates
and gauge faces on the diesel room's legend texture).

Each is labelled by train (its anchor's variant: 0 1A, 1 1B) and joins the nearest diesel generator, as the starting
air pieces do (nuclear/StartingAir.java): their gauges, lamps and the expansion tank's sight glass follow it (Justin:
keep-warm skids "Looks and live gauges only", the cooler "Live gauges, no trip").

Frames as the set's: x to the right as you face the piece's front, y up, z away from you, the front at z 0."""
import math

import board_live as L
import control_boards as C
import diesel_set as DS
import hvac_boards as H
from control_boards import sw, _r
from diesel_set import DARK, MID, BLACK, WHITE, STEEL, YELLOW, run, rev, joint, bolts, valve, seal, plate, front_sign, sign, gauge, \
    labelled, on_floor, own_atlas

TRAINS = ("1A", "1B")
JW, LO, SW = "pipe_green", "pipe_brown", "paint_blue"
GR = 0.15                      # the local gauges' radius (their faces legible close up)


def face(unit, lo, hi):
    """A local gauge's face, its figures big enough to read close up (the low end, the middle and the top; the unit in
    the gap at the bottom; 270 degrees, as the game's needle sweeps): on the diesel room's legend texture."""
    from PIL import ImageDraw
    p = L.px(GR * 1.36)

    def draw(img):
        d = ImageDraw.Draw(img)
        ink = (20, 20, 22, 255)
        d.rectangle([0, 0, p - 1, p - 1], fill=(36, 38, 42, 255))
        d.ellipse([2, 2, p - 3, p - 3], fill=(240, 240, 232, 255))
        cx = cy = p / 2
        r = p / 2 - 5
        for k in range(21):
            a = math.radians(135 + 270 * k / 20)
            major = k % 10 == 0
            r0 = r - (9 if major else 6 if k % 2 == 0 else 4)
            d.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r * math.cos(a), cy + r * math.sin(a))], fill=ink, width=3 if major else 1)
        f = L._font(max(12, int(p * 0.16)))
        for k, v in ((0, lo), (10, (lo + hi) / 2), (20, hi)):
            a = math.radians(135 + 270 * k / 20)
            rt = r - 19
            d.text((cx + rt * math.cos(a), cy + rt * math.sin(a) + (3 if k == 10 else 0)), L._nice(v), font=f, fill=ink, anchor="mm")
        d.text((cx, cy + r * 0.74), unit, font=L._font(max(10, int(p * 0.11))), fill=ink, anchor="mm")
    return L.ATLAS.add(f"edgroom:{unit}:{lo}:{hi}:{p}", p, p, draw)


def lamp(b, live, name, c, r, colour):
    """A pilot lamp facing -z at c: its bezel, the lens the game's (lit by name)."""
    x, y, z = c
    b.cyl((x, y, z), (x, y, z - 0.02), r + 0.012, DARK, DARK, 10)
    live["lamps"].append({"name": name, "c": _r(x, y, z - 0.023), "r": r, "colour": colour, "on": C.LENS[colour], "off": C.LENS["off_" + colour]})


def pad(b, x0, z0, x1, z1, h=0.1):
    b.box(x0, 0.0, z0, x1, h, z1, "concrete")


def motor(b, x0, x1, y, z, r, mat="paint_blue"):
    """A motor along x from x0 to x1 (its fan end at x0): the frame and fins, the fan cowl, feet, the terminal box and
    its conduit down to the base."""
    H.pipe(b, (x0, y, z), (x1, y, z), r, mat, sides=16)
    for k in range(1, 8):
        x = x0 + (x1 - x0) * k / 8
        b.cyl((x - 0.012, y, z), (x + 0.012, y, z), r + 0.025, mat, mat, 16)
    b.cyl((x0 - 0.08, y, z), (x0, y, z), r * 0.92, mat, DARK, 16)
    b.box(x0 + 0.05, y - r - 0.06, z - r * 0.7, x1 - 0.05, y - r * 0.6, z + r * 0.7, mat)
    b.box((x0 + x1) / 2 - 0.09, y + r * 0.55, z - 0.09, (x0 + x1) / 2 + 0.09, y + r + 0.08, z + 0.09, "paint_grey")
    return y - r - 0.06


def skid(b, x0, z0, x1, z1, top=0.24):
    """A skid's base: two channels along x, cross members, the deck plate."""
    for zc in (z0, z1 - 0.14):
        b.box(x0, 0.1, zc, x1, top, zc + 0.14, DARK)
    b.box(x0 + 0.05, top - 0.03, z0 + 0.14, x1 - 0.05, top, z1 - 0.14, "carbon_steel")


def controller(b, live, x0, x1, y0, y1, zf, lamps, title):
    """A local control box on a post: grey box facing -z at zf, its lamps (name, legend, colour) in a row, the
    HAND-OFF-AUTO selector at AUTO, its plate."""
    b.box(x0, y0, zf, x1, y1, zf + 0.2, "paint_grey")
    xc = (x0 + x1) / 2
    b.box(xc - 0.03, 0.24 if y0 > 0.5 else 0.1, zf + 0.07, xc + 0.03, y0, zf + 0.13, DARK)
    n = len(lamps)
    for k, (name, legend, colour) in enumerate(lamps):
        x = x0 + (x1 - x0) * (k + 0.5) / n
        lamp(b, live, name, (x, y1 - 0.12, zf), 0.03, colour)
        front_sign(b, zf, x - 0.075, x + 0.075, y1 - 0.24, y1 - 0.18, plate(legend, 0.15, 0.06, "white", 12), d=0.004)
    b.cyl((xc, y0 + 0.17, zf), (xc, y0 + 0.17, zf - 0.03), 0.045, BLACK, DARK, 10)
    b.box(xc - 0.01, y0 + 0.15, zf - 0.04, xc + 0.04, y0 + 0.19, zf - 0.03, WHITE)
    front_sign(b, zf, x0 + 0.03, x1 - 0.03, y0 + 0.05, y0 + 0.09, plate("HAND   OFF   AUTO", x1 - x0 - 0.06, 0.04, "white"), d=0.004)
    front_sign(b, zf, x0, x1, y1 + 0.02, y1 + 0.12, plate(title, x1 - x0, 0.1, "black"), d=0.006)


def heater_vessel(b, x0, x1, y, z, r, mat, band):
    """A horizontal heater vessel along x: its shell with dished heads, the immersion element's flange and terminal
    box at the right end (its conduit down), the saddles."""
    hd = r * 0.4
    prof = []
    for k in range(6):
        ph = math.pi / 2 * k / 5
        prof.append((hd - hd * math.cos(ph), max(0.001, r * math.sin(ph))))
    prof.append((x1 - x0 - hd, r))
    rev(b, (x0, y, z), (x1 - 0.12, y, z), prof + [(x1 - x0 - 0.12, r)], mat, None, mat, 18)
    b.cyl((x1 - 0.12, y, z), (x1 - 0.06, y, z), r + 0.04, "flange", sw("mgrey"), 18)
    bolts(b, (x1 - 0.06, y, z), r - 0.01, 10, 0.016, 0.02, MID, "x")
    b.box(x1 - 0.06, y - 0.17, z - 0.15, x1 + 0.2, y + 0.17, z + 0.15, "paint_grey")
    b.cyl((x1 + 0.07, y - 0.17, z), (x1 + 0.07, 0.24, z), 0.025, "stainless_dark", "stainless_dark", 6)
    for xs in (x0 + 0.25, x1 - 0.35):
        b.box(xs - 0.05, 0.24, z - r * 0.75, xs + 0.05, y - r * 0.6, z + r * 0.75, DARK)
    b.cyl((x0 + (x1 - x0) * 0.45, y, z), (x0 + (x1 - x0) * 0.55, y, z), r + 0.005, band, band, 18)


def pump(b, x, y, z, r, mat):
    """An end-suction pump's volute facing -x at (x, y, z) on its pedestal; its suction eye out of the left."""
    b.cyl((x - 0.08, y, z), (x + 0.08, y, z), r, mat, mat, 14)
    b.cyl((x - 0.16, y, z), (x - 0.08, y, z), r * 0.45, mat, mat, 10)
    b.box(x - 0.08, 0.24, z - r * 0.6, x + 0.08, y - r * 0.5, z + r * 0.6, mat)
    b.cyl((x + 0.08, y, z), (x + 0.2, y, z), r * 0.4, "stainless", DARK, 8)


# ------------------------------------------------------------------------------------------------ jacket water keep-warm

def jw_keepwarm(seed=331):
    with own_atlas():
        return _jw_keepwarm(seed)


def _jw_keepwarm(seed=331):
    """The jacket water keep-warm skid, 3 wide, 2 deep, 2 tall: on its skid on a pad, the circulating pump (its motor,
    coupling guard and suction strainer) drawing from the engine's jacket out of the floor, through the electric heater
    (30 kW: its vessel along the back, the element's terminal box), back down into the floor to the engine; isolating
    valves, the thermostat's bulb, the outlet and inlet temperature gauges (the game's), the controller with HEATER ON,
    PUMP RUN and POWER (the game's) and its plates."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    green = "paint_green"
    pad(b, 0.06, 0.06, 2.94, 1.94)
    skid(b, 0.15, 0.15, 2.85, 1.85)
    b.core = len(b.still)
    heater_vessel(b, 0.35, 2.2, 1.2, 1.4, 0.3, green, sw("yellow"))
    # The pump at the front left (its suction eye left), the coupling guard, the motor.
    pump(b, 0.65, 0.55, 0.62, 0.2, green)
    b.box(0.85, 0.42, 0.52, 1.05, 0.68, 0.72, YELLOW)
    motor(b, 1.05, 1.65, 0.55, 0.62, 0.2, green)
    # Suction: up out of the floor at the left, through the valve and the Y strainer, into the pump's eye.
    sx, sz = 0.28, 0.62
    run(b, [(sx, 0.0, sz), (sx, 0.55, sz), (0.49, 0.55, sz)], 0.05, JW, joints=DARK, sides=10)
    valve(b, (sx, 0.32, sz), "y", 0.05, (-1, 0, 0))
    b.cyl((0.34, 0.55, sz), (0.44, 0.55, sz), 0.07, "gunmetal", DARK, 10)
    b.cyl((0.39, 0.55, sz), (0.39, 0.42, sz + 0.06), 0.035, "gunmetal", DARK, 8)
    seal(b, sx, sz, 0.05)
    # Discharge: up out of the volute's top, back into the heater's left end low; the heater's outlet up out of its
    # top at the right, a valve, round and down into the floor at the right front.
    run(b, [(0.65, 0.75, sz), (0.65, 0.95, sz), (0.65, 0.95, 1.1), (0.65, 1.05, 1.1), (0.65, 1.05, 1.4), (0.75, 1.05, 1.4)], 0.045, JW, joints=DARK, sides=10)
    ox = 1.9
    run(b, [(ox, 1.48, 1.4), (ox, 1.75, 1.4), (ox, 1.75, 0.3), (2.62, 1.75, 0.3), (2.62, 0.0, 0.3)], 0.045, JW, joints=DARK, sides=10)
    valve(b, (2.25, 1.75, 0.3), "x", 0.045, (0, 1, 0))
    seal(b, 2.62, 0.3, 0.045)
    # The thermostat's bulb in the outlet line and its capillary to the controller.
    b.cyl((ox, 1.75, 0.8), (ox, 1.87, 0.8), 0.02, "stainless", "stainless", 6)
    b.box(ox - 0.04, 1.87, 0.75, ox + 0.04, 1.95, 0.85, "gunmetal")
    # The gauges: outlet temperature on the outlet line, inlet temperature on the suction.
    run(b, [(2.62, 1.3, 0.3), (2.62, 1.3, 0.18)], 0.012, "stainless", sides=6)
    gauge(b, live, "KW OUT TEMP", (2.62, 1.3, 0.18), GR, face("°F", 50, 250))
    front_sign(b, 0.19, 2.5, 2.74, 1.06, 1.12, plate("OUTLET", 0.24, 0.05, "white"), d=0.004)
    run(b, [(sx, 0.85, sz), (sx, 0.85, 0.44)], 0.012, "stainless", sides=6)
    gauge(b, live, "KW IN TEMP", (sx, 0.85, 0.44), GR, face("°F", 50, 250))
    front_sign(b, 0.45, sx - 0.12, sx + 0.12, 0.62, 0.68, plate("INLET", 0.24, 0.05, "white"), d=0.004)
    # The controller on its post at the front right.
    controller(b, live, 1.95, 2.45, 0.85, 1.55, 0.12, [("heater", "HEATER", "amber"), ("pump", "PUMP", "red"), ("power", "POWER", "white")],
               "KEEP-WARM CONTROL")
    b.cyl((2.2, 0.85, 0.22), (2.2, 0.24, 0.22), 0.02, "stainless_dark", "stainless_dark", 6)
    # Its plates: the name by train, the heater's data, the hazard.
    labelled(b, TRAINS, lambda t: front_sign(b, 1.4 - 0.3, 0.75, 1.75, 1.42, 1.62, plate(f"JACKET WATER KEEP-WARM {t}", 1.0, 0.2, "green", 30)))
    b.box(0.8, 1.42, 1.1, 1.7, 1.6, 1.13, "paint_grey")
    sign(b, 1, 2.4, 1.3, 1.5, 1.1, 1.3, plate("30 KW|480 V 3 PH", 0.2, 0.2, "white", 18), d=0.006)
    front_sign(b, 1.1, 1.85, 2.15, 1.36, 1.5, plate("DANGER|HOT SURFACE", 0.3, 0.14, "red", 18), d=0.006)
    b.box(1.85, 1.36, 1.1, 2.15, 1.5, 1.115, "paint_grey")
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ lube oil keep-warm

def lo_keepwarm(seed=337):
    with own_atlas():
        return _lo_keepwarm(seed)


def _lo_keepwarm(seed=337):
    """The lube oil keep-warm and prelube skid, 3 wide, 2 deep, 2 tall: the prelube pump (a gear pump and its motor)
    drawing from the engine's sump out of the floor, through the duplex filter (its two canisters and changeover
    handle) and the electric heater (20 kW) and back down into the floor to the engine's main gallery; isolating valves,
    the oil pressure and temperature gauges (the game's), the controller with HEATER ON, PRELUBE RUN and POWER (the
    game's) and its plates."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    pad(b, 0.06, 0.06, 2.94, 1.94)
    skid(b, 0.15, 0.15, 2.85, 1.85)
    b.core = len(b.still)
    heater_vessel(b, 0.35, 2.2, 1.2, 1.4, 0.27, "tank_grey", sw("orange"))
    # The gear pump (a block with its relief valve) and its motor along the front.
    b.box(0.5, 0.24, 0.45, 0.8, 0.72, 0.8, "gunmetal")
    b.box(0.55, 0.72, 0.55, 0.75, 0.82, 0.7, "gunmetal")
    b.box(0.8, 0.42, 0.52, 0.98, 0.68, 0.73, YELLOW)
    motor(b, 0.98, 1.55, 0.55, 0.625, 0.19, "paint_blue")
    sx, sz = 0.28, 0.62
    run(b, [(sx, 0.0, sz), (sx, 0.55, sz), (0.5, 0.55, sz)], 0.045, LO, joints=DARK, sides=10)
    valve(b, (sx, 0.32, sz), "y", 0.045, (-1, 0, 0))
    seal(b, sx, sz, 0.045)
    # The duplex filter on the pump's discharge: two canisters on a body, the changeover handle.
    b.box(0.6, 0.85, 0.95, 1.2, 1.0, 1.08, "gunmetal")
    for fx in (0.72, 1.08):
        b.cyl((fx, 1.0, 1.02), (fx, 1.42, 1.02), 0.1, "paint_grey", DARK, 12)
        b.cyl((fx, 1.42, 1.02), (fx, 1.46, 1.02), 0.11, DARK, DARK, 12)
        b.cyl((fx, 1.5, 1.02), (fx, 1.53, 1.02), 0.03, "gunmetal", DARK, 6)
    b.box(0.86, 1.0, 0.9, 0.94, 1.12, 0.95, sw("red"))
    b.box(0.88, 1.075, 0.75, 0.92, 1.115, 0.92, sw("red"))
    run(b, [(0.65, 0.82, 0.62), (0.65, 0.92, 0.62), (0.65, 0.92, 0.95)], 0.04, LO, joints=DARK, sides=8)
    run(b, [(1.15, 0.92, 1.02), (1.3, 0.92, 1.02), (1.3, 0.92, 1.4), (1.3, 0.95, 1.4)], 0.04, LO, joints=DARK, sides=8)
    ox = 1.9
    run(b, [(ox, 1.45, 1.4), (ox, 1.75, 1.4), (ox, 1.75, 0.3), (2.62, 1.75, 0.3), (2.62, 0.0, 0.3)], 0.04, LO, joints=DARK, sides=10)
    valve(b, (2.25, 1.75, 0.3), "x", 0.04, (0, 1, 0))
    seal(b, 2.62, 0.3, 0.04)
    run(b, [(2.62, 1.3, 0.3), (2.62, 1.3, 0.18)], 0.012, "stainless", sides=6)
    gauge(b, live, "LO KW TEMP", (2.62, 1.3, 0.18), GR, face("°F", 50, 250))
    front_sign(b, 0.19, 2.48, 2.76, 1.06, 1.12, plate("OIL TEMP", 0.28, 0.05, "white"), d=0.004)
    run(b, [(0.65, 0.92, 0.75), (0.45, 0.92, 0.75), (0.45, 1.25, 0.75), (0.45, 1.25, 0.6)], 0.012, "stainless", sides=6)
    gauge(b, live, "LO KW PRESS", (0.45, 1.25, 0.6), GR, face("PSIG", 0, 100))
    front_sign(b, 0.61, 0.31, 0.59, 1.02, 1.08, plate("OIL PRESS", 0.28, 0.05, "white"), d=0.004)
    controller(b, live, 1.95, 2.45, 0.85, 1.55, 0.12, [("heater", "HEATER", "amber"), ("prelube", "PRELUBE", "red"), ("power", "POWER", "white")],
               "KEEP-WARM CONTROL")
    b.cyl((2.2, 0.85, 0.22), (2.2, 0.24, 0.22), 0.02, "stainless_dark", "stainless_dark", 6)
    labelled(b, TRAINS, lambda t: front_sign(b, 1.1, 0.75, 1.75, 1.42, 1.62, plate(f"LUBE OIL KEEP-WARM {t}", 1.0, 0.2, "orange", 30)))
    b.box(0.8, 1.42, 1.1, 1.7, 1.6, 1.13, "paint_grey")
    sign(b, 1, 2.4, 1.3, 1.5, 1.1, 1.3, plate("20 KW|480 V 3 PH", 0.2, 0.2, "white", 18), d=0.006)
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the expansion tank

TANK_Y, TANK_R = 4.15, 0.55          # the tank's axis height and radius
GLASS = (1.0, 3.72, 4.58)            # the sight glass: its x, bottom, top (on the front)


def expansion_tank(seed=347):
    with own_atlas():
        return _expansion_tank(seed)


def _expansion_tank(seed=347):
    """The jacket water expansion tank on its stand, 2 wide, 2 deep, 5 tall: four angle legs braced on their pads, the
    top frame and saddles, the horizontal tank with dished heads; on its front the sight glass in its guard (the water
    level the game's: it rises as the jacket water warms), the low level switch; on top the filler cap and the vent;
    the expansion line down from its bottom into the floor, the makeup line up the right leg with its valve, the
    overflow down the left leg; the ladder on the right; its plates."""
    b = C.Board(2, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    green = "paint_green"
    legs = ((0.2, 0.2), (1.8, 0.2), (0.2, 1.8), (1.8, 1.8))
    for lx, lz in legs:
        b.box(lx - 0.13, 0.0, lz - 0.13, lx + 0.13, 0.08, lz + 0.13, "concrete")
        b.box(lx - 0.06, 0.08, lz - 0.06, lx + 0.06, 3.5, lz + 0.06, "carbon_steel")
    for y in (0.9, 2.2):
        b.box(0.26, y, 0.16, 1.74, y + 0.08, 0.24, "carbon_steel")
        b.box(0.26, y, 1.76, 1.74, y + 0.08, 1.84, "carbon_steel")
        b.box(0.16, y, 0.26, 0.24, y + 0.08, 1.74, "carbon_steel")
        b.box(1.76, y, 0.26, 1.84, y + 0.08, 1.74, "carbon_steel")
    # The top frame: four beams round the legs' heads and two under the saddles (the lines pass between them).
    for z0, z1, y0 in ((0.08, 0.32, 3.38), (1.68, 1.92, 3.5)):
        b.box(0.08, y0, z0, 1.92, 3.6, z1, "carbon_steel")
    for x0, x1 in ((0.08, 0.32), (1.68, 1.92), (0.35, 0.55), (1.45, 1.65)):
        b.box(x0, 3.5, 0.32, x1, 3.6, 1.68, "carbon_steel")
    hd = 0.2
    prof = []
    for k in range(6):
        ph = math.pi / 2 * k / 5
        prof.append((hd - hd * math.cos(ph), max(0.001, TANK_R * math.sin(ph))))
    for k in range(6):
        ph = math.pi / 2 * k / 5
        prof.append((1.5 + hd + hd * math.sin(ph), max(0.001, TANK_R * math.cos(ph))))
    rev(b, (0.05, TANK_Y, 1.0), (1.95, TANK_Y, 1.0), prof, green, None, None, 22)
    for xs in (0.45, 1.55):
        b.box(xs - 0.06, 3.6, 0.5, xs + 0.06, TANK_Y - TANK_R * 0.6, 1.5, DARK)
    b.core = len(b.still)
    # Cross bracing on the sides (flat bars), the welds on the tank.
    for x in (0.2, 1.8):
        for (ya, yb) in ((0.15, 0.9), (0.98, 2.2), (2.28, 3.5)):
            ln = math.hypot(yb - ya, 1.6)
            b.obox((x, (ya + yb) / 2, 1.0), (0, (yb - ya) / ln, 1.6 / ln), (1, 0, 0), ln / 2, 0.025, 0.01, "carbon_steel")
    for x in (0.25, 1.75):
        b.cyl((x - 0.01, TANK_Y, 1.0), (x + 0.01, TANK_Y, 1.0), TANK_R + 0.006, "weld_band", sw("mgrey"), 22)
    # The sight glass on the front: its two cocks, the glass tube in its guard (the level the game's).
    gx, gy0, gy1 = GLASS
    zf = 1.0 - TANK_R
    for y in (gy0 - 0.04, gy1 + 0.04):
        dz = math.sqrt(max(0.0, TANK_R ** 2 - (y - TANK_Y) ** 2))
        b.cyl((gx, y, 1.0 - dz + 0.02), (gx, y, zf - 0.12), 0.02, "stainless", "stainless", 6)
        b.box(gx - 0.04, y - 0.03, zf - 0.17, gx + 0.04, y + 0.03, zf - 0.1, "gunmetal")
    b.box(gx - 0.07, gy0, zf - 0.19, gx - 0.05, gy1, zf - 0.08, "stainless")
    b.box(gx + 0.05, gy0, zf - 0.19, gx + 0.07, gy1, zf - 0.08, "stainless")
    b.box(gx - 0.05, gy0, zf - 0.1, gx + 0.05, gy1, zf - 0.08, sw("white"))
    live["levels"].append({"name": "JW EXPANSION", "x0": _r(gx - 0.045)[0], "x1": _r(gx + 0.045)[0], "y0": _r(gy0)[0], "y1": _r(gy1)[0],
                           "z": _r(zf - 0.105)[0]})
    # Its scale beside it on the guard: the ticks, HIGH and LOW.
    b.box(gx + 0.07, gy0, zf - 0.1, gx + 0.34, gy1, zf - 0.08, "stainless")
    for k in range(5):
        y = gy0 + 0.02 + (gy1 - gy0 - 0.04) * k / 4
        b.box(gx + 0.07, y - 0.005, zf - 0.104, gx + 0.13, y + 0.005, zf - 0.1, BLACK)
    front_sign(b, zf - 0.1, gx + 0.14, gx + 0.33, gy1 - 0.1, gy1 - 0.02, plate("HIGH", 0.19, 0.08, "white"), d=0.004)
    front_sign(b, zf - 0.1, gx + 0.14, gx + 0.33, gy0 + 0.02, gy0 + 0.1, plate("LOW", 0.19, 0.08, "white"), d=0.004)
    # The low level switch on the left of the front, its conduit down the leg.
    lx = 0.55
    dz = math.sqrt(TANK_R ** 2 - (3.85 - TANK_Y) ** 2)
    b.cyl((lx, 3.85, 1.0 - dz + 0.02), (lx, 3.85, zf - 0.05), 0.025, "stainless", "stainless", 6)
    b.box(lx - 0.08, 3.75, zf - 0.17, lx + 0.08, 3.95, zf - 0.05, "paint_grey")
    run(b, [(lx, 3.75, zf - 0.11), (lx, 3.62, zf - 0.11), (0.2, 3.62, zf - 0.11), (0.2, 0.08, zf - 0.11)], 0.018, "stainless_dark", sides=6)
    front_sign(b, zf - 0.17, lx - 0.08, lx + 0.08, 3.96, 4.02, plate("LS", 0.16, 0.06, "black"), d=0.004)
    # The filler cap and the vent on top.
    top = TANK_Y + TANK_R
    b.cyl((1.4, top - 0.05, 1.0), (1.4, top + 0.12, 1.0), 0.1, green, green, 12)
    b.cyl((1.4, top + 0.12, 1.0), (1.4, top + 0.17, 1.0), 0.13, "gunmetal", DARK, 12)
    run(b, [(0.6, top - 0.03, 1.0), (0.6, top + 0.3, 1.0), (0.6, top + 0.3, 1.25)], 0.025, JW, joints=DARK, sides=8)
    b.cyl((0.6, top + 0.3, 1.25), (0.6, top + 0.23, 1.25), 0.04, "gunmetal", DARK, 8)
    # The expansion line from its bottom down the middle into the floor; the makeup line up the right leg; the overflow
    # down the left.
    run(b, [(1.0, TANK_Y - TANK_R + 0.02, 1.0), (1.0, 0.0, 1.0)], 0.05, JW, joints=DARK, sides=10)
    valve(b, (1.0, 1.4, 1.0), "y", 0.05, (0, 0, -1))
    seal(b, 1.0, 1.0, 0.05)
    run(b, [(1.7, 0.0, 1.0), (1.7, 3.9, 1.0), (1.55, 3.9, 1.0)], 0.025, "stainless", joints="stainless", sides=8)
    valve(b, (1.7, 1.6, 1.0), "y", 0.025, (0, 0, -1), 0.06)
    seal(b, 1.7, 1.0, 0.025)
    run(b, [(0.3, TANK_Y + 0.2, 1.0), (0.3, 0.0, 1.0)], 0.025, JW, joints=DARK, sides=8)
    seal(b, 0.3, 1.0, 0.025)
    # The ladder up the right side (rungs between two stiles), from the floor to the top frame.
    lz0, lz1 = 0.75, 1.25
    for zs in (lz0, lz1):
        b.box(1.93, 0.0, zs - 0.03, 1.99, 3.9, zs + 0.03, YELLOW)
    for k in range(13):
        y = 0.3 + k * 0.28
        b.cyl((1.96, y, lz0), (1.96, y, lz1), 0.018, YELLOW, YELLOW, 6)
    # Its plates on a bracket under the tank: the name by train, its data.
    labelled(b, TRAINS, lambda t: front_sign(b, 0.08, 0.5, 1.5, 3.4, 3.58, plate(f"JW EXPANSION TANK {t}", 1.0, 0.18, "green", 26)))
    front_sign(b, 0.16, 0.4, 0.9, 2.04, 2.24, plate("CAPACITY 150 GAL|TREATED WATER", 0.5, 0.2, "white", 18), d=0.006)
    b.live = live
    live["mats"] = {"needle": sw("red"), "water": "water"}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the jacket water cooler

COOLER_Y, COOLER_R = 1.15, 0.6
SHELL_X = (0.95, 6.05)


def jw_cooler(seed=349):
    with own_atlas():
        return _jw_cooler(seed)


def _jw_cooler(seed=349):
    """The jacket water cooler, 7 wide, 2 deep, 3 tall (along a wall): a shell and tube heat exchanger on two saddles on
    concrete piers, the jacket water through the shell and the plant's service water through the tubes (two passes:
    the channel head on the left with its pass partition, the floating head's cover on the right). The jacket water in
    over the top at the right through the three-way thermostatic valve (its actuator and position pointer the game's;
    its bypass round the cooler) and out of the bottom at the left; the service water in and out of the channel head,
    all down into the floor at the back. Vents and drains, the relief valve, four temperature gauges (the game's), its
    nameplate and data plate."""
    b = C.Board(7, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    green = "paint_green"
    zc = 1.0
    x0, x1 = SHELL_X
    for px in (1.7, 5.3):
        b.box(px - 0.35, 0.0, zc - 0.55, px + 0.35, 0.35, zc + 0.55, "concrete")
        b.box(px - 0.08, 0.42, zc - 0.5, px + 0.08, COOLER_Y - COOLER_R * 0.5, zc + 0.5, DARK)
        b.box(px - 0.3, 0.35, zc - 0.5, px + 0.3, 0.42, zc + 0.5, DARK)
    H.pipe(b, (x0, COOLER_Y, zc), (x1, COOLER_Y, zc), COOLER_R, green, sides=24)
    # The channel head (left), its flange and the bonnet; the floating head's cover (right).
    b.cyl((0.45, COOLER_Y, zc), (x0, COOLER_Y, zc), COOLER_R - 0.02, green, green, 24)
    rev(b, (0.18, COOLER_Y, zc), (0.45, COOLER_Y, zc), [(0, 0.15), (0.12, 0.45), (0.27, COOLER_R - 0.02)], green, green, None, 24)
    rev(b, (x1, COOLER_Y, zc), (6.55, COOLER_Y, zc), [(0, COOLER_R), (0.2, COOLER_R * 0.95), (0.4, 0.4), (0.5, 0.15)], green, None, green, 24)
    b.core = len(b.still)
    for fx in (0.45, x0, x1):
        b.cyl((fx - 0.03, COOLER_Y, zc), (fx + 0.03, COOLER_Y, zc), COOLER_R + 0.08, "flange", sw("mgrey"), 24)
        bolts(b, (fx + 0.03, COOLER_Y, zc), COOLER_R + 0.04, 16, 0.016, 0.025, MID, "x")
        bolts(b, (fx - 0.03, COOLER_Y, zc), COOLER_R + 0.04, 16, 0.016, 0.025, MID, "-x")
    for x in (2.4, 3.5, 4.6):
        b.cyl((x - 0.01, COOLER_Y, zc), (x + 0.01, COOLER_Y, zc), COOLER_R + 0.006, "weld_band", sw("mgrey"), 24)
    # Service water: in the bottom of the channel head, out of its top, both back and down into the floor.
    top, bot = COOLER_Y + COOLER_R, COOLER_Y - COOLER_R
    for (y_a, y_b, name) in ((bot, 0.35, "in"), (top, top + 0.3, "out")):
        p0 = (0.7, y_a, zc)
        p1 = (0.7, y_b, zc)
        b.cyl(p0, p1, 0.11, SW, SW, 12)
        H.flange(b, p1, "y", 0.11)
        if name == "in":
            run(b, [(0.7, 0.35, zc), (0.7, 0.35, 1.7), (0.7, 0.0, 1.7)], 0.11, SW, joints=DARK, sides=12)
            seal(b, 0.7, 1.7, 0.11)
        else:
            run(b, [(0.7, top + 0.3, zc), (0.7, top + 0.45, zc), (0.7, top + 0.45, 1.75), (0.25, top + 0.45, 1.75), (0.25, 0.0, 1.75)], 0.11, SW,
                joints=DARK, sides=12)
            seal(b, 0.25, 1.75, 0.11)
    # Jacket water in: up out of the floor at the back right, through the three-way valve over the cooler, down into
    # the shell's top; the bypass from the valve's side down the back to the outlet; out of the shell's bottom at the
    # left, back and down into the floor.
    vx, vy = 5.6, 2.3
    run(b, [(6.4, 0.0, 1.75), (6.4, vy, 1.75), (6.4, vy, zc), (vx + 0.2, vy, zc)], 0.1, JW, joints=DARK, sides=12)
    seal(b, 6.4, 1.75, 0.1)
    b.box(vx - 0.2, vy - 0.16, zc - 0.16, vx + 0.2, vy + 0.16, zc + 0.16, "gunmetal")
    for fx in (vx - 0.2, vx + 0.2):
        H.flange(b, (fx, vy, zc), "x", 0.1)
    run(b, [(vx, vy - 0.16, zc), (vx, top + 0.03, zc)], 0.1, JW, joints=DARK, sides=12)
    H.flange(b, (vx, top + 0.12, zc), "y", 0.1)
    ox = 1.2
    run(b, [(vx - 0.2, vy, zc), (4.8, vy, zc), (4.8, vy, 1.75), (1.6, vy, 1.75), (1.6, 0.25, 1.75), (ox + 0.08, 0.25, 1.75)], 0.08, JW, joints=DARK,
        sides=10)
    run(b, [(ox, bot, zc), (ox, 0.25, zc), (ox, 0.25, 1.75), (ox, 0.0, 1.75)], 0.1, JW, joints=DARK, sides=12)
    b.box(ox - 0.12, 0.13, 1.63, ox + 0.12, 0.37, 1.87, "gunmetal")
    seal(b, ox, 1.75, 0.1)
    # The thermostatic valve's actuator on top (the element housing and its position pointer: the game's).
    b.cyl((vx, vy + 0.16, zc), (vx, vy + 0.4, zc), 0.11, "gunmetal", DARK, 12)
    b.cyl((vx, vy + 0.4, zc), (vx, vy + 0.46, zc), 0.14, DARK, DARK, 12)
    b.box(vx - 0.16, vy - 0.08, zc - 0.25, vx + 0.16, vy + 0.24, zc - 0.17, WHITE)
    front_sign(b, zc - 0.25, vx - 0.155, vx - 0.005, vy + 0.15, vy + 0.21, plate("BYPASS", 0.15, 0.06, "white", 14), d=0.003)
    front_sign(b, zc - 0.25, vx + 0.005, vx + 0.155, vy + 0.15, vy + 0.21, plate("COOLER", 0.15, 0.06, "white", 14), d=0.003)
    front_sign(b, zc - 0.25, vx - 0.16, vx + 0.16, vy + 0.24, vy + 0.31, plate("JW TEMP VALVE", 0.32, 0.07, "black", 15), d=0.006)
    live["gauges"].append({"name": "VALVE POSITION", "c": _r(vx, vy - 0.06, zc - 0.255), "len": 0.14, "n": [0, 0, -1], "sweep": 90})
    # Vents and drains, the relief valve on the shell.
    for x, y, out in ((3.0, top, 1), (2.2, bot, -1)):
        b.cyl((x, y, zc), (x, y + out * 0.15, zc), 0.025, "stainless", "stainless", 6)
        b.cyl((x, y + out * 0.15, zc), (x, y + out * 0.21, zc), 0.035, "gunmetal", DARK, 8)
    b.cyl((4.2, top - 0.02, zc), (4.2, top + 0.18, zc), 0.04, "stainless", "stainless", 8)
    b.cyl((4.2, top + 0.18, zc), (4.2, top + 0.38, zc), 0.07, "gunmetal", DARK, 10)
    b.box(4.19, top + 0.33, zc - 0.15, 4.21, top + 0.36, zc + 0.02, sw("red"))
    # The four temperature gauges on their wells at the front: the jacket water in (right) and out (left), the service
    # water in and out (on the channel head).
    for name, x, y, legend, below in (("JW IN TEMP", 6.0, 2.3, "JW IN", True), ("JW OUT TEMP", ox, 0.42, "JW OUT", True),
                                      ("SW IN TEMP", 0.7, 0.45, "SW IN", True), ("SW OUT TEMP", 0.7, 1.93, "SW OUT", True)):
        zg = zc - 0.32
        b.cyl((x, y, zc - 0.09), (x, y, zg), 0.015, "stainless", "stainless", 6)
        gauge(b, live, name, (x, y, zg), GR, face("°F", 50, 250))
        front_sign(b, zg + 0.02, x - 0.13, x + 0.13, y - 0.23, y - 0.17, plate(legend, 0.26, 0.06, "white"), d=0.004)
    # The nameplate (by train) and the data plate on brackets off the shell's front; the flow arrows.
    zf = zc - COOLER_R - 0.05
    for xa, xb, ya, yb in ((2.4, 4.1, 1.25, 1.6), (4.4, 5.2, 1.2, 1.5)):
        back = zc - math.sqrt(COOLER_R ** 2 - (max(ya, yb) - 0.08 - COOLER_Y) ** 2) + 0.03
        for x in (xa + 0.1, xb - 0.16):
            b.box(x, ya + 0.08, zf + 0.01, x + 0.06, yb - 0.08, back, DARK)
    labelled(b, TRAINS, lambda t: front_sign(b, zf + 0.01, 2.4, 4.1, 1.25, 1.6, plate(f"JACKET WATER COOLER {t}|JW-HX-{t}", 1.7, 0.35, "green", 30)))
    front_sign(b, zf + 0.01, 4.4, 5.2, 1.2, 1.5, plate("SHELL JW 150 PSIG|TUBES SW 150 PSIG|9.2 MBTU/HR", 0.8, 0.3, "white", 21))
    front_sign(b, zc - COOLER_R + 0.02, 0.5, 0.9, 1.08, 1.2, plate("SERVICE|WATER", 0.4, 0.12, "blue", 18), d=0.008)
    b.live = live
    live["mats"] = {"needle": sw("red"), "pointer": sw("black")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the NGR

def ngr(seed=353):
    with own_atlas():
        return _ngr(seed)


def _ngr(seed=353):
    """The generator's neutral grounding resistor, 2 wide, 2 deep, 2 tall: a ventilated steel enclosure (louvred side
    panels, a raised roof with its screened gap) on its base channels; the entrance bushing on top for the neutral cable
    from the terminal cubicle, the ground pad and its green ground cable down into the floor; its plates and the
    danger signs."""
    b = C.Board(2, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    grey = "paint_grey"
    pad(b, 0.04, 0.04, 1.96, 1.96)
    for x in (0.12, 1.72):
        b.box(x, 0.1, 0.12, x + 0.16, 0.22, 1.88, DARK)
    b.box(0.12, 0.22, 0.12, 1.88, 1.65, 1.88, grey)
    b.box(0.06, 1.75, 0.06, 1.94, 1.82, 1.92, grey)
    for x, z in ((0.14, 0.14), (1.78, 0.14), (0.14, 1.78), (1.78, 1.78)):
        b.box(x, 1.65, z, x + 0.08, 1.75, z + 0.08, grey)
    b.core = len(b.still)
    # The screened gap under the roof, the louvres on the front and the sides (slats leaning out).
    b.box(0.13, 1.65, 0.12, 1.87, 1.75, 0.125, "grating")
    for k in range(7):
        y = 0.6 + k * 0.125
        b.obox((1.0, y, 0.1), (0, 0.6, -0.8), (1, 0, 0), 0.06, 0.65, 0.008, grey)
        for side, xf in ((-1, 0.12), (1, 1.88)):
            b.obox((xf + side * 0.02, y, 1.0), (side * 0.8, 0.6, 0), (0, 0, 1), 0.06, 0.65, 0.008, grey)
    for xf in (0.3, 1.7):
        b.box(xf - 0.02, 0.52, 0.09, xf + 0.02, 1.38, 0.12, grey)
    # The entrance bushing on the roof: its porcelain sheds, the cap and the cable's lug.
    b.cyl((1.0, 1.82, 1.0), (1.0, 1.86, 1.0), 0.14, DARK, DARK, 12)
    for k in range(4):
        y = 1.86 + k * 0.05
        b.cyl((1.0, y, 1.0), (1.0, y + 0.03, 1.0), 0.09, sw("enamel"), sw("enamel"), 12)
        b.cyl((1.0, y + 0.03, 1.0), (1.0, y + 0.05, 1.0), 0.06, sw("enamel"), sw("enamel"), 12)
    b.cyl((1.0, 2.06, 1.0), (1.0, 2.12, 1.0), 0.04, "copper", "copper", 8)
    # The neutral cable from the lug back over the roof and down behind into the floor (to the terminal cubicle).
    run(b, [(1.0, 2.1, 1.0), (1.0, 2.1, 1.955), (1.0, 0.0, 1.955)], 0.022, BLACK, joints=BLACK, sides=8)
    b.cyl((1.0, 0.0, 1.955), (1.0, 0.03, 1.955), 0.04, sw("red"), sw("red"), 10)
    # The ground pad on the right side, the green ground cable down into the floor.
    b.box(1.88, 0.35, 0.3, 1.92, 0.5, 0.6, "copper")
    run(b, [(1.93, 0.42, 0.45), (1.97, 0.42, 0.45), (1.97, 0.0, 0.45)], 0.02, sw("green"), joints=sw("green"), sides=6)
    # Its plates: the name by train, the rating, the danger signs.
    labelled(b, TRAINS, lambda t: front_sign(b, 0.09, 0.5, 1.5, 1.42, 1.6, plate(f"NEUTRAL GROUNDING RESISTOR {t}", 1.0, 0.18, "black", 22)))
    front_sign(b, 0.09, 0.3, 1.2, 0.28, 0.5, plate("2400 V   400 A   10 SEC|6.0 OHMS", 0.9, 0.22, "white", 18))
    front_sign(b, 0.09, 1.32, 1.72, 0.28, 0.5, plate("DANGER|HIGH VOLTAGE", 0.4, 0.22, "red", 16))
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the terminal cubicle

PHASES = (0.55, 0.835, 1.12)        # the bus bars' x, behind the left door's window
ARRESTERS = (1.95, 2.2, 2.45)       # the surge arresters' and capacitors' x, behind the right door's


def glare(b, x0, y0, x1, y1, z):
    """A window's glass: open (the cubicle's inside seen through it), two streaks of light across it."""
    for off, w in ((-0.12, 0.024), (0.0, 0.01)):
        cx, cy = (x0 + x1) / 2 + off, (y0 + y1) / 2
        dx, dy = 0.5, 0.866
        ts = []
        for t in ((x0 - cx) / dx, (x1 - cx) / dx):
            ts.append(t)
        for t in ((y0 - cy) / dy, (y1 - cy) / dy):
            ts.append(t)
        lo = max(min(ts[0], ts[1]), min(ts[2], ts[3])) + w
        hi = min(max(ts[0], ts[1]), max(ts[2], ts[3])) - w
        mid = (lo + hi) / 2
        b.obox((cx + dx * mid, cy + dy * mid, z), (dx, dy, 0), (0, 0, 1), (hi - lo) / 2, 0.001, w / 2, sw("enamel"))

def gen_cubicle(seed=359):
    with own_atlas():
        return _gen_cubicle(seed)


def _gen_cubicle(seed=359):
    """The generator's terminal and CT cubicle, 3 wide, 2 deep, 3 tall: a metal-clad cubicle on its base, two doors
    (hinges, handle and padlock hasp, a wired-glass window each) onto what is inside: the three phase buses (copper,
    their insulating supports), a current transformer round each phase at the top (the protection's) and at the
    bottom (the metering's), the three surge arresters and surge capacitors on their shelf; the cables come up through
    the floor; the vent hood on top; its plates and the danger signs."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    grey = "paint_grey"
    pad(b, 0.04, 0.04, 2.96, 1.96)
    b.box(0.1, 0.1, 0.1, 2.9, 0.2, 1.9, DARK)
    zf = 0.3
    # The body: sides, back, roof, the floor plate; the front frame round the two door openings.
    b.box(0.1, 0.2, zf, 0.2, 2.6, 1.9, grey)
    b.box(2.8, 0.2, zf, 2.9, 2.6, 1.9, grey)
    b.box(0.2, 0.2, 1.8, 2.8, 2.6, 1.9, grey)
    b.box(0.1, 2.6, zf, 2.9, 2.68, 1.9, grey)
    b.box(0.2, 0.2, zf, 2.8, 0.24, 1.8, DARK)
    b.box(0.2, 0.24, zf, 2.8, 0.35, zf + 0.05, grey)
    b.box(0.2, 2.5, zf, 2.8, 2.6, zf + 0.05, grey)
    b.box(1.47, 0.35, zf, 1.53, 2.5, zf + 0.05, grey)
    b.core = len(b.still)
    # The doors (closed: their windows show inside), hinges on the outer edges, handles at the middle.
    for (dx0, dx1, hinge) in ((0.2, 1.47, 0.2), (1.53, 2.8, 2.8)):
        wx0, wx1, wy0, wy1 = dx0 + 0.2, dx1 - 0.2, 1.0, 2.15
        d = 0.03
        b.box(dx0, 0.35, zf - d, dx1, wy0, zf, grey)
        b.box(dx0, wy1, zf - d, dx1, 2.5, zf, grey)
        b.box(dx0, wy0, zf - d, wx0, wy1, zf, grey)
        b.box(wx1, wy0, zf - d, dx1, wy1, zf, grey)
        glare(b, wx0, wy0, wx1, wy1, zf - 0.012)
        for y in (wy0, wy1):
            b.box(wx0 - 0.02, y - 0.02, zf - d - 0.006, wx1 + 0.02, y + 0.02, zf - d, BLACK)
        for x in (wx0, wx1):
            b.box(x - 0.02, wy0 + 0.02, zf - d - 0.006, x + 0.02, wy1 - 0.02, zf - d, BLACK)
        for hy in (0.6, 2.1):
            b.cyl((hinge, hy - 0.08, zf - d - 0.02), (hinge, hy + 0.08, zf - d - 0.02), 0.02, DARK, MID, 8)
        hx = dx1 - 0.12 if hinge < 1 else dx0 + 0.12
        b.box(hx - 0.03, 1.25, zf - d - 0.03, hx + 0.03, 1.5, zf - d, BLACK)
        b.box(hx - 0.05, 1.18, zf - d - 0.02, hx + 0.05, 1.22, zf - d, "gunmetal")
    # Inside, seen through the windows (the mounting pan white): behind the left door the three phases, each cable up
    # through the floor to its stress cone and lug on its bus bar, the bar on its porcelain standoffs through the
    # protection CT (top) and the metering CT (below) and out at the top back down to the floor to the switchgear;
    # behind the right door the three surge arresters (porcelain sheds) on their shelf over the three surge capacitors;
    # the ground bus along the bottom.
    pan = 1.2
    b.box(0.2, 0.24, pan - 0.03, 2.8, 2.35, pan, "paint_white")
    zi = 0.75
    for x in PHASES:
        b.cyl((x, 0.24, zi), (x, 0.62, zi), 0.045, BLACK, BLACK, 10)
        rev(b, (x, 0.62, zi), (x, 0.74, zi), [(0, 0.045), (0.08, 0.07), (0.12, 0.05)], BLACK, None, BLACK, 10)
        b.box(x - 0.03, 0.72, zi - 0.015, x + 0.03, 0.82, zi + 0.015, "copper")
        b.box(x - 0.05, 0.78, zi - 0.01, x + 0.05, 2.22, zi + 0.01, "copper")
        b.box(x - 0.03, 2.2, zi - 0.015, x + 0.03, 2.3, zi + 0.015, "copper")
        run(b, [(x, 2.28, zi), (x, 2.45, zi), (x, 2.45, 1.5), (x, 0.24, 1.5)], 0.04, BLACK, joints=BLACK, sides=8)
        for y in (0.95, 2.05):
            b.cyl((x, y, zi + 0.01), (x, y, pan - 0.03), 0.035, "glaze_brown", "glaze_brown", 8)
            for k in range(3):
                zs = zi + 0.08 + k * 0.1
                b.cyl((x, y, zs), (x, y, zs + 0.025), 0.055, "glaze_brown", "glaze_brown", 8)
        for y0, y1 in ((1.62, 1.8), (1.18, 1.36)):
            b.cyl((x, y0, zi), (x, y1, zi), 0.12, BLACK, DARK, 14)
            b.box(x - 0.05, y0 + 0.04, zi + 0.12, x + 0.05, y1 - 0.04, pan - 0.03, DARK)
            b.box(x - 0.04, y0 + 0.05, zi - 0.121, x + 0.04, y1 - 0.05, zi - 0.118, WHITE)
    for y in (1.0, 1.5):
        b.box(1.62, y - 0.04, 0.55, 2.75, y, pan - 0.03, DARK)
    for x in ARRESTERS:
        b.box(x - 0.07, 1.5, 0.73, x + 0.07, 1.55, 0.87, DARK)
        b.cyl((x, 1.55, 0.8), (x, 1.98, 0.8), 0.045, "glaze_brown", "glaze_brown", 10)
        for k in range(4):
            y = 1.62 + k * 0.09
            b.cyl((x, y, 0.8), (x, y + 0.03, 0.8), 0.078, "glaze_brown", "glaze_brown", 10)
        b.cyl((x, 1.98, 0.8), (x, 2.03, 0.8), 0.055, MID, MID, 10)
        b.box(x - 0.08, 1.0, 0.71, x + 0.08, 1.36, 0.89, "paint_grey")
        b.cyl((x, 1.36, 0.8), (x, 1.44, 0.8), 0.03, sw("enamel"), sw("enamel"), 8)
    b.box(0.25, 0.36, pan - 0.06, 2.75, 0.42, pan - 0.03, "copper")
    # The vent hood on the roof; the plates; the danger signs.
    b.box(0.6, 2.68, 0.7, 2.4, 2.78, 1.5, grey)
    b.box(0.6, 2.74, 0.68, 2.4, 2.76, 0.7, "grating")
    labelled(b, TRAINS, lambda t: front_sign(b, zf - 0.03, 0.4, 2.6, 2.28, 2.46, plate(f"GENERATOR TERMINAL AND CT CUBICLE {t}", 2.2, 0.18, "black", 22)))
    front_sign(b, zf - 0.03, 0.35, 0.95, 0.5, 0.8, plate("DANGER|4160 VOLTS", 0.6, 0.3, "red", 20))
    front_sign(b, zf - 0.03, 2.05, 2.65, 0.5, 0.8, plate("DANGER|4160 VOLTS", 0.6, 0.3, "red", 20))
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the CO2 bank

CO2_X = [0.32 + k * 0.46 for k in range(5)]
MAN_Y, MAN_Z = 1.92, 0.5                # the manifold over the heads
RISER_X = 0.5                           # the riser out of the top (a block's middle: the room's CO2 riser goes on up from it)


def co2_bank(seed=367):
    with own_atlas():
        return _co2_bank(seed)


def _co2_bank(seed=367):
    """The room's CO2 fire suppression cylinders, 3 wide, 1 deep, 2 tall, against the wall: five high pressure CO2
    cylinders (red) in their rack, each with its valve and discharge head and a flexible hose up to the manifold, a
    stencilled band and a strap; the nitrogen pilot cylinder with its solenoid actuator (its conduit up out of the top,
    to the release station) and the pilot line along the heads into each cylinder's valve; the manifold and its check
    valves, the safety outlet, the riser out of the top at the right (on up the wall to the nozzles); the front
    restraint bar and its warning; the rack's header with the system's name by train."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    red = "pipe_red"
    zc = 0.55
    # The rack against the wall (two posts, the rails) and the cylinders in it.
    b.box(0.08, 0.0, 0.85, 0.16, 2.0, 0.95, "carbon_steel")
    b.box(2.84, 0.0, 0.85, 2.92, 2.0, 0.95, "carbon_steel")
    b.box(0.16, 1.92, 0.85, 2.84, 2.0, 0.95, "carbon_steel")
    for y in (0.6, 1.3):
        b.box(0.16, y, 0.85, 2.84, y + 0.08, 0.95, "carbon_steel")
    for x in CO2_X:
        rev(b, (x, 0.02, zc), (x, 1.5, zc), [(0, 0.17), (0.03, 0.2), (1.3, 0.2), (1.42, 0.12), (1.48, 0.05)], red, sw("red"), red, 16)
    b.core = len(b.still)
    px = 2.6
    for x in CO2_X:
        # The valve and discharge head (its pilot actuator), the hose up to the manifold; the stencilled band, the strap.
        b.cyl((x, 1.5, zc), (x, 1.6, zc), 0.045, "copper", "copper", 8)
        b.box(x - 0.06, 1.6, zc - 0.06, x + 0.06, 1.72, zc + 0.06, "gunmetal")
        b.box(x - 0.035, 1.62, zc - 0.1, x + 0.035, 1.7, zc - 0.06, DARK)
        run(b, [(x, 1.72, zc), (x, 1.78, zc), (x, 1.78, zc - 0.17), (x, 1.92, zc - 0.17), (x, 1.92, MAN_Z - 0.05)], 0.02, BLACK, joints=BLACK, sides=6)
        b.cyl((x, 0.86, zc), (x, 0.92, zc), 0.203, sw("white"), sw("white"), 16)
        b.cyl((x, 1.16, zc), (x, 1.2, zc), 0.205, "carbon_steel", "carbon_steel", 16)
    # The pilot line along the heads' actuators, back from the pilot cylinder's head.
    run(b, [(CO2_X[0], 1.66, zc - 0.115), (px, 1.66, zc - 0.115), (px, 1.66, zc), (px, 1.42, zc)], 0.012, "stainless", sides=6)
    # The manifold along over the heads with its check valves (each hose loops up into one), the safety outlet at the
    # right, the riser up out of the top at the left (on up the wall: the room's CO2 piping).
    run(b, [(2.8, MAN_Y, MAN_Z), (RISER_X, MAN_Y, MAN_Z), (RISER_X, 2.0, MAN_Z)], 0.05, red, joints=red, sides=10)
    for x in CO2_X:
        b.box(x - 0.045, MAN_Y - 0.055, MAN_Z - 0.09, x + 0.045, MAN_Y + 0.055, MAN_Z - 0.04, "gunmetal")
    b.cyl((2.8, MAN_Y, MAN_Z), (2.86, MAN_Y, MAN_Z), 0.035, "gunmetal", DARK, 8)
    b.box(2.84, MAN_Y - 0.03, MAN_Z - 0.03, 2.9, MAN_Y + 0.08, MAN_Z + 0.03, sw("red"))
    # The nitrogen pilot cylinder (small, grey) on its stand, its solenoid actuator, the conduit up out of the top.
    b.box(px - 0.15, 0.0, zc - 0.15, px + 0.15, 0.06, zc + 0.15, DARK)
    rev(b, (px, 0.06, zc), (px, 1.2, zc), [(0, 0.11), (1.0, 0.11), (1.1, 0.05), (1.14, 0.03)], "paint_grey", sw("mgrey"), "paint_grey", 12)
    b.box(px - 0.08, 1.2, zc - 0.08, px + 0.08, 1.36, zc + 0.08, sw("red"))
    b.box(px - 0.05, 1.24, zc - 0.081, px + 0.05, 1.32, zc - 0.08, WHITE)
    run(b, [(px, 1.36, zc + 0.04), (px, 1.75, zc + 0.04), (px, 1.75, 0.8), (px, 2.0, 0.8)], 0.015, "stainless_dark", sides=6)
    # The front restraint bar (its arms back to the posts) and its warning; the header's nameplate.
    b.box(0.12, 1.05, 0.32, 2.88, 1.13, 0.345, "carbon_steel")
    for x0 in (0.08, 2.84):
        b.box(x0, 1.05, 0.345, x0 + 0.08, 1.13, 0.85, "carbon_steel")
    front_sign(b, 0.32, 1.35, 2.55, 0.94, 1.24, plate("WARNING  CARBON DIOXIDE|HIGH PRESSURE 850 PSIG", 1.2, 0.3, "yellow", 18), d=0.006)
    labelled(b, TRAINS, lambda t: front_sign(b, 0.32, 0.25, 1.25, 0.94, 1.24, plate(f"CO2 SYSTEM|EDG ROOM {t}", 1.0, 0.3, "red", 20), d=0.006))
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the monorail hoist

MONO_W = 20
BEAM_Z = 1.0                                  # the beam's middle (over the crankshaft)
BEAM_TOP, BEAM_BOT = 0.92, 0.5                # its top flange's top, its bottom flange's underside
FLANGE, WEB = 0.17, 0.02                      # the flanges' half width, the web's half thickness
HANGERS = (1.5, 5.5, 10.5, 14.5, 18.5)        # the hangers' x (clear of the CO2 branches overhead)


def monorail(seed=373):
    with own_atlas():
        return _monorail(seed)


def _monorail(seed=373):
    """The engine's monorail hoist beam, 20 wide, 2 deep, 1 high (hung under the roof over the crankshaft): a yellow
    I-beam from end to end on its hangers (beam clamps on the top flange, threaded rods up to plates on the roof), the
    end stops with their rubber bumpers, the hoist's conductor bar along the front of the web on its brackets, the
    capacity stencilled on the web both sides. The trolley, the electric chain hoist, its chain and hook block are the
    game's (they travel along the beam and the hook goes up and down from the pendant station: live)."""
    b = C.Board(MONO_W, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    Y = YELLOW
    z0, z1 = BEAM_Z - FLANGE, BEAM_Z + FLANGE
    b.box(0.0, BEAM_TOP - 0.05, z0, MONO_W, BEAM_TOP, z1, Y)
    b.box(0.0, BEAM_BOT, z0, MONO_W, BEAM_BOT + 0.05, z1, Y)
    b.box(0.0, BEAM_BOT + 0.05, BEAM_Z - WEB, MONO_W, BEAM_TOP - 0.05, BEAM_Z + WEB, Y)
    b.core = len(b.still)
    for hx in HANGERS:
        # The beam clamp (two jaws over the top flange's edges and their bolts), the rod up to the roof, its plate.
        b.box(hx - 0.1, BEAM_TOP, z0 - 0.03, hx + 0.1, BEAM_TOP + 0.06, z1 + 0.03, DARK)
        for zj in (z0 - 0.03, z1 - 0.01):
            b.box(hx - 0.1, BEAM_TOP - 0.07, zj, hx + 0.1, BEAM_TOP - 0.005, zj + 0.04, DARK)
        b.cyl((hx, BEAM_TOP + 0.06, BEAM_Z), (hx, 1.97, BEAM_Z), 0.025, "stainless", "stainless", 6)
        for y in (BEAM_TOP + 0.06, 1.9):
            b.cyl((hx, y, BEAM_Z), (hx, y + 0.04, BEAM_Z), 0.045, MID, MID, 6)
        b.box(hx - 0.12, 1.97, BEAM_Z - 0.12, hx + 0.12, 2.0, BEAM_Z + 0.12, DARK)
    for x, d in ((0.06, 1), (MONO_W - 0.06, -1)):
        # The end stop: a plate across under the bottom flange, bolted through it, its bumper facing along the beam.
        xa, xb = (x - 0.05, x + 0.05)
        b.box(xa, BEAM_BOT - 0.16, z0 - 0.02, xb, BEAM_BOT, z1 + 0.02, DARK)
        b.cyl((x + d * 0.05, BEAM_BOT - 0.08, BEAM_Z), (x + d * 0.14, BEAM_BOT - 0.08, BEAM_Z), 0.06, BLACK, BLACK, 10)
    # The conductor bar along the web's front (the hoist's power, its collector the game's), on brackets.
    b.box(0.3, BEAM_BOT + 0.12, z0 + 0.02, MONO_W - 0.3, BEAM_BOT + 0.2, z0 + 0.06, sw("dgrey"))
    b.box(0.3, BEAM_BOT + 0.13, z0 + 0.015, MONO_W - 0.3, BEAM_BOT + 0.14, z0 + 0.02, sw("red"))
    for x in [0.5 + 2.0 * k for k in range(10)]:
        b.box(x - 0.03, BEAM_BOT + 0.14, z0 + 0.06, x + 0.03, BEAM_BOT + 0.18, BEAM_Z - WEB, MID)
    # The capacity stencilled on the web, on both sides (the front's clear of the conductor bar: above it).
    for x in (4.0, 12.0):
        b.box(x, BEAM_BOT + 0.21, BEAM_Z - WEB - 0.004, x + 1.6, BEAM_TOP - 0.06, BEAM_Z - WEB,
              [plate("CAPACITY 5 TON", 1.6, 0.15, "yellow", 20), None, None, None, None, None])
        b.box(x, BEAM_BOT + 0.07, BEAM_Z + WEB, x + 1.6, BEAM_TOP - 0.06, BEAM_Z + WEB + 0.004,
              [None, plate("CAPACITY 5 TON", 1.6, 0.29, "yellow", 24), None, None, None, None])
    labelled(b, TRAINS, lambda t: b.box(8.0, BEAM_BOT + 0.21, BEAM_Z - WEB - 0.004, 10.0, BEAM_TOP - 0.06, BEAM_Z - WEB,
                                        [plate(f"EDG {t} MONORAIL", 2.0, 0.15, "black", 18), None, None, None, None, None]))
    live["hoist"] = {"x0": 0.55, "x1": MONO_W - 0.55, "rail": BEAM_BOT + 0.05, "z": BEAM_Z, "flange": FLANGE, "bar": z0 + 0.04, "drop": 6.5}
    b.live = live
    live["mats"] = {"needle": sw("red"), "body": YELLOW, "chain": DARK, "hook": "stainless", "wheel": DARK, "bag": BLACK}
    return b


BOARDS = {"edg_jw_keepwarm": ("Jacket Water Keep-Warm Skid", 3, 2, 2, jw_keepwarm),
          "edg_lo_keepwarm": ("Lube Oil Keep-Warm Skid", 3, 2, 2, lo_keepwarm),
          "edg_expansion_tank": ("Jacket Water Expansion Tank", 2, 2, 5, expansion_tank),
          "edg_jw_cooler": ("Jacket Water Cooler", 7, 2, 3, jw_cooler),
          "edg_ngr": ("Neutral Grounding Resistor", 2, 2, 2, ngr),
          "edg_gen_cubicle": ("Generator Terminal and CT Cubicle", 3, 2, 3, gen_cubicle),
          "edg_co2_bank": ("CO2 Cylinder Bank", 3, 1, 2, co2_bank),
          "edg_monorail": ("Monorail Hoist", MONO_W, 2, 1, monorail)}
DS.BOARDS.update(BOARDS)
C.SECTIONS.update({k: v[4] for k, v in BOARDS.items()})
