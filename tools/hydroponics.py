"""Hydroponics (batch 33, docs/features/hydroponics.md): the hydroponic bay grows crops indoors, in nutrient solution.

A seed or cutting in the bay grows into a harvest every cycle, on power and nutrient solution (fertilizer dissolved in
water, from the chemical reactor), with no soil, sunlight or farmland: underground, in the Nether or the End. The seed
comes back each time, as with the tree farm. Java: MachineKind.HYDROPONIC_BAY; tools/check_mod_data.py checks the
numbers here against MachineKind.
"""

# The bay's nutrient tank (mB), what one harvest uses (mB), and how long a harvest takes (ticks).
TANK = 8_000
SOLUTION_PER_HARVEST = 100
TICKS = 600

# What grows: (input, output, count, extra seed chance or None, features). The input always comes back.
_VANILLA = [
    ("minecraft:wheat_seeds", "minecraft:wheat", 2, 0.5),
    ("minecraft:carrot", "minecraft:carrot", 3, None),
    ("minecraft:potato", "minecraft:potato", 3, None),
    ("minecraft:beetroot_seeds", "minecraft:beetroot", 2, 0.5),
    ("minecraft:melon_seeds", "minecraft:melon_slice", 6, None),
    ("minecraft:pumpkin_seeds", "minecraft:pumpkin", 1, None),
    ("minecraft:sugar_cane", "minecraft:sugar_cane", 3, None),
    ("minecraft:cactus", "minecraft:cactus", 2, None),
    ("minecraft:bamboo", "minecraft:bamboo", 4, None),
    ("minecraft:sweet_berries", "minecraft:sweet_berries", 3, None),
    ("minecraft:glow_berries", "minecraft:glow_berries", 2, None),
    ("minecraft:cocoa_beans", "minecraft:cocoa_beans", 3, None),
    ("minecraft:nether_wart", "minecraft:nether_wart", 3, None),
    ("minecraft:kelp", "minecraft:kelp", 3, None),
    ("minecraft:red_mushroom", "minecraft:red_mushroom", 2, None),
    ("minecraft:brown_mushroom", "minecraft:brown_mushroom", 2, None),
]


def crops():
    """[(input, output, count, extra chance, features)] for every crop the bay grows."""
    out = [(i, o, n, x, ["machines"]) for i, o, n, x in _VANILLA]
    out.append(("jugcraft:cotton_seeds", "jugcraft:cotton", 2, 0.5, ["machines"]))
    import agriculture as ag
    for info in ag.CROPS.values():
        seed, produce = info["seed"], info["produce"]
        extra = 0.5 if seed != produce else None
        out.append((f"jugcraft:{seed}", f"jugcraft:{produce}", 2 if extra else 3, extra, ["machines", ag.FEATURE]))
    for info in ag.TALL_CROPS.values():
        if "pick" in info:
            out.append((f"jugcraft:{info['seed']}", f"jugcraft:{info['pick']['item']}", 2, 0.5, ["machines", ag.FEATURE]))
    for gourd, info in ag.GOURDS.items():
        out.append((f"jugcraft:{info['seed']}", f"jugcraft:{gourd}", 1, None, ["machines", ag.FEATURE]))
    return out


def recipes():
    """The bay's machine recipes, in tools/machines.py's format: the seed comes back every time."""
    out = []
    for seed, produce, count, extra, features in crops():
        byproducts = [[seed, 1, 1.0, None]]
        if extra:
            byproducts.append([seed, 1, extra, None])
        out.append({"name": seed.split(":")[1], "input": seed, "output": produce, "count": count, "ticks": TICKS,
                    "features": features, "renewable": True, "byproducts": byproducts})
    return out
