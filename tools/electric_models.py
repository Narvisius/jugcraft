"""Electric-look models for the power gear (see docs/ART_DIRECTION.md), replacing their steampunk and dieselpunk
looks: the battery box, capacitor bank, solar panel and electric pump; and the hydrogen fuel cell, the first new
machine built in this look. (The charging station is in tool_models.py,
the electric motor and dynamo in kinetic_models.py; cables are built in generate_material_data.py.)

Dark graphite casings with bevelled panels, trim posts and vents, lit by glowing mint-green strips (emissive, see
model_writer.EMISSIVE), with green-on-black screens, status lamps and power ports. Same footprints, ports and
connection points as before: a cable or pipe still meets each machine near the middle of each side.
steampunk_models.MODELS takes these in place of its own; the classic style pack is unchanged.
"""
from steampunk_models import box, cyl, dial

CASING, SEAMS, FRAME, GLOW = "el_casing", "el_seams", "el_frame", "el_glow"
VENT, PORT, CELL, SOLAR, HAZARD = "el_vent", "el_port", "el_cell", "el_solar", "el_hazard"
SCREEN, LAMP, LIGHT_BARS = "el_screen_on", "el_lamp_on", "el_light_bars"


def battery_box():
    """A compact power cell: a graphite cabinet on a trim plinth with four corner posts, two panels of glowing charge
    bars either side of the output port on the front (the box gives power out of its front), a glowing strip under the top edge
    of every side, a vented cap with a status lamp, and ports in the middle of the other sides for cables."""
    m = [box((0, 0, 0), (16, 1.5, 16), FRAME)]
    m.append(box((1.5, 1.5, 1.5), (14.5, 13.5, 14.5), CASING))
    for x, z in ((0.75, 0.75), (13.25, 0.75), (0.75, 13.25), (13.25, 13.25)):
        m.append(box((x, 1.5, z), (x + 2, 14.5, z + 2), FRAME))
    # Front: two charge panels of glowing bars and the output port between them.
    for x in (2.5, 10):
        m.append(box((x, 2.5, 1), (x + 3.5, 12, 1.5), {"*": FRAME, "north": LIGHT_BARS + "!"}))
    m.append(box((6.75, 4.5, 0.75), (9.25, 7, 1.5), {"*": FRAME, "north": PORT + "!"}))
    m.append(dial("north", (8, 9.75, 1), 1.75, texture=LAMP, body=FRAME))
    # A glowing strip under the top edge of each side.
    m.append(box((2.75, 12.5, 1.1), (13.25, 13, 1.5), GLOW))
    m.append(box((2.75, 12.5, 14.5), (13.25, 13, 14.9), GLOW))
    m.append(box((1.1, 12.5, 2.75), (1.5, 13, 13.25), GLOW))
    m.append(box((14.5, 12.5, 2.75), (14.9, 13, 13.25), GLOW))
    # Vented cap.
    m.append(box((1, 13.5, 1), (15, 15, 15), {"*": FRAME, "up": SEAMS}))
    m.append(box((4, 15, 4), (12, 15.75, 12), {"*": FRAME, "up": VENT + "!"}))
    # Ports where cables meet the other sides.
    m.append(box((0.25, 5.5, 6), (1.5, 10.5, 10), {"*": FRAME, "west": PORT + "!"}))
    m.append(box((14.5, 5.5, 6), (15.75, 10.5, 10), {"*": FRAME, "east": PORT + "!"}))
    m.append(box((6, 5.5, 14.5), (10, 10.5, 15.75), {"*": FRAME, "south": PORT + "!"}))
    return m


