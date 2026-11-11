"""The automated warehouse, part W3 (floors and scale; claude/jugcraft-warehouse-plan.md): the Pallet Lift (a
vertical reciprocating conveyor joining the floors: a landing on each floor with its gate and transfer conveyor, the
shaft between them, the drive on top), and the fittings of a real distribution centre: aisle signs, pedestrian
walkways and drone crossings with their beacons, rack end barriers, in-rack sprinklers (the plant kit's railings,
high-bay lights, sprinkler mains and steel beams are used as they are).

Every piece is drawn as tools/warehouse.py draws them: front to +z (toward its user in the aisle), x across."""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
import warehouse as W
from warehouse import box, round_x, round_y, round_z, rot_x, rot_y

YELLOW, GRAPHITE, STEEL, CHROME, BLACK, HAZARD = W.YELLOW, W.GRAPHITE, W.STEEL, W.CHROME, W.BLACK, W.HAZARD
MACHINE, RUBBER = W.MACHINE, W.RUBBER
MESH, MESH_Y, CHAIN = "pi_wh_mesh", "pi_wh_mesh_frame", "pi_wh_chain"
RED = "pi_wh_spk_red"

# ================================================================================================ the pallet lift
# Its column: three wide, two deep. The shaft is the middle column's two blocks (the carriage holds a pallet with its
# middle at z 16); the gate is in the front face (z 32) over the opening x 14.6..33.4 (the dispatch hatch's width), up
# to y 32; the transfer conveyor stands out front in the next block (the bed cell, z 32..48), its pallet's middle at
# z 40, as Ready Storage's roller bed. The counterweight runs in its channel on the left, the carriage's cable track
# on the right; the call station is on the right of the front.
LIFT_X0, LIFT_X1, LIFT_TOP = 14.6, 33.4, 32.0      # the opening
LANDING_H = 48.0                                    # a landing: three blocks (the opening, the gate's housing)
DECK_Y = 2.15                                       # the carriage's rollers' tops over its floor (as the bed's)
CARRIAGE_Z = 16.0                                   # the pallet's middle on the carriage
BED_Z = 40.0                                        # and on the transfer conveyor
POSTS = ((10.6, 13.0), (35.0, 37.4))                # the four posts' x spans (front and back)


def _posts(y0, y1, front=True):
    """The shaft's four yellow posts (square tube, front ones either side of the gate's frame)."""
    out = []
    for x0, x1 in POSTS:
        out.append(box(x0, y0, 1.0, x1, y1, 3.4, YELLOW))
        if front:
            out.append(box(x0, y0, 29.6, x1, y1, 32.0, YELLOW))
    return out


def _rails(y0, y1):
    """The carriage's guide rails (steel tees) on the side posts' inner faces, and the rails' brackets."""
    out = [box(13.0, y0, 14.6, 13.8, y1, 17.4, STEEL), box(34.2, y0, 14.6, 35.0, y1, 17.4, STEEL),
           box(13.8, y0, 15.6, 14.2, y1, 16.4, STEEL), box(33.8, y0, 15.6, 34.2, y1, 16.4, STEEL)]
    # Each rail stands off a bar across from the back post to the front post.
    for y in (y0 + 3.0, y0 + 11.0):
        if y + 1.0 <= y1:
            out += [box(11.0, y, 3.4, 13.0, y + 1.0, 29.6, GRAPHITE), box(35.0, y, 3.4, 37.0, y + 1.0, 29.6, GRAPHITE)]
    return out


def _mesh_panel_x(x, z0, z1, y0, y1):
    """A welded mesh panel across z (its face to x) in its graphite frame."""
    return [box(x, y0, z0, x + 0.4, y1, z1, MESH),
            box(x - 0.1, y0, z0, x + 0.5, y0 + 0.6, z1, GRAPHITE), box(x - 0.1, y1 - 0.6, z0, x + 0.5, y1, z1, GRAPHITE)]


def _mesh_panel_z(z, x0, x1, y0, y1):
    """A welded mesh panel across x (its face to z) in its graphite frame."""
    return [box(x0, y0, z, x1, y1, z + 0.4, MESH),
            box(x0, y0, z - 0.1, x1, y0 + 0.6, z + 0.5, GRAPHITE), box(x0, y1 - 0.6, z - 0.1, x1, y1, z + 0.5, GRAPHITE)]


