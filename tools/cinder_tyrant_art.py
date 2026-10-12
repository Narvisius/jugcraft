"""Original textures for the Cinder Tyrant and his fight (docs/features/cinder-tyrant.md) (requires Pillow): the sheets
of his GeckoLib bodies (tools/cinder_tyrant_models.py) and their glowmasks, painted box by box and face by face at
TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- The Tyrant: a hide of dark basalt scales over a dull ember belly; plates of obsidian with a violet sheen, faceted and
  lit along their edges; a crest of obsidian spikes paling to their tips; magma burning in the seams between his plates
  (and white-hot cores swelling from them in the Molten Heart); a broad flat head with small burning eyes under an
  obsidian brow, pale fangs and a mouth glowing deep inside; clawed feet; a club of basalt columns at his tail's end.
  Under his plates the top of his hide is a cooled crust, seen only when his seams go dark. His glowmask holds the seams,
  the cores, his eyes, his throat and his mouth.
- His Cinderlings: little salamanders of slag, a black crust cracked all over with fire, two bright eyes.
- His things: the gob of magma (fire under a cooling crust) and the falling cinder (glowing rock trailing smoke).
- The worn crest's textures, and the icons of the Tyrant Scale, the Salamander Charm and the crest (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import cinder_tyrant as tyrant_numbers
import cinder_tyrant_models as tm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, cell

S = tm.TEXTURE_SCALE

# Palettes, darkest first.
OBSIDIAN = [rgb("0b0712"), rgb("17101f"), rgb("221832"), rgb("342649"), rgb("4e3b6c"), rgb("7a64a2")]
BASALT = [rgb("17141a"), rgb("221e24"), rgb("2e2930"), rgb("3c363d"), rgb("4f474e")]
COLUMN = [rgb("232023"), rgb("343034"), rgb("47424a"), rgb("5c565d"), rgb("756e74")]
EMBER = [rgb("3a1a12"), rgb("55271a"), rgb("723823"), rgb("8f4b2d")]
CRUST = [rgb("0f0c0d"), rgb("1b1617"), rgb("2a2223"), rgb("3a2e2c")]
MAGMA = [rgb("8c1c06"), rgb("cc3d0a"), rgb("f26b12"), rgb("ffa338"), rgb("ffd877")]
HOT = [rgb("ffb35a"), rgb("ffdc8e"), rgb("fff3d2"), rgb("ffffff")]
EYE = [rgb("c25a08"), rgb("ff9a1a"), rgb("ffd23a"), rgb("fff4b0")]
SLIT = rgb("1a0a04")
MOUTH = [rgb("1c0606"), rgb("3a0d0b"), rgb("62180f")]
BONE = [rgb("6e665a"), rgb("a39a88"), rgb("d6cebb")]
SMOKE = [rgb("2f2b2c"), rgb("474244"), rgb("625c5d"), rgb("807a7a")]

SIDES = ("right", "left", "front", "back")


# ------------------------------------------------------------------------------------------------- materials

def scales(sheet, rect, pal=BASALT, base=2, phase=0):
    """A side of hide in rows of scales on the grid: each scale two model pixels wide and one high, lit along its top,
    a dark notch between it and the next; every other row offset by one."""
    x, y, w, h = rect
    sheet.fill(rect, pal[base])
    for row in range(h // S):
        shift = (row + phase) % 2
        top = y + row * S
        sheet.fill((x, top, w, 1), pal[base + 1])
        for c in range(-shift, w // S, 2):
            left = x + c * S
            sheet.fill((left, top, 1, S), pal[base - 1])
            sheet.fill((left + 1, top + S - 1, 2 * S - 1, 1), pal[base - 1])


def hide(sheet, part, phase=0, belly=True):
    """A part in the Tyrant's hide: its sides in rows of basalt scales, its top a tone lighter, its underside (with
    `belly`) a dull ember, banded across."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            scales(sheet, rect, base=3, phase=phase)
        elif name == "bottom":
            if belly:
                sheet.fill(rect, EMBER[1])
                for r in range(0, h, 2 * S):
                    sheet.fill((x, y + r, w, S // 2), EMBER[2])
                sheet.fill((x + w // 2 - S, y, 2 * S, h), EMBER[2])
            else:
                sheet.fill(rect, BASALT[1])
        else:
            scales(sheet, rect, phase=phase + (1 if name in ("front", "back") else 0))
            if belly:
                sheet.fill((x, y + h - S, w, S), EMBER[1])      # the belly's edge under the scales
                sheet.fill((x, y + h - S, w, 1), EMBER[2])


def obsidian(sheet, part, tip=False, sheen=True):
    """Obsidian, faceted: each side a mid tone with a lit diagonal band across it (the violet sheen), lit along its
    top edge and dark along its right; the top lit, the underside dark; with `tip`, paling to its top as a spike does."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, OBSIDIAN[4] if tip else OBSIDIAN[3])
            sheet.fill((x, y, w, 2), OBSIDIAN[5] if tip else OBSIDIAN[4])
            continue
        if name == "bottom":
            sheet.fill(rect, OBSIDIAN[0])
            continue
        sheet.fill(rect, OBSIDIAN[2])
        if sheen:
            cols, rows = max(1, w // S), max(1, h // S)
            for cy in range(rows):
                for cx in range(cols):
                    band = (cx + cy) % 6
                    if band == 0:
                        sheet.fill(cell(rect, cx, cy), OBSIDIAN[4])
                    elif band == 1:
                        sheet.fill(cell(rect, cx, cy), OBSIDIAN[3])
        if tip:
            sheet.fill((x, y, w, min(h, 2 * S)), OBSIDIAN[4])
            sheet.fill((x, y, w, 2), OBSIDIAN[5])
        sheet.fill((x, y, w, 2), OBSIDIAN[4])
        sheet.fill((x + w - 2, y, 2, h), OBSIDIAN[1])
        sheet.fill((x, y + h - 2, w, 2), OBSIDIAN[1])


def magma(sheet, part, pal=MAGMA, glow=True):
    """Magma burning in a seam: bright along its middle, deepening to its edges; drawn on the glowmask too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px_ in range(x, x + w):
                edge = min(px_ - x, x + w - 1 - px_, py - y, y + h - 1 - py)
                colour = pal[min(len(pal) - 1, 2 + edge // 2)] if pal is MAGMA else pal[min(len(pal) - 1, 1 + edge // 2)]
                sheet.put(px_, py, colour)
                if glow:
                    sheet.put(px_, py, colour, glow=True)


def crust(sheet, rect):
    """The cooled crust on the top of his hide under his plates: near black, crazed with dull cracks on the grid."""
    x, y, w, h = rect
    sheet.fill(rect, CRUST[1])
    for cy in range(0, h // S, 3):
        for cx in range((cy // 3) % 2 * 2, w // S, 4):
            sheet.fill(cell(rect, cx, cy, 2, 1), CRUST[2])
            sheet.fill(cell(rect, cx + 1, cy + 1, 1, 2), CRUST[0])


def columns(sheet, part):
    """Basalt in columns: the sides in upright columns two model pixels wide, each lit on its left and parted from the
    next by a dark seam; the top and underside the columns' ends, hexagon-ish cells on the grid."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, COLUMN[3] if name == "top" else COLUMN[1])
            for cy in range(0, h // S, 2):
                for cx in range((cy // 2) % 2, w // S, 2):
                    sheet.fill(cell(rect, cx, cy), COLUMN[2] if name == "top" else COLUMN[0])
            continue
        sheet.fill(rect, COLUMN[2])
        for c in range(0, w // S, 2):
            left = x + c * S
            sheet.fill((left, y, 1, h), COLUMN[0])
            sheet.fill((left + 1, y, S - 1, h), COLUMN[3])
            # Each column broken across at its own height.
            cut = y + ((c * 5) % max(1, h // S)) * S
            sheet.fill((left, cut, 2 * S, 1), COLUMN[1])
        sheet.fill((x, y, w, 2), COLUMN[4])


# ------------------------------------------------------------------------------------------------- the Tyrant

def paint_body(sheet):
    hide(sheet, "torso", phase=0)
    crust(sheet, sheet.faces("torso")["top"])
    for part in ("plate", "flank"):
        obsidian(sheet, part)
    # Each back plate's top: a raised boss down its middle, lit.
    for name, rect in sheet.faces("plate").items():
        if name == "top":
            x, y, w, h = rect
            sheet.fill((x + w // 2 - 2 * S, y + S, 4 * S, h - 2 * S), OBSIDIAN[4])
            sheet.fill((x + w // 2 - 2 * S, y + S, 4 * S, 2), OBSIDIAN[5])
    for part in ("spike_tall", "spike", "spike_low"):
        obsidian(sheet, part, tip=True)
    for part in ("seam", "spine_seam", "flank_seam"):
        magma(sheet, part)
    for part in ("core", "spine_core"):
        magma(sheet, part, pal=HOT)


def paint_head(sheet):
    hide(sheet, "neck", phase=1)
    hide(sheet, "skull", phase=0, belly=False)
    f = sheet.faces("skull")
    # His snout's front: a broad lip line, two nostrils, and his upper teeth along its bottom edge.
    front = f["front"]
    x, y, w, h = front
    sheet.fill((x, y + h - 2 * S, w, 2 * S), BASALT[3])
    sheet.fill((x, y + h - 2 * S, w, 1), BASALT[1])
    for cx in (8, 14):
        sheet.fill(cell(front, cx, 2, 2, 1), BASALT[0])
    for c in range(1, w // S, 3):
        sheet.fill(cell(front, c, (h // S) - 1, 1, 1), BONE[2])
    # The underside of his snout: the roof of his mouth, glowing deep inside.
    x, y, w, h = f["bottom"]
    sheet.fill(f["bottom"], MOUTH[1])
    sheet.fill((x + 2 * S, y + 2 * S, w - 4 * S, h - 4 * S), MOUTH[2])
    sheet.fill((x + 4 * S, y + 4 * S, w - 8 * S, h - 8 * S), MAGMA[0])
    sheet.fill((x + 4 * S, y + 4 * S, w - 8 * S, h - 8 * S), MAGMA[1], glow=True)
    obsidian(sheet, "brow", sheen=False)
    obsidian(sheet, "horn", tip=True)
    # His eyes: burning amber, a dark slit across each, on the glowmask.
    for name, rect in sheet.faces("eye").items():
        x, y, w, h = rect
        sheet.fill(rect, EYE[1])
        sheet.fill((x, y, w, max(1, h // 3)), EYE[2])
        sheet.fill(rect, EYE[1], glow=True)
        sheet.fill((x, y, w, max(1, h // 3)), EYE[2], glow=True)
        if name in ("front", "top", "right", "left"):
            sheet.fill((x + w // 2 - 1, y, 2, h), SLIT)
            sheet.fill((x + w // 2 - 1, y, 2, h), CLEAR, glow=True)
        sheet.fill((x + 1, y + 1, 2, 2), EYE[3])
        sheet.fill((x + 1, y + 1, 2, 2), EYE[3], glow=True)
    for name, rect in sheet.faces("fang").items():
        x, y, w, h = rect
        sheet.fill(rect, BONE[1])
        sheet.fill((x, y, max(1, w // 2), h), BONE[2])
        sheet.fill((x, y + h - 1, w, 1), BONE[0])
    # The jaw: hide outside; inside (its top), his mouth glowing deep, his lower teeth along its front edge.
    for name, rect in sheet.faces("jaw").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, MOUTH[1])
            sheet.fill((x + 3 * S, y + 2 * S, w - 6 * S, h - 5 * S), MOUTH[2])
            sheet.fill((x + 5 * S, y + 3 * S, w - 10 * S, h - 8 * S), MAGMA[1])
            sheet.fill((x + 5 * S, y + 3 * S, w - 10 * S, h - 8 * S), MAGMA[2], glow=True)
            sheet.fill((x, y + h - S, w, S), BONE[2])
            for c in range(1, w // S, 3):
                sheet.fill((x + c * S, y + h - S, 1, S), BONE[0])
        elif name == "bottom":
            sheet.fill(rect, EMBER[1])
            for r in range(0, h, 2 * S):
                sheet.fill((x, y + r, w, S // 2), EMBER[2])
        else:
            scales(sheet, rect, phase=1)
            sheet.fill((x, y, w, S), BASALT[3])
    # His throat, swelling with fire as he gathers it.
    magma(sheet, "throat")


def paint_legs(sheet):
    for part in ("upper_leg", "thigh", "shin", "shank"):
        hide(sheet, part, phase=1, belly=False)
    for part in ("fore_foot", "hind_foot"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "top":
                scales(sheet, rect, base=3)
                # His claws: obsidian, along the top's front rows.
                for c in range(0, w // S, 3):
                    sheet.fill((x + c * S, y + h - 3 * S, 2 * S, 3 * S), OBSIDIAN[3])
                    sheet.fill((x + c * S, y + h - 3 * S, 2 * S, 1), OBSIDIAN[4])
            elif name == "bottom":
                sheet.fill(rect, BASALT[1])
            elif name == "front":
                sheet.fill(rect, BASALT[2])
                for c in range(0, w // S, 3):
                    sheet.fill((x + c * S, y, 2 * S, h), OBSIDIAN[2])
                    sheet.fill((x + c * S, y, 2 * S, 1), OBSIDIAN[4])
            else:
                scales(sheet, rect)


def paint_tail(sheet):
    for part in ("tail_base", "tail_mid", "tail_end"):
        hide(sheet, part, phase=0)
    for part in ("tail_plate", "tail_plate_mid"):
        obsidian(sheet, part)
    for part in ("tail_spike", "tail_spike_low"):
        obsidian(sheet, part, tip=True)
    for part in ("club", "club_knob", "club_knob_top"):
        columns(sheet, part)
    for part in ("tail_seam", "tail_seam_side"):
        magma(sheet, part)


def cinder_tyrant():
    sheet = Sheet(tm.cinder_tyrant_model())
    paint_body(sheet)
    paint_head(sheet)
    paint_legs(sheet)
    paint_tail(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- his Cinderlings

def slag(sheet, part, cracks=5):
    """Slag: a black crust, lit along its top edge, split by thin cracks of fire (on the glowmask) running on the slant
    across the grid, one every `cracks` model pixels, with a short branch off each now and then; a glowing pit where a
    branch leaves its crack."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, CRUST[2] if name != "bottom" else CRUST[1])
        sheet.fill((x, y, w, 1), CRUST[3])
        cols, rows = max(1, w // S), max(1, h // S)
        for cy in range(rows):
            for cx in range(cols):
                crack = (cx + 2 * cy) % cracks == 0
                branch = cy % 3 == 1 and (2 * cx - cy) % (2 * cracks + 1) == 0
                if crack or branch:
                    tone = MAGMA[3] if crack and branch else MAGMA[2] if crack else MAGMA[1]
                    sheet.fill(cell(rect, cx, cy), tone)
                    sheet.fill(cell(rect, cx, cy), tone, glow=True)


def cinderling():
    sheet = Sheet(tm.cinderling_model())
    for part, n in (("ling_body", 4), ("ling_ridge", 3), ("ling_skull", 4), ("ling_tail", 4), ("ling_leg", 3)):
        slag(sheet, part, n)
    for name, rect in sheet.faces("ling_eye").items():
        sheet.fill(rect, EYE[3])
        sheet.fill(rect, EYE[3], glow=True)
    return sheet


# ------------------------------------------------------------------------------------------------- his things

def gob():
    sheet = Sheet(tm.gob_model())
    for part in ("gob_core", "gob_lump", "gob_drip"):
        magma(sheet, part)
    # The crust cooling on one side: dark, cracked with fire.
    slag(sheet, "gob_crust", 3)
    return sheet


def cinder():
    sheet = Sheet(tm.cinder_model())
    for part in ("cinder_rock", "cinder_shard", "cinder_shard_low"):
        slag(sheet, part, 4)
    # The smoke trailing above it: pale grey wisps, darker near the cinder, thinning upwards: more and more of each face
    # cut away (the sheets are cut-outs) in a scatter on the grid, so it frays into the air.
    for name, rect in sheet.faces("trail").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            for py in range(y, y + h):
                for px_ in range(x, x + w):
                    sheet.clear(px_, py)
            continue
        cols, rows = max(1, w // S), max(1, h // S)
        for cy in range(rows):
            up = 1.0 - cy / max(1, rows - 1)                 # 1 at the top, 0 at the cinder
            for cx in range(cols):
                hashed = ((cx * 7 + cy * 13 + len(name)) * 37) % 100 / 100.0
                if hashed < 0.15 + 0.6 * up:
                    for py in range(y + cy * S, y + (cy + 1) * S):
                        for px_ in range(x + cx * S, x + (cx + 1) * S):
                            sheet.clear(px_, py)
                    continue
                tone = SMOKE[3] if up > 0.6 else SMOKE[2] if up > 0.25 else SMOKE[1]
                sheet.fill(cell(rect, cx, cy), tone)
    return sheet


# ------------------------------------------------------------------------------------------------- the worn crest

def crest(part):
    """The worn crest's textures (16x16): its spikes, obsidian lit in vertical facets, paling to their tips; and the
    magma between them, bright along its middle."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "obsidian":
                tone = OBSIDIAN[5] if y < 2 else OBSIDIAN[4] if x % 4 == 0 else OBSIDIAN[3] if x % 4 == 1 else OBSIDIAN[2]
                if y > 13:
                    tone = OBSIDIAN[1]
            else:
                edge = min(y, 15 - y)
                tone = MAGMA[min(4, 1 + edge // 2)]
            px[x, y] = tone + (255,)
    return img


def cinder_tyrant_textures():
    """Every texture of the Cinder Tyrant's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {("item", f"tyrant_crest_{part}"): crest(part) for part in ("obsidian", "magma")}
    sheets = {"cinder_tyrant": cinder_tyrant(), "cinderling": cinderling(), "magma_gob": gob(), "falling_cinder": cinder()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in tyrant_numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in tyrant_numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
