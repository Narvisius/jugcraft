"""The storage tank MBS and the safety kit round it (nuclear/StorageTanks.java; Justin's round 11, the "silo").

A storage tank is built from its blueprint (it is far bigger than 5x5): a ring of storage tank plates on a concrete
foundation, a roof of plates, an outlet nozzle (the controller) and an inlet nozzle low on the shell, a steel ladder
with a safety cage, a railing round the roof with a self-closing gate where the ladder comes up. When the outlet
finds the shell, roof and floor closed round a space it forms the tank: the space fills with invisible interior
blocks (nothing spawns in there), the plates turn from grey primer to the finished paint and it holds one fluid,
four buckets for every block of space inside.

Models are drawn facing north: whatever faces out (a nozzle, a ladder's climbing side, a manway) looks north (-z) and
the tank shell is on the south side (z = 16), as in vanilla's ladder. Elements are (from, to, texture[, options]) in
pixels, written through model_writer (which keeps flush faces from flickering).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import model_writer as MW
import nuclear_materials as M


# steampunk_models imports nuclear (through hightech_models), which imports this: its helpers are loaded when used.
def box(*args, **kwargs):
    import steampunk_models
    return steampunk_models.box(*args, **kwargs)


def cyl(*args, **kwargs):
    import steampunk_models
    return steampunk_models.cyl(*args, **kwargs)


def dial(*args, **kwargs):
    import steampunk_models
    return steampunk_models.dial(*args, **kwargs)

MOD = "jugcraft"
NAMES = {
    "storage_tank_plate": "Storage Tank Plate",
    "storage_tank_outlet": "Storage Tank Outlet",
    "storage_tank_inlet": "Storage Tank Inlet",
    "storage_tank_fill_pipe": "Storage Tank Fill Line",
    "storage_tank_manway": "Tank Manway",
    "storage_tank_vent": "Tank Roof Vent",
    "storage_tank_hatch": "Tank Roof Hatch",
    "railing": "Railing",
    "railing_gate": "Self-Closing Railing Gate",
    "caged_ladder": "Caged Steel Ladder",
    # The ratings' fittings (nuclear/TankFittings.java; Justin, Oct 7, the tank ratings).
    "storage_tank_pv_vent": "Pressure/Vacuum Vent",
    "storage_tank_scrubber_vent": "Scrubber Vent",
    "tank_foam_chamber": "Foam Chamber",
    "tank_foam_line": "Foam Line",
    "tank_earthing_boss": "Tank Earthing Boss",
    "safety_shower": "Safety Shower and Eyewash",
    "safety_shower_head": "Safety Shower Head",
    "tank_relief_valves": "Cryogenic Relief Valves",
    "cryogenic_vaporizer": "Ambient Vaporizer",
    "tank_leg": "Tank Leg",
}
INTERIOR = "storage_tank_interior"
BLOCKS = list(NAMES) + [INTERIOR]
# The plate's finished paint, by where it is in the formed shell (StorageTankPlateBlock.Band).
BANDS = ("wall", "band", "stripe", "rim", "roof")
# What a tank is rated for (nuclear/TankRating.java), and the paint of each band of a formed tank of that rating: a
# general tank white with the checker band; a flammable one with a red product stripe under it; a corrosive one
# rubber-lined, its shell darker, a yellow stripe; a cryogenic one plain white all over, no checker band.
RATINGS = ("general", "flammable", "corrosive", "cryogenic")
PAINT = {
    "general": {"wall": "st_wall", "band": "st_band", "stripe": "st_wall", "rim": "st_rim", "roof": "st_roof"},
    "flammable": {"wall": "st_wall", "band": "st_band", "stripe": "st_stripe_red", "rim": "st_rim", "roof": "st_roof"},
    "corrosive": {"wall": "st_wall_dark", "band": "st_band", "stripe": "st_stripe_yellow", "rim": "st_rim", "roof": "st_roof_dark"},
    "cryogenic": {"wall": "st_cryo", "band": "st_cryo", "stripe": "st_cryo", "rim": "st_cryo_rim", "roof": "st_cryo_roof"},
}
# Items: the upgrade parts, the shell section the kits are made of, and the kits (one places a whole tank).
KIT_SIZES = ("small", "medium", "large")
KITS = [f"{rating}_{size}" for rating in ("general", "flammable", "corrosive") for size in KIT_SIZES] + ["cryogenic"]
ITEM_NAMES = {"flame_arrester": "Flame Arrester", "tank_rubber_lining": "Tank Rubber Lining",
              "tank_shell_section": "Tank Shell Section"}
for _kit in KITS:
    _rating, _, _size = _kit.partition("_")
    ITEM_NAMES[f"tank_kit_{_kit}"] = ("Cryogenic Tank Kit" if _kit == "cryogenic"
                                      else f"{_size.title()} {_rating.title()} Tank Kit")
ITEMS = list(ITEM_NAMES)
# What a tank's outlet is stencilled for (StorageTankOutletBlock.Service).
SERVICES = ("none", "condensate", "refuelling_water", "diesel_fuel", "diesel_day_a", "diesel_day_b")
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

GUN, CHROME, RED = "dp_gunmetal", "dp_chrome", "sp_red_iron"
WHITE, PIPE, GALV, MESH, YELLOW = "st_white", "st_pipe", "st_galv", "st_mesh", "pi_rail_yellow"
GREEN, BLUE, BRASS, COPPER, FIN = "st_green", "st_blue", "st_brass", "st_copper", "st_fin"
TEXTURES = ["st_primer", "st_wall", "st_band", "st_rim", "st_roof", "st_white", "st_pipe", "st_galv", "st_mesh",
            "st_wall_dark", "st_roof_dark", "st_stripe_red", "st_stripe_yellow", "st_cryo", "st_cryo_roof", "st_cryo_rim",
            "st_green", "st_blue", "st_brass", "st_copper", "st_fin", "st_shower_sign", "st_rubber", "st_rubber_end"]

CRAFTING = {
    "storage_tank_plate": (["PPP", "PPP", "PP "], {"P": "#c:plates/steel"}, 8),
    "storage_tank_outlet": (["SVS", " P "], {"S": "#c:plates/steel", "V": "jugcraft:fluid_valve", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "storage_tank_inlet": (["SPS", " V "], {"S": "#c:plates/steel", "V": "jugcraft:fluid_valve", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "storage_tank_fill_pipe": (["P", "P", "P"], {"P": "jugcraft:steel_fluid_pipe"}, 3),
    "storage_tank_manway": (["SSS", "SNS", "SSS"], {"S": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 1),
    "storage_tank_vent": ([" P ", "PBP", " P "], {"P": "jugcraft:steel_fluid_pipe", "B": "minecraft:iron_bars"}, 1),
    "storage_tank_hatch": (["SSS", "N N"], {"S": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 1),
    "railing": (["NYN", "N N"], {"N": "minecraft:iron_nugget", "Y": "minecraft:yellow_dye"}, 6),
    "railing_gate": (["RIR"], {"R": "jugcraft:railing", "I": "minecraft:iron_ingot"}, 1),
    "caged_ladder": (["I I", "III", "I I"], {"I": "minecraft:iron_ingot"}, 8),
    # The ratings' parts and fittings.
    "flame_arrester": (["SBS", "BVB", "SBS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "V": "jugcraft:fluid_valve"}, 1),
    "tank_rubber_lining": (["RRR", "RSR", "RRR"], {"R": "jugcraft:rubber", "S": "#c:plates/steel"}, 1),
    "tank_shell_section": (["PPP", "PPP", "PPP"], {"P": "jugcraft:storage_tank_plate"}, 1),
    "storage_tank_pv_vent": ([" A ", " P "], {"A": "jugcraft:flame_arrester", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "storage_tank_scrubber_vent": (["SBS", " P "], {"S": "#c:plates/steel", "B": "minecraft:charcoal", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "tank_foam_chamber": (["SRS", " P "], {"S": "#c:plates/steel", "R": "minecraft:red_dye", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "tank_foam_line": (["P", "R", "P"], {"P": "jugcraft:steel_fluid_pipe", "R": "minecraft:red_dye"}, 3),
    "tank_earthing_boss": (["S", "C"], {"S": "#c:plates/steel", "C": "minecraft:copper_ingot"}, 1),
    "safety_shower": (["PGP", " P ", " S "], {"P": "jugcraft:steel_fluid_pipe", "G": "minecraft:green_dye", "S": "#c:plates/steel"}, 1),
    "safety_shower_head": (["PPP", " G "], {"P": "jugcraft:steel_fluid_pipe", "G": "minecraft:green_dye"}, 1),
    "tank_relief_valves": (["V V", "PPP"], {"V": "jugcraft:fluid_valve", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "cryogenic_vaporizer": (["APA", "APA", "APA"], {"A": "#c:plates/aluminum", "P": "jugcraft:steel_fluid_pipe"}, 2),
    "tank_leg": (["S ", "SS"], {"S": "#c:plates/steel"}, 2),
    # The kits: a small tank from shell sections and its nozzles, ladder and railing; a bigger one from four of the
    # size below; a rated kit from a general kit and the rating's part; the cryogenic tank its own.
    "tank_kit_general_small": (["RLR", "SSS", "OSI"], {"R": "jugcraft:railing", "L": "jugcraft:caged_ladder", "S": "jugcraft:tank_shell_section",
                                                       "O": "jugcraft:storage_tank_outlet", "I": "jugcraft:storage_tank_inlet"}, 1),
    "tank_kit_general_medium": (["KK", "KK"], {"K": "jugcraft:tank_kit_general_small"}, 1),
    "tank_kit_general_large": (["KK", "KK"], {"K": "jugcraft:tank_kit_general_medium"}, 1),
    "tank_kit_cryogenic": (["VCV", "SKS", "ACA"], {"V": "jugcraft:tank_relief_valves", "C": "jugcraft:ceramic_insulator",
                                                   "S": "jugcraft:tank_shell_section", "K": "jugcraft:tank_kit_general_small",
                                                   "A": "jugcraft:cryogenic_vaporizer"}, 1),
}
for _size in KIT_SIZES:
    CRAFTING[f"tank_kit_flammable_{_size}"] = (["K", "A"], {"K": f"jugcraft:tank_kit_general_{_size}", "A": "jugcraft:flame_arrester"}, 1)
    CRAFTING[f"tank_kit_corrosive_{_size}"] = (["K", "R"], {"K": f"jugcraft:tank_kit_general_{_size}", "R": "jugcraft:tank_rubber_lining"}, 1)


# ------------------------------------------------------------------------------------------------- the pieces

def port_body(role):
    """A nozzle low on the shell with its gate valve, facing out (north): an outside-screw-and-yoke gate valve in
    gunmetal on a pipe support, flanged at both ends, the bonnet and yoke on top (the rising stem and red handwheel
    are drawn by client/StorageTankRenderer, which turns them). Pipes join at the flange on the north face.
    The outlet's nozzle runs on into the shell through a flange and a reinforcing pad, with a drain valve under it;
    the inlet's turns up through an elbow into the fill line that climbs the shell."""
    vz0, vz1 = (6.2, 11.1) if role == "outlet" else (3.2, 8.2)
    vc = (vz0 + vz1) / 2
    m = []
    # The pipe support under the valve.
    m.append(box((5.5, 0, vc - 2), (10.5, 0.6, vc + 2), GUN))
    m.append(box((7.2, 0.6, vc - 0.8), (8.8, 4.6, vc + 0.8), GUN))
    m.append(box((6, 4.6, vc - 1.6), (10, 5.1, vc + 1.6), GUN))
    # The valve body: round along the pipe, the gate's housing standing up out of it, a flange at each end.
    m += cyl("z", 8, 8, 3.0, vz0 + 0.8, vz1 - 0.8, GUN)
    m.append(box((5.4, 6.2, vc - 1.8), (10.6, 11.2, vc + 1.8), GUN))
    m += cyl("z", 8, 8, 3.7, vz0, vz0 + 0.8, GUN)
    m += cyl("z", 8, 8, 3.7, vz1 - 0.8, vz1, GUN)
    # Bonnet, its flange, the two yoke arms and the yoke's head with the stem nut.
    m.append(box((5.2, 11.2, vc - 2.2), (10.8, 11.8, vc + 2.2), GUN))
    m.append(box((5.8, 11.8, vc - 1.8), (10.2, 13.0, vc + 1.8), GUN))
    for x in (6.0, 9.4):
        m.append(box((x, 13.0, vc - 0.4), (x + 0.6, 14.6, vc + 0.4), GUN))
    m.append(box((6.0, 14.6, vc - 0.7), (10.0, 15.2, vc + 0.7), GUN))
    m.append(box((7.2, 15.2, vc - 0.8), (8.8, 15.6, vc + 0.8), CHROME))
    # The pipe side: a spool out to the flange where pipes join.
    m += cyl("z", 8, 8, 3.7, 0, 0.8, GUN)
    m += cyl("z", 8, 8, 2.3, 0.8, vz0, PIPE)
    if role == "outlet":
        m += cyl("z", 8, 8, 2.3, vz1, 12.2, PIPE)
        m += cyl("z", 8, 8, 3.7, 12.2, 13.0, WHITE)       # the tank's nozzle flange, in the tank's paint
        m += cyl("z", 8, 8, 2.4, 13.0, 15.4, WHITE)
        m += cyl("z", 8, 8, 4.6, 15.4, 16.0, WHITE)       # reinforcing pad on the shell
        # The drain: a branch down from the nozzle, a ball valve and its red lever, the spout.
        m.append(box((7.4, 3.4, 13.6), (8.6, 5.7, 14.8), PIPE))
        m.append(box((6.9, 2.0, 13.3), (9.1, 3.4, 15.1), GUN))
        m.append(box((7.6, 1.0, 13.9), (8.4, 2.0, 14.5), PIPE))
        m.append(box((9.1, 2.5, 13.9), (12.6, 2.9, 14.5), RED))
    else:
        m += cyl("z", 8, 8, 2.3, vz1, 10.3, PIPE)
        m.append(box((5.6, 5.6, 10.3), (10.4, 10.4, 14.9), PIPE))      # the elbow
        m += cyl("y", 8, 12.6, 2.3, 10.4, 16, PIPE)                      # into the fill line above
        m.append(box((6.6, 0, 11.2), (9.4, 5.6, 14.0), GUN))             # a shoe under the elbow
        m.append(box((6.8, 6.8, 14.9), (9.2, 9.2, 16.0), GUN))           # a bracket back to the shell
    return m


