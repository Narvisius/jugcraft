"""Original textures for Vesperine, the Last Reaper, and her fight (docs/features/vesperine.md) (requires Pillow): the
sheets of her GeckoLib bodies (tools/vesperine_models.py) and their glowmasks, painted box by box and face by face at
TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in the clean style: two or
three flat tones a material, hard edges, no noise, eyes on the model's pixel grid, skulls with square sockets.

- Vesperine: pale skin, red eyes in a band of shadow, black hair with a violet sheen, layered black armour edged in
  silver with a crescent moon on the breastplate, a torn black robe over a crimson lining, a black pole bound in silver
  and a moon-pale blade; her glowmask holds her eyes, her halo and the blade's edge.
- Dirge and Requiem: great dark skulls with square sockets; Dirge's glow is soul blue, Requiem's red; black wisps.
- Her thrown scythe (the same scythe), and the grave thralls: old bone, ribs cut apart, soul-flame eyes.
- Items: Reaper's Shade, the Reaper's Hood (and the cloth its worn model wears), and the Dirge and Requiem skull
  trophies' block faces.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other
texture, is read, traced or recoloured.
"""
from PIL import Image

import vesperine_models as vm
from crop_textures import rgb

S = vm.TEXTURE_SCALE

# Palettes, darkest first.
ARMOUR = [rgb("0d0c12"), rgb("19171f"), rgb("26232e"), rgb("37333f"), rgb("4b4656")]
SILVER = [rgb("5e5a6c"), rgb("8e8aa0"), rgb("bdb9cc"), rgb("e4e1ec")]
ROBE = [rgb("0c0910"), rgb("150f1a"), rgb("1f1725"), rgb("2b2032")]
CRIMSON = [rgb("3a0c18"), rgb("5a1426"), rgb("7a1d34")]
SKIN = [rgb("8a849a"), rgb("aaa4b8"), rgb("cbc6d6"), rgb("e2deea")]
HAIR = [rgb("07060a"), rgb("0f0d14"), rgb("19161f"), rgb("27222f"), rgb("3c3448")]
EYE = [rgb("6e0808"), rgb("c01414"), rgb("ff4230")]
LIPS = rgb("4a2234")
BLADE = [rgb("6a667c"), rgb("9894ab"), rgb("c4c1d2"), rgb("e3e1ec"), rgb("f8f7ff")]
POLE = [rgb("0f0c12"), rgb("1a1520"), rgb("261f2d")]
HALO = [rgb("6f64a8"), rgb("9d92d6"), rgb("c4bcf0")]
GLOW_EDGE = rgb("e9e6ff")
CLEAR = (0, 0, 0, 0)


def faces(u, v, w, h, d):
    """A box's faces in its unwrapped (box-UV) region, as (x, y, width, height) in model pixels. On top and bottom the
    last row is the front edge; on the right-hand side the last column is the front, on the left-hand side the first."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Sheet:
    """A model's sheet, painted at S times its size, and its glowmask."""

    def __init__(self, model):
        self.size = model.texture * S
        self.img = Image.new("RGBA", (self.size, self.size), CLEAR)
        self.glow = Image.new("RGBA", (self.size, self.size), CLEAR)
        self.px = self.img.load()
        self.gx = self.glow.load()
        self.regions = model.regions()

    def faces(self, part):
        """Each face of a part's box as (x, y, w, h) in painted pixels; faces with no area are left out."""
        u, v, w, h, d = self.regions[part]
        return {name: (x * S, y * S, fw * S, fh * S) for name, (x, y, fw, fh) in faces(u, v, w, h, d).items()
                if fw > 0 and fh > 0}

    def fill(self, rect, colour, glow=False):
        x0, y0, w, h = rect
        target = self.gx if glow else self.px
        c = tuple(colour) + ((255,) if len(colour) == 3 else ())
        for y in range(max(0, y0), min(self.size, y0 + h)):
            for x in range(max(0, x0), min(self.size, x0 + w)):
                target[x, y] = c

    def put(self, x, y, colour, glow=False):
        if 0 <= x < self.size and 0 <= y < self.size:
            (self.gx if glow else self.px)[x, y] = tuple(colour) + ((255,) if len(colour) == 3 else ())

    def clear(self, x, y):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[x, y] = CLEAR
            self.gx[x, y] = CLEAR

    def get(self, x, y):
        return self.px[x, y]


# ------------------------------------------------------------------------------------------------- painting helpers

def bevel(sheet, rect, pal, base=1, light=2, dark=0, top=2, side=1, bottom=2):
    """A flat face in pal[base], lit along its top (and left) edge and shadowed along its bottom (and right)."""
    x, y, w, h = rect
    sheet.fill(rect, pal[base])
    sheet.fill((x, y, w, top), pal[light])
    sheet.fill((x, y, side, h), pal[light])
    sheet.fill((x, y + h - bottom, w, bottom), pal[dark])
    sheet.fill((x + w - side, y + top, side, h - top), pal[dark])


