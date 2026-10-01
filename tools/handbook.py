"""The Engineer's Handbook: in-game guide content, generated from the same tables as the mod.

generate_material_data.py writes it to assets/jugcraft/handbook/en_us.json; the client screen
(client/HandbookScreen.java) reads it. Numbers and recipes come from tools/machines.py and
tools/materials.py, so the book stays in step with the game. Only the prose is written here.

Format: {"chapters": [{"title", "icon", "pages": [{"title", "icon", "text": [paragraphs],
"craft": {"grid": [9 item ids or null], "result", "count"}, "recipes": [{"in": [[id, count]], "out": [id, count]}]}]}]}
"""
from materials import COMPONENTS, METALS, MINERALS, ingot_id, ore_ids
from machines import (CRAFTING, MACHINES, STATS, CABLES, PIPES, FLUID_BLOCKS, ITEM_PIPES, LOGISTICS_BLOCKS, STORAGE_BLOCKS, KINETIC_BLOCKS, TOOLS, POWERED_TOOLS, TOOL_BLOCKS, UPGRADE_MODULES,
                      UPGRADES, BYPRODUCTS, ORE_PROCESSING_MULTIPLIER, ORE_WASHING_MULTIPLIER, machine_recipes)

MOD = "jugcraft"

# Recipe list in machine_recipes() for each machine block.
RECIPE_LISTS = {"crusher": "crusher", "arc_furnace_controller": "arc_furnace", "alloy_smelter": "alloy_smelter",
                "metal_press": "metal_press", "wire_drawer": "wire_drawer", "circuit_assembler": "circuit_assembler",
                "pulverizer": "pulverizer", "ore_washer": "ore_washer", "sieve": "sieve", "sawmill": "sawmill",
                "coke_oven": "coke_oven", "steel_foundry": "steel_foundry", "tree_farm": "tree_farm"}