def port_lamp(role, texture):
    """The lamp on the bonnet's front: green open, amber shut, dark while the tank is not formed."""
    vz0, vz1 = (6.2, 11.1) if role == "outlet" else (3.2, 8.2)
    vc = (vz0 + vz1) / 2
    return [dial("north", (8, 12.4, vc - 2.25), 1.3, texture=texture, body=CHROME)]


def port_wheel(role):
    """The stem and handwheel as they stand shut (only for the item: in the world the renderer draws them)."""
    import steampunk_models
    wheel = steampunk_models.wheel
    vz0, vz1 = (6.2, 11.1) if role == "outlet" else (3.2, 8.2)
    vc = (vz0 + vz1) / 2
    return [box((7.6, 13.0, vc - 0.4), (8.4, 16.0, vc + 0.4), CHROME)] + wheel("y", 8, vc, 3.4, 15.6, 16.0, RED, CHROME)


def fill_pipe(top):
    """A length of the inlet's fill line climbing the shell, 3.4 px off it, clamped to it at mid height; the top
    length turns into the shell through a flanged elbow (the fill enters at the top of the tank)."""
    m = []
    if top:
        m += cyl("y", 8, 12.6, 2.3, 0, 7.6, PIPE)
        m.append(box((5.7, 7.6, 10.3), (10.3, 12.2, 14.9), PIPE))
        m += cyl("z", 8, 9.9, 2.3, 14.9, 15.4, PIPE)
        m += cyl("z", 8, 9.9, 3.4, 15.4, 16.0, WHITE)
        clamp_y = 3.4
    else:
        m += cyl("y", 8, 12.6, 2.3, 0, 16, PIPE)
        clamp_y = 7.4
    # The clamp: a U-strap round the pipe on a stand-off welded to the shell.
    y0, y1 = clamp_y, clamp_y + 1.2
    m.append(box((5.3, y0, 9.9), (10.7, y1, 10.3), GUN))
    m.append(box((5.3, y0, 10.3), (5.7, y1, 15.2), GUN))
    m.append(box((10.3, y0, 10.3), (10.7, y1, 15.2), GUN))
    m.append(box((6.5, y0 - 0.6, 15.2), (9.5, y1 + 0.6, 16.0), GUN))
    return m


def manway():
    """A bolted manway on the shell near the foot: the pad round the opening, the cover plate, a ring of bolts and
    two handles (facing out, the shell behind it)."""
    m = cyl("z", 8, 8, 6.2, 15.3, 16.0, WHITE)
    m += cyl("z", 8, 8, 5.4, 14.2, 15.3, WHITE)
    for k in range(12):
        a = k / 12 * math.tau
        x, y = 8 + 4.7 * math.cos(a), 8 + 4.7 * math.sin(a)
        m.append(box((x - 0.4, y - 0.4, 13.7), (x + 0.4, y + 0.4, 14.2), GUN))
    for x in (5.2, 10.0):
        m.append(box((x, 6.2, 12.6), (x + 0.8, 6.8, 14.2), CHROME))
        m.append(box((x, 9.2, 12.6), (x + 0.8, 9.8, 14.2), CHROME))
        m.append(box((x, 6.2, 12.0), (x + 0.8, 9.8, 12.6), CHROME))
    return m


