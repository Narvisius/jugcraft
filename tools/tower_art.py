"""Realistic textures for the Drone Tower's building blocks (64x64, seamless).

Each block is painted from tileable noise (built in frequency space so it wraps at the edges), with the
details a real material shows: formwork seams and tie holes in concrete, brushed grain and fasteners on metal
cladding, a twill weave under resin on composite, weld seams and wear on armour, crystal facets on silicon
carbide, a hexagonal lattice on graphene. Lighting is baked from the top-left like vanilla's textures, but kept
soft so a wall of blocks reads as one surface rather than a grid of tiles.

Steel Armor Plate and Hazard Plating are the exceptions: the owner rejected the noisy armour on 5 October 2026, so both
are drawn flat at 32x32 in the clean style (steel_armor_plate and hazard_plating below; docs/ART_DIRECTION.md, "Tiling
building blocks"). Hazard Plating is the armour plate with a hazard band, because the tower lays it right round armour
pads and doors. The rest of the set still awaits that clean pass.
"""
import math

import numpy as np
from PIL import Image

N = 64


# ---------------------------------------------------------------- helpers

def pnoise(seed, scale, size=N, octaves=3, persistence=0.5):
    """Periodic fractal noise in roughly -1..1: random spectra low-passed at a wavelength of ``scale`` px."""
    rng = np.random.default_rng(seed)
    fy = np.fft.fftfreq(size)[:, None]
    fx = np.fft.fftfreq(size)[None, :]
    f = np.sqrt(fx ** 2 + fy ** 2)
    out = np.zeros((size, size))
    amp = 1.0
    for o in range(octaves):
        s = scale / (2 ** o)
        spec = np.fft.fft2(rng.normal(size=(size, size)))
        spec *= np.exp(-(f * s) ** 2 * 2)
        layer = np.real(np.fft.ifft2(spec))
        layer /= (np.abs(layer).max() + 1e-9)
        out += layer * amp
        amp *= persistence
    return out / (np.abs(out).max() + 1e-9)


def grain(seed, size=N, horizontal=True, strength=1.0):
    """Brushed-metal streaks: noise stretched along one axis (periodic)."""
    rng = np.random.default_rng(seed)
    line = rng.normal(size=size)
    line = np.real(np.fft.ifft(np.fft.fft(line) * np.exp(-(np.fft.fftfreq(size) * 3) ** 2)))
    line /= np.abs(line).max() + 1e-9
    g = np.tile(line[:, None] if horizontal else line[None, :], (1, size) if horizontal else (size, 1))
    g = g + rng.normal(size=(size, size)) * 0.25
    return g * strength


def colorize(base, field, spread):
    """base colour (r,g,b) shifted by a -1..1 field times spread (per channel or scalar)."""
    base = np.array(base, dtype=float)
    spread = np.array(spread if isinstance(spread, (tuple, list)) else (spread,) * 3, dtype=float)
    return base[None, None, :] + field[..., None] * spread[None, None, :]


def shade(img, factor_map):
    return img * factor_map[..., None]


def rect_mask(x0, y0, x1, y1, size=N):
    m = np.zeros((size, size), dtype=bool)
    m[y0:y1, x0:x1] = True
    return m


def bevel(img, x0, y0, x1, y1, depth=1, light=1.18, dark=0.7):
    """Raised panel edge: lit top/left, shadowed bottom/right."""
    img[y0:y0 + depth, x0:x1] *= light
    img[y0:y1, x0:x0 + depth] *= light
    img[y1 - depth:y1, x0:x1] *= dark
    img[y0:y1, x1 - depth:x1] *= dark


def groove(img, x0, y0, x1, y1, dark=0.45, lip=1.15):
    """A recessed line (a seam): dark core with a lit lower/right lip."""
    if x1 - x0 >= y1 - y0:          # horizontal
        img[y0:y1, x0:x1] *= dark
        img[y1:y1 + 1, x0:x1] *= lip
    else:
        img[y0:y1, x0:x1] *= dark
        img[y0:y1, x1:x1 + 1] *= lip


def disc(img, cx, cy, r, color, edge_dark=0.6, highlight=1.35):
    yy, xx = np.mgrid[0:N, 0:N]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    m = d <= r
    img[m] = np.array(color, dtype=float)
    ring = (d > r - 0.9) & (d <= r)
    img[ring] *= edge_dark
    hl = np.sqrt((xx - cx + r * 0.35) ** 2 + (yy - cy + r * 0.35) ** 2) <= max(0.6, r * 0.35)
    img[hl & m] *= highlight


def bolt(img, cx, cy, r=1.6, color=(150, 154, 160)):
    disc(img, cx, cy, r + 0.8, np.array(color) * 0.45, edge_dark=1.0, highlight=1.0)   # countersink shadow
    disc(img, cx, cy, r, color)


def wear(img, seed, amount=0.12, color=(170, 170, 170)):
    """Scuffs on edges and faces: lighter patches where paint has worn through."""
    n = pnoise(seed, 6, octaves=2)
    m = n > (1 - amount * 2)
    img[m] = img[m] * 0.4 + np.array(color, dtype=float) * 0.6


def grime(img, seed, amount=0.18):
    """Darker streaks running down from the top (weathering)."""
    rng = np.random.default_rng(seed)
    cols = pnoise(seed + 1, 4, octaves=2)[0]
    streak = np.clip(cols, 0, 1)[None, :] * np.linspace(0.3, 1.0, N)[:, None]
    img *= (1 - amount * streak)[..., None]
    img *= (1 - 0.05 * rng.random((N, N)))[..., None]


def to_image(img, alpha=None):
    arr = np.clip(img, 0, 255).astype(np.uint8)
    if alpha is None:
        return Image.fromarray(arr, "RGB").convert("RGBA")
    a = np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([arr, a]), "RGBA")


# ---------------------------------------------------------------- materials

def reinforced_concrete():
    """Cast-in-place concrete: mottled grey, fine pores, a formwork seam across the middle, four tie-rod holes."""
    n = pnoise(1, 18) * 0.6 + pnoise(2, 5) * 0.4
    img = colorize((138, 137, 132), n, 14)
    # A few pores, spaced apart, in place of a sprinkle at every few pixels and a jitter on every pixel.
    rng = np.random.default_rng(3)
    for px_, py_ in rng.integers(2, N - 2, size=(14, 2)):
        img[py_, px_] *= 0.75
    # A faint formwork joint once a block (real pours show seams, but a line on every block edge turned
    # walls and the buttresses into a grid of stripes).
    groove(img, 0, 63, N, 64, dark=0.9, lip=1.0)
    for cx, cy in ((12, 15), (44, 15), (12, 47), (44, 47)):
        disc(img, cx + 4, cy, 1.8, (112, 112, 108), edge_dark=0.85, highlight=0.95)
        disc(img, cx + 4, cy, 0.8, (92, 92, 90), edge_dark=1, highlight=1)
    grime(img, 4, 0.1)
    return to_image(img)


