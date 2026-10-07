"""Original textures for the graveyard pack (requires Pillow): the four memorial stones (white marble, dark slate,
speckled granite, warm sandstone) at each of their four stages of weathering (clean, worn, mossy, overgrown), their
carved and polished faces, rock-faced granite, Celtic knotwork, ivy, and the Stonemason's Chisel; for the buildings of
pack 3, weathering oak and roof slates, stained glass, a chequered marble floor, lamp and lantern glass, a bronze door's
smoked glass and the Bronze Mausoleum Door's item.

Weathering is drawn the way stone weathers: worn stone is duller, with grime run down from the top and chipped arrises;
mossy stone has moss creeping up from the ground and lichen rosettes; overgrown stone is green to a third of its height
and crusted with lichen. Block faces take their texture by position, so the bottom rows of a texture lie along the
ground: each stage has an `_upper` form, without the band of ground moss, for the parts of a monument above its first
block.

Every material is painted in the manner of the vanilla blocks (tools/block_style.py): stone as stone, cobblestone or
gravel, oak as planks, moss as on mossy cobblestone; a short palette used mostly in its mid-tones, variation in small
clumps rather than per-pixel static, and every pattern tiling so neighbouring blocks join up.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured.
"""
import math
import random

from PIL import Image

import block_style as bs

from crop_textures import Canvas, rgb, outline
from halloween_textures import shade

STAGES = ("clean", "worn", "mossy", "overgrown")

# name: (base colours darkest to lightest, accent colours, seed)
STONES = {
    "marble": ([rgb("d6d4ce"), rgb("dddbd5"), rgb("e4e2dc"), rgb("eae8e2")], [rgb("c2c0bb"), rgb("cdcbc5")], 7101),
    "slate": ([rgb("3c424b"), rgb("444a54"), rgb("4b525c"), rgb("545b66")], [rgb("353a42"), rgb("5d6570")], 7102),
    "granite": ([rgb("8a8280"), rgb("99908d"), rgb("a69d99"), rgb("b2aaa5")], [rgb("3e3b3c"), rgb("c49d92"), rgb("d8d2cc")], 7103),
    "sandstone": ([rgb("b99e6c"), rgb("c7ad7a"), rgb("d3ba88"), rgb("dcc596")], [rgb("a88c5c"), rgb("e4d0a6")], 7104),
}
MOSS = [rgb("2f4a1c"), rgb("3e5e22"), rgb("4f7429"), rgb("63883a"), rgb("7a9c48")]
LICHEN = {"marble": [rgb("b8c08a"), rgb("a6b07a")], "slate": [rgb("7f8a72"), rgb("939e86")],
          "granite": [rgb("d9a548"), rgb("c88a3a")], "sandstone": [rgb("cf8f3a"), rgb("b9c07e")]}
GRIME = rgb("3a3631")
IVY = [rgb("1f3a14"), rgb("2b4d1a"), rgb("3a6322"), rgb("4c7a2c"), rgb("67953a")]


def _ramp(stone):
    """A stone's six tones, darkest first: its four base colours with a darker and a lighter one beyond them."""
    base = STONES[stone][0]
    return [shade(base[0], 0.88)] + list(base) + [shade(base[3], 1.05)]