def vent():
    """The roof vent: a standpipe off a flange on the roof (or on the pipe it tops), the flame arrester housing
    (flanged, bolted), and a gooseneck turning the vent down, a screen over its mouth."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m += cyl("y", 8, 8, 2.3, 0.6, 5.6, PIPE)
    m += cyl("y", 8, 8, 3.9, 5.6, 6.2, GUN)
    m += cyl("y", 8, 8, 3.5, 6.2, 9.4, GUN)
    m += cyl("y", 8, 8, 3.9, 9.4, 10.0, GUN)
    m += cyl("y", 8, 8, 2.3, 10.0, 13.0, PIPE)
    m.append(box((5.7, 13.0, 0.9), (10.3, 15.6, 10.3), PIPE))
    m += cyl("y", 8, 3.2, 2.3, 10.6, 13.0, PIPE)
    m.append(box((5.9, 10.2, 1.1), (10.1, 10.6, 5.3), {"*": PIPE, "down": MESH}))
    return m


def vent_riser():
    """The vent at the foot of a vent line: the flange on the roof, the flame arrester, and the pipe on up into the
    steel pipe above it."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m += cyl("y", 8, 8, 2.3, 0.6, 4.6, PIPE)
    m += cyl("y", 8, 8, 3.9, 4.6, 5.2, GUN)
    m += cyl("y", 8, 8, 3.5, 5.2, 8.4, GUN)
    m += cyl("y", 8, 8, 3.9, 8.4, 9.0, GUN)
    m.append(box((6, 9.0, 6), (10, 16, 10), "steel_fluid_pipe"))
    return m


def hatch(formed, rating="general"):
    """A roof hatch, a cell of the roof itself (hinges at the back, -z): the roof plate round a square opening, the
    opening's sides dark, a galvanised safety grate across it a pixel below the roof (bearing bars and cross bars on
    a frame), the coaming standing three pixels proud of the roof round the opening, and on its back the hinge cheeks
    and pins the lid swings on (the lid itself is drawn by client/StorageTankHatchRenderer)."""
    top = PAINT[rating]["roof"] if formed else "st_primer"
    rim = PAINT[rating]["rim"]
    plate = {"*": top, "north": rim, "south": rim, "east": rim, "west": rim, "down": rim}
    m = [box((0, 0, 0), (16, 16, 3), {**plate, "north": top}), box((0, 0, 13), (16, 16, 16), {**plate, "south": top}),
         box((0, 0, 3), (3, 16, 13), {**plate, "west": top}), box((13, 0, 3), (16, 16, 13), {**plate, "east": top})]
    # The grate: a frame, three bearing bars front to back and three cross bars, galvanised.
    m.append(box((3, 14.2, 3), (13, 15.6, 3.6), GALV))
    m.append(box((3, 14.2, 12.4), (13, 15.6, 13), GALV))
    for x in (5.4, 7.7, 10.0):
        m.append(box((x, 14.0, 3.6), (x + 0.6, 15.6, 12.4), GALV))
    for z in (5.6, 8.0, 10.4):
        m.append(box((3, 14.4, z), (13, 15.0, z + 0.5), GALV))
    # The coaming, painted like the roof, its top edge dark.
    coat = {"*": PAINT[rating]["wall"] if formed else WHITE, "up": rim}
    m += [box((2.5, 16, 2.5), (13.5, 19, 3), coat), box((2.5, 16, 13), (13.5, 19, 13.5), coat),
          box((2.5, 16, 3), (3, 19, 13), coat), box((13, 16, 3), (13.5, 19, 13), coat)]
    # Two hinges: cheeks either side of where the lid's knuckle turns, and the pin through them.
    for x0, x1 in ((4.0, 5.5), (10.5, 12.0)):
        for cx in (x0 - 0.7, x1 + 0.1):
            m.append(box((cx, 16.0, 1.0), (cx + 0.6, 20.0, 2.5), GUN))
        m.append(box((x0 - 0.7, 19.1, 1.5), (x1 + 0.7, 19.7, 2.1), CHROME))
    # The stop the open lid rests back on.
    m.append(box((7.2, 16.0, 0.6), (8.8, 18.6, 2.5), GUN))
    return m


def hatch_lid():
    """The lid as it sits shut (for the item; in the world the renderer draws and swings it)."""
    m = [box((2.0, 19.0, 2.0), (14.0, 19.8, 14.0), WHITE),
         box((2.0, 18.1, 2.0), (2.5, 19.0, 14.0), WHITE), box((13.5, 18.1, 2.0), (14.0, 19.0, 14.0), WHITE),
         box((2.5, 18.1, 13.5), (13.5, 19.0, 14.0), WHITE)]
    for x in (4.0, 10.5):
        m.append(box((x, 19.8, 1.8), (x + 1.5, 20.15, 7.0), GUN))
    m += [box((6.0, 19.8, 11.6), (6.6, 21.2, 12.2), YELLOW), box((9.4, 19.8, 11.6), (10.0, 21.2, 12.2), YELLOW),
          box((6.0, 21.2, 11.6), (10.0, 21.8, 12.2), YELLOW)]
    return m


# ------------------------------------------------------------------------------------------------- the ratings' fittings

def pv_vent():
    """A flammable tank's roof vent: the nozzle off its roof flange, the flame arrester (a red housing between bolted
    flanges), the pressure/vacuum valve body with the vacuum pallet's housing off one side, and the weather hood over
    the pressure pallet."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m += cyl("y", 8, 8, 2.3, 0.6, 2.4, PIPE)
    m += cyl("y", 8, 8, 4.0, 2.4, 3.0, GUN)
    m += cyl("y", 8, 8, 4.4, 3.0, 6.6, RED)
    m += cyl("y", 8, 8, 4.0, 6.6, 7.2, GUN)
    for k in range(8):
        a = (k + 0.5) / 8 * math.tau
        x, z = 8 + 3.55 * math.cos(a), 8 + 3.55 * math.sin(a)
        m.append(box((x - 0.35, 7.2, z - 0.35), (x + 0.35, 7.6, z + 0.35), CHROME))
    m += cyl("y", 8, 8, 2.8, 7.2, 10.0, GUN)
    m.append(box((10.6, 7.6, 6.6), (12.8, 9.8, 9.4), GUN))         # the vacuum pallet's housing
    m.append(box((12.8, 7.9, 6.9), (13.0, 9.5, 9.1), {"*": GUN, "east": MESH}))
    m += cyl("y", 8, 8, 2.0, 10.0, 10.6, MESH)                     # the screened gap under the hood
    m += cyl("y", 8, 8, 4.8, 10.6, 11.2, GALV)
    m += cyl("y", 8, 8, 3.6, 11.2, 11.9, GALV)
    m += cyl("y", 8, 8, 1.8, 11.9, 12.6, GALV)
    return m


def scrubber_vent():
    """A corrosive tank's roof vent: the nozzle off its flange into a blue scrubber canister (bolted flanges top and
    bottom, a white service band), and a small vent stub out of its dished top."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m += cyl("y", 8, 8, 2.0, 0.6, 2.8, PIPE)
    m += cyl("y", 8, 8, 4.6, 2.8, 3.4, GUN)
    m += cyl("y", 8, 8, 4.2, 3.4, 7.0, BLUE)
    m += cyl("y", 8, 8, 4.25, 7.0, 7.8, WHITE)
    m += cyl("y", 8, 8, 4.2, 7.8, 11.2, BLUE)
    m += cyl("y", 8, 8, 4.6, 11.2, 11.8, GUN)
    m += cyl("y", 8, 8, 3.2, 11.8, 12.6, BLUE)
    m += cyl("y", 8, 8, 1.2, 12.6, 14.0, PIPE)
    m.append(box((6.4, 14.0, 6.4), (9.6, 14.8, 9.6), GUN))
    return m


def foam_chamber():
    """A foam chamber on the top course of a flammable tank (the shell to the south): the red chamber on its pad, a
    bolted front cover, the vapour seal box on top, and the foam line's riser coming up under it with its foam maker."""
    m = [box((4.0, 3.0, 15.4), (12.0, 13.0, 16.0), RED)]
    m += cyl("z", 8, 9, 3.4, 9.6, 15.4, RED)
    m += cyl("z", 8, 9, 3.9, 9.0, 9.6, GUN)
    m += cyl("z", 8, 9, 2.6, 8.4, 9.0, RED)
    for k in range(8):
        a = (k + 0.5) / 8 * math.tau
        x, y = 8 + 3.45 * math.cos(a), 9 + 3.45 * math.sin(a)
        m.append(box((x - 0.3, y - 0.3, 8.6), (x + 0.3, y + 0.3, 9.0), CHROME))
    m.append(box((6.2, 12.4, 10.6), (9.8, 13.8, 14.6), RED))       # the vapour seal box
    m += cyl("y", 8, 13.5, 1.3, 0, 5.8, RED)                        # the riser up from the foam line
    m += cyl("y", 8, 13.5, 1.9, 2.0, 2.8, GUN)                      # the foam maker's flanges
    m += cyl("y", 8, 13.5, 1.6, 2.8, 4.0, GALV)
    m += cyl("y", 8, 13.5, 1.9, 4.0, 4.6, GUN)
    return m


