"""The office, lobby, break room, locker room and lab part of the plant kit (see tools/plant_interior.py).

Same conventions as plant_interior: boxes in 1/16 block for a block facing north (whoever uses it looks north, its
front is the south face), "name!" maps a whole texture onto that face. Kinds:
  stacks   joins up when stacked (body, a cap on the top block, a plinth on the bottom one)
  row      joins side by side with its group (body, an end piece where nothing joins on the left/right)
  variants comes out as one of n at random, like paintings; "vtex" is the texture that changes ({vtex}_{i})
Plants are crossed planes (their own model code). The round meeting table is nine blocks (conference_table).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M

S = M.CELL

# --------------------------------------------------------------------------------------------------- the pieces
COUNTER_BODY = [
    (0, 0, 1.5, 16, 1, 14.5, "pi_toe_kick"),
    (0, 1, 1, 16, 14, 15, {"*": "pi_cab_white", "south": "pi_kitchen_front!"}),
    (0, 14, 0.5, 16, 15, 16, {"*": "pi_worktop", "up": "pi_worktop!"}),
    # The wall cupboards over it.
    (0, 22, 0.5, 16, 30, 6.5, {"*": "pi_cab_white", "south": "pi_wall_cab!"}),
]
LAB_BODY = [
    (0, 0, 2, 16, 1, 14, "pi_toe_kick"),
    (0, 1, 1.5, 16, 13, 14.5, {"*": "pi_lab_cabinet", "south": "pi_lab_front!"}),
    (0, 13, 0.5, 16, 14.5, 16, {"*": "pi_epoxy_black", "up": "pi_epoxy_black!"}),
    # The reagent shelf along the back.
    (7.4, 14.5, 1, 8.6, 23, 2, "pi_chrome"),
    (0, 22.2, 0.6, 16, 23, 5, "pi_chrome"),
    (0.6, 23, 1, 15.4, 27, 4.4, {"*": "pi_reagents", "south": "pi_reagents!", "north": "pi_reagents!"}),
]
DESK_FRAME = [
    (0, 11.5, 1, 16, 12.5, 15, {"*": "pi_laminate", "up": "pi_laminate!"}),
    (0.5, 0, 1, 1.5, 11.5, 15, "pi_desk_panel"), (14.5, 0, 1, 15.5, 11.5, 15, "pi_desk_panel"),
    (1.5, 3, 1.2, 14.5, 11.5, 2, "pi_desk_panel"),
]

OFFICE = {
    # ---- storage
    "locker_open": {"display": "Open Locker", "stacks": True, "boxes": [
        (1, 0, 2, 15, 16, 3, "pi_locker_side"), (1, 0, 3, 2, 16, 15, "pi_locker_side"), (14, 0, 3, 15, 16, 15, "pi_locker_side"),
        (2, 0, 3, 14, 16, 3.2, "pi_locker_inside"),
        # The door swung open on its hinge.
        (0.4, 0.4, 15, 1.2, 15.6, 21.5, {"*": "pi_locker_side", "west": "pi_locker_door!", "east": "pi_locker_door_in!"})],
        "cap": [(0.8, 15.4, 1.8, 15.2, 16, 15.2, "pi_cabinet_dark"),
                (2, 12.4, 3, 14, 13, 14.5, "pi_locker_side"),                      # the hat shelf
                (5, 13, 5, 11, 15, 11, {"*": "pi_hardhat_white", "up": "pi_hardhat_white"}),
                (2.5, 11, 8.6, 13.5, 11.6, 9.4, "pi_chrome"),                       # the hanging rail
                (4, -9, 6, 12, 10.8, 11, {"*": "pi_lab_coat", "south": "pi_lab_coat!", "north": "pi_lab_coat!"}),
                (6.5, 10.8, 8.2, 9.5, 11.2, 9.8, "pi_chrome")],
        "plinth": [(0.8, 0, 1.8, 15.2, 1, 15.2, "pi_cabinet_dark"),
                   (3.5, 1, 6, 7.5, 4, 12, "pi_boot"), (8.5, 1, 6, 12.5, 4, 12, "pi_boot")]},
    "bookcase": {"display": "Bookcase", "stacks": True, "boxes": [
        (1, 0, 1, 2, 16, 15, "pi_veneer"), (14, 0, 1, 15, 16, 15, "pi_veneer"), (2, 0, 1, 14, 16, 2, "pi_veneer"),
        (2, 7.5, 2, 14, 8.5, 15, "pi_veneer"), (2, 15.4, 2, 14, 16, 15, "pi_veneer"),
        (2.1, 8.5, 2.5, 13.9, 15.4, 13, {"*": "pi_books_side", "south": "pi_books!"}),
        (2.1, 0.6, 2.5, 13.9, 7.5, 13, {"*": "pi_books_side", "south": "pi_binders!"})],
        "cap": [(0.6, 15.6, 0.6, 15.4, 16.4, 15.4, "pi_veneer")],
        "plinth": [(1, 0, 1, 15, 0.6, 15, "pi_veneer")]},
    "fridge": {"display": "Fridge", "stacks": True, "boxes": [
        (1.5, 0, 2, 14.5, 16, 15, {"*": "pi_fridge", "south": "pi_fridge_front!"}),
        (12, 2, 15, 12.8, 14, 15.8, "pi_chrome")],
        "cap": [(1.3, 15.5, 1.8, 14.7, 16, 15.2, "pi_fridge")],
        "plinth": [(1.7, 0, 2, 14.3, 1, 14.8, "pi_toe_kick")]},
    "fume_hood": {"display": "Fume Hood", "stacks": True, "boxes": [
        (0, 0, 1, 1, 16, 15.5, "pi_hood_side"), (15, 0, 1, 16, 16, 15.5, "pi_hood_side"), (1, 0, 1, 15, 16, 2, "pi_hood_inside")],
        "cap": [(0, 12, 1, 16, 16, 15.5, {"*": "pi_hood_side", "south": "pi_hood_front!"}),
                (1, -4, 13.2, 15, 12, 13.8, {"*": "pi_chrome", "south": "pi_sash!", "north": "pi_sash!"})],
        "plinth": [(1, 0, 2, 15, 9, 15, {"*": "pi_lab_cabinet", "south": "pi_lab_front!"}),
                   (0.5, 9, 1, 15.5, 10, 15.5, {"*": "pi_epoxy_black", "up": "pi_epoxy_black!"}),
                   (4, 10, 4, 7, 14, 7, "pi_glassware"), (9, 10, 5, 11, 13, 7, "pi_glassware")]},
    # ---- side by side
    "reception_desk": {"display": "Reception Desk", "row": "reception", "boxes": [
        (0, 0, 1, 16, 15, 3, {"*": "pi_veneer", "north": "pi_reception_front!"}),
        (0, 15, 0, 16, 16, 5, {"*": "pi_quartz", "up": "pi_quartz!"}),
        (0, 10.5, 3, 16, 11.5, 15.5, {"*": "pi_laminate", "up": "pi_laminate!"}),
        (0, 0, 3, 16, 1, 6, "pi_veneer"),
        (7, 11.5, 6, 9, 11.8, 8, "pi_monitor"), (7.6, 11.8, 6.5, 8.4, 13.5, 7, "pi_monitor"),
        (3.5, 13, 6, 12.5, 19, 6.8, {"*": "pi_monitor", "south": "pi_office_screen!"}),
        (4, 11.5, 9, 12, 11.9, 11.5, {"*": "pi_monitor", "up": "pi_keyboard!"})],
        "left": [(0, 0, 3, 1, 10.5, 15.5, "pi_veneer")], "right": [(15, 0, 3, 16, 10.5, 15.5, "pi_veneer")],
        "glow": ["pi_office_screen"]},
    "sofa": {"display": "Sofa", "row": "sofa", "boxes": [
        (0, 0.5, 2, 16, 3, 15.5, "pi_sofa_dark"),
        (0, 3, 2, 16, 6.5, 12, {"*": "pi_sofa", "up": "pi_sofa_seat"}),
        (0, 3, 12, 16, 14, 15.5, "pi_sofa"),
        (0, 6.5, 10, 16, 13, 12, "pi_sofa_seat")],
        "left": [(0, 0.4, 1.8, 2.5, 10, 15.7, "pi_sofa"), (0.4, 0, 2.4, 1.6, 0.5, 3.6, "pi_chrome"), (0.4, 0, 14, 1.6, 0.5, 15.2, "pi_chrome")],
        "right": [(13.5, 0.4, 1.8, 16, 10, 15.7, "pi_sofa"), (14.4, 0, 2.4, 15.6, 0.5, 3.6, "pi_chrome"), (14.4, 0, 14, 15.6, 0.5, 15.2, "pi_chrome")]},
    "locker_bench": {"display": "Locker Room Bench", "row": "bench", "boxes": [
        (0, 5, 4, 16, 6.5, 12, {"*": "pi_bench_wood", "up": "pi_bench_wood!"})],
        "left": [(1, 0, 5, 2.5, 5, 11, "pi_chrome")], "right": [(13.5, 0, 5, 15, 5, 11, "pi_chrome")]},
    "kitchen_counter": {"display": "Kitchen Counter", "row": "counter", "boxes": COUNTER_BODY, "left": [], "right": []},
    "kitchen_sink": {"display": "Kitchen Sink", "row": "counter", "boxes": COUNTER_BODY[:2] + [
        (0, 14, 0.5, 3, 15, 16, "pi_worktop"), (13, 14, 0.5, 16, 15, 16, "pi_worktop"),
        (3, 14, 0.5, 13, 15, 3, "pi_worktop"), (3, 14, 13, 13, 15, 16, "pi_worktop"),
        (3, 12, 3, 13, 14.2, 13, {"*": "pi_chrome", "up": "pi_sink!"}),
        (7.4, 15, 1.2, 8.6, 20, 2.4, "pi_chrome"), (7.4, 19, 2.4, 8.6, 20, 6.5, "pi_chrome"),
        COUNTER_BODY[3]], "left": [], "right": []},
    "coffee_counter": {"display": "Coffee Counter", "row": "counter", "boxes": COUNTER_BODY + [
        (1.5, 15, 1.5, 7, 22.5, 8.5, {"*": "pi_appliance", "south": "pi_coffee_machine!"}),
        (8.5, 15, 1.5, 15.5, 20, 10, {"*": "pi_appliance", "south": "pi_microwave!"})],
        "left": [], "right": [], "glow": ["pi_coffee_machine"]},
    "lab_bench": {"display": "Lab Bench", "row": "lab", "boxes": LAB_BODY + [
        (3, 14.5, 7, 5, 18, 9, "pi_glassware"), (6, 14.5, 8, 9, 16.5, 11, "pi_glassware"),
        (11, 14.5, 6, 14, 15.5, 11, {"*": "pi_paper", "up": "pi_paper!"})], "left": [], "right": []},
    "lab_sink": {"display": "Lab Sink", "row": "lab", "boxes": LAB_BODY[:2] + [
        (0, 13, 0.5, 4, 14.5, 16, "pi_epoxy_black"), (12, 13, 0.5, 16, 14.5, 16, "pi_epoxy_black"),
        (4, 13, 0.5, 12, 14.5, 4, "pi_epoxy_black"), (4, 13, 12, 12, 14.5, 16, "pi_epoxy_black"),
        (4, 11, 4, 12, 13.2, 12, {"*": "pi_epoxy_black", "up": "pi_sink_black!"}),
        (7.4, 14.5, 2, 8.6, 23, 3.2, "pi_chrome"), (7.4, 22, 3.2, 8.6, 23, 7, "pi_chrome")] + LAB_BODY[3:],
        "left": [], "right": []},
    "lab_analyzer": {"display": "Water Analyzer", "row": "lab", "boxes": LAB_BODY[:3] + [
        (1.5, 14.5, 2, 14.5, 22, 11, {"*": "pi_appliance", "south": "pi_analyzer!"}),
        (2, 14.5, 11.5, 6, 15, 14, "pi_glassware")], "left": [], "right": [], "glow": ["pi_analyzer"]},
    # ---- single pieces
    "cctv_desk": {"display": "CCTV Desk", "boxes": DESK_FRAME + [
        (1, 12.5, 3, 15, 13, 5, "pi_monitor"), (7.5, 13, 3.5, 8.5, 22, 4.2, "pi_monitor"),
        (0.2, 14, 3.4, 5.4, 18, 4.2, {"*": "pi_monitor", "south": "pi_cctv!"}),
        (5.4, 14, 3.4, 10.6, 18, 4.2, {"*": "pi_monitor", "south": "pi_cctv2!"}),
        (10.6, 14, 3.4, 15.8, 18, 4.2, {"*": "pi_monitor", "south": "pi_cctv!"}),
        (2.8, 18.2, 3.4, 8, 22.2, 4.2, {"*": "pi_monitor", "south": "pi_cctv2!"}),
        (8, 18.2, 3.4, 13.2, 22.2, 4.2, {"*": "pi_monitor", "south": "pi_cctv!"}),
        (4, 12.5, 8, 12, 12.9, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"}),
        (13, 12.5, 8, 15, 13.5, 11, "pi_appliance")], "glow": ["pi_cctv", "pi_cctv2"]},
    "coffee_table": {"display": "Coffee Table", "boxes": [
        (1, 6, 3, 15, 7, 13, {"*": "pi_veneer", "up": "pi_veneer!"}),
        (1.5, 0, 3.5, 2.5, 6, 4.5, "pi_chrome"), (13.5, 0, 3.5, 14.5, 6, 4.5, "pi_chrome"),
        (1.5, 0, 11.5, 2.5, 6, 12.5, "pi_chrome"), (13.5, 0, 11.5, 14.5, 6, 12.5, "pi_chrome"),
        (3, 7, 5, 7.5, 7.4, 10, {"*": "pi_paper", "up": "pi_magazine!"}), (9, 7, 6, 13, 7.3, 9, {"*": "pi_paper", "up": "pi_magazine!"})]},
    "cafe_table": {"display": "Cafe Table", "boxes": [
        (2, 11.5, 2, 14, 12.5, 14, {"*": "pi_laminate", "up": "pi_laminate!"}),
        (7, 1, 7, 9, 11.5, 9, "pi_chrome"), (4, 0, 4, 12, 1, 12, "pi_chrome")]},
    "cafe_chair": {"display": "Cafe Chair", "boxes": [
        (3.5, 6, 3.5, 12.5, 7, 12.5, "pi_plastic"), (3.5, 7, 11.5, 12.5, 15, 12.5, "pi_plastic"),
        (4, 0, 4, 5, 6, 5, "pi_chrome"), (11, 0, 4, 12, 6, 5, "pi_chrome"), (4, 0, 11, 5, 6, 12, "pi_chrome"), (11, 0, 11, 12, 6, 12, "pi_chrome")]},
    "coat_rack": {"display": "Coat Rack", "boxes": [
        (1, 12.5, 0, 15, 13.5, 1.2, "pi_veneer"), (1, 15, 0, 15, 15.5, 4.5, "pi_veneer"),
        (2, 2.5, 0.6, 6.5, 12.5, 4, {"*": "pi_lab_coat", "south": "pi_lab_coat!"}),
        (9.5, 3, 0.6, 14, 12.5, 4, {"*": "pi_hi_vis", "south": "pi_hi_vis!"}),
        (2.5, 15.5, 1, 6.5, 17.5, 4, "pi_hardhat_white"), (9.5, 15.5, 1, 13.5, 17.5, 4, "pi_hardhat_yellow")]},
    "printer": {"display": "Office Copier", "boxes": [
        (1, 1, 2, 15, 11, 14, {"*": "pi_copier", "south": "pi_copier_front!"}),
        (1.5, 0, 2.5, 14.5, 1, 13.5, "pi_toe_kick"),
        (1, 11, 2, 15, 12.6, 14, {"*": "pi_copier", "up": "pi_copier_top!"}),
        (2, 12.6, 10, 8, 13.2, 13.5, {"*": "pi_monitor", "up": "pi_copier_panel!"}),
        (15, 7, 4, 16, 7.5, 12, "pi_copier"), (15, 7.5, 5, 15.8, 8, 11, "pi_paper")],
        "glow": ["pi_copier_panel"]},
    "wall_display": {"display": "Wall Screen", "boxes": [
        (0, 4, 0, 16, 14, 1.2, {"*": "pi_monitor", "south": "pi_tv!"})], "glow": ["pi_tv"]},
    # ---- one of many
    "whiteboard": {"display": "Whiteboard", "variants": 24, "vtex": "pi_whiteboard", "boxes": [
        (0.5, 3, 0, 15.5, 15, 0.6, {"*": "pi_chrome", "south": "pi_whiteboard!"}),
        (1, 3, 0.6, 15, 3.4, 1.6, "pi_chrome"),
        (3, 3.4, 0.8, 5.5, 3.9, 1.3, "pi_marker_blue"), (6, 3.4, 0.8, 8.5, 3.9, 1.3, "pi_marker_red"),
        (11, 3.4, 0.7, 13.5, 4.4, 1.5, "pi_eraser")]},
    "framed_print": {"display": "Framed Print", "variants": 12, "vtex": "pi_print", "boxes": [
        (1, 3, 0, 15, 14, 1, {"*": "pi_frame", "south": "pi_print!"})]},
    "office_plant": {"display": "Office Plant", "variants": 8, "plant": True},
}

PLANT_HEIGHTS = [22, 26, 18, 20, 28, 16, 24, 14]
POTS = ["pi_pot_white", "pi_pot_grey", "pi_pot_terracotta"]

# Collision is given in PlantInterior.java.
CRAFTING = {
    "locker_open": (["PP", "PW", "PP"], {"P": "#c:plates/iron", "W": "minecraft:white_wool"}, 2),
    "bookcase": (["PPP", "BBB", "PPP"], {"P": "#minecraft:planks", "B": "minecraft:book"}, 2),
    "fridge": (["PP", "PI", "PP"], {"P": "#c:plates/iron", "I": "minecraft:packed_ice"}, 2),
    "fume_hood": (["PGP", "P P", "PPP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane"}, 2),
    "reception_desk": (["QQQ", "PPP", "P P"], {"Q": "minecraft:quartz_slab", "P": "#minecraft:planks"}, 3),
    "sofa": (["W  ", "WWW", "P P"], {"W": "minecraft:gray_wool", "P": "#minecraft:planks"}, 2),
    "locker_bench": (["SSS", "N N"], {"S": "minecraft:oak_slab", "N": "minecraft:iron_nugget"}, 3),
    "kitchen_counter": (["SSS", "PPP", "PPP"], {"S": "minecraft:smooth_stone_slab", "P": "#minecraft:planks"}, 3),
    "kitchen_sink": (["SBS", "PPP"], {"S": "minecraft:smooth_stone_slab", "B": "minecraft:cauldron", "P": "#minecraft:planks"}, 1),
    "coffee_counter": (["SCS", "PPP"], {"S": "minecraft:smooth_stone_slab", "C": "minecraft:cocoa_beans", "P": "#minecraft:planks"}, 1),
    "lab_bench": (["BBB", "PPP", "PPP"], {"B": "minecraft:blackstone_slab", "P": "#c:plates/iron"}, 3),
    "lab_sink": (["BCB", "PPP"], {"B": "minecraft:blackstone_slab", "C": "minecraft:cauldron", "P": "#c:plates/iron"}, 1),
    "lab_analyzer": (["GCG", "BBB", "PPP"], {"G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "B": "minecraft:blackstone_slab", "P": "#c:plates/iron"}, 1),
    "cctv_desk": (["GGG", "PCP", "P P"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "P": "#minecraft:planks"}, 1),
    "coffee_table": (["SSS", "N N"], {"S": "minecraft:oak_slab", "N": "minecraft:iron_nugget"}, 2),
    "cafe_table": (["SSS", " N ", " N "], {"S": "minecraft:smooth_stone_slab", "N": "minecraft:iron_nugget"}, 2),
    "cafe_chair": (["P  ", "PPP", "N N"], {"P": "jugcraft:plastic_sheet", "N": "minecraft:iron_nugget"}, 2),
    "coat_rack": (["SSS", "WNW"], {"S": "minecraft:oak_slab", "W": "minecraft:white_wool", "N": "minecraft:iron_nugget"}, 2),
    "printer": (["GPG", "PCP", "PPP"], {"G": "minecraft:glass_pane", "P": "#c:plates/iron", "C": "jugcraft:basic_circuit"}, 1),
    "wall_display": (["GGG", "GCG"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit"}, 1),
    "framed_print": (["SSS", "SPS", "SSS"], {"S": "minecraft:stick", "P": "minecraft:paper"}, 2),
    "office_plant": ([" L ", "LLL", " T "], {"L": "#minecraft:leaves", "T": "minecraft:flower_pot"}, 2),
    "conference_table": (["SSS", "SSS", " N "], {"S": "minecraft:oak_slab", "N": "#c:plates/iron"}, 1),
}


# --------------------------------------------------------------------------------------------------- models

def plant_model(i, rid):
    """A pot of soil with a plant in it, two crossed planes of leaves (a little taller than a block for the big ones)."""
    h = PLANT_HEIGHTS[i]
    pot = POTS[i % 3]
    els = [
        {"from": [4, 0, 4], "to": [12, 7, 12], "faces": {f: {"texture": "#pot"} for f in ("north", "south", "east", "west", "down")}},
        {"from": [4.6, 6, 4.6], "to": [11.4, 6.6, 11.4], "faces": {"up": {"texture": "#soil"}}},
    ]
    for angle in (45, -45):
        els.append({"from": [0, 5, 8], "to": [16, h, 8], "rotation": {"angle": angle, "axis": "y", "origin": [8, 8, 8]},
                    "shade": False, "faces": {"north": {"texture": "#leaves", "uv": [0, 0, 16, 16]},
                                               "south": {"texture": "#leaves", "uv": [0, 0, 16, 16]}}})
    return {"textures": {"pot": rid(f"block/{pot}"), "soil": rid("block/pi_soil"), "leaves": rid(f"block/pi_plant_{i}"),
                         "particle": rid(f"block/pi_plant_{i}")}, "elements": els}


def table_boxes(part):
    """The round table's boxes over block `part` of its 3 x 3 (a disc of radius 23 px round the middle)."""
    bx = part % 3
    bz = part // 3
    boxes = []
    for z in range(0, 16):
        gz = bz * 16 + z + 0.5 - 24
        half = math.sqrt(max(0, 23 * 23 - gz * gz))
        x0, x1 = max(0, 24 - half - bx * 16), min(16, 24 + half - bx * 16)
        if x1 - x0 > 0.5:
            x0, x1 = round(x0 * 4) / 4, round(x1 * 4) / 4
            boxes.append((x0, 12, z, x1, 13, z + 1, {"*": "pi_walnut_edge", "up": "pi_walnut", "down": "pi_walnut_edge"}))
    if part == 4:
        boxes += [(6, 1, 6, 10, 12, 10, "pi_steel_dark"), (2, 0, 2, 14, 1, 14, "pi_steel_dark"),
                  (3, 11, 3, 13, 12, 13, "pi_steel_dark")]
    return boxes


