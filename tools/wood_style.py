"""The woods and leaves of every tree the mod adds, in vanilla's way of drawing them (requires Pillow).

The owner repainted the woods (5 October 2026) and asked for every wood type to be matched to the closest of their
paintings, with everything made from each wood based on it, looking "more similar to how the vanilla textures are".
Each wood takes its colour from one of the owner's paintings (WOOD: which one, and the colour sampled from it): the
first 13 from their 24 painted stripped logs or their second set, and the cedar (the tree roster's batch 1) from the
second set's western red cedar. Each is drawn here, by code from fixed seeds, in vanilla's manner:

- bark: long vertical furrows that wander a pixel now and then, ridges between them, a few light flecks, and no
  rings across the trunk (the owner: "I don't want rings in the trees"); each tree keeps its own bark colour and
  character (aspen white with dark marks, eucalyptus in rainbow streaks);
- log ends: square growth rings to the edge, a dark line every other ring and a dark pith, inside a one-pixel ring of
  bark (stripped: inside a darker ring of the wood);
- stripped wood: straight vertical grain, broken streaks in two darker tones and a few light ones;
- planks: four boards a block, each lit along its top, a dark seam along its bottom, horizontal grain and a butt joint
  in a different place on each board;
- leaves: vanilla's fine speckle of one- and two-pixel leaves in four tones, with see-through gaps (dark under
  them, so fast graphics show dense foliage); needles darker with short slanting strokes; blossom over a little green;
  bare: twigs only.

No Mojang or other texture is read, traced or recoloured: vanilla is followed for its manner only, as the owner's
paintings follow it. Saplings and everything else keep their own textures. Called last from
crop_textures.crop_textures(), so these are the textures the game gets.
"""
import colorsys
import math
import random

from crop_textures import Canvas, rgb


def pal(*hexes):
    return [rgb(h) for h in hexes]


def ramp(base, factors=(0.58, 0.72, 0.86, 1.0, 1.12, 1.24)):
    """Six tones of one colour, dark to light, by lightness (more saturated in the shadows, as paint is)."""
    h, l, s = colorsys.rgb_to_hls(*(v / 255.0 for v in base))
    out = []
    for f in factors:
        ll = max(0.0, min(0.97, l * f))
        ss = max(0.0, min(1.0, s * (1.08 if f < 1 else 0.96)))
        out.append(tuple(round(v * 255) for v in colorsys.hls_to_rgb(h, ll, ss)))
    return out


# Each wood: the owner's painting it follows, and the wood's colour, sampled from that painting's side. A number is a
# painting in their first set (24 stripped logs, left to right, top to bottom, from 0); "2:<row>" is a row of their
# second set (OWNER_BANK below).
WOOD = {
    "chestnut": (11, "b6a075"),
    "larch": (1, "c98634"),
    "maple": (0, "cc9d71"),
    "aspen": (9, "d5d1ce"),
    "fir": (16, "ceb678"),
    "dead": (23, "7a7a7a"),
    "jacaranda": ("2:6", "7a5a5e"),   # the second set's mauve wood (it was painting 7, #a97b74)
    "willow": (22, "c7c785"),
    "palm": (10, "e5d5b2"),
    "cypress": (14, "916558"),
    "redwood": (15, "ab5740"),
    "eucalyptus": (21, "bda281"),
    "mahogany": (2, "7a1f0d"),
    # The second set's western red cedar (#84654d), darkened (lightness 0.410 to 0.355, hue 26 to 23) so that it stands
    # off vanilla spruce (dE 9.6 to 10.0) and our cypress (8.2 to 10.6); docs/features/trees-batch-1.md. Appended last,
    # so every other wood keeps its seeds.
    "cedar": ("2:0", "725543"),
    # Initial batch-2 fallback; owner_art replaces pine with the owner's originals (15 October).
    # Keep this slot so future woods retain their deterministic seeds.
    "pine": (6, "a49a84"),
    "yew": ("2:7", "654135"),
    "bald_cypress": (23, "9c9391"),
    "magnolia": ("2:1", "807d55"),
    "fruitwood": (7, "ab7d76"),
    "persimmon": (18, "3d3733"),
    "juniper": (20, "8b4654"),
    "holly": (10, "b6baa1"),
    "olive": (3, "9c9951"),
    "tamarisk": ("2:6", "a06f7e"),
    "yucca": ("2:3", "68654e"),
    "dragonblood": (12, "689db2"),
    "baobab": (21, "bb9b8e"),
    "fig": (8, "7f6f63"),
    "mango": ("2:4", "938167"),
}
# The owner's second set of painted woods (5 October 2026: "you can use these and use them to recolor for future wood"):
# eight full woods (bark, ends, stripped, planks). Row 6's wood is the jacaranda's and row 0's (western red cedar) the
# cedar's, in WOOD and BARK; the other six wait for new trees, each under the species a panel judged it suits
# (docs/NATURAL_TEXTURES.md, "Adding a new wood"). Each: its row, the
# wood's colour (the stripped side's), the bark's tones (dark to light) and the bark's kind. Only the colours are used;
# the patterns are drawn here. Before one becomes a tree, check it against vanilla's woods and the others here: rows 4
# and 5 are close to each other and to the cedar (row 0), and 2, 4 and 5 lie near vanilla's dark oak and spruce.
OWNER_BANK = {
    "london_plane": (1, "9b8059", pal("676251", "78735f", "847f6b", "9a9583", "a29e8b"), "marked"),
    "black_walnut": (2, "67533c", pal("392e27", "3c322c", "413730", "4d433d", "594f49"), "furrowed"),
    "wenge": (3, "544233", pal("3a3323", "433b2b", "484031", "4f4637", "584c3e"), "furrowed"),
    "elm": (4, "866448", pal("433527", "493b2b", "50412f", "64513b", "6a583f"), "furrowed"),
    "shagbark_hickory": (5, "78573c", pal("524938", "5c5241", "615644", "746753", "857660"), "stringy"),
    "yew": (7, "654135", pal("473729", "564636", "614f3f", "6e5d4a", "766551"), "plated"),
}

