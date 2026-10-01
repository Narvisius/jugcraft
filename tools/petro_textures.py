"""Original textures for petrochemistry: animated still and flowing fluids, and filled buckets.

Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or recoloured. Called from
generate_textures.py.
"""
import math
import random

from PIL import Image

from petro import FLUIDS

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


def draw_all(save, save_animation):
    for index, (fluid, info) in enumerate(FLUIDS.items()):
        save_animation(still(info["colors"], 800 + index), f"{fluid}_still", frametime=3)
        save_animation(flowing(info["colors"], 850 + index), f"{fluid}_flow", frametime=2)
        save(bucket(info["colors"], 900 + index), "item", f"{fluid}_bucket")
