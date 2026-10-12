"""The Engineer's Handbook: in-game guide content, generated from the same tables as the mod.

generate_material_data.py writes it to assets/jugcraft/handbook/en_us.json; the client screen
(client/HandbookScreen.java) reads it. Numbers and recipes come from tools/machines.py and
tools/materials.py, so the book stays in step with the game. Only the prose is written here.

Format: {"chapters": [{"title", "icon", "pages": [{"title", "icon", "text": [paragraphs],
"craft": {"grid": [9 item ids or null], "result", "count"}, "recipes": [{"in": [[id, count]], "out": [id, count]}],
"steps": [{"item", "label"}]}]}]}. Steps are drawn as a numbered chain (the Progression chapter).
"""
from materials import COMPONENTS, METALS, MINERALS, ingot_id, ore_ids
from machines import (ELECTRONICS_BLOCKS, FARMING_BLOCKS, CRAFTING, MACHINES, STATS, CABLES, PIPES, FLUID_BLOCKS, ITEM_PIPES, LOGISTICS_BLOCKS, STORAGE_BLOCKS, KINETIC_BLOCKS, TOOLS, POWERED_TOOLS, TOOL_BLOCKS, UPGRADE_MODULES, SLOPE_BLOCKS,
                      UPGRADES, BYPRODUCTS, ORE_PROCESSING_MULTIPLIER, ORE_WASHING_MULTIPLIER, ORE_LEACHING_MULTIPLIER,
                      machine_recipes)

MOD = "jugcraft"

# Recipe list in machine_recipes() for each machine block.
RECIPE_LISTS = {"crusher": "crusher", "arc_furnace_controller": "arc_furnace", "alloy_smelter": "alloy_smelter",
                "metal_press": "metal_press", "wire_drawer": "wire_drawer", "circuit_assembler": "circuit_assembler",
                "pulverizer": "pulverizer", "ore_washer": "ore_washer", "sieve": "sieve", "sawmill": "sawmill",
                "coke_oven": "coke_oven", "steel_foundry": "steel_foundry", "tree_farm": "tree_farm",
                "hydroponic_bay": "hydroponic_bay", "rocket_workshop": "rocket_workshop"}