# The owner's eucalyptus is speckled in pastels, as rainbow eucalyptus wood is.
EUCALYPTUS_FLECKS = pal("e8a578", "e3a0a0", "9cc0d8", "e6c87a", "f0e0c0")

# Each tree's bark: its colour (dark to light) and its character.
BARK = {
    "fig": (pal("4f4d4a", "686561", "817d79", "989592", "b0adab"), "stringy"),
    "mango": (pal("312d2b", "413c39", "514b48", "615a56", "716965"), "furrowed"),
    "baobab": (pal("61524c", "7b6861", "937e77", "a69691", "baafab"), "plated"),
    "dragonblood": (pal("2f3237", "444950", "5a6068", "6f7680", "878e96"), "streaked"),
    "tamarisk": (pal("312022", "483032", "5e4043", "745153", "896165"), "furrowed"),
    "yucca": (pal("5f5a42", "7c7556", "98906b", "aca688", "c0bca5"), "stringy"),
    "olive": (pal("474a3b", "616451", "7b7f66", "93987e", "aaad99"), "furrowed"),
    "holly": (pal("5e655a", "717a6c", "858e7f", "99a094", "acb2a9"), "plated"),
    "juniper": (pal("372625", "4c3533", "624341", "7a5452", "936562"), "stringy"),
    "persimmon": (pal("141416", "1b1c1f", "232528", "2c2e32", "36383c"), "plated"),
    "yew": (OWNER_BANK["yew"][2], OWNER_BANK["yew"][3]),
    "chestnut": (pal("3a3226", "4c4232", "5e5341", "706552", "857a66"), "furrowed"),
    "larch": (pal("2e1914", "44241a", "5c3222", "74422c", "8e5638"), "plated"),
    "maple": (pal("3a3330", "4d4540", "615751", "776b63", "8c8077"), "furrowed"),
    "aspen": (pal("2c2824", "a9a597", "c9c5b8", "dedace", "f0eee6"), "marked"),
    "fir": (pal("2e2621", "40352d", "54473c", "685849", "7c6a57"), "plated"),
    "dead": (pal("33302d", "4a4642", "615c56", "79736b", "908a81"), "furrowed"),
    "jacaranda": (pal("3b3330", "504641", "665a53", "7c6f66", "938479"), "furrowed"),
    "willow": (pal("2f2a22", "443b30", "5a4e3f", "71634f", "887960"), "furrowed"),
    "palm": (pal("4a4036", "5f5345", "766855", "8c7e68", "a2957c"), "furrowed"),
    "cypress": (pal("3e2620", "55342a", "6c4436", "835543", "996752"), "stringy"),
    "redwood": (pal("3a1a12", "552519", "6e3121", "88402b", "a35238"), "stringy"),
    "eucalyptus": (pal("4d6b4a", "6f8f5a", "8fae6c", "b4c486", "d7d9a4"), "streaked"),
    "mahogany": (pal("2e2420", "433530", "584741", "6e5a52", "856e64"), "plated"),
    # The owner's painted western red cedar bark (second set, row 0: #483229 #513a2f #563d31 #65493a #6b4f40), darkened
    # by the wood's own lightness ratio (0.355 / 0.410), so the log keeps the painting's contrast between bark and wood
    # (dE 14 from the wood; as painted 17; the painted bark beside the darkened wood would be 10, the least of any wood).
    # Stringy, as cedar bark peels in long fibrous strips. docs/features/trees-batch-1.md.
    "cedar": (pal("3e2b24", "463229", "4a352a", "573f32", "5d4437"), "stringy"),
    "pine": (pal("2d2521", "43312a", "5f4433", "805a3d", "9c6c48"), "plated"),
    "bald_cypress": (pal("392d28", "4e3e37", "624e46", "7a6157", "927468"), "stringy"),
    "magnolia": (pal("3b3a30", "515043", "686755", "7e7d67", "95947e"), "plated"),
    "fruitwood": (pal("2e2928", "655a58", "7b6d6b", "90817f", "a39695"), "marked"),
}
EUCALYPTUS_STREAKS = pal("d07a2e", "9a4f9e", "4f7fb8", "c9a63a", "5ea06a")

