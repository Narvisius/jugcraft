"""The train framework (docs/features/trains.md): a diesel locomotive, a passenger coach and a cargo wagon that run on
any rails, and the Coupler that makes them a train.

A train keeps its own place on the line (train/TrackPoint: a rail, the way along it and how far) rather than using the
minecart's physics, so a whole train moves as one: the locomotive moves every coupled car the same distance along its
own stretch of line, as far as its leading end can go, and stops at the end of the line or at another train. Each car
stands between two points a little in from its ends, so it leans into curves and up slopes.

Java: train/ (TrackPoint, TrainCar, Locomotive, PassengerCar, CargoWagon, TrainItem, CouplerItem, TrainInputPayload,
JugcraftTrains); client/TrainRenderer draws the parts exported here (assets/jugcraft/train_quads.json) and turns the
wheels at AXLES; client/TrainClient sends the driver's keys. tools/check_mod_data.py keeps the numbers the same.
"""
import json

import item_icons
from steampunk_models import box, cyl
from zeppelin import tiled_quads

MOD = "jugcraft"

# Each vehicle's length over its buffers (blocks), its entity box (blocks) and its seats.
LOCOMOTIVE = dict(LENGTH=4.5, WIDTH=1.4, HEIGHT=2.75, SEATS=2)
PASSENGER_CAR = dict(LENGTH=4.0, WIDTH=1.4, HEIGHT=2.65, SEATS=4)
CARGO_WAGON = dict(LENGTH=3.5, WIDTH=1.4, HEIGHT=2.35, SLOTS=27)
# Driving: throttle notches forwards and in reverse, blocks a tick for each notch, and how fast it gains speed, brakes
# and coasts (blocks a tick, each tick).
NOTCHES = 4
REVERSE_NOTCHES = 2
NOTCH_SPEED = 0.08
ACCELERATION = 0.004
BRAKING = 0.02
COASTING = 0.002
# Fuel in mB (diesel or kerosene): the tank, a bucket, and what each notch burns a second.
FUEL_TANK = 8000
FUEL_PER_BUCKET = 1000
FUEL_PER_NOTCH_SECOND = 2
MAX_CARS = 8
LOCOMOTIVE_HEALTH = 80.0
# Coupling: how near the end of the train a car must stand (blocks), and the gap left between buffers (blocks).
COUPLE_REACH = 2.5
COUPLING_GAP = 0.25
# Where a car's two leaning points are, in from each end (blocks).
BOGIE_INSET = 0.6
# The wheels: radius and distance out from the middle (pixels), and each vehicle's axles along it (pixels, forwards
# positive). Keep in step with client/TrainRenderer and JugcraftClient.
WHEEL_RADIUS = 5
WHEEL_X = 4.5
AXLES = {"locomotive": (22, 8, -8, -22), "passenger_car": (20, 10, -10, -20), "cargo_wagon": (16, -16)}
# Seats (blocks: x across, y up, z forwards) and the locomotive's exhaust, where its smoke rises.
SEAT_Y = 1.05
STACK = (0, 2.6, 0.625)

ITEMS = {"locomotive": "Diesel Locomotive", "passenger_car": "Passenger Coach", "cargo_wagon": "Cargo Wagon",
         "coupler": "Coupler"}
ENTITIES = {"locomotive": "Diesel Locomotive", "passenger_car": "Passenger Coach", "cargo_wagon": "Cargo Wagon"}
TOOLTIPS = {
    "locomotive": "Use on a rail to set it on the line. Ride in the cab: forwards and back work the throttle, jump "
                  "brakes. Refuel with a diesel or kerosene bucket.",
    "passenger_car": "Use on a rail to set it on the line behind a locomotive, then couple it. Seats four.",
    "cargo_wagon": "Use on a rail to set it on the line behind a locomotive, then couple it. Use it to open its 27 "
                   "slots of goods.",
    "coupler": "Use on a car standing just behind a train to couple it on. Sneak and use to uncouple a car and those "
               "behind it, or every car from a locomotive.",
}
MESSAGES = {
    "full": "The fuel tank is full",
    "refuelled": "Refuelled: %s%%",
    "empty": "Out of fuel",
    "stopped": "The train stopped: the line ends, or something is in the way",
    "status": "%s | %s blocks/s | Fuel %s%%",
    "notch.idle": "Idle",
    "notch.forward": "Forward %s",
    "notch.reverse": "Reverse %s",
    "on_rail": "Set it on a rail",
    "no_room": "Another train is in the way here",
    "coupled": "Coupled: %s vehicles in the train",
    "uncoupled": "Uncoupled this car and those behind it",
    "uncoupled_all": "Uncoupled every car",
    "nothing_coupled": "Nothing is coupled here",
    "already_coupled": "This car is already coupled",
    "couple_wagon": "Use the Coupler on a car standing just behind the train",
    "couple_behind": "Set the car on the line just behind a train's last car, then couple it",
}

