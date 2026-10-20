"""The four entry chemistry forms in the owner's package 2; local playtest, October 12.

This slice delivers gas production, cleaning, methanation and contact conversion. Wet acid absorption in the
existing Chemical Reactor remains a separate integration step (its present layout has only one fluid input).
Numbers not previously selected by the owner are explicit playtest defaults, not retroactive design approvals.
"""
MOD = "jugcraft"
GASES = {
    "carbon_monoxide": {"display": "Carbon Monoxide", "feature": "machines", "colors": [(100, 112, 108), (136, 151, 143), (176, 187, 177), (218, 225, 216)]},
    "carbon_dioxide": {"display": "Carbon Dioxide", "feature": "machines", "colors": [(116, 106, 92), (156, 145, 128), (194, 183, 164), (228, 219, 200)]},
    "methane": {"display": "Methane", "feature": "machines", "colors": [(94, 139, 132), (133, 180, 169), (176, 210, 195), (220, 238, 225)]},
    "hydrogen_chloride": {"display": "Hydrogen Chloride Gas", "feature": "machines", "colors": [(139, 144, 102), (178, 185, 140), (212, 219, 178), (239, 242, 216)]},
    "sulfur_dioxide": {"display": "Sulfur Dioxide", "feature": "machines", "colors": [(153, 135, 76), (189, 172, 113), (218, 204, 157), (242, 234, 203)]},
    "sulfur_trioxide": {"display": "Sulfur Trioxide", "feature": "machines", "colors": [(133, 129, 114), (172, 169, 150), (205, 202, 181), (235, 234, 217)]},
    "dirty_synthesis_gas": {"display": "Dirty Synthesis Gas", "feature": "machines", "colors": [(67, 60, 48), (98, 91, 74), (137, 130, 109), (177, 172, 150)]},
}
ITEMS = {
    "nickel_catalyst_bed": "Nickel Catalyst Bed",
    "vanadium_contact_bed": "Vanadium Contact Bed",
    "vanadium_iron_concentrate": "Vanadium-bearing Iron Concentrate",
    "vanadium_oxide": "Vanadium Oxide",
    "mixed_gas_residue": "Mixed Gas-cleaning Residue",
    "oxidizer_burner": "Lined Oxidizer Burner",
}

def form(display, family, width, depth, height, inputs, outputs, capability, ports, recipe, **extra):
    layers = [["#" * width for _ in range(depth)] for _ in range(height)]
    layers[0][0] = "C" + "#" * (width - 1)
    return dict(display=display, family=family, layers=layers, profile="ENTRY", tank_mb=2000,
                input_tanks=inputs, output_tanks=outputs, capabilities=[f"jugcraft:{capability}"],
                upgrades=True, warmup=40, ports=ports, recipe=recipe, features=["machines"], **extra)