def rows():
    return {k: v for k, v in OFFICE.items() if "row" in v}


# --------------------------------------------------------------------------------------------------- textures

def _img(colour):
    return M.to_img(M.base(colour, S))


def _noise(img, seed, colours, count):
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for _ in range(count):
        d.point((r.randrange(S), r.randrange(S)), fill=r.choice(colours))
    return img


def _wood(colour, seed, grain=12):
    a = M.base(colour, S)
    M.shade(a, M.streaks(S, seed, "h", count=120, amount=grain))
    M.shade(a, M.fbm(S, seed + 1, 3, 64) * 5)
    return M.to_img(a)


def _veneer():
    return _wood((150, 108, 70), 401)


def _walnut():
    img = _wood((96, 64, 42), 402, 14)
    d = ImageDraw.Draw(img)
    d.line([(0, 2), (S, 2)], fill=(130, 92, 60, 255), width=2)
    return img


def _cab_white():
    return M.painted((232, 232, 226), 403, chip_count=0, drip_count=0, dirt=0.04)


def _kitchen_front():
    img = _cab_white()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 62, 125], outline=(196, 196, 190, 255), width=2)
    d.rectangle([66, 2, 125, 125], outline=(196, 196, 190, 255), width=2)
    d.rectangle([2, 2, 125, 26], outline=(196, 196, 190, 255), width=2)
    for x in (52, 76):
        d.rectangle([x, 40, x + 3, 70], fill=(150, 154, 158, 255))
    d.rectangle([50, 12, 78, 15], fill=(150, 154, 158, 255))
    return img


