"""JSON resources for the fourth batch of Halloween decorations, the witch's cottage, from tools/agriculture.py: the
Bubbling Cauldron, the Apothecary Shelf, the Crystal Ball, the Grimoire Stand and the Witch's Broom; their names and
messages, loot, tags (and the brew ingredient tags).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: block model
element rotation, face rotation and light emission. Model rotations are right-handed: about x, a positive angle turns
+y toward +z. Textures with see-through pixels are only used where every face reads inside them.
"""
from agriculture import CAULDRON, APOTHECARY_SHELF, CRYSTAL_BALL, GRIMOIRE, BROOM, HEX
from decor_data import MOD, HORIZONTAL, SIDES, rid, turned, box, block_model, flat_item, self_drop
from decor3_data import fitted, UP_SIDES

BREW_COLOURS = ("green", "purple", "orange")
HEXES = tuple(HEX["brews"])
# How high a hex brew stands in the pot with each number of doses left (fall addition 21).
HEX_LEVELS = {1: 7.5, 2: 9.0, 3: 10.5}


# ---------------------------------------------------------------- the bubbling cauldron

def cauldron_model(contents, level=10.5):
    """An iron pot on three stubby legs with a lip round its rim; inside, water, a glowing brew or a hex brew (lower
    as its doses are drawn off)."""
    elements = [box((2, 2, 2), (14, 13, 3), "#iron"), box((2, 2, 13), (14, 13, 14), "#iron"),
                box((2, 2, 3), (3, 13, 13), "#iron"), box((13, 2, 3), (14, 13, 13), "#iron"),
                box((3, 2, 3), (13, 3, 13), "#iron"),
                box((1.5, 12, 1.5), (14.5, 13.5, 2.5), "#iron"), box((1.5, 12, 13.5), (14.5, 13.5, 14.5), "#iron"),
                box((1.5, 12, 2.5), (2.5, 13.5, 13.5), "#iron"), box((13.5, 12, 2.5), (14.5, 13.5, 13.5), "#iron"),
                box((3, 0, 3), (5, 2, 5), "#iron"), box((11, 0, 3), (13, 2, 5), "#iron"), box((7, 0, 11), (9, 2, 13), "#iron")]
    if contents != "empty":
        glowing = contents in BREW_COLOURS or contents in HEXES
        elements.append(box((3, level, 3), (13, level + 0.5, 13), "#liquid", faces=("up",), light=(12 if contents in HEXES else 10) if glowing else None))
    textures = {"iron": "bubbling_cauldron_iron"}
    if contents in HEXES:
        textures["liquid"] = f"bubbling_cauldron_hex_{contents}"
    elif contents != "empty":
        textures["liquid"] = "bubbling_cauldron_water" if contents == "water" else f"bubbling_cauldron_brew_{contents}"
    return block_model(textures, elements, "bubbling_cauldron_iron")


# ---------------------------------------------------------------- the apothecary shelf

def jar(x, y, colour):
    """A glass jar with something in it and a cork, on a shelf at height y, its left side at x."""
    return [box((x, y, 10.5), (x + 3, y + 4, 13.5), "#glass", uvs={side: (0, 0, 16, 16) for side in SIDES}),
            box((x + 0.5, y, 11), (x + 2.5, y + 3, 13), f"#{colour}"),
            box((x + 0.5, y + 4, 11), (x + 2.5, y + 4.5, 13), "#cork", faces=UP_SIDES)]


def bottle(x, y, colour):
    return [box((x, y, 11), (x + 2, y + 3, 13), f"#{colour}"), box((x + 0.5, y + 3, 11.5), (x + 1.5, y + 4.5, 12.5), "#cork")]


def skull(x, y):
    return [box((x, y, 10.5), (x + 3, y + 3, 13.5), "#skull", textures={"north": "#skull_face"})]


def candle(x, y):
    return [box((x, y, 11.5), (x + 1, y + 3, 12.5), "#wax"), box((x + 0.25, y + 3, 11.75), (x + 0.75, y + 4, 12.25), "#flame", light=15)]


# Four ways to set the shelves out: (lower shelf, upper shelf), each a list of (kind, x, colour).
ARRANGEMENTS = [
    ([("jar", 1, "green"), ("bottle", 5, "purple"), ("skull", 8, None), ("jar", 12, "amber")],
     [("bottle", 1, "red"), ("bottle", 4, "green"), ("jar", 7, "purple"), ("candle", 12, None)]),
    ([("skull", 1, None), ("jar", 5, "red"), ("jar", 9, "green"), ("bottle", 13, "amber")],
     [("jar", 2, "amber"), ("candle", 7, None), ("bottle", 10, "purple"), ("bottle", 13, "red")]),
    ([("bottle", 1, "green"), ("bottle", 4, "green"), ("bottle", 7, "purple"), ("candle", 12, None)],
     [("jar", 1, "red"), ("skull", 6, None), ("jar", 11, "purple")]),
    ([("jar", 1, "purple"), ("jar", 5, "amber"), ("bottle", 9, "red"), ("skull", 12, None)],
     [("candle", 2, None), ("jar", 5, "green"), ("bottle", 10, "amber"), ("bottle", 13, "green")]),
]