OLIVE, HAZARD, LAMP, EXHAUST, SOOT, GRILLE = "dp_olive", "dp_hazard", "dp_lamp_on", "dp_exhaust", "dr_soot", "dp_grille"
SKID, BAND, PULLEY, RED, BRASS = "dr_skid", "dr_band", "dr_pulley", "dr_red_paint", "ik_brass"
PINE, MAPLE, CEDAR, CORRUGATED, BARE = "aged_pine_planks", "aged_maple_planks", "cedar_planks", "bk_corrugated", "dr_rust_bare"

GUIDE_PAGES = [
    dict(title="Trains", icon="jugcraft:locomotive", text=[
        "A Diesel Locomotive, Passenger Coaches and Cargo Wagons run on any rails: plain, powered, detector and activator "
        "rails all carry them, round curves and up slopes as you lay them. Use one on a rail to set it on the line, facing "
        "the way you face.",
        "Ride in the locomotive's cab to drive. Forwards notches the throttle up, to four; back notches it down through "
        "idle into two notches of reverse; jump brakes. The train gathers speed and slows gradually, and stops at the end "
        "of the line or at another train. Breaking the rail under a car knocks it off the line as an item."]),
    dict(title="Coupling and fuel", icon="jugcraft:coupler", text=[
        "Set a car on the line just behind a train's last car and use the Coupler on it: it joins the train, up to eight "
        "cars behind one locomotive, and the locomotive pulls or pushes them all. Sneak and use the Coupler to uncouple a "
        "car and those behind it, or every car from the locomotive.",
        "The locomotive burns diesel or kerosene: use a bucket of either on it (it holds eight). Each notch of throttle "
        "burns 2 mB a second. Cargo Wagons hold 27 stacks, opened by using them anywhere along the line; coaches seat four."]),
]


def _ends(half, beam, face, axle_y=11.75):
    """Buffer beams across both ends (their outer faces in `face`) and two round buffers on each."""
    m = []
    for sign in (1, -1):
        outer = "south" if sign > 0 else "north"
        z0, z1 = sorted((sign * (half - 2.5), sign * (half - 1.5)))
        m.append(box((-11, 9, z0), (11, 14.5, z1), {"*": beam, outer: face}))
        b0, b1 = sorted((sign * (half - 1.5), sign * half))
        for x in (-7, 7):
            m += cyl("z", x, axle_y, 1.5, b0, b1, PULLEY, BAND)
    return m


def _running_gear(axles, frame):
    """The axles between the wheels (the renderer adds the turning wheels), and a side frame outside each wheel with
    an axle box at each axle; frame is the side frames' z span for each bogie or axle guard."""
    m = []
    for z in axles:
        m += cyl("x", WHEEL_RADIUS, z, 0.8, -3.5, 3.5, BAND)
        for sign in (1, -1):
            x0, x1 = sorted((sign * 7.25, sign * 8.25))
            m.append(box((x0, 3.5, z - 1.5), (x1, 6.5, z + 1.5), {"*": BAND}))
    for z0, z1 in frame:
        for sign in (1, -1):
            x0, x1 = sorted((sign * 5.75, sign * 7.25))
            m.append(box((x0, 2.5, z0), (x1, 9, z1), {"*": SKID}))
        m.append(box((-7.25, 9, (z0 + z1) / 2 - 2), (7.25, 10.5, (z0 + z1) / 2 + 2), {"*": SKID}))
    return m


