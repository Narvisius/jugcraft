"""Machine screens (docs/features/machine-screens.md): five looks by tech level over one simple layout.

Every machine screen is 268x166. Its top is the machine's body: a title strip with a status lamp, the machine pane
(energy, slots, tanks and progress, where the menu puts them) and, on the right, the status pane (what it is doing,
what to do about it, progress, time left and power). Under the body sit the player's inventory on its own tray and,
on the right, the help box: the icon buttons along its top (on machines that have them), then what the machine does,
the faces' key while the sides are shown, or a form's tool sockets and upgrade slots.

The look follows the machine's tech level (the owner's references of 14 October 2026, used for mood, colour and layout
only; everything here is drawn fresh):

- "workshop": the first machines. A warm brass casing with soft blotches and an orange-on-brown screen.
- "industrial": steel and oil, and the industrial forms. A grey monitor bezel, green phosphor panes, green buttons.
- "electric": the power gear. A chunky blue device with cyan glass, side vents and a blue tray.
- "lab": chemistry. A dark blueprint grid in a pale frame, thin blue line art and coloured toggle tabs.
- "advanced": electronics and nuclear fuel. Translucent dark teal with cyan hologram frames and cut corners.

client/MachineScreenThemes reads the looks from assets/jugcraft/gui/machine_themes.json; client/MachineSkin draws the
parts that move or vary (slots, gauges, buttons, icons) in the look's colours; the taglines and hints are lang keys.
"""
import random

from PIL import Image, ImageDraw

WIDTH, HEIGHT = 268, 166
BAY = 176
# The parts of the layout (inclusive pixel boxes), shared with client/MachineSkin.
BODY = (0, 0, WIDTH - 1, 77)
MACHINE_PANE = (3, 11, 174, 76)
STATUS_PANE = (179, 11, 264, 76)
TRAY = (0, 79, BAY - 1, HEIGHT - 1)
HELP_BOX = (178, 79, WIDTH - 2, HEIGHT - 2)

