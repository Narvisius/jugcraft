"""The Control Room HVAC Equipment Room's own pieces (round 26, #11; Justin: "Real ducts with dampers"): the plant
kit's block pieces for the ducts and pipes between the machines (tools/hvac_boards.py), laid out by
tools/medium_plant.py top_floor:

- galvanized rectangular duct (13 px square, a half flange at each end so every joint shows one flange), straight, on a
  trapeze hanger from the roof, risers, elbows (across, down, up), tees (across, down), labelled runs with the flow's
  arrow (OUTSIDE AIR, FILTERED AIR, CR SUPPLY AIR, CR RETURN AIR), the floor sleeve with its fire damper where a duct goes
  down into the control room, the control room's ceiling diffusers and return grilles;
- the isolation dampers (labelled: each intake's normal outside air damper, its emergency intake damper, each filtration
  unit's outlet damper), open or shut as the HVAC control panel sets them (the variant: 2 x label + open): the actuator
  on its bracket, the linkage, the blade shaft's position pointer and its OPEN / SHUT plate, the damper's tag;
- each intake's plenum against its louvres (joined side by side: its end panels where the row ends), its base, outlets,
  access door and the radiation and toxic gas monitors' sample point;
- the chilled water pipes (insulated, aluminium jacketed): straight, on a clevis hanger or a trapeze beside the pipe
  over it, risers, elbows, the labelled runs (CHW SUPPLY, CHW RETURN) with the flow's arrow.

Models in model pixels for the piece facing north: x across, y up, z from its back (north, z 0) to its front (south, +z).
A run's piece "facing north" runs along z, its flow (where it has one) toward +z. lay() picks each block's piece and
facing from where its neighbours are."""
import math

from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP

# ------------------------------------------------------------------------------------------------ duct
D0, D1 = 1.5, 14.5          # the duct's sides (13 px square, in the middle of its block)
F0, F1 = 0.9, 15.1          # its flanges' outer edge
HF = 0.25                   # a half flange's thickness (two meet at every joint)
GALV, GALV_DARK = "pi_hv_galv", "pi_hv_galv_dark"


def _flange_ring(axis, t0, t1, c0=8.0, c1=8.0):
    """A duct's flange: four bars round its sides, from t0 to t1 along axis ("z" or "x": a run; "y": a riser)."""
    a0, a1, b0, b1 = D0, D1, F0, F1
    if axis == "z":
        return [(b0, a1, t0, b1, b1, t1, {"*": GALV_DARK, "down": None}), (b0, b0, t0, b1, a0, t1, {"*": GALV_DARK, "up": None}),
                (b0, a0, t0, a0, a1, t1, {"*": GALV_DARK, "east": None}), (a1, a0, t0, b1, a1, t1, {"*": GALV_DARK, "west": None})]
    if axis == "x":
        return [(t0, a1, b0, t1, b1, b1, {"*": GALV_DARK, "down": None}), (t0, b0, b0, t1, a0, b1, {"*": GALV_DARK, "up": None}),
                (t0, a0, b0, t1, a1, a0, {"*": GALV_DARK, "south": None}), (t0, a0, a1, t1, a1, b1, {"*": GALV_DARK, "north": None})]
    return [(b0, t0, a1, b1, t1, b1, {"*": GALV_DARK, "north": None}), (b0, t0, b0, b1, t1, a0, {"*": GALV_DARK, "south": None}),
            (b0, t0, a0, a0, t1, a1, {"*": GALV_DARK, "east": None}), (a1, t0, a0, b1, t1, a1, {"*": GALV_DARK, "west": None})]


def _ends(conn):
    """Half flanges at each connected end of a piece (conn: the faces it opens on: "n", "s", "e", "w", "u", "d")."""
    out = []
    for c in conn:
        out += {"n": lambda: _flange_ring("z", 0, HF), "s": lambda: _flange_ring("z", 16 - HF, 16),
                "w": lambda: _flange_ring("x", 0, HF), "e": lambda: _flange_ring("x", 16 - HF, 16),
                "d": lambda: _flange_ring("y", 0, HF), "u": lambda: _flange_ring("y", 16 - HF, 16)}[c]()
    return out


def _body(x0, y0, z0, x1, y1, z1, open_faces=(), tex=GALV):
    faces = {"*": tex}
    for f in open_faces:
        faces[f] = None
    return (x0, y0, z0, x1, y1, z1, faces)


def duct_straight():
    return [_body(D0, D0, 0, D1, D1, 16, ("north", "south"))] + _ends("ns")


def _trapeze(top=32.0):
    """A trapeze hanger under the duct: the angle across under it, the threaded rods up beside it to the roof (top: the
    model y of the roof's underside), their nuts and the roof's anchors."""
    y0 = F0 - 0.9
    out = [(-0.6, y0, 7.2, 16.6, y0 + 0.5, 8.8, "pi_steel_dark"), (-0.6, y0 + 0.5, 7.2, 16.6, y0 + 0.9, 7.6, "pi_steel_dark")]
    for x in (-0.3, 15.7):
        out += [(x, y0 + 0.5, 7.75, x + 0.6, top - 0.4, 8.35, "pi_steel"), (x - 0.25, y0 - 0.35, 7.6, x + 0.85, y0, 8.5, "pi_steel_dark"),
                (x - 0.5, top - 0.4, 7.3, x + 1.1, top, 8.8, "pi_steel_dark")]
    return out


def duct_hanger(top=32.0):
    return duct_straight() + _trapeze(top)


def duct_riser():
    return [_body(D0, 0, D0, D1, 16, D1, ("up", "down"))] + _ends("ud")


def duct_elbow():
    """North to east, square throated (the heel at the south-west), its turning vanes' access door on the heel."""
    out = [_body(D0, D0, 0, D1, D1, D0, ("north", "south")),
           _body(D0, D0, D0, D1, D1, D1, ("north", "east")),
           _body(D1, D0, D0, 16, D1, D1, ("west", "east"))]
    out += [(D0 + 3.0, D0 + 3.0, D1, D1 - 3.0, D1 - 3.0, D1 + 0.2, {"*": GALV_DARK, "north": None})]
    return out + _ends("ne")


def duct_elbow_down():
    """North, turning down."""
    out = [_body(D0, D0, 0, D1, D1, D0, ("north", "south")),
           _body(D0, D0, D0, D1, D1, D1, ("north", "down")),
           _body(D0, 0, D0, D1, D0, D1, ("up", "down"))]
    return out + _ends("nd")


def duct_elbow_up():
    """North, turning up."""
    out = [_body(D0, D0, 0, D1, D1, D0, ("north", "south")),
           _body(D0, D0, D0, D1, D1, D1, ("north", "up")),
           _body(D0, D1, D0, D1, 16, D1, ("up", "down"))]
    return out + _ends("nu")


def duct_tee():
    """The run north to south, a branch east."""
    out = [_body(D0, D0, 0, D1, D1, D0, ("north", "south")),
           _body(D0, D0, D0, D1, D1, D1, ("north", "south", "east")),
           _body(D0, D0, D1, D1, D1, 16, ("north", "south")),
           _body(D1, D0, D0, 16, D1, D1, ("west", "east"))]
    return out + _ends("nse")


def duct_tee_down():
    """The run north to south, a branch down."""
    out = [_body(D0, D0, 0, D1, D1, D0, ("north", "south")),
           _body(D0, D0, D0, D1, D1, D1, ("north", "south", "down")),
           _body(D0, D0, D1, D1, D1, 16, ("north", "south")),
           _body(D0, 0, D0, D1, D0, D1, ("up", "down"))]
    return out + _ends("nsd")


# Label plates on a run's sides: the flow toward +z (south) reads the arrow right on the west side and left on the east
# (each side's picture is drawn for it). LABELS: name, its legend, its colours.
DUCT_LABELS = {"oa": "OUTSIDE AIR", "filtered": "FILTERED AIR", "supply": "CR SUPPLY AIR", "return": "CR RETURN AIR"}
PIPE_LABELS = {"chws": "CHILLED WATER SUPPLY", "chwr": "CHILLED WATER RETURN"}
# Every lettered strip on one of four sheets (512 px a block: SHARP), four 4:1 strips a sheet: where each is.
STRIPS = ([f"duct_{k}_{d}" for k in DUCT_LABELS for d in "rl"] + [f"riser_{k}" for k in ("supply", "return")]
          + [f"pipe_{k}_{d}" for k in PIPE_LABELS for d in "rl"]
          + [f"tag_{k}" for k in range(6)] + ["plenum_a", "plenum_b"])


def strip(name):
    """(texture, uv) of a lettered strip."""
    i = STRIPS.index(name)
    sheet, row = divmod(i, 4)
    return (f"pi_hv_strips_{sheet}", [0, row * 4, 16, row * 4 + 4])


def duct_label(kind):
    """A run with its label on both sides (and the arrow of the flow, toward +z)."""
    y0, y1, z0, z1 = 5.0, 11.0, 0.8, 15.2       # 24 by 6: a 4:1 strip
    return duct_straight() + [
        (D0 - 0.12, 6.0, 2.0, D0, 10.0, 14.0, {"*": "pi_pnl_white", "west": strip(f"duct_{kind}_r"), "east": None}),
        (D1, 6.0, 2.0, D1 + 0.12, 10.0, 14.0, {"*": "pi_pnl_white", "east": strip(f"duct_{kind}_l"), "west": None})]


def riser_label(kind):
    """A riser with its label (the arrow up or down) on all four sides."""
    st = strip(f"riser_{kind}")
    return duct_riser() + [
        (D0 - 0.12, 6.0, 2.0, D0, 10.0, 14.0, {"*": "pi_pnl_white", "west": st, "east": None}),
        (D1, 6.0, 2.0, D1 + 0.12, 10.0, 14.0, {"*": "pi_pnl_white", "east": st, "west": None}),
        (2.0, 6.0, D0 - 0.12, 14.0, 10.0, D0, {"*": "pi_pnl_white", "north": st, "south": None}),
        (2.0, 6.0, D1, 14.0, 10.0, D1 + 0.12, {"*": "pi_pnl_white", "south": st, "north": None})]


# ---- the dampers: (tag, its name), the plant's six (each intake's normal outside air isolation, its emergency intake,
# each filtration unit's outlet), A then B.
DAMPERS = [("CR-D-1A", "NORMAL OA ISOLATION"), ("CR-D-1B", "NORMAL OA ISOLATION"), ("CR-D-2A", "EMERGENCY INTAKE"),
           ("CR-D-2B", "EMERGENCY INTAKE"), ("CR-D-3A", "CREFS OUTLET"), ("CR-D-3B", "CREFS OUTLET")]
DAMPER_SECTION = (5.0, 11.0)