# What each block is for, in a sentence or two. Numbers are added from the tables below.
ABOUT = {
    "coal_generator": "Burns coal, charcoal, coal blocks or coke to make power. Coke lasts twice as long as coal, and "
                      "charcoal three quarters as long. It stops burning when full, so fuel is never wasted.",
    "battery_box": "Stores power. It charges from every side and gives power out of its front only.",
    "electric_furnace": "Smelts anything a vanilla furnace can, twice as fast.",
    "crusher": "Crushes one ore into two raw ores, minerals into extra minerals, cobblestone into gravel and gravel into sand.",
    "arc_furnace_controller": "The heart of the Arc Furnace: build a solid 3x3x3 cube of Arc Furnace Casing with this block in the "
                              "middle of one face, facing out. It melts what an ordinary furnace cannot, and it pulls "
                              "silicon boules: 4 silicon and a phosphate (the dopant) give a boule every 20 seconds. "
                              "Pipe argon into the controller and it works twice as fast, for 1 mB a tick.",
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
    "steel_foundry": "Two by two and five tall, needs no power. Refines one iron ingot with one coke into one steel "
                     "ingot. Pipe oxygen into it (from the air separation unit) and it blows the charge: twice as "
                     "fast, for 2 mB of oxygen a tick.",
    "copper_cable": "Carries power between generators, batteries and machines. It connects by itself to anything that stores "
                    "or uses power on the touching face.",
    "silver_cable": "A faster cable: 1,024 JE/t, four times copper. Cable tiers join into one network, which carries "
                    "as much as its slowest cable.",
    "aluminum_cable": "Steel-armored power line: 4,096 JE/t, for big batteries and the arc furnace.",
    "high_pressure_extractor": "A steel extractor: 32 items every 4 ticks, four times the brass one.",
    "capacitor_bank": "A 2x2 bank of Leyden jars: 4,000,000 JE. It charges from any side and gives power out of the "
                      "sockets on its front, 4,096 JE/t.",
    "sprinkler": "Pipe water in (or use a water bucket) and it sprays the crops within 4 blocks, at its height and one "
                 "below: every 5 seconds it uses 50 mB and gives each growing crop an extra growth tick. Load up to 16 "
                 "fertilizer (by hand or hopper) and every 30 seconds it spreads one over the 5x5 crops around it.",
    "cryogenic_liquefier": "A cold box of chrome heat exchangers under frost. Pipe oxygen (from the air separation unit) "
                           "into its tank: every 4 seconds a bucket of oxygen condenses into 250 mB of liquid oxygen, "
                           "the oxidizer for liquid rocket motors. 96 JE/t.",
    "ammonia_chiller": "Ammonia boiling in its coils draws the heat out of water. Pipe ammonia into its first tank and "
                       "water into its second: every 5 seconds a bucket of water freezes into a block of ice. Put four "
                       "ice in its slot and it presses them into packed ice, and four packed ice into blue ice, against "
                       "nine of each by crafting. Each batch loses 5 mB of ammonia. It works anywhere, even in the "
                       "Nether.",
    "electroplating_bath": "Plates a tool, weapon or piece of armor and repairs it fully, without experience, so "
                           "enchanted gear keeps going. Put the item in the first slot and an ingot in the second, with "
                           "sulfuric acid piped in: 10 seconds and 100 mB a plating. Nickel makes it half as durable "
                           "again; silver gives a weapon Smite III; gold makes armor count as gold for piglins. Plate "
                           "again with the same metal to repair; a different metal is refused.",
    "hydroponic_bay": "Grows a seed or cutting in nutrient solution under grow lights: no soil, sunlight or farmland, "
                      "so it works underground, in the Nether or the End. A harvest every 30 seconds uses 100 mB of "
                      "solution (a fertilizer in a bucket of water, in the chemical reactor) and gives the seed back.",
    "crop_harvester": "Two blocks tall. Harvests the ripe crops in the 9x9 field in front of it, starting the block in "
                      "front: one crop a second at 24 JE/t. It keeps the drops and plants one of the seeds again, and "
                      "waits when its three result slots are full. Crops on farmland sit at its own height.",
    "gas_holder": "A 3x3x3 sphere on legs: 1,024 buckets of one gas, and nothing but gases (liquids go in the steel "
                  "tank). Pumps and pipes fill and empty it from any face; right-click with an empty hand to read it. "
                  "Comparators read how full it is.",
    "steel_tank": "A 2x2 riveted tank: 128 buckets of one fluid. Buckets, pumps and pipes fill and empty it; right-click "
                  "with an empty hand to read it.",
    "item_crate": "Holds 32 stacks of one item. Right-click with an item to put it in, with an empty hand to take a "
                  "stack out (sneak to just look). Works with pipes, hoppers and comparators.",
    "bronze_fluid_pipe": "Carries fluid that a pump pushes into it to every tank and fluid machine it touches.",
    "fluid_tank": "Holds 16 buckets of one fluid. Fill or empty it with buckets; right-click with an empty hand to read it.",
    "electric_pump": "Pulls water or lava from the block below it and pushes it out of its top and sides.",
    "fluid_valve": "A steel pipe segment with a valve. Open, it carries fluid like a steel pipe; a redstone signal "
                   "closes it, and the pipes on either side become separate lines. Its lamp is green while open and "
                   "amber while closed.",
    "fluid_filter": "A steel pipe segment with a strainer. Fluid passes along it, but the tanks and machines it touches "
                    "only get its chosen fluid (nothing until one is chosen). Use a filled bucket on it, or right-click "
                    "it beside a tank holding the fluid (the way to choose a gas); sneak and right-click to clear it.",
    "steel_fluid_pipe": "Like the bronze pipe, but carries 1,000 mB a tick for refinery flows. A pipe line carries as much "
                        "as its slowest pipe, so one bronze pipe holds a steel line back to 250 mB.",
    "heavy_pump": "A steel-tier pump: 1,000 mB a tick from below (water, lava or a tank) out of its top and sides, at "
                  "40 JE a tick, with a 16-bucket buffer.",
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
    "conveyor_slope": "Carries items one block up, onto the top of the block in front, or down from a conveyor one "
                      "block higher behind it. Use it with an empty hand to switch between up and down. It joins and "
                      "runs with the conveyors around it.",
    "conveyor_splitter": "A conveyor that sends items left, straight on and right in turn, skipping any way that is "
                         "blocked.",
    "brass_wrench": "Right-click turns a machine. Sneak and right-click to pick a Jugcraft block up, with everything inside.",
    "speed_upgrade": "In a machine's upgrade slot: each card makes it faster but uses more energy per item. Four cards: 3x as "
                     "fast for twice the energy.",
    "efficiency_upgrade": "In a machine's upgrade slot: each card cuts energy use by a fifth. Four cards: 41% of the energy.",
    "prospector": "Right-click to survey the 3x3 chunks around you, from the bottom of the world to a little above you. "
                  "It shows which ores resonate, how strongly (1 to 5 bars) and roughly how deep, never exactly where.",
    "pumpjack": "One block wide, three tall and three long; place it with the wellhead where you want the well. If "
                "the chunk under the wellhead holds pumpable oil (the prospector's Oil reading), it pumps 2 mB of crude "
                "oil a tick into its 16-bucket tank and pushes it into pipes and tanks touching it. A reservoir runs dry "
                "for good after 50 to 250 buckets; shale oil needs a fracking rig instead.",
    "distillation_tower": "Two by two and seven blocks tall. Heats crude oil and splits each bucket into 100 mB of "
                          "refinery gas, 250 mB of naphtha, 400 mB of diesel and 250 mB of heavy fuel oil, a bucket every "
                          "5 seconds. Each fraction comes out at its own height: heavy fuel oil at the base, diesel two "
                          "blocks up, naphtha four up, and refinery gas at the top. Give each its own pipe or tank. Pipe heavy fuel oil "
                          "in instead and it boils it under vacuum: each bucket gives 400 mB of lubricant (one block "
                          "up) and two asphalt binder (in its slot), every 6 seconds.",
    "catalytic_cracker": "Two by two and four blocks tall. Cracks heavy fuel oil into lighter fuels with steam and a "
                         "catalyst: 1,000 mB of heavy fuel oil, 250 mB of water and a cracking catalyst give 500 mB of "
                         "diesel (out at the base), 300 mB of naphtha (two blocks up) and 200 mB of refinery gas (at the "
                         "top), every 8 seconds. Pipe naphtha in instead and it reforms it over the same catalyst: "
                         "each bucket and a catalyst give 900 mB of gasoline (one block up) and 100 mB of refinery gas "
                         "(at the top), every 6 seconds.",
    "fracking_rig": "Three by three and five blocks tall. Place it with its front left block over shale oil (the "
                    "prospector's Shale oil reading). Each powered tick it pumps 4 mB of fracking fluid down the well "
                    "and brings up 6 mB of crude oil (out at the base), 2 mB of refinery gas (out at the top) and 3 mB "
                    "of flowback water (out one block up), until the shale is spent.",
    "flowback_treatment_unit": "Three wide, one tall and two deep: settling basins and a filter press. It separates "
                               "what settles. Flowback water from a fracking rig: each bucket gives 750 mB of clean "
                               "water and a salt (a quarter is lost each time round). Oil sand (mined with silk touch) "
                               "and 250 mB of water give 500 mB of crude oil and a block of sand; a bitumen and 100 mB "
                               "of water give 150 mB. A block of mud gives four clay balls and 250 mB of water.",
    "diesel_generator": "Three wide, two tall and two deep. Burns diesel or heavy fuel oil piped into its 8-bucket tank: "
                        "256 JE/t, a bucket of diesel every 1,000 ticks (256,000 JE) or heavy fuel oil twice as fast "
                        "(128,000 JE a bucket). It refuses crude oil and other fluids.",
    "electrolytic_cell": "Three wide, three tall and two deep. Splits brine with electricity: a bucket gives 250 mB of "
                         "chlorine (out of the top row), 250 mB of hydrogen (the middle row) and 500 mB of lye (the "
                         "bottom row), every 20 seconds at 256 JE/t. Make brine in the chemical reactor from two salt "
                         "and a bucket of water. It also splits plain water, slowly: a bucket gives 500 mB of hydrogen "
                         "(middle row) and 250 mB of oxygen (top row) every 40 seconds, an early fuel for the fuel cell.",
    "chemical_reactor": "Two by two by two, lined with lead against the acid: the general chemistry vessel, with two "
                        "item slots, a tank in and a tank out. Two sulfur dust and a bucket of water make a bucket of "
                        "sulfuric acid. It also mixes: two salt and a bucket of water make brine; two sand, a dried "
                        "kelp and a bucket of water make fracking fluid. An ore in 250 mB of sulfuric acid gives four washed "
                        "ores, the best ore route. Eight crops (wheat, sugar cane, potatoes, carrots, beetroot, "
                        "berries, melon or apples) in a bucket of water ferment into 250 mB of bioethanol. Its other "
                        "reactions are on its recipe pages.",
    "air_separation_unit": "Two by two and six tall: a cold box and its distillation column. It needs no input: it "
                           "liquefies air and splits it, 8 mB of nitrogen a tick out of the top row, 2 mB of oxygen "
                           "out of the bottom row and a little argon (1 mB every 2 ticks) out of the middle, at 64 "
                           "JE/t. Pipe the gases to the synthesis converter, the steel foundry, the arc furnace or "
                           "a gas holder; it stops while any tank is full.",
    "synthesis_converter": "Three wide, four tall and two deep: a high-pressure catalytic converter. Haber-Bosch: 300 mB "
                           "of hydrogen and 100 mB of nitrogen make 200 mB of ammonia. Ostwald: 100 mB of ammonia, 200 "
                           "mB of oxygen and 100 mB of water make 200 mB of nitric acid. Each takes 2 seconds at 128 "
                           "JE/t.",
    "rocket_workshop": "Builds rockets from up to three ingredients in any slots, like the circuit assembler: solid "
                       "propellant from ammonium perchlorate, aluminum and rubber; casings, nozzles and guidance "
                       "units; motors; and the rockets themselves.",
    "logic_controller": "Runs your factory by rules. Join sensors and relays to it with data cable, then right-click "
                        "it: each of its eight rules reads \"IF channel below or above N% THEN channel ON or OFF\". "
                        "Every second it averages each channel's sensors and works through the rules in order, a later "
                        "rule overriding an earlier one; a channel no rule switches this time keeps its state, so two "
                        "rules (off above 90%, on below 50%) leave a dead band. The strip along the bottom shows every "
                        "channel's reading.",
    "network_terminal": "A beige retro computer. Cable it into a power network and right-click it: it shows the "
                        "network's cables, the rate its slowest cable sets, how many devices it reaches and the "
                        "energy they hold. It uses no power.",
    "lithography_station": "Three wide, two tall and two deep: a cleanroom and an operator's desk with a monitor bank. "
                           "A silicon wafer, two copper wire and 100 mB of sulfuric acid make four microchips, every "
                           "10 seconds at 192 JE/t. Pipe the acid into its tank.",
    "lithium_battery_bank": "Three wide, two tall, one deep: six lithium battery modules holding 32,000,000 JE, eight "
                            "capacitor banks. It charges from any side and gives power out of the sockets on its "
                            "front, 16,384 JE/t.",
    "flow_battery": "Three wide, three tall, two deep: a vanadium redox flow battery. It holds 1,000 JE for every mB "
                    "of vanadium electrolyte in its tank, so 64,000,000 JE with all 64 buckets in. Fill it by pipe or "
                    "bucket; the electrolyte stays in it (also when broken). It charges from any side and gives power "
                    "out of its front, 8,192 JE/t. Make the electrolyte in the chemical reactor from two asphalt "
                    "binder and a bucket of sulfuric acid.",
    "advanced_solar_panel": "A white pedestal carrying a 3x3 array of solar cells on the layer above it: 64 JE/t in full "
                            "sun (eight solar panels), half in rain, none at night. The cells need open sky. Cables meet "
                            "the pedestal's foot.",
    "advanced_engine": "Two blocks long, four cylinders. Burns gasoline (448 KE a mB) or diesel (320) piped into its "
                       "8-bucket tank and turns a shaft out of the back of its right-hand block at up to 1,024 KE/t, "
                       "burning only for what the line takes. Through a magnet dynamo it is the best use of either fuel. "
                       "Put a turbocharger in its slot and pipe water into its second tank: up to 1,536 KE/t, 10% more "
                       "from each mB of fuel, using 2 mB of water a tick.",
    "fuel_cell": "One block. Combines hydrogen with the air: 128 JE/t, burning a millibucket of hydrogen a tick (128,000 "
                 "JE a bucket). Pipe hydrogen from the electrolytic cell into it. Its screen lights while it runs.",
    "diesel_engine": "Two wide, two tall and three long. Burns diesel (256 KE a mB) or heavy fuel oil (128) piped into "
                     "its 8-bucket tank and turns a shaft out of the back of its upper right back block: up to "
                     "512 KE/t, twice the large steam engine. It burns only for the rotation the line takes.",
    "polymerization_reactor": "Two by two and three blocks tall. Polymerizes refinery gas into plastic: a bucket of gas "
                              "gives four plastic pellets, every 5 seconds. The metal press flattens each pellet "
                              "into a plastic sheet.",
    "hydrotreater": "Two by two and three tall, its catalyst built in. Pipe diesel into its first tank and hydrogen "
                    "into its second: 1,000 mB of diesel and 100 mB of hydrogen become 1,000 mB of premium diesel (base) "
                    "and 100 mB of hydrogen sulfide (top), every 6 seconds. Or blend 900 mB of gasoline with 100 mB "
                    "of bioethanol into a bucket of premium gasoline. 128 JE/t.",
    "heat_recovery_unit": "One block, two tall. Put it against a running diesel generator or gas turbine: it boils "
                          "water in their exhaust and makes 30% of their power again (77 or 154 JE/t). It needs water "
                          "(1 mB per 64 JE, piped in or from a water source right below it) and lubricant (1 mB every "
                          "2 seconds). Two units on one generator share its heat.",
    "gas_turbine": "Four wide, two tall and two deep. Burns gasoline or refinery gas from its 16-bucket tank: 512 JE/t, "
                   "384,000 JE a bucket of gasoline or 192,000 JE a bucket of gas. Its second tank takes lubricant "
                   "from the distillation tower: 1 mB every second of running, and it stops when it runs dry.",
    "deposit_drill": "Three blocks square and two tall. Build it on a surface deposit (coal, iron, copper or tin, on "
                     "stony hills): every 15 seconds it takes one coal or raw ore from each kind of deposit under it "
                     "or one block round it, down to 3 blocks deep, using 16 JE/t. It pushes what it mines out of every side "
                     "into a chest, item pipe, conveyor or machine beside it. Each deposit block holds 1,000 and then "
                     "turns to stone; right-click one to see how much is left. Picks only break deposits, for nothing.",
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
    "belt_pulley": "A shaft with a grooved wheel. Use a Drive Belt on two pulleys with the same axis (level with each "
                   "other along it, up to 16 blocks apart) and the second turns with the first: power jumps gaps and "
                   "walls.",
    "belt": "Links two belt pulleys: use it on one, then on the other. Breaking a pulley drops the belt.",
    "electric_motor": "Turns JE from cables back into rotation at 75%, up to 96 KE/t out of its shaft, which points "
                      "the way you looked when placing it. Motor and dynamo together always lose power.",
    "magnet_dynamo": "A dynamo wound round rare-earth magnets: 512 KE/t into JE at 95%, against the copper "
                     "dynamo's 128 at 75%. It pushes the JE into cables on every side.",
    "solar_tracker": "A solar panel on a motorised mount that follows the sun from east to west: 20 JE/t all day in "
                     "full sun (two and a half solar panels), half in rain, none at night. Cables take power from "
                     "any side.",
    "heliostat": "A mirror on a post that follows the sun to keep its light on a solar receiver above it. It does "
                 "nothing on its own.",
    "solar_receiver": "Put it on a tower over a field of heliostats: it counts those under open sky within 8 blocks "
                      "across and 16 below, and makes 12 JE/t for each (up to 48, 576 JE/t) in daylight, boiling a "
                      "mB of water for every 32 JE. Pipe water into it; cables take power from any side. Right-click "
                      "it to read its field.",
    "flywheel": "Stores rotation: a steel wheel that holds up to 2,000,000 KE. Shafts into any face but its front spin it "
                "up, 2,048 KE/t at most; its front shaft drives what it faces from the store at up to 2,048 KE/t. "
                "Friction takes a ten-thousandth of what it holds each tick, so it runs down when left alone. "
                "Right-click it to read how much it holds.",
    "magnet_motor": "An electric motor with rare-earth magnets: takes 1,024 JE/t and turns it into up to 384 KE/t at "
                    "95%. Paired with a magnet dynamo it still loses a tenth every round.",
    "dynamo": "Turns rotation reaching any face into JE at 75% and pushes it into cables on every side: the bridge "
              "from a shaft line to the electric network.",
    "auto_crafter": "Crafts the crafting recipe laid out in its 3x3 grid. Set the pattern by hand; each grid slot "
                    "keeps its last item as the pattern, so it crafts while every filled slot holds two or more. Pipes "
                    "and hoppers top up slots that already hold that item. Empty buckets and bottles go to the slot "
                    "above the output.",
    "engineers_handbook": "This book. Craft it from a book and a copper ingot.",
}

