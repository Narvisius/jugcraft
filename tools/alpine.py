"""Alpine Spawn: a large, cool alpine meadow on mountain plateaus, where new worlds start.

The biome takes the place of vanilla's meadows in the Overworld climate table: every meadow entry becomes Alpine
Spawn, and so do the cool plateau's forest and taiga, which border the cool meadows in vanilla's plateau table
(world/AlpineSpawn, through mixin/OverworldBiomeBuilderMixin). Forest and taiga elsewhere (lowlands, other bands)
stay as they are.

New worlds start in it, at a village when there is one: on a new world's first start the server looks for the
alpine village nearest the origin, up to SPAWN["village_cells"] cells of the alpine village grid away (as far as
SPAWN["radius"]), and moves the world spawn there. Only when there is none does it start in the nearest Alpine Spawn
within SPAWN["radius"] blocks. Alpine villages are common: taiga-style villages (and their Retro Game Shop) that
generate only in Alpine Spawn, on their own grid, VILLAGES, much tighter than vanilla's 34 chunks.

Seasons: its grass and foliage follow the season colours, and winter snow covers it.
"""

BIOME = "alpine_spawn"
DISPLAY = "Alpine Spawn"
FEATURE = "alpine_spawn"

# The biome's own climate values (for colours, rain and snow): cooler and wetter than vanilla's meadow (0.5, 0.8
# temperature 0.5, downfall 0.8), so grass and leaves are a cool green and snow lies on the high ground.
TEMPERATURE = 0.3
DOWNFALL = 0.7
WATER_COLOR = "#3d6ee0"
# Fallen leaves (vanilla leaf litter) take this colour: larch gold.
DRY_FOLIAGE_COLOR = "#b8902a"
# Which vanilla climate entries become Alpine Spawn: every meadow, and the plateau table's cool row (temperature
# index 1) at humidity indexes 2 and 3, vanilla's forest and taiga there (their weird variants are meadows).
REPLACES = "minecraft:meadow"
PLATEAU = {"temperature": 1, "humidity": [2, 3]}

# The start search: alpine villages up to village_cells grid cells (spacing chunks each) from the origin, else
# the nearest Alpine Spawn within radius blocks, sampled every step blocks.
SPAWN = {"radius": 6400, "step": 64, "village_cells": 25}

# Its own villages: vanilla's taiga villages (spruce houses suit the mountains), on a tight grid.
VILLAGE = "village_alpine"
VILLAGE_SET = "alpine_villages"
VILLAGES = {"spacing": 16, "separation": 5, "salt": 20261002}
VILLAGE_TAG = "jugcraft:has_structure/village_alpine"
# Structure tag of the alpine villages, which the start search looks for.
VILLAGE_STRUCTURES = "jugcraft:alpine_villages"

# Vanilla biome tags it joins, like the meadow (but its own villages instead of plains villages).
BIOME_TAGS = ["minecraft:is_overworld", "minecraft:is_mountain", "minecraft:stronghold_biased_to",
              "minecraft:has_structure/trial_chambers", "minecraft:has_structure/abandoned_camp_meadow"]

# Scattered trees: 50% larch, 20% stone pine, 20% spruce, 10% juniper bush. Keep 0–2 tries per chunk.
TREES = {"feature": "alpine_spawn_trees", "count": 1, "default": "minecraft:spruce_checked",
         "picks": [["jugcraft:larch_checked", 0.5], ["jugcraft:stone_pine_checked", 0.2 / 0.5],
                   ["jugcraft:juniper_bush_checked", 0.1 / 0.3]]}
