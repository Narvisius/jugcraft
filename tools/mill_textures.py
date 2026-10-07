"""Textures for the giant sawmill's blade and the giant sieve (tools/giant_models.py sawmill() and sieve()), redrawn
after the owner's 5 October 2026 review ("the saw spins", "the sieve should be better detailed and more interesting").

Drawn in the clean style (docs/ART_DIRECTION.md, "Texturing: keep it clean"): flat fills from four or five shades,
shape from light (lit top and left, shaded bottom and right), wear placed by hand, and regular patterns instead of
noise. No checker and no dotted lines. Every texture is opaque (the machine models and the blade's quads are drawn
solid) and tiles where the models tile it. Deterministic; names start with "dr_" like the rest of the giants' set.
Called from generate_textures.py.
"""
import math

from PIL import Image

from dieselrust_textures import AMBER, IRON, PLATE, RED, STEEL
from steampunk_textures import save

# Ground saw steel, a little bluer and brighter than the giants' plate so the blade reads against the red guard.
SAW = [(66, 70, 76), (104, 110, 116), (142, 148, 152), (176, 180, 182), (206, 210, 210), (236, 238, 236)]
SAWDUST = [(118, 84, 48), (152, 112, 68), (184, 144, 94), (212, 178, 128), (234, 208, 162)]
GRAVEL = [(56, 54, 54), (80, 77, 76), (108, 104, 100), (136, 131, 125), (166, 160, 152)]
FLINT = [(24, 26, 32), (38, 42, 50), (56, 62, 72), (86, 94, 106), (150, 158, 170)]
SOIL = [(54, 36, 24), (78, 54, 36), (104, 74, 50), (132, 98, 68)]
GOLD = [(150, 98, 20), (226, 176, 52), (252, 228, 120)]
IRON_NUGGET = [(140, 112, 100), (198, 176, 162), (236, 224, 214)]
TIN = [(116, 124, 130), (170, 178, 184), (220, 226, 230)]
MESH = [(34, 32, 32), (52, 50, 50), (106, 106, 104), (140, 140, 136), (176, 176, 170)]
YELLOW = [(120, 84, 14), (184, 136, 28), (222, 176, 48), (246, 210, 96)]
SPRING = [(32, 52, 82), (46, 78, 120), (70, 110, 160), (120, 160, 206)]


def canvas(size, fill):
    return Image.new("RGBA", (size, size), tuple(fill[:3]) + (255,))


def put(img, x, y, color):
    """Sets a pixel, wrapping round the edges, so every placed shape tiles."""
    img.putpixel((x % img.width, y % img.height), tuple(color[:3]) + (255,))


def stone(img, x, y, w, h, palette, shade):
    """A rounded stone or lump: a flat body, its top-left corner lit and its bottom-right corner shaded."""
    for j in range(h):
        for i in range(w):
            if (i, j) in ((0, h - 1), (w - 1, 0)) and w > 2 and h > 1:
                continue  # Clip two corners so a lump reads round, not square.
            put(img, x + i, y + j, palette[shade])
    put(img, x, y, palette[min(len(palette) - 1, shade + 1)])
    if w > 1:
        put(img, x + 1, y, palette[min(len(palette) - 1, shade + 1)])
    put(img, x + w - 1, y + h - 1, palette[max(0, shade - 1)])


# ------------------------------------------------------------------ the saw

