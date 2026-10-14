"""Original textures for the Crypt Lich and his fight (docs/features/crypt-lich.md) (requires Pillow): the sheets of his
GeckoLib bodies (tools/crypt_lich_models.py) and their glowmasks, painted box by box and face by face at TEXTURE_SCALE
times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- The Lich: a scholar's robe of plum-grey cloth gone to mould, in long folds, grey-green mould creeping up its ragged
  hem; chains of tarnished gold draped across its front; a heavy mantle edged with gold and a high stiff collar; old
  yellowed bone for his skull, hands and fingers, his eyes cold blue soul-fire in deep sockets; a crown of black iron,
  its points tipped with soul-fire, dark sapphires set in its band; his staff a thighbone, knobbed and bound twice with
  gold, a gold cup and prongs round a soul gem; the skull of soul-fire that forms in his hand. His glowmask holds his
  eyes, the soul-fire on his crown, the skull in his hand and the gem.
- The Restless Dead: old bone, its ribs open, in a torn grave-shroud of linen gone grey-green with the grave; its eyes
  soul-fire.
- The Soul Bolt: a skull of soul-fire, its sockets dark, flames streaming back from it.
- His bones: old bone, knobbed and cracked, on a ragged footing.
- The worn crown's textures, and the icons of Grave Dust, the Emptied Phylactery and the Lich's Crown (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import crypt_lich as lich_numbers
import crypt_lich_models as lm
from crop_textures import rgb
from vesperine_art import CLEAR, SOCKET, Sheet, cell, ragged

S = lm.TEXTURE_SCALE

# Palettes, darkest first.
BONE = [rgb("4a4338"), rgb("6b6252"), rgb("8c826d"), rgb("aca18a"), rgb("c8bea5"), rgb("e0d8c2")]
ROBE = [rgb("15111b"), rgb("1f1927"), rgb("2a2233"), rgb("372d42"), rgb("463a53")]
MOULD = [rgb("26301f"), rgb("37442c"), rgb("4d5b3e"), rgb("66744f")]
GOLD = [rgb("3a2a10"), rgb("62491c"), rgb("8c6c2a"), rgb("b4923e"), rgb("d7b863")]
IRON = [rgb("0c0c10"), rgb("17171d"), rgb("23232b"), rgb("33333d"), rgb("45454f")]
SAPPHIRE = [rgb("08243a"), rgb("0e3a5c"), rgb("195a86")]
SOUL = [rgb("0d6f9a"), rgb("1da6d8"), rgb("7fe6ff"), rgb("d8faff")]
LINEN = [rgb("22231f"), rgb("30322b"), rgb("41443a"), rgb("54584b"), rgb("686c5c")]
EARTH = [rgb("1a1612"), rgb("26201a"), rgb("332b23")]


# ------------------------------------------------------------------------------------------------- materials

def cloth(sheet, part, pal=ROBE, hem=False, mould=False, phase=0):
    """Heavy cloth in long folds on the grid: a mid tone, a darker fold every third model pixel down it and a lit one
    beside it; lit along its top. With `mould`, grey-green mould creeps up its lowest rows in a stepped edge; with `hem`,
    the side faces' bottom edge is torn into tatters (cut away)."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, pal[1] if name == "bottom" else pal[3])
            continue
        cols, rows = max(1, w // S), max(1, h // S)
        sheet.fill(rect, pal[2])
        for cx in range(cols):
            k = (cx + phase) % 3
            if k == 0:
                sheet.fill((x + cx * S, y, S // 2, h), pal[1])
            elif k == 1:
                sheet.fill((x + cx * S + S // 2, y, S // 2, h), pal[3])
        sheet.fill((x, y, w, max(1, S // 2)), pal[4])
        if mould:
            for cx in range(cols):
                rise = 1 + (cx * 5 + phase + len(name)) % 3
                for cy in range(max(0, rows - rise), rows):
                    sheet.fill(cell(rect, cx, cy), MOULD[1] if (cx + cy) % 2 else MOULD[2])
                if rise >= 2:
                    sheet.fill(cell(rect, cx, rows - rise), MOULD[3])
        if hem:
            for cx in range(cols):
                depth = (cx * 7 + len(name) + phase) % 3
                for d in range(depth):
                    for py in range(y + h - (d + 1) * S, y + h - d * S):
                        for px in range(x + cx * S, x + (cx + 1) * S):
                            sheet.clear(px, py)


def bone(sheet, part, knobs=False, cracks=False, phase=0):
    """Old bone: a mid tone lit down one edge and shaded down the other, its ends lighter; with `knobs`, a lighter band
    at each end; with `cracks`, a dark crack running down it here and there."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, BONE[4] if name == "top" else BONE[2])
            sheet.fill((x + w // 4, y + h // 4, max(1, w // 2), max(1, h // 2)), BONE[3])
            continue
        sheet.fill(rect, BONE[3])
        sheet.fill((x, y, max(1, S // 2), h), BONE[4])
        sheet.fill((x + w - max(1, S // 2), y, max(1, S // 2), h), BONE[2])
        if knobs and h >= 4 * S:
            sheet.fill((x, y, w, S), BONE[4])
            sheet.fill((x, y + h - S, w, S), BONE[4])
            sheet.fill((x, y + S, w, 1), BONE[1])
            sheet.fill((x, y + h - S - 1, w, 1), BONE[1])
        if cracks:
            rows = max(1, h // S)
            for cy in range(1, rows - 1):
                if (cy + phase) % 4 == 1:
                    cx = (cy * 3 + phase) % max(1, w // S)
                    sheet.fill((x + cx * S + S // 2, y + cy * S, 1, 2 * S), BONE[1])


def gold(sheet, part, studs=False):
    """Tarnished gold: a mid tone lit along its top and shaded along its bottom; with `studs`, a lit stud every other model
    pixel round it."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, GOLD[2] if name != "bottom" else GOLD[1])
        if name in ("top", "bottom"):
            continue
        sheet.fill((x, y, w, max(1, S // 2)), GOLD[4])
        sheet.fill((x, y + h - max(1, S // 2), w, max(1, S // 2)), GOLD[0])
        if studs:
            for cx in range(0, max(1, w // S), 2):
                sheet.fill((x + cx * S + 1, y + h // 2 - 1, 2, 2), GOLD[4])


def iron(sheet, part):
    """Black iron: near-black, lit along its top edge and down its left, a darker seam at its foot."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, IRON[2] if name != "bottom" else IRON[1])
        sheet.fill((x, y, w, 2), IRON[4] if name == "top" else IRON[3])
        sheet.fill((x, y, 1, h), IRON[3])
        sheet.fill((x, y + h - 2, w, 2), IRON[0])


def glowing(sheet, part, pal=SOUL):
    """Soul-fire: bright along its middle, deepening to its edges; drawn on the glowmask too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                edge = min(px - x, x + w - 1 - px, py - y, y + h - 1 - py)
                colour = pal[min(len(pal) - 1, 1 + edge // 2)]
                sheet.put(px, py, colour)
                sheet.put(px, py, colour, glow=True)


def eye(sheet, rect, cx, cy, size):
    """A deep square socket `size` model pixels across at (cx, cy), with cold soul-fire burning in it (on the glowmask)."""
    x, y, w, h = cell(rect, cx, cy, size, size)
    sheet.fill((x - 1, y - 1, w + 2, h + 2), BONE[1])
    sheet.fill((x, y, w, h), SOCKET[1])
    sheet.fill((x, y, w, h // 3), SOCKET[0])
    flame = (x + S // 2, y + S // 2, w - S, h - S)
    sheet.fill(flame, SOUL[0])
    sheet.fill(flame, SOUL[0], glow=True)
    core = (x + w // 2 - S // 2, y + h // 2 - S // 2, S, S)
    sheet.fill(core, SOUL[2])
    sheet.fill(core, SOUL[2], glow=True)


def teeth(sheet, rect, cy, count, cx0=0):
    """A row of square teeth across one model-pixel row, with dark gaps."""
    x, y, w, h = rect
    for i in range(count):
        tx = x + (cx0 + i) * S
        sheet.fill((tx, y + cy * S, S, S), BONE[5])
        sheet.fill((tx, y + cy * S + S - 1, S, 1), BONE[3])
        sheet.fill((tx + S - 1, y + cy * S, 1, S), SOCKET[1])


def cut(sheet, rect):
    """Cuts a rectangle away (transparent)."""
    x, y, w, h = rect
    for py in range(y, y + h):
        for px in range(x, x + w):
            sheet.clear(px, py)


# ------------------------------------------------------------------------------------------------- the Lich

def paint_robe(sheet):
    cloth(sheet, "hem", hem=True, mould=True, phase=0)
    cloth(sheet, "lap", phase=1)
    cloth(sheet, "robe", phase=2)
    # The robe's front: a dark placket down its middle.
    front = sheet.faces("robe")["front"]
    x, y, w, h = front
    mid = x + w // 2
    sheet.fill((mid - S, y, 2 * S, h), ROBE[0])
    sheet.fill((mid - S, y, 1, h), ROBE[1])
    # The mantle: heavier cloth, mould on its shoulders, a gold edge round its lower rim.
    cloth(sheet, "mantle", pal=ROBE, phase=1)
    for name, rect in sheet.faces("mantle").items():
        x, y, w, h = rect
        if name == "top":
            for cx in range(0, w // S):
                if (cx * 3) % 5 == 1:
                    sheet.fill(cell(rect, cx, (cx * 2) % max(1, h // S)), MOULD[2])
        elif name != "bottom":
            sheet.fill((x, y + h - S, w, S), GOLD[2])
            sheet.fill((x, y + h - S, w, 1), GOLD[4])
            for cx in range(0, w // S, 2):
                sheet.fill((x + cx * S + 1, y + h - S + 1, 2, 2), GOLD[4])
    # The collar: stiff, dark inside, its rim gold.
    for name, rect in sheet.faces("collar").items():
        x, y, w, h = rect
        sheet.fill(rect, ROBE[1] if name == "front" else ROBE[2])
        if name in ("front", "back"):
            sheet.fill((x, y, w, S), GOLD[3])
            sheet.fill((x, y, w, 1), GOLD[4])
            for cx in range(1, w // S, 3):
                sheet.fill((x + cx * S, y + S, S // 2, h - S), ROBE[3] if name == "back" else ROBE[0])
        elif name == "top":
            sheet.fill(rect, GOLD[3])


def paint_chains(sheet):
    """The chains of tarnished gold draped across his robe: two swags of links on a cut-out plane, the upper one
    shallower, each link a lit oval with a dark eye."""
    for name, rect in sheet.faces("chains").items():
        x, y, w, h = rect
        cut(sheet, rect)
        for top, sag in ((S * 1, S * 2.2), (S * 4, S * 3.0)):
            steps = w // 2
            for i in range(steps):
                t = i / max(1, steps - 1)
                px = x + 2 * i
                py = y + int(top + sag * 4 * t * (1 - t))
                link = (i // (S // 2)) % 2
                colour = GOLD[3] if link else GOLD[2]
                sheet.fill((px, py, 2, 3), colour)
                sheet.fill((px, py, 2, 1), GOLD[4] if link else GOLD[3])
                if link and i % S == 0:
                    sheet.fill((px, py + 1, 1, 1), GOLD[0])
        # A clasp where the swags meet his collar's edge: a gold disc at each upper corner.
        for cx in (0, w // S - 1):
            sheet.fill(cell(rect, cx, 0, 1, 2), GOLD[3])
            sheet.fill((x + cx * S, y, S, 1), GOLD[4])


def paint_head(sheet):
    for name, rect in sheet.faces("skull").items():
        x, y, w, h = rect
        sheet.fill(rect, BONE[3])
        if name == "front":
            sheet.fill((x, y, w, S), BONE[4])
            sheet.fill(cell(rect, 0, 0, 1, 6), BONE[2])
            sheet.fill(cell(rect, 7, 0, 1, 6), BONE[2])
            # A heavy brow over deep sockets of soul-fire, a dark notch of a nose, cheekbones, the upper teeth.
            sheet.fill(cell(rect, 0, 1, 8, 1), BONE[4])
            eye(sheet, rect, 1, 2, 2)
            eye(sheet, rect, 5, 2, 2)
            sheet.fill(cell(rect, 3, 4, 2, 1), SOCKET[1])
            sheet.fill(cell(rect, 3, 4, 2, 1)[:2] + (2 * S, S // 2), SOCKET[0])
            sheet.fill(cell(rect, 0, 4, 2, 1), BONE[4])
            sheet.fill(cell(rect, 6, 4, 2, 1), BONE[4])
            sheet.fill(cell(rect, 1, 5, 6, 1), BONE[2])
            teeth(sheet, rect, 6, 6, cx0=1)
        elif name == "top":
            sheet.fill(rect, BONE[4])
            for j in range(0, h, 2):
                sheet.fill((x + w // 2 - 1 + (j // 2) % 3 - 1, y + j, 2, 2), BONE[2])
        elif name == "bottom":
            sheet.fill(rect, BONE[1])
        else:
            sheet.fill((x, y, w, S), BONE[4])
            sheet.fill((x, y + h - S, w, S), BONE[2])
            if name in ("left", "right"):
                # The temple's hollow, and the hinge of the jaw.
                sheet.fill(cell(rect, 2, 2, 3, 2), BONE[2])
                sheet.fill(cell(rect, 5 if name == "right" else 2, 5, 1, 1), SOCKET[1])
            elif name == "back":
                for j in range(0, h - S, 2):
                    sheet.fill((x + w // 3 + (j // 2) % 4, y + j, 2, 2), BONE[1])
    for name, rect in sheet.faces("jaw").items():
        x, y, w, h = rect
        sheet.fill(rect, BONE[3])
        if name == "front":
            teeth(sheet, rect, 0, 6)
            sheet.fill(cell(rect, 0, 1, 6, 1), BONE[2])
        elif name == "top":
            sheet.fill(rect, SOCKET[1])
            sheet.fill((x, y + h - S, w, S), BONE[5])
        elif name == "bottom":
            sheet.fill(rect, BONE[1])
        else:
            sheet.fill((x, y + h - S, w, S), BONE[2])


def paint_crown(sheet):
    iron(sheet, "crown_band")
    # Dark sapphires set round the band, a gold rim to each.
    for name, rect in sheet.faces("crown_band").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            # A ring, open in the middle: the skull's crown shows through it.
            sheet.fill(rect, IRON[3] if name == "top" else IRON[1])
            cut(sheet, (x + S, y + S, w - 2 * S, h - 2 * S))
            continue
        for cx in range(1, w // S - 1, 3):
            sheet.fill((x + cx * S - 1, y + 1, S + 2, h - 2), GOLD[2])
            sheet.fill((x + cx * S, y + 2, S, h - 4), SAPPHIRE[1])
            sheet.fill((x + cx * S, y + 2, S // 2, 2), SAPPHIRE[2])
    iron(sheet, "crown_point")
    for name, rect in sheet.faces("crown_point").items():
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            sheet.fill((x, y, w, S // 2), IRON[4])
    flame_tongue(sheet, "crown_flame")


def flame_tongue(sheet, part):
    """Soul-fire as a tongue on each side face: a rounded foot the full width, narrowing to a point at the top, the rest
    cut away; pale at its heart, deep blue at its edges. The top and bottom are cut away. Drawn on the glowmask too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        cut(sheet, rect)
        if name in ("top", "bottom"):
            continue
        for j in range(h):
            t = j / max(1, h - 1)                     # 0 at the tip, 1 at the foot
            half = (w / 2.0) * min(1.0, 0.25 + t * 1.1)
            for i in range(w):
                d = abs(i + 0.5 - w / 2.0)
                if d <= half:
                    colour = SOUL[0] if half - d < 1.0 else SOUL[2] if t > 0.45 and d < half / 2 else SOUL[1]
                    sheet.put(x + i, y + j, colour)
                    sheet.put(x + i, y + j, colour, glow=True)


def paint_arms(sheet):
    cloth(sheet, "upper_sleeve", phase=1)
    cloth(sheet, "lower_sleeve", hem=True, mould=True, phase=2)
    for name, rect in sheet.faces("lower_sleeve").items():
        if name == "bottom":
            sheet.fill(rect, ROBE[0])  # the sleeve's hollow
    bone(sheet, "hand")
    for name, rect in sheet.faces("hand").items():
        x, y, w, h = rect
        if name in ("front", "back"):
            sheet.fill((x, y + h // 2, w, 1), BONE[1])
    bone(sheet, "finger")
    for name, rect in sheet.faces("finger").items():
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            sheet.fill((x, y + S, w, 1), BONE[1])
            sheet.fill((x, y + 2 * S, w, 1), BONE[1])
        if name == "bottom":
            sheet.fill(rect, BONE[1])


def paint_soul_skull(sheet):
    """The skull of soul-fire in his hand: all aglow but its two dark sockets and its grin."""
    glowing(sheet, "soul_skull")
    front = sheet.faces("soul_skull")["front"]
    for cx in (0, 2):
        r = cell(front, cx, 1, 1, 1)
        x, y, w, h = r
        sheet.fill((x + 1, y, w - 1, h), SOCKET[1])
        sheet.fill((x + 1, y, w - 1, h), CLEAR, glow=True)
    x, y, w, h = cell(front, 1, 3, 2, 1)
    for i in range(0, w, 3):
        sheet.fill((x + i, y + 1, 1, h - 1), SOCKET[1])
        sheet.fill((x + i, y + 1, 1, h - 1), CLEAR, glow=True)


def paint_staff(sheet):
    bone(sheet, "femur", cracks=True)
    bone(sheet, "femur_knob")
    for name, rect in sheet.faces("femur_knob").items():
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            sheet.fill((x + w // 2 - 1, y, 2, h), BONE[2])  # the knuckle's groove
    gold(sheet, "binding", studs=True)
    gold(sheet, "sceptre_cup", studs=True)
    gold(sheet, "sceptre_prong")
    glowing(sheet, "gem")


def crypt_lich():
    sheet = Sheet(lm.crypt_lich_model())
    paint_robe(sheet)
    paint_chains(sheet)
    paint_head(sheet)
    paint_crown(sheet)
    paint_arms(sheet)
    paint_soul_skull(sheet)
    paint_staff(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- the Restless Dead

def restless_dead():
    sheet = Sheet(lm.restless_dead_model())
    for name, rect in sheet.faces("dead_skull").items():
        x, y, w, h = rect
        sheet.fill(rect, BONE[3])
        sheet.fill((x, y, w, 2), BONE[4])
        if name == "front":
            eye(sheet, rect, 1, 1, 2)
            eye(sheet, rect, 4, 1, 2)
            sheet.fill(cell(rect, 3, 3, 1, 1), SOCKET[1])
            sheet.fill(cell(rect, 0, 4, 7, 1), BONE[2])
            teeth(sheet, rect, 5, 5, cx0=1)
        elif name == "top":
            sheet.fill(rect, BONE[4])
            for j in range(0, h, 2):
                sheet.fill((x + w // 2 + (j // 2) % 3 - 1, y + j, 2, 2), BONE[2])
        elif name == "bottom":
            sheet.fill(rect, BONE[1])
        else:
            sheet.fill((x, y + h - S, w, S), BONE[2])
    for name, rect in sheet.faces("dead_jaw").items():
        x, y, w, h = rect
        sheet.fill(rect, BONE[3])
        if name == "front":
            teeth(sheet, rect, 0, 5)
        elif name == "top":
            sheet.fill(rect, SOCKET[1])
        elif name == "bottom":
            sheet.fill(rect, BONE[2])
    # The ribs: bars of bone with the gaps between them cut out, a breastbone down the front.
    for name, rect in sheet.faces("ribs").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, BONE[3] if name == "top" else CLEAR)
            if name == "bottom":
                cut(sheet, rect)
            continue
        for row in range(h // S):
            if row % 2 == 0:
                sheet.fill(cell(rect, 0, row, w // S, 1), BONE[3])
                sheet.fill((x, y + row * S, w, 1), BONE[4])
                sheet.fill((x, y + row * S + S - 1, w, 1), BONE[1])
            else:
                cut(sheet, (x, y + row * S, w, S))
        if name in ("front", "back"):
            mid = x + w // 2 - S // 2
            sheet.fill((mid, y, S, h), BONE[3] if name == "front" else BONE[2])
    for part in ("spine", "pelvis", "dead_arm", "dead_leg"):
        bone(sheet, part, knobs=part in ("dead_arm", "dead_leg"), cracks=part == "dead_leg", phase=len(part))
    bone(sheet, "dead_claw")
    for name, rect in sheet.faces("dead_claw").items():
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            for i in range(S, w, S):
                sheet.fill((x + i, y + S, 1, h - S), SOCKET[1])
            ragged(sheet, rect, every=S, depth=S)
    # The shroud: stained linen over its shoulders, torn open down its front to the ribs, ragged at its hem.
    for name, rect in sheet.faces("shroud").items():
        x, y, w, h = rect
        if name == "bottom":
            cut(sheet, rect)
            continue
        if name == "top":
            sheet.fill(rect, LINEN[3])
            continue
        sheet.fill(rect, LINEN[2])
        cols, rows = max(1, w // S), max(1, h // S)
        for cx in range(cols):
            if cx % 3 == 0:
                sheet.fill((x + cx * S, y, S // 2, h), LINEN[1])
            elif cx % 3 == 2:
                sheet.fill((x + cx * S, y, S // 2, h), LINEN[3])
        sheet.fill((x, y, w, S // 2), LINEN[4])
        for cx in range(cols):  # stains, earth-brown, from the hem up
            for cy in range(rows - 1 - (cx * 5 % 3), rows):
                sheet.fill(cell(rect, cx, cy), LINEN[0] if (cx + cy) % 2 else LINEN[1])
        if name == "front":
            # Torn open from the chest down, the ribs behind.
            for cy in range(2, rows):
                half = min(cols // 2, 1 + cy // 2)
                cut(sheet, (x + (cols // 2 - half) * S, y + cy * S, 2 * half * S, S))
        ragged(sheet, rect, every=S, depth=2 * S, offset=1)
    for name, rect in sheet.faces("rag").items():
        x, y, w, h = rect
        sheet.fill(rect, LINEN[2])
        for cx in range(0, w // S, 2):
            sheet.fill((x + cx * S, y, S // 2, h), LINEN[1])
        ragged(sheet, rect, every=S, depth=3 * S, offset=2)
    return sheet


# ------------------------------------------------------------------------------------------------- the Soul Bolt

def tongues(sheet, rect, pointing):
    """Soul-fire tongues streaming back on a cut-out plane: thick at the skull, tapering to points, waving; glowing."""
    import math
    x, y, w, h = rect
    for i in range(w):
        t = i / max(1, w - 1) if pointing == "right" else (w - 1 - i) / max(1, w - 1)
        for j in range(h):
            colour = None
            for centre, thick, phase in ((0.3, 0.22, 0.0), (0.68, 0.26, 1.7)):
                c = centre * h + math.sin(t * math.pi * 1.4 + phase) * h * 0.08
                half = thick * h * (1.0 - t) ** 0.7
                d = abs(j + 0.5 - c)
                if d <= half:
                    colour = SOUL[1] if half - d < 2.0 else (SOUL[3] if t < 0.25 else SOUL[2] if t < 0.6 else SOUL[1])
            if colour:
                sheet.put(x + i, y + j, colour)
                sheet.put(x + i, y + j, colour, glow=True)
            else:
                sheet.clear(x + i, y + j)


def soul_bolt():
    sheet = Sheet(lm.soul_bolt_model())
    glowing(sheet, "bolt_skull")
    front = sheet.faces("bolt_skull")["front"]
    for cx in (1, 4):
        x, y, w, h = cell(front, cx, 1, 1, 2)
        sheet.fill((x, y + 1, w, h - 2), SOCKET[1])
        sheet.fill((x, y + 1, w, h - 2), CLEAR, glow=True)
    x, y, w, h = cell(front, 1, 4, 4, 1)
    for i in range(0, w, 3):
        sheet.fill((x + i, y, 1, h), SOCKET[1])
        sheet.fill((x + i, y, 1, h), CLEAR, glow=True)
    glowing(sheet, "bolt_jaw")
    for name, rect in sheet.faces("trail").items():
        tongues(sheet, rect, "right" if name == "right" else "left")
    for name, rect in sheet.faces("trail_side").items():
        tongues(sheet, rect, "right")
    return sheet


# ------------------------------------------------------------------------------------------------- his bones

def lich_bones():
    sheet = Sheet(lm.lich_bones_model())
    # The footing: broken bone on dark earth, its corners and edges ragged (cut away).
    for name, rect in sheet.faces("bone_base").items():
        x, y, w, h = rect
        sheet.fill(rect, EARTH[1])
        if name == "top":
            cols, rows = w // S, h // S
            for cx in range(cols):
                for cy in range(rows):
                    k = (cx * 5 + cy * 3) % 11
                    if k == 0:
                        sheet.fill(cell(rect, cx, cy), BONE[2])
                    elif k in (4, 7):
                        sheet.fill(cell(rect, cx, cy), EARTH[2])
                    corner = min(cx, cols - 1 - cx) + min(cy, rows - 1 - cy)
                    if corner < 3:
                        cut(sheet, cell(rect, cx, cy))
        elif name != "bottom":
            for cx in range(w // S):
                if (cx * 3) % 4 == 0:
                    sheet.fill(cell(rect, cx, 0), BONE[2])
    bone(sheet, "tall_bone", knobs=True, cracks=True, phase=1)
    bone(sheet, "long_bone", knobs=True, cracks=True, phase=2)
    bone(sheet, "short_bone", knobs=True, phase=3)
    # A rib: a curved bone drawn on the plane's sides, the rest cut away.
    for name, rect in sheet.faces("rib").items():
        x, y, w, h = rect
        if name not in ("left", "right"):
            sheet.fill(rect, BONE[3])
            continue
        cut(sheet, rect)
        import math
        for j in range(h):
            t = j / max(1, h - 1)
            c = int((w - S) * (0.5 + 0.45 * math.cos(t * math.pi * 0.9)))
            sheet.fill((x + c, y + j, S, 1), BONE[3])
            sheet.fill((x + c, y + j, 1, 1), BONE[4])
            sheet.fill((x + c + S - 1, y + j, 1, 1), BONE[1])
    return sheet


# ------------------------------------------------------------------------------------------------- the worn crown

def crown_texture(part):
    """The worn crown's textures (16x16): black iron, lit along its top rows; across rows 4 to 7, the strip the band's long
    faces show, a lit row, two rows of dark sapphires each rimmed with gold every fourth column, and a dark foot. And
    soul-fire: a tongue of blue flame on clear, for the fire over its points."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "iron":
                tone = IRON[3] if y < 2 else IRON[2]
                if y == 4:
                    tone = IRON[4]
                elif y in (5, 6):
                    tone = GOLD[2] if x % 4 == 0 else SAPPHIRE[2 if y == 5 else 1] if x % 4 in (1, 2) else IRON[2]
                elif y == 7 or y > 13:
                    tone = IRON[0]
                px[x, y] = tone + (255,)
            else:
                half = max(0, 7 - abs(15 - y) // 2) if y > 2 else 0
                if abs(x - 7.5) <= half / 2 + 0.5 and y > 2:
                    tone = SOUL[3] if abs(x - 7.5) < 1.5 and y > 8 else SOUL[2] if abs(x - 7.5) < half / 2 - 0.5 else SOUL[1]
                    px[x, y] = tone + (255,)
    return img


def crypt_lich_textures():
    """Every texture of the Crypt Lich's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {("item", f"lichs_crown_{part}"): crown_texture(part) for part in ("iron", "fire")}
    sheets = {"crypt_lich": crypt_lich(), "restless_dead": restless_dead(), "soul_bolt": soul_bolt(), "lich_bones": lich_bones()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in lich_numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in lich_numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
