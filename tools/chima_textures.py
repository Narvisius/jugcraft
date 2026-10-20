"""Chima the lion cub's textures (docs/features/chima.md), drawn from its designer's sketches onto the box layout of
client/chima/ChimaModel.java: its own look and the four it takes on (pumpkin, zombie, skeleton, shark), each wild and
tamed (with a red collar), and the spawn egg's 16x16 icon.

The model's layout is 128 by 64 texture units; these are drawn at twice that, 256 by 128, so the face gets fine eyes.
Colours are the sketches' own flat colours with a little grain. Every look shares the one model: what a look does not
have (the mane's spikes on the round pumpkin, skull and shark, the pumpkin's stem on the others, the skeleton's flesh
between its ribs) is left transparent.

Called from chima_data.py; run alone to redraw them.
"""
import random
from pathlib import Path

from PIL import Image

UNITS_W, UNITS_H = 128, 64
SCALE = 2
# (texture u, v, width, height, depth) of each box, in texture units, as ChimaModel.createLayer() places them. The two
# spike boxes are laid out for the largest spike; smaller spikes use part of the same, evenly coloured, area.
BOXES = {
    "skull": (0, 0, 12, 12, 9),
    "head_wide": (0, 21, 14, 8, 8),
    "head_tall": (44, 0, 8, 14, 8),
    "spike_right": (44, 22, 6, 6, 6),
    "spike_left": (68, 22, 6, 6, 6),
    "fin": (76, 0, 1.5, 5, 5),
    "stem": (90, 0, 2, 3, 2),
    "body": (0, 37, 7, 5, 9),
    "front_leg": (32, 37, 3, 3, 3),
    "back_leg": (32, 43, 3, 3, 3),
    "tail": (44, 37, 1, 1, 5),
    "tail_end": (44, 43, 1, 1, 5),
    "tuft": (56, 37, 3, 3, 3),
    "tuft_star": (56, 43, 3.5, 3.5, 3.5),
}
HEAD = ("skull", "head_wide", "head_tall")
SKINS = ["normal", "pumpkin", "zombie", "skeleton", "shark"]
CLEAR = (0, 0, 0, 0)

ORANGE, ORANGE_TOP, ORANGE_UNDER = (255, 127, 39), (255, 152, 72), (226, 104, 28)
YELLOW, YELLOW_TOP, YELLOW_UNDER = (255, 236, 24), (255, 246, 120), (226, 200, 12)
INK, WHITE, EYE_BLUE, BLUSH = (40, 26, 22), (255, 255, 255), (178, 212, 240), (255, 148, 160)
TAIL = (48, 34, 30)
COLLAR, TAG = (206, 34, 44), (255, 208, 64)


