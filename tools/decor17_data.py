"""Generated data for Halloween decorations batch 17, the Witch's Workshop (tools/decor17.py): sculpted models on the toolkit
of tools/flora_art.py (each block one texture painted here, 64 x 64 or 128 x 128 for the detailed ones), blockstates,
items, names, loot and tags; the candelabra's candle layout for Java (/jugcraft/candelabra.json); the quads the client
draws the moving parts from (assets/jugcraft/decor17_quads.json); and the small greyscale textures the client tints
(brew, fumes, wax, flames).

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import math
import random

from PIL import Image

import block_style as bs
import cute_art as ca
import decor16_data as d16d
import decor8_data as d8d
import agriculture as ag
import decor17 as d17
import flora_art as fa
from flora_art import SIDES4, Px, Sculpt, column, cube, pal, plane_xy, plane_xz, plane_zy, rotation, shade, solid, strip
from decor6_data import quads
from decor_data import block_model

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL6 = ("north", "south", "east", "west", "up", "down")

IRON = pal("0b0b0e", "141419", "1d1e24", "282a32", "353843", "464a57", "5e6372")
POT = pal("0c0b0e", "151318", "1e1b22", "29252e", "35303b", "433d4a", "554e5e")
RUST = pal("2a140c", "46200f", "653216", "84461e")
BONE = pal("8a7a5c", "b5a684", "d6cba8", "ebe2c4", "f8f3e2", "fffaf0")
HORN = pal("2a221a", "3e3426", "564836", "6e5c44", "8a7556", "a6906c")
SOCKET = pal("070505", "120d0b", "1e1612")
EMBER = pal("3a0a02", "7a1a04", "c23c0a", "ec7416", "ffae3c", "ffe08a")
COAL = pal("0a0909", "141212", "1e1a19", "2a2422", "3a322e")
STONE = pal("1a1918", "272524", "353231", "45413e", "57524d", "6c665f")
WOOD = pal("1a0f0a", "28170e", "3a2214", "4e2e1c", "653c24", "7e4e30")
VELVET = pal("1c0a20", "2c1032", "401848", "56225f", "6e2e78")
BRASS = pal("4a3210", "6e4e1a", "967024", "bc9436", "dcb854", "f4dc8c")
GLASS = (214, 232, 236)
TWIG = pal("261a0e", "3a2816", "52381e", "6c4c28", "866034", "a07a44")
CORD = pal("2e2014", "4a3620", "665030", "826a44")
TIN = pal("2e3136", "44484e", "5c6168", "747a82", "8e959e", "a8afb8")
COPPER = pal("5a2a14", "84401e", "aa5a2c", "cc7a44", "e8a068")
PAPER = pal("7a6c4e", "a29470", "c4b892", "dcd2b0", "eee6ca")
INK = pal("1e1612", "3a2c22")
AMETHYST = pal("3a1a5c", "58288a", "7a3cb4", "9c5cd4", "c08cf0")
HEART = pal("320508", "560a0e", "7e1418", "a42226", "c43a3c", "e06a66")
FAT = pal("b8a064", "d8c484", "eee0a8")
FUR = pal("100c0c", "1c1616", "2a2222", "3a302e", "4e423e")
BAT_WING = pal("120c0e", "1e1618", "2c2224", "3e3234", "524446")
SCALE = pal("142410", "203a16", "30521e", "426c28", "5a8a36", "7aa84a")
BELLY = pal("7c7234", "a49a50", "c8bc70", "e0d494")
SKIN = pal("5e4e44", "7e6c60", "9e8a7c", "baa696", "d2c0b0", "e6d8ca")
NAIL = pal("7a6a5a", "a09080", "c4b6a6")
MURK = {"jar_of_eyeballs": pal("2e3a10", "44521a", "5e6c24"), "beating_heart_jar": pal("4a0a10", "6e1420", "902430"),
        "bat_in_a_jar": pal("3a2c18", "4e3c22", "62502e"), "two_headed_snake_jar": pal("2e4a2a", "3e6238", "507a48"),
        "hand_in_a_jar": pal("6a5a24", "8a7832", "a69444")}
LUNA = pal("4a6e3a", "6a9a4a", "8ec462", "b2e08a", "d4f4b4", "eefcd8")
LUNA_EYE = pal("6a2a10", "a65a20", "f0c060")
HAWK = pal("1a120c", "2e2014", "4a341e", "6a4c2a", "8e6a3a", "b89050")
HAWK_SKULL = pal("c8a848", "e8cc6a")
ATLAS = pal("4a120a", "6e2010", "963218", "b84a22", "d06a34", "e89a5c")
ATLAS_WINDOW = pal("e8e0cc", "fffaf0")
MOTH_BODY = pal("2a1e14", "4a3624", "6e5438")
BACKING = pal("b4a882", "ccc29c", "e0d8b8")


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def mirror_uv(uv):
    u0, v0, u1, v1 = uv
    return (u1, v0, u0, v1)


# ---------------------------------------------------------------- painters

def noise(palette, seed=1, base=None, spread=(0, 0, 0, -1, 1)):
    def paint(p):
        rng = random.Random(seed)
        k = len(palette) // 2 if base is None else base
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(palette, k + (0 if fa.QUIET else rng.choice(spread))))
    return paint


def cast_iron(seed=1, rust=0.05, ridge=None):
    """Black cast iron: dark, faintly pitted, a sheen down one side, a few rust blooms; `ridge` rows lit as a lip."""
    def paint(p):
        rng = random.Random(seed)
        n = len(POT)
        for y in range(p.h):
            for x in range(p.w):
                across = x / max(1, p.w - 1)
                k = 2 + int((1 - abs(across - 0.3) * 1.6) * 2) + rng.choice((0, 0, 0, -1, 1))
                p.put(x, y, shade(POT, k))
                if rng.random() < 0.02:
                    p.put(x, y, POT[0])
        for _ in range(int(p.w * p.h * rust / 6)):
            cx, cy = rng.randrange(p.w), rng.randrange(p.h)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
                if rng.random() < 0.6:
                    p.put(cx + dx, cy + dy, rng.choice(RUST[:2]))
        for y in (ridge or ()):
            for x in range(p.w):
                p.put(x, y, POT[n - 1] if x % 3 else POT[n - 2])
    return paint


def band(seed=1, rivets=4):
    """A riveted iron band: a lit top edge, a dark bottom one, round rivet heads along it."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 4 if y == 0 else (1 if y == p.h - 1 else 3)
                p.put(x, y, shade(IRON, k + rng.choice((0, 0, -1))))
        step = max(3, p.w // max(1, rivets))
        for x in range(step // 2, p.w, step):
            cy = p.h // 2
            p.put(x, cy, IRON[6])
            p.put(x + 1, cy, IRON[5])
            p.put(x, cy + 1, IRON[2])
            p.put(x + 1, cy + 1, IRON[1])
    return paint


def wrought(seed=1, horizontal=False):
    """Hammered wrought iron: near black, a highlight along one edge, hammer marks."""
    def paint(p):
        rng = random.Random(seed)
        n = len(IRON)
        for y in range(p.h):
            for x in range(p.w):
                across = (y / max(1, p.h - 1)) if horizontal else (x / max(1, p.w - 1))
                k = n - 2 - int(across * (n - 2)) + rng.choice((0, 0, 0, -1, 1))
                p.put(x, y, shade(IRON, k))
        for _ in range(max(1, p.w * p.h // 30)):
            p.put(rng.randrange(p.w), rng.randrange(p.h), IRON[1])
    return paint


def wrought_clean(horizontal=False):
    """Wrought iron, clean (5 October 2026; it replaces the speckled `wrought` everywhere but the Beating Heart Jar's lid,
    which the owner loves as it is): flat dark iron, a lit line along its top and left and a dark one along its bottom
    and right; `horizontal` lights only the top and shades only the bottom, for a long bar."""
    return ca.bevel(IRON, 3, light=2, dark=2, sides="tb" if horizontal else "tlbr")


def twisted(seed=1):
    """A twisted iron bar seen from the side: bright ridges running diagonally round it, evenly (`seed` is kept for
    callers and no longer speckles it)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                phase = (x * 1.6 + y) % 5
                k = 5 if phase < 1 else (4 if phase < 2 else 2)
                p.put(x, y, shade(IRON, k))
    return paint


def scroll(kind="leg", seed=1):
    """A flat wrought-iron scroll, cut out, its bar two texels thick and lit along its upper side. `leg`: a foot curving
    out and down from its top inner corner to a tight curl on the floor; `bracket`: a curve rising from a curl at the
    stem's foot out to the arm's tip above; `crest`: two C-scrolls back to back under a little spire."""
    def paint(p):
        w, h = p.w, p.h
        pts = []

        def bezier(p0, p1, p2, steps=90):
            for i in range(steps + 1):
                t = i / steps
                x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
                y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
                pts.append((x, y, 1.0))

        def curl(cx, cy, r, start, direction, turns=1.15):
            steps = int(110 * turns)
            for i in range(steps + 1):
                t = i / steps
                a = start + direction * t * turns * math.tau
                rr = r * (1 - t * 0.8)
                pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr, 1.0 - t * 0.5))

        if kind == "leg":
            bezier((w * 0.06, h * 0.04), (w * 0.9, h * 0.08), (w * 0.82, h * 0.8))
            curl(w * 0.66, h * 0.8, w * 0.16, 0.0, 1)
        elif kind == "bracket":
            bezier((w * 0.12, h * 0.85), (w * 0.12, h * 0.1), (w * 0.95, h * 0.06))
            curl(w * 0.28, h * 0.85, w * 0.16, math.pi, -1)
        else:
            curl(w * 0.3, h * 0.62, h * 0.24, 0.0, -1)
            curl(w * 0.7, h * 0.62, h * 0.24, math.pi, 1)
            for y in range(int(h * 0.05), int(h * 0.62)):
                pts.append((w * 0.5, y, 1.0))
            for i in range(4):
                pts.append((w * 0.5 - 1, h * 0.06 + i, 1.0))
                pts.append((w * 0.5 + 1, h * 0.06 + i, 1.0))
        for x, y, thick in pts:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if abs(dx) + abs(dy) <= (1 if thick > 0.7 else 0):
                        p.put(x + dx, y + dy, IRON[5] if dy < 0 else (IRON[3] if dy == 0 else IRON[1]))
    return paint


def drip_pan():
    """A wrought-iron drip pan from above: a dish with a lit rim and a dark well, its square corners solid iron (opaque,
    so the pan never shows its own inside)."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot(x - cx, y - cy) / (p.w / 2)
                p.put(x, y, IRON[5] if d > 0.78 else (IRON[2] if d > 0.4 else IRON[1]))
    return paint


def bone(seed=1, cracks=2, porous=True):
    """Clean bone: smooth warm cream, lit along its top and left and shaded along its bottom and right (the clean art
    style of tools/cute_art.py; `seed`, `cracks` and `porous` are kept for callers and no longer scatter marks)."""
    return ca.soft(ca.BONE, 3)


def horn(seed=1):
    """A ram's horn: grey-brown keratin in even growth rings across it, lit along one side."""
    return ca.bands(HORN, 3, period=4)


def claw():
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, IRON[4] if y < p.h * 0.4 else IRON[2])
        p.put(p.w // 2, p.h - 1, IRON[6])
    return paint


def embers(seed=1):
    """Glowing embers: orange-hot coals with black crusts and white-hot cracks."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                v = rng.random()
                p.put(x, y, COAL[rng.randrange(1, 4)] if v < 0.3 else shade(EMBER, 2 + int(v * 3.5)))
        for _ in range(p.w * p.h // 18):
            p.put(rng.randrange(p.w), rng.randrange(p.h), EMBER[5])
    return paint


def stones(seed=1, glow_hole=False):
    """Sooty fieldstones laid in courses with dark mortar; with an arched draft hole showing embers if `glow_hole`."""
    def paint(p):
        rng = random.Random(seed)
        w, h = p.w, p.h
        for y in range(h):
            for x in range(w):
                p.put(x, y, STONE[0])
        y = 0
        while y < h:
            course = rng.choice((3, 4, 4, 5))
            x = -rng.randrange(0, 4)
            while x < w:
                length = rng.choice((4, 5, 6, 7))
                tone = rng.randrange(2, 5)
                for yy in range(y, min(h, y + course - 1)):
                    for xx in range(max(0, x), min(w, x + length - 1)):
                        edge = yy == y or xx == x
                        k = tone + (1 if edge else 0) - (1 if yy == y + course - 2 else 0) + rng.choice((0, 0, -1))
                        p.put(xx, yy, shade(STONE, k))
                x += length
            y += course
        # Soot climbing from the bottom.
        for x in range(w):
            for yy in range(h - int(h * (0.25 + rng.random() * 0.2)), h):
                if rng.random() < 0.55:
                    p.put(x, yy, STONE[rng.randrange(0, 2)])
        if glow_hole:
            cx = w / 2
            for yy in range(h):
                for xx in range(w):
                    dx = (xx - cx) / (w * 0.22)
                    dy = (yy - h * 0.7) / (h * 0.32)
                    if (dy > 0 and abs(dx) < 1 and yy < h - 1) or dx * dx + dy * dy < 1:
                        if yy < h - 1:
                            p.put(xx, yy, shade(EMBER, 2 + rng.randrange(0, 4)))
    return paint


def wood(palette=WOOD, seed=1, grain=True, panel=False):
    """Dark polished wood: long grain, a lit edge; with a sunken panel's bevel if `panel`. In the clean style
    (flora_art.QUIET) the grain is thin streaks along the piece's length, as on vanilla planks."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        if fa.QUIET and grain:
            bs.streaks(palette[1:n - 1], seed, vertical=p.h > p.w, spread=0.7)(p)
        else:
            offsets = [rng.random() * 6 for _ in range(p.h + 1)]
            for y in range(p.h):
                for x in range(p.w):
                    g = math.sin((x + offsets[y // 3] * 2) * 0.9 + y * 0.05) if grain else 0
                    k = n // 2 + (1 if g > 0.6 else 0) - (1 if g < -0.7 else 0) + (0 if fa.QUIET else rng.choice((0, 0, 0, -1)))
                    p.put(x, y, shade(palette, k))
        if panel and p.w > 6 and p.h > 6:
            for x in range(2, p.w - 2):
                p.put(x, 2, palette[n - 1])
                p.put(x, p.h - 3, palette[1])
            for y in range(2, p.h - 2):
                p.put(2, y, palette[n - 1])
                p.put(p.w - 3, y, palette[1])
    return paint


def carved_crest(seed=1):
    """The cabinet's crest: a moth with spread wings over a little skull, carved in relief in the dark wood."""
    def paint(p):
        wood(WOOD, seed, panel=False)(p)
        w, h = p.w, p.h
        cx, cy = w / 2, h * 0.45
        for y in range(h):
            for x in range(w):
                dx, dy = (x - cx) / (w * 0.45), (y - cy) / (h * 0.42)
                wing = abs(dx) < 1 and abs(dy) < 1 and abs(dy) < 1.15 - abs(dx) * 0.6 and abs(dx) > 0.08
                if wing:
                    p.put(x, y, WOOD[4] if dy < 0 else WOOD[3])
        for y in range(int(h * 0.15), int(h * 0.85)):
            p.put(cx, y, WOOD[5])
        # The skull below the moth.
        sx, sy = cx, h * 0.82
        for y in range(h):
            for x in range(w):
                if math.hypot(x - sx, (y - sy) * 1.3) < h * 0.14:
                    p.put(x, y, BONE[3])
        p.put(sx - 1, sy, SOCKET[0])
        p.put(sx + 1, sy, SOCKET[0])
    return paint


def glass(alpha=70, streak=True, rim=True):
    """Clear glass: faint, with a pale rim and a streak of reflection."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, GLASS, alpha)
        if rim:
            for x in range(p.w):
                p.put(x, 0, (236, 246, 248), 150)
                p.put(x, p.h - 1, (236, 246, 248), 150)
        if streak:
            for y in range(1, p.h - 1):
                x = int(p.w * 0.22 + y * 0.08)
                p.put(x, y, (250, 254, 255), 170)
                if y % 3:
                    p.put(x + 1, y, (250, 254, 255), 120)
    return paint


def fluid(palette, seed=1, alpha=150):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, palette[1 if fa.QUIET else rng.choice((0, 1, 1, 2))], alpha)
    return paint


def label(seed=1, lines=3):
    """A paper label, foxed at the edges, with lines of faded handwriting."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                edge = x in (0, p.w - 1) or y in (0, p.h - 1)
                p.put(x, y, PAPER[1] if edge else shade(PAPER, 3 + rng.choice((0, 0, -1))))
        for line in range(lines):
            y = int((line + 1) * p.h / (lines + 1))
            x = 2
            while x < p.w - 2:
                length = rng.randrange(2, 5)
                for xx in range(x, min(p.w - 2, x + length)):
                    p.put(xx, y + (1 if rng.random() < 0.2 else 0), INK[rng.randrange(2)])
                x += length + 1
    return paint


def label_clean(lines=2):
    """A paper label, clean: flat paper inside a one-texel darker edge, and lines of ink as even dashes (a word, a gap,
    a longer word), each line starting a step further in."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                edge = x in (0, p.w - 1) or y in (0, p.h - 1)
                p.put(x, y, PAPER[1] if edge else PAPER[3])
        words = (3, 2, 4, 2)
        for line in range(lines):
            y = int((line + 1) * p.h / (lines + 1))
            x, k = 2 + line, line
            while x < p.w - 2:
                for xx in range(x, min(p.w - 2, x + words[k % len(words)])):
                    p.put(xx, y, INK[1])
                x += words[k % len(words)] + 1
                k += 1
    return paint


def velvet(seed=1):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 2 + (1 if (x + y) % 7 == 0 else 0) + (0 if fa.QUIET else rng.choice((0, 0, -1)))
                p.put(x, y, shade(VELVET, k))
    return paint


def brass(seed=1):
    return strip(BRASS, seed, light=True)


def twigs(seed=1, glow=False):
    """A besom's bristles: thin twigs side by side running down the piece, their ends ragged at the bottom."""
    def paint(p):
        rng = random.Random(seed)
        for x in range(p.w):
            length = p.h - rng.choice((0, 0, 1, 2, 3))
            k = rng.randrange(1, len(TWIG))
            for y in range(length):
                p.put(x, y, shade(TWIG, k + (1 if y % 7 == 0 else 0) - (1 if y > length - 3 else 0)))
            if glow and rng.random() < 0.3:
                p.put(x, length - 1, AMETHYST[4])
    return paint


def cord(seed=1):
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, CORD[(x + y) % 3 + (1 if y == 0 else 0)])
    return paint


def gnarled(seed=1):
    """A crooked broom handle: bark-brown with knots."""
    return strip(TWIG, seed, knots=4, light=True)


def tin(seed=1, dents=3):
    def paint(p):
        rng = random.Random(seed)
        strip(TIN, seed, light=True)(p)
        for _ in range(dents):
            cx, cy = rng.randrange(p.w), rng.randrange(p.h)
            p.put(cx, cy, TIN[1])
            p.put(cx + 1, cy, TIN[4])
    return paint


def moth_wings(kind):
    """A moth's two wings on one side, painted together (forewing above, hindwing below), the body's edge on the
    right-hand side; seen from above with the head up, in clean flat colour. Luna: pale green, lighter along the leading
    edge, a round eyespot on each wing and long trailing tails; death's-head: brown forewings with a pale band over
    ochre hindwings in even black bands; atlas: rust red with pale tips and white windows."""
    def paint(p):
        w, h = p.w, p.h

        def fore(x, y):
            dx, dy = (w - 1 - x) / w, y / h
            return dx < 0.98 and dy < 0.55 and dy > 0.04 and dy < 0.08 + dx * 0.62 and dy > dx * 0.08

        def hind(x, y):
            dx, dy = (w - 1 - x) / w, y / h
            if kind == "luna":
                tail = dy > 0.5 and abs(dx - (dy - 0.5) * 0.9 - 0.18) < 0.06 and dy < 0.98
                return tail or (0.42 < dy < 0.82 and dx < 0.62 - (dy - 0.42) * 0.9)
            return 0.42 < dy < 0.84 and dx < 0.64 - abs(dy - 0.6) * 0.7
        for y in range(h):
            for x in range(w):
                f, hh = fore(x, y), hind(x, y)
                if not (f or hh):
                    continue
                out = (w - 1 - x) / w
                if kind == "luna":
                    c = LUNA[4] if f and y < h * 0.16 else LUNA[3] if f else LUNA[2]
                elif kind == "deaths_head":
                    c = (HAWK[4] if 0.4 < out < 0.55 else HAWK[3]) if f else (HAWK[0] if int((w - x) / 3) % 3 == 0 else HAWK_SKULL[0])
                else:
                    c = (ATLAS[5] if out > 0.82 and y < h * 0.16 else ATLAS[3]) if f else ATLAS[2]
                p.put(x, y, c)
        if kind == "luna":
            for cx, cy in ((w * 0.55, h * 0.26), (w * 0.62, h * 0.62)):
                r = max(1.2, w * 0.07)
                ca.ellipse(p, cx, cy, r, r, LUNA_EYE[1])
                p.put(cx - 0.5, cy - 0.5, LUNA_EYE[2])
        if kind == "atlas":
            for cx, cy in ((w * 0.5, h * 0.3), (w * 0.58, h * 0.6)):
                ca.ellipse(p, cx, cy + 1, 1.2, 1.6, ATLAS_WINDOW[1])
        p.outline((40, 28, 20))
    return paint


def thorax(kind):
    def paint(p):
        palette = {"luna": LUNA[3:], "deaths_head": HAWK[2:], "atlas": ATLAS[1:4]}[kind]
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(palette, 1 + (1 if x == 0 else 0) - (1 if y % 3 == 0 else 0)))
        if kind == "deaths_head":
            # The pale skull on its thorax that gives it its name.
            cx, cy = (p.w - 1) / 2, p.h * 0.3
            for y in range(p.h):
                for x in range(p.w):
                    if math.hypot(x - cx, (y - cy) * 1.4) < p.w * 0.42:
                        p.put(x, y, HAWK_SKULL[1])
            p.put(cx - 0.5, cy, HAWK[0])
            p.put(cx + 0.5, cy, HAWK[0])
    return paint


def heart(seed=1):
    """A cartoon heart's muscle: smooth deep red, a soft highlight high on its left, two clean vessels down it and a
    neat cap of fat along its top."""
    def paint(p):
        ca.soft(HEART, 3, edge=1, top=0.25, bottom=0.3)(p)
        ca.ellipse(p, p.w * 0.32, p.h * 0.38, max(1.0, p.w * 0.12), max(1.0, p.h * 0.09), HEART[5])
        for vx in (0.58, 0.74):
            x = p.w * vx
            for y in range(int(p.h * 0.2), int(p.h * 0.85)):
                p.put(x + (0.6 if y > p.h * 0.5 else 0), y, HEART[1])
        for x in range(p.w):
            for y in range(int(p.h * 0.18)):
                p.put(x, y, FAT[2] if y == 0 else FAT[1])
    return paint


def fur(seed=1):
    """Soft black fur: one flat tone with a lit top and left edge."""
    return ca.bevel(FUR, 2)


def scales(seed=1, belly=False):
    """Snake scales in neat offset rows, each a flat green lit at its top; a paler belly band along the bottom if
    `belly`."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                sx, sy = (x + (y // 2) % 2 * 2) % 4, y % 2
                k = 3 + (1 if sy == 0 and sx < 2 else 0) - (1 if sx == 3 else 0)
                p.put(x, y, SCALE[k])
        if belly:
            for y in range(int(p.h * 0.7), p.h):
                for x in range(p.w):
                    p.put(x, y, BELLY[2] if y % 2 else BELLY[3])
    return paint


def skin(seed=1, nail=False, palette=SKIN):
    """Smooth skin (pale, or in `palette`), lit along its top and shaded along its bottom; with a neat nail along the top
    if `nail`."""
    def paint(p):
        ca.soft(palette, 3)(p)
        if nail:
            for x in range(p.w):
                p.put(x, 0, NAIL[2])
                p.put(x, 1, NAIL[1])
    return paint


def stitches(seed=1, palette=SKIN):
    """Smooth skin with a neat stitched seam across its middle: a dark line crossed by evenly spaced stitches."""
    def paint(p):
        skin(seed, palette=palette)(p)
        y = p.h // 2
        for x in range(1, p.w - 1):
            p.put(x, y, palette[1])
        for x in range(2, p.w - 2, 3):
            for dy in (-1, 0, 1):
                p.put(x, y + dy, (40, 20, 18))
    return paint


# ---------------------------------------------------------------- 1. the horned skull cauldron

def cute_jaw():
    """The ram skull's jaw: a soft cream strip with a neat row of little square teeth."""
    def paint(p):
        ca.soft(ca.BONE, 2)(p)
        ca.teeth(p, 1, p.w - 1, 0, 2, ca.BONE[4], ca.SOCKET[1], tooth=2)
    return paint


def horned_skull_cauldron():
    """A squat black-iron pot on four clawed feet with two riveted bands, a lip round its rim and a hollow inside lined
    with iron down to its floor, and a ram's skull bolted to its front, its ringed horns curling out past the rim. The
    skull's eye sockets are square and dark, the Minecraft way (SKULL_EYES, which the client's glow covers exactly);
    there are no nose holes. The brew, the floating things, the fumes and the glow are the client's (the pot's inside is
    3.3..12.7 across, its floor at 8.5).

    Closed everywhere (5 October 2026, after the owner saw through the old pot's open shells): the belly is three boxes
    side by side that never overlap, with undersides; its shoulder outside the neck is capped with flat plates; the neck,
    the rim and the band round the neck are box_ring frames, so each is capped on top without lidding the inside, and the
    neck's lining runs from the inside floor up to the rim. Every piece is sized to its face (about 4 to 5 texels a
    pixel), so no face squeezes a large picture into a thin strip."""
    sc = Sculpt(d17.CAULDRON["block"], 171, 128)
    # Packed row by row, so the tall pieces go first, side by side.
    under = sc.piece("under", 36, 36, ca.bevel(ca.IRON, 1, light=0))
    floor = sc.piece("floor", 36, 36, ca.bevel(ca.IRON, 1, edge=2))
    under_side = sc.piece("under_side", 10, 36, ca.bevel(ca.IRON, 1, light=0))
    shoulder_side = sc.piece("shoulder_side", 4, 36, ca.bevel(ca.IRON, 3))
    neck_under_side = sc.piece("neck_under_side", 5, 36, ca.bevel(ca.IRON, 1, light=0))
    rim_top_side = sc.piece("rim_top_side", 4, 36, ca.bevel(ca.IRON, 4))
    neck_band_top_side = sc.piece("neck_band_top_side", 1, 36, ca.bevel(ca.IRON, 4, edge=0))
    outer = sc.piece("outer", 46, 31, ca.soft(ca.IRON, 2))
    outer_end = sc.piece("outer_end", 10, 31, ca.soft(ca.IRON, 2))
    face = sc.piece("skull_face", 28, 22, ca.block_skull_face(SKULL_FACE, SKULL_EYES))
    side = sc.piece("skull_side", 6, 22, ca.soft(ca.BONE, 2))
    nose = sc.piece("snout", 16, 12, ca.soft(ca.BONE, 3, bottom=0.3))
    nose_side = sc.piece("snout_side", 6, 12, ca.soft(ca.BONE, 2))
    neck = sc.piece("neck", 46, 16, ca.soft(ca.IRON, 2, top=0.4, bottom=0.0))
    inner = sc.piece("inner", 46, 16, ca.bevel(ca.IRON, 1))
    foot = sc.piece("foot", 8, 12, ca.bevel(ca.IRON, 2))
    horns = sc.piece("horn", 12, 10, ca.bands(ca.HORN, 3, period=4, width=1))
    neck_end = sc.piece("neck_end", 5, 16, ca.soft(ca.IRON, 2, top=0.4, bottom=0.0))
    rim = sc.piece("rim", 52, 4, ca.bevel(ca.IRON, 4))
    band = sc.piece("band", 52, 4, ca.riveted(ca.IRON, 3, 8))
    foot_end = sc.piece("foot_end", 8, 8, ca.bevel(ca.IRON, 2))
    band_flat = sc.piece("band_flat", 8, 8, ca.bevel(ca.IRON, 3))
    rim_top = sc.piece("rim_top", 52, 4, ca.bevel(ca.IRON, 4))
    neck_band = sc.piece("neck_band", 48, 4, ca.riveted(ca.IRON, 3, 8))
    band_end = sc.piece("band_end", 8, 4, ca.bevel(ca.IRON, 3))
    rim_end = sc.piece("rim_end", 4, 4, ca.bevel(ca.IRON, 4))
    tiny_end = sc.piece("tiny_end", 1, 4, ca.bevel(ca.IRON, 3, edge=0))
    bolt = sc.piece("bolt", 4, 4, d16d.bolt_head())
    neck_under = sc.piece("neck_under", 46, 5, ca.bevel(ca.IRON, 1, light=0))
    base = sc.piece("base", 48, 6, ca.bevel(ca.IRON, 2))
    top = sc.piece("skull_top", 24, 6, ca.soft(ca.BONE, 3))
    band_short = sc.piece("band_short", 40, 4, ca.riveted(ca.IRON, 3, 6))
    shoulder = sc.piece("shoulder", 36, 4, ca.bevel(ca.IRON, 3))
    neck_band_top = sc.piece("neck_band_top", 48, 1, ca.bevel(ca.IRON, 4, edge=0))
    jaw = sc.piece("jaw", 12, 4, cute_jaw())
    claws = sc.piece("claw", 4, 6, ca.bevel(ca.IRON, 4))
    # The belly: a middle box the pot's depth and a box each side of it, so nothing overlaps; each draws only its
    # outward sides and its underside.
    els = [cube((3.4, 3.6, 1.4), (12.6, 9.8, 14.6), {"north": outer, "south": outer, "east": outer, "west": outer, "down": under}),
           cube((1.4, 3.6, 3.4), (3.4, 9.8, 12.6), {"north": outer_end, "south": outer_end, "west": outer, "down": under_side}),
           cube((12.6, 3.6, 3.4), (14.6, 9.8, 12.6), {"north": outer_end, "south": outer_end, "east": outer, "down": under_side}),
           cube((2.6, 2.4, 2.6), (13.4, 3.6, 13.4), {**{s: base for s in SIDES4}, "up": under, "down": floor}),
           # The shoulder: the belly's top where it shows outside the neck, as flat plates that never cover the inside.
           cube((3.4, 9.8, 1.4), (12.6, 9.8, 2.2), {"up": shoulder}), cube((3.4, 9.8, 13.8), (12.6, 9.8, 14.6), {"up": shoulder}),
           cube((1.4, 9.8, 3.4), (2.2, 9.8, 12.6), {"up": shoulder_side}), cube((13.8, 9.8, 3.4), (14.6, 9.8, 12.6), {"up": shoulder_side}),
           # The neck: walls lined from the inside floor up, closed underneath where they overhang the belly's corners
           # and on top inside the rim; and the inside floor.
           *fa.box_ring(2.2, 2.2, 13.8, 13.8, 1.1, 8.5, 12.6, neck, inner, top=(neck_under, neck_under_side), bottom=(neck_under, neck_under_side),
                        ends=neck_end),
           cube((3.3, 8.0, 3.3), (12.7, 8.5, 12.7), {"up": floor}),
           # A rolled lip round the top, a pixel wide, standing on the walls' outer edge.
           *fa.box_ring(1.5, 1.5, 14.5, 14.5, 1.0, 12.6, 13.6, rim, rim, top=(rim_top, rim_top_side), bottom=(rim_top, rim_top_side),
                        ends=rim_end),
           # Two riveted bands round the belly, and a band round the neck capped on top.
           cube((3.3, 5.0, 1.3), (12.7, 6.0, 14.7), {"north": band_short, "south": band_short, "east": band, "west": band, "up": band_flat,
                                                      "down": band_flat}),
           cube((1.3, 5.0, 3.3), (3.3, 6.0, 12.7), {"north": band_end, "south": band_end, "west": band_short, "up": band_flat, "down": band_flat}),
           cube((12.7, 5.0, 3.3), (14.7, 6.0, 12.7), {"north": band_end, "south": band_end, "east": band_short, "up": band_flat,
                                                       "down": band_flat}),
           *fa.box_ring(2.1, 2.1, 13.9, 13.9, 0.2, 9.8, 10.6, neck_band, top=(neck_band_top, neck_band_top_side), ends=tiny_end)]
    # Four feet, each ending in three iron claws.
    for x, z in ((2.2, 2.2), (11.8, 2.2), (2.2, 11.8), (11.8, 11.8)):
        els.append(cube((x, 0.6, z), (x + 2.0, 3.4, z + 2.0), {**{s: foot for s in SIDES4}, "up": foot_end, "down": foot_end}))
        cx, cz = x + 1.0, z + 1.0
        ox = -1 if cx < 8 else 1
        oz = -1 if cz < 8 else 1
        claw = {s: claws for s in ALL6}
        els.append(cube((cx - 0.4 + ox * 1.0, 0.0, cz - 0.4), (cx + 0.4 + ox * 1.0, 1.2, cz + 0.4), claw))
        els.append(cube((cx - 0.4, 0.0, cz - 0.4 + oz * 1.0), (cx + 0.4, 1.2, cz + 0.4 + oz * 1.0), claw))
        els.append(cube((cx - 0.4 + ox * 0.7, 0.0, cz - 0.4 + oz * 0.7), (cx + 0.4 + ox * 0.7, 1.0, cz + 0.4 + oz * 0.7), claw))
    # The ram's skull on the front: cranium, brow, the long face and its jaw, and a bolt at each temple.
    x0, y0, x1, y1 = SKULL_FACE
    els += [cube((x0, y0, d17.CAULDRON["skull_face_z"]), (x1, y1, 2.0), {"north": face, "east": side, "west": side, "up": top, "down": top}),
            cube((5.0, 10.0, 0.45), (11.0, 10.9, 0.9), {"north": top, "up": top, "east": side, "west": side, "down": top}),
            cube((6.2, 5.0, 0.3), (9.8, 7.6, 1.5), {"north": nose, "east": nose_side, "west": nose_side, "up": top, "down": top}),
            cube((6.6, 4.3, 0.5), (9.4, 5.0, 1.4), {"north": jaw, "east": side, "west": side, "up": top, "down": top}),
            cube((4.6, 9.0, 1.3), (5.2, 9.6, 1.9), {s: bolt for s in ALL6}),
            cube((10.8, 9.0, 1.3), (11.4, 9.6, 1.9), {s: bolt for s in ALL6})]
    # The horns, each a chain of tapering segments: up and out from the crown, back down past the cheek, curling forward.
    for side_, sign in (("left", -1), ("right", 1)):
        def mx(a, b):
            lo, hi = (a, b) if sign < 0 else (16 - b, 16 - a)
            return lo, hi
        hx0, hx1 = mx(3.2, 6.2)
        els.append(cube((hx0, 10.6, 0.4), (hx1, 12.6, 2.4), {s: horns for s in ALL6}, rotation(((hx0 + hx1) / 2, 11.6, 1.4), "z", -22.5 * sign)))
        hx0, hx1 = mx(1.2, 3.6)
        els.append(cube((hx0, 8.6, 0.7), (hx1, 11.2, 2.5), {s: horns for s in ALL6}, rotation(((hx0 + hx1) / 2, 9.9, 1.6), "z", 22.5 * sign)))
        hx0, hx1 = mx(0.4, 2.4)
        els.append(cube((hx0, 6.2, 0.6), (hx1, 8.9, 2.2), {s: horns for s in ALL6}, rotation(((hx0 + hx1) / 2, 8.9, 1.4), "x", 22.5)))
        hx0, hx1 = mx(0.8, 2.2)
        els.append(cube((hx0, 5.0, -0.6), (hx1, 6.6, 0.8), {s: horns for s in ALL6}, rotation(((hx0 + hx1) / 2, 6.6, 0.8), "x", 45)))
    sc.models[d17.CAULDRON["block"]] = els
    return sc


# The ram skull's face and its square eye sockets, which the client's glow covers exactly (tools/decor17.py CAULDRON); the
# face's texture is 5 texels to a pixel, so the sockets' edges fall on whole texels.
SKULL_FACE = d17.CAULDRON["skull_face"]
SKULL_EYES = d17.CAULDRON["skull_eyes"]


def ember_bed():
    """A hearth of clean-cut stone blocks a block high round a bed of glowing coals, with an arched opening in each side
    showing a smooth fire, and a few lumps of coal on top. The cauldron stands on its stones."""
    sc = Sculpt(d17.EMBER_BED["block"], 172)
    wall = sc.piece("wall", 32, 32, ca.hearth_wall(2))
    wall_in = sc.piece("wall_in", 20, 32, ca.blocks(ca.STONE, 3, course=5, length=8, base=1))
    top = sc.piece("top", 32, 6, ca.blocks(ca.STONE, 4, course=6, length=8, base=3))
    glow = sc.piece("embers", 22, 22, ca.coals(5))
    lump = sc.piece("lump", 4, 4, ca.bevel(ca.COAL, 1))
    els = [cube((0, 0, 0), (16, 16, 3), {"north": wall, "south": wall_in, "up": top, "east": top, "west": top, "down": top}),
           cube((0, 0, 13), (16, 16, 16), {"south": wall, "north": wall_in, "up": top, "east": top, "west": top, "down": top}),
           cube((0, 0, 3), (3, 16, 13), {"west": wall, "east": wall_in, "up": top, "down": top}),
           cube((13, 0, 3), (16, 16, 13), {"east": wall, "west": wall_in, "up": top, "down": top}),
           cube((3, 0, 3), (13, 12.5, 13), {"up": glow}, light=15)]
    for x, z, h in ((4.0, 5.0, 1.4), (8.5, 4.0, 1.0), (10.0, 9.0, 1.6), (5.5, 9.5, 1.2), (7.5, 7.0, 0.8)):
        els.append(cube((x, 12.5, z), (x + 2.2, 12.5 + h, z + 2.0), {**{s: lump for s in SIDES4}, "up": lump}))
    sc.models[d17.EMBER_BED["block"]] = els
    return sc


def brew_ladle():
    """The Brew Ladle's item picture: a long turned handle and a deep wooden bowl."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    p = Px(img)
    for i in range(22):
        x, y = 6 + i * 0.85, 26 - i * 0.85
        for d in (-1, 0, 1):
            p.put(x + d * 0.5, y + d * 0.5, TWIG[4] if d < 0 else (TWIG[3] if d == 0 else TWIG[1]))
    cx, cy = 7.0, 25.0
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, (y - cy) * 1.1)
            if d < 5.5:
                p.put(x, y, TWIG[2] if d > 4.2 else (TWIG[4] if x < cx - 1 else TWIG[3]))
    for y in range(32):
        for x in range(32):
            if math.hypot(x - cx + 0.8, y - cy + 0.8) < 2.6:
                p.put(x, y, TWIG[1])
    return img


# ---------------------------------------------------------------- 2. the candelabra

def candelabra():
    """The four iron fittings on one texture: the frames only (the candles and their flames are the client's, at
    tools/decor17.py CANDELABRA)."""
    sc = Sculpt("candelabra", 173, 128)
    iron = sc.piece("iron", 16, 16, wrought_clean())
    iron_h = sc.piece("iron_h", 32, 4, wrought_clean(horizontal=True))
    twist = sc.piece("twist", 8, 40, twisted(4))
    c_scroll = sc.piece("c_scroll", 16, 24, scroll("leg", 5))
    s_scroll = sc.piece("s_scroll", 16, 28, scroll("bracket", 6))
    crest = sc.piece("crest", 24, 20, scroll("crest", 7))
    pan = sc.piece("pan", 8, 8, drip_pan())
    mirror = sc.piece("mirror", 12, 18, mirror_glass())
    chain = sc.piece("chain", 4, 40, chain_links())
    knop = sc.piece("knop", 6, 6, wrought_clean())
    hook = sc.piece("hook", 10, 10, hook_ring())
    plate = sc.piece("plate", 18, 26, back_plate())
    sc.piece("item_wax", 4, 10, solid(pal("c8bc9c", "ddd2b4", "efe6cc"), 30))

    def bar_x(x0, x1, y, z, t=0.8):
        return cube((x0, y - t / 2, z - t / 2), (x1, y + t / 2, z + t / 2), {"north": iron_h, "south": iron_h, "up": iron_h, "down": iron_h,
                                                                            "east": iron, "west": iron})

    def bar_z(z0, z1, y, x, t=0.8):
        return cube((x - t / 2, y - t / 2, z0), (x + t / 2, y + t / 2, z1), {"east": iron_h, "west": iron_h, "up": iron_h, "down": iron_h,
                                                                            "north": iron, "south": iron})

    def cup(x, y, z, w=2.6):
        """A drip pan whose top is at `y` with the candle's socket in it."""
        r = w / 2
        return [cube((x - r, y - 0.5, z - r), (x + r, y, z + r), {**{s: iron for s in SIDES4}, "up": pan, "down": pan}),
                cube((x - 0.55, y - 1.4, z - 0.55), (x + 0.55, y - 0.5, z + 0.55), {**{s: iron for s in SIDES4}, "down": iron})]

    floor = []
    # Four scrolled feet round a base ring, a knop, the twisted stem with two more knops.
    for x0, x1, z, flip in ((0.5, 7.5, 8.0, False), (8.5, 15.5, 8.0, True)):
        uv = c_scroll if not flip else mirror_uv(c_scroll)
        floor.append(plane_xy(x0, x1, 0.0, 6.0, z, uv))
    for z0, z1, flip in ((0.5, 7.5, False), (8.5, 15.5, True)):
        uv = c_scroll if not flip else mirror_uv(c_scroll)
        floor.append(plane_zy(z0, z1, 0.0, 6.0, 8.0, uv))
    floor += [cube((5.5, 3.0, 5.5), (10.5, 4.2, 10.5), {**{s: iron for s in SIDES4}, "up": iron, "down": iron}),
              cube((6.6, 4.2, 6.6), (9.4, 6.0, 9.4), {**{s: knop for s in SIDES4}, "up": knop}),
              column(8.0, 8.0, 6.0, 26.0, 1.4, twist),
              cube((6.9, 11.0, 6.9), (9.1, 12.4, 9.1), {**{s: knop for s in SIDES4}, "up": knop, "down": knop}),
              cube((6.9, 16.6, 6.9), (9.1, 17.8, 9.1), {**{s: knop for s in SIDES4}, "up": knop, "down": knop}),
              bar_x(1.5, 14.5, 17.2, 8.0), bar_z(1.5, 14.5, 17.2, 8.0),
              bar_x(4.5, 11.5, 21.3, 8.0)]
    # Scroll brackets under the lower arms, and up the upper arm's ends.
    floor += [plane_xy(2.0, 7.4, 11.0, 16.8, 8.0, mirror_uv(s_scroll)), plane_xy(8.6, 14.0, 11.0, 16.8, 8.0, s_scroll),
              plane_zy(2.0, 7.4, 11.0, 16.8, 8.0, mirror_uv(s_scroll)), plane_zy(8.6, 14.0, 11.0, 16.8, 8.0, s_scroll),
              plane_xy(4.0, 7.6, 21.6, 25.6, 8.0, mirror_uv(s_scroll)), plane_xy(8.4, 12.0, 21.6, 25.6, 8.0, s_scroll)]
    for x, y, z, _ in d17.CANDELABRA["floor_candelabrum"]["candles"]:
        if (x, z) != (8.0, 8.0):
            floor.append(column(x, z, y - 1.3, y - 0.4, 0.8, iron))
        floor += cup(x, y, z)
    sc.models["floor_candelabrum"] = floor

    table = [cube((5.0, 0.0, 5.0), (11.0, 1.2, 11.0), {**{s: iron for s in SIDES4}, "up": iron, "down": iron}),
             cube((6.0, 1.2, 6.0), (10.0, 2.4, 10.0), {**{s: knop for s in SIDES4}, "up": knop}),
             column(8.0, 8.0, 2.4, 10.6, 1.2, twist),
             cube((7.0, 5.2, 7.0), (9.0, 6.4, 9.0), {**{s: knop for s in SIDES4}, "up": knop, "down": knop}),
             bar_x(3.0, 13.0, 7.4, 8.0),
             plane_xy(3.4, 7.4, 3.0, 7.0, 8.0, mirror_uv(s_scroll)), plane_xy(8.6, 12.6, 3.0, 7.0, 8.0, s_scroll)]
    for x, y, z, _ in d17.CANDELABRA["table_candelabrum"]["candles"]:
        if x != 8.0:
            table.append(column(x, z, 7.4, y - 0.4, 0.8, iron))
        table += cup(x, y, z)
    sc.models["table_candelabrum"] = table

    wall = [cube((4.5, 1.5, 15.2), (11.5, 14.5, 16.0), {"north": plate, "south": iron, "east": iron, "west": iron, "up": iron, "down": iron}),
            cube((5.8, 3.8, 15.0), (10.2, 11.2, 15.2), {"north": mirror}),
            plane_xy(4.0, 12.0, 13.0, 17.0, 15.6, crest),
            bar_z(10.0, 15.2, 6.6, 8.0), bar_x(3.0, 13.0, 6.6, 10.0),
            column(8.0, 9.0, 6.6, 9.6, 0.8, iron),
            bar_z(9.0, 10.0, 6.6, 8.0),
            plane_zy(10.0, 15.0, 2.0, 6.4, 8.0, s_scroll)]
    for x, y, z, _ in d17.CANDELABRA["wall_girandole"]["candles"]:
        wall += cup(x, y, z)
    sc.models["wall_girandole"] = wall

    hanging = [cube((7.2, 14.0, 7.2), (8.8, 15.0, 8.8), {**{s: knop for s in SIDES4}, "up": knop, "down": knop}),
               plane_xy(6.5, 9.5, 14.8, 16.0, 8.0, hook),
               column(8.0, 8.0, -7.0, 14.0, 1.2, twist),
               cube((6.9, -8.6, 6.9), (9.1, -7.0, 9.1), {**{s: knop for s in SIDES4}, "down": knop}),
               cube((6.9, -2.2, 6.9), (9.1, -0.4, 9.1), {**{s: knop for s in SIDES4}, "up": knop, "down": knop}),
               bar_x(-10.0, 26.0, -6.0, 8.0), bar_z(-10.0, 26.0, -6.0, 8.0),
               dict(bar_x(-4.7, 20.7, -6.0, 8.0), rotation=rotation((8.0, -6.0, 8.0), "y", 45)),
               dict(bar_x(-4.7, 20.7, -6.0, 8.0), rotation=rotation((8.0, -6.0, 8.0), "y", -45)),
               dict(bar_x(0.2, 15.8, -1.0, 8.0), rotation=rotation((8.0, -1.0, 8.0), "y", 45)),
               dict(bar_x(0.2, 15.8, -1.0, 8.0), rotation=rotation((8.0, -1.0, 8.0), "y", -45)),
               bar_x(3.5, 12.5, 3.4, 8.0), bar_z(3.5, 12.5, 3.4, 8.0)]
    # Chains from the crown down to the four long arms' tips.
    for sign in (-1, 1):
        hanging.append(plane_xy(8.0 - 0.6 + sign * 9.0, 8.0 + 0.6 + sign * 9.0, -9.5, 17.0, 8.0, chain,
                                rotation((8.0 + sign * 9.0, 3.75, 8.0), "z", 45 * sign)))
        hanging.append(plane_zy(8.0 - 0.6 + sign * 9.0, 8.0 + 0.6 + sign * 9.0, -9.5, 17.0, 8.0, chain,
                                rotation((8.0, 3.75, 8.0 + sign * 9.0), "x", -45 * sign)))
    for x, y, z, _ in d17.CANDELABRA["branching_chandelier"]["candles"]:
        hanging += cup(x, y, z, 2.4)
    for x, z in ((-10.0, 8.0), (26.0, 8.0), (8.0, -10.0), (8.0, 26.0)):
        # A drop under each long arm's tip (from its low corner up: a box written top-down draws inside out).
        hanging.append(cube((x - 0.5, -7.6, z - 0.5), (x + 0.5, -6.4, z + 0.5), {**{s: knop for s in SIDES4}, "down": knop}))
    sc.models["branching_chandelier"] = hanging
    return sc


def mirror_glass():
    """Black looking-glass in an oval, grey reflections streaking it."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if d <= 1.0:
                    streak = abs((x - y * 0.4) - p.w * 0.1) < 1
                    p.put(x, y, (90, 96, 110) if streak else ((14, 14, 20) if d < 0.85 else IRON[5]))
    return paint


def chain_links():
    def paint(p):
        for y in range(p.h):
            link = y % 4
            for x in range(p.w):
                if link in (0, 3) and 0 < x < p.w - 1 or link in (1, 2) and x in (0, p.w - 1):
                    p.put(x, y, IRON[5] if x < p.w / 2 else IRON[3])
    return paint


def hook_ring():
    def paint(p):
        cx, cy = (p.w - 1) / 2, p.h * 0.45
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot(x - cx, y - cy)
                if p.w * 0.25 < d < p.w * 0.45:
                    p.put(x, y, IRON[5] if y < cy else IRON[3])
    return paint


def back_plate():
    """The girandole's cast back-plate, clean: flat dark iron bevelled at its edges, a raised bead round the mirror's
    place lit along its upper half and shaded along its lower."""
    def paint(p):
        wrought_clean()(p)
        for i in range(80):
            a = i / 80 * math.tau
            x = p.w / 2 + math.cos(a) * p.w * 0.42
            y = p.h / 2 + math.sin(a) * p.h * 0.42
            p.put(x, y, IRON[6] if math.sin(a) < 0 else IRON[4])
    return paint


# ---------------------------------------------------------------- 3. the enchanted broom, the dustpan and the rack

def enchanted_broom():
    """A besom of bound birch twigs on a crooked handle, an amethyst bound in with a purple cord: standing on its bristles
    (the block's own model shows only particles; the client draws this, swaying and sweeping, about (8, 0, 8))."""
    sc = Sculpt(d17.BROOM["block"], 174, 128)
    bristle = sc.piece("bristles", 24, 20, twigs(2))
    bristle_lit = sc.piece("bristles_lit", 24, 20, twigs(3, glow=True))
    tops = sc.piece("tops", 10, 10, noise(TWIG, 4, 2))
    handle = sc.piece("handle", 4, 46, gnarled(5))
    binding = sc.piece("binding", 14, 4, cord(6))
    ribbon = sc.piece("ribbon", 6, 4, noise(AMETHYST, 7, 1))
    gem = sc.piece("gem", 4, 4, strip(AMETHYST, 8, light=True))
    base = [cube((5.0, 0.0, 5.0), (11.0, 3.0, 11.0), {**{s: bristle for s in SIDES4}, "down": tops}),
            cube((5.6, 3.0, 5.6), (10.4, 6.0, 10.4), {**{s: bristle for s in SIDES4}}),
            cube((6.3, 6.0, 6.3), (9.7, 8.5, 9.7), {**{s: bristle for s in SIDES4}, "up": tops}),
            cube((6.1, 6.6, 6.1), (9.9, 7.6, 9.9), {s: binding for s in SIDES4}),
            cube((6.4, 8.0, 6.4), (9.6, 8.8, 9.6), {s: binding for s in SIDES4}),
            column(8.0, 8.0, 3.0, 26.0, 1.2, handle),
            cube((7.2, 9.0, 6.6), (8.8, 10.4, 7.2), {s: gem for s in ALL6}),
            plane_xy(6.6, 9.4, 7.4, 9.0, 6.5, ribbon)]
    # A few stray twigs splaying from the bundle.
    for x, z, axis, angle in ((5.4, 8.0, "z", 22.5), (10.6, 8.0, "z", -22.5), (8.0, 5.4, "x", -22.5), (8.0, 10.6, "x", 22.5)):
        base.append(column(x, z, 0.0, 4.0, 0.4, handle, rot=rotation((x, 4.0, z), axis, angle)))
    sc.models["enchanted_broom"] = base
    sc.models["enchanted_broom_glow"] = [cube((4.9, -0.01, 4.9), (11.1, 2.0, 11.1), {s: bristle_lit for s in SIDES4})]
    return sc


@fa.quietly
def dustpan():
    """A tin dustpan lying open to its front (north), its back and sides turned up, a short handle at the back."""
    sc = Sculpt(d17.DUSTPAN["block"], 175)
    pan = sc.piece("pan", 26, 22, tin(2))
    edge = sc.piece("edge", 26, 6, tin(3, 1))
    handle = sc.piece("handle", 4, 14, tin(4, 0))
    grip = sc.piece("grip", 6, 10, wood(WOOD, 5))
    els = [cube((2.5, 0.0, 3.0), (13.5, 0.5, 13.0), {"up": pan, "down": pan, "north": edge, "south": edge, "east": edge, "west": edge}),
           cube((2.5, 0.5, 12.2), (13.5, 3.8, 13.0), {s: edge for s in ALL6}),
           cube((2.5, 0.5, 4.0), (3.2, 2.2, 12.2), {s: edge for s in ALL6}),
           cube((12.8, 0.5, 4.0), (13.5, 2.2, 12.2), {s: edge for s in ALL6}),
           cube((7.3, 1.6, 13.0), (8.7, 2.6, 15.5), {s: handle for s in ALL6}),
           cube((7.1, 1.4, 15.5), (8.9, 2.8, 16.5), {s: grip for s in ALL6})]
    sc.models[d17.DUSTPAN["block"]] = els
    return sc


# Where the dustpan's heap of sweepings lies, for the client: {x, z} pixels for up to three stacks.
PAN_HEAP = [(6.0, 7.0), (10.0, 8.0), (8.0, 10.5)]


@fa.quietly
def broom_rack():
    """A dark oak rail for a wall (facing north, on the wall to its south) with three turned pegs; the brooms hung on
    them are the client's."""
    sc = Sculpt(d17.BROOM_RACK["block"], 176)
    rail = sc.piece("rail", 32, 8, wood(WOOD, 2))
    end = sc.piece("end", 8, 8, wood(WOOD, 3))
    peg = sc.piece("peg", 4, 10, strip(BRASS, 4, light=True))
    cap = sc.piece("cap", 4, 4, solid(BRASS[2:], 5))
    els = [cube((0.5, 9.0, 14.0), (15.5, 13.0, 16.0), {"north": rail, "up": rail, "down": rail, "east": end, "west": end}),
           cube((0.5, 13.0, 14.6), (15.5, 13.6, 16.0), {"north": rail, "up": rail, "east": end, "west": end})]
    for x in RACK_PEGS:
        els += [cube((x - 0.6, 10.4, 10.2), (x + 0.6, 11.6, 14.0), {s: peg for s in ALL6}),
                cube((x - 0.9, 10.1, 9.6), (x + 0.9, 11.9, 10.2), {s: cap for s in ALL6})]
    sc.models[d17.BROOM_RACK["block"]] = els
    return sc


RACK_PEGS = [3.5, 8.0, 12.5]


# ---------------------------------------------------------------- 4. the cabinet of curiosities

@fa.quietly
def curiosity_cabinet():
    """A carved mahogany cabinet two blocks tall (the lower block's model holds it all; the upper is empty), facing
    north: bun feet, a plinth, panelled sides and back, three shelves lined in purple velvet, and a crown with a carved
    moth-and-skull crest. Its glazed doors are the client's (hinged at x 1.5 and 14.5, z 3.0)."""
    sc = Sculpt(d17.CABINET["block"], 177, 128)
    # The tall pieces first, side by side in one row, so the texture packs.
    side = sc.piece("side", 24, 60, wood(WOOD, 2, panel=True))
    back = sc.piece("back", 28, 60, velvet(3))
    frame = sc.piece("frame", 4, 60, wood(WOOD, 6))
    door_frame = sc.piece("door_frame", 4, 56, wood(WOOD, 12))
    pane = sc.piece("pane", 20, 48, glass(60))
    shelf = sc.piece("shelf", 26, 22, wood(WOOD, 4))
    shelf_edge = sc.piece("shelf_edge", 26, 3, wood(WOOD, 5))
    rail = sc.piece("rail", 28, 4, wood(WOOD, 7))
    crown = sc.piece("crown", 32, 8, wood(WOOD, 8))
    crest = sc.piece("crest", 18, 12, carved_crest(9))
    foot = sc.piece("foot", 6, 6, wood(WOOD, 10))
    top = sc.piece("top", 32, 28, wood(WOOD, 11))
    els = [cube((1.0, 1.5, 3.0), (2.0, 30.0, 15.0), {"west": side, "east": side, "up": frame, "down": frame, "south": frame}),
           cube((14.0, 1.5, 3.0), (15.0, 30.0, 15.0), {"east": side, "west": side, "up": frame, "down": frame, "south": frame}),
           cube((2.0, 1.5, 14.0), (14.0, 30.0, 15.0), {"north": back, "south": side}),
           cube((0.6, 1.5, 2.6), (15.4, 2.5, 15.4), {**{s: rail for s in SIDES4}, "up": shelf, "down": shelf}),
           cube((2.0, 11.4, 3.2), (14.0, 12.4, 14.0), {"up": shelf, "down": shelf, "north": shelf_edge}),
           cube((2.0, 21.4, 3.2), (14.0, 22.4, 14.0), {"up": shelf, "down": shelf, "north": shelf_edge}),
           cube((2.0, 29.0, 3.0), (14.0, 30.0, 14.0), {"down": shelf}),
           cube((0.4, 30.0, 2.4), (15.6, 31.6, 15.6), {**{s: crown for s in SIDES4}, "up": top, "down": top}),
           cube((5.0, 29.6, 2.1), (11.0, 31.9, 2.4), {"north": crest, "south": rail, "up": rail, "down": rail,
                                                       "east": rail, "west": rail}),
           cube((1.0, 2.5, 2.6), (2.0, 30.0, 3.0), {"north": frame, "east": frame, "west": frame}),
           cube((14.0, 2.5, 2.6), (15.0, 30.0, 3.0), {"north": frame, "west": frame, "east": frame})]
    for x, z in ((1.0, 2.8), (13.0, 2.8), (1.0, 13.2), (13.0, 13.2)):
        els.append(cube((x, 0.0, z), (x + 2.0, 1.5, z + 2.0), {**{s: foot for s in SIDES4}, "down": foot}))
    sc.models[d17.CABINET["block"]] = els
    # The doors, each hinged at its outer edge: a frame of four rails round a glazed pane split by a glazing bar.
    door_rail = sc.piece("door_rail", 26, 4, wood(WOOD, 13))
    knob = sc.piece("knob", 3, 3, solid(BRASS[2:], 14))
    for name, x0, x1 in (("door_left", 1.5, 8.0), ("door_right", 8.0, 14.5)):
        door = [cube((x0, 2.5, 2.6), (x0 + 0.8, 29.0, 3.0), {s: door_frame for s in ALL6}),
                cube((x1 - 0.8, 2.5, 2.6), (x1, 29.0, 3.0), {s: door_frame for s in ALL6}),
                cube((x0, 2.5, 2.6), (x1, 3.3, 3.0), {s: door_rail for s in ALL6}),
                cube((x0, 28.2, 2.6), (x1, 29.0, 3.0), {s: door_rail for s in ALL6}),
                cube((x0, 15.4, 2.6), (x1, 16.1, 3.0), {s: door_rail for s in ALL6})]
        kx = x1 - 1.6 if name == "door_left" else x0 + 0.6
        door.append(cube((kx, 15.0, 2.0), (kx + 1.0, 16.4, 2.6), {s: knob for s in ALL6}))
        sc.models[f"curiosity_cabinet_{name}"] = door
        sc.models[f"curiosity_cabinet_{name}_pane"] = [cube((x0 + 0.8, 3.3, 2.75), (x1 - 0.8, 28.2, 2.85), {"north": pane, "south": pane})]
    return sc


# Where each of the cabinet's nine places is: {x, y, z} pixels (the item's bottom middle), row by row from the bottom.
CABINET_PLACES = [(x, y, 8.5) for y in (2.5, 12.4, 22.4) for x in (4.5, 8.0, 11.5)]


def bell_jar():
    """A glass dome with a brass knob on a turned walnut plinth; what it shows is the client's (on the plinth at y 3)."""
    sc = Sculpt(d17.BELL_JAR["block"], 178)
    plinth = sc.piece("plinth", 24, 6, wood(WOOD, 2))
    plinth_top = sc.piece("plinth_top", 22, 22, wood(WOOD, 3))
    glass_side = sc.piece("glass_side", 14, 20, glass(55))
    glass_top = sc.piece("glass_top", 12, 12, glass(55, streak=False))
    knob = sc.piece("knob", 4, 4, strip(BRASS, 4, light=True))
    els = [cube((2.5, 0.0, 2.5), (13.5, 1.5, 13.5), {**{s: plinth for s in SIDES4}, "up": plinth_top, "down": plinth_top}),
           cube((3.5, 1.5, 3.5), (12.5, 2.6, 12.5), {**{s: plinth for s in SIDES4}, "up": plinth_top}),
           cube((4.2, 2.6, 4.2), (11.8, 11.8, 11.8), {s: glass_side for s in SIDES4}),
           # The shoulder where the dome narrows: flat glass over the lower part's top round the upper's foot.
           *[cube(lo, hi, {"up": glass_top, "down": glass_top}) for lo, hi in (((4.2, 11.8, 4.2), (11.8, 11.8, 5.0)),
                                                                              ((4.2, 11.8, 11.0), (11.8, 11.8, 11.8)),
                                                                              ((4.2, 11.8, 5.0), (5.0, 11.8, 11.0)),
                                                                              ((11.0, 11.8, 5.0), (11.8, 11.8, 11.0)))],
           cube((5.0, 11.8, 5.0), (11.0, 13.2, 11.0), {**{s: glass_side for s in SIDES4}, "up": glass_top}),
           cube((7.2, 13.2, 7.2), (8.8, 14.6, 8.8), {s: knob for s in ALL6})]
    sc.models[d17.BELL_JAR["block"]] = els
    return sc


def moth_case():
    """A shallow glazed case for a wall (facing north, on the wall to its south): a black frame round a cream backing with
    a hand-written label. The moths are the client's: pinned at MOTHS, their wings opening and closing at night."""
    sc = Sculpt(d17.MOTH_CASE["block"], 179, 128)
    frame = sc.piece("frame", 4, 28, wrought_clean())
    frame_h = sc.piece("frame_h", 30, 4, wrought_clean(horizontal=True))
    backing = sc.piece("backing", 26, 22, ca.bevel(BACKING, 1, light=1, dark=0))
    tag = sc.piece("tag", 10, 3, label(5, 1))
    pane = sc.piece("pane", 26, 22, glass(40))
    els = [cube((1.5, 2.5, 15.4), (14.5, 13.5, 16.0), {"north": backing}),
           cube((1.0, 2.0, 14.0), (2.0, 14.0, 16.0), {s: frame for s in ALL6}),
           cube((14.0, 2.0, 14.0), (15.0, 14.0, 16.0), {s: frame for s in ALL6}),
           cube((2.0, 2.0, 14.0), (14.0, 3.0, 16.0), {s: frame_h for s in ALL6}),
           cube((2.0, 13.0, 14.0), (14.0, 14.0, 16.0), {s: frame_h for s in ALL6}),
           cube((5.5, 3.3, 15.35), (10.5, 4.3, 15.4), {"north": tag}),
           cube((2.0, 3.0, 14.2), (14.0, 13.0, 14.25), {"north": pane, "south": pane})]
    sc.models[d17.MOTH_CASE["block"]] = els
    # Each moth: a body, and one model per side of wings hinged at the body's middle line (x 0 at the body).
    for kind in d17.MOTH_CASE["moths"]:
        wings = sc.piece(f"{kind}_wings", 20, 22, moth_wings(kind))
        body = sc.piece(f"{kind}_body", 3, 8, thorax(kind))
        pin = sc.piece("pin", 2, 2, solid(BRASS[3:], 6))
        sc.models[f"moth_{kind}_body"] = [cube((-0.45, -2.2, -0.6), (0.45, 2.0, 0.0), {s: body for s in ALL6}),
                                          cube((-0.2, 1.6, -0.9), (0.2, 2.0, -0.6), {s: pin for s in ALL6})]
        sc.models[f"moth_{kind}_wing_left"] = [plane_xy(-5.5, 0.0, -3.4, 2.4, -0.2, wings)]
        sc.models[f"moth_{kind}_wing_right"] = [plane_xy(0.0, 5.5, -3.4, 2.4, -0.2, mirror_uv(wings))]
    return sc


# Where each pinned moth sits on the backing (x, y in pixels, z just in front of it) and how big it is.
MOTHS = [(8.0, 8.6, 15.3, 1.0), (4.2, 5.4, 15.3, 0.55), (11.8, 5.4, 15.3, 0.55)]


# ---------------------------------------------------------------- 5. the oddity jars

# The oddity jars' shape (tools/decor17.py ODDITY_JAR).
JAR = d17.ODDITY_JAR


def lid_top_clean():
    """A jar lid from above, clean: flat dark iron with a lit top-left edge and a pressed ring round its middle."""
    def paint(p):
        ca.bevel(IRON, 3)(p)
        i = max(2, p.w // 5)
        for x in range(i, p.w - i):
            p.put(x, i, IRON[1])
            p.put(x, p.h - 1 - i, IRON[5])
        for y in range(i, p.h - i):
            p.put(i, y, IRON[1])
            p.put(p.w - 1 - i, y, IRON[5])
    return paint


def jar(name, murk_alpha=140, dry=False, size=None, legacy=False):
    """A squat glass jar on a dark base under an iron lid with a knob, a paper label on its front, and (unless `dry`)
    murky fluid; what is in it is the client's (round its middle, (8, 6, 8)). `size` changes its shape (JAR).

    The lid, base and knob are clean bevelled iron, the murk lies in three even bands and the label is flat paper with
    even lines of ink and a plain paper back (5 October 2026). `legacy` keeps the first look exactly, hammered iron,
    speckled murk and all: the Beating Heart Jar keeps it, as the owner loves it."""
    j = dict(JAR, **(size or {}))
    sc = Sculpt(name, 180 + list(d17.JARS).index(name), 64)
    glass_side = sc.piece("glass", 14, 22, glass(50))
    if legacy:
        lid = sc.piece("lid", 18, 4, wrought(2, horizontal=True))
        lid_top = sc.piece("lid_top", 16, 16, wrought(3))
        base = sc.piece("base", 18, 3, wrought(4, horizontal=True))
        base_top = lid_top
    else:
        lid = sc.piece("lid", 18, 4, ca.bevel(IRON, 4))
        lid_top = sc.piece("lid_top", 16, 16, lid_top_clean())
        base = sc.piece("base", 18, 3, ca.bevel(IRON, 3))
        base_top = lid_top
    tag = sc.piece("label", 12, 8, label(5) if legacy else label_clean())
    knob = sc.piece("knob", 4, 4, wrought(6) if legacy else ca.bevel(IRON, 5))
    b, g, ld, k = j["base"], j["glass"], j["lid"], j["knob"]
    lx0, ly0, lx1, ly1 = j["label"]
    els = [cube((b, 0.0, b), (16 - b, 1.0, 16 - b), {**{s: base for s in SIDES4}, "down": base_top, "up": base_top}),
           cube((g, 1.0, g), (16 - g, j["glass_top"], 16 - g), {s: glass_side for s in SIDES4}),
           cube((ld, j["glass_top"], ld), (16 - ld, j["lid_top"], 16 - ld), {**{s: lid for s in SIDES4}, "up": lid_top, "down": lid_top}),
           cube((8 - k, j["lid_top"], 8 - k), (8 + k, j["knob_top"], 8 + k), {**{s: knob for s in SIDES4}, "up": knob})]
    if legacy:
        els.append(cube((lx0, ly0, g - 0.1), (lx1, ly1, g - 0.05), {"north": tag}))
    else:
        back = sc.piece("label_back", 6, 4, ca.bevel(PAPER, 2))
        els.append(cube((lx0, ly0, g - 0.15), (lx1, ly1, g - 0.05), {"north": tag, "south": back}))
    if not dry:
        murk = sc.piece("murk", 14, 18, fluid(MURK[name], 7, murk_alpha) if legacy else murk_clean(MURK[name], murk_alpha))
        murk_top = sc.piece("murk_top", 14, 14, fluid(MURK[name], 8, murk_alpha) if legacy else murk_clean(MURK[name], murk_alpha, top=True))
        els.append(cube((4.6, 1.0, 4.6), (11.4, 9.6, 11.4), {**{s: murk for s in SIDES4}, "up": murk_top}))
    sc.models[name] = els
    return sc


def jar_of_eyeballs():
    sc = jar("jar_of_eyeballs")
    side = sc.piece("eye_sclera", 8, 8, d16d.sclera(21, 3))
    iris = sc.piece("eye_iris", 8, 8, d16d.iris())
    # One eyeball centred on the origin, looking north; the client draws nine, each turned its own way.
    sc.models["oddity_eyeball"] = [cube((-1.1, -1.1, -1.1), (1.1, 1.1, 1.1), {s: side for s in ALL6}),
                                   cube((-0.7, -0.7, -1.15), (0.7, 0.7, -1.15), {"north": iris})]
    return sc


# Where the jar's nine eyeballs float: {x, y, z} pixels.
EYEBALLS = [(6.2, 2.4, 6.4), (9.6, 2.5, 6.6), (7.8, 2.3, 9.6), (6.6, 4.8, 8.6), (9.8, 4.6, 9.2), (8.0, 5.0, 6.2),
            (6.4, 7.2, 6.8), (9.4, 7.4, 7.6), (7.8, 7.6, 9.8)]


def beating_heart_jar():
    sc = jar("beating_heart_jar", 120, legacy=True)
    muscle = sc.piece("heart", 14, 14, heart(21))
    vessel = sc.piece("vessel", 4, 8, ca.bevel(HEART, 2, sides="lr"))
    stand = sc.piece("stand", 6, 6, strip(BRASS, 23, light=True))
    dial = sc.piece("dial", 6, 6, dial_face())
    # The heart centred on the origin (it swells about its middle as it beats), and its brass stand and dial.
    sc.models["oddity_heart"] = [cube((-2.4, -2.6, -2.0), (2.4, 1.8, 2.0), {s: muscle for s in ALL6}),
                                 cube((-2.0, -3.6, -1.6), (0.6, -2.6, 1.4), {s: muscle for s in ALL6}),
                                 cube((-1.4, 1.8, -0.6), (-0.2, 3.6, 0.6), {s: vessel for s in ALL6}),
                                 cube((0.4, 1.8, -0.4), (1.4, 3.2, 0.6), {s: vessel for s in ALL6}),
                                 cube((-0.2, 1.8, 0.8), (0.8, 2.8, 1.8), {s: vessel for s in ALL6})]
    sc.models["oddity_heart_stand"] = [cube((7.4, 1.0, 7.4), (8.6, 3.0, 8.6), {s: stand for s in ALL6}),
                                       cube((5.0, 1.0, 5.0), (7.0, 3.2, 5.2), {"north": dial, "east": stand, "west": stand, "up": stand})]
    return sc


def dial_face():
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot(x - cx, y - cy)
                p.put(x, y, BRASS[4] if d > p.w * 0.42 else PAPER[4])
        p.put(cx, cy, INK[0])
        p.put(cx + 1, cy - 1, (150, 20, 20))
    return paint


def bat_in_a_jar():
    sc = jar("bat_in_a_jar", dry=True, size=d17.JARS["bat_in_a_jar"]["jar"])
    body = sc.piece("bat_fur", 8, 8, fur(21))
    face = sc.piece("bat_face", 6, 5, bat_face())
    wing = sc.piece("bat_wing", 14, 12, bat_wing())
    ear = sc.piece("bat_ear", 2, 3, ca.bevel(FUR, 1))
    # The bat centred on the origin, head down as it hangs (the client turns it head up as it wakes and flies).
    sc.models["oddity_bat_body"] = [cube((-1.2, -1.6, -1.0), (1.2, 1.6, 1.0), {s: body for s in ALL6}),
                                    cube((-1.0, -3.2, -1.0), (1.0, -1.6, 0.8), {"north": face, "south": body, "east": body, "west": body,
                                                                                 "down": body}),
                                    cube((-1.0, -3.9, -0.3), (-0.4, -3.2, 0.3), {s: ear for s in ALL6}),
                                    cube((0.4, -3.9, -0.3), (1.0, -3.2, 0.3), {s: ear for s in ALL6}),
                                    # Its little feet, gripping the lid as it hangs.
                                    cube((-0.8, 1.6, -0.3), (-0.3, 2.3, 0.3), {**{s: ear for s in SIDES4}, "up": ear}),
                                    cube((0.3, 1.6, -0.3), (0.8, 2.3, 0.3), {**{s: ear for s in SIDES4}, "up": ear})]
    sc.models["oddity_bat_wing_left"] = [plane_xy(-4.6, 0.0, -2.0, 2.0, 0.0, wing)]
    sc.models["oddity_bat_wing_right"] = [plane_xy(0.0, 4.6, -2.0, 2.0, 0.0, mirror_uv(wing))]
    return sc


def bat_face():
    """A bat's cute face: soft black fur, two round red eyes each with a glint, and one little white fang."""
    def paint(p):
        ca.bevel(FUR, 2)(p)
        for x in (1, p.w - 3):
            p.put(x, 1, (220, 60, 60))
            p.put(x + 1, 1, (220, 60, 60))
            p.put(x, 2, (160, 30, 36))
            p.put(x + 1, 2, (160, 30, 36))
            p.put(x, 1, ca.GLINT)
        p.put(p.w // 2, p.h - 1, (240, 234, 222))
    return paint


def bat_wing():
    """A bat's wing in black membrane (decor16's painter, in other colours)."""
    def paint(p):
        old = d16d.WING, d16d.WING_BONE
        d16d.WING, d16d.WING_BONE = BAT_WING, FUR[:3]
        try:
            d16d.bat_wing(31)(p)
        finally:
            d16d.WING, d16d.WING_BONE = old
    return paint


def two_headed_snake_jar():
    sc = jar("two_headed_snake_jar", 110)
    coil = sc.piece("scales", 16, 16, scales(21))
    belly = sc.piece("scales_belly", 16, 8, scales(22, belly=True))
    head = sc.piece("snake_head", 6, 6, snake_head())
    tongue = sc.piece("tongue", 2, 3, ca.bevel(pal("8a1420", "be2a36", "e0505a"), 1))
    # The coil, round the jar's middle, in three stacked loops; each head on its neck centred on the origin, looking north.
    coils = []
    for i, (y, r) in enumerate(((1.4, 2.6), (2.6, 2.2), (3.7, 1.7))):
        coils.append(cube((8 - r, y, 8 - r), (8 + r, y + 1.2, 8 + r), {**{s: coil for s in SIDES4}, "up": coil, "down": belly}))
    sc.models["oddity_snake_coil"] = coils
    sc.models["oddity_snake_head"] = [cube((-0.5, -3.0, -0.5), (0.5, 0.0, 0.5), {s: coil for s in ALL6}),
                                      cube((-0.9, 0.0, -1.6), (0.9, 1.0, 0.4), {"north": head, "up": head, "east": coil, "west": coil,
                                                                               "south": coil, "down": belly})]
    sc.models["oddity_snake_tongue"] = [cube((-0.15, 0.2, -2.6), (0.15, 0.3, -1.6), {s: tongue for s in ALL6})]
    return sc


def snake_head():
    """A snake's head from the front: its scales, two big round yellow eyes with dark slit pupils."""
    def paint(p):
        scales(24)(p)
        for x in (0, p.w - 2):
            for dx in (0, 1):
                p.put(x + dx, 1, (240, 200, 50))
                p.put(x + dx, 2, (220, 170, 30))
            p.put(x + (1 if x == 0 else 0), 1, (30, 20, 10))
            p.put(x + (1 if x == 0 else 0), 2, (30, 20, 10))
    return paint


def hand_in_a_jar():
    sc = jar("hand_in_a_jar", 100)
    palm = sc.piece("palm", 12, 12, stitches(21))
    finger = sc.piece("finger", 4, 8, skin(22))
    tip = sc.piece("fingertip", 4, 4, skin(23, nail=True))
    # The palm centred on the origin, fingers up, back of the hand to the north; each finger hinged at its knuckle.
    sc.models["oddity_hand_palm"] = [cube((-2.0, -2.4, -0.7), (2.0, 1.6, 0.7), {s: palm for s in ALL6}),
                                     cube((-1.4, -4.6, -0.6), (1.4, -2.4, 0.6), {s: palm for s in ALL6}),
                                     cube((2.0, -1.8, -0.5), (3.0, 0.4, 0.5), {s: finger for s in ALL6},
                                          rotation((2.0, -1.8, 0.0), "z", -22.5)),
                                     cube((2.6, 0.2, -0.45), (3.3, 1.6, 0.45), {"up": tip, **{s: finger for s in SIDES4}},
                                          rotation((2.6, 0.2, 0.0), "z", -45))]
    sc.models["oddity_hand_finger"] = [cube((-0.45, 0.0, -0.45), (0.45, 2.2, 0.45), {**{s: finger for s in SIDES4}, "down": finger}),
                                       cube((-0.4, 2.2, -0.4), (0.4, 3.2, 0.4), {**{s: finger for s in SIDES4}, "up": tip})]
    return sc


# The hand's four fingers' knuckles, from the palm's middle: {x, y, z} pixels.
KNUCKLES = [(-1.5, 1.6, 0.0), (-0.5, 1.6, 0.0), (0.5, 1.6, 0.0), (1.5, 1.6, 0.0)]


# ---------------------------------------------------------------- 6. the bigger jars

FACE_AXIS = {"west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1)}


def _default_uv(side, lo, hi):
    fx, fy, fz = lo
    tx, ty, tz = hi
    return {"down": (fx, 16 - tz, tx, 16 - fz), "up": (fx, fz, tx, tz), "north": (16 - tx, 16 - ty, 16 - fx, 16 - fy),
            "south": (fx, 16 - ty, tx, 16 - fy), "west": (fz, 16 - ty, tz, 16 - fy), "east": (16 - tz, 16 - ty, 16 - fz, 16 - fy)}[side]


def crop_uv(side, frm, to, lo, hi, uv):
    """The part of a face's pinned `uv` that its box clipped to lo..hi keeps (the picture laid over the whole face as a
    block model lays it, so the cropped pieces join up)."""
    whole, part = _default_uv(side, frm, to), _default_uv(side, lo, hi)
    u0, v0, u1, v1 = uv

    def at(a, b, lo_, hi_, x):
        return a + (b - a) * ((x - lo_) / (hi_ - lo_) if hi_ != lo_ else 0.0)
    return (round(at(u0, u1, whole[0], whole[2], part[0]), 4), round(at(v0, v1, whole[1], whole[3], part[1]), 4),
            round(at(u0, u1, whole[0], whole[2], part[2]), 4), round(at(v0, v1, whole[1], whole[3], part[3]), 4))


def cell_part(elements, cell, size):
    """The part of a prop modelled whole in its own frame (pixels from 0 to 16 x `size` blocks) inside its block `cell`
    (blocks across, up and deep), moved into that block: boxes clipped to it, the faces on the cuts left out and each
    pinned uv cropped to the part of its face kept, so a picture runs on unbroken from block to block. What reaches out
    past the prop's outer sides stays with the outer blocks; a sheet on the boundary between two blocks goes to the upper
    one. Unrotated boxes only."""
    lo_cell = [cell[k] * 16 if cell[k] > 0 else -64.0 for k in range(3)]
    hi_cell = [cell[k] * 16 + 16 if cell[k] < size[k] - 1 else 16 * size[k] + 64.0 for k in range(3)]
    origin = [cell[k] * 16 for k in range(3)]
    out = []
    for e in elements:
        assert "rotation" not in e, "cell_part cuts unrotated boxes only"
        frm, to = e["from"], e["to"]
        lo = [max(frm[k], lo_cell[k]) for k in range(3)]
        hi = [min(to[k], hi_cell[k]) for k in range(3)]
        if any((to[k] - frm[k] < 1e-9 and not lo_cell[k] <= frm[k] < hi_cell[k]) or (to[k] - frm[k] >= 1e-9 and hi[k] - lo[k] <= 1e-6)
               for k in range(3)):
            continue
        faces = {}
        for side, spec in e["faces"].items():
            axis, sign = FACE_AXIS[side]
            if (sign < 0 and lo[axis] > frm[axis] + 1e-9) or (sign > 0 and hi[axis] < to[axis] - 1e-9):
                continue
            spec = dict(spec)
            if "uv" in spec:
                spec["uv"] = list(crop_uv(side, frm, to, lo, hi, spec["uv"]))
            faces[side] = spec
        if faces:
            out.append({**e, "from": [round(lo[k] - origin[k], 4) for k in range(3)], "to": [round(hi[k] - origin[k], 4) for k in range(3)],
                        "faces": faces})
    return out


def cells(size):
    """The blocks of a prop `size` (across, up, deep) in part order, as MultiDecorationBlock counts them: {right, up,
    away} from its first block, the part being right + across x (up + up-count x away). The frame runs across to the
    placer's left, so its block across is (across - 1 - right)."""
    w, h, d = size
    return [(r, u, a) for a in range(d) for u in range(h) for r in range(w)]


def frame_cell(size, part):
    r, u, a = cells(size)[part]
    return (size[0] - 1 - r, u, a)


def murk_clean(palette, alpha, top=False):
    """Murky fluid, clean: three even bands, palest at the top under a one-texel bright line where the surface meets the
    glass; from above (`top`) one flat shade inside a pale rim."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if top:
                    c = palette[2] if (x in (0, p.w - 1) or y in (0, p.h - 1)) else palette[1]
                else:
                    c = palette[2] if y == 0 else palette[2 - min(2, int(3 * y / p.h))]
                p.put(x, y, c, alpha)
    return paint


def pinned(elements):
    """`elements` with each face's uv written out where a block model works it out from the box's place, so a face keeps
    its picture when its box is moved or scaled."""
    return [dict(e, faces={side: dict(spec, uv=list(spec.get("uv") or _default_uv(side, e["from"], e["to"])))
                           for side, spec in e["faces"].items()}) for e in elements]


def grown(elements, scale, size):
    """`elements`, modelled in one block, `scale` times bigger in the frame of a prop `size` blocks (across, up, deep)
    (tools/decor17.py): every box about the block's bottom middle (8, 0, 8), set at the middle of the frame's floor, each
    face keeping its picture, so it looks just the same, only bigger."""
    return fa.transformed(pinned(elements), scale, (8 * size[0] - 8, 0, 8 * size[2] - 8))


def giant_beating_heart():
    """The Giant's Beating Heart, modelled whole in its frame: the Beating Heart Jar's boxes, three times bigger, on the
    jar's own texture. Its heart and brass stand are the jar's too, drawn as much bigger by the client."""
    g = d17.GIANT_HEART
    return grown(build("beating_heart_jar").models["beating_heart_jar"], g["size"], (g["size"],) * 3)


def specimen_vessel(name):
    """A bigger Specimen Jar, modelled whole in its frame: the Specimen Jar's boxes (tools/decor8_data.py jar_elements),
    `scale` times bigger, on its own textures. What floats in it is the jar's specimen, drawn as much bigger by the
    client."""
    v = d17.SPECIMEN_VESSELS[name]
    return grown(d8d.jar_elements(), v["scale"], v["size"])


# ---------------------------------------------------------------- the client's greyscale textures

def tint_textures():
    """Textures the client tints: brew (a rippled surface), fume (a wisp, fading up), wax (a candle's side and top),
    flame (white-hot core, paler edge, cut out), and glow (a flat square for the skull's sockets). All in even steps, with
    no per-pixel noise."""
    out = {}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = 200 + 12 * round(35 * math.sin(x * 0.9 + y * 0.4) * math.cos(y * 0.7 - x * 0.2) / 12)
            img.putpixel((x, y), (v, v, v, 215))
    out["witchs_workshop_brew"] = img
    img = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for y in range(64):
        t = y / 63
        for x in range(16):
            core = abs(x - 7.5 - math.sin(t * 9) * 3) / 8
            a = int(max(0, (1 - core * 1.4)) * 180 * t ** 0.6)
            if a > 0:
                img.putpixel((x, y), (230, 230, 230, max(0, min(255, a))))
    out["witchs_workshop_fume"] = img
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = 210 + 6 * round(25 * (1 - abs(x - 5) / 10) / 6)
            if x >= 12:
                v = 236 - (y % 3) * 6
            img.putpixel((x, y), (min(255, v), min(255, v), min(255, v), 255))
    out["witchs_workshop_wax"] = img
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    for y in range(32):
        t = 1 - y / 31
        half = max(0.0, math.sin(math.pi * min(1.0, t * 1.1)) ** 0.8 * 6.5 * (1 - t * 0.35))
        for x in range(16):
            d = abs(x - 7.5)
            if d <= half:
                inner = d < half * 0.5 and t < 0.75
                v = 255 if inner else 200
                img.putpixel((x, y), (v, v, int(v * (1.0 if inner else 0.92)), 255))
    out["witchs_workshop_flame"] = img
    # The skull's glowing sockets: a flat square, nearly opaque, with a one-texel rim a little fainter (the client draws it
    # exactly over each square socket, so a lit socket stays square, with no soft halo round it).
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y in range(8):
        for x in range(8):
            rim = x in (0, 7) or y in (0, 7)
            img.putpixel((x, y), (255, 255, 255, 170 if rim else 235))
    out["witchs_workshop_glow"] = img
    return out


# ---------------------------------------------------------------- building

JAR_BUILDERS = {"jar_of_eyeballs": jar_of_eyeballs, "beating_heart_jar": beating_heart_jar, "bat_in_a_jar": bat_in_a_jar,
                "two_headed_snake_jar": two_headed_snake_jar, "hand_in_a_jar": hand_in_a_jar}
_built = {}


def build(name):
    if name not in _built:
        builders = {d17.CAULDRON["block"]: horned_skull_cauldron, d17.EMBER_BED["block"]: ember_bed, "candelabra": candelabra,
                    d17.BROOM["block"]: enchanted_broom, d17.DUSTPAN["block"]: dustpan, d17.BROOM_RACK["block"]: broom_rack,
                    d17.CABINET["block"]: curiosity_cabinet, d17.BELL_JAR["block"]: bell_jar, d17.MOTH_CASE["block"]: moth_case,
                    **JAR_BUILDERS}
        _built[name] = builders[name]()
    return _built[name]


def texture_of(block):
    """The texture (and Sculpt) block `block` is drawn from: the candelabra share one, and the Giant's Beating Heart is
    drawn on the Beating Heart Jar's."""
    return "candelabra" if block in d17.CANDELABRA else "beating_heart_jar" if block == d17.GIANT_HEART["block"] else block


TALL_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, -3.5, 0], "scale": [0.5, 0.5, 0.5]},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.3, 0.3, 0.3]},
             "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.48, 0.48, 0.48]},
             "head": {"rotation": [0, 0, 0], "translation": [0, 8, 7], "scale": [0.6, 0.6, 0.6]},
             "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0.5, 1.5], "scale": [0.35, 0.35, 0.35]},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.38, 0.38, 0.38]}}
HANGING_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, 3, 0], "scale": [0.36, 0.36, 0.36]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.35, 0.35, 0.35]},
                "head": {"rotation": [0, 0, 0], "translation": [0, 10, 7], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 1.5], "scale": [0.25, 0.25, 0.25]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.28, 0.28, 0.28]}}


def item_candles(kind):
    """The item model's candles (ivory, unlit), a box for each, so the icon shows where they stand."""
    wax = build("candelabra").atlas.uvs["item_wax"]
    out = []
    w = d17.CANDLE_WIDTH / 2
    for x, y, z, h in d17.CANDELABRA[kind]["candles"]:
        out.append(cube((x - w, y, z - w), (x + w, y + h, z + w), {**{s: wax for s in SIDES4}, "up": wax}))
    return out


def single_sheets(elements):
    """`elements` with each sheet of no thickness keeping only its first face: the renderer draws these parts cut out or
    see-through, and 26.3's entityCutout and entityTranslucent draw both sides of a quad, so a reversed twin on the same
    plane would fight it (the moths' and bat's wings flickered as they flapped)."""
    out = []
    for e in elements:
        thin = [k for k in range(3) if abs(e["to"][k] - e["from"][k]) < 1e-9]
        if thin:
            axis_faces = {0: ("east", "west"), 1: ("up", "down"), 2: ("north", "south")}[thin[0]]
            keep = next(f for f in e["faces"] if f in axis_faces)
            e = dict(e, faces={keep: e["faces"][keep]})
        out.append(e)
    return out


def decor17_quads():
    """The renderer's models: the broom, the cabinet's doors, the moths and what floats in the jars."""
    out = {}
    for block, names in ((d17.BROOM["block"], ["enchanted_broom", "enchanted_broom_glow"]),
                         (d17.CABINET["block"], ["curiosity_cabinet_door_left", "curiosity_cabinet_door_right",
                                                 "curiosity_cabinet_door_left_pane", "curiosity_cabinet_door_right_pane"]),
                         (d17.MOTH_CASE["block"], [f"moth_{k}_{part}" for k in d17.MOTH_CASE["moths"]
                                                   for part in ("body", "wing_left", "wing_right")]),
                         ("jar_of_eyeballs", ["oddity_eyeball"]),
                         ("beating_heart_jar", ["oddity_heart", "oddity_heart_stand"]),
                         ("bat_in_a_jar", ["oddity_bat_body", "oddity_bat_wing_left", "oddity_bat_wing_right"]),
                         ("two_headed_snake_jar", ["oddity_snake_coil", "oddity_snake_head", "oddity_snake_tongue"]),
                         ("hand_in_a_jar", ["oddity_hand_palm", "oddity_hand_finger"])):
        sc = build(block)
        for name in names:
            qs = quads(single_sheets(sc.models[name]), {"p": block})
            # Glass is drawn see-through from both sides; everything else cut out.
            out[name] = [dict(q, nocull=True) if name.endswith("_pane") else dict(q, cutout=True) for q in qs]
    return out


# ---------------------------------------------------------------- files

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
    lang.update({f"block.{MOD}.{block}": display for block, display in d17.names().items()})
    lang[f"item.{MOD}.{d17.LADLE['item']}"] = d17.LADLE["display"]
    lang.update(d17.MESSAGES)

    def simple(block, display=fa.PLANT_DISPLAY, facing=False, model=None):
        sc = build(texture_of(block))
        write(models / f"{block}.json", fa.model(sc.name, sc.models[model or block]))
        if facing:
            write(states / f"{block}.json", {"variants": {f"facing={f}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})}
                                                          for f, y in FACINGS.items()}})
        else:
            write(states / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(items / f"{block}.json", {"parent": rid(f"block/{block}"), **({"display": display} if display else {})})

    simple(d17.CAULDRON["block"], facing=True)
    simple(d17.EMBER_BED["block"], display=None)
    write(items / f"{d17.LADLE['item']}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{d17.LADLE['item']}")}})

    # The candelabra: the frame by block state (the candles are the client's); the items show ivory candles too.
    sc = build("candelabra")
    for kind, info in d17.CANDELABRA.items():
        write(models / f"{kind}.json", fa.model(sc.name, sc.models[kind]))
        item_model = fa.model(sc.name, sc.models[kind] + item_candles(kind),
                              TALL_ITEM if info["tall"] else (HANGING_ITEM if kind == "branching_chandelier" else fa.PLANT_DISPLAY))
        write(items / f"{kind}.json", item_model)
    write(models / "floor_candelabrum_upper.json", {"textures": {"particle": rid("block/candelabra")}})
    write(states / "floor_candelabrum.json", {"multipart": [
        *[{"when": {"half": "lower", "facing": f}, "apply": {"model": rid("block/floor_candelabrum"), **({"y": y} if y else {})}}
          for f, y in FACINGS.items()],
        {"when": {"half": "upper"}, "apply": {"model": rid("block/floor_candelabrum_upper")}}]})
    for kind in ("table_candelabrum", "wall_girandole"):
        write(states / f"{kind}.json", {"multipart": [{"when": {"facing": f}, "apply": {"model": rid(f"block/{kind}"), **({"y": y} if y else {})}}
                                                      for f, y in FACINGS.items()]})
    write(states / "branching_chandelier.json", {"multipart": [{"apply": {"model": rid("block/branching_chandelier")}}]})

    # The broom is the client's; its block model only gives the particles.
    broom = d17.BROOM["block"]
    sc = build(broom)
    write(models / f"{broom}.json", {"textures": {"particle": rid(f"block/{broom}")}})
    write(states / f"{broom}.json", {"variants": {"": {"model": rid(f"block/{broom}")}}})
    write(items / f"{broom}.json", fa.model(broom, sc.models["enchanted_broom"], TALL_ITEM))
    simple(d17.DUSTPAN["block"], facing=True)
    simple(d17.BROOM_RACK["block"], facing=True)

    cabinet = d17.CABINET["block"]
    sc = build(cabinet)
    item = sc.models[cabinet] + [e for d in ("left", "right") for e in sc.models[f"curiosity_cabinet_door_{d}"]
                                 + sc.models[f"curiosity_cabinet_door_{d}_pane"]]
    write(models / f"{cabinet}.json", fa.model(cabinet, sc.models[cabinet]))
    write(models / f"{cabinet}_upper.json", {"textures": {"particle": rid(f"block/{cabinet}")}})
    write(states / f"{cabinet}.json", {"variants": {
        f"facing={f},half={h}": {"model": rid(f"block/{cabinet}" + ("_upper" if h == "upper" else "")), **({"y": y} if y and h == "lower" else {})}
        for f, y in FACINGS.items() for h in ("lower", "upper")}})
    write(items / f"{cabinet}.json", fa.model(cabinet, item, TALL_ITEM))
    simple(d17.BELL_JAR["block"])
    moth = d17.MOTH_CASE["block"]
    sc = build(moth)
    write(models / f"{moth}.json", fa.model(moth, sc.models[moth]))
    write(states / f"{moth}.json", {"variants": {
        f"facing={f},moth={m}": {"model": rid(f"block/{moth}"), **({"y": y} if y else {})}
        for f, y in FACINGS.items() for m in d17.MOTH_CASE["moths"]}})
    item = sc.models[moth] + fa.transformed(sc.models["moth_luna_body"] + sc.models["moth_luna_wing_left"] + sc.models["moth_luna_wing_right"],
                                            1.0, (8.0, 8.6, 15.3), (0, 0, 0))
    write(items / f"{moth}.json", fa.model(moth, item, fa.PLANT_DISPLAY))
    for name in d17.JARS:
        sc = build(name)
        write(models / f"{name}.json", fa.model(name, sc.models[name]))
        write(items / f"{name}.json", {"parent": rid(f"block/{name}"), "display": fa.PLANT_DISPLAY})
    write(states / "jar_of_eyeballs.json", {"variants": {"": {"model": rid("block/jar_of_eyeballs")}}})
    write(states / "beating_heart_jar.json", {"variants": {f"beat={str(b).lower()},tempo={t}": {"model": rid("block/beating_heart_jar")}
                                                           for b in (False, True) for t in range(len(d17.JARS["beating_heart_jar"]["tempos"]))}})
    for name in ("bat_in_a_jar", "two_headed_snake_jar"):
        write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
    write(states / "hand_in_a_jar.json", {"variants": {f"powered={str(p).lower()}": {"model": rid("block/hand_in_a_jar")} for p in (False, True)}})

    big_jar_assets(root, write)
    for block in d17.items():
        if block not in d17.SPECIMEN_VESSELS:
            write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
    write(root / "decor17_quads.json", decor17_quads())
    # The candelabra's candles, for Java to light and draw them by.
    write(root.parent.parent / MOD / "candelabra.json", d17.layout())


