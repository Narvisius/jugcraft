"""Original textures for petrochemistry: animated still and flowing fluids, and filled buckets.

Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or recoloured. Called from
generate_textures.py.
"""
import math
import random

from PIL import Image

import block_style as bs
import material_style as ms
from petro import FLUIDS, GASES

FRAMES = 16


def still(colors, seed):
    """A slow, heavy surface, as vanilla still water and lava: mostly the middle tone, with lighter swells drifting
    across, a thin sheen on their crests and a few darker troughs."""
    dark, mid, light, sheen = colors
    rng = random.Random(seed)
    # Wave vectors in whole cycles per tile, so the texture tiles; each wave moves a whole cycle per loop.
    waves = [(1, 0), (0, 1), (1, 1), (2, -1), (-1, 2)]
    phases = [rng.uniform(0, 2 * math.pi) for _ in waves]
    frames = []
    for frame in range(FRAMES):
        t = frame / FRAMES * 2 * math.pi
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = sum(math.sin(2 * math.pi * (x * fx + y * fy) / 16 + p + t * (1 if i % 2 else -1))
                        for i, ((fx, fy), p) in enumerate(zip(waves, phases))) / len(waves)
                c = dark if v < -0.42 else mid if v < 0.26 else light
                if v > 0.5:
                    c = sheen
                img.putpixel((x, y), c + (255,))
        frames.append(img)
    return frames


def flowing(colors, seed):
    """The same fluid running downhill, as vanilla flowing water: soft streaks a few pixels wide and long, mostly the
    middle tone, sliding down the texture one pixel a frame (the pattern tiles, so the loop is seamless)."""
    dark, mid, light, sheen = colors
    long, short = bs.Field(16, 16, seed, 3.0, 8.0), bs.Field(16, 16, seed + 1, 2.0, 4.0)
    frames = []
    for frame in range(FRAMES):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = 0.7 * long(x, (y - frame) % 16) + 0.3 * short(x, (y - frame) % 16)
                c = dark if v < 0.33 else mid if v < 0.6 else light
                if v > 0.72:
                    c = sheen
                img.putpixel((x, y), c + (255,))
        frames.append(img)
    return frames


# A bucket, drawn for Jugcraft in the manner of the vanilla one: an iron pail seen from a little above, its round
# mouth full of the fluid, the body narrowing to the base, lit on the left and outlined in dark iron.
IRON = {"0": (52, 52, 58), "r": (214, 214, 220), "R": (168, 168, 176), "a": (198, 198, 204), "b": (150, 150, 158),
        "c": (112, 112, 120)}
BUCKET = [
    "................",
    "................",
    "................",
    "....00000000....",
    "...0rrrrrrrr0...",
    "..0rDDDDDDDDr0..",
    "..0rLlLLLLLLR0..",
    "..0rLLLLLLLLR0..",
    "...0RRRRRRRR0...",
    "...0aabbbbbc0...",
    "...0abbbbbbc0...",
    "...0abbbbbcc0...",
    "....0abbbbc0....",
    "....0abbbcc0....",
    ".....000000.....",
    "................",
]


def bucket(colors, seed=0):
    """A bucket of the fluid: its dark, mid and light tones on the surface (shadowed under the back of the rim, a
    highlight at the front left). Every bucket shares the pail; `seed` is kept for the callers."""
    fluid = {"D": colors[0], "L": colors[1], "l": colors[2]}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BUCKET):
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), (fluid.get(key) or IRON[key]) + (255,))
    return img


def catalyst():
    """Cracking catalyst: a heap of small grey-white alumina pellets with a faint green nickel tint."""
    rng = random.Random(960)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shades = [(150, 158, 150), (186, 194, 184), (214, 222, 210), (120, 132, 122)]
    centres = [(rng.uniform(3, 13), rng.uniform(5 + abs(8 - x) * 0.4, 14)) for x in range(22)]
    for cx, cy in sorted(centres, key=lambda c: c[1]):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= 2.2:
                    shade = shades[2] if x < cx and y < cy else shades[0] if x > cx and y > cy else shades[1]
                    img.putpixel((x, y), shade + (255,))
    return img


