"""Original textures for the eighth batch of Halloween decorations, the mad scientist and monsters (requires Pillow):
the Tesla Coil's iron, copper, winding, toroid and arc, the Lab Table's steel, straps, sheet and the patient's hand,
the Specimen Jar's iron, glass, fluid, bubble and three of its specimens (its eye is tools/decor16_data.py's), the
Mummy Sarcophagus's case, gold, painted lid and the mummy's wraps and face, the Raven's perch, feathers, wing, beak and
eye, and the Black Cat's fur, faces and eyes.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Block textures are 16x16 and opaque, except the
jar's glass (see-through in the middle) and its fluid (translucent); the arc, bubble and cat's eyes have see-through
pixels.

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""
import math
import random

from crop_textures import Canvas, rgb
from decor_textures import noise
import block_style as bs

IRON = [rgb("2a2a2e"), rgb("38383e"), rgb("46464e"), rgb("6a6a74")]
COPPER = [rgb("7a3e1c"), rgb("a4562a"), rgb("c8743a"), rgb("e89a5a")]
ALUMINIUM = [rgb("8e949a"), rgb("aab0b6"), rgb("c8ced4"), rgb("e8eef2")]
STEEL = [rgb("6e7478"), rgb("82888c"), rgb("969ca0"), rgb("b4babe")]
LEATHER = [rgb("3e2414"), rgb("4e2e1a"), rgb("5e3a22")]
SHEET = [rgb("a8a294"), rgb("bcb6a8"), rgb("cec8ba"), rgb("ddd8cc")]
STAIN = rgb("7a5a3a")
SKIN = [rgb("6a7a62"), rgb("7a8a70"), rgb("8a9a7e")]
GLASS = [rgb("a8d0c8"), rgb("d0eee8")]
FLUID = [rgb("2e9a3a"), rgb("44b84a"), rgb("62d45e")]
SANDSTONE = [rgb("b8945a"), rgb("c8a46a"), rgb("d6b47c")]
GOLD = [rgb("8a6414"), rgb("b88a22"), rgb("dcb03c"), rgb("f4d870")]
LAPIS = [rgb("1a2e6e"), rgb("24408e"), rgb("3458b0")]
WRAPS = [rgb("8a7e62"), rgb("a2967a"), rgb("b8ac90"), rgb("cec4a8")]
FEATHERS = [rgb("0c0c12"), rgb("14141c"), rgb("1e1e2a"), rgb("2e3044")]
WOOD = [rgb("3a2614"), rgb("4a321c"), rgb("5a3e24")]
FUR = [rgb("0a0a0c"), rgb("121214"), rgb("1a1a1e"), rgb("262630")]
INK = rgb("0c0a08")


def put(img, x, y, color, alpha=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color + (alpha,))


# ---------------------------------------------------------------- the tesla coil

def coil_iron():
    """Dark riveted plate."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 18101, [2, 3, 2])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (7, 14)):
        c.px(x, y, IRON[3])
    return c.img


def coil_copper():
    """Thick copper turns of the primary coil."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, COPPER[[1, 2, 3, 2, 1, 0][y % 6]])
    return c.img


def winding():
    """Fine copper wire wound round and round: thin bright and dark rows."""
    c = Canvas()
    tarnish = bs.wobble(18121, 1)
    for y in range(16):
        for x in range(16):
            tone = 3 if y % 2 == 0 else 1
            c.px(x, y, COPPER[max(0, tone + min(0, int(tarnish(x, y))))])
    return c.img


def toroid():
    """Polished aluminium with a highlight band."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            band = math.exp(-((y - 5) / 2.5) ** 2) * 2.5
            c.px(x, y, ALUMINIUM[min(3, int(1 + band))])
    return c.img


def arc():
    """A white-hot core with a violet glow at its edges, across the card."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 7.5
            if d < 0.35:
                c.px(x, y, (250, 240, 255))
            elif d < 0.7:
                c.px(x, y, (200, 150, 255))
            else:
                put(c.img, x, y, (150, 90, 240), 255)
    return c.img


# ---------------------------------------------------------------- the lab table

def table_steel():
    """Brushed steel, its grain in long streaks across, with scratches."""
    c = Canvas()
    rng = random.Random(18201)
    bs.streaks(STEEL[:3], 18202, vertical=False, spread=0.7)(c)
    for _ in range(6):
        x, y = rng.randrange(14), rng.randrange(16)
        c.px(x, y, STEEL[3])
        c.px(x + 1, y, STEEL[3])
    return c.img


def leather():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, LEATHER, 18211, [2, 3, 1])
    c.rect(5, 6, 10, 9, STEEL[3])  # the buckle
    c.rect(6, 7, 9, 8, LEATHER[0])
    return c.img


def table_sheet():
    """A greyed hospital sheet, folded, with old stains."""
    c = Canvas()
    rng = random.Random(18221)
    for y in range(16):
        for x in range(16):
            fold = 1 + int(1.5 + math.sin(x * 0.8 + y * 0.2) * 1.2)
            c.px(x, y, SHEET[min(3, fold)])
    for _ in range(3):
        x, y = rng.randrange(2, 13), rng.randrange(2, 13)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1)):
            c.px(x + dx, y + dy, STAIN)
    return c.img


def skin():
    """Grey-green skin, with dark nails at one end."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, SKIN, 18231, [2, 3, 2])
    c.rect(0, 13, 15, 15, rgb("3a4436"))
    return c.img