# What each block is for, in a sentence or two. Numbers are added from the tables below.
ABOUT = {
    "coal_generator": "Burns coal, charcoal, coal blocks or coke to make power. It stops burning when full, so fuel is never wasted.",
    "battery_box": "Stores power. It charges from every side and gives power out of its front only.",
    "electric_furnace": "Smelts anything a vanilla furnace can, twice as fast.",
    "crusher": "Crushes one ore into two raw ores, minerals into extra minerals, cobblestone into gravel and gravel into sand.",
    "arc_furnace_controller": "The heart of the Arc Furnace: build a solid 3x3x3 cube of Arc Furnace Casing with this block in the "
                              "middle of one face, facing out. It melts what an ordinary furnace cannot.",
    "solar_panel": "Makes power from daylight under open sky: 8 JE/t in sun, half in rain, nothing at night.",
    "steam_generator": "Boils water with coal or bitumen: twice the power of the coal generator per fuel. A water source "
                       "block directly below refills it for free.",
    "alloy_smelter": "A 2x2 furnace that melts two ingredients together into alloys. Power goes in only through its copper "
                     "socket, on the outer side of its lower right block.",
    "metal_press": "Presses one ingot into one plate. Four plates craft a gear.",
    "wire_drawer": "Draws one ingot into three wires.",
    "circuit_assembler": "Assembles circuits from up to three ingredients, in any slots.",
    "geothermal_generator": "Two blocks wide. Burns lava from its tank (buckets, pumps or pipes): one bucket lasts 1,000 ticks.",
    "wind_turbine": "Nine blocks tall with a seven-block rotor. The higher it stands and the worse the weather, the more it "
                    "makes, up to 72 JE/t (twice that in a thunderstorm). The rotor needs a clear 7x7 square in front of "
                    "the top.",
    "pulverizer": "Grinds ore into two dusts, with a chance of a second metal. Also grinds washed ore, raw metal and ingots.",
    "ore_washer": "Washes one ore into three washed ores, using 500 mB of water each time. Grind them in the pulverizer.",
    "sieve": "Sifts gravel into flint and soul sand into soul soil, with a small chance of nuggets or quartz.",
    "sawmill": "Cuts logs into six planks (four by hand) with sawdust, and planks into three sticks.",
    "coke_oven": "Two blocks tall, needs no power. Bakes coal into coal coke, a hotter fuel and the carbon for steel.",
    "steel_foundry": "Three blocks tall, needs no power. Refines one iron ingot with one coke into one steel ingot.",
    "copper_cable": "Carries power between generators, batteries and machines. It connects by itself to anything that stores "
                    "or uses power on the touching face.",
    "silver_cable": "A faster cable: 1,024 JE/t, four times copper. Cable tiers join into one network, which carries "
                    "as much as its slowest cable.",
    "aluminum_cable": "Steel-armored power line: 4,096 JE/t, for big batteries and the arc furnace.",
    "high_pressure_extractor": "A steel extractor: 32 items every 4 ticks, four times the brass one.",
    "capacitor_bank": "A 2x2 bank of Leyden jars: 4,000,000 JE. It charges from any side and gives power out of the "
                      "sockets on its front, 4,096 JE/t.",
    "steel_tank": "A 2x2 riveted tank: 128 buckets of one fluid. Buckets, pumps and pipes fill and empty it; right-click "
                  "with an empty hand to read it.",
    "item_crate": "Holds 32 stacks of one item. Right-click with an item to put it in, with an empty hand to take a "
                  "stack out (sneak to just look). Works with pipes, hoppers and comparators.",
    "bronze_fluid_pipe": "Carries fluid that a pump pushes into it to every tank and fluid machine it touches.",
    "fluid_tank": "Holds 16 buckets of one fluid. Fill or empty it with buckets; right-click with an empty hand to read it.",
    "electric_pump": "Pulls water or lava from the block below it and pushes it out of its top and sides.",
    "brass_item_pipe": "Joins inventories into a network. Items go to matching sorters first, then take turns between the "
                       "other inventories.",
    "pneumatic_extractor": "Pulls 16 items every 8 ticks from what it faces and pushes them out of its other sides. A "
                           "redstone signal pauses it.",
    "item_sorter": "Takes only the items in its 9-slot filter, and puts them into the inventory it faces.",
    "mining_drill": "Runs on JE instead of wearing out: 60 JE a block, 100,000 JE when full. Mines everything a "
                    "pickaxe or shovel does, faster than netherite. Sneak and use it to switch mode: one block, a 3x3 "
                    "square, or a whole ore vein. Empty, it mines like a bare hand.",
    "chainsaw": "A JE-powered axe that also cuts leaves: 40 JE a block. Cutting a log fells the whole tree above it; "
                "sneak to cut one log.",
    "rocket_pack": "Wear it and hold jump in the air to fly, 50 JE a tick (200,000 JE when full). Landing is safe while "
                   "it fires. On a dedicated server, set allow-flight=true or long hovers get you kicked.",
    "charging_station": "Two blocks tall. Hang a drill, chainsaw or rocket pack on its cradle and it fills it from "
                        "cables, 512 JE a tick; take it back with an empty hand. Its lamp lights while it charges.",
    **{module: f"{about}. Fit it by using it on a charging station holding the tool; it is used up and stays in "
                 "the tool." for module, (_, _, about) in UPGRADE_MODULES.items()},
    "conveyor": "Carries items the way you faced when placing it, 2.5 blocks a second, while rotation drives it. A shaft, "
                "gearbox or motor on any side drives every conveyor joined to it, for 1 KE per conveyor per tick. Pipes, "
                "hoppers and machines load it, and so do items dropped on it; at the end items go into the conveyor or "
                "inventory ahead, or onto the ground. It carries you too; sneak to stand still.",
    "conveyor_splitter": "A conveyor that sends items left, straight on and right in turn, skipping any way that is "
                         "blocked.",
    "brass_wrench": "Right-click turns a machine. Sneak and right-click to pick a Jugcraft block up, with everything inside.",
    "speed_upgrade": "In a machine's upgrade slot: each card makes it faster but uses more energy per item. Four cards: 3x as "
                     "fast for twice the energy.",
    "efficiency_upgrade": "In a machine's upgrade slot: each card cuts energy use by a fifth. Four cards: 41% of the energy.",
    "prospector": "Right-click to survey the 3x3 chunks around you, from the bottom of the world to a little above you. "
                  "It shows which ores resonate, how strongly (1 to 5 bars) and roughly how deep, never exactly where.",
    "ore_drill": "Two blocks tall. Mines the ore blocks in a 9x9 column below it, one layer at a time down to the bottom "
                 "of the world, one ore every 2 seconds. Each hole is refilled with stone or deepslate. The ores come "
                 "out whole, ready for ore processing.",
    "cobblestone_generator": "Makes one cobblestone a second while water and lava touch it, on any sides. Neither "
                             "is used up. Speed upgrades make it faster.",
    "tree_farm": "Grows a sapling into six logs in 20 seconds and gives the sapling back, sometimes with an apple, "
                 "cocoa beans or other extras. Feed the sapling back in with eject and a pipe for endless wood.",
    "water_wheel": "Two blocks tall. The wheel on its right side turns in flowing water: 8 JE/t for each of its two "
                   "blocks with flowing water beside it, 12 if the water is falling. Still water does not turn it.",
    "iron_shaft": "Carries rotation (KE, kinetic energy) along its length, placed like a log. Machines at the end of a "
                  "shaft line run straight off it: 1 KE counts as 1 JE, with no cables.",
    "brass_gearbox": "Passes rotation out of all six sides, to branch a shaft line or turn a corner. Power is shared "
                     "evenly between everything on the line.",
    "hand_crank": "Place it against a shaft, gearbox or machine and right-click: each crank turns it for 5 seconds "
                  "(up to 20) at 16 KE/t. Cranking makes you a little hungry.",
    "steam_engine": "Burns coal, charcoal, coke or bitumen and boils water to turn its flywheel: 64 KE/t out of its "
                    "back. Right-click with fuel or a water bucket, or feed it with hoppers, pipes and pumps; a water "
                    "source below refills it. It burns only while something takes the power.",
    "large_steam_engine": "Two by two by two. Four times the small steam engine: 256 KE/t out of a shaft at the back of "
                          "its upper right block, using 40 mB of water per tick and fuel four times as fast. It has a "
                          "screen like the steam generator's, and a water source under it refills it.",
    "belt_pulley": "A shaft with a grooved wheel. Use a Leather Belt on two pulleys with the same axis (level with each "
                   "other along it, up to 16 blocks apart) and the second turns with the first: power jumps gaps and "
                   "walls.",
    "belt": "Links two belt pulleys: use it on one, then on the other. Breaking a pulley drops the belt.",
    "electric_motor": "Turns JE from cables back into rotation at 75%, up to 96 KE/t out of its shaft, which points "
                      "the way you looked when placing it. Motor and dynamo together always lose power.",
    "dynamo": "Turns rotation reaching any face into JE at 75% and pushes it into cables on every side: the bridge "
              "from a shaft line to the electric network.",
    "auto_crafter": "Crafts the crafting recipe laid out in its 3x3 grid. Set the pattern by hand; each grid slot "
                    "keeps its last item as the pattern, so it crafts while every filled slot holds two or more. Pipes "
                    "and hoppers top up slots that already hold that item. Empty buckets and bottles go to the slot "
                    "above the output.",
    "engineers_handbook": "This book. Craft it from a book and a copper ingot.",
}

