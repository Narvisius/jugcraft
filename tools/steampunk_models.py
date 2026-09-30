"""Steampunk models for every machine (the default style; see tools/model_writer.py).

Coordinates are pixels in structure space, facing north (the front is the z = 0 side). One-block
machines stay inside 0..16. Multi-block machines use the footprints in tools/large_machines.py and
are sliced into per-block models by the generator.

Design rules:
- Everything is built from the helpers below: stepped round prisms (boilers, drums, tanks), gears,
  valve wheels, gauges, lamps and pipes. Only symmetric shapes use rotation (gear teeth, wheel rims,
  windmill spokes), so their look does not depend on the rotation direction.
- Overlapping boxes never share a visible face plane (that flickers in game).
- Near the middle of each side (pixels 6..10), the machine body comes within about a pixel of the
  block edge, so cables and pipes that connect there meet the machine instead of floating.
- Glowing textures listed in GLOW switch to their "_on" version while the machine runs.
"""
import math

GLOW = {
    "sp_firebox": "sp_firebox_on",
    "sp_window": "sp_window_on",
    "sp_arc_window": "sp_arc_window_on",
    "sp_lava_window": "sp_lava_window_on",
    "sp_lamp": "sp_lamp_on",
}
# Full-cube parts keep cube models; only their texture changes.
CUBES = {"machine_casing": "sp_machine_casing", "arc_furnace_casing": "sp_arc_casing"}
PARTICLE = "sp_iron_plate"

IRON, IRON_PLATE, BRASS, BRASS_PLATE, COPPER = "sp_iron", "sp_iron_plate", "sp_brass", "sp_brass_plate", "sp_copper"
AXES = {"x": 0, "y": 1, "z": 2}
CAP_FACES = {"x": ("east", "west"), "y": ("up", "down"), "z": ("north", "south")}
FACE_DIR = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}


def q(value):
    """Snap to quarter pixels so textures stay crisp."""
    return round(value * 4) / 4


def box(frm, to, texture, rotation=None):
    frm = tuple(float(v) for v in frm)
    to = tuple(float(v) for v in to)
    if rotation:
        return frm, to, texture, {"rotation": rotation}
    return frm, to, texture


def span(axis, a0, a1, u0, u1, v0, v1):
    """(from, to) of a box with a0..a1 along `axis` and u/v along the other two axes (in x, y, z order)."""
    i = AXES[axis]
    others = [j for j in range(3) if j != i]
    frm, to = [0.0] * 3, [0.0] * 3
    frm[i], to[i] = a0, a1
    frm[others[0]], to[others[0]] = u0, u1
    frm[others[1]], to[others[1]] = v0, v1
    return frm, to


def point(axis, a, u, v):
    i = AXES[axis]
    others = [j for j in range(3) if j != i]
    p = [0.0] * 3
    p[i], p[others[0]], p[others[1]] = a, u, v
    return p


def cyl(axis, cu, cv, r, a0, a1, side, cap=None):
    """A round prism along `axis` (a pixel-art circle of non-overlapping boxes); cu/cv center it."""
    cap = cap or side
    texture = {"*": side, CAP_FACES[axis][0]: cap, CAP_FACES[axis][1]: cap}
    if r <= 1.6:
        return [box(*span(axis, a0, a1, q(cu - r), q(cu + r), q(cv - r), q(cv + r)), texture)]
    heights = [0, 0.6 * r, r] if r <= 3.2 else [0, 0.45 * r, 0.8 * r, r]
    widths = [math.sqrt(max(0.0, r * r - h * h)) for h in heights[:-1]]
    out = [box(*span(axis, a0, a1, q(cu - widths[0]), q(cu + widths[0]), q(cv - heights[1]), q(cv + heights[1])), texture)]
    for band in range(1, len(widths)):
        w = widths[band]
        low, high = q(heights[band]), q(heights[band + 1])
        out.append(box(*span(axis, a0, a1, q(cu - w), q(cu + w), q(cv + low), q(cv + high)), texture))
        out.append(box(*span(axis, a0, a1, q(cu - w), q(cu + w), q(cv - high), q(cv - low)), texture))
    return out


def gear(axis, cu, cv, r, a0, a1, texture=BRASS, hub=IRON):
    """Eight-toothed gear: a disk, four straight teeth bars and the same bars turned 45 degrees."""
    out = cyl(axis, cu, cv, r * 0.72, a0, a1, texture)
    inset = min(0.15, (a1 - a0) / 4)
    tooth = max(0.6, r * 0.2)
    origin = point(axis, (a0 + a1) / 2, cu, cv)
    bars = [span(axis, a0 + inset, a1 - inset, q(cu - r), q(cu + r), q(cv - tooth), q(cv + tooth)),
            span(axis, a0 + inset, a1 - inset, q(cu - tooth), q(cu + tooth), q(cv - r), q(cv + r))]
    for frm, to in bars:
        out.append(box(frm, to, texture))
        out.append(box(frm, to, texture, rotation=(axis, 45, origin)))
    out += cyl(axis, cu, cv, max(0.75, r * 0.22), a0 - 0.3, a1 + 0.3, hub)
    return out


