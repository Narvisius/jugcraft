"""Models for kinetic-power blocks (style-independent), built from the steampunk helpers.

Each model faces north. Blocks with a turning look swap one texture for an animated one (TURNING);
the steam engine's fire door glows while it runs (LIT).
"""
from steampunk_models import BRASS, BRASS_PLATE, COPPER, IRON, IRON_PLATE, box, cyl, dial, wheel

# Texture swapped for its animated version while the block turns, and the lit steam engine's glow.
TURNING = {"iron_shaft": ("iron_shaft", "iron_shaft_turning"),
           "brass_gearbox": ("brass_gearbox", "brass_gearbox_turning")}
LIT = {"steam_engine": ("sp_firebox", "sp_firebox_on")}
# Blocks with a "turning" block state (the hand crank has one but keeps a single model).
STATES_TURNING = {"iron_shaft", "brass_gearbox", "hand_crank"}


def iron_shaft():
    """A 4-pixel iron shaft along z with brass couplings at both ends."""
    m = [box((6, 6, 0), (10, 10, 16), "iron_shaft")]
    for z0, z1 in ((0, 1.5), (14.5, 16)):
        m.append(box((5.5, 5.5, z0), (10.5, 10.5, z1), BRASS))
    return m


def brass_gearbox():
    """A brass-cased gearbox with a turning gear on every face and iron corner posts."""
    m = [box((0.5, 0.5, 0.5), (15.5, 15.5, 15.5), "brass_gearbox")]
    for x in (0, 14.5):
        for z in (0, 14.5):
            m.append(box((x, 0, z), (x + 1.5, 16, z + 1.5), IRON))
    return m


def hand_crank():
    """A hub against the driven block (north), an axle, a brass arm and a wooden handle."""
    m = cyl("z", 8, 8, 3, 0, 1.5, IRON_PLATE, BRASS)
    m += cyl("z", 8, 8, 1, 1.5, 7, IRON)
    m.append(box((7, 7, 7), (9, 13.5, 8.5), BRASS))
    m += cyl("z", 12, 12.5, 0.9, 8.5, 13, "sp_wood")
    return m


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
    # Flywheel and the axle out of the back.
    m += wheel("z", 8, 8, 6, 13, 14.5, IRON, BRASS)
    m += cyl("z", 8, 8, 1.25, 11, 16, IRON)
    m.append(box((6, 1, 13.25), (10, 2.75, 14.25), IRON_PLATE))
    return m


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


MODELS = {"iron_shaft": iron_shaft(), "brass_gearbox": brass_gearbox(), "hand_crank": hand_crank(),
          "steam_engine": steam_engine(), "dynamo": dynamo()}
