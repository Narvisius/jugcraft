"""The art of Arms VII's variants (docs/features/arms-vii.md; tables in tools/arms_variants.py): a design each, drawn by
tools/arms_pixel.py as a pixel-art icon on the diagonal and a 3D model in the hand, as the other arms are
(tools/arms_art.py, whose parts these reuse). Each line has its own materials:

- gilded: polished steel, gold fittings, royal-blue velvet grips, sapphires;
- ironclad (dieselpunk): dark gun steel, olive-drab paint, hazard stripes, black rubber grips;
- bonecarved: bone and horn, leather and sinew, a garnet eye;
- runebound: void-dark steel and iron with runes that glow cyan (lit in the dark in the hand);
- the bosses' trophies: ice and white fur (the Yeti King), obsidian and magma (the Cinder Tyrant), blackthorn and
  venom (the Mire Hag), dark iron and soul fire (the Crypt Lich), gunmetal, copper coils and arcs (the Iron
  Dreadnought), silver and wolf fur (the Alpha Werewolf), storm steel, feathers and lightning (the Storm Roc), and
  sea-green bronze, pearl and a glowing tide line (the Abyssal Leviathan).

All original; the designs follow their kinds' proportions (tools/arms_art.py), so each is held as its kind is, and
are kept plain as the studied mods' are: one or two accents an arm, clean silhouettes, no dotted or scattered detail.
"""
import math

from PIL import Image, ImageDraw

import arms_art
import arms_pixel as px
from arms_art import blade, curved, gem, grip, guard, haft, pommel, rivets, sickle, socket
from arms_pixel import DARK, HIGHLIGHT, LIGHT, MID, OUT_DARK, Design, Material, Style

M = Material
# ---------------------------------------------------------------- materials
GOLD = M((70, 40, 0), (130, 82, 6), (178, 120, 14), (222, 170, 36), (250, 214, 80), (255, 246, 170))
POLISHED = M((26, 28, 34), (70, 76, 88), (120, 126, 140), (164, 170, 184), (204, 210, 222), (240, 244, 250))
ROYAL = M((12, 16, 48), (24, 34, 90), (34, 48, 128), (52, 72, 170), (84, 108, 210), (130, 156, 236), shine=False)
SAPPHIRE = M((8, 20, 60), (16, 40, 110), (28, 64, 170), (52, 104, 220), (110, 170, 250), (210, 236, 255))
GUNSTEEL = M((16, 18, 22), (44, 48, 56), (72, 78, 90), (102, 108, 120), (140, 146, 158), (184, 190, 202))
HAZARD = M((60, 44, 0), (120, 90, 6), (180, 140, 14), (226, 184, 30), (250, 216, 70), (255, 240, 150))
BONE = M((54, 46, 30), (104, 92, 66), (150, 136, 104), (196, 182, 146), (226, 214, 180), (246, 238, 214))
HORN = M((22, 14, 8), (46, 32, 20), (64, 46, 30), (90, 66, 44), (120, 90, 60), (150, 118, 84), shine=False)
VOIDSTEEL = M((8, 6, 16), (28, 24, 42), (42, 36, 60), (60, 52, 84), (84, 74, 114), (118, 106, 148))
VIOLET = M((18, 8, 28), (36, 18, 54), (52, 28, 76), (72, 42, 104), (98, 62, 138), (128, 92, 170), shine=False)
RUNE = M((6, 40, 60), (10, 80, 110), (20, 130, 170), (50, 190, 230), (130, 235, 255), (225, 252, 255), glow=True)
ICE = M((20, 56, 86), (48, 104, 146), (86, 154, 196), (136, 200, 232), (188, 232, 248), (240, 252, 255))
FUR = M((84, 90, 100), (136, 144, 154), (178, 184, 194), (212, 218, 226), (234, 238, 244), (250, 252, 255), shine=False)
OBSIDIAN = M((6, 3, 12), (22, 12, 32), (34, 20, 48), (50, 32, 68), (72, 50, 94), (102, 78, 128))
MAGMA = M((110, 24, 2), (170, 52, 6), (220, 92, 12), (250, 146, 30), (255, 204, 84), (255, 246, 196), glow=True)
THORN = M((14, 12, 10), (32, 28, 22), (46, 40, 30), (64, 56, 42), (86, 76, 58), (110, 98, 76), shine=False)
VENOM = M((16, 46, 8), (34, 92, 16), (62, 144, 26), (104, 196, 46), (166, 238, 90), (226, 255, 180), glow=True)
MOSS = M((18, 28, 14), (36, 54, 26), (52, 74, 38), (72, 98, 52), (98, 126, 70), (130, 156, 96), shine=False)
SOUL = M((4, 36, 42), (8, 74, 82), (18, 116, 124), (38, 168, 174), (98, 220, 222), (200, 255, 255), glow=True)
DARK_IRON = M((10, 10, 12), (28, 28, 32), (40, 40, 46), (56, 56, 64), (78, 78, 88), (108, 108, 120))
COPPER = M((50, 20, 10), (100, 44, 20), (140, 70, 34), (186, 104, 56), (222, 146, 92), (246, 196, 150))
ARC = M((10, 30, 70), (20, 60, 140), (40, 100, 210), (80, 160, 255), (150, 210, 255), (225, 245, 255), glow=True)
SILVER = M((40, 44, 52), (96, 102, 114), (150, 156, 168), (194, 200, 210), (226, 230, 238), (252, 252, 255))
WOLF = M((26, 20, 16), (50, 40, 32), (70, 56, 44), (94, 76, 60), (122, 100, 80), (150, 126, 104), shine=False)
MOONSTONE = M((40, 46, 70), (80, 90, 130), (130, 142, 180), (180, 192, 222), (220, 228, 246), (250, 252, 255), glow=True)
STORMSTEEL = M((14, 20, 32), (40, 52, 74), (66, 82, 108), (96, 114, 142), (134, 152, 180), (182, 198, 222))
FEATHER = M((70, 66, 60), (120, 114, 104), (160, 154, 142), (198, 192, 180), (226, 222, 212), (246, 244, 238), shine=False)
BOLT = M((90, 70, 0), (160, 126, 4), (220, 180, 10), (250, 220, 40), (255, 244, 120), (255, 255, 220), glow=True)
SEABRONZE = M((6, 30, 34), (12, 60, 66), (20, 90, 96), (36, 124, 128), (70, 166, 166), (140, 214, 206))
PEARL = M((90, 86, 100), (150, 146, 160), (196, 192, 206), (226, 222, 234), (242, 240, 248), (255, 255, 255))
TIDEGLOW = M((4, 40, 44), (8, 80, 84), (16, 126, 128), (40, 180, 176), (110, 230, 218), (210, 255, 248), glow=True)