def foam_line():
    """A length of foam line up the shell to its chamber, red, clamped to the shell on a stand-off."""
    m = cyl("y", 8, 13.5, 1.3, 0, 16, RED)
    m += cyl("y", 8, 13.5, 1.7, 7.0, 8.0, GUN)
    m.append(box((7.4, 7.0, 15.2), (8.6, 8.0, 16.0), GUN))
    return m


def earthing_boss():
    """The earthing boss at a flammable tank's foot: a lug welded to the shell, the copper strap bolted to it and
    run down into the ground, a clamp on the strap."""
    m = [box((5.6, 3.0, 15.0), (10.4, 7.4, 16.0), GALV)]
    m.append(box((7.4, 4.6, 14.4), (8.6, 5.8, 15.0), CHROME))       # the bolt
    m.append(box((7.2, 4.4, 13.4), (8.8, 5.4, 15.0), COPPER))        # the strap off the lug...
    m.append(box((7.2, 0, 12.6), (8.8, 5.4, 13.4), COPPER))          # ...and down into the ground
    m.append(box((6.8, 1.8, 12.2), (9.2, 3.0, 13.8), GUN))           # the clamp
    return m


def safety_shower():
    """The safety shower's foot (facing out to whoever uses it): the riser on its floor flange, the foot pedal, the
    eyewash bowl on its arm with two nozzles and a push flag, and the green sign on the riser."""
    m = cyl("y", 8, 12.0, 2.4, 0, 0.6, GUN)
    m += cyl("y", 8, 12.0, 1.1, 0.6, 16, GREEN)
    m.append(box((5.6, 1.4, 5.6), (10.4, 1.9, 9.4), GALV))            # the pedal
    m.append(box((7.6, 0.6, 9.4), (8.4, 1.9, 10.9), GALV))
    m.append(box((7.3, 10.2, 7.8), (8.7, 11.2, 10.9), GREEN))         # the arm to the bowl
    m += cyl("y", 8, 4.6, 2.2, 9.2, 10.2, GREEN)                      # the bowl: its foot, floor and rim
    m.append(box((4.6, 10.2, 1.2), (11.4, 10.8, 8.0), GREEN))
    m += [box((4.0, 10.2, 0.6), (12.0, 11.8, 1.2), GREEN), box((4.0, 10.2, 8.0), (12.0, 11.8, 8.6), GREEN),
          box((4.0, 10.2, 1.2), (4.6, 11.8, 8.0), GREEN), box((11.4, 10.2, 1.2), (12.0, 11.8, 8.0), GREEN)]
    m.append(box((6.0, 10.8, 4.0), (7.0, 11.6, 5.0), YELLOW))          # the nozzles' dust caps
    m.append(box((9.0, 10.8, 4.0), (10.0, 11.6, 5.0), YELLOW))
    m.append(box((12.0, 10.4, 4.2), (14.6, 12.8, 5.0), YELLOW))         # the push flag
    m.append(box((4.6, 12.6, 10.4), (11.4, 15.8, 10.8), {"*": GREEN, "north": "st_shower_sign!"}))
    return m


def safety_shower_head():
    """The safety shower's top, on the block above its foot: the riser turning out over whoever stands under it, the
    shower head, and the pull rod with its handle."""
    m = cyl("y", 8, 12.0, 1.1, 0, 10.2, GREEN)
    m.append(box((6.8, 10.2, 10.8), (9.2, 12.6, 13.2), GREEN))         # the elbow
    m.append(box((7.0, 10.4, 6.6), (9.0, 12.4, 10.8), GREEN))          # the arm out
    m += cyl("y", 8, 5.6, 0.9, 8.8, 10.4, GREEN)
    m += cyl("y", 8, 5.6, 3.4, 7.6, 8.8, GREEN)                        # the head
    m += cyl("y", 8, 5.6, 3.0, 7.2, 7.6, CHROME)
    m.append(box((9.0, 10.8, 7.4), (11.6, 11.4, 8.2), GALV))           # the rod's bracket, the rod, its handle
    m.append(box((11.0, 0.8, 7.5), (11.6, 10.8, 8.1), GALV))
    m.append(box((9.8, 0.2, 7.5), (12.8, 0.8, 8.1), YELLOW))
    return m


def relief_valves():
    """A cryogenic tank's relief valves: two brass relief valves on a manifold off the roof flange, their outlets
    turned out to either side, test levers on top, and the manifold's pressure gauge."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m.append(box((3.8, 0.6, 6.4), (12.2, 3.0, 9.6), GALV))
    for x, out in ((5.4, -1), (10.6, 1)):
        m += cyl("y", x, 8, 1.6, 3.0, 6.6, BRASS)
        m += cyl("y", x, 8, 1.1, 6.6, 10.4, BRASS)
        m += cyl("y", x, 8, 1.4, 10.4, 11.4, BRASS)
        x0, x1 = (3.0, x) if out < 0 else (x, 13.0)
        m.append(box((x0, 4.0, 7.2), (x1, 5.6, 8.8), BRASS))
        m.append(box((x - 0.3, 11.4, 7.6), (x + 0.3, 13.8, 8.4), RED))
    m.append(dial("north", (8, 1.8, 6.0), 1.1, texture="dp_gauge", body=CHROME))
    return m


def vaporizer():
    """A length of an ambient vaporizer column: the aluminium tube with eight long fins round it, four square and
    four on the diagonals."""
    m = cyl("y", 8, 8, 1.4, 0, 16, FIN)
    m += [box((9.4, 0, 7.6), (15.4, 16, 8.4), FIN), box((0.6, 0, 7.6), (6.6, 16, 8.4), FIN),
          box((7.6, 0, 9.4), (8.4, 16, 15.4), FIN), box((7.6, 0, 0.6), (8.4, 16, 6.6), FIN)]
    # (The diagonal fins start clear of the tube's square corners, so no two fins' ends share a place.)
    for angle in (45, -45):
        m.append(box((10.1, 0, 7.6), (14.6, 16, 8.4), FIN, rotation=("y", angle, (8, 8, 8))))
        m.append(box((1.4, 0, 7.6), (5.9, 16, 8.4), FIN, rotation=("y", angle, (8, 8, 8))))
    return m


def tank_leg():
    """A leg under a cryogenic tank's floor: an H-section column on a bolted base plate, a bearing pad on top."""
    m = [box((4.0, 0, 4.0), (12.0, 0.8, 12.0), GUN)]
    for x, z in ((4.8, 4.8), (10.6, 4.8), (4.8, 10.6), (10.6, 10.6)):
        m.append(box((x, 0.8, z), (x + 0.6, 1.3, z + 0.6), CHROME))
    m += [box((5.4, 0.8, 5.6), (10.6, 15.2, 6.4), GALV), box((5.4, 0.8, 9.6), (10.6, 15.2, 10.4), GALV),
          box((7.6, 0.8, 6.4), (8.4, 15.2, 9.6), GALV)]
    m.append(box((4.6, 15.2, 4.6), (11.4, 16.0, 11.4), GUN))
    return m


# ------------------------------------------------------------------------------------------------- the parts, in 3D

def flame_arrester_part():
    """The flame arrester as it stands in a pressure/vacuum vent, laid on its side: the red housing (the arrester's
    element bank inside it, its band showing) between two bolted flanges, a pipe stub out of each."""
    m = cyl("x", 8, 8, 1.8, 0.6, 3.4, PIPE) + cyl("x", 8, 8, 1.8, 12.6, 15.4, PIPE)
    m += cyl("x", 8, 8, 3.4, 3.4, 4.4, GUN) + cyl("x", 8, 8, 3.4, 11.6, 12.6, GUN)
    m += cyl("x", 8, 8, 3.8, 4.4, 7.6, RED) + cyl("x", 8, 8, 3.8, 8.4, 11.6, RED)
    m += cyl("x", 8, 8, 3.95, 7.6, 8.4, GALV)
    for x in (3.2, 12.6):
        for k in range(6):
            a = (k + 0.5) / 6 * math.tau
            y, z = 8 + 2.7 * math.cos(a), 8 + 2.7 * math.sin(a)
            m.append(box((x, y - 0.35, z - 0.35), (x + 0.2, y + 0.35, z + 0.35), CHROME))
    return m


def rubber_lining_part():
    """A roll of rubber tank lining on its steel core, the free end of the sheet lying out in front."""
    m = cyl("x", 8, 7.6, 4.2, 2.6, 13.4, "st_rubber", cap="st_rubber_end")
    m += cyl("x", 8, 7.6, 1.2, 2.2, 2.6, GALV) + cyl("x", 8, 7.6, 1.2, 13.4, 13.8, GALV)
    m.append(box((2.6, 3.8, 9.6), (13.4, 4.3, 15.4), "st_rubber"))
    return m


