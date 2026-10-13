"""Cakes: the seven the owner drew on their "CAKES & BAKES - 3D CAKES" page (shared 8 October 2026, kept as
art/owner-library/drawings/cakes_and_bakes.png): carrot, birthday, ice cream, red velvet, cheesecake, coffee and apple.

The owner chose to have them baked in the Hearth Oven as the pies are (Java's PieFilling holds them after the pies, so a
saved oven keeps its pie): Cake Batter, sugar and the cake's own two ingredients make a raw cake; baked, the cake is set
down whole, facing whoever set it down, and eaten, or cut with a knife, a quarter at a time, as the owner's INTERIOR
drawing shows (CakeBlock: the front right quarter first, then the front left, the back left and the back right). Left in
the oven too long it comes out a Burnt Cake. Only a whole cake can be picked up again.

A cake is a whole block wide and HEIGHT texels tall, the drawing's proportions. tools/cake_art.py rebuilds its textures
from the drawing and tools/cake_data.py its models. The toppings below are in the drawing's own coordinates (texels: x
east, z south, the cake's top at y = HEIGHT, seen from the south-east, so the quarter the INTERIOR drawing cuts away is
x 8-16, z 8-16); the models turn them to face their front (turn() below). Each topping lies in one quarter of the cake but
the cheesecake's jam, which the quarters share.
"""

MOD = "jugcraft"
HEIGHT = 9
BATTER = "cake_batter"
BURNT = "burnt_cake"

# In the order of PieFilling's cakes (after its pies): name, what a slice gives (hunger, saturation modifier, as a pie
# slice), the sponge's colour (the Hearth Oven draws the baking cake in it), and the raw cake's own ingredients (with the
# Cake Batter and a sugar).
CAKES = {
    "carrot_cake": {"display": "Carrot Cake", "food": [4, 0.6], "color": 0xA8501E,
                    "with": ["minecraft:carrot", "minecraft:carrot"]},
    "birthday_cake": {"display": "Birthday Cake", "food": [4, 0.6], "color": 0xF0D8A0,
                      "with": ["minecraft:candle", "minecraft:pink_dye"]},
    "ice_cream_cake": {"display": "Ice Cream Cake", "food": [4, 0.6], "color": 0x9A5226,
                       "with": ["minecraft:snowball", "minecraft:cocoa_beans"]},
    "red_velvet_cake": {"display": "Red Velvet Cake", "food": [4, 0.6], "color": 0xB0121E,
                        "with": ["minecraft:cocoa_beans", "minecraft:beetroot"]},
    "cheesecake": {"display": "Cheesecake", "food": [4, 0.7], "color": 0xF2D896,
                   "with": ["minecraft:milk_bucket", "minecraft:sweet_berries"]},
    "coffee_cake": {"display": "Coffee Cake", "food": [4, 0.6], "color": 0xC88A48,
                    "with": ["jugcraft:coffee_beans", "minecraft:cocoa_beans"]},
    "apple_cake": {"display": "Apple Cake", "food": [4, 0.6], "color": 0x9E5A26,
                   "with": ["minecraft:apple", "minecraft:apple"]},
}

# Cake Batter: two wheat, an egg, a sugar and a bucket of milk (the bucket comes back, as in vanilla's cake).
BATTER_RECIPE = ["minecraft:wheat", "minecraft:wheat", "minecraft:egg", "minecraft:sugar", "minecraft:milk_bucket"]

# The toppings, read off the drawing (tools/cake_art.py fits each to it). Bars (the carrots and the apple slices): each
# (the axis it lies along, where it starts along it, where it starts across it[, the end its leaves are at]), all of
# one length, width and height; a carrot is four texels of carrot and one of green leaves.
BARS = {
    "carrot_cake": {"length": 5, "width": 2, "height": 1.5, "leaves": 1, "bars": [
        ("z", 3, 2, "lo"), ("x", 1, 8, "lo"), ("x", 3, 13, "lo"), ("z", 10, 8, "hi"), ("z", 9, 13, "hi"), ("z", 1, 6, "lo"),
        ("x", 9, 2, "hi"), ("x", 10, 5, "hi")]},
    "apple_cake": {"length": 4, "width": 2, "height": 1.5, "leaves": 0, "bars": [
        ("z", 3, 2), ("z", 2, 6), ("x", 10, 2), ("x", 11, 5), ("x", 2, 8), ("z", 10, 8), ("x", 3, 13), ("z", 10, 13)]},
}
# The birthday cake's candles: one texel square and CANDLE_HEIGHT tall, striped white and their colour, two on each side.
CANDLE_HEIGHT = 4
CANDLE_ORDER = ["blue", "yellow", "green", "pink"]
CANDLES = {"birthday_cake": [(1, 10, "blue"), (1, 6, "yellow"), (4, 1, "green"), (9, 1, "pink"), (5, 13, "pink"),
                             (10, 13, "green"), (14, 9, "yellow"), (13, 4, "blue")]}
# The cheesecake's square of berry jam, in the middle of the top: (x0, z0, x1, z1) and its height.
JAM = {"cheesecake": ((6, 6, 10, 10), 1)}


def turn(lo, hi):
    """A box (from, to) in the drawing's coordinates, in the model's: the drawing turned half round, so the quarter it
    cuts away is the model's front right (its north-west, the model facing north)."""
    return [16 - hi[0], lo[1], 16 - hi[2]], [16 - lo[0], hi[1], 16 - lo[2]]


def bar_box(cake, bar):
    """The drawing's box for a bar: (from, to)."""
    spec = BARS[cake]
    axis, start, across = bar[:3]
    top = HEIGHT + spec["height"]
    if axis == "x":
        return [start, HEIGHT, across], [start + spec["length"], top, across + spec["width"]]
    return [across, HEIGHT, start], [across + spec["width"], top, start + spec["length"]]


def candle_box(candle):
    x, z = candle[:2]
    return [x, HEIGHT, z], [x + 1, HEIGHT + CANDLE_HEIGHT, z + 1]


def jam_box(cake):
    (x0, z0, x1, z1), height = JAM[cake]
    return [x0, HEIGHT, z0], [x1, HEIGHT + height, z1]


def topping_boxes(cake):
    """Every topping's box, in the drawing's coordinates."""
    return ([bar_box(cake, b) for b in BARS.get(cake, {}).get("bars", [])] + [candle_box(c) for c in CANDLES.get(cake, [])]
            + ([jam_box(cake)] if cake in JAM else []))


def raw(cake):
    return f"raw_{cake}"


def slice_item(cake):
    return f"{cake}_slice"


def blocks():
    return list(CAKES) + [BURNT]


def items():
    return [BATTER] + [raw(c) for c in CAKES] + list(CAKES) + [slice_item(c) for c in CAKES] + [BURNT]


ITEMS = {BATTER: {"display": "Cake Batter", "compost": "medium", "tags": []}}

SHAPELESS = [{"id": BATTER, "inputs": BATTER_RECIPE, "result": BATTER, "count": 1, "category": "misc"}] + [
    {"id": raw(cake), "inputs": [f"{MOD}:{BATTER}", "minecraft:sugar"] + info["with"], "result": raw(cake), "count": 1, "category": "misc"}
    for cake, info in CAKES.items()]
