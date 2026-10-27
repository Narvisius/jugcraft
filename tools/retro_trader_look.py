"""The Retro Trader's look: his 64x64 villager overlay and his expression sheet (called from pixel_hollows_textures.py).

A bearded millennial who runs the village's retro game shop: a burgundy knit beanie, thick black glasses over
green eyes, a full brown beard, a forest-green tartan flannel open over a charcoal tee with a little pixel
gamepad on it, sleeves rolled up to show a calculator watch, light cuffed jeans and red canvas high-tops.
Everything is original pixel art drawn here; nothing is read, traced or recoloured from Mojang's textures.

The overlay follows the villager model's UV layout, so the villager's own model stays underneath. The
expression sheet (textures/entity/retro_trader/expressions.png) is read by the client's RetroTraderLayer:

- eye frames, 8x7 each: the head's front rows 0-6 (forehead, brows, glasses and eyes), drawn over the face;
- mouth frames, 8x3 each: the head's front rows 7-9 (moustache, mouth and beard);
- two free arms for the stomp-clap (4x10x4 boxes in the model's UV layout), which stand in for the villager's
  folded arms while he claps;
- his beanie (three boxes: cuff, crown and dome), which the layer draws on his head.

The face on the overlay itself is the neutral face (EYES_OPEN, MOUTH_NEUTRAL), so he looks the same wherever
the layer is not drawn (his zombie form uses the same overlay).
"""
from PIL import Image

CLEAR = (0, 0, 0, 0)
SKIN = [(204, 152, 118), (182, 132, 100), (160, 112, 84)]
HAIR = [(64, 40, 24), (88, 56, 32), (112, 74, 44)]
BEARD = [(70, 42, 24), (92, 58, 34), (54, 32, 18)]
BROW = (52, 32, 18)
LASH = (40, 26, 18)
FRAME = [(20, 20, 24), (44, 44, 52)]
EYE_WHITE, EYE_GREEN, PUPIL = (238, 238, 230), (52, 150, 64), (24, 52, 28)
MOUTH, TONGUE, TEETH, LIP, LIP_SHADOW = (58, 20, 22), (150, 60, 66), (240, 238, 228), (176, 98, 86), (112, 64, 50)
BEANIE_COLOURS = [(78, 20, 30), (118, 32, 44), (150, 50, 62)]  # burgundy, well away from any hair colour
CUFF = [(96, 24, 36), (166, 62, 74)]  # the turned-up cuff's ribs
FLANNEL = [(44, 92, 60), (30, 62, 44), (26, 32, 56), (200, 162, 64)]
TEE = [(66, 68, 76), (54, 56, 64)]
PRINT = {"W": (236, 236, 240), "C": (60, 214, 226), "M": (232, 76, 170), "G": (150, 152, 162)}
BELT, BUCKLE = (84, 52, 30), (210, 176, 90)
JEANS = [(96, 132, 178), (78, 110, 154), (124, 160, 200), (210, 200, 160)]
WATCH, LCD = (30, 30, 34), (150, 196, 120)
SHOE = [(188, 40, 44), (150, 28, 34)]
SOLE, TOE = (236, 230, 214), (244, 242, 236)

# Eye frames (index into the sheet's first two rows of cells) and mouth frames (the row below them).
EYES = ["open", "half", "closed", "look_left", "look_right", "happy", "wide", "squeeze", "frown"]
MOUTHS = ["neutral", "smile", "grin", "talk", "shout", "ooh", "frown"]
EYE_CELL, MOUTH_CELL = (8, 7), (8, 3)
MOUTH_ROW = 16  # sheet y of the first mouth frame
ARMS = {"right": (0, 32), "left": (16, 32)}  # texture offsets of the free arms (4x10x4 boxes)
# The beanie, in head space (pixels; y down, his front towards -z): a ribbed cuff standing just off his hairline, a
# knit crown above the head and a smaller dome on top, the crown and dome set back a little so it slouches. Each box:
# name, origin, size, texture size (whole pixels, the sheet's box layout) and texture offset.
BEANIE = [("cuff", (-4.5, -10.0, -4.5), (9.0, 2.0, 9.0), (9, 2, 9), (0, 20)),
          ("crown", (-4.3, -12.4, -4.0), (8.6, 2.4, 8.6), (9, 3, 9), (0, 48)),
          ("dome", (-3.5, -13.2, -3.0), (7.0, 0.8, 7.0), (7, 1, 7), (36, 20))]


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height and color is not None:
        img.putpixel((x, y), tuple(color[:3]) + ((color[3],) if len(color) > 3 else (255,)))


