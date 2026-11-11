"""The control building's cable spreading room rebuilt (punch list #15; Justin, 15 October 2026: "Full cable runs",
"Colour-coded, separated", "Working sprinklers").

The room under the control room (medium plant: tools/medium_plant.py spreading_room) is where every cable to the boards
over it is sorted out: ladder trays in tiers stacked by kind (instrument lowest, control over it, power on top, as IEEE
422 has them), each tray full of its own kind of cable, tied down, tagged with its number at its hangers; bundles
leaving the trays and going up through fire-stopped sleeves in the slab into the boards' bottoms; risers up out of the
floor from the switchgear and DC rooms below. The safety trains are kept apart as a real plant keeps them (IEEE 384):
Train A's trays and cables orange on the west side, Train B's blue on the east, non-safety grey between, a clear gap
between the areas with a fire-rated barrier board in it, and where a Train B raceway has to pass through the other
areas (to the safety systems board's Train B half) it runs wrapped in a one-hour fire barrier. The ceiling is the bare
slab. Fire protection: smoke and heat detectors on the slab and a pre-action sprinkler system: its valve station on
the riser by the door, its releasing panel beside it, a manual pull station at the door, the cross main and the branch
lines with their pendent heads under the slab (nuclear/SpreadingRoomFire.java works it).

Every piece is one block, its parts real boxes (16 px a block). Pieces facing north run along x (the trays across z
3..13, their rails 11.2..14 up the block, as tools/plant_office2.py's); the trays' pieces take the train as their
variant (0 Train A, 1 Train B, 2 non-safety; plus 3 with a trapeze hanger and the tray's tag)."""
import math

from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP

TRAINS = ("a", "b", "n")
TRAIN_NAMES = {"a": "1A", "b": "1B", "n": "NS"}
JACKET = {"a": (222, 112, 30), "b": (40, 92, 196), "n": (58, 60, 64)}     # the trains' cable jackets
BAND = {"a": (234, 120, 28), "b": (40, 96, 204), "n": (64, 66, 70)}       # the paint bands on their trays' rails
TIERS = ("power", "control", "instr")
TIER_LETTER = {"power": "P", "control": "C", "instr": "I"}
GALV, GALV_DARK = "pi_steel", "pi_steel_dark"


def jkt(t):
    return f"pi_csr_jkt_{t}"


# ------------------------------------------------------------------------------------------------ round runs, the tray
# (As tools/plant_office2.py's: that module imports this one's neighbours, so its helpers are repeated here.)

def oct_x(z, y, x0, x1, half, tex):
    """A round-ish run along x: a square and the same square turned 45 degrees (its ends a hair short)."""
    return [(x0, y - half, z - half, x1, y + half, z + half, tex),
            (x0 + 0.05, y - half, z - half, x1 - 0.05, y + half, z + half, tex, {"rotation": {"angle": 45, "axis": "x", "origin": [x0, y, z]}})]


def oct_y(x, z, y0, y1, half, tex):
    return [(x - half, y0, z - half, x + half, y1, z + half, tex),
            (x - half, y0 + 0.05, z - half, x + half, y1 - 0.05, z + half, tex, {"rotation": {"angle": 45, "axis": "y", "origin": [x, y0, z]}})]


def oct_z(x, y, z0, z1, half, tex):
    return [(x - half, y - half, z0, x + half, y + half, z1, tex),
            (x - half, y - half, z0 + 0.05, x + half, y + half, z1 - 0.05, tex, {"rotation": {"angle": 45, "axis": "z", "origin": [x, y, z0]}})]


TRAY_Y = (11.2, 14.0)                    # the rails' bottom and top
TRAY_RUNGS = (2.0, 6.0, 10.0, 14.0)
RUNG_TOP = 11.85


def tray_rails(x0=0.0, x1=16.0):
    y0, y1 = TRAY_Y
    out = []
    for w0, w1, f0, f1 in ((3.0, 3.4, 3.4, 4.1), (12.6, 13.0, 11.9, 12.6)):
        out += [(x0, y0, w0, x1, y1, w1, GALV), (x0, y0, f0, x1, y0 + 0.3, f1, GALV), (x0, y1 - 0.3, f0, x1, y1, f1, GALV)]
    return out


def tray_rungs(skip=()):
    return [(c - 0.45, TRAY_Y[0] + 0.05, 3.4, c + 0.45, RUNG_TOP, 12.6, GALV) for c in TRAY_RUNGS if c not in skip]


# ------------------------------------------------------------------------------------------------ the trays' cables

POWER_R = 0.72
POWER_Z = (4.9, 7.0, 9.1, 11.2)          # far enough apart (more than 2 x 0.72 x root 2) that no two octagons overlap
POWER_Y = RUNG_TOP + POWER_R
LAYER_Z = (3.45, 12.55)                  # a layer of cables from rail to rail (as the relay room's control tray)
CTRL_LAYER = (11.9, 12.75)
CTRL_OVER = (5.4, 7.6, 9.9, 11.1)
CTRL_OVER_R = 0.33
INSTR_LAYER = (11.9, 12.4)
INSTR_OVER = (6.0, 9.4)
INSTR_OVER_R = 0.28
TOP = {"power": POWER_Y + POWER_R, "control": CTRL_LAYER[1] + 2 * CTRL_OVER_R, "instr": INSTR_LAYER[1] + 2 * INSTR_OVER_R}


def _layer_tex(t, thin):
    k = "i" if thin else ""
    side = f"pi_csr_{k}layer_{t}"
    return {"*": f"pi_csr_{k}ends_{t}", "up": side, "down": side, "north": side, "south": side}


def fill(tier, t, x0=0.0, x1=16.0):
    """A tray's cables along x: power, four big three-conductor cables side by side (the train's colour and black);
    control, a layer of small cables from rail to rail with a few laid over it; instrument, a thinner layer of shielded
    pairs with two grey cables laid over it."""
    out = []
    if tier == "power":
        for k, z in enumerate(POWER_Z):
            out += oct_x(z, POWER_Y, x0, x1, POWER_R, jkt(t) if k % 2 == 0 else "pi_csr_jkt_black")
    elif tier == "control":
        out.append((x0, CTRL_LAYER[0], LAYER_Z[0], x1, CTRL_LAYER[1], LAYER_Z[1], _layer_tex(t, False)))
        r = CTRL_OVER_R
        for k, z in enumerate(CTRL_OVER):
            out.append((x0, CTRL_LAYER[1], z - r, x1, CTRL_LAYER[1] + 2 * r, z + r, jkt(t) if k % 2 else "pi_csr_jkt_black"))
    else:
        out.append((x0, INSTR_LAYER[0], LAYER_Z[0], x1, INSTR_LAYER[1], LAYER_Z[1], _layer_tex(t, True)))
        r = INSTR_OVER_R
        for z in INSTR_OVER:
            out.append((x0, INSTR_LAYER[1], z - r, x1, INSTR_LAYER[1] + 2 * r, z + r, "pi_csr_jkt_grey"))
    return out


def stripe(t):
    """The train's colour along the outside of both rails under their top flanges, the whole run."""
    tex = f"pi_csr_band_{t}"
    return [(0.0, 12.9, 2.88, 16.0, 13.5, 3.0, tex), (0.0, 12.9, 13.0, 16.0, 13.5, 13.12, tex)]


def bands(t, x0=7.0, x1=9.0):
    """The train's paint band round the outside of both rails (at the hangers)."""
    y0, y1 = TRAY_Y
    tex = f"pi_csr_band_{t}"
    return [(x0, y0 + 0.2, 2.82, x1, y1 - 0.2, 3.0, tex), (x0, y0 + 0.2, 13.0, x1, y1 - 0.2, 13.18, tex)]


def tie(tier, x=12.0):
    """A cable tie round the cables and a rung (the cables are tied down at every hanger)."""
    top = TOP[tier]
    return [(x - 0.3, top, 3.6, x + 0.3, top + 0.2, 12.4, "pi_cable_tie"), (x - 0.3, TRAY_Y[0] - 0.2, 3.6, x + 0.3, TRAY_Y[0], 12.4, "pi_cable_tie")]