def damper():
    """The damper's body (both states): the run, the damper's sleeve round it, its access door (west), the actuator on
    its bracket over it with its crank, the linkage down to the blade shaft's crank arm on the east side."""
    z0, z1 = DAMPER_SECTION
    out = [_body(D0, D0, 0, D1, D1, z0, ("north", "south")), _body(D0, D0, z0, D1, D1, z1, ("north", "south"), GALV_DARK),
           _body(D0, D0, z1, D1, D1, 16, ("north", "south"))]
    out += _flange_ring("z", z0 - 0.4, z0) + _flange_ring("z", z1, z1 + 0.4) + _ends("ns")
    # The access door on the west side beside the damper (its frame, two cam latches).
    out += [(D0 - 0.25, 4.0, 11.6, D0, 12.0, 15.4, {"*": GALV_DARK, "east": None}),
            (D0 - 0.5, 6.0, 12.0, D0 - 0.25, 6.8, 12.6, "pi_pnl_black"), (D0 - 0.5, 9.2, 12.0, D0 - 0.25, 10.0, 12.6, "pi_pnl_black")]
    # The actuator's bracket and the actuator on top (its box, the label strip, the manual override crank), its crank
    # arm and the linkage rod down the east side to the shaft's arm.
    out += [(3.0, D1, 5.4, 13.0, D1 + 0.4, 10.6, "pi_steel_dark"),
            (4.0, D1 + 0.4, 5.8, 12.0, D1 + 3.6, 10.2, {"*": "pi_hv_actuator", "up": "pi_hv_actuator_top!"}),
            (12.0, D1 + 1.6, 7.6, 13.4, D1 + 2.4, 8.4, "pi_steel"), (13.4, D1 + 1.2, 7.4, 14.2, D1 + 2.8, 8.6, "pi_steel_dark"),
            (14.6, 8.6, 7.7, 15.0, D1 + 2.0, 8.3, "pi_steel"),
            (D1, 7.4, 7.4, D1 + 0.9, 8.6, 8.6, "pi_steel_dark"), (D0 - 0.9, 7.4, 7.4, D0, 8.6, 8.6, "pi_steel_dark")]
    return out


def damper_variant(v):
    """What a damper's variant adds (2 x label + open): its tag hung under it on two chains (read from either side), the
    position plate on the shaft at each side and its pointer (toward OPEN along the run, up to SHUT across it)."""
    label, opened = divmod(v, 2)
    tag = strip(f"tag_{label}")
    out = [(7.94, -2.6, 2.3, 8.06, 0.25, 13.7, {"*": "pi_pnl_white", "west": tag, "east": tag}),
           (7.9, 0.25, 4.4, 8.1, D0, 4.6, "pi_steel"), (7.9, 0.25, 11.4, 8.1, D0, 11.6, "pi_steel")]
    for x0, x1, xp0, xp1, face, right in ((D0 - 1.02, D0 - 0.9, D0 - 1.2, D0 - 1.02, "west", 1), (D1 + 0.9, D1 + 1.02, D1 + 1.02, D1 + 1.2, "east", -1)):
        other = "east" if face == "west" else "west"
        out.append((x0, 5.0, 5.0, x1, 11.0, 11.0, {"*": "pi_pnl_white", face: "pi_hv_position!"}))
        if opened:
            z0, z1 = sorted((8.0 - right * 0.3, 8.0 + right * 1.25))
            out.append((xp0, 7.65, z0, xp1, 8.35, z1, "pi_pnl_red"))
        else:
            out.append((xp0, 7.7, 7.65, xp1, 9.25, 8.35, "pi_pnl_red"))
    return out


# ---- through the floor into the control room: the sleeve (in the floor's own block: the floor round it), its fire
# damper's access door and fusible link plate on the sleeve over the floor, the retaining angles; the diffuser and
# the return grille in the control room's ceiling (the ceiling tile's block).
def sleeve():
    """The floor's block round the duct going down through it (its outer faces against the floor's other blocks)."""
    W = {"*": "epoxy_floor", "north": None, "south": None, "east": None, "west": None}
    out = [(0, 0, 0, 16, 16, D0, dict(W, south="epoxy_floor")), (0, 0, D1, 16, 16, 16, dict(W, north="epoxy_floor")),
           (0, 0, D0, D0, 16, D1, dict(W, east="epoxy_floor")), (D1, 0, D0, 16, 16, D1, dict(W, west="epoxy_floor"))]
    out[0] = (0, 0, 0, 16, 16, D0, {"up": "epoxy_floor", "down": "epoxy_floor", "south": "pi_hv_galv_dark"})
    out[1] = (0, 0, D1, 16, 16, 16, {"up": "epoxy_floor", "down": "epoxy_floor", "north": "pi_hv_galv_dark"})
    out[2] = (0, 0, D0, D0, 16, D1, {"up": "epoxy_floor", "down": "epoxy_floor", "east": "pi_hv_galv_dark"})
    out[3] = (D1, 0, D0, 16, 16, D1, {"up": "epoxy_floor", "down": "epoxy_floor", "west": "pi_hv_galv_dark"})
    return out


def riser_fire_damper():
    """The riser's first block up out of the floor: the fire damper's sleeve, the retaining angles round it on the
    floor, its access door (red: FIRE DAMPER) and fusible link plate."""
    out = [_body(D0, 0.6, D0, D1, 16, D1, ("up", "down"))]
    out += [(F0, 0, F0, F1, 0.6, D0, GALV_DARK), (F0, 0, D1, F1, 0.6, F1, GALV_DARK),
            (F0, 0, D0, D0, 0.6, D1, GALV_DARK), (D1, 0, D0, F1, 0.6, D1, GALV_DARK),
            (D0 + 0.4, 0, D0 + 0.4, D1 - 0.4, 0.6, D1 - 0.4, {"up": None, "down": None, "*": GALV_DARK}),
            (D0 + 0.3, 3.0, D1, D1 - 0.3, 11.0, D1 + 0.2, {"*": "pi_pnl_red", "south": "pi_hv_fire_door!", "north": None}),
            (D1 - 2.0, 6.4, D1 + 0.2, D1 - 1.2, 7.6, D1 + 0.6, "pi_pnl_black")]
    return out + _flange_ring("y", 16 - HF, 16)


def ceiling_vent(face):
    """A ceiling tile's block with a supply diffuser (or a return grille) in its underside, hung a hair under it; the
    duct coming down into its top."""
    C = "ceiling_tile"
    return [(0, 0, 0, 16, 16, 16, {"down": C}), (1.0, -0.4, 1.0, 15.0, 0, 15.0, {"*": "pi_pnl_white", "down": f"{face}!", "up": None})]


# ---- an intake's plenum (joined side by side; its end panels where it ends): its front 15.5 from the wall.
PL = 15.8


def _plenum_front(door=False):
    out = [(0, 0, PL - 0.5, 16, 16, PL, {"*": "pi_hv_plenum", "north": None, "east": None, "west": None}),
           (0, 0, PL, 0.4, 16, 16, {"*": "pi_hv_plenum", "north": None})]
    return out


def plenum(kind):
    """One block of an intake plenum: "base" (on the floor: its channel base, drain pan), "plain", "outlet" (a collar
    out of the front, low: the emergency damper's), "door_a" / "door_b" (the access door, its stencil), "detector" (the
    radiation monitor's detector and the toxic gas analyser's sample probe in the air stream), "top", "top_outlet" (the
    normal outside air's collar up out of the top)."""
    out = _plenum_front()
    if kind == "base":
        out += [(0, 0, 0.2, 16, 1.2, PL, {"*": "pi_steel_dark", "east": None, "west": None}),
                (0, 1.2, 0.2, 16, 1.6, PL - 0.5, {"up": "pi_hv_galv", "east": None, "west": None, "north": None, "south": None, "down": None})]
    elif kind == "outlet":
        out += [(D0, D0, PL, D1, D1, PL + 0.1, {"south": "pi_pnl_black"})] + _flange_ring("z", PL, 16)
    elif kind in ("door_a", "door_b"):
        out += [(2.0, 1.0, PL, 14.0, 15.0, PL + 0.3, {"*": "pi_hv_galv_dark", "south": "pi_hv_plenum_door!"}),
                (12.6, 6.6, PL + 0.3, 13.4, 9.4, PL + 0.8, "pi_pnl_black"), (12.8, 7.4, PL + 0.8, 13.2, 8.6, PL + 1.6, "pi_steel"),
                (2.2, 3.0, PL + 0.3, 2.8, 5.0, PL + 0.6, "pi_steel_dark"), (2.2, 11.0, PL + 0.3, 2.8, 13.0, PL + 0.6, "pi_steel_dark"),
                (3.4, 11.4, PL + 0.3, 11.8, 13.6, PL + 0.45, {"*": "pi_pnl_white", "south": strip(f"plenum_{kind[-1]}")})]
    elif kind == "detector":
        # The detector's lead-shielded housing on a boss through the front, its cable conduit; the probe's tube.
        out += [(4.0, 6.0, PL, 9.0, 11.0, PL + 0.4, "pi_steel_dark")]
        out += PP.round_y(6.5, PL + 2.1, 1.7, 4.0, 13.4, {"*": "pi_hv_detector", "up": "pi_steel_dark", "down": "pi_steel_dark"})
        out += [(6.0, 8.0, PL + 0.4, 7.0, 9.0, PL + 0.6, "pi_steel"), (5.9, 13.4, PL + 1.7, 7.1, 16.0, PL + 2.5, "pi_chrome"),
                (11.0, 9.6, PL, 12.4, 11.0, PL + 0.8, "pi_steel_dark"), (11.4, 11.0, PL + 0.2, 12.0, 16.0, PL + 0.6, "pi_chrome")]
    elif kind == "top":
        out += [(0, 15.6, 0.2, 16, 16, PL - 0.5, {"up": "pi_hv_plenum"})]
    elif kind == "top_outlet":
        top = {"up": "pi_hv_plenum"}
        out += [(0, 15.6, 0.2, F0, 16, PL - 0.5, top), (F1, 15.6, 0.2, 16, 16, PL - 0.5, top),
                (F0, 15.6, 0.2, F1, 16, F0, top), (F0, 15.6, F1, F1, 16, PL - 0.5, top),
                (D0, 15.5, D0, D1, 15.6, D1, {"up": "pi_pnl_black"})] + _flange_ring("y", 15.6, 16)
    return out


def plenum_end(side):
    """The plenum's end panel where the row ends (left: x 0, right: x 16), its corner angle."""
    x0, x1 = (0.0, 0.5) if side == "left" else (15.5, 16.0)
    return [(x0, 0, 0.2, x1, 16, PL, {"*": "pi_hv_plenum", "north": None}),
            ((x0 - 0.3, 0, PL - 0.6, x0 + 0.2, 16, PL + 0.4, "pi_steel_dark") if side == "left" else (x1 - 0.2, 0, PL - 0.6, x1 + 0.3, 16, PL + 0.4, "pi_steel_dark"))]


# ------------------------------------------------------------------------------------------------ chilled water pipe
PR = 1.76                   # the insulated pipe's radius (as tools/hvac_boards.py CHW_R, 0.11 of a block)
JACKET, BAND = "pi_hv_jacket", "pi_hv_jacket_band"