FORMS = {
    "chemical_infuser": form("Chemical Infuser", "synthesis_converter", 2, 3, 3,
        ["reagent_a", "reagent_b"], ["product_gas", "process_water"], "starter_gas_infusing", [
            ("reagent_a_in", "FLUID_IN", 0, 0, 0, 2, "FRONT"),
            ("reagent_b_in", "FLUID_IN", 1, 1, 0, 2, "FRONT"),
            ("product_out", "FLUID_OUT", 0, 0, 2, 1, "BACK"),
            ("water_out", "FLUID_OUT", 1, 1, 2, 0, "BACK"),
            ("power", "ENERGY_IN", 0, 0, 0, 0, "FRONT")],
        (["PIP", "TCT", "PWP"], {"P": "#c:plates/steel", "I": "jugcraft:fluid_tank", "T": "jugcraft:steel_fluid_pipe", "C": "jugcraft:basic_circuit", "W": "jugcraft:copper_cable"}),
        sockets=[("catalyst_bed", "jugcraft:infuser_catalysts")], water_drain=1),
    "chemical_oxidizer": form("Chemical Oxidizer", "chemical_reactor", 2, 2, 3,
        ["oxidant"], ["product_gas"], "starter_gas_oxidizing", [
            ("oxygen_in", "FLUID_IN", 0, 1, 0, 0, "FRONT"),
            ("feed_in", "ITEM_IN", 0, 0, 1, 2, "BACK"),
            ("gas_out", "FLUID_OUT", 0, 1, 1, 2, "BACK"),
            ("power", "ENERGY_IN", 0, 0, 0, 0, "FRONT")],
        (["PBP", "MCK", "P P"], {"P": "#c:plates/steel", "B": "jugcraft:oxidizer_burner", "M": "jugcraft:electric_motor", "C": "jugcraft:basic_circuit", "K": "jugcraft:machine_casing"}), items=(1, 0)),
    "coal_gasifier": form("Coal Gasifier", "chemical_reactor", 4, 4, 6,
        ["feed_water"], ["dirty_gas"], "starter_gasification", [
            ("water_in", "FLUID_IN", 0, 0, 3, 0, "BACK"),
            ("carbon_in", "ITEM_IN", 0, 0, 1, 3, "LEFT"),
            ("dirty_gas_out", "FLUID_OUT", 0, 3, 3, 5, "BACK"),
            ("power", "ENERGY_IN", 0, 0, 0, 0, "FRONT")],
        (["PBP", "BFK", "PTP"], {"P": "#c:plates/steel", "B": "minecraft:brick", "F": "minecraft:furnace", "K": "jugcraft:machine_casing", "T": "jugcraft:steel_fluid_pipe"}), items=(1, 0)),
    "gas_cleaning_rack": form("Gas Cleaning and Separation Rack", "chemical_reactor", 3, 5, 4,
        ["dirty_gas", "wash_water"], ["carbon_monoxide", "hydrogen"], "starter_gas_cleaning", [
            ("dirty_gas_in", "FLUID_IN", 0, 2, 4, 3, "BACK"),
            ("wash_water_in", "FLUID_IN", 1, 0, 0, 0, "LEFT"),
            ("co_out", "FLUID_OUT", 0, 0, 0, 2, "FRONT"),
            ("hydrogen_out", "FLUID_OUT", 1, 1, 0, 2, "FRONT"),
            ("residue_out", "ITEM_OUT", 0, 1, 0, 0, "FRONT"),
            ("power", "ENERGY_IN", 0, 2, 0, 0, "FRONT")],
        (["PTP", "ICI", "PMP"], {"P": "#c:plates/steel", "T": "jugcraft:steel_tank", "I": "jugcraft:fluid_tank", "C": "jugcraft:basic_circuit", "M": "jugcraft:electric_motor"}), items=(0, 1)),
}

def recipe(name, form_name, fluids, outputs, ticks, energy, **extras):
    return dict(name=name, capability=FORMS[form_name]["capabilities"][0], fluids=fluids, fluid_results=outputs,
                ticks=ticks, energy_per_tick=energy, features=["machines"], **extras)

RECIPES = {
    "chemical_infuser": [
        recipe("infuser_hydrogen_chloride", "chemical_infuser", [("jugcraft:hydrogen", 250), ("jugcraft:chlorine", 250)], [("jugcraft:hydrogen_chloride", 500, 0)], 100, 96),
        recipe("infuser_co_methanation", "chemical_infuser", [("jugcraft:carbon_monoxide", 100), ("jugcraft:hydrogen", 300)], [("jugcraft:methane", 100, 0), ("minecraft:water", 200, 1)], 200, 128, tool="jugcraft:nickel_catalyst_bed"),
        recipe("infuser_co2_methanation", "chemical_infuser", [("jugcraft:carbon_dioxide", 100), ("jugcraft:hydrogen", 400)], [("jugcraft:methane", 100, 0), ("minecraft:water", 400, 1)], 200, 128, tool="jugcraft:nickel_catalyst_bed"),
        recipe("infuser_contact_conversion", "chemical_infuser", [("jugcraft:sulfur_dioxide", 250), ("jugcraft:oxygen", 125)], [("jugcraft:sulfur_trioxide", 250, 0)], 100, 128, tool="jugcraft:vanadium_contact_bed"),
    ],
    "chemical_oxidizer": [
        recipe("oxidizer_sulfur_dioxide", "chemical_oxidizer", [("jugcraft:oxygen", 250)], [("jugcraft:sulfur_dioxide", 250, 0)], 100, 96, items=[("jugcraft:sulfur_dust", 1)]),
        recipe("oxidizer_carbon_dioxide", "chemical_oxidizer", [("jugcraft:oxygen", 250)], [("jugcraft:carbon_dioxide", 250, 0)], 100, 96, items=[("minecraft:charcoal", 1)]),
    ],
    "coal_gasifier": [
        recipe("gasifier_" + name, "coal_gasifier", [("minecraft:water", amount)], [("jugcraft:dirty_synthesis_gas", amount, 0)], 200, 64, items=[(item, 1)], emissions=emissions)
        for name, item, amount, emissions in [("coal", "minecraft:coal", 1000, 4), ("charcoal", "minecraft:charcoal", 500, 1), ("coke", "jugcraft:coke", 1500, 3)]
    ],
    "gas_cleaning_rack": [
        recipe("rack_clean_synthesis_gas", "gas_cleaning_rack", [("jugcraft:dirty_synthesis_gas", 500), ("minecraft:water", 125)],
               [("jugcraft:carbon_monoxide", 250, 0), ("jugcraft:hydrogen", 250, 1)], 100, 48, results=[("jugcraft:mixed_gas_residue", 1)]),
    ],
}