# ---------------------------------------------------------------- the specimen jar

def jar_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[1:], 18301, [2, 3, 1])
    return c.img


def jar_glass():
    """Clear glass: a pale rim round the edges and a streak of reflection; see-through between."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                put(c.img, x, y, GLASS[0])
            elif x == 3 and 2 <= y <= 12 or x == 4 and 4 <= y <= 8:
                put(c.img, x, y, GLASS[1])
    return c.img


def jar_fluid():
    """Glowing green fluid, translucent, a little cloudy."""
    c = Canvas()
    fluid = bs.surface(FLUID, 18311)
    for y in range(16):
        for x in range(16):
            put(c.img, x, y, fluid(x, y), 150)
    return c.img


def bubble():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 56:
                put(c.img, x, y, (210, 255, 210) if x + y < 12 else (120, 220, 130))
    return c.img


def nerve():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("8a2a2a"), rgb("a43a34"), rgb("c05a4a")], 18331, [2, 3, 1])
    return c.img


def tentacle():
    """Purple skin with pale suckers in a row."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("4a2058"), rgb("5c2a6c"), rgb("6e3680")], 18341, [2, 3, 2])
    for y in range(1, 16, 4):
        for x in (4, 11):
            c.rect(x, y, x + 1, y + 1, rgb("d6a8c8"))
    return c.img


def pumpkin():
    """A pickled pumpkin, faded orange, with ribs."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("b86a1e") if x % 4 == 0 else rgb("d0842e") if x % 4 != 2 else rgb("e09a40"))
    return c.img


def stem():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("3a4a1e"), rgb("4a5c26")], 18351, [1, 1])
    return c.img


def brain():
    """Pink folds."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            fold = math.sin(x * 1.3 + math.sin(y * 0.9) * 2.0)
            c.px(x, y, rgb("d48a96") if fold > 0.4 else rgb("b86a78") if fold > -0.4 else rgb("e8a8b2"))
    return c.img


# ---------------------------------------------------------------- the mummy sarcophagus

def sarcophagus_case():
    """Sandstone painted with bands of lapis and gold."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, SANDSTONE, 18401, [1, 2, 2])
    for y in (3, 11):
        c.rect(0, y, 15, y, LAPIS[1])
        c.rect(0, y + 1, 15, y + 1, GOLD[2])
    return c.img


def sarcophagus_inside():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("1e140c"), rgb("281a10"), rgb("322216")], 18411, [2, 3, 1])
    return c.img


def sarcophagus_gold():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GOLD[:3], 18421, [1, 2, 2])
    for i in range(0, 16, 4):
        c.px(i, i % 3, GOLD[3])
    return c.img


LID_FACE = [
    "..LLLLLLLLLLLL..",
    ".LLGGGGGGGGGGLL.",
    ".LGGggggggggGGL.",
    ".LGgEEggggEEgGL.",
    ".LGggggggggggGL.",
    ".LLGgggrrgggGLL.",
    "..LLGGGGGGGGLL..",
    "..GLGLGLGLGLGL..",
    "..LGLGLGLGLGLG..",
    "...GGGGGGGGGG...",
    "...SSSLSSLSSS...",
    "...SSSSLLSSSS...",
    "...SSSSSSSSSS...",
    "...SSLSSSSLSS...",
    "...SSSSSSSSSS...",
    "...GGGGGGGGGG..."]


def lid_face():
    """The painted lid: a gold death mask with lapis headdress stripes, a collar of bands, and the body below,
    stretched over the whole lid."""
    c = Canvas()
    colours = {"L": LAPIS[1], "G": GOLD[2], "g": GOLD[3], "E": rgb("101030"), "r": GOLD[0], "S": SANDSTONE[1]}
    for y, row in enumerate(LID_FACE):
        for x, ch in enumerate(row):
            c.px(x, y, colours.get(ch, SANDSTONE[0]))
    return c.img


def wraps():
    """Old linen wrappings, crossing in bands, grimy."""
    c = Canvas()
    grime = bs.wobble(18431, 1)
    for y in range(16):
        for x in range(16):
            band = (x + 2 * y) % 6 < 3
            tone = 2 if band else 1
            tone += min(0, int(grime(x, y)))
            c.px(x, y, WRAPS[max(0, tone)])
        if y % 5 == 4:
            c.rect(0, y, 15, y, WRAPS[0])
    return c.img


def mummy_face():
    """The mummy's wrapped face: a gap in the wrappings with two pale glowing eyes."""
    c = Canvas()
    c.img.paste(wraps(), (0, 0))
    c.rect(3, 6, 12, 8, rgb("1a140c"))
    for x in (5, 9):
        c.rect(x, 7, x + 1, 7, rgb("e8f0c0"))
    return c.img


