"""Material painters in the manner of the vanilla block textures, so Jugcraft's blocks sit beside the world's own
without standing out (docs/ART_DIRECTION.md, "Texturing: keep it clean"; the owner's 5 October 2026 note that stone
should look like stone, cobblestone or stone bricks, not flat grey and not salt-and-pepper noise).

What the vanilla textures do, and what these painters copy in manner only (no Mojang texture is read, traced or
recoloured; every pixel here comes from code):
- **A short palette, mostly mid-tones.** Five or six tones per material; most pixels are the middle two, the darkest and
  lightest are rare accents.
- **Clumped variation, not per-pixel noise.** Tones come in small patches two to four pixels across (smooth value noise
  at two scales, snapped to the palette), so a face reads as a surface with a little grain, never as static.
- **Shape from light.** Stones, bricks and boards are lit along their top and left edges and shaded along their
  bottom and right, with dark joints between them.
- **Everything tiles.** Every pattern repeats every `size` pixels, so neighbouring blocks join up seamlessly.

Painters take a seed and return a function that paints a `Px` (tools/flora_art.py) or a Canvas-like object with
`px(x, y, colour)`; `img(painter, size)` makes an image directly.
"""
import math
import random

from PIL import Image


def _put(p, x, y, c):
    if hasattr(p, "put"):
        p.put(x, y, c)
    else:
        p.px(x, y, c)


def _size(p):
    if hasattr(p, "w"):
        return p.w, p.h
    return 16, 16


def img(painter, size=16):
    """A size x size image painted by `painter`."""
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    class _P:
        w = h = size

        def put(self, x, y, c):
            x, y = int(x) % size, int(y) % size
            image.putpixel((x, y), tuple(c[:3]) + (255,))
    painter(_P())
    return image


class Field:
    """Smooth periodic value noise (0 to 1) on a `w` x `h` tile: features about `cell` pixels across (and `cell_y`
    down, when given, for streaks), wrapping at the tile's edges so the texture tiles."""

    def __init__(self, w, h, seed, cell, cell_y=None):
        self.w, self.h, self.cell = w, h, cell
        self.nx, self.ny = max(1, round(w / cell)), max(1, round(h / (cell_y or cell)))
        rng = random.Random(seed)
        self.grid = [[rng.random() for _ in range(self.nx)] for _ in range(self.ny)]

    def __call__(self, x, y):
        gx, gy = x / self.w * self.nx, y / self.h * self.ny
        ix, iy = int(math.floor(gx)), int(math.floor(gy))
        tx, ty = gx - ix, gy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        g = self.grid
        a, b = g[iy % self.ny][ix % self.nx], g[iy % self.ny][(ix + 1) % self.nx]
        c, d = g[(iy + 1) % self.ny][ix % self.nx], g[(iy + 1) % self.ny][(ix + 1) % self.nx]
        top, bottom = a + (b - a) * tx, c + (d - c) * tx
        return top + (bottom - top) * ty


def grain(w, h, seed, fine=2.0, coarse=5.0, mix=0.55):
    """Two octaves of periodic value noise mixed: small clumps over gentle patches (0 to 1)."""
    f1, f2 = Field(w, h, seed, fine), Field(w, h, seed + 7, coarse)
    return lambda x, y: mix * f1(x, y) + (1 - mix) * f2(x, y)


def tone(value, palette, middle=0.5, spread=1.0):
    """Snaps a 0..1 value to `palette` (darkest first), weighted towards its middle tones: the extremes need the value
    far from the middle, so they appear as rare accents."""
    n = len(palette)
    v = middle + (value - 0.5) * spread
    # A soft S-curve keeps most values near the middle tones.
    v = 0.5 + (v - 0.5) * 1.35
    k = int(round(max(0.0, min(1.0, v)) * (n - 1)))
    return palette[max(0, min(n - 1, k))]


