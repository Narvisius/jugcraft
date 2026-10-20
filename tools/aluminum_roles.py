"""Aluminum's bulk electrical, lightweight structural and vehicle roles (docs/features/aluminum-roles.md): aluminum plates
in place of steel where a light metal suits the job, as alternatives that leave the steel recipes in place.

- Aluminum-sheathed cable: the aluminum cable's steel armour swapped for an aluminum sheath, and four cables, not three.
- The zeppelin's frame of aluminum plates, as the great airships had (duralumin girders).
- The heliostat's mirror backing of an aluminum plate.
"""

MOD = "jugcraft"
ALUMINUM_PLATE = "#c:plates/aluminum"

# id: (pattern, key, result, count). Each mirrors an existing steel recipe with its steel plates made of aluminum.
RECIPES = {
    "aluminum_cable_aluminum_sheathed": (["WPW", "RRR"], {"W": "#c:wires/aluminum", "P": ALUMINUM_PLATE,
                                                          "R": f"{MOD}:silver_cable"}, "aluminum_cable", 4),
    "zeppelin_aluminum_frame": (["HHH", "HEH", "PCP"], {"H": f"{MOD}:hydrogen_lift_cell", "E": f"{MOD}:diesel_engine",
                                                        "P": ALUMINUM_PLATE, "C": "minecraft:chest"}, "zeppelin", 1),
    "heliostat_aluminum_backing": (["GGG", " M ", " P "], {"G": "minecraft:glass_pane", "M": f"{MOD}:electric_motor",
                                                           "P": ALUMINUM_PLATE}, "heliostat", 2),
}


def write_all(write, data, condition):
    """The recipes, loading with the machines and aluminum switches."""
    for name, (pattern, key, result, count) in RECIPES.items():
        write(data / "recipe" / f"{name}.json", {
            "fabric:load_conditions": condition("machines") + condition("aluminum"),
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{result}", "count": count}})
