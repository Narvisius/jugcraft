"""More of the plant kit (round 5): desks that come with different things on them, security lockers, first aid,
engineering, documentation, IT, cubicles, and furniture bigger than a block (wall screens, the oval table).

Same conventions as tools/plant_office.py. Extra keys:
  tops   (row pieces) a list of box lists, one per variant: what is on the desk; picked at random when placed
  multi  (width, rows, vertical): placed and broken as one; "screen" names the picture drawn across it, or
         "table" makes it an oval meeting table
A box may carry an 8th item, extra element JSON (a rotation).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_office3 as O3
import plant_office4 as O4
import plant_panels as PN

S = M.CELL

# ---------------------------------------------------------------------------------------------- desk tops
MONITOR = [(6.5, 12.5, 3, 9.5, 12.8, 5.5, "pi_monitor"), (7.6, 12.8, 3.6, 8.4, 15.5, 4.2, "pi_monitor"),
           (2.5, 15, 3.2, 13.5, 22, 4, "pi_monitor"),
           (2.8, 15.3, 4, 13.2, 21.7, 4.1, {"*": "pi_monitor", "south": "pi_office_screen!"}),
           (4, 12.5, 8, 12, 12.9, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"}),
           (12.8, 12.5, 8.5, 13.8, 12.9, 10, "pi_monitor")]
# Paperwork you can read (an operating procedure, a radiation work permit...), a pen cup with pens in it.
PAPERS = [(1.4, 12.5, 3.4, 6.2, 13.3, 10.2, {"*": "pi_pages", "up": "pi_doc_0!"}),
          (7.2, 12.5, 4.2, 11.6, 12.7, 10.4, {"*": "pi_paper", "up": "pi_doc_1!"})] + O4.pen_cup(12.4, 4.0, 12.5)
# Binders standing between two bookends, spines (with their labels) toward whoever sits there, and a coffee.
BINDERS = [b for k, (code, _) in enumerate(O3.DESK_BINDERS)
           for b in O3.binder(1.6 + k * O3.DESK_BINDER_W, 12.8, 9, 5.5, "desk:" + code, O3.DESK_BINDER_W, O3.DESK_BINDER_H)] + [
           (1, 12.5, 3.5, 1.6, 17.4, 9, "pi_steel_dark"), (8.2, 12.5, 3.5, 8.8, 17.4, 9, "pi_steel_dark"),
           (1, 12.5, 3.5, 8.8, 12.8, 9, "pi_steel_dark")] + O4.mug(10.6, 6.6, 12.5)
MUG_AT = (10.6 + O3.MUG_CENTRE, 15.6, 6.6 + O3.MUG_CENTRE)   # where the steam rises from (model pixels)
LAMP = [(2, 12.5, 3, 5, 12.9, 6, "pi_steel_dark"), (3.2, 12.9, 4.2, 3.8, 19, 4.8, "pi_steel_dark"),
        (2, 18.5, 4, 7, 20, 7, "pi_steel_dark")] + O4.phone(9, 3.6, 12.5) + [
        (8, 12.5, 8, 13, 12.7, 12, {"*": "pi_paper", "up": "pi_doc_2!"})]
LAPTOP = [(4, 12.5, 6, 12, 12.9, 11, {"*": "pi_laptop", "up": "pi_keyboard!"}),
          (4, 12.9, 5.4, 12, 18, 6, {"*": "pi_laptop", "south": "pi_office_screen!"}),
          (13, 12.5, 7, 15, 12.8, 11, {"*": "pi_paper", "up": "pi_notepad!"})]
ROLLS = [(1, 12.5, 4, 15, 14, 5.5, "pi_blueprint_roll"), (2, 12.5, 6, 14, 14, 7.5, "pi_blueprint_roll"),
         (3, 12.5, 9, 13, 12.7, 13, {"*": "pi_blueprint_0", "up": "pi_blueprint_0!"})]
# Two screens side by side, both inside the desk's own block (they used to overhang its sides, and desks in a row
# pushed their screens into each other: Justin's round 23).
DUAL = [(2.5, 12.5 + 0.5, 3, 5.5, 13.3, 5, "pi_monitor"), (10.5, 13, 3, 13.5, 13.3, 5, "pi_monitor"),
        (0.4, 15, 3.2, 7.7, 21, 4, {"*": "pi_monitor", "south": "pi_office_screen!"}),
        (8.3, 15, 3.2, 15.6, 21, 4, {"*": "pi_monitor", "south": "pi_tv!"}),
        (3.7, 13, 3.5, 4.3, 15, 4), (11.7, 13, 3.5, 12.3, 15, 4),
        (4, 13, 8, 12, 13.4, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"})]
DUAL[4] = DUAL[4] + ("pi_monitor",)
DUAL[5] = DUAL[5] + ("pi_monitor",)
BLOTTER = [(3, 13, 5, 13, 13.2, 11, {"*": "pi_leather", "up": "pi_leather!"}),
           (5.6, 13.2, 5.6, 10.4, 13.4, 10.4, {"*": "pi_paper", "up": "pi_doc_4!"}),
           (2, 13, 12, 8, 14.5, 13.5, {"*": "pi_walnut_edge", "south": "pi_nameplate!"})] + O4.pen_cup(13.2, 3.4, 13)
EXEC_LAMP = [(1.5, 13, 3, 4.5, 13.4, 6, "pi_brass"), (2.7, 13.4, 4.2, 3.3, 20, 4.8, "pi_brass"),
             (0.5, 19, 2.5, 5.5, 22, 7.5, "pi_lamp_shade")] + O4.phone(7.6, 3.4, 13) + [
             (6, 13, 8.5, 11, 13.3, 12.5, {"*": "pi_paper", "up": "pi_doc_3!"})]
PC_OPEN = [(1.5, 12, 3, 7.5, 19, 12, {"*": "pi_pc_case", "east": "pi_pc_inside!"}),
           (8.5, 12, 4, 14, 12.3, 8, {"*": "pi_circuit", "up": "pi_circuit!"}),
           (9, 12, 9, 13.5, 12.4, 10, "pi_ram"), (9, 12, 11, 13.5, 12.4, 12, "pi_ram"),
           (13.5, 12, 9.5, 15, 13.5, 13, "pi_steel_dark")]
TOOLS = [(1.5, 12, 4, 7, 12.3, 9, {"*": "pi_circuit", "up": "pi_circuit!"}),
         (8, 12, 4, 10, 13, 7, "pi_steel_dark"), (9.5, 12, 4.5, 14.5, 12.4, 5.5, "pi_solder_iron"),
         (8.5, 12, 8, 14.5, 12.6, 12, {"*": "pi_tool_case", "up": "pi_tool_case!"}),
         (2, 12, 10, 6, 12.3, 13, "pi_ram")]
DOCS = [(2.4, 12, 4, 7.4, 12.4, 11, {"*": "pi_pages", "up": "pi_doc_5!"}),
        (9, 12, 5, 14, 12.4, 11, {"*": "pi_monitor", "up": "pi_tablet!"})]

# The control room operators' consoles (Justin's round 23: "keep the black ones ... at least three blocks each ...
# have like little setups"): a black desk with a raised shelf along the back for the screens, joining up side by
# side; each block carries its own setup, everything inside its own block so joined pieces never overlap.
CONSOLE_BODY = [(0, 11, 0.5, 16, 12, 15.5, {"*": "pi_console_edge", "up": "pi_console_top!"}),
                (0, 0, 1, 16, 11, 2.5, "pi_console_body"),
                (0, 12, 0.5, 16, 13.2, 5, {"*": "pi_console_edge", "up": "pi_console_top!"})]


def _screen(x0, x1, y0, y1, tex, z=2.2):
    """A monitor standing on the console's shelf: its case, the picture toward the operator, its foot."""
    mid = (x0 + x1) / 2
    return [(x0, y0, z, x1, y1, z + 0.8, {"*": "pi_monitor", "south": f"{tex}!"}),
            (mid - 0.4, 13.2, z + 0.1, mid + 0.4, y0, z + 0.7, "pi_monitor"),
            (mid - 1.6, 13.2, z - 0.4, mid + 1.6, 13.5, z + 1.6, "pi_monitor")]


