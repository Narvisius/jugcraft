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
# Blocks with a "turning" block state.
STATES_TURNING = {"iron_shaft", "brass_gearbox", "hand_crank", "belt_pulley", "electric_motor", "conveyor",
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
    rotor = cyl("z", 8, 8, 1, 1.5, 7, IRON)
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
    m.append(box((6, 1, 13.25), (10, 2.75, 14.25), IRON_PLATE))
    # Flywheel and the axle out of the back spin while the engine runs.
    rotor = wheel("z", 8, 8, 6, 13, 14.5, IRON, BRASS)
    rotor += cyl("z", 8, 8, 1.25, 11, 16, IRON)
    return m, rotor


def dynamo():
    """A copper-wound coil on an iron frame, with an axle through it for shafts at the front and back, and brass
    terminals where cables meet it."""
    m = [box((1, 0, 1), (15, 1.5, 15), IRON_PLATE)]
    m += cyl("z", 8, 8.5, 5.25, 2, 14, "sp_coil", BRASS)
    for z in (2, 13.25):
        m.append(box((2, 1.5, z), (14, 14.5, z + 0.75), IRON))
    m += cyl("z", 8, 8.5, 1.25, 0, 16, IRON)
    # Terminals in the middle of the other faces so cables meet the dynamo.
    m.append(box((0.25, 6, 6.5), (2, 10, 9.5), {"*": BRASS, "west": BRASS_PLATE}))
    m.append(box((14, 6, 6.5), (15.75, 10, 9.5), {"*": BRASS, "east": BRASS_PLATE}))
    m.append(box((6.5, 13.75, 6.5), (9.5, 15.75, 9.5), {"*": BRASS, "up": BRASS_PLATE}))
    m.append(box((5, 1.5, 5), (11, 3, 11), COPPER))
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
    """A copper-wound motor on iron feet; its shaft comes out of the front (north), with brass terminals where cables
    meet it."""
    m = [box((2, 0, 3), (14, 1.5, 15), IRON_PLATE)]
    m += cyl("z", 8, 8.5, 5.5, 3.5, 14.5, "sp_coil", BRASS_PLATE)
    for z in (3, 14):
        m += cyl("z", 8, 8.5, 5.9, z, z + 1, IRON)
    m.append(box((0.25, 6, 7), (2.5, 11, 11), {"*": BRASS, "west": BRASS_PLATE}))
    m.append(box((13.5, 6, 7), (15.75, 11, 11), {"*": BRASS, "east": BRASS_PLATE}))
    m.append(box((6, 14, 7), (10, 15.75, 11), {"*": BRASS, "up": BRASS_PLATE}))
    m.append(box((6, 6.5, 14.5), (10, 10.5, 15.75), {"*": BRASS, "south": BRASS_PLATE}))
    # The output shaft spins, with a coupling on it.
    rotor = [box((6, 6.5, 0), (10, 10.5, 3.5), "iron_shaft"), box((5.5, 6, 1), (10.5, 11, 2.5), BRASS)]
    return m, rotor


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


def brass_gearbox_parts():
    return brass_gearbox(), []


def dynamo_parts():
    return dynamo(), []


PARTS = {"iron_shaft": iron_shaft(), "brass_gearbox": brass_gearbox_parts(), "hand_crank": hand_crank(),
         "steam_engine": steam_engine(), "dynamo": dynamo_parts(), "belt_pulley": belt_pulley(),
         "electric_motor": electric_motor(), "conveyor": (conveyor(), []),
         "conveyor_splitter": (conveyor_splitter(), [])}
# Full models (rotor standing still) and the static parts shown while the rotor spins.
MODELS = {block: static + rotor for block, (static, rotor) in PARTS.items()}
STATIC = {block: static for block, (static, rotor) in PARTS.items()}
# Rotor axis (in the north-facing model), the point it turns about (pixels, on the other two axes), the block state
# property that sets it spinning, and its speed in degrees per tick.
ROTORS = {
    "iron_shaft": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 9},
    "belt_pulley": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 9},
    "electric_motor": {"axis": "z", "center": (8, 8.5), "property": "turning", "speed": 12},
    "hand_crank": {"axis": "z", "center": (8, 8), "property": "turning", "speed": 6},
    "steam_engine": {"axis": "z", "center": (8, 8), "property": "lit", "speed": 9},
}
for _block in ROTORS:
    ROTORS[_block]["elements"] = PARTS[_block][1]
