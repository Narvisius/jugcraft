"""Pixel Hollows (a rare retro-electronics cave biome) and the Retro Trader who sells maps to it.

Single source of truth for their IDs, recipes, loot, tags, worldgen, biome and sounds.
generate_material_data.py writes the JSON from these tables and check_mod_data.py audits them.
The Java side lives in world/ (PixelHollows, RetroTrader); the checker compares the two.

Balance (docs/features/pixel-hollows.md, docs/features/retro-trader.md):
- Copper and redstone: 1.5x the vanilla per-chunk average inside the biome (an extra half of the vanilla
  attempts, with the same height spread, filtered to the biome). Tin likewise, when tin is enabled.
- Pixel crystal clusters do not grow or tick. They drop 1-2 shards (Fortune adds 0-1 per level), so shards are
  finite; nothing turns back into shards.
- The trader's shard buyback never pays more per shard than his cheapest possible shard sale (see TRADES).
"""

MOD = "jugcraft"
CAVE = "pixel_hollows"
TRADER = "retro_trader"

# Full-cube blocks: display name, feature switch, the vanilla block whose properties the Java side copies, tool tag.
CUBES = {
    "circuitstone": {"display": "Circuitstone", "feature": CAVE, "copy": "DEEPSLATE", "tool": "pickaxe"},
    "polished_circuitstone": {"display": "Polished Circuitstone", "feature": CAVE, "copy": "POLISHED_DEEPSLATE",
                              "tool": "pickaxe"},
    "circuitstone_bricks": {"display": "Circuitstone Bricks", "feature": CAVE, "copy": "DEEPSLATE_BRICKS",
                            "tool": "pickaxe"},
    # Glass, full light. Like a sea lantern, any tool breaks it and it drops itself.
    "pixel_lamp": {"display": "Pixel Lamp", "feature": CAVE, "copy": "SEA_LANTERN", "tool": None},
}
CLUSTER = "pixel_crystal_cluster"
SHARD = "pixel_shard"
CABINET = "arcade_cabinet"
# Sold by the Retro Trader; used, it searches for the nearest Pixel Hollows and becomes a marked explorer map.
MAP = "pixel_hollows_map"
# Light: clusters glow faintly (3) so the cave stays dark enough for hostile mobs between patches.
CLUSTER_LIGHT = 3
CLUSTER_DROPS = (1, 2)

NAMES = {CLUSTER: "Pixel Crystal Cluster", SHARD: "Pixel Shard", CABINET: "Arcade Cabinet", MAP: "Pixel Hollows Map"}


def blocks():
    return list(CUBES) + [CLUSTER, CABINET]


def items():
    return [SHARD, MAP]


def feature_of(entry):
    if entry in CUBES:
        return CUBES[entry]["feature"]
    if entry in (CLUSTER, SHARD):
        return CAVE
    if entry in (CABINET, MAP):
        return TRADER
    raise KeyError(entry)


# ---------------------------------------------------------------- recipes
# (id, kind, features, ingredient(s) / pattern, result, count). Nothing here (or anywhere) outputs pixel shards.
STONECUTTING = [
    ("polished_circuitstone_from_circuitstone_stonecutting", "jugcraft:circuitstone", "polished_circuitstone"),
    ("circuitstone_bricks_from_circuitstone_stonecutting", "jugcraft:circuitstone", "circuitstone_bricks"),
    ("circuitstone_bricks_from_polished_circuitstone_stonecutting", "jugcraft:polished_circuitstone",
     "circuitstone_bricks"),
]
SHAPED = {
    # Like deepslate: four blocks in a square make four of the next finish.
    "polished_circuitstone": ([CAVE], ["##", "##"], {"#": "jugcraft:circuitstone"}, 4, "building"),
    "circuitstone_bricks": ([CAVE], ["##", "##"], {"#": "jugcraft:polished_circuitstone"}, 4, "building"),
    # 4 shards + 1 glass -> 1 lamp.
    "pixel_lamp": ([CAVE], [" S ", "SGS", " S "], {"S": "jugcraft:pixel_shard", "G": "minecraft:glass"}, 1, "building"),
    # The Retro Trader's workstation: the solo route to a trader, once the cave has been visited.
    "arcade_cabinet": ([CAVE, TRADER], ["PGP", "SRS", "PPP"],
                       {"P": "#minecraft:planks", "G": "minecraft:glass_pane", "S": "jugcraft:pixel_shard",
                        "R": "minecraft:redstone"}, 1, "misc"),
}