def _lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def surface(palette, seed=1, weights=None, spread=0.8, size=(16, 16)):
    """(x, y) -> colour: a plain surface of `palette` in small clumps, in the vanilla manner: one ground tone (the
    commonest by `weights`, one per colour in the order given; otherwise the middle, or the lighter of the two middle
    tones) covering most of the face, with clumps of the tones either side of it as accents. The colours are put in
    order darkest first; a larger `spread` makes the accents commoner."""
    order = sorted(range(len(palette)), key=lambda i: _luma(palette[i]))
    ordered = [palette[i] for i in order]
    n = len(ordered)
    counts = [weights[i] for i in order] if weights else [1] * n
    top = max(counts)
    modes = [k for k, count in enumerate(counts) if count == top]
    ground = int(math.floor(sum(modes) / len(modes) + 0.51))
    g = grain(size[0], size[1], seed)
    return lambda x, y: ordered[max(0, min(n - 1, ground + int(round((g(x, y) - 0.5) * spread * 4.0))))]


def fill(c, x0, y0, x1, y1, palette, seed=1, weights=None, spread=0.8):
    """Fills x0..x1, y0..y1 of a Canvas (or anything with `px`) with `surface(palette, ...)`: the clumped replacement for
    a fill of random pixels."""
    size = c.img.size if hasattr(c, "img") else _size(c)
    s = surface(palette, seed, weights, spread, size)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            _put(c, x, y, s(x, y))


def wobble(seed, amount, size=(16, 16)):
    """(x, y) -> -amount, 0 or +amount in soft clumps: a gentle variation for surfaces shaded by formula (paper, glass,
    linen) in place of a random amount at every pixel."""
    g = grain(size[0], size[1], seed)
    return lambda x, y: amount if g(x, y) > 0.6 else -amount if g(x, y) < 0.4 else 0.0


# ---------------------------------------------------------------- stone

def stone(palette, seed=1, fine=2.0, coarse=5.0, cracks=3, spread=1.0, middle=0.5):
    """Plain stone, as vanilla stone: a mid-tone surface of small clumps, a few darker short cracks running roughly
    across it."""
    def paint(p):
        w, h = _size(p)
        g = grain(w, h, seed, fine, coarse)
        for y in range(h):
            for x in range(w):
                _put(p, x, y, tone(g(x, y), palette, middle, spread))
        rng = random.Random(seed + 31)
        for _ in range(cracks):
            x, y = rng.randrange(w), rng.randrange(h)
            length = rng.randint(2, 4)
            for i in range(length):
                _put(p, (x + i) % w, (y + (1 if i == length - 1 and rng.random() < 0.5 else 0)) % h, palette[1])
    return paint


