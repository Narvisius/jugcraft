"""Machine recipes for recipe viewers (JEI), generated from the same tables as the mod's recipe data.

Written to assets/jugcraft/recipe_view.json by generate_material_data.py. Recipes live on the server
in 26.x, so the client-side JEI plugin (client/compat/JugcraftJeiPlugin) reads this copy instead.
Data packs that change machine recipes are not reflected here.

Format: {"machines": [{"block": id, "type": recipe type, "recipes": [
  {"in": [[ref, count]], "out": [id, count], "extra": [[id, count, chance]], "ticks": n}]}]}
Input refs may be tags ("#c:ingots/tin"); the plugin shows every item in the tag.

Fluid machines (tools/petro.py) are listed under "fluid_machines": {"block", "type", "recipes": [
  {"items": [[ref, count]], "fluids": [[fluid, mB]], "fluid_results": [[fluid, mB]], "results": [[id, count]],
   "ticks": n}]}.
"""
from machines import MACHINES, machine_recipes

MOD = "jugcraft"
# Recipe list (machine_recipes()) and recipe type for each machine block.
LISTS = {"crusher": ("crusher", "crushing"), "arc_furnace_controller": ("arc_furnace", "arc_smelting"),
         "alloy_smelter": ("alloy_smelter", "alloying"), "metal_press": ("metal_press", "pressing"),
         "wire_drawer": ("wire_drawer", "wire_drawing"), "circuit_assembler": ("circuit_assembler", "circuit_assembly"),
         "pulverizer": ("pulverizer", "pulverizing"), "ore_washer": ("ore_washer", "ore_washing"),
         "sieve": ("sieve", "sifting"), "sawmill": ("sawmill", "sawing"), "coke_oven": ("coke_oven", "coking"),
         "steel_foundry": ("steel_foundry", "steelmaking"), "tree_farm": ("tree_farm", "tree_growing")}


def build():
    recipes = machine_recipes()
    machines = []
    for block, (key, kind) in LISTS.items():
        assert block in MACHINES, block
        rows = []
        for recipe in recipes[key]:
            rows.append({
                "in": [[ref, count] for ref, count in (recipe.get("inputs") or [[recipe["input"], 1]])],
                "out": [recipe["output"], recipe["count"]],
                "extra": [[item, count, chance] for item, count, chance, _ in recipe.get("byproducts", [])],
                "ticks": recipe["ticks"],
            })
        machines.append({"block": f"{MOD}:{block}", "type": kind, "recipes": rows})
    import petro
    fluid_machines = []
    for block, recipes in petro.FLUID_RECIPES.items():
        rows = [{"items": [[ref, count] for ref, count in recipe.get("items", [])],
                 "fluids": [[fluid, mb] for fluid, mb in recipe.get("fluids", [])],
                 "fluid_results": [[fluid, mb] for fluid, mb in recipe.get("fluid_results", [])],
                 "results": [[item, count] for item, count in recipe.get("results", [])],
                 "ticks": recipe["ticks"]} for recipe in recipes]
        fluid_machines.append({"block": f"{MOD}:{block}", "type": petro.FLUID_MACHINES[block]["recipe_type"],
                               "recipes": rows})
    return {"machines": machines, "fluid_machines": fluid_machines}