def lames(sheet, rect, pal, band=2 * S, trim=None):
    """Armour in overlapping horizontal plates `band` pixels deep: each lit along its top and shadowed along its lower
    lip; with `trim`, the lowest plate's lip is silver."""
    x, y, w, h = rect
    sheet.fill(rect, pal[1])
    for top in range(y, y + h, band):
        sheet.fill((x, top, w, 1), pal[3])
        sheet.fill((x, top + 1, w, 1), pal[2])
        lip = min(y + h, top + band) - 2
        sheet.fill((x, lip, w, 2), pal[0])
    if trim:
        sheet.fill((x, y + h - 2, w, 2), trim[1])
        sheet.fill((x, y + h - 3, w, 1), trim[2])


def trim(sheet, rect, pal=SILVER, width=2, edges="tlbr"):
    """A silver border along the face's edges."""
    x, y, w, h = rect
    if "t" in edges:
        sheet.fill((x, y, w, width), pal[2])
        sheet.fill((x, y + width - 1, w, 1), pal[1])
    if "b" in edges:
        sheet.fill((x, y + h - width, w, width), pal[1])
        sheet.fill((x, y + h - 1, w, 1), pal[0])
    if "l" in edges:
        sheet.fill((x, y, width, h), pal[2])
    if "r" in edges:
        sheet.fill((x + w - width, y, width, h), pal[1])


def rivet(sheet, x, y, pal=SILVER):
    sheet.fill((x, y, 2, 2), pal[2])
    sheet.put(x + 1, y + 1, pal[0])
    sheet.put(x, y, pal[3])


def folds(sheet, rect, pal, period=3 * S, phase=0, shade_bottom=True):
    """Cloth hanging in vertical folds: each fold a lit ridge and a shadowed valley, in flat bands; darker towards the
    hem."""
    x, y, w, h = rect
    sheet.fill(rect, pal[1])
    for i in range(w):
        p = (i + phase) % period
        if p < 2:
            sheet.fill((x + i, y, 1, h), pal[2])
        elif period // 2 <= p < period // 2 + 3:
            sheet.fill((x + i, y, 1, h), pal[0])
    if shade_bottom:
        sheet.fill((x, y + h - S, w, S), pal[0])


