"""JSON resources for the fifth batch of Halloween decorations, the harvest party, from tools/agriculture.py: the
Bobbing for Apples Tub, the Pumpkin Crate, the Hay Bale Seat, the Autumn Wreath and the Leaf Piles; their names and
messages, loot and tags (and the crate's produce tag).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: block model
element rotation, a select on a block state property for an item, copy_state in loot and a count by block state
(match_block, as the floating candles'). Model rotations are right-handed: about z, a positive angle turns +x toward +y.
"""
from agriculture import BOBBING_TUB, PUMPKIN_CRATE, HAY_BALE_SEAT, AUTUMN_WREATH, LEAF_PILES, leaf_piles
from decor_data import MOD, HORIZONTAL, SIDES, rid, turned, box, block_model, self_drop
from decor3_data import fitted, UP_SIDES


# ---------------------------------------------------------------- the bobbing tub

# Where the floating apples bob, in the order they go in: (x, z) of each apple's north-west corner.
APPLE_SPOTS = [(4, 4), (9, 8.5), (4.5, 9.5), (9.5, 3.5)]


def tub_model(apples):
    """A round-ish tub of plank staves with two iron bands, full of water, with `apples` apples afloat."""
    elements = [box((1, 0, 1), (15, 1, 15), "#wood"),
                box((1, 1, 1), (15, 10, 2), "#wood"), box((1, 1, 14), (15, 10, 15), "#wood"),
                box((1, 1, 2), (2, 10, 14), "#wood"), box((14, 1, 2), (15, 10, 14), "#wood"),
                # the iron bands, a hair proud of the staves
                box((0.75, 2, 0.75), (15.25, 3, 15.25), "#band", faces=SIDES), box((0.75, 7, 0.75), (15.25, 8, 15.25), "#band", faces=SIDES),
                box((2, 8, 2), (14, 8.5, 14), "#water", faces=("up",))]
    for x, z in APPLE_SPOTS[:apples]:
        elements += [box((x, 7.5, z), (x + 3, 10.5, z + 3), "#apple"),
                     box((x + 1.25, 10.5, z + 1.25), (x + 1.75, 11.5, z + 1.75), "#stem", faces=UP_SIDES)]
    return block_model({"wood": "bobbing_tub_wood", "band": "bobbing_tub_band", "water": "bobbing_tub_water",
                        "apple": "bobbing_tub_apple", "stem": "bobbing_tub_stem"}, elements, "bobbing_tub_wood")


# ---------------------------------------------------------------- the pumpkin crate

def crate_model():
    """A low crate of two slats a side between corner posts, open on top, facing north with a painted label."""
    elements = [box((0, 0, 0), (16, 1.5, 16), "#wood")]
    for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
        elements.append(box((x, 0, z), (x + 2, 7, z + 2), "#post"))
    for y0, y1 in ((1.5, 3.5), (4.5, 6.5)):
        elements += [box((2, y0, 0.5), (14, y1, 1.5), "#wood", textures={"north": "#label"} if y0 > 2 else None),
                     box((2, y0, 14.5), (14, y1, 15.5), "#wood"),
                     box((0.5, y0, 2), (1.5, y1, 14), "#wood"), box((14.5, y0, 2), (15.5, y1, 14), "#wood")]
    return block_model({"wood": "pumpkin_crate_wood", "post": "pumpkin_crate_post", "label": "pumpkin_crate_label"}, elements,
                       "pumpkin_crate_wood")


# ---------------------------------------------------------------- the hay bale seat

def bale_model():
    """A low bale, its cut ends to the north and south, bound by two twine bands round its middle (the blockstate turns it)."""
    elements = [box((0, 0, 0), (16, 10, 16), "#side", textures={"up": "#top", "down": "#top", "north": "#end", "south": "#end"})]
    for x in (3.5, 11.5):
        elements.append(box((x, -0.01, -0.01), (x + 1, 10.01, 16.01), "#twine", faces=("up", "north", "south"),
                            uvs={"up": (0, 0, 1, 16), "north": (0, 0, 1, 10), "south": (0, 0, 1, 10)}))
    return fitted(block_model({"side": "hay_bale_seat_side", "top": "hay_bale_seat_top", "end": "hay_bale_seat_end",
                               "twine": "hay_bale_seat_twine"}, elements, "hay_bale_seat_side"))


