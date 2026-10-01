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


MODELS = {"steel_foundry": steel_foundry(), "capacitor_bank": capacitor_bank(), "steel_tank": steel_tank(),
          "ore_drill": ore_drill()}
