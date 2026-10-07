"""Models for kinetic-power blocks (style-independent), built from the steampunk helpers.

Each model faces north. Parts that spin are listed separately as rotors: the block model shows them
standing still, and while the block turns (or the steam engine runs) its "_active" model leaves them
out and client/KineticRotorRenderer draws them spinning about the rotor axis. generate_material_data
exports the rotors to assets/jugcraft/kinetic_rotors.json.
"""
from steampunk_models import BRASS, BRASS_PLATE, COPPER, IRON, IRON_PLATE, box, cyl, dial, wheel

# Texture swapped for its animated version while the block turns (the gearbox has no rotor: its face gears are
# animated), and the lit steam engine's glow.
TURNING = {"brass_gearbox": ("brass_gearbox", "brass_gearbox_turning"),
           "conveyor": ("conveyor_belt", "conveyor_belt_moving"),
           "conveyor_splitter": ("conveyor_belt", "conveyor_belt_moving")}
LIT = {"steam_engine": ("sp_firebox", "sp_firebox_on")}
# How far (pixels) a spinning rotor stays off the faces of the still block it meets in the "_active" model: the two are
# drawn separately, so a shared plane would flicker (docs/ART_DIRECTION.md, Rules for everything).
ROTOR_GAP = 0.1
# Blocks with a "turning" block state.
STATES_TURNING = {"iron_shaft", "brass_gearbox", "hand_crank", "belt_pulley", "electric_motor", "magnet_motor", "flywheel", "solar_tracker", "heliostat", "conveyor",
                  "conveyor_splitter"}


def iron_shaft():
    """A 4-pixel iron shaft along z with brass couplings at both ends and a brass collar in the middle, so its turning
    shows. All of it spins."""
    rotor = [box((6, 6, 0), (10, 10, 16), "iron_shaft")]
    for z0, z1 in ((0, 1.5), (7, 9), (14.5, 16)):
        rotor.append(box((5.5, 5.5, z0), (10.5, 10.5, z1), BRASS))
    rotor.append(box((7.5, 10.5, 7.25), (8.5, 11, 8.75), BRASS_PLATE))
    return [], rotor


def brass_gearbox():
    """A brass-cased gearbox with a turning gear on every face and iron corner posts."""
    m = [box((0.5, 0.5, 0.5), (15.5, 15.5, 15.5), "brass_gearbox")]
    for x in (0, 14.5):
        for z in (0, 14.5):
            m.append(box((x, 0, z), (x + 1.5, 16, z + 1.5), IRON))
    return m


def hand_crank():
    """A hub plate against the driven block (north); the axle, brass arm and wooden handle spin."""
    static = cyl("z", 8, 8, 3, 0, 1.5, IRON_PLATE, BRASS)
    # The axle starts ROTOR_GAP inside the hub plate: its end is hidden in the plate, and no plane of the spinning rotor
    # is shared with the still block (nor does a slit open between them).
    rotor = cyl("z", 8, 8, 1, 1.5 - ROTOR_GAP, 7, IRON)
    rotor.append(box((7, 7, 7), (9, 13.5, 8.5), BRASS))
    rotor += cyl("z", 8, 12.5, 0.9, 8.5, 13, "sp_wood")
    return static, rotor


def steam_engine():
    """A firebrick firebox under a riveted boiler with a chimney; a piston drives the flywheel at the back, whose
    axle comes out at the middle of the back face for a shaft."""
    m = [box((0, 0, 0), (16, 1, 16), IRON_PLATE)]
    m.append(box((1, 1, 1), (15, 6, 11), {"*": "sp_firebrick", "north": "sp_firebrick"}))
    m.append(box((5, 1.5, 0.5), (11, 5.5, 1.25), {"*": IRON, "north": "sp_firebox!"}))
    m += cyl("x", 9.5, 6, 4, 1.5, 14.5, IRON_PLATE, BRASS_PLATE)
    for x in (4, 12):
        m += cyl("x", 9.5, 6, 4.3, x, x + 0.75, BRASS)
    m += cyl("y", 12, 4, 1.25, 13, 16, IRON, "sp_hopper_inside")
    m.append(dial("north", (3.5, 10, 2.25), 2))
    # Piston cylinder and rod back to the flywheel crank.
    m += cyl("z", 3.5, 3.5, 1.75, 6, 12, BRASS, IRON)
    m.append(box((3, 3, 12), (4, 4, 14), IRON))
    m.append(box((6, 1, 13.25), (10, 2.75 - ROTOR_GAP, 14.25), IRON_PLATE))  # under the flywheel rim, not flush
    # Flywheel and the axle out of the back spin while the engine runs.
    rotor = wheel("z", 8, 8, 6, 13, 14.5, IRON, BRASS)
    rotor += cyl("z", 8, 8, 1.25, 11, 16, IRON)
    return m, rotor


