"""Gas storage (batch 35, docs/features/gas-storage.md): the gas cylinder and the ammonia chiller.

Java: fluid/GasCylinderItem.java (the cylinder) and MachineKind.AMMONIA_CHILLER (a fluid processor; its recipes are in
FLUID_RECIPES below and go into tools/petro.py). tools/check_mod_data.py keeps the numbers here and in Java the same.

Rethought for usefulness: gases could only move through pipes, so hydrogen for a far-off fuel cell, oxygen for a scuba
tank in the field or nitrogen for the grapple meant running a pipe there. A cylinder carries 8 buckets of one gas and
fills or empties by hand. Ammonia, made by the synthesis converter, had only fertilizer and nitric acid as uses; as a
refrigerant it makes ice anywhere, even in the Nether, and packs it far more cheaply than crafting does, so blue ice
for fast boat roads no longer needs a frozen ocean.
"""
import math

MOD = "jugcraft"

# The cylinder: mB of one gas.
CYLINDER_CAPACITY = 8_000

# The chiller: ticks per batch, ammonia lost per batch (the refrigerant loop leaks a little), and its tanks.
CHILLER_TICKS = 100
AMMONIA_PER_BATCH = 5
AMMONIA_TANK = 4_000
WATER_TANK = 8_000
# Ice blocks per packed ice and packed ice per blue ice (crafting needs 9 of each).
PACK = 4

ITEMS = {"gas_cylinder": "Gas Cylinder"}
TOOLTIPS = {
    "gas_cylinder": "Holds 8 buckets of one gas. Use it on a tank, pipe or machine to fill it; sneak to empty it "
                    "there instead. Used in the air, it tops up a scuba tank (oxygen) or a grapple (nitrogen) in "
                    "your other hand.",
}

# The chiller's tanks: ammonia first, then water (Java: MachineKind.fluidSpec()).
FLUID_MACHINE = {"inputs": [AMMONIA_TANK, WATER_TANK], "outputs": [], "item_inputs": 1, "item_outputs": 1,
                 "recipe_type": "chilling"}
FLUID_RECIPES = [
    # A bucket of water frozen into a block of ice.
    {"name": "ice", "fluids": [("jugcraft:ammonia", AMMONIA_PER_BATCH), ("minecraft:water", 1000)],
     "results": [("minecraft:ice", 1)], "ticks": CHILLER_TICKS, "features": ["machines"]},
    # Ice and packed ice pressed and chilled further.
    {"name": "packed_ice", "items": [("minecraft:ice", PACK)], "fluids": [("jugcraft:ammonia", AMMONIA_PER_BATCH)],
     "results": [("minecraft:packed_ice", 1)], "ticks": CHILLER_TICKS, "features": ["machines"]},
    {"name": "blue_ice", "items": [("minecraft:packed_ice", PACK)], "fluids": [("jugcraft:ammonia", AMMONIA_PER_BATCH)],
     "results": [("minecraft:blue_ice", 1)], "ticks": CHILLER_TICKS, "features": ["machines"]},
]


def items():
    return list(ITEMS)


def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"message.{MOD}.gas_cylinder.filled"] = "Gas cylinder: %s mB of %s"
    lang[f"message.{MOD}.gas_cylinder.nothing"] = "Nothing to move"
    write(data / "recipe" / "gas_cylinder.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": [" V ", "PGP", "PPP"],
        "key": {"V": f"{MOD}:fluid_valve", "G": f"{MOD}:gasket", "P": "#c:plates/steel"},
        "result": {"id": f"{MOD}:gas_cylinder", "count": 1}})


# ------------------------------------------------------------------ art (64x64, tools/hd_art.py)

def gas_cylinder():
    """An industrial gas cylinder standing upright: a gunmetal body with a painted white shoulder and a stencilled
    band, a steel foot ring, a chrome valve under a brass guard collar with a red handwheel, and a little gauge."""
    import hd_art as hd
    from hd_art import Canvas
    c = Canvas()
    # The body: round-shouldered, its top painted white to show the gas, a hazard band round the middle.
    c.capsule((32, 25), (32, 50), 12, hd.GUNMETAL,
              bands=[(-1.0, 0.12, hd.WHITE_PAINT), (0.46, 0.56, hd.SAFETY_YELLOW)])
    c.capsule((32, 54), (32, 59), 11.5, hd.GUNMETAL, flat_ends=True)
    c.box((32, 60), 11, 1.6, 0, hd.STEEL, bevel=1.0)  # foot ring
    for x in range(25, 40, 4):  # stencil marks under the band
        c.box((x + 1, 44), 1.0, 2.0, 0, hd.WHITE_PAINT, bevel=0.3)
    # Neck, valve and the guard collar round it.
    c.capsule((32, 12), (32, 18), 4.2, hd.STEEL, flat_ends=True)
    c.capsule((32, 6), (32, 12), 2.6, hd.CHROME, flat_ends=True)
    c.capsule((32, 9), (39, 9), 1.4, hd.BRASS)  # outlet
    c.ring((32, 9), 9.5, 8.0, hd.GUNMETAL)  # guard collar
    c.capsule((26, 4), (38, 4), 1.4, hd.RED)  # handwheel
    c.disc((32, 4), 1.6, hd.CHROME, 0.9)
    # A small gauge on the outlet.
    c.ring((42, 9), 3.4, 2.2, hd.CHROME)
    c.disc((42, 9), 2.3, hd.GAUGE_FACE, 0.2)
    c.line((42, 9), (42 + math.cos(math.radians(-40)) * 1.8, 9 + math.sin(math.radians(-40)) * 1.8), (30, 30, 34))
    return c.finish()


HD_ITEMS = {"gas_cylinder": gas_cylinder}


def draw_all(save):
    for item, draw in HD_ITEMS.items():
        save(draw(), "item", item)
