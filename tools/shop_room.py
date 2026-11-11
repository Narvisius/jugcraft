"""The turbine building's maintenance workshop (turbine hall part 3): a plant's mechanical shop under the operating floor,
fitted out as the plant's other rooms are (tools/switchgear_room.py: every piece one block, its parts real boxes at 16
px a block, models for the piece facing north: x across, y up, z from its back (north, z 0) to its front (south, +z);
a piece may stand up out of its block, up to 32 px, as a tall machine does).

- shop_bench (variants 0 its left end with the machinist's vise, 1 a middle with the toolbox, 2 its right end with the
  bench grinder): a steel bench with a laminated maple top, its drawer unit and lower shelf; shop_pegboard (variants 0
  to 2) on the wall over it: the perforated board, the hand tools hung on it, the shelf over them;
- shop_drill_press: a floor drill press (base, column, table, head, belt guard, motor, feed handles, work light);
- shop_lathe (variants 0 the headstock end, 1 the carriage, 2 the tailstock end): an engine lathe three blocks long,
  its bed, chip pan and splash guard, the headstock with its chuck and guard, the carriage and its apron, the tailstock,
  a bar of work between centres, the lead screw and feed rod;
- shop_tool_chest: a rolling tool cabinet with its top chest;
- shop_press: a 20-ton hydraulic H-frame press;
- shop_parts_washer: the solvent tank on its stand, its lid propped open, the sink, the brush and its hose;
- shop_weld_screen: a welding screen (its yellow tube frame on casters, the amber curtain), to make a booth;
- shop_weld_table: the steel welding table with its clamps;
- shop_welder: the MIG welder on its cart, its gas bottle, the gun and the work clamp hung on it;
- shop_torch_cart: the oxygen and acetylene cylinders chained on their cart, the regulators, the hoses to the torch;
- sign_eye_protection: the EYE PROTECTION REQUIRED notice."""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
from plant_panels import round_y
from spreading_room import oct_x, oct_y, oct_z
from admin_rooms import ring_z

STEEL, STEEL_DARK, CHROME, BLACK = "pi_steel", "pi_steel_dark", "pi_chrome", "pi_pnl_black"
RED, YELLOW, GREEN, WHITE = "pi_pnl_red", "pi_pnl_yellow", "pi_pnl_green", "pi_pnl_white"
WOOD = "pi_bench_wood"
PEG = "pi_dc_pegboard_0"
GLASS = "pi_cabinet_glass"
CURTAIN = "pi_shop_curtain"          # the welding screens' curtain (orange, see-through)
MACHINE = "pi_shop_machine"            # the machine tools' grey-green paint
BLUE = "pi_shop_blue"                  # the vise's and the welder's blue



def _tote(x0, y0, z0, x1, y1, z1, tex, wall, floor, fill, parts):
    """An open-topped tote bin: its four walls and its floor (each a box of its own, so its inside shows from above), the
    parts in it up to {fill}."""
    return [(x0, y0, z0, x1, y1, z0 + wall, tex), (x0, y0, z1 - wall, x1, y1, z1, tex),
            (x0, y0, z0 + wall, x0 + wall, y1, z1 - wall, tex), (x1 - wall, y0, z0 + wall, x1, y1, z1 - wall, tex),
            (x0 + wall, y0, z0 + wall, x1 - wall, y0 + floor, z1 - wall, tex),
            (x0 + wall + 0.02, y0 + floor, z0 + wall + 0.02, x1 - wall - 0.02, fill, z1 - wall - 0.02, parts)]


def ring_x(cz, cy, a_in, a_out, x0, x1, tex):
    """An octagonal ring standing up, facing +x (a coil hung on a hook, a tailstock's handwheel): as
    tools/admin_rooms.py ring_z, turned."""
    h = a_out * math.tan(math.radians(22.5)) + 0.05
    bars = [(x0, cy - h, cz + a_in, x1, cy + h, cz + a_out), (x0, cy - h, cz - a_out, x1, cy + h, cz - a_in),
            (x0, cy + a_in, cz - h, x1, cy + a_out, cz + h), (x0, cy - a_out, cz - h, x1, cy - a_in, cz + h)]
    rot = {"rotation": {"angle": 45, "axis": "x", "origin": [x0, cy, cz]}}
    return [b + (tex,) for b in bars] + [(x0 + 0.1, b[1], b[2], x1 - 0.1, b[4], b[5], tex, rot) for b in bars]


# The drawn faces: each drawing's size in px (drawn at the top left of a 256 px square, the face's UV that corner).
DRAWN = {"pi_shop_chest_low": (256, 220), "pi_shop_chest_top": (256, 90), "pi_shop_welder_front": (256, 230),
         "pi_shop_lathe_tag": (256, 64), "pi_shop_press_tag": (256, 43), "pi_shop_washer_front": (256, 164),
         "pi_shop_gauge": (256, 256), "pi_shop_estop": (256, 256),
         "pi_shop_lbl_o2": (256, 195), "pi_shop_lbl_ac": (256, 195), "pi_shop_lbl_ar": (256, 195)}


def F(tex, base=STEEL):
    """A box's faces: its front (south) the drawing, the rest base."""
    w, h = DRAWN[tex]
    return {"*": base, "south": (tex, [0, 0, round(16 * w / 256, 4), round(16 * h / 256, 4)])}


# ------------------------------------------------------------------------------------------------ the workbench

TOP_Y = (13.6, 14.6)                   # the maple top
TOP_Z = (3.0, 15.4)