def dynamo():
    """In the electric look of the power gear: a graphite generator housing with glowing bands on a trim base, an axle
    through it for shafts at the front and back, a vented top and power ports where cables meet it."""
    m = [box((1, 0, 1), (15, 1.5, 15), "el_frame")]
    m += cyl("z", 8, 8.5, 5.25, 2, 14, "el_casing", "el_frame")
    for z in (4.5, 11):
        m += cyl("z", 8, 8.5, 5.5, z, z + 0.5, "el_glow")
    for z in (2, 13.25):
        m.append(box((2, 1.5, z), (14, 14.5, z + 0.75), "el_frame"))
    m += cyl("z", 8, 8.5, 1.25, 0, 16, IRON)
    # Ports in the middle of the other faces so cables meet the dynamo.
    m.append(box((0.25, 6, 6.5), (2, 10, 9.5), {"*": "el_frame", "west": "el_port!"}))
    m.append(box((14, 6, 6.5), (15.75, 10, 9.5), {"*": "el_frame", "east": "el_port!"}))
    m.append(box((6.5, 13.75, 6.5), (9.5, 15.75, 9.5), {"*": "el_frame", "up": "el_vent!"}))
    m.append(box((5, 1.5, 5), (11, 3, 11), "el_casing"))
    return m


def magnet_dynamo():
    """The dynamo's housing and axle with rare-earth magnets: a ring of nickel-plated magnet segments round the middle
    between cyan bands (the higher-tech colour), on the same base with the same ports."""
    m = [box((1, 0, 1), (15, 1.5, 15), "el_frame")]
    m += cyl("z", 8, 8.5, 5.25, 2, 14, "el_casing", "el_frame")
    for z in (4, 11.5):
        m += cyl("z", 8, 8.5, 5.5, z, z + 0.5, "el_glow_cyan")
    m += cyl("z", 8, 8.5, 5.75, 6, 10, "el_magnet")
    for z in (2, 13.25):
        m.append(box((2, 1.5, z), (14, 14.5, z + 0.75), "el_frame"))
    m += cyl("z", 8, 8.5, 1.25, 0, 16, IRON)
    m.append(box((0.25, 6, 6.5), (2, 10, 9.5), {"*": "el_frame", "west": "el_port!"}))
    m.append(box((14, 6, 6.5), (15.75, 10, 9.5), {"*": "el_frame", "east": "el_port!"}))
    m.append(box((6.5, 14.25, 6.5), (9.5, 15.75, 9.5), {"*": "el_frame", "up": "el_vent!"}))
    m.append(box((5, 1.5, 5), (11, 3, 11), "el_casing"))
    return m


def belt_pulley():
    """A shaft along z carrying a grooved wooden wheel with brass rims; the belt (drawn by the client) runs in the
    groove."""
    rotor = [box((6, 6, 0), (10, 10, 16), "iron_shaft")]
    rotor += cyl("z", 8, 8, 5.75, 6, 10, "sp_wood", BRASS_PLATE)
    for z0 in (5, 10):
        rotor += cyl("z", 8, 8, 6.75, z0, z0 + 1, BRASS)
    rotor += cyl("z", 8, 8, 2, 4.5, 11.5, IRON, BRASS_PLATE)
    # Spokes painted on the wheel's faces, so its turning shows.
    for z0, z1 in ((4.75, 5), (11, 11.25)):
        rotor.append(box((7.5, 2.5, z0), (8.5, 13.5, z1), IRON))
    return [], rotor


