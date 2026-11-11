"""The upper floor's room kit pieces (tools/room_kit.py): which room each belongs to, and each one's crafting recipe.

Every piece has a shape of its own: a recipe no other recipe in the mod matches, mirrored or not (the tower's tiers are
paid for in the blocks themselves, so each must be craftable). `clashes()` checks that against every shaped recipe the
generator writes. Ingredients are what the piece is made of: planks for the wooden furniture (oak, birch for the light
woods, dark oak for the walnut), wool and leather for the upholstery, plates, nuggets and wire for the metalwork, glass
panes and circuits for the screens, plastic sheet for the moulded things.
"""
from __future__ import annotations

# the pieces by room (as tools/tripillar_rooms_upper.py lays them out; "all": the wall screens, in several rooms)
ROOMS = {
    "simulators": ["acoustic_panel", "headset_rack", "instructor_station", "pilot_simulator", "waiting_bench"],
    "briefing": ["aisle_step", "av_credenza", "briefing_lectern", "briefing_sign", "ceiling_speaker", "digital_wall_clock",
                 "tier_riser", "training_chair", "training_desk"],
    "planning": ["area_chart", "binder_shelf", "chart_bin", "chart_table", "dispatch_counter", "mfp_copier", "notam_board",
                 "steel_filing_cabinet", "strip_board", "wx_kiosk", "zone_clocks"],
    "training": ["bag_cubbies", "build_table", "class_whiteboard", "classroom_poster", "controller_rack", "instructor_desk",
                 "parts_board", "study_carrel", "touch_display_cart", "training_table"],
    "lounge": ["bar_stool", "dining_chair", "dining_table", "drinks_fridge", "foosball_table", "fridge_freezer", "kitchen_island",
               "kitchen_run", "lounge_rug", "lounge_tv", "media_console", "vending_machine"],
    "lab": ["bench_stool", "cad_desk", "electronics_bench", "flight_case", "lab_pegboard", "lab_plotter", "lab_tool_cabinet",
            "lab_workbench", "lipo_cabinet", "parts_rack", "plan_chest", "printer_farm", "rolling_whiteboard", "task_chair",
            "test_cage", "thrust_test_stand"],
    "manager": ["bistro_table", "coat_stand", "display_plinth", "exec_chair", "exec_credenza", "exec_desk", "floor_lamp",
                "framed_certificate", "leather_sofa", "lounge_coffee_table", "office_bookcase", "visitor_chair", "waste_basket"],
    "meeting": ["boardroom_table", "logo_wall", "pendant_light", "refreshment_credenza", "vc_camera"],
    "restrooms": ["bottle_filler", "chemical_dispenser", "floor_scrubber", "janitor_cart", "mop_rack", "mop_sink", "sanitary_bin",
                  "supply_shelf", "vanity_counter", "vanity_mirror", "water_heater", "wet_floor_sign"],
    "first_aid": ["column_scale", "crash_cart", "crew_bunk", "crew_coffee_table", "crew_fridge", "crew_locker", "crew_side_table",
                  "crew_sofa", "diagnostic_set", "dressing_trolley", "exam_couch", "eyewash_bottles", "first_aid_box", "iv_pole",
                  "kitchenette", "med_desk", "med_fridge", "med_poster", "medicine_cupboard", "oxygen_trolley", "pedal_bin",
                  "privacy_screen", "recliner", "scrub_sink", "spine_board", "step_stool", "wall_tv", "water_dispenser",
                  "wheelchair"],
    "offices": ["mail_sorter", "office_bench", "office_bench_single", "office_noticeboard", "phone_booth", "recycling_station",
                "storage_cupboard", "team_table", "wall_whiteboard", "year_planner"],
    "all": ["ops_wall_screen", "ops_wall_screen_large"],
    # the control building's roof plant (tools/roof_plant.py, tools/roof_plant_layout.py)
    "roof": ["comms_dish", "comms_mast", "comms_mast_base", "comms_mast_tie", "comms_mast_top", "dry_cooler", "duct_roof_curb",
             "kitchen_exhaust_fan", "pipe_crossover", "pipe_doghouse", "rf_cabinet", "roof_exhaust_fan", "roof_pipe_support",
             "roof_upblast_fan", "rooftop_ahu"],
    # the Advanced Crafting Table (tools/adv_crafting_table.py): its 6 x 6 grid makes the drones and the advanced kit
    "workshop": ["advanced_crafting_table"],
    # dispatch (work list item 20, tools/goods_lift.py): the goods lift up to the roof's pickup, the exchanges' plates
    "dispatch": ["cargo_case_packer", "cargo_infeed", "cargo_port_plate", "crate_pallet", "energy_port_plate", "goods_lift_door",
                 "lift_rail"],
    # the hall: the tower diorama's showroom case (tools/diorama_case.py)
    "hall": ["tower_diorama_case"],
}
PIECES = {piece: room for room, pieces in ROOMS.items() for piece in pieces}
# the pieces that give light (tower/RoomKit: a piece's light level, every state)
LIGHT = {"floor_lamp": 12, "pendant_light": 14, "ops_wall_screen": 3, "ops_wall_screen_large": 3, "lounge_tv": 3,
         "tower_diorama_case": 6}

