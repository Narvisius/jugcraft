"""JSON resources for the thirteenth batch of Halloween decorations, treats, from tools/agriculture.py: the Witch's Brew
Punch Bowl, the Barmbrack and its ring, and Giant Candy props; their names, messages, loot and tags. The treats
themselves (soul cakes, pumpkin bread, cupcakes, cookies, the latte and the punch) are foods in ITEMS, whose item
models and names agriculture_data.py writes with the others.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py to decor12_data.py). The Barmbrack is modelled facing north and eaten from its west end,
like a vanilla cake; the blockstate turns it.
"""
from agriculture import PUNCH_BOWL, BARMBRACK, GIANT_CANDY
from decor_data import MOD, HORIZONTAL, SIDES, rid, box, block_model, self_drop, flat_item, turned
from decor3_data import fitted
from decor9_data import FULL

PUNCH_TEXTURES = {"glass": "punch_bowl_glass", "stand": "punch_bowl_stand", "punch": "punch_bowl_punch", "eye": "punch_bowl_eye",
                  "ladle": "punch_bowl_ladle"}
# The punch's height in the bowl by how full it is (none, low, half, full), and the servings each level shows from.
PUNCH_LEVELS = {"empty": None, "low": 5.5, "half": 7.5, "full": 9.25}


def punch_level(servings):
    if servings <= 0:
        return "empty"
    third = PUNCH_BOWL["servings"] / 3
    return "low" if servings <= third else "half" if servings <= 2 * third else "full"


def punch_bowl(level):
    """A wide glass bowl on a black iron tripod stand, with glowing green punch to `level`; eyeballs float in a full
    bowl and an iron ladle leans in it while there is punch to ladle."""
    s, g = "#stand", "#glass"
    elements = [box((3, 0, 3), (5, 1, 5), s), box((11, 0, 3), (13, 1, 5), s), box((7, 0, 11), (9, 1, 13), s),
                box((4, 1, 4), (5, 3, 5), s, faces=SIDES), box((11, 1, 4), (12, 3, 5), s, faces=SIDES), box((7.5, 1, 11), (8.5, 3, 12), s, faces=SIDES),
                box((3, 3, 3), (13, 4, 13), s),
                box((2, 4, 2), (14, 4.5, 14), g),
                box((1, 4, 1), (15, 10, 2), g), box((1, 4, 14), (15, 10, 15), g),
                box((1, 4, 2), (2, 10, 14), g), box((14, 4, 2), (15, 10, 14), g)]
    height = PUNCH_LEVELS[level]
    if height:
        elements.append(box((2.1, 4.5, 2.1), (13.9, height, 13.9), "#punch", light=10))
        elements.append(box((9.5, height - 0.5, 4), (12, height, 6.5), "#ladle"))
        elements.append(box((12, height - 0.5, 4.75), (12.75, 14, 5.75), "#ladle", faces=SIDES + ("up",),
                            rotation={"origin": [12, height, 5], "axis": "z", "angle": -22.5}))
    if level == "full":
        eye = {side: FULL for side in SIDES + ("up",)}
        elements.append(box((4, height - 1, 8), (6, height + 1, 10), "#eye", faces=SIDES + ("up",), uvs=eye))
        elements.append(box((7, height - 1, 4), (9, height + 1, 6), "#eye", faces=SIDES + ("up",), uvs=eye,
                            rotation={"origin": [8, height, 5], "axis": "y", "angle": 45}))
    return block_model(PUNCH_TEXTURES, elements, PUNCH_TEXTURES["glass"])


BRACK_TEXTURES = {"crust": "barmbrack_crust", "top": "barmbrack_top", "inside": "barmbrack_inside", "end": "barmbrack_end"}


def barmbrack(bites):
    """A glazed fruit loaf lying along x, with `bites` of its six slices cut from its west end (showing the crumb)."""
    cut = 1 + bites * 14 / BARMBRACK["slices"]
    cut = round(cut, 3)
    west = "#inside" if bites else "#end"
    elements = [box((cut, 0, 4), (15, 6, 12), "#crust", textures={"up": "#top", "west": west, "east": "#end", "down": "#end"}),
                box((max(cut, 1.5), 6, 4.5), (14.5, 7, 11.5), "#top", textures={"west": west if bites else "#top"})]
    return block_model(BRACK_TEXTURES, elements, BRACK_TEXTURES["crust"])


CANDY_TEXTURES = {
    "candy_corn": {"white": "giant_candy_white", "orange": "giant_candy_orange", "yellow": "giant_candy_yellow"},
    "lollipop": {"swirl": "giant_lollipop_swirl", "edge": "giant_lollipop_edge", "stick": "giant_lollipop_stick"},
    "wrapped_candy": {"wrapper": "giant_wrapped_candy", "twist": "giant_wrapped_candy_twist"},
    "gumdrop": {"sugar": "giant_gumdrop"},
}


