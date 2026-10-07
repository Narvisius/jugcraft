"""Glass tanks and tank gauges (batch 20; docs/features/tank-gauges.md).

The glass tank is a tinplate tank in a frame of borosilicate glass: client/GlassTankRenderer draws the fluid inside,
tinted (the fluid texture here is a pale ripple the tint colours). The tank gauge is a sight-glass panel hung on any
fluid store, with a block state "level" (0-8 eighths) the model shows. Style-independent, like tools/deposits.py.
"""
import random

from PIL import Image

import block_style as bs

from steampunk_models import BRASS, IRON, box

MOD = "jugcraft"
BLOCKS = {
    "glass_tank": {"display": "Glass Tank"},
    "tank_gauge": {"display": "Tank Gauge"},
}
LEVELS = 9
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
FEATURES = ["machines", "silicon"]
RECIPES = {
    # Four borosilicate glass in a steel frame.
    "glass_tank": (["PGP", "G G", "PGP"], {"P": "#c:plates/steel", "G": "jugcraft:borosilicate_glass"}, 1),
    # A sight glass on a steel plate, read by a comparator's works.
    "tank_gauge": (["PGP", " C "], {"P": "#c:plates/steel", "G": "jugcraft:borosilicate_glass",
                                    "C": "minecraft:comparator"}, 2),
}


def rid(path):
    return f"{MOD}:{path}"


# ---------------------------------------------------------------- textures

def glass_side(seed):
    """Borosilicate glass with a faint blue-green tint, half see-through, in a riveted steel frame two pixels wide."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = (70, 74, 80, 255)
            elif edge == 1:
                c = (120, 126, 134, 255) if x < 8 or y < 8 else (96, 100, 108, 255)
            else:
                c = (200, 226, 232, 60)
                if x - y in (2, 3) or x - y == -6:  # Two highlights across the pane.
                    c = (236, 248, 250, 120)
            img.putpixel((x, y), c)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel((x, y), (168, 172, 180, 255))
    del rng
    return img


def fluid_ripple(seed):
    """Near-white in soft clumps with even, lighter diagonal ripples: the glass tank's renderer tints it the fluid's
    colour."""
    g = bs.grain(16, 16, seed, 3.0, 6.0, 0.5)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 230 if g(x, y) > 0.66 else 214 if g(x, y) < 0.34 else 222
            if (x + 2 * y) % 7 == 0:
                v = 248
            img.putpixel((x, y), (v, v, v, 255))
    return img


def gauge_face(seed):
    """The gauge's sight glass: dark glass with fine graduation marks every eighth down its right edge."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (22, 30, 34) if x not in (0, 15) else (54, 58, 64)
            if x in (12, 13) and y % 2 == 0:
                c = (176, 182, 188)
            img.putpixel((x, y), c + (255,))
    return img


def gauge_fill(seed):
    """The liquid column in the gauge: a bright cyan-blue that glows, with a lighter meniscus line."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (60, 170, 220) if 3 <= x <= 12 else (40, 120, 170)
            if y == 0:
                c = (170, 230, 250)
            img.putpixel((x, y), c + (255,))
    return img


def draw_all(save):
    save(glass_side(1601), "block", "glass_tank_side")
    save(fluid_ripple(1602), "block", "glass_tank_fluid")
    save(gauge_face(1603), "block", "tank_gauge_glass")
    save(gauge_fill(1604), "block", "tank_gauge_fill")


# ---------------------------------------------------------------- models

def gauge(level):
    """Facing north (hung on the block to its south): a steel back plate with a brass rim, a tall sight glass, and the
    column inside it filled to `level` eighths."""
    m = [box((3, 1, 14), (13, 15, 16), "dp_gunmetal"),
         box((5.5, 2, 13.25), (10.5, 14, 14), {"*": BRASS, "north": "tank_gauge_glass"}),
         box((3.5, 0.5, 14.5), (12.5, 1.5, 15.5), IRON),
         box((3.5, 14.5, 14.5), (12.5, 15.5, 15.5), IRON)]
    if level:
        height = 11.5 * level / 8
        m.append(box((6.25, 2.25, 13), (9.75, 2.25 + height, 13.25), "tank_gauge_fill!"))
    return m


def write_all(write, assets, data, lang, model_writer):
    """Block states, models, items, names, loot tables and recipes for both blocks."""
    lang[f"block.{MOD}.glass_tank"] = BLOCKS["glass_tank"]["display"]
    lang[f"block.{MOD}.tank_gauge"] = BLOCKS["tank_gauge"]["display"]
    # Glass tank: a cube of framed glass (the fluid is drawn by the client).
    write(assets / "blockstates" / "glass_tank.json", {"variants": {"": {"model": rid("block/glass_tank")}}})
    write(assets / "models" / "block" / "glass_tank.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/glass_tank_side")}})
    write(assets / "items" / "glass_tank.json", {"model": {"type": "minecraft:model", "model": rid("block/glass_tank")}})
    # Tank gauge: one model per eighth, turned to face out from what it hangs on.
    variants = {}
    for level in range(LEVELS):
        elements = gauge(level)
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/dp_gunmetal")
        write(assets / "models" / "block" / f"tank_gauge_{level}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": model_writer.slice_model("tank_gauge", elements, [(0, 0, 0)])[0]})
        for facing, y in FACING_Y.items():
            variant = {"model": rid(f"block/tank_gauge_{level}")}
            if y:
                variant["y"] = y
            variants[f"facing={facing},level={level}"] = variant
    write(assets / "blockstates" / "tank_gauge.json", {"variants": variants})
    write(assets / "items" / "tank_gauge.json", {"model": {"type": "minecraft:model", "model": rid("block/tank_gauge_5")}})
    # Loot: the glass tank keeps its fluid like the tinplate tank; the gauge drops itself.
    survives = {"type": "minecraft:survives_explosion"}
    for block in BLOCKS:
        entry = {"type": "minecraft:item", "name": rid(block)}
        if block == "glass_tank":
            entry["modifier"] = {"type": "minecraft:copy_components", "source": "block_entity",
                                 "include": [rid("stored_fluid")]}
        write(data / "loot_table" / "blocks" / f"{block}.json", {
            "type": "minecraft:block", "random_sequence": rid(f"blocks/{block}"),
            "pools": [{"rolls": 1, "entries": [entry], "condition": survives}]})
    for name, (pattern, key, count) in RECIPES.items():
        write(data / "recipe" / f"{name}.json", {
            "fabric:load_conditions": [{"condition": rid("feature_enabled"), "feature": f} for f in FEATURES],
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": rid(name), "count": count}})
