"""Models for the electronics tier in the cyan look (see docs/ART_DIRECTION.md, "Electric"): near-black casings with
lit cyan seams, cyan glass, cyan screens and violet conduits, following the owner's high-tech references. Built
with the steampunk helpers; steampunk_models.MODELS takes these after the electric ones. The classic style pack
keeps a plain look.
"""
from steampunk_models import box, cyl, dial

DARK, FRAME, GLASS, CONDUIT = "el_dark", "el_frame", "el_glass", "el_conduit"
CYAN, VIOLET, SCREEN, LAMP, VENT, PORT = "el_glow_cyan", "el_glow_violet", "el_screen_cyan", "el_lamp_on", "el_vent", "el_port"


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


import nuclear as _nuclear  # noqa: E402
MODELS = {"lithography_station": lithography_station(),
          "gas_centrifuge_cascade": _nuclear.gas_centrifuge_cascade_model(), "fuel_fabricator": _nuclear.fuel_fabricator_model()}
# Blocks outside the machine framework (generate_material_data: ELECTRONICS_BLOCKS).
from machine_connections import with_sockets, exterior_ports
BLOCKS = {"network_terminal": with_sockets(network_terminal(), exterior_ports())}