def _channel(y0, y1):
    """The counterweight's channel on the left (its mesh round three sides, its corner angles) and the cable track's
    trough on the right (the carriage's flat cables loop down it)."""
    out = []
    # Left: the channel x 1.6..10.6, z 6..26.
    for x0, z0 in ((1.6, 6.0), (1.6, 25.0)):
        out.append(box(x0, y0, z0, x0 + 1.0, y1, z0 + 1.0, GRAPHITE))
    out += _mesh_panel_x(1.6, 7.0, 25.0, y0, y1) + _mesh_panel_z(26.0, 2.6, 10.6, y0, y1)
    out += _mesh_panel_z(5.6, 2.6, 10.6, y0, y1)
    # The counterweight's guide rails.
    out += [box(5.4, y0, 6.4, 6.6, y1, 7.0, STEEL), box(5.4, y0, 25.0, 6.6, y1, 25.6, STEEL)]
    # Right: the cable track's trough at the back corner, and the conduit beside it.
    out += [box(39.0, y0, 1.0, 46.0, y1, 1.6, GRAPHITE), box(39.0, y0, 1.6, 39.6, y1, 7.0, GRAPHITE), box(45.4, y0, 1.6, 46.0, y1, 7.0, GRAPHITE)]
    out += [box(47.0, y0, 2.0, 47.8, y1, 2.8, STEEL)]
    # The back of the shaft: mesh between the back posts, and the sides between the posts.
    out += _mesh_panel_z(0.6, 13.0, 35.0, y0, y1)
    out += _mesh_panel_x(10.8, 3.4, 29.6, y0, y1) + _mesh_panel_x(37.0, 3.4, 29.6, y0, y1)
    return out


def lift_shaft(front=True):
    """A Pallet Lift Shaft section (three wide, one high, two deep): the posts, rails, the shaft's mesh (the front too:
    front), the counterweight's channel and the cable track. Stood between landings, as many as the floor is high."""
    out = _posts(0.0, 16.0) + _rails(0.0, 16.0) + _channel(0.0, 16.0)
    if front:
        out += _mesh_panel_z(31.0, 13.0, 35.0, 0.0, 16.0)
    return out


def lift_gate(f):
    """The landing's telescoping gate, f open (0 shut, 1 up in its housing): two mesh panels in yellow frames, the
    lower riding up behind the upper. In the opening at z 30.4..31.6."""
    x0, x1, top = LIFT_X0, LIFT_X1, LIFT_TOP
    h = top / 2 + 0.4
    out = []
    # The lower panel rises twice as fast as the upper: open, they stand together in the housing (y 31.2..47.4).
    yu = top / 2 - 0.4 + f * (top / 2 - 0.4)
    yl = 0.4 + f * (top - 1.2)
    for y0, z0 in ((yl, 30.4), (yu, 31.0)):
        out += [box(x0 + 0.2, y0, z0 + 0.15, x1 - 0.2, y0 + h - 0.2, z0 + 0.45, MESH),
                box(x0 + 0.2, y0, z0, x1 - 0.2, y0 + 0.8, z0 + 0.6, YELLOW), box(x0 + 0.2, y0 + h - 1.0, z0, x1 - 0.2, y0 + h - 0.2, z0 + 0.6, YELLOW),
                box(x0 + 0.2, y0 + 0.8, z0, x0 + 1.0, y0 + h - 1.0, z0 + 0.6, YELLOW), box(x1 - 1.0, y0 + 0.8, z0, x1 - 0.2, y0 + h - 1.0, z0 + 0.6, YELLOW)]
    out += [box(x0 + 0.2, yl - 0.4, 30.3, x1 - 0.2, yl, 30.9, RUBBER)]
    return out


