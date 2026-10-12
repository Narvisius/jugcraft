"""Industrial machine forms the game registers (docs/features/industrial-machine-foundation.md): the starter factory's
Electrolytic Separator (docs/features/industrial-electrolytic-separator.md) and Gas Burning Generator
(docs/features/industrial-gas-burning-generator.md), package 2 of the owner's factory plan.

Each form is described here for its art, recipes and data, and in IndustrialForms.java for the game;
tools/check_mod_data.py checks that the two agree. Layers are given bottom first, each as rows from the front to the
back, each row's characters from the viewer's left to right, standing in front of the machine: C the controller,
# another structural block, ~ clearance for a moving part and . open access space.

Models are authored in structure space like tools/large_machines.py: pixels, facing north with the front at z = 0,
the controller's block at 0..16 on every axis and the viewer's right towards -x. model_writer.split_model slices
them into one model per part, in the form's part order: the controller first, then every other structural block
from the bottom layer up, the front row first, left to right (MachineForm in Java).
"""
import math

import model_writer
# steampunk_models first: it imports dieselpunk_models' finished models at its end.
from steampunk_models import GLOW, box, cyl, dial, q, span
from dieselpunk_models import CHROME, EXHAUST, GAUGE, GRILLE, GUNMETAL, HAZARD, LAMP, OLIVE, RUBBER, STENCIL

MOD = "jugcraft"

# Splitting water and ordinary brine: what the Separator offers its family's recipes.
AQUEOUS_ELECTROLYSIS = f"{MOD}:aqueous_electrolysis"

# The owner's white tank family (the tank restyle) for the collection towers, with its checker band.
TANK_BODY, TANK_TOP, TANK_CHECKER = "tk_body", "tk_top", "tk_checker"
INSULATOR, BUSBAR, COPPER = "ceramic_insulator", "copper_busbar", "sp_copper"
# Amber level strips that light while it works (steampunk_models.GLOW).
STRIP = "dr_amber"
# The Gas Burning Generator's materials (the art brief): graphite ribs, olive combustion covers, a pale heat shield,
# a ribbed generator housing, and mint lamps at the electrical output that light while it runs.
SHIELD, RIBBED, MINT = "el_ribbed", "dw_ribbed_steel", "el_lamp"
# What lights while a form works: steampunk_models.GLOW and the mint lamp.
FORM_GLOW = dict(GLOW, el_lamp="el_lamp_on")
# What the generator burns (tools/petro.py FLUID_FUELS keeps the Fuel Cell's value): (fluid, JE per mB, JE a tick).
# Hydrogen at the Fuel Cell's 128 JE/mB and the owner's selected 128 JE/t; methane (256 JE/t) joins it with the
# methane Infuser.
GAS_GENERATOR_FUELS = [(f"{MOD}:hydrogen", 128, 128)]