TANK_NAMES = {"reagent_a": "First reagent", "reagent_b": "Second reagent", "product_gas": "Product gas", "process_water": "Recovered water",
              "oxidant": "Oxygen", "feed_water": "Process water", "dirty_gas": "Dirty synthesis gas", "wash_water": "Wash water", "carbon_monoxide": "Carbon monoxide"}
HANDBOOK = {
    "chemical_infuser": [
        "Two wide, three deep and three tall. Join the upper front collars to first and second reagent supplies. The product leaves the middle back left; recovered water leaves low at the back right. Power joins the front control box.",
        "Hydrogen + chlorine makes hydrogen chloride gas. Carbon monoxide or dioxide + hydrogen makes methane with a Nickel Catalyst Bed. Sulfur dioxide + oxygen makes sulfur trioxide with a Vanadium Contact Bed. Put the reusable bed in the protected tool socket, not an input slot. It cannot be removed during a batch.",
        "Drain excess water is ON by default. It collects water that fits and drains only excess plain water. Switch to Keep water to stop when the water tank is full. The selection applies to the next batch; a running batch keeps its original setting. No gases, lye or acids are dumped.",
        "Nickel bed: nickel ingot between two bricks. Vanadium bed: wash Vanadium-bearing Iron Concentrate, made by crushing mined Vanadium-bearing Iron (one raw feed or intact ore per concentrate, 200 ticks at 16 JE/t), into oxide, then put the oxide between two bricks. This starter route needs no acid. Pipe product gas to a Chemical Reactor side, with water in its first tank. For sulfuric acid, charge the top with one bucket of sulfuric acid made by the original sulfur-and-water recipe or obtained by trade. The charge stays inside and is retained when the reactor is broken."],
    "chemical_oxidizer": [
        "Two wide and deep, three tall. Place sulfur dust in the feed slot and pipe 250 mB oxygen into the low front right collar. In five seconds at 96 JE/t it makes 250 mB sulfur dioxide. Charcoal instead makes carbon dioxide for methanation. Pipe the product out of the upper rear collar.",
        "The rear hopper, guarded feed drive, lined reaction throat and capped collector stay closed during work. A spinning feed coupling and amber inspection slit show that it is running. Its burner subassembly is a furnace plus glass, so the final crafting grid fits the full steel-entry bill."],
    "coal_gasifier": [
        "Four wide and deep, six tall: a refractory column with a broad sealed ash chamber, side charging chute, capped header and service cabinet. Feed carbon through the left-side chute, water at the rear foot and power at the front cabinet. Dirty synthesis gas leaves the upper rear right header.",
        "One coal + 1000 mB water gives 1000 mB dirty gas; charcoal uses 500 water for 500 gas; coke uses 1500 water for 1500 gas. Each batch pays 200 ticks at 64 JE/t including electrical heating. Upgrade cards fit the screen. Dirty gas must be cleaned; it is not a second burnable fuel.",
        "The local process counter records 4 emissions units per coal, 1 per charcoal, 3 per coke only after completion. These are playtest balancing units, not a world pollution or terrain-damage system. Cancelling never earns outputs or emissions."],
    "gas_cleaning_rack": [
        "Three wide, five deep and four tall. A stepped wash tower, collection pot, separator, closed pipe loops and service bridge share one machine. Dirty gas enters high at the rear; wash water enters low on the left. The two front upper outlets give carbon monoxide on the left, hydrogen on the right. Residue leaves the low front drawer.",
        "500 mB dirty synthesis gas + 125 mB wash water gives 250 mB carbon monoxide, 250 mB hydrogen and one wet mixed residue in 100 ticks at 48 JE/t. Both gases and the residue must fit before work starts. There are no disposable filters.",
        "Stabilize one residue with clay and two gravel into two cobblestone road-fill blocks. That consumes the residue once; it gives no second fuel or recovered metals. Existing gas cylinders, tanks and pipes carry every new gas. Feed the cleaned hydrogen and carbon monoxide to an Infuser; methane drives the Gas Burning Generator at 256 JE/t, 448 JE/mB."],
}