def locomotive():
    """The diesel locomotive, nose forwards (+z): a long olive hood with a brass band, a grille, the exhaust stack and a
    headlamp; an open-windowed cab at the back with the driver's bench; on a steel frame over two bogies."""
    half = LOCOMOTIVE["LENGTH"] * 8
    m = [box((-10, 10.5, -half + 2.5), (10, 13, half - 2.5), {"*": SKID})]
    m += _ends(half, BAND, HAZARD)
    # The hood, its band, grille, stack and headlamp.
    m.append(box((-8, 13, -6), (8, 31, 32), {"*": OLIVE}))
    m.append(box((-8.25, 24, -6), (8.25, 25.5, 32.25), {"*": BRASS}))
    m.append(box((-6, 31, 2), (6, 31.5, 28), {"*": GRILLE}))
    m += cyl("y", 0, STACK[2] * 16, 2, 31, STACK[1] * 16, EXHAUST, SOOT)
    m.append(box((-2, 26, 32), (2, 29, 33), {"*": BAND, "south": f"{LAMP}!"}))
    # Handrails along the walkways beside the hood.
    for sign in (1, -1):
        x0, x1 = sorted((sign * 9.5, sign * 10))
        for z in (-4, 10, 24):
            m.append(box((x0, 13, z), (x1, 21, z + 0.5), {"*": BAND}))
        m.append(box((x0, 21, -4), (x1, 21.5, 31), {"*": BAND}))
    # The cab, open-windowed so its crew shows: floor, lower walls, corner pillars, headers and the roof.
    cab0, cab1 = -half + 2.5, -8
    m.append(box((-9.5, 13, cab0 + 1.5), (9.5, 14, cab1 - 1.5), {"*": PINE}))
    for sign in (1, -1):
        x0, x1 = sorted((sign * 9.5, sign * 11))
        m.append(box((x0, 13, cab0), (x1, 26, cab1), {"*": OLIVE}))
        m.append(box((x0, 26, cab0), (x1, 38, cab0 + 3), {"*": OLIVE}))
        m.append(box((x0, 26, cab1 - 3), (x1, 38, cab1), {"*": OLIVE}))
        m.append(box((x0, 38, cab0), (x1, 42, cab1), {"*": OLIVE}))
    for z0, z1, low in ((cab0, cab0 + 1.5, 26), (cab1 - 1.5, cab1, 31)):
        m.append(box((-9.5, 13, z0), (9.5, low, z1), {"*": OLIVE}))
        for sign in (1, -1):
            x0, x1 = sorted((sign * 6, sign * 9.5))
            m.append(box((x0, low, z0), (x1, 38, z1), {"*": OLIVE}))
        m.append(box((-9.5, 38, z0), (9.5, 42, z1), {"*": OLIVE}))
    m.append(box((-12, 42, cab0 - 1), (12, 44, cab1 + 1), {"*": SKID}))
    m.append(box((-9.5, 14, -27), (9.5, 19.5, -17), {"*": RED}))
    m.append(box((-9.5, 19.5, -27), (9.5, 27, -25.5), {"*": RED}))
    return m + _running_gear(AXLES["locomotive"], [(3, 27), (-27, -3)])


def passenger_car():
    """The passenger coach: a red body with a brass band, a row of open windows between pillars, two facing benches
    inside, a clerestory roof, on a steel frame over two bogies."""
    half = PASSENGER_CAR["LENGTH"] * 8
    body = half - 2
    m = [box((-10, 10.5, -body), (10, 13, body), {"*": SKID})]
    m += _ends(half, BAND, RED)
    m.append(box((-9.5, 12.5, -body + 1.5), (9.5, 13.5, body - 1.5), {"*": PINE}))
    for sign in (1, -1):
        x0, x1 = sorted((sign * 9.5, sign * 11))
        m.append(box((x0, 13, -body), (x1, 24, body), {"*": RED}))
        b0, b1 = sorted((sign * 11, sign * 11.25))
        m.append(box((b0, 22, -body), (b1, 23, body), {"*": BRASS}))
        for z in (-body, -16, -2, 12, body - 4):
            m.append(box((x0, 24, z), (x1, 36, z + 4), {"*": RED}))
        m.append(box((x0, 36, -body), (x1, 40, body), {"*": RED}))
    for z0, z1 in ((-body, -body + 1.5), (body - 1.5, body)):
        m.append(box((-9.5, 13, z0), (9.5, 40, z1), {"*": RED}))
    # The benches, a facing pair.
    for sign in (1, -1):
        z0, z1 = sorted((sign * 9, sign * 15))
        m.append(box((-9.5, 13.5, z0), (9.5, 19.5, z1), {"*": MAPLE}))
        b0, b1 = sorted((sign * 15, sign * 16.5))
        m.append(box((-9.5, 13.5, b0), (9.5, 27, b1), {"*": MAPLE}))
    m.append(box((-12, 40, -body - 1), (12, 42, body + 1), {"*": SKID}))
    m.append(box((-6, 42, -body + 2), (6, 43.5, body - 2), {"*": SKID}))
    return m + _running_gear(AXLES["passenger_car"], [(5, 25), (-25, -5)])