def wheel(axis, cu, cv, r, a0, a1, texture="sp_red_iron", hub=BRASS, rim=0.75, spokes=True):
    """Spoked wheel (valve handwheel or flywheel): an octagon rim, a cross of spokes and a hub."""
    out = []
    half_side = r * math.tan(math.radians(22.5))
    origin = point(axis, (a0 + a1) / 2, cu, cv)
    sides = [span(axis, a0, a1, q(cu + r - rim), q(cu + r), q(cv - half_side), q(cv + half_side)),
             span(axis, a0, a1, q(cu - r), q(cu - r + rim), q(cv - half_side), q(cv + half_side)),
             span(axis, a0, a1, q(cu - half_side), q(cu + half_side), q(cv + r - rim), q(cv + r)),
             span(axis, a0, a1, q(cu - half_side), q(cu + half_side), q(cv - r), q(cv - r + rim))]
    for frm, to in sides:
        out.append(box(frm, to, texture))
        out.append(box(frm, to, texture, rotation=(axis, 45, origin)))
    spoke = 0.35
    inset = min(0.1, (a1 - a0) / 4)
    if not spokes:
        return out + cyl(axis, cu, cv, max(0.7, r * 0.2), a0 - 0.25, a1 + 0.25, hub)
    out.append(box(*span(axis, a0 + inset, a1 - inset, q(cu - r + rim), q(cu + r - rim), cv - spoke, cv + spoke), texture))
    out.append(box(*span(axis, a0 + inset, a1 - inset, cu - spoke, cu + spoke, q(cv - r + rim), q(cv - spoke)), texture))
    out.append(box(*span(axis, a0 + inset, a1 - inset, cu - spoke, cu + spoke, q(cv + spoke), q(cv + r - rim)), texture))
    out += cyl(axis, cu, cv, max(0.7, r * 0.2), a0 - 0.25, a1 + 0.25, hub)
    return out


def dial(face, center, size, depth=0.75, texture="sp_gauge", body=BRASS):
    """A square-bodied instrument: gauge, lamp or window, its texture stretched over the front.
    center[axis] is where the front face sits; the body reaches `depth` into the machine, so put the
    front in front of the surface it is mounted on (never on the same plane)."""
    axis, sign = FACE_DIR[face]
    half = size / 2
    frm, to = [0.0] * 3, [0.0] * 3
    for j in range(3):
        if j == axis:
            if sign > 0:
                frm[j], to[j] = center[j] - depth, center[j]
            else:
                frm[j], to[j] = center[j], center[j] + depth
        else:
            frm[j], to[j] = center[j] - half, center[j] + half
    return box(frm, to, {"*": body, face: f"{texture}!"})


def pipe(frm, to, texture=COPPER):
    return box(frm, to, texture)


# ------------------------------------------------------------------ one-block machines

def coal_generator():
    """Coal-fired dynamo: firebox with a draft door, a copper-wound dynamo drum, flywheel and power terminals."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    # Firebox (front left) with brass belt and coal chute.
    m.append(box((1, 1, 0.75), (10, 10, 9.5), {"*": IRON_PLATE, "north": "sp_firebox!", "up": IRON}))
    m.append(box((0.5, 8.25, 0.25), (10.5, 9.25, 10), BRASS))
    m.append(box((1.75, 10, 1.75), (4.75, 12.5, 4.75), {"*": IRON, "up": "sp_hopper_inside"}))
    # Tall chimney with a brass crown.
    m += cyl("y", 3.5, 7.25, 1.5, 10, 15, IRON, "sp_hopper_inside")
    m += cyl("y", 3.5, 7.25, 2, 15, 16, BRASS, "sp_hopper_inside")
    m += cyl("y", 3.5, 7.25, 1.9, 12, 12.75, BRASS)
    # Main terminal on top: insulator, copper post and contact disc.
    m += cyl("y", 8, 8, 1.25, 10, 12, "sp_ceramic")
    m += cyl("y", 8, 8, 0.5, 12, 15.25, COPPER)
    m += cyl("y", 8, 8, 1.6, 15.25, 16, COPPER)
    # Dynamo drum along x at the back, with brass end bells and terminal posts.
    m += cyl("x", 5.25, 12.5, 3.1, 2.5, 13.5, "sp_coil", BRASS)
    m += cyl("x", 5.25, 12.5, 3.4, 1.5, 2.5, BRASS_PLATE)
    m += cyl("x", 5.25, 12.5, 3.4, 13.5, 14.5, BRASS_PLATE)
    for x in (5, 11):
        m.append(box((x, 8.25, 11.75), (x + 1.5, 10, 13.25), "sp_ceramic"))
        m.append(box((x + 0.5, 10, 12.25), (x + 1, 11, 12.75), COPPER))
    # Junction box on the right side, and the flywheel behind it.
    m.append(box((10, 5.25, 4.75), (15.5, 10.75, 10), BRASS_PLATE))
    m.append(box((15.5, 6.5, 6.5), (16, 9.5, 9.5), COPPER))
    m += wheel("x", 5.25, 13, 2.75, 15, 15.75, IRON, BRASS)
    m.append(box((14.5, 4.75, 12.5), (15, 5.75, 13.5), IRON))
    # Voltage gauge on a post.
    m.append(box((11.5, 1, 1.5), (12.5, 5.25, 2.5), IRON))
    m.append(dial("north", (12.75, 8, 4.25), 3.5))
    return m


def battery_box():
    """Leyden cabinet: brass-cornered wooden cabinet, four Leyden jars wired to a central terminal, output bus bar at the front."""
    m = [box((1, 0, 1), (15, 11, 15), {"*": "sp_wood", "up": BRASS_PLATE})]
    for x, z in ((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5)):
        m.append(box((x, 0, z), (x + 2, 11.5, z + 2), BRASS))
    # Output side: copper bus bar, terminals and a charge gauge.
    m.append(box((3, 2, 0.5), (13, 3.5, 1), COPPER))
    for x in (4, 10.5):
        m.append(box((x, 3.5, 0.25), (x + 1.5, 5, 1), "sp_ceramic"))
    m.append(dial("north", (8, 7.5, 0.5), 4))
    # Jars on top, each wired to the central terminal.
    for cx, cz in ((4.5, 4.5), (11.5, 4.5), (4.5, 11.5), (11.5, 11.5)):
        m += cyl("y", cx, cz, 2.25, 11, 15, "sp_leyden_jar!", BRASS)
        m.append(box((cx - 0.25, 15, cz - 0.25), (cx + 0.25, 15.5, cz + 0.25), COPPER))
    m.append(box((4.25, 15.25, 7.75), (11.75, 15.75, 8.25), COPPER))
    m.append(box((7.75, 15.25, 4.25), (8.25, 15.75, 7.75), COPPER))
    m.append(box((7.75, 15.25, 8.25), (8.25, 15.75, 11.75), COPPER))
    m.append(box((4.25, 15.25, 4.25), (4.75, 15.75, 7.75), COPPER))
    m.append(box((11.25, 15.25, 4.25), (11.75, 15.75, 7.75), COPPER))
    m.append(box((4.25, 15.25, 8.25), (4.75, 15.75, 11.75), COPPER))
    m.append(box((11.25, 15.25, 8.25), (11.75, 15.75, 11.75), COPPER))
    m += cyl("y", 8, 8, 1.1, 11, 14.5, "sp_ceramic")
    m += cyl("y", 8, 8, 1.5, 14.5, 16, COPPER)
    return m


def electric_furnace():
    """Electric kiln: firebrick body wrapped in copper heating coils, porthole door, chimney."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m.append(box((1, 1, 1), (15, 12, 15), {"*": "sp_firebrick", "up": IRON_PLATE}))
    m.append(box((2, 12, 2), (14, 13, 14), "sp_firebrick"))
    m.append(box((4, 13, 4), (12, 14, 12), "sp_firebrick"))
    m += cyl("y", 8, 8, 1.75, 14, 15.25, IRON, "sp_hopper_inside")
    m += cyl("y", 8, 8, 2.25, 15.25, 16, BRASS, "sp_hopper_inside")
    for y in (2, 5.25, 8.5):
        m.append(box((0.5, y, 0.5), (15.5, y + 1.75, 15.5), "sp_coil"))
    for y in (1.75, 10.25):
        m.append(box((0.25, y, 0.25), (15.75, y + 0.25, 15.75), BRASS))
    # Porthole door with hinges.
    m.append(box((3.5, 2.5, 0), (12.5, 11.5, 0.25), {"*": BRASS, "north": "sp_window!"}))
    m.append(box((2.5, 4, 0.1), (3.5, 5.5, 0.5), IRON))
    m.append(box((2.5, 8.5, 0.1), (3.5, 10, 0.5), IRON))
    # Insulators on the right side.
    for z in (4, 10.5):
        m.append(box((15.5, 11, z), (16, 12.5, z + 1.5), "sp_ceramic"))
    # Thermometer.
    m.append(dial("north", (13.75, 13.5, 1.5), 2.5))
    return m