# Leaves, dark to light (six tones; the darkest stays under the gaps). Each tree's looks as its blocks have them.
LEAVES = {
    "chestnut_leaves": (pal("1b3514", "254a1b", "306024", "3e772d", "508f38", "6aa84a"), "leaves"),
    "larch_needles": (pal("1a3317", "23441e", "2f5826", "3c6c2f", "4d833a", "65a04a"), "needles"),
    "larch_needles_gold": (pal("5a3f0a", "7c5810", "a07418", "c39324", "dbb13a", "edcd62"), "needles"),
    "maple_leaves": (pal("223f12", "2e5418", "3e6c20", "52872b", "6ba13a", "8cbc55"), "leaves"),
    "maple_leaves_red": (pal("3e0a0a", "5e1210", "7e1a16", "9e2620", "bc3a2c", "d65a3e"), "leaves"),
    "maple_leaves_orange": (pal("5a2206", "80360a", "a44d10", "c4661a", "dc8428", "eea548"), "leaves"),
    "maple_leaves_gold": (pal("5c3f06", "82600c", "a78014", "c7a020", "dfbd36", "efd668"), "leaves"),
    "aspen_leaves": (pal("27501a", "356a21", "47852b", "5ea137", "7dbc4e", "a2d571"), "leaves"),
    "aspen_leaves_gold": (pal("6a4f08", "92700e", "b8911a", "d6b02c", "eac94a", "f6e284"), "leaves"),
    "fir_needles": (pal("0d261c", "143525", "1d4530", "27563c", "33684a", "457d5c"), "needles"),
    "jacaranda_leaves": (pal("2a1d52", "3b2a6b", "523b8e", "6b52b0", "8670cc", "a492e0"), "blossom"),
    "willow_leaves": (pal("223f18", "2f5620", "3f6e29", "548834", "6fa244", "93bd62"), "leaves"),
    "willow_leaves_gold": (pal("6a5a0e", "8f7a12", "b39a1c", "cdb52e", "e2cd4a", "f0e27c"), "leaves"),
    "palm_fronds": (pal("173d14", "1f5019", "2b671f", "3a8028", "4f9a34", "6db44c"), "fronds"),
    "cypress_leaves": (pal("0f2a1a", "163522", "1e432b", "275235", "326240", "41754e"), "needles"),
    "redwood_needles": (pal("102414", "16301b", "1d3d22", "264c2b", "315c35", "3f6f42"), "needles"),
    "eucalyptus_leaves": (pal("2c4a44", "3a5f56", "4a7468", "5e8a7c", "77a194", "96bcb0"), "leaves"),
    "mahogany_leaves": (pal("0e2a12", "143719", "1b4520", "245529", "2f6634", "3d7a42"), "leaves"),
    # The cedar's flat sprays of scale-leaves: a muted sage green (hue 84), yellower than the cypress's blue-green and
    # the fir's (mean dE 14.8 and 16.1); evergreen. Appended last, so every other look keeps its seed.
    "cedar_leaves": (pal("27301a", "343f22", "43522c", "536539", "657a46", "7d9657"), "needles"),
    "yew_needles": (pal("111a12", "182419", "1f3021", "2b3d2d", "354c38", "425f45"), "needles"),
    "bald_cypress_needles": (pal("333d19", "485523", "5e702d", "73883a", "8da647", "a2b866"), "needles"),
    "bald_cypress_needles_gold": (pal("392014", "502d1b", "683b24", "7f492f", "9b5a39", "b56e4a"), "needles"),
    "magnolia_leaves": (pal("122c12", "1a3d19", "225020", "2b612a", "357733", "438c40"), "leaves"),
}
LEAF_OPTIONS = {"yew_needles": {"gap_threshold": 0.15}, "bald_cypress_needles": {"gap_threshold": 0.2},
                "bald_cypress_needles_gold": {"gap_threshold": 0.2}, "magnolia_leaves": {"gap_threshold": 0.18}}
