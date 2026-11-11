"""The control building's switchgear room brought up to the detail standard (older rooms round 1; Justin, 15 October
2026: "Real 480 V switchgear", "Overhead power tray", fittings "One-line and signs", "Breaker handling gear", "Lights,
detectors, cooling", "Log desk").

The room (medium plant: tools/medium_plant.py switchgear_room) holds the two Class 1E 480 V load centres face to face
(tools/load_centre.py: LC 1A, Train A, along the north wall; LC 1B, Train B, across the aisle), their feeders' cables
leaving by an overhead power tray over each lineup:

- the trays (the cable spreading room's power tray, in the train's colour) under the ceiling over each lineup's back,
  a drop over every feeder section's gland plate (the cables leaving through the rungs, tied, down into the gland),
  and at the run's end the cables turning up through a fire-stopped sleeve in the ceiling into the spreading room
  over it: LC 1B's into a riser up to its row's Train B power tray, LC 1A's into a one-hour fire-wrapped raceway that
  rises, runs west along the north wall under the slab out of the Train B area and drops into Train A's power tray
  (IEEE 384: a train's cables never run bare through the other train's area);
- the room's cooling: a fan coil unit hung under the ceiling over the rear aisle (its chilled water and condensate
  into the east wall), its supply duct along the aisle with two registers, its return grille underneath;
- the breaker handling gear at the aisle's east end: a spare breaker on its lift truck, the remote racking device on
  its cart, a breaker test set;
- the log desk (a standing desk three wide: the phone, the rounds logbook, the laptop that reads the trip units) with
  the framed one-line diagram over it; the lineups' signs at the aisle's west end, the DANGER sign outside the door.

Every piece is one block (the lift truck two), its parts real boxes (16 px a block). Models in model pixels for the
piece facing north: x across, y up, z from its back (north, z 0) to its front (south, +z)."""
import math

from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP
import spreading_room as SR

STEEL, STEEL_DARK, CHROME = "pi_steel", "pi_steel_dark", "pi_chrome"
BLACK = "pi_pnl_black"
GALV, GALV_DARK = "pi_hv_galv", "pi_hv_galv_dark"


def clip_y(boxes, y0, y1, dy=0.0):
    """The boxes' parts between y0 and y1, moved by dy (a two-block piece cut into its blocks)."""
    out = []
    for b in boxes:
        a, c = max(b[1], y0), min(b[4], y1)
        if c - a > 1e-6:
            out.append((b[0], a + dy, b[2], b[3], c + dy, b[5]) + tuple(b[6:]))
    return out


# ------------------------------------------------------------------------------------------------ the trays

def tray_down(t):
    """The power tray with a drop: the feeder's cables leaving through the rungs at the block's middle (the middle rung
    left out there), tied to the next rung, down out of the block's bottom (into the drop under it)."""
    out = SR.tray_rails() + SR.tray_rungs() + SR.fill("power", t) + SR.stripe(t)
    out += SR.bundle_y(t, -0.0, SR.TRAY_Y[0] + 0.6, ties=(SR.TRAY_Y[0] - 2.2,))
    out += [(6.6, SR.TRAY_Y[0] - 0.2, 6.2, 9.4, SR.TRAY_Y[0], 9.8, "pi_cable_tie")]
    return out


def hung_down(t):
    """The drop on a trapeze hanger (its rods up to the ceiling only: a single tier), tied down, its tag and band."""
    return tray_down(t) + SR.hanger(True) + SR.tie("power") + SR.tag("power", t) + SR.bands(t)


def lc_drop(t):
    """The drop's cables down from the tray to the load centre's gland plate (its top 0.55 of a block under this
    block): the bundle, its ties, the cable grip where it hangs from the tray, the gland's locknut."""
    out = SR.bundle_y(t, -8.7, 16.0, ties=(2.0, 9.0))
    out += PP.ring_y(8, 8, 1.36, 1.7, 13.2, 15.6, "pi_steel_dark")
    out += PP.ring_y(8, 8, 1.36, 1.62, -8.7, -7.9, "pi_steel_dark")
    return out


def tray_ceiling(t):
    """The run's end: the tray in from the east, its four power cables turning up off it and up through the ceiling's
    fire-stopped sleeve (the steel collar, the red fire stop round the cables, the escutcheon on the ceiling)."""
    y0, y1 = SR.TRAY_Y
    out = SR.tray_rails(4.0, 16.0) + [r for r in SR.tray_rungs() if r[0] > 4.0] + SR.stripe(t)
    out = [b for b in out if b[0] >= 4.0 or b[3] > 4.0]
    out = [(max(b[0], 4.0),) + b[1:] for b in out]
    # The end plate across the rails' end.
    out += [(3.6, y0, 3.0, 4.0, y1, 13.0, "pi_steel")]
    texes = (SR.jkt(t), "pi_csr_jkt_black")
    r = SR.POWER_R
    for k, z in enumerate(SR.POWER_Z):
        tex = texes[k % 2]
        out += SR.oct_x(z, SR.POWER_Y, 6.6, 16.0, r, tex) + SR.oct_y(5.9, z, SR.POWER_Y - r, 16.0, r, tex)
    # The sleeve in the ceiling round them: collar, fire stop, escutcheon.
    out += [(4.2, 14.6, 3.6, 7.6, 15.8, 12.6, "pi_firestop"), (3.6, 14.2, 3.0, 8.2, 14.6, 13.2, GALV),
            (2.8, 15.8, 2.2, 9.0, 16.0, 14.0, GALV_DARK)]
    return out


# ------------------------------------------------------------------------------------------------ the fire-wrapped raceway

W_Z0, W_Y0, W_Z1, W_Y1 = SR.WRAP
RA, RB = SR.RISE


def _bands_rise(ys):
    out = []
    for y in ys:
        out += [(RA - 0.12, y, RA - 0.12, RB + 0.12, y + 0.8, RA, GALV), (RA - 0.12, y, RB, RB + 0.12, y + 0.8, RB + 0.12, GALV),
                (RA - 0.12, y, RA, RA, y + 0.8, RB, GALV), (RB, y, RA, RB + 0.12, y + 0.8, RB, GALV)]
    return out


def wrap_floor():
    """The wrapped raceway up out of the floor: its sleeve's fire stop and escutcheon on the floor, the wrap up."""
    out = [(RA, 0.6, RA, RB, 16.0, RB, {"*": "pi_fire_wrap", "up": "pi_fire_wrap_end", "down": "pi_fire_wrap_end"})]
    out += _bands_rise((4.0, 12.0))
    out += [(RA - 0.6, 0.2, RA - 0.6, RB + 0.6, 0.6, RB + 0.6, "pi_firestop"), (RA - 1.4, 0.0, RA - 1.4, RB + 1.4, 0.2, RB + 1.4, GALV_DARK)]
    return out


def wrap_elbow():
    """The wrapped rise turning west under the slab, into the run along the wall."""
    out = [(RA, 0.0, RA, RB, W_Y0, RB, {"*": "pi_fire_wrap", "down": "pi_fire_wrap_end", "up": None})]
    out += [(0.0, W_Y0, W_Z0, RB, W_Y1, W_Z1, {"*": SR.WRAP_TEX["*"], "north": SR.WRAP_TEX["north"], "south": SR.WRAP_TEX["south"],
                                               "up": "pi_fire_wrap", "down": "pi_fire_wrap", "west": None})]
    out += _bands_rise((4.0,))
    return out


def wrap_hanger():
    """The run on a trapeze: its strut under it, two rods up to the slab."""
    out = SR.wrap()
    x = 8.0
    out += [(x - 0.8, W_Y0 - 0.6, W_Z0 - 1.0, x + 0.8, W_Y0, W_Z1 + 1.0, "pi_steel_dark")]
    for z in (W_Z0 - 0.6, W_Z1 + 0.6):
        out += [(x - 0.2, W_Y0 - 0.9, z - 0.2, x + 0.2, 16.0, z + 0.2, "pi_steel_dark"), (x - 0.4, W_Y0 - 0.9, z - 0.4, x + 0.4, W_Y0 - 0.6, z + 0.4, "pi_steel_dark")]
    return out


