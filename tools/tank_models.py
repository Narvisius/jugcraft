"""Tanks in the owner's reference look (batch 10): light grey panelled bodies with yellow-and-black checker bands,
dark rims and necks, a sight glass and flanged pipe stubs. Replaces the tinplate tank, steel tank and gas holder in
steampunk_models.MODELS; the classic style pack keeps its plain look. Shapes and connection points are unchanged.
"""
from steampunk_models import box, pipe

BODY, CHECKER, RIM, TOP = "tk_body", "tk_checker", "tk_rim", "tk_top"


def fluid_tank():
    """One block: a square tank on a dark base, a checker band near the foot and another round a narrower shoulder,
    a dark rim and neck on top, a sight glass on the front and pipe flanges in the middle of each side."""
    m = [box((1, 0, 1), (15, 1.5, 15), RIM)]
    m.append(box((1.5, 1.5, 1.5), (14.5, 10.5, 14.5), BODY))
    m.append(box((1.25, 2.5, 1.25), (14.75, 5.5, 14.75), {"*": CHECKER + "!", "up": RIM, "down": RIM}))
    m.append(box((2.5, 10.5, 2.5), (13.5, 13, 13.5), BODY))
    m.append(box((2.25, 10.5, 2.25), (13.75, 12.75, 13.75), {"*": CHECKER + "!", "up": RIM, "down": RIM}))
    m.append(box((3, 13, 3), (13, 14.25, 13), {"*": RIM, "up": TOP}))
    m.append(box((5, 14.25, 5), (11, 16, 11), {"*": RIM, "up": TOP}))
    m.append(box((6, 6, 1.25), (10, 9.75, 1.5), {"*": RIM, "north": "sp_sight_glass!"}))
    for frm, to in (((0, 5.5, 6), (1.5, 9.5, 10)), ((14.5, 5.5, 6), (16, 9.5, 10)),
                    ((6, 5.5, 14.5), (10, 9.5, 16))):
        m.append(box(frm, to, RIM))
    return m


def steel_tank():
    """Two by two, one tall: the same tank at four times the size, with two checker bands, a dark lid with a hatch
    neck, a sight glass on the front, a ladder up the west side and pipe stubs in the middle of every outer face."""
    m = [box((-16, 0, 0), (16, 1.5, 32), RIM)]
    m.append(box((-15, 1.5, 1), (15, 13, 31), BODY))
    for y in (2.5, 9.5):
        m.append(box((-15.25, y, 0.75), (15.25, y + 3, 31.25), {"*": CHECKER, "up": RIM, "down": RIM}))
    m.append(box((-14, 13, 2), (14, 15, 30), RIM))
    m.append(box((-5, 15, 11), (5, 17.5, 21), {"*": RIM, "up": TOP}))
    m.append(box((-3, 6, 0.75), (3, 9, 1), {"*": RIM, "north": "sp_sight_glass!"}))
    # Ladder up the west side.
    for z in (12.5, 19.5):
        m.append(box((-15.75, 1.5, z), (-15.25, 15, z + 0.75), "dp_chrome"))
    for y in range(4, 15, 3):
        m.append(box((-16, y, 12.5), (-15.75, y + 0.5, 20.25), "dp_chrome"))
    # Flanged stubs so pipes meet the tank at the middle of each outer face.
    for x in (-8, 8):
        m.append(pipe((x - 1.5, 6.5, 0), (x + 1.5, 9.5, 1), RIM))
        m.append(pipe((x - 1.5, 6.5, 31), (x + 1.5, 9.5, 32), RIM))
    for z in (8, 24):
        m.append(pipe((15, 6.5, z - 1.5), (16, 9.5, z + 1.5), RIM))
    return m


def gas_holder():
    """The gas holder's sphere and legs in the tank colours."""
    from dieselpunk_models import gas_holder as sphere
    return sphere(body="el_white", band="el_white", stripes=CHECKER, frame=RIM)


MODELS = {"fluid_tank": fluid_tank(), "steel_tank": steel_tank(), "gas_holder": gas_holder()}
