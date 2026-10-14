"""Four shared process families; retain all registered specialist IDs and saved roles."""
PRIMARY = {
    'brine_concentrator': ('Concentrator', ['jugcraft:acid_concentration']),
    'gas_cleaning_rack': ('Gas Scrubber', ['jugcraft:biogas_cleaning']),
    'aluminate_filter_press': ('Filter Press', ['jugcraft:digestate_separation']),
    'alumina_calciner': ('Industrial Oven', ['jugcraft:chloride_drying', 'jugcraft:ptfe_sintering']),
}
LEGACY = {
    'acid_concentrator': 'brine_concentrator',
    'biogas_cleaner': 'gas_cleaning_rack',
    'digestate_separator': 'aluminate_filter_press',
    'chloride_dryer': 'alumina_calciner',
    'ptfe_sintering_oven': 'alumina_calciner',
}
LABELS = {
    'brine_concentrator': ['Process feed', 'Concentrated product', 'Recovered water'],
    'gas_cleaning_rack': ['Dirty gas', 'Wash water', 'Clean gas / product 1', 'Recovered liquid / product 2'],
    'aluminate_filter_press': ['Slurry feed', 'Recovered liquid 1', 'Recovered liquid 2'],
    'alumina_calciner': ['Solution feed', 'Recovered condensate'],
}
GUIDES = {
    'brine_concentrator': ['One Concentrator handles coastal seawater and ordinary sulfuric acid. It replaces the need to build a separate Acid Concentrator. Product leaves outlet 1; recovered water leaves outlet 2. Each process keeps its original ingredients, yields, time and power.'],
    'gas_cleaning_rack': ['One Gas Scrubber cleans synthesis gas, raw biogas and wet fermentation CO2. Feed gas and wash water into their labeled inputs. Outlet 1 collects CO/clean biogas/CO2; outlet 2 collects hydrogen/wash digestate/water respectively. Capture solid synthesis-gas residue in the drawer.'],
    'aluminate_filter_press': ['One Filter Press handles aluminate slurry and digestate. Aluminate produces aluminum hydroxide, bauxite residue, lye at outlet 1 and water at outlet 2. Digestate produces fiber and water at outlet 1; its second liquid/solid outlets remain unused.'],
    'alumina_calciner': ['One Industrial Oven calcines aluminum hydroxide, dries magnesium chloride solution and sinters PTFE blanks. A new lower-rear solution inlet feeds drying; the old solid hatches and condensate outlet stay in place. Solid recipes require the solution tank to be empty. All recipes retain their original costs and products.'],
}

def apply(forms, handbook):
    for name, (display, capabilities) in PRIMARY.items():
        forms[name]['display'] = display
        forms[name]['capabilities'].extend(capabilities)
        handbook[name][:0] = GUIDES[name] + [
            'Open Recipes to search and preview jobs. Lock recipe limits new batches; Automatic restores input-based selection. Locks persist across saves; current batches finish unchanged. Drain incompatible input AND output fluids and remove other feed items before changing jobs. Empty outputs for sustained automation. You can use one machine for different jobs or dedicate identical copies to separate factory lines.'
        ]
    for old, primary in LEGACY.items():
        handbook[old].insert(0, f"Optional specialist model: existing machines, recipes, inventories and production remain supported. For a new factory, build the {PRIMARY[primary][0]}, which also runs this machine's processes. This specialist stays craftable for existing layouts but is omitted from the main creative machinery list.")

def write_labels(lang, forms):
    lang.update({'container.jugcraft.form.' + key: text for key, text in UI_LABELS.items()})
    lang['container.jugcraft.form.reason.mixed_inputs'] = 'Mixed jobs: clear incompatible feed items or liquids.'
    for name, labels in LABELS.items():
        roles = forms[name]['input_tanks'] + forms[name]['output_tanks']
        for role, label in zip(roles, labels, strict=True):
            lang[f'container.jugcraft.form.tank.{name}.{role}'] = label

UI_LABELS = {
    "no_recipe_matches": "No matching recipes. Try an ingredient or product name.",
    "cleaning_title": "Gas Scrubber",
    "recipes_locked": "Locked recipe",
    "reason.recipe_unavailable": "Locked recipe unavailable. Restore it or choose another recipe.",
    "recipes": "Recipes",
    "recipes_hint": "Browse inputs, outputs and costs; lock a job or use automatic mode.",
    "search_recipes": "Search recipes",
    "lock_recipe": "Lock recipe",
    "automatic": "Automatic",
    "back_machine": "Back to machine",
    "no_recipes": "No supported recipes on this server.",
    "recipe_cost": "%s ticks (%s s) | %s JE/t | %s JE per batch",
    "recipe_cost_hint": "Per lane, with installed upgrades. %s parallel lanes; no extra upgrade required.",
    "recipe_inputs": "INPUTS - consumed per batch",
    "recipe_outputs": "OUTPUTS - products and byproducts",
    "recipe_item_in": "Input slot %s: %s x %s",
    "recipe_fluid_in": "Input tank %s: %s mB %s",
    "recipe_item_out": "Output slot %s: %s x %s",
    "recipe_fluid_out": "Output tank %s: %s mB %s",
    "recipe_tool_present": "Reusable tool present (not consumed): %s",
    "recipe_tool_missing": "Reusable tool REQUIRED (not consumed): %s",
    "recipe_no_tool": "No reusable tool required.",
    "recipe_retained": "Retained liquid: kept even with overflow enabled.",
    "recipe_overflow": "Overflow setting can drain/vent this product. Turn it off to retain the full yield.",
    "recipe_ports_hint": "Ports: column, depth, height (1-based); sides relative to machine front. Scroll for all details.",
    "recipe_status": "Live machine status: ",
    "catalog_stale": "Recipes reloaded: close and reopen the machine to refresh.",
    "automatic_hint": "Automatic: inputs choose the next job.",
    "locked_recipe": "Locked: %s",
    "lock_unavailable": "Locked recipe unavailable; choose a valid job or Automatic.",
    "next_batch_hint": "Changes affect new batches only."
}
