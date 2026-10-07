"""Surface resource deposits (docs/features/resource-deposits.md): blocks that only a deposit drill can work.

Each deposit block holds 1,000 units (Deposits.CAPACITY in Java); the drill takes 4 at a time and gets one item per
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


def deposit_texture(name, seed):
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
                img.putpixel((x, y), JOINT + (255,))
                continue
            px, py = pts[i]
            dx = ((x + 0.5 - px + 8) % 16) - 8
            dy = ((y + 0.5 - py + 8) % 16) - 8
            tones = (shadow, body, light) if ore[i] else RUBBLE
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
