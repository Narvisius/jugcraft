"""Models for the electronics tier in the cyan look (see docs/ART_DIRECTION.md, "Electric"): near-black casings with
lit cyan seams, cyan glass, cyan screens and violet conduits, following the owner's high-tech references. Built
with the steampunk helpers; steampunk_models.MODELS takes these after the electric ones. The classic style pack
keeps a plain look.
"""
from steampunk_models import box, cyl, dial

DARK, FRAME, GLASS, CONDUIT = "el_dark", "el_frame", "el_glass", "el_conduit"
CYAN, VIOLET, SCREEN, LAMP, VENT, PORT = "el_glow_cyan", "el_glow_violet", "el_screen_cyan", "el_lamp_on", "el_vent", "el_port"


def crystal_grower():
    """Two blocks tall: a dark control cabinet with a cyan screen, status lamp and vents, under a glass growth chamber
    where a silicon boule hangs from its pull rod over a glowing crucible ring, and a pull head on top. Violet
    conduits run up the back corners from the cabinet to the head; power ports sit on the cabinet's sides."""
    m = [box((0, 0, 0), (16, 1.5, 16), FRAME)]
    # Cabinet.
    m.append(box((1, 1.5, 1), (15, 13, 15), DARK))
    m.append(box((3, 6, 0.5), (13, 11.5, 1), {"*": FRAME, "north": SCREEN + "!"}))
    m.append(dial("north", (4.5, 3.5, 0.5), 1, texture=LAMP, body=FRAME))
    m.append(box((7, 2.5, 0.5), (13, 4.5, 1), {"*": FRAME, "north": VENT + "!"}))
    m.append(box((1.5, 12.5, 0.6), (14.5, 13, 1), CYAN))
    for x0, x1, face in ((0.25, 1, "west"), (15, 15.75, "east")):
        m.append(box((x0, 4.5, 6), (x1, 9.5, 10), {"*": FRAME, face: PORT + "!"}))
    m.append(box((0.5, 13, 0.5), (15.5, 14.5, 15.5), {"*": FRAME, "up": DARK}))
    # Growth chamber: corner posts, glass on the back and sides, a bezel round the open front.
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.append(box((x, 14.5, z), (x + 2, 27, z + 2), FRAME))
    m.append(box((3, 14.5, 14), (13, 27, 14.5), GLASS))
    m.append(box((1.5, 14.5, 3), (2, 27, 13), GLASS))
    m.append(box((14, 14.5, 3), (14.5, 27, 13), GLASS))
    for y0 in (14.5, 26):
        m.append(box((3, y0, 0.75), (13, y0 + 1, 1.5), FRAME))
    # Crucible with its heater ring, the boule and the pull rod.
    m += cyl("y", 8, 8, 3.5, 14.5, 16.5, FRAME)
    m += cyl("y", 8, 8, 3.75, 15.25, 15.75, CYAN)
    m += cyl("y", 8, 8, 2.75, 18, 23.5, "el_boule")
    m += cyl("y", 8, 8, 1.25, 17, 18, "el_boule")
    m += cyl("y", 8, 8, 0.5, 23.5, 27, FRAME)
    # Pull head with a vented top.
    m.append(box((0.5, 27, 0.5), (15.5, 29, 15.5), FRAME))
    m.append(box((2, 29, 2), (14, 31.5, 14), DARK))
    m.append(box((4.5, 31.5, 4.5), (11.5, 32, 11.5), {"*": FRAME, "up": VENT + "!"}))
    m.append(box((2, 28, 0.25), (14, 28.5, 0.5), CYAN))
    # Violet conduits up the back corners.
    for x in (3.5, 11.5):
        m.append(box((x, 13, 15), (x + 1, 29, 16), CONDUIT))
    return m