# Style(blade, fitting, grip, haft, gem, accent, cloth)
GILDED = Style(POLISHED, GOLD, ROYAL, px.DARK_WOOD, SAPPHIRE, GOLD, ROYAL)
IRONCLAD = Style(GUNSTEEL, px.OLIVE, px.RUBBER, px.GUNMETAL, HAZARD, px.GUNMETAL, px.OLIVE)
BONECARVED = Style(BONE, HORN, px.LEATHER, BONE, px.GARNET, BONE, px.LEATHER)
RUNEBOUND = Style(VOIDSTEEL, DARK_IRON, VIOLET, px.DARK_WOOD, RUNE, RUNE, VIOLET)
YETI = Style(ICE, POLISHED, FUR, px.WOOD, ICE, BONE, FUR)
CINDER = Style(OBSIDIAN, OBSIDIAN, px.LEATHER, OBSIDIAN, MAGMA, MAGMA, px.CLOTH_RED)
HAG = Style(MOSS, THORN, MOSS, THORN, VENOM, VENOM, MOSS)
LICH = Style(DARK_IRON, BONE, VIOLET, DARK_IRON, SOUL, SOUL, VIOLET)
DREADNOUGHT = Style(GUNSTEEL, px.GUNMETAL, px.RUBBER, px.GUNMETAL, ARC, COPPER, px.OLIVE)
WEREWOLF = Style(SILVER, DARK_IRON, WOLF, px.DARK_WOOD, MOONSTONE, SILVER, WOLF)
ROC = Style(STORMSTEEL, px.GUNMETAL, px.LEATHER, px.DARK_WOOD, BOLT, BOLT, FEATHER)
LEVIATHAN = Style(SEABRONZE, SEABRONZE, px.LEATHER, px.DARK_WOOD, PEARL, PEARL, TIDEGLOW)
# Each line's materials, by its name in tools/arms_variants.py (a variant's 16x16 icon is coloured from them).
LINE_STYLES = {"gilded": GILDED, "ironclad": IRONCLAD, "bonecarved": BONECARVED, "runebound": RUNEBOUND,
               "yeti_king": YETI, "cinder_tyrant": CINDER, "mire_hag": HAG, "crypt_lich": LICH,
               "iron_dreadnought": DREADNOUGHT, "werewolf_alpha": WEREWOLF, "storm_roc": ROC,
               "abyssal_leviathan": LEVIATHAN}


# ---------------------------------------------------------------- shared parts


def rune_line(d, s0, s1, t_of, width=0.32, material=RUNE):
    """A glowing rune line inlaid along a path t_of(s), one clean stroke (dashes read as noise at this size)."""
    d.strip(s0, s1, lambda v: width - t_of(v), lambda v: width + t_of(v), material=material, depth=1.5, z=3, part="rune")


def hazard(d, s0, s1, w, depth, z=1, period=1.0):
    """A hazard-striped band from s0 to s1: yellow and black."""
    d.strip(s0, s1, w, material=HAZARD, depth=depth, z=z, stripes=(period, px.OUT_DARK))


def bolts(d, points, material=px.GUNMETAL):
    for s, t in points:
        d.disc(s, t, 0.6, material, depth=2.4, z=4, tone=LIGHT)


def fur_wrap(d, s0, s1, w, material=FUR):
    """A shaggy fur wrap: a strip with ragged tufts either side."""
    d.strip(s0, s1, w, material=material, depth=w * 2.2, z=1, part="fur")
    s = s0 + 0.6
    side = 1
    while s < s1 - 0.4:
        d.poly([(s - 0.6, side * w), (s, side * (w + 0.9)), (s + 0.5, side * w)], material, depth=w * 2.0, z=1, part="fur")
        s += 1.3
        side = -side


def teeth(d, s0, s1, t, side, size=0.9, step=1.6, material=BONE):
    """A row of teeth along s at t, pointing to `side`."""
    s = s0
    while s + step * 0.6 <= s1:
        d.poly([(s, t), (s + step * 0.5, t + side * size), (s + step * 0.9, t)], material, depth=1.2, z=1)
        s += step


def skull(d, s, t, r, material=BONE, depth=3.0, z=2):
    """A little skull facing out: dome, jaw, dark eye sockets."""
    d.disc(s + 0.2 * r, t, r, material, depth=depth, z=z, part=f"skull{s}")
    d.strip(s - r * 0.9, s - r * 0.2, r * 0.55, material=material, depth=depth * 0.9, z=z, part=f"skull{s}")
    for side in (-1, 1):
        d.disc(s + 0.1 * r, t + side * r * 0.42, r * 0.32, material, depth=depth + 0.2, z=z + 1, tone=OUT_DARK)
    d.strip(s - r * 0.55, s - r * 0.25, r * 0.12, material=material, depth=depth + 0.2, z=z + 1, tone=OUT_DARK)   # the nose


def feathers(d, s, t, side, n=3, length=4.0, material=FEATHER):
    """A bunch of feathers tied at (s, t), hanging back and out to `side`."""
    for i in range(n):
        a = math.radians(200 + i * 18) if side > 0 else math.radians(160 - i * 18)
        tip = (s + math.cos(a) * length, t + side * abs(math.sin(a)) * length * 0.7 + side * i * 0.4)
        d.poly([(s, t - 0.3), (s + (tip[0] - s) * 0.5, t + (tip[1] - t) * 0.5 - 0.6), tip,
                (s + (tip[0] - s) * 0.5, t + (tip[1] - t) * 0.5 + 0.6), (s, t + 0.3)], material, depth=0.8, part=f"feather{i}")


# ---------------------------------------------------------------- gilded


def gilded_longsword():
    st = GILDED
    d = Design(36, grip=5.5)
    pommel(d, 1.7, 1.7, st)
    gem(d, 1.7, 0.0, 0.7, st)
    grip(d, 2.6, 8.6, 0.95, st)
    guard(d, 9.3, 5.8, st, curl=1.6)
    for side in (1, -1):
        d.disc(9.3, side * 6.6, 0.8, GOLD, depth=2.6, z=1)
    blade(d, 10.0, 31.5, 1.6, 1.25, st, tip=4.0, fuller=(0.04, 0.72))
    d.strip(10.6, 16.0, 0.35, material=GOLD, depth=1.4, z=2)   # gold inlay in the fuller
    gem(d, 9.3, 0.0, 0.8, st)
    return d


def gilded_rapier():
    st = GILDED
    d = Design(34, grip=5.0)
    pommel(d, 1.6, 1.5, st)
    grip(d, 2.4, 7.8, 0.85, st, period=1.6)
    # The rapier's swept hilt in gold: quillons and a knuckle bow clear of the grip.
    guard(d, 8.5, 3.6, st, thick=1.0, curl=1.0)
    d.line(8.2, -3.8, 2.8, -3.8, 0.8, GOLD, depth=1.6)
    d.line(2.8, -3.8, 1.6, -1.2, 0.8, GOLD, depth=1.6)
    blade(d, 9.0, 31.0, 0.75, 0.55, st, tip=3.0, ridge=True)
    gem(d, 8.5, 0.0, 0.6, st)
    return d


def gilded_sabre():
    st = GILDED
    d = Design(34, grip=4.8)
    pommel(d, 1.4, 1.3, st, cap=False)
    grip(d, 2.2, 7.6, 0.85, st, period=1.6)
    d.strip(7.8, 8.8, 4.0, 1.8, material=GOLD, depth=2.2)
    d.line(8.3, -3.6, 2.6, -3.6, 0.9, GOLD, depth=1.6)
    d.line(2.6, -3.6, 1.4, -1.0, 0.9, GOLD, depth=1.6)
    curved(d, 8.8, 30.5, 1.2, lambda s: 0.006 * (s - 8.8) ** 2, st, tip=3.2)
    # A gold line etched along the back of the blade.
    d.strip(10.0, 24.0, lambda s: -0.75 + 0.006 * (s - 8.8) ** 2 + 0.2, lambda s: 0.95 - 0.006 * (s - 8.8) ** 2,
            material=GOLD, depth=1.4, z=2)
    gem(d, 8.3, 0.0, 0.55, st)
    return d


