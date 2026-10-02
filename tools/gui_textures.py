"""Machine screens (batch 22, docs/features/machine-screens.md): three themed backgrounds and what each machine is for.

Every machine screen is a 176x166 machine bay with the player inventory, and a 92-pixel control terminal joined on its
right: a CRT that says what the machine does and how it is doing, the side and redstone controls, and a keyboard.
The look follows the machine's model (docs/ART_DIRECTION.md):

- "diesel": the steampunk and dieselpunk machines. Riveted dark iron with brass, an amber-on-brown terminal and an
  orange keyboard, after the owner's first reference (an orange terminal with a pixel keyboard).
- "electric": the electric-look power gear. Graphite panels with mint-green glow, a green-text monitor, after the
  dark green-text monitor reference.
- "lab": chemistry and electronics. Slate-teal panels with cyan accents, after the slate-teal inventory and teal
  mech-GUI references.

All drawn here, original. client/MachineScreen reads the themes from assets/jugcraft/gui/machine_themes.json and draws
slots, gauges and text in the theme's colours; the taglines are lang keys container.jugcraft.tagline.<machine>.
"""
import random

from PIL import Image

WIDTH, HEIGHT = 268, 166
BAY = 176

THEMES = {
    "diesel": {
        "panel": [(44, 36, 30), (58, 47, 39), (72, 59, 48), (98, 80, 64), (128, 106, 84)],
        "trim": [(120, 86, 34), (170, 126, 52), (214, 170, 86)],
        "screen": [(26, 15, 6), (36, 21, 8), (48, 29, 11)],
        "keys": [(150, 70, 22), (204, 102, 36), (236, 150, 74), (60, 30, 12)],
        "slot": [(28, 22, 18), (40, 32, 26), (108, 88, 70)],
    },
    "electric": {
        "panel": [(28, 31, 35), (40, 44, 49), (54, 59, 65), (78, 84, 92), (110, 117, 126)],
        "trim": [(34, 138, 84), (62, 204, 124), (128, 244, 172)],
        "screen": [(4, 12, 9), (7, 20, 14), (12, 30, 21)],
        "keys": [(50, 55, 62), (72, 78, 87), (110, 118, 128), (20, 22, 26)],
        "slot": [(18, 20, 23), (28, 31, 35), (86, 92, 100)],
    },
    "lab": {
        "panel": [(26, 40, 46), (36, 54, 61), (48, 70, 78), (72, 100, 108), (110, 146, 152)],
        "trim": [(28, 128, 148), (56, 200, 218), (144, 242, 250)],
        "screen": [(5, 20, 24), (8, 30, 35), (12, 42, 48)],
        "keys": [(40, 96, 108), (58, 128, 142), (110, 186, 196), (16, 40, 46)],
        "slot": [(16, 26, 30), (26, 38, 44), (90, 124, 132)],
    },
}
# Text colours the screen uses (ARGB), per theme: bright, dim, warning, the energy and progress fills.
COLORS = {
    "diesel": {"text": 0xFFFFB040, "dim": 0xFFB07028, "warn": 0xFFFF6040, "energy": 0xFFF0A020,
               "progress": 0xFFFFC860, "label": 0xFFE8C890},
    "electric": {"text": 0xFF6CF08C, "dim": 0xFF3A9A58, "warn": 0xFFFF7060, "energy": 0xFF58E890,
                 "progress": 0xFFA8FFC8, "label": 0xFFC8D4DC},
    "lab": {"text": 0xFF7FE6F0, "dim": 0xFF3C9AA8, "warn": 0xFFFF8070, "energy": 0xFF60D8F0,
            "progress": 0xFFB8F6FF, "label": 0xFFC6E2E6},
}

ELECTRIC = {"battery_box", "capacitor_bank", "solar_panel", "fuel_cell", "lithium_battery_bank", "advanced_solar_panel",
            "advanced_engine", "flow_battery"}
LAB = {"crystal_grower", "lithography_station", "chemical_reactor", "electrolytic_cell", "air_separation_unit",
       "synthesis_converter", "polymerization_reactor", "chemical_mixer", "flowback_treatment_unit",
       "circuit_assembler"}