def shell_section_part():
    """A curved tank shell section standing on its edge: steel plates in grey primer, bent to the tank's curve."""
    m = []
    for k in range(-3, 4):
        x0 = 8 + k * 1.9 - 0.95
        z = 6.4 + 0.32 * k * k
        m.append(box((x0, 1.5, z), (x0 + 1.9, 14.5, z + 1.2), "st_primer"))
    return m


def _cull(model):
    """Faces on the block's edge are hidden against a full neighbour (the roof plates round a hatch)."""
    for e in model["elements"]:
        for face, entry in e["faces"].items():
            axis, sign = MW._FACE_DIR[face]
            edge = e["to"][axis] if sign > 0 else e["from"][axis]
            if (sign > 0 and edge >= 16) or (sign < 0 and edge <= 0):
                entry["cullface"] = face
    return model


# The railing: a post, and an arm towards each joined neighbour (drawn here towards the north: the top rail, the
# knee rail and the kick plate at the foot).
def rail_post():
    return [box((7.0, 0, 7.0), (9.0, 16.0, 9.0), YELLOW)]


def rail_arm():
    return [box((7.3, 14.6, 0), (8.7, 15.8, 8), YELLOW), box((7.4, 7.6, 0), (8.6, 8.6, 8), YELLOW),
            box((7.7, 0, 0), (8.3, 2.0, 8), YELLOW)]


def gate_frame():
    """The gate's fixed parts, swinging to the north: the hinge post (with its spring) and the latch post with the
    stop the gate closes against. The gate itself is drawn by client/RailingGateRenderer, which swings it."""
    m = [box((0.4, 0, 7.1), (2.2, 16.0, 8.9), YELLOW)]
    m.append(box((2.2, 9.0, 7.5), (2.9, 13.0, 8.5), GUN))           # the closing spring
    m.append(box((13.8, 0, 7.1), (15.6, 16.0, 8.9), YELLOW))
    m.append(box((12.8, 8.0, 8.9), (13.8, 9.0, 9.6), GUN))           # the stop (the gate rests against it)
    return m


def gate_leaf():
    """The gate, closed (for the item model; the renderer draws the same boxes swung open)."""
    return [box((2.4, 14.4, 7.5), (13.8, 15.6, 8.5), YELLOW), box((2.4, 7.6, 7.6), (13.8, 8.6, 8.4), YELLOW),
            box((2.4, 1.8, 7.6), (13.8, 2.8, 8.4), YELLOW), box((2.4, 1.8, 7.5), (3.4, 15.6, 8.5), YELLOW),
            box((12.8, 1.8, 7.5), (13.8, 15.6, 8.5), YELLOW)]


def ladder_base():
    """A galvanised steel ladder standing off the wall (south): two flat-bar stringers, round rungs four to a block,
    and brackets back to the wall."""
    m = [box((2.4, 0, 10.6), (3.0, 16, 12.8), GALV), box((13.0, 0, 10.6), (13.6, 16, 12.8), GALV)]
    for y in (1.6, 5.6, 9.6, 13.6):
        m += cyl("x", y, 11.7, 0.5, 3.0, 13.0, GALV)
    for x0 in (2.4, 13.0):
        m.append(box((x0, 6.6, 12.8), (x0 + 0.6, 7.4, 15.4), GALV))
        m.append(box((x0 - 0.8, 5.8, 15.4), (x0 + 1.4, 8.2, 16.0), GALV))
    return m


def ladder_cage():
    """The safety cage round the climber: a hoop (flat bar, out from the stringers and round in front) and five
    vertical bars."""
    y0, y1 = 11.4, 12.0
    m = [box((4.0, y0, 0.4), (12.0, y1, 1.0), GALV),
         box((0.6, y0, 4.0), (1.2, y1, 11.4), GALV), box((14.8, y0, 4.0), (15.4, y1, 11.4), GALV),
         box((0.6, y0, 10.8), (2.4, y1, 11.4), GALV), box((13.6, y0, 10.8), (15.4, y1, 11.4), GALV)]
    m.append(box((0.3, y0, 2.2), (4.9, y1, 2.8), GALV, rotation=("y", 45, (2.6, (y0 + y1) / 2, 2.5))))
    m.append(box((11.1, y0, 2.2), (15.7, y1, 2.8), GALV, rotation=("y", -45, (13.4, (y0 + y1) / 2, 2.5))))
    for x0, z0 in ((0.65, 7.0), (14.85, 7.0), (5.0, 0.45), (7.75, 0.45), (10.5, 0.45)):
        m.append(box((x0, 0, z0), (x0 + 0.5, 16, z0 + 0.5), GALV))
    return m


def ladder_top():
    """The top length: the stringers carry on and bend over towards the platform as handholds."""
    m = []
    for x0 in (2.4, 13.0):
        m.append(box((x0, 13.6, 12.8), (x0 + 0.6, 14.4, 16.0), GALV))
    return m


# ------------------------------------------------------------------------------------------------- the textures

def _primer():
    return M.painted((128, 134, 130), 7301, seams=(("h", 126),), chip_count=3, drip_count=2, dirt=0.25)


def _wall():
    return M.painted((232, 234, 230), 7302, seams=(("h", 126),), chip_count=2, drip_count=4, dirt=0.3)


def _band():
    img = M.painted((226, 184, 34), 7303, seams=(("h", 126),), chip_count=3, drip_count=0, dirt=0.2)
    d = ImageDraw.Draw(img)
    for row in range(2):
        for col in range(2):
            if (row + col) % 2:
                d.rectangle([col * 64, row * 64, col * 64 + 63, row * 64 + 63], fill=(26, 26, 28, 255))
    d.line([(0, 126), (127, 126)], fill=(40, 40, 42, 255), width=2)
    return img


def _rim():
    return M.painted((54, 58, 62), 7304, seams=(("h", 126),), chip_count=4, drip_count=2, dirt=0.25)


def _roof():
    img = M.painted((222, 224, 220), 7305, chip_count=2, drip_count=0, dirt=0.35)
    d = ImageDraw.Draw(img)
    # Lapped roof plates: a weld line along one edge and across one corner, and a few spatter dots.
    d.line([(0, 1), (127, 1)], fill=(176, 178, 174, 255), width=2)
    d.line([(1, 0), (1, 127)], fill=(190, 192, 188, 255), width=1)
    rr = random.Random(7306)
    for _ in range(18):
        x, y = rr.randrange(128), rr.randrange(128)
        d.point((x, y), fill=(160, 162, 158, 255))
    return img


def _white():
    return M.painted((236, 236, 232), 7307, chip_count=1, drip_count=1, dirt=0.2)


def _pipe():
    return M.painted((188, 192, 190), 7308, chip_count=4, drip_count=1, dirt=0.3)


def _galv():
    """Hot-dip galvanised steel: dull silver with the zinc's spangle (large faint crystals)."""
    a = M.base((168, 174, 176))
    rng = np.random.default_rng(7309)
    pts = rng.integers(0, 128, (40, 2))
    yy, xx = np.mgrid[0:128, 0:128]
    d = np.full((128, 128), 1e9)
    near = np.zeros((128, 128), int)
    for i, (px, py) in enumerate(pts):
        dist = (xx - px) ** 2 + (yy - py) ** 2
        mask = dist < d
        d[mask] = dist[mask]
        near[mask] = i
    shift = rng.normal(0, 7, len(pts))[near]
    a[..., :3] = np.clip(a[..., :3] + shift[..., None], 0, 255)
    M.shade(a, M.grain(128, 7310, 4))
    return M.to_img(a)


def _mesh():
    img = Image.new("RGBA", (128, 128), (40, 42, 44, 255))
    d = ImageDraw.Draw(img)
    for k in range(0, 128, 8):
        d.line([(k, 0), (k, 127)], fill=(120, 124, 128, 255), width=2)
        d.line([(0, k), (127, k)], fill=(120, 124, 128, 255), width=2)
    return img


def _wall_dark():
    """A corrosive tank's shell: rubber-lined steel painted a dark grey, so a spill shows."""
    return M.painted((92, 96, 100), 7311, seams=(("h", 126),), chip_count=2, drip_count=3, dirt=0.25)


def _roof_dark():
    img = M.painted((98, 102, 106), 7312, chip_count=2, drip_count=0, dirt=0.3)
    d = ImageDraw.Draw(img)
    d.line([(0, 1), (127, 1)], fill=(70, 72, 76, 255), width=2)
    d.line([(1, 0), (1, 127)], fill=(78, 80, 84, 255), width=1)
    return img


def _stripe(colour, seed, edge):
    """The product stripe painted round the course under the checker band: the colour from edge to edge, a thin line
    of the shell's own paint where it meets the next course, the course's weld seam."""
    img = M.painted(colour, seed, seams=(("h", 126),), chip_count=3, drip_count=0, dirt=0.2)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 5], fill=edge + (255,))
    d.rectangle([0, 118, 127, 123], fill=edge + (255,))
    return img


def _cryo():
    """A cryogenic tank's outer shell: smooth white paint over the vacuum jacket, a fine vertical seam."""
    return M.painted((240, 242, 240), 7315, seams=(("v", 64),), chip_count=0, drip_count=1, dirt=0.12)


