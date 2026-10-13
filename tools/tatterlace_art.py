"""Original textures for Madame Tatterlace and her fight (docs/features/tatterlace.md) (requires Pillow): the sheets of
her GeckoLib bodies (tools/tatterlace_models.py) and their glowmasks, painted box by box and face by face at
TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- Tatterlace: black chitin lit violet-grey, fringed and banded in red; red chevrons down her abdomen and three gold
  thimbles on its back; eight red eyes and a purple gem at her brow; a headdress of red velvet on a gold band, its
  spires tipped in gold; gold cuffs at her knees (and red ones that glow in Frenzied Stitching); a lace doily in her
  palps, a steel needle on her right foreleg, and her silk dragline. Her glowmask holds her eyes, her gem and the red
  cuffs.
- Her things: the tossed thimble (dimpled gold), the rolling spool (red thread on pale wood, its flanges cut round),
  her egg sacs (silk-white, bound in silk) and her spiderlings (black, red-eyed).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other
texture, is read, traced or recoloured.
"""
import math

import tatterlace as tatterlace_numbers
import tatterlace_models as tm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, bevel, cell, ragged

S = tm.TEXTURE_SCALE

# Palettes, darkest first.
CHITIN = [rgb("0b0a0e"), rgb("16141a"), rgb("221f28"), rgb("302c38"), rgb("433d4e")]
RED = [rgb("4a0a12"), rgb("7a121e"), rgb("a41c28"), rgb("cc3636")]
GOLD = [rgb("6a440e"), rgb("9c6c1a"), rgb("c8962a"), rgb("e6bc48"), rgb("f8e088")]
VELVET = [rgb("3e0a12"), rgb("62101c"), rgb("861a28"), rgb("a82838")]
GEM = [rgb("3c1260"), rgb("6c2aa4"), rgb("a456e0"), rgb("dcaaff")]
EYE = [rgb("5a0606"), rgb("b01010"), rgb("ff3a2a"), rgb("ffb4a4")]
FANG = [rgb("6e6458"), rgb("a89c86"), rgb("d8ceb8")]
LACE = [rgb("c7ba98"), rgb("e0d5b8"), rgb("f0e8d4"), rgb("fbf6ea")]
STEEL = [rgb("5c6672"), rgb("7e8894"), rgb("a0a9b4"), rgb("c4ccd6"), rgb("e4e9ee")]
SILK = [rgb("a8a49c"), rgb("cfcbc3"), rgb("e8e5de"), rgb("f8f6f2")]
FRENZY = [rgb("8a0c0c"), rgb("e01e1e"), rgb("ff6a50")]
WOOD = [rgb("94744a"), rgb("ad8b5a"), rgb("c4a26c"), rgb("d6b882")]
AXLE = [rgb("4a3420"), rgb("5e442a"), rgb("745636")]
THREAD = [rgb("5a1418"), rgb("7e1f26"), rgb("a03036"), rgb("c04c4e")]