TAG_ITEMS = {"#c:silicon": "jugcraft:silicon", "#minecraft:planks": "minecraft:oak_planks", "#minecraft:logs": "minecraft:oak_log",
             "#minecraft:bamboo_blocks": "minecraft:bamboo_block", "#c:coal_coke": "jugcraft:coke", "#minecraft:coals": "minecraft:coal"}


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
    if form == "nuggets":
        return f"minecraft:{metal}_nugget" if metal in ("copper", "iron", "gold") else f"{MOD}:{metal}_nugget"
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
    if "recovery_percent" in stats:
        return f"Makes {stats['recovery_percent']}% of a touching generator's output. Holds {stats['capacity']:,} JE."
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


def form_page(form):
    """An industrial form's page (tools/industrial_forms.py): its text and construction recipe."""
    import industrial_forms
    info = industrial_forms.FORMS[form]
    pattern, key = info["recipe"]
    grid = [item_for(key[ch]) if ch != " " else None for row in pattern for ch in row.ljust(3)]
    return {"title": info["display"], "icon": f"{MOD}:{form}", "text": list(industrial_forms.HANDBOOK[form]),
            "craft": {"grid": grid, "result": f"{MOD}:{form}", "count": 1}}


def arms_pages():
    """Batch 42: the arms, three pages: swords, maces and hammers, polearms. Batch 45 (Arms II): two more."""
    import arms
    import gear

    def grid(kind):
        key = {"#": item_for(gear.GEAR_TIERS["steel"]["ingot"]), **{ch: item_for(ref) for ch, ref in arms.KEYS.items()}}
        return [key.get(ch) for row in arms.info(kind)["pattern"] for ch in row.ljust(3)]

    def craft(kind):
        return {"grid": grid(kind), "result": f"{MOD}:steel_{kind}", "count": 1}
    k = arms.KINDS
    return [
        {"title": "Arms: Swords", "icon": f"{MOD}:steel_longsword", "text": [
            "Bronze and steel make arms beyond the sword, each with its own way of fighting. All sweep like swords.",
            f"Longsword: a longer reach ({k['longsword']['reach'][1]} blocks). Hold use to parry: it blocks "
            f"{round(k['longsword']['parry'] * 100)}% of a blow from in front, as a shield does all of it.",
            f"Greatsword: two-handed, slow and heavy, with a {k['greatsword']['reach'][1]}-block reach; a hit stops a "
            f"shield blocking for {k['greatsword']['disable']:g} seconds.",
            f"Rapier: quick thrusts, two a second; its parry blocks {round(k['rapier']['parry'] * 100)}%."],
         "craft": craft("longsword")},
        {"title": "Arms: Maces and Hammers", "icon": f"{MOD}:steel_war_hammer", "text": [
            f"Flanged mace: a hit stops a shield blocking for {k['flanged_mace']['disable']:g} seconds.",
            f"War hammer: the heaviest blow of all, knocking foes back; a hit stops a shield blocking for "
            f"{k['war_hammer']['disable']:g} seconds, as an axe's does.",
            "Both take Sharpness, Smite, Bane of Arthropods, Knockback, Looting and Fire Aspect."],
         "craft": craft("war_hammer")},
        {"title": "Arms: Polearms", "icon": f"{MOD}:steel_halberd", "text": [
            f"Glaive: a blade on a pole that sweeps at {k['glaive']['reach'][1]} blocks.",
            f"Halberd: thrusts through every foe in line out to {k['halberd']['reach'][1]} blocks (not closer than "
            f"{k['halberd']['reach'][0]:g}); a hit stops a shield blocking for {k['halberd']['disable']:g} seconds.",
            "Spear: jab, or hold use to charge, as vanilla's spears do; faster on a horse or at a run.",
            f"Lance: a horseman's charge that hits harder and unhorses riders at lower speeds, reaching "
            f"{arms.LANCE_REACH[1]} blocks."],
         "craft": craft("halberd")},
        # Arms II (batch 45).
        {"title": "Arms: Blades of Skill", "icon": f"{MOD}:steel_dagger", "text": [
            f"Dagger: quick stabs at a {k['dagger']['reach'][1]}-block reach; a blow from behind deals "
            f"{round(arms.BACKSTAB * 100)}% more.",
            f"Sabre: quick sweeping cuts, and {arms.SADDLE:g} more damage from the saddle.",
            f"Estoc: thrusts through mail, {arms.ARMOR_PIERCE:g} more damage for each point of the foe's armor (at most "
            f"{arms.ARMOR_PIERCE_MAX:g}).",
            f"Quarterstaff: two-handed, knocks foes back; hold use to parry {round(k['quarterstaff']['parry'] * 100)}% of a "
            "blow from in front."],
         "craft": craft("dagger")},
        {"title": "Arms: Heavy and Long", "icon": f"{MOD}:steel_battle_axe", "text": [
            f"Battle axe: two-handed; chops wood like an axe and stops a shield blocking for "
            f"{k['battle_axe']['disable']:g} seconds.",
            f"Flail: a hit dazes the foe, slowing it for {arms.DAZE[0] // 20} seconds.",
            f"Scythe: wide sweeps at {k['scythe']['reach'][1]:g} blocks. Use it on a ripe crop to reap every ripe crop "
            "around it, 3 by 3, and replant them.",
            f"Pike: the longest reach, {k['pike']['reach'][1]:g} blocks, but nothing nearer than {k['pike']['reach'][0]:g}; "
            f"{round(arms.RIDERS * 100)}% more damage to riders and their mounts."],
         "craft": craft("scythe")},
        # Arms III (batch 46).
        {"title": "Arms: Two Hands", "icon": f"{MOD}:steel_greatsword", "text": [
            "Greatswords, war hammers, glaives, battle axes, scythes, quarterstaves, pikes and the two-handers swing "
            "with both hands: a click starts the swing, and the blow lands as it comes round, on every foe in its arc.",
            f"You slow to {round((1 - arms.TWO_HANDED_SLOW) * 100)}% of your speed while the swing is in the air. A click "
            "near its end follows straight on.",
            f"The last swing of each combo is the finishing blow, {round((arms.FINISHER - 1) * 100)}% stronger.",
            "There is no hand left for a shield: with one in the off hand, a two-handed arm will not swing."],
         "craft": craft("zweihander")},
        {"title": "Arms: The Two-Handers", "icon": f"{MOD}:steel_maul", "text": [
            f"Zweihander: the widest cleave, up to {arms.TWO_HANDED['zweihander']['targets']} foes; hold use to guard "
            f"against {round(k['zweihander']['parry'] * 100)}% of a blow from in front.",
            f"Maul: the heaviest blow; its finishing blow shakes the ground, striking foes within {arms.QUAKE_RADIUS:g} "
            "blocks and slowing them.",
            f"Executioner's sword: {round(arms.EXECUTE * 100)}% more damage to a foe at or below "
            f"{round(arms.EXECUTE_HEALTH * 100)}% of its health.",
            "Bill: a hooked polearm that pulls the foes it strikes towards you and drags riders from the saddle."],
         "craft": craft("maul")},
        # Arms IV (batch 47).
        {"title": "Arms: Masterworks", "icon": f"{MOD}:steel_labrys", "text": [
            "Two-handed, set with a garnet (bronze) or a lit phosphor stone (steel):",
            f"Labrys: a double-bitted axe whose finishing blow whirls right round, striking up to {arms.WHIRL_TARGETS} foes "
            "about you.",
            f"Battleblade: a saw-backed cleaver; each hit wears every piece of the foe's armor by {arms.SUNDER} more.",
            f"War fork: a barbed fork set against a charge: {round(arms.BRACE * 100)}% more damage to a foe coming at you."],
         "craft": craft("labrys")},
        {"title": "Arms: Kama and War Pick", "icon": f"{MOD}:steel_war_pick", "text": [
            "Kama: quick hooking cuts. Use it on grass, ferns, vines or leaves to cut all of them about it, 3 by 3 by 3, "
            "dropping what they drop.",
            "War pick: a beaked pick that fights and mines stone and ore as its metal's pickaxe does."],
         "craft": craft("war_pick")},
        # Arms V (batch 48): weapon arts.
        {"title": "Arms: Weapon Arts", "icon": f"{MOD}:steel_moonblade", "text": [
            "Six arms each have a weapon art: a special move of their own, used with the use key. Each plays its own "
            "move and strikes in its own way; then the arm needs a few seconds before its art is ready again (shown "
            "on the hotbar). Plain blows are not held back.",
            "An art's hits are the arm's own damage times a share, with its enchantments, and land in full even in "
            "quick succession. While busy with one you cannot swing.",
            "Against one foe an art does no better than plain blows: it pays against many, or to get somewhere."],
         "craft": craft("moonblade")},
        {"title": "Arms: Twinblade, Nodachi, Earthbreaker", "icon": f"{MOD}:steel_earthbreaker", "text": [
            "Two-handed, all three.",
            f"Twinblade, Cyclone: {arms.CYCLONE_HITS} spins, each cutting every foe within {arms.CYCLONE_RADIUS:g} blocks "
            "all round and drawing them in.",
            f"Nodachi, Iaido: a dash of about {arms.IAIDO_SPEED * arms.IAIDO_DASH:g} blocks; every foe you pass is cut a "
            "moment later, all at once.",
            f"Earthbreaker, Leap Slam: leap from the ground and slam where you land: hardest at the centre, and "
            f"{round(arms.LEAP_PER_BLOCK * 100)}% more for each block you came down below your take-off."],
         "craft": craft("earthbreaker")},
        {"title": "Arms: Katar, Moonblade, Kusarigama", "icon": f"{MOD}:steel_kusarigama", "text": [
            f"Katar, Flurry: {arms.FLURRY_JABS} quick jabs at the foe ahead, then a driving finish.",
            f"Moonblade (two-handed), Crescent: a wave that runs ahead about {arms.CRESCENT_SPEED * arms.CRESCENT_TICKS:g} "
            "blocks, through every foe in its way, weakening as it goes, until a wall stops it.",
            f"Kusarigama, Chain Lash: the chain catches the first foe in line up to {arms.LASH_RANGE:g} blocks off, hauls "
            "it in and the sickle reaps it as it comes."],
         "craft": craft("kusarigama")},
        # Arms VI (batch 55).
        {"title": "Arms: Katana and Brazier Mace", "icon": f"{MOD}:steel_katana", "text": [
            f"Katana, Seven Cuts: {arms.CUTS_COUNT} cuts in a breath, each across every foe ahead (up to "
            f"{arms.CUTS_TARGETS}), leaving arcs in the air: crimson from bronze, pale gold from steel.",
            f"Brazier mace: a burning brazier on a haft. A hit sets the foe alight for {arms.IGNITE_SECONDS} seconds; use "
            "it on a campfire, a candle or the ground to light it, as flint and steel does. Coal in its recipe."],
         "craft": craft("brazier_mace")},
        {"title": "Arms: Longbow and Arbalest", "icon": f"{MOD}:steel_longbow", "text": [
            f"Longbow: slower to draw than a bow ({arms.RANGED[('longbow', 'steel')]['draw'] / 20:g} s for a full draw), "
            "but its arrows fly faster and flatter, and hit harder for it.",
            "Arbalest: a crossbow with a metal prod, loaded as a crossbow is; its bolts fly faster and hit harder.",
            "Shot for shot they beat a bow and crossbow, but not a second for a second. Both take their vanilla "
            "counterparts' enchantments and shoot ordinary arrows and (the arbalest) fireworks."],
         "craft": craft("longbow")},
        {"title": "Arms: Shields", "icon": f"{MOD}:steel_tower_shield", "text": [
            "Heater shield: a light shield of planks and metal, quicker to raise than a shield.",
            f"Tower shield: a great board that covers {arms.SHIELDS[('tower_shield', 'steel')]['angle']:g} degrees either "
            "side of ahead (a shield: 90), wears less for each blow it stops and braces you against being knocked back, "
            f"but is slower to raise and slows you by {round(arms.SHIELDS[('tower_shield', 'steel')]['weight'] * 100)}% "
            "while held.",
            "Hold use to block with either, as with a shield. An axe's blow still knocks them down for a while."],
         "craft": craft("tower_shield")},
        # Arms VIII (batch 59).
        {"title": "Arms: Thrown Arms", "icon": f"{MOD}:steel_javelin", "text": [
            "A javelin, francisca, chakram or harpoon fights in the hand like any arm. Hold use to wind it back and let go "
            f"to throw it, as a trident is thrown; a steel javelin's throw hits for {arms.THROWN[('javelin', 'steel')]['damage']:g}, "
            "more with Sharpness and the like. It comes down where it struck, as itself, to be picked up again.",
            "Javelin: flies far and straight. Francisca: tumbles end over end and knocks a raised shield down for "
            f"{arms.FRANCISCA_DISABLE:g} seconds.",
            f"Chakram: flies flat, cuts every foe on its way out (up to {arms.CHAKRAM_RANGE:g} blocks) and back, and "
            "returns to your hand. Harpoon: keeps its speed underwater and hauls what it strikes towards you."],
         "craft": craft("javelin")},
    ] + variant_pages()


