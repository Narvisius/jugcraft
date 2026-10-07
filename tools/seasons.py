"""Seasons: which biomes change with the server's date, and the seasonal snow block.

#jugcraft:has_seasons: grass and foliage change colour (client/SeasonColors, season/SeasonPalette). Every
biome with four seasons: temperate grassland, forest, taiga, hills and swamp. Not the tropics (jungle,
mangrove swamp), the arid lands (desert, savanna, badlands), the always-frozen biomes (snowy plains, ice spikes,
the peaks, grove), oceans, beaches, caves or rivers (a river runs through jungles and deserts too, and its banks
would turn autumn-gold there). The pale garden keeps its fixed grey; birch, spruce, cherry and
poplar leaves keep their fixed colours, so in their biomes only grass and oak-type leaves change.

#jugcraft:has_winter_snow: with seasons.snow=on, winter rain falls as snow there and lies as seasonal snow
(season/SeasonalSnow) that melts in spring. The seasonal biomes and the pale garden.
"""

BIOMES = [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
    "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest", "minecraft:dappled_forest", "minecraft:cherry_grove",
    "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
    "minecraft:windswept_forest", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
    "minecraft:swamp",
    "jugcraft:alpine_spawn",
] + [f"jugcraft:{name}" for name, info in __import__("biomes").BIOMES.items() if info["seasons"]]

TAG = "jugcraft:has_seasons"

# Jugcraft biomes with mild winters ("winter_snow": False in tools/biomes.py) keep their seasonal colours but no snow.
WINTER_SNOW = [biome for biome in BIOMES
               if __import__("biomes").BIOMES.get(biome.removeprefix("jugcraft:"), {}).get("winter_snow", True)] + ["minecraft:pale_garden"]

WINTER_SNOW_TAG = "jugcraft:has_winter_snow"

# The seasonal snow block: vanilla snow layers' looks (its models) and drops (a snowball per layer), its own block
# so that only snow the season laid melts in spring.
SNOW_BLOCK = "seasonal_snow"
SNOW_DISPLAY = "Seasonal Snow"

BLOCKS = [SNOW_BLOCK]
