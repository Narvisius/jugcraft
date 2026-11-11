"""Drone Tower data (docs/features/drone-tower.md): building materials, command-room furniture, the Tower
Core and their names, models, textures, loot and recipes.

The building materials are ordinary blocks meant for every big Jugcraft structure (the Drone Tower now; the
reactors and the black hole facility later), each made from a real material:
  reinforced concrete, steel girders, hazard plating, steel armour plate, laminated blast glass, hangar bay
  doors, aluminium cladding, tungsten-steel frame, carbon composite (pitch fibre in resin), silicon carbide
  armour (silicon + coke), depleted-uranium armour, graphene lattice (grown on copper), superconducting
  conduit (rare-earth tape), red light strips and warning lights.
The plain cube materials also come as stairs and slabs, for the tower's slopes and curves.

The furniture blocks have their own models (no stairs as chairs): operator chair, console desk, equipment
rack, ceiling light panel, cable tray; with acoustic wall panels, raised access floor tiles, carpet tiles and
concrete columns for the room itself. drone/JugcraftTower.java registers the same IDs (checked by
check_mod_data.py).
"""
import math
import random

import tower_realism_art
import tower_exchanges
import tower_master_console
import tripillar_fixtures
import room_kit_recipes

from PIL import Image

FEATURE = "drones"
MOD = "jugcraft"

# id: display, kind (cube | pillar | glass | light), light level, stairs and slab?
BUILDING = {
    "reinforced_concrete": {"display": "Reinforced Concrete", "kind": "cube", "variants": True},
    "steel_girder": {"display": "Steel Girder", "kind": "pillar"},
    "hazard_plating": {"display": "Hazard Plating", "kind": "cube", "variants": True},
    "steel_armor_plate": {"display": "Steel Armor Plate", "kind": "cube", "variants": True},
    "blast_glass": {"display": "Blast Glass", "kind": "glass"},
    "hangar_bay_door": {"display": "Hangar Bay Door", "kind": "cube"},
    "aluminum_cladding": {"display": "Aluminum Cladding", "kind": "cube", "variants": True},
    "tungsten_steel_frame": {"display": "Tungsten-Steel Frame", "kind": "cube", "variants": True},
    "carbon_composite_panel": {"display": "Carbon Composite Panel", "kind": "cube", "variants": True},
    "silicon_carbide_armor": {"display": "Silicon Carbide Armor", "kind": "cube", "variants": True},
    "depleted_uranium_armor": {"display": "Depleted Uranium Armor", "kind": "cube", "variants": True},
    "graphene_lattice": {"display": "Graphene Lattice", "kind": "cube", "variants": True},
    "superconducting_conduit": {"display": "Superconducting Conduit", "kind": "light", "light": 5},
    "red_light_strip": {"display": "Recessed Floor Light", "kind": "light", "light": 8},
    "warning_light": {"display": "Warning Light", "kind": "light", "light": 13},
    # Room surfaces for the command room.
    "acoustic_wall_panel": {"display": "Acoustic Wall Panel", "kind": "cube"},
    "access_floor_tile": {"display": "Access Floor Tile", "kind": "cube"},
    "carpet_tile": {"display": "Carpet Tile", "kind": "cube"},
    "concrete_column": {"display": "Concrete Column", "kind": "pillar"},
    # The Energy Exchange (a substation) and the Storage Exchange (a loading dock) on the field's edges.
    "transformer_casing": {"display": "Transformer Casing", "kind": "cube"},
    "cooling_fin": {"display": "Cooling Fin", "kind": "pillar"},
    "ceramic_insulator": {"display": "Ceramic Insulator", "kind": "pillar"},
    "copper_busbar": {"display": "Copper Busbar", "kind": "pillar"},
    "armored_conduit": {"display": "Armored Conduit", "kind": "pillar"},
    "dock_plating": {"display": "Dock Plating", "kind": "cube"},
    "intake_funnel": {"display": "Intake Funnel", "kind": "cube"},
    # The landing pad inside each tower hangar (a red charger glow in the middle).
    "hangar_pad": {"display": "Hangar Pad", "kind": "light", "light": 6},
    # The Tri-Pillar's amber lines (roof line, deck edges, floor guide lines, the ring round the holo table): a flush LED
    # fitting, a frosted amber diffuser in a steel bezel, so a run of them reads as a segmented light line.
    "amber_light_strip": {"display": "Amber Light Strip", "kind": "light", "light": 12},
}