def _base(stone, seed_extra=0, smooth=False):
    """The bare stone of `stone`, in the manner of the vanilla stones (tools/block_style.py): marble a pale stone of soft
    clumps with a grey vein meandering across, slate dark and fine-grained in broken laminations, granite speckled with
    black mica, pink feldspar and white quartz, sandstone in soft bedding bands."""
    base, accent, seed = STONES[stone]
    seed += seed_extra
    ramp = _ramp(stone)
    c = Canvas()
    if stone == "granite":
        bs.speckled(ramp, accent, seed, density=0.06 if smooth else 0.09)(c)
        return c
    bs.stone(ramp, seed, cracks=0 if stone != "slate" else 2, spread=0.55 if smooth else 0.75)(c)
    g = bs.grain(16, 16, seed + 13, 2.0, 5.0)
    if stone == "marble":
        phase = seed % 16
        for y in range(16):
            x = int(round(8 + 3.0 * math.sin(2 * math.pi * (y + phase) / 16.0) + 1.5 * math.sin(2 * math.pi * (y + phase) / 8.0)))
            if g(x, y) > 0.3:
                c.px(x % 16, y, accent[1] if g(x, y) < 0.65 else accent[0])
    elif stone == "slate":
        for y in range(0, 16, 4):
            for x in range(16):
                if g(x, y) > 0.38:
                    c.px(x, y, ramp[1])
    elif stone == "sandstone":
        for y in range(16):
            for x in range(16):
                if y % 5 == 2 and g(x, y) > 0.3:
                    c.px(x, y, ramp[1])
                elif y % 5 == 3 and g(x, y) > 0.55:
                    c.px(x, y, ramp[4])
    return c


def _weather(c, stone, stage, seed, upper=False):
    """Weathers the canvas to `stage`, in the manner of vanilla mossy cobblestone: duller stone, soft grime washed down
    from the top, a pale chip or two; then moss in irregular clumps (and from the ground up, unless this is an upper
    part) and small rosettes of lichen."""
    if stage == 0:
        return c
    dull = (1.0, 0.95, 0.9, 0.86)[stage]
    for y in range(16):
        for x in range(16):
            pixel = c.get(x, y)
            if pixel:
                c.px(x, y, shade(pixel[:3], dull))
    bs.grime_over(c, GRIME, seed * 7 + stage, streaks=1 + stage, strength=0.22 + 0.06 * stage)
    rng = random.Random(seed * 11 + stage)
    for _ in range(stage):
        x, y = rng.randrange(16), rng.randrange(16)
        pixel = c.get(x, y)
        if pixel:
            c.px(x, y, shade(pixel[:3], 1.12))
    if stage >= 2:
        ground = 0 if upper else (3 if stage == 2 else 6)
        bs.moss_over(c, MOSS[1:], seed * 13 + stage, amount=0.1 if stage == 2 else 0.2, ground=ground,
                     lichen=LICHEN[stone], lichen_count=3 if stage == 2 else 6)
    return c


def stone_face(stone, stage, upper=False):
    return _weather(_base(stone), stone, STAGES.index(stage), STONES[stone][2], upper).img


def relief_face(stone, stage, upper=False):
    """Carved and polished work, smoother than the dressed face but still stone: marble polished almost white with a
    faint vein, slate the pale grey of fresh-cut slate, granite polished dark with fine specks, sandstone rubbed smooth;
    all in soft, low-contrast clumps."""
    base, accent, seed = STONES[stone]
    c = Canvas()
    if stone == "granite":
        dark = [rgb("5e5756"), rgb("676060"), rgb("6e6766"), rgb("766e6c"), rgb("7d7573")]
        bs.speckled(dark, (rgb("2e2b2c"), rgb("a8857c"), rgb("b9b2ac")), seed + 50, density=0.05)(c)
    else:
        lift = {"marble": 1.03, "slate": 1.38, "sandstone": 1.03}[stone]
        ramp = [shade(k, lift) for k in _ramp(stone)]
        bs.stone(ramp, seed + 50, cracks=0, spread=0.45, coarse=7.0)(c)
        if stone == "marble":
            g = bs.grain(16, 16, seed + 51, 2.0, 5.0)
            for y in range(16):
                x = int(round(8 + 3.0 * math.sin(2 * math.pi * y / 16.0)))
                if g(x, y) > 0.45:
                    c.px(x % 16, y, accent[1])
    return _weather(c, stone, STAGES.index(stage), STONES[stone][2] + 3, upper).img