TAG_ITEMS = {"#c:silicon": "jugcraft:silicon", "#minecraft:planks": "minecraft:oak_planks", "#minecraft:logs": "minecraft:oak_log",
             "#minecraft:bamboo_blocks": "minecraft:bamboo_block", "#c:coal_coke": "jugcraft:coke"}


def item_for(ref):
    """An item that stands for a recipe reference (tags show one of their items)."""
    if not ref.startswith("#"):
        return ref
    if ref in TAG_ITEMS:
        return TAG_ITEMS[ref]
    ns, path = ref[1:].split(":")
    if ns == "minecraft" and (path.endswith("_logs") or path.endswith("_stems")):
        return f"minecraft:{path[:-1]}"
    form, _, metal = path.partition("/")
    if form == "ingots":
        return ingot_id(metal)
    if form == "ores":
        return ore_ids(metal)[0]
    if form == "raw_materials":
        return f"minecraft:raw_{metal}" if metal in ("copper", "iron", "gold") else f"{MOD}:raw_{metal}"
    if form in ("plates", "gears", "wires", "dusts"):
        return f"{MOD}:{metal}_{form[:-1]}"
    raise KeyError(ref)


def craft(result):
    pattern, key, count = CRAFTING[result]
    rows = [row.ljust(3) for row in pattern] + ["   "] * (3 - len(pattern))
    grid = [item_for(key[ch]) if ch != " " else None for row in rows for ch in row]
    return {"grid": grid, "result": f"{MOD}:{result}", "count": count}