def crusher():
    """Ore crusher: toothed jaw housing, big side gears, an open funnel hopper with iron bands."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m.append(box((2, 1, 1), (14, 9, 15), {"*": IRON_PLATE, "north": "sp_crusher_jaws!"}))
    for x in (1.5, 13):
        for z in (0.5, 13):
            m.append(box((x, 1, z), (x + 1.5, 9.5, z + 2.5), BRASS))
    # Drive gears on both sides.
    m += gear("x", 5.5, 8, 4.25, 0.5, 1.5)
    m += gear("x", 5.5, 8, 4.25, 14.5, 15.5)
    m.append(box((1.5, 5, 7.5), (2, 6, 8.5), IRON))
    m.append(box((14, 5, 7.5), (14.5, 6, 8.5), IRON))
    # Funnel hopper: widening wooden tiers, open on top, with iron bands.
    m.append(box((3, 9, 3), (13, 11, 13), {"*": IRON, "up": "sp_hopper_inside"}))
    m.append(box((2, 11, 2), (14, 13, 14), {"*": "sp_wood", "up": "sp_hopper_inside"}))
    m.append(box((1, 13, 1), (15, 15, 15), {"*": "sp_wood", "up": "sp_hopper_inside"}))
    for frm, to in (((0.5, 15, 0.5), (15.5, 16, 2)), ((0.5, 15, 14), (15.5, 16, 15.5)),
                    ((0.5, 15, 2), (2, 16, 14)), ((14, 15, 2), (15.5, 16, 14))):
        m.append(box(frm, to, IRON))
    m.append(box((1.5, 12.25, 1.5), (14.5, 12.75, 14.5), IRON))
    # Running lamp.
    m.append(dial("north", (12.5, 7.25, 0.5), 1.5, texture="sp_lamp"))
    return m


def arc_furnace_controller():
    """Arc furnace control panel: strapped casing with an arc porthole, meters and a knife switch."""
    m = [box((0, 0, 1), (16, 16, 16), "sp_arc_casing")]
    m.append(box((0, 0, 0.5), (16, 16, 1), {"*": IRON_PLATE, "north": BRASS_PLATE}))
    m.append(box((3.5, 5, 0), (12.5, 14, 0.5), {"*": BRASS, "north": "sp_arc_window!"}))
    m.append(dial("north", (2.25, 12.5, 0), 2.5))
    m.append(dial("north", (13.75, 12.5, 0), 2.5))
    # Knife switch.
    m.append(box((5.5, 1.25, 0), (10.5, 3.75, 0.5), "sp_ceramic"))
    m.append(box((7.5, 2, -1.5), (8.5, 3, 0), COPPER))
    m.append(box((6.5, 2.25, -1.75), (9.5, 2.75, -1.5), "sp_red_iron"))
    m.append(dial("north", (2.25, 2.5, 0), 1.5, texture="sp_lamp"))
    return m


def solar_panel():
    """Heliograph collector: brass column and gearbox with four mounting arms, holding a tilted panel of cells."""
    m = [box((2, 0, 2), (14, 1.5, 14), IRON_PLATE)]
    m += cyl("y", 8, 8, 4.5, 1.5, 2.25, BRASS)
    m += cyl("y", 8, 8, 1.75, 2.25, 7, BRASS)
    m.append(box((5, 6.5, 5), (11, 9, 11), IRON_PLATE))
    # Mounting arms out to each side, capped with copper terminals.
    for frm, to in (((0.75, 7.25, 7), (5, 8.75, 9)), ((11, 7.25, 7), (15.25, 8.75, 9)),
                    ((7, 7.25, 0.75), (9, 8.75, 5)), ((7, 7.25, 11), (9, 8.75, 15.25))):
        m.append(box(frm, to, BRASS))
    for frm, to in (((0, 6.75, 6.5), (0.75, 9.25, 9.5)), ((15.25, 6.75, 6.5), (16, 9.25, 9.5)),
                    ((6.5, 6.75, 0), (9.5, 9.25, 0.75)), ((6.5, 6.75, 15.25), (9.5, 9.25, 16))):
        m.append(box(frm, to, COPPER))
    m += gear("x", 8, 8, 2.25, 11, 11.75)
    tilt = ("x", 22.5, (8, 10, 8))
    m.append(box((1, 9.5, 1), (15, 10.5, 15), {"*": BRASS, "up": "sp_solar", "down": IRON}, rotation=tilt))
    m.append(box((0.5, 9.25, 0.5), (15.5, 9.5, 15.5), BRASS, rotation=tilt))
    return m


def steam_generator():
    """Steam engine: riveted copper boiler, steam dome, piston cylinder and a flywheel."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m += cyl("z", 6.5, 6.5, 5.25, 1, 15, "sp_tank", BRASS_PLATE)
    for z in (3, 12):
        m += cyl("z", 6.5, 6.5, 5.6, z, z + 1, BRASS)
    m.append(box((3.5, 2.5, 0.5), (9.5, 7.5, 1), {"*": IRON, "north": "sp_firebox!"}))
    # Steam dome, whistle and stack.
    m += cyl("y", 6.5, 7, 1.75, 11.5, 14, BRASS)
    m += cyl("y", 6.5, 7, 1, 14, 15, BRASS_PLATE)
    m.append(box((6.25, 15, 6.75), (6.75, 16, 7.25), BRASS))
    m += cyl("y", 3.5, 13, 1.25, 11, 16, IRON, "sp_hopper_inside")
    # Pressure gauge on top of the boiler.
    m.append(box((9.25, 11.5, 2.75), (10.25, 12.5, 3.75), IRON))
    m.append(dial("north", (9.75, 13.75, 2.5), 2.5))
    # Piston cylinder, rod and crank to the flywheel.
    m.append(box((12, 1, 3), (15, 5, 10), IRON_PLATE))
    m.append(box((13, 2.5, 10), (14, 3.5, 13.5), IRON))
    m.append(pipe((8.25, 11.5, 6.5), (13.75, 12.5, 7.5)))
    m.append(pipe((12.75, 5, 6.5), (13.75, 11.5, 7.5)))
    m += wheel("x", 7, 11.5, 4.25, 15.25, 16, IRON, BRASS)
    return m