def gilded_halberd():
    st = GILDED
    d = Design(59, grip=13.0)
    d.disc(0.9, 0.0, 1.2, GOLD, depth=2.2)
    haft(d, 0.8, 44.0, 0.8, st, rings=(24.0,))
    grip(d, 8.0, 18.0, 0.95, st)
    socket(d, 37.0, 47.0, 1.2, st)
    d.poly([(46.0, -1.2), (59.0, 0.0), (46.0, 1.2)], st.blade, depth=1.4, bevel=0.0)
    d.glint(55.0, -0.3)
    # The halberd's head in polished steel, its hook in gold, a sapphire in the socket.
    d.poly(arms_art.HALBERD_AXE, st.blade, depth=1.1, part="axe")
    d.poly(arms_art.HALBERD_EDGE, st.blade, depth=1.1, z=1, tone=HIGHLIGHT)
    d.poly(arms_art.HALBERD_HOOK, GOLD, depth=1.1, part="hook")
    gem(d, 42.0, 0.0, 0.8, st)
    return d


# ---------------------------------------------------------------- ironclad (dieselpunk)


def ironclad_zweihander():
    st = IRONCLAD
    d = Design(59, grip=9.0)
    d.strip(0.4, 3.4, 1.7, material=px.GUNMETAL, depth=3.0)   # a hex-bolt pommel
    bolts(d, [(1.9, 0.0)], HAZARD)
    grip(d, 3.4, 15.5, 1.1, st)
    d.strip(15.5, 17.3, 8.4, material=px.GUNMETAL, depth=3.0, part="guard")   # a square, heavy crossguard
    hazard(d, 15.6, 17.2, 8.2, 3.2, period=0.9)
    d.strip(17.3, 23.0, 1.9, material=px.OLIVE, depth=1.8, part="ricasso")
    blade(d, 23.0, 54.0, 2.5, 2.0, st, tip=5.0)
    # A painted reinforcing spine down the back half.
    d.strip(23.0, 46.0, 2.5, -1.2, material=px.OLIVE, depth=1.8, z=1, part="spine")
    return d


def ironclad_maul():
    st = IRONCLAD
    d = Design(48, grip=8.0)
    d.disc(1.0, 0.0, 1.3, px.GUNMETAL, depth=2.6)
    d.strip(1.0, 37.0, 0.9, material=px.GUNMETAL, depth=1.9)   # a steel-tube haft
    for s in (16.0, 22.0, 28.0):
        d.strip(s - 0.4, s + 0.4, 1.15, material=px.GUNMETAL, depth=2.3, z=1)
    grip(d, 2.0, 13.0, 1.15, st)
    socket(d, 33.0, 37.5, 1.5, st)
    # An engine-block head: painted olive, steel striking plates at both faces, a hazard band.
    d.strip(37.0, 47.5, 6.6, material=px.OLIVE, depth=7.0, part="head")
    for s0, s1 in ((37.0, 38.4), (46.1, 47.5)):
        d.strip(s0, s1, 7.0, material=GUNSTEEL, depth=7.4, z=1, part=f"plate{s0}")
    hazard(d, 41.4, 43.2, 6.7, 7.2, period=1.0)
    return d


def ironclad_war_pick():
    st = IRONCLAD
    d = Design(31, grip=5.0)
    d.disc(0.9, 0.0, 1.1, px.GUNMETAL, depth=2.2)
    d.strip(0.8, 26.0, 0.85, material=px.GUNMETAL, depth=1.8)
    grip(d, 1.6, 9.0, 1.05, st, period=1.6)
    socket(d, 21.0, 27.5, 1.25, st)
    d.poly([(24.6, 1.25), (26.8, 1.25), (26.0, 6.2), (22.4, 11.0), (21.0, 10.4), (23.2, 5.6)], GUNSTEEL, depth=1.8,
           part="beak")
    d.strip(22.6, 27.0, 4.4, -1.25, material=px.OLIVE, depth=3.6, part="face")   # a painted hammer block
    hazard(d, 23.4, 24.6, 4.5, 3.8, period=0.8)
    d.poly([(27.5, -0.9), (31.0, 0.0), (27.5, 0.9)], GUNSTEEL, depth=1.6)
    return d


def ironclad_battle_axe():
    st = IRONCLAD
    d = Design(42, grip=7.5)
    d.disc(1.0, 0.0, 1.3, px.GUNMETAL, depth=2.4)
    d.strip(1.0, 41.0, 0.85, material=px.GUNMETAL, depth=1.8)
    grip(d, 1.8, 12.0, 1.05, st)
    socket(d, 31.0, 39.5, 1.3, st)
    hazard(d, 29.0, 31.0, 1.2, 2.8, period=0.7)
    # A squared cleaver bit, painted, its edge ground bright.
    d.poly([(39.5, 1.3), (41.5, 3.0), (41.5, 11.0), (29.0, 11.0), (29.0, 8.0), (31.0, 1.3)], px.OLIVE, depth=1.3,
           part="bit")
    d.strip(29.0, 41.5, -9.4, 11.0, material=GUNSTEEL, depth=1.2, z=1, part="edge")
    d.strip(29.0, 41.5, -10.4, 11.0, material=GUNSTEEL, depth=1.2, z=2, tone=HIGHLIGHT, part="edge")
    d.poly([(37.0, -1.3), (39.5, -4.8), (35.5, -4.0), (33.5, -1.3)], GUNSTEEL, depth=1.2)
    return d


# ---------------------------------------------------------------- bonecarved


def bonecarved_dagger():
    st = BONECARVED
    d = Design(24, grip=3.8)
    d.disc(1.2, 0.0, 1.3, BONE, depth=2.4)          # a knuckle-bone pommel
    d.disc(1.2, -0.7, 0.5, BONE, depth=2.6, z=1, tone=OUT_DARK)
    grip(d, 2.0, 6.0, 0.85, st, period=1.2)
    d.poly([(6.0, -3.2), (7.4, -2.6), (7.4, 2.6), (6.0, 3.2), (6.6, 0.0)], HORN, depth=2.4, part="guard")
    # A fang: a broad bone blade curving to a point, its edge serrated.
    d.poly([(7.4, -1.5), (14.0, -1.6), (19.0, -1.0), (22.5, 0.6), (18.0, 1.2), (12.0, 1.6), (7.4, 1.5)], BONE, depth=1.3,
           part="blade", bevel=lambda s: -0.2 + 0.03 * (s - 7.4))
    teeth(d, 8.4, 17.0, 1.5, 1, size=0.6, step=1.4)
    d.glint(19.5, -0.4)
    return d


def bonecarved_flail_handle(d=None):
    """The Bonecarved Flail's handle alone: the bone haft and its knobbed joints, the grip, the horn collar and a horn
    eye the spine hangs from (tools/arms_heads.py VARIANT_HEADS draws the spine and skull swinging, in the hand)."""
    st = BONECARVED
    d = d or Design(34, grip=5.5)
    d.disc(1.0, 0.0, 1.2, HORN, depth=2.4)
    d.strip(1.0, 14.0, 0.9, material=BONE, depth=1.9)
    for s in (5.0, 10.0):
        d.strip(s - 0.5, s + 0.5, 1.15, material=BONE, depth=2.3, z=1)
    grip(d, 1.8, 9.8, 1.05, st)
    d.strip(13.5, 15.5, 1.1, material=HORN, depth=2.6)
    d.ring(arms_art.FLAIL_EYE, 0.0, 1.1, 0.45, HORN, depth=1.2, part="eye")
    return d


