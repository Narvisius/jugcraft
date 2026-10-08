"""Power lines (docs/features/power-lines.md): the supports, the grid-connection boxes and their art, models, names,
loot and recipes. powerline/LineKind.java lists the same kinds in the same order (checked by tools/check_mod_data.py),
and client/LineRenderer.java draws them from the material sheet made here (regions checked too)."""
import math
import random

from PIL import Image, ImageDraw

import structure_items

MOD = "jugcraft"
FEATURE = "machines"

# id: (display, family, tier, capacity JE/t, height, reach). Same order as LineKind.java.
KINDS = {
    "wooden_pole": ("Wooden Pole", "wood", 1, 4_096, 12, 32),
    "double_arm_pole": ("Double-Arm Pole", "wood", 2, 8_192, 13, 32),
    "spun_concrete_pole": ("Spun-Concrete Pole", "wood", 3, 16_384, 14, 32),
    "steel_monopole": ("Steel Monopole", "monopole", 1, 131_072, 30, 64),
    "double_circuit_monopole": ("Double-Circuit Monopole", "monopole", 2, 262_144, 32, 64),
    "compact_monopole": ("Compact Monopole", "monopole", 3, 524_288, 36, 64),
    "lattice_tower": ("Lattice Tower", "lattice", 1, 2_097_152, 56, 128),
    "reconductored_lattice_tower": ("Reconductored Lattice Tower", "lattice", 2, 3_145_728, 56, 128),
    "monitored_lattice_tower": ("Monitored Lattice Tower", "lattice", 3, 3_670_016, 56, 128),
    "uhv_tower": ("765 kV UHV Tower", "uhv", 1, 4_194_304, 65, 160),
    "uhvdc_tower": ("UHVDC Tower", "uhvdc", 1, 5_242_880, 70, 160),
    "superconducting_pylon": ("Superconducting Pylon", "superconducting", 1, 16_777_216, 20, 64),
    "service_pedestal": ("Service Pedestal", "box", 1, 16_384, 2, 32),
    "pad_mount_transformer": ("Pad-Mount Transformer", "box", 2, 524_288, 2, 64),
    "unit_substation": ("Unit Substation", "box", 3, 16_777_216, 11, 160),
    "converter_station": ("HVDC Converter Station", "box", 4, 5_242_880, 14, 160),
}
FRAME = "power_line_frame"

TOOLTIPS = {
    "wooden_pole": "Distribution line. Wires to wooden poles; feeds a Service Pedestal or a Pad-Mount Transformer",
    "double_arm_pole": "25 kV distribution: two crossarms, a pin on top",
    "spun_concrete_pole": "35 kV distribution on polymer post insulators",
    "steel_monopole": "138 kV sub-transmission. Wires to steel monopoles; feeds a Pad-Mount Transformer or a Unit Substation",
    "double_circuit_monopole": "230 kV, two circuits",
    "compact_monopole": "345 kV compact line on braced post insulators",
    "lattice_tower": "500 kV transmission, four-conductor bundles. Stands its legs on uneven ground",
    "reconductored_lattice_tower": "Carbon-core conductors: more current on the same tower",
    "monitored_lattice_tower": "Line sensors let it run closer to its limit",
    "uhv_tower": "765 kV, six-conductor bundles on V strings",
    "uhvdc_tower": "+/-1,100 kV DC, eight-conductor bundles. Ends at HVDC Converter Stations",
    "superconducting_pylon": "Cryogenic superconducting cable. Carries power only while a Unit Substation on the line has nitrogen",
    "service_pedestal": "Where a wooden pole's service drop becomes the mod's cables",
    "pad_mount_transformer": "Fed by a riser down a wooden pole or monopole (buried cable, no overhead wire)",
    "unit_substation": "Takes every line but UHVDC on its gantry. Holds nitrogen for superconducting lines",
    "converter_station": "Where a UHVDC line becomes the mod's cables",
}