def bench(v):
    """A block of the bench: v 0 its left end (the vise), 1 a middle (the toolbox), 2 its right end (the grinder)."""
    y0, y1 = TOP_Y
    z0, z1 = TOP_Z
    faces = {"*": WOOD}
    if v != 0:
        faces["west"] = None
    if v != 2:
        faces["east"] = None
    out = [(0.0, y0, z0, 16.0, y1, z1, faces)]
    # The steel frame: legs at the ends (and one pair in the middle block), the apron under the top's front, the
    # stretchers and the lower shelf.
    legs = {0: (0.6,), 1: (7.4,), 2: (14.2,)}[v]
    for lx in legs:
        for lz in (3.4, 13.8):
            out.append((lx, 0.0, lz, lx + 1.2, y0, lz + 1.2, STEEL_DARK))
            out.append((lx - 0.3, 0.0, lz - 0.3, lx + 1.5, 0.3, lz + 1.5, STEEL_DARK))
    af = {"*": STEEL_DARK}
    if v != 0:
        af["west"] = None
    if v != 2:
        af["east"] = None
    out += [(0.0, y0 - 1.0, z1 - 0.6, 16.0, y0, z1 - 0.1, af), (0.0, y0 - 1.0, z0, 16.0, y0, z0 + 0.5, af)]
    out += [(0.0, 3.0, z0 + 0.4, 16.0, 3.7, z1 - 0.7, dict(af, up=STEEL))]
    if v == 0:
        # The drawer unit under its right half, three drawers.
        out.append((8.6, 3.7, z0 + 0.5, 15.6, y0 - 1.0, z1 - 0.7, {"*": STEEL_DARK, "south": STEEL}))
        for k, (dy0, dy1) in enumerate(((4.2, 6.9), (7.2, 9.9), (10.2, 12.4))):
            out.append((9.0, dy0, z1 - 0.7, 15.2, dy1, z1 - 0.5, STEEL))
            out.append((11.0, (dy0 + dy1) / 2 - 0.25, z1 - 0.5, 13.2, (dy0 + dy1) / 2 + 0.25, z1, CHROME))
        # The machinist's vise on the top's front left: its base, body, fixed and moving jaws (their plates), the screw
        # and the tommy bar.
        vx0, vx1 = 1.4, 6.0
        out += [(vx0 + 0.2, y1, 8.5, vx1 - 0.2, y1 + 0.8, 14.2, BLUE), (vx0 + 0.6, y1 + 0.8, 8.0, vx1 - 0.6, y1 + 3.0, 13.4, BLUE)]
        out += [(vx0, y1 + 1.6, 7.6, vx1, y1 + 4.0, 8.8, BLUE), (vx0 + 0.1, y1 + 2.4, 8.8, vx1 - 0.1, y1 + 4.0, 9.1, STEEL_DARK)]
        out += [(vx0, y1 + 1.6, 12.4, vx1, y1 + 4.0, 13.6, BLUE), (vx0 + 0.1, y1 + 2.4, 12.1, vx1 - 0.1, y1 + 4.0, 12.4, STEEL_DARK)]
        cx, cy = (vx0 + vx1) / 2, y1 + 2.2
        out += oct_z(cx, cy, 13.6, 16.6, 0.35, CHROME)
        out += [(cx - 0.6, cy - 0.6, 15.8, cx + 0.6, cy + 0.6, 16.4, STEEL_DARK)]
        out += oct_x(16.1, cy, cx - 2.8, cx + 2.8, 0.22, CHROME)
        out += [(cx - 3.1, cy - 0.35, 15.75, cx - 2.7, cy + 0.35, 16.45, BLACK), (cx + 2.7, cy - 0.35, 15.75, cx + 3.1, cy + 0.35, 16.45, BLACK)]
    elif v == 1:
        # A toolbox and a tray of parts on the top; two bins on the shelf.
        out += [(2.5, y1, 6.0, 9.5, y1 + 3.2, 11.0, RED), (2.4, y1 + 3.2, 5.9, 9.6, y1 + 3.5, 11.1, RED),
                (4.5, y1 + 3.5, 8.3, 7.5, y1 + 4.3, 8.7, BLACK), (4.4, y1 + 3.5, 8.2, 4.8, y1 + 4.3, 8.8, CHROME),
                (7.2, y1 + 3.5, 8.2, 7.6, y1 + 4.3, 8.8, CHROME), (2.4, y1 + 1.4, 11.0, 9.6, y1 + 1.6, 11.15, CHROME)]
        out += [(11.0, y1, 7.0, 14.5, y1 + 0.8, 12.0, STEEL), (11.4, y1 + 0.2, 7.4, 14.1, y1 + 0.75, 11.6, STEEL_DARK)]
        for bx in (2.0, 8.6):
            out += _tote(bx, 3.7, 5.0, bx + 5.4, 7.6, 14.2, BLUE, 0.3, 0.2, 7.0, STEEL_DARK)
    else:
        # The drawer unit under its left half; the bench grinder on its right: motor, its two wheel guards (the wheels
        # in them), the eye shields, the tool rests, its switch.
        out.append((0.4, 3.7, z0 + 0.5, 7.4, y0 - 1.0, z1 - 0.7, {"*": STEEL_DARK, "south": STEEL}))
        for dy0, dy1 in ((4.2, 6.9), (7.2, 9.9), (10.2, 12.4)):
            out.append((0.8, dy0, z1 - 0.7, 7.0, dy1, z1 - 0.5, STEEL))
            out.append((2.8, (dy0 + dy1) / 2 - 0.25, z1 - 0.5, 5.0, (dy0 + dy1) / 2 + 0.25, z1, CHROME))
        gx, gz, gy = 11.0, 9.5, y1 + 3.2
        out += [(gx - 2.0, y1, gz - 2.2, gx + 2.0, y1 + 0.8, gz + 2.2, MACHINE), (gx - 1.2, y1 + 0.8, gz - 1.2, gx + 1.2, gy - 1.6, gz + 1.2, MACHINE)]
        out += oct_x(gz, gy, gx - 2.6, gx + 2.6, 1.9, MACHINE)
        for s in (-1, 1):
            wx0, wx1 = (gx - 4.8, gx - 2.6) if s < 0 else (gx + 2.6, gx + 4.8)
            out += [(wx0, gy - 2.6, gz - 2.6, wx1, gy + 2.6, gz + 0.8, MACHINE)]
            out += [(wx0 + 0.3, gy - 2.4, gz + 0.8, wx1 - 0.3, gy - 0.2, gz + 2.4, "pi_shop_wheel")]
            out += [(wx0 + 0.2, gy + 0.6, gz + 2.0, wx1 - 0.2, gy + 2.2, gz + 2.15, GLASS)]
            out += [(wx0 + 0.4, gy - 0.9, gz + 2.4, wx1 - 0.4, gy - 0.6, gz + 3.4, STEEL_DARK)]
        out += [(gx - 0.8, y1 + 0.8, gz + 1.2, gx + 0.8, y1 + 2.0, gz + 1.6, BLACK), (gx - 0.25, y1 + 1.2, gz + 1.6, gx + 0.25, y1 + 1.6, gz + 1.9, YELLOW)]
    return out


def pegboard(v):
    """The pegboard on the wall over a bench block (its foot down behind the bench's top): the board on its battens,
    the tools hung on it, the shelf over them (v: which tools)."""
    out = [(0.0, -1.4, 0.0, 16.0, 11.6, 0.6, {"*": "pi_dc_peg_edge", "south": PEG, "west": None if v else "pi_dc_peg_edge",
                                                "east": None if v != 2 else "pi_dc_peg_edge"})]
    out += [(0.0, 11.6, 0.0, 16.0, 12.2, 4.5, {"*": STEEL_DARK, "west": None if v else STEEL_DARK, "east": None if v != 2 else STEEL_DARK})]
    for bx in (1.5, 14.5):
        out.append((bx - 0.3, 9.4, 0.6, bx + 0.3, 11.6, 3.8, STEEL_DARK))
    z = 0.6
    if v == 0:
        # Combination wrenches, smallest to largest, then two hammers.
        for k in range(6):
            x = 1.6 + k * 1.1
            ln = 4.0 + k * 0.7
            out += [(x - 0.22, 8.8 - ln, z, x + 0.22, 8.8, z + 0.25, CHROME), (x - 0.42, 8.8 - ln - 0.6, z, x + 0.42, 8.8 - ln, z + 0.3, CHROME)]
        for hx in (10.5, 13.0):
            out += [(hx - 0.3, 1.0, z, hx + 0.3, 7.8, z + 0.6, WOOD), (hx - 1.3, 7.8, z, hx + 1.3, 8.9, z + 0.9, STEEL_DARK)]
    elif v == 1:
        # Screwdrivers in a rack, pliers, a tape, a level.
        out.append((1.0, 7.6, z, 7.0, 8.2, z + 1.4, STEEL_DARK))
        for k in range(6):
            x = 1.6 + k * 0.95
            out += [(x - 0.3, 7.6, z + 0.5, x + 0.3, 9.8, z + 1.1, (RED, YELLOW)[k % 2]), (x - 0.1, 3.8 + k * 0.3, z + 0.7, x + 0.1, 7.6, z + 0.9, CHROME)]
        for k, px in enumerate((8.8, 10.6)):
            out += [(px - 0.6, 5.2, z, px - 0.2, 8.6, z + 0.3, RED), (px + 0.2, 5.2, z, px + 0.6, 8.6, z + 0.3, RED),
                    (px - 0.5, 8.6, z, px + 0.5, 10.0, z + 0.4, STEEL_DARK)]
        out += [(12.2, 6.8, z, 14.6, 8.8, z + 1.0, YELLOW), (11.6, 2.0, z, 15.2, 3.0, z + 0.7, YELLOW), (12.9, 2.2, z + 0.7, 13.9, 2.8, z + 0.8, GREEN)]
    else:
        # A breaker bar and sockets on a rail, files, a grease gun.
        out += [(1.0, 7.8, z, 8.0, 8.2, z + 0.5, STEEL_DARK)]
        for k in range(8):
            x = 1.5 + k * 0.85
            out += oct_y(x, z + 0.6, 6.6 - k * 0.1, 7.8, 0.3 + k * 0.02, CHROME)
        out += [(9.5, 1.0, z, 10.1, 9.5, z + 0.4, CHROME), (9.4, 9.5, z, 10.2, 10.3, z + 0.6, CHROME)]
        for k in range(3):
            fx = 11.2 + k * 0.7
            out += [(fx - 0.2, 3.0, z, fx + 0.2, 7.6, z + 0.25, STEEL_DARK), (fx - 0.3, 7.6, z, fx + 0.3, 9.4, z + 0.6, WOOD)]
        out += oct_y(14.0, z + 1.0, 2.0, 7.0, 0.9, RED) + [(13.8, 7.0, z + 0.8, 14.2, 9.2, z + 1.2, STEEL_DARK)]
    # On the shelf: the manuals and an oil can (v 0), a row of parts bins (v 1), spray cans and a roll of tape (v 2).
    if v == 0:
        out += [(2.0, 12.2, 1.0, 2.9, 15.6, 4.0, "pi_binder_side"), (3.0, 12.2, 1.0, 3.9, 15.4, 4.0, "pi_binder_side"),
                (4.0, 12.2, 1.0, 4.9, 15.2, 4.0, "pi_binder_side")]
        out += oct_y(12.5, 2.5, 12.2, 14.6, 1.0, "pi_can_red") + oct_y(12.5, 2.5, 14.6, 16.4, 0.18, CHROME)
    elif v == 1:
        for k in range(5):
            x = 1.2 + k * 2.9
            out += _tote(x, 12.2, 0.8, x + 2.6, 14.0, 4.2, (BLUE, YELLOW)[k % 2], 0.2, 0.2, 13.4, STEEL_DARK)
    else:
        for k, (x, tex) in enumerate(((2.0, RED), (3.4, YELLOW), (4.8, "pi_can_silver"))):
            out += oct_y(x, 2.4, 12.2, 15.4, 0.55, tex) + oct_y(x, 2.4, 15.4, 15.9, 0.25, WHITE)
        out += [(10.0, 12.2, 1.4, 12.4, 13.0, 3.8, BLACK), (10.6, 13.0, 2.0, 11.8, 13.05, 3.2, STEEL_DARK)]
    return out


