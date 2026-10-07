"""Model geometry for the graveyard pack's buildings (pack 3, tools/graveyard.py BUILDINGS): the family mausoleum, the
lych gate, the cemetery gateway and the columbarium, and the Bronze Mausoleum Door. Each function returns the elements
of a whole building in pixels, facing north (its front toward -z), its design grid starting at (0, 0, 0): a building
`size` blocks wide, tall and deep fills x 0..16 * width, y 0..16 * height, z 0..16 * depth, and its roof may rise up to
a block above its top row. graveyard_data.py cuts the whole into one model per block (cell) and maps the texture
variables to a stage of weathering.

Texture variables, besides the headstones' (#stone, #top, #relief, #relief_top, #rough, #ivy, #iron, #bronze):
#floor (a chequered marble floor), #glass (leaded stained glass, glowing faintly), #lamp (a sanctuary lamp's red
glass), #lantern (a lantern's lit panes), #oak (weathering oak), #roof (weathering slates), #door_glass (a bronze
door's smoked glass).

Block models may turn an element only by 22.5 or 45 degrees; graveyard_data.py checks every element stays within
-16..32 of the block it is given to.
"""
import math

from graveyard_models import b, edge_box, octagon, rot

# How brightly the stained glass and the lamps' glass glow in the dark (block model light_emission).
GLASS_GLOW = 9
LAMP_GLOW = 15


def lit(element, level):
    element["light_emission"] = level
    return element


def ring(cx, cy, z0, z1, radius, thick, tex, segments=16):
    """A ring round (cx, cy) in the x-y plane, `radius` to its middle and `thick` across, of `segments` (8 or 16) short
    boxes laid along it, each turned to its slope (a multiple of 22.5 degrees)."""
    out = []
    step = 360.0 / segments
    length = 2 * math.pi * radius / segments * 1.12
    for k in range(segments):
        theta = k * step
        x = cx + radius * math.cos(math.radians(theta))
        y = cy + radius * math.sin(math.radians(theta))
        phi = (theta + 90) % 180  # the ring's direction here, 0..180
        if phi <= 45 or phi >= 135:
            spin = phi if phi <= 45 else phi - 180
            lo, hi = (x - length / 2, y - thick / 2, z0), (x + length / 2, y + thick / 2, z1)
        else:
            spin = phi - 90
            lo, hi = (x - thick / 2, y - length / 2, z0), (x + thick / 2, y + length / 2, z1)
        out.append(b(lo, hi, tex, rotation=rot("z", spin, (x, y, (z0 + z1) / 2)) if spin else None))
    return out


def ring_z(cx, cz, y0, y1, radius, thick, tex, segments=16):
    """A flat ring round (cx, cz) in the x-z plane (a wreath lying down, a lamp's rim), from y0 to y1."""
    out = []
    step = 360.0 / segments
    length = 2 * math.pi * radius / segments * 1.12
    for k in range(segments):
        theta = k * step
        x = cx + radius * math.cos(math.radians(theta))
        z = cz + radius * math.sin(math.radians(theta))
        phi = (theta + 90) % 180
        if phi <= 45 or phi >= 135:
            spin = phi if phi <= 45 else phi - 180
            lo, hi = (x - length / 2, y0, z - thick / 2), (x + length / 2, y1, z + thick / 2)
        else:
            spin = phi - 90
            lo, hi = (x - thick / 2, y0, z - length / 2), (x + thick / 2, y1, z + length / 2)
        # Turning about y the other way round: x towards z is a negative angle.
        out.append(b(lo, hi, tex, rotation=rot("y", -spin, (x, (y0 + y1) / 2, z)) if spin else None))
    return out


def slope(x0, y0, x1, y1, z0, z1, thick, tex, pieces, inside=1, z_step=16.0):
    """A sloping slab from (x0, y0) to (x1, y1) (a multiple of 22.5 degrees) `thick` pixels deep on the side `inside`
    (as edge_box), from z0 to z1, laid in `pieces` along its slope and in lengths of `z_step` along z, so that each
    piece goes whole to one block."""
    out = []
    z = z0
    while z < z1 - 1e-6:
        zb = min(z1, z + z_step)
        for i in range(pieces):
            a = (x0 + (x1 - x0) * i / pieces, y0 + (y1 - y0) * i / pieces)
            c = (x0 + (x1 - x0) * (i + 1) / pieces, y0 + (y1 - y0) * (i + 1) / pieces)
            out.append(edge_box(a, c, thick, inside, z, zb, tex, extend=0.15))
        z = zb
    return out


def gable_fill(cx, base, half, rise, z0, z1, tex, inset=0.0, step=1.0):
    """A triangle standing in the x-y plane: `half` pixels each side of `cx` at height `base`, narrowing to a point
    `rise` above it, in horizontal strips `step` pixels tall (the raking cornices cover their stepped ends)."""
    out = []
    y = base
    while y < base + rise - 0.5:
        top = min(y + step, base + rise)
        hw = half * (1 - (top - base) / rise) - inset
        if hw <= 0.3:
            break
        out.append(b((cx - hw, y, z0), (cx + hw, top, z1), tex))
        y = top
    return out


def ivy_trail(x, z, normal, y0, y1, seed=0):
    """A trail of ivy climbing a wall from y0 to y1, its stem against the face at (x, z) facing `normal` ('north',
    'south', 'east', 'west'), wandering a little from side to side, with leaves either side."""
    out = []
    y = y0
    k = seed
    sideways = 0.0
    while y < y1 - 0.5:
        top = min(y1, y + 2.6)
        k += 1
        sideways += (0.7 if (k * 7 + seed) % 3 == 0 else -0.6 if (k * 5 + seed) % 4 == 0 else 0.2)
        sideways = max(-2.5, min(2.5, sideways))
        if normal in ("north", "south"):
            zf = z - 0.6 if normal == "north" else z
            out.append(b((x + sideways - 0.35, y, zf), (x + sideways + 0.35, top, zf + 0.6), "#ivy"))
            lx = x + sideways + (1.0 if k % 2 else -2.2)
            out.append(b((lx, y + 0.8, zf - 0.3 if normal == "north" else zf), (lx + 1.2, y + 2.0, zf + 0.9 if normal == "south" else zf + 0.6), "#ivy"))
        else:
            xf = x - 0.6 if normal == "west" else x
            out.append(b((xf, y, z + sideways - 0.35), (xf + 0.6, top, z + sideways + 0.35), "#ivy"))
            lz = z + sideways + (1.0 if k % 2 else -2.2)
            out.append(b((xf - 0.3 if normal == "west" else xf, y + 0.8, lz), (xf + 0.6 if normal == "west" else xf + 0.9, y + 2.0, lz + 1.2), "#ivy"))
        y = top
    return out


def ivy_clump(x0, z0, x1, z1, height):
    """A low mound of ivy and moss on the ground."""
    return [b((x0, 0, z0), (x1, height, z1), "#ivy"), b((x0 + 0.8, height, z0 + 0.8), (x1 - 0.8, height + 0.8, z1 - 0.8), "#ivy")]


# ---------------------------------------------------------------- columns and mouldings