def chitin(sheet, part, top=2, side=1):
    """A part in plain chitin: its top lit a tone up and along its upper edge, the sides a tone down, the underside
    darkest."""
    for name, rect in sheet.faces(part).items():
        if name == "top":
            bevel(sheet, rect, CHITIN, base=top, light=top + 1, dark=top - 1, top=S // 2, side=S // 2, bottom=S // 2)
        elif name == "bottom":
            sheet.fill(rect, CHITIN[0])
        else:
            x, y, w, h = rect
            sheet.fill(rect, CHITIN[side])
            sheet.fill((x, y, w, min(h, S // 2)), CHITIN[side + 1])
            sheet.fill((x, y + h - min(h, S // 2), w, min(h, S // 2)), CHITIN[max(0, side - 1)])


def gold(sheet, part, base=2):
    """Gold: lit along the top, shadowed along the bottom, its top face brightest."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, GOLD[base + 1])
            sheet.fill((x, y, w, 2), GOLD[4])
        elif name == "bottom":
            sheet.fill(rect, GOLD[0])
        else:
            sheet.fill(rect, GOLD[base])
            sheet.fill((x, y, w, 2), GOLD[4])
            sheet.fill((x, y + 2, w, 2), GOLD[base + 1])
            sheet.fill((x, y + h - 2, w, 2), GOLD[0])


def dimples(sheet, rect, step=6):
    """A thimble's dimples on a staggered lattice, one every `step` pixels: each a shaded pit with its lower right
    catching the light."""
    x, y, w, h = rect
    for row, top in enumerate(range(y + 2, y + h - 2, step)):
        for left in range(x + 2 + (step // 2 if row % 2 else 0), x + w - 2, step):
            sheet.put(left, top, GOLD[1])
            sheet.put(left + 1, top + 1, GOLD[4])


# ------------------------------------------------------------------------------------------------- Tatterlace

def paint_body(sheet):
    # The thorax: a groove down its back and a red fringe along both edges.
    chitin(sheet, "thorax")
    f = sheet.faces("thorax")
    x, y, w, h = f["top"]
    sheet.fill((x + w // 2 - 2, y, 4, h), CHITIN[1])
    sheet.fill((x, y, S, h), RED[1])
    sheet.fill((x + w - S, y, S, h), RED[1])
    for name in ("right", "left", "front", "back"):
        x, y, w, h = f[name]
        sheet.fill((x, y + h - S, w, S), RED[1])
        sheet.fill((x, y + h - S, w, 1), RED[2])
    # The abdomen: three red chevrons down its back pointing forward (the top's last row is its front), red bands
    # low on its sides where the fringe hangs, and a red ring round the spinnerets.
    chitin(sheet, "abdomen")
    f = sheet.faces("abdomen")
    x, y, w, h = f["top"]
    sheet.fill((x, y, S, h), RED[1])
    sheet.fill((x + w - S, y, S, h), RED[1])
    centre = w // S // 2
    for apex in (8, 13, 18):
        for i in range(4):
            for cx in (centre - 1 - i, centre + i):
                sheet.fill(cell(f["top"], cx, apex - i), RED[2])
                sheet.fill((x + cx * S, y + (apex - i) * S, S, 1), RED[3])
    for name in ("right", "left"):
        x, y, w, h = f[name]
        sheet.fill((x, y + h - 4 * S, w, 2 * S), RED[1])
        sheet.fill((x, y + h - 4 * S, w, 1), RED[2])
    x, y, w, h = f["back"]
    cx, cy = x + w / 2, y + h / 2
    for py in range(y, y + h):
        for px in range(x, x + w):
            r = math.hypot(px + 0.5 - cx, py + 0.5 - cy) / S
            if 4.0 <= r < 5.0:
                sheet.put(px, py, RED[1] if r < 4.5 else RED[0])
    x, y, w, h = f["bottom"]
    sheet.fill((x + w // 4, y + h // 4, w // 2, h // 2), CHITIN[1])
    # The fringe: red hairs hanging in strands, cut into points along the bottom.
    for name, rect in sheet.faces("fringe").items():
        x, y, w, h = rect
        sheet.fill(rect, RED[1])
        for i in range(0, w, 2 * S):
            sheet.fill((x + i, y, S // 2, h), RED[2])
            sheet.fill((x + i + S, y, S // 2, h), RED[0])
        sheet.fill((x, y, w, S // 2), RED[0])
        ragged(sheet, rect, every=2 * S, depth=S + 2)
    # The spinnerets: three red-tipped nubs.
    chitin(sheet, "spinnerets", top=2, side=1)
    x, y, w, h = sheet.faces("spinnerets")["back"]
    for i in range(3):
        sheet.fill((x + 1 + i * (w // 3), y + h // 3, w // 3 - 2, h // 2), RED[1])
    # Her thimbles: dimpled gold.
    gold(sheet, "thimble")
    for name, rect in sheet.faces("thimble").items():
        if name != "bottom":
            dimples(sheet, rect)


def paint_head(sheet):
    chitin(sheet, "face", top=2, side=2)
    front = sheet.faces("face")["front"]
    x, y, w, h = front
    sheet.fill(front, CHITIN[2])
    sheet.fill((x, y, w, S // 2), CHITIN[3])
    sheet.fill(cell(front, 0, 6, 10, 2), CHITIN[1])   # the chelicerae's base
    sheet.fill(cell(front, 0, 7, 10, 1), CHITIN[0])
    # Eight red eyes: four small over two big, with a small one at each outer corner below.
    eyes = [(1, 2, 1), (3, 2, 1), (6, 2, 1), (8, 2, 1), (2, 3, 2), (6, 3, 2), (1, 5, 1), (8, 5, 1)]
    for cx, cy, size in eyes:
        rect = cell(front, cx, cy, size, size)
        ex, ey, ew, eh = rect
        sheet.fill(rect, EYE[1])
        sheet.fill(rect, EYE[2] if size == 2 else EYE[1], glow=True)
        sheet.fill((ex, ey + eh - 1, ew, 1), EYE[0])
        sheet.fill((ex + 1, ey + 1, max(1, ew // 3), max(1, eh // 3)), EYE[3])
        sheet.fill((ex + 1, ey + 1, max(1, ew // 3), max(1, eh // 3)), EYE[3], glow=True)
    # The gem at her brow.
    for name, rect in sheet.faces("gem").items():
        x, y, w, h = rect
        tone = GEM[2] if name in ("front", "top") else GEM[1]
        sheet.fill(rect, tone)
        sheet.fill(rect, tone, glow=True)
        if name == "front":
            sheet.fill((x, y + h - 2, w, 2), GEM[0])
            sheet.fill((x, y + h - 2, w, 2), GEM[0], glow=True)
            sheet.fill((x + 1, y + 1, 2, 2), GEM[3])
            sheet.fill((x + 1, y + 1, 2, 2), GEM[3], glow=True)
    # The fangs: glossy black, pale at the tips.
    for name, rect in sheet.faces("fang").items():
        x, y, w, h = rect
        if name == "bottom":
            sheet.fill(rect, FANG[1])
            continue
        sheet.fill(rect, CHITIN[1])
        sheet.fill((x, y, 1, h), CHITIN[3])
        if name != "top":
            sheet.fill((x, y + h - S, w, S), FANG[1])
            sheet.fill((x, y + h - S, w, 1), FANG[2])
    # The palps, each with a gold ring.
    chitin(sheet, "palp")
    for name, rect in sheet.faces("palp").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill((x, y + 2 * S, w, S), GOLD[2 if name == "top" else 1])
        elif name in ("right", "left"):
            sheet.fill((x + 2 * S, y, S, h), GOLD[2])


def paint_headdress(sheet):
    # The band: gold, set with three red studs across its front.
    gold(sheet, "band")
    front = sheet.faces("band")["front"]
    for cx in (2, 5, 8):
        x, y, w, h = cell(front, cx, 0, 1, 2)
        sheet.fill((x, y + 2, w, h - 4), RED[2])
        sheet.fill((x + 1, y + 3, 1, 1), RED[3])
    # The crown: red velvet, piped in gold at its edges and down its middle.
    for name, rect in sheet.faces("crown").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, VELVET[3])
            sheet.fill((x, y, w, 2), GOLD[3])
            sheet.fill((x, y + h - 2, w, 2), GOLD[3])
            continue
        if name == "bottom":
            sheet.fill(rect, VELVET[0])
            continue
        sheet.fill(rect, VELVET[2] if name == "front" else VELVET[1])
        sheet.fill((x, y, w, S // 2), VELVET[3])
        sheet.fill((x, y + h - S // 2, w, S // 2), VELVET[0])
        for px in (x, x + w - 2):
            sheet.fill((px, y, 2, h), GOLD[3])
        if name in ("front", "back"):
            sheet.fill((x + w // 2 - 1, y, 2, h), GOLD[2])
    # The spires: velvet, tipped in gold.
    for part in ("spire", "spire_side"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "top":
                sheet.fill(rect, GOLD[4])
                continue
            if name == "bottom":
                sheet.fill(rect, VELVET[0])
                continue
            sheet.fill(rect, VELVET[2] if name in ("front", "right") else VELVET[1])
            sheet.fill((x, y, w, 2 * S), GOLD[3])
            sheet.fill((x, y, w, 2), GOLD[4])
            sheet.fill((x, y + 2 * S - 1, w, 1), GOLD[1])


def lace_disc(sheet, rect):
    """A round doily of lace on a square face, cut out: a scalloped edge, a band of close stitches, open mesh, a solid
    ring and a six-petalled rosette at the middle (symmetric, so both faces of the plane match)."""
    x, y, w, h = rect
    cx, cy = x + w / 2, y + h / 2
    radius = w / 2 / S
    for py in range(y, y + h):
        for px in range(x, x + w):
            dx, dy = (px + 0.5 - cx) / S, (py + 0.5 - cy) / S
            r = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            edge = radius - 0.3 + 0.3 * math.cos(16 * angle)
            if r > edge:
                tone = None
            elif r > edge - 0.55:
                tone = LACE[2] if dx + dy < 0 else LACE[1]
            elif r > radius - 1.9:
                tone = None if (px + py) % 6 < 2 and (px - py) % 6 < 2 else LACE[1]   # close stitches, pinholed
            elif r > radius - 3.0:
                tone = LACE[2] if ((px + py) % 8 < 2 or (px - py) % 8 < 2) else None   # open mesh
            elif r > radius - 3.6:
                tone = LACE[3] if dx + dy < 0 else LACE[2]
            elif r > 0.9:
                tone = LACE[1] if math.cos(6 * angle) > -0.1 else None                 # the rosette's petals
            else:
                tone = LACE[3]
            if tone is None:
                sheet.clear(px, py)
            else:
                sheet.put(px, py, tone)


def paint_doily(sheet):
    for name, rect in sheet.faces("doily").items():
        lace_disc(sheet, rect)


def paint_legs(sheet):
    # The femur: lit along its top, red bands at both ends.
    chitin(sheet, "femur", top=3, side=1)
    for name, rect in sheet.faces("femur").items():
        x, y, w, h = rect
        if name in ("top", "bottom", "front", "back"):
            sheet.fill((x, y, S, h), RED[1])
            sheet.fill((x + w - S, y, S, h), RED[1])
    # The tibia: long and black, ringed in red near each end.
    chitin(sheet, "tibia", top=2, side=1)
    for name, rect in sheet.faces("tibia").items():
        x, y, w, h = rect
        if name in ("top", "bottom", "front", "back"):
            for at in (S, w - 2 * S):
                sheet.fill((x + at, y, S, h), RED[1])
                sheet.fill((x + at, y, 1, h), RED[2])
    # The gold cuffs at her knees (the leg runs through them along x): a rolled rim at each end and a raised bead round
    # the middle; their ends gold round the leg.
    for name, rect in sheet.faces("cuff").items():
        x, y, w, h = rect
        if name in ("right", "left"):
            sheet.fill(rect, GOLD[1])
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), CHITIN[1])
            continue
        lit = name == "top"
        sheet.fill(rect, GOLD[3] if lit else GOLD[2])
        sheet.fill((x, y, S // 2, h), GOLD[1])
        sheet.fill((x + w - S // 2, y, S // 2, h), GOLD[1])
        sheet.fill((x + w // 2 - 1, y, 2, h), GOLD[4] if lit else GOLD[3])
        if name in ("front", "back"):
            sheet.fill((x, y, w, 1), GOLD[4])
            sheet.fill((x, y + h - 1, w, 1), GOLD[0])
    # The red cuffs of Frenzied Stitching, glowing.
    for name, rect in sheet.faces("cuff_red").items():
        x, y, w, h = rect
        sheet.fill(rect, FRENZY[1])
        sheet.fill(rect, FRENZY[1], glow=True)
        sheet.fill((x, y, w, 2), FRENZY[2])
        sheet.fill((x, y, w, 2), FRENZY[2], glow=True)
        sheet.fill((x, y + h - 2, w, 2), FRENZY[0])
        sheet.fill((x, y + h - 2, w, 2), FRENZY[0], glow=True)
    # The needle: polished steel, lit along its top; its eye a dark slot.
    for name, rect in sheet.faces("needle").items():
        x, y, w, h = rect
        sheet.fill(rect, STEEL[{"top": 4, "bottom": 1, "front": 3, "back": 2}.get(name, 2)])
        if name in ("front", "back"):
            sheet.fill((x, y, w, 1), STEEL[4])
    for name, rect in sheet.faces("needle_eye").items():
        x, y, w, h = rect
        sheet.fill(rect, STEEL[3])
        sheet.fill((x, y, w, 1), STEEL[4])
        if name in ("front", "back", "top", "bottom"):
            sheet.fill((x + w // 2 - 1, y + 2, 2, h - 4), STEEL[0])


def paint_dragline(sheet):
    for name, rect in sheet.faces("dragline").items():
        x, y, w, h = rect
        sheet.fill(rect, SILK[2])
        if w >= 3:
            sheet.fill((x, y, 1, h), SILK[3])
            sheet.fill((x + w - 1, y, 1, h), SILK[1])


def tatterlace():
    sheet = Sheet(tm.tatterlace_model())
    paint_body(sheet)
    paint_head(sheet)
    paint_headdress(sheet)
    paint_doily(sheet)
    paint_legs(sheet)
    paint_dragline(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- her things

def tossed_thimble():
    sheet = Sheet(tm.thimble_model())
    for part in ("thimble_side", "thimble_top"):
        gold(sheet, part)
        for name, rect in sheet.faces(part).items():
            if name != "bottom":
                dimples(sheet, rect)
    gold(sheet, "thimble_rim", base=1)
    return sheet


def rolling_spool():
    """Red thread wound round the axle (each turn a line across the faces that run along it), between two pale wooden
    flanges cut round, on a dark wooden axle."""
    sheet = Sheet(tm.spool_model())
    for name, rect in sheet.faces("spool_thread").items():
        x, y, w, h = rect
        if name in ("right", "left"):
            sheet.fill(rect, THREAD[1])
            continue
        for i in range(w):
            tone = THREAD[0] if i % (2 * S) == 0 else THREAD[2] if (i // 2) % 2 else THREAD[1]
            sheet.fill((x + i, y, 1, h), tone)
        if name == "top":
            sheet.fill((x, y, w, 2), THREAD[3])
    for name, rect in sheet.faces("spool_flange").items():
        x, y, w, h = rect
        if name in ("right", "left"):
            cx, cy = x + w / 2, y + h / 2
            for py in range(y, y + h):
                for px in range(x, x + w):
                    r = math.hypot(px + 0.5 - cx, py + 0.5 - cy) / S
                    edge = w / 2 / S
                    if r > edge:
                        sheet.clear(px, py)
                    elif r > edge - 1.0:
                        sheet.put(px, py, WOOD[0])
                    elif r < 2.0:
                        sheet.put(px, py, AXLE[0])
                    elif r < 3.0 or edge - 4.0 < r < edge - 3.0:
                        sheet.put(px, py, WOOD[1])
                    else:
                        sheet.put(px, py, WOOD[2])
            continue
        # The rim of a round flange would stand at the box's square edge: it is left out, so the flange is a disc.
        sheet.fill(rect, CLEAR)
    for name, rect in sheet.faces("spool_axle").items():
        x, y, w, h = rect
        sheet.fill(rect, AXLE[1])
        sheet.fill((x, y, w, 2), AXLE[2])
    return sheet


def egg_sac():
    """Silk-white sacs, shaded blue-grey low down, with fine threads drawn round them; a silk band binds them."""
    sheet = Sheet(tm.egg_sac_model())
    for part in ("sac_big", "sac"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "bottom":
                sheet.fill(rect, SILK[0])
            else:
                sheet.fill(rect, SILK[2])
                if name != "top":
                    sheet.fill((x, y + h // 2, w, h - h // 2), SILK[1])
                    sheet.fill((x, y + h - S, w, S), SILK[0])
                sheet.fill((x, y, w, S // 2), SILK[3])
                # Strands of silk wound round it, on the slant.
                for i in range(0, w, 3 * S):
                    for j in range(h):
                        sheet.put(x + (i + j // 2) % w, y + j, SILK[3])
            # Rounded: each face's corners cut away, so the sacs look swollen rather than boxed.
            for cx, cy in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
                for i in range(S + 1):
                    for j in range(S + 1 - i):
                        sheet.clear(cx + (i if cx == x else -i), cy + (j if cy == y else -j))
    for name, rect in sheet.faces("sac_wrap").items():
        x, y, w, h = rect
        sheet.fill(rect, SILK[3])
        sheet.fill((x, y + h - 2, w, 2), SILK[1])
    return sheet


def spiderling():
    sheet = Sheet(tm.spiderling_model())
    chitin(sheet, "ling_body")
    chitin(sheet, "ling_head")
    chitin(sheet, "ling_leg", top=2, side=1)
    x, y, w, h = sheet.faces("ling_body")["top"]
    sheet.fill((x + w // 2 - S // 2, y + S, S, h - 2 * S), RED[2])
    front = sheet.faces("ling_head")["front"]
    for cx in (0, 2):
        rect = cell(front, cx, 0, 1, 1)
        sheet.fill(rect, EYE[2])
        sheet.fill(rect, EYE[2], glow=True)
    return sheet


# ------------------------------------------------------------------------------------------------- the headdress

def headdress(part):
    """The worn headdress's textures (16x16): red velvet in soft vertical folds, lit at the top; gold, lit along its top
    and shadowed below, with a bead every fourth pixel; and the amethyst, lit at its top left."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "velvet":
                tone = VELVET[(1, 2, 2, 3, 2, 1, 1, 0)[x % 8]] if y > 1 else VELVET[3]
            elif part == "gold":
                tone = GOLD[4] if y < 2 else GOLD[0] if y > 13 else GOLD[3] if x % 4 == 1 and 5 <= y <= 8 else GOLD[2]
            else:
                tone = GEM[3] if x < 6 and y < 6 else GEM[0] if x > 11 or y > 11 else GEM[2]
            px[x, y] = tone + (255,)
    return img


def tatterlace_textures():
    """Every texture of Tatterlace's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {}
    for part in ("velvet", "gold", "gem"):
        out[("item", f"tatterlace_headdress_{part}")] = headdress(part)
    sheets = {"tatterlace": tatterlace(), "tossed_thimble": tossed_thimble(), "rolling_spool": rolling_spool(),
              "tatter_egg_sac": egg_sac(), "tatter_spiderling": spiderling()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in tatterlace_numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in ("gossamer_silk", "golden_thimble", "tatterlace_headdress"):
        out[("item", item)] = item_icons.draw(item)
    return out