def strands(sheet, rect, pal=HAIR, period=5, sheen=None, phase=0):
    """Hair in long flat strands: alternating tones a few pixels wide, with a band of violet sheen across them."""
    x, y, w, h = rect
    tones = (pal[1], pal[2], pal[1], pal[3], pal[2], pal[0])
    for i in range(w):
        tone = tones[((i + phase) // period) % len(tones)]
        sheet.fill((x + i, y, 1, h), tone)
    if sheen is not None:
        top, depth = sheen
        for i in range(w):
            if ((i + phase) // period) % 2 == 0:
                sheet.fill((x + i, y + top, 1, depth), pal[4])


def ragged(sheet, rect, every=3 * S, depth=2 * S, offset=0, edge="bottom"):
    """Cuts the face's edge into even points (the sheets are cut-outs: cleared pixels are not drawn)."""
    x, y, w, h = rect
    if edge == "bottom":
        for i in range(w):
            p = ((i + offset) % every) / every
            cut = int(round(depth * (1 - abs(2 * p - 1))))
            for j in range(cut):
                sheet.clear(x + i, y + h - 1 - j)
    elif edge == "top":
        for i in range(w):
            p = ((i + offset) % every) / every
            cut = int(round(depth * (1 - abs(2 * p - 1))))
            for j in range(cut):
                sheet.clear(x + i, y + j)


def ragged_side(sheet, rect, every=3 * S, depth=2 * S, offset=0, side="right"):
    """Cuts a face's left or right edge into even points."""
    x, y, w, h = rect
    for j in range(h):
        p = ((j + offset) % every) / every
        cut = int(round(depth * (1 - abs(2 * p - 1))))
        for i in range(cut):
            sheet.clear(x + w - 1 - i if side == "right" else x + i, y + j)


def cell(rect, cx, cy, cw=1, ch=1):
    """A rect of whole model pixels inside a face: (cx, cy) counted from the face's top left."""
    x, y, _, _ = rect
    return x + cx * S, y + cy * S, cw * S, ch * S


# ------------------------------------------------------------------------------------------------- the scythe

def paint_scythe(sheet):
    """The pole (black, lit down one edge), silver butt, rings and finial, a black socket bound in silver, and the
    blade: a darker spine, a pale body and a bright cutting edge along its inner curve, which glows."""
    for name, rect in sheet.faces("pole").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, POLE[1])
            continue
        sheet.fill(rect, POLE[1])
        sheet.fill((x, y, 2, h), POLE[2])
        sheet.fill((x + w - 2, y, 2, h), POLE[0])
        # A spiral of fine silver wire every few pixels up the pole, as a binding.
        for top in range(y + 6 * S, y + h - 4 * S, 9 * S):
            sheet.fill((x, top, w, 1), SILVER[0])
    for part in ("butt", "ring", "finial"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, SILVER[2])
            sheet.fill((x, y, w, 1), SILVER[3])
            sheet.fill((x, y + h - 1, w, 1), SILVER[0])
            if name in ("left", "right", "front", "back") and w > 3:
                sheet.fill((x + w - 2, y, 2, h), SILVER[1])
    for name, rect in sheet.faces("socket").items():
        x, y, w, h = rect
        sheet.fill(rect, ARMOUR[2])
        if name in ("top", "bottom"):
            trim(sheet, rect, width=2)
            continue
        sheet.fill((x, y, w, 3), SILVER[2])
        sheet.fill((x, y + h - 3, w, 3), SILVER[1])
        sheet.fill((x, y + 3, 1, h - 6), ARMOUR[3])
        rivet(sheet, x + w // 2 - 1, y + h // 2 - 1)
    for i, (length, width, _) in enumerate(vm.BLADE):
        part = f"blade_{i}"
        tip = i == len(vm.BLADE) - 1
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "top":
                sheet.fill(rect, BLADE[1])
            elif name == "bottom":
                sheet.fill(rect, BLADE[4])
                sheet.fill(rect, GLOW_EDGE, glow=True)
            elif name in ("left", "right"):
                sheet.fill(rect, BLADE[1])
            else:
                sheet.fill(rect, BLADE[2])
                sheet.fill((x, y, w, 3), BLADE[1])          # the spine
                sheet.fill((x, y + 3, w, 1), BLADE[0])
                sheet.fill((x, y + h - 6, w, 3), BLADE[3])  # the bevel to the edge
                sheet.fill((x, y + h - 3, w, 3), BLADE[4])  # the edge
                sheet.fill((x, y + h - 3, w, 3), GLOW_EDGE, glow=True)
                if tip:
                    # The point: the tip's outer end is cut away below its spine (the outer end is on the left of
                    # the front face and the right of the back).
                    for j in range(h):
                        cut = int((w * 0.85) * j / h)
                        for k in range(cut):
                            sheet.clear(x + (w - 1 - k if name == "back" else k), y + h - 1 - (h - 1 - j))
            if tip and name == "bottom":
                sheet.fill(rect, CLEAR)


# ------------------------------------------------------------------------------------------------- Vesperine

def paint_face(sheet):
    f = sheet.faces("face")
    front = f["front"]
    x, y, w, h = front
    sheet.fill(front, SKIN[2])
    # The sides of her face and under her jaw a step darker.
    sheet.fill(cell(front, 1, 6, 1, 3), SKIN[1])
    sheet.fill(cell(front, 6, 6, 1, 3), SKIN[1])
    sheet.fill(cell(front, 0, 8, 8, 1), SKIN[1])
    sheet.fill((x + S, y + h - 2, w - 2 * S, 2), SKIN[0])
    # Hair: a fringe over the top two rows, falling past her temples to her cheeks.
    strands(sheet, cell(front, 0, 0, 8, 2), period=4, sheen=(3, 2))
    strands(sheet, cell(front, 0, 2, 2, 4), period=4)
    strands(sheet, cell(front, 6, 2, 2, 4), period=4, phase=2)
    strands(sheet, cell(front, 2, 2, 1, 1), period=4, phase=1)
    for i in range(S):  # the fringe ends in two points over her brow
        for j in range(S - abs(2 * i - S + 1) // 2 - 1):
            sheet.put(x + 3 * S + i, y + 2 * S + j, HAIR[2])
            sheet.put(x + 4 * S + i, y + 2 * S + j, HAIR[1])
    sheet.fill(cell(front, 0, 6, 1, 1), HAIR[1])
    sheet.fill(cell(front, 7, 6, 1, 1), HAIR[1])
    # The eyes in a band of shadow, each two pixels: the outer a deep red, the inner a bright one.
    sheet.fill((x + S, y + 4 * S - 2, 6 * S, S + 4), SKIN[0])
    sheet.fill((x + 3 * S, y + 4 * S - 2, 2 * S, S + 4), SKIN[1])
    for col, tone in ((1, 1), (2, 2), (5, 2), (6, 1)):
        sheet.fill(cell(front, col, 4), EYE[tone])
        sheet.fill(cell(front, col, 4), (EYE[2] if tone == 2 else rgb("e0281c")), glow=True)
        sheet.fill((x + col * S, y + 4 * S, S, 1), EYE[0])
    # Her brows: thin dark strokes above the shadow, rising to the outside.
    sheet.fill((x + S, y + 3 * S, 2 * S, 1), HAIR[1])
    sheet.fill((x + S, y + 3 * S + 1, S, 1), HAIR[1])
    sheet.fill((x + 5 * S, y + 3 * S, 2 * S, 1), HAIR[1])
    sheet.fill((x + 6 * S, y + 3 * S + 1, S, 1), HAIR[1])
    # Nose: one line of shade, and the shadow under it.
    sheet.fill((x + 4 * S, y + 5 * S, 1, S), SKIN[1])
    sheet.fill((x + 3 * S + 2, y + 6 * S - 1, S, 1), SKIN[1])
    # Lips: one dark line.
    sheet.fill((x + 3 * S + 1, y + 7 * S + 1, 2 * S - 2, 1), LIPS)
    for name in ("right", "left"):
        rect = f[name]
        rx, ry, rw, rh = rect
        strands(sheet, rect, period=4, sheen=(2 * S, 2))
        # Her cheek: the front column, below the fringe.
        col = rx + rw - S if name == "right" else rx
        sheet.fill((col, ry + 2 * S, S, rh - 2 * S), SKIN[1])
        sheet.fill((col, ry + rh - S, S, S), SKIN[0])
    strands(sheet, f["top"], period=4)
    strands(sheet, f["back"], period=4, sheen=(2 * S, 2))
    bx, by, bw, bh = f["bottom"]
    sheet.fill(f["bottom"], HAIR[1])
    sheet.fill((bx, by + bh // 2, bw, bh - bh // 2), SKIN[0])


def paint_hair(sheet):
    for part, sheen in (("hair_cap", 3), ("lock", 2 * S), ("hair_back", 2 * S), ("hair_tail", 3 * S)):
        for name, rect in sheet.faces(part).items():
            strands(sheet, rect, period=4, sheen=(sheen, 3) if name not in ("top", "bottom") else None,
                    phase={"front": 0, "back": 2, "left": 1, "right": 3}.get(name, 0))
    # The locks and the long hair end in points.
    for name, rect in sheet.faces("lock").items():
        if name not in ("top", "bottom"):
            ragged(sheet, rect, every=S, depth=S + 2)
    for name, rect in sheet.faces("hair_tail").items():
        if name not in ("top", "bottom"):
            ragged(sheet, rect, every=2 * S, depth=3 * S, offset=S if name in ("left", "right") else 0)
        if name == "bottom":
            sheet.fill(rect, CLEAR)


def paint_armour(sheet):
    # The cuirass: plates over her chest and sides.
    for name, rect in sheet.faces("chest").items():
        if name in ("top", "bottom"):
            bevel(sheet, rect, ARMOUR, base=2, light=3, dark=1)
        else:
            lames(sheet, rect, ARMOUR, band=2 * S, trim=None)
    # The breastplate: a raised plate edged in silver, a ridge down its middle, a crescent moon.
    for name, rect in sheet.faces("breastplate").items():
        x, y, w, h = rect
        if name != "front":
            sheet.fill(rect, SILVER[1])
            sheet.fill((x, y, w, 1), SILVER[2])
            continue
        bevel(sheet, rect, ARMOUR, base=2, light=3, dark=0, top=2, side=2, bottom=2)
        sheet.fill((x, y + h - 2, w, 2), SILVER[1])
        sheet.fill((x, y, w, 1), SILVER[1])
        # The crescent: a disc less a disc set off up and to the side, in pale silver, glowing faintly.
        cx, cy, r = x + w / 2, y + 2.4 * S, 1.6 * S
        for j in range(int(cy - r) - 1, int(cy + r) + 2):
            for i in range(int(cx - r) - 1, int(cx + r) + 2):
                inside = (i + 0.5 - cx) ** 2 + (j + 0.5 - cy) ** 2 <= r * r
                bite = (i + 0.5 - cx - 0.7 * r) ** 2 + (j + 0.5 - cy + 0.35 * r) ** 2 <= (0.85 * r) ** 2
                if inside and not bite:
                    sheet.put(i, j, SILVER[3])
                    sheet.put(i, j, HALO[1], glow=True)
    for name, rect in sheet.faces("gorget").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, ARMOUR[1])
            continue
        lames(sheet, rect, ARMOUR, band=S, trim=None)
        sheet.fill((x, y, w, 2), SILVER[2])
    for part in ("pauldron", "pauldron_low", "cuff", "gauntlet", "tasset"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "top":
                bevel(sheet, rect, ARMOUR, base=2, light=4, dark=1)
                if part == "pauldron":
                    sheet.fill((x + w // 2 - S, y + 2, 2 * S, h - 4), ARMOUR[3])
                continue
            if name == "bottom":
                sheet.fill(rect, ARMOUR[0])
                continue
            band = {"pauldron": 2 * S, "pauldron_low": 2 * S, "cuff": 2 * S, "gauntlet": 2 * S + 2, "tasset": 2 * S}[part]
            lames(sheet, rect, ARMOUR, band=band, trim=SILVER)
            if part in ("pauldron", "cuff") and w >= 4 * S:
                rivet(sheet, x + 3, y + 3)
                rivet(sheet, x + w - 5, y + 3)
            if part == "tasset":
                rivet(sheet, x + w // 2 - 1, y + 3)
    # The belt, with a silver buckle at its front.
    for name, rect in sheet.faces("belt").items():
        x, y, w, h = rect
        sheet.fill(rect, ROBE[0])
        if name in ("top", "bottom"):
            continue
        sheet.fill((x, y, w, 1), SILVER[1])
        sheet.fill((x, y + h - 1, w, 1), SILVER[0])
        if name == "front":
            bx = x + w // 2 - S
            sheet.fill((bx, y + 1, 2 * S, h - 2), SILVER[2])
            sheet.fill((bx + 2, y + 3, 2 * S - 4, h - 6), ROBE[0])
            sheet.fill((bx + S - 1, y + 3, 2, h - 6), SILVER[3])


def paint_robe(sheet):
    for name, rect in sheet.faces("waist").items():
        folds(sheet, rect, ROBE, period=2 * S, shade_bottom=False)
    for part in ("skirt_top", "skirt_low"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "top":
                sheet.fill(rect, ROBE[1])
                continue
            if name == "bottom":
                sheet.fill(rect, ROBE[0])
                continue
            folds(sheet, rect, ROBE, period=3 * S, phase=S if part == "skirt_low" else 0, shade_bottom=part == "skirt_low")
            if name == "front":
                # The robe parts at the front over its crimson underskirt, widening towards the hem.
                for j in range(h):
                    t = j / max(1, h - 1)
                    half = int((S + (S if part == "skirt_low" else 0) * t) if part == "skirt_low" else S * (0.6 + 0.4 * t))
                    cx = x + w // 2
                    sheet.fill((cx - half, y + j, 2 * half, 1), CRIMSON[1])
                    sheet.fill((cx - half, y + j, 1, 1), CRIMSON[0])
                    sheet.fill((cx + half - 1, y + j, 1, 1), CRIMSON[0])
                    sheet.fill((cx - 1, y + j, 1, 1), CRIMSON[2])
    for part in ("tatters", "tatters_side"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            folds(sheet, rect, ROBE, period=3 * S, phase=S, shade_bottom=False)
            sheet.fill((x, y, w, S), ROBE[0])
            if part == "tatters" and name in ("front", "back"):
                cx = x + w // 2
                sheet.fill((cx - 2 * S, y, 4 * S, h), CRIMSON[1])
                sheet.fill((cx - 2 * S, y, 1, h), CRIMSON[0])
                sheet.fill((cx + 2 * S - 1, y, 1, h), CRIMSON[0])
            # Torn into long points, deeper at every other.
            for i in range(w):
                p = (i % (3 * S)) / (3 * S)
                deep = (i // (3 * S)) % 2 == 0
                depth = (5 * S if deep else 3 * S) * (1 - abs(2 * p - 1))
                keep = h - int(round(depth))
                for j in range(keep, h):
                    sheet.clear(x + i, y + j)
            # Top of each point a step lighter, so they read as separate strips.
            for i in range(0, w, 3 * S):
                sheet.fill((x + i, y, 1, h - S), ROBE[0])


def paint_cape(sheet):
    for part in ("cape", "cape_low"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "front":  # towards her back: the crimson lining
                folds(sheet, rect, CRIMSON, period=3 * S, shade_bottom=False)
            elif name == "back":
                folds(sheet, rect, ROBE, period=3 * S, phase=S, shade_bottom=False)
                if part == "cape":
                    sheet.fill((x, y, w, S), ARMOUR[2])
                    sheet.fill((x, y + S, w, 2), SILVER[1])
            else:
                sheet.fill(rect, ROBE[1])
                if name in ("left", "right"):
                    sheet.fill((x, y, w, h), CRIMSON[0])
            if part == "cape_low" and name in ("front", "back", "left", "right"):
                ragged(sheet, rect, every=3 * S, depth=4 * S, offset=S if name == "back" else 0)
            if part == "cape_low" and name == "bottom":
                sheet.fill(rect, CLEAR)


def paint_arms(sheet):
    for name, rect in sheet.faces("upper_arm").items():
        folds(sheet, rect, ROBE, period=S + 2, shade_bottom=False)
    for name, rect in sheet.faces("hand").items():
        x, y, w, h = rect
        sheet.fill(rect, SKIN[2])
        if name in ("front", "back", "right", "left"):
            # Long fingers below the knuckles.
            sheet.fill((x, y, w, 2), SKIN[1])
            for i in range(S, w, S):
                sheet.fill((x + i - 1, y + S, 1, h - S), SKIN[0])
            sheet.fill((x, y + h - 2, w, 2), SKIN[1])
        if name == "bottom":
            sheet.fill(rect, SKIN[1])
        if name == "top":
            sheet.fill(rect, ROBE[0])
    for name, rect in sheet.faces("neck").items():
        x, y, w, h = rect
        sheet.fill(rect, SKIN[1])
        sheet.fill((x, y, w, 2), SKIN[0])


def paint_halo(sheet):
    for part in ("halo_inner_bar", "halo_inner_post", "halo_outer_bar", "halo_outer_post"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, HALO[1])
            sheet.fill(rect, HALO[1], glow=True)
            if w > 2 and h > 2:
                sheet.fill((x, y, w, 1), HALO[2])
                sheet.fill((x, y, w, 1), HALO[2], glow=True)
                sheet.fill((x, y + h - 1, w, 1), HALO[0])
                sheet.fill((x, y + h - 1, w, 1), HALO[0], glow=True)


def vesperine():
    model = vm.vesperine_model()
    sheet = Sheet(model)
    paint_face(sheet)
    paint_hair(sheet)
    paint_armour(sheet)
    paint_robe(sheet)
    paint_cape(sheet)
    paint_arms(sheet)
    paint_halo(sheet)
    paint_scythe(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- the skulls

SKULLS = {
    # Dirge: slate-dark bone, soul-blue sockets; Requiem: charcoal bone with a mauve cast, red sockets.
    "dirge": {"bone": [rgb("15171e"), rgb("232631"), rgb("323644"), rgb("464b5c"), rgb("5f6578"), rgb("7a8196")],
              "glow": [rgb("2f8fe8"), rgb("7fd6ff"), rgb("d4f6ff")], "wisp": rgb("1d2a44")},
    "requiem": {"bone": [rgb("19121a"), rgb("281c24"), rgb("372830"), rgb("4b3840"), rgb("624b52"), rgb("7d6268")],
                "glow": [rgb("b81c18"), rgb("ff4a32"), rgb("ffc0a0")], "wisp": rgb("3e1420")},
}
SOCKET = [rgb("07050a"), rgb("130d18"), rgb("1e1626")]
WISP = [rgb("06040a"), rgb("100b16"), rgb("1b1424")]


def socket(sheet, rect, cx, cy, size, bone, glow, lip=2):
    """A square socket `size` model pixels across at (cx, cy) on a face: hard-edged dark plum, darkest under the brow and
    a step lighter along the far rim, over a lip of shaded bone; its glow a flat square over it, brighter at its
    middle."""
    x, y, w, h = cell(rect, cx, cy, size, size)
    sheet.fill((x - lip, y - lip, w + 2 * lip, h + 2 * lip), bone[1])
    sheet.fill((x, y, w, h), SOCKET[1])
    sheet.fill((x, y, w, h // 3), SOCKET[0])
    sheet.fill((x, y + h - 2, w, 2), SOCKET[2])
    sheet.fill((x, y, w, h), glow[0], glow=True)
    inner = max(1, size // 2) * S
    sheet.fill((x + (w - inner) // 2, y + (h - inner) // 2, inner, inner), glow[1], glow=True)


def teeth_row(sheet, rect, cy, count, bone, cx0=0, width=2):
    """A neat row of square teeth across one model-pixel row, with dark gaps."""
    x, y, w, h = rect
    for i in range(count):
        tx = x + (cx0 + i * width) * S
        sheet.fill((tx, y + cy * S, width * S, S), bone[5])
        sheet.fill((tx, y + cy * S + S - 1, width * S, 1), bone[3])
        sheet.fill((tx + width * S - 1, y + cy * S, 1, S), SOCKET[1])


def wisp(sheet, rect, front_right, tongues, tint):
    """Black wisps streaming back from the skull: tapering, waving tongues on a cut-out plane."""
    x, y, w, h = rect
    import math
    for i in range(w):
        t = (w - 1 - i) / max(1, w - 1) if front_right else i / max(1, w - 1)  # 0 at the skull, 1 at the far end
        for j in range(h):
            colour = None
            for centre, thick, phase in tongues:
                c = centre * h + math.sin(t * math.pi * 1.6 + phase) * h * 0.1
                half = thick * h * (1.0 - t) ** 0.75
                d = abs(j + 0.5 - c)
                if d <= half:
                    edge = half - d < 2.5
                    colour = tint if edge else (WISP[2] if t < 0.3 else WISP[1] if t < 0.7 else WISP[0])
            sheet.put(x + i, y + j, colour) if colour else sheet.clear(x + i, y + j)


def skull(kind):
    model = vm.skull_model()
    sheet = Sheet(model)
    bone, glow, tint = SKULLS[kind]["bone"], SKULLS[kind]["glow"], SKULLS[kind]["wisp"]
    for name, rect in sheet.faces("cranium").items():
        x, y, w, h = rect
        sheet.fill(rect, bone[3])
        if name == "front":
            sheet.fill((x, y, w, 2), bone[4])
            sheet.fill(cell(rect, 0, 0, 1, 12), bone[2])
            sheet.fill(cell(rect, 15, 0, 1, 12), bone[2])
            socket(sheet, rect, 2, 5, 4, bone, glow)
            socket(sheet, rect, 10, 5, 4, bone, glow)
            # The nose: a dark notch between the cheekbones, cheekbones lit under the sockets.
            sheet.fill(cell(rect, 7, 9, 2, 2), SOCKET[1])
            sheet.fill(cell(rect, 7, 9, 2, 1), SOCKET[0])
            sheet.fill(cell(rect, 2, 9, 4, 1), bone[4])
            sheet.fill(cell(rect, 10, 9, 4, 1), bone[4])
            sheet.fill(cell(rect, 0, 11, 16, 1), bone[2])
        elif name == "top":
            sheet.fill(rect, bone[4])
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), bone[5] if kind == "dirge" else bone[4])
            # Sutures: a seam down the middle and one across, stepped.
            for j in range(0, h, 2):
                sheet.fill((x + w // 2 - 1 + (j // 2) % 3 - 1, y + j, 2, 2), bone[2])
            for i in range(0, w, 2):
                sheet.fill((x + i, y + h // 3 + (i // 2) % 3 - 1, 2, 2), bone[2])
        elif name == "bottom":
            sheet.fill(rect, bone[1])
        else:
            sheet.fill((x, y, w, 2), bone[4])
            sheet.fill((x, y + h - S, w, S), bone[2])
            if name == "back":
                for j in range(0, h - S, 2):  # a crack running down the back
                    sheet.fill((x + w // 3 + (j // 2) % 4, y + j, 2, 2), bone[1])
    for name, rect in sheet.faces("brow").items():
        x, y, w, h = rect
        sheet.fill(rect, bone[4])
        if name == "front":
            sheet.fill((x, y, w, 2), bone[5])
            sheet.fill((x, y + h - 2, w, 2), bone[2])
        if name == "bottom":
            sheet.fill(rect, bone[1])
    for name, rect in sheet.faces("maxilla").items():
        x, y, w, h = rect
        sheet.fill(rect, bone[3])
        if name == "front":
            sheet.fill((x, y, w, 2), bone[4])
            sheet.fill(cell(rect, 0, 2, 12, 1), bone[2])
            teeth_row(sheet, rect, 3, 6, bone)
        elif name in ("left", "right"):
            front = x + w - 2 * S if name == "right" else x
            sheet.fill((front, y + 2 * S, 2 * S, 2 * S), bone[5])
            sheet.fill((front + (2 * S - 1 if name == "right" else 0), y + 2 * S, 1, 2 * S), SOCKET[1])
        elif name == "bottom":
            sheet.fill(rect, SOCKET[1])
            sheet.fill((x, y + h - S, w, S), bone[4])
    for name, rect in sheet.faces("jaw").items():
        x, y, w, h = rect
        sheet.fill(rect, bone[3])
        if name == "front":
            teeth_row(sheet, rect, 0, 6, bone)
            sheet.fill(cell(rect, 0, 2, 12, 1), bone[2])
        elif name == "top":
            sheet.fill(rect, SOCKET[1])
            sheet.fill((x, y + h - S, w, S), bone[5])
        elif name == "bottom":
            sheet.fill(rect, bone[1])
        elif name in ("left", "right"):
            sheet.fill((x, y + h - S, w, S), bone[2])
    for part, tongues in (("wisp_middle", ((0.25, 0.2, 0.0), (0.58, 0.26, 1.3), (0.86, 0.14, 2.1))),
                          ("wisp_side", ((0.32, 0.26, 0.7), (0.72, 0.22, 2.4)))):
        for name, rect in sheet.faces(part).items():
            if name in ("right", "left"):
                wisp(sheet, rect, name == "right", tongues, tint)
    return sheet


# ------------------------------------------------------------------------------------------------- the grave thrall

OLD_BONE = [rgb("4a4338"), rgb("6b6252"), rgb("8c826d"), rgb("aca18a"), rgb("c8bea5"), rgb("e0d8c2")]
SOUL = [rgb("1da6d8"), rgb("7fe6ff"), rgb("d8faff")]


def thrall():
    model = vm.thrall_model()
    sheet = Sheet(model)
    b = OLD_BONE
    for name, rect in sheet.faces("skull").items():
        x, y, w, h = rect
        sheet.fill(rect, b[3])
        sheet.fill((x, y, w, 2), b[4])
        if name == "front":
            socket(sheet, rect, 1, 2, 2, b, SOUL, lip=1)
            socket(sheet, rect, 5, 2, 2, b, SOUL, lip=1)
            sheet.fill(cell(rect, 3, 4, 2, 1), SOCKET[1])  # the nose
            sheet.fill(cell(rect, 0, 5, 8, 1), b[2])
            for i in range(6):  # upper teeth
                sheet.fill(cell(rect, 1 + i, 6), b[5])
                sheet.fill((x + (2 + i) * S - 1, y + 6 * S, 1, S), SOCKET[1])
        elif name == "top":
            sheet.fill(rect, b[4])
            for j in range(0, h, 2):
                sheet.fill((x + w // 2 + (j // 2) % 3 - 1, y + j, 2, 2), b[2])
        elif name == "bottom":
            sheet.fill(rect, b[1])
        else:
            sheet.fill((x, y + h - S, w, S), b[2])
    for name, rect in sheet.faces("thrall_jaw").items():
        x, y, w, h = rect
        sheet.fill(rect, b[3])
        if name == "front":
            for i in range(6):
                sheet.fill(cell(rect, i, 0), b[5])
                sheet.fill((x + (1 + i) * S - 1, y, 1, S), SOCKET[1])
        elif name == "top":
            sheet.fill(rect, SOCKET[1])
        elif name == "bottom":
            sheet.fill(rect, b[2])
    # The ribs: bars of bone with the gaps between them cut out, a breastbone down the front, a spine at the back.
    for name, rect in sheet.faces("ribs").items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, b[3])
            sheet.fill((x, y, w, 2), b[4])
            continue
        if name == "bottom":
            sheet.fill(rect, CLEAR)
            continue
        for row in range(7):
            if row in (0, 2, 4):
                sheet.fill(cell(rect, 0, row, w // S, 1), b[3])
                sheet.fill((x, y + row * S, w, 1), b[4])
                sheet.fill((x, y + row * S + S - 1, w, 1), b[1])
            else:
                for i in range(w):
                    for j in range(S):
                        sheet.clear(x + i, y + row * S + j)
        if name in ("front", "back"):
            mid = x + w // 2 - S
            sheet.fill((mid, y, 2 * S, h - S), b[3] if name == "front" else b[2])
            sheet.fill((mid, y, 1, h - S), b[4])
            for j in range(S, h - S, S):
                sheet.fill((mid, y + j, 2 * S, 1), b[1])
    for part in ("spine", "pelvis", "arm", "leg"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, b[3])
            if name in ("top", "bottom"):
                sheet.fill(rect, b[2] if name == "bottom" else b[4])
                continue
            sheet.fill((x, y, 1, h), b[4])
            sheet.fill((x + w - 1, y, 1, h), b[1])
            if part == "spine":
                for j in range(S - 1, h, S):
                    sheet.fill((x, y + j, w, 1), b[1])
            elif part == "pelvis" and name == "front":
                sheet.fill(cell(rect, 1, 1), SOCKET[1])
                sheet.fill(cell(rect, 4, 1), SOCKET[1])
            elif part in ("arm", "leg"):
                joint = 6 if part == "arm" else 5
                sheet.fill(cell(rect, 0, joint, w // S, 1), b[4])
                sheet.fill((x, y + joint * S + S - 1, w, 1), b[2])
                if part == "arm":
                    # Bony fingers: dark gaps between them, their tips cut into points.
                    for i in range(2, w, 3):
                        sheet.fill((x + i, y + 10 * S, 1, 2 * S), SOCKET[1])
                    ragged(sheet, rect, every=3, depth=3)
                else:
                    sheet.fill((x, y + h - S, w, S), b[2])
    return sheet


def thrown_scythe():
    sheet = Sheet(vm.thrown_scythe_model())
    paint_scythe(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- the hood and trophies

def hood_cloth():
    """The worn hood's cloth (16x16): night-black in long folds, lit along each ridge with a violet cast."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    tones = [rgb("120d18"), rgb("1b1424"), rgb("251c30"), rgb("33283f")]
    for x in range(16):
        tone = tones[(1, 1, 2, 3, 2, 1, 0, 0)[x % 8]]
        for y in range(16):
            img.putpixel((x, y), tone + (255,))
    return img


def hood_lining():
    """The hood's crimson lining (16x16), in softer folds."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    tones = [CRIMSON[0], CRIMSON[1], CRIMSON[2]]
    for x in range(16):
        tone = tones[(1, 1, 2, 1, 0, 1)[x % 6]]
        for y in range(16):
            img.putpixel((x, y), tone + (255,))
    return img


def trophy(kind):
    """A skull trophy's block textures (32x32): its face (sampled from the top left over 10 by 8 of the block's 16),
    its glow (the sockets alone), its teeth and its bone."""
    bone, glow = SKULLS[kind]["bone"], SKULLS[kind]["glow"]
    out = {}
    b = Image.new("RGBA", (32, 32), bone[3] + (255,))
    px = b.load()
    for y in range(32):
        for x in range(32):
            if y < 2:
                px[x, y] = bone[4] + (255,)
            if (x + y // 2) % 11 == 0 and 8 < y < 24 and x < 20:   # a fine crack
                px[x, y] = bone[1] + (255,)
    out["bone"] = b
    face = Image.new("RGBA", (32, 32), bone[3] + (255,))
    g = Image.new("RGBA", (32, 32), CLEAR)
    fp, gp = face.load(), g.load()
    for x in range(20):
        fp[x, 0] = fp[x, 1] = bone[4] + (255,)
        fp[x, 15] = bone[2] + (255,)
    # Two square sockets, a quarter of the face's width each (5 of 20), under the brow; dark plum, darkest above.
    for sx in (2, 13):
        for y in range(4, 11):
            for x in range(sx - 1, sx + 6):
                fp[x, y] = bone[1] + (255,)
        for y in range(5, 10):
            for x in range(sx, sx + 5):
                fp[x, y] = (SOCKET[0] if y < 7 else SOCKET[1]) + (255,)
                gp[x, y] = (glow[1] if 1 <= x - sx <= 3 and 6 <= y <= 8 else glow[0]) + (255,)
    for y in range(11, 13):  # the nose
        for x in range(9, 11):
            fp[x, y] = SOCKET[1] + (255,)
    out["face"], out["glow"] = face, g
    teeth = Image.new("RGBA", (32, 32), bone[2] + (255,))
    tp = teeth.load()
    for x in range(32):
        for y in range(0, 5):
            tp[x, y] = (SOCKET[1] if x % 4 == 3 else bone[5] if y < 4 else bone[3]) + (255,)
    out["teeth"] = teeth
    return out


def vesperine_textures():
    """Every texture of Vesperine's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {}
    sheets = {"vesperine": vesperine(), "dirge": skull("dirge"), "requiem": skull("requiem"),
              "thrown_scythe": thrown_scythe(), "grave_thrall": thrall()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in ("reaper_shade", "reaper_hood"):
        out[("item", item)] = item_icons.draw(item)
    out[("item", "reaper_hood_cloth")] = hood_cloth()
    out[("item", "reaper_hood_lining")] = hood_lining()
    for kind in ("dirge", "requiem"):
        for part, image in trophy(kind).items():
            out[("block", f"{kind}_skull_{part}")] = image
    return out
