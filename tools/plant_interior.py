"""The pre-made plants' building kit (nuclear/PlantInterior.java): models, textures, blockstates, recipes and names.

Inside the plants things look realistic; outside they stay plain Minecraft. Furniture models are boxes in 1/16 block
for a block facing north: whoever uses it looks north (-z), its front is the south face. Pieces that join up when
stacked (server racks, relay cabinets, lockers, pallet racking) have a body model, a top cap shown on the highest
block of a stack and a plinth shown on the lowest (multipart on up/down).

Textures are drawn at 128 px and brought down to 64 px with nuclear_materials.stylize, like the equipment sheet.
"""
import random

import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_office
import plant_office2

MOD = "jugcraft"

# --------------------------------------------------------------------------------------------------- the pieces
# A box's texture is a name, or {face: name}; "name!" maps the whole texture onto that face (a front or a screen).
FURNITURE = {
    "server_rack": {"display": "Server Rack", "stacks": True, "boxes": [
        (1, 0, 1, 15, 16, 1.6, "pi_rack_body"),
        (1, 0, 1.6, 2, 16, 14.6, "pi_rack_body"), (14, 0, 1.6, 15, 16, 14.6, "pi_rack_body"),
        (2, 0, 1.6, 14, 16, 14.4, {"*": "pi_rack_body", "south": "pi_server_face!"}),
        (1, 0, 14.6, 15, 16, 15.4, {"*": "pi_rack_body", "south": "pi_server_mesh!", "north": "pi_server_mesh!"})],
        # The door handle goes with the top block of a stack (about chest height on a two-high rack).
        "cap": [(0.8, 15.3, 0.8, 15.2, 16, 15.6, {"*": "pi_rack_body", "up": "pi_rack_top!"}),
                (13.2, 0.5, 15.4, 13.8, 5.5, 15.9, "pi_chrome")],
        "plinth": [(0.8, 0, 0.8, 15.2, 0.8, 15.6, "pi_rack_body")]},
    "relay_cabinet": {"display": "Relay Cabinet", "stacks": True, "boxes": [
        (1, 0, 2, 15, 16, 15, {"*": "pi_cabinet", "south": "pi_relay_front!"}),
        (13.2, 6.5, 15, 13.8, 9.5, 15.5, "pi_chrome")],
        "cap": [(0.8, 15.4, 1.8, 15.2, 16, 15.2, "pi_cabinet_dark")],
        "plinth": [(0.8, 0, 1.8, 15.2, 1, 15.2, "pi_cabinet_dark")]},
    "locker": {"display": "Locker", "stacks": True, "boxes": [
        (1, 0, 2, 15, 16, 15, {"*": "pi_locker_side", "south": "pi_locker_front!"})],
        "cap": [(0.8, 15.4, 1.8, 15.2, 16, 15.2, "pi_cabinet_dark")],
        "plinth": [(0.8, 0, 1.8, 15.2, 1, 15.2, "pi_cabinet_dark")]},
    "pallet_rack": {"display": "Pallet Racking", "stacks": True, "boxes": [
        (0, 0, 0, 1.5, 16, 1.5, "pi_upright"), (14.5, 0, 0, 16, 16, 1.5, "pi_upright"),
        (0, 0, 14.5, 1.5, 16, 16, "pi_upright"), (14.5, 0, 14.5, 16, 16, 16, "pi_upright"),
        (1.5, 0, 0.1, 14.5, 1.8, 1.3, "pi_beam"), (1.5, 0, 14.7, 14.5, 1.8, 15.9, "pi_beam"),
        (1.5, 1.4, 1.3, 14.5, 1.6, 14.7, "pi_wire_deck"),
        (1, 1.8, 1.5, 15, 3, 14.5, {"*": "pi_pallet", "up": "pi_pallet_top"}),
        (1.8, 3, 2.2, 14.2, 12.5, 13.8, {"*": "pi_carton", "south": "pi_carton!", "north": "pi_carton!"}),
        (3, 12.5, 4, 9.5, 14.5, 10.5, "pi_carton")],
        "cap": [(1.5, 14.6, 0.1, 14.5, 16, 1.3, "pi_beam"), (1.5, 14.6, 14.7, 14.5, 16, 15.9, "pi_beam")],
        "plinth": [(-0.3, 0, -0.3, 1.8, 0.4, 1.8, "pi_upright"), (14.2, 0, -0.3, 16.3, 0.4, 1.8, "pi_upright"),
                   (-0.3, 0, 14.2, 1.8, 0.4, 16.3, "pi_upright"), (14.2, 0, 14.2, 16.3, 0.4, 16.3, "pi_upright")]},
    "office_desk": {"display": "Office Desk", "boxes": [
        (0, 11.5, 1, 16, 12.5, 15, "pi_laminate"),
        (0.5, 0, 1, 1.5, 11.5, 15, "pi_desk_panel"), (14.5, 0, 1, 15.5, 11.5, 15, "pi_desk_panel"),
        (1.5, 3, 1.2, 14.5, 11.5, 2, "pi_desk_panel"),
        (6.5, 12.5, 3, 9.5, 12.8, 5.5, "pi_monitor"), (7.6, 12.8, 3.6, 8.4, 15.5, 4.2, "pi_monitor"),
        (2.5, 15, 3.2, 13.5, 22, 4, {"*": "pi_monitor"}),
        (2.8, 15.3, 4, 13.2, 21.7, 4.1, {"*": "pi_monitor", "south": "pi_office_screen!"}),
        (4, 12.5, 8, 12, 12.9, 10.5, {"*": "pi_monitor", "up": "pi_keyboard!"}),
        (12.8, 12.5, 8.5, 13.8, 12.9, 10, "pi_monitor"),
        (13, 12.5, 4, 15, 13.3, 6.8, "pi_monitor"),
        (1.2, 12.5, 9, 3.8, 12.6, 12.5, {"*": "pi_paper", "up": "pi_paper!"})],
        "glow": ["pi_office_screen"]},
    "filing_cabinet": {"display": "Filing Cabinet", "boxes": [
        (2, 0, 2, 14, 15, 15, {"*": "pi_filing_side", "south": "pi_filing_front!"}),
        (1.8, 15, 1.8, 14.2, 15.4, 15.2, "pi_filing_side")]},
    "whiteboard": {"display": "Whiteboard", "boxes": [
        (0.5, 5, 0, 15.5, 14, 0.6, {"*": "pi_chrome", "south": "pi_whiteboard!"}),
        (1, 5, 0.6, 15, 5.4, 1.6, "pi_chrome")]},
    "water_cooler": {"display": "Water Cooler", "boxes": [
        # The cabinet, with a real recess at the front where the cup goes: a drip tray at the bottom of it, the two
        # taps (cold, hot) with push paddles hanging from its top. The bottle stands upside down in the collar on
        # top: see-through blue plastic, the water inside, ribs round it. A cup dispenser on the side.
        (3, 0, 3.5, 13, 0.6, 13, "pi_toe_kick"),
        (3, 0.6, 3.5, 13, 7, 13, {"*": "pi_cooler", "south": "pi_cooler_lower!"}),
        (3, 7, 3.5, 13, 13, 9.5, "pi_cooler"),
        (3, 7, 9.5, 4.5, 13, 13, "pi_cooler"), (11.5, 7, 9.5, 13, 13, 13, "pi_cooler"),
        (3, 13, 3.5, 13, 18, 13, {"*": "pi_cooler", "south": "pi_cooler_badge!"}),
        (4.5, 7, 9.5, 11.5, 7.1, 13, "pi_steel_dark"),
        (5, 7.1, 10, 11, 7.6, 12.6, {"*": "pi_chrome", "up": "pi_drip_grate!"}),
        (5.6, 11.4, 10.2, 7, 13, 11.6, "pi_chrome"), (6, 10.6, 10.6, 6.6, 11.4, 11.2, "pi_chrome"),
        (9, 11.4, 10.2, 10.4, 13, 11.6, "pi_chrome"), (9.4, 10.6, 10.6, 10, 11.4, 11.2, "pi_chrome"),
        (5.4, 11.2, 11.6, 7.2, 12.8, 12.2, "pi_marker_blue"), (8.8, 11.2, 11.6, 10.6, 12.8, 12.2, "pi_marker_red"),
        (4, 18, 4.5, 12, 18.6, 12, "pi_cooler"),
        (7, 18.6, 7.5, 9, 20, 9.5, "pi_bottle_plastic"),
        (5.5, 20, 6, 10.5, 21.4, 11, "pi_bottle_plastic"),
        (4.5, 21.4, 5, 11.5, 30.5, 12, "pi_bottle_plastic"),
        (4.3, 24.5, 4.8, 11.7, 25.1, 12.2, "pi_bottle_plastic"), (4.3, 28, 4.8, 11.7, 28.6, 12.2, "pi_bottle_plastic"),
        (4.8, 20.2, 5.3, 11.2, 29, 11.7, "pi_bottle_water"),
        (13, 6, 6.5, 14.6, 15, 8.5, {"*": "pi_cab_white", "east": "pi_cup_stack!"})]},
    "guard_rail": {"display": "Safety Railing", "boxes": [
        (7.2, 0, 0.4, 8.8, 16, 2, "pi_rail_yellow"), (0, 15, 0.6, 16, 16, 1.8, "pi_rail_yellow"),
        (0, 8, 0.8, 16, 9, 1.6, "pi_rail_yellow"), (0, 0, 0.6, 16, 2, 1.0, "pi_rail_yellow")]},
    "turnstile": {"display": "Turnstile", "boxes": [
        (9, 0, 1, 15, 14.5, 15, "pi_steel"), (8.5, 14.5, 0.5, 15.5, 16, 15.5, {"*": "pi_steel", "up": "pi_reader!"}),
        (8, 10, 6.5, 9, 13, 9.5, "pi_chrome"), (1, 11, 7.4, 8, 12, 8.6, "pi_chrome")],
        "glow": ["pi_reader"]},
}
FURNITURE["server_rack"]["glow"] = ["pi_server_face"]
FURNITURE["relay_cabinet"]["glow"] = ["pi_relay_front"]
# Pallet racking with drums on it (the turbine building's oil store): the racking's frame, drums on the pallet.
FURNITURE["pallet_rack_drums"] = dict(FURNITURE["pallet_rack"], display="Pallet Racking (Drums)",
                                      boxes=FURNITURE["pallet_rack"]["boxes"][:8] + plant_office.O4.O7.DRUM_LOAD[1:])