SKINS = {
    "workshop": {
        "casing": [(78, 46, 20), (138, 88, 40), (176, 118, 58), (206, 150, 84), (232, 186, 112)],
        "outline": (40, 22, 8),
        "screen": (26, 17, 10), "grid": (42, 26, 13), "frame": (226, 120, 36), "band": (48, 30, 14),
        "tray": [(120, 78, 36), (186, 132, 70), (220, 168, 102)],
        "slot": [(30, 18, 8), (60, 36, 18), (196, 140, 76)],
        "help": (60, 36, 18),
    },
    "industrial": {
        "casing": [(46, 48, 50), (88, 91, 94), (124, 127, 130), (160, 163, 166), (196, 199, 201)],
        "outline": (24, 25, 26),
        "screen": (16, 18, 19), "grid": (20, 23, 22), "frame": (112, 116, 119), "band": None,
        "tray": [(60, 63, 66), (104, 107, 110), (150, 153, 156)],
        "slot": [(10, 11, 12), (26, 28, 29), (112, 116, 119)],
        "help": (16, 18, 19),
    },
    "electric": {
        "casing": [(10, 28, 74), (22, 74, 170), (34, 104, 214), (62, 146, 246), (132, 200, 255)],
        "outline": (6, 14, 40),
        "screen": (12, 22, 52), "grid": (20, 36, 82), "frame": (70, 178, 255), "band": None,
        "tray": [(14, 44, 120), (28, 86, 200), (62, 140, 240)],
        "slot": [(10, 30, 90), (18, 56, 150), (60, 130, 236)],
        "help": (18, 56, 150),
    },
    "lab": {
        "casing": [(40, 42, 46), (92, 96, 100), (150, 154, 158), (178, 182, 186), (214, 217, 220)],
        "outline": (20, 21, 23),
        "screen": (10, 13, 20), "grid": (19, 26, 39), "frame": (60, 124, 220), "band": None,
        "tray": [(10, 13, 20), (10, 13, 20), (60, 124, 220)],
        "slot": [(60, 124, 220), (12, 16, 26), (60, 124, 220)],
        "help": (10, 13, 20),
    },
    "advanced": {
        "casing": [(6, 14, 16), (10, 22, 25), (14, 30, 34), (20, 44, 48), (40, 90, 94)],
        "outline": (4, 10, 12),
        "screen": (8, 20, 22), "grid": (13, 31, 33), "frame": (56, 214, 214), "band": None,
        "tray": [(8, 18, 20), (8, 18, 20), (32, 150, 152)],
        "slot": [(42, 168, 168), (10, 26, 28), (42, 168, 168)],
        "help": (8, 20, 22),
    },
    # The drone family's screens (depot terminal, drone tower): the advanced look's shapes in the tower's graphite and
    # dull red. No machine uses it.
    "drone": {
        "casing": [(10, 8, 9), (20, 16, 18), (28, 22, 24), (40, 30, 32), (96, 44, 40)],
        "outline": (6, 4, 4),
        "screen": (30, 26, 28), "grid": (40, 32, 33), "frame": (224, 80, 64), "band": None,
        "tray": [(20, 16, 18), (20, 16, 18), (150, 70, 60)],
        "slot": [(150, 70, 60), (20, 16, 18), (150, 70, 60)],
        "help": (30, 26, 28),
    },
}
# Colours the screen draws with (ARGB), per look: screen text bright and dim, warnings, all's-well, the energy and
# progress fills, the title on the body, the help box's text, and the buttons' face, edge, highlight and icon.
COLORS = {
    "workshop": {"text": 0xFFFF9E34, "dim": 0xFFB0621F, "warn": 0xFFFF5A3C, "ok": 0xFF7CE65A, "energy": 0xFFFF8C1E,
                 "progress": 0xFFFFBE5A, "label": 0xFF2A1606, "help": 0xFFFFE6C0, "button_face": 0xFF1E130A,
                 "button_edge": 0xFFE27824, "button_light": 0xFFFFB060, "button_icon": 0xFFE27824},
    "industrial": {"text": 0xFF60E66E, "dim": 0xFF308C3E, "warn": 0xFFFF6046, "ok": 0xFF60E66E, "energy": 0xFF60E66E,
                   "progress": 0xFF96FFA0, "label": 0xFF18191A, "help": 0xFF60E66E, "button_face": 0xFF24A84C,
                   "button_edge": 0xFF125C28, "button_light": 0xFF78F096, "button_icon": 0xFFE6FFEC},
    "electric": {"text": 0xFFC8ECFF, "dim": 0xFF6496D6, "warn": 0xFFFF5C84, "ok": 0xFF46E65A, "energy": 0xFF5AD2FF,
                 "progress": 0xFFAAE8FF, "label": 0xFFE6F4FF, "help": 0xFFDCECFF, "button_face": 0xFF343E4C,
                 "button_edge": 0xFF181E26, "button_light": 0xFF8C9AAA, "button_icon": 0xFFC8D2DE},
    "lab": {"text": 0xFF78B0FF, "dim": 0xFF3868B8, "warn": 0xFFFF6050, "ok": 0xFF5ADC78, "energy": 0xFF5A96FF,
            "progress": 0xFF96C4FF, "label": 0xFF78B0FF, "help": 0xFF78B0FF, "button_face": 0xFF181E2C,
            "button_edge": 0xFF3C7CDC, "button_light": 0xFF6AA8FF, "button_icon": 0xFFF0F0F0},
    "advanced": {"text": 0xFF80F4F0, "dim": 0xFF349696, "warn": 0xFFFF6E60, "ok": 0xFF80F4F0, "energy": 0xFF5AECE8,
                 "progress": 0xFFAAFFFA, "label": 0xFF80F4F0, "help": 0xFF80F4F0, "button_face": 0xFF0A1A1C,
                 "button_edge": 0xFF38D6D6, "button_light": 0xFF80F4F0, "button_icon": 0xFF80F4F0},
    "drone": {"text": 0xFFECE6E6, "dim": 0xFFA8564A, "warn": 0xFFFF6A5A, "ok": 0xFF78DC8C, "energy": 0xFFE05040,
              "progress": 0xFFFF8A70, "label": 0xFFE05040, "help": 0xFFC8B4B0, "button_face": 0xFF1E1416,
              "button_edge": 0xFFE05040, "button_light": 0xFFFF8A70, "button_icon": 0xFFECE6E6},
}
# How slots and buttons are drawn: sunken bevels on the casing looks, thin outlines on the screen-like ones.
OUTLINED = {"lab", "advanced", "drone"}
# The five looks machines use; the others are for screens only, drawn in the shapes of the look named here.
MACHINE_LOOKS = ("workshop", "industrial", "electric", "lab", "advanced")
SHAPES = {"drone": "advanced"}
# The other machine screens (docs/features/machine-screens.md) and their looks, by tech level.
SCREENS = {
    "prospector": "workshop",
    "rocket_pad": "industrial",
    "storage_tank": "industrial",
    "logic_controller": "advanced",
    "reactor_console": "advanced",
    "remote_shutdown": "advanced",
    "drone_terminal": "drone",
    "drone_tower": "drone",
    # The benches: the workshop look.
    "cooking_pot": "workshop",
    "lampwright_bench": "workshop",
    "farm_stand": "workshop",
    "cave_survey": "workshop",
    "carving": "workshop",
}

