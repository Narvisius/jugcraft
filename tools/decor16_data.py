"""Generated data for Halloween decorations batch 16, the haunted house's props (tools/decor16.py): sculpted models on the
toolkit of tools/flora_art.py (each block one 64 x 64 texture painted here), blockstates, items, names, loot and tags,
and the quads the client draws the Flying Eyeball from (assets/jugcraft/decor16_quads.json). The five harvest plushes
are midway prizes: tools/midway_data.py asks here for their models.

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import math
import random

from PIL import Image

import cute_art as ca
import decor16 as d16
import flora_art as fa
from flora_art import SIDES4, Px, Sculpt, column, cube, pal, plane_xy, plane_zy, rotation, shade, solid, strip
from decor6_data import quads

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
FACE_TURNS = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}

SCLERA = pal("b9a49a", "d8c8bd", "ece0d6", "f8f1ea", "fffcf8")
# The eyes the owner looks at closely (the Flying Eyeball and the Specimen Jar's eye): a warmer, softer ivory than pure
# white, so the eye sits in its scene instead of glowing out of it (5 October 2026).
WARM_SCLERA = pal("9a8578", "bba798", "d6c6b6", "e6dacb", "f1e8dc")
VEIN = pal("7a0e14", "a3161e", "c8323a")
IRIS = pal("06301c", "0b5230", "167a45", "2fa45e", "5ccf7e", "a6f0b4")
# The Specimen Jar's eye is blue, to stand out against its green fluid.
BLUE_IRIS = pal("0a1a44", "143272", "22519e", "3a76c8", "66a2e6", "b0d6fa")
PUPIL = pal("040406", "101014")
WING = pal("5a0c0c", "8a1a14", "b52e1c", "d9482a", "f06a3a", "ff9a5c")
WING_BONE = pal("3a0606", "5c0e0e", "7e1a14")
NERVE = pal("6e1c26", "9a3040", "c25060", "e08090")
IVORY = pal("8e7f62", "b3a383", "cfc2a3", "e4dbc2", "f3ecd9", "fffaf0")
BLACK_WAX = pal("0b0a0d", "16141a", "211e27", "2e2a36", "403b4c", "585266")
WICK = pal("0e0b08", "241c14", "3a2e22")
EMBER = pal("8a2a08", "d8601a", "ffb040")
WEB = (232, 236, 240)
SKIN = pal("1d4a22", "2c6a2e", "43903c", "5fb04a", "7ccb5e", "9fe07e")
SKIN_DARK = pal("10261a", "1a3a24", "275030")
HAIR = pal("07080a", "101318", "1a2028", "26303a", "34424e")
STITCH = pal("140c0a", "2a1c16", "40302a")
MOUTH = pal("1c0608", "3a0e12", "5a1a1e", "7a2a2a")
TOOTH = pal("8e866a", "c0b892", "e2dcbc", "f6f2dc")
METAL = pal("2a2c30", "44474e", "62666e", "868b94", "b0b5bc")
EYE_GLOW = pal("6a8a10", "a8d020", "d8f040", "f4ff9a", "ffffe0")
EYE_DULL = pal("5a5636", "8a8458", "b4ae80", "d6d0a6")
SHIRT = pal("0e0e12", "18181e", "24242c")


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


# ---------------------------------------------------------------- painters

def felt(palette, seed=1, seam=True, k=None):
    """Stuffed felt: one smooth colour, lit along its top and shaded along its bottom, with a dashed seam stitched a texel
    inside the edge."""
    def paint(p):
        n = len(palette)
        base = n // 2 if k is None else k
        ca.soft(palette, base, edge=0, top=0.2, bottom=0.2)(p)
        if seam and p.w > 4 and p.h > 4:
            for x in range(1, p.w - 1):
                if x % 3 != 2:
                    p.put(x, 1, shade(palette, base + 2))
                    p.put(x, p.h - 2, shade(palette, base - 2))
            for y in range(1, p.h - 1):
                if y % 3 != 2:
                    p.put(1, y, shade(palette, base + 1))
                    p.put(p.w - 2, y, shade(palette, base - 1))
    return paint


def sclera(seed=1, veins=5, toward=None, palette=SCLERA):
    """The white of the eye, clean and glossy: bright white with a soft grey shade round its lower edge, and a few smooth
    red veins curling in from the edges (toward the iris, `toward` (x, y) in 0..1), each a single even line. `palette`
    (five shades, darkest first) gives the white."""
    def paint(p):
        w, h = p.w, p.h
        for y in range(h):
            for x in range(w):
                k = 4 if y < h * 0.35 else 3
                if y >= h - 1 or x >= w - 1:
                    k = 1
                elif y >= h * 0.8 or x >= w * 0.85:
                    k = 2
                p.put(x, y, palette[k])
        tx, ty = toward or (0.5, 0.5)
        for v in range(veins):
            # Fixed, even spacing round the edge (the seed turns them), each curving gently inward.
            t = ((v + 0.5) / max(1, veins) + seed * 0.17) % 1.0
            side = int(t * 4)
            along = t * 4 - side
            x, y = [(along * w, 0), (w - 1, along * h), ((1 - along) * w, h - 1), (0, (1 - along) * h)][side]
            angle = math.atan2(ty * h - y, tx * w - x)
            bend = 0.08 if v % 2 else -0.08
            for s in range(int(min(w, h) * 0.3)):
                p.put(x, y, VEIN[1])
                angle += bend
                x += math.cos(angle)
                y += math.sin(angle)
    return paint


def iris(palette=IRIS, corner=SCLERA[3], glint=ca.GLINT, glints=2):
    """The iris seen face on: a clean disc in three rings of `palette` (pale round the pupil to deep at its rim), a dark
    rim, a big round black pupil and `glints` glints of colour `glint`. `corner` fills the corners outside the disc (the
    white of the eye); None leaves them see-through, for an iris laid over a white drawn separately."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        r = p.w / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot(x - cx, y - cy) / r
                if d > 0.98:
                    if corner is not None:
                        p.put(x, y, corner)
                elif d > 0.84:
                    p.put(x, y, palette[1])
                elif d > 0.66:
                    p.put(x, y, palette[3] if y < cy else palette[2])
                elif d > 0.42:
                    p.put(x, y, palette[4] if y < cy else palette[3])
                else:
                    p.put(x, y, PUPIL[0])
        if glints:
            ca.ellipse(p, cx - r * 0.3 + 0.5, cy - r * 0.3 + 0.5, max(1.0, r * 0.2), max(1.0, r * 0.2), glint)
        if glints > 1:
            p.put(cx + r * 0.25, cy + r * 0.2, glint)
    return paint


def bat_wing(seed=1):
    """A bat's wing, its hinge on the right-hand edge: a smooth membrane in two tones (lit along its leading edge),
    three neat scallops along its trailing edge, and the arm and fingers drawn as clean darker lines."""
    def paint(p):
        w, h = p.w, p.h
        hinge_top, hinge_bottom = (w - 1, h * 0.38), (w - 1, h * 0.72)
        wrist = (w * 0.42, h * 0.08)
        tips = [(0, h * 0.22), (w * 0.02, h * 0.62), (w * 0.28, h * 0.94), (w * 0.62, h * 0.9)]
        polygon = [hinge_top, wrist] + tips + [hinge_bottom]

        def inside(x, y):
            n, c = len(polygon), False
            for i in range(n):
                (x1, y1), (x2, y2) = polygon[i], polygon[(i + 1) % n]
                if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / ((y2 - y1) or 1e-9) + x1:
                    c = not c
            return c

        for y in range(h):
            for x in range(w):
                if inside(x + 0.5, y + 0.5):
                    p.put(x, y, WING[4] if y < h * 0.3 else WING[3])
        # Scallops: bite the trailing edge between each pair of neighbouring tips.
        edge = tips + [hinge_bottom]
        for (x1, y1), (x2, y2) in zip(edge, edge[1:]):
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            span = math.hypot(x2 - x1, y2 - y1)
            ox, oy = mx - wrist[0], my - wrist[1]
            norm = math.hypot(ox, oy) or 1
            bx, by = mx + ox / norm * span * 0.42, my + oy / norm * span * 0.42
            radius = span * 0.62
            for y in range(h):
                for x in range(w):
                    if math.hypot(x + 0.5 - bx, y + 0.5 - by) < radius:
                        p.img.putpixel((x, y), (0, 0, 0, 0))
        # A darker rim a texel inside the trailing edge, so the scallops read cleanly.
        for y in range(h):
            for x in range(w):
                if p.get(x, y) and y > h * 0.3:
                    below = p.get(x, y + 1) if y + 1 < h else None
                    if not below:
                        p.put(x, y, WING[2])

        def line(a, b, c, width=1):
            steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
            for s in range(steps + 1):
                t = s / steps
                x, y = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
                if p.get(x, y):
                    p.put(x, y, c)
                    if width > 1 and p.get(x, y + 1):
                        p.put(x, y + 1, c)

        line(hinge_top, wrist, WING_BONE[2], 2)
        for tip in tips:
            line(wrist, tip, WING_BONE[2])
        p.put(wrist[0], wrist[1], WING_BONE[1])
        p.put(wrist[0] - 1, wrist[1] - 1, WING_BONE[1])
    return paint


