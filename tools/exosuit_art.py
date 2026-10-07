"""Art for the powered exosuit (batch 28, docs/features/exosuit.md), in two liveries.

- Vanguard: heavy gunmetal plate with layered shoulder guards and skirt plates, knee guards, a T-visor helmet and
  teal running lights. After the owner's first reference image.
- Ronin: crimson, silver and black, with glowing red eyes under a conical hat. After the second.

Nothing is traced or copied from either image: only the overall themes and colours are followed. Everything here
is drawn procedurally, at double resolution:
- 32x32 inventory icons;
- 128x64 worn layers (the game scales armor UVs to the texture's size);
- 16x16 textures for the 3D parts (shoulder plates, skirt plates, knee guards, the hat), whose boxes are in
  tools/exosuit.py.
"""
from PIL import Image

from hitech import Face, _faces2x, _outline, _mix, BLACK

PALETTES = {
    "vanguard": {
        "edge": (20, 22, 26), "dark": (36, 39, 46), "base": (60, 64, 74), "mid": (86, 92, 104),
        "light": (128, 136, 150), "shine": (176, 184, 196),
        "accent": (98, 122, 124), "accent_light": (138, 164, 162),   # the slate-teal forearm and thigh panels
        "glow": (54, 214, 214), "glow_core": (188, 255, 248),
        "joint": (26, 27, 32),
    },
    "ronin": {
        "edge": (14, 12, 14), "dark": (70, 12, 18), "base": (124, 22, 30), "mid": (162, 34, 42),
        "light": (196, 58, 62), "shine": (232, 112, 108),
        "accent": (170, 176, 186), "accent_light": (226, 230, 236),  # silver
        "glow": (255, 54, 48), "glow_core": (255, 196, 180),
        "joint": (22, 22, 26),
    },
}


def _plate(f, x0, y0, x1, y1, p, fill="base"):
    """A bevelled plate: lit top and left edges, shaded bottom and right edges."""
    f.rect(x0, y0, x1, y1, p[fill])
    f.hline(x0, x1, y0, p["light"])
    f.rect(x0, y0, x0, y1, p["mid"])
    f.hline(x0, x1, y1, p["dark"])
    f.rect(x1, y0 + 1, x1, y1, p["dark"])