def capacitor_bank():
    """Two wide and two tall: a graphite rack holding four capacitor modules, each with a glowing charge column down
    both sides and a power socket in the middle (the bank gives power out of its front), trim posts at the corners,
    a header with a status screen and lamp, and vented side panels."""
    m = [box((-16, 0, 0), (16, 1, 16), FRAME)]
    m.append(box((-15, 1, 5), (15, 31, 15), SEAMS))
    for x, z in ((-16, 0), (14, 0), (-16, 14), (14, 14)):
        m.append(box((x, 1, z), (x + 2, 31, z + 2), FRAME))
    m.append(box((-15.5, 1, 2), (-14, 29.5, 14), {"*": FRAME, "west": VENT}))
    m.append(box((14, 1, 2), (15.5, 29.5, 14), {"*": FRAME, "east": VENT}))
    # Four modules, one in front of each block, with glowing charge columns and a socket in the middle.
    for x0 in (-14, 1.5):
        for y0 in (1.5, 15.5):
            x1, y1 = x0 + 12.5, y0 + 13
            m.append(box((x0, y0, 1.5), (x1, y1, 5), {"*": FRAME, "north": CASING}))
            for gx in (x0 + 1.25, x1 - 2.25):
                m.append(box((gx, y0 + 1.5, 1.1), (gx + 1, y1 - 1.5, 1.5), GLOW))
    for x in (-8, 8):
        for y in (8, 22):
            m.append(box((x - 2, y - 2, 0.25), (x + 2, y + 2, 1.5), {"*": FRAME, "north": PORT + "!"}))
    # Header with a status screen and lamp.
    m.append(box((-16, 29, 0.5), (16, 32, 16), {"*": FRAME, "up": SEAMS}))
    m.append(box((-7, 29.5, 0.25), (5, 31.5, 0.5), {"*": FRAME, "north": SCREEN + "!"}))
    m.append(dial("north", (8, 30.5, 0.25), 1.75, texture=LAMP, body=FRAME))
    m.append(box((-15, 28.5, 0.5), (15, 29, 1.5), GLOW))
    return m


def solar_panel():
    """A tracking solar collector: a graphite pedestal on a trim base with ports out to each side for cables, a
    glowing ring round the column, and a tilted panel of dark cells in a graphite frame with a green status edge."""
    m = [box((2, 0, 2), (14, 1.5, 14), FRAME)]
    m += cyl("y", 8, 8, 4, 1.5, 2.5, CASING)
    m += cyl("y", 8, 8, 1.75, 2.5, 7, FRAME)
    m += cyl("y", 8, 8, 2, 4, 4.5, GLOW)
    m.append(box((5, 6.5, 5), (11, 9, 11), CASING))
    # Arms out to each side, with ports where cables meet them.
    for frm, to in (((0.75, 7.25, 7), (5, 8.75, 9)), ((11, 7.25, 7), (15.25, 8.75, 9)),
                    ((7, 7.25, 0.75), (9, 8.75, 5)), ((7, 7.25, 11), (9, 8.75, 15.25))):
        m.append(box(frm, to, FRAME))
    for frm, to, face in (((0, 6.75, 6.5), (0.75, 9.25, 9.5), "west"), ((15.25, 6.75, 6.5), (16, 9.25, 9.5), "east"),
                          ((6.5, 6.75, 0), (9.5, 9.25, 0.75), "north"), ((6.5, 6.75, 15.25), (9.5, 9.25, 16), "south")):
        m.append(box(frm, to, {"*": FRAME, face: PORT + "!"}))
    tilt = ("x", 22.5, (8, 10, 8))
    m.append(box((1, 9.5, 1), (15, 10.5, 15), {"*": FRAME, "up": SOLAR, "down": CASING}, rotation=tilt))
    m.append(box((0.5, 9.25, 0.5), (15.5, 9.5, 15.5), FRAME, rotation=tilt))
    m.append(box((1.5, 10.5, 14), (14.5, 10.75, 14.5), GLOW, rotation=tilt))
    return m