FURNITURE.update(plant_office.OFFICE)
FURNITURE.update(plant_office2.PIECES)
# Caps sit a hair above the body's top so the two tops don't fight for the same plane (the flicker on rack tops).
for _info in FURNITURE.values():
    if "cap" in _info:
        _info["cap"] = [b if b[4] != 16 else b[:4] + (16.1,) + b[5:] for b in _info["cap"]]

CUBES = {"ceiling_tile": "Suspended Ceiling Tile", "vinyl_floor": "Vinyl Floor Tile", "epoxy_floor": "Epoxy Floor",
         "painted_wall": "Painted Block Wall", "tank_plate": "Painted Tank Plate", "intake_louvre": "Intake Louvre"}
DOORS = {"steel_door": "Steel Door", "glass_door": "Glass Door"}
OTHER = {"hvac_duct": "HVAC Duct", "roller_door": "Roller Door", "building_sign": "Building Sign", "room_sign": "Room Sign"}
# Same order as PlantSignBlock.Label.
SIGNS = {
    "unit_1": "UNIT 1", "reactor_building": "REACTOR BUILDING", "turbine_building": "TURBINE BUILDING",
    "control_building": "CONTROL BUILDING", "auxiliary_building": "AUXILIARY BUILDING", "fuel_building": "FUEL BUILDING",
    "diesel_building": "EMERGENCY DIESEL GENERATORS", "pump_house": "INTAKE PUMP HOUSE", "administration": "ADMINISTRATION",
    "security": "SECURITY", "warehouse": "WAREHOUSE", "transformer_yard": "MAIN TRANSFORMERS", "switchyard": "SWITCHYARD",
    "main_control_room": "Main Control Room", "shift_supervisor": "Shift Supervisor", "relay_room": "Relay Room",
    "cable_spreading_room": "Cable Spreading Room", "server_room": "Plant Computer Room", "battery_room": "Battery Room",
    "hvac_room": "HVAC Equipment", "switchgear_room": "Switchgear", "lube_oil_room": "Lube Oil", "workshop": "Workshop",
    "turbine_control_room": "Turbine Control", "diesel_a": "Diesel Generator A", "diesel_b": "Diesel Generator B",
    "office": "Office", "meeting_room": "Meeting Room", "cafeteria": "Cafeteria", "lobby": "Reception", "stairs": "Stairs",
    "guard_room": "Guard Room", "stores": "Stores", "pump_room": "Pump Room", "electrical_room": "Electrical Room",
    "health_physics": "Health Physics", "charging_pumps": "Charging Pumps", "rhr_pumps": "RHR Pumps",
    "heat_exchangers": "Heat Exchangers", "tank_room": "Tank Room", "ventilation": "Ventilation", "fuel_pool": "Spent Fuel Pool",
    "truck_bay": "Truck Bay", "locker_room": "Locker Room",
    "break_room": "Break Room", "chemistry_lab": "Chemistry Lab", "document_control": "Documentation Storage",
    "training_room": "Training Room", "engineering": "Engineering", "plant_manager": "Plant Manager", "it_support": "IT Support",
    "first_aid": "First Aid", "open_office": "Open Office", "board_room": "Board Room", "screening": "Security Screening",
    "staff_only": "Staff Only", "operations": "Operations", "maintenance_planning": "Maintenance Planning",
    "human_resources": "Human Resources", "visitors": "Visitors", "washroom": "Washroom", "intern_area": "Intern Area",
    "radwaste_building": "RADWASTE BUILDING", "airlock": "Airlock", "waste_vault": "Waste Vault",
    "inspection_corridor": "Inspection Corridor",
}

OTHER["conference_table"] = "Round Meeting Table"
MULTI = [k for k, v in FURNITURE.items() if "multi" in v]
BLOCKS = list(FURNITURE) + list(CUBES) + ["hvac_duct"] + list(DOORS) + ["roller_door", "building_sign", "room_sign", "conference_table"]
NAMES = {**{k: v["display"] for k, v in FURNITURE.items()}, **CUBES, **DOORS, **OTHER}
UNIFORMS = {"lab_coat": "Lab Coat", "security_jacket": "Security Jacket"}   # wearable (client/UniformLayer.java)
ITEM_TEXTURES = list(DOORS) + ["building_sign", "room_sign", "conference_table"] + MULTI + list(UNIFORMS)

