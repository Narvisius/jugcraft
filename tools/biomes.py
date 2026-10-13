"""The biomes branch (docs/branches/BIOMES.md): Jugcraft regions and the biomes in them.

Jugcraft regions (biome/JugcraftRegions, docs/features/biome-regions.md): the Overworld is divided into irregular
cells about REGIONS["size"] blocks across, and REGIONS["share"] of them are Jugcraft regions, each in one of LAYOUTS
layouts: vanilla's climate table with that layout's RULES applied (a vanilla biome, in the given climate bands, becomes
a Jugcraft biome). The rest stay vanilla. Every Jugcraft biome is also listed in vanilla's table at a climate no place
has, so world generation knows it. RULES are written to /jugcraft/region_rules.json, which Java reads.

Biomes are built from the vanilla biome they replace (biome_bases.BASES): its ores, caves, lakes and springs, with
their own trees and plants (trees.SHAPES). Batch 1 is the seasonal forests.
"""

import end_noise
from biome_bases import BASE_TAGS

MOD = "jugcraft"
FEATURE = "biomes"

REGIONS = {"size": 1024, "share": 0.5}

# Vanilla's climate band edges (OverworldBiomeBuilder): an entry's band is where the middle of its range falls,
# 0 = coldest (driest) to 4 = hottest (wettest).
TEMPERATURE_BANDS = [-0.45, -0.15, 0.2, 0.55]
HUMIDITY_BANDS = [-0.35, -0.1, 0.1, 0.3]

# Jugcraft regions come in LAYOUTS layouts, equally often, each with its own replacements, so that every vanilla climate
# slot can hold several Jugcraft biomes. Their characters: 0 woodland, 1 meadow, 2 wetland, 3 wild.
LAYOUTS = 4
LAYOUT_NAMES = ["woodland", "meadow", "wetland", "wild"]


def rule(layouts, replaces, temperature, humidity, biome, weirdness=0):
    """In each of `layouts`, entries of vanilla biome `replaces` whose temperature and humidity bands lie in the given
    [min, max] ranges, and whose weirdness half is `weirdness` (-1 negative, 1 positive, 0 either), become `biome`. The
    first rule that matches an entry decides it; check_mod_data refuses rules that overlap."""
    return {"layouts": list(layouts), "replaces": f"minecraft:{replaces}", "temperature": list(temperature),
            "humidity": list(humidity), "weirdness": weirdness, "biome": f"{MOD}:{biome}"}


RULES = [
    # Batch 1, the seasonal forests: in every layout, except where the meadows and wetlands take a climate.
    rule([0, 1, 2, 3], "taiga", (1, 1), (0, 4), "coniferous_forest"),
    rule([0, 1, 2, 3], "snowy_taiga", (0, 0), (0, 4), "snowy_coniferous_forest"),
    rule([0, 2, 3], "forest", (1, 1), (0, 4), "maple_woods"),
    rule([0, 1, 3], "forest", (2, 2), (0, 4), "seasonal_forest"),
    rule([0, 1, 2, 3], "birch_forest", (0, 4), (0, 4), "aspen_glade"),
    rule([0, 1, 2, 3], "old_growth_birch_forest", (0, 4), (0, 4), "aspen_glade"),
    rule([0, 3], "plains", (1, 1), (0, 0), "dead_forest"),
    rule([0, 3], "plains", (1, 1), (1, 1), "tundra"),
    rule([0, 1, 2, 3], "snowy_plains", (0, 0), (2, 2), "snowy_forest"),
    rule([0, 1, 2, 3], "snowy_plains", (0, 0), (1, 1), "muskeg"),
    # Batch 2, fields and meadows: the meadow layout.
    rule([1], "forest", (1, 1), (0, 4), "field"),
    rule([1], "plains", (1, 1), (0, 0), "steppe"),
    rule([1], "plains", (1, 1), (1, 1), "grassland"),
    rule([1], "plains", (2, 2), (0, 4), "prairie", weirdness=-1),
    rule([1], "plains", (2, 2), (0, 4), "shrubland", weirdness=1),
    rule([1], "sunflower_plains", (0, 4), (0, 4), "lavender_field"),
    rule([1], "flower_forest", (0, 4), (0, 4), "flower_meadow"),
    rule([1], "savanna", (3, 3), (1, 1), "heathland"),
    rule([1], "sparse_jungle", (3, 3), (0, 4), "lush_grassland"),
    rule([1], "plains", (3, 3), (0, 4), "lush_grassland"),
    # Batch 3, wetlands: the wetland layout; its swamps and bayous also grow in the woodland and wild layouts.
    rule([0, 2, 3], "swamp", (0, 1), (0, 4), "bog", weirdness=-1),
    rule([0, 2, 3], "swamp", (0, 1), (0, 4), "dead_swamp", weirdness=1),
    rule([0, 2, 3], "swamp", (2, 4), (0, 4), "lush_swamp", weirdness=-1),
    rule([0, 2, 3], "swamp", (2, 4), (0, 4), "swamp_woods", weirdness=1),
    rule([0, 2, 3], "mangrove_swamp", (0, 3), (0, 4), "bayou"),
    rule([0, 2, 3], "mangrove_swamp", (4, 4), (0, 4), "floodplain"),
    rule([2], "dark_forest", (0, 4), (0, 4), "ghost_forest", weirdness=-1),
    rule([2], "dark_forest", (0, 4), (0, 4), "sludge_mire", weirdness=1),
    rule([2], "river", (2, 3), (0, 4), "lush_river"),
    rule([2], "old_growth_spruce_taiga", (0, 4), (0, 4), "fen"),
    rule([2], "forest", (2, 2), (0, 4), "lake_district"),
    rule([2], "plains", (1, 1), (0, 0), "quagmire"),
    rule([2], "plains", (1, 1), (1, 1), "marsh"),
    rule([2], "plains", (2, 2), (0, 4), "wetland"),
    # Batch 4, warm and dry: the meadow layout's dry lands, the wild layout's deserts and scrub, two woodland forests.
    rule([1], "savanna", (3, 3), (0, 0), "dryland"),
    rule([1], "savanna_plateau", (3, 3), (0, 4), "xeric_shrubland"),
    rule([1], "forest", (3, 3), (0, 4), "jacaranda_glade"),
    rule([1], "desert", (4, 4), (0, 4), "lush_desert", weirdness=-1),
    rule([1], "desert", (4, 4), (0, 4), "bone_flats", weirdness=1),
    rule([1], "river", (4, 4), (0, 4), "dry_river"),
    rule([1], "snowy_plains", (0, 0), (0, 0), "cold_desert"),
    rule([3], "savanna", (3, 3), (0, 0), "scrubland"),
    rule([3], "savanna", (3, 3), (1, 1), "lush_savanna"),
    rule([3], "desert", (4, 4), (0, 4), "outback", weirdness=-1),
    rule([3], "desert", (4, 4), (0, 4), "oasis", weirdness=1),
    rule([3], "badlands", (4, 4), (0, 4), "wasteland"),
    rule([3], "eroded_badlands", (4, 4), (0, 4), "wasteland"),
    rule([3], "forest", (3, 3), (0, 4), "burnt_forest"),
    rule([0], "forest", (3, 3), (0, 4), "mediterranean_forest"),
    rule([0], "plains", (2, 2), (0, 4), "orchard"),
    # Batch 5: big trees and rainforests. The woodland layout's tropical slots (jungles, savannas), its dark forests,
    # flower forests and old-growth taigas; the old-growth taigas are giant forests in the wild layout too.
    rule([0], "jungle", (3, 3), (4, 4), "rainforest"),
    rule([0], "jungle", (3, 3), (3, 3), "eucalyptus_forest"),
    rule([0], "savanna_plateau", (3, 3), (0, 4), "eucalyptus_forest"),
    rule([0], "sparse_jungle", (3, 3), (0, 4), "tropics"),
    rule([0], "savanna", (3, 3), (0, 4), "subtropics"),
    rule([0], "plains", (3, 3), (0, 4), "subtropics"),
    rule([0], "dark_forest", (0, 4), (0, 4), "dense_forest"),
    rule([0, 3], "old_growth_pine_taiga", (0, 4), (0, 4), "redwood_forest"),
    rule([0, 3], "old_growth_spruce_taiga", (0, 4), (0, 4), "temperate_rainforest"),
    rule([0], "flower_forest", (0, 4), (0, 4), "woodland"),
    rule([0], "sunflower_plains", (0, 4), (0, 4), "woodland"),
    # Batch 6: mountains, coasts and volcanoes. The wild layout's mountains and windswept hills; the wetland layout's
    # beaches and mushroom islands; the frozen and deep seas in both.
    rule([3], "stony_peaks", (0, 4), (0, 4), "volcano"),
    rule([3], "windswept_savanna", (0, 4), (0, 4), "volcano"),
    rule([3], "wooded_badlands", (0, 4), (0, 4), "canyon"),
    rule([3], "windswept_hills", (0, 4), (0, 4), "highland"),
    rule([3], "windswept_gravelly_hills", (0, 4), (0, 4), "basin"),
    rule([3], "windswept_forest", (0, 4), (0, 4), "shield"),
    rule([3], "jagged_peaks", (0, 4), (0, 4), "karst_pinnacles"),
    rule([3], "grove", (0, 4), (0, 4), "hot_springs"),
    rule([2, 3], "frozen_ocean", (0, 4), (0, 4), "ice_sheet"),
    rule([2, 3], "deep_frozen_ocean", (0, 4), (0, 4), "ice_sheet"),
    rule([2, 3], "deep_ocean", (0, 4), (0, 4), "ocean_trench"),
    rule([2, 3], "deep_cold_ocean", (0, 4), (0, 4), "ocean_trench"),
    rule([2], "beach", (1, 1), (0, 4), "gravel_beach"),
    rule([2], "beach", (2, 2), (0, 4), "dune_beach"),
    rule([2], "beach", (3, 3), (0, 4), "overgrown_beach"),
    rule([2], "mushroom_fields", (0, 4), (0, 4), "flower_isle"),
    # Batch 7: wonders and caves. Rare or unused climates in each layout, and the dripstone caves below: damp grottoes in
    # the wetland layout, spiders' nests in the wild one.
    rule([1], "badlands", (0, 4), (0, 4), "cinder_barrens"),
    rule([1], "eroded_badlands", (0, 4), (0, 4), "cinder_barrens"),
    rule([1], "mushroom_fields", (0, 4), (0, 4), "elder_vale"),
    rule([0], "snowy_plains", (0, 0), (0, 0), "frostlight_garden"),
    rule([2], "savanna_plateau", (0, 4), (0, 4), "gilded_shrubland"),
    rule([3], "flower_forest", (0, 4), (0, 4), "glimmer_grove"),
    rule([3], "dark_forest", (0, 4), (0, 4), "gloomweald"),
    rule([2], "dripstone_caves", (0, 4), (0, 4), "glowcap_grotto"),
    rule([1], "swamp", (0, 4), (0, 4), "hallowed_bog"),
    rule([2], "plains", (3, 3), (0, 4), "highsun_meadow"),
    rule([2], "savanna", (0, 4), (0, 4), "highsun_meadow"),
    rule([3], "jungle", (0, 4), (0, 4), "mycelial_jungle"),
    rule([3], "bamboo_jungle", (0, 4), (0, 4), "shrine_springs"),
    rule([3], "snowy_plains", (0, 0), (0, 0), "snowpetal_grove"),
    rule([3], "dripstone_caves", (0, 4), (0, 4), "spider_nest"),
    rule([1], "dark_forest", (0, 4), (0, 4), "starlit_wood"),
    rule([0], "mushroom_fields", (0, 4), (0, 4), "toadstool_field"),
    rule([1], "mangrove_swamp", (0, 4), (0, 4), "webwood"),
    rule([2], "forest", (3, 3), (0, 4), "wild_greens"),
]

# Trees from other features, given a placed feature ("<name>") that checks the given sapling-like block would survive.
PLACED_TREES = {"chestnut_checked": ("jugcraft:chestnut", "jugcraft:chestnut_sapling"),
                # The orchards' fruit trees (tools/orchard.py), picked by the Orchard, Mediterranean Forest, Subtropics,
                # Tropics and Rainforest.
                **{f"{tree}_checked": (f"jugcraft:{tree}_tree", f"jugcraft:{tree}_sapling")
                   for tree in ("pear", "peach", "lemon", "orange", "plum", "banana")},
                "azalea_tree_checked": ("minecraft:azalea_tree", "minecraft:azalea"),
                # Huge mushrooms as trees, on soil (where an oak sapling could stand).
                "huge_red_mushroom_on_soil": ("minecraft:huge_red_mushroom", "minecraft:oak_sapling"),
                "huge_brown_mushroom_on_soil": ("minecraft:huge_brown_mushroom", "minecraft:oak_sapling")}


def rules_file():
    """The generated rules Java reads (/jugcraft/region_rules.json on the classpath)."""
    return RULES


def dimension_file():
    """The generated Nether and End placements Java reads (/jugcraft/dimension_biomes.json; biome/JugcraftDimensions):
    a Nether biome's [temperature, humidity, offset], an End biome's zone and Fabric weight, in the order Java adds them.

    An End biome gives its share instead of a weight: of the highlands ("highlands"), or of the barrens beside
    vanilla's End Highlands ("barrens", keyed by minecraft:end_highlands). Fabric's weights do not give shares in
    proportion (tools/end_noise.py), so the weights are worked out from the shares."""
    out = {"nether": [], "end": []}
    highlands, barrens = [], []
    for name, info in BIOMES.items():
        if info.get("dimension") == "nether":
            temperature, humidity, offset = info["nether"]
            out["nether"].append({"biome": f"{MOD}:{name}", "temperature": temperature, "humidity": humidity, "offset": offset})
        elif info.get("dimension") == "end":
            (highlands if info["end"]["zone"] == "highlands" else barrens).append((name, info["end"]))
    weights, vanilla = end_noise.highlands_weights([end["share"] for _, end in highlands])
    for (name, end), weight in zip(highlands, weights):
        out["end"].append({"biome": f"{MOD}:{name}", "zone": "highlands", "share": round(end["share"], 4),
                           "weight": round(weight, 4)})
    for name, end in barrens:
        out["end"].append({"biome": f"{MOD}:{name}", "zone": "barrens", "highlands": end["highlands"],
                           "share": round(end["share"], 4), "weight": round(end_noise.barrens_weight(end["share"], vanilla), 4)})
    return out