def wrap_drop(t="a"):
    """The run's west end: the wrap from the east ends in its flap; the cables come out of it, turn south over the next
    block's tray and down onto its cables (the tray one block south and one down: its cables' top 2.7 under this
    block)."""
    out = SR.wrap(8.0, 16.0)
    out += [(7.6, W_Y0 - 0.1, W_Z0 - 0.1, 8.0, W_Y1 + 0.1, W_Z1 + 0.1, "pi_fire_wrap_end")]
    cy = (W_Y0 + W_Y1) / 2
    out += SR.bundle_z(t, 4.0, 24.4, cy, cx=6.2) + SR.bundle_y(t, -2.4, cy + 0.9, cx=6.2, cz=24.4, ties=(4.0,))
    return out


# ------------------------------------------------------------------------------------------------ the cooling

D0, D1 = 1.5, 14.5
F0, F1 = 0.9, 15.1


def _duct_flange(t0, t1):
    a0, a1, b0, b1 = D0, D1, F0, F1
    return [(b0, a1, t0, b1, b1, t1, {"*": GALV_DARK, "down": None}), (b0, b0, t0, b1, a0, t1, {"*": GALV_DARK, "up": None}),
            (b0, a0, t0, a0, a1, t1, {"*": GALV_DARK, "east": None}), (a1, a0, t0, b1, a1, t1, {"*": GALV_DARK, "west": None})]


def duct(hanger=False):
    out = [(D0, D0, 0, D1, D1, 16, {"*": GALV, "north": None, "south": None})] + _duct_flange(0, 0.25) + _duct_flange(15.75, 16)
    if hanger:
        y0 = F0 - 0.9
        out += [(-0.6, y0, 7.2, 16.6, y0 + 0.5, 8.8, STEEL_DARK)]
        for x in (-0.3, 15.7):
            out += [(x, y0 + 0.5, 7.75, x + 0.6, 15.6, 8.35, STEEL), (x - 0.5, 15.6, 7.3, x + 1.1, 16.0, 8.8, STEEL_DARK)]
    return out


def duct_register():
    """The duct with a supply register in its underside: its frame, the louvred blades (double deflection)."""
    out = duct()
    out += [(3.2, D0 - 0.5, 3.2, 12.8, D0, 12.8, {"*": "pi_hv_galv_dark", "down": "pi_sgr_register!", "up": None})]
    return out


def duct_end():
    """The run's end: the duct closed by its end cap (the cap's flange and stiffener)."""
    out = [(D0, D0, 0.6, D1, D1, 16, {"*": GALV, "south": None, "north": GALV_DARK})] + _duct_flange(15.75, 16)
    out += [(F0, F0, 0.2, F1, F1, 0.6, GALV_DARK), (D0 + 1.0, 7.6, 0.0, D1 - 1.0, 8.4, 0.2, GALV_DARK)]
    return out


FCU_Y = (2.2, 15.0)             # the fan coil unit's casing (hung under the ceiling)
FCU_Z = (1.0, 15.2)


def _fcu_casing(x0, x1, ends):
    y0, y1 = FCU_Y
    z0, z1 = FCU_Z
    faces = {"*": "pi_sgr_fcu", "up": "pi_sgr_fcu"}
    for e in ends:
        faces[e] = None
    return [(x0, y0, z0, x1, y1, z1, faces)]


def fan_coil():
    """The fan coil unit's west half (facing north: its discharge north): the casing, the return grille and filter rack
    underneath, the filter access door on its front, the nameplate, the discharge collar north into the duct, the
    hanger rods at its corners."""
    y0, y1 = FCU_Y
    z0, z1 = FCU_Z
    out = _fcu_casing(0.6, 16.0, ("east",))
    out += [(1.4, y0 - 0.4, z0 + 1.0, 15.4, y0, z1 - 1.0, {"*": "pi_steel_dark", "down": "pi_sgr_return!", "up": None})]
    out += [(2.0, y0 + 1.2, z1, 14.2, y1 - 1.2, z1 + 0.2, {"*": "pi_sgr_fcu", "south": "pi_sgr_fcu_door!"})]
    for y in (y0 + 3.0, y1 - 3.0):
        out += [(13.0, y - 0.5, z1 + 0.2, 13.6, y + 0.5, z1 + 0.8, CHROME)]
    out += [(D0, D0, 0.0, D1, D1, z0, {"*": GALV, "north": None, "south": None})] + _duct_flange(0, 0.25)
    for x, z in ((1.6, z0 + 1.2), (1.6, z1 - 1.2)):
        out += [(x - 0.2, y1, z - 0.2, x + 0.2, 16.0, z + 0.2, STEEL_DARK), (x - 0.5, y1, z - 0.5, x + 0.5, y1 + 0.3, z + 0.5, STEEL_DARK)]
    return out


def fan_coil_end():
    """The east half: the casing over the coil, the coil's access panel and the plate with the unit's data, the
    chilled water supply and return (insulated, labelled) and the condensate drain out of its east end into the
    wall (their escutcheons on it), the drain pan's lip, the hanger rods."""
    y0, y1 = FCU_Y
    z0, z1 = FCU_Z
    out = _fcu_casing(0.0, 15.4, ("west",))
    out += [(1.0, y0 + 1.2, z1, 13.6, y1 - 1.2, z1 + 0.2, {"*": "pi_sgr_fcu", "south": "pi_sgr_fcu_plate!"})]
    out += [(0.0, y0 - 0.6, z0 + 0.4, 15.4, y0, z1 - 0.4, STEEL_DARK)]
    for k, (z, tex) in enumerate(((5.0, "pi_sgr_chws"), (10.0, "pi_sgr_chwr"))):
        out += [(15.4, 8.8 - 1.6, z - 1.6, 16.0, 8.8 + 1.6, z + 1.6, {"*": "pi_hv_jacket", "east": "pi_hv_jacket", "west": "pi_hv_jacket",
                                                                       "north": (tex, [0, 0, 16, 16]), "south": (tex, [0, 0, 16, 16])}),
                (15.6, 8.8 - 2.3, z - 2.3, 16.0, 8.8 + 2.3, z + 2.3, {"*": CHROME, "west": CHROME})]
    out += [(14.0, y0 - 1.6, 12.4, 15.0, y0 - 0.6, 13.4, "pi_hv_jacket"), (15.0, y0 - 1.6, 12.4, 16.0, y0 - 0.6, 13.4, "pi_hv_jacket"),
            (15.6, y0 - 2.0, 12.0, 16.0, y0 - 0.2, 13.8, CHROME)]
    for x, z in ((14.4, z0 + 1.2), (14.4, z1 - 1.2)):
        out += [(x - 0.2, y1, z - 0.2, x + 0.2, 16.0, z + 0.2, STEEL_DARK), (x - 0.5, y1, z - 0.5, x + 0.5, y1 + 0.3, z + 0.5, STEEL_DARK)]
    return out


# ------------------------------------------------------------------------------------------------ the handling gear

CHARCOAL, GREY, YELLOW = "pi_sgr_charcoal", "pi_sgr_grey", "pi_sgr_yellow"


def _breaker(x0, y0, z0, x1, y1, z1):
    """A draw-out breaker on its own: its side sheets, its front (the drawing), the primary disconnect fingers at its back,
    its wheels, the lifting eyes on top."""
    out = [(x0, y0, z0, x1, y1, z1, {"*": GREY, "south": "pi_sgr_breaker_front!", "up": GREY})]
    for k in range(3):
        x = x0 + (x1 - x0) * (0.25 + 0.25 * k)
        for y in (y0 + 2.6, y1 - 3.2):
            out += [(x - 0.6, y - 0.9, z0 - 1.2, x + 0.6, y + 0.9, z0, "pi_sgr_copper")]
    for x in (x0 + 0.6, x1 - 1.6):
        for z in (z0 + 0.8, z1 - 1.8):
            out += [(x, y0 - 0.7, z, x + 1.0, y0, z + 1.0, BLACK)]
    for x in (x0 + 1.5, x1 - 2.5):
        out += [(x, y1, z0 + 4.0, x + 1.0, y1 + 1.2, z0 + 4.4, STEEL), (x, y1 + 0.8, z0 + 3.2, x + 1.0, y1 + 1.2, z0 + 5.2, STEEL)]
    return out