def multi_part_models(models, states, write, block, elements, size, model):
    """A prop of several blocks: one model per block, cut from its whole model `elements` (model(elements) makes a block
    model of them, and of nothing, for a block with nothing of it to draw, the particles only), and a blockstate choosing
    each block's model by its part and turning it by its facing."""
    write(models / f"{block}_part.json", {"textures": {"particle": model([])["textures"]["particle"]}})
    parts = []
    for part in range(len(cells(size))):
        cut = cell_part(elements, frame_cell(size, part), size)
        name = f"{block}_{part}" if cut else f"{block}_part"
        if cut:
            write(models / f"{name}.json", model(cut))
        for facing, y in FACINGS.items():
            parts.append({"when": {"facing": facing, "part": str(part)}, "apply": {"model": rid(f"block/{name}"), **({"y": y} if y else {})}})
    write(states / f"{block}.json", {"multipart": parts})


def bigger(display, k, elements):
    """Item `display` with the item (`elements`) drawn `k` times bigger in an inventory slot and moved up or down to its
    middle, so it stays inside the slot (a bigger jar's item is its small jar's)."""
    gui = display["gui"]
    scale = gui["scale"][0] * k
    ax, ay = (math.radians(a) for a in gui["rotation"][:2])
    # Each corner turned as the slot turns it (about y, then x), and how high it lands.
    ys = [((p[1] - 8) * math.cos(ax) - (-(p[0] - 8) * math.sin(ay) + (p[2] - 8) * math.cos(ay)) * math.sin(ax)) * scale
          for e in elements for p in ((x, y, z) for x in (e["from"][0], e["to"][0]) for y in (e["from"][1], e["to"][1])
                                      for z in (e["from"][2], e["to"][2]))]
    return dict(display, gui=dict(gui, translation=[0, round(-(min(ys) + max(ys)) / 2, 2), 0], scale=[round(scale, 4)] * 3))