def electric_pump():
    """A sealed graphite pump: a round motor housing with glowing bands on a trim plinth, a vented head, a small
    status screen, and flanged outlets in the middle of every side and the top where pipes meet it."""
    m = [box((1, 0, 1), (15, 1.5, 15), FRAME)]
    m += cyl("y", 8, 8, 4.25, 1.5, 12, CASING, FRAME)
    for y in (3, 9.5):
        m += cyl("y", 8, 8, 4.5, y, y + 0.75, GLOW)
    m += cyl("y", 8, 8, 3.25, 12, 13.5, FRAME)
    m.append(box((4.25, 13.5, 4.25), (11.75, 14.5, 11.75), {"*": FRAME, "up": VENT + "!"}))
    m += cyl("y", 8, 8, 1.5, 14.5, 15.25, CASING)
    m += cyl("y", 8, 8, 2.5, 15.25, 16, FRAME)
    # Outlet pipes and flanges where pipes connect (the pump pushes out of its top and sides).
    for frm, to in (((0.75, 6.5, 6.5), (4, 9.5, 9.5)), ((12, 6.5, 6.5), (15.25, 9.5, 9.5)),
                    ((6.5, 6.5, 0.75), (9.5, 9.5, 4)), ((6.5, 6.5, 12), (9.5, 9.5, 15.25))):
        m.append(box(frm, to, CASING))
    for frm, to in (((0, 5.5, 5.5), (0.75, 10.5, 10.5)), ((15.25, 5.5, 5.5), (16, 10.5, 10.5)),
                    ((5.5, 5.5, 0), (10.5, 10.5, 0.75)), ((5.5, 5.5, 15.25), (10.5, 10.5, 16))):
        m.append(box(frm, to, FRAME))
    m.append(box((10.5, 10, 2.75), (13.5, 12.5, 3.75), {"*": FRAME, "north": SCREEN + "!"}))
    return m


def fuel_cell():
    """A hydrogen fuel cell: a stack of graphite cell plates between trim end plates, with glowing seams between the
    plates, a hydrogen inlet on top, a status screen on the front that lights while it runs, and power ports in the
    middle of the other sides for cables."""
    m = [box((0, 0, 0), (16, 1.5, 16), FRAME)]
    # End plates and the stack of cells between them, with a glowing seam between each pair.
    m.append(box((1, 1.5, 1.5), (15, 13, 3.5), FRAME))
    m.append(box((1, 1.5, 12.5), (15, 13, 14.5), FRAME))
    for z in range(4, 12, 2):
        m.append(box((1.5, 2, z), (14.5, 12.5, z + 1.5), CASING))
        m.append(box((1.75, 2.5, z + 1.5), (14.25, 12, z + 2), GLOW))
    # Tie rods along the stack.
    for x, y in ((2, 3), (14, 3), (2, 11.5), (14, 11.5)):
        m.append(box((x - 0.75, y - 0.75, 0.75), (x + 0.75, y + 0.75, 15.25), FRAME))
    # Hydrogen inlet on top, status screen on the front.
    m.append(box((4, 13, 5), (12, 14, 11), {"*": FRAME, "up": VENT + "!"}))
    m.append(box((6.5, 14, 6.5), (9.5, 16, 9.5), {"*": CASING, "up": PORT + "!"}))
    m.append(box((4, 4, 0.75), (12, 10, 1.5), {"*": FRAME, "north": "el_screen!"}))
    # Ports where cables meet it.
    m.append(box((0.25, 5.5, 6), (1, 10.5, 10), {"*": FRAME, "west": PORT + "!"}))
    m.append(box((15, 5.5, 6), (15.75, 10.5, 10), {"*": FRAME, "east": PORT + "!"}))
    m.append(box((6, 5.5, 14.5), (10, 10.5, 15.75), {"*": FRAME, "south": PORT + "!"}))
    return m