def _wall_cab():
    img = _cab_white()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 62, 125], outline=(196, 196, 190, 255), width=2)
    d.rectangle([66, 2, 125, 125], outline=(196, 196, 190, 255), width=2)
    for x in (54, 72):
        d.rectangle([x, 96, x + 3, 120], fill=(150, 154, 158, 255))
    return img


def _worktop():
    a = M.base((70, 72, 76), S)
    M.shade(a, M.fbm(S, 404, 4, 24) * 10)
    img = _noise(M.to_img(a), 405, [(110, 112, 116, 255), (50, 52, 56, 255), (140, 136, 130, 255)], 900)
    return img


def _sink(black=False):
    img = _img((40, 42, 46) if black else (190, 194, 198))
    d = ImageDraw.Draw(img)
    c = (24, 24, 26, 255) if black else (150, 154, 158, 255)
    d.rounded_rectangle([10, 10, S - 11, S - 11], radius=14, outline=c, width=6)
    d.ellipse([56, 56, 72, 72], fill=(30, 30, 32, 255))
    return img


def _appliance():
    return M.brushed((46, 48, 52), 406, "v", dirt=0.05)


def _coffee_machine():
    img = _appliance()
    d = ImageDraw.Draw(img)
    d.rectangle([20, 14, 108, 40], fill=(20, 22, 26, 254))
    d.rectangle([28, 20, 70, 32], fill=(90, 200, 240, 254))
    for k in range(3):
        d.ellipse([78 + k * 9, 22, 84 + k * 9, 28], fill=(240, 240, 240, 254))
    d.rectangle([44, 56, 84, 64], fill=(170, 174, 178, 254))
    d.rectangle([50, 82, 78, 112], fill=(240, 240, 236, 254))  # a cup
    d.rectangle([20, 116, 108, 124], fill=(120, 124, 128, 254))
    return img


