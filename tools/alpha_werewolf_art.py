"""Original textures for the Alpha Werewolf (docs/features/alpha-werewolf.md) (requires Pillow): the sheet of his GeckoLib
body (tools/alpha_werewolf_models.py) and its glowmask, painted box by box and face by face at TEXTURE_SCALE times the
size the model declares (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style (tools/vesperine_art.py,
whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise, patterns on the model's
pixel grid.

- The Alpha: charcoal-black fur in long locks over his body, arms and legs, a hump of silver-grey mane across his
  shoulders, a silver ridge down his back, a silver ruff at his throat and a silver tip to his tail; a heavy silver
  brow; a grey muzzle with a pale scar raked across it; a black nose, ivory fangs, a dark mouth; his left ear pricked
  and his right one torn, both pink-grey inside; bare dark skin on his palms and pads, ivory claws darkening to their
  points; and eyes that burn moon-white with a dark slit. His glowmask holds his eyes.
- The worn hood's textures (his silver-grey fur and his muzzle), and the icons of the Alpha Pelt, the Packfinder Charm
  and the Alpha's Hood (tools/item_icons/).

His Packmates are drawn in the werewolves' own fur (tools/werewolf_textures.py). Called from
crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture, is read,
traced or recoloured.
"""
from PIL import Image

import alpha_werewolf_models as am
import item_icons
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet

S = am.TEXTURE_SCALE

# Palettes, darkest first.
BLACK = [rgb("0b0c10"), rgb("14161c"), rgb("1e2129"), rgb("2a2e38"), rgb("383d4a")]
SILVER = [rgb("5a606b"), rgb("7d8490"), rgb("a2a9b5"), rgb("c6ccd6"), rgb("e4e8ef")]
SKIN = [rgb("15111a"), rgb("221b26"), rgb("332a37"), rgb("473b4b")]
CLAW = [rgb("4e473b"), rgb("80776a"), rgb("b8ad98"), rgb("e2d9c4")]
FANG = [rgb("8a826e"), rgb("c8bfa6"), rgb("eee7d4")]
MOUTH = [rgb("190a0e"), rgb("3a1218"), rgb("652029"), rgb("8e3440")]
NOSE = [rgb("08080a"), rgb("141418"), rgb("2c2c34"), rgb("5a5a66")]
EAR_INNER = [rgb("3a2c34"), rgb("5a4450"), rgb("7a5e6c")]
SCAR = [rgb("6e5f66"), rgb("9a8890"), rgb("c4b4ba")]
EYE = [rgb("9fc0e6"), rgb("d6e8fb"), rgb("f6fbff")]
PUPIL = rgb("0a0d14")

# The tone (an index into a fur palette) of each model-pixel column of a lock pattern, repeating: wide locks, a lit one,
# a shadowed parting.
LOCKS = (2, 2, 3, 3, 2, 2, 1, 2, 2, 3, 2, 1, 1, 2)
FACE_PHASE = {"right": 0, "front": 3, "left": 6, "back": 1}


# ------------------------------------------------------------------------------------------------- fur