def recipe_rows(machine, limit=4):
    rows = []
    for recipe in machine_recipes()[RECIPE_LISTS[machine]]:
        inputs = recipe.get("inputs") or [[recipe["input"], 1]]
        if any(ref.split(":")[-1].startswith("deepslate_") for ref, _ in inputs):
            continue  # Same as the stone ore's row.
        row = {"in": [[item_for(ref), count] for ref, count in inputs], "out": [recipe["output"], recipe["count"]]}
        if recipe.get("byproducts"):
            row["extra"] = [[item, chance] for item, _, chance, _ in recipe["byproducts"]]
        if row not in rows:
            rows.append(row)
        if len(rows) == limit:
            break
    return rows


def power_line(block):
    stats = STATS.get(block)
    if not stats or not stats.get("capacity"):
        return "Needs no power."
    if "use_per_tick" in stats:
        return f"Uses {stats['use_per_tick']} JE/t while working. Holds {stats['capacity']:,} JE."
    if "generation_per_tick" in stats:
        return f"Makes up to {stats['generation_per_tick']} JE/t. Holds {stats['capacity']:,} JE."
    return f"Holds {stats['capacity']:,} JE; {stats['io_per_tick']} JE/t in and out."


def milestone_pages():
    """The advancement quest line (tools/advancements.py), in order, a few steps to a page."""
    from advancements import TREE
    steps = [(title, description, items if isinstance(items, str) else items[0])
             for _, (_, items, title, description, _) in TREE.items()]
    pages = []
    for start in range(0, len(steps), 5):
        chunk = steps[start:start + 5]
        pages.append({"title": "Milestones" if start == 0 else f"Milestones ({start // 5 + 1})",
                      "icon": f"{MOD}:{chunk[0][2]}",
                      "text": (["Your advancements (key L) track these steps."] if start == 0 else [])
                      + [f"{title}: {description}." for title, description, _ in chunk]})
    return pages


def block_page(block, display):
    page = {"title": display, "icon": f"{MOD}:{block}", "text": [ABOUT[block]]}
    if block in MACHINES:
        page["text"].append(power_line(block))
    if block in CRAFTING:
        page["craft"] = craft(block)
    if block in RECIPE_LISTS:
        page["recipes"] = recipe_rows(block)
    return page


def machine_page(block):
    return block_page(block, MACHINES[block]["display"])


def ores_page():
    lines = []
    for metal, info in METALS.items():
        if info["mined"]:
            gen = info["gen"]
            lines.append(f"{info['display']}: Y {gen['min_y']} to {gen['max_y']}, needs a {info['tool']} pickaxe.")
    for mineral, info in MINERALS.items():
        gen = info["gen"]
        lines.append(f"{info['ore_display']}: Y {gen['min_y']} to {gen['max_y']}.")
    return {"title": "Ores", "icon": f"{MOD}:tin_ore",
            "text": ["Jugcraft ores appear in the stone and deepslate of every Overworld biome:"] + lines}