def bonecarved_flail():
    st = BONECARVED
    d = Design(34, grip=5.5)
    d.disc(1.0, 0.0, 1.2, HORN, depth=2.4)
    d.strip(1.0, 14.0, 0.9, material=BONE, depth=1.9)   # a bone haft, its joints knobbed
    for s in (5.0, 10.0):
        d.strip(s - 0.5, s + 0.5, 1.15, material=BONE, depth=2.3, z=1)
    grip(d, 1.8, 9.8, 1.05, st)
    d.strip(13.5, 15.5, 1.1, material=HORN, depth=2.6)
    # A chain of vertebrae to a skull.
    for i, (s, t) in enumerate([(16.2, 0.6), (17.8, 1.7), (19.3, 2.8), (20.8, 3.7), (22.3, 4.2)]):
        d.disc(s, t, 0.85, BONE, depth=1.6 if i % 2 else 2.2, part=f"vertebra{i}")
        d.disc(s, t, 0.3, BONE, depth=2.4, z=1, tone=OUT_DARK)
    for a in (20, 70, 120):
        r = math.radians(a)
        d.poly([(26.6 + math.cos(r) * 2.6 - math.sin(r) * 0.6, 4.4 + math.sin(r) * 2.6 + math.cos(r) * 0.6),
                (26.6 + math.cos(r) * 5.0, 4.4 + math.sin(r) * 5.0),
                (26.6 + math.cos(r) * 2.6 + math.sin(r) * 0.6, 4.4 + math.sin(r) * 2.6 - math.cos(r) * 0.6)],
               HORN, depth=1.6, part="horn")
    skull(d, 26.4, 4.4, 3.9, depth=6.0, z=1)
    return d


def bonecarved_glaive():
    st = BONECARVED
    d = Design(56, grip=12.0)
    d.disc(0.9, 0.0, 1.2, BONE, depth=2.2)
    d.strip(0.8, 41.0, 0.8, material=BONE, depth=1.7)   # a long bone haft, jointed
    for s in (14.0, 24.0, 33.0):
        d.disc(s, 0.0, 1.15, BONE, depth=2.3, z=1)
    grip(d, 7.0, 17.0, 0.95, st)
    d.strip(37.0, 41.0, 1.2, material=HORN, depth=2.4)
    # A jawbone blade: a long bone crescent, teeth along its edge.
    d.poly([(40.5, -1.2), (41.0, 1.2), (45.0, 2.8), (50.0, 3.0), (54.0, 1.8), (56.0, -0.3), (52.0, -1.3), (46.0, -1.5)],
           BONE, depth=1.2, part="blade", bevel=lambda s: 0.6)
    teeth(d, 41.5, 52.0, 2.4, 1, size=1.0, step=1.8, material=BONE)
    d.disc(43.0, -0.2, 0.5, px.GARNET, depth=1.8, z=2)
    d.glint(52.0, 0.4)
    return d


def bonecarved_labrys():
    st = BONECARVED
    d = Design(45, grip=7.5)
    d.disc(1.0, 0.0, 1.3, HORN, depth=2.4)
    d.strip(1.0, 42.0, 0.9, material=BONE, depth=1.9)
    for s in (16.0, 24.0):
        d.disc(s, 0.0, 1.2, BONE, depth=2.4, z=1)
    grip(d, 1.8, 13.0, 1.1, st)
    socket(d, 32.0, 42.0, 1.35, st)
    # Two shoulder blades for bits.
    for side in (1, -1):
        d.poly([(33.0, side * 1.3), (30.0, side * 5.0), (28.5, side * 10.0), (33.0, side * 11.6), (38.0, side * 12.0),
                (43.0, side * 10.6), (44.0, side * 6.0), (40.0, side * 1.3)], BONE, depth=1.3, part=f"bit{side}",
               bevel=side * 7.0)
        d.line(33.0, side * 3.0, 39.0, side * 10.0, 0.6, BONE, depth=1.3, z=1, tone=DARK)   # the blade's spine ridge
    skull(d, 37.0, 0.0, 2.0, depth=3.6, z=2)
    return d


# ---------------------------------------------------------------- runebound


def runebound_nodachi():
    st = RUNEBOUND
    d = Design(56, grip=9.0)
    d.strip(0.0, 1.4, 1.1, material=DARK_IRON, depth=2.4)
    grip(d, 1.4, 16.0, 1.05, st, period=1.8)
    d.disc(16.8, 0.0, 3.2, DARK_IRON, depth=2.4)
    d.ring(16.8, 0.0, 2.6, 1.8, RUNE, depth=2.6, z=1)   # a glowing ring in the tsuba
    d.strip(17.6, 18.8, 1.4, material=DARK_IRON, depth=2.2, z=1)
    curved(d, 18.8, 52.5, 1.5, lambda s: 0.0016 * (s - 18.8) ** 2, st, tip=3.4)
    rune_line(d, 21.0, 46.0, lambda s: -0.5 + 0.0016 * (s - 18.8) ** 2, width=0.45)
    gem(d, 16.8, 0.0, 0.8, st)
    return d


def runebound_moonblade():
    st = RUNEBOUND
    d = Design(50, grip=7.0)
    pommel(d, 1.7, 1.6, st)
    grip(d, 2.8, 12.0, 1.05, st)
    guard(d, 12.8, 4.2, st, curl=1.4)
    d.poly([(13.6, -1.6), (30.0, -2.4), (44.0, -1.8), (50.0, 0.0), (46.0, 2.2), (38.0, 5.4), (28.0, 6.4), (19.0, 4.8),
            (13.6, 1.8)], st.blade, depth=1.1, part="blade", bevel=lambda s: 1.2)
    # A crescent of runes inside the edge, and a glowing heart at the guard.
    rune_line(d, 18.0, 44.0, lambda s: 3.9 - ((s - 31.0) / 13.0) ** 2 * 3.4, width=0.45)
    gem(d, 12.8, 0.0, 1.0, st)
    return d


def runebound_staff():
    st = RUNEBOUND
    d = Design(50, grip=25.0)
    d.strip(0.0, 50.0, 1.0, material=px.DARK_WOOD, depth=2.0)
    # Crystal ends in iron claws, rune bands along the staff.
    for s0, s1, tip in ((0.0, 3.0, -1.0), (47.0, 50.0, 1.0)):
        d.strip(s0, s1, 1.35, material=DARK_IRON, depth=2.6, z=1)
        c = s0 if tip < 0 else s1
        d.disc(c - tip * 0.4, 0.0, 1.0, RUNE, depth=2.2, z=2)
    for s in (6.0, 12.0, 38.0, 44.0):
        d.strip(s - 0.5, s + 0.5, 1.15, material=RUNE, depth=2.4, z=1, part=f"band{s}")
    grip(d, 20.0, 30.0, 1.15, st)
    return d


def runebound_war_hammer():
    st = RUNEBOUND
    d = Design(38, grip=6.0)
    d.disc(1.0, 0.0, 1.3, DARK_IRON, depth=2.4)
    haft(d, 1.0, 32.0, 0.85, st, rings=(16.0,))
    grip(d, 1.8, 10.5, 1.0, st)
    socket(d, 25.0, 32.0, 1.25, st)
    # The war hammer's head in void steel, a glowing rune diamond on its face.
    d.poly([(27.0, 1.25), (33.5, 1.25), (34.0, 6.6), (26.5, 6.6)], st.blade, depth=4.2, part="face")
    d.poly([(30.2, 2.4), (32.0, 3.9), (30.2, 5.4), (28.4, 3.9)], RUNE, depth=4.6, z=2, part="sigil")
    d.poly([(28.0, -1.25), (32.5, -1.25), (29.6, -7.0)], st.blade, depth=2.0, part="beak")
    d.poly([(32.0, -1.0), (37.5, 0.0), (32.0, 1.0)], st.blade, depth=1.8)
    return d


# ---------------------------------------------------------------- the Yeti King