def metal_press():
    """Screw press: anvil bed, two iron columns, brass crossbeam, threaded screw, handwheel, back plate and guard rail."""
    m = [box((1, 0, 2), (15, 3, 14), IRON_PLATE)]
    m.append(box((3, 3, 4), (13, 3.75, 12), IRON))
    for x in (1.5, 12):
        m.append(box((x, 3, 6), (x + 2.5, 13, 10), IRON_PLATE))
    m.append(box((1, 13, 5), (15, 15.25, 11), BRASS_PLATE))
    # Riveted back plate between the columns.
    m.append(box((4, 3, 14.25), (12, 13, 15.25), IRON_PLATE))
    m.append(box((4, 3, 10), (12, 4, 14.25), IRON))
    # Screw, ram and handwheel.
    m += cyl("y", 8, 8, 1.1, 6.5, 15.25, "sp_screw")
    m.append(box((4.5, 5, 5.5), (11.5, 6.5, 10.5), IRON_PLATE))
    m.append(box((5, 6.5, 6), (11, 7, 10), BRASS))
    m += wheel("y", 8, 8, 4.5, 15.25, 16, "sp_red_iron", BRASS)
    # Guard rail across the front with a terminal plate.
    m.append(box((1.5, 7.5, 0.75), (14.5, 8.5, 1.5), BRASS))
    m.append(box((1.5, 11, 0.75), (14.5, 11.5, 1.5), BRASS))
    for x in (1.5, 14):
        m.append(box((x, 7.5, 1.5), (x + 0.5, 11.5, 6), BRASS))
    m.append(box((6.5, 6.5, 0.25), (9.5, 9.5, 0.75), COPPER))
    # Side gear and running lamp.
    m += gear("x", 8, 8, 2, 14.5, 15.25)
    m.append(dial("north", (2.75, 12.25, 5.5), 1.5, texture="sp_lamp"))
    # Edge plates so cables at the sides meet the machine.
    m.append(box((0.5, 5, 6.5), (1.5, 11, 9.5), BRASS))
    m.append(box((15.25, 5, 6.5), (16, 11, 9.5), BRASS))
    return m