TAG = (0.6, 11.7, 4.2, 13.5)             # the tray's tag on the south rail's outside (x0, y0, x1, y1)
TAG_UV = [0, 0, 16, round(16 * (TAG[3] - TAG[1]) / (TAG[2] - TAG[0]), 4)]
HANGER_X = 3.0                           # the trapezes stand off the block's middle, clear of the bundles leaving


def tag(tier, t):
    x0, y0, x1, y1 = TAG
    return [(x0, y0, 13.12, x1, y1, 13.3, {"*": "pi_pnl_yellow", "south": (f"pi_csr_tag_{t}{TIER_LETTER[tier].lower()}", TAG_UV)})]


def hanger(bottom, x=HANGER_X):
    """A trapeze under the tray: its strut, the two threaded rods outside the rails (on up through the block: the tiers
    over it hang from the same rods), the nuts under the strut."""
    out = [(x - 0.8, 10.2, 1.4, x + 0.8, TRAY_Y[0], 14.6, GALV)]
    for z in (2.0, 14.0):
        out += [(x - 0.2, 9.6 if bottom else 0.0, z - 0.2, x + 0.2, 16.0, z + 0.2, GALV_DARK),
                (x - 0.4, 9.75, z - 0.4, x + 0.4, 10.2, z + 0.4, GALV_DARK)]
    return out


def hanger_rods(x=HANGER_X):
    """The rods on up to the slab over the top tier, their anchor plates on its underside."""
    out = []
    for z in (2.0, 14.0):
        out += [(x - 0.2, 0.0, z - 0.2, x + 0.2, 15.6, z + 0.2, GALV_DARK), (x - 0.8, 15.6, z - 0.8, x + 0.8, 16.0, z + 0.8, GALV_DARK),
                (x - 0.45, 15.2, z - 0.45, x + 0.45, 15.6, z + 0.45, GALV_DARK)]
    return out


def tray(tier, t):
    return tray_rails() + tray_rungs() + fill(tier, t) + stripe(t)


def with_hanger(boxes, tier, t):
    return boxes + hanger(tier == "instr") + tie(tier) + tag(tier, t) + bands(t)


# ------------------------------------------------------------------------------------------------ bundles and sleeves

_HEX = 0.84
BUNDLE = [(0, 0, 0), (_HEX, 0, 1), (-_HEX, 0, 2), (_HEX / 2, _HEX * 0.866, 1), (-_HEX / 2, _HEX * 0.866, 0),
          (_HEX / 2, -_HEX * 0.866, 2), (-_HEX / 2, -_HEX * 0.866, 0)]
BUNDLE_R = 0.34
SIDE_Y = 15.3                            # a bundle leaving a tray over its rail crosses into the next block this high


def _btex(t):
    return (jkt(t), "pi_csr_jkt_black", "pi_csr_jkt_grey")


def bundle_y(t, y0, y1, cx=8.0, cz=8.0, ties=()):
    """The bundle up the block: seven cables, square in section (at this size no rounder to the eye), none touching."""
    out = []
    texes = _btex(t)
    for dx, dz, k in BUNDLE:
        out.append((cx + dx - BUNDLE_R, y0, cz + dz - BUNDLE_R, cx + dx + BUNDLE_R, y1, cz + dz + BUNDLE_R, texes[k]))
    for y in ties:
        out += PP.ring_y(cx, cz, 1.36, 1.52, y, y + 0.4, "pi_cable_tie")
    return out


def bundle_z(t, z0, z1, cy, cx=8.15):
    """The bundle running along z at height cy (a hair off the middle, so its cables' sides never share a plane with the
    same bundle's where it turns up)."""
    out = []
    texes = _btex(t)
    for dx, dy, k in BUNDLE:
        out.append((cx + dx - BUNDLE_R, cy + dy - BUNDLE_R, z0, cx + dx + BUNDLE_R, cy + dy + BUNDLE_R, z1, texes[k]))
    return out


def drop_side(tier, t):
    """A tray with a bundle leaving it over its south rail: up out of its cables, over the rail, out of the block's
    south face at SIDE_Y (into an elbow, which takes it up)."""
    lay = {"power": POWER_Y + POWER_R, "control": CTRL_LAYER[1], "instr": INSTR_LAYER[1]}[tier]
    return tray(tier, t) + bundle_y(t, lay - 0.2, SIDE_Y + 0.9, cz=10.6) + bundle_z(t, 10.6, 16.0, SIDE_Y)


def elbow_up(t):
    """A bundle in from the north (out of a tray over its rail) turning up."""
    return bundle_z(t, 0.0, 9.3, SIDE_Y) + bundle_y(t, SIDE_Y - 1.4, 16.0)


def drop_up(t):
    """A top tier's bundle leaving it straight up (out of the block's top, into the sleeve in the slab over it)."""
    return tray("power", t) + bundle_y(t, POWER_Y + POWER_R - 0.2, 16.0, ties=(14.6,))


def sleeve(t):
    """The bundle's last block under the slab: up into its sleeve (the block's top), the sleeve's steel collar, the red
    fire stop round the cables in it, its escutcheon plate on the slab."""
    return (bundle_y(t, 0.0, 16.0, ties=(4.0, 10.0)) + PP.ring_y(8, 8, 1.62, 2.15, 13.4, 15.8, GALV)
            + PP.round_y(8, 8, 1.58, 14.9, 15.8, "pi_firestop") + [(5.2, 15.8, 5.2, 10.8, 16.0, 10.8, GALV_DARK)])


# ------------------------------------------------------------------------------------------------ the risers

RISER_X = (4.6, 6.4, 8.2, 10.0, 11.8)
RISER_CABLE_R = 0.6                      # (1.8 apart: no two octagons overlap)


def _riser_ladder(y1):
    return ([(3.0, 0.0, 0.0, 3.4, y1, 3.4, GALV), (12.6, 0.0, 0.0, 13.0, y1, 3.4, GALV)]
            + [(3.4, y - 0.45, 0.2, 12.6, y + 0.45, 0.9, GALV) for y in (2.0, 6.0, 10.0, 14.0) if y + 0.45 < y1])


def riser(t, floor=False):
    """A riser up the north wall: a vertical ladder tray (its rails, its rungs against the wall), five power cables up
    it, the train's band; the lowest block with the fire-stopped sleeve the cables come up through out of the floor."""
    out = _riser_ladder(16.0)
    texes = (jkt(t), "pi_csr_jkt_black")
    for k, x in enumerate(RISER_X):
        out += oct_y(x, 1.8, 0.0, 16.0, RISER_CABLE_R, texes[k % 2])
    out += [(3.4, 7.0, 0.95, 12.6, 9.0, 1.05, f"pi_csr_band_{t}")]
    if floor:
        out += [(2.6, 0.3, 0.05, 13.4, 0.8, 3.8, "pi_firestop"), (2.2, 0.0, 0.1, 13.8, 0.3, 4.2, GALV_DARK)]
    return out


def riser_top(t):
    """The riser's top: its cables up the wall turn south over a short level ladder, over the next block's tray's rail
    and down onto its cables (in the next block south: the tray's)."""
    out = _riser_ladder(13.6)
    out += [(3.0, 13.6, 0.0, 3.4, 15.9, 16.0, GALV), (12.6, 13.6, 0.0, 13.0, 15.9, 16.0, GALV)]
    out += [(3.4, 13.6, z - 0.45, 12.6, 14.2, z + 0.45, GALV) for z in (5.0, 9.0, 13.0)]
    texes = (jkt(t), "pi_csr_jkt_black")
    for k, x in enumerate(RISER_X):
        tex = texes[k % 2]
        r = RISER_CABLE_R
        out += oct_y(x, 1.8, 0.0, 15.3, r, tex) + oct_z(x, 14.9, 1.8, 21.6, r, tex) + oct_y(x, 20.9, POWER_Y + POWER_R - 0.1, 14.6, r, tex)
    return out


# ------------------------------------------------------------------------------------------------ the gaps, the tee

def gap(bottom):
    """The clear gap between two trains' areas: a blind plate on each run's end, and the fire-rated barrier board
    standing in the gap (its tiers' boards make one wall; the lowest stops under the lowest tray)."""
    y0, y1 = TRAY_Y
    return [(0.0, y0, 3.0, 0.4, y1, 13.0, GALV), (15.6, y0, 3.0, 16.0, y1, 13.0, GALV),
            (7.3, 9.4 if bottom else 0.0, 1.0, 8.7, 16.0, 15.0, {"*": "pi_fire_board_edge", "east": "pi_fire_board", "west": "pi_fire_board"})]


