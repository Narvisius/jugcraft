"""Wild plants of the biomes branch (docs/branches/BIOMES.md): flowers and ground cover that its biomes grow.

Each plant is registered alike from the generated /jugcraft/plants.json (agriculture/JugcraftAgriculture
.registerWildPlants), so Java needs no list of its own. Kinds, each with vanilla behaviour:
- "flower": a small flower (vanilla's FlowerBlock, like the dandelion), with its potted form; dye and suspicious stew.
- "tall_flower": a two-block flower (TallFlowerBlock, like the lilac); bone meal drops a copy; dye x2.
- "flowerbed": ground cover of up to four clumps (agriculture/GroundCoverBlock, like pink petals).
- "tall_plant": a two-block plant that is not a flower (DoublePlantBlock), dropping itself from its lower half.
- "dune_plant": a tall plant that also stands on sand (agriculture/DunePlantBlock: wherever vanilla's dry grass can).
- "floor_plant": a small plant on any sturdy floor, stone, mud and netherrack included (agriculture/FloorPlantBlock).
A plant's "light" (0-15) makes it glow (flowers and glow plants).
- "water_plant": a plant under water (agriculture/WaterPlantBlock, like seagrass); only shears take it.
- "surface": a plant floating on still water (agriculture/FloatingPlantBlock, like a lily pad); placed on water.
- "grass": short grass or a fern (agriculture/WildGrassBlock): replaced like grass when built over, only shears take it,
  and bone meal grows it into its "tall" form.
- "tall_grass": that tall form (vanilla's DoublePlantBlock, like tall grass), also replaceable and taken by shears.
- "hanging": a plant hanging from leaves or the underside of a block (agriculture/HangingPlantBlock), in strands that
  bone meal lengthens a block at a time; only shears take it.
- "vine": a creeper on any face of a block, several at once (vanilla's GlowLichenBlock without its glow): bone meal
  spreads it; only shears take it.
Every plant composts, burns like vanilla flowers and follows the "biomes" feature switch for its recipes. Textures are
drawn by tools/wild_textures.py, or for "art": "flora" plants sculpted by tools/flora_models.py. Biomes place them
through tools/biomes.py EXTRAS; a plant's "patch" also scatters it in vanilla's biomes with any of the given conventional
biome tags (one patch in about `rarity` chunks, `tries` tries a patch).
"""

FEATURE = "biomes"

# effect: the suspicious stew effect (vanilla effect ID) and its seconds, for small flowers.
PLANTS = {
    # Batch 2: fields and meadows.
    "lavender": {"kind": "flower", "display": "Lavender", "dye": "purple", "effect": "minecraft:regeneration", "seconds": 5.0},
    "tall_lavender": {"kind": "tall_flower", "display": "Tall Lavender", "dye": "purple"},
    "goldenrod": {"kind": "flower", "display": "Goldenrod", "dye": "yellow", "effect": "minecraft:saturation", "seconds": 0.35},
    "heather": {"kind": "flower", "display": "Heather", "dye": "magenta", "effect": "minecraft:speed", "seconds": 6.0},
    "orange_cosmos": {"kind": "flower", "display": "Orange Cosmos", "dye": "orange", "effect": "minecraft:fire_resistance",
                      "seconds": 4.0},
    "clover": {"kind": "flowerbed", "display": "Clover"},
    # Batch 3: wetlands.
    "cattail": {"kind": "tall_plant", "display": "Cattail"},
    "watergrass": {"kind": "water_plant", "display": "Watergrass"},
    "duckweed": {"kind": "surface", "display": "Duckweed"},
    # Batch 5: big trees and rainforests (the tropics and subtropics).
    "hibiscus": {"kind": "flower", "display": "Hibiscus", "dye": "pink", "effect": "minecraft:water_breathing", "seconds": 5.0},
    "hydrangea": {"kind": "tall_flower", "display": "Hydrangea", "dye": "light_blue"},
    # Batch 6: mountains, coasts and volcanoes.
    "sea_oats": {"kind": "dune_plant", "display": "Sea Oats"},
    # Batch 7: wonders and caves.
    "glowcap": {"kind": "floor_plant", "display": "Glowcap", "light": 10},
    "glimmerbloom": {"kind": "flower", "display": "Glimmerbloom", "dye": "magenta", "effect": "minecraft:glowing", "seconds": 8.0,
                     "light": 7},
    "frost_iris": {"kind": "flower", "display": "Frost Iris", "dye": "light_blue", "effect": "minecraft:slow_falling", "seconds": 4.0},
    "snowpetals": {"kind": "flowerbed", "display": "Snowpetals"},
    # Batch 8: the Nether.
    "bramble": {"kind": "floor_plant", "display": "Bramble"},
    # The graveyard flora (docs/features/graveyard-flora.md): plants for a haunted churchyard, sculpted.
    "spider_lily": {"kind": "flower", "display": "Spider Lily", "dye": "red", "effect": "minecraft:weakness", "seconds": 7.0,
                    "art": "flora", "patch": {"biomes": ["IS_SWAMP"], "rarity": 10, "tries": 24}},
    "snowdrop": {"kind": "flower", "display": "Snowdrop", "dye": "white", "effect": "minecraft:absorption", "seconds": 6.0,
                 "art": "flora", "patch": {"biomes": ["IS_SNOWY", "IS_TAIGA"], "rarity": 10, "tries": 32}},
    "deadly_nightshade": {"kind": "flower", "display": "Deadly Nightshade", "dye": "purple", "effect": "minecraft:poison",
                          "seconds": 10.0, "art": "flora", "patch": {"biomes": ["IS_SPOOKY"], "rarity": 6, "tries": 16}},
    "bleeding_heart": {"kind": "flower", "display": "Bleeding Heart", "dye": "pink", "effect": "minecraft:health_boost",
                       "seconds": 8.0, "art": "flora"},
    "ghost_pipe": {"kind": "flower", "display": "Ghost Pipe", "dye": "light_gray", "effect": "minecraft:invisibility",
                   "seconds": 6.0, "light": 6, "art": "flora", "patch": {"biomes": ["IS_SPOOKY"], "rarity": 4, "tries": 24}},
    "black_rose": {"kind": "tall_flower", "display": "Black Rose", "dye": "black", "art": "flora",
                   "patch": {"biomes": ["IS_SPOOKY"], "rarity": 14, "tries": 12}},
    "foxglove": {"kind": "tall_flower", "display": "Foxglove", "dye": "magenta", "art": "flora",
                 "patch": {"biomes": ["IS_FOREST"], "rarity": 18, "tries": 16}},
    "funeral_lily": {"kind": "tall_flower", "display": "Funeral Lily", "dye": "white", "art": "flora"},
    "asphodel": {"kind": "tall_flower", "display": "Asphodel", "dye": "white", "art": "flora"},
    "withered_grass": {"kind": "grass", "display": "Withered Grass", "tall": "tall_withered_grass", "art": "flora"},
    "tall_withered_grass": {"kind": "tall_grass", "display": "Tall Withered Grass", "art": "flora"},
    "ghost_fern": {"kind": "grass", "display": "Ghost Fern", "tall": "large_ghost_fern", "art": "flora",
                   "patch": {"biomes": ["IS_SPOOKY"], "rarity": 2, "tries": 32}},
    "large_ghost_fern": {"kind": "tall_grass", "display": "Large Ghost Fern", "art": "flora"},
    "dead_mans_fingers": {"kind": "floor_plant", "display": "Dead Man's Fingers", "art": "flora",
                          "patch": {"biomes": ["IS_SPOOKY"], "rarity": 8, "tries": 12}},
    "grave_moss": {"kind": "flowerbed", "display": "Grave Moss", "art": "flora",
                   "patch": {"biomes": ["IS_SPOOKY"], "rarity": 6, "tries": 24}},
    "shroud_moss": {"kind": "hanging", "display": "Shroud Moss", "art": "flora"},
    "creeping_ivy": {"kind": "vine", "display": "Creeping Ivy", "art": "flora"},
}
KINDS = ("flower", "tall_flower", "flowerbed", "tall_plant", "dune_plant", "floor_plant", "water_plant", "surface", "grass", "tall_grass",
         "hanging", "vine")
