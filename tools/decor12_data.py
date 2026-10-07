"""JSON resources for the twelfth batch of Halloween decorations, night events, from tools/agriculture.py: the
trick-or-treaters' thank-you gifts, Toilet Paper Rolls and their streamers, the Haunted Hayride and the Halloween
Bonfire with marshmallows to toast; their names, messages, loot and tags, and the quads the client draws the hayride
from (assets/jugcraft/decor12_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py to decor11_data.py). A hanging streamer is two crossed sheets, like a plant; a draped one
sits in the block above a fence or wall and hangs down over it (below its own block). The hayride's wagon is modelled
along x about the middle of the block, as the client turns it with its rails.
"""
from agriculture import TRICK_OR_TREATERS, TOILET_PAPER, HAYRIDE, BONFIRE
from decor_data import MOD, rid, box, block_model, self_drop, flat_item
from decor3_data import fitted
from decor6_data import quads
from decor9_data import pinned, FULL

PAPER_TEXTURES = {"paper": "toilet_paper_streamer", "drape": "toilet_paper_drape"}
SHEET = ("north", "south")


def hanging():
    """Two crossed sheets of paper strips hanging the height of the block."""
    turn = {"origin": [8, 8, 8], "axis": "y"}
    return [box((0.8, 0, 8), (15.2, 16, 8), "#paper", faces=SHEET, uvs={s: FULL for s in SHEET}, rotation={**turn, "angle": 45}),
            box((0.8, 0, 8), (15.2, 16, 8), "#paper", faces=SHEET, uvs={s: FULL for s in SHEET}, rotation={**turn, "angle": -45})]


def draped():
    """A strip over the top of the fence or wall below, hanging down both its sides."""
    return [box((0, 0, 6.5), (16, 0.25, 9.5), "#drape", faces=("up", "down"), uvs={"up": FULL, "down": FULL}),
            box((0, -9, 6.4), (16, 0, 6.4), "#drape", faces=SHEET, uvs={s: FULL for s in SHEET}),
            box((0, -9, 9.6), (16, 0, 9.6), "#drape", faces=SHEET, uvs={s: FULL for s in SHEET})]


BONFIRE_TEXTURES = {"stone": "halloween_bonfire_stone", "log": "halloween_bonfire_log", "log_end": "halloween_bonfire_log_end",
                    "embers": "halloween_bonfire_embers", "embers_out": "halloween_bonfire_embers_out", "skewer": "halloween_bonfire_skewer"}


def bonfire(lit):
    """A ring of stones round a bed of embers, four logs leaning together into a cone over it, and four skewers leaning
    in over the fire from outside the ring, their tips where the client draws the food."""
    s, l = "#stone", "#log"
    ends = {"up": "#log_end", "down": "#log_end"}
    elements = [box((0.5, 0, 0.5), (4, 2.5, 4), s), box((12, 0, 0.5), (15.5, 2.5, 4), s), box((0.5, 0, 12), (4, 2.5, 15.5), s),
                box((12, 0, 12), (15.5, 2.5, 15.5), s), box((5.5, 0, 0), (10.5, 2, 2.5), s), box((5.5, 0, 13.5), (10.5, 2, 16), s),
                box((0, 0, 5.5), (2.5, 2, 10.5), s), box((13.5, 0, 5.5), (16, 2, 10.5), s),
                box((3, 0, 3), (13, 1.5, 13), "#embers" if lit else "#embers_out", light=15 if lit else None),
                box((6.75, 0.5, 2), (9.25, 14.5, 4.5), l, textures=ends, rotation={"origin": [8, 0.5, 3.25], "axis": "x", "angle": 22.5}),
                box((6.75, 0.5, 11.5), (9.25, 14.5, 14), l, textures=ends, rotation={"origin": [8, 0.5, 12.75], "axis": "x", "angle": -22.5}),
                box((2, 0.5, 6.75), (4.5, 14.5, 9.25), l, textures=ends, rotation={"origin": [3.25, 0.5, 8], "axis": "z", "angle": -22.5}),
                box((11.5, 0.5, 6.75), (14, 14.5, 9.25), l, textures=ends, rotation={"origin": [12.75, 0.5, 8], "axis": "z", "angle": 22.5})]
    k = "#skewer"
    elements += [box((7.75, 4, -1.5), (8.25, 4.5, 6.5), k, rotation={"origin": [8, 4, -1.5], "axis": "x", "angle": -45}),
                 box((7.75, 4, 9.5), (8.25, 4.5, 17.5), k, rotation={"origin": [8, 4, 17.5], "axis": "x", "angle": 45}),
                 box((-1.5, 4, 7.75), (6.5, 4.5, 8.25), k, rotation={"origin": [-1.5, 4, 8], "axis": "z", "angle": 45}),
                 box((9.5, 4, 7.75), (17.5, 4.5, 8.25), k, rotation={"origin": [17.5, 4, 8], "axis": "z", "angle": -45})]
    return elements


HAYRIDE_TEXTURES = {"planks": "hayride_planks", "hay_side": "hayride_hay_side", "hay_top": "hayride_hay_top", "wheel": "hayride_wheel",
                    "iron": "hayride_iron", "pumpkin": "hayride_pumpkin", "face": "hayride_pumpkin_face"}
# Quads drawn with these textures are drawn cut out (the spaces between the spokes).
HAYRIDE_CUTOUT = {"hayride_wheel"}