def electric_motor():
    """In the electric look of the power gear: a graphite motor housing with glowing bands and end rings on trim feet;
    its shaft comes out of the front (north), with power ports where cables meet it."""
    m = [box((2, 0, 3), (14, 1.5, 15), "el_frame")]
    m += cyl("z", 8, 8.5, 5.5, 3.5, 14.5, "el_casing", "el_frame")
    for z in (3, 14):
        m += cyl("z", 8, 8.5, 5.9, z, z + 1, "el_frame")
    for z in (6.5, 10.5):
        m += cyl("z", 8, 8.5, 5.75, z, z + 0.5, "el_glow")
    m.append(box((0.25, 6, 7), (2.5, 11, 11), {"*": "el_frame", "west": "el_port!"}))
    m.append(box((13.5, 6, 7), (15.75, 11, 11), {"*": "el_frame", "east": "el_port!"}))
    m.append(box((6, 14, 7), (10, 15.75, 11), {"*": "el_frame", "up": "el_vent!"}))
    m.append(box((6, 6.5, 14.5), (10, 10.5, 15.75), {"*": "el_frame", "south": "el_port!"}))
    # The output shaft spins, with a coupling on it; it ends ROTOR_GAP inside the housing, off its front ring.
    rotor = [box((6, 6.5, 0), (10, 10.5, 3.5 + ROTOR_GAP), "iron_shaft"), box((5.5, 6, 1), (10.5, 11, 2.5), "el_frame")]
    return m, rotor


def magnet_motor():
    """The electric motor with rare-earth magnets: a ring of nickel-plated magnet segments round its middle between
    cyan bands, on the same feet with the same ports and output shaft."""
    m = [box((2, 0, 3), (14, 1.5, 15), "el_frame")]
    m += cyl("z", 8, 8.5, 5.5, 3.5, 14.5, "el_casing", "el_frame")
    for z in (3, 14):
        m += cyl("z", 8, 8.5, 5.9, z, z + 1, "el_frame")
    for z in (5.5, 11.5):
        m += cyl("z", 8, 8.5, 5.75, z, z + 0.5, "el_glow_cyan")
    m += cyl("z", 8, 8.5, 6, 7, 10.5, "el_magnet")
    m.append(box((0.25, 6, 7), (2.5, 11, 11), {"*": "el_frame", "west": "el_port!"}))
    m.append(box((13.5, 6, 7), (15.75, 11, 11), {"*": "el_frame", "east": "el_port!"}))
    m.append(box((6, 14.5, 4), (10, 15.75, 6.5), {"*": "el_frame", "up": "el_vent!"}))
    m.append(box((6, 6.5, 14.5), (10, 10.5, 15.75), {"*": "el_frame", "south": "el_port!"}))
    rotor = [box((6, 6.5, 0), (10, 10.5, 3.5 + ROTOR_GAP), "iron_shaft"), box((5.5, 6, 1), (10.5, 11, 2.5), "el_frame")]
    return m, rotor


def flywheel():
    """A heavy steel flywheel between two bearing pedestals on a riveted base: a thick rim with spokes and a hub on a
    shaft along z, coming out of the front (north) to drive what it faces and out of the back where the drive comes
    in. The wheel and shaft spin."""
    m = [box((0.5, 0, 1), (15.5, 1.5, 15), "dp_gunmetal")]
    for z0 in (1.5, 12.5):
        m.append(box((5.5, 1.5, z0), (10.5, 5, z0 + 2), "dp_gunmetal"))
        m.append(box((6.5, 5, z0), (9.5, 10, z0 + 2), BRASS_PLATE))
    # The shaft is ROTOR_GAP slimmer on each side than the bearing blocks it runs through, so their sides never share
    # a plane with the spinning shaft's.
    g = ROTOR_GAP
    rotor = [box((6.5 + g, 6.5 + g, 0), (9.5 - g, 9.5 - g, 16), "iron_shaft")]
    rotor += cyl("z", 8, 8, 7.25, 5.5, 10.5, IRON_PLATE, IRON)
    rotor += cyl("z", 8, 8, 2, 4.5, 11.5, BRASS, BRASS_PLATE)
    for z0, z1 in ((5.25, 5.5), (10.5, 10.75)):
        rotor.append(box((7.25, 1.5, z0), (8.75, 14.5, z1), BRASS))
        rotor.append(box((1.5, 7.25, z0), (14.5, 8.75, z1), BRASS))
    return m, rotor