# ---------------------------------------------------------------- worn layers (128x64)
def _vanguard_layer1(img, p):
    head = _faces2x("head")
    f = Face(img, head["front"])
    _plate(f, 0, 0, 15, 15, p)
    f.hline(1, 14, 3, p["shine"])                      # brow ridge
    f.rect(2, 6, 13, 7, p["edge"])                     # T-visor
    f.rect(7, 6, 8, 12, p["edge"])
    f.hline(3, 12, 6, _mix(p["edge"], p["glow"], 0.45))
    f.px(7, 11, _mix(p["edge"], p["glow"], 0.45))
    for x in range(4, 12, 2):                          # mouth grille
        f.rect(x, 12, x, 14, p["dark"])
    for x, y in ((1, 9), (14, 9)):                     # cheek guard seams
        f.rect(x, y, x, 15, p["dark"])
    for side in ("right", "left"):
        s = Face(img, head[side])
        _plate(s, 0, 0, 15, 15, p)
        s.rect(5, 6, 10, 11, p["dark"])                # ear module with a teal ring
        s.rect(6, 7, 9, 10, p["glow"])
        s.rect(7, 8, 8, 9, p["dark"])
        s.hline(0, 15, 3, p["shine"])
    back = Face(img, head["back"])
    _plate(back, 0, 0, 15, 15, p)
    for y in range(7, 14, 2):                          # neck vents
        back.hline(4, 11, y, p["dark"])
    top = Face(img, head["top"])
    _plate(top, 0, 0, 15, 15, p)
    top.rect(7, 0, 8, 15, p["shine"])                  # crest line

    body = _faces2x("body")
    f = Face(img, body["front"])
    _plate(f, 0, 0, 15, 8, p)                          # chest plate
    f.hline(4, 11, 0, p["edge"])                       # collar
    f.rect(7, 1, 8, 7, p["dark"])                      # sternum seam
    for i, y in enumerate((9, 12, 15)):                # segmented abdomen
        _plate(f, 1 + i % 2, y, 14 - i % 2, y + 2, p, "mid" if i == 1 else "base")
    for x in (0, 15):                                  # teal slits at the waist
        f.rect(x, 12, x, 16, p["glow"])
    f.rect(0, 18, 15, 19, p["joint"])                  # belt and X buckle
    f.rect(5, 17, 10, 21, p["dark"])
    for i in range(4):
        f.px(6 + i, 18 + i % 4, p["shine"])
        f.px(9 - i, 18 + i % 4, p["shine"])
    f.rect(0, 20, 15, 23, p["dark"])
    back = Face(img, body["back"])
    _plate(back, 0, 0, 15, 17, p)
    _plate(back, 4, 2, 11, 14, p, "mid")               # power pack
    for y in (4, 7, 10):
        back.hline(6, 9, y, p["glow"])
    back.rect(0, 18, 15, 19, p["joint"])
    back.rect(0, 20, 15, 23, p["dark"])
    for side in ("right", "left"):
        s = Face(img, body[side])
        _plate(s, 0, 0, 7, 17, p)
        s.rect(0, 18, 7, 19, p["joint"])
        s.rect(0, 20, 7, 23, p["dark"])
    top = Face(img, body["top"])
    _plate(top, 0, 0, 15, 7, p)

    arm = _faces2x("arm")
    for side in ("front", "right", "left", "back"):
        a = Face(img, arm[side])
        _plate(a, 0, 0, 7, 9, p)                       # upper arm under the shoulder guard
        a.rect(0, 10, 7, 11, p["joint"])               # elbow
        _plate(a, 0, 12, 7, 21, p, "accent")           # slate-teal forearm
        a.hline(1, 6, 12, p["accent_light"])
        for y in (14, 17):                             # bound with dark straps
            a.hline(0, 7, y, p["edge"])
            a.hline(0, 7, y + 1, p["dark"])
        a.rect(0, 22, 7, 23, p["joint"])               # gauntlet
    a = Face(img, arm["front"])
    a.rect(3, 20, 4, 21, p["glow"])
    top = Face(img, arm["top"])
    _plate(top, 0, 0, 7, 7, p)

    leg = _faces2x("leg")
    for side in ("front", "right", "left", "back"):
        b = Face(img, leg[side])
        _plate(b, 0, 13, 7, 19, p)                     # armored boot
        b.rect(0, 20, 7, 23, p["joint"])
        b.hline(0, 7, 23, p["edge"])
    b = Face(img, leg["front"])
    b.rect(3, 13, 4, 19, p["shine"])                   # shin ridge
    b.rect(1, 20, 6, 22, p["mid"])                     # toe cap
    b.hline(1, 6, 20, p["light"])
    bottom = Face(img, leg["bottom"])
    bottom.rect(0, 0, 7, 7, p["joint"])


def _vanguard_layer2(img, p):
    body = _faces2x("body")
    for side in ("front", "back", "right", "left"):
        b = Face(img, body[side])
        w = b.w - 1
        b.rect(0, 16, w, 17, p["joint"])               # hip belt
        _plate(b, 0, 18, w, 23, p)
    leg = _faces2x("leg")
    for side in ("front", "right", "left", "back"):
        b = Face(img, leg[side])
        _plate(b, 0, 0, 7, 9, p, "dark")               # thigh
        _plate(b, 1, 2, 6, 8, p, "accent")             # slate-teal thigh panel
        b.rect(0, 10, 7, 11, p["joint"])               # knee joint (the knee guard is 3D)
        _plate(b, 0, 12, 7, 17, p)                     # upper shin
    b = Face(img, leg["front"])
    b.rect(3, 13, 4, 16, p["glow"])