def _call_station(screen):
    """The call station on the right of the front: the steel box on its post, the screen (the lift's state, live in
    the game), CALL, SEND, the key switch, the E-stop, the amber beacon on top (flashing while the lift moves)."""
    out = [box(38.6, 0.0, 29.4, 40.2, 22.0, 31.0, STEEL)]
    out += [box(38.0, 22.0, 29.0, 47.0, 36.0, 32.0, {"*": "pi_wh_desk", "south": "pi_wh_lift_station!"})]
    out += W.screen_unit(39.6, 28.4, 45.4, 34.6, 32.0, screen, hood=False)
    # CALL (green), SEND (blue), the key switch, the E-stop under a yellow guard.
    out += round_z(40.4, 26.9, 0.9, 32.0, 32.8, "pi_wh_lamp_green") + round_z(42.6, 26.9, 0.9, 32.0, 32.8, "pi_wh_lamp_blue")
    out += [box(44.2, 26.1, 32.0, 45.6, 27.7, 32.5, CHROME), box(44.7, 26.8, 32.5, 45.1, 27.0, 32.9, BLACK)]
    out += [box(38.6, 22.4, 32.0, 41.4, 23.8, 33.6, YELLOW)] + round_z(40.0, 23.1, 0.6, 32.0, 32.9, "pi_wh_estop")
    out += round_y(42.5, 31.5, 1.3, 36.0, 36.8, GRAPHITE) + PP.round_y(42.5, 31.5, 1.1, 36.8, 39.0, "pi_wh_beacon")
    return out


def _header(level_tex):
    """Over the opening: the gate's housing (the panels go up into it), its fascia with the lift's name and the floor
    (live text in the game), the position indicator's up and down arrows."""
    out = [box(13.0, LIFT_TOP, 29.0, 35.0, 47.6, 31.8, GRAPHITE)]
    out += [box(11.0, LIFT_TOP + 0.4, 31.8, 37.0, 47.2, 32.2, STEEL)]
    out += [box(13.0, 36.6, 32.2, 35.0, 44.6, 32.4, {"*": STEEL, "south": level_tex + "!"})]
    out += [box(15.0, 45.0, 32.2, 33.0, 46.8, 32.6, BLACK),
            box(29.6, 45.3, 32.6, 31.0, 46.5, 32.7, "pi_wh_lamp_green"), box(31.4, 45.3, 32.6, 32.8, 46.5, 32.7, "pi_wh_lamp_amber")]
    # A hazard band on the housing's bottom edge, over the opening.
    out += [box(LIFT_X0, LIFT_TOP, 31.8, LIFT_X1, LIFT_TOP + 1.2, 32.6, HAZARD)]
    return out


def _gate_frame():
    """The opening's yellow frame (the jambs the gate runs in), the light curtain's two posts inside it, the sill."""
    out = [box(13.0, 0.0, 29.6, LIFT_X0, LIFT_TOP, 32.6, YELLOW), box(LIFT_X1, 0.0, 29.6, 35.0, LIFT_TOP, 32.6, YELLOW)]
    out += [box(LIFT_X0, 0.0, 29.0, LIFT_X0 + 0.8, 26.0, 29.6, BLACK), box(LIFT_X1 - 0.8, 0.0, 29.0, LIFT_X1, 26.0, 29.6, BLACK)]
    for y in range(2, 26, 3):
        out += [box(LIFT_X0 + 0.8, y, 29.1, LIFT_X0 + 0.9, y + 0.5, 29.5, "pi_wh_emitter"),
                box(LIFT_X1 - 0.9, y, 29.1, LIFT_X1 - 0.8, y + 0.5, 29.5, "pi_wh_emitter")]
    out += [box(LIFT_X0, 0.0, 29.6, LIFT_X1, 0.3, 32.6, STEEL)]
    return out


def transfer_conveyor():
    """The landing's transfer conveyor out front (in the bed cell): the side frames on legs, the driven rollers, the
    chain guard and gear motor on the right, the end stop, the photo-eyes either side, the hazard edge on the floor."""
    x0, x1 = LIFT_X0, LIFT_X1
    out = [box(x0 - 1.0, 0.6, 32.6, x0, 2.4, 48.0, "pi_wh_conv_frame"), box(x1, 0.6, 32.6, x1 + 1.0, 2.4, 48.0, "pi_wh_conv_frame")]
    for k in range(10):
        out += round_x(1.6, 33.6 + k * 1.45, 0.55, x0, x1, CHROME)
    for zl in (33.2, 47.2):
        out += [box(x0 - 0.8, 0.0, zl - 0.4, x0 - 0.2, 0.6, zl + 0.4, STEEL), box(x1 + 0.2, 0.0, zl - 0.4, x1 + 0.8, 0.6, zl + 0.4, STEEL)]
    out += [box(x0 - 1.0, 2.4, 47.2, x1 + 1.0, 3.2, 47.8, YELLOW)]
    # The drive: chain guard along the right frame, the gear motor hung under it.
    out += [box(x1 + 1.0, 0.8, 34.0, x1 + 2.0, 2.6, 46.0, YELLOW), box(x1 + 1.0, 0.2, 38.0, x1 + 3.4, 2.2, 41.4, MACHINE)]
    out += round_z(x1 + 2.2, 1.2, 0.9, 41.4, 43.4, MACHINE)
    # The photo-eyes on their posts at the conveyor's far end.
    out += [box(x0 - 2.0, 0.0, 45.6, x0 - 1.2, 5.0, 46.4, BLACK), box(x1 + 1.2, 0.0, 45.6, x1 + 2.0, 5.0, 46.4, BLACK),
            box(x0 - 1.2, 3.6, 45.8, x0 - 1.1, 4.2, 46.2, "pi_wh_emitter"), box(x1 + 1.1, 3.6, 45.8, x1 + 1.2, 4.2, 46.2, "pi_wh_emitter")]
    return out