# ------------------------------------------------------------------------------------------------ the machine tools

def drill_press():
    """A floor drill press against the wall: its base, the column, the table on its arm, the head (spindle, chuck and
    bit, the feed handles, the switch), the belt guard and the motor behind it, the work light."""
    out = [(2.6, 0.0, 2.0, 13.4, 1.2, 13.0, MACHINE), (3.0, 1.2, 2.4, 13.0, 1.5, 12.6, MACHINE)]
    out += round_y(8.0, 4.2, 1.1, 1.5, 27.5, CHROME)
    out += [(6.6, 12.0, 3.0, 9.4, 13.4, 6.6, MACHINE), (4.0, 12.8, 6.6, 12.0, 13.8, 13.8, MACHINE), (4.4, 13.8, 7.0, 11.6, 14.0, 13.4, STEEL_DARK)]
    out += [(5.2, 13.5, 13.8, 5.6, 13.9, 14.6, CHROME)]
    out += [(4.4, 22.0, 2.4, 11.6, 27.6, 11.4, MACHINE), (4.8, 22.4, 11.4, 11.2, 27.2, 11.6, MACHINE)]
    out += round_y(8.0, 9.4, 0.75, 17.6, 22.0, CHROME) + round_y(8.0, 9.4, 1.0, 16.4, 17.6, STEEL_DARK) + round_y(8.0, 9.4, 0.25, 14.2, 16.4, STEEL)
    out += oct_x(8.0, 20.0, 11.6, 12.6, 0.7, MACHINE)
    for dy, dz in ((2.6, 0.0), (-1.3, 2.25), (-1.3, -2.25)):
        y, z = 20.0 + dy, 8.0 + dz
        out += [(12.1, min(20.0, y) - 0.15, min(8.0, z) - 0.15, 12.4, max(20.0, y) + 0.15, max(8.0, z) + 0.15, CHROME)]
        out += [(11.9, y - 0.45, z - 0.45, 12.6, y + 0.45, z + 0.45, BLACK)]
    out += [(5.0, 23.0, 11.6, 7.0, 24.6, 12.1, BLACK), (5.5, 23.4, 12.1, 6.0, 24.2, 12.4, GREEN), (6.1, 23.4, 12.1, 6.6, 24.2, 12.4, RED)]
    out += [(3.6, 27.6, 1.8, 12.4, 30.4, 10.2, MACHINE), (3.8, 30.4, 2.0, 12.2, 30.6, 10.0, STEEL_DARK)]
    out += round_y(8.0, 1.6, 1.4, 27.6, 31.6, MACHINE) + [(6.8, 31.6, 0.4, 9.2, 32.0, 2.8, STEEL_DARK)]
    out += [(3.2, 22.4, 4.0, 4.4, 23.4, 5.0, STEEL_DARK)]
    out += oct_x(4.5, 23.0, 1.6, 3.2, 0.2, STEEL_DARK) + oct_z(1.8, 23.0, 4.5, 9.0, 0.2, STEEL_DARK)
    out += [(1.2, 20.6, 8.4, 2.4, 22.6, 10.0, STEEL_DARK), (1.3, 20.4, 8.5, 2.3, 20.6, 9.9, "pi_emlight_glass")]
    return out


LATHE_Y = (11.0, 15.0)                 # the bed
LATHE_C = (19.5, 8.5)                  # the centres' height and their z