def rough_face(stone, stage, upper=False):
    """Rock-faced stone, split rather than sawn: rounded lumps, in the manner of vanilla cobblestone, each lit along its
    upper left and shaded along its lower right, with dark joints."""
    c = Canvas()
    ramp = _ramp(stone)
    bs.cobble(ramp, STONES[stone][2] + 90, count=7, joint=shade(ramp[0], 0.85))(c)
    return _weather(c, stone, STAGES.index(stage), STONES[stone][2] + 9, upper).img


def knotwork(stone, stage, upper=False):
    """A plait of two strands crossing on the diagonal, carved as raised bands with a dark hollow either side; at the
    middle the strand falling to the right passes over, at the corners the other (the face of a Celtic cross)."""
    c = _base(stone, 120, smooth=True)
    base = STONES[stone][0]
    hollow = shade(base[0], 0.78)
    edge = shade(base[1], 0.88)
    band = [shade(base[2], 1.06), shade(base[3], 1.1)]

    def dist(x, y, sign):
        """Distance (pixels) from (x, y) to the nearest strand of one direction, tiling every 16 pixels."""
        u = (x - y) if sign > 0 else (x + y - 15)
        u = (u + 8) % 16 - 8
        return abs(u) / 1.414

    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            a, b = dist(px, py, 1), dist(px, py, -1)
            centre = abs(px - 8) < 4 and abs(py - 8) < 4
            over = a if centre else b
            under = b if centre else a
            if over <= 1.3:
                c.px(x, y, band[1])
            elif over <= 1.9:
                c.px(x, y, edge)
            elif under <= 1.3:
                c.px(x, y, band[0])
            elif under <= 1.9:
                c.px(x, y, edge)
            else:
                c.px(x, y, hollow)
    return _weather(c, stone, STAGES.index(stage), STONES[stone][2] + 21, upper).img


IRON = [rgb("1d1e21"), rgb("26282c"), rgb("303338"), rgb("3c4046")]
RUST = [rgb("5a2e17"), rgb("7a3e1c"), rgb("9a5224"), rgb("b4682e")]
BRONZE = [rgb("5e4220"), rgb("7a5a2c"), rgb("96723a"), rgb("b48c4c")]
VERDIGRIS = [rgb("3f7a66"), rgb("4f927a"), rgb("68a88e"), rgb("86bea4")]


def iron(stage, seed=7301):
    """Cast iron painted black, in soft clumps with a sheen down every eighth column: chipped at worn, rust breaking
    through in clumps at mossy, rusty and mossy from the bottom when overgrown."""
    c = Canvas()
    ramp = [rgb("16171a")] + IRON + [rgb("4a4e55")]
    bs.stone(ramp, seed, cracks=0, spread=0.55)(c)
    for x in range(0, 16, 8):
        for y in range(16):
            c.px(x, y, ramp[-1] if y % 5 else ramp[-2])
    if stage >= 1:
        bs.moss_over(c, RUST[1:], seed + stage, amount=(0.04, 0.16, 0.34)[stage - 1], ground=0 if stage < 3 else 4)
    if stage == 3:
        bs.moss_over(c, MOSS[1:], seed + 17, amount=0.08, ground=3)
    return c.img


def bronze(stage, seed=7401):
    """Cast bronze, in the manner of a vanilla metal block: a warm face of soft clumps with a lit edge, darkening to a
    brown patina, then with verdigris in clumps, crusting up from the bottom when overgrown."""
    factor = (1.0, 0.78, 0.68, 0.6)[stage]
    ramp = [shade(k, factor) for k in [shade(BRONZE[0], 0.85)] + BRONZE + [shade(BRONZE[3], 1.15)]]
    c = Canvas()
    bs.metal(ramp, seed, panels=False)(c)
    if stage >= 2:
        bs.moss_over(c, VERDIGRIS[1:], seed + stage, amount=0.14 if stage == 2 else 0.3, ground=0 if stage == 2 else 5)
    return c.img


def ivy():
    """Ivy leaves, dark and glossy, packed over their stems, in the manner of vanilla leaves: clumps of greens with
    darker shadows between."""
    c = Canvas()
    bs.stone(IVY, 7201, fine=1.6, coarse=4.0, cracks=0, spread=1.0)(c)
    return c.img


