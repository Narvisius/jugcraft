"""The control building's restroom (round 26, #14; Justin: "2 stalls, urinal, 2 sinks", every fixture working
("Flushing", "Sit on the toilets", "Faucets and hand dryer", "Stall doors latch"), the toilet paper and the trash can
working, ceramic tile). The fixtures as real 3D parts, their live states as block states (nuclear/Restroom*.java):

- restroom_toilet: a floor-mounted commercial toilet: elongated bowl on its pedestal, water in the bowl, an open-front
  seat on chrome hinges, the chrome flush valve (control stop, valve body, handle, vacuum breaker, spud), and a twin
  jumbo-roll toilet paper dispenser on its right side (the stall's partition or wall). Variant 0 standard, 1 accessible
  (the seat higher, the bowl 7 px from the side wall, rear and side grab bars). paper 0..8: rolls left to take (two
  jumbo rolls in the dispenser, each good for four); flushing: the water churning, the handle down.
- restroom_urinal: a wall-hung washout urinal with its deodorizer screen and flush valve; flushing.
- restroom_sink: a solid-surface vanity with an undermount bowl, a sensor faucet, the soap spout, the insulated trap
  and supply covers under it (open below, accessible); running: the water stream and the sensor's lamp.
- restroom_hand_dryer: a high-speed hand dryer and its splash guard; running: the lamp, the air.
- restroom_trash_can: a stainless receptacle with its liner; fill 0..3 (paper towels in it).
- restroom_stall_door (and _accessible): toilet partition doors (a vanilla door's states plus latched): pilasters,
  hinges, the latch's VACANT / OCCUPIED indicator outside, the slide latch and coat hook inside.
- restroom_wall_tile / restroom_wall_tile_cap: a wall block tiled on one face (the lower course with its cove base,
  the upper with the accent band and the bullnose cap), painted (or concrete: the window sill) on the others.
- restroom_floor_tile: grey porcelain floor tile.
- plain pieces (plant_panels.RISERS): the mirror, the towel dispenser, the partitions, the fixed front panel, the
  headrail, the urinal screen, the baby changing station, the floor drain, the exhaust grille, the hand washing sign.

Model coordinates are pixels, the piece facing north (the viewer looking north at it), its back (the wall) at z 0.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

import plant_panels as PP

PORC = "pi_rr_porcelain"
SEAT = "pi_rr_seat"
CHROME = "pi_chrome"
SS = "pi_rr_stainless"
PART = "pi_rr_partition"
ALU = "pi_rr_alu"
WATER = "pi_rr_water"
CHURN = "pi_rr_churn"
COUNTER = "pi_rr_counter"
GUARD = "pi_rr_trapguard"
PAPER = "pi_rr_paper"
BLACK = "pi_cap_black"
SENSOR = "pi_rr_sensor"
LAMP_OFF = "pi_rr_lamp_off"
LAMP_RED = "pi_rr_lamp_red"
LAMP_GREEN = "pi_rr_lamp_green"
SMOKE = "pi_rr_smoke"


# ------------------------------------------------------------------------------------------------ round parts
def disc(cx, cz, r, y0, y1, tex):
    """A round part standing up, however thin: an octagon (plant_panels.round_y) when there is height for its steps,
    an eight-pointed star when less, a plain square when very thin."""
    h = y1 - y0
    if h >= 0.7:
        return PP.round_y(cx, cz, r, y0, y1, tex)
    if h >= 0.25:
        return PP.star_y(cx, cz, r * 0.92, y0, y1, tex, inset=0.1)
    s = r * 0.9
    return [(cx - s, y0, cz - s, cx + s, y1, cz + s, tex)]


def round_x(cz, cy, x0, x1, r, tex):
    """A round part lying along x (a paper roll), as touching columns across z."""
    return [(x0, round(cy - h * r, 3), round(cz + a0 * r, 3), x1, round(cy + h * r, 3), round(cz + a1 * r, 3), tex)
            for a0, a1, h in ((-1.0, -0.75, 0.45), (-0.75, -0.35, 0.8), (-0.35, 0.35, 1.0), (0.35, 0.75, 0.8), (0.75, 1.0, 0.45))]


T225 = math.tan(math.radians(22.5))


def _rot(cx, y0, cz, angle):
    return {"rotation": {"angle": angle, "axis": "y", "origin": [cx, y0, cz]}}


def oct_bars(cx, cz, a_in, a_out, y0, y1, tex, which="NESW"):
    """Some of an octagonal ring's eight bars (apothems a_in, a_out) about (cx, cz): N E S W the straight ones, and
    a lower-case letter pair for a diagonal ("ne", "nw", "se", "sw"; turned 45 degrees, 0.1 shorter at both ends so
    no two faces share a plane)."""
    h = a_out * T225 + 0.05
    bars = {"E": (cx + a_in, y0, cz - h, cx + a_out, y1, cz + h, tex), "W": (cx - a_out, y0, cz - h, cx - a_in, y1, cz + h, tex),
            "S": (cx - h, y0, cz + a_in, cx + h, y1, cz + a_out, tex), "N": (cx - h, y0, cz - a_out, cx + h, y1, cz - a_in, tex)}
    out = [bars[k] for k in "NESW" if k in which]
    diag = {"nw": ("N", 45), "ne": ("N", -45), "se": ("S", 45), "sw": ("S", -45)}
    for k, (base, ang) in diag.items():
        if k in which:
            b = bars[base]
            out.append((b[0], y0 + 0.1, b[2], b[3], y1 - 0.1, b[5], tex, _rot(cx, y0, cz, ang)))
    return out


def capsule(cx, z0, z1, a, y0, y1, tex):
    """A solid with straight sides from z0 to z1 and octagonal ends (apothem a): the middle box and the two ends."""
    out = [(cx - a, y0, z0, cx + a, y1, z1, tex)]
    s = a * T225
    for cz, sign in ((z0, -1), (z1, 1)):
        out.append((cx - s, y0 + 0.1, cz - a if sign < 0 else cz, cx + s, y1 - 0.1, cz if sign < 0 else cz + a, tex))
        if y1 - y0 < 1.2:
            continue                       # too low for its turned steps: the ends stay square-cut
        for ang, inset in ((45, 0.2 if sign < 0 else 0.4), (-45, 0.3 if sign < 0 else 0.5)):
            out.append((cx - s, y0 + inset, cz - a, cx + s, y1 - inset, cz + a, tex, _rot(cx, y0, cz, ang)))
    return out


def capsule_ring(cx, z0, z1, a_in, a_out, y0, y1, tex, front=True):
    """The wall of a capsule (front=False: open at the front, a seat): the back end's three bars, the front end's,
    the two long sides."""
    h = a_out * T225 + 0.05
    out = oct_bars(cx, z0, a_in, a_out, y0, y1, tex, "N ne nw")
    out += oct_bars(cx, z1, a_in, a_out, y0, y1, tex, ("S " if front else "") + "se sw")
    out += [(cx + a_in, y0, z0 - h, cx + a_out, y1, z1 + h, tex), (cx - a_out, y0, z0 - h, cx - a_in, y1, z1 + h, tex)]
    return out


def mirror_x(boxes):
    """The same parts mirrored across x 8 (east and west faces swapped)."""
    out = []
    for b in boxes:
        t = b[6]
        if isinstance(t, dict):
            t = {({"east": "west", "west": "east"}.get(k, k)): v for k, v in t.items()}
        out.append((round(16 - b[3], 3), b[1], b[2], round(16 - b[0], 3), b[4], b[5], t) + tuple(b[7:]))
    return out


_TURN = {1: {"east": "north", "north": "west", "west": "south", "south": "east"},
         -1: {"east": "south", "south": "west", "west": "north", "north": "east"}}


def turn_y(boxes, px, pz, sign):
    """The parts swung 90 degrees about a vertical pin at (px, pz): sign 1 takes +z to +x (a door leaf swinging out)."""
    out = []
    for b in boxes:
        x0, y0, z0, x1, y1, z1, t = b[:7]
        pts = []
        for x, z in ((x0, z0), (x1, z1)):
            dx, dz = x - px, z - pz
            pts.append((px + dz, pz - dx) if sign == 1 else (px - dz, pz + dx))
        nx0, nx1 = sorted((pts[0][0], pts[1][0]))
        nz0, nz1 = sorted((pts[0][1], pts[1][1]))
        if isinstance(t, dict):
            t = {(_TURN[sign].get(k, k)): v for k, v in t.items()}
        out.append((round(nx0, 3), y0, round(nz0, 3), round(nx1, 3), y1, round(nz1, 3), t) + tuple(b[7:]))
    return out


# ------------------------------------------------------------------------------------------------ the flush valve
def flush_valve(cx, cz, y0, spud_y, down=False):
    """A chrome flushometer on the wall behind a fixture: the supply out of the wall on the left into the control stop,
    across into the valve body (its cap on top), the handle out of the front, the vacuum breaker and flush tube down to
    the fixture's spud. y0: the valve body's bottom. down: the handle pushed down (flushing)."""
    out = []
    out += PP.star_y(cx, cz, 0.36, spud_y, y0 - 1.6, CHROME)                      # the flush tube
    out += disc(cx, cz, 0.52, y0 - 1.6, y0 - 1.0, CHROME)                          # the vacuum breaker
    out += PP.star_y(cx, cz, 0.36, y0 - 1.0, y0, CHROME)
    out += disc(cx, cz, 0.55, spud_y - 0.35, spud_y + 0.2, CHROME)                 # the spud's coupling nut
    out += PP.round_y(cx, cz, 0.82, y0, y0 + 2.2, CHROME)                         # the valve body
    out += disc(cx, cz, 0.66, y0 + 2.2, y0 + 2.8, CHROME)                          # its cap
    hy = y0 + 0.5
    out += [(cx - 0.45, hy - 0.45, cz + 0.7, cx + 0.45, hy + 0.45, cz + 1.3, CHROME)]   # the handle's coupling nut
    if down:
        out += [(cx - 0.2, hy - 1.3, cz + 1.3, cx + 0.2, hy - 0.2, cz + 2.0, CHROME),
                (cx - 0.3, hy - 2.2, cz + 1.7, cx + 0.3, hy - 1.2, cz + 2.4, CHROME)]
    else:
        out += [(cx - 0.2, hy - 0.2, cz + 1.3, cx + 0.2, hy + 0.2, cz + 3.4, CHROME),
                (cx - 0.3, hy - 0.3, cz + 2.7, cx + 0.3, hy + 0.3, cz + 3.8, CHROME)]
    # The supply: out of the wall (its escutcheon) on the left, the control stop with its cap, across into the body.
    sx, sy = cx - 2.8, y0 + 1.4
    out += [(sx - 0.7, sy - 0.7, 0.0, sx + 0.7, sy + 0.7, 0.2, CHROME),
            (sx - 0.35, sy - 0.35, 0.2, sx + 0.35, sy + 0.35, cz - 0.6, CHROME),
            (sx - 0.8, sy - 0.6, cz - 0.6, sx + 0.8, sy + 0.6, cz + 0.6, CHROME),
            (sx - 0.25, sy + 0.6, cz - 0.25, sx + 0.25, sy + 1.0, cz + 0.25, CHROME),
            (sx + 0.8, sy - 0.25, cz - 0.25, cx - 0.82, sy + 0.25, cz + 0.25, CHROME)]
    return out


# ------------------------------------------------------------------------------------------------ the toilet
TOILET_SEAT = (7.1, 7.7)     # the seat's top, standard and accessible (pixels: 44 and 48 cm)
VALVE_Z = 1.9


def toilet_cx(accessible):
    return 9.0 if accessible else 8.0


def toilet_boxes(accessible=False):
    """The bowl and pedestal, the seat, the flush valve (handle up), the dispenser's housing (rolls apart), the grab
    bars on an accessible one. The water is its own part (flushing or not)."""
    cx = toilet_cx(accessible)
    up = TOILET_SEAT[1] - TOILET_SEAT[0] if accessible else 0.0
    P = PORC
    out = []
    out += capsule(cx, 5.4, 7.2, 2.0, 0.0, 2.4 + up, P)                          # the pedestal
    for sx in (cx - 2.25, cx + 1.95):                                            # the bolt caps
        out.append((sx, 0.5, 5.8, sx + 0.3, 1.2, 6.8, P))
    out += capsule(cx, 6.0, 8.0, 2.5, 2.4 + up, 3.6 + up, P)                     # the trapway under the bowl
    out += capsule_ring(cx, 6.2, 8.8, 2.0, 2.9, 3.6 + up, 5.6 + up, P)           # the bowl
    out += capsule_ring(cx, 6.2, 8.8, 2.15, 3.05, 5.6 + up, 6.4 + up, P)         # the rim
    out.append((cx - 1.1, 4.4 + up, 1.2, cx + 1.1, 6.9 + up, 3.6, P))            # the inlet at the back
    # The open-front seat on its hinges, its bumpers under it.
    out += capsule_ring(cx, 6.4, 8.8, 1.95, 2.95, 6.5 + up, 7.1 + up, SEAT, front=False)
    for hx in (cx - 1.9, cx + 1.4):
        out.append((hx, 6.5 + up, 2.8, hx + 0.5, 7.5 + up, 3.7, CHROME))
    out.append((cx - 1.9, 7.2 + up, 3.0, cx + 1.9, 7.5 + up, 3.4, CHROME))
    out += tp_housing()
    if accessible:
        out += grab_bars()
    return out


def toilet_water(accessible, flushing):
    """The water standing in the bowl (three plates that meet, their corners in the bowl's wall)."""
    cx = toilet_cx(accessible)
    y = 4.6 + (TOILET_SEAT[1] - TOILET_SEAT[0] if accessible else 0.0)
    t = CHURN if flushing else WATER
    return [(cx - 2.0, y, 6.2, cx + 2.0, y + 0.05, 8.8, t), (cx - 1.4, y, 4.6, cx + 1.4, y + 0.05, 6.2, t),
            (cx - 1.4, y, 8.8, cx + 1.4, y + 0.05, 10.4, t)]


def toilet_valve(accessible, flushing):
    cx = toilet_cx(accessible)
    up = TOILET_SEAT[1] - TOILET_SEAT[0] if accessible else 0.0
    return flush_valve(cx, VALVE_Z, 10.6 + up, 6.9 + up, down=flushing)


# The twin jumbo-roll dispenser on the right (x 16: the partition or the side wall): its housing, the smoked windows
# onto the rolls' ends, the tear bar; each roll seen through its window and its tail hanging out under the housing.
TP_ROLLS = (5.2, 9.4)          # the rolls' centres along z
TP_Y = 10.4                    # their axis' height
TP_HIT = (13.3, 7.0, 3.2, 16.0, 12.9, 12.4)   # where a click takes paper (nuclear/RestroomToiletBlock.java)


def tp_housing():
    out = [(13.9, 8.6, 3.4, 16.0, 12.6, 12.2, SS),
           (13.6, 8.9, 3.7, 13.9, 12.3, 11.9, {"*": SS, "west": "pi_rr_tp_front!"}),
           (13.7, 8.4, 3.6, 13.95, 8.6, 12.0, CHROME),                           # the tear bar
           (14.6, 12.6, 7.4, 15.2, 12.75, 8.2, CHROME)]                          # the lock
    for cz in TP_ROLLS:
        out.append((13.55, 9.4, cz - 0.9, 13.6, 11.4, cz + 0.9, SMOKE))
    return out


def tp_roll(k):
    cz = TP_ROLLS[k]
    out = [b for b in round_x(cz, TP_Y, 14.0, 15.8, 1.75, PAPER)]
    out.append((13.95, 9.3, cz - 0.7, 14.0, 11.5, cz + 0.7, {"*": PAPER, "west": "pi_rr_tp_end!"}))
    out.append((13.98, 7.3, cz - 1.1, 14.04, 8.6, cz + 1.1, PAPER))                 # the tail hanging out
    return out


def grab_bars():
    """The accessible stall's grab bars, 36 in. on the rear wall over the flush valve, 42 in. on the side wall, 34 in.
    up: stainless, on round flanges, their ends returning to the wall."""
    y0, y1 = 14.4, 15.0
    out = [(2.0, y0, 1.1, 14.6, y1, 1.7, SS),
           (1.4, y0, 0.25, 2.0, y1, 1.7, SS), (14.6, y0, 0.25, 15.2, y1, 1.7, SS),
           (1.0, y0 - 0.4, 0.0, 2.4, y1 + 0.4, 0.25, SS), (14.2, y0 - 0.4, 0.0, 15.6, y1 + 0.4, 0.25, SS)]
    out += [(14.8, y0, 2.4, 15.4, y1, 14.6, SS),
            (14.8, y0, 1.8, 15.75, y1, 2.4, SS), (14.8, y0, 14.6, 15.75, y1, 15.2, SS),
            (15.75, y0 - 0.4, 1.4, 16.0, y1 + 0.4, 2.8, SS), (15.75, y0 - 0.4, 14.2, 16.0, y1 + 0.4, 15.6, SS)]
    return out


# ------------------------------------------------------------------------------------------------ the urinal
def urinal_boxes():
    """A wall-hung washout urinal: the basin and its lip (round at the front), the sides leaning back to the wall as
    they rise, the back inside, the rounded hood; the deodorizer screen in the basin; the spud on top."""
    P = PORC
    out = [(4.9, 6.6, 0.0, 11.1, 16.8, 0.6, P)]                                  # against the wall
    out += disc(8.0, 1.2, 3.0, 6.4, 7.2, P)                                       # the basin (its back in the wall)
    out += oct_bars(8.0, 1.2, 2.4, 3.1, 7.2, 8.0, P, "ES W se sw")               # its lip
    lean = {"rotation": {"angle": -12, "axis": "x", "origin": [8, 7.6, 0.6]}}
    for x0, x1 in ((4.9, 5.7), (10.3, 11.1)):
        out.append((x0, 7.6, -0.4, x1, 16.2, 4.4, P, lean))
    out.append((5.7, 7.2, 0.6, 10.3, 15.8, 1.0, P))
    out += PP.round_y(8.0, 0.6, 3.1, 15.8, 16.8, P)                              # the hood
    out += [(6.4, 7.2, 1.0, 9.6, 7.42, 3.4, {"*": "pi_rr_screen_edge", "up": "pi_rr_urinal_screen!"})]   # the screen
    out += [(7.6, 16.8, 1.5, 8.4, 17.2, 2.3, CHROME)]                            # the spud
    return out


def urinal_valve(flushing):
    return flush_valve(8.0, VALVE_Z, 19.4, 17.2, down=flushing)


def urinal_water(flushing):
    """The water standing in the trap, churning while it flushes, and running down the back."""
    out = [(6.0, 7.25, 1.0, 10.0, 7.3, 3.6, CHURN if flushing else WATER)]
    if flushing:
        out.append((5.8, 7.4, 1.0, 10.2, 15.6, 1.08, WATER))
    return out


# ------------------------------------------------------------------------------------------------ the vanity
SINK = (8.0, 4.5, 2.3)              # the bowl's opening (a regular octagon): centre x, z, apothem
TOP = 13.8                         # the counter's top (86 cm)
SPOUT = (8.0, 3.4, 15.35)          # the faucet's aerator: x, z, its bottom


def sink_boxes():
    cx, cz, a = SINK
    x0, x1, z0, z1 = cx - a, cx + a, cz - a, cz + a
    out = [(0.0, 13.0, 0.0, x0, TOP, 8.8, COUNTER), (x1, 13.0, 0.0, 16.0, TOP, 8.8, COUNTER),
           (x0, 13.0, 0.0, x1, TOP, z0, COUNTER), (x0, 13.0, z1, x1, TOP, 8.8, COUNTER)]
    # The opening's cut corners: squares turned 45 degrees on the corners (0.12 below the top, under the counter's).
    w = a * (1 - T225) / math.sqrt(2)
    for px, pz in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        out.append((px - w, 13.2, pz - w, px + w, TOP - 0.12, pz + w, COUNTER, _rot(px, 13.2, pz, 45)))
    out += [(0.0, 11.6, 8.8, 16.0, TOP, 9.3, COUNTER), (0.0, TOP, 0.0, 16.0, 15.4, 0.5, COUNTER)]
    # The bowl under the counter.
    out += PP.ring_y(cx, cz, a - 0.1, a + 0.3, 12.2, 13.0, PORC)
    out += PP.ring_y(cx, cz, a - 0.7, a - 0.1, 11.4, 12.2, PORC)
    out += disc(cx, cz, a - 0.7, 10.9, 11.4, PORC)
    out += disc(cx, cz, 0.45, 11.4, 11.46, CHROME)                                # the drain
    # The sensor faucet: its base, the column, the gooseneck over the bowl, the aerator; the sensor's window.
    out += disc(cx, 1.4, 0.55, TOP, TOP + 0.5, CHROME)
    out += PP.round_y(cx, 1.4, 0.38, TOP + 0.5, 16.6, CHROME)
    out += [(cx - 0.3, 16.2, 1.0, cx + 0.3, 16.8, 3.7, CHROME), (cx - 0.35, 15.5, 3.05, cx + 0.35, 16.2, 3.75, CHROME),
            (cx - 0.25, 15.35, 3.15, cx + 0.25, 15.5, 3.65, BLACK),
            (cx - 0.25, 14.6, 1.76, cx + 0.25, 15.0, 1.84, SENSOR)]
    # The soap spout to the right.
    out += disc(12.6, 1.6, 0.45, TOP, TOP + 0.4, CHROME)
    out += PP.star_y(12.6, 1.6, 0.3, TOP + 0.4, 15.2, CHROME)
    out += [(12.42, 15.0, 1.4, 12.78, 15.3, 2.8, CHROME), (12.3, 15.2, 1.25, 12.9, 15.5, 1.95, CHROME)]
    # Under the counter: the insulated trap and supply covers, the angle stops; the counter's brackets.
    out += PP.round_y(cx, cz, 0.55, 8.6, 10.9, GUARD)
    out += [(cx - 0.55, 8.0, 0.0, cx + 0.55, 9.1, cz - 0.5, GUARD)]
    for sx in (cx - 1.8, cx + 1.8):
        out += [(sx - 0.45, 9.2, 0.0, sx + 0.45, 10.0, 1.3, CHROME), (sx - 0.3, 10.0, 0.6, sx + 0.3, 13.0, 1.2, GUARD)]
    for bx in (0.6, 14.9):
        out += [(bx, 8.4, 0.0, bx + 0.5, 13.0, 0.5, "pi_steel_dark"), (bx, 12.4, 0.5, bx + 0.5, 13.0, 8.2, "pi_steel_dark"),
                (bx, 10.4, 0.5, bx + 0.5, 11.0, 2.6, "pi_steel_dark"), (bx, 11.0, 2.2, bx + 0.5, 11.6, 4.6, "pi_steel_dark"),
                (bx, 11.6, 4.2, bx + 0.5, 12.4, 6.6, "pi_steel_dark")]
    return out


def sink_live(running):
    cx = SINK[0]
    out = [(cx - 0.12, 15.04, 1.76, cx + 0.12, 15.24, 1.86, LAMP_RED if running else LAMP_OFF)]
    if running:
        x, z, y = SPOUT
        out.append((x - 0.13, 11.46, z - 0.13, x + 0.13, y, z + 0.13, WATER))
        out += [(x - 0.7, 11.46, z + 0.3, x + 0.7, 11.5, z + 1.5, CHURN)]
    return out


def mirror_boxes():
    """The mirror over a sink (its bottom at 1 m): stainless channel frame, the glass."""
    out = [(1.0, 0.4, 0.0, 15.0, 0.9, 0.55, SS), (1.0, 14.1, 0.0, 15.0, 14.6, 0.55, SS),
           (1.0, 0.9, 0.0, 1.5, 14.1, 0.55, SS), (14.5, 0.9, 0.0, 15.0, 14.1, 0.55, SS),
           (1.5, 0.9, 0.05, 14.5, 14.1, 0.4, {"*": SS, "south": "pi_rr_mirror!"})]
    return out


# ------------------------------------------------------------------------------------------------ dryer, towels, bin
def dryer_boxes():
    """A high-speed hand dryer (1.1 m up): brushed stainless cover rounded at its sides, the air nozzle under it, the
    sensor window; the stainless splash guard on the wall below."""
    out = []
    for x0, x1, d, inset in ((5.2, 5.6, 2.2, 0.3), (5.6, 6.2, 2.6, 0.1), (6.2, 9.8, 2.8, 0.0), (9.8, 10.4, 2.6, 0.1),
                             (10.4, 10.8, 2.2, 0.3)):
        face = {"*": SS, "south": "pi_rr_dryer_front"}
        out.append((x0, 2.0 + inset, 0.0, x1, 7.4 - inset, d, face))
    out += [(6.6, 1.5, 0.8, 9.4, 2.0, 2.3, BLACK), (7.0, 1.4, 1.2, 9.0, 1.5, 2.0, "pi_rr_nozzle"),
            (7.3, 2.4, 2.8, 8.7, 2.9, 2.86, SENSOR)]
    out.append((4.4, -6.0, 0.0, 11.6, 1.8, 0.2, {"*": SS, "south": "pi_rr_guard!"}))
    return out


def dryer_live(running):
    return [(7.82, 6.6, 2.8, 8.18, 6.9, 2.9, LAMP_GREEN if running else LAMP_OFF)]


def towel_boxes():
    """A folded paper towel dispenser: the stainless cabinet, its front with the level window and lock, a towel
    hanging out of the bottom."""
    return [(5.4, 2.0, 0.0, 10.6, 9.0, 1.7, SS),
            (5.6, 2.2, 1.7, 10.4, 8.8, 1.85, {"*": SS, "south": "pi_rr_towel_front!"}),
            (6.6, 0.9, 0.95, 9.4, 2.0, 1.05, {"*": PAPER, "south": "pi_rr_towel!", "north": "pi_rr_towel!"})]


def bin_boxes():
    """A round stainless receptacle with its liner folded over the rim."""
    out = PP.ring_y(8.0, 8.0, 2.55, 2.85, 0.0, 7.9, SS)
    out += [(6.2, 0.0, 6.2, 9.8, 0.5, 9.8, "pi_rr_liner")]
    out += PP.ring_y(8.0, 8.0, 2.5, 2.95, 7.9, 8.3, "pi_rr_liner")
    out += [(6.9, 4.6, 10.85, 9.1, 5.6, 10.92, {"*": SS, "south": "pi_rr_bin_label!"})]
    return out


def _clear_of(b, others, gap=0.12):
    """No face of b within gap of a face of another box it overlaps, facing the same way."""
    for o in others:
        over = [min(b[i + 3], o[i + 3]) - max(b[i], o[i]) for i in range(3)]
        for axis in range(3):
            if all(over[j] > -gap for j in range(3) if j != axis):
                if abs(b[axis] - o[axis]) < gap or abs(b[axis + 3] - o[axis + 3]) < gap:
                    return False
    return True


def bin_fill(level):
    """Crumpled paper towels in the bin, to a third, two thirds or the top: lumps of different sizes, no two of their
    faces in one plane."""
    if level <= 0:
        return []
    rng = random.Random(1400 + level)
    top = (2.6, 5.2, 7.6)[level - 1]
    n = 6 + 6 * level
    out = []
    for _ in range(6000):
        if len(out) == n:
            break
        k = len(out)
        r = rng.uniform(0.8, 1.2)
        a = rng.uniform(0, 2 * math.pi)
        d = rng.uniform(0.4, 2.3 - r)
        x, z = 8.0 + d * math.cos(a), 8.0 + d * math.sin(a)
        y = 0.62 + (top - 0.62) * (k + 1) / n
        b = (round(x - r, 2), round(max(0.62 + rng.uniform(0, 0.5), y - r), 2), round(z - r, 2), round(x + r, 2),
             round(min(top - rng.uniform(0, 0.3), y + r * 0.8), 2), round(z + r, 2))
        if b[4] - b[1] > 0.3 and _clear_of(b, out):
            out.append(b)
    return [b + ("pi_rr_crumple",) for b in out]


# ------------------------------------------------------------------------------------------------ the stalls
PANEL_Y = (4.8, 28.5)        # the partitions' and doors' bottom and top (30 cm and 1.78 m over the floor)
PIL_W = 1.6                  # a pilaster's width
FRONT = (0.0, 0.7)           # the front's thickness (the door frame: x from the stall's side)


def partition_boxes(end=None):
    """A partition panel along the block's back edge (z 0.3..0.8), 16 wide, its bottom and top as PANEL_Y. end: "wall"
    or "pilaster" brackets at x 0 (mirror the piece for x 16)."""
    y0, y1 = PANEL_Y
    out = [(0.0, y0, 0.3, 16.0, y1, 0.8, {"*": "pi_rr_partition_edge", "north": PART, "south": PART})]
    if end == "wall":
        for ya in (y0 + 2.0, y1 - 4.0):
            out += [(0.0, ya, 0.0, 1.0, ya + 2.0, 0.3, SS), (0.0, ya, 0.8, 1.0, ya + 2.0, 1.1, SS)]
    elif end == "pilaster":
        for ya in (y0 + 2.0, y1 - 4.0):
            out += [(0.0, ya, 0.8, 1.2, ya + 1.6, 1.1, SS)]
    return out


def front_panel_boxes():
    """The fixed front panel beside the accessible stall's door (east of the aisle's block): the panel up to the
    pilaster's height, a wall channel at x 16 (the facing-east frame: x along the front, z 0 the front line)."""
    y0, y1 = PANEL_Y
    out = [(0.0, y0, 0.0, 15.6, y1, 0.7, {"*": "pi_rr_partition_edge", "north": PART, "south": PART}),
           (15.6, 2.0, 0.0, 16.0, 30.0, 1.1, SS),
           (0.0, 0.0, 0.0, 1.0, 32.0, 0.7, PART), (0.0, 0.0, -0.1, 1.1, 1.2, 0.8, SS)]
    return out


def headrail_boxes(end=None):
    """The anodised aluminium headrail over the stall fronts, on top of the pilasters (2 m up): along x, at the
    block's back edge. end: "cap" at x 0, "wall" (its bracket) at x 16."""
    out = [(0.0, 0.0, 0.0, 16.0, 0.8, 0.7, {"*": ALU, "north": "pi_rr_alu_side", "south": "pi_rr_alu_side"})]
    if end == "cap":
        out.append((-0.05, -0.05, -0.05, 0.25, 0.85, 0.75, "pi_rr_alu_side"))
    elif end == "wall":
        out.append((15.4, -0.3, 0.0, 16.0, 1.1, 1.0, ALU))
    return out


def screen_boxes():
    """The privacy screen beside the urinal: a panel standing out of the wall (along z) at the block's west edge,
    1.4 m tall, on two wall brackets."""
    return [(0.3, 5.6, 0.0, 0.8, 22.6, 7.4, {"*": "pi_rr_partition_edge", "east": PART, "west": PART}),
            (0.0, 7.6, 0.0, 1.1, 9.6, 1.0, SS), (0.0, 18.6, 0.0, 1.1, 20.6, 1.0, SS)]


# The doors (vanilla door frame: drawn facing east, the front line at x 0, the aisle toward +x; a left hinge at z 0).
DOOR_LEAF = (0.15, 0.65)     # the leaf's thickness (x)
LEAF_Z = (PIL_W + 0.1, 16 - PIL_W - 0.1)


def _pilasters(lower):
    out = []
    for z0, z1 in ((0.0, PIL_W), (16 - PIL_W, 16.0)):
        out.append((0.0, 0.0, z0, FRONT[1], 16.0, z1, PART))
        if lower:
            out.append((-0.1, 0.0, z0 - 0.0, FRONT[1] + 0.1, 1.2, z1, SS))
    return out


def door_leaf(lower, hinge, latched, accessible):
    """The leaf and its hardware, closed (the hinge's pin at x 0.65, z LEAF_Z[0] for a left hinge)."""
    y0, y1 = (PANEL_Y[0], 16.0) if lower else (0.0, PANEL_Y[1] - 16.0)
    x0, x1 = DOOR_LEAF
    z0, z1 = LEAF_Z
    out = [(x0, y0, z0, x1, y1, z1, {"*": "pi_rr_partition_edge", "east": PART, "west": PART})]
    hz = z0 if hinge == "left" else z1
    lz = z1 - 1.4 if hinge == "left" else z0 + 1.4         # the latch's place
    s = 1 if hinge == "left" else -1
    # The wrap-around hinges, on the leaf and the pilaster.
    for ya in ((7.0,) if lower else (8.4,)):
        out.append((x0 - 0.1, ya, hz - 0.5, x1 + 0.1, ya + 1.6, hz + 0.5, CHROME))
    if lower:
        # Outside: the indicator over the latch, a bumper; inside: the slide latch and its keeper.
        ind = "pi_rr_occupied" if latched else "pi_rr_vacant"
        out += [(x1, 13.9, lz - 1.1, x1 + 0.2, 15.5, lz + 1.1, CHROME),
                (x1 + 0.2, 14.3, lz - 1.0, x1 + 0.24, 15.1, lz + 1.0, {"*": CHROME, "east": ind + "!"}),
                (x0 - 0.35, 14.2, lz - 1.3, x0, 15.2, lz + 1.3, CHROME),
                (x0 - 0.6, 14.5, lz - 0.2, x0 - 0.35, 14.9, lz + 0.2, CHROME)]
        if accessible:
            # The pulls: a U-pull outside by the latch, one inside by the hinge (to close it behind you).
            out += [(x1, 10.0, lz - 0.2, x1 + 0.9, 10.4, lz + 0.2, SS), (x1, 13.2, lz - 0.2, x1 + 0.9, 13.6, lz + 0.2, SS),
                    (x1 + 0.6, 10.0, lz - 0.2, x1 + 0.9, 13.6, lz + 0.2, SS)]
            pz = hz + 2.5 * s
            out += [(x0 - 0.9, 10.0, pz - 0.2, x0, 10.4, pz + 0.2, SS), (x0 - 0.9, 13.2, pz - 0.2, x0, 13.6, pz + 0.2, SS),
                    (x0 - 0.9, 10.0, pz - 0.2, x0 - 0.6, 13.6, pz + 0.2, SS)]
    else:
        # Inside: the coat hook and its rubber bumper; outside on the accessible door, its sign.
        mz = (z0 + z1) / 2
        out += [(x0 - 0.9, 7.8, mz - 0.3, x0, 8.2, mz + 0.3, CHROME), (x0 - 0.9, 8.2, mz - 0.3, x0 - 0.6, 9.2, mz + 0.3, CHROME)]
        if accessible:
            out.append((x1, 3.0, mz - 1.6, x1 + 0.12, 6.2, mz + 1.6, {"*": "pi_pnl_white", "east": "pi_rr_isa!"}))
    return out


def door_boxes(lower, hinge, opened, latched, accessible):
    out = _pilasters(lower)
    leaf = door_leaf(lower, hinge, latched and not opened, accessible)
    if opened:
        z0, z1 = LEAF_Z
        pin_z = z0 if hinge == "left" else z1
        leaf = turn_y(leaf, DOOR_LEAF[1], pin_z, 1 if hinge == "left" else -1)
    return out + leaf


# ------------------------------------------------------------------------------------------------ the room's pieces
def changing_boxes():
    """A wall-mounted baby changing station, folded up (its bed 86 cm up when down): moulded grey body, the front
    with its pictogram and instructions, the handle."""
    return [(1.2, 13.0, 0.0, 14.8, 13.4, 1.2, "pi_rr_koala"),
            (1.2, 13.4, 0.0, 14.8, 22.4, 1.5, "pi_rr_koala"),
            (1.6, 13.6, 1.5, 14.4, 22.2, 1.7, {"*": "pi_rr_koala", "south": "pi_rr_changing!"}),
            (4.6, 21.4, 1.7, 11.4, 22.0, 2.4, "pi_rr_koala_dark"), (4.6, 20.4, 2.0, 5.2, 21.4, 2.4, "pi_rr_koala_dark"),
            (10.8, 20.4, 2.0, 11.4, 21.4, 2.4, "pi_rr_koala_dark")]


def floor_drain_boxes():
    """A floor drain in the tile: the tile with the grate's opening, the chrome grate a hair below the floor."""
    out = [(0, 0, 0, 16, 15.4, 16, "restroom_floor_tile")]
    for x0, x1, z0, z1 in ((0, 16, 0, 5.4), (0, 16, 10.6, 16), (0, 5.4, 5.4, 10.6), (10.6, 16, 5.4, 10.6)):
        out.append((x0, 15.4, z0, x1, 16.0, z1, {"*": "restroom_floor_tile"}))
    out += [(5.4, 15.4, 5.4, 10.6, 15.85, 10.6, {"*": CHROME, "up": "pi_rr_grate!"})]
    return out


def exhaust_boxes():
    """The restroom's ceiling exhaust grille in a ceiling tile (an exhaust fan's grille: louvres, a frame)."""
    out = [(0, 0.6, 0, 16, 16, 16, {"*": "ceiling_tile"})]
    out += [(2.0, 0.0, 2.0, 14.0, 0.6, 14.0, {"*": "pi_pnl_white", "down": "pi_rr_exhaust!"})]
    return out


SIGN_TEX = "pi_rr_sign_wash"


def wash_sign_boxes():
    return PP._sign_boxes(SIGN_TEX)


# ------------------------------------------------------------------------------------------------ textures
K = 256


def _solid(rgb, seed, dirt=0.03, size=32):
    return PP._solid(rgb, seed, dirt)


def _brushed(base=(196, 198, 196), seed=1401, size=64):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), base + (255,))
    d = ImageDraw.Draw(img)
    for y in range(size):
        v = rng.randint(-10, 10)
        d.line([(0, y), (size, y)], fill=tuple(max(0, min(255, c + v)) for c in base) + (255,))
    return img