LANG = {
    f"block.{MOD}.{FRAME}": "Power Line Structure",
    "death.attack.jugcraft.electrocution": "%1$s touched a live power line",
    "death.attack.jugcraft.electrocution.player": "%1$s touched a live power line while fighting %2$s",
    "tooltip.jugcraft.power_line.capacity": "Carries %s JE/t",
    "tooltip.jugcraft.power_line.size": "%s m tall, wires reach %s blocks",
    "message.jugcraft.power_line.readout": "%s: line rate %s JE/t, %s%% loaded, %s structures, %s wires",
    "message.jugcraft.power_line.too_steep": "The ground is too uneven for its legs",
    "message.jugcraft.power_line.no_room": "Not enough room for it here",
    "message.jugcraft.power_line.not_a_structure": "That is not a power line structure",
    "message.jugcraft.power_line.incompatible": "Those two cannot be wired together",
    "message.jugcraft.power_line.too_far": "Too far apart for a wire",
    "message.jugcraft.power_line.already_wired": "Already wired together",
    "message.jugcraft.power_line.full": "One of them already has all the wires it can take",
    "message.jugcraft.power_line.blocked": "Something is in the way of the wires",
    "message.jugcraft.power_line.wired": "Wired",
    "message.jugcraft.power_line.cut": "Wire cut",
    "message.jugcraft.power_line.selected": "Selected: sneak-use another structure to wire them (or cut the wire)",
    "message.jugcraft.power_line.upgraded": "Upgraded",
}

# Each tier costs the one below it plus pricier materials; each family's first tier starts from the family below's top.
CRAFTING = {
    "wooden_pole": (["TCT", " L ", " L "], {"T": "minecraft:white_terracotta", "C": "jugcraft:copper_cable", "L": "minecraft:spruce_log"}, 1),
    "double_arm_pole": (["TCT", "IPI", " L "], {"T": "minecraft:white_terracotta", "C": "jugcraft:silver_cable", "I": "#c:ingots/iron",
                                               "P": "jugcraft:wooden_pole", "L": "minecraft:spruce_log"}, 1),
    "spun_concrete_pole": (["SCS", "RPR", "RRR"], {"S": "#c:ingots/steel", "C": "jugcraft:silver_cable", "R": "minecraft:gray_concrete",
                                                  "P": "jugcraft:double_arm_pole"}, 1),
    "steel_monopole": (["TAT", "PWP", "PCP"], {"T": "minecraft:white_terracotta", "A": "jugcraft:aluminum_cable", "P": "#c:plates/steel",
                                              "W": "jugcraft:spun_concrete_pole", "C": "minecraft:gray_concrete"}, 1),
    "double_circuit_monopole": (["AAA", "PMP", "PSP"], {"A": "jugcraft:aluminum_cable", "P": "#c:plates/steel",
                                                       "M": "jugcraft:steel_monopole", "S": "#c:ingots/steel"}, 1),
    "compact_monopole": (["XAX", "PMP", "PCP"], {"X": "jugcraft:plastic_sheet", "A": "jugcraft:aluminum_cable", "P": "#c:plates/steel",
                                                "M": "jugcraft:double_circuit_monopole", "C": "jugcraft:advanced_circuit"}, 1),
    "lattice_tower": (["AXA", "SMS", "BSB"], {"A": "jugcraft:aluminum_cable", "X": "jugcraft:plastic_sheet", "S": "#c:ingots/steel",
                                             "M": "jugcraft:compact_monopole", "B": "#c:storage_blocks/steel"}, 1),
    "reconductored_lattice_tower": (["KAK", "ALA", "KAK"], {"K": "jugcraft:coke", "A": "jugcraft:aluminum_cable",
                                                           "L": "jugcraft:lattice_tower"}, 1),
    "monitored_lattice_tower": (["CRC", "PLP", "CRC"], {"C": "jugcraft:advanced_circuit", "R": "minecraft:redstone",
                                                       "P": "#c:plates/aluminum", "L": "jugcraft:reconductored_lattice_tower"}, 1),
    "uhv_tower": (["TTT", "BLB", "BXB"], {"T": "#c:plates/tungsten", "B": "#c:storage_blocks/steel",
                                         "L": "jugcraft:monitored_lattice_tower", "X": "jugcraft:processor"}, 1),
    "uhvdc_tower": (["XAX", "PUP", "BPB"], {"X": "jugcraft:processor", "A": "jugcraft:aluminum_cable", "P": "#c:plates/titanium",
                                           "U": "jugcraft:uhv_tower", "B": "#c:storage_blocks/steel"}, 1),
    "superconducting_pylon": (["TTT", "PUP", "BXB"], {"T": "jugcraft:superconducting_tape", "P": "#c:plates/titanium",
                                                     "U": "jugcraft:uhvdc_tower", "B": "#c:storage_blocks/steel",
                                                     "X": "jugcraft:processor"}, 1),
    "service_pedestal": (["IGI", "ICI", "IRI"], {"I": "#c:ingots/iron", "G": "minecraft:glass_pane", "C": "jugcraft:copper_cable",
                                                "R": "minecraft:redstone"}, 1),
    "pad_mount_transformer": (["PGP", "WSW", "PAP"], {"P": "#c:plates/steel", "G": "minecraft:green_dye", "W": "#c:wires/copper",
                                                     "S": "jugcraft:service_pedestal", "A": "jugcraft:aluminum_cable"}, 1),
    "unit_substation": (["BTB", "CMC", "FFF"], {"B": "#c:storage_blocks/steel", "T": "jugcraft:pad_mount_transformer",
                                               "C": "jugcraft:advanced_circuit", "M": "jugcraft:machine_casing",
                                               "F": "minecraft:iron_bars"}, 1),
    "converter_station": (["XCX", "TST", "BBB"], {"X": "jugcraft:processor", "C": "jugcraft:advanced_circuit",
                                                 "T": "#c:plates/titanium", "S": "jugcraft:unit_substation",
                                                 "B": "#c:storage_blocks/steel"}, 1),
}
# Features the mod's own ingredients need beyond "machines".
ITEM_FEATURES = {"plastic_sheet": ["crude_oil"], "processor": ["silicon"], "superconducting_tape": ["rare_earths", "silicon"]}
BASE_METALS = ("copper", "iron", "gold", "steel")