def round_ax(axis, c1, c2, a, t0, t1, tex):
    """A round part along axis ("x", "y" or "z") about (c1, c2) in the other two (x, z for y; x, y for z; z, y for x):
    an octagon (inradius a) as tools/plant_panels.round_y builds it, each box 0.1 shorter at both ends than the one
    before (tools/zfight.py never has to move them)."""
    s = a * math.tan(math.radians(22.5))

    def B(u0, u1, v0, v1, w0, w1, rot=None):
        if axis == "y":
            box = (u0, w0, v0, u1, w1, v1, tex)
        elif axis == "z":
            box = (u0, v0, w0, u1, v1, w1, tex)
        else:
            box = (w0, v0, u0, w1, v1, u1, tex)
        return box + (rot,) if rot else box
    if axis == "y":
        origin = [c1, t0, c2]
    elif axis == "z":
        origin = [c1, c2, t0]
    else:
        origin = [t0, c2, c1]
    rot = {"rotation": {"angle": 45, "axis": axis, "origin": origin}}
    return [B(c1 - a, c1 + a, c2 - s, c2 + s, t0, t1), B(c1 - s, c1 + s, c2 - a, c2 + a, t0 + 0.1, t1 - 0.1),
            B(c1 - a, c1 + a, c2 - s, c2 + s, t0 + 0.2, t1 - 0.2, rot), B(c1 - s, c1 + s, c2 - a, c2 + a, t0 + 0.3, t1 - 0.3, rot)]


def _bands(axis, t_list, a=PR + 0.25):
    """The jacket's bands at the joints (t: 0 at the block's near face, 16 its far one), a hair in from the face so
    none of their ends lie in the pipe's boxes' planes (tools/zfight.py)."""
    out = []
    for t in t_list:
        t0 = 0.05 if t < 8 else 15.15
        out += round_ax(axis, 8, 8, a, t0, t0 + 0.8, BAND)
    return out


OPEN_ENDS = {"z": {"*": JACKET, "north": None, "south": None}, "x": {"*": JACKET, "east": None, "west": None},
             "y": {"*": JACKET, "up": None, "down": None}}


def _seg(axis, t0, t1):
    return round_ax(axis, 8, 8, PR, t0, t1, OPEN_ENDS[axis])


def pipe_straight():
    return _seg("z", 0, 16) + _bands("z", [0, 16])


def pipe_riser():
    return _seg("y", 0, 16) + _bands("y", [0, 16])


def _fitting():
    """An elbow's (or a tee's) fitting cover at the block's middle: the jacket's mitred cover, a box round the turn."""
    a = PR + 0.45
    return [(8 - a, 8 - a, 8 - a, 8 + a, 8 + a, 8 + a, BAND)]


IN = 8 - PR - 0.3


def pipe_elbow():
    return _seg("z", 0, IN) + _seg("x", 16 - IN, 16) + _fitting() + _bands("z", [0]) + _bands("x", [16])


def pipe_elbow_down():
    return _seg("z", 0, IN) + _seg("y", 0, IN) + _fitting() + _bands("z", [0]) + _bands("y", [0])


def pipe_elbow_up():
    return _seg("z", 0, IN) + _seg("y", 16 - IN, 16) + _fitting() + _bands("z", [0]) + _bands("y", [16])


def pipe_tee():
    return _seg("z", 0, IN) + _seg("z", 16 - IN, 16) + _seg("x", 16 - IN, 16) + _fitting() + _bands("z", [0, 16]) + _bands("x", [16])


def pipe_clevis(top=16.0):
    """A clevis hanger round the pipe (its insulation shield under it), the rod up to the roof (top: model y)."""
    y0 = 8 - PR
    return [(6.0, y0 - 0.9, 6.6, 10.0, y0 - 0.4, 9.4, "pi_steel_dark"), (5.6, y0 - 0.4, 6.4, 10.4, y0 - 0.1, 9.6, "pi_steel"),
            (5.7, y0 - 0.9, 7.6, 6.1, 8 + PR + 0.9, 8.4, "pi_steel_dark"), (9.9, y0 - 0.9, 7.6, 10.3, 8 + PR + 0.9, 8.4, "pi_steel_dark"),
            (5.7, 8 + PR + 0.9, 7.6, 10.3, 8 + PR + 1.3, 8.4, "pi_steel_dark"),
            (7.7, 8 + PR + 1.3, 7.7, 8.3, top - 0.4, 8.3, "pi_steel"), (7.2, top - 0.4, 7.2, 8.8, top, 8.8, "pi_steel_dark")]


def pipe_trapeze(top=32.0):
    """A trapeze under the pipe, its rods up either side (clear of the pipe run over it) to the roof."""
    y0 = 8 - PR - 0.5
    out = [(1.6, y0 - 0.5, 7.2, 14.4, y0, 8.8, "pi_steel_dark"), (6.0, y0, 6.6, 10.0, y0 + 0.3, 9.4, "pi_steel")]
    for x in (2.0, 13.4):
        out += [(x, y0, 7.7, x + 0.6, top - 0.4, 8.3, "pi_steel"), (x - 0.4, top - 0.4, 7.4, x + 1.0, top, 8.6, "pi_steel_dark")]
    return out


def pipe_label(kind):
    """A run with its label band round it, the legend and the flow's arrow on each side."""
    a = PR + 0.3
    out = pipe_straight() + [(8 - a, 8 - a, 3.0, 8 + a, 8 + a, 13.0, {"*": "pi_hv_green", "east": None, "west": None})]
    out += [(8 - a - 0.1, 8 - a, 1.0, 8 - a, 8 + a, 15.0, {"*": "pi_hv_green", "west": strip(f"pipe_{kind}_r"), "east": None}),
            (8 + a, 8 - a, 1.0, 8 + a + 0.1, 8 + a, 15.0, {"*": "pi_hv_green", "east": strip(f"pipe_{kind}_l"), "west": None})]
    return out


# ---- each chilled water loop's expansion tank (its bladder tank on legs beside the pump, its line up into the return
# main's tap), its chemical (bypass) feed pot across the pump, the refrigerant sensors at the chillers, the spare
# filters on their racks (Justin: "fill the south bay").
SMALL = 0.6                 # a 1 inch line's radius
STEEL = "pi_steel"


def _small_y(y0, y1, x=8.0, z=8.0, tex=STEEL):
    return [(x - SMALL, y0, z - SMALL, x + SMALL, y1, z + SMALL, tex)]


def expansion_tank(part):
    """The bladder tank: "lower" (its legs, base ring and the shell), "upper" (the shell's top, its dished head, the
    air charging valve, the data plate, the system line up out of its top through its ball valve and gauge tee)."""
    body = {"*": "pi_hv_tank", "up": None, "down": None}
    if part == "lower":
        out = []
        for x, z in ((4.0, 4.0), (11.2, 4.0), (4.0, 11.2), (11.2, 11.2)):
            out.append((x, 0, z, x + 0.8, 3.4, z + 0.8, "pi_steel_dark"))
        out += PP.round_y(8, 8, 5.4, 3.0, 3.7, "pi_steel_dark")
        out += PP.round_y(8, 8, 5.0, 3.7, 16, body)
        out.append((5.0, 9.0, 12.85, 11.0, 12.0, 12.95, {"*": "pi_pnl_white", "south": "pi_hv_tank_plate!", "north": None}))
        return out
    out = PP.round_y(8, 8, 5.0, 0, 7.6, body)
    out += PP.round_y(8, 8, 4.2, 7.6, 9.0, "pi_hv_tank") + PP.round_y(8, 8, 2.6, 9.0, 10.0, "pi_hv_tank")
    out += [(12.6, 5.0, 7.6, 13.6, 6.0, 8.4, "pi_brass"), (13.6, 5.2, 7.8, 14.2, 5.8, 8.2, "pi_cap_black")]
    out += _small_y(10.0, 16.0)
    out += [(7.0, 11.6, 7.0, 9.0, 12.8, 9.0, "pi_brass"), (8.0, 12.0, 7.9, 11.0, 12.4, 8.1, "pi_pnl_red"),
            (8.4, 14.0, 7.4, 9.6, 14.6, 8.6, STEEL), (9.6, 13.7, 7.6, 10.2, 14.9, 8.4, "pi_pnl_black")]
    return out


def small_pipe():
    """A 1 inch line up the block, its coupling."""
    return _small_y(0, 16) + [(8 - 0.9, 7.4, 8 - 0.9, 8 + 0.9, 8.6, 8 + 0.9, "pi_steel_dark")]


def pipe_tap():
    """The insulated main with a weldolet and a 1 inch line down out of its underside (the expansion tank's)."""
    return pipe_straight() + PP.round_y(8, 8, 1.1, 8 - PR - 0.9, 8 - PR + 0.4, "pi_steel_dark") + _small_y(0, 8 - PR - 0.9)


def chem_feeder():
    """A bypass chemical feeder beside its pump: the pot on its legs, its fill cap, the inlet and outlet needle
    valves; its lines west into the pump (from its discharge, back into its volute's side)."""
    out = []
    for x, z in ((5.6, 5.6), (9.6, 5.6), (5.6, 9.6), (9.6, 9.6)):
        out.append((x, 0, z, x + 0.8, 3.2, z + 0.8, "pi_steel_dark"))
    out += PP.round_y(8, 8, 3.0, 3.0, 12.0, {"*": "pi_hv_feeder", "up": "pi_steel_dark", "down": "pi_steel_dark"})
    out += PP.round_y(8, 8, 1.6, 12.0, 13.0, "pi_steel_dark") + PP.round_y(8, 8, 2.0, 13.0, 13.8, "pi_cap_black")
    # The lines out of its side, west: the upper (in, from the pump's discharge above its check valve), the lower (out,
    # back into the volute's vent), each through its needle valve.
    for y, x_end in ((14.7, -7.0), (8.8, -2.5)):
        out.append((x_end, y - SMALL, 8 - SMALL, 5.0, y + SMALL, 8 + SMALL, STEEL))
        out.append((2.4, y - 1.0, 7.0, 3.8, y + 1.0, 9.0, "pi_brass"))
        out.append((2.9, y + 1.0, 7.8, 3.3, y + 2.2, 8.2, "pi_pnl_red"))
    out.append((5.0, 14.1, 7.4, 6.2, 15.3, 8.6, STEEL))
    out += _small_y(12.0, 15.3, 5.6)
    return out


def refrig_sensor():
    """A refrigerant sensor at the floor beside a chiller (the gas is heavier than air): its base plate, post and
    housing, its vents, its conduit."""
    out = [(5.0, 0, 5.0, 11.0, 0.3, 11.0, "pi_steel_dark")] + PP.round_y(8, 8, 0.6, 0.3, 4.0, STEEL)
    out += [(5.0, 4.0, 5.6, 11.0, 9.0, 10.4, {"*": "pi_pnl_white", "south": "pi_hv_sensor!", "north": "pi_hv_sensor!"}),
            (7.4, 9.0, 7.4, 8.6, 16.0, 8.6, "pi_chrome")]
    return out


FILTERS = [("hepa", "HEPA FILTER", "24 X 24 X 12"), ("char", "CHARCOAL", "ADSORBER TRAY"), ("pre", "PREFILTER", "24 X 24 X 2")]


