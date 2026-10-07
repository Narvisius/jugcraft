"""Generated data for Halloween decorations batch 20, Pumpkin Night (tools/decor20.py): sculpted models on the toolkit of
tools/flora_art.py (each block one texture painted here), blockstates, items, names, loot and tags; the textures the
client draws with (the singing pumpkins' faces, the garlands, the effigy's cloak); and the quads it draws the effigy's
cloak from (assets/jugcraft/decor20_quads.json). The Red Kuri and Kabocha pumpkins' skins, seeds and dishes are painted
with the other heirlooms in tools/halloween_textures.py.

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import copy
import math
import random

from PIL import Image

import block_style as bs
import cute_art as ca
import decor17_data as d17d
import decor18_data as d18d
import decor20 as d20
import flora_art as fa
from flora_art import Px, Sculpt, cube, pal, plane_xy, plane_zy, rotation, shade, solid, strip
from decor6_data import quads
from model_writer import separate_coplanar

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL6 = ("north", "south", "east", "west", "up", "down")
DYES = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
        "brown", "green", "red", "black")

faces = d18d.faces
shifted = d18d.shifted

WEATHERED = pal("2e2218", "463424", "5e4832", "775e42", "8e7454", "a68c6a")
PLANK = pal("3a2414", "54361e", "6e4828", "885c34", "a07242")
POST = pal("241810", "362416", "4a321e", "5e4028", "745034")
CRATE = pal("6a4a26", "8a6432", "a87e40", "c49a56", "dcb672")
SLATE = pal("181c1e", "22282a", "2c3436", "374042", "434e50")
CHALK = pal("b8b6aa", "dcdad0", "f4f2ea")
CANVAS = pal("b8ae98", "d0c8b2", "e4dcc8", "f2ecdc", "fbf8ee")
AWNING = pal("8a3206", "b04a0c", "d06414", "e8822a", "f4a04a")
HAY = pal("7a5e1c", "9e7c26", "bea034", "d8bc4c", "ecd674", "f8eaa0")
STRAW = pal("6e5418", "917022", "b28e30", "ceaa46", "e2c464", "f2dc8c")
WICKER = pal("4a3216", "6a4a22", "8a6430", "a87e42", "c49a5c")
TWINE = pal("5a4426", "7a6036", "9a7c4a", "b89a62")
CORN_HUSK = pal("8a7448", "a88e5a", "c4aa72", "dcc490", "eedcae")
CLOTH = pal("8a8a8a", "a6a6a6", "c0c0c0", "d8d8d8", "eeeeee", "ffffff")
ASH = pal("2a2826", "3e3a37", "56514c", "6e6862", "8a847c", "a49e96")
CHAR = pal("0e0b09", "1a1410", "261d17", "36291f")
EMBER = d17d.EMBER
LEAF_GREEN = pal("1e3a14", "2c5220", "3e6c2c", "56883c", "72a44e")
VINE = pal("2a3a14", "3c5220", "506a2c", "688638")
GLOW = pal("b04a08", "e07a14", "f8a42a", "ffcc58", "ffeca0", "fffbe4")
# Each voice's pumpkin's skin (its shape and face are tools/decor20.py VOICES).
VOICE_SKIN = {"bass": pal("5a2204", "7e3208", "a4460c", "c45e14", "dc7a22", "ec9638"),
              "tenor": pal("6e2c06", "96420a", "bc5c12", "d87a1c", "ec9632", "f6b250"),
              "alto": pal("7e4206", "a8600c", "cc8016", "e49e26", "f2ba3e", "fad468"),
              "soprano": pal("82806e", "a6a492", "c4c2b0", "dcdac8", "eeecde", "fafaf2")}
STEM = pal("2a2410", "3e3618", "564a22", "6e5e2e")


def separated(elements):
    """A copy of a model drawn whole and then shared out among several blocks' part models, its differently drawn faces
    pulled apart first (model_writer.separate_coplanar): each part's own pass at writing cannot see the others'."""
    elements = copy.deepcopy(elements)
    separate_coplanar(elements)
    return elements


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


# ---------------------------------------------------------------- painters

def boards(palette, seed=1, width=6, horizontal=True, nails=True, worn=0.15):
    """Weathered boards side by side: grain along each, a dark seam between, a lighter worn edge, iron nail heads."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        span, across = (p.w, p.h) if horizontal else (p.h, p.w)
        seams = list(range(0, across, width))
        tone = {s: rng.choice((-1, 0, 0, 1)) for s in seams}
        for a in range(across):
            board = max(s for s in seams if s <= a)
            for b in range(span):
                g = math.sin(b * 0.55 + board * 1.7 + rng.random() * 0.4)
                k = n // 2 + tone[board] + (1 if g > 0.75 else 0) - (1 if g < -0.8 else 0) + rng.choice((0, 0, 0, 0, -1))
                if a == board:
                    k = 0
                elif a == board + 1 and rng.random() < 0.7:
                    k = n - 1
                if rng.random() < worn * 0.1:
                    k = n - 1
                x, y = (b, a) if horizontal else (a, b)
                p.put(x, y, shade(palette, k))
        if nails:
            for s in seams:
                for b in (1, span - 2):
                    x, y = (b, s + 2) if horizontal else (s + 2, b)
                    p.put(x, y, d17d.IRON[4])
    return paint