WORKSHOP = {"coal_generator", "steam_generator", "water_wheel", "wind_turbine", "crusher", "electric_furnace",
            "alloy_smelter", "metal_press", "wire_drawer", "pulverizer", "ore_washer", "sieve", "sawmill",
            "cobblestone_generator", "tree_farm", "crop_harvester", "auto_crafter", "large_steam_engine"}
ELECTRIC = {"lead_acid_battery_bank", "flow_tank_module", "flow_stack_module", "battery_box", "capacitor_bank", "solar_panel", "advanced_solar_panel", "fuel_cell", "lithium_battery_bank",
            "flow_battery", "advanced_engine"}
LAB = {"chemical_reactor", "electrolytic_cell", "air_separation_unit", "synthesis_converter", "polymerization_reactor"}
ADVANCED = {"circuit_assembler", "lithography_station", "gas_centrifuge_cascade", "fuel_fabricator"}
# Every other machine, and every industrial form, is industrial: steel and oil.
FORM_LOOK = "industrial"
# What each industrial form is for (tools/industrial_forms.py FORMS), shown in its help box.
FORM_TAGLINES = {
    "bulk_electrolytic_separator": "Splits fluids in four lanes, with efficient bulk power use.",
    "bulk_chemical_infuser": "Combines gases and processes synthetic fuels in four lanes.",
    "bulk_chemical_oxidizer": "Oxidizes solid feeds in four parallel batches.",
    "bulk_fermenter": "Ferments crops or mash in four lanes and collects exhaust gas.",
    "large_bio_generator": "Generates power from bioethanol or biogas at factory scale.",
    "large_gas_burning_generator": "Generates high-output power from hydrogen or methane.",
    "combined_cycle_power_plant": "Recovers exhaust heat for high-output gas power.",
    "precision_finisher": "Finishes ceramics, shafts and lenses with reusable heads and coolant.",
    "wet_processing_bench": "Cleans, coats, develops and etches wafers; collect the spent solution.",
    "electrolytic_separator": "Splits water or brine into gases, and lye from brine.",
    "gas_burning_generator": "Burns hydrogen or methane into power.",
    "chemical_infuser": "Joins gases into hydrogen chloride, methane or sulfur trioxide.",
    "chemical_oxidizer": "Burns sulfur or coal with oxygen into gas.",
    "industrial_fermenter": "Ferments crops with water into bioethanol.",
    "bulk_chemical_reactor": "Big batches of acid: absorbing, leaching and neutralizing.",
    "anaerobic_digester": "Digests organic waste into biogas and bone meal.",
    "coal_gasifier": "Turns coal and steam into raw syngas.",
    "gas_cleanup_rack": "Splits raw syngas into carbon monoxide and hydrogen.",
    "bio_generator": "Burns bioethanol or biogas into power.",
    # The forms of the factory chains (#358), from their tanks and their records.
    "gas_cleaning_rack": "Washes dirty gas with water into carbon monoxide and hydrogen.",
    "biogas_cleaner": "Washes raw biogas clean with water.",
    "digestate_separator": "Presses digestate, recovering its water.",
    "mashing_station": "Cooks crops in hot water into a mash.",
    "chemical_distillation_station": "Distils ethanol out of fermented broth.",
    "bauxite_digester": "Dissolves bauxite in hot lye into aluminate slurry.",
    "aluminate_filter_press": "Filters slurries, recovering lye and water.",
    "alumina_calciner": "Bakes alumina, dries chlorides and sinters PTFE.",
    "aluminum_reduction_cell": "Smelts alumina into aluminum with power.",
    "coastal_seawater_intake": "Pumps seawater from the coast.",
    "brine_concentrator": "Boils water out of seawater and dilute acid.",
    "mineral_conditioner": "Prepares minerals with a reagent for the next step.",
    "chloride_dryer": "Dries chloride solutions.",
    "magnesium_electrolysis_cell": "Splits magnesium chloride into magnesium and chlorine.",
    "titanium_chlorinator": "Chlorinates titanium with coke into crude chloride.",
    "titanium_reduction_retort": "Reduces titanium chloride with magnesium into sponge.",
    "polymer_molding_press": "Molds polymer into parts with reusable molds.",
    "monomer_preparation_unit": "Prepares monomer for the kettle.",
    "polymerization_kettle": "Polymerizes monomer into polymer.",
    "polymer_extruder": "Extrudes polymer through reusable dies.",
    "polymer_reclaimer": "Reclaims clean polymer scrap, at a cost.",
    "acid_concentrator": "Boils dilute acid into concentrated acid.",
    "chemical_dissolution_chamber": "Dissolves fluorite in acid into hydrogen fluoride.",
    "fluorocarbon_reactor": "Joins a carbon feed and a halogen into fluorocarbons.",
    "fluoromonomer_cracker": "Cracks fluorocarbons into fluoromonomer.",
    "ptfe_sintering_oven": "Sinters shaped PTFE into finished parts.",
    "lined_fluorochemical_reactor": "Makes fluorocarbons in an acid-proof lining.",
}