# Vanilla's block display (minecraft:block/block), which the Specimen Jar's item keeps.
BLOCK_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}


def big_jar_assets(root, write):
    """The bigger jars' block models (one a block, cut from the whole), blockstates and items. Each item is its small
    jar's, a little bigger in a slot: the Giant's Beating Heart's the Beating Heart Jar with its heart resting on its
    stand, each specimen jar's the Specimen Jar's own item with the specimen it holds."""
    models, states, items = root / "models" / "block", root / "blockstates", root / "models" / "item"
    g = d17.GIANT_HEART
    heart = build("beating_heart_jar")
    multi_part_models(models, states, write, g["block"], giant_beating_heart(), (g["size"],) * 3, lambda els: fa.model(heart.name, els))
    resting = fa.transformed(heart.models["oddity_heart"], 1.0, d17.JARS["beating_heart_jar"]["heart"], (0, 0, 0))
    write(items / f"{g['block']}.json", fa.model(heart.name, heart.models["beating_heart_jar"] + heart.models["oddity_heart_stand"] + resting,
                                                bigger(fa.PLANT_DISPLAY, g["icon"], heart.models["beating_heart_jar"])))
    jar = ag.SPECIMEN_JAR["block"]
    for name, v in d17.SPECIMEN_VESSELS.items():
        multi_part_models(models, states, write, name, specimen_vessel(name), v["size"],
                          lambda els: dict(block_model(d8d.JAR_TEXTURES, els, d8d.JAR_TEXTURES["glass"]), ambientocclusion=False))
        for specimen in ag.SPECIMEN_JAR["specimens"]:
            write(items / f"{name}_{specimen}.json", {"parent": rid(f"block/{jar}_{specimen}_item"),
                                                      "display": bigger(BLOCK_DISPLAY, v["icon"], d8d.jar_elements())})
        first = ag.SPECIMEN_JAR["specimens"][0]
        write(root / "items" / f"{name}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "specimen",
            "cases": [{"when": s_, "model": {"type": "minecraft:model", "model": rid(f"item/{name}_{s_}")}}
                      for s_ in ag.SPECIMEN_JAR["specimens"] if s_ != first],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{name}_{first}")}}})


def loot(out, write):
    from decor_data import self_drop
    for block in d17.big_jars():
        # Placed and broken as one: the first block drops it (a specimen jar keeping its specimen).
        entry = {"type": "minecraft:item", "name": rid(block)}
        if block in d17.SPECIMEN_VESSELS:
            entry["modifier"] = {"type": "minecraft:copy_state", "block": rid(block), "properties": ["specimen"]}
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                {"type": "minecraft:match_block", "blocks": rid(block), "state": {"part": "0"}}]},
            "entries": [entry], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})
    for block in d17.blocks():
        if block in d17.big_jars():
            continue
        if block in ("floor_candelabrum", d17.CABINET["block"]):
            # Two blocks tall: the lower half drops it.
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
                "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                    {"type": "minecraft:match_block", "blocks": rid(block), "state": {"half": "lower"}}]},
                "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})
        else:
            write(out / f"{block}.json", self_drop(block))


def tags(tags):
    tags.add("block", f"{MOD}:heat_sources", rid(d17.EMBER_BED["block"]))
    for broom in d17.BROOMS:
        tags.add("item", f"{MOD}:brooms", broom)
    for floater in d17.FLOATERS:
        tags.add("item", f"{MOD}:cauldron_floaters", floater)
    for block in [d17.CAULDRON["block"], d17.EMBER_BED["block"], *d17.CANDELABRA, d17.DUSTPAN["block"]]:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in [d17.BROOM_RACK["block"], d17.CABINET["block"], d17.BELL_JAR["block"]]:
        tags.add("block", "minecraft:mineable/axe", rid(block))
    for block in d17.big_jars():
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))


def textures():
    """(kind, name) -> image for each block's texture, the ladle's item picture and the client's tinted textures."""
    # The bigger specimen jars draw on the Specimen Jar's textures (tools/decor8_textures.py).
    out = {("block", texture_of(block)): build(texture_of(block)).atlas.img for block in d17.blocks() if block not in d17.SPECIMEN_VESSELS}
    out[("item", d17.LADLE["item"])] = brew_ladle()
    for name, img in tint_textures().items():
        out[("entity", name)] = img
    return out