def solar_tracker():
    """A solar panel on a motorised mount (the electric look): a graphite post with glowing trim and a cable port on
    a trim plinth, carrying a 14-pixel panel of cells on a pivot running north to south. The panel (with its pivot
    and frame) tilts with the sun ("sun" rotor)."""
    m = [box((3, 0, 3), (13, 1.5, 13), "el_frame")]
    m.append(box((6, 1.5, 6), (10, 7, 10), {"*": "el_casing", "north": "el_port!"}))
    m.append(box((5.5, 4, 5.5), (10.5, 4.5, 10.5), "el_glow"))
    m.append(box((7, 7, 2), (9, 7.5, 14), "el_frame"))
    rotor = [box((1, 8.5, 1), (15, 9.5, 15), {"*": "el_frame", "up": "el_solar"}),
             # The pivot sinks into the mount bar and is ROTOR_GAP narrower on each side: no shared plane, no slit.
             box((7 + ROTOR_GAP, 7.5 - ROTOR_GAP, 1.5), (9 - ROTOR_GAP, 8.5, 14.5), "iron_shaft")]
    return m, rotor


def heliostat():
    """A heliostat: a slim post on a small footing with a square mirror on a pivot that tilts after the sun ("sun"
    rotor, half as far as a tracker, as a mirror aiming at a fixed receiver turns half the sun's angle)."""
    m = [box((5, 0, 5), (11, 1, 11), "el_frame"), box((7, 1, 7), (9, 9, 9), "el_casing")]
    rotor = [box((2, 9, 2), (14, 10, 14), {"*": "el_frame", "up": "el_mirror"}),
             # Inside the post's sides; its top meets the mirror's underside (both turn), so no slit opens under the
             # mirror. Its top shares the post's top plane only where the mirror covers both.
             box((7 + ROTOR_GAP, 8, 2.5), (9 - ROTOR_GAP, 9, 13.5), "iron_shaft")]
    return m, rotor


def solar_receiver():
    """The solar receiver: a block-sized absorber, its four sides walls of orange-hot tubes, under a graphite cap with
    a vent and on a graphite base with power and water ports."""
    m = [box((0.5, 0, 0.5), (15.5, 2, 15.5), {"*": "el_frame", "north": "el_port!"})]
    m.append(box((1.5, 2, 1.5), (14.5, 13, 14.5), {"*": "el_receiver!", "up": "el_casing", "down": "el_casing"}))
    m.append(box((0.5, 13, 0.5), (15.5, 16, 15.5), {"*": "el_frame", "up": "el_vent!"}))
    return m


def conveyor():
    """A low conveyor: a rubber belt (items ride on it at 5 pixels, heading north) between riveted iron side rails,
    over rollers at both ends, with brass drive hubs on the rails where a shaft or motor meets it."""
    m = []
    for x0, x1 in ((0.5, 2), (14, 15.5)):
        m.append(box((x0, 0, 0), (x1, 6, 16), {"*": IRON_PLATE, "up": IRON}))
    m.append(box((2, 1.5, 0.25), (14, 5, 15.75), {"*": "belt", "up": "conveyor_belt"}))
    for z in (2, 14):
        m += cyl("x", 3.25, z, 1.75, 2, 14, IRON, BRASS)
    m.append(box((2, 0, 6), (14, 1.5, 10), IRON_PLATE))
    for x0, x1 in ((0, 0.5), (15.5, 16)):
        m += cyl("x", 3.25, 8, 1.5, x0, x1, BRASS_PLATE, BRASS)
    return m


def conveyor_splitter():
    """A conveyor under a brass arch with a copper flap at the front that sends items left, straight on and right in
    turn, and a selector dial on top."""
    m = conveyor()
    for x0, x1 in ((0.5, 2), (14, 15.5)):
        m.append(box((x0, 6, 6.5), (x1, 11, 9.5), BRASS))
    m.append(box((0.5, 11, 6), (15.5, 12.5, 10), BRASS_PLATE))
    m.append(dial("up", (8, 12.5, 8), 3, depth=0.5))
    m.append(box((7.5, 5, 1), (8.5, 9, 6), COPPER))
    m.append(box((7.25, 9, 3), (8.75, 11, 4.5), BRASS))
    return m