def membrane_wing():
    """The Flying Eyeball's bat wing, its hinge on the right-hand edge, drawn clean (5 October 2026): the membrane in
    flat panels between the fingers, alternately lit and shaded so the wing reads as stretched skin, a lit band under
    the arm along its leading edge, three neat scallops along its trailing edge with a darker rim, the arm two texels
    thick and the fingers one, meeting at a knuckle with a little pale claw. No pixel is picked at random."""
    def paint(p):
        w, h = p.w, p.h
        hinge_top, hinge_bottom = (w - 1, h * 0.38), (w - 1, h * 0.72)
        wrist = (w * 0.42, h * 0.08)
        tips = [(0, h * 0.22), (w * 0.02, h * 0.62), (w * 0.28, h * 0.94), (w * 0.62, h * 0.9)]
        polygon = [hinge_top, wrist] + tips + [hinge_bottom]

        def inside(x, y):
            n, c = len(polygon), False
            for i in range(n):
                (x1, y1), (x2, y2) = polygon[i], polygon[(i + 1) % n]
                if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / ((y2 - y1) or 1e-9) + x1:
                    c = not c
            return c

        # Each panel lies between two neighbouring fingers, seen from the wrist.
        rays = [math.atan2(ty - wrist[1], tx - wrist[0]) for tx, ty in tips + [hinge_bottom]]

        def panel(x, y):
            a = math.atan2(y - wrist[1], x - wrist[0])
            for k in range(len(rays) - 1):
                lo, hi = sorted((rays[k], rays[k + 1]))
                if lo <= a <= hi:
                    return k
            return len(rays)

        def near_arm(x, y):
            ax, ay = hinge_top
            bx, by = wrist
            t = max(0.0, min(1.0, ((x - ax) * (bx - ax) + (y - ay) * (by - ay)) / ((bx - ax) ** 2 + (by - ay) ** 2)))
            return math.hypot(x - (ax + (bx - ax) * t), y - (ay + (by - ay) * t))

        for y in range(h):
            for x in range(w):
                if inside(x + 0.5, y + 0.5):
                    k = 3 if panel(x + 0.5, y + 0.5) % 2 == 0 else 2
                    if near_arm(x + 0.5, y + 0.5) < 3.0:
                        k = 4
                    p.put(x, y, WING[k])
        # Scallops: bite the trailing edge between each pair of neighbouring tips.
        edge = tips + [hinge_bottom]
        for (x1, y1), (x2, y2) in zip(edge, edge[1:]):
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            span = math.hypot(x2 - x1, y2 - y1)
            ox, oy = mx - wrist[0], my - wrist[1]
            norm = math.hypot(ox, oy) or 1
            bx, by = mx + ox / norm * span * 0.42, my + oy / norm * span * 0.42
            radius = span * 0.62
            for y in range(h):
                for x in range(w):
                    if math.hypot(x + 0.5 - bx, y + 0.5 - by) < radius:
                        p.img.putpixel((x, y), (0, 0, 0, 0))
        # A darker rim along the trailing edge, a texel wide, so the scallops read cleanly.
        for x, y in p.filled():
            if y > h * 0.3 and any(p.get(x + dx, y + dy) is None for dx, dy in ((0, 1), (1, 0), (-1, 0))):
                p.put(x, y, WING[1])

        def line(a, b, c, lit=None):
            steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
            for s in range(steps + 1):
                t = s / steps
                x, y = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
                if p.get(x, y):
                    p.put(x, y, c)
                    if lit is not None and p.get(x, y + 1):
                        p.put(x, y + 1, c)
                        p.put(x, y, lit)

        for tip in tips:
            line(wrist, tip, WING_BONE[1])
        line(hinge_top, wrist, WING_BONE[1], lit=WING_BONE[2])
        p.put(wrist[0], wrist[1], WING_BONE[0])
        p.put(wrist[0] + 1, wrist[1], WING_BONE[0])
        p.put(wrist[0] - 1, wrist[1] - 1, IVORY[3])
    return paint


def wax(palette, seed=1):
    """A church candle's side: shaded wax, its top rim lighter, drips of fresh wax running down from it unevenly."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                across = x / max(1, p.w - 1)
                jitter = rng.choice((0, 0, 0, -1, 1))  # drawn all the same, so the drips fall where they did
                k = n - 3 - int(abs(across - 0.35) * 2.2) + (0 if fa.QUIET else jitter)
                if y > p.h * 0.85:
                    k -= 1
                p.put(x, y, shade(palette, k))
        x = 0
        while x < p.w:
            width = rng.choice((1, 2, 2, 3))
            length = int(p.h * rng.uniform(0.08, 0.55))
            for dx in range(width):
                for y in range(length):
                    k = n - 1 if dx == 0 else n - 2
                    p.put(x + dx, y, shade(palette, k))
                p.put(x + dx, length, shade(palette, n - 2))
                p.put(x + dx, length + 1, shade(palette, n - 4))
            x += width + rng.choice((1, 2, 3))
        for x in range(p.w):
            p.put(x, 0, palette[n - 1])
    return paint


def wax_top(palette):
    """A candle's top from above: a raised rim of wax round the hollow its flame has melted, the pool darker."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                d = max(abs(x - cx), abs(y - cy)) / (p.w / 2)
                k = n - 1 if d > 0.72 else (n - 2 if d > 0.55 else n - 4)
                p.put(x, y, shade(palette, k))
    return paint


