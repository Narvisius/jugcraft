"""Original textures for petrochemistry: animated still and flowing fluids, and filled buckets.

Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or recoloured. Called from
generate_textures.py.
"""
import math
import random

from PIL import Image

from petro import FLUIDS, GASES

FRAMES = 16


def still(colors, seed):
    """A slow, heavy surface: dark swells drifting across, with a thin oily sheen catching on the crests."""
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
                c = dark if v < -0.2 else mid if v < 0.25 else light
                if v > 0.55:
                    c = sheen
                img.putpixel((x, y), c + (255,))
        frames.append(img)
    return frames


def flowing(colors, seed):
    """The same oil running downhill: long streaks that slide down the texture one pixel a frame."""
    dark, mid, light, sheen = colors
    rng = random.Random(seed)
    columns = []
    for _ in range(16):
        raw = [rng.random() for _ in range(16)]
        # Smooth along the flow so each column is a few long streaks rather than speckle.
        columns.append([(raw[y] + raw[(y - 1) % 16] + raw[(y - 2) % 16]) / 3 for y in range(16)])
    frames = []
    for frame in range(FRAMES):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = columns[x][(y - frame) % 16]
                c = dark if v < 0.42 else mid if v < 0.62 else light
                if v > 0.78:
                    c = sheen
                img.putpixel((x, y), c + (255,))
        frames.append(img)
    return frames


# A bucket seen from the front and a little above: rim, handle, tapered body.
BUCKET = [
    "................",
    "....HHHHHHHH....",
    "...H........H...",
    "..H..........H..",
    "..RRRRRRRRRRRR..",
    "..RFFFFFFFFFFR..",
    "..RFFFFFFFFFFR..",
    "..RFFFFFFFFFFR..",
    "...SBBBBBBBBS...",
    "...SBBBBBBBBS...",
    "...SBBBBBBBBS...",
    "....SBBBBBBS....",
    "....SBBBBBBS....",
    ".....SSSSSS.....",
    "................",
    "................",
]
STEEL = {"H": (70, 72, 76), "R": (196, 200, 204), "S": (64, 66, 70), "B": (132, 136, 142)}


def bucket(colors, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BUCKET):
        for x, key in enumerate(row):
            if key == ".":
                continue
            if key == "F":
                c = colors[1] if rng.random() < 0.7 else colors[2]
                if x in (5, 6) and y == 5:
                    c = colors[3]
            else:
                c = STEEL[key]
                if key == "B" and x < 6:
                    c = (156, 160, 166)
            img.putpixel((x, y), c + (255,))
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
                    shade = shades[2] if x < cx and y < cy else shades[1] if (x + y) % 3 else shades[0]
                    img.putpixel((x, y), shade + (255,))
    return img


def asphalt_binder():
    """Asphalt binder: a glossy black lump of tar with a dull sheen and a few stuck grains."""
    rng = random.Random(961)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 8) / 6.5) ** 2 + ((y - 9) / 5) ** 2
            if d <= 1:
                c = (36, 32, 30) if rng.random() < 0.7 else (22, 20, 19)
                if d < 0.35 and x < 8 and y < 9:
                    c = (78, 74, 70)
                if rng.random() < 0.05:
                    c = (120, 112, 98)
                img.putpixel((x, y), c + (255,))
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
    """Asphalt: dark grey-black binder with pale and rust-brown aggregate chips, worn a little lighter in patches.
    Tiles seamlessly (no edge treatment)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = 46 + rng.randint(-5, 5) + (6 if (x * 7 + y * 3) % 11 == 0 else 0)
            img.putpixel((x, y), (base, base, base + 2, 255))
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        chip = rng.choice([(96, 94, 90), (118, 114, 106), (84, 70, 60), (70, 70, 72)])
        img.putpixel((x, y), chip + (255,))
    return img


def asphalt_road_line():
    """The top of a road-line block: asphalt with a dashed yellow centre line running north to south (the
    blockstate turns it to the direction the player faced)."""
    img = asphalt(964)
    for y in range(16):
        if y % 8 in (1, 2, 3, 4, 5):
            for x in (7, 8):
                shade = (226, 184, 40) if (x + y) % 5 else (196, 158, 34)
                img.putpixel((x, y), shade + (255,))
    return img


def alumina():
    """Alumina: a small heap of fine white powder with grey shading and a few glinting grains."""
    rng = random.Random(965)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(6, 15):
        half = (y - 5) * 0.75 + 1
        for x in range(16):
            if abs(x - 7.5) <= half:
                c = (236, 236, 232) if rng.random() < 0.6 else (212, 214, 214)
                if x > 7.5 + half - 1.5 or y == 14:
                    c = (182, 184, 188)
                if rng.random() < 0.04:
                    c = (252, 252, 255)
                img.putpixel((x, y), c + (255,))
    return img


def fertilizer():
    """Fertilizer: a tied burlap sack with a green leaf stencilled on it and a few grey-white granules spilt."""
    rng = random.Random(966)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 15):
        half = 4 + (1 if 6 <= y <= 12 else 0) - (1 if y < 5 else 0)
        for x in range(16):
            if abs(x - 7.5) <= half:
                c = (176, 146, 96) if rng.random() < 0.7 else (150, 122, 78)
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
    """Titanium sponge: a porous, crumbly blue-grey lump full of dark pits."""
    rng = random.Random(967)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 6.5) ** 2 + ((y - 8.5) / 5.5) ** 2 + rng.uniform(-0.12, 0.12)
            if d <= 1:
                c = (150, 156, 168) if rng.random() < 0.55 else (118, 124, 138)
                if rng.random() < 0.18:
                    c = (52, 56, 66)
                if d < 0.3 and x < 8 and y < 8:
                    c = (196, 202, 212)
                img.putpixel((x, y), c + (255,))
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


def draw_all(save, save_animation):
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
    save(asphalt_binder(), "item", "asphalt_binder")
    for index, (fluid, info) in enumerate(FLUIDS.items()):
        save_animation(still(info["colors"], 800 + index), f"{fluid}_still", frametime=3)
        save_animation(flowing(info["colors"], 850 + index), f"{fluid}_flow", frametime=2)
        save(bucket(info["colors"], 900 + index), "item", f"{fluid}_bucket")
    # Gases: a still swirl only, for recipe viewers (they have no block, so nothing flows in the world).
    for index, (gas, info) in enumerate(GASES.items()):
        save_animation(still(info["colors"], 880 + index), f"{gas}_still", frametime=4)