def glacier_maul():
    st = YETI
    d = Design(48, grip=8.0)
    d.disc(1.0, 0.0, 1.4, BONE, depth=2.6)
    haft(d, 1.0, 37.0, 0.95, st, rings=())
    fur_wrap(d, 2.0, 13.0, 1.1)
    d.strip(31.0, 37.0, 1.3, material=BONE, depth=2.6, z=1, stripes=(1.5, DARK))   # bound with tusk and thong
    # A great chunk of glacier ice, faceted, with icicle spikes.
    d.poly([(36.0, -4.0), (38.5, -6.8), (44.0, -7.2), (48.0, -4.6), (48.0, 4.8), (44.5, 7.0), (39.0, 6.8), (36.0, 3.8)],
           ICE, depth=6.6, part="ice", bevel=lambda s: 0.0)
    for s, t in ((40.0, -7.0), (45.0, -7.0), (42.0, 6.9)):
        side = 1 if t > 0 else -1
        d.poly([(s - 1.0, t), (s, t + side * 2.6), (s + 1.0, t)], ICE, depth=2.0, part=f"icicle{s}")
    d.line(39.0, -3.0, 45.0, 2.5, 0.5, ICE, depth=7.0, z=1, tone=HIGHLIGHT)   # a bright fracture
    d.glint(44.0, -4.5)
    return d


def rimeclaw():
    st = YETI
    d = Design(24, grip=4.0)
    d.poly([(0.0, -4.2), (8.0, -4.2), (8.0, -2.8), (0.0, -2.8)], POLISHED, depth=1.8, part="frame")
    d.poly([(0.0, 2.8), (8.0, 2.8), (8.0, 4.2), (0.0, 4.2)], POLISHED, depth=1.8, part="frame")
    d.strip(3.2, 5.0, 2.8, material=FUR, depth=2.2, part="fur")
    d.strip(7.6, 9.2, 4.2, material=POLISHED, depth=2.4, part="top")
    # Three ice claws, the middle one longest, each curving to its point.
    for t, top in ((-2.8, 19.0), (0.0, 24.0), (2.8, 19.0)):
        d.poly([(9.2, t - 0.9), (top - 2.0, t - 0.4 + t * 0.12), (top, t * 1.15), (top - 3.0, t + 0.7 + t * 0.1),
                (9.2, t + 0.9)], ICE, depth=1.3, part=f"claw{t}", bevel=t)
    d.glint(21.0, -0.3)
    return d


# ---------------------------------------------------------------- the Cinder Tyrant


def cinderbrand():
    st = CINDER
    d = Design(48, grip=7.0)
    d.disc(1.8, 0.0, 1.9, OBSIDIAN, depth=2.6)
    d.disc(1.8, 0.0, 0.9, MAGMA, depth=2.8, z=1)
    grip(d, 3.0, 12.5, 1.05, st)
    # A guard of flames licking up the blade.
    for side in (1, -1):
        d.poly([(12.5, side * 1.2), (12.8, side * 7.0), (16.0, side * 5.6), (15.0, side * 3.6), (18.0, side * 2.6),
                (14.4, side * 1.2)], OBSIDIAN, depth=2.6, part=f"guard{side}")
        d.poly([(13.0, side * 2.0), (13.2, side * 5.6), (15.2, side * 4.2)], MAGMA, depth=2.8, z=1)
    blade(d, 14.4, 43.0, 2.3, 1.8, st, tip=4.6)
    # A molten core down the middle, as a fuller.
    d.strip(15.0, 42.0, 0.55, material=MAGMA, depth=1.6, z=2, part="core")
    return d


def magmaw():
    st = CINDER
    d = Design(50, grip=8.0)
    d.disc(1.0, 0.0, 1.4, OBSIDIAN, depth=2.6)
    haft(d, 1.0, 38.0, 1.0, st, rings=(20.0,))
    grip(d, 2.0, 13.5, 1.15, st)
    socket(d, 34.0, 38.0, 1.5, st)
    # A drum of obsidian split by a glowing seam of magma.
    d.strip(38.0, 49.5, 6.0, material=OBSIDIAN, depth=6.6, part="head")
    d.strip(42.5, 45.0, 6.2, material=MAGMA, depth=6.8, z=1, part="maw")
    for s in (38.0, 49.5):
        d.strip(s - 0.8, s + 0.8, 7.2, material=OBSIDIAN, depth=7.4, z=1, part=f"flange{s}")
    return d


# ---------------------------------------------------------------- the Mire Hag


def hagthorn():
    st = HAG
    d = Design(53, grip=10.0)
    d.disc(0.9, 0.0, 1.2, THORN, depth=2.2)
    # A gnarled blackthorn snath: kinked, with thorns.
    d.strip(0.8, 50.0, lambda s: 0.8 + 0.25 * math.sin(s * 0.9) - 0.15 * math.sin(s * 0.37),
            lambda s: 0.8 - 0.25 * math.sin(s * 0.9) + 0.15 * math.sin(s * 0.37), material=THORN, depth=1.8)
    for s, side in ((18.0, 1), (27.0, -1), (35.0, 1), (43.0, -1)):
        d.poly([(s - 0.6, side * 0.8), (s + 0.8, side * 2.4), (s + 0.6, side * 0.8)], THORN, depth=1.4)
    grip(d, 6.0, 14.0, 0.95, st)
    d.line(25.0, -0.8, 25.0, -3.6, 1.1, THORN, depth=1.8)
    d.strip(47.5, 50.5, 1.2, material=MOSS, depth=2.4, stripes=(1.0, DARK))   # bound with moss and rag
    sickle(d, 40.0, -1.0, 11.5, 4.2, 0.0, -112.0, Style(VENOM, THORN, MOSS, THORN, VENOM, VENOM), steps=16)
    return d


def bogfang():
    st = HAG
    d = Design(27, grip=4.5)
    d.disc(0.8, 0.0, 1.0, THORN, depth=2.0)
    d.strip(0.8, 18.0, 0.85, material=THORN, depth=1.7)
    grip(d, 1.5, 9.5, 0.95, st, period=1.6)
    d.strip(16.5, 19.0, 1.1, material=THORN, depth=2.4)
    sickle(d, 12.0, 0.6, 7.6, 3.0, 0.0, 125.0, Style(VENOM, THORN, MOSS, THORN, VENOM, VENOM), steps=12)
    # Serrations on the inner edge.
    for a in range(20, 110, 18):
        r = math.radians(a)
        s, t = 12.0 + math.cos(r) * 4.4, 0.6 + math.sin(r) * 4.4
        d.poly([(s, t), (s - math.cos(r) * 0.9 + 0.3, t - math.sin(r) * 0.9), (s + math.sin(r) * 0.9, t - math.cos(r) * 0.9)],
               VENOM, depth=1.0, z=2)
    d.disc(17.8, 0.0, 0.7, VENOM, depth=2.4, z=2)
    return d


# ---------------------------------------------------------------- the Crypt Lich


def soulreaver():
    st = LICH
    d = Design(50, grip=7.0)
    d.disc(1.7, 0.0, 1.6, BONE, depth=2.4)
    grip(d, 2.8, 12.0, 1.05, st)
    # A guard of ribs, and a skull at its heart.
    for side in (1, -1):
        d.line(12.6, side * 1.0, 14.6, side * 4.6, 0.7, BONE, depth=2.2)
        d.line(12.2, side * 1.0, 13.4, side * 3.4, 0.6, BONE, depth=2.0)
    d.poly([(13.6, -1.6), (30.0, -2.4), (44.0, -1.8), (50.0, 0.0), (46.0, 2.2), (38.0, 5.4), (28.0, 6.4), (19.0, 4.8),
            (13.6, 1.8)], DARK_IRON, depth=1.1, part="blade", bevel=lambda s: 1.2)
    # Soul fire along the edge.
    d.poly([(46.0, 2.2), (38.0, 5.4), (28.0, 6.4), (19.0, 4.8), (20.0, 4.0), (28.0, 5.5), (38.0, 4.5), (45.0, 1.5)],
           SOUL, depth=1.3, z=1, part="soul")
    skull(d, 12.8, 0.0, 1.6, depth=3.2, z=2)
    return d


