"""JSON resources for rice and wet farming (tools/rice.py), in the owner's own textures: wild rice, the Bag of Rice, the rice
and straw bales, the tatami and its mats, and the Rice Roll Medley (its models, blockstates, loot, worldgen and names). The
rice plant is one of tools/agriculture.py's TALL_CROPS, so tools/agriculture_data.py writes it with the other tall crops;
the rice dishes set down through tools/menu_data.py; the items, recipes and cutting come from tools/agriculture.py and
tools/kitchen.py.

Called from agriculture_data.py (assets, loot, tags, worldgen). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import BOG_SOIL_TAG
from decor_data import MOD, rid, turned, self_drop
from rice import WILD_RICE, STORAGE, TATAMI, TATAMI_MATS, MEDLEY

SIDES = ("north", "south", "east", "west")


def model(parent, **textures):
    return {"parent": parent, "textures": {key: rid(f"block/{value}") for key, value in textures.items()}}


def face(uv, texture):
    return {"texture": texture, "uv": [round(v, 3) for v in uv]}


def box(lo, hi, faces):
    return {"from": list(lo), "to": list(hi), "faces": {side: face(uv, texture) for side, (uv, texture) in faces.items()}}


def paired_variant(model_name, partner):
    """A tatami half whose partner lies at `partner`: the owner's even half joins to the east and the odd half to the west,
    so the half whose partner is east or south shows the even half (turned 90 degrees for south), the other the odd."""
    half, y = {"east": ("even", 0), "south": ("even", 90), "west": ("odd", 0), "north": ("odd", 90)}[partner]
    variant = {"model": rid(f"block/{model_name}_{half}")}
    if y:
        variant["y"] = y
    return variant


def mat(top):
    """A tatami mat a pixel thick: the owner's mat on top and its woven edge round the sides."""
    side = ([0, 15, 16, 16], "#side")
    return {"parent": "minecraft:block/block", "textures": {"particle": rid(f"block/{top}"), "top": rid(f"block/{top}"),
                                                            "side": rid("block/tatami_mat_side")},
            "elements": [box((0, 0, 0), (16, 1, 16), {"up": ([0, 0, 16, 16], "#top"), "down": ([0, 0, 16, 16], "#top"),
                                                      **{s: side for s in SIDES}})]}


# ---------------------------------------------------------------- the rice roll medley

# Where each place's roll lies on the platter (x and z from and to), by tools/rice.py MEDLEY "pieces": nigiri in a row at
# the back, kelp roll slices along the front.
PLACES = [((1.5, 10.5), (4.5, 13.5)), ((5, 10.5), (8, 13.5)), ((9.5, 2), (11.5, 9)), ((2.5, 2), (4.5, 9)),
          ((8.5, 10.5), (11.5, 13.5)), ((12, 10.5), (15, 13.5)), ((12.5, 2), (14.5, 9)), ((5.5, 2), (7.5, 9))]
# The owner's medley texture: a salmon nigiri's top (2 x 7) and side (7 x 2), a cod nigiri's beside it, and a kelp roll
# slice's face (3 x 3) above its green side.
NIGIRI = {"salmon_roll": 0, "cod_roll": 7}


def roll(piece, lo, hi):
    (x0, z0), (x1, z1) = lo, hi
    if piece == "kelp_roll_slice":
        side = ([0, 13, 3, 14.5], "#medley")
        return box((x0, 1, z0), (x1, 2.5, z1), {"up": ([0, 10, 3, 13], "#medley"), "down": ([0, 10, 3, 13], "#medley"),
                                                **{s: side for s in SIDES}})
    u = NIGIRI[piece]
    return box((x0, 1, z0), (x1, 3, z1), {"up": ([u, 0, u + 2, 7], "#medley"), "down": ([u, 0, u + 2, 7], "#medley"),
                                          "east": ([u, 0, u + 7, 2], "#medley"), "west": ([u, 0, u + 7, 2], "#medley"),
                                          "north": ([u, 0, u + 2, 2], "#medley"), "south": ([u, 0, u + 2, 2], "#medley")})