def box_faces(u, v, w, h, d):
    """Texture rectangles (x, y, width, height) of a model box at texture offset (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def fill(img, rect, color_at):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            put(img, x0 + x, y0 + y, color_at(x, y))


def knit(x, y):
    """Stockinette: little V stitches in columns (light on the left leg of each V), a dark purl ridge every third row."""
    if y % 3 == 2:
        return BEANIE_COLOURS[0]
    return BEANIE_COLOURS[2] if (x + y) % 2 == 0 else BEANIE_COLOURS[1]


def rib(x, y):
    """The cuff's vertical ribbing: alternate raised (light) and sunken (dark) columns."""
    return CUFF[1] if x % 2 == 0 else CUFF[0]


def tartan(x, y):
    """Forest-green tartan: navy bands every four pixels each way (darker where they cross), a mustard pinstripe."""
    band_x, band_y = x % 4 == 0, y % 4 == 0
    if band_x and band_y:
        return shade(FLANNEL[2], -8)
    if band_x or band_y:
        return FLANNEL[2]
    if x % 8 == 2:
        return FLANNEL[3]
    return FLANNEL[1] if (x + y) % 4 == 2 else FLANNEL[0]


def jeans(x, y):
    """Light-wash denim on a diagonal twill."""
    return JEANS[2] if (x + y) % 5 == 0 else JEANS[0] if (x + 2 * y) % 7 else JEANS[1]


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color[:3])


def strand(tones, x, y, period=4):
    return tones[1] if (x + 2 * y) % period == 0 else tones[0]


# ---------------------------------------------------------------- the face (8 wide, 10 tall: the head's front)

def lens(eye, x, y, inner):
    """One lens's 2x2 inside (x, y in 0..1; inner is the column nearer the nose)."""
    iris = {"open": inner, "look_left": 0, "look_right": 1}.get(eye)
    if iris is not None:
        return EYE_GREEN if x == iris else EYE_WHITE
    if eye == "half":
        return SKIN[1] if y == 0 else (EYE_GREEN if x == inner else EYE_WHITE)
    if eye == "closed":
        return SKIN[0] if y == 0 else LASH
    if eye == "happy":
        return LASH if y == 0 else SKIN[0]  # a lash line high up: the eye smiling shut
    if eye == "wide":
        return PUPIL if (x == inner and y == 1) else EYE_WHITE
    if eye == "squeeze":
        return LASH if (y == 0) != (x == inner) else SKIN[1]  # creased shut, a zigzag across the lens
    if eye == "frown":
        return SKIN[2] if y == 0 else (EYE_GREEN if x == inner else EYE_WHITE)
    raise ValueError(eye)


def brows(eye):
    """Brow pixels {(x, y)} for an eye frame, all on row 2 (the beanie's cuff covers rows 0-1): short and lifted at
    the outer ends for wide or happy eyes, knitted together for a frown or a squeeze."""
    if eye in ("wide", "happy"):
        return {(1, 2), (6, 2)}
    if eye in ("frown", "squeeze"):
        return {(1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2)}
    return {(1, 2), (2, 2), (5, 2), (6, 2)}


def eyes_at(eye, x, y):
    """The head's front, rows 0-6: hair line, forehead, brows, and the glasses with the eyes inside."""
    if (x, y) in brows(eye):
        return BROW
    if y == 0:
        return HAIR[1] if x in (0, 7) else SKIN[0]
    if y in (1, 2):
        return HAIR[0] if x in (0, 7) and y == 1 else SKIN[0]
    # Glasses, rows 3-6: two thick rectangular frames side by side, cols 0-3 and 4-7.
    left = x < 4
    lx = x if left else x - 4
    if y in (3, 6) or lx in (0, 3):
        return FRAME[1] if (y == 3 and lx in (1, 2)) else FRAME[0]
    inner = 1 if left else 0
    return lens(eye, lx - 1, y - 4, inner)