def _microwave():
    img = _img((214, 216, 214))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 14, 90, 114], fill=(30, 32, 36, 255), outline=(150, 152, 150, 255), width=3)
    d.line([(16, 30), (60, 90)], fill=(60, 64, 70, 255), width=3)
    d.rectangle([98, 18, 120, 34], fill=(30, 40, 30, 255))
    for row in range(4):
        for col in range(2):
            d.rectangle([100 + col * 10, 44 + row * 12, 106 + col * 10, 50 + row * 12], fill=(170, 172, 170, 255))
    return img


def _toe_kick():
    return _img((40, 40, 42))


def _lab_cabinet():
    return M.painted((206, 210, 212), 407, chip_count=0, drip_count=0, dirt=0.06)


def _lab_front():
    img = _lab_cabinet()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 125, 36], outline=(160, 164, 166, 255), width=2)
    d.rectangle([2, 40, 62, 125], outline=(160, 164, 166, 255), width=2)
    d.rectangle([66, 40, 125, 125], outline=(160, 164, 166, 255), width=2)
    d.rectangle([50, 16, 78, 20], fill=(110, 114, 118, 255))
    for x in (52, 74):
        d.rectangle([x, 50, x + 3, 74], fill=(110, 114, 118, 255))
    return img


def _epoxy_black():
    a = M.base((34, 36, 38), S)
    M.shade(a, M.fbm(S, 408, 4, 32) * 5)
    return M.to_img(a)