# Furniture with real models: boxes (x0, y0, z0, x1, y1, z1, texture) in 1/16 block, for a block facing north
# (whoever uses it looks towards -z; the blockstate turns it). light: block light; glow: emissive textures.
FURNITURE = {
    "operator_chair": {"display": "Operator Chair", "light": 0, "boxes": [
        (2, 0, 7, 14, 1, 9, "frame"), (7, 0, 2, 9, 1, 14, "frame"), (7, 1, 7, 9, 6, 9, "chrome"),
        (3, 6, 3, 13, 7.5, 13, "frame"), (3.5, 7.5, 3.5, 12.5, 8.5, 12.5, "fabric"),
        (3.5, 8, 12, 12.5, 19, 13.5, "mesh"), (3, 8, 13, 13, 19.5, 14, "frame"), (5, 19.5, 12.5, 11, 22.5, 14, "fabric"),
        (2, 7.5, 9, 3, 11, 10, "frame"), (1.5, 11, 4.5, 3.5, 12, 10, "fabric"),
        (13, 7.5, 9, 14, 11, 10, "frame"), (12.5, 11, 4.5, 14.5, 12, 10, "fabric")],
        "shape": [(2, 0, 2, 14, 9, 14), (3, 9, 12, 13, 22, 14)]},
    "console_desk": {"display": "Console Desk", "light": 4, "boxes": [
        (0, 11, 0.5, 16, 12, 15.5, "desk"), (0, 0, 0.5, 16, 11, 3, "desk_body"),
        (0.5, 0, 3, 1.5, 11, 15, "desk_body"), (14.5, 0, 3, 15.5, 11, 15, "desk_body"),
        (2, 12, 9, 14, 12.6, 11.5, "keys"), (12.5, 12, 12, 13.8, 12.7, 14, "frame"),
        (3.5, 12, 2, 5, 12.5, 4.5, "frame"), (3.8, 12.5, 3, 4.7, 15, 3.8, "chrome"),
        (1, 14.5, 3, 7.5, 20.5, 3.8, "frame"), (1.4, 14.9, 3.75, 7.1, 20.1, 3.9, "screen_a"),
        (11, 12, 2, 12.5, 12.5, 4.5, "frame"), (11.3, 12.5, 3, 12.2, 15, 3.8, "chrome"),
        (8.5, 14.5, 3, 15, 20.5, 3.8, "frame"), (8.9, 14.9, 3.75, 14.6, 20.1, 3.9, "screen_b")],
        "shape": [(0, 0, 0, 16, 12, 16), (1, 12, 2, 15, 21, 5)]},
    "equipment_rack": {"display": "Equipment Rack", "light": 2, "boxes": [
        (1, 0, 1, 15, 16, 15, "rack"), (1.5, 0.5, 15, 14.5, 15.5, 15.4, "rack_door")],
        "shape": [(1, 0, 1, 15, 16, 15)]},
    "ceiling_light_panel": {"display": "Ceiling Light Panel", "light": 15, "boxes": [
        (1, 14.5, 1, 15, 16, 15, "chrome"), (2, 14.4, 2, 14, 14.5, 14, "light_panel")],
        "shape": [(1, 14.5, 1, 15, 16, 15)], "no_facing": True},
    "cable_tray": {"display": "Cable Tray", "light": 0, "boxes": [
        (0, 12, 4, 16, 12.5, 12, "chrome"), (0, 12.5, 4, 16, 14, 4.5, "chrome"), (0, 12.5, 11.5, 16, 14, 12, "chrome"),
        (0, 12.5, 5, 16, 13.5, 7, "cable_red"), (0, 12.5, 7.5, 16, 13.5, 11, "cable_black"),
        (7.5, 14, 7.5, 8.5, 16, 8.5, "chrome")],
        "shape": [(0, 12, 4, 16, 16, 12)]},
}
# The rack's front (towards whoever uses it, +z) gets server slots and a column of small status LEDs.
FURNITURE["equipment_rack"]["boxes"] += [(2.5, y, 15.4, 13.5, y + 0.5, 15.6, "frame") for y in range(2, 15, 2)] + \
    [(12, y + 0.6, 15.5, 12.8, y + 1.2, 15.7, "led") for y in range(2, 15, 2)]
# The Drone Depot Terminal is the command desk's terminal: a console with one big display (drone/DroneTerminalBlock).
TERMINAL_MODEL = {"display": None, "boxes": [
    (0, 11, 0.5, 16, 12, 15.5, "desk"), (0, 0, 0.5, 16, 11, 3, "desk_body"),
    (0.5, 0, 3, 1.5, 11, 15, "desk_body"), (14.5, 0, 3, 15.5, 11, 15, "desk_body"),
    (2, 12, 9, 14, 12.6, 11.5, "keys"), (6.5, 12, 2, 9.5, 12.5, 5, "frame"), (7.5, 12.5, 3, 8.5, 16, 4, "chrome"),
    (0.5, 15, 3, 15.5, 25, 4, "frame"), (1, 15.5, 3.9, 15, 24.5, 4.1, "screen_c")]}
FURNITURE_GLOW = {"screen_a", "screen_b", "screen_c", "light_panel", "led"}
FURNITURE_TEXTURES = ["frame", "chrome", "fabric", "mesh", "desk", "desk_body", "keys", "screen_a", "screen_b", "rack",
                      "rack_door", "light_panel", "cable_red", "cable_black", "led", "screen_c"]

# The Drone Tower Core: the block the tower grows from, set into the ground where the middle of the hall will be.
CORE = {"drone_tower_core": {"display": "Drone Tower Core"}}
# The two exchange ports: anything that delivers energy or items into them feeds the nearest drone depot.
PORTS = {"energy_exchange_port": {"display": "Energy Exchange Port"}, "cargo_exchange_port": {"display": "Cargo Exchange Port"}}

# Tower modules were the upgrade currency until October 2026; every step now costs the blocks it places
# (tower/TowerCosts.java), and the modules are gone.


def variant_blocks():
    return [f"{b}_{v}" for b, info in BUILDING.items() if info.get("variants") for v in ("stairs", "slab")]


SURFACE_LIGHTS = {"wall_task_light": "Sealed Wall Task Light", "linear_ceiling_light": "Sealed Linear Ceiling Light"}


def tower_blocks():
    return (list(BUILDING) + variant_blocks() + list(FURNITURE) + list(CORE) + list(PORTS)
            + list(tower_realism_art.DETAIL_BLOCKS) + list(tower_exchanges.STATIONS)
            + list(SURFACE_LIGHTS) + [tower_master_console.BLOCK_ID] + list(tripillar_fixtures.FIXTURES)
            + list(room_kit_recipes.PIECES))


def tower_items():
    return []


# ---------------------------------------------------------------- recipes