def tuscan_column(cx, cz, y0, y1, radius=3.2):
    """A Tuscan column standing on (cx, cz) from y0 to y1: a square plinth, a torus, a smooth shaft with a slight
    swell (polished marble), a necking ring, an echinus and a square abacus."""
    out = [b((cx - radius - 0.9, y0, cz - radius - 0.9), (cx + radius + 0.9, y0 + 1.4, cz + radius + 0.9))]
    out += octagon(cx - radius - 0.5, y0 + 1.4, cz - radius - 0.5, cx + radius + 0.5, y0 + 2.4, cz + radius + 0.5, "#stone")
    shaft0, shaft1 = y0 + 2.4, y1 - 4.0
    mid = shaft0 + (shaft1 - shaft0) * 0.45
    out += octagon(cx - radius, shaft0, cz - radius, cx + radius, mid, cz + radius, "#relief")
    out += octagon(cx - radius + 0.2, mid, cz - radius + 0.2, cx + radius - 0.2, shaft1, cz + radius - 0.2, "#relief")
    out += octagon(cx - radius + 0.1, shaft1 - 1.4, cz - radius + 0.1, cx + radius - 0.1, shaft1 - 0.8, cz + radius - 0.1, "#stone")
    out += octagon(cx - radius - 0.4, shaft1, cz - radius - 0.4, cx + radius + 0.4, shaft1 + 1.6, cz + radius + 0.4, "#stone")
    out.append(b((cx - radius - 1.0, shaft1 + 1.6, cz - radius - 1.0), (cx + radius + 1.0, y1, cz + radius + 1.0)))
    return out


def pilaster(x0, x1, z0, z1, y0, y1):
    """A flat pilaster (a column's shadow on a wall) with its base and capital."""
    return [b((x0 - 0.5, y0, z0 - 0.5), (x1 + 0.5, y0 + 2.4, z1 + 0.5)), b((x0, y0 + 2.4, z0), (x1, y1 - 2.4, z1), "#relief"),
            b((x0 - 0.6, y1 - 2.4, z0 - 0.6), (x1 + 0.6, y1, z1 + 0.6))]


def dentils(x0, x1, y0, y1, z0, z1, step=2.4, width=1.4):
    out = []
    x = x0 + 0.4
    while x + width <= x1 - 0.2:
        out.append(b((x, y0, z0), (x + width, y1, z1), "#stone"))
        x += step
    return out


def dentils_z(z0, z1, y0, y1, x0, x1, step=2.4, width=1.4):
    out = []
    z = z0 + 0.4
    while z + width <= z1 - 0.2:
        out.append(b((x0, y0, z), (x1, y1, z + width), "#stone"))
        z += step
    return out


def laurel_wreath(cx, cy, z0, z1, radius):
    """A carved laurel wreath: a ring of leaves, tied at the foot with ribbons falling either side."""
    out = ring(cx, cy, z0, z1, radius, 1.4, "#relief")
    # Leaves standing out of the ring, every 45 degrees.
    for k in range(8):
        theta = 22.5 + 45 * k
        x = cx + (radius + 0.6) * math.cos(math.radians(theta))
        y = cy + (radius + 0.6) * math.sin(math.radians(theta))
        out.append(b((x - 0.7, y - 0.7, z0 - 0.3), (x + 0.7, y + 0.7, z1), "#relief", rotation=rot("z", 45, (x, y, z0))))
    out += [b((cx - 1.0, cy - radius - 1.2, z0 - 0.4), (cx + 1.0, cy - radius + 0.6, z1), "#relief"),
            edge_box((cx - 0.6, cy - radius - 0.6), (cx - 3.0, cy - radius - 3.0), 0.8, 1, z0, z1, "#relief"),
            edge_box((cx + 0.6, cy - radius - 0.6), (cx + 3.0, cy - radius - 3.0), 0.8, -1, z0, z1, "#relief")]
    return out


def candlestick(cx, cz, y0, height):
    """A bronze altar candlestick with a white candle and its flame."""
    out = [b((cx - 1.1, y0, cz - 1.1), (cx + 1.1, y0 + 0.5, cz + 1.1), "#bronze"), b((cx - 0.4, y0 + 0.5, cz - 0.4), (cx + 0.4, y0 + height, cz + 0.4), "#bronze"),
           b((cx - 0.8, y0 + height, cz - 0.8), (cx + 0.8, y0 + height + 0.4, cz + 0.8), "#bronze"),
           b((cx - 0.45, y0 + height + 0.4, cz - 0.45), (cx + 0.45, y0 + height + 3.0, cz + 0.45), "#relief_top")]
    out.append(lit(b((cx - 0.25, y0 + height + 3.0, cz - 0.25), (cx + 0.25, y0 + height + 3.8, cz + 0.25), "#lantern"), LAMP_GLOW))
    return out


# ---------------------------------------------------------------- the family mausoleum (marble)

# The mausoleum's plan (pixels): a portico of four Tuscan columns in front (z 0..16), the cella behind it with walls
# 8 thick at the sides (they hold the crypts), 5 at the front and 6 at the back.
M_W, M_D = 80, 80
M_WALL_TOP = 40
M_ARCHITRAVE = (40, 44)
M_FRIEZE = (44, 52)
M_CORNICE = (52, 56)
M_DOOR = (32, 48, 32)      # x0, x1, height of the doorway
M_SIDE = 8
M_FRONT = (16, 21)
M_BACK = (74, 80)
M_CEILING = (44, 48)
# The crypt fronts on the side walls: three high in each of the two middle bays, each in one block.
M_CRYPT_ROWS = ((1.5, 14.5), (17.0, 30.0), (32.5, 43.0))
M_CRYPT_BAYS = ((33.0, 47.0), (49.0, 63.0))
M_PITCH = math.tan(math.radians(22.5))


def mausoleum_crypt_texts():
    """Where each crypt front's inscription is cut, in design pixels: the west wall's six (facing east into the room)
    then the east wall's six, each bay's three from the top down."""
    out = []
    for wall in ("west", "east"):
        x = M_SIDE + 0.7 if wall == "west" else M_W - M_SIDE - 0.7
        for z0, z1 in M_CRYPT_BAYS:
            for y0, y1 in reversed(M_CRYPT_ROWS):
                out.append({"face": "EAST" if wall == "west" else "WEST", "x": x, "y": (y0 + y1) / 2, "z": (z0 + z1) / 2,
                            "width": 10.0, "height": 6.6, "max_scale": 1 / 44})
    return out


def crypt_front(wall, z0, z1, y0, y1):
    """A crypt front on a side wall's inner face: a polished marble tablet in a moulded frame with a bronze rosette at
    each corner. `wall` is 'west' (the face at x = 8, facing east) or 'east' (x = 72, facing west)."""
    out = []
    face = M_SIDE if wall == "west" else M_W - M_SIDE
    sign = 1 if wall == "west" else -1

    def slab(d0, d1, za, zb, ya, yb, tex):
        xa, xb = face + sign * d0, face + sign * d1
        return b((min(xa, xb), ya, za), (max(xa, xb), yb, zb), tex)

    out.append(slab(0, 0.7, z0 + 1.0, z1 - 1.0, y0 + 1.0, y1 - 1.0, "#relief"))
    out += [slab(0, 1.1, z0, z1, y0, y0 + 1.0, "#stone"), slab(0, 1.1, z0, z1, y1 - 1.0, y1, "#stone"),
            slab(0, 1.1, z0, z0 + 1.0, y0 + 1.0, y1 - 1.0, "#stone"), slab(0, 1.1, z1 - 1.0, z1, y0 + 1.0, y1 - 1.0, "#stone")]
    for za in (z0 + 1.4, z1 - 2.4):
        for ya in (y0 + 1.4, y1 - 2.4):
            out.append(slab(0.7, 1.1, za, za + 1.0, ya, ya + 1.0, "#bronze"))
    return out