def web():
    """A spider's web over a whole face: radial threads from a hub a little above the middle out to the edges, the
    spiral of sticky thread sagging between them, a few broken strands, and a small spider waiting by the hub."""
    img = Image.new("RGBA", (fa.SIZE, fa.SIZE), (0, 0, 0, 0))
    p = Px(img)
    rng = random.Random(16)
    hub = (31.5, 27.0)
    spokes = 13
    angles = [i * math.tau / spokes + rng.uniform(-0.12, 0.12) for i in range(spokes)]

    def reach(a):
        """How far a spoke at angle `a` runs from the hub before it leaves the face."""
        dx, dy = math.cos(a), math.sin(a)
        ts = []
        for edge, d, origin in ((0, dx, hub[0]), (63, dx, hub[0]), (0, dy, hub[1]), (63, dy, hub[1])):
            if abs(d) > 1e-6:
                t = (edge - origin) / d
                if t > 0:
                    ts.append(t)
        return min(ts)

    def put(x, y, a=210):
        if 0 <= x < 64 and 0 <= y < 64:
            img.putpixel((int(x), int(y)), WEB + (a,))

    def line(a, b, alpha=210, skip=None):
        steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
        for s in range(steps + 1):
            if skip and skip(s / steps):
                continue
            t = s / steps
            put(round(a[0] + (b[0] - a[0]) * t), round(a[1] + (b[1] - a[1]) * t), alpha)

    for i, a in enumerate(angles):
        r = reach(a)
        end = (hub[0] + math.cos(a) * r, hub[1] + math.sin(a) * r)
        broken = (lambda t: 0.62 < t < 0.7) if i in (4, 9) else None
        line(hub, end, 225, broken)
    # The spiral: a ring of chords between neighbouring spokes every few texels out, each sagging toward the hub.
    radius = 3.0
    ring = 0
    while radius < 46:
        for i in range(spokes):
            a1, a2 = angles[i], angles[(i + 1) % spokes]
            if a2 < a1:
                a2 += math.tau
            r1 = min(radius + ring * 0.0, reach(a1))
            r2 = min(radius + 0.35, reach(angles[(i + 1) % spokes]))
            if radius > reach(a1) + 1 or radius > reach(angles[(i + 1) % spokes]) + 1:
                continue
            steps = 12
            prev = None
            for s in range(steps + 1):
                t = s / steps
                a = a1 + (a2 - a1) * t
                r = r1 + (r2 - r1) * t - math.sin(t * math.pi) * radius * 0.08
                pt = (hub[0] + math.cos(a) * r, hub[1] + math.sin(a) * r)
                if prev and not (ring in (5, 6) and i == 2 and 0.3 < t < 0.8):
                    line(prev, pt, 190)
                prev = pt
        ring += 1
        radius += 3.0 + ring * 0.35
    # The spider by the hub: a dark body, a smaller head and eight legs.
    sx, sy = hub[0] + 5, hub[1] + 8
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (0, -1), (1, -1), (0, 2), (1, 2)):
        img.putpixel((int(sx + dx), int(sy + dy)), (22, 16, 20, 255))
    img.putpixel((int(sx), int(sy - 2)), (40, 30, 34, 255))
    img.putpixel((int(sx + 1), int(sy - 2)), (40, 30, 34, 255))
    img.putpixel((int(sx), int(sy + 1)), (120, 20, 26, 255))
    for side in (-1, 1):
        for j, (lx, ly) in enumerate(((2, -2), (3, -1), (3, 1), (2, 3))):
            x0 = sx + (1 if side > 0 else 0)
            for t in (1, 2):
                img.putpixel((int(x0 + side * lx * t / 2), int(sy + ly * t / 2)), (30, 22, 26, 255))
    return img


