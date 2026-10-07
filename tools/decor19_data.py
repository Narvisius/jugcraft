"""Generated data for Halloween decorations batch 19, the Laboratory, the Larder and the Dining Room (tools/decor19.py):
sculpted models on the toolkit of tools/flora_art.py (each block one texture painted here), blockstates, items, names,
loot and tags; and the quads the client draws the moving parts from (assets/jugcraft/decor19_quads.json): the crawling
hand, the cocoon, the spiderlings, the spools' silk, the dining chair, the floating table setting, the clock's hands,
moon, pendulum and ghost, the witchlights' wisp and glow, the yard silhouettes, the harvest moon's faces, and the Lab
Table patient's arms and eyes for when a Lightning Harness wakes it.

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import math
import random

from PIL import Image, ImageDraw

import cute_art as ca
import decor16_data as d16d
import decor17_data as d17d
import decor18_data as d18d
import decor19 as d19
import flora_art as fa
from flora_art import SIDES4, Px, Sculpt, cube, pal, plane_xy, plane_xz, plane_zy, rotation, shade, solid, strip
from decor6_data import quads

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL6 = ("north", "south", "east", "west", "up", "down")

IRON = d17d.IRON
BRASS = d17d.BRASS
COPPER = d17d.COPPER
BONE = d17d.BONE
SKIN = d17d.SKIN
# The Crawling Hand is the monster's (tools/decor16_data.py monster_head): the same bright green.
MONSTER_SKIN = d16d.SKIN
GLASS = d17d.GLASS
DARK_WOOD = pal("140b07", "1f120b", "2c1a10", "3a2315", "4a2e1c", "5c3a24")
WALNUT = pal("1e120a", "2e1c10", "402818", "543522", "6a442c", "805638")
RED = d18d.RED
GOLD = d18d.GOLD
PORCELAIN = pal("9aa0a8", "bcc2ca", "d8dde2", "eceff2", "fbfcfd")
PORCELAIN_BLUE = pal("1e2c6a", "2e44a0", "4a64c8")
PEWTER = pal("3a3d44", "50545c", "686c76", "828793", "9ea3ae", "bcc1ca")
WINE = pal("2a0610", "460a1a", "640f26")
TEA = pal("3a1a08", "5a2c10", "7a4018")
ROAST = pal("4a1e0c", "6e3010", "944418", "b8622a", "d4864a")
WAX = pal("b8ae96", "d4cab2", "ece4cc", "f8f2e0")
FLAME = pal("ff7a14", "ffb030", "ffe070", "fff8c8")
SILK = pal("8e8a84", "aeaaa2", "cac6be", "e0ddd6", "f2f0ea", "ffffff")
SPOOL_SILK = pal("9a9a9a", "b8b8b8", "d2d2d2", "e8e8e8", "f8f8f8")
SAC = pal("8c8678", "b0aa98", "d0caba", "e8e2d4", "faf6ec")
SAC_VEIN = pal("6a5a48", "8a7860")
BRAIN = pal("6a2a36", "8e3c4a", "b05462", "cc7280", "e298a2")
VAT_FLUID = pal("1e5a2a", "2a7a38", "3c9a4a")
PORCELAIN_INSULATOR = pal("8a8070", "b0a690", "d4cab2", "eee6d2")
SILHOUETTE = (14, 12, 16)
EYE_YELLOW = (255, 214, 64)
MOON = pal("6a4a1a", "9a7028", "c69a40", "e2be62", "f4d888", "fcecba")
MOON_SHADOW = pal("12141e", "1c2030", "262c40")
NIGHT_GLASS = {"purple": (150, 70, 220), "green": (100, 220, 80), "orange": (240, 140, 40), "blue": (70, 170, 240),
               "red": (230, 60, 50)}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


faces = d18d.faces
mirror_x = d18d.mirror_x


def noise(palette, seed=1, spread=(0, 0, 0, -1, 1)):
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(palette, n // 2 + rng.choice(spread)))
    return paint


def coil(seed=1, palette=COPPER):
    """Copper windings: bright and dark bands across the piece, a highlight down one side."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                k = 3 if y % 2 == 0 else 1
                if x == 0:
                    k += 1
                k += 0 if fa.QUIET else rng.choice((0, 0, 0, -1))
                p.put(x, y, shade(palette, k))
    return paint


def riveted(palette, seed=1, rivet=None):
    """A plate of `palette` with a rivet in each corner and a darker rim."""
    def paint(p):
        solid(palette, seed, rim=True)(p)
        r = rivet or palette[-1]
        for x, y in ((1, 1), (p.w - 2, 1), (1, p.h - 2), (p.w - 2, p.h - 2)):
            p.put(x, y, r)
    return paint


def dials():
    """The console's front: brass with two round gauges (white faces, black needles) and a row of toggles."""
    def paint(p):
        riveted(BRASS, 3)(p)
        for cx in (p.w * 0.28, p.w * 0.62):
            cy, r = p.h * 0.45, p.h * 0.32
            for y in range(p.h):
                for x in range(p.w):
                    d = math.hypot(x - cx, y - cy)
                    if d <= r:
                        p.put(x, y, PORCELAIN[3] if d < r - 1 else IRON[2])
            for t in range(int(r)):
                p.put(cx + t * 0.6, cy - t * 0.7, IRON[0])
            for a in range(0, 360, 45):
                p.put(cx + math.cos(math.radians(a)) * (r - 1.5), cy + math.sin(math.radians(a)) * (r - 1.5), IRON[1])
        for i in range(3):
            x = int(p.w * 0.82) + (i % 2)
            y = int(p.h * 0.25) + i * 3
            p.put(x, y, RED[4])
            p.put(x, y + 1, IRON[1])
    return paint