def mausoleum():
    """A family mausoleum in white marble, in the Greek temple manner: five blocks square and nearly five tall
    (5 x 5 x 4.6 m). A portico of four Tuscan columns carries the entablature, its frieze cut with the family's name,
    and a pediment with a laurel wreath, a cross on its peak. Behind, the doorway (for a door of the player's own; the
    Bronze Mausoleum Door is made for it) opens into a room with six crypt fronts on each side wall, each with its own
    inscription, a coffered ceiling, a chequered floor, an altar with candlesticks under a stained-glass window, and a
    sanctuary lamp burning on its chain."""
    out = []
    W, D = M_W, M_D
    side, (f0, f1), (k0, k1) = M_SIDE, M_FRONT, M_BACK
    top = M_WALL_TOP
    dx0, dx1, door_h = M_DOOR

    # The platform the portico stands on, a low step all round, and the plinth along the cella's walls.
    out += [b((-1.0, 0, 0.5), (W + 1.0, 1.2, f0), "#stone"), b((-0.4, 1.2, 1.2), (W + 0.4, 1.6, f0), "#relief")]
    out += [b((-1.0, 0, f0), (side + 0.6, 4.0, D + 1.0)), b((W - side - 0.6, 0, f0), (W + 1.0, 4.0, D + 1.0)),
            b((side, 0, k0 - 0.6), (W - side, 4.0, D + 1.0))]
    # Walls.
    out += [b((0, 4.0, f0), (side, top, D)), b((W - side, 4.0, f0), (W, top, D))]
    out += [b((side, 0, f0), (dx0, top, f1)), b((dx1, 0, f0), (W - side, top, f1)), b((dx0, door_h, f0), (dx1, top, f1))]
    win_x0, win_x1, win_y0, win_spring = 33.0, 47.0, 10.0, 29.0
    radius = (win_x1 - win_x0) / 2
    out += [b((side, 4.0, k0), (win_x0, top, k1)), b((win_x1, 4.0, k0), (W - side, top, k1)), b((win_x0, 4.0, k0), (win_x1, win_y0, k1)),
            b((win_x0, win_spring + radius, k0), (win_x1, top, k1))]
    # The window's round head: the wall closes in over it, row by row, and the glass fills the rest.
    y = win_spring
    while y < win_spring + radius - 0.01:
        mid = y + 0.5
        half = math.sqrt(max(0.0, radius ** 2 - (mid - win_spring) ** 2))
        if half < radius - 0.05:
            out += [b((win_x0, y, k0), (40 - half, y + 1, k1)), b((40 + half, y, k0), (win_x1, y + 1, k1))]
        out.append(lit(b((40 - half, y, 76.6), (40 + half, y + 1, 77.4), "#glass"), GLASS_GLOW))
        y += 1
    out.append(lit(b((win_x0, win_y0, 76.6), (win_x1, win_spring, 77.4), "#glass"), GLASS_GLOW))
    # The window's moulded surround, inside and out, and a bronze grille outside.
    for face, z0, z1 in (("in", k0 - 0.8, k0), ("out", k1, k1 + 0.8)):
        out += [b((win_x0 - 1.4, win_y0 - 1.0, z0), (win_x1 + 1.4, win_y0, z1)), b((win_x0 - 1.4, win_y0, z0), (win_x0, win_spring, z1)),
                b((win_x1, win_y0, z0), (win_x1 + 1.4, win_spring, z1))]
        out += [e for e in ring(40, win_spring, z0, z1, radius + 0.7, 1.4, "#stone") if e["from"][1] >= win_spring - 0.8]
        out.append(b((39.0, win_spring + radius, z0 - 0.3), (41.0, win_spring + radius + 2.4, z1 + 0.3), "#relief"))
    for gx in (35.6, 38.2, 40.8, 43.4):
        out.append(b((gx, win_y0, k1 + 0.1), (gx + 0.6, win_spring + math.sqrt(max(0, radius ** 2 - (gx + 0.3 - 40) ** 2)) - 0.4, k1 + 0.7), "#bronze"))
    for gy in (16.0, 23.0):
        out.append(b((win_x0, gy, k1 + 0.1), (win_x1, gy + 0.6, k1 + 0.7), "#bronze"))

    # The doorway's architrave, a cornice hood over it, and a carved panel above.
    out += [b((dx0 - 2.2, 0, f0 - 0.9), (dx0, door_h + 2.2, f0), "#relief"), b((dx1, 0, f0 - 0.9), (dx1 + 2.2, door_h + 2.2, f0), "#relief"),
            b((dx0, door_h, f0 - 0.9), (dx1, door_h + 2.2, f0), "#relief"),
            b((dx0 - 3.6, door_h + 2.2, f0 - 2.2), (dx1 + 3.6, door_h + 3.6, f0)), b((dx0 - 3.0, door_h + 3.6, f0 - 1.6), (dx1 + 3.0, door_h + 4.4, f0))]
    # A winged hourglass carved over the door: time flies.
    out += [b((39.2, 37.0, f0 - 0.7), (40.8, 39.6, f0 - 0.1), "#relief"), b((38.6, 36.6, f0 - 0.8), (41.4, 37.0, f0 - 0.1), "#relief"),
            b((38.6, 39.6, f0 - 0.8), (41.4, 40.0, f0 - 0.1), "#relief")]
    for sign in (-1, 1):
        x0 = 40 + sign * 1.6
        for i, (length, dy) in enumerate(((6.0, 0.0), (5.0, -0.8), (3.8, -1.6))):
            xa, xb = (x0, x0 + sign * length)
            out.append(b((min(xa, xb), 38.6 + dy, f0 - 0.6), (max(xa, xb), 39.4 + dy, f0 - 0.1), "#relief"))

    # On each side wall outside: a string course, and two sunken panels framed by a moulding.
    for x0, x1 in ((-0.6, 0.0), (W, W + 0.6)):
        out.append(b((x0, 19.0, f1), (x1, 20.2, k0 + 0.4), "#stone"))
        for z0, z1 in ((26.0, 44.0), (52.0, 70.0)):
            out += [b((x0, 23.0, z0), (x1, 24.0, z1), "#stone"), b((x0, 35.0, z0), (x1, 36.0, z1), "#stone"),
                    b((x0, 24.0, z0), (x1, 35.0, z0 + 1.0), "#stone"), b((x0, 24.0, z1 - 1.0), (x1, 35.0, z1), "#stone"),
                    b((x0, 6.0, z0), (x1, 7.0, z1), "#stone"), b((x0, 16.0, z0), (x1, 17.0, z1), "#stone"),
                    b((x0, 7.0, z0), (x1, 16.0, z0 + 1.0), "#stone"), b((x0, 7.0, z1 - 1.0), (x1, 16.0, z1), "#stone")]
            # A carved cross in each upper panel.
            xc0, xc1 = (x0 - 0.3, x1) if x0 < 0 else (x0, x1 + 0.3)
            zc = (z0 + z1) / 2
            out += [b((xc0, 25.5, zc - 0.6), (xc1, 33.5, zc + 0.6), "#relief"), b((xc0, 30.0, zc - 2.6), (xc1, 31.2, zc + 2.6), "#relief")]
    # Pilasters (antae) at the cella's corners, front and back.
    for x0, x1 in ((-0.5, side + 0.5), (W - side - 0.5, W + 0.5)):
        out += pilaster(x0, x1, f0 - 0.6, f1 + 0.4, 4.0, top)
        out += pilaster(x0, x1, k0 - 0.4, D + 0.6, 4.0, top)

    # The portico: four Tuscan columns.
    for cx in (8.0, 26.0, 54.0, 72.0):
        out += tuscan_column(cx, 8.0, 1.6, top)
    # The entablature: architrave and frieze over the columns and along every side, the cornice above with dentils.
    a0, a1 = M_ARCHITRAVE
    q0, q1 = M_FRIEZE
    c0, c1 = M_CORNICE
    out += [b((0.5, a0, 3.6), (W - 0.5, a1, 12.4), "#stone"), b((0, a0 + 2.8, 12.4), (W, a1, f1))]
    out += [b((-0.3, a0, f0), (side, a1, D + 0.3)), b((W - side, a0, f0), (W + 0.3, a1, D + 0.3)), b((side, a0, k0), (W - side, a1, D + 0.3))]
    out += [b((0, q0, 3.6), (W, q1, f1), "#relief"), b((-0.1, q0, f1), (side, q1, D + 0.1), "#relief"),
            b((W - side, q0, f1), (W + 0.1, q1, D + 0.1), "#relief"), b((side, q0, k0), (W - side, q1, D + 0.1), "#relief")]
    out += [b((-2.4, c0, 1.0), (W + 2.4, c1, f1)), b((-2.4, c0, f1), (side, c1, D + 2.4)), b((W - side, c0, f1), (W + 2.4, c1, D + 2.4)),
            b((side, c0, k0), (W - side, c1, D + 2.4)), b((-1.6, c0 - 1.0, 2.0), (W + 1.6, c0, f1), "#stone"),
            b((-1.6, c0 - 1.0, f1), (0, c0, D + 1.6)), b((W, c0 - 1.0, f1), (W + 1.6, c0, D + 1.6)), b((0, c0 - 1.0, D), (W, c0, D + 1.6))]
    out += dentils(-1.0, W + 1.0, c0 - 2.6, c0 - 1.0, 2.8, 3.6)
    out += dentils_z(3.0, D + 1.0, c0 - 2.6, c0 - 1.0, -0.8, 0)
    out += dentils_z(3.0, D + 1.0, c0 - 2.6, c0 - 1.0, W, W + 0.8)
    # The portico's coffered ceiling.
    out.append(b((0.5, a1 - 0.4, 12.4), (W - 0.5, a1, f0)))
    # Pediments, front and back: a tympanum between raking cornices, the front one carved with a laurel wreath.
    rise = (W / 2 + 2.4) * M_PITCH
    for front in (True, False):
        zt0, zt1 = (5.2, 9.0) if front else (D - 3.0, D + 0.6)
        out += gable_fill(W / 2, c1, W / 2 + 0.5, (W / 2 + 0.5) * M_PITCH, zt0, zt1, "#stone", inset=0.6)
        zr0, zr1 = (0.6, 6.4) if front else (D - 4.0, D + 2.6)
        out += slope(-2.4, c1, W / 2, c1 + rise, zr0, zr1, 3.0, "#relief", 3, inside=-1, z_step=99)
        out += slope(W / 2, c1 + rise, W + 2.4, c1, zr0, zr1, 3.0, "#relief", 3, inside=-1, z_step=99)
    out += laurel_wreath(W / 2, c1 + 6.4, 4.6, 5.2, 3.6)
    # The roof: marble slabs over a gable running front to back, ribs over their joints, a ridge cap.
    out += slope(-2.6, c1, W / 2, c1 + rise, 0.6, D + 2.6, 2.2, "#top", 3)
    out += slope(W / 2, c1 + rise, W + 2.6, c1, 0.6, D + 2.6, 2.2, "#top", 3)
    z = 4.0
    while z < D:
        for p0, p1 in (((-2.4, c1 + 2.2), (W / 2 - 1.0, c1 + rise + 1.8)), ((W / 2 + 1.0, c1 + rise + 1.8), (W + 2.4, c1 + 2.2))):
            out += [edge_box((p0[0] + (p1[0] - p0[0]) * i / 3, p0[1] + (p1[1] - p0[1]) * i / 3),
                             (p0[0] + (p1[0] - p0[0]) * (i + 1) / 3, p0[1] + (p1[1] - p0[1]) * (i + 1) / 3), 0.8, 1, z, z + 1.2, "#top", extend=0.1)
                    for i in range(3)]
        z += 7.6
    out.append(b((W / 2 - 1.6, c1 + rise + 0.6, 0.6), (W / 2 + 1.6, c1 + rise + 2.8, D + 2.6), "#top"))
    # A cross on the pediment's peak and blocks (acroteria) at its feet.
    peak = c1 + rise + 2.8
    out += [b((W / 2 - 0.8, peak - 1.0, 2.2), (W / 2 + 0.8, min(peak + 3.4, 79.8), 3.8), "#relief_top"),
            b((W / 2 - 2.2, peak + 1.2, 2.2), (W / 2 + 2.2, peak + 2.3, 3.8), "#relief_top")]
    for x0 in (-2.4, W - 0.8):
        out.append(b((x0, c1, 0.6), (x0 + 3.2, c1 + 2.6, 4.2), "#relief_top"))

    # Inside: a chequered floor, the crypt fronts, the altar under the window, a coffered ceiling and the lamp.
    out.append(b((side, 0, f1), (W - side, 0.1, k0), "#floor", faces=("up",)))
    out.append(b((dx0, 0, f0), (dx1, 0.1, f1), "#floor", faces=("up",)))
    for wall in ("west", "east"):
        for z0, z1 in M_CRYPT_BAYS:
            for y0, y1 in M_CRYPT_ROWS:
                out += crypt_front(wall, z0, z1, y0, y1)
        # Plain panels at the ends of each side wall and a skirting.
        face = M_SIDE if wall == "west" else M_W - M_SIDE
        x0, x1 = (face, face + 0.6) if wall == "west" else (face - 0.6, face)
        out.append(b((x0, 0, f1), (x1, 1.2, k0), "#relief"))
    out += [b((26.0, 0, 66.0), (54.0, 9.0, k0), "#stone"), b((25.2, 9.0, 65.2), (54.8, 10.4, k0), "#relief_top"),
            b((38.8, 2.4, 65.6), (41.2, 7.4, 66.0), "#bronze"), b((37.4, 5.2, 65.6), (42.6, 6.2, 66.0), "#bronze")]
    out += candlestick(30.0, 69.5, 10.4, 3.4) + candlestick(50.0, 69.5, 10.4, 3.4)
    out += [b((38.0, 10.4, 69.0), (42.0, 11.4, 72.0), "#bronze"), b((38.6, 11.4, 69.6), (41.4, 14.2, 71.4), "#relief_top")]
    ceiling0, ceiling1 = M_CEILING
    out.append(b((side, ceiling0, f1), (W - side, ceiling1, k0), "#stone", faces=("down", "north", "south", "east", "west")))
    for x in (24.0, 40.0, 56.0):
        out.append(b((x - 0.8, ceiling0 - 1.4, f1), (x + 0.8, ceiling0, k0), "#stone"))
    for zc in (32.0, 48.0, 64.0):
        out.append(b((side, ceiling0 - 1.4, zc - 0.8), (W - side, ceiling0, zc + 0.8), "#stone"))
    # The sanctuary lamp on its chain, over the middle of the room.
    lx, lz = 40.0, 52.0
    out += [b((lx - 0.2, 35.0, lz - 0.2), (lx + 0.2, ceiling0 - 1.4, lz + 0.2), "#bronze")]
    out += octagon(lx - 1.6, 32.0, lz - 1.6, lx + 1.6, 33.0, lz + 1.6, "#bronze") + octagon(lx - 0.8, 31.0, lz - 0.8, lx + 0.8, 32.0, lz + 0.8, "#bronze")
    out += [lit(e, LAMP_GLOW) for e in octagon(lx - 1.2, 33.0, lz - 1.2, lx + 1.2, 35.0, lz + 1.2, "#lamp")]
    out += [b((lx - 1.7, 34.8, lz - 1.7), (lx + 1.7, 35.2, lz + 1.7), "#bronze")]
    return out