def _porcelain():
    img = Image.new("RGBA", (64, 64), (244, 244, 240, 255))
    d = ImageDraw.Draw(img)
    for k in range(64):
        d.line([(0, k), (64, k)], fill=(246 - k // 12, 246 - k // 12, 242 - k // 12, 255))
    return img


def _water(churn=False, alpha=120):
    img = Image.new("RGBA", (64, 64), (176, 214, 222, alpha))
    d = ImageDraw.Draw(img)
    rng = random.Random(1402 if churn else 1403)
    if churn:
        for k in range(14):
            cx, cy, r = rng.randint(4, 60), rng.randint(4, 60), rng.randint(4, 14)
            d.arc([cx - r, cy - r, cx + r, cy + r], rng.randint(0, 360), rng.randint(0, 360) + 140, fill=(246, 250, 252, 200), width=3)
    else:
        for k in range(6):
            y = rng.randint(4, 60)
            d.line([(rng.randint(0, 20), y), (rng.randint(40, 64), y + rng.randint(-2, 2))], fill=(210, 236, 242, alpha + 30), width=1)
    return img


def _partition():
    img = Image.new("RGBA", (64, 64), (194, 190, 178, 255))
    rng = random.Random(1404)
    px = img.load()
    for _ in range(500):
        x, y = rng.randrange(64), rng.randrange(64)
        v = rng.randint(-8, 8)
        r, g, b, a = px[x, y]
        px[x, y] = (r + v, g + v, b + v, a)
    return img


def _counter():
    """Solid surface: pale warm grey with a fine, quiet fleck."""
    img = Image.new("RGBA", (64, 64), (222, 220, 214, 255))
    rng = random.Random(1405)
    d = ImageDraw.Draw(img)
    for _ in range(140):
        x, y = rng.randrange(64), rng.randrange(64)
        c = rng.choice([(200, 198, 192), (210, 208, 202), (234, 232, 228)])
        d.point((x, y), fill=c + (255,))
    return img


def _mirror():
    """The mirror: a cool grey-blue sheen, lighter toward the top, two soft diagonal glints, a thin bevel."""
    W, H = 256, 256
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(170 + 40 * (1 - t)), int(186 + 34 * (1 - t)), int(196 + 30 * (1 - t)), 255))
    glint = Image.new("L", (W, H), 0)
    gd = ImageDraw.Draw(glint)
    for off, w in ((40, 26), (96, 12)):
        gd.polygon([(off, H), (off + w, H), (off + w + 150, 0), (off + 150, 0)], fill=70)
    glint = glint.filter(ImageFilter.GaussianBlur(10))
    img = Image.composite(Image.new("RGBA", (W, H), (236, 244, 248, 255)), img, glint)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], outline=(140, 150, 156, 255), width=3)
    return img


