"""Sprites of the Arms VI kit (batch 55, docs/features/arms-vi.md): the longbows and arbalests, drawn as vanilla's bow
and crossbow are (at rest and drawn in three steps; a crossbow also loaded), and the shields' painted faces, backs and
metal trim, which tools/arms_kit.py maps onto their 3D models.

After the owner's reference sheets of an iron-and-wood war kit (studied for the look; nothing is copied). As the other
arms: bronze is warm and steampunk (oiled oak, brass, crimson paint), steel kaiserpunk (dark wood, blued steel,
gunmetal, olive paint, docs/ART_DIRECTION.md). All original.
"""
import math
import random

from PIL import Image

import arms
import arms_pixel as px
import arms_kit
import hd_art as hd
from hd_art import Material

CRIMSON = Material([(52, 8, 10), (88, 16, 18), (128, 26, 26), (164, 40, 36), (196, 64, 52), (226, 104, 86)], 0.1, 6)
NAVY = Material([(10, 14, 24), (18, 24, 40), (28, 38, 60), (40, 54, 82), (58, 76, 108), (86, 106, 138)], 0.1, 6)
OLIVE = hd.OLIVE
GOLD = hd.BRASS


def _tone(material, value):
    """A ramp colour for a brightness from 0 to 1."""
    ramp = material.ramp
    return ramp[max(0, min(len(ramp) - 1, int(value * len(ramp))))]


def _put(img, x, y, colour, alpha=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(colour[:3]) + (alpha,))


def _metal(style):
    """The trim's metal: bronze's blade bronze, steel's blued steel."""
    return style.blade


def _boards(img, material, rng, width, x0=0, y0=0, x1=None, y1=None, vertical=True):
    """Planks with a grain, a dark seam between each, lit a little from the top left."""
    x1 = img.width if x1 is None else x1
    y1 = img.height if y1 is None else y1
    shift = [0.0 for _ in range(64)]
    for y in range(y0, y1):
        for x in range(x0, x1):
            along, across = (y, x - x0) if vertical else (x, y - y0)
            board = across // width
            edge = across % width
            grain = 0.06 * math.sin(along * 0.45 + board * 2.1 + math.sin(along * 0.11 + board) * 3.0)
            value = 0.55 + shift[board % 64] + grain - 0.12 * (along / max(1, (y1 if vertical else x1)))
            if edge == 0:
                value = 0.12
            elif edge == 1:
                value += 0.08
            _put(img, x, y, _tone(material, value))


def _rivet(img, x, y, material, big=False):
    """A domed rivet head: lit at its top left, shadowed below."""
    _put(img, x, y, material.ramp[-2])
    _put(img, x - 1, y - 1, material.ramp[-1]) if big else None
    _put(img, x + 1, y, material.ramp[2])
    _put(img, x, y + 1, material.ramp[1])
    _put(img, x + 1, y + 1, (12, 10, 10))
    if big:
        _put(img, x - 1, y, material.ramp[-2])
        _put(img, x, y - 1, material.ramp[-1])


def _wear(img, rng, count, bare, length=(2, 6)):
    """Scratches through the paint, to the wood or metal beneath."""
    for _ in range(count):
        x, y = rng.uniform(0, img.width), rng.uniform(0, img.height)
        angle = rng.uniform(0, math.pi)
        for step in range(rng.randint(*length)):
            px, py = int(x + math.cos(angle) * step), int(y + math.sin(angle) * step)
            if 0 <= px < img.width and 0 <= py < img.height and img.getpixel((px, py))[3]:
                _put(img, px, py, bare)


def _vignette(img, strength=0.35):
    """Darkens the face towards its edges, so the painted board reads as curved under the rim."""
    cx, cy = img.width / 2, img.height / 2
    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            d = max(abs(x + 0.5 - cx) / cx, abs(y + 0.5 - cy) / cy)
            k = 1.0 - strength * max(0.0, d - 0.6) / 0.4
            pixels[x, y] = (int(r * k), int(g * k), int(b * k), a)


def _face_size(kind):
    """The painted part of a shield's 64x64 face texture: the plate at arms_kit.face_scale, along the left side."""
    px0, py0, px1, py1 = arms_kit.PLATE[kind]
    k = arms_kit.face_scale(kind)
    return int(round((px1 - px0) * k)), int(round((py0 - py1) * k))