def concrete_column():
    n = pnoise(5, 22) * 0.6 + pnoise(6, 4) * 0.4
    img = colorize((132, 131, 126), n, 12)
    # Rounded column: darker at the sides.
    x = np.linspace(-1, 1, N)
    img *= (0.78 + 0.22 * np.sqrt(1 - x ** 2 * 0.95))[None, :, None]
    groove(img, 0, 0, N, 1, dark=0.85)
    grime(img, 7, 0.12)
    return to_image(img)


def steel_girder():
    """An I-beam from the side: the web with brushed grain, top and bottom flanges, a row of bolts and a weld bead."""
    img = colorize((88, 92, 100), grain(10, horizontal=True), 7)
    img[:, :8] = colorize((74, 78, 86), grain(11, horizontal=False), 6)[:, :8]
    img[:, 56:] = colorize((74, 78, 86), grain(12, horizontal=False), 6)[:, 56:]
    bevel(img, 0, 0, 8, N, 2, 1.25, 0.65)
    bevel(img, 56, 0, N, N, 2, 1.25, 0.65)
    img[:, 8:10] *= 0.6                                     # web shadow under the flange
    for y in (8, 24, 40, 56):
        bolt(img, 32, y, 1.8, (140, 144, 150))
    for y in range(0, N, 3):                                # weld bead along the flange
        img[y:y + 2, 10:12] *= 1.12
    wear(img, 13, 0.06, (150, 150, 155))
    return to_image(img)


def steel_girder_top():
    img = np.zeros((N, N, 3)) + 22
    img[2:62, 2:62] = colorize((80, 84, 92), grain(14), 6)[2:62, 2:62]
    img[10:54, 10:28] = 26
    img[10:54, 36:54] = 26
    bevel(img, 2, 2, 62, 62, 2)
    return to_image(img)


def aluminum_cladding():
    """Brushed aluminium rainscreen: two horizontal panels with a shadow gap and a recessed fixing at each end."""
    img = colorize((148, 153, 160), grain(20), 8)
    img *= (1 + pnoise(21, 30) * 0.05)[..., None]
    sheen = np.exp(-((np.arange(N) - 20) / 22.0) ** 2) * 0.05
    img *= (1 + sheen[:, None])[..., None]
    for y in (0, 32):
        groove(img, 0, y, N, y + 2, dark=0.35, lip=1.2)
        bevel(img, 0, y + 2, N, y + 32 if y == 32 else y + 32, 1, 1.12, 0.85)
    for x, y in ((5, 9), (58, 9), (5, 41), (58, 41)):
        disc(img, x, y + 8, 1.4, (120, 124, 130))
    return to_image(img)


