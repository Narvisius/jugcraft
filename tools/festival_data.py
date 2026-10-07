"""Writes the JSON resources of the Agriculture branch's festival crops (slice 3) from tools/agriculture.py.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files: pumpkin stems, sweet
berry bushes, oak wood, oak leaves and trees, read from the game jar. No model or texture is copied: the
stem and wood models below reuse vanilla's templates (stairs, slab, fence, fence gate) with Jugcraft
textures, and the stem models are rebuilt here without vanilla's biome tint.
"""
from agriculture import (GOURDS, CRANBERRY, BOG_SOIL_TAG, BOG_SOIL, CHESTNUT, WOOD, WOOD_SETS, WOOD_TAGS, TREE_BLOCKS, DECOR,
                         FOUND_WILD, GOURD_PATCH, CRANBERRY_PATCH, CHESTNUT_TREES, stem, attached_stem)

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
# Blockstate y rotation that turns a model drawn facing north to each direction.
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def box(lo, hi, faces):
    """A box; faces maps each face to a texture, and UVs come from the box's position."""
    return {"from": list(lo), "to": list(hi), "faces": {face: {"texture": texture} for face, texture in faces.items()}}


def sides(texture, top=None, bottom=None):
    out = {face: texture for face in ("north", "south", "east", "west")}
    out["up"] = top or texture
    out["down"] = bottom or top or texture
    return out


def variants_by_facing(model, rotation=FACING_Y):
    return {"variants": {f"facing={f}": ({"model": model, "y": rotation[f]} if rotation[f] else {"model": model}) for f in HORIZONTAL}}


# ---------------------------------------------------------------- models

# Gourds, drawn facing north (the stem end of a butternut squash points north).
GOURD_SHAPES = {
    "butternut_squash": [((6, 0, 1), (10, 5, 8)), ((4, 0, 7), (12, 8, 15)), ((7.5, 5, 1.5), (8.5, 6.5, 3))],
    "acorn_squash": [((3, 0, 3), (13, 9, 13)), ((7, 9, 7), (9, 11, 9))],
    "warty_gourd": [((4, 0, 4), (12, 8, 12)), ((6, 8, 6), (10, 12, 10)), ((7, 12, 7), (9, 14, 9))],
    # A round bottom, a narrow neck, a small top bulb and the stalk (Halloween harvest).
    "bottle_gourd": [((4, 0, 4), (12, 7, 12)), ((6, 7, 6), (10, 11, 10)), ((5, 11, 5), (11, 15, 11)), ((7.5, 15, 7.5), (8.5, 16.5, 8.5))],
}


def gourd_model(gourd):
    side, top = rid(f"block/{gourd}_side"), rid(f"block/{gourd}_top")
    if GOURDS[gourd].get("cube"):
        # Heirloom pumpkins are whole blocks like vanilla's: vanilla's cube_column shape, our textures.
        return {"parent": "minecraft:block/cube_column", "textures": {"end": top, "side": side}}
    elements = []
    for index, (lo, hi) in enumerate(GOURD_SHAPES[gourd]):
        texture = "#stem" if index == len(GOURD_SHAPES[gourd]) - 1 and gourd != "warty_gourd" else "#side"
        elements.append(box(lo, hi, sides(texture, "#top" if texture == "#side" else "#stem", "#side" if texture == "#side" else "#stem")))
    return {"parent": "minecraft:block/block",
            "textures": {"particle": side, "side": side, "top": top, "stem": rid("block/gourd_stalk")},
            "elements": elements}


def stem_model(age):
    """Vanilla's stem_growth shape (two crossed planes, 2 pixels taller per age), without the tint."""
    top = 2 * age + 1
    planes = []
    for frm, to, faces in (([0, -1, 8], [16, top, 8], ("north", "south")), ([8, -1, 0], [8, top, 16], ("west", "east"))):
        planes.append({"from": frm, "to": to, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                       "faces": {faces[0]: {"uv": [0, 0, 16, top + 1], "texture": "#stem"},
                                 faces[1]: {"uv": [16, 0, 0, top + 1], "texture": "#stem"}}})
    return {"ambientocclusion": False, "textures": {"particle": rid("block/gourd_stem"), "stem": rid("block/gourd_stem")},
            "elements": planes}


def attached_stem_model():
    """Vanilla's stem_fruit shape: a low crossed stem and one plane bending towards the gourd (west)."""
    model = stem_model(3)
    model["textures"]["upperstem"] = rid("block/gourd_stem_attached")
    model["elements"].append({"from": [0, 0, 8], "to": [9, 16, 8], "faces": {
        "north": {"uv": [9, 0, 0, 16], "texture": "#upperstem"}, "south": {"uv": [0, 0, 9, 16], "texture": "#upperstem"}}})
    return model