def medley(rolls):
    platter = box((1, 0, 1), (15, 1, 15), {"up": ([1, 1, 15, 15], "#platter"), "down": ([1, 1, 15, 15], "#platter"),
                                           **{s: ([1, 0, 15, 1], "#platter") for s in SIDES}})
    elements = [platter] + [roll(MEDLEY["pieces"][place], *PLACES[place]) for place in range(rolls)]
    return {"parent": "minecraft:block/block", "textures": {"particle": rid(f"block/{MEDLEY['platter']}"),
                                                            "platter": rid(f"block/{MEDLEY['platter']}"),
                                                            "medley": rid("block/rice_roll_medley")}, "elements": elements}


# ---------------------------------------------------------------- writers

def flat_item(root, write, name, texture=None):
    write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture or rid(f"item/{name}")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})


def block_item(root, write, name, block_model=None):
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block_model or name}")}})


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    wild, textures = WILD_RICE["block"], WILD_RICE["textures"]
    for half, texture in textures.items():
        write(models / f"{texture}.json", model("minecraft:block/cross", cross=texture))
    write(states / f"{wild}.json", {"variants": {f"half={half}": {"model": rid(f"block/{texture}")} for half, texture in textures.items()}})
    flat_item(root, write, wild, rid(f"block/{textures['upper']}"))
    lang[f"block.{MOD}.{wild}"] = WILD_RICE["display"]

    for name, info in STORAGE.items():
        if info["kind"] == "bag":
            write(models / f"{name}.json", model("minecraft:block/orientable_with_bottom", top="rice_bag_top", bottom="rice_bag_bottom",
                                                 side="rice_bag_side", front="rice_bag_side_tied", particle="rice_bag_side"))
            write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in SIDES}})
        elif name == "straw_bale":
            write(models / f"{name}.json", model("minecraft:block/cube_column", end="straw_bale_end", side="straw_bale_side"))
            write(models / f"{name}_horizontal.json", model("minecraft:block/cube_column_horizontal", end="straw_bale_end",
                                                            side="straw_bale_side"))
            write(states / f"{name}.json", {"variants": {
                "axis=y": {"model": rid(f"block/{name}")},
                "axis=z": {"model": rid(f"block/{name}_horizontal"), "x": 90},
                "axis=x": {"model": rid(f"block/{name}_horizontal"), "x": 90, "y": 90}}})
        else:
            # The rice bale stands its panicles up, its stalks below; laid on its side it turns over as a hay bale does.
            write(models / f"{name}.json", model("minecraft:block/cube_bottom_top", top="rice_bale_top", bottom="rice_bale_bottom",
                                                 side="rice_bale_side"))
            write(states / f"{name}.json", {"variants": {
                "axis=y": {"model": rid(f"block/{name}")},
                "axis=z": {"model": rid(f"block/{name}"), "x": 90},
                "axis=x": {"model": rid(f"block/{name}"), "x": 90, "y": 90}}})
        block_item(root, write, name)
        lang[f"block.{MOD}.{name}"] = info["display"]

    name = TATAMI["block"]
    write(models / f"{name}.json", model("minecraft:block/cube_all", all="tatami"))
    for half in ("even", "odd"):
        write(models / f"{name}_{half}.json", model("minecraft:block/cube_bottom_top", top=f"tatami_{half}", bottom="tatami", side="tatami"))
    variants = {f"facing={f},paired=false": turned(rid(f"block/{name}"), f) for f in SIDES}
    variants.update({f"facing={f},paired=true": paired_variant(name, f) for f in SIDES})
    write(states / f"{name}.json", {"variants": variants})
    block_item(root, write, name)
    lang[f"block.{MOD}.{name}"] = TATAMI["display"]

    for name, info in TATAMI_MATS.items():
        if info["length"] == 2:
            for half in ("even", "odd"):
                write(models / f"{name}_{half}.json", mat(f"tatami_mat_{half}"))
            # The foot's partner is ahead (facing), the head's behind it.
            opposite = {"north": "south", "south": "north", "east": "west", "west": "east"}
            write(states / f"{name}.json", {"variants": {
                f"facing={f},part={part}": paired_variant(name, f if part == "foot" else opposite[f])
                for f in SIDES for part in ("foot", "head")}})
        else:
            write(models / f"{name}.json", mat("tatami_mat_half"))
            write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in SIDES}})
        flat_item(root, write, name)
        lang[f"block.{MOD}.{name}"] = info["display"]

    name = MEDLEY["block"]
    for rolls in range(len(MEDLEY["pieces"]) + 1):
        write(models / f"{name}_{rolls}.json", medley(rolls))
    write(states / f"{name}.json", {"variants": {f"facing={f},rolls={r}": turned(rid(f"block/{name}_{r}"), f)
                                                 for f in SIDES for r in range(len(MEDLEY["pieces"]) + 1)}})
    flat_item(root, write, name)
    lang[f"block.{MOD}.{name}"] = MEDLEY["display"]