def carbon_composite_panel():
    """Carbon-fibre twill under clear resin, framed by a dark gasket; a soft gloss band."""
    yy, xx = np.mgrid[0:N, 0:N]
    cell = 4
    twill = ((xx // cell + yy // cell) % 2).astype(float)
    tow = np.where(twill > 0, np.sin((xx % cell) / cell * math.pi), np.sin((yy % cell) / cell * math.pi))
    img = colorize((46, 48, 54), tow * 0.8 + twill * 0.3 - 0.4, 12)
    gloss = np.exp(-((xx + yy - 50) / 18.0) ** 2) * 0.35
    img += (gloss * 70)[..., None]
    img[:2, :] = 18
    img[:, :2] = 18
    img[62:, :] = 18
    img[:, 62:] = 18
    bevel(img, 2, 2, 62, 62, 1, 1.3, 0.75)
    return to_image(img)


def graphene_lattice():
    """A hexagonal lattice of dark nodes and bonds over deep charcoal, catching a faint iridescent sheen."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((40, 43, 50), pnoise(30, 16), 5)
    a = 8.0                                                 # hexagon size (tiles 64 = 8*8 across)
    h = a * math.sqrt(3) / 2
    # Distance to the nearest hexagon edge via axial coordinates on a tileable grid.
    best = np.full((N, N), 99.0)
    for cy in np.arange(-h * 2, N + h * 2, h * 2):
        for i, cx in enumerate(np.arange(-a * 3, N + a * 3, a * 3)):
            for ox, oy in ((0, 0), (a * 1.5, h)):
                px, py = cx + ox, cy + oy
                dx, dy = np.abs(xx - px), np.abs(yy - py)
                d = np.maximum(dx * 0.866 + dy * 0.5, dy)
                best = np.minimum(best, np.abs(d - h))
    bond = np.clip(1 - best / 1.1, 0, 1)
    sheen = colorize((0, 0, 0), np.sin((xx + yy) / 18.0), (18, 10, 26))
    img = img * (1 - bond[..., None]) + (np.array((98, 104, 118)) + sheen) * bond[..., None]
    return to_image(img)


def tungsten_steel_frame():
    """A heavy dark-steel frame: diagonal cross-bracing between thick rails, bolted gusset plates at the corners."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((44, 46, 52), pnoise(40, 10), 6)                 # dark infill behind the bracing
    member = colorize((76, 80, 88), grain(41), 8)
    diag = (np.abs(xx - yy) < 5) | (np.abs(xx + yy - (N - 1)) < 5)
    rails = (xx < 7) | (xx > N - 8) | (yy < 7) | (yy > N - 8)
    m = diag | rails
    img[m] = member[m]
    img[diag & ~rails] *= 0.92
    bevel(img, 0, 0, N, N, 2, 1.2, 0.6)
    bevel(img, 7, 7, N - 7, N - 7, 1, 0.65, 1.15)                  # inner edge of the rails, in shadow
    for cx, cy in ((10, 10), (53, 10), (10, 53), (53, 53)):
        img[cy - 5:cy + 6, cx - 5:cx + 6] = colorize((86, 90, 98), grain(42), 6)[cy - 5:cy + 6, cx - 5:cx + 6]
        bevel(img, cx - 5, cy - 5, cx + 6, cy + 6, 1)
        bolt(img, cx - 2, cy - 2, 1.2)
        bolt(img, cx + 2, cy + 2, 1.2)
    bolt(img, 32, 32, 2.0)
    wear(img, 43, 0.05, (120, 122, 128))
    return to_image(img)


def armor_plate(base, seed, rivet_color, weld=True, stripe=None):
    """Thick armour: two plates with a welded seam, rows of countersunk bolts, scuffed edges and grime. Only
    depleted-uranium armour still uses it; steel armour and hazard plating are drawn clean below."""
    img = colorize(base, pnoise(seed, 14) * 0.6 + pnoise(seed + 1, 3) * 0.4, 10)
    for x0, x1 in ((0, 32), (32, N)):
        bevel(img, x0, 0, x1, N, 2, 1.18, 0.7)
    if weld:
        for y in range(0, N, 2):
            img[y:y + 2, 30:34] = img[y:y + 2, 30:34] * 0.0 + np.array(base) * (1.15 if (y // 2) % 2 else 0.85)
    for x in (6, 26, 38, 58):
        for y in (6, 32, 58):
            bolt(img, x, y, 1.6, rivet_color)
    if stripe:
        img[44:48, :] = np.array(stripe) * (1 + pnoise(seed + 2, 6)[44:48, :, None] * 0.08)
    wear(img, seed + 3, 0.07, tuple(min(255, c * 1.6) for c in base))
    grime(img, seed + 4, 0.12)
    return to_image(img)


# Steel Armor Plate in gunmetal, dark to light: 0 deep shadow and seam, 1 shade and recess, 2 fill, 3 sheen, 4 lit
# edge, 5 glint. Drawn flat in the clean style (docs/ART_DIRECTION.md, "Tiling building blocks"), not from noise: the
# owner called the old noisy, banded plate horrific on 5 October 2026.
ARMOR = [(40, 44, 51), (56, 61, 69), (72, 78, 87), (86, 92, 102), (104, 110, 120), (140, 146, 156)]


def steel_armor_plate():
    """Steel Armor Plate, 32x32: one thick plate a block with its corners chamfered, so where four blocks meet the
    chamfers make a recessed diamond, and a quarter of a round bolt boss in each corner completes one whole boss in it.
    A two-pixel bevel is split across the edge (lit top and left, shaded and seamed bottom and right), so a wall shows
    one seam between plates and no top-to-bottom banding; a sheen band high on the plate and three brushed streaks one
    shade up. No detail crosses the slab cut (rows 15|16)."""
    n, chamfer, boss = 32, 7, 3.6
    img = Image.new("RGBA", (n, n), ARMOR[2] + (255,))
    px = img.load()

    def put(x, y, k):
        px[x, y] = ARMOR[k] + (255,)

    for i in range(n):
        put(i, 0, 4)
        put(0, i, 4)
        put(i, n - 1, 0)
        put(n - 1, i, 0)
    for i in range(1, n - 1):
        put(i, 1, 3)
        put(1, i, 3)
        put(i, n - 2, 1)
        put(n - 2, i, 1)
    for y in range(3, 8):
        for x in range(2, n - 2):
            put(x, y, 3)
    for x0, x1, y in ((8, 17, 12), (13, 24, 19), (6, 11, 24)):
        for x in range(x0, x1 + 1):
            put(x, y, 3)
    # The chamfered corners. Each corner is a quarter of the junction where four blocks meet; sx, sy run from that
    # junction's centre, so the boss is lit on its upper left whichever block draws the quarter.
    edge = {(0, 0): 4, (n, 0): 3, (0, n): 1, (n, n): 0}
    for y in range(n):
        for x in range(n):
            for (cx, cy), lit in edge.items():
                sx, sy = x + 0.5 - cx, y + 0.5 - cy
                d = abs(sx) + abs(sy)
                if d >= chamfer + 1:
                    continue
                if d >= chamfer:
                    put(x, y, lit)
                    continue
                r = math.hypot(sx, sy)
                if r < boss - 0.8:
                    put(x, y, 5 if sx + sy < -2.0 else 4 if sx + sy < -0.4 else 3 if sx + sy < 1.2 else 2)
                elif r < boss:
                    put(x, y, 3 if sx + sy < 0 else 1)
                elif r < boss + 0.9 and sx + sy > 0:
                    put(x, y, 0)
                else:
                    put(x, y, 1)
    return img


def depleted_uranium_armor():
    """Depleted-uranium armour: dense, dark gunmetal plate with a faint bronze-green cast (the oxide), heavier
    bolts and darker grime than steel plate."""
    img = np.array(armor_plate((58, 60, 62), 60, (100, 102, 104)), dtype=float)[..., :3]
    oxide = pnoise(61, 12)
    img += (np.clip(oxide, 0, 1) * 6)[..., None] * np.array((0.7, 0.6, 0.4))
    return to_image(img)


def silicon_carbide_armor():
    """Ceramic armour tiles: blue-grey hexagonal tiles with a faint crystalline sparkle and dark grout."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((58, 64, 76), pnoise(70, 8), 8)
    rng = np.random.default_rng(71)
    sparkle = rng.random((N, N)) < 0.02
    img[sparkle] = img[sparkle] * 1.6
    # Offset ceramic tiles, 32 wide by 16 tall, staggered half a tile per row so they tile seamlessly.
    for row in range(4):
        y0 = row * 16
        off = 16 if row % 2 else 0
        img[y0:y0 + 1, :] *= 0.45
        for col in range(3):
            x = (col * 32 + off) % N
            img[y0:y0 + 16, x:x + 1] *= 0.45
            if x + 31 <= N:
                bevel(img, x + 1, y0 + 1, x + 32, y0 + 16, 1, 1.2, 0.8)
            else:
                bevel(img, x + 1, y0 + 1, N, y0 + 16, 1, 1.2, 1.0)
                bevel(img, 0, y0 + 1, (x + 32) % N, y0 + 16, 1, 1.0, 0.8)
    return to_image(img)


# The hazard band's paint: the dieselpunk machines' hazard stripes (tools/dieselpunk_textures.HAZARD), yellow, lit
# yellow and black.
HAZARD = [(222, 172, 28), (246, 204, 60), (28, 26, 24)]


def hazard_plating():
    """Hazard Plating, 32x32: Steel Armor Plate (the same chamfered plate and bolt bosses, so a hazard rim and the
    armour it frames meet as one steel and complete each other's bosses) with a flat band of yellow-and-black diagonal
    stripes across its lower half. The band is lit along its top row and shaded along its bottom row, and its stripes
    repeat every 16 pixels, which divides the block, so a row of blocks shows one unbroken band. It starts below the
    slab cut (rows 15|16) and stops short of the corner chamfers, so a slab shows it whole or not at all. No noise or
    wear: the armour's six steel shades and three of paint."""
    n = 32
    img = steel_armor_plate()
    px = img.load()
    for y in range(16, 26):
        for x in range(n):
            if min(x + 0.5, n - x - 0.5) + (n - y - 0.5) < 8:
                continue  # leave the corner chamfer whole
            if y == 16:
                c = ARMOR[4]
            elif y == 25:
                c = ARMOR[0]
            else:
                phase = (x + y) % 16
                c = HAZARD[2] if phase >= 8 else HAZARD[1] if phase == 0 else HAZARD[0]
            px[x, y] = c + (255,)
    return img


def blast_glass():
    """Thick laminated glass in a steel frame: smoky tint, two reflection streaks, a wired interlayer."""
    yy, xx = np.mgrid[0:N, 0:N]
    img = colorize((46, 54, 62), pnoise(90, 30), 6)
    alpha = np.full((N, N), 150.0)
    refl = (np.abs((xx - yy) - 10) < 3) | (np.abs((xx - yy) + 14) < 1.5)
    img[refl] += 50
    alpha[refl] = 190
    wire = ((xx % 16 == 0) | (yy % 16 == 0))
    img[wire] *= 0.7
    frame = (xx < 3) | (xx > N - 4) | (yy < 3) | (yy > N - 4)
    img[frame] = colorize((70, 74, 82), grain(91), 6)[frame]
    alpha[frame] = 255
    bevel(img, 0, 0, N, N, 1, 1.25, 0.6)
    return to_image(img, alpha)


def red_light_strip():
    """A flush LED strip: a steel housing (matching the cladding) with a narrow frosted diffuser glowing red.
    Kept slim so the many strips the tower needs to stay lit read as fine accent lines, not red blotches."""
    yy = np.arange(N)[:, None].astype(float)
    img = colorize((52, 54, 60), grain(100), 6)
    diff = np.broadcast_to((yy > 24) & (yy < 40), (N, N))
    glow = np.broadcast_to(np.exp(-((yy - 32) / 3.5) ** 2), (N, N))
    red = np.stack([120 + 135 * glow, 12 + 90 * glow ** 4, 10 + 70 * glow ** 4], -1)
    img = np.where(diff[..., None], red, img)
    img[23:25, :] *= 0.45
    img[40:41, :] *= 1.2
    return to_image(img)


def superconducting_conduit():
    """An armoured cryogenic conduit: ribbed steel sleeve with a frost-blue window onto the red-taped core."""
    xx = np.arange(N)[None, :].astype(float)
    yy = np.arange(N)[:, None].astype(float)
    img = colorize((80, 84, 92), grain(110, horizontal=False), 7)
    ribs = (np.sin(yy / 64 * 2 * math.pi * 8) > 0.6)
    img[np.broadcast_to(ribs, (N, N))] *= 1.15
    round_ = 0.7 + 0.3 * np.sqrt(np.clip(1 - ((xx - 31.5) / 32) ** 2, 0, 1))
    img *= np.broadcast_to(round_, (N, N))[..., None]
    win = (np.abs(xx - 31.5) < 9)
    core = np.zeros((N, N, 3))
    core[..., 0] = 150
    core[..., 1] = 34
    core[..., 2] = 30
    core += (np.exp(-((xx - 31.5) / 4) ** 2) * 60)[..., None]
    frost = np.broadcast_to(win, (N, N))
    img[frost] = core[frost] * 0.7 + np.array((170, 210, 230)) * 0.3
    img[:, 22:23] *= 0.5
    img[:, 41:42] *= 1.3
    return to_image(img)


def hangar_bay_door():
    """Roll-up armoured door (the block): the same slats as the animated hangar doors, a hazard rail along the bottom."""
    img = np.array(hangar_door_slats(), dtype=float)[..., :3]
    rail = np.array(hangar_door_rail(), dtype=float)[..., :3]
    img[54:, :] = rail[44:54, :]
    return to_image(img)


def landing_platform():
    """Non-slip steel deck: diamond tread plate in panels, worn bright where traffic runs, yellow edge line."""
    yy, xx = np.mgrid[0:N, 0:N]
    img = colorize((96, 98, 100), pnoise(130, 12), 8)
    u = (xx + yy) % 8
    v = (xx - yy) % 8
    lug = ((u == 2) | (u == 3)) & ((v >= 1) & (v <= 5)) & (((xx // 4 + yy // 4) % 2) == 0)
    lug |= ((v == 2) | (v == 3)) & ((u >= 1) & (u <= 5)) & (((xx // 4 + yy // 4) % 2) == 1)
    img[lug] *= 1.3
    img[np.roll(lug, 1, axis=0) & ~lug] *= 0.75
    worn = pnoise(131, 20) > 0.3
    img[worn] *= 1.08
    groove(img, 0, 0, N, 1, 0.5)
    groove(img, 0, 0, 1, N, 0.5)
    grime(img, 132, 0.1)
    return to_image(img)


def hangar_pad():
    """A drone charging pad: dark deck with tread, a red-lit charging ring, a glowing contact and corner chevrons."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = np.array(landing_platform(), dtype=float)[..., :3] * 0.6
    d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    ring = np.abs(d - 22) < 3
    img[ring] = np.array((200, 50, 38)) * (1 + 0.25 * np.cos((d[ring] - 22) / 3 * math.pi / 2))[:, None]
    halo = np.exp(-((d - 22) / 4.5) ** 2) * 0.5
    img[..., 0] += halo * 60
    contact = d < 5
    img[contact] = np.array((255, 130, 90)) - (d[contact] * 12)[:, None]
    for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        cx = 31.5 + sx * 25
        cy = 31.5 + sy * 25
        chev = (np.abs((xx - cx) * sx + (yy - cy) * sy + 3) < 1.6) & (np.abs(xx - cx) < 5) & (np.abs(yy - cy) < 5)
        img[chev] = (214, 172, 40)
    return to_image(img)


TEXTURES = {
    "reinforced_concrete": reinforced_concrete, "concrete_column": concrete_column, "steel_girder": steel_girder,
    "steel_girder_top": steel_girder_top, "aluminum_cladding": aluminum_cladding, "carbon_composite_panel": carbon_composite_panel,
    "graphene_lattice": graphene_lattice, "tungsten_steel_frame": tungsten_steel_frame, "steel_armor_plate": steel_armor_plate,
    "depleted_uranium_armor": depleted_uranium_armor, "silicon_carbide_armor": silicon_carbide_armor,
    "hazard_plating": hazard_plating, "blast_glass": blast_glass, "red_light_strip": red_light_strip,
    "superconducting_conduit": superconducting_conduit, "hangar_bay_door": hangar_bay_door,
    "landing_platform": landing_platform, "hangar_pad": hangar_pad,
}


# ---------------------------------------------------------------- the rest of the tower's blocks

def concrete_column_top():
    img = colorize((128, 127, 122), pnoise(140, 14), 10)
    yy, xx = np.mgrid[0:N, 0:N]
    d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    img[d > 30] *= 0.8
    img[np.abs(d - 30) < 1] *= 0.6
    return to_image(img)


def acoustic_wall_panel():
    """Fabric-wrapped acoustic panels in a dark charcoal felt, with fine perforation and a recessed join."""
    yy, xx = np.mgrid[0:N, 0:N]
    img = colorize((66, 68, 72), pnoise(150, 3, octaves=2), 6)
    perf = (xx % 4 == 1) & (yy % 4 == 1)
    img[perf] *= 0.7
    groove(img, 0, 0, N, 1, 0.55)
    groove(img, 0, 0, 1, N, 0.55)
    groove(img, 31, 0, 32, N, 0.7)
    return to_image(img)


def access_floor_tile():
    """Raised access floor: anti-static vinyl tiles with a steel edge trim and a lift-socket in each corner."""
    img = colorize((96, 98, 102), pnoise(160, 10) * 0.5 + pnoise(161, 2) * 0.5, 6)
    img[:2, :] = (64, 66, 70)
    img[:, :2] = (64, 66, 70)
    bevel(img, 2, 2, N, N, 1, 1.1, 0.85)
    for cx, cy in ((7, 7), (57, 7), (7, 57), (57, 57)):
        disc(img, cx, cy, 2.2, (70, 72, 76), edge_dark=0.7, highlight=1.0)
    return to_image(img)


def carpet_tile():
    """Commercial loop-pile carpet tiles: dark grey with a fine flecked texture, quarter-turned in a checker."""
    yy, xx = np.mgrid[0:N, 0:N]
    q = ((xx // 32 + yy // 32) % 2) == 0
    loops = np.where(q, np.sin(xx * 1.6) * 0.5, np.sin(yy * 1.6) * 0.5)
    img = colorize((52, 54, 58), loops + pnoise(170, 2) * 0.6, 7)
    rng = np.random.default_rng(171)
    img[rng.random((N, N)) < 0.04] *= 1.35
    img[(xx % 32 == 0) | (yy % 32 == 0)] *= 0.85
    return to_image(img)


def transformer_casing():
    """A transformer cabinet: grey enamelled steel with louvred vents and a rating plate."""
    img = colorize((82, 88, 94), pnoise(180, 16), 6)
    bevel(img, 0, 0, N, N, 2, 1.15, 0.65)
    for y in range(10, 44, 4):
        img[y:y + 2, 8:56] *= 0.4
        img[y + 2:y + 3, 8:56] *= 1.25
    img[48:58, 36:56] = (176, 178, 172)
    img[50:51, 38:54] = 60
    img[53:54, 38:50] = 60
    bolt(img, 5, 5, 1.3)
    bolt(img, 58, 5, 1.3)
    bolt(img, 5, 58, 1.3)
    bolt(img, 58, 58, 1.3)
    grime(img, 181, 0.1)
    return to_image(img)


def warning_light():
    """A caged beacon: red glass dome with a hot core inside a steel guard."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((60, 64, 72), grain(190), 6)
    d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    dome = d < 22
    glow = np.clip(1 - d / 22, 0, 1)
    img[dome] = np.stack([150 + 105 * glow, 30 + 120 * glow ** 3, 20 + 100 * glow ** 3], -1)[dome]
    cage = dome & ((np.abs(xx - 31.5) < 1.2) | (np.abs(yy - 31.5) < 1.2) | (np.abs(d - 14) < 1))
    img[cage] = (46, 48, 54)
    img[np.abs(d - 22.5) < 1.5] = (90, 94, 102)
    return to_image(img)


def dock_plating():
    """Worn deck plate where drones dock: tread plate in a lighter steel with scuffed paint."""
    img = np.array(landing_platform(), dtype=float)[..., :3] * 1.05
    wear(img, 200, 0.1, (150, 150, 150))
    return to_image(img)


def intake_funnel():
    """An intake hopper seen from above: steel walls stepping down to a dark throat, hazard-striped rim."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((84, 88, 96), grain(210), 7)
    depth = np.maximum(np.abs(xx - 31.5), np.abs(yy - 31.5))
    img *= np.clip(depth / 32 * 0.9 + 0.2, 0.15, 1.0)[..., None]
    img[depth < 10] = 14
    rim = depth > 28
    stripes = ((xx + yy) // 8) % 2 == 0
    img[rim & stripes] = (214, 172, 40)
    img[rim & ~stripes] = (30, 30, 32)
    return to_image(img)


def armored_conduit():
    """Armoured cable duct: a steel sleeve with clamp bands and a red cable visible through an inspection slot."""
    yy = np.arange(N)[:, None].astype(float)
    xx = np.arange(N)[None, :].astype(float)
    img = colorize((80, 84, 92), grain(220, horizontal=True), 7)
    img *= np.broadcast_to(0.7 + 0.3 * np.sqrt(np.clip(1 - ((yy - 31.5) / 32) ** 2, 0, 1)), (N, N))[..., None]
    slot = np.broadcast_to(np.abs(yy - 31.5) < 5, (N, N))
    img[slot] = np.array((150, 34, 30)) * np.broadcast_to(1 + 0.3 * np.cos((yy - 31.5) / 5 * math.pi / 2), (N, N))[slot][:, None]
    for x in (0, 31):
        img[:, x:x + 4] = colorize((110, 114, 122), grain(221, horizontal=False), 6)[:, x:x + 4]
        bevel(img, x, 0, x + 4, N, 1)
    return to_image(img)


def round_end(base, core, seed):
    """End of a round conduit: steel ring around a coloured core."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    img = colorize(base, pnoise(seed, 8), 6)
    img[d < 30] *= 1.1
    img[np.abs(d - 30) < 1.2] *= 0.5
    img[d < 14] = core
    img[np.abs(d - 14) < 1] *= 0.6
    return to_image(img)


def cooling_fin():
    """Extruded aluminium heat-sink fins: deep slots between bright fin edges."""
    xx = np.arange(N)[None, :]
    img = colorize((150, 156, 164), grain(230, horizontal=False), 8)
    phase = xx % 8
    img[np.broadcast_to(phase < 3, (N, N))] = 26
    img[np.broadcast_to(phase == 3, (N, N))] *= 1.25
    img[np.broadcast_to(phase == 7, (N, N))] *= 0.8
    return to_image(img)


def cooling_fin_top():
    img = np.array(cooling_fin(), dtype=float)[..., :3]
    img[:4, :] = 60
    img[60:, :] = 60
    return to_image(img)


def ceramic_insulator():
    """Glazed porcelain insulator sheds: off-white rings with a glossy highlight, grey caps top and bottom."""
    yy = np.arange(N)[:, None].astype(float)
    xx = np.arange(N)[None, :].astype(float)
    shed = np.sin(yy / 16 * 2 * math.pi) * 0.5 + 0.5
    img = np.zeros((N, N, 3)) + np.array((214, 208, 198))
    img *= np.broadcast_to(0.7 + 0.3 * shed, (N, N))[..., None]
    gloss = np.broadcast_to(np.exp(-((xx - 22) / 6) ** 2) * 0.25, (N, N))
    img *= (1 + gloss)[..., None]
    img *= np.broadcast_to(0.75 + 0.25 * np.sqrt(np.clip(1 - ((xx - 31.5) / 32) ** 2, 0, 1)), (N, N))[..., None]
    img[:4, :] = (84, 86, 90)
    img[60:, :] = (84, 86, 90)
    return to_image(img)


def ceramic_insulator_top():
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    img = np.zeros((N, N, 3)) + 50
    img[d < 30] = (214, 208, 198)
    img[np.abs(d - 22) < 2] *= 0.8
    img[d < 9] = (90, 92, 96)
    return to_image(img)


def copper_busbar():
    """Copper busbars: three bars of rolled copper with a warm sheen, tarnish and dark insulating spacers."""
    xx = np.arange(N)[None, :].astype(float)
    img = colorize((182, 110, 62), grain(240, horizontal=False), 12)
    phase = xx % 21
    img[np.broadcast_to(phase < 3, (N, N))] = (40, 36, 34)
    hl = np.broadcast_to(np.exp(-((phase - 8) / 2.5) ** 2) * 0.35, (N, N))
    img *= (1 + hl)[..., None]
    tarn = pnoise(241, 8) > 0.55
    img[tarn] = img[tarn] * 0.7 + np.array((70, 110, 90)) * 0.3
    return to_image(img)


def copper_busbar_top():
    img = np.array(copper_busbar(), dtype=float)[..., :3]
    img[:3, :] *= 0.6
    img[61:, :] *= 0.6
    return to_image(img)


def port(accent, seed, cargo):
    """Exchange port faces: hazard-striped frame around a steel panel with an energy socket or a cargo hatch."""
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    img = colorize((76, 80, 88), grain(seed), 7)
    edge = np.minimum(np.minimum(xx, yy), np.minimum(N - 1 - xx, N - 1 - yy))
    rim = edge < 5
    stripes = ((xx + yy) // 6) % 2 == 0
    img[rim & stripes] = (214, 172, 40)
    img[rim & ~stripes] = (30, 30, 32)
    bevel(img, 5, 5, N - 5, N - 5, 1, 0.7, 1.15)
    if cargo:
        img[14:50, 14:50] = 22
        img[16:48, 16:31] = colorize((96, 100, 108), grain(seed + 1), 6)[16:48, 16:31]
        img[16:48, 33:48] = colorize((96, 100, 108), grain(seed + 2), 6)[16:48, 33:48]
        img[30:34, 16:48] = (214, 172, 40)
    else:
        d = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
        img[d < 16] = (40, 42, 48)
        img[np.abs(d - 12) < 2] = np.array(accent)
        img[d < 5] = np.array(accent) * 1.2
    return to_image(img)


def platform_deck():
    """The darker deck the landing pads and pickups are laid on (tread plate, black paint)."""
    img = np.array(landing_platform(), dtype=float)[..., :3]
    img = img * 0.32 + 6
    return img


def formed_pad(tiles=5):
    """A formed 5x5 landing pad, one picture at 64 px a block: dark tread deck, hazard rim, cyan edge lights,
    corner brackets, a dashed touchdown circle, arrows in to the charger port in the middle."""
    S = N * tiles
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    deck = np.tile(platform_deck(), (tiles, tiles, 1))
    img = deck.copy()
    c = (S - 1) / 2
    edge = np.minimum(np.minimum(xx, yy), np.minimum(S - 1 - xx, S - 1 - yy))
    rim = edge < 12
    stripes = ((xx + yy) // 16) % 2 == 0
    img[rim & stripes] = (214, 172, 40)
    img[rim & ~stripes] = (22, 22, 24)
    light = (np.abs(edge - 20) < 2)
    img[light] = (90, 220, 240)
    img[np.abs(edge - 20) < 5] += np.array((0, 30, 40)) * 0.6
    for sx in (0, 1):
        for sy in (0, 1):
            x0 = 36 if sx == 0 else S - 37
            y0 = 36 if sy == 0 else S - 37
            dx = 1 if sx == 0 else -1
            dy = 1 if sy == 0 else -1
            for k in range(28):
                img[y0 - 2:y0 + 2, x0 + dx * k - 2:x0 + dx * k + 2] = 225
                img[y0 + dy * k - 2:y0 + dy * k + 2, x0 - 2:x0 + 2] = 225
    d = np.sqrt((xx - c) ** 2 + (yy - c) ** 2)
    ang = np.degrees(np.arctan2(yy - c, xx - c)) % 360
    circle = (np.abs(d - 104) < 3) & ((ang // 10) % 2 == 0)
    img[circle] = 220
    hub = d < 46
    img[hub & (d > 38)] = colorize((150, 154, 162), grain(250, size=S), 8)[hub & (d > 38)]
    img[(d <= 38) & (d > 34)] = (40, 42, 48)
    img[(d <= 34) & (d > 27)] = (24, 30, 44)
    img[(d <= 27) & (d > 22)] = (90, 220, 240)
    img[(d <= 22) & (d > 9)] = (18, 22, 32)
    img[d <= 9] = (140, 240, 255)
    for k in range(16):
        w = k
        img[64 + k:65 + k, int(c) - w:int(c) + w + 1] = (230, 176, 50)
        img[S - 65 - k:S - 64 - k, int(c) - w:int(c) + w + 1] = (230, 176, 50)
        img[int(c) - w:int(c) + w + 1, 64 + k:65 + k] = (230, 176, 50)
        img[int(c) - w:int(c) + w + 1, S - 65 - k:S - 64 - k] = (230, 176, 50)
    for t in range(1, tiles):
        img[t * N:t * N + 1, :] *= 0.75
        img[:, t * N:t * N + 1] *= 0.75
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def formed_pickup(tiles=3):
    """A formed 3x3 supply pickup at 64 px a block: hazard border, dark deck, arrows in, and the closed two-door
    lift hatch in the middle block."""
    S = N * tiles
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = np.tile(platform_deck() * 1.3, (tiles, tiles, 1))
    edge = np.minimum(np.minimum(xx, yy), np.minimum(S - 1 - xx, S - 1 - yy))
    rim = edge < 16
    stripes = ((xx + yy) // 16) % 2 == 0
    img[rim & stripes] = (226, 182, 40)
    img[rim & ~stripes] = (22, 22, 24)
    c = S // 2
    for k in range(16):
        img[36 + k:37 + k, c - k:c + k + 1] = (250, 214, 90)
        img[S - 37 - k:S - 36 - k, c - k:c + k + 1] = (250, 214, 90)
        img[c - k:c + k + 1, 36 + k:37 + k] = (250, 214, 90)
        img[c - k:c + k + 1, S - 37 - k:S - 36 - k] = (250, 214, 90)
    h0, h1 = N + 4, 2 * N - 4
    img[h0 - 4:h1 + 4, h0 - 4:h1 + 4] = colorize((110, 114, 122), grain(260, size=S), 6)[h0 - 4:h1 + 4, h0 - 4:h1 + 4]
    door = colorize((84, 88, 96), grain(261, size=S, horizontal=False), 6)
    img[h0:h1, h0:h1] = door[h0:h1, h0:h1]
    img[h0:h1, c - 1:c + 1] = 14
    for y in range(h0 + 6, h1 - 6, 10):
        img[y:y + 4, h0 + 6:c - 6] = (214, 172, 40)
        img[y:y + 4, c + 6:h1 - 6] = (214, 172, 40)
    for cx, cy in ((h0 + 2, h0 + 2), (h1 - 3, h0 + 2), (h0 + 2, h1 - 3), (h1 - 3, h1 - 3)):
        img[cy - 2:cy + 2, cx - 2:cx + 2] = (90, 220, 240)
    for t in range(1, tiles):
        img[t * N:t * N + 1, :] *= 0.8
        img[:, t * N:t * N + 1] *= 0.8
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def landing_pad():
    """A loose pad plate (not yet in a complete 5x5): dark deck with amber corner marks and a grey cross."""
    img = platform_deck()
    img[:3, :] = 50
    img[:, :3] = 50
    for (y0, x0) in ((6, 6), (6, 50), (50, 6), (50, 50)):
        img[y0:y0 + 8, x0:x0 + 8] = (214, 172, 40)
        img[y0 + 3:y0 + 8, x0 + 3:x0 + 8] = platform_deck()[y0 + 3:y0 + 8, x0 + 3:x0 + 8]
    img[29:35, 20:44] = (110, 114, 122)
    img[20:44, 29:35] = (110, 114, 122)
    return to_image(img)


def landing_pad_side():
    img = platform_deck()
    img[54:58, :] = (90, 220, 240)
    img[:2, :] = 50
    return to_image(img)


def supply_pickup():
    """A loose pickup plate: dark deck with a yellow-black border."""
    yy, xx = np.mgrid[0:N, 0:N]
    img = platform_deck() * 1.3
    edge = np.minimum(np.minimum(xx, yy), np.minimum(N - 1 - xx, N - 1 - yy))
    stripes = ((xx + yy) // 8) % 2 == 0
    img[(edge < 6) & stripes] = (226, 182, 40)
    img[(edge < 6) & ~stripes] = (22, 22, 24)
    return to_image(img)


def supply_pickup_side():
    yy, xx = np.mgrid[0:N, 0:N]
    img = platform_deck() * 1.3
    stripes = ((xx + yy) // 8) % 2 == 0
    img[(yy < 10) & stripes] = (226, 182, 40)
    img[(yy < 10) & ~stripes] = (22, 22, 24)
    return to_image(img)


TEXTURES.update({
    "concrete_column_top": concrete_column_top, "acoustic_wall_panel": acoustic_wall_panel,
    "access_floor_tile": access_floor_tile, "carpet_tile": carpet_tile, "transformer_casing": transformer_casing,
    "warning_light": warning_light, "dock_plating": dock_plating, "intake_funnel": intake_funnel,
    "armored_conduit": armored_conduit, "armored_conduit_top": lambda: round_end((80, 84, 92), (150, 34, 30), 222),
    "cooling_fin": cooling_fin, "cooling_fin_top": cooling_fin_top, "ceramic_insulator": ceramic_insulator,
    "ceramic_insulator_top": ceramic_insulator_top, "copper_busbar": copper_busbar, "copper_busbar_top": copper_busbar_top,
    "energy_exchange_port": lambda: port((235, 62, 44), 270, False), "cargo_exchange_port": lambda: port((214, 172, 40), 271, True),
    "landing_pad": landing_pad, "landing_pad_side": landing_pad_side,
    "supply_pickup": supply_pickup, "supply_pickup_side": supply_pickup_side,
})


# ---------------------------------------------------------------- joined-up hangar pads and the roll-up doors

# Hangar pad parts (tower/HangarPadBlock.PART): 0 loose; then each layout's blocks row by row (z, then x).
HANGAR_PAD_LAYOUTS = [("small", 3, 3), ("large", 7, 7), ("medium", 6, 4), ("medium", 4, 6)]
HANGAR_PAD_PARTS = 1 + sum(w * d for _, w, d in HANGAR_PAD_LAYOUTS)


def hangar_pad_part(kind, wx, dz, rx, rz):
    """The part number for block (rx, rz) of a ``wx`` by ``dz`` hangar floor of ``kind``."""
    base = 1
    for k, w, d in HANGAR_PAD_LAYOUTS:
        if (k, w, d) == (kind, wx, dz):
            return base + rz * w + rx
        base += w * d
    raise KeyError((kind, wx, dz))


def formed_hangar_pad(kind, wb, db):
    """One hangar's whole floor as one pad, 64 px a block: the dark tread deck, a hazard rim, a cyan edge light,
    white corner brackets, and in the middle a touchdown mark sized for the hangar's drone with the charging
    port. Each size is its own design: small bays get a compact ringed charger, medium bays a long dashed
    rectangle with two chargers, large bays the full circle with arrows in. Symmetric under a half turn, so
    which side the bay opens to does not matter."""
    W, D = N * wb, N * db
    yy, xx = np.mgrid[0:D, 0:W].astype(float)
    img = np.tile(platform_deck(), (db, wb, 1))
    cx, cy = (W - 1) / 2, (D - 1) / 2
    edge = np.minimum(np.minimum(xx, yy), np.minimum(W - 1 - xx, D - 1 - yy))
    rim = edge < 10
    stripes = ((xx + yy) // 14) % 2 == 0
    img[rim & stripes] = (204, 164, 44)
    img[rim & ~stripes] = (22, 22, 24)
    img[np.abs(edge - 16) < 1.6] = (90, 220, 240)
    img[np.abs(edge - 16) < 4] += np.array((0, 18, 24))
    arm = min(W, D) // 7
    for sx, sy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        x0 = 26 if sx == 0 else W - 27
        y0 = 26 if sy == 0 else D - 27
        dx = 1 if sx == 0 else -1
        dy = 1 if sy == 0 else -1
        for k in range(arm):
            img[y0 - 2:y0 + 2, x0 + dx * k - 2:x0 + dx * k + 2] = 222
            img[y0 + dy * k - 2:y0 + dy * k + 2, x0 - 2:x0 + 2] = 222

    def charger(px, py, r):
        d = np.sqrt((xx - px) ** 2 + (yy - py) ** 2)
        img[(d <= r) & (d > r * 0.82)] = (150, 154, 162)
        img[(d <= r * 0.82) & (d > r * 0.6)] = (24, 30, 44)
        img[(d <= r * 0.6) & (d > r * 0.48)] = (90, 220, 240)
        img[(d <= r * 0.48) & (d > r * 0.2)] = (18, 22, 32)
        img[d <= r * 0.2] = (140, 240, 255)

    if kind == "small":
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        img[np.abs(d - 50) < 2.5] = (220, 220, 220)
        charger(cx, cy, 22)
    elif kind == "medium":
        long_x = W >= D
        hx, hy = (W * 0.36, D * 0.26) if long_x else (W * 0.26, D * 0.36)
        box = (np.abs(np.abs(xx - cx) - hx) < 2.5) & (np.abs(yy - cy) <= hy) | (np.abs(np.abs(yy - cy) - hy) < 2.5) & (np.abs(xx - cx) <= hx)
        dash = ((xx + yy) // 12) % 2 == 0
        img[box & dash] = (220, 220, 220)
        off = N * 0.9
        for s in (-1, 1):
            charger(cx + s * off if long_x else cx, cy if long_x else cy + s * off, 20)
        # A stripe between the two chargers.
        if long_x:
            img[(np.abs(yy - cy) < 2) & (np.abs(xx - cx) < off - 22)] = (204, 164, 44)
        else:
            img[(np.abs(xx - cx) < 2) & (np.abs(yy - cy) < off - 22)] = (204, 164, 44)
    else:
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        ang = np.degrees(np.arctan2(yy - cy, xx - cx)) % 360
        img[(np.abs(d - 150) < 3) & ((ang // 9) % 2 == 0)] = 220
        charger(cx, cy, 44)
        for k in range(18):
            for sgn in (-1, 1):
                img[int(cy + sgn * (110 - k)) - 1:int(cy + sgn * (110 - k)) + 1, int(cx) - k:int(cx) + k + 1] = (220, 172, 50)
                img[int(cy) - k:int(cy) + k + 1, int(cx + sgn * (110 - k)) - 1:int(cx + sgn * (110 - k)) + 1] = (220, 172, 50)
    for t in range(1, wb):
        img[:, t * N:t * N + 1] *= 0.8
    for t in range(1, db):
        img[t * N:t * N + 1, :] *= 0.8
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def hangar_pad_side():
    img = platform_deck()
    img[50:54, :] = (90, 220, 240)
    img[:2, :] = 50
    return to_image(img)


def hangar_door_slats():
    """One block of the roll-up door: eight interlocking steel slats in the tower's armour steel, each with a
    rolled lip catching the light, a shadowed joint and a little grime collecting on the ledges."""
    yy, xx = np.mgrid[0:N, 0:N]
    img = colorize((76, 82, 92), grain(280), 7)
    slat = yy % 8
    img[slat == 0] *= 0.4
    img[slat == 1] *= 1.3
    img[slat == 2] *= 1.12
    img[slat == 7] *= 0.78
    img *= (1 + pnoise(281, 20) * 0.04)[..., None]
    for x in (4, 59):                              # guide shoes riding in the side tracks
        img[:, x - 1:x + 2] *= 0.75
    grime(img, 282, 0.1)
    return to_image(img)


def hangar_door_rail():
    """The door's bottom rail: a steel bar painted in hazard stripes, with a rubber seal along its foot."""
    yy, xx = np.mgrid[0:N, 0:N]
    stripe = ((xx + yy) // 10) % 2 == 0
    img = np.where(stripe[..., None], colorize((206, 166, 44), pnoise(290, 8), 10), colorize((30, 30, 32), pnoise(291, 8), 4))
    img[54:, :] = colorize((26, 26, 28), pnoise(292, 4), 3)[54:, :]
    bevel(img, 0, 0, N, 54, 2, 1.2, 0.7)
    wear(img, 293, 0.06, (120, 122, 128))
    return to_image(img)


def write_all(root):
    """Writes every tower texture (and the formed pad and pickup tiles) into textures/block under ``root``."""
    import os
    out = os.path.join(root, "textures", "block")
    for name, fn in TEXTURES.items():
        fn().save(os.path.join(out, name + ".png"))
    pad = formed_pad()
    for dz in range(5):
        for dx in range(5):
            pad.crop((dx * N, dz * N, dx * N + N, dz * N + N)).save(os.path.join(out, f"landing_pad_formed_{1 + dz * 5 + dx}.png"))
    hangar_pad_side().save(os.path.join(out, "hangar_pad_side.png"))
    for kind, w, d in HANGAR_PAD_LAYOUTS:
        pic = formed_hangar_pad(kind, w, d)
        for rz in range(d):
            for rx in range(w):
                part = hangar_pad_part(kind, w, d, rx, rz)
                pic.crop((rx * N, rz * N, rx * N + N, rz * N + N)).save(os.path.join(out, f"hangar_pad_{part}.png"))
    ent = os.path.join(root, "textures", "entity")
    os.makedirs(ent, exist_ok=True)
    hangar_door_slats().save(os.path.join(ent, "hangar_door_slats.png"))
    hangar_door_rail().save(os.path.join(ent, "hangar_door_rail.png"))
    pick = formed_pickup()
    for dz in range(3):
        for dx in range(3):
            pick.crop((dx * N, dz * N, dx * N + N, dz * N + N)).save(os.path.join(out, f"supply_pickup_formed_{1 + dz * 3 + dx}.png"))
