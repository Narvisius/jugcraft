"""Dieselpunk models for the powered tools (3D item models) and the charging station (a 2-tall block).

Built from the same box helpers as the machines (steampunk_models), with the dieselpunk textures
(dieselpunk_textures, names "dp_*"): olive paint, gunmetal, chrome, hazard stripes, bakelite grips.
The tools are modelled upright (the business end at the top, +y), centred on x = 8 and z = 8, and
turned into the hand by their display transforms (DISPLAY below).
"""
from steampunk_models import box, cyl, dial

OLIVE, STENCIL, GUNMETAL, CHROME = "dp_olive", "dp_olive_stencil", "dp_gunmetal", "dp_chrome"
HAZARD, RUBBER, RIBBED, BAKELITE = "dp_hazard", "dp_rubber", "dp_rubber_ribbed", "dp_bakelite"
GRILLE, EXHAUST = "dp_grille", "dp_exhaust"


def mining_drill():
    """A pistol-grip mining drill: bakelite grip and trigger, an olive motor housing with vent grilles and a hazard
    band, a gauge on the power cell at the back, an exhaust stack, a side handle, a chrome chuck and a stepped,
    fluted bit."""
    m = [box((6, 0, 7.5), (10, 1, 11), RUBBER),
         box((6.5, 1, 8), (9.5, 6.5, 10.5), {"*": BAKELITE, "north": RIBBED, "south": RIBBED}),
         box((7.5, 6, 6.25), (8.5, 7.5, 7.25), CHROME),
         box((7.25, 5.5, 5), (8.75, 6, 7.25), GUNMETAL),
         box((5, 6.5, 4), (11, 13, 12), {"*": OLIVE, "east": GRILLE, "west": GRILLE, "up": STENCIL}),
         box((4.75, 11.25, 3.75), (11.25, 12.25, 12.25), HAZARD),
         box((5.5, 7, 12), (10.5, 12.5, 14.5), GUNMETAL),
         dial("south", (8, 9.75, 14.75), 3.5, texture="dp_gauge", body=CHROME)]
    m += cyl("y", 12.5, 10, 0.9, 8, 15.5, EXHAUST, CHROME)
    m += cyl("x", 11, 7, 0.9, 11, 14.5, BAKELITE, RUBBER)
    m += cyl("y", 8, 8, 2.6, 13, 15, CHROME)
    m += cyl("y", 8, 8, 2.0, 15, 16.5, GUNMETAL, CHROME)
    for r, y0, y1 in ((1.75, 16.5, 19.5), (1.25, 19.5, 22), (0.75, 22, 24)):
        m += cyl("y", 8, 8, r, y0, y1, "dp_drill_bit")
    m.append(box((7.75, 24, 7.75), (8.25, 25, 8.25), CHROME))
    return m


def chainsaw():
    """A chainsaw: a bakelite rear grip under an olive engine with cooling fins, a pull-start cover, a muffler, a
    chrome chain-brake guard, and a long guide bar with the saw chain running round its edges."""
    m = [box((7, 0, 6.5), (9, 4.5, 9.5), {"*": BAKELITE, "north": RIBBED, "south": RIBBED}),
         box((7.5, 3, 5.5), (8.5, 4, 6.5), CHROME),
         box((5, 4.5, 5), (11, 11, 11), {"*": OLIVE, "up": HAZARD, "north": STENCIL})]
    for y in (5.5, 7, 8.5, 10):
        m.append(box((11, y, 5.5), (11.75, y + 0.75, 10.5), GUNMETAL))
    m += cyl("x", 7.75, 8, 2.5, 4.25, 5, GUNMETAL, CHROME)
    m.append(box((8, 5, 11), (10.5, 7.5, 12.5), EXHAUST))
    m.append(box((4.5, 11, 5.5), (11.5, 12, 6.5), CHROME))
    m += cyl("y", 6.5, 9.5, 0.75, 11, 11.75, CHROME)
    # Guide bar and chain.
    m.append(box((7.6, 11, 6.75), (8.4, 23, 9.25), {"*": CHROME, "east": GUNMETAL, "west": GUNMETAL}))
    m.append(box((7.5, 11.5, 6.25), (8.5, 23.5, 6.75), "dp_saw_chain"))
    m.append(box((7.5, 11.5, 9.25), (8.5, 23.5, 9.75), "dp_saw_chain"))
    m.append(box((7.5, 23.5, 6.25), (8.5, 24.25, 9.75), "dp_saw_chain"))
    return m