CTRL_Z = (3.45, 12.55)
_LAYER_Z = {"*": "pi_ctrl_ends", "up": "pi_ctrl_layer_z", "down": "pi_ctrl_layer_z", "east": "pi_ctrl_layer", "west": "pi_ctrl_layer"}


def control_tee():
    """Train A's control tray with the relay room's header leaving it to the south (tools/plant_office2.py's tee, this
    tray's cables along it, the header's own cables laid over them into the branch)."""
    y0, y1 = TRAY_Y
    out = [(0, y0, 3.0, 16, y1, 3.4, GALV), (0, y0, 3.4, 16, y0 + 0.3, 4.1, GALV), (0, y1 - 0.3, 3.4, 16, y1, 4.1, GALV)]
    for a0, a1 in ((0.0, 3.4), (12.6, 16.0)):
        out += [(a0, y0, 12.6, a1, y1, 13.0, GALV), (a0, y0, 11.9, a1, y0 + 0.3, 12.6, GALV), (a0, y1 - 0.3, 11.9, a1, y1, 12.6, GALV)]
    for w0, w1, f0, f1 in ((3.0, 3.4, 3.4, 4.1), (12.6, 13.0, 11.9, 12.6)):
        out += [(w0, y0, 13.0, w1, y1, 16, GALV), (f0, y0, 13.0, f1, y0 + 0.3, 16, GALV), (f0, y1 - 0.3, 13.0, f1, y1, 16, GALV)]
    out += tray_rungs() + [(3.4, y0 + 0.15, 13.55, 12.6, RUNG_TOP, 14.45, GALV)]
    out += fill("control", "a") + [(CTRL_Z[0], CTRL_LAYER[0] + 0.15, 8.0, CTRL_Z[1], CTRL_LAYER[1] + 0.3, 16.0, _LAYER_Z)]
    return out


# ------------------------------------------------------------------------------------------------ the fire wrap

WRAP = (2.4, 10.5, 13.6, 14.8)            # round a tray: z0, y0, z1, y1
WRAP_SIDE_UV = [0, 0, 16, round(WRAP[3] - WRAP[1], 4)]
WRAP_TEX = {"*": "pi_fire_wrap_end", "north": ("pi_fire_wrap_side", WRAP_SIDE_UV), "south": ("pi_fire_wrap_side", WRAP_SIDE_UV),
            "up": "pi_fire_wrap", "down": "pi_fire_wrap"}
RISE = (5.4, 10.6)                        # a wrapped rise (x and z)


def wrap(x0=0.0, x1=16.0):
    """A Train B tray wrapped in a one-hour fire barrier blanket, its stainless bands (near the block's ends, clear of
    the rating printed along it)."""
    z0, y0, z1, y1 = WRAP
    out = [(x0, y0, z0, x1, y1, z1, WRAP_TEX)]
    for x in (1.0, 15.0):
        if x0 <= x - 0.4 and x + 0.4 <= x1:
            out += [(x - 0.4, y0 - 0.12, z0 - 0.12, x + 0.4, y0, z1 + 0.12, GALV), (x - 0.4, y1, z0 - 0.12, x + 0.4, y1 + 0.12, z1 + 0.12, GALV),
                    (x - 0.4, y0, z0 - 0.12, x + 0.4, y1, z0, GALV), (x - 0.4, y0, z1, x + 0.4, y1, z1 + 0.12, GALV)]
    return out


def wrap_start():
    """The wrapped raceway's east end: a Train B bundle in from the north (out of its tray over the rail) down into it,
    wrapped from where it leaves the tray."""
    return wrap(0.0, 14.0) + [(4.6, 12.6, 0.0, 11.4, 16.9, 8.6, {"*": "pi_fire_wrap", "north": "pi_fire_wrap_end"})]


def wrap_up(tee):
    """The wrapped raceway's west end (tee: a tee on the way) and a wrapped rise up out of it."""
    z0, y0, z1, y1 = WRAP
    a, b = RISE
    return (wrap(0.0, 16.0) if tee else wrap(2.4, 16.0)) + [(a, y1, a, b, 16.0, b, {"*": "pi_fire_wrap", "up": "pi_fire_wrap_end"})]


def wrap_rise(top=False):
    """The wrapped rise up to the slab (on top: its sleeve's fire stop round it, the escutcheon)."""
    a, b = RISE
    out = [(a, 0.0, a, b, 16.0, b, {"*": "pi_fire_wrap", "up": "pi_fire_wrap_end", "down": "pi_fire_wrap_end"})]
    for y in ((4.0,) if top else (3.0, 11.0)):
        out += [(a - 0.12, y, a - 0.12, b + 0.12, y + 0.8, a, GALV), (a - 0.12, y, b, b + 0.12, y + 0.8, b + 0.12, GALV),
                (a - 0.12, y, a, a, y + 0.8, b, GALV), (b, y, a, b + 0.12, y + 0.8, b, GALV)]
    if top:
        out += [(a - 0.8, 14.0, a - 0.8, b + 0.8, 15.8, b + 0.8, "pi_firestop"), (a - 1.4, 15.8, a - 1.4, b + 1.4, 16.0, b + 1.4, GALV_DARK)]
    return out


# ------------------------------------------------------------------------------------------------ the sprinklers

SPK_Y = 14.0                             # the branch lines' and the main's middle (under the slab)
BRANCH_R, MAIN_R, RISER_R = 0.5, 0.8, 1.0
RED = "pi_spk_red"
RISER_Z = 12.0                            # the riser stands 4 px off the south wall (as the valve station's pipe)


def _pipe_hanger_x(x):
    """A branch line's hanger: the rod to the slab, the band round the pipe."""
    y0, y1 = SPK_Y - BRANCH_R - 0.25, SPK_Y + BRANCH_R + 0.3
    return [(x - 0.2, y1, 7.8, x + 0.2, 15.7, 8.2, GALV_DARK), (x - 0.25, y0, 7.25, x + 0.25, y1, 7.4, GALV_DARK),
            (x - 0.25, y0, 8.6, x + 0.25, y1, 8.75, GALV_DARK), (x - 0.25, y0 - 0.15, 7.25, x + 0.25, y0, 8.75, GALV_DARK),
            (x - 0.7, 15.7, 7.3, x + 0.7, 16.0, 8.7, GALV_DARK)]


def head(x=8.0, z=8.0):
    """A pendent sprinkler head under the pipe: the drop nipple out of the tee, the brass frame, the red glass bulb in
    it, the deflector."""
    y = SPK_Y - BRANCH_R
    return (PP.round_y(x, z, 0.62, y - 0.9, y + 0.2, RED) + PP.round_y(x, z, 0.5, y - 1.6, y - 0.9, "pi_spk_brass")
            + [(x - 0.12, y - 3.0, z - 0.55, x + 0.12, y - 1.6, z - 0.31, "pi_spk_brass"), (x - 0.12, y - 3.0, z + 0.31, x + 0.12, y - 1.6, z + 0.55, "pi_spk_brass")]
            + oct_y(x, z, y - 2.85, y - 1.7, 0.2, "pi_spk_bulb") + oct_y(x, z, y - 3.25, y - 3.0, 0.8, "pi_spk_brass"))


def spk_pipe(with_head=False, end=False):
    x1 = 9.4 if end else 16.0
    out = oct_x(8.0, SPK_Y, 0.0, x1, BRANCH_R, RED)
    if end:
        out += oct_x(8.0, SPK_Y, x1, x1 + 0.6, BRANCH_R + 0.15, RED)
    if with_head or end:
        out += head() + _pipe_hanger_x(HANGER_X)
    return out