def _cryo_roof():
    return M.painted((232, 234, 232), 7316, chip_count=0, drip_count=0, dirt=0.15)


def _cryo_rim():
    return M.painted((186, 190, 192), 7317, chip_count=1, drip_count=0, dirt=0.15)


def _fin():
    """Extruded aluminium: bright, with the extrusion's fine lines running up it."""
    return M.brushed((206, 212, 216), 7318, "v", dirt=0.05)


def _shower_sign():
    """The safety shower's sign: white on green, a figure under a shower's spray and an eye with a jet of water."""
    img = Image.new("RGBA", (128, 128), (0, 136, 74, 255))
    d = ImageDraw.Draw(img)
    w = (250, 250, 246, 255)
    d.rectangle([4, 4, 123, 123], outline=w, width=5)
    d.rectangle([30, 20, 70, 26], fill=w)                         # the shower's arm and head
    d.polygon([(54, 26), (78, 26), (86, 36), (46, 36)], fill=w)
    for x in (50, 60, 70, 80):                                    # the spray
        d.line([(x, 42), (x - 4, 58)], fill=w, width=3)
    d.ellipse([56, 62, 72, 78], fill=w)                           # the figure
    d.rectangle([58, 80, 70, 104], fill=w)
    d.line([(58, 84), (46, 70)], fill=w, width=5)
    d.line([(70, 84), (82, 70)], fill=w, width=5)
    d.rectangle([58, 104, 62, 116], fill=w)
    d.rectangle([66, 104, 70, 116], fill=w)
    return img


def _rubber_end():
    """The end of a roll of rubber sheet: the turns of the sheet round the steel core, a thin light line between them."""
    img = M.rubber(7335)
    d = ImageDraw.Draw(img)
    for r in range(18, 64, 7):
        d.ellipse([64 - r, 64 - r, 64 + r, 64 + r], outline=(92, 94, 98, 255), width=2)
    d.ellipse([46, 46, 82, 82], fill=(150, 154, 158, 255), outline=(70, 72, 76, 255), width=3)
    return img


def draw_block_textures():
    makers = {"st_primer": _primer, "st_wall": _wall, "st_band": _band, "st_rim": _rim, "st_roof": _roof,
              "st_white": _white, "st_pipe": _pipe, "st_galv": _galv, "st_mesh": _mesh,
              "st_wall_dark": _wall_dark, "st_roof_dark": _roof_dark,
              "st_stripe_red": lambda: _stripe((196, 36, 32), 7313, (232, 234, 230)),
              "st_stripe_yellow": lambda: _stripe((232, 188, 30), 7314, (92, 96, 100)),
              "st_cryo": _cryo, "st_cryo_roof": _cryo_roof, "st_cryo_rim": _cryo_rim,
              "st_green": lambda: M.painted((0, 136, 74), 7319, chip_count=2, drip_count=0, dirt=0.15),
              "st_blue": lambda: M.painted((40, 92, 170), 7331, chip_count=2, drip_count=1, dirt=0.15),
              "st_brass": lambda: M.brushed((196, 160, 70), 7332, "h", dirt=0.1),
              "st_copper": lambda: M.copper(7333), "st_fin": _fin, "st_shower_sign": _shower_sign,
              "st_rubber": lambda: M.rubber(7334), "st_rubber_end": _rubber_end}
    sharp = {"st_band": 16, "st_shower_sign": 6}
    return {name: M.stylize(make(), sharp.get(name, 14)) for name, make in makers.items()}


# The renderers' sheet (client/StorageTankRenderer, StorageTankHatchRenderer, RailingGateRenderer): 512 x 256 px,
# 64 px regions, 8 across and 4 down; the first four columns as before, then the inside of a tank and the hatch lid's
# gasket in the fifth to eighth of the top row.
SHEET = {"red_iron": (0, 0), "chrome": (1, 0), "gunmetal": (2, 0), "yellow": (3, 0),
         "gauge_board": (0, 1), "pointer": (1, 1), "black": (2, 1), "galvanised": (3, 1),
         "white": (0, 2), "nfpa_blue": (1, 2), "nfpa_red": (2, 2), "nfpa_yellow": (3, 2),
         "rim": (0, 3), "spring": (1, 3), "hazard_tag": (2, 3), "nfpa_white": (3, 3),
         "lining": (4, 0), "lining_floor": (5, 0), "lining_roof": (6, 0), "gasket": (7, 0),
         "dark_paint": (4, 1)}


def _lining(colour, seed, floor=False):
    """The inside of a tank: oxide primer gone dark, a weld seam round each course, streaks of rust running down
    (on the floor, sediment instead)."""
    img = M.painted(colour, seed, seams=() if floor else (("h", 122),), chip_count=0, drip_count=0 if floor else 7, dirt=0.9)
    d = ImageDraw.Draw(img)
    rr = random.Random(seed)
    if floor:
        for _ in range(60):
            x, y, r = rr.randrange(128), rr.randrange(128), rr.choice((1, 1, 2, 3))
            d.ellipse([x - r, y - r, x + r, y + r], fill=tuple(max(0, c - rr.randrange(10, 30)) for c in colour) + (255,))
    else:
        for x in range(0, 128, 3):
            y = 121 + rr.randint(-1, 1)
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(68, 52, 44, 255))
        for _ in range(5):
            x = rr.randrange(8, 120)
            d.line([(x, 0), (x + rr.randint(-2, 2), rr.randrange(30, 110))], fill=(120, 66, 40, 255), width=rr.choice((1, 2)))
    return img


def _gauge_board():
    """One block's length of the float gauge's board: white, a black edge, a long tick every half block and short
    ticks between (the numbers are lettered by the renderer)."""
    img = Image.new("RGBA", (128, 128), (240, 240, 236, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 127], outline=(30, 30, 32, 255), width=5)
    for k in range(10):
        y = int(k * 12.8)
        w = 70 if k % 5 == 0 else 36
        d.rectangle([6, y, 6 + w, y + 3], fill=(24, 24, 26, 255))
    return img


def draw_sheet():
    tiles = {
        "red_iron": M.painted((160, 34, 30), 7320, chip_count=6, drip_count=0, dirt=0.2),
        "chrome": M.brushed((206, 210, 214), 7321, "v", dirt=0.05),
        "gunmetal": M.painted((62, 66, 70), 7322, chip_count=4, drip_count=0, dirt=0.2),
        "yellow": M.painted((232, 190, 30), 7323, chip_count=8, drip_count=0, dirt=0.25),
        "gauge_board": _gauge_board(),
        "pointer": M.painted((200, 30, 26), 7324, chip_count=0, drip_count=0, dirt=0.1),
        "black": Image.new("RGBA", (128, 128), (24, 24, 26, 255)),
        "galvanised": _galv(),
        "white": _white(),
        "nfpa_blue": Image.new("RGBA", (128, 128), (36, 86, 190, 255)),
        "nfpa_red": Image.new("RGBA", (128, 128), (206, 36, 34, 255)),
        "nfpa_yellow": Image.new("RGBA", (128, 128), (240, 210, 40, 255)),
        "rim": _rim(),
        "spring": M.brushed((70, 74, 78), 7325, "h", dirt=0.1),
        "hazard_tag": M.hazard(7326),
        "nfpa_white": Image.new("RGBA", (128, 128), (246, 246, 242, 255)),
        "lining": _lining((92, 70, 60), 7327),
        "lining_floor": _lining((70, 60, 54), 7328, floor=True),
        "lining_roof": _lining((84, 74, 68), 7329),
        "gasket": M.rubber(7330),
        "dark_paint": _wall_dark(),
    }
    sheet = Image.new("RGBA", (512, 256), (0, 0, 0, 255))
    for name, (col, row) in SHEET.items():
        sheet.paste(tiles[name].resize((64, 64), Image.LANCZOS).convert("RGBA"), (col * 64, row * 64))
    return sheet


# ------------------------------------------------------------------------------------------------- writing

def _model(elements, rid, extra=None, glow=()):
    out = []
    for item in elements:
        frm, to, texture, options = MW.unpack(item)
        e = MW.element(frm, to, texture, rotation=options.get("rotation"))
        names = {f["texture"][1:] for f in e["faces"].values()}
        if names & set(glow):
            e["light_emission"] = 15
        out.append(e)
    names = MW.texture_names(elements)
    textures = {n: rid(f"block/{n}") for n in names}
    textures["particle"] = textures[names[0]]
    if extra:
        textures.update(extra)
    return {"textures": textures, "elements": out}


def _turn(y):
    return {"y": y} if y else {}