def rocket_pack():
    """Twin olive fuel tanks with stencilled bands, chrome domes and hazard stripes, on a gunmetal spine with a gauge
    and rubber harness straps; flared exhaust nozzles underneath."""
    m = []
    for x in (4.5, 11.5):
        m += cyl("y", x, 8, 2.75, 2, 13, OLIVE, CHROME)
        m += cyl("y", x, 8, 2.9, 9, 10, HAZARD)
        m += cyl("y", x, 8, 2.0, 13, 14, CHROME)
        m += cyl("y", x, 8, 1.2, 14, 14.75, CHROME, GUNMETAL)
        m += cyl("y", x, 8, 1.5, 0.75, 2, EXHAUST)
        m += cyl("y", x, 8, 2.1, 0, 0.75, GUNMETAL, EXHAUST)
    m.append(box((7, 3, 9), (9, 12, 11.5), GUNMETAL))
    m.append(dial("south", (8, 8.5, 11.75), 2.5, texture="dp_gauge", body=CHROME))
    for y in (4, 11):
        m.append(box((1.5, y, 5), (14.5, y + 0.75, 5.5), RUBBER))
    return m


def charging_station():
    """A two-block-tall charging station: a gunmetal plinth with hazard edging, an olive cabinet with vent grilles, a
    charge gauge and a warning lamp, a chrome cradle that holds the tool at chest height, and a rubber conduit up
    its side. Faces north (the tool hangs on the z = 0 side)."""
    m = [box((0, 0, 0), (16, 2, 16), {"*": GUNMETAL, "north": HAZARD}),
         box((2, 2, 9), (14, 29, 15), {"*": OLIVE, "east": GRILLE, "west": GRILLE, "north": STENCIL}),
         box((1, 29, 7.5), (15, 31.5, 15.5), {"*": GUNMETAL, "north": HAZARD}),
         dial("north", (8, 11, 8.25), 5, texture="dp_gauge", body=CHROME),
         dial("north", (8, 25.5, 8.25), 3, texture="dp_lamp", body=GUNMETAL),
         # Cradle: two chrome arms and a rubber-padded rest.
         box((4, 17, 3.5), (5, 18.5, 9), CHROME),
         box((11, 17, 3.5), (12, 18.5, 9), CHROME),
         box((3.5, 16, 3), (12.5, 17, 4.5), RIBBED),
         # Charging contacts above the cradle.
         box((6.5, 21, 7.75), (9.5, 22.5, 9), CHROME)]
    m += cyl("y", 14.75, 12.5, 1, 2, 29, RUBBER)
    # Terminals where cables meet the lower block (middle of each side).
    m.append(box((0.25, 5, 6), (2, 11, 10), {"*": GUNMETAL, "west": CHROME}))
    m.append(box((14, 5, 6), (15.75, 11, 10), {"*": GUNMETAL, "east": CHROME}))
    m.append(box((6, 5, 15), (10, 11, 15.75), {"*": GUNMETAL, "south": CHROME}))
    return m


ITEMS = {"mining_drill": mining_drill(), "chainsaw": chainsaw(), "rocket_pack": rocket_pack()}
BLOCKS = {"charging_station": charging_station()}
# The lamp lights while the station charges something.
LIT = {"charging_station": ("dp_lamp", "dp_lamp_on")}

HANDHELD = {
    # In the hand, +z is forward: tip the upright tool forward so the bit or bar points away from the player.
    "thirdperson_righthand": {"rotation": [70, 0, 0], "translation": [0, 1, 2], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [70, 0, 0], "translation": [0, 1, 2], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [80, -10, 0], "translation": [-1.5, 4.5, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [80, 10, 0], "translation": [1.5, 4.5, 1], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [20, 30, -40], "translation": [0, -2, 0], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.55, 0.55, 0.55]},
}
PACK = {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
    "gui": {"rotation": [20, 210, 0], "scale": [0.7, 0.7, 0.7]},
    "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 180, 0], "scale": [0.6, 0.6, 0.6]},
}
DISPLAY = {"mining_drill": HANDHELD, "chainsaw": HANDHELD, "rocket_pack": PACK}
