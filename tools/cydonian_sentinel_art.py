"""Original textures for the Cydonian Sentinel and its fight (docs/features/cydonian-sentinel.md) (requires Pillow): the
sheets of its GeckoLib bodies (tools/cydonian_sentinel_models.py) and their glowmasks, painted box by box and face by face
at TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"), in Vesperine's clean
style (tools/vesperine_art.py, whose sheet and helpers these use): two or three flat tones a material, hard edges, no
noise, patterns on the model's pixel grid.

- The Sentinel: plates of red Martian stone, stacked and seamed like dressed blocks, lit along their tops; rusted iron at
  its knees, feet, pauldrons and crest, riveted; its fists knuckled stone; its visor a band of dark iron with a slit of
  red light across it; the Ancient Altar's red star cut across its two chest plates and burning; its core a sphere of
  red light, white-hot at its heart, and the cold dark stone it is while it waits; the shards that orbit the unbound
  core, and that bristle on its forearms, rust crystal with red-hot edges. Its glowmask holds the core, the slit, the
  star and the shards' edges.
- A Shard Sentry: stones of red rock round a red spark.
- A Rust Shard: a splinter of rust crystal round a red-hot heart.
- A Quake Spike: red rock, darker at its broken footing, its points lighter.
- A Meteor: dark rock cracked with fire, and the fire streaming behind it.
- The worn visor's textures, the Rusted effect's icon, and the icons of the Cydonian Shard, the Lowfall Core and the
  Sentinel's Visor (tools/item_icons/).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture, nor any other texture,
is read, traced or recoloured.
"""
import cydonian_sentinel as numbers
import cydonian_sentinel_models as sm
from crop_textures import rgb
from vesperine_art import CLEAR, Sheet, cell

S = sm.TEXTURE_SCALE

# Palettes, darkest first.
STONE = [rgb("3a1610"), rgb("5a2216"), rgb("7a3420"), rgb("9a4a2c"), rgb("b8653e"), rgb("d08a5a")]
RUST = [rgb("24160f"), rgb("3e2618"), rgb("62391f"), rgb("865025"), rgb("a86a30"), rgb("c88a46")]
IRON = [rgb("140f0d"), rgb("221a17"), rgb("332824"), rgb("463833")]
COLD = [rgb("1c1412"), rgb("2a1e1b"), rgb("3a2b26"), rgb("4c3a33")]
FIRE = [rgb("5a0804"), rgb("a0140a"), rgb("e03416"), rgb("ff7a32"), rgb("ffc890"), rgb("fff2dc")]
CRYSTAL = [rgb("3a140c"), rgb("6a2414"), rgb("963a1c"), rgb("c0582a")]


# ------------------------------------------------------------------------------------------------- materials

