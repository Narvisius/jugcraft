"""Original textures for the Mire Hag and her fight (docs/features/mire-hag.md) (requires Pillow): the sheets of her
GeckoLib bodies (tools/mire_hag_models.py) and their glowmasks, painted box by box and face by face at TEXTURE_SCALE times
the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid.

- The Hag (and her Likenesses, who are she to look at): a skirt and robe of rotted sackcloth in a coarse weave, patched,
  stained dark by the water along its ragged hem, belted with knotted rope; a shawl and hood of grey moss in clumps, its
  fringe and the shawl's back ragged; sickly grey-green skin, a gaunt face with eyes of marsh-light green under the
  hood's brim, a long hooked nose with a wart; matted locks and blades of sedge; a staff of bog oak, knotted; a lantern
  of blackened iron whose panes are open, its flame marsh-light. Her glowmask holds her eyes and the flame.
- The Root Knot and her roots: fen-roots, dark and wet, barked in rings, moss clinging to them.
- Her Mire Grasp: hands of black mud, dripping.
- Her brew: a squat flask of green glass, the venom glowing inside it, corked.
- The worn shawl's textures, and the icons of Hagmoss, the Hag's Eye and the Hag's Shawl (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import mire_hag as hag_numbers
import mire_hag_models as hm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, cell

S = hm.TEXTURE_SCALE

# Palettes, darkest first.
SACK = [rgb("29221b"), rgb("372e25"), rgb("463b2f"), rgb("57493a"), rgb("6a5a47")]
STAIN = [rgb("1c1914"), rgb("26221b"), rgb("312c23")]
ROPE = [rgb("43361f"), rgb("5f4f30"), rgb("7d6a44"), rgb("9b875c")]
LEATHER = [rgb("2a1d12"), rgb("3d2a1a"), rgb("543a24")]
MOSS = [rgb("363c30"), rgb("485141"), rgb("5e6855"), rgb("77806a"), rgb("929a82")]
SKIN = [rgb("434d34"), rgb("596647"), rgb("707e5a"), rgb("88966f"), rgb("a1ad86")]
WART = rgb("5d5135")
HAIR = [rgb("1b1f17"), rgb("272c21"), rgb("343b2c")]
SEDGE = [rgb("57522a"), rgb("736c37"), rgb("918844"), rgb("aea255")]
OAK = [rgb("1a130d"), rgb("251b13"), rgb("33261a"), rgb("433223"), rgb("53402d")]
IRON = [rgb("141417"), rgb("1f1f23"), rgb("2d2d33"), rgb("3f3f47")]
MARSH = [rgb("5bc934"), rgb("8ae95a"), rgb("bbff8a"), rgb("ecffd4")]
SOCKET = rgb("0e110b")
ROOTS = [rgb("1e170e"), rgb("2c2116"), rgb("3c2d1e"), rgb("4e3c28"), rgb("614d35")]
MUD = [rgb("1a1611"), rgb("251f18"), rgb("332a20"), rgb("43382b")]
GLASS = [rgb("173021"), rgb("21442e"), rgb("2f5c3e"), rgb("4a7d58")]
VENOM = [rgb("2c5510"), rgb("468419"), rgb("6dba27"), rgb("a2ea47")]
CORK = [rgb("5a3e22"), rgb("7a5732")]


# ------------------------------------------------------------------------------------------------- materials

def sackcloth(sheet, part, hem=False, phase=0):
    """Rotted sackcloth in a coarse weave on the grid: a mid tone crossed by darker threads every third model pixel, each
    way, and a lighter thread beside them down the cloth; a patch here and there, stitched. With `hem`,
    the side faces' last rows are stained dark by the water and their bottom edge is torn into tatters (cut away)."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, SACK[1] if name == "bottom" else SACK[2])
            continue
        cols, rows = max(1, w // S), max(1, h // S)
        sheet.fill(rect, SACK[2])
        for cy in range(rows):
            if (cy + phase) % 3 == 0:
                sheet.fill((x, y + cy * S, w, 1), SACK[1])
        for cx in range(cols):
            if (cx + phase) % 3 == 0:
                sheet.fill((x + cx * S, y, 1, h), SACK[1])
            elif (cx + phase) % 3 == 1:
                sheet.fill((x + cx * S + S // 2, y, 1, h), SACK[3])
        # A patch, its stitches lighter, where the hash puts one.
        if cols >= 6 and rows >= 6:
            px0, py0 = (2 + phase * 3) % (cols - 4), (1 + phase * 2) % (rows - 4)
            sheet.fill(cell(rect, px0, py0, 3, 3), SACK[3])
            sheet.fill(cell(rect, px0, py0, 3, 1), SACK[4])
            for k in range(3):
                sheet.fill((x + (px0 + k) * S + S // 2, y + py0 * S, 1, 1), SACK[0])
        if hem:
            for cy in range(max(0, rows - 3), rows):
                for cx in range(cols):
                    sheet.fill(cell(rect, cx, cy), STAIN[2] if (cx + cy) % 2 else STAIN[1])
            # The tatters: from the bottom edge, a ragged run of cut-away pixels, one to three deep.
            for cx in range(cols):
                depth = (cx * 7 + len(name) + phase) % 4
                for d in range(depth):
                    for py in range(y + h - (d + 1) * S, y + h - d * S):
                        for px in range(x + cx * S, x + (cx + 1) * S):
                            sheet.clear(px, py)


def moss(sheet, part, fringe=False, phase=0):
    """Grey moss in clumps on the grid: a mid tone, each clump a lighter top and a darker underside, parted by dark gaps;
    the top of the part lighter. With `fringe`, its side faces' bottom edge hangs in ragged strands (the rest cut away)."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        cols, rows = max(1, w // S), max(1, h // S)
        sheet.fill(rect, MOSS[2] if name != "bottom" else MOSS[1])
        for cy in range(rows):
            for cx in range(cols):
                k = (cx * 3 + cy * 5 + phase) % 7
                if k == 0:
                    sheet.fill(cell(rect, cx, cy), MOSS[4] if name == "top" else MOSS[3])
                elif k == 3:
                    sheet.fill(cell(rect, cx, cy), MOSS[1])
                elif k == 5 and name != "bottom":
                    sheet.fill(cell(rect, cx, cy), MOSS[3])
        if name == "top":
            continue
        sheet.fill((x, y, w, 1), MOSS[3])
        if fringe and name not in ("top", "bottom"):
            for cx in range(cols):
                depth = 1 + (cx * 5 + phase + len(name)) % 3
                for d in range(depth):
                    for py in range(y + h - (d + 1) * S, y + h - d * S):
                        for px in range(x + cx * S + (S // 2 if (cx + d) % 2 else 0), x + (cx + 1) * S):
                            sheet.clear(px, py)


def skin(sheet, part, knuckles=False):
    """Her sickly skin: a mid tone, lit along the top, darker toward the bottom; with `knuckles`, a darker band every
    other model pixel down its length, as knotted fingers."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, SKIN[3])
            continue
        if name == "bottom":
            sheet.fill(rect, SKIN[1])
            continue
        sheet.fill(rect, SKIN[2])
        sheet.fill((x, y, w, max(1, S // 2)), SKIN[3])
        sheet.fill((x, y + h - S, w, S), SKIN[1])
        if knuckles:
            for cy in range(1, max(1, h // S), 2):
                sheet.fill((x, y + cy * S, w, S // 2), SKIN[1])


def oak(sheet, part):
    """Bog oak: near-black wood with lighter grain running along it in broken lines, its ends rings."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, OAK[3])
            sheet.fill((x + w // 4, y + h // 4, max(1, w // 2), max(1, h // 2)), OAK[2])
            continue
        sheet.fill(rect, OAK[2])
        cols, rows = max(1, w // S), max(1, h // S)
        tall = h >= w
        for c in range(cols if tall else rows):
            for r in range(rows if tall else cols):
                if (r + c * 3) % 5 == 0:
                    sheet.fill(cell(rect, c, r) if tall else cell(rect, r, c), OAK[3])
                elif (r + c * 3) % 5 == 2:
                    sheet.fill(cell(rect, c, r) if tall else cell(rect, r, c), OAK[1])
        sheet.fill((x, y, 1, h) if tall else (x, y, w, 1), OAK[4])


def iron(sheet, part, open_panes=False):
    """Blackened iron, lit along its top edge. With `open_panes`, each side is a frame a model pixel wide round an open
    pane (cut away), so the flame inside shows."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, IRON[2] if name != "bottom" else IRON[1])
        sheet.fill((x, y, w, 2), IRON[3])
        sheet.fill((x + w - 2, y, 2, h), IRON[1])
        if open_panes and name not in ("top", "bottom"):
            for py in range(y + S, y + h - S):
                for px in range(x + S, x + w - S):
                    sheet.clear(px, py)


def glowing(sheet, part, pal):
    """A part that glows (the flame, the venom): bright along its middle, deepening to its edges; drawn on the glowmask
    too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                edge = min(px - x, x + w - 1 - px, py - y, y + h - 1 - py)
                colour = pal[min(len(pal) - 1, 1 + edge // 2)]
                sheet.put(px, py, colour)
                sheet.put(px, py, colour, glow=True)


def roots(sheet, part, phase=0):
    """Fen-roots, dark and wet: barked in rings across their length, a lighter streak down one side, and moss clinging
    in tufts."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, ROOTS[3])
            sheet.fill((x + w // 4, y + h // 4, max(1, w // 2), max(1, h // 2)), ROOTS[2])
            continue
        sheet.fill(rect, ROOTS[2])
        cols, rows = max(1, w // S), max(1, h // S)
        tall = h >= w
        for r in range(rows if tall else cols):
            if (r + phase) % 3 == 0:
                sheet.fill((x, y + r * S, w, 2) if tall else (x + r * S, y, 2, h), ROOTS[1])
        sheet.fill((x, y, 2, h) if tall else (x, y, w, 2), ROOTS[4])
        for r in range(rows if tall else cols):
            for c in range(cols if tall else rows):
                if (r * 5 + c * 3 + phase) % 11 == 0:
                    sheet.fill(cell(rect, c, r) if tall else cell(rect, r, c), MOSS[2])


def mud(sheet, part, phase=0):
    """Black mud, wet: a mid tone with darker drips running down it and a glistening lighter fleck here and there."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, MUD[2] if name != "bottom" else MUD[1])
        if name in ("top", "bottom"):
            continue
        cols, rows = max(1, w // S), max(1, h // S)
        for c in range(cols):
            run = 1 + (c * 5 + phase) % max(1, rows)
            sheet.fill((x + c * S + S // 4, y, S // 2, run * S), MUD[1])
            if (c + phase) % 3 == 0:
                sheet.fill(cell(rect, c, (c * 3 + phase) % rows), MUD[3])


# ------------------------------------------------------------------------------------------------- the Hag

def paint_robe(sheet):
    sackcloth(sheet, "skirt", hem=True, phase=0)
    sackcloth(sheet, "waist", phase=1)
    # The rope belt: twisted strands on the slant, and the pouch, leather with a flap.
    for name, rect in sheet.faces("belt").items():
        x, y, w, h = rect
        sheet.fill(rect, ROPE[1])
        if name in ("top", "bottom"):
            continue
        for c in range(0, w // S):
            sheet.fill(cell(rect, c, c % 2), ROPE[2])
        sheet.fill((x, y, w, 1), ROPE[3])
        sheet.fill((x, y + h - 1, w, 1), ROPE[0])
    for name, rect in sheet.faces("pouch").items():
        x, y, w, h = rect
        sheet.fill(rect, LEATHER[1])
        sheet.fill((x, y, w, h // 3), LEATHER[2])
        sheet.fill((x, y + h // 3, w, 1), LEATHER[0])


def paint_shawl(sheet):
    sackcloth(sheet, "chest", phase=2)
    moss(sheet, "shawl", phase=0)
    moss(sheet, "shawl_back", fringe=True, phase=2)
    moss(sheet, "fringe", fringe=True, phase=4)
    moss(sheet, "hood", phase=1)
    moss(sheet, "hood_tail", fringe=True, phase=3)
    # Under the hood's brim, where her face meets it, the moss is in shadow.
    hood = sheet.faces("hood")
    x, y, w, h = hood["front"]
    sheet.fill((x, y + h - S, w, S), MOSS[0])


def paint_head(sheet):
    skin(sheet, "skull")
    f = sheet.faces("skull")
    front = f["front"]
    # Her face, rows counted from the top of the skull: the hood covers the top three. Under its brim her eyes, deep
    # sockets with marsh-light in them (on the glowmask); her mouth a thin dark line with one tooth; furrows on her cheeks.
    for cx in (1, 4):
        sheet.fill(cell(front, cx, 3, 2, 1), SOCKET)
        sheet.fill(cell(front, cx, 3, 2, 1), CLEAR, glow=True)
        eye = (front[0] + cx * S + 1, front[1] + 3 * S + 1, 2 * S - 2, S - 2)
        sheet.fill(eye, MARSH[1])
        sheet.fill(eye, MARSH[1], glow=True)
        pupil = (front[0] + cx * S + S - 1, front[1] + 3 * S + 1, 2, S - 2)
        sheet.fill(pupil, MARSH[3])
        sheet.fill(pupil, MARSH[3], glow=True)
    sheet.fill(cell(front, 1, 6, 5, 1), SKIN[0])
    sheet.fill((front[0] + 4 * S, front[1] + 6 * S, S // 2, S // 2), SKIN[4])
    for cx in (0, 6):
        sheet.fill(cell(front, cx, 5), SKIN[1])
    sheet.fill(cell(front, 0, 7, 7, 1), SKIN[1])
    # The nose: long and hooked, a wart on its bridge.
    skin(sheet, "nose")
    nose = sheet.faces("nose")
    sheet.fill((nose["front"][0] + S // 2, nose["front"][1] + S, S, S), WART)
    skin(sheet, "nose_tip")
    # Matted locks at her cheeks: dark, in strands, sedge caught in them.
    for name, rect in sheet.faces("lock").items():
        x, y, w, h = rect
        sheet.fill(rect, HAIR[1])
        for c in range(0, max(1, w // S)):
            if c % 2 == 0:
                sheet.fill((x + c * S, y, S // 2, h), HAIR[2])
        if name in ("right", "left"):
            sheet.fill((x + w // 2, y + h // 3, 2, h // 2), SEDGE[2])
    # The sedge: blades on each crossed plane, the rest cut away.
    for name, rect in sheet.faces("sedge").items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                sheet.clear(px, py)
        for i, (c, top) in enumerate(((0, 2), (2, 0), (3, 3), (5, 1), (6, 4))):
            if c * S >= w:
                continue
            sheet.fill((x + c * S + 1, y + top * S, S - 2, h - top * S), SEDGE[1 + i % 3])
            sheet.fill((x + c * S + 1, y + top * S, S - 2, S), SEDGE[3])


def paint_arms(sheet):
    sackcloth(sheet, "sleeve", phase=1)
    sackcloth(sheet, "cuff", phase=2)
    for part in ("upper_arm", "forearm", "hand"):
        skin(sheet, part)
    for part in ("finger", "finger_short", "finger_long"):
        skin(sheet, part, knuckles=True)
        # Each finger's tip a dark, cracked nail.
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name == "bottom":
                sheet.fill(rect, HAIR[0])
            elif name != "top":
                sheet.fill((x, y + h - S, w, S), HAIR[1])


def paint_staff(sheet):
    for part in ("staff", "knot", "crook", "crook_tip"):
        oak(sheet, part)
    for part in ("lantern_cap", "lantern_base"):
        iron(sheet, part)
    iron(sheet, "lantern_cage", open_panes=True)
    for name, rect in sheet.faces("chain").items():
        x, y, w, h = rect
        sheet.fill(rect, IRON[2])
        for r in range(0, h, S):
            sheet.fill((x, y + r, w, 1), IRON[3])
    glowing(sheet, "flame", MARSH)


def mire_hag():
    sheet = Sheet(hm.mire_hag_model())
    paint_robe(sheet)
    paint_shawl(sheet)
    paint_head(sheet)
    paint_arms(sheet)
    paint_staff(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- her things

def root_knot():
    sheet = Sheet(hm.root_knot_model())
    for i, part in enumerate(("knot", "knot_lump", "knot_side", "tendril", "curl")):
        roots(sheet, part, phase=i)
    return sheet


def hag_roots():
    sheet = Sheet(hm.hag_roots_model())
    for i, part in enumerate(("thick_root", "thin_root", "short_root")):
        roots(sheet, part, phase=i)
    return sheet


def mire_grasp():
    sheet = Sheet(hm.mire_grasp_model())
    for i, part in enumerate(("mud_arm", "mud_palm", "mud_finger")):
        mud(sheet, part, phase=i)
    return sheet


def poison_brew():
    """The flask: green glass, the venom glowing through it below a lighter shoulder; the neck glass; the cork."""
    sheet = Sheet(hm.poison_brew_model())
    for name, rect in sheet.faces("bottle").items():
        x, y, w, h = rect
        sheet.fill(rect, GLASS[2])
        if name in ("top", "bottom"):
            continue
        venom = (x + S // 2, y + S + S // 2, w - S, h - 2 * S)
        sheet.fill(venom, VENOM[2])
        sheet.fill(venom, VENOM[2], glow=True)
        sheet.fill((venom[0], venom[1], venom[2], 2), VENOM[3])
        sheet.fill((venom[0], venom[1], venom[2], 2), VENOM[3], glow=True)
        sheet.fill((x, y, w, 2), GLASS[3])
        sheet.fill((x + 1, y + S, 2, h - 2 * S), GLASS[3])
    for name, rect in sheet.faces("neck").items():
        sheet.fill(rect, GLASS[1])
    for name, rect in sheet.faces("cork").items():
        x, y, w, h = rect
        sheet.fill(rect, CORK[1])
        sheet.fill((x, y + h - 1, w, 1), CORK[0])
    return sheet


# ------------------------------------------------------------------------------------------------- the worn shawl

def shawl(part):
    """The worn shawl's textures (16x16): grey moss in clumps on a 2-pixel grid, its lower edge darker; and sedge, pale
    blades on clear, for the stalks standing out of it."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "moss":
                k = (x // 2 * 3 + y // 2 * 5) % 7
                tone = MOSS[3] if k == 0 else MOSS[1] if k == 3 else MOSS[2]
                if y > 13:
                    tone = MOSS[1]
                px[x, y] = tone + (255,)
            elif x % 4 in (1, 2) and y >= (x * 5) % 6:
                px[x, y] = (SEDGE[3] if y < 4 else SEDGE[2] if x % 4 == 1 else SEDGE[1]) + (255,)
    return img


def mire_hag_textures():
    """Every texture of the Mire Hag's fight, keyed (kind, name) as crop_textures returns them. Her Likenesses' sheet is
    hers, pixel for pixel."""
    import item_icons
    out = {("item", f"hags_shawl_{part}"): shawl(part) for part in ("moss", "sedge")}
    hag = mire_hag()
    sheets = {"mire_hag": hag, "hag_likeness": hag, "root_knot": root_knot(), "hag_roots": hag_roots(),
              "mire_grasp": mire_grasp(), "poison_brew": poison_brew()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in hag_numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in hag_numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
