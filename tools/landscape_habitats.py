"""Where the menagerie's creatures live in the landscape collection (docs/features/landscape-wildlife.md).

The menagerie (menagerie/Menagerie.habitat) places each species by its habitat: the Overworld, Nether or End through
vanilla's dimension tags (#minecraft:is_overworld and the like), then forest, ocean, jungle and the rest through vanilla's
kind tags and a few biome names. The landscape collection (tools/landscape_kinds.py) carries none of those tags, so no
species came to any of its 159 biomes. Each is given here the habitats it holds: from its kind, and biome by biome where
a kind is broad (arid land is desert, savanna or badlands; magic land is woods, fields or jungle by its ground).

The tags #jugcraft:landscape_habitat/<habitat> carry them, and Menagerie.habitat reads them for the landscape biomes alone,
so nothing else that reads vanilla's or the common tags (trees, crops, structures) changes there. The habitats that
follow the climate or the weather need no table: Menagerie applies its own rule to the landscape biomes as to any other
(snowy below 0.15, a storm or a cave on any land).
"""

import landscape_kinds as lk

MOD = lk.MOD
TAG = f"{MOD}:landscape_habitat"

# The habitats Menagerie.habitat reads from these tags, and the ones it decides by its own rule (or never by biome).
TAGGED = ("forest", "plains", "meadow", "mountain", "swamp", "jungle", "beach", "ocean", "deep_ocean", "river", "desert",
          "savanna", "badlands", "nether", "end")
BY_RULE = ("storm", "cave", "snowy", "summoned", "mushroom")

# What each kind holds, as vanilla's biomes of that kind do (forest and taiga are both "forest" to the menagerie). Arid
# and magic land differ biome by biome (BIOMES); volcanic land and the coastal cliffs hold only the climate's creatures.
KIND_HABITATS = {
    "forest": ["forest"], "taiga": ["forest"], "dark_woods": ["forest"], "grassland": ["plains"], "hills": ["mountain"],
    "swamp": ["swamp"], "tropical": ["jungle"], "beach": ["beach"], "ocean": ["ocean"], "river": ["river"],
    "skylands": ["plains"], "volcanic": [], "coast": [], "cave": [], "nether": ["nether"], "end": ["end"],
    "arid": None, "magic": None,
}

# Biome by biome: the arid and magic land, and where a biome holds more than its kind does.
BIOMES = {
    # Arid land, by its ground and plants: open sand (desert); dry grass and scattered trees (savanna); red and
    # painted clay (badlands).
    **{f"landscape_{name}": ["desert"] for name in (
        "ancient_sands", "cold_desert", "desert_canyon", "desert_oasis", "desert_spires", "gravel_desert", "lush_desert",
        "sandstone_valley", "wasteland", "wasteland_steppe")},
    **{f"landscape_{name}": ["savanna"] for name in (
        "arid_highlands", "ashen_savanna", "brushland", "dryland", "fractured_savanna", "lush_savanna", "savanna_slopes",
        "scrubland")},
    **{f"landscape_{name}": ["badlands"] for name in (
        "bryce_canyon", "painted_mountains", "red_oasis", "snowy_badlands", "warped_mesa", "white_mesa")},
    "landscape_savanna_badlands": ["savanna", "badlands"],
    # Magic land, by its ground: groves and isles of trees, open valleys, flowered gardens, amethyst jungle.
    **{f"landscape_{name}": ["forest"] for name in ("mirage_isles", "moonlight_grove", "mystic_grove")},
    **{f"landscape_{name}": ["plains"] for name in (
        "alpha_islands", "alpha_islands_winter", "origin_valley", "wintry_origin_valley")},
    "landscape_moonlight_valley": ["plains", "meadow"],
    "landscape_auroral_garden": ["meadow"],
    **{f"landscape_{name}": ["jungle"] for name in ("amethyst_canyon", "amethyst_rainforest")},
    # Flowered fields and a flowered wood, as vanilla's meadow and flower forest: meadow besides.
    **{f"landscape_{name}": ["plains", "meadow"] for name in (
        "blooming_plateau", "blooming_valley", "lavender_field", "lavender_valley", "overgrown_greens", "sakura_valley")},
    "landscape_lavender_forest": ["forest", "meadow"],
    # Wooded highlands: mountain and forest.
    "landscape_forested_highlands": ["mountain", "forest"],
    # The deep ocean is ocean too, as vanilla's #is_deep_ocean biomes are #is_ocean.
    "landscape_deep_warm_ocean": ["ocean", "deep_ocean"],
}

# Every landscape biome's habitats.
HABITATS = {biome: BIOMES.get(biome, KIND_HABITATS[kind]) for biome, kind in lk.KINDS.items()}
# The landscape biomes of the Overworld (the caves among them, as vanilla's caves are #is_overworld).
OVERWORLD = [biome for biome, kind in lk.KINDS.items() if kind not in ("nether", "end")]


def habitat_tags():
    """Each tag (#jugcraft:landscape_habitat/overworld and one for each habitat) and its biomes, in tools/' order."""
    out = {f"{TAG}/overworld": list(OVERWORLD)}
    for habitat in TAGGED:
        out[f"{TAG}/{habitat}"] = [biome for biome, habitats in HABITATS.items() if habitat in (habitats or [])]
    return out


def tags(out):
    for tag, biomes in habitat_tags().items():
        for biome in biomes:
            out.add("worldgen/biome", tag, f"{MOD}:{biome}")
