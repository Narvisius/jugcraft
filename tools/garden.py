"""Garden crops in the owner's own art: slice 7a of the kitchen and cooking expansion (docs/branches/AGRICULTURE.md; the
record: docs/features/garden-crops.md). On 9 October 2026 the owner chose ("go with the recommendations") to have
Jugcraft's cabbage, onion, tomato and corn grow through their growth stages, their wild plants added (carrots, potatoes,
beetroots) and drawn for the wild cabbage, onion, tomato and corn, the Garden Salad drawn as their mixed salad, a rotten
tomato for vines left too long, and their mushroom colonies made a crop.

Every texture here is a byte-for-byte copy of the owner's library file (tools/owner_art.py imports TEXTURES). The one
adaptation is ornamental corn's ripe stage: their ripe corn with its ears' three golden tones swapped for flint corn's
(ORNAMENTAL_EARS, flint_ears below). tools/agriculture.py's tables use these names; tools/check_mod_data.py
compares this module with the Java (TallCrop, TomatoVineBlock, MushroomColonyBlock, RottenTomato, RichSoilBlock,
JugcraftAgriculture) and the data, and tools/garden_data.py writes the colonies' and the rotten tomato's JSON.

How the owner's stages fit Jugcraft's plants:
- Cabbage: eight stages, one for each of the crop's eight ages (Jugcraft's own drawing had four).
- Onion: four stages, as before (ages 0-1, 2-3, 4-6, 7).
- Tomato: the budding vine while it is one block tall, then the fruiting vine (flowers, green, turning, red) in the trellis
  cage, the vine drawn for a rope as its upper block. The rope versions draw no rope, so the trellis stays the support.
- Corn: the owner's corn is two blocks tall, Jugcraft's three from age 5, so cornfields and mazes keep their height: the
  stalk stands below (their stage 4, before the ears), the stage's own stalk with its ears in the middle and its top above.
  Their tops for the first three stages go unused: Jugcraft's corn is one block tall until age 3.
"""
FEATURE = "agriculture"

# The cabbage's texture for each age (tools/agriculture.py CROPS stages): the owner drew one for each.
CABBAGE_STAGES = [0, 1, 2, 3, 4, 5, 6, 7]

# Corn and ornamental corn (tools/agriculture.py TALL_CROPS): each age's textures, bottom block first.
CORN_TEXTURES = [
    ["corn_crop_stage0"], ["corn_crop_stage1"], ["corn_crop_stage2"],
    ["corn_crop_stage3", "corn_top_stage3"],
    ["corn_crop_stage4", "corn_top_stage4"],
    ["corn_crop_stage4", "corn_crop_stage5", "corn_top_stage5"],
    ["corn_crop_stage4", "corn_crop_stage6", "corn_top_stage6"],
    ["corn_crop_stage4", "corn_crop_stage7", "corn_top_stage7"],
]
ORNAMENTAL_TEXTURES = CORN_TEXTURES[:7] + [["corn_crop_stage4", "ornamental_corn_crop_stage7", "ornamental_corn_top_stage7"]]
# Ornamental corn's ripe ears: runtime texture -> the owner's ripe corn it is recoloured from. The ears' three golden tones
# (the only pixels of those colours) become flint corn's kernels (tools/halloween_textures.py FLINT): red, with every
# third one gold, as Jugcraft's ornamental corn always had, and some purple.
ORNAMENTAL_EARS = {"block/ornamental_corn_crop_stage7": "corn_crop_stage7", "block/ornamental_corn_top_stage7": "corn_top_stage7"}
EAR_TONES = [(255, 193, 14), (236, 151, 24), (189, 115, 25)]
# Flint corn's kernels (tools/halloween_textures.py FLINT): red, purple and gold.
FLINT_KERNELS = [(0x8a, 0x1e, 0x1e), (0x5a, 0x2a, 0x6a), (0xe0, 0xa8, 0x2a)]
# How much darker each of the ears' tones is than the brightest, kept on the flint kernels so the ears keep their shading.
EAR_SHADE = [1.0, 0.85, 0.65]
# The corn maze's corn (tools/maze_data.py): ripe corn, three blocks tall, which never grows.
MAZE_SECTIONS = {0: "corn_crop_stage4", 1: "corn_crop_stage7", 2: "corn_top_stage7"}

# The tomato on its trellis (tools/agriculture.py TALL_CROPS): each age's textures, bottom block first.
TOMATO_TEXTURES = [
    ["budding_tomatoes_stage0"], ["budding_tomatoes_stage1"], ["budding_tomatoes_stage2"],
    ["budding_tomatoes_stage3", "budding_tomatoes_stage1"],
    ["tomatoes_stage0", "tomatoes_on_rope_stage0"],
    ["tomatoes_stage1", "tomatoes_on_rope_stage1"],
    ["tomatoes_stage2", "tomatoes_on_rope_stage2"],
    ["tomatoes_stage3", "tomatoes_on_rope_stage3"],
]
# A ripe vine left unpicked goes over: on each of its random ticks (its bottom block's) it turns with a 1 in `chance`, about
# eleven minutes on average (a block's random tick comes every 68 seconds or so). It shows the owner's withered vine, and
# picking or breaking it then gives rotten tomatoes, as many as a ripe vine gives tomatoes, and it grows back as usual.
OVERRIPE = {"property": "overripe", "chance": 10, "item": "rotten_tomato",
            "textures": ["tomatoes_old_stage3", "tomatoes_old_stage3"]}