def match(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def loot(out, write):
    """Loot in 26.3's own form (singular "condition" and "modifier", minecraft:match_block)."""
    wild, drops = WILD_RICE["block"], WILD_RICE["drops"]
    rice = {"type": "minecraft:item", "name": rid(drops["item"]), "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": drops["min"], "max": drops["max"]}, "add": False},
        {"type": "minecraft:explosion_decay"}]}
    write(out / f"{wild}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{wild}"), "pools": [{
        "rolls": 1, "condition": match(wild, half="lower"),
        "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": rid(wild), "condition": "minecraft:tool/can_shear"}, rice]}]}]})
    for name in list(STORAGE) + [TATAMI["block"]]:
        write(out / f"{name}.json", self_drop(name))
    for name, info in TATAMI_MATS.items():
        write(out / f"{name}.json", self_drop(name, match(name, part="foot") if info["length"] == 2 else None))
    name, rolls = MEDLEY["block"], len(MEDLEY["pieces"])
    whole = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(name)}],
             "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"}, match(name, rolls=rolls)]}}
    platter = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(MEDLEY["platter"])}],
               "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                   {"type": "minecraft:inverted", "term": match(name, rolls=rolls)}]}}
    write(out / f"{name}.json", {"type": "minecraft:block", "pools": [whole, platter], "random_sequence": rid(f"blocks/{name}")})


def worldgen(data, write):
    """Wild rice in swamp and river water exactly one block deep, over bog soil, with air above for its top (the cranberry
    bog's placement)."""
    folder = data / MOD / "worldgen"
    block, patch = WILD_RICE["block"], WILD_RICE["patch"]
    write(folder / "feature" / f"{block}.json", {"type": "minecraft:simple_block", "to_place": {"id": rid(block)}})
    spread = patch["spread_xz"]
    write(folder / "placed_feature" / f"patch_{block}.json", {"feature": rid(block), "placement": [
        {"type": "minecraft:rarity_filter", "chance": patch["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:count", "count": patch["tries"]},
        {"type": "minecraft:offset",
         "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
         "y": {"type": "minecraft:trapezoid", "max": 0, "min": 0, "plateau": 0},
         "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air", "offset": [0, 1, 0]},
            {"type": "minecraft:matching_block_tag", "tag": BOG_SOIL_TAG, "offset": [0, -1, 0]}]}},
    ]})


def tags(tags):
    for name in ("rice_bale", "straw_bale", "tatami", *TATAMI_MATS):
        tags.add("block", "minecraft:mineable/hoe", rid(name))
    tags.add("block", "minecraft:mineable/axe", rid(MEDLEY["block"]))