def saw_disc():
    """The blade's faces, 32x32, mapped once across the whole disc (client/MachineRotors draws it): two flat steel shades
    split by a tension-ring groove, lit along its top-left quarter and shaded along its bottom right, four expansion
    slots ending in round holes, one stamped amber maker's plate so the eye can follow the spin, a dark arbor seat and a
    bright ground rim where the teeth start. The outline comes from the geometry, so nothing here is see-through."""
    S = 32
    img = canvas(S, SAW[2])
    c = (S - 1) / 2
    for y in range(S):
        for x in range(S):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy) / (S / 2)  # 0 at the centre, 1 at the tooth tips
            a = math.atan2(dy, dx)
            col = SAW[3] if r < 0.6 else SAW[2]
            # Light from the top left on the outer band: one shade up there, one down at the bottom right.
            light = math.cos(a + math.pi * 0.75)
            if 0.64 <= r < 0.84:
                col = SAW[3] if light > 0.8 else SAW[1] if light < -0.85 else SAW[2]
            if 0.58 <= r < 0.64:
                col = SAW[1]  # tension-ring groove
            if r >= 0.84:
                col = SAW[4]  # ground rim and teeth
            if r >= 0.93:
                col = SAW[5]
            if r < 0.3:
                col = SAW[1] if r >= 0.24 else IRON[2]  # arbor seat (under the flanges)
            img.putpixel((x, y), col + (255,))
    # Four expansion slots on the diagonals (clean pixel diagonals): cut in from the rim and ending in a round hole.
    for sx, sy in ((1, 1), (-1, 1), (-1, -1), (1, -1)):
        # Diagonal pixel steps from the rim (about r 0.9) inwards; then a 2x2 hole at the inner end (about r 0.7).
        x0, y0 = (16 + 9 * sx) if sx > 0 else (15 + 9 * sx), (16 + 9 * sy) if sy > 0 else (15 + 9 * sy)
        for step in range(3):
            img.putpixel((x0 - sx * step, y0 - sy * step), IRON[1] + (255,))
        hx, hy = x0 - sx * 3, y0 - sy * 3
        for ox, oy in ((0, 0), (sx, 0), (0, sy), (sx, sy)):
            img.putpixel((hx - ox, hy - oy), IRON[0] + (255,))
    # The stamped maker's plate: a small amber lozenge on the inner plate, above the arbor, lit on its top edge and
    # shaded on its bottom one, with two stamped marks.
    x0, y0, x1, y1 = 13, 7, 18, 10
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x, y) in ((x0, y0), (x1, y1)):
                continue
            img.putpixel((x, y), AMBER[2] + (255,))
    for x in range(x0 + 1, x1 + 1):
        img.putpixel((x, y0), AMBER[4] + (255,))
    for x in range(x0, x1):
        img.putpixel((x, y1), AMBER[1] + (255,))
    for x in (x0 + 2, x0 + 4):
        img.putpixel((x, y0 + 1), AMBER[1] + (255,))
        img.putpixel((x, y0 + 2), AMBER[1] + (255,))
    return img


def saw_edge():
    """Ground steel for the rim and the tooth faces: bright, lit along one side of the blade's thickness and shaded
    along the other (the quads stretch it once across each tooth face)."""
    img = canvas(16, SAW[4])
    for x in range(16):
        for y in range(0, 4):
            put(img, x, y, SAW[5])
        for y in range(12, 16):
            put(img, x, y, SAW[3])
    return img


def flange():
    """A saw flange's face, mapped once across the disc: a lit outer ring, six bolt heads (they show the spin) and the
    dark arbor bore."""
    img = canvas(16, SAW[3])
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r >= 6.5:
                put(img, x, y, SAW[4] if (x + y) < 15 else SAW[2])
            elif r < 2.2:
                put(img, x, y, IRON[1])
            elif r < 3:
                put(img, x, y, SAW[1])
    for k in range(6):
        a = k * math.pi / 3 + math.pi / 6
        bx, by = round(7 + 4.5 * math.cos(a)), round(7 + 4.5 * math.sin(a))
        put(img, bx, by, SAW[5])
        put(img, bx + 1, by, SAW[4])
        put(img, bx, by + 1, SAW[4])
        put(img, bx + 1, by + 1, SAW[0])
    return img


def pulley():
    """A cast pulley's face, mapped once across the disc: a dark rim, four spokes between four dark openings (so the
    turn shows) and a bright hub."""
    img = canvas(16, IRON[1])
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r >= 6.5:
                put(img, x, y, PLATE[3] if dy < 0 else PLATE[1])
            elif r >= 5.5:
                put(img, x, y, PLATE[2])
            elif r < 2.5:
                put(img, x, y, STEEL[3] if r >= 1.2 else IRON[0])
            elif abs(dx) < 1.2 or abs(dy) < 1.2:
                put(img, x, y, PLATE[3] if dx < 0 or dy < 0 else PLATE[2])
            else:
                put(img, x, y, IRON[0])
    return img