def blocks():
    """The anchor blocks, each with an item (the frame block has neither item nor name in the item list)."""
    return list(KINDS)


def recipe_features(result, feature_of):
    """Every feature switch a recipe depends on: its metals, the mod's ingredients and the tier it is built from."""
    found = set()
    for ref in CRAFTING[result][1].values():
        if ref.startswith("#c:") and ref.split("/")[0] in ("#c:plates", "#c:ingots", "#c:wires", "#c:storage_blocks", "#c:gears"):
            metal = ref.split("/")[-1]
            if metal not in BASE_METALS:
                found.add(feature_of(f"{metal}_ingot"))
        elif ref.startswith(f"{MOD}:"):
            name = ref.split(":")[1]
            found.update(ITEM_FEATURES.get(name, []))
            if name in CRAFTING:
                found |= recipe_features(name, feature_of)
    return found - {FEATURE}


# ---------------------------------------------------------------- the material sheet (client/LineRenderer.java)

SHEET = 512
CELL = 64
# name: slot on the 512x512 sheet, 64 px cells, 8 per row. LineRenderer.java has a region for each, in this order.
# Textures are drawn the right way up: the renderer puts the top of a cell at the top of every vertical face.
MATERIALS = ["wood", "wood_band", "crossarm", "galvanized", "lattice", "steel", "steel_dark", "foundation",
             "concrete", "glaze_teal", "glaze_grey", "glaze_brown", "conductor", "carbon_conductor", "cryo", "cryo_jacket",
             "red_light", "green_enclosure", "enclosure_door", "warning_label", "pad", "gravel", "chain_link", "barbed_wire",
             "cabinet", "cabinet_door", "meter_face", "vent", "transformer_tank", "radiator", "breaker", "pole_can",
             "conduit", "sensor", "valve_hall", "copper"]


def slot(name):
    index = MATERIALS.index(name)
    return index % 8, index // 8


def _rnd(seed):
    return random.Random(seed)


