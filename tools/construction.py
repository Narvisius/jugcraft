"""Construction chemistry (batch 32, docs/features/construction-chemistry.md): spray foam, cement and concrete.

Java: chemistry/ConstructionChemistry.java (numbers, blocks and items), FoamSprayerItem and CementItem.
tools/check_mod_data.py keeps the numbers here and in Java the same. The foam canister is made in the chemical reactor
(tools/petro.py, FLUID_RECIPES["chemical_reactor"]).
"""
import random

from PIL import Image

MOD = "jugcraft"

# The foam sprayer: how far it reaches (blocks), how many blocks one spray fills at most and how far they spread from
# where it lands, ticks between sprays, and blocks of foam in a canister.
SPRAY_RANGE = 16
SPRAY_BLOCKS = 12
SPRAY_RADIUS = 2.5
SPRAY_COOLDOWN = 8
CANISTER_FOAM = 32

# Blocks: display name, hardness, blast resistance, and whether they come as slabs and stairs too.
BLOCKS = {
    "construction_foam": ("Construction Foam", 0.3, 0.5, False),
    "concrete": ("Concrete", 2.5, 9.0, True),
    "blastproof_concrete": ("Blast-Proof Concrete", 15.0, 1200.0, True),
}
ITEMS = {
    "cement_mix": "Cement Mix",
    "cement": "Cement",
    "rebar": "Rebar",
    "foam_canister": "Foam Canister",
    "foam_sprayer": "Foam Sprayer",
}
TOOLTIPS = {
    "cement": "Use on construction foam to set it into concrete. Mix with gravel and water to make concrete.",
    "foam_canister": "Foam for the foam sprayer: fills 32 blocks.",
    "foam_sprayer": "Sprays construction foam where you aim, filling up to 12 open blocks: bridge gaps, seal caves and "
                    "hold back water or lava. Uses foam canisters from your inventory.",
    "construction_foam": "Light and quick to break. Use cement on it to set it into concrete.",
    "blastproof_concrete": "As blast-proof as obsidian.",
}


def blocks():
    out = []
    for block, (_, _, _, variants) in BLOCKS.items():
        out.append(block)
        if variants:
            out += [f"{block}_slab", f"{block}_stairs"]
    return out


def items():
    return list(ITEMS)


def stairs_blockstate(model):
    """The vanilla stairs blockstate (facing x half x shape), pointing at model, model_inner and model_outer."""
    import tower
    return tower.stairs_blockstate(model)