def lathe(v):
    """A block of the lathe (its spindle along x, the headstock on the left as you face it): v 0 the headstock end, 1
    the carriage, 2 the tailstock end."""
    by0, by1 = LATHE_Y
    cy, cz = LATHE_C
    end = {"west": None} if v else {}
    if v != 2:
        end["east"] = None
    out = []
    # The bed with its ways, the chip pan, the splash guard behind, the lead screw and the feed rod along its front.
    out.append((0.0, by0, 4.8, 16.0, by1, 12.2, dict({"*": MACHINE}, **end)))
    for wz in (5.2, 10.8):
        out.append((0.0, by1, wz, 16.0, by1 + 0.6, wz + 1.0, dict({"*": CHROME}, **end)))
    out.append((0.0, by0 - 0.6, 2.4, 16.0, by0, 14.6, dict({"*": STEEL}, **end)))
    out.append((0.0, by0, 2.4, 16.0, by0 + 0.4, 2.8, dict({"*": STEEL}, **end)))
    out.append((0.0, by0, 14.2, 16.0, by0 + 0.4, 14.6, dict({"*": STEEL}, **end)))
    out.append((0.0, by0, 1.8, 16.0, cy + 3.0, 2.4, dict({"*": STEEL}, **end)))
    for ry, rz in ((12.6, 12.8), (13.6, 12.8)):
        out += [(0.0, ry - 0.3, rz - 0.3, 16.0, ry + 0.3, rz + 0.3, dict({"*": CHROME}, **end))]
    if v == 0:
        # The headstock cabinet under it, the headstock (its gearbox levers, the spindle and three-jaw chuck, the chuck
        # guard), the control panel, the tag.
        out += [(1.0, 0.0, 3.0, 13.0, by0 - 0.6, 13.6, {"*": MACHINE, "south": MACHINE}), (1.4, 1.0, 13.6, 12.6, 9.6, 13.8, MACHINE)]
        out += [(5.8, 4.6, 13.8, 8.2, 5.4, 14.3, CHROME)]
        out += [(0.6, by1 + 0.6, 3.6, 11.4, cy + 4.6, 12.6, MACHINE), (0.8, cy + 4.6, 3.8, 11.2, cy + 5.0, 12.4, STEEL_DARK)]
        for k, (lx, ly) in enumerate(((2.4, 21.8), (5.0, 21.8), (7.6, 21.8), (2.4, 17.6), (5.0, 17.6))):
            out += oct_z(lx, ly, 12.6, 13.4, 0.6, STEEL_DARK) + oct_z(lx, ly + (0.8 if k % 2 else -0.8), 13.0, 14.6, 0.18, CHROME)
            out += [(lx - 0.35, ly + (1.6 if k % 2 else -1.6) - 0.35, 14.4, lx + 0.35, ly + (1.6 if k % 2 else -1.6) + 0.35, 15.1, BLACK)]
        out += [(8.6, 16.4, 12.6, 11.0, 19.2, 13.2, BLACK), (9.0, 17.0, 13.2, 9.6, 17.6, 13.5, GREEN), (9.9, 17.0, 13.2, 10.5, 17.6, 13.5, RED),
                (9.2, 18.0, 13.2, 10.4, 19.2, 13.6, F("pi_shop_estop", BLACK))]
        out.append((0.9, 22.9, 12.6, 4.5, 23.8, 12.7, F("pi_shop_lathe_tag")))
        out += oct_x(cz, cy, 11.4, 12.4, 2.2, STEEL_DARK) + oct_x(cz, cy, 12.4, 14.4, 3.0, CHROME)
        for dy, dz in ((2.4, 0.0), (-1.2, 2.1), (-1.2, -2.1)):
            out += [(14.4, cy + dy - 0.5, cz + dz - 0.5, 15.0, cy + dy + 0.5, cz + dz + 0.5, STEEL_DARK)]
        out += [(11.6, cy + 3.2, cz - 3.6, 15.2, cy + 3.6, cz + 3.6, MACHINE), (11.6, cy + 1.0, cz + 3.6, 15.2, cy + 3.6, cz + 3.9, GLASS)]
        out += oct_x(cz, cy, 15.0, 16.0, 1.1, STEEL)
    elif v == 1:
        # The carriage (saddle, apron and its handwheel and levers, cross slide, compound and tool post with its tool),
        # the work between centres.
        out += oct_x(cz, cy, 0.0, 16.0, 1.1, STEEL)
        out += [(4.6, by1 + 0.6, 3.6, 11.4, by1 + 2.2, 13.4, MACHINE), (4.8, by0 + 0.2, 13.4, 11.2, by1 + 2.2, 15.0, MACHINE)]
        out += oct_z(6.4, 13.2, 15.0, 15.8, 0.35, CHROME) + ring_z(6.4, 13.2, 1.5, 1.85, 15.25, 15.65, CHROME)
        out += [(6.25, 13.55, 15.3, 6.55, 14.7, 15.6, CHROME), (6.2, 14.4, 15.65, 6.6, 14.8, 16.5, BLACK)]
        out += oct_z(9.6, 15.6, 15.0, 16.0, 0.18, CHROME) + [(9.25, 15.25, 15.9, 9.95, 15.95, 16.6, BLACK)]
        out += [(5.6, by1 + 2.2, 4.6, 10.4, by1 + 3.0, 12.4, MACHINE), (6.4, by1 + 3.0, 9.6, 9.6, by1 + 3.8, 12.4, MACHINE)]
        out += [(7.0, by1 + 3.8, 10.0, 9.0, by1 + 6.4, 12.0, STEEL_DARK), (7.6, cy - 0.35, cz + 1.0, 8.4, cy + 0.15, 10.0, STEEL)]
        out += oct_z(8.0, by1 + 6.6, 11.0, 13.6, 0.18, CHROME)
    else:
        # The tailstock (its quill and live centre toward the work, the handwheel, the clamp lever), the cabinet under it.
        out += [(3.0, 0.0, 3.0, 15.0, by0 - 0.6, 13.6, MACHINE), (3.4, 1.0, 13.6, 14.6, 9.6, 13.8, MACHINE)]
        out += oct_x(cz, cy, 0.0, 1.0, 1.1, STEEL)
        out += [(4.0, by1 + 0.6, 5.0, 12.0, cy + 2.0, 12.0, MACHINE), (5.0, cy + 2.0, 6.0, 11.0, cy + 2.6, 11.0, MACHINE)]
        out += oct_x(cz, cy, 2.0, 4.0, 1.0, CHROME) + oct_x(cz, cy, 1.0, 2.0, 0.6, STEEL_DARK)
        out += oct_x(cz, cy, 12.0, 13.6, 0.35, CHROME) + ring_x(cz, cy, 1.6, 2.0, 13.1, 13.5, CHROME)
        out += [(13.15, cy + 0.35, cz - 0.15, 13.45, cy + 1.7, cz + 0.15, CHROME), (13.5, cy + 1.6, cz - 0.2, 14.6, cy + 2.0, cz + 0.2, BLACK)]
        out += oct_z(9.0, cy + 1.4, 12.0, 13.6, 0.2, CHROME) + [(8.6, cy + 1.0, 13.6, 9.4, cy + 1.8, 14.4, BLACK)]
    return out


def tool_chest():
    """A rolling tool cabinet with the chest on top: its drawers (their fronts drawn), the side handle, the casters."""
    out = [(1.6, 1.8, 3.0, 14.4, 12.8, 13.0, F("pi_shop_chest_low", RED)), (1.4, 12.8, 2.8, 14.6, 13.2, 13.2, BLACK)]
    out += [(2.0, 13.2, 3.6, 14.0, 17.4, 12.2, F("pi_shop_chest_top", RED)), (1.9, 17.4, 3.5, 14.1, 17.9, 12.3, RED)]
    out += [(6.4, 17.9, 7.4, 9.6, 18.5, 8.2, BLACK)]
    out += [(14.4, 8.0, 5.0, 14.7, 8.5, 5.5, CHROME), (14.4, 8.0, 10.5, 14.7, 8.5, 11.0, CHROME), (14.7, 7.9, 4.9, 15.2, 8.6, 11.1, CHROME)]
    for cx in (2.8, 13.2):
        for cz in (4.2, 11.8):
            out += [(cx - 0.6, 1.4, cz - 0.6, cx + 0.6, 1.8, cz + 0.6, CHROME), (cx - 0.25, 0.0, cz - 0.7, cx + 0.25, 1.4, cz + 0.7, BLACK)]
    return out


def press():
    """A 20-ton hydraulic H-frame press: the uprights on their feet, the bed on its pins, the top beam, the cylinder
    and ram, the hand pump and its handle, the gauge, its tag."""
    out = []
    for x0 in (1.4, 13.2):
        out += [(x0, 0.6, 6.0, x0 + 1.4, 30.0, 10.0, RED), (x0 - 0.6, 0.0, 2.0, x0 + 2.0, 0.6, 14.0, RED)]
    out += [(2.8, 12.0, 6.2, 13.2, 14.4, 9.8, RED), (2.8, 14.4, 6.6, 13.2, 14.8, 9.4, STEEL_DARK)]
    for px in (2.1, 13.9):
        out += oct_z(px, 11.2, 5.4, 10.6, 0.35, CHROME)
    out += [(2.8, 27.0, 6.0, 13.2, 30.0, 10.0, RED)]
    out += round_y(8.0, 8.0, 1.8, 21.0, 27.0, BLACK) + round_y(8.0, 8.0, 0.9, 17.4, 21.0, CHROME) + round_y(8.0, 8.0, 1.2, 16.4, 17.4, STEEL_DARK)
    out += [(14.6, 7.0, 10.0, 16.0, 10.0, 12.0, RED), (15.0, 10.0, 10.6, 15.6, 16.0, 11.2, CHROME)]
    out += [(15.1, 15.4, 9.4, 15.5, 15.8, 11.2, CHROME)]
    out += oct_z(11.0, 24.0, 10.0, 10.8, 0.25, CHROME) + oct_z(11.0, 24.0, 10.8, 11.3, 1.3, BLACK)
    out += [(10.0, 23.0, 11.3, 12.0, 25.0, 11.35, F("pi_shop_gauge", BLACK))]
    out.append((4.4, 28.0, 10.0, 11.6, 29.2, 10.1, F("pi_shop_press_tag", RED)))
    return out