STEEL_PLATE = "#c:plates/steel"
CRAFTING = {
    "server_rack": (["PCP", "PXP", "PCP"], {"P": STEEL_PLATE, "C": "jugcraft:basic_circuit", "X": "jugcraft:processor"}, 2),
    "relay_cabinet": (["PGP", "PCP", "PRP"], {"P": STEEL_PLATE, "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 2),
    "locker": (["PP", "PP", "PP"], {"P": "#c:plates/iron"}, 2),
    "pallet_rack": (["S S", "BBB", "S S"], {"S": STEEL_PLATE, "B": "minecraft:barrel"}, 4),
    "pallet_rack_drums": (["S S", "BDB", "S S"], {"S": STEEL_PLATE, "B": "minecraft:barrel", "D": "minecraft:cauldron"}, 4),
    "office_desk": (["GGG", "PPP", "P P"], {"G": "minecraft:black_stained_glass_pane", "P": "#minecraft:planks"}, 1),
    "filing_cabinet": (["PP", "PP"], {"P": "#c:plates/iron"}, 1),
    "whiteboard": (["QQQ", "P P"], {"Q": "minecraft:quartz", "P": "#c:plates/iron"}, 2),
    "water_cooler": (["G", "B", "P"], {"G": "minecraft:glass_bottle", "B": "minecraft:water_bucket", "P": "#c:plates/iron"}, 1),
    "guard_rail": (["YYY", "S S"], {"Y": "minecraft:yellow_dye", "S": "minecraft:iron_nugget"}, 8),
    "turnstile": (["SRS", "S S"], {"S": STEEL_PLATE, "R": "minecraft:redstone"}, 1),
    "ceiling_tile": (["WWW", "WNW", "WWW"], {"W": "minecraft:white_wool", "N": "minecraft:iron_nugget"}, 8),
    "vinyl_floor": (["SS", "SS"], {"S": "jugcraft:plastic_sheet"}, 8),
    "epoxy_floor": (["CCC", "CSC", "CCC"], {"C": "minecraft:light_gray_concrete", "S": "jugcraft:plastic_sheet"}, 8),
    "painted_wall": (["CCC", "CWC", "CCC"], {"C": "minecraft:stone_bricks", "W": "minecraft:white_dye"}, 8),
    "tank_plate": (["CCC", "CWC", "CCC"], {"C": "minecraft:smooth_stone", "W": "minecraft:white_dye"}, 8),
    "intake_louvre": (["NNN", "N N", "NNN"], {"N": "minecraft:iron_nugget"}, 4),
    "hvac_duct": (["NNN", "   ", "NNN"], {"N": "minecraft:iron_nugget"}, 6),
    "steel_door": (["PP", "PG", "PP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane"}, 3),
    "glass_door": (["NG", "NG", "NG"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass_pane"}, 3),
    "roller_door": (["NNN", "NNN"], {"N": "minecraft:iron_nugget"}, 6),
    "building_sign": (["PPP", "PDP"], {"P": "#c:plates/iron", "D": "minecraft:black_dye"}, 1),
    "room_sign": (["PD"], {"P": "#c:plates/iron", "D": "minecraft:black_dye"}, 4),
}
CRAFTING.update(plant_office.CRAFTING)
CRAFTING.update(plant_office2.CRAFTING)
CRAFTING.update(plant_office2.PN.CRAFTING)
CRAFTING["lab_coat"] = (["W W", "WWW", "WWW"], {"W": "minecraft:white_wool"}, 1)
CRAFTING["security_jacket"] = (["W W", "WDW", "WWW"], {"W": "minecraft:blue_wool", "D": "minecraft:black_dye"}, 1)


def _box_textures(boxes):
    names = set()
    for box in boxes:
        t = box[6]
        for v in (t.values() if isinstance(t, dict) else [t]):
            if v is None:
                continue
            names.add(v[0] if isinstance(v, tuple) else v.split("@")[0].rstrip("!"))
    return names


def textures_used():
    names = set()
    for info in FURNITURE.values():
        for part in ("boxes", "cap", "plinth", "left", "right"):
            names |= _box_textures(info.get(part, []))
        for top in info.get("tops", []):
            names |= _box_textures(top)
        if "prints" in info:
            for v in range(info["prints"]):
                for part in range(2):
                    names |= _box_textures(plant_office2.lprint_boxes(part, v))
        elif "multi" in info:
            for v in range(info.get("versions", 1)):
                for _, boxes in plant_office2.multi_parts(info, v):
                    names |= _box_textures(boxes)
        for extra in info.get("vboxes", []):
            names |= _box_textures(extra)
        if "vtex" in info:
            names.discard(info["vtex"])
            names |= {f"{info['vtex']}_{i}" for i in range(info["variants"])}
        if info.get("plant"):
            names |= plant_office.O4.O6.plant_textures() | set(plant_office.POTS) | {"pi_soil"}
    for part in range(9):
        names |= _box_textures(plant_office.table_boxes(part))
    return names


TEXTURES = {t for t in textures_used() if not t.startswith("mc_")} | set(CUBES) | {"hvac_duct", "hvac_duct_top", "pi_roller_slats", "pi_sign"} | \
    {f"{d}_{h}" for d in DOORS for h in ("top", "bottom")}
HI_RES = TEXTURES | set(ITEM_TEXTURES)


# --------------------------------------------------------------------------------------------------- models

def _faces(t, glow):
    out = {}
    spec = t if isinstance(t, dict) else {"*": t}
    for face in ("north", "south", "east", "west", "up", "down"):
        name = spec.get(face, spec.get("*"))
        if name is None:                     # no face there (the inside cuts of a glass top in strips)
            continue
        if isinstance(name, tuple):          # (texture, uv): a region of a sheet (the book spines)
            out[face] = {"texture": f"#{name[0]}", "uv": [round(v, 4) for v in name[1]]}
            continue
        name, _, turn = name.partition("@")       # "name!@180": the picture turned (paper read from the far side)
        full = name.endswith("!")
        name = name.rstrip("!")
        f = {"texture": f"#{name}"}
        if full:
            f["uv"] = [0, 0, 16, 16]
        if turn:
            f["rotation"] = int(turn)
        out[face] = f
    return out


def _auto_uv(face, x0, y0, z0, x1, y1, z1):
    """The texture area a face would take from its position, kept inside the texture (parts of a model that reach
    outside their block, like a coat hanging into the block below, would otherwise read outside it)."""
    if face in ("north", "south"):
        u = (16 - x1, 16 - x0) if face == "north" else (x0, x1)
        v = (16 - y1, 16 - y0)
    elif face in ("east", "west"):
        u = (16 - z1, 16 - z0) if face == "east" else (z0, z1)
        v = (16 - y1, 16 - y0)
    else:
        u, v = (x0, x1), ((z0, z1) if face == "up" else (16 - z1, 16 - z0))

    def fit(a, b):
        if 0 <= a <= 16 and 0 <= b <= 16:
            return a, b
        span = min(16, b - a)
        a = min(max(a, 0), 16 - span)
        return a, a + span
    u, v = fit(*u), fit(*v)
    return [round(u[0], 3), round(v[0], 3), round(u[1], 3), round(v[1], 3)]


def _elements(boxes, glow):
    elements, used = [], set()
    for box in boxes:
        x0, y0, z0, x1, y1, z1, t = box[:7]
        faces = _faces(t, glow)
        if min(x0, y0, z0) < 0 or max(x1, y1, z1) > 16:
            for fname, f in faces.items():
                f.setdefault("uv", _auto_uv(fname, x0, y0, z0, x1, y1, z1))
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
        if len(box) > 7:
            e.update(box[7])
        names = {f["texture"][1:] for f in faces.values()}
        if names & set(glow):
            e["light_emission"] = 12
        used |= names
        elements.append(e)
    return elements, used


NUDGE = 0.03


def separate(boxes, under=()):
    """No two faces in the same plane: where a box's face lies in the same plane as a face of a box before it (or of
    a box in `under`, the model it is drawn over) pointing the same way and overlapping it, the later box's face is
    moved out a hair, so it covers the other instead of the two flickering for which shows (z-fighting)."""
    done = [list(b[:6]) for b in under if len(b) <= 7]
    out = []
    for box in boxes:
        b = list(box[:6])
        if len(box) <= 7:
            for axis in range(3):
                o1, o2 = [a for a in range(3) if a != axis]
                for side in (0, 1):
                    k = axis + 3 * side
                    for other in done:
                        if abs(other[k] - b[k]) < 1e-6 and min(b[o1 + 3], other[o1 + 3]) - max(b[o1], other[o1]) > 1e-6 \
                                and min(b[o2 + 3], other[o2 + 3]) - max(b[o2], other[o2]) > 1e-6:
                            # Out a hair; but a face on the block's edge goes in instead (out, it would lie on the
                            # next block's faces: a row of counters).
                            moved = b[k] + (NUDGE if side else -NUDGE)
                            inside = 0 <= b[axis] and b[axis + 3] <= 16
                            if inside and not 0 <= moved <= 16 and b[axis + 3] - b[axis] > 2 * NUDGE:
                                moved = b[k] - (NUDGE if side else -NUDGE)
                            b[k] = moved
                            break
            done.append(b)
        out.append(tuple(b) + tuple(box[6:]))
    return out


def model(boxes, glow, rid, under=()):
    elements, used = _elements(separate(boxes, under), glow)
    # "mc_x": the game's own texture block/x (the chain); everything else is ours.
    textures = {t: ("minecraft:block/" + t[3:] if t.startswith("mc_") else rid(f"block/{t}")) for t in sorted(used)}
    textures["particle"] = textures[sorted(used)[0]]
    # Smooth lighting on (the default): the light blends across a row of joined pieces instead of stepping at each
    # block. (Flat lighting takes each block's own light level, which differs from block to block.)
    return {"textures": textures, "elements": elements}


TURN = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def write_assets(write, rid, assets, lang):
    import zfight
    write = zfight.unfighting(write)  # no two faces of a model in the same place (Justin: never any z-fighting)
    import plant_panels
    write(assets / "live_panels.json", plant_panels.layout())
    models = assets / "models" / "block"
    for block, info in FURNITURE.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        glow = info.get("glow", [])
        item_model = rid(f"block/{block}")
        if info.get("plant"):
            for i in range(info["variants"]):
                write(models / f"{block}_{i}.json", plant_office.plant_model(i, rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},variant={i}": {"model": rid(f"block/{block}_{i}"), **({"y": y} if y else {})}
                for face, y in TURN for i in range(info["variants"])}})
            item_model = rid(f"block/{block}_0")
        elif "vboxes" in info:
            for i, extra in enumerate(info["vboxes"]):
                write(models / f"{block}_{i}.json", model(info["boxes"] + extra, glow, rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},variant={i}": {"model": rid(f"block/{block}_{i}"), **({"y": y} if y else {})}
                for face, y in TURN for i in range(info["variants"])}})
            item_model = rid(f"block/{block}_{min(3, info['variants'] - 1)}")
        elif "variants" in info:
            vtex = info["vtex"]
            for i in range(info["variants"]):
                boxes = [b[:6] + (({k: v.replace(vtex, f"{vtex}_{i}") for k, v in b[6].items()} if isinstance(b[6], dict)
                                   else b[6].replace(vtex, f"{vtex}_{i}")),) for b in info["boxes"]]
                write(models / f"{block}_{i}.json", model(boxes, glow, rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},variant={i}": {"model": rid(f"block/{block}_{i}"), **({"y": y} if y else {})}
                for face, y in TURN for i in range(info["variants"])}})
            item_model = rid(f"block/{block}_0")
        elif "prints" in info:
            for v in range(info["prints"]):
                for part in range(2):
                    write(models / f"{block}_{v}_{part}.json", model(plant_office2.lprint_boxes(part, v), [], rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},part={part},variant={v}": {"model": rid(f"block/{block}_{v}_{part}"), **({"y": y} if y else {})}
                for face, y in TURN for part in range(2) for v in range(info["prints"])}})
            write(assets / "models" / "item" / f"{block}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{block}")}})
            item_model = rid(f"item/{block}")
        elif "multi" in info and info.get("versions"):
            # A piece over several blocks in a few versions (the meeting table laid out differently): part x version.
            w, rows, vertical = info["multi"]
            for v in range(info["versions"]):
                for part, boxes in plant_office2.multi_parts(info, v):
                    write(models / f"{block}_{v}_{part}.json", model(boxes, [], rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},part={part},variant={v}": {"model": rid(f"block/{block}_{v}_{part}"), **({"y": y} if y else {})}
                for face, y in TURN for part in range(w * rows) for v in range(info["versions"])}})
            write(assets / "models" / "item" / f"{block}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{block}")}})
            item_model = rid(f"item/{block}")
        elif "multi" in info:
            w, rows, vertical = info["multi"]
            for part, boxes in plant_office2.multi_parts(info):
                g = [b[6]["south"].rstrip("!") for b in boxes if isinstance(b[6], dict) and "south" in b[6]] if vertical is True else []
                write(models / f"{block}_{part}.json", model(boxes, g, rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face},part={part}": {"model": rid(f"block/{block}_{part}"), **({"y": y} if y else {})}
                for face, y in TURN for part in range(w * rows)}})
            write(assets / "models" / "item" / f"{block}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{block}")}})
            item_model = rid(f"item/{block}")
        elif "row" in info:
            write(models / f"{block}.json", model(info["boxes"], glow, rid))
            parts = []
            for i, top in enumerate(info.get("tops", [])):
                write(models / f"{block}_top_{i}.json", model(top, glow, rid, under=info["boxes"]))
            for side in ("left", "right"):
                if info[side]:
                    write(models / f"{block}_{side}.json", model(info[side], glow, rid, under=info["boxes"]))
            for face, y in TURN:
                rot = {"y": y} if y else {}
                parts.append({"when": {"facing": face}, "apply": {"model": rid(f"block/{block}"), **rot}})
                for side in ("left", "right"):
                    if info[side]:
                        parts.append({"when": {"facing": face, side: "false"}, "apply": {"model": rid(f"block/{block}_{side}"), **rot}})
                for i in range(len(info.get("tops", []))):
                    parts.append({"when": {"facing": face, "variant": str(i)}, "apply": {"model": rid(f"block/{block}_top_{i}"), **rot}})
            write(assets / "blockstates" / f"{block}.json", {"multipart": parts})
            write(models / f"{block}_inventory.json", model(info["boxes"] + info["left"] + info["right"] + (info["tops"][0] if info.get("tops") else []), glow, rid))
            item_model = rid(f"block/{block}_inventory")
        elif info.get("stacks"):
            write(models / f"{block}.json", model(info["boxes"], glow, rid))
            write(models / f"{block}_cap.json", model(info["cap"], glow, rid, under=info["boxes"]))
            write(models / f"{block}_plinth.json", model(info["plinth"], glow, rid, under=info["boxes"]))
            # The item shows the whole unit: body, cap and plinth.
            write(models / f"{block}_inventory.json", model(info["boxes"] + info["cap"] + info["plinth"], glow, rid))
            parts = []
            for face, y in TURN:
                rot = {"y": y} if y else {}
                parts.append({"when": {"facing": face}, "apply": {"model": rid(f"block/{block}"), **rot}})
                parts.append({"when": {"facing": face, "up": "false"}, "apply": {"model": rid(f"block/{block}_cap"), **rot}})
                parts.append({"when": {"facing": face, "down": "false"}, "apply": {"model": rid(f"block/{block}_plinth"), **rot}})
            write(assets / "blockstates" / f"{block}.json", {"multipart": parts})
            item_model = rid(f"block/{block}_inventory")
        else:
            write(models / f"{block}.json", model(info["boxes"], glow, rid))
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={face}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})} for face, y in TURN}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": item_model}})
    # The wearable uniforms: items with flat icons; worn and in lockers they are drawn in 3D, so their equipment
    # asset has no flat layers.
    for name, display in UNIFORMS.items():
        lang[f"item.{MOD}.{name}"] = display
        write(assets / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
    # The equipment asset needs a layer: a blank one (the coat itself is drawn in 3D by client/UniformLayer.java).
    lang[f"container.{MOD}.storage_rack"] = "Storage Rack"
    write(assets / "equipment" / "uniform.json", {"layers": {"humanoid": [{"texture": "jugcraft:blank"}]}})
    # The round meeting table: nine blocks, each its slice of the top.
    lang[f"block.{MOD}.conference_table"] = OTHER["conference_table"]
    for part in range(9):
        write(models / f"conference_table_{part}.json", model(plant_office.table_boxes(part), [], rid))
    write(assets / "blockstates" / "conference_table.json", {"variants": {
        f"part={part}": {"model": rid(f"block/conference_table_{part}")} for part in range(9)}})
    write(assets / "models" / "item" / "conference_table.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/conference_table")}})
    write(assets / "items" / "conference_table.json", {"model": {"type": "minecraft:model", "model": rid("item/conference_table")}})
    for block, name in CUBES.items():
        lang[f"block.{MOD}.{block}"] = name
        write(models / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{block}")}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    lang[f"block.{MOD}.hvac_duct"] = OTHER["hvac_duct"]
    write(models / "hvac_duct.json", {"parent": "minecraft:block/cube_column", "textures": {
        "side": rid("block/hvac_duct"), "end": rid("block/hvac_duct_top")}})
    write(models / "hvac_duct_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "side": rid("block/hvac_duct"), "end": rid("block/hvac_duct_top")}})
    write(assets / "blockstates" / "hvac_duct.json", {"variants": {
        "axis=y": {"model": rid("block/hvac_duct")},
        "axis=z": {"model": rid("block/hvac_duct_horizontal"), "x": 90},
        "axis=x": {"model": rid("block/hvac_duct_horizontal"), "x": 90, "y": 90}}})
    write(assets / "items" / "hvac_duct.json", {"model": {"type": "minecraft:model", "model": rid("block/hvac_duct")}})
    # Doors: the vanilla door models and blockstate pattern.
    for door, name in DOORS.items():
        lang[f"block.{MOD}.{door}"] = name
        tex = {"bottom": rid(f"block/{door}_bottom"), "top": rid(f"block/{door}_top")}
        for half in ("bottom", "top"):
            for hinge in ("left", "right"):
                for opened in ("", "_open"):
                    write(models / f"{door}_{half}_{hinge}{opened}.json", {"parent": f"minecraft:block/door_{half}_{hinge}{opened}", "textures": tex})
        closed = {"east": 0, "south": 90, "west": 180, "north": 270}
        variants = {}
        for facing, y0 in closed.items():
            for half, part in (("lower", "bottom"), ("upper", "top")):
                for hinge in ("left", "right"):
                    for opened in (False, True):
                        y = (y0 + (90 if hinge == "left" else 270)) % 360 if opened else y0
                        v = {"model": rid(f"block/{door}_{part}_{hinge}{'_open' if opened else ''}")}
                        if y:
                            v["y"] = y
                        variants[f"facing={facing},half={half},hinge={hinge},open={'true' if opened else 'false'}"] = v
        write(assets / "blockstates" / f"{door}.json", {"variants": variants})
        write(assets / "models" / "item" / f"{door}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{door}")}})
        write(assets / "items" / f"{door}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{door}")}})
    # The roller door: slats in the middle of the opening, nothing when rolled up; while it rolls (moving) the block
    # model is empty and client/RollerDoorRenderer.java draws the curtain moving and the coil at the top.
    lang[f"block.{MOD}.roller_door"] = OTHER["roller_door"]
    write(models / "roller_door.json", {"textures": {"slats": rid("block/pi_roller_slats"), "particle": rid("block/pi_roller_slats")},
                                        "elements": [{"from": [0, 0, 6.5], "to": [16, 16, 9.5], "faces": {
                                            f: {"texture": "#slats", **({"uv": [0, 0, 16, 16]} if f in ("north", "south") else {})}
                                            for f in ("north", "south", "east", "west", "up", "down")}}]})
    write(models / "roller_door_open.json", {"textures": {"particle": rid("block/pi_roller_slats")}, "elements": []})
    write(assets / "blockstates" / "roller_door.json", {"variants": {
        f"facing={face},moving={m},open={o}": {"model": rid("block/roller_door" + ("_open" if o == "true" or m == "true" else "")),
                                                **({"y": y} if y else {})}
        for face, y in TURN for o in ("false", "true") for m in ("false", "true")}})
    write(assets / "items" / "roller_door.json", {"model": {"type": "minecraft:model", "model": rid("block/roller_door")}})
    # Signs: drawn by client/PlantSignRenderer.java; the block model only gives break particles.
    for sign in ("building_sign", "room_sign"):
        lang[f"block.{MOD}.{sign}"] = OTHER[sign]
        write(models / f"{sign}.json", {"textures": {"particle": rid("block/pi_sign")}})
        write(assets / "blockstates" / f"{sign}.json", {"variants": {"": {"model": rid(f"block/{sign}")}}})
        write(assets / "models" / "item" / f"{sign}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{sign}")}})
        write(assets / "items" / f"{sign}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{sign}")}})
    for key, text in SIGNS.items():
        lang[f"sign.{MOD}.{key}"] = text