# ---------------------------------------------------------------- worldgen
# Extra ore attempts inside the biome: half the vanilla count with the vanilla height spread, so the biome
# holds 1.5x the usual ore (vanilla placed features: ore_copper 16 trapezoid -16..112; ore_redstone 4 uniform
# -64..15; ore_redstone_lower 8 trapezoid -96..-32; Jugcraft ore_tin 8 trapezoid -32..96).
BONUS_ORES = {
    "pixel_hollows_copper": {"feature": "minecraft:ore_copper_small", "count": 8, "shape": "trapezoid",
                             "min_y": -16, "max_y": 112},
    "pixel_hollows_redstone": {"feature": "minecraft:ore_redstone", "count": 2, "shape": "uniform",
                               "min_y": -64, "max_y": 15},
    "pixel_hollows_redstone_lower": {"feature": "minecraft:ore_redstone", "count": 4, "shape": "trapezoid",
                                     "min_y": -96, "max_y": -32},
    # Added from Java (world/PixelHollows) only while tin is enabled.
    "pixel_hollows_tin": {"feature": "jugcraft:ore_tin", "count": 4, "shape": "trapezoid", "min_y": -32, "max_y": 96,
                          "switch": "tin"},
}
# The lining: big blobs of circuitstone replacing the stone and deepslate inside the biome, after the ores, so
# exposed ore still shows. COUNT attempts per chunk, filtered to the biome. The biome lies 40 to 115 blocks below
# the surface, so under mountains it reaches up to about Y 160: the lining and crystals cover Y -56 to 192, with
# counts for that span (the density per block of height is what it was over -56 to 56).
LINING = "pixel_hollows_lining"
LINING_BLOCK = "circuitstone"
LINING_GEN = {"size": 64, "count": 212, "min_y": -56, "max_y": 192}
# Crystal clusters on floors (facing up) and ceilings (facing down): attempts per chunk over the lining's span,
# filtered to the biome.
CRYSTALS = {"pixel_crystals_floor": {"facing": "up", "scan": "down", "offset": 1, "count": 31},
            "pixel_crystals_ceiling": {"facing": "down", "scan": "up", "offset": -1, "count": 18}}

# The biome's features, one list per generation step. Vanilla entries keep vanilla's relative order (a different
# order in two biomes is a "feature order cycle" crash); the JugcraftGameTests.overworldFeatureOrderHasNoCycle test
# sorts every Overworld biome's features to prove it. Stone variety blobs, glow lichen and surface vegetation are
# left out: the walls are circuitstone and the only light is the crystals.
STEPS = ["raw_generation", "lakes", "local_modifications", "underground_structures", "surface_structures",
         "strongholds", "underground_ores", "underground_decoration", "fluid_springs", "vegetal_decoration",
         "top_layer_modification"]
VANILLA_FEATURES = {
    "lakes": ["lake_lava_underground", "lake_lava_surface"],
    "local_modifications": ["amethyst_geode"],
    "underground_structures": ["monster_room", "monster_room_deep"],
    "underground_ores": ["ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle", "ore_iron_small",
                         "ore_gold", "ore_gold_lower", "ore_redstone", "ore_redstone_lower", "ore_diamond",
                         "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried", "ore_lapis",
                         "ore_lapis_buried", "ore_copper"],
    "fluid_springs": ["spring_water", "spring_lava"],
}
OWN_FEATURES = {
    "underground_ores": ["pixel_hollows_copper", "pixel_hollows_redstone", "pixel_hollows_redstone_lower"],
    "underground_decoration": [LINING],
    "vegetal_decoration": list(CRYSTALS),
}


def biome_features():
    return [[f"minecraft:{name}" for name in VANILLA_FEATURES.get(step, [])]
            + [f"{MOD}:{name}" for name in OWN_FEATURES.get(step, [])] for step in STEPS]


# Colours: a dim violet haze, like the inside of a dark CRT.
FOG = "#2b2340"
SKY = "#7a6fa8"
WATER = "#3a6ea5"
WATER_FOG = "#12233a"

SOUNDS = {
    # A quiet original chiptune hum (tools/pixel_hollows_sound.py writes the .ogg).
    "ambient.pixel_hollows.loop": {"sounds": [{"name": f"{MOD}:ambient/pixel_hollows_loop", "volume": 0.5}],
                                   "subtitle": f"subtitles.{MOD}.ambient.pixel_hollows.loop"},
    # Now and then a blip, from the note block's own "bit" sound at a few pitches.
    "ambient.pixel_hollows.additions": {"sounds": [{"name": "minecraft:note/bit", "volume": 0.35, "pitch": p}
                                                   for p in (0.5, 0.63, 0.75, 1.0, 1.26)],
                                        "subtitle": f"subtitles.{MOD}.ambient.pixel_hollows.additions"},
    # The Retro Trader at his cabinet.
    "entity.villager.work_retro_trader": {"sounds": [{"name": "minecraft:note/bit", "volume": 0.6, "pitch": p}
                                                     for p in (1.0, 1.19, 1.5)],
                                          "subtitle": f"subtitles.{MOD}.entity.villager.work_retro_trader"},
}
SUBTITLES = {
    "ambient.pixel_hollows.loop": "Circuitry hums",
    "ambient.pixel_hollows.additions": "Something beeps",
    "entity.villager.work_retro_trader": "Retro Trader plays",
}


