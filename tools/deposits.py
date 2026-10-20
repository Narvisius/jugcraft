"""Surface resource deposits (docs/features/resource-deposits.md): blocks that only a deposit drill can work.

Each deposit block holds 1,000 units (Deposits.CAPACITY in Java); the drill takes 1 at a time and gets one item per
unit. Patches are flat disks in the top layer of stony hill biomes. Keep DEPOSITS in sync with JugcraftDeposits.java;
tools/check_mod_data.py checks it.
"""

import random

from PIL import Image

import block_style as bs

# Block id -> display name, item each unit gives, the feature switch(es) its worldgen needs, and how rare its patches
# are (one patch per this many chunks of a matching biome, on average).
DEPOSITS = {
    "coal_deposit": {"display": "Coal Deposit", "yield": "minecraft:coal", "features": ["deposits"], "chance": 8},
    "iron_deposit": {"display": "Iron Deposit", "yield": "minecraft:raw_iron", "features": ["deposits"], "chance": 10},
    "copper_deposit": {"display": "Copper Deposit", "yield": "minecraft:raw_copper", "features": ["deposits"],
                       "chance": 12},
    "tin_deposit": {"display": "Tin Deposit", "yield": "jugcraft:raw_tin", "features": ["deposits", "tin"],
                    "chance": 12},
}
CAPACITY = 1_000
UNDERGROUND = {
    "coal": (48, ["jugcraft:woodland", "jugcraft:coniferous_forest", "jugcraft:highland", "jugcraft:basin",
                  "minecraft:forest", "minecraft:taiga", "minecraft:windswept_hills", "minecraft:windswept_forest"]),
    "iron": (64, ["jugcraft:shield", "jugcraft:highland", "jugcraft:basin", "jugcraft:tundra",
                  "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:windswept_gravelly_hills"]),
    "copper": (64, ["jugcraft:canyon", "jugcraft:outback", "jugcraft:dryland", "jugcraft:volcano",
                    "minecraft:badlands", "minecraft:wooded_badlands", "minecraft:savanna_plateau"]),
    "tin": (96, ["jugcraft:shield", "jugcraft:basin", "jugcraft:highland", "jugcraft:temperate_rainforest",
                 "minecraft:windswept_forest", "minecraft:old_growth_pine_taiga",
                 "minecraft:old_growth_spruce_taiga", "minecraft:stony_peaks"]),
}


def all_blocks():
    return list(DEPOSITS) + ["deepslate_" + name for name in DEPOSITS] + ["deep_drilling_assembly"]

# Patch size: a disk of this radius, in the top layer of the ground only (half_height 1 at the surface).
RADIUS = (2, 4)
# What a patch may replace: the ground of stony hills, including soil, so patches show on grassy slopes too.
REPLACES = ["minecraft:stone", "minecraft:andesite", "minecraft:diorite", "minecraft:granite", "minecraft:tuff",
            "minecraft:calcite", "minecraft:gravel", "minecraft:grass_block", "minecraft:dirt",
            "minecraft:coarse_dirt"]

# ---------------------------------------------------------------- textures

# Weathered host rock: grey-brown rubble with dark cracks between the stones.
# Rubble in plain, neutral stone grey, joined by dark gaps, with the ore's own lumps among it.
RUBBLE = [(100, 100, 100), (124, 124, 124), (146, 146, 146)]
JOINT = (58, 58, 60)
# Ore lumps per deposit: shadow, body, light, highlight.
LUMPS = {
    "coal_deposit": [(14, 14, 17), (32, 32, 37), (54, 54, 61), (104, 104, 116)],
    "iron_deposit": [(116, 82, 60), (170, 128, 96), (204, 162, 124), (238, 210, 178)],
    "copper_deposit": [(104, 52, 32), (178, 98, 60), (220, 140, 92), (244, 196, 150)],
    "tin_deposit": [(40, 28, 22), (74, 54, 42), (108, 86, 70), (214, 206, 196)],
}
# A few green verdigris specks make copper read as copper at a distance.
ACCENTS = {"copper_deposit": (84, 160, 128)}