def _ronin_layer1(img, p):
    head = _faces2x("head")
    f = Face(img, head["front"])
    f.rect(0, 0, 15, 15, p["joint"])                   # black hood
    _plate(f, 2, 3, 13, 14, {**p, "base": p["accent"], "light": p["accent_light"], "mid": p["accent"],
                              "dark": (110, 116, 126)}, "base")  # silver faceplate
    f.rect(3, 6, 12, 8, p["edge"])                     # the eye slit
    for x0 in (3, 10):                                 # glowing red eyes
        f.rect(x0, 7, x0 + 2, 7, p["glow"])
        f.px(x0 + 1, 7, p["glow_core"])
    for x in range(5, 11, 2):                          # mouth vents
        f.rect(x, 11, x, 13, p["edge"])
    f.rect(0, 0, 15, 2, p["base"])                     # crimson brow under the hat
    f.hline(0, 15, 2, p["dark"])
    for side in ("right", "left", "back"):
        s = Face(img, head[side])
        s.rect(0, 0, 15, 15, p["joint"])
        s.rect(0, 0, 15, 2, p["base"])
        for y in range(5, 15, 3):                      # cable ribs
            s.hline(1, 14, y, (52, 52, 60))
    top = Face(img, head["top"])
    top.rect(0, 0, 15, 15, p["base"])

    body = _faces2x("body")
    silver = {**p, "base": p["accent"], "light": p["accent_light"], "mid": p["accent"], "dark": (110, 116, 126)}
    f = Face(img, body["front"])
    f.rect(0, 0, 15, 23, p["joint"])
    _plate(f, 0, 0, 5, 9, p)                           # crimson pectoral plates
    _plate(f, 10, 0, 15, 9, p)
    _plate(f, 5, 1, 10, 8, silver)                     # silver sternum plate
    f.rect(6, 4, 9, 4, p["edge"])                      # its panel line
    for y in range(10, 17, 2):                         # black mechanical ribs
        f.hline(2, 13, y, (56, 56, 64))
        f.hline(4, 11, y, (84, 84, 94))
    _plate(f, 0, 17, 15, 18, silver)                   # silver belt with a crimson buckle
    f.rect(6, 17, 9, 19, p["base"])
    f.rect(0, 19, 15, 23, p["dark"])                   # crimson hip wrap
    back = Face(img, body["back"])
    back.rect(0, 0, 15, 23, p["joint"])
    _plate(back, 1, 0, 14, 9, p)
    back.rect(7, 1, 8, 16, (70, 70, 80))               # spine
    _plate(back, 0, 17, 15, 18, silver)
    back.rect(0, 19, 15, 23, p["dark"])
    for side in ("right", "left"):
        s = Face(img, body[side])
        s.rect(0, 0, 7, 23, p["joint"])
        _plate(s, 0, 0, 7, 7, p)
        _plate(s, 0, 17, 7, 18, silver)
    top = Face(img, body["top"])
    _plate(top, 0, 0, 15, 7, p)

    arm = _faces2x("arm")
    for side in ("front", "right", "left", "back"):
        a = Face(img, arm[side])
        a.rect(0, 0, 7, 23, p["joint"])                # black mechanical arm
        _plate(a, 0, 0, 7, 8, p)                       # crimson upper arm
        a.rect(0, 9, 7, 10, (60, 60, 70))              # elbow joint
        _plate(a, 0, 11, 7, 19, p)                     # crimson forearm plate with silver trim
        a.hline(0, 7, 19, p["accent_light"])
        for y in (20, 22):                             # black fingers
            a.hline(0, 7, y, (52, 52, 60))
    top = Face(img, arm["top"])
    _plate(top, 0, 0, 7, 7, p)

    leg = _faces2x("leg")
    for side in ("front", "right", "left", "back"):
        b = Face(img, leg[side])
        b.rect(0, 13, 7, 23, p["joint"])               # black mechanical shin
        _plate(b, 0, 14, 7, 18, silver)                # silver greave
        b.rect(0, 22, 7, 23, (60, 60, 70))
    b = Face(img, leg["front"])
    for x in (0, 3, 6):                                # clawed toes
        b.rect(x, 21, x + 1, 23, p["accent"])
    bottom = Face(img, leg["bottom"])
    bottom.rect(0, 0, 7, 7, p["joint"])


def _ronin_layer2(img, p):
    body = _faces2x("body")
    for side in ("front", "back", "right", "left"):
        b = Face(img, body[side])
        w = b.w - 1
        b.rect(0, 16, w, 23, p["dark"])                # crimson hip wrap
        b.hline(0, w, 16, p["accent"])
    leg = _faces2x("leg")
    for side in ("front", "right", "left", "back"):
        b = Face(img, leg[side])
        b.rect(0, 0, 7, 17, p["joint"])
        _plate(b, 0, 0, 7, 8, p)                       # crimson thigh plate
        b.rect(0, 9, 7, 10, (60, 60, 70))
        _plate(b, 1, 11, 6, 13, {**p, "base": p["accent"], "light": p["accent_light"], "mid": p["accent"],
                                  "dark": (110, 116, 126)})  # silver knee


