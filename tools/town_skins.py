"""Skins for the town's townsfolk, drawn by code: 64 x 64 textures in the player skin layout (the townsfolk use a
player-shaped model, town/TownsfolkModel.java), with the outer layer for hats, hoods, helmets, collars and aprons.
Every skin is original. Written to assets/jugcraft/textures/entity/townsfolk/<name>.png.
"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "entity" / "townsfolk"

# (texture u, v, box width, height, depth) of each part, as the player model lays them out (classic arms).
PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "right_arm": (40, 16, 4, 12, 4), "right_sleeve": (40, 32, 4, 12, 4),
    "left_arm": (32, 48, 4, 12, 4), "left_sleeve": (48, 48, 4, 12, 4),
    "right_leg": (0, 16, 4, 12, 4), "right_pants": (0, 32, 4, 12, 4),
    "left_leg": (16, 48, 4, 12, 4), "left_pants": (0, 48, 4, 12, 4),
}
OVERLAY = {"head": "hat", "body": "jacket", "right_arm": "right_sleeve", "left_arm": "left_sleeve",
           "right_leg": "right_pants", "left_leg": "left_pants"}

SKIN_TONES = [(244, 208, 177), (230, 188, 152), (205, 156, 118), (170, 120, 84), (128, 86, 58), (96, 64, 44)]
HAIR = [(40, 28, 20), (90, 60, 34), (150, 104, 56), (200, 160, 90), (120, 40, 20), (170, 170, 170), (30, 30, 34)]
EYES = [(60, 90, 160), (70, 120, 70), (90, 60, 40), (50, 50, 60)]


def faces(part):
    """Each face's rectangle (x, y, w, h) on the texture: top, bottom, right, front, left, back."""
    u, v, w, h, d = PARTS[part]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


class Skin:
    def __init__(self, seed, clean=False):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)
        self.clean = clean

    def fill(self, part, colour, sides=None, rows=None, noise=8, alpha=255, folds=True):
        """Colours a part's faces (all, or the named ones), optionally only rows [r0, r1) of the side faces counted
        from the top, with a little noise and a shade from top (light) to bottom (dark).

        A clean skin (the townsfolk's) has no noise, in the manner of the vanilla skins: cloth is lit in its upper half,
        a shade darker below and darker again at the hem; bare skin (`folds=False`) keeps its sides flat."""
        for name, (x, y, w, h) in faces(part).items():
            if sides and name not in sides:
                continue
            for j in range(h):
                if rows and name not in ("top", "bottom") and not rows[0] <= j < rows[1]:
                    continue
                if rows and name == "top" and rows[0] > 0:
                    continue
                if rows and name == "bottom" and rows[1] < PARTS[part][3]:
                    continue
                if name in ("top", "bottom"):
                    f = 1.06 if name == "top" else 0.8
                elif not self.clean:
                    f = 1.0 - 0.18 * (j / max(1, h - 1))
                elif not folds:
                    f = 1.0
                else:
                    f = 0.84 if j == h - 1 else 0.92 if j >= h // 2 else 1.0
                for i in range(w):
                    n = self.rng.randint(-noise, noise)
                    if self.clean:
                        n = 0  # drawn all the same, so each seed keeps the hair style it gave before
                    self.px[x + i, y + j] = tuple(max(0, min(255, int(c * f) + n)) for c in colour) + (alpha,)

    def dot(self, part, face, i, j, colour, alpha=255):
        x, y, w, h = faces(part)[face]
        if 0 <= i < w and 0 <= j < h:
            self.px[x + i, y + j] = tuple(colour) + (alpha,)

    def clear(self, part):
        for name, (x, y, w, h) in faces(part).items():
            for j in range(h):
                for i in range(w):
                    self.px[x + i, y + j] = (0, 0, 0, 0)

    def save(self, path):
        self.img.save(path, optimize=True)


def strands(s, hair, style):
    """Hair in neat strands, a shade darker on a regular slant across the crown, the back and the sides."""
    strand = darker(hair, 0.82)
    rows = {"top": 8, "back": 7 if style == "long" else 5, "right": 5 if style == "long" else 3}
    rows["left"] = rows["right"]
    for face, h in rows.items():
        for j in range(h):
            for i in range(8):
                if (i + 2 * j) % 5 == 0:
                    s.dot("head", face, i, j, strand)