def bolt_head():
    """A neck bolt's head: pale steel, bevelled, with a round boss and a glint."""
    def paint(p):
        ca.bevel(METAL, 3)(p)
        ca.ellipse(p, p.w / 2, p.h / 2, max(1.0, p.w * 0.28), max(1.0, p.h * 0.28), METAL[2])
        p.put(p.w // 2 - 1, p.h // 2 - 1, METAL[4])
    return paint


def monster_skin(seed=1, stitches=0, horizontal=False):
    """The monster's bright green skin: smooth, lit along its top and shaded along its bottom, with `stitches` neat rows
    of evenly spaced black stitches across it."""
    def paint(p):
        ca.soft(SKIN, 3, edge=1, top=0.22, bottom=0.2)(p)
        for s in range(stitches):
            if horizontal:
                y = int(p.h * (s + 1) / (stitches + 1))
                x0, x1 = int(p.w * 0.2), int(p.w * 0.8)
                for x in range(x0, x1):
                    p.put(x, y, SKIN[1])
                for x in range(x0 + 1, x1 - 1, 3):
                    p.put(x, y - 1, STITCH[0])
                    p.put(x, y, STITCH[0])
                    p.put(x, y + 1, STITCH[0])
            else:
                x = int(p.w * (s + 1) / (stitches + 1))
                y0, y1 = int(p.h * 0.15), int(p.h * 0.85)
                for y in range(y0, y1):
                    p.put(x, y, SKIN[1])
                for y in range(y0 + 1, y1 - 1, 3):
                    p.put(x - 1, y, STITCH[0])
                    p.put(x, y, STITCH[0])
                    p.put(x + 1, y, STITCH[0])
    return paint


# The monster's eyes on the front of its head (x from, to; y from, to, in pixels), and its forehead's scar's height.
MONSTER_EYES = ((4.0, 7.2), (8.8, 12.0))
MONSTER_EYE_Y = (9.9, 11.9)


def monster_face(awake):
    """The front of the monster's head above the jaw, after the owner's reference: smooth bright green, shaded under the
    fringe, a neat stitched scar across the forehead, big heavy-lidded eyes in dark sockets (sleepy, or wide and glowing
    when it wakes), cheeks shaded down the sides; asleep, a closed mouth's neat line runs along the bottom."""
    def paint(p):
        monster_skin(11)(p)
        w, h = p.w, p.h
        # The fringe's shadow along the top, and the cheeks' shade down each side.
        for y in range(h):
            for x in range(w):
                if y < h * 0.12:
                    p.put(x, y, SKIN[2])
                elif x < 2 or x >= w - 2:
                    p.put(x, y, SKIN[2] if y < h - 2 else SKIN[1])
        # A neat stitched scar down the left cheek.
        x = int(w * 0.1)
        for y in range(int(h * 0.62), int(h * 0.92)):
            p.put(x, y, SKIN[1])
        for y in range(int(h * 0.64), int(h * 0.92), 3):
            for dx in (-1, 0, 1):
                p.put(x + dx, y, STITCH[0])
        # Big eyes under the brow, in the boxes of MONSTER_EYES (the awake glow is drawn over them, monster_head).
        for ex0, ex1 in MONSTER_EYES:
            x0, x1 = w * (ex0 - 2.0) / 12.0, w * (ex1 - 2.0) / 12.0
            y0, y1 = h * (15.0 - MONSTER_EYE_Y[1]) / 8.0, h * (15.0 - MONSTER_EYE_Y[0]) / 8.0
            ca.rounded_rect(p, x0, y0, x1, y1, SKIN_DARK[0] if awake else SKIN[2], corner=1)
            inner = (x0 + 1, y0 + 1, x1 - 1, y1 - 1)
            if awake:
                ca.rounded_rect(p, *inner, EYE_GLOW[2], corner=1)
                ca.rounded_rect(p, inner[0] + 1, inner[1] + 1, inner[2] - 1, inner[3] - 1, EYE_GLOW[4], corner=0)
            else:
                # Asleep: the socket a soft shadow, the eye shut in a thick dark curve, lashes down.
                ca.rounded_rect(p, *inner, SKIN[2], corner=1)
                cx = (x0 + x1) / 2
                half = (x1 - x0) / 2 - 1
                for x in range(int(x0) + 1, int(x1) - 1):
                    t = (x + 0.5 - cx) / half
                    p.put(x, int(y1) - (3 if abs(t) > 0.6 else 2), SKIN_DARK[0])
        # Asleep, the closed mouth: a neat dark line with a stitch at each corner.
        if not awake:
            for x in range(int(w * 0.24), int(w * 0.76)):
                p.put(x, h - 2, STITCH[0])
            for x in (int(w * 0.24), int(w * 0.76) - 1):
                p.put(x, h - 3, STITCH[0])
                p.put(x, h - 1, STITCH[0])
    return paint


def jaw_front(awake):
    """The chin and lower lip: smooth green, the lip a shade darker along its top while the mouth is shut."""
    def paint(p):
        monster_skin(12)(p)
        if awake:
            return
        for x in range(int(p.w * 0.24), int(p.w * 0.76)):
            p.put(x, 0, SKIN[2])
    return paint


def teeth(seed=1):
    """A neat row of square cream teeth, two texels apart from a dark gap."""
    def paint(p):
        for x in range(p.w):
            gap = x % 3 == 2
            for y in range(p.h):
                p.put(x, y, MOUTH[0] if gap else (TOOTH[3] if y == 0 else TOOTH[2]))
    return paint


def hair(seed=1, fringe=False):
    """Black hair, combed flat: a dark flat colour with a cool sheen in even combed lines; a fringe is cut straight
    along its lower edge with regular square notches (after the reference's blunt bangs)."""
    def paint(p):
        for x in range(p.w):
            length = p.h
            if fringe:
                length = p.h if (x // 3) % 3 else int(p.h * 0.6)
            for y in range(length):
                k = 2 if x % 5 == 0 else 1
                if fringe and y == length - 1:
                    k = 0
                p.put(x, y, HAIR[k])
    return paint


# ---------------------------------------------------------------- the flying eyeball

EYE_CENTRE = (8.0, 8.0, 8.0)
WING_HINGES = {"left": (4.8, 9.0, 8.6), "right": (11.2, 9.0, 8.6)}


def flying_eyeball():
    """The eyeball (centred on EYE_CENTRE, its iris to the north) and its two wings (each hinged at the origin, for the
    renderer to sweep back and beat), as models for the client; and the whole, wings raised, as the item's model.

    The iris is a disc with see-through corners (the client draws it glowing, so nothing round it may glow), with a white
    glint inside the disc, laid 0.1 pixel proud of the cap's white front; the cap's thin sides sample a thin strip,
    about four texels to a pixel like the rest. The client's wings are one face each: 26.3 draws a cut-out quad from
    both sides, so a second, back-to-back face would only fight it. The item's wings are drawn from both sides as two
    faces 0.1 pixel apart (each lifted 0.05 off the wing's plane), so they cannot fight whether or not items cull."""
    sc = Sculpt(d16.FLYING_EYEBALL["block"], 71)
    wing = sc.piece("wing", 28, 24, membrane_wing())
    side = sc.piece("sclera", 24, 24, sclera(3, 6, palette=WARM_SCLERA))
    front = sc.piece("sclera_front", 24, 24, sclera(4, 9, (0.5, 0.5), palette=WARM_SCLERA))
    small = sc.piece("sclera_small", 16, 16, sclera(5, 3, palette=WARM_SCLERA))
    eye = sc.piece("iris", 16, 16, iris(corner=None, glint=ca.GLINT, glints=1))
    nerve = sc.piece("nerve", 4, 12, ca.bevel(NERVE, 2, sides="lr"))
    nu0, nv0, nu1, nv1 = nerve
    nerve_tip = (nu0, nv0, nu1, nv0 + (nu1 - nu0))          # a 4 x 4 texel end of the strip, for the nerve's tip
    rim = sc.piece("cap_side", 16, 2, ca.bevel(WARM_SCLERA, 3, sides="b"))
    u0, v0, u1, v1 = rim
    rim_end = (u0, v0, u0 + (v1 - v0), v1)                 # a 2 x 2 texel corner of the strip, for the 0.5-px-wide ends
    sides = {s: side for s in ("south", "east", "west", "up", "down")}
    body = [cube((5, 5, 5), (11, 11, 11), {"north": front, **sides}),
            cube((4.5, 6, 6), (11.5, 10, 10), {s: small for s in ("east", "west", "up", "down", "north", "south")}),
            cube((6, 4.5, 6), (10, 11.5, 10), {s: small for s in ("up", "down", "north", "south", "east", "west")}),
            cube((6, 6, 10), (10, 10, 11.5), {s: small for s in ("south", "east", "west", "up", "down")}),
            # The cornea's cap, its front white behind the iris's see-through corners.
            cube((6, 6, 4.5), (10, 10, 5), {"north": small, "up": rim, "down": rim, "east": rim_end, "west": rim_end}),
            # The optic nerve trailing behind, drooping.
            cube((7.3, 7.3, 11.5), (8.7, 8.7, 13.5), {s: nerve for s in ("east", "west", "up", "down")}),
            cube((7.45, 7.45, 13.3), (8.55, 8.55, 16.0), {**{s: nerve for s in ("east", "west", "up", "down")}, "south": nerve_tip},
                 rotation((8, 8, 13.5), "x", 22.5))]
    iris_quad = [cube((6, 6, 4.4), (10, 10, 4.4), {"north": eye})]
    u0, v0, u1, v1 = wing
    left = plane_xy(-7.0, 0.0, -2.5, 3.5, 0.0, wing)
    right = plane_xy(0.0, 7.0, -2.5, 3.5, 0.0, (u1, v0, u0, v1))
    sc.models["flying_eyeball_body"] = body
    sc.models["flying_eyeball_iris"] = iris_quad
    # The client's wings: the south face only (see above).
    sc.models["flying_eyeball_wing_left"] = [dict(left, faces={"south": left["faces"]["south"]})]
    sc.models["flying_eyeball_wing_right"] = [dict(right, faces={"south": right["faces"]["south"]})]
    # The item: the whole at rest, wings raised in a V, each wing's two faces lifted apart (see above).
    lx, ly, lz = WING_HINGES["left"]
    rx, ry, rz = WING_HINGES["right"]

    def lifted(sheet):
        sheet["from"][2] = round(sheet["from"][2] - 0.05, 4)
        sheet["to"][2] = round(sheet["to"][2] + 0.05, 4)
        return sheet
    item = body + iris_quad + [
        lifted(plane_xy(lx - 7.0, lx, ly - 2.5, ly + 3.5, lz, wing, rotation((lx, ly, lz), "z", -22.5))),
        lifted(plane_xy(rx, rx + 7.0, ry - 2.5, ry + 3.5, rz, (u1, v0, u0, v1), rotation((rx, ry, rz), "z", 22.5)))]
    sc.models["flying_eyeball_item"] = item
    return sc


def eyeball_quads():
    """The renderer's models: the eyeball's body, its iris (drawn glowing) and its wings, all cut out."""
    sc = build("flying_eyeball")
    out = {}
    for name in ("flying_eyeball_body", "flying_eyeball_iris", "flying_eyeball_wing_left", "flying_eyeball_wing_right"):
        out[name] = [dict(q, cutout=True) for q in quads(sc.models[name], {"p": d16.FLYING_EYEBALL["block"]})]
    return out


def specimen_eye():
    """The Specimen Jar's eye (tools/decor8_data.py draws it in the jar; client/SpecimenJarRenderer turns it about the
    jar's middle, (8, 6, 8)): the Flying Eyeball's clean white and veins round a blue iris (it stands out against the
    green fluid), built rounder than a cube: a 4-pixel body with a bulge through each side, its optic nerve hanging
    straight down on the axis it turns about. Every face shows its own piece at about four texels to a pixel. The whole
    specimen is lit by the fluid's glow, so the iris's corners are white of the eye, drawn solid."""
    sc = Sculpt("specimen_eye", 74)
    body = sc.piece("sclera", 16, 16, sclera(6, 3, palette=WARM_SCLERA))
    front = sc.piece("sclera_front", 16, 16, sclera(7, 4, (0.5, 0.5), palette=WARM_SCLERA))
    wide = sc.piece("bulge_wide", 18, 12, sclera(8, 2, palette=WARM_SCLERA))
    tall = sc.piece("bulge_tall", 12, 18, sclera(9, 2, palette=WARM_SCLERA))
    end = sc.piece("bulge_end", 12, 12, sclera(10, 1, palette=WARM_SCLERA))
    eye = sc.piece("iris", 12, 12, iris(BLUE_IRIS, corner=WARM_SCLERA[3], glint=BLUE_IRIS[5], glints=1))
    nerve = sc.piece("nerve", 4, 5, ca.bevel(NERVE, 2, sides="lr"))
    u0, v0, u1, v1 = nerve
    nerve_end = (u0, v0, u1, v0 + (u1 - u0))
    sc.models["specimen_eye"] = [
        cube((6, 4, 6), (10, 8, 10), {"north": front, **{s: body for s in ("south", "east", "west", "up", "down")}}),
        cube((5.75, 4.5, 6.5), (10.25, 7.5, 9.5), {"east": end, "west": end, **{s: wide for s in ("north", "south", "up", "down")}}),
        cube((6.5, 3.75, 6.5), (9.5, 8.25, 9.5), {"up": end, "down": end, **{s: tall for s in ("north", "south", "east", "west")}}),
        cube((6.5, 4.5, 5.75), (9.5, 7.5, 10.25), {"north": eye, "south": end, "east": wide, "west": wide, "up": tall, "down": tall}),
        # The nerve, its top hidden 0.1 pixel up inside the lower bulge.
        cube((7.5, 2.5, 7.5), (8.5, 3.85, 8.5), {"north": nerve, "south": nerve, "east": nerve, "west": nerve, "down": nerve_end})]
    return sc


def specimen_eye_elements(key="#p"):
    """The Specimen Jar eye's elements, drawing on texture `key` (the jar's models call it #eye)."""
    sc = build("specimen_eye")
    return [dict(e, faces={side: dict(face, texture=key) for side, face in e["faces"].items()}) for e in sc.models["specimen_eye"]]


# ---------------------------------------------------------------- the pillar candles

@fa.quietly
def pillar_candles(block):
    """One to four church candles in a cluster (tools/decor16.py CANDLES), each dripping wax down its sides with a wick
    on its melted top, unlit or lit (the wick's tip glowing; the flame is vanilla's particle)."""
    palette = IVORY if block.startswith("ivory") else BLACK_WAX
    sc = Sculpt(block, 72 if block.startswith("ivory") else 73)
    sides = {5.0: sc.piece("side5", 20, 28, wax(palette, 2)), 4.0: sc.piece("side4", 16, 28, wax(palette, 3))}
    tops = {5.0: sc.piece("top5", 10, 10, wax_top(palette)), 4.0: sc.piece("top4", 8, 8, wax_top(palette))}
    wick = sc.piece("wick", 2, 4, solid(WICK, 1))
    lit_wick = sc.piece("wick_lit", 2, 4, strip(EMBER, 2, light=True))
    drip = sc.piece("drip", 2, 6, strip(palette[-3:], 4, light=True))
    puddle = sc.piece("puddle", 6, 6, solid(palette[-4:-1], 5))
    for count, candles in d16.CANDLES.items():
        for lit in (False, True):
            els = []
            for i, (x, z, w, h) in enumerate(candles):
                u0, v0, u1, v1 = sides[w]
                half = (u1 - u0) / 2
                cut = v0 + (v1 - v0) * h / 14.0
                ns = (u0, v0, u0 + half, cut)
                ew = (u0 + half, v0, u1, cut)
                r = w / 2
                els.append(cube((x - r, 0, z - r), (x + r, h, z + r),
                                {"north": ns, "south": ns, "east": ew, "west": ew, "up": tops[w]}))
                # A puddle of spilt wax at its foot and a long drip down its front.
                els.append(cube((x - r - 0.4, 0, z - r - 0.4), (x + r + 0.4, 0.3, z + r + 0.4),
                                {**{s: puddle for s in SIDES4}, "up": puddle}))
                dz = z - r - 0.3 if i % 2 == 0 else z + r
                dx = x + (0.6 if i % 2 == 0 else -1.0)
                els.append(cube((dx, h * 0.45, dz), (dx + 0.5, h, dz + 0.3), {s: drip for s in SIDES4}))
                els.append(column(x, z, h, h + 1.0, 0.5, lit_wick if lit else wick, light=10 if lit else None))
            sc.models[f"{block}_{count}" + ("_lit" if lit else "")] = els
    return sc


# ---------------------------------------------------------------- the monster's head

def monster_head():
    """The monster's great green head on its stub of neck and collar: flat-topped, its black hair combed down in a
    jagged fringe, a heavy brow over deep-set eyes, a broad nose, ears, stitches and a bolt each side of the neck.
    Awake, its jaw drops open on its teeth and its eyes glow."""
    sc = Sculpt(d16.MONSTER_HEAD["block"], 74)
    # Painted in this order, so the pieces pack into the 64 x 64 texture row by row.
    face = sc.piece("face", 30, 20, monster_face(False))
    face_awake = sc.piece("face_awake", 30, 20, monster_face(True))
    hair_top = sc.piece("hair_top", 20, 20, hair(3))
    skin_side = sc.piece("skin_side", 23, 16, monster_skin(13, 1, horizontal=True))
    skin_back = sc.piece("skin_back", 18, 12, monster_skin(14, 2))
    jaw = sc.piece("jaw", 22, 6, jaw_front(False))
    jaw_side = sc.piece("jaw_side", 16, 6, monster_skin(15))
    mouth = sc.piece("mouth", 8, 4, ca.bevel(MOUTH, 1, light=0))
    teeth_uv = sc.piece("teeth", 12, 2, teeth(2))
    hair_side = sc.piece("hair_side", 26, 4, hair(4))
    fringe = sc.piece("fringe", 26, 7, hair(5, fringe=True))
    neck = sc.piece("neck", 10, 6, monster_skin(16, 1, horizontal=True))
    collar = sc.piece("collar", 10, 2, ca.bevel(SHIRT, 1))
    brow = sc.piece("brow", 16, 3, ca.bevel(SKIN, 2, light=1, dark=1, sides="tb"))
    nose = sc.piece("nose", 4, 5, ca.bevel(SKIN, 3))
    ear = sc.piece("ear", 4, 5, ca.bevel(SKIN, 3))
    bolt = sc.piece("bolt", 4, 4, bolt_head())
    shaft = sc.piece("shaft", 3, 3, ca.bevel(METAL, 2))
    glow = sc.piece("glow", 4, 2, ca.bevel(EYE_GLOW, 3, light=1, dark=0))

    def head(awake):
        els = [cube((4.5, 0, 4.5), (11.5, 1.2, 11.2), {**{s: collar for s in SIDES4}, "up": collar}),
               cube((5.0, 1.2, 5.0), (11.0, 4.0, 10.8), {s: neck for s in SIDES4})]
        for x0, x1, hx0, hx1 in ((3.8, 5.0, 3.0, 3.8), (11.0, 12.2, 12.2, 13.0)):
            els.append(cube((x0, 2.0, 7.2), (x1, 3.2, 8.4), {s: shaft for s in ("north", "south", "up", "down", "east", "west")}))
            els.append(cube((hx0, 1.6, 6.8), (hx1, 3.6, 8.8), {s: bolt for s in ("north", "south", "up", "down", "east", "west")}))
        # The cranium above the jaw.
        els.append(cube((2.0, 7.0, 2.0), (14.0, 15.0, 13.5),
                        {"north": face_awake if awake else face, "south": skin_back, "east": skin_side, "west": skin_side,
                         "down": skin_back}))
        els += [cube((1.6, 14.4, 1.6), (14.4, 16.2, 13.9), {"up": hair_top, "east": hair_side, "west": hair_side,
                                                            "south": hair_side, "north": hair_side}),
                plane_xy(1.6, 14.4, 13.0, 14.6, 1.5, fringe),
                cube((3.0, 12.0, 1.4), (13.0, 12.9, 2.0), {"north": brow, "up": brow, "down": brow, "east": brow, "west": brow}),
                cube((7.3, 8.6, 1.4), (8.7, 10.4, 2.0), {"north": nose, "east": nose, "west": nose, "down": nose}),
                cube((1.2, 8.5, 7.0), (2.0, 11.5, 9.5), {s: ear for s in ("west", "north", "south", "up")}),
                cube((14.0, 8.5, 7.0), (14.8, 11.5, 9.5), {s: ear for s in ("east", "north", "south", "up")})]
        jaw_els = [cube((2.5, 4.0, 2.5), (13.5, 7.0, 13.0), {"north": jaw, "east": jaw_side, "west": jaw_side,
                                                             "down": jaw_side, "south": jaw_side})]
        if awake:
            # The jaw drops about its hinge at the back; the mouth's dark inside and the teeth show.
            hinge = rotation((8.0, 7.0, 12.5), "x", -22.5)
            jaw_els = [dict(e, rotation=hinge) for e in jaw_els]
            jaw_els.append(cube((3.5, 6.6, 2.9), (12.5, 7.2, 3.3), {s: teeth_uv for s in ("north", "up")}, hinge))
            els += [cube((3.5, 4.2, 3.2), (12.5, 7.0, 12.0), {"north": mouth, "east": mouth, "west": mouth, "down": mouth}),
                    cube((3.5, 6.2, 2.3), (12.5, 7.0, 2.7), {"north": teeth_uv, "down": teeth_uv})]
            # Glowing eyes over the painted ones (the face is drawn mirrored on a north face, so they sit mirrored too).
            for x0, x1 in MONSTER_EYES:
                els.append(cube((x0 + 0.6, MONSTER_EYE_Y[0] + 0.5, 1.95), (x1 - 0.6, MONSTER_EYE_Y[1] - 0.5, 1.95), {"north": glow}, light=15))
        return els + jaw_els

    sc.models["monster_head"] = head(False)
    sc.models["monster_head_awake"] = head(True)
    return sc


# ---------------------------------------------------------------- the harvest plushes

OWL = pal("2e1a10", "4a2c1a", "6a4024", "8a5630", "a8703e")
OWL_BELLY = pal("8a6a1c", "b8902a", "e0b83a", "f4d662")
OWL_TUFT = pal("7a1e0c", "b0381a", "e0602a", "f4904a")
BEAK = pal("8a5208", "d08a14", "f4b830")
EYE_YELLOW = pal("8a6a08", "d4a814", "f4d43a", "fff08a")
HEDGEHOG = pal("2a1c12", "48321f", "6c4e32", "93704c", "bc9c74", "e8dcc2")
HEDGEHOG_FACE = pal("8a7660", "b4a088", "d4c4ac", "ece0cc")
ACORN = pal("5a2a10", "844018", "aa5a22", "c8763a", "e09a5a")
ACORN_CAP = pal("2a1a10", "40281a", "5a3a24", "74502e", "8e6a40")
KERNEL = pal("8a5a08", "c48e10", "e8b41e", "f8d44a", "fff08e")
HUSK = pal("1e3a14", "2e5a1e", "44782a", "62983c", "86b856")
SILK = pal("6a4a1a", "a0782c", "d0a848", "ecd078")
MAPLE = pal("6a1008", "9a1e0e", "c8361a", "e45a24", "f48a3a", "fcb45a")
STEM_BROWN = pal("3a2214", "5a3820", "7a5030")
CHEEK = pal("c0405a", "e06a7e")


def plush_face(palette, eyes, mouth="smile", eye_colour=None, cheeks=True, seed=1, eye_size=0.16):
    """A plush's embroidered face on its felt: two eyes (at `eyes`, (x, y) as fractions) with a glint, a mouth (a smile,
    a grin with teeth, a little o, or a worried wobble) and rosy cheeks."""
    def paint(p):
        felt(palette, seed, seam=False)(p)
        w, h = p.w, p.h
        r = max(1.2, w * eye_size)
        for ex, ey in eyes:
            cx, cy = ex * w, ey * h
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                for x in range(int(cx - r) - 1, int(cx + r) + 2):
                    d = math.hypot(x - cx, y - cy)
                    if d <= r:
                        ring = eye_colour and d > r * 0.55
                        p.put(x, y, shade(eye_colour, 2) if ring else PUPIL[0])
            p.put(cx - r * 0.35, cy - r * 0.35, (255, 255, 255))
        mx, my = w / 2, h * 0.74
        span = w * 0.18
        if mouth == "smile":
            for i in range(-int(span), int(span) + 1):
                p.put(mx + i, my + (span - abs(i)) * 0.35, PUPIL[1])
        elif mouth == "grin":
            for i in range(-int(span * 1.3), int(span * 1.3) + 1):
                depth = int((span * 1.3 - abs(i)) * 0.5)
                for d in range(depth + 1):
                    p.put(mx + i, my - 1 + d, (250, 248, 240) if d == 0 and abs(i) < span else MOUTH[1])
        elif mouth == "o":
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                p.put(mx - 0.5 + dx, my + dy, MOUTH[1])
        elif mouth == "worried":
            for i in range(-int(span), int(span) + 1):
                p.put(mx + i, my + (1 if i % 4 in (0, 1) else 0), PUPIL[1])
        if cheeks:
            for ex, ey in eyes:
                cx = ex * w + (-r if ex < 0.5 else r) * 0.6
                p.put(cx, ey * h + r * 1.7, CHEEK[1])
                p.put(cx + 1, ey * h + r * 1.7, CHEEK[0])
    return paint


def feathers(palette, seed=1, rows=4):
    """Owl's felt feathers: rows of little scallops, each lit at its top."""
    def paint(p):
        felt(palette, seed, seam=False)(p)
        step = max(3, p.w // 5)
        for row, y0 in enumerate(range(1, p.h, max(3, p.h // rows))):
            for x0 in range(-(row % 2) * step // 2, p.w, step):
                for i in range(step):
                    y = y0 + int(abs(i - step / 2) * 0.6)
                    p.put(x0 + i, y, shade(palette, 0))
                    p.put(x0 + i, y + 1, shade(palette, len(palette) - 1))
    return paint


def spines(seed=1, jagged=False):
    """A hedgehog's spines, neat and soft: a warm brown back with even rows of short pale-tipped quills all lying back
    (down the piece); along the top edge, if `jagged`, their rounded points stand out (cut out between them)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, HEDGEHOG[2])
        for row in range(-2, p.h, 4):
            for x0 in range((row // 4) % 2 * 2, p.w + 2, 4):
                for i in range(4):
                    k = (3, 3, 4, 5)[i]
                    p.put(x0, row + i, HEDGEHOG[k])
                    p.put(x0 + 1, row + i, HEDGEHOG[k - 1])
        if jagged:
            for x in range(p.w):
                cut = (0, 1, 2, 1)[x % 4]
                for y in range(min(cut, p.h)):
                    p.img.putpixel((x, y), (0, 0, 0, 0))
                if cut < p.h:
                    p.put(x, cut, HEDGEHOG[4])
    return paint


def kernels(seed=1, face=False):
    """An ear of corn: neat rows of plump kernels, each a flat yellow square lit at its top-left with a darker seam
    between; with an embroidered grinning face if `face`."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                kx, ky = x % 3, (y + (x // 3) % 2) % 3
                k = 4 if (kx, ky) == (0, 0) else (3 if kx < 2 and ky < 2 else 1)
                p.put(x, y, KERNEL[k])
        if face:
            plush_face(KERNEL, [(0.3, 0.38), (0.7, 0.38)], "grin", cheeks=False, eye_size=0.13)(Overlay(p, KERNEL))
    return paint


class Overlay:
    """Draws only a face's dark marks (eyes, mouth, glints) over what is painted already, not its felt."""

    def __init__(self, p, palette):
        self.p, self.palette = p, set(palette)
        self.w, self.h, self.img = p.w, p.h, p.img

    def put(self, x, y, c, a=255):
        if tuple(c) not in self.palette:
            self.p.put(x, y, c, a)


def acorn_cap(seed=1):
    """An acorn's cap: even rows of little overlapping scales, each a flat brown with a darker lower edge."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                scale = (x + (y // 2) % 2 * 2) % 4 == 0 or y % 2 == 0 and x % 4 == 2
                p.put(x, y, ACORN_CAP[2 if scale else 3])
    return paint


def maple_leaf(face):
    """A maple leaf's five lobes, deeply cut, veined from the stem; with an embroidered face on its middle if `face`."""
    def paint(p):
        w, h = p.w, p.h
        cx, cy = w / 2, h * 0.62
        lobes = [(-90, 1.0), (-35, 0.9), (-145, 0.9), (20, 0.62), (-200, 0.62)]
        for y in range(h):
            for x in range(w):
                dx, dy = (x + 0.5 - cx) / (w / 2), (y + 0.5 - cy) / (h * 0.62)
                d = math.hypot(dx, dy)
                a = math.degrees(math.atan2(dy, dx))
                reach = 0.28
                for la, length in lobes:
                    off = abs((a - la + 180) % 360 - 180)
                    if off < 40:
                        # Each lobe: a point, with smaller teeth each side.
                        lobe = length * (1 - off / 40) ** 0.55
                        tooth = 0.1 * max(0.0, math.sin(off / 40 * math.pi * 3.5))
                        reach = max(reach, lobe * 0.95 + tooth)
                if d <= reach:
                    p.put(x, y, MAPLE[3] if dy < 0.0 else MAPLE[2])
        p.outline(MAPLE[1])
        for la, length in lobes:
            for s in range(int(length * h * 0.55)):
                t = s / (h * 0.62)
                x = cx + math.cos(math.radians(la)) * t * w / 2
                y = cy + math.sin(math.radians(la)) * t * h * 0.62
                if p.get(x, y):
                    p.put(x, y, MAPLE[3] if s % 2 else MAPLE[4])
        if face:
            plush_face(MAPLE, [(0.38, 0.5), (0.62, 0.5)], "grin", cheeks=False, eye_size=0.07)(Overlay(p, MAPLE))
    return paint


def owl_plush():
    sc = Sculpt("owl_plush", 81)
    body = sc.piece("body", 12, 18, feathers(OWL, 2, 5))
    belly = sc.piece("belly", 12, 18, feathers(OWL_BELLY, 3, 4))
    head = sc.piece("head", 12, 8, felt(OWL, 4))
    face = sc.piece("face", 12, 8, plush_face(OWL, [(0.27, 0.48), (0.73, 0.48)], "o", EYE_YELLOW, eye_size=0.17))
    wing = sc.piece("wing", 8, 12, feathers(OWL[:4], 5, 4))
    tuft = sc.piece("tuft", 3, 5, fa.leaf(OWL_TUFT, "lance", 6, vein=False))
    beak = sc.piece("beak", 2, 3, solid(BEAK, 7))
    foot = sc.piece("foot", 3, 2, solid(BEAK, 8))
    tail = sc.piece("tail", 8, 4, felt(OWL, 9, k=1))
    els = [cube((5.0, 0.6, 6.0), (11.0, 9.0, 11.5), {"north": belly, "south": body, "east": body, "west": body, "up": head}),
           cube((5.2, 9.0, 6.2), (10.8, 12.6, 11.3), {"north": face, "south": head, "east": head, "west": head, "up": head}),
           cube((7.4, 9.4, 5.6), (8.6, 10.8, 6.2), {s: beak for s in ("north", "east", "west", "down")}),
           cube((4.4, 2.0, 7.0), (5.0, 8.4, 11.0), {s: wing for s in ("west", "north", "south", "up", "down")}),
           cube((11.0, 2.0, 7.0), (11.6, 8.4, 11.0), {s: wing for s in ("east", "north", "south", "up", "down")}),
           cube((6.0, 0.0, 5.2), (7.4, 0.8, 6.6), {s: foot for s in ("north", "up", "east", "west")}),
           cube((8.6, 0.0, 5.2), (10.0, 0.8, 6.6), {s: foot for s in ("north", "up", "east", "west")}),
           cube((6.0, 0.6, 11.5), (10.0, 2.6, 12.4), {s: tail for s in ("south", "up", "east", "west", "down")}),
           cube((5.4, 12.4, 7.6), (6.8, 14.6, 9.0), {s: tuft for s in SIDES4 + ("up",)}, rotation((6.1, 12.4, 8.3), "z", 22.5)),
           cube((9.2, 12.4, 7.6), (10.6, 14.6, 9.0), {s: tuft for s in SIDES4 + ("up",)}, rotation((9.9, 12.4, 8.3), "z", -22.5))]
    sc.models["owl_plush"] = els
    return sc


def hedgehog_plush():
    sc = Sculpt("hedgehog_plush", 82)
    back = sc.piece("spines", 18, 18, spines(2))
    side = sc.piece("spines_side", 18, 10, spines(3, jagged=True))
    rear = sc.piece("spines_rear", 16, 10, spines(4, jagged=True))
    face = sc.piece("face", 14, 10, plush_face(HEDGEHOG_FACE, [(0.27, 0.42), (0.73, 0.42)], "smile", eye_size=0.12))
    belly = sc.piece("belly", 14, 16, felt(HEDGEHOG_FACE, 5))
    snout = sc.piece("snout", 5, 4, felt(HEDGEHOG_FACE, 6, seam=False, k=2))
    nose = sc.piece("nose", 2, 2, solid(PUPIL, 7))
    foot = sc.piece("foot", 3, 2, solid(HEDGEHOG[:3], 8))
    els = [cube((4.8, 0.6, 4.0), (11.2, 5.0, 12.0), {"north": face, "down": belly, "east": belly, "west": belly, "south": belly}),
           cube((4.2, 2.0, 5.0), (11.8, 6.6, 12.8), {"up": back, "east": side, "west": side, "south": rear}),
           cube((5.4, 6.6, 6.0), (10.6, 7.4, 11.6), {"up": back, "east": side, "west": side, "south": rear, "north": side}),
           cube((6.8, 1.2, 2.8), (9.2, 3.2, 4.0), {"north": snout, "east": snout, "west": snout, "up": snout, "down": snout}),
           cube((7.4, 2.2, 2.4), (8.6, 3.2, 2.8), {s: nose for s in ("north", "east", "west", "up")})]
    for x in (5.4, 9.4):
        for z in (4.6, 10.4):
            els.append(cube((x, 0.0, z), (x + 1.2, 0.8, z + 1.2), {s: foot for s in SIDES4}))
    sc.models["hedgehog_plush"] = els
    return sc


def acorn_plush():
    sc = Sculpt("acorn_plush", 83)
    body = sc.piece("body", 12, 14, felt(ACORN, 2))
    face = sc.piece("face", 12, 14, plush_face(ACORN, [(0.3, 0.42), (0.7, 0.42)], "worried", eye_size=0.13))
    bottom = sc.piece("bottom", 10, 10, felt(ACORN, 3, k=2))
    cap = sc.piece("cap", 16, 5, acorn_cap(4))
    cap_top = sc.piece("cap_top", 16, 16, acorn_cap(5))
    stem = sc.piece("stem", 3, 4, strip(STEM_BROWN, 6))
    limb = sc.piece("limb", 3, 6, felt(ACORN, 7, seam=False, k=1))
    els = [cube((5.0, 1.2, 5.0), (11.0, 8.2, 11.0), {"north": face, "south": body, "east": body, "west": body}),
           cube((5.6, 0.6, 5.6), (10.4, 1.2, 10.4), {**{s: bottom for s in SIDES4}, "down": bottom}),
           cube((4.2, 8.0, 4.2), (11.8, 10.5, 11.8), {**{s: cap for s in SIDES4}, "down": cap_top, "up": cap_top}),
           cube((5.0, 10.5, 5.0), (11.0, 11.3, 11.0), {**{s: cap for s in SIDES4}, "up": cap_top}),
           column(8.0, 8.0, 11.3, 13.0, 1.2, stem, rot=rotation((8.0, 11.3, 8.0), "z", -22.5)),
           cube((3.8, 3.4, 7.2), (5.0, 6.4, 8.6), {s: limb for s in SIDES4 + ("down",)}, rotation((5.0, 6.4, 7.9), "z", -22.5)),
           cube((11.0, 3.4, 7.2), (12.2, 6.4, 8.6), {s: limb for s in SIDES4 + ("down",)}, rotation((11.0, 6.4, 7.9), "z", 22.5)),
           cube((6.0, 0.0, 6.8), (7.4, 1.2, 8.4), {s: limb for s in SIDES4}),
           cube((8.6, 0.0, 6.8), (10.0, 1.2, 8.4), {s: limb for s in SIDES4})]
    sc.models["acorn_plush"] = els
    return sc


def corn_plush():
    sc = Sculpt("corn_plush", 84)
    face = sc.piece("face", 10, 22, kernels(2, face=True))
    cob = sc.piece("cob", 10, 22, kernels(3))
    tip = sc.piece("tip", 8, 8, kernels(4))
    husk = sc.piece("husk", 8, 18, fa.leaf(HUSK, "lance", 5, wavy=0.4))
    arm = sc.piece("arm", 5, 10, fa.leaf(HUSK, "lance", 6))
    silk = sc.piece("silk", 6, 6, fa.blades(SILK, 7, 8, droop=0.6))
    leg = sc.piece("leg", 3, 4, strip(HUSK[:3], 9))
    els = [cube((6.0, 1.4, 6.0), (10.0, 12.4, 10.0), {"north": face, "south": cob, "east": cob, "west": cob}),
           cube((6.5, 12.4, 6.5), (9.5, 13.2, 9.5), {**{s: tip for s in SIDES4}, "up": tip}),
           plane_xy(6.5, 9.5, 13.2, 15.0, 8.0, silk), plane_zy(6.5, 9.5, 13.2, 15.0, 8.0, silk),
           # Husk leaves at the sides and back, leaning out, and two smaller ones in front like arms.
           plane_zy(5.5, 10.5, 0.8, 9.8, 5.7, husk, rotation((5.7, 0.8, 8.0), "z", 22.5)),
           plane_zy(5.5, 10.5, 0.8, 9.8, 10.3, husk, rotation((10.3, 0.8, 8.0), "z", -22.5)),
           plane_xy(5.5, 10.5, 0.8, 9.8, 10.3, husk, rotation((8.0, 0.8, 10.3), "x", 22.5)),
           plane_xy(3.6, 6.0, 4.0, 9.0, 6.4, arm, rotation((6.0, 4.0, 6.4), "z", 45)),
           plane_xy(10.0, 12.4, 4.0, 9.0, 6.4, arm, rotation((10.0, 4.0, 6.4), "z", -45)),
           cube((6.6, 0.0, 7.2), (7.6, 1.4, 8.4), {s: leg for s in SIDES4}),
           cube((8.4, 0.0, 7.2), (9.4, 1.4, 8.4), {s: leg for s in SIDES4})]
    sc.models["corn_plush"] = els
    return sc


def maple_leaf_plush():
    sc = Sculpt("maple_leaf_plush", 85)
    front = sc.piece("front", 28, 28, maple_leaf(True))
    back = sc.piece("back", 28, 28, maple_leaf(False))
    filling = sc.piece("filling", 12, 12, felt(MAPLE, 3))
    edge = sc.piece("edge", 12, 3, felt(MAPLE, 4, seam=False, k=2))
    stem = sc.piece("stem", 3, 8, strip(STEM_BROWN, 5))
    mitt = sc.piece("mitt", 3, 3, solid(MAPLE[:3], 6))
    lean = rotation((8.0, 2.0, 8.0), "x", 22.5)
    els = [column(8.0, 8.0, 0.0, 6.5, 1.2, stem, rot=lean),
           cube((4.6, 4.0, 7.2), (11.4, 11.0, 8.8), {"up": edge, "down": edge, "east": edge, "west": edge, "north": filling, "south": filling}, lean),
           dict(plane_xy(1.0, 15.0, 2.0, 16.0, 7.1, front, lean), faces={"north": plane_xy(1.0, 15.0, 2.0, 16.0, 7.1, front)["faces"]["north"]}),
           dict(plane_xy(1.0, 15.0, 2.0, 16.0, 8.9, back, lean), faces={"south": plane_xy(1.0, 15.0, 2.0, 16.0, 8.9, back)["faces"]["south"]}),
           cube((2.4, 6.0, 7.4), (3.8, 7.4, 8.6), {s: mitt for s in SIDES4 + ("up", "down")}, lean),
           cube((12.2, 6.0, 7.4), (13.6, 7.4, 8.6), {s: mitt for s in SIDES4 + ("up", "down")}, lean)]
    sc.models["maple_leaf_plush"] = els
    return sc


PLUSH_BUILDERS = {"owl_plush": owl_plush, "hedgehog_plush": hedgehog_plush, "acorn_plush": acorn_plush, "corn_plush": corn_plush,
                  "maple_leaf_plush": maple_leaf_plush}

_built = {}


def build(name):
    if name not in _built:
        builders = {"flying_eyeball": flying_eyeball, "specimen_eye": specimen_eye, "monster_head": monster_head, **PLUSH_BUILDERS,
                    **{block: (lambda b=block: pillar_candles(b)) for block in d16.PILLAR_CANDLES}}
        _built[name] = builders[name]()
    return _built[name]


def plush_model(name):
    """The model of harvest plush `name`, for tools/midway_data.py."""
    sc = build(name)
    return fa.model(sc.name, sc.models[name], PLUSH_DISPLAY)


PLUSH_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.9, 0.9, 0.9]},
                 "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                 "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
                 "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
                 "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
                 "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]}}


# ---------------------------------------------------------------- files

def assets(root, write, lang):
    models = root / "models" / "block"
    states = root / "blockstates"
    items = root / "models" / "item"
    lang.update({f"block.{MOD}.{block}": display for block, display in d16.names().items()})

    eye = d16.FLYING_EYEBALL["block"]
    sc = build(eye)
    # Drawn by the client so it can hover, beat its wings and turn; the block model only gives the particles.
    write(models / f"{eye}.json", {"textures": {"particle": rid(f"block/{eye}")}})
    write(states / f"{eye}.json", {"variants": {"": {"model": rid(f"block/{eye}")}}})
    write(items / f"{eye}.json", fa.model(eye, sc.models["flying_eyeball_item"], fa.PLANT_DISPLAY))
    write(root / "decor16_quads.json", eyeball_quads())

    for block in d16.PILLAR_CANDLES:
        sc = build(block)
        for model, elements in sc.models.items():
            write(models / f"{model}.json", fa.model(block, elements))
        write(states / f"{block}.json", {"variants": {
            f"candles={n},lit={str(lit).lower()}": {"model": rid(f"block/{block}_{n}" + ("_lit" if lit else ""))}
            for n in d16.CANDLES for lit in (False, True)}})
        write(items / f"{block}.json", {"parent": rid(f"block/{block}_1"), "display": fa.PLANT_DISPLAY})

    web_block = d16.SPIDER_WEB["block"]
    write(models / f"{web_block}.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
                                         "textures": {"web": rid(f"block/{web_block}"), "particle": rid(f"block/{web_block}")},
                                         "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1], "shade": False,
                                                       "faces": {"north": {"uv": [16, 0, 0, 16], "texture": "#web"},
                                                                 "south": {"uv": [0, 0, 16, 16], "texture": "#web"}}}]})
    write(states / f"{web_block}.json", {"multipart": [{"apply": {"model": rid(f"block/{web_block}"), **turn}, "when": {face: "true"}}
                                                       for face, turn in FACE_TURNS.items()]})
    write(items / f"{web_block}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"block/{web_block}")}})

    head = d16.MONSTER_HEAD["block"]
    sc = build(head)
    for model, elements in sc.models.items():
        write(models / f"{model}.json", fa.model(head, elements))
    write(states / f"{head}.json", {"variants": {
        f"facing={f},powered={str(p).lower()}": {"model": rid(f"block/{head}" + ("_awake" if p else "")), **({"y": y} if y else {})}
        for f, y in FACINGS.items() for p in (False, True)}})
    write(items / f"{head}.json", {"parent": rid(f"block/{head}"), "display": fa.PLANT_DISPLAY})

    for block in d16.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})


def loot(out, write):
    from decor_data import self_drop
    for block in (d16.FLYING_EYEBALL["block"], d16.MONSTER_HEAD["block"]):
        write(out / f"{block}.json", self_drop(block))
    # A candle for each candle in the block, as vanilla's candles.
    for block in d16.PILLAR_CANDLES:
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
            *[{"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": rid(block), "state": {"candles": str(n)}},
               "count": n} for n in range(2, 5)], {"type": "minecraft:explosion_decay"}], "name": rid(block)}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")})
    # A web for each face it covers, as vanilla's multiface blocks.
    web_block = d16.SPIDER_WEB["block"]
    write(out / f"{web_block}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
        *[{"type": "minecraft:set_count", "add": True, "condition": {"type": "minecraft:match_block", "blocks": rid(web_block),
                                                                     "state": {face: "true"}}, "count": 1}
          for face in ("north", "east", "south", "west", "up", "down")],
        {"type": "minecraft:set_count", "add": True, "count": -1}, {"type": "minecraft:explosion_decay"}], "name": rid(web_block)}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{web_block}")})


def tags(tags):
    for block in d16.PILLAR_CANDLES:
        # Lit with flint and steel, or a fire charge, or a burning arrow, put out by hand or a splash, as vanilla's.
        tags.add("block", "minecraft:candles", rid(block))
    tags.add("block", "minecraft:mineable/pickaxe", rid(d16.MONSTER_HEAD["block"]))
    tags.add("block", "minecraft:sword_efficient", rid(d16.SPIDER_WEB["block"]))


def textures():
    """(kind, name) -> image for each prop's and harvest plush's texture, and the Specimen Jar's eye (tools/decor8_data.py)."""
    out = {("block", name): build(name).atlas.img for name in [d16.FLYING_EYEBALL["block"], "specimen_eye", *d16.PILLAR_CANDLES,
                                                                d16.MONSTER_HEAD["block"], *PLUSH_BUILDERS]}
    out[("block", d16.SPIDER_WEB["block"])] = web()
    return out