def _model(kind, x, y):
    """A face pixel's centre in model pixels."""
    px0, py0, _px1, _py1 = arms_kit.PLATE[kind]
    k = arms_kit.face_scale(kind)
    return px0 + (x + 0.5) / k, py0 - (y + 0.5) / k


def _on_face(kind, painted):
    """The painted plate on a 64x64 face texture, at its top left."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    img.paste(painted, (0, 0))
    return img


def heater_face(metal):
    """A heater shield's face. Bronze: crimson, a gold chevron and a gold jug in chief (Jugcraft's own charge). Steel:
    navy, a riveted steel bend and a pale-green gauge-light roundel."""
    style = px.STYLES[metal]
    width, height = _face_size("heater_shield")
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    rng = random.Random(f"heater_{metal}")
    field = CRIMSON if metal == "bronze" else NAVY
    for y in range(height):
        for x in range(width):
            grain = 0.03 * math.sin(y * 0.5 + math.sin(x * 0.3) * 2)
            _put(img, x, y, _tone(field, 0.52 + grain - 0.1 * y / height))
    if metal == "bronze":
        # The chevron: a band rising from the flanks to a peak at the middle.
        for y in range(height):
            for x in range(width):
                mx, my = _model("heater_shield", x, y)
                peak = 11.5 - abs(mx - 8.0) * 1.15
                if peak - 2.2 <= my <= peak:
                    lit = 0.75 if my > peak - 0.6 else (0.35 if my < peak - 1.7 else 0.6)
                    _put(img, x, y, _tone(GOLD, lit))
        # A jug in chief: a round belly, a neck and a handle, in gold, in model pixels about (8, 15).
        for y in range(height):
            for x in range(width):
                mx, my = _model("heater_shield", x, y)
                dx, dy = mx - 8.0, 15.0 - my
                belly = (dx / 2.3) ** 2 + ((dy - 0.6) / 2.0) ** 2 <= 1.0
                neck = abs(dx) <= 0.8 and -2.6 <= dy <= -1.0
                lip = abs(dx) <= 1.15 and -3.1 <= dy <= -2.6
                handle = dx > 1.9 and 0.9 <= math.hypot(dx - 2.0, dy + 0.4) <= 1.5
                if belly or neck or lip or handle:
                    lit = 0.82 if dx < -0.5 and dy < 0.6 else (0.4 if dx > 1.0 or dy > 1.6 else 0.62)
                    _put(img, x, y, _tone(GOLD, lit))
    else:
        # The bend: a riveted steel band from the top left to the bottom right, through the middle. Along it is
        # (0.8, -0.6) in model pixels, across it (0.6, 0.8), from the plate's top-left corner.
        px0, py0 = arms_kit.PLATE["heater_shield"][:2]
        for y in range(height):
            for x in range(width):
                mx, my = _model("heater_shield", x, y)
                across = (mx - px0) * 0.6 + (my - py0) * 0.8
                if -5.2 <= across <= -2.0:
                    lit = 0.78 if across > -2.5 else (0.3 if across < -4.7 else 0.55)
                    _put(img, x, y, _tone(style.blade, lit))
        for step in range(12):
            along = 1.5 + step * 2.2
            mx, my = px0 + 0.6 * -3.6 + 0.8 * along, py0 + 0.8 * -3.6 - 0.6 * along
            k = arms_kit.face_scale("heater_shield")
            x, y = int((mx - px0) * k), int((py0 - my) * k)
            if 2 <= x < width - 2 and 2 <= y < height - 2:
                _rivet(img, x, y, hd.BRASS)
        # A roundel in the upper right, clear of the bend (model pixels about (11, 15.5)): a lit green gauge light in a
        # gunmetal bezel.
        for y in range(height):
            for x in range(width):
                mx, my = _model("heater_shield", x, y)
                d = math.hypot(mx - 11.0, my - 15.5)
                if d <= 1.9:
                    _put(img, x, y, _tone(hd.GUNMETAL, 0.7 if (mx - 11.0) - (my - 15.5) < 0 else 0.35) if d > 1.35
                         else _tone(px.PHOSPHOR, 0.85 - d / 2.6))
    _wear(img, rng, 18, (style.haft.ramp[3] if metal == "bronze" else style.blade.ramp[3]))
    _vignette(img)
    return _on_face("heater_shield", img)


def tower_face(metal):
    """A tower shield's face. Bronze: oak boards bound with three bronze bands. Steel: olive-painted riveted plates with a
    vision slit near the top, the paint worn to the steel at the edges."""
    style = px.STYLES[metal]
    width, height = _face_size("tower_shield")
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    rng = random.Random(f"tower_{metal}")
    if metal == "bronze":
        _boards(img, style.haft, rng, 9)
        for band in (0.16, 0.5, 0.84):
            y0 = int(height * band) - 3
            for y in range(y0, y0 + 6):
                for x in range(width):
                    lit = 0.8 if y == y0 else (0.28 if y == y0 + 5 else 0.55)
                    _put(img, x, y, _tone(_metal(style), lit))
            for x in range(4, width - 2, 9):
                _rivet(img, x, y0 + 2, GOLD, big=True)
    else:
        for y in range(height):
            for x in range(width):
                _put(img, x, y, _tone(OLIVE, 0.5 - 0.12 * y / height))
        seams = [int(height * f) for f in (0.3, 0.62)]
        for seam in seams:
            for x in range(width):
                _put(img, x, seam, OLIVE.ramp[0])
                _put(img, x, seam + 1, OLIVE.ramp[4])
            for x in range(3, width - 1, 6):
                _rivet(img, x, seam - 3, OLIVE)
                _rivet(img, x, seam + 4, OLIVE)
        # The vision slit: dark, with a lit lower lip.
        sy = int(height * 0.14)
        for y in range(sy, sy + 3):
            for x in range(12, width - 12):
                _put(img, x, y, (8, 8, 10))
        for x in range(11, width - 11):
            _put(img, x, sy - 1, OLIVE.ramp[1])
            _put(img, x, sy + 3, OLIVE.ramp[5])
        # A stencilled number in pale paint, low on the board.
        for i, (dx, dy) in enumerate([(0, 0), (1, 0), (2, 0), (2, 1), (2, 2), (1, 2), (0, 2), (0, 3), (0, 4), (1, 4), (2, 4),
                                      (5, 0), (5, 1), (5, 2), (5, 3), (5, 4)]):
            for sx in range(2):
                for sy2 in range(2):
                    _put(img, width // 2 - 6 + dx * 2 + sx, int(height * 0.78) + dy * 2 + sy2, hd.WHITE_PAINT.ramp[2])
        _wear(img, rng, 28, style.blade.ramp[3])
    _vignette(img, 0.3)
    return _on_face("tower_shield", img)


def back(metal):
    """A shield's back: bare boards (dark for steel), a leather strap across their middle (STRAP_UV)."""
    style = px.STYLES[metal]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(f"back_{metal}")
    _boards(img, style.haft, rng, 8)
    u0, v0, u1, v1 = arms_kit.STRAP_UV
    strap = px.LEATHER if metal == "bronze" else hd.RUBBER
    for y in range(int(v0 * 4) - 2, int(v1 * 4) + 2):
        for x in range(64):
            edge = y in (int(v0 * 4) - 2, int(v1 * 4) + 1)
            _put(img, x, y, _tone(strap, 0.2 if edge else 0.5 + 0.08 * math.sin(x * 0.7)))
    for x in (10, 32, 54):
        _rivet(img, x, int(v0 * 4) + 3, style.fitting, big=True)
    return img