def lifter_boxes():
    """The breaker lift truck with the spare breaker on its forks (two blocks tall, facing north: its forks toward its
    user, south): the base frame on its casters and outriggers, the mast up the back, the hand winch, the carriage
    and forks, the chain over the pulley, the push handle, the breaker, its tag."""
    out = []
    # The base: side rails, the cross member at the back, the outriggers forward, the casters.
    for x0 in (1.0, 13.6):
        out += [(x0, 1.2, 1.0, x0 + 1.4, 2.6, 15.6, YELLOW)]
        out += [(x0 + 0.2, 0.0, 13.8, x0 + 1.2, 1.2, 15.0, BLACK), (x0 + 0.2, 0.0, 1.4, x0 + 1.2, 1.2, 2.6, BLACK)]
    out += [(2.4, 1.2, 1.0, 13.6, 2.6, 3.0, YELLOW)]
    # The mast (two channels), its top and the pulley.
    for x0 in (3.0, 11.6):
        out += [(x0, 2.6, 1.4, x0 + 1.4, 30.6, 3.0, YELLOW)]
    out += [(3.0, 30.6, 1.2, 13.0, 31.6, 3.2, YELLOW), (7.2, 28.6, 2.0, 8.8, 30.6, 3.6, STEEL_DARK)]
    # The winch on the right channel: its drum, the crank.
    out += [(13.0, 13.0, 1.6, 15.2, 15.4, 3.0, "pi_steel_dark"), (15.2, 13.9, 2.0, 15.6, 14.5, 2.6, STEEL),
            (15.6, 13.9, 2.0, 16.0, 17.6, 2.6, STEEL), (15.4, 17.2, 2.0, 16.4, 17.8, 3.6, BLACK)]
    # The carriage on the mast, the forks forward under the breaker.
    out += [(2.6, 3.2, 3.0, 13.4, 5.0, 3.8, YELLOW)]
    for x0 in (3.8, 10.8):
        out += [(x0, 3.2, 3.8, x0 + 1.4, 3.8, 15.4, STEEL_DARK)]
    # The chain from the pulley down to the carriage.
    out += [(7.8, 5.0, 2.6, 8.2, 28.6, 3.0, "pi_steel")]
    # The push handle across the back.
    for x in (3.4, 12.6):
        out += [(x - 0.3, 18.0, 0.4, x + 0.3, 18.6, 1.4, CHROME)]
    out += [(3.1, 18.0, 0.2, 12.9, 18.6, 0.8, CHROME)]
    # The breaker on the forks (all in the lower block: its front is one drawing).
    out += _breaker(3.0, 4.5, 5.0, 13.0, 14.2, 14.0)
    # Its tag hung on its lifting eye.
    out += [(9.4, 11.2, 14.0, 11.8, 13.6, 14.15, {"*": "pi_pnl_white", "south": "pi_sgr_spare_tag!"})]
    return out


def lifter():
    return clip_y(lifter_boxes(), 0.0, 16.0)


def lifter_top():
    return clip_y(lifter_boxes(), 16.0, 32.0, -16.0)


def racking_device():
    """The remote racking device on its cart (facing north: its drive toward its user, south): the hand truck (its
    frame, axle and wheels, the toe plate), the motor and gearbox on their height column, the drive shaft and the
    racking adapter, the control pendant on its hook with its coiled cable, the cable reel, the plate."""
    out = []
    for x0 in (2.0, 12.6):
        out += [(x0, 1.6, 2.0, x0 + 1.4, 20.0, 3.4, "pi_sgr_rrd_frame")]
        out += [(x0 - 0.8 if x0 < 8 else x0 + 1.4, 0.0, 1.2, x0 if x0 < 8 else x0 + 2.2, 3.2, 4.4, BLACK)]
    out += [(2.0, 0.6, 3.4, 14.0, 1.2, 10.0, "pi_sgr_rrd_frame"), (1.2, 1.4, 2.2, 14.8, 2.2, 3.2, "pi_steel_dark")]
    out += [(2.0, 19.4, 2.0, 14.0, 20.4, 3.4, "pi_sgr_rrd_frame")]
    # The height column and the motor/gearbox unit on it.
    out += [(7.0, 1.2, 4.0, 9.0, 12.0, 6.0, STEEL)]
    out += [(4.4, 8.0, 5.0, 11.6, 13.6, 11.0, {"*": "pi_sgr_rrd", "south": "pi_sgr_rrd_front!", "up": "pi_sgr_rrd"}),
            (5.0, 9.0, 1.4, 11.0, 12.8, 5.0, "pi_steel_dark")]
    # The drive shaft forward and the hex adapter.
    out += [(7.4, 10.4, 11.0, 8.6, 11.6, 14.8, CHROME), (7.0, 10.0, 14.8, 9.0, 12.0, 15.8, "pi_steel_dark")]
    # The pendant on its hook, its cable coiled on the reel.
    out += [(13.0, 14.0, 3.4, 13.4, 16.0, 3.8, STEEL), (12.8, 9.6, 3.6, 14.8, 14.0, 4.6, {"*": YELLOW, "south": "pi_sgr_pendant!"})]
    out += [(3.6, 13.4, 3.4, 6.4, 18.0, 4.2, "pi_steel_dark"), (3.2, 14.0, 4.2, 6.8, 17.4, 5.6, "pi_sgr_cable")]
    out += [(4.4, 19.0, 3.4, 11.6, 20.2, 3.55, {"*": "pi_pnl_white", "south": "pi_sgr_rrd_plate!"})]
    return out


def test_set():
    """A breaker test set on its cart (facing north: its panel toward its user): the cart's shelves and wheels, the test
    set's case with its lid open (the panel: the display dark, switched off; the current dial, the selector, the
    output terminals), the leads coiled on the lower shelf, the trip unit tester beside it."""
    out = []
    for x in (1.2, 14.0):
        for z in (2.0, 13.4):
            out += [(x, 0.0, z, x + 0.8, 1.0, z + 0.8, BLACK), (x + 0.2, 1.0, z + 0.2, x + 0.6, 10.6, z + 0.6, STEEL_DARK)]
    out += [(1.0, 3.4, 1.8, 15.0, 4.0, 14.4, STEEL), (1.0, 10.6, 1.8, 15.0, 11.2, 14.4, STEEL)]
    out += [(0.6, 10.6, 1.8, 1.0, 13.0, 14.4, STEEL_DARK)]
    # The test set's case and its open lid.
    out += [(2.2, 11.2, 5.0, 11.8, 14.8, 13.6, {"*": "pi_sgr_case", "up": "pi_sgr_testset!"}),
            (2.2, 14.8, 3.6, 11.8, 21.4, 5.0, {"*": "pi_sgr_case", "south": "pi_sgr_lid!"})]
    # The trip unit tester beside it.
    out += [(12.2, 11.2, 7.0, 14.8, 12.6, 12.4, {"*": "pi_sgr_case", "up": "pi_sgr_tut!"})]
    # The leads coiled on the lower shelf.
    for k, tex in enumerate(("pi_sgr_lead_red", "pi_sgr_lead_black")):
        x0 = 3.0 + k * 5.4
        out += [(x0, 4.0, 5.0, x0 + 4.4, 5.2, 11.0, tex)]
    return out


# ------------------------------------------------------------------------------------------------ the log desk

DESK_Y = 16.6                   # a standing desk's worktop (about 42 in)
DESK_T = 0.8