STEEL_PLATE, STEEL_INGOT = "#c:plates/steel", "#c:ingots/steel"
CRAFTING = {
    "reinforced_concrete": (["CCC", "CSC", "CCC"], {"C": "minecraft:gray_concrete", "S": STEEL_INGOT}, 8),
    "steel_girder": (["SSS", " S ", "SSS"], {"S": STEEL_INGOT}, 6),
    "hazard_plating": (["PY", "BP"], {"P": STEEL_PLATE, "Y": "minecraft:yellow_dye", "B": "minecraft:black_dye"}, 4),
    # Rolled armour plate bolted up (its own shape: 2x2 steel plates was already corrugated iron and skid iron).
    "steel_armor_plate": (["PNP", "PPP", "PNP"], {"P": STEEL_PLATE, "N": "minecraft:iron_nugget"}, 4),
    "blast_glass": (["GSG", "G G", "GSG"], {"G": "minecraft:glass", "S": "jugcraft:plastic_sheet"}, 6),
    "hangar_bay_door": (["PPP", "GPG"], {"P": STEEL_PLATE, "G": "#c:gears/steel"}, 3),
    "aluminum_cladding": (["PP", "PP"], {"P": "#c:plates/aluminum"}, 4),
    "tungsten_steel_frame": (["TST", "S S", "TST"], {"T": "#c:ingots/tungsten", "S": STEEL_INGOT}, 4),
    "carbon_composite_panel": (["KPK", "PKP", "KPK"], {"K": "jugcraft:coke", "P": "jugcraft:plastic_sheet"}, 4),
    "silicon_carbide_armor": (["SKS", "KSK", "SKS"], {"S": "jugcraft:silicon", "K": "jugcraft:coke"}, 4),
    "depleted_uranium_armor": (["UPU", "PUP", "UPU"], {"U": "#c:ingots/uranium", "P": STEEL_PLATE}, 4),
    "graphene_lattice": (["KKK", "KCK", "KKK"], {"K": "jugcraft:coke", "C": "#c:plates/copper"}, 2),
    "superconducting_conduit": (["PPP", "TTT", "PPP"], {"P": STEEL_PLATE, "T": "jugcraft:superconducting_tape"}, 4),
    "red_light_strip": (["GGG", "RDR"], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "D": "minecraft:red_dye"}, 6),
    "amber_light_strip": (["GGG", "RDR"], {"G": "minecraft:glass_pane", "R": "minecraft:glowstone_dust", "D": "minecraft:orange_dye"}, 6),
    "warning_light": (["G", "L", "P"], {"G": "minecraft:red_stained_glass", "L": "minecraft:redstone_lamp", "P": "#c:plates/iron"}, 2),
    "acoustic_wall_panel": (["WWW", "WPW", "WWW"], {"W": "minecraft:gray_wool", "P": "#c:plates/iron"}, 8),
    "access_floor_tile": (["PPP", "S S"], {"P": "#c:plates/iron", "S": "#c:ingots/steel"}, 6),
    "carpet_tile": (["CC", "CC"], {"C": "minecraft:gray_carpet"}, 4),
    "concrete_column": (["C", "C", "C"], {"C": "jugcraft:reinforced_concrete"}, 3),
    "operator_chair": (["  W", "WWW", "SIS"], {"W": "minecraft:black_wool", "S": STEEL_INGOT, "I": "#c:ingots/iron"}, 1),
    "console_desk": (["GCG", "PPP", "P P"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit",
                                            "P": "#c:plates/iron"}, 1),
    "equipment_rack": (["PCP", "PCP", "PCP"], {"P": STEEL_PLATE, "C": "jugcraft:basic_circuit"}, 2),
    "ceiling_light_panel": (["PPP", "GLG"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "L": "minecraft:glowstone_dust"}, 4),
    "cable_tray": (["P P", "WPW"], {"P": "#c:plates/iron", "W": "#c:wires/copper"}, 8),
    "transformer_casing": (["PWP", "WIW", "PWP"], {"P": STEEL_PLATE, "W": "#c:wires/copper", "I": "#c:ingots/iron"}, 2),
    "cooling_fin": (["A A", "AAA", "A A"], {"A": "#c:plates/aluminum"}, 4),
    "ceramic_insulator": (["T", "T", "T"], {"T": "minecraft:white_terracotta"}, 3),
    "copper_busbar": (["C", "C", "C"], {"C": "#c:plates/copper"}, 3),
    "armored_conduit": (["PWP"], {"P": STEEL_PLATE, "W": "#c:wires/copper"}, 3),
    "dock_plating": (["PP", "SS"], {"P": "#c:plates/iron", "S": "minecraft:smooth_stone"}, 4),
    "intake_funnel": (["P P", "P P", " P "], {"P": STEEL_PLATE}, 2),
    "hangar_pad": (["PRP", "PCP"], {"P": STEEL_PLATE, "R": "minecraft:redstone", "C": "#c:wires/copper"}, 4),
    "energy_exchange_port": (["PRP", "WCW", "PRP"], {"P": STEEL_PLATE, "R": "minecraft:redstone", "W": "#c:wires/copper",
                                                    "C": "jugcraft:advanced_circuit"}, 1),
    "cargo_exchange_port": (["PHP", "BCB", "PHP"], {"P": STEEL_PLATE, "H": "minecraft:hopper", "B": "minecraft:barrel",
                                                   "C": "jugcraft:basic_circuit"}, 1),
    # The core builds the control building itself and runs the whole tower: a mid-game machine (Justin, Oct 2026).
    "drone_tower_core": (["BPB", "CMC", "BPB"], {"B": "#c:storage_blocks/steel", "P": "jugcraft:processor",
                                                "C": "jugcraft:advanced_circuit", "M": "jugcraft:drone_depot_terminal"}, 1),
}
ITEM_FEATURES = {"plastic_sheet": ["crude_oil"], "coke": [], "silicon": ["silicon"], "processor": ["silicon"],
                 "superconducting_tape": ["rare_earths", "silicon"]}
CRAFTING.update(tower_realism_art.CRAFTING)
CRAFTING.update(tower_exchanges.CRAFTING)
CRAFTING.update(tower_master_console.CRAFTING)
CRAFTING.update(tripillar_fixtures.CRAFTING)
CRAFTING.update(room_kit_recipes.CRAFTING)     # the upper floor's room kits (tools/room_kit.py), each a shape of its own
# Each light its own shape (they all had the ceiling light panel's, so only one of the three could be crafted).
CRAFTING.update({
    "linear_ceiling_light": (["PLP", "GGG"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "L": "minecraft:glowstone_dust"}, 4),
    "wall_task_light": (["PG", "PL"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "L": "minecraft:glowstone_dust"}, 2),
})