def _text_sign(lines, w=512, h=320, bg=(250, 250, 246), fg=(24, 24, 26), band=None):
    img = Image.new("RGBA", (w, h), bg + (255,))
    d = ImageDraw.Draw(img)
    top = 0
    if band:
        colour, text = band
        d.rectangle([0, 0, w - 1, 84], fill=colour + (255,))
        PP._fit_line(d, (w // 2, 42), text, 58, w - 40, (250, 250, 246, 255))
        top = 84
    n = len(lines)
    step = (h - top - 20) / n
    for k, line in enumerate(lines):
        PP._fit_line(d, (w // 2, int(top + 10 + step * (k + 0.5))), line, 44, w - 40, fg + (255,))
    d.rectangle([0, 0, w - 1, h - 1], outline=(40, 40, 42, 255), width=6)
    return img


def _wash_sign():
    """The hand washing notice, drawn as plant_panels' signs are: 433 by 512 px in a 512 px square."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    BLUE, BLACK, WHITE = (30, 70, 140, 255), (20, 20, 22, 255), (250, 250, 246, 255)
    d.rectangle([0, 0, W - 1, H - 1], fill=WHITE, outline=BLACK, width=10)
    d.rectangle([10, 10, W - 11, 120], fill=BLUE)
    PP._fit_line(d, (W // 2, 65), "NOTICE", 72, W - 80, WHITE)
    for y, line, size in ((196, "EMPLOYEES", 62), (268, "MUST WASH", 62), (340, "HANDS", 62), (414, "BEFORE RETURNING", 40),
                          (462, "TO WORK", 40)):
        PP._fit_line(d, (W // 2, y), line, size, W - 56, BLACK)
    return img


def _indicator(word, colour):
    img = Image.new("RGBA", (192, 80), (30, 30, 32, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 185, 73], fill=colour + (255,))
    PP._fit_line(d, (96, 40), word, 40, 168, (250, 250, 246, 255))
    return img


def _isa():
    """The International Symbol of Access: white on blue."""
    img = Image.new("RGBA", (128, 128), (20, 70, 160, 255))
    d = ImageDraw.Draw(img)
    W = (250, 250, 250, 255)
    d.ellipse([54, 14, 70, 30], fill=W)                                  # head
    d.line([(60, 34), (56, 66)], fill=W, width=9)                        # back
    d.line([(56, 64), (86, 64)], fill=W, width=8)                        # seat
    d.line([(86, 64), (96, 92)], fill=W, width=8)                        # leg
    d.line([(58, 46), (80, 46)], fill=W, width=7)                        # arm
    d.arc([22, 54, 82, 114], -10, 260, fill=W, width=8)                  # wheel, open where the body meets it
    return img


def _tp_front():
    img = _brushed((200, 202, 200), 1406, 128)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([4, 4, 123, 123], 10, outline=(150, 152, 150, 255), width=3)
    d.ellipse([58, 6, 70, 18], fill=(90, 92, 94, 255))
    return img


def _tp_end():
    img = Image.new("RGBA", (64, 64), (246, 246, 240, 255))
    d = ImageDraw.Draw(img)
    for r in range(4, 32, 3):
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], outline=(228, 228, 220, 255), width=1)
    d.ellipse([22, 22, 42, 42], fill=(170, 130, 90, 255))
    d.ellipse([27, 27, 37, 37], fill=(60, 50, 40, 255))
    return img


def _urinal_screen():
    img = Image.new("RGBA", (128, 128), (40, 110, 200, 255))
    d = ImageDraw.Draw(img)
    for y in range(8, 128, 12):
        for x in range(8 + (y // 12 % 2) * 6, 128, 12):
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(20, 70, 150, 255))
    d.ellipse([46, 46, 82, 82], fill=(230, 240, 250, 255))
    return img


def _dryer_front():
    """The dryer's cover: brushed stainless with a pressed line round its face."""
    img = _brushed((206, 208, 206), 1407, 128)
    d = ImageDraw.Draw(img)
    d.rectangle([42, 20, 86, 112], outline=(176, 178, 176, 255), width=2)
    return img


def _towel_front():
    img = _brushed((204, 206, 204), 1408, 128)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([54, 26, 74, 98], 4, fill=(60, 62, 66, 255))     # the level window
    d.rectangle([58, 60, 70, 96], fill=(236, 232, 220, 255))             # towels to halfway
    d.ellipse([58, 8, 70, 20], fill=(90, 92, 94, 255))                   # the lock
    PP._fit_line(d, (64, 114), "PAPER TOWELS", 13, 110, (60, 62, 66, 255))
    return img


def _towel():
    img = Image.new("RGBA", (64, 64), (236, 232, 220, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 8):
        d.line([(x, 0), (x, 64)], fill=(222, 218, 204, 255), width=1)
    return img


def _changing():
    """The changing station's front: a pictogram (an adult changing a baby on a table) and its instructions."""
    img = Image.new("RGBA", (256, 192), (186, 190, 194, 255))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([8, 8, 247, 183], 18, outline=(150, 154, 158, 255), width=4)
    d.rounded_rectangle([88, 24, 168, 104], 10, fill=(30, 70, 140, 255))
    W = (250, 250, 250, 255)
    d.ellipse([100, 36, 114, 50], fill=W)                                # the adult's head
    d.line([(107, 52), (107, 82)], fill=W, width=7)
    d.line([(107, 60), (128, 70)], fill=W, width=5)
    d.line([(116, 74), (156, 74)], fill=W, width=5)                      # the table
    d.ellipse([140, 62, 150, 72], fill=W)                                # the baby's head
    d.line([(122, 68), (140, 68)], fill=W, width=6)
    PP._fit_line(d, (128, 124), "BABY CHANGING STATION", 22, 220, (40, 42, 46, 255))
    PP._fit_line(d, (128, 148), "PULL DOWN TO OPEN", 17, 200, (40, 42, 46, 255))
    PP._fit_line(d, (128, 168), "USE THE SAFETY STRAP", 17, 200, (40, 42, 46, 255))
    return img


def _grate():
    img = Image.new("RGBA", (128, 128), (190, 192, 194, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 123, 123], fill=(160, 162, 164, 255), outline=(120, 122, 124, 255), width=4)
    for k in range(-3, 4):
        d.rounded_rectangle([60 + k * 14 - 4, 22, 60 + k * 14 + 4, 106], 3, fill=(30, 30, 32, 255))
    return img


def _exhaust():
    img = Image.new("RGBA", (128, 128), (238, 238, 234, 255))
    d = ImageDraw.Draw(img)
    for k in range(10, 120, 10):
        d.line([(10, k), (118, k)], fill=(150, 152, 150, 255), width=4)
    d.rectangle([2, 2, 125, 125], outline=(200, 200, 196, 255), width=4)
    return img


def _guard():
    img = _brushed((200, 202, 200), 1409, 128)
    return img


def _bin_label():
    img = Image.new("RGBA", (128, 48), (200, 202, 200, 255))
    d = ImageDraw.Draw(img)
    PP._fit_line(d, (64, 24), "PAPER TOWELS ONLY", 20, 120, (40, 42, 46, 255))
    return img


def _crumple():
    img = Image.new("RGBA", (32, 32), (232, 228, 214, 255))
    d = ImageDraw.Draw(img)
    rng = random.Random(1410)
    for _ in range(18):
        x, y = rng.randrange(32), rng.randrange(32)
        d.line([(x, y), (x + rng.randint(-8, 8), y + rng.randint(-8, 8))], fill=(206, 200, 184, 255), width=1)
    return img


def _wall_tile(cap):
    """Glazed 4-1/4 in. wall tile, white with grey grout; the lower course's cove base; the upper's accent band of
    blue-grey tile and the bullnose cap along its top."""
    img = Image.new("RGBA", (128, 128), (132, 134, 132, 255))
    d = ImageDraw.Draw(img)
    rng = random.Random(1411 + cap)
    for r in range(8):
        for c in range(8):
            y0 = r * 16
            col = (240, 240, 234)
            if cap and r == 5:
                col = (126, 146, 164)
            v = rng.randint(-4, 4)
            col = tuple(min(255, max(0, k + v)) for k in col)
            d.rectangle([c * 16 + 1, y0 + 1, c * 16 + 15, y0 + 15], fill=col + (255,))
            d.line([(c * 16 + 2, y0 + 2), (c * 16 + 13, y0 + 2)], fill=tuple(min(255, k + 10) for k in col) + (255,))
    if cap:
        d.rectangle([0, 0, 127, 7], fill=(236, 236, 230, 255))
        d.line([(0, 6), (127, 6)], fill=(200, 200, 194, 255), width=2)
        d.line([(0, 0), (127, 0)], fill=(250, 250, 246, 255), width=1)
    else:
        d.rectangle([0, 116, 127, 127], fill=(110, 112, 114, 255))
        d.line([(0, 117), (127, 117)], fill=(140, 142, 144, 255), width=2)
    return img


def _floor_tile():
    """12 in. grey porcelain floor tile, dark grout."""
    img = Image.new("RGBA", (128, 128), (88, 88, 86, 255))
    d = ImageDraw.Draw(img)
    rng = random.Random(1412)
    for r in range(4):
        for c in range(4):
            base = 150 + rng.randint(-6, 6)
            d.rectangle([c * 32 + 1, r * 32 + 1, c * 32 + 30, r * 32 + 30], fill=(base, base, base - 4, 255))
            for _ in range(20):
                x, y = c * 32 + rng.randint(2, 29), r * 32 + rng.randint(2, 29)
                v = base + rng.randint(-18, 12)
                d.point((x, y), fill=(v, v, v - 4, 255))
    return img


SHARP = {"pi_rr_mirror", SIGN_TEX, "pi_rr_vacant", "pi_rr_occupied", "pi_rr_isa", "pi_rr_tp_front", "pi_rr_tp_end",
         "pi_rr_urinal_screen", "pi_rr_dryer_front", "pi_rr_towel_front", "pi_rr_changing", "pi_rr_grate", "pi_rr_exhaust", "pi_rr_bin_label", "restroom_wall_tile",
         "restroom_wall_tile_cap", "restroom_floor_tile", "pi_rr_water", "pi_rr_churn", "pi_rr_smoke",
         "pi_rr_guard"}
SEE_THROUGH = {"pi_rr_water", "pi_rr_churn", "pi_rr_smoke"}


def _square(make, size):
    """A picture drawn in its face's proportions, stretched to the square texture the game wants (the face's full UV
    squeezes it back)."""
    return lambda: make().resize((size, size), Image.LANCZOS)


def makers():
    S = PP._solid
    return {
        PORC: _porcelain, SEAT: lambda: S((228, 226, 218), 1420, 0.01), SS: lambda: _brushed(),
        PART: _partition, "pi_rr_partition_edge": lambda: S((170, 166, 156), 1421, 0.02),
        ALU: lambda: _brushed((206, 208, 210), 1422), "pi_rr_alu_side": lambda: S((186, 188, 190), 1423, 0.02),
        WATER: lambda: _water(False), CHURN: lambda: _water(True, 170),
        "pi_rr_smoke": lambda: Image.new("RGBA", (32, 32), (60, 62, 66, 150)),
        COUNTER: _counter, GUARD: lambda: S((236, 236, 232), 1424, 0.02), PAPER: lambda: S((246, 246, 240), 1425, 0.01),
        SENSOR: lambda: S((70, 20, 24), 1426, 0.0), LAMP_OFF: lambda: S((60, 40, 40), 1427, 0.0),
        LAMP_RED: lambda: S((255, 60, 50), 1428, 0.0), LAMP_GREEN: lambda: S((70, 230, 90), 1429, 0.0),
        "pi_rr_mirror": _mirror, SIGN_TEX: _wash_sign,
        "pi_rr_vacant": _square(lambda: _indicator("VACANT", (30, 140, 60)), 256),
        "pi_rr_occupied": _square(lambda: _indicator("OCCUPIED", (196, 30, 28)), 256),
        "pi_rr_isa": _isa, "pi_rr_tp_front": _tp_front, "pi_rr_tp_end": _tp_end,
        "pi_rr_urinal_screen": _urinal_screen, "pi_rr_screen_edge": lambda: S((30, 90, 180), 1430, 0.0),
        "pi_rr_dryer_front": _dryer_front, "pi_rr_nozzle": lambda: S((20, 20, 22), 1431, 0.0),
        "pi_rr_guard": _guard, "pi_rr_towel_front": _towel_front, "pi_rr_towel": _towel,
        "pi_rr_liner": lambda: S((26, 26, 28), 1432, 0.02), "pi_rr_bin_label": _square(_bin_label, 128), "pi_rr_crumple": _crumple,
        "pi_rr_koala": lambda: S((186, 190, 194), 1433, 0.02), "pi_rr_koala_dark": lambda: S((120, 124, 128), 1434, 0.02),
        "pi_rr_changing": _square(_changing, 256),
        "pi_rr_grate": _grate, "pi_rr_exhaust": _exhaust,
        "restroom_wall_tile": lambda: _wall_tile(False), "restroom_wall_tile_cap": lambda: _wall_tile(True),
        "restroom_floor_tile": _floor_tile,
    }


# ------------------------------------------------------------------------------------------------ the plain pieces
PIECES = {
    "restroom_mirror": {"display": "Restroom Mirror", "boxes": mirror_boxes()},
    "restroom_towel_dispenser": {"display": "Paper Towel Dispenser", "boxes": towel_boxes()},
    "restroom_stall_panel": {"display": "Toilet Partition Panel", "boxes": front_panel_boxes()},
    "restroom_urinal_screen": {"display": "Urinal Privacy Screen", "boxes": screen_boxes()},
    "restroom_changing_station": {"display": "Baby Changing Station", "boxes": changing_boxes()},
    "restroom_floor_drain": {"display": "Floor Drain", "boxes": floor_drain_boxes()},
    "restroom_exhaust_grille": {"display": "Restroom Exhaust Grille", "boxes": exhaust_boxes()},
    "sign_wash_hands": {"display": "Hand Washing Sign", "boxes": wash_sign_boxes()},
    # Variants: 0 plain, 1 wall brackets at x 0, 2 at x 16, 3 pilaster brackets at x 0, 4 at x 16.
    "restroom_stall_partition": {"display": "Toilet Partition", "variants": 5, "boxes": partition_boxes(),
                                 "vboxes": [[], partition_boxes("wall")[1:], mirror_x(partition_boxes("wall"))[1:],
                                            partition_boxes("pilaster")[1:], mirror_x(partition_boxes("pilaster"))[1:]]},
    # Variants: 0 plain, 1 its end cap at x 0, 2 its wall bracket at x 16.
    "restroom_stall_headrail": {"display": "Toilet Partition Headrail", "variants": 3, "boxes": headrail_boxes(),
                                "vboxes": [[], headrail_boxes("cap")[1:], headrail_boxes("wall")[1:]]},
}


# ------------------------------------------------------------------------------------------------ the working fixtures
# Blocks with states of their own (nuclear/RestroomBlocks.java), their models and blockstates written here.
FIXTURES = {"restroom_toilet": "Toilet", "restroom_urinal": "Urinal", "restroom_sink": "Restroom Sink",
            "restroom_hand_dryer": "Hand Dryer", "restroom_trash_can": "Trash Can"}
DOORS = {"restroom_stall_door": "Toilet Stall Door", "restroom_stall_door_accessible": "Accessible Toilet Stall Door"}
TILES = {"restroom_wall_tile": "Restroom Wall Tile", "restroom_wall_tile_cap": "Restroom Wall Tile (Top Course)"}
# the tile's back and edges: painted plaster, bare concrete, or the tower's carbon composite panel (item 22 room 12)
BACKS = (("painted", "jugcraft:block/painted_wall"), ("concrete", "minecraft:block/light_gray_concrete"),
         ("carbon", "jugcraft:block/carbon_composite_panel"))
PARTITION_ON_TOILET = 2      # restroom_stall_partition's variant standing along a toilet's side (its wall end at the back)


def toilet_loot(rid):
    """The toilet, and its partition if one was fitted."""
    return {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("restroom_toilet")}],
         "condition": {"type": "minecraft:survives_explosion"}},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("restroom_stall_partition")}],
         "condition": {"type": "minecraft:all_of", "terms": [
             {"type": "minecraft:match_block", "blocks": rid("restroom_toilet"), "state": {"partition": "true"}},
             {"type": "minecraft:survives_explosion"}]}}],
        "random_sequence": rid("blocks/restroom_toilet")}