def desk(v):
    """The standing log desk, three blocks wide (v 0 its left end with the phone, 1 the middle with the logbook, 2 the
    right end with the laptop: its own piece, sgr_desk_laptop, the screen live): the worktop, the end panels at its
    ends, the modesty panel, the foot rail, the shelf under it with the room's procedures, and on it what this block
    has."""
    y0 = DESK_Y - DESK_T
    out = [(0.0, y0, 1.0, 16.0, DESK_Y, 14.6, {"*": "pi_tsc_edge", "up": "pi_tsc_top"})]
    out += [(0.0, 3.0, 1.2, 16.0, y0, 2.0, "pi_tsc_body")]
    out += [(0.0, 5.0, 2.0, 16.0, 5.6, 12.6, "pi_tsc_body")]
    out += [(0.0, 1.8, 12.4, 16.0, 2.6, 13.2, CHROME)]
    if v == 0:
        out += [(0.0, 0.0, 1.0, 1.2, y0, 14.6, "pi_tsc_body")]
    if v == 2:
        out += [(14.8, 0.0, 1.0, 16.0, y0, 14.6, "pi_tsc_body")]
    if v == 1:
        out += [(7.4, 0.0, 1.2, 8.6, y0, 2.6, "pi_tsc_body")]
    # The shelf's binders (the room's procedures).
    for k in range(5 if v != 1 else 6):
        x0 = 1.6 + k * 2.3
        h = 6.0 + (k % 3) * 0.6
        out += [(x0, 5.6, 3.0, x0 + 2.0, 5.6 + h, 10.0, {"*": "pi_tsc_binder_side", "south": "pi_sgr_binder_spine!"})]
    y = DESK_Y
    if v == 2:
        return out + laptop()
    if v == 0:
        # The phone (as the TSC's) and the PAGE handset box on the wall side.
        out += [(3.0, y, 3.0, 6.8, y + 0.55, 6.0, {"*": "pi_tsc_phone_side", "up": "pi_tsc_phone!"}),
                (3.0, y + 0.55, 3.0, 6.8, y + 0.95, 4.2, {"*": "pi_tsc_phone_side", "up": "pi_tsc_phone_side"}),
                (3.15, y + 0.95, 3.1, 4.1, y + 1.5, 5.9, "pi_cap_black"), (3.05, y + 0.55, 5.3, 4.2, y + 1.1, 5.95, "pi_cap_black")]
        out += [(9.0, y, 3.0, 14.6, y + 0.7, 8.0, {"*": "pi_tsc_binder_side", "up": "pi_sgr_sop!", "west": "pi_tsc_binder_spine!"})]
    else:
        # The open rounds logbook, its pen.
        out += [(2.4, y, 3.4, 13.6, y + 0.35, 11.4, {"*": "pi_sgr_book_edge", "up": "pi_sgr_logbook!"}),
                (9.0, y + 0.35, 9.6, 12.6, y + 0.55, 10.0, "pi_pen_blue")]
    return out


# The laptop (sgr_desk_laptop, a live panel: client/EdgTelemetry.java sgr writes its screen from the nearest load
# centre's trip units, nuclear/SwitchgearRoom.java): its base with the keyboard, the screen standing up from its hinge
# (upright, so the game's lines lie on it), the mouse.
LT_KEY = "sgr_desk_laptop"
LT_SCREEN = (2.4, DESK_Y + 0.5, 12.6, DESK_Y + 7.2)     # the lid's front: x0, y0, x1, y1
LT_Z = 6.6                                              # its face
LT_BEZEL = 0.35
LT_SIZE = 0.37
LT_TITLE = 23.14                                        # the title bar's line (the bar 22.83 to the bezel)
LT_ROW0, LT_STEP, LT_ROWS = 22.53, 0.47, 9               # the breakers' lines: the first's middle, the step down
LT_FOOT = 18.175                                        # the transformer's line (its band 17.9 to 18.45)
LT_X = (2.95, 7.0, 12.05)                               # the columns: name, status (left), amps (right)
LT_NAME, LT_GREEN, LT_AMBER, LT_WHITE = (206, 214, 228), (120, 230, 150), (255, 190, 60), (226, 228, 232)


def laptop():
    y = DESK_Y
    x0, y0, x1, y1 = LT_SCREEN
    return [(2.4, y, 6.4, 12.6, y + 0.5, 13.0, {"*": "pi_cap_black", "up": "pi_sgr_laptop_keys!"}),
            # The hinge's barrel along the lid's foot.
            (3.0, y + 0.5, 6.15, 12.0, y + 0.95, 6.55, "pi_cap_black"),
            (x0, y0 + 0.45, LT_Z - 0.6, x1, y1, LT_Z, {"*": "pi_cap_black", "south": "pi_sgr_laptop_screen!"}),
            (13.4, y, 9.4, 14.6, y + 0.45, 11.2, "pi_cap_black")]


def laptop_front(label=None):
    """The screen's live lines (the screen itself the lid's texture, laptop_screen_tex): the title bar's lineup and
    volts, a line a breaker (its name, its state in its colour, its amps), the transformer's line at the foot."""
    f = PP.Front(1, 711)
    z = LT_Z + 0.02
    f.text(LT_X[0], LT_TITLE, z, 0.40, LT_WHITE, "sgr_t", "left")
    f.text(LT_X[2], LT_TITLE, z, 0.40, LT_WHITE, "sgr_v", "right")
    for k in range(LT_ROWS):
        ry = LT_ROW0 - k * LT_STEP
        f.text(LT_X[0], ry, z, LT_SIZE, LT_NAME, f"sgr_n{k}", "left")
        for ch, rgb in (("g", LT_GREEN), ("y", LT_AMBER), ("w", LT_WHITE)):
            f.text(LT_X[1], ry, z, LT_SIZE, rgb, f"sgr_{ch}{k}", "left")
        f.text(LT_X[2], ry, z, LT_SIZE, LT_WHITE, f"sgr_a{k}", "right")
    f.text(LT_X[0], LT_FOOT, z, LT_SIZE, LT_NAME, "sgr_f", "left")
    f.text(LT_X[2], LT_FOOT, z, LT_SIZE, LT_AMBER, "sgr_x", "right")
    return f


# ------------------------------------------------------------------------------------------------ the textures

def _img(w, h, rgb):
    return Image.new("RGBA", (w, h), tuple(rgb) + (255,))


def _f(size, bold=True):
    return PP.rfont(size, bold)


def _text(d, xy, s, size, fill, anchor="mm", bold=True, width=None):
    while width and size > 6 and d.textlength(s, font=_f(size, bold)) > width:
        size -= 1
    d.text(xy, s, font=_f(size, bold), fill=fill, anchor=anchor)


def register_tex():
    img = _img(256, 256, (178, 182, 184))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(120, 122, 124), width=10)
    for y in range(26, 236, 14):
        d.rectangle([22, y, 233, y + 7], fill=(60, 62, 66))
    for x in range(40, 230, 28):
        d.line([(x, 22), (x, 234)], fill=(150, 152, 154), width=3)
    return img


def return_tex():
    img = _img(256, 256, (170, 174, 176))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(110, 112, 114), width=8)
    for x in range(18, 240, 10):
        for y in range(18, 240, 10):
            d.rectangle([x, y, x + 6, y + 6], fill=(40, 42, 46))
    return img


def fcu_door_tex():
    img = M.to_img(M.base((128, 142, 156), 256))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 253, 253], outline=(120, 122, 120), width=3)
    d.rectangle([60, 18, 196, 54], fill=(238, 238, 232), outline=(40, 40, 40))
    _text(d, (128, 30), "FILTER ACCESS", 15, (20, 20, 22))
    _text(d, (128, 46), "MERV 13  24 x 24 x 4", 11, (20, 20, 22), bold=False)
    return img


def fcu_plate_tex():
    img = M.to_img(M.base((128, 142, 156), 256))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 253, 253], outline=(120, 122, 120), width=3)
    d.rectangle([20, 14, 236, 132], fill=(214, 216, 212), outline=(90, 90, 90), width=2)
    lines = [("FCU-SG-1", 22, True), ("SWITCHGEAR ROOM", 13, True), ("FAN COIL UNIT", 13, True), ("3000 CFM  2 HP  480 V", 11, False),
             ("CHW 45 °F IN  57 °F OUT", 11, False), ("FED FROM MCC 1A3", 11, False)]
    for k, (s, size, bold) in enumerate(lines):
        _text(d, (128, 30 + k * 19), s, size, (20, 20, 22), bold=bold, width=200)
    return img


def pipe_label_tex(word):
    img = _img(128, 128, (60, 130, 70))
    d = ImageDraw.Draw(img)
    _text(d, (64, 64), word, 30, (250, 250, 246))
    return img