def _reagents():
    """Bottles of reagents and buffer solutions on a shelf: brown glass, clear glass, coloured caps and labels."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(409)
    x = 2
    while x < S - 14:
        w = r.choice((12, 14, 18))
        h = r.randint(70, 120)
        body = r.choice([(120, 70, 30), (190, 214, 220), (110, 64, 26), (230, 230, 226), (90, 140, 200)])
        d.rectangle([x, S - h, x + w - 2, S - 1], fill=body + (255,))
        d.rectangle([x + 2, S - h - 10, x + w - 4, S - h], fill=r.choice([(220, 40, 40), (40, 90, 200), (240, 240, 240), (30, 30, 30)]) + (255,))
        d.rectangle([x + 1, S - h // 2 - 10, x + w - 3, S - h // 2 + 8], fill=(246, 246, 240, 255))
        x += w + 2
    return img


def _glassware():
    img = _img((200, 222, 230))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 70, S, S], fill=(110, 170, 210, 255))
    d.line([(24, 0), (24, S)], fill=(240, 250, 252, 255), width=6)
    return img


def _hood_side():
    return M.painted((222, 224, 222), 410, chip_count=0, drip_count=0, dirt=0.05)


def _hood_inside():
    return M.brushed((180, 184, 186), 411, "v", dirt=0.15)


def _hood_front():
    img = _hood_side()
    d = ImageDraw.Draw(img)
    d.rectangle([10, 30, 70, 50], fill=(240, 240, 236, 255), outline=(60, 60, 60, 255))
    d.line([(16, 40), (60, 40)], fill=(30, 30, 30, 255), width=3)
    d.rectangle([86, 24, 116, 56], fill=(30, 34, 38, 255))
    d.rectangle([92, 30, 110, 42], fill=(90, 220, 120, 254))
    d.ellipse([96, 46, 104, 54], fill=(250, 200, 40, 254))
    d.rectangle([4, 96, S - 5, 104], fill=(170, 174, 178, 255))
    return img


def _sash():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (170, 176, 180)
    a[..., 3] = 255
    a[10:S - 22, 8:S - 8, 3] = 70
    a[10:S - 22, 8:S - 8, :3] = (190, 220, 230)
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle([20, S - 18, S - 21, S - 12], fill=(60, 62, 66, 255))
    d.line([(20, 20), (60, 70)], fill=(230, 245, 250, 140), width=3)
    return img


def _reception_front():
    """The visitors' side of the reception desk: wood veneer panels with a brushed steel band and the plant's logo."""
    img = _veneer()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 92, S, 104], fill=(176, 180, 184, 255))
    for x in (0, 63):
        d.line([(x, 0), (x, 92)], fill=(110, 76, 48, 255), width=2)
    return img


def _quartz():
    img = _img((236, 234, 228))
    return _noise(img, 412, [(214, 210, 204, 255), (246, 246, 242, 255), (190, 186, 180, 255)], 700)


def _sofa():
    a = M.base((66, 84, 104), S)
    M.shade(a, M.grain(S, 413, 6))
    return M.to_img(a)


def _sofa_seat():
    img = _sofa()
    d = ImageDraw.Draw(img)
    d.line([(0, 2), (S, 2)], fill=(50, 64, 80, 255), width=3)
    d.line([(63, 0), (63, S)], fill=(52, 66, 82, 255), width=2)
    return img


def _bench_wood():
    img = _wood((176, 132, 86), 414, 10)
    d = ImageDraw.Draw(img)
    for y in (42, 86):
        d.line([(0, y), (S, y)], fill=(110, 80, 50, 255), width=3)
    return img


def _locker_inside():
    return _img((92, 102, 116))


def _locker_door(inside=False):
    img = M.painted((120, 132, 150), 415 + inside, chip_count=1, drip_count=0, dirt=0.1)
    d = ImageDraw.Draw(img)
    if not inside:
        for y in range(14, 34, 5):
            d.line([(30, y), (98, y)], fill=(50, 58, 70, 255), width=2)
        d.rectangle([40, 44, 88, 52], fill=(230, 230, 222, 255))
    else:
        d.rectangle([30, 20, 98, 60], fill=(240, 240, 236, 255))   # a mirror
        d.rectangle([32, 22, 96, 58], fill=(196, 214, 222, 255))
    return img


def _lab_coat():
    img = _img((240, 240, 236))
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, S)], fill=(200, 200, 196, 255), width=3)
    d.polygon([(64, 0), (40, 30), (64, 44)], fill=(220, 220, 216, 255))
    d.polygon([(64, 0), (88, 30), (64, 44)], fill=(220, 220, 216, 255))
    d.rectangle([20, 60, 44, 72], outline=(200, 200, 196, 255), width=2)
    d.rectangle([84, 60, 108, 72], outline=(200, 200, 196, 255), width=2)
    d.rectangle([90, 50, 102, 58], fill=(40, 90, 200, 255))      # a dosimeter badge
    for y in (40, 70, 100):
        d.ellipse([60, y, 66, y + 6], fill=(180, 180, 176, 255))
    return img


def _hi_vis():
    img = _img((240, 140, 30))
    d = ImageDraw.Draw(img)
    for y in (50, 90):
        d.rectangle([0, y, S, y + 10], fill=(214, 216, 214, 255))
    d.line([(64, 0), (64, S)], fill=(200, 110, 20, 255), width=3)
    return img


def _hardhat(colour):
    img = _img(colour)
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, S)], fill=tuple(max(0, c - 30) for c in colour) + (255,), width=6)
    return img


def _boot():
    img = _img((30, 28, 26))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 100, S, S], fill=(170, 120, 40, 255))
    return img


def _books(binders=False):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(416 + binders)
    x = 0
    while x < S:
        w = r.choice((12, 16, 20)) if binders else r.choice((5, 6, 8, 10))
        h = r.randint(100, 126) if binders else r.randint(70, 124)
        col = r.choice([(40, 80, 160), (200, 200, 196), (30, 30, 34), (170, 40, 40), (40, 120, 70)] if binders else
                       [(120, 40, 40), (40, 60, 110), (60, 90, 50), (140, 110, 60), (40, 40, 44), (190, 170, 120), (90, 50, 90)])
        d.rectangle([x, S - h, x + w - 1, S - 1], fill=col + (255,), outline=tuple(c // 2 for c in col) + (255,))
        if binders:
            d.rectangle([x + 3, S - h + 20, x + w - 4, S - h + 40], fill=(246, 246, 240, 255))
            d.ellipse([x + w // 2 - 3, S - 30, x + w // 2 + 3, S - 24], fill=(20, 20, 20, 255))
        else:
            d.line([(x + 1, S - h + 10), (x + w - 2, S - h + 10)], fill=(220, 190, 100, 255))
        x += w
        if not binders and r.random() < 0.08:
            x += 10
    return img


def _fridge(front=False):
    img = M.brushed((206, 208, 210), 417 + front, "v", dirt=0.04)
    if front:
        d = ImageDraw.Draw(img)
        d.line([(0, 44), (S, 44)], fill=(150, 152, 154, 255), width=3)
        d.rectangle([20, 60, 40, 72], fill=(240, 240, 236, 255))   # notes on the door
        d.rectangle([46, 70, 60, 86], fill=(250, 230, 90, 255))
    return img


def _analyzer():
    img = _img((220, 222, 220))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 10, 70, 60], fill=(24, 30, 36, 254))
    d.line([(14, 50), (26, 30), (38, 40), (50, 20), (64, 28)], fill=(90, 220, 140, 254), width=2)
    d.rectangle([80, 14, 118, 50], fill=(60, 64, 70, 255))
    for k in range(4):
        d.rectangle([84 + k * 9, 60, 90 + k * 9, 66], fill=(60, 64, 70, 255))
    d.rectangle([12, 80, 116, 118], fill=(40, 44, 48, 255))
    for k in range(6):
        d.ellipse([18 + k * 16, 92, 28 + k * 16, 102], fill=(190, 214, 222, 255))
    return img


def _cctv(seed):
    """Camera feeds: four grey views of fences, roads and doors with a timestamp bar."""
    img = _img((20, 22, 24))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for qx in (0, 64):
        for qy in (0, 64):
            base = r.randint(60, 110)
            d.rectangle([qx + 2, qy + 2, qx + 61, qy + 61], fill=(base, base + 4, base + 6, 254))
            hz = qy + r.randint(16, 34)
            d.rectangle([qx + 2, qy + 2, qx + 61, hz], fill=(base + 40, base + 46, base + 52, 254))
            if r.random() < 0.6:
                for x in range(qx + 4, qx + 60, 6):
                    d.line([(x, hz - 8), (x, hz + 8)], fill=(base - 30, base - 28, base - 26, 254))
            else:
                d.polygon([(qx + 20, qy + 61), (qx + 30, hz), (qx + 36, hz), (qx + 50, qy + 61)], fill=(base - 20,) * 3 + (254,))
            d.rectangle([qx + 2, qy + 54, qx + 40, qy + 60], fill=(10, 10, 10, 254))
    a = np.asarray(img).copy()
    a[..., 3] = 254
    return Image.fromarray(a, "RGBA")


def _copier(part):
    img = _img((226, 226, 220))
    d = ImageDraw.Draw(img)
    if part == "front":
        for k in range(3):
            y = 40 + k * 28
            d.rectangle([8, y, S - 9, y + 24], outline=(180, 180, 176, 255), width=2)
            d.rectangle([50, y + 8, 78, y + 12], fill=(120, 124, 128, 255))
        d.rectangle([8, 8, S - 9, 32], fill=(60, 64, 70, 255))
    elif part == "top":
        d.rectangle([10, 10, S - 11, S - 30], fill=(60, 64, 70, 255))
    return img


def _copier_panel():
    img = _img((30, 32, 36))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 80, 60], fill=(60, 140, 220, 254))
    for k in range(9):
        d.rectangle([90 + (k % 3) * 12, 14 + (k // 3) * 14, 98 + (k % 3) * 12, 22 + (k // 3) * 14], fill=(200, 200, 200, 254))
    return img


def _tv():
    """The meeting room screen: the plant's daily status slide."""
    img = _img((236, 240, 244))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 16], fill=(20, 60, 120, 254))
    d.rectangle([8, 26, 60, 70], fill=(250, 250, 252, 254), outline=(180, 186, 194, 254))
    for i, h in enumerate((26, 30, 34, 32, 36, 38)):
        d.rectangle([12 + i * 8, 66 - h, 17 + i * 8, 66], fill=(40, 140, 80, 254))
    d.ellipse([72, 26, 116, 70], fill=(40, 120, 200, 254))
    d.pieslice([72, 26, 116, 70], 0, 80, fill=(240, 180, 40, 254))
    for y in range(82, 120, 10):
        d.rectangle([8, y, 8 + random.Random(y).randint(40, 110), y + 4], fill=(150, 156, 166, 254))
    a = np.asarray(img).copy()
    a[..., 3] = 254
    return Image.fromarray(a, "RGBA")


