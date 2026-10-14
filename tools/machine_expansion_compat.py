"""Combine the owner playtest expansion with released factory definitions.

Existing main saves own the shared IDs, layouts and inventory slot indices.
Preview tank-role aliases retain fluid contents where those prototypes differed.
"""
from copy import deepcopy

SHARED = ("chemical_infuser", "chemical_oxidizer", "coal_gasifier",
          "anaerobic_digester", "industrial_fermenter", "bio_generator")


def apply(released, forms, recipes, handbook, models, rotors, extras):
    for name in SHARED:
        preview = forms[name]
        form = deepcopy(released["forms"][name])
        forms[name] = form
        form["alternate_recipe"] = preview["recipe"]
        form["capabilities"] = list(dict.fromkeys(form["capabilities"] + preview["capabilities"]))
        form["aliases"] = [(old, new) for key in ("input_tanks", "output_tanks")
                           for old, new in zip(preview[key], form[key]) if old != new]
        # Keep all previously published recipe IDs and give colliding preview recipes
        # their own stable IDs. Both appear in the recipe picker.
        old_recipes = deepcopy(released["recipes"].get(name, []))
        names = {row["name"] for row in old_recipes}
        added = deepcopy(recipes.get(name, []))
        for row in added:
            row["priority"] = -10  # Published automatic routes remain first; locks can select either.
            if name == "chemical_infuser":
                row["fluids"].sort(key=lambda feed: 0 if feed[0] in ("jugcraft:hydrogen", "jugcraft:oxygen") else 1)
            if row["name"] in names:
                row["name"] = "expanded_" + row["name"]
        if old_recipes or added:
            recipes[name] = old_recipes + added
        else:
            recipes.pop(name, None)
        handbook[name] = deepcopy(released["handbook"][name])
        if added:
            handbook[name].append("Expanded processing is also available. Open Recipes to inspect each route's exact inputs, outputs and power, then lock a recipe for automation. Empty incompatible fluids before changing processes; old factory connections and recipes remain supported.")
        models[name] = released["models"][name]
        extras[name] = released["extras"].get(name, [])
    for key in list(rotors):
        if rotors[key]["block"] in SHARED:
            del rotors[key]
    rotors.update({key: value for key, value in released["rotors"].items() if value["block"] in SHARED})
    forms["chemical_infuser"]["water_drain"] = 1
    # Append a liquid digestate output without moving the old biogas/solid slots.
    digester = forms["anaerobic_digester"]
    digester["output_tanks"].append("digestate")
    digester["ports"].append(("liquid_digestate_out", "FLUID_OUT", 1, 0, 5, 0, "BACK"))
    handbook["anaerobic_digester"].append("The expanded route collects liquid digestate at the lower rear-left outlet for the Filter Press. The original route still makes biogas and bone meal. This vessel retains four bulk lanes and 16,000 mB tanks.")
    # The fermenter's existing collect/vent control applies to either recipe's gas.
    # The secondary preview vent toggle would otherwise bypass that control.
    forms["bio_generator"]["fuels"].append(("jugcraft:clean_biogas", 192, 64))
    handbook["bio_generator"].append("Also burns cleaned biogas from the expanded scrubber chain at 192 JE/mB, up to 64 JE/t.")