def mouth_at(mouth, x, y):
    """The head's front, rows 7-9 (y 0-2 here): beard, moustache and mouth.

    His nose is raised two pixels (RetroTraderModelMixin), so it covers columns 3 and 4 of rows 5-8 and ends just above
    row 9: the moustache runs along row 8 beside it and the mouth is row 9, right under the nose, across the face.
    """
    beard = strand(BEARD[:2], x, y)
    inner = 2 <= x <= 5  # the mouth's width
    centre = x in (3, 4)
    corner = x in (1, 6)
    if y == 0:
        return beard
    if y == 1:
        if mouth in ("smile", "grin") and corner:
            return LIP  # the corners of a smile lifting into the moustache
        if mouth == "shout" and inner:
            return TEETH  # the top teeth under the moustache
        return BEARD[2] if 1 <= x <= 6 else beard  # the moustache
    if not inner:
        return beard
    if mouth == "neutral":
        return LIP_SHADOW
    if mouth == "smile":
        return LIP
    if mouth == "grin":
        return TEETH
    if mouth == "talk":
        return MOUTH if centre else LIP
    if mouth == "shout":
        return TONGUE if centre else MOUTH
    if mouth == "ooh":
        return MOUTH if centre else BEARD[2]
    if mouth == "frown":
        return MOUTH if not centre else LIP  # the corners dark, pulled down
    raise ValueError(mouth)


def face_at(eye, mouth, x, y):
    return eyes_at(eye, x, y) if y < 7 else mouth_at(mouth, x, y - 7)


# ---------------------------------------------------------------- the overlay

def overlay():
    img = Image.new("RGBA", (64, 64), CLEAR)

    # Head (0,0; 8x10x8): short hair at the back and sides under the beanie, sideburns into the beard, beard below.
    head = box_faces(0, 0, 8, 10, 8)
    fill(img, head["front"], lambda x, y: face_at("open", "neutral", x, y))
    fill(img, head["top"], lambda x, y: strand(HAIR, x, y, 3))
    fill(img, head["bottom"], lambda x, y: strand(BEARD[:2], x, y))
    fill(img, head["back"], lambda x, y: strand(HAIR, x, y, 3) if y < 8 else SKIN[1])

    def side(front_at_high_u):
        def paint(x, y):
            toward_front = x if front_at_high_u else 7 - x
            if y >= 6 and toward_front >= 3:
                return strand(BEARD[:2], x, y)  # the beard wrapping round the jaw
            if y == 4 and toward_front >= 5:
                return FRAME[0]  # the arm of the glasses
            if toward_front >= 5 and y < 6:
                return HAIR[0] if toward_front == 5 else SKIN[0]  # sideburn, then cheek
            if toward_front == 4 and 4 <= y < 7:
                return SKIN[1]  # the ear
            return strand(HAIR, x, y, 3) if y < 8 else SKIN[1]
        return paint
    fill(img, head["right"], side(True))
    fill(img, head["left"], side(False))

    # Hat layer (32,0): left empty. His beanie is a small model of its own (BEANIE, above), drawn on his head by the
    # client's RetroTraderLayer, so it has a beanie's shape; his zombie form, which has no such layer, shows his hair.

    # Nose (24,0; 2x4x2): the moustache along its underside.
    nose = box_faces(24, 0, 2, 4, 2)
    for name in ("front", "right", "left"):
        fill(img, nose[name], lambda x, y: BEARD[2] if y == 3 else None)
    fill(img, nose["bottom"], lambda x, y: BEARD[2])

    gamepad = ["WWWW", "CGGM", "W..W"]

    def torso_front(x, y):
        """The flannel open over the tee (cols 2-5), a pixel gamepad on the chest, a belt, jeans below."""
        if y >= 12:
            return JEANS[3] if (y == 12 and x in (1, 6)) else jeans(x, y)  # a stitched pocket corner
        if y == 11:
            return BUCKLE if x in (3, 4) else BELT
        if x in (0, 1, 6, 7):
            return shade(tartan(x, y), -14) if x in (1, 6) else tartan(x, y)  # the open placket's shadowed edge
        if 3 <= y <= 5:
            mark = gamepad[y - 3][x - 2]
            if mark != ".":
                return PRINT[mark]
        return TEE[1] if y == 0 else TEE[0]

    def torso_back(x, y):
        if y >= 12:
            return jeans(x, y)
        if y == 11:
            return BELT
        if y == 1:
            return shade(tartan(x, y), 12)  # the yoke seam
        return tartan(x, y)

    def torso_side(x, y):
        if y >= 12:
            return jeans(x, y)
        return BELT if y == 11 else tartan(x, y)

    # Body (16,20; 8x12x6) and the robe layer over it (0,38; 8x20x6): the robe becomes shirt and jeans.
    for faces in (box_faces(16, 20, 8, 12, 6), box_faces(0, 38, 8, 20, 6)):
        fill(img, faces["front"], torso_front)
        fill(img, faces["back"], torso_back)
        fill(img, faces["right"], torso_side)
        fill(img, faces["left"], torso_side)
        fill(img, faces["top"], lambda x, y: TEE[1] if 2 <= x <= 5 and y <= 2 else tartan(x, y))
        fill(img, faces["bottom"], jeans)

    # Folded arms (44,22; 4x8x4 each, the left mirrored): flannel sleeves, rolled at the elbow.
    arm = box_faces(44, 22, 4, 8, 4)
    for name in ("top", "front", "back", "right", "left"):
        fill(img, arm[name], lambda x, y: shade(FLANNEL[0], 24) if y == 7 else tartan(x, y))
    fill(img, arm["bottom"], lambda x, y: SKIN[0])
    # The forearms across the chest (40,38; 8x4x4): bare forearms, hands clasped in the middle, the watch.
    forearms = box_faces(40, 38, 8, 4, 4)

    def forearm(x, y):
        if x in (0, 7):
            return shade(FLANNEL[0], 24)  # the rolled cuffs
        if x == 2:
            return LCD if y == 1 else WATCH  # the calculator watch on his left wrist
        return SKIN[1] if x in (3, 4) and y in (0, 3) else SKIN[0]
    for name in ("top", "bottom", "back", "front"):
        fill(img, forearms[name], forearm)
    fill(img, forearms["right"], lambda x, y: shade(FLANNEL[0], 24))
    fill(img, forearms["left"], lambda x, y: shade(FLANNEL[0], 24))

    # Legs (0,22; 4x12x4): only the bottom third shows below the robe: a rolled cuff, then red high-tops.
    leg = box_faces(0, 22, 4, 12, 4)

    def leg_side(toe):
        def paint(x, y):
            if y < 8:
                return jeans(x, y)
            if y == 8:
                return JEANS[3] if x % 2 else shade(JEANS[3], -16)  # the rolled cuff
            if y == 11:
                return SOLE
            if toe and y == 10:
                return TOE
            if y == 9 and x in (1, 2) and toe:
                return TOE  # the laces
            return SHOE[1] if y == 9 else SHOE[0]
        return paint
    fill(img, leg["front"], leg_side(True))
    for name in ("right", "left", "back"):
        fill(img, leg[name], leg_side(False))
    fill(img, leg["top"], jeans)
    fill(img, leg["bottom"], lambda x, y: SOLE)
    return img


