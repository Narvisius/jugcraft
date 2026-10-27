"""Original textures for the Pumpkin King (docs/features/pumpkin-king.md) (requires Pillow): the sheets of his GeckoLib
bodies (tools/pumpkin_king_models.py) and their glowmasks, painted box by box and face by face at TEXTURE_SCALE times
the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- The King: a harvest coat of tan burlap woven on the grid, patched in faded red, green and blue-grey with stitches round
  each patch, its tails frayed; sacking trousers, a patch on the right knee, tied at the ankles with rope; straw bursting
  from the ties, the cuffs and the shoulders; a rope belt; hands of pale corn husk, veined; a stick of a neck; a giant
  pumpkin, ribbed deep orange, its carved eyes, nose and grin dark inside with a lighter rim of flesh; a green stem; a
  crown of dried vines twisted round, five ears of gold corn with husk leaves; a crow on his shoulder, black, its eye a
  glint, its beak grey; and his scythe, a snath of pale bound corn stalks with nodes, three bands of twine, a dark iron
  socket and a rusted blade with a dull edge. His glowmask holds the fire in his face: two sets of planes, orange (his
  fire) and white-hot.
- His Jacks: sacking tunics belted with twine, limbs of bound straw, husk hands, and a warty yellow-green gourd for a head
  with its carved face lit (on the glowmask too).
- His rolling head (his pumpkin and crown, its face lit) and the tossed pumpkin (smaller, its face lit).
- The King's Lantern's block faces, the worn crown's textures, and the icons of the King's Ember, the Crowward Charm
  and the crown (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
from PIL import Image

import item_icons
import pumpkin_king_models as pm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, ragged

S = pm.TEXTURE_SCALE

# Palettes, darkest first.
BURLAP = [rgb("3a2a17"), rgb("5a4325"), rgb("7a5c35"), rgb("9a7848"), rgb("b8955f")]
SACKING = [rgb("2a2318"), rgb("443a29"), rgb("5e523b"), rgb("786b4f"), rgb("928565")]
PATCH_RED = [rgb("3e1612"), rgb("62261d"), rgb("84382a")]
PATCH_GREEN = [rgb("1e2a1a"), rgb("2f4228"), rgb("435c38")]
PATCH_BLUE = [rgb("222a33"), rgb("343f4a"), rgb("4a5662")]
STITCH = rgb("d8c79c")
STRAW = [rgb("6e5418"), rgb("9c7a26"), rgb("c49c3a"), rgb("e0bc58"), rgb("f2d880")]
ROPE = [rgb("4a3a20"), rgb("6e5830"), rgb("8e7442"), rgb("ad9258")]
HUSK = [rgb("56562c"), rgb("787640"), rgb("9a9658"), rgb("b9b374"), rgb("d6d096")]
STICK = [rgb("2a1e12"), rgb("3e2c1a"), rgb("543e26")]
PUMPKIN = [rgb("5a1e04"), rgb("8a3408"), rgb("b84e10"), rgb("dc6c1c"), rgb("f08e34"), rgb("f8b060")]
HOLLOW = [rgb("140602"), rgb("261006")]
FLESH = [rgb("c87a22"), rgb("e8a040")]
STEM = [rgb("2a2a12"), rgb("44441e"), rgb("5e5e2a"), rgb("7a7a3a")]
VINE = [rgb("261f12"), rgb("3b321c"), rgb("524628"), rgb("6a5c36")]
CORN = [rgb("7a5410"), rgb("a8801c"), rgb("d0a830"), rgb("ecca50"), rgb("f8e488")]
LEAF = [rgb("6a6038"), rgb("8c824c"), rgb("aea464"), rgb("cac080")]
RUST = [rgb("2a120a"), rgb("4a2010"), rgb("6c3218"), rgb("8c4822"), rgb("a8663a")]
EDGE = [rgb("6a6460"), rgb("928c84"), rgb("b4aea4")]
IRON = [rgb("1a1a1e"), rgb("2c2c32"), rgb("404048")]
SNATH = [rgb("605828"), rgb("827838"), rgb("a29850"), rgb("bcb26a")]
TWINE = [rgb("54442a"), rgb("76643e"), rgb("948054")]
CROW = [rgb("08080c"), rgb("121216"), rgb("1c1c24"), rgb("282832"), rgb("383846")]
BEAK = [rgb("26262a"), rgb("3a3a40"), rgb("56565e")]
GOURD = [rgb("4a4a14"), rgb("6c6c1e"), rgb("8e8a2c"), rgb("b0a63c"), rgb("cfc258")]
FLAME = [rgb("b03c06"), rgb("e86a14"), rgb("ffa028"), rgb("ffd458"), rgb("fff2b0")]
WHITE = [rgb("9cc0f0"), rgb("c8deff"), rgb("eaf4ff"), rgb("ffffff")]
EMBER = [rgb("2a0c02"), rgb("5a1a04"), rgb("a43a08"), rgb("ff8a1c"), rgb("ffd060")]


# ------------------------------------------------------------------------------------------------- materials

def weave(sheet, rect, pal, phase=0):
    """Burlap or sacking woven on the model's grid: rows of alternating warp and weft, a thread a model pixel, two flat
    tones with a darker line between rows."""
    x, y, w, h = rect
    sheet.fill(rect, pal[2])
    for cy in range(h // S):
        for cx in range(w // S):
            if (cx + cy + phase) % 2 == 0:
                sheet.fill((x + cx * S, y + cy * S, S, S // 2), pal[3])
        sheet.fill((x, y + cy * S + S - 1, w, 1), pal[1])


def cloth(sheet, part, pal, phase=0, dark_bottom=True):
    """A part in woven cloth: its sides woven, its top a tone lighter, its underside in shadow."""
    for name, rect in sheet.faces(part).items():
        if name == "bottom" and dark_bottom:
            sheet.fill(rect, pal[1])
        else:
            weave(sheet, rect, pal, phase + {"front": 0, "back": 1, "right": 0, "left": 1, "top": 1, "bottom": 0}[name])


def patch(sheet, rect, cx, cy, cw, ch, pal):
    """A patch of `pal` cloth sewn on a face at model pixel (cx, cy), cw by ch, stitched round with pale thread."""
    x, y, _, _ = rect
    px, py, pw, ph = x + cx * S, y + cy * S, cw * S, ch * S
    sheet.fill((px, py, pw, ph), pal[1])
    for i in range(cw):
        for j in range(ch):
            if (i + j) % 2 == 0:
                sheet.fill((px + i * S, py + j * S, S, S // 2), pal[2])
    sheet.fill((px, py + ph - 1, pw, 1), pal[0])
    for i in range(0, pw, S):
        sheet.fill((px + i + 1, py, 2, 1), STITCH)
        sheet.fill((px + i + 1, py + ph - 1, 2, 1), STITCH)
    for j in range(0, ph, S):
        sheet.fill((px, py + j + 1, 1, 2), STITCH)
        sheet.fill((px + pw - 1, py + j + 1, 1, 2), STITCH)


def straw(sheet, part, tufted=True, phase=0):
    """Straw: strands down every face, a model pixel each, in four tones; with `tufted`, the bottoms of the sides cut into
    points (the cut-out sheet leaves them open)."""
    order = (2, 3, 1, 4, 2, 3, 2, 1, 3)
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, STRAW[2] if name == "top" else STRAW[1])
            for cx in range(0, w // S, 2):
                sheet.fill((x + cx * S, y, S // 2, h), STRAW[3] if name == "top" else STRAW[0])
            continue
        for cx in range(w // S):
            tone = STRAW[order[(cx + phase) % len(order)]]
            sheet.fill((x + cx * S, y, S, h), tone)
            sheet.fill((x + cx * S, y, 1, h), STRAW[0])
        if tufted and h >= 2 * S:
            ragged(sheet, rect, every=2 * S, depth=S, offset=phase)


def rope(sheet, part, pal=ROPE):
    """Rope or twine: twisted strands, a diagonal stripe each model pixel."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, pal[1])
        for i in range(-h, w, S):
            for j in range(h):
                if 0 <= i + j < w:
                    sheet.fill((x + i + j, y + j, max(1, S // 2), 1), pal[2])
        sheet.fill((x, y + h - 1, w, 1), pal[0])


def husk(sheet, part):
    """Corn husk: pale, veined lengthwise, darker toward its tip."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, HUSK[2] if name != "bottom" else HUSK[1])
        for i in range(0, w, 2):
            sheet.fill((x + i, y, 1, h), HUSK[3])
        if name not in ("top", "bottom"):
            sheet.fill((x, y + h - S, w, S), HUSK[1])


# ------------------------------------------------------------------------------------------------- his pumpkin

# Where the carved face is on the pumpkin's front, in model pixels from the face's top left (tools/pumpkin_king_models.py
# face_planes: the eyes 4 by 3, the nose 2 by 2, the grin 12 by 3; the pumpkin 20 wide and 16 high).
FACE_HOLES = {"eye_right": (3, 4, 4, 3), "eye_left": (13, 4, 4, 3), "nose": (9, 7, 2, 2), "grin": (4, 11, 12, 3)}


def ribbed(sheet, rect, pal, rib=4):
    """A pumpkin's side: ribs `rib` model pixels wide, each lit along its middle and shadowed in its grooves, darker
    toward the foot and shoulder."""
    x, y, w, h = rect
    sheet.fill(rect, pal[3])
    for i in range(w):
        p = (i // S) % rib
        tone = pal[2] if p == 0 else pal[4] if p == rib // 2 else pal[3]
        sheet.fill((x + i, y, 1, h), tone)
    sheet.fill((x, y, w, S), pal[2])
    sheet.fill((x, y + h - S, w, S), pal[1])
    for i in range(0, w, rib * S):
        sheet.fill((x + i, y, 1, h), pal[1])


def carve(sheet, rect, holes, flesh=FLESH, hollow=HOLLOW):
    """Carves `holes` ({name: (cx, cy, cw, ch)} in model pixels) into a face: a rim of pale flesh, dark inside; the eyes
    cut as triangles, their top corners left uncut."""
    x, y, _, _ = rect
    for name, (cx, cy, cw, ch) in holes.items():
        px, py, pw, ph = x + cx * S, y + cy * S, cw * S, ch * S
        sheet.fill((px - 1, py - 1, pw + 2, ph + 2), flesh[0])
        sheet.fill((px, py, pw, ph), hollow[0])
        sheet.fill((px, py, pw, 2), hollow[1])
        if name.startswith("eye"):
            # A triangle: the top row whole, narrowing to a point at the bottom.
            for j in range(ph):
                inset = int(j * pw / (2 * ph))
                sheet.fill((px - 1, py + j, inset + 1, 1), flesh[1] if j else flesh[0])
                sheet.fill((px + pw - inset, py + j, inset + 1, 1), flesh[1] if j else flesh[0])
        if name.endswith("grin"):
            # Two teeth left standing in the grin.
            for tooth in (pw // 3, 2 * pw // 3):
                sheet.fill((px + tooth - S // 2, py, S, S), flesh[1])


def paint_pumpkin(sheet, part="pumpkin", holes=FACE_HOLES):
    """The pumpkin: ribbed sides, its top ribs meeting under the stem, its foot in shadow, and its carved face."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, PUMPKIN[3])
            for i in range(0, w, 4 * S):
                sheet.fill((x + i, y, S // 2, h), PUMPKIN[2])
            sheet.fill((x + w // 2 - 2 * S, y + h // 2 - 2 * S, 4 * S, 4 * S), PUMPKIN[2])
        elif name == "bottom":
            sheet.fill(rect, PUMPKIN[1])
        else:
            ribbed(sheet, rect, PUMPKIN)
            if name == "front" and holes:
                carve(sheet, rect, holes)


def paint_crown(sheet, ears=("ear", "ear_tall")):
    """The crown: the band of dried vine twisted round; each ear of corn in rows of gold kernels, its tip paler; the husk
    leaves pale and veined, their tips ragged; the stem green, its top cut."""
    for name, rect in sheet.faces("crown_band").items():
        x, y, w, h = rect
        sheet.fill(rect, VINE[2])
        for i in range(-h, w, 3 * S):
            for j in range(h):
                if 0 <= i + j < w:
                    sheet.fill((x + i + j, y + j, S, 1), VINE[3])
                if 0 <= i + j + S < w:
                    sheet.fill((x + i + j + S, y + j, 1, 1), VINE[0])
        sheet.fill((x, y + h - 1, w, 1), VINE[0])
    for part in ears:
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, CORN[1])
            for cy in range(h // S):
                for cx in range(w // S):
                    sheet.fill((x + cx * S + 1, y + cy * S + 1, S - 2, S - 2), CORN[3] if (cx + cy) % 2 else CORN[2])
            if name not in ("top", "bottom"):
                sheet.fill((x, y, w, S), CORN[4])
    for name, rect in sheet.faces("husk_leaf").items():
        x, y, w, h = rect
        sheet.fill(rect, LEAF[2])
        sheet.fill((x + w // 2 - 1, y, 2, h), LEAF[3])
        sheet.fill((x, y + h - S, w, S), LEAF[1])
        ragged(sheet, rect, every=w // 2 or 1, depth=S + S // 2, edge="top")
    for name, rect in sheet.faces("stem").items():
        x, y, w, h = rect
        sheet.fill(rect, STEM[2])
        sheet.fill((x, y, max(1, w // 3), h), STEM[3])
        if name == "top":
            sheet.fill(rect, STEM[1])
            sheet.fill((x + S // 2, y + S // 2, w - S, h - S), STEM[3])


def paint_flames(sheet, parts, pal, glow_pal):
    """The fire showing through a carved face: planes filled bright at their heart, darker toward their edges, on the sheet
    and on the glowmask; the eyes' bottom corners cut away so they burn as triangles."""
    for part in parts:
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            for glow, p in ((False, pal), (True, glow_pal)):
                sheet.fill(rect, p[1], glow=glow)
                sheet.fill((x + 1, y + 1, w - 2, h - 2), p[2], glow=glow)
                sheet.fill((x + w // 4, y + h // 4, max(1, w // 2), max(1, h // 2)), p[3], glow=glow)
            if part.endswith("_eye"):
                for j in range(h):
                    inset = int(j * w / (2 * h))
                    for i in range(inset):
                        sheet.clear(x + i, y + j)
                        sheet.clear(x + w - 1 - i, y + j)


# ------------------------------------------------------------------------------------------------- the King

def paint_coat(sheet):
    """The coat: burlap, its front parted down the middle by a dark seam with three wooden toggles, a red patch on the
    right breast, a green one low on the left, a blue-grey one on the back; the collar and pelvis in sacking; the tails
    frayed."""
    cloth(sheet, "coat", BURLAP)
    front = sheet.faces("coat")["front"]
    x, y, w, h = front
    sheet.fill((x + w // 2 - 1, y, 2, h), BURLAP[0])
    for cy in (4, 10, 16):
        sheet.fill((x + w // 2 - S, y + cy * S, 2 * S, S), STICK[2])
    patch(sheet, front, 2, 3, 5, 4, PATCH_RED)
    patch(sheet, front, 11, 12, 5, 5, PATCH_GREEN)
    patch(sheet, sheet.faces("coat")["back"], 6, 6, 6, 6, PATCH_BLUE)
    cloth(sheet, "collar", SACKING, phase=1)
    cloth(sheet, "pelvis", SACKING)
    for part, phase in (("tail_back", 0), ("tail_front", 1)):
        cloth(sheet, part, BURLAP, phase=phase, dark_bottom=False)
        for name, rect in sheet.faces(part).items():
            if name in ("front", "back"):
                ragged(sheet, rect, every=3 * S, depth=2 * S, offset=phase * S)
    patch(sheet, sheet.faces("tail_back")["back"], 4, 6, 4, 4, PATCH_RED)
    rope(sheet, "belt")


def paint_limbs(sheet):
    """Sacking trousers, a patch on the right knee, tied at the ankles with rope, straw below; burlap sleeves, a patch on
    the elbow, straw at the cuffs and shoulders; husk hands; a stick neck."""
    cloth(sheet, "thigh", SACKING)
    cloth(sheet, "shin", SACKING, phase=1)
    patch(sheet, sheet.faces("shin")["front"], 1, 1, 3, 3, PATCH_BLUE)
    rope(sheet, "ankle_tie")
    straw(sheet, "foot_straw")
    cloth(sheet, "sleeve", BURLAP, phase=1)
    cloth(sheet, "forearm", BURLAP)
    patch(sheet, sheet.faces("forearm")["back"], 1, 3, 3, 4, PATCH_GREEN)
    straw(sheet, "cuff_straw", phase=2)
    straw(sheet, "shoulder_straw", tufted=False, phase=4)
    husk(sheet, "husk")
    husk(sheet, "palm")
    for name, rect in sheet.faces("neck").items():
        x, y, w, h = rect
        sheet.fill(rect, STICK[1])
        sheet.fill((x, y, max(1, w // 3), h), STICK[2])


def paint_scythe(sheet):
    """The snath: pale corn stalks bound together, a darker node every few pixels and a groove between stalks; three bands
    of twine; the nib and socket dark iron; the blade rusted, darker along its spine, a dull grey edge along its foot."""
    for name, rect in sheet.faces("snath").items():
        x, y, w, h = rect
        sheet.fill(rect, SNATH[2])
        sheet.fill((x + w // 2, y, 1, h), SNATH[0])
        sheet.fill((x, y, 1, h), SNATH[3])
        for j in range(0, h, 6 * S):
            sheet.fill((x, y + j, w, S // 2), SNATH[1])
    for part in ("snath_foot", "socket", "nib"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, IRON[1] if part != "snath_foot" else SNATH[1])
            sheet.fill((x, y, w, 1), IRON[2])
    rope(sheet, "twine", TWINE + [TWINE[-1]])
    for i in range(len(pm.BLADE)):
        for name, rect in sheet.faces(f"blade_{i}").items():
            x, y, w, h = rect
            sheet.fill(rect, RUST[2])
            for cx in range(0, w, 3 * S):
                sheet.fill((x + cx + S, y + S, S, S), RUST[3])
                sheet.fill((x + cx, y + 2 * S, S, S), RUST[1])
            if name in ("front", "back"):
                sheet.fill((x, y, w, S), RUST[0])
                sheet.fill((x, y + h - S, w, S), EDGE[1])
                sheet.fill((x, y + h - 1, w, 1), EDGE[2])
            elif name == "bottom":
                sheet.fill(rect, EDGE[1])


def paint_crow(sheet):
    """The crow: black, its feathers in scallops of a tone lighter; its head with a bright glint of an eye each side; its
    beak grey; its tail darker."""
    for part in ("crow_body", "crow_head", "crow_tail"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, CROW[2] if part != "crow_tail" else CROW[1])
            for cx in range(0, w, 2 * S):
                for cy in range(0, h, 2 * S):
                    sheet.fill((x + cx, y + cy + S, 2 * S, 1), CROW[3])
    for name in ("right", "left"):
        x, y, w, h = sheet.faces("crow_head")[name]
        sheet.fill((x + w // 2 - 1, y + h // 3, 3, 3), CROW[0])
        sheet.put(x + w // 2, y + h // 3, rgb("e8e4d0"))
    for name, rect in sheet.faces("crow_beak").items():
        sheet.fill(rect, BEAK[1])
        sheet.fill((rect[0], rect[1], rect[2], 1), BEAK[2])


def paint_held(sheet):
    """The pumpkin in his hand, ribbed, uncarved."""
    paint_pumpkin(sheet, "held", holes=None)


def pumpkin_king():
    sheet = Sheet(pm.pumpkin_king_model())
    paint_coat(sheet)
    paint_limbs(sheet)
    paint_scythe(sheet)
    paint_crow(sheet)
    paint_held(sheet)
    paint_pumpkin(sheet)
    paint_crown(sheet)
    paint_flames(sheet, ("flame_eye", "flame_nose", "flame_grin"), FLAME, FLAME)
    paint_flames(sheet, ("white_eye", "white_nose", "white_grin"), WHITE, WHITE)
    return sheet


# ------------------------------------------------------------------------------------------------- his Jacks

JACK_FACE = {"eye_right": (1, 1, 2, 2), "eye_left": (4, 1, 2, 2), "grin": (1, 4, 5, 1)}


def jack_o_straw():
    """A Jack: a sacking tunic belted with twine, straw limbs, husk hands, and a warty gourd head with its face lit."""
    sheet = Sheet(pm.jack_o_straw_model())
    cloth(sheet, "tunic", SACKING)
    for name, rect in sheet.faces("tunic").items():
        if name in ("front", "back", "right", "left"):
            ragged(sheet, rect, every=2 * S, depth=S)
    rope(sheet, "jack_belt", TWINE + [TWINE[-1]])
    straw(sheet, "jack_leg", tufted=False)
    straw(sheet, "jack_arm", tufted=False, phase=3)
    husk(sheet, "jack_husk")
    for name, rect in sheet.faces("gourd").items():
        x, y, w, h = rect
        ribbed(sheet, rect, GOURD + [GOURD[-1]], rib=3)
        for cx in range(1, w // S, 3):
            for cy in range(1, h // S, 2):
                sheet.fill((x + cx * S + 1, y + cy * S + 1, S // 2, S // 2), GOURD[4])
        if name == "front":
            carve(sheet, rect, JACK_FACE)
            for hole in JACK_FACE.values():
                cx, cy, cw, ch = hole
                sheet.fill((x + cx * S, y + cy * S, cw * S, ch * S), FLAME[3], glow=True)
                sheet.fill((x + cx * S, y + cy * S, cw * S, ch * S), FLAME[2])
    for name, rect in sheet.faces("gourd_stem").items():
        sheet.fill(rect, STEM[1])
    return sheet


# ------------------------------------------------------------------------------------------------- his rolling head, and the tossed pumpkin

def kings_head():
    sheet = Sheet(pm.kings_head_model())
    paint_pumpkin(sheet)
    paint_crown(sheet)
    paint_flames(sheet, ("flame_eye", "flame_nose", "flame_grin"), FLAME, FLAME)
    return sheet


def kings_pumpkin():
    sheet = Sheet(pm.kings_pumpkin_model())
    paint_pumpkin(sheet, "small_pumpkin", holes={"eye_right": (1, 2, 3, 2), "eye_left": (6, 2, 3, 2), "grin": (2, 5, 6, 2)})
    for name, rect in sheet.faces("small_stem").items():
        sheet.fill(rect, STEM[2])
    paint_flames(sheet, ("small_eye", "small_grin"), FLAME, FLAME)
    return sheet


# ------------------------------------------------------------------------------------------------- the King's Lantern

def _square(fill):
    return Image.new("RGBA", (16, 16), tuple(fill) + (255,))


def lantern_side():
    """A deep orange pumpkin side, ribbed: four ribs, lit down their middles, dark in their grooves."""
    img = _square(PUMPKIN[3])
    px = img.load()
    for x in range(16):
        tone = PUMPKIN[[1, 3, 4, 3][x % 4]]
        for y in range(16):
            px[x, y] = tuple(tone) + (255,)
        px[x, 0] = tuple(PUMPKIN[2]) + (255,)
        px[x, 15] = tuple(PUMPKIN[1]) + (255,)
    return img


def lantern_top():
    """The top: ribs meeting under a cut green stem, ringed by a twist of dried vine."""
    img = _square(PUMPKIN[3])
    px = img.load()
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            px[x, y] = tuple(PUMPKIN[4] if (x + y) % 4 == 0 else PUMPKIN[3] if d < 6 else PUMPKIN[2]) + (255,)
    for x in range(4, 12):
        for y in (4, 11):
            px[x, y] = tuple(VINE[2 + (x % 2)]) + (255,)
            px[y, x] = tuple(VINE[2 + (x % 2)]) + (255,)
    for x in range(6, 10):
        for y in range(6, 10):
            px[x, y] = tuple(STEM[2] if (x + y) % 2 else STEM[3]) + (255,)
    return img


# The King's grin, 16 x 16: E an ember's glow, e its deeper glow, . the pumpkin.
LANTERN_FACE = [
    "................",
    "................",
    "..e.e.e....e.e..",
    "..EEEEe...eEEEE.",
    "...EEEe...eEEE..",
    "....EE.....EE...",
    "................",
    ".......ee.......",
    "................",
    ".eE..........Ee.",
    ".eEEeEEEEEEeEEe.",
    "..eEE.EEEE.EEe..",
    "...eEEEeeEEEe...",
    ".....eeEEee.....",
    "................",
    "................",
]


def lantern_front():
    """Its face: the King's grin and crowned eyes, carved, his ember glowing through."""
    img = lantern_side()
    px = img.load()
    for y, row in enumerate(LANTERN_FACE):
        for x, c in enumerate(row):
            if c == "E":
                px[x, y] = tuple(EMBER[4]) + (255,)
            elif c == "e":
                px[x, y] = tuple(EMBER[3]) + (255,)
    return img


# ------------------------------------------------------------------------------------------------- the worn crown

def crown_vine():
    """The worn crown's band (16 x 16): dried vine twisted round, a diagonal strand each two pixels."""
    img = _square(VINE[2])
    px = img.load()
    for x in range(16):
        for y in range(16):
            if (x + y) % 4 == 0:
                px[x, y] = tuple(VINE[3]) + (255,)
            elif (x + y) % 4 == 2:
                px[x, y] = tuple(VINE[1]) + (255,)
    return img


def crown_corn():
    """The worn crown's ears of corn (16 x 16): rows of gold kernels, paler toward the tip (the top)."""
    img = _square(CORN[1])
    px = img.load()
    for x in range(16):
        for y in range(16):
            if x % 2 == 0 and y % 2 == 0:
                continue
            px[x, y] = tuple(CORN[4] if y < 3 else CORN[3] if (x // 2 + y // 2) % 2 else CORN[2]) + (255,)
    return img


def crown_husk():
    """The worn crown's husk leaves (16 x 16): pale, veined lengthwise."""
    img = _square(LEAF[2])
    px = img.load()
    for x in range(16):
        for y in range(16):
            if x % 3 == 0:
                px[x, y] = tuple(LEAF[3]) + (255,)
            if y > 12:
                px[x, y] = tuple(LEAF[1]) + (255,)
    return img


def pumpkin_king_textures():
    """Every texture of the Pumpkin King's: (folder, name) -> image."""
    out = {}
    for entity, build in (("pumpkin_king", pumpkin_king), ("jack_o_straw", jack_o_straw), ("kings_head", kings_head),
                          ("kings_pumpkin", kings_pumpkin)):
        sheet = build()
        out[("entity", entity)] = sheet.img
        out[("entity", f"{entity}_glowmask")] = sheet.glow
    out[("block", "kings_lantern_side")] = lantern_side()
    out[("block", "kings_lantern_top")] = lantern_top()
    out[("block", "kings_lantern_front")] = lantern_front()
    out[("item", "pumpkin_kings_crown_vine")] = crown_vine()
    out[("item", "pumpkin_kings_crown_corn")] = crown_corn()
    out[("item", "pumpkin_kings_crown_husk")] = crown_husk()
    for item in ("kings_ember", "crowward_charm", "pumpkin_kings_crown"):
        out[("item", item)] = item_icons.draw(item)
    return out


if __name__ == "__main__":
    import sys
    for (folder, name), image in pumpkin_king_textures().items():
        image.save(f"{sys.argv[1]}/{name}.png")