def spk_main(west=False, east=False, end=False):
    """The cross main along z (a piece facing north), its branch lines' tees (west, east); end: the main's north end
    (it comes from the south: the riser), capped."""
    z0 = 8.0 - MAIN_R - 0.6 if end else 0.0
    out = oct_z(8.0, SPK_Y, z0, 16.0, MAIN_R, RED)
    if end:
        out += oct_z(8.0, SPK_Y, z0 - 0.6, z0, MAIN_R + 0.15, RED)
    if west:
        out += oct_x(8.0, SPK_Y, 0.0, 8.0 - MAIN_R + 0.1, BRANCH_R, RED)
    if east:
        out += oct_x(8.0, SPK_Y, 8.0 + MAIN_R - 0.1, 16.0, BRANCH_R, RED)
    if west or east:
        out += PP.round_y(8.0, 8.0, MAIN_R + 0.25, SPK_Y - MAIN_R - 0.1, SPK_Y + MAIN_R + 0.1, RED)
    out += [(7.8, SPK_Y + MAIN_R + 0.3, 13.6, 8.2, 15.7, 14.0, GALV_DARK), (7.3, 15.7, 13.1, 8.7, 16.0, 14.5, GALV_DARK),
            (7.0, SPK_Y - MAIN_R - 0.4, 13.6, 9.0, SPK_Y + MAIN_R + 0.3, 14.0, GALV_DARK)]
    return out


def spk_riser(top=False):
    """The riser from the valve station up the south wall (its clamp to the wall); on top the bend into the main (north)
    with the tees to the branch line along the wall."""
    y1 = SPK_Y if top else 16.0
    out = oct_y(8.0, RISER_Z, 0.0, y1, RISER_R, RED)
    out += [(7.6, 6.0, RISER_Z + RISER_R, 8.4, 7.0, 16.0, GALV_DARK), (6.6, 5.6, 15.6, 9.4, 7.4, 16.0, GALV_DARK)]
    if top:
        out += PP.round_y(8.0, RISER_Z, RISER_R + 0.3, SPK_Y - 1.4, SPK_Y + 1.1, RED)
        out += oct_z(8.0, SPK_Y, 0.0, RISER_Z - RISER_R, MAIN_R, RED)
        out += oct_x(8.0, SPK_Y, 0.0, 8.0 - MAIN_R + 0.1, BRANCH_R, RED) + oct_x(8.0, SPK_Y, 8.0 + MAIN_R - 0.1, 16.0, BRANCH_R, RED)
        out += PP.round_y(8.0, 8.0, MAIN_R + 0.25, SPK_Y - MAIN_R - 0.1, SPK_Y + MAIN_R + 0.1, RED)
    return out


def heat_detector():
    """A heat detector on the slab (fixed temperature and rate of rise): its base, the white body, the black thermal
    element's fins under it, the LED."""
    out = [(5.6, 15.6, 5.6, 10.4, 16.0, 10.4, "pi_smoke_det")]
    out += PP.round_y(8, 8, 2.0, 14.4, 15.6, {"*": "pi_smoke_side", "up": "pi_smoke_det", "down": "pi_smoke_det"})
    out += [(7.6, 13.4, 6.4, 8.4, 14.4, 9.6, "pi_pnl_black"), (6.4, 13.5, 7.6, 7.6, 14.4, 8.4, "pi_pnl_black"),
            (8.4, 13.5, 7.6, 9.6, 14.4, 8.4, "pi_pnl_black"), (9.2, 14.2, 9.2, 9.7, 14.4, 9.7, "pi_pnl_red")]
    return out


def pull_station():
    """The manual pull station at the door (on the wall, a block and a half up): the red box, the white PULL DOWN
    handle in its recess, the conduit up the wall."""
    F = "pi_csr_pull_face"
    out = [(4.4, 2.0, 0.0, 11.6, 11.6, 2.4, {"*": "pi_pnl_red", "south": F})]
    out += [(5.8, 6.6, 2.4, 10.2, 7.8, 3.6, "pi_pnl_white"), (6.0, 7.8, 2.4, 6.8, 8.6, 3.2, "pi_pnl_white"), (9.2, 7.8, 2.4, 10.0, 8.6, 3.2, "pi_pnl_white")]
    out += [(7.4, 11.6, 0.0, 8.6, 16.0, 1.2, "pi_chrome")]
    return out


# ------------------------------------------------------------------------------------------------ the pieces

def _v(boxes_of, hangered):
    """A piece's variants: by train (and with a hanger)."""
    vs = [boxes_of(t) for t in TRAINS]
    if hangered:
        vs += [hangered(t) for t in TRAINS]
    return vs


PIECES = {}
for _tier, _name in (("power", "Power"), ("control", "Control"), ("instr", "Instrument")):
    PIECES[f"cable_tray_{_tier}"] = {"display": f"Cable Tray ({_name})", "boxes": [], "vboxes": _v(
        lambda t, tier=_tier: tray(tier, t), lambda t, tier=_tier: with_hanger(tray(tier, t), tier, t))}
    PIECES[f"cable_tray_{_tier}_drop"] = {"display": f"Cable Tray ({_name}, Bundle Out)", "boxes": [], "vboxes": _v(
        lambda t, tier=_tier: drop_side(tier, t), lambda t, tier=_tier: with_hanger(drop_side(tier, t), tier, t))}
PIECES["cable_tray_power_up"] = {"display": "Cable Tray (Power, Bundle Up)", "boxes": [], "vboxes": _v(drop_up, None)}
PIECES["cable_bundle"] = {"display": "Cable Bundle", "boxes": [], "vboxes": _v(lambda t: bundle_y(t, 0.0, 16.0, ties=(3.0, 11.0)), None)}
PIECES["cable_bundle_elbow"] = {"display": "Cable Bundle (Elbow Up)", "boxes": [], "vboxes": _v(elbow_up, None)}
PIECES["cable_bundle_sleeve"] = {"display": "Cable Bundle (Slab Sleeve)", "boxes": [], "vboxes": _v(sleeve, None)}
PIECES["cable_tray_riser"] = {"display": "Cable Tray Riser", "boxes": [], "vboxes": _v(riser, None)}
PIECES["cable_tray_riser_floor"] = {"display": "Cable Tray Riser (Floor Sleeve)", "boxes": [], "vboxes": _v(lambda t: riser(t, True), None)}
PIECES["cable_tray_riser_top"] = {"display": "Cable Tray Riser (Top)", "boxes": [], "vboxes": _v(riser_top, None)}
for _k, _info in PIECES.items():
    _info["variants"] = len(_info["vboxes"])
PIECES.update({
    "cable_tray_rods": {"display": "Cable Tray Hanger Rods", "boxes": hanger_rods()},
    "cable_tray_gap": {"display": "Cable Tray Train Separation", "boxes": [], "variants": 2, "vboxes": [gap(False), gap(True)]},
    "cable_tray_control_tee": {"display": "Cable Tray (Control, Tee)", "boxes": control_tee()},
    "fire_wrap": {"display": "Fire-Wrapped Raceway", "boxes": wrap()},
    "fire_wrap_start": {"display": "Fire-Wrapped Raceway (Start)", "boxes": wrap_start()},
    "fire_wrap_up": {"display": "Fire-Wrapped Raceway (Up)", "boxes": wrap_up(False)},
    "fire_wrap_tee_up": {"display": "Fire-Wrapped Raceway (Tee Up)", "boxes": wrap_up(True)},
    "fire_wrap_rise": {"display": "Fire-Wrapped Rise", "boxes": wrap_rise()},
    "fire_wrap_sleeve": {"display": "Fire-Wrapped Rise (Slab Sleeve)", "boxes": wrap_rise(True)},
    "sprinkler_pipe": {"display": "Sprinkler Branch Line", "boxes": spk_pipe()},
    "sprinkler_pipe_head": {"display": "Sprinkler Branch Line (Pendent Head)", "boxes": spk_pipe(True)},
    "sprinkler_pipe_end": {"display": "Sprinkler Branch Line (End Head)", "boxes": spk_pipe(end=True)},
    "sprinkler_main": {"display": "Sprinkler Cross Main", "boxes": spk_main()},
    "sprinkler_main_cross": {"display": "Sprinkler Cross Main (Cross)", "boxes": spk_main(True, True)},
    "sprinkler_main_end": {"display": "Sprinkler Cross Main (End)", "boxes": spk_main(True, True, end=True)},
    "sprinkler_riser": {"display": "Sprinkler Riser", "boxes": spk_riser()},
    "sprinkler_riser_top": {"display": "Sprinkler Riser (Top)", "boxes": spk_riser(True)},
    "heat_detector": {"display": "Heat Detector", "boxes": heat_detector()},
    "fire_pull_station": {"display": "Fire Alarm Pull Station", "boxes": pull_station()},
})


