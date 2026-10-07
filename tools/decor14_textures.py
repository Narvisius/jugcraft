"""Original textures for the fourteenth batch of Halloween decorations, costumes (requires Pillow): the six outfits as
worn (entity/costume/<outfit>, 128 texels wide, painted box by box on the layout decor14_data.layout gives them, which
the client's CostumeLayer reads from costumes.json), the Skeleton Suit's glowing bones (entity/costume/skeleton_suit_glow),
the outfits' items, and the Costume Trunk's wood, lid, brass, lining and the costumes heaped in it.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. The worn textures are see-through where nothing is
painted (between a box's faces, the hem of the cape, the bat hood's face opening, the points of the ears and the
scallops of the wings); the block textures are 16x16 and opaque; the items are see-through round their shapes.

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""
import math

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put
from decor14_data import layout
from agriculture import OUTFITS

BLACK = [rgb("0e0e12"), rgb("16161c"), rgb("1e1e26"), rgb("2a2a34")]
RED = [rgb("5a0a12"), rgb("7a0e1a"), rgb("9a1422"), rgb("b81e2c")]
LINEN = [rgb("8a7e62"), rgb("a2967a"), rgb("b8ac90"), rgb("cec4a8")]
BONE = [rgb("d8d4c4"), rgb("ece8da"), rgb("f8f6ee")]
FUR = [rgb("3a2e24"), rgb("4a3a2c"), rgb("5a4834"), rgb("6e5a42")]
BAT = [rgb("1e1a1c"), rgb("2a2426"), rgb("383032"), rgb("463c3e")]
PINK = rgb("d07a8a")
GOLD = [rgb("8a6414"), rgb("b88a22"), rgb("dcb03c"), rgb("f4d870")]
PURPLE = [rgb("3a1458"), rgb("4a1a72"), rgb("5e248c"), rgb("7432a8")]
WOOD = [rgb("3a2414"), rgb("4a2e1a"), rgb("5a3820"), rgb("6a4428")]
GLOW = (190, 255, 200)


# ---------------------------------------------------------------- painting a box's faces

def faces(uv):
    """Each face of a box laid out as a vanilla model box's: (face, left, top, width, height) on the texture."""
    u, v, w, h, d = uv
    return [("up", u + d, v, w, d), ("down", u + d + w, v, w, d), ("west", u, v + d, d, h), ("north", u + d, v + d, w, h),
            ("east", u + d + w, v + d, d, h), ("south", u + 2 * d + w, v + d, w, h)]


def dark(g, x, y):
    """The black cloth of a costume: its middle tone in small clumps of the one beside it (tools/block_style.py)."""
    return BLACK[2] if g(x, y) > 0.56 else BLACK[1]


def cape(face, x, y, w, h, g):
    if y >= h - 2 and face in ("north", "south") and (x % 4 in (1, 2)) == (y == h - 1):
        return None                                     # a scalloped hem
    if face == "north":
        return RED[1 + (x % 3 == 0)] if x % 5 else RED[0]
    return BLACK[3] if x % 4 == 0 and face == "south" else dark(g, x, y)


def collar(face, x, y, w, h, g):
    return BLACK[2] if face == "south" or face == "up" else RED[2 if y % 3 else 1]