CUBES = {"restroom_floor_tile": "Restroom Floor Tile"}
BLOCKS = list(FIXTURES) + list(DOORS) + list(TILES) + list(CUBES)
NAMES = {**FIXTURES, **DOORS, **TILES, **CUBES}
PAPER_MAX = 8                 # toilet paper rolls a dispenser holds (two jumbo rolls, four each)
TURN = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
DOOR_TURN = {"east": 0, "south": 90, "west": 180, "north": 270}     # a vanilla door's: its models are drawn facing east


def _y(y):
    return {"y": y} if y else {}


def write_assets(write, rid, assets, lang):
    import plant_interior as PI
    models = assets / "models" / "block"

    def mdl(name, boxes):
        write(models / f"{name}.json", PI.model(boxes, [], rid))

    def item(block, model):
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model}")}})

    for block, name in NAMES.items():
        lang[f"block.jugcraft.{block}"] = name
    lang["message.jugcraft.restroom.paper_empty"] = "The dispenser is empty: it is refilled a roll at a time"
    lang["message.jugcraft.restroom.occupied"] = "Occupied"
    lang["container.jugcraft.restroom_trash_can"] = "Trash Can (thrown away when you close it)"
    # The toilet: body by variant, water and valve by flushing, the rolls by the paper left.
    for v in (0, 1):
        acc = v == 1
        mdl(f"restroom_toilet_{v}", toilet_boxes(acc))
        for f in (False, True):
            tag = "_flush" if f else ""
            mdl(f"restroom_toilet_{v}_water{tag}", toilet_water(acc, f))
            mdl(f"restroom_toilet_{v}_valve{tag}", toilet_valve(acc, f))
    for k in (0, 1):
        mdl(f"restroom_toilet_roll_{k}", tp_roll(k))
    mdl("restroom_toilet_inventory", toilet_boxes() + toilet_water(False, False) + toilet_valve(False, False) + tp_roll(0) + tp_roll(1))
    parts = []
    for face, y in TURN:
        for v in (0, 1):
            parts.append({"when": {"facing": face, "variant": str(v)}, "apply": {"model": rid(f"block/restroom_toilet_{v}"), **_y(y)}})
            for f in ("false", "true"):
                tag = "_flush" if f == "true" else ""
                for p in ("water", "valve"):
                    parts.append({"when": {"facing": face, "variant": str(v), "flushing": f},
                                  "apply": {"model": rid(f"block/restroom_toilet_{v}_{p}{tag}"), **_y(y)}})
        for k in (0, 1):
            have = "|".join(str(n) for n in range(1 + 4 * k, PAPER_MAX + 1))
            parts.append({"when": {"facing": face, "paper": have}, "apply": {"model": rid(f"block/restroom_toilet_roll_{k}"), **_y(y)}})
        # The stall partition along its left side as you face it (two stalls side by side): the partition's wall-end
        # variant (its bracket on the back wall), turned a quarter from the toilet's back edge to its west edge.
        parts.append({"when": {"facing": face, "partition": "true"},
                      "apply": {"model": rid(f"block/restroom_stall_partition_{PARTITION_ON_TOILET}"), **_y((y + 270) % 360)}})
    write(assets / "blockstates" / "restroom_toilet.json", {"multipart": parts})
    item("restroom_toilet", "restroom_toilet_inventory")
    # The urinal.
    mdl("restroom_urinal", urinal_boxes())
    for f in (False, True):
        tag = "_flush" if f else ""
        mdl(f"restroom_urinal_valve{tag}", urinal_valve(f))
        mdl(f"restroom_urinal_water{tag}", urinal_water(f))
    mdl("restroom_urinal_inventory", urinal_boxes() + urinal_valve(False) + urinal_water(False))
    parts = []
    for face, y in TURN:
        parts.append({"when": {"facing": face}, "apply": {"model": rid("block/restroom_urinal"), **_y(y)}})
        for f in ("false", "true"):
            tag = "_flush" if f == "true" else ""
            for p in ("valve", "water"):
                parts.append({"when": {"facing": face, "flushing": f}, "apply": {"model": rid(f"block/restroom_urinal_{p}{tag}"), **_y(y)}})
    write(assets / "blockstates" / "restroom_urinal.json", {"multipart": parts})
    item("restroom_urinal", "restroom_urinal_inventory")
    # The sink and the hand dryer: running or not.
    for block, body, live in (("restroom_sink", sink_boxes, sink_live), ("restroom_hand_dryer", dryer_boxes, dryer_live)):
        for r in (False, True):
            mdl(f"{block}{'_running' if r else ''}", body() + live(r))
        write(assets / "blockstates" / f"{block}.json", {"variants": {
            f"facing={face},running={r}": {"model": rid(f"block/{block}{'_running' if r == 'true' else ''}"), **_y(y)}
            for face, y in TURN for r in ("false", "true")}})
        item(block, block)
    # The trash can, by how full it is.
    for lv in range(4):
        mdl(f"restroom_trash_can_{lv}", bin_boxes() + bin_fill(lv))
    write(assets / "blockstates" / "restroom_trash_can.json", {"variants": {
        f"facing={face},fill={lv}": {"model": rid(f"block/restroom_trash_can_{lv}"), **_y(y)} for face, y in TURN for lv in range(4)}})
    item("restroom_trash_can", "restroom_trash_can_0")
    # The stall doors: a vanilla door's states and latched; the pilasters stay put when the leaf swings.
    for door in DOORS:
        acc = door.endswith("accessible")
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for opened in (False, True):
                    for latched in ((False, True) if half == "lower" and not opened else (False,)):
                        name = f"{door}_{half}_{hinge}{'_open' if opened else ''}{'_latched' if latched else ''}"
                        mdl(name, door_boxes(half == "lower", hinge, opened, latched, acc))
        # Swinging (doors round: nuclear/PlantDoorBlock.java MOVING), only the pilasters: client/PlantDoorRenderer.java
        # draws the leaf turning (its part models: tools/plant_doors.py).
        for half in ("lower", "upper"):
            mdl(f"{door}_{half}_moving", _pilasters(half == "lower"))
        variants = {}
        for facing, y in DOOR_TURN.items():
            for half in ("lower", "upper"):
                for hinge in ("left", "right"):
                    for opened in ("false", "true"):
                        for latched in ("false", "true"):
                            lat = latched == "true" and half == "lower" and opened == "false"
                            name = f"{door}_{half}_{hinge}{'_open' if opened == 'true' else ''}{'_latched' if lat else ''}"
                            for moving in ("false", "true"):
                                key = f"facing={facing},half={half},hinge={hinge},latched={latched},moving={moving},open={opened}"
                                shown = name if moving == "false" else f"{door}_{half}_moving"
                                variants[key] = {"model": rid(f"block/{shown}"), **_y(y)}
        write(assets / "blockstates" / f"{door}.json", {"variants": variants})
        whole = door_boxes(True, "left", False, False, acc) + [b[:1] + (b[1] + 16,) + b[2:4] + (b[4] + 16,) + b[5:]
                                                                for b in door_boxes(False, "left", False, False, acc)]
        mdl(f"{door}_inventory", whole)
        item(door, f"{door}_inventory")
    # The wall tile: tiled on its facing side, painted (or concrete) on the others.
    for block in TILES:
        for back, tex in BACKS:
            faces = {f: {"texture": "#side"} for f in ("east", "west", "up", "down", "south")}
            faces["north"] = {"texture": "#tile", "cullface": "north"}
            for f in ("east", "west", "up", "down", "south"):
                faces[f]["cullface"] = f
                if back == "carbon":
                    faces[f]["uv"] = [0, 0, 8, 8]          # the panel's texture spans two blocks: one block's quarter
            write(models / f"{block}_{back}.json", {"parent": "minecraft:block/block", "textures": {
                "tile": rid(f"block/{block}"), "side": tex, "particle": rid(f"block/{block}")},
                "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]})
        write(assets / "blockstates" / f"{block}.json", {"variants": {
            f"back={back},facing={face}": {"model": rid(f"block/{block}_{back}"), **_y(y)}
            for face, y in TURN for back, _ in BACKS}})
        item(block, f"{block}_painted")
    for block in CUBES:
        write(models / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{block}")}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        item(block, block)


CRAFTING = {
    "restroom_toilet": (["Q  ", "QBQ", "QQQ"], {"Q": "minecraft:quartz_block", "B": "minecraft:water_bucket"}, 1),
    "restroom_urinal": (["QIQ", "Q Q", " Q "], {"Q": "minecraft:quartz_block", "I": "minecraft:iron_ingot"}, 1),
    "restroom_sink": (["I  ", "QCQ", " Q "], {"I": "minecraft:iron_ingot", "Q": "minecraft:quartz_slab", "C": "minecraft:cauldron"}, 1),
    "restroom_hand_dryer": (["PPP", "PRP", "P P"], {"P": "#c:plates/iron", "R": "minecraft:redstone"}, 1),
    "restroom_trash_can": (["P P", "P P", "PPP"], {"P": "#c:plates/iron"}, 1),
    "restroom_stall_door": (["PP", "PP", "PP"], {"P": "#c:plates/steel"}, 3),
    "restroom_stall_door_accessible": (["PP", "PB", "PP"], {"P": "#c:plates/steel", "B": "minecraft:blue_dye"}, 3),
    "restroom_wall_tile": (["QQ", "QQ"], {"Q": "minecraft:quartz"}, 4),
    "restroom_wall_tile_cap": (["QQ", "QD"], {"Q": "minecraft:quartz", "D": "minecraft:light_blue_dye"}, 4),
    "restroom_floor_tile": (["SS", "SS"], {"S": "minecraft:polished_andesite"}, 4),
    "restroom_mirror": (["IGI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glass_pane"}, 1),
    "restroom_towel_dispenser": (["PPP", "PRP"], {"P": "#c:plates/iron", "R": "minecraft:paper"}, 1),
    # (item 22: the partition and the front panel each a recipe of their own, not corrugated iron's or the door's)
    "restroom_stall_partition": (["PD", "DP"], {"P": "#c:plates/steel", "D": "minecraft:white_dye"}, 4),
    "restroom_stall_panel": (["PP", "PP", "NN"], {"P": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 2),
    "restroom_stall_headrail": (["NNN"], {"N": "minecraft:iron_nugget"}, 6),
    "restroom_urinal_screen": (["P", "P"], {"P": "#c:plates/steel"}, 2),
    "restroom_changing_station": (["PPP", "PWP"], {"P": "#c:plates/iron", "W": "minecraft:white_wool"}, 1),
    "restroom_floor_drain": (["SNS"], {"S": "minecraft:polished_andesite", "N": "minecraft:iron_bars"}, 1),
    "restroom_exhaust_grille": (["NNN", "NWN", "NNN"], {"N": "minecraft:iron_nugget", "W": "minecraft:white_wool"}, 1),
    "sign_wash_hands": (["PD"], {"P": "minecraft:paper", "D": "minecraft:blue_dye"}, 2),
}


def register():
    PP.RISERS.update(PIECES)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP
    PP.SEE_THROUGH |= SEE_THROUGH
    PP.CRAFTING.update(CRAFTING)


register()
