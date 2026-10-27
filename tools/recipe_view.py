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

Things only a ritual or a lair boss gives (tools/acquisition.py) are listed under "acquisition_info": [{"item": id,
"key": key}], each shown as an information page with the words of jei.jugcraft.acquire.<key>.

The Mulch Maw's compost worths (tools/concordance_ecology.py compost_view) are listed under "garden_compost":
[{"tag": tag, "quarters": n}]; each tag's items are shown as information pages, an item at its highest worth.
"""
from machines import MACHINES, machine_recipes

MOD = "jugcraft"
# Recipe list (machine_recipes()) and recipe type for each machine block.
LISTS = {"crusher": ("crusher", "crushing"), "arc_furnace_controller": ("arc_furnace", "arc_smelting"),
         "alloy_smelter": ("alloy_smelter", "alloying"), "metal_press": ("metal_press", "pressing"),
         "wire_drawer": ("wire_drawer", "wire_drawing"), "circuit_assembler": ("circuit_assembler", "circuit_assembly"),
         "pulverizer": ("pulverizer", "pulverizing"), "ore_washer": ("ore_washer", "ore_washing"),
         "sieve": ("sieve", "sifting"), "sawmill": ("sawmill", "sawing"), "coke_oven": ("coke_oven", "coking"),
         "steel_foundry": ("steel_foundry", "steelmaking"), "tree_farm": ("tree_farm", "tree_growing"),
         "hydroponic_bay": ("hydroponic_bay", "hydroponics"), "rocket_workshop": ("rocket_workshop", "rocket_assembly"),
         "gas_centrifuge_cascade": ("gas_centrifuge_cascade", "enrichment"), "fuel_fabricator": ("fuel_fabricator", "fuel_fabrication")}


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
    # These five native processors use the same MachineRecipe/JEI shape but
    # retain their owner-authored industrial recipe catalog.
    import jimtafacts_industry
    industrial = jimtafacts_industry.recipes()
    # Retained blade requirements are shared by the full sawmill and compact cutting mill.
    sawmill = next(machine for machine in machines if machine['type']=='sawing')
    sawmill['recipes'] += [{"in":[[r['ingredient'],r.get('input_count',1)]],
        "out":[r['result']['id'],r['result'].get('count',1)],"extra":[],"ticks":r['time'],
        "blade_hardness":r['blade_hardness']} for r in industrial.values() if r.get('blade_hardness',0)>0]
    for block, kind in {'jimta_compressor':'jimta_compressing',
                        'jimta_extractor':'jimta_extracting',
                        'jimta_recycler':'jimta_recycling',
                        'jimta_metal_cutter':'jimta_metal_cutting',
                        'jimta_thermal_centrifuge':'jimta_thermal_centrifuging'}.items():
        rows = [{"in":[[r['ingredient'],r.get('input_count',1)]],
                 "out":[r['result']['id'],r['result'].get('count',1)],
                 "extra":[[b['result']['id'],b['result'].get('count',1),b['chance']] for b in r.get('byproducts',[])],
                 "ticks":r['time'], "minimum_heat":r.get('minimum_heat',0), "blade_hardness":r.get('blade_hardness',0)}
                for r in industrial.values() if r['type']=='jugcraft:'+kind]
        machines.append({"block":"jugcraft:"+block,"type":kind,"recipes":rows})
    import petro
    fluid_machines = []
    for block, recipes in petro.FLUID_RECIPES.items():
        rows = [{"items": [[ref, count] for ref, count in recipe.get("items", [])],
                 "fluids": [[fluid, mb] for fluid, mb in recipe.get("fluids", [])],
                 "fluid_results": [[r[0], r[1]] for r in recipe.get("fluid_results", [])],
                 "results": [[item, count] for item, count in recipe.get("results", [])],
                 "ticks": recipe["ticks"], "tool": recipe.get("tool", ""), "retained_inputs": recipe.get("retained_inputs", [])} for recipe in recipes]
        fluid_machines.append({"block": f"{MOD}:{block}", "type": petro.FLUID_MACHINES[block]["recipe_type"],
                               "recipes": rows})
    # Industrial forms run their own capability recipes, so each form is its own category.
    import industrial_forms
    fluid_machines += industrial_forms.recipe_view()
    import starter_workshop
    for index, station in enumerate(starter_workshop.STATIONS):
        rows = []
        for _, r in starter_workshop.processes():
            if r['station'] != index: continue
            inputs = [[p['ingredient'], p['count']] for p in r['ingredients']]
            retained = []
            if index == 2:
                retained = [len(inputs), len(inputs)+1]
                inputs += [['jugcraft:basic_ceramic_lining',1],['jugcraft:ceramic_crucible',1]]
            if r.get('high_heat'):
                retained += [len(inputs),len(inputs)+1]
                inputs += [['jugcraft:crucible_heat_insert',1],['jugcraft:crucible_bellows',1]]
            rows.append(dict(items=inputs, fluids=[], fluid_results=[], results=[[r['result']['id'],r['result']['count']]],
                ticks=r['time'], tool=r.get('tool',{}).get('id',''), retained_inputs=retained, energy_per_tick=0))
        fluid_machines.append(dict(block='jugcraft:'+station,type='starter_'+station,recipes=rows))
    import concordance
    import acquisition
    import station_recipe_view
    return {"machines": machines, "fluid_machines": fluid_machines, "concordance": concordance.recipe_view(),
            "stations": station_recipe_view.build(),
            "concordance_stations": concordance.recipe_stations(),
            "alchemy_ingredients": concordance.alchemy.recipe_ingredients(), "acquisition_info": acquisition.rows(),
            # Concordance Chunk C, C-07 (C-P57): what a Mulch Maw makes of each tag it eats, for JEI's information pages.
            "garden_compost": concordance.ecology.compost_view()}