# What each machine is for, in a line the terminal shows.
TAGLINES = {
    "lead_acid_battery_bank":"Steel-era storage. Front output; other faces charge.",
    "flow_tank_module":"Paired capacity. Connect behind the middle bottom block.",
    "flow_stack_module":"Rate upgrade. Mount above the middle front top block.",
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
    "hydroponic_bay": "Grows crops in nutrient solution, anywhere.",
    "electroplating_bath": "Plates and repairs tools, weapons and armor.",
    "ammonia_chiller": "Freezes water into ice, anywhere.",
    "cryogenic_liquefier": "Liquefies oxygen, methane and hydrogen. Cooling tools unlock colder gases.",
    "rocket_workshop": "Builds rocket parts and rockets.",
    "water_wheel": "Turns flowing water into power.",
    "auto_crafter": "Crafts the pattern in its grid.",
    "large_steam_engine": "Boils water into rotation for shafts.",
    "geothermal_generator": "Turns lava into power.",
    "pumpjack": "Pumps crude oil from a reservoir.",
    "distillation_tower": "Splits crude and heavy oil by boiling point.",
    "catalytic_cracker": "Cracks heavy oil; reforms naphtha.",
    "fracking_rig": "Frees oil and gas from shale.",
    "flowback_treatment_unit": "Settles oil sand, flowback water, mud.",
    "diesel_generator": "Burns diesel into power.",
    "gas_turbine": "Burns gas or gasoline into power.",
    "polymerization_reactor": "Turns gases into plastic and rubber.",
    "electrolytic_cell": "Splits brine or water; smelts aluminum.",
    "chemical_reactor": "Mixes and reacts items with fluids.",
    "air_separation_unit": "Splits air into nitrogen, oxygen, argon.",
    "synthesis_converter": "Makes ammonia and nitric acid.",
    "fuel_cell": "Turns hydrogen into power.",
    "hydrotreater": "Upgrades diesel and gasoline to premium.",
    "heat_recovery_unit": "Power from a generator's exhaust heat.",
    "lithium_battery_bank": "Stores 32M JE. Gives it out of its front.",
    "flow_battery": "Stores 1,000 JE per mB of electrolyte.",
    "lithography_station": "Exposes patterned wafers and makes legacy chips.",
    "gas_centrifuge_cascade": "Enriches uranium for reactor fuel.",
    "fuel_fabricator": "Makes reactor fuel assemblies.",
    "gas_holder": "Holds 1,024 buckets of one gas.",
    "advanced_solar_panel": "Turns sunlight into 64 JE/t.",
    "advanced_engine": "Burns fuel into rotation. Turbo slot.",
    "crop_harvester": "Harvests and replants ripe crops.",
    "diesel_engine": "Burns diesel into rotation for shafts.",
    "wind_turbine": "Turns wind into power. Higher is better.",
}