# How far the slope's tilted belt and rails sit above the block's centre line (pixels): without it the belt's top
# met the block's low edge at 1 pixel, below the flat conveyor's 5, and the two did not join.
SLOPE_LIFT = 4


def conveyor_slope(ascending):
    """A conveyor ramp, one block up (ascending: rising towards the front, north) or down: the belt and its side rails
    tilted 45 degrees across the block's diagonal, on iron legs. Items ride it the same way as a flat conveyor."""
    angle = 45 if ascending else -45
    # Raised by SLOPE_LIFT so the belt's top meets the flat conveyors' (5 pixels up) at both ends: 5 at the low edge,
    # 21 (one block plus 5) at the high edge. Items ride at the same heights (ConveyorBlockEntity.riseAt).
    lift = SLOPE_LIFT
    tilt = ("x", angle, (8, 8 + lift, 8), True)
    m = [box((2, 6.5 + lift, 0.25), (14, 8.5 + lift, 15.75), {"*": "belt", "up": "conveyor_belt"}, rotation=tilt)]
    for x0, x1 in ((0.5, 2), (14, 15.5)):
        m.append(box((x0, 6.25 + lift, 0), (x1, 9.5 + lift, 16), {"*": IRON_PLATE, "up": IRON}, rotation=tilt))
    # Legs up to the underside of the rails: tall at the high end, short in the middle.
    high, mid = (1, 3) if ascending else (13, 15), (7, 9)
    for x0, x1 in ((0.5, 2), (14, 15.5)):
        m.append(box((x0, 0, high[0]), (x1, 15, high[1]), IRON))
        m.append(box((x0, 0, mid[0]), (x1, 8.5, mid[1]), IRON))
    m.append(box((2, 0, high[0]), (14, 1.5, high[1]), IRON_PLATE))
    return m


def brass_gearbox_parts():
    return brass_gearbox(), []


def dynamo_parts():
    return dynamo(), []


PARTS = {"iron_shaft": iron_shaft(), "brass_gearbox": brass_gearbox_parts(), "hand_crank": hand_crank(),
         "steam_engine": steam_engine(), "dynamo": dynamo_parts(), "belt_pulley": belt_pulley(),
         "electric_motor": electric_motor(), "magnet_dynamo": (magnet_dynamo(), []), "magnet_motor": magnet_motor(),
         "flywheel": flywheel(),
         "solar_tracker": solar_tracker(), "heliostat": heliostat(), "solar_receiver": (solar_receiver(), []),
         "conveyor": (conveyor(), []),
         "conveyor_splitter": (conveyor_splitter(), [])}
# Conveyor slopes: one block (conveyor_slope, logistics/ConveyorSlopeBlock) with an ascending and a descending model.
SLOPES = {"conveyor_slope": conveyor_slope(True), "conveyor_slope_down": conveyor_slope(False)}
# Full models (rotor standing still) and the static parts shown while the rotor spins.
MODELS = {block: static + rotor for block, (static, rotor) in PARTS.items()}
STATIC = {block: static for block, (static, rotor) in PARTS.items()}
# Rotor axis (in the north-facing model), the point it turns about (pixels, on the other two axes), the block state
# property that sets it spinning, and its speed in degrees per tick.
ROTORS = {
    "iron_shaft": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 9},
    "belt_pulley": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 9},
    "electric_motor": {"axis": "z", "center": (8, 8.5), "property": "turning", "speed": 12},
    "magnet_motor": {"axis": "z", "center": (8, 8.5), "property": "turning", "speed": 18},
    "flywheel": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 4},
    # Sun rotors tilt with the time of day, "speed" degrees each way (client/KineticRotors).
    "solar_tracker": {"axis": "z", "center": (8, 9), "property": "turning", "speed": 60, "mode": "sun"},
    "heliostat": {"axis": "z", "center": (8, 9.5), "property": "turning", "speed": 30, "mode": "sun"},
    "hand_crank": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 6},
    "steam_engine": {"axis": "z", "center": (8, 8), "property": "lit", "speed": 9},
}
for _block in ROTORS:
    ROTORS[_block]["elements"] = PARTS[_block][1]