# ------------------------------------------------------------------------------------------------ textures

def _jacket(rgb, legend=True):
    """A cable's jacket: its colour, a sheen along it, now and then its printed legend."""
    img = Image.new("RGBA", (128, 128), rgb + (255,))
    d = ImageDraw.Draw(img)
    for y in range(0, 128, 16):
        k = 1.12 if y % 32 == 0 else 0.93
        d.rectangle([0, y, 127, y + 7], fill=tuple(min(255, int(c * k)) for c in rgb) + (255,))
    if legend:
        ink = (240, 240, 232, 255) if sum(rgb) < 380 else (30, 30, 32, 255)
        for x in range(4, 128, 44):
            d.rectangle([x, 60, x + 26, 64], fill=ink)
    return img


def _layer(t, thin=False):
    """A tray's layer of cables seen from above (they run along u): many side by side, the train's, black and grey,
    the ties across them."""
    img = Image.new("RGBA", (256, 256), (20, 20, 22, 255))
    d = ImageDraw.Draw(img)
    rgb = JACKET[t]
    w = 6 if thin else 9
    cols = [(70, 72, 76), (40, 40, 42), rgb, (110, 112, 116)] if thin else [rgb, (40, 40, 42), (90, 92, 96), rgb, (40, 40, 42)]
    for k, y in enumerate(range(0, 256, w + 1)):
        c = cols[k % len(cols)]
        d.rectangle([0, y, 255, y + w - 1], fill=c + (255,))
        d.rectangle([0, y, 255, y + 1], fill=tuple(min(255, int(v * 1.25)) for v in c) + (255,))
    for x in (60, 188):
        d.rectangle([x, 0, x + 5, 255], fill=(28, 28, 30, 255))
    return img


def _ends(t, thin=False):
    """The layer's end: cable ends packed side by side, their copper."""
    img = Image.new("RGBA", (128, 128), (16, 16, 18, 255))
    d = ImageDraw.Draw(img)
    r = 5 if thin else 7
    k = 0
    for y in range(r, 128, 2 * r):
        for x in range(r, 128, 2 * r):
            c = JACKET[t] if (k * 7 + y) % 3 == 0 else (52, 52, 54)
            d.ellipse([x - r + 1, y - r + 1, x + r - 1, y + r - 1], fill=c + (255,))
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(190, 120, 70, 255))
            k += 1
    return img


def _tag(t, tier):
    """A tray's tag: its number in black on yellow (the upper part of the square is what the tag shows)."""
    img = Image.new("RGBA", (512, 512), (236, 196, 36, 255))
    d = ImageDraw.Draw(img)
    h = round(512 * TAG_UV[3] / 16)
    d.rectangle([0, 0, 511, h - 1], outline=(30, 30, 32, 255), width=10)
    PP._fit_line(d, (256, h / 2 + 2), f"{TRAIN_NAMES[t]}-{TIER_LETTER[tier]}", round(h * 0.66), 460, (24, 24, 26, 255))
    return img


def _quilt(img):
    d = ImageDraw.Draw(img)
    for y in range(0, 256, 32):
        d.line([(0, y), (255, y)], fill=(196, 194, 184, 255), width=2)
    return d


def _fire_wrap(kind="plain"):
    """The fire barrier blanket: off-white, quilted; plain (the run's top, the rises: a label in the middle third,
    which a rise shows whole), side (the run's long sides: its band with the rating, in the strip WRAP_SIDE_UV shows),
    end (an end's flap)."""
    img = Image.new("RGBA", (256, 256), (222, 220, 210, 255))
    d = _quilt(img)
    red, white = (200, 40, 34, 255), (250, 250, 246, 255)
    if kind == "end":
        d.rectangle([24, 24, 231, 231], outline=(196, 194, 184, 255), width=4)
    elif kind == "plain":
        d.rectangle([90, 92, 166, 164], fill=red)
        for k, line in enumerate(("1 HR", "FIRE", "WRAP")):
            d.text((128, 104 + k * 24), line, font=PP.rfont(20, True), fill=white, anchor="mm")
    else:
        h = round(256 * WRAP_SIDE_UV[3] / 16)
        d.rectangle([0, h * 0.3, 255, h * 0.72], fill=red)
        PP._fit_line(d, (128, h * 0.51), "1 HR FIRE BARRIER", round(h * 0.34), 190, white)
    return img


def _fire_board(edge=False):
    """The barrier board in a gap between the trains' areas: grey calcium silicate board, its screws, its legend (near
    its top, inside the part its face shows)."""
    img = Image.new("RGBA", (256, 256), (178, 176, 168, 255))
    d = ImageDraw.Draw(img)
    if edge:
        return img
    for x in (24, 232):
        for y in (8, 128, 248):
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(120, 120, 116, 255))
    d.rectangle([16, 22, 239, 84], fill=(200, 40, 34, 255))
    PP._fit_line(d, (128, 42), "TRAIN SEPARATION", 24, 210, (250, 250, 246, 255))
    PP._fit_line(d, (128, 68), "FIRE BARRIER", 20, 210, (250, 250, 246, 255))
    return img