def write_all(write, assets, data, lang, condition, self_drop):
    for block, (name, _, _, variants) in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        texture = {"all": f"{MOD}:block/{block}"}
        write(assets / "models" / "block" / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": texture})
        write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": f"{MOD}:block/{block}"}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{block}"}})
        if block == "construction_foam":
            # Foam crumbles to nothing: it is made by the sprayer, not carried about.
            write(data / "loot_table" / "blocks" / f"{block}.json", {"type": "minecraft:block", "pools": []})
        else:
            write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))
        if not variants:
            continue
        side = {"bottom": f"{MOD}:block/{block}", "top": f"{MOD}:block/{block}", "side": f"{MOD}:block/{block}"}
        slab, stairs = f"{block}_slab", f"{block}_stairs"
        lang[f"block.{MOD}.{slab}"] = f"{name} Slab"
        lang[f"block.{MOD}.{stairs}"] = f"{name} Stairs"
        models = assets / "models" / "block"
        write(models / f"{slab}.json", {"parent": "minecraft:block/slab", "textures": side})
        write(models / f"{slab}_top.json", {"parent": "minecraft:block/slab_top", "textures": side})
        write(assets / "blockstates" / f"{slab}.json", {"variants": {
            "type=bottom": {"model": f"{MOD}:block/{slab}"}, "type=top": {"model": f"{MOD}:block/{slab}_top"},
            "type=double": {"model": f"{MOD}:block/{block}"}}})
        write(assets / "items" / f"{slab}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{slab}"}})
        for suffix in ("", "_inner", "_outer"):
            write(models / f"{stairs}{suffix}.json", {"parent": f"minecraft:block/{'stairs' if not suffix else suffix[1:] + '_stairs'}",
                                                     "textures": side})
        write(assets / "blockstates" / f"{stairs}.json", stairs_blockstate(f"{MOD}:block/{stairs}"))
        write(assets / "items" / f"{stairs}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{stairs}"}})
        table = self_drop(slab)
        table["pools"][0]["entries"][0]["modifier"] = [
            {"type": "minecraft:set_count", "count": 2, "add": False,
             "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{slab}", "state": {"type": "double"}}},
            {"type": "minecraft:explosion_decay"}]
        write(data / "loot_table" / "blocks" / f"{slab}.json", table)
        write(data / "loot_table" / "blocks" / f"{stairs}.json", self_drop(stairs))
        write(data / "recipe" / f"{slab}.json", shaped(condition, ["BBB"], {"B": f"{MOD}:{block}"}, slab, 6, "building"))
        write(data / "recipe" / f"{stairs}.json", shaped(condition, ["B  ", "BB ", "BBB"], {"B": f"{MOD}:{block}"}, stairs,
                                                         4, "building"))
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        parent = "minecraft:item/handheld" if item == "foam_sprayer" else "minecraft:item/generated"
        write(assets / "models" / "item" / f"{item}.json", {"parent": parent, "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"message.{MOD}.foam_sprayer.empty"] = "No foam canisters"

    recipes = data / "recipe"
    # Cement: crushed limestone (calcite, or a bone block for its lime) burnt with clay and sand to clinker.
    for name, lime in (("cement_mix", "minecraft:calcite"), ("cement_mix_from_bone_block", "minecraft:bone_block")):
        write(recipes / f"{name}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shapeless", "category": "building",
            "ingredients": [lime, "minecraft:clay_ball", "minecraft:sand"],
            "result": {"id": f"{MOD}:cement_mix", "count": 4}})
    for kind, ticks in (("smelting", 200), ("blasting", 100)):
        write(recipes / f"cement_from_{kind}.json", {
            "fabric:load_conditions": condition("machines"), "type": f"minecraft:{kind}", "category": "misc",
            "ingredient": f"{MOD}:cement_mix", "result": {"id": f"{MOD}:cement"}, "experience": 0.1, "cookingtime": ticks})
    write(recipes / "concrete.json", shaped(condition, ["CGC", "GWG", "CGC"], {
        "C": f"{MOD}:cement", "G": "minecraft:gravel", "W": "minecraft:water_bucket"}, "concrete", 8, "building"))
    write(recipes / "rebar.json", shaped(condition, ["  S", " S ", "S  "], {"S": "#c:ingots/steel"}, "rebar", 6, "misc"))
    write(recipes / "blastproof_concrete.json", shaped(condition, ["CCC", "CRC", "CCC"], {
        "C": f"{MOD}:concrete", "R": f"{MOD}:rebar"}, "blastproof_concrete", 8, "building"))
    write(recipes / "foam_sprayer.json", shaped(condition, ["PPN", "RT ", "S  "], {
        "P": "#c:plates/steel", "N": "minecraft:iron_nugget", "R": f"{MOD}:rubber", "T": "minecraft:piston",
        "S": "#c:plates/steel"}, "foam_sprayer", 1, "equipment"))


def shaped(condition, pattern, key, result, count, category):
    return {"fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": category,
            "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{result}", "count": count}}


# ------------------------------------------------------------------ art

def _speckle(base, spread, seed, dots=()):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = rng.randint(-spread, spread)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    for (x, y), c in dots:
        img.putpixel((x, y), c + (255,))
    return img


# Plain concrete, dark to light: 0 the lift joint, 1 a faint form mark, 2 the fill, 3 the lift's lit top edge. Lighter,
# warmer and plainer than blast-proof concrete (BLAST below), so the concretes read as a set: plain (light and smooth),
# bastion (warm board marks, tools/fortifications.py) and blast-proof (cool, dark, heavy lifts and tie holes).
CONCRETE = [(128, 127, 122), (136, 135, 130), (146, 145, 140), (158, 157, 152)]


def concrete():
    """Poured concrete: one smooth lift a block, lit along its top row with its joint on its bottom row, so a wall reads
    as level pours with no vertical joints and the block edge looks like any other lift line. Two short form marks one
    shade under the fill, one in each half, so slabs and stair steps show whole marks. No tie holes and no per-pixel
    flecks: four colours."""
    img = Image.new("RGBA", (16, 16), CONCRETE[2] + (255,))
    for x in range(16):
        _px(img, x, 0, CONCRETE[3])
        _px(img, x, 15, CONCRETE[0])
    for x0, x1, y in ((3, 7, 5), (9, 13, 10)):
        for x in range(x0, x1 + 1):
            _px(img, x, y, CONCRETE[1])
    return img


# Blast-proof concrete, dark to light: 0 the depth of a tie hole, 1 a lift joint, 2 the fill, 3 a lift's lit top edge.
# Cooler and darker than bastion concrete (the fortifications batch, tools/fortifications.py), which the owner liked
# on 5 October 2026 when they called the old speckled, framed blast-proof block horrific.
BLAST = [(78, 81, 84), (96, 99, 102), (110, 113, 115), (124, 127, 129)]