def weight():
    """An eccentric weight, painted safety yellow so its turn reads from across the room (mapped across the whole disc it
    is half of; its rim samples the corner): a lit rim, a stamped balance groove and a bolt."""
    img = canvas(16, YELLOW[2])
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r >= 6.5:
                put(img, x, y, YELLOW[3] if y < 8 else YELLOW[1])
            elif r < 2:
                put(img, x, y, PLATE[2])
    for x in range(5, 11):
        put(img, x, 3, YELLOW[0])
    put(img, 7, 5, PLATE[5])
    put(img, 8, 5, PLATE[4])
    put(img, 7, 6, PLATE[4])
    put(img, 8, 6, PLATE[0])
    return img


def sawdust():
    """Sawdust: pale flakes laid in offset rows over a darker bed, each lit on its top edge (a pattern, not noise)."""
    img = canvas(16, SAWDUST[1])
    for row in range(8):
        y = row * 2
        for col in range(4):
            x = col * 4 + (row % 2) * 2
            shade = 3 if (row + col) % 3 else 2
            put(img, x, y, SAWDUST[shade])
            put(img, x + 1, y, SAWDUST[shade])
            put(img, x + 2, y, SAWDUST[shade - 1])
            put(img, x + 1, y + 1, SAWDUST[0] if (row + col) % 2 else SAWDUST[2])
    for x, y in ((3, 4), (11, 10), (6, 13)):
        put(img, x, y, SAWDUST[4])
    return img


# ------------------------------------------------------------------ the sieve

def screen_mesh():
    """Woven screen wire, 32x32 (two texels a pixel, so the weave is fine and the block atlas's mipmaps fade it to an
    even grey at a distance instead of shimmering), period 4 texels, so it tiles on every deck panel: a plain weave of
    one-texel wires over dark openings. Where a wire passes over its neighbour it is lit, where it dives under it is
    one shade darker, and each opening has a shadow under its top wire, so the weave has depth from light."""
    img = canvas(32, MESH[1])
    for y in range(32):
        for x in range(32):
            i, j = x // 4, y // 4
            kx, ky = x % 4, y % 4
            over_h = (i + j) % 2 == 0  # At this cell's crossing (x = 4i, y = 4j) the horizontal wire is on top.
            if kx == 0 and ky == 0:
                c = MESH[4]
            elif ky == 0:
                # Horizontal wire between crossings: high next to the crossing it passes over, low next to the other.
                near_over = over_h if kx == 1 else (not over_h) if kx == 3 else None
                c = MESH[3] if near_over else MESH[2] if near_over is None else MESH[2]
            elif kx == 0:
                over_v = not over_h
                near_over = over_v if ky == 1 else (not over_v) if ky == 3 else None
                c = MESH[3] if near_over else MESH[2]
            elif ky == 1:
                c = MESH[0]  # the shadow under the wire
            else:
                c = MESH[1]
            put(img, x, y, c)
    return img


def screen_frame():
    """The screen box's red side plate: a dark seam, a lit flange edge, bolt heads every four pixels along the flange
    (so the 12-pixel wall panels repeat it seamlessly), the flange's shadow and the plain painted plate below."""
    img = canvas(16, RED[2])
    for x in range(16):
        put(img, x, 0, IRON[1])
        put(img, x, 1, RED[4])
        put(img, x, 2, RED[3])
        put(img, x, 3, RED[3])
        put(img, x, 4, RED[0])
        put(img, x, 15, RED[0])
        put(img, x, 14, RED[1])
    for x in range(1, 16, 4):
        put(img, x, 2, PLATE[5])
        put(img, x + 1, 2, PLATE[4])
        put(img, x, 3, PLATE[4])
        put(img, x + 1, 3, PLATE[1])
    # Placed wear: one chip at a lower corner and a short scuff.
    for x, y in ((2, 12), (3, 12), (2, 13)):
        put(img, x, y, PLATE[3])
    for x in range(9, 12):
        put(img, x, 8, RED[3])
    return img


def red_paint():
    """Plain red paint for beams and posts of any size: no frame (beams sample it anywhere), a few placed scuffs one
    shade lighter and one chip to bare steel."""
    img = canvas(16, RED[2])
    for x, y, n in ((2, 3, 3), (9, 6, 2), (5, 11, 3), (12, 13, 2)):
        for i in range(n):
            put(img, x + i, y, RED[3])
    for x, y in ((13, 2), (14, 2), (13, 3)):
        put(img, x, y, PLATE[3])
    return img