def variant_recipes():
    out = {}
    for block, info in BUILDING.items():
        if info.get("variants"):
            out[f"{block}_stairs"] = (["B  ", "BB ", "BBB"], {"B": f"jugcraft:{block}"}, 4)
            out[f"{block}_slab"] = (["BBB"], {"B": f"jugcraft:{block}"}, 6)
    return out


# ---------------------------------------------------------------- textures (16x16, deterministic)

def _img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _fill(img, fn, seed):
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            c = fn(x, y, rnd)
            if c is not None:
                img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,)))
    return img


def _noise(base, rnd, n=6):
    d = rnd.randint(-n, n)
    return tuple(v + d for v in base)


def plate(base, seams="quad", rivets=True, noise=6):
    """A matte armour/cladding plate: base colour with noise, dark seams, light rivets."""
    dark = tuple(v * 0.62 for v in base)
    light = tuple(min(255, v * 1.22) for v in base)

    def fn(x, y, rnd):
        if x == 0 or y == 0:
            return dark
        if seams == "quad" and (x == 8 or y == 8):
            return tuple(v * 0.72 for v in base)
        if seams == "rows" and y in (5, 11):
            return tuple(v * 0.72 for v in base)
        if rivets and (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13), (6, 2), (2, 6)):
            return light
        return _noise(base, rnd, noise)
    return fn