def lift_landing(gate=0.0, screen="pi_wh_scr_lift", level="pi_wh_lift_level"):
    """A Pallet Lift Landing (three wide, three high, two deep), one on each floor the lift serves: the shaft's posts,
    rails and mesh, the opening in its yellow frame with the telescoping gate (a live part, lift_gate), the housing
    over it with the lift's name and floor, the call station on the right, the counterweight's channel on the left,
    the transfer conveyor out front. gate: how far open, for the mockups (None: not drawn)."""
    out = _posts(0.0, LANDING_H) + _rails(0.0, LANDING_H) + _channel(0.0, LANDING_H)
    out += _gate_frame() + _header(level) + _call_station(screen) + transfer_conveyor()
    if gate is not None:
        out += lift_gate(gate)
    # The left block's front: the channel's mesh over a steel kick plate with its warning.
    out += [box(2.6, 0.0, 26.6, 10.6, 6.0, 27.0, {"*": STEEL, "south": "pi_wh_cw_plate!"})]
    return out


def lift_drive():
    """The Pallet Lift Drive (three wide, two high, two deep), on the top landing or shaft: the head frame of yellow
    beams over the shaft, the head shaft across it with a sprocket over each side's chain, the gear motor with its
    brake and the encoder on the right, the overspeed governor, the junction box, the counterweight's sheave."""
    out = []
    # The head frame: two beams along z over the side posts, two across.
    out += [box(10.6, 0.0, 1.0, 13.0, 3.0, 32.0, YELLOW), box(35.0, 0.0, 1.0, 37.4, 3.0, 32.0, YELLOW),
            box(10.6, 3.0, 1.0, 37.4, 5.4, 3.4, YELLOW), box(10.6, 3.0, 29.6, 37.4, 5.4, 32.0, YELLOW)]
    # The head shaft (across x, over the carriage's chains at x 13.6 and 34.4), its bearings, the sprockets.
    out += round_x(8.0, 16.0, 0.8, 9.0, 41.0, CHROME)
    for xb in (11.0, 35.4):
        out += [box(xb, 3.0, 13.6, xb + 1.6, 9.6, 18.4, GRAPHITE)]
    for xs in (13.4, 33.4):
        out += round_x(8.0, 16.0, 3.4, xs, xs + 1.2, BLACK)
    # The gear motor on its frame on the right (legs, base), the gearbox the head shaft runs into, the motor, its brake
    # (yellow) and the encoder on its end, the breather on the gearbox's top.
    for x, z in ((37.6, 8.4), (37.6, 22.6), (46.0, 8.4), (46.0, 22.6)):
        out.append(box(x, 0.0, z, x + 1.0, 3.0, z + 1.0, GRAPHITE))
    out += [box(37.4, 3.0, 8.0, 47.2, 4.0, 24.0, GRAPHITE), box(38.0, 4.0, 11.0, 42.0, 12.0, 21.0, MACHINE)]
    out += [box(42.6, 4.0, 13.4, 46.4, 5.2, 18.6, GRAPHITE)]
    out += round_x(8.0, 16.0, 3.0, 42.0, 46.0, MACHINE) + round_x(8.0, 16.0, 2.6, 46.0, 47.4, YELLOW)
    out += round_x(8.0, 16.0, 1.0, 47.4, 47.9, BLACK)
    out += [box(38.4, 12.0, 12.0, 41.6, 13.0, 20.0, MACHINE)] + round_y(40.0, 18.6, 0.5, 13.0, 14.2, BLACK)
    # The counterweight's sheave over its channel on the left (on its pedestals), and the governor beside it.
    out += [box(1.6, 0.0, 6.0, 10.6, 1.0, 26.0, GRAPHITE)]
    out += [box(1.6, 1.0, 14.4, 2.6, 9.4, 17.6, GRAPHITE), box(9.6, 1.0, 14.4, 10.6, 9.4, 17.6, GRAPHITE)]
    out += round_x(8.0, 16.0, 3.4, 4.6, 7.4, BLACK) + round_x(8.0, 16.0, 0.8, 2.6, 9.6, CHROME)
    out += [box(3.0, 1.0, 21.6, 6.0, 7.0, 24.6, YELLOW)] + round_x(5.0, 23.1, 1.6, 6.0, 7.0, BLACK)
    # The junction box on the frame's front, the conduit down into the cable track.
    out += [box(38.0, 4.0, 26.0, 46.0, 14.0, 31.0, {"*": "pi_wh_desk", "south": "pi_wh_lift_jbox!"})]
    out += [box(47.0, 0.0, 2.0, 47.8, 6.0, 2.8, STEEL)]
    return out