def darker(c, f=0.75):
    return tuple(int(v * f) for v in c)


def lighter(c, f=1.2):
    return tuple(min(255, int(v * f)) for v in c)


def person(seed, outfit, clean=False):
    """A townsperson: skin, face and hair from the seed, then the outfit (a function drawing the clothes). A clean
    person (the townsfolk) is drawn without noise, its hair in neat strands."""
    s = Skin(seed, clean)
    rng = s.rng
    skin = rng.choice(SKIN_TONES)
    hair = rng.choice(HAIR)
    eyes = rng.choice(EYES)
    for part in ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"):
        s.fill(part, skin, noise=3, folds=False)
    # Face: eyes, brows, nose shade, mouth.
    for i in (1, 2, 5, 6):
        s.dot("head", "front", i, 4, (245, 245, 245))
    s.dot("head", "front", 2, 4, eyes)
    s.dot("head", "front", 5, 4, eyes)
    for i in (1, 2, 5, 6):
        s.dot("head", "front", i, 3, darker(hair, 0.9))
    s.dot("head", "front", 3, 5, darker(skin, 0.88))
    s.dot("head", "front", 4, 5, darker(skin, 0.88))
    for i in (3, 4):
        s.dot("head", "front", i, 6, (150, 80, 70))
    # Hair: crown, back and sides, a fringe; long hair falls down the back.
    style = rng.choice(["short", "short", "long", "bald", "bun"])
    if style != "bald":
        s.fill("head", hair, sides=["top"], noise=10)
        s.fill("head", hair, sides=["back"], rows=(0, 7 if style == "long" else 5), noise=10)
        s.fill("head", hair, sides=["right", "left"], rows=(0, 5 if style == "long" else 3), noise=10)
        for i in range(8):
            s.dot("head", "front", i, 0, hair)
            s.dot("head", "front", i, 1, hair if i not in (3,) or style == "long" else skin)
        if style == "long":
            s.dot("head", "front", 0, 2, hair)
            s.dot("head", "front", 7, 2, hair)
            s.dot("head", "front", 0, 3, hair)
            s.dot("head", "front", 7, 3, hair)
        if clean:
            strands(s, hair, style)
    else:
        s.fill("head", darker(skin, 0.95), sides=["top"], noise=3)
        for i in range(1, 7):
            s.dot("head", "front", i, 6, hair)
            s.dot("head", "front", i, 7, hair)
        s.dot("head", "front", 3, 6, (150, 80, 70))
        s.dot("head", "front", 4, 6, (150, 80, 70))
    outfit(s, rng, skin, hair)
    return s


# ---------------------------------------------------------------- outfits

def tunic(colour, trousers, belt=(70, 45, 25), boots=(60, 40, 25), sleeves="full", collar=None):
    def draw(s, rng, skin, hair):
        s.fill("body", colour)
        for arm in ("right_arm", "left_arm"):
            s.fill(arm, colour, rows=(0, 12 if sleeves == "full" else 5))
        for leg in ("right_leg", "left_leg"):
            s.fill(leg, trousers)
            s.fill(leg, boots, rows=(9, 12))
            s.fill(leg, boots, sides=["bottom"])
        for i in range(8):
            s.dot("body", "front", i, 7, belt)
            s.dot("body", "back", i, 7, belt)
        for side in ("right", "left"):
            for i in range(4):
                s.dot("body", side, i, 7, belt)
        s.dot("body", "front", 3, 7, (200, 170, 60))
        if collar:
            s.fill("jacket", collar, sides=["top"])
            for i in range(8):
                s.dot("jacket", "front", i, 0, collar)
                s.dot("jacket", "back", i, 0, collar)
    return draw


def with_apron(base, apron=(225, 220, 205)):
    def draw(s, rng, skin, hair):
        base(s, rng, skin, hair)
        for j in range(2, 12):
            for i in range(1, 7):
                s.dot("jacket", "front", i, j, darker(apron, 1 - 0.01 * j))
        for leg, i0 in (("right_pants", 0), ("left_pants", 0)):
            for j in range(0, 6):
                for i in range(4):
                    s.dot(leg, "front", i, j, darker(apron, 0.95))
        s.dot("jacket", "front", 1, 1, apron)
        s.dot("jacket", "front", 6, 1, apron)
    return draw