FORMS = {
    "electrolytic_separator": {
        "display": "Electrolytic Separator",
        "family": "electrolytic_cell",
        "layers": [["C#", "##"], ["##", "##"], ["##", "##"]],
        # OperatingProfile.ENTRY: one batch at a time in 2,000 mB tanks, at the family's full draw.
        "profile": "ENTRY",
        "tank_mb": 2_000,
        "input_tanks": ["feed"],
        # Oxygen from water or chlorine from brine share one destination; the lye tank stays empty for water.
        "output_tanks": ["anode_gas", "hydrogen", "lye"],
        "capabilities": [AQUEOUS_ELECTROLYSIS],
        # Two upgrade slots (speed and efficiency cards), and a warm-up label for the first two seconds after a cold
        # start, inside the paid work (the owner's choice: heating included in the processing power).
        "upgrades": True,
        "warmup": 40,
        # (name, kind, target, column, row, layer, side), as FormPort in Java.
        "ports": [
            ("feed_in", "FLUID_IN", 0, 0, 1, 0, "BACK"),
            ("anode_gas_out", "FLUID_OUT", 0, 1, 0, 2, "FRONT"),
            ("hydrogen_out", "FLUID_OUT", 1, 0, 0, 2, "FRONT"),
            ("lye_out", "FLUID_OUT", 2, 1, 0, 1, "FRONT"),
            ("power", "ENERGY_IN", 0, 1, 0, 0, "FRONT"),
        ],
        # The owner's selected construction: 4 steel plates and two Steel Tanks (8 plates each), 20 plates in all,
        # with copper cable and a basic circuit where the Electrolytic Cell needs aluminum cable and an advanced one.
        "recipe": (["PWP", "TCT", "PMP"], {"P": "#c:plates/steel", "W": f"{MOD}:copper_cable", "T": f"{MOD}:steel_tank",
                                           "C": f"{MOD}:basic_circuit", "M": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "gas_burning_generator": {
        "display": "Gas Burning Generator",
        # The combustion generators' family (the Gas Turbine's energy figures and screen); the form's own fuel list,
        # not the family's, says what it burns, so the turbine never takes hydrogen.
        "family": "gas_turbine",
        # Every block the model reaches: the skid fills the ground; the engine-generator train the middle columns;
        # the housing and the cabinet the back rows; the cabinet's top two blocks of the fourth layer.
        "layers": [["C###", "####", "####", "####", "####"],
                   [".##.", ".##.", ".##.", "####", "####"],
                   [".##.", ".##.", ".##.", "####", "####"],
                   ["....", "....", "....", "....", "..##"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        "input_tanks": ["fuel"],
        "output_tanks": [],
        "capabilities": [],
        "fuels": GAS_GENERATOR_FUELS,
        "upgrades": False,
        "warmup": 0,
        "ports": [
            ("fuel_in", "FLUID_IN", 0, 3, 1, 0, "RIGHT"),
            ("power_out", "ENERGY_OUT", 0, 2, 4, 1, "BACK"),
        ],
        # The plan's starter bill (docs/features/industrial-starter-gas-and-acid-factory.md): 4 steel plates, an
        # electric motor, a steel gear, a Tinplate Tank, a basic circuit and a Machine Casing.
        "recipe": (["PGP", "MCT", "PKP"], {"P": "#c:plates/steel", "G": f"{MOD}:steel_gear", "M": f"{MOD}:electric_motor",
                                           "C": f"{MOD}:basic_circuit", "T": f"{MOD}:fluid_tank",
                                           "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
}

# Each form's Engineer's Handbook page (tools/handbook.py form_page), after its family's machine.
HANDBOOK = {
    "electrolytic_separator": [
        "Two wide, two deep and three tall: the first of the big industrial machines, built from steel, copper cable "
        "and a basic circuit, before the Electrolytic Cell's aluminum cable and advanced circuit. It is placed from "
        "the front; every block it fills must be free, or it says what is in the way and where.",
        "Pipe water or brine into the inlet low at the back. A bucket of water gives 500 mB of hydrogen (the top "
        "left collar) and 250 mB of oxygen (top right) in 40 seconds; a bucket of brine gives 250 mB of hydrogen, "
        "250 mB of chlorine (top right) and 500 mB of lye (the return on the right) in 20 seconds. It draws 256 JE/t "
        "through the socket at the foot of the right tower.",
        "It keeps its lye: when the lye tank is full, brine stops until the lye is piped away. Its screen says what "
        "it is waiting for, and upgrade cards fit its terminal.",
    ],
    "gas_burning_generator": [
        "Four wide, five deep and four tall: a gas engine turning a generator on a steel skid, built from steel "
        "plates, an electric motor, a steel gear, a Tinplate Tank and a basic circuit. Every block it fills must be "
        "free when it is placed; the corners above the skid at the front stay open.",
        "Pipe hydrogen into the capped inlet low on its right side. It burns a millibucket a tick, 128 JE each, and "
        "sends 128 JE/t out of the socket on the terminal cabinet at the back: a bucket lasts 50 seconds. Its coupling "
        "and intake fan spin up as it starts.",
        "It makes only as much as its store has room for, so no fuel is wasted when nothing draws its power, and its "
        "screen says when the fuel tank is empty or the store is full. Methane, at 256 JE/t, comes with the methane "
        "Infuser.",
    ],
}

# Tank labels on the screen and in its reasons (MachineStatus.tankLabel), by role.
TANK_NAMES = {
    "fuel": "Fuel tank",
    "feed": "Feed tank",
    "anode_gas": "Oxygen/chlorine tank",
    "hydrogen": "Hydrogen tank",
    "lye": "Lye tank",
}

# Recipes only forms run: each names the capability a form must offer. Fluid results go to output tanks by their
# third value. The quantities and times are the owner's selected starter baseline
# (docs/features/industrial-starter-gas-and-acid-factory.md, eighth batch, question 9).
FORM_RECIPES = {
    "electrolytic_separator": [
        # Ordinary brine: chlorine at the anode, hydrogen at the cathode, lye left behind. 400 ticks at 256 JE/t,
        # 102,400 JE, the same as the Electrolytic Cell's own brine recipe.
        {"name": "separator_brine", "capability": AQUEOUS_ELECTROLYSIS, "fluids": [(f"{MOD}:brine", 1000)],
         "fluid_results": [(f"{MOD}:chlorine", 250, 0), (f"{MOD}:hydrogen", 250, 1), (f"{MOD}:lye", 500, 2)],
         "ticks": 400, "features": ["salt"]},
        # Water: two hydrogen to one oxygen, as in the cell. 800 ticks at 256 JE/t, 204,800 JE.
        {"name": "separator_water", "capability": AQUEOUS_ELECTROLYSIS, "fluids": [("minecraft:water", 1000)],
         "fluid_results": [(f"{MOD}:oxygen", 250, 0), (f"{MOD}:hydrogen", 500, 1)],
         "ticks": 800, "features": ["machines"]},
    ],
}


def blocks():
    return list(FORMS)


def footprint(layers):
    """The parts' offsets from the controller, in part order (MachineForm in Java): x towards the viewer's left,
    y up, z towards the back."""
    width, depth = len(layers[0][0]), len(layers[0])
    cells = [(column, row, layer) for layer in range(len(layers)) for row in range(depth) for column in range(width)]
    controller = next(c for c in cells if layers[c[2]][c[1]][c[0]] == "C")
    parts = [controller] + [c for c in cells if layers[c[2]][c[1]][c[0]] == "#"]
    return [(controller[0] - column, layer - controller[2], row - controller[1]) for column, row, layer in parts]


def electrolytic_separator():
    """Two wide, two deep, three tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a tall
    olive cell housing with a stepped door stands between two narrower round collection towers, white with checker
    shoulders, on a shared gunmetal plinth. At the front: the left-side control box with two gauges and an amber lamp
    (the controller), an insulated bus connection with its power socket at the foot of the right tower, the lye
    return recessed above it, and an outlet pipe forward from each tower to a collar high on the front, hydrogen on
    the left and oxygen or chlorine on the right, banded in their colours. The feed comes into the cell low at the
    back. Each port sits at the middle of its block's face, where pipes and cables meet it."""
    m = [box((-16, 0, 0), (16, 2, 32), GUNMETAL)]
    # The cell housing: a gunmetal skirt, the olive body and a crown with four electrode feedthroughs on ceramic
    # insulators and copper caps, joined in pairs by bus bars.
    m.append(box((-6.5, 2, 2.5), (6.5, 4, 26.5), GUNMETAL))
    m.append(box((-6, 4, 3), (6, 44, 26), {"*": OLIVE, "up": GUNMETAL}))
    for y in (15.25, 31.25):
        m.append(box((-6.5, y, 2.5), (6.5, y + 1.5, 26.5), GUNMETAL))
    m.append(box((-7, 44, 2), (7, 46, 27), GUNMETAL))
    for x in (-3, 3):
        for z in (8, 20):
            m += cyl("y", x, z, 1.25, 46, 47.25, INSULATOR)
            m.append(box((x - 0.75, 47.25, z - 0.75), (x + 0.75, 47.75, z + 0.75), COPPER))
    for z in (8, 20):
        m.append(box((-2.25, 46.5, z - 0.5), (2.25, 47, z + 0.5), BUSBAR))
    # The stepped door: a gunmetal frame, an olive leaf, a stencilled upper panel, a handle on the right and two
    # hinges on the left.
    m.append(box((-4.5, 6, 2.25), (4.5, 36, 3), GUNMETAL))
    m.append(box((-3.5, 7, 1.5), (3.5, 35, 2.25), OLIVE))
    m.append(box((-2.5, 22, 1), (2.5, 33, 1.5), {"*": OLIVE, "north": STENCIL}))
    m.append(box((-3.25, 13, 0.75), (-2.5, 18, 1.5), CHROME))
    for y in (17, 30):
        m.append(box((3.75, y, 1.5), (4.75, y + 2, 2.25), CHROME))
    # The collection towers, round and white, either side of the housing: a checker shoulder, a gunmetal dome with
    # a chrome vent, and an amber level strip on the outer side.
    for sign in (1, -1):
        cx = sign * 10.5
        m += cyl("y", cx, 14, 4, 2, 41, TANK_BODY, TANK_TOP)
        m += cyl("y", cx, 14, 4.4, 33, 35, TANK_CHECKER)
        m += cyl("y", cx, 14, 3, 41, 42.5, GUNMETAL)
        m += cyl("y", cx, 14, 1.25, 42.5, 44, CHROME)
        strip_lo, strip_hi = sorted((sign * 14.5, sign * 14.75))
        m.append(box((strip_lo, 8, 13), (strip_hi, 30, 15), STRIP))
    # An outlet pipe forward from each tower to a collar at the middle of its upper front face: charcoal, banded in
    # its gas's colours, with a chrome flange where pipes meet it.
    for x, bands in ((8, ["hydrogen_still"]), (-8, ["oxygen_still", "chlorine_still"])):
        m.append(box((x - 1.5, 38.5, 1), (x + 1.5, 41.5, 12), RUBBER))
        for number, band in enumerate(bands):
            z = 3 + number * 1.5
            m.append(box((x - 2, 38, z), (x + 2, 42, z + 1), band))
        m.append(box((x - 3, 37, 0.25), (x + 3, 43, 1), CHROME))
    # The control box (the controller), standing on the plinth before the left tower: two gauges, the running lamp
    # and a level strip.
    m.append(box((5.25, 2, 0.5), (15, 15, 7), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (12.5, 10.5, 0.25), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (8, 10.5, 0.25), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (10.25, 5, 0.25), 2.5, texture=LAMP, body=GUNMETAL))
    # The insulated bus connection at the foot of the right tower: a gunmetal junction box with a hazard band at
    # the live end, its power socket, two ceramic insulators on top and a copper bus bar into the cell housing.
    m.append(box((-14, 2, 1.25), (-6, 13, 7), GUNMETAL))
    m.append(box((-14, 13, 1.25), (-6, 14, 7), HAZARD))
    m.append(box((-11, 5, 0.25), (-5, 11, 1.25), {"*": CHROME, "north": "power_port!"}))
    for x in (-12, -9):
        m += cyl("y", x, 3.5, 1, 14, 17.5, INSULATOR)
    m.append(box((-12.75, 17.5, 2.75), (-6, 18.5, 4.25), BUSBAR))
    # The lye return from the cell, recessed in a gunmetal frame bolted to the housing: a dark well, a pipe stub in
    # lye colour and a chrome flange.
    m.append(box((-12.5, 19.5, 1.5), (-4.75, 28.5, 7), GUNMETAL))
    m.append(box((-11.5, 20, 1.25), (-5, 28, 1.5), "sp_hopper_inside"))
    m.append(box((-9.5, 22.5, 0.75), (-6.5, 25.5, 1.25), "lye_still"))
    m.append(box((-10.5, 21.5, 0.25), (-5.5, 26.5, 0.75), CHROME))
    # The feed inlet into the cell, low at the back: a charcoal pipe banded in brine colour and a chrome flange.
    m.append(box((5.5, 6.5, 24), (9.5, 9.5, 31), RUBBER))
    m.append(box((5.25, 6, 27.5), (10, 10, 28.5), "brine_still"))
    m.append(box((5, 5, 31), (11, 11, 31.75), CHROME))
    # A gunmetal access panel on the back of the cell housing.
    m.append(box((-4, 8, 26), (4, 36, 26.75), {"*": GUNMETAL, "south": STENCIL}))
    return m


def octagon_ring(axis, cu, cv, r_out, r_in, a0, a1, texture):
    """A ring of eight bars round (cu, cv) along `axis`: an octagon r_out across its flats with a hole r_in across."""
    side = q(r_out * math.tan(math.radians(22.5)))
    out = []
    for lo, hi in ((r_in, r_out), (-r_out, -r_in)):
        out.append(box(*span(axis, a0, a1, q(cu - side), q(cu + side), q(cv + lo), q(cv + hi)), texture))
        out.append(box(*span(axis, a0, a1, q(cu + lo), q(cu + hi), q(cv - side), q(cv + side)), texture))
    # The four corners, a little shorter along the axis so their caps never share a plane with the bars'.
    middle = (r_in + r_out) / 2 / math.sqrt(2)
    half = (r_out - r_in) / 2
    for su in (1, -1):
        for sv in (1, -1):
            u, v = cu + su * middle, cv + sv * middle
            out.append(box(*span(axis, a0 + 0.125, a1 - 0.125, q(u - half), q(u + half), q(v - half), q(v + half)), texture))
    return out


def arch(cu, cv, r, thickness, z0, z1, texture):
    """An arched hood over a cylinder along z: a flat crown, stepped shoulders and straight sides down to the axis,
    the outside an octagon r across its flats, `thickness` thick."""
    side = q(r * math.tan(math.radians(22.5)))
    out = [box((q(cu - side), q(cv + r - thickness), z0), (q(cu + side), q(cv + r), z1), texture)]
    steps = 3
    for sign in (1, -1):
        for step in range(steps):
            u0 = side + (r - side) * step / steps
            u1 = side + (r - side) * (step + 1) / steps
            top = cv + r + side - u0
            bottom = cv + r + side - u1 - thickness
            lo, hi = sorted((cu + sign * u0, cu + sign * u1))
            out.append(box((q(lo), q(bottom), z0), (q(hi), q(top), z1), texture))
        lo, hi = sorted((cu + sign * (r - thickness), cu + sign * r))
        out.append(box((q(lo), q(cv), z0), (q(hi), q(cv + side - thickness), z1), texture))
    return out


def gas_burning_generator():
    """Four wide, five deep and four tall (the art brief, docs/features/industrial-machine-models-and-textures.md): an
    engine-generator train along the middle of a gunmetal skid. At the front, the round intake with its fan (drawn
    turning by MachineRotors) and, at the front left, the stepped ignition box with its gauges and amber lamp (the
    controller). Behind them the olive combustion section with graphite ribs and amber sight glasses, under an arched
    pale heat shield on four posts; the guarded coupling (turning, behind hazard-banded hoops); then the large ribbed
    generator housing with a cooling grille at its back. At the rear right the tall terminal cabinet: an olive door,
    mint lamps, the power socket and copper terminals on ceramic insulators on top. The capped gas inlet enters the
    right side above the skid and runs through a regulator to the burner; the exhaust leaves the burner's belly, runs
    back low along the skid on the left and turns out at the back."""
    cu, cv = -16, 26
    m = [box((-47, 0, 1), (15, 3, 79), GUNMETAL)]
    # The combustion section on two saddles: olive covers, graphite ribs, a dark intake throat at its front.
    for z in (9, 31):
        m.append(box((cu - 9, 3, z), (cu + 9, 14, z + 4), GUNMETAL))
    m += cyl("z", cu, cv, 13, 6, 40, OLIVE, EXHAUST)
    for z in (11, 19, 27, 35):
        m += cyl("z", cu, cv, 13.75, z, z + 1.5, GUNMETAL)
    # Amber sight glasses on either side of the burner, in gunmetal frames.
    for sign in (1, -1):
        frame_lo, frame_hi = sorted((cu + sign * 13, cu + sign * 13.4))
        glass_lo, glass_hi = sorted((cu + sign * 13, cu + sign * 13.6))
        m.append(box((frame_lo, 20.5, 8.5), (frame_hi, 25.5, 15.5), GUNMETAL))
        m.append(box((glass_lo, 21, 9), (glass_hi, 25, 15), STRIP))
    # The round intake: a chrome rim round the throat and a cross of guard bars in front of the fan, with a hub cap.
    m += octagon_ring("z", cu, cv, 14, 11.5, 3, 6.5, CHROME)
    m.append(box((cu - 12, cv - 0.75, 2.25), (cu + 12, cv + 0.75, 3), GUNMETAL))
    m.append(box((cu - 0.75, cv - 12, 2.5), (cu + 0.75, cv + 12, 2.75), GUNMETAL))
    m += cyl("z", cu, cv, 2, 1.75, 2.25, CHROME)
    # The arched heat shield over the combustion section, on four posts down to the skid.
    m += arch(cu, cv, 16, 1.5, 4, 42, SHIELD)
    for sign in (1, -1):
        lo, hi = sorted((cu + sign * 14.5, cu + sign * 16))
        for z in (4, 40):
            m.append(box((lo, 3, z), (hi, cv, z + 2), GUNMETAL))
    # The stepped ignition box (the controller) at the front left: gauges and the amber running lamp, a stencilled
    # upper step, and a conduit under the burner.
    m.append(box((2, 3, 1), (14, 12, 12), {"*": OLIVE, "up": STENCIL}))
    m.append(box((4, 12, 3), (12, 15.5, 10), GUNMETAL))
    m.append(dial("north", (11, 8.5, 0.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (6.5, 8.5, 0.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (8.75, 4.75, 0.75), 2.25, texture=LAMP, body=GUNMETAL))
    m.append(box((-8.5, 13.5, 6.5), (2, 15.5, 8.5), RUBBER))
    # The guarded coupling between the combustion section and the generator: hazard-banded hoops joined by four bars,
    # standing on a gunmetal leg.
    m += octagon_ring("z", cu, cv, 10.5, 9.5, 40.5, 41.5, HAZARD)
    m += octagon_ring("z", cu, cv, 10.5, 9.5, 46.5, 47.5, HAZARD)
    for su in (1, -1):
        for sv in (1, -1):
            u, v = cu + su * 7, cv + sv * 7
            m.append(box((u - 0.75, v - 0.75, 41.5), (u + 0.75, v + 0.75, 46.5), GUNMETAL))
    m.append(box((cu - 1, 3, 43), (cu + 1, cv - 10.5, 45), GUNMETAL))
    # The large ribbed generator housing on two cradles, a bearing boss at its front and a cooling grille at its back.
    for z in (51, 65):
        m.append(box((cu - 12, 3, z), (cu + 12, 8, z + 4), GUNMETAL))
    m += cyl("z", cu, cv, 20, 48, 72, OLIVE, GUNMETAL)
    for z in (49.5, 53.5, 57.5, 61.5, 65.5, 69.5):
        m += cyl("z", cu, cv, 21, z, z + 1.5, RIBBED)
    m += cyl("z", cu, cv, 8, 46.5, 48, GUNMETAL)
    m += cyl("z", cu, cv, 13, 72, 72.75, GRILLE)
    # The terminal cabinet at the rear right: gunmetal, an olive door with a stencil, a hazard band at the top, two
    # mint lamps, the power socket, and three ceramic insulators with copper caps and a bus bar on top. Black cables
    # come over from the generator housing.
    m.append(box((-47, 3, 72.5), (-21, 54, 79), GUNMETAL))
    m.append(box((-44.5, 26, 79), (-30.5, 48, 79.5), {"*": OLIVE, "south": STENCIL}))
    m.append(box((-47.25, 50, 72.25), (-20.75, 52, 79.25), HAZARD))
    for y in (45.5, 40):
        m.append(dial("south", (-25.5, y, 79.25), 2.5, texture=MINT, body=CHROME))
    m.append(box((-27, 21, 79), (-21, 27, 79.75), {"*": CHROME, "south": "power_port!"}))
    for x in (-42, -34, -26):
        m += cyl("y", x, 76, 1.5, 54, 57.5, INSULATOR)
        m.append(box((x - 1, 57.5, 75), (x + 1, 58, 77), COPPER))
    m.append(box((-42.75, 58, 75.25), (-25.25, 59, 76.75), BUSBAR))
    for x in (-30, -24):
        m.append(box((x - 1.5, 43, 60), (x + 1.5, 46, 72.5), RUBBER))
    # The exhaust: out of the combustion section's belly, across to the left, back along the skid and turned out at the
    # back.
    m.append(box((-12, 12, 23.5), (-8, 14, 28.5), EXHAUST))
    m.append(box((-10, 8, 23), (5, 12, 29), EXHAUST))
    m.append(box((5, 7, 23), (11, 12, 76), EXHAUST))
    for z in (44, 56, 68):
        m.append(box((4.75, 6.75, z), (11.25, 12.25, z + 1.5), CHROME))
    m.append(box((5, 7, 76), (11, 16, 79), EXHAUST))
    m.append(box((4.5, 10, 79), (11.5, 16.5, 79.75), {"*": CHROME, "south": EXHAUST}))
    # The capped gas inlet on the right side above the skid: a chrome flange, a black hose banded in hydrogen colour,
    # a regulator, and the line into the burner's belly.
    m.append(box((-47.75, 5, 21), (-47, 11, 27), CHROME))
    m += cyl("x", 8, 24, 2, -47, -35, RUBBER)
    m += cyl("x", 8, 24, 2.3, -44, -43, "hydrogen_still")
    m.append(box((-37.5, 10, 21.5), (-32.5, 15, 26.5), GUNMETAL))
    m += cyl("x", 13.25, 24, 1.25, -32.5, -23, RUBBER)
    return m


def gas_burning_generator_rotor():
    """The Gas Burning Generator's turning parts, about the train's axis (client/MachineRotors draws them): the intake
    fan's hub and six blades behind the guard bars, and the coupling's shaft and two bolted flanges inside the guard."""
    import machine_rotors as rotors
    cu, cv = -16, 26
    quads = rotors.disc("z", 3.25, 5.75, cu, cv, 2.5, CHROME, CHROME)
    for blade in range(6):
        angle = math.radians(60 * blade + 15)
        c, s = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cu + r * c - w * s, cv + r * s + w * c)
        quads += rotors.prism("z", 4, 5, [at(2.25, -1.25), at(10.25, -2), at(10.25, 2), at(2.25, 1.25)], GUNMETAL, GUNMETAL)
    quads += rotors.disc("z", 38, 50, cu, cv, 2.5, CHROME, CHROME)
    for z0, z1, bolts in ((42.5, 44, (42, 42.5)), (44.5, 46, (46, 46.5))):
        quads += rotors.disc("z", z0, z1, cu, cv, 7, CHROME, "dr_flange")
        for bolt in range(4):
            angle = math.radians(90 * bolt + 45)
            quads += rotors.disc("z", bolts[0], bolts[1], cu + 5 * math.cos(angle), cv + 5 * math.sin(angle), 0.75,
                                 GUNMETAL, GUNMETAL, sides=6)
    return quads


def gas_burning_generator_still():
    """The turning parts standing still, for the item model only (the block models leave them to MachineRotors)."""
    cu, cv = -16, 26
    return (cyl("z", cu, cv, 10.25, 4, 5, GUNMETAL, "dr_fan") + cyl("z", cu, cv, 2.5, 3.25, 5.75, CHROME)
            + cyl("z", cu, cv, 2.5, 38, 50, CHROME) + cyl("z", cu, cv, 7, 42.5, 44, CHROME, "dr_flange")
            + cyl("z", cu, cv, 7, 44.5, 46, CHROME, "dr_flange"))


# Turning parts (tools/machine_rotors.py writes them for client/MachineRotors): on the train's axis, spinning up over
# two seconds when the machine starts and running down as long when it stops, standing still while idle.
ROTORS = {
    "gas_burning_generator_train": {
        "block": "gas_burning_generator", "axis": "z", "center": [-16, 26, 24], "property": "lit", "speed": 12,
        "ease": 40, "always": True, "quads": gas_burning_generator_rotor(),
    },
}

MODELS = {"electrolytic_separator": electrolytic_separator(), "gas_burning_generator": gas_burning_generator()}
# Extra elements for a form's item model alone: its turning parts, standing still.
ITEM_EXTRAS = {"gas_burning_generator": gas_burning_generator_still()}
PARTICLE = "dp_olive"


def write_all(write, assets, data, lang, condition, self_drop):
    """Blockstates, sliced part models, item models, names, loot, the construction recipe and the form recipes."""
    out = model_writer.StyleWriter(assets)
    for role, name in TANK_NAMES.items():
        lang[f"container.{MOD}.form.tank.{role}"] = name
    for form, info in FORMS.items():
        lang[f"block.{MOD}.{form}"] = info["display"]
        elements = MODELS[form]
        item_elements = elements + ITEM_EXTRAS.get(form, [])
        textures = {name: f"{MOD}:block/{name}" for name in model_writer.texture_names(item_elements)}
        textures["particle"] = f"{MOD}:block/{PARTICLE}"
        glow = {name: f"{MOD}:block/{FORM_GLOW[name]}" for name in model_writer.texture_names(elements) if name in FORM_GLOW}
        offsets = footprint(info["layers"])
        for index, part in enumerate(model_writer.split_model(form, elements, offsets)):
            out.model(f"{form}_part{index}", {"ambientocclusion": False, "textures": textures, "elements": part})
            out.model(f"{form}_part{index}_on", {"parent": f"{MOD}:block/{form}_part{index}", "textures": glow})
        variants = {}
        for facing, y in model_writer.FACING_Y.items():
            rotation = {"y": y} if y else {}
            for lit in ("false", "true"):
                for part in range(len(offsets)):
                    on = "_on" if lit == "true" else ""
                    variants[f"facing={facing},lit={lit},part={part}"] = {"model": f"{MOD}:block/{form}_part{part}{on}",
                                                                          **rotation}
        out.blockstate(form, {"variants": variants})
        out.item_model(form, {"parent": "minecraft:block/block", "textures": textures,
                              "elements": model_writer.scaled_elements(item_elements)})
        out.item(form, f"{MOD}:item/{form}")
        write(data / "loot_table" / "blocks" / f"{form}.json", self_drop(form))
        pattern, key = info["recipe"]
        write(data / "recipe" / f"{form}.json", {
            "fabric:load_conditions": [c for f in info["features"] for c in condition(f)],
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{form}", "count": 1}})
    for kind, name, recipe in form_recipe_files(condition):
        write(data / "recipe" / kind / f"{name}.json", recipe)


def form_recipe_files(condition):
    """(recipe type, file name, JSON) for every form recipe: a family fluid recipe with its capability."""
    import petro
    for form, recipes in FORM_RECIPES.items():
        kind = petro.FLUID_MACHINES[FORMS[form]["family"]]["recipe_type"]
        for recipe in recipes:
            data = {"fabric:load_conditions": [c for f in recipe["features"] for c in condition(f)],
                    "type": f"{MOD}:{kind}", "capability": recipe["capability"]}
            if recipe.get("items"):
                data["items"] = [{"ingredient": item, "count": count} for item, count in recipe["items"]]
            if recipe.get("fluids"):
                data["fluids"] = [{"fluid": fluid, "amount": mb} for fluid, mb in recipe["fluids"]]
            if recipe.get("fluid_results"):
                data["fluid_results"] = [{"fluid": r[0], "amount": r[1]} | ({"tank": r[2]} if len(r) > 2 else {})
                                         for r in recipe["fluid_results"]]
            if recipe.get("results"):
                data["results"] = [{"id": item, "count": count} for item, count in recipe["results"]]
            data["time"] = recipe["ticks"]
            yield kind, recipe["name"], data


def recipe_view():
    """The forms' JEI categories (tools/recipe_view.py): one per form that runs recipes, its recipes and the form as the
    station. A generator form runs none (its fuels are in the Engineer's Handbook, like every generator's)."""
    return [{"block": f"{MOD}:{form}", "type": form,
             "recipes": [{"items": [[ref, count] for ref, count in recipe.get("items", [])],
                          "fluids": [[fluid, mb] for fluid, mb in recipe.get("fluids", [])],
                          "fluid_results": [[r[0], r[1]] for r in recipe.get("fluid_results", [])],
                          "results": [[item, count] for item, count in recipe.get("results", [])],
                          "ticks": recipe["ticks"]} for recipe in FORM_RECIPES[form]]}
            for form in FORMS if FORM_RECIPES.get(form)]