def shelf_model(arrangement):
    """Two plank shelves on iron brackets against a wall to the south, facing north, with jars set out on them."""
    elements = [box((0, 1, 9), (16, 2, 16), "#wood"), box((0, 8, 9), (16, 9, 16), "#wood"),
                box((1, 0, 15), (2, 8, 16), "#bracket"), box((14, 0, 15), (15, 8, 16), "#bracket"),
                box((1, 7, 15), (2, 15, 16), "#bracket"), box((14, 7, 15), (15, 15, 16), "#bracket")]
    makers = {"jar": jar, "bottle": bottle, "skull": lambda x, y, c: skull(x, y), "candle": lambda x, y, c: candle(x, y)}
    for shelf_y, things in zip((2, 9), ARRANGEMENTS[arrangement]):
        for kind, x, colour in things:
            elements += makers[kind](x, shelf_y, colour)
    return fitted(block_model({"wood": "apothecary_shelf_wood", "bracket": "apothecary_shelf_bracket", "glass": "apothecary_shelf_glass",
                               "cork": "apothecary_shelf_cork", "skull": "apothecary_shelf_skull", "skull_face": "apothecary_shelf_skull_face",
                               "wax": "apothecary_shelf_wax", "flame": "apothecary_shelf_flame",
                               **{c: f"apothecary_shelf_{c}" for c in ("green", "purple", "red", "amber")}}, elements,
                              "apothecary_shelf_wood"))


# ---------------------------------------------------------------- the crystal ball, grimoire stand and broom

def ball_model(gazing):
    """A gilt claw stand holding a glass orb with violet mist inside; gazing, the mist blazes."""
    claws = [box((x, 1, z), (x + 1.5, 3.5, z + 1.5), "#gold") for x, z in ((4.5, 4.5), (10, 4.5), (4.5, 10), (10, 10))]
    return block_model({"gold": "crystal_ball_gold", "orb": "crystal_ball_orb", "mist": "crystal_ball_mist_bright" if gazing else "crystal_ball_mist"}, [
        box((4, 0, 4), (12, 1, 12), "#gold"), box((6.5, 1, 6.5), (9.5, 3, 9.5), "#gold"), *claws,
        box((5.5, 4, 5.5), (10.5, 9, 10.5), "#mist", light=15 if gazing else 8),
        box((4.5, 3, 4.5), (11.5, 10, 11.5), "#orb", uvs={side: (0, 0, 16, 16) for side in SIDES + ("up", "down")}),
    ], "crystal_ball_gold")


def grimoire_model(spread):
    """A carved stand facing north, its top tipped toward the reader, holding the book open at `spread`. The pages'
    tops face away from the reader (turned 180 degrees on the top face)."""
    tilt = {"origin": [8, 11, 8], "axis": "x", "angle": -22.5}
    page = {"texture": "#pages", "rotation": 180}
    left, right = box((1.5, 12, 3), (8, 12.75, 13), "#paper"), box((8, 12, 3), (14.5, 12.75, 13), "#paper")
    for element in (left, right):
        element["faces"]["up"] = dict(page, uv=[element["from"][0], element["from"][2], element["to"][0], element["to"][2]])
        element["rotation"] = tilt
    return fitted(block_model({"wood": "grimoire_stand_wood", "cover": "grimoire_stand_cover", "paper": "grimoire_stand_paper",
                               "pages": f"grimoire_stand_{spread}"}, [
        box((3, 0, 3), (13, 2, 13), "#wood"), box((6, 2, 6), (10, 11, 10), "#wood"),
        box((1, 11, 2.5), (15, 12, 13.5), "#cover", rotation=tilt), left, right,
        box((7.75, 12.75, 3), (8.25, 13, 13), "#cover", faces=("up",), rotation=tilt),
    ], "grimoire_stand_wood"))