def deposit_texture(name, seed, deep=False):
    """A rich deposit face, in the manner of vanilla gravel: rubble packed together, about half of the pieces the
    ore's own lumps, each piece lit on its upper left and shaded on its lower right with dark gaps between, so it
    reads as a patch of ore at a glance and never looks like an ordinary ore block. Tiles seamlessly."""
    shadow, body, light, glint = LUMPS[name]
    pts = bs._cells(16, 16, seed, 13)
    rng = random.Random(seed)
    ore = [rng.random() < 0.5 for _ in pts]
    accent = ACCENTS.get(name)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            i, d1, d2 = bs._nearest(pts, x + 0.5, y + 0.5, 16, 16)
            if d2 - d1 < 0.9:
                img.putpixel((x, y), ((36, 37, 41) if deep else JOINT) + (255,))
                continue
            px, py = pts[i]
            dx = ((x + 0.5 - px + 8) % 16) - 8
            dy = ((y + 0.5 - py + 8) % 16) - 8
            tones = (shadow, body, light) if ore[i] else ([(54, 55, 60), (72, 73, 80), (94, 95, 102)] if deep else RUBBLE)
            k = 1 + (1 if dx + dy < -1.5 else 0) - (1 if dx + dy > 1.6 and d2 - d1 < 2.2 else 0)
            c = tones[k]
            if ore[i] and -2.6 < dx + dy < -1.6 and abs(dx - dy) < 1.0:
                c = glint
            elif accent and not ore[i] and k == 0 and (x + 2 * y) % 5 == 0:
                c = accent
            img.putpixel((x, y), c + (255,))
    return img


def draw_all(save):
    """save(img, kind, name) from generate_textures."""
    for index, name in enumerate(DEPOSITS):
        save(deposit_texture(name, 1500 + index), "block", name)
        save(deposit_texture(name, 1500 + index, deep=True), "block", "deepslate_" + name)


# ---------------------------------------------------------------- data

def feature(name):
    """A flat disk of the deposit in the top layer of the ground (Minecraft 26.x layout: worldgen/feature/)."""
    return {
        "type": "minecraft:disk",
        "half_height": 1,
        "radius": {"type": "minecraft:uniform", "min_inclusive": RADIUS[0], "max_inclusive": RADIUS[1]},
        "state_provider": {"id": f"jugcraft:{name}"},
        "target": {"type": "minecraft:matching_blocks", "blocks": REPLACES},
    }


def placed_feature(name, info):
    """At the surface (never under water), in one chunk in `chance`; JugcraftWorldgen picks the biomes."""
    return {
        "feature": f"jugcraft:{name}",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": info["chance"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
            {"type": "minecraft:biome"},
        ],
    }


def loot_table(name):
    """Nothing: a deposit can only be worked by a drill."""
    return {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{name}"}


def write_all(write, assets, data, lang):
    """Block states, models, item definitions, names, loot tables and worldgen for every deposit."""
    for name, info in DEPOSITS.items():
        write(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"jugcraft:block/{name}"}}})
        write(assets / "models" / "block" / f"{name}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"jugcraft:block/{name}"}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{name}"}})
        lang[f"block.jugcraft.{name}"] = info["display"]
        write(data / "loot_table" / "blocks" / f"{name}.json", loot_table(name))
        write(data / "worldgen" / "feature" / f"{name}.json", feature(name))
        write(data / "worldgen" / "placed_feature" / f"{name}.json", placed_feature(name, info))
    lang["message.jugcraft.deposit"] = "%s: %s of %s left"
    for name, info in DEPOSITS.items():
        deep = "deepslate_" + name
        write(assets / "blockstates" / f"{deep}.json", {"variants": {"": {"model": f"jugcraft:block/{deep}"}}})
        write(assets / "models" / "block" / f"{deep}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"jugcraft:block/{deep}"}})
        write(assets / "items" / f"{deep}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{deep}"}})
        write(data / "loot_table" / "blocks" / f"{deep}.json", loot_table(deep))
        lang[f"block.jugcraft.{deep}"] = "Deepslate " + info["display"]
    for family, (chance, biomes) in UNDERGROUND.items():
        name = f"underground_{family}_deposit"
        write(data / "worldgen" / "feature" / f"{name}.json",
              {"type": "jugcraft:underground_deposit", "family": family, "surface_biomes": f"jugcraft:has_bulk_deposit/{family}"})
        write(data / "worldgen" / "placed_feature" / f"{name}.json",
              {"feature": f"jugcraft:{name}", "placement": [
                  {"type": "minecraft:rarity_filter", "chance": chance}, {"type": "minecraft:in_square"}]})
        write(data / "tags" / "worldgen" / "biome" / "has_bulk_deposit" / f"{family}.json", {"replace": False, "values": biomes})
        write(data / "tags" / "block" / "bulk_deposits" / f"{family}.json",
              {"replace": False, "values": [f"jugcraft:{family}_deposit", f"jugcraft:deepslate_{family}_deposit"]})
        lang[f"prospector.jugcraft.bulk_{family}"] = f"Bulk {family.title()} Deposit"
    write_assembly(write, assets, data, lang)