def parts_washer():
    """A parts washer: the tank on its stand, the sink over it, the lid propped open behind (its fusible link), the brush
    on its hose, the pump under the sink, the label."""
    out = []
    for lx in (2.0, 13.0):
        for lz in (3.0, 12.4):
            out.append((lx, 0.0, lz, lx + 1.0, 6.0, lz + 1.0, STEEL_DARK))
    out += [(1.6, 6.0, 2.6, 14.4, 14.2, 13.8, F("pi_shop_washer_front", GREEN))]
    out += [(1.4, 14.2, 2.4, 14.6, 14.8, 14.0, GREEN), (2.4, 14.0, 3.4, 13.6, 14.3, 13.0, STEEL_DARK)]
    out += [(1.6, 14.8, 1.6, 14.4, 25.0, 2.2, GREEN), (1.6, 14.8, 2.2, 2.0, 15.6, 2.8, STEEL_DARK)]
    out += [(7.6, 14.8, 2.2, 8.4, 20.0, 2.6, CHROME)]
    out += oct_z(4.0, 15.6, 3.2, 8.0, 0.3, BLACK) + oct_y(4.0, 8.0, 14.3, 15.6, 0.3, BLACK)
    out += [(9.0, 14.3, 7.0, 13.0, 15.0, 9.0, RED), (9.4, 14.3, 7.4, 12.6, 14.4, 8.6, YELLOW)]
    out += [(5.0, 1.0, 5.0, 11.0, 5.0, 11.0, STEEL_DARK)] + round_y(8.0, 8.0, 1.6, 0.0, 1.0, STEEL_DARK)
    return out


def weld_screen():
    """A welding screen: its yellow tube frame on casters, the amber curtain on its rings."""
    out = []
    for x0, x1 in ((0.2, 1.0), (15.0, 15.8)):
        out += [(x0, 1.6, 7.6, x1, 29.6, 8.4, YELLOW), (x0 - 0.2, 1.2, 4.0, x1 + 0.2, 1.6, 12.0, YELLOW)]
        for z in (4.6, 11.4):
            out += [(x0 + 0.05, 0.0, z - 0.4, x1 - 0.05, 1.2, z + 0.4, BLACK)]
    out += [(1.0, 28.8, 7.6, 15.0, 29.6, 8.4, YELLOW), (1.0, 2.0, 7.7, 15.0, 2.6, 8.3, YELLOW)]
    out += [(1.2, 3.2, 7.85, 14.8, 28.4, 8.15, CURTAIN)]
    for k in range(7):
        x = 1.8 + k * 2.1
        out += [(x - 0.3, 28.2, 7.5, x + 0.3, 28.8, 8.5, CHROME)]
    return out


def weld_table():
    """The welding table: its slotted steel top, the legs and shelf, the C-clamps and a ground clamp on its edge."""
    out = [(0.6, 13.4, 1.0, 15.4, 14.6, 15.0, STEEL_DARK)]
    for k in range(5):
        x = 2.6 + k * 2.7
        out.append((x - 0.25, 14.4, 2.0, x + 0.25, 14.62, 14.0, BLACK))
    for lx in (1.2, 13.6):
        for lz in (1.6, 13.2):
            out.append((lx, 0.0, lz, lx + 1.2, 13.4, lz + 1.2, STEEL_DARK))
    out += [(1.2, 3.0, 1.6, 14.8, 3.6, 14.4, STEEL_DARK)]
    for cx in (4.0, 11.0):
        out += [(cx - 0.4, 12.2, 14.6, cx + 0.4, 16.4, 15.4, RED), (cx - 0.4, 15.6, 12.6, cx + 0.4, 16.4, 14.6, RED),
                (cx - 0.15, 10.4, 14.85, cx + 0.15, 12.2, 15.15, CHROME), (cx - 0.7, 10.0, 14.7, cx + 0.7, 10.4, 15.3, BLACK)]
    out += [(14.2, 13.2, 6.0, 15.8, 15.0, 7.6, CHROME)]
    return out


def welder():
    """The MIG welder on its cart: the machine (its front drawn: the display, the knobs, the gun's socket), the gas
    bottle chained at the back with its regulator, the gun and work cables coiled on the handle's hooks, the wheels."""
    out = [(2.0, 2.4, 2.0, 14.0, 3.0, 14.0, STEEL_DARK)]
    for wx in (2.6, 13.4):
        out += oct_x(11.0, 2.0, wx - 0.6, wx + 0.6, 2.0, BLACK) + oct_x(11.0, 2.0, wx - 0.7, wx + 0.7, 0.7, CHROME)
    out += [(2.8, 0.0, 3.2, 3.6, 2.4, 4.0, STEEL_DARK), (12.4, 0.0, 3.2, 13.2, 2.4, 4.0, STEEL_DARK)]
    out += [(3.0, 3.0, 6.0, 13.0, 12.0, 14.0, F("pi_shop_welder_front", BLUE)), (3.4, 12.0, 6.4, 12.6, 12.4, 13.6, STEEL_DARK)]
    out += [(5.0, 12.4, 9.4, 11.0, 13.2, 10.2, BLACK)]
    out += round_y(8.0, 3.4, 2.0, 3.0, 24.0, "pi_shop_cyl_ar") + round_y(8.0, 3.4, 1.2, 24.0, 25.0, "pi_shop_cyl_ar")
    out += [(7.2, 20.4, 5.4, 8.8, 21.62, 5.45, F("pi_shop_lbl_ar", WHITE))]
    out += [(5.6, 12.0, 0.6, 10.4, 12.8, 6.0, STEEL_DARK), (5.6, 19.0, 0.6, 10.4, 19.8, 6.0, STEEL_DARK)]
    out += round_y(8.0, 3.4, 0.4, 25.0, 26.4, CHROME) + [(7.2, 26.0, 2.6, 8.8, 27.4, 4.2, CHROME)]
    out += oct_z(8.8, 26.7, 4.2, 4.8, 0.7, BLACK) + [(8.3, 26.2, 4.8, 9.3, 27.2, 4.85, F("pi_shop_gauge", BLACK))]
    out += [(2.2, 3.0, 2.4, 2.8, 16.0, 3.0, STEEL_DARK), (13.2, 3.0, 2.4, 13.8, 16.0, 3.0, STEEL_DARK), (2.2, 15.4, 2.4, 13.8, 16.0, 3.0, STEEL_DARK)]
    # The cables coiled on the hook on its right side; the gun laid on its top; the work clamp on the handle.
    out += [(13.0, 10.0, 8.6, 14.8, 10.4, 9.4, STEEL_DARK)]
    out += ring_x(9.0, 8.0, 1.5, 1.9, 13.2, 13.8, BLACK) + ring_x(9.0, 8.1, 1.4, 1.8, 13.9, 14.5, BLACK)
    out += [(4.0, 12.4, 11.8, 7.0, 13.2, 12.6, BLACK), (7.0, 12.5, 12.0, 9.5, 13.0, 12.4, CHROME), (9.5, 12.45, 11.9, 10.4, 13.05, 12.5, CHROME)]
    out += [(13.8, 12.6, 2.2, 14.8, 13.8, 3.2, "pi_dc_clamp")]
    return out