def wire_drawer():
    """Wire-drawing bench: iron-legged bench, draw die, wire running under a guide pulley to a copper-wound drum, motor and belt."""
    m = []
    for x in (1, 13.5):
        for z in (2, 12.5):
            m.append(box((x, 0, z), (x + 1.5, 8, z + 1.5), IRON))
    m.append(box((0.5, 8, 1.5), (15.5, 9.5, 14.5), "sp_wood"))
    m.append(box((1, 9.5, 5.5), (15, 10.25, 6.5), IRON))
    m.append(box((1, 9.5, 9.5), (15, 10.25, 10.5), IRON))
    # Draw die on the left, wire, winding drum on the right with a gear.
    m.append(box((1.5, 9.5, 5), (5, 13.5, 11), {"*": IRON_PLATE, "east": "sp_die!"}))
    m.append(box((5, 11.25, 7.75), (9.5, 11.75, 8.25), COPPER))
    m += cyl("z", 11.75, 12.25, 2.5, 3, 13, "sp_coil", BRASS)
    m += gear("z", 11.75, 12.25, 3, 1.75, 2.75)
    m.append(box((10.75, 9.5, 3.5), (12.75, 10.5, 12.5), IRON))
    # Guide pulley gantry over the wire, with a top terminal.
    for z in (3.25, 11.75):
        m.append(box((6.75, 10.25, z), (7.75, 15.25, z + 1), BRASS))
    m.append(box((6.25, 15.25, 3), (8.25, 16, 13), BRASS_PLATE))
    m += wheel("z", 7.25, 12.75, 1.25, 7.5, 8.5, BRASS, IRON, rim=0.5)
    # Motor under the bench with a belt up to the drum.
    m += cyl("x", 4, 8, 2.25, 4.5, 11, "sp_coil", BRASS_PLATE)
    m.append(box((11, 3.5, 7.5), (12, 4.5, 8.5), IRON))
    m.append(box((12, 2.5, 12.5), (13, 12, 13.25), "sp_belt"))
    m.append(box((6, 0, 6), (10, 1.75, 10), IRON_PLATE))
    m.append(box((7.25, 1.75, 7.25), (8.75, 2, 8.75), IRON))
    # Lamp and side plates for cables.
    m.append(dial("north", (3.25, 12.5, 4.5), 1.5, texture="sp_lamp"))
    m.append(box((0, 3, 6.5), (0.75, 7, 9.5), BRASS))
    m.append(box((15.25, 3, 6.5), (16, 7, 9.5), BRASS))
    return m


def circuit_assembler():
    """Difference engine: tall wooden cabinet with a punch-card reader, brass columns of number wheels, a gear train on top."""
    m = [box((0.5, 0, 1), (15.5, 9, 15), "sp_wood")]
    m.append(box((0, 8.25, 0.5), (16, 9, 15.5), BRASS))
    m.append(box((0, 0, 0.5), (16, 0.75, 15.5), BRASS))
    m.append(box((3, 2, 0.5), (13, 7, 1), {"*": BRASS, "north": "sp_punchcard!"}))
    m.append(dial("north", (14.25, 4.5, 0.5), 1.5, texture="sp_lamp"))
    for x in (4, 8, 12):
        m += cyl("y", x, 8, 1.75, 9, 13.75, "sp_digit_wheels", BRASS)
        m.append(box((x - 0.4, 13.75, 7.6), (x + 0.4, 14, 8.4), IRON))
    for x, z in ((1, 2), (14, 2), (1, 13), (14, 13)):
        m.append(box((x, 9, z), (x + 1, 14, z + 1), BRASS))
    m.append(box((0.5, 14, 1.5), (15.5, 14.75, 14.5), BRASS_PLATE))
    m += gear("y", 4.5, 8, 2.25, 14.75, 15.5, COPPER)
    m += gear("y", 8.25, 8, 1.75, 14.75, 15.5, BRASS)
    m += gear("y", 11.75, 8, 2.25, 14.75, 15.5, COPPER)
    m += wheel("x", 11.5, 8, 2.25, 15.5, 16, "sp_red_iron", BRASS)
    return m


def electric_pump():
    """Pump: riveted copper barrel with brass bands, iron head and piston crosshead, flanged outlets on every side."""
    m = [box((1, 0, 1), (15, 1.5, 15), IRON_PLATE)]
    m += cyl("y", 8, 8, 4.25, 1.5, 12, "sp_tank", IRON_PLATE)
    for y in (3, 9.5):
        m += cyl("y", 8, 8, 4.6, y, y + 1, BRASS)
    m += cyl("y", 8, 8, 3.25, 12, 13.5, IRON_PLATE)
    # Piston rods and crosshead on top, around the top outlet.
    for x in (4.75, 10.75):
        m.append(box((x, 13.5, 7.5), (x + 0.5, 15, 8.5), IRON))
    m.append(box((4.25, 15, 7.25), (11.75, 15.5, 8.75), IRON_PLATE))
    m += cyl("y", 8, 8, 1.5, 13.5, 15.25, COPPER)
    m += cyl("y", 8, 8, 2.5, 15.25, 16, BRASS)
    # Outlet pipes and flanges where pipes connect (the pump pushes out of its top and sides).
    for frm, to in (((0.75, 6.5, 6.5), (4, 9.5, 9.5)), ((12, 6.5, 6.5), (15.25, 9.5, 9.5)),
                    ((6.5, 6.5, 0.75), (9.5, 9.5, 4)), ((6.5, 6.5, 12), (9.5, 9.5, 15.25))):
        m.append(box(frm, to, COPPER))
    for frm, to in (((0, 5.5, 5.5), (0.75, 10.5, 10.5)), ((15.25, 5.5, 5.5), (16, 10.5, 10.5)),
                    ((5.5, 5.5, 0), (10.5, 10.5, 0.75)), ((5.5, 5.5, 15.25), (10.5, 10.5, 16))):
        m.append(box(frm, to, BRASS))
    # Valve on the front outlet.
    m.append(box((7.5, 9.5, 1.75), (8.5, 11.5, 2.75), IRON))
    m += wheel("y", 8, 2.25, 1.5, 11.5, 12, "sp_red_iron", BRASS)
    m.append(dial("north", (12, 12.25, 3.5), 2))
    return m