def locks(sheet, rect, pal, phase=0, lift=0, hem=True, tips=None):
    """Fur hanging in long locks down a face: each model-pixel column a flat tone (LOCKS, `lift` tones up), a shadowed
    parting where a lock gives way to a darker one; with `tips` (a palette), the locks' last model-pixel row in its
    tones; with `hem`, their ends pointed along the bottom edge."""
    x, y, w, h = rect
    cols = max(1, w // S)
    top = len(pal) - 1
    for c in range(cols):
        tone = min(top, LOCKS[(c + phase) % len(LOCKS)] + lift)
        sheet.fill((x + c * S, y, S, h), pal[tone])
        if tone < min(top, LOCKS[(c - 1 + phase) % len(LOCKS)] + lift):
            sheet.fill((x + c * S, y, 1, h), pal[max(0, tone - 1)])
        if tips:
            sheet.fill((x + c * S, y + h - S, S, S), tips[min(len(tips) - 1, tone)])
    if hem and h >= 2 * S:
        for c in range(1, cols, 2):
            left = x + c * S
            for j in range(S):
                sheet.fill((left - j // 2, y + h - S + j, 1 + j, 1), pal[0])


def fur_top(sheet, rect, pal, lift=0):
    """The top of a furred part: the fur's lit tone, its locks' ends showing as short strokes on the grid."""
    x, y, w, h = rect
    top = len(pal) - 1
    tone = lambda t: pal[max(0, min(top, t + lift))]
    sheet.fill(rect, tone(3))
    for cy in range(0, h // S, 3):
        for cx in range((cy // 3) % 2, w // S, 3):
            sheet.fill((x + cx * S, y + cy * S, S, S // 2), tone(4))
            sheet.fill((x + cx * S, y + cy * S + S // 2, 1, S // 2), tone(1))


def fur(sheet, part, pal=BLACK, phase=0, lift=0, hem=True, tips=None):
    """A part in fur: its sides in locks, its top lit, its underside in shadow."""
    for name, rect in sheet.faces(part).items():
        if name == "top":
            fur_top(sheet, rect, pal, lift)
        elif name == "bottom":
            sheet.fill(rect, pal[max(0, lift)])
        else:
            locks(sheet, rect, pal, phase + FACE_PHASE[name], lift, hem, tips)


def skin(sheet, rect, lit=True):
    """Bare dark skin: flat, a tone lighter along its top edge."""
    x, y, w, h = rect
    sheet.fill(rect, SKIN[2])
    if lit:
        sheet.fill((x, y, w, S // 2), SKIN[3])


def ivory(sheet, part, pal=CLAW, dark_tip=True):
    """Claw or fang: lit on its left, darker toward its point (the bottom); with `dark_tip`, its last row darkest."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, pal[-2] if name in ("top", "front", "right") else pal[-3])
        sheet.fill((x, y, max(1, w // 3), h), pal[-1])
        if dark_tip and h >= 2 * S:
            sheet.fill((x, y + h - S, w, S), pal[0])
            sheet.fill((x, y + h - 2 * S, w, S), pal[1])


# ------------------------------------------------------------------------------------------------- his parts

def paint_body(sheet):
    """His chest, hips and back in black fur; the hump of mane, the ridge down his back and his ruff in silver."""
    fur(sheet, "chest", BLACK, phase=0, lift=0)
    fur(sheet, "pelvis", BLACK, phase=5, lift=0)
    fur(sheet, "mane", SILVER, phase=2, lift=0, tips=SILVER[:3])
    # The mane's top a tone darker than a lit top: seen from above, it should not glare.
    fur_top(sheet, sheet.faces("mane")["top"], SILVER, lift=-1)
    fur(sheet, "mane_ridge", SILVER, phase=7, lift=0)
    fur(sheet, "ruff", SILVER, phase=4, lift=0)
    fur(sheet, "neck", BLACK, phase=9, lift=1)
    fur(sheet, "tail_root", BLACK, phase=1)
    fur(sheet, "tail_brush", BLACK, phase=4)
    fur(sheet, "tail_tip", SILVER, phase=6, lift=0)


def paint_limbs(sheet):
    """His arms and legs in black fur, a tone lighter on his shoulders; dark skin on his palms, knuckles, pads and toes;
    ivory claws."""
    fur(sheet, "shoulder", BLACK, phase=3, lift=1)
    for part, phase in (("upper_arm", 0), ("forearm", 6), ("thigh", 2), ("shin", 8), ("hock", 4)):
        fur(sheet, part, BLACK, phase=phase)
    for part in ("hand", "paw"):
        for name, rect in sheet.faces(part).items():
            if name == "top":
                fur_top(sheet, rect, BLACK)
            elif name == "bottom":
                # The pads of his palm or his paw: dark skin, a pad lighter in its middle.
                x, y, w, h = rect
                sheet.fill(rect, SKIN[1])
                sheet.fill((x + w // 4, y + h // 4, w // 2, h // 2), SKIN[3])
            elif name == "front":
                skin(sheet, rect)
                x, y, w, h = rect
                for k in range(1, w // S, 2):
                    sheet.fill((x + k * S, y, 1, h), SKIN[0])
            else:
                locks(sheet, rect, BLACK, FACE_PHASE[name], 0, hem=False)
    ivory(sheet, "claw")
    ivory(sheet, "toe_claw", dark_tip=False)


def paint_head(sheet):
    """His head: black fur on his skull and neck, a heavy silver brow, a grey muzzle raked by a pale scar, a glossy
    black nose, a dark mouth and tongue on his jaw, ivory fangs, silver cheek tufts, ears pink-grey inside (the right
    one torn), and moon-white eyes with a dark slit."""
    fur(sheet, "skull", BLACK, phase=2, lift=1, hem=False)
    # The skull's face, between his eyes: darker fur in a short widow's peak under the brow.
    x, y, w, h = sheet.faces("skull")["front"]
    sheet.fill((x + w // 2 - S, y, 2 * S, h // 2), BLACK[1])
    fur(sheet, "brow", SILVER, phase=1, lift=1, hem=False)
    fur(sheet, "cheek", SILVER, phase=3, lift=0)
    # The muzzle: grey along its top, black down its sides to a dark lip; the scar across its top and down its right.
    for name, rect in sheet.faces("muzzle").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, SILVER[1])
            for c in range(0, w // S, 3):
                sheet.fill((x + c * S, y, S, h), SILVER[2])
            # The scar: three model pixels wide, slanting across the muzzle's top.
            for i in range(min(w, h) // S):
                sheet.fill((x + w - (i + 2) * S, y + i * S, 2 * S, S), SCAR[1])
                sheet.fill((x + w - (i + 2) * S, y + i * S, 2 * S, 1), SCAR[2])
        elif name == "bottom":
            sheet.fill(rect, MOUTH[0])
        elif name == "front":
            sheet.fill(rect, BLACK[2])
            sheet.fill((x, y + h - S, w, S), MOUTH[0])
        else:
            locks(sheet, rect, BLACK, FACE_PHASE[name], 1, hem=False)
            sheet.fill((x, y + h - S, w, S), MOUTH[0])
            if name == "right":
                sheet.fill((x + w // 3, y, S, h - S), SCAR[1])
    # The nose: glossy black, its top lit.
    for name, rect in sheet.faces("nose").items():
        x, y, w, h = rect
        sheet.fill(rect, NOSE[1])
        if name in ("top", "front"):
            sheet.fill((x + S // 2, y + S // 2, S, S // 2), NOSE[3])
    ivory(sheet, "fang", FANG + [FANG[-1]], dark_tip=False)
    # The jaw: black fur outside, the mouth and tongue on its top (inside), a grey throat beneath.
    for name, rect in sheet.faces("jaw").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, MOUTH[1])
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), MOUTH[3])
            sheet.fill((x + w // 2 - S // 2, y + S, S, h - 2 * S), MOUTH[2])
        elif name == "bottom":
            sheet.fill(rect, SILVER[0])
        else:
            locks(sheet, rect, BLACK, FACE_PHASE[name], 1, hem=False)
            sheet.fill((x, y, w, S // 2), MOUTH[0])
    # The ears: black fur outside, pink-grey inside with a dark rim, silver at the tip; the right one torn short.
    for part, torn in (("ear", False), ("ear_torn", True)):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "front":
                sheet.fill(rect, BLACK[1])
                sheet.fill((x + S // 2, y + S, w - S, h - S), EAR_INNER[1])
                sheet.fill((x + S, y + 2 * S, w - 2 * S, h - 3 * S), EAR_INNER[2])
            elif name == "top":
                sheet.fill(rect, SILVER[2])
            else:
                locks(sheet, rect, BLACK, FACE_PHASE.get(name, 0), 1, hem=False)
                if name != "bottom":
                    sheet.fill((x, y, w, S), SILVER[2])
            if torn and name in ("front", "back", "right", "left"):
                # A notch torn from its top: cut out, a ragged step down from one corner.
                for i in range(S):
                    for j in range(S + S // 2 - i):
                        sheet.clear(x + w - 1 - i, y + j)
                        sheet.clear(x + w - 1 - S - i // 2, y + j // 2)
    # The eyes: moon-white, a dark slit down the middle, burning (on the glowmask too).
    for name, rect in sheet.faces("eye").items():
        x, y, w, h = rect
        for glow in (False, True):
            sheet.fill(rect, EYE[1], glow=glow)
            sheet.fill((x, y, w, S // 2), EYE[2], glow=glow)
            sheet.fill((x + w // 2 - 1, y, 2, h), PUPIL, glow=glow)


def alpha_werewolf():
    sheet = Sheet(am.alpha_werewolf_model())
    paint_body(sheet)
    paint_limbs(sheet)
    paint_head(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- the worn hood

def hood_fur():
    """The hood's silver-grey fur (16 x 16): locks hanging down, lit along their tops."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for x in range(16):
        tone = SILVER[1 + LOCKS[x % len(LOCKS)]] if 1 + LOCKS[x % len(LOCKS)] < len(SILVER) else SILVER[-1]
        for y in range(16):
            px[x, y] = tuple(tone) + (255,)
        px[x, 0] = tuple(SILVER[4]) + (255,)
    for x in range(1, 16, 3):
        px[x, 15] = tuple(SILVER[0]) + (255,)
    return img


def hood_muzzle():
    """The hood's muzzle (16 x 16): black fur shading to grey along its top, the scar across it, the black nose at its
    front middle."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for x in range(16):
        for y in range(16):
            c = SILVER[1] if y < 4 else BLACK[2 + (x % 3 == 0)]
            px[x, y] = tuple(c) + (255,)
    for i in range(6):
        px[10 - i, 2 + i] = tuple(SCAR[2]) + (255,)
        px[11 - i, 2 + i] = tuple(SCAR[1]) + (255,)
    for x in range(6, 10):
        for y in range(11, 15):
            px[x, y] = tuple(NOSE[1] if (x, y) != (7, 11) else NOSE[3]) + (255,)
    return img


def alpha_werewolf_textures():
    """Every texture of the Alpha's: (folder, name) -> image."""
    sheet = alpha_werewolf()
    return {
        ("entity", "alpha_werewolf"): sheet.img,
        ("entity", "alpha_werewolf_glowmask"): sheet.glow,
        ("item", "alphas_hood_fur"): hood_fur(),
        ("item", "alphas_hood_muzzle"): hood_muzzle(),
        ("item", "alpha_pelt"): item_icons.draw("alpha_pelt"),
        ("item", "packfinder_charm"): item_icons.draw("packfinder_charm"),
        ("item", "alphas_hood"): item_icons.draw("alphas_hood"),
    }


if __name__ == "__main__":
    import sys
    for (folder, name), image in alpha_werewolf_textures().items():
        image.save(f"{sys.argv[1]}/{name}.png")