def gravewarden():
    st = LICH
    d = Design(53, grip=8.0)
    d.disc(1.8, 0.0, 1.8, BONE, depth=2.6)
    d.strip(3.0, 13.5, 1.1, material=BONE, depth=2.2, stripes=(1.4, DARK))   # a grip of finger bones
    d.strip(13.5, 15.4, 4.8, material=BONE, depth=2.8)
    # A broad slab of a blade, a headstone's round top, with soul fire in its three holes.
    d.strip(15.4, 48.5, 3.6, material=DARK_IRON, depth=1.2, part="blade", bevel=0.0)
    d.poly([(48.5, -3.6), (51.0, -2.6), (52.5, 0.0), (51.0, 2.6), (48.5, 3.6)], DARK_IRON, depth=1.2, part="blade",
           bevel=0.0)
    for s in (21.0, 29.0, 37.0):
        d.disc(s, 0.0, 0.9, SOUL, depth=1.6, z=2, part=f"hole{s}")
    d.strip(17.0, 46.0, 3.6, -3.0, material=DARK_IRON, depth=1.2, z=1, tone=HIGHLIGHT)
    return d


# ---------------------------------------------------------------- the Iron Dreadnought


def dynamo_halberd():
    st = DREADNOUGHT
    d = Design(59, grip=13.0)
    d.disc(0.9, 0.0, 1.2, px.GUNMETAL, depth=2.2)
    d.strip(0.8, 44.0, 0.85, material=px.GUNMETAL, depth=1.8)
    grip(d, 8.0, 18.0, 1.0, st)
    d.strip(30.0, 36.5, 1.3, material=COPPER, depth=2.6, z=1, stripes=(0.7, DARK))   # a copper coil below the head
    socket(d, 37.0, 47.0, 1.25, st)
    d.poly([(46.0, -1.2), (59.0, 0.0), (46.0, 1.2)], GUNSTEEL, depth=1.4)
    # The halberd's head in gun steel, its edge charged.
    d.poly(arms_art.HALBERD_AXE, GUNSTEEL, depth=1.2, part="axe")
    d.poly(arms_art.HALBERD_EDGE, ARC, depth=1.3, z=1, part="charge")
    d.poly(arms_art.HALBERD_HOOK, GUNSTEEL, depth=1.1, part="hook")
    return d


def piston_hammer():
    st = DREADNOUGHT
    d = Design(38, grip=6.0)
    d.disc(1.0, 0.0, 1.3, px.GUNMETAL, depth=2.4)
    d.strip(1.0, 32.0, 0.85, material=px.GUNMETAL, depth=1.8)
    grip(d, 1.8, 10.5, 1.05, st)
    d.strip(14.0, 22.0, 1.15, material=COPPER, depth=2.4, z=1, stripes=(0.8, DARK))   # a coiled cable
    socket(d, 25.0, 32.0, 1.25, st)
    # A piston cylinder for a head, its rod the striking face, a vent glowing with charge.
    d.strip(26.0, 35.0, 2.2, 6.8, material=GUNSTEEL, depth=4.6, part="cylinder")
    for s in (27.0, 34.0):
        d.strip(s - 0.4, s + 0.4, 2.3, 7.0, material=COPPER, depth=5.0, z=1, part=f"ring{s}")
    d.strip(28.0, 33.0, -6.8, 8.6, material=POLISHED, depth=3.4, part="rod")   # the face, out to the right
    d.strip(30.0, 31.0, -2.6, 5.4, material=ARC, depth=4.8, z=2, part="vent")
    d.poly([(28.5, -2.2), (33.0, -2.2), (31.0, -5.8), (28.0, -7.0)], GUNSTEEL, depth=2.0, part="beak")
    return d


# ---------------------------------------------------------------- the Alpha Werewolf


def moonfang():
    st = WEREWOLF
    d = Design(34, grip=4.8)
    d.disc(1.4, 0.0, 1.4, MOONSTONE, depth=2.4)   # a moonstone pommel
    fur_wrap(d, 2.2, 7.6, 0.85, WOLF)
    d.strip(7.8, 8.8, 2.4, 2.8, material=DARK_IRON, depth=2.2)
    # A claw guard over the knuckles.
    for i, t in enumerate((-1.2, -2.2, -3.0)):
        d.poly([(8.4, t), (6.0 - i * 0.6, t - 1.2), (5.6 - i * 0.6, t - 0.6)], BONE, depth=1.6)
    curved(d, 8.8, 30.5, 1.25, lambda s: 0.009 * (s - 8.8) ** 2, st, tip=3.6)
    teeth(d, 10.0, 18.0, -1.2, -1, size=0.6, step=1.6, material=SILVER)   # a notched back
    return d


def howler():
    st = WEREWOLF
    d = Design(48, grip=24.0)
    fur_wrap(d, 18.0, 30.0, 1.1, WOLF)
    for sign in (1, -1):
        g = 24.0 + sign * 6.0
        d.strip(min(g, g + sign * 1.4), max(g, g + sign * 1.4), 3.4, material=DARK_IRON, depth=2.6, part=f"guard{sign}")
        b0, b1 = g + sign * 1.4, 24.0 + sign * 21.0
        # Each blade a long claw, curving the opposite way to the other.
        def width(s, b0=b0, b1=b1):
            return 1.5 - 0.6 * abs(s - b0) / abs(b1 - b0)
        bend = (lambda s, b0=b0, sign=sign: sign * 0.004 * (s - b0) ** 2)
        d.strip(min(b0, b1), max(b0, b1), lambda s, w=width, b=bend: w(s) + b(s), lambda s, w=width, b=bend: w(s) - b(s),
                material=SILVER, depth=1.1, part=f"blade{sign}", bevel=lambda s, b=bend: -b(s))
        e = bend(b1)
        d.poly([(b1, -0.9 - e), (b1 + sign * 3.0, -sign * 1.4 - e), (b1, 0.9 - e)], SILVER, depth=1.1, part=f"blade{sign}")
    gem(d, 24.0, 0.0, 0.9, st)
    return d


# ---------------------------------------------------------------- the Storm Roc


def stormcaller():
    st = ROC
    d = Design(56, grip=12.0)
    d.disc(0.9, 0.0, 1.1, px.GUNMETAL, depth=2.2)
    haft(d, 0.8, 41.0, 0.8, st, rings=(22.0,))
    grip(d, 7.0, 17.0, 0.95, st)
    socket(d, 37.0, 41.0, 1.15, st)
    feathers(d, 36.6, -0.8, -1, n=3, length=4.4)
    # A lightning-bolt blade: a zigzag edge, glowing at its core.
    d.poly([(40.5, -1.4), (41.0, 1.4), (45.0, 4.4), (47.0, 2.2), (51.0, 4.6), (55.0, 1.8), (57.0, -0.4), (52.0, -1.6),
            (48.4, -0.6), (46.0, -2.2)], STORMSTEEL, depth=1.1, part="blade", bevel=lambda s: 0.8)
    d.line(41.6, 0.4, 45.0, 2.6, 0.7, BOLT, depth=1.6, z=2)
    d.line(45.0, 2.6, 47.4, 0.6, 0.7, BOLT, depth=1.6, z=2)
    d.line(47.4, 0.6, 51.0, 2.8, 0.7, BOLT, depth=1.6, z=2)
    d.line(51.0, 2.8, 55.6, 0.4, 0.7, BOLT, depth=1.6, z=2)
    return d