def asphalt_binder():
    """Asphalt binder: a glossy black lump of tar with a dull sheen and a few stuck grains."""
    img = ms.lumps([(6.6, 9.6, 4.2), (10.4, 8.4, 3.6), (8.4, 6.2, 3.0)],
                    [(12, 11, 10), (24, 22, 21), (38, 35, 33), (62, 58, 55), (104, 98, 92)])
    for x, y in ((5, 11), (10, 11), (11, 7)):
        img.putpixel((x, y), (120, 112, 98, 255))
    return img


def plastic_pellets():
    """Plastic pellets: a little pile of glossy off-white nurdles, each with a bright highlight."""
    rng = random.Random(962)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    centres = [(rng.uniform(3, 13), rng.uniform(6 + abs(8 - x) * 0.35, 14)) for x in range(18)]
    for cx, cy in sorted(centres, key=lambda c: c[1]):
        for y in range(16):
            for x in range(16):
                d = (x - cx) ** 2 + (y - cy) ** 2
                if d <= 2.4:
                    c = (232, 230, 220) if d < 0.8 and x <= cx else (204, 202, 190) if y <= cy else (168, 166, 156)
                    img.putpixel((x, y), c + (255,))
    return img


def plastic_sheet():
    """Plastic sheet: a thin, slightly translucent-looking cream panel seen at an angle, with a moulded edge."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 14):
        shift = (13 - y) // 3
        for x in range(2 + shift, 13 + shift):
            edge = y in (3, 13) or x in (2 + shift, 12 + shift)
            c = (150, 146, 132) if edge else (226, 222, 204) if (x + 2 * y) % 9 else (240, 238, 224)
            if not edge and x - shift < 6 and y < 7:
                c = (246, 244, 234)
            img.putpixel((x, y), c + (255,))
    return img


def asphalt(seed=963):
    """Asphalt: dark grey-black binder in soft clumps with pale and rust-brown aggregate chips spaced across it. Tiles
    seamlessly (no edge treatment)."""
    img = Image.new("RGBA", (16, 16))
    s = bs.surface([(38, 38, 40), (46, 46, 48), (54, 54, 57)], seed, spread=0.7)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), s(x, y) + (255,))
    rng = random.Random(seed)
    taken = set()
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        if len(taken) >= 14 or any(((x + dx) % 16, (y + dy) % 16) in taken for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            continue
        taken.add((x, y))
        img.putpixel((x, y), rng.choice([(96, 94, 90), (118, 114, 106), (84, 70, 60), (70, 70, 72)]) + (255,))
    return img


def asphalt_road_line():
    """The top of a road-line block: asphalt with a dashed yellow centre line running north to south (the
    blockstate turns it to the direction the player faced)."""
    img = asphalt(964)
    for y in range(16):
        if y % 8 in (1, 2, 3, 4, 5):
            for x in (7, 8):
                shade = (226, 184, 40) if x == 7 else (204, 164, 34)
                img.putpixel((x, y), shade + (255,))
    return img


def alumina():
    """Alumina: a small heap of fine white powder with grey shading."""
    return ms.dust([(146, 150, 156), (186, 190, 196), (214, 216, 220), (236, 236, 234), (252, 252, 255)])


def fertilizer():
    """Fertilizer: a tied burlap sack with a green leaf stencilled on it and a few grey-white granules spilt."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 15):
        half = 4 + (1 if 6 <= y <= 12 else 0) - (1 if y < 5 else 0)
        for x in range(16):
            if abs(x - 7.5) <= half:
                c = (176, 146, 96) if y % 3 else (162, 132, 86)
                if x < 7.5 - half + 1:
                    c = (196, 166, 112)
                if x > 7.5 + half - 1 or y == 14:
                    c = (120, 96, 60)
                img.putpixel((x, y), c + (255,))
    for x in range(6, 10):
        img.putpixel((x, 4), (96, 76, 44, 255))
    for x, y in ((7, 8), (8, 8), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10), (7, 11)):
        img.putpixel((x, y), (60, 140, 50, 255))
    for x, y in ((2, 14), (3, 15), (13, 15), (12, 14)):
        img.putpixel((x, y), (214, 218, 210, 255))
    return img


