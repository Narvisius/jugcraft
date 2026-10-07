"""JSON resources for the cider mill (fall additions 2), from tools/agriculture.py: the apple tree (its sapling, leaves that
blossom and fruit, loot and worldgen), the Cider Press and the Cider Barrel (models, blockstates, items, names, messages,
loot and tags).

Called from agriculture_data.py (assets, loot, tags, worldgen). Formats follow vanilla Minecraft 26.3's own files. The
press's moving parts (the apples in its hopper, the pulp in its basket, its screw and pressing plate, and the juice in
its trough) are drawn by the client's CiderPressRenderer; the block model is its frame.
"""
from agriculture import CIDER
from decor_data import MOD, rid, box, block_model, turned, self_drop
from festival_data import SHEARS_OR_SILK

TREE, PRESS, BARREL = CIDER["tree"], CIDER["press"], CIDER["barrel"]
LEAF_TEXTURES = ["apple_leaves", "apple_leaves_blossom", "apple_leaves_ripe"]
SIDES = ["north", "south", "east", "west"]
# The chalk mark on the barrel's head by its `cider` state: empty, then sweet, sparkling and aged.
BARREL_HEADS = ["cider_barrel_head", "cider_barrel_head_sweet", "cider_barrel_head_sparkling", "cider_barrel_head_aged"]


def press_model():
    """A cider press facing north: a trough tray, a slatted basket bound with iron hoops, two posts and a beam with the
    screw's iron nut, and behind the basket the grinder: a drum under a hopper, with a crank on its east side."""
    w, s, i = "#wood", "#slats", "#iron"
    elements = [
        # The trough: a floor and a rim.
        box((0, 0, 0), (16, 1, 16), w),
        box((0, 1, 0), (16, 2.5, 1), w), box((0, 1, 15), (16, 2.5, 16), w),
        box((0, 1, 1), (1, 2.5, 15), w), box((15, 1, 1), (16, 2.5, 15), w),
        # The basket: a slatted floor and walls, and two iron hoops round it.
        box((3, 1, 3), (13, 2, 13), s),
        box((3, 2, 3), (13, 9, 4), s), box((3, 2, 12), (13, 9, 13), s),
        box((3, 2, 4), (4, 9, 12), s), box((12, 2, 4), (13, 9, 12), s),
        box((2.75, 3, 2.75), (13.25, 4, 13.25), i, faces=SIDES),
        box((2.75, 7, 2.75), (13.25, 8, 13.25), i, faces=SIDES),
        # The frame: two posts, a beam across and the screw's nut.
        box((0.5, 2.5, 7), (2.5, 16, 9), w), box((13.5, 2.5, 7), (15.5, 16, 9), w),
        box((2.5, 13, 6.5), (13.5, 15, 9.5), w),
        box((6.5, 15, 6.5), (9.5, 16, 9.5), i),
        # The grinder: an iron-bound drum under a wooden hopper, and its crank.
        box((4, 9, 13), (12, 12, 15.5), w), box((4, 10, 12.9), (12, 11, 15.6), i, faces=["north", "south", "up"]),
        box((3.5, 12, 12.5), (12.5, 15, 13), w), box((3.5, 12, 15.5), (12.5, 15, 16), w),
        box((3.5, 12, 13), (4, 15, 15.5), w), box((12, 12, 13), (12.5, 15, 15.5), w),
        box((4, 12, 13), (12, 12.5, 15.5), i),
        box((12, 10, 13.75), (14, 11, 14.75), i), box((14, 8, 13.75), (15, 11, 14.75), i),
        box((14, 7.5, 12.25), (15, 8.5, 14.75), w),
    ]
    return block_model({"wood": "cider_press_wood", "slats": "cider_press_slats", "iron": "cider_press_iron"}, elements,
                       "cider_press_wood")


