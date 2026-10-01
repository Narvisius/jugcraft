"""Dieselpunk models for the steel-tier machines (see docs/ART_DIRECTION.md), replacing their steampunk looks.

Same footprints, ports and running lights as before (tools/large_machines.py, MachineKind): only the look
changes. Gunmetal shells with olive-drab panels worn to bare metal, hazard stripes, chrome trim, rubber
hoses, sooty exhaust stacks, green phosphor gauges and caged amber lamps (dieselpunk_textures).
steampunk_models.MODELS takes these in place of its own; the classic style pack is unchanged.
"""
from steampunk_models import box, cyl, dial, pipe, wheel

GUNMETAL, OLIVE, STENCIL, CHROME = "dp_gunmetal", "dp_olive", "dp_olive_stencil", "dp_chrome"
HAZARD, RUBBER, GRILLE, EXHAUST, BAKELITE = "dp_hazard", "dp_rubber", "dp_grille", "dp_exhaust", "dp_bakelite"
GAUGE, LAMP = "dp_gauge", "dp_lamp"


def steel_foundry():
    """Two by two by five blast furnace: an olive, riveted hearth with a glowing tap hole behind a chrome frame, a
    hazard-striped plinth, a hot-blast ring of sooty ducts with a tuyere into each side, a banded gunmetal stack
    and a grated charging deck with a hopper, safety rails and an exhaust stack."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    m.append(box((-15, 2, 1), (15, 26, 31), {"*": OLIVE, "north": STENCIL}))
    for y in (6, 16, 25):
        m.append(box((-15.5, y, 0.5), (15.5, y + 1.25, 31.5), GUNMETAL))
    # Tap hole: glowing window in a chrome frame, a pouring spout, a gauge and the running lamp.
    m.append(box((-5.5, 2.5, 0.25), (5.5, 11.5, 0.75), CHROME))
    m.append(box((-4.5, 3, 0), (4.5, 11, 0.25), {"*": GUNMETAL, "north": "sp_window!"}))
    m.append(box((-1.5, 2, -2.5), (1.5, 3, 0.25), GUNMETAL))
    m.append(dial("north", (-10.5, 12, 0.25), 4.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (10.5, 12, 0.25), 3, texture=LAMP, body=GUNMETAL))
    # Hot-blast ring and tuyeres.
    for frm, to in (((-16, 27, -0.5), (16, 30, 1.5)), ((-16, 27, 30.5), (16, 30, 32.5)),
                    ((-16.5, 27, 1.5), (-14.5, 30, 30.5)), ((14.5, 27, 1.5), (16.5, 30, 30.5))):
        m.append(pipe(frm, to, EXHAUST))
    for frm, to in (((-1.5, 20, 0), (1.5, 27, 1.5)), ((-1.5, 20, 30.5), (1.5, 27, 32)),
                    ((-16, 20, 14.5), (-14.5, 27, 17.5)), ((14.5, 20, 14.5), (16, 27, 17.5))):
        m.append(pipe(frm, to, RUBBER))
    # Stack: gunmetal shell in three steps, with hazard and chrome bands.
    m += cyl("y", 0, 16, 14, 26, 46, GUNMETAL)
    m += cyl("y", 0, 16, 12.5, 46, 60, OLIVE)
    m += cyl("y", 0, 16, 11, 60, 70, GUNMETAL)
    for y, r, texture in ((36, 14.4, HAZARD), (44, 14.4, CHROME), (55, 12.9, CHROME), (65, 11.4, HAZARD)):
        m += cyl("y", 0, 16, r, y, y + 1, texture)
    # Grated charging deck, hopper, safety rails and an exhaust stack.
    m.append(box((-12, 70, 4), (12, 71.5, 28), {"*": GUNMETAL, "up": GRILLE}))
    m.append(box((-5, 71.5, 11), (5, 76, 21), {"*": GUNMETAL, "up": "sp_hopper_inside"}))
    m.append(box((-5.5, 76, 10.5), (5.5, 77, 21.5), {"*": HAZARD, "up": "sp_hopper_inside"}))
    for x in (-12, 11):
        for z in (4, 27):
            m.append(box((x, 71.5, z), (x + 1, 78, z + 1), CHROME))
    for frm, to in (((-12, 78, 4), (12, 79, 5)), ((-12, 78, 27), (12, 79, 28)),
                    ((-12, 78, 5), (-11, 79, 27)), ((11, 78, 5), (12, 79, 27))):
        m.append(box(frm, to, HAZARD))
    m += cyl("y", 7.5, 22, 1.5, 71.5, 79.5, EXHAUST, "sp_hopper_inside")
    return m


def capacitor_bank():
    """Two-by-two capacitor bank: an olive cabinet with vent grilles holding two rows of big chrome capacitor cans
    with bakelite caps, copper bus bars, a charge gauge and warning lamp under a hazard-striped header, and a power
    socket on the front of each block (the bank gives power out of its front)."""
    m = [box((-16, 0, 0), (16, 1, 16), GUNMETAL)]
    m.append(box((-15.5, 1, 8), (15.5, 31, 15.5), {"*": OLIVE, "north": GRILLE}))
    m.append(box((-15.5, 1, 1), (-14, 31, 8), {"*": OLIVE, "west": STENCIL}))
    m.append(box((14, 1, 1), (15.5, 31, 8), {"*": OLIVE, "east": STENCIL}))
    for y in (1, 15.5):
        m.append(box((-14, y, 1.5), (14, y + 1, 8), GUNMETAL))
    m.append(box((-16, 31, 0), (16, 32, 16), {"*": GUNMETAL, "north": HAZARD}))
    for x, z in ((-16, 0), (14.5, 0), (-16, 14.5), (14.5, 14.5)):
        m.append(box((x, 1, z), (x + 1.5, 31, z + 1.5), CHROME))
    for y0 in (2, 16.5):
        for cx in (-10.5, -3.5, 3.5, 10.5):
            m += cyl("y", cx, 5, 3, y0, y0 + 8.5, CHROME, BAKELITE)
            m.append(box((cx - 0.5, y0 + 8.5, 4.5), (cx + 0.5, y0 + 10.25, 5.5), CHROME))
        m.append(box((-11.25, y0 + 10.25, 4.5), (11.25, y0 + 10.75, 5.5), "sp_copper"))
    # Charge gauge and warning lamp on a gunmetal board under the header.
    m.append(box((-7, 27.5, 1), (7, 30.75, 2), GUNMETAL))
    m.append(dial("north", (-2, 29.1, 0.5), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (3.5, 29.1, 0.5), 2, texture=LAMP, body=GUNMETAL))
    # Power sockets, one on the front of each block.
    for x in (-8, 8):
        for y in (8, 24):
            m.append(box((x - 2, y - 2, 0.25), (x + 2, y + 2, 1.5), {"*": CHROME, "north": "power_port!"}))
    return m


def steel_tank():
    """A squat storage tank on a two-by-two base: an olive, riveted shell with a stencilled band and hazard stripes,
    a gunmetal roof with a manhole, a sight glass and a red valve wheel at the front, a ladder up the side, and
    chrome-flanged pipe stubs at the middle of every outer face."""
    m = [box((-16, 0, 0), (16, 1, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    m += cyl("y", 0, 16, 15.25, 1, 15, OLIVE, GUNMETAL)
    m += cyl("y", 0, 16, 15.6, 3.5, 4.25, HAZARD)
    m += cyl("y", 0, 16, 15.6, 11, 11.75, GUNMETAL)
    m += cyl("y", 0, 16, 12, 15, 17.5, GUNMETAL)
    m += cyl("y", 0, 16, 7.5, 17.5, 19.5, GUNMETAL, GRILLE)
    m += cyl("y", 0, 16, 2.5, 19.5, 21, CHROME, GUNMETAL)
    # Sight glass and drain valve on the front.
    m.append(box((-1.25, 2.5, 0.25), (1.25, 13.5, 1), {"*": CHROME, "north": "sp_sight_glass!"}))
    m += wheel("z", 4.5, 3, 1.75, 0.25, 0.75, "sp_red_iron", CHROME)
    m.append(box((4, 2.5, 0.75), (5, 3.5, 2), GUNMETAL))
    # Ladder up the west side.
    for z in (12.5, 19.5):
        m.append(box((-15.75, 1, z), (-15.25, 17, z + 0.75), CHROME))
    for y in range(3, 17, 3):
        m.append(box((-16, y, 12.5), (-15.75, y + 0.5, 20.25), CHROME))
    # Flanged stubs so pipes meet the tank at the middle of each outer face.
    for x in (-8, 8):
        m.append(pipe((x - 1.5, 6.5, 0), (x + 1.5, 9.5, 3.5), GUNMETAL))
        m.append(box((x - 2, 6, 0), (x + 2, 10, 0.75), CHROME))
        m.append(pipe((x - 1.5, 6.5, 28.5), (x + 1.5, 9.5, 32), GUNMETAL))
        m.append(box((x - 2, 6, 31.25), (x + 2, 10, 32), CHROME))
    for z in (8, 24):
        m.append(pipe((12.5, 6.5, z - 1.5), (16, 9.5, z + 1.5), GUNMETAL))
        m.append(box((15.25, 6, z - 2), (16, 10, z + 2), CHROME))
        m.append(pipe((-16, 6.5, z - 1.5), (-12.5, 9.5, z + 1.5), GUNMETAL))
        m.append(box((-16, 6, z - 2), (-15.25, 10, z + 2), CHROME))
    return m


def ore_drill():
    """Drilling derrick: a gunmetal lattice tower with hazard-striped frames and crown, the drill string running down
    through a chrome rotary table into the ground, an olive diesel motor with a smoking exhaust stack at the back,
    and a control box with a phosphor gauge and a caged lamp at the front."""
    m = [box((0, 0, 0), (16, 1, 16), {"*": GUNMETAL, "north": HAZARD})]
    # Wellhead casing and the rotary table that turns the drill string.
    m += cyl("y", 8, 8, 2.5, 1, 3, GUNMETAL)
    m += cyl("y", 8, 8, 4.5, 3, 4.25, CHROME, GRILLE)
    m.append(box((6.5, 4.25, 6.5), (9.5, 6, 9.5), GUNMETAL))
    m += cyl("y", 8, 8, 1, 3.5, 30.5, "dp_drill_bit")
    # Lattice tower in three stages, narrowing towards the top, with a frame at each step.
    stages = ((1, 16.5, 1.5), (17.25, 26.5, 3.5), (27.25, 31, 5.5))
    for lo, hi, a in stages:
        for x in (a, 14 - a):
            for z in (a, 14 - a):
                m.append(box((x, lo, z), (x + 1, hi, z + 1), GUNMETAL))
    for y, outer, inner in ((16.5, 1.5, 3.5), (26.5, 3.5, 5.5)):
        width = inner + 1 - outer
        far = 15 - outer
        m.append(box((outer, y, outer), (far, y + 0.75, outer + width), HAZARD))
        m.append(box((outer, y, far - width), (far, y + 0.75, far), HAZARD))
        m.append(box((outer, y, outer + width), (outer + width, y + 0.75, far - width), HAZARD))
        m.append(box((far - width, y, outer + width), (far, y + 0.75, far - width), HAZARD))
    for y, a in ((9, 1.5), (22, 3.5)):  # Mid-stage brace bars on the front and back.
        m.append(box((a + 1, y, a + 0.25), (14 - a, y + 0.5, a + 0.75), CHROME))
        m.append(box((a + 1, y, 14.25 - a), (14 - a, y + 0.5, 14.75 - a), CHROME))
    # Crown block with the pulley wheel.
    m.append(box((5, 31, 5), (11, 32, 11), HAZARD))
    m.append(box((5.75, 32, 7.5), (6.75, 35.5, 8.5), GUNMETAL))
    m.append(box((9.25, 32, 7.5), (10.25, 35.5, 8.5), GUNMETAL))
    m += wheel("x", 34.5, 8, 2.25, 7.25, 8.75, GUNMETAL, CHROME)
    # Diesel motor at the back, driving the table, with its exhaust stack.
    m.append(box((8.5, 1, 10.5), (14, 6.5, 14.5), {"*": OLIVE, "east": GRILLE, "up": STENCIL}))
    m.append(box((8.25, 3.5, 9.5), (9.25, 4.5, 10.5), CHROME))
    m += cyl("y", 10.5, 13, 0.75, 6.5, 12, EXHAUST, "sp_hopper_inside")
    # Control box at the front: gauge and the running lamp.
    m.append(box((2.75, 1, 0.75), (6.25, 8, 3.25), {"*": OLIVE, "north": GUNMETAL}))
    m.append(dial("north", (4.5, 5.75, 0.5), 2.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (4.5, 2.75, 0.5), 1.5, texture=LAMP, body=GUNMETAL))
    # Cable junction boxes in the middle of the other sides.
    m.append(box((0.25, 4, 6), (1.25, 9, 10), {"*": GUNMETAL, "west": CHROME}))
    m.append(box((14.75, 4, 6), (15.75, 9, 10), {"*": GUNMETAL, "east": CHROME}))
    m.append(box((5.5, 1, 14.75), (8, 6, 15.75), {"*": GUNMETAL, "south": CHROME}))
    return m


def pumpjack():
    """A nodding-donkey pumpjack, one block wide, three tall and three long (front = the wellhead, -z). A hazard-
    striped skid carries the wellhead with its valve tree and polished rod, an A-frame samson post, an olive walking
    beam with a curved horse head over the well and its bridle lines, and at the back a gear reducer with crank arms,
    two heavy counterweights, pitman arms up to the equalizer bar, and the diesel motor with its belt guard and
    exhaust stack. A control box with a phosphor gauge and a caged lamp stands by the wellhead."""
    m = [box((1, 0, 0), (15, 2, 48), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    for z in (0.5, 46.5):
        m.append(box((0.5, 0, z), (15.5, 2.5, z + 1), HAZARD))
    # Wellhead: casing flange, valve tree with a red-handled valve, stuffing box and the polished rod.
    m += cyl("y", 8, 6, 3, 2, 3.5, GUNMETAL, CHROME)
    m += cyl("y", 8, 6, 1.75, 3.5, 9, GUNMETAL)
    m.append(box((4, 5.5, 5), (12, 7, 7), GUNMETAL))
    m += cyl("x", 6.25, 6, 1.25, 2.5, 4, CHROME)
    m += cyl("x", 6.25, 6, 1.25, 12, 13.5, CHROME)
    m += wheel("x", 6.25, 6, 2, 1.5, 2.5, "sp_red_iron", CHROME, spokes=False)
    m += cyl("y", 8, 6, 2.25, 9, 10.5, CHROME)
    m.append(box((7.5, 10.5, 5.5), (8.5, 27, 6.5), CHROME))
    # Carrier bar hung from the horse head by two bridle lines.
    m.append(box((5, 27, 5), (11, 28, 7), GUNMETAL))
    for x in (5.5, 9.75):
        m.append(box((x, 28, 5.6), (x + 0.75, 33.5, 6.4), RUBBER))
    # Horse head: a curved plate stepped round the front of the beam, hazard-striped face.
    for y0, y1, z0 in ((33.5, 35, 4), (35, 37, 2.5), (37, 41, 1.5), (41, 43, 2.5), (43, 44.5, 4)):
        m.append(box((5, y0, z0), (11, y1, 9), {"*": OLIVE, "north": HAZARD}))
    # Walking beam: an olive girder with gunmetal flanges along the top of the machine.
    m.append(box((6, 38, 9), (10, 42, 42), {"*": OLIVE, "east": STENCIL, "west": STENCIL}))
    m.append(box((5.5, 42, 9), (10.5, 42.75, 42), GUNMETAL))
    m.append(box((5.5, 37.25, 9), (10.5, 38, 42), GUNMETAL))
    # Samson post: an A-frame of four legs with hazard braces and the saddle bearing at the top.
    for x in (2.5, 12.5):
        for z0, z1 in ((17, 19), (29, 31)):
            m.append(box((x, 2, z0), (x + 1, 34, z1), GUNMETAL))
        m.append(box((x, 12, 19), (x + 1, 13, 29), HAZARD))
        m.append(box((x, 24, 19), (x + 1, 25, 29), HAZARD))
    m.append(box((3.5, 34, 17), (12.5, 35, 31), GUNMETAL))
    m.append(box((5, 35, 21), (11, 37.25, 27), CHROME))
    m += cyl("x", 36, 24, 1.5, 3.5, 12.5, GUNMETAL, CHROME)
    # Gear reducer and crank shaft at the back.
    m.append(box((4, 2, 33), (12, 15, 43), {"*": OLIVE, "up": STENCIL, "south": GRILLE}))
    m += cyl("x", 11, 38, 2, 1, 15, CHROME)
    for x0, x1 in ((1.5, 3.5), (12.5, 14.5)):
        # Crank arm and its counterweight (a heavy gunmetal block with hazard edges).
        m.append(box((x0, 9, 36.5), (x1, 24, 39.5), GUNMETAL))
        m.append(box((x0 - 0.5, 17, 33.5), (x1 + 0.5, 25, 44.5), {"*": GUNMETAL, "up": HAZARD, "down": HAZARD}))
        # Pitman arm from the crank pin up to the equalizer bar.
        m.append(box((x0 + 0.5, 25, 37.5), (x1 - 0.5, 37, 38.5), CHROME))
    m.append(box((1.5, 37, 36.5), (14.5, 38, 40), GUNMETAL))
    # Diesel motor with a belt guard and an exhaust stack.
    m.append(box((3.5, 2, 43.5), (12.5, 9, 47.5), {"*": OLIVE, "east": GRILLE, "west": GRILLE, "up": STENCIL}))
    m.append(box((12.5, 3, 40), (14.5, 12, 46), {"*": GUNMETAL, "east": HAZARD}))
    m += cyl("y", 5.5, 45.5, 0.9, 9, 22, EXHAUST, "sp_hopper_inside")
    # Control box by the wellhead: gauge and the running lamp, with its conduit.
    m.append(box((11.5, 2, 10), (14.5, 10, 14), {"*": OLIVE, "north": GUNMETAL}))
    m.append(dial("north", (13, 7.5, 9.75), 2.25, texture=GAUGE, body=CHROME))
    m.append(dial("north", (13, 4, 9.75), 1.75, texture=LAMP, body=GUNMETAL))
    m.append(box((12.5, 2, 14), (13.5, 3, 34), RUBBER))
    return m


def oil_sand_extractor():
    """Two by two by two hot-water extraction plant. A hazard-striped skid under a big olive separation vessel with a
    stepped cone bottom on gunmetal legs; oil sand rides up a short belt into a hopper on the rim, and the froth of
    crude oil shows in the open top inside a chrome launder. At the back a gunmetal water heater with an exhaust stack
    feeds the vessel through rubber hoses; a control panel with a phosphor gauge and a caged lamp faces the front."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Legs and the cone bottom.
    for x in (-10, 8):
        for z in (6, 24):
            m.append(box((x, 2, z), (x + 2, 9, z + 2), GUNMETAL))
    m += cyl("y", 0, 16, 5, 2, 4, GUNMETAL, CHROME)
    m += cyl("y", 0, 16, 8, 4, 6.5, OLIVE)
    m += cyl("y", 0, 16, 10.5, 6.5, 9, OLIVE)
    # The vessel: olive shell with gunmetal and hazard bands, open at the top with the oil froth showing.
    m += cyl("y", 0, 16, 12, 9, 27, OLIVE, GUNMETAL)
    for y, texture in ((13, GUNMETAL), (20, HAZARD), (25.5, GUNMETAL)):
        m += cyl("y", 0, 16, 12.4, y, y + 1, texture)
    m += cyl("y", 0, 16, 12.75, 27, 28.5, CHROME, GUNMETAL)
    m += cyl("y", 0, 16, 11, 28.5, 28.75, "crude_oil_still")
    # Froth outlet: a chrome pipe from the rim down the right side.
    m.append(box((-14.5, 10, 14.5), (-12.5, 28, 17.5), CHROME))
    # Feed hopper on the rim and the belt that carries oil sand up to it.
    m.append(box((4, 28.5, 10), (11, 31.5, 16), {"*": GUNMETAL, "up": "oil_sand"}))
    m.append(box((10, 2, 1), (15, 4, 12), {"*": GUNMETAL, "up": "oil_sand"}))
    m.append(box((11, 4, 3), (14, 28.5, 5), {"*": RUBBER, "north": "oil_sand"}))
    m.append(box((11, 26, 5), (14, 28.5, 11), {"*": RUBBER, "up": "oil_sand"}))
    # Water heater at the back left with its exhaust stack, and hoses into the vessel.
    m.append(box((-15, 2, 25), (-5, 14, 31), {"*": GUNMETAL, "south": GRILLE, "up": STENCIL}))
    m += cyl("y", -8, 28, 1.25, 14, 31, EXHAUST, "sp_hopper_inside")
    for y in (6, 10):
        m.append(box((-6, y, 22), (-4.5, y + 1.25, 25), RUBBER))
    # Control panel at the front: gauge and the running lamp.
    m.append(box((-12, 2, 0.5), (-4, 13, 3.5), {"*": OLIVE, "north": GUNMETAL}))
    m.append(dial("north", (-8, 9.5, 0.25), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (-8, 5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    return m


def heavy_pump():
    """Heavy pump: a gunmetal skid under an olive pump casing with a ribbed rubber-sealed volute, a chrome motor bell
    on top with an exhaust vent, hazard-striped corner guards and thick chrome flanges on its top and four sides."""
    m = [box((0.5, 0, 0.5), (15.5, 1.5, 15.5), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD, "east": HAZARD,
                                                  "west": HAZARD})]
    m += cyl("y", 8, 8, 5.5, 1.5, 9.5, OLIVE, GUNMETAL)
    for y in (3, 6.5):
        m += cyl("y", 8, 8, 5.9, y, y + 0.75, RUBBER)
    m += cyl("y", 8, 8, 3.75, 9.5, 13, CHROME, GRILLE)
    m += cyl("y", 8, 8, 2, 13, 14.5, GUNMETAL)
    m += cyl("y", 8, 8, 2.75, 14.5, 16, CHROME)
    for frm, to in (((0, 5.5, 5.5), (2.5, 10.5, 10.5)), ((13.5, 5.5, 5.5), (16, 10.5, 10.5)),
                    ((5.5, 5.5, 0), (10.5, 10.5, 2.5)), ((5.5, 5.5, 13.5), (10.5, 10.5, 16))):
        m.append(box(frm, to, GUNMETAL))
    for frm, to in (((0, 5, 5), (0.75, 11, 11)), ((15.25, 5, 5), (16, 11, 11)),
                    ((5, 5, 0), (11, 11, 0.75)), ((5, 5, 15.25), (11, 11, 16))):
        m.append(box(frm, to, CHROME))
    for x, z in ((1, 1), (12.5, 1), (1, 12.5), (12.5, 12.5)):
        m.append(box((x, 1.5, z), (x + 2.5, 5, z + 2.5), HAZARD))
    m.append(dial("north", (8, 12, 4), 2.25, texture=GAUGE, body=CHROME))
    return m


def distillation_tower():
    """A two by two fractionating column seven blocks tall. A hazard-striped skid carries the fired reboiler (glowing
    firebox window, gauge, caged lamp, exhaust stack) at the foot of a banded olive-and-gunmetal column. Grated
    platforms with hazard rails ring it at three heights, a ladder climbs the back, and a chrome draw-off pipe with a
    red valve comes out of the front at each fraction's height: heavy fuel oil at the base, then diesel, naphtha and
    refinery gas at the top, under a domed cap with a vent."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Reboiler at the foot, front left (seen from the front): firebox, gauge and lamp, and its stack.
    m.append(box((1, 2, 0.5), (15, 14, 9), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(box((4, 3, 0.25), (12, 9, 0.5), {"*": GUNMETAL, "north": "sp_window!"}))
    m.append(dial("north", (5, 11.5, 0.25), 2.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 11.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    m += cyl("y", 12.5, 5, 1, 14, 26, EXHAUST, "sp_hopper_inside")
    # The column: olive sections between gunmetal flanges, hazard bands at the platforms.
    for y0, y1, texture in ((2, 30, OLIVE), (30, 34, GUNMETAL), (34, 62, OLIVE), (62, 66, GUNMETAL),
                            (66, 94, OLIVE), (94, 98, GUNMETAL), (98, 106, OLIVE)):
        m += cyl("y", 0, 16, 10, y0, y1, texture)
    for y in (16, 48, 80):
        m += cyl("y", 0, 16, 10.4, y, y + 1, HAZARD)
    # Domed cap and vent.
    m += cyl("y", 0, 16, 8, 106, 108, GUNMETAL)
    m += cyl("y", 0, 16, 5, 108, 110, CHROME)
    m += cyl("y", 0, 16, 1, 110, 116, EXHAUST, "sp_hopper_inside")
    # Platforms: grated decks with hazard rails on the front and sides.
    for y in (31, 63, 95):
        m.append(box((-15.5, y, 0.5), (15.5, y + 1, 31.5), {"*": GUNMETAL, "up": GRILLE, "down": GRILLE}))
        for x in (-15.5, 14.5):
            m.append(box((x, y + 1, 0.5), (x + 1, y + 7, 1.5), GUNMETAL))
            m.append(box((x, y + 1, 30.5), (x + 1, y + 7, 31.5), GUNMETAL))
        m.append(box((-15.5, y + 6, 0.5), (15.5, y + 7, 1.25), HAZARD))
        m.append(box((-15.5, y + 6, 1.25), (-14.75, y + 7, 30.75), HAZARD))
        m.append(box((14.75, y + 6, 1.25), (15.5, y + 7, 30.75), HAZARD))
    # Ladder up the back.
    for x in (-3, 2):
        m.append(box((x, 2, 29.5), (x + 1, 102, 30.5), GUNMETAL))
    for y in range(6, 102, 4):
        m.append(box((-2, y, 29.75), (2, y + 0.75, 30.25), CHROME))
    # Draw-offs out of the front at each fraction's height (block layers 0, 2, 4 and 6), each with a red valve.
    for y in (9, 40, 72, 104):
        m.append(box((-12, y - 2, 0.5), (-8, y + 2, 6.5), CHROME))
        m.append(box((-13, y - 3, 0), (-7, y + 3, 0.5), GUNMETAL))
        m += wheel("z", -10, y + 4.5, 1.5, 2.5, 3.25, "sp_red_iron", CHROME, spokes=False)
    return m


def catalytic_cracker():
    """A two by two fluid catalytic cracker four blocks tall: the slim riser-reactor (front left) and the fat
    regenerator vessel (right), both olive with gunmetal and hazard bands, joined at the top by a chrome crossover
    with cyclone caps. A catalyst hopper and a steam line feed the reactor; the firebox and control panel sit at its
    foot; chrome draw-offs with red valves at the base, two blocks up and at the top give diesel, naphtha and gas."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Feed heater at the foot of the reactor: firebox window, gauge and lamp.
    m.append(box((1, 2, 0.5), (15, 14, 9), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(box((4, 3, 0.25), (12, 9, 0.5), {"*": GUNMETAL, "north": "sp_window!"}))
    m.append(dial("north", (5, 11.5, 0.25), 2.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 11.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Riser-reactor: a slim column behind the heater.
    m += cyl("y", 8, 18, 6, 2, 56, OLIVE, GUNMETAL)
    for y in (14, 30, 46):
        m += cyl("y", 8, 18, 6.4, y, y + 1, HAZARD)
    m += cyl("y", 8, 18, 4, 56, 60, CHROME)
    # Regenerator: a fat vessel on legs on the right.
    for x, z in ((-14, 4), (-4, 4), (-14, 27), (-4, 27)):
        m.append(box((x, 2, z), (x + 2, 10, z + 2), GUNMETAL))
    m += cyl("y", -8, 17, 9, 10, 46, OLIVE, GUNMETAL)
    for y in (20, 36):
        m += cyl("y", -8, 17, 9.4, y, y + 1, HAZARD)
    m += cyl("y", -8, 17, 6, 46, 50, GUNMETAL)
    # Crossover duct between the tops, and two cyclone caps on the regenerator.
    m.append(box((-6, 50, 15.5), (8, 54, 20.5), CHROME))
    m.append(box((-7, 46, 15.5), (-3, 54, 20.5), CHROME))
    for x in (-12, -5):
        m += cyl("y", x, 22, 2, 50, 56, GUNMETAL, CHROME)
    # Catalyst hopper and the steam line into the reactor.
    m.append(box((10, 30, 26), (15, 36, 31), {"*": GUNMETAL, "up": "sp_hopper_inside"}))
    m.append(box((11.5, 24, 22), (13.5, 30, 28), RUBBER))
    m.append(box((14, 14, 16), (15.5, 40, 18), RUBBER))
    # Draw-offs out of the front at the base, two blocks up and at the top (layers 0, 2 and 3), with red valves.
    for y in (9, 40, 57):
        m.append(box((-12, y - 2, 0.5), (-8, y + 2, 6.5), CHROME))
        m.append(box((-13, y - 3, 0), (-7, y + 3, 0.5), GUNMETAL))
        m += wheel("z", -10, y + 4.5, 1.5, 2.5, 3.25, "sp_red_iron", CHROME, spokes=False)
    return m


def vacuum_distillation_unit():
    """A two by two vacuum distillation unit three blocks tall: a squat olive column, wide at the base and stepping in
    towards the top, under a nest of chrome steam ejectors and a condenser drum that keep it under vacuum. A fired
    heater with a glowing window, gauge and lamp stands at its foot, a residue chute drops asphalt binder at the back,
    and a chrome draw-off with a red valve gives lubricant out of the front."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    m.append(box((1, 2, 0.5), (15, 14, 9), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(box((4, 3, 0.25), (12, 9, 0.5), {"*": GUNMETAL, "north": "sp_window!"}))
    m.append(dial("north", (5, 11.5, 0.25), 2.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 11.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # The column: wide below, stepping in.
    m += cyl("y", -2, 18, 12, 2, 18, OLIVE, GUNMETAL)
    m += cyl("y", -2, 18, 12.4, 10, 11, HAZARD)
    m += cyl("y", -2, 18, 10, 18, 30, OLIVE, GUNMETAL)
    m += cyl("y", -2, 18, 10.4, 24, 25, GUNMETAL)
    m += cyl("y", -2, 18, 7, 30, 38, OLIVE, GUNMETAL)
    m += cyl("y", -2, 18, 4, 38, 41, CHROME)
    # Steam ejectors: three chrome nozzles on top, feeding a condenser drum lying across the back.
    for x in (-7, -2, 3):
        m.append(box((x - 0.75, 41, 17), (x + 0.75, 46, 19), CHROME))
        m.append(box((x - 1.25, 44, 19), (x + 1.25, 45.5, 26), CHROME))
    m += cyl("x", 44, 27, 3, -12, 8, GUNMETAL, CHROME)
    # Residue chute at the back: asphalt binder drops out here.
    m.append(box((-6, 2, 29), (2, 8, 32), {"*": GUNMETAL, "south": "sp_hopper_inside"}))
    # Lubricant draw-off on the front with its red valve.
    m.append(box((-12, 7, 0.5), (-8, 11, 6.5), CHROME))
    m.append(box((-13, 6, 0), (-7, 12, 0.5), GUNMETAL))
    m += wheel("z", -10, 13.5, 1.5, 2.5, 3.25, "sp_red_iron", CHROME, spokes=False)
    return m


def catalytic_reformer():
    """Three wide, two tall and two deep: three olive reactor drums in a row, hazard-banded under domed chrome caps,
    joined across the top by a gunmetal header, with a fired heater (glowing window, gauge, lamp, exhaust stack) at
    the front left. Chrome draw-offs with red valves on the front right give gasoline (bottom row) and refinery gas
    (top row)."""
    m = [box((-32, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Fired heater at the front left (the master block).
    m.append(box((1, 2, 0.5), (15, 14, 9), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(box((4, 3, 0.25), (12, 9, 0.5), {"*": GUNMETAL, "north": "sp_window!"}))
    m.append(dial("north", (5, 11.5, 0.25), 2.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 11.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    m += cyl("y", 12.5, 5, 1, 14, 28, EXHAUST, "sp_hopper_inside")
    # Three reactor drums, front to back across the row (centres at x = 8, -8, -24).
    for x in (8, -8, -24):
        y0 = 14 if x == 8 else 2
        m += cyl("y", x, 20, 6, y0, 24, OLIVE, GUNMETAL)
        m += cyl("y", x, 20, 6.4, 18, 19, HAZARD)
        m += cyl("y", x, 20, 4.5, 24, 26, CHROME)
        m += cyl("y", x, 20, 2.5, 26, 27.5, CHROME)
    # The first drum stands on the heater's plinth behind it.
    m.append(box((2, 2, 12), (14, 14, 28), GUNMETAL))
    # Header across the tops, with risers into each drum.
    m.append(box((-27, 28, 18.5), (11, 30.5, 21.5), GUNMETAL))
    for x in (8, -8, -24):
        m.append(box((x - 1, 27.5, 19), (x + 1, 28, 21), CHROME))
    # Draw-offs on the front right, off a product manifold in front of the last drum: gasoline at the bottom,
    # refinery gas in the top row.
    m.append(box((-29.5, 2, 6.5), (-22.5, 27, 14.5), {"*": GUNMETAL, "north": STENCIL}))
    for y in (9, 23):
        m.append(box((-28, y - 2, 0.5), (-24, y + 2, 6.5), CHROME))
        m.append(box((-29, y - 3, 0), (-23, y + 3, 0.5), GUNMETAL))
        m += wheel("z", -26, y + 4.5, 1.5, 2.5, 3.25, "sp_red_iron", CHROME, spokes=False)
    return m


def chemical_mixer():
    """A two by two stirred mixing vessel two blocks tall: an olive tank on a hazard skid with gunmetal bands and a
    sight-glass stripe, a domed lid carrying the agitator's motor and gearbox, a sand hopper and a kelp chute feeding
    the lid, and a control panel with a gauge and caged lamp at the front."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # The vessel, with a sight glass showing the grey mixture.
    m += cyl("y", -1, 18, 11, 2, 24, OLIVE, GUNMETAL)
    for y in (8, 17):
        m += cyl("y", -1, 18, 11.4, y, y + 1, GUNMETAL)
    m.append(box((-2.5, 4, 6.5), (0.5, 22, 7), {"*": CHROME, "north": "fracking_fluid_still"}))
    m += cyl("y", -1, 18, 8, 24, 26, GUNMETAL, CHROME)
    # Agitator drive: motor and gearbox on the lid.
    m.append(box((-5, 26, 14), (3, 30, 22), {"*": OLIVE, "east": GRILLE, "west": GRILLE}))
    m += cyl("y", -1, 18, 2.5, 30, 32, CHROME)
    # Sand hopper (right) and kelp chute (back), into the lid.
    m.append(box((-15, 18, 10), (-9, 28, 16), {"*": GUNMETAL, "up": "sp_hopper_inside"}))
    m.append(box((-11, 24, 15), (-8, 26, 18), RUBBER))
    m.append(box((4, 24, 27), (9, 30, 31), {"*": GUNMETAL, "up": "sp_hopper_inside"}))
    return m


def fracking_rig():
    """A three by three fracking derrick five blocks tall. A hazard-striped skid carries the frac pump and its diesel
    engine (under the drill floor, with twin exhaust stacks), the wellhead with its frac tree of red valves, and a
    control panel by the front left corner. Above, a grated drill floor on gunmetal legs holds the doghouse (crew cabin
    with a lit window) and a lattice derrick narrowing in three stages to a hazard-striped crown block with its sheave,
    the travelling block and kelly hanging down the middle. Chrome draw-offs with red valves give crude oil (base),
    flowback water (one block up) and gas (top)."""
    m = [box((-32, 0, 0), (16, 2, 48), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Control panel at the front left (the master block): gauge and the running lamp.
    m.append(box((2, 2, 0.5), (14, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5.5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (10.5, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Wellhead and frac tree under the middle of the floor.
    m += cyl("y", -8, 24, 3.5, 2, 4, GUNMETAL, CHROME)
    m += cyl("y", -8, 24, 2, 4, 14, CHROME)
    for y in (6, 10):
        m.append(box((-13, y, 23), (-3, y + 2, 25), GUNMETAL))
        m += wheel("x", y + 1, 24, 1.75, -14.5, -13.5, "sp_red_iron", CHROME, spokes=False)
    # Frac pump and its diesel engine under the back of the floor, with twin exhaust stacks past the floor's edge.
    m.append(box((-27, 2, 31), (-11, 12, 45), {"*": OLIVE, "up": STENCIL, "east": GRILLE, "west": GRILLE}))
    m.append(box((-10, 2, 33), (-2, 9, 43), {"*": GUNMETAL, "up": GRILLE}))
    m.append(box((-14, 5, 26), (-10, 8, 33), RUBBER))
    for x in (-30, -26.5):
        m += cyl("y", x, 46, 1, 2, 30, EXHAUST, "sp_hopper_inside")
    # Drill floor on legs.
    for x in (-28, 10):
        for z in (4, 42):
            m.append(box((x, 2, z), (x + 2, 14, z + 2), GUNMETAL))
    m.append(box((-28, 14, 4), (12, 16, 44), {"*": GUNMETAL, "up": GRILLE, "down": GRILLE}))
    m.append(box((-28, 16, 4), (12, 17, 5), HAZARD))
    m.append(box((-28, 16, 43), (12, 17, 44), HAZARD))
    # Doghouse on the floor's front right corner, with a lit window.
    m.append(box((-27, 17, 6), (-17, 28, 16), {"*": OLIVE, "up": STENCIL}))
    m.append(box((-25, 21, 5.75), (-19, 26, 6), {"*": GUNMETAL, "north": "sp_window!"}))
    # Lattice derrick in three stages, narrowing towards the crown.
    stages = ((17, 40, 0), (40, 62, 4), (62, 74, 8))
    for lo, hi, a in stages:
        x0, x1, z0, z1 = -22 + a, 4 - a, 10 + a, 36 - a
        for x in (x0, x1):
            for z in (z0, z1):
                m.append(box((x, lo, z), (x + 2, hi, z + 2), GUNMETAL))
        m.append(box((x0, hi - 1, z0), (x1 + 2, hi, z0 + 2), HAZARD))
        m.append(box((x0, hi - 1, z1), (x1 + 2, hi, z1 + 2), HAZARD))
        m.append(box((x0, hi - 1, z0 + 2), (x0 + 2, hi, z1), HAZARD))
        m.append(box((x1, hi - 1, z0 + 2), (x1 + 2, hi, z1), HAZARD))
        mid = (lo + hi) // 2
        m.append(box((x0 + 2, mid, z0 + 0.5), (x1, mid + 1, z0 + 1.5), CHROME))
        m.append(box((x0 + 2, mid, z1 + 0.5), (x1, mid + 1, z1 + 1.5), CHROME))
    # Crown block with the sheave, and the travelling block and kelly down the middle.
    m.append(box((-14, 74, 18), (-2, 76, 30), HAZARD))
    m += wheel("x", 78, 24, 2.5, -9, -7, GUNMETAL, CHROME)
    m.append(box((-9, 52, 23), (-7, 74, 25), RUBBER))
    m.append(box((-10.5, 46, 21.5), (-5.5, 52, 26.5), {"*": OLIVE, "north": HAZARD}))
    m.append(box((-8.5, 17, 23.5), (-7.5, 46, 24.5), CHROME))
    # Draw-offs out of the front right: crude oil (base), flowback water (one block up), gas (top).
    for y in (9, 24, 70):
        m.append(box((-28, y - 2, 0.5), (-24, y + 2, 6.5), CHROME))
        m.append(box((-29, y - 3, 0), (-23, y + 3, 0.5), GUNMETAL))
        m += wheel("z", -26, y + 4.5, 1.5, 2.5, 3.25, "sp_red_iron", CHROME, spokes=False)
    m.append(box((-28, 26, 6.5), (-24, 70, 8), RUBBER))
    return m


def flowback_treatment_unit():
    """Three wide, one tall and two deep: a filter press (front left, the master block) with a gauge and caged lamp,
    beside two open gunmetal settling basins with hazard-striped rims, murky flowback in the first and cleaner water
    settling in the second, joined by a chrome weir, and a stack of chrome filter plates squeezed by a red-handled
    screw at the back."""
    m = [box((-32, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Filter press control at the front left.
    m.append(box((1, 2, 0.5), (15, 12, 8), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Filter press: a row of chrome plates between gunmetal heads, with its screw handwheel.
    m.append(box((2, 2, 11), (14, 12, 13), GUNMETAL))
    for z in range(13, 27, 2):
        m.append(box((2.5, 3, z), (13.5, 11, z + 1.5), CHROME))
    m.append(box((2, 2, 27), (14, 12, 29), GUNMETAL))
    m.append(box((7, 6, 29), (9, 8, 31), CHROME))
    m += wheel("z", 8, 7, 2.5, 31, 31.75, "sp_red_iron", CHROME)
    # Two settling basins across the rest of the skid.
    for x0, x1, surface in ((-31, -17, "flowback_water_still"), (-15.5, -1.5, "fracking_fluid_still")):
        m.append(box((x0, 2, 1.5), (x1, 11, 30.5), {"*": GUNMETAL, "up": surface}))
        m.append(box((x0 - 0.5, 11, 1), (x1 + 0.5, 12, 2), HAZARD))
        m.append(box((x0 - 0.5, 11, 30), (x1 + 0.5, 12, 31), HAZARD))
        m.append(box((x0 - 0.5, 11, 2), (x0 + 0.5, 12, 30), HAZARD))
        m.append(box((x1 - 0.5, 11, 2), (x1 + 0.5, 12, 30), HAZARD))
    m.append(box((-17.5, 9, 14), (-15, 12, 18), CHROME))
    m.append(box((-1.5, 6, 18), (2, 8, 20), RUBBER))
    return m


def diesel_generator():
    """Three wide, two tall and two deep: an inline six diesel engine on a hazard-striped skid. Olive engine block
    with six chrome rocker covers, an exhaust manifold along its front feeding two sooty stacks, a grilled radiator
    at the right end, a gunmetal alternator drum at the left behind the control panel (the master block, gauge and
    caged lamp), and a red-valved fuel line from the day tank at the back."""
    m = [box((-32, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Control panel at the front left (the master block).
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Alternator drum behind the panel, coupled to the crankshaft.
    m += cyl("x", 11, 18, 7, 2, 15, GUNMETAL, CHROME)
    m.append(box((0, 8, 15), (2, 14, 21), CHROME))
    # Engine block and its six rocker covers.
    m.append(box((-24, 2, 11), (0, 18, 25), {"*": OLIVE, "up": GUNMETAL}))
    for x in range(-23, -1, 4):
        m.append(box((x, 18, 12.5), (x + 3, 21, 23.5), CHROME))
    # Exhaust manifold on the front of the block and two stacks.
    m.append(box((-23, 12, 9), (-1, 15, 11), EXHAUST))
    for x in (-19, -7):
        m.append(box((x - 1, 15, 8.5), (x + 1, 17, 10.5), EXHAUST))
        m += cyl("y", x, 9.5, 1.5, 17, 31, EXHAUST)
    # Radiator at the right end.
    m.append(box((-31, 2, 4), (-26, 26, 28), {"*": GUNMETAL, "east": GRILLE, "west": GRILLE}))
    m.append(box((-31.5, 26, 3.5), (-25.5, 27, 28.5), HAZARD))
    m.append(box((-26, 8, 15), (-24, 12, 19), RUBBER))
    # Day tank and fuel line at the back.
    m.append(box((-22, 2, 27), (-2, 10, 31), {"*": OLIVE, "up": STENCIL}))
    m.append(box((-12, 10, 26), (-10, 14, 28), CHROME))
    m += wheel("z", -11, 15.5, 1.5, 26, 26.75, "sp_red_iron", CHROME, spokes=False)
    return m


def gas_turbine():
    """Four wide, two tall and two deep: a gas turbine on a hazard-striped skid. A grilled air-intake filter house at
    the right end feeds a long gunmetal turbine casing with chrome bands; the hot section rises into a sooty exhaust
    stack, and the shaft drives an alternator drum behind the control panel (the master block, gauge and caged lamp).
    A small olive lubricant tank with a golden sight glass and red valve sits at the back."""
    m = [box((-48, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Control panel at the front left (the master block).
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Alternator drum behind the panel and the coupling to the turbine shaft.
    m += cyl("x", 13, 18, 8, 2, 15, OLIVE, GUNMETAL)
    m += cyl("x", 13, 18, 3, -2, 2, CHROME)
    # Turbine casing: compressor (right, wider), combustor and hot section (left), with chrome bands.
    m += cyl("x", 13, 17, 9, -36, -20, GUNMETAL, CHROME)
    m += cyl("x", 13, 17, 7, -20, -2, GUNMETAL, CHROME)
    for x in (-34, -27, -14, -8):
        m += cyl("x", 13, 17, 9.4 if x < -20 else 7.4, x, x + 1, CHROME)
    # Burner cans around the combustor.
    for z in (8, 26):
        m.append(box((-20, 9, z - 1.5), (-16, 13, z + 1.5), CHROME))
    # Air intake filter house at the right end.
    m.append(box((-47, 2, 2), (-37, 30, 30), {"*": GUNMETAL, "north": GRILLE, "south": GRILLE, "west": GRILLE}))
    m.append(box((-47.5, 30, 1.5), (-36.5, 31, 30.5), HAZARD))
    # Exhaust duct and stack over the hot section.
    m.append(box((-10, 20, 12), (-4, 24, 22), EXHAUST))
    m += cyl("y", -7, 17, 3, 24, 32, EXHAUST)
    # Lubricant tank, sight glass and valve at the back.
    m.append(box((-32, 2, 26.5), (-22, 9, 31), {"*": OLIVE, "up": STENCIL}))
    m.append(box((-28, 3, 31), (-26, 8, 31.25), {"*": CHROME, "south": "lubricant_still"}))
    m.append(box((-23, 9, 27.5), (-21, 11, 29.5), RUBBER))
    m += wheel("y", -22, 28.5, 1.5, 11, 11.75, "sp_red_iron", CHROME, spokes=False)
    return m


def polymerization_reactor():
    """Two by two and three tall: a jacketed olive reactor vessel with gunmetal heating bands, a domed head carrying
    the agitator drive, a gas feed line with a red valve, a chrome sight glass showing the hot gas, and a pellet
    extruder at the base dropping cream pellets into a hopper beside the control panel (the master block)."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Control panel at the front left (the master block).
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Skirt and the reactor vessel with its heating jacket bands.
    m += cyl("y", -1, 18, 9, 2, 8, GUNMETAL)
    m += cyl("y", -1, 18, 11, 8, 38, OLIVE, GUNMETAL)
    for y in (12, 20, 28, 35):
        m += cyl("y", -1, 18, 11.4, y, y + 1.5, GUNMETAL)
    m.append(box((-3, 22, 6.5), (1, 32, 7), {"*": CHROME, "north": "lubricant_still"}))
    # Domed head and agitator drive.
    m += cyl("y", -1, 18, 8, 38, 41, GUNMETAL, CHROME)
    m += cyl("y", -1, 18, 4, 41, 44, OLIVE)
    m.append(box((-4, 44, 15), (2, 48, 21), {"*": OLIVE, "east": GRILLE, "west": GRILLE}))
    # Gas feed line down the right side with its valve.
    m.append(box((-15, 2, 16), (-12, 34, 19), CHROME))
    m.append(box((-13, 32, 16), (-11, 34, 19), CHROME))
    m += wheel("x", 24, 17.5, 1.5, -15.75, -15, "sp_red_iron", CHROME, spokes=False)
    # Pellet extruder and catch hopper at the front.
    m.append(box((3, 12, 8), (9, 16, 14), GUNMETAL))
    m.append(box((4, 12.5, 6.5), (8, 15.5, 8), CHROME))
    m.append(box((3.5, 14, 14), (8.5, 18, 17), RUBBER))
    return m


def diesel_engine():
    """Two wide, two tall and three long: a V8 diesel engine on a hazard-striped skid. Gunmetal crankcase, two olive
    cylinder banks with chrome rocker covers and a gunmetal intake manifold between them, four short sooty exhaust
    stacks, a grilled radiator and the control panel (the master block) at the front, a day tank down the left, and
    a flywheel housing at the back with the chrome output shaft leaving the upper right back block."""
    m = [box((-16, 0, 0), (16, 2, 48), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Control panel (master block, front left) and radiator (front right).
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    m.append(box((-15, 2, 1), (-1, 24, 6), {"*": GUNMETAL, "north": GRILLE, "south": GRILLE}))
    m.append(box((-15.5, 24, 0.5), (-0.5, 25, 6.5), HAZARD))
    m.append(box((-9, 8, 6), (-6, 11, 9), RUBBER))
    # Crankcase and the two cylinder banks.
    m.append(box((-12, 2, 8), (12, 12, 41), {"*": GUNMETAL, "up": OLIVE}))
    for x0, x1 in ((2, 12), (-12, -2)):
        m.append(box((x0, 12, 10), (x1, 21, 39), OLIVE))
        m.append(box((x0 + 1, 21, 11), (x1 - 1, 23, 38), CHROME))
        for z in (14, 22, 30):
            m.append(box((x0 + 2, 23, z), (x1 - 2, 23.5, z + 4), GUNMETAL))
    m.append(box((-2, 12, 12), (2, 20, 37), GUNMETAL))
    # Exhaust stacks off the outer side of each bank.
    for x in (13.5, -13.5):
        for z in (16, 32):
            m.append(box((x - 1.5, 14, z - 1.5), (x + 1.5, 17, z + 1.5), EXHAUST))
            m += cyl("y", x, z, 1.25, 17, 31, EXHAUST)
    # Day tank down the left side, below the stacks.
    m.append(box((12, 2, 20), (15.5, 11, 28), {"*": OLIVE, "up": STENCIL}))
    # Flywheel housing at the back and the output shaft (centre of the upper right back block's back face).
    m += cyl("z", -8, 22, 7.5, 41, 46, GUNMETAL, CHROME)
    m.append(box((-12, 2, 41), (12, 12, 46), GUNMETAL))
    m += cyl("z", -8, 24, 2, 46, 48, CHROME)
    return m


def electrolytic_cell():
    """Three wide, three tall and two deep: an electrolysis house. Three olive cells in a row on a hazard-striped
    skid, each with a chrome lid, rubber-gasketed electrode posts and a sight glass, fed by heavy copper bus bars from a
    rectifier cabinet at the front left (the master block, gauge and caged lamp). Gas headers run the length of the
    back: pale green chlorine at the top, white hydrogen in the middle row, and the lye main along the base, each with
    a red valve where it leaves."""
    m = [box((-32, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Rectifier cabinet (master block) with its gauge, lamp and vents.
    m.append(box((1, 2, 0.5), (15, 28, 14), {"*": OLIVE, "north": GUNMETAL, "east": GRILLE, "up": STENCIL}))
    m.append(dial("north", (5, 22, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 22, 0.25), 2, texture=LAMP, body=GUNMETAL))
    m.append(box((3, 4, 0.25), (13, 14, 0.5), GRILLE))
    # Three cells in a row, each with a lid, electrode posts and a sight glass of brine.
    for x0 in (-31, -21, -11):
        x1 = x0 + 9
        m.append(box((x0, 2, 3), (x1, 20, 22), {"*": OLIVE, "up": GUNMETAL}))
        m.append(box((x0 - 0.5, 20, 2.5), (x1 + 0.5, 22, 22.5), CHROME))
        m.append(box((x0 + 2.5, 6, 2.75), (x0 + 6.5, 16, 3), {"*": CHROME, "north": "brine_still"}))
        for px in (x0 + 1.5, x0 + 6):
            m.append(box((px, 22, 9), (px + 1.5, 26, 10.5), RUBBER))
            m.append(box((px - 0.25, 26, 8.75), (px + 1.75, 27, 10.75), "sp_copper"))
    # Copper bus bars from the rectifier over the cells.
    m.append(box((-31, 27, 9), (1, 28.5, 11), "sp_copper"))
    m.append(box((-31, 27, 13), (1, 28.5, 15), "sp_copper"))
    # Headers along the back: chlorine at the top, hydrogen in the middle, lye at the base.
    for y, cap in ((40, "chlorine_still"), (24, "hydrogen_still"), (8, "lye_still")):
        m.append(box((-31.5, y - 2, 24), (15, y + 2, 28), {"*": CHROME, "east": cap, "west": cap}))
        m += wheel("z", 9, y, 1.5, 28, 28.75, "sp_red_iron", CHROME, spokes=False)
    for x in (-27.5, -17.5, -7.5):
        m.append(box((x, 20, 22), (x + 2, 38, 24), RUBBER))
    # A gunmetal rack at the back carries the headers: three posts, a crown beam and hazard caps.
    for x in (-31.5, -9, 13):
        m.append(box((x, 2, 28), (x + 2.5, 44, 31), GUNMETAL))
        m.append(box((x - 0.25, 44, 27.75), (x + 2.75, 45, 31.25), HAZARD))
    m.append(box((-29, 42, 28.5), (13, 44, 30.5), GUNMETAL))
    return m


def chemical_reactor():
    """Two by two by two: an acid plant. A squat lead-grey reactor vessel with gunmetal bands and a yellow acid sight
    glass, a sulfur burner (a firebrick box with a hopper on top) beside it feeding hot gas through a chrome duct, a
    slim absorption tower at the back with a red valve, and the control panel (the master block) at the front left."""
    m = [box((-16, 0, 0), (16, 2, 32), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    m.append(box((1, 2, 0.5), (15, 12, 6), {"*": OLIVE, "up": STENCIL, "north": GUNMETAL}))
    m.append(dial("north", (5, 8.5, 0.25), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (11, 8.5, 0.25), 2, texture=LAMP, body=GUNMETAL))
    # Reactor vessel (lead-lined: gunmetal shell) with bands and a sight glass of acid.
    m += cyl("y", -2, 19, 10, 2, 22, GUNMETAL, CHROME)
    for y in (6, 13, 20):
        m += cyl("y", -2, 19, 10.4, y, y + 1, OLIVE)
    m.append(box((-4, 8, 8.75), (0, 18, 9.25), {"*": CHROME, "north": "sulfuric_acid_still"}))
    m += cyl("y", -2, 19, 6, 22, 24, GUNMETAL, CHROME)
    # Sulfur burner on the right with its hopper, and the duct into the vessel.
    m.append(box((-15, 2, 2), (-7, 12, 10), {"*": "sp_firebrick", "north": "sp_firebox!"}))
    m.append(box((-14, 12, 3), (-8, 16, 9), {"*": GUNMETAL, "up": "sp_hopper_inside"}))
    m.append(box((-12, 16, 5), (-10, 26, 7), CHROME))
    m.append(box((-12, 24, 7), (-10, 26, 14), CHROME))
    # Absorption tower at the back.
    m += cyl("y", 9, 27, 3, 2, 30, OLIVE, GUNMETAL)
    m.append(box((7, 30, 25), (11, 31, 29), HAZARD))
    m.append(box((4, 14, 25), (6, 16, 27), CHROME))
    m += wheel("x", 15, 26, 1.5, 3.25, 4, "sp_red_iron", CHROME, spokes=False)
    return m


def gas_holder():
    """A Horton sphere: an olive steel ball three blocks across, stencilled round its equator between hazard bands, on
    six gunmetal legs braced to concrete-grey pads, with a chrome ladder up the side, a relief valve and gauge on
    top, and a flanged inlet at the foot of the front and back for pipes."""
    import math
    cx, cz, cy, radius = -8, 24, 26, 20
    m = []
    # Legs on pads, braced in a ring under the sphere.
    for i in range(6):
        angle = math.radians(30 + 60 * i)
        x, z = cx + 15 * math.cos(angle), cz + 15 * math.sin(angle)
        m.append(box((x - 2.5, 0, z - 2.5), (x + 2.5, 1.5, z + 2.5), GUNMETAL))
        m.append(box((x - 1.25, 1.5, z - 1.25), (x + 1.25, 22, z + 1.25), GUNMETAL))
    m += cyl("y", cx, cz, 16.25, 9, 10, GUNMETAL)
    # The sphere, in slices.
    step = 2.5
    y = cy - radius
    while y < cy + radius - 0.01:
        top = min(y + step, cy + radius)
        mid = (y + top) / 2
        r = math.sqrt(max(0.0, radius * radius - (mid - cy) ** 2))
        if r > 1.6:
            texture = STENCIL if abs(mid - cy) < 2 else OLIVE
            m += cyl("y", cx, cz, r, y, top, texture, OLIVE)
        y = top
    for band in (cy - 3.5, cy + 2.5):
        m += cyl("y", cx, cz, radius + 0.3, band, band + 1, HAZARD)
    # Relief valve and gauge on top.
    m += cyl("y", cx, cz, 2.5, cy + radius - 0.5, cy + radius + 1.5, CHROME, GUNMETAL)
    m += cyl("y", cx, cz, 1.25, cy + radius + 1.5, 47.5, GUNMETAL, CHROME)
    m.append(dial("north", (cx, 44, cz - 3), 2, texture=GAUGE, body=CHROME))
    m.append(box((cx - 0.5, 41.5, cz - 3), (cx + 0.5, 44, cz - 2), CHROME))
    # A ladder up the east side.
    for z in (21, 27):
        m.append(box((14.5, 0, z), (15.25, 36, z + 0.75), CHROME))
    for y in range(3, 36, 3):
        m.append(box((14.25, y, 21), (14.5, y + 0.5, 27.75), CHROME))
    # Inlets where pipes meet it, at the foot of the front and back.
    for z0, z1, plate in ((0, 6, (0, 0.75)), (42, 48, (47.25, 48))):
        m.append(pipe((cx - 2, 4, z0), (cx + 2, 8, z1), GUNMETAL))
        m.append(box((cx - 2.75, 3.25, plate[0]), (cx + 2.75, 8.75, plate[1]), CHROME))
    m.append(pipe((cx - 1.5, 8, 4.5), (cx + 1.5, 11, 7.5), GUNMETAL))
    return m


MODELS = {"steel_foundry": steel_foundry(), "capacitor_bank": capacitor_bank(), "steel_tank": steel_tank(),
          "ore_drill": ore_drill(), "pumpjack": pumpjack(),
          "oil_sand_extractor": oil_sand_extractor(), "heavy_pump": heavy_pump(),
          "distillation_tower": distillation_tower(), "catalytic_cracker": catalytic_cracker(),
          "vacuum_distillation_unit": vacuum_distillation_unit(), "catalytic_reformer": catalytic_reformer(),
          "chemical_mixer": chemical_mixer(), "fracking_rig": fracking_rig(),
          "flowback_treatment_unit": flowback_treatment_unit(), "diesel_generator": diesel_generator(),
          "gas_turbine": gas_turbine(),
          "polymerization_reactor": polymerization_reactor(),
          "diesel_engine": diesel_engine(),
          "electrolytic_cell": electrolytic_cell(),
          "chemical_reactor": chemical_reactor(), "gas_holder": gas_holder()}
