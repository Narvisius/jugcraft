"""Pies and tarts: the ten the owner drew on their "CAKES & BAKES - 3D PIES & TARTS" page (shared 8 October 2026, kept as
art/owner-library/drawings/pies_and_tarts.png): a strawberry pie, a blueberry tart, a plum pie, a banoffee pie, a sweet
berry tart, a lemon tart, a pumpkin pie, a pork pie, a strawberry tart and a coffee tart.

The owner's answers settled the page: its golden tart (labelled "PORK PIE" beside the tall one) is the Lemon Tart, and the
tall closed pie is the Pork Pie; the pumpkin pie is an oven pie of its own (vanilla's pumpkin pie keeps the owner's art
when set down, tools/feasts.py), so it is the Whipped Pumpkin Pie here, after the cream on its top; the pies Jugcraft
already has stay as they are. They are baked in the Hearth Oven as the pies and cakes are (Java's PieFilling holds them
after the cakes): Pastry Dough, sugar (not in the pork pie) and the bake's own ingredients make the raw pie or tart;
baked, it is set down whole, facing whoever set it down, and eaten, or cut with a knife, a quarter at a time, the front
right quarter first, as the page shows the strawberry pie and the blueberry tart cut (CakeBlock, at the bake's own
height). Left in the oven too long, one comes out a Burnt Pie.

The page draws every bake a block wide, in two shapes:
- a **pie** PIE["height"] texels tall: a crust lid from PIE["lid"] up, over a body inset a texel all round;
- a **tart** TART["height"] texels tall: a base TART["base"] tall inset a texel, under a wall a texel wide whose rim stands
  a texel above the filling (at TART["filling"]).
tools/pie_tart_art.py rebuilds their textures from the drawing and tools/pie_tart_data.py their models. The toppings are
boxes in the drawing's own coordinates (texels: x east, z south, seen from the south-east, so the quarter the page cuts
away is x 8-16, z 8-16), each standing on the bake's top (the pies' lid, the tarts' filling), fitted to the drawing; the
models turn them to face their front (cakes.turn). A topping across a cut is cut with the quarters, as the cheesecake's
jam is.
"""
import cakes

MOD = "jugcraft"
DOUGH = "pastry_dough"
BURNT = "burnt_pie"
PIE = {"height": 7, "lid": 4.5}
TART = {"height": 4, "base": 1.5, "filling": 3}
INSET = 1

# In the order of PieFilling's pies and tarts (after its cakes): name, shape, what a slice gives (hunger, saturation
# modifier: a pie's as the other pies', a tart's lower and smaller one a hunger less, the pork pie's meat more), the
# filling's colour (the Hearth Oven draws the baking bake in it), the raw bake's own ingredients (with the Pastry Dough and
# a sugar, but the pork pie's), and its toppings: a box's size (x, z, height) and where each stands (x, z of its
# north-west corner).
BAKES = {
    "strawberry_pie": {"display": "Strawberry Pie", "shape": "pie", "food": [4, 0.6], "color": 0xB82838,
                       "with": ["jugcraft:strawberry", "jugcraft:strawberry"]},
    "blueberry_tart": {"display": "Blueberry Tart", "shape": "tart", "food": [3, 0.6], "color": 0x6A44B0,
                       "with": ["jugcraft:blueberries", "jugcraft:blueberries"],
                       "toppings": {"size": [2, 2, 1], "at": [[3, 2], [6, 1], [10.5, 2.5], [11.5, 5], [1, 6], [1.5, 9.5], [5.5, 11.5]]}},
    "plum_pie": {"display": "Plum Pie", "shape": "pie", "food": [4, 0.6], "color": 0x5A1E48,
                 "with": ["jugcraft:plum", "jugcraft:plum"]},
    "banoffee_pie": {"display": "Banoffee Pie", "shape": "pie", "food": [4, 0.6], "color": 0xEEE0A0,
                     "with": ["jugcraft:banana", "jugcraft:banana", "minecraft:milk_bucket"],
                     "toppings": {"size": [4, 4, 1], "at": [[6, 6]]}},
    "sweet_berry_tart": {"display": "Sweet Berry Tart", "shape": "tart", "food": [3, 0.6], "color": 0xD84868,
                         "with": ["minecraft:sweet_berries", "minecraft:sweet_berries"],
                         "toppings": {"size": [2, 2, 1], "at": [[2.5, 2], [6, 1], [10, 3], [11.5, 6.5], [1, 6.5], [1.5, 9.5], [6, 11.5], [9, 10.5]]}},
    "lemon_tart": {"display": "Lemon Tart", "shape": "tart", "food": [3, 0.6], "color": 0xE8C040,
                   "with": ["jugcraft:lemon", "minecraft:egg"],
                   "toppings": {"size": [2, 2, 1.5], "at": [[3, 2], [6.5, 1], [10.5, 3], [11.5, 6], [1, 6.5], [2, 9.5], [6.5, 11.5], [10, 11]]}},
    "whipped_pumpkin_pie": {"display": "Whipped Pumpkin Pie", "shape": "pie", "food": [4, 0.6], "color": 0xE07828,
                            "with": ["minecraft:pumpkin", "minecraft:egg"],
                            "toppings": {"size": [4, 4, 1], "at": [[6, 6]]}},
    "pork_pie": {"display": "Pork Pie", "shape": "pie", "food": [5, 0.8], "color": 0xE0A848,
                 "with": ["minecraft:porkchop", "minecraft:porkchop"], "savory": True},
    "strawberry_tart": {"display": "Strawberry Tart", "shape": "tart", "food": [3, 0.6], "color": 0xE0607A,
                        "with": ["jugcraft:strawberry", "jugcraft:strawberry", "minecraft:milk_bucket"],
                        "toppings": {"size": [4, 4, 1.5], "at": [[5, 1], [9.5, 5], [1, 5.5], [5.5, 9.5]]}},
    "coffee_tart": {"display": "Coffee Tart", "shape": "tart", "food": [3, 0.6], "color": 0xD8B890,
                    "with": ["jugcraft:coffee_beans", "minecraft:milk_bucket"]},
}
# The two the page draws large, with a quarter cut away: their insides are read off it (tools/pie_tart_art.py); the others'
# are made from these, in their own filling's colours.
CUT = {"pie": "strawberry_pie", "tart": "blueberry_tart"}


def height(bake):
    return PIE["height"] if BAKES[bake]["shape"] == "pie" else TART["height"]


def top_of(bake):
    """Where a bake's toppings stand: a pie's lid, a tart's filling."""
    return PIE["height"] if BAKES[bake]["shape"] == "pie" else TART["filling"]


def topping_boxes(bake):
    """Every topping's box (from, to), in the drawing's coordinates."""
    spec = BAKES[bake].get("toppings")
    if not spec:
        return []
    w, d, h = spec["size"]
    y = top_of(bake)
    return [([x, y, z], [x + w, y + h, z + d]) for x, z in spec["at"]]


def raw(bake):
    return f"raw_{bake}"


def slice_item(bake):
    return f"{bake}_slice"


def blocks():
    return list(BAKES)


def items():
    return [raw(b) for b in BAKES] + list(BAKES) + [slice_item(b) for b in BAKES]


SHAPELESS = [{"id": raw(bake), "inputs": [f"{MOD}:{DOUGH}"] + ([] if info.get("savory") else ["minecraft:sugar"]) + info["with"],
              "result": raw(bake), "count": 1, "category": "misc"} for bake, info in BAKES.items()]
# The model turns the drawing as the cakes' do.
turn = cakes.turn
