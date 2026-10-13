"""Milkshakes: the seven the owner drew on their "CAKES & BAKES - 3D MILKSHAKE" page (shared 8 October 2026, kept as
art/owner-library/drawings/milkshakes.png): strawberry, banana, plum, apple, blueberry, pumpkin and chocolate.

Each is a drink of the menu's kind (tools/menu.py "drink"): drunk even on a full stomach for a short effect, leaving its
glass bottle, and set down by a sneaking player as the menu's dishes are (PlacedDishBlock), here as the owner's 3D glass,
and taken back with an empty hand. A Milk Bottle, a snowball, a sugar and the milkshake's own flavour make one; the
bottle the milk was in is the glass.

The glass is as the page draws it, in the drawing's own coordinates (texels: x east, z south, y up from the underside of
its foot; seen from the south-east, so the straw stands at the back right): four glass bars round a square foot, the
base of the glass a little narrower than it (its corners lighter, like short legs), the glass itself (its corner posts and
the shake between them), a band of glass round its rim, the cream heaped over it, the fruit on top and a straw leaning
back through the cream. As the page draws it, the base stands a texel clear of the foot's bars. A dish set down faces
whoever set it down with its south side, so the model keeps the drawing's coordinates: seen as set down, the glass looks
as drawn. tools/milkshake_art.py reads the textures off the drawing and tools/milkshake_data.py builds the model.

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): every milkshake gives FOOD, at most COOK_BONUS over what goes in
(the flavour; the milk, snowball and sugar are not food); tools/check_mod_data.py checks.
"""
MOD = "jugcraft"
MILK = "jugcraft:milk_bottle"
FOOD = [5, 0.6]
# A sugar rush: Haste for half a minute.
EFFECT = ["HASTE", 30]

# In the page's order: name, and the flavour that goes in with the milk, snowball and sugar. The chocolate milkshake's
# cherry is a sweet berry.
SHAKES = {
    "strawberry_milkshake": {"display": "Strawberry Milkshake", "with": ["jugcraft:strawberry", "jugcraft:strawberry"]},
    "banana_milkshake": {"display": "Banana Milkshake", "with": ["jugcraft:banana"]},
    "plum_milkshake": {"display": "Plum Milkshake", "with": ["jugcraft:plum"]},
    "apple_milkshake": {"display": "Apple Milkshake", "with": ["minecraft:apple"]},
    "blueberry_milkshake": {"display": "Blueberry Milkshake", "with": ["jugcraft:blueberries", "jugcraft:blueberries"]},
    "pumpkin_milkshake": {"display": "Pumpkin Milkshake", "with": ["minecraft:pumpkin"]},
    "chocolate_milkshake": {"display": "Chocolate Milkshake", "with": ["minecraft:cocoa_beans", "minecraft:sweet_berries"]},
}
# The one the page draws large: the glass, cream, base, foot and straw are read off it; each milkshake's own shake and
# fruit off its own drawing.
LARGE = "strawberry_milkshake"

# The glass, part by part: (from, to) in texels, as fitted to the page (tools/milkshake_art.py CAMERAS).
FOOT = {"bars": [((6, 0, 10), (10, 1, 11)), ((10, 0, 6), (11, 1, 10)), ((6, 0, 5), (10, 1, 6)), ((5, 0, 6), (6, 1, 10))]}
PLATE = ((5.5, 2, 5.5), (10.5, 4, 10.5))
BODY = ((5, 4, 5), (11, 10.5, 11))
BAND = ((4.75, 8.75, 4.75), (11.25, 10.5, 11.25))
CAP = ((5, 10.5, 5), (11, 12.25, 11))
FRUIT = ((7, 12.25, 7), (9, 14.25, 9))
# The straw: a texel square, from inside the cream up past the cap, leaning back (north) about its foot on the cap.
STRAW = {"from": (9.5, 11.25, 5.5), "to": (10.5, 15.75, 6.5), "origin": (10, 12.25, 6), "axis": "x", "angle": -22.5}
# The block's outline, round the glass's widest part (its band) and up to the top of the straw.
OUTLINE = (4.75, 0, 4.75, 11.25, 15.75, 11.25)

# Every face's place on a milkshake's texture (block/menu/<name>, 64 x 64: four pixels to a texel, so the band's quarter
# texels are whole pixels), (u0, v0, u1, v1) in texels: the cap's top, its south and east sides; the band's sides and its
# top (a ring of glass, one colour); the glass's sides; the base's sides and its underside (one colour, the glass's too);
# the fruit's top and sides; a foot bar's top, long side and end; the straw's sides and end. The north and west sides wear
# the south's and east's.
LAYOUT = {
    "cap_up": (0, 0, 6, 6), "cap_s": (0, 6, 6, 7.75), "cap_e": (0, 7.75, 6, 9.5),
    "band_s": (0, 9.5, 6.5, 11.25), "band_e": (0, 11.25, 6.5, 13),
    "bar_up": (0, 13, 4, 14), "bar_side": (0, 14, 4, 15), "bar_end": (4, 13, 5, 14),
    "body_s": (6, 0, 12, 6.5), "body_e": (6, 6.5, 12, 13),
    "band_up": (12, 0, 13, 1), "plate_down": (13, 0, 14, 1),
    "fruit_up": (12, 1, 14, 3), "fruit_s": (14, 1, 16, 3), "fruit_e": (12, 3, 14, 5),
    "plate_s": (6, 13, 11, 15), "plate_e": (11, 13, 16, 15),
    "straw_s": (12, 5, 13, 9.5), "straw_e": (13, 5, 14, 9.5), "straw_up": (14, 5, 15, 6),
}
PIXELS = 4


def placed():
    """The milkshakes that set down, with their model template (as tools/menu.py placed())."""
    return {name: ["milkshake"] for name in SHAKES}


def blocks():
    return list(SHAKES)


DISHES = {name: {"display": info["display"], "food": list(FOOD), "kind": "drink", "effect": list(EFFECT), "model": ["milkshake"]}
          for name, info in SHAKES.items()}
# Items for tools/agriculture.py ITEMS: each milkshake a drink. Its item model is its 3D glass (tools/milkshake_data.py).
ITEMS = {name: {"display": info["display"], "food": list(FOOD), "drink": list(EFFECT), "tags": ["c:foods"]} for name, info in SHAKES.items()}
SHAPELESS = [{"id": name, "inputs": [MILK, "minecraft:snowball", "minecraft:sugar"] + info["with"], "result": name, "count": 1,
              "category": "misc"} for name, info in SHAKES.items()]