LAYERS = {"vanguard": (_vanguard_layer1, _vanguard_layer2), "ronin": (_ronin_layer1, _ronin_layer2)}


def layer(style, leggings):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    LAYERS[style][1 if leggings else 0](img, PALETTES[style])
    return img


# ---------------------------------------------------------------- textures for the 3D parts (16x16)
def part_textures():
    """name -> image, saved as textures/block/<name>.png and drawn by client/WornModelLayer."""
    out = {}
    v, r = PALETTES["vanguard"], PALETTES["ronin"]

    def tile(fill_fn):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
        fill_fn(Face(img, (0, 0, 16, 16)))
        return img

    def vanguard_plate(f):
        _plate(f, 0, 0, 15, 15, v)
        f.hline(1, 14, 7, v["dark"])
        for x, y in ((2, 2), (13, 2), (2, 12), (13, 12)):
            f.px(x, y, v["shine"])
    out["exo_vanguard_plate"] = tile(vanguard_plate)

    def vanguard_light(f):
        vanguard_plate(f)
        f.rect(5, 4, 10, 9, v["dark"])
        f.rect(6, 5, 9, 8, v["glow"])
        f.rect(7, 6, 8, 7, v["glow_core"])
    out["exo_vanguard_light"] = tile(vanguard_light)

    def vanguard_knee(f):
        f.rect(0, 0, 15, 15, v["dark"])
        f.rect(2, 1, 13, 14, v["base"])
        f.rect(1, 2, 14, 13, v["base"])
        f.rect(3, 2, 8, 6, v["light"])
        f.hline(4, 11, 13, v["edge"])
    out["exo_vanguard_knee"] = tile(vanguard_knee)

    def ronin_plate(f):
        _plate(f, 0, 0, 15, 15, r)
        f.rect(0, 13, 15, 15, r["accent"])             # silver trim
        f.hline(0, 15, 13, r["accent_light"])
        f.hline(2, 10, 5, r["dark"])
    out["exo_ronin_plate"] = tile(ronin_plate)

    def ronin_silver(f):
        _plate(f, 0, 0, 15, 15, {**r, "base": r["accent"], "light": r["accent_light"], "mid": r["accent"],
                                  "dark": (110, 116, 126)})
        f.rect(5, 5, 10, 10, r["edge"])                # a port ring
        f.rect(6, 6, 9, 9, r["glow"])
    out["exo_ronin_silver"] = tile(ronin_silver)

    def ronin_kasa(f):
        # Lacquered crimson with dark ribs, like a conical hat's woven frame.
        f.rect(0, 0, 15, 15, r["base"])
        for i in range(0, 16, 4):
            f.hline(0, 15, i, r["dark"])
            f.rect(i, 0, i, 15, r["dark"])
        for x, y in ((2, 2), (10, 6), (6, 10), (14, 13)):
            f.px(x, y, r["shine"])
    out["exo_ronin_kasa"] = tile(ronin_kasa)

    out["exo_ronin_cord"] = tile(lambda f: f.rect(0, 0, 15, 15, (40, 40, 46)))

    def ronin_cloth(f):
        f.rect(0, 0, 15, 15, r["dark"])
        for x in range(0, 16, 4):                      # long vertical strips, crimson and black
            f.rect(x, 0, x + 1, 15, r["base"])
        f.hline(0, 15, 0, r["accent"])
    out["exo_ronin_cloth"] = tile(ronin_cloth)

    def ronin_tabard(f):
        f.rect(0, 0, 15, 15, r["joint"])
        f.rect(1, 0, 14, 15, (30, 30, 34))
        f.rect(7, 2, 8, 13, r["glow"])                 # a red sigil line
        f.hline(4, 11, 5, r["glow"])
        f.hline(0, 15, 0, r["accent"])
    out["exo_ronin_tabard"] = tile(ronin_tabard)
    return out


# ---------------------------------------------------------------- inventory icons (32x32)
def _icon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    return img, Face(img, (0, 0, 32, 32))