def lithium_battery_bank():
    """Three wide, two tall, one deep: a graphite rack of six battery modules, one in front of each block, each with
    a column of glowing charge bars either side of a power socket (the bank gives power out of its front). Trim posts
    at the corners and dividers between the modules, a header with a status screen, lamps and a hazard edge, vented
    side panels with glowing strips and ports where cables meet them, and vents on the back."""
    m = [box((-32, 0, 0), (16, 1.5, 16), FRAME)]
    m.append(box((-31, 1.5, 6), (15, 30, 15), SEAMS))
    for x, z in ((-32, 0), (14, 0), (-32, 14), (14, 14)):
        m.append(box((x, 1.5, z), (x + 2, 30, z + 2), FRAME))
    for x in (-17, -1):
        m.append(box((x, 1.5, 0.5), (x + 2, 30, 6), FRAME))
    m.append(box((-30, 15, 0.75), (14, 16.5, 6), FRAME))
    # Six modules, one in front of each block: charge bars either side of a socket.
    for x0 in (-30, -14, 2):
        x1 = x0 + 12
        for y0, y1 in ((2, 14.5), (17, 29.5)):
            m.append(box((x0, y0, 1.5), (x1, y1, 6), {"*": FRAME, "north": CASING}))
            for bx in (x0 + 0.75, x1 - 3.5):
                m.append(box((bx, y0 + 1.5, 1.1), (bx + 2.75, y1 - 1.5, 1.5), {"*": FRAME, "north": LIGHT_BARS + "!"}))
            cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
            m.append(box((cx - 2, cy - 2, 0.75), (cx + 2, cy + 2, 1.5), {"*": FRAME, "north": PORT + "!"}))
    # Header with a status screen, two lamps and a hazard edge.
    m.append(box((-32, 30, 0.5), (16, 32, 16), {"*": FRAME, "up": SEAMS}))
    m.append(box((-14, 30.25, 0.25), (-2, 31.75, 0.5), {"*": FRAME, "north": SCREEN + "!"}))
    for x in (-24, 8):
        m.append(dial("north", (x, 31, 0.25), 0.75, texture=LAMP, body=FRAME))
    m.append(box((-30, 29.6, 0.5), (14, 30, 1.5), {"*": FRAME, "north": HAZARD}))
    # Vented side panels with a glowing strip and a port where cables meet them.
    for x0, x1, face, out in ((-31.5, -30, "west", (-32, -31.5)), (14, 15.5, "east", (15.5, 16))):
        m.append(box((x0, 1.5, 2), (x1, 30, 14), {"*": FRAME, face: VENT}))
        m.append(box((out[0], 27, 3), (out[1], 27.5, 13), GLOW))
        m.append(box((out[0], 13, 6), (out[1], 19, 10), {"*": FRAME, face: PORT + "!"}))
    # Vents on the back.
    for y in (4, 18):
        m.append(box((-24, y, 15), (8, y + 8, 15.75), {"*": FRAME, "south": VENT}))
    return m


def advanced_solar_panel():
    """The owner's reference: a white pedestal on a graphite foot with a green-lit ring and power ports, a dark stripe
    up the front of the column, a white yoke on top carrying the axle, and two large wings of deep blue cells in
    graphite frames spread over the 3x3 layer above, tilted towards the sun."""
    white = "el_white"
    m = [box((1, 0, 1), (15, 1.5, 15), FRAME)]
    m.append(box((2.5, 1.5, 2.5), (13.5, 3.5, 13.5), CASING))
    m.append(box((2.25, 2.25, 2.25), (13.75, 2.75, 13.75), GLOW))
    # Ports where cables meet the foot.
    for frm, to, face in (((0.25, 1, 6), (2.5, 4, 10), "west"), ((13.5, 1, 6), (15.75, 4, 10), "east"),
                          ((6, 1, 0.25), (10, 4, 2.5), "north"), ((6, 1, 13.5), (10, 4, 15.75), "south")):
        m.append(box(frm, to, {"*": FRAME, face: PORT + "!"}))
    # The column with its dark stripe, and the yoke.
    m.append(box((5, 3.5, 5), (11, 20, 11), white))
    m.append(box((7, 4, 4.75), (9, 19, 5), FRAME))
    m.append(box((7.25, 4.5, 4.6), (8.75, 18.5, 4.75), GLOW))
    m.append(box((3.5, 18, 5.5), (12.5, 21, 10.5), white))
    for x0 in (3, 11.5):
        m.append(box((x0, 21, 6.5), (x0 + 1.5, 24.5, 9.5), white))
    # The axle and the two wings of cells, tilted together about it.
    m += cyl("x", 23, 8, 0.9, -15.5, 31.5, FRAME)
    tilt = ("x", 22.5, (8, 23, 8))
    for x0, x1 in ((-15.5, 2.5), (13.5, 31.5)):
        # Each wing in tiles no larger than a block, so the cells keep their size instead of stretching.
        xs = (x0, (x0 + x1) / 2, x1)
        zs = (-14, -14 + 44 / 3, -14 + 88 / 3, 30)
        for i in range(2):
            for j in range(3):
                m.append(box((xs[i], 24, zs[j]), (xs[i + 1], 25.25, zs[j + 1]),
                             {"*": FRAME, "up": "el_solar_large", "down": CASING}, rotation=tilt))
        m.append(box((x0 + 0.5, 23.25, -13), (x1 - 0.5, 24, 29), CASING, rotation=tilt))
    return m