def wagon():
    """A plank wagon bed with low sides on two axles and four spoked wheels, and hay bales along both sides to sit on."""
    p = "#planks"
    hay = {"up": "#hay_top", "down": "#hay_top"}
    wheel = {"north": FULL, "south": FULL}
    elements = [box((-6, 5, 1), (22, 7, 15), p), box((-6, 7, 0), (22, 10, 1), p), box((-6, 7, 15), (22, 10, 16), p),
                box((-6, 7, 1), (-5, 10, 15), p), box((21, 7, 1), (22, 10, 15), p),
                box((-4, 7, 1), (20, 11, 5), "#hay_side", textures=hay), box((-4, 7, 11), (20, 11, 15), "#hay_side", textures=hay),
                box((-4, 7, 5), (20, 7.5, 11), "#hay_top", faces=("up",)),
                box((0, 3, -1), (2, 5, 17), "#iron"), box((14, 3, -1), (16, 5, 17), "#iron"),
                box((20, 10, 7.25), (21.5, 23, 8.75), p), box((21.5, 21.5, 7.25), (24.5, 23, 8.75), p)]
    for x in (-3, 11):
        for z0, z1 in ((-1.5, -1), (17, 17.5)):
            elements.append(box((x, 0, z0), (x + 8, 8, z1), "#wheel", faces=SHEET, uvs=wheel))
    return elements


def lantern():
    """A jack o'lantern hanging from the post's arm, grinning forward (+x)."""
    return [box((22, 16, 5.5), (27, 21, 10.5), "#pumpkin", textures={"east": "#face"}, uvs={"east": FULL}),
            box((24.25, 21, 7.75), (24.75, 21.5, 8.25), "#iron")]


def hayride_quads(elements):
    out = quads(pinned(elements), HAYRIDE_TEXTURES)
    for quad in out:
        if quad["texture"] in HAYRIDE_CUTOUT:
            quad["cutout"] = True
    return out


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"

    def item_model(name, model_name):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model_name}")}})

    streamer = TOILET_PAPER["block"]
    write(models / f"{streamer}.json", block_model(PAPER_TEXTURES, hanging(), PAPER_TEXTURES["paper"]))
    write(models / f"{streamer}_draped.json", block_model(PAPER_TEXTURES, draped(), PAPER_TEXTURES["drape"]))
    write(states / f"{streamer}.json", {"variants": {"draped=false": {"model": rid(f"block/{streamer}")},
                                                     "draped=true": {"model": rid(f"block/{streamer}_draped")}}})
    item_model(streamer, streamer)
    lang[f"block.{MOD}.{streamer}"] = TOILET_PAPER["block_display"]
    roll = TOILET_PAPER["item"]
    flat_item(root, write, roll)
    lang[f"item.{MOD}.{roll}"] = TOILET_PAPER["display"]
    lang[f"entity.{MOD}.{roll}"] = TOILET_PAPER["display"]

    ride = HAYRIDE["item"]
    flat_item(root, write, ride)
    lang[f"item.{MOD}.{ride}"] = HAYRIDE["display"]
    lang[f"entity.{MOD}.{ride}"] = HAYRIDE["display"]

    fire = BONFIRE["block"]
    for lit in (False, True):
        write(models / f"{fire}{'' if lit else '_out'}.json", fitted(block_model(BONFIRE_TEXTURES, bonfire(lit), BONFIRE_TEXTURES["log"])))
    write(states / f"{fire}.json", {"variants": {f"lit={str(lit).lower()}": {"model": rid(f"block/{fire}{'' if lit else '_out'}")}
                                                 for lit in (False, True)}})
    item_model(fire, fire)
    lang[f"block.{MOD}.{fire}"] = BONFIRE["display"]
    stick = BONFIRE["stick"]
    write(root / "models" / "item" / f"{stick}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{stick}")}})
    write(items / f"{stick}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{stick}")}})
    lang[f"item.{MOD}.{stick}"] = BONFIRE["stick_display"]
    lang[f"message.{MOD}.{stick}.no_fire"] = "Hold it over a lit bonfire or campfire to toast it"

    for key, text in {"knock": "Knock, knock: \"Trick or treat!\"", "thanks": "The trick-or-treaters take their treats and leave you a thank-you gift",
                      "prank": "The candy bowl is empty! The trick-or-treaters run off giggling... and your trees are covered in toilet paper"}.items():
        lang[f"message.{MOD}.trick_or_treaters.{key}"] = text

    write(root / "decor12_quads.json", {"hayride_wagon": hayride_quads(wagon()), "hayride_lantern": hayride_quads(lantern())})


def loot(out, write):
    """The bonfire drops itself; streamers drop nothing (an empty table, which packs may fill); the trick-or-treaters'
    gift table."""
    write(out / f"{BONFIRE['block']}.json", self_drop(BONFIRE["block"]))
    streamer = TOILET_PAPER["block"]
    write(out / f"{streamer}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{streamer}")})
    table = TRICK_OR_TREATERS["gift_table"]
    entries = [{"type": "minecraft:item", "name": item, "weight": weight} for item, weight in TRICK_OR_TREATERS["gifts"]]
    write(out.parent / f"{table}.json", {"type": "minecraft:gift", "pools": [{"entries": entries, "rolls": 1}], "random_sequence": rid(table)})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(BONFIRE["block"]))
    for value in ("#minecraft:leaves", "#minecraft:logs"):
        tags.add("block", "jugcraft:toilet_paper_hangs_from", value)
    for value in ("#minecraft:fences", "#minecraft:walls"):
        tags.add("block", "jugcraft:toilet_paper_drapes_over", value)