def lithography_station():
    """Three wide, two tall, two deep. On the left, a cleanroom: dark walls with a long cyan glass window that glows
    while the stepper inside works, a pass-through hatch, and a filter unit on the roof. On the right, the operator's
    desk: a keyboard on the desk and a bank of four cyan monitors on a stand. Violet and cyan conduits run along the
    top from the filter unit to the monitor stand; power ports on the outer sides."""
    m = [box((-32, 0, 0), (16, 1.5, 32), FRAME)]
    # Cleanroom: walls, posts, the glass window and the hatch.
    m.append(box((-31, 1.5, 6), (-2, 27, 31), DARK))
    for x in (-32, -3):
        m.append(box((x, 1.5, 4), (x + 2, 28, 6), FRAME))
    m.append(box((-30, 1.5, 4.5), (-3, 5, 6), DARK))
    m.append(box((-30, 6, 5.5), (-3, 24, 6), GLASS))
    m.append(box((-30, 5, 5), (-3, 6, 6), FRAME))
    m.append(box((-30, 24, 5), (-3, 25, 6), FRAME))
    m.append(box((-30, 25, 4.5), (-3, 28, 6), DARK))
    m.append(box((-30, 25.5, 4.4), (-3, 26, 4.5), CYAN))
    m.append(box((-28, 1.5, 3), (-20, 4.5, 4.5), {"*": FRAME, "north": VENT + "!"}))
    m.append(dial("north", (-6, 3, 4.5), 0.75, texture=LAMP, body=FRAME))
    # Filter unit on the roof.
    m.append(box((-31.5, 27, 4), (-1.5, 28.5, 31.5), FRAME))
    m.append(box((-29, 28.5, 8), (-5, 31, 28), {"*": DARK, "up": VENT}))
    # Operator's desk.
    m.append(box((-1.5, 1.5, 10), (15.5, 10, 30), DARK))
    m.append(box((0.5, 1.5, 3), (2.5, 10, 10), FRAME))
    m.append(box((12.5, 1.5, 3), (14.5, 10, 10), FRAME))
    m.append(box((-1.5, 10, 2), (15.5, 11.5, 30), {"*": FRAME, "up": DARK}))
    m.append(box((1.5, 11.5, 3), (13.5, 12, 8.5), {"*": FRAME, "up": "el_keyboard"}))
    m.append(box((-1.25, 10.5, 1.75), (15.25, 11, 2), CYAN))
    # Monitor bank: a stand with four cyan screens.
    m.append(box((6, 11.5, 15), (8, 14, 17), FRAME))
    m.append(box((-0.5, 14, 13), (14.5, 29.5, 15.5), FRAME))
    for x0 in (0.25, 7.25):
        for y0 in (14.75, 22.25):
            m.append(box((x0, y0, 12.5), (x0 + 6.5, y0 + 6.5, 13), {"*": FRAME, "north": SCREEN + "!"}))
    # Conduits along the top: violet from the filter unit, cyan back to the stand.
    m.append(box((-29, 29, 29), (13, 30.5, 30.5), CONDUIT))
    m.append(box((12, 14, 29), (13.5, 29, 30.5), CONDUIT))
    m.append(box((-29, 28.5, 26), (14, 29, 26.5), VIOLET))
    # Power ports on the outer sides.
    m.append(box((-32, 8, 14), (-31, 14, 20), {"*": FRAME, "west": PORT + "!"}))
    m.append(box((15.5, 4, 18), (16, 9, 24), {"*": FRAME, "east": PORT + "!"}))
    return m


def network_terminal():
    """A beige retro computer (one of the owner's references): a desktop case with two drive bays and a power lamp,
    a CRT monitor on top showing the network's cyan readout, a keyboard in front, and power ports on its sides where
    cables meet it."""
    beige = "rt_beige"
    m = [box((1, 0, 5), (15, 5, 15.5), {"*": beige, "north": "rt_floppy"})]
    # The CRT: a deep cabinet tapering to the back, with the screen set into its bezel.
    m.append(box((2.5, 5, 5.5), (13.5, 14.5, 13), beige))
    m.append(box((4, 6, 13), (12, 13, 15), beige))
    m.append(box((3.5, 6, 5), (12.5, 13.5, 5.5), {"*": beige, "north": "el_screen_cyan_on!"}))
    m.append(dial("north", (11.5, 5.75, 5), 0.4, texture=LAMP, body=beige))
    # The keyboard on the desk in front.
    m.append(box((2, 0, 0.75), (14, 1.25, 4.25), {"*": beige, "up": "rt_keys"}))
    # Power ports where cables meet it.
    for x0, x1, face in ((0.25, 1, "west"), (15, 15.75, "east")):
        m.append(box((x0, 1, 8), (x1, 4, 12), {"*": FRAME, face: PORT + "!"}))
    m.append(box((6, 1, 15.5), (10, 4, 16), {"*": FRAME, "south": PORT + "!"}))
    return m


MODELS = {"crystal_grower": crystal_grower(), "lithography_station": lithography_station()}
# Blocks outside the machine framework (generate_material_data: ELECTRONICS_BLOCKS).
BLOCKS = {"network_terminal": network_terminal()}
