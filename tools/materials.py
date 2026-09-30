"""Single source of truth for Jugcraft material data (IDs, recipes, tags, worldgen).

Minecraft 26.3's data-generation run cannot execute in every contributor environment,
so the JSON resources are produced by generate_material_data.py from this table and
checked by check_mod_data.py. Keep it in sync with materials/JugcraftMaterials.java.
"""

MOD = "jugcraft"

# name -> (mined, extra items, display name, feature switch)
METALS = {
    "tin": {"mined": True, "extras": [], "display": "Tin", "feature": "tin"},
    "bronze": {"mined": False, "extras": ["bronze_blend"], "display": "Bronze", "feature": "tin"},
}

EXTRA_NAMES = {"bronze_blend": "Bronze Blend"}

# Ore generation targets from issue #2 (tunable; roughly half of vanilla copper).
ORE_GEN = {
    "tin": {"size": 9, "count": 8, "min_y": -32, "max_y": 96},
}


def blocks(metal):
    info = METALS[metal]
    out = []
    if info["mined"]:
        out += [f"{metal}_ore", f"deepslate_{metal}_ore", f"raw_{metal}_block"]
    out.append(f"{metal}_block")
    return out


def items(metal):
    info = METALS[metal]
    out = []
    if info["mined"]:
        out.append(f"raw_{metal}")
    out += info["extras"]
    out += [f"{metal}_ingot", f"{metal}_nugget"]
    return out


def all_blocks():
    return [b for m in METALS for b in blocks(m)]


def all_items():
    return [i for m in METALS for i in items(m)]