# Logs of their own that are no wood set (no planks), drawn as the woods are: the Cinnamon Tree's (tools/spices.py
# CINNAMON). Each: its stripped side's colour and its bark, as in WOOD and BARK. The cinnamon's stripped side is its inner
# bark, which is the spice, so it takes the spice's colour, not one of the owner's paintings; its bark is the smooth
# grey-brown a cinnamon tree's is.
LOGS = {"cinnamon": ("b8743a", (pal("3a302a", "4e433b", "62564c", "776a5e", "8c7e70"), "plated"))}
# Bare deciduous leaves (twigs in the bark's colour), and the chestnut's fruit.
BARE = {"larch_needles_bare": "larch", "maple_leaves_bare": "maple", "aspen_leaves_bare": "aspen",
        "willow_leaves_bare": "willow", "bald_cypress_needles_bare": "bald_cypress"}
LEAF_GREEN = pal("1f3b16", "2b501d", "3a6826", "4c8231", "659c40", "86b85a")
BUR = pal("5a7a20", "7c9c2c", "a2bc3e", "d0d86a")
BUR_DRY = pal("6a4e22", "8c6a30", "b08a44", "d2b064")
NUT = pal("3e2416", "5a3420", "76462a")


def wood_ramp(wood):
    return ramp(rgb(WOOD[wood][1] if wood in WOOD else LOGS[wood][0]))


def bark_of(wood):
    return BARK[wood] if wood in BARK else LOGS[wood][1]


# ---------------------------------------------------------------- bark


