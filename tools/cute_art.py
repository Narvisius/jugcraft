"""A clean, cute art style for the creature decorations and their fires, after the owner's reference pictures (the
Frankenstein head, the gargoyle, the skull candle, the eyeball bat): flat colour in two or three tones per material, a lit
top-left edge and a shaded bottom-right one, crisp outlines, chunky shapes and simple faces (big round eyes with a glint
for living creatures; square, dark, pixel-aligned sockets for skulls, the Minecraft way; no nostrils; a neat smile).
Nothing here scatters per-pixel noise: every shade is placed on purpose, so the props read smooth at any distance.

Painters take a Px (tools/flora_art.py) and return nothing; the helpers draw onto one.
"""
import math

from flora_art import pal, shade

# Shared colours. Bone is warm cream; a socket is a soft dark plum rather than black, so faces stay friendly.
BONE = pal("8a7a5c", "b5a684", "d6cba8", "ebe2c4", "f8f3e2")
SOCKET = pal("231a24", "3a2c3a", "54404f")
GLINT = (255, 255, 255)
BLUSH = (240, 150, 150)
FIRE = pal("8e1f0c", "c8400f", "ee7a16", "fbb32c", "ffe27a", "fff8d8")
COAL = pal("1c1414", "2c2020", "3e2e2a")
IRON = pal("16141a", "221f28", "302c38", "433e4e", "5c5668", "7a7488")
STONE = pal("2b2a2e", "3c3b40", "504e55", "66646b", "7e7c83", "98969c")
HORN = pal("5a4630", "7a6244", "9a8058", "b89e72", "d4bc90")


# ---------------------------------------------------------------- surfaces

def bevel(palette, base=None, edge=1, light=1, dark=1, sides="tlbr"):
    """Flat colour with a lit top and left edge and a shaded bottom and right edge, `edge` texels wide; `sides` picks
    which edges are drawn (t, l, b, r)."""
    def paint(p):
        b = len(palette) // 2 if base is None else base
        for y in range(p.h):
            for x in range(p.w):
                k = b
                if ("t" in sides and y < edge) or ("l" in sides and x < edge):
                    k = b + light
                if ("b" in sides and y >= p.h - edge) or ("r" in sides and x >= p.w - edge):
                    k = b - dark
                p.put(x, y, shade(palette, k))
    return paint


def soft(palette, base=None, edge=1, top=0.3, bottom=0.25):
    """A rounded form lit from above: a lighter band over the top `top` of it, a darker one under the bottom `bottom`,
    flat between, with bevelled edges."""
    def paint(p):
        b = len(palette) // 2 if base is None else base
        for y in range(p.h):
            band = 1 if y < p.h * top else (-1 if y >= p.h * (1 - bottom) else 0)
            for x in range(p.w):
                k = b + band
                if y < edge or x < edge:
                    k = b + band + 1
                if y >= p.h - edge or x >= p.w - edge:
                    k = b + band - 1
                p.put(x, y, shade(palette, k))
    return paint


def bands(palette, base=None, period=4, width=1, horizontal=True, edge=1):
    """Regular stripes (a horn's growth rings, ribs, fur): flat colour with a darker stripe every `period` texels."""
    def paint(p):
        b = len(palette) // 2 if base is None else base
        for y in range(p.h):
            for x in range(p.w):
                along = y if horizontal else x
                across = x if horizontal else y
                size = p.w if horizontal else p.h
                k = b - (1 if along % period < width else 0)
                if across < edge:
                    k += 1
                elif across >= size - edge:
                    k -= 1
                p.put(x, y, shade(palette, k))
    return paint