def fluid_tank():
    """Riveted cistern: round copper tank with brass bands, a domed lid, a sight glass and flanges on every side."""
    m = [box((1, 0, 1), (15, 1, 15), IRON_PLATE)]
    m += cyl("y", 8, 8, 6.5, 1, 14, "sp_tank", IRON_PLATE)
    for y in (2.5, 7.5, 12.5):
        m += cyl("y", 8, 8, 6.85, y, y + 0.75, BRASS)
    m += cyl("y", 8, 8, 5, 14, 15, BRASS_PLATE)
    m += cyl("y", 8, 8, 2.5, 15, 16, BRASS)
    for frm, to in (((0, 5.5, 5.5), (1.75, 10.5, 10.5)), ((14.25, 5.5, 5.5), (16, 10.5, 10.5)),
                    ((5.5, 5.5, 0), (10.5, 10.5, 1.75)), ((5.5, 5.5, 14.25), (10.5, 10.5, 16))):
        m.append(box(frm, to, BRASS))
    m.append(box((11.75, 2, 2.25), (13.25, 13, 2.75), {"*": BRASS, "north": "sp_sight_glass!"}))
    return m


def geothermal_generator():
    """Lava boiler (right block) feeding a copper turbine and dynamo (left block)."""
    m = [box((-16, 0, 0), (16, 1, 16), IRON_PLATE)]
    # Turbine body (master): iron base with gauges, copper turbine casing on top, exhaust stack.
    m.append(box((1, 1, 1), (15, 7, 15), IRON_PLATE))
    m.append(dial("north", (4, 4, 0.5), 3))
    m.append(dial("north", (12, 4, 0.5), 3))
    m.append(dial("north", (8, 4, 0.5), 1.5, texture="sp_lamp"))
    m += cyl("z", 8, 10.5, 3.75, 1.5, 14.5, COPPER, BRASS_PLATE)
    for z in (4, 11):
        m += cyl("z", 8, 10.5, 4.1, z, z + 1, BRASS)
    m += cyl("y", 12.5, 12.5, 1.25, 12, 16, IRON, "sp_hopper_inside")
    m += cyl("y", 12.5, 12.5, 1.75, 15, 16, BRASS)
    m += wheel("z", 8, 10.5, 2.25, 0.75, 1.5, "sp_red_iron", BRASS)
    # Lava boiler (right block, x -16..0).
    m += cyl("y", -8, 8, 6.25, 1, 13, "sp_tank", IRON_PLATE)
    for y in (3, 10):
        m += cyl("y", -8, 8, 6.6, y, y + 1, BRASS)
    m.append(box((-11, 4.5, 1.25), (-5, 10.5, 1.75), {"*": BRASS, "north": "sp_lava_window!"}))
    m += cyl("y", -8, 8, 4.5, 13, 14.25, BRASS_PLATE)
    m += cyl("y", -8, 8, 2.25, 14.25, 16, IRON_PLATE)
    # Heat pipe from the boiler into the turbine body.
    m.append(pipe((-2.25, 7.5, 7), (1, 9.5, 9)))
    m += cyl("x", 8.5, 8, 1.6, -2.5, -1.75, BRASS)
    m += cyl("x", 8.5, 8, 1.6, 0.5, 1, BRASS)
    return m


def wind_turbine():
    """Victorian windpump: brick footing and generator house, lattice tower, eight-vane wheel with a rim, tail vane."""
    m = [box((0, 0, 0), (16, 2, 16), "sp_firebrick")]
    m.append(box((2, 2, 2), (14, 11, 14), {"*": IRON_PLATE, "north": BRASS_PLATE}))
    m.append(box((5.5, 2, 1.5), (10.5, 9, 2), {"*": IRON, "north": "sp_wood"}))
    m.append(dial("north", (12, 8.5, 1.5), 2))
    m.append(box((1.5, 11, 1.5), (14.5, 12, 14.5), BRASS))
    # Lattice tower: four legs with braces.
    for x in (3.5, 11.5):
        for z in (3.5, 11.5):
            m.append(box((x, 12, z), (x + 1, 40, z + 1), IRON))
    for y in (19.5, 27.5, 35.5):
        m.append(box((3.5, y, 3.5), (12.5, y + 0.75, 4.5), IRON))
        m.append(box((3.5, y, 11.5), (12.5, y + 0.75, 12.5), IRON))
        m.append(box((3.5, y, 4.5), (4.5, y + 0.75, 11.5), IRON))
        m.append(box((11.5, y, 4.5), (12.5, y + 0.75, 11.5), IRON))
    for y0 in (12, 20.25, 28.25):
        mid = y0 + 3.6
        for z in (3.75, 11.75):  # X braces on the front and back faces
            for frm, to in (((2.5, mid - 0.25, z), (13.5, mid + 0.25, z + 0.5)),):
                m.append(box(frm, to, IRON, rotation=("z", 45, (8, mid, z + 0.25))))
                m.append(box(frm, to, IRON, rotation=("z", -45, (8, mid, z + 0.25))))
    # Gearbox, tail and wheel at the top.
    m.append(box((4.5, 40, 3), (11.5, 45, 13), {"*": IRON_PLATE, "up": BRASS_PLATE}))
    m.append(box((7.5, 41, 13), (8.5, 42, 18), IRON))
    m.append(box((7.75, 38.5, 17), (8.25, 47, 24), "sp_vane"))
    m += cyl("z", 8, 43, 1.5, 0.5, 3, BRASS, BRASS_PLATE)
    for angle in (0, 45):
        rotation = ("z", angle, (8, 43, 1.5)) if angle else None
        m.append(box((-2.5, 41.5, 1.25), (18.5, 44.5, 1.75), "sp_vane", rotation=rotation))
        m.append(box((6.5, 32.5, 1.25), (9.5, 53.5, 1.75), "sp_vane", rotation=rotation))
    m += wheel("z", 8, 43, 10.75, 1, 1.5, IRON, BRASS, spokes=False)
    return m