def brain(seed=1, lit=False):
    """A cartoon brain's folds: smooth pink, neat wavy grooves in even rows and a parting down the middle; a step
    brighter when it remembers."""
    def paint(p):
        lift = 1 if lit else 0
        for y in range(p.h):
            for x in range(p.w):
                wave = (y + (1 if (x // 2) % 2 else 0)) % 3 == 2
                p.put(x, y, BRAIN[1 + lift] if wave else BRAIN[3 + lift] if y % 3 == 0 else BRAIN[2 + lift])
        for y in range(p.h):
            p.put(p.w // 2, y, BRAIN[0])
    return paint


def silk(seed=1, palette=SILK, face=False):
    """Silk wound round and round: clean pale diagonal bands; if `face`, a sleepy face pressed through it (two closed
    eyes and a little round mouth)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                band = (x + y * 2) % 6
                p.put(x, y, palette[4] if band == 0 else palette[3] if band < 4 else palette[2])
        if face:
            cx = p.w / 2
            for ex in (cx - 2.5, cx + 1.5):
                p.put(ex, 4, palette[0])
                p.put(ex + 1, 4, palette[0])
                p.put(ex - 0.5, 3, palette[1])
                p.put(ex + 1.5, 3, palette[1])
            p.put(cx - 0.5, 7, palette[0])
            p.put(cx + 0.5, 7, palette[1])
    return paint


def sacs(seed=1, glisten=0):
    """Egg sacs: smooth pale round bumps, lit along their top, each with a glint that brightens with `glisten` (0 to
    3)."""
    def paint(p):
        ca.soft(SAC, 2, edge=1, top=0.3, bottom=0.3)(p)
        g = SAC[min(4, 2 + glisten)] if glisten < 3 else (255, 255, 255)
        p.put(1, 1, g)
        if p.w > 4:
            p.put(2, 1, g)
            p.put(1, 2, g)
    return paint


def web_sheet(size=64, seed=1, dew=0.2):
    """A sagging web curtain: radial threads from the top corners and the middle, swags hanging between them, and dew."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    rng = random.Random(seed)
    thread = (232, 236, 240, 200)
    faint = (220, 226, 232, 130)
    anchors = [(0, 0), (size - 1, 0), (size // 2, 0)]
    step = 2 if fa.QUIET else 1  # the clean style draws every other thread, so a small sheet does not clog
    for ax, ay in anchors:
        for k in range(0, 9, step):
            a = math.radians(20 + k * 17 + (0 if fa.QUIET else rng.uniform(-4, 4)))
            length = size * (0.9 if fa.QUIET else rng.uniform(0.7, 1.1))
            ex = ax + math.cos(a) * length * (1 if ax <= size // 2 else -1) * (0.6 if ax == size // 2 else 1)
            ey = ay + math.sin(a) * length
            draw.line([(ax, ay), (ex, ey)], fill=faint, width=1)
    for row in range(1, 9, step):
        y0 = row * size / 10
        pts = []
        for i in range(17):
            x = i * (size - 1) / 16
            sag = math.sin(i / 16 * math.pi) * size * 0.05 * (1 + row * 0.1)
            pts.append((x, y0 + sag + (0 if fa.QUIET else rng.uniform(-0.6, 0.6))))
        draw.line(pts, fill=thread, width=1)
    px = img.load()
    for _ in range(0 if fa.QUIET else int(size * size * dew / 40)):
        x, y = rng.randrange(size), rng.randrange(size)
        if px[x, y][3]:
            px[x, y] = (250, 252, 255, 255)
    return img


def porcelain(seed=1, rim=True):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                edge = rim and (x in (0, p.w - 1) or y in (0, p.h - 1))
                p.put(x, y, PORCELAIN_BLUE[1] if edge else shade(PORCELAIN, 3 + rng.choice((0, 0, -1))))
    return paint


def flame():
    """A candle flame, wide at the bottom, white at its heart, with see-through all round."""
    def paint(p):
        cx = (p.w - 1) / 2
        for y in range(p.h):
            t = y / max(1, p.h - 1)
            half = (p.w / 2) * math.sin(math.pi * (0.15 + t * 0.85)) * (0.5 + 0.5 * t)
            for x in range(p.w):
                d = abs(x - cx)
                if d <= half:
                    k = 3 if d < half * 0.4 and t > 0.4 else 2 if d < half * 0.7 else 1 if t > 0.3 else 0
                    p.put(x, y, FLAME[k])
    return paint


def glow_disc(colour=(255, 255, 255), alpha=255):
    """A soft round glow fading to nothing at its edge."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        r = min(cx, cy) + 0.5
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot(x - cx, y - cy) / r
                if d < 1:
                    p.put(x, y, colour, int(alpha * (1 - d) ** 1.6))
    return paint


def wisp_texture():
    """A wisp: a pale flame-shaped flicker with a curling tail, white so the client can colour it."""
    def paint(p):
        cx = (p.w - 1) / 2
        for y in range(p.h):
            t = y / max(1, p.h - 1)
            half = p.w * 0.38 * math.sin(math.pi * t) ** 0.8
            off = math.sin(t * 5.0) * p.w * 0.12
            for x in range(p.w):
                d = abs(x - cx - off)
                if d <= half:
                    p.put(x, y, (255, 255, 255), int(255 * (1 - d / max(half, 0.5)) ** 0.6))
    return paint


# ---------------------------------------------------------------- 11. the reanimation rig

@fa.quietly
def lightning_harness():
    """The Lightning Harness, hung from a ceiling: a riveted copper crown plate, a cage of copper ribs and coil rings
    round a wound coil, two glass valves on the crown, and two brass electrode arms hanging on chains each side, their
    tips (the arcs' ends) at x 3.5 and 12.5, y 3.2, z 8."""
    sc = Sculpt(d19.HARNESS["block"], 191, 64)
    plate = sc.piece("plate", 24, 24, riveted(COPPER, 2))
    rim = sc.piece("rim", 24, 2, d17d.brass(3))
    rib = sc.piece("rib", 2, 16, coil(4))
    ring = sc.piece("ring", 20, 2, coil(5))
    core = sc.piece("core", 6, 14, coil(6))
    chain = sc.piece("chain", 2, 14, d17d.chain_links())
    brass = sc.piece("brass", 4, 6, d17d.brass(7))
    insulator = sc.piece("insulator", 4, 4, solid(PORCELAIN_INSULATOR, 8, rim=True))
    glass = sc.piece("glass", 4, 6, d17d.glass(120))
    filament = sc.piece("filament", 2, 4, solid([(255, 200, 120), (255, 230, 170)], 9))
    els = [cube((2.0, 14.6, 2.0), (14.0, 15.9, 14.0), faces(plate, ALL6)),
           cube((1.6, 14.2, 1.6), (14.4, 14.8, 14.4), faces(rim, ALL6)),
           cube((6.5, 7.0, 6.5), (9.5, 14.2, 9.5), faces(core, SIDES4, down=insulator))]
    for x, z in ((3.0, 3.0), (12.0, 3.0), (3.0, 12.0), (12.0, 12.0)):
        els.append(cube((x, 6.0, z), (x + 1.0, 14.2, z + 1.0), faces(rib, ALL6)))
    for y in (6.0, 9.4, 12.8):
        els += [cube((3.0, y, 3.0), (13.0, y + 0.8, 4.0), faces(ring, ALL6)), cube((3.0, y, 12.0), (13.0, y + 0.8, 13.0), faces(ring, ALL6)),
                cube((3.0, y, 4.0), (4.0, y + 0.8, 12.0), faces(ring, ALL6)), cube((12.0, y, 4.0), (13.0, y + 0.8, 12.0), faces(ring, ALL6))]
    for x in (5.0, 10.0):
        els += [cube((x, 15.9, 7.0), (x + 1.4, 16.0, 8.4), faces(brass, ("up",))),
                cube((x - 0.2, 10.4, 6.8), (x + 1.6, 14.2, 8.6), faces(glass, ALL6)),
                cube((x + 0.4, 11.0, 7.4), (x + 1.0, 13.6, 8.0), faces(filament, ALL6), light=12)]
    for x in (3.5, 12.5):
        side = -1 if x < 8 else 1
        els += [cube((x - 0.5, 13.0, 7.5), (x + 0.5, 14.0, 8.5), faces(brass, ALL6)),
                plane_zy(7.5, 8.5, 5.6, 13.0, x, chain),
                cube((x - 0.9, 5.0, 7.1), (x + 0.9, 5.6, 8.9), faces(insulator, ALL6)),
                cube((x - 0.5, 3.6, 7.5), (x + 0.5, 5.0, 8.5), faces(brass, ALL6)),
                cube((x - 0.7, 2.8, 7.3), (x + 0.7, 3.6, 8.7), faces(brass, ALL6))]
        els.append(cube((x - 0.4 + side * 0.0, 13.2, 6.0), (x + 0.4, 13.8, 7.5), faces(brass, ALL6)))
    sc.models[d19.HARNESS["block"]] = els
    return sc


@fa.quietly
def brain_vat_console():
    """The Brain-Vat Console, facing north: a riveted brass console with a sloping gauge panel and a toggle lever, and on
    its back a glass vat of green fluid with a brain floating in it under a brass cap; cords run from the cap down into
    the console. The lit model's brain glows."""
    sc = Sculpt(d19.CONSOLE["block"], 192, 64)
    body = sc.piece("body", 28, 16, riveted(BRASS, 2))
    top = sc.piece("top", 28, 24, riveted(BRASS, 3))
    panel = sc.piece("panel", 26, 10, dials())
    glass = sc.piece("glass", 12, 13, d17d.glass(90))
    fluid = sc.piece("fluid", 10, 10, d17d.fluid(VAT_FLUID, 4, 140))
    dark = sc.piece("brain", 8, 8, brain(5))
    lit = sc.piece("brain_lit", 8, 8, brain(5, lit=True))
    cap = sc.piece("cap", 14, 14, d17d.brass(6))
    cord = sc.piece("cord", 2, 10, solid([(30, 20, 18), (50, 34, 28)], 7))
    lever = sc.piece("lever", 2, 6, solid(IRON[2:], 8))
    knob = sc.piece("knob", 2, 2, solid(RED[3:], 9))
    base = [cube((1.0, 0.0, 1.0), (15.0, 7.0, 15.0), faces(body, SIDES4, up=top, down=top)),
            cube((1.5, 6.0, 1.6), (14.5, 7.4, 6.6), faces(panel, ("north", "up", "east", "west")), rotation((8.0, 6.0, 6.6), "x", -22.5)),
            cube((12.4, 7.0, 3.0), (13.0, 10.0, 3.6), faces(lever, ALL6)),
            cube((12.2, 10.0, 2.8), (13.2, 11.0, 3.8), faces(knob, ALL6)),
            cube((4.6, 7.0, 7.6), (11.4, 7.8, 14.4), faces(cap, ALL6)),
            cube((4.8, 7.8, 7.8), (11.2, 14.4, 14.2), faces(glass, ALL6)),
            cube((5.4, 8.0, 8.4), (10.6, 13.0, 13.6), faces(fluid, ALL6)),
            cube((4.4, 14.4, 7.4), (11.6, 15.4, 14.6), faces(cap, ALL6)),
            cube((7.4, 15.4, 10.4), (8.6, 16.0, 11.6), faces(cap, ALL6)),
            plane_zy(10.0, 13.0, 7.0, 15.0, 4.2, cord),
            plane_zy(10.0, 13.0, 7.0, 15.0, 11.8, cord)]
    for name, uv, glow in (("brain_vat_console", dark, None), ("brain_vat_console_lit", lit, 10)):
        sc.models[name] = base + [cube((6.4, 9.2, 9.4), (9.6, 12.0, 12.6), faces(uv, ALL6), light=glow),
                                  cube((7.6, 8.4, 10.6), (8.4, 9.2, 11.4), faces(uv, ALL6), light=glow)]
    return sc


# The crawling hand's knuckles (x, y, z in pixels), as CrawlingHandRenderer lifts its fingers about them.
KNUCKLES = [(4.8, 1.4, 5.0), (6.4, 1.4, 4.4), (8.0, 1.4, 4.6), (9.6, 1.4, 5.2)]
FINGER_LENGTHS = [3.0, 3.8, 3.6, 2.8]


def crawling_hand():
    """The Crawling Hand, lying palm down pointing north in the middle of its block: a stitched palm, its
    wrist cut off at the south, a thumb out to the east, and four fingers (each its own quad model, so the client
    lifts them) with yellowed nails; bright green, the monster's own skin."""
    sc = Sculpt(d19.HAND["block"], 193, 64)
    back = sc.piece("back", 14, 12, d17d.stitches(2, MONSTER_SKIN))
    side = sc.piece("side", 12, 3, d17d.skin(3, palette=MONSTER_SKIN))
    stump = sc.piece("stump", 10, 4, ca.bevel(RED, 3))
    finger = sc.piece("finger", 3, 8, d17d.skin(5, nail=True, palette=MONSTER_SKIN))
    palm = [cube((4.0, 0.0, 5.0), (11.0, 1.6, 10.5), faces(back, ("up",), north=side, south=side, east=side, west=side, down=side)),
            cube((4.6, 0.0, 10.5), (10.4, 1.4, 12.2), faces(side, ("up", "east", "west", "down"), south=stump)),
            cube((11.0, 0.0, 6.4), (13.2, 1.2, 8.2), faces(finger, ALL6), rotation((11.0, 0.6, 8.2), "y", 22.5))]
    sc.models["crawling_hand"] = palm
    fingers = []
    for i, ((x, y, z), length) in enumerate(zip(KNUCKLES, FINGER_LENGTHS)):
        f = [cube((x - 0.7, 0.0, z - length), (x + 0.7, 1.2, z + 0.4), faces(finger, ALL6))]
        sc.models[f"crawling_hand_finger_{i}"] = f
        fingers += f
    sc.models["item"] = palm + fingers
    return sc


def lab_table_arms():
    """The Lab Table patient's arms for when it is woken, in the torso's lying frame (decor8_data.patient_torso, hips at
    y 13.5, z 2): from the shoulders (z -9) straight up out of the sheet, grey hands at their ends; sat up, they reach
    out in front. Drawn on the Lab Table's own textures."""
    els = []
    for x0, x1 in ((1.6, 3.4), (12.6, 14.4)):
        els += [{"from": [x0, 13.0, -9.0], "to": [x1, 21.0, -7.2], "faces": {s: {"texture": "#sheet"} for s in ALL6}},
                {"from": [x0 - 0.1, 21.0, -9.1], "to": [x1 + 0.1, 23.6, -7.1], "faces": {s: {"texture": "#skin"} for s in ALL6}}]
    return els


def lab_table_eyes():
    """Two eyes glowing through the sheet on the patient's face (the head's top in the lying frame, y 17), each one sheet
    0.1 above it (the renderer draws it from both sides) showing the bright middle of the glow picture, its UVs pinned
    inside the sprite."""
    eye = {"up": {"texture": "#eye", "uv": [4, 4, 12, 12]}}
    return [{"from": [6.2, 17.1, -13.4], "to": [7.4, 17.1, -12.4], "faces": eye},
            {"from": [8.6, 17.1, -13.4], "to": [9.8, 17.1, -12.4], "faces": eye}]


# ---------------------------------------------------------------- 12. the spider's larder

def silk_cocoon():
    """The Silk Cocoon, hung from the top of its block: a thread from the ceiling to a knot, then a long wrapped bundle,
    broad at the shoulders, a head's bulge at the top with a face pressed through the silk, and loose strands."""
    sc = Sculpt(d19.COCOON["block"], 194, 64)
    wrap = sc.piece("wrap", 16, 20, silk(2))
    face_ = sc.piece("face", 10, 10, silk(3, face=True))
    top = sc.piece("top", 10, 10, silk(4))
    thread = sc.piece("thread", 2, 8, ca.bevel(SILK, 4, light=1, dark=0))
    strand = sc.piece("strand", 6, 6, d18d.membrane(SILK, 6) if hasattr(d18d, "membrane") else silk(6))
    els = [plane_zy(7.5, 8.5, 13.0, 16.0, 8.0, thread), plane_xy(7.5, 8.5, 13.0, 16.0, 8.0, thread),
           cube((7.0, 12.0, 7.0), (9.0, 13.2, 9.0), faces(top, ALL6)),
           cube((5.8, 8.6, 5.8), (10.2, 12.0, 10.2), faces(top, SIDES4, north=face_, up=top)),
           cube((5.0, 3.0, 5.0), (11.0, 8.6, 11.0), faces(wrap, ALL6)),
           cube((5.4, 4.0, 4.6), (10.6, 8.0, 11.4), faces(wrap, ("north", "south"))),
           cube((5.6, 1.2, 5.6), (10.4, 3.0, 10.4), faces(wrap, ALL6)),
           cube((6.6, 0.2, 6.6), (9.4, 1.2, 9.4), faces(top, ALL6))]
    for (x, y, z, rot) in ((4.6, 6.0, 8.0, 22.5), (11.4, 5.0, 7.0, -22.5)):
        els.append(plane_zy(z - 1.5, z + 1.5, y - 2.0, y + 1.0, x, strand, rotation((x, y, z), "x", rot)))
    sc.models["silk_cocoon"] = els
    sc.models["item"] = els
    return sc


@fa.quietly
def egg_sac_cluster(glisten=0):
    """The Egg Sac Cluster on the north face of its block (the blockstate turns it to each face): a mat of web over the
    face and a cluster of round sacs of several sizes on it. Its texture is a strip of frames, the sacs' glints
    brightening and dimming (`glisten` 0 to 3), so they pulse. Each sac is a closed box, its back too (seen through the
    web on the item from behind): 0.1 or more in front of the mat, and on a plane of its own wherever it overlaps an
    earlier sac, so no two backs share a plane."""
    sc = Sculpt(d19.EGG_SACS["block"], 195, 32)
    mat = sc.piece("mat", 16, 16, lambda p: p.img.alpha_composite(web_sheet(16, 4, 0.4)))
    big = sc.piece("big", 6, 6, sacs(2, glisten))
    small = sc.piece("small", 4, 4, sacs(3, glisten))
    els = [cube((0.0, 0.0, 0.05), (16.0, 16.0, 0.1), {"south": mat, "north": mat})]
    rng = random.Random(7)
    spots = [(5.0, 6.0, 3.0), (9.5, 7.5, 2.6), (7.0, 10.5, 2.2), (11.0, 11.5, 1.8), (4.0, 10.0, 1.8), (10.5, 3.8, 2.0), (7.0, 3.0, 1.6)]
    placed = []
    for x, y, r in spots:
        uv = big if r > 2.1 else small
        depth = r * 0.9 + rng.uniform(0, 0.3)
        taken = {back for px, py, pr, back in placed if abs(px - x) < pr + r and abs(py - y) < pr + r}
        back = next(b for b in (0.2, 0.3, 0.4, 0.5, 0.6) if b not in taken)
        placed.append((x, y, r, back))
        els.append(cube((x - r, y - r, back), (x + r, y + r, 0.1 + depth), faces(uv, ALL6)))
    sc.models[d19.EGG_SACS["block"]] = els
    sc.models["item"] = d18d.shifted(els, dz=7.0)
    return sc


# The egg sacs' frames: their glints brighten and fade again.
GLISTEN = [0, 1, 2, 3, 2, 1]


def egg_sac_strip():
    frames = [egg_sac_cluster(g).atlas.img for g in GLISTEN]
    strip = Image.new("RGBA", (32, 32 * len(frames)), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, 32 * i))
    return strip


def spiderling_sculpt():
    sc = Sculpt("spiderling", 209, 16)
    sc.piece("spider_body", 4, 4, lambda p: (ca.bevel(pal("100b0a", "1a1210", "261a16", "34241e"), 1)(p), p.put(1, 1, (220, 70, 70))))
    sc.piece("spider_leg", 2, 4, ca.bevel(pal("120c0a", "1e1612", "2a201a"), 1))
    sc.models["spiderling"] = spiderling(sc)
    return sc


def web_drape():
    """The Web Drape, facing north: a plane of web across the middle of each of its four blocks, the curtain two blocks
    wide and two tall, each block showing its quarter of one 32 x 32 picture."""
    sc = Sculpt(d19.DRAPE["block"], 196, 64)
    sheet = web_sheet(64, 3)
    whole = sc.piece("web", 64, 64, lambda p: p.img.alpha_composite(sheet))
    u0, v0, u1, v1 = whole
    mu, mv = (u0 + u1) / 2, (v0 + v1) / 2
    quarter = {0: (mu, mv, u1, v1), 1: (u0, mv, mu, v1), 2: (mu, v0, u1, mv), 3: (u0, v0, mu, mv)}
    for part, uv in quarter.items():
        sc.models[f"web_drape_{part}"] = [plane_xy(0.0, 16.0, 0.0, 16.0, 8.0, uv)]
    sc.models["item"] = [plane_xy(0.0, 16.0, 0.0, 16.0, 8.0, whole)]
    return sc


# The spools' centres across the block, left to right as seen from the front (north): spool 0 is the east one.
SPOOL_X = [12.0, 8.0, 4.0]


@fa.quietly
def silk_spool_stack():
    """The Silk Spool Stack, facing north: a walnut board with three tall turned spools standing on it, each a core
    between two flanges; the silk wound on them is the client's (tinted). A long needle lies across the board's front."""
    sc = Sculpt(d19.SPOOLS["block"], 197, 64)
    board = sc.piece("board", 28, 20, d17d.wood(WALNUT, 2, panel=True))
    edge = sc.piece("edge", 28, 3, d17d.wood(WALNUT, 3))
    flange = sc.piece("flange", 8, 8, d17d.wood(WALNUT, 4))
    core = sc.piece("core", 4, 20, d17d.wood(WALNUT, 5))
    needle = sc.piece("needle", 12, 1, solid(PEWTER[3:], 6))
    silk_ = sc.piece("silk", 8, 18, silk(7, SPOOL_SILK))
    els = [cube((1.0, 0.0, 3.0), (15.0, 1.5, 13.0), faces(edge, SIDES4, up=board, down=board)),
           cube((2.0, 1.5, 3.6), (14.0, 1.9, 4.0), faces(needle, ALL6))]
    for x in SPOOL_X:
        els += [cube((x - 2.6, 1.5, 5.4), (x + 2.6, 2.6, 10.6), faces(flange, ALL6)),
                cube((x - 1.2, 2.6, 6.8), (x + 1.2, 12.6, 9.2), faces(core, ALL6)),
                cube((x - 2.6, 12.6, 5.4), (x + 2.6, 13.6, 10.6), faces(flange, ALL6))]
    sc.models[d19.SPOOLS["block"]] = els
    silks = {}
    for i, x in enumerate(SPOOL_X):
        # Closed at both ends: the client draws the silk on its own, culled, so an open end is a hole into it.
        silks[i] = [cube((x - 2.1, 2.8, 5.9), (x + 2.1, 12.4, 10.1), faces(silk_, ALL6)),
                    cube((x - 1.6, 2.8, 5.4), (x + 1.6, 12.4, 10.6), faces(silk_, ALL6))]
        sc.models[f"silk_spool_silk_{i}"] = silks[i]
    sc.models["item"] = els + [e for i in silks for e in silks[i]]
    return sc


def spiderling(sc):
    """A spiderling no bigger than a fingernail, centred on the origin and standing on y 0 (EggSacRenderer runs three
    out from the sacs at night): a round abdomen, a smaller head, and eight legs bent at the knee."""
    body = sc.atlas.uvs["spider_body"]
    leg = sc.atlas.uvs["spider_leg"]
    els = [cube((-0.7, 0.35, 0.1), (0.7, 1.25, 1.7), faces(body, ALL6)),
           cube((-0.5, 0.3, -0.9), (0.5, 0.95, 0.15), faces(body, ALL6))]
    for side in (-1, 1):
        for i, z in enumerate((-0.6, -0.25, 0.1, 0.45)):
            spread = {"origin": [0.0, 0.8, z], "axis": "y", "angle": side * (i - 1.5) * 25.0}
            x0, x1 = (0.4, 1.7) if side > 0 else (-1.7, -0.4)
            knee = 1.47 * side
            els += [{"from": [x0, 0.74, z - 0.08], "to": [x1, 0.86, z + 0.08], "faces": {s: {"texture": "#p", "uv": list(leg)} for s in ALL6},
                     "rotations": [{"origin": [0.0, 0.8, z], "axis": "z", "angle": side * 30.0}, spread]},
                    {"from": [knee - 0.07, 0.0, z - 0.07], "to": [knee + 0.07, 1.65, z + 0.07],
                     "faces": {s: {"texture": "#p", "uv": list(leg)} for s in ALL6},
                     "rotations": [{"origin": [knee, 1.65, z], "axis": "z", "angle": side * 20.0}, spread]}]
    return els


# ---------------------------------------------------------------- 13. the poltergeist's dinner party

@fa.quietly
def haunted_dining_chair():
    """The Haunted Dining Chair, its sitter facing north to the table: four turned legs and an apron, a tufted red velvet
    seat, and a tall gothic back between two posts with finials: a pierced tracery splat under a carved crest rail.
    Drawn by DiningChairRenderer from the quads (it slides out); the block model only gives particles."""
    sc = Sculpt(d19.CHAIR["block"], 198, 128)
    leg = sc.piece("leg", 3, 16, d17d.wood(DARK_WOOD, 2))
    rail = sc.piece("rail", 24, 3, d17d.wood(DARK_WOOD, 3))
    seat = sc.piece("seat", 24, 22, d17d.wood(DARK_WOOD, 4, panel=True))
    cushion = sc.piece("cushion", 22, 20, d18d.velvet(RED, 5))
    cushion_side = sc.piece("cushion_side", 22, 2, d18d.velvet(RED, 6, tufted=False))
    post = sc.piece("post", 3, 28, d17d.wood(DARK_WOOD, 7))
    knob = sc.piece("knob", 3, 3, d17d.wood(DARK_WOOD, 8))
    crest = sc.piece("crest", 32, 10, d17d.carved_crest(9))
    splat = sc.piece("splat", 32, 40, tracery(10))
    els = []
    for x0, z0 in ((2.4, 2.4), (12.0, 2.4)):
        els += [cube((x0 + 0.2, 0.0, z0 + 0.2), (x0 + 1.4, 0.6, z0 + 1.4), faces(knob, ALL6)),
                cube((x0 + 0.3, 0.6, z0 + 0.3), (x0 + 1.3, 3.2, z0 + 1.3), faces(leg, ALL6)),
                cube((x0 + 0.1, 3.2, z0 + 0.1), (x0 + 1.5, 4.2, z0 + 1.5), faces(knob, ALL6)),
                cube((x0 + 0.25, 4.2, z0 + 0.25), (x0 + 1.35, 7.5, z0 + 1.35), faces(leg, ALL6))]
    for x0 in (2.4, 12.0):
        els += [cube((x0 + 0.2, 0.0, 12.2), (x0 + 1.4, 0.6, 13.8), faces(knob, ALL6)),
                cube((x0 + 0.2, 0.6, 12.4), (x0 + 1.4, 27.0, 13.6), faces(post, ALL6)),
                cube((x0 + 0.05, 27.0, 12.25), (x0 + 1.55, 28.0, 13.75), faces(knob, ALL6)),
                cube((x0 + 0.35, 28.0, 12.55), (x0 + 1.25, 29.6, 13.45), faces(knob, ALL6)),
                cube((x0 + 0.6, 29.6, 12.8), (x0 + 1.0, 30.2, 13.2), faces(knob, ALL6))]
    els += [cube((2.6, 6.0, 2.6), (13.4, 7.5, 3.0), faces(rail, ALL6)), cube((2.6, 6.0, 12.6), (13.4, 7.5, 13.0), faces(rail, ALL6)),
            cube((2.6, 6.0, 3.0), (3.0, 7.5, 12.6), faces(rail, ALL6)), cube((13.0, 6.0, 3.0), (13.4, 7.5, 12.6), faces(rail, ALL6)),
            cube((2.6, 1.6, 3.6), (3.6, 2.2, 12.6), faces(rail, ALL6)), cube((12.4, 1.6, 3.6), (13.4, 2.2, 12.6), faces(rail, ALL6)),
            cube((3.6, 1.6, 7.8), (12.4, 2.2, 8.4), faces(rail, ALL6)),
            cube((2.0, 7.5, 2.0), (14.0, 8.6, 13.4), faces(seat, ("up", "down"), north=rail, south=rail, east=rail, west=rail)),
            cube((2.6, 8.6, 2.5), (13.4, 9.8, 12.4), faces(cushion, ("up",), north=cushion_side, south=cushion_side, east=cushion_side,
                                                            west=cushion_side)),
            cube((3.8, 9.6, 12.6), (12.2, 10.4, 13.4), faces(rail, ALL6)),
            plane_xy(3.8, 12.2, 10.4, 24.4, 13.0, splat),
            cube((3.8, 24.4, 12.5), (12.2, 27.2, 13.5), faces(rail, ("east", "west", "up", "down"), north=crest, south=crest)),
            cube((6.4, 27.2, 12.7), (9.6, 28.4, 13.3), faces(knob, ALL6)),
            cube((7.5, 28.4, 12.8), (8.5, 29.4, 13.2), faces(knob, ALL6))]
    sc.models["haunted_dining_chair"] = els
    return sc


def tracery(seed=1):
    """A chair-back splat cut in gothic tracery: a pointed arch with two lancets under a quatrefoil, all pierced, carved
    in the dark wood."""
    def paint(p):
        w, h = p.w, p.h
        d17d.wood(DARK_WOOD, seed)(p)
        img = p.img
        hole = Image.new("L", (w, h), 0)
        draw = ImageDraw.Draw(hole)

        def lancet(x0, x1, y0, y1):
            r = x1 - x0
            draw.rectangle([x0, y0 + r * 0.6, x1, y1], fill=255)
            draw.pieslice([x0, y0, x0 + 2 * r, y0 + 2 * r], 180, 240, fill=255)
            draw.pieslice([x1 - 2 * r, y0, x1, y0 + 2 * r], 300, 360, fill=255)
        lancet(w * 0.18, w * 0.46, h * 0.38, h * 0.9)
        lancet(w * 0.54, w * 0.82, h * 0.38, h * 0.9)
        cx, cy, r = w / 2, h * 0.2, w * 0.11
        for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            draw.ellipse([cx + dx * r * 0.8 - r * 0.75, cy + dy * r * 0.8 - r * 0.75, cx + dx * r * 0.8 + r * 0.75, cy + dy * r * 0.8 + r * 0.75],
                         fill=255)
        px = img.load()
        hp = hole.load()
        for y in range(h):
            for x in range(w):
                if hp[x, y]:
                    px[x, y] = (0, 0, 0, 0)
                elif any(0 <= x + dx < w and 0 <= y + dy < h and hp[x + dx, y + dy] for dx, dy in ((1, 0), (0, 1))):
                    px[x, y] = DARK_WOOD[5] + (255,)
                elif any(0 <= x + dx < w and 0 <= y + dy < h and hp[x + dx, y + dy] for dx, dy in ((-1, 0), (0, -1))):
                    px[x, y] = DARK_WOOD[0] + (255,)
        for x in range(w):
            for y in (0, h - 1):
                px[x, y] = DARK_WOOD[1] + (255,)
    return paint


def disc(cx, cz, r, y0, y1, uv_top, uv_side, steps=(1.0, 0.72), bottom=None):
    """A round dish or foot of radius `r` as two crossed boxes (an octagon, near enough)."""
    a, b = r * steps[0], r * steps[1]
    return [cube((cx - a, y0, cz - b), (cx + a, y1, cz + b), faces(uv_side, SIDES4, up=uv_top, down=bottom or uv_top)),
            cube((cx - b, y0, cz - a), (cx + b, y1, cz + a), faces(uv_side, SIDES4, up=uv_top, down=bottom or uv_top))]


def floating_table_setting():
    """The Floating Table Setting's pieces, laid for a diner to the north, each its own quad model so the client bobs
    them apart: a pewter charger with a folded napkin, a fork and a knife, a goblet of wine (its foot at the renderer's
    GOBLET), a brass candlestick (its wick at WICK) and the flame; for tea a saucer, a cup and a teapot (its foot at
    TEAPOT) with its spout to the east; for a feast a platter with a roast bird. The block model only gives particles;
    the item is the dinner."""
    sc = Sculpt(d19.SETTING["block"], 199, 128)
    pewter = sc.piece("pewter", 16, 16, solid(PEWTER, 2, rim=True))
    pewter_side = sc.piece("pewter_side", 16, 2, solid(PEWTER[1:4], 3))
    napkin = sc.piece("napkin", 8, 8, d18d.velvet(RED, 4, tufted=False))
    steel = sc.piece("steel", 2, 10, solid(PEWTER[3:], 5))
    goblet = sc.piece("goblet", 6, 8, solid(PEWTER[2:], 6, glossy=PEWTER[-1]))
    wine = sc.piece("wine", 6, 6, solid(WINE, 7))
    brass = sc.piece("brass", 6, 8, d17d.brass(8))
    wax = sc.piece("wax", 4, 8, solid(WAX, 9))
    wick = sc.piece("wick", 1, 2, solid([(20, 16, 14)], 10))
    flame_ = sc.piece("flame", 8, 12, flame())
    china = sc.piece("china", 12, 12, porcelain(11))
    china_side = sc.piece("china_side", 12, 3, porcelain(12))
    tea = sc.piece("tea", 6, 6, solid(TEA, 13))
    roast = sc.piece("roast", 16, 12, roasted(14))
    bone = sc.piece("bone", 2, 4, solid(BONE[3:], 15))
    garnish = sc.piece("garnish", 4, 4, solid(pal("3a6a1e", "4e8a26", "c86a1a", "e08a2a"), 16))
    pieces = {}
    pieces["setting_plate"] = (disc(6.5, 7.5, 4.0, 1.5, 1.9, pewter, pewter_side)
                               + disc(6.5, 7.5, 2.8, 1.9, 2.1, pewter, pewter_side)
                               + [cube((5.0, 2.1, 6.2), (8.0, 2.6, 9.0), faces(napkin, ALL6)),
                                  cube((5.6, 2.6, 6.4), (7.4, 3.0, 8.6), faces(napkin, ALL6), rotation((6.5, 2.6, 7.5), "y", 45))])
    pieces["setting_cutlery"] = [cube((1.4, 1.5, 6.0), (1.9, 1.75, 11.0), faces(steel, ALL6)),
                                 cube((1.1, 1.5, 4.0), (2.2, 1.75, 6.0), faces(steel, ALL6))]
    for i in range(4):
        pieces["setting_cutlery"].append(cube((1.15 + i * 0.28, 1.5, 3.0), (1.3 + i * 0.28, 1.75, 4.0), faces(steel, ALL6)))
    pieces["setting_cutlery"] += [cube((11.0, 1.5, 8.0), (11.45, 1.75, 11.5), faces(steel, ALL6)),
                                  cube((10.9, 1.5, 5.6), (11.6, 1.7, 8.0), faces(steel, ALL6))]
    gx, gy, gz = 11.5, 1.5, 5.0
    pieces["setting_goblet"] = (disc(gx, gz, 1.3, gy, gy + 0.3, goblet, goblet)
                                + [cube((gx - 0.3, gy + 0.3, gz - 0.3), (gx + 0.3, gy + 2.6, gz + 0.3), faces(goblet, ALL6)),
                                   cube((gx - 0.6, gy + 1.2, gz - 0.6), (gx + 0.6, gy + 1.6, gz + 0.6), faces(goblet, ALL6))]
                                + disc(gx, gz, 1.0, gy + 2.6, gy + 3.0, goblet, goblet)
                                + disc(gx, gz, 1.35, gy + 3.0, gy + 5.4, wine, goblet, bottom=goblet))
    wx, wz = 12.5, 11.5
    pieces["setting_candlestick"] = (disc(wx, wz, 1.6, 1.5, 2.0, brass, brass)
                                     + disc(wx, wz, 1.0, 2.0, 2.5, brass, brass)
                                     + [cube((wx - 0.35, 2.5, wz - 0.35), (wx + 0.35, 5.6, wz + 0.35), faces(brass, ALL6)),
                                        cube((wx - 0.6, 3.6, wz - 0.6), (wx + 0.6, 4.1, wz + 0.6), faces(brass, ALL6))]
                                     + disc(wx, wz, 1.1, 5.6, 6.1, brass, brass)
                                     + [cube((wx - 0.55, 6.1, wz - 0.55), (wx + 0.55, 9.6, wz + 0.55), faces(wax, ALL6)),
                                        cube((wx + 0.55, 7.2, wz - 0.2), (wx + 0.75, 9.3, wz + 0.2), faces(wax, ALL6)),
                                        cube((wx - 0.06, 9.6, wz - 0.06), (wx + 0.06, 10.1, wz + 0.06), faces(wick, ALL6))])
    pieces["setting_flame"] = [plane_xy(wx - 0.8, wx + 0.8, 9.8, 12.2, wz, flame_), plane_zy(wz - 0.8, wz + 0.8, 9.8, 12.2, wx, flame_)]
    sx, sz = 10.5, 6.5
    pieces["setting_saucer"] = disc(sx, sz, 2.2, 1.5, 1.8, china, china_side) + disc(sx, sz, 1.6, 1.8, 1.9, china, china_side)
    pieces["setting_cup"] = (disc(sx, sz, 0.8, 1.9, 2.3, china, china_side)
                             + disc(sx, sz, 1.25, 2.3, 4.1, tea, china_side, bottom=china)
                             + [cube((sx + 1.2, 2.6, sz - 0.15), (sx + 1.9, 2.9, sz + 0.15), faces(china_side, ALL6)),
                                cube((sx + 1.7, 2.9, sz - 0.15), (sx + 1.95, 3.7, sz + 0.15), faces(china_side, ALL6)),
                                cube((sx + 1.2, 3.6, sz - 0.15), (sx + 1.9, 3.9, sz + 0.15), faces(china_side, ALL6))])
    tx, ty, tz = 5.0, 1.5, 6.0
    pieces["setting_teapot"] = (disc(tx, tz, 1.4, ty, ty + 0.4, china, china_side)
                                + disc(tx, tz, 2.4, ty + 0.4, ty + 3.2, china_side, china)
                                + disc(tx, tz, 1.8, ty + 3.2, ty + 3.8, china, china_side)
                                + [cube((tx - 0.6, ty + 3.8, tz - 0.6), (tx + 0.6, ty + 4.2, tz + 0.6), faces(china, ALL6)),
                                   cube((tx - 0.3, ty + 4.2, tz - 0.3), (tx + 0.3, ty + 4.6, tz + 0.3), faces(china, ALL6)),
                                   cube((tx + 2.2, ty + 1.0, tz - 0.4), (tx + 4.4, ty + 1.8, tz + 0.4), faces(china_side, ALL6),
                                        rotation((tx + 2.2, ty + 1.4, tz), "z", 45)),
                                   cube((tx - 3.4, ty + 2.6, tz - 0.25), (tx - 2.2, ty + 3.0, tz + 0.25), faces(china_side, ALL6)),
                                   cube((tx - 3.6, ty + 1.0, tz - 0.25), (tx - 3.2, ty + 3.0, tz + 0.25), faces(china_side, ALL6)),
                                   cube((tx - 3.4, ty + 0.8, tz - 0.25), (tx - 2.2, ty + 1.2, tz + 0.25), faces(china_side, ALL6))])
    px_, pz = 6.0, 8.0
    pieces["setting_platter"] = ([cube((1.6, 1.5, 3.6), (10.4, 1.9, 12.4), faces(pewter, ("up", "down"), north=pewter_side, south=pewter_side,
                                                                                 east=pewter_side, west=pewter_side)),
                                  cube((2.4, 1.5, 2.8), (9.6, 1.9, 13.2), faces(pewter, ("up", "down"), north=pewter_side, south=pewter_side,
                                                                                 east=pewter_side, west=pewter_side)),
                                  cube((3.0, 1.9, 4.8), (9.0, 4.6, 11.2), faces(roast, ALL6)),
                                  cube((3.6, 4.6, 5.6), (8.4, 5.4, 10.4), faces(roast, ALL6)),
                                  cube((4.2, 2.4, 10.8), (5.4, 3.6, 13.0), faces(roast, ALL6), rotation((4.8, 3.0, 11.0), "y", 22.5)),
                                  cube((6.6, 2.4, 10.8), (7.8, 3.6, 13.0), faces(roast, ALL6), rotation((7.2, 3.0, 11.0), "y", -22.5)),
                                  cube((4.5, 2.7, 12.8), (5.1, 3.3, 13.8), faces(bone, ALL6), rotation((4.8, 3.0, 11.0), "y", 22.5)),
                                  cube((6.9, 2.7, 12.8), (7.5, 3.3, 13.8), faces(bone, ALL6), rotation((7.2, 3.0, 11.0), "y", -22.5))]
                                 + [cube((x - 0.6, 1.9, z - 0.6), (x + 0.6, 2.6, z + 0.6), faces(garnish, ALL6))
                                    for x, z in ((2.4, 5.0), (9.6, 5.4), (2.6, 10.8), (9.4, 10.6), (2.2, 8.0), (9.8, 8.0))])
    for name, els in pieces.items():
        sc.models[name] = els
    sc.models["item"] = pieces["setting_plate"] + pieces["setting_cutlery"] + pieces["setting_goblet"] + pieces["setting_candlestick"]
    return sc


def roasted(seed=1):
    """A roast's glazed brown skin, darker in the creases and shining here and there."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(ROAST, 2 + rng.choice((0, 0, 1, -1, 1))))
        for _ in range(p.w * p.h // 10):
            p.put(rng.randrange(p.w), rng.randrange(p.h), ROAST[0])
        for _ in range(p.w * p.h // 14):
            p.put(rng.randrange(p.w), rng.randrange(p.h), ROAST[4])
    return paint


# The clock's dial centre (pixels), as GrandfatherClockRenderer turns the hands about it, and the pendulum's pivot.
DIAL = (8.0, 22.5, 2.4)
PENDULUM_PIVOT = (8.0, 15.0)


def clock_dial(seed=1):
    """The clock's dial plate: a brass plate with engraved corner spandrels round an ivory chapter ring, its hours in
    black strokes (twelve doubled), the minutes ticked round its rim, and a painted rose at its heart."""
    def paint(p):
        d17d.brass(seed)(p)
        w, h = p.w, p.h
        cx, cy = (w - 1) / 2, (h - 1) / 2
        r = min(w, h) / 2 - 0.5
        for y in range(h):
            for x in range(w):
                d = math.hypot(x - cx, y - cy)
                if d < r:
                    p.put(x, y, PORCELAIN[3] if d < r - 1.2 else IRON[3])
                elif d < r + 1.5:
                    p.put(x, y, BRASS[5])
        for corner in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
            for k in range(4):
                p.put(corner[0] + (k if corner[0] < cx else -k), corner[1], BRASS[1])
                p.put(corner[0], corner[1] + (k if corner[1] < cy else -k), BRASS[1])
        for m in range(60):
            a = math.radians(m * 6 - 90)
            p.put(cx + math.cos(a) * (r - 1.8), cy + math.sin(a) * (r - 1.8), IRON[1] if m % 5 else IRON[0])
        for hour in range(12):
            a = math.radians(hour * 30 - 90)
            for t in range(4):
                rr = r - 3.0 - t * 0.9
                p.put(cx + math.cos(a) * rr, cy + math.sin(a) * rr, IRON[0])
                if hour == 0:
                    p.put(cx + math.cos(a) * rr + 1, cy + math.sin(a) * rr, IRON[0])
        for a in range(0, 360, 60):
            for t in range(3):
                p.put(cx + math.cos(math.radians(a)) * t, cy + math.sin(math.radians(a)) * t, RED[4] if t < 2 else RED[2])
    return paint


def night_sky(seed=1):
    """The arch above the dial: a deep blue night with stars and a ribbon of cloud, a brass rim round it."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(MOON_SHADOW, 1 + (1 if y > p.h * 0.6 else 0) + rng.choice((0, 0, 0, -1))))
        for _ in range(p.w * p.h // 18):
            p.put(rng.randrange(p.w), rng.randrange(int(p.h * 0.8)), (230, 226, 200))
        for x in range(p.w):
            p.put(x, p.h - 1, BRASS[3])
            p.put(x, 0, BRASS[4])
    return paint


def moon_picture(phase, size=24, palette=MOON, face=False, background=None):
    """The moon in `phase` (0 full, 4 new, as the game counts them: 1 to 3 waning, lit on the left; 5 to 7 waxing, lit
    on the right) on a see-through ground: craters, a darker rim, the unlit part ashen blue; with `face`, a cute
    sleeping face (closed eyes, a little smile, rosy cheeks)."""
    lit_angle = {0: math.pi, 1: 0.75 * math.pi, 2: 0.5 * math.pi, 3: 0.25 * math.pi, 4: 0.0, 5: 0.25 * math.pi,
                 6: 0.5 * math.pi, 7: 0.75 * math.pi}[phase]
    waxing = phase >= 5
    img = Image.new("RGBA", (size, size), background or (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(40 + size)
    craters = [(rng.uniform(-0.7, 0.7), rng.uniform(-0.7, 0.7), rng.uniform(0.06, 0.2)) for _ in range(9)]
    c = (size - 1) / 2
    r = size / 2 - 0.4
    for y in range(size):
        for x in range(size):
            u, v = (x - c) / r, (y - c) / r
            d = math.hypot(u, v)
            if d > 1:
                continue
            s = math.sqrt(max(0.0, 1 - v * v))
            lit = (u if waxing else -u) > s * math.cos(lit_angle) if phase not in (0, 4) else phase == 0
            k = 4 - int(d * 2.2)
            for cu, cv, cr in craters:
                if math.hypot(u - cu, v - cv) < cr:
                    k -= 1
            if face:
                # A sleepy smile: closed eyes curving down, a little smile and rosy cheeks.
                for side in (-1, 1):
                    eu = u - side * 0.3
                    if abs(math.hypot(eu, v + 0.22) - 0.13) < 0.045 and v > -0.22:
                        k = 0
                    if math.hypot(eu * 0.8 - side * 0.06, (v - 0.12) * 1.4) < 0.1:
                        k = 99
                if abs(math.hypot(u, v - 0.12) - 0.2) < 0.045 and v > 0.2:
                    k = 0
            if k == 99:
                colour = (236, 140, 120) if lit else shade(MOON_SHADOW, 2)
            else:
                colour = shade(palette, k) if lit else shade(MOON_SHADOW, 1 + (1 if k > 2 else 0))
            px[x, y] = tuple(colour) + (255,)
    return img


def ghost_face(seed=1):
    """A ghost's face for the clock's case at midnight, cute and see-through: a smooth pale sheet, its outline a touch
    brighter, two big round dark eyes each with a glint, and a little round "oo" mouth."""
    def paint(p):
        cx = (p.w - 1) / 2
        for y in range(p.h):
            t = y / max(1, p.h - 1)
            half = p.w * 0.46 * (math.sin(math.pi * min(1.0, t * 1.4)) ** 0.5 if t < 0.7 else 1 - (t - 0.7) * 1.2)
            for x in range(p.w):
                d = abs(x - cx)
                if d < half:
                    p.put(x, y, (236, 244, 255) if d > half - 1.2 else (214, 230, 252), 170)
        for ex in (cx - p.w * 0.18, cx + p.w * 0.18):
            ca.ellipse(p, ex + 0.5, p.h * 0.38, p.w * 0.09, p.h * 0.075, (20, 24, 40))
            ca.ellipse(p, ex - p.w * 0.03 + 0.5, p.h * 0.36, 1.0, 1.0, ca.GLINT)
        ca.ellipse(p, cx + 0.5, p.h * 0.6, p.w * 0.06, p.h * 0.06, (20, 24, 40))
    return paint


@fa.quietly
def grandfather_clock():
    """The Grandfather Clock, two blocks tall and facing north: a moulded plinth; a trunk with a glazed door, the
    weights on their chains inside it; a hood with corner columns, the brass dial (its centre at DIAL) and above it an
    arch of night sky where the moon shows tonight's phase; a cornice and a broken pediment with brass finials. The
    hands, moon, pendulum and midnight ghost are the client's."""
    sc = Sculpt(d19.CLOCK["block"], 200, 128)
    case = sc.piece("case", 24, 24, d17d.wood(WALNUT, 2, panel=True))
    side = sc.piece("side", 16, 32, d17d.wood(WALNUT, 3))
    trim = sc.piece("trim", 32, 3, d17d.wood(WALNUT, 4))
    inside = sc.piece("inside", 16, 22, d17d.wood(DARK_WOOD, 5))
    glass_ = sc.piece("glass", 12, 16, d17d.glass(60))
    brass = sc.piece("brass", 6, 6, d17d.brass(8))
    chain = sc.piece("chain", 2, 12, d17d.chain_links())
    hand = sc.piece("hand", 2, 8, solid(IRON[:3], 9))
    rod = sc.piece("rod", 2, 16, d17d.brass(10))
    dial = sc.piece("dial", 44, 44, clock_dial(6))
    sky = sc.piece("sky", 36, 16, night_sky(7))
    ghost = sc.piece("ghost", 24, 30, ghost_face(12))
    bob = sc.piece("bob", 10, 10, d17d.brass(11))
    moons = [sc.piece(f"moon_{phase}", 14, 14, lambda p, phase=phase: p.img.alpha_composite(moon_picture(phase, 14)))
             for phase in range(8)]
    els = [cube((1.6, 0.0, 2.6), (14.4, 1.0, 13.4), faces(trim, ALL6)),
           cube((2.0, 1.0, 3.0), (14.0, 3.6, 13.0), faces(side, ("east", "west", "south", "down"), north=case, up=case)),
           cube((1.8, 3.6, 2.8), (14.2, 4.2, 13.2), faces(trim, ALL6)),
           # The trunk: its sides and back, open at the front behind the glazed door.
           cube((3.0, 4.2, 11.0), (13.0, 15.2, 12.0), faces(side, ("south", "up", "down", "east", "west"), north=inside)),
           cube((3.0, 4.2, 4.4), (4.0, 15.2, 11.0), faces(side, ("west", "up", "down"), east=inside)),
           cube((12.0, 4.2, 4.4), (13.0, 15.2, 11.0), faces(side, ("east", "up", "down"), west=inside)),
           cube((3.0, 4.2, 3.6), (5.0, 15.2, 4.4), faces(trim, ALL6)),
           cube((11.0, 4.2, 3.6), (13.0, 15.2, 4.4), faces(trim, ALL6)),
           cube((5.0, 4.2, 3.6), (11.0, 5.4, 4.4), faces(trim, ALL6)),
           cube((5.0, 13.8, 3.6), (11.0, 15.2, 4.4), faces(trim, ALL6)),
           cube((4.0, 4.2, 4.4), (12.0, 4.6, 11.0), faces(inside, ("up",))),
           cube((4.0, 14.8, 4.4), (12.0, 15.2, 11.0), faces(inside, ("down",))),
           plane_xy(5.0, 11.0, 5.4, 13.8, 4.0, glass_),
           cube((5.0, 10.6, 8.6), (6.4, 13.4, 10.0), faces(brass, ALL6)),
           cube((9.6, 11.4, 8.6), (11.0, 14.2, 10.0), faces(brass, ALL6)),
           plane_xy(5.4, 6.0, 13.4, 14.8, 9.3, chain), plane_xy(10.0, 10.6, 14.2, 14.8, 9.3, chain),
           cube((7.6, 14.2, 7.0), (8.4, 14.8, 10.6), faces(brass, ALL6)),
           # The waist and the hood.
           cube((2.6, 15.2, 3.2), (13.4, 16.0, 12.8), faces(trim, ALL6)),
           cube((2.0, 16.0, 2.6), (14.0, 17.0, 13.0), faces(trim, ALL6)),
           cube((2.4, 17.0, 3.0), (13.6, 30.0, 12.6), faces(side, ("east", "west", "south"), north=case)),
           cube((3.4, 17.9, 2.6), (12.6, 27.1, 3.0), faces(dial, ("north",), east=trim, west=trim, up=trim, down=trim)),
           cube((4.2, 27.1, 2.6), (11.8, 29.6, 3.0), faces(sky, ("north",), east=trim, west=trim, up=trim, down=trim)),
           cube((3.0, 27.1, 2.4), (4.2, 29.8, 3.0), faces(trim, ALL6)),
           cube((11.8, 27.1, 2.4), (13.0, 29.8, 3.0), faces(trim, ALL6))]
    for x in (2.4, 12.6):
        els += [cube((x, 17.0, 2.2), (x + 1.0, 17.6, 3.2), faces(brass, ALL6)),
                cube((x + 0.15, 17.6, 2.35), (x + 0.85, 29.4, 3.05), faces(side, ALL6)),
                cube((x, 29.4, 2.2), (x + 1.0, 30.0, 3.2), faces(brass, ALL6))]
    els += [cube((1.8, 30.0, 2.2), (14.2, 30.8, 13.4), faces(trim, ALL6)),
            cube((2.6, 30.8, 2.6), (6.6, 31.6, 3.4), faces(trim, ALL6), rotation((2.6, 30.8, 3.0), "z", 22.5)),
            cube((9.4, 30.8, 2.6), (13.4, 31.6, 3.4), faces(trim, ALL6), rotation((13.4, 30.8, 3.0), "z", -22.5)),
            cube((7.3, 30.8, 2.5), (8.7, 31.4, 3.5), faces(brass, ALL6)),
            cube((7.6, 31.4, 2.8), (8.4, 32.0, 3.2), faces(brass, ALL6)),
            # The corner finials stand 0.1 proud of the turned pediment pieces' backs (z 3.4), so they never share it.
            cube((2.0, 30.8, 2.4), (3.0, 31.6, 3.5), faces(brass, ALL6)),
            cube((13.0, 30.8, 2.4), (14.0, 31.6, 3.5), faces(brass, ALL6))]
    sc.models[d19.CLOCK["block"]] = els
    cx, cy, cz = DIAL
    sc.models["clock_hour_hand"] = [cube((cx - 0.5, cy - 0.5, cz - 0.15), (cx + 0.5, cy + 0.5, cz + 0.1), faces(brass, ALL6)),
                                    cube((cx - 0.2, cy - 0.9, cz - 0.05), (cx + 0.2, cy + 2.0, cz + 0.1), faces(hand, ALL6)),
                                    cube((cx - 0.4, cy + 1.6, cz - 0.05), (cx + 0.4, cy + 2.4, cz + 0.1), faces(hand, ALL6),
                                         rotation((cx, cy + 2.0, cz), "z", 45)),
                                    cube((cx - 0.1, cy + 2.4, cz - 0.05), (cx + 0.1, cy + 2.8, cz + 0.1), faces(hand, ALL6))]
    sc.models["clock_minute_hand"] = [cube((cx - 0.14, cy - 1.0, cz - 0.2), (cx + 0.14, cy + 3.6, cz - 0.08), faces(hand, ALL6)),
                                      cube((cx - 0.3, cy + 2.4, cz - 0.2), (cx + 0.3, cy + 3.0, cz - 0.08), faces(hand, ALL6),
                                           rotation((cx, cy + 2.7, cz), "z", 45))]
    for phase, uv in enumerate(moons):
        sc.models[f"clock_moon_{phase}"] = [plane_xy(7.0, 9.0, 27.4, 29.4, 2.55, uv)]
    px, py = PENDULUM_PIVOT
    sc.models["clock_pendulum"] = ([cube((px - 0.2, py - 7.0, 5.6), (px + 0.2, py, 6.0), faces(rod, ALL6)),
                                    cube((px - 0.5, py - 0.5, 5.5), (px + 0.5, py + 0.1, 6.1), faces(brass, ALL6))]
                                   + [cube((px - a, py - 8.4 - b, 5.3), (px + a, py - 8.4 + b, 6.3), faces(bob, ALL6))
                                      for a, b in ((1.7, 1.2), (1.2, 1.7))])
    sc.models["clock_ghost_face"] = [plane_xy(5.2, 10.8, 6.0, 13.6, 4.6, ghost)]
    sc.models["item"] = els
    return sc


# ---------------------------------------------------------------- 14. the witchlight lantern path

# Where each lamp's lantern stands (the lantern's foot, y, and its centre, x and z), as WitchlightRenderer finds its
# centre five pixels up from the base's foot... centre y = foot + 3: on a stake, hanging, and on a lamp-post (pixels from
# the post's lower half).
LANTERN = {"witchlight_path_stake": (8.0, 7.0, 8.0), "hanging_witchlight": (8.0, 4.0, 8.0), "witchlight_lamp_post": (8.0, 19.5, 3.5)}


def leaded_glass(colour, awake, seed=1):
    """A witchlight's pane: tinted glass in a diamond lattice of lead, faint asleep and bright awake."""
    def paint(p):
        rng = random.Random(seed)
        r, g, b = colour
        lift = 1.0 if awake else 0.55
        for y in range(p.h):
            for x in range(p.w):
                k = lift * (0.85 + rng.random() * 0.15)
                p.put(x, y, (min(255, int(r * k + (40 if awake else 0))), min(255, int(g * k + (40 if awake else 0))),
                             min(255, int(b * k + (40 if awake else 0)))), 225 if awake else 165)
        for y in range(p.h):
            for x in range(p.w):
                if (x + y) % 6 == 0 or (x - y) % 6 == 0:
                    p.put(x, y, IRON[2], 255)
        for x in range(p.w):
            p.put(x, 0, IRON[3], 255)
            p.put(x, p.h - 1, IRON[1], 255)
    return paint


def lantern(sc, kind, colour, awake):
    """A witchlight lantern of `kind` (its foot and centre in LANTERN): a pierced iron base, four corner posts, glass
    panes of `colour`, and a gothic peaked cap stepping up to a finial or, hung, a ring."""
    cx, y0, cz = LANTERN[kind]
    iron = sc.atlas.uvs["iron"]
    cap = sc.atlas.uvs["cap"]
    pane = sc.piece(f"glass_{colour}{'_awake' if awake else ''}", 8, 10,
                    leaded_glass(NIGHT_GLASS[colour], awake, 3 + d19.WITCHLIGHT_COLOURS.index(colour)))
    light = d19.WITCHLIGHT["awake"] if awake else None
    els = [cube((cx - 2.4, y0, cz - 2.4), (cx + 2.4, y0 + 0.6, cz + 2.4), faces(iron, ALL6)),
           cube((cx - 2.0, y0 + 0.6, cz - 2.0), (cx + 2.0, y0 + 1.0, cz + 2.0), faces(iron, ALL6)),
           cube((cx - 1.8, y0 + 1.0, cz - 1.8), (cx + 1.8, y0 + 5.0, cz + 1.8), faces(pane, SIDES4), light=light)]
    for dx in (-2.1, 1.6):
        for dz in (-2.1, 1.6):
            els.append(cube((cx + dx, y0 + 1.0, cz + dz), (cx + dx + 0.5, y0 + 5.0, cz + dz + 0.5), faces(iron, ALL6)))
    els += [cube((cx - 2.5, y0 + 5.0, cz - 2.5), (cx + 2.5, y0 + 5.5, cz + 2.5), faces(cap, ALL6)),
            cube((cx - 1.9, y0 + 5.5, cz - 1.9), (cx + 1.9, y0 + 6.0, cz + 1.9), faces(cap, ALL6)),
            cube((cx - 1.2, y0 + 6.0, cz - 1.2), (cx + 1.2, y0 + 6.5, cz + 1.2), faces(cap, ALL6)),
            cube((cx - 0.5, y0 + 6.5, cz - 0.5), (cx + 0.5, y0 + 7.0, cz + 0.5), faces(cap, ALL6))]
    for angle in (-45, 45):
        els.append(cube((cx - 2.4, y0 + 5.0, cz - 0.2), (cx + 2.4, y0 + 5.7, cz + 0.2), faces(cap, ALL6), rotation((cx, y0 + 5.3, cz), "y", angle)))
    return els


def fluted_iron(seed=1):
    """A fluted cast-iron post: dark grooves and lit ribs running up it."""
    def paint(p):
        d17d.cast_iron(seed)(p)
        for x in range(p.w):
            for y in range(p.h):
                if x % 2 == 0:
                    p.put(x, y, d17d.POT[5] if x < p.w / 2 else d17d.POT[4])
                else:
                    p.put(x, y, d17d.POT[1])
    return paint


def witchlight(kind):
    """A witchlight `kind` (tools/decor19.py WITCHLIGHTS) in every glass colour, asleep and awake: the path stake, a
    twisted iron spike with a curl; the hanging lantern on its chain; the lamp-post, a fluted cast-iron post on a stone
    foot whose crook arm hangs the lantern out in front."""
    sc = Sculpt(kind, 201 + list(d19.WITCHLIGHTS).index(kind), 64)
    sc.piece("iron", 8, 8, d17d.cast_iron(2))
    sc.piece("cap", 10, 6, d17d.cast_iron(3, ridge=(0,)))
    twisted = sc.piece("twisted", 2, 16, d17d.twisted(4))
    chain = sc.piece("chain", 2, 12, d17d.chain_links())
    curl = sc.piece("curl", 12, 12, d17d.scroll("bracket", 5))
    stone = sc.piece("stone", 12, 12, d18d.stone(d18d.STONES["stone_brick"], 6, tooled=True))
    fluted = sc.piece("fluted", 4, 16, fluted_iron(7))
    iron = sc.atlas.uvs["iron"]
    cx, y0, cz = LANTERN[kind]
    if kind == "witchlight_path_stake":
        base = [cube((7.6, 1.0, 7.6), (8.4, 7.0, 8.4), faces(twisted, ALL6)),
                cube((7.8, 0.0, 7.8), (8.2, 1.0, 8.2), faces(iron, ALL6)),
                cube((7.0, 6.2, 7.0), (9.0, 7.0, 9.0), faces(iron, ALL6)),
                plane_zy(8.4, 12.4, 1.6, 5.6, 8.0, curl), plane_zy(3.6, 7.6, 1.6, 5.6, 8.0, (curl[2], curl[1], curl[0], curl[3]))]
    elif kind == "hanging_witchlight":
        base = [cube((cx - 0.6, y0 + 7.0, cz - 0.6), (cx + 0.6, y0 + 7.4, cz + 0.6), faces(iron, ALL6)),
                plane_xy(cx - 0.5, cx + 0.5, y0 + 7.4, 16.0, cz, chain), plane_zy(cz - 0.5, cz + 0.5, y0 + 7.4, 16.0, cx, chain),
                cube((cx - 1.2, 15.6, cz - 1.2), (cx + 1.2, 16.0, cz + 1.2), faces(iron, ALL6)),
                cube((cx - 0.6, y0 - 1.2, cz - 0.6), (cx + 0.6, y0, cz + 0.6), faces(iron, ALL6)),
                cube((cx - 0.25, y0 - 2.0, cz - 0.25), (cx + 0.25, y0 - 1.2, cz + 0.25), faces(iron, ALL6))]
    else:
        base = [cube((5.0, 0.0, 5.0), (11.0, 1.6, 11.0), faces(stone, ALL6)),
                cube((5.8, 1.6, 5.8), (10.2, 2.4, 10.2), faces(iron, ALL6)),
                cube((6.4, 2.4, 6.4), (9.6, 4.0, 9.6), faces(iron, ALL6)),
                cube((7.1, 4.0, 7.1), (8.9, 28.0, 8.9), faces(fluted, SIDES4, up=iron, down=iron))]
        for y in (10.0, 20.0, 26.0):
            base.append(cube((6.8, y, 6.8), (9.2, y + 0.6, 9.2), faces(iron, ALL6)))
        base += [cube((7.4, 28.0, 7.4), (8.6, 29.4, 8.6), faces(iron, ALL6)),
                 cube((7.75, 29.4, 7.75), (8.25, 30.6, 8.25), faces(iron, ALL6)),
                 cube((7.6, 26.6, 2.9), (8.4, 27.4, 7.1), faces(iron, ALL6)),
                 cube((7.5, 26.4, 2.6), (8.5, 27.6, 3.4), faces(iron, ALL6)),
                 plane_zy(3.6, 7.1, 27.4, 30.2, 8.0, (curl[2], curl[3], curl[0], curl[1])),
                 cube((cx - 0.15, y0 + 7.0, cz - 0.15), (cx + 0.15, 26.6, cz + 0.15), faces(iron, ALL6)),
                 cube((cx - 0.6, y0 - 1.0, cz - 0.6), (cx + 0.6, y0, cz + 0.6), faces(iron, ALL6))]
    for colour in d19.WITCHLIGHT_COLOURS:
        for awake in (False, True):
            sc.models[f"{kind}_{colour}{'_awake' if awake else ''}"] = base + lantern(sc, kind, colour, awake)
    sc.models["item"] = sc.models[f"{kind}_purple_awake"]
    return sc


def witchlight_glow():
    """What burns in a witchlight, modelled about the origin (pixels) for WitchlightRenderer: a soft glow, three crossed
    discs, and a wisp, two crossed flickers; both white, for the client to colour."""
    glow = (0.0, 0.0, 16.0, 16.0)
    out = {"witchlight_glow": [plane_xy(-3.6, 3.6, -3.6, 3.6, 0.0, glow), plane_zy(-3.6, 3.6, -3.6, 3.6, 0.0, glow),
                               plane_xz(-3.6, 3.6, -3.6, 3.6, 0.0, glow)],
           "witchlight_wisp": [plane_xy(-0.9, 0.9, -1.4, 1.6, 0.0, glow), plane_zy(-0.9, 0.9, -1.4, 1.6, 0.0, glow)]}
    return out


# ---------------------------------------------------------------- 15. the yard silhouettes and the harvest moon

# Each figure's box in its block (x0, x1, y0, y1, pixels; it stands on a stake at x 8), how tall its stake is, and where
# its eyes are (pixels across and down its picture). The picture is painted SCALE texels to the pixel.
SCALE = 2.5
FIGURE_BOXES = {"arched_cat": ((2.0, 14.0, 1.0, 13.0), 3.0, [(1.8, 4.9), (2.6, 4.9)]),
                "prowling_cat": ((-2.0, 18.0, 1.0, 9.0), 3.0, [(2.5, 3.3), (3.6, 3.3)]),
                "witch": ((-4.0, 20.0, 10.0, 26.0), 17.4, [(9.5, 2.9), (15.85, 5.5), (16.3, 5.5)]),
                "bats": ((1.0, 15.0, 6.0, 24.0), 6.2, [(3.8, 3.3), (4.25, 3.3), (10.15, 6.0), (10.65, 6.0), (5.4, 10.3), (5.8, 10.3)]),
                "wolf": ((2.0, 14.0, 1.0, 21.0), 4.0, [(4.7, 3.2)]),
                "crow": ((3.0, 13.0, 1.0, 15.0), 4.0, [(2.7, 4.6)])}


def figure(name):
    """A figure's picture, painted black on see-through at SCALE texels to the pixel, drawn in its own pixels (x right,
    y down from its box's top left)."""
    (x0, x1, y0, y1), _, _ = FIGURE_BOXES[name]
    w, h = x1 - x0, y1 - y0
    img = Image.new("RGBA", (int(w * SCALE), int(h * SCALE)), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    ink = SILHOUETTE + (255,)

    def P(pts):
        return [(x * SCALE, y * SCALE) for x, y in pts]

    def poly(*pts):
        draw.polygon(P(pts), fill=ink)

    def ell(xa, ya, xb, yb):
        draw.ellipse(P([(xa, ya), (xb, yb)]), fill=ink)

    def rect(xa, ya, xb, yb):
        draw.rectangle(P([(xa, ya), (xb, yb)]), fill=ink)

    def line(pts, width):
        draw.line(P(pts), fill=ink, width=max(1, int(width * SCALE)))

    def tail(p0, p1, p2, r0, r1, steps=40):
        for i in range(steps + 1):
            t = i / steps
            x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
            y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
            r = r0 + (r1 - r0) * t
            ell(x - r, y - r, x + r, y + r)

    def bat(cx, cy, s):
        for side in (-1, 1):
            poly((cx, cy - 0.3 * s), (cx + side * 1.4 * s, cy - 1.0 * s), (cx + side * 2.6 * s, cy - 1.1 * s),
                 (cx + side * 3.3 * s, cy - 0.2 * s), (cx + side * 2.6 * s, cy + 0.1 * s), (cx + side * 2.2 * s, cy + 0.7 * s),
                 (cx + side * 1.5 * s, cy + 0.3 * s), (cx + side * 1.0 * s, cy + 0.8 * s), (cx, cy + 0.4 * s))
            poly((cx + side * 0.15 * s, cy - 0.7 * s), (cx + side * 0.45 * s, cy - 1.4 * s), (cx + side * 0.55 * s, cy - 0.6 * s))
        ell(cx - 0.55 * s, cy - 0.9 * s, cx + 0.55 * s, cy + 0.9 * s)

    if name == "arched_cat":
        poly((2.6, 7.4), (3.1, 5.4), (4.0, 4.0), (5.4, 3.3), (6.8, 3.6), (7.9, 4.8), (8.6, 6.6), (8.9, 7.9),
             (8.2, 7.4), (7.2, 6.0), (5.8, 5.4), (4.6, 5.8), (3.6, 7.0), (3.4, 7.9))
        for xa, xb, ya in ((2.6, 3.4, 7.2), (3.3, 4.0, 7.2), (7.9, 8.7, 7.4), (8.4, 9.2, 7.4)):
            rect(xa, ya, xb, 11.0)
        ell(1.05, 3.85, 3.35, 6.15)
        poly((1.2, 4.6), (1.3, 3.0), (2.1, 4.0))
        poly((2.4, 3.9), (3.1, 2.9), (3.2, 4.6))
        tail((8.7, 6.6), (10.8, 4.6), (9.9, 1.6), 0.5, 0.32)
        tail((9.9, 1.6), (9.6, 0.8), (8.9, 1.2), 0.32, 0.25, 12)
        for k in range(6):
            poly((4.2 + k * 0.7, 3.9 - 0.3 * math.sin(k)), (4.5 + k * 0.7, 3.0 + 0.2 * (k % 2)), (4.8 + k * 0.7, 3.8))
        rect(0.0, 11.0, 12.0, 12.0)
        for x in (0.6, 11.0):
            rect(x, 10.4, x + 0.4, 11.0)
    elif name == "prowling_cat":
        ell(5.0, 2.6, 15.0, 5.4)
        ell(4.0, 2.0, 8.0, 5.0)
        ell(12.0, 2.2, 15.6, 5.2)
        ell(1.6, 2.2, 4.6, 4.8)
        poly((1.9, 2.6), (2.2, 1.0), (3.0, 2.3))
        poly((3.1, 2.3), (3.8, 1.0), (4.1, 2.7))
        poly((4.6, 4.2), (5.6, 4.2), (4.2, 7.4), (3.4, 7.4))
        poly((6.4, 4.4), (7.2, 4.4), (7.0, 7.4), (6.2, 7.4))
        poly((12.6, 4.6), (13.6, 4.6), (13.2, 7.4), (12.4, 7.4))
        poly((14.4, 4.4), (15.4, 4.4), (16.0, 7.4), (15.2, 7.4))
        tail((15.2, 3.4), (18.8, 3.0), (19.3, 0.8), 0.45, 0.25)
        rect(0.0, 7.4, 20.0, 8.0)
    elif name == "witch":
        line([(0.6, 9.8), (19.0, 7.6)], 0.55)
        poly((17.6, 7.0), (23.6, 4.6), (24.0, 6.6), (23.4, 9.8), (18.0, 8.6))
        for k in range(5):
            line([(18.4, 7.6 + k * 0.2), (23.8, 4.9 + k * 1.2)], 0.25)
        poly((8.0, 9.4), (9.0, 5.4), (10.6, 3.8), (12.6, 4.2), (13.6, 6.2), (15.6, 9.8), (13.8, 10.6), (12.2, 9.6), (10.2, 10.8))
        poly((11.6, 4.0), (16.4, 3.0), (19.2, 4.6), (17.2, 5.6), (14.0, 6.0))
        ell(8.8, 2.2, 10.8, 4.2)
        poly((9.0, 3.0), (7.6, 3.6), (9.0, 3.7))
        poly((9.1, 3.8), (8.5, 4.6), (9.6, 4.1))
        ell(7.6, 1.9, 12.6, 2.7)
        poly((8.8, 2.3), (11.4, 2.3), (11.0, 1.0), (12.8, 0.1), (10.4, 0.7), (9.8, 1.4))
        poly((9.4, 5.4), (8.0, 8.2), (8.6, 8.6), (10.2, 6.0))
        poly((10.6, 9.4), (11.6, 9.4), (11.0, 12.0), (9.4, 12.7), (9.2, 12.2), (10.4, 11.6))
        for k in range(4):
            line([(10.6, 3.0 + k * 0.3), (12.8 + k * 0.4, 4.2 + k * 0.5)], 0.22)
        ell(15.4, 6.0, 16.7, 7.9)
        ell(15.5, 4.9, 16.6, 6.0)
        poly((15.5, 5.3), (15.6, 4.5), (16.0, 5.0))
        poly((16.1, 5.0), (16.5, 4.5), (16.6, 5.3))
        tail((16.6, 7.6), (17.6, 7.4), (17.4, 6.0), 0.2, 0.15, 10)
    elif name == "bats":
        for cx, cy, s in ((4.0, 3.6, 1.0), (10.4, 6.3, 1.15), (5.6, 10.6, 0.9)):
            bat(cx, cy, s)
            line([(cx, cy + 0.5 * s), (7.0, 18.0)], 0.22)
    elif name == "wolf":
        poly((0.4, 20.0), (1.2, 16.6), (3.6, 15.4), (8.4, 15.0), (11.0, 16.4), (11.8, 20.0))
        ell(5.6, 10.2, 10.4, 16.4)
        poly((4.4, 6.8), (6.4, 6.2), (7.6, 9.0), (7.4, 15.8), (5.0, 15.8), (4.2, 11.0))
        rect(4.6, 12.0, 5.6, 16.0)
        rect(5.8, 12.0, 6.8, 16.0)
        poly((4.4, 7.4), (4.2, 4.2), (5.6, 3.4), (6.8, 5.2), (6.6, 7.6))
        poly((3.4, 4.6), (3.8, 2.6), (4.8, 2.0), (5.8, 2.6), (6.0, 3.8), (5.2, 4.6))
        poly((3.8, 2.6), (2.4, 0.6), (3.0, 0.4), (4.6, 2.0))
        poly((3.5, 3.4), (2.0, 1.9), (2.4, 1.6), (3.9, 2.9))
        poly((5.0, 2.2), (5.6, 0.8), (6.0, 2.4))
        for k in range(5):
            poly((6.2 + k * 0.25, 3.6 + k * 0.9), (7.4 + k * 0.3, 3.9 + k * 0.9), (6.6 + k * 0.25, 4.5 + k * 0.9))
        tail((10.0, 14.6), (12.2, 15.6), (11.0, 17.8), 0.75, 0.5)
    elif name == "crow":
        poly((3.6, 14.0), (3.6, 10.2), (5.1, 9.4), (6.6, 10.2), (6.6, 14.0))
        ell(2.6, 5.2, 7.6, 9.6)
        ell(1.8, 3.6, 4.2, 6.0)
        poly((1.9, 4.6), (0.2, 5.0), (1.9, 5.4))
        poly((6.8, 7.6), (9.8, 9.0), (9.6, 9.8), (6.4, 9.0))
        poly((5.0, 5.8), (8.2, 6.6), (7.0, 7.8))
        for x in (4.2, 5.4):
            line([(x, 9.2), (x - 0.2, 10.0)], 0.25)
    return img


def eye_texture():
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if d < 1:
                    p.put(x, y, EYE_YELLOW if d > 0.35 or x != int(cx) else (40, 20, 0))
    return paint


def yard_silhouette():
    """The Yard Silhouette's figures, each a black plywood cut-out (one sheet, drawn from both sides) on a weathered
    stake, facing north, with its eyes as separate sheets just in front and behind for the client to light at night.
    The block model only gives particles; the item is the arched cat."""
    sc = Sculpt(d19.SILHOUETTE["block"], 207, 128)
    order = ["wolf", "bats", "crow", "arched_cat", "witch", "prowling_cat"]
    pictures = {}
    for name in order:
        picture = figure(name)
        pictures[name] = sc.piece(name, picture.width, picture.height, lambda p, picture=picture: p.img.alpha_composite(picture))
    stake = sc.piece("stake", 4, 20, d17d.wood(pal("2a241c", "3a3228", "4c4234", "5e5242", "706452"), 2))
    eye = sc.piece("eye", 6, 4, eye_texture())
    for name, ((x0, x1, y0, y1), top, eyes) in FIGURE_BOXES.items():
        u0, v0, u1, v1 = pictures[name]
        sc.models[f"silhouette_{name}"] = [plane_xy(x0, x1, y0, y1, 8.0, (u0, v0, u1, v1)),
                                           cube((7.6, 0.0, 8.1), (8.4, top, 8.7), faces(stake, ALL6)),
                                           cube((7.75, -0.6, 8.25), (8.25, 0.0, 8.55), faces(stake, ALL6))]
        size = 0.55 if name == "bats" else 0.85
        planes = []
        for ex, ey in eyes:
            x, y = x0 + ex, y1 - ey
            for z in (7.94, 8.06):
                planes.append(plane_xy(x - size / 2, x + size / 2, y - size * 0.3, y + size * 0.3, z, eye))
        sc.models[f"silhouette_{name}_eyes"] = planes
    sc.models["item"] = [e for e in sc.models["silhouette_arched_cat"] if e["from"][1] >= 0.0]
    return sc


# The Harvest Moon Lamp: its moon's centre and radius (pixels, in the frame of its first block, the second column to
# the west), and its iron ring's.
MOON_CENTRE = (0.0, 17.0)
MOON_RADIUS = 13.4
RING_RADIUS = 14.0


def harvest_moon_lamp():
    """The Harvest Moon Lamp, two blocks by two facing north: a ring of wrought iron sixteen bars round, on two short
    posts on a long footed base, a garland of autumn leaves over its lower arc and a pumpkin at each foot. Each block
    has its own quarter (models harvest_moon_lamp_0 to _3, by part); the moon itself is the client's."""
    sc = Sculpt(d19.MOON["block"], 208, 64)
    bar = sc.piece("bar", 4, 12, d17d.wrought_clean())
    flat = sc.piece("flat", 12, 4, d17d.wrought_clean(horizontal=True))
    iron = sc.piece("iron", 8, 8, d17d.cast_iron(4))
    leaves = sc.piece("leaves", 16, 8, leaf_garland(5))
    pumpkin = sc.piece("pumpkin", 8, 8, solid(pal("8a3a08", "b04e0c", "d06a14", "e88a28"), 6, rim=True))
    stem = sc.piece("stem", 2, 2, solid(pal("3a2a10", "4e3a18"), 7))
    mx, my = MOON_CENTRE
    length = 2 * RING_RADIUS * math.tan(math.radians(11.25)) + 0.2
    master = []  # (element in the first block's frame)
    for k in range(16):
        theta = k * 22.5
        x = mx + RING_RADIUS * math.cos(math.radians(theta))
        y = my + RING_RADIUS * math.sin(math.radians(theta))
        tangent = (theta + 90) % 180
        if tangent == 0:
            master.append(cube((x - length / 2, y - 0.8, 7.0), (x + length / 2, y + 0.8, 9.0), faces(flat, ALL6)))
        elif tangent == 90:
            master.append(cube((x - 0.8, y - length / 2, 7.0), (x + 0.8, y + length / 2, 9.0), faces(bar, ALL6)))
        elif tangent in (22.5, 45):
            master.append(cube((x - length / 2, y - 0.8, 7.0), (x + length / 2, y + 0.8, 9.0), faces(flat, ALL6),
                               rotation((x, y, 8.0), "z", tangent)))
        elif tangent in (135, 157.5):
            master.append(cube((x - length / 2, y - 0.8, 7.0), (x + length / 2, y + 0.8, 9.0), faces(flat, ALL6),
                               rotation((x, y, 8.0), "z", tangent - 180)))
        else:  # 67.5 or 112.5: an upright bar leaning
            master.append(cube((x - 0.8, y - length / 2, 7.0), (x + 0.8, y + length / 2, 9.0), faces(bar, ALL6),
                               rotation((x, y, 8.0), "z", tangent - 90)))
    master += [cube((-12.0, 0.0, 5.4), (12.0, 1.2, 10.6), faces(iron, ALL6)),
               cube((-13.0, 0.0, 4.6), (-10.6, 0.8, 11.4), faces(iron, ALL6)),
               cube((10.6, 0.0, 4.6), (13.0, 0.8, 11.4), faces(iron, ALL6)),
               cube((-5.6, 1.2, 7.2), (-4.4, 4.4, 8.8), faces(bar, ALL6)),
               cube((4.4, 1.2, 7.2), (5.6, 4.4, 8.8), faces(bar, ALL6)),
               cube((-0.6, 1.2, 7.2), (0.6, 3.2, 8.8), faces(bar, ALL6))]
    for x in (-8.6, 8.6):
        master += [cube((x - 1.6, 1.2, 6.4), (x + 1.6, 3.6, 9.6), faces(pumpkin, ALL6)),
                   cube((x - 1.1, 1.0, 5.9), (x + 1.1, 3.8, 10.1), faces(pumpkin, ALL6)),
                   cube((x - 0.25, 3.6, 7.75), (x + 0.25, 4.4, 8.25), faces(stem, ALL6))]
    for k, (x, y, a) in enumerate(((-9.6, 7.0, 45), (-5.2, 4.4, 22.5), (0.0, 3.6, 0), (5.2, 4.4, -22.5), (9.6, 7.0, -45))):
        master.append(plane_xy(x - 3.0, x + 3.0, y - 1.4, y + 1.4, 6.8, leaves, rotation((x, y, 6.8), "z", a) if a else None))
    for part, (dx, dy) in enumerate(((0, 0), (16, 0), (0, -16), (16, -16))):
        mine = []
        for e in master:
            centre = [(e["from"][i] + e["to"][i]) / 2 for i in range(2)]
            if "rotation" in e:
                centre = e["rotation"]["origin"][:2]
            col = 0 if centre[0] >= 0 else 1
            row = 0 if centre[1] < 16 else 1
            whole_x = e["from"][0] < 0 < e["to"][0] and "rotation" not in e
            whole_y = e["from"][1] < 16 < e["to"][1] and "rotation" not in e
            if (col, row) == (part % 2, part // 2) or (whole_x and row == part // 2) or (whole_y and col == part % 2):
                piece = d18d.shifted([e], dx, dy, 0)[0]
                lo, hi = list(piece["from"]), list(piece["to"])
                if whole_x or whole_y:
                    lo = [max(lo[0], 0.0), max(lo[1], 0.0), lo[2]]
                    hi = [min(hi[0], 16.0), min(hi[1], 16.0), hi[2]]
                    if lo[0] >= hi[0] or lo[1] >= hi[1]:
                        continue
                mine.append(dict(piece, **{"from": lo, "to": hi}))
        sc.models[f"harvest_moon_lamp_{part}"] = mine
    sc.models["item"] = d18d.shifted(master, 8.0, 0.0, 0.0)
    return sc


def leaf_garland(seed=1):
    """A garland of autumn leaves, red, orange and gold, overlapping on a twine."""
    def paint(p):
        rng = random.Random(seed)
        colours = [pal("6a1a0a", "9a2a10", "c43e16"), pal("8a3a08", "c05a10", "e8801c"), pal("8a6a10", "c09a1c", "e8c040")]
        for x in range(p.w):
            p.put(x, p.h // 2, d17d.CORD[2])
        for _ in range(p.w // 2 + 4):
            cx, cy = rng.uniform(0, p.w), rng.uniform(1, p.h - 2)
            colour = rng.choice(colours)
            r = rng.uniform(1.2, 2.2)
            for y in range(p.h):
                for x in range(p.w):
                    if math.hypot((x - cx) / 1.3, y - cy) < r:
                        p.put(x, y, colour[1 if (x + y) % 3 else 2])
            p.put(cx, cy, colour[0])
    return paint


def harvest_moon_faces():
    """The moon of the Harvest Moon Lamp, one quad model a phase for HarvestMoonLampRenderer: its face front and back
    (the front facing north) on entity/harvest_moon_<phase>, cut out round."""
    mx, my = MOON_CENTRE
    r = MOON_RADIUS
    face = [{"from": [mx - r, my - r, 7.4], "to": [mx + r, my + r, 7.4], "faces": {"north": {"texture": "#m", "uv": [0, 0, 16, 16]}}},
            {"from": [mx - r, my - r, 8.6], "to": [mx + r, my + r, 8.6], "faces": {"south": {"texture": "#m", "uv": [0, 0, 16, 16]}}}]
    return {f"harvest_moon_face_{phase}": [dict(q, cutout=True) for q in quads(face, {"m": f"entity/harvest_moon_{phase}"})]
            for phase in range(8)}


def moon_texture(phase):
    return moon_picture(phase, 64, face=True)


def lab_eye_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glow_disc((200, 255, 120), 255)(Px(img))
    return img


def white_glow_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glow_disc((255, 255, 255), 255)(Px(img))
    return img


def wisp_image():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wisp_texture()(Px(img))
    return img


# ---------------------------------------------------------------- building, quads and files

_built = {}


def build(name):
    """The Sculpt a block's models are drawn from (by its block's ID, or "spiderling")."""
    if name not in _built:
        builders = {d19.HARNESS["block"]: lightning_harness, d19.CONSOLE["block"]: brain_vat_console, d19.HAND["block"]: crawling_hand,
                    d19.COCOON["block"]: silk_cocoon, d19.EGG_SACS["block"]: egg_sac_cluster, d19.DRAPE["block"]: web_drape,
                    d19.SPOOLS["block"]: silk_spool_stack, d19.CHAIR["block"]: haunted_dining_chair,
                    d19.SETTING["block"]: floating_table_setting, d19.CLOCK["block"]: grandfather_clock,
                    **{kind: (lambda kind=kind: witchlight(kind)) for kind in d19.WITCHLIGHTS},
                    d19.SILHOUETTE["block"]: yard_silhouette, d19.MOON["block"]: harvest_moon_lamp, "spiderling": spiderling_sculpt}
        _built[name] = builders[name]()
    return _built[name]


def decor19_quads():
    """The renderer's models: everything that moves, sways, bobs, glows or is drawn whole by the client."""
    out = {}

    def add(name, block, model=None, **flags):
        sc = build(block)
        elements = sc.models[model or name]
        # Cut-out and see-through parts are drawn from both sides, so a sheet keeps one face (no reversed twin to fight).
        if flags.get("cutout") or flags.get("nocull"):
            elements = d17d.single_sheets(elements)
        out[name] = [dict(q, **flags) for q in quads(elements, {"p": block})]

    hand = d19.HAND["block"]
    add(hand, hand, cutout=True)
    for i in range(len(KNUCKLES)):
        add(f"{hand}_finger_{i}", hand, cutout=True)
    out["lab_table_arms"] = quads(lab_table_arms(), {"sheet": "lab_table_sheet", "skin": "lab_table_skin"})
    out["lab_table_eyes"] = [dict(q, nocull=True) for q in quads(d17d.single_sheets(lab_table_eyes()), {"eye": "entity/lab_eye_glow"})]
    add("silk_cocoon", d19.COCOON["block"], cutout=True)
    out["spiderling"] = [dict(q, cutout=True) for q in quads(d17d.single_sheets(build("spiderling").models["spiderling"]), {"p": "entity/spiderling"})]
    for i in range(len(SPOOL_X)):
        add(f"silk_spool_silk_{i}", d19.SPOOLS["block"])
    add("haunted_dining_chair", d19.CHAIR["block"], cutout=True)
    for name in ("setting_plate", "setting_cutlery", "setting_goblet", "setting_candlestick", "setting_flame", "setting_saucer",
                 "setting_cup", "setting_teapot", "setting_platter"):
        add(name, d19.SETTING["block"], cutout=True)
    clock = d19.CLOCK["block"]
    for name in ["clock_hour_hand", "clock_minute_hand", "clock_pendulum"] + [f"clock_moon_{p}" for p in range(8)]:
        add(name, clock, cutout=True)
    add("clock_ghost_face", clock, nocull=True)
    for name, els in witchlight_glow().items():
        out[name] = [dict(q, nocull=True) for q in quads(d17d.single_sheets(els), {"p": f"entity/{name}"})]
    for figure_ in d19.FIGURES:
        add(f"silhouette_{figure_}", d19.SILHOUETTE["block"], cutout=True)
        add(f"silhouette_{figure_}_eyes", d19.SILHOUETTE["block"], cutout=True)
    out.update(harvest_moon_faces())
    return out


SMALL_ITEM = d18d.SMALL_ITEM
TALL_ITEM = d18d.TALL_ITEM
MOON_ITEM = {"gui": {"rotation": [25, 225, 0], "translation": [0, -2, 0], "scale": [0.32, 0.32, 0.32]},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
             "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.32, 0.32, 0.32]},
             "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [0.4, 0.4, 0.4]},
             "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]}}
# The egg sacs on each face, as glow lichen's are: the model is drawn on the north face.
MULTIFACE = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


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
    lang.update({f"block.{MOD}.{block}": display for block, display in d19.names().items()})
    lang.update(d19.MESSAGES)

    def model(name, block, elements=None):
        sc = build(block)
        write(models / f"{name}.json", fa.model(block, elements if elements is not None else sc.models[name]))

    def particle(name, texture):
        write(models / f"{name}.json", {"textures": {"particle": rid(f"block/{texture}")}})

    def turned(model_name, facing, **extra):
        return {"model": rid(f"block/{model_name}"), **({"y": FACINGS[facing]} if FACINGS[facing] else {}), **extra}

    def item(block, elements, display):
        write(items / f"{block}.json", fa.model(block, elements, display))

    booleans = ("false", "true")
    # 11. The reanimation rig.
    harness = d19.HARNESS["block"]
    model(harness, harness)
    write(states / f"{harness}.json", {"variants": {f"powered={p}": {"model": rid(f"block/{harness}")} for p in booleans}})
    item(harness, build(harness).models[harness], fa.PLANT_DISPLAY)
    console = d19.CONSOLE["block"]
    model(console, console)
    model(f"{console}_lit", console)
    write(states / f"{console}.json", {"variants": {
        f"clearing={c},facing={f},power={p}": turned(f"{console}_lit" if p else console, f)
        for c in booleans for f in FACINGS for p in range(16)}})
    item(console, build(console).models[f"{console}_lit"], fa.PLANT_DISPLAY)
    hand = d19.HAND["block"]
    particle(hand, hand)
    write(states / f"{hand}.json", {"variants": {f"facing={f},powered={p}": turned(hand, f) for f in FACINGS for p in booleans}})
    item(hand, build(hand).models["item"], fa.PLANT_DISPLAY)

    # 12. The spider's larder.
    cocoon = d19.COCOON["block"]
    particle(cocoon, cocoon)
    write(states / f"{cocoon}.json", {"variants": {"": {"model": rid(f"block/{cocoon}")}}})
    item(cocoon, build(cocoon).models["item"], fa.PLANT_DISPLAY)
    sacs_ = d19.EGG_SACS["block"]
    model(sacs_, sacs_)
    write(states / f"{sacs_}.json", {"multipart": [{"when": {face: "true"}, "apply": {"model": rid(f"block/{sacs_}"), **turn}}
                                                    for face, turn in MULTIFACE.items()]})
    item(sacs_, build(sacs_).models["item"], fa.PLANT_DISPLAY)
    drape = d19.DRAPE["block"]
    for part in range(4):
        model(f"{drape}_{part}", drape)
    write(states / f"{drape}.json", {"variants": {f"facing={f},lit={l},part={p}": turned(f"{drape}_{p}", f)
                                                  for f in FACINGS for l in booleans for p in range(4)}})
    item(drape, build(drape).models["item"], fa.PLANT_DISPLAY)
    spools = d19.SPOOLS["block"]
    model(spools, spools)
    write(states / f"{spools}.json", {"variants": {f"facing={f}": turned(spools, f) for f in FACINGS}})
    item(spools, build(spools).models["item"], fa.PLANT_DISPLAY)

    # 13. The dinner party: the chair and the setting are drawn by the client.
    chair = d19.CHAIR["block"]
    particle(chair, chair)
    write(states / f"{chair}.json", {"variants": {f"facing={f},out={o}": turned(chair, f) for f in FACINGS for o in booleans}})
    item(chair, build(chair).models["haunted_dining_chair"], TALL_ITEM)
    setting = d19.SETTING["block"]
    particle(setting, setting)
    write(states / f"{setting}.json", {"variants": {f"facing={f},lit={l},setting={s}": turned(setting, f)
                                                    for f in FACINGS for l in booleans for s in d19.SETTINGS}})
    item(setting, build(setting).models["item"], fa.PLANT_DISPLAY)
    clock = d19.CLOCK["block"]
    model(clock, clock)
    particle(f"{clock}_upper", clock)
    write(states / f"{clock}.json", {"variants": {
        f"facing={f},half={h},powered={p}": turned(clock if h == "lower" else f"{clock}_upper", f if h == "lower" else "north")
        for f in FACINGS for h in ("lower", "upper") for p in booleans}})
    item(clock, build(clock).models["item"], TALL_ITEM)

    # 14. The witchlights, in every colour, asleep and awake.
    for kind in d19.WITCHLIGHTS:
        sc = build(kind)
        for colour in d19.WITCHLIGHT_COLOURS:
            for awake in (False, True):
                name = f"{kind}_{colour}{'_awake' if awake else ''}"
                write(models / f"{name}.json", fa.model(kind, sc.models[name]))
        if kind == "witchlight_lamp_post":
            particle(f"{kind}_upper", kind)
            write(states / f"{kind}.json", {"variants": {
                f"awake={a},colour={c},facing={f},half={h}":
                    turned(f"{kind}_{c}{'_awake' if a == 'true' else ''}" if h == "lower" else f"{kind}_upper", f if h == "lower" else "north")
                for a in booleans for c in d19.WITCHLIGHT_COLOURS for f in FACINGS for h in ("lower", "upper")}})
            item(kind, sc.models["item"], TALL_ITEM)
        else:
            write(states / f"{kind}.json", {"variants": {
                f"awake={a},colour={c}": {"model": rid(f"block/{kind}_{c}{'_awake' if a == 'true' else ''}")}
                for a in booleans for c in d19.WITCHLIGHT_COLOURS}})
            item(kind, sc.models["item"], fa.PLANT_DISPLAY)

    # 15. The silhouettes (drawn by the client) and the harvest moon.
    silhouette = d19.SILHOUETTE["block"]
    particle(silhouette, silhouette)
    write(states / f"{silhouette}.json", {"variants": {f"figure={g},rotation={r}": {"model": rid(f"block/{silhouette}")}
                                                       for g in d19.FIGURES for r in range(16)}})
    item(silhouette, build(silhouette).models["item"], fa.PLANT_DISPLAY)
    moon = d19.MOON["block"]
    for part in range(4):
        model(f"{moon}_{part}", moon)
    write(states / f"{moon}.json", {"variants": {f"facing={f},lit={l},part={p}": turned(f"{moon}_{p}", f)
                                                 for f in FACINGS for l in booleans for p in range(4)}})
    item(moon, build(moon).models["item"], MOON_ITEM)

    for block in d19.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
    write(root / "textures" / "block" / f"{sacs_}.png.mcmeta", {"animation": {"frametime": 12, "interpolate": True}})
    write(root / "decor19_quads.json", decor19_quads())


def loot(out, write):
    from decor_data import self_drop
    for block in d19.blocks():
        half = {d19.CLOCK["block"]: ("half", "lower"), "witchlight_lamp_post": ("half", "lower"), d19.DRAPE["block"]: ("part", "0"),
                d19.MOON["block"]: ("part", "0")}.get(block)
        if block == d19.EGG_SACS["block"]:
            # One cluster for each face it covers, as glow lichen gives (a prop: no shears needed).
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
                {"type": "minecraft:set_count", "count": 0, "add": False},
                *[{"type": "minecraft:set_count", "count": 1, "add": True,
                   "condition": {"type": "minecraft:match_block", "blocks": rid(block), "state": {face: "true"}}} for face in ALL6],
                {"type": "minecraft:explosion_decay"}], "name": rid(block)}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})
            continue
        if half is None:
            write(out / f"{block}.json", self_drop(block))
            continue
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                                {"type": "minecraft:match_block", "blocks": rid(block), "state": {half[0]: half[1]}}]},
            "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})


def tags(tags):
    for block in (d19.HARNESS["block"], d19.CONSOLE["block"], *d19.WITCHLIGHTS, d19.MOON["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in (d19.SPOOLS["block"], d19.CHAIR["block"], d19.CLOCK["block"], d19.SILHOUETTE["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))


def textures():
    """(kind, name) -> image for each block's texture and the client's glows, spiderling and moons."""
    out = {}
    for block in d19.blocks():
        if block == d19.EGG_SACS["block"]:
            out[("block", block)] = egg_sac_strip()
        else:
            out[("block", block)] = build(block).atlas.img
    out[("entity", "spiderling")] = build("spiderling").atlas.img
    out[("entity", "lab_eye_glow")] = lab_eye_texture()
    out[("entity", "witchlight_glow")] = white_glow_texture()
    out[("entity", "witchlight_wisp")] = wisp_image()
    for phase in range(8):
        out[("entity", f"harvest_moon_{phase}")] = moon_texture(phase)
    return out