def breaker_front_tex():
    """The spare breaker's front (as the load centre's breakers' fronts, standing: open, discharged)."""
    img = _img(256, 256, (34, 35, 38))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 251, 251], outline=(70, 70, 74), width=4)
    for (cx, cy), c, word in (((34, 46), (190, 36, 30), "CLOSE"), ((34, 96), (40, 140, 64), "OPEN")):
        d.ellipse([cx - 14, cy - 14, cx + 14, cy + 14], fill=c)
        _text(d, (cx + 40, cy), word, 12, (236, 236, 230))
    for (cy, bg, ink, word) in ((40, (40, 140, 64), (250, 250, 246), "OPEN"), (90, (236, 236, 230), (20, 20, 22), "DISCHARGED")):
        d.rectangle([102, cy - 10, 162, cy + 10], fill=bg)
        _text(d, (132, cy), word, 10, ink, width=56)
    d.rectangle([172, 20, 238, 130], fill=(92, 96, 100))
    d.rectangle([180, 30, 230, 52], fill=(70, 80, 60))
    _text(d, (205, 80), "TRIP UNIT", 10, (236, 236, 230))
    d.rectangle([20, 190, 90, 214], fill=(16, 16, 18))
    _text(d, (55, 180), "CHARGE", 10, (236, 236, 230))
    d.rectangle([110, 160, 170, 180], fill=(200, 36, 30))
    _text(d, (140, 170), "DISCONNECT", 9, (250, 250, 246), width=56)
    d.ellipse([124, 192, 156, 224], fill=(16, 16, 18), outline=(110, 110, 110), width=3)
    d.rectangle([176, 196, 240, 236], fill=(186, 188, 186))
    _text(d, (208, 208), "800 AF", 10, (20, 20, 22))
    _text(d, (208, 224), "225 AT", 10, (20, 20, 22))
    return img


def spare_tag_tex():
    img = _img(128, 128, (238, 238, 232))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 22], fill=(40, 120, 64))
    _text(d, (64, 11), "SPARE", 16, (250, 250, 246))
    for k, s in enumerate(("LV BREAKER", "800 AF 225 AT", "TESTED 08/26", "READY FOR USE")):
        _text(d, (64, 40 + k * 22), s, 13, (20, 20, 22), width=120)
    return img


def rrd_front_tex():
    """The gearbox's face: its name band over the drive's boss, the warning under it."""
    img = _img(256, 256, (226, 186, 36))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(150, 120, 20), width=6)
    d.rectangle([18, 16, 238, 60], fill=(30, 30, 32))
    _text(d, (128, 38), "REMOTE RACKING  RRD-480", 18, (240, 240, 232), width=210)
    d.ellipse([92, 96, 164, 168], fill=(60, 60, 64), outline=(30, 30, 32), width=4)
    d.rectangle([18, 196, 238, 240], fill=(250, 250, 246))
    _text(d, (128, 210), "OPERATE FROM OUTSIDE THE", 13, (20, 20, 22), width=210)
    _text(d, (128, 228), "ARC FLASH BOUNDARY", 13, (20, 20, 22), width=210)
    return img


def pendant_tex():
    img = _img(128, 128, (226, 186, 36))
    d = ImageDraw.Draw(img)
    for k, (c, word) in enumerate((((40, 140, 64), "IN"), ((190, 36, 30), "OUT"), ((30, 30, 32), "STOP"))):
        cy = 26 + k * 38
        d.ellipse([20, cy - 13, 46, cy + 13], fill=c)
        _text(d, (84, cy), word, 18, (20, 20, 22))
    return img


def rrd_plate_tex():
    img = _img(256, 64, (238, 238, 232))
    d = ImageDraw.Draw(img)
    _text(d, (128, 20), "USE FOR ALL RACKING", 18, (20, 20, 22))
    _text(d, (128, 44), "SOP-E-480-07", 16, (20, 20, 22), bold=False)
    return img


def testset_tex():
    img = _img(256, 256, (54, 58, 62))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 18, 140, 70], fill=(14, 16, 18))
    _text(d, (80, 86), "OUTPUT AMPS", 12, (230, 230, 224))
    d.ellipse([170, 20, 236, 86], fill=(232, 232, 226))
    d.ellipse([196, 46, 210, 60], fill=(20, 20, 22))
    _text(d, (203, 100), "CURRENT", 12, (230, 230, 224))
    for k, (c, s) in enumerate((((190, 36, 30), "A"), ((30, 30, 32), "COM"), ((190, 36, 30), "B"))):
        x = 48 + k * 64
        d.ellipse([x - 16, 150, x + 16, 182], fill=c, outline=(200, 200, 200), width=3)
        _text(d, (x, 196), s, 12, (230, 230, 224))
    _text(d, (128, 236), "PRIMARY INJECTION TEST SET", 13, (230, 230, 224), width=230)
    return img


def lid_tex():
    img = _img(256, 128, (40, 42, 46))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 20, 232, 108], fill=(238, 238, 232))
    _text(d, (128, 40), "BREAKER TEST SET", 18, (20, 20, 22))
    _text(d, (128, 66), "QUALIFIED PERSONNEL ONLY", 13, (20, 20, 22), bold=False)
    _text(d, (128, 90), "CAL DUE 03/27", 13, (20, 20, 22), bold=False)
    return img


def tut_tex():
    img = _img(128, 128, (226, 186, 36))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 14, 114, 52], fill=(16, 18, 20))
    _text(d, (64, 72), "TRIP UNIT", 14, (20, 20, 22))
    _text(d, (64, 92), "TESTER", 14, (20, 20, 22))
    return img


def lead_tex(rgb):
    img = _img(64, 64, rgb)
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.line([(0, y), (63, y + 4)], fill=tuple(max(0, c - 40) for c in rgb), width=2)
    return img


def binder_spine_tex(k=0):
    img = _img(64, 256, (32, 70, 140))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 30, 56, 150], fill=(238, 238, 232))
    for j, s in enumerate(("SOP", "E-480", "LC", "OPS")):
        _text(d, (32, 50 + j * 26), s, 14, (20, 20, 22))
    return img


def sop_tex():
    img = _img(256, 256, (32, 70, 140))
    d = ImageDraw.Draw(img)
    d.rectangle([30, 40, 226, 150], fill=(238, 238, 232))
    _text(d, (128, 66), "SOP-E-480-07", 20, (20, 20, 22))
    _text(d, (128, 96), "480 V LOAD CENTER", 16, (20, 20, 22))
    _text(d, (128, 122), "OPERATION", 16, (20, 20, 22))
    return img


def logbook_tex():
    """The open logbook: the left page's header and filled-in rows, the right page's headed rows half filled in."""
    img = _img(512, 360, (246, 244, 236))
    d = ImageDraw.Draw(img)
    d.line([(256, 0), (256, 359)], fill=(180, 176, 166), width=4)
    head = ("TIME", "LC 1A AMPS", "LC 1B AMPS", "XFMR °C", "INIT")
    for page, x0 in enumerate((14, 270)):
        _text(d, (x0 + 114, 18), "SWITCHGEAR ROOM ROUNDS" if page == 0 else "OCT 15 2026  SHIFT B", 15, (20, 20, 22))
        for k, h in enumerate(head):
            _text(d, (x0 + 8 + k * 47, 44), h, 8, (60, 60, 64), "lm", width=46)
        for r in range(12):
            y = 62 + r * 24
            d.line([(x0, y + 12), (x0 + 228, y + 12)], fill=(170, 190, 210), width=1)
            if page == 0 or r < 5:
                vals = (f"{(r * 2 + 6) % 24:02d}00", str(600 + (r * 37) % 90), str(420 + (r * 23) % 70), str(62 + r % 5), "JD" if r % 2 else "MK")
                for k, v in enumerate(vals):
                    d.text((x0 + 8 + k * 47, y), v, font=_f(12, False), fill=(30, 40, 120), anchor="lm")
    return img


def laptop_keys_tex():
    img = _img(256, 160, (30, 30, 32))
    d = ImageDraw.Draw(img)
    for r in range(5):
        for c in range(14):
            x, y = 12 + c * 17, 10 + r * 17
            d.rectangle([x, y, x + 13, y + 13], fill=(54, 54, 58))
    d.rectangle([90, 104, 166, 150], fill=(46, 46, 50))
    return img