def bark(wood, seed):
    """A log's side: vertical furrows in the bark's darkest tones, ridges between, tiling top to bottom."""
    if wood == "yucca":
        return yucca_bark(seed)
    p, kind = bark_of(wood)
    rng = random.Random(seed)
    c = Canvas()
    # Ridges: each column a tone, in runs of two or three, so the bark reads as raised strips.
    tones = []
    while len(tones) < 16:
        tones += [rng.choice((2, 3, 3, 2, 1))] * rng.choice((2, 3))
    for y in range(16):
        for x in range(16):
            c.px(x, y, p[tones[x]])
    if kind == "marked":
        # Aspen: pale bark with short dark marks, as birch's.
        for y in range(16):
            for x in range(16):
                c.px(x, y, p[2 + (1 if rng.random() < 0.55 else 0) + (1 if rng.random() < 0.2 else 0)])
        for _ in range(7):
            x0, y0, w = rng.randrange(16), rng.randrange(16), rng.choice((2, 3, 3, 4))
            for i in range(w):
                c.px((x0 + i) % 16, y0, p[0])
            if rng.random() < 0.4:
                c.px((x0 + 1) % 16, (y0 + 1) % 16, p[1])
        return c.img
    furrows = {"furrowed": 5, "plated": 4, "stringy": 7, "streaked": 3}[kind]
    for i in range(furrows):
        x0 = (i * 16 // furrows + rng.randrange(2)) % 16
        phase, amp = rng.uniform(0, 2 * math.pi), rng.choice((0, 1, 1))
        for y in range(16):
            x = (x0 + round(amp * math.sin(2 * math.pi * y / 16 + phase))) % 16
            c.px(x, y, p[0])
            if kind != "stringy":
                c.px((x + 1) % 16, y, p[1])
    if kind == "streaked":
        # Rainbow eucalyptus: long streaks of colour where the bark has peeled.
        for _ in range(6):
            x0, y0, length = rng.randrange(16), rng.randrange(16), rng.randint(4, 9)
            col = rng.choice(pal("462a2a", "583232", "683a36") if wood == "dragonblood" else EUCALYPTUS_STREAKS)
            for i in range(length):
                c.px(x0, (y0 + i) % 16, col)
                if i % 3 == 1:
                    c.px((x0 + 1) % 16, (y0 + i) % 16, col)
    # Light flecks on the ridges.
    for _ in range(9 if kind != "stringy" else 5):
        x, y = rng.randrange(16), rng.randrange(16)
        if c.get(x, y) != p[0] and c.get(x, y) != p[1]:
            c.px(x, y, p[4])
            if rng.random() < 0.5:
                c.px(x, (y + 1) % 16, p[4 if kind == "stringy" else 3])
    if wood in ("olive", "fig"):
        return bark_details(c.img, p, seed + 11000)
    return c.img


def bark_details(image, palette, seed):
    """Small broken ridges within existing bark bands, retaining the species palette."""
    rng = random.Random(seed)
    c = Canvas()
    c.img = image.copy()
    # Short, narrow patches interrupt broad flat strips without crossing a furrow.
    # Use the original pixels as the mask so marks cannot grow into noisy clusters.
    for _ in range(22):
        x, y0 = rng.randrange(16), rng.randrange(16)
        tone = palette.index(image.getpixel((x, y0))[:3])
        if tone == 0:
            continue
        shade = max(1, min(4, tone + rng.choice((-1, 1))))
        for dy in range(rng.randint(2, 4)):
            y = (y0 + dy) % 16
            if image.getpixel((x, y))[:3] != palette[tone]:
                break
            c.px(x, y, palette[shade])
    # Pair a few recessed nicks with a raised edge, staggered around the tile.
    for x0, y0 in ((2, 3), (7, 10), (12, 5), (5, 15)):
        for dy in range(2):
            x, y = (x0 + dy) % 16, (y0 + dy) % 16
            tone = palette.index(image.getpixel((x, y))[:3])
            if tone < 2:
                continue
            c.px(x, y, palette[tone - 1])
            edge = (x + 1) % 16
            if image.getpixel((edge, y))[:3] == palette[tone]:
                c.px(edge, y, palette[min(4, tone + 1)])
    return c.img


def yucca_bark(seed):
    """Short, overlapping dry fibers with shaded creases and raised pale edges."""
    p = pal("4d4938", "6d674e", "898267", "a39c7d", "b4ae92", "c8c3ac")
    rng = random.Random(seed)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, p[3])
    # Stagger the fiber ends and wrap each plate across the texture boundary.
    # Narrow stepped creases give depth without unbroken dark vertical stripes.
    for x0, width in ((0, 3), (3, 3), (6, 4), (10, 3), (13, 3)):
        y0 = rng.randrange(16)
        lengths = rng.choice(((5, 4, 7), (6, 6, 4), (4, 7, 5)))
        for length in lengths:
            tone = rng.choice((2, 3, 3, 4))
            step = rng.choice((1, length - 2, length - 1))
            for dy in range(length):
                y = (y0 + dy) % 16
                for dx in range(width):
                    c.px(x0 + dx, y, p[tone])
                crease = x0 + (1 if dy >= step else 0)
                c.px(crease, y, p[1 if dy in (0, length - 1) else 0])
                if crease + 1 < x0 + width:
                    c.px(crease + 1, y, p[2 if dy >= length - 2 else 4])
                if 1 <= dy < length - 2:
                    c.px(x0 + width - 1, y, p[5 if tone >= 3 else 3])
            y0 += length
    return c.img


# ---------------------------------------------------------------- log ends, stripped wood and planks


def rings(wood, seed, rim):
    """A log's end: square growth rings to the edge round a dark pith; `rim`, the outer ring's colour (bark, or a darker
    ring of the wood when stripped)."""
    p = wood_ramp(wood)
    rng = random.Random(seed)
    c = Canvas()
    # From the rim inwards: light, a ring line, light, mid, a ring line, light, the pith; soft, as the owner's are.
    band = {7: None, 6: 4, 5: 2, 4: 4, 3: 3, 2: 2, 1: 4, 0: 1}
    for y in range(16):
        for x in range(16):
            r = int(max(abs(x - 7.5), abs(y - 7.5)))
            c.px(x, y, rim(x, y) if r == 7 else p[band[r]])
    # Vanilla's rings are not ruled: a few ring pixels step in or out.
    for _ in range(9):
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        r = int(max(abs(x - 7.5), abs(y - 7.5)))
        if 1 <= r <= 5 and band[r] == 2:
            c.px(x, y, p[3])
    if wood == "eucalyptus":
        for _ in range(10):
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            if band[int(max(abs(x - 7.5), abs(y - 7.5)))] in (3, 4):
                c.px(x, y, rng.choice(EUCALYPTUS_FLECKS))
    return c.img