def trim(metal):
    """A shield's metal: the rim band along the top (RIM_UV), the boss's face at the bottom left (BOSS_UV), plain metal
    at the bottom right (PLAIN_UV)."""
    style = px.STYLES[metal]
    metal_material = _metal(style)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(f"trim_{metal}")
    for y in range(16):
        for x in range(32):
            lit = (0.85, 0.65, 0.5, 0.3)[min(3, y)] if y < 4 else 0.5
            _put(img, x, y, _tone(metal_material, lit))
    for x in range(3, 32, 8):
        _rivet(img, x, 1, style.fitting)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8.0, y + 0.5 - 8.0
            dist = math.hypot(dx, dy)
            if dist <= 2.2:
                _put(img, x, 16 + y, style.fitting.highlight if dx + dy < 0 else style.fitting.light)
            elif 6.0 <= dist <= 7.9:
                _put(img, x, 16 + y, _tone(style.fitting, 0.75 - (dx + dy) / 22.0))
            elif dist < 6.0:
                _put(img, x, 16 + y, _tone(metal_material, 0.62 - (dx + dy) / 18.0))
    for y in range(16, 32):
        for x in range(16, 32):
            _put(img, x, y, _tone(metal_material, 0.5))
    return img


# ---------------------------------------------------------------- bows and crossbows
#
# In the arms' pixel style (tools/arms_pixel.py), as vanilla 26.3 lays out its bow and crossbow (read in game), so
# vanilla's hand poses hold them: a bow's limbs run from the top right to the bottom left with its grip at the centre
# and its arrow pointing to the top left; a crossbow's stock runs from its butt at the bottom right to its prod at the
# top left. All of a bow's (or crossbow's) frames are fitted alike, so it stays put as it is drawn.
ICON = 32
# How far back the string is drawn in each of the three steps (design units), and a crossbow's in its three.
BOW_DRAW = (4.0, 7.0, 10.0)
CROSSBOW_PULL = (2.5, 5.0, 7.5)
STRING = px.Material((70, 64, 52), (110, 102, 86), (150, 142, 122), (196, 190, 170), (226, 222, 204), (240, 238, 226),
                     shine=False)