def write_assembly(write, assets, data, lang):
    """Two-block mast with a lower collar reaching the drill's recessed gantry."""
    import model_writer as mw
    def box(start, end, texture):
        return (start, end, texture)
    name = "deep_drilling_assembly"
    parts = [box((1, -1.8, 1), (15, 2, 15), "dp_gunmetal"),
             box((3, 29, 3), (13, 32, 13), "dp_hazard"),
             box((6.75, -9, 6.75), (9.25, 29, 9.25), "dp_chrome"),
             box((4, 18, 4), (12, 25, 12), "dp_olive"),
             box((3.5, 18, 3.5), (12.5, 19, 12.5), "dp_chrome"),
             box((3.5, 24, 3.5), (12.5, 25, 12.5), "dp_chrome"),
             box((1, -10.85, 1), (15, -8.85, 15), "dp_gunmetal")]
    for x in (2, 12):
        for z in (2, 12):
            parts.append(box((x, -10, z), (x + 2, 29, z + 2), "dp_gunmetal"))
    for y in (-8, 6, 14, 27):
        parts += [box((2, y, 2), (14, y + 1, 4), "dp_chrome"),
                  box((2, y, 12), (14, y + 1, 14), "dp_chrome")]
    # Split each rail at the half boundary; the support collar belongs to the lower model.
    split_parts = []
    for start, end, texture in parts:
        if start[1] < 16 < end[1]:
            split_parts += [box(start, (end[0], 16, end[2]), texture),
                            box((start[0], 16, start[2]), end, texture)]
        else:
            split_parts.append(box(start, end, texture))
    models = mw.split_model(name, split_parts, [(0, 0, 0), (0, 1, 0)])
    textures = {n: f"jugcraft:block/{n}" for n in ("dp_gunmetal", "dp_hazard", "dp_chrome", "dp_olive")}
    for suffix, elements in zip(("", "_upper"), models):
        mw.finish_closed(elements)
        write(assets / "models" / "block" / f"{name}{suffix}.json",
              {"parent": "minecraft:block/block", "textures": {"particle": "jugcraft:block/dp_gunmetal", **textures}, "elements": elements})
    # A complete, reduced mast in inventory rather than just its lower half.
    def reduced(point):
        return (8 + (point[0] - 8) * 0.5, (point[1] + 11) * 0.5, 8 + (point[2] - 8) * 0.5)
    icon = mw.split_model(name, [box(reduced(a), reduced(b), t) for a, b, t in parts], [(0, 0, 0)])[0]
    mw.finish_closed(icon)
    write(assets / "models" / "item" / f"{name}.json",
          {"parent": "minecraft:block/block", "textures": {"particle": "jugcraft:block/dp_gunmetal", **textures}, "elements": icon})
    write(assets / "blockstates" / f"{name}.json", {"variants": {
        "half=lower": {"model": f"jugcraft:block/{name}"}, "half=upper": {"model": f"jugcraft:block/{name}_upper"}}})
    write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{name}"}})
    write(data / "loot_table" / "blocks" / f"{name}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{name}"}],
         "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
             {"type": "minecraft:match_block", "blocks": f"jugcraft:{name}", "state": {"half": "lower"}}]}}]})
    write(data / "recipe" / f"{name}.json", {
        "fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"}],
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PGP", "GCG", "PDP"],
        "key": {"P": "#c:plates/steel", "G": "#c:gears/steel", "C": "jugcraft:basic_circuit", "D": "jugcraft:steel_pickaxe"},
        "result": {"id": f"jugcraft:{name}", "count": 1}})
    lang[f"block.jugcraft.{name}"] = "Deep-Drilling Assembly"
    lang["message.jugcraft.deep_drilling"] = "Depth: %s/%s blocks | Boring: %s%% | Extraction: %s/%s ticks"
    lang["container.jugcraft.drill.boring"] = "Boring: %s / 128 blocks"
    lang["container.jugcraft.drill.extracting"] = "Working at depth %s"