def advanced_engine():
    """The owner's reference engine: two blocks long, graphite. A plinth with a row of recessed squares, a block with
    a sloping cylinder bank in ribbed steel carrying four cylinder heads with white caps, tall side pylons with slit vents and a port,
    two front ports with an orange and a cyan indicator, and the output shaft out of the back of its right block."""
    dark = "el_dark"
    m = [box((-16, 0, 0.5), (16, 2, 16), FRAME)]
    for x in range(-15, 15, 4):
        m.append(box((x + 0.5, 0.5, 0.25), (x + 3, 1.5, 0.5), dark))
    # Engine block and the cylinder bank, sloping up from the front, in light ribbed steel.
    m.append(box((-13, 2, 2), (13, 9, 14.5), {"*": CASING, "north": SEAMS}))
    m.append(box((-12.5, 8.5, 3.5), (12.5, 11.5, 13.5), {"*": "el_ribbed", "east": CASING, "west": CASING},
                 rotation=("x", -22.5, (0, 10, 8.5))))
    m.append(box((-12.5, 9, 9), (12.5, 13, 14.5), {"*": CASING, "up": SEAMS}))
    for x in (-9.5, -3.2, 3.2, 9.5):
        m += cyl("y", x, 8.5, 1.4, 11.5, 15.5, dark, FRAME)
        m.append(box((x - 2.4, 13.25, 6.25), (x + 2.4, 14.25, 10.75), {"*": "el_white", "down": CASING}))
    # Side pylons with slit vents, louvres and a port.
    for x0, x1, out, face in ((-15.5, -13, (-16, -15.5), "west"), (13, 15.5, (15.5, 16), "east")):
        m.append(box((x0, 2, 1), (x1, 15.5, 15), CASING))
        m.append(box((x0 + 0.5, 15.5, 1.5), (x1 - 0.5, 16, 14.5), FRAME))
        m.append(box((x0 + 0.75, 5, 0.75), (x1 - 0.75, 13, 1), dark))
        m.append(box((x0 + 0.25, 2.25, 0.75), (x1 - 0.25, 4.5, 1), {"*": FRAME, "north": VENT + "!"}))
        m.append(box((out[0], 6, 5.5), (out[1], 11, 10.5), {"*": FRAME, face: PORT + "!"}))
    # Front ports with indicators.
    for x, lamp in ((-7, "dp_lamp_on"), (5, "el_glow_cyan")):
        m.append(box((x - 2.5, 2.5, 1.25), (x + 2.5, 8.5, 2), {"*": FRAME, "north": CASING}))
        m.append(box((x - 1.25, 3.25, 1), (x + 1.25, 6, 1.25), dark))
        m.append(dial("north", (x, 7.25, 1), 1.25, texture=lamp, body=FRAME))
    # The output shaft out of the back of the master (right-hand) block.
    m.append(box((5.5, 5.5, 14.5), (10.5, 10.5, 15.25), FRAME))
    m.append(box((6, 6, 15.25), (10, 10, 16), "iron_shaft"))
    return m


MODELS = {"battery_box": battery_box(), "capacitor_bank": capacitor_bank(), "solar_panel": solar_panel(),
          "electric_pump": electric_pump(), "fuel_cell": fuel_cell(),
          "lithium_battery_bank": lithium_battery_bank(), "advanced_solar_panel": advanced_solar_panel(),
          "advanced_engine": advanced_engine()}