# ---------------------------------------------------------------- the autumn wreath

def wreath_model(flowers):
    """A ring of leaves against a wall to the south, facing north: an octagon of bars, two crossed ears of corn at the
    bottom and a spray of `flowers` mums at the upper left."""
    z0, z1 = 13, 16
    leaves = [box((4, 12, z0), (12, 15, z1), "#leaves"), box((4, 1, z0), (12, 4, z1), "#leaves"),
              box((1, 4, z0), (4, 12, z1), "#leaves"), box((12, 4, z0), (15, 12, z1), "#leaves"),
              box((2, 11, z0 - 0.5), (5, 14, z1), "#leaves"), box((11, 11, z0 - 0.5), (14, 14, z1), "#leaves"),
              box((2, 2, z0 - 0.5), (5, 5, z1), "#leaves"), box((11, 2, z0 - 0.5), (14, 5, z1), "#leaves")]
    corn = [box((5.5, 1, 12), (7.5, 7, 13), "#corn", rotation={"origin": [6.5, 2, 12.5], "axis": "z", "angle": 22.5}),
            box((8.5, 1, 12), (10.5, 7, 13), "#corn", rotation={"origin": [9.5, 2, 12.5], "axis": "z", "angle": -22.5}),
            box((7, 0.5, 11.5), (9, 2.5, 13), "#husk")]
    mums = [box((2.5, 10.5, 11.5), (5.5, 13.5, 13), "#mum"), box((5, 12, 11.5), (8, 15, 13), "#mum"), box((1.5, 7.5, 11.5), (4, 10, 13), "#mum")]
    return fitted(block_model({"leaves": "autumn_wreath_leaves", "corn": "autumn_wreath_corn", "husk": "autumn_wreath_husk",
                               "mum": f"autumn_wreath_mum_{flowers}"}, leaves + corn + mums, "autumn_wreath_leaves"))


# ---------------------------------------------------------------- the leaf piles