# Retired pieces: kept as blocks (so worlds that have one load), but with no recipe and not in the creative menu; any
# placed turns into what's named (tower/RetiredBlocks). The Lounge TV (Justin, 10 November 2026: "I don't want to use
# that shitty TV at all, anywhere. It should be actual TVs"): a Flat Screen TV in its place, two high where it can be.
RETIRED = {"lounge_tv": "flat_tv"}

# ingredients
IRON, STEEL, ALU, WIRE = "#c:plates/iron", "#c:plates/steel", "#c:plates/aluminum", "#c:wires/copper"
NUG, GNUG, CHAIN = "minecraft:iron_nugget", "minecraft:gold_nugget", "minecraft:iron_chain"
CIRC, ADV, PLAS = "jugcraft:basic_circuit", "jugcraft:advanced_circuit", "jugcraft:plastic_sheet"
PANE, DARK_PANE = "minecraft:glass_pane", "minecraft:black_stained_glass_pane"
OAK, BIRCH, SPRUCE, WALNUT = "minecraft:oak_planks", "minecraft:birch_planks", "minecraft:spruce_planks", "minecraft:dark_oak_planks"
PAPER, BOOK, MAP, CLOCK = "minecraft:paper", "minecraft:book", "minecraft:map", "minecraft:clock"
LEATHER, STRING, STICK = "minecraft:leather", "minecraft:string", "minecraft:stick"
GLOW, REDSTONE, QUARTZ = "minecraft:glowstone_dust", "minecraft:redstone", "minecraft:quartz"
BOTTLE, BUCKET, WATER, ICE = "minecraft:glass_bottle", "minecraft:bucket", "minecraft:water_bucket", "minecraft:ice"
CHEST, BARREL, FURNACE, CAULDRON = "minecraft:chest", "minecraft:barrel", "minecraft:furnace", "minecraft:cauldron"
MOTOR, RUBBER, BARS, COPPER = "jugcraft:electric_motor", "jugcraft:rubber", "minecraft:iron_bars", "minecraft:copper_ingot"


def wool(c):
    return f"minecraft:{c}_wool"


def dye(c):
    return f"minecraft:{c}_dye"