def filter_rack():
    """A steel shelving unit (two shelves in the block: stacked, its posts run on), against the wall behind it
    (z 0)."""
    out = []
    for x in (0.5, 14.7):
        for z in (1.0, 13.0):
            out.append((x, 0, z, x + 0.8, 16, z + 0.8, {"*": "pi_rail_blue", "up": None, "down": None}))
    for y in (0.6, 8.4):
        out += [(1.3, y, 1.0, 14.7, y + 0.4, 13.8, "pi_rail_orange"), (1.3, y + 0.4, 1.4, 14.7, y + 0.5, 13.4, "pi_hv_galv_dark")]
    out += [(1.3, 4.0, 1.2, 14.7, 4.6, 1.6, "pi_rail_blue")]
    return out


def filter_rack_variant(v):
    """What is on a rack's two shelves: v 0 HEPA cells, 1 charcoal trays, 2 prefilter cartons, 3 a mix."""
    kinds = [("hepa", "hepa"), ("char", "char"), ("pre", "pre"), ("hepa", "pre")][v]
    out = []
    for (y, k) in zip((0.9, 8.7), kinds):
        tex = {"*": f"pi_hv_carton_{k}", "south": f"pi_hv_carton_{k}_f!"}
        if k == "hepa":
            out += [(1.6, y, 2.0, 7.6, y + 6.6, 12.8, tex), (8.2, y, 2.0, 14.2, y + 6.6, 12.8, tex)]
        elif k == "char":
            for j in range(3):
                out.append((1.8, y + j * 2.2, 2.2, 14.0, y + j * 2.2 + 2.0, 12.6, tex))
        else:
            for j in range(4):
                out.append((1.6 + j * 3.2, y, 2.4, 1.6 + j * 3.2 + 2.9, y + 6.4, 12.6, tex))
    return out


# ------------------------------------------------------------------------------------------------ the pieces
PIECES = {
    "hv_duct": {"display": "HVAC Duct", "boxes": duct_straight(), "conn": "ns"},
    "hv_duct_hanger": {"display": "HVAC Duct (Hanger)", "boxes": duct_hanger(), "conn": "ns"},
    "hv_duct_riser": {"display": "HVAC Duct Riser", "boxes": duct_riser(), "conn": "ud"},
    "hv_duct_elbow": {"display": "HVAC Duct Elbow", "boxes": duct_elbow(), "conn": "ne"},
    "hv_duct_elbow_down": {"display": "HVAC Duct Elbow (Down)", "boxes": duct_elbow_down(), "conn": "nd"},
    "hv_duct_elbow_up": {"display": "HVAC Duct Elbow (Up)", "boxes": duct_elbow_up(), "conn": "nu"},
    "hv_duct_tee": {"display": "HVAC Duct Tee", "boxes": duct_tee(), "conn": "nse"},
    "hv_duct_tee_down": {"display": "HVAC Duct Tee (Down)", "boxes": duct_tee_down(), "conn": "nsd"},
    "hv_duct_sleeve": {"display": "HVAC Duct Floor Sleeve", "boxes": sleeve(), "conn": "ud"},
    "hv_duct_riser_fd": {"display": "HVAC Duct Riser (Fire Damper)", "boxes": riser_fire_damper(), "conn": "ud"},
    "hv_supply_diffuser": {"display": "Ceiling Supply Diffuser", "boxes": ceiling_vent("pi_hv_diffuser"), "conn": "u"},
    "hv_return_grille": {"display": "Ceiling Return Grille", "boxes": ceiling_vent("pi_hv_grille"), "conn": "u"},
    "hv_chw_pipe": {"display": "Chilled Water Pipe", "boxes": pipe_straight(), "conn": "ns"},
    "hv_chw_pipe_hanger": {"display": "Chilled Water Pipe (Hanger)", "boxes": pipe_straight() + pipe_clevis(), "conn": "ns"},
    "hv_chw_pipe_trapeze": {"display": "Chilled Water Pipe (Trapeze)", "boxes": pipe_straight() + pipe_trapeze(), "conn": "ns"},
    "hv_chw_riser": {"display": "Chilled Water Pipe Riser", "boxes": pipe_riser(), "conn": "ud"},
    "hv_chw_elbow": {"display": "Chilled Water Pipe Elbow", "boxes": pipe_elbow(), "conn": "ne"},
    "hv_chw_elbow_down": {"display": "Chilled Water Pipe Elbow (Down)", "boxes": pipe_elbow_down(), "conn": "nd"},
    "hv_chw_elbow_up": {"display": "Chilled Water Pipe Elbow (Up)", "boxes": pipe_elbow_up(), "conn": "nu"},
    "hv_chw_tee": {"display": "Chilled Water Pipe Tee", "boxes": pipe_tee(), "conn": "nse"},
}
PIECES.update({f"hv_duct_label_{k}": {"display": f"HVAC Duct ({v.title().replace('Cr ', 'Control Room ')} Label)", "boxes": duct_label(k),
                                       "conn": "ns", "flow": "s"} for k, v in DUCT_LABELS.items()})
PIECES.update({f"hv_duct_riser_label_{k}": {"display": f"HVAC Duct Riser ({DUCT_LABELS[k].title().replace('Cr ', 'Control Room ')} Label)",
                                             "boxes": riser_label(k), "conn": "ud"} for k in ("supply", "return")})
PIECES.update({f"hv_chw_label_{k}": {"display": f"Chilled Water Pipe ({v.title()} Label)", "boxes": pipe_label(k), "conn": "ns", "flow": "s"}
               for k, v in PIPE_LABELS.items()})
PLENUM_KINDS = {"base": "Base", "plain": "", "outlet": "Outlet", "door_a": "Access Door A", "door_b": "Access Door B",
                "detector": "Monitor Sample Point", "top": "Top", "top_outlet": "Top Outlet"}
for k, v in PLENUM_KINDS.items():
    PIECES[f"hv_plenum_{k}" if k != "plain" else "hv_plenum"] = {
        "display": "Air Intake Plenum" + (f" ({v})" if v else ""), "boxes": plenum(k), "row": "hv_plenum",
        "left": plenum_end("left"), "right": plenum_end("right")}
PIECES.update({
    "hv_expansion_tank": {"display": "Chilled Water Expansion Tank", "boxes": expansion_tank("lower")},
    "hv_expansion_tank_top": {"display": "Chilled Water Expansion Tank (Top)", "boxes": expansion_tank("upper")},
    "hv_small_pipe": {"display": "Small Pipe", "boxes": small_pipe(), "conn": "ud"},
    "hv_chw_pipe_tap": {"display": "Chilled Water Pipe (Tap)", "boxes": pipe_tap(), "conn": "ns"},
    "hv_chem_feeder": {"display": "Chemical Feed Pot", "boxes": chem_feeder()},
    "hv_refrig_sensor": {"display": "Refrigerant Sensor", "boxes": refrig_sensor()},
    "hv_filter_rack": {"display": "Spare Filter Rack", "boxes": filter_rack(), "variants": 4, "vboxes": [filter_rack_variant(v) for v in range(4)]},
})
PIECES["hv_damper"] = {"display": "HVAC Isolation Damper", "boxes": damper(), "conn": "ns",
                       "variants": 2 * len(DAMPERS), "vboxes": [damper_variant(v) for v in range(2 * len(DAMPERS))]}