def look_of(machine):
    if machine in WORKSHOP:
        return "workshop"
    if machine in ELECTRIC:
        return "electric"
    if machine in LAB:
        return "lab"
    if machine in ADVANCED:
        return "advanced"
    return "industrial"


def _bevel(d, box, light, dark, face=None):
    """A raised box (light top-left edge) over an inclusive pixel box, optionally filled."""
    x0, y0, x1, y1 = box
    if face is not None:
        d.rectangle(box, fill=face)
    d.line((x0, y0, x1, y0), fill=light)
    d.line((x0, y0, x0, y1), fill=light)
    d.line((x0, y1, x1, y1), fill=dark)
    d.line((x1, y0, x1, y1), fill=dark)


def _chamfer(d, box, color, cut=4, fill=None):
    """An outline with its top-left and bottom-right corners cut, as a hologram panel."""
    x0, y0, x1, y1 = box
    points = [(x0 + cut, y0), (x1, y0), (x1, y1 - cut), (x1 - cut, y1), (x0, y1), (x0, y0 + cut)]
    if fill is not None:
        d.polygon(points, fill=fill)
    d.line(points + [points[0]], fill=color)


def _grid(d, box, color, step):
    x0, y0, x1, y1 = box
    for x in range(x0, x1 + 1, step):
        d.line((x, y0, x, y1), fill=color)
    for y in range(y0, y1 + 1, step):
        d.line((x0, y, x1, y), fill=color)