def mausoleum_ivy():
    """Ivy up the mausoleum's corners and one column, and clumps at its feet."""
    out = ivy_trail(-0.2, 30.0, "west", 4.0, 34.0, 1) + ivy_trail(80.2, 60.0, "east", 4.0, 30.0, 2)
    out += ivy_trail(12.0, 80.4, "south", 4.0, 36.0, 3) + ivy_trail(66.0, 80.4, "south", 4.0, 22.0, 4)
    out += ivy_trail(8.0, 4.4, "north", 1.6, 26.0, 5)
    out += ivy_clump(-2.4, 18.0, 1.0, 26.0, 1.8) + ivy_clump(79.0, 70.0, 82.4, 79.0, 2.2) + ivy_clump(4.0, 1.0, 12.0, 3.4, 1.4)
    return out


# ---------------------------------------------------------------- the lych gate (oak, slates, rubble walls)

L_W, L_D = 64, 32
L_PLATE = 40.0       # the wall plates' underside
L_TIE = (36.0, 44.5)  # the tie beam carrying the inscription
L_EAVE = (-4.0, 36.0)
L_RIDGE = 72.0


def lych_gate():
    """A lych gate: the roofed gateway of an old churchyard, where a coffin rested on its way in. Oak posts on low
    rubble walls, with benches inside, carry a steep roof of slates, its gables boarded and finished with bargeboards
    and a cross; the front tie beam carries an inscription. The passage between is two blocks wide, for a pair of gates
    of the player's own. 4 x 2 blocks, 4.5 m to the ridge."""
    out = []
    W, D = L_W, L_D
    for left in (False, True):
        part = []
        part += [b((1.0, 0, 0), (10.0, 12.0, D), "#rough"), b((0, 12.0, -0.6), (11.0, 14.4, D + 0.6), "#stone")]
        # The bench along the wall's inner side.
        part += [b((10.0, 8.0, 3.0), (15.6, 9.2, D - 3.0), "#oak")]
        for z in (5.0, D - 6.2):
            part += [b((10.6, 0, z), (14.6, 8.0, z + 1.2), "#oak")]
        # Posts on the coping, a sill beam, balusters and a rail between them.
        for z0 in (0.8, D - 5.8):
            part.append(b((3.0, 14.4, z0), (8.0, L_PLATE, z0 + 5.0), "#oak"))
        part += [b((2.6, 14.4, 0.2), (8.4, 16.4, D - 0.2), "#oak"), b((3.2, 26.0, 5.8), (7.8, 27.6, D - 5.8), "#oak")]
        for z in (9.0, 13.0, 17.0, 21.0, 25.0):
            part.append(b((4.9, 16.4, z - 0.6), (6.1, 26.0, z + 0.6), "#oak"))
        # The wall plate along the top, reaching out to carry the gables.
        part.append(b((2.0, L_PLATE, -4.0), (9.0, 44.5, D + 4.0), "#oak"))
        # Braces from the posts up to the tie beams, front and back.
        for z0, z1 in ((1.6, 5.0), (D - 5.0, D - 1.6)):
            part.append(edge_box((8.0, 28.0), (16.0, 36.0), 1.6, -1, z0, z1, "#oak", extend=0.4))
        out += part if not left else [_mirror(e, W) for e in part]
    t0, t1 = L_TIE
    for z0, z1 in ((0.5, 6.0), (D - 6.0, D - 0.5)):
        out.append(b((2.0, t0, z0), (W - 2.0, t1, z1), "#oak"))
    # The front tie beam's carved board, a moulding over and under it.
    out += [b((12.0, t0 + 1.2, 0.0), (W - 12.0, t1 - 1.0, 0.5), "#oak"), b((10.0, t1 - 1.0, -0.4), (W - 10.0, t1, 0.5), "#oak"),
            b((10.0, t0 + 0.2, -0.4), (W - 10.0, t0 + 1.2, 0.5), "#oak")]
    # King posts, the gables' boarding and their bargeboards, front and back.
    ex, ey = L_EAVE
    rise = W / 2 - ex
    for z0, z1, bz0, bz1 in ((2.2, 3.6, -1.8, 0.4), (D - 3.6, D - 2.2, D - 0.4, D + 1.8)):
        out.append(b((W / 2 - 2.0, t1, z0 - 0.6), (W / 2 + 2.0, L_RIDGE - 2.0, z1 + 0.6), "#oak"))
        x = 4.0
        while x < W - 4.0:
            wide = 3.0
            xm = x + wide / 2
            height = ey + (min(xm, W - xm) - ex) - 3.4
            if height > t1 + 0.5 and not (W / 2 - 2.2 < xm < W / 2 + 2.2):
                out.append(b((x, t1, z0), (x + wide - 0.3, height, z1), "#oak"))
            x += wide
        out += slope(ex, ey, W / 2, ey + rise, bz0, bz1, 3.2, "#oak", 4, inside=-1, z_step=99)
        out += slope(W / 2, ey + rise, W - ex, ey, bz0, bz1, 3.2, "#oak", 4, inside=-1, z_step=99)
        # The pendant under the peak.
        out.append(b((W / 2 - 0.9, L_RIDGE - 9.0, bz0), (W / 2 + 0.9, L_RIDGE - 2.0, bz1), "#oak"))
    # A cross on the front gable's peak.
    out += [b((W / 2 - 0.9, L_RIDGE + 0.8, -1.4), (W / 2 + 0.9, 79.6, 0.4), "#oak"),
            b((W / 2 - 3.0, L_RIDGE + 4.6, -1.4), (W / 2 + 3.0, L_RIDGE + 6.2, 0.4), "#oak")]
    # The roof: slates on both slopes from the eaves to the ridge, over the gables front and back, a ridge cap.
    out += slope(ex - 0.6, ey - 0.6, W / 2, ey + rise, -4.6, D + 4.6, 2.4, "#roof", 4, z_step=13.0)
    out += slope(W / 2, ey + rise, W - ex + 0.6, ey - 0.6, -4.6, D + 4.6, 2.4, "#roof", 4, z_step=13.0)
    out.append(b((W / 2 - 1.8, L_RIDGE + 0.4, -4.6), (W / 2 + 1.8, L_RIDGE + 2.6, D + 4.6), "#roof"))
    return out