# ---------------------------------------------------------------- the expression sheet

def free_arm(img, u, v, watch):
    """A 4x10x4 arm: tartan sleeve (rows 0-4), the rolled cuff (row 5), forearm and hand; the watch on one wrist."""
    faces = box_faces(u, v, 4, 10, 4)

    def side(x, y):
        if y <= 4:
            return tartan(x, y)
        if y == 5:
            return shade(FLANNEL[0], 24)
        if watch and y == 8:
            return LCD if x == 1 else WATCH
        return SKIN[1] if y == 9 else SKIN[0]
    for name in ("front", "back", "right", "left"):
        fill(img, faces[name], side)
    fill(img, faces["top"], tartan)
    fill(img, faces["bottom"], lambda x, y: SKIN[1])


def expressions():
    img = Image.new("RGBA", (64, 64), CLEAR)
    for i, eye in enumerate(EYES):
        x0, y0 = EYE_CELL[0] * (i % 8), EYE_CELL[1] * (i // 8)
        fill(img, (x0, y0, *EYE_CELL), lambda x, y, eye=eye: eyes_at(eye, x, y))
    for i, mouth in enumerate(MOUTHS):
        x0, y0 = MOUTH_CELL[0] * (i % 8), MOUTH_ROW + MOUTH_CELL[1] * (i // 8)
        fill(img, (x0, y0, *MOUTH_CELL), lambda x, y, mouth=mouth: mouth_at(mouth, x, y))
    free_arm(img, *ARMS["right"], watch=False)
    free_arm(img, *ARMS["left"], watch=True)
    for name, _, _, (w, h, d), (u, v) in BEANIE:
        faces = box_faces(u, v, w, h, d)
        for side in ("front", "back", "right", "left"):
            fill(img, faces[side], rib if name == "cuff" else knit)
        fill(img, faces["top"], (lambda x, y: BEANIE_COLOURS[2] if (x + y) % 3 else BEANIE_COLOURS[1]) if name == "dome" else knit)
        fill(img, faces["bottom"], lambda x, y: BEANIE_COLOURS[0])
    return img