# The rotten tomato: thrown like a snowball (speed in blocks a tick, as SnowballItem's), it bursts where it lands, bumping
# whoever it hits without hurting them; or it goes on the compost.
ROTTEN_TOMATO = {"item": "rotten_tomato", "speed": 1.5, "stack": 16}
ITEMS = {"rotten_tomato": {"display": "Rotten Tomato", "thrown": True, "compost": "medium_high", "tags": []}}

# Wild plants (tools/agriculture.py WILD_CROPS, joined after the fruit crops'): found in patches on grass in these biomes
# (ConventionalBiomeTags); broken, they give 1-2 of `seed`, vanilla's here; shears take the plant.
WILD = {
    "wild_carrots": {"display": "Wild Carrots", "seed": "minecraft:carrot", "texture": "wild_carrots",
                     "biomes": ["IS_PLAINS", "IS_FLORAL"]},
    "wild_potatoes": {"display": "Wild Potatoes", "seed": "minecraft:potato", "texture": "wild_potatoes",
                      "biomes": ["IS_TAIGA", "IS_HILL"]},
    "wild_beetroots": {"display": "Wild Beetroots", "seed": "minecraft:beetroot_seeds", "texture": "wild_beetroots",
                       "biomes": ["IS_PLAINS", "IS_SWAMP"]},
}
# The existing wild plants the owner drew: wild plant -> its texture (the owner's file).
WILD_ART = {"wild_corn": "wild_corn", "wild_tomato": "wild_tomatoes", "wild_onion": "wild_onions", "wild_cabbage": "wild_cabbages"}

# Items drawn by the owner: item -> the owner's file. The Garden Salad keeps its ID and recipe; it is drawn as their mixed salad.
ITEM_ART = {"cabbage": "cabbage", "cabbage_seeds": "cabbage_seeds", "onion": "onion", "tomato": "tomato",
            "tomato_seeds": "tomato_seeds", "corn": "corn", "corn_kernels": "corn_seeds", "rotten_tomato": "rotten_tomato",
            "garden_salad": "mixed_salad"}

# Mushroom colonies: a brown or red mushroom used on Rich Soil (tools/soil.py) plants one. It grows through the owner's four
# stages (a 1 in `growth` chance on each random tick, the soil's boost included) only where the light from the sky and
# blocks is at most `max_light`, as vanilla mushrooms spread, and stands on Rich Soil or a block mushrooms grow on in any
# light (#minecraft:mushroom_grow_block: mycelium, podzol, nylium). Grown, shears or a knife pick `pick` mushrooms and the
# colony goes back to stage `pick_reset`. Broken, it gives back its mushroom, and grown, as many more as picking.
COLONY = {"stages": 4, "max_light": 12, "growth": 5, "pick": {"min": 2, "max": 3}, "pick_reset": 1}
COLONIES = {
    "brown_mushroom_colony": {"display": "Brown Mushroom Colony", "mushroom": "minecraft:brown_mushroom"},
    "red_mushroom_colony": {"display": "Red Mushroom Colony", "mushroom": "minecraft:red_mushroom"},
}

TEXTURES = {f"block/cabbage_stage{i}": f"cabbages_stage{i}" for i in CABBAGE_STAGES}
TEXTURES.update({f"block/onion_stage{i}": f"onions_stage{i}" for i in range(4)})
for _textures in TOMATO_TEXTURES + [OVERRIPE["textures"]] + CORN_TEXTURES:
    TEXTURES.update({f"block/{name}": name for name in _textures})
TEXTURES.update({f"block/{name}": name for name in list(WILD_ART.values()) + [info["texture"] for info in WILD.values()]})
TEXTURES.update({f"item/{item}": source for item, source in ITEM_ART.items()})
for _colony in COLONIES:
    TEXTURES.update({f"block/{_colony}_stage{i}": f"{_colony}_stage{i}" for i in range(COLONY["stages"])})

TEXT = {
    "entity.jugcraft.rotten_tomato": "Rotten Tomato",
}


def blocks():
    return list(COLONIES)


def items():
    return list(ITEMS)


def itemless():
    """The colonies: a mushroom plants one, and picks it (as a crop's seed does)."""
    return list(COLONIES)


def colony_texture(colony, stage):
    return f"{colony}_stage{stage}"


def flint_ears(image):
    """The owner's ripe corn with its ears in flint corn's colours: every pixel of the ears' three golden tones becomes a red
    kernel, every third one (by position) gold and every fifth purple, shaded as the tone it replaces."""
    out = image.convert("RGBA").copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if not a or (r, g, b) not in EAR_TONES:
                continue
            base = FLINT_KERNELS[2] if (x + y) % 3 == 0 else FLINT_KERNELS[1] if (x + 2 * y) % 5 == 0 else FLINT_KERNELS[0]
            shade = EAR_SHADE[EAR_TONES.index((r, g, b))]
            out.putpixel((x, y), tuple(round(c * shade) for c in base) + (a,))
    return out