def _mirror(e, width):
    """`e` reflected across the middle of a building `width` pixels wide."""
    f, t = e["from"], e["to"]
    copy = {**e, "from": [width - t[0], f[1], f[2]], "to": [width - f[0], t[1], t[2]], "faces": dict(e["faces"])}
    faces = copy["faces"]
    east, west = faces.pop("east", None), faces.pop("west", None)
    if west:
        faces["east"] = west
    if east:
        faces["west"] = east
    if "rotation" in e:
        r = e["rotation"]
        copy["rotation"] = {**r, "origin": [width - r["origin"][0], r["origin"][1], r["origin"][2]],
                            "angle": -r["angle"] if r["axis"] in ("y", "z") else r["angle"]}
    return copy


def lych_gate_ivy():
    return (ivy_trail(1.0, 16.0, "west", 0.0, 12.0, 1) + ivy_trail(63.0, 10.0, "east", 0.0, 12.0, 2)
            + ivy_trail(5.5, 0.8, "north", 14.4, 34.0, 3) + ivy_clump(-1.6, 4.0, 1.4, 14.0, 2.0) + ivy_clump(62.6, 18.0, 65.6, 28.0, 2.4))


# ---------------------------------------------------------------- the cemetery gateway (granite piers, wrought iron)

G_W = 80
G_PIER = 16