def alloy_smelter():
    """Brick furnace with a porthole door (master), copper hoppers above, iron crucible tower on the right with its power socket."""
    m = [box((-16, 0, 0), (16, 1, 16), IRON_PLATE)]
    # Furnace body (master).
    m.append(box((1, 1, 1), (15, 13, 15), {"*": "sp_firebrick", "up": IRON_PLATE}))
    for y in (2.5, 11):
        m.append(box((0.5, y, 0.5), (15.5, y + 1, 15.5), BRASS))
    m.append(box((3.5, 3.75, 0.25), (12.5, 10.75, 1), {"*": BRASS, "north": "sp_window!"}))
    m.append(dial("north", (13.5, 7.25, 0.5), 1.5, texture="sp_lamp"))
    # Two copper ingredient hoppers above the body (upper left block).
    for cx in (4.75, 11.25):
        m.append(box((cx - 3.25, 22, cx - 3.25), (cx + 3.25, 27, cx + 3.25), {"*": COPPER, "up": "sp_hopper_inside"}))
        m.append(box((cx - 3.5, 27, cx - 3.5), (cx + 3.5, 27.75, cx + 3.5), {"*": BRASS, "up": "sp_hopper_inside"}))
        m.append(box((cx - 2, 17, cx - 2), (cx + 2, 22, cx + 2), COPPER))
        m.append(box((cx - 1, 13, cx - 1), (cx + 1, 17, cx + 1), IRON))
    # Crucible base and tower (right blocks, x -16..0).
    m.append(box((-15, 1, 1), (-1, 10, 15), {"*": "sp_firebrick", "up": IRON_PLATE}))
    m.append(dial("north", (-12, 6, 0.5), 3))
    m.append(dial("north", (-4, 6, 0.5), 3))
    m += cyl("y", -8, 8, 5.5, 10, 25, IRON_PLATE)
    for y in (13, 21):
        m += cyl("y", -8, 8, 5.9, y, y + 1, BRASS)
    m += cyl("y", -8, 8, 4, 25, 26.5, BRASS_PLATE)
    m += cyl("y", -8, 8, 2, 26.5, 28, IRON)
    m += cyl("y", -8, 8, 1.1, 28, 32, IRON, "sp_hopper_inside")
    m += wheel("z", -8, 18, 2.5, 1.75, 2.5, "sp_red_iron", BRASS)
    # Pour trough into the furnace.
    m.append(pipe((-2.5, 17, 7), (3, 18.75, 9)))
    m.append(pipe((1.25, 13, 7), (3, 17, 9)))
    # Power socket: the only place cables connect (tools/large_machines.py POWER_PORTS).
    m.append(box((-16, 4, 4), (-15, 12, 12), BRASS_PLATE))
    m.append(box((-16.5, 6.5, 6.5), (-16, 9.5, 9.5), "power_port"))
    return m


# ------------------------------------------------------------------ ore processing

def pulverizer():
    """Ball mill: a banded grinding drum on iron cradles, feed hopper, bevel gear drive, motor and dust chute."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    for z in (2.5, 11.5):
        m.append(box((1.75, 1, z), (14.25, 3.5, z + 2), IRON))
    # Grinding drum along x with brass end plates, bands and bearing hubs.
    m += cyl("x", 7, 8, 5, 2, 14, IRON_PLATE, BRASS_PLATE)
    for x in (4.5, 10.5):
        m += cyl("x", 7, 8, 5.4, x, x + 1, BRASS)
    m += cyl("x", 7, 8, 1.25, 0.75, 2, BRASS)
    # Flywheel on the left, bevel gear drive on the right.
    m += wheel("x", 7, 8, 4, 0.25, 0.75, IRON, BRASS)
    m += gear("x", 7, 8, 3.75, 14.25, 15.25)
    # Motor on the back with a copper-wound body.
    m += cyl("z", 8, 7, 2.75, 12.5, 15.25, "sp_coil", BRASS)
    m += cyl("z", 8, 7, 1, 15.25, 16, COPPER)
    # Feed hopper on top.
    m.append(box((5, 11.5, 5), (11, 13, 11), {"*": IRON, "up": "sp_hopper_inside"}))
    m.append(box((3.5, 13, 3.5), (12.5, 15.25, 12.5), {"*": "sp_wood", "up": "sp_hopper_inside"}))
    for frm, to in (((3, 15.25, 3), (13, 16, 4)), ((3, 15.25, 12), (13, 16, 13)),
                    ((3, 15.25, 4), (4, 16, 12)), ((12, 15.25, 4), (13, 16, 12))):
        m.append(box(frm, to, IRON))
    # Front: instrument plate with a gauge and lamp, dust chute below.
    m.append(box((4.5, 4.5, 1.75), (11.5, 11, 3.5), BRASS_PLATE))
    m.append(dial("north", (8, 8.25, 1.25), 3.5))
    m.append(dial("north", (10.25, 5.5, 1.25), 1.25, texture="sp_lamp"))
    m.append(box((5.5, 1, 0.5), (10.5, 3.75, 2), {"*": IRON, "north": "sp_hopper_inside"}))
    return m


def ore_washer():
    """Washing tub: a banded wooden vat of water, agitator shaft and gear on a crossbeam, water wheel and inlet valve."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m.append(box((1, 1, 1), (15, 10, 15), {"*": "sp_wood", "up": "sp_water"}))
    for y in (3, 7.5):
        m.append(box((0.5, y, 0.5), (15.5, y + 0.75, 15.5), IRON))
    # Rim boards above the water line.
    for frm, to in (((1, 10, 1), (15, 11.5, 2)), ((1, 10, 14), (15, 11.5, 15)),
                    ((1, 10, 2), (2, 11.5, 14)), ((14, 10, 2), (15, 11.5, 14))):
        m.append(box(frm, to, "sp_wood"))
    # Crossbeam with posts, agitator shaft and drive gear.
    for x in (1.25, 13.25):
        m.append(box((x, 11.5, 7), (x + 1.5, 13, 9), IRON))
    m.append(box((1, 13, 7), (15, 14.25, 9), BRASS_PLATE))
    m += cyl("y", 8, 8, 0.75, 8, 13, IRON)
    m += gear("y", 8, 8, 2.75, 14.25, 15)
    m += cyl("y", 8, 8, 0.6, 15, 16, BRASS)
    # Water wheel on the right side.
    m += wheel("x", 6, 8, 4.5, 15.25, 16, IRON, BRASS)
    # Water inlet from the back: copper pipe, brass flange and a red valve wheel on top.
    m.append(pipe((7.25, 8.5, 14.5), (8.75, 10, 16)))
    m.append(box((6.75, 8, 15.5), (9.25, 10.5, 16), BRASS))
    m.append(pipe((7.5, 10, 14.75), (8.5, 12.25, 15.75)))
    m += wheel("y", 8, 15.25, 1.75, 12.25, 12.75)
    # Front: sight glass and lamp, washed-ore chute.
    m.append(dial("north", (6, 6, 0.5), 3.5, texture="sp_sight_glass"))
    m.append(dial("north", (11.5, 6, 0.5), 1.25, texture="sp_lamp"))
    m.append(box((9.5, 1, 0.25), (13.5, 3, 1.25), {"*": IRON, "north": "sp_hopper_inside"}))
    return m