def laptop_screen_tex():
    """The laptop lid's face: its black bezel, the screen (the viewer's window: its title bar, a faint band every other
    breaker's line, the status bar at its foot); the lines are the game's (laptop_front, client/EdgTelemetry.java sgr)."""
    K = 40
    x0, y1 = LT_SCREEN[0], LT_SCREEN[3]
    w, h = (LT_SCREEN[2] - x0), (y1 - LT_SCREEN[1] - 0.45)
    img = _img(int(w * K), int(h * K), (16, 16, 18))
    d = ImageDraw.Draw(img)

    def r(ax, ay, bx, by, fill):
        d.rectangle([(ax - x0) * K, (y1 - by) * K, (bx - x0) * K - 1, (y1 - ay) * K - 1], fill=fill)
    b = LT_BEZEL
    r(x0 + b, LT_SCREEN[1] + 0.45 + b, LT_SCREEN[2] - b, y1 - b, (14, 22, 34))
    r(x0 + b, LT_TITLE - 0.31, LT_SCREEN[2] - b, y1 - b, (30, 70, 140))
    for k in range(1, LT_ROWS, 2):
        cy = LT_ROW0 - k * LT_STEP
        r(x0 + b, cy - LT_STEP / 2, LT_SCREEN[2] - b, cy + LT_STEP / 2, (20, 30, 44))
    r(x0 + b, LT_SCREEN[1] + 0.45 + b, LT_SCREEN[2] - b, LT_FOOT + 0.275, (26, 38, 58))
    # The camera's eye in the bezel over the screen.
    cx = (x0 + LT_SCREEN[2]) / 2
    d.ellipse([(cx - x0) * K - 3, b / 2 * K - 3, (cx - x0) * K + 3, b / 2 * K + 3], fill=(40, 40, 46))
    return img


def sign_tex(kind):
    """The lineups' signs and the DANGER sign by the door (433 by 512 in a 512 square, as the plant's signs)."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    if kind.startswith("sign_lc_"):
        t = kind[-1]
        band = {"a": (226, 116, 30), "b": (40, 92, 196), "c": (150, 152, 156), "d": (150, 152, 156)}[t]
        d.rectangle([0, 0, W - 1, H - 1], fill=(24, 24, 26))
        d.rectangle([14, 14, W - 15, H - 15], outline=(236, 236, 230), width=6)
        _text(d, (W / 2, 120), f"LC 1{t.upper()}", 128, (250, 250, 246))
        _text(d, (W / 2, 230), "480 V LOAD CENTER", 40, (236, 236, 230), width=W - 60)
        d.rectangle([30, 290, W - 31, 380], fill=band)
        _text(d, (W / 2, 335), f"CLASS 1E  TRAIN {t.upper()}" if t in "ab" else "NON-CLASS 1E", 40,
              (250, 250, 246) if t == "b" else (20, 20, 22), width=W - 80)
        _text(d, (W / 2, 430), f"FED FROM 4160 V BUS 1{t.upper()}", 30, (236, 236, 230), width=W - 60)
    else:
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246), outline=(20, 20, 22), width=8)
        d.rectangle([8, 8, W - 9, 150], fill=(20, 20, 22))
        d.rounded_rectangle([60, 30, W - 61, 128], radius=48, fill=(206, 30, 32))
        _text(d, (W / 2, 80), "DANGER", 68, (250, 250, 246))
        for k, s in enumerate(("480 VOLTS", "ARC FLASH HAZARD", "AUTHORIZED", "PERSONNEL ONLY")):
            _text(d, (W / 2, 200 + k * 76), s, 50 if k == 0 else 40, (20, 20, 22), width=W - 50)
    return img


SIGNS = {"sign_lc_a": "LC 1A Sign", "sign_lc_b": "LC 1B Sign", "sign_switchgear_danger": "Switchgear Room Danger Sign",
         # The turbine building's switchgear room (turbine hall part 3): its load centres LC 1C and 1D.
         "sign_lc_c": "LC 1C Sign", "sign_lc_d": "LC 1D Sign"}


def _square(make):
    """The game's block textures are square, a power of two on a side: a drawing made to its face's shape is stretched
    so (its face maps the whole of it, so it lies on the face as drawn)."""
    def made():
        img = make()
        side = 1 << (max(img.size) - 1).bit_length()      # (and a power of two, as the game's atlas wants)
        return img if img.size == (side, side) else img.resize((side, side), Image.LANCZOS)
    return made


def makers():
    return {k: _square(v) for k, v in _makers().items()}


def _makers():
    return {
        "pi_sgr_register": register_tex, "pi_sgr_return": return_tex, "pi_sgr_fcu": lambda: PP._solid((128, 142, 156), 701, 0.04),
        "pi_sgr_fcu_door": fcu_door_tex, "pi_sgr_fcu_plate": fcu_plate_tex, "pi_sgr_chws": lambda: pipe_label_tex("CHWS"),
        "pi_sgr_chwr": lambda: pipe_label_tex("CHWR"), "pi_sgr_charcoal": lambda: PP._solid((34, 35, 38), 702, 0.03),
        "pi_sgr_grey": lambda: PP._solid((176, 180, 178), 703, 0.03), "pi_sgr_yellow": lambda: PP._solid((228, 186, 34), 704, 0.06),
        "pi_sgr_copper": lambda: PP._solid((190, 112, 60), 705, 0.04), "pi_sgr_breaker_front": breaker_front_tex,
        "pi_sgr_spare_tag": spare_tag_tex, "pi_sgr_rrd_frame": lambda: PP._solid((50, 54, 60), 706, 0.04),
        "pi_sgr_rrd": lambda: PP._solid((226, 186, 36), 707, 0.04), "pi_sgr_rrd_front": rrd_front_tex, "pi_sgr_pendant": pendant_tex,
        "pi_sgr_cable": lambda: lead_tex((30, 30, 32)), "pi_sgr_rrd_plate": rrd_plate_tex, "pi_sgr_case": lambda: PP._solid((40, 42, 46), 708, 0.03),
        "pi_sgr_testset": testset_tex, "pi_sgr_lid": lid_tex, "pi_sgr_tut": tut_tex, "pi_sgr_lead_red": lambda: lead_tex((180, 30, 26)),
        "pi_sgr_lead_black": lambda: lead_tex((30, 30, 32)), "pi_sgr_binder_spine": binder_spine_tex, "pi_sgr_sop": sop_tex,
        "pi_sgr_book_edge": lambda: PP._solid((236, 232, 220), 709, 0.02), "pi_sgr_logbook": logbook_tex,
        "pi_sgr_laptop_keys": laptop_keys_tex, "pi_sgr_laptop_screen": laptop_screen_tex,
        **{f"pi_{k}": (lambda k=k: sign_tex(k)) for k in SIGNS},
    }


SHARP = {"pi_sgr_register", "pi_sgr_return", "pi_sgr_fcu_door", "pi_sgr_fcu_plate", "pi_sgr_chws", "pi_sgr_chwr", "pi_sgr_breaker_front",
         "pi_sgr_spare_tag", "pi_sgr_rrd_front", "pi_sgr_pendant", "pi_sgr_rrd_plate", "pi_sgr_testset", "pi_sgr_lid", "pi_sgr_tut",
         "pi_sgr_binder_spine", "pi_sgr_sop", "pi_sgr_logbook", "pi_sgr_laptop_keys", "pi_sgr_laptop_screen"} | {f"pi_{k}" for k in SIGNS}


# ------------------------------------------------------------------------------------------------ the pieces

def rod_run():
    """The trapeze's two rods through a block (no anchor plates: the slab is further up)."""
    x = SR.HANGER_X
    return [(x - 0.2, 0.0, z - 0.2, x + 0.2, 16.0, z + 0.2, GALV_DARK) for z in (2.0, 14.0)]


def _v(boxes_of, hangered):
    vs = [boxes_of(t) for t in SR.TRAINS]
    if hangered:
        vs += [hangered(t) for t in SR.TRAINS]
    return vs