def biome():
    """data/jugcraft/worldgen/biome/pixel_hollows.json (26.x: looks and sounds are environment attributes)."""
    return {
        "has_precipitation": False,
        "temperature": 0.8,
        "downfall": 0.4,
        "attributes": {
            "minecraft:visual/fog_color": FOG,
            "minecraft:visual/sky_color": SKY,
            "minecraft:visual/water_fog_color": WATER_FOG,
            "minecraft:audio/ambient_sounds": {
                "loop": f"{MOD}:ambient.pixel_hollows.loop",
                "mood": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8,
                         "offset": 2.0},
                "additions": [{"sound": f"{MOD}:ambient.pixel_hollows.additions", "tick_chance": 0.006}],
            },
        },
        "effects": {"water_color": WATER},
        "spawners": {
            "monster": [{"type": f"minecraft:{mob}", "minCount": low, "maxCount": high, "weight": weight}
                        for mob, weight, low, high in (("spider", 100, 4, 4), ("zombie", 95, 4, 4),
                                                       ("zombie_villager", 5, 1, 1), ("skeleton", 100, 4, 4),
                                                       ("creeper", 100, 4, 4), ("slime", 100, 4, 4),
                                                       ("enderman", 10, 1, 4), ("witch", 5, 1, 1))],
            "ambient": [{"type": "minecraft:bat", "minCount": 8, "maxCount": 8, "weight": 10}],
        },
        "spawn_costs": {},
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "features": biome_features(),
    }


# Biome tags: an Overworld cave (vanilla and convention tags), with mineshafts like the vanilla caves.
BIOME_TAGS = ["minecraft:is_overworld", "minecraft:has_structure/mineshaft", "c:is_cave", "c:is_underground"]

# ---------------------------------------------------------------- the Retro Trader
# Trades are data (26.1+): one villager_trade file each, a tag per level listing them, and a trade set per level
# that the profession names (world/RetroTrader). The novice and apprentice levels have exactly two trades, and
# their trade sets draw two, so the trader always offers all of them.
#
# No profit loop: buying shards costs 4 emeralds for 2; reputation and Hero of the Village can lower that to
# 1 emerald for 2 (0.5 each). The buyback's reputation_discount is 0, so reputation never lowers it, and Hero of
# the Village V lowers 6 shards only to 3 (floor(0.55 * 6) = 3 off): at best 1/3 emerald per shard. 1/3 < 1/2.
TRADES = {
    "pixel_hollows_map": {"level": 1, "wants": ("minecraft:emerald", 12), "additional_wants": ("minecraft:compass", 1),
                          "gives": (f"{MOD}:{MAP}", 1), "max_uses": 1, "xp": 5, "reputation_discount": 0.2,
                          "features": [CAVE, TRADER]},
    "circuitstone": {"level": 1, "wants": ("minecraft:emerald", 1), "gives": (f"{MOD}:circuitstone", 8), "max_uses": 12,
                     "xp": 1, "reputation_discount": 0.05, "features": [CAVE]},
    "pixel_shard_buyback": {"level": 2, "wants": (f"{MOD}:{SHARD}", 6), "gives": ("minecraft:emerald", 1), "max_uses": 12,
                            "xp": 5, "reputation_discount": 0.0, "features": [CAVE]},
    "pixel_lamp": {"level": 2, "wants": ("minecraft:emerald", 3), "gives": (f"{MOD}:pixel_lamp", 1), "max_uses": 4, "xp": 5,
                   "reputation_discount": 0.05, "features": [CAVE]},
    "pixel_shards": {"level": 3, "wants": ("minecraft:emerald", 4), "gives": (f"{MOD}:{SHARD}", 2), "max_uses": 3,
                     "xp": 10, "reputation_discount": 0.05, "features": [CAVE]},
}
TRADE_LEVELS = sorted({trade["level"] for trade in TRADES.values()})
# Bounded search behind the map (Java: world/PixelHollowsMaps): radius, column spacing, height spacing, start height.
MAP_SEARCH = {"radius": 2048, "step": 64, "vertical_step": 32, "start_y": -16}
# Village shop: one in every new village of these types (not zombie villages). It joins each type's
# minecraft:village/<type>/houses pool with this weight, and world/RetroShopPlacement makes it the first choice for a
# village's house slots until one is placed, then removes it (Java: RetroTrader.VILLAGE_HOUSES and SHOP_WEIGHT).
SHOP_VILLAGES = ["plains", "desert", "savanna", "snowy", "taiga"]
SHOP_WEIGHT = 1
SHOP = "village/plains/retro_game_shop"
