"""What the town's decor sites show in each theme (tools/town.py places the sites; the mod's townsfolk change them,
town/TownDecor.java). A theme is the season or a running event: December, Halloween and the Harvest Feast win over
the season, in that order (TownDecor.theme()).

For every kind of site but the centrepiece, a theme gives one list of choices per slot; the mod picks
choices[site index % len(choices)], so neighbouring sites differ. "{facing}" and "{rotation}" in a state are the site's
own. The centrepiece (7 x 7 columns by 13 high on the square) is a whole scene per theme: slot = dx + 7 dz + 49 dy.

Everything is vanilla blocks that stand on their own (pots, leaves kept persistent, full blocks, wall banners on a
wall), so no decoration pops off when something next to it changes.
"""
from town_voxels import S

THEMES = ["spring", "summer", "autumn", "winter", "halloween", "harvest", "december"]


def pots(*names):
    return [S(f"potted_{n}") for n in names]


def banners(*colours):
    return [f"minecraft:{c}_wall_banner[facing={{facing}}]" for c in colours]


def flags(*colours):
    return [f"minecraft:{c}_banner[rotation={{rotation}}]" for c in colours]


def wool(*colours):
    return [S(f"{c}_wool") for c in colours]


def leaves(name):
    return S(name, distance="7", persistent="true", waterlogged="false")


KINDS = {
    "window_box": {
        "spring": [pots("red_tulip", "pink_tulip", "allium", "white_tulip", "orange_tulip", "lily_of_the_valley"),
                   pots("cherry_sapling", "flowering_azalea_bush", "pink_tulip", "azalea_bush")],
        "summer": [pots("poppy", "dandelion", "cornflower", "blue_orchid", "oxeye_daisy", "torchflower"),
                   pots("azure_bluet", "poppy", "cornflower", "dandelion")],
        "autumn": [pots("fern", "red_mushroom", "brown_mushroom", "dark_oak_sapling", "acacia_sapling"),
                   pots("dead_bush", "fern", "brown_mushroom")],
        "winter": [pots("spruce_sapling", "azure_bluet", "white_tulip", "dead_bush"),
                   pots("spruce_sapling", "lily_of_the_valley")],
        "halloween": [pots("wither_rose", "dead_bush", "crimson_fungus", "closed_eyeblossom", "crimson_roots"),
                      pots("warped_fungus", "open_eyeblossom", "dead_bush")],
        "harvest": [pots("fern", "brown_mushroom", "red_mushroom", "bamboo", "jungle_sapling"),
                    pots("dead_bush", "fern", "acacia_sapling")],
        "december": [pots("spruce_sapling", "red_mushroom", "azure_bluet", "spruce_sapling", "lily_of_the_valley"),
                     pots("spruce_sapling", "red_tulip")],
    },
    "banner": {
        "spring": [banners("pink", "lime", "light_blue", "magenta")],
        "summer": [banners("yellow", "light_blue", "orange", "cyan")],
        "autumn": [banners("orange", "brown", "red", "yellow")],
        "winter": [banners("white", "light_blue", "light_gray", "cyan")],
        "halloween": [banners("orange", "black", "purple")],
        "harvest": [banners("yellow", "brown", "orange", "red")],
        "december": [banners("red", "green", "white")],
    },
    "gate_banner": {
        "spring": [banners("lime"), banners("pink")],
        "summer": [banners("yellow"), banners("light_blue")],
        "autumn": [banners("orange"), banners("brown")],
        "winter": [banners("white"), banners("light_blue")],
        "halloween": [banners("orange"), banners("black")],
        "harvest": [banners("yellow"), banners("brown")],
        "december": [banners("red"), banners("green")],
    },
    "tower_flag": {
        "spring": [flags("lime", "pink")], "summer": [flags("yellow", "light_blue")], "autumn": [flags("orange", "red")],
        "winter": [flags("white", "light_blue")], "halloween": [flags("orange", "purple")],
        "harvest": [flags("yellow", "brown")], "december": [flags("red", "green")],
    },
    "awning": {
        "spring": [wool("pink"), wool("white")], "summer": [wool("yellow"), wool("white")],
        "autumn": [wool("orange"), wool("brown")], "winter": [wool("light_blue"), wool("white")],
        "halloween": [wool("orange"), wool("black")], "harvest": [wool("yellow"), wool("brown")],
        "december": [wool("red"), wool("white")],
    },
    "planter": {
        "spring": [[leaves("flowering_azalea_leaves"), leaves("cherry_leaves")]],
        "summer": [[leaves("azalea_leaves"), leaves("flowering_azalea_leaves"), leaves("oak_leaves")]],
        "autumn": [[S("pumpkin"), S("hay_block", axis="y"), S("pumpkin")]],
        "winter": [[S("snow_block"), leaves("spruce_leaves")]],
        "halloween": [[S("jack_o_lantern", facing="{facing}"), S("carved_pumpkin", facing="{facing}")]],
        "harvest": [[S("hay_block", axis="y"), S("pumpkin"), S("melon")]],
        "december": [[leaves("spruce_leaves"), S("red_wool"), S("green_wool")]],
    },
    "lamp": {
        "spring": [[S("lantern", hanging="false", waterlogged="false")]],
        "summer": [[S("lantern", hanging="false", waterlogged="false")]],
        "autumn": [[S("lantern", hanging="false", waterlogged="false")]],
        "winter": [[S("lantern", hanging="false", waterlogged="false")]],
        "halloween": [[S("soul_lantern", hanging="false", waterlogged="false")]],
        "harvest": [[S("lantern", hanging="false", waterlogged="false")]],
        "december": [[S("lantern", hanging="false", waterlogged="false")]],
    },
}