PIECES = {
    "cable_tray_power_down": {"display": "Cable Tray (Power, Drop Down)", "boxes": [], "vboxes": _v(tray_down, hung_down)},
    "lc_cable_drop": {"display": "Switchgear Cable Drop", "boxes": [], "vboxes": _v(lc_drop, None)},
    "cable_tray_power_ceiling": {"display": "Cable Tray (Power, Up Through Ceiling)", "boxes": [], "vboxes": _v(tray_ceiling, None)},
}
for _k, _info in PIECES.items():
    _info["variants"] = len(_info["vboxes"])
PIECES.update({
    "fire_wrap_floor": {"display": "Fire-Wrapped Rise (Floor Sleeve)", "boxes": wrap_floor()},
    "fire_wrap_elbow": {"display": "Fire-Wrapped Raceway (Elbow)", "boxes": wrap_elbow()},
    "fire_wrap_hanger": {"display": "Fire-Wrapped Raceway (Hanger)", "boxes": wrap_hanger()},
    "fire_wrap_drop": {"display": "Fire-Wrapped Raceway (Into Tray)", "boxes": wrap_drop()},
    "sgr_duct": {"display": "Switchgear Room Supply Duct", "boxes": duct()},
    "sgr_duct_hanger": {"display": "Switchgear Room Supply Duct (Hanger)", "boxes": duct(True)},
    "sgr_duct_register": {"display": "Supply Duct Register", "boxes": duct_register()},
    "sgr_duct_end": {"display": "Supply Duct End Cap", "boxes": duct_end()},
    "sgr_fan_coil": {"display": "Switchgear Room Fan Coil Unit", "boxes": fan_coil()},
    "sgr_fan_coil_end": {"display": "Switchgear Room Fan Coil Unit (Coil End)", "boxes": fan_coil_end()},
    "breaker_lifter": {"display": "Breaker Lift Truck", "boxes": lifter()},
    "breaker_lifter_top": {"display": "Breaker Lift Truck (Mast)", "boxes": lifter_top()},
    "remote_racking_device": {"display": "Remote Racking Device", "boxes": racking_device()},
    "breaker_test_set": {"display": "Breaker Test Set", "boxes": test_set()},
    "sgr_log_desk": {"display": "Switchgear Log Desk", "boxes": [], "variants": 2, "vboxes": [desk(v) for v in range(2)]},
    # A tray's hanger rods on up through a tall room (turbine hall part 3: the turbine building's switchgear room, its
    # trays five blocks under the operating floor's slab): the bare rods, cable_tray_rods' anchor plates over them.
    "cable_tray_rod_run": {"display": "Cable Tray Hanger Rods (Run)", "boxes": rod_run()},
})


# ------------------------------------------------------------------------------------------------ the one-line diagram

OL_W, OL_H = 3, 2               # blocks: the framed drawing over the log desk
OL_PX = 512