# ---------------------------------------------------------------- the raven on a perch

def perch_wood():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WOOD, 18501, [2, 3, 2])
    return c.img


def feathers():
    """Glossy black feathers with a blue-violet sheen."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            tone = (x + y // 2) % 3
            c.px(x, y, FEATHERS[tone + (1 if (x * 5 + y * 3) % 13 == 0 else 0)])
    return c.img


def wing():
    """Long flight feathers lying back."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, FEATHERS[3] if x % 3 == 0 else FEATHERS[1] if y % 4 else FEATHERS[2])
    return c.img


def beak():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("1a1a1c"), rgb("26262a"), rgb("38383e")], 18521, [2, 2, 1])
    return c.img


def raven_eye():
    """The side of the raven's head: black feathers and a bright, beady eye."""
    c = Canvas()
    c.img.paste(feathers(), (0, 0))
    c.rect(5, 5, 8, 8, rgb("0a0a0a"))
    c.px(6, 6, rgb("f0e8c0"))
    return c.img


# ---------------------------------------------------------------- the black cat

def fur():
    """Glossy black fur in short streaks, with a glint every few rows."""
    c = Canvas()
    bs.streaks(FUR[:3], 18601, across=1.5, along=4.0)(c)
    for y in range(1, 16, 4):
        x = (y * 5) % 16
        c.px(x, y, FUR[3])
        c.px(x + 1, y, FUR[3])
    return c.img


CAT_FACE = [
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "fffeeeffffeeefff",
    "fffeKeffffeKefff",
    "fffeeeffffeeefff",
    "ffffffffffffffff",
    "fffffffnnfffffff",
    "ffffffWffWffffff",
    "fffffffWWfffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff"]
CAT_HISS = [
    "ffffffffffffffff",
    "ffffffffffffffff",
    "fffeeeffffeeefff",
    "fffeKeffffeKefff",
    "fffeeeffffeeefff",
    "ffffffffffffffff",
    "fffffffnnfffffff",
    "ffffMMMMMMMMffff",
    "ffffMwMMMMwMffff",
    "ffffMMMMMMMMffff",
    "fffffMMMMMMfffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff",
    "ffffffffffffffff"]


def cat_face(grid):
    """The cat's face: green eyes with slit pupils, a pink nose, whiskers; hissing, an open mouth and fangs."""
    c = Canvas()
    colours = {"e": rgb("6ac83a"), "K": rgb("080808"), "n": rgb("8a4a52"), "W": rgb("4a4a52"), "M": rgb("5a1820"),
               "w": rgb("f0f0e8")}
    fur = bs.surface(FUR[:3], 18611)
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            c.px(x, y, colours.get(ch) or fur(x, y))
    return c.img


def cat_eyes():
    """The glow of the cat's eyes at night."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            put(c.img, x, y, (120, 255, 90) if 6 <= x <= 9 else (60, 200, 50))
    return c.img


def decor8_textures():
    """(kind, name) -> image for every texture of the eighth decorations batch."""
    return {
        ("block", "tesla_coil_iron"): coil_iron(),
        ("block", "tesla_coil_copper"): coil_copper(),
        ("block", "tesla_coil_winding"): winding(),
        ("block", "tesla_coil_toroid"): toroid(),
        ("entity", "tesla_coil_arc"): arc(),
        ("block", "lab_table_steel"): table_steel(),
        ("block", "lab_table_leather"): leather(),
        ("block", "lab_table_sheet"): table_sheet(),
        ("block", "lab_table_skin"): skin(),
        ("block", "specimen_jar_iron"): jar_iron(),
        ("block", "specimen_jar_glass"): jar_glass(),
        ("block", "specimen_jar_fluid"): jar_fluid(),
        ("entity", "specimen_jar_bubble"): bubble(),
        ("block", "specimen_nerve"): nerve(),
        ("block", "specimen_tentacle"): tentacle(),
        ("block", "specimen_pumpkin"): pumpkin(),
        ("block", "specimen_stem"): stem(),
        ("block", "specimen_brain"): brain(),
        ("block", "mummy_sarcophagus_case"): sarcophagus_case(),
        ("block", "mummy_sarcophagus_inside"): sarcophagus_inside(),
        ("block", "mummy_sarcophagus_gold"): sarcophagus_gold(),
        ("block", "mummy_sarcophagus_lid_face"): lid_face(),
        ("block", "mummy_sarcophagus_wraps"): wraps(),
        ("block", "mummy_sarcophagus_mummy_face"): mummy_face(),
        ("block", "raven_perch_wood"): perch_wood(),
        ("block", "raven_feathers"): feathers(),
        ("block", "raven_wing"): wing(),
        ("block", "raven_beak"): beak(),
        ("block", "raven_eye"): raven_eye(),
        ("block", "black_cat_fur"): fur(),
        ("block", "black_cat_face"): cat_face(CAT_FACE),
        ("block", "black_cat_face_hiss"): cat_face(CAT_HISS),
        ("entity", "black_cat_eyes"): cat_eyes(),
    }
