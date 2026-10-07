"""The storage tank MBS and the safety kit round it (nuclear/StorageTanks.java; Justin's round 11, the "silo").

A storage tank is built from its blueprint (it is far bigger than 5x5): a ring of storage tank plates on a concrete
foundation, a roof of plates, an outlet nozzle (the controller) and an inlet nozzle low on the shell, a steel ladder
with a safety cage, a railing round the roof with a self-closing gate where the ladder comes up. When the outlet
finds the shell, roof and floor closed round a space it forms the tank: the space fills with invisible interior
blocks (nothing spawns in there), the plates turn from grey primer to the finished paint and it holds one fluid,
a bucket for every block of space inside.

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
    "steel_ladder": "Steel Ladder",
}
INTERIOR = "storage_tank_interior"
BLOCKS = list(NAMES) + [INTERIOR]
# The plate's finished paint, by where it is in the formed shell (StorageTankPlateBlock.Band).
BANDS = ("wall", "band", "rim", "roof")
# What a tank's outlet is stencilled for (StorageTankOutletBlock.Service).
SERVICES = ("none", "condensate", "refuelling_water", "diesel_fuel")
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

GUN, CHROME, RED = "dp_gunmetal", "dp_chrome", "sp_red_iron"
WHITE, PIPE, GALV, MESH, YELLOW = "st_white", "st_pipe", "st_galv", "st_mesh", "pi_rail_yellow"
TEXTURES = ["st_primer", "st_wall", "st_band", "st_rim", "st_roof", "st_white", "st_pipe", "st_galv", "st_mesh"]

CRAFTING = {
    "storage_tank_plate": (["PPP", "P P", "PPP"], {"P": "#c:plates/steel"}, 8),
    "storage_tank_outlet": (["SVS", " P "], {"S": "#c:plates/steel", "V": "jugcraft:fluid_valve", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "storage_tank_inlet": (["SPS", " V "], {"S": "#c:plates/steel", "V": "jugcraft:fluid_valve", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "storage_tank_fill_pipe": (["P", "P", "P"], {"P": "jugcraft:steel_fluid_pipe"}, 3),
    "storage_tank_manway": (["SSS", "SNS", "SSS"], {"S": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 1),
    "storage_tank_vent": ([" P ", "PBP", " P "], {"P": "jugcraft:steel_fluid_pipe", "B": "minecraft:iron_bars"}, 1),
    "storage_tank_hatch": (["SSS", "N N"], {"S": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 1),
    "railing": (["NYN", "N N"], {"N": "minecraft:iron_nugget", "Y": "minecraft:yellow_dye"}, 6),
    "railing_gate": (["RIR"], {"R": "jugcraft:railing", "I": "minecraft:iron_ingot"}, 1),
    "steel_ladder": (["I I", "III", "I I"], {"I": "minecraft:iron_ingot"}, 8),
}


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
    """The roof vent: a standpipe off a flange on the roof, the flame arrester housing (flanged, bolted), and a
    gooseneck turning the vent down, a screen over its mouth."""
    m = cyl("y", 8, 8, 3.6, 0, 0.6, GUN)
    m += cyl("y", 8, 9.6, 2.3, 0.6, 5.6, PIPE)
    m += cyl("y", 8, 9.6, 3.9, 5.6, 6.2, GUN)
    m += cyl("y", 8, 9.6, 3.5, 6.2, 9.4, GUN)
    m += cyl("y", 8, 9.6, 3.9, 9.4, 10.0, GUN)
    m += cyl("y", 8, 9.6, 2.3, 10.0, 13.0, PIPE)
    m.append(box((5.7, 13.0, 2.5), (10.3, 15.6, 11.9), PIPE))
    m += cyl("y", 8, 4.8, 2.3, 10.6, 13.0, PIPE)
    m.append(box((5.9, 10.2, 2.7), (10.1, 10.6, 6.9), {"*": PIPE, "down": MESH}))
    return m


def hatch():
    """A roof hatch: a square curb, the lid on two hinges at the back, a yellow lifting handle at the front."""
    m = [box((3, 0, 3), (13, 1.8, 13), "st_rim")]
    m.append(box((2.6, 1.8, 2.6), (13.4, 2.5, 13.4), WHITE))
    for x in (4.5, 10.5):
        m.append(box((x, 1.4, 13.4), (x + 1.0, 2.6, 14.0), GUN))
    m.append(box((6.0, 2.5, 3.6), (10.0, 3.3, 4.2), YELLOW))
    for x in (6.0, 9.4):
        m.append(box((x, 2.5, 4.2), (x + 0.6, 3.3, 4.8), YELLOW))
    return m


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


def draw_block_textures():
    makers = {"st_primer": _primer, "st_wall": _wall, "st_band": _band, "st_rim": _rim, "st_roof": _roof,
              "st_white": _white, "st_pipe": _pipe, "st_galv": _galv, "st_mesh": _mesh}
    return {name: M.stylize(make(), 16 if name == "st_band" else 14) for name, make in makers.items()}


# The renderers' sheet (client/StorageTankRenderer, RailingGateRenderer): 256 px, 64 px regions, 4 by 4.
SHEET = ["red_iron", "chrome", "gunmetal", "yellow",
         "gauge_board", "pointer", "black", "galvanised",
         "white", "nfpa_blue", "nfpa_red", "nfpa_yellow",
         "rim", "spring", "hazard_tag", "nfpa_white"]


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
    }
    sheet = Image.new("RGBA", (256, 256), (0, 0, 0, 255))
    for k, name in enumerate(SHEET):
        sheet.paste(tiles[name].resize((64, 64), Image.LANCZOS).convert("RGBA"), ((k % 4) * 64, (k // 4) * 64))
    return sheet


def draw_item_textures():
    return {}


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
    models = assets / "models" / "block"
    for block, name in NAMES.items():
        lang[f"block.{MOD}.{block}"] = name
    lang[f"block.{MOD}.{INTERIOR}"] = "Storage Tank Interior"

    # Plates: grey primer until the tank forms, then painted by where they are in the shell.
    write(models / "storage_tank_plate.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/st_primer")}})
    for band in BANDS:
        write(models / f"storage_tank_plate_{band}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/st_{band}")}})
    write(assets / "blockstates" / "storage_tank_plate.json", {"variants": {
        f"band={band},formed={formed}": {"model": rid("block/storage_tank_plate" + (f"_{band}" if formed == "true" else ""))}
        for band in BANDS for formed in ("false", "true")}})
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

    # Manway, roof vent, roof hatch: plain pieces turned by facing.
    for block, elements in (("storage_tank_manway", manway()), ("storage_tank_vent", vent()), ("storage_tank_hatch", hatch())):
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
    write(models / "steel_ladder.json", _model(ladder_base(), rid))
    write(models / "steel_ladder_cage.json", _model(ladder_cage(), rid))
    write(models / "steel_ladder_top.json", _model(ladder_top(), rid))
    parts = []
    for face, y in FACINGS:
        parts.append({"when": {"facing": face}, "apply": {"model": rid("block/steel_ladder"), **_turn(y)}})
        parts.append({"when": {"facing": face, "cage": "true"}, "apply": {"model": rid("block/steel_ladder_cage"), **_turn(y)}})
        parts.append({"when": {"facing": face, "top": "true"}, "apply": {"model": rid("block/steel_ladder_top"), **_turn(y)}})
    write(assets / "blockstates" / "steel_ladder.json", {"multipart": parts})
    write(assets / "items" / "steel_ladder.json", {"model": {"type": "minecraft:model", "model": rid("block/steel_ladder")}})

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
        f"tank.{MOD}.condensate.name": "CONDENSATE STORAGE TANK",
        f"tank.{MOD}.condensate.tag": "CST-01",
        f"tank.{MOD}.refuelling_water.name": "REFUELLING WATER STORAGE TANK",
        f"tank.{MOD}.refuelling_water.tag": "RWST-01",
        f"tank.{MOD}.diesel_fuel.name": "DIESEL FUEL OIL",
        f"tank.{MOD}.diesel_fuel.tag": "DFO-1A",
        f"tank.{MOD}.diesel_fuel.warning": "NO SMOKING",
        f"tank.{MOD}.diesel_fuel.warning2": "NO NAKED FLAMES",
        f"tank.{MOD}.empty": "EMPTY",
    })


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
    tags.add("block", "minecraft:climbable", rid("steel_ladder"))