def titanium_sponge():
    """Titanium sponge: a porous, crumbly blue-grey lump with dark pits."""
    img = ms.lumps([(6.0, 9.6, 3.8), (10.6, 8.0, 3.6), (8.2, 5.8, 2.8), (10.0, 11.6, 2.6)],
                    [(52, 56, 66), (100, 106, 120), (136, 142, 156), (170, 176, 188), (204, 210, 220)])
    for x, y in ((5, 9), (9, 6), (11, 9), (7, 12), (12, 12), (8, 9)):
        img.putpixel((x, y), (52, 56, 66, 255))
    return img


def lithium_cell():
    """A lithium cell: an upright cylinder with an aluminum cap and button terminal, a graphite wrapper with a
    shaded round edge and a green charge band (the electric look's colour)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wrap = [(30, 33, 37), (46, 50, 55), (64, 69, 75), (82, 88, 95)]
    metal = [(132, 138, 146), (190, 196, 202), (236, 238, 242)]
    green = [(34, 138, 84), (62, 204, 124), (128, 244, 172)]
    for y in range(2, 15):
        for x in range(4, 12):
            shade = 0 if x in (4, 11) else 1 if x in (5, 10) else 3 if x == 6 else 2
            if y <= 3:
                c = metal[min(2, shade)] if y == 3 else metal[0]
            elif y == 14:
                c = metal[0]
            elif 8 <= y <= 9:
                c = green[0] if x in (4, 11) else green[2] if x == 6 else green[1]
            else:
                c = wrap[shade]
            img.putpixel((x, y), c + (255,))
    for x in range(6, 10):
        img.putpixel((x, 1), metal[1] + (255,))
    img.putpixel((7, 5), (236, 238, 242, 255))
    img.putpixel((7, 6), (236, 238, 242, 255))
    img.putpixel((6, 5), (236, 238, 242, 255))
    img.putpixel((8, 5), (236, 238, 242, 255))
    return img


def neodymium_magnet():
    """A neodymium magnet: a nickel-plated horseshoe with a red north pole and a blue-grey south pole."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    plate = [(120, 126, 136), (176, 182, 190), (230, 234, 240)]
    for y in range(2, 15):
        for x in range(2, 14):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            arch = y <= 8 and 2.5 <= d <= 5.6
            leg = y > 8 and (3 <= x <= 5 or 10 <= x <= 12)
            if not (arch or leg):
                continue
            c = plate[2] if x in (3, 10) or d < 3.3 else plate[0] if x in (5, 12) or d > 5 else plate[1]
            if y >= 12:
                c = (200, 52, 48) if x <= 5 else (70, 92, 150)
                if y == 12:
                    c = (230, 96, 88) if x <= 5 else (110, 132, 190)
            img.putpixel((x, y), c + (255,))
    return img


def silicon_boule():
    """A silicon boule: a long grey-blue single crystal, lying diagonally, with a cone at the seed end and glints."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body = [(70, 78, 92), (104, 114, 130), (140, 150, 166), (190, 198, 212), (232, 236, 244)]
    for y in range(16):
        for x in range(16):
            along, across = (x + y) / 2, (x - y)
            width = 3.2 if 3 <= along <= 13 else 3.2 - (3 - along) * 1.2 if along < 3 else 3.2 - (along - 13) * 2
            if width <= 0 or abs(across) > width or along < 1 or along > 14.5:
                continue
            shade = 4 if across < -width + 1.2 else 3 if across < 0 else 2 if across < width - 1.2 else 0
            img.putpixel((x, y), body[shade] + (255,))
    return img


def silicon_wafer():
    """A silicon wafer: a thin mirror-grey disc with a flat edge, catching a cyan-violet sheen."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 6.6 or y > 13:
                continue
            c = (120, 128, 146) if d > 5.8 else (176, 184, 200)
            if abs((x - y) - 2) <= 1 and d < 5.8:
                c = (150, 210, 228)
            elif abs((x - y) + 3) <= 0 and d < 5.8:
                c = (180, 150, 230)
            img.putpixel((x, y), c + (255,))
    return img