def inverted_torch(cx, y0, y1, z):
    """A torch turned down, its flame guttering out: a Victorian sign of a life put out, carved on a polished panel."""
    out = [b((cx - 0.9, y0 + 4.0, z - 0.6), (cx + 0.9, y1, z), "#relief"), b((cx - 1.4, y1 - 2.0, z - 0.8), (cx + 1.4, y1 - 1.2, z), "#relief"),
           b((cx - 1.3, y0 + 4.0, z - 0.8), (cx + 1.3, y0 + 5.0, z), "#relief")]
    out += [b((cx - 1.0, y0 + 1.6, z - 0.7), (cx + 1.0, y0 + 4.0, z), "#relief"), b((cx - 0.5, y0, z - 0.6), (cx + 0.5, y0 + 1.6, z), "#relief"),
            b((cx - 1.8, y0 + 2.4, z - 0.5), (cx - 1.0, y0 + 3.6, z), "#relief"), b((cx + 1.0, y0 + 2.0, z - 0.5), (cx + 1.8, y0 + 3.2, z), "#relief")]
    return out


def lantern(cx, cz, y0):
    """A wrought-iron lantern with lit panes, on a pier's cap."""
    out = [b((cx - 2.6, y0, cz - 2.6), (cx + 2.6, y0 + 1.0, cz + 2.6), "#iron"), b((cx - 1.2, y0 + 1.0, cz - 1.2), (cx + 1.2, y0 + 2.0, cz + 1.2), "#iron")]
    out.append(lit(b((cx - 2.0, y0 + 2.0, cz - 2.0), (cx + 2.0, y0 + 8.0, cz + 2.0), "#lantern"), LAMP_GLOW))
    for sx in (-1, 1):
        for sz in (-1, 1):
            out.append(b((cx + sx * 2.0 - 0.4, y0 + 2.0, cz + sz * 2.0 - 0.4), (cx + sx * 2.0 + 0.4, y0 + 8.0, cz + sz * 2.0 + 0.4), "#iron"))
    out += [b((cx - 2.8, y0 + 8.0, cz - 2.8), (cx + 2.8, y0 + 8.8, cz + 2.8), "#iron"), b((cx - 2.0, y0 + 8.8, cz - 2.0), (cx + 2.0, y0 + 9.8, cz + 2.0), "#iron"),
            b((cx - 1.0, y0 + 9.8, cz - 1.0), (cx + 1.0, y0 + 10.8, cz + 1.0), "#iron"), b((cx - 0.3, y0 + 10.8, cz - 0.3), (cx + 0.3, y0 + 13.0, cz + 0.3), "#iron"),
            b((cx - 0.7, y0 + 12.4, cz - 0.7), (cx + 0.7, y0 + 13.4, cz + 0.7), "#iron")]
    return out


def gate_pier(x0):
    """A granite gate pier on a moulded plinth: rock-faced courses with dressed corners, a polished panel carved with an
    inverted torch, a cornice and a stepped cap with a lantern."""
    out = [b((x0 + 0.4, 0, 0.4), (x0 + 15.6, 4.0, 15.6)), b((x0 + 0.9, 4.0, 0.9), (x0 + 15.1, 5.2, 15.1), "#relief")]
    y = 5.2
    k = 0
    while y < 38.0:
        top = min(38.0, y + 5.4)
        inset = 0.0 if k % 2 == 0 else 0.4
        out.append(b((x0 + 1.4 + inset, y, 1.4 + inset), (x0 + 14.6 - inset, top - 0.35, 14.6 - inset), "#rough"))
        out.append(b((x0 + 1.9, top - 0.35, 1.9), (x0 + 14.1, top, 14.1), "#stone"))
        y = top
        k += 1
    # The panel on the front and back.
    for z0, z1, front in ((0.9, 1.4, True), (14.6, 15.1, False)):
        out.append(b((x0 + 4.0, 12.0, z0), (x0 + 12.0, 32.0, z1), "#relief"))
        if front:
            out += inverted_torch(x0 + 8.0, 14.0, 30.0, z0)
    out += [b((x0 + 0.6, 38.0, 0.6), (x0 + 15.4, 40.0, 15.4)), b((x0 + 0.2, 40.0, 0.2), (x0 + 15.8, 41.4, 15.8), "#relief"),
            b((x0 - 0.4, 41.4, -0.4), (x0 + 16.4, 43.6, 16.4))]
    for k, (inset, height) in enumerate(((1.4, 1.6), (3.0, 1.4), (4.6, 1.2), (5.8, 1.0))):
        y0 = 43.6 + sum(h for _, h in ((1.4, 1.6), (3.0, 1.4), (4.6, 1.2), (5.8, 1.0))[:k])
        out.append(b((x0 + inset, y0, inset), (x0 + 16 - inset, y0 + height, 16 - inset), "#top"))
    out += lantern(x0 + 8.0, 8.0, 48.8)
    return out


def scroll_c(cx, cy, z0, z1, radius, opening):
    """A C-scroll of wrought iron: most of a ring, open towards `opening` ('up', 'down', 'left', 'right')."""
    skip = {"up": (90,), "down": (270,), "left": (180,), "right": (0,)}[opening]
    out = []
    for e, k in zip(ring(cx, cy, z0, z1, radius, 0.6, "#iron", segments=8), range(8)):
        if k * 45 not in skip:
            out.append(e)
    return out