TALL = ("tall_flower", "tall_plant", "dune_plant", "tall_grass")
# Hanging plants: bone meal lengthens a strand to at most `max_length` blocks; worldgen hangs strands of 1 to
# `worldgen_length` blocks under leaves and logs.
HANGING = {"max_length": 8, "worldgen_length": 4}


def flora():
    """The graveyard flora's plants: sculpted rather than drawn as crossed pictures."""
    return [plant for plant, info in PLANTS.items() if info.get("art") == "flora"]


def potted(plant):
    return f"potted_{plant}"


def blocks():
    """Every wild plant block, potted forms included."""
    out = list(PLANTS)
    out += [potted(p) for p, info in PLANTS.items() if info["kind"] == "flower"]
    return out


def items():
    return list(PLANTS)


def itemless():
    """Potted plants have no item of their own (like vanilla's)."""
    return [potted(p) for p, info in PLANTS.items() if info["kind"] == "flower"]


def dye_recipes():
    """Shapeless dye recipes: a small flower makes one dye, a tall flower two (as vanilla's do)."""
    out = []
    for plant, info in PLANTS.items():
        if "dye" in info:
            out.append({"id": f"{info['dye']}_dye_from_{plant}", "inputs": [f"jugcraft:{plant}"], "result": f"minecraft:{info['dye']}_dye",
                        "count": 2 if info["kind"] == "tall_flower" else 1, "group": f"{info['dye']}_dye", "switch": FEATURE})
    return out


def registration():
    """The generated /jugcraft/plants.json that Java registers from."""
    out = []
    for plant, info in PLANTS.items():
        entry = {"id": plant, "kind": info["kind"]}
        if info["kind"] == "flower":
            entry["effect"] = info["effect"]
            entry["seconds"] = info["seconds"]
        if "light" in info:
            entry["light"] = info["light"]
        if "tall" in info:
            entry["tall"] = info["tall"]
        if info["kind"] == "hanging":
            entry["max_length"] = HANGING["max_length"]
        if "patch" in info:
            entry["patch"] = info["patch"]["biomes"]
        out.append(entry)
    return out


def textures():
    """Block textures the plants' models use."""
    out = []
    for plant, info in PLANTS.items():
        if info.get("art") == "flora":
            out.append(plant)
        elif info["kind"] in TALL:
            out += [f"{plant}_bottom", f"{plant}_top"]
        elif info["kind"] == "flowerbed":
            out += [plant, f"{plant}_stem"]
        else:
            out.append(plant)
    return out