def slats(palette, seed=1, count=3, gap=1):
    """A crate side: `count` slats across with dark gaps between them, stencilled at the ends."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        size = max(2, (p.h - gap * (count - 1)) // count)
        for y in range(p.h):
            slat = y // (size + gap)
            within = y - slat * (size + gap)
            for x in range(p.w):
                if within >= size:
                    p.put(x, y, shade(palette, 0))
                    continue
                k = n // 2 + (1 if within == 0 else 0) - (1 if within == size - 1 else 0) + rng.choice((0, 0, 0, -1))
                p.put(x, y, shade(palette, k))
            if within < size and slat < count:
                for x in (1, p.w - 2):
                    p.put(x, y, palette[1])
    return paint


def straw(palette=STRAW, seed=1, bands=(), ragged=False):
    """Straw stalks running down the piece in clean gold and pale stripes, with twine `bands` (rows) bound round; `ragged`
    frays the foot."""
    def paint(p):
        for x in range(p.w):
            # Stalks in a fixed run of tones, two texels wide; a ragged foot frays in a regular zigzag.
            tone = (3, 4, 3, 2, 3, 4)[(x // 2 + seed) % 6]
            end = p.h - ((0, 1, 2, 1)[x % 4] if ragged else 0)
            for y in range(end):
                p.put(x, y, palette[tone])
        for row in bands:
            for x in range(p.w):
                p.put(x, row, TWINE[1 + (x % 3 == 0)])
                p.put(x, row + 1, TWINE[0])
    return paint


def tuft(palette=STRAW, seed=1):
    """A tuft of straw splaying downwards from a bound top, cut out round its stalks."""
    def paint(p):
        rng = random.Random(seed)
        for stalk in range(p.w + 4):
            x0 = p.w / 2 + rng.uniform(-1.2, 1.2)
            x1 = rng.uniform(-1, p.w)
            length = rng.uniform(0.7, 1.0) * p.h
            for t in range(int(length) + 1):
                f = t / max(1.0, length)
                p.put(x0 + (x1 - x0) * f, t, shade(palette, rng.choice((1, 2, 3, 4))))
        for x in range(p.w):
            p.put(x, 0, TWINE[1])
            p.put(x, 1, TWINE[2])
    return paint


def wicker(seed=1):
    """Woven wicker: withies over and under in a basket weave, the crossings shaded."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                cell = (x // 3 + y // 3) % 2
                within = (y % 3) if cell else (x % 3)
                k = 3 if within == 1 else 2 if within == 0 else 1
                k += rng.choice((0, 0, 0, -1))
                p.put(x, y, shade(WICKER, k))
    return paint