def build():
    chapters = [
        {"title": "Getting Started", "icon": f"{MOD}:engineers_handbook", "pages": [
            {"title": "Welcome, Engineer", "icon": f"{MOD}:engineers_handbook", "text": [
                "This handbook covers every Jugcraft machine: what it does, what it needs and how to build it.",
                "Pick a chapter on the left. Page through a chapter with the arrows.",
                "Power is measured in JE (Jugcraft Energy) per tick, fluids in mB (1,000 mB is a bucket)."]},
            {"title": "Your First Power", "icon": f"{MOD}:coal_generator", "text": [
                "Mine tin and zinc, then make bronze (3 copper + 1 tin) by hand and smelt it.",
                "Build a Machine Casing (bronze and zinc), a Coal Generator and some Copper Cable, then an Electric "
                "Furnace. Run cable from the generator to the furnace, or place them side by side.",
                "Every machine holds its own charge, so it keeps working for a while after the power stops."],
             "craft": craft("machine_casing")},
        ] + milestone_pages()},
        {"title": "Materials", "icon": f"{MOD}:bronze_ingot", "pages": [
            ores_page(),
            block_page("prospector", TOOLS["prospector"]),
            machine_page("ore_drill"),
            {"title": "Ore Processing", "icon": f"{MOD}:tin_dust", "text": [
                "Smelting an ore gives one ingot.",
                f"Crushing or pulverizing it first gives {ORE_PROCESSING_MULTIPLIER}.",
                f"Washing it, then pulverizing the washed ore, gives {ORE_WASHING_MULTIPLIER}.",
                "Pulverizing ore sometimes gives a second metal's dust: " + ", ".join(
                    f"{a} gives {b}" for a, (b, _) in list(BYPRODUCTS.items())[:6]) + "."]},
            {"title": "Parts", "icon": f"{MOD}:bronze_gear", "text": [
                "Plates (Metal Press): " + ", ".join(COMPONENTS["plate"]) + ".",
                "Gears (four plates): " + ", ".join(COMPONENTS["gear"]) + ".",
                "Wires (Wire Drawer): " + ", ".join(COMPONENTS["wire"]) + ".",
                "Every part works with other mods' parts through c: tags."]},
        ]},
        {"title": "Power", "icon": f"{MOD}:coal_generator", "pages":
            [machine_page(m) for m in ("coal_generator", "solar_panel", "steam_generator", "geothermal_generator",
                                       "wind_turbine", "battery_box")]
            + [block_page(c, CABLES[c]["display"]) for c in CABLES]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("steam_engine",)]
            + [machine_page("large_steam_engine")]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("hand_crank", "iron_shaft", "brass_gearbox",
                                                                     "belt_pulley")]
            + [block_page("belt", TOOLS["belt"])]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("dynamo", "electric_motor")]},
        {"title": "Processing", "icon": f"{MOD}:crusher", "pages":
            [machine_page(m) for m in ("electric_furnace", "crusher", "alloy_smelter", "metal_press", "wire_drawer",
                                       "circuit_assembler", "arc_furnace_controller", "auto_crafter")]},
        {"title": "Ore Processing", "icon": f"{MOD}:pulverizer", "pages":
            [machine_page(m) for m in ("pulverizer", "ore_washer", "sieve", "sawmill")]},
        {"title": "Steel", "icon": f"{MOD}:steel_ingot", "pages":
            [machine_page(m) for m in ("coke_oven", "steel_foundry")]
            + [block_page(b, TOOL_BLOCKS[b]["display"]) for b in TOOL_BLOCKS]
            + [block_page(t, POWERED_TOOLS[t]) for t in POWERED_TOOLS]
            + [block_page(m, UPGRADE_MODULES[m][0]) for m in UPGRADE_MODULES]},
        {"title": "Fluids", "icon": f"{MOD}:fluid_tank", "pages":
            [block_page("bronze_fluid_pipe", PIPES["bronze_fluid_pipe"]["display"])]
            + [block_page(b, FLUID_BLOCKS[b]["display"]) for b in ("fluid_tank", "electric_pump")]},
        {"title": "Logistics", "icon": f"{MOD}:brass_item_pipe", "pages": [
            {"title": "Machine Sides", "icon": f"{MOD}:crusher", "text": [
                "Every processing machine's screen has six face buttons: front, back, left, right, top and bottom.",
                "Click one to cycle: In (blue), Out (orange), Both (green) or Off (gray).",
                "Eject makes the machine push its results out of its Out faces by itself.",
                "The R button sets how it reacts to redstone: always run, run with a signal, or run without one."]},
            block_page("brass_item_pipe", ITEM_PIPES["brass_item_pipe"]["display"]),
            block_page("pneumatic_extractor", LOGISTICS_BLOCKS["pneumatic_extractor"]["display"]),
            block_page("high_pressure_extractor", LOGISTICS_BLOCKS["high_pressure_extractor"]["display"]),
            block_page("item_sorter", LOGISTICS_BLOCKS["item_sorter"]["display"]),
            block_page("conveyor", KINETIC_BLOCKS["conveyor"]["display"]),
            block_page("conveyor_splitter", KINETIC_BLOCKS["conveyor_splitter"]["display"]),
            block_page("brass_wrench", TOOLS["brass_wrench"]),
        ]},
        {"title": "Storage", "icon": f"{MOD}:item_crate", "pages":
            [block_page("item_crate", STORAGE_BLOCKS["item_crate"]["display"])]
            + [machine_page(m) for m in ("capacitor_bank", "steel_tank")]},
        {"title": "Renewables", "icon": f"{MOD}:tree_farm", "pages":
            [machine_page(m) for m in ("water_wheel", "cobblestone_generator", "tree_farm")]},
        {"title": "Upgrades", "icon": f"{MOD}:speed_upgrade", "pages":
            [block_page(u, UPGRADES[u]) for u in UPGRADES] + [
            {"title": "Comparators", "icon": "minecraft:comparator", "text": [
                "A comparator next to a generator or battery shows how full it is.",
                "Next to a processing machine, it shows how full its slots are, like a chest."]}]},
    ]
    return {"chapters": chapters}
