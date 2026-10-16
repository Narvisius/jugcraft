"""Original textures for the Iron Dreadnought and his fight (docs/features/iron-dreadnought.md) (requires Pillow): the
sheets of his GeckoLib bodies (tools/iron_dreadnought_models.py) and their glowmasks, painted box by box and face by face
at TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean style
(tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no noise,
patterns on the model's pixel grid. The dieselpunk tiers' look (docs/ART_DIRECTION.md): olive drab and gunmetal, rivets
and seams, hazard stripes, chrome pistons and copper.

- The Dreadnought: riveted plates of olive drab in panels, worn bright at their edges, his number stencilled in cream on
  his flanks and glacis; gunmetal joints, braces and fists, bolted; chrome piston rods; a ram and a toe on each foot
  striped yellow and black; treads of black rubber; soot-black stacks; tesla coils of fine copper windings ribbed with
  brown porcelain, a polished copper toroid and a ball of violet light on each; the furnaces behind his vents glowing
  orange behind their grilles, and his vents' flaps louvred, the glow showing at their seams; his searchlight's lens
  warm white. His glowmask holds the lens, the coils' balls, the arcs, the furnaces and the seams of his vents.
- The Sparker: a domed shell of gunmetal, two lit amber lenses, a little copper coil on its back and a violet ball.
- The hot rivet: steel glowing orange to yellow, a streak of heat behind it.
- The mortar shell: olive drab, a brass band, a red nose and a burning fuse; four steel fins.
- The worn visor's textures, and the icons of Salvaged Coil, the Grounding Strap and the Dreadnought Visor
  (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import iron_dreadnought as numbers
import iron_dreadnought_models as dm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, cell

S = dm.TEXTURE_SCALE

# Palettes, darkest first.
OLIVE = [rgb("1f2214"), rgb("2e331d"), rgb("414829"), rgb("555d35"), rgb("6b7444"), rgb("8a9161")]
GUNMETAL = [rgb("17191d"), rgb("24272c"), rgb("33373e"), rgb("464b53"), rgb("5d636c"), rgb("7c838c")]
WORN = rgb("98a0a6")             # bare steel where the paint is worn through
CHROME = [rgb("4a5157"), rgb("7d868d"), rgb("aab3b9"), rgb("d3dade"), rgb("f2f5f6")]
RUBBER = [rgb("0e0f10"), rgb("18191b"), rgb("232528"), rgb("2f3236")]
HAZARD_YELLOW = [rgb("8a6a10"), rgb("c99a1c"), rgb("e9b92a"), rgb("f6d35a")]
HAZARD_BLACK = [rgb("101012"), rgb("1c1c1f")]
COPPER = [rgb("4a2410"), rgb("6e3618"), rgb("974c22"), rgb("bf6a32"), rgb("dc8e4c"), rgb("f2b67a")]
PORCELAIN = [rgb("2e1a10"), rgb("4a2c1a"), rgb("6a4026"), rgb("8a5634")]
SOOT = [rgb("0c0c0d"), rgb("161617"), rgb("222224"), rgb("2e2e31")]
CREAM = rgb("d8cfa8")
BRASS = [rgb("5a4410"), rgb("8a6a1c"), rgb("b8922e"), rgb("dcbc5a")]
SHELL_RED = [rgb("5a160e"), rgb("8a2416"), rgb("b23a22")]
FURNACE = [rgb("4a1404"), rgb("8a2c06"), rgb("d0560c"), rgb("f48a1c"), rgb("ffc048"), rgb("fff0a8")]
LIVE = [rgb("2a1046"), rgb("4a1e7a"), rgb("7036b0"), rgb("9c62dc"), rgb("c8a0f4"), rgb("f0e4ff")]
LENS = [rgb("6a5a30"), rgb("c8b060"), rgb("f2e0a0"), rgb("fff8de")]
AMBER = [rgb("5a3a0e"), rgb("c08420"), rgb("eab040"), rgb("fde08a")]

# A 3 x 5 stencil face for his markings: each glyph's rows, "#" painted.
GLYPHS = {
    "D": ("##.", "#.#", "#.#", "#.#", "##."),
    "-": ("...", "...", "###", "...", "..."),
    "0": ("###", "#.#", "#.#", "#.#", "###"),
    "7": ("###", "..#", ".#.", ".#.", ".#."),
}
MARK = "D-07"


# ------------------------------------------------------------------------------------------------- painting helpers

def _glow(sheet, rect, colour):
    sheet.fill(rect, colour)
    sheet.fill(rect, colour, glow=True)


def rivet(sheet, x, y):
    """A rivet head two painted pixels across: lit at its top left, shadowed under its bottom right."""
    sheet.put(x, y, OLIVE[5])
    sheet.put(x + 1, y, OLIVE[4])
    sheet.put(x, y + 1, OLIVE[4])
    sheet.put(x + 1, y + 1, OLIVE[3])
    sheet.put(x + 2, y + 2, OLIVE[0])
    sheet.put(x + 1, y + 2, OLIVE[1])
    sheet.put(x + 2, y + 1, OLIVE[1])


def bolt(sheet, x, y, pal=GUNMETAL):
    """A hex bolt head three painted pixels across."""
    sheet.fill((x, y, 3, 3), pal[4])
    sheet.put(x + 1, y + 1, pal[5])
    sheet.fill((x + 1, y + 3, 3, 1), pal[0])
    sheet.fill((x + 3, y + 1, 1, 3), pal[0])


def plate(sheet, part, pal=OLIVE, panel=8, rivets=True, worn=True, seam_rows=True):
    """Riveted plate in panels: a mid tone (the top lit, the bottom dark), a seam every `panel` model pixels across each
    side face (and across its middle if it is tall), rivets every three model pixels along every seam and edge a model
    pixel in, the plate worn
    to bare steel along its top edge in a broken line, and a model pixel of grime along its foot."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, pal[3] if name == "top" else pal[1] if name == "bottom" else pal[2])
        if name == "bottom":
            continue
        cols = list(range(panel * S, w, panel * S))
        rows = [h // 2 // S * S] if seam_rows and h >= 12 * S and name != "top" else []
        for cx in cols:
            sheet.fill((x + cx - 1, y, 1, h), pal[0])
            sheet.fill((x + cx, y, 1, h), pal[4])
        for cy in rows:
            sheet.fill((x, y + cy - 1, w, 1), pal[0])
            sheet.fill((x, y + cy, w, 1), pal[4])
        # The face's own edges: lit along the top and left, shadowed along the bottom and right.
        sheet.fill((x, y, w, 1), pal[4])
        sheet.fill((x, y, 1, h), pal[4])
        sheet.fill((x, y + h - 1, w, 1), pal[0])
        sheet.fill((x + w - 1, y, 1, h), pal[0])
        if name != "top":
            sheet.fill((x + 1, y + h - S - 1, w - 2, S), pal[1])
        if worn:
            for i in range(0, w, S):
                if (i // S * 7 + len(part)) % 5 in (0, 1):
                    sheet.fill((x + i, y, S, 2), WORN)
        if rivets and w >= 3 * S and h >= 3 * S:
            lines_x = [S] + [cx + S // 2 for cx in cols] + [w - 2 * S + 1]
            lines_y = [S] + [cy + S // 2 for cy in rows] + [h - 2 * S - 1]
            for ry in lines_y:
                for rx in range(S, w - 2 * S + 1, 3 * S):
                    rivet(sheet, x + rx, y + ry)
            for rx in lines_x:
                for ry in range(S, h - 2 * S, 3 * S):
                    rivet(sheet, x + rx, y + ry)


def metal(sheet, part, pal=GUNMETAL, bolts=True):
    """Gunmetal: a mid tone, lit along its top and left, a darker band along its foot, a bolt in each corner of a side
    face big enough to hold one."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, pal[3] if name == "top" else pal[1] if name == "bottom" else pal[2])
        sheet.fill((x, y, w, 2), pal[4])
        sheet.fill((x, y, 2, h), pal[3])
        sheet.fill((x, y + h - 2, w, 2), pal[0])
        sheet.fill((x + w - 2, y, 2, h), pal[1])
        if bolts and name != "bottom" and w >= 4 * S and h >= 4 * S:
            for bx, by in ((S, S), (w - 2 * S, S), (S, h - 2 * S), (w - 2 * S, h - 2 * S)):
                bolt(sheet, x + bx, y + by, pal)


def chrome(sheet, part):
    """A polished rod: across each side face dark at the edges and bright in a band a third of the way in; its ends
    mid."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, CHROME[1])
            continue
        for i in range(w):
            t = i / max(1, w - 1)
            tone = CHROME[0] if t < 0.1 or t > 0.9 else CHROME[4] if 0.25 <= t <= 0.38 else CHROME[2] if t < 0.6 else CHROME[1]
            sheet.fill((x + i, y, 1, h), tone)
        sheet.fill((x, y + h - 2, w, 2), CHROME[0])


def hazard(sheet, part, width=2):
    """Stripes of yellow and black on the diagonal, `width` model pixels each, on every face; a dark lip along the foot
    and a lit one along the top, and a few scrapes to bare steel."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(h):
            for px in range(w):
                band = ((px + py) // (width * S)) % 2
                tone = HAZARD_YELLOW[2] if band == 0 else HAZARD_BLACK[1]
                sheet.put(x + px, y + py, tone)
        sheet.fill((x, y, w, 1), HAZARD_YELLOW[3])
        sheet.fill((x, y + h - 2, w, 2), HAZARD_BLACK[0])
        for i in range(0, w, 7 * S):
            if name != "bottom" and h > 3 * S:
                sheet.fill((x + i + S, y + h // 3, 2 * S, 1), WORN)


def rubber(sheet, part):
    """A tread of black rubber: its sides ribbed every two model pixels, its sole in blocks."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, RUBBER[1])
        if name == "bottom":
            for bx in range(0, w, 3 * S):
                for by in range(0, h, 3 * S):
                    sheet.fill((x + bx + 1, y + by + 1, 2 * S, 2 * S), RUBBER[2])
            continue
        if name == "top":
            sheet.fill(rect, GUNMETAL[2])
            sheet.fill((x, y, w, 2), GUNMETAL[4])
            for bx in range(S, w - S, 4 * S):
                bolt(sheet, x + bx, y + h // 2 - 1)
            continue
        for bx in range(0, w, 2 * S):
            sheet.fill((x + bx, y, S, h), RUBBER[2])
            sheet.fill((x + bx, y, S, 1), RUBBER[3])
        sheet.fill((x, y + h - 2, w, 2), RUBBER[0])


def copper_windings(sheet, part):
    """The coil's secondary: fine copper wire wound round it, a lit thread every other painted row."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            sheet.fill(rect, COPPER[2])
            continue
        for py in range(h):
            sheet.fill((x, y + py, w, 1), COPPER[3] if py % 2 == 0 else COPPER[1])
        sheet.fill((x, y, 1, h), COPPER[4])
        sheet.fill((x + w - 1, y, 1, h), COPPER[0])


def copper(sheet, part):
    """Polished copper: a mid tone, lit along its top and down a band of its sides, dark at its foot."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, COPPER[4] if name == "top" else COPPER[1] if name == "bottom" else COPPER[3])
        if name in ("top", "bottom"):
            continue
        sheet.fill((x, y, w, 1), COPPER[5])
        sheet.fill((x + w // 4, y, max(1, w // 6), h), COPPER[4])
        sheet.fill((x, y + h - 1, w, 1), COPPER[0])


def porcelain(sheet, part):
    """Brown-glazed porcelain: a mid tone with a glossy lit top row."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, PORCELAIN[2])
        sheet.fill((x, y, w, 1), PORCELAIN[3])
        sheet.fill((x, y + h - 1, w, 1), PORCELAIN[0])


def soot(sheet, part, bands=True):
    """Soot-blackened steel: near black, a lit edge, with darker bands round it every three model pixels."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, SOOT[2] if name != "top" else SOOT[0])
        if name == "top":
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), SOOT[0])
            continue
        sheet.fill((x, y, 1, h), SOOT[3])
        if bands:
            for by in range(0, h, 3 * S):
                sheet.fill((x, y + by, w, S // 2), SOOT[1])


def glowing(sheet, part, pal):
    """Light itself: bright in the middle, deepening to its edges; drawn on the glowmask too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                edge = min(px - x, x + w - 1 - px, py - y, y + h - 1 - py)
                colour = pal[min(len(pal) - 1, 1 + edge // 2)]
                sheet.put(px, py, colour)
                sheet.put(px, py, colour, glow=True)


def grille(sheet, part, every=2):
    """A furnace behind a grille: orange light deepening to its edges (on the glowmask), crossed by dark bars every
    `every` model pixels, which do not glow."""
    glowing(sheet, part, FURNACE[1:])
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        horizontal = w < h
        span = h if horizontal else w
        for i in range(0, span, every * S):
            bar = (x, y + i, w, S // 2) if horizontal else (x + i, y, S // 2, h)
            sheet.fill(bar, GUNMETAL[0])
            sheet.fill(bar, CLEAR, glow=True)


def louvres(sheet, part):
    """A vent's flap: olive plate in slanted slats every two model pixels, a seam of the furnace's glow between them (on
    the glowmask: the weak vents glow before they open)."""
    plate(sheet, part, panel=64, rivets=False, worn=False, seam_rows=False)
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name in ("top", "bottom") or min(w, h) < 3 * S:
            continue
        for i in range(S, h - S, 2 * S):
            sheet.fill((x + S, y + i, w - 2 * S, S), OLIVE[3])
            sheet.fill((x + S, y + i + S, w - 2 * S, 1), OLIVE[0])
            _glow(sheet, (x + S, y + i + S + 1, w - 2 * S, 1), FURNACE[2])
        for bx, by in ((1, 1), (w - 4, 1), (1, h - 4), (w - 4, h - 4)):
            bolt(sheet, x + bx, y + by)


def stencil(sheet, rect, text, cx, cy, colour=CREAM):
    """`text` stencilled in 3 x 5 glyphs a model pixel to the stroke, from (cx, cy) model pixels in the face, the
    stencil's bridges left as breaks in the strokes."""
    x, y, _, _ = rect
    for i, ch in enumerate(text):
        rows = GLYPHS[ch]
        for gy, row in enumerate(rows):
            for gx, c in enumerate(row):
                if c == "#":
                    px, py = x + (cx + i * 4 + gx) * S, y + (cy + gy) * S
                    sheet.fill((px, py, S, S), colour)
                    if gy == 2 and ch not in "-":
                        sheet.fill((px, py + S // 2, S, 1), OLIVE[2])


# ------------------------------------------------------------------------------------------------- the Dreadnought

def paint_hull(sheet):
    plate(sheet, "hull", panel=13)
    hull = sheet.faces("hull")
    # His number on both flanks over the vents, and on the front.
    for side in ("left", "right"):
        stencil(sheet, hull[side], MARK, 2, 3)
    front = hull["front"]
    stencil(sheet, front, MARK, 18, 4)
    # A gauge panel on the front by his number: two dials, their needles and amber glass (glowing).
    for i, (gx, gy) in enumerate(((6, 4), (11, 4))):
        dial = cell(front, gx, gy, 4, 4)
        sheet.fill(dial, GUNMETAL[0])
        inner = (dial[0] + 2, dial[1] + 2, dial[2] - 4, dial[3] - 4)
        _glow(sheet, inner, AMBER[2])
        sheet.fill((inner[0] + inner[2] // 2, inner[1] + 2, 1, inner[3] // 2), GUNMETAL[0])
    plate(sheet, "glacis", panel=12)
    glacis = sheet.faces("glacis")["front"]
    # A hazard band along the glacis's foot.
    x, y, w, h = glacis
    for py in range(h - 3 * S, h - S):
        for px in range(w):
            band = ((px + py) // (2 * S)) % 2
            sheet.put(x + px, y + py, HAZARD_YELLOW[2] if band == 0 else HAZARD_BLACK[1])
    plate(sheet, "skirt_front", panel=8, seam_rows=False)
    plate(sheet, "skirt_side", panel=8, seam_rows=False)
    for part in ("skirt_front", "skirt_side"):
        for name, rect in sheet.faces(part).items():
            if name not in ("top", "bottom"):
                x, y, w, h = rect
                sheet.fill((x, y + h - 2 * S, w, 2 * S), OLIVE[1])  # mud splashed up the skirt
                for i in range(0, w, 3 * S):
                    sheet.fill((x + i, y + h - 3 * S, S + (i // S) % 2 * S, S), OLIVE[1])
    metal(sheet, "deck_rail", bolts=False)
    grille(sheet, "furnace_side")
    grille(sheet, "furnace_back")
    louvres(sheet, "vent_flap")
    louvres(sheet, "vent_back_flap")
    hazard(sheet, "ram")
    metal(sheet, "ram_brace")
    metal(sheet, "belly_hatch", bolts=False)
    belly = sheet.faces("belly_hatch")
    if "bottom" in belly:
        x, y, w, h = belly["bottom"]
        for py in range(h):
            for px in range(w):
                if ((px + py) // (2 * S)) % 2 == 0:
                    sheet.put(x + px, y + py, HAZARD_YELLOW[1])


def paint_cupola(sheet):
    plate(sheet, "cupola", panel=10)
    cupola = sheet.faces("cupola")
    # Vision slits round the cupola's sides and back: dark slots, a lit lip over each.
    for side in ("left", "right", "back"):
        x, y, w, h = cupola[side]
        for sx in range(2 * S, w - 4 * S, 5 * S):
            sheet.fill((x + sx, y + 3 * S, 3 * S, S), GUNMETAL[0])
            sheet.fill((x + sx, y + 3 * S - 1, 3 * S, 1), OLIVE[4])
    metal(sheet, "cupola_roof")
    metal(sheet, "lamp")
    # The lamp housing's front ring, darker round the lens.
    x, y, w, h = sheet.faces("lamp")["front"]
    sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), GUNMETAL[0])
    glowing(sheet, "lens", LENS)
    # The lens's bars: a cross of thin dark lines over the light (not glowing).
    for name, rect in sheet.faces("lens").items():
        x, y, w, h = rect
        for bx in (w // 3, 2 * w // 3):
            sheet.fill((x + bx, y, 1, h), LENS[0])
            sheet.fill((x + bx, y, 1, h), CLEAR, glow=True)
        sheet.fill((x, y + h // 2, w, 1), LENS[0])
        sheet.fill((x, y + h // 2, w, 1), CLEAR, glow=True)
    metal(sheet, "lamp_shutter", bolts=False)
    for name, rect in sheet.faces("lamp_shutter").items():
        if name in ("front", "back"):
            x, y, w, h = rect
            for i in range(S, h - S, 2 * S):
                sheet.fill((x + S, y + i, w - 2 * S, 1), GUNMETAL[0])
                sheet.fill((x + S, y + i + 1, w - 2 * S, 1), GUNMETAL[4])


def paint_back(sheet):
    soot(sheet, "stack")
    soot(sheet, "stack_cap", bands=False)
    metal(sheet, "hatch", bolts=False)
    plate(sheet, "mortar_tube", panel=64, rivets=False, seam_rows=False)
    x, y, w, h = sheet.faces("mortar_tube")["top"]
    sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), SOOT[0])  # the bore
    metal(sheet, "mortar_band", bolts=False)


def paint_shoulders(sheet):
    plate(sheet, "pauldron", panel=7)
    for side in ("left", "right"):
        rect = sheet.faces("pauldron")[side]
        x, y, w, h = rect
        # A hazard band along the pauldron's front edge.
        for py in range(h):
            for px in range(2 * S):
                band = ((px + py) // (2 * S)) % 2
                sheet.put(x + (w - 2 * S + px if side == "right" else px), y + py,
                          HAZARD_YELLOW[2] if band == 0 else HAZARD_BLACK[1])
    metal(sheet, "coil_base")
    copper_windings(sheet, "coil_core")
    porcelain(sheet, "coil_ring")
    copper(sheet, "coil_torus")
    glowing(sheet, "coil_ball", LIVE[1:])
    metal(sheet, "battery")
    battery = sheet.faces("battery")
    x, y, w, h = battery["front"]
    sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), GUNMETAL[1])
    for name, rect in sheet.faces("muzzle").items():
        x, y, w, h = rect
        sheet.fill(rect, GUNMETAL[3])
        if name == "front":
            sheet.fill((x + 2, y + 2, w - 4, h - 4), SOOT[0])
    plate(sheet, "battery_shutter", panel=64, rivets=False, seam_rows=False)
    for name, rect in sheet.faces("battery_shutter").items():
        if name in ("front", "back"):
            x, y, w, h = rect
            for i in range(S, h - S, 2 * S):
                sheet.fill((x + S, y + i, w - 2 * S, 1), OLIVE[0])
                sheet.fill((x + S, y + i + 1, w - 2 * S, 1), OLIVE[4])


def arc_plane(sheet, part, seed):
    """A crackle of arcs on a cut-out plane: three jagged bolts from its foot's middle up and out, white-hot at their
    cores and violet at their edges (all glowing)."""
    for name, rect in sheet.faces(part).items():
        if name not in ("front", "back", "left", "right"):
            continue
        x, y, w, h = rect
        for i in range(w):
            for j in range(h):
                sheet.clear(x + i, y + j)
        for b in range(3):
            px, py = w // 2, h - 1
            direction = (-1, 0, 1)[b]
            step = 0
            while py > 1:
                jog = ((step * 7 + b * 3 + seed) % 5) - 2 + direction
                nx = max(1, min(w - 2, px + jog))
                ny = py - S
                for t in range(S):
                    cx = px + (nx - px) * t // S
                    cy = py - t
                    _glow(sheet, (x + cx - 1, y + cy, 3, 1), LIVE[3])
                    _glow(sheet, (x + cx, y + cy, 1, 1), LIVE[5])
                px, py = nx, ny
                step += 1


def paint_limbs(sheet):
    for part in ("hip_joint", "knee", "ankle", "elbow"):
        metal(sheet, part)
    plate(sheet, "thigh", panel=6)
    plate(sheet, "shin", panel=64, seam_rows=False)
    for name, rect in sheet.faces("shin").items():
        if name not in ("top", "bottom"):
            x, y, w, h = rect
            sheet.fill((x, y + h - 2 * S, w, 2 * S), GUNMETAL[2])  # the housing's steel collar
            sheet.fill((x, y + h - 2 * S, w, 1), GUNMETAL[4])
    chrome(sheet, "shin_rod")
    rubber(sheet, "foot")
    hazard(sheet, "toe", width=1)
    plate(sheet, "upper_arm", panel=64, seam_rows=False)
    plate(sheet, "forearm", panel=64, seam_rows=False)
    for name, rect in sheet.faces("forearm").items():
        if name not in ("top", "bottom"):
            x, y, w, h = rect
            for py in range(h - 3 * S, h - S):
                for px in range(w):
                    band = ((px + py) // (2 * S)) % 2
                    sheet.put(x + px, y + py, HAZARD_YELLOW[2] if band == 0 else HAZARD_BLACK[1])
    chrome(sheet, "piston_rod")
    metal(sheet, "fist")
    for name, rect in sheet.faces("fist").items():
        if name in ("front", "left", "right", "back"):
            x, y, w, h = rect
            # Knuckle plates: three raised bars across the fist's lower half.
            for i in range(3):
                bar = (x + S, y + h // 2 + i * S + 1, w - 2 * S, S - 1)
                sheet.fill(bar, GUNMETAL[4] if i % 2 == 0 else GUNMETAL[3])
        if name == "bottom":
            x, y, w, h = rect
            sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), GUNMETAL[1])


def iron_dreadnought():
    sheet = Sheet(dm.iron_dreadnought_model())
    paint_hull(sheet)
    paint_cupola(sheet)
    paint_back(sheet)
    paint_shoulders(sheet)
    arc_plane(sheet, "arc", 1)
    arc_plane(sheet, "arc_cross", 4)
    paint_limbs(sheet)
    return sheet


# ------------------------------------------------------------------------------------------------- his Sparkers

def sparker():
    sheet = Sheet(dm.sparker_model())
    metal(sheet, "sparker_shell")
    shell = sheet.faces("sparker_shell")
    x, y, w, h = shell["top"]
    sheet.fill((x + S, y + S, w - 2 * S, h - 2 * S), GUNMETAL[3])  # the dome's lit crown
    sheet.fill((x + 2 * S, y + 2 * S, w - 4 * S, h - 4 * S), GUNMETAL[4])
    metal(sheet, "sparker_belly", bolts=False)
    for name, rect in sheet.faces("sparker_lens").items():
        x, y, w, h = rect
        sheet.fill(rect, GUNMETAL[0])
        for lx in (S // 2, w - 2 * S - S // 2):
            _glow(sheet, (x + lx, y + S // 2, 2 * S, h - S), AMBER[2])
            _glow(sheet, (x + lx + S // 2, y + S // 2 + 1, S, S // 2), AMBER[3])
    copper_windings(sheet, "sparker_coil")
    porcelain(sheet, "sparker_ring")
    glowing(sheet, "sparker_ball", LIVE[1:])
    chrome(sheet, "sparker_thigh")
    return sheet


# ------------------------------------------------------------------------------------------------- his rivets and shells

def hot_rivet():
    sheet = Sheet(dm.hot_rivet_model())
    glowing(sheet, "rivet_head", FURNACE[2:])
    glowing(sheet, "rivet_shank", FURNACE[1:5])
    for name, rect in sheet.faces("rivet_streak").items():
        x, y, w, h = rect
        for i in range(w):
            for j in range(h):
                sheet.clear(x + i, y + j)
        for i in range(w):
            fade = i / max(1, w - 1)
            band = max(1, int((h // 2) * (1 - fade)))
            colour = FURNACE[4] if fade < 0.3 else FURNACE[3] if fade < 0.7 else FURNACE[2]
            _glow(sheet, (x + (i if name != "left" else w - 1 - i), y + h // 2 - band // 2, 1, band), colour)
    return sheet


def mortar_shell():
    sheet = Sheet(dm.mortar_shell_model())
    plate(sheet, "shell_body", panel=64, rivets=False, worn=False, seam_rows=False)
    for name, rect in sheet.faces("shell_body").items():
        if name not in ("front", "back"):
            x, y, w, h = rect
            band = (x + (w // 3 if name in ("top", "bottom") else 0), y, S, h) if name in ("top", "bottom") else (x, y, S, h)
            sheet.fill(band, BRASS[2])
    for name, rect in sheet.faces("shell_nose").items():
        sheet.fill(rect, SHELL_RED[1] if name != "top" else SHELL_RED[2])
        x, y, w, h = rect
        sheet.fill((x, y, w, 1), SHELL_RED[2])
    glowing(sheet, "shell_fuse", FURNACE[3:])
    for part in ("shell_fin", "shell_fin_flat"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            sheet.fill(rect, GUNMETAL[3])
            sheet.fill((x, y, w, 1), GUNMETAL[5])
            sheet.fill((x, y + h - 1, w, 1), GUNMETAL[1])
    return sheet


# ------------------------------------------------------------------------------------------------- the worn visor

def visor_texture(part):
    """The worn visor's textures (16x16): "plate", olive drab riveted along its top and bottom rows, a seam down its
    middle, worn bright along its top edge; "lamp", the searchlight's housing round a warm white lens (the lens rows 5
    to 10, columns 5 to 10)."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "plate":
                tone = OLIVE[3] if y < 2 else OLIVE[2]
                if y == 0:
                    tone = WORN if x % 5 in (0, 1) else OLIVE[4]
                if y == 15:
                    tone = OLIVE[0]
                if x in (7, 8) and 2 <= y <= 13:
                    tone = OLIVE[0] if x == 7 else OLIVE[4]
                if y in (2, 13) and x % 3 == 1:
                    tone = OLIVE[5]
            else:
                tone = GUNMETAL[2]
                if y in (0, 1) or x in (0, 15):
                    tone = GUNMETAL[4] if y == 0 else GUNMETAL[3]
                if 4 <= x <= 11 and 4 <= y <= 11:
                    tone = GUNMETAL[0]
                if 5 <= x <= 10 and 5 <= y <= 10:
                    tone = LENS[3] if 6 <= x <= 9 and 6 <= y <= 9 else LENS[2]
                    if x == 7 or y == 7:
                        tone = LENS[1]
            px[x, y] = tone + (255,)
    return img


def iron_dreadnought_textures():
    """Every texture of the Iron Dreadnought's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {("item", f"dreadnought_visor_{part}"): visor_texture(part) for part in ("plate", "lamp")}
    sheets = {"iron_dreadnought": iron_dreadnought(), "sparker": sparker(), "hot_rivet": hot_rivet(),
              "mortar_shell": mortar_shell()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