def write_assets(write, rid, assets, lang):
    import zfight
    write = zfight.unfighting(write)  # no two faces of a model in the same place (Justin: never any z-fighting)
    models = assets / "models" / "block"
    for block, name in NAMES.items():
        lang[f"block.{MOD}.{block}"] = name
    lang[f"block.{MOD}.{INTERIOR}"] = "Storage Tank Interior"

    # Plates: grey primer until the tank forms, then painted by where they are in the shell and what the tank is
    # rated for (PAINT); one model for each paint.
    write(models / "storage_tank_plate.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/st_primer")}})
    for paint in sorted({p for bands in PAINT.values() for p in bands.values()}):
        write(models / f"storage_tank_plate_{paint[3:]}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{paint}")}})

    def plate_model(band, formed, rating):
        return rid("block/storage_tank_plate" + (f"_{PAINT[rating][band][3:]}" if formed == "true" else ""))
    write(assets / "blockstates" / "storage_tank_plate.json", {"variants": {
        f"band={band},formed={formed},rating={rating}": {"model": plate_model(band, formed, rating)}
        for band in BANDS for formed in ("false", "true") for rating in RATINGS}})
    write(assets / "items" / "storage_tank_plate.json", {"model": {"type": "minecraft:model", "model": rid("block/storage_tank_plate")}})

    # The interior: nothing to see (the block is invisible); the model only gives break particles.
    write(models / f"{INTERIOR}.json", {"textures": {"particle": rid("block/st_primer")}})
    write(assets / "blockstates" / f"{INTERIOR}.json", {"variants": {"": {"model": rid(f"block/{INTERIOR}")}}})

    # Nozzles: the body turned by facing, and the lamp (green open, amber shut, dark until the tank forms).
    lamps = {"open": "el_lamp_on", "shut": "dp_lamp_on", "off": "dp_lamp"}
    for role in ("outlet", "inlet"):
        block = f"storage_tank_{role}"
        write(models / f"{block}.json", _model(port_body(role), rid))
        for state, tex in lamps.items():
            write(models / f"{block}_lamp_{state}.json", _model(port_lamp(role, tex), rid, glow=("el_lamp_on", "dp_lamp_on")))
        write(models / f"{block}_inventory.json", _model(port_body(role) + port_lamp(role, "el_lamp_on") + port_wheel(role), rid))
        parts = []
        for face, y in FACINGS:
            parts.append({"when": {"facing": face}, "apply": {"model": rid(f"block/{block}"), **_turn(y)}})
            parts.append({"when": {"facing": face, "formed": "true", "open": "true", "powered": "false"},
                          "apply": {"model": rid(f"block/{block}_lamp_open"), **_turn(y)}})
            parts.append({"when": {"facing": face, "formed": "true", "open": "false"},
                          "apply": {"model": rid(f"block/{block}_lamp_shut"), **_turn(y)}})
            parts.append({"when": {"facing": face, "formed": "true", "open": "true", "powered": "true"},
                          "apply": {"model": rid(f"block/{block}_lamp_shut"), **_turn(y)}})
            parts.append({"when": {"facing": face, "formed": "false"}, "apply": {"model": rid(f"block/{block}_lamp_off"), **_turn(y)}})
        write(assets / "blockstates" / f"{block}.json", {"multipart": parts})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}_inventory")}})

    # The fill line: a length, or the top length turning into the shell.
    write(models / "storage_tank_fill_pipe.json", _model(fill_pipe(False), rid))
    write(models / "storage_tank_fill_pipe_top.json", _model(fill_pipe(True), rid))
    write(assets / "blockstates" / "storage_tank_fill_pipe.json", {"variants": {
        f"facing={face},top={top}": {"model": rid("block/storage_tank_fill_pipe" + ("_top" if top == "true" else "")), **_turn(y)}
        for face, y in FACINGS for top in ("false", "true")}})
    write(assets / "items" / "storage_tank_fill_pipe.json", {"model": {"type": "minecraft:model", "model": rid("block/storage_tank_fill_pipe")}})

    # The roof hatch: a cell of the roof, primer until the tank forms; turned so its hinges are on the side it faces.
    write(models / "storage_tank_hatch.json", _cull(_model(hatch(False), rid)))
    for rating in RATINGS:
        suffix = "" if rating == "general" else f"_{rating}"
        write(models / f"storage_tank_hatch_formed{suffix}.json", _cull(_model(hatch(True, rating), rid)))
    write(models / "storage_tank_hatch_inventory.json", _model(hatch(True) + hatch_lid(), rid))

    def hatch_model(formed, rating):
        if formed == "false":
            return rid("block/storage_tank_hatch")
        return rid("block/storage_tank_hatch_formed" + ("" if rating == "general" else f"_{rating}"))
    write(assets / "blockstates" / "storage_tank_hatch.json", {"variants": {
        f"band={band},facing={face},formed={formed},open={o},rating={rating}": {"model": hatch_model(formed, rating), **_turn(y)}
        for band in BANDS for face, y in FACINGS for formed in ("false", "true") for o in ("false", "true") for rating in RATINGS}})
    write(assets / "items" / "storage_tank_hatch.json", {"model": {"type": "minecraft:model", "model": rid("block/storage_tank_hatch_inventory")}})

    # The roof vent: a gooseneck, or the foot of a vent line with a pipe on top of it.
    write(models / "storage_tank_vent.json", _model(vent(), rid))
    write(models / "storage_tank_vent_riser.json", _model(vent_riser(), rid))
    write(assets / "blockstates" / "storage_tank_vent.json", {"variants": {
        f"facing={face},riser={r}": {"model": rid("block/storage_tank_vent" + ("_riser" if r == "true" else "")), **_turn(y)}
        for face, y in FACINGS for r in ("false", "true")}})
    write(assets / "items" / "storage_tank_vent.json", {"model": {"type": "minecraft:model", "model": rid("block/storage_tank_vent")}})

    # Manway and the ratings' fittings: plain pieces turned by facing.
    for block, elements in (("storage_tank_manway", manway()), ("storage_tank_pv_vent", pv_vent()),
                            ("storage_tank_scrubber_vent", scrubber_vent()), ("tank_foam_chamber", foam_chamber()),
                            ("tank_foam_line", foam_line()), ("tank_earthing_boss", earthing_boss()),
                            ("safety_shower", safety_shower()), ("safety_shower_head", safety_shower_head()),
                            ("tank_relief_valves", relief_valves()), ("cryogenic_vaporizer", vaporizer()),
                            ("tank_leg", tank_leg())):
        write(models / f"{block}.json", _model(elements, rid))
        write(assets / "blockstates" / f"{block}.json", {"variants": {
            f"facing={face}": {"model": rid(f"block/{block}"), **_turn(y)} for face, y in FACINGS}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})

    # The railing joins like a fence: a post (at corners, ends and every other block of a straight run) and an arm
    # to each railing or gate beside it.
    write(models / "railing_post.json", _model(rail_post(), rid))
    write(models / "railing_arm.json", _model(rail_arm(), rid))
    # The item: a post with an arm each way (north and south).
    south_arm = [box((b[0][0], b[0][1], 16 - b[1][2]), (b[1][0], b[1][1], 16 - b[0][2]), b[2]) for b in rail_arm()]
    write(models / "railing_inventory.json", _model(rail_post() + rail_arm() + south_arm, rid))
    parts = [{"when": {"post": "true"}, "apply": {"model": rid("block/railing_post")}}]
    for face, y in FACINGS:
        parts.append({"when": {face: "true"}, "apply": {"model": rid("block/railing_arm"), **_turn(y)}})
    write(assets / "blockstates" / "railing.json", {"multipart": parts})
    write(assets / "items" / "railing.json", {"model": {"type": "minecraft:model", "model": rid("block/railing_inventory")}})

    # The gate: its posts in the block model; the gate itself drawn swinging by the renderer.
    write(models / "railing_gate.json", _model(gate_frame(), rid))
    write(models / "railing_gate_inventory.json", _model(gate_frame() + gate_leaf(), rid))
    write(assets / "blockstates" / "railing_gate.json", {"variants": {
        f"facing={face},open={o}": {"model": rid("block/railing_gate"), **_turn(y)} for face, y in FACINGS for o in ("false", "true")}})
    write(assets / "items" / "railing_gate.json", {"model": {"type": "minecraft:model", "model": rid("block/railing_gate_inventory")}})

    # The steel ladder: the ladder, its cage from the third block up, the handholds on the top length.
    write(models / "caged_ladder.json", _model(ladder_base(), rid))
    write(models / "caged_ladder_cage.json", _model(ladder_cage(), rid))
    write(models / "caged_ladder_top.json", _model(ladder_top(), rid))
    parts = []
    for face, y in FACINGS:
        parts.append({"when": {"facing": face}, "apply": {"model": rid("block/caged_ladder"), **_turn(y)}})
        parts.append({"when": {"facing": face, "cage": "true"}, "apply": {"model": rid("block/caged_ladder_cage"), **_turn(y)}})
        parts.append({"when": {"facing": face, "top": "true"}, "apply": {"model": rid("block/caged_ladder_top"), **_turn(y)}})
    write(assets / "blockstates" / "caged_ladder.json", {"multipart": parts})
    write(assets / "items" / "caged_ladder.json", {"model": {"type": "minecraft:model", "model": rid("block/caged_ladder")}})

    lang.update({
        f"block.{MOD}.storage_tank_outlet.desc": "The tank's controller: forms the tank, holds its fluid",
        f"message.{MOD}.storage_tank.opened": "%s valve opened. %s",
        f"message.{MOD}.storage_tank.shut": "%s valve shut. %s",
        f"message.{MOD}.storage_tank.held_shut": "%s valve held shut by a redstone signal. %s",
        f"message.{MOD}.storage_tank.inlet": "Inlet",
        f"message.{MOD}.storage_tank.outlet": "Outlet",
        f"message.{MOD}.storage_tank.contents": "%s: %s / %s buckets",
        f"message.{MOD}.storage_tank.empty": "Empty (%s buckets)",
        f"message.{MOD}.storage_tank.not_formed": "Tank not formed: the shell, roof and floor must close round the space inside, with the outlet on the shell",
        f"message.{MOD}.storage_tank.no_tank": "Not on a formed tank: an inlet works once the tank's outlet has formed it",
        f"message.{MOD}.storage_tank.hatch": "Roof hatch. %s. Pour liquid in here from a bucket",
        f"message.{MOD}.storage_tank.hatch_other": "This tank holds %s: it won't take %s as well",
        f"message.{MOD}.storage_tank.hatch_full": "Not enough room in the tank for that. %s",
        f"tank.{MOD}.condensate.name": "CONDENSATE STORAGE TANK",
        f"tank.{MOD}.condensate.tag": "CST-01",
        f"tank.{MOD}.refuelling_water.name": "REFUELLING WATER STORAGE TANK",
        f"tank.{MOD}.refuelling_water.tag": "RWST-01",
        f"tank.{MOD}.diesel_fuel.name": "DIESEL FUEL OIL",
        f"tank.{MOD}.diesel_fuel.tag": "DFO-01",
        f"tank.{MOD}.diesel_fuel.warning": "NO SMOKING",
        f"tank.{MOD}.diesel_fuel.warning2": "NO NAKED FLAMES",
        f"tank.{MOD}.diesel_day_a.name": "DIESEL FUEL OIL DAY TANK",
        f"tank.{MOD}.diesel_day_a.tag": "DT-1A",
        f"tank.{MOD}.diesel_day_a.warning": "NO SMOKING",
        f"tank.{MOD}.diesel_day_a.warning2": "NO NAKED FLAMES",
        f"tank.{MOD}.diesel_day_b.name": "DIESEL FUEL OIL DAY TANK",
        f"tank.{MOD}.diesel_day_b.tag": "DT-1B",
        f"tank.{MOD}.diesel_day_b.warning": "NO SMOKING",
        f"tank.{MOD}.diesel_day_b.warning2": "NO NAKED FLAMES",
        f"tank.{MOD}.empty": "EMPTY",
        f"screen.{MOD}.tank.title": "Storage Tank",
        f"screen.{MOD}.tank.not_formed": "Not formed: the shell, roof and floor must close round the space inside",
        f"screen.{MOD}.tank.fluid": "Fluid",
        f"screen.{MOD}.tank.empty": "Empty",
        f"screen.{MOD}.tank.contents": "Contents",
        f"screen.{MOD}.tank.of": "of %s mB (%s buckets)",
        f"screen.{MOD}.tank.fill": "%s%% full",
        f"screen.{MOD}.tank.in": "In",
        f"screen.{MOD}.tank.out": "Out",
        f"screen.{MOD}.tank.net": "Net",
        f"screen.{MOD}.tank.full_in": "Full in %s",
        f"screen.{MOD}.tank.empty_in": "Empty in %s",
        f"screen.{MOD}.tank.steady": "Steady",
        f"screen.{MOD}.tank.outlet": "Outlet",
        f"screen.{MOD}.tank.open": "Open (feeding its line)",
        f"screen.{MOD}.tank.shut": "Shut",
        f"screen.{MOD}.tank.held_shut": "Held shut (redstone)",
        f"screen.{MOD}.tank.hint": "Right-click the outlet to open or shut it",
        f"screen.{MOD}.tank.rating": "Rated for %s",
    })
    write_rating_assets(write, rid, assets, lang)