# Count per floor layer: how many ground patches a Nether or End biome lays per chunk ("ground", below).
GROUND_COUNT = 5


# Features the biomes add, in one fixed order, after their base's own (keeps the Overworld's feature order acyclic).
EXTRAS = {
    "tundra_rocks": {"feature": "minecraft:forest_rock", "step": 2, "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]},
    "patch_dead_bush": {"feature": "minecraft:dead_bush", "step": 9, "count": 4},
    "patch_pumpkin_dense": {"feature": "minecraft:pumpkin", "step": 9, "rarity": 24, "count": 96, "on": "minecraft:grass_block"},
    # Batch 2. "block": a vanilla or Jugcraft block placed alone (a feature of its own); "patches": patches per chunk.
    "field_sunflowers": {"feature": "minecraft:sunflower", "step": 9, "rarity": 3, "count": 48},
    "field_flowers": {"feature": "minecraft:flower_plain", "step": 9, "count": 24},
    "meadow_flowers": {"feature": "minecraft:flower_meadow", "step": 9, "patches": 2, "count": 96},
    "meadow_wildflowers": {"feature": "minecraft:wildflower", "step": 9, "count": 32},
    "lavender": {"feature": "jugcraft:lavender", "step": 9, "patches": 3, "count": 96},
    "tall_lavender": {"feature": "jugcraft:tall_lavender", "step": 9, "patches": 2, "count": 48},
    "clover": {"feature": "jugcraft:clover", "step": 9, "patches": 2, "count": 48},
    "goldenrod": {"feature": "jugcraft:goldenrod", "step": 9, "rarity": 2, "count": 48},
    "heather": {"feature": "jugcraft:heather", "step": 9, "patches": 2, "count": 64},
    "orange_cosmos": {"feature": "jugcraft:orange_cosmos", "step": 9, "rarity": 2, "count": 32},
    "oxeye_daisies": {"block": "minecraft:oxeye_daisy", "step": 9, "rarity": 2, "count": 32},
    "alliums": {"block": "minecraft:allium", "step": 9, "rarity": 3, "count": 24},
    "tall_grass_dense": {"feature": "minecraft:tall_grass", "step": 9, "count": 48},
    "dry_grass": {"feature": "minecraft:dry_grass", "step": 9, "patches": 2, "count": 32},
    "bushes_dense": {"feature": "minecraft:bush", "step": 9, "patches": 2, "count": 24},
    # Batch 3. "configured": the extra's own feature, as written. Placements follow vanilla's for the same kind of feature
    # (lakes, disks, sugar cane, seagrass, lily pads).
    "ponds": {"configured": {"type": "minecraft:lake", "barrier": {"id": "minecraft:mud"}, "can_place_feature": {"type": "minecraft:true"},
                             "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {
                                 "type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
                             "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {
                                 "type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
                             "fluid": {"id": "minecraft:water", "properties": {"level": "0"}}},
              "step": 1, "placement": [{"type": "minecraft:rarity_filter", "chance": 4}, {"type": "minecraft:in_square"},
                                       {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]},
    "mud": {"configured": {"type": "minecraft:disk", "half_height": 1, "radius": {"type": "minecraft:uniform", "max_inclusive": 5, "min_inclusive": 2},
                           "state_provider": {"id": "minecraft:mud"}, "target": {"type": "minecraft:matching_blocks", "blocks": [
                               "minecraft:dirt", "minecraft:grass_block", "minecraft:clay", "minecraft:coarse_dirt"]}},
            "step": 6, "placement": [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
                                     {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]},
    "cattails": {"feature": "jugcraft:cattail", "step": 9, "placement": [
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": 32},
        {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 5, "min": -5, "plateau": 0}, "y": 0,
         "z": {"type": "minecraft:trapezoid", "max": 5, "min": -5, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": {"id": "jugcraft:cattail", "properties": {"half": "lower"}}},
            {"type": "minecraft:any_of", "predicates": [
                {"type": "minecraft:matching_fluids", "fluids": ["minecraft:water", "minecraft:flowing_water"], "offset": offset}
                for offset in ([1, -1, 0], [-1, -1, 0], [0, -1, 1], [0, -1, -1])]}]}}]},
    "watergrass": {"feature": "jugcraft:watergrass", "step": 9, "placement": [
        {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": 48},
        {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}, "y": 0,
         "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"}},
        {"type": "minecraft:biome"}]},
    "duckweed": {"feature": "jugcraft:duckweed", "step": 9, "placement": [
        {"type": "minecraft:count", "count": 6}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": 16},
        {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
         "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0},
         "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}]},
    "lily_pads": {"feature": "minecraft:waterlily", "step": 9, "placement": [
        {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": 12},
        {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
         "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0},
         "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}]},
    "cranberries": {"block": "jugcraft:cranberry_bush", "properties": {"age": "3"}, "step": 9, "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": 24},
        {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 5, "min": -5, "plateau": 0}, "y": 0,
         "z": {"type": "minecraft:trapezoid", "max": 5, "min": -5, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air", "offset": [0, 1, 0]}]}}]},
    "ferns": {"block": "minecraft:fern", "step": 9, "patches": 2, "count": 24},
    "large_ferns": {"feature": "minecraft:large_fern", "step": 9, "rarity": 2, "count": 16},
    "berry_bushes": {"feature": "minecraft:berry_bush", "step": 9, "rarity": 3, "count": 12},
    # Batch 4. "survive": also check that block would survive there.
    "cacti": {"feature": "minecraft:cactus", "step": 9, "rarity": 2, "count": 6, "survive": "minecraft:cactus"},
    "bone_spikes": {"configured": {"type": "minecraft:block_column", "allowed_placement": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up", "layers": [
        {"height": {"type": "minecraft:uniform", "max_inclusive": 3, "min_inclusive": 1},
         "provider": {"id": "minecraft:bone_block", "properties": {"axis": "y"}}}], "prioritize_tip": False},
        "step": 9, "rarity": 2, "count": 3},
    "poppies_dense": {"block": "minecraft:poppy", "step": 9, "patches": 3, "count": 64},
    "rose_bushes": {"block": "minecraft:rose_bush", "properties": {"half": "lower"}, "step": 9, "rarity": 2, "count": 24},
    "peonies": {"block": "minecraft:peony", "properties": {"half": "lower"}, "step": 9, "rarity": 2, "count": 24},
    "blue_orchids": {"block": "minecraft:blue_orchid", "step": 9, "patches": 2, "count": 24},
    "lilies_of_the_valley": {"block": "minecraft:lily_of_the_valley", "step": 9, "rarity": 2, "count": 24},
    "salt_outcrops": {"configured": {"type": "minecraft:disk", "half_height": 1, "radius": {
        "type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1}, "state_provider": {"id": "jugcraft:salt_ore"},
        "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:calcite", "minecraft:coarse_dirt"]}},
        "step": 6, "placement": [{"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
                                 {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]},
    # Batch 5.
    "leaf_litter": {"feature": "minecraft:leaf_litter", "step": 9, "patches": 2, "count": 32, "on": "minecraft:grass_block"},
    "poppies": {"block": "minecraft:poppy", "step": 9, "rarity": 2, "count": 24},
    "melons": {"feature": "minecraft:melon", "step": 9, "rarity": 4, "count": 32, "on": "minecraft:grass_block"},
    "bamboo_groves": {"feature": "minecraft:bamboo_no_podzol", "step": 9, "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]},
    "hibiscus": {"feature": "jugcraft:hibiscus", "step": 9, "patches": 2, "count": 32},
    "hydrangeas": {"feature": "jugcraft:hydrangea", "step": 9, "rarity": 2, "count": 24},
    # Batch 6.
    "lava_pools": {"configured": {"type": "minecraft:lake", "barrier": {"id": "minecraft:blackstone"},
                                  "can_place_feature": {"type": "minecraft:true"},
                                  "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {
                                      "type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
                                  "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {
                                      "type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
                                  "fluid": {"id": "minecraft:lava", "properties": {"level": "0"}}},
                   "step": 1, "placement": [{"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
                                            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]},
    "canyon_lava_pools": {"feature": "minecraft:lake_lava", "step": 1, "placement": [
        {"type": "minecraft:rarity_filter", "chance": 6}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]},
    "hot_pools": {"configured": {"type": "minecraft:lake", "barrier": {"id": "minecraft:calcite"},
                                 "can_place_feature": {"type": "minecraft:true"},
                                 "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {
                                     "type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
                                 "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {
                                     "type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
                                 "fluid": {"id": "minecraft:water", "properties": {"level": "0"}}},
                  "step": 1, "placement": [{"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
                                           {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]},
    "basalt_spires": {"configured": {"type": "minecraft:block_column", "allowed_placement": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up", "layers": [
        {"height": {"type": "minecraft:uniform", "max_inclusive": 5, "min_inclusive": 2},
         "provider": {"id": "minecraft:basalt", "properties": {"axis": "y"}}}], "prioritize_tip": False},
        "step": 9, "rarity": 2, "count": 4, "on": ["minecraft:blackstone", "minecraft:basalt", "minecraft:tuff"]},
    "coal_outcrops": {"configured": {"type": "minecraft:disk", "half_height": 1, "radius": {
        "type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1}, "state_provider": {"id": "minecraft:coal_ore"},
        "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:stone", "minecraft:andesite"]}},
        "step": 6, "placement": [{"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
                                 {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]},
    # Floes of packed ice in the frozen sea, after vanilla freezes its top (step 10).
    "ice_floes": {"configured": {"type": "minecraft:disk", "half_height": 1, "radius": {
        "type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 3}, "state_provider": {"id": "minecraft:packed_ice"},
        "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:ice", "minecraft:water"]}},
        "step": 10, "placement": [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
                                  {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]},
    "sea_oats": {"feature": "jugcraft:sea_oats", "step": 9, "patches": 2, "count": 24},
    # Batch 8: the Nether, placed on every floor layer (vanilla's Nether placement).
    "nether_glowcaps": {"feature": "jugcraft:glowcap", "step": 9, "placement": [
        {"type": "minecraft:count_on_every_layer", "count": 4}, {"type": "minecraft:biome"}]},
    "nether_brambles": {"feature": "jugcraft:bramble", "step": 9, "placement": [
        {"type": "minecraft:count_on_every_layer", "count": 8}, {"type": "minecraft:biome"}]},
    "nether_huge_red_mushrooms": {"feature": "minecraft:huge_red_mushroom", "step": 9, "placement": [
        {"type": "minecraft:count_on_every_layer", "count": 1}, {"type": "minecraft:biome"}]},
    "nether_huge_brown_mushrooms": {"feature": "minecraft:huge_brown_mushroom", "step": 9, "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:count_on_every_layer", "count": 1}, {"type": "minecraft:biome"}]},
    "nether_bone_spires": {"feature": "jugcraft:bone_spikes", "step": 9, "placement": [
        {"type": "minecraft:count_on_every_layer", "count": 3}, {"type": "minecraft:biome"}]},
    "quartz_spires": {"configured": {"type": "minecraft:block_column", "allowed_placement": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up", "layers": [
        {"height": {"type": "minecraft:uniform", "max_inclusive": 6, "min_inclusive": 2},
         "provider": {"id": "minecraft:quartz_pillar", "properties": {"axis": "y"}}}], "prioritize_tip": False},
        "step": 9, "placement": [{"type": "minecraft:count_on_every_layer", "count": 1}, {"type": "minecraft:biome"}]},
    "sulfur_spikes": {"feature": "minecraft:sulfur_spike_cluster", "step": 9, "placement": [
        {"type": "minecraft:count_on_every_layer", "count": 2}, {"type": "minecraft:biome"}]},
    # Batch 9: the End.
    "dead_coral": {"configured": {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries": [
        {"data": {"id": f"minecraft:dead_{coral}", "properties": {"waterlogged": "false"}}, "weight": 1}
        for coral in ("tube_coral", "brain_coral", "bubble_coral", "fire_coral", "horn_coral",
                      "tube_coral_fan", "brain_coral_fan", "bubble_coral_fan", "fire_coral_fan", "horn_coral_fan")]}},
        "step": 9, "patches": 3, "count": 24},
    "reef_pools": {"configured": {"type": "minecraft:lake", "barrier": {"id": "minecraft:sandstone"},
                                  "can_place_feature": {"type": "minecraft:true"},
                                  "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {
                                      "type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
                                  "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {
                                      "type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
                                  "fluid": {"id": "minecraft:water", "properties": {"level": "0"}}},
                   "step": 1, "placement": [{"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
                                            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]},
    "sandstone_pillars": {"configured": {"type": "minecraft:block_column", "allowed_placement": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up", "layers": [
        {"height": {"type": "minecraft:uniform", "max_inclusive": 5, "min_inclusive": 2}, "provider": {"id": "minecraft:sandstone"}}],
        "prioritize_tip": False}, "step": 9, "rarity": 2, "count": 3},
    "obsidian_pillars": {"configured": {"type": "minecraft:block_column", "allowed_placement": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up", "layers": [
        {"height": {"type": "minecraft:uniform", "max_inclusive": 6, "min_inclusive": 2}, "provider": {"id": "minecraft:obsidian"}}],
        "prioritize_tip": False}, "step": 9, "rarity": 3, "count": 3},
    "pale_moss_carpets": {"block": "minecraft:pale_moss_carpet", "step": 9, "patches": 2, "count": 32},
    "pale_flowers": {"feature": "minecraft:pale_forest_flower", "step": 9, "rarity": 2, "count": 24},
    # Batch 7. Cave extras are placed like vanilla's lush caves': on cave floors (or under ceilings) found by scanning
    # from random heights.
    "dandelions": {"block": "minecraft:dandelion", "step": 9, "rarity": 2, "count": 24},
    "lilacs": {"block": "minecraft:lilac", "properties": {"half": "lower"}, "step": 9, "rarity": 2, "count": 16},
    "glimmerblooms": {"feature": "jugcraft:glimmerbloom", "step": 9, "rarity": 2, "count": 24},
    "frost_irises": {"feature": "jugcraft:frost_iris", "step": 9, "patches": 2, "count": 24},
    "snowpetals": {"feature": "jugcraft:snowpetals", "step": 9, "patches": 2, "count": 32},
    "toadstools": {"feature": "minecraft:red_mushroom", "step": 9, "patches": 2, "count": 32},
    "brown_toadstools": {"feature": "minecraft:brown_mushroom", "step": 9, "patches": 1, "count": 32},
    "surface_glowcaps": {"feature": "jugcraft:glowcap", "step": 9, "rarity": 2, "count": 16},
    "grotto_mud": {"configured": {"type": "minecraft:vegetation_patch", "depth": 1, "extra_bottom_block_chance": 0.0,
                                  "extra_edge_column_chance": 0.3, "ground_state": {"id": "minecraft:mud"},
                                  "replaceable": "#minecraft:moss_replaceable", "surface": "floor", "vegetation_chance": 0.25,
                                  "vegetation_feature": {"feature": "jugcraft:glowcap", "placement": []}, "vertical_range": 5,
                                  "xz_radius": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 4}},
                   "step": 9, "placement": [{"type": "minecraft:count", "count": 60}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 256},
                                                     "min_inclusive": {"above_bottom": 0}}},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": "down", "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:offset", "x": 0, "y": 1, "z": 0}, {"type": "minecraft:biome"}]},
    "grotto_moss": {"feature": "minecraft:moss_patch", "step": 9, "placement": [{"type": "minecraft:count", "count": 30}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 256},
                                                     "min_inclusive": {"above_bottom": 0}}},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": "down", "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:offset", "x": 0, "y": 1, "z": 0}, {"type": "minecraft:biome"}]},
    "grotto_glowcaps": {"feature": "jugcraft:glowcap", "step": 9, "placement": [{"type": "minecraft:count", "count": 120}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 256},
                                                     "min_inclusive": {"above_bottom": 0}}},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": "down", "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:offset", "x": 0, "y": 1, "z": 0}, {"type": "minecraft:biome"}]},
    "cave_cobwebs": {"configured": {"type": "minecraft:simple_block", "to_place": {"id": "minecraft:cobweb"}},
                     "step": 9, "placement": [{"type": "minecraft:count", "count": 100}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 256},
                                                     "min_inclusive": {"above_bottom": 0}}},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": "up", "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:offset", "x": 0, "y": -1, "z": 0}, {"type": "minecraft:biome"}]},
    "floor_cobwebs": {"feature": "jugcraft:cave_cobwebs", "step": 9, "placement": [{"type": "minecraft:count", "count": 40}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 256},
                                                     "min_inclusive": {"above_bottom": 0}}},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": "down", "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:offset", "x": 0, "y": 1, "z": 0}, {"type": "minecraft:biome"}]},
    # Cobwebs hung in the trees: inside crowns, just under leaves.
    "tree_cobwebs": {"feature": "jugcraft:cave_cobwebs", "step": 9, "placement": [
        {"type": "minecraft:count", "count": 24}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:offset", "x": 0, "y": {"type": "minecraft:uniform", "max_inclusive": -2, "min_inclusive": -6}, "z": 0},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:leaves", "offset": [0, 1, 0]}]}}]},
    # The graveyard flora (docs/features/graveyard-flora.md), for the haunted and dying biomes.
    "flora_withered_grass": {"feature": "jugcraft:withered_grass", "step": 9, "patches": 3, "count": 48},
    "flora_tall_withered_grass": {"feature": "jugcraft:tall_withered_grass", "step": 9, "patches": 1, "count": 24},
    "flora_ghost_pipe": {"feature": "jugcraft:ghost_pipe", "step": 9, "rarity": 2, "count": 16},
    "flora_dead_mans_fingers": {"feature": "jugcraft:dead_mans_fingers", "step": 9, "rarity": 3, "count": 8},
    "flora_spider_lily": {"feature": "jugcraft:spider_lily", "step": 9, "rarity": 2, "count": 24},
    "flora_grave_moss": {"feature": "jugcraft:grave_moss", "step": 9, "patches": 1, "count": 32},
    "flora_black_rose": {"feature": "jugcraft:black_rose", "step": 9, "rarity": 4, "count": 8},
    "flora_ghost_fern": {"feature": "jugcraft:ghost_fern", "step": 9, "patches": 3, "count": 48},
    "flora_large_ghost_fern": {"feature": "jugcraft:large_ghost_fern", "step": 9, "patches": 1, "count": 24},
    "flora_deadly_nightshade": {"feature": "jugcraft:deadly_nightshade", "step": 9, "rarity": 2, "count": 16},
    "flora_foxglove": {"feature": "jugcraft:foxglove", "step": 9, "rarity": 2, "count": 16},
    "flora_bleeding_heart": {"feature": "jugcraft:bleeding_heart", "step": 9, "rarity": 2, "count": 16},
    "flora_snowdrop": {"feature": "jugcraft:snowdrop", "step": 9, "patches": 2, "count": 32},
    "flora_funeral_lily": {"feature": "jugcraft:funeral_lily", "step": 9, "rarity": 2, "count": 16},
    "flora_asphodel": {"feature": "jugcraft:asphodel", "step": 9, "patches": 1, "count": 24},
    "flora_wild_mandrake": {"feature": "jugcraft:wild_mandrake", "step": 9, "rarity": 4, "count": 12, "on": "minecraft:grass_block"},
    # Strands hung under leaves and branches, and ivy over trunks and stones (plants_data's placements).
    "flora_shroud_moss": {"feature": "jugcraft:shroud_moss", "step": 9, "placement": "hanging", "count": 48},
    "flora_creeping_ivy": {"feature": "jugcraft:creeping_ivy", "step": 9, "placement": "vine", "count": 24},
}