def log_top(wood, seed):
    bark_p = bark_of(wood)[0]
    return rings(wood, seed, lambda x, y: bark_p[1] if (x + y) % 3 else bark_p[0])


def stripped_top(wood, seed):
    p = wood_ramp(wood)
    return rings(wood, seed, lambda x, y: p[2] if (x + y) % 4 else p[1])


def stripped_side(wood, seed):
    """Stripped wood: straight vertical grain, broken streaks in two darker tones and a few light."""
    p = wood_ramp(wood)
    rng = random.Random(seed)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, p[3])
    for _ in range(12):
        x, y0, length = rng.randrange(16), rng.randrange(16), rng.randint(3, 9)
        tone = rng.choice((1, 2, 2, 2))
        for i in range(length):
            c.px(x, (y0 + i) % 16, p[tone])
            if i == length // 2 and rng.random() < 0.4:
                x = (x + rng.choice((-1, 1))) % 16
    for _ in range(7):
        x, y0, length = rng.randrange(16), rng.randrange(16), rng.randint(2, 5)
        for i in range(length):
            c.px(x, (y0 + i) % 16, p[4])
    if wood == "eucalyptus":
        for _ in range(26):
            x, y = rng.randrange(16), rng.randrange(16)
            c.px(x, y, rng.choice(EUCALYPTUS_FLECKS))
            if rng.random() < 0.5:
                c.px(x, (y + 1) % 16, rng.choice(EUCALYPTUS_FLECKS))
    return c.img


def planks(wood, seed):
    """Four boards a block: lit along the top, a dark seam along the bottom, horizontal grain, a butt joint each."""
    p = wood_ramp(wood)
    rng = random.Random(seed)
    c = Canvas()
    joints = [rng.randrange(2, 14)]
    for _ in range(3):
        x = rng.randrange(2, 14)
        while abs(x - joints[-1]) < 4:
            x = rng.randrange(2, 14)
        joints.append(x)
    for board in range(4):
        y0 = board * 4
        for x in range(16):
            c.px(x, y0, p[4] if rng.random() < 0.8 else p[3])
            c.px(x, y0 + 1, p[3])
            c.px(x, y0 + 2, p[3] if rng.random() < 0.75 else p[2])
            c.px(x, y0 + 3, p[1])
        # Grain: short darker dashes along the board.
        for _ in range(3):
            x0, y = rng.randrange(16), y0 + rng.choice((1, 2))
            for i in range(rng.randint(2, 5)):
                c.px((x0 + i) % 16, y, p[2])
        j = joints[board]
        for y in range(y0, y0 + 3):
            c.px(j, y, p[1])
            c.px(j + 1, y, p[4] if y == y0 else p[3])
        if wood == "eucalyptus":
            for _ in range(5):
                c.px(rng.randrange(16), y0 + rng.choice((1, 2)), rng.choice(EUCALYPTUS_FLECKS))
    return c.img


# ---------------------------------------------------------------- leaves


def leaves(name, seed):
    """Vanilla's speckle: one- and two-pixel leaves in four tones, light on top of each clump, with gaps."""
    p, kind = LEAVES[name]
    return paint_leaves(p, kind, seed, **LEAF_OPTIONS.get(name, {}))