def cargo_wagon():
    """The cargo wagon: a plank goods van with a sliding door on each side under its rail, corner straps and a
    corrugated steel roof, on a steel frame over two axles."""
    half = CARGO_WAGON["LENGTH"] * 8
    body = half - 2.5
    m = [box((-10, 10.5, -body), (10, 13, body), {"*": SKID})]
    m += _ends(half, BAND, RED)
    m.append(box((-10.5, 13, -body + 0.5), (10.5, 35, body - 0.5), {"*": MAPLE, "up": CORRUGATED}))
    for sign in (1, -1):
        x0, x1 = sorted((sign * 10.5, sign * 11))
        m.append(box((x0, 14, -6), (x1, 33, 6), {"*": CEDAR}))
        r0, r1 = sorted((sign * 10.5, sign * 11.5))
        m.append(box((r0, 33, -12), (r1, 34, 12), {"*": BAND}))
        for z in (-body + 0.5, body - 2):
            m.append(box((x0, 13, z), (x1, 35, z + 1.5), {"*": BAND}))
    m.append(box((-11.5, 35, -body - 0.5), (11.5, 37, body + 0.5), {"*": CORRUGATED}))
    return m + _running_gear(AXLES["cargo_wagon"], [(12, 20), (-20, -12)])


def wheel():
    """One wheel about its axle (the origin, along x): a steel tyre with a bare-steel face and a cross of spokes on each
    face, so it shows turning."""
    m = cyl("x", 0, 0, WHEEL_RADIUS, -1, 1, SKID, BARE)
    for sign in (1, -1):
        x0, x1 = sorted((sign * 1, sign * 1.25))
        m.append(box((x0, -4, -0.75), (x1, 4, 0.75), {"*": BAND}))
        m.append(box((x0, -0.75, -4), (x1, 0.75, -0.75), {"*": BAND}))
        m.append(box((x0, -0.75, 0.75), (x1, 0.75, 4), {"*": BAND}))
    return m


def export():
    return {"train_locomotive": tiled_quads(locomotive()), "train_passenger_car": tiled_quads(passenger_car()),
            "train_cargo_wagon": tiled_quads(cargo_wagon()), "train_wheel": tiled_quads(wheel())}


def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    for entity, name in ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = name
    for key, text in MESSAGES.items():
        lang[f"message.{MOD}.train.{key}"] = text
    (assets / "train_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")

    def shaped(name, pattern, key):
        write(data / "recipe" / f"{name}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{name}", "count": 1}})
    shaped("locomotive", ["PGP", "PEP", "WMW"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane",
                                                 "E": f"{MOD}:diesel_engine", "W": "#c:gears/steel",
                                                 "M": "minecraft:minecart"})
    shaped("passenger_car", ["PGP", "PWP", " M "], {"P": "#c:plates/steel", "G": "minecraft:glass_pane",
                                                    "W": "#minecraft:wool", "M": "minecraft:minecart"})
    shaped("cargo_wagon", ["PCP", " M "], {"P": "#c:plates/steel", "C": "minecraft:chest", "M": "minecraft:minecart"})
    shaped("coupler", [" H", "P "], {"H": "minecraft:tripwire_hook", "P": "#c:plates/steel"})


def draw_all(save):
    for item in ITEMS:
        save(item_icons.draw(item), "item", item)