def lift_carriage(load=None):
    """The carriage (a live part, its floor at y 0): the yellow platform frame, the driven rollers it takes a pallet
    on with (as the conveyors'), the side rails and the mesh back guard, the guide shoes to the rails, the chain
    yokes, the cable track's end, the deck's drive under it."""
    out = [box(14.2, 0.0, 3.8, 33.8, 1.0, 29.2, YELLOW)]
    for k in range(17):
        out += round_x(1.6, 5.0 + k * 1.45, 0.55, 15.2, 32.8, CHROME)
    # Side rails: two tubes on posts each side; the back guard (mesh in a frame).
    for x0 in (14.2, 32.8):
        out += [box(x0, 1.0, 4.4, x0 + 1.0, 10.0, 5.4, YELLOW), box(x0, 1.0, 27.6, x0 + 1.0, 10.0, 28.6, YELLOW),
                box(x0, 9.0, 4.4, x0 + 1.0, 10.0, 28.6, YELLOW), box(x0, 5.0, 4.4, x0 + 1.0, 5.8, 28.6, YELLOW)]
    out += [box(15.2, 1.0, 3.8, 32.8, 18.0, 4.2, MESH), box(15.2, 17.2, 3.6, 32.8, 18.0, 4.4, YELLOW)]
    # Guide shoes on the rails (black) and the chain yokes on top of the side frames.
    for xs in (13.8, 33.4):
        out += [box(xs, 0.0, 14.8, xs + 0.8, 7.0, 17.2, BLACK)]
    out += [box(12.8, 9.4, 15.0, 15.2, 11.0, 17.0, GRAPHITE), box(32.8, 9.4, 15.0, 35.2, 11.0, 17.0, GRAPHITE)]
    # The deck's drive under the frame on the right; the cable track's end bracket.
    out += [box(33.8, 1.0, 6.0, 34.9, 4.0, 12.0, MACHINE), box(34.0, 0.0, 3.0, 39.0, 2.0, 5.0, GRAPHITE)]
    return out


def lift_counterweight():
    """The counterweight (a live part, its bottom at y 0): a frame of channels round its stacked steel plates, the
    guide shoes, the chain's eye on top."""
    out = [box(3.4, 0.0, 7.2, 8.6, 1.0, 24.8, GRAPHITE), box(3.4, 15.0, 7.2, 8.6, 16.0, 24.8, GRAPHITE),
           box(3.4, 1.0, 7.2, 8.6, 15.0, 8.2, GRAPHITE), box(3.4, 1.0, 23.8, 8.6, 15.0, 24.8, GRAPHITE)]
    for k in range(7):
        out.append(box(3.6, 1.2 + k * 2.0, 8.2, 8.4, 2.9 + k * 2.0, 23.8, STEEL))
    out += [box(5.2, 16.0, 15.0, 6.8, 18.0, 17.0, BLACK)]
    return out


def lift_chain():
    """One lifting chain, 16 long from y 0 (the game stretches it from the carriage's yoke up to its sprocket)."""
    return [box(-0.4, 0.0, -0.9, 0.4, 16.0, 0.9, CHAIN)]