def variant_pages():
    """Arms VII (batch 56): the styles' patterns, the bosses' trophies and the armor sets' arms."""
    import arms_variants as av
    rows, key = av.STYLES["gilded"]["pattern_recipe"]
    grid = [item_for(key[ch]) if ch in key else None for row in rows for ch in row.ljust(3)]
    styles = ", ".join(info["display"].lower() for info in av.STYLES.values())
    return [
        {"title": "Arms: Styles", "icon": f"{MOD}:gilded_longsword", "text": [
            f"Four styles restyle a steel arm at a smithing table: {styles}. Put the style's pattern, the steel arm and "
            "the style's material in; the arm keeps its enchantments and wear.",
            f"Gilded (a gold ingot): longsword, rapier, sabre, halberd. Takes enchantments as gold does.",
            f"Ironclad (a steel plate): zweihander, maul, war pick, battle axe. Painted, plated and bolted; lasts twice as long.",
            f"Bonecarved (a bone block): dagger, flail, glaive, labrys. {round(av.GRAVEBANE * 100)}% harder against the undead.",
            "Runebound (ectoplasm): nodachi, moonblade, staff, war hammer. Its runes glow, and a foe it strikes glows "
            f"for {av.MARK_TICKS // 20} seconds, seen through walls.",
            "Each fights as its kind does: the same swing, reach, trait and art."],
         "craft": {"grid": grid, "result": f"{MOD}:gilders_pattern", "count": 1}},
        {"title": "Arms: Trophies", "icon": f"{MOD}:glacier_maul", "text": [
            "Great foes yet to be met in the world will each carry two arms of their own, with a boon:",
            ] + [f"{info['display'][0].upper()}{info['display'][1:]}: "
                 + " and ".join(av.BY_ID[name][3] for name in av.trophies(boss)) + "."
                 for boss, info in av.BOSSES.items()] + [
            "Trophies last twice as long as steel, and fight as their kinds do."]},
    ] + set_pages()


def set_pages():
    """The armor sets' arms and shields (tools/arms_variants.py SETS and SET_SHIELDS). None drops from a foe yet, so the
    page says so rather than where to win one."""
    import arms
    import arms_variants as av
    sets = [(info["display"], av.set_arms(armor_set) + av.set_shields(armor_set)) for armor_set, info in av.SETS.items()
            if av.set_arms(armor_set)]
    if not sets:
        return []

    def named(name):
        if name in av.SET_SHIELDS:
            info = av.SET_SHIELDS[name]
            return f"{info['display']} (a shield)"
        _kind, _line, boon, display = av.BY_ID[name]
        return f"{display} ({av.trait(av.BOONS[boon])[0]})" if boon else display
    shields = ", ".join(sorted({f"a {av.SET_SHIELD_METAL} {arms.SHIELD_KINDS[info['base']]['display'].lower()}"
                                for info in av.SET_SHIELDS.values()}))
    return [{"title": "Arms: Armor Sets", "icon": f"{MOD}:{sets[0][1][0]}", "text": [
        "Some armor sets have an arm of their own, made in the set's look, with a boon, and some a shield:",
        ] + [f"{display}: " + " and ".join(named(name) for name in names) + "." for display, names in sets] + [
        "How they are won is still to be settled; until then they are found only in creative. They last twice as long "
        f"as steel, and fight as their kinds do; a set's shield blocks as {shields} does."]}]


def thallite_gear_pages():
    """Thallite's tools and armor, and Earthbound armor with its Earthbinding Template (docs/features/thallite.md)."""
    import gear
    pickaxe = [item_for(gear.GEAR_TIERS["thallite"]["ingot"]) if ch == "#" else ("minecraft:stick" if ch == "S" else None)
               for row in gear.PATTERNS["pickaxe"] for ch in row.ljust(3)]
    style = gear.ARMOR_STYLES["earthbound_thallite"]
    rows, key = style["template_recipe"]
    template = [item_for(key[ch]) if ch in key else None for row in rows for ch in row.ljust(3)]
    return [
        {"title": "Thallite Gear", "icon": f"{MOD}:thallite_pickaxe", "text": [
            "Thallite makes swords, pickaxes, axes, shovels, hoes and armor, shaped like iron ones. They mine and "
            "protect as iron does, wear out a little sooner, and take enchantments best of all.",
            f"Regrowth: thallite gear you wear or hold mends while you stand on living soil (grass, dirt, moss, mud "
            f"or farmland): one use every {gear.REGROWTH_SECONDS} seconds, up to {gear.REGROWTH_CAP_PERCENT}% of "
            "full. It never brings back a broken piece, so mending and anvils still matter."],
         "craft": {"grid": pickaxe, "result": f"{MOD}:thallite_pickaxe", "count": 1}},
        {"title": "Earthbound Armor", "icon": f"{MOD}:earthbound_thallite_chestplate", "text": [
            "At a smithing table, an Earthbinding Template, a piece of thallite armor and a gold ingot bind the piece "
            "into Earthbound armor, trimmed in gold, for good. It keeps its enchantments and wear.",
            "Rooted: on natural ground (soil, stone, sand or gravel), each Earthbound piece takes 7.5% off knockback, "
            f"30% for a full set. With {gear.EARTHBOUND_FOR_STONE} or more worn, Regrowth works on stone, sand and "
            "gravel too."],
         "craft": {"grid": template, "result": f"{MOD}:{style['template']}", "count": style["template_count"]}},
    ]