def gateway():
    """A cemetery gateway: two granite piers carrying lanterns, with a wrought-iron overthrow arched between them, the
    cemetery's name in gilt on a plate framed by the arch, scrolls in its spandrels and a ringed cross on its crown. The
    opening is three blocks wide for gates of the player's own (the cemetery gate fits). 5 x 1 blocks, 4.3 m to the
    cross."""
    out = gate_pier(0.0) + gate_pier(G_W - G_PIER)
    z0, z1 = 7.4, 8.6
    x0, x1 = G_PIER, G_W - G_PIER
    mid = G_W / 2
    # A bar from pier to pier under the caps, and the arch springing from it.
    out.append(b((x0, 40.0, z0), (x1, 41.2, z1), "#iron"))
    arch = [(x0, 41.2), (x0 + 10.0, 51.2), (x0 + 17.0, 54.1), (x1 - 17.0, 54.1), (x1 - 10.0, 51.2), (x1, 41.2)]
    for p, q in zip(arch[:-1], arch[1:]):
        out.append(edge_box(p, q, 1.2, 1, z0, z1, "#iron", extend=0.4))
    # The name plate inside the arch, framed, hung from the crown and standing on the bar.
    plate = (x0 + 10.0, 43.0, x1 - 10.0, 49.8)
    out += [b((plate[0], plate[1], z0 - 0.2), (plate[2], plate[3], z1 + 0.2), "#iron"),
            b((plate[0] - 0.6, plate[1] - 0.6, z0 - 0.4), (plate[2] + 0.6, plate[1], z1 + 0.4), "#iron"),
            b((plate[0] - 0.6, plate[3], z0 - 0.4), (plate[2] + 0.6, plate[3] + 0.6, z1 + 0.4), "#iron"),
            b((plate[0] - 0.6, plate[1], z0 - 0.4), (plate[0], plate[3], z1 + 0.4), "#iron"),
            b((plate[2], plate[1], z0 - 0.4), (plate[2] + 0.6, plate[3], z1 + 0.4), "#iron")]
    for x in (plate[0] + 2.0, mid, plate[2] - 2.0):
        out.append(b((x - 0.3, 41.2, z0 + 0.2), (x + 0.3, plate[1] - 0.6, z1 - 0.2), "#iron"))
        out.append(b((x - 0.3, plate[3] + 0.6, z0 + 0.2), (x + 0.3, 54.0 if x == mid else 52.0, z1 - 0.2), "#iron"))
    # Scrolls in the spandrels, uprights by the piers, and twists along the bar.
    out += scroll_c(x0 + 5.6, 44.8, z0 + 0.1, z1 - 0.1, 2.4, "right") + scroll_c(x1 - 5.6, 44.8, z0 + 0.1, z1 - 0.1, 2.4, "left")
    out += scroll_c(x0 + 9.4, 49.6, z0 + 0.1, z1 - 0.1, 1.4, "down") + scroll_c(x1 - 9.4, 49.6, z0 + 0.1, z1 - 0.1, 1.4, "down")
    for x in (x0 + 2.4, x1 - 2.4):
        out.append(b((x - 0.3, 41.2, z0 + 0.2), (x + 0.3, 43.4, z1 - 0.2), "#iron"))
    for x in range(int(x0) + 3, int(x1) - 2, 4):
        out.append(b((x - 0.5, 38.6, z0 + 0.2), (x + 0.5, 39.6, z1 - 0.2), "#iron", rotation=rot("z", 45, (x, 39.1, 8.0))))
        out.append(b((x - 0.2, 39.2, z0 + 0.3), (x + 0.2, 40.0, z1 - 0.3), "#iron"))
    # The crown: scrolls either side of a ringed cross.
    out += scroll_c(mid - 4.2, 57.0, z0 + 0.1, z1 - 0.1, 2.0, "down") + scroll_c(mid + 4.2, 57.0, z0 + 0.1, z1 - 0.1, 2.0, "down")
    out += [b((mid - 0.5, 54.1, z0), (mid + 0.5, 68.0, z1), "#iron"), b((mid - 2.8, 63.0, z0), (mid + 2.8, 64.0, z1), "#iron")]
    out += ring(mid, 63.5, z0 + 0.1, z1 - 0.1, 1.9, 0.5, "#iron", segments=8)
    out.append(b((mid - 0.8, 68.0, z0 - 0.1), (mid + 0.8, 69.0, z1 + 0.1), "#iron"))
    return out


def gateway_ivy():
    return (ivy_trail(15.0, 4.0, "east", 0.0, 34.0, 1) + ivy_trail(1.0, 12.0, "west", 0.0, 22.0, 2)
            + ivy_trail(65.0, 10.0, "west", 0.0, 28.0, 3) + ivy_clump(-1.0, 2.0, 1.6, 10.0, 2.0) + ivy_clump(78.4, 6.0, 81.0, 15.0, 1.6))


# ---------------------------------------------------------------- the columbarium (marble)

C_W = 48
C_ROWS = ((6.5, 15.5), (19.0, 28.0))   # the niches' fronts, bottom row then top row (one block each)
C_NICHE_HALF = 5.2


def columbarium_texts():
    """The frieze's inscription, then each niche's plaque: the top row from the viewer's left, then the bottom row."""
    out = [{"face": "FRONT", "x": C_W / 2, "y": 39.0, "z": 1.9, "width": 38.0, "height": 4.8, "max_scale": 1 / 30}]
    for y0, y1 in reversed(C_ROWS):
        for cx in (40.0, 24.0, 8.0):
            out.append({"face": "FRONT", "x": cx, "y": (y0 + y1) / 2 + 0.6, "z": 3.4, "width": 8.0, "height": 5.6, "max_scale": 1 / 52})
    return out