def _soffit():
    """The slab's underside (the ceiling here): board-formed concrete, painted off-white, the form joints."""
    img = M.painted((206, 204, 196), 621, seams=(), chip_count=0, drip_count=0, dirt=0.08)
    d = ImageDraw.Draw(img)
    w, h = img.size
    for y in (h // 4, 3 * h // 4):
        d.line([(0, y), (w, y)], fill=(184, 182, 174, 255), width=2)
    return img


def _pull_face():
    """The pull station's face: FIRE over the handle, PULL DOWN under it."""
    img = Image.new("RGBA", (512, 512), (196, 36, 30, 255))
    d = ImageDraw.Draw(img)
    s = 512 / 16
    X = lambda x: x * s
    Y = lambda y: (16 - y) * s
    PP._fit_line(d, (X(8.0), Y(10.2)), "FIRE", 70, X(6.6), (250, 250, 246, 255))
    PP._fit_line(d, (X(8.0), Y(5.2)), "PULL DOWN", 34, X(6.4), (250, 250, 246, 255))
    PP._fit_line(d, (X(8.0), Y(3.6)), "SPRINKLER RELEASE", 22, X(6.6), (250, 250, 246, 255))
    d.rectangle([X(5.6), Y(8.8), X(10.4), Y(6.4)], fill=(70, 12, 10, 255))
    return img


def makers():
    out = {}
    for t in TRAINS:
        out[f"pi_csr_jkt_{t}"] = lambda t=t: _jacket(JACKET[t])
        out[f"pi_csr_band_{t}"] = lambda t=t: PP._solid(BAND[t], 600 + TRAINS.index(t), 0.03)
        out[f"pi_csr_layer_{t}"] = lambda t=t: _layer(t)
        out[f"pi_csr_ilayer_{t}"] = lambda t=t: _layer(t, True)
        out[f"pi_csr_ends_{t}"] = lambda t=t: _ends(t)
        out[f"pi_csr_iends_{t}"] = lambda t=t: _ends(t, True)
        for tier in TIERS:
            out[f"pi_csr_tag_{t}{TIER_LETTER[tier].lower()}"] = lambda t=t, tier=tier: _tag(t, tier)
    out.update({
        "pi_csr_jkt_black": lambda: _jacket((40, 40, 42)), "pi_csr_jkt_grey": lambda: _jacket((104, 106, 110), False),
        "pi_fire_wrap": lambda: _fire_wrap(), "pi_fire_wrap_end": lambda: _fire_wrap("end"), "pi_fire_wrap_side": lambda: _fire_wrap("side"),
        "pi_fire_board": lambda: _fire_board(), "pi_fire_board_edge": lambda: _fire_board(True),
        "pi_spk_red": lambda: PP._solid((186, 34, 28), 611, 0.04), "pi_spk_brass": lambda: PP._solid((196, 160, 70), 612, 0.03),
        "pi_spk_bulb": lambda: PP._solid((230, 40, 30), 613, 0.0), "pi_slab_soffit": _soffit, "pi_csr_pull_face": _pull_face,
    })
    return out


SHARP = ({f"pi_csr_tag_{t}{TIER_LETTER[tier].lower()}" for t in TRAINS for tier in TIERS}
         | {"pi_fire_wrap", "pi_fire_wrap_side", "pi_fire_board", "pi_csr_pull_face"})


# ------------------------------------------------------------------------------------------------ the fire panels
# Built as the diesel rooms' fittings (tools/diesel_fittings.py): a front drawn at 512 px a block, every part out of it
# a real box, what moves the game's (live_panels.json; nuclear/SpreadingRoomFire.java). Frames: x right as you face it,
# y up from the floor (two blocks: 0..32), z out of the wall toward you.
VALVE, RELEASE = "csr_valve_station", "csr_release_panel"
WHITE, BLACK, REDC = (238, 238, 232, 255), (24, 24, 26, 255), (190, 34, 28, 255)
LENS = {"green": "pi_lens_green", "red": "pi_lens_red", "amber": "pi_lens_amber", "white": "pi_lens_white"}
LAMP_RGB = {"green": (40, 180, 70), "red": (230, 50, 40), "amber": (240, 170, 30), "white": (240, 240, 230)}


def _fit(f, cx, cy, text, size, max_w, fill=PP.INK, anchor="mm"):
    while size > 5 and f.d.textlength(text, font=PP.cfont(size, True)) > max_w * PP.U:
        size -= 0.5
    f.T(cx, cy, text, size, fill=fill, bold=True, anchor=anchor)


def _screws(b, pts, z, tex="pi_steel"):
    for x, y in pts:
        b += PP._sc_round(x, y, z, z + 0.12, 0.42, tex)


LAMP_R = 0.45


def _lamp_boxes(x, y, colour, z):
    """A pilot lamp's chrome bezel and its lens (lit live)."""
    return PP._sc_round(x, y, z, z + 0.3, LAMP_R + 0.22, "pi_chrome") + PP._sc_round(x, y, z + 0.3, z + 0.5, LAMP_R, LENS[colour])


def _gauge(f, cx, cy, r, scale, unit, ch, z, label, red=None):
    """A round pressure gauge's dial in the drawing: the white face, its scale, the red band, the unit; the needle live
    from ch (the gauge's face at z)."""
    lo, hi = scale
    start, sweep = 225, 270
    f.E(cx, cy, r + 0.3, fill=(30, 30, 32, 255))
    f.E(cx, cy, r, fill=(246, 246, 240, 255))
    if red:
        a0, a1 = (math.radians(start - sweep * (v - lo) / (hi - lo)) for v in red)
        for k in range(30):
            a = a0 + (a1 - a0) * k / 29
            f.E(cx + math.cos(a) * r * 0.84, cy + math.sin(a) * r * 0.84, r * 0.07, fill=(200, 40, 34, 255))
    for k in range(21):
        a = math.radians(start - sweep * k / 20)
        major = k % 5 == 0
        f.line([(cx + math.cos(a) * r * (0.72 if major else 0.8), cy + math.sin(a) * r * (0.72 if major else 0.8)),
                (cx + math.cos(a) * r * 0.92, cy + math.sin(a) * r * 0.92)], PP.INK, 3 if major else 1)
        if major:
            v = lo + (hi - lo) * k / 20
            f.T(cx + math.cos(a) * r * 0.5, cy + math.sin(a) * r * 0.5, f"{v:g}", 5.5, bold=True)
    f.T(cx, cy - r * 0.42, unit, 5.5, bold=True)
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": r * 0.9, "z": z + 0.35, "lo": lo, "hi": hi, "start": start, "sweep": sweep, "ch": ch})
    for k, line in enumerate(label.split("|")):
        _fit(f, cx, cy - r - 0.75 - k * 0.6, line, 6, 2 * r + 1.6, fill=BLACK)


def _gauge_boxes(b, F, cx, cy, r, z):
    """The gauge's black case (its face the drawing), the chrome bezel round the glass."""
    b += PP._sc_round(cx, cy, z - 1.0, z - 0.3, r + 0.3, "pi_pnl_black")
    b += [(cx - r * 0.76, cy - r * 0.76, z - 0.15, cx + r * 0.76, cy + r * 0.76, z + 0.05, {"*": "pi_pnl_black", "south": F})]
    b += PP._oct_ring_z(cx, cy, r * 0.8, r + 0.3, z - 0.22, z + 0.45, "pi_chrome")


# ---- the releasing panel: a red steel cabinet at eye level (its window over the display and the lamps), its keys
# under it; the conduit up the wall from its top.
RP_BOX = (3.0, 11.0, 13.0, 25.2)
RP_D = 2.4
RP_WIN = (3.8, 15.6, 12.2, 22.8)           # the window (its frame round it)
RP_LCD = (4.4, 19.95, 11.6, 22.2)
# The lamps in three columns of two, their legends under them (no two bezels or legends touch; none across the
# blocks' seam at 16).
RP_LAMPS = [(5.2, 19.0, "green", "csr_power", "AC POWER"), (5.2, 17.1, "amber", "csr_sup", "SUPERVISORY"),
            (8.0, 19.0, "red", "csr_alarm", "ALARM"), (8.0, 17.1, "amber", "csr_trbl", "TROUBLE"),
            (10.8, 19.0, "red", "csr_rel", "RELEASED"), (10.8, 17.1, "amber", "csr_air", "LOW AIR")]
RP_KEYS = [(4.9, 13.7, "pi_pnl_yellow", "ACK"), (7.0, 13.7, "pi_pnl_white", "SILENCE"), (9.1, 13.7, "pi_pnl_black", "RESET"),
           (11.2, 13.7, "pi_pnl_black", "DRILL")]


# The areas a pre-action system protects, by the panels' labels (turbine hall part 3: the panels lettered for their own
# area, the warehouse's and the turbine building's lube oil room's as well as the spreading room's): the releasing
# panel's zone line, the valve station sign's two lines.
ZONES = {"CSR": ("CABLE SPREADING ROOM  ZONE 1", ("CABLE SPREADING", "ROOM - ZONE 1")),
         "WH": ("WAREHOUSE  ZONE 1", ("WAREHOUSE", "ZONE 1")),
         "LOR": ("TB LUBE OIL ROOM  ZONE 1", ("TB LUBE OIL", "ROOM - ZONE 1"))}
ZONE_LABELS = list(ZONES)


def release_panel(label=None):
    f = PP.Front(1, 631)
    x0, y0, x1, y1 = RP_BOX
    f.R(x0, y0, x1, y1, fill=REDC)
    _fit(f, 8.0, 24.35, "PRE-ACTION RELEASING PANEL", 7.5, x1 - x0 - 0.8, fill=WHITE)
    _fit(f, 8.0, 23.55, ZONES[label or "CSR"][0], 6, x1 - x0 - 0.8, fill=WHITE)
    # The window: its black surround, the display (live text), the lamps and their legends.
    f.R(*RP_WIN, fill=(36, 36, 38, 255))
    lx0, ly0, lx1, ly1 = RP_LCD
    f.R(lx0, ly0, lx1, ly1, fill=(20, 40, 28, 255))
    f.text(lx0 + 0.3, (ly0 + ly1) / 2 + 0.38, RP_D + 0.3, 0.55, (130, 240, 160), "csr_lcd1", "left")
    f.text(lx0 + 0.3, (ly0 + ly1) / 2 - 0.42, RP_D + 0.3, 0.55, (130, 240, 160), "csr_lcd2", "left")
    for x, y, colour, ch, label in RP_LAMPS:
        f.live.append({"t": "lamp", "x": x, "y": y, "r": LAMP_R, "z": RP_D + 0.52, "colour": list(LAMP_RGB[colour]), "ch": ch})
        _fit(f, x, y - 0.98, label, 5.5, 2.6, fill=WHITE)
    for x, y, tex, label in RP_KEYS:
        _fit(f, x, y - 1.15, label, 5.5, 2.0, fill=WHITE)
    return f


def release_panel_boxes():
    F = f"pi_{RELEASE}_f"
    x0, y0, x1, y1 = RP_BOX
    b = [(x0, y0, 0.2, x1, y1, RP_D, {"*": "pi_pnl_red", "south": F}), (x0 + 0.4, y1, 0.4, x1 - 0.4, y1 + 0.3, RP_D - 0.3, "pi_pnl_red")]
    # The window's frame and its glass over the display.
    wx0, wy0, wx1, wy1 = RP_WIN
    b += [(wx0 - 0.25, wy0 - 0.25, RP_D, wx0, wy1 + 0.25, RP_D + 0.5, "pi_pnl_black"), (wx1, wy0 - 0.25, RP_D, wx1 + 0.25, wy1 + 0.25, RP_D + 0.5, "pi_pnl_black"),
          (wx0, wy0 - 0.25, RP_D, wx1, wy0, RP_D + 0.5, "pi_pnl_black"), (wx0, wy1, RP_D, wx1, wy1 + 0.25, RP_D + 0.5, "pi_pnl_black"),
          (wx0, wy0, RP_D + 0.75, wx1, wy1, RP_D + 0.8, {"south": PP.SC_GLASS, "north": PP.SC_GLASS})]
    for x, y, colour, ch, label in RP_LAMPS:
        b += _lamp_boxes(x, y, colour, RP_D)
    for x, y, tex, label in RP_KEYS:
        b += PP._sc_round(x, y, RP_D, RP_D + 0.25, 0.75, "pi_chrome") + PP._sc_round(x, y, RP_D + 0.25, RP_D + 0.6, 0.52, tex)
    # The key switch (the panel's door lock) and the screws; the conduit up the wall out of its top.
    b += PP._sc_round(10.3, 11.75, RP_D, RP_D + 0.3, 0.45, "pi_chrome")
    _screws(b, ((x0 + 0.6, y0 + 0.6), (x1 - 0.6, y0 + 0.6), (x0 + 0.6, y1 - 0.6), (x1 - 0.6, y1 - 0.6)), RP_D)
    b += [(7.4, y1 + 0.3, 1.0, 8.6, 31.4, 2.2, "pi_chrome"), (7.15, y1 + 0.3, 0.75, 8.85, y1 + 0.9, 2.45, "pi_steel_dark")]
    return b


# ---- the valve station on the riser: the supply up out of the floor, the OS&Y control valve (its handwheel out
# toward you, the tamper switch on its yoke), the pre-action valve (its clapper cover, the solenoid that opens it),
# the trim: the water supply gauge and the system air gauge on their stems, the main drain valve and its drain down to
# the floor; the riser on up to the next block; the valve's sign on the wall beside it.
VS_X, VS_Z, VS_R = 8.0, 4.0, 1.0           # the riser's middle (across, out of the wall) and its radius
VS_WATER, VS_AIR = (4.4, 20.4, 1.15), (11.6, 20.4, 1.15)
VS_GZ = VS_Z + 1.4                        # the gauges' faces
VS_SIGN = (0.6, 25.0, 6.6, 31.0)


def valve_station(label=None):
    f = PP.Front(1, 633)
    _gauge(f, *VS_WATER, (0, 300), "PSI", "csr_water", VS_GZ, "WATER|SUPPLY", red=(250, 300))
    _gauge(f, *VS_AIR, (0, 80), "PSI", "csr_air", VS_GZ, "SYSTEM|AIR", red=(0, 10))
    x0, y0, x1, y1 = VS_SIGN
    f.R(x0, y0, x1, y1, fill=(250, 250, 246, 255), outline=REDC, width=4)
    f.R(x0, y1 - 1.5, x1, y1, fill=REDC)
    _fit(f, (x0 + x1) / 2, y1 - 0.75, "SPRINKLER", 7, x1 - x0 - 0.5, fill=WHITE)
    z1, z2 = ZONES[label or "CSR"][1]
    for k, (line, size) in enumerate((("PRE-ACTION VALVE", 6.5), (z1, 6), (z2, 6), ("CONTROL VALVE", 6),
                                      ("NORMALLY OPEN", 6))):
        _fit(f, (x0 + x1) / 2, y1 - 2.2 - k * 0.78, line, size, x1 - x0 - 0.5, fill=BLACK)
    return f


def valve_station_boxes():
    F = f"pi_{VALVE}_f"
    b = []
    x, z, r = VS_X, VS_Z, VS_R
    # The riser (in its pieces, so the valves sit on it), the flanges.
    for y0, y1 in ((0.0, 3.4), (8.2, 10.6), (19.4, 31.4)):
        b += oct_y(x, z, y0, y1, r, RED)
    for y in (3.4, 7.6, 10.6, 18.6):
        b += PP.round_y(x, z, r + 0.55, y, y + 0.8, RED)
    # The OS&Y gate valve: its body, the yoke out toward you, the stem, the handwheel; the tamper switch.
    b += [(x - 1.5, 4.0, z - 1.3, x + 1.5, 7.6, z + 1.3, RED)]
    b += [(x - 1.0, 5.0, z + 1.3, x - 0.6, 6.6, z + 4.4, RED), (x + 0.6, 5.0, z + 1.3, x + 1.0, 6.6, z + 4.4, RED),
          (x - 1.0, 5.0, z + 4.4, x + 1.0, 6.6, z + 4.8, RED), (x - 0.15, 5.65, z + 1.3, x + 0.15, 5.95, z + 6.6, "pi_chrome")]
    b += PP._oct_ring_z(x, 5.8, 1.6, 1.95, z + 5.9, z + 6.3, RED)
    b += [(x - 1.65, 5.65, z + 6.0, x + 1.65, 5.95, z + 6.22, RED), (x - 0.15, 4.15, z + 5.92, x + 0.15, 7.45, z + 6.14, RED)]
    b += [(x + 1.0, 4.4, z + 1.6, x + 2.6, 6.2, z + 3.4, "pi_pnl_black"), (x + 2.6, 5.0, z + 2.2, x + 3.4, 5.6, z + 2.8, "pi_chrome")]
    # The pre-action valve: its body, the clapper cover bolted on its front, the solenoid on its side.
    b += [(x - 2.4, 11.2, z - 2.0, x + 2.4, 18.8, z + 2.2, RED), (x - 1.6, 12.4, z + 2.2, x + 1.6, 17.6, z + 2.8, RED)]
    _screws(b, ((x - 1.2, 12.8), (x + 1.2, 12.8), (x - 1.2, 17.2), (x + 1.2, 17.2)), z + 2.8, "pi_steel_dark")
    b += oct_x(z, 15.0, x + 2.4, x + 3.6, 0.35, "pi_steel") + oct_y(x + 4.2, z, 13.8, 16.6, 0.7, "pi_pnl_black")
    b += [(x + 3.8, 16.6, z - 0.4, x + 4.6, 17.4, z + 0.4, "pi_pnl_black"), (x + 4.0, 17.4, z - 0.25, x + 4.4, 31.4, z + 0.25, "pi_chrome")]
    # The trim: the gauges on their stems (out of the valve's sides), the main drain valve, its drain down to the floor.
    for (gx, gy, gr), side in ((VS_WATER, -1), (VS_AIR, 1)):
        b += [(min(x + side * 2.4, gx), 17.6, z - 0.25, max(x + side * 2.4, gx), 18.1, z + 0.25, "pi_chrome"),
              (gx - 0.25, 18.1, z - 0.25, gx + 0.25, gy - gr - 0.3, z + 0.25, "pi_chrome"),
              (gx - 0.25, gy - 0.25, z + 0.25, gx + 0.25, gy + 0.25, VS_GZ - 0.9, "pi_chrome")]
        _gauge_boxes(b, F, gx, gy, gr, VS_GZ)
    b += oct_x(z, 12.4, x - 4.2, x - 2.4, 0.4, RED) + [(x - 3.8, 11.8, z - 0.65, x - 2.6, 13.0, z + 0.85, "pi_spk_brass")]
    b += [(x - 3.4, 12.25, z + 0.9, x - 1.4, 12.55, z + 1.2, "pi_pnl_red")]
    b += oct_y(x - 4.6, z, 0.4, 12.55, 0.4, RED) + [(x - 5.2, 0.0, z - 0.6, x - 4.0, 0.4, z + 0.6, "pi_steel_dark")]
    # Its pipe clamps to the wall; the sign on the wall beside it.
    for y in (2.0, 26.0):
        b += [(x - 0.3, y, 0.0, x + 0.3, y + 0.8, z - r, "pi_steel_dark"), (x - 1.4, y - 0.2, 0.0, x + 1.4, y + 1.0, 0.3, "pi_steel_dark")]
    x0, y0, x1, y1 = VS_SIGN
    b += [(x0, y0, 0.1, x1, y1, 0.3, {"*": "pi_pnl_white", "south": F})]
    return b


# ------------------------------------------------------------------------------------------------ the signs
# The areas' signs on the long walls at eye level, and the sprinkler system's beside its panels (plant_panels' sign
# plates: a 433 by 512 px picture).
SIGNS = {"sign_csr_train_a": "Train A Cable Area Sign", "sign_csr_train_b": "Train B Cable Area Sign",
         "sign_csr_train_n": "Non-Safety Cable Area Sign", "sign_csr_preaction": "Pre-Action Sprinkler System Sign"}


def sign_image(kind):
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    fit = PP._fit_line
    M_ = W - 56
    BLACK_, WHITE_ = (20, 20, 22, 255), (250, 250, 246, 255)
    if kind.startswith("sign_csr_train_"):
        t = kind[-1]
        head = {"a": "TRAIN A", "b": "TRAIN B", "n": "NON-SAFETY"}[t]
        colour = BAND[t] + (255,)
        ink = BLACK_ if t == "a" else WHITE_
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE_, outline=BLACK_, width=10)
        d.rectangle([10, 10, W - 11, 150], fill=colour)
        fit(d, (W // 2, 80), head, 92, W - 70, ink)
        lines = ([("SAFETY RELATED", 46), (f"SEPARATION GROUP {TRAIN_NAMES[t]}", 34), ({"a": "ORANGE", "b": "BLUE"}[t] + " CABLES", 40),
                  ("AND RACEWAYS ONLY", 34)] if t != "n" else
                 [("NO SAFETY-RELATED", 42), ("CABLES IN THIS AREA", 36), ("GREY CABLES", 40), ("AND RACEWAYS ONLY", 34)])
        for k, (line, size) in enumerate(lines):
            fit(d, (W // 2, 206 + k * 62), line, size, M_, BLACK_)
        d.rectangle([40, 456, W - 41, 466], fill=colour)
    elif kind == "sign_csr_preaction":
        RED_ = (196, 34, 28, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE_, outline=RED_, width=12)
        d.rectangle([12, 12, W - 13, 126], fill=RED_)
        fit(d, (W // 2, 70), "FIRE PROTECTION", 56, W - 60, WHITE_)
        for y, line, size in ((172, "PRE-ACTION", 54), (226, "SPRINKLER SYSTEM", 44), (286, "ON FIRE ALARM AND", 30), (326, "RELEASE THE HEADS", 30),
                              (366, "DISCHARGE WATER", 30), (436, "LEAVE THE ROOM", 40)):
            fit(d, (W // 2, y), line, size, M_, RED_ if y == 436 else BLACK_)
    return img


# ------------------------------------------------------------------------------------------------ registering

PANELS = {VALVE: {"display": "Pre-Action Sprinkler Valve Station", "width": 1, "item3d": True, "labels": ZONE_LABELS},
          RELEASE: {"display": "Pre-Action Releasing Panel", "width": 1, "item3d": True, "labels": ZONE_LABELS}}
BUILDERS = {VALVE: (valve_station, valve_station_boxes), RELEASE: (release_panel, release_panel_boxes)}
_BUILT = {}


def built(key, label=None):
    if (key, label) not in _BUILT:
        draw, boxes = BUILDERS[key]
        _BUILT[(key, label)] = (draw(label), boxes())
    return _BUILT[(key, label)]


_TRAY = {"P": "#c:plates/iron", "N": "minecraft:iron_nugget"}
CRAFTING = {
    "cable_tray_power": (["P P", "WNW"], {**_TRAY, "W": "#c:wires/copper"}, 8),   # (item 22: not the tower's cable tray's)
    "cable_tray_control": (["P P", "SPS"], {**_TRAY, "S": "minecraft:string"}, 8),
    "cable_tray_instr": (["P P", "RPR"], {**_TRAY, "R": "minecraft:redstone"}, 8),
    "cable_tray_power_drop": (["PWP"], {**_TRAY, "W": "#c:wires/copper"}, 2),
    "cable_tray_control_drop": (["PSP"], {**_TRAY, "S": "minecraft:string"}, 2),
    "cable_tray_instr_drop": (["PRP"], {**_TRAY, "R": "minecraft:redstone"}, 2),
    "cable_tray_power_up": (["W", "P"], {**_TRAY, "W": "#c:wires/copper"}, 2),
    "cable_tray_control_tee": (["PPP", " P "], _TRAY, 2),
    "cable_tray_rods": (["N N", "N N"], _TRAY, 4),
    "cable_tray_gap": (["PCP"], {**_TRAY, "C": "minecraft:smooth_stone"}, 2),
    "cable_bundle": (["S", "S", "S"], {"S": "minecraft:string"}, 4),
    "cable_bundle_elbow": (["SS", " S"], {"S": "minecraft:string"}, 2),
    "cable_bundle_sleeve": (["S", "R"], {"S": "minecraft:string", "R": "minecraft:red_dye"}, 2),
    "cable_tray_riser": (["P P", "PWP", "P P"], {**_TRAY, "W": "#c:wires/copper"}, 4),
    "cable_tray_riser_floor": (["PWP", "R R"], {**_TRAY, "W": "#c:wires/copper", "R": "minecraft:red_dye"}, 2),
    "cable_tray_riser_top": (["PPP", "PW "], {**_TRAY, "W": "#c:wires/copper"}, 2),
    "fire_wrap": (["WWW", "P P", "WWW"], {**_TRAY, "W": "minecraft:white_wool"}, 4),
    "fire_wrap_start": (["WW", "PW"], {**_TRAY, "W": "minecraft:white_wool"}, 2),
    "fire_wrap_up": (["W ", "WW"], {"W": "minecraft:white_wool"}, 2),
    "fire_wrap_tee_up": (["W", "W", "W"], {"W": "minecraft:white_wool"}, 2),
    "fire_wrap_rise": (["W", "W"], {"W": "minecraft:white_wool"}, 4),
    "fire_wrap_sleeve": (["W", "R"], {"W": "minecraft:white_wool", "R": "minecraft:red_dye"}, 2),
    "sprinkler_pipe": (["RRR", "III"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 8),
    "sprinkler_pipe_head": (["RIR", " G "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "G": "minecraft:gold_nugget"}, 4),
    "sprinkler_pipe_end": (["RI ", " G "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "G": "minecraft:gold_nugget"}, 4),
    "sprinkler_main": (["RIR", "RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "sprinkler_main_cross": (["RIR", "III", "RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "sprinkler_main_end": (["RIR", "I I"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 2),
    "sprinkler_riser": (["I", "R", "I"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "sprinkler_riser_top": (["II", "RI"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 2),
    "heat_detector": (["Q", "B"], {"Q": "minecraft:quartz", "B": "minecraft:black_dye"}, 2),
    "fire_pull_station": (["RRR", "RLR", "RQR"], {"R": "minecraft:red_dye", "L": "minecraft:lever", "Q": "minecraft:quartz"}, 1),
    VALVE: (["RIR", "GVG", "RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "G": "minecraft:clock", "V": "#c:plates/iron"}, 1),
    RELEASE: (["PRP", "LCL", "PBP"], {"P": "#c:plates/steel", "R": "minecraft:red_dye", "L": "minecraft:redstone_lamp", "C": "jugcraft:basic_circuit",
                                     "B": "minecraft:stone_button"}, 1),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": {"a": "minecraft:orange_dye", "b": "minecraft:blue_dye", "n": "minecraft:gray_dye",
                                                             "p": "minecraft:red_dye"}[k[-1] if k != "sign_csr_preaction" else "p"],
                             "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
}


def _signs():
    old = PP._sign_image

    def draw(kind):
        return sign_image(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})
    PP.EXTRA_MAKERS.append(lambda: {f"pi_{k}": (lambda k=k: sign_image(k)) for k in SIGNS})
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], 2, "panel")
        info["panel"] = key
        PP.PANELS[key] = info
        PP.DRAWINGS[key] = lambda label=None, key=key: built(key, label)[0]
        PP.CABINETS[key] = lambda key=key: built(key)[1]
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP | {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(len(ZONE_LABELS))
                         for p in range(2 * PANELS[key]["width"])}
    _signs()


register()