def wraps(face, x, y, w, h, g):
    band = (y + x // 3) // 2
    if (y + x // 3) % 2 == 1 and g(x, y) < 0.3:
        return rgb("3a3024")                            # a gap between strips
    return LINEN[[1, 2, 3, 2][band % 4]]


def mummy_head(face, x, y, w, h, g):
    if face == "north" and y == 3 and 1 <= x <= w - 2:
        return rgb("1a1410") if x not in (2, w - 3) else rgb("c8f0a0")
    return wraps(face, x, y, w, h, g)


def strip(face, x, y, w, h, g):
    return LINEN[2 + (y % 3 == 0)]


SKULL = [
    ".........",
    "..BBBBB..",
    ".BBBBBBB.",
    ".B..B..B.",
    ".B..B..B.",
    ".BBB.BBB.",
    "..BBBBB..",
    "..B.B.B..",
    "........."]


def skull(face, x, y, w, h, g):
    if face == "north" and y < len(SKULL) and x < len(SKULL[y]) and SKULL[y][x] == "B":
        return BONE[2 if y < 3 else 1]
    return dark(g, x, y)


def ribs(face, x, y, w, h, g):
    mid = w // 2
    if face == "north":
        if x in (mid - 1, mid) and 1 <= y <= 8:
            return BONE[1]                              # the breastbone
        if y in (2, 4, 6, 8) and 1 <= x <= w - 2 and abs(x - mid + 0.5) > 1:
            return BONE[2]                              # ribs
        if y == 11 and 1 <= x <= w - 2:
            return BONE[0]                              # the pelvis
    if face == "south" and x in (mid - 1, mid) and y % 2 == 0:
        return BONE[1]                                  # the spine
    return dark(g, x, y)


def long_bone(face, x, y, w, h, g, knee):
    """Two long bones, one above the other, down the middle of the front and back: a thin shaft with a knob at each
    end, the two meeting at the elbow or knee."""
    if face in ("north", "south"):
        mid = w // 2
        ends = (1, knee - 1, knee + 1, h - 2)
        if y in ends and abs(x - mid) <= 1:
            return BONE[2] if x == mid else BONE[0]
        if x == mid and 1 <= y <= h - 2:
            return BONE[1]
    return dark(g, x, y)


def arm_bones(face, x, y, w, h, g):
    return long_bone(face, x, y, w, h, g, 6)


def leg_bones(face, x, y, w, h, g):
    return long_bone(face, x, y, w, h, g, 6)


def fur(face, x, y, w, h, g):
    v = g(x, y)
    if v < 0.3:
        return FUR[0]                                   # a shaggy streak
    return FUR[1 if v < 0.5 else 2 if v < 0.72 else 3]


def wolf_head(face, x, y, w, h, g):
    if face == "north" and y == 3 and x in (2, 6):
        return rgb("f0c020")                            # yellow eyes
    if face == "north" and y == 2 and x in (1, 2, 6, 7):
        return FUR[0]                                   # the brow
    return fur(face, x, y, w, h, g)


def snout(face, x, y, w, h, g):
    if face == "north":
        if y == 0:
            return rgb("141010")                        # the nose
        if y == h - 1:
            return BONE[2] if x in (0, w - 1) else rgb("2a0e0e")   # fangs either side of the mouth
    if face == "up" and y < 1:
        return rgb("141010")
    return FUR[2]


def wolf_ear(face, x, y, w, h, g):
    if face in ("north", "south") and y == 0 and x != w // 2:
        return None
    return rgb("8a5a5a") if face == "north" and y > 0 and 0 < x < w - 1 else FUR[1]


def claw(face, x, y, w, h, g):
    return BONE[0]


def tail_fur(face, x, y, w, h, g):
    if face == "south":
        return FUR[3]
    return fur(face, x, y, w, h, g)


def band(face, x, y, w, h, g):
    return BLACK[2]


def pointed(face, x, y, w, h, palette, inner):
    """An ear: pointed on its front and back, with a coloured inside on its front."""
    if face in ("north", "south") and y == 0 and x != w // 2:
        return None
    if face == "north" and y >= 1 and 0 < x < w - 1:
        return inner
    return palette[2]


def cat_ear(face, x, y, w, h, g):
    return pointed(face, x, y, w, h, BLACK, PINK)


def catsuit(face, x, y, w, h, g):
    return BLACK[3] if (x + y) % 7 == 0 else dark(g, x, y)


def bell(face, x, y, w, h, g):
    return GOLD[3] if (x, y) == (0, 0) else GOLD[2]


def tail(face, x, y, w, h, g):
    return BLACK[1 if y % 3 == 0 else 2]


def bat_hood(face, x, y, w, h, g):
    if face == "north" and 2 <= x <= w - 3 and 2 <= y <= h - 2:
        return None                                     # the face shows through
    return BAT[2 if g(x, y) > 0.56 else 1]


def bat_ear(face, x, y, w, h, g):
    return pointed(face, x, y, w, h, BAT, rgb("6a3a40"))


def bat_suit(face, x, y, w, h, g):
    return BAT[3 if g(x, y) > 0.72 else 2 if g(x, y) > 0.5 else 1]


def wing(face, x, y, w, h, g):
    """Leathery membrane between four finger bones, its bottom edge scalloped between them."""
    fingers = (0, 4, 8, w - 1)
    for a, c in zip(fingers, fingers[1:]):
        if a <= x <= c:
            depth = round(3 * math.sin(math.pi * (x - a) / max(1, c - a)))
            if face in ("north", "south") and y > h - 1 - depth:
                return None
    if face in ("north", "south") and (x in fingers or y == 0):
        return BAT[3]
    return BAT[1 if g(x, y) > 0.6 else 0]


PAINTERS = {"cape": cape, "collar": collar, "wraps": wraps, "mummy_head": mummy_head, "strip": strip, "skull": skull, "ribs": ribs,
            "arm_bones": arm_bones, "leg_bones": leg_bones, "fur": fur, "wolf_head": wolf_head, "snout": snout, "wolf_ear": wolf_ear,
            "claw": claw, "tail_fur": tail_fur, "band": band, "cat_ear": cat_ear, "catsuit": catsuit, "bell": bell, "tail": tail,
            "tail_tip": tail, "bat_hood": bat_hood, "bat_ear": bat_ear, "bat_suit": bat_suit, "wing": wing}


def worn(name, seed):
    """The outfit's texture, and (for a glowing outfit) its glow: the bones alone, everything else clear."""
    pieces, (width, height) = layout(name)
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    g = bs.grain(16, 16, seed)
    for p in pieces:
        for bx in p["boxes"]:
            paint = PAINTERS[bx["paint"]]
            for face, left, top, fw, fh in faces(bx["uv"]):
                for y in range(fh):
                    for x in range(fw):
                        colour = paint(face, x, y, fw, fh, g)
                        if colour is not None:
                            img.putpixel((left + x, top + y), colour + (255,))
                            if colour in BONE:
                                glow.putpixel((left + x, top + y), GLOW + (255,))
    return img, glow


# ---------------------------------------------------------------- items

def icon(rows, colours):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                put(img, x, y, colours[ch])
    return img


CAPE_ICON = [
    "................",
    "....RR....RR....",
    "....RBBBBBBR....",
    "....BBBBBBBB....",
    "...BBrrrrrrBB...",
    "...BBrrrrrrBB...",
    "..BBBrrrrrrBBB..",
    "..BBrrrrrrrrBB..",
    "..BBrrrrrrrrBB..",
    ".BBBrrrrrrrrBBB.",
    ".BBrrrrrrrrrrBB.",
    ".BBrrrrrrrrrrBB.",
    "BBBrrrrrrrrrrBBB",
    "BB.BB.BBBB.BB.BB",
    "................",
    "................"]

WRAPS_ICON = [
    "................",
    "................",
    "......LLLL......",
    "....LLllllLL....",
    "...LllLLLLllL...",
    "...LlL....LlL...",
    "..LlL..DD..LlL..",
    "..LlL.D..D.LlL..",
    "..LlL.D..D.LlL..",
    "..LlL..DD..LlL..",
    "...LlL....LlL...",
    "...LllLLLLllLLLL",
    "....LLllllLLllll",
    "......LLLL....LL",
    "................",
    "................"]

SKELETON_ICON = [
    "................",
    "....KK....KK....",
    "...KKKKKKKKKK...",
    "..KKKKKWWKKKKK..",
    "..KKWWWWWWWWKK..",
    "..KKKKKWWKKKKK..",
    "..KKWWWWWWWWKK..",
    "..KKKKKWWKKKKK..",
    "..KKWWWWWWWWKK..",
    "..KKKKKWWKKKKK..",
    "..KKKKKWWKKKKK..",
    "..KKWWWWWWWWKK..",
    "..KKKKKKKKKKKK..",
    "..KKKKKKKKKKKK..",
    "................",
    "................"]

WOLF_ICON = [
    "................",
    "..FF........FF..",
    "..FpF......FpF..",
    "..FppFFFFFFppF..",
    "..FFFFFFFFFFFF..",
    ".FFFFFFFFFFFFFF.",
    ".FFYFFFFFFFFYFF.",
    ".FFFFFFFFFFFFFF.",
    ".FFFFfffffFFFFF.",
    "..FFfffffffFFF..",
    "..FFffNNNffFF...",
    "...FffffffFF....",
    "...FWfffffWF....",
    "....FFFFFFF.....",
    "................",
    "................"]

CAT_ICON = [
    "................",
    "..K........K....",
    "..KK......KK....",
    "..KPK....KPK....",
    "..KPPK..KPPK....",
    "..KKKKKKKKKK....",
    "..K........K....",
    "..K........K....",
    "................",
    "...........KK...",
    "............K...",
    "............K...",
    "...........KK...",
    "...KKKKKKKKK....",
    "................",
    "................"]

BAT_ICON = [
    "................",
    "................",
    "B.............B.",
    "BB...........BB.",
    "BbB...KK....BbB.",
    "BbbB.KKKK..BbbB.",
    "BbbbBKKKKKBbbbB.",
    "BbbbbBKKKBbbbbB.",
    "BbbbbbBKBbbbbbB.",
    "BbBbbBbKbBbbBbB.",
    "B.B.B..K..B.B.B.",
    "................",
    "................",
    "................",
    "................",
    "................"]


def items():
    return {
        "vampire_cape": icon(CAPE_ICON, {"B": BLACK[2], "R": RED[3], "r": RED[1]}),
        "mummy_wraps": icon(WRAPS_ICON, {"L": LINEN[3], "l": LINEN[1], "D": LINEN[0]}),
        "skeleton_suit": icon(SKELETON_ICON, {"K": BLACK[2], "W": BONE[2]}),
        "werewolf_mask": icon(WOLF_ICON, {"F": FUR[2], "f": FUR[3], "p": rgb("8a5a5a"), "Y": rgb("f0c020"), "N": rgb("141010"),
                                          "W": BONE[2]}),
        "cat_ears_and_tail": icon(CAT_ICON, {"K": rgb("3a3a48"), "P": PINK}),
        "bat_wings": icon(BAT_ICON, {"B": BAT[3], "b": BAT[1], "K": BAT[2]}),
    }


# ---------------------------------------------------------------- the costume trunk

def trunk_wood():
    c = Canvas()
    bs.planks(WOOD, 24101)(c)
    return c.img


def trunk_lid():
    """Wood with a purple label band across it, a gold star in the middle."""
    c = Canvas()
    base = trunk_wood()
    for y in range(16):
        for x in range(16):
            c.px(x, y, (PURPLE[1] if y in (5, 10) else PURPLE[2]) if 5 <= y <= 10 else base.getpixel((x, y))[:3])
    for x, y in ((7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (6, 9), (9, 9)):
        c.px(x, y, GOLD[3])
    return c.img


def trunk_brass():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GOLD[:3], 24102, [1, 3, 2])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        c.px(x, y, GOLD[3])
    return c.img


def trunk_inside():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PURPLE[:3], 24103, [1, 3, 2])
    return c.img


def trunk_clothes():
    """Costumes heaped up: folded bundles of cape, linen, fur, orange and green cloth, each lit along its upper left."""
    c = Canvas()
    colours = [BLACK[2], RED[2], LINEN[2], FUR[2], rgb("e07a1a"), rgb("3a9a3a"), PURPLE[3]]

    def tones(colour):
        return [tuple(int(v * 0.72) for v in colour), colour, tuple(min(255, int(v * 1.22) + 8) for v in colour)]
    bs.heap([tones(colour) for colour in colours], 24104, count=8, joint=rgb("141014"))(c)
    return c.img


def decor14_textures():
    """(kind, name) -> image for every texture of the fourteenth decorations batch."""
    out = {}
    for i, name in enumerate(OUTFITS):
        img, glow = worn(name, 24001 + i)
        out[("entity", f"costume/{name}")] = img
        if OUTFITS[name].get("glow"):
            out[("entity", f"costume/{name}_glow")] = glow
    for name, img in items().items():
        out[("item", name)] = img
    out.update({("block", "costume_trunk_wood"): trunk_wood(), ("block", "costume_trunk_lid"): trunk_lid(),
                ("block", "costume_trunk_brass"): trunk_brass(), ("block", "costume_trunk_inside"): trunk_inside(),
                ("block", "costume_trunk_clothes"): trunk_clothes()})
    return out
