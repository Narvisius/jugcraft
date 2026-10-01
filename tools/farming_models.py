"""Models for farming blocks outside the machine framework (batch 9): the sprinkler. Built with the steampunk helpers
in the dieselpunk palette, like the crop harvester.
"""
from steampunk_models import box, cyl, pipe

GUNMETAL, OLIVE, CHROME, GRILLE = "dp_gunmetal", "dp_olive", "dp_chrome", "dp_grille"


def sprinkler():
    """A sprinkler on a post: a gunmetal foot with flanged water inlets on every side, an olive water tank, a small
    fertilizer hopper on its back, a chrome riser and a rotor head with two spray arms and nozzles."""
    m = [box((3, 0, 3), (13, 1.5, 13), GUNMETAL)]
    # Inlets at the middle of each side where pipes meet it.
    for frm, to in (((0, 2, 6.5), (3, 5, 9.5)), ((13, 2, 6.5), (16, 5, 9.5)),
                    ((6.5, 2, 0), (9.5, 5, 3)), ((6.5, 2, 13), (9.5, 5, 16))):
        m.append(pipe(frm, to, GUNMETAL))
    for frm, to in (((0, 1.5, 6), (0.75, 5.5, 10)), ((15.25, 1.5, 6), (16, 5.5, 10)),
                    ((6, 1.5, 0), (10, 5.5, 0.75)), ((6, 1.5, 15.25), (10, 5.5, 16))):
        m.append(box(frm, to, CHROME))
    # Water tank and fertilizer hopper.
    m += cyl("y", 8, 8, 4, 1.5, 9, OLIVE, GUNMETAL)
    m += cyl("y", 8, 8, 4.25, 3.5, 4, CHROME)
    m.append(box((9.5, 6, 9.5), (13.5, 9.5, 13.5), {"*": OLIVE, "up": GRILLE}))
    # Riser and rotor head with two spray arms.
    m += cyl("y", 8, 8, 1, 9, 13, CHROME)
    m += cyl("y", 8, 8, 1.75, 13, 14.25, GUNMETAL, CHROME)
    m.append(box((2, 13.75, 7.5), (14, 14.5, 8.5), CHROME))
    for x0 in (1.25, 13.75):
        m.append(box((x0, 13.5, 7.25), (x0 + 1, 15, 8.75), GUNMETAL))
    m.append(box((7.5, 14.25, 7.5), (8.5, 15.5, 8.5), CHROME))
    return m


MODELS = {"sprinkler": sprinkler()}