def speckled(palette, specks, seed=1, density=0.09, fine=2.0, coarse=5.0):
    """Speckled stone, as vanilla granite, diorite and andesite: a clumped base with small specks (one or two pixels)
    of the `specks` colours scattered evenly, never touching each other."""
    def paint(p):
        w, h = _size(p)
        stone(palette, seed, fine, coarse, cracks=0, spread=0.8)(p)
        rng = random.Random(seed + 57)
        taken = set()
        for _ in range(int(w * h * density)):
            x, y = rng.randrange(w), rng.randrange(h)
            if any(((x + dx) % w, (y + dy) % h) in taken for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                continue
            c = specks[rng.randrange(len(specks))]
            _put(p, x, y, c)
            taken.add((x, y))
            if rng.random() < 0.35:
                _put(p, (x + 1) % w, y, c)
                taken.add(((x + 1) % w, y))
    return paint


def _cells(w, h, seed, count):
    """`count` points scattered over a w x h tile (a jittered grid, so stones come out evenly sized)."""
    rng = random.Random(seed)
    cols = max(1, round(math.sqrt(count * w / h)))
    rows = max(1, math.ceil(count / cols))
    pts = []
    for r in range(rows):
        for c in range(cols):
            pts.append(((c + 0.2 + rng.random() * 0.6) * w / cols, (r + 0.2 + rng.random() * 0.6) * h / rows))
    return pts


def _nearest(pts, x, y, w, h):
    """The two nearest points to (x, y) on a wrapping tile: (index, distance) of the nearest and the second distance."""
    best, second, index = 1e9, 1e9, 0
    for i, (px, py) in enumerate(pts):
        dx = min(abs(x - px), w - abs(x - px))
        dy = min(abs(y - py), h - abs(y - py))
        d = math.hypot(dx, dy)
        if d < best:
            best, second, index = d, best, i
        elif d < second:
            second = d
    return index, best, second


def cobble(palette, seed=1, count=9, joint=None):
    """Cobblestone, as vanilla: rounded stones of a few tones each, lit along their upper left and shaded along their
    lower right, set in dark joints."""
    def paint(p):
        w, h = _size(p)
        pts = _cells(w, h, seed, count)
        rng = random.Random(seed + 3)
        base = [rng.choice((2, 2, 3, 3, 1)) for _ in pts]
        g = grain(w, h, seed + 11, 1.6, 4.0)
        n = len(palette)
        for y in range(h):
            for x in range(w):
                i, d1, d2 = _nearest(pts, x + 0.5, y + 0.5, w, h)
                if d2 - d1 < 1.0:
                    _put(p, x, y, joint or palette[0])
                    continue
                px, py = pts[i]
                dx = ((x + 0.5 - px + w / 2) % w) - w / 2
                dy = ((y + 0.5 - py + h / 2) % h) - h / 2
                k = base[i] + (1 if dx + dy < -1.8 else 0) - (1 if dx + dy > 2.2 or d2 - d1 < 1.8 and dx + dy > 0 else 0)
                k += 1 if g(x, y) > 0.72 else -1 if g(x, y) < 0.22 else 0
                _put(p, x, y, palette[max(1, min(n - 1, k))])
    return paint


def bricks(palette, seed=1, rows=2, cols=1, mortar=None, offset=True):
    """Stone bricks, as vanilla: `rows` courses of `cols` bricks across the tile (each course offset by half a brick),
    each brick a clumped stone surface lit along its top and left edge and shaded along its bottom and right, set in a
    dark joint along its bottom and right."""
    def paint(p):
        w, h = _size(p)
        stone(palette, seed, 2.0, 5.0, cracks=1, spread=0.75)(p)
        course = h // rows
        length = w // cols
        n = len(palette)
        for y in range(h):
            r = y // course
            for x in range(w):
                u = (x + (length // 2 if offset and r % 2 else 0)) % w
                within_x = u % length
                within_y = y % course
                if within_y == course - 1 or within_x == length - 1:
                    _put(p, x, y, mortar or palette[0])
                elif within_y == 0 or within_x == 0:
                    _put(p, x, y, palette[n - 2] if (x + y) % 5 else palette[n - 1])
                elif within_y == course - 2 or within_x == length - 2:
                    _put(p, x, y, palette[1])
    return paint


# ---------------------------------------------------------------- earth

def dirt(palette, seed=1, pebbles=None, count=5):
    """Earth, as vanilla dirt: small clumps of a few browns, with a few pebbles (two-pixel specks) of `pebbles`."""
    def paint(p):
        w, h = _size(p)
        g = grain(w, h, seed, 1.8, 4.5, 0.65)
        for y in range(h):
            for x in range(w):
                _put(p, x, y, tone(g(x, y), palette))
        if pebbles:
            rng = random.Random(seed + 5)
            for _ in range(count):
                x, y = rng.randrange(w), rng.randrange(h)
                _put(p, x, y, pebbles[0])
                _put(p, (x + 1) % w, y, pebbles[-1])
    return paint


def gravel(palette, seed=1, count=16):
    """Gravel, as vanilla: small rounded pebbles of several tones packed together, each lit at its top, dark between."""
    return cobble(palette, seed, count)


# ---------------------------------------------------------------- wood

def planks(palette, seed=1, boards=4, vertical=False, joint=True):
    """Planks, as vanilla: `boards` boards across the tile, each a warm mid-tone with darker grain streaks running along
    it and a lit edge; dark seams between boards and an end joint in every other board."""
    def paint(p):
        w, h = _size(p)
        along, across = (h, w) if vertical else (w, h)
        board = across // boards
        rng = random.Random(seed)
        n = len(palette)
        cells = {}
        for b in range(boards):
            base = n // 2 + (0 if b % 2 else -1 if n > 4 else 0)
            for i in range(along):
                for j in range(board):
                    cells[(i, b * board + j)] = palette[base]
            # Grain: a few darker streaks along the board, and a lighter one.
            for _ in range(3):
                j = rng.randrange(1, max(2, board - 1))
                start = rng.randrange(along)
                length = rng.randint(3, along // 2)
                for i in range(length):
                    cells[((start + i) % along, b * board + j)] = palette[base - 1]
            j = rng.randrange(1, max(2, board - 1))
            start = rng.randrange(along)
            for i in range(rng.randint(2, 5)):
                cells[((start + i) % along, b * board + j)] = palette[min(n - 1, base + 1)]
            for i in range(along):
                cells[(i, b * board)] = palette[min(n - 1, base + 1)]
                cells[(i, b * board + board - 1)] = palette[0]
            if joint and b % 2:
                end = rng.randrange(along)
                for j in range(board - 1):
                    cells[(end, b * board + j)] = palette[1]
        for (i, j), c in cells.items():
            if vertical:
                _put(p, j, i, c)
            else:
                _put(p, i, j, c)
    return paint


def log_bark(palette, seed=1):
    """Bark, as a vanilla log's side: vertical ridges of a few tones with darker furrows between."""
    def paint(p):
        w, h = _size(p)
        g = grain(w, h, seed, 2.0, 6.0)
        rng = random.Random(seed)
        furrows = sorted(rng.sample(range(w), max(2, w // 4)))
        for y in range(h):
            for x in range(w):
                c = tone(g(x * 2.5, y * 0.5), palette)
                if x in furrows and (y + x) % 7 not in (0, 1):
                    c = palette[0]
                _put(p, x, y, c)
    return paint


def streaks(palette, seed=1, vertical=True, across=1.5, along=8.0, spread=1.0):
    """Fibres, as on the side of a vanilla hay bale: tones in thin streaks a pixel or two across and several long,
    running down the tile (or across it), the palette's middle tones commonest."""
    def paint(p):
        w, h = _size(p)
        cx, cy = (across, along) if vertical else (along, across)
        f1, f2 = Field(w, h, seed, cx, cy), Field(w, h, seed + 7, cx * 2, cy * 1.5)
        for y in range(h):
            for x in range(w):
                _put(p, x, y, tone(0.65 * f1(x, y) + 0.35 * f2(x, y), palette, spread=spread))
    return paint


def heap(palettes, seed=1, count=10, joint=None, weights=None):
    """Small things heaped together, as vanilla gravel but in colours: `count` rounded pieces, each in one of
    `palettes` (dark, mid, light), lit along its upper left and shaded along its lower right, dark between."""
    def paint(p):
        w, h = _size(p)
        pts = _cells(w, h, seed, count)
        rng = random.Random(seed + 3)
        picks = [i for i, n in enumerate(weights or [1] * len(palettes)) for _ in range(n)]
        kinds = [palettes[rng.choice(picks)] for _ in pts]
        for y in range(h):
            for x in range(w):
                i, d1, d2 = _nearest(pts, x + 0.5, y + 0.5, w, h)
                if d2 - d1 < 0.9:
                    _put(p, x, y, joint or (20, 16, 14))
                    continue
                px, py = pts[i]
                dx = ((x + 0.5 - px + w / 2) % w) - w / 2
                dy = ((y + 0.5 - py + h / 2) % h) - h / 2
                k = 1 + (1 if dx + dy < -1.5 else 0) - (1 if dx + dy > 1.8 and d2 - d1 < 2.2 else 0)
                _put(p, x, y, kinds[i][max(0, min(2, k))])
    return paint


# ---------------------------------------------------------------- metal and cloth

def metal(palette, seed=1, panels=True, rivets=False):
    """Metal sheet, as a vanilla iron or gold block: a near-flat bright face with soft clumps one tone apart, a lit top
    and left edge, a dark bottom and right one, and a faint seam across the middle."""
    def paint(p):
        w, h = _size(p)
        n = len(palette)
        g = grain(w, h, seed, 2.0, 4.0, 0.6)
        mid = n // 2
        for y in range(h):
            for x in range(w):
                v = g(x, y)
                k = mid + (1 if v > 0.76 else -1 if v < 0.22 else 0)
                if y == 0 or x == 0:
                    k = n - 1
                elif y == h - 1 or x == w - 1:
                    k = 1
                elif panels and y == h // 2:
                    k = mid - 1
                elif panels and y == h // 2 + 1:
                    k = mid + 1
                _put(p, x, y, palette[max(0, min(n - 1, k))])
        if rivets:
            for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
                _put(p, x, y, palette[n - 1])
                _put(p, x + 1, y + 1, palette[1])
    return paint


def cloth(palette, seed=1):
    """Cloth, as vanilla wool: a soft surface of low-contrast clumps in two or three neighbouring tones."""
    def paint(p):
        w, h = _size(p)
        g = grain(w, h, seed, 1.7, 4.0, 0.6)
        for y in range(h):
            for x in range(w):
                _put(p, x, y, tone(g(x, y), palette, spread=0.6))
    return paint


# ---------------------------------------------------------------- weathering, in the manner of mossy cobblestone

def moss_over(p, moss, seed=1, amount=0.3, ground=0, lichen=None, lichen_count=0):
    """Paints moss over what is drawn on `p` in irregular clumps, as on vanilla mossy cobblestone: where a clumped field
    passes `1 - amount` (more towards the bottom rows when `ground` is set: the bottom `ground` rows are green), in
    three greens, lighter at the top of each clump. Lichen: `lichen_count` small rosettes of the `lichen` colours."""
    w, h = _size(p)
    g = grain(w, h, seed, 1.8, 4.5, 0.6)
    n = len(moss)
    for y in range(h):
        for x in range(w):
            v = g(x, y)
            if ground:
                v += max(0.0, (y - (h - 1 - ground)) / max(1, ground)) * 0.55
            if v > 1 - amount:
                above = g(x, y - 1) + (max(0.0, (y - 1 - (h - 1 - ground)) / max(1, ground)) * 0.55 if ground else 0)
                k = n - 1 if above <= 1 - amount else n - 2 if v < 1 - amount + 0.12 else n - 3
                _put(p, x, y, moss[max(0, k)])
    if lichen and lichen_count:
        rng = random.Random(seed + 91)
        for _ in range(lichen_count):
            x, y = rng.randrange(w), rng.randrange(h - ground if ground else h)
            _put(p, x, y, lichen[0])
            for dx, dy in ((1, 0), (0, 1)):
                _put(p, (x + dx) % w, (y + dy) % h, lichen[-1])


def grime_over(p, grime, seed=1, streaks=3, strength=0.3):
    """Grime washed down from the top in soft, uneven streaks a pixel or two wide."""
    w, h = _size(p)
    rng = random.Random(seed)
    get = getattr(p, "get", None)
    for _ in range(streaks):
        x = rng.randrange(w)
        length = rng.randint(h // 4, h // 2)
        for y in range(length):
            for xx in (x, x + (1 if rng.random() < 0.4 else 0)):
                c = get(xx % w, y) if get else None
                if c:
                    t = strength * (1 - y / length)
                    _put(p, xx % w, y, _lerp(c[:3], grime, t))