def microchip():
    """A microchip: a black package with silver pins down both sides and a cyan die mark."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        for x in range(4, 12):
            c = (30, 32, 38) if (x, y) != (5, 4) else (90, 96, 110)
            if 6 <= x <= 9 and 6 <= y <= 9:
                c = (40, 120, 140) if (x + y) % 2 else (56, 170, 190)
            img.putpixel((x, y), c + (255,))
        if y % 2:
            for x in (2, 3, 12, 13):
                img.putpixel((x, y), (190, 196, 206, 255) if x in (3, 12) else (140, 146, 156, 255))
    return img


def rubber():
    """Synthetic rubber: a dark grey-black bale with a soft sheen and a pressed seam."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(4, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (4, 13)
            c = (24, 24, 26) if edge else (58, 58, 62) if y < 7 and x < 9 else (40, 40, 44)
            if y == 9 and 3 <= x <= 12:
                c = (30, 30, 33)
            img.putpixel((x, y), c + (255,))
    for x, y in ((4, 5), (5, 5), (6, 6)):
        img.putpixel((x, y), (92, 92, 98, 255))
    return img


def gasket():
    """Gasket: a flat black rubber ring round a steel face, seen from above."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 3 <= d <= 6.5:
                c = (36, 36, 40) if d > 5.5 else (150, 154, 160) if d < 4 else (60, 60, 66)
                if d < 4 and x < 8 and y < 8:
                    c = (196, 200, 206)
                img.putpixel((x, y), c + (255,))
    return img


def pvc_resin():
    """PVC resin: a little heap of chalk-white powder with grey shading, a warmer white than alumina."""
    return ms.dust([(150, 150, 142), (188, 188, 180), (214, 214, 206), (234, 234, 226), (250, 250, 244)])


def soap():
    """Soap: a rounded pale green bar with a pressed mark and a few bubbles."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(6, 13):
        for x in range(2, 14):
            corner = (x in (2, 13)) and (y in (6, 12))
            if corner:
                continue
            edge = x in (2, 13) or y in (6, 12)
            c = (126, 170, 130) if edge else (176, 214, 170) if y < 9 else (154, 198, 150)
            img.putpixel((x, y), c + (255,))
    for x in range(5, 11):
        img.putpixel((x, 9), (132, 180, 134, 255))
    for x, y in ((11, 3), (12, 4), (4, 4), (13, 2)):
        img.putpixel((x, y), (226, 240, 246, 255))
    return img


def guncotton():
    """Guncotton: a fluffy tuft of nitrated cotton, off-white with a faint straw tint and soft grey shadows."""
    return ms.lumps([(5.6, 10.6, 3.0), (6.6, 7.6, 3.6), (10.4, 7.2, 3.2), (10.8, 10.8, 2.8), (8.4, 11.6, 3.0)],
                     [(168, 160, 132), (206, 198, 170), (230, 224, 200), (244, 240, 222), (255, 253, 244)])


def grenade():
    """Grenade: an olive-drab steel body with a ribbed waist, a steel fuse head, a spoon down its side and a ring pin."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    olive = [(46, 54, 30), (70, 82, 44), (96, 110, 62), (128, 142, 86)]
    for y in range(6, 15):
        for x in range(4, 12):
            d = ((x - 7.5) / 4) ** 2 + ((y - 10) / 4.5) ** 2
            if d > 1:
                continue
            c = olive[3] if d < 0.25 and x < 7 and y < 10 else olive[2] if x < 8 else olive[1]
            if y in (9, 11) or x in (4, 11):
                c = olive[0]
            img.putpixel((x, y), c + (255,))
    for y in range(3, 6):  # Fuse head.
        for x in range(6, 10):
            img.putpixel((x, y), ((176, 180, 186) if x < 8 else (128, 132, 140)) + (255,))
    for y in range(4, 12):  # The spoon down the right side.
        img.putpixel((11 if y > 5 else 10, y), (150, 154, 160, 255))
    for x, y in ((3, 2), (4, 1), (5, 1), (3, 3), (4, 4), (5, 4), (6, 3)):  # Ring pin.
        img.putpixel((x, y), (200, 204, 210, 255))
    return img


def grenade_launcher():
    """Grenade launcher, held like a tool: a stubby wide steel barrel, a drum, a rubber grip and a stock."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    steel = [(44, 46, 52), (78, 82, 90), (120, 126, 136), (170, 176, 186)]
    for i in range(7):  # The barrel, diagonally up to the top right.
        x, y = 8 + i, 7 - i
        for dx, dy, c in ((0, 0, steel[3]), (-1, 0, steel[2]), (0, 1, steel[2]), (-1, 1, steel[1]), (1, 1, steel[1]),
                          (0, 2, steel[0])):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                img.putpixel((x + dx, y + dy), c + (255,))
    img.putpixel((14, 1), (20, 20, 22, 255))
    for y in range(7, 12):  # The drum.
        for x in range(5, 10):
            if (x, y) in ((5, 7), (9, 7), (5, 11), (9, 11)):
                continue
            img.putpixel((x, y), (steel[2] if y < 9 else steel[1]) + (255,))
    for x, y in ((7, 8), (6, 10), (8, 10)):
        img.putpixel((x, y), steel[0] + (255,))
    for i in range(4):  # Rubber grip and stock down to the bottom left.
        for dx in (0, 1):
            img.putpixel((4 - i + dx, 11 + i), (30, 30, 32) + (255,) if dx else (52, 52, 56, 255))
    for x, y in ((0, 14), (1, 14), (0, 15), (1, 15), (2, 15)):
        img.putpixel((x, y), steel[1] + (255,))
    return img