def torch_cart():
    """The oxy-acetylene cart: its frame and wheels, the oxygen (green) and acetylene (maroon) cylinders chained in it,
    their regulators and gauges, the twin hose coiled on the hook, the torch hung by it."""
    out = [(1.6, 1.6, 4.0, 14.4, 2.2, 13.4, STEEL_DARK), (1.6, 2.2, 3.4, 2.2, 26.0, 4.0, STEEL_DARK), (13.8, 2.2, 3.4, 14.4, 26.0, 4.0, STEEL_DARK),
           (1.6, 25.4, 3.4, 14.4, 26.0, 4.0, STEEL_DARK), (1.6, 13.0, 3.4, 14.4, 13.6, 4.0, STEEL_DARK)]
    for wx in (1.0, 15.0):
        out += oct_x(4.4, 2.2, wx - 0.6, wx + 0.6, 2.2, BLACK) + oct_x(4.4, 2.2, wx - 0.7, wx + 0.7, 0.7, CHROME)
    out += [(7.4, 0.0, 12.4, 8.6, 1.6, 13.4, STEEL_DARK)]
    for cx, tex, top in ((4.8, "pi_shop_cyl_o2", 24.2), (11.2, "pi_shop_cyl_ac", 21.6)):
        out += round_y(cx, 8.0, 2.8, 2.2, top, tex) + round_y(cx, 8.0, 1.6, top, top + 1.2, tex)
        out += [(cx - 1.05, top - 9.4, 10.8, cx + 1.05, top - 7.8, 10.85, F(tex.replace("_cyl_", "_lbl_"), WHITE))]
        out += round_y(cx, 8.0, 0.45, top + 1.2, top + 2.4, CHROME)
        out += [(cx - 0.9, top + 1.8, 9.0, cx + 0.9, top + 3.4, 10.6, CHROME)]
        out += oct_z(cx - 0.9, top + 2.6, 10.6, 11.2, 0.75, BLACK) + oct_z(cx + 0.9, top + 2.6, 10.6, 11.2, 0.75, BLACK)
        out += [(cx - 1.55, top + 1.95, 11.2, cx - 0.25, top + 3.25, 11.25, F("pi_shop_gauge", BLACK)),
                (cx + 0.25, top + 1.95, 11.2, cx + 1.55, top + 3.25, 11.25, F("pi_shop_gauge", BLACK))]
    for yy in (9.0, 17.0):
        out += [(1.6, yy, 4.0, 14.4, yy + 0.3, 4.3, CHROME), (1.6, yy, 11.4, 14.4, yy + 0.3, 11.7, CHROME)]
    out += ring_z(8.0, 13.8, 1.5, 2.0, 12.4, 12.9, GREEN) + ring_z(8.0, 13.8, 1.5, 2.0, 12.9, 13.4, RED)
    out += [(7.6, 16.2, 11.7, 8.4, 17.0, 13.6, STEEL_DARK)]
    out += [(9.6, 10.0, 11.6, 10.2, 15.0, 12.2, CHROME), (9.4, 15.0, 11.4, 10.4, 16.2, 12.4, BLACK), (9.75, 8.0, 11.75, 10.05, 10.0, 12.05, CHROME)]
    return out


GANTRY_TOP = (28.0, 32.0)              # its beam (an I-section: bottom flange, web, top flange)


def gantry(v):
    """A block of the portable gantry crane (a 1-ton A-frame on casters, three blocks across): v 0 its left end, 1 its
    middle (the trolley and the chain hoist on its beam, a motor slung from the hook over a pallet), 2 its right end.
    Each end: its upright on a base along z (the casters under the base's ends), the knee braces, the beam's end plate."""
    y0, y1 = GANTRY_TOP
    out = []
    xa, xb = {0: (0.0, 16.0), 1: (0.0, 16.0), 2: (0.0, 16.0)}[v]
    if v != 1:
        ux = 2.0 if v == 0 else 12.0
        out += [(ux - 0.4, 1.6, 0.4, ux + 2.4, 3.0, 15.6, YELLOW)]
        for cz in (1.4, 14.6):
            out += [(ux, 1.2, cz - 0.6, ux + 2.0, 1.6, cz + 0.6, STEEL_DARK), (ux + 0.5, 0.0, cz - 0.7, ux + 1.5, 1.2, cz + 0.7, BLACK)]
        out += [(ux, 3.0, 7.0, ux + 2.0, y0, 9.0, YELLOW), (ux - 0.4, y0 - 0.4, 6.2, ux + 2.4, y1, 9.8, YELLOW)]
        # The knee braces: 45 degrees from the base to the upright, either side.
        for z, a in ((2.6, 45), (13.4, -45)):
            out.append((ux + 0.3, 3.0, z - 0.4, ux + 1.7, 3.0 + 6.4, z + 0.4, YELLOW,
                        {"rotation": {"angle": a, "axis": "x", "origin": [ux + 0.3, 3.0, z]}}))
        bx0, bx1 = (ux + 2.4, 16.0) if v == 0 else (0.0, ux - 0.4)
    else:
        bx0, bx1 = 0.0, 16.0
    f = {"*": YELLOW}
    if v != 0 or bx0 == 0.0:
        f["west"] = None
    if v != 2 or bx1 == 16.0:
        f["east"] = None
    if v == 0:
        f["west"] = YELLOW
    if v == 2:
        f["east"] = YELLOW
    out += [(bx0, y0, 6.4, bx1, y0 + 0.8, 9.6, f), (bx0, y0 + 0.8, 7.6, bx1, y1 - 0.8, 8.4, f), (bx0, y1 - 0.8, 6.4, bx1, y1, 9.6, f)]
    if v == 1:
        # The trolley's side plates and wheels on the bottom flange, the chain hoist under it (its body, the hand chain's
        # wheel and loop), the load chain down to the hook block, the slings to the motor's eyes, the pallet under it.
        for z0, z1 in ((5.6, 6.0), (10.0, 10.4)):
            out += [(6.4, y0 - 1.6, z0, 9.6, y0 + 1.4, z1, STEEL)]
        for x in (7.0, 9.0):
            out += oct_z(x, y0 + 1.1, 6.0, 6.4, 0.45, BLACK) + oct_z(x, y0 + 1.1, 9.6, 10.0, 0.45, BLACK)
        out += [(6.4, y0 - 2.0, 5.6, 9.6, y0 - 1.6, 10.4, STEEL), (7.5, 23.6, 7.5, 8.5, y0 - 2.0, 8.5, STEEL_DARK)]
        out += [(6.0, 18.8, 6.0, 10.0, 23.6, 10.0, RED), (5.6, 19.4, 6.4, 6.0, 23.0, 9.6, RED), (10.0, 19.4, 6.4, 10.4, 23.0, 9.6, RED)]
        out += oct_x(8.0, 21.2, 10.4, 11.0, 2.2, RED) + oct_x(8.0, 21.2, 11.0, 11.4, 0.5, STEEL_DARK)
        for z in (5.4, 6.0):
            out += [(10.55, 8.0, z, 10.75, 18.6, z + 0.2, CHROME)]
        out += [(10.5, 7.8, 5.3, 10.8, 8.0, 6.3, CHROME)]
        out += [(7.85, 13.2, 7.85, 8.15, 18.8, 8.15, CHROME), (7.2, 11.4, 7.2, 8.8, 13.2, 8.8, YELLOW), (7.75, 10.2, 7.75, 8.25, 11.4, 8.25, STEEL_DARK)]
        out += [(4.6, 9.6, 7.8, 7.75, 10.0, 8.2, CHROME), (8.25, 9.6, 7.8, 11.4, 10.0, 8.2, CHROME)]
        for x in (4.4, 11.2):
            out += [(x, 7.4, 7.8, x + 0.4, 9.6, 8.2, CHROME)]
        # The motor (its frame, the fan cowl at one end, the shaft at the other, the terminal box, the lifting eyes).
        out += oct_x(8.0, 5.0, 3.6, 12.2, 2.3, BLUE) + oct_x(8.0, 5.0, 2.8, 3.6, 2.1, STEEL_DARK) + oct_x(8.0, 5.0, 12.2, 13.4, 0.4, CHROME)
        out += [(6.6, 7.0, 9.4, 9.0, 8.2, 10.8, BLUE)]
        out += [(4.3, 7.0, 7.7, 4.9, 7.4, 8.3, STEEL_DARK), (11.1, 7.0, 7.7, 11.7, 7.4, 8.3, STEEL_DARK)]
        out += [(1.6, 0.0, 2.2, 14.4, 0.4, 13.8, WOOD), (1.6, 0.4, 2.2, 14.4, 1.0, 3.2, WOOD), (1.6, 0.4, 7.5, 14.4, 1.0, 8.5, WOOD),
                (1.6, 0.4, 12.8, 14.4, 1.0, 13.8, WOOD), (1.6, 1.0, 2.2, 14.4, 1.4, 13.8, WOOD)]
    return out