def draw_textures():
    """{texture name: Image} for every tower block texture (block/<name>)."""
    tex = {}
    tex["reinforced_concrete"] = _fill(_img(), lambda x, y, r: (78, 78, 76) if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12))
                                       else (112, 112, 108) if x == 0 or y == 0 else _noise((136, 136, 131), r, 7), 1)
    tex["steel_girder"] = _fill(_img(), lambda x, y, r: (52, 55, 61) if x in (0, 15) else (70, 74, 80) if x in (1, 14)
                                else (40, 42, 46) if 4 <= x <= 11 and y % 4 == 0 else _noise((84, 88, 96), r, 4), 2)
    tex["steel_girder_top"] = _fill(_img(), lambda x, y, r: (84, 88, 96) if (y in (1, 2, 13, 14) or 7 <= x <= 8)
                                    and 1 <= x <= 14 and 1 <= y <= 14 else (26, 27, 30), 3)
    tex["hazard_plating"] = _fill(_img(), lambda x, y, r: (40, 42, 46) if x == 0 or y == 0 else
                                  _noise((214, 170, 38), r, 8) if ((x + y) // 4) % 2 else _noise((30, 30, 32), r, 3), 4)
    tex["steel_armor_plate"] = _fill(_img(), plate((72, 78, 88)), 5)
    tex["blast_glass"] = _fill(_img(), lambda x, y, r: (52, 55, 61, 255) if x in (0, 15) or y in (0, 15)
                               else (60, 50, 54, 150) if (x - y) % 7 == 0 else (36, 30, 32, 120), 6)
    tex["hangar_bay_door"] = _fill(_img(), lambda x, y, r: (30, 32, 36) if y % 3 == 0 else
                                   (214, 170, 38) if y >= 14 and ((x // 2) % 2) else _noise((64, 70, 80), r, 4), 7)
    tex["aluminum_cladding"] = _fill(_img(), plate((150, 156, 164), seams="rows", rivets=False, noise=5), 8)
    tex["tungsten_steel_frame"] = _fill(_img(), lambda x, y, r: (40, 41, 45) if (x - y) % 8 == 0 or (x + y) % 8 == 7
                                        or x in (0, 15) or y in (0, 15) else _noise((62, 64, 70), r, 4), 9)
    tex["carbon_composite_panel"] = _fill(_img(), lambda x, y, r: (24, 25, 28) if x == 0 or y == 0 else
                                          (46, 48, 53) if ((x // 2 + y // 2) % 2) else (34, 35, 39), 10)
    tex["silicon_carbide_armor"] = _fill(_img(), lambda x, y, r: (36, 40, 48) if (y % 5 == 0 or (x + (y // 5) * 3) % 6 == 0)
                                         else _noise((62, 68, 80), r, 5), 11)
    tex["depleted_uranium_armor"] = _fill(_img(), plate((58, 66, 56)), 12)
    tex["graphene_lattice"] = _fill(_img(), lambda x, y, r: (52, 54, 60) if ((x + 2 * (y % 2)) % 4 == 0 and y % 2 == 0)
                                    or (y % 4 == 1 and x % 4 == 2) else _noise((28, 30, 34), r, 2), 13)
    tex["superconducting_conduit"] = _fill(_img(), lambda x, y, r: (60, 62, 68) if x <= 2 or x >= 13 else
                                           (120, 28, 26) if 5 <= x <= 10 else (40, 42, 46), 14)
    tex["red_light_strip"] = _fill(_img(), lambda x, y, r: (60, 62, 68) if y <= 1 or y >= 14 else
                                   (150, 38, 30) if 4 <= y <= 11 else (90, 24, 20), 15)
    tex["amber_light_strip"] = _fill(_img(), lambda x, y, r: (58, 60, 66) if x in (0, 15) or y in (0, 15) else
                                     (255, 196, 108) if 4 <= x <= 11 and 4 <= y <= 11 else (232, 140, 48), 17)
    tex["warning_light"] = _fill(_img(), lambda x, y, r: (52, 55, 61) if x in (0, 15) or y in (0, 15) else
                                 (235, 62, 44) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 30 else (110, 26, 20), 16)
    tex["acoustic_wall_panel"] = _fill(_img(), lambda x, y, r: (44, 46, 50) if x == 0 or y == 0 else
                                       _noise((62, 64, 68), r, 3) if (x + y) % 2 else _noise((58, 60, 64), r, 3), 17)
    tex["access_floor_tile"] = _fill(_img(), lambda x, y, r: (60, 62, 66) if x in (0, 15) or y in (0, 15) else
                                     (74, 76, 80) if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)) else _noise((88, 90, 94), r, 3), 18)
    tex["carpet_tile"] = _fill(_img(), lambda x, y, r: _noise((40, 42, 46), r, 4) if (x // 4 + y // 4) % 2 else _noise((36, 38, 42), r, 4), 19)
    tex["concrete_column"] = _fill(_img(), lambda x, y, r: (100, 100, 96) if x in (0, 15) else
                                   (112, 112, 108) if x in (1, 14) else _noise((132, 132, 128), r, 6), 20)
    tex["concrete_column_top"] = _fill(_img(), lambda x, y, r: (100, 100, 96) if x in (0, 15) or y in (0, 15) else _noise((126, 126, 122), r, 5), 21)
    tex["transformer_casing"] = _fill(_img(), lambda x, y, r: (34, 36, 40) if x in (0, 15) or y in (0, 15) else
                                      (44, 46, 52) if x % 3 == 1 else _noise((60, 64, 72), r, 4), 22)
    tex["cooling_fin"] = _fill(_img(), lambda x, y, r: (26, 27, 30) if x % 2 == 0 else _noise((52, 55, 62), r, 3), 23)
    tex["cooling_fin_top"] = _fill(_img(), lambda x, y, r: (26, 27, 30) if x % 2 == 0 or y in (0, 15) else (60, 64, 72), 24)
    tex["ceramic_insulator"] = _fill(_img(), lambda x, y, r: (150, 146, 138) if y % 4 == 3 else
                                     (40, 42, 46) if x <= 1 or x >= 14 else _noise((214, 208, 198), r, 4), 25)
    tex["ceramic_insulator_top"] = _fill(_img(), lambda x, y, r: (90, 90, 92) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 6 else
                                         (214, 208, 198) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 49 else (40, 42, 46), 26)
    tex["copper_busbar"] = _fill(_img(), lambda x, y, r: (90, 52, 30) if x in (0, 5, 10, 15) else
                                 (200, 120, 70) if x in (2, 7, 12) else _noise((176, 104, 58), r, 6), 27)
    tex["copper_busbar_top"] = _fill(_img(), lambda x, y, r: (90, 52, 30) if x in (0, 5, 10, 15) or y in (0, 15) else (176, 104, 58), 28)
    tex["armored_conduit"] = _fill(_img(), lambda x, y, r: (34, 36, 40) if y in (0, 15) or x % 5 == 0 else
                                   (120, 28, 26) if 6 <= y <= 9 else _noise((60, 64, 72), r, 4), 29)
    tex["armored_conduit_top"] = _fill(_img(), lambda x, y, r: (120, 28, 26) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 10 else
                                       (34, 36, 40) if (x - 7.5) ** 2 + (y - 7.5) ** 2 > 50 else (60, 64, 72), 30)
    tex["dock_plating"] = _fill(_img(), lambda x, y, r: (70, 70, 68) if x in (0, 15) or y in (0, 15) else
                                (120, 120, 116) if (x + 2 * y) % 6 == 0 and (x // 3 + y // 3) % 2 else _noise((92, 92, 88), r, 5), 31)
    tex["intake_funnel"] = _fill(_img(), lambda x, y, r: (214, 170, 38) if (x in (0, 15) or y in (0, 15)) and ((x + y) // 2) % 2 else
                                 (20, 20, 22) if 4 <= x <= 11 and 4 <= y <= 11 else _noise((64, 66, 70), r, 4), 32)
    tex["energy_exchange_port"] = _fill(_img(), lambda x, y, r: (214, 170, 38) if (x in (0, 15) or y in (0, 15)) and ((x + y) // 2) % 2 else
                                        (30, 30, 32) if x in (0, 15) or y in (0, 15) else
                                        (235, 62, 44) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 8 else
                                        (60, 64, 72) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 30 else (40, 42, 46), 33)
    tex["cargo_exchange_port"] = _fill(_img(), lambda x, y, r: (214, 170, 38) if (x in (0, 15) or y in (0, 15)) and ((x + y) // 2) % 2 else
                                       (30, 30, 32) if x in (0, 15) or y in (0, 15) else
                                       (226, 170, 40) if (x in (3, 12) and 3 <= y <= 12) or (y in (3, 12) and 3 <= x <= 12) else
                                       (16, 16, 18) if 4 <= x <= 11 and 4 <= y <= 11 else (150, 156, 164), 34)
    # Furniture textures.
    fur = {
        "frame": lambda x, y, r: _noise((30, 30, 33), r, 2), "chrome": lambda x, y, r: _noise((150, 154, 160), r, 6),
        "fabric": lambda x, y, r: _noise((36, 36, 40), r, 4), "mesh": lambda x, y, r: (24, 24, 26) if (x + y) % 2 else (52, 52, 56),
        "desk": lambda x, y, r: _noise((72, 74, 78), r, 3), "desk_body": lambda x, y, r: _noise((50, 52, 56), r, 3),
        "keys": lambda x, y, r: (24, 24, 26) if x % 2 and y % 2 else (44, 46, 48),
        "screen_a": lambda x, y, r: (214, 156, 60) if y in (4, 5) and 2 <= x <= 9 else (176, 186, 186) if y == 9 and x < 12
        else (62, 80, 82) if y > 11 and x % 3 else (26, 32, 34),
        "screen_b": lambda x, y, r: (196, 62, 50) if y == 3 and x < 7 else (62, 80, 82) if 5 <= y <= 12 and (x + y) % 4 == 0
        else (176, 186, 186) if y == 14 and x < 10 else (26, 32, 34),
        "screen_c": lambda x, y, r: (176, 186, 186) if y == 2 and 2 <= x <= 9 else (214, 156, 60) if 5 <= y <= 6 and 9 <= x <= 13
        else (196, 62, 50) if y == 8 and 9 <= x <= 13 else (62, 80, 82) if 4 <= y <= 9 and 2 <= x <= 6 else (26, 32, 34),
        "rack": lambda x, y, r: _noise((28, 28, 30), r, 2), "rack_door": lambda x, y, r: (20, 20, 22) if (x + y) % 3 == 0 else (42, 42, 46),
        "light_panel": lambda x, y, r: (236, 236, 228), "cable_red": lambda x, y, r: (110, 30, 26) if y % 4 else (80, 22, 20),
        "cable_black": lambda x, y, r: (24, 24, 26) if y % 4 else (40, 40, 44), "led": lambda x, y, r: (214, 64, 48),
    }
    for name, fn in fur.items():
        tex[f"furniture_{name}"] = _fill(_img(), fn, 30 + len(tex))
    # Hangar Pad: dark deck plate, a red charging ring with a glowing contact in the middle, corner chevrons.
    def hangar_pad(x, y, r):
        d2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
        if 26 <= d2 <= 40:
            return (200, 52, 38)
        if d2 < 3:
            return (255, 110, 80)
        if (x < 3 and y < 3 and x + y == 2) or (x > 12 and y < 3 and (15 - x) + y == 2) or \
                (x < 3 and y > 12 and x + (15 - y) == 2) or (x > 12 and y > 12 and (15 - x) + (15 - y) == 2):
            return (214, 170, 38)
        return (34, 36, 40) if x in (0, 15) or y in (0, 15) else _noise((56, 60, 66), r, 3)
    tex["hangar_pad"] = _fill(_img(), hangar_pad, 35)
    tex["drone_tower_core_side"] = _fill(_img(), lambda x, y, r: (26, 27, 30) if x in (0, 15) or y in (0, 15) else
                                         (150, 38, 30) if y in (7, 8) and 3 <= x <= 12 else _noise((44, 46, 50), r, 3), 50)
    tex["drone_tower_core_top"] = _fill(_img(), lambda x, y, r: (26, 27, 30) if x in (0, 15) or y in (0, 15) else
                                        (235, 62, 44) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 5 else
                                        (150, 38, 30) if abs((x - 7.5) ** 2 + (y - 7.5) ** 2 - 25) < 6 else _noise((44, 46, 50), r, 3), 51)
    return tex


def draw_item_textures():
    """{item: Image} for the tower's items (none since the modules went)."""
    return {}


# ---------------------------------------------------------------- models, blockstates, loot

def write_assets(write, rid, assets, lang):
    for block, info in BUILDING.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        models = assets / "models" / "block"
        if info["kind"] == "pillar":
            write(models / f"{block}.json", {"parent": "minecraft:block/cube_column", "textures": {
                "side": rid(f"block/{block}"), "end": rid(f"block/{block}_top")}})
            write(models / f"{block}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
                "side": rid(f"block/{block}"), "end": rid(f"block/{block}_top")}})
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                "axis=y": {"model": rid(f"block/{block}")},
                "axis=z": {"model": rid(f"block/{block}_horizontal"), "x": 90},
                "axis=x": {"model": rid(f"block/{block}_horizontal"), "x": 90, "y": 90}}})
        else:
            sprite = rid(f"block/{block}")
            if info["kind"] == "glass":
                sprite = {"force_translucent": True, "sprite": sprite}
            write(models / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": sprite}})
            write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        if block == "hangar_pad":
            # One model per part of the joined-up hangar pads (tower/HangarPadBlock, textures from tools/tower_art.py).
            import tower_art
            variants = {}
            for part in range(tower_art.HANGAR_PAD_PARTS):
                top = rid("block/hangar_pad") if part == 0 else rid(f"block/hangar_pad_{part}")
                name = "hangar_pad" if part == 0 else f"hangar_pad_{part}"
                write(models / f"{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
                    "top": top, "side": rid("block/hangar_pad_side"), "bottom": rid("block/landing_platform"),
                    "particle": rid("block/hangar_pad")}})
                variants[f"part={part}"] = {"model": rid(f"block/{name}")}
            write(assets / "blockstates" / f"{block}.json", {"variants": variants})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        if info.get("variants"):
            tex = rid(f"block/{block}")
            t = {"bottom": tex, "top": tex, "side": tex}
            write(models / f"{block}_slab.json", {"parent": "minecraft:block/slab", "textures": t})
            write(models / f"{block}_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
            write(assets / "blockstates" / f"{block}_slab.json", {"variants": {
                "type=bottom": {"model": rid(f"block/{block}_slab")}, "type=top": {"model": rid(f"block/{block}_slab_top")},
                "type=double": {"model": rid(f"block/{block}")}}})
            write(assets / "items" / f"{block}_slab.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}_slab")}})
            for suffix in ("", "_inner", "_outer"):
                write(models / f"{block}_stairs{suffix}.json", {"parent": f"minecraft:block/{'stairs' if not suffix else suffix[1:] + '_stairs'}",
                                                               "textures": t})
            write(assets / "blockstates" / f"{block}_stairs.json", stairs_blockstate(rid(f"block/{block}_stairs")))
            write(assets / "items" / f"{block}_stairs.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}_stairs")}})
            lang[f"block.{MOD}.{block}_slab"] = f"{info['display']} Slab"
            lang[f"block.{MOD}.{block}_stairs"] = f"{info['display']} Stairs"
    for block, info in FURNITURE.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        write(assets / "models" / "block" / f"{block}.json", furniture_model(block, info, rid))
        if info.get("no_facing"):
            write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        else:
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})}
                for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    from machine_connections import with_model_sockets
    write(assets / "models" / "block" / "drone_depot_terminal.json", with_model_sockets(furniture_model("drone_depot_terminal", TERMINAL_MODEL, rid)))
    write(assets / "blockstates" / "drone_depot_terminal.json", {"variants": {
        f"facing={face}": {"model": rid("block/drone_depot_terminal"), **({"y": y} if y else {})}
        for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(assets / "items" / "drone_depot_terminal.json", {"model": {"type": "minecraft:model", "model": rid("block/drone_depot_terminal")}})
    for block, info in PORTS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        write(assets / "models" / "block" / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{block}")}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    for block, info in CORE.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        write(assets / "models" / "block" / f"{block}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": rid(f"block/{block}_side"), "top": rid(f"block/{block}_top"), "bottom": rid(f"block/{block}_side")}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    lang.update(LANG)
    tower_realism_art.write_assets(write, rid, assets, lang)
    tower_exchanges.write_assets(root=assets.parents[4], lang=lang)
    tower_master_console.write_all(root=assets.parents[4])
    lang.update({f"block.{MOD}.{block}": display for block, display in SURFACE_LIGHTS.items()})
    lang[f"block.{MOD}.{tower_master_console.BLOCK_ID}"] = tower_master_console.DISPLAY
    # The Tri-Pillar hall's live fittings, the glass-door equipment rack (its model replaces the one above) and bay tags.
    tripillar_fixtures.write_all(write, rid, assets, lang)
    # The Console Desk's joined bench models (left / middle / right / on its own), replacing its one-piece blockstate and item.
    import ops_desk
    ops_desk.write_all(write, lang)


def stairs_blockstate(model):
    """The vanilla stairs blockstate (facing x half x shape), pointing at model, model_inner and model_outer."""
    variants = {}
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in base_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = model + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = y0
                if half == "bottom" and shape in ("inner_left", "outer_left"):
                    y = (y0 + 270) % 360
                if half == "top" and shape in ("inner_right", "outer_right"):
                    y = (y0 + 90) % 360
                v = {"model": m}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def furniture_model(block, info, rid):
    elements = []
    for x0, y0, z0, x1, y1, z1, t in info["boxes"]:
        faces = {face: {"texture": f"#{t}"} for face in ("north", "south", "east", "west", "up", "down")}
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
        if t in FURNITURE_GLOW:
            e["light_emission"] = 12
        elements.append(e)
    used = sorted({b[6] for b in info["boxes"]})
    textures = {t: rid(f"block/furniture_{t}") for t in used}
    textures["particle"] = rid(f"block/furniture_{used[0]}")
    return {"textures": textures, "elements": elements}


def write_loot(write, rid, out, self_drop):
    for block in tower_blocks():
        table = self_drop(block)
        if block.endswith("_slab"):
            # 26.x loot keys: one "modifier" list, typed with "type", one "condition".
            table["pools"][0]["entries"][0]["modifier"] = [
                {"type": "minecraft:set_count", "count": 2, "add": False,
                 "condition": {"type": "minecraft:match_block", "blocks": rid(block), "state": {"type": "double"}}},
                {"type": "minecraft:explosion_decay"}]
        write(out / f"{block}.json", table)


# The Tri-Pillar's tiers (tools/tower_tripillar.py TIER_NAMES; the only design since October 2026).
TIER_NAMES = ["Roof Pads", "Pillar Feet", "Small Hangar Rows", "Medium Hangars", "Logistics Levels", "Composite Levels",
              "Large Hangars", "Heavy Levels", "Crown"]

LANG = {
    "message.jugcraft.master_console.unlinked": "Console incomplete or unlinked. Place near a Drone Depot Terminal.",
    "container.jugcraft.drone_tower": "Drone Tower",
    **{f"tower.jugcraft.tier.{i + 1}": name for i, name in enumerate(TIER_NAMES)},
    "tower.jugcraft.tier.0": "Foundation",
    "tower.jugcraft.foundation": "Control Building",
    "message.jugcraft.tower.not_owner": "Only this tower's owner (or their party, in Party mode) can do that",
    # the holograms (tower/Holograms): the tower's own, placed by it (or by a creative player)
    "message.jugcraft.hologram.tower_only": "Holograms are the Drone Tower's own: it puts them in as it builds",
    "message.jugcraft.tower.busy": "The tower is already building",
    "message.jugcraft.tower.foundation_started": "Building the foundation: the control building, its exchanges and supply yard",
    "message.jugcraft.tower.max": "The tower is complete (tier 9)",
    "message.jugcraft.tower.too_close": "Too close to your Drone Tower at %s, %s, %s: a new tower must stand more than %s chunks from your others",
    "message.jugcraft.tower.no_terminal": "The tower's Drone Depot Terminal is missing from the supply yard",
    "message.jugcraft.tower.missing_blocks": "Needs %s more %s (%s kinds of block still short: see the tower screen)",
    "message.jugcraft.tower.started": "Building tier %s: %s",
    "message.jugcraft.tower.deposited_blocks": "%s blocks put in for the tower",
    "message.jugcraft.tower.nothing_needed": "The tower needs none of that",
    # loading the tower (tower/TowerLoadMenu): the first layer at the core, then the tower storage at the Cargo Exchange
    "message.jugcraft.tower.first_layer_started": "Building the first layer: the control building, then tier 1",
    "message.jugcraft.tower.first_layer_queued": "Tier 1 starts as soon as the foundation is done",
    "message.jugcraft.tower.use_cargo_exchange": "The tower's blocks go in through its Cargo Exchange now (the tower storage)",
    "message.jugcraft.tower.storage_full": "The tower storage is full (10 double chests)",
    "screen.jugcraft.tower_load.title": "Load the First Layer",
    "screen.jugcraft.tower_load.storage_title": "Tower Storage",
    "screen.jugcraft.tower_load.heading": "LOAD THE FIRST LAYER",
    "screen.jugcraft.tower_load.storage_heading": "TOWER STORAGE",
    "screen.jugcraft.tower_storage.load_blocks": "Load Blocks",
    "screen.jugcraft.tower_storage.hint": "Click to take, shift-click to move",
    "message.jugcraft.tower.storage_plain": "Only plain items go in the tower storage (no names, enchantments or damage)",
    "screen.jugcraft.tower_load.foundation_and_tier1": "FOUNDATION + TIER 1",
    "screen.jugcraft.tower_load.tier1": "TIER 1",
    "screen.jugcraft.tower_load.next_tier": "NEXT: TIER %s %s",
    "screen.jugcraft.tower_load.put_in": "PUT BLOCKS IN",
    "screen.jugcraft.tower_load.stacks": "%s / %s stacks",
    "screen.jugcraft.tower_load.upgrade_at": "UPGRADE at the Tower",
    "screen.jugcraft.tower_load.upgrade_at2": "Control console",
    "screen.jugcraft.tower_load.inventory": "Inventory",
    "screen.jugcraft.tower_load.load_all": "Load All",
    "screen.jugcraft.tower_load.build": "Build First Layer",
    # the core's placement warning (tower/TowerPlacement, client/TowerPlaceScreen)
    "message.jugcraft.tower.place_again": "WARNING: be sure this is where you want your Drone Tower. It is an extremely large structure: finished, %s blocks wide, %s long and %s tall. Use the core on the same spot again within %s seconds to place it.",
    "screen.jugcraft.tower_place.title": "DRONE TOWER CORE",
    "screen.jugcraft.tower_place.sub": "CONFIRM THE SITE",
    "screen.jugcraft.tower_place.warning": "WARNING",
    "screen.jugcraft.tower_place.message": "Be sure this is the location you want to select for your Drone Tower. It is an extremely large structure.",
    "screen.jugcraft.tower_place.finished": "FINISHED TOWER, TIER 9",
    "screen.jugcraft.tower_place.width": "WIDTH",
    "screen.jugcraft.tower_place.length": "LENGTH",
    "screen.jugcraft.tower_place.height": "HEIGHT",
    "screen.jugcraft.tower_place.blocks": "%s blocks",
    "screen.jugcraft.tower_place.east_west": "east-west",
    "screen.jugcraft.tower_place.north_south": "north-south",
    "screen.jugcraft.tower_place.up_down": "%s up, %s down from the core",
    "screen.jugcraft.tower_place.reaches": "Reaches %s E, %s W, %s N, %s S",
    "screen.jugcraft.tower_place.core_at": "Core at %s, %s, %s",
    "screen.jugcraft.tower_place.tip": "Hold the core and shift+scroll to see the space it takes.",
    "screen.jugcraft.tower_place.cancel": "Cancel",
    "screen.jugcraft.tower_place.place": "Place Tower Here",
    "screen.jugcraft.tower.title": "DRONE TOWER",
    "screen.jugcraft.tower.tier": "TIER %s · %s",
    "screen.jugcraft.tower.unlocked": "Drones up to T%s · %s drones",
    "screen.jugcraft.tower.none_unlocked": "Build the foundation and tier 1 to start a depot",
    "screen.jugcraft.tower.next_foundation": "NEXT: FOUNDATION · %s",
    "screen.jugcraft.tower.building_foundation": "BUILDING THE FOUNDATION · %s%%",
    "screen.jugcraft.tower.converting": "REBUILDING AS THE TRI-PILLAR · %s%%",
    "screen.jugcraft.tower.converting_note": "Drones and stores carry over",
    "screen.jugcraft.tower.next": "NEXT: TIER %s %s",
    "screen.jugcraft.tower.complete": "TOWER COMPLETE",
    "screen.jugcraft.tower.building": "BUILDING TIER %s · %s%%",
    "screen.jugcraft.tower.building_core": "The core builds this itself",
    "screen.jugcraft.tower.building_drones": "Drones fly the new floors in, bottom up",
    "screen.jugcraft.tower.upgrade": "UPGRADE DRONE TOWER",
    "message.jugcraft.tower.preview": "Tower preview, step %s of 9: %s (shift+scroll)",
    "screen.jugcraft.tower.load": "LOAD FROM INVENTORY",
    "screen.jugcraft.tower.load_hint": "Puts in every block the first layer (the foundation and tier 1) still needs from your inventory. After that the tower's blocks go in through its Cargo Exchange, into the tower storage.",
    "screen.jugcraft.tower.storage": "TOWER STORAGE %s / %s STACKS · FILL IT AT THE CARGO EXCHANGE",
    "screen.jugcraft.tower.materials": "MATERIALS %s/%s READY · %s / %s",
    "screen.jugcraft.tower.row_ready": "%s: all in",
    "screen.jugcraft.tower.row_missing": "%s: %s more needed",
    "screen.jugcraft.tower.scroll": "%s-%s OF %s",
    "tooltip.jugcraft.drone_tower_core": "Set it on the ground where the middle of the tower's hall should be (it warns you first, with the finished tower's size), load it with the first layer's blocks and press BUILD FIRST LAYER: it builds the control building and tier 1 itself",
    "tooltip.jugcraft.drone_tower_core.store": "Holds %s blocks (%s kinds) for its tower",
    "screen.jugcraft.tower.legend": "Solid: built · outline: next",
    "screen.jugcraft.terminal.tower": "TOWER CONTROL",
    "message.jugcraft.drone.link.needs_tower": "Drones need a Drone Tower: craft a Drone Tower Core, set it on the ground, load it with the blocks its foundation needs and build up to tier 1 (tier %s asked)",
    "message.jugcraft.drone.link.tower_tier": "Tier %s drones need the Drone Tower at that tier",
    "message.jugcraft.drone.status.no_tower": "No Drone Tower · build a Tower Core to fly drones",
    "message.jugcraft.drone.status.tower": "Drone Tower tier %s · drones up to T%s",
}