def corn_sheaf(seed=1):
    """A sheaf of dried corn stalks standing, tied in the middle, with dry leaves flagging out; cut out round them."""
    def paint(p):
        rng = random.Random(seed)
        for _ in range(p.w):
            x = rng.uniform(0, p.w - 1)
            lean = rng.uniform(-0.18, 0.18)
            top = rng.randrange(0, p.h // 5)
            for y in range(top, p.h):
                p.put(x + lean * (p.h - y) * 0.3, y, shade(CORN_HUSK, rng.choice((1, 2, 2, 3))))
        for _ in range(p.w // 2):
            y = rng.randrange(1, p.h // 2)
            x = rng.uniform(1, p.w - 2)
            side = rng.choice((-1, 1))
            for t in range(rng.randrange(3, 7)):
                p.put(x + side * t, y + t * 0.6, CORN_HUSK[3 + (t % 2)])
        tie = int(p.h * 0.55)
        for x in range(p.w):
            p.put(x, tie, TWINE[1])
            p.put(x, tie + 1, TWINE[0])
    return paint


def canvas_stripes(seed=1, stripe=4, scallop=False):
    """Awning canvas: wide orange and cream stripes along it, a fold of shadow at each stripe's edge; `scallop` cuts the
    foot into scallops, one a stripe."""
    def paint(p):
        rng = random.Random(seed)
        for x in range(p.w):
            orange = (x // stripe) % 2 == 0
            palette = AWNING if orange else CANVAS
            edge = x % stripe in (0, stripe - 1)
            for y in range(p.h):
                if scallop:
                    centre = (x // stripe) * stripe + (stripe - 1) / 2
                    depth = p.h - 1 - int(round(math.sqrt(max(0.0, (stripe / 2) ** 2 - (x - centre) ** 2)) * 0.8))
                    if y > depth:
                        continue
                k = 3 - (1 if edge else 0) + rng.choice((0, 0, 0, -1))
                p.put(x, y, shade(palette, k))
    return paint


def slate(seed=1, frame=True):
    """A slate board: dark, smudged with old chalk, in a thin wooden frame."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 2 + rng.choice((0, 0, 0, -1, 1))
                c = shade(SLATE, k)
                if rng.random() < 0.05:
                    c = shade(SLATE, 4)
                p.put(x, y, c)
        if frame:
            for x in range(p.w):
                p.put(x, 0, PLANK[3])
                p.put(x, p.h - 1, PLANK[1])
            for y in range(p.h):
                p.put(0, y, PLANK[2])
                p.put(p.w - 1, y, PLANK[2])
    return paint


def cloth(seed=1, tatters=True):
    """Pale homespun cloth (tinted by its dye in the game): smooth, soft folds down it, and a tattered, holed foot."""
    def paint(p):
        rng = random.Random(seed)
        folds = [rng.uniform(0, p.w) for _ in range(max(2, p.w // 6))]
        cut = [p.h - 1 - (rng.randrange(0, max(2, p.h // 4)) if tatters else 0) for _ in range(p.w)]
        for x in range(1, p.w - 1):
            if rng.random() < 0.4:
                cut[x] = (cut[x - 1] + cut[x + 1]) // 2
        for y in range(p.h):
            for x in range(p.w):
                if y > cut[x]:
                    continue
                fold = min(abs(x - f) for f in folds)
                k = 3 + (1 if fold < 1.0 else 0) - (1 if 1.5 < fold < 2.5 else 0)
                p.put(x, y, shade(CLOTH, k))
        if tatters:
            for _ in range(p.w * p.h // 60):
                hx, hy = rng.randrange(p.w), rng.randrange(p.h // 2, p.h)
                p.img.putpixel((hx, hy), (0, 0, 0, 0))
    return paint


def ashes(seed=1, embers=0.0):
    """Grey wood ash, fine and drifted in soft clumps, with charred flecks on a staggered lattice and (if `embers`) a few
    embers still glowing among them."""
    def paint(p):
        ground = bs.surface(ASH[2:5], seed, weights=[1, 4, 2], spread=0.7, size=(p.w, p.h))
        for y in range(p.h):
            for x in range(p.w):
                c = ground(x, y)
                if x % 7 == 3 and (y + 2 * (x // 7)) % 6 == 1:
                    c = CHAR[2]
                elif embers and x % 9 == 5 and (y + 3 * (x // 9)) % 8 == 4:
                    c = EMBER[3]
                p.put(x, y, c)
    return paint


def charred(seed=1):
    """A charred stick: black, lit along its top, with grey crazing on a slant and a red heart where it split."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                c = CHAR[3] if y == 0 else ASH[2] if (x * 3 + y) % 7 == 0 else CHAR[1]
                p.put(x, y, c)
        p.put(p.w // 3, p.h // 2, EMBER[3])
    return paint


def pumpkin_skin(palette, seed=1, ribs=4, face=None):
    """A pumpkin's side, clean and smooth: even ribs down it (a dark groove, a lit edge beside it), lighter at the
    shoulder; `face` paints the carved face on: glowing eye holes and a
    closed smile ({eye_x: [x, x], eye_y, eye, mouth_x, mouth_y, mouth_w} in texels)."""
    def paint(p):
        n = len(palette)
        step = max(2, p.w // ribs) if ribs else 0
        for y in range(p.h):
            for x in range(p.w):
                k = n - 2 - int(y / max(1, p.h - 1) * 2.4)
                if step and x % step == 0:
                    k -= 1
                elif step and x % step == 1:
                    k += 1
                p.put(x, y, shade(palette, k))
        if face is None:
            return
        # The carved eyes: upright triangles cut through, a dark cut edge round the candlelight glowing within (tested at
        # each pixel's middle, so every size comes out clean).
        e = face["eye"]
        cut = shade(palette, 0)
        for ex in face["eye_x"]:
            top = face["eye_y"] - e / 2
            for y in range(int(top) - 2, int(top + e) + 2):
                for x in range(int(ex - e) - 2, int(ex + e) + 2):
                    down = (y + 0.5 - top) / e
                    across = abs(x + 0.5 - ex) / (e / 2)
                    if 0.0 <= down <= 1.0 and across <= down:
                        glow = across / max(0.05, down)
                        p.put(x, y, GLOW[5 if glow < 0.35 and down > 0.5 else 4 if glow < 0.75 else 3])
                    elif -0.6 / e * 2 <= down <= 1.0 + 1.0 / e and across <= down + 2.2 / e:
                        p.put(x, y, cut)
        # The closed smile: a crescent slit, its corners turned up, glowing along it, cut dark above and below.
        w = face["mouth_w"]
        for x in range(int(face["mouth_x"] - w / 2) - 1, int(face["mouth_x"] + w / 2) + 1):
            t = (x + 0.5 - face["mouth_x"]) / (w / 2)
            if abs(t) > 1.08:
                continue
            centre = face["mouth_y"] - 1.6 * t * t
            for y in range(int(centre) - 3, int(centre) + 4):
                d = abs(y + 0.5 - centre)
                if d < 0.75 and abs(t) <= 1.0:
                    p.put(x, y, GLOW[4] if abs(t) < 0.7 else GLOW[3])
                elif d < 1.75:
                    p.put(x, y, cut)
    return paint


# ---------------------------------------------------------------- 17. the Farm Stand

STAND_W = 15.8
POST_X = 15.3
CRATE_W = 9.0
CRATE_D = 7.0
TABLE_TOP = 8.0
RISER_TOP = 13.0
FRONT_POST_TOP = 24.0


def crate_middles():
    """Each crate's middle (x from the seam, floor y, z), as FarmStandRenderer.CRATES lists them."""
    return [tuple(c) for c in d20.FARM_STAND["crate_middles"]]


def halves(lo, hi, uv_faces, rot=None):
    """A box from `lo` to `hi` (x measured from the seam, in the first block's frame) cut at the seam, so each half
    belongs to its own block."""
    if lo[0] < 0 < hi[0]:
        return [cube(lo, (0.0, hi[1], hi[2]), uv_faces, rot), cube((0.0, lo[1], lo[2]), hi, uv_faces, rot)]
    return [cube(lo, hi, uv_faces, rot)]


def farm_stand():
    """The Farm Stand, two blocks wide facing north: a trestle of weathered boards with a stepped riser at the back, six
    slatted crates (three on the table, three on the riser) for the goods, a striped orange-and-cream awning on four
    posts sloping to a scalloped valance, a slate header board over its front for the owner's name, price tags on the
    apron and the back crates, a hay bale and pumpkins under the table and a bundle of corn on the east post. Modelled
    in the first block's frame with x from the seam (the second block lies west, at negative x); each block has its own
    half (models farm_stand_0 and _1)."""
    sc = Sculpt(d20.FARM_STAND["block"], 2001, 128)
    top = sc.piece("top", 32, 18, boards(WEATHERED, 1, width=5))
    apron = sc.piece("apron", 32, 10, boards(WEATHERED, 2, width=5, nails=False))
    riser = sc.piece("riser", 32, 12, boards(WEATHERED, 3, width=4))
    post = sc.piece("post", 4, 32, strip(POST, 4, knots=3))
    post_end = sc.piece("post_end", 4, 4, solid(POST, 5, rim=True))
    crate_side = sc.piece("crate_side", 20, 10, slats(CRATE, 6))
    crate_end = sc.piece("crate_end", 14, 10, slats(CRATE, 7))
    crate_floor = sc.piece("crate_floor", 20, 14, boards(CRATE, 8, width=4, nails=False))
    crate_low = sc.piece("crate_low", 20, 5, slats(CRATE, 9, count=2))
    awning = sc.piece("awning", 32, 34, canvas_stripes(10, stripe=4))
    valance = sc.piece("valance", 32, 6, canvas_stripes(11, stripe=4, scallop=True))
    ridge = sc.piece("ridge", 32, 4, boards(PLANK, 12, width=4, nails=False))
    tag = sc.piece("tag", 12, 6, slate(13))
    board = sc.piece("board", 24, 12, slate(14))
    chain = sc.piece("chain", 2, 6, solid(d17d.IRON, 15))
    hay_side = sc.piece("hay_side", 20, 10, straw(HAY, 16, bands=(2, 7)))
    hay_top = sc.piece("hay_top", 20, 16, straw(HAY, 17, bands=(4, 11)))
    pumpkin = sc.piece("pumpkin", 10, 8, pumpkin_skin(VOICE_SKIN["tenor"], 18, ribs=3))
    white = sc.piece("white_pumpkin", 8, 6, pumpkin_skin(VOICE_SKIN["soprano"], 19, ribs=3))
    stem = sc.piece("stem", 2, 3, solid(STEM, 20))
    husk = sc.piece("husk", 4, 8, tuft(CORN_HUSK, 21))
    twine = sc.piece("twine", 6, 2, solid(TWINE, 23))
    kernels = sc.piece("kernels", 4, 8, solid(pal("6a1a14", "9a3a1a", "c86a20", "e8a83a", "5a2a5a"), 22, spots=[(240, 220, 160)], spot_count=4))
    el = []
    # The trestle: four posts (the back ones taller, for the awning's slope), the table top and its apron, the riser.
    for x in (-POST_X, POST_X):
        el.append(cube((x - 0.7, 0.0, 0.6), (x + 0.7, FRONT_POST_TOP, 2.0), faces(post, ("north", "south", "east", "west"), up=post_end)))
        el.append(cube((x - 0.7, 0.0, 14.0), (x + 0.7, 30.0, 15.4), faces(post, ("north", "south", "east", "west"), up=post_end)))
    el += halves((-STAND_W, TABLE_TOP - 1.0, 0.6), (STAND_W, TABLE_TOP, 9.0), faces(top, ("up", "down"), north=apron, south=apron, east=apron, west=apron))
    el += halves((-STAND_W + 0.2, TABLE_TOP - 3.2, 0.6), (STAND_W - 0.2, TABLE_TOP - 1.0, 1.0), faces(apron, ("north", "south", "down", "east", "west")))
    el += halves((-STAND_W, TABLE_TOP - 1.0, 9.0), (STAND_W, RISER_TOP, 15.4), faces(riser, ("north", "east", "west", "south"), up=top))
    # Crossbraces between the legs, low down.
    el += halves((-POST_X + 0.7, 1.6, 14.2), (POST_X - 0.7, 2.6, 15.2), faces(post, ALL6))
    # The crates: a floor and four slatted walls, the front wall low to show the goods.
    for x, floor, z in crate_middles():
        x0, x1 = x - CRATE_W / 2, x + CRATE_W / 2
        z0, z1 = z - CRATE_D / 2, z + CRATE_D / 2
        y0 = floor - 0.5
        el.append(cube((x0, y0, z0), (x1, floor, z1), faces(crate_floor, ("up", "down", "north", "south", "east", "west"))))
        el.append(cube((x0, floor, z0), (x1, floor + 2.5, z0 + 0.6), faces(crate_low, ALL6)))
        el.append(cube((x0, floor, z1 - 0.6), (x1, floor + 4.0, z1), faces(crate_side, ALL6)))
        el.append(cube((x0, floor, z0 + 0.6), (x0 + 0.6, floor + 3.5, z1 - 0.6), faces(crate_end, ALL6)))
        el.append(cube((x1 - 0.6, floor, z0 + 0.6), (x1, floor + 3.5, z1 - 0.6), faces(crate_end, ALL6)))
    # The price tags: on the apron under each front crate, on the front of each back crate.
    for i, (x, floor, z) in enumerate(crate_middles()):
        tag_y, face = d20.FARM_STAND["tags"][i // 3]
        el.append(cube((x - 3.0, tag_y - 1.4, face + 0.02), (x + 3.0, tag_y + 1.4, face + 0.5), faces(tag, ALL6)))
    # The awning: striped canvas sloping from the back posts' tops to the front, a ridge board at the back and a
    # scalloped valance hanging from its front edge.
    slope = rotation((0.0, 30.0, 15.4), "x", -22.5)
    length = 15.4 / math.cos(math.radians(22.5)) + 0.6
    for x0, x1, outer in ((-STAND_W - 0.5, 0.0, "west"), (0.0, STAND_W + 0.5, "east")):
        el.append(cube((x0, 30.0, 15.4 - length), (x1, 30.5, 15.4), faces(awning, ("up", "down", "north"), **{outer: ridge}), slope))
        el.append(plane_xy(x0, x1, 21.0, 23.6, -0.5, valance))
        el.append(cube((x0, 29.6, 14.6), (x1, 31.2, 16.0), faces(ridge, ALL6)))
    # The slate header board standing over the awning's front edge, braced back to the awning.
    bx, by, bz, bw = d20.FARM_STAND["board"]
    el.append(cube((bx - bw / 2, by - 2.8, bz + 0.1), (bx + bw / 2, by + 2.8, bz + 0.7), faces(board, ALL6)))
    for cx in (bx - bw / 2 + 1.5, bx + bw / 2 - 1.5):
        el.append(cube((cx - 0.4, by - 2.6, bz + 0.7), (cx + 0.4, by - 1.8, bz + 2.6), faces(chain, ALL6)))
    # Under the table: a hay bale and pumpkins; on the east front post, a bundle of ornamental corn.
    el.append(cube((1.5, 0.0, 3.0), (12.5, 5.0, 12.0), faces(hay_side, ("north", "south", "east", "west"), up=hay_top, down=hay_top)))
    for x, z, r, piece in ((-5.0, 4.0, 2.4, pumpkin), (-10.0, 6.5, 2.0, white), (-7.0, 10.5, 2.0, pumpkin), (8.0, 1.6, 1.4, white)):
        h = r * 1.4
        el.append(cube((x - r, 0.0, z - r * 0.8), (x + r, h, z + r * 0.8), faces(piece, ALL6)))
        el.append(cube((x - r * 0.8, 0.0, z - r), (x + r * 0.8, h * 1.06, z + r), faces(piece, ALL6)))
        el.append(cube((x - 0.3, h, z - 0.3), (x + 0.3, h + 0.9, z + 0.3), faces(stem, ALL6)))
    # Three cobs of ornamental corn hung by their husks from a nail on the east front post, the husks pulled back.
    for i, (dx, drop) in enumerate(((-0.9, 0.0), (0.0, 1.2), (0.9, 0.4))):
        x = POST_X + dx
        top = 19.5 - drop
        el.append(cube((x - 0.55, top - 4.2, -0.2), (x + 0.55, top, 0.9), faces(kernels, ALL6)))
        for side in (-1, 1):
            el.append(plane_xy(x + side * 0.3 - 0.9, x + side * 0.3 + 0.9, top - 0.6, top + 2.6, 0.35, husk,
                               rotation((x, top, 0.35), "z", -22.5 * side)))
    el.append(cube((POST_X - 1.4, 21.4, 0.2), (POST_X + 1.4, 22.0, 0.6), faces(twine, ALL6)))
    el = separated(el)
    sc.models["whole"] = el
    for part, dx in ((0, 0.0), (1, 16.0)):
        mine = [e for e in el if ((e["from"][0] + e["to"][0]) / 2 >= 0) == (part == 0)]
        sc.models[f"farm_stand_{part}"] = shifted(mine, dx, 0.0, 0.0)
    sc.models["item"] = shifted(el, 8.0, 0.0, 0.0)
    return sc


# ---------------------------------------------------------------- 19. the Harvest Effigy and its ashes

NECK_TOP = d20.EFFIGY["neck_top"]
HAND_X = d20.EFFIGY["hand_x"]
HAND_Y = d20.EFFIGY["hand_y"]


def harvest_effigy():
    """The Harvest Effigy, three blocks tall facing north: a wicker man on a straw-bound post, legs bound in straw, a
    woven wicker body under a straw shoulder roll, his arms a cross-pole sleeved in straw with tufts for hands, standing
    in a ring of corn sheaves on a straw mound. His head is the pumpkin he wears and his cloak is dyed: both the
    client's (HarvestEffigyRenderer). Each block has its own third (models harvest_effigy_0 to _2)."""
    sc = Sculpt(d20.EFFIGY["block"], 2002, 128)
    pole = sc.piece("pole", 4, 48, strip(POST, 1, knots=4))
    leg = sc.piece("leg", 8, 32, straw(STRAW, 2, bands=(4, 14, 26)))
    leg_end = sc.piece("leg_end", 8, 8, straw(STRAW, 3))
    hips = sc.piece("hips", 18, 8, straw(STRAW, 4, bands=(1,), ragged=True))
    body = sc.piece("body", 18, 26, wicker(5))
    body_side = sc.piece("body_side", 14, 26, wicker(6))
    body_top = sc.piece("body_top", 18, 14, wicker(7))
    shoulder = sc.piece("shoulder", 24, 6, straw(STRAW, 8, bands=(2,)))
    arm = sc.piece("arm", 56, 3, strip(POST, 9, horizontal=True))
    sleeve = sc.piece("sleeve", 14, 6, straw(STRAW, 10, bands=(1, 4)))
    hand = sc.piece("hand", 8, 12, tuft(STRAW, 11))
    neck = sc.piece("neck", 6, 6, straw(STRAW, 12, bands=(1, 4)))
    belt = sc.piece("belt", 18, 2, solid(TWINE, 13))
    mound = sc.piece("mound", 32, 4, straw(STRAW, 14, ragged=True))
    mound_top = sc.piece("mound_top", 32, 32, straw(STRAW, 15))
    sheaf = sc.piece("sheaf", 10, 30, corn_sheaf(16))
    el = [cube((7.2, 0.0, 7.2), (8.8, NECK_TOP, 8.8), faces(pole, ALL6)),
          cube((0.5, 0.0, 0.5), (15.5, 1.5, 15.5), faces(mound, ("north", "south", "east", "west"), up=mound_top)),
          cube((2.5, 1.5, 2.5), (13.5, 2.5, 13.5), faces(mound, ("north", "south", "east", "west"), up=mound_top))]
    for x in (4.6, 8.2):
        el.append(cube((x, 2.0, 6.4), (x + 3.2, 18.0, 9.6), faces(leg, ("north", "south", "east", "west"), up=leg_end, down=leg_end)))
    el += [cube((3.6, 16.0, 5.0), (12.4, 20.0, 11.0), faces(hips, ("north", "south", "east", "west"), up=body_top)),
           cube((3.5, 19.5, 4.5), (12.5, 32.0, 11.5), faces(body, ("north", "south"), east=body_side, west=body_side, up=body_top, down=body_top)),
           cube((3.2, 19.6, 4.2), (12.8, 20.6, 11.8), faces(belt, ALL6)),
           cube((2.0, 31.0, 5.0), (14.0, 33.5, 11.0), faces(shoulder, ALL6)),
           cube((8.0 - HAND_X - 1.0, HAND_Y - 0.6, 7.4), (8.0 + HAND_X + 1.0, HAND_Y + 0.6, 8.6), faces(arm, ALL6)),
           cube((8.0 - HAND_X + 1.0, HAND_Y - 1.4, 6.6), (2.5, HAND_Y + 1.4, 9.4), faces(sleeve, ALL6)),
           cube((13.5, HAND_Y - 1.4, 6.6), (8.0 + HAND_X - 1.0, HAND_Y + 1.4, 9.4), faces(sleeve, ALL6)),
           cube((6.5, 33.0, 6.5), (9.5, NECK_TOP, 9.5), faces(neck, ALL6))]
    for side in (-1, 1):
        x = 8.0 + side * HAND_X
        el.append(plane_xy(x - 2.0, x + 2.0, HAND_Y - 6.0, HAND_Y + 0.4, 8.0, hand))
        el.append(plane_zy(6.0, 10.0, HAND_Y - 6.0, HAND_Y + 0.4, x, hand))
    # The corn sheaves leaning on him all round.
    for i, (x, z, axis, angle) in enumerate(((3.0, 3.0, "x", 22.5), (13.0, 3.0, "z", -22.5), (13.0, 13.0, "x", -22.5), (3.0, 13.0, "z", 22.5),
                                            (8.0, 2.2, "x", 22.5), (8.0, 13.8, "x", -22.5))):
        rot = rotation((x, 1.5, z), axis, angle)
        el.append(plane_xy(x - 2.5, x + 2.5, 1.5, 16.5, z, sheaf, rot))
        el.append(plane_zy(z - 2.5, z + 2.5, 1.5, 16.5, x, sheaf, rot))
    el = separated(el)
    sc.models["whole"] = el
    for part in range(3):
        mine = [e for e in el if part * 16 <= (e["from"][1] + e["to"][1]) / 2 < (part + 1) * 16 or (part == 2 and (e["from"][1] + e["to"][1]) / 2 >= 48)]
        sc.models[f"harvest_effigy_{part}"] = shifted(mine, 0.0, -16.0 * part, 0.0)
    pumpkin = sc.piece("item_pumpkin", 24, 24, pumpkin_skin(VOICE_SKIN["tenor"], 17, ribs=4))
    head = [cube((8.0 - 6.0, NECK_TOP, 8.0 - 6.0), (8.0 + 6.0, NECK_TOP + 12.0, 8.0 + 6.0), faces(pumpkin, ALL6))]
    sc.models["item"] = shifted(el + head, 0.0, -16.0, 0.0)
    return sc


def effigy_cloak():
    """The effigy's cloak, for HarvestEffigyRenderer to draw in its dye: a cape down his back, its fronts hanging open
    from the shoulders, sides wrapping him, ragged sleeves under his arms and a collar round his neck, every sheet seen
    from both sides (on entity/harvest_effigy_cloak, a pale cloth the dye tints)."""
    sc = Sculpt("harvest_effigy_cloak", 2003, 64)
    back = sc.piece("back", 22, 40, cloth(1))
    front = sc.piece("front", 8, 28, cloth(2))
    side = sc.piece("side", 16, 30, cloth(3))
    sleeve = sc.piece("sleeve", 12, 12, cloth(4))
    collar = sc.piece("collar", 20, 4, cloth(5, tatters=False))
    el = [plane_xy(2.6, 13.4, 13.0, 33.6, 12.0, back),
          plane_xy(2.8, 6.6, 19.0, 33.4, 4.0, front), plane_xy(9.4, 13.2, 19.0, 33.4, 4.0, front),
          plane_zy(4.0, 12.0, 17.5, 33.4, 2.6, side), plane_zy(4.0, 12.0, 17.5, 33.4, 13.4, side),
          plane_xy(-3.5, 2.5, 25.0, 30.8, 8.0, sleeve), plane_xy(13.5, 19.5, 25.0, 30.8, 8.0, sleeve),
          cube((3.4, 32.6, 3.8), (12.6, 34.4, 12.2), faces(collar, ("north", "south", "east", "west")))]
    sc.models["harvest_effigy_cloak"] = el
    return sc


@fa.quietly
def effigy_ashes():
    """Effigy Ashes: a low drift of grey ash where he burnt, charred sticks of his frame crossed in it, the stump of his
    post and embers still glowing (they give off a little light of their own)."""
    sc = Sculpt(d20.ASHES["block"], 2004, 64)
    ash = sc.piece("ash", 28, 28, ashes(1, embers=0.02))
    ash_side = sc.piece("ash_side", 28, 4, ashes(2))
    stick = sc.piece("stick", 24, 2, charred(3))
    stump = sc.piece("stump", 4, 8, charred(4))
    ember = sc.piece("ember", 4, 4, solid(EMBER, 5, glossy=EMBER[5]))
    el = [cube((1.0, 0.0, 1.0), (15.0, 1.4, 15.0), faces(ash_side, ("north", "south", "east", "west"), up=ash)),
          cube((3.0, 1.4, 2.5), (13.0, 2.4, 13.0), faces(ash_side, ("north", "south", "east", "west"), up=ash)),
          cube((5.5, 2.4, 5.0), (10.5, 3.0, 10.5), faces(ash_side, ("north", "south", "east", "west"), up=ash)),
          cube((7.2, 2.0, 7.2), (8.8, 5.5, 8.8), faces(stump, ALL6))]
    for (x0, z0, x1, z1, y, angle) in ((2.0, 6.0, 14.0, 7.0, 2.0, 22.5), (3.0, 9.5, 13.0, 10.5, 1.8, -22.5), (7.0, 2.0, 8.0, 14.0, 2.3, 0)):
        rot = rotation(((x0 + x1) / 2, y, (z0 + z1) / 2), "y", angle) if angle else None
        el.append(cube((x0, y, z0), (x1, y + 1.0, z1), faces(stick, ALL6), rot))
    for x, z in ((5.0, 4.5), (10.5, 11.0), (11.5, 5.0)):
        el.append(cube((x - 0.6, 1.4, z - 0.6), (x + 0.6, 2.2, z + 0.6), faces(ember, ALL6), light=9))
    sc.models[d20.ASHES["block"]] = el
    return sc


# ---------------------------------------------------------------- 20. the Singing Pumpkins

def singing_pumpkin(voice):
    """A Singing Pumpkin: a ribbed pumpkin of the voice's own shape and colour, built of three overlapping boxes so it
    reads round, its face carved on the front (glowing eye holes, a closed smile; the client moves its pupils and opens
    its mouth), a stem on top, and the voice's mark: the bass a heavy brow and thick stem, the tenor a curling leaf, the
    alto a ribbon bow, the soprano a pale ghost skin and a curly tendril."""
    block = d20.VOICES[voice]["block"]
    skin = VOICE_SKIN[voice]
    width, height = d20.VOICES[voice]["shape"]
    front, eye_y, eye_dx, eye, mouth_y, mouth_w, _ = d20.VOICES[voice]["face"]
    sc = Sculpt(block, 2010 + list(d20.VOICES).index(voice), 64)
    x0, x1 = 8.0 - width / 2, 8.0 + width / 2
    texel = 2.0
    face_px = {"eye_x": [(width / 2 - eye_dx) * texel, (width / 2 + eye_dx) * texel], "eye_y": (height - 1.0 - eye_y) * texel,
               "eye": eye * texel, "mouth_x": width / 2 * texel, "mouth_y": (height - 1.0 - mouth_y) * texel, "mouth_w": mouth_w * texel}
    face = sc.piece("face", int(width * texel), int((height - 2.0) * texel), pumpkin_skin(skin, 1, ribs=4, face=face_px))
    side = sc.piece("side", int(width * texel), int((height - 2.0) * texel), pumpkin_skin(skin, 2, ribs=4))
    inner = sc.piece("inner", int((width - 2.0) * texel), int(height * texel), pumpkin_skin(skin, 3, ribs=3))
    cap = sc.piece("cap", int(width * texel), int(width * texel), ca.bevel(skin, len(skin) - 2, light=0, dark=2))
    stem_w = 2.4 if voice == "bass" else 1.6 if voice != "soprano" else 1.0
    stem = sc.piece("stem", 4, 8, ca.bevel(STEM, 2, sides="lr"))
    el = [cube((x0, 1.0, x0), (x1, height - 1.0, x1), faces(side, ("south", "east", "west"), north=face, up=cap, down=cap)),
          cube((x0 + 1.0, 0.0, x0 + 1.0), (x1 - 1.0, height, x1 - 1.0), faces(inner, ("south", "east", "west", "north"), up=cap, down=cap)),
          cube((x0 + 0.5, 0.4, x0 + 0.6), (x1 - 0.5, height - 0.4, x1 - 0.5), faces(inner, ("east", "west", "south"), up=cap, down=cap)),
          cube((8.0 - stem_w / 2, height, 8.0 - stem_w / 2), (8.0 + stem_w / 2, height + 2.2, 8.0 + stem_w / 2), faces(stem, ALL6)),
          cube((8.0 - stem_w / 2, height + 1.6, 8.0 - stem_w / 2), (8.0 + stem_w / 2 + 1.4, height + 2.4, 8.0 + stem_w / 2), faces(stem, ALL6))]
    if voice == "bass":
        brow = sc.piece("brow", 8, 2, ca.bevel([skin[0], skin[1]], 0, light=1, dark=0))
        for s in (-1, 1):
            el.append(cube((8.0 + s * eye_dx - 2.0, eye_y + eye / 2 + 0.2, front - 0.3), (8.0 + s * eye_dx + 2.0, eye_y + eye / 2 + 1.0, front + 0.5),
                           faces(brow, ALL6), rotation((8.0 + s * eye_dx, eye_y + eye / 2 + 0.6, front), "z", -22.5 * s)))
    elif voice == "tenor":
        leaf = sc.piece("leaf", 10, 10, fa.leaf(LEAF_GREEN, "heart", 7))
        el.append(fa.plane_xz(8.0, 13.0, 4.0, 9.0, height + 0.3, leaf, rotation((8.0, height + 0.3, 6.5), "z", 22.5)))
    elif voice == "alto":
        ribbon = sc.piece("ribbon", 6, 4, ca.bevel(pal("4a1a5a", "6a2a80", "8a40a6", "aa60c4"), 2))
        el += [cube((7.0, height + 0.4, 6.8), (9.0, height + 1.4, 9.2), faces(ribbon, ALL6)),
               cube((4.6, height + 0.6, 7.4), (7.0, height + 2.2, 8.6), faces(ribbon, ALL6), rotation((7.0, height + 1.4, 8.0), "z", 22.5)),
               cube((9.0, height + 0.6, 7.4), (11.4, height + 2.2, 8.6), faces(ribbon, ALL6), rotation((9.0, height + 1.4, 8.0), "z", -22.5))]
    else:
        lash = sc.piece("lash", 4, 2, ca.bevel(pal("1a1410", "2a2018"), 0, light=1, dark=0))
        tendril = sc.piece("tendril", 2, 2, ca.bevel(LEAF_GREEN, 3))
        for s in (-1, 1):
            el.append(cube((8.0 + s * eye_dx - 1.4, eye_y + eye / 2, front - 0.2), (8.0 + s * eye_dx + 1.4, eye_y + eye / 2 + 0.5, front + 0.2),
                           faces(lash, ALL6)))
        for i, (dx, dy) in enumerate(((1.0, 2.4), (2.0, 3.0), (2.6, 2.4), (2.2, 1.8), (1.6, 2.0))):
            el.append(cube((8.0 + dx - 0.3, height + dy - 0.3, 7.7), (8.0 + dx + 0.3, height + dy + 0.3, 8.3), faces(tendril, ALL6)))
    sc.models[block] = el
    return sc


def face_texture():
    """The singing faces' moving parts (entity/singing_pumpkin_face, 32 x 32): an eye hole wide with candlelight (top
    left), the open mouth, a round O glowing hot in the middle and dark at its rim (top right), and a pupil (below)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    p = Px(img)
    for y in range(16):
        for x in range(16):
            # An upright triangle, base at the bottom.
            half = (y + 1) / 16 * 8
            if abs(x - 7.5) <= half:
                depth = abs(x - 7.5) / max(1.0, half)
                p.put(x, y, GLOW[5 if depth < 0.3 and y > 6 else 4 if depth < 0.6 else 3 if depth < 0.85 else 2])
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 7.5, (y - 7.5) / 7.5)
            if d <= 1.0:
                p.put(16 + x, y, GLOW[5] if d < 0.35 else GLOW[4] if d < 0.6 else GLOW[2] if d < 0.85 else GLOW[0])
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d <= 3.6:
                p.put(x, 16 + y, (18, 10, 4) if d > 1.0 or (x, y) != (2, 2) else (255, 240, 200))
    p.put(2, 18, (255, 240, 200))
    return img


# ---------------------------------------------------------------- 18. the garlands

def garland_texture(vine):
    """A garland strand's texture (entity/pumpkin_vine_garland or autumn_leaf_garland, 64 x 16, read in quarters by
    StringLightsRenderer): the cord, a leaf, a second leaf (the vine's little pumpkin), and the bulb. The autumn leaves
    are pale, for the renderer tints each its own autumn colour."""
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    rng = random.Random(2020 if vine else 2021)
    for x in range(16):
        for y in range(16):
            if vine:
                img.putpixel((x, y), tuple(shade(VINE, 1 + ((x + y) // 3) % 3)) + (255,))
            else:
                img.putpixel((x, y), tuple(shade(TWINE, 1 + ((x + y * 2) // 2) % 3)) + (255,))

    def leaf(ox, palette, lobes, seed):
        r = random.Random(seed)
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 8.5
                angle = math.atan2(dy, dx)
                radius = 6.8 * (0.72 + 0.28 * abs(math.cos(angle * lobes / 2))) if lobes else 6.8 * (1 - 0.35 * abs(math.sin(angle)))
                if math.hypot(dx, dy) <= radius and y >= 1:
                    vein = abs(dx) < 0.6 or abs(dy - dx * 0.6) < 0.5 or abs(dy + dx * 0.6) < 0.5
                    k = len(palette) - 2 if vein else len(palette) // 2 + r.choice((0, 0, -1, 1))
                    img.putpixel((ox + x, y), tuple(shade(palette, k)) + (255,))
        for y in range(0, 3):
            img.putpixel((ox + 7, y), tuple(STEM[2]) + (255,))

    pale = pal("a89a78", "c4b894", "dcd2b0", "eee6cc", "faf6e8")
    if vine:
        leaf(16, LEAF_GREEN, 5, 1)
        skin = VOICE_SKIN["tenor"]
        for y in range(16):
            for x in range(16):
                k = 3 - (1 if x % 4 == 0 else 0) + (1 if y < 4 else 0) - (1 if y > 11 else 0) + rng.choice((0, 0, -1))
                img.putpixel((32 + x, y), tuple(shade(skin, k)) + (255,))
    else:
        leaf(16, pale, 5, 2)
        leaf(32, pale, 0, 3)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            img.putpixel((48 + x, y), tuple(GLOW[4] if d < 4 else GLOW[3] if d < 7 else GLOW[2]) + (255,))
    return img


def garland_item(vine):
    """A coiled garland for the inventory: a loop of vine or twine with leaves and a pumpkin or two."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = Px(img)
    rng = random.Random(2022 if vine else 2023)
    cord = VINE if vine else TWINE
    for t in range(64):
        a = t / 64 * 2 * math.pi
        p.put(7.5 + 5.6 * math.cos(a), 8 + 4.6 * math.sin(a), cord[1 + t % 2])
    colours = [LEAF_GREEN] if vine else [pal("7a1a0a", "a8301a", "c8461e"), pal("8a3a08", "c05a10", "e8801c"), pal("8a6a10", "c09a1c", "e8c040")]
    for i in range(9):
        a = i / 9 * 2 * math.pi + 0.3
        cx, cy = 7.5 + 5.6 * math.cos(a), 8 + 4.6 * math.sin(a)
        c = colours[i % len(colours)]
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            p.put(cx + dx, cy + dy, c[1 if (dx + dy) % 2 else 2])
    if vine:
        for cx, cy in ((4, 13), (11, 12)):
            for dx in (-1, 0, 1):
                for dy in (-1, 0):
                    p.put(cx + dx, cy + dy, VOICE_SKIN["tenor"][3 if dy else 2])
            p.put(cx, cy - 2, STEM[2])
    else:
        p.put(7, 12, GLOW[4])
        p.put(8, 12, GLOW[3])
    return img


def hearth_ash_item():
    """Hearth Ash: a soft rounded heap of grey ash, paler on top where it catches the light, with a few dark flecks."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = Px(img)
    for y in range(6, 15):
        half = 7.2 * math.sqrt((y - 5.5) / 9.0)
        for x in range(16):
            dx = x + 0.5 - 8.0
            if abs(dx) <= half:
                light = (1.0 - (y - 6) / 9.0) * 2.4 - dx / 7.0
                c = shade(ASH, 2 + int(round(light)))
                if (x, y) in ((6, 10), (10, 9), (8, 12), (4, 13), (11, 13)):
                    c = CHAR[1]
                p.put(x, y, c)
    p.outline(ASH[0])
    return img


# ---------------------------------------------------------------- building, quads and files

_built = {}


def build(name):
    """The Sculpt a block's models are drawn from (by its block's ID, or "harvest_effigy_cloak")."""
    if name not in _built:
        builders = {d20.FARM_STAND["block"]: farm_stand, d20.EFFIGY["block"]: harvest_effigy, d20.ASHES["block"]: effigy_ashes,
                    "harvest_effigy_cloak": effigy_cloak,
                    **{spec["block"]: (lambda voice=voice: singing_pumpkin(voice)) for voice, spec in d20.VOICES.items()}}
        _built[name] = builders[name]()
    return _built[name]


def decor20_quads():
    """The renderer's models: the effigy's cloak (tinted with its dye), cut out round its tatters."""
    cloak = build("harvest_effigy_cloak")
    return {"harvest_effigy_cloak": [dict(q, cutout=True) for q in quads(d17d.single_sheets(cloak.models["harvest_effigy_cloak"]),
                                                                         {"p": "entity/harvest_effigy_cloak"})]}


STAND_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, -1.5, 0], "scale": [0.36, 0.36, 0.36]},
              "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
              "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]},
              "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [0.4, 0.4, 0.4]},
              "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]},
              "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]}}
EFFIGY_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, -0.5, 0], "scale": [0.3, 0.3, 0.3]},
               "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
               "fixed": {"rotation": [0, 0, 0], "translation": [0, -1, 0], "scale": [0.3, 0.3, 0.3]},
               "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [0.4, 0.4, 0.4]},
               "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.2, 0.2, 0.2]},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.2, 0.2, 0.2]}}


def _image_of(name):
    """The Sculpt texture a model's "#p" (block/<name>) is drawn from, for fa.closing_writer; None for another."""
    try:
        return build(name).atlas.img
    except KeyError:
        return None


def assets(root, write, lang):
    # Every block and item model is closed: no face a box leaves out shows a hole (docs/ART_DIRECTION.md).
    write = fa.closing_writer(write, _image_of)
    models = root / "models" / "block"
    states = root / "blockstates"
    items = root / "models" / "item"
    lang.update({f"block.{MOD}.{block}": display for block, display in d20.names().items()})
    lang.update({f"item.{MOD}.{item}": display for item, display in d20.item_names().items()})
    lang.update(d20.MESSAGES)

    def model(name, block):
        write(models / f"{name}.json", fa.model(block, build(block).models[name]))

    def turned(model_name, facing):
        return {"model": rid(f"block/{model_name}"), **({"y": FACINGS[facing]} if FACINGS[facing] else {})}

    def item(block, elements, display):
        write(items / f"{block}.json", fa.model(block, elements, display))

    booleans = ("false", "true")
    stand = d20.FARM_STAND["block"]
    for part in range(2):
        model(f"{stand}_{part}", stand)
    write(states / f"{stand}.json", {"variants": {f"facing={f},lit={l},part={p}": turned(f"{stand}_{p}", f)
                                                  for f in FACINGS for l in booleans for p in range(2)}})
    item(stand, build(stand).models["item"], STAND_ITEM)

    effigy = d20.EFFIGY["block"]
    for part in range(3):
        model(f"{effigy}_{part}", effigy)
    write(states / f"{effigy}.json", {"variants": {f"cloak={c},facing={f},lit={l},part={p}": turned(f"{effigy}_{p}", f)
                                                   for c in DYES for f in FACINGS for l in booleans for p in range(3)}})
    item(effigy, build(effigy).models["item"], EFFIGY_ITEM)
    ashes_ = d20.ASHES["block"]
    model(ashes_, ashes_)
    write(states / f"{ashes_}.json", {"variants": {"": {"model": rid(f"block/{ashes_}")}}})
    item(ashes_, build(ashes_).models[ashes_], fa.PLANT_DISPLAY)

    for voice, spec in d20.VOICES.items():
        block = spec["block"]
        model(block, block)
        write(states / f"{block}.json", {"variants": {f"facing={f},note={n},powered={p}": turned(block, f)
                                                      for f in FACINGS for n in range(d20.CHOIR["notes"]) for p in booleans}})
        item(block, build(block).models[block], fa.PLANT_DISPLAY)

    for item_id in [g["item"] for g in d20.GARLANDS.values()] + [d20.HEARTH_ASH["item"]]:
        write(items / f"{item_id}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item_id}")}})
    for block in d20.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
    write(root / "decor20_quads.json", decor20_quads())


def loot(out, write):
    from decor_data import self_drop
    for block in d20.blocks():
        if block == d20.ASHES["block"]:
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
                {"type": "minecraft:set_count", "count": d20.ASHES["ash_yield"], "add": False}, {"type": "minecraft:explosion_decay"}],
                "name": rid(d20.HEARTH_ASH["item"])}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})
            continue
        if block in (d20.FARM_STAND["block"], d20.EFFIGY["block"]):
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
                "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                    {"type": "minecraft:match_block", "blocks": rid(block), "state": {"part": "0"}}]},
                "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})
            continue
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    for block in [d20.FARM_STAND["block"]] + [spec["block"] for spec in d20.VOICES.values()]:
        tags.add("block", "minecraft:mineable/axe", rid(block))
    tags.add("block", "minecraft:mineable/hoe", rid(d20.EFFIGY["block"]))
    tags.add("block", "minecraft:mineable/shovel", rid(d20.ASHES["block"]))


def textures():
    """(kind, name) -> image for each block's texture, the client's face, garland and cloak textures, and the items."""
    out = {}
    for block in d20.blocks():
        out[("block", block)] = build(block).atlas.img
    out[("entity", "harvest_effigy_cloak")] = build("harvest_effigy_cloak").atlas.img
    out[("entity", "singing_pumpkin_face")] = face_texture()
    out[("entity", "pumpkin_vine_garland")] = garland_texture(True)
    out[("entity", "autumn_leaf_garland")] = garland_texture(False)
    out[("item", d20.GARLANDS["pumpkin_vine"]["item"])] = garland_item(True)
    out[("item", d20.GARLANDS["autumn_leaves"]["item"])] = garland_item(False)
    out[("item", d20.HEARTH_ASH["item"])] = hearth_ash_item()
    return out