def bog_bush_model():
    """Vanilla's cross shape (two planes at 45 degrees), lifted from 0-16 to 4-20 pixels."""
    planes = []
    for frm, to, faces in (([0.8, 4, 8], [15.2, 20, 8], ("north", "south")), ([8, 4, 0.8], [8, 20, 15.2], ("west", "east"))):
        planes.append({"from": frm, "to": to, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                       "shade": False, "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#cross"} for face in faces}})
    return {"ambientocclusion": False, "textures": {"particle": "#cross"}, "elements": planes}


def lantern_model():
    """A carved turnip, 8 pixels wide, with its face on the north side and a tuft of leaf stalks on top.
    The bottom shows the root end, drawn in the side texture's top rows."""
    side, face, top = rid("block/turnip_lantern_side"), rid("block/turnip_lantern_face"), rid("block/turnip_lantern_top")
    body = box((4, 0, 4), (12, 8, 12), {"north": "#face", "south": "#side", "east": "#side", "west": "#side", "up": "#top", "down": "#side"})
    body["faces"]["down"]["uv"] = [4, 0, 12, 8]
    tuft = box((7, 8, 7), (9, 11, 9), {"north": "#top", "south": "#top", "east": "#top", "west": "#top", "up": "#top"})
    return {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "face": face, "top": top},
            "elements": [body, tuft]}


def stairs_variants(model):
    """Vanilla's stairs blockstate: the same rotations its data generator writes for oak stairs."""
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    out = {}
    for facing, y in base.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                kind = {"straight": "", "inner_left": "_inner", "inner_right": "_inner",
                        "outer_left": "_outer", "outer_right": "_outer"}[shape]
                turn = y
                if half == "bottom" and shape.endswith("left"):
                    turn = y + 270
                elif half == "top" and shape.endswith("right"):
                    turn = y + 90
                variant = {"model": rid(f"block/{model}{kind}")}
                x = 180 if half == "top" else 0
                if x:
                    variant["x"] = x
                if turn % 360:
                    variant["y"] = turn % 360
                if x or turn % 360:
                    variant["uvlock"] = True
                out[f"facing={facing},half={half},shape={shape}"] = variant
    return {"variants": out}


def wood_assets(root, write, lang):
    for wood in WOOD_SETS:
        wood_set_assets(wood, root, write)
    for name, display in WOOD.items():
        model = f"{name.removesuffix('_fence')}_fence_inventory" if name.endswith("_fence") else name
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model}")}})
        lang[f"block.{MOD}.{name}"] = display