FLETCH = px.CLOTH_RED


def _arrow_design(d, s, t_nock, length, st, along_s=False):
    """An arrow from its nock to its head, across the bow (along -t) or along a crossbow's stock (along s)."""
    if along_s:
        d.strip(t_nock, t_nock + length - 2.4, 0.45, material=px.WOOD, depth=1.0, z=3)
        d.poly([(t_nock + length - 2.6, -1.3), (t_nock + length, 0.0), (t_nock + length - 2.6, 1.3)], st.blade, z=3)
        for side in (1, -1):
            d.poly([(t_nock + 0.3, 0.0), (t_nock + 0.9, side * 1.2), (t_nock + 3.6, side * 1.2), (t_nock + 4.2, 0.0)],
                   FLETCH, z=2)
        return
    head = t_nock - length
    d.line(s, t_nock, s, head + 2.4, 0.9, px.WOOD, z=3)
    d.poly([(s - 1.3, head + 2.6), (s, head), (s + 1.3, head + 2.6)], st.blade, z=3)
    for side in (1, -1):
        d.poly([(s, t_nock - 0.3), (s + side * 1.2, t_nock - 0.9), (s + side * 1.2, t_nock - 3.6), (s, t_nock - 4.2)],
               FLETCH, z=2)


def longbow_design(metal, draw=None):
    """A longbow at rest (draw None) or drawn back `draw` with an arrow nocked. Bronze: yew with brass nocks and a
    leather grip. Steel: dark wood backed with blued steel, gunmetal fittings and a rubber grip."""
    st = px.STYLES[metal]
    half = 15.0
    d = px.Design(2 * half, grip=half)
    bend = 2.6 + (draw or 0.0) * 0.22

    def centre(s):
        return bend * (abs(s - half) / half) ** 1.7

    def width(s):
        return 1.05 - 0.5 * abs(s - half) / half
    d.strip(0.8, 2 * half - 0.8, lambda s: width(s) - centre(s), lambda s: width(s) + centre(s), material=st.haft,
            part="limbs")
    if metal == "steel":
        d.strip(2.0, 2 * half - 2.0, lambda s: width(s) + 0.6 - centre(s), lambda s: -width(s) + centre(s),
                material=st.blade, part="backing")
    for tip in (0.8, 2 * half - 0.8):
        d.disc(tip, centre(tip), 0.9, st.fitting, z=2)
    nock = bend + (draw or 0.0)
    if draw is None:
        d.line(0.8, bend, 2 * half - 0.8, bend, 0.8, STRING, z=-1)
    else:
        d.line(0.8, bend, half, nock, 0.8, STRING, z=-1)
        d.line(half, nock, 2 * half - 0.8, bend, 0.8, STRING, z=-1)
        _arrow_design(d, half, nock, 17.0, st)
    d.strip(half - 2.6, half + 2.6, 1.35, material=st.grip, stripes=(1.3, px.DARK), z=1)
    for s in (half - 2.9, half + 2.9):
        d.strip(s - 0.4, s + 0.4, 1.45, material=st.fitting, z=2)
    return d