def galefeather():
    st = ROC
    d = Design(38, grip=6.0)
    pommel(d, 1.6, 1.5, st)
    grip(d, 2.5, 9.6, 0.9, st)
    # A guard of swept wings.
    for side in (1, -1):
        d.poly([(9.6, side * 0.8), (9.8, side * 3.0), (8.6, side * 6.4), (10.6, side * 5.6), (11.8, side * 3.2),
                (11.0, side * 0.8)], FEATHER, depth=2.0, part=f"wing{side}")
        d.line(9.8, side * 1.4, 9.4, side * 5.4, 0.3, FEATHER, depth=2.2, z=1, tone=DARK)
    d.disc(10.3, 0.0, 0.9, BOLT, depth=3.0, z=3)
    blade(d, 11.0, 32.0, 1.0, 0.8, st, tip=5.5, ridge=True)
    return d


# ---------------------------------------------------------------- the Abyssal Leviathan


def tidebreaker():
    st = LEVIATHAN
    d = Design(62, grip=14.0)
    d.disc(0.9, 0.0, 1.1, SEABRONZE, depth=2.2)
    haft(d, 0.8, 49.0, 0.8, st, rings=(26.0, 40.0))
    grip(d, 8.5, 19.0, 0.95, st)
    socket(d, 45.0, 50.0, 1.25, st)
    # A trident: three barbed tines from a crossbar, a pearl at its heart.
    d.strip(49.5, 51.5, 5.4, material=SEABRONZE, depth=2.6, part="crest")
    for t in (-4.4, 0.0, 4.4):
        top = 59.0 if t == 0 else 56.5
        d.poly([(51.5, t - 0.7), (top, t - 0.6), (top, t - 1.0), (top + 3.0, t), (top, t + 1.0), (top, t + 0.6),
                (51.5, t + 0.7)], SEABRONZE, depth=1.6, part=f"tine{t}", bevel=t)
        d.poly([(top - 0.4, t - 0.65), (top - 1.8, t - 1.9), (top - 2.2, t - 0.65)], SEABRONZE, depth=1.4)
    gem(d, 50.5, 0.0, 1.0, st)
    return d


def leviathans_hook():
    st = LEVIATHAN
    d = Design(62, grip=14.0)
    d.disc(0.9, 0.0, 1.1, SEABRONZE, depth=2.2)
    haft(d, 0.8, 46.0, 0.8, st, rings=(26.0,))
    grip(d, 8.5, 19.0, 0.95, st)
    socket(d, 41.0, 47.0, 1.2, st)
    # The bill's hooked blade in sea bronze, like a leviathan's tooth, a glowing tide line along its edge; a pearl spike.
    d.poly(arms_art.BILL_BLADE, SEABRONZE, depth=1.2, part="blade", bevel=lambda s: 2.2)
    d.line(48.0, 4.2, 53.0, 4.8, 0.45, TIDEGLOW, depth=1.6, z=2)
    d.line(53.0, 4.8, 57.6, 6.4, 0.45, TIDEGLOW, depth=1.6, z=2)
    d.poly([(52.5, -0.9), (62.0, 0.0), (52.5, 0.9)], PEARL, depth=1.4)
    return d


DESIGNS = {name: fn for name, fn in globals().items() if callable(fn) and name in (
    "gilded_longsword", "gilded_rapier", "gilded_sabre", "gilded_halberd", "ironclad_zweihander", "ironclad_maul",
    "ironclad_war_pick", "ironclad_battle_axe", "bonecarved_dagger", "bonecarved_flail", "bonecarved_glaive",
    "bonecarved_labrys", "runebound_nodachi", "runebound_moonblade", "runebound_staff", "runebound_war_hammer",
    "glacier_maul", "rimeclaw", "cinderbrand", "magmaw", "hagthorn", "bogfang", "soulreaver", "gravewarden",
    "dynamo_halberd", "piston_hammer", "moonfang", "howler", "stormcaller", "galefeather", "tidebreaker",
    "leviathans_hook")}


# ---------------------------------------------------------------- drawing (as tools/arms_art.py draws the kinds)


def design(name):
    return DESIGNS[name]()


# Variants whose head swings free (tools/arms_heads.py VARIANT_HEADS): their 3D model is the handle alone.
HANDLES = {"bonecarved_flail": bonecarved_flail_handle}


def model_design(name):
    """The design a variant's 3D model is built from: the handle alone for one whose head swings free, else the whole."""
    return HANDLES[name]() if name in HANDLES else design(name)


def layout(name, held):
    """(icon size, grip pixel, diagonal steps to a design unit, hand factor), as arms_art.layout."""
    d = design(name)
    size = arms_art.icon_size(held)
    grip_px, scale = px.fit([d], size)
    return size, grip_px, scale, ((size - 3.0) / d.length) / scale


def draw(name, held):
    size, grip_px, scale, _factor = layout(name, held)
    return px.icon(design(name), size, grip_px, scale)


def held_at(name, held):
    size, (gx, gy), _scale, factor = layout(name, held)
    return (gx, gy), size, factor


def model(name, held):
    """The variant's 3D model: (its texture, its elements), lying over its icon (arms_art.model)."""
    size, grip_px, scale, _factor = layout(name, held)
    unit = scale * math.sqrt(2.0) * 16.0 / size
    grip_model = (grip_px[0] * 16.0 / size, 16.0 - grip_px[1] * 16.0 / size)
    d = model_design(name)
    upright, elements = px.model_elements(d, arms_art.MODEL_TEXTURE, (0, 0), grip_model, unit, width=px.upright_width(d))
    texture = Image.new("RGBA", (arms_art.MODEL_TEXTURE, arms_art.MODEL_TEXTURE), (0, 0, 0, 0))
    texture.paste(upright, (0, 0))
    if name in HANDLES:
        import arms_heads
        arms_heads.paint_swatches(texture, {"chain": BONE, "blade": BONE, "fitting": HORN}, skull=True)
    return texture, elements


def head_layout(name, held):
    """For a variant whose head swings free: (the hand's point in model pixels, model pixels a design unit, the grip and
    the eye along the haft in design units), from the same layout as its model (tools/arms_heads.py entry)."""
    size, grip_px, scale, _factor = layout(name, held)
    unit = scale * math.sqrt(2.0) * 16.0 / size
    grip_model = (grip_px[0] * 16.0 / size, 16.0 - grip_px[1] * 16.0 / size)
    return grip_model, unit, design(name).grip, arms_art.FLAIL_EYE


# ---------------------------------------------------------------- the patterns (smithing templates)

# The goggle glass of Steampunk Armor (tools/armor_styles.py: G and g).
TEAL = M((14, 56, 52), (24, 92, 86), (32, 116, 108), (40, 140, 132), (110, 200, 188), (190, 246, 232))
# The black leather of Kaiser Armor's helmet (tools/armor_styles.py: K, k and w).
BLACK_LEATHER = M((10, 9, 12), (18, 17, 20), (24, 22, 26), (40, 38, 44), (54, 52, 60), (132, 134, 146), shine=False)