def wood_set_assets(wood, root, write):
    """One wood set's block models and blockstates (vanilla oak's layouts, the wood's own textures)."""
    log, log_top = rid(f"block/{wood}_log"), rid(f"block/{wood}_log_top")
    stripped, stripped_top = rid(f"block/stripped_{wood}_log"), rid(f"block/stripped_{wood}_log_top")
    planks = rid(f"block/{wood}_planks")
    models = root / "models" / "block"
    for name, side, end in ((f"{wood}_log", log, log_top), (f"stripped_{wood}_log", stripped, stripped_top),
                            (f"{wood}_wood", log, log), (f"stripped_{wood}_wood", stripped, stripped)):
        write(models / f"{name}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": end, "side": side}})
        if name.endswith("_log"):
            write(models / f"{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                       "textures": {"end": end, "side": side}})
            horizontal = rid(f"block/{name}_horizontal")
        else:
            horizontal = rid(f"block/{name}")
        write(root / "blockstates" / f"{name}.json", {"variants": {
            "axis=x": {"model": horizontal, "x": 90, "y": 90}, "axis=y": {"model": rid(f"block/{name}")},
            "axis=z": {"model": horizontal, "x": 90}}})
    write(models / f"{wood}_planks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": planks}})
    write(root / "blockstates" / f"{wood}_planks.json", {"variants": {"": {"model": rid(f"block/{wood}_planks")}}})
    three = {"bottom": planks, "side": planks, "top": planks}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        write(models / f"{wood}_stairs{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": three})
    write(root / "blockstates" / f"{wood}_stairs.json", stairs_variants(f"{wood}_stairs"))
    write(models / f"{wood}_slab.json", {"parent": "minecraft:block/slab", "textures": three})
    write(models / f"{wood}_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": three})
    write(root / "blockstates" / f"{wood}_slab.json", {"variants": {
        "type=bottom": {"model": rid(f"block/{wood}_slab")}, "type=double": {"model": rid(f"block/{wood}_planks")},
        "type=top": {"model": rid(f"block/{wood}_slab_top")}}})
    for suffix, parent in (("_post", "fence_post"), ("_side", "fence_side"), ("_inventory", "fence_inventory")):
        write(models / f"{wood}_fence{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"texture": planks}})
    parts = [{"apply": {"model": rid(f"block/{wood}_fence_post")}}]
    for direction, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": rid(f"block/{wood}_fence_side"), "uvlock": True}
        if y:
            apply["y"] = y
        parts.append({"apply": apply, "when": {direction: "true"}})
    write(root / "blockstates" / f"{wood}_fence.json", {"multipart": parts})
    gate_models = {"": "template_fence_gate", "_open": "template_fence_gate_open", "_wall": "template_fence_gate_wall",
                   "_wall_open": "template_fence_gate_wall_open"}
    for suffix, parent in gate_models.items():
        write(models / f"{wood}_fence_gate{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"texture": planks}})
    gate = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for in_wall in ("false", "true"):
            for is_open in ("false", "true"):
                suffix = ("_wall" if in_wall == "true" else "") + ("_open" if is_open == "true" else "")
                variant = {"model": rid(f"block/{wood}_fence_gate{suffix}"), "uvlock": True}
                if y:
                    variant["y"] = y
                gate[f"facing={facing},in_wall={in_wall},open={is_open}"] = variant
    write(root / "blockstates" / f"{wood}_fence_gate.json", {"variants": gate})


def assets(root, write, lang):
    models = root / "models" / "block"
    # Gourds, their stems (one set of stem models shared by every gourd) and seeds.
    for age in range(8):
        write(models / f"gourd_stem_stage{age}.json", stem_model(age))
    write(models / "gourd_stem_attached.json", attached_stem_model())
    for gourd, info in GOURDS.items():
        write(models / f"{gourd}.json", gourd_model(gourd))
        write(root / "blockstates" / f"{gourd}.json", variants_by_facing(rid(f"block/{gourd}")))
        write(root / "items" / f"{gourd}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{gourd}")}})
        lang[f"block.{MOD}.{gourd}"] = info["display"]
        write(root / "blockstates" / f"{stem(gourd)}.json", {"variants": {
            f"age={age}": {"model": rid(f"block/gourd_stem_stage{age}")} for age in range(8)}})
        # The attached model bends west; vanilla's attached stems use the same rotations.
        write(root / "blockstates" / f"{attached_stem(gourd)}.json",
              variants_by_facing(rid("block/gourd_stem_attached"), {"west": 0, "north": 90, "east": 180, "south": 270}))
        lang[f"block.{MOD}.{stem(gourd)}"] = f"{info['display']} Stem"
        lang[f"block.{MOD}.{attached_stem(gourd)}"] = f"Attached {info['display']} Stem"

    # The cranberry bush: crossed planes like vanilla's cross model, raised 4 pixels so that the bush
    # stands out of its water (the surface is 14 pixels up) instead of hiding under it.
    write(models / "cranberry_bush.json", bog_bush_model())
    for texture in CRANBERRY["stages"]:
        write(models / f"{texture}.json", {"parent": rid("block/cranberry_bush"), "textures": {"cross": rid(f"block/{texture}")}})
    write(root / "blockstates" / f"{CRANBERRY['block']}.json", {"variants": {
        f"age={age}": {"model": rid(f"block/{texture}")} for age, texture in enumerate(CRANBERRY["stages"])}})
    lang[f"block.{MOD}.{CRANBERRY['block']}"] = CRANBERRY["display"]

    # The chestnut tree: sapling, fruiting leaves (untinted, so the burs keep their colour) and wood.
    sapling, leaves = CHESTNUT["sapling"], CHESTNUT["leaves"]
    write(models / f"{sapling}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{sapling}")}})
    write(root / "blockstates" / f"{sapling}.json", {"variants": {"": {"model": rid(f"block/{sapling}")}}})
    leaf_textures = ["chestnut_leaves", "chestnut_leaves_burs", "chestnut_leaves_ripe"]
    for texture in leaf_textures:
        write(models / f"{texture}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{texture}")}})
    write(root / "blockstates" / f"{leaves}.json", {"variants": {
        f"fruit={fruit}": {"model": rid(f"block/{texture}")} for fruit, texture in enumerate(leaf_textures)}})
    write(root / "items" / f"{leaves}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{leaves}")}})
    for block, display in TREE_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = display
    wood_assets(root, write, lang)

    # Decorations.
    write(models / "turnip_lantern.json", lantern_model())
    write(root / "blockstates" / "turnip_lantern.json", variants_by_facing(rid("block/turnip_lantern")))
    for block, info in DECOR.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = info["display"]