def arbalest_design(metal, pull=None, load=None):
    """An arbalest: a wooden stock with a metal prod across its nose and a stirrup to hold it down, at rest, wound back
    `pull`, or loaded with an arrow or a firework. The steel one carries a cranequin's geared rack."""
    st = px.STYLES[metal]
    d = px.Design(30.0, grip=7.0)
    nose, nut = 24.5, 15.0
    d.strip(0.0, 6.5, 2.0, material=st.haft, part="butt")
    d.strip(6.5, nose + 1.5, 1.1, material=st.haft, part="tiller")
    d.line(9.0, 1.1, 12.0, 3.2, 0.8, st.fitting)      # the trigger lever
    d.disc(nut, 0.0, 1.0, st.fitting, z=2)             # the nut
    if metal == "steel":
        d.strip(7.5, nut - 1.5, -1.1, 2.3, material=px.GUNMETAL, stripes=(1.0, px.DARK), z=1)
    flex = (pull or ((nose - nut) * 0.6 if load else 0.0)) * 0.35
    tips = []
    for side in (1, -1):
        pts_out, pts_in = [], []
        for i in range(9):
            f = i / 8
            sv = nose - (2.0 + flex) * f ** 1.5
            pts_out.append((sv + 0.6, side * 11.0 * f))
            pts_in.append((sv - 0.6, side * 11.0 * f))
        d.poly(pts_out + pts_in[::-1], st.blade, part=f"prod{side}")
        tips.append((nose - 2.0 - flex, side * 11.0))
        d.disc(nose - 2.0 - flex, side * 11.0, 0.8, st.fitting, z=2)
    d.strip(nose + 1.0, nose + 4.5, 1.6, -0.6, material=st.fitting, part="stirrup")
    d.strip(nose + 1.0, nose + 4.5, -0.6, 1.6, material=st.fitting, part="stirrup2")
    d.strip(nose + 3.6, nose + 4.5, 1.6, material=st.fitting, part="stirrup3")
    back = nut if load else (nose - 2.0 - flex - (pull or 0.0))
    for s_tip, t_tip in tips:
        d.line(s_tip, t_tip, back, 0.0, 0.8, STRING, z=1)
    if load == "arrow":
        _arrow_design(d, 0.0, nut, 16.0, st, along_s=True)
    elif load == "firework":
        d.strip(nut + 0.5, nose + 2.5, 1.5, material=px.CLOTH_RED, stripes=(2.5, px.HIGHLIGHT), z=3)
        d.poly([(nose + 2.5, -1.5), (nose + 5.5, 0.0), (nose + 2.5, 1.5)], hd_white, z=3)
    return d


hd_white = px.Material((90, 90, 92), (140, 140, 144), (184, 184, 188), (214, 214, 218), (236, 236, 240), (252, 252, 255))


def _frames(designs, mirrored):
    grip_px, scale = px.fit(designs, ICON)
    if mirrored:
        grip_px = (ICON - grip_px[0], grip_px[1])
    return [px.icon(d, ICON, grip_px, scale, mirrored=mirrored) for d in designs]


def bow_sprites(metal):
    designs = [longbow_design(metal)] + [longbow_design(metal, draw) for draw in BOW_DRAW]
    # A bow's arrow points to the top left: the icon's across direction (t) runs that way mirrored about the diagonal,
    # which the bow's symmetry makes the same as turning it: draw it with t towards the bottom right, arrow along -t.
    frames = _frames(designs, mirrored=False)
    return dict(zip(["", "_pulling_0", "_pulling_1", "_pulling_2"], frames))


def crossbow_sprites(metal):
    designs = [arbalest_design(metal)] + [arbalest_design(metal, pull) for pull in CROSSBOW_PULL] + [
        arbalest_design(metal, load="arrow"), arbalest_design(metal, load="firework")]
    frames = _frames(designs, mirrored=True)
    return dict(zip(["_standby", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"], frames))


RANGED_ART = {"bow": bow_sprites, "crossbow": crossbow_sprites}


SHIELD_FACES = {"heater_shield": heater_face, "tower_shield": tower_face}


def draw_all(save):
    for item in arms.kit():
        metal, kind = arms.split(item)
        if kind in arms.SHIELD_KINDS:
            save(SHIELD_FACES[kind](metal), "item", f"{item}_face")
            save(back(metal), "item", f"{item}_back")
            save(trim(metal), "item", f"{item}_trim")
        else:
            for suffix, img in RANGED_ART[arms.RANGED_KINDS[kind]["type"]](metal).items():
                save(img, "item", f"{item}{suffix}")