RATING_NAMES = {"general": "general liquids", "flammable": "flammable liquids", "corrosive": "corrosive liquids",
                "cryogenic": "cryogenic liquids"}
WARNINGS = {"flammable": "FLAMMABLE", "no_smoking": "NO SMOKING", "no_naked_flames": "NO NAKED FLAMES",
            "corrosive": "CORROSIVE", "no_water": "DO NOT ADD WATER", "oxidizer": "OXIDIZER",
            "face_shield": "WEAR FACE SHIELD", "cryogenic": "CRYOGENIC LIQUID"}
KIT_TOOLTIPS = {"general": "Holds water, brine, fracking fluid and other general liquids",
                "flammable": "Rated for flammable liquids: fuels, naphtha, crude oil (and general liquids)",
                "corrosive": "Rubber-lined for corrosive liquids: acids, lye, electrolyte (and general liquids)",
                "cryogenic": "Vacuum-insulated, for cryogenic liquids: liquid oxygen"}


def write_rating_assets(write, rid, assets, lang):
    """The items (the upgrade parts, the shell section and the kits, each shown in 3D as it is in the world) and the
    words of the ratings: their names, the label's warning lines, the messages and the kits'
    tooltips."""
    # Each shows in the slot as it is in the world, in 3D (Justin, Oct 8): a kit as the very tank it places, in
    # miniature (tools/tank_kit_models.py); a part as the part itself.
    import tank_kit_models
    parts = {"flame_arrester": flame_arrester_part(), "tank_rubber_lining": rubber_lining_part(),
             "tank_shell_section": shell_section_part()}
    for item, name in ITEM_NAMES.items():
        lang[f"item.{MOD}.{item}"] = name
        if item in parts:
            model = dict(_model(parts[item], rid), parent="minecraft:block/block")
        else:
            model = tank_kit_models.item_model(item[len("tank_kit_"):], rid)
        write(assets / "models" / "item" / f"{item}.json", model)
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for rating, name in RATING_NAMES.items():
        lang[f"tank.{MOD}.rating.{rating}"] = name
        lang[f"tooltip.{MOD}.tank_kit.{rating}"] = KIT_TOOLTIPS[rating]
    for key, words in WARNINGS.items():
        lang[f"tank.{MOD}.warning.{key}"] = words
    lang.update({
        f"message.{MOD}.storage_tank.titled": "Tank titled %s",
        f"message.{MOD}.storage_tank.rerated": "Tank re-rated for %s: repainted and refitted",
        f"message.{MOD}.storage_tank.already_rated": "This tank is already rated for %s",
        f"message.{MOD}.storage_tank.cryogenic_fixed": "A cryogenic tank is built cold: it keeps its rating, and no other tank can be made cryogenic",
        f"message.{MOD}.storage_tank.empty_first": "Empty the tank before re-rating it for %s",
        f"message.{MOD}.storage_tank.refused": "This tank is rated for %s: %s needs a tank rated for %s",
        f"message.{MOD}.storage_tank.refused_gas": "%s is a gas: store it in a gas holder or gas cylinders",
        f"message.{MOD}.storage_tank.refused_lava": "%s is too hot for a storage tank: keep it in a steel tank",
        f"message.{MOD}.tank_kit.missing": "This kit's tank is missing from the mod's data",
        f"message.{MOD}.tank_kit.no_room": "Not enough room here for the tank",
        f"message.{MOD}.tank_kit.blocked": "Something is in the way: %s at %s, %s, %s",
        f"tooltip.{MOD}.tank_kit.size": "%s x %s, %s tall",
        f"tooltip.{MOD}.tank_kit.use": "Use on the ground to place the whole tank, reaching away from you. Breaking any part of it takes it all down and gives the kit back, empty",
        f"tooltip.{MOD}.flame_arrester": "Use on an empty storage tank to re-rate it for flammable liquids",
        f"tooltip.{MOD}.tank_rubber_lining": "Use on an empty storage tank to re-rate it for corrosive liquids",
        f"tooltip.{MOD}.tank_shell_section": "Nine tank plates welded into a curved section: what tank kits are made of",
    })


def draw_item_textures():
    """None: the tank items show in 3D (write_rating_assets)."""
    return {}


def write_loot(write, rid, out, self_drop):
    for block in NAMES:
        if block == "storage_tank_outlet":
            # The outlet holds the tank's fluid: broken, its item carries it (like a tinplate tank).
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
                "type": "minecraft:item", "name": rid(block), "modifier": {"type": "minecraft:copy_components",
                                                                          "source": "block_entity", "include": [f"{MOD}:stored_fluid"]}}],
                "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": rid(f"blocks/{block}")})
        else:
            write(out / f"{block}.json", self_drop(block))
    write(out / f"{INTERIOR}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{INTERIOR}")})


def write_tags(tags, rid):
    for block in NAMES:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:climbable", rid("caged_ladder"))