def broom_model():
    """A besom standing on its bristles (to the north), its crooked handle leaning back to the south."""
    lean = {"origin": [8, 5, 6.5], "axis": "x", "angle": 22.5}
    return fitted(block_model({"bristle": "witchs_broom_bristle", "handle": "witchs_broom_handle", "twine": "witchs_broom_twine"}, [
        box((5, 0, 4), (11, 5, 9), "#bristle"), box((6, 5, 5), (10, 6.5, 8), "#twine"),
        box((7.5, 6.5, 6), (8.5, 17, 7), "#handle", rotation=lean), box((7.5, 17, 6), (8.5, 24, 7), "#handle",
                                                                        rotation={"origin": [8, 5, 6.5], "axis": "x", "angle": 22.5}),
    ], "witchs_broom_handle"))


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    cauldron = CAULDRON["block"]
    for contents in ("empty", "water") + BREW_COLOURS:
        write(models / f"{cauldron}_{contents}.json", cauldron_model(contents))
    variants = {f"contents={c}": {"model": rid(f"block/{cauldron}_{c}")} for c in ("empty", "water") + BREW_COLOURS}
    for hex_name in HEXES:
        for doses, level in HEX_LEVELS.items():
            write(models / f"{cauldron}_{hex_name}_{doses}.json", cauldron_model(hex_name, level))
            variants[f"contents={hex_name},doses={doses}"] = {"model": rid(f"block/{cauldron}_{hex_name}_{doses}")}
    write(states / f"{cauldron}.json", {"variants": variants})
    for hex_name, info in HEX["brews"].items():
        flat_item(root, write, info["item"])
        lang[f"item.{MOD}.{info['item']}"] = info["display"]
        if "effect" in info:
            lang[f"effect.{MOD}.{info['effect']}"] = info["effect_display"]
    lang.update({"message.jugcraft.hex.no_room": "There is no room to grow here",
                 "tooltip.jugcraft.hex.shrinking": "Makes the drinker half their size",
                 "tooltip.jugcraft.hex.giant": "Makes the drinker a giant, if there is room",
                 "tooltip.jugcraft.hex.flying": "Rub on for a feather-light fall"})
    write(root / "items" / f"{cauldron}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{cauldron}_green")}})
    lang[f"block.{MOD}.{cauldron}"] = CAULDRON["display"]

    shelf = APOTHECARY_SHELF["block"]
    for n in range(APOTHECARY_SHELF["arrangements"]):
        write(models / f"{shelf}_{n}.json", shelf_model(n))
    write(states / f"{shelf}.json", {"variants": {f"arrangement={n},facing={f}": turned(rid(f"block/{shelf}_{n}"), f)
                                                  for f in HORIZONTAL for n in range(APOTHECARY_SHELF["arrangements"])}})
    flat_item(root, write, shelf)
    lang[f"block.{MOD}.{shelf}"] = APOTHECARY_SHELF["display"]

    ball = CRYSTAL_BALL["block"]
    write(models / f"{ball}.json", ball_model(False))
    write(models / f"{ball}_gazing.json", ball_model(True))
    write(states / f"{ball}.json", {"variants": {"gazing=false": {"model": rid(f"block/{ball}")},
                                                 "gazing=true": {"model": rid(f"block/{ball}_gazing")}}})
    write(root / "items" / f"{ball}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{ball}")}})
    lang[f"block.{MOD}.{ball}"] = CRYSTAL_BALL["display"]
    for n, fortune in enumerate(CRYSTAL_BALL["fortunes"]):
        lang[f"message.{MOD}.crystal_ball.fortune.{n}"] = fortune

    grimoire = GRIMOIRE["block"]
    for spread in GRIMOIRE["spreads"]:
        write(models / f"{grimoire}_{spread}.json", grimoire_model(spread))
        lang[f"message.{MOD}.grimoire.{spread}"] = GRIMOIRE["spreads"][spread]
    write(states / f"{grimoire}.json", {"variants": {f"facing={f},page={p}": turned(rid(f"block/{grimoire}_{p}"), f)
                                                     for f in HORIZONTAL for p in GRIMOIRE["spreads"]}})
    write(root / "items" / f"{grimoire}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{grimoire}_moons")}})
    lang[f"block.{MOD}.{grimoire}"] = GRIMOIRE["display"]

    broom = BROOM["block"]
    write(models / f"{broom}.json", broom_model())
    write(states / f"{broom}.json", {"variants": {f"facing={f}": turned(rid(f"block/{broom}"), f) for f in HORIZONTAL}})
    flat_item(root, write, broom)
    lang[f"block.{MOD}.{broom}"] = BROOM["display"]


def loot(out, write):
    for block in (CAULDRON["block"], APOTHECARY_SHELF["block"], CRYSTAL_BALL["block"], GRIMOIRE["block"], BROOM["block"]):
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    for colour, items in CAULDRON["brews"].items():
        for item in items:
            tags.add("item", f"{MOD}:brew/{colour}", item)
    for hex_name, info in HEX["brews"].items():
        for item in info["ingredients"]:
            tags.add("item", f"{MOD}:hex/{hex_name}", item)
    for block in (CAULDRON["block"], CRYSTAL_BALL["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in (APOTHECARY_SHELF["block"], GRIMOIRE["block"], BROOM["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