def faces(box):
    """Each face of a box as a texel rectangle (x, y, width, height): vanilla's box unwrap, doubled. The front's left
    column is the cub's right side; the right side's columns run from its back to its front, the left side's from its
    front to its back; the top's first row is its back edge."""
    u, v, w, h, d = (round(n * SCALE) for n in BOXES[box])
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (UNITS_W * SCALE, UNITS_H * SCALE), CLEAR)
        self.random = random.Random(seed)

    def px(self, x, y, colour):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((int(x), int(y)), tuple(colour[:3]) + (255,) if len(colour) == 3 else colour)

    def rect(self, x, y, w, h, colour, grain=0):
        for j in range(int(y), int(y + h)):
            for i in range(int(x), int(x + w)):
                self.px(i, j, vary(colour, self.random.randint(-grain, grain)) if grain else colour)

    def box(self, box, colour, top=None, under=None, grain=5, only=None):
        """Every face (or those in {@code only}) of a box in {@code colour} with a little grain, the top {@code top}
        and the underside {@code under} when given."""
        for name, rect in faces(box).items():
            if only and name not in only:
                continue
            self.rect(*rect, top if name == "up" and top else under if name == "down" and under else colour, grain)

    def clear(self, box, names=None):
        for name, rect in faces(box).items():
            if names is None or name in names:
                self.rect(*rect, CLEAR)

    def at(self, box, name, i, j, colour, w=1, h=1):
        x, y, _, _ = faces(box)[name]
        self.rect(x + i, y + j, w, h, colour)

    def each(self, box, name, colour_of):
        """Repaints each texel of a face with {@code colour_of(i, j)}; None leaves it."""
        x, y, w, h = faces(box)[name]
        for j in range(h):
            for i in range(w):
                c = colour_of(i, j)
                if c is not None:
                    self.px(x + i, y + j, c)

    def grid(self, box, name, rows, palette, ox=0, oy=0):
        """Draws rows of characters onto a face from texel (ox, oy); characters not in the palette leave what is there."""
        x, y, _, _ = faces(box)[name]
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in palette:
                    self.px(x + ox + i, y + oy + j, palette[ch])

    def collar(self):
        """The tame cub's red collar round its chest, below the great head, with a gold tag in front."""
        x, y, w, h = faces("body")["front"]
        self.rect(x, y + 3, w, 3, COLLAR)
        self.rect(x + w // 2 - 1, y + 6, 2, 2, TAG)
        x, y, d, h = faces("body")["right"]
        self.rect(x + d - 4, y, 3, h, COLLAR)
        x, y, d, h = faces("body")["left"]
        self.rect(x + 1, y, 3, h, COLLAR)
        x, y, w, d = faces("body")["up"]
        self.rect(x, y + d - 4, w, 3, COLLAR)
        x, y, w, d = faces("body")["down"]
        self.rect(x, y + 1, w, 3, COLLAR)


def vary(colour, k):
    return tuple(max(0, min(255, c + k)) for c in colour[:3])


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def mirror(rows):
    return [row[::-1] for row in rows]


def face_rows(left_eye, right_eye, ex, width=24):
    """Two eye grids side by side on a face's rows: the left one from column {@code ex}, the right one as far in from the
    other edge."""
    rows = []
    for j in range(len(left_eye)):
        row = ["."] * width
        for i, ch in enumerate(left_eye[j]):
            row[ex + i] = ch
        for i, ch in enumerate(right_eye[j]):
            row[width - ex - len(right_eye[j]) + i] = ch
        rows.append("".join(row))
    return rows


# Chima's own eyes: big and white with a pale blue lower lid, their tops slanting down towards the middle.
LION_EYE = [
    "OOOO...",
    "OWWWOO.",
    "OWWWWWO",
    "OWWWWWO",
    "OBBBBBO",
    ".OOOOO.",
]
CAT_MOUTH = [
    "O......O",
    ".O.OO.O.",
    "..O..O..",
]


def lion_face(p):
    p.grid("skull", "front", face_rows(LION_EYE, mirror(LION_EYE), 3), {"O": INK, "W": WHITE, "B": EYE_BLUE}, 0, 10)
    p.grid("skull", "front", CAT_MOUTH, {"O": INK}, 8, 17)
    p.grid("skull", "front", ["PP..................PP"], {"P": BLUSH}, 1, 17)


def spiky_mane(p, colour, top, under):
    for box in HEAD:
        p.box(box, colour, top, under)
    p.box("spike_right", colour, grain=7)
    p.box("spike_left", colour, grain=7)
    p.box("fin", colour, top)


def lion_body(p, colour=YELLOW, top=YELLOW_TOP, under=YELLOW_UNDER):
    p.box("body", colour, top, under)
    for leg in ("front_leg", "back_leg"):
        p.box(leg, colour, top, under)
        toes(p, leg, mix(colour, INK, 0.35))


def toes(p, leg, colour):
    """Two little toe lines at the foot of a leg's front."""
    p.at(leg, "front", 2, 4, colour, 1, 2)
    p.at(leg, "front", 4, 4, colour, 1, 2)


def tail(p, colour, tip=None, star=None):
    p.box("tail", colour, grain=3)
    p.box("tail_end", colour, grain=3)
    if tip:
        p.box("tuft", tip, grain=6)
    else:
        p.clear("tuft")
    if star:
        p.box("tuft_star", star, grain=6)
    else:
        p.clear("tuft_star")


def normal(p):
    spiky_mane(p, ORANGE, ORANGE_TOP, ORANGE_UNDER)
    lion_face(p)
    p.clear("stem")
    lion_body(p)
    tail(p, TAIL, ORANGE, ORANGE)


PUMPKIN, PUMPKIN_TOP, PUMPKIN_RIB, PUMPKIN_UNDER = (240, 128, 30), (250, 152, 52), (198, 90, 20), (206, 98, 22)
GLOW, GLOW_HOT, CARVE = (255, 210, 60), (255, 242, 150), (110, 46, 8)
STEM, STEM_TOP = (86, 98, 38), (118, 132, 56)
PUMPKIN_EYE = [
    "OO......",
    "OGOO....",
    "OGHGOOO.",
    "OGHHHHGO",
    ".OGGGGO.",
    "..OOOO..",
]


def pumpkin(p):
    for box in HEAD:
        p.box(box, PUMPKIN, PUMPKIN_TOP, PUMPKIN_UNDER)
        for name in ("front", "back", "right", "left"):
            step = 5 if faces(box)[name][2] > 8 else 4
            p.each(box, name, lambda i, j, step=step: PUMPKIN_RIB if (i + 2) % step == 0 else None)
    for box in ("spike_right", "spike_left", "fin", "tuft_star"):
        p.clear(box)
    p.grid("skull", "front", face_rows(PUMPKIN_EYE, mirror(PUMPKIN_EYE), 2), {"O": CARVE, "G": GLOW, "H": GLOW_HOT}, 0, 9)
    p.grid("skull", "front", CAT_MOUTH, {"O": CARVE}, 8, 17)
    p.box("stem", STEM, STEM_TOP)
    lion_body(p)
    tail(p, TAIL, PUMPKIN)
    # The tail's little jack o'lantern, grinning on every side.
    for name in ("front", "back", "right", "left"):
        p.grid("tuft", name, ["......", ".G..G.", "......", "GGGGGG", ".G.G.G", "......"], {"G": GLOW})


GREEN, GREEN_TOP, GREEN_UNDER = (70, 156, 56), (96, 178, 74), (52, 124, 42)
FLESH, FLESH_TOP, FLESH_UNDER = (236, 104, 162), (246, 140, 186), (204, 80, 136)
DIRT, DIRT_DARK, BLOOD, STITCH = (112, 72, 40), (88, 44, 40), (172, 30, 42), (34, 26, 26)
SOCKET, RIM = (112, 40, 72), (150, 34, 34)
ZOMBIE_EYE = [
    "RR.....",
    "RWRR...",
    "RWWWRR.",
    "RWWWWWR",
    ".RWWWR.",
    "..RRR..",
]
HOLLOW_EYE = [
    ".OOOOO.",
    "OWWWWWO",
    "OWWWWWO",
    "OWWWWWO",
    "OMMMMMO",
    ".OOOOO.",
]
ZOMBIE_MOUTH = [
    ".RRRR.",
    "RKFKKR",
    "RKKKKR",
    ".RKKR.",
    "..R...",
]


def zombie(p):
    # Its right half (the left of its face as you look at it) green, its left half pink, stitched down the middle.
    wobble = lambda j: [0, 0, 1, 1, 0, -1, -1, 0][(j // 3) % 8]
    for box in HEAD:
        p.box(box, GREEN, GREEN_TOP, GREEN_UNDER, only=("right", "up", "down", "front", "back"))
        p.box(box, FLESH, FLESH_TOP, FLESH_UNDER, only=("left",))
        for name in ("front", "up", "down", "back"):
            x, y, w, h = faces(box)[name]
            pink = FLESH_TOP if name == "up" else FLESH_UNDER if name == "down" else FLESH
            for j in range(h):
                s = w // 2 + wobble(j)
                for i in (range(0, s) if name == "back" else range(s, w)):
                    p.px(x + i, y + j, vary(pink, p.random.randint(-5, 5)))
                if j % 4 == 1:
                    p.rect(x + s - 2, y + j, 4, 1, STITCH)
    p.box("spike_right", GREEN, grain=7)
    p.box("spike_left", FLESH, grain=7)
    p.box("fin", GREEN, GREEN_TOP)
    p.clear("stem")
    p.grid("skull", "front", face_rows(ZOMBIE_EYE, HOLLOW_EYE, 3), {"R": RIM, "W": WHITE, "O": STITCH, "M": SOCKET}, 0, 10)
    p.grid("skull", "front", ZOMBIE_MOUTH, {"R": BLOOD, "K": (30, 14, 14), "F": WHITE}, 6, 16)
    # A green body grubby with earth, a pink patch stitched over its left haunch, pink paws.
    p.box("body", GREEN, GREEN_TOP, GREEN_UNDER)
    for name in ("right", "left", "up", "front", "back"):
        x, y, w, h = faces("body")[name]
        for _ in range(w * h // 14):
            i, j = p.random.randrange(w), p.random.randrange(h)
            p.rect(x + i, y + j, 2, 1, p.random.choice((DIRT, DIRT_DARK)))
    x, y, d, h = faces("body")["left"]
    p.rect(x + d - 9, y + 1, 8, h - 2, FLESH, 5)
    for j in range(1, h - 1, 3):
        p.rect(x + d - 10, y + j, 2, 1, STITCH)
    x, y, w, d = faces("body")["up"]
    p.rect(x + w - 6, y + 1, 5, 7, FLESH_TOP, 5)
    for leg in ("front_leg", "back_leg"):
        p.box(leg, GREEN, GREEN_TOP, FLESH_UNDER)
        for name in ("front", "back", "right", "left"):
            x, y, w, h = faces(leg)[name]
            p.rect(x, y + h - 2, w, 2, FLESH, 4)
            p.rect(x + 1, y + h - 3, 1, 1, STITCH)
            p.rect(x + w - 2, y + h - 3, 1, 1, STITCH)
    # The tail's stitched-up ball.
    tail(p, TAIL, (206, 220, 232))
    for name in ("front", "back", "right", "left", "up"):
        p.grid("tuft", name, ["......", ".K..K.", "..KK..", "..KK..", ".K..K.", "......"], {"K": STITCH})


BONE, BONE_SHADE, BONE_LINE, HOLLOW, CRACK = (242, 240, 230), (212, 210, 198), (150, 148, 140), (24, 20, 20), (140, 138, 130)
SKULL_EYE = [
    ".KKK.",
    "KKKKK",
    "KKKKK",
    "KKKKK",
    "KKKKK",
    ".KKK.",
]


def rib(zi):
    """Whether texel row {@code zi} (counted from the back of the body) is one of its ribs."""
    return 3 <= zi <= 16 and (zi - 3) % 4 < 2


def skeleton(p):
    for box in HEAD:
        p.box(box, BONE, BONE, BONE_SHADE, grain=3)
    for box in ("spike_right", "spike_left", "fin", "stem", "tuft", "tuft_star"):
        p.clear(box)
    p.grid("skull", "front", face_rows(SKULL_EYE, SKULL_EYE, 4), {"K": HOLLOW}, 0, 10)
    p.grid("skull", "front", CAT_MOUTH, {"O": HOLLOW}, 8, 17)
    p.grid("skull", "front", ["...C", "..C.", "..CC", "...C", "..C."], {"C": CRACK}, 14, 0)
    p.grid("head_tall", "up", ["......C.........", ".....C..........", ".....CC........."], {"C": CRACK}, 0, 13)
    # Bare bones: a spine along the back, ribs round the sides, a breastbone and hips; nothing between them.
    p.clear("body")
    x, y, w, d = faces("body")["up"]
    for j in range(d):
        p.rect(x + w // 2 - 1, y + j, 2, 1, BONE)
        if rib(j):
            p.rect(x, y + j, w, 1, BONE_SHADE if j % 2 else BONE)
    x, y, w, d = faces("body")["down"]
    for j in range(d):
        if rib(j):
            p.rect(x, y + j, w, 1, BONE_SHADE)
    x, y, d, h = faces("body")["right"]
    for i in range(d):
        if rib(i):
            p.rect(x + i, y, 1, h, BONE if i % 2 else BONE_SHADE)
    x, y, d, h = faces("body")["left"]
    for i in range(d):
        if rib(d - 1 - i):
            p.rect(x + i, y, 1, h, BONE if i % 2 == 0 else BONE_SHADE)
    x, y, w, h = faces("body")["front"]
    p.rect(x + w // 2 - 1, y, 2, h, BONE)
    p.rect(x, y, w, 2, BONE)
    x, y, w, h = faces("body")["back"]
    p.rect(x + 2, y, w - 4, h - 3, BONE)
    p.rect(x + w // 2 - 1, y + h - 3, 2, 3, BONE_SHADE)
    for leg in ("front_leg", "back_leg"):
        p.box(leg, BONE, BONE, BONE_SHADE, grain=3)
        for name in ("front", "back", "right", "left"):
            x, y, w, h = faces(leg)[name]
            p.rect(x, y + 2, w, 1, BONE_LINE)
        toes(p, leg, BONE_LINE)
    # A tail of little vertebrae.
    for box in ("tail", "tail_end"):
        p.box(box, BONE, grain=2)
        for name in ("right", "left", "up", "down"):
            x, y, w, h = faces(box)[name]
            for k in range(1, max(w, h), 3):
                if name in ("right", "left"):
                    p.rect(x + k, y, 1, h, BONE_LINE)
                else:
                    p.rect(x, y + k, w, 1, BONE_LINE)


SHARK, SHARK_TOP, SHARK_UNDER, SHARK_DEEP = (40, 92, 190), (62, 116, 212), (30, 72, 156), (30, 70, 146)
BELLY, MOUTH, MOUTH_DARK, GILL = (176, 206, 246), (240, 164, 232), (200, 110, 190), (22, 46, 104)
SAND, SAND_TOP, KHAKI, STAR_PINK = (250, 232, 110), (255, 244, 156), (196, 184, 96), (242, 170, 242)


def shark(p):
    for box in HEAD:
        p.box(box, SHARK, SHARK_TOP, BELLY)
    for box in ("spike_right", "spike_left", "stem"):
        p.clear(box)
    p.box("fin", SHARK, SHARK_TOP)
    # Its face is a shark's: a wide toothy mouth over a pale belly.
    w = faces("skull")["front"][2]

    def front(i, j):
        if j >= 13:
            return vary(BELLY, p.random.randint(-3, 3))
        if 3 <= j <= 12 and 2 <= i <= w - 3:
            if j in (3, 12) or i in (2, w - 3):
                return INK
            if (j == 4 and i % 3 != 1) or (j == 11 and i % 3 != 2) or (j in (5, 10) and i % 3 == 0):
                return WHITE
            return MOUTH if 5 <= j <= 10 else MOUTH_DARK
        return None
    p.each("skull", "front", front)
    for box in ("head_wide", "head_tall"):
        x, y, w, h = faces(box)["front"]
        p.rect(x, y + h // 2, w, h - h // 2, BELLY, 3)
    # Eyes and gills on the sides.
    for name, eye_col, gill_col in (("right", 11, 3), ("left", 3, 8)):
        p.at("head_wide", name, eye_col, 3, INK, 2, 3)
        p.at("head_wide", name, eye_col + (1 if name == "right" else 0), 3, WHITE)
        for j in (9, 11, 13):
            p.at("head_wide", name, gill_col, j, GILL, 5, 1)
    # Blue at the front, sand-yellow behind, the line between them ragged.
    p.box("body", SHARK, SHARK_TOP, BELLY, only=("front", "down"))
    p.box("body", SAND, SAND_TOP, only=("back",))
    edge = lambda k: 8 + [0, 1, 0, -1, 1, 0, -1, 0, 1, 0][k % 10]
    for name in ("right", "left", "up"):
        x, y, a, b = faces("body")[name]
        span, along = (b, a) if name == "up" else (a, b)
        for k in range(along):
            for z in range(span):
                from_back = span - 1 - z if name == "left" else z
                yellow = from_back < edge(k)
                c = (SAND_TOP if yellow else SHARK_TOP) if name == "up" else (SAND if yellow else SHARK)
                if name == "up":
                    p.px(x + k, y + z, vary(c, p.random.randint(-4, 4)))
                else:
                    p.px(x + z, y + k, vary(c, p.random.randint(-4, 4)))
    p.box("front_leg", SHARK_DEEP, SHARK, SHARK_UNDER)
    p.box("back_leg", KHAKI, SAND, mix(KHAKI, INK, 0.2))
    tail(p, SAND, STAR_PINK, STAR_PINK)


DRAW = {"normal": normal, "pumpkin": pumpkin, "zombie": zombie, "skeleton": skeleton, "shark": shark}


def texture(skin, tame):
    p = Painter(SKINS.index(skin))
    DRAW[skin](p)
    if tame:
        p.collar()
    return p.img


def spawn_egg():
    """The spawn egg: a yellow egg with orange spiky spots, lit from the top left, outlined in its own darks."""
    rows = [
        "................",
        "......dddd......",
        ".....dHYYYd.....",
        "....dHYoYYYd....",
        "....dYoooYYd....",
        "...dYYYoYYYYd...",
        "...dYYYYYYoYd...",
        "...dYoYYYoooYd..",
        "...dooYYYYoYYd..",
        "...dYoYYYYYYYd..",
        "...dYYYYoYYYyd..",
        "....dYYoooYyd...",
        "....dYYYoYyyd...",
        ".....dYYYyyd....",
        "......ddddd.....",
        "................",
    ]
    tones = {"d": (92, 64, 12), "Y": YELLOW, "y": YELLOW_UNDER, "H": (255, 246, 160), "o": ORANGE}
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in tones:
                img.putpixel((x, y), tones[ch] + (255,))
    return img


def save_all(assets):
    """Writes every look, wild and tame, and the spawn egg's icon, under {@code assets} (assets/jugcraft)."""
    entity = assets / "textures" / "entity"
    entity.mkdir(parents=True, exist_ok=True)
    for skin in SKINS:
        texture(skin, False).save(entity / f"chima_{skin}.png", compress_level=0)
        texture(skin, True).save(entity / f"chima_{skin}_tame.png", compress_level=0)
    item = assets / "textures" / "item"
    item.mkdir(parents=True, exist_ok=True)
    spawn_egg().save(item / "chima_spawn_egg.png", compress_level=0)


if __name__ == "__main__":
    save_all(Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft")
