"""The Ferris wheel (fall addition 27): a fairground big wheel, real-life sized, turned by the mod's kinetic power. The
numbers here are what agriculture/FerrisWheel and FerrisWheelBoothBlock(Entity) use; tools/check_mod_data.py compares
them. tools/ferris_wheel_data.py writes the JSON and the quads the client draws the wheel from, and
tools/ferris_wheel_textures.py the textures.

The Ferris Wheel block is its booth: a loading platform with the operator's controls, and the only real block. Placed,
it raises the wheel over itself, an entity: two A-frames of lattice steel standing `frame_feet` blocks either side of the
booth, the hub `hub` blocks up, and two rims `radius` blocks round with `cars` cars hanging between them, `seats` to a
car. It needs a clear space `width` blocks across (along the wheel), `height` blocks up from the booth's foot and `depth`
blocks through (along the axle); the wheel faces whoever places it.

The booth takes kinetic energy (KE) from a shaft, gearbox or source touching it, as a machine does: up to `need` KE a
tick, which turns the wheel at full speed, one turn in `turn_ticks` ticks; less turns it slower, in proportion. It speeds
up by at most `accel` of full speed a tick and slows by `decel` a tick when the power stops. A hand crank (16 KE a tick
for 100 ticks a crank) turns it at full speed, so one player can crank for their friends.

Using the booth puts a player in the empty seat of the car nearest the bottom. Getting off (sneak) sets the rider down
on the platform in front of the booth, however high their car was. If a block stands where a car is about to go, the
wheel stops (it is jammed) until it is cleared; it never carries a rider into a block. Riding all the way round earns
Round and Round; riding all the way round with someone in the seat beside you, Two to a Car.
"""
import math

FERRIS_WHEEL = {
    "block": "ferris_wheel", "display": "Ferris Wheel", "entity": "ferris_wheel", "entity_display": "Ferris Wheel",
    # Geometry, in blocks: the hub's height above the booth's foot, the cars' pivots' radius, the A-frames' feet either
    # side of the booth, and the clear space the wheel needs.
    "hub": 9.5, "radius": 6.0, "frame_feet": 4.5, "width": 15, "height": 16, "depth": 3,
    "cars": 8, "seats": 2,
    # A rider's seat, in blocks from their car's pivot: across the car (either side), down, and back from its middle.
    "seat_across": 0.34, "seat_down": 1.75, "seat_back": 0.25,
    # Power and motion: KE a tick for full speed, ticks to a turn at full speed, speeding up and slowing down (a share
    # of full speed a tick).
    "need": 12, "turn_ticks": 800, "accel": 1 / 40, "decel": 1 / 60,
    # How far from the booth a player may be to board, in blocks.
    "board_reach": 4.0,
}

# The cars' colours, in turn round the wheel.
CAR_COLOURS = ("pumpkin", "cranberry", "mustard", "spruce")

ADVANCEMENTS = {
    "round_and_round": {"icon": "jugcraft:ferris_wheel", "title": "Round and Round", "description": "Ride the Ferris Wheel all the way round",
                        "frame": "task"},
    "two_to_a_car": {"icon": "jugcraft:ferris_wheel", "title": "Two to a Car",
                     "description": "Ride the Ferris Wheel all the way round with someone beside you", "frame": "goal"},
}

# The wheel: steel for its rims and frame, redstone lamps for its lights, an iron block for its hub and wool for the cars'
# seats. Its power comes from the kinetic network (a hand crank is planks and an iron shaft).
SHAPED = [
    {"id": FERRIS_WHEEL["block"], "pattern": ["LBL", "BIB", "WBW"],
     "key": {"L": "minecraft:redstone_lamp", "B": "minecraft:iron_bars", "I": "minecraft:iron_block", "W": "#minecraft:wool"},
     "result": FERRIS_WHEEL["block"], "count": 1, "category": "building"},
]


def blocks():
    return [FERRIS_WHEEL["block"]]


def items():
    return blocks()


def speed():
    """Full speed, in radians a tick."""
    return 2 * math.pi / FERRIS_WHEEL["turn_ticks"]


def car_angle(car, angle=0.0):
    """Where car `car` is round the wheel turned to `angle` (radians): car 0 starts at the bottom."""
    return angle - math.pi / 2 + car * 2 * math.pi / FERRIS_WHEEL["cars"]