def helmet_icon(style):
    p = PALETTES[style]
    img, f = _icon()
    if style == "vanguard":
        _plate(f, 8, 4, 23, 25, p)                     # domed shell
        f.rect(9, 3, 22, 3, p["mid"])
        f.rect(15, 1, 16, 4, p["shine"])               # crest fin
        f.hline(9, 22, 8, p["shine"])                  # brow
        f.rect(10, 12, 21, 14, p["edge"])              # T-visor
        f.rect(14, 12, 17, 21, p["edge"])
        f.hline(11, 20, 13, p["glow"])
        f.rect(15, 15, 16, 19, _mix(p["edge"], p["glow"], 0.5))
        for x in range(11, 21, 2):                     # mouth grille
            if not 14 <= x <= 17:
                f.rect(x, 21, x, 24, p["dark"])
        f.rect(5, 15, 8, 23, p["dark"])                # cheek guards
        f.rect(23, 15, 26, 23, p["dark"])
        f.rect(6, 17, 7, 19, p["glow"])
        f.rect(24, 17, 25, 19, p["glow"])
    else:
        f.rect(10, 13, 21, 27, p["joint"])             # black hood under the hat
        f.rect(12, 16, 19, 26, p["accent"])            # silver faceplate
        f.hline(12, 19, 16, p["accent_light"])
        f.rect(12, 19, 19, 20, p["edge"])              # eye slit, glowing red
        f.rect(12, 19, 14, 19, p["glow"])
        f.rect(17, 19, 19, 19, p["glow"])
        f.px(13, 19, p["glow_core"])
        f.px(18, 19, p["glow_core"])
        for x in (14, 16):
            f.rect(x, 23, x, 25, p["edge"])
        for i in range(8):                             # the conical hat, wider than the head
            y = 4 + i
            half = 2 + i * 2
            f.rect(16 - half, y, 15 + half, y, p["base"] if i % 2 else p["mid"])
        f.hline(0, 31, 12, p["dark"])                  # brim edge
        f.rect(15, 2, 16, 4, p["accent_light"])        # finial
        for y in range(13, 28, 2):                     # hanging cords
            f.px(4, y, (40, 40, 46))
            f.px(27, y, (40, 40, 46))
    return _outline(img, BLACK, alpha=170)


def chestplate_icon(style):
    p = PALETTES[style]
    img, f = _icon()
    if style == "vanguard":
        _plate(f, 9, 5, 22, 14, p)                     # chest
        f.hline(12, 19, 5, p["edge"])
        f.rect(15, 6, 16, 13, p["dark"])
        for i, y in enumerate((15, 18, 21)):           # abdomen segments
            _plate(f, 10 + i % 2, y, 21 - i % 2, y + 2, p, "mid" if i == 1 else "base")
        f.rect(9, 17, 9, 21, p["glow"])
        f.rect(22, 17, 22, 21, p["glow"])
        f.rect(10, 24, 21, 25, p["joint"])             # belt with X buckle
        f.rect(14, 23, 17, 26, p["dark"])
        f.px(14, 23, p["shine"])
        f.px(17, 23, p["shine"])
        f.px(15, 24, p["shine"])
        f.px(16, 24, p["shine"])
        f.px(15, 25, p["shine"])
        f.px(16, 25, p["shine"])
        for x0, d in ((1, 1), (23, -1)):               # stacked shoulder guards
            for i in range(3):
                y = 5 + i * 3
                xa, xb = (x0 + (i if d > 0 else 0), x0 + 7 - (0 if d > 0 else i))
                _plate(f, xa, y, xb, y + 2, p)
            f.rect(x0 + 3, 6, x0 + 4, 6, p["glow"])
    else:
        f.rect(9, 5, 22, 26, p["joint"])
        _plate(f, 9, 5, 14, 13, p)                     # crimson pectorals
        _plate(f, 17, 5, 22, 13, p)
        _plate(f, 14, 6, 17, 12, {**p, "base": p["accent"], "light": p["accent_light"], "mid": p["accent"],
                                   "dark": (110, 116, 126)})
        for y in range(15, 22, 2):                     # black ribs
            f.hline(11, 20, y, (70, 70, 80))
        f.rect(9, 22, 22, 23, p["accent"])             # silver belt
        f.rect(14, 22, 17, 24, p["base"])
        f.rect(13, 24, 18, 29, p["dark"])              # tabard
        f.rect(15, 25, 16, 28, p["glow"])
        for x0 in (1, 23):                             # big crimson shoulder plates with a silver port
            _plate(f, x0, 4, x0 + 7, 14, p)
            f.hline(x0, x0 + 7, 14, p["accent_light"])
            f.rect(x0 + 2, 7, x0 + 5, 10, p["accent"])
            f.rect(x0 + 3, 8, x0 + 4, 9, p["edge"])
    return _outline(img, BLACK, alpha=170)