CRAFTING = {
    # room 9, the simulators
    "acoustic_panel": (["WKW", "WKW"], {"W": wool("black"), "K": PLAS}, 4),
    "headset_rack": (["LCL", "PPP"], {"L": LEATHER, "C": CIRC, "P": IRON}, 1),
    "instructor_station": (["BBB", "CCC", "SSS"], {"B": DARK_PANE, "C": CIRC, "S": STEEL}, 1),
    "pilot_simulator": (["BBB", "AWA", "SPS"], {"B": DARK_PANE, "A": ADV, "W": wool("black"), "S": STEEL, "P": "minecraft:piston"}, 1),
    "waiting_bench": (["WWW", "S S"], {"W": wool("gray"), "S": STEEL}, 1),
    # room 1, the briefing room
    "aisle_step": (["CCC", "PYP"], {"C": "minecraft:gray_carpet", "P": ALU, "Y": dye("orange")}, 4),
    "av_credenza": (["DDD", "CBC", "DDD"], {"D": WALNUT, "C": CIRC, "B": DARK_PANE}, 1),
    "briefing_lectern": (["DCD", " D ", "DPD"], {"D": WALNUT, "C": CIRC, "P": IRON}, 1),
    "briefing_sign": (["BLB", "ACA"], {"B": DARK_PANE, "L": GLOW, "A": ALU, "C": CIRC}, 1),
    "ceiling_speaker": (["PMP", " W "], {"P": IRON, "M": "minecraft:note_block", "W": WIRE}, 2),
    "digital_wall_clock": (["BBB", "KCK"], {"B": DARK_PANE, "K": CLOCK, "C": CIRC}, 1),
    "tier_riser": (["CCC", "OOO", "OOO"], {"C": "minecraft:gray_carpet", "O": OAK}, 6),
    "training_chair": (["K  ", "KKK", "P P"], {"K": PLAS, "P": IRON}, 2),
    "training_desk": (["HHH", "P P"], {"H": BIRCH, "P": IRON}, 1),
    # room 5, flight planning
    "area_chart": (["AMA", "AMA"], {"A": ALU, "M": MAP}, 1),
    "binder_shelf": (["OBO", "OBO"], {"O": OAK, "B": BOOK}, 1),
    "chart_bin": (["P P", "PXP"], {"P": IRON, "X": PAPER}, 2),
    "chart_table": (["GGG", "OMO", "O O"], {"G": PANE, "O": OAK, "M": MAP}, 1),
    "dispatch_counter": (["BBB", "HCH", "HHH"], {"B": DARK_PANE, "H": BIRCH, "C": CIRC}, 1),
    "mfp_copier": (["GCG", "PXP", "PPP"], {"G": PANE, "C": CIRC, "P": IRON, "X": PAPER}, 1),
    "notam_board": (["SXS", "SXS"], {"S": SPRUCE, "X": PAPER}, 1),
    "steel_filing_cabinet": (["SCS", "SCS"], {"S": STEEL, "C": CHEST}, 1),
    "strip_board": (["AXA", "AXA", "AXA"], {"A": ALU, "X": PAPER}, 1),
    "wx_kiosk": (["BB", "CD", "SS"], {"B": DARK_PANE, "C": CIRC, "D": "minecraft:daylight_detector", "S": STEEL}, 1),
    "zone_clocks": (["KKK", "PPP"], {"K": CLOCK, "P": IRON}, 1),
    # room 10, the training room
    "bag_cubbies": (["OSO", "OSO"], {"O": OAK, "S": STEEL}, 1),
    "build_table": (["HHH", "SWS", "S S"], {"H": BIRCH, "S": STEEL, "W": WIRE}, 1),
    "class_whiteboard": (["AQA", "AQA", "AQA"], {"A": ALU, "Q": QUARTZ}, 1),
    "classroom_poster": (["XRX", "XBX"], {"X": PAPER, "R": dye("red"), "B": dye("blue")}, 2),
    "controller_rack": (["PCP", "PCP"], {"P": IRON, "C": CIRC}, 1),
    "instructor_desk": (["DCD", "D D"], {"D": WALNUT, "C": CIRC}, 1),
    "parts_board": (["KKK", "OOO", "KKK"], {"K": PLAS, "O": OAK}, 1),
    "study_carrel": (["HGH", "H H"], {"H": BIRCH, "G": PANE}, 1),
    "touch_display_cart": (["BBB", "BCB", "NSN"], {"B": DARK_PANE, "C": CIRC, "S": STEEL, "N": NUG}, 1),
    "training_table": (["HHH", "S S"], {"H": BIRCH, "S": STEEL}, 2),
    # room 7, the crew lounge
    "bar_stool": (["L", "S", "S"], {"L": LEATHER, "S": STEEL}, 2),
    "dining_chair": (["O  ", "OWO", "O O"], {"O": OAK, "W": wool("white")}, 2),
    "dining_table": (["OOO", "N N"], {"O": OAK, "N": NUG}, 1),
    "drinks_fridge": (["SGS", "SIS", "SRS"], {"S": STEEL, "G": PANE, "I": ICE, "R": REDSTONE}, 1),
    "foosball_table": (["OGO", "TKT", "O O"], {"O": OAK, "G": wool("green"), "T": STICK, "K": PLAS}, 1),
    "fridge_freezer": (["SIS", "SRS", "SIS"], {"S": STEEL, "I": ICE, "R": REDSTONE}, 1),
    "kitchen_island": (["QQQ", "OBO"], {"Q": "minecraft:quartz_slab", "O": OAK, "B": BARREL}, 1),
    "kitchen_run": (["QQQ", "OFO", "OCO"], {"Q": "minecraft:quartz_slab", "O": OAK, "F": FURNACE, "C": CAULDRON}, 1),
    "lounge_rug": (["WWW", "GGG", "WWW"], {"W": "minecraft:gray_carpet", "G": "minecraft:light_gray_carpet"}, 1),
    "media_console": (["BBB", "DCD"], {"B": DARK_PANE, "D": WALNUT, "C": CIRC}, 1),
    # (lounge_tv: retired, RETIRED below; its recipe was ["BBB", "BAB", " P "], B dark pane, A advanced circuit, P iron)
    "vending_machine": (["SGS", "SCS", "SES"], {"S": STEEL, "G": PANE, "C": CIRC, "E": "minecraft:emerald"}, 1),
    # room 3, the engineering lab
    "bench_stool": (["K", "S"], {"K": PLAS, "S": STEEL}, 2),
    "cad_desk": (["BCB", "HHH", "S S"], {"B": DARK_PANE, "C": CIRC, "H": BIRCH, "S": STEEL}, 1),
    "electronics_bench": (["WCW", "SSS", "S S"], {"W": WIRE, "C": CIRC, "S": STEEL}, 1),
    "flight_case": (["AKA", "AKA"], {"A": ALU, "K": PLAS}, 1),
    "lab_pegboard": (["ONO", "NON", "ONO"], {"O": OAK, "N": NUG}, 2),
    "lab_plotter": (["XXX", "PCP", "P P"], {"X": PAPER, "P": IRON, "C": CIRC}, 1),
    "lab_tool_cabinet": (["SRS", "SIS", "SRS"], {"S": STEEL, "R": dye("red"), "I": "#c:ingots/iron"}, 1),
    "lab_workbench": (["HHH", "SNS", "S S"], {"H": BIRCH, "N": NUG, "S": STEEL}, 1),
    "lipo_cabinet": (["YSY", "S S", "YSY"], {"Y": dye("yellow"), "S": STEEL}, 1),
    "parts_rack": (["SKS", "SKS", "SKS"], {"S": STEEL, "K": PLAS}, 1),
    "plan_chest": (["SSS", "XXX", "SSS"], {"S": STEEL, "X": PAPER}, 1),
    "printer_farm": (["GGG", "RCR", "SSS"], {"G": PANE, "R": REDSTONE, "C": CIRC, "S": STEEL}, 1),
    "rolling_whiteboard": (["AQA", "AQA", "N N"], {"A": ALU, "Q": QUARTZ, "N": NUG}, 1),
    "task_chair": (["  W", "WWW", "KNK"], {"W": wool("black"), "K": PLAS, "N": NUG}, 1),
    "test_cage": (["STS", "T T", "STS"], {"S": STEEL, "T": STRING}, 1),
    "thrust_test_stand": (["ACA", "A A"], {"A": ALU, "C": CIRC}, 1),
    # room 2, the manager's office
    "bistro_table": (["DDD", " N ", " S "], {"D": WALNUT, "N": NUG, "S": STEEL}, 1),
    "coat_stand": (["DND", " D ", " D "], {"D": WALNUT, "N": NUG}, 1),
    "display_plinth": (["GGG", "DDD", "DDD"], {"G": PANE, "D": WALNUT}, 1),
    "exec_chair": (["  L", "LLL", "SNS"], {"L": LEATHER, "S": STEEL, "N": NUG}, 1),
    "exec_credenza": (["DDD", "DCD", "DDD"], {"D": WALNUT, "C": CHEST}, 1),
    "exec_desk": (["DDD", "DLD", "D D"], {"D": WALNUT, "L": LEATHER}, 1),
    "floor_lamp": (["WGW", " S ", "SSS"], {"W": wool("white"), "G": GLOW, "S": STEEL}, 1),
    "framed_certificate": (["NXN"], {"N": GNUG, "X": PAPER}, 1),
    "leather_sofa": (["L L", "LLL", "DDD"], {"L": LEATHER, "D": WALNUT}, 1),
    "lounge_coffee_table": (["GGG", "D D"], {"G": PANE, "D": WALNUT}, 1),
    "office_bookcase": (["DBD", "DBD", "DBD"], {"D": WALNUT, "B": BOOK}, 1),
    "visitor_chair": (["L  ", "LLL", "N N"], {"L": LEATHER, "N": NUG}, 2),
    "waste_basket": (["S S", " S "], {"S": STEEL}, 2),
    # room 11, the meeting room
    "boardroom_table": (["DDD", "DWD", "S S"], {"D": WALNUT, "W": WIRE, "S": STEEL}, 1),
    "logo_wall": (["DGD", "DLD"], {"D": WALNUT, "G": PANE, "L": GLOW}, 1),
    "pendant_light": (["N", "A", "L"], {"N": CHAIN, "A": ALU, "L": GLOW}, 2),
    "refreshment_credenza": (["BBB", "DDD"], {"B": BOTTLE, "D": WALNUT}, 1),
    "vc_camera": (["KSK", " C "], {"K": PLAS, "S": "minecraft:spyglass", "C": CIRC}, 1),
    # room 12, the restrooms and the custodian's closet
    "bottle_filler": (["SGS", "SBS", "SWS"], {"S": STEEL, "G": PANE, "B": BOTTLE, "W": WATER}, 1),
    "chemical_dispenser": (["BBB", "KCK"], {"B": BOTTLE, "K": PLAS, "C": CIRC}, 1),
    "floor_scrubber": (["KCK", "PSP"], {"K": PLAS, "C": CIRC, "P": STEEL, "S": "minecraft:sponge"}, 1),
    "janitor_cart": (["YKY", "KBK", "N N"], {"Y": dye("yellow"), "K": PLAS, "B": BUCKET, "N": NUG}, 1),
    "mop_rack": (["TTT", "NSN"], {"T": STICK, "N": NUG, "S": STEEL}, 1),
    "mop_sink": (["Q Q", "QQQ"], {"Q": "minecraft:quartz_block"}, 1),
    "sanitary_bin": (["KNK", "KKK"], {"K": PLAS, "N": NUG}, 2),
    "supply_shelf": (["N N", "SSS", "N N"], {"N": NUG, "S": STEEL}, 1),
    "vanity_counter": (["QCQ", "Q Q"], {"Q": "minecraft:smooth_quartz", "C": CAULDRON}, 1),
    "vanity_mirror": (["GGG", "NNN"], {"G": PANE, "N": NUG}, 1),
    "water_heater": (["SWS", "SFS", "SBS"], {"S": STEEL, "W": WIRE, "F": FURNACE, "B": BUCKET}, 1),
    "wet_floor_sign": (["YKY", "K K"], {"Y": dye("yellow"), "K": PLAS}, 2),
    # room 4, first aid and crew rest
    "column_scale": (["NSN", " S ", "SPS"], {"N": NUG, "S": STEEL, "P": "minecraft:heavy_weighted_pressure_plate"}, 1),
    "crash_cart": (["SRS", "SCS", "N N"], {"S": STEEL, "R": dye("red"), "C": CHEST, "N": NUG}, 1),
    "crew_bunk": (["WWW", "S S", "WWW"], {"W": wool("white"), "S": STEEL}, 1),
    "crew_coffee_table": (["HHH", "N N"], {"H": BIRCH, "N": NUG}, 1),
    "crew_fridge": (["SIS", "SRS"], {"S": STEEL, "I": ICE, "R": REDSTONE}, 1),
    "crew_locker": (["SCS", "S S", "SCS"], {"S": STEEL, "C": CHEST}, 1),
    "crew_side_table": (["HHH", "H H"], {"H": BIRCH}, 2),
    "crew_sofa": (["W W", "WWW", "OOO"], {"W": wool("gray"), "O": OAK}, 1),
    "diagnostic_set": (["BCB", "KWK"], {"B": DARK_PANE, "C": CIRC, "K": PLAS, "W": WIRE}, 1),
    "dressing_trolley": (["SSS", "SWS", "N N"], {"S": STEEL, "W": wool("white"), "N": NUG}, 1),
    "exam_couch": (["LLL", "SSS"], {"L": LEATHER, "S": STEEL}, 1),
    "eyewash_bottles": (["BBB", "KPK"], {"B": BOTTLE, "K": PLAS, "P": IRON}, 1),
    "first_aid_box": (["KRK", "KWK"], {"K": PLAS, "R": dye("red"), "W": wool("white")}, 1),
    "iv_pole": (["NBN", " S ", "NSN"], {"N": NUG, "B": BOTTLE, "S": STEEL}, 1),
    "kitchenette": (["QQQ", "HCH"], {"Q": "minecraft:quartz_slab", "H": BIRCH, "C": CAULDRON}, 1),
    "med_desk": (["HXH", "H H"], {"H": BIRCH, "X": PAPER}, 1),
    "med_fridge": (["SGS", "RIR", "SSS"], {"S": STEEL, "G": PANE, "R": dye("red"), "I": ICE}, 1),
    "med_poster": (["XGX", "XRX"], {"X": PAPER, "G": dye("green"), "R": dye("red")}, 2),
    "medicine_cupboard": (["HGH", "HBH"], {"H": BIRCH, "G": PANE, "B": BOTTLE}, 1),
    "oxygen_trolley": (["SGS", "SGS", "N N"], {"S": STEEL, "G": dye("green"), "N": NUG}, 1),
    "pedal_bin": (["SNS", "S S", "SSS"], {"S": STEEL, "N": NUG}, 2),
    "privacy_screen": (["AWA", "AWA", "N N"], {"A": ALU, "W": wool("white"), "N": NUG}, 1),
    "recliner": (["L  ", "LLL", "LSL"], {"L": LEATHER, "S": STEEL}, 1),
    "scrub_sink": (["SWS", "SSS"], {"S": STEEL, "W": WATER}, 1),
    "spine_board": (["KYK", "KKK", "KYK"], {"K": PLAS, "Y": dye("yellow")}, 1),
    "step_stool": (["SSS", "K K"], {"S": STEEL, "K": PLAS}, 2),
    "wall_tv": (["BBB", "BCB", " P "], {"B": DARK_PANE, "C": CIRC, "P": IRON}, 1),
    "water_dispenser": (["GWG", "SNS"], {"G": PANE, "W": WATER, "S": STEEL, "N": NUG}, 1),
    "wheelchair": (["S  ", "SLS", "NSN"], {"S": STEEL, "L": LEATHER, "N": NUG}, 1),
    # rooms 6 and 8, the offices
    "mail_sorter": (["SXS", "SXS", "SSS"], {"S": STEEL, "X": PAPER}, 1),
    "office_bench": (["HHH", "HHH", "S S"], {"H": BIRCH, "S": STEEL}, 1),
    "office_bench_single": (["HKH", "S S"], {"H": BIRCH, "K": PLAS, "S": STEEL}, 1),
    "office_noticeboard": (["OXO", "OXO", "OXO"], {"O": OAK, "X": PAPER}, 1),
    "phone_booth": (["AGA", "WGW", "AWA"], {"A": ALU, "G": PANE, "W": wool("gray")}, 1),
    "recycling_station": (["BGY", "KKK", "KKK"], {"B": dye("blue"), "G": dye("green"), "Y": dye("yellow"), "K": PLAS}, 1),
    "storage_cupboard": (["HHH", "HCH", "HHH"], {"H": BIRCH, "C": CHEST}, 1),
    "team_table": (["OOO", "OOO", "S S"], {"O": OAK, "S": STEEL}, 1),
    "wall_whiteboard": (["AQA", "AQA"], {"A": ALU, "Q": QUARTZ}, 1),
    "year_planner": (["XXX", "XKX"], {"X": PAPER, "K": PLAS}, 1),
    # the wall screens (the briefing room, the training room, flight planning, the lounge...)
    "ops_wall_screen": (["BBB", "BCB", "BBB"], {"B": DARK_PANE, "C": CIRC}, 1),
    "ops_wall_screen_large": (["BBB", "BAB", "BBB"], {"B": DARK_PANE, "A": ADV}, 1),
    # the roof plant
    "rooftop_ahu": (["AAA", "MWM", "SCS"], {"A": ALU, "M": MOTOR, "W": wool("white"), "S": STEEL, "C": CIRC}, 1),
    "dry_cooler": (["MMM", "KAK", "S S"], {"M": MOTOR, "K": COPPER, "A": ALU, "S": STEEL}, 1),
    "duct_roof_curb": (["I I", "IAI"], {"I": IRON, "A": ALU}, 2),
    "roof_exhaust_fan": (["AAA", "AMA", "I I"], {"A": ALU, "M": MOTOR, "I": IRON}, 1),
    "roof_upblast_fan": (["A A", "AMA", "I I"], {"A": ALU, "M": MOTOR, "I": IRON}, 1),
    "kitchen_exhaust_fan": (["A A", "AMA", "IBI"], {"A": ALU, "M": MOTOR, "I": IRON, "B": BUCKET}, 1),
    "roof_pipe_support": (["WKW", "RIR"], {"W": wool("white"), "K": COPPER, "R": RUBBER, "I": IRON}, 4),
    "pipe_doghouse": (["III", "R R", "III"], {"I": IRON, "R": RUBBER}, 1),
    "pipe_crossover": (["Y Y", "BBB", "S S"], {"Y": dye("yellow"), "B": BARS, "S": STEEL}, 1),
    "comms_mast_base": (["N N", "SSS", "KKK"], {"N": NUG, "S": STEEL, "K": "minecraft:smooth_stone"}, 1),
    "comms_mast": (["B B", "BNB", "B B"], {"B": BARS, "N": NUG}, 4),
    "comms_mast_tie": (["B S", "BNS"], {"B": BARS, "N": NUG, "S": STEEL}, 1),
    "comms_mast_top": (["WCW", "QAQ", " B "], {"W": WIRE, "C": CIRC, "Q": QUARTZ, "A": ALU, "B": BARS}, 1),
    "comms_dish": ([" AA", "ACA", " AI"], {"A": ALU, "C": CIRC, "I": IRON}, 1),
    "rf_cabinet": (["SSS", "SAS", "SRS"], {"S": STEEL, "A": ADV, "R": REDSTONE}, 1),
    # the Advanced Crafting Table: steel round a crafting table, its lit glass grid, circuits and redstone for the screen
    "advanced_crafting_table": (["SGS", "CTC", "SRS"], {"S": STEEL, "G": PANE, "C": CIRC, "T": "minecraft:crafting_table",
                                                         "R": REDSTONE}, 1),
    # dispatch (work list item 20)
    "goods_lift_door": (["SGS", "PCP", "SRS"], {"S": STEEL, "G": PANE, "P": "minecraft:piston", "C": CIRC, "R": REDSTONE}, 1),
    "lift_rail": (["SN", "SN", "SN"], {"S": STEEL, "N": NUG}, 6),
    "energy_port_plate": (["YSY", " W "], {"Y": dye("yellow"), "S": STEEL, "W": WIRE}, 2),
    "cargo_port_plate": (["YSY", " H "], {"Y": dye("yellow"), "S": STEEL, "H": "minecraft:hopper"}, 2),
    "crate_pallet": (["CC", "SS"], {"C": CHEST, "S": "minecraft:oak_slab"}, 1),
    "cargo_case_packer": (["SHS", "CMC", "SPS"], {"S": STEEL, "H": "minecraft:hopper", "C": CIRC, "M": "minecraft:piston",
                                                  "P": PANE}, 1),
    "cargo_infeed": (["SSS", "RRR", " H "], {"S": STEEL, "R": "minecraft:black_wool", "H": "minecraft:hopper"}, 1),
    # the diorama case: acrylic over a map on a white plinth, the light strip's glowstone
    "tower_diorama_case": (["PGP", "PMP", "QAQ"], {"P": PANE, "G": GLOW, "M": MAP, "Q": "minecraft:quartz_block", "A": ALU}, 1),
}