# Arms VII's four styles, and the styled armor's two (tools/gear.py: ARMOR_STYLES), all drawn by pattern() below.
PATTERN_INK = {"gilded": (GOLD, SAPPHIRE), "ironclad": (HAZARD, px.OLIVE), "bonecarved": (BONE, px.GARNET),
               "runebound": (RUNE, VIOLET), "steampunk": (px.BRASS, TEAL), "kaiser": (GOLD, BLACK_LEATHER)}


# The styled armor's patterns at vanilla's 16x16 (docs/ITEM_ICONS.md on PR #201: every new item icon is 16x16; the four
# Arms VII patterns below are legacy 32-pixel icons). A small rolled sheet of parchment with its ends curled, and the
# style's emblem in a 10x8 map laid on the sheet: O, D, M and H the ink's outline, dark, mid and highlight; o, m, l and h
# the second ink's dark, mid, light and highlight; k a leather strap; "." leaves the parchment.
PATTERN16_EMBLEMS = {
    "steampunk": [   # a pair of brass-rimmed goggles with teal glass, on a leather strap
        "..........",
        "..........",
        "..DD..DD..",
        "kDhlDDhlDk",
        "kDlmDDlmDk",
        "..DD..DD..",
        "..........",
        "..........",
    ],
    "kaiser": [      # a black spiked helmet with a gold spike, plate and brim
        "....OO....",
        "....HM....",
        "...OMMO...",
        "..ohmmmo..",
        "..omHMmo..",
        "..ommmmo..",
        ".OMMMMMMO.",
        "..........",
    ],
}


def parchment16(emblem, colours):
    """A 16-pixel smithing pattern: a rolled sheet of parchment, its ends curled, with a 10x8 emblem laid on the sheet at
    (3, 4). emblem is 8 rows of 10 symbols; colours maps each symbol to an RGB colour, and "." leaves the parchment.
    pattern16 below and the Earthbinding Template (tools/thallite_armor.py) draw on it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    outline, face, light, shade = (92, 70, 40), (226, 206, 160), (242, 228, 190), (200, 176, 128)
    g.rectangle((2, 3, 13, 12), fill=face, outline=outline)
    g.line((3, 4, 12, 4), fill=light)
    g.line((3, 11, 12, 11), fill=shade)
    for x in (1, 13):   # the curled ends, a pixel taller than the sheet
        g.rectangle((x, 2, x + 1, 13), fill=shade, outline=outline)
    g.line((1, 3, 1, 12), fill=shade)
    g.line((14, 3, 14, 12), fill=light)
    assert len(emblem) == 8 and all(len(row) == 10 for row in emblem), emblem
    for y, row in enumerate(emblem):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((3 + x, 4 + y), tuple(colours[ch]) + (255,))
    return img


def pattern16(style):
    """A 16-pixel smithing pattern: a rolled sheet of parchment, its ends curled, the style's emblem (PATTERN16_EMBLEMS)."""
    ink, second = PATTERN_INK[style]
    colours = {"O": ink.outline_dark, "D": ink.dark, "M": ink.mid, "H": ink.highlight,
               "o": second.dark, "m": second.mid, "l": second.light, "h": second.highlight, "k": (70, 44, 24)}
    return parchment16(PATTERN16_EMBLEMS[style], colours)


def pattern(style):
    """A 32-pixel smithing pattern: a rolled sheet of parchment, its ends curled, the style's emblem drawn on it."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    paper = ((92, 70, 40), (226, 206, 160), (242, 228, 190), (200, 176, 128))
    outline, face, light, shade = paper
    g.rectangle((5, 6, 26, 25), fill=face, outline=outline)
    g.line((6, 7, 25, 7), fill=light)
    g.line((6, 24, 25, 24), fill=shade)
    for x in (3, 27):   # the curled ends
        g.rectangle((x - 1, 5, x + 2, 26), fill=shade, outline=outline)
        g.line((x, 6, x, 25), fill=light)
    ink, second = PATTERN_INK[style]
    c = tuple(ink.mid)
    hi = tuple(ink.highlight)
    dk = tuple(ink.dark)
    if style == "gilded":
        # A gold sword over a blue gem.
        g.line((10, 21, 21, 10), fill=c, width=2)
        g.line((11, 21, 21, 11), fill=hi)
        g.line((11, 17, 14, 20), fill=dk, width=2)
        g.ellipse((14, 14, 17, 17), fill=tuple(second.mid), outline=tuple(second.dark))
    elif style == "ironclad":
        # A hazard-striped plate with bolts.
        g.rectangle((10, 11, 21, 20), fill=tuple(second.mid), outline=tuple(second.outline_dark))
        for i in range(-10, 12, 4):
            g.line((10 + max(0, i), 20 - max(0, -i), 21 - max(0, -i - 1), 11 + max(0, i)), fill=c)
        for x, y in ((11, 12), (20, 12), (11, 19), (20, 19)):
            g.point((x, y), fill=(36, 39, 46))
    elif style == "bonecarved":
        # A skull.
        g.ellipse((11, 9, 21, 19), fill=c, outline=dk)
        g.rectangle((13, 17, 19, 21), fill=c, outline=dk)
        g.rectangle((13, 13, 14, 15), fill=(40, 30, 20))
        g.rectangle((18, 13, 19, 15), fill=(40, 30, 20))
        g.point((16, 17), fill=(40, 30, 20))
        g.point((16, 15), fill=tuple(second.mid))
    elif style == "steampunk":
        # A brass cog behind a pair of teal-glassed goggles on their strap.
        for i in range(8):
            a = i * math.pi / 4
            x, y = round(15 + 6.6 * math.cos(a)), round(15 + 6.6 * math.sin(a))   # a tooth, 3 pixels square
            g.rectangle((x - 1, y - 1, x + 1, y + 1), fill=c, outline=dk)
        g.ellipse((10, 10, 21, 21), fill=c, outline=dk)
        g.ellipse((13, 13, 18, 18), fill=tuple(ink.light), outline=dk)
        g.line((7, 15, 25, 15), fill=(70, 44, 24), width=2)   # the leather strap
        for left in (9, 17):
            g.ellipse((left, 12, left + 6, 18), fill=dk, outline=tuple(ink.outline_dark))
            g.ellipse((left + 1, 13, left + 5, 17), fill=tuple(second.mid))
            g.point((left + 2, 14), fill=tuple(second.highlight))
        g.rectangle((15, 14, 16, 15), fill=hi)   # the bridge
    elif style == "kaiser":
        # A black spiked helmet with a gold plate, a gold spike and a gold brim.
        leather, gloss = tuple(second.mid), tuple(second.light)
        g.polygon([(15, 8), (16, 8), (17, 12), (14, 12)], fill=c, outline=dk)   # the spike
        g.line((15, 9, 15, 11), fill=hi)
        g.rectangle((13, 12, 18, 13), fill=c, outline=dk)                      # its base
        g.chord((9, 13, 22, 27), 180, 360, fill=leather, outline=tuple(second.outline_dark))
        g.arc((11, 15, 20, 25), 200, 260, fill=gloss)
        g.line((8, 20, 23, 20), fill=c)                                        # the brim
        g.line((8, 21, 23, 21), fill=dk)
        g.polygon([(15, 15), (16, 15), (18, 17), (16, 19), (15, 19), (13, 17)], fill=c, outline=dk)   # the plate
        g.point((15, 16), fill=hi)
    else:
        # A glowing rune: a diamond with a bar through it.
        g.polygon([(16, 9), (22, 15), (16, 21), (10, 15)], outline=c)
        g.line((16, 8, 16, 22), fill=hi)
        g.line((12, 15, 20, 15), fill=c)
        g.point((16, 15), fill=(255, 255, 255))
    return img