CRAFTING = {
    "hv_duct": (["PPP", "P P", "PPP"], {"P": "#c:plates/iron"}, 8),
    "hv_duct_hanger": (["N N", "PPP", "P P"], {"N": "minecraft:iron_nugget", "P": "#c:plates/iron"}, 4),
    "hv_duct_riser": (["P P", "P P", "P P"], {"P": "#c:plates/iron"}, 8),
    "hv_duct_elbow": (["PP ", "P P", " PP"], {"P": "#c:plates/iron"}, 4),
    "hv_duct_elbow_down": (["PPP", "P P", "P  "], {"P": "#c:plates/iron"}, 4),
    "hv_duct_elbow_up": (["P  ", "P P", "PPP"], {"P": "#c:plates/iron"}, 4),
    "hv_duct_tee": (["PPP", "P P", "P P"], {"P": "#c:plates/iron"}, 4),
    "hv_duct_tee_down": (["PPP", " P ", "P P"], {"P": "#c:plates/iron"}, 4),
    "hv_duct_sleeve": (["C C", "CPC", "C C"], {"P": "#c:plates/iron", "C": "minecraft:smooth_stone"}, 2),
    "hv_duct_riser_fd": (["PRP", "P P", "PRP"], {"P": "#c:plates/iron", "R": "minecraft:red_dye"}, 2),
    "hv_supply_diffuser": (["PNP", "N N", "PNP"], {"P": "#c:plates/iron", "N": "minecraft:iron_bars"}, 2),
    "hv_return_grille": (["NNN", "NPN", "NNN"], {"P": "#c:plates/iron", "N": "minecraft:iron_bars"}, 2),
    "hv_chw_pipe": (["WWW", "CCC", "WWW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 8),
    "hv_chw_pipe_hanger": (["N", "W", "C"], {"N": "minecraft:iron_nugget", "W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4),
    "hv_chw_pipe_trapeze": (["N N", "WCW"], {"N": "minecraft:iron_nugget", "W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4),
    "hv_chw_riser": (["WCW", "WCW", "WCW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 8),
    "hv_chw_elbow": (["WC ", " CW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4),
    "hv_chw_elbow_down": (["WCW", " C "], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4),
    "hv_chw_elbow_up": ([" C ", "WCW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4),
    "hv_chw_tee": (["WCW", " C "], {"W": "minecraft:copper_ingot", "C": "minecraft:copper_ingot"}, 4),
    "hv_damper": (["PRP", "PIP", "PPP"], {"P": "#c:plates/iron", "R": "minecraft:redstone", "I": "minecraft:iron_bars"}, 2),
}
CRAFTING["hv_chw_tee"] = (["WCW", " C "], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot"}, 4)
CRAFTING.update({
    "hv_expansion_tank": (["PPP", "PRP", "N N"], {"P": "#c:plates/steel", "R": "minecraft:rabbit_hide", "N": "minecraft:iron_nugget"}, 1),
    "hv_expansion_tank_top": (["NCN", "PPP", "P P"], {"P": "#c:plates/steel", "C": "minecraft:copper_ingot", "N": "minecraft:iron_nugget"}, 1),
    "hv_small_pipe": (["N", "N", "N"], {"N": "minecraft:iron_nugget"}, 8),
    "hv_chw_pipe_tap": (["WNW", "CCC", "WWW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot", "N": "minecraft:iron_nugget"}, 4),
    "hv_chem_feeder": ([" N ", "PBP", "N N"], {"P": "#c:plates/steel", "B": "minecraft:bucket", "N": "minecraft:iron_nugget"}, 1),
    "hv_refrig_sensor": (["PRP", " N "], {"P": "#c:plates/iron", "R": "minecraft:redstone", "N": "minecraft:iron_nugget"}, 2),
    "hv_filter_rack": (["P P", "SSS", "P P"], {"P": "#c:plates/iron", "S": "minecraft:paper"}, 2),
})
for k in DUCT_LABELS:
    CRAFTING[f"hv_duct_label_{k}"] = (["PPP", "PDP", "PPP"], {"P": "#c:plates/iron", "D": "minecraft:paper"}, 8)
for k in ("supply", "return"):
    CRAFTING[f"hv_duct_riser_label_{k}"] = (["P P", "PDP", "P P"], {"P": "#c:plates/iron", "D": "minecraft:paper"}, 8)
for k in PIPE_LABELS:
    CRAFTING[f"hv_chw_label_{k}"] = (["WWW", "CDC", "WWW"], {"W": "minecraft:white_wool", "C": "minecraft:copper_ingot", "D": "minecraft:green_dye"}, 8)
for i, k in enumerate(PLENUM_KINDS):
    name = f"hv_plenum_{k}" if k != "plain" else "hv_plenum"
    CRAFTING[name] = (["PPP", "PXP", "PPP"], {"P": "#c:plates/iron", "X": ["minecraft:iron_bars", "minecraft:smooth_stone_slab", "minecraft:iron_nugget",
                                                                            "minecraft:iron_trapdoor", "minecraft:iron_door", "minecraft:redstone",
                                                                            "minecraft:iron_ingot", "minecraft:hopper"][i]}, 4)
# The hitboxes (pixels, facing north), as nuclear/PlantInterior.java has them.
SHAPES = {"duct": (1.5, 1.5, 0, 14.5, 14.5, 16), "riser": (1.5, 0, 1.5, 14.5, 16, 14.5), "pipe": (6.0, 6.0, 0, 10.0, 10.0, 16),
          "pipe_riser": (6.0, 0, 6.0, 10.0, 16, 10.0), "full": (0, 0, 0, 16, 16, 16), "plenum": (0, 0, 0, 16, 16, 15.8),
          "vent": (0, 0, 0, 16, 16, 16)}


# The south bay's pieces: their own outlines.
SHAPES.update({"hv_expansion_tank": (2.6, 0, 2.6, 13.4, 16, 13.4), "hv_expansion_tank_top": (3.0, 0, 3.0, 13.0, 16, 13.0),
               "hv_small_pipe": (6.6, 0, 6.6, 9.4, 16, 9.4), "hv_chw_pipe_tap": (6.0, 0, 0, 10.0, 10.0, 16),
               "hv_chem_feeder": (5.0, 0, 5.0, 11.0, 16, 11.0), "hv_refrig_sensor": (5.0, 0, 5.0, 11.0, 16, 11.0),
               "hv_filter_rack": (0.5, 0, 1.0, 15.5, 16, 13.8)})


def shape(name):
    if name in SHAPES:
        return SHAPES[name]
    if name.startswith("hv_plenum"):
        return SHAPES["plenum"]
    if name in ("hv_duct_sleeve", "hv_supply_diffuser", "hv_return_grille"):
        return SHAPES["full"]
    if name.startswith("hv_chw"):
        conn = PIECES[name]["conn"]
        return SHAPES["pipe_riser"] if conn == "ud" else SHAPES["pipe"] if conn == "ns" else SHAPES["full"]
    conn = PIECES[name].get("conn", "ns")
    return SHAPES["riser"] if conn == "ud" else SHAPES["duct"] if conn == "ns" else SHAPES["full"]


# ------------------------------------------------------------------------------------------------ textures
K = 512


def _galv(dark=False):
    """Galvanized sheet: its spangle (a mottle of light and dark crystals), the cross-break's faint diagonals."""
    import numpy as np
    rng = np.random.default_rng(611 if dark else 610)
    base = np.array((150, 156, 160) if dark else (186, 192, 196), float)
    img = np.ones((128, 128, 3)) * base
    cells = rng.normal(0, 1, (16, 16))
    img += np.kron(cells, np.ones((8, 8)))[..., None] * 5 + rng.normal(0, 1.5, (128, 128))[..., None]
    out = Image.fromarray(np.clip(img, 0, 255).astype("uint8"), "RGB").convert("RGBA")
    if not dark:
        d = ImageDraw.Draw(out)
        d.line([(0, 0), (127, 127)], fill=(170, 176, 180, 255), width=1)
        d.line([(0, 127), (127, 0)], fill=(170, 176, 180, 255), width=1)
    return out


def _jacket(band=False):
    """The pipe's aluminium jacket: brushed, its lap seam; a band's straps."""
    img = M.brushed((204, 208, 210) if not band else (186, 190, 194), 612 + band, "h", dirt=0.05)
    d = ImageDraw.Draw(img)
    if not band:
        d.line([(0, 96), (127, 96)], fill=(160, 164, 168, 255), width=2)
    return img


def _plenum_paint():
    return M.painted((176, 180, 176), 614, seams=(), chip_count=1, drip_count=0, dirt=0.25)


def _vent(grille):
    """A diffuser's face (four-way louvres in rings round its neck) or a return grille's egg crate."""
    img = Image.new("RGBA", (256, 256), (236, 236, 232, 255))
    d = ImageDraw.Draw(img)
    if grille:
        for k in range(12, 248, 14):
            d.line([(k, 8), (k, 248)], fill=(130, 132, 132, 255), width=3)
            d.line([(8, k), (248, k)], fill=(130, 132, 132, 255), width=3)
        d.rectangle([4, 4, 251, 251], outline=(200, 200, 196, 255), width=6)
    else:
        for k in range(5):
            a = 10 + k * 22
            d.rectangle([a, a, 255 - a, 255 - a], outline=(150, 152, 152, 255), width=4)
            d.rectangle([a + 6, a + 6, 249 - a, 249 - a], outline=(206, 208, 206, 255), width=2)
        d.rectangle([118, 118, 138, 138], fill=(90, 92, 94, 255))
    return img


def _actuator(top=False):
    img = Image.new("RGBA", (128, 128), (64, 66, 70, 255))
    d = ImageDraw.Draw(img)
    if top:
        d.rectangle([12, 30, 116, 98], fill=(238, 238, 232, 255))
        d.text((64, 52), "ACTUATOR", font=PP.rfont(15, True), fill=(30, 30, 32, 255), anchor="mm")
        d.text((64, 76), "120 VAC", font=PP.rfont(13, True), fill=(30, 30, 32, 255), anchor="mm")
    else:
        d.rectangle([0, 0, 127, 127], outline=(40, 40, 44, 255), width=6)
    return img


def _position():
    """The shaft's position plate (seen from its side): SHUT at the top, OPEN at the right (the pointer, by variant,
    points at one), its scale between."""
    import math as _m
    img = Image.new("RGBA", (K, K), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, K - 5, K - 5], outline=(60, 60, 62, 255), width=8)
    c = 256
    for k in range(10):
        a = _m.radians(90 - 10 * k)
        r0, r1 = 150, 176
        d.line([(c + r0 * _m.cos(a), c - r0 * _m.sin(a)), (c + r1 * _m.cos(a), c - r1 * _m.sin(a))], fill=(30, 30, 32, 255), width=7)
    d.text((c, 52), "SHUT", font=PP.rfont(64, True), fill=(180, 30, 26, 255), anchor="mm")
    d.text((430, 322), "OPEN", font=PP.rfont(50, True), fill=(30, 120, 50, 255), anchor="mm")
    d.ellipse([c - 30, c - 30, c + 30, c + 30], fill=(30, 30, 32, 255))
    return img


def _plenum_door():
    img = M.painted((168, 172, 168), 615, seams=(), chip_count=1, drip_count=0, dirt=0.2)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 125, 125], outline=(40, 40, 42, 255), width=3)
    d.rectangle([40, 30, 88, 54], fill=(200, 210, 214, 255), outline=(30, 30, 32, 255), width=2)
    return img


def _detector():
    """The radiation detector's housing: yellow, its band with the trefoil (magenta on yellow) round it."""
    img = Image.new("RGBA", (256, 256), (232, 196, 40, 255))
    d = ImageDraw.Draw(img)
    for x0 in (0, 128):
        cx, cy, r = x0 + 64, 128, 44
        d.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], fill=(150, 30, 120, 255))
        for a in (90, 210, 330):
            d.pieslice([cx - r, cy - r, cx + r, cy + r], -a - 30, -a + 30, fill=(150, 30, 120, 255))
        d.ellipse([cx - 15, cy - 15, cx + 15, cy + 15], outline=(232, 196, 40, 255), width=5)
    d.rectangle([0, 0, 255, 20], fill=(40, 40, 42, 255))
    d.rectangle([0, 235, 255, 255], fill=(40, 40, 42, 255))
    return img


def _tank_plate():
    img = Image.new("RGBA", (K, 256), (236, 236, 228, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, K - 1, 255], outline=(30, 30, 32, 255), width=8)
    PP._fit_line(d, (256, 70), "EXPANSION TANK", 64, 470, (24, 24, 26, 255))
    PP._fit_line(d, (256, 150), "CHILLED WATER  ET-1", 50, 470, (24, 24, 26, 255))
    PP._fit_line(d, (256, 210), "PRECHARGE 12 PSIG  125 PSIG MAX", 36, 470, (24, 24, 26, 255))
    out = Image.new("RGBA", (K, K), (236, 236, 228, 255))
    out.paste(img.resize((K, K)), (0, 0))
    return out


def _sensor_face():
    img = Image.new("RGBA", (256, 256), (232, 232, 226, 255))
    d = ImageDraw.Draw(img)
    for y in range(40, 150, 18):
        d.rectangle([40, y, 216, y + 8], fill=(90, 92, 94, 255))
    d.rectangle([0, 172, 255, 255], fill=(36, 128, 76, 255))
    d.text((128, 213), "R-134A", font=PP.rfont(52, True), fill=(246, 246, 240, 255), anchor="mm")
    d.ellipse([200, 14, 226, 40], fill=(40, 180, 70, 255))
    return img


def _carton(kind, title=None, sub=None):
    """A spare filter's carton (its sides plain, its end lettered)."""
    base = {"hepa": (226, 222, 210), "char": (60, 62, 66), "pre": (196, 160, 112)}[kind]
    img = M.painted(base, 622 + len(kind), seams=(), chip_count=0, drip_count=0, dirt=0.15) if title is None else Image.new("RGBA", (256, 256), base + (255,))
    if title:
        d = ImageDraw.Draw(img)
        ink = (24, 24, 26, 255) if kind != "char" else (240, 240, 234, 255)
        d.rectangle([6, 6, 249, 249], outline=ink, width=6)
        PP._fit_line(d, (128, 100), title, 44, 220, ink)
        PP._fit_line(d, (128, 160), sub, 30, 220, ink)
    return img


def _fire_door():
    img = Image.new("RGBA", (K, K), (196, 40, 34, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 8, K - 9, K - 9], outline=(120, 22, 18, 255), width=12)
    d.text((256, 200), "FIRE", font=PP.rfont(120, True), fill=(250, 246, 240, 255), anchor="mm")
    d.text((256, 330), "DAMPER", font=PP.rfont(110, True), fill=(250, 246, 240, 255), anchor="mm")
    return img


def _strip_text(name):
    """A strip's picture (512 by 128): a duct's or a pipe's label with its arrow, a damper's tag, a plenum's stencil."""
    img = Image.new("RGBA", (K, 128), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    ink = (22, 22, 24, 255)

    def arrow(x0, x1, y, right, fill):
        d.rectangle([x0, y - 9, x1, y + 9], fill=fill)
        tip = (x1 + 34, y) if right else (x0 - 34, y)
        base = x1 if right else x0
        d.polygon([(base, y - 30), tip, (base, y + 30)], fill=fill)

    kind, _, rest = name.partition("_")
    if kind == "duct":
        key, side = rest.rsplit("_", 1)
        d.rectangle([0, 0, K - 1, 127], outline=ink, width=6)
        right = side == "r"
        arrow(*((340, 430) if right else (82, 172)), 64, right, ink)
        PP._fit_line(d, (180 if right else 332, 64), DUCT_LABELS[key], 64, 300, ink)
    elif kind == "riser":
        d.rectangle([0, 0, K - 1, 127], outline=ink, width=6)
        up = rest == "return"
        x = 440
        d.rectangle([x - 9, 22, x + 9, 106], fill=ink)
        d.polygon([(x - 28, 40 if up else 88), (x, 6 if up else 122), (x + 28, 40 if up else 88)], fill=ink)
        PP._fit_line(d, (208, 64), DUCT_LABELS[rest], 64, 380, ink)
    elif kind == "pipe":
        key, side = rest.rsplit("_", 1)
        green, white = (30, 120, 60, 255), (246, 246, 240, 255)
        d.rectangle([0, 0, K - 1, 127], fill=green)
        right = side == "r"
        arrow(*((376, 440) if right else (72, 136)), 64, right, white)
        PP._fit_line(d, (210 if right else 302, 64), PIPE_LABELS[key], 60, 330, white)
    elif kind == "tag":
        tag, title = DAMPERS[int(rest)]
        d.rectangle([0, 0, K - 1, 127], fill=(236, 236, 228, 255), outline=ink, width=5)
        d.rectangle([0, 0, 150, 127], fill=(226, 120, 28, 255) if tag.endswith("A") else (36, 128, 76, 255))
        PP._fit_line(d, (75, 64), tag[-2:], 76, 130, ink if tag.endswith("A") else (246, 246, 240, 255))
        PP._fit_line(d, (332, 40), tag, 46, 330, ink)
        PP._fit_line(d, (332, 92), title, 40, 330, ink)
    elif kind == "plenum":
        d.rectangle([0, 0, K - 1, 127], fill=(30, 30, 32, 255))
        PP._fit_line(d, (256, 42), "CONTROL ROOM", 42, 470, (246, 246, 240, 255))
        PP._fit_line(d, (256, 94), f"AIR INTAKE {rest.upper()}", 48, 470, (240, 200, 40, 255))
    return img


def _strips(sheet):
    img = Image.new("RGBA", (K, K), (240, 240, 234, 255))
    for row in range(4):
        i = sheet * 4 + row
        if i < len(STRIPS):
            img.paste(_strip_text(STRIPS[i]), (0, row * 128))
    return img


SHEETS = (len(STRIPS) + 3) // 4


def makers():
    out = {"pi_hv_galv": _galv, "pi_hv_galv_dark": lambda: _galv(True), "pi_hv_jacket": _jacket, "pi_hv_jacket_band": lambda: _jacket(True),
           "pi_hv_plenum": _plenum_paint, "pi_hv_diffuser": lambda: _vent(False), "pi_hv_grille": lambda: _vent(True),
           "pi_hv_actuator": _actuator, "pi_hv_actuator_top": lambda: _actuator(True), "pi_hv_position": _position,
           "pi_hv_plenum_door": _plenum_door, "pi_hv_detector": _detector, "pi_hv_green": lambda: PP._solid((30, 120, 60), 617, 0.02),
           "pi_hv_fire_door": _fire_door, "pi_hv_tank": lambda: M.painted((178, 40, 36), 618, seams=(), chip_count=2, drip_count=0, dirt=0.2),
           "pi_hv_tank_plate": _tank_plate, "pi_hv_feeder": lambda: M.painted((40, 92, 160), 619, seams=(), chip_count=2, drip_count=0, dirt=0.2),
           "pi_hv_sensor": _sensor_face, "pi_rail_orange": lambda: PP._solid((226, 118, 30), 620, 0.03), "pi_rail_blue": lambda: PP._solid((40, 84, 170), 621, 0.03)}
    for k, title, sub in FILTERS:
        out[f"pi_hv_carton_{k}"] = (lambda k=k: _carton(k))
        out[f"pi_hv_carton_{k}_f"] = (lambda k=k, title=title, sub=sub: _carton(k, title, sub))
    out.update({f"pi_hv_strips_{k}": (lambda k=k: _strips(k)) for k in range(SHEETS)})
    return out


SHARP = {f"pi_hv_strips_{k}" for k in range(SHEETS)} | {"pi_hv_position", "pi_hv_diffuser", "pi_hv_grille", "pi_hv_actuator_top", "pi_hv_fire_door", "pi_hv_detector",
                                                         "pi_hv_tank_plate", "pi_hv_sensor"} | {f"pi_hv_carton_{k}_f" for k, _, _ in FILTERS}


# ------------------------------------------------------------------------------------------------ laying runs
DIRS = {"n": (0, 0, -1), "s": (0, 0, 1), "e": (1, 0, 0), "w": (-1, 0, 0), "u": (0, 1, 0), "d": (0, -1, 0)}
TURN = {"north": 0, "east": 1, "south": 2, "west": 3}
CYCLE = "nesw"


def turned(conn, facing):
    """The faces a piece opens on, facing this way (the piece's own: facing north)."""
    k = TURN[facing]
    return "".join(sorted(CYCLE[(CYCLE.index(c) + k) % 4] if c in CYCLE else c for c in conn))


def facing_for(conn, want, flow=None):
    """The facing that turns a piece opening on conn to open on want (and, with flow, its +z toward flow)."""
    for f in TURN:
        if turned(conn, f) == "".join(sorted(want)) and (flow is None or turned("s", f) == flow):
            return f
    return None


def step_dir(a, b):
    d = tuple(q - p for p, q in zip(a, b))
    for k, v in DIRS.items():
        if v == d:
            return k
    raise ValueError(f"{a} -> {b} not adjacent")


def connections(runs):
    """Where each block of the runs opens: runs are lists of cells in order, with an opening into whatever is at
    either end (start, end: a face letter or None)."""
    conn = {}
    for cells, start, end in runs:
        for i, c in enumerate(cells):
            s = conn.setdefault(c, set())
            if i > 0:
                s.add(step_dir(c, cells[i - 1]))
            if i + 1 < len(cells):
                s.add(step_dir(c, cells[i + 1]))
        if start:
            conn[cells[0]].add(start)
        if end:
            conn[cells[-1]].add(end)
    return conn


DUCT_FAMILY = {"ns": "hv_duct", "ud": "hv_duct_riser", "ne": "hv_duct_elbow", "nd": "hv_duct_elbow_down", "nu": "hv_duct_elbow_up",
               "nse": "hv_duct_tee", "nsd": "hv_duct_tee_down"}
PIPE_FAMILY = {"ns": "hv_chw_pipe", "ud": "hv_chw_riser", "ne": "hv_chw_elbow", "nd": "hv_chw_elbow_down", "nu": "hv_chw_elbow_up",
               "nse": "hv_chw_tee"}


def pieces_for(conn, family, special=None):
    """Each block's piece and facing: special maps a block to (piece, flow or None, variant) for the labelled runs,
    hangers, dampers, sleeves and vents; the rest are the family's plain piece that opens where the block does."""
    special = special or {}
    out = {}
    for cell, faces in conn.items():
        want = "".join(sorted(faces))
        if cell in special:
            name, flow, variant = special[cell]
            base = PIECES[name].get("conn", "ns")
            f = facing_for(base, want, flow) or facing_for(base, want)
            if f is None:
                raise ValueError(f"{name} cannot open {want} at {cell}")
            out[cell] = (name, f, variant)
            continue
        for base, name in family.items():
            f = facing_for(base, want)
            if f:
                out[cell] = (name, f, None)
                break
        else:
            raise ValueError(f"no piece opens {want} at {cell}")
    return out


# ------------------------------------------------------------------------------------------------ the panels
# The room's HVAC control panel (three wide, two high, standing on the floor): the system's mimic for both trains (each
# damper lit red open, green shut; each fan, chiller and pump lit red running, green stopped), the mode selector
# (NORMAL, AUTO, EMERGENCY) and its lamps, the control room's pressure, temperature, chilled water and filtered flow
# meters, its annunciator. Each intake's radiation monitor and toxic gas analyser (one wide, two high, on the wall),
# labelled by train. What moves is the game's (client/EdgTelemetry.java hvac; nuclear/HvacRoom.java).
import relay_room as RR

HP_KEY, RAD_KEY, TOX_KEY = "hv_control_panel", "hv_rad_monitor", "hv_tox_monitor"
HP_D = RR.DOOR
HP_TITLE = (2.0, 29.2, 46.0, 31.1)
HP_MIMIC = (2.0, 16.9, 46.0, 28.7)
HP_ROWS = {"A": 26.2, "B": 20.6}            # each train's main line on the mimic
# The mimic's boxes (x0, x1, legend lines, channel stem): along the main line, then the emergency branch under it.
HP_MAIN = [(2.6, 7.6, ["OA INTAKE", "{t}"], None), (12.4, 15.6, ["CR-D-1{t}", "NORM OA"], "d1"), (26.2, 32.2, ["AHU-1{t}", "SUPPLY FAN"], "ahu"),
           (38.0, 45.4, ["TO CONTROL", "ROOM"], None)]
HP_BRANCH = [(10.4, 13.6, ["CR-D-2{t}", "EMERG"], "d2"), (15.0, 21.0, ["CRF-1{t}", "CREFS FAN"], "efu"), (22.0, 25.0, ["CR-D-3{t}", "OUTLET"], "d3"),
             (27.6, 31.2, ["CH-1{t}", "CHILLER"], "ch"), (32.6, 36.4, ["CHP-1{t}", "CHW PUMP"], "pump")]
HP_SEL = (6.6, 10.4)
HP_LAMPS = [(3.4, "NORMAL", RR.GREEN, "hv_l_normal", "pi_lens_green"), (6.6, "EMERG", RR.RED, "hv_l_emerg", "pi_lens_red"),
            (9.8, "PRESS OK", (240, 240, 230), "hv_l_press", "pi_lens_white")]
HP_LAMP_Y = 4.4
HP_METERS = [(13.2, 8.4, 18.8, 14.0, "CR DIFF PRESS", "IN WC", ("0", "0.125", "0.25"), "hv_m_dp"),
             (19.6, 8.4, 25.2, 14.0, "CR TEMP", "DEG F", ("60", "75", "90"), "hv_m_temp"),
             (26.0, 8.4, 31.6, 14.0, "CHW SUPPLY", "DEG F", ("30", "50", "70"), "hv_m_chw"),
             (32.4, 8.4, 38.0, 14.0, "CREFS FLOW", "CFM", ("0", "1500", "3000"), "hv_m_flow")]
HP_ANN = [("REACTOR|TRIP", "RX HIGH|TEMP"), ("INTAKE|HIGH RAD", "TOXIC|GAS"), ("EMERG|MODE", "CREFS|LOW FLOW"), ("CHILLER|TRIP", "CR LOW|DIFF PRESS")]
HP_ANN_X = [(39.0, 42.4), (42.6, 46.0)]
HP_ANN_Y = 14.6                     # the annunciator's top: rows of 1.9 down from it
HP_BUTTONS = [(40.4, "ACK"), (43.0, "RESET"), (45.4, "TEST")]


def _mimic_box(f, x0, x1, yc, lines, ch, z, t):
    y0, y1 = yc - 0.95, yc + 0.95
    f.R(x0, y0, x1, y1, fill=(236, 236, 228, 255), outline=(30, 30, 32, 255), width=3)
    for i, w in enumerate(lines):
        RR._fit(f, (x0 + x1) / 2, yc + 0.32 - i * 0.66, w.format(t=t), 7, x1 - x0 - 0.3)
    if ch:
        # Lit red running (open), green stopped (shut), over the legend (it shows through).
        f.live.append({"t": "window", "x0": x0 + 0.08, "y0": y0 + 0.08, "x1": x1 - 0.08, "y1": y1 - 0.08, "z": z, "colour": [230, 50, 40],
                       "colour2": [50, 190, 80], "ch": f"hv_{ch}_{t.lower()}", "alpha": 0.5})


def hv_control_panel():
    f = PP.Front(3, 660)
    D = HP_D
    f.R(1.0, 1.2, 47.0, 31.4, outline=(130, 132, 128, 255), width=3)
    x0, y0, x1, y1 = HP_TITLE
    f.R(x0, y0, x1, y1, fill=(30, 70, 140, 255), outline=(20, 40, 90, 255), width=3)
    RR._fit(f, 24.0, y1 - 0.6, "CONTROL ROOM HVAC  -  LOCAL CONTROL PANEL  CR-HVAC-1", 10, 42.0, fill=(246, 246, 240, 255))
    RR._fit(f, 24.0, y0 + 0.5, "CONTROL ROOM EMERGENCY VENTILATION AND AIR CONDITIONING", 7, 42.0, fill=(246, 246, 240, 255))
    mx0, my0, mx1, my1 = HP_MIMIC
    f.R(mx0, my0, mx1, my1, fill=(206, 208, 202, 255), outline=(90, 92, 90, 255), width=2)
    ink = (24, 24, 26, 255)
    for t, yc in HP_ROWS.items():
        yb = yc - 2.6
        RR._fit(f, mx0 + 1.2, yc + 1.6, f"TRAIN {t}", 7, 5.0, fill=(150, 70, 10, 255) if t == "A" else (20, 100, 50, 255))
        # The ducts: the intake, its normal damper, the air handler, on to the control room; the branch down through
        # the emergency damper, the filtration unit, its outlet damper, up into the air handler's inlet; chilled water.
        f.line([(7.6, yc), (12.4, yc)], ink, 6)
        f.line([(15.6, yc), (26.2, yc)], ink, 6)
        f.line([(32.2, yc), (38.0, yc)], ink, 6)
        f.line([(9.0, yc), (9.0, yb), (10.4, yb)], ink, 6)
        f.line([(13.6, yb), (15.0, yb)], ink, 6)
        f.line([(21.0, yb), (22.0, yb)], ink, 6)
        f.line([(25.0, yb), (25.4, yb), (25.4, yc)], ink, 6)
        f.line([(29.4, yb + 0.95), (29.4, yc - 0.95)], (40, 110, 200, 255), 5)
        f.line([(31.2, yb), (32.6, yb)], (40, 110, 200, 255), 5)
        f.line([(36.6, yc), (37.4, yc - 0.4), (36.6, yc - 0.8)], ink, 4)
        for x0_, x1_, lines, ch in HP_MAIN:
            _mimic_box(f, x0_, x1_, yc, lines, ch, D + 0.05, t)
        for x0_, x1_, lines, ch in HP_BRANCH:
            _mimic_box(f, x0_, x1_, yb, lines, ch, D + 0.05, t)
    RR._fit(f, 41.6, my0 + 1.35, "LIT RED:  RUNNING / OPEN", 7, 8.4)
    RR._fit(f, 41.6, my0 + 0.6, "LIT GREEN:  STOPPED / SHUT", 7, 8.4)
    # The mode selector: its plate, the three positions, the pointer (live: one bar of three).
    sx, sy = HP_SEL
    f.R(sx - 4.2, 7.0, sx + 4.2, 15.6, fill=(214, 216, 210, 255), outline=(90, 92, 90, 255), width=2)
    RR._fit(f, sx, 15.0, "CR HVAC MODE", 8, 7.8)
    f.E(sx, sy, 1.25, fill=(46, 46, 48, 255))
    for name, (dx, dy) in (("NORMAL", (-1.55, 1.15)), ("AUTO", (0.0, 1.95)), ("EMERG", (1.55, 1.15))):
        f.T(sx + dx, sy + dy, name, 8, bold=True, anchor="rm" if dx < 0 else "lm" if dx > 0 else "mm")
    RR._fit(f, sx, 7.75, "AUTO: EMERG ON RX TRIP OR HI TEMP", 6, 7.8)
    for k, (bx0, by0, bx1, by1) in enumerate(((sx - 0.75, sy + 0.05, sx - 0.05, sy + 0.75), (sx - 0.12, sy, sx + 0.12, sy + 1.0),
                                              (sx + 0.05, sy + 0.05, sx + 0.75, sy + 0.75))):
        f.live.append({"t": "toggle", "ch": f"hv_sel{k}", "colour": [240, 240, 234], "on": [bx0, by0, D + 0.75, bx1, by1, D + 0.95], "off": []})
    for x, label, colour, ch, _ in HP_LAMPS:
        RR._live_lamp(f, x, HP_LAMP_Y, 0.55, colour, ch)
        RR._fit(f, x, HP_LAMP_Y - 1.35, label, 7, 3.0)
    for x0_, y0_, x1_, y1_, title, unit, ticks, ch in HP_METERS:
        RR._quarter_meter(f, x0_, y0_, x1_, y1_, None, unit, [(ticks[0], 0.0), (ticks[1], 0.5), (ticks[2], 1.0)], ch, D + 0.2)
        RR._fit(f, (x0_ + x1_) / 2, y1_ + 0.55, title, 7, x1_ - x0_)
    f.R(38.8, 6.8, 46.2, HP_ANN_Y + 0.2, fill=(30, 30, 32, 255))
    RR._fit(f, 42.5, HP_ANN_Y + 0.75, "ANNUNCIATOR", 7, 7.0)
    for r, row in enumerate(HP_ANN):
        for c, text in enumerate(row):
            wx0, wx1 = HP_ANN_X[c]
            wy1 = HP_ANN_Y - r * 1.9
            wy0 = wy1 - 1.7
            f.R(wx0, wy0, wx1, wy1, fill=(232, 232, 222, 255))
            lines = text.split("|")
            for i, w in enumerate(lines):
                RR._fit(f, (wx0 + wx1) / 2, (wy0 + wy1) / 2 + 0.3 - i * 0.6, w, 7, wx1 - wx0 - 0.2)
            f.live.append({"t": "window", "x0": wx0, "y0": wy0, "x1": wx1, "y1": wy1, "z": D + 0.35, "colour": [255, 190, 70],
                           "ch": f"hv_a{r * 2 + c}", "alpha": 0.55})
    for x, name in HP_BUTTONS:
        f.E(x, 4.4, 0.6, fill=(40, 40, 42, 255))
        RR._fit(f, x, 3.05, name, 7, 2.4)
    f.T(24.0, 2.2, "PANEL CR-HVAC-1    480 V MCC 1A-7 / 1B-7    DWG M-1101", 6, bold=True)
    return f


def _hp_shell(W):
    return [(1.0, 1.2, 2.0, 1.6, 31.4, 14.6, "pi_cabinet"), (W - 1.6, 1.2, 2.0, W - 1.0, 31.4, 14.6, "pi_cabinet"),
            (1.6, 1.2, 2.0, W - 1.6, 31.4, 2.6, "pi_cabinet"),
            (0.8, 0, 1.8, W - 0.8, 1.2, 15.2, "pi_cabinet_dark"), (0.8, 31.4, 1.8, W - 0.8, 32, 15.4, "pi_cabinet_dark")]


def hv_control_panel_boxes():
    F = f"pi_{HP_KEY}_f"
    D = HP_D
    face = {"*": "pi_pnl_black", "south": F}
    b = _hp_shell(48) + [(1.0, 1.2, 14.6, 47.0, 31.4, D, {"*": "pi_cabinet", "south": F})]
    x0, y0, x1, y1 = HP_TITLE
    b.append((x0, y0, D, x1, y1, D + 0.12, face))
    for x0_, y0_, x1_, y1_, *_ in HP_METERS:
        b += RR._meter_case(x0_, y0_, x1_, y1_, face)
    for x, _, _, _, lens in HP_LAMPS:
        b += RR._lamp(x, HP_LAMP_Y, lens, r=0.55)
    # The selector: its knob's skirt and the knob (the pointer over it, live).
    sx, sy = HP_SEL
    b += PP._sc_round(sx, sy, D, D + 0.3, 1.25, "pi_cap_black") + [(sx - 0.25, sy - 1.0, D + 0.3, sx + 0.25, sy + 1.0, D + 0.7, "pi_cap_black")]
    # The annunciator's frame standing out round its windows (they are set back in it, the lit glass the game's).
    b += [(38.8, 6.8, D, 39.0, HP_ANN_Y + 0.2, D + 0.45, "pi_pnl_black"), (46.0, 6.8, D, 46.2, HP_ANN_Y + 0.2, D + 0.45, "pi_pnl_black"),
          (39.0, 6.8, D, 46.0, 7.0, D + 0.45, "pi_pnl_black"), (39.0, HP_ANN_Y, D, 46.0, HP_ANN_Y + 0.2, D + 0.45, "pi_pnl_black"),
          (42.4, 7.0, D, 42.6, HP_ANN_Y, D + 0.45, "pi_pnl_black")]
    for r in range(1, len(HP_ANN)):
        y = HP_ANN_Y - r * 1.9
        b.append((39.0, y, D, 46.0, y + 0.2, D + 0.45, "pi_pnl_black"))
    for x, _ in HP_BUTTONS:
        b += PP._sc_round(x, 4.4, D, D + 0.25, 0.75, "pi_chrome") + PP._sc_round(x, 4.4, D + 0.25, D + 0.6, 0.5, "pi_cap_black")
    # The mimic's glass-free panel, a hair proud; its train labels' frame.
    b.append((HP_MIMIC[0], HP_MIMIC[1], D, HP_MIMIC[2], HP_MIMIC[3], D + 0.02, face))
    # The conduit hubs on its top (its control cables up to the tray).
    b += PP._top_hub(8.0, 32.0 - 0.6) + PP._top_hub(40.0, 32.0 - 0.6)
    return b


# ---- the monitors: on the wall (the piece facing north: the wall behind at z 0), its enclosure over both blocks.
MON_BOX = (2.4, 3.0, 13.6, 29.0)
MON_D = 3.6
RAD_METER = (3.4, 16.6, 12.6, 24.2)
RAD_DISPLAY = (4.0, 12.6, 12.0, 14.8)
MON_LAMPS = {RAD_KEY: [(4.6, "NORMAL", RR.GREEN, "hv_rad_ok", "pi_lens_green"), (8.0, "ALERT", RR.AMBER, "hv_rad_alert", "pi_lens_amber"),
                       (11.4, "HIGH", RR.RED, "hv_rad_high", "pi_lens_red")],
             TOX_KEY: [(4.6, "NORMAL", RR.GREEN, "hv_tox_ok", "pi_lens_green"), (8.0, "FAULT", RR.AMBER, "hv_tox_fault", "pi_lens_amber"),
                       (11.4, "ALARM", RR.RED, "hv_tox_alarm", "pi_lens_red")]}
MON_LAMP_Y = 9.0
TOX_DISPLAYS = [(4.0, 19.4, 12.0, 21.8, "CHLORINE  PPM", "hv_cl2"), (4.0, 13.4, 12.0, 15.8, "AMMONIA  PPM", "hv_nh3")]


def _mon_band(f, title, sub, colours):
    x0, y0, x1, y1 = MON_BOX
    bg, fg = colours
    f.R(x0, y1 - 3.0, x1, y1, fill=bg, outline=(40, 40, 42, 255), width=2)
    RR._fit(f, (x0 + x1) / 2, y1 - 0.95, title, 9, x1 - x0 - 0.6, fill=fg)
    RR._fit(f, (x0 + x1) / 2, y1 - 2.15, sub, 7, x1 - x0 - 0.6, fill=fg)


def _digital(f, x0, y0, x1, y1, ch, D):
    f.R(x0, y0, x1, y1, fill=(30, 30, 32, 255))
    f.R(x0 + 0.25, y0 + 0.25, x1 - 0.25, y1 - 0.25, fill=(28, 6, 6, 255))
    f.text(x1 - 0.5, (y0 + y1) / 2, D + 0.2, 1.2, (255, 70, 50), ch, "right")


def hv_rad_monitor(label="A"):
    f = PP.Front(1, 670 + "AB".index(label))
    D = MON_D
    x0, y0, x1, y1 = MON_BOX
    f.R(x0, y0, x1, y1, outline=(90, 92, 90, 255), width=3)
    _mon_band(f, "RADIATION MONITOR", f"CR INTAKE {label}  RM-CR-1{label}", ((236, 196, 36, 255), (24, 24, 26, 255)))
    mx0, my0, mx1, my1 = RAD_METER
    RR._quarter_meter(f, mx0, my0, mx1, my1, None, "CPM", [("10", 0.0), ("1E3", 0.4), ("1E5", 0.8), ("1E6", 1.0)], "hv_rad_m", D + 0.2)
    dx0, dy0, dx1, dy1 = RAD_DISPLAY
    _digital(f, dx0, dy0, dx1, dy1, "hv_rad_cpm", D)
    RR._fit(f, (dx0 + dx1) / 2, dy0 - 0.5, "COUNT RATE  CPM", 7, 8.0)
    for x, name, colour, ch, _ in MON_LAMPS[RAD_KEY]:
        RR._live_lamp(f, x, MON_LAMP_Y, 0.5, colour, ch, D)
        RR._fit(f, x, MON_LAMP_Y - 1.3, name, 7, 3.2)
    RR._fit(f, 8.0, 5.8, "ALERT 1E3   HIGH 1E4 CPM", 6, 10.6)
    RR._fit(f, 8.0, 4.8, "HIGH: CR HVAC TO EMERGENCY", 6, 10.6)
    return f


def hv_tox_monitor(label="A"):
    f = PP.Front(1, 680 + "AB".index(label))
    D = MON_D
    x0, y0, x1, y1 = MON_BOX
    f.R(x0, y0, x1, y1, outline=(90, 92, 90, 255), width=3)
    _mon_band(f, "TOXIC GAS ANALYZER", f"CR INTAKE {label}  AT-CR-2{label}", ((30, 70, 140, 255), (246, 246, 240, 255)))
    for dx0, dy0, dx1, dy1, title, ch in TOX_DISPLAYS:
        _digital(f, dx0, dy0, dx1, dy1, ch, D)
        RR._fit(f, (dx0 + dx1) / 2, dy1 + 0.55, title, 7, 8.0)
    for x, name, colour, ch, _ in MON_LAMPS[TOX_KEY]:
        RR._live_lamp(f, x, MON_LAMP_Y, 0.5, colour, ch, D)
        RR._fit(f, x, MON_LAMP_Y - 1.3, name, 7, 3.2)
    RR._fit(f, 8.0, 5.8, "ALARM CL2 5   NH3 25 PPM", 6, 10.6)
    RR._fit(f, 8.0, 4.8, "SAMPLE FROM INTAKE PLENUM", 6, 10.6)
    return f


def _monitor_boxes(key):
    F = f"pi_{key}_f"
    D = MON_D
    x0, y0, x1, y1 = MON_BOX
    face = {"*": "pi_pnl_black", "south": F}
    b = [(x0, y0, 0.2, x1, y1, D, {"*": "pi_cabinet", "south": F}), (x0 + 0.4, y1, 0.4, x1 - 0.4, y1 + 0.3, D - 0.2, "pi_cabinet_dark")]
    if key == RAD_KEY:
        b += RR._meter_case(*RAD_METER, face, D=D)
        displays = [RAD_DISPLAY]
    else:
        displays = [d[:4] for d in (RF_DISPLAYS if key == RF_KEY else TOX_DISPLAYS)]
    for dx0, dy0, dx1, dy1 in displays:
        b += [(dx0, dy0, D, dx0 + 0.25, dy1, D + 0.3, "pi_pnl_black"), (dx1 - 0.25, dy0, D, dx1, dy1, D + 0.3, "pi_pnl_black"),
              (dx0 + 0.25, dy0, D, dx1 - 0.25, dy0 + 0.25, D + 0.3, "pi_pnl_black"), (dx0 + 0.25, dy1 - 0.25, D, dx1 - 0.25, dy1, D + 0.3, "pi_pnl_black")]
    for x, _, _, _, lens in MON_LAMPS[key]:
        b += RR._lamp(x, MON_LAMP_Y, lens, D=D, r=0.5)
    for x, y in ((x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x0 + 0.5, y1 - 3.5), (x1 - 0.5, y1 - 3.5)):
        b.append((x - 0.2, y - 0.2, D, x + 0.2, y + 0.2, D + 0.12, "pi_steel"))
    # Its conduit (the detector's cable, or the analyser's sample line) up out of its top.
    b += [(7.3, y1 + 0.3, 1.6, 8.7, 32, 3.0, "pi_chrome"), (7.0, y1 + 0.3, 1.3, 9.0, y1 + 1.0, 3.3, "pi_steel_dark")]
    return b


# ---- the refrigerant monitor on the wall by the chillers (ASHRAE 15): each chiller's sensor's reading, its alarm.
RF_KEY = "hv_refrig_monitor"
RF_DISPLAYS = [(4.0, 19.4, 12.0, 21.8, "CH-1A  PPM", "hv_rf_a"), (4.0, 13.4, 12.0, 15.8, "CH-1B  PPM", "hv_rf_b")]
MON_LAMPS[RF_KEY] = [(4.6, "NORMAL", RR.GREEN, "hv_rf_ok", "pi_lens_green"), (8.0, "FAULT", RR.AMBER, "hv_rf_fault", "pi_lens_amber"),
                     (11.4, "ALARM", RR.RED, "hv_rf_alarm", "pi_lens_red")]


def hv_refrig_monitor():
    f = PP.Front(1, 690)
    D = MON_D
    x0, y0, x1, y1 = MON_BOX
    f.R(x0, y0, x1, y1, outline=(90, 92, 90, 255), width=3)
    _mon_band(f, "REFRIGERANT MONITOR", "R-134A  CHILLER AREA  RM-CR-3", ((36, 128, 76, 255), (246, 246, 240, 255)))
    for dx0, dy0, dx1, dy1, title, ch in RF_DISPLAYS:
        _digital(f, dx0, dy0, dx1, dy1, ch, D)
        RR._fit(f, (dx0 + dx1) / 2, dy1 + 0.55, title, 7, 8.0)
    for x, name, colour, ch, _ in MON_LAMPS[RF_KEY]:
        RR._live_lamp(f, x, MON_LAMP_Y, 0.5, colour, ch, D)
        RR._fit(f, x, MON_LAMP_Y - 1.3, name, 7, 3.2)
    RR._fit(f, 8.0, 5.8, "ALARM 300 PPM", 6, 10.6)
    RR._fit(f, 8.0, 4.8, "ON ALARM LEAVE THE ROOM", 6, 10.6)
    return f


PANELS = {HP_KEY: {"display": "Control Room HVAC Control Panel", "width": 3, "item3d": True},
          RAD_KEY: {"display": "Air Intake Radiation Monitor", "width": 1, "item3d": True, "labels": ["A", "B"]},
          TOX_KEY: {"display": "Air Intake Toxic Gas Analyzer", "width": 1, "item3d": True, "labels": ["A", "B"]},
          RF_KEY: {"display": "Refrigerant Monitor", "width": 1, "item3d": True}}
DRAWINGS = {HP_KEY: hv_control_panel, RAD_KEY: hv_rad_monitor, TOX_KEY: hv_tox_monitor, RF_KEY: hv_refrig_monitor}
BOXES = {HP_KEY: hv_control_panel_boxes, RAD_KEY: lambda: _monitor_boxes(RAD_KEY), TOX_KEY: lambda: _monitor_boxes(TOX_KEY),
         RF_KEY: lambda: _monitor_boxes(RF_KEY)}
CRAFTING.update({
    HP_KEY: (["LGL", "CXC", "PRP"], {"L": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                     "X": "minecraft:comparator", "P": "#c:plates/steel", "R": "minecraft:redstone"}, 1),
    RAD_KEY: (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                      "X": "jugcraft:geiger_counter", "R": "minecraft:redstone"}, 1),
    TOX_KEY: (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                      "X": "minecraft:glass_bottle", "R": "minecraft:redstone"}, 1),
    RF_KEY: (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                     "X": "minecraft:snowball", "R": "minecraft:redstone"}, 1),
})


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], 2, "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update({k: {kk: vv for kk, vv in v.items() if kk not in ("conn", "flow")} for k, v in PIECES.items()})
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(PP.variants(key)) for p in range(2 * PANELS[key]["width"])}


register()