def _casing(img, d, name, box):
    """The machine's body, in the look's material."""
    t = SKINS[name]
    c = t["casing"]
    x0, y0, x1, y1 = box
    if name == "workshop":
        # Soft brass blotches: smooth value noise on a coarse grid, in three tones.
        rng = random.Random(1401)
        cell = 14
        grid = [[rng.random() for _ in range((x1 - x0) // cell + 3)] for _ in range((y1 - y0) // cell + 3)]

        def smooth(v):
            return v * v * (3 - 2 * v)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                fx, fy = (x - x0) / cell, (y - y0) / cell
                ix, iy = int(fx), int(fy)
                tx, ty = smooth(fx - ix), smooth(fy - iy)
                top = grid[iy][ix] * (1 - tx) + grid[iy][ix + 1] * tx
                bottom = grid[iy + 1][ix] * (1 - tx) + grid[iy + 1][ix + 1] * tx
                v = top * (1 - ty) + bottom * ty
                img.putpixel((x, y), (c[2] if v < 0.38 else c[3] if v < 0.72 else c[4]) + (255,))
        _bevel(d, box, c[4], c[0])
        d.rectangle(box, outline=t["outline"])
        for sx, sy in ((x0 + 2, y0 + 2), (x1 - 3, y0 + 2), (x0 + 2, y1 - 3), (x1 - 3, y1 - 3)):
            d.rectangle((sx, sy, sx + 1, sy + 1), fill=c[0])
            img.putpixel((sx, sy), c[4] + (255,))
    elif name == "industrial":
        d.rectangle(box, fill=c[2])
        for y in range(y0, y1 + 1, 2):
            d.line((x0, y, x1, y), fill=c[3] if (y // 2) % 3 == 0 else c[2])
        _bevel(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), c[4], c[1])
        d.rectangle(box, outline=t["outline"])
    elif name == "electric":
        d.rectangle(box, fill=c[2])
        _bevel(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), c[4], c[0])
        _bevel(d, (x0 + 2, y0 + 2, x1 - 2, y1 - 2), c[3], c[1])
        d.rectangle(box, outline=t["outline"])
        for vy in range(y0 + 14, y1 - 6, 5):  # Vents down the right edge.
            d.rectangle((x1 - 3, vy, x1 - 2, vy + 1), fill=c[0])
        for cx, cy in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
            img.putpixel((cx, cy), (0, 0, 0, 0))
    elif name == "lab":
        d.rectangle(box, fill=t["screen"])
        _grid(d, box, t["grid"], 8)
        _bevel(d, box, c[4], c[1])
        _bevel(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), c[3], c[2])
        d.rectangle((x0 + 2, y0 + 2, x1 - 2, y1 - 2), outline=c[0])
    else:  # advanced
        _chamfer(d, box, t["frame"], cut=6, fill=c[1])
        for y in range(y0 + 1, y1, 3):
            d.line((x0 + 1, y, x1 - 1, y), fill=c[2])
        _chamfer(d, box, t["frame"], cut=6)
        _chamfer(d, (x0 + 2, y0 + 2, x1 - 2, y1 - 2), (24, 92, 94), cut=5)


def _pane(d, name, box):
    """A display pane on the body: the machine pane and the status pane."""
    t = SKINS[name]
    x0, y0, x1, y1 = box
    if name == "advanced":
        _chamfer(d, box, t["frame"], cut=4, fill=t["screen"])
    elif name == "lab":
        d.rectangle(box, fill=t["screen"], outline=t["frame"])
        _grid(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), t["grid"], 8)
        d.rectangle(box, outline=t["frame"])
    elif name == "industrial":
        _bevel(d, (x0 - 1, y0 - 1, x1 + 1, y1 + 1), t["casing"][0], t["casing"][4], t["casing"][1])
        d.rectangle(box, fill=t["screen"], outline=t["frame"])
    elif name == "electric":
        d.rectangle((x0 - 1, y0 - 1, x1 + 1, y1 + 1), fill=t["casing"][0])
        d.rectangle(box, fill=t["screen"])
        _grid(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), t["grid"], 6)
        d.rectangle(box, outline=t["frame"])
    else:  # workshop: dark glass with a faint orange grid, a lighter scan band and an orange rim
        d.rectangle((x0 - 1, y0 - 1, x1 + 1, y1 + 1), fill=t["outline"])
        d.rectangle(box, fill=t["screen"])
        _grid(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), t["grid"], 5)
        band = y0 + (y1 - y0) * 3 // 5
        for y in range(band, min(band + 3, y1)):
            d.line((x0 + 1, y, x1 - 1, y), fill=t["band"])
        d.rectangle(box, outline=t["frame"])


def _slot(d, name, x, y):
    """A player inventory slot (x, y is the item's corner, as the menu places it)."""
    dark, face, light = SKINS[name]["slot"]
    box = (x - 1, y - 1, x + 16, y + 16)
    if name in OUTLINED:
        d.rectangle(box, fill=face, outline=dark)
    else:
        _bevel(d, box, dark, light, face)


def _tray(d, name, box):
    t = SKINS[name]
    dark, face, light = t["tray"]
    if name == "advanced":
        _chamfer(d, box, light, cut=5, fill=dark)
    elif name == "lab":
        d.rectangle(box, fill=dark, outline=light)
    else:
        _bevel(d, box, light, dark, face)
        d.rectangle(box, outline=t["outline"])


def _help_box(d, name, box):
    t = SKINS[name]
    if name == "advanced":
        _chamfer(d, box, t["frame"], cut=4, fill=t["help"])
    elif name == "lab":
        d.rectangle(box, fill=t["help"], outline=t["frame"])
    elif name == "industrial":
        _bevel(d, (box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1), t["casing"][0], t["casing"][4], t["casing"][1])
        d.rectangle(box, fill=t["help"], outline=t["frame"])
    else:
        d.rectangle(box, fill=t["help"], outline=t["slot"][2] if name == "workshop" else t["frame"])
        d.rectangle((box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1), outline=t["outline"])