def paint_leaves(p, kind, seed, *, gap_threshold=None, edge_green=None, under_gap=None,
                 flower_coverage=None, midrib_count=3, midrib_length=8, midrib_angle=None):
    """Leaves of six tones `p` (dark to light) in the manner `kind` ("leaves", "needles", "blossom" or "fronds"), as
    leaves() paints each tree's; the orchards' fruit trees (tools/orchard_textures.py) are painted with it too."""
    rng = random.Random(seed)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.img.putpixel((x, y), (under_gap or p[0]) + (0,))
    noise = [[rng.random() for _ in range(16)] for _ in range(16)]
    field = [[0.55 * noise[y][x] + 0.45 * (noise[(y - 1) % 16][x] + noise[y][(x - 1) % 16]) / 2 for x in range(16)]
             for y in range(16)]
    gaps = {"leaves": 0.2, "needles": 0.16, "blossom": 0.18, "fronds": 0.22}[kind] if gap_threshold is None else gap_threshold
    if not 0 <= gaps <= 1 or (flower_coverage is not None and not 0 <= flower_coverage <= 1):
        raise ValueError("Leaf gap threshold and flower coverage must be between zero and one")
    if midrib_count < 0 or midrib_length < 1:
        raise ValueError("Invalid frond midrib count or length")
    for y in range(16):
        for x in range(16):
            v = field[y][x]
            if v < gaps:
                continue
            tone = 1 if v < 0.38 else 2 if v < 0.55 else 3 if v < 0.72 else 4
            # Light falls on the top of each clump: lighter where the pixel above is open or darker.
            if tone < 4 and (y == 0 or field[y - 1][x] < v - 0.18) and rng.random() < 0.5:
                tone += 1
            c.px(x, y, p[tone])
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if not c.empty(x, y):
            c.px(x, y, p[5])
    if kind == "needles":
        # Short slanting strokes of needles, lit along one edge.
        for _ in range(9):
            x0, y0 = rng.randrange(16), rng.randrange(16)
            for i in range(3):
                c.px((x0 + i) % 16, (y0 + i) % 16, p[3 if i else 4])
            c.px((x0 + 1) % 16, y0, p[1])
    elif kind == "blossom":
        # Blossom over a little green: clumps of flowers, the leaves showing at the edges.
        green = edge_green or LEAF_GREEN[2:4]
        opaque = sorted(field[y][x] for y in range(16) for x in range(16) if not c.empty(x, y))
        edge_cutoff = .34 if flower_coverage is None else (float("inf") if flower_coverage == 0 else
            opaque[min(len(opaque) - 1, int(len(opaque) * (1 - flower_coverage)))] if opaque else 0)
        for y in range(16):
            for x in range(16):
                if not c.empty(x, y) and field[y][x] < edge_cutoff:
                    c.px(x, y, green[1 if field[y][x] > edge_cutoff - .07 else 0])
    elif kind == "fronds":
        # Long leaflets crossing the tile on a lit midrib.
        for _ in range(midrib_count):
            x0, y0 = rng.randrange(16), rng.randrange(16)
            for i in range(midrib_length):
                dx, dy = (i, i // 2) if midrib_angle is None else (round(i * math.cos(math.radians(midrib_angle))), round(i * math.sin(math.radians(midrib_angle))))
                c.px((x0 + dx) % 16, (y0 + dy) % 16, p[4])
    return c.img


def bare(name, seed):
    """Bare twigs in the bark's colour, crossing and branching, the rest open."""
    if name == "bald_cypress_needles_bare":
        return cypress_bare(seed)
    p = BARK[BARE[name]][0]
    rng = random.Random(seed)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.img.putpixel((x, y), p[0] + (0,))
    for _ in range(4):
        x, y = rng.uniform(0, 16), rng.uniform(0, 16)
        angle = rng.uniform(0, 2 * math.pi)
        for i in range(14):
            c.px(x % 16, y % 16, p[1 if i % 3 else 2])
            if i % 5 == 4:
                bx, by, ba = x, y, angle + rng.choice((-0.9, 0.9))
                for j in range(4):
                    c.px(bx % 16, by % 16, p[2])
                    bx += math.cos(ba)
                    by += math.sin(ba)
            x += math.cos(angle)
            y += math.sin(angle)
            angle += rng.uniform(-0.3, 0.3)
    return c.img


def cypress_bare(seed, palette=None):
    """One joined, tapering branch network; shared edge anchors connect neighbouring faces/tiles."""
    p = BARK["bald_cypress"][0] if palette is None else palette
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.img.putpixel((x, y), p[0] + (0,))
    paths = [
        ([(8, 16), (8, 12), (7, 9), (9, 5), (8, 1), (8, -1)], (-1, 0)),
        ([(-1, 8), (3, 7), (6, 8), (8, 8), (12, 9), (16, 8)], (0, -1)),
        ([(8, 12), (4, 10), (2, 5), (1, 2)], (-1, 0)),
        ([(8, 8), (12, 5), (13, 2), (15, 1)], (0, 1)),
        ([(8, 4), (5, 3), (3, 1)], (0, 1)),
        ([(3, 7), (1, 10), (1, 13)], (1, 0)),
        ([(12, 9), (13, 12), (11, 14)], (-1, 0)),
    ]
    for points, (dx, dy) in paths:
        for a, b in zip(points, points[1:]):
            c.line(a[0] + dx, a[1] + dy, b[0] + dx, b[1] + dy, p[1])
            c.line(*a, *b, p[2])
    rng = random.Random(seed)
    for y in range(1, 15):
        for x in range(1, 15):
            if not c.empty(x, y) and rng.random() < .16:
                c.px(x, y, p[3])
    # Identical two-pixel ports on all four edges join around corners as well as across a wall.
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            c.img.putpixel((x, y), (p[2] + (255,)) if i in (7, 8) else p[0] + (0,))
    return c.img


def chestnut_fruit(base, fruit):
    """The chestnut's leaves with green spiny burs (1), or ripe ones split open on their nuts (2)."""
    c = Canvas()
    c.img = base.copy()
    for cx, cy in ((4, 5), (11, 10), (12, 2)):
        if fruit == 1:
            for dx in range(-1, 2):
                for dy in range(-1, 2):
                    c.px(cx + dx, cy + dy, BUR[2 if dx + dy < 0 else 1])
            for sx, sy in ((-2, 0), (2, 0), (0, -2), (0, 2), (-2, -2), (2, 2), (2, -2), (-2, 2)):
                c.px(cx + sx, cy + sy, BUR[3])
        else:
            for dx in range(-1, 2):
                for dy in range(-1, 2):
                    c.px(cx + dx, cy + dy, BUR_DRY[1 if dx + dy < 0 else 0])
            c.px(cx, cy, NUT[2])
            c.px(cx - 1, cy, NUT[1])
            c.px(cx, cy + 1, NUT[0])
            for sx, sy in ((-2, -1), (2, 1), (1, -2), (-1, 2)):
                c.px(cx + sx, cy + sy, BUR_DRY[3])
    return c.img


# ---------------------------------------------------------------- every texture


def wood_textures():
    """Every wood's bark, log end, stripped wood and end, and planks; every tree's leaves in each of its looks."""
    out = {}
    for index, wood in enumerate(WOOD):
        out[("block", f"{wood}_log")] = bark(wood, 700 + index)
        out[("block", f"{wood}_log_top")] = log_top(wood, 720 + index)
        out[("block", f"stripped_{wood}_log")] = stripped_side(wood, 740 + index)
        out[("block", f"stripped_{wood}_log_top")] = stripped_top(wood, 760 + index)
        out[("block", f"{wood}_planks")] = planks(wood, 780 + index)
    for index, wood in enumerate(LOGS):
        out[("block", f"{wood}_log")] = bark(wood, 860 + index)
        out[("block", f"{wood}_log_top")] = log_top(wood, 870 + index)
        out[("block", f"stripped_{wood}_log")] = stripped_side(wood, 880 + index)
        out[("block", f"stripped_{wood}_log_top")] = stripped_top(wood, 890 + index)
    for index, name in enumerate(LEAVES):
        # The new cypress keeps exactly the same needle arrangement through its colour change.
        seed = 800 + list(LEAVES).index("bald_cypress_needles") if name.startswith("bald_cypress_needles") else 800 + index
        out[("block", name)] = leaves(name, seed)
    for index, name in enumerate(BARE):
        out[("block", name)] = bare(name, 840 + index)
    from tree_roster_textures import textures as roster_textures
    out.update(roster_textures(out))
    from seasonal_fruit_textures import textures as seasonal_fruit_textures
    out.update(seasonal_fruit_textures())
    from hawthorn_persimmon_textures import textures as hawthorn_persimmon_textures
    out.update(hawthorn_persimmon_textures())
    from evergreen_scrub_textures import textures as evergreen_scrub_textures
    out.update(evergreen_scrub_textures())
    from holly_textures import textures as holly_textures
    out.update(holly_textures())
    from olive_textures import textures as olive_textures, figure
    out.update(olive_textures())
    from dry_country_textures import textures as dry_country_textures
    out.update(dry_country_textures())
    from fungal_trees_textures import textures as fungal_textures
    out.update(fungal_textures())
    from hanging_fruit_textures import textures as hanging_textures
    out.update(hanging_textures())
    from tropical_tree_textures import textures as tropical_textures
    out.update(tropical_textures())
    from rainforest_textures import textures as rainforest_textures
    out.update(rainforest_textures())
    out["block", "olive_planks"] = figure(out["block", "olive_planks"])
    chestnut = out[("block", "chestnut_leaves")]
    out[("block", "chestnut_leaves_burs")] = chestnut_fruit(chestnut, 1)
    out[("block", "chestnut_leaves_ripe")] = chestnut_fruit(chestnut, 2)
    return out