def leggings_icon(style):
    p = PALETTES[style]
    img, f = _icon()
    if style == "vanguard":
        f.rect(8, 3, 23, 5, p["joint"])                # hip belt
        for x0 in (8, 17):
            _plate(f, x0, 6, x0 + 6, 27, p, "dark")
            _plate(f, x0 + 1, 8, x0 + 5, 14, p, "accent")
            _plate(f, x0, 16, x0 + 6, 19, p, "light")  # knee guard
        for x0 in (5, 21):                             # skirt plates
            _plate(f, x0, 4, x0 + 5, 12, p)
        _plate(f, 12, 4, 19, 10, p)
        f.rect(15, 6, 16, 8, p["glow"])
    else:
        f.rect(8, 3, 23, 5, p["accent"])
        for x0 in (8, 17):
            f.rect(x0, 6, x0 + 6, 27, p["joint"])
            _plate(f, x0, 6, x0 + 6, 13, p)
            f.rect(x0 + 2, 16, x0 + 4, 18, p["accent"])
        for x in range(6, 26, 3):                      # long crimson skirt strips
            f.rect(x, 5, x + 1, 20 + (x % 4), p["base"] if x % 2 else p["dark"])
        f.rect(14, 5, 17, 22, (30, 30, 34))            # tabard and its sigil
        f.rect(15, 8, 16, 19, p["glow"])
    return _outline(img, BLACK, alpha=170)


def boots_icon(style):
    p = PALETTES[style]
    img, f = _icon()
    for x0 in (5, 18):
        if style == "vanguard":
            _plate(f, x0, 8, x0 + 8, 22, p)
            f.rect(x0 + 4, 9, x0 + 4, 21, p["shine"])
            f.rect(x0, 23, x0 + 9, 27, p["joint"])
            _plate(f, x0 + 1, 23, x0 + 8, 25, p, "mid")
            f.rect(x0 + 3, 18, x0 + 5, 19, p["glow"])
        else:
            f.rect(x0, 8, x0 + 8, 27, p["joint"])
            _plate(f, x0, 10, x0 + 8, 18, {**p, "base": p["accent"], "light": p["accent_light"], "mid": p["accent"],
                                            "dark": (110, 116, 126)})
            for x in (x0, x0 + 3, x0 + 6):              # claws
                f.rect(x, 24, x + 2, 27, p["accent"])
            f.hline(x0, x0 + 8, 8, p["base"])
    return _outline(img, BLACK, alpha=170)


def livery_icon(style):
    """A livery template: a rolled stencil sheet in the style's colours."""
    p = PALETTES[style]
    img, f = _icon()
    _plate(f, 6, 4, 25, 27, {**p, "base": (214, 206, 186), "light": (240, 234, 218), "mid": (200, 192, 172),
                             "dark": (150, 142, 124)})
    f.rect(9, 8, 22, 11, p["base"])
    f.rect(9, 13, 22, 16, p["dark"] if style == "ronin" else p["accent"])
    f.rect(9, 18, 22, 21, p["joint"])
    f.rect(13, 23, 18, 24, p["glow"])
    return _outline(img, BLACK, alpha=170)


ICONS = {"helmet": helmet_icon, "chestplate": chestplate_icon, "leggings": leggings_icon, "boots": boots_icon}


def draw_all(save, save_armor):
    """save(img, kind, name, animation=...) as in generate_textures; save_armor(img, layer, name)."""
    import exosuit
    import hitech
    for style, (prefix, _, asset) in exosuit.STYLES.items():
        for piece in exosuit.PIECES:
            save(ICONS[piece](style), "item", f"{prefix}_{piece}")
        save_armor(layer(style, False), "humanoid", asset)
        save_armor(layer(style, True), "humanoid_leggings", asset)
        save(livery_icon(style), "item", f"{style}_livery")
    save(hitech.katana(hitech.KATANA_COLOURS["crimson"]), "item", "ronin_katana", animation={"frametime": 2})
    for name, img in part_textures().items():
        save(img, "block", name)