def dress(colour, trim):
    def draw(s, rng, skin, hair):
        s.fill("body", colour)
        for arm in ("right_arm", "left_arm"):
            s.fill(arm, colour, rows=(0, 9))
        for leg in ("right_leg", "left_leg"):
            s.fill(leg, colour)
            s.fill(leg, darker(colour, 0.85), rows=(10, 12))
        # The skirt as an outer layer so it hangs a little wider.
        for leg in ("right_pants", "left_pants"):
            s.fill(leg, colour, sides=["front", "back", "right", "left"], noise=6)
        for i in range(8):
            s.dot("body", "front", i, 4, trim)
            s.dot("body", "back", i, 4, trim)
        for j in range(5, 12):
            s.dot("jacket", "front", 3, j, trim)
            s.dot("jacket", "front", 4, j, trim)
    return draw


def hat(kind, colour):
    def draw(s):
        if kind == "cap":
            s.fill("hat", colour, sides=["top"])
            for side in ("front", "back", "right", "left"):
                for i in range(8):
                    s.dot("hat", side, i, 0, colour)
                    s.dot("hat", side, i, 1, darker(colour, 0.9))
        elif kind == "hood":
            s.fill("hat", colour, sides=["top", "back", "right", "left"])
            for i in range(8):
                s.dot("hat", "front", i, 0, colour)
            for j in range(8):
                s.dot("hat", "front", 0, j, colour)
                s.dot("hat", "front", 7, j, colour)
        elif kind == "helmet":
            s.fill("hat", colour, sides=["top", "back", "right", "left"], noise=12)
            for i in range(8):
                s.dot("hat", "front", i, 0, colour)
                s.dot("hat", "front", i, 1, lighter(colour))
            for j in range(2, 7):
                s.dot("hat", "front", 3, j, darker(colour))
                s.dot("hat", "front", 4, j, darker(colour))
        elif kind == "chef":
            s.fill("hat", colour, sides=["top", "back", "right", "left", "front"], noise=4)
            for i in range(8):
                for j in range(3, 8):
                    s.dot("hat", "front", i, j, (0, 0, 0), alpha=0)
                    s.dot("hat", "back", i, j, (0, 0, 0), alpha=0)
                    s.dot("hat", "right", i, j, (0, 0, 0), alpha=0)
                    s.dot("hat", "left", i, j, (0, 0, 0), alpha=0)
        elif kind == "feather":
            hat("cap", colour)(s)
            for j in range(0, 4):
                s.dot("hat", "left", 5, j, (230, 60, 50))
                s.dot("hat", "left", 6, j + 1, (230, 60, 50))
        elif kind == "mitre":
            s.fill("hat", colour, sides=["top", "back", "right", "left", "front"], noise=4)
            for side in ("front", "back"):
                for j in range(0, 4):
                    s.dot("hat", side, 3, j, (220, 180, 60))
                    s.dot("hat", side, 4, j, (220, 180, 60))
                for i in range(8):
                    for j in range(3, 8):
                        s.dot("hat", side, i, j, (0, 0, 0), alpha=0)
            for side in ("right", "left"):
                for i in range(8):
                    for j in range(3, 8):
                        s.dot("hat", side, i, j, (0, 0, 0), alpha=0)
    return draw


def guard(tabard):
    mail = (150, 150, 158)

    def draw(s, rng, skin, hair):
        tunic(mail, (80, 80, 88), belt=(60, 40, 25), boots=(50, 45, 40))(s, rng, skin, hair)
        for j in range(12):
            for i in range(2, 6):
                s.dot("jacket", "front", i, j, tabard if j < 10 else darker(tabard))
                s.dot("jacket", "back", i, j, tabard)
        # A white cross on the tabard.
        for j in range(2, 7):
            s.dot("jacket", "front", 3, j, (235, 235, 235))
            s.dot("jacket", "front", 4, j, (235, 235, 235))
        for i in range(2, 6):
            s.dot("jacket", "front", i, 3, (235, 235, 235))
        hat("helmet", (170, 172, 180))(s)
    return draw