def signature(pattern, key):
    """A shaped recipe as the grid sees it: the rows of ingredients, and the same mirrored (the game matches both)."""
    rows = tuple(tuple(key.get(ch, "") if ch != " " else "" for ch in row) for row in pattern)
    return {rows, tuple(tuple(reversed(r)) for r in rows)}


def clashes(others):
    """Pieces whose recipe another recipe matches. others: {name: (pattern, key)} of every other shaped recipe."""
    seen = {}
    for name, (pattern, key) in others.items():
        if name in CRAFTING:
            continue
        for s in signature(pattern, key):
            seen.setdefault(s, name)
    out = []
    for name, (pattern, key, _) in CRAFTING.items():
        sig = signature(pattern, key)
        hit = next((seen[s] for s in sig if s in seen), None)
        if hit:
            out.append((name, hit))
        for s in sig:
            seen.setdefault(s, name)
    return out


# The kits' textures (each kit's own prefix: tools/training_kit.py td_, manager_kit mg_, lab_kit lk_, rest_kit rk_,
# planning_kit pk_, office_kit ok_, lounge_kit lk2_, sim_kit sk_, class_kit cr_, meeting_kit mt_, wash_kit wr_;
# room_extra_kit sim*, brs_, ows_): materials at 16 texels a pixel (256 a block), their pictures, screens and prints in
# atlases up to 2048 square, as the approved mockups drew them (docs/ART_DIRECTION.md, "High resolution"). The roof
# plant's (tools/roof_plant.py, rp_) and the Advanced Crafting Table's (tools/adv_crafting_table.py, act_) are 64 square
# (4 texels a pixel) but for their pictures' atlases, 512 square.
TEXTURE_PREFIXES = ("td_", "mg_", "lk_", "rk_", "pk_", "ok_", "lk2_", "sk_", "cr_", "mt_", "wr_", "sim", "brs_", "ows_", "rp_", "act_", "gl_")


def sharp(name, size):
    """Whether a texture of this size is one of the kits' (square, a power of two up to 2048)."""
    w, h = size
    return name.startswith(TEXTURE_PREFIXES) and w == h and w in (256, 512, 1024, 2048)