# ---------------------------------------------------------------- centrepieces (7 x 7 x 13 scenes)

def slot(dx, dy, dz):
    return dx + 7 * dz + 49 * (dy - 1)


def maypole():
    """Spring: a maypole with streamers of wool falling to the ground, flower beds round it."""
    out = {}
    for y in range(1, 12):
        out[slot(3, y, 3)] = S("stripped_birch_log", axis="y")
    out[slot(3, 12, 3)] = leaves("flowering_azalea_leaves")
    colours = ["pink", "light_blue", "yellow", "lime"]
    for i, (dx, dz) in enumerate(((0, 0), (6, 0), (0, 6), (6, 6))):
        for y in range(1, 12):
            # A streamer runs from the pole's top out to each corner.
            t = (11 - y) / 10
            x = round(3 + (dx - 3) * t)
            z = round(3 + (dz - 3) * t)
            if (x, z) != (3, 3):
                out[slot(x, y, z)] = S(f"{colours[i]}_wool")
    for dx, dz in ((1, 3), (5, 3), (3, 1), (3, 5)):
        out[slot(dx, 1, dz)] = leaves("flowering_azalea_leaves")
    return out


def flower_bed():
    """Summer: a young oak in a bed of leaves and flowering bushes, with a stone border."""
    out = {}
    for dx in range(7):
        for dz in range(7):
            edge = dx in (0, 6) or dz in (0, 6)
            if edge and (dx + dz) % 2 == 0:
                out[slot(dx, 1, dz)] = S("stone_brick_slab", type="bottom", waterlogged="false")
            elif not edge:
                out[slot(dx, 1, dz)] = leaves("flowering_azalea_leaves" if (dx * 3 + dz) % 3 else "azalea_leaves")
    for y in range(1, 7):
        out[slot(3, y, 3)] = S("oak_log", axis="y")
    for y in range(5, 9):
        r = 2 if y < 8 else 1
        for dx in range(3 - r, 4 + r):
            for dz in range(3 - r, 4 + r):
                if (dx, dz) != (3, 3) or y > 6:
                    if not (abs(dx - 3) == r and abs(dz - 3) == r):
                        out[slot(dx, y, dz)] = leaves("oak_leaves")
    return out


def harvest_display(feast=False):
    """Autumn and the Harvest Feast: a stack of hay bales and barrels heaped with pumpkins, melons and apples' crates;
    for the Feast a long table with a cake at its head."""
    out = {}
    for dx in range(1, 6):
        for dz in range(1, 6):
            out[slot(dx, 1, dz)] = S("hay_block", axis="x" if dz % 2 else "z")
    for dx in range(2, 5):
        for dz in range(2, 5):
            out[slot(dx, 2, dz)] = S("hay_block", axis="y")
    out[slot(3, 3, 3)] = S("carved_pumpkin", facing="south")
    for dx, dz in ((1, 1), (5, 1), (1, 5), (5, 5)):
        out[slot(dx, 2, dz)] = S("pumpkin")
    for dx, dz in ((0, 3), (6, 3), (3, 0), (3, 6)):
        out[slot(dx, 1, dz)] = S("barrel", facing="up", open="false")
        out[slot(dx, 2, dz)] = S("melon") if (dx + dz) % 2 else S("pumpkin")
    if feast:
        for dz in range(1, 6):
            out[slot(0, 1, dz)] = S("spruce_slab", type="top", waterlogged="false")
        out[slot(0, 2, 3)] = S("cake", bites="0")
        out[slot(0, 2, 1)] = S("candle", candles="2", lit="true", waterlogged="false")
        out[slot(0, 2, 5)] = S("candle", candles="2", lit="true", waterlogged="false")
    return out


