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


def draw_all(save, save_animation):
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