# What each machine is for, in a line the terminal shows.
TAGLINES = {
    "coal_generator": "Burns coal and other fuel into power.",
    "battery_box": "Stores power. Gives it out of its front.",
    "electric_furnace": "Smelts anything a furnace can, on power.",
    "crusher": "Crushes one ore into two raw ores.",
    "arc_furnace_controller": "Melts what a furnace cannot, once its casing is built.",
    "solar_panel": "Turns sunlight into power.",
    "steam_generator": "Boils water over a fire for power.",
    "alloy_smelter": "Melts two ingredients into an alloy.",
    "metal_press": "Presses ingots into plates.",
    "wire_drawer": "Draws ingots into wire.",
    "circuit_assembler": "Assembles circuits from parts.",
    "pulverizer": "Grinds ore into two dusts, with extras.",
    "ore_washer": "Washes ore into three. Needs water.",
    "sieve": "Sifts gravel and soul sand for extras.",
    "sawmill": "Saws logs into planks and sawdust.",
    "coke_oven": "Bakes coal into coke. No power needed.",
    "steel_foundry": "Refines iron and coke into steel.",
    "capacitor_bank": "Stores power. Gives it out of its front.",
    "steel_tank": "Holds 128 buckets of one fluid.",
    "ore_drill": "Mines the ore under it, layer by layer.",
    "deposit_drill": "Works surface deposits into ore.",
    "cobblestone_generator": "Makes cobblestone from water and lava.",
    "tree_farm": "Grows saplings into logs.",
    "water_wheel": "Turns flowing water into power.",
    "auto_crafter": "Crafts the pattern in its grid.",
    "large_steam_engine": "Boils water into rotation for shafts.",
    "geothermal_generator": "Turns lava into power.",
    "pumpjack": "Pumps crude oil from a reservoir.",
    "oil_sand_extractor": "Washes crude oil out of oil sand.",
    "distillation_tower": "Splits crude oil into its fractions.",
    "catalytic_cracker": "Cracks heavy oil into lighter fuels.",
    "vacuum_distillation_unit": "Boils heavy oil into lubricant and binder.",
    "catalytic_reformer": "Reforms naphtha into gasoline.",
    "chemical_mixer": "Stirs powders into water.",
    "fracking_rig": "Frees oil and gas from shale.",
    "flowback_treatment_unit": "Cleans the water a fracking rig returns.",
    "diesel_generator": "Burns diesel into power.",
    "gas_turbine": "Burns gas or gasoline into power.",
    "polymerization_reactor": "Turns gases into plastic and rubber.",
    "electrolytic_cell": "Splits brine; smelts aluminum.",
    "chemical_reactor": "Reacts items with acids and lye.",
    "air_separation_unit": "Splits air into nitrogen, oxygen, argon.",
    "synthesis_converter": "Makes ammonia and nitric acid.",
    "fuel_cell": "Turns hydrogen into power.",
    "lithium_battery_bank": "Stores 32M JE. Gives it out of its front.",
    "flow_battery": "Stores 1,000 JE per mB of electrolyte.",
    "crystal_grower": "Grows doped silicon boules.",
    "lithography_station": "Etches wafers into microchips.",
    "gas_holder": "Holds 1,024 buckets of one gas.",
    "advanced_solar_panel": "Turns sunlight into 64 JE/t.",
    "advanced_engine": "Burns fuel into rotation. Turbo slot.",
    "crop_harvester": "Harvests and replants ripe crops.",
    "diesel_engine": "Burns diesel into rotation for shafts.",
    "wind_turbine": "Turns wind into power. Higher is better.",
}


def theme_of(machine):
    return "electric" if machine in ELECTRIC else "lab" if machine in LAB else "diesel"