def blastproof_concrete():
    """Heavy cast concrete in two smooth lifts a block: each lift lit along its top row with its joint on its bottom
    row, so the block edge is drawn exactly like the joint inside it and a wall reads as continuous lifts, with no
    vertical joints. Recessed 2 x 2 tie-cone holes (dark at the top left, lit at the bottom right) sit on a lattice
    eight pixels apart, staggered half a step between the lifts, and nothing else marks the smooth face. Each lift
    holds its own holes, so slabs and stair steps show whole ones. Four colours and no per-pixel noise."""
    img = Image.new("RGBA", (16, 16), BLAST[2] + (255,))
    for y0 in (0, 8):
        for x in range(16):
            _px(img, x, y0, BLAST[3])
            _px(img, x, y0 + 7, BLAST[1])
    for x, y in ((2, 3), (10, 3), (6, 11), (14, 11)):
        _px(img, x, y, BLAST[0])
        _px(img, x + 1, y, BLAST[1])
        _px(img, x, y + 1, BLAST[1])
        _px(img, x + 1, y + 1, BLAST[3])
    return img


def construction_foam():
    """Expanded foam: pale yellow in soft clumps with round bubble pores spaced evenly, each lit at its upper left."""
    import block_style as bs
    g = bs.grain(16, 16, 3205, 3.0, 6.0, 0.5)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (234, 220, 164) if g(x, y) > 0.66 else (216, 200, 138) if g(x, y) < 0.34 else (226, 210, 150)
            img.putpixel((x, y), c + (255,))
    for x, y in ((3, 2), (10, 3), (6, 7), (13, 8), (2, 11), (9, 12), (14, 14)):
        img.putpixel((x, y), (186, 168, 104, 255))
        img.putpixel((x - 1, y - 1), (246, 236, 190, 255))
    return img


def _px(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,))


def cement_mix():
    """A heap of grey-brown powder with clay flecks."""
    rng = random.Random(3206)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(7, 15):
        half = (y - 6) * 0.9
        for x in range(16):
            if abs(x - 7.5) <= half:
                c = rng.choice([(150, 140, 126), (170, 160, 146), (132, 122, 110), (176, 120, 90)])
                if y == 14 or abs(x - 7.5) > half - 1:
                    c = tuple(v - 30 for v in c)
                _px(img, x, y, c)
    return img


def cement():
    """A paper sack of cement, folded at the top, with a grey band."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 15):
        for x in range(3, 13):
            c = (214, 196, 150) if x < 9 else (190, 172, 128)
            if y in (8, 9, 10):
                c = (140, 142, 144) if x < 9 else (118, 120, 122)
            _px(img, x, y, c)
    for x in range(4, 12):
        _px(img, x, 2, (170, 152, 108))
    for y in range(3, 15):
        _px(img, 12, y, (150, 134, 96))
    for x in range(3, 13):
        _px(img, x, 14, (150, 134, 96))
    return img


def rebar():
    """Three ribbed steel bars laid diagonally."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for offset in (-3, 0, 3):
        for i in range(2, 14):
            x, y = i + offset, 15 - i
            if 0 <= x < 16 and 0 <= y < 16:
                _px(img, x, y, (96, 76, 62) if i % 2 else (132, 106, 86))
                if x + 1 < 16:
                    _px(img, x + 1, y, (70, 56, 46))
    return img


def foam_canister():
    """A spray can: yellow body, white band, black nozzle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(4, 15):
        for x in range(5, 11):
            f = (x - 5) / 5
            c = (246, 214, 70) if f < 0.3 else (226, 188, 40) if f < 0.7 else (186, 150, 26)
            if y in (8, 9):
                c = (236, 236, 232) if f < 0.6 else (200, 200, 196)
            _px(img, x, y, c)
    for x in range(6, 10):
        _px(img, x, 3, (176, 180, 186))
    _px(img, 7, 2, (30, 30, 34))
    _px(img, 8, 2, (30, 30, 34))
    _px(img, 8, 1, (30, 30, 34))
    return img


def foam_sprayer():
    """A spray gun held like a tool: a steel nozzle up to the top right, a yellow canister and a rubber grip."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    steel = [(60, 64, 72), (110, 116, 126), (170, 176, 186)]
    for i in range(6):  # the lance
        x, y = 9 + i, 6 - i
        _px(img, x, y, steel[2])
        if y + 1 < 16:
            _px(img, x, y + 1, steel[0])
    _px(img, 15, 0, (226, 210, 150))
    for y in range(6, 11):  # body
        for x in range(5, 10):
            _px(img, x, y, steel[1] if y > 7 else steel[2])
    for y in range(3, 7):  # the canister on top
        for x in range(4, 7):
            _px(img, x, y, (236, 200, 50) if x < 6 else (196, 160, 30))
    for i in range(4):  # grip
        _px(img, 6 - i, 11 + i, (30, 30, 34))
        _px(img, 7 - i, 11 + i, (54, 54, 60))
    _px(img, 8, 11, steel[0])  # trigger
    return img


def draw_all(save):
    for block in BLOCKS:
        save(globals()[block](), "block", block)
    # Items are drawn at 64x64 with the high-detail renderer (tools/hd_art.py, tools/construction_art.py).
    import construction_art
    for item in ITEMS:
        save(construction_art.ITEMS[item](), "item", item)