def _c(c):
    return tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def _shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def _blank(colour=(0, 0, 0, 0)):
    return Image.new("RGBA", (CELL, CELL), colour)


def _noise_fill(base, seed, amount=10, grain=None, scale=1):
    """A cell of noisy colour; grain 'v' or 'h' streaks it like wood or brushed metal."""
    r = _rnd(seed)
    img = _blank()
    streak = [r.randint(-amount, amount) for _ in range(CELL)]
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            d = r.randint(-amount, amount) // 2
            if grain == "v":
                d += streak[x // scale]
            elif grain == "h":
                d += streak[y // scale]
            px[x, y] = _shade(base, d)
    return img


def _wood(base, seed, grain="v"):
    img = _noise_fill(base, seed, 8, grain, scale=2)
    r = _rnd(seed + 1)
    d = ImageDraw.Draw(img)
    for _ in range(6):                                   # checks in weathered creosote wood
        if grain == "v":
            x = r.randint(3, CELL - 4)
            y0 = r.randint(0, CELL - 20)
            d.line([(x, y0), (x, y0 + r.randint(8, 18))], fill=_shade(base, -40))
        else:
            y = r.randint(3, CELL - 4)
            x0 = r.randint(0, CELL - 20)
            d.line([(x0, y), (x0 + r.randint(8, 18), y)], fill=_shade(base, -40))
    kx, ky = r.randint(12, 50), r.randint(12, 50)
    d.ellipse([kx - 4, ky - 2, kx + 4, ky + 2], fill=_shade(base, -30))
    d.ellipse([kx - 2, ky - 1, kx + 2, ky + 1], fill=_shade(base, -50))
    return img


def _metal(base, seed, spangle=False, angle=False, brushed=None):
    img = _noise_fill(base, seed, 5, brushed, scale=1)
    r = _rnd(seed + 7)
    d = ImageDraw.Draw(img)
    if spangle:                                           # hot-dip galvanizing: faint crystal patches
        for _ in range(14):
            x, y = r.randint(0, CELL), r.randint(0, CELL)
            sz = r.randint(6, 14)
            d.polygon([(x, y - sz), (x + sz, y), (x, y + sz), (x - sz, y)], fill=_shade(base, r.choice((-8, 8, 12))))
    if angle:                                             # angle-iron members: a lit flange and a shaded one
        px = img.load()
        for y in range(CELL):
            for x in range(CELL):
                if x < 6:
                    px[x, y] = _shade(px[x, y], 18)
                elif x > CELL - 7:
                    px[x, y] = _shade(px[x, y], -22)
        for y in range(8, CELL, 20):                      # bolt heads
            d.ellipse([CELL // 2 - 2, y - 2, CELL // 2 + 2, y + 2], fill=_shade(base, -45))
            d.point((CELL // 2 - 1, y - 1), fill=_shade(base, 20))
    return img


def _glaze(base, seed):
    """A glazed insulator: a smooth shade that wraps round the shed with a highlight stripe, like light on glass."""
    img = _blank()
    px = img.load()
    r = _rnd(seed)
    for x in range(CELL):
        u = x / CELL
        # dark at the edges of the wrap, a bright specular stripe a third of the way round
        light = -45 + 70 * math.sin(u * math.pi)
        if 0.22 < u < 0.34:
            light += 55 * math.sin((u - 0.22) / 0.12 * math.pi)
        for y in range(CELL):
            px[x, y] = _shade(base, int(light) + r.randint(-3, 3))
    return img


def _strands(base, seed, lay=6):
    """Stranded conductor: the diagonal lay of the outer strands."""
    img = _noise_fill(base, seed, 4)
    px = img.load()
    for y in range(CELL):
        for x in range(CELL):
            k = (x + y) % lay
            if k == 0:
                px[x, y] = _shade(px[x, y], -26)
            elif k == 1:
                px[x, y] = _shade(px[x, y], 14)
    return img


def _lens(base, seed, rim=(40, 40, 44)):
    img = _blank(_c(rim))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, CELL - 4, CELL - 4], fill=_c(_shade(base, -30)))
    d.rectangle([8, 8, CELL - 9, CELL - 9], fill=_c(base))
    d.rectangle([14, 12, CELL - 24, 20], fill=_c(_shade(base, 60)))
    return img


def _panel(base, seed, seams=(), bolts=False, brushed=None):
    img = _metal(base, seed, brushed=brushed)
    d = ImageDraw.Draw(img)
    for x in seams:
        d.line([(x, 0), (x, CELL)], fill=_shade(base, -30))
        d.line([(x + 1, 0), (x + 1, CELL)], fill=_shade(base, 14))
    if bolts:
        for x in range(8, CELL, 16):
            d.ellipse([x - 2, 4, x + 2, 8], fill=_shade(base, -40))
    return img


def _louvers(img, x0, y0, x1, y1, base, step=5):
    d = ImageDraw.Draw(img)
    for y in range(y0, y1, step):
        d.line([(x0, y), (x1, y)], fill=_shade(base, -55))
        d.line([(x0, y + 1), (x1, y + 1)], fill=_shade(base, -20))
        d.line([(x0, y + 2), (x1, y + 2)], fill=_shade(base, 18))


def _door(base, seed, label=False):
    """A hinged door in an enclosure: recessed outline, hinges on the left, a handle, louvers at the bottom."""
    img = _metal(base, seed, brushed="v")
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=_shade(base, -50))
    d.rectangle([1, 1, CELL - 2, CELL - 2], outline=_shade(base, -25))
    d.line([(2, CELL - 2), (CELL - 2, CELL - 2)], fill=_shade(base, 18))
    for y in (10, CELL - 14):                                     # hinges
        d.rectangle([2, y, 5, y + 7], fill=_shade(base, -35))
        d.line([(3, y + 1), (3, y + 6)], fill=_shade(base, 25))
    d.rectangle([CELL - 12, 26, CELL - 7, 38], fill=(40, 42, 46, 255))   # handle
    d.rectangle([CELL - 11, 27, CELL - 10, 37], fill=(120, 124, 130, 255))
    if label:
        d.rectangle([14, 8, 38, 20], fill=(226, 228, 230, 255), outline=(60, 62, 66, 255))
        for y in (11, 14, 17):
            d.line([(17, y), (35, y)], fill=(90, 92, 96, 255))
    _louvers(img, 12, CELL - 26, CELL - 14, CELL - 8, base)
    return img


def _warning(seed):
    """A yellow 'danger: high voltage' plate: black border, triangle and lightning bolt, point up."""
    img = _blank((226, 184, 26, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=(30, 28, 20, 255), width=3)
    d.polygon([(32, 7), (58, 50), (6, 50)], outline=(20, 20, 20, 255), width=3)
    d.polygon([(35, 14), (24, 33), (32, 33), (28, 46), (41, 26), (33, 26)], fill=(20, 20, 20, 255))
    d.rectangle([10, 54, 54, 59], fill=(30, 28, 20, 255))
    return img


def _meter(seed):
    """A service pedestal's face: brushed steel with a round glass meter, dials and a spinning disc."""
    img = _metal((164, 168, 174), seed, brushed="v")
    d = ImageDraw.Draw(img)
    d.ellipse([8, 4, 56, 52], fill=(92, 96, 102, 255))                         # socket ring
    d.ellipse([11, 7, 53, 49], fill=(214, 224, 232, 255))                       # glass
    d.ellipse([15, 11, 49, 45], fill=(244, 244, 240, 255))                      # dial face
    for x in (20, 27, 34, 41):                                                  # register dials
        d.ellipse([x - 2, 19, x + 2, 23], outline=(60, 60, 64, 255))
        d.line([(x, 21), (x + 1, 19)], fill=(60, 60, 64, 255))
    d.rectangle([22, 27, 42, 32], fill=(28, 28, 30, 255))                       # digits
    for x in range(24, 42, 4):
        d.line([(x, 28), (x, 31)], fill=(210, 210, 210, 255))
    d.line([(17, 38), (47, 38)], fill=(90, 90, 94, 255))                        # the disc's edge
    d.line([(18, 39), (46, 39)], fill=(160, 160, 164, 255))
    d.ellipse([20, 9, 26, 13], fill=(255, 255, 255, 120))                       # glass glint
    d.rectangle([20, 56, 44, 61], fill=(226, 228, 230, 255), outline=(60, 62, 66, 255))   # name plate
    return img


def _vent(seed):
    img = _metal((170, 174, 180), seed, brushed="v")
    _louvers(img, 8, 8, CELL - 9, CELL - 8, (170, 174, 180), step=6)
    return img


def _chain_link(seed, colour=(172, 178, 182, 255)):
    """Chain-link mesh: a few bold diamonds per block, so it stays calm at a distance (entity textures have no mipmaps)."""
    img = _blank()
    d = ImageDraw.Draw(img)
    for k in range(-CELL, CELL * 2, 32):
        d.line([(k, 0), (k + CELL, CELL)], fill=colour, width=4)
        d.line([(k, CELL), (k + CELL, 0)], fill=_shade(colour, -18), width=4)
    d.line([(0, 2), (CELL, 2)], fill=_shade(colour, -30), width=4)           # top rail
    return img


def _barbed(seed):
    img = _blank()
    d = ImageDraw.Draw(img)
    for y in (14, 34, 54):
        d.line([(0, y), (CELL, y)], fill=(124, 128, 132, 255), width=3)
        for x in range(8, CELL, 24):
            d.line([(x - 5, y - 5), (x + 5, y + 5)], fill=(150, 154, 158, 255), width=3)
            d.line([(x - 5, y + 5), (x + 5, y - 5)], fill=(150, 154, 158, 255), width=3)
    return img


def _gravel(seed):
    r = _rnd(seed)
    img = _blank((128, 126, 118, 255))
    d = ImageDraw.Draw(img)
    for _ in range(420):
        x, y = r.randint(0, CELL - 1), r.randint(0, CELL - 1)
        c = r.choice([(150, 148, 140), (112, 110, 104), (168, 164, 154), (96, 96, 92), (140, 134, 120)])
        sz = r.randint(1, 3)
        d.ellipse([x, y, x + sz, y + sz - 1], fill=_c(c))
        d.point((x, y), fill=_shade(_c(c), 25))
    return img


def _radiator(seed):
    """Transformer radiator fins seen from the side: vertical fins with a header pipe top and bottom."""
    base = (96, 100, 108)
    img = _metal(base, seed)
    d = ImageDraw.Draw(img)
    for x in range(0, CELL, 6):
        d.rectangle([x, 6, x + 2, CELL - 7], fill=_shade(base, 22))
        d.line([(x + 3, 6), (x + 3, CELL - 7)], fill=_shade(base, -40))
    for y in (2, CELL - 5):
        d.rectangle([0, y, CELL, y + 3], fill=_shade(base, -15))
        d.line([(0, y), (CELL, y)], fill=_shade(base, 20))
    return img


def _round(base, seed, bands=()):
    """Something round drawn on a box: shaded darker towards the sides of each face, so it reads as a cylinder."""
    img = _blank()
    px = img.load()
    r = _rnd(seed)
    for x in range(CELL):
        light = -40 + 60 * math.sin((x + 0.5) / CELL * math.pi)
        for y in range(CELL):
            d = int(light) + r.randint(-4, 4)
            if any(b0 <= y < b1 for b0, b1 in bands):
                d -= 28
            px[x, y] = _shade(base, d)
    return img


def _cryo(seed):
    img = _blank((110, 210, 250, 255))
    d = ImageDraw.Draw(img)
    r = _rnd(seed)
    for _ in range(120):                                  # frost
        x, y = r.randint(0, CELL - 1), r.randint(0, CELL - 1)
        d.point((x, y), fill=(220, 245, 255, 255))
    d.rectangle([0, 28, CELL, 36], fill=(170, 235, 255, 255))
    return img


def draw_sheet():
    art = {
        "wood": _wood((104, 78, 54), 1), "wood_band": _wood((84, 62, 44), 2),
        "crossarm": _wood((118, 92, 64), 3, grain="h"), "galvanized": _metal((176, 182, 188), 4, spangle=True),
        "lattice": _metal((170, 176, 182), 5, spangle=True, angle=True), "steel": _metal((140, 146, 154), 6),
        "steel_dark": _metal((74, 78, 86), 7), "foundation": _noise_fill((150, 150, 144), 8, 9),
        "concrete": _noise_fill((178, 178, 172), 9, 7), "glaze_teal": _glaze((96, 150, 158), 10),
        "glaze_grey": _glaze((128, 132, 140), 11), "glaze_brown": _glaze((132, 78, 50), 12),
        "conductor": _strands((124, 128, 132), 13), "carbon_conductor": _strands((66, 68, 72), 14),
        "cryo": _cryo(15), "cryo_jacket": _round((200, 206, 212), 16, bands=((0, 4), (60, 64))),
        "red_light": _lens((240, 60, 40), 17), "green_enclosure": _panel((78, 112, 82), 18, seams=(31,), brushed="v"),
        "enclosure_door": _door((78, 112, 82), 19, label=True), "warning_label": _warning(20),
        "pad": _noise_fill((170, 170, 164), 21, 6), "gravel": _gravel(22), "chain_link": _chain_link(23),
        "barbed_wire": _barbed(24), "cabinet": _panel((164, 168, 174), 25, seams=(), brushed="v"),
        "cabinet_door": _door((164, 168, 174), 26), "meter_face": _meter(27), "vent": _vent(28),
        "transformer_tank": _panel((70, 74, 82), 29, seams=(21, 42), bolts=True), "radiator": _radiator(30),
        "breaker": _panel((200, 204, 210), 31, seams=(31,), brushed="v"), "pole_can": _round((130, 136, 140), 32, bands=((8, 12), (52, 56))),
        "conduit": _round((62, 64, 68), 33), "sensor": _lens((255, 180, 60), 34, rim=(60, 50, 30)),
        "valve_hall": _panel((222, 224, 226), 35, seams=(15, 31, 47), brushed="v"), "copper": _strands((190, 112, 62), 36, lay=4),
    }
    sheet = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    for name in MATERIALS:
        col, row = slot(name)
        sheet.paste(art[name], (col * CELL, row * CELL))
    return sheet


# ---------------------------------------------------------------- models, blockstates, names, loot, recipes

PARTICLE = {"wood": "minecraft:block/stripped_spruce_log", "box": "minecraft:block/iron_block"}


def particle(kind):
    if kind == "spun_concrete_pole":
        return "minecraft:block/smooth_stone"
    return PARTICLE.get(KINDS[kind][1], "minecraft:block/iron_block")


def write_assets(write, rid, assets, lang):
    for kind, (display, family, *_rest) in KINDS.items():
        lang[f"block.{MOD}.{kind}"] = display
        lang[f"tooltip.{MOD}.power_line.{kind}"] = TOOLTIPS[kind]
        # The structure is drawn by client/LineRenderer.java; the block model only gives break particles.
        write(assets / "models" / "block" / f"{kind}.json", {"textures": {"particle": particle(kind)}})
        write(assets / "blockstates" / f"{kind}.json", {"variants": {"": {"model": rid(f"block/{kind}")}}})
        # The item shows the structure itself, in miniature (tools/structure_items.py).
        structure_items.line_item(write, rid, assets, kind, particle(kind))
    write(assets / "models" / "block" / f"{FRAME}.json", {"textures": {"particle": "minecraft:block/iron_block"}})
    write(assets / "blockstates" / f"{FRAME}.json", {"variants": {"": {"model": rid(f"block/{FRAME}")}}})
    lang.update(LANG)


def write_loot(write, rid, out, self_drop):
    for kind in KINDS:
        write(out / f"{kind}.json", self_drop(kind))
    # Breaking a frame block breaks the structure, and its anchor drops the item.
    write(out / f"{FRAME}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{FRAME}")})


def write_data(write, data):
    """The electrocution damage type (exhaustion like a fire tick, scaled by difficulty like most damage)."""
    write(data / MOD / "damage_type" / "electrocution.json",
          {"message_id": "jugcraft.electrocution", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