def turbocharger():
    """Turbocharger: a snail-shaped cast housing (dark iron) round a bright compressor wheel, with a flanged outlet."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7) ** 2 + (y - 8) ** 2) ** 0.5
            if d <= 6.5:
                c = (60, 62, 68) if d > 5.5 else (92, 96, 104) if d > 3.5 else (40, 42, 46)
                if d <= 3:
                    blade = int(((math.degrees(math.atan2(y - 8, x - 7)) + 360) % 360) // 45) % 2
                    c = (196, 200, 208) if blade else (150, 156, 166)
                if d > 3.5 and x < 7 and y < 8:
                    c = tuple(v + 24 for v in c)
                img.putpixel((x, y), c + (255,))
    for y in range(2, 7):  # The outlet and its flange.
        for x in range(11, 15):
            img.putpixel((x, y), ((120, 124, 132) if y == 2 or x == 14 else (84, 88, 96)) + (255,))
    img.putpixel((7, 8), (230, 232, 236, 255))
    return img


def draw_all(save, save_animation):
    save(turbocharger(), "item", "turbocharger")
    save(guncotton(), "item", "guncotton")
    save(grenade(), "item", "grenade")
    save(grenade_launcher(), "item", "grenade_launcher")
    save(microchip(), "item", "microchip")
    save(silicon_boule(), "item", "silicon_boule")
    save(silicon_wafer(), "item", "silicon_wafer")
    save(neodymium_magnet(), "item", "neodymium_magnet")
    save(lithium_cell(), "item", "lithium_cell")
    save(titanium_sponge(), "item", "titanium_sponge")
    save(fertilizer(), "item", "fertilizer")
    save(alumina(), "item", "alumina")
    save(asphalt(), "block", "asphalt")
    save(asphalt_road_line(), "block", "asphalt_road_line")
    save(plastic_pellets(), "item", "plastic_pellets")
    save(plastic_sheet(), "item", "plastic_sheet")
    save(catalyst(), "item", "cracking_catalyst")
    save(rubber(), "item", "rubber")
    save(gasket(), "item", "gasket")
    save(pvc_resin(), "item", "pvc_resin")
    save(soap(), "item", "soap")
    save(asphalt_binder(), "item", "asphalt_binder")
    for index, (fluid, info) in enumerate(FLUIDS.items()):
        save_animation(still(info["colors"], 800 + index), f"{fluid}_still", frametime=3)
        save_animation(flowing(info["colors"], 850 + index), f"{fluid}_flow", frametime=2)
        save(bucket(info["colors"], 900 + index), "item", f"{fluid}_bucket")
    # Gases: a still swirl only, for recipe viewers (they have no block, so nothing flows in the world).
    for index, (gas, info) in enumerate(GASES.items()):
        save_animation(still(info["colors"], 880 + index), f"{gas}_still", frametime=4)