# Each biome: its base, climate values, trees (count: [usual, sometimes]; default and weighted picks of placed
# features; None: no trees), base features it drops or swaps, extras it adds, mob changes ("creatures" and "monsters":
# [entity, weight, min, max] lists replacing the base's; [] for none), tags, and seasons.
# Nether and End biomes ("dimension") are placed by dimension_file() instead of region rules, and lay their own
# "ground": patches of {block: weight} over the floors of every layer (vanilla's vegetation patch feature, so no
# vanilla material rule is copied), optionally grown with a plant; "surface" is for Overworld biomes.
# "seasons": in #jugcraft:has_seasons and #jugcraft:has_winter_snow (four-season biomes; not the frozen ones);
# "winter_snow": False keeps a seasonal biome out of #jugcraft:has_winter_snow (mild winters).
BIOMES = {
    "coniferous_forest": {
        "display": "Coniferous Forest", "base": "taiga", "temperature": 0.25, "downfall": 0.8, "seasons": True,
        "trees": {"count": [9, 10], "default": "jugcraft:fir_checked", "picks": [
            ["jugcraft:tall_fir_checked", 0.2], ["jugcraft:larch_checked", 0.125],
            ["jugcraft:stunted_fir_checked", 0.1143], ["jugcraft:dead_snag_checked", 0.0363],
            ["jugcraft:dead_snag_bent_checked", 0.0126], ["jugcraft:fallen_fir_tree", 0.0339]]},
        "tags": ["c:is_coniferous_tree"],
    },
    "snowy_coniferous_forest": {
        "display": "Snowy Coniferous Forest", "base": "snowy_taiga", "temperature": -0.5, "downfall": 0.4, "seasons": False,
        "trees": {"count": [7, 8], "default": "jugcraft:fir_checked", "picks": [
            ["jugcraft:tall_fir_checked", 0.2], ["jugcraft:subalpine_fir_checked", 0.25],
            ["jugcraft:stunted_fir_checked", 0.1667], ["jugcraft:fallen_fir_tree", 0.04]]},
        "tags": ["c:is_coniferous_tree", "c:is_snowy"],
    },
    "maple_woods": {
        "display": "Maple Woods", "base": "forest", "temperature": 0.45, "downfall": 0.8, "seasons": True,
        "trees": {"count": [10, 11], "default": "jugcraft:maple_checked", "picks": [
            ["jugcraft:big_maple_checked", 0.12], ["jugcraft:fir_checked", 0.1364],
            ["jugcraft:fallen_maple_tree", 0.0132]]},
        "tags": ["minecraft:has_structure/village_taiga", "c:is_deciduous_tree"],
    },
    "seasonal_forest": {
        "display": "Seasonal Forest", "base": "forest", "temperature": 0.6, "downfall": 0.8, "seasons": True,
        "trees": {"count": [10, 11], "default": "minecraft:oak_bees_0002_leaf_litter", "picks": [
            ["jugcraft:maple_checked", 0.35], ["jugcraft:aspen_checked", 0.2], ["jugcraft:big_maple_checked", 0.0577],
            ["jugcraft:fir_checked", 0.0816], ["jugcraft:fallen_maple_tree", 0.0222]]},
        "extras": ["patch_pumpkin_dense"],
        "tags": ["minecraft:has_structure/village_plains", "c:is_deciduous_tree"],
    },
    "aspen_glade": {
        "display": "Aspen Glade", "base": "birch_forest", "temperature": 0.5, "downfall": 0.6, "seasons": True,
        "trees": {"count": [10, 11], "default": "jugcraft:aspen_checked", "picks": [
            ["jugcraft:maple_checked", 0.1], ["jugcraft:young_aspen_checked", 0.1667],
            ["jugcraft:subalpine_fir_checked", 0.0667], ["jugcraft:fallen_aspen_tree", 0.0143]]},
        "tags": ["minecraft:has_structure/village_plains", "c:is_deciduous_tree", "c:is_birch_forest"],
    },
    "dead_forest": {
        "display": "Dead Forest", "base": "plains", "temperature": 0.4, "downfall": 0.25, "seasons": True,
        "effects": {"grass_color": "#a39a5e", "foliage_color": "#9c8a52"},
        "trees": {"count": [2, 3], "default": "jugcraft:dead_tree_checked", "picks": [
            ["minecraft:spruce_checked", 0.1], ["minecraft:oak_checked", 0.1111],
            ["jugcraft:dead_snag_checked", 0.3281], ["jugcraft:dead_snag_bent_checked", 0.1628],
            ["jugcraft:fallen_dead_tree", 0.1111]]},
        "drop": ["minecraft:patch_tall_grass_2", "minecraft:flower_plains"],
        "swap": {"minecraft:patch_grass_plain": "minecraft:patch_grass_badlands"},
        "extras": ["patch_dead_bush", "flora_withered_grass", "flora_tall_withered_grass"],
        "creatures": [["minecraft:rabbit", 4, 2, 3], ["minecraft:wolf", 2, 2, 4]],
        "untags": ["minecraft:has_structure/village_plains"],
        "tags": ["c:is_dead"],
    },
    # Tundra is as warm as vanilla taiga (0.25), so vanilla snow only lies above about y 160: lower down its snow is
    # Jugcraft's winter.
    "tundra": {
        "display": "Tundra", "base": "plains", "temperature": 0.25, "downfall": 0.5, "seasons": True,
        "effects": {"grass_color": "#9a9a5e", "foliage_color": "#a07a3c"},
        "trees": {"count": [3, 4], "default": "jugcraft:maple_bush_checked", "picks": [
            ["jugcraft:willow_bush_checked", 0.3], ["jugcraft:fir_bush_checked", 0.2857]]},
        "drop": ["minecraft:flower_plains", "minecraft:patch_tall_grass_2"],
        "swap": {"minecraft:patch_grass_plain": "minecraft:patch_grass_taiga_2"},
        "extras": ["tundra_rocks", "patch_dead_bush"],
        "creatures": [["minecraft:rabbit", 6, 2, 3], ["minecraft:fox", 4, 2, 4], ["minecraft:wolf", 2, 2, 4]],
        "untags": ["minecraft:has_structure/village_plains"],
        "tags": ["minecraft:has_structure/village_taiga"],
    },
    "snowy_forest": {
        "display": "Snowy Forest", "base": "snowy_plains", "temperature": -0.3, "downfall": 0.5, "seasons": False,
        "trees": {"count": [7, 8], "default": "minecraft:oak_checked", "picks": [
            ["jugcraft:fir_checked", 0.3], ["jugcraft:maple_checked", 0.2], ["jugcraft:aspen_checked", 0.3571]]},
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:pig", 10, 4, 4], ["minecraft:chicken", 10, 4, 4],
                      ["minecraft:cow", 8, 4, 4], ["minecraft:rabbit", 6, 2, 3], ["minecraft:wolf", 5, 4, 4]],
        "untags": ["minecraft:has_structure/village_snowy"],
        "tags": ["c:is_snowy", "c:is_deciduous_tree"],
    },
    "muskeg": {
        "display": "Muskeg", "base": "snowy_plains", "temperature": -0.2, "downfall": 0.9, "seasons": False,
        "effects": {"grass_color": "#8c9a6a", "foliage_color": "#7f8f5a"},
        "trees": {"count": [2, 3], "default": "jugcraft:tamarack_checked", "picks": [
            ["jugcraft:bog_fir_checked", 0.3], ["jugcraft:dead_snag_checked", 0.2143],
            ["jugcraft:dead_snag_bent_checked", 0.0909], ["jugcraft:dead_tree_checked", 0.2],
            ["jugcraft:fallen_dead_tree", 0.075], ["jugcraft:fallen_larch_tree", 0.0541]]},
        "creatures": [["minecraft:rabbit", 6, 2, 3]],
        "untags": ["minecraft:has_structure/village_snowy"],
        "tags": ["c:is_snowy", "c:is_dead"],
    },
    # ---------------------------------------------------------------- batch 3: wetlands (bases' swamp colours unless set)
    # A cool cranberry bog: reddish-orange grass, maple scrub, tamaracks and thin bog firs, bushes, cranberries in its
    # shallow, muddy pools.
    "bog": {
        "display": "Bog", "base": "swamp", "temperature": 0.4, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#ad6c3c", "foliage_color": "#9c5a30", "grass_color_modifier": "none", "water_color": "#4f6a54"},
        "trees": {"count": [2, 3], "default": "jugcraft:maple_bush_checked", "picks": [
            ["jugcraft:dead_tree_checked", 0.1], ["jugcraft:tamarack_checked", 0.3333],
            ["jugcraft:bog_fir_checked", 0.25]]},
        "drop": ["minecraft:flower_swamp"],
        "extras": ["mud", "cranberries", "cattails", "watergrass", "bushes_dense", "berry_bushes"],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # Dark, muddy ponds with sparse dead trees and grey snags standing in them, and fallen logs; no animals; villages.
    "dead_swamp": {
        "display": "Dead Swamp", "base": "swamp", "temperature": 0.35, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#6f6a45", "foliage_color": "#6a6440", "grass_color_modifier": "none", "water_color": "#3e4528"},
        "trees": {"count": [1, 2], "default": "jugcraft:dead_tree_checked", "picks": [
            ["jugcraft:dead_snag_checked", 0.375], ["jugcraft:dead_snag_bent_checked", 0.2],
            ["jugcraft:fallen_dead_tree", 0.1]], "water_depth": 2},
        "drop": ["minecraft:flower_swamp", "minecraft:patch_pumpkin"],
        "extras": ["mud", "ponds", "cattails", "patch_dead_bush", "flora_spider_lily", "flora_withered_grass", "flora_shroud_moss"],
        "creatures": [],
        "tags": ["c:is_swamp", "c:is_dead", "minecraft:has_structure/village_taiga"],
    },
    # A verdant swamp: vibrant grass and blue water, tall oaks hung with vines, willows and red maples, cattails, ferns
    # and berries.
    "lush_swamp": {
        "display": "Lush Swamp", "base": "swamp", "temperature": 0.7, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#56b13a", "foliage_color": "#4aa22e", "grass_color_modifier": "none", "water_color": "#2e86a8"},
        "trees": {"count": [3, 4], "default": "jugcraft:tall_vine_oak_checked", "picks": [
            ["jugcraft:willow_checked", 0.3], ["jugcraft:maple_checked", 0.1429]], "water_depth": 2},
        "extras": ["cattails", "watergrass", "duckweed", "lily_pads", "ferns", "berry_bushes"],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # More forest than swamp: willows, vine-hung oaks and swamp azaleas over moss, duckweed and lily pads; animals.
    "swamp_woods": {
        "display": "Swamp Woods", "base": "swamp", "temperature": 0.65, "downfall": 0.9, "seasons": True,
        "trees": {"count": [7, 9], "default": "jugcraft:willow_checked", "picks": [
            ["jugcraft:tall_vine_oak_checked", 0.2], ["jugcraft:azalea_tree_checked", 0.125]], "water_depth": 2},
        "extras": ["duckweed", "lily_pads", "cattails", "ferns"],
        "creatures": [["minecraft:frog", 10, 2, 5], ["minecraft:sheep", 8, 4, 4], ["minecraft:pig", 8, 4, 4], ["minecraft:cow", 6, 4, 4],
                      ["minecraft:chicken", 8, 4, 4]],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # A warm, overcast bayou: willows standing in the water, trailing moss (vines), cattails and ferns on mud.
    "bayou": {
        "display": "Bayou", "base": "mangrove_swamp", "temperature": 0.85, "downfall": 0.95, "seasons": True, "winter_snow": False,
        "effects": {"water_color": "#4d6a4c"},
        "attributes": {"minecraft:visual/fog_color": "#a9b8a6"},
        "trees": {"count": [5, 6], "default": "jugcraft:willow_checked", "picks": [["jugcraft:tall_vine_oak_checked", 0.15]],
                  "water_depth": 3},
        "extras": ["mud", "cattails", "ferns", "large_ferns", "duckweed", "flora_shroud_moss"],
        "untags": ["minecraft:has_structure/trail_ruins"],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # A warm, flooded plain: willow scrub and a few oaks, tall grass, orange cosmos, and lily pads and watergrass in the
    # floods.
    "floodplain": {
        "display": "Floodplain", "base": "mangrove_swamp", "temperature": 0.9, "downfall": 0.9, "seasons": True, "winter_snow": False,
        "effects": {"grass_color": "#6cb041", "foliage_color": "#5ea034", "grass_color_modifier": "none", "water_color": "#3d7f9a"},
        "trees": {"count": [3, 4], "default": "jugcraft:willow_bush_checked", "picks": [
            ["minecraft:oak_checked", 0.3]], "water_depth": 1},
        "extras": ["tall_grass_dense", "orange_cosmos", "watergrass", "lily_pads"],
        "tags": ["c:is_wet"],
    },
    # A dead forest of grey snags and dead trunks, with a living fringe of cedars, around many lakes, with clay.
    "ghost_forest": {
        "display": "Ghost Forest", "base": "dark_forest", "base_trees": "minecraft:dark_forest_vegetation",
        "temperature": 0.5, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#8b9478", "foliage_color": "#7d8670", "grass_color_modifier": "none"},
        "trees": {"count": [3, 4], "default": "jugcraft:dead_snag_checked", "picks": [
            ["jugcraft:dead_tree_checked", 0.3], ["jugcraft:cedar_checked", 0.2143],
            ["jugcraft:dead_snag_bent_checked", 0.25]]},
        "drop": ["minecraft:forest_flowers"],
        "extras": ["ponds", "patch_dead_bush", "flora_withered_grass", "flora_tall_withered_grass", "flora_ghost_pipe",
                   "flora_dead_mans_fingers", "flora_spider_lily", "flora_grave_moss", "flora_black_rose", "flora_shroud_moss"],
        "untags": ["minecraft:is_forest"],
        "tags": ["c:is_dead", "c:is_wet"],
    },
    # A dusky mire under a dense canopy of big oaks and dark oaks, with mud, sludgy pools and algae; no animals; villages.
    "sludge_mire": {
        "display": "Sludge Mire", "base": "dark_forest", "base_trees": "minecraft:dark_forest_vegetation",
        "temperature": 0.6, "downfall": 0.95, "seasons": True,
        "effects": {"grass_color": "#5d6b34", "foliage_color": "#55632e", "grass_color_modifier": "none", "water_color": "#3d4a24"},
        "trees": {"count": [10, 12], "default": "minecraft:dark_oak_checked", "picks": [
            ["minecraft:fancy_oak_checked", 0.3], ["jugcraft:tall_vine_oak_checked", 0.2]]},
        "drop": ["minecraft:forest_flowers", "minecraft:flower_default"],
        "extras": ["mud", "ponds", "duckweed", "flora_deadly_nightshade", "flora_bleeding_heart", "flora_shroud_moss"],
        "creatures": [],
        "untags": ["minecraft:is_forest"],
        "tags": ["c:is_swamp", "c:is_wet", "minecraft:has_structure/village_taiga"],
    },
    # A river lush with duckweed, lily pads and watergrass, willow scrub and willows along its banks.
    "lush_river": {
        "display": "Lush River", "base": "river", "temperature": 0.7, "downfall": 0.8, "seasons": True, "winter_snow": False,
        "effects": {"grass_color": "#5eb33f", "foliage_color": "#52a434", "water_color": "#2f9a96"},
        "trees": {"count": [1, 2], "default": "jugcraft:willow_bush_checked", "picks": [
            ["jugcraft:willow_checked", 0.2], ["jugcraft:oak_bush_checked", 0.125]]},
        "extras": ["duckweed", "lily_pads", "watergrass", "cattails"],
        "tags": ["c:is_wet"],
    },
    # A cool fen: thin bog firs, tamaracks and willow scrub over muddy pools, cattails and lily pads; cows, sheep and
    # slimes.
    "fen": {
        "display": "Fen", "base": "old_growth_spruce_taiga", "temperature": 0.3, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#6c8f4c", "foliage_color": "#5d8040"},
        "trees": {"count": [4, 5], "default": "jugcraft:bog_fir_checked", "picks": [
            ["jugcraft:tamarack_checked", 0.35], ["jugcraft:willow_bush_checked", 0.2308]]},
        "extras": ["mud", "ponds", "cattails", "lily_pads"],
        "untags": ["minecraft:is_taiga"],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # Forest of oaks, firs and larches broken by many lakes with muddy shores, cattails and lily pads.
    "lake_district": {
        "display": "Lake District", "base": "forest", "temperature": 0.5, "downfall": 0.8, "seasons": True,
        "trees": {"count": [6, 7], "default": "minecraft:oak_checked", "picks": [
            ["jugcraft:fir_checked", 0.3], ["jugcraft:larch_checked", 0.1429], ["minecraft:fancy_oak_checked", 0.1]]},
        "extras": ["ponds", "mud", "cattails", "lily_pads"],
        "tags": ["c:is_wet"],
    },
    # Muddy flats with brown ponds, cattails and lily pads, no trees, no animals.
    "quagmire": {
        "display": "Quagmire", "base": "swamp", "temperature": 0.4, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#7d7a4a", "foliage_color": "#6f6c40", "grass_color_modifier": "none", "water_color": "#6b5a3c"},
        "trees": None,
        "drop": ["minecraft:flower_swamp", "minecraft:patch_pumpkin"],
        "extras": ["mud", "ponds", "cattails"],
        "creatures": [],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # A wide, green plain of shallow lakes full of watergrass, ringed by cattails; tall grass, no trees, no flowers.
    "marsh": {
        "display": "Marsh", "base": "swamp", "temperature": 0.5, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#5f9f46", "foliage_color": "#559340", "grass_color_modifier": "none"},
        "trees": None,
        "drop": ["minecraft:flower_swamp"],
        "extras": ["ponds", "watergrass", "cattails", "tall_grass_dense"],
        "creatures": [],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # Murky grass and mud with ferns, cedars, willows and a few spruces, cattails and sugar cane, and purple water;
    # villages.
    "wetland": {
        "display": "Wetland", "base": "swamp", "temperature": 0.55, "downfall": 0.9, "seasons": True,
        "effects": {"water_color": "#6b5c9c"},
        "trees": {"count": [2, 3], "default": "jugcraft:cedar_checked", "picks": [
            ["jugcraft:willow_checked", 0.4], ["minecraft:spruce_checked", 0.1667]], "water_depth": 1},
        "extras": ["mud", "ferns", "cattails", "watergrass"],
        "tags": ["c:is_swamp", "c:is_wet", "minecraft:has_structure/village_taiga"],
    },
    # ---------------------------------------------------------------- batch 4: warm and dry ("surface": its own ground)
    # Hot, dry grassland under a lilac sky: pines, oaks and brush oaks, bushes, cacti and bone pillars; villages.
    "dryland": {
        "display": "Dryland", "base": "savanna", "temperature": 1.4, "downfall": 0.15, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#b2aa5a", "foliage_color": "#9c9a50"},
        "attributes": {"minecraft:visual/sky_color": "#a99fd8"},
        "trees": {"count": [2, 3], "default": "minecraft:pine_checked", "picks": [
            ["minecraft:oak_checked", 0.25], ["jugcraft:oak_bush_checked", 0.35]]},
        "extras": ["bushes_dense", "patch_dead_bush", "cacti", "bone_spikes"],
        "untags": ["minecraft:has_structure/village_savanna"],
        "tags": ["minecraft:has_structure/village_desert", "c:is_dry", "c:is_hot"],
    },
    # Low hills of sand and grass with small acacias, cacti, dead shrubs and little lakes.
    "xeric_shrubland": {
        "display": "Xeric Shrubland", "base": "savanna_plateau", "temperature": 1.6, "downfall": 0.1, "seasons": False,
        "precipitation": False,
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.35, 0.35, "minecraft:sand"]]},
        "trees": {"count": [1, 2], "default": "jugcraft:desert_acacia_checked", "picks": []},
        "extras": ["cacti", "patch_dead_bush", "dry_grass", "ponds"],
        "tags": ["c:is_dry", "c:is_hot"],
    },
    # A forest of blossoming jacarandas with cherries, oaks and azaleas, blue orchids and lilies of the valley.
    "jacaranda_glade": {
        "display": "Jacaranda Glade", "base": "forest", "temperature": 0.75, "downfall": 0.8, "seasons": True, "winter_snow": False,
        "trees": {"count": [6, 7], "default": "jugcraft:jacaranda_checked", "picks": [
            ["minecraft:cherry_checked", 0.15], ["minecraft:oak_bees_002", 0.2], ["jugcraft:azalea_tree_checked", 0.1]]},
        "extras": ["blue_orchids", "lilies_of_the_valley"],
        "tags": ["c:is_flower_forest"],
    },
    # Orange dunes of red sand that see rain: dune grass, acacia brush and bushes, a few wildflowers; villages.
    "lush_desert": {
        "display": "Lush Desert", "base": "desert", "temperature": 1.0, "downfall": 0.3, "seasons": False, "precipitation": True,
        "surface": {"floor": "minecraft:red_sand", "under": "minecraft:red_sandstone", "patches": [[-0.2, 0.2, "minecraft:sand"]]},
        "trees": {"count": [1, 2], "default": "jugcraft:desert_acacia_checked", "picks": [["jugcraft:oak_bush_checked", 0.3]]},
        "extras": ["dry_grass", "meadow_wildflowers", "bushes_dense"],
        "tags": ["c:is_dry", "c:is_hot", "c:is_desert"],
    },
    # Dry, coarse flats where pillars of bone stand among dead bushes and dry grass.
    "bone_flats": {
        "display": "Bone Flats", "base": "desert", "temperature": 1.8, "downfall": 0.0, "seasons": False, "precipitation": False,
        "surface": {"floor": "minecraft:coarse_dirt", "under": "minecraft:dirt", "patches": [[-0.2, 0.2, "minecraft:sand"]]},
        "trees": None,
        "extras": ["bone_spikes", "dry_grass", "patch_dead_bush"],
        "tags": ["c:is_dry", "c:is_hot"],
    },
    # A river of the hot, dry lands, between sandy banks with dead bushes.
    "dry_river": {
        "display": "Dry River", "base": "river", "temperature": 2.0, "downfall": 0.0, "seasons": False, "precipitation": False,
        "effects": {"water_color": "#4a9db0"},
        "surface": {"floor": "rule:minecraft:overworld/sand_or_sandstone_if_ceiling", "under": "minecraft:sand"},
        "trees": None,
        "drop": ["minecraft:patch_bush", "minecraft:flower_default", "minecraft:patch_grass_badlands"],
        "extras": ["patch_dead_bush"],
        "tags": ["c:is_dry", "c:is_hot"],
    },
    # A cold, gravelly waste with coarse dirt, dry grass and treacherous powder snow; no animals; snowy villages. Too
    # dry for snow: no snowfall, and none laid when the land is made (vanilla lays it on any freezing biome).
    "cold_desert": {
        "display": "Cold Desert", "base": "snowy_plains", "temperature": -0.3, "downfall": 0.05, "seasons": False,
        "precipitation": False,
        "surface": {"floor": "minecraft:gravel", "under": "minecraft:dirt",
                    "patches": [[-0.3, 0.0, "minecraft:coarse_dirt"], [0.45, 0.6, "minecraft:powder_snow"]]},
        "trees": None,
        "drop": ["minecraft:freeze_top_layer"],
        "extras": ["dry_grass", "patch_dead_bush"],
        "creatures": [],
        "tags": ["c:is_dry", "c:is_cold", "c:is_desert"],
    },
    # Flat scrub of dry grass and wildflowers with scattered oak bushes; animals.
    "scrubland": {
        "display": "Scrubland", "base": "savanna", "temperature": 1.2, "downfall": 0.2, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#aaa65c", "foliage_color": "#949650"},
        "trees": {"count": [1, 2], "default": "jugcraft:oak_bush_checked", "picks": []},
        "extras": ["dry_grass", "meadow_wildflowers"],
        "tags": ["c:is_dry", "c:is_hot"],
    },
    # A savanna field of poppies and rose bushes on grass blotched with coarse dirt; no trees, no animals.
    "lush_savanna": {
        "display": "Lush Savanna", "base": "savanna", "temperature": 1.1, "downfall": 0.5, "seasons": False, "precipitation": True,
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.15, 0.15, "minecraft:coarse_dirt"]]},
        "trees": None,
        "extras": ["poppies_dense", "rose_bushes"],
        "creatures": [],
        "tags": ["c:is_hot"],
    },
    # Red sand patched with grass, tiny acacias and cacti, pools of water and lava; villages and desert temples.
    "outback": {
        "display": "Outback", "base": "desert", "temperature": 2.0, "downfall": 0.0, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#b8a858", "foliage_color": "#a49a4c"},
        "surface": {"floor": "minecraft:red_sand", "under": "minecraft:red_sandstone",
                    "patches": [[-0.25, 0.1, "minecraft:grass_block"]]},
        "trees": {"count": [1, 2], "default": "jugcraft:desert_acacia_checked", "picks": []},
        "extras": ["dry_grass"],
        "tags": ["c:is_dry", "c:is_hot"],
    },
    # Sand around pools of water, with palms, grass and sugar cane.
    "oasis": {
        "display": "Oasis", "base": "desert", "temperature": 1.6, "downfall": 0.4, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#7fbf4a", "foliage_color": "#6aad3c"},
        "surface": {"floor": "rule:minecraft:overworld/sand_or_sandstone_if_ceiling", "under": "minecraft:sand",
                    "patches": [[0.0, 0.45, "minecraft:grass_block"]]},
        "trees": {"count": [2, 3], "default": "jugcraft:palm_checked", "picks": []},
        "extras": ["ponds", "tall_grass_dense"],
        "tags": ["c:is_hot", "c:is_desert"],
    },
    # A cruel waste of dried salt (pale calcite, rock salt) with dead trees and snags on its coarse dirt, and dead grass;
    # no animals; husks.
    "wasteland": {
        "display": "Wasteland", "base": "desert", "temperature": 2.0, "downfall": 0.0, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#9a9068", "foliage_color": "#8a8060"},
        "surface": {"floor": "minecraft:calcite", "under": "minecraft:calcite", "patches": [[-0.15, 0.15, "minecraft:coarse_dirt"]]},
        "trees": {"count": [1, 1], "default": "jugcraft:dead_tree_checked", "picks": [
            ["jugcraft:dead_snag_checked", 0.3], ["jugcraft:dead_snag_bent_checked", 0.1429]]},
        "extras": ["salt_outcrops", "dry_grass", "patch_dead_bush"],
        "creatures": [],
        "untags": ["minecraft:has_structure/village_desert"],
        "tags": ["c:is_dry", "c:is_hot", "c:is_dead", "c:is_wasteland"],
    },
    # A burnt forest: grey snags and dead trunks on scorched grass and coarse dirt, fallen logs, ash drifting in the air.
    "burnt_forest": {
        "display": "Burnt Forest", "base": "forest", "temperature": 0.9, "downfall": 0.3, "seasons": False,
        "effects": {"grass_color": "#5f5b48", "foliage_color": "#5a5446"},
        "attributes": {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.01}],
                                                               "modifier": "append"},
                       "minecraft:visual/fog_color": "#8c867a"},
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.3, 0.3, "minecraft:coarse_dirt"]]},
        "trees": {"count": [3, 4], "default": "jugcraft:dead_tree_checked", "picks": [
            ["jugcraft:oak_bush_checked", 0.1], ["jugcraft:dead_snag_checked", 0.375],
            ["jugcraft:dead_snag_bent_checked", 0.2], ["jugcraft:fallen_dead_tree", 0.1111]]},
        "drop": ["minecraft:forest_flowers", "minecraft:flower_default"],
        "extras": ["patch_dead_bush"],
        "untags": ["minecraft:is_forest"],
        "tags": ["c:is_dead", "c:is_dry"],
    },
    # A Mediterranean forest: tall cypresses, oaks and dark oaks, lemon and orange trees, shrubs, peonies; villages.
    "mediterranean_forest": {
        "display": "Mediterranean Forest", "base": "forest", "temperature": 0.8, "downfall": 0.5, "seasons": True, "winter_snow": False,
        "trees": {"count": [5, 6], "default": "jugcraft:cypress_checked", "picks": [
            ["minecraft:oak_checked", 0.3], ["minecraft:dark_oak_checked", 0.15], ["jugcraft:oak_bush_checked", 0.2],
            ["jugcraft:lemon_checked", 0.1], ["jugcraft:orange_checked", 0.1]]},
        "extras": ["peonies", "bushes_dense"],
        "tags": ["minecraft:has_structure/village_plains"],
    },
    # An orchard: chestnut, pear, peach and plum trees (the agriculture branch's), oaks with bees and flowering azaleas,
    # rose bushes and daisies.
    "orchard": {
        "display": "Orchard", "base": "plains", "temperature": 0.8, "downfall": 0.5, "seasons": True,
        "effects": {"grass_color": "#8fbd5a", "foliage_color": "#77ad48"},
        "trees": {"count": [2, 3], "default": "jugcraft:chestnut_checked", "picks": [
            ["minecraft:oak_bees_002", 0.2], ["jugcraft:azalea_tree_checked", 0.1], ["jugcraft:pear_checked", 0.2],
            ["jugcraft:peach_checked", 0.2], ["jugcraft:plum_checked", 0.15]]},
        "extras": ["rose_bushes", "oxeye_daisies"],
        "tags": ["c:is_plains"],
    },
    # ---------------------------------------------------------------- batch 5: big trees and rainforests
    # A hot, steaming rainforest: tall mahoganies and giant ones, jungle trees and bushes and banana trees over ferns,
    # orange cosmos and puddles; only parrots.
    "rainforest": {
        "display": "Rainforest", "base": "jungle", "temperature": 0.95, "downfall": 0.95, "seasons": False,
        "effects": {"grass_color": "#3fa82c", "foliage_color": "#2f9a24"},
        "trees": {"count": [24, 28], "default": "jugcraft:mahogany_checked", "picks": [
            ["jugcraft:giant_mahogany_checked", 0.12], ["minecraft:mega_jungle_tree_checked", 0.08],
            ["minecraft:jungle_tree", 0.15], ["minecraft:jungle_bush", 0.25], ["jugcraft:banana_checked", 0.1]]},
        "extras": ["ponds", "ferns", "large_ferns", "orange_cosmos"],
        "creatures": [["minecraft:parrot", 40, 1, 2]],
        "tags": ["c:is_tropical", "c:is_wet"],
    },
    # A forest of tall eucalyptus, their bark streaked in rainbow colours, over oak scrub, melons and wildflowers;
    # parrots.
    "eucalyptus_forest": {
        "display": "Eucalyptus Forest", "base": "sparse_jungle", "temperature": 0.95, "downfall": 0.8, "seasons": False,
        "effects": {"grass_color": "#7fb653", "foliage_color": "#6aa84a"},
        "trees": {"count": [8, 9], "default": "jugcraft:eucalyptus_checked", "picks": [
            ["jugcraft:big_eucalyptus_checked", 0.3], ["jugcraft:oak_bush_checked", 0.25]]},
        "extras": ["field_flowers", "melons", "bushes_dense"],
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:pig", 10, 4, 4], ["minecraft:chicken", 10, 4, 4],
                      ["minecraft:cow", 8, 4, 4], ["minecraft:parrot", 20, 1, 2]],
        "tags": ["c:is_tropical"],
    },
    # Bright green islands of palms, flowering azaleas, jungle bushes and banana trees, hibiscus, hydrangeas and bamboo;
    # parrots.
    "tropics": {
        "display": "Tropics", "base": "sparse_jungle", "temperature": 0.95, "downfall": 0.85, "seasons": False,
        "effects": {"grass_color": "#5fcf3a", "foliage_color": "#4cc02e", "water_color": "#3fc7d8"},
        "trees": {"count": [4, 5], "default": "jugcraft:palm_checked", "picks": [
            ["jugcraft:small_palm_checked", 0.25], ["minecraft:jungle_bush", 0.2], ["jugcraft:azalea_tree_checked", 0.15],
            ["jugcraft:banana_checked", 0.15]]},
        "extras": ["hibiscus", "hydrangeas", "bamboo_groves"],
        "creatures": [["minecraft:parrot", 30, 1, 2], ["minecraft:chicken", 10, 4, 4], ["minecraft:pig", 10, 4, 4],
                      ["minecraft:sheep", 8, 4, 4]],
        "tags": ["c:is_tropical", "minecraft:has_structure/jungle_temple"],
    },
    # Warm, green, plains-like country with flowering azaleas, oaks, birches, small palms, vine-hung oaks and lemon and
    # orange trees, hydrangeas and sugar cane; villages.
    "subtropics": {
        "display": "Subtropics", "base": "plains", "temperature": 0.9, "downfall": 0.7, "seasons": False,
        "effects": {"grass_color": "#6cc043", "foliage_color": "#5ab035"},
        "trees": {"count": [2, 3], "default": "jugcraft:azalea_tree_checked", "picks": [
            ["minecraft:oak_checked", 0.3], ["minecraft:birch_checked", 0.1], ["jugcraft:small_palm_checked", 0.2],
            ["jugcraft:tall_vine_oak_checked", 0.1], ["jugcraft:lemon_checked", 0.15], ["jugcraft:orange_checked", 0.15]]},
        "extras": ["hydrangeas", "field_flowers"],
        "tags": [],
    },
    # A dense forest of big, spreading oaks, with dark oaks and a rare great oak among them; leaf litter and ferns
    # below; woodland mansions.
    "dense_forest": {
        "display": "Dense Forest", "base": "forest", "temperature": 0.7, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#5a9a3a", "foliage_color": "#4a8a2c"},
        "trees": {"count": [14, 16], "default": "minecraft:fancy_oak_checked", "picks": [
            ["minecraft:oak_checked", 0.3], ["minecraft:dark_oak_checked", 0.1], ["jugcraft:oak_bush_checked", 0.1111],
            ["jugcraft:great_oak_checked", 0.0357]]},
        "extras": ["leaf_litter", "ferns"],
        "tags": ["minecraft:has_structure/woodland_mansion"],
    },
    # A forest of giant redwoods two blocks wide and tall single ones, with a few firs and mossy maples, on podzol
    # broken by moss; ferns and tall ferns.
    "redwood_forest": {
        "display": "Redwood Forest", "base": "old_growth_pine_taiga", "temperature": 0.5, "downfall": 0.8, "seasons": True,
        "winter_snow": False,
        "surface": {"floor": "minecraft:podzol", "under": "minecraft:dirt",
                    "patches": [[-0.12, 0.12, "minecraft:moss_block"], [0.4, 0.55, "minecraft:coarse_dirt"]]},
        "trees": {"count": [10, 11], "default": "jugcraft:redwood_checked", "picks": [
            ["jugcraft:giant_redwood_checked", 0.35], ["jugcraft:fir_checked", 0.0923],
            ["jugcraft:mossy_maple_checked", 0.0847], ["jugcraft:fallen_redwood_tree", 0.037]]},
        "extras": ["ferns", "large_ferns"],
        "tags": ["c:is_coniferous_tree", "c:is_old_growth"],
    },
    # A cool, dripping rainforest under firs and redwoods, moss-hung bigleaf maples and willows, thick with ferns.
    "temperate_rainforest": {
        "display": "Temperate Rainforest", "base": "old_growth_spruce_taiga", "temperature": 0.45, "downfall": 0.95,
        "seasons": True, "winter_snow": False,
        "effects": {"grass_color": "#78c84a", "foliage_color": "#62b53a"},
        "trees": {"count": [11, 12], "default": "jugcraft:fir_checked", "picks": [
            ["jugcraft:redwood_checked", 0.3], ["jugcraft:tall_fir_checked", 0.2],
            ["jugcraft:mossy_maple_checked", 0.1964], ["jugcraft:willow_checked", 0.0889]]},
        "extras": ["ferns", "large_ferns", "berry_bushes"],
        "tags": ["c:is_wet"],
    },
    # Plain oak woodland: oaks big and small, fallen logs, leaf litter, poppies, daisies and berry bushes; villages
    # and woodland mansions.
    "woodland": {
        "display": "Woodland", "base": "forest", "temperature": 0.7, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#7aa84a", "foliage_color": "#68983e"},
        "trees": {"count": [9, 10], "default": "minecraft:oak_leaf_litter", "picks": [
            ["minecraft:fancy_oak_leaf_litter", 0.25], ["jugcraft:oak_bush_checked", 0.1], ["minecraft:fallen_oak_tree", 0.03]]},
        "extras": ["leaf_litter", "berry_bushes", "oxeye_daisies", "poppies"],
        "tags": ["minecraft:has_structure/village_plains", "minecraft:has_structure/woodland_mansion"],
    },
    # ---------------------------------------------------------------- batch 6: mountains, coasts and volcanoes
    # A volcano: blackstone and basalt slopes cracked with magma, pools of lava, basalt spires, ash in the air; no
    # animals. Its windswept, shattered foothills are volcanic too.
    "volcano": {
        "display": "Volcano", "base": "stony_peaks", "temperature": 2.0, "downfall": 0.0, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#6d5f45", "foliage_color": "#625840", "water_color": "#5a6a70"},
        "attributes": {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.02}],
                                                               "modifier": "append"},
                       "minecraft:visual/fog_color": "#6e625a", "minecraft:visual/sky_color": "#8c8a96"},
        "surface": {"floor": "minecraft:blackstone", "under": "minecraft:basalt",
                    "patches": [[-0.06, 0.06, "minecraft:magma_block"], [0.3, 0.6, "minecraft:basalt"], [-0.6, -0.35, "minecraft:tuff"]]},
        "trees": None,
        "drop": ["minecraft:freeze_top_layer"],
        "extras": ["lava_pools", "basalt_spires"],
        "creatures": [],
        "tags": ["c:is_hot", "c:is_dry", "c:is_mountain"],
    },
    # A canyon: cliffs banded in terracotta (vanilla's badlands bands) with grassy ledges, pines and lava pools.
    "canyon": {
        "display": "Canyon", "base": "wooded_badlands", "temperature": 1.4, "downfall": 0.3, "seasons": False,
        "effects": {"grass_color": "#8f9a50", "foliage_color": "#7f8c48"},
        "surface": {"floor": {"type": "minecraft:bandlands"}, "under": {"type": "minecraft:bandlands"},
                    "patches": [[-0.25, 0.1, "minecraft:grass_block"], [0.35, 0.5, "minecraft:coarse_dirt"]]},
        "trees": {"count": [2, 3], "default": "minecraft:pine_checked", "picks": [["jugcraft:spruce_bush_checked", 0.3]]},
        "extras": ["canyon_lava_pools"],
        "tags": ["c:is_hot", "c:is_dry"],
    },
    # A high, treeless grassland of rolling hills: tall grass, coarse dirt, gravel and stone, mossy boulders; sheep
    # and cattle.
    "highland": {
        "display": "Highland", "base": "windswept_hills", "temperature": 0.4, "downfall": 0.6, "seasons": True,
        "effects": {"grass_color": "#80a65a", "foliage_color": "#6f9a4e"},
        "surface": {"floor": "minecraft:grass_block",
                    "patches": [[0.35, 0.45, "minecraft:coarse_dirt"], [-0.45, -0.38, "minecraft:gravel"], [0.6, 0.75, "minecraft:stone"]]},
        "trees": None,
        "extras": ["tundra_rocks", "tall_grass_dense"],
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:cow", 8, 4, 4], ["minecraft:rabbit", 4, 2, 3]],
        "tags": [],
    },
    # A barren basin of gravel and bare stone among the hills, with dead bushes; no animals.
    "basin": {
        "display": "Basin", "base": "windswept_gravelly_hills", "temperature": 0.5, "downfall": 0.3, "seasons": False,
        "effects": {"grass_color": "#8a9070", "foliage_color": "#7a8060"},
        "surface": {"floor": "rule:minecraft:overworld/gravel_or_stone_if_ceiling", "under": "minecraft:gravel",
                    "patches": [[-0.2, 0.0, "minecraft:stone"], [0.5, 0.6, "minecraft:andesite"]]},
        "trees": None,
        "extras": ["patch_dead_bush"],
        "creatures": [],
        "tags": ["c:is_dry"],
    },
    # A shield of old rock: humps of bare stone and andesite with seams of coal at the surface, among firs, pines,
    # spruces, larches, aspens and lakes.
    "shield": {
        "display": "Shield", "base": "windswept_forest", "temperature": 0.25, "downfall": 0.7, "seasons": True,
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.12, 0.12, "minecraft:stone"], [0.45, 0.55, "minecraft:andesite"]]},
        "trees": {"count": [6, 7], "default": "jugcraft:fir_checked", "picks": [
            ["minecraft:pine_checked", 0.25], ["minecraft:spruce_checked", 0.1867], ["jugcraft:aspen_checked", 0.1311],
            ["jugcraft:larch_checked", 0.1509]]},
        "extras": ["ponds", "coal_outcrops", "tundra_rocks"],
        "tags": ["c:is_coniferous_tree"],
    },
    # Green karst pinnacles: steep peaks clothed in grass, pines and spruce scrub, with pale limestone (calcite)
    # showing through, under a grey-green sky; pandas.
    "karst_pinnacles": {
        "display": "Karst Pinnacles", "base": "jagged_peaks", "temperature": 0.7, "downfall": 0.8, "seasons": False,
        "effects": {"grass_color": "#5f9e62", "foliage_color": "#4f8f55"},
        "attributes": {"minecraft:visual/sky_color": "#a3c4b5", "minecraft:visual/fog_color": "#b9d0c4"},
        "surface": {"floor": "minecraft:grass_block", "under": "minecraft:dirt",
                    "patches": [[-0.1, 0.1, "minecraft:calcite"], [0.4, 0.55, "minecraft:stone"]]},
        "trees": {"count": [6, 8], "default": "minecraft:pine_checked", "picks": [["jugcraft:spruce_bush_checked", 0.35]]},
        "creatures": [["minecraft:panda", 4, 1, 2]],
        "tags": [],
    },
    # Hot springs among pines: warm, turquoise pools banked with calcite, blackstone showing through; trail ruins.
    "hot_springs": {
        "display": "Hot Springs", "base": "grove", "temperature": 0.4, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#6f9e5a", "foliage_color": "#5f8f4c", "water_color": "#3fbfc8"},
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.06, 0.06, "minecraft:calcite"], [0.5, 0.58, "minecraft:blackstone"]]},
        "trees": {"count": [4, 5], "default": "minecraft:pine_checked", "picks": [["minecraft:spruce_checked", 0.3]]},
        "extras": ["hot_pools", "ferns"],
        "tags": ["minecraft:has_structure/trail_ruins"],
    },
    # A frozen sea under a sheet of ice and floes of packed ice; polar bears.
    "ice_sheet": {
        "display": "Ice Sheet", "base": "frozen_ocean", "temperature": -0.7, "downfall": 0.4, "seasons": False,
        "effects": {"water_color": "#2f3fa8"},
        "trees": None,
        "extras": ["ice_floes"],
        "creatures": [["minecraft:polar_bear", 1, 1, 2]],
        "tags": [],
    },
    # The deepest seas: a dark floor of deepslate, gravel and obsidian under dim, inky water.
    "ocean_trench": {
        "display": "Ocean Trench", "base": "deep_ocean", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "effects": {"water_color": "#1e3a80"},
        "attributes": {"minecraft:visual/water_fog_color": "#030d26",
                       "minecraft:visual/water_fog_end_distance": {"argument": 0.6, "modifier": "multiply"}},
        "surface": {"floor": "minecraft:deepslate", "under": "minecraft:deepslate",
                    "patches": [[-0.05, 0.05, "minecraft:obsidian"], [0.3, 0.5, "minecraft:gravel"]]},
        "trees": None,
        "tags": [],
    },
    # A cool shore of gravel, with no grass and no animals; shipwrecks wash up as on any beach.
    "gravel_beach": {
        "display": "Gravel Beach", "base": "beach", "temperature": 0.3, "downfall": 0.6, "seasons": False,
        "surface": {"floor": "rule:minecraft:overworld/gravel_or_stone_if_ceiling", "under": "minecraft:gravel"},
        "trees": None,
        "creatures": [],
        "tags": [],
    },
    # A sandy shore of dunes grown with dune grass and sea oats; no trees.
    "dune_beach": {
        "display": "Dune Beach", "base": "beach", "temperature": 0.8, "downfall": 0.4, "seasons": False,
        "surface": {"floor": "rule:minecraft:overworld/sand_or_sandstone_if_ceiling", "under": "minecraft:sand"},
        "trees": None,
        "extras": ["dry_grass", "sea_oats"],
        "tags": [],
    },
    # A warm shore overgrown with grass, oak scrub, tall grass and sea oats, sand showing between.
    "overgrown_beach": {
        "display": "Overgrown Beach", "base": "beach", "temperature": 0.9, "downfall": 0.6, "seasons": False,
        "effects": {"grass_color": "#7cc04a", "foliage_color": "#68b03c"},
        "surface": {"floor": "rule:minecraft:overworld/sand_or_sandstone_if_ceiling", "under": "minecraft:sand",
                    "patches": [[-0.3, 0.3, "minecraft:grass_block"]]},
        "trees": {"count": [1, 2], "default": "jugcraft:oak_bush_checked", "picks": []},
        "extras": ["tall_grass_dense", "sea_oats"],
        "tags": [],
    },
    # A peaceful island of flowers: rolling hills of grass, poppies, sunflowers and hydrangeas, oak scrub; no
    # monsters spawn, as on vanilla's mushroom islands.
    "flower_isle": {
        "display": "Flower Isle", "base": "mushroom_fields", "temperature": 0.75, "downfall": 0.8, "seasons": True,
        "winter_snow": False,
        "effects": {"grass_color": "#6cd15a", "foliage_color": "#5cc04c"},
        "trees": {"count": [1, 2], "default": "jugcraft:oak_bush_checked", "picks": []},
        "drop": ["minecraft:mushroom_island_vegetation", "minecraft:brown_mushroom_taiga", "minecraft:red_mushroom_taiga"],
        "extras": ["field_sunflowers", "meadow_flowers", "hydrangeas", "poppies", "tall_grass_dense"],
        "creatures": [["minecraft:chicken", 10, 2, 4], ["minecraft:rabbit", 6, 2, 3], ["minecraft:sheep", 8, 2, 4]],
        "tags": ["c:is_floral"],
    },
    # ---------------------------------------------------------------- batch 7: wonders and caves
    # A burnt-out waste of ash-grey tuff and gravel, smouldering with magma, its water blood-red; grey snags on its
    # coarse dirt (where an oak sapling could stand), lava pools, ash in the air; no animals.
    "cinder_barrens": {
        "display": "Cinder Barrens", "base": "badlands", "temperature": 2.0, "downfall": 0.0, "seasons": False, "precipitation": False,
        "effects": {"grass_color": "#6a5e4e", "foliage_color": "#5e5446", "water_color": "#8a1a1a"},
        "attributes": {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.015}],
                                                               "modifier": "append"},
                       "minecraft:visual/fog_color": "#7a6e66", "minecraft:visual/water_fog_color": "#4a0a0a"},
        "surface": {"floor": "minecraft:tuff", "under": "minecraft:tuff",
                    "patches": [[-0.05, 0.05, "minecraft:magma_block"], [0.25, 0.45, "minecraft:gravel"], [-0.45, -0.3, "minecraft:coarse_dirt"]]},
        "trees": {"count": [1, 2], "default": "jugcraft:dead_snag_checked", "picks": [
            ["jugcraft:dead_snag_bent_checked", 0.25]]},
        "extras": ["lava_pools", "patch_dead_bush"],
        "creatures": [],
        "tags": ["c:is_hot", "c:is_dry", "c:is_dead"],
    },
    # A vale of the old world: bright, simple green grass, plain oaks, poppies and dandelions, under a clear blue sky;
    # an island where no monsters spawn.
    "elder_vale": {
        "display": "Elder Vale", "base": "mushroom_fields", "temperature": 0.7, "downfall": 0.8, "seasons": False,
        "effects": {"grass_color": "#5ec43a", "foliage_color": "#4cb42e", "water_color": "#2c46d8"},
        "attributes": {"minecraft:visual/sky_color": "#9cbcff"},
        "trees": {"count": [3, 4], "default": "minecraft:oak_checked", "picks": [["minecraft:fancy_oak_checked", 0.1]]},
        "drop": ["minecraft:mushroom_island_vegetation", "minecraft:brown_mushroom_taiga", "minecraft:red_mushroom_taiga"],
        "extras": ["poppies", "dandelions"],
        "creatures": [["minecraft:pig", 10, 4, 4], ["minecraft:sheep", 12, 4, 4], ["minecraft:cow", 8, 4, 4], ["minecraft:chicken", 10, 4, 4]],
        "tags": [],
    },
    # A frozen garden under a cold, shimmering sky: firs and birches, frost irises and glimmerblooms in the snow;
    # no monsters spawn.
    "frostlight_garden": {
        "display": "Frostlight Garden", "base": "snowy_plains", "temperature": -0.3, "downfall": 0.5, "seasons": False,
        "effects": {"grass_color": "#8fc8b0", "foliage_color": "#7ab8a0"},
        "attributes": {"minecraft:visual/sky_color": "#86d6cc", "minecraft:visual/fog_color": "#c4ecf0"},
        "trees": {"count": [3, 4], "default": "jugcraft:fir_checked", "picks": [["minecraft:birch_checked", 0.4]]},
        "extras": ["frost_irises", "glimmerblooms"],
        "creatures": [["minecraft:rabbit", 10, 2, 3], ["minecraft:fox", 6, 2, 4]],
        "monsters": [],
        "tags": ["c:is_snowy", "c:is_cold"],
    },
    # A shrubland gilded gold: golden grass and golden-leaved oak scrub, goldenrod and dry grass, little lakes.
    "gilded_shrubland": {
        "display": "Gilded Shrubland", "base": "savanna_plateau", "temperature": 1.0, "downfall": 0.4, "seasons": False,
        "effects": {"grass_color": "#d4b84a", "foliage_color": "#d8b030"},
        "trees": {"count": [3, 4], "default": "jugcraft:oak_bush_checked", "picks": []},
        "extras": ["ponds", "goldenrod", "dry_grass"],
        "tags": [],
    },
    # A grove of twilight magic: jacarandas, giant red mushrooms, glimmerblooms, alliums, lilacs and hydrangeas under
    # pink air, with pink water; only witches spawn.
    "glimmer_grove": {
        "display": "Glimmer Grove", "base": "flower_forest", "temperature": 0.6, "downfall": 0.8, "seasons": False,
        "effects": {"grass_color": "#8ad6a0", "foliage_color": "#7cc890", "water_color": "#e070c8"},
        "attributes": {"minecraft:visual/sky_color": "#d4a4e0", "minecraft:visual/fog_color": "#e8b8e8",
                       "minecraft:visual/water_fog_color": "#a0408c"},
        "trees": {"count": [6, 7], "default": "jugcraft:jacaranda_checked", "picks": [
            ["jugcraft:huge_red_mushroom_on_soil", 0.1], ["minecraft:oak_checked", 0.2]]},
        "extras": ["glimmerblooms", "alliums", "lilacs", "hydrangeas"],
        "monsters": [["minecraft:witch", 10, 1, 1]],
        "tags": ["c:is_floral"],
    },
    # A gloomy weald of dark oaks, dead trees and giant mushrooms over leaf litter and toadstools, with dark purple
    # pools.
    "gloomweald": {
        "display": "Gloomweald", "base": "dark_forest", "temperature": 0.5, "downfall": 0.8, "seasons": False,
        "effects": {"grass_color": "#4a5a3a", "foliage_color": "#3e4c32", "water_color": "#3a2850"},
        "attributes": {"minecraft:visual/sky_color": "#6a6478", "minecraft:visual/fog_color": "#4a4058",
                       "minecraft:visual/water_fog_color": "#1e1428"},
        "trees": {"count": [2, 3], "default": "jugcraft:dead_tree_checked", "picks": [["minecraft:spruce_checked", 0.3]]},
        "extras": ["ponds", "leaf_litter", "toadstools", "brown_toadstools", "flora_ghost_fern", "flora_large_ghost_fern",
                   "flora_ghost_pipe", "flora_deadly_nightshade", "flora_foxglove", "flora_wild_mandrake", "flora_creeping_ivy"],
        "tags": [],
    },
    # A cave grotto: mud floors grown with glowcaps that light the dark, moss, glow lichen; no dripstone.
    "glowcap_grotto": {
        "display": "Glowcap Grotto", "base": "dripstone_caves", "temperature": 0.8, "downfall": 0.6, "seasons": False,
        "trees": None,
        "drop": ["minecraft:large_dripstone", "minecraft:dripstone_cluster", "minecraft:pointed_dripstone"],
        "extras": ["grotto_mud", "grotto_moss", "grotto_glowcaps"],
        "tags": [],
    },
    # A hallowed bog, pale and bright: willows and young white aspens over pale grass, lilies of the valley and
    # daisies, clear blue water; no monsters spawn.
    "hallowed_bog": {
        "display": "Hallowed Bog", "base": "swamp", "temperature": 0.7, "downfall": 0.9, "seasons": True,
        "effects": {"grass_color": "#a8d890", "foliage_color": "#98c880", "grass_color_modifier": "none", "water_color": "#7fd0e8"},
        "attributes": {"minecraft:visual/sky_color": "#a8c8ff", "minecraft:visual/water_fog_color": "#4aa0c0"},
        "trees": {"count": [2, 3], "default": "jugcraft:willow_checked", "picks": [
            ["jugcraft:young_aspen_checked", 0.3]], "water_depth": 2},
        "extras": ["lilies_of_the_valley", "oxeye_daisies", "lily_pads", "cattails", "flora_snowdrop", "flora_funeral_lily",
                   "flora_asphodel"],
        "monsters": [],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # A sunny meadow on rolling hills: golden-green grass, sunflowers, goldenrod and wildflowers, a few small oaks.
    "highsun_meadow": {
        "display": "Highsun Meadow", "base": "plains", "temperature": 0.95, "downfall": 0.5, "seasons": False,
        "effects": {"grass_color": "#a8c84a", "foliage_color": "#98b840"},
        "attributes": {"minecraft:visual/sky_color": "#8ec8ff", "minecraft:visual/fog_color": "#fff2c8"},
        "trees": {"count": [0, 1], "default": "jugcraft:oak_bush_checked", "picks": []},
        "extras": ["field_sunflowers", "goldenrod", "meadow_wildflowers", "tall_grass_dense"],
        "tags": [],
    },
    # A jungle of giant mushrooms: huge red and brown mushrooms, jungle bushes and oaks over grass and mycelium,
    # toadstools everywhere, spore-green air; mooshrooms.
    "mycelial_jungle": {
        "display": "Mycelial Jungle", "base": "jungle", "temperature": 0.9, "downfall": 0.9, "seasons": False,
        "effects": {"grass_color": "#6aa048", "foliage_color": "#5a9440"},
        "attributes": {"minecraft:visual/fog_color": "#a8c890", "minecraft:visual/sky_color": "#a4c8a0"},
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.15, 0.15, "minecraft:mycelium"]]},
        "trees": {"count": [10, 12], "default": "minecraft:jungle_bush", "picks": [
            ["jugcraft:huge_red_mushroom_on_soil", 0.3], ["jugcraft:huge_brown_mushroom_on_soil", 0.15], ["minecraft:oak_checked", 0.15]]},
        "extras": ["toadstools", "brown_toadstools"],
        "creatures": [["minecraft:mooshroom", 8, 4, 8], ["minecraft:parrot", 20, 1, 2], ["minecraft:chicken", 10, 4, 4]],
        "tags": ["c:is_tropical"],
    },
    # Shrine springs: great oaks two blocks wide among jungle bushes, warm pools banked with calcite, dark green grass.
    "shrine_springs": {
        "display": "Shrine Springs", "base": "jungle", "temperature": 0.95, "downfall": 0.9, "seasons": False,
        "effects": {"grass_color": "#3a8a2a", "foliage_color": "#2f7a22", "water_color": "#3fbfc8"},
        "trees": {"count": [2, 3], "default": "jugcraft:great_oak_checked", "picks": [
            ["minecraft:jungle_bush", 0.3], ["minecraft:oak_checked", 0.2]]},
        "extras": ["hot_pools", "ferns", "large_ferns"],
        "tags": ["c:is_tropical"],
    },
    # A snowy grove of blossoming cherries and aspens, snowpetals and clover, mossy boulders, snowflakes on the air.
    "snowpetal_grove": {
        "display": "Snowpetal Grove", "base": "snowy_plains", "temperature": -0.2, "downfall": 0.5, "seasons": False,
        "effects": {"grass_color": "#9ac8a0", "foliage_color": "#8ab890"},
        "attributes": {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:snowflake"}, "probability": 0.005}],
                                                               "modifier": "append"}},
        "trees": {"count": [3, 4], "default": "minecraft:cherry_checked", "picks": [["jugcraft:aspen_checked", 0.25]]},
        "extras": ["tundra_rocks", "snowpetals", "clover"],
        "creatures": [["minecraft:rabbit", 10, 2, 3], ["minecraft:fox", 6, 2, 4]],
        "tags": ["c:is_snowy", "c:is_cold"],
    },
    # A cave of spiders: cobwebs strung from the ceilings and across the floors; spiders and cave spiders.
    "spider_nest": {
        "display": "Spider Nest", "base": "dripstone_caves", "temperature": 0.8, "downfall": 0.4, "seasons": False,
        "trees": None,
        "drop": ["minecraft:large_dripstone", "minecraft:dripstone_cluster", "minecraft:pointed_dripstone"],
        "extras": ["cave_cobwebs", "floor_cobwebs"],
        "monsters": [["minecraft:spider", 100, 2, 4], ["minecraft:cave_spider", 60, 1, 3], ["minecraft:skeleton", 40, 2, 4],
                     ["minecraft:zombie", 40, 2, 4]],
        "tags": [],
    },
    # A starlit wood of soaring birches and tall firs, glimmerblooms and lilies of the valley below, motes of light
    # drifting under a twilight-blue sky.
    "starlit_wood": {
        "display": "Starlit Wood", "base": "birch_forest", "temperature": 0.6, "downfall": 0.7, "seasons": False,
        "effects": {"grass_color": "#6ab89a", "foliage_color": "#5aa88a"},
        "attributes": {"minecraft:visual/sky_color": "#5a6ab8", "minecraft:visual/fog_color": "#8a92c8",
                       "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:end_rod"}, "probability": 0.002}],
                                                             "modifier": "append"}},
        "trees": {"count": [8, 9], "default": "minecraft:super_birch_bees", "picks": [["jugcraft:tall_fir_checked", 0.25]]},
        "extras": ["glimmerblooms", "lilies_of_the_valley"],
        "tags": [],
    },
    # A field of toadstools: mycelium patched with grass, giant mushrooms, red and brown toadstools and glowcaps;
    # mooshrooms, and no monsters, as on vanilla's mushroom islands.
    "toadstool_field": {
        "display": "Toadstool Field", "base": "mushroom_fields", "temperature": 0.9, "downfall": 1.0, "seasons": False,
        "surface": {"floor": "minecraft:mycelium", "patches": [[-0.2, 0.2, "minecraft:grass_block"]]},
        "trees": None,
        "extras": ["toadstools", "brown_toadstools", "surface_glowcaps"],
        "tags": ["c:is_mushroom"],
    },
    # A webwood: willows, dead trees and vine-hung oaks strung with cobwebs over a swampy floor, toadstools and
    # cattails, grey air; spiders everywhere.
    "webwood": {
        "display": "Webwood", "base": "mangrove_swamp", "temperature": 0.8, "downfall": 0.9, "seasons": False,
        "effects": {"grass_color": "#5a6a48", "foliage_color": "#4e5e40", "water_color": "#5a6a5a"},
        "attributes": {"minecraft:visual/fog_color": "#9aa0a0"},
        "trees": {"count": [6, 7], "default": "jugcraft:willow_checked", "picks": [
            ["jugcraft:dead_tree_checked", 0.2], ["jugcraft:tall_vine_oak_checked", 0.2], ["minecraft:birch_checked", 0.1]]},
        "extras": ["tree_cobwebs", "toadstools", "cattails"],
        "monsters": [["minecraft:spider", 200, 2, 4], ["minecraft:zombie", 60, 2, 4], ["minecraft:skeleton", 60, 2, 4],
                     ["minecraft:creeper", 60, 2, 4], ["minecraft:enderman", 10, 1, 4], ["minecraft:witch", 5, 1, 1]],
        "tags": ["c:is_swamp", "c:is_wet"],
    },
    # Wild greens: a riot of tall grass, clover, ferns and wildflowers over swathes of coarse dirt; no trees; cattle,
    # sheep and chickens.
    "wild_greens": {
        "display": "Wild Greens", "base": "forest", "temperature": 0.8, "downfall": 0.8, "seasons": True,
        "effects": {"grass_color": "#6cbc44", "foliage_color": "#5cac38"},
        "surface": {"floor": "minecraft:grass_block", "patches": [[-0.3, -0.05, "minecraft:coarse_dirt"]]},
        "trees": None,
        "extras": ["tall_grass_dense", "clover", "large_ferns", "field_flowers", "meadow_wildflowers"],
        "creatures": [["minecraft:cow", 10, 4, 4], ["minecraft:sheep", 12, 4, 4], ["minecraft:chicken", 10, 4, 4]],
        "untags": ["minecraft:is_forest"],
        "tags": [],
    },
    # ---------------------------------------------------------------- batch 8: the Nether
    # Placed by Fabric's Nether biome API at [temperature, humidity, offset] in the Nether's climate; the offsets keep
    # them rarer than vanilla's five.
    # Ash-grey wastes of tuff and gravel, smouldering with magma, under falling ash.
    "ashfall_wastes": {
        "display": "Ashfall Wastes", "base": "nether_wastes", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [0.5, -0.5, 0.2],
        "attributes": {"minecraft:visual/fog_color": "#5e5048",
                       "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.04}],
                                                             "modifier": "append"}},
        "ground": {"blocks": {"minecraft:tuff": 6, "minecraft:gravel": 2, "minecraft:magma_block": 1, "minecraft:netherrack": 1}},
        "trees": None,
        "tags": [],
    },
    # Blighted dunes of soul sand and soul soil, overgrown with brambles, soul fire flickering.
    "blighted_sands": {
        "display": "Blighted Sands", "base": "soul_sand_valley", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [-0.3, -0.75, 0.25],
        "attributes": {"minecraft:visual/fog_color": "#4e3e30"},
        "ground": {"blocks": {"minecraft:soul_sand": 5, "minecraft:soul_soil": 3}, "plant": "jugcraft:bramble", "plant_chance": 0.12},
        "trees": None,
        "tags": [],
    },
    # A frozen rift: floors of snow, packed ice and blue ice under a cold blue haze, snow on the air, no glowstone;
    # strays among the piglins.
    "frost_rift": {
        "display": "Frost Rift", "base": "nether_wastes", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [-0.8, 0.45, 0.4],
        "attributes": {"minecraft:visual/fog_color": "#2e4a78",
                       "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:snowflake"}, "probability": 0.01}],
                                                             "modifier": "append"}},
        "ground": {"blocks": {"minecraft:snow_block": 4, "minecraft:packed_ice": 3, "minecraft:blue_ice": 1}},
        "trees": None,
        "drop": ["minecraft:glowstone_extra", "minecraft:glowstone"],
        "monsters": [["minecraft:zombified_piglin", 60, 2, 4], ["minecraft:stray", 40, 2, 4], ["minecraft:ghast", 20, 4, 4]],
        "tags": [],
    },
    # A thicket of fungi: mycelium and crimson nylium under huge red and brown mushrooms, crimson fungi and glowcaps.
    "fungal_thicket": {
        "display": "Fungal Thicket", "base": "crimson_forest", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [0.55, 0.55, 0.3],
        "attributes": {"minecraft:visual/fog_color": "#5a3a4a"},
        "ground": {"blocks": {"minecraft:mycelium": 3, "minecraft:crimson_nylium": 2}},
        "trees": None,
        "extras": ["nether_glowcaps", "nether_huge_red_mushrooms", "nether_huge_brown_mushrooms"],
        "tags": [],
    },
    # Volcanic fields of sulfur and cinnabar, magma and blackstone, sulfur spikes, lava deltas and basalt, under an
    # orange haze.
    "magma_fields": {
        "display": "Magma Fields", "base": "basalt_deltas", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [0.8, -0.2, 0.25],
        "attributes": {"minecraft:visual/fog_color": "#b8704a"},
        "ground": {"blocks": {"minecraft:sulfur": 4, "minecraft:cinnabar": 2, "minecraft:magma_block": 2, "minecraft:blackstone": 1}},
        "trees": None,
        "extras": ["sulfur_spikes"],
        "tags": [],
    },
    # A heap of bone and nether wart, with spires of bone, under a dark red haze.
    "marrow_heap": {
        "display": "Marrow Heap", "base": "nether_wastes", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [0.2, -0.95, 0.3],
        "attributes": {"minecraft:visual/fog_color": "#4a0a12"},
        "ground": {"blocks": {"minecraft:bone_block": 3, "minecraft:nether_wart_block": 3, "minecraft:netherrack": 2}},
        "trees": None,
        "extras": ["nether_bone_spires"],
        "tags": [],
    },
    # A green brush of the Nether: warped and crimson nylium thick with brambles, roots and warped fungi, spores in
    # the green air.
    "netherbrush": {
        "display": "Netherbrush", "base": "warped_forest", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [-0.4, 0.8, 0.25],
        "attributes": {"minecraft:visual/fog_color": "#2e4a22"},
        "ground": {"blocks": {"minecraft:warped_nylium": 3, "minecraft:crimson_nylium": 1, "minecraft:netherrack": 1},
                   "plant": "jugcraft:bramble", "plant_chance": 0.2},
        "trees": None,
        "extras": ["nether_brambles"],
        "tags": [],
    },
    # A rift of pale stone and quartz: calcite floors seamed with quartz ore, quartz spires, white sparks in a scarlet
    # haze.
    "quartz_rift": {
        "display": "Quartz Rift", "base": "nether_wastes", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [-0.9, -0.3, 0.3],
        "attributes": {"minecraft:visual/fog_color": "#7a1a2a",
                       "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:end_rod"}, "probability": 0.003}],
                                                             "modifier": "append"}},
        "ground": {"blocks": {"minecraft:netherrack": 3, "minecraft:calcite": 2, "minecraft:nether_quartz_ore": 1}},
        "trees": None,
        "extras": ["quartz_spires"],
        "tags": [],
    },
    # A withered hollow, near black: blackstone floors with obsidian and a little crying obsidian, no glowstone;
    # endermen and skeletons.
    "withered_hollow": {
        "display": "Withered Hollow", "base": "nether_wastes", "temperature": 2.0, "downfall": 0.0, "seasons": False,
        "dimension": "nether", "nether": [0.9, 0.9, 0.35],
        "attributes": {"minecraft:visual/fog_color": "#0e0a10"},
        "ground": {"blocks": {"minecraft:blackstone": 12, "minecraft:obsidian": 2, "minecraft:crying_obsidian": 1}},
        "trees": None,
        "drop": ["minecraft:glowstone_extra", "minecraft:glowstone"],
        "monsters": [["minecraft:enderman", 40, 1, 4], ["minecraft:skeleton", 60, 1, 3]],
        "tags": [],
    },
    # ---------------------------------------------------------------- batch 9: the End
    # Placed by Fabric's End biome API among the outer End's highlands (or as barrens); each keeps the End's chorus
    # and End cities where its base has them.
    # A reef of the End: sand, sandstone and dead coral blocks among the chorus, dead corals and fans, still pools and
    # sandstone pillars.
    "chorus_reef": {
        "display": "Chorus Reef", "base": "end_highlands", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "dimension": "end", "end": {"zone": "highlands", "share": 0.125},
        "ground": {"blocks": {"minecraft:end_stone": 3, "minecraft:sand": 2, "minecraft:sandstone": 1, "minecraft:dead_brain_coral_block": 1,
                              "minecraft:dead_tube_coral_block": 1, "minecraft:dead_horn_coral_block": 1},
                   "replaceable": "#jugcraft:end_ground_replaceable"},
        "trees": None,
        "extras": ["reef_pools", "dead_coral", "sandstone_pillars"],
        "tags": [],
    },
    # Wild growth in the End: moss over the end stone, violet jacarandas and azaleas, glowcaps and glimmerblooms,
    # glinting motes.
    "ender_wilds": {
        "display": "Ender Wilds", "base": "end_highlands", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "dimension": "end", "end": {"zone": "highlands", "share": 0.125},
        "attributes": {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:glow"}, "probability": 0.003}],
                                                               "modifier": "append"}},
        "ground": {"blocks": {"minecraft:moss_block": 3, "minecraft:end_stone": 2}, "replaceable": "#jugcraft:end_ground_replaceable"},
        "trees": {"count": [2, 3], "default": "jugcraft:jacaranda_checked", "picks": [["jugcraft:azalea_tree_checked", 0.3]]},
        "extras": ["glimmerblooms", "surface_glowcaps"],
        "tags": [],
    },
    # The outer flats: low, wide barrens of end stone, sand and gravel at the islands' edges, with dead bushes.
    "outer_flats": {
        "display": "Outer Flats", "base": "end_barrens", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "dimension": "end", "end": {"zone": "barrens", "highlands": "minecraft:end_highlands", "share": 1 / 3},
        "ground": {"blocks": {"minecraft:end_stone": 4, "minecraft:sand": 2, "minecraft:gravel": 1},
                   "replaceable": "#jugcraft:end_ground_replaceable"},
        "trees": None,
        "extras": ["patch_dead_bush"],
        "tags": [],
    },
    # A phantom garden: pale moss and its carpets, pale oaks and eyeblossoms, in the End's dark.
    "phantom_garden": {
        "display": "Phantom Garden", "base": "end_highlands", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "dimension": "end", "end": {"zone": "highlands", "share": 0.125},
        "ground": {"blocks": {"minecraft:pale_moss_block": 3, "minecraft:end_stone": 1}, "plant": "jugcraft:pale_moss_carpets",
                   "plant_chance": 0.3, "replaceable": "#jugcraft:end_ground_replaceable"},
        "trees": {"count": [2, 3], "default": "minecraft:pale_oak_checked", "picks": []},
        "extras": ["pale_flowers"],
        "tags": [],
    },
    # A rotted expanse: coarse dirt and soul soil seeping through the end stone, dead trees, grey snags and fallen
    # logs on its coarse dirt, obsidian pillars and murky pools; no endermen.
    "rotted_expanse": {
        "display": "Rotted Expanse", "base": "end_highlands", "temperature": 0.5, "downfall": 0.5, "seasons": False,
        "dimension": "end", "end": {"zone": "highlands", "share": 0.125},
        "effects": {"water_color": "#4a4a3a"},
        "ground": {"blocks": {"minecraft:end_stone": 3, "minecraft:coarse_dirt": 2, "minecraft:soul_soil": 1},
                   "replaceable": "#jugcraft:end_ground_replaceable"},
        "trees": {"count": [1, 2], "default": "jugcraft:dead_tree_checked", "picks": [
            ["jugcraft:dead_snag_checked", 0.225], ["jugcraft:dead_snag_bent_checked", 0.0968],
            ["jugcraft:fallen_dead_tree", 0.1429]]},
        "extras": ["ponds", "obsidian_pillars"],
        "monsters": [],
        "tags": [],
    },
    # ---------------------------------------------------------------- batch 2: fields and meadows
    # Cool, flat land with teal grass, scattered small spruces and clumps of oak bush, and many flowers.
    "field": {
        "display": "Field", "base": "forest", "temperature": 0.35, "downfall": 0.6, "seasons": True,
        "effects": {"grass_color": "#7fb38c", "foliage_color": "#6fa880"},
        "trees": {"count": [2, 3], "default": "jugcraft:oak_bush_checked", "picks": [
            ["minecraft:spruce_checked", 0.3], ["minecraft:oak_bees_002", 0.05]]},
        "extras": ["field_sunflowers", "field_flowers"],
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:pig", 10, 4, 4], ["minecraft:chicken", 10, 4, 4],
                      ["minecraft:cow", 8, 4, 4], ["minecraft:rabbit", 4, 2, 3], ["minecraft:fox", 2, 2, 4]],
        "untags": ["minecraft:is_forest"],
        "tags": ["minecraft:has_structure/village_taiga", "minecraft:has_structure/mineshaft", "c:is_plains"],
    },
    # Treeless and thick with vanilla flowers on almost every block.
    "flower_meadow": {
        "display": "Flower Meadow", "base": "flower_forest", "temperature": 0.5, "downfall": 0.7, "seasons": True,
        "effects": {"grass_color": "#83c25f"},
        "trees": None,
        "extras": ["meadow_flowers", "meadow_wildflowers"],
        "untags": ["minecraft:is_forest"],
        "tags": ["minecraft:has_structure/mineshaft", "c:is_plains", "c:is_flower_forest"],
    },
    # Treeless, blue-green grass carpeted with clover.
    "grassland": {
        "display": "Grassland", "base": "plains", "temperature": 0.6, "downfall": 0.6, "seasons": True,
        "effects": {"grass_color": "#6fbd84", "foliage_color": "#62ad73"},
        "trees": None,
        "drop": ["minecraft:patch_bush"],
        "extras": ["clover", "tall_grass_dense"],
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:pig", 10, 4, 4], ["minecraft:chicken", 10, 4, 4],
                      ["minecraft:cow", 8, 4, 4]],
        "tags": ["c:is_plains"],
    },
    # Warm, dry heath: brown-tinged grass, heather and wildflowers, pines and oak bushes; wild horses.
    "heathland": {
        "display": "Heathland", "base": "plains", "temperature": 0.9, "downfall": 0.45, "seasons": True, "winter_snow": False,
        "effects": {"grass_color": "#a6a85c", "foliage_color": "#8f9a4c"},
        "trees": {"count": [2, 3], "default": "jugcraft:oak_bush_checked", "picks": [["minecraft:pine_checked", 0.3]]},
        "drop": ["minecraft:flower_plains"],
        "extras": ["heather", "meadow_wildflowers", "dry_grass"],
        "creatures": [["minecraft:horse", 8, 2, 6], ["minecraft:donkey", 2, 1, 3], ["minecraft:sheep", 10, 4, 4],
                      ["minecraft:rabbit", 4, 2, 3], ["minecraft:chicken", 6, 4, 4]],
        "tags": ["c:is_plains"],
    },
    # Rolling fields smothered in lavender, with blossoming jacarandas and oaks; bees.
    "lavender_field": {
        "display": "Lavender Field", "base": "sunflower_plains", "temperature": 0.7, "downfall": 0.6, "seasons": True,
        "effects": {"grass_color": "#8cbf64"},
        "trees": {"count": [1, 2], "default": "jugcraft:jacaranda_checked", "picks": [["minecraft:oak_bees_002", 0.4]]},
        "drop": ["minecraft:patch_sunflower", "minecraft:flower_plains", "minecraft:patch_bush"],
        "extras": ["lavender", "tall_lavender"],
        "tags": ["c:is_plains", "c:is_flower_forest"],
    },
    # Warm, wet grassland: small jungle trees and oak bushes among orange cosmos and oxeye daisies; villages.
    "lush_grassland": {
        "display": "Lush Grassland", "base": "sparse_jungle", "temperature": 0.85, "downfall": 0.85, "seasons": True,
        "winter_snow": False,
        "effects": {"grass_color": "#5fbd4a", "foliage_color": "#4fb03a"},
        "trees": {"count": [2, 3], "default": "minecraft:jungle_bush", "picks": [
            ["jugcraft:oak_bush_checked", 0.3], ["minecraft:jungle_tree", 0.1]]},
        "drop": ["minecraft:patch_melon_sparse"],
        "extras": ["orange_cosmos", "oxeye_daisies", "tall_grass_dense"],
        "creatures": [["minecraft:sheep", 12, 4, 4], ["minecraft:pig", 10, 4, 4], ["minecraft:chicken", 10, 4, 4],
                      ["minecraft:cow", 8, 4, 4], ["minecraft:parrot", 4, 1, 2]],
        "untags": ["minecraft:is_jungle"],
        "tags": ["minecraft:has_structure/village_plains", "c:is_plains"],
    },
    # Flat, golden prairie: tall grass and goldenrod, brushy oaks; villages and water holes.
    "prairie": {
        "display": "Prairie", "base": "plains", "temperature": 0.8, "downfall": 0.4, "seasons": True,
        "effects": {"grass_color": "#a3bd5c", "foliage_color": "#8fae4c"},
        "trees": {"count": [1, 2], "default": "jugcraft:oak_bush_checked", "picks": [["minecraft:fancy_oak_checked", 0.15]]},
        "extras": ["goldenrod", "tall_grass_dense"],
        "tags": ["c:is_plains"],
    },
    # Low hills of grass densely set with oak bushes and bushes, and alliums; no trees; wild horses.
    "shrubland": {
        "display": "Shrubland", "base": "plains", "temperature": 0.7, "downfall": 0.3, "seasons": True,
        "effects": {"grass_color": "#93ab5a", "foliage_color": "#7f9a49"},
        "trees": {"count": [4, 6], "default": "jugcraft:oak_bush_checked", "picks": []},
        "extras": ["bushes_dense", "alliums"],
        "creatures": [["minecraft:horse", 8, 2, 6], ["minecraft:donkey", 1, 1, 3], ["minecraft:sheep", 12, 4, 4],
                      ["minecraft:pig", 8, 4, 4], ["minecraft:chicken", 10, 4, 4], ["minecraft:cow", 8, 4, 4]],
        "tags": ["c:is_plains"],
    },
    # Cool, dry, tan grassland with almost nothing growing: dry grass and dead bushes; horses, donkeys and llamas.
    "steppe": {
        "display": "Steppe", "base": "plains", "temperature": 0.45, "downfall": 0.2, "seasons": True,
        "effects": {"grass_color": "#b8ab6c", "foliage_color": "#a89a5c"},
        "trees": None,
        "drop": ["minecraft:patch_bush", "minecraft:flower_plains", "minecraft:patch_tall_grass_2"],
        "swap": {"minecraft:patch_grass_plain": "minecraft:patch_grass_savanna"},
        "extras": ["dry_grass", "patch_dead_bush"],
        "creatures": [["minecraft:horse", 6, 2, 6], ["minecraft:donkey", 2, 1, 3], ["minecraft:llama", 4, 2, 4],
                      ["minecraft:rabbit", 4, 2, 3]],
        "tags": ["c:is_plains", "c:is_dry"],
    },
}

def biome_tags(name):
    info = BIOMES[name]
    tags = [t for t in BASE_TAGS[info["base"]] if t not in info.get("untags", [])]
    return tags + [t for t in info.get("tags", []) if t not in tags]