def write_extras(write, assets, data, lang, condition):
    for index, title in enumerate(["Liquid feed (sides)", "Absorption gas (sides)", "Retained acid charge (top)", "Product (sides)"]):
        lang[f"container.jugcraft.reactor.tank.{index}"] = title
    from steampunk_models import box, cyl
    import model_writer
    write(data / "tags/item/infuser_catalysts.json", {"replace": False, "values": ["jugcraft:nickel_catalyst_bed", "jugcraft:vanadium_contact_bed", "jugcraft:cobalt_catalyst_bed"]})
    lang["container.jugcraft.form.socket.catalyst_bed"] = "Reusable catalyst bed"
    lang["container.jugcraft.form.cleaning_title"] = "Gas Scrubber"
    lang["container.jugcraft.form.drain_on"] = "Drain excess"
    lang["container.jugcraft.form.drain_off"] = "Keep water"
    lang["container.jugcraft.form.drain_hint"] = "Next batch: collect water that fits and drain only excess water. Running batches keep their setting."
    lang["container.jugcraft.form.emissions"] = "Process emissions: %s units (local counter)"
    def craft(name, ingredients, result, count=1):
        if name in ("nickel_catalyst_bed", "vanadium_contact_bed"): name = "expanded_" + name
        conditions = condition("machines")
        if name == "vanadium_iron_concentrate": conditions += [{"condition": "jugcraft:vanadium_legacy_iron"}]
        write(data / "recipe" / f"{name}.json", {"fabric:load_conditions": conditions, "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": result, "count": count}})
    craft("nickel_catalyst_bed", ["#c:ingots/nickel", "minecraft:brick", "minecraft:brick"], "jugcraft:nickel_catalyst_bed")
    craft("vanadium_contact_bed", ["jugcraft:vanadium_oxide", "minecraft:brick", "minecraft:brick"], "jugcraft:vanadium_contact_bed")
    craft("vanadium_iron_concentrate", ["minecraft:raw_iron"] * 4, "jugcraft:vanadium_iron_concentrate")
    craft("oxidizer_burner", ["minecraft:furnace", "minecraft:glass"], "jugcraft:oxidizer_burner")
    craft("stabilized_gas_residue", ["jugcraft:mixed_gas_residue", "minecraft:clay_ball", "minecraft:gravel", "minecraft:gravel"], "minecraft:cobblestone", 2)
    # Small native item models, using the same ceramic and metal surfaces as the machine. No new rendering system.
    out = model_writer.StyleWriter(assets)
    for name in ITEMS:
        if name.endswith("bed"):
            coating = "dp_chrome" if name.startswith("nickel") else "sp_copper"
            elements = [box((2, 3, 3), (14, 6, 13), "el_white"), box((1, 3, 2), (15, 4, 14), "dp_gunmetal")]
            for x in (4, 7, 10):
                elements.append(box((x, 6, 4), (x+2, 7, 12), coating))
        elif name == "oxidizer_burner":
            elements = [box((2, 2, 2), (14, 14, 14), "el_white"), box((4, 4, 1.5), (12, 11, 2), "dp_gunmetal"), box((5, 5, 1.25), (11, 9, 1.5), "dr_amber")]
        else:
            coating = "sp_copper" if name == "vanadium_oxide" else "dp_gunmetal"
            elements = [box((3, 2, 3), (13, 5, 13), coating), box((5, 5, 5), (11, 7, 11), coating)]
        textures = {n: f"jugcraft:block/{n}" for n in model_writer.texture_names(elements)}
        textures['particle'] = 'jugcraft:block/dp_olive'
        out.item_model(name, {"parent": "minecraft:block/block", "textures": textures, "elements": model_writer.scaled_elements(elements)})
        out.item(name, f"jugcraft:item/{name}")