def _plastic():
    a = M.base((60, 110, 150), S)
    M.shade(a, M.grain(S, 418, 3))
    return M.to_img(a)


def _magazine():
    img = _img((240, 240, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 50], fill=(30, 90, 150, 255))
    d.rectangle([10, 60, 110, 70], fill=(60, 60, 60, 255))
    return img


def _marker(colour):
    return _img(colour)


def _frame():
    return _img((40, 38, 36))


# ---- whiteboards: 24 boards from a meeting's worth of scribbles each

INKS = [(30, 60, 160), (200, 40, 40), (30, 130, 60), (30, 30, 34)]


def _scribble(d, r, x0, y0, x1, ink, width=2):
    """A line of handwriting: a wavy run of short strokes."""
    x = x0
    while x < x1 - 4:
        w = r.randint(6, 16)
        pts = [(x + k * 2, y0 + r.randint(-3, 3)) for k in range(w // 2)]
        if len(pts) > 1:
            d.line(pts, fill=ink + (255,), width=width)
        x += w + r.randint(3, 6)


def _board(i):
    img = _img((246, 248, 248))
    d = ImageDraw.Draw(img)
    r = random.Random(500 + i)
    kind = i % 8
    ink = INKS[r.randrange(4)]
    # A title at the top left.
    _scribble(d, r, 10, 14, r.randint(60, 100), ink, 3)
    if kind == 0:      # bar chart
        d.line([(16, 110), (110, 110)], fill=(30, 30, 34, 255), width=2)
        d.line([(16, 110), (16, 34)], fill=(30, 30, 34, 255), width=2)
        for k in range(5):
            h = r.randint(16, 66)
            d.rectangle([24 + k * 17, 110 - h, 36 + k * 17, 110], outline=INKS[k % 3] + (255,), width=2)
    elif kind == 1:    # flowchart
        boxes = [(14, 34), (70, 34), (14, 78), (70, 78)]
        for bx, by in boxes:
            d.rectangle([bx, by, bx + 42, by + 24], outline=ink + (255,), width=2)
            _scribble(d, r, bx + 4, by + 12, bx + 38, (30, 30, 34), 1)
        d.line([(56, 46), (70, 46)], fill=(30, 30, 34, 255), width=2)
        d.line([(91, 58), (91, 78)], fill=(30, 30, 34, 255), width=2)
        d.line([(70, 90), (56, 90)], fill=(30, 30, 34, 255), width=2)
    elif kind == 2:    # line graph with a limit line
        d.line([(16, 112), (114, 112)], fill=(30, 30, 34, 255), width=2)
        d.line([(16, 112), (16, 30)], fill=(30, 30, 34, 255), width=2)
        pts = [(16 + k * 12, 100 - r.randint(0, 50)) for k in range(9)]
        d.line(pts, fill=ink + (255,), width=2)
        d.line([(16, 44), (114, 44)], fill=(200, 40, 40, 255), width=1)
    elif kind == 3:    # checklist with ticks
        for k in range(6):
            y = 36 + k * 14
            d.rectangle([14, y - 4, 22, y + 4], outline=(30, 30, 34, 255), width=1)
            if r.random() < 0.6:
                d.line([(15, y), (18, y + 3), (23, y - 5)], fill=(30, 130, 60, 255), width=2)
            _scribble(d, r, 28, y, r.randint(70, 118), (30, 30, 34), 1)
    elif kind == 4:    # sticky notes
        for k in range(r.randint(5, 9)):
            x, y = r.randint(8, 96), r.randint(30, 96)
            c = r.choice([(250, 230, 90), (250, 170, 190), (140, 220, 240), (170, 230, 130)])
            d.rectangle([x, y, x + 22, y + 22], fill=c + (255,))
            _scribble(d, r, x + 3, y + 8, x + 20, (40, 40, 40), 1)
    elif kind == 5:    # a sketch of the plant: the reactor dome, a tower, pipes
        d.arc([14, 40, 54, 80], 180, 360, fill=ink + (255,), width=2)
        d.rectangle([14, 60, 54, 108], outline=ink + (255,), width=2)
        d.polygon([(80, 108), (86, 50), (104, 50), (110, 108)], outline=(30, 30, 34, 255))
        d.line([(54, 84), (80, 84)], fill=(200, 40, 40, 255), width=2)
        d.line([(54, 96), (80, 96)], fill=(30, 60, 160, 255), width=2)
    elif kind == 6:    # a schedule grid (an outage plan)
        for k in range(6):
            d.line([(12, 32 + k * 15), (116, 32 + k * 15)], fill=(150, 150, 154, 255), width=1)
        for k in range(5):
            x = 40 + k * 16
            d.line([(x, 32), (x, 107)], fill=(150, 150, 154, 255), width=1)
        for k in range(5):
            y = 34 + k * 15
            x0 = 40 + r.randint(0, 3) * 16
            d.rectangle([x0, y + 2, x0 + r.randint(1, 3) * 16, y + 10], fill=INKS[k % 3] + (255,))
            _scribble(d, r, 14, y + 6, 36, (30, 30, 34), 1)
    else:              # notes and a sum
        for k in range(5):
            _scribble(d, r, 14, 36 + k * 13, r.randint(60, 116), r.choice(INKS), 2)
        d.ellipse([70, 92, 116, 118], outline=(200, 40, 40, 255), width=2)
    # Old marks not quite wiped off.
    for _ in range(r.randint(0, 3)):
        x, y = r.randint(10, 100), r.randint(20, 110)
        d.line([(x, y), (x + r.randint(10, 30), y + r.randint(-4, 4))], fill=(214, 218, 222, 255), width=2)
    d.rectangle([0, 0, S - 1, S - 1], outline=(170, 174, 178, 255), width=4)
    return img


# ---- framed prints

def _print(i):
    r = random.Random(600 + i)
    a = np.zeros((S, S, 3), np.float32)
    yy = np.linspace(0, 1, S)[:, None]
    kind = i % 6
    if kind in (0, 3):     # mountains over a lake / sunset
        top = np.array([90, 140, 210] if kind == 0 else [240, 140, 80], np.float32)
        bottom = np.array([200, 220, 240] if kind == 0 else [250, 210, 140], np.float32)
        a[:] = top * (1 - yy[..., None]) + bottom * yy[..., None]
        img = Image.fromarray(a.astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(img)
        pts = [(0, 80)] + [(x, 80 - r.randint(10, 46)) for x in range(10, S, 18)] + [(S, 80), (S, S), (0, S)]
        d.polygon(pts, fill=(70, 80, 96, 255) if kind == 0 else (90, 50, 60, 255))
        d.rectangle([0, 86, S, S], fill=(60, 110, 160, 255) if kind == 0 else (200, 120, 80, 255))
        if kind == 3:
            d.ellipse([70, 50, 100, 80], fill=(255, 230, 160, 255))
    elif kind == 1:        # the cooling towers at dusk
        a[:] = np.array([60, 80, 130], np.float32) * (1 - yy[..., None]) + np.array([230, 160, 110], np.float32) * yy[..., None]
        img = Image.fromarray(a.astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(img)
        for cx, w in ((44, 30), (92, 26)):
            d.polygon([(cx - w, 120), (cx - w * 0.55, 60), (cx + w * 0.55, 60), (cx + w, 120)], fill=(50, 50, 60, 255))
            d.ellipse([cx - 22, 26, cx + 22, 64], fill=(230, 220, 214, 200))
        d.rectangle([0, 118, S, S], fill=(40, 40, 46, 255))
    elif kind == 2:        # abstract colour blocks
        img = Image.new("RGBA", (S, S), (236, 232, 222, 255))
        d = ImageDraw.Draw(img)
        for _ in range(6):
            x, y = r.randint(0, 90), r.randint(0, 90)
            d.rectangle([x, y, x + r.randint(20, 60), y + r.randint(20, 60)],
                        fill=r.choice([(200, 60, 50), (40, 80, 160), (240, 190, 40), (30, 30, 30), (90, 150, 110)]) + (255,))
    elif kind == 4:        # a forest
        a[:] = np.array([200, 222, 230], np.float32)
        img = Image.fromarray(a.astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(img)
        for _ in range(26):
            x, h = r.randint(-10, S), r.randint(40, 100)
            g = r.randint(50, 110)
            d.polygon([(x, S), (x + 12, S - h), (x + 24, S)], fill=(30, g, 50, 255))
    else:                  # a safety poster
        img = Image.new("RGBA", (S, S), (246, 246, 240, 255))
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, S, 30], fill=(30, 120, 60, 255))
        d.polygon([(64, 40), (100, 104), (28, 104)], fill=(250, 200, 30, 255), outline=(20, 20, 20, 255))
        d.rectangle([60, 58, 68, 84], fill=(20, 20, 20, 255))
        d.rectangle([60, 90, 68, 98], fill=(20, 20, 20, 255))
        d.rectangle([20, 110, 108, 118], fill=(30, 120, 60, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(244, 242, 236, 255), width=8)   # the mount
    return img


# ---- plants

def _plant(i):
    """Leaves on a transparent sheet, drawn for crossed planes: each variant a different kind of office plant."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(700 + i)
    kind = i % 8
    green = [(40, 110, 50), (60, 140, 60), (30, 90, 40), (80, 160, 70)]
    if kind == 1:          # snake plant: tall upright blades
        for k in range(9):
            x = 30 + k * 8 + r.randint(-3, 3)
            top = r.randint(4, 40)
            d.polygon([(x - 4, S), (x, top), (x + 4, S)], fill=(50, 110, 60, 255))
            d.line([(x - 2, S), (x, top + 10)], fill=(200, 200, 90, 255))
    elif kind == 4:        # bamboo palm: canes with fronds
        for k in range(4):
            x = 40 + k * 14
            d.line([(x, S), (x + r.randint(-6, 6), 10)], fill=(110, 140, 60, 255), width=3)
            for y in range(10, 80, 12):
                d.line([(x, y), (x - 20, y + 14)], fill=r.choice(green) + (255,), width=3)
                d.line([(x, y), (x + 20, y + 14)], fill=r.choice(green) + (255,), width=3)
    elif kind == 3:        # monstera: big split leaves
        for _ in range(8):
            cx, cy = r.randint(24, 104), r.randint(16, 90)
            d.ellipse([cx - 18, cy - 14, cx + 18, cy + 14], fill=(40, 110, 50, 255))
            for k in range(3):
                d.line([(cx, cy), (cx + r.randint(-18, 18), cy + r.choice((-14, 14)))], fill=(0, 0, 0, 0), width=2)
            d.line([(cx, cy), (64, S)], fill=(60, 120, 50, 255), width=2)
    elif kind == 7:        # succulent bowl, low
        for k in range(14):
            cx, cy = r.randint(34, 94), r.randint(80, 120)
            d.ellipse([cx - 9, cy - 6, cx + 9, cy + 6], fill=(110, 160, 130, 255), outline=(70, 120, 90, 255))
    else:                  # ficus, fern, rubber plant, peace lily: a stem and clusters of leaves
        stem = (90, 70, 40)
        d.line([(64, S), (64, 20)], fill=stem + (255,), width=3)
        for _ in range(130 if kind in (0, 2) else 50):
            cx, cy = r.randint(20, 108), r.randint(6, 110)
            if math.hypot(cx - 64, (cy - 56) * 0.9) > 52:
                continue
            w = 5 if kind in (0, 2) else 10
            d.ellipse([cx - w, cy - w // 2, cx + w, cy + w // 2], fill=r.choice(green) + (255,))
        if kind == 6:
            for _ in range(5):
                cx, cy = r.randint(40, 88), r.randint(20, 70)
                d.ellipse([cx - 3, cy - 6, cx + 3, cy + 6], fill=(246, 246, 240, 255))
    return img


def _pot(colour):
    img = M.painted(colour, 720 + colour[0], chip_count=0, drip_count=0, dirt=0.08)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 10], fill=tuple(max(0, c - 24) for c in colour) + (255,))
    return img


def _soil():
    img = _img((60, 44, 30))
    return _noise(img, 730, [(80, 60, 40, 255), (40, 30, 20, 255), (120, 110, 100, 255)], 1500)


def makers():
    out = {
        "pi_veneer": _veneer, "pi_walnut": _walnut, "pi_walnut_edge": lambda: _wood((80, 52, 34), 431, 6),
        "pi_steel_dark": lambda: M.brushed((60, 62, 66), 432, "v", dirt=0.05),
        "pi_cab_white": _cab_white, "pi_kitchen_front": _kitchen_front, "pi_wall_cab": _wall_cab, "pi_worktop": _worktop,
        "pi_sink": _sink, "pi_sink_black": lambda: _sink(True), "pi_appliance": _appliance, "pi_coffee_machine": _coffee_machine,
        "pi_microwave": _microwave, "pi_toe_kick": _toe_kick, "pi_lab_cabinet": _lab_cabinet, "pi_lab_front": _lab_front,
        "pi_epoxy_black": _epoxy_black, "pi_reagents": _reagents, "pi_glassware": _glassware, "pi_hood_side": _hood_side,
        "pi_hood_inside": _hood_inside, "pi_hood_front": _hood_front, "pi_sash": _sash, "pi_reception_front": _reception_front,
        "pi_quartz": _quartz, "pi_sofa": _sofa, "pi_sofa_seat": _sofa_seat, "pi_sofa_dark": lambda: _img((36, 40, 46)),
        "pi_bench_wood": _bench_wood, "pi_locker_inside": _locker_inside, "pi_locker_door": _locker_door,
        "pi_locker_door_in": lambda: _locker_door(True), "pi_lab_coat": _lab_coat, "pi_hi_vis": _hi_vis,
        "pi_hardhat_white": lambda: _hardhat((236, 236, 230)), "pi_hardhat_yellow": lambda: _hardhat((240, 200, 30)),
        "pi_boot": _boot, "pi_books": _books, "pi_binders": lambda: _books(True), "pi_books_side": lambda: _img((230, 226, 214)),
        "pi_fridge": _fridge, "pi_fridge_front": lambda: _fridge(True), "pi_analyzer": _analyzer,
        "pi_cctv": lambda: _cctv(440), "pi_cctv2": lambda: _cctv(441),
        "pi_copier": lambda: _copier("side"), "pi_copier_front": lambda: _copier("front"), "pi_copier_top": lambda: _copier("top"),
        "pi_copier_panel": _copier_panel, "pi_tv": _tv, "pi_plastic": _plastic, "pi_magazine": _magazine,
        "pi_marker_blue": lambda: _marker((40, 70, 180)), "pi_marker_red": lambda: _marker((200, 40, 40)),
        "pi_eraser": lambda: _marker((40, 40, 44)), "pi_frame": _frame,
        "pi_pot_white": lambda: _pot((230, 230, 226)), "pi_pot_grey": lambda: _pot((90, 94, 98)),
        "pi_pot_terracotta": lambda: _pot((180, 100, 70)), "pi_soil": _soil,
    }
    for i in range(OFFICE["whiteboard"]["variants"]):
        out[f"pi_whiteboard_{i}"] = (lambda i=i: _board(i))
    for i in range(OFFICE["framed_print"]["variants"]):
        out[f"pi_print_{i}"] = (lambda i=i: _print(i))
    for i in range(OFFICE["office_plant"]["variants"]):
        out[f"pi_plant_{i}"] = (lambda i=i: _plant(i))
    return out


# Textures kept at more colours (pictures, screens, scribbles).
RICH = {"pi_cctv", "pi_cctv2", "pi_tv", "pi_reagents", "pi_books", "pi_binders", "pi_analyzer", "pi_coffee_machine"} | \
    {f"pi_whiteboard_{i}" for i in range(24)} | {f"pi_print_{i}" for i in range(12)} | {f"pi_plant_{i}" for i in range(8)}


def conference_icon():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([10, 30, 118, 80], fill=(96, 64, 42, 255), outline=(60, 40, 26, 255), width=4)
    d.rectangle([56, 78, 72, 112], fill=(60, 62, 66, 255))
    d.rectangle([34, 108, 94, 116], fill=(60, 62, 66, 255))
    return M.stylize(img, 12)