# ------------------------------------------------------------------------------------------------ textures

def _img(w, h, rgb):
    return Image.new("RGBA", (w, h), tuple(rgb) + (255,))


def _square(img):
    """A drawing set at the top left of a 256 px square (the rest its edge colour; F's UV shows only the drawing)."""
    out = Image.new("RGBA", (256, 256), img.getpixel((0, img.height - 1)))
    out.paste(img, (0, 0))
    return out


def _f(size, bold=True):
    return PP.rfont(size, bold)


def _red(w, h, seed):
    return PP._solid((176, 30, 28), seed, 0.04).convert("RGBA").resize((w, h))


def chest_low():
    """The cabinet's front: six drawers (the bottom two deeper), each its pull."""
    img = _red(*DRAWN["pi_shop_chest_low"], 761)
    d = ImageDraw.Draw(img)
    rows = [0, 30, 60, 90, 120, 166, 220]
    for a, b in zip(rows, rows[1:]):
        d.rectangle([8, a + 4, 247, b - 3], outline=(110, 18, 16, 255), width=3)
        d.rectangle([40, a + 9, 215, a + 15], fill=(196, 200, 204, 255), outline=(90, 92, 96, 255), width=1)
    return _square(img)


def chest_top():
    img = _red(*DRAWN["pi_shop_chest_top"], 763)
    d = ImageDraw.Draw(img)
    for a in (0, 30, 60):
        d.rectangle([8, a + 3, 247, a + 28], outline=(110, 18, 16, 255), width=3)
        d.rectangle([60, a + 8, 196, a + 13], fill=(196, 200, 204, 255))
    return _square(img)


def welder_front():
    """The welder's face: its display (amps and volts), the two knobs, the power switch, the gun's socket and the work
    lead's terminal, the vents."""
    img = _img(*DRAWN["pi_shop_welder_front"], (34, 66, 140))
    d = ImageDraw.Draw(img)
    d.rectangle([18, 18, 238, 74], fill=(20, 20, 22, 255))
    d.text((70, 46), "185", font=_f(34), fill=(255, 120, 40, 255), anchor="mm")
    d.text((180, 46), "21.5", font=_f(30), fill=(255, 120, 40, 255), anchor="mm")
    d.text((70, 90), "AMPS", font=_f(16), fill=(230, 230, 230, 255), anchor="mm")
    d.text((180, 90), "VOLTS", font=_f(16), fill=(230, 230, 230, 255), anchor="mm")
    for cx in (70, 180):
        d.ellipse([cx - 24, 106, cx + 24, 154], fill=(24, 24, 26, 255), outline=(150, 150, 150, 255), width=2)
        d.line([(cx, 130), (cx + 12, 110)], fill=(230, 230, 230, 255), width=3)
    d.ellipse([22, 176, 56, 210], fill=(20, 20, 22, 255), outline=(170, 170, 170, 255), width=3)
    d.ellipse([200, 176, 234, 210], fill=(20, 20, 22, 255), outline=(170, 170, 170, 255), width=3)
    d.rectangle([108, 178, 148, 208], fill=(20, 20, 22, 255))
    d.text((128, 193), "ON", font=_f(14), fill=(230, 230, 230, 255), anchor="mm")
    for y in range(218, 228, 4):
        d.line([(30, y), (226, y)], fill=(20, 40, 90, 255), width=2)
    return _square(img)


def cylinder(rgb, seed):
    return PP._solid(rgb, seed, 0.06)


def cyl_label(word, rgb):
    """A cylinder's contents label (a decal on its front): the gas's name over its colour band."""
    img = _img(256, 195, (240, 240, 232))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 44], fill=tuple(rgb) + (255,))
    PP._fit_line(d, (128, 100), word, 64, 236, (20, 20, 22, 255))
    PP._fit_line(d, (128, 160), "COMPRESSED GAS", 30, 236, (160, 30, 28, 255))
    return _square(img)


def lathe_tag():
    img = _img(256, 64, (196, 200, 204))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 63], outline=(110, 112, 116, 255), width=3)
    PP._fit_line(d, (128, 32), "ENGINE LATHE  16 x 60", 30, 236, (30, 30, 34, 255))
    return _square(img)


def press_tag():
    img = _img(256, 43, (176, 30, 28))
    d = ImageDraw.Draw(img)
    PP._fit_line(d, (128, 22), "20 TON", 36, 236, (250, 250, 246, 255))
    return _square(img)


def washer_front():
    """The washer's front: its maker's plate, small, as the real thing's (not a placard)."""
    img = PP._solid((48, 120, 64), 767, 0.05).convert("RGBA").resize((256, 164))
    d = ImageDraw.Draw(img)
    d.rectangle([70, 22, 186, 62], fill=(220, 220, 212, 255), outline=(20, 20, 22, 255), width=2)
    PP._fit_line(d, (128, 35), "PARTS WASHER", 16, 104, (20, 20, 22, 255))
    PP._fit_line(d, (128, 52), "SOLVENT - KEEP LID SHUT", 10, 104, (160, 30, 28, 255))
    d.rectangle([20, 150, 236, 156], fill=(36, 92, 48, 255))
    return _square(img)


def gauge_face():
    img = _img(256, 256, (20, 20, 22))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 8, 247, 247], fill=(236, 236, 228, 255), outline=(150, 150, 150, 255), width=10)
    for k in range(9):
        a = math.radians(225 - 270 * k / 8)
        d.line([(128 + 84 * math.cos(a), 128 - 84 * math.sin(a)), (128 + 104 * math.cos(a), 128 - 104 * math.sin(a))], fill=(20, 20, 22, 255), width=6)
    d.line([(128, 128), (182, 86)], fill=(200, 30, 28, 255), width=10)
    return img


def wheel_tex():
    img = _img(64, 64, (120, 116, 110))
    d = ImageDraw.Draw(img)
    for r in range(4, 32, 6):
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], outline=(96, 92, 88, 255), width=2)
    return img


def estop_tex():
    img = _img(256, 256, (236, 196, 36))
    d = ImageDraw.Draw(img)
    d.ellipse([32, 32, 223, 223], fill=(200, 30, 28, 255))
    return img