def background(name):
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    _casing(img, d, name, BODY)
    _pane(d, name, MACHINE_PANE)
    _pane(d, name, STATUS_PANE)
    _tray(d, name, TRAY)
    for row in range(3):
        for col in range(9):
            _slot(d, name, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        _slot(d, name, 8 + col * 18, 142)
    _help_box(d, name, HELP_BOX)
    return img


def body_tile(name):
    """A 96-pixel tile of a patterned look's casing, which the other screens repeat over bodies of any size."""
    c = SKINS[name]["casing"]
    img = Image.new("RGBA", (96, 96), c[2] + (255,))
    if name == "workshop":
        # The machine backgrounds' soft brass blotches, on a grid that wraps so the tiles join unseen.
        rng = random.Random(1401)
        cells = 8
        cell = 96 // cells
        grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]

        def smooth(v):
            return v * v * (3 - 2 * v)
        for y in range(96):
            for x in range(96):
                ix, iy = x // cell, y // cell
                tx, ty = smooth(x % cell / cell), smooth(y % cell / cell)
                top = grid[iy][ix] * (1 - tx) + grid[iy][(ix + 1) % cells] * tx
                bottom = grid[(iy + 1) % cells][ix] * (1 - tx) + grid[(iy + 1) % cells][(ix + 1) % cells] * tx
                v = top * (1 - ty) + bottom * ty
                img.putpixel((x, y), (c[2] if v < 0.38 else c[3] if v < 0.72 else c[4]) + (255,))
    else:  # industrial: brushed steel, a lighter line every third pair of rows
        d = ImageDraw.Draw(img)
        for y in range(0, 96, 2):
            d.line((0, y, 95, y), fill=c[3] if (y // 2) % 3 == 0 else c[2])
    return img


def draw_all(save_gui):
    """save_gui(img, name) writes textures/gui/<name>.png."""
    for name in MACHINE_LOOKS:
        save_gui(background(name), f"machine_{name}")
    for name in ("workshop", "industrial"):
        save_gui(body_tile(name), f"screen_{name}")


def write_all(write, assets, lang, machines):
    """Each look's colours, which look every machine and form uses, the taglines and the screens' words."""
    looks = {}
    for name, colors in COLORS.items():
        look = {key: f"{value:08X}" for key, value in colors.items()}
        dark, face, light = SKINS[name]["slot"]
        look.update({"slot_dark": "FF%02X%02X%02X" % dark, "slot_face": "FF%02X%02X%02X" % face,
                     "slot_light": "FF%02X%02X%02X" % light, "outlined": name in OUTLINED})
        # What the other screens draw their bodies and panes with (client/ScreenKit).
        skin = SKINS[name]
        look.update({"shape": SHAPES.get(name, name), "casing": ["FF%02X%02X%02X" % c for c in skin["casing"]],
                     "outline": "FF%02X%02X%02X" % skin["outline"], "screen": "FF%02X%02X%02X" % skin["screen"],
                     "grid": "FF%02X%02X%02X" % skin["grid"], "frame": "FF%02X%02X%02X" % skin["frame"],
                     "help_box": "FF%02X%02X%02X" % skin["help"]})
        if skin["band"] is not None:
            look["band"] = "FF%02X%02X%02X" % skin["band"]
        looks[name] = look
    write(assets / "gui" / "machine_themes.json",
          {"themes": looks, "machines": {machine: look_of(machine) for machine in machines}, "forms": FORM_LOOK,
           "screens": SCREENS})
    for machine in machines:
        lang[f"container.jugcraft.tagline.{machine}"] = TAGLINES[machine]
    import industrial_forms
    for form in industrial_forms.FORMS:  # Every form needs a line: a KeyError names the one missing.
        lang[f"container.jugcraft.tagline.{form}"] = FORM_TAGLINES[form]
    words = {
        "running": "RUNNING", "idle": "IDLE", "no_power": "NO POWER", "generating": "GENERATING",
        "charging": "CHARGING", "storing": "STORING", "holding": "HOLDING", "stopped": "STOPPED",
        "discharging": "DISCHARGING", "sides": "SIDES", "power": "POWER", "rate": "RATE", "fuel": "FUEL",
        "output_full": "OUTPUT FULL", "time_left": "%s left", "emitted": "EMITTED",
    }
    for key, text in words.items():
        lang[f"container.jugcraft.terminal.{key}"] = text
    # What to do about the state the status pane shows.
    hints = {
        "no_power": "Connect a cable or a generator.",
        "add_input": "Put something to work on in IN.",
        "output_full": "Take the results from OUT, or turn on Eject.",
        "add_fuel": "Put fuel in its slot.",
    }
    for key, text in hints.items():
        lang[f"container.jugcraft.hint.{key}"] = text
    # The slots' small labels, and what an empty slot says when hovered.
    for key, text in {"in": "IN", "out": "OUT", "extra": "EXTRA"}.items():
        lang[f"container.jugcraft.label.{key}"] = text
    slots = {
        "in": "Input: what the machine works on",
        "out": "Output: what it makes",
        "extra": "Extra: what it sometimes makes as well",
        "upgrade": "Upgrade: a speed or efficiency card",
        "fuel": "Fuel: coal, charcoal, coke or a coal block",
        "water": "Water: a water bucket to fill the boiler",
        "bucket": "Empty buckets come out here",
    }
    for key, text in slots.items():
        lang[f"container.jugcraft.slot.{key}"] = text
    lang["container.jugcraft.button.sides"] = "Sides: choose what each face does"
    lang["container.jugcraft.button.status"] = "Back to the status"
    lang["container.jugcraft.sides.help"] = "Click a face to change it."
    # The faces' key in the help box: short enough for one line each.
    for key, text in {"input": "in", "output": "out", "both": "in and out", "none": "closed"}.items():
        lang[f"container.jugcraft.sides.key.{key}"] = text
    # The other machine screens' new words: headings, slot help and a line on how to use each.
    screens = {
        "prospector.survey": "Survey: 3x3 chunks",
        "prospector.signals": "%s signals",
        "prospector.none": "No resonance detected",
        "prospector.fainter": "+%s fainter",
        "prospector.depth.0": "Shallow",
        "prospector.depth.1": "Middle",
        "prospector.depth.2": "Deep",
        "prospector.help": "Strongest first. More bars, more ore. Readings are approximate.",
        "rocket_pad.slot.cargo": "Cargo: what the rocket carries",
        "rocket_pad.slot.rocket": "Rocket: a Delivery Rocket, used up on launch",
        "rocket_pad.slot.plan": "Flight plan: the pad to deliver to",
        "rocket_pad.launch_help": "Sends the cargo to the pad the flight plan names.",
        "logic_controller.rule": "#",
        "logic_controller.sensor": "SENSOR",
        "logic_controller.level": "LEVEL",
        "logic_controller.target": "TARGET",
        "logic_controller.set": "SET",
        "logic_controller.channels": "CHANNELS",
        "logic_controller.help": "Click any part of a rule to change it; its number turns it on or off.",
        "logic_controller.tip.enable": "Turn this rule on or off",
        "logic_controller.tip.sensor": "The channel to read: click for the next",
        "logic_controller.tip.compare": "Below or above the level",
        "logic_controller.tip.down": "Lower the level by 5%",
        "logic_controller.tip.up": "Raise the level by 5%",
        "logic_controller.tip.target": "The channel to switch: click for the next",
        "logic_controller.tip.action": "Switch it on or off",
        "tank.heading.contents": "CONTENTS",
        "tank.heading.flow": "FLOW",
        "tank.heading.outlet": "OUTLET",
        # The benches in the workshop look.
        "cooking_pot.assisted": "+50% speed",
        "cave_survey.title": "Cave survey",
        "cave_survey.choose": "CHOOSE AN EXPEDITION",
        "cave_survey.heading": "SURVEY",
        "cave_survey.found": "Found",
        "cave_survey.status.0": "A survey does not load or alter distant chunks.",
        "cave_survey.status.1": "Surveying nearby terrain... %s%%",
        "cave_survey.status.2": "X %s   Y -16   Z %s",
        "cave_survey.status.3": "No match within 1,024 blocks. Try farther away.",
        "cave_survey.status.5": "Survey from the Overworld.",
        "cave_survey.status.6": "Survey desks are busy. Try again shortly.",
        "cave_survey.help": "Bring paper and a cave tablet to the desk to keep the location on a physical map.",
    }
    for key, text in screens.items():
        lang[f"screen.jugcraft.{key}"] = text