def one_line_image(tb=False):
    """The 480 V Class 1E auxiliary power one-line diagram (drawn at 512 px a block, 3 x 2): the 4160 V buses 1A and
    1B, each feeding its load centre's transformer through its breaker, the main breakers, the 480 V buses, the ten
    feeders each with its breaker and load; the legend, the title block. tb: the turbine building's (turbine hall part
    3): the non-1E buses 1C and 1D and their load centres in the turbine building's switchgear room."""
    W, H = OL_W * OL_PX, OL_H * OL_PX
    img = Image.new("RGBA", (W, H), (250, 250, 246, 255))
    d = ImageDraw.Draw(img)
    ink, red, blue = (20, 20, 24), (180, 30, 28), (30, 60, 160)
    d.rectangle([14, 14, W - 15, H - 15], outline=ink, width=3)
    _text(d, (W / 2, 52), "UNIT 1   480 V TURBINE BUILDING AUXILIARY POWER SYSTEM   ONE LINE DIAGRAM" if tb else
          "UNIT 1   480 V CLASS 1E AUXILIARY POWER SYSTEM   ONE LINE DIAGRAM", 34, ink)

    def brk(x, y, tag, closed=True, s=18):
        d.rectangle([x - s, y - s, x + s, y + s], fill=(red if closed else (250, 250, 246)), outline=ink, width=3)
        _text(d, (x + s + 8, y), tag, 15, ink, "lm", bold=False)

    pair = (("C", (150, 152, 156)), ("D", (150, 152, 156))) if tb else (("A", (226, 116, 30)), ("B", (40, 92, 196)))
    for k, (t, colour) in enumerate(pair):
        x0 = 60 + k * 740
        cx = x0 + 330
        # The 4160 V bus and its feeder breaker, the transformer.
        d.line([(x0, 120), (x0 + 660, 120)], fill=ink, width=9)
        _text(d, (x0, 98), f"4160 V BUS 1{t}  (NON-CLASS 1E)" if tb else f"4160 V BUS 1{t}  (CLASS 1E  TRAIN {t})", 20, ink, "lm")
        d.rectangle([x0 + 560, 88, x0 + 660, 106], fill=colour)
        d.line([(cx, 120), (cx, 210)], fill=ink, width=4)
        brk(cx, 170, f"52-1{t}1-05")
        d.ellipse([cx - 34, 210, cx + 34, 278], outline=ink, width=4)
        d.ellipse([cx - 34, 252, cx + 34, 320], outline=ink, width=4)
        _text(d, (cx + 48, 246), f"XFMR LC 1{t}", 17, ink, "lm")
        _text(d, (cx + 48, 268), "1500/2000 kVA  AA/FA", 14, ink, "lm", bold=False)
        _text(d, (cx + 48, 288), "4160 - 480Y/277 V  Z 5.75 %", 14, ink, "lm", bold=False)
        d.line([(cx, 320), (cx, 440)], fill=ink, width=4)
        brk(cx, 380, f"52-1{t}-01  MAIN  3200 AF / 2000 AT")
        # The 480 V bus and its ten feeders.
        d.line([(x0, 440), (x0 + 660, 440)], fill=ink, width=9)
        _text(d, (x0, 418), f"480 V LOAD CENTER LC 1{t}", 20, ink, "lm")
        loads = feeders_of(f"1{t}")
        for i, load in enumerate(loads):
            fx = x0 + 33 + i * 66
            d.line([(fx, 440), (fx, 700)], fill=ink, width=3)
            closed = load != "SPARE"
            d.rectangle([fx - 13, 520 - 13, fx + 13, 520 + 13], fill=(red if closed else (250, 250, 246)), outline=ink, width=3)
            tag = f"-{i + 2:02d}"
            _text(d, (fx + 6, 552), tag, 13, ink, "lm", bold=False)
            d.polygon([(fx - 9, 700), (fx + 9, 700), (fx, 716)], fill=ink)
            words = load.split(" ")
            for j, wd in enumerate(words):
                _text(d, (fx, 736 + j * 18), wd, 13, ink, width=62)
        _text(d, (x0 + 330, 800), f"MCC 1{t}1 TO 1{t}4: TURBINE BUILDING GROUND FLOOR AND OPERATING FLOOR" if tb else
              f"DC MCC 1{t}: MCC {('1A','1B')[k]} IN DC EQUIPMENT ROOM 1{t}", 14, blue, bold=False)
    # The legend and the title block.
    lx, ly = 70, 860
    d.rectangle([lx - 10, ly - 20, lx + 520, ly + 120], outline=ink, width=2)
    _text(d, (lx, ly), "LEGEND", 16, ink, "lm")
    d.rectangle([lx, ly + 22, lx + 24, ly + 46], fill=red, outline=ink, width=2)
    _text(d, (lx + 36, ly + 34), "BREAKER, NORMALLY CLOSED", 14, ink, "lm", bold=False)
    d.rectangle([lx, ly + 58, lx + 24, ly + 82], fill=(250, 250, 246), outline=ink, width=2)
    _text(d, (lx + 36, ly + 70), "BREAKER, NORMALLY OPEN (SPARE)", 14, ink, "lm", bold=False)
    d.ellipse([lx + 300, ly + 16, lx + 330, ly + 46], outline=ink, width=3)
    d.ellipse([lx + 300, ly + 36, lx + 330, ly + 66], outline=ink, width=3)
    _text(d, (lx + 342, ly + 42), "TRANSFORMER", 14, ink, "lm", bold=False)
    tx0, ty0 = W - 640, H - 170
    d.rectangle([tx0, ty0, W - 30, H - 30], outline=ink, width=3)
    d.line([(tx0, ty0 + 50), (W - 30, ty0 + 50)], fill=ink, width=2)
    _text(d, (tx0 + 300, ty0 + 25), "480 V TURBINE BLDG AUX POWER  ONE LINE" if tb else "480 V CLASS 1E AUX POWER  ONE LINE", 20, ink)
    rows = [("DWG", "E-0012 SH 1" if tb else "E-0011 SH 3"), ("REV", "4" if tb else "7"), ("DATE", "2026-02-20" if tb else "2026-03-12"),
            ("STATUS", "CONTROLLED COPY")]
    for k, (a, b) in enumerate(rows):
        _text(d, (tx0 + 20 + (k % 2) * 300, ty0 + 76 + (k // 2) * 34), f"{a}  {b}", 16, ink, "lm")
    return img


def feeders_of(tag):
    return [f"MCC {tag}1", f"MCC {tag}2", f"MCC {tag}3", f"MCC {tag}4", f"INVERTER {tag}", f"DC MCC {tag}", f"CR HVAC {tag}",
            f"LIGHTING {tag}", "SPARE", "SPARE"]


def one_line_part(c, r, tb=False):
    """Block (c across from the left as you face it, r up) of the framed drawing (facing north: hung on the north
    wall, the drawing's face south): its share of the backing board, the drawing behind its acrylic, the frame."""
    p = r * OL_W + c
    z0, z1 = 0.2, 0.7
    out = [(0, 0, z0, 16, 16, z1, {"*": "pi_sgr_frame", "south": f"pi_sgr_oneline{'_tb' if tb else ''}_{p}!"})]
    f = 0.8
    edges = []
    if c == 0:
        edges.append((0, 0, z1, f, 16, z1 + 0.5))
    if c == OL_W - 1:
        edges.append((16 - f, 0, z1, 16, 16, z1 + 0.5))
    # The rails along the foot and head run between the stiles (no two of the frame's faces in one place).
    ax, bx = (f if c == 0 else 0), (16 - f if c == OL_W - 1 else 16)
    if r == 0:
        edges.append((ax, 0, z1, bx, f, z1 + 0.5))
    if r == OL_H - 1:
        edges.append((ax, 16 - f, z1, bx, 16, z1 + 0.5))
    out += [e + ("pi_sgr_frame",) for e in edges]
    return out


def one_line_textures():
    out = {}
    for tb, sfx in ((False, ""), (True, "_tb")):
        img = one_line_image(tb)
        for p in range(OL_W * OL_H):
            c, r = p % OL_W, p // OL_W
            x0, y0 = c * OL_PX, (OL_H - 1 - r) * OL_PX
            out[f"pi_sgr_oneline{sfx}_{p}"] = (lambda x0=x0, y0=y0, img=img: img.crop((x0, y0, x0 + OL_PX, y0 + OL_PX)))
    out["pi_sgr_frame"] = lambda: PP._solid((168, 172, 176), 710, 0.02)
    return out


# ------------------------------------------------------------------------------------------------ registering

# The one-line diagram, a framed drawing three blocks wide and two high (plant_office2.multi_parts "parts"), and the
# laptop end of the log desk, a live panel (plant_panels PANELS: its drawing the lines the game writes).
MULTI = {"sgr_one_line": {"display": "Switchgear One-Line Diagram", "multi": (OL_W, OL_H, "frame"), "parts": one_line_part, "item3d": True},
         # The turbine building's (turbine hall part 3: LC 1C and 1D).
         "sgr_one_line_tb": {"display": "Turbine Building One-Line Diagram", "multi": (OL_W, OL_H, "frame"),
                             "parts": lambda c, r: one_line_part(c, r, True), "item3d": True}}
PANELS = {LT_KEY: {"display": "Switchgear Log Desk (Laptop)", "width": 1, "rows": 1, "item3d": True}}
CRAFTING = {
    "cable_tray_power_down": (["I I", "IPI"], {"I": "minecraft:iron_bars", "P": "#c:plates/steel"}, 4),
    "lc_cable_drop": (["CCC", "SSS"], {"C": "minecraft:black_wool", "S": "minecraft:string"}, 4),
    "cable_tray_power_ceiling": (["IPI", "I I"], {"I": "minecraft:iron_bars", "P": "#c:plates/steel"}, 4),
    "fire_wrap_floor": (["WCW", "WCW"], {"W": "minecraft:white_wool", "C": "minecraft:black_wool"}, 2),
    "fire_wrap_elbow": (["WW ", "WCW", " WW"], {"W": "minecraft:white_wool", "C": "minecraft:black_wool"}, 2),
    "fire_wrap_hanger": (["IWI", "WCW"], {"I": "minecraft:iron_bars", "W": "minecraft:white_wool", "C": "minecraft:black_wool"}, 2),
    "fire_wrap_drop": (["WCW", " W "], {"W": "minecraft:white_wool", "C": "minecraft:black_wool"}, 2),
    "sgr_duct": (["PPP", "   ", "PPP"], {"P": "#c:plates/iron"}, 4),
    "sgr_duct_hanger": (["I I", "PPP", "PPP"], {"I": "minecraft:iron_bars", "P": "#c:plates/iron"}, 4),
    "sgr_duct_register": (["PBP"], {"P": "#c:plates/iron", "B": "minecraft:iron_bars"}, 2),
    "sgr_duct_end": (["PP", "PP"], {"P": "#c:plates/iron"}, 2),
    "sgr_fan_coil": (["PPP", "BCB", "PPP"], {"P": "#c:plates/steel", "B": "minecraft:iron_bars", "C": "minecraft:copper_ingot"}, 1),
    "sgr_fan_coil_end": (["PCP", "PPP"], {"P": "#c:plates/steel", "C": "minecraft:copper_ingot"}, 1),
    "breaker_lifter": (["PIP", "PIP", "W W"], {"P": "#c:plates/steel", "I": "minecraft:iron_ingot", "W": "minecraft:iron_nugget"}, 1),
    "breaker_lifter_top": (["I", "C", "I"], {"I": "minecraft:iron_ingot", "C": "minecraft:iron_chain"}, 1),
    "remote_racking_device": (["YRY", "PIP"], {"Y": "minecraft:yellow_dye", "R": "minecraft:redstone", "P": "#c:plates/steel",
                                                "I": "minecraft:iron_ingot"}, 1),
    "breaker_test_set": (["PGP", "RCR"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "R": "minecraft:redstone",
                                          "C": "jugcraft:basic_circuit"}, 1),
    "sgr_log_desk": (["SSS", "P P"], {"S": "minecraft:smooth_stone_slab", "P": "#c:plates/iron"}, 3),
    LT_KEY: (["GCG", "SSS", "P P"], {"G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "S": "minecraft:smooth_stone_slab",
                                     "P": "#c:plates/iron"}, 1),
    "sgr_one_line": (["FPF", "PIP", "FPF"], {"F": "minecraft:stick", "P": "minecraft:paper", "I": "minecraft:black_dye"}, 1),
    "cable_tray_rod_run": (["N", "N"], {"N": "minecraft:iron_nugget"}, 8),
    "sgr_one_line_tb": (["FPF", "PIP", "FPF"], {"F": "minecraft:stick", "P": "minecraft:paper", "I": "minecraft:gray_dye"}, 1),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": {"a": "minecraft:orange_dye", "b": "minecraft:blue_dye", "r": "minecraft:red_dye",
                                                          "c": "minecraft:light_gray_dye", "d": "minecraft:gray_dye"}[
        "r" if k == "sign_switchgear_danger" else k[-1]], "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info["rows"], "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS[LT_KEY] = laptop_front
    PP.CABINETS[LT_KEY] = lambda: desk(2)
    PP.RISERS.update(PIECES)
    PP.RISERS.update(MULTI)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.EXTRA_MAKERS.append(one_line_textures)
    PP.SHARP |= SHARP | {f"pi_sgr_oneline{sfx}_{p}" for sfx in ("", "_tb") for p in range(OL_W * OL_H)} | {f"pi_{LT_KEY}_f_{p}" for p in range(2)}
    # The lineups' signs and the DANGER sign: the plant's signs (plant_panels SIGNS), drawn here.
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