def pile_model(colour, layers):
    height = layers * LEAF_PILES["layer_pixels"]
    return block_model({"top": f"{colour}_leaf_pile_top", "side": f"{colour}_leaf_pile_side"}, [
        box((0, 0, 0), (16, height, 16), "#side", textures={"up": "#top", "down": "#top"},
            uvs={side: (0, 16 - height, 16, 16) for side in SIDES}),
    ], f"{colour}_leaf_pile_top")


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    tub = BOBBING_TUB["block"]
    for n in range(BOBBING_TUB["max_apples"] + 1):
        write(models / f"{tub}_{n}.json", tub_model(n))
    write(states / f"{tub}.json", {"variants": {f"apples={n},splashing={str(s).lower()}": {"model": rid(f"block/{tub}_{n}")}
                                                for n in range(BOBBING_TUB["max_apples"] + 1) for s in (False, True)}})
    write(root / "items" / f"{tub}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{tub}_{BOBBING_TUB['max_apples']}")}})
    lang[f"block.{MOD}.{tub}"] = BOBBING_TUB["display"]
    for key, text in BOBBING_TUB["messages"].items():
        lang[f"message.{MOD}.{tub}.{key}"] = text

    crate = PUMPKIN_CRATE["block"]
    write(models / f"{crate}.json", crate_model())
    write(states / f"{crate}.json", {"variants": {f"facing={f}": turned(rid(f"block/{crate}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{crate}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{crate}")}})
    lang[f"block.{MOD}.{crate}"] = PUMPKIN_CRATE["display"]

    bale = HAY_BALE_SEAT["block"]
    write(models / f"{bale}.json", bale_model())
    write(states / f"{bale}.json", {"variants": {f"facing={f}": turned(rid(f"block/{bale}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{bale}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{bale}")}})
    lang[f"block.{MOD}.{bale}"] = HAY_BALE_SEAT["display"]
    lang[f"entity.{MOD}.{HAY_BALE_SEAT['entity']}"] = "Seat"

    wreath = AUTUMN_WREATH["block"]
    for flowers in AUTUMN_WREATH["flowers"]:
        write(models / f"{wreath}_{flowers}.json", wreath_model(flowers))
    write(states / f"{wreath}.json", {"variants": {f"facing={f},flowers={c}": turned(rid(f"block/{wreath}_{c}"), f)
                                                   for f in HORIZONTAL for c in AUTUMN_WREATH["flowers"]}})
    # The item shows the wreath with the flowers it was broken with (the loot table copies them onto it).
    write(root / "items" / f"{wreath}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "flowers",
        "cases": [{"when": c, "model": {"type": "minecraft:model", "model": rid(f"block/{wreath}_{c}")}}
                  for c in AUTUMN_WREATH["flowers"] if c != AUTUMN_WREATH["default"]],
        "fallback": {"type": "minecraft:model", "model": rid(f"block/{wreath}_{AUTUMN_WREATH['default']}")}}})
    lang[f"block.{MOD}.{wreath}"] = AUTUMN_WREATH["display"]

    for colour, display in LEAF_PILES["colours"].items():
        pile = f"{colour}_leaf_pile"
        for layers in range(1, LEAF_PILES["max_layers"] + 1):
            write(models / f"{pile}_height{layers}.json", pile_model(colour, layers))
        write(states / f"{pile}.json", {"variants": {f"layers={n}": {"model": rid(f"block/{pile}_height{n}")}
                                                     for n in range(1, LEAF_PILES["max_layers"] + 1)}})
        write(root / "items" / f"{pile}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{pile}_height2")}})
        lang[f"block.{MOD}.{pile}"] = display


def counted(block, item, prop, least, most):
    """A pool dropping `item` once for each step of the block's `prop` (from `least`, 0 or 1, to `most`)."""
    return {"condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": rid(item), "modifier": [
                {"type": "minecraft:set_count", "count": n, "condition": {"type": "minecraft:match_block", "blocks": rid(block),
                                                                         "state": {prop: str(n)}}}
                for n in range(least, most + 1) if n != 1]}],
            "rolls": 1}


def loot(out, write):
    """The tub drops itself and its apples; a wreath keeps its flowers; a pile drops one pile a layer."""
    tub = BOBBING_TUB["block"]
    apples = counted(tub, "minecraft:apple", "apples", 0, BOBBING_TUB["max_apples"])
    tub_loot = self_drop(tub)
    tub_loot["pools"].append(apples)
    write(out / f"{tub}.json", tub_loot)
    for block in (PUMPKIN_CRATE["block"], HAY_BALE_SEAT["block"]):
        write(out / f"{block}.json", self_drop(block))
    wreath = AUTUMN_WREATH["block"]
    write(out / f"{wreath}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(wreath),
                     "modifier": {"type": "minecraft:copy_state", "block": rid(wreath), "properties": ["flowers"]}}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{wreath}")})
    for pile in leaf_piles():
        write(out / f"{pile}.json", {"type": "minecraft:block", "pools": [counted(pile, pile, "layers", 1, LEAF_PILES["max_layers"])],
                                     "random_sequence": rid(f"blocks/{pile}")})


def tags(tags):
    for value in PUMPKIN_CRATE["produce"]:
        tags.add("item", PUMPKIN_CRATE["produce_tag"], value)
    for block in (BOBBING_TUB["block"], PUMPKIN_CRATE["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    for block in [HAY_BALE_SEAT["block"], AUTUMN_WREATH["block"]] + leaf_piles():
        tags.add("block", "minecraft:mineable/hoe", rid(block))