def columbarium():
    """A columbarium: a wall of marble with six niches for the ashes of the dead, three blocks wide and three high
    (3 x 3.4 m). Each niche is closed by a polished tablet with a bronze rosette at each corner and a little bronze vase,
    and carries its own inscription; pilasters stand between them, and over them a frieze for one more (the family's or
    the cemetery's), a cornice, a pediment with a carved cross, and urns at the ends."""
    out = []
    W = C_W
    out += [b((-0.4, 0, 0.8), (W + 0.4, 4.0, 15.2)), b((0.2, 4.0, 1.4), (W - 0.2, 5.0, 14.6), "#relief")]
    # The wall's body behind the niche fronts, and its face around them (left in front of the niches' recesses).
    out.append(b((0.8, 5.0, 4.0), (W - 0.8, 34.0, 13.4)))
    out.append(b((0.8, 5.0, 13.4), (W - 0.8, 34.0, 14.0), "#relief"))
    face0, face1 = 2.4, 4.0
    rows = [(5.0, C_ROWS[0][0] - 0.8), (C_ROWS[0][1] + 0.8, C_ROWS[1][0] - 0.8), (C_ROWS[1][1] + 0.8, 34.0)]
    for y0, y1 in rows:
        out.append(b((0.8, y0, face0), (W - 0.8, y1, face1)))
    for y0, y1 in C_ROWS:
        cuts = [0.8] + [v for cx in (8.0, 24.0, 40.0) for v in (cx - C_NICHE_HALF - 0.8, cx + C_NICHE_HALF + 0.8)] + [W - 0.8]
        for xa, xb in zip(cuts[::2], cuts[1::2]):
            out.append(b((xa, y0 - 0.8, face0), (xb, y1 + 0.8, face1)))
        for cx in (8.0, 24.0, 40.0):
            out += niche(cx, y0, y1, face0, face1)
    # Pilasters between and at the ends of the niches.
    for x in (0.6, 15.0, 31.0, W - 2.6):
        out += [b((x, 5.0, 1.6), (x + 2.0, 33.2, face0), "#relief"), b((x - 0.4, 32.2, 1.2), (x + 2.4, 34.0, face0))]
    # The entablature: architrave, frieze, cornice.
    out += [b((0.2, 34.0, 1.6), (W - 0.2, 36.2, 14.4)), b((0.4, 36.2, 1.9), (W - 0.4, 42.0, 14.1), "#relief"),
            b((-0.6, 42.0, 0.6), (W + 0.6, 43.4, 15.4)), b((-1.2, 43.4, 0.0), (W + 1.2, 45.0, 16.0), "#top")]
    out += dentils(0.0, W, 41.2, 42.0, 1.1, 1.9, step=2.0, width=1.0)
    # The pediment with its carved cross, a cross on its peak, and urns at the cornice's ends.
    rise = 18.0 * M_PITCH
    out += gable_fill(W / 2, 45.0, 17.0, 17.0 * M_PITCH, 3.0, 13.0, "#stone", inset=0.4)
    out += slope(W / 2 - 18.0, 45.0, W / 2, 45.0 + rise, 2.2, 13.8, 1.6, "#relief", 2, inside=-1, z_step=99)
    out += slope(W / 2, 45.0 + rise, W / 2 + 18.0, 45.0, 2.2, 13.8, 1.6, "#relief", 2, inside=-1, z_step=99)
    out += [b((W / 2 - 0.6, 46.0, 2.6), (W / 2 + 0.6, 51.0, 3.0), "#relief"), b((W / 2 - 1.8, 48.6, 2.6), (W / 2 + 1.8, 49.6, 3.0), "#relief")]
    peak = 45.0 + rise
    out += [b((W / 2 - 0.7, peak - 0.6, 7.3), (W / 2 + 0.7, peak + 6.0, 8.7), "#relief_top"),
            b((W / 2 - 2.2, peak + 3.2, 7.3), (W / 2 + 2.2, peak + 4.4, 8.7), "#relief_top")]
    for cx in (3.0, W - 3.0):
        out += octagon(cx - 2.0, 45.0, 6.0, cx + 2.0, 45.8, 10.0, "#relief_top") + octagon(cx - 1.0, 45.8, 7.0, cx + 1.0, 46.6, 9.0, "#relief_top")
        out += octagon(cx - 1.8, 46.6, 6.2, cx + 1.8, 50.0, 9.8, "#relief_top") + octagon(cx - 1.2, 50.0, 6.8, cx + 1.2, 51.0, 9.2, "#relief_top")
        out += [b((cx - 0.5, 51.0, 7.5), (cx + 0.5, 52.0, 8.5), "#relief_top")]
    # Panels on the back.
    for cx in (8.0, 24.0, 40.0):
        for y0, y1 in ((7.0, 17.0), (19.5, 31.0)):
            out.append(b((cx - 5.6, y0, 14.0), (cx + 5.6, y1, 14.4), "#stone"))
    return out


def niche(cx, y0, y1, z0, z1):
    """One niche: a moulded frame standing out from the wall, the polished tablet closing it, four bronze rosettes and
    a little bronze vase on a bracket below the inscription."""
    h = C_NICHE_HALF
    out = [b((cx - h, y0, 3.4), (cx + h, y1, z1), "#relief")]
    out += [b((cx - h - 0.8, y0 - 0.8, z0 - 0.5), (cx + h + 0.8, y0, z1), "#stone"), b((cx - h - 0.8, y1, z0 - 0.5), (cx + h + 0.8, y1 + 0.8, z1), "#stone"),
            b((cx - h - 0.8, y0, z0 - 0.5), (cx - h, y1, z1), "#stone"), b((cx + h, y0, z0 - 0.5), (cx + h + 0.8, y1, z1), "#stone")]
    for x in (cx - h + 0.5, cx + h - 1.3):
        for y in (y0 + 0.5, y1 - 1.3):
            out.append(b((x, y, 3.0), (x + 0.8, y + 0.8, 3.4), "#bronze"))
    out += [b((cx - 0.9, y0 + 0.4, 2.6), (cx + 0.9, y0 + 0.8, 3.4), "#bronze"), b((cx - 0.5, y0 + 0.8, 2.7), (cx + 0.5, y0 + 2.4, 3.3), "#bronze"),
            b((cx - 0.7, y0 + 2.4, 2.6), (cx + 0.7, y0 + 2.8, 3.4), "#bronze")]
    return out


def columbarium_ivy():
    return ivy_trail(-0.4, 9.0, "west", 0.0, 30.0, 1) + ivy_trail(15.0, 1.0, "north", 5.0, 20.0, 2) + ivy_clump(40.0, 0.2, 47.0, 1.4, 1.6)


# ---------------------------------------------------------------- the Bronze Mausoleum Door

def mausoleum_door():
    """A bronze door, made to look like a pair of narrow leaves: a frame with a meeting stile up the middle, smoked
    glass behind a grille of bars in the upper panels, raised panels below, and a pull either side of the middle on
    both faces. Facing as vanilla's door models do (the leaf from x 0 to 3), the whole 32 pixels tall; it is the same
    whichever way it is hung or swung, so one model serves every hinge."""
    out = []
    t0, t1 = 0.0, 3.0
    out += [b((t0, 0, 0), (t1, 32, 1.6), "#bronze"), b((t0, 0, 14.4), (t1, 32, 16), "#bronze"), b((t0, 0, 7.3), (t1, 32, 8.7), "#bronze"),
            b((t0, 30.4, 0), (t1, 32, 16), "#bronze"), b((t0, 0, 0), (t1, 2.4, 16), "#bronze"), b((t0, 13.2, 0), (t1, 15.0, 16), "#bronze")]
    for z0, z1 in ((1.6, 7.3), (8.7, 14.4)):
        out.append(b((1.2, 15.0, z0), (1.8, 30.4, z1), "#door_glass"))
        out += [b((0.4, 2.4, z0), (2.6, 13.2, z1), "#bronze"), b((0.0, 4.0, z0 + 1.0), (3.0, 11.6, z1 - 1.0), "#bronze")]
        zc = (z0 + z1) / 2
        for z in (zc - 1.8, zc, zc + 1.8):
            out.append(b((0.6, 15.0, z - 0.3), (2.4, 30.4, z + 0.3), "#bronze"))
        for y in (20.0, 25.6):
            out.append(b((0.6, y - 0.3, z0), (2.4, y + 0.3, z1), "#bronze"))
    for z in (6.2, 9.2):
        out += [b((-0.6, 15.0, z), (-0.1, 19.0, z + 0.6), "#bronze"), b((-0.2, 15.0, z), (0, 15.4, z + 0.6), "#bronze"),
                b((-0.2, 18.6, z), (0, 19.0, z + 0.6), "#bronze"),
                b((3.1, 15.0, z), (3.6, 19.0, z + 0.6), "#bronze"), b((3.0, 15.0, z), (3.2, 15.4, z + 0.6), "#bronze"),
                b((3.0, 18.6, z), (3.2, 19.0, z + 0.6), "#bronze")]
    return out