def door_loot(rid, door):
    """Only the lower half drops the door (26.x loot format)."""
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(door),
            "condition": {"type": "minecraft:match_block", "blocks": rid(door), "state": {"half": "lower"}}}],
            "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": rid(f"blocks/{door}")}


# --------------------------------------------------------------------------------------------------- textures

S = M.CELL  # 128: drawn big, brought down to 64


def _img(colour):
    return M.to_img(M.base(colour, S))


def _speckle(img, seed, colours, count):
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for _ in range(count):
        x, y = r.randrange(S), r.randrange(S)
        d.point((x, y), fill=r.choice(colours))
    return img


def _rack_body():
    return M.painted((34, 35, 38), 301, chip_count=0, drip_count=0, dirt=0.05)


def _server_face():
    """Servers and switches behind the door: 1U/2U units, drive bays, patch panel with cables, status LEDs."""
    img = _img((20, 21, 24))
    d = ImageDraw.Draw(img)
    r = random.Random(302)
    y = 4
    while y < S - 6:
        h = r.choice((6, 6, 12, 12, 18))
        kind = r.random()
        d.rectangle([4, y, S - 5, y + h - 1], fill=(44, 46, 50, 255), outline=(16, 16, 18, 255))
        if kind < 0.18:      # patch panel with cables
            for x in range(10, S - 10, 6):
                d.rectangle([x, y + 1, x + 3, y + h - 2], fill=(18, 18, 20, 255))
                d.line([(x + 1, y + h - 2), (x + 1 + r.randint(-3, 3), y + h + 2)], fill=r.choice([(40, 110, 200, 255), (220, 200, 60, 255), (60, 170, 80, 255)]), width=2)
        else:
            for x in range(10, S - 30, 9):
                d.rectangle([x, y + 1, x + 6, y + h - 2], fill=(60, 62, 68, 255))
            for k in range(3):
                c = r.choice([(80, 230, 110), (80, 230, 110), (80, 160, 240), (250, 180, 40)])
                d.rectangle([S - 22 + k * 5, y + 2, S - 20 + k * 5, y + 3], fill=c + (254,))
        y += h + 1
    return img