def giant_candy(design):
    """A sweet as big as a block, facing north."""
    if design == "candy_corn":
        # A kernel, broad and flat: a yellow base, an orange middle and a white tip, each a little narrower.
        elements = [box((2, 0, 4.5), (14, 5, 11.5), "#yellow"), box((3, 5, 5), (13, 10, 11), "#orange"),
                    box((4.5, 10, 5.5), (11.5, 14, 10.5), "#white"), box((6, 14, 6), (10, 15.5, 10), "#white")]
    elif design == "lollipop":
        # A round swirl taller than a block on a paper stick, standing in a sugar-crusted foot.
        swirl = {"north": FULL, "south": FULL}
        elements = [box((5, 0, 5), (11, 2, 11), "#edge"), box((7.25, 2, 7.25), (8.75, 10, 8.75), "#stick", faces=SIDES),
                    box((2, 9, 7), (14, 21, 9), "#edge", textures={"north": "#swirl", "south": "#swirl"}, uvs=swirl),
                    box((3, 8, 7.25), (13, 9, 8.75), "#edge"), box((3, 21, 7.25), (13, 22, 8.75), "#edge"),
                    box((1, 10, 7.25), (2, 20, 8.75), "#edge"), box((14, 10, 7.25), (15, 20, 8.75), "#edge")]
    elif design == "wrapped_candy":
        # A round sweet in a shiny wrapper, twisted shut at both ends into fans.
        elements = [box((3.5, 0, 4), (12.5, 8, 12), "#wrapper"),
                    box((2, 2.5, 6.5), (3.5, 5.5, 9.5), "#twist"), box((12.5, 2.5, 6.5), (14, 5.5, 9.5), "#twist"),
                    box((0, 0.5, 4.5), (2, 7.5, 11.5), "#twist"), box((14, 0.5, 4.5), (16, 7.5, 11.5), "#twist")]
    else:
        # A sugared dome, stepped round.
        elements = [box((2, 0, 2), (14, 6, 14), "#sugar"), box((3, 6, 3), (13, 10, 13), "#sugar"),
                    box((4.5, 10, 4.5), (11.5, 12.5, 11.5), "#sugar"), box((6, 12.5, 6), (10, 13.5, 10), "#sugar")]
    textures = CANDY_TEXTURES[design]
    return fitted(block_model(textures, elements, next(iter(textures.values()))))


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"

    def item_model(name, model_name):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model_name}")}})

    bowl = PUNCH_BOWL["block"]
    for level in PUNCH_LEVELS:
        write(models / f"{bowl}_{level}.json", punch_bowl(level))
    write(states / f"{bowl}.json", {"variants": {f"servings={n}": {"model": rid(f"block/{bowl}_{punch_level(n)}")}
                                                 for n in range(PUNCH_BOWL["servings"] + 1)}})
    item_model(bowl, f"{bowl}_full")
    lang[f"block.{MOD}.{bowl}"] = PUNCH_BOWL["display"]

    brack = BARMBRACK["block"]
    for bites in range(BARMBRACK["slices"]):
        write(models / f"{brack}_slice{bites}.json" if bites else models / f"{brack}.json", barmbrack(bites))
    write(states / f"{brack}.json", {"variants": {
        f"bites={bites},facing={facing}": turned(rid(f"block/{brack}_slice{bites}" if bites else f"block/{brack}"), facing)
        for facing in HORIZONTAL for bites in range(BARMBRACK["slices"])}})
    flat_item(root, write, brack)
    lang[f"block.{MOD}.{brack}"] = BARMBRACK["display"]
    ring = BARMBRACK["ring"]
    flat_item(root, write, ring)
    lang[f"item.{MOD}.{ring}"] = BARMBRACK["ring_display"]
    fortunes = {"ring": "You found the ring in your slice! You'll be married within the year",
                "coin": "A coin in your slice: a year of good fortune", "pea": "A pea in your slice: no wedding this year",
                "stick": "A stick in your slice: a year of quarrels", "cloth": "A rag in your slice: a year of hard times",
                "crumbs": "Just fruit and crumbs: the year ahead is yours to make"}
    for key in ["ring"] + BARMBRACK["fortunes"]:
        lang[f"message.{MOD}.{brack}.{key}"] = fortunes[key]

    candy = GIANT_CANDY["block"]
    for design in GIANT_CANDY["designs"]:
        write(models / f"{candy}_{design}.json", giant_candy(design))
    write(states / f"{candy}.json", {"variants": {
        f"design={design},facing={facing}": turned(rid(f"block/{candy}_{design}"), facing)
        for facing in HORIZONTAL for design in GIANT_CANDY["designs"]}})
    item_model(candy, f"{candy}_{GIANT_CANDY['designs'][0]}")
    lang[f"block.{MOD}.{candy}"] = GIANT_CANDY["display"]


def loot(out, write):
    """The bowl and the candy drop themselves (the punch in the bowl is lost); the barmbrack only while it is whole."""
    for block in (PUNCH_BOWL["block"], GIANT_CANDY["block"]):
        write(out / f"{block}.json", self_drop(block))
    brack = BARMBRACK["block"]
    write(out / f"{brack}.json", self_drop(brack, {"type": "minecraft:match_block", "blocks": rid(brack), "state": {"bites": "0"}}))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(GIANT_CANDY["block"]))
    for item in PUNCH_BOWL["ingredients"]:
        tags.add("item", "jugcraft:witchs_brew_ingredients", item)