def snowman():
    """Winter: a snowman with a carved pumpkin head, ice round his feet and a spruce sapling beside."""
    out = {}
    for dx in range(2, 5):
        for dz in range(2, 5):
            out[slot(dx, 1, dz)] = S("snow_block")
            out[slot(dx, 2, dz)] = S("snow_block") if (dx, dz) != (2, 2) and (dx, dz) != (4, 4) else S("snow_block")
    out[slot(3, 3, 3)] = S("snow_block")
    out[slot(3, 4, 3)] = S("snow_block")
    out[slot(3, 5, 3)] = S("carved_pumpkin", facing="south")
    out[slot(2, 4, 3)] = S("spruce_fence")
    out[slot(4, 4, 3)] = S("spruce_fence")
    for dx in range(7):
        for dz in range(7):
            if (dx in (0, 6) or dz in (0, 6)) and (dx + dz) % 2:
                out[slot(dx, 1, dz)] = S("packed_ice")
    return out


def jack_pile():
    """Halloween: a heap of jack o'lanterns and carved pumpkins under a dead tree hung with cobwebs."""
    out = {}
    for dx in range(1, 6):
        for dz in range(1, 6):
            out[slot(dx, 1, dz)] = S("jack_o_lantern", facing="south") if (dx + dz) % 2 else S("carved_pumpkin", facing="south")
    for dx in range(2, 5):
        for dz in range(2, 5):
            out[slot(dx, 2, dz)] = S("jack_o_lantern", facing="south") if (dx + dz) % 2 == 0 else S("pumpkin")
    out[slot(3, 3, 3)] = S("jack_o_lantern", facing="south")
    for y in range(4, 11):
        out[slot(3, y, 3)] = S("dark_oak_log", axis="y")
    for y, (dx, dz), axis in ((8, (2, 3), "x"), (8, (1, 3), "x"), (9, (4, 3), "x"), (9, (5, 3), "x"), (10, (3, 2), "z"),
                              (10, (3, 1), "z")):
        out[slot(dx, y, dz)] = S("dark_oak_log", axis=axis)
    for dx, y, dz in ((1, 7, 3), (5, 8, 3), (3, 9, 1), (2, 11, 3)):
        out[slot(dx, y, dz)] = S("cobweb")
    return out


def tree_of_lights():
    """December: a tall spruce tree hung with glowing baubles, a star on top, wrapped presents underneath."""
    out = {}
    for y in range(1, 12):
        out[slot(3, y, 3)] = S("spruce_log", axis="y")
    baubles = [S("red_concrete"), S("glowstone"), S("gold_block"), S("sea_lantern"), S("red_concrete"), S("lapis_block")]
    k = 0
    for y in range(2, 12):
        r = max(0, (12 - y) // 3)
        for dx in range(3 - r, 4 + r):
            for dz in range(3 - r, 4 + r):
                if (dx, dz) == (3, 3) and y < 12:
                    continue
                if abs(dx - 3) + abs(dz - 3) > r + (1 if r >= 2 else 0):
                    continue
                edge = abs(dx - 3) + abs(dz - 3) >= r
                if edge and (dx * 5 + dz * 3 + y) % 7 == 0:
                    out[slot(dx, y, dz)] = baubles[k % len(baubles)]
                    k += 1
                else:
                    out[slot(dx, y, dz)] = leaves("spruce_leaves")
    out[slot(3, 12, 3)] = S("gold_block")
    out[slot(3, 13, 3)] = S("lantern", hanging="false", waterlogged="false")
    for (dx, dz), colour in (((0, 1), "red"), ((6, 2), "green"), ((1, 6), "white"), ((5, 6), "red"), ((0, 5), "lime")):
        out[slot(dx, 1, dz)] = S(f"{colour}_wool")
    return out


CENTERPIECES = {
    "spring": maypole, "summer": flower_bed, "autumn": harvest_display, "winter": snowman,
    "halloween": jack_pile, "harvest": lambda: harvest_display(feast=True), "december": tree_of_lights,
}


def data():
    """The decor table the mod reads (written into town.json.gz as "decor")."""
    centre = {theme: {str(k): v for k, v in sorted(fn().items())} for theme, fn in CENTERPIECES.items()}
    return {"themes": THEMES, "kinds": KINDS, "centerpiece": centre}


def all_states():
    """Every state any theme can place, for checking."""
    out = set()
    for kind in KINDS.values():
        for slots in kind.values():
            for choices in slots:
                for state in choices:
                    out.add(state.replace("{facing}", "north").replace("{rotation}", "0"))
    for theme in CENTERPIECES.values():
        out.update(theme().values())
    return out