def armor_style_pages():
    """Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md): bronze and steel armor in another look,
    smithed with a pattern."""
    import gear

    def grid(style):
        rows, key = gear.ARMOR_STYLES[style]["template_recipe"]
        return [item_for(key[ch]) if ch in key else None for row in rows for ch in row.ljust(3)]
    steampunk, kaiser = gear.ARMOR_STYLES["steampunk"], gear.ARMOR_STYLES["kaiser"]
    return [
        {"title": "Steampunk and Kaiser Armor", "icon": f"{MOD}:kaiser_helmet", "text": [
            "Steampunk armor is a bronze engineer's rig: an aviator cap with teal-glassed goggles, a breastplate with a "
            "pressure gauge, a copper boiler on the back, and buckled boots. Kaiser armor is the parade dress of the "
            "Winged Cog: a black spiked helmet with a gold plate, a field-grey tunic under a steel cuirass, gilt "
            "epaulettes and black jackboots.",
            "At a smithing table, a Steampunk Pattern, a piece of bronze armor and a copper ingot make the Steampunk "
            f"piece. A Kaiser Pattern, a piece of steel armor and a gold ingot make the Kaiser piece. Each pattern "
            f"craft makes {steampunk['template_count']}, one for each piece of a set. The Kaiser Pattern needs an "
            "Imperial Crest, made from black lacquer plates.",
            "Same protection as the bronze or steel piece, in its own look: bronze and steel armor are knight's plate. "
            "Enchantments, wear, trims and plating carry over. The same pattern and an ingot of the metal turn it "
            "back."],
         "craft": {"grid": grid("steampunk"), "result": f"{MOD}:{steampunk['template']}",
                   "count": steampunk["template_count"]}},
        {"title": "Kaiser Pattern", "icon": f"{MOD}:{kaiser['template']}", "text": [
            "The Kaiser Pattern takes an Imperial Crest, the Winged Cog of the Kaiserworks blocks: three black lacquer "
            "plates, four gold nuggets, a gold ingot and a red dye make two crests.",
            "Black lacquer plates come from iron plates and black dye."],
         "craft": {"grid": grid("kaiser"), "result": f"{MOD}:{kaiser['template']}", "count": kaiser["template_count"]}},
    ]


def gear_pages():
    """Batch 25: bronze and steel tools and armor, and paxels; batch 27 gear; batch 28 exosuit."""
    import exosuit
    import grapple
    import field_chemistry as fc
    import construction as cn
    import gear
    grid = [item_for(gear.GEAR_TIERS["steel"]["ingot"]) if ch == "#" else ("minecraft:stick" if ch == "S" else None)
            for row in gear.PATTERNS["pickaxe"] for ch in row.ljust(3)]
    return [
        {"title": "Bronze and Steel Gear", "icon": f"{MOD}:steel_pickaxe", "text": [
            "Bronze and steel make swords, pickaxes, axes, shovels, hoes and armor, shaped like iron ones.",
            "Bronze tools get the same drops as iron and last a little longer. Bronze armor matches iron's and is "
            "slightly tougher: knight's plate in copper-bronze, riveted in brass, with a brass collar.",
            "Steel tools mine obsidian and ancient debris, and last over three times as long as iron. Steel armor sits "
            "between iron and diamond: knight's plate with a crested helm, layered pauldrons and a skirt of plates.",
            "Steampunk and Kaiser armor keep the stylized looks bronze and steel armor were first made in, as sets of "
            "their own."],
         "craft": {"grid": grid, "result": f"{MOD}:steel_pickaxe", "count": 1}},
    ] + armor_style_pages() + thallite_gear_pages() + arms_pages() + [
        {"title": "Paxels", "icon": f"{MOD}:steel_paxel", "text": [
            "A paxel is a pickaxe, an axe and a shovel in one tool: it mines stone, wood and dirt at full speed.",
            "Craft one from a pickaxe, an axe and a shovel of the same tier, from wood to netherite, bronze or steel. "
            "It lasts as long as all three together."]},
        {"title": "Scuba Gear", "icon": f"{MOD}:scuba_tank", "text": [
            "Wear the scuba mask and the scuba tank together to breathe under water.",
            f"The tank holds {gear.SCUBA_OXYGEN:,} mB of oxygen. Use it on anything holding oxygen to fill it: a gas "
            "holder, or the oxygen tank of an electrolytic cell or air separation unit.",
            f"Under water it keeps your air full for {gear.SCUBA_OXYGEN_PER_TICK} mB a tick: a full tank lasts "
            f"{gear.SCUBA_OXYGEN // gear.SCUBA_OXYGEN_PER_TICK // 20} seconds of breathing."]},
        {"title": "Free Runners", "icon": f"{MOD}:free_runners", "text": [
            "Rubber-soled boots that take away all fall damage and step up a full block without jumping.",
            "They protect like iron boots and are mended with rubber."]},
        {"title": "Power Katana and Power Bow", "icon": f"{MOD}:power_katana", "text": [
            "Two weapons that run on JE instead of wearing out. Charge them at a charging station; capacity modules fit "
            "both.",
            "The power katana hits harder and faster than a netherite sword, for 1,000 JE a hit. Empty, it hits for 1.",
            "The power bow fires arrows of energy for 500 JE a shot: no arrows needed, and they fly faster and hit "
            "harder. Empty, it is an ordinary bow that shoots your arrows."]},
        {"title": "Powered Exosuit", "icon": f"{MOD}:exosuit_helmet", "text": [
            "Four pieces of armor as strong as netherite that run on JE. Each piece holds "
            f"{exosuit.CAPACITY:,} JE and charges at a charging station; capacity modules fit.",
            "Helmet: night vision whenever it is dark. Chestplate: an energy shield that regrows up to four hearts of "
            "absorption, and a jetpack (hold jump in the air). Leggings: 30% more speed. Boots: no fall damage and a "
            "full-block step.",
            "Each piece works only while it is charged; a flat piece is plain armor."]},
        {"title": "Liveries", "icon": f"{MOD}:ronin_livery", "text": [
            "The exosuit comes in Vanguard gunmetal. A Ronin livery at a smithing table, with a piece and red dye, "
            "repaints it crimson and silver, with the Ronin's hat and skirt; the power katana becomes the crimson Ronin "
            "katana.",
            "A Vanguard livery with cyan dye paints it back. Repainting keeps the charge, modules and enchantments."]},
        {"title": "Pneumatic Grapple", "icon": f"{MOD}:pneumatic_grapple", "text": [
            f"A harpoon gun on compressed nitrogen. It holds {grapple.CAPACITY:,} mB; fill it by using it on the air "
            f"separation unit's nitrogen or a gas holder. Each shot uses {grapple.SHOT_COST} mB.",
            f"The hook flies up to {grapple.RANGE} blocks on its line. In a block, it reels you in fast with no fall "
            "damage and lets go with a hop at the end, so you can climb cliffs and cross gaps. In a mob, it drags the "
            "mob to you; bosses and golems are too heavy.",
            "Use it again to let go. Party members are never hooked; other players only where PvP is on."]},
        {"title": "Chemical Grenades", "icon": f"{MOD}:chlorine_grenade", "text": [
            "Thrown by hand or fired from the grenade launcher like the frag grenade. None of them breaks or burns a block.",
            f"Chlorine: a {fc.CHLORINE_RADIUS:g}-block cloud for {fc.CHLORINE_TICKS // 20} s that hurts whatever breathes "
            "inside, through armor (not fish or the undead). Steel plate and nugget with 500 mB of chlorine in the "
            "chemical reactor.",
            f"Smoke: a {fc.SMOKE_RADIUS:g}-block screen for {fc.SMOKE_TICKS // 20} s. Mobs lose sight of anyone inside; "
            "players inside without a mask can't see. Steel plate and sugar with ammonia in the reactor.",
            f"Thermite: a white-hot pool for {fc.THERMITE_TICKS // 20} s that burns whatever stands in it, through armor. "
            "Thermite is an aluminum ingot and two iron dust; fill grenades like frag grenades.",
            "Flashbang: blinds players who see it and staggers mobs (they lose their target, slowed and weakened). "
            "No damage."]},
        {"title": "Gas Mask", "icon": f"{MOD}:gas_mask", "text": [
            "Keeps out chlorine and smoke; its tinted lenses keep out a flashbang.",
            f"The filter wears a point a second in gas or smoke ({11 * fc.GAS_MASK_DURABILITY} s in all): repair it "
            "with charcoal on an anvil.",
            f"The scuba mask and tank also keep gas out while the tank has oxygen ({fc.SCUBA_GAS_OXYGEN} mB a second)."]},
        {"title": "Foam Sprayer", "icon": f"{MOD}:foam_sprayer", "text": [
            f"Aim at a block up to {cn.SPRAY_RANGE} blocks away: foam fills the open space in front of it, up to "
            f"{cn.SPRAY_BLOCKS} blocks at a time, through air, water, lava and plants. It never replaces a solid block or "
            "fills a space a mob stands in.",
            f"Bridge gaps, seal caves, stop a flood or a lava flow. Each canister holds {cn.CANISTER_FOAM} blocks of foam; "
            "make canisters in the chemical reactor from plastic pellets and an iron nugget in ammonia.",
            "Foam breaks in a moment and drops nothing. Use cement on it to set it into concrete."]},
        {"title": "Concrete", "icon": f"{MOD}:blastproof_concrete", "text": [
            "Cement mix: calcite (or a bone block), clay and sand; smelt it into cement.",
            "Four cement, four gravel and a water bucket make eight concrete, as hard as stone and tougher.",
            "Eight concrete round a rebar make eight blast-proof concrete: as blast-proof as obsidian, mined with a "
            "diamond or steel pickaxe. Both come as slabs and stairs."]},
        {"title": "Medicines", "icon": f"{MOD}:first_aid_kit", "text": [
            "Made in the chemical reactor. First aid kit: two cotton and a soap in bioethanol. Heals four hearts, "
            f"then a {fc.FIRST_AID_COOLDOWN} s wait.",
            "Antidote: two charcoal and a bottle in lye. Clears poison, wither and every other harmful effect, and "
            "keeps the good ones (milk clears both).",
            f"Stimulant: four cocoa beans and a bottle in bioethanol. Speed II and Haste II for "
            f"{fc.STIMULANT_TICKS // 20} s, with hunger."]},
    ]