def spring_steel():
    """Blue-painted spring steel, lit along the top of each coil and shaded under it (the coils are 1-pixel rings)."""
    img = canvas(16, SPRING[2])
    for y in range(16):
        for x in range(16):
            put(img, x, y, SPRING[3] if y % 4 == 0 else SPRING[0] if y % 4 == 3 else SPRING[2])
    return img


# Stones: (x, y, w, h, shade). Laid in offset rows; every position wraps, so the heaps tile on any face.
PEBBLES = [(0, 0, 3, 2, 2), (4, 1, 2, 2, 3), (7, 0, 3, 2, 2), (11, 1, 3, 2, 3), (14, 0, 2, 2, 2),
           (1, 3, 2, 2, 3), (4, 4, 3, 2, 2), (8, 3, 2, 2, 3), (11, 4, 3, 2, 2), (15, 3, 2, 2, 3),
           (0, 6, 3, 2, 2), (4, 7, 2, 2, 3), (7, 6, 3, 2, 2), (10, 7, 2, 2, 3), (13, 6, 3, 2, 2),
           (2, 9, 2, 2, 3), (5, 10, 3, 2, 2), (9, 9, 2, 2, 3), (12, 10, 3, 2, 2), (15, 9, 2, 2, 2),
           (1, 12, 3, 2, 3), (5, 13, 2, 2, 2), (8, 12, 3, 2, 3), (12, 13, 2, 2, 2), (14, 12, 2, 2, 3)]


def gravel():
    """Gravel: rounded grey stones lit at the top left on a darker bed, laid in offset rows."""
    img = canvas(16, GRAVEL[0])
    for x, y, w, h, shade in PEBBLES:
        stone(img, x, y, w, h, GRAVEL, shade)
    return img


def flint_heap():
    """Flint: dark blue-black shards with a pale glint on one edge of a few, over a gravel-grey bed."""
    img = canvas(16, GRAVEL[1])
    for n, (x, y, w, h, shade) in enumerate(PEBBLES):
        stone(img, x, y, w, h, FLINT, shade - 1)
        if n % 4 == 1:
            put(img, x, y, FLINT[4])
    return img


def nugget_heap():
    """Sifted nuggets: small gold, iron and tin nuggets, each lit at its top left, on a bed of fine grey grit."""
    img = canvas(16, GRAVEL[0])
    for n, (x, y, w, h, shade) in enumerate(PEBBLES):
        if n % 2 == 0:
            stone(img, x, y, w, h, GRAVEL, 2)
            continue
        metal = (GOLD, IRON_NUGGET, TIN)[(n // 2) % 3]
        put(img, x, y, metal[2])
        put(img, x + 1, y, metal[1])
        put(img, x, y + 1, metal[1])
        put(img, x + 1, y + 1, metal[0])
    return img


def fines():
    """The fine stuff that drops through the mesh (soil and grit): small two-tone clods in offset rows."""
    img = canvas(16, SOIL[1])
    for row in range(8):
        y = row * 2
        for col in range(5):
            x = col * 3 + (row % 2) * 2 - 1
            put(img, x, y, SOIL[3] if (row + col) % 3 == 0 else SOIL[2])
            put(img, x + 1, y, SOIL[2])
            put(img, x + 1, y + 1, SOIL[0])
    return img


def draw_all():
    save(saw_disc(), "dr_saw_disc")
    save(saw_edge(), "dr_saw_edge")
    save(flange(), "dr_flange")
    save(pulley(), "dr_pulley")
    save(weight(), "dr_weight")
    save(sawdust(), "dr_sawdust")
    save(screen_mesh(), "dr_screen_mesh")
    save(screen_frame(), "dr_screen_frame")
    save(red_paint(), "dr_red_paint")
    save(spring_steel(), "dr_spring")
    save(gravel(), "dr_gravel")
    save(flint_heap(), "dr_flint_heap")
    save(nugget_heap(), "dr_nugget_heap")
    save(fines(), "dr_fines")
