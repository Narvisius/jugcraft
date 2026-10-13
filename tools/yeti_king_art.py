"""Original textures for the Yeti King and his fight (docs/features/yeti-king.md) (requires Pillow): the sheets of his
GeckoLib bodies (tools/yeti_king_models.py) and the King's glowmask, painted box by box and face by face at TEXTURE_SCALE
times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- The King: white fur in long locks, shadowed blue-grey and frosted blue at their tips, a shaggy mantle over his
  shoulders and hanging fur on his forearms hung with icicles; a bare blue-grey face, palms, knuckles and soles; eyes
  of glacier blue under a heavy white brow; ivory fangs and a dark mouth with its tongue; a crown of blue ice on a
  faceted band; the blue fire of the Fury of the Peaks round its points and rising from his eyes; and the block of ice
  he heaves over his head. His glowmask holds his eyes and that fire.
- His whelps: round white cubs with big glacier eyes, a pale face and a tuft of blue frost.
- His things: the hurled block (packed ice under a crust of snow), the falling icicle (rime at its broken stump, clear
  ice to its point) and the glacial spike (clear blue shards in broken snow).
- The worn crown's textures, and the icons of Yeti Fur, the Yeti Mitten and the crown (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import yeti_king as yeti_numbers
import yeti_king_models as ym
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, cell, ragged

S = ym.TEXTURE_SCALE

# Palettes, darkest first.
FUR = [rgb("8ea3c2"), rgb("b4c5dc"), rgb("d3dfec"), rgb("e9f0f7"), rgb("fafcff")]
FROST = [rgb("5b86b8"), rgb("86acd8"), rgb("b5d2ee")]
SKIN = [rgb("27313f"), rgb("3a4757"), rgb("516075"), rgb("6b7c93")]
EYE = [rgb("0e4f8c"), rgb("2192e0"), rgb("74d2ff"), rgb("e4f8ff")]
PUPIL = rgb("0a1626")
ICE = [rgb("1f5fa6"), rgb("3784cc"), rgb("62aee8"), rgb("9fd3f6"), rgb("dff2fd")]
BLAZE = [rgb("1e7cf0"), rgb("4fbcff"), rgb("a8ecff"), rgb("f0fdff")]
FANG = [rgb("8f8670"), rgb("c9c0a8"), rgb("efe9d8")]
MOUTH = [rgb("1b1222"), rgb("341a2c"), rgb("5c2a40"), rgb("8a4a60")]
PACKED = [rgb("4d7fb6"), rgb("6c9ccf"), rgb("93bde4"), rgb("c2dcf2"), rgb("e8f4fc")]
SNOW = [rgb("c3d2e4"), rgb("e1eaf4"), rgb("f7fafd")]

# The tone (an index into FUR) of each model-pixel column of a lock pattern, repeating: wide locks, a lit one, a shadowed
# parting.
LOCKS = (3, 3, 3, 4, 4, 3, 3, 2, 2, 3, 3, 3, 2, 2)
SIDES = ("right", "left", "front", "back")


# ------------------------------------------------------------------------------------------------- fur

def locks(sheet, rect, phase=0, darker=0, frost=0, hem=True):
    """Fur hanging in long locks down a face: each model-pixel column a flat tone (LOCKS, `darker` tones down), a
    shadowed parting where a lock gives way to a darker one; its last `frost` model-pixel rows frosted blue at the
    tips; and with `hem`, the locks' ends pointed along the bottom edge."""
    x, y, w, h = rect
    cols = max(1, w // S)
    for c in range(cols):
        tone = LOCKS[(c + phase) % len(LOCKS)]
        sheet.fill((x + c * S, y, S, h), FUR[max(0, tone - darker)])
        if tone == 2 and LOCKS[(c - 1 + phase) % len(LOCKS)] > 2:
            sheet.fill((x + c * S, y, 1, h), FUR[max(0, 1 - darker)])
        if frost:
            tip = FROST[2] if tone >= 3 else FROST[1]
            sheet.fill((x + c * S, y + h - frost * S, S, frost * S), tip)
    if hem:
        # Between each pair of locks a notch of shadow, so their ends hang in points.
        for c in range(1, cols, 2):
            left = x + c * S
            for j in range(S):
                sheet.fill((left - j // 2, y + h - S + j, 1 + j, 1), FUR[0] if not frost else FROST[0])


def fur_top(sheet, rect, darker=0, glints=False):
    """The top of a furred part: the fur's lit tone, its locks' ends showing as short strokes on the grid; with `glints`,
    frost glinting on it."""
    x, y, w, h = rect
    sheet.fill(rect, FUR[max(0, 3 - darker)])
    for cy in range(0, h // S, 3):
        for cx in range((cy // 3) % 2, w // S, 3):
            sheet.fill((x + cx * S, y + cy * S, S, S // 2), FUR[max(0, 4 - darker)])
            sheet.fill((x + cx * S, y + cy * S + S // 2, 1, S // 2), FUR[max(0, 2 - darker)])
    if glints:
        for cy in range(1, h // S, 4):
            for cx in range(1 + (cy // 4) % 2 * 2, w // S, 4):
                sheet.fill((x + cx * S + 1, y + cy * S + 1, 2, 2), FROST[2])


def fur(sheet, part, phase=0, darker=0, frost=0, hem=True, glints=False):
    """A part in fur: its sides in locks, its top lit, its underside in shadow."""
    for name, rect in sheet.faces(part).items():
        if name == "top":
            fur_top(sheet, rect, darker, glints)
        elif name == "bottom":
            sheet.fill(rect, FUR[max(0, 1 - darker)])
        else:
            locks(sheet, rect, phase + {"right": 0, "front": 3, "left": 6, "back": 1}[name], darker, frost, hem)


def skin(sheet, rect, lit=True):
    """Bare blue-grey skin: flat, a tone lighter along its top edge."""
    x, y, w, h = rect
    sheet.fill(rect, SKIN[2])
    if lit:
        sheet.fill((x, y, w, S // 2), SKIN[3])


def icicles(sheet, rect, every=3, length=2):
    """Small icicles hanging from a face's bottom edge, drawn on it: one every `every` model pixels, `length` long, a
    lit stroke down their left side."""
    x, y, w, h = rect
    for c in range(1, w // S - 1, every):
        for j in range(length * S):
            width = max(1, round(S * (1 - j / (length * S))))
            sheet.fill((x + c * S, y + h - length * S + j, width, 1), FROST[2])
            sheet.put(x + c * S, y + h - length * S + j, ICE[4])


# ------------------------------------------------------------------------------------------------- ice and fire

def ice(sheet, part, base=2):
    """Clear ice, faceted: the face's left third lit a tone up, its right edge shadowed, its top edge catching the
    light; the top brightest, the underside darkest."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, ICE[4])
        elif name == "bottom":
            sheet.fill(rect, ICE[0])
        else:
            sheet.fill(rect, ICE[base])
            sheet.fill((x, y, max(S, w // 3), h), ICE[base + 1])
            sheet.fill((x + w - max(2, S // 2), y, max(2, S // 2), h), ICE[base - 1])
            sheet.fill((x, y, w, 2), ICE[4])


def flames(sheet, rect, tongues=2, glow=True):
    """Blue fire filling a face from below: bright at its root, cut into `tongues` points along its top (cleared)."""
    x, y, w, h = rect
    for py in range(y, y + h):
        t = (py - y) / max(1, h - 1)            # 0 at the top, 1 at the bottom
        tone = BLAZE[3] if t > 0.8 else BLAZE[2] if t > 0.45 else BLAZE[1]
        for px in range(x, x + w):
            u = (px - x + 0.5) / w * tongues % 1.0  # across each tongue, 0 to 1
            reach = 1.0 - abs(2 * u - 1)            # 1 at a tongue's middle, 0 between
            if t < 0.55 * (1 - reach):
                sheet.clear(px, py)
                continue
            colour = BLAZE[0] if abs(2 * u - 1) > 0.8 and t < 0.7 else tone
            sheet.put(px, py, colour)
            if glow:
                sheet.put(px, py, colour, glow=True)


def blaze(sheet, part):
    """The Fury of the Peaks' fire round a crown point: flames up every side, no top or underside."""
    for name, rect in sheet.faces(part).items():
        if name in ("top", "bottom"):
            x, y, w, h = rect
            for py in range(y, y + h):
                for px in range(x, x + w):
                    sheet.clear(px, py)
        else:
            flames(sheet, rect, tongues=2)


def eye_fire(sheet):
    """Blue flames rising from his eyes, drawn on the plane before his face: two wisps leaning outwards, rooted at the
    eyes' tops (the plane's lowest rows), cleared everywhere else."""
    for name, rect in sheet.faces("eye_flare").items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                sheet.clear(px, py)
        for centre, lean in ((3.0, -1.0), (10.0, 1.0)):
            for py in range(y, y + h):
                t = (y + h - 0.5 - py) / h          # 0 at the root, 1 at the top
                half = 1.1 * S * (1 - t) ** 0.8
                mid = x + (centre + lean * 1.5 * t) * S
                for px in range(int(mid - half), int(mid + half) + 1):
                    if x <= px < x + w:
                        edge = abs(px + 0.5 - mid) / max(0.5, half)
                        colour = BLAZE[3] if edge < 0.35 and t < 0.5 else BLAZE[2] if edge < 0.7 else BLAZE[1]
                        sheet.put(px, py, colour)
                        sheet.put(px, py, colour, glow=True)


# ------------------------------------------------------------------------------------------------- the King

def paint_body(sheet):
    # The chest: locks all round, a parting down the middle of his front and a paler bib of fur across it.
    fur(sheet, "chest", phase=0, frost=1)
    f = sheet.faces("chest")
    x, y, w, h = f["front"]
    sheet.fill(cell(f["front"], 8, 2, 8, 8), FUR[4])
    for c in (9, 11, 13):
        sheet.fill(cell(f["front"], c, 3, 1, 7), FUR[3])
    sheet.fill((x + w // 2, y, 1, h), FUR[1])
    # Down his back a ridge of frosted blue fur.
    x, y, w, h = f["back"]
    sheet.fill((x + w // 2 - S, y, 2 * S, h), FROST[2])
    sheet.fill((x + w // 2 - S, y, 1, h), FROST[1])
    # The mantle: longer locks, frosted tips and icicles along its hem, frost glinting on top.
    fur(sheet, "mantle", phase=4, frost=2, glints=True)
    for name in ("front", "back", "right", "left"):
        icicles(sheet, sheet.faces("mantle")[name], every=4, length=2)
    # His hips and legs, a tone darker in the shadow under him; his shins frosted at the hem.
    fur(sheet, "pelvis", phase=2, darker=1)
    fur(sheet, "thigh", phase=5, darker=1)
    fur(sheet, "shin", phase=7, darker=1, frost=1)


def paint_feet(sheet):
    """Fur over the heel, bare skin to the toes: four toes on the front with dark nails, a padded sole."""
    f = sheet.faces("foot")
    for name, rect in f.items():
        x, y, w, h = rect
        if name == "top":
            fur_top(sheet, rect, darker=1)
            sheet.fill((x, y + h - 4 * S, w, 4 * S), SKIN[2])          # the toes: the top's front rows
            for c in range(2, w // S, 2):
                sheet.fill((x + c * S, y + h - 4 * S, 1, 4 * S), SKIN[0])
        elif name == "bottom":
            sheet.fill(rect, SKIN[1])
            for cx, cy in ((1, 1), (5, 1), (3, 5), (1, 9), (4, 9), (7, 9)):
                sheet.fill(cell(rect, cx, cy, 2, 2), SKIN[0])
        elif name == "front":
            skin(sheet, rect)
            for c in range(2, w // S, 2):
                sheet.fill((x + c * S, y, 1, h), SKIN[0])
            for c in range(0, w // S - 1, 2):
                sheet.fill(cell(rect, c, 2, 2, 1), FANG[0])
        elif name == "back":
            locks(sheet, rect, darker=1, hem=False)
        else:
            # The sides: fur along the heel's half, skin towards the toes (the right face's front is its last
            # column, the left's its first).
            locks(sheet, rect, darker=1, hem=False)
            toes = (x + w - 4 * S, y, 4 * S, h) if name == "right" else (x, y, 4 * S, h)
            skin(sheet, toes)


def paint_arms(sheet):
    fur(sheet, "upper_arm", phase=3)
    fur(sheet, "forearm", phase=8, frost=2)
    for name in ("right", "left", "back"):
        icicles(sheet, sheet.faces("forearm")[name], every=3, length=2)
    # His fists: fur over the wrist, then bare fingers curled to the knuckles he walks on.
    f = sheet.faces("fist")
    for name, rect in f.items():
        x, y, w, h = rect
        if name == "top":
            fur_top(sheet, rect)
        elif name == "bottom":
            sheet.fill(rect, SKIN[2])
            for c in range(0, w // S, 3):
                sheet.fill((x + c * S, y, 1, h), SKIN[0])
                sheet.fill((x + c * S + 1, y, S, h), SKIN[3])
        elif name == "back":
            locks(sheet, (x, y, w, h - 3 * S), phase=2, hem=True)
            skin(sheet, (x, y + h - 3 * S, w, 3 * S))
        else:
            locks(sheet, (x, y, w, 3 * S), phase=1, hem=True)
            skin(sheet, (x, y + 3 * S, w, h - 3 * S))
            fingers = 4 if name == "front" else 3
            step = max(1, (w // S) // fingers)
            for i in range(1, fingers):
                sheet.fill((x + i * step * S, y + 3 * S, 1, h - 3 * S), SKIN[0])
            for i in range(fingers):
                sheet.fill((x + i * step * S + 1, y + 3 * S + 1, step * S - 2, 1), SKIN[3])
    # The fur hanging from his forearms: long locks frosted at their ragged ends, hung with icicles.
    for name, rect in sheet.faces("arm_fur").items():
        locks(sheet, rect, phase=4, frost=3, hem=False)
        icicles(sheet, rect, every=2, length=3)
        ragged(sheet, rect, every=2 * S, depth=S + 2)


def paint_head(sheet):
    # The skull: fur all round, a bare face in front.
    fur(sheet, "skull", phase=1, hem=False)
    front = sheet.faces("skull")["front"]
    sheet.fill(cell(front, 2, 4, 12, 10), SKIN[2])
    sheet.fill(cell(front, 2, 4, 12, 1), SKIN[1])                  # in the brow's shadow
    sheet.fill(cell(front, 7, 5, 2, 3), SKIN[3])                   # the bridge of his nose
    for c in (0, 1, 14, 15):
        sheet.fill(cell(front, c, 8, 1, 6), FUR[2 if c in (1, 14) else 3])   # the fur of his cheeks
    # His eyes: glacier blue, lit above, a dark pupil and a glint; they glow.
    for cx in (3, 10):
        iris = cell(front, cx, 5, 3, 2)
        sheet.fill(iris, EYE[1])
        sheet.fill(cell(front, cx, 5, 3, 1), EYE[2])
        x, y, w, h = iris
        for rect, colour in ((iris, EYE[1]), (cell(front, cx, 5, 3, 1), EYE[2])):
            sheet.fill(rect, colour, glow=True)
        sheet.fill((x + S, y + 2, S, S + 1), PUPIL)
        sheet.fill((x + S, y + 2, S, S + 1), CLEAR, glow=True)
        sheet.fill((x + S, y + 1, 2, 2), EYE[3])
        sheet.fill((x + S, y + 1, 2, 2), EYE[3], glow=True)
        sheet.fill((x, y + h - 1, w, 1), EYE[0])
    # The heavy brow: white fur falling over his eyes.
    fur(sheet, "brow", phase=3, hem=True)
    x, y, w, h = sheet.faces("brow")["front"]
    sheet.fill((x, y, w, S), FUR[4])
    # The muzzle: skin, a broad nose with two nostrils, wrinkles over it, the upper lip; under it the dark of his
    # mouth with his upper teeth along its front edge.
    f = sheet.faces("muzzle")
    for name, rect in f.items():
        x, y, w, h = rect
        if name == "bottom":
            sheet.fill(rect, MOUTH[1])
            sheet.fill((x, y + h - S, w, S), FANG[2])               # the front edge: his teeth
            for c in range(1, w // S, 2):
                sheet.fill((x + c * S, y + h - S, 1, S), FANG[0])
        elif name == "top":
            sheet.fill(rect, SKIN[3])
            for cy in (1, 3):
                sheet.fill((x + S, y + cy * S, w - 2 * S, 1), SKIN[1])
        else:
            sheet.fill(rect, SKIN[3])
            sheet.fill((x, y + h - 1, w, 1), SKIN[2])
    front = f["front"]
    sheet.fill(cell(front, 3, 0, 4, 2), SKIN[1])                    # the nose
    sheet.fill(cell(front, 3, 0, 4, 1), SKIN[3])
    for cx in (3, 5):
        x, y, _, _ = cell(front, cx, 1)
        sheet.fill((x + 1, y, 2 * S - 2, S - 1), SKIN[0])
    x, y, w, h = front
    sheet.fill((x + S, y + h - S, w - 2 * S, 1), SKIN[0])          # the line of his lip
    sheet.fill((x + w // 2, y + 2 * S, 1, h - 3 * S), SKIN[1])
    # The jaw: inside, the dark of his mouth, his tongue and lower teeth; outside, lip and chin, and a beard of fur.
    f = sheet.faces("jaw")
    for name, rect in f.items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, MOUTH[1])
            sheet.fill(cell(rect, 2, 3, 5, 7), MOUTH[3])
            sheet.fill(cell(rect, 2, 3, 5, 1), MOUTH[2])
            sheet.fill((x + w // 2, y + 4 * S, 1, 5 * S), MOUTH[2])
            sheet.fill((x, y + h - S, w, S), FANG[2])
            for c in range(1, w // S, 2):
                sheet.fill((x + c * S, y + h - S, 1, S), FANG[0])
        elif name == "bottom":
            fur_top(sheet, rect, darker=1)
        elif name == "front":
            sheet.fill(rect, FUR[3])
            skin(sheet, (x, y, w, S))
            sheet.fill((x, y + S, w, 1), SKIN[1])
        else:
            locks(sheet, rect, phase=2, darker=1, hem=False)
    # His fangs: ivory, lit on one side.
    for name, rect in sheet.faces("fang").items():
        x, y, w, h = rect
        sheet.fill(rect, FANG[1])
        sheet.fill((x, y, max(1, w // 2), h), FANG[2])
        sheet.fill((x, y + h - 1, w, 1), FANG[0])


def paint_crown(sheet):
    # The band: blue ice carved in a row of facets, lit along its top.
    for name, rect in sheet.faces("band").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, ICE[3])
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), ICE[1])   # its hollow, where the head is
        elif name == "bottom":
            sheet.fill(rect, ICE[0])
        else:
            sheet.fill(rect, ICE[2])
            for c in range(0, w // S, 2):
                sheet.fill(cell(rect, c, 0, 1, 1), ICE[3])
                sheet.fill(cell(rect, c + 1, 1, 1, 1), ICE[1])
            sheet.fill((x, y, w, 2), ICE[4])
            sheet.fill((x, y + h - 1, w, 1), ICE[0])
    for part in ("point_tall", "point", "point_low"):
        ice(sheet, part)
    # The Fury of the Peaks: fire round the points, and up from his eyes.
    blaze(sheet, "blaze_tall")
    blaze(sheet, "blaze")
    eye_fire(sheet)


def paint_held(sheet):
    """The block of ice he heaves over his head, crusted with snow on top."""
    packed_ice(sheet, "held_ice")


def packed_ice(sheet, part, crust=True):
    """Packed ice in facets on the grid: a lit diagonal band across each side, the top crusted with snow drifting over
    its edges."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, SNOW[1] if crust else PACKED[3])
            if crust:
                for cy in range(0, h // S, 3):
                    for cx in range(cy % 2, w // S, 3):
                        sheet.fill(cell(rect, cx, cy), SNOW[2])
            continue
        if name == "bottom":
            sheet.fill(rect, PACKED[0])
            continue
        sheet.fill(rect, PACKED[2])
        cols, rows = w // S, h // S
        for cy in range(rows):
            for cx in range(cols):
                band = (cx + cy) % 7
                if band == 0:
                    sheet.fill(cell(rect, cx, cy), PACKED[3])
                elif band == 4:
                    sheet.fill(cell(rect, cx, cy), PACKED[1])
        sheet.fill((x, y + h - 2, w, 2), PACKED[0])
        if crust:
            # Snow over the top edge, its lower edge rising and falling cell by cell.
            for cx in range(cols):
                depth = (1, 2, 2, 1, 1, 2)[cx % 6]
                sheet.fill(cell(rect, cx, 0, 1, depth), SNOW[1])
                sheet.fill(cell(rect, cx, 0, 1, 1), SNOW[2])


def yeti_king():
    sheet = Sheet(ym.yeti_king_model())
    paint_body(sheet)
    paint_feet(sheet)
    paint_arms(sheet)
    paint_head(sheet)
    paint_crown(sheet)
    paint_held(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- his whelps

def whelp():
    sheet = Sheet(ym.whelp_model())
    fur(sheet, "cub_body", phase=2, hem=True)
    x, y, w, h = sheet.faces("cub_body")["front"]
    sheet.fill((x + 2 * S, y + S, w - 4 * S, h - 3 * S), FUR[4])   # a pale fluffy belly
    fur(sheet, "cub_skull", phase=5, hem=False)
    fur(sheet, "cub_leg", phase=1, darker=1, hem=False)
    sheet.fill(sheet.faces("cub_leg")["bottom"], SKIN[2])
    fur(sheet, "cub_arm", phase=3, hem=True)
    sheet.fill(sheet.faces("cub_arm")["bottom"], SKIN[2])
    # The face: pale skin round two big eyes, a dark button nose on the muzzle.
    front = sheet.faces("cub_skull")["front"]
    sheet.fill(cell(front, 1, 2, 7, 6), SKIN[3])
    sheet.fill(cell(front, 0, 1, 9, 1), FUR[4])
    for cx in (1, 6):
        x, y, w, h = cell(front, cx, 3, 2, 2)
        sheet.fill((x, y, w, h), EYE[1])
        sheet.fill((x, y, w, S), EYE[2])
        sheet.fill((x + 2, y + 3, w - 3, h - 3), PUPIL)
        sheet.fill((x + 2, y + 2, 2, 2), EYE[3])
    for name, rect in sheet.faces("cub_muzzle").items():
        skin(sheet, rect)
    x, y, w, h = sheet.faces("cub_muzzle")["front"]
    sheet.fill((x + S + 2, y, w - 2 * S - 4, S + 1), PUPIL)
    sheet.fill((x + w // 2, y + S + 1, 1, h - S - 2), SKIN[0])
    for name, rect in sheet.faces("cub_ear").items():
        sheet.fill(rect, FUR[3])
        if name == "front":
            sheet.fill((rect[0] + 1, rect[1] + 1, rect[2] - 2, rect[3] - 2), SKIN[3])
    ice(sheet, "cub_tuft", base=2)
    return sheet


# ------------------------------------------------------------------------------------------------- his things

def boulder():
    sheet = Sheet(ym.boulder_model())
    packed_ice(sheet, "boulder")
    packed_ice(sheet, "lump", crust=False)
    packed_ice(sheet, "chunk", crust=False)
    for name, rect in sheet.faces("crust").items():
        x, y, w, h = rect
        sheet.fill(rect, SNOW[1])
        sheet.fill((x, y, w, 2), SNOW[2])
        if name not in ("top", "bottom"):
            sheet.fill((x, y + h - 2, w, 2), SNOW[0])
    return sheet


def icicle():
    sheet = Sheet(ym.icicle_model())
    # The broken stump: rime over clear ice, its broken top showing the ice's core.
    for name, rect in sheet.faces("icicle_stump").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, SNOW[1])
            sheet.fill(cell(rect, 2, 2, 4, 4), ICE[3])
            sheet.fill(cell(rect, 3, 3, 2, 2), ICE[2])
        elif name == "bottom":
            sheet.fill(rect, ICE[2])
        else:
            sheet.fill(rect, ICE[3])
            sheet.fill((x, y, w, 2 * S), SNOW[1])
            for c in range(0, w // S, 2):
                sheet.fill(cell(rect, c, 2, 1, 1), SNOW[1])
            sheet.fill((x + S, y, 2, h), ICE[4])
    # Clearer and paler towards the point, a lit streak down each side.
    for part, tone in (("icicle_upper", 3), ("icicle_lower", 3), ("icicle_tip", 4)):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, ICE[tone] if name != "bottom" else ICE[4])
            if name not in ("top", "bottom"):
                sheet.fill((x, y, max(1, w // 3), h), ICE[4])
                sheet.fill((x + w - 2, y, 2, h), ICE[tone - 1])
    return sheet


def spike():
    sheet = Sheet(ym.spike_model())
    ice(sheet, "shard", base=2)
    for name, rect in sheet.faces("shard").items():
        if name not in ("top", "bottom"):
            x, y, w, h = rect
            # Cracks through the shard on the slant, on the grid.
            for cy in range(2, h // S - 1, 5):
                for i in range(3):
                    sheet.fill(cell(rect, 1 + i, cy + i, 1, 1), ICE[3])
    ice(sheet, "shard_tip", base=3)
    ice(sheet, "shard_side", base=1)
    for name, rect in sheet.faces("rubble").items():
        x, y, w, h = rect
        sheet.fill(rect, SNOW[1])
        if name == "top":
            for cy in range(0, h // S, 3):
                for cx in range(cy % 2, w // S, 3):
                    sheet.fill(cell(rect, cx, cy), PACKED[2])
        elif name != "bottom":
            sheet.fill((x, y, w, 2), SNOW[2])
    return sheet


# ------------------------------------------------------------------------------------------------- the worn crown

def crown(part):
    """The worn crown's textures (16x16): its points, clear ice in vertical facets lit on the left; and its band, lit
    along its top, shadowed below, carved with a row of facets."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "ice":
                tone = ICE[4] if x % 4 == 0 else ICE[3] if x % 4 == 1 else ICE[2] if x % 4 == 2 else ICE[1]
                if y < 2:
                    tone = ICE[4]
            else:
                tone = (ICE[4] if y < 2 else ICE[0] if y > 13 else ICE[3] if (x + y) % 4 == 0 and 4 <= y <= 11
                        else ICE[2])
            px[x, y] = tone + (255,)
    return img


def yeti_king_textures():
    """Every texture of the Yeti King's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {("item", f"yeti_king_crown_{part}"): crown(part) for part in ("ice", "band")}
    sheets = {"yeti_king": yeti_king(), "yeti_whelp": whelp(), "hurled_boulder": boulder(), "falling_icicle": icicle(),
              "glacial_spike": spike()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in yeti_numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in yeti_numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