def robe(colour, trim, chain=None):
    def draw(s, rng, skin, hair):
        s.fill("body", colour)
        for arm in ("right_arm", "left_arm"):
            s.fill(arm, colour, rows=(0, 11))
        for leg in ("right_leg", "left_leg"):
            s.fill(leg, colour)
        for leg in ("right_pants", "left_pants"):
            s.fill(leg, colour, sides=["front", "back", "right", "left"], noise=5)
        for j in range(12):
            s.dot("body", "front", 3, j, trim)
            s.dot("body", "front", 4, j, trim)
        if chain:
            for i in range(1, 7):
                s.dot("jacket", "front", i, 1 if i in (1, 6) else 2 if i in (2, 5) else 3, chain)
            s.dot("jacket", "front", 3, 4, chain)
            s.dot("jacket", "front", 4, 4, chain)
            s.fill("jacket", trim, sides=["top"])
    return draw


def combine(*fns):
    def draw(s, rng, skin, hair):
        for fn in fns:
            fn(s, rng, skin, hair) if fn.__code__.co_argcount == 4 else fn(s)
    return draw


SKINS = {
    "guard_blue": (11, combine(guard((40, 70, 150)))),
    "guard_red": (12, combine(guard((150, 40, 40)))),
    "shopkeeper_green": (21, combine(with_apron(tunic((60, 110, 70), (90, 70, 50)), (215, 205, 180)), hat("cap", (90, 60, 35)))),
    "shopkeeper_brown": (22, combine(with_apron(tunic((150, 110, 70), (70, 60, 50), sleeves="rolled"), (230, 225, 215)))),
    "shopkeeper_blue": (23, combine(tunic((60, 90, 150), (60, 55, 50), collar=(230, 220, 200)), hat("feather", (40, 50, 90)))),
    "florist": (24, combine(dress((200, 110, 150), (240, 230, 120)), hat("cap", (240, 200, 210)))),
    "banker": (31, combine(robe((40, 38, 48), (200, 170, 70), chain=(230, 190, 60)), hat("cap", (30, 30, 36)))),
    "mayor": (32, combine(robe((150, 30, 40), (240, 235, 225), chain=(235, 195, 60)), hat("cap", (110, 20, 30)))),
    "priest": (33, combine(robe((235, 232, 225), (200, 170, 60)), hat("mitre", (235, 232, 225)))),
    "innkeeper": (41, combine(with_apron(tunic((230, 225, 210), (80, 60, 40), sleeves="rolled"), (120, 90, 60)))),
    "baker": (42, combine(with_apron(tunic((235, 232, 225), (200, 195, 185)), (245, 245, 240)), hat("chef", (245, 245, 245)))),
    "decorator_green": (51, combine(tunic((80, 140, 60), (110, 80, 50), collar=(200, 180, 120)), hat("feather", (70, 110, 50)))),
    "decorator_orange": (52, combine(tunic((210, 120, 50), (90, 70, 50)), hat("cap", (150, 80, 40)))),
    "townsfolk_1": (61, combine(tunic((120, 60, 50), (80, 70, 60)))),
    "townsfolk_2": (62, combine(dress((70, 110, 150), (230, 220, 200)), hat("hood", (90, 70, 50)))),
    "townsfolk_3": (63, combine(tunic((100, 120, 60), (70, 60, 50), sleeves="rolled"))),
    "townsfolk_4": (64, combine(dress((150, 120, 70), (120, 60, 40)))),
    "townsfolk_5": (65, combine(tunic((90, 80, 130), (60, 60, 70)), hat("hood", (60, 70, 50)))),
}

# Which skins each role wears (the layout spreads them).
ROLE_SKINS = {
    "guard": ["guard_blue", "guard_red"],
    "shopkeeper": ["shopkeeper_green", "shopkeeper_brown", "shopkeeper_blue"],
    "vendor": ["shopkeeper_brown", "townsfolk_4", "townsfolk_3"],
    "florist": ["florist"],
    "banker": ["banker"], "mayor": ["mayor"], "priest": ["priest"],
    "innkeeper": ["innkeeper"], "baker": ["baker"],
    "decorator": ["decorator_green", "decorator_orange"],
    "townsfolk": ["townsfolk_1", "townsfolk_2", "townsfolk_3", "townsfolk_4", "townsfolk_5"],
}


def write():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (seed, outfit) in SKINS.items():
        person(seed, outfit, clean=True).save(OUT / f"{name}.png")
    return list(SKINS)


if __name__ == "__main__":
    print(write())
