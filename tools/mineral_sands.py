"""Approved regional sand lenses. One saved block is one finite feed unit; no ore-yield tags."""

SANDS = {
    "titanium_sand": {"display": "Titanium-rich Mineral Sand", "family": 0,
        "length": [24, 40], "width": [12, 24], "thickness": [3, 6], "cover": [0, 2], "rarity": 48,
        "biomes": ["jugcraft:dune_beach", "jugcraft:overgrown_beach", "minecraft:beach"],
        "texture": "biomes and tree blocks/black_sand"},
    "zircon_sand": {"display": "Zircon-rich Mineral Sand", "family": 1,
        "length": [20, 36], "width": [10, 20], "thickness": [3, 5], "cover": [1, 3], "rarity": 64,
        "biomes": ["jugcraft:outback", "jugcraft:lush_desert", "jugcraft:dry_river", "minecraft:desert", "minecraft:badlands"],
        "texture": "biomes and tree blocks/white_sand"},
    "rare_earth_sand": {"display": "Rare-earth-rich Mineral Sand", "family": 2,
        "length": [16, 28], "width": [10, 18], "thickness": [3, 5], "cover": [1, 4], "rarity": 96,
        "biomes": ["jugcraft:basin", "jugcraft:lake_district", "jugcraft:lush_river", "minecraft:river"],
        "texture": "Big Cannons and Mounted Guns/textures/block/blastsand"},
    "garnet_sand": {"display": "Garnet-rich Mineral Sand", "family": 3,
        "length": [20, 32], "width": [10, 18], "thickness": [2, 4], "cover": [0, 2], "rarity": 48,
        "biomes": ["jugcraft:gravel_beach", "jugcraft:highland", "jugcraft:shield", "minecraft:stony_shore", "minecraft:windswept_gravelly_hills"],
        "texture": "Big Cannons and Mounted Guns/textures/block/blastredsand"},
}


def worldgen(write, data):
    for name, info in SANDS.items():
        feature = {"type": "jugcraft:mineral_sand", "sand": "jugcraft:" + name,
                   "surface_biomes": "jugcraft:has_surface_sand/" + name, "family": info["family"]}
        for key in ("length", "width", "thickness", "cover"):
            feature["min_" + key], feature["max_" + key] = info[key]
        write(data / "worldgen" / "feature" / ("ore_" + name + ".json"), feature)
        # A chunk-centered anchor bounds even the largest rotated lens to the surrounding 3x3 chunks.
        write(data / "worldgen" / "placed_feature" / ("ore_" + name + ".json"), {
            "feature": "jugcraft:ore_" + name,
            "placement": [{"type": "minecraft:rarity_filter", "chance": info["rarity"]}]})


def tags(tags):
    # 26.3 derives heightmap solidity from tags, independently of the block's collision shape.
    # Use the motion tag directly: adding these feeds to ordinary sand would grant generic sand recipes.
    tags.add("block", "minecraft:blocks_motion_no_leaves", "#jugcraft:mineral_sands")
    for name, info in SANDS.items():
        tags.add("block", "jugcraft:mineral_sands", "jugcraft:" + name)
        tags.add("block", "jugcraft:mineral_sands/" + name, "jugcraft:" + name)
        for biome in info["biomes"]:
            tags.add("worldgen/biome", "jugcraft:has_surface_sand/" + name, biome)