# ================================================================================================ the fittings
def aisle_sign():
    """An aisle sign hung over an aisle's end: the two chains from the ceiling, the double-sided sign (the aisle's
    floor and letter, the bays along it; live text in the game) in its frame."""
    out = [box(2.6, 12.0, 7.7, 3.2, 16.0, 8.3, CHAIN), box(12.8, 12.0, 7.7, 13.4, 16.0, 8.3, CHAIN),
           box(2.0, 15.6, 7.2, 3.8, 16.0, 8.8, STEEL), box(12.2, 15.6, 7.2, 14.0, 16.0, 8.8, STEEL)]
    out += [box(0.4, 3.0, 7.4, 15.6, 12.0, 8.6, GRAPHITE),
            box(0.8, 3.4, 7.3, 15.2, 11.6, 7.4, {"*": GRAPHITE, "north": "pi_wh_aisle_sign!"}),
            box(0.8, 3.4, 8.6, 15.2, 11.6, 8.7, {"*": GRAPHITE, "south": "pi_wh_aisle_sign!"})]
    return out


def end_barrier():
    """A rack end barrier (end-of-aisle protection): a heavy steel angle across the rack's end, yellow and black, on
    its three bolted feet, the rubber buffer facing the aisle."""
    out = [box(1.0, 0.0, 9.0, 15.0, 6.0, 12.4, {"*": YELLOW, "south": "pi_wh_barrier_face!"}),
           box(1.0, 6.0, 9.0, 15.0, 6.6, 12.4, BLACK), box(1.0, 0.0, 5.0, 15.0, 0.6, 9.0, YELLOW)]
    for x in (2.0, 7.2, 12.4):
        out += [box(x, 0.0, 5.4, x + 1.6, 0.9, 7.0, STEEL)]
    out += [box(1.4, 1.4, 12.4, 14.6, 4.6, 13.2, RUBBER)]
    return out


def crossing_beacon(lit=False):
    """A drone crossing's beacon post at a walkway's edge: the yellow post on its base, the lamp head with two amber
    lamps each way (they flash while a drone is coming through the crossing; live in the game), the STOP - DRONES
    CROSSING plate on both faces under them."""
    out = [box(5.6, 0.0, 5.6, 10.4, 0.4, 10.4, STEEL)] + PP.round_y(8.0, 8.0, 1.0, 0.4, 9.8, YELLOW)
    out += [box(5.4, 9.8, 6.6, 10.6, 15.8, 9.4, BLACK)]
    lamp = "pi_wh_beacon" if lit else "pi_wh_lamp_amber"
    for y in (14.2, 11.4):
        out += round_z(8.0, y, 1.2, 9.4, 10.4, lamp) + round_z(8.0, y, 1.2, 5.6, 6.6, lamp)
    out += [box(5.4, 4.0, 9.0, 10.6, 8.6, 9.4, {"*": STEEL, "south": "pi_wh_cross_plate!"}),
            box(5.4, 4.0, 6.6, 10.6, 8.6, 7.0, {"*": STEEL, "north": "pi_wh_cross_plate!"})]
    return out