def chisel_item():
    """A stonemason's chisel: a steel blade, square shank and a ringed head, laid on the slant with an ash mallet."""
    c = Canvas()
    steel = [rgb("5c636b"), rgb("7d858e"), rgb("a2aab2"), rgb("cfd5da")]
    ash = [rgb("8a6236"), rgb("a77a45"), rgb("c49a5e")]
    # The chisel, from its edge (lower left) to its struck head (upper right).
    for i in range(10):
        x, y = 3 + i, 12 - i
        c.px(x, y, steel[2])
        c.px(x + 1, y, steel[1])
        c.px(x, y - 1, steel[3] if i < 2 else steel[2])
    c.px(2, 13, steel[3])
    c.px(3, 13, steel[3])
    for x, y in ((12, 3), (13, 3), (13, 2), (12, 2), (11, 3)):
        c.px(x, y, steel[1])
    c.px(13, 1, steel[3])
    # The mallet, a dumpy round head on a short handle, below it.
    for x in range(8, 13):
        for y in range(11, 15):
            c.px(x, y, ash[2] if y == 11 else ash[1])
    for i in range(4):
        c.px(6 - i, 12 + i // 2, ash[0])
    outline(c, rgb("26292d"))
    return c.img


# ---------------------------------------------------------------- pack 3: buildings

OAK = [rgb("6a4527"), rgb("7d5532"), rgb("8f643c"), rgb("a27448")]
SILVER = rgb("8c8a84")
SLATES = [rgb("2f343d"), rgb("373d47"), rgb("3f4651"), rgb("48505c")]


def oak(stage, seed=7501):
    """Oak timber in the manner of vanilla planks, its grain running along it: warm brown when new, silvering grey as
    it weathers, then with moss and lichen in clumps."""
    silver = (0.0, 0.35, 0.55, 0.65)[stage]
    ramp = [shade(OAK[0], 0.85)] + OAK + [shade(OAK[3], 1.08)]
    ramp = [tuple(int(p + (g - p) * silver) for p, g in zip(k, SILVER)) for k in ramp]
    c = Canvas()
    bs.planks(ramp, seed + stage, boards=4, vertical=True)(c)
    if stage >= 2:
        bs.moss_over(c, MOSS[1:], seed + stage, amount=0.08 if stage == 2 else 0.18, lichen=[rgb("a6b07a"), rgb("8a9a62")],
                     lichen_count=2 if stage == 2 else 4)
    return c.img


def roof_slate(stage, seed=7601):
    """Roofing slates in courses (running across the texture's u, as a roof's slope lies along a box's length): each
    slate a fine-grained dark stone, lit along its upper edge, its lower edge dark; worn slates go dusty, then moss
    creeps into the joints in clumps, with yellow lichen."""
    dust = (1.0, 1.08, 1.0, 0.95)[stage]
    ramp = [shade(SLATES[0], 0.85)] + SLATES + [shade(SLATES[3], 1.1)]
    g = bs.grain(16, 16, seed, 2.0, 5.0)
    c = Canvas()
    for x in range(16):
        course = x // 4
        for y in range(16):
            joint = (y + course * 3) % 6 == 0
            colour = ramp[0] if joint else bs.tone(g(x, y), ramp[1:5], spread=0.7)
            if x % 4 == 0:
                colour = shade(SLATES[0], 0.8)
            elif x % 4 == 1 and not joint:
                colour = ramp[4]
            c.px(x, y, shade(colour, dust))
    if stage >= 2:
        bs.moss_over(c, MOSS[1:], seed + stage, amount=0.1 if stage == 2 else 0.22, lichen=LICHEN["granite"],
                     lichen_count=1 if stage == 2 else 3)
    return c.img


def stained_glass():
    """Leaded stained glass, opaque: diamond quarries in a lattice of lead, deep blue and ruby by turns, with a gold
    rosette where every other pair of leads cross."""
    c = Canvas()
    lead = rgb("2a2a2e")
    blues = [rgb("1f3a8a"), rgb("2a4aa8"), rgb("3858bc")]
    reds = [rgb("8c1422"), rgb("a81c2c"), rgb("c02a38")]
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 8, (x - y) % 8
            if u == 0 or v == 0:
                c.px(x, y, lead)
                continue
            cell = ((x + y) // 8 + (x - y + 16) // 8) % 2
            palette = blues if cell == 0 else reds
            # Lighter towards each pane's middle, as light comes through thinner glass.
            depth = min(u, 8 - u, v, 8 - v)
            c.px(x, y, palette[min(2, depth - 1)])
    for cx, cy in ((0, 0), (8, 8)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            c.px((cx + dx) % 16, (cy + dy) % 16, rgb("d8a83a") if (dx, dy) != (0, 0) else rgb("f4d470"))
    return c.img


def marble_floor():
    """A chequered floor of white marble and black slate in squares of four pixels (25 cm), polished: each square a
    soft-grained stone, lit along its top and left edge."""
    c = Canvas()
    white = _ramp("marble")
    dark = [rgb("22252a"), rgb("26292e"), rgb("2e3238"), rgb("353a41"), rgb("3c4249")]
    g = bs.grain(16, 16, 7701, 2.0, 4.0)
    for y in range(16):
        for x in range(16):
            lit = x % 4 == 0 or y % 4 == 0
            if (x // 4 + y // 4) % 2 == 0:
                c.px(x, y, white[5] if lit else bs.tone(g(x, y), white[1:5], spread=0.6))
            else:
                c.px(x, y, dark[4] if lit else bs.tone(g(x, y), dark[:4], spread=0.6))
    return c.img


def lamp_glass():
    """A sanctuary lamp's ruby glass, lit from within: three clean rings of red, brightest in the middle."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            glow = max(0.0, 1 - (abs(x - 7.5) + abs(y - 9)) / 12)
            glow = 0.0 if glow < 0.3 else 0.5 if glow < 0.65 else 1.0
            c.px(x, y, tuple(min(255, int(v + 90 * glow)) for v in (176, 24, 34)))
    return c.img


def lantern_glass():
    """A lantern's panes with the flame's light behind them: warm amber in three clean rings, brightest in the middle."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            glow = max(0.0, 1 - (abs(x - 7.5) + abs(y - 8.5)) / 11)
            glow = 0.0 if glow < 0.3 else 0.5 if glow < 0.65 else 1.0
            c.px(x, y, (min(255, int(222 + 33 * glow)), min(255, int(150 + 80 * glow)), min(255, int(60 + 110 * glow))))
    return c.img


def door_glass():
    """Smoked glass behind a bronze grille: flat near-black, with one clean diagonal sheen."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("1b1f24"))
    for i in range(5):
        c.px(10 + i // 2, 2 + i, rgb("3c444e"))
    return c.img


def mausoleum_door_item():
    """The Bronze Mausoleum Door as an item: a flat bronze frame with a meeting stile, a grille over smoked glass above
    and raised panels below."""
    c = Canvas()
    for y in range(1, 16):
        for x in range(3, 13):
            c.px(x, y, BRONZE[2] if x in (3, 4) or y == 1 else BRONZE[1])
    for y in range(2, 8):
        for x in (4, 5, 6, 9, 10, 11):
            c.px(x, y, rgb("1b1f24"))
    for y in range(2, 8):
        c.px(5, y, BRONZE[2])
        c.px(10, y, BRONZE[2])
    for x in range(3, 13):
        c.px(x, 5, BRONZE[2])
    for y in range(10, 14):
        for x in (5, 10):
            c.px(x, y, BRONZE[3])
    for y in range(1, 16):
        c.px(7, y, BRONZE[0])
        c.px(8, y, BRONZE[0])
    c.px(6, 9, BRONZE[3])
    c.px(9, 9, BRONZE[3])
    outline(c, rgb("2a1d10"))
    return c.img


# ---------------------------------------------------------------- pack 4: the grounds

CHIPS = [rgb("d9d8d2"), rgb("e6e5df"), rgb("f0efe9"), rgb("c4c3bc"), rgb("b0afa8")]
SOIL = [rgb("3b2a1c"), rgb("46321f"), rgb("523b25")]
FLOWERS = [rgb("c8283a"), rgb("f2efe6"), rgb("f0c838"), rgb("8a52b8")]
GRASS = [rgb("3f6b24"), rgb("4e7f2c"), rgb("5f9234"), rgb("74a540")]


def chippings(stage, seed=7901):
    """White marble chippings, in the manner of vanilla gravel: small rounded chips of several pale tones packed
    together; dirt settling in clumps at worn, moss at mossy, and moss with grass blades when overgrown."""
    c = Canvas()
    bs.gravel([CHIPS[4], CHIPS[3], CHIPS[0], CHIPS[1], CHIPS[2]], seed, count=18)(c)
    if stage >= 1:
        bs.moss_over(c, [rgb("8a8478"), rgb("9a9488"), rgb("aaa498")], seed + 1, amount=0.06 * stage)
    if stage >= 2:
        bs.moss_over(c, MOSS[1:], seed + 2, amount=0.12 if stage == 2 else 0.24)
    if stage == 3:
        rng = random.Random(seed + 3)
        for _ in range(6):
            x, y = rng.randrange(16), rng.randrange(3, 16)
            for dy in range(rng.randint(2, 3)):
                c.px(x, y - dy, GRASS[3] if dy else GRASS[2])
    return c.img


def flower_bed(stage, seed=8001):
    """A grave's planted bed seen from above and from the side alike: dark earth in the manner of vanilla dirt, neat
    little flowers in rows, each on two leaves; let go, the flowers thin and grass comes through; overgrown, grass and
    brambles."""
    c = Canvas()
    bs.dirt([rgb("2f2116")] + SOIL + [rgb("5e4630")], seed)(c)
    keep = (4, 3, 2, 1)[stage]
    for i, (x0, y0) in enumerate((x, y) for y in range(0, 16, 4) for x in range(0, 16, 4)):
        x = x0 + (2 if (y0 // 4) % 2 else 0)
        if i % 4 >= keep:
            continue
        colour = FLOWERS[(x0 // 4 + y0 // 4) % 4]
        c.px((x + 1) % 16, y0 + 2, GRASS[1])
        c.px((x - 1) % 16, y0 + 2, GRASS[2])
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c.px((x + dx) % 16, y0 + 1 + dy, colour)
        c.px(x % 16, y0 + 1, rgb("f8e070"))
    if stage >= 1:
        bs.moss_over(c, GRASS, seed + stage, amount=(0.06, 0.16, 0.34)[stage - 1])
    if stage == 3:
        for x, y in ((4, 6), (10, 10), (13, 5)):
            c.px(x, y, rgb("3a1f2e"))
            c.px(x + 1, y, rgb("5b2a40"))
    return c.img


def pit():
    """An open grave's darkness, seen down into: nearly black earth in faint clumps, tiling so a grave two blocks long
    shows no seam (its rim is the fresh earth round it)."""
    c = Canvas()
    bs.dirt([rgb("0a0705"), rgb("0e0a07"), rgb("130e0a"), rgb("18120c"), rgb("1d160f")], 8301)(c)
    return c.img


def straps():
    """Canvas lowering straps: webbing with a woven stripe."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("b8a27a") if (y % 4) else rgb("9c8660"))
    return c.img


PETALS = {"white": [rgb("e8e4d8"), rgb("f4f1e8"), rgb("fffdf6")], "red": [rgb("8c1422"), rgb("b01e2e"), rgb("d03644")],
          "yellow": [rgb("d8a020"), rgb("eec030"), rgb("f8dc60")], "purple": [rgb("5a2e8a"), rgb("7442a8"), rgb("9264c4")]}


def petals(colour, seed=8101):
    """Flower heads in `colour` (or `mixed`: all of them, in irregular patches), or `wilted`: browned and dry; in the
    manner of vanilla flowering leaves: clumps of petal colour, a shade darker in the gaps between flowers."""
    c = Canvas()
    if colour == "mixed":
        field = bs.Field(16, 16, seed, 3.0)
        parts = {}
        for i, name in enumerate(("white", "red", "yellow", "purple")):
            part = Canvas()
            bs.stone([shade(PETALS[name][0], 0.8)] + PETALS[name], seed + i, fine=1.5, coarse=3.5, cracks=0)(part)
            parts[i] = part
        for y in range(16):
            for x in range(16):
                c.px(x, y, parts[min(3, int(field(x, y) * 4))].get(x, y)[:3])
        return c.img
    palette = [rgb("5a4a30"), rgb("6e5a3a"), rgb("85704a")] if colour == "wilted" else PETALS[colour]
    bs.stone([shade(palette[0], 0.8)] + palette, seed + len(colour), fine=1.5, coarse=3.5, cracks=0)(c)
    return c.img


def leaves(seed=8201):
    """Green leaves in the manner of vanilla leaves: clumps of greens with darker shadows between."""
    c = Canvas()
    bs.stone(GRASS, seed, fine=1.6, coarse=4.0, cracks=0, spread=1.0)(c)
    return c.img


def lantern_unlit():
    """A lantern's glass by day, unlit: flat dark amber with one clean gleam."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("4a3a22"))
    for i in range(4):
        c.px(11 + i // 2, 2 + i, rgb("8a7a5a"))
    return c.img


def graveyard_textures():
    out = {}
    for stone in STONES:
        for stage in STAGES:
            for upper in (False, True):
                suffix = f"{stage}_upper" if upper else stage
                out[("block", f"gy_{stone}_{suffix}")] = stone_face(stone, stage, upper)
                out[("block", f"gy_{stone}_relief_{suffix}")] = relief_face(stone, stage, upper)
        if stone == "granite":
            for stage in STAGES:
                out[("block", f"gy_granite_rough_{stage}")] = rough_face(stone, stage)
                for upper in (False, True):
                    suffix = f"{stage}_upper" if upper else stage
                    out[("block", f"gy_granite_knot_{suffix}")] = knotwork(stone, stage, upper)
    for k, stage in enumerate(STAGES):
        out[("block", f"gy_iron_{stage}")] = iron(k)
        out[("block", f"gy_bronze_{stage}")] = bronze(k)
    out[("block", "gy_ivy")] = ivy()
    out[("item", "stonemasons_chisel")] = chisel_item()
    for k, stage in enumerate(STAGES):
        out[("block", f"gy_oak_{stage}")] = oak(k)
        out[("block", f"gy_roof_slate_{stage}")] = roof_slate(k)
    out[("block", "gy_stained_glass")] = stained_glass()
    out[("block", "gy_marble_floor")] = marble_floor()
    out[("block", "gy_lamp_glass")] = lamp_glass()
    out[("block", "gy_lantern_glass")] = lantern_glass()
    out[("block", "gy_door_glass")] = door_glass()
    out[("item", "bronze_mausoleum_door")] = mausoleum_door_item()
    for k, stage in enumerate(STAGES):
        out[("block", f"gy_chippings_{stage}")] = chippings(k)
        out[("block", f"gy_flower_bed_{stage}")] = flower_bed(k)
    out[("block", "gy_pit")] = pit()
    out[("block", "gy_straps")] = straps()
    for colour in ("white", "red", "yellow", "purple", "mixed", "wilted"):
        out[("block", f"gy_petals_{colour}")] = petals(colour)
    out[("block", "gy_leaves")] = leaves()
    out[("block", "gy_lantern_unlit")] = lantern_unlit()
    return out
