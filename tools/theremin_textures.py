"""Original textures for the Theremin (fall additions 19) (requires Pillow): figured walnut for the cabinet and legs; its
front, a speaker grille of woven cloth behind walnut bars over a brass inlay line; polished brass for the lid's edge; copper
for the antennas; black bakelite for the knobs and insulator; and the magic-eye tube, dark green glass at rest and a
glowing green fan while it plays.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
import math

from PIL import Image

from crop_textures import rgb
import block_style as bs

WALNUT = [rgb("3a2416"), rgb("4a2e1c"), rgb("5a3a24"), rgb("6a462c")]
BRASS = [rgb("8a6a24"), rgb("b08a34"), rgb("caa44a"), rgb("e4c470")]
COPPER = [rgb("8a4a2a"), rgb("b06438"), rgb("cc7a48"), rgb("e49a68")]
BAKELITE = [rgb("141210"), rgb("1e1a16"), rgb("2a2420")]
CLOTH = [rgb("a89470"), rgb("b8a480"), rgb("c4b28e")]


def noise(palette, seed, weights=None):
    """A plain 16x16 of the palette in small clumps, in the manner of the vanilla blocks (tools/block_style.py)."""
    img = Image.new("RGBA", (16, 16))
    surface = bs.surface(palette, seed, weights)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), surface(x, y) + (255,))
    return img


def walnut(seed=19001):
    """Figured walnut: bands of grain running across in waves, a dark line between every few."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            wave = int(2 * math.sin((x + seed % 7) * 0.6) + y)
            img.putpixel((x, y), (WALNUT[0] if wave % 6 == 0 else WALNUT[1 + (wave // 3) % 2]) + (255,))
    return img


def front():
    """The cabinet's front (its upper half shows): a speaker grille, cloth behind walnut bars, and a brass inlay line."""
    img = walnut(19002)
    for x in range(3, 13):
        for y in range(3, 7):
            img.putpixel((x, y), (WALNUT[0] if x % 2 == 0 else CLOTH[(x + y) % 3]) + (255,))
    for x in range(2, 14):
        img.putpixel((x, 7), BRASS[2] + (255,))
        img.putpixel((x, 2), BRASS[1] + (255,))
    return img


def brass():
    """Polished brass: light streaks along it."""
    img = noise(BRASS, 19010, [1, 3, 3, 1])
    for x in range(16):
        img.putpixel((x, 5), BRASS[3] + (255,))
        img.putpixel((x, 11), BRASS[3] + (255,))
    return img


def eye(on):
    """The magic-eye tube: dark green glass, or glowing green with the dark fan of its shadow wedge."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            angle = abs(math.degrees(math.atan2(x - 7.5, -(y - 7.5))))
            if not on:
                colour = rgb("123a1e") if d < 6 else rgb("0c2414")
            elif d < 1.5:
                colour = rgb("0a1e10")
            elif angle < 30:
                colour = rgb("1e6a32")
            else:
                colour = rgb("7af08a") if d < 5 else rgb("3ac85a")
            img.putpixel((x, y), colour + (255,))
    return img


def theremin_textures():
    return {("block", "theremin_walnut"): walnut(), ("block", "theremin_front"): front(), ("block", "theremin_brass"): brass(),
            ("block", "theremin_copper"): noise(COPPER, 19020, [1, 3, 3, 1]), ("block", "theremin_bakelite"): noise(BAKELITE, 19030),
            ("block", "theremin_eye"): eye(False), ("block", "theremin_eye_on"): eye(True)}