def _server_mesh():
    """The perforated front door: a steel frame with a hex-perforated panel (the holes are cut out)."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (30, 31, 34)
    a[..., 3] = 255
    for yy in range(0, S):
        for xx in range(10, S - 10):
            cx = (xx - 10) % 6
            cy = (yy + (3 if ((xx - 10) // 6) % 2 else 0)) % 6
            if 1 <= cx <= 4 and 1 <= cy <= 4:
                a[yy, xx, 3] = 0
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 5, S - 1], fill=(52, 54, 58, 255))
    d.rectangle([S - 6, 0, S - 1, S - 1], fill=(52, 54, 58, 255))
    return img


def _rack_top():
    img = M.painted((34, 35, 38), 303, chip_count=0, drip_count=0, dirt=0.05)
    d = ImageDraw.Draw(img)
    for cx in (40, 88):
        d.ellipse([cx - 20, 44, cx + 20, 84], fill=(20, 20, 22, 255))
        for rr in range(4, 20, 4):
            d.ellipse([cx - rr, 64 - rr, cx + rr, 64 + rr], outline=(60, 62, 66, 255))
    d.rectangle([10, 100, 118, 116], fill=(18, 18, 20, 255))
    return img


def _cabinet(colour=(196, 198, 192), seed=304):
    return M.painted(colour, seed, seams=(("v", 64),), chip_count=1, drip_count=0, dirt=0.1)


def _relay_front():
    """A relay panel cabinet door: a glass window over rows of relays and terminal blocks, a label holder."""
    img = _cabinet()
    d = ImageDraw.Draw(img)
    r = random.Random(305)
    d.rectangle([12, 10, S - 13, 22], fill=(240, 240, 232, 255), outline=(90, 92, 90, 255))
    d.line([(18, 16), (80, 16)], fill=(40, 40, 44, 255), width=2)
    d.rectangle([12, 28, S - 13, S - 14], fill=(28, 34, 40, 255), outline=(90, 92, 90, 255), width=3)
    for row in range(5):
        y = 34 + row * 17
        d.rectangle([18, y + 12, S - 19, y + 14], fill=(70, 74, 78, 255))  # DIN rail
        for x in range(18, S - 24, 10):
            d.rectangle([x, y, x + 7, y + 11], fill=r.choice([(60, 64, 70, 255), (200, 200, 194, 255), (90, 120, 160, 255)]))
            if r.random() < 0.35:
                d.rectangle([x + 2, y + 2, x + 4, y + 3], fill=r.choice([(250, 80, 60), (80, 230, 110)]) + (254,))
    d.line([(20, 30), (S - 20, S - 20)], fill=(70, 90, 104, 255))   # glass reflection
    return img


def _locker_front():
    img = _cabinet((120, 132, 150), 306)
    d = ImageDraw.Draw(img)
    for x0 in (8, 68):
        d.rectangle([x0, 6, x0 + 52, S - 7], outline=(70, 80, 94, 255), width=2)
        for y in range(14, 34, 5):
            d.line([(x0 + 12, y), (x0 + 40, y)], fill=(50, 58, 70, 255), width=2)
        d.rectangle([x0 + 42, 60, x0 + 46, 74], fill=(200, 202, 204, 255))
        d.rectangle([x0 + 14, 40, x0 + 38, 48], fill=(230, 230, 222, 255))
    return img


def _upright():
    """A blue racking upright: painted steel with the punched slots the beams hook into."""
    img = M.painted((40, 92, 170), 307, chip_count=6, drip_count=0, dirt=0.2)
    d = ImageDraw.Draw(img)
    for y in range(4, S, 12):
        d.rectangle([56, y, 72, y + 6], fill=(14, 30, 64, 255))
    return img


def _beam():
    img = M.painted((226, 118, 30), 308, chip_count=8, drip_count=0, dirt=0.25)
    d = ImageDraw.Draw(img)
    d.rectangle([50, 30, 78, 98], fill=(250, 250, 244, 255), outline=(40, 40, 40, 255))
    d.line([(56, 50), (72, 50)], fill=(30, 30, 30, 255), width=4)
    return img


def _wire_deck():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 10):
        d.line([(x, 0), (x, S)], fill=(160, 164, 168, 255), width=2)
    for y in range(0, S, 30):
        d.line([(0, y), (S, y)], fill=(130, 134, 138, 255), width=3)
    return img


def _pallet():
    img = _img((150, 112, 70))
    d = ImageDraw.Draw(img)
    for y in range(0, S, 32):
        d.rectangle([0, y + 20, S, y + 31], fill=(60, 44, 28, 255))
    for x in (0, 56, 112):
        d.rectangle([x, 0, x + 16, S], fill=(132, 96, 58, 255))
    return _speckle(img, 309, [(120, 88, 52, 255), (170, 130, 84, 255)], 400)


def _pallet_top():
    img = _img((60, 44, 28))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 26):
        d.rectangle([x, 0, x + 19, S], fill=(160, 120, 76, 255))
        d.line([(x + 3, 8), (x + 3, S - 8)], fill=(130, 96, 58, 255))
    return img


def _carton():
    """Stacked cardboard boxes in stretch wrap, with shipping labels."""
    img = _img((176, 136, 88))
    d = ImageDraw.Draw(img)
    r = random.Random(310)
    for row in range(3):
        y0 = row * 43
        x = 0
        while x < S:
            w = r.choice((32, 42, 54))
            d.rectangle([x, y0, x + w - 1, y0 + 42], fill=r.choice([(176, 136, 88, 255), (164, 124, 78, 255), (186, 146, 96, 255)]),
                        outline=(120, 88, 52, 255))
            d.rectangle([x + w // 2 - 3, y0, x + w // 2 + 3, y0 + 42], fill=(196, 170, 120, 255))
            if r.random() < 0.6:
                d.rectangle([x + 6, y0 + 16, x + 22, y0 + 28], fill=(244, 244, 238, 255))
                d.line([(x + 8, y0 + 20), (x + 20, y0 + 20)], fill=(30, 30, 30, 255))
            x += w
    a = np.asarray(img).astype(np.float32)
    a[..., :3] = a[..., :3] * 0.9 + 26   # the stretch wrap's sheen
    return M.to_img(a)


def _drum():
    img = M.painted((40, 80, 150), 311, seams=(("h", 30), ("h", 98)), chip_count=6, drip_count=2, dirt=0.3)
    d = ImageDraw.Draw(img)
    d.rectangle([40, 50, 88, 78], fill=(240, 240, 230, 255))
    d.polygon([(64, 54), (72, 74), (56, 74)], fill=(240, 190, 30, 255), outline=(20, 20, 20, 255))
    return img


def _laminate():
    img = _img((196, 172, 136))
    a = np.asarray(img).astype(np.float32)
    M.shade(a, M.streaks(S, 312, "h", count=90, amount=10))
    return M.to_img(a)


def _office_screen():
    img = _img((236, 240, 244))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, 12], fill=(40, 90, 160, 254))
    for y in range(20, S - 6, 9):
        for x in range(6, S - 6, 24):
            d.rectangle([x, y, x + 21, y + 6], outline=(190, 196, 204, 254), fill=(250, 250, 252, 254))
    d.rectangle([70, 60, 120, 110], fill=(250, 250, 252, 254), outline=(150, 160, 170, 254))
    for i, h in enumerate((20, 34, 26, 40, 30)):
        d.rectangle([76 + i * 9, 104 - h, 82 + i * 9, 104], fill=(40, 120, 200, 254))
    a = np.asarray(img).copy()
    a[..., 3] = 254
    return Image.fromarray(a, "RGBA")


def _keyboard():
    img = _img((30, 30, 32))
    d = ImageDraw.Draw(img)
    for row in range(5):
        for k in range(14):
            d.rectangle([4 + k * 9, 10 + row * 22, 10 + k * 9, 26 + row * 22], fill=(56, 56, 60, 255))
    return img


def _paper():
    img = _img((246, 246, 242))
    d = ImageDraw.Draw(img)
    for y in range(16, S - 10, 8):
        d.line([(12, y), (12 + random.Random(y).randint(40, 100), y)], fill=(120, 120, 124, 255), width=2)
    return img


def _filing_front():
    img = _cabinet((178, 180, 176), 313)
    d = ImageDraw.Draw(img)
    for k in range(4):
        y = 4 + k * 31
        d.rectangle([6, y, S - 7, y + 28], outline=(110, 112, 110, 255), width=2)
        d.rectangle([44, y + 8, 84, y + 13], fill=(90, 92, 94, 255))
        d.rectangle([56, y + 16, 72, y + 22], fill=(236, 236, 228, 255))
    return img


def _whiteboard():
    img = _img((246, 248, 248))
    d = ImageDraw.Draw(img)
    d.text((10, 10), "OUTAGE PLAN", fill=(30, 60, 160, 255))
    for i, c in enumerate([(30, 60, 160), (200, 40, 40), (30, 140, 60)]):
        d.line([(12, 34 + i * 14), (12 + 30 + i * 24, 34 + i * 14)], fill=c + (255,), width=2)
    d.rectangle([70, 60, 116, 108], outline=(40, 40, 44, 255), width=2)
    d.line([(74, 100), (88, 80), (98, 90), (112, 66)], fill=(200, 40, 40, 255), width=2)
    d.rectangle([0, 0, S - 1, S - 1], outline=(170, 174, 178, 255), width=5)
    return img


def _cooler(front=False):
    img = M.painted((226, 228, 224), 314, chip_count=0, drip_count=0, dirt=0.05)
    if front:
        d = ImageDraw.Draw(img)
        d.rectangle([40, 20, 88, 44], fill=(60, 62, 66, 255))
        d.rectangle([48, 26, 56, 36], fill=(60, 120, 220, 255))
        d.rectangle([72, 26, 80, 36], fill=(220, 60, 50, 255))
        d.rectangle([30, 90, 98, 96], fill=(150, 152, 156, 255))
    return img


def _bottle():
    img = _img((120, 180, 230))
    d = ImageDraw.Draw(img)
    for y in (20, 60, 100):
        d.line([(0, y), (S, y)], fill=(90, 150, 210, 255), width=3)
    d.line([(30, 0), (30, S)], fill=(190, 225, 245, 255), width=6)
    return img


def _rail_yellow():
    return M.painted((232, 190, 30), 315, chip_count=10, drip_count=0, dirt=0.3)


def _steel():
    return M.brushed((176, 180, 184), 316, "v", dirt=0.1)


def _reader():
    img = M.brushed((176, 180, 184), 317, "h", dirt=0.1)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([70, 10, 120, 60], radius=8, fill=(24, 26, 30, 254))
    d.ellipse([84, 24, 106, 46], fill=(60, 230, 110, 254))
    return img


def _ceiling_tile():
    """A suspended ceiling: two by two fissured mineral tiles in a white T-bar grid."""
    img = _img((232, 232, 226))
    img = _speckle(img, 318, [(206, 206, 200, 255), (214, 214, 208, 255), (196, 196, 190, 255)], 900)
    d = ImageDraw.Draw(img)
    for p in (0, 63, 64, 127):
        d.line([(p, 0), (p, S)], fill=(246, 246, 242, 255), width=1)
        d.line([(0, p), (S, p)], fill=(246, 246, 242, 255), width=1)
    for p in (1, 62, 65, 126):
        d.line([(p, 0), (p, S)], fill=(184, 184, 178, 255))
        d.line([(0, p), (S, p)], fill=(184, 184, 178, 255))
    return img


def _vinyl_floor():
    img = _img((198, 200, 196))
    img = _speckle(img, 319, [(160, 162, 158, 255), (220, 222, 218, 255), (140, 146, 150, 255)], 1400)
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (S, 0)], fill=(170, 172, 168, 255))
    d.line([(0, 0), (0, S)], fill=(170, 172, 168, 255))
    return img


def _epoxy_floor():
    a = M.base((176, 180, 178), S)
    M.shade(a, M.fbm(S, 320, 4, 64) * 10)
    M.shade(a, M.grain(S, 321, 3))
    return M.to_img(a)


def _painted_wall():
    """Painted concrete block: off-white with faint mortar joints (two blocks high, running bond)."""
    a = M.base((226, 224, 214), S)
    M.shade(a, M.fbm(S, 322, 3, 32) * 6)
    img = M.to_img(a)
    d = ImageDraw.Draw(img)
    for row in range(4):
        y = row * 32
        d.line([(0, y), (S, y)], fill=(204, 202, 192, 255), width=2)
        off = 0 if row % 2 == 0 else 32
        for x in range(off, S, 64):
            d.line([(x, y), (x, y + 32)], fill=(204, 202, 192, 255), width=2)
    return img


def _tank_plate():
    img = M.painted((226, 228, 224), 323, seams=(("h", 64),), chip_count=2, drip_count=4, dirt=0.35)
    return img


def _louvre():
    return M.louvre(324)


def _duct(top=False):
    img = M.brushed((176, 180, 182), 325, "h", dirt=0.08)
    d = ImageDraw.Draw(img)
    if top:
        d.rectangle([0, 0, S - 1, S - 1], outline=(120, 124, 126, 255), width=10)
        d.rectangle([12, 12, S - 13, S - 13], fill=(60, 62, 64, 255))
    else:
        for y in (2, 62, 126):
            d.line([(0, y), (S, y)], fill=(130, 134, 136, 255), width=4)
        d.line([(0, 30), (S, 98)], fill=(196, 200, 202, 255), width=2)
        d.line([(0, 98), (S, 30)], fill=(196, 200, 202, 255), width=2)
    return img


def _roller_slats():
    img = M.painted((150, 156, 160), 326, chip_count=2, drip_count=2, dirt=0.3)
    d = ImageDraw.Draw(img)
    for y in range(0, S, 10):
        d.line([(0, y), (S, y)], fill=(100, 104, 108, 255), width=2)
        d.line([(0, y + 2), (S, y + 2)], fill=(186, 190, 194, 255))
    return img


def _steel_door(top):
    """A painted steel door leaf: frame edge, a narrow wired-glass vision panel in the top half (cut out so you can
    see through), the lever handle and lock at the top of the bottom half, a steel kick plate."""
    img = M.painted((150, 158, 164), 327 + top, chip_count=1, drip_count=1, dirt=0.15)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(96, 102, 108, 255), width=6)
    a = np.asarray(img).copy()
    if top:
        a[30:110, 58:96, 3] = 0           # the vision panel
        a[30:110, 58:96, :3] = 0
        img = Image.fromarray(a, "RGBA")
        d = ImageDraw.Draw(img)
        d.rectangle([56, 28, 97, 111], outline=(70, 74, 78, 255), width=3)
    else:
        img = Image.fromarray(a, "RGBA")
        d = ImageDraw.Draw(img)
        d.rectangle([92, 4, 102, 22], fill=(190, 192, 194, 255))
        d.rectangle([76, 8, 102, 13], fill=(206, 208, 210, 255))
        d.ellipse([94, 26, 100, 32], fill=(60, 62, 64, 255))
        d.rectangle([6, 96, S - 7, S - 7], fill=(176, 180, 184, 255), outline=(120, 124, 128, 255))
    return img


def _glass_door(top):
    """An aluminium-framed glass entrance door: dark bronze frame, clear glass (cut out), a push bar across."""
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (70, 66, 60)
    a[..., 3] = 255
    a[14:S - 14 if top else S - 30, 14:S - 14, 3] = 0
    if top:
        a[14:S - 14, 14:S - 14, 3] = 0
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    if not top:
        d.rectangle([14, 6, S - 15, 12], fill=(190, 194, 198, 255))   # the push bar
        d.rectangle([14, S - 30, S - 15, S - 15], fill=(70, 66, 60, 255))
    return img


def _sign_particle():
    return M.brushed((44, 48, 54), 329, "h", dirt=0.05)


def draw_block_textures():
    makers = {
        "pi_rack_body": _rack_body, "pi_server_face": _server_face, "pi_server_mesh": _server_mesh, "pi_rack_top": _rack_top,
        "pi_cabinet": _cabinet, "pi_cabinet_dark": lambda: _cabinet((112, 116, 118), 330), "pi_relay_front": _relay_front,
        "pi_locker_side": lambda: _cabinet((120, 132, 150), 331), "pi_locker_front": _locker_front,
        "pi_upright": _upright, "pi_beam": _beam, "pi_wire_deck": _wire_deck, "pi_pallet": _pallet, "pi_pallet_top": _pallet_top,
        "pi_carton": _carton, "pi_laminate": _laminate, "pi_desk_panel": lambda: _cabinet((150, 152, 150), 332),
        "pi_monitor": lambda: _img((24, 24, 26)), "pi_office_screen": _office_screen, "pi_keyboard": _keyboard, "pi_paper": _paper,
        "pi_filing_side": lambda: _cabinet((178, 180, 176), 333), "pi_filing_front": _filing_front, "pi_whiteboard": _whiteboard,
        "pi_chrome": lambda: M.brushed((196, 200, 204), 334, "h", dirt=0.05), "pi_cooler": _cooler,
        "pi_cooler_front": lambda: _cooler(True), "pi_bottle": _bottle, "pi_rail_yellow": _rail_yellow, "pi_steel": _steel,
        "pi_reader": _reader, "pi_roller_slats": _roller_slats, "pi_sign": _sign_particle,
        "ceiling_tile": _ceiling_tile, "vinyl_floor": _vinyl_floor, "epoxy_floor": _epoxy_floor, "painted_wall": _painted_wall,
        "tank_plate": _tank_plate, "intake_louvre": _louvre, "hvac_duct": _duct, "hvac_duct_top": lambda: _duct(True),
        "steel_door_bottom": lambda: _steel_door(0), "steel_door_top": lambda: _steel_door(1),
        "glass_door_bottom": lambda: _glass_door(0), "glass_door_top": lambda: _glass_door(1),
    }
    makers.update(plant_office.makers())
    makers.update(plant_office2.makers())
    del makers["pi_whiteboard"]
    missing = TEXTURES - set(makers)
    assert not missing, missing
    out = {}
    for name, make in makers.items():
        rich = name in ("pi_server_face", "pi_relay_front", "pi_office_screen", "pi_carton") or name in plant_office.RICH or name in plant_office2.RICH
        colours = 64 if name in plant_office2.PAINTINGS else 24 if rich else 14
        src = make()
        out[name] = M.stylize(src, colours)
        if name in plant_office2.O3.SHARP or name in plant_office2.O4.SHARP or name in plant_office2.PN.SHARP:
            out[name] = src            # kept at full size so the writing reads
        if name in plant_office2.TRANSLUCENT:
            alpha = src.convert("RGBA").split()[3].resize(out[name].size)
            out[name].putalpha(alpha)
    return out


def draw_item_textures():
    """Door and sign items, 64 px."""
    def door(top_img, bottom_img):
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        img.paste(top_img.resize((60, 62)), (34, 2))
        img.paste(bottom_img.resize((60, 62)), (34, 64))
        return M.stylize(img, 16)

    def sign(w):
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rectangle([64 - w, 40, 64 + w, 88], fill=(44, 48, 54, 255), outline=(190, 194, 198, 255), width=5)
        for x in range(64 - w + 14, 64 + w - 14, 14):
            d.rectangle([x, 56, x + 8, 72], fill=(240, 240, 236, 255))
        return M.stylize(img, 8)
    return {"steel_door": door(_steel_door(1), _steel_door(0)), "glass_door": door(_glass_door(1), _glass_door(0)),
            "building_sign": sign(60), "room_sign": sign(34), "conference_table": plant_office.conference_icon(),
            **{name: plant_office2.icon(name) for name in MULTI},
            **{name: plant_office2.O4.O6.uniform_icon(name) for name in UNIFORMS}}