def walkway_model(mask, stencil=False):
    """A pedestrian walkway floor tile: green epoxy, a yellow line on each edge with no walkway beyond it (mask: N 1,
    E 2, S 4, W 8), the walking figure stencilled on some."""
    return [box(0, 0, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": f"pi_wh_walk_{mask}_{int(stencil)}!"})]


def crossing_model(along_x):
    """A drone crossing tile: a lane tile (drones drive on it) with the white zebra bars along the drones' way across
    the walkway (along_x: the drones cross along x)."""
    return [box(0, 0, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": f"pi_wh_cross_{int(along_x)}!"})]


def rack_sprinkler():
    """An in-rack sprinkler line fitted to a pallet place (drawn with the rack's place, at its back, under its top): the
    red branch line along the flue, its hangers off the back uprights, the upright head in its guard over the middle,
    the water shield over the head."""
    out = round_x(15.55, 2.15, 0.45, 0.0, 16.0, RED)
    for x in (3.0, 13.0):
        out += [box(x - 0.3, 15.1, 1.4, x + 0.3, 16.0, 1.6, STEEL)]
    # The head on the line's front, pointing out over the flue: its fitting, the brass frame and deflector, the bulb,
    # the wire guard round it.
    out += [box(7.6, 15.2, 2.6, 8.4, 15.9, 3.2, RED), box(7.7, 15.25, 3.2, 8.3, 15.85, 3.9, "pi_wh_brass"),
            box(7.4, 15.0, 3.9, 8.6, 16.0, 4.1, "pi_wh_brass"), box(7.9, 15.4, 3.3, 8.1, 15.7, 3.8, "pi_wh_bulb")]
    out += [box(7.0, 15.0, 2.6, 7.1, 16.0, 4.3, STEEL), box(8.9, 15.0, 2.6, 9.0, 16.0, 4.3, STEEL),
            box(7.1, 15.0, 4.2, 8.9, 15.1, 4.3, STEEL)]
    return out


# ================================================================================================ textures
def _mesh():
    """Welded wire mesh: black wires on a 2 px pitch at this scale, see-through between."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for k in range(0, 64, 8):
        d.line([(k, 0), (k, 63)], fill=(36, 38, 40, 255), width=1)
        d.line([(0, k), (63, k)], fill=(36, 38, 40, 255), width=1)
    return img


def _chain():
    img = W._img(32, 64, (40, 42, 44))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.rounded_rectangle([6, y + 1, 25, y + 9], radius=4, outline=(110, 114, 118, 255), width=2)
    return img


def _walkway(mask, stencil):
    import nuclear_materials as M
    img = M.concrete(2240, tie_holes=False).convert("RGBA").resize((128, 128))
    img = Image.alpha_composite(img, Image.new("RGBA", (128, 128), (52, 120, 74, 200)))
    d = ImageDraw.Draw(img)
    Y = (236, 190, 24, 255)
    if mask & 1:
        d.rectangle([0, 0, 127, 9], fill=Y)
    if mask & 4:
        d.rectangle([0, 118, 127, 127], fill=Y)
    if mask & 2:
        d.rectangle([118, 0, 127, 127], fill=Y)
    if mask & 8:
        d.rectangle([0, 0, 9, 127], fill=Y)
    if stencil:
        c = (236, 240, 236, 255)
        d.ellipse([56, 22, 72, 38], fill=c)
        d.line([(64, 40), (64, 74)], fill=c, width=10)
        d.line([(64, 72), (50, 104)], fill=c, width=8)
        d.line([(64, 72), (78, 104)], fill=c, width=8)
        d.line([(64, 48), (46, 66)], fill=c, width=7)
        d.line([(64, 48), (82, 62)], fill=c, width=7)
    return img.resize((64, 64), Image.LANCZOS)


def _crossing(along_x):
    img = W._lane(0, 1).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    for k in range(4, 128, 32):
        if not along_x:
            d.rectangle([k, 10, k + 16, 117], fill=(240, 242, 240, 255))
        else:
            d.rectangle([10, k, 117, k + 16], fill=(240, 242, 240, 255))
    return img.resize((64, 64), Image.LANCZOS)


def _aisle_sign(letter="2B", sub="BAYS 01 - 08", lv="LEVELS 0 - 4"):
    img = W._img(256, 152, (24, 60, 120))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 151], outline=(246, 246, 240, 255), width=6)
    W._t(d, (128, 62), "AISLE " + letter, 54, (250, 250, 246), width=232)
    W._t(d, (128, 112), sub, 22, (250, 250, 246), width=232)
    W._t(d, (128, 136), lv, 16, (200, 214, 236), width=232)
    return W._sq(img)


def _lift_level(n=2, name="PALLET LIFT 1"):
    img = W._img(512, 186, (246, 246, 240))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 185], outline=(30, 30, 32, 255), width=6)
    d.rectangle([0, 0, 150, 185], fill=(30, 30, 32, 255))
    W._t(d, (75, 96), str(n), 120, (236, 184, 20))
    W._t(d, (330, 60), name, 40, (20, 20, 22), width=330)
    W._t(d, (330, 118), f"FLOOR {n}  -  KEEP CLEAR", 26, (20, 20, 22), width=330)
    W._t(d, (330, 156), "AUTOMATIC GATE  -  NO RIDERS", 20, (160, 30, 26), width=330)
    return W._sq(img)


def _lift_page(state="AT FLOOR 1", load="2B-04-1  COAL", to="FLOOR 2", col=(100, 230, 140)):
    img = W._img(256, 160, (12, 18, 28))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 30], fill=(24, 48, 84, 255))
    W._t(d, (10, 15), "PALLET LIFT 1", 18, (236, 240, 246), anchor="lm", width=170)
    W._t(d, (246, 15), "FLOOR 1", 16, (150, 200, 240), anchor="rm", width=80)
    rows = (("CARRIAGE", state, col), ("PALLET", load, (236, 240, 246)), ("TO", to, (110, 210, 240)), ("GATE", "SHUT - LOCKED", (236, 240, 246)))
    for k, (a, b, c) in enumerate(rows):
        y = 50 + k * 26
        W._t(d, (10, y), a, 15, (150, 160, 178), anchor="lm", width=90)
        W._t(d, (246, y), b, 15, c, anchor="rm", width=150)
    return W._sq(img)


def _barrier_face():
    img = W._hazard(128, 16)
    return img


def _cw_plate():
    img = W._img(256, 96, (236, 184, 20))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 95], outline=(26, 26, 28, 255), width=5)
    W._t(d, (128, 36), "DANGER", 34, (26, 26, 28))
    W._t(d, (128, 72), "MOVING COUNTERWEIGHT", 20, (26, 26, 28), width=236)
    return W._sq(img)


def _lift_station():
    """The call station's front (drawn at the face's own proportions, 16 to the pixel): its labels under the buttons."""
    img = W._img(144, 224, (196, 200, 204))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 143, 223], outline=(150, 154, 158, 255), width=3)
    for x, t in ((38, "CALL"), (74, "SEND"), (110, "AUTO")):
        W._t(d, (x, 178), t, 11, (30, 30, 32))
    return W._sq(img)


def _lift_jbox():
    img = W._img(128, 128, (196, 200, 204))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 127], outline=(150, 154, 158, 255), width=4)
    d.rectangle([14, 20, 113, 52], fill=(236, 184, 20, 255))
    W._t(d, (64, 36), "400 V", 22, (26, 26, 28))
    W._t(d, (64, 84), "LIFT 1 DRIVE", 16, (30, 30, 32), width=110)
    return img


def _cross_plate():
    img = W._img(256, 200, (246, 246, 240))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 199], outline=(26, 26, 28, 255), width=6)
    d.rectangle([0, 0, 255, 70], fill=(200, 30, 26, 255))
    W._t(d, (128, 38), "STOP", 50, (250, 250, 246))
    W._t(d, (128, 112), "DRONES", 34, (26, 26, 28))
    W._t(d, (128, 160), "CROSSING", 34, (26, 26, 28))
    return W._sq(img)


def w3_makers():
    import nuclear_materials as M
    out = {
        MESH: _mesh, CHAIN: _chain, RED: lambda: PP._solid((196, 34, 30), 2241, 0.04),
        "pi_wh_brass": lambda: M.brushed((200, 160, 70), 2242, "h", dirt=0.05), "pi_wh_bulb": lambda: PP._solid((220, 40, 30), 2244, 0.02),
        "pi_wh_aisle_sign": _aisle_sign, "pi_wh_lift_level": _lift_level, "pi_wh_scr_lift": _lift_page,
        "pi_wh_barrier_face": _barrier_face, "pi_wh_cw_plate": _cw_plate, "pi_wh_lift_station": _lift_station,
        "pi_wh_lift_jbox": _lift_jbox, "pi_wh_cross_plate": _cross_plate,
        "pi_wh_cross_0": lambda: _crossing(False), "pi_wh_cross_1": lambda: _crossing(True),
    }
    for m in range(16):
        for s in (0, 1):
            out[f"pi_wh_walk_{m}_{s}"] = lambda m=m, s=s: _walkway(m, s)
    return out


W3_SHARP = {"pi_wh_aisle_sign", "pi_wh_lift_level", "pi_wh_scr_lift", "pi_wh_cw_plate", "pi_wh_lift_station", "pi_wh_lift_jbox",
            "pi_wh_cross_plate", "pi_wh_cross_0", "pi_wh_cross_1", "pi_wh_barrier_face"} | \
    {f"pi_wh_walk_{m}_{s}" for m in range(16) for s in (0, 1)}
W3_SEE_THROUGH = {MESH}