def plates(sheet, part, seam=4, phase=0, lit_top=True):
    """Dressed red stone: a mid tone, a dark seam every `seam` model pixels down each side (stacked blocks), each block lit
    along its top row and shaded along its right; the top face lit, the bottom dark."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        if name == "top":
            sheet.fill(rect, STONE[4] if lit_top else STONE[3])
            sheet.fill((x, y, w, 1), STONE[5])
            continue
        if name == "bottom":
            sheet.fill(rect, STONE[1])
            continue
        sheet.fill(rect, STONE[3])
        rows = max(1, h // S)
        for cy in range(rows):
            if (cy + phase) % seam == 0:
                sheet.fill((x, y + cy * S, w, max(1, S // 2)), STONE[4])
            if (cy + phase) % seam == seam - 1:
                sheet.fill((x, y + cy * S + S - max(1, S // 2), w, max(1, S // 2)), STONE[1])
        # A vertical seam, staggered block to block, as dressed stone is laid.
        cols = max(1, w // S)
        if cols >= 4:
            for cy in range(rows):
                block = (cy + phase) // seam
                cx = (cols // 2 + (block % 2) * (cols // 4 + 1)) % cols
                sheet.fill((x + cx * S, y + cy * S, 1, S), STONE[1])
        sheet.fill((x + w - max(1, S // 2), y, max(1, S // 2), h), STONE[2])
        sheet.fill((x, y, max(1, S // 2), h), STONE[4])


def rust(sheet, part, rivets=True):
    """Rusted iron: a mid tone, lit along its top and down its left, a dark seam at its foot, streaks of deeper rust down
    it every third model pixel; a rivet at each corner of the bigger faces."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, RUST[3] if name != "bottom" else RUST[1])
        if name in ("top", "bottom"):
            sheet.fill((x, y, w, 1), RUST[4])
            continue
        cols = max(1, w // S)
        for cx in range(cols):
            if cx % 3 == 1:
                sheet.fill((x + cx * S + 1, y + S, max(1, S // 2), h - 2 * S), RUST[2])
        sheet.fill((x, y, w, max(1, S // 2)), RUST[5])
        sheet.fill((x, y, 1, h), RUST[4])
        sheet.fill((x, y + h - max(1, S // 2), w, max(1, S // 2)), RUST[0])
        if rivets and w >= 4 * S and h >= 4 * S:
            for rx, ry in ((x + S // 2, y + S), (x + w - S - 1, y + S), (x + S // 2, y + h - 2 * S), (x + w - S - 1, y + h - 2 * S)):
                sheet.fill((rx, ry, 2, 2), RUST[1])
                sheet.put(rx, ry, RUST[5])


def glowing(sheet, part, pal=FIRE, heart=True):
    """Red light: deepening from a white-hot heart to its edges; drawn on the glowmask too."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        for py in range(y, y + h):
            for px in range(x, x + w):
                edge = min(px - x, x + w - 1 - px, py - y, y + h - 1 - py)
                step = edge * 2 // max(1, S)
                colour = pal[min(len(pal) - 1, 1 + step)] if heart else pal[min(3, 1 + step)]
                sheet.put(px, py, colour)
                sheet.put(px, py, colour, glow=True)


def crystal(sheet, part, edge_glow=True):
    """Rust crystal: dark facets, a lit one down its left; with `edge_glow`, its edges red-hot (on the glowmask)."""
    for name, rect in sheet.faces(part).items():
        x, y, w, h = rect
        sheet.fill(rect, CRYSTAL[1])
        sheet.fill((x, y, max(1, w // 2), h), CRYSTAL[2])
        sheet.fill((x, y, 1, h), CRYSTAL[3])
        if edge_glow:
            for px, py, pw, ph in ((x, y, w, 1), (x, y + h - 1, w, 1), (x + w - 1, y, 1, h)):
                sheet.fill((px, py, pw, ph), FIRE[2])
                sheet.fill((px, py, pw, ph), FIRE[2], glow=True)


# ------------------------------------------------------------------------------------------------- the Sentinel

STAR = [  # the Ancient Altar's red star, as cut across its chest plates: the right half, its middle column at the inner edge
    "....X",
    "....X",
    "...XX",
    "XXXXX",
    ".XXXX",
    "..XXX",
    "..XXX",
    ".XX.X",
    ".X..X",
    "X....",
]


def paint_plates(sheet):
    """The chest plates: smooth red stone framed in rust, the star cut across the pair and burning. The left plate is the
    right's mirror (one region), so the star is cut as its right half, its middle at the inner edge."""
    for name, rect in sheet.faces("plate").items():
        x, y, w, h = rect
        if name != "front":
            sheet.fill(rect, STONE[2] if name != "back" else STONE[1])
            continue
        sheet.fill(rect, STONE[3])
        sheet.fill((x, y, w, S), RUST[3])
        sheet.fill((x, y + h - S, w, S), RUST[2])
        sheet.fill((x, y, S, h), RUST[3])
        top = 1
        for row, line in enumerate(STAR):
            for col, ch in enumerate(line):
                if ch == "X":
                    rx, ry, rw, rh = cell(rect, col, top + row)
                    sheet.fill((rx, ry, rw, rh), FIRE[2])
                    sheet.fill((rx, ry, rw, rh), FIRE[2], glow=True)
                    sheet.fill((rx + 1, ry + 1, rw - 2, rh - 2), FIRE[3])
                    sheet.fill((rx + 1, ry + 1, rw - 2, rh - 2), FIRE[3], glow=True)


def paint_helm(sheet):
    plates(sheet, "helm", seam=5)
    for name, rect in sheet.faces("helm").items():
        x, y, w, h = rect
        if name == "front":
            # Brows over the visor, and a mouth-plate under it.
            sheet.fill(cell(rect, 0, 2, 10, 1), STONE[1])
            sheet.fill(cell(rect, 2, 7, 6, 2), STONE[2])
            sheet.fill(cell(rect, 2, 7, 6, 1), STONE[4])
    for name, rect in sheet.faces("visor").items():
        x, y, w, h = rect
        sheet.fill(rect, IRON[2])
        sheet.fill((x, y, w, 1), IRON[3])
        if name == "front":
            slit = (x + S, y + S, w - 2 * S, S)
            sheet.fill(slit, FIRE[3])
            sheet.fill(slit, FIRE[3], glow=True)
            sheet.fill((x + 3 * S, y + S + 1, w - 6 * S, S - 2), FIRE[5])
            sheet.fill((x + 3 * S, y + S + 1, w - 6 * S, S - 2), FIRE[5], glow=True)
    rust(sheet, "crest", rivets=False)


def paint_fist(sheet):
    plates(sheet, "fist", seam=9)
    for name, rect in sheet.faces("fist").items():
        x, y, w, h = rect
        if name in ("front", "bottom"):
            # Knuckles: four ridges across it.
            for i in range(4):
                sheet.fill(cell(rect, 1 + i * 2, 1 if name == "front" else 2, 1, 2), STONE[4])
                sheet.fill(cell(rect, 2 + i * 2, 1 if name == "front" else 2, 1, 2), STONE[2])


def paint_core(sheet):
    glowing(sheet, "core")
    glowing(sheet, "core_facet")
    for name, rect in sheet.faces("core_stone").items():
        x, y, w, h = rect
        sheet.fill(rect, COLD[2])
        sheet.fill((x, y, w, S), COLD[3])
        sheet.fill((x + w - S, y, S, h), COLD[1])
        # Cracks where the light sleeps (dark, not glowing).
        for cx, cy, cw, ch in ((2, 1, 1, 3), (3, 3, 2, 1), (5, 4, 1, 3), (1, 5, 2, 1)):
            sheet.fill(cell(rect, cx, cy, cw, ch), COLD[0])
            sheet.fill(cell(rect, cx, cy, cw, ch)[:2] + (S // 2, S // 2), rgb("4a120c"))


def cydonian_sentinel():
    sheet = Sheet(sm.cydonian_sentinel_model())
    for part, seam, phase in (("hips", 3, 0), ("thigh", 4, 1), ("shin", 4, 0), ("waist", 3, 2), ("chest_back", 6, 0),
                              ("chest_side", 6, 0), ("chest_top", 3, 0), ("chest_bottom", 3, 0), ("upper_arm", 4, 2),
                              ("forearm", 5, 0)):
        plates(sheet, part, seam=seam, phase=phase)
    for part in ("knee", "foot", "pauldron"):
        rust(sheet, part)
    paint_plates(sheet)
    paint_helm(sheet)
    paint_fist(sheet)
    paint_core(sheet)
    crystal(sheet, "arm_shard")
    crystal(sheet, "orbit_shard")
    return sheet


# ------------------------------------------------------------------------------------------------- its fight's bodies

def shard_sentry():
    sheet = Sheet(sm.shard_sentry_model())
    plates(sheet, "sentry_stone", seam=2)
    glowing(sheet, "spark")
    return sheet


def rust_shard():
    sheet = Sheet(sm.rust_shard_model())
    crystal(sheet, "splinter", edge_glow=False)
    crystal(sheet, "shard_barb")
    glowing(sheet, "shard_heart")
    return sheet


def quake_spike():
    sheet = Sheet(sm.quake_spike_model())
    for name, rect in sheet.faces("rubble").items():
        x, y, w, h = rect
        sheet.fill(rect, STONE[1])
        for cx in range(0, max(1, w // S), 3):
            sheet.fill(cell(rect, cx, 0, 1, 1), STONE[2])
    for part in ("spike_tall", "spike_short"):
        for name, rect in sheet.faces(part).items():
            x, y, w, h = rect
            if name in ("top", "bottom"):
                sheet.fill(rect, STONE[4] if name == "top" else STONE[1])
                continue
            # Darker at the foot, lighter towards the point (the top of the face).
            rows = max(1, h // S)
            for cy in range(rows):
                share = cy / max(1, rows - 1)
                tone = STONE[4] if share < 0.2 else STONE[3] if share < 0.6 else STONE[2]
                sheet.fill((x, y + cy * S, w, S), tone)
            sheet.fill((x, y, 1, h), STONE[5])
            sheet.fill((x + w - 1, y, 1, h), STONE[1])
    return sheet


def sentinel_meteor():
    sheet = Sheet(sm.sentinel_meteor_model())
    for name, rect in sheet.faces("meteor_rock").items():
        x, y, w, h = rect
        sheet.fill(rect, COLD[2])
        sheet.fill((x, y, w, S), COLD[3])
        for cx, cy, cw, ch in ((1, 2, 3, 1), (3, 2, 1, 3), (5, 5, 2, 1), (2, 6, 1, 1)):
            sheet.fill(cell(rect, cx, cy, cw, ch), FIRE[3])
            sheet.fill(cell(rect, cx, cy, cw, ch), FIRE[3], glow=True)
    for name, rect in sheet.faces("meteor_fire").items():
        x, y, w, h = rect
        if name in ("top", "bottom"):
            continue
        rows = h // S
        for cy in range(rows):
            # The flame narrows and pales towards its tail (the face's top); cut out round it.
            share = cy / max(1, rows - 1)
            half = (w / S) * (0.2 + 0.3 * share)
            tone = FIRE[2] if share > 0.7 else FIRE[3] if share > 0.35 else FIRE[4]
            for cx in range(w // S):
                if abs(cx + 0.5 - w / S / 2) <= half:
                    sheet.fill(cell(rect, cx, cy), tone)
                    sheet.fill(cell(rect, cx, cy), tone, glow=True)
    return sheet


# ------------------------------------------------------------------------------------------------- the worn visor, the effect

def visor_texture(part):
    """The worn visor's textures (16x16): red stone, lit along its top rows, with a band of dark iron across rows 6 to
    9 and a red slit in it (the slit's row glows on the model as light_emission). And the slit's light alone."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if part == "stone":
                tone = STONE[4] if y < 2 else STONE[3] if y % 4 else STONE[2]
                if 6 <= y <= 9:
                    tone = IRON[3] if y == 6 else IRON[2] if y != 9 else IRON[1]
                px[x, y] = tone + (255,)
            else:
                px[x, y] = (FIRE[5] if 6 <= x <= 9 else FIRE[3]) + (255,)
    return img


def rusted_icon():
    """The Rusted effect's icon (18x18): a cracked shield of iron eaten through with rust."""
    from PIL import Image
    img = Image.new("RGBA", (18, 18), CLEAR)
    px = img.load()
    for y in range(2, 16):
        half = 7 if y < 10 else 7 - (y - 9)
        for x in range(9 - half, 9 + half):
            edge = x == 9 - half or x == 9 + half - 1 or y == 2 or y == 15
            tone = IRON[0] if edge else RUST[3] if (x * 3 + y * 5) % 7 > 2 else RUST[2]
            if not edge and (x + y) % 9 == 0:
                tone = RUST[5]
            px[x, y] = tone + (255,)
    for x, y in ((8, 4), (9, 5), (9, 6), (8, 7), (8, 8), (9, 9), (10, 10)):
        px[x, y] = IRON[0] + (255,)
    return img


def cydonian_sentinel_textures():
    """Every texture of the Cydonian Sentinel's fight, keyed (kind, name) as crop_textures returns them."""
    import item_icons
    out = {("item", f"sentinels_visor_{part}"): visor_texture(part) for part in ("stone", "glow")}
    out[("mob_effect", "rusted")] = rusted_icon()
    sheets = {"cydonian_sentinel": cydonian_sentinel(), "shard_sentry": shard_sentry(), "rust_shard": rust_shard(),
              "quake_spike": quake_spike(), "sentinel_meteor": sentinel_meteor()}
    for name, sheet in sheets.items():
        out[("entity", name)] = sheet.img
        if name in numbers.GLOWING:
            out[("entity", f"{name}_glowmask")] = sheet.glow
    for item in numbers.items():
        out[("item", item)] = item_icons.draw(item)
    return out