def _bevel(img, x0, y0, x1, y1, light, dark, face=None):
    """A raised (light top-left) box from (x0, y0) to (x1, y1) inclusive, optionally filled."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if x == x0 or y == y0:
                c = light
            elif x == x1 or y == y1:
                c = dark
            elif face is not None:
                c = face
            else:
                continue
            img.putpixel((x, y), c + (255,))


def _recess(img, x0, y0, x1, y1, light, dark, face):
    """A sunken box: dark top-left edge, light bottom-right edge."""
    _bevel(img, x0, y0, x1, y1, dark, light, face)


def background(name):
    t = THEMES[name]
    p, trim, scr, keys, slot = t["panel"], t["trim"], t["screen"], t["keys"], t["slot"]
    rng = random.Random(2200 + list(THEMES).index(name))
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    # Panels: brushed noise over the face colour, a dark outline and a bevel.
    for y in range(HEIGHT):
        for x in range(WIDTH):
            c = p[2] if rng.random() < 0.82 else p[1] if rng.random() < 0.5 else p[3]
            img.putpixel((x, y), c + (255,))
    _bevel(img, 0, 0, BAY - 1, HEIGHT - 1, p[4], p[0])
    _bevel(img, BAY, 0, WIDTH - 1, HEIGHT - 1, p[4], p[0])
    for y in range(HEIGHT):  # The seam between bay and terminal.
        img.putpixel((BAY - 1, y), p[0] + (255,))
        img.putpixel((BAY, y), p[0] + (255,))
    # Header band behind the title, with a trim line under it.
    for y in range(2, 15):
        for x in range(2, BAY - 2):
            img.putpixel((x, y), p[1] + (255,))
    for x in range(2, BAY - 2):
        img.putpixel((x, 15), trim[1] + (255,))
    # The machine bay: a recess the slots and gauges sit in.
    _recess(img, 4, 17, BAY - 5, 71, p[4], p[0], p[1])
    # Player inventory slots.
    for row in range(3):
        for col in range(9):
            _recess(img, 7 + col * 18, 83 + row * 18, 24 + col * 18, 100 + row * 18, slot[2], slot[0], slot[1])
    for col in range(9):
        _recess(img, 7 + col * 18, 141, 24 + col * 18, 158, slot[2], slot[0], slot[1])
    # Screws in the corners of each panel.
    for sx, sy in ((3, 3), (BAY - 5, 3), (3, HEIGHT - 5), (BAY - 5, HEIGHT - 5), (BAY + 3, 3), (WIDTH - 5, 3),
                   (BAY + 3, HEIGHT - 5), (WIDTH - 5, HEIGHT - 5)):
        img.putpixel((sx, sy), trim[2] + (255,))
        img.putpixel((sx + 1, sy + 1), trim[0] + (255,))
    # The terminal's CRT: a bezel, a dark glass with scanlines and a soft glow towards the middle.
    sx0, sy0, sx1, sy1 = BAY + 6, 6, WIDTH - 7, 96
    _bevel(img, sx0 - 2, sy0 - 2, sx1 + 2, sy1 + 2, p[0], p[4], p[0])
    for y in range(sy0, sy1 + 1):
        for x in range(sx0, sx1 + 1):
            cx, cy = (x - (sx0 + sx1) / 2) / (sx1 - sx0), (y - (sy0 + sy1) / 2) / (sy1 - sy0)
            glow = max(0.0, 1 - (cx * cx + cy * cy) * 2.4) ** 1.5
            base = tuple(round(scr[0][i] + (scr[2][i] - scr[0][i]) * glow) for i in range(3))
            if y % 2 == 1:
                base = tuple(max(0, v - 4) for v in base)
            img.putpixel((x, y), base + (255,))
    for (x, y) in ((sx0, sy0), (sx1, sy0), (sx0, sy1), (sx1, sy1)):  # Rounded glass corners.
        img.putpixel((x, y), p[0] + (255,))
    # A power lamp and a label plate under the screen.
    for y in range(99, 102):
        for x in range(WIDTH - 12, WIDTH - 8):
            img.putpixel((x, y), trim[2 if y == 99 else 1] + (255,))
    # The control area (side buttons are drawn over it) and the keyboard.
    _recess(img, BAY + 4, 104, WIDTH - 5, 139, p[4], p[0], p[1])
    kx0, ky0 = BAY + 5, 143
    _bevel(img, kx0 - 1, ky0 - 1, WIDTH - 5, HEIGHT - 5, p[0], p[4], keys[3])
    rows = [(0, 13), (2, 12), (4, 11)]
    for r, (offset, count) in enumerate(rows):
        for k in range(count):
            x = kx0 + 1 + offset + k * 6
            y = ky0 + 1 + r * 6
            if x + 5 > WIDTH - 6:
                break
            for yy in range(5):
                for xx in range(5):
                    c = keys[1]
                    if yy == 0 or xx == 0:
                        c = keys[2]
                    elif yy == 4 or xx == 4:
                        c = keys[0]
                    img.putpixel((x + xx, y + yy), c + (255,))
            # A legend on each key.
            img.putpixel((x + 2, y + 2), (keys[3] if name == "diesel" else trim[2]) + (255,))
    return img


def draw_all(save_gui):
    """save_gui(img, name) writes textures/gui/<name>.png."""
    for name in THEMES:
        save_gui(background(name), f"machine_{name}")


def write_all(write, assets, lang, machines):
    """The theme of every machine's screen, and its tagline."""
    slot_colors = {name: {"slot_dark": THEMES[name]["slot"][0], "slot_face": THEMES[name]["slot"][1],
                          "slot_light": THEMES[name]["slot"][2]} for name in THEMES}
    themes = {}
    for name, colors in COLORS.items():
        theme = {key: f"{value:08X}" for key, value in colors.items()}
        theme.update({key: "FF%02X%02X%02X" % rgb for key, rgb in slot_colors[name].items()})
        themes[name] = theme
    write(assets / "gui" / "machine_themes.json",
          {"themes": themes, "machines": {machine: theme_of(machine) for machine in machines}})
    for machine in machines:
        lang[f"container.jugcraft.tagline.{machine}"] = TAGLINES[machine]
    words = {
        "running": "RUNNING", "idle": "IDLE", "no_power": "NO POWER", "generating": "GENERATING",
        "charging": "CHARGING", "storing": "STORING", "holding": "HOLDING", "stopped": "STOPPED",
        "discharging": "DISCHARGING", "sides": "SIDES", "power": "POWER", "rate": "RATE", "fuel": "FUEL",
    }
    for key, text in words.items():
        lang[f"container.jugcraft.terminal.{key}"] = text