CONSOLE_KEYS = [(3.5, 12, 8, 12.5, 12.5, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"}),
                (13.2, 12, 8.5, 14.4, 12.5, 10.5, "pi_monitor")]
CONSOLE_TWIN = _screen(0.6, 7.6, 14, 20.4, "pi_plant_screen_a") + _screen(8.4, 15.4, 14, 20.4, "pi_plant_screen_b") + CONSOLE_KEYS
CONSOLE_WIDE = (_screen(1.0, 15.0, 14, 21.0, "pi_plant_screen_c") + O4.phone(11, 10, 12) +
                [(2, 12, 8, 10, 12.5, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"})])
CONSOLE_STACK = (_screen(2.0, 14.0, 14, 19.0, "pi_plant_screen_b") +
                 [(2.0, 19.2, 2.3, 14.0, 24.2, 3.1, {"*": "pi_monitor", "south": "pi_plant_screen_a!"}),
                  (7.6, 19.0, 2.5, 8.4, 19.2, 2.9, "pi_monitor"),
                  (1.5, 12, 7.5, 8.5, 12.4, 12.5, {"*": "pi_pages", "up": "pi_doc_5!"}),
                  (10, 12, 9, 14.5, 12.5, 11, {"*": "pi_monitor", "up": "pi_keyboard!"})])
CONSOLE_PHONE = (_screen(0.6, 7.6, 14, 20.4, "pi_plant_screen_c") + O4.phone(9.4, 6.5, 12) +
                 [(2.5, 12, 8, 7.5, 12.4, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"}),
                  (9.5, 12, 11.5, 14.5, 12.3, 14.5, {"*": "pi_paper", "up": "pi_doc_3!"})])

DESK_BODY = [(0, 11.5, 1, 16, 12.5, 15, {"*": "pi_laminate", "up": "pi_laminate!"}),
             (0, 3, 1.2, 16, 11.5, 2, "pi_desk_panel")]
GLASS_BODY = [(0, 11.5, 1, 16, 12, 15, {"*": "pi_glass_edge", "up": "pi_glass_top", "down": "pi_glass_top"}),
              (0, 10.8, 1.5, 16, 11.5, 2.5, "pi_steel_dark"), (0, 10.8, 13.5, 16, 11.5, 14.5, "pi_steel_dark")]
EXEC_BODY = [(0, 12, 0.5, 16, 13, 15.5, {"*": "pi_walnut_edge", "up": "pi_walnut!"}),
             (0, 1, 0.5, 16, 12, 1.5, {"*": "pi_walnut", "north": "pi_walnut!", "south": "pi_walnut!"}),
             (0, 0, 0.5, 16, 1, 2, "pi_brass")]


def _shift(boxes, dy):
    return [(b[0], b[1] + dy, b[2], b[3], b[4] + dy, b[5]) + tuple(b[6:]) for b in boxes]


PIECES = {
    "office_desk_long": {"display": "Office Desk (joins up)", "row": "desk", "boxes": DESK_BODY,
                         "left": [(0.5, 0, 1, 1.5, 11.5, 15, "pi_desk_panel")], "right": [(14.5, 0, 1, 15.5, 11.5, 15, "pi_desk_panel")],
                         "tops": [MONITOR, PAPERS, BINDERS, LAMP, LAPTOP, ROLLS], "glow": ["pi_office_screen"]},
    "executive_desk": {"display": "Executive Desk", "row": "exec", "boxes": EXEC_BODY,
                       "left": [(0, 0, 0.5, 1.2, 12, 15.5, {"*": "pi_walnut", "west": "pi_walnut!"})],
                       "right": [(14.8, 0, 0.5, 16, 12, 15.5, {"*": "pi_walnut", "east": "pi_walnut!"})],
                       "tops": [DUAL, BLOTTER, EXEC_LAMP], "glow": ["pi_office_screen", "pi_tv"]},
    "operator_console": {"display": "Operator Console", "row": "opcon", "boxes": CONSOLE_BODY,
                         "left": [(0, 0, 2.5, 1.2, 11, 15.5, "pi_console_body")],
                         "right": [(14.8, 0, 2.5, 16, 11, 15.5, "pi_console_body")],
                         "tops": [CONSOLE_TWIN, CONSOLE_WIDE, CONSOLE_STACK, CONSOLE_PHONE],
                         "glow": ["pi_plant_screen_a", "pi_plant_screen_b", "pi_plant_screen_c"]},
    "glass_desk": {"display": "Glass Desk", "row": "glass", "boxes": GLASS_BODY,
                   "left": [(0.4, 0, 1.5, 1.4, 11.5, 2.5, "pi_steel_dark"), (0.4, 0, 13.5, 1.4, 11.5, 14.5, "pi_steel_dark")],
                   "right": [(14.6, 0, 1.5, 15.6, 11.5, 2.5, "pi_steel_dark"), (14.6, 0, 13.5, 15.6, 11.5, 14.5, "pi_steel_dark")],
                   "tops": [_shift(MONITOR, -0.5), PC_OPEN, DOCS, TOOLS], "glow": ["pi_office_screen"]},
    "stretcher": {"display": "Stretcher Bed", "row": "stretcher", "boxes": [
        (0, 5.5, 2, 16, 6.5, 14, "pi_chrome"),
        (0, 6.5, 2.5, 16, 9.5, 13.5, {"*": "pi_mattress", "up": "pi_sheet!"}),
        (0, 6, 1.5, 16, 7, 2, "pi_chrome"), (0, 6, 14, 16, 7, 14.5, "pi_chrome")],
        "left": [(0.5, 0, 2.5, 2, 5.5, 4, "pi_chrome"), (0.5, 0, 12, 2, 5.5, 13.5, "pi_chrome"),
                 (0, 5.5, 2, 1, 13, 14, "pi_chrome"), (1.5, 9.5, 3.5, 6, 11, 12.5, {"*": "pi_sheet", "up": "pi_sheet!"})],
        "right": [(14, 0, 2.5, 15.5, 5.5, 4, "pi_chrome"), (14, 0, 12, 15.5, 5.5, 13.5, "pi_chrome"),
                  (15, 5.5, 2, 16, 11, 14, "pi_chrome")]},
    "cubicle_panel": {"display": "Cubicle Partition", "row": "cubicle", "boxes": [
        (0, 0.5, 7, 16, 20, 9, "pi_partition"), (0, 20, 6.8, 16, 20.6, 9.2, "pi_chrome"),
        (0, 0, 7.2, 16, 0.5, 8.8, "pi_steel_dark")],
        "left": [(0, 0, 6.5, 1, 20.8, 9.5, "pi_chrome")], "right": [(15, 0, 6.5, 16, 20.8, 9.5, "pi_chrome")]},
    "locker_security": {"display": "Security Locker", "stacks": True, "boxes": [
        (1, 0, 2, 15, 16, 3, "pi_locker_side"), (1, 0, 3, 2, 16, 15, "pi_locker_side"), (14, 0, 3, 15, 16, 15, "pi_locker_side"),
        (2, 0, 3, 14, 16, 3.2, "pi_locker_inside"),
        (0.4, 0.4, 15, 1.2, 15.6, 21.5, {"*": "pi_locker_side", "west": "pi_locker_door!", "east": "pi_locker_door_in!"})],
        "cap": [(0.8, 15.4, 1.8, 15.2, 16.1, 15.2, "pi_cabinet_dark"),
                (2, 12.4, 3, 14, 13, 14.5, "pi_locker_side"),
                (4.5, 13, 5, 9.5, 14.6, 10, {"*": "pi_security_cap", "up": "pi_security_cap"}),
                (10, 13, 6, 12, 16, 8, {"*": "pi_radio", "south": "pi_radio!"}),
                (2.5, 11, 8.6, 13.5, 11.6, 9.4, "pi_chrome"),
               ],
        "plinth": [(0.8, 0, 1.8, 15.2, 1, 15.2, "pi_cabinet_dark"),
                   (3.5, 1, 6, 7.5, 4, 12, "pi_boot_black"), (8.5, 1, 6, 12.5, 4, 12, "pi_boot_black")]},
    # ---- first aid
    "aed_cabinet": {"display": "Defibrillator (AED) Cabinet", "boxes": [
        (3, 3, 0, 13, 14, 3.5, {"*": "pi_cab_white", "south": "pi_aed!"}),
        (4, 15, 0, 12, 19, 0.4, {"*": "pi_cab_white", "south": "pi_aed_sign!"})], "glow": ["pi_aed_sign"]},
    "fire_extinguisher": {"display": "Fire Extinguisher Cabinet", "boxes": O4.EXTINGUISHER_CABINET},
    "medicine_cabinet": {"display": "First Aid Cabinet", "boxes": [
        (1, 5, 0, 15, 16, 4.5, {"*": "pi_cab_white", "south": "pi_medicine_front!"})]},
    "vitals_monitor": {"display": "Patient Monitor", "boxes": [
        # A five-castor stand, the pole, a basket for the leads, the monitor on its bracket with a handle on top,
        # the leads coiled down the pole.
        (3, 0.8, 7.5, 13, 1.5, 8.5, "pi_steel_dark"), (7.5, 0.8, 3, 8.5, 1.5, 13, "pi_steel_dark"),
        (3, 0, 7.4, 4.2, 0.8, 8.6, "pi_boot_black"), (11.8, 0, 7.4, 13, 0.8, 8.6, "pi_boot_black"),
        (7.4, 0, 3, 8.6, 0.8, 4.2, "pi_boot_black"), (7.4, 0, 11.8, 8.6, 0.8, 13, "pi_boot_black"),
        (7.5, 1.5, 7.5, 8.5, 14, 8.5, "pi_chrome"),
        (5.5, 7, 8.5, 10.5, 9.5, 11.5, {"*": "pi_steel_dark", "up": "pi_basket!"}),
        (6.5, 13, 7, 9.5, 14.5, 9, "pi_steel_dark"),
        (2.5, 14, 6, 13.5, 22, 9.5, {"*": "pi_monitor_case", "south": "pi_vitals!"}),
        (3.5, 13.5, 6.5, 12.5, 14, 9, "pi_monitor_case"),
        (5, 22, 7.2, 11, 23, 8.2, "pi_steel_dark"), (5, 21, 7.2, 5.8, 22, 8.2, "pi_steel_dark"),
        (10.2, 21, 7.2, 11, 22, 8.2, "pi_steel_dark"),
        (8.5, 4, 8, 9, 14, 8.5, "pi_cable"), (9, 10, 8, 10.5, 10.5, 8.5, "pi_cable")], "glow": ["pi_vitals"]},
    "glass_coffee_table": {"display": "Glass Coffee Table", "boxes": [
        (1, 6.5, 2, 15, 7.3, 14, {"*": "pi_glass_edge", "up": "pi_glass_top", "down": "pi_glass_top"}),
        (1.5, 0, 2.5, 2.5, 6.5, 3.5, "pi_chrome"), (13.5, 0, 2.5, 14.5, 6.5, 3.5, "pi_chrome"),
        (1.5, 0, 12.5, 2.5, 6.5, 13.5, "pi_chrome"), (13.5, 0, 12.5, 14.5, 6.5, 13.5, "pi_chrome"),
        (2.5, 2, 3, 13.5, 2.5, 13, "pi_walnut"),
] + O4.mug(3.6, 5.6, 7.3) + O4.magazines(8.4, 4.4, 7.3) + O4.magazines(4, 7.5, 2.5)[:1]},
    # ---- documents and engineering
    "filing_cabinet_open": {"display": "Filing Cabinet (open)", "boxes": O3.LOW_CABINET_OPEN},
    "filing_cabinet_tall": O3.TALL_CABINET,
    "filing_cabinet_tall_open": O3.TALL_CABINET_OPEN,
    "office_copier": {"display": "Office Copier", "multi": (2, 2, "box"), "copier": True},
    "drafting_table": {"display": "Drafting Table", "boxes": [
        (1, 0, 3, 2, 11, 4, "pi_steel_dark"), (14, 0, 3, 15, 11, 4, "pi_steel_dark"),
        (1, 0, 12, 2, 11, 13, "pi_steel_dark"), (14, 0, 12, 15, 11, 13, "pi_steel_dark"),
        (1, 4, 3, 15, 5, 13, "pi_steel_dark"),
        (0, 12, 2, 16, 13, 15, {"*": "pi_cab_white", "up": "pi_blueprint_1!"}, {"rotation": {"angle": 22.5, "axis": "x", "origin": [8, 12, 8]}}),
        (1, 15.5, 1, 2, 22, 2, "pi_steel_dark"), (1, 21, 1, 7, 22, 2, "pi_steel_dark"),
        (5, 19, 0.5, 9, 21, 3.5, "pi_lamp_shade")]},
    "blueprint_sheet": {"display": "Blueprint", "variants": 6, "vtex": "pi_blueprint", "boxes": [
        (1, 2, 0, 15, 15, 0.6, {"*": "pi_blueprint", "south": "pi_blueprint!"}),
        (2, 14.2, 0.6, 3, 15, 1, "pi_marker_red"), (13, 14.2, 0.6, 14, 15, 1, "pi_marker_red")]},
    # ---- bigger than a block
    "training_screen": {"display": "Training Screen", "multi": (3, 2, True), "screen": "training"},
    "plant_map_screen": {"display": "Plant Overview Screen", "multi": (3, 2, True), "screen": "plant_map"},
    "status_screen": {"display": "Plant Status Screen", "multi": (3, 2, True), "screen": "status"},
    "flat_tv": {"display": "Flat Screen TV", "multi": (3, 2, True), "screen": "blank"},
    "oval_table": {"display": "Oval Meeting Table", "multi": (5, 3, False), "table": True, "versions": 3},
    "lounge_table": {"display": "Oval Glass Coffee Table", "multi": (3, 2, False), "lounge": True},
    # ---- the warehouse
    "storage_rack": {"display": "Storage Rack", "multi": (3, 3, "rack"), "rack": True},
    "high_bay_light": {"display": "High-Bay Light", "boxes": O4.O7.HIGH_BAY, "glow": ["pi_highbay_lens"]},
    "light_hanger": {"display": "Light Hanger Chain", "boxes": O4.O7.LIGHT_HANGER},
    "large_print": {"display": "Large Framed Picture", "multi": (2, 1, True), "prints": 12},
}
# The diesel generator rooms' control and protection panels (tools/plant_panels.py).
PIECES.update(PN.PANELS)
PIECES.update(PN.RISERS)

CRAFTING = {
    "office_desk_long": (["SSS", "P P"], {"S": "minecraft:smooth_stone_slab", "P": "#minecraft:planks"}, 2),
    "executive_desk": (["SSS", "PPP", "P P"], {"S": "minecraft:dark_oak_slab", "P": "minecraft:dark_oak_planks"}, 2),
    "glass_desk": (["GGG", "N N"], {"G": "minecraft:glass_pane", "N": "#c:plates/iron"}, 2),
    "operator_console": (["GCG", "SSS", "S S"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit",
                                                 "S": "minecraft:smooth_stone_slab"}, 2),
    "stretcher": (["WWW", "N N"], {"W": "minecraft:white_wool", "N": "minecraft:iron_nugget"}, 2),
    "cubicle_panel": (["WWW", "WWW"], {"W": "minecraft:light_gray_wool"}, 4),
    "locker_security": (["PP", "PB", "PP"], {"P": "#c:plates/iron", "B": "minecraft:blue_wool"}, 2),
    "aed_cabinet": (["PGP", "PRP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "R": "minecraft:redstone"}, 1),
    "fire_extinguisher": (["N", "R", "R"], {"N": "minecraft:iron_nugget", "R": "minecraft:red_dye"}, 2),
    "medicine_cabinet": (["PGP", "PWP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "W": "minecraft:white_wool"}, 1),
    "vitals_monitor": (["GRG", " N ", " N "], {"G": "minecraft:black_stained_glass_pane", "R": "minecraft:redstone", "N": "minecraft:iron_nugget"}, 1),
    "filing_cabinet_open": (["PP", "PB"], {"P": "#c:plates/iron", "B": "minecraft:paper"}, 1),
    "filing_cabinet_tall": (["PP", "PP", "PP"], {"P": "#c:plates/iron"}, 2),
    "filing_cabinet_tall_open": (["PP", "PB", "PP"], {"P": "#c:plates/iron", "B": "minecraft:paper"}, 2),
    "office_copier": (["GCG", "PRP", "PPP"], {"G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone",
                                              "P": "#c:plates/iron"}, 1),
    "drafting_table": (["PPP", "S S"], {"P": "minecraft:paper", "S": "minecraft:stick"}, 1),
    "blueprint_sheet": (["BBB", "BPB", "BBB"], {"B": "minecraft:blue_dye", "P": "minecraft:paper"}, 2),
    "training_screen": (["GGG", "GCG"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit"}, 1),
    "plant_map_screen": (["GGG", "GCG", " M "], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "M": "minecraft:map"}, 1),
    "status_screen": (["GGG", "GCG", " R "], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:comparator"}, 1),
    "oval_table": (["SSS", "SSS", "N N"], {"S": "minecraft:dark_oak_slab", "N": "#c:plates/iron"}, 1),
    "lounge_table": (["GGG", "W W"], {"G": "minecraft:glass_pane", "W": "minecraft:dark_oak_log"}, 1),
    "storage_rack": (["SBS", "SBS", "SBS"], {"S": "#c:plates/iron", "B": "minecraft:barrel"}, 1),
    "high_bay_light": ([" N ", "PGP", " L "], {"N": "minecraft:iron_chain", "P": "#c:plates/iron", "G": "minecraft:glowstone", "L": "minecraft:glass_pane"}, 2),
    "light_hanger": (["N", "N", "S"], {"N": "minecraft:iron_chain", "S": "minecraft:string"}, 4),
    "flat_tv": (["GGG", "GRG"], {"G": "minecraft:black_stained_glass_pane", "R": "minecraft:redstone"}, 1),
    "large_print": (["SSS", "SPS", "SSS"], {"S": "minecraft:dark_oak_planks", "P": "minecraft:painting"}, 1),
    "glass_coffee_table": (["GGG", "N N"], {"G": "minecraft:glass_pane", "N": "minecraft:iron_nugget"}, 1),
}


# ---------------------------------------------------------------------------------------------- models

def oval_boxes(c, r, w, rows):
    """Block (c, r)'s slice of an oval tabletop over w x rows blocks (radius just inside the edges), and the
    pedestals under every other block of the middle row."""
    a, b = w * 8 - 1, rows * 8 - 1
    cx, cz = w * 8, rows * 8
    out = []
    for z in range(16):
        gz = r * 16 + z + 0.5 - cz
        half = 0 if gz * gz >= b * b else a * math.sqrt(1 - gz * gz / (b * b))
        x0, x1 = max(0, cx - half - c * 16), min(16, cx + half - c * 16)
        if x1 - x0 > 0.5:
            out.append((round(x0 * 4) / 4, 12, z, round(x1 * 4) / 4, 13, z + 1,
                        {"*": "pi_walnut_edge", "up": "pi_walnut_top", "down": "pi_walnut_edge"}))
    if r == rows // 2 and 0 < c < w - 1 and c % 2 == 1:
        out += [(6, 1, 6, 10, 12, 10, "pi_steel_dark"), (2, 0, 3, 14, 1, 13, "pi_steel_dark"), (3, 11, 3, 13, 12, 13, "pi_steel_dark")]
    return out


def screen_boxes(name, c, r, w, rows):
    """Block (c, r)'s piece of a wall screen: a thin panel on the wall showing its slice of the picture, with the
    bezel round the outside edges."""
    return [(0, 0, 0, 16, 16, 1.6, {"*": "pi_monitor", "south": f"pi_{name}_{r * w + c}!"})]


LPRINT_KINDS = [0, 1, 3, 4, 6, 7, 9, 11, 13, 14, 2, 10]


def lprint_boxes(c, v):
    """Block c's share of a big framed picture two blocks across: a frame standing off the wall round a recessed
    canvas (whose texture is the picture cut in two, mapped by position so the halves meet)."""
    whole = [(2.2, 2.7, 0, 29.8, 13.3, 0.8, {"*": "pi_frame", "south": f"pi_lprint_{v}_{c}"}),
             (1, 1.5, 0, 31, 2.7, 1.8, "pi_frame"), (1, 13.3, 0, 31, 14.5, 1.8, "pi_frame"),
             (1, 2.7, 0, 2.2, 13.3, 1.8, "pi_frame"), (29.8, 2.7, 0, 31, 13.3, 1.8, "pi_frame")]
    out = []
    for b in whole:
        x0, x1 = max(b[0], c * 16), min(b[3], c * 16 + 16)
        if x1 - x0 > 0.01:
            t = b[6]
            if isinstance(t, dict) and c * 16 > b[0] or isinstance(t, dict) and c * 16 + 16 < b[3]:
                t = dict(t)
            out.append((x0 - c * 16, b[1], b[2], x1 - c * 16, b[4], b[5], t))
    return out


def lprint_texture(v):
    """The picture for big print v, 256 x 128 over its two blocks: a mat, the panorama inside it."""
    import paintings
    img = Image.new("RGBA", (2 * S, S), (240, 238, 230, 255))
    pic = paintings.painted(LPRINT_KINDS[v], 408, 136, 950 + v).resize((204, 68), Image.LANCZOS).convert("RGBA")
    img.paste(pic, (26, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([25, 29, 230, 98], outline=(200, 196, 186, 255))
    return {f"pi_lprint_{v}_{c}": img.crop((c * S, 0, c * S + S, S)) for c in range(2)}


def multi_parts(info, version=0):
    w, rows, vertical = info["multi"]
    items = O4.O5.meeting_items(version) if info.get("versions") else []
    for p in range(w * rows):
        c, r = p % w, p // w
        if info.get("rack"):
            yield p, O4.O7.rack_part(c, r)
        elif info.get("lounge"):
            yield p, O4.O5.lounge_part(c, r)
        elif info.get("versions"):
            yield p, oval_boxes(c, r, w, rows) + O4.O5.slice_into(items, c, r)
        elif info.get("copier"):
            yield p, O3.copier_part(c, r)
        elif info.get("panel"):
            yield p, PN.part(info["panel"], c, r)
        elif vertical:
            yield p, screen_boxes(info["screen"], c, r, w, rows)
        else:
            yield p, oval_boxes(c, r, w, rows)


# ---------------------------------------------------------------------------------------------- textures

def _img(colour, size=S):
    return M.to_img(M.base(colour, size))


def _flat(colour):
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = colour
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _noise(img, seed, colours, count):
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    w, h = img.size
    for _ in range(count):
        d.point((r.randrange(w), r.randrange(h)), fill=r.choice(colours))
    return img


def _glow(img):
    a = np.asarray(img).copy()
    a[..., 3] = np.where(a[..., 3] > 0, 254, 0)
    return Image.fromarray(a, "RGBA")


def _phone():
    img = _img((30, 32, 36))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([10, 10, 50, 118], radius=10, fill=(20, 20, 22, 255))
    for k in range(12):
        d.rectangle([62 + (k % 3) * 20, 20 + (k // 3) * 24, 76 + (k % 3) * 20, 36 + (k // 3) * 24], fill=(70, 72, 78, 255))
    d.rectangle([62, 116, 116, 122], fill=(90, 200, 240, 255))
    return img


def _visitor_book():
    img = _img((240, 240, 232))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, S], outline=(40, 60, 110, 255), width=8)
    d.line([(64, 8), (64, S - 8)], fill=(170, 170, 160, 255), width=3)
    for y in range(20, S - 10, 10):
        d.line([(14, y), (58, y)], fill=(150, 160, 190, 255))
        d.line([(70, y), (114, y)], fill=(150, 160, 190, 255))
    return img


def _leaf_ball():
    img = _img((50, 120, 60))
    return _noise(img, 801, [(30, 90, 40, 255), (80, 160, 70, 255)], 1500)


def _pen_cup():
    return _img((40, 42, 46))


def _pens():
    img = _img((30, 30, 34))
    d = ImageDraw.Draw(img)
    for k, c in enumerate([(30, 60, 200), (200, 40, 40), (20, 20, 20), (240, 200, 40)]):
        d.ellipse([20 + k * 22, 40 + (k % 2) * 30, 34 + k * 22, 54 + (k % 2) * 30], fill=c + (255,))
    return img


def _mug():
    img = _img((236, 236, 230))
    d = ImageDraw.Draw(img)
    d.rectangle([30, 40, 90, 80], fill=(40, 90, 160, 255))
    return img


def _notepad():
    img = _img((250, 240, 140))
    d = ImageDraw.Draw(img)
    for y in range(20, S, 12):
        d.line([(8, y), (8 + random.Random(y).randint(40, 110), y)], fill=(90, 90, 100, 255), width=2)
    return img


def _laptop():
    return M.brushed((150, 154, 160), 802, "h", dirt=0.05)


def _blueprint(i):
    """A blueprint sheet: white lines on blue: a P&ID, a floor plan, a pump section, a cable route, a tower, a
    valve detail."""
    img = _img((26, 66, 140))
    d = ImageDraw.Draw(img)
    r = random.Random(820 + i)
    line = (220, 232, 250, 255)
    for x in range(0, S, 16):
        d.line([(x, 0), (x, S)], fill=(40, 82, 156, 255))
        d.line([(0, x), (S, x)], fill=(40, 82, 156, 255))
    kind = i % 6
    if kind == 0:     # P&ID: pipes, valves, a pump, a tank
        d.rectangle([10, 30, 40, 90], outline=line, width=2)
        d.line([(40, 50), (90, 50), (90, 100), (118, 100)], fill=line, width=2)
        d.polygon([(60, 44), (60, 56), (70, 50)], outline=line)
        d.polygon([(80, 44), (80, 56), (70, 50)], outline=line)
        d.ellipse([84, 86, 104, 106], outline=line, width=2)
    elif kind == 1:   # floor plan
        d.rectangle([10, 14, 118, 114], outline=line, width=3)
        for x in (44, 80):
            d.line([(x, 14), (x, 70)], fill=line, width=2)
        d.line([(10, 70), (118, 70)], fill=line, width=2)
        for x in (24, 60, 96):
            d.arc([x, 62, x + 14, 76], 180, 270, fill=line)
    elif kind == 2:   # pump section
        d.ellipse([30, 30, 98, 98], outline=line, width=2)
        d.ellipse([52, 52, 76, 76], outline=line, width=2)
        d.rectangle([60, 6, 68, 30], outline=line)
        d.line([(10, 64), (30, 64)], fill=line, width=2)
    elif kind == 3:   # cable route with dimensions
        pts = [(10, 100)] + [(r.randint(20, 110), r.randint(20, 110)) for _ in range(4)] + [(118, 20)]
        d.line(pts, fill=line, width=2)
        for x in range(14, 118, 26):
            d.line([(x, 120), (x + 20, 120)], fill=line)
    elif kind == 4:   # cooling tower elevation
        d.polygon([(30, 118), (44, 50), (84, 50), (98, 118)], outline=line)
        d.line([(44, 50), (40, 20), (88, 20), (84, 50)], fill=line)
    else:             # valve detail
        d.rectangle([40, 60, 88, 80], outline=line, width=2)
        d.line([(64, 60), (64, 20)], fill=line, width=2)
        d.ellipse([48, 12, 80, 24], outline=line, width=2)
        d.line([(10, 70), (40, 70)], fill=line, width=3)
        d.line([(88, 70), (118, 70)], fill=line, width=3)
    # Title block, bottom right.
    d.rectangle([84, 104, 124, 124], outline=line)
    d.line([(88, 112), (118, 112)], fill=line)
    return img


def _blueprint_roll():
    img = _img((200, 214, 236))
    d = ImageDraw.Draw(img)
    d.line([(0, 30), (S, 30)], fill=(40, 80, 150, 255), width=6)
    return img


def _leather():
    a = M.base((50, 30, 22), S)
    M.shade(a, M.grain(S, 803, 8))
    return M.to_img(a)


def _nameplate():
    img = M.brushed((196, 168, 100), 804, "h", dirt=0.02)
    d = ImageDraw.Draw(img)
    d.rectangle([10, 40, 118, 88], fill=(30, 26, 22, 255))
    for x in range(18, 110, 9):
        d.rectangle([x, 56, x + 5, 72], fill=(220, 196, 130, 255))
    return img


def _lamp_shade():
    return _img((30, 70, 50))


def _pc_case():
    return _img((40, 42, 46))


def _pc_inside():
    img = _img((26, 28, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 118, 118], fill=(30, 110, 60, 255))
    d.rectangle([30, 30, 60, 60], fill=(160, 164, 168, 255))   # cooler
    for y in (70, 80, 90):
        d.rectangle([20, y, 110, y + 4], fill=(60, 60, 64, 255))
    d.rectangle([70, 20, 76, 60], fill=(40, 40, 44, 255))
    d.line([(20, 100), (100, 30)], fill=(200, 40, 40, 255), width=2)
    return img


def _circuit():
    img = _img((30, 110, 60))
    d = ImageDraw.Draw(img)
    r = random.Random(805)
    for _ in range(16):
        x, y = r.randint(4, 110), r.randint(4, 110)
        d.rectangle([x, y, x + r.randint(6, 18), y + r.randint(6, 14)], fill=r.choice([(20, 20, 24, 255), (200, 180, 90, 255), (180, 180, 184, 255)]))
    for _ in range(12):
        d.line([(r.randint(0, S), r.randint(0, S)), (r.randint(0, S), r.randint(0, S))], fill=(190, 160, 70, 255))
    return img


def _ram():
    img = _img((30, 110, 60))
    d = ImageDraw.Draw(img)
    for x in range(8, S, 24):
        d.rectangle([x, 20, x + 16, 100], fill=(20, 20, 24, 255))
    return img


def _solder_iron():
    img = _img((60, 120, 200))
    d = ImageDraw.Draw(img)
    d.rectangle([90, 0, S, S], fill=(180, 180, 184, 255))
    return img


def _tool_case():
    img = _img((200, 60, 40))
    d = ImageDraw.Draw(img)
    for k in range(5):
        d.rectangle([12 + k * 22, 20, 24 + k * 22, 100], fill=(30, 30, 34, 255))
        d.rectangle([14 + k * 22, 22, 22 + k * 22, 60], fill=(190, 190, 194, 255))
    return img


def _console(part):
    """The operator console's black laminate top, its edge and its dark grey body panels."""
    if part == "top":
        a = M.base((34, 36, 40))
        M.shade(a, M.fbm(S, 711, 3, 32) * 5)
        M.shade(a, M.grain(S, 712, 3))
        return M.to_img(a)
    if part == "edge":
        return _img((22, 22, 24))
    img = _img((52, 54, 58))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, S - 4, S - 4], outline=(40, 42, 46, 255), width=3)
    for y in range(20, S - 10, 10):
        d.line([(18, y), (S - 19, y)], fill=(44, 46, 50, 255), width=2)
    return img


def _plant_screen(kind):
    """A plant computer display: a (dark) system mimic with values (a), trends and the alarm list (b), the overview of
    the unit's main parameters as bar graphs (c)."""
    img = _img((14, 20, 30))
    d = ImageDraw.Draw(img)
    r = random.Random({"a": 731, "b": 732, "c": 733}[kind])
    d.rectangle([0, 0, S, 10], fill=(30, 50, 90, 254))
    d.rectangle([4, 3, 40, 7], fill=(220, 226, 236, 254))
    if kind == "a":
        boxes = [(10, 24, 36, 60), (52, 20, 80, 44), (92, 30, 118, 70), (22, 82, 58, 110), (72, 78, 112, 108)]
        for x0, y0, x1, y1 in boxes:
            d.rectangle([x0, y0, x1, y1], outline=(90, 200, 230, 254), width=2)
            d.rectangle([x0 + 3, y1 - 8, x0 + 3 + r.randint(8, x1 - x0 - 6), y1 - 4], fill=(240, 190, 60, 254))
        for a, b in (((36, 42), (52, 32)), ((80, 32), (92, 50)), ((40, 82), (40, 60)), ((92, 78), (104, 70))):
            d.line([a, b], fill=(90, 200, 230, 254), width=2)
        d.ellipse([60, 54, 74, 68], outline=(70, 220, 110, 254), width=2)
    elif kind == "b":
        for k, colour in enumerate(((230, 80, 60), (70, 200, 230), (240, 200, 70))):
            pts, y = [], 30 + k * 12
            for x in range(6, 122, 6):
                y = max(16, min(66, y + r.randint(-4, 4)))
                pts.append((x, y))
            d.line(pts, fill=colour + (254,), width=2)
        d.rectangle([4, 72, S - 5, 74], fill=(60, 70, 90, 254))
        for k in range(6):
            y = 80 + k * 8
            d.rectangle([6, y, 10, y + 4], fill=((230, 60, 50) if k < 2 else (240, 190, 50)) + (254,))
            d.rectangle([14, y, 14 + r.randint(40, 100), y + 4], fill=(200, 206, 216, 254))
    else:
        for k in range(8):
            x = 10 + k * 14
            h = r.randint(30, 84)
            d.rectangle([x, 112 - h, x + 9, 112], fill=((70, 200, 110) if h < 76 else (240, 190, 50)) + (254,))
            d.rectangle([x, 116, x + 9, 120], fill=(200, 206, 216, 254))
    return img


def _tablet():
    img = _img((20, 20, 24))
    d = ImageDraw.Draw(img)
    d.rectangle([12, 10, 116, 118], fill=(230, 236, 244, 254))
    d.rectangle([12, 10, 116, 24], fill=(40, 90, 160, 254))
    return img


def _glass_top():
    """Clear glass with a faint green tint: see-through (kept translucent, not cut out), the same all over so a row
    of glass desks reads as one sheet."""
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (196, 224, 222)
    a[..., 3] = 96
    return Image.fromarray(a, "RGBA")


def _glass_edge():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (120, 176, 170)
    a[..., 3] = 170
    return Image.fromarray(a, "RGBA")


def _binder_side():
    return _img((40, 80, 160))


def _binder_spines():
    img = _img((30, 30, 34))
    d = ImageDraw.Draw(img)
    r = random.Random(830)
    x = 0
    while x < S:
        w = r.choice((14, 18, 22))
        col = r.choice([(40, 80, 160), (170, 40, 40), (30, 30, 34), (200, 200, 196), (40, 120, 70)])
        d.rectangle([x, 0, x + w - 1, S - 1], fill=col + (255,), outline=tuple(c // 2 for c in col) + (255,))
        d.rectangle([x + 3, 18, x + w - 4, 40], fill=(246, 246, 240, 255))
        d.ellipse([x + w // 2 - 3, S - 30, x + w // 2 + 3, S - 24], fill=(20, 20, 20, 255))
        x += w
    return img


def _binder_top():
    img = _img((230, 228, 220))
    d = ImageDraw.Draw(img)
    r = random.Random(830)
    x = 0
    while x < S:
        w = r.choice((14, 18, 22))
        col = r.choice([(40, 80, 160), (170, 40, 40), (30, 30, 34), (200, 200, 196), (40, 120, 70)])
        d.rectangle([x, 0, x + 2, S - 1], fill=col + (255,))
        d.rectangle([x + w - 3, 0, x + w - 1, S - 1], fill=col + (255,))
        x += w
    return img


def _basket():
    img = _img((50, 52, 56))
    d = ImageDraw.Draw(img)
    for k in range(0, S, 16):
        d.line([(k, 0), (k, S)], fill=(120, 124, 128, 255), width=3)
        d.line([(0, k), (S, k)], fill=(120, 124, 128, 255), width=3)
    return img


def _monitor_case():
    return M.painted((214, 218, 220), 831, chip_count=0, drip_count=0, dirt=0.04)


def _cable():
    return _img((60, 140, 220))


def _tinted(colour, alpha):
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = colour
    a[..., 3] = alpha
    return Image.fromarray(a, "RGBA")


def _lab_glass():
    img = _tinted((214, 232, 236), 70)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(240, 250, 252, 150), width=6)
    d.line([(26, 10), (26, S - 10)], fill=(250, 252, 255, 140), width=8)
    for y in range(40, S - 8, 22):
        d.line([(S - 30, y), (S - 12, y)], fill=(250, 252, 255, 150), width=3)   # graduations
    return img


def _label():
    img = _img((246, 246, 240))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 24], fill=(30, 90, 160, 255))
    for y in (44, 64, 84):
        d.line([(12, y), (100, y)], fill=(80, 80, 84, 255), width=6)
    d.polygon([(96, 92), (116, 92), (106, 112)], fill=(240, 180, 30, 255))
    return img


def _cooler_lower():
    img = _img((230, 228, 220))
    d = ImageDraw.Draw(img)
    for y in range(70, 118, 10):
        d.rectangle([20, y, 108, y + 4], fill=(150, 150, 146, 255))   # the vent
    return img


def _cooler_badge():
    img = _img((230, 228, 220))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([34, 40, 94, 80], radius=8, fill=(40, 110, 190, 255))
    d.rectangle([44, 56, 84, 62], fill=(240, 244, 250, 255))
    return img


def _drip_grate():
    img = _img((70, 72, 76))
    d = ImageDraw.Draw(img)
    for x in range(8, S, 14):
        d.rectangle([x, 8, x + 6, S - 8], fill=(30, 30, 32, 255))
    return img


def _cup_stack():
    img = _img((236, 236, 232))
    d = ImageDraw.Draw(img)
    for y in range(10, S, 12):
        d.line([(30, y), (98, y)], fill=(200, 200, 196, 255), width=3)
    return img


def _mattress():
    return _img((60, 110, 170))


def _sheet():
    img = _img((236, 240, 240))
    return _noise(img, 807, [(220, 226, 228, 255), (246, 248, 248, 255)], 800)


def _partition():
    a = M.base((104, 118, 132), S)
    M.shade(a, M.grain(S, 808, 7))
    return M.to_img(a)


def _security_jacket():
    img = _img((30, 38, 60))
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, S)], fill=(20, 26, 44, 255), width=3)
    d.rectangle([20, 34, 46, 46], fill=(220, 220, 214, 255))     # SECURITY flash
    d.rectangle([84, 30, 104, 46], fill=(200, 170, 60, 255))      # badge
    d.rectangle([0, 96, S, 104], fill=(190, 196, 200, 255))       # reflective band
    return img


def _security_cap():
    img = _img((26, 32, 52))
    d = ImageDraw.Draw(img)
    d.rectangle([50, 50, 78, 70], fill=(200, 170, 60, 255))
    return img


def _radio():
    img = _img((24, 24, 26))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 20, 108, 60], fill=(60, 120, 80, 255))
    for y in range(76, 120, 10):
        d.line([(20, y), (108, y)], fill=(50, 50, 54, 255), width=3)
    return img


def _boot_black():
    return _img((20, 20, 22))


def _aed():
    img = _img((238, 240, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 118, 118], outline=(30, 150, 80, 255), width=6)
    d.rectangle([30, 30, 98, 98], fill=(40, 170, 90, 255))
    d.polygon([(64, 84), (40, 58), (48, 44), (64, 54), (80, 44), (88, 58)], fill=(250, 250, 250, 255))
    d.polygon([(66, 48), (56, 66), (66, 66), (60, 82), (76, 60), (66, 60), (72, 48)], fill=(240, 200, 30, 255))
    return img


def _aed_sign():
    img = _img((40, 170, 90))
    d = ImageDraw.Draw(img)
    d.text((22, 50), "AED", fill=(255, 255, 255, 255))
    d.rectangle([20, 48, 108, 80], outline=(255, 255, 255, 255), width=3)
    return _glow(img)


def _extinguisher():
    img = M.painted((200, 30, 30), 809, chip_count=2, drip_count=0, dirt=0.1)
    d = ImageDraw.Draw(img)
    d.rectangle([30, 40, 98, 90], fill=(240, 240, 236, 255))
    for y in (50, 60, 70, 80):
        d.line([(36, y), (92, y)], fill=(60, 60, 60, 255))
    return img


def _fire_sign():
    img = _img((200, 30, 30))
    d = ImageDraw.Draw(img)
    d.polygon([(64, 20), (84, 70), (64, 100), (44, 70)], fill=(250, 250, 250, 255))
    return img


def _medicine_front():
    """A white wall cabinet with a glass door: shelves of bandages, plasters, gauze rolls and bottles, a red cross."""
    img = _img((240, 240, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 123, 123], outline=(190, 190, 186, 255), width=4)
    d.rectangle([52, 8, 76, 20], fill=(210, 40, 40, 255))
    d.rectangle([58, 2, 70, 26], fill=(210, 40, 40, 255))
    d.rectangle([10, 30, 117, 117], fill=(214, 226, 228, 255))
    r = random.Random(810)
    for shelf, y in enumerate((58, 86, 114)):
        d.line([(10, y), (117, y)], fill=(170, 176, 178, 255), width=3)
        x = 12
        while x < 110:
            w = r.choice((10, 14, 18))
            h = r.randint(14, 24)
            col = r.choice([(250, 250, 250), (230, 200, 160), (60, 120, 200), (200, 40, 40), (240, 240, 220)])
            if r.random() < 0.3:
                d.ellipse([x, y - h, x + w, y], fill=col + (255,))
            else:
                d.rectangle([x, y - h, x + w, y - 1], fill=col + (255,))
            x += w + 3
    d.line([(14, 34), (60, 110)], fill=(240, 248, 250, 255), width=2)
    return img


def _vitals():
    img = _img((10, 14, 16))
    d = ImageDraw.Draw(img)
    pts = []
    for x in range(0, 90, 3):
        k = x % 30
        y = 30 - (18 if k == 12 else -8 if k == 15 else 0)
        pts.append((x + 4, y))
    d.line(pts, fill=(70, 240, 110, 254), width=2)
    d.line([(4, 62), (30, 62), (40, 52), (50, 70), (90, 62)], fill=(240, 220, 60, 254), width=2)
    d.line([(4, 94), (90, 90)], fill=(80, 180, 250, 254), width=2)
    for k, c in enumerate([(70, 240, 110), (240, 220, 60), (80, 180, 250)]):
        d.rectangle([98, 20 + k * 34, 122, 40 + k * 34], fill=c + (254,))
    return _glow(img)


def _drawer_front():
    img = _img((178, 180, 176))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 123, 123], outline=(110, 112, 110, 255), width=3)
    d.rectangle([44, 30, 84, 44], fill=(90, 92, 94, 255))
    return img


def _folders():
    img = _img((60, 60, 60))
    d = ImageDraw.Draw(img)
    r = random.Random(811)
    for y in range(0, S, 9):
        d.rectangle([0, y, S, y + 6], fill=r.choice([(220, 190, 110), (210, 180, 100), (90, 140, 200), (200, 80, 70)]) + (255,))
        if r.random() < 0.5:
            x = r.randint(10, 90)
            d.rectangle([x, y - 2, x + 18, y + 2], fill=(240, 220, 150, 255))
    return img


def _brass():
    return M.brushed((190, 160, 90), 812, "h", dirt=0.03)


def _walnut_top():
    a = M.base((96, 64, 42), S)
    M.shade(a, M.streaks(S, 813, "h", count=140, amount=14))
    M.shade(a, M.fbm(S, 814, 3, 64) * 5)
    return M.to_img(a)


# ---- screens: one picture drawn across the whole screen, cut into its blocks

def _screen_picture(kind, w, rows):
    W, H = w * S, rows * S
    img = Image.new("RGBA", (W, H), (16, 20, 26, 255))
    d = ImageDraw.Draw(img)
    bez = 10
    d.rectangle([0, 0, W - 1, H - 1], outline=(30, 32, 36, 255), width=bez)
    inner = [bez, bez, W - bez - 1, H - bez - 1]
    if kind == "status":
        # The live page is drawn over this by the renderer: a dark panel, a header bar, faint grid lines.
        d.rectangle(inner, fill=(14, 22, 34, 254))
        d.rectangle([bez, bez, W - bez - 1, bez + 26], fill=(24, 48, 84, 254))
        for y in range(bez + 40, H - bez, 24):
            d.line([(bez + 6, y), (W - bez - 6, y)], fill=(20, 32, 48, 254))
    elif kind == "blank":
        # Switched off: black glass with a soft reflection across it.
        d.rectangle(inner, fill=(12, 13, 15, 255))
        for k in range(40):
            d.line([(bez + 40 + k * 3, bez), (bez + k * 3 - 40, H - bez - 1)], fill=(22, 24, 28, 255))
    elif kind == "training":
        d.rectangle(inner, fill=(240, 242, 246, 254))
        d.rectangle([bez, bez, W - bez - 1, bez + 40], fill=(20, 70, 130, 254))
        d.rectangle([bez + 20, bez + 12, bez + 220, bez + 28], fill=(240, 244, 250, 254))
        # Bullets on the left, a reactor diagram on the right.
        for k in range(6):
            y = bez + 66 + k * 26
            d.ellipse([bez + 24, y, bez + 34, y + 10], fill=(20, 70, 130, 254))
            d.rectangle([bez + 44, y + 2, bez + 44 + random.Random(k).randint(100, 150), y + 8], fill=(80, 88, 100, 254))
        cx = W * 0.72
        d.rectangle([cx - 40, 90, cx + 40, 210], outline=(40, 50, 60, 254), width=4)
        d.arc([cx - 40, 50, cx + 40, 130], 180, 360, fill=(40, 50, 60, 254), width=4)
        d.rectangle([cx - 18, 130, cx + 18, 200], fill=(70, 150, 230, 254))
        d.line([(cx + 40, 150), (cx + 100, 150)], fill=(220, 60, 50, 254), width=6)
        d.line([(cx + 40, 180), (cx + 100, 180)], fill=(60, 120, 220, 254), width=6)
        d.rectangle([bez + 20, H - bez - 26, bez + 120, H - bez - 16], fill=(220, 160, 40, 254))
    else:  # plant_map: the site from above, buildings labelled, lights showing what runs
        d.rectangle(inner, fill=(30, 46, 40, 254))
        d.rectangle([bez, bez, W - bez - 1, bez + 50], fill=(30, 60, 100, 254))   # the lake
        def bldg(x0, y0, x1, y1, col):
            d.rectangle([x0, y0, x1, y1], fill=col + (254,), outline=(220, 226, 230, 254), width=2)
        bldg(70, 80, 130, 140, (150, 150, 160))      # containment (dome)
        d.ellipse([78, 88, 122, 132], fill=(190, 190, 200, 254), outline=(230, 230, 236, 254), width=2)
        bldg(150, 76, 250, 120, (120, 130, 150))     # turbine hall
        bldg(40, 160, 100, 200, (110, 120, 130))     # aux
        bldg(120, 160, 160, 200, (110, 120, 130))    # fuel
        bldg(170, 150, 220, 200, (140, 120, 90))     # control
        bldg(240, 150, 280, 190, (120, 110, 100))    # diesels
        bldg(270, 70, 330, 110, (90, 100, 110))      # transformers
        for cx in (130, 290):
            d.ellipse([cx - 26, 214, cx + 26, H - bez - 4], fill=(200, 204, 210, 254), outline=(240, 240, 244, 254), width=2)
        d.line([(100, 110), (150, 98)], fill=(230, 80, 60, 254), width=4)   # main steam
        d.line([(250, 98), (270, 90)], fill=(240, 200, 60, 254), width=4)   # power out
        for x, y, col in ((100, 110, (80, 240, 110)), (200, 98, (80, 240, 110)), (300, 90, (80, 240, 110)), (260, 170, (240, 200, 60))):
            d.ellipse([x - 6, y - 6, x + 6, y + 6], fill=col + (254,))
        d.rectangle([W - bez - 120, H - bez - 50, W - bez - 10, H - bez - 10], fill=(16, 24, 30, 254), outline=(80, 90, 100, 254))
        for k, col in enumerate([(80, 240, 110), (240, 200, 60), (230, 80, 60)]):
            d.ellipse([W - bez - 112, H - bez - 46 + k * 12, W - bez - 104, H - bez - 38 + k * 12], fill=col + (254,))
            d.rectangle([W - bez - 98, H - bez - 44 + k * 12, W - bez - 30, H - bez - 40 + k * 12], fill=(140, 150, 160, 254))
    return img


def screen_slices(kind, w, rows):
    """The picture cut into blocks: part (c, r) counts rows from the bottom."""
    big = _screen_picture(kind, w, rows)
    out = {}
    for r in range(rows):
        for c in range(w):
            top = (rows - 1 - r) * S
            tile = big.crop((c * S, top, (c + 1) * S, top + S))
            out[f"pi_{kind}_{r * w + c}"] = tile
    return out


def _plant_top(i):
    """The plant seen from above: clusters of leaves on a transparent sheet."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(900 + i)
    green = [(40, 110, 50), (60, 140, 60), (30, 90, 40), (80, 160, 70)]
    radius = 30 if i % 8 in (1, 4, 7) else 54
    for _ in range(160):
        a, rr = r.random() * math.tau, radius * math.sqrt(r.random())
        cx, cy = 64 + math.cos(a) * rr, 64 + math.sin(a) * rr
        w = 5 if i % 8 in (0, 2) else 9
        d.ellipse([cx - w, cy - w / 2, cx + w, cy + w / 2], fill=r.choice(green) + (255,))
    return img


def makers():
    out = {
        "pi_phone": _phone, "pi_visitor_book": _visitor_book, "pi_leaf_ball": _leaf_ball, "pi_pen_cup": _pen_cup, "pi_pens": _pens,
        "pi_mug": _mug, "pi_notepad": _notepad, "pi_laptop": _laptop, "pi_blueprint_roll": _blueprint_roll,
        "pi_leather": _leather, "pi_nameplate": _nameplate, "pi_lamp_shade": _lamp_shade, "pi_pc_case": _pc_case,
        "pi_pc_inside": _pc_inside, "pi_circuit": _circuit, "pi_ram": _ram, "pi_solder_iron": _solder_iron,
        "pi_tool_case": _tool_case, "pi_tablet": _tablet, "pi_glass_top": _glass_top, "pi_glass_edge": _glass_edge,
        "pi_mattress": _mattress, "pi_sheet": _sheet, "pi_partition": _partition, "pi_security_jacket": _security_jacket,
        "pi_security_cap": _security_cap, "pi_radio": _radio, "pi_boot_black": _boot_black, "pi_aed": _aed,
        "pi_aed_sign": _aed_sign, "pi_extinguisher": _extinguisher, "pi_fire_sign": _fire_sign,
        "pi_medicine_front": _medicine_front, "pi_vitals": _vitals, "pi_drawer_front": _drawer_front,
        "pi_folders": _folders, "pi_brass": _brass, "pi_walnut_top": _walnut_top,
        "pi_binder_side": _binder_side, "pi_binder_spines": _binder_spines, "pi_binder_top": _binder_top,
        "pi_basket": _basket, "pi_monitor_case": _monitor_case, "pi_cable": _cable,
        "pi_lab_glass": _lab_glass, "pi_label": _label,
        "pi_liquid_blue": lambda: _tinted((60, 130, 230), 150), "pi_liquid_amber": lambda: _tinted((220, 140, 40), 160),
        "pi_liquid_green": lambda: _tinted((70, 190, 90), 150), "pi_liquid_clear": lambda: _tinted((200, 230, 240), 110),
        "pi_amber_glass": lambda: _tinted((120, 66, 22), 215), "pi_white_bottle": lambda: _img((236, 236, 230)),
        "pi_cap_black": lambda: _img((30, 30, 32)), "pi_cap_blue": lambda: _img((40, 80, 180)), "pi_cap_red": lambda: _img((190, 40, 40)),
        "pi_cooler_lower": _cooler_lower, "pi_cooler_badge": _cooler_badge, "pi_drip_grate": _drip_grate,
        "pi_bottle_plastic": lambda: _tinted((120, 180, 230), 90), "pi_bottle_water": lambda: _tinted((70, 140, 220), 120),
        "pi_cup_stack": _cup_stack,
        "pi_console_top": lambda: _console("top"), "pi_console_edge": lambda: _console("edge"),
        "pi_console_body": lambda: _console("body"), "pi_plant_screen_a": lambda: _plant_screen("a"),
        "pi_plant_screen_b": lambda: _plant_screen("b"), "pi_plant_screen_c": lambda: _plant_screen("c"),
    }
    for i in range(6):
        out[f"pi_blueprint_{i}"] = (lambda i=i: _blueprint(i))
    for i in range(8):
        out[f"pi_plant_top_{i}"] = (lambda i=i: _plant_top(i))
    out.update(O3.makers())
    out.update(O4.makers())
    out.update(PN.makers())
    for v in range(12):
        for tex, img in lprint_texture(v).items():
            out[tex] = (lambda img=img: img)
    for name, info in PIECES.items():
        if "screen" in info:
            w, rows, _ = info["multi"]
            for tex, img in screen_slices(info["screen"], w, rows).items():
                out[tex] = (lambda img=img: img)
    return out


# Kept see-through (translucent) instead of cut out.
TRANSLUCENT = O4.TRANSLUCENT | {"pi_glass_top", "pi_glass_edge", "pi_lab_glass", "pi_liquid_blue", "pi_liquid_amber", "pi_liquid_green",
               "pi_liquid_clear", "pi_amber_glass", "pi_bottle_plastic", "pi_bottle_water"}
RICH = {"pi_medicine_front", "pi_binder_spines", "pi_vitals", "pi_circuit", "pi_pc_inside", "pi_folders", "pi_aed",
        "pi_plant_screen_a", "pi_plant_screen_b", "pi_plant_screen_c"} | \
    {f"pi_blueprint_{i}" for i in range(6)} | {f"pi_plant_top_{i}" for i in range(8)} | \
    {f"pi_{k}_{p}" for k in ("training", "plant_map", "status", "blank") for p in range(6)}


PAINTINGS = {f"pi_lprint_{v}_{c}" for v in range(12) for c in range(2)} | {f"pi_print_{i}" for i in range(16)}


def icon(name):
    """Inventory icons for the pieces bigger than a block."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if name == "large_print":
        import paintings
        d.rectangle([2, 30, 126, 98], fill=(80, 52, 34, 255))
        img.paste(paintings.painted(0, 112, 56, 950).convert("RGBA"), (8, 36))
        return M.stylize(img, 24)
    if name in PN.PANELS:
        return PN.icon(name)
    if name == "office_copier":
        fr = O3._COPIER_TEX
        big = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
        for p_ in range(4):
            big.paste(fr[f"pi_copier_f_{p_}"], ((p_ % 2) * 256, (1 - p_ // 2) * 256))
        crop = big.crop((16, 150, 512, 512)).resize((124, 90))
        img.paste(crop, (2, 30))
        return M.stylize(img, 24)
    if name == "storage_rack":
        d.rectangle([10, 14, 118, 114], outline=(30, 60, 120, 255), width=6)
        for y in (46, 80):
            d.rectangle([10, y, 118, y + 6], fill=(214, 110, 30, 255))
        for x in (44, 78):
            d.rectangle([x, 14, x + 5, 114], fill=(30, 60, 120, 255))
        for bx, by in ((16, 90), (50, 90), (84, 56), (16, 22)):
            d.rectangle([bx, by, bx + 24, by + 22], fill=(196, 154, 100, 255), outline=(150, 110, 60, 255))
    elif name == "lounge_table":
        d.ellipse([6, 40, 122, 84], fill=(200, 226, 230, 255), outline=(150, 190, 196, 255), width=3)
        d.rectangle([30, 84, 50, 112], fill=(92, 58, 36, 255))
        d.rectangle([78, 84, 98, 112], fill=(92, 58, 36, 255))
    elif name == "oval_table":
        d.ellipse([6, 36, 122, 80], fill=(96, 64, 42, 255), outline=(60, 40, 26, 255), width=4)
        d.rectangle([40, 78, 50, 110], fill=(60, 62, 66, 255))
        d.rectangle([78, 78, 88, 110], fill=(60, 62, 66, 255))
    else:
        kind = PIECES[name]["screen"]
        pic = _screen_picture(kind, 3, 2).resize((120, 80))
        img.paste(pic, (4, 24))
        d.rectangle([60, 104, 68, 120], fill=(40, 42, 46, 255))
    return M.stylize(img, 16)