def barrel_model(head):
    """An oak cask on its side along the north-south axis, in a cradle: its head (with the chalk mark) to the north with a
    brass tap, three iron hoops, a bung on top. Three boxes, each a step narrower and taller than the last, round its
    profile; their ends together show the whole round head."""
    st, sd, hd, ir, br, wd = "#staves", "#side", "#head", "#iron", "#brass", "#wood"
    ends = {"north": hd, "south": hd, "east": sd, "west": sd}
    # (x0, y0, x1, y1, z0, z1) of each body box: tall and narrow, square, wide and low.
    body = [(4, 2, 12, 14, 0.5, 15.5), (3, 3, 13, 13, 0.75, 15.25), (2, 4, 14, 12, 1, 15)]
    elements = [
        # The cradle: two saddles across the barrel.
        box((1, 0, 2.5), (15, 2.5, 4.5), wd), box((1, 0, 11.5), (15, 2.5, 13.5), wd),
        box((1, 2.5, 2.5), (2.5, 5, 4.5), wd), box((13.5, 2.5, 2.5), (15, 5, 4.5), wd),
        box((1, 2.5, 11.5), (2.5, 5, 13.5), wd), box((13.5, 2.5, 11.5), (15, 5, 13.5), wd),
    ]
    elements += [box((x0, y0, z0), (x1, y1, z1), st, textures=ends) for x0, y0, x1, y1, z0, z1 in body]
    # Iron hoops near each end and round the belly, a quarter pixel proud of each body box.
    for z in (2, 7.5, 13):
        elements += [box((x0 - 0.25, y0 - 0.25, z), (x1 + 0.25, y1 + 0.25, z + 1), ir, faces=["up", "down", "east", "west"])
                     for x0, y0, x1, y1, _, _ in body]
    # The brass tap low on the head, and the bung.
    elements += [box((7.5, 4, -1), (8.5, 5, 0.5), br), box((7, 5, -0.75), (9, 6, 0), br), box((7, 14, 7), (9, 14.5, 9), wd)]
    return block_model({"staves": "cider_barrel_staves", "side": "cider_barrel_staves_side", "head": head, "iron": "cider_press_iron",
                        "brass": "cider_barrel_brass", "wood": "cider_press_wood"}, elements, "cider_barrel_staves")


TEXT = {
    "message.jugcraft.cider_press.full": "The hopper is full",
    "message.jugcraft.cider_press.pressing": "Finish pressing first: turn the screw",
    "message.jugcraft.cider_press.no_juice": "There's no juice in the trough",
    "message.jugcraft.cider_press.trough_full": "The trough is full: bottle some juice first",
    "message.jugcraft.cider_press.turned": "The screw goes down: %s of %s turns",
    "message.jugcraft.cider_press.pressed_out": "Pressed dry! Out comes the pomace (%s)",
    "message.jugcraft.cider_press.status": "Cider Press: %s apples to grind, %s ground, %s of %s servings of juice",
    "message.jugcraft.cider_barrel.full": "The barrel is full",
    "message.jugcraft.cider_barrel.fermenting": "It's already fermenting: draw it off before adding fresh juice",
    "message.jugcraft.cider_barrel.empty": "The barrel is empty",
    "message.jugcraft.cider_barrel.ageing": "Cider Barrel: %s of %s servings of %s, %s in about %s minutes",
    "message.jugcraft.cider_barrel.aged": "Cider Barrel: %s of %s servings of %s, as good as it gets",
    "tooltip.jugcraft.cider_barrel.servings": "Holds %s of %s servings of cider (still ageing)",
}


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    # The apple tree: a sapling, and leaves (untinted, so the blossom and apples keep their colours) by fruit stage.
    sapling, leaves = TREE["sapling"], TREE["leaves"]
    write(models / f"{sapling}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{sapling}")}})
    write(states / f"{sapling}.json", {"variants": {"": {"model": rid(f"block/{sapling}")}}})
    for texture in LEAF_TEXTURES:
        write(models / f"{texture}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{texture}")}})
    write(states / f"{leaves}.json", {"variants": {
        f"fruit={fruit}": {"model": rid(f"block/{texture}")} for fruit, texture in enumerate(LEAF_TEXTURES)}})
    write(items / f"{leaves}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{leaves}")}})
    for block, display in TREE["display"].items():
        lang[f"block.{MOD}.{block}"] = display

    press = PRESS["block"]
    write(models / f"{press}.json", press_model())
    write(states / f"{press}.json", {"variants": {f"facing={f}": turned(rid(f"block/{press}"), f) for f in ("north", "south", "east", "west")}})
    write(items / f"{press}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{press}")}})
    lang[f"block.{MOD}.{press}"] = PRESS["display"]

    barrel = BARREL["block"]
    for head in BARREL_HEADS:
        name = barrel if head == "cider_barrel_head" else f"{barrel}_{head.rsplit('_', 1)[1]}"
        write(models / f"{name}.json", barrel_model(head))
    write(states / f"{barrel}.json", {"variants": {
        f"cider={c},facing={f}": turned(rid(f"block/{barrel}" + ("" if c == 0 else f"_{BARREL_HEADS[c].rsplit('_', 1)[1]}")), f)
        for c in range(len(BARREL_HEADS)) for f in ("north", "south", "east", "west")}})
    write(items / f"{barrel}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{barrel}")}})
    lang[f"block.{MOD}.{barrel}"] = BARREL["display"]
    lang.update(TEXT)