def biome_names(biomes):
    """'Lush Caves and the Glowcap Grotto' for a worldgen entry's biome ids (biome tags are left out)."""
    import biomes as bm
    names = []
    for biome in biomes:
        if biome.startswith("#"):
            continue
        namespace, path = biome.split(":")
        names.append(f"the {bm.BIOMES[path]['display']}" if namespace == MOD and path in bm.BIOMES
                     else " ".join(word.capitalize() for word in path.split("_")))
    return " and ".join(names) if len(names) < 3 else ", ".join(names[:-1]) + " and " + names[-1]


def ores_page():
    lines = []
    for metal, info in METALS.items():
        if info["mined"]:
            gen = info["gen"]
            line = f"{info['display']}: Y {gen['min_y']} to {gen['max_y']}, needs a {info['tool']} pickaxe."
            if "rich_gen" in info and info["rich_gen"].get("biomes"):
                line += f" Richer in {biome_names(info['rich_gen']['biomes'])}."
            lines.append(line)
    for mineral, info in MINERALS.items():
        gen = info["gen"]
        lines.append(f"{info['ore_display']}: Y {gen['min_y']} to {gen['max_y']}.")
    return {"title": "Ores", "icon": f"{MOD}:tin_ore",
            "text": ["Jugcraft ores appear in the stone and deepslate of every Overworld biome:"] + lines}


# The path through the mod, stage by stage (the "Progression" chapter). Each stage: title, icon, what it is for, a
# plan in a few lines, and the steps in order, each an item and a short label. Shown as a chain of numbered steps.
PROGRESSION = [
    ("Bronze Age", "bronze_ingot", "Get power running and double every ore.", [
        "Mine copper and tin and smelt bronze. Build a machine casing, a coal generator and copper cable.",
        "Put the crusher first: every ore through it gives two raw ores, twice the ingots.",
        "Bronze armor protects like iron; a Steampunk Pattern at a smithing table makes it Steampunk armor (see "
        "Steampunk and Kaiser Armor)."], [
        ("tin_ingot", "Mine tin, copper"), ("bronze_ingot", "Smelt bronze"), ("machine_casing", "Machine casing"),
        ("coal_generator", "Coal generator"), ("copper_cable", "Copper cable"), ("electric_furnace", "Electric furnace"),
        ("crusher", "Crusher: 2x ore")]),
    ("Workshop", "basic_circuit", "Plates, wires and circuits: the parts every later machine needs.", [
        "Make plates in the metal press and wire in the wire drawer, then basic circuits.",
        "Store power in a battery box. The pulverizer and ore washer take ore to three ingots each.",
        "Find surface deposits with the prospector and put a deposit drill on them."], [
        ("metal_press", "Metal press"), ("wire_drawer", "Wire drawer"), ("basic_circuit", "Basic circuit"),
        ("battery_box", "Battery box"), ("alloy_smelter", "Alloy smelter"), ("pulverizer", "Pulverizer"),
        ("ore_washer", "Ore washer: 3x ore"), ("prospector", "Prospector"), ("deposit_drill", "Deposit drill")]),
    ("Rotation and Logistics", "iron_shaft", "Move power by shaft and items by pipe and belt.", [
        "A steam engine or water wheel turns shafts; machines run on rotation directly, or a dynamo makes power.",
        "Item pipes, extractors and conveyors carry ore from drills to furnaces without you."], [
        ("hand_crank", "Hand crank"), ("steam_engine", "Steam engine"), ("iron_shaft", "Shafts"),
        ("dynamo", "Dynamo"), ("water_wheel", "Water wheel"), ("brass_item_pipe", "Item pipes"),
        ("pneumatic_extractor", "Extractor"), ("conveyor", "Conveyors"), ("auto_crafter", "Auto-crafter")]),
    ("Steel", "steel_ingot", "Steel opens every heavy machine and the powered tools.", [
        "Bake coal into coke, then refine iron with coke into steel in the steel foundry.",
        "The arc furnace melts what furnaces cannot. Advanced circuits need steel.",
        "Charge a mining drill at the charging station."], [
        ("coke_oven", "Coke oven"), ("coke", "Coke"), ("steel_foundry", "Steel foundry"), ("steel_ingot", "Steel"),
        ("arc_furnace_controller", "Arc furnace"), ("advanced_circuit", "Advanced circuit"),
        ("charging_station", "Charging station"), ("mining_drill", "Mining drill")]),
    ("Oil", "crude_oil_bucket", "Crude oil becomes fuel, plastic and asphalt.", [
        "Prospect for a reservoir and pump it with a pumpjack. Steel pipes and tanks carry the oil.",
        "Distil it; burn diesel in a diesel generator. Crack heavy oil and reform naphtha in the cracker, make plastic, "
        "and run a gas turbine."], [
        ("prospector", "Find oil"), ("pumpjack", "Pumpjack"), ("steel_fluid_pipe", "Steel pipes"),
        ("distillation_tower", "Distillation tower"), ("diesel_generator", "Diesel generator"),
        ("catalytic_cracker", "Cracker"), ("plastic_sheet", "Plastic"), ("gas_turbine", "Gas turbine"),
        ("diesel_engine", "Diesel engine"), ("hydrotreater", "Premium fuels"), ("heat_recovery_unit", "Heat recovery")]),
    ("Chemistry", "electrolytic_cell", "Salt, sulfur and air become acids, metals and gases.", [
        "Make brine and split it in the electrolytic cell into chlorine, hydrogen and lye.",
        "The chemical reactor makes sulfuric acid, titanium sponge, lithium and rare earths.",
        "Store a lot of power in a lithium battery bank. Split air for nitrogen and make ammonia and nitric acid."], [
        ("brine_bucket", "Brine"), ("electrolytic_cell", "Electrolytic cell"), ("chemical_reactor", "Chemical reactor"),
        ("sulfuric_acid_bucket", "Sulfuric acid"), ("titanium_ingot", "Titanium"), ("neodymium_magnet", "Magnets"),
        ("lithium_battery_bank", "Lithium bank"), ("air_separation_unit", "Air separation"),
        ("synthesis_converter", "Ammonia, nitric acid")]),
    ("Electronics", "processor", "Silicon becomes chips and processors.", [
        "Pull silicon boules in the arc furnace, saw them into wafers and etch microchips in the lithography station.",
        "Four microchips and an advanced circuit make a processor, for the top machines."], [
        ("silicon_boule", "Boule: arc furnace"), ("silicon_wafer", "Wafers"), ("lithography_station", "Lithography"),
        ("microchip", "Microchips"), ("processor", "Processor"), ("network_terminal", "Network terminal")]),
    ("Late Game", "flow_battery", "Big, efficient power.", [
        "Magnet dynamos and motors lose almost nothing. Fit a turbocharger to the advanced engine and give it "
        "coolant water; a flywheel smooths out a bursty shaft line.",
        "Solar trackers follow the sun; a field of heliostats around a tower boils water at a solar receiver. A flow "
        "battery stores 64 million JE."], [
        ("magnet_dynamo", "Magnet dynamo"), ("advanced_engine", "Advanced engine"), ("turbocharger", "Turbocharger"),
        ("flywheel", "Flywheel"), ("advanced_solar_panel", "Advanced solar"), ("solar_tracker", "Solar tracker"),
        ("heliostat", "Heliostats"), ("solar_receiver", "Solar receiver"), ("flow_battery", "Flow battery")]),
    ("Special Materials", "borosilicate_glass", "Rubber, glass and weapons from the chemistry you already run.", [
        "Naphtha cracked to butadiene in the chemical reactor becomes rubber and gaskets, for sealed pipes and the "
        "turbocharger.",
        "Borax turns sand into borosilicate glass: glass tanks that join into big see-through stores, tank gauges "
        "and optical fibre for processors. Guncotton fills grenades."], [
        ("rubber", "Rubber"), ("gasket", "Gaskets"), ("borosilicate_glass", "Borosilicate glass"),
        ("glass_tank", "Glass tanks"), ("tank_gauge", "Tank gauge"), ("optical_fibre", "Optical fibre"),
        ("grenade_launcher", "Grenade launcher")]),
]


def progression_pages():
    """An overview of the stages, then one page per stage with its plan and the steps in order."""
    overview = {"title": "The Road Ahead", "icon": f"{MOD}:engineers_handbook", "text": [
        "Jugcraft builds in stages, each needing parts from the ones before. Pick a stage on the left to see its "
        "steps."],
        "steps": [{"item": f"{MOD}:{icon}", "label": title}
                  for title, icon, _, _, _ in PROGRESSION]}
    pages = [overview]
    for n, (title, icon, summary, plan, steps) in enumerate(PROGRESSION):
        pages.append({"title": f"{n + 1}. {title}", "icon": f"{MOD}:{icon}", "text": [summary] + plan,
                      "steps": [{"item": f"{MOD}:{item}", "label": label} for item, label in steps]})
    return pages