def curtain():
    """The welding curtain: orange vinyl a welder's arc is safe to look at through, its folds."""
    img = Image.new("RGBA", (64, 64), (214, 92, 26, 150))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 8):
        d.line([(x, 0), (x, 63)], fill=(170, 64, 16, 170), width=2)
        d.line([(x + 4, 0), (x + 4, 63)], fill=(236, 128, 52, 140), width=1)
    return img


def makers():
    return {"pi_shop_machine": lambda: PP._solid((118, 132, 124), 751, 0.05),
            "pi_shop_blue": lambda: PP._solid((40, 74, 150), 753, 0.05),
            "pi_shop_chest_low": chest_low, "pi_shop_chest_top": chest_top, "pi_shop_welder_front": welder_front,
            "pi_shop_cyl_o2": lambda: cylinder((40, 120, 60), 755), "pi_shop_cyl_ac": lambda: cylinder((120, 30, 36), 757),
            "pi_shop_cyl_ar": lambda: cylinder((100, 104, 96), 759),
            "pi_shop_lbl_o2": lambda: cyl_label("OXYGEN", (40, 120, 60)), "pi_shop_lbl_ac": lambda: cyl_label("ACETYLENE", (120, 30, 36)),
            "pi_shop_lbl_ar": lambda: cyl_label("ARGON / CO2", (100, 104, 96)),
            "pi_shop_lathe_tag": lathe_tag, "pi_shop_press_tag": press_tag, "pi_shop_washer_front": washer_front,
            "pi_shop_gauge": gauge_face, "pi_shop_wheel": wheel_tex, "pi_shop_estop": estop_tex, CURTAIN: curtain}


SHARP = {"pi_shop_lbl_o2", "pi_shop_lbl_ac", "pi_shop_lbl_ar", "pi_shop_chest_low", "pi_shop_chest_top", "pi_shop_welder_front", "pi_shop_lathe_tag", "pi_shop_press_tag",
         "pi_shop_washer_front", "pi_shop_gauge", "pi_shop_estop"}


# ------------------------------------------------------------------------------------------------ the signs

SIGNS = {"sign_eye_protection": "Eye Protection Required Sign"}


def sign_image(kind, d, W, H, F, M_, fit):
    BLUE_, WHITE_, BLACK_ = (20, 80, 170, 255), (250, 250, 246, 255), (20, 20, 22, 255)
    d.rectangle([0, 0, W - 1, H - 1], fill=WHITE_, outline=BLACK_, width=10)
    d.rectangle([10, 10, W - 11, 132], fill=BLUE_)
    fit(d, (W // 2, 71), "NOTICE", 70, W - 60, WHITE_)
    # Safety glasses: the two lenses and the bridge, the arms.
    cx, cy = W // 2, 236
    for k in (-1, 1):
        d.ellipse([cx + k * 64 - 52, cy - 40, cx + k * 64 + 52, cy + 40], outline=BLUE_, width=14)
    d.arc([cx - 22, cy - 30, cx + 22, cy + 10], 200, 340, fill=BLUE_, width=12)
    fit(d, (W // 2, 352), "EYE PROTECTION", 44, M_, BLACK_)
    fit(d, (W // 2, 404), "REQUIRED", 52, M_, BLACK_)
    fit(d, (W // 2, 456), "IN THIS AREA", 34, M_, BLACK_)


def _signs():
    old = PP._sign_image

    def draw(kind):
        if kind not in SIGNS:
            return old(kind)
        W, H = 433, 512
        img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
        sign_image(kind, ImageDraw.Draw(img), W, H, lambda s: PP.rfont(s, True), W - 56, PP._fit_line)
        return img
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})
    PP.EXTRA_MAKERS.append(lambda: {f"pi_{k}": (lambda k=k: draw(k)) for k in SIGNS})
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}


# ------------------------------------------------------------------------------------------------ registering

PIECES = {
    "shop_bench": {"display": "Workshop Bench", "boxes": [], "variants": 3, "vboxes": [bench(v) for v in range(3)]},
    "shop_pegboard": {"display": "Workshop Pegboard", "boxes": [], "variants": 3, "vboxes": [pegboard(v) for v in range(3)]},
    "shop_drill_press": {"display": "Floor Drill Press", "boxes": drill_press()},
    "shop_lathe": {"display": "Engine Lathe", "boxes": [], "variants": 3, "vboxes": [lathe(v) for v in range(3)]},
    "shop_tool_chest": {"display": "Rolling Tool Chest", "boxes": tool_chest()},
    "shop_press": {"display": "Hydraulic Shop Press", "boxes": press()},
    "shop_parts_washer": {"display": "Parts Washer", "boxes": parts_washer()},
    "shop_weld_screen": {"display": "Welding Screen", "boxes": weld_screen()},
    "shop_weld_table": {"display": "Welding Table", "boxes": weld_table()},
    "shop_welder": {"display": "MIG Welder", "boxes": welder()},
    "shop_torch_cart": {"display": "Oxy-Acetylene Torch Cart", "boxes": torch_cart()},
    "shop_gantry": {"display": "Portable Gantry Crane", "boxes": [], "variants": 3, "vboxes": [gantry(v) for v in range(3)]},
}
_P = "#c:plates/steel"
CRAFTING = {
    "shop_bench": (["WWW", "P P", "P P"], {"W": "#minecraft:planks", "P": _P}, 3),
    "shop_pegboard": (["SSS", "PPP"], {"S": "minecraft:stick", "P": "minecraft:paper"}, 3),
    "shop_drill_press": (["PMP", " I ", "PPP"], {"P": _P, "M": "jugcraft:electric_motor", "I": "minecraft:iron_ingot"}, 1),
    "shop_lathe": (["PMP", "III", "P P"], {"P": _P, "M": "jugcraft:electric_motor", "I": "minecraft:iron_ingot"}, 3),
    "shop_tool_chest": (["RRR", "RCR", "N N"], {"R": "minecraft:red_dye", "C": "minecraft:chest", "N": "minecraft:iron_nugget"}, 1),
    "shop_press": (["PPP", "IPI", "R R"], {"P": _P, "I": "minecraft:iron_ingot", "R": "minecraft:red_dye"}, 1),
    "shop_parts_washer": (["PGP", "PBP", "I I"], {"P": _P, "G": "minecraft:green_dye", "B": "minecraft:bucket", "I": "minecraft:iron_ingot"}, 1),
    "shop_weld_screen": (["YOY", "YOY", "N N"], {"Y": "minecraft:yellow_dye", "O": "minecraft:orange_stained_glass_pane", "N": "minecraft:iron_nugget"}, 2),
    "shop_weld_table": (["PPP", "I I", "I I"], {"P": _P, "I": "minecraft:iron_ingot"}, 1),
    "shop_welder": (["BRB", "BCB", "N N"], {"B": "minecraft:blue_dye", "R": "minecraft:redstone", "C": "minecraft:copper_ingot", "N": "minecraft:iron_nugget"}, 1),
    "shop_torch_cart": (["G R", "IPI", "N N"], {"G": "minecraft:green_dye", "R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "P": _P,
                                               "N": "minecraft:iron_nugget"}, 1),
    "shop_gantry": (["PPP", "Y Y", "Y Y"], {"P": _P, "Y": "minecraft:yellow_dye"}, 3),
    # (its own shape: the plant signs' paper-dye-nugget one was ten others'; the tower's lab uses it, layout 25)
    "sign_eye_protection": (["PBP", "PGP"], {"P": "minecraft:paper", "B": "minecraft:blue_dye", "G": "minecraft:glass_pane"}, 2),
}


def register():
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP
    PP.SEE_THROUGH.add(CURTAIN)
    _signs()


register()