def blocks(palette, seed=1, course=4, length=7, mortar=0, base=None):
    """Clean masonry: blocks in staggered courses, each flat with a lit top-left and shaded bottom-right edge, its tone
    varied a step from its neighbours by a fixed pattern, in dark mortar lines."""
    def paint(p):
        b = len(palette) // 2 if base is None else base
        for y in range(p.h):
            row = y // course
            for x in range(p.w):
                offset = (row % 2) * (length // 2) + (seed * 3) % length
                col = (x + offset) // length
                within_y = y % course
                within_x = (x + offset) % length
                if within_y == course - 1 or within_x == length - 1:
                    p.put(x, y, shade(palette, mortar))
                    continue
                tone = b + ((row * 7 + col * 3 + seed) % 3 == 0) - ((row * 5 + col * 11 + seed) % 4 == 0)
                k = tone + (1 if within_y == 0 or within_x == 0 else 0) - (1 if within_y == course - 2 or within_x == length - 2 else 0)
                p.put(x, y, shade(palette, k))
    return paint


# ---------------------------------------------------------------- fire

def _tongue(u, v, centre, height, width):
    """How deep inside one flame tongue (u across from -1 to 1, v up from 0 to 1) the point is, 0 at its edge, or
    negative outside."""
    if v > height:
        return -1.0
    t = v / height
    half = width * (math.sin(math.pi * min(1.0, t * 0.85 + 0.15)) ** 0.8) * (1.0 - t) ** 0.35
    if half <= 0:
        return -1.0
    return 1.0 - abs(u - centre) / half


TONGUES = [(0.0, 1.0, 0.62), (-0.45, 0.72, 0.42), (0.47, 0.66, 0.4), (-0.15, 0.5, 0.5), (0.2, 0.84, 0.36)]


def flame_depth(u, v, tongues=TONGUES):
    """How deep inside the fire (the union of its tongues) a point is: above 0 inside, the more the deeper."""
    return max(_tongue(u, v, c, h, w) * (1.0 - 0.35 * v / h) for c, h, w in tongues)


def fire_colour(depth, v=0.0):
    """The fire's colour at a depth into it: deep orange-red at the edge through orange and yellow to a pale core."""
    if depth < 0.18:
        return FIRE[1]
    if depth < 0.38:
        return FIRE[2]
    if depth < 0.62:
        return FIRE[3]
    if depth < 0.82 or v > 0.55:
        return FIRE[4]
    return FIRE[5]


def flames(tongues=TONGUES, box=(0.0, 0.0, 1.0, 1.0)):
    """A smooth fire of rounded tongues in clean bands of colour, filling `box` (fractions of the piece: x0, y0, x1,
    y1, top to bottom), transparent outside the flames."""
    def paint(p):
        x0, y0, x1, y1 = box
        for y in range(p.h):
            for x in range(p.w):
                fx = (x + 0.5) / p.w
                fy = (y + 0.5) / p.h
                if not (x0 <= fx <= x1 and y0 <= fy <= y1):
                    continue
                u = (fx - x0) / (x1 - x0) * 2 - 1
                v = 1.0 - (fy - y0) / (y1 - y0)
                d = flame_depth(u, v, tongues)
                if d > 0:
                    p.put(x, y, fire_colour(d, v))
    return paint


def coals(seed=1, lumps=7):
    """A bed of glowing coals from above: a hot orange glow brightest in the middle, rounded dark coals sitting in it
    each ringed with a bright rim where the heat meets them."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                p.put(x, y, FIRE[4] if d < 0.35 else FIRE[3] if d < 0.62 else FIRE[2] if d < 0.9 else FIRE[1])
        # Coals at fixed places round the glow (the seed turns the ring).
        for i in range(lumps):
            a = (i / lumps + seed * 0.13) * 2 * math.pi
            ring = 0.55 + 0.25 * ((i * 7) % 3) / 2
            ox = cx + math.cos(a) * ring * p.w / 2
            oy = cy + math.sin(a) * ring * p.h / 2
            r = p.w * (0.09 + 0.03 * (i % 3))
            for y in range(int(oy - r) - 2, int(oy + r) + 3):
                for x in range(int(ox - r) - 2, int(ox + r) + 3):
                    d = math.hypot(x + 0.5 - ox, y + 0.5 - oy)
                    if d <= r:
                        k = 2 if (x + 0.5 - ox) + (y + 0.5 - oy) < -r * 0.4 else 1 if d < r * 0.7 else 0
                        p.put(x, y, COAL[k])
                    elif d <= r + 1.0:
                        p.put(x, y, FIRE[4])
    return paint


# ---------------------------------------------------------------- faces

def ellipse(p, cx, cy, rx, ry, colour):
    """Fills an ellipse centred at (cx, cy) with radii rx, ry (texels), tested at each pixel's middle."""
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0:
                p.put(x, y, colour)


def rounded_rect(p, x0, y0, x1, y1, colour, corner=1):
    """Fills a rectangle from (x0, y0) to (x1, y1) exclusive, its corners clipped by `corner` texels."""
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            dx = min(x - int(x0), int(x1) - 1 - x)
            dy = min(y - int(y0), int(y1) - 1 - y)
            if dx + dy >= corner:
                p.put(x, y, colour)


def eye(p, cx, cy, rx, ry, socket=None, glint=True, rim=None):
    """A big round eye socket (or eye): dark, a shade lighter at its lower rim, with a white glint high on one side."""
    socket = socket or SOCKET
    if rim is not None:
        ellipse(p, cx, cy, rx + 1, ry + 1, rim)
    ellipse(p, cx, cy, rx, ry, socket[0])
    ellipse(p, cx, cy + ry * 0.35, rx * 0.8, ry * 0.45, socket[1])
    ellipse(p, cx, cy - ry * 0.12, rx * 0.86, ry * 0.7, socket[0])
    if glint:
        g = max(1.0, min(rx, ry) * 0.36)
        ellipse(p, cx - rx * 0.35, cy - ry * 0.35, g, g, GLINT)


def smile(p, cx, cy, width, depth, colour, thickness=1):
    """A curved smile `width` texels across, its middle `depth` texels below its corners."""
    half = width / 2
    for i in range(int(width) + 1):
        x = cx - half + i
        t = (x - cx) / half
        y = cy + depth * (1 - t * t) - depth
        for k in range(thickness):
            p.put(x, y + k, colour)


def teeth(p, x0, x1, y, height, bone=None, gap=None, tooth=2):
    """A neat row of square teeth from x0 to x1 (texels), `height` tall from y, each `tooth` wide with a dark gap."""
    bone = bone or BONE[4]
    gap = gap or SOCKET[1]
    x = int(x0)
    while x < int(x1):
        for dy in range(int(height)):
            for dx in range(tooth):
                if x + dx < int(x1):
                    p.put(x + dx, y + dy, bone)
        if x + tooth < int(x1):
            for dy in range(int(height)):
                p.put(x + tooth, y + dy, gap)
        x += tooth + 1


def blush(p, cx, cy, r, colour=BLUSH):
    ellipse(p, cx, cy, r, r * 0.6, colour)


def skull_face(palette=BONE, sockets=0.24, socket_y=0.42, socket_x=0.3, mouth=True, glint=True, brow=True, tall=0.17):
    """A skull from the front: a soft cream dome, two square dark eye sockets the Minecraft way (`square_socket`), no
    nose holes, and a neat row of teeth along the bottom. `sockets` is each socket's width and `tall` half its height as
    shares of the face; `socket_x` how far in from each side the sockets' middles are and `socket_y` how far down.
    (`glint` is kept for callers: skulls have no glints since 5 October 2026.)"""
    def paint(p):
        soft(palette, 3)(p)
        w, h = p.w, p.h
        sw = max(2, round(w * sockets))
        sh = max(2, round(2 * h * tall))
        for ex in (socket_x, 1.0 - socket_x):
            u0, v0 = round(w * ex - sw / 2), round(h * socket_y - sh / 2)
            square_socket(p, u0, v0, u0 + sw, v0 + sh, lip=palette[1])
        if brow:
            for x in range(1, w - 1):
                p.put(x, 0, palette[4])
        if mouth:
            teeth(p, w * 0.22, w * 0.78, h - max(2, h * 0.16) - 1, max(2, h * 0.16), palette[4], SOCKET[1], tooth=max(1, w // 10))
    return paint


def square_socket(p, u0, v0, u1, v1, socket=None, lip=None, glow=None):
    """A skull's eye socket the Minecraft way (5 October 2026, after the owner found round, glinting skull eyes
    goofy): a hard-edged rectangle of whole texels, u0..u1 by v0..v1 (exclusive), darkest along its top in the brow's
    shadow and a step lighter along its bottom row and right column, where the far rim catches the light, over a
    one-texel lip of shaded bone (`lip`). No glint, no rounding. With `glow`, the socket is filled flat with that
    colour instead: a lit socket, square and exactly the socket's size."""
    socket = socket or SOCKET
    u0, v0, u1, v1 = int(round(u0)), int(round(v0)), int(round(u1)), int(round(v1))
    for y in range(v0, v1):
        for x in range(u0, u1):
            if glow is not None:
                p.put(x, y, glow)
                continue
            k = 0 if y == v0 else (1 if y == v1 - 1 or x == u1 - 1 else 0)
            p.put(x, y, socket[k])
    if lip is not None:
        for x in range(u0, u1):
            p.put(x, v1, lip)


def block_skull_face(face, eyes, palette=BONE, side="north", brow=True):
    """A skull's face the Minecraft way: flat cream, lit along its top and left and shaded along its bottom and right,
    with square sockets (`square_socket`) placed from model coordinates, so a glow drawn over them by a renderer lands
    on them exactly. `face` is the face's box (x0, y0, x1, y1) in model pixels and `eyes` each socket's box; on a north
    face the texture runs from x1 (its left, seen from the north) to x0, on a south face from x0 to x1. No nose holes
    and no glints."""
    x0, y0, x1, y1 = face

    def paint(p):
        soft(palette, 3)(p)
        if brow:
            for x in range(1, p.w - 1):
                p.put(x, 0, palette[4])
        for ex0, ey0, ex1, ey1 in eyes:
            if side == "north":
                u0, u1 = (x1 - ex1) / (x1 - x0) * p.w, (x1 - ex0) / (x1 - x0) * p.w
            else:
                u0, u1 = (ex0 - x0) / (x1 - x0) * p.w, (ex1 - x0) / (x1 - x0) * p.w
            v0, v1 = (y1 - ey1) / (y1 - y0) * p.h, (y1 - ey0) / (y1 - y0) * p.h
            square_socket(p, u0, v0, u1, v1, lip=palette[1])
    return paint


def riveted(palette, base=None, rivets=6, edge=1):
    """A bevelled strap with round rivet heads along it, evenly spaced."""
    def paint(p):
        bevel(palette, base, edge)(p)
        b = len(palette) // 2 if base is None else base
        r = max(0.8, p.h * 0.28)
        for i in range(rivets):
            cx = (i + 0.5) * p.w / rivets
            ellipse(p, cx, p.h / 2, r, r, shade(palette, b + 2))
            p.put(cx - 0.5, p.h / 2 - 0.5, shade(palette, b + 3))
    return paint


def hearth_wall(seed=1, arch=True):
    """A hearth's stone wall: clean blocks, and an arched opening in its lower middle with a smooth fire in it."""
    def paint(p):
        blocks(STONE, seed, course=max(3, p.h // 6), length=max(5, p.w // 4), base=2)(p)
        if not arch:
            return
        cx = p.w / 2
        aw = p.w * 0.24
        top = p.h * 0.42
        spring = p.h * 0.62
        for y in range(int(top) - 1, p.h - 1):
            for x in range(p.w):
                dx = (x + 0.5 - cx) / aw
                if y + 0.5 >= spring:
                    inside = abs(dx) <= 1.0
                else:
                    dy = (y + 0.5 - spring) / (spring - top)
                    inside = dx * dx + dy * dy <= 1.0
                outline = abs(dx) <= 1.0 + 1.4 / aw and not inside and (y + 0.5 >= top - 1)
                if inside:
                    p.put(x, y, COAL[0])
                elif outline and (y + 0.5 >= spring or ((x + 0.5 - cx) / (aw + 1.4)) ** 2 + ((y + 0.5 - spring) / (spring - top + 1.4)) ** 2 <= 1.0):
                    p.put(x, y, STONE[0])
        # The fire inside the arch, standing on a glow of coals along its foot.
        flames(box=((cx - aw * 0.92) / p.w, (top + 1) / p.h, (cx + aw * 0.92) / p.w, (p.h - 2) / p.h))(p)
        for x in range(int(cx - aw) + 1, int(cx + aw)):
            p.put(x, p.h - 2, FIRE[3])
            p.put(x, p.h - 3, FIRE[2] if x % 3 else COAL[1])
    return paint


def bone_ends(seed=1, step=6):
    """The packed ends of long bones in a catacomb wall, in neat staggered courses: each a round cream knob lit on its
    upper left, with a small marrow dot, in warm brown shadow."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, BONE[0])
        r = step * 0.45
        for row, cy in enumerate(range(step // 2, p.h + step, step)):
            for cx in range(step // 2 + (row % 2) * (step // 2), p.w + step, step):
                for y in range(int(cy - r) - 1, int(cy + r) + 2):
                    for x in range(int(cx - r) - 1, int(cx + r) + 2):
                        dx, dy = x + 0.5 - cx, y + 0.5 - cy
                        d = math.hypot(dx, dy)
                        if d <= r:
                            k = 4 if dx + dy < -r * 0.5 else 3 if d < r * 0.75 else 2
                            p.put(x % p.w, y % p.h, BONE[k])
                p.put(cx % p.w, cy % p.h, BONE[1])
    return paint


def scatter(palette, base, bits, seed=1, bit_palette=None):
    """Flat ground (earth, dust) with a few small chips of something lighter set at fixed places."""
    def paint(p):
        bevel(palette, base)(p)
        bp = bit_palette or BONE
        for i in range(bits):
            x = (i * 7 + seed * 5) % max(1, p.w - 2) + 1
            y = (i * 5 + seed * 3) % max(1, p.h - 2) + 1
            p.put(x, y, bp[3])
            p.put(x + 1, y, bp[2])
    return paint