# ---------------------------------------------------------------- loot tables

SHEARS_OR_SILK = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}


def self_drop(block):
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def binomial(p):
    return {"type": "minecraft:binomial", "n": 3, "p": p}


def loot(out, write):
    for gourd, info in GOURDS.items():
        write(out / f"{gourd}.json", self_drop(gourd))
        # Vanilla pumpkin stems: more seeds back the older the stem.
        seed = rid(info["seed"])
        write(out / f"{stem(gourd)}.json", {"type": "minecraft:block", "pools": [{"entries": [{
            "type": "minecraft:item", "name": seed, "modifier": [
                {"type": "minecraft:set_count", "count": binomial(round((age + 1) / 15, 8)),
                 "condition": {"type": "minecraft:match_block", "blocks": rid(stem(gourd)), "state": {"age": str(age)}}}
                for age in range(8)]}], "modifier": {"type": "minecraft:explosion_decay"}, "rolls": 1}],
            "random_sequence": rid(f"blocks/{stem(gourd)}")})
        write(out / f"{attached_stem(gourd)}.json", {"type": "minecraft:block", "pools": [{"entries": [{
            "type": "minecraft:item", "name": seed, "modifier": {"type": "minecraft:set_count", "count": binomial(round(8 / 15, 8))}}],
            "modifier": {"type": "minecraft:explosion_decay"}, "rolls": 1}],
            "random_sequence": rid(f"blocks/{attached_stem(gourd)}")})

    # A cranberry bush gives its planting berry back, plus its crop if ripe.
    bush, berries = CRANBERRY["block"], rid(CRANBERRY["seed"])
    pick = CRANBERRY["pick"]
    write(out / f"{bush}.json", {"type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"}, "pools": [
        {"entries": [{"type": "minecraft:item", "name": berries}], "rolls": 1},
        {"condition": {"type": "minecraft:match_block", "blocks": rid(bush), "state": {"age": "3"}},
         "entries": [{"type": "minecraft:item", "name": berries, "modifier": [
             {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": pick["min"] - 1, "max": pick["max"] - 1}},
             {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
              "parameters": {"bonusMultiplier": 1}}]}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{bush}")})

    # Chestnut leaves: vanilla oak leaves with a chestnut for the sapling and no apples, plus the
    # chestnuts of a ripe bur.
    leaves, nut = CHESTNUT["leaves"], rid(CHESTNUT["seed"])
    not_shears = {"type": "minecraft:inverted", "term": SHEARS_OR_SILK}
    write(out / f"{leaves}.json", {"type": "minecraft:block", "pools": [
        {"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": SHEARS_OR_SILK, "name": rid(leaves)},
            {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:table_bonus", "chances": [0.05, 0.0625, 0.083333336, 0.1], "enchantment": "minecraft:fortune"}]},
             "name": nut}]}], "rolls": 1},
        {"condition": not_shears, "entries": [{"type": "minecraft:item", "condition": {
            "type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1], "enchantment": "minecraft:fortune"},
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                         {"type": "minecraft:explosion_decay"}], "name": "minecraft:stick"}], "rolls": 1},
        {"condition": {"type": "minecraft:match_block", "blocks": rid(leaves), "state": {"fruit": "2"}},
         "entries": [{"type": "minecraft:item", "name": nut, "modifier": [
             {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": CHESTNUT["pick"]["min"],
                                                        "max": CHESTNUT["pick"]["max"]}},
             {"type": "minecraft:explosion_decay"}]}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{leaves}")})
    sapling = self_drop(CHESTNUT["sapling"])
    sapling["pools"][0]["entries"][0]["name"] = nut
    write(out / f"{CHESTNUT['sapling']}.json", sapling)
    for block in WOOD:
        if block.endswith("_slab"):
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
                {"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": rid(block),
                                                              "state": {"type": "double"}}, "count": 2},
                {"type": "minecraft:explosion_decay"}], "name": rid(block)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{block}")})
        else:
            write(out / f"{block}.json", self_drop(block))
    for block in DECOR:
        write(out / f"{block}.json", self_drop(block))


# ---------------------------------------------------------------- tags

def tags(tags):
    for gourd, info in GOURDS.items():
        for tag in info["tags"]:
            tags.add("item", tag, rid(gourd))
            namespace, _, path = tag.partition(":")
            if namespace == "c" and path.startswith("crops/"):
                tags.add("item", "c:crops", f"#{tag}")
        tags.add("block", "minecraft:mineable/axe", rid(gourd))
        tags.add("block", "minecraft:sword_efficient", rid(gourd))
        for block in (stem(gourd), attached_stem(gourd)):
            tags.add("block", "minecraft:maintains_farmland", rid(block))
        tags.add("block", "minecraft:crops", rid(stem(gourd)))
    for soil in BOG_SOIL:
        tags.add("block", BOG_SOIL_TAG, soil)
    tags.add("block", "minecraft:mineable/axe", rid("turnip_lantern"))
    tags.add("block", "minecraft:sword_efficient", rid(CRANBERRY["block"]))

    # Every wood set behaves like vanilla wood: burnable logs (charcoal, fuel), planks for every planks recipe,
    # fences that connect to other wooden fences, and leaves and saplings in the vanilla tags.
    for wood, log_tag in WOOD_TAGS.items():
        for registry in ("block", "item"):
            for log in (f"{wood}_log", f"{wood}_wood", f"stripped_{wood}_log", f"stripped_{wood}_wood"):
                tags.add(registry, log_tag, rid(log))
            tags.add(registry, "minecraft:logs_that_burn", f"#{log_tag}")
            tags.add(registry, "minecraft:planks", rid(f"{wood}_planks"))
            tags.add(registry, "minecraft:wooden_stairs", rid(f"{wood}_stairs"))
            tags.add(registry, "minecraft:wooden_slabs", rid(f"{wood}_slab"))
            tags.add(registry, "minecraft:wooden_fences", rid(f"{wood}_fence"))
            tags.add(registry, "minecraft:fence_gates", rid(f"{wood}_fence_gate"))
        tags.add("block", "minecraft:overworld_natural_logs", rid(f"{wood}_log"))
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:leaves", rid(CHESTNUT["leaves"]))
    tags.add("block", "minecraft:saplings", rid(CHESTNUT["sapling"]))


# ---------------------------------------------------------------- worldgen

def worldgen(data, write):
    folder = data / MOD / "worldgen"
    for block, info in FOUND_WILD.items():
        if info["on"] == "grass":
            # A gourd lying in any of the four directions.
            write(folder / "feature" / f"{block}.json", {"type": "minecraft:simple_block", "to_place": {
                "type": "minecraft:weighted", "entries": [
                    {"data": {"id": rid(block), "properties": {"facing": facing}}, "weight": 1} for facing in HORIZONTAL]}})
            spread = GOURD_PATCH["spread_xz"]
            placement = [
                {"type": "minecraft:rarity_filter", "chance": GOURD_PATCH["rarity"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
                {"type": "minecraft:count", "count": GOURD_PATCH["tries"]},
                {"type": "minecraft:offset",
                 "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
                 "y": {"type": "minecraft:trapezoid", "max": GOURD_PATCH["spread_y"], "min": -GOURD_PATCH["spread_y"], "plateau": 0},
                 "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
                {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:grass_block", "offset": [0, -1, 0]}]}},
            ]
        else:
            # Ripe cranberry bushes in swamp water exactly one block deep, over bog soil.
            write(folder / "feature" / f"{block}.json", {"type": "minecraft:simple_block",
                                                         "to_place": {"id": rid(block), "properties": {"age": "3"}}})
            spread = CRANBERRY_PATCH["spread_xz"]
            placement = [
                {"type": "minecraft:rarity_filter", "chance": CRANBERRY_PATCH["rarity"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:count", "count": CRANBERRY_PATCH["tries"]},
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
            ]
        write(folder / "placed_feature" / f"patch_{block}.json", {"feature": rid(block), "placement": placement})

    # The chestnut tree: a straight trunk with a broad, round crown (vanilla's oak shapes, larger).
    trunk, foliage = CHESTNUT["trunk"], CHESTNUT["foliage"]
    write(folder / "feature" / "chestnut.json", {
        "type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": [],
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": foliage["height"], "offset": 0, "radius": foliage["radius"]},
        "foliage_provider": {"id": rid(CHESTNUT["leaves"]),
                             "properties": {"distance": "7", "fruit": "0", "persistent": "false", "waterlogged": "false"}},
        "ignore_vines": True, "minimum_size": {"type": "minecraft:two_layers_feature_size"},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": trunk["base_height"],
                         "height_rand_a": trunk["height_rand_a"], "height_rand_b": 0},
        "trunk_provider": {"id": rid("chestnut_log"), "properties": {"axis": "y"}}})
    write(folder / "placed_feature" / "patch_chestnut_tree.json", {"feature": rid("chestnut"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": CHESTNUT_TREES["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": rid(CHESTNUT["sapling"])}},
    ]})