def loot(out, write):
    """Apple leaves drop as oak leaves do (seeds for a sapling, sticks, now and then an apple) and their ripe apples; the
    sapling gives its seeds back; the press drops itself; the barrel drops itself with its cider."""
    leaves, seeds = TREE["leaves"], rid(TREE["seed"])
    pick = TREE["pick"]
    not_shears = {"type": "minecraft:inverted", "term": SHEARS_OR_SILK}
    write(out / f"{leaves}.json", {"type": "minecraft:block", "pools": [
        {"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": SHEARS_OR_SILK, "name": rid(leaves)},
            {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:table_bonus", "chances": [0.05, 0.0625, 0.083333336, 0.1], "enchantment": "minecraft:fortune"}]},
             "name": seeds}]}], "rolls": 1},
        {"condition": not_shears, "entries": [{"type": "minecraft:item", "condition": {
            "type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1], "enchantment": "minecraft:fortune"},
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                         {"type": "minecraft:explosion_decay"}], "name": "minecraft:stick"}], "rolls": 1},
        {"condition": not_shears, "entries": [{"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:survives_explosion"},
            {"type": "minecraft:table_bonus", "chances": [0.005, 0.0055555557, 0.00625, 0.008333334, 0.025],
             "enchantment": "minecraft:fortune"}]}, "name": "minecraft:apple"}], "rolls": 1},
        {"condition": {"type": "minecraft:match_block", "blocks": rid(leaves), "state": {"fruit": "2"}},
         "entries": [{"type": "minecraft:item", "name": pick["item"], "modifier": [
             {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": pick["min"], "max": pick["max"]}},
             {"type": "minecraft:explosion_decay"}]}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{leaves}")})
    sapling = self_drop(TREE["sapling"])
    sapling["pools"][0]["entries"][0]["name"] = seeds
    write(out / f"{TREE['sapling']}.json", sapling)
    write(out / f"{PRESS['block']}.json", self_drop(PRESS["block"]))
    barrel = self_drop(BARREL["block"])
    barrel["pools"][0]["entries"][0]["modifier"] = [{"type": "minecraft:copy_components", "source": "block_entity",
                                                     "include": [rid("barrel_cider")]}]
    write(out / f"{BARREL['block']}.json", barrel)


def tags(tags):
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:leaves", rid(TREE["leaves"]))
    tags.add("block", "minecraft:saplings", rid(TREE["sapling"]))
    for block in (PRESS["block"], BARREL["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    for item in PRESS["apple_items"]:
        tags.add("item", PRESS["apples"], item)


def worldgen(data, write):
    """The apple tree: a short oak trunk with a rounded crown of apple leaves; found in plains and flower-rich places."""
    folder = data / MOD / "worldgen"
    trunk, foliage = TREE["trunk"], TREE["foliage"]
    write(folder / "feature" / "apple_tree.json", {
        "type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": [],
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": foliage["height"], "offset": 0, "radius": foliage["radius"]},
        "foliage_provider": {"id": rid(TREE["leaves"]),
                             "properties": {"distance": "7", "fruit": "0", "persistent": "false", "waterlogged": "false"}},
        "ignore_vines": True, "minimum_size": {"type": "minecraft:two_layers_feature_size"},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": trunk["base_height"],
                         "height_rand_a": trunk["height_rand_a"], "height_rand_b": 0},
        "trunk_provider": {"id": "minecraft:oak_log", "properties": {"axis": "y"}}})
    write(folder / "placed_feature" / "patch_apple_tree.json", {"feature": rid("apple_tree"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": TREE["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": rid(TREE["sapling"])}},
    ]})