def sieve():
    """Shaker sieve: a tilted brass mesh tray in an iron frame, feed hopper above, catch bin below, eccentric drive."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    for x in (1, 13.5):
        for z in (1, 13.5):
            m.append(box((x, 1, z), (x + 1.5, 12, z + 1.5), IRON))
    for frm, to in (((1, 11, 2.5), (2.5, 12, 13.5)), ((13.5, 11, 2.5), (15, 12, 13.5)),
                    ((2.5, 11, 1), (13.5, 12, 2.5)), ((2.5, 11, 13.5), (13.5, 12, 15))):
        m.append(box(frm, to, BRASS))
    # Catch bin and the tilted mesh tray.
    m.append(box((2.5, 1, 2.5), (13.5, 5, 13.5), {"*": "sp_wood", "up": "sp_hopper_inside"}))
    tilt = ("x", 22.5, (8, 8, 8))
    m.append(box((2.75, 7.5, 2.75), (13.25, 8, 13.25), {"*": BRASS, "up": "sp_mesh", "down": "sp_mesh"}, rotation=tilt))
    # Feed hopper.
    m.append(box((5, 12, 5), (11, 13.5, 11), {"*": IRON, "up": "sp_hopper_inside"}))
    m.append(box((3.5, 13.5, 3.5), (12.5, 16, 12.5), {"*": "sp_wood", "up": "sp_hopper_inside"}))
    m.append(box((3.25, 14.75, 3.25), (12.75, 15.25, 12.75), IRON))
    # Motor on the left, eccentric wheel on the right.
    m += cyl("x", 7, 8, 2.5, 0.5, 2.5, "sp_coil", BRASS)
    m += wheel("x", 7, 8, 3.5, 15, 15.75, IRON, BRASS)
    # Front instrument plate and chute; back brace.
    m.append(box((5.5, 5, 1.25), (10.5, 10.5, 2.5), BRASS_PLATE))
    m.append(dial("north", (8, 8, 0.75), 3))
    m.append(dial("north", (8, 5.75, 0.75), 1, texture="sp_lamp"))
    m.append(box((5.5, 1, 0.75), (10.5, 4, 2.5), {"*": IRON, "north": "sp_hopper_inside"}))
    m.append(box((5, 3, 13.5), (11, 10, 15.25), IRON_PLATE))
    return m


def sawmill():
    """Steam sawmill: a wooden bench with an iron table, a toothed circular saw under a brass hood, fence rails and a flywheel."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m.append(box((1, 1, 1), (15, 9, 15), "sp_wood"))
    for y in (2, 7):
        m.append(box((0.5, y, 0.5), (15.5, y + 0.75, 15.5), IRON))
    m.append(box((0.5, 9, 0.5), (15.5, 10, 15.5), {"*": IRON, "up": IRON_PLATE}))
    # Saw blade rising through the table, under a hood held from the back.
    m += gear("x", 10, 8, 4.75, 7.75, 8.25, texture="sp_saw", hub=BRASS)
    m.append(box((7, 13.5, 4.5), (9, 15.5, 11.5), BRASS_PLATE))
    m.append(box((7.5, 10, 12.5), (8.5, 15, 13.5), IRON))
    m.append(box((7.5, 14.5, 11.5), (8.5, 15, 12.5), IRON))
    # Fence rails.
    m.append(box((2, 10, 1), (3, 11, 15), BRASS))
    m.append(box((13, 10, 1), (14, 11, 15), BRASS))
    # Flywheel on the right, motor on the left.
    m += wheel("x", 5, 8, 3.5, 15.25, 16, IRON, BRASS)
    m += cyl("x", 5, 8, 2.25, 0.25, 1, "sp_coil", BRASS)
    # Front: gauge, lamp and sawdust chute.
    m.append(dial("north", (4, 5.5, 0.5), 3))
    m.append(dial("north", (12, 5.5, 0.5), 1.25, texture="sp_lamp"))
    m.append(box((6, 1.5, 0.25), (10, 4, 1.25), {"*": IRON, "north": "sp_hopper_inside"}))
    return m


MODELS = {
    "coal_generator": coal_generator(),
    "battery_box": battery_box(),
    "electric_furnace": electric_furnace(),
    "crusher": crusher(),
    "arc_furnace_controller": arc_furnace_controller(),
    "solar_panel": solar_panel(),
    "steam_generator": steam_generator(),
    "alloy_smelter": alloy_smelter(),
    "metal_press": metal_press(),
    "wire_drawer": wire_drawer(),
    "circuit_assembler": circuit_assembler(),
    "pulverizer": pulverizer(),
    "ore_washer": ore_washer(),
    "sieve": sieve(),
    "sawmill": sawmill(),
    "geothermal_generator": geothermal_generator(),
    "wind_turbine": wind_turbine(),
    "electric_pump": electric_pump(),
    "fluid_tank": fluid_tank(),
}