def build():
    chapters = [
        {"title": "Progression", "icon": f"{MOD}:engineers_handbook", "pages": progression_pages()},
        {"title": "Getting Started", "icon": f"{MOD}:engineers_handbook", "pages": [
            {"title": "Welcome, Engineer", "icon": f"{MOD}:engineers_handbook", "text": [
                "This handbook covers every Jugcraft machine: what it does, what it needs and how to build it.",
                "Pick a chapter on the left; it opens to show its pages. Scroll with the mouse wheel, or turn pages "
                "with the arrows or the arrow keys.",
                "New here? The Progression chapter shows the order to build things in.",
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
            machine_page("deposit_drill"),
            machine_page("ore_drill"),
            {"title": "Ore Processing", "icon": f"{MOD}:tin_dust", "text": [
                "Smelting an ore gives one ingot.",
                f"Crushing or pulverizing it first gives {ORE_PROCESSING_MULTIPLIER}.",
                f"Washing it, then pulverizing the washed ore, gives {ORE_WASHING_MULTIPLIER}.",
                f"Dissolving it in sulfuric acid in the chemical reactor gives {ORE_LEACHING_MULTIPLIER} washed ores: "
                f"{ORE_LEACHING_MULTIPLIER} ingots.",
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
                                       "wind_turbine", "battery_box", "advanced_solar_panel")]
            + [block_page(c, CABLES[c]["display"]) for c in CABLES]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("steam_engine",)]
            + [machine_page("large_steam_engine")]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("hand_crank", "iron_shaft", "brass_gearbox",
                                                                     "belt_pulley")]
            + [block_page("belt", TOOLS["belt"])]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("dynamo", "electric_motor", "magnet_dynamo",
                                                                     "magnet_motor", "flywheel")]
            + [block_page(b, KINETIC_BLOCKS[b]["display"]) for b in ("solar_tracker", "heliostat", "solar_receiver")]},
        {"title": "Processing", "icon": f"{MOD}:crusher", "pages":
            [machine_page(m) for m in ("electric_furnace", "crusher", "alloy_smelter", "metal_press", "wire_drawer",
                                       "circuit_assembler", "arc_furnace_controller", "auto_crafter")]},
        {"title": "Ore Processing", "icon": f"{MOD}:pulverizer", "pages":
            [machine_page(m) for m in ("pulverizer", "ore_washer", "sieve", "sawmill")]},
        {"title": "Steel", "icon": f"{MOD}:steel_ingot", "pages":
            [machine_page(m) for m in ("coke_oven", "steel_foundry")]
            + [block_page(b, TOOL_BLOCKS[b]["display"]) for b in TOOL_BLOCKS]
            + [block_page(t, POWERED_TOOLS[t]) for t in POWERED_TOOLS]
            + [block_page(m, UPGRADE_MODULES[m][0]) for m in UPGRADE_MODULES]
            + gear_pages()},
        {"title": "Fluids", "icon": f"{MOD}:fluid_tank", "pages":
            [block_page(p, PIPES[p]["display"]) for p in ("bronze_fluid_pipe", "steel_fluid_pipe", "fluid_valve",
                                                          "fluid_filter")]
            + [block_page(b, FLUID_BLOCKS[b]["display"]) for b in ("fluid_tank", "electric_pump", "heavy_pump")]
            + [{"title": "Joined Tanks and Gauges", "icon": f"{MOD}:tank_gauge", "text": [
                "Tinplate and glass tanks touching face to face join into one tank of one fluid, up to 64 of them. "
                "They fill from the bottom and drain from the top; pipes, buckets and comparators see the whole group.",
                "A glass tank (four borosilicate glass in a steel frame) shows the fluid inside it.",
                "Hang a tank gauge on the side of any tank or machine: its sight glass shows how full it is in "
                "eighths. Right-click it to read the fluid and amount; a comparator reads it too."]}]},
        {"title": "Oil", "icon": f"{MOD}:crude_oil_bucket", "pages": [
            {"title": "Crude Oil", "icon": f"{MOD}:crude_oil_bucket", "text": [
                "Crude oil lies in hidden reservoirs under some Overworld chunks. The prospector reports Oil (pumpable) "
                "and Shale oil (needs fracking) under the 3x3 chunks around you.",
                "It is a thick, slow fluid that never makes new sources, so every reservoir runs dry in the end.",
                "Pipes, pumps and tanks carry it like water; refineries turn it into fuels."]},
            machine_page("pumpjack"),
            machine_page("distillation_tower"),
            machine_page("catalytic_cracker"),
            machine_page("fracking_rig"),
            machine_page("flowback_treatment_unit"),
            {"title": "Fuel Values", "icon": f"{MOD}:diesel_bucket", "text": [
                "What a bucket is worth: diesel 256,000 JE (diesel generator) or KE (diesel engine); heavy fuel oil "
                "128,000; gasoline 384,000 and refinery gas 192,000 (gas turbine). Bioethanol from crops: 192,000 in "
                "the gas turbine, 256,000 KE in the advanced engine.",
                "Refined all the way, a bucket of crude oil gives about 525 mB of diesel, 293 mB of gasoline and 183 mB "
                "of gas: about 282,000 JE, for about 40,000 JE of pumping and refining.",
                "Oil never comes back: every reservoir runs dry."]},
            machine_page("diesel_generator"),
            machine_page("gas_turbine"),
            machine_page("polymerization_reactor"),
            machine_page("diesel_engine"),
            machine_page("advanced_engine"),
            machine_page("hydrotreater"),
            {"title": "Premium Fuels", "icon": f"{MOD}:premium_diesel_bucket", "text": [
                "Premium diesel burns a quarter better: 320,000 JE a bucket in the diesel generator, 320,000 KE in the "
                "diesel engine and 400,000 KE in the advanced engine.",
                "Premium gasoline: 448,000 JE a bucket in the gas turbine (gasoline and bioethanol apart give 364,800) "
                "and 512,000 KE in the advanced engine.",
                "Hydrotreating pays best with the electrolytic cell's spare hydrogen. Its hydrogen sulfide goes to the "
                "chemical reactor: 200 mB make a sulfur dust, so oil becomes a source of sulfur for the acids."]},
            machine_page("heat_recovery_unit"),
            {"title": "Asphalt", "icon": f"{MOD}:asphalt", "text": [
                "Eight gravel around an asphalt binder (heavy fuel oil boiled in the distillation tower) make eight "
                "asphalt.",
                "Walking on asphalt, its slabs or road line is 1.3 times as fast. Three asphalt make six slabs.",
                "Four asphalt and a yellow dye make four road line blocks; the dashed line points the way you face "
                "when you place it."],
             "craft": {"grid": ["minecraft:gravel"] * 4 + [f"{MOD}:asphalt_binder"] + ["minecraft:gravel"] * 4,
                       "result": f"{MOD}:asphalt", "count": 8}},
            {"title": "Cracking Catalyst", "icon": f"{MOD}:cracking_catalyst", "text": [
                "Bauxite (alumina) and sand (silica) with a nickel ingot make four. The catalytic cracker uses one for "
                "each bucket of heavy fuel oil it cracks or naphtha it reforms."], "craft": craft("cracking_catalyst")},
        ]},
        {"title": "Chemistry", "icon": f"{MOD}:brine_bucket", "pages": [
            {"title": "Industrial Chemistry", "icon": f"{MOD}:salt", "text": [
                "Salt, sulfur, phosphate and bauxite get their real uses here.",
                "Dissolve salt in water to make brine (chemical reactor), then split it in the electrolytic cell into "
                "chlorine, hydrogen and lye. Gases live only in tanks, pipes and gas cylinders."]},
            machine_page("electrolytic_cell"),
            form_page("electrolytic_separator"),
            machine_page("chemical_reactor"),
            machine_page("fuel_cell"),
            form_page("gas_burning_generator"),
            machine_page("electroplating_bath"),
            {"title": "Rubber", "icon": f"{MOD}:rubber", "text": [
                "Crack a bucket of naphtha in the chemical reactor: 500 mB of butadiene. The polymerization reactor "
                "turns 500 mB of butadiene into four synthetic rubber.",
                "Rubber and string make two belts. A steel plate faced with rubber cuts into four gaskets, and "
                "gasketed steel pipe comes four to a steel plate pair, without a bronze pipe."],
             "craft": craft("gasket")},
            {"title": "Glass Chemistry", "icon": f"{MOD}:borosilicate_glass", "text": [
                "Tincal, natural borax, crusts the sand of deserts and badlands; mine it for 1-3 borax.",
                "Two sand and a borax melt into two borosilicate glass in the alloy smelter; the wire drawer pulls "
                "each into four optical fibre, which can carry a processor's signals instead of gold.",
                "Iron and borax make ferroboron; a rare earth oxide with ferroboron gives two neodymium magnets, "
                "twice the old recipe."]},
            {"title": "Plastic Blocks", "icon": f"{MOD}:light_blue_plastic", "text": [
                "Eight plastic sheets around a dye make eight plastic blocks of that colour, in all sixteen dye "
                "colours.",
                "They are smooth, bright building blocks, as hard as concrete; mine them with a pickaxe."]},
            {"title": "Chlorine and Lye", "icon": f"{MOD}:pvc_resin", "text": [
                "PVC: the synthesis converter joins 250 mB of refinery gas and 250 mB of chlorine into 250 mB of vinyl "
                "chloride; the polymerization reactor turns 500 mB of it into four PVC resin, and the metal press "
                "makes two plastic sheets from each.",
                "Soap: boil two rotten flesh in 250 mB of lye in the chemical reactor for four bars. Use a bar to "
                "wash every status effect off, as milk does."]},
            {"title": "Nitrogen Chemistry", "icon": f"{MOD}:nitric_acid_bucket", "text": [
                "Air is four parts nitrogen to one of oxygen. The air separation unit splits it; hydrogen comes from "
                "the electrolytic cell.",
                "The synthesis converter joins hydrogen and nitrogen into ammonia, and burns ammonia in oxygen over "
                "water into nitric acid.",
                "Ammonia and phosphate make fertilizer (six for two phosphate, against four with sulfuric acid). "
                "Nitric acid etches microchips with half as much acid as sulfuric."]},
            machine_page("air_separation_unit"),
            machine_page("synthesis_converter"),
            machine_page("ammonia_chiller"),
            machine_page("cryogenic_liquefier"),
            {"title": "Gas Cylinders", "icon": f"{MOD}:gas_cylinder", "text": [
                "A gas cylinder carries 8 buckets of one gas: hydrogen to a far-off fuel cell, ammonia to a chiller, "
                "oxygen or nitrogen into the field.",
                "Use it on a tank, gas holder, pipe or machine to fill it from there; sneak and use it to empty it "
                "back. Its bar shows how full it is.",
                "Used in the air with a scuba tank or a pneumatic grapple in the other hand, it tops that up with "
                "oxygen or nitrogen."]},
            {"title": "Grenades", "icon": f"{MOD}:grenade", "text": [
                "Two cotton in 250 mB of nitric acid in the chemical reactor make two guncotton.",
                "Two steel plates, a guncotton and an iron nugget make four grenades. Throw one with right-click; it "
                "goes off where it hits.",
                "The blast hurts living things within 4 blocks, up to eight hearts at the centre, and walls shield "
                "from it. It never breaks a block.",
                "The grenade launcher fires grenades from your inventory much further."]},
            {"title": "Aluminum, the Real Way", "icon": f"{MOD}:alumina", "text": [
                "Digest a bauxite in 250 mB of lye in the chemical reactor: two alumina.",
                "Smelt two alumina with a coal coke anode in the electrolytic cell: two aluminum ingots, every 8 "
                "seconds.",
                "That is two ingots from each bauxite, twice what the arc furnace gets and far more than the blast "
                "furnace's nugget."]},
            {"title": "Titanium", "icon": f"{MOD}:titanium_ingot", "text": [
                "No furnace can smelt titanium. Chlorinate it instead: a raw titanium, a coal coke and 250 mB of "
                "chlorine in the chemical reactor make a titanium sponge.",
                "The arc furnace melts the sponge into a titanium ingot. Titanium frames the lithium battery bank."]},
            {"title": "Leaching", "icon": f"{MOD}:lithium_carbonate", "text": [
                "Dissolve ores in sulfuric acid in the chemical reactor: a lepidolite and 250 mB of acid give two "
                "lithium carbonate, a monazite two rare earth oxide.",
                "That is twice what the blast furnace gets.",
                "Two lithium carbonate, four aluminum plates and a copper wire make two lithium cells.",
                "The alloy smelter melts a rare earth oxide with an iron ingot into a neodymium magnet, for the "
                "magnet dynamo and magnet motor."]},
            machine_page("lithium_battery_bank"),
            machine_page("flow_battery"),
            {"title": "Fertilizer", "icon": f"{MOD}:fertilizer", "text": [
                "Two phosphate and 250 mB of sulfuric acid in the chemical reactor make four fertilizer.",
                "Use one on the ground or a crop: every crop in the 5x5 area around it (a block up or down too) gets "
                "two doses of bone meal. Grass and saplings are left alone."]},
        ]},
        {"title": "Electronics", "icon": f"{MOD}:silicon_wafer", "pages": [
            {"title": "From Sand to Silicon", "icon": f"{MOD}:silicon_boule", "text": [
                "The electronics tier turns silicon into chips. It has the cyan look: dark casings, cyan glass and "
                "screens, violet conduits.",
                "Pull a silicon boule from 4 silicon and a phosphate in the arc furnace, then saw it into 8 silicon "
                "wafers in the sawmill."]},
            machine_page("lithography_station"),
            {"title": "Processors", "icon": f"{MOD}:processor", "text": [
                "Four microchips, an advanced circuit and a gold ingot make a processor in the circuit assembler: the "
                "third circuit tier."]},
            block_page("network_terminal", ELECTRONICS_BLOCKS["network_terminal"]["display"]),
            {"title": "Control Networks", "icon": f"{MOD}:sensor", "text": [
                "Data cable (optical fibre in a plastic sheath) joins sensors, relays and a logic controller. It "
                "carries no power. Channels are the sixteen dye colours: use a dye on a sensor or relay to set its "
                "channel.",
                "A sensor goes on a tank, battery, machine or chest and reads how full it is: energy first, then "
                "fluids, then items. It gives a redstone signal like a comparator's and reports the exact percentage "
                "on its channel. Right-click it to see the reading.",
                "A relay gives a full redstone signal on every side while the controller has its channel on. Set a "
                "machine beside it to a redstone mode and the controller runs the machine."]},
            block_page("logic_controller", "Logic Controller"),
            {"title": "The Control Room", "icon": f"{MOD}:control_monitor", "text": [
                "Six control monitor panels in a wall, three wide and two tall, all facing the same way, form one "
                "screen. Run a data cable from any panel to a logic controller and it shows every channel: its "
                "colour, reading, a bar, a two-minute graph and whether the controller has it on.",
                "An alarm klaxon is switched like a relay: while its channel is on it lights up and sounds.",
                "A control remote flips one channel by hand. Use it on a logic controller to bind it, sneak and use it "
                "to pick the channel, and use it to switch. The controller's rules may switch the channel back when "
                "their condition next holds. It works within 256 blocks of the controller."]},
        ]},
        {"title": "Rocketry", "icon": f"{MOD}:survey_rocket", "pages": [
            {"title": "Rocket Fuel", "icon": f"{MOD}:solid_propellant", "text": [
                "Rockets burn solid propellant: ammonium perchlorate (the oxidizer), aluminum and a rubber binder.",
                "Ammonium perchlorate: a salt and 250 mB of ammonia in the chemical reactor make two. Iodine: eight "
                "dried kelp and 100 mB of sulfuric acid make one; with silver dust it makes silver iodide, which "
                "seeds clouds."]},
            machine_page("rocket_workshop"),
            {"title": "Rockets", "icon": f"{MOD}:rocket_motor", "text": [
                "A rocket motor is a casing, a nozzle and two solid propellant. Fire any rocket with right-click under "
                "open sky; it rises like a firework and does its work at the top.",
                "Survey rocket (motor, guidance unit, sensor): surveys ores and oil under 7x7 chunks. Cloud-seeding "
                "rocket (motor, two silver iodide): five minutes of rain. Clear-sky rocket (motor, two guncotton): "
                "five minutes of clear sky. Weather rockets share a two-minute cooldown.",
                "Signal flares (four from propellant, paper and red dye) burst red and tell players within 512 blocks "
                "where they went up. Illumination flares (with glowstone) make hostile mobs within 48 blocks glow for "
                "30 seconds."]},
            {"title": "Rocket Post", "icon": f"{MOD}:delivery_rocket", "text": [
                "A rocket pad sends its cargo to another pad up to 4096 blocks away in the same dimension. Sneak and "
                "use a flight plan on the pad to deliver to, then put the plan, a delivery rocket (motor, casing, "
                "guidance unit) and up to nine stacks of cargo in the sending pad.",
                "Press Launch, or give the pad a redstone pulse. It needs open sky. The flight takes three seconds "
                "plus a second for every 80 blocks; the cargo lands in the target pad's slots.",
                "If nobody is near the target, the rocket waits and lands as soon as that area is loaded again. "
                "Hoppers load a pad from the top and sides and unload it from the bottom."]},
            {"title": "Ziplines", "icon": f"{MOD}:line_rocket", "text": [
                "Place two zipline anchors up to 96 blocks apart with nothing solid between them. Stand within 4 "
                "blocks of one and use a line-throwing rocket while looking at the other: it strings a steel line.",
                "Use either anchor with an empty hand to ride the line to the other end, hanging below it. A steeper "
                "drop is faster. Sneak to let go early (you fall from there).",
                "Breaking either anchor takes the line down. Each anchor holds one line."]},
            {"title": "Rocket Launcher", "icon": f"{MOD}:rocket_launcher", "text": [
                "The rocket launcher fires a rocket from your inventory (the other hand first), straight and fast. One "
                "shot every two seconds.",
                "High-explosive rockets (four from two solid propellant, two guncotton and a rocket casing in the "
                "rocket workshop) burst hard where they hit: twelve hearts at the centre, falling off over 5 blocks.",
                "Homing rockets (with a guidance unit instead of the casing) lock on to the hostile mob nearest your "
                "crosshair within 48 blocks, if you can see it, and steer into it. Their blast is smaller.",
                "Like grenades, rockets hurt living things only: they never break, move or burn a block."]},
            {"title": "Liquid Fuels", "icon": f"{MOD}:lox_tank", "text": [
                "RP-1 kerosene: hydrocrack heavy fuel oil with hydrogen (in the water tank) over the catalyst in the "
                "catalytic cracker. A bucket and 200 mB of hydrogen give 800 mB of kerosene, drawn off with the "
                "naphtha. It is jet fuel too: 448 JE/mB in the gas turbine, 480 in the advanced engine.",
                "Liquid oxygen: the cryogenic liquefier condenses a bucket of oxygen into 250 mB.",
                "Fill a rocket casing with a bucket of each in the chemical reactor to make a kerosene tank and a "
                "liquid oxygen tank. With two nozzles they make three rocket motors in the rocket workshop, with no "
                "solid propellant."]},
            {"title": "Booster Rails", "icon": f"{MOD}:booster_rail", "text": [
                "A booster rail is a straight rail with rocket thrusters. Power it with redstone like a powered rail; "
                "unpowered it is an ordinary rail and does not brake.",
                "Load it with solid propellant (use it on the rail, or feed it from a hopper): each gives 8 boosts, "
                "up to 64 held.",
                "A cart that rolls on to a powered, loaded booster rail (or stands on it) is kicked to full speed and "
                "held there for 10 seconds, up slopes too, trailing flame. A cart standing still goes uphill, or away "
                "from a block at one end."]},
        ]},
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
            block_page("conveyor_slope", SLOPE_BLOCKS["conveyor_slope"]["display"]),
            block_page("brass_wrench", TOOLS["brass_wrench"]),
        ]},
        {"title": "Storage", "icon": f"{MOD}:item_crate", "pages":
            [block_page("item_crate", STORAGE_BLOCKS["item_crate"]["display"])]
            + [machine_page(m) for m in ("capacitor_bank", "steel_tank", "gas_holder")]},
        {"title": "Renewables", "icon": f"{MOD}:tree_farm", "pages":
            [machine_page(m) for m in ("water_wheel", "cobblestone_generator", "tree_farm")]},
        {"title": "Farming", "icon": f"{MOD}:crop_harvester", "pages": [
            machine_page("crop_harvester"),
            machine_page("hydroponic_bay"),
            block_page("sprinkler", FARMING_BLOCKS["sprinkler"]["display"]),
            {"title": "Cotton", "icon": f"{MOD}:cotton", "text": [
                "Sift coarse dirt in the sieve: now and then it turns up cotton seeds (and wheat seeds).",
                "Plant them on farmland like wheat. A ripe plant gives one to three cotton and more seeds; the harvester, "
                "sprinkler and fertilizer all work on it.",
                "One cotton spins into one string."]},
        ]},
        {"title": "Upgrades", "icon": f"{MOD}:speed_upgrade", "pages":
            [block_page(u, UPGRADES[u]) for u in UPGRADES] + [
            {"title": "Comparators", "icon": "minecraft:comparator", "text": [
                "A comparator next to a generator or battery shows how full it is.",
                "Next to a processing machine, it shows how full its slots are, like a chest."]}]},
    ]
    return {"chapters": chapters}
