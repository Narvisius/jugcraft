"""Art for Ember, part 1 (tools/concordance_ember.py; docs/features/arcane-concordance-ember.md).

Every pixel is drawn here by code, with no randomness, so the same run always writes the same files; nothing is traced or
copied. The spell icons follow tools/concordance_art.py: 32x32 pixel art on the round violet ground every Concordance
spell icon shares, flat fills from a few shades a material, lit from the top left, and each shows its role at a glance:

- Hearthspark (utility): a campfire of two crossed logs catching, a spark leaping above it;
- Hearthguard (defense): a figure inside a dome of banked flame;
- Cinderbolt (damage): a burning shard flying from the bottom left to the top right, embers trailing it;
- Hearthflare (damage, round you): a ring of flame tongues bursting out from a bright centre.

The Smoulder status icon (18x18, like vanilla's) is a small flame with a dark outline, no ground.

Called from tools/generate_textures.py (concordance_art.textures()).
"""
import math

from PIL import Image

import clean_metal as cm
from concordance_art import HAND, _disc, _line, _mask, _outline, _ring, _spell_ground

# Fire, from the deepest ember to the white heart of a flame.
EMBER = [(92, 26, 10), (156, 48, 16), (214, 92, 28), (240, 140, 50), (255, 196, 96), (255, 238, 170)]
# Logs: bark and the cut ends.
WOOD = [(58, 36, 20), (92, 60, 32), (128, 88, 50), (170, 128, 78)]

FLAME_MASK = [
    "....a....",
    "...aba...",
    "...abba..",
    "..abbcba.",
    "..abccba.",
    ".abccdcba",
    ".abcddcba",
    "abcdddcba",
    "abcdeedcb",
    "abcdeedcb",
    ".abcddcba",
    "..abccba.",
    "...aaaa..",
]
FLAME = {"a": EMBER[1], "b": EMBER[2], "c": EMBER[3], "d": EMBER[4], "e": EMBER[5]}


def hearthspark_icon():
    """Hearthspark: two crossed logs catching, a flame rising from them and a spark leaping above. Utility."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # The logs: two crossed bars, bark lit from the top left, a cut end on each.
    _line(img, (7, 24), (24, 19), WOOD[1], 3)
    _line(img, (8, 19), (25, 24), WOOD[2], 3)
    _line(img, (8, 19), (25, 24), WOOD[3], 1)
    for x, y in ((7, 24), (25, 24)):
        cm.put(img, x, y, WOOD[3])
    # The flame catching on them.
    _mask(img, FLAME_MASK, FLAME, 12, 8)
    # The spark: a small four-point star above the flame, and two embers.
    _mask(img, ["..a..", ".aba.", "abcba", ".aba.", "..a.."], {"a": EMBER[3], "b": EMBER[4], "c": EMBER[5]}, 20, 3)
    for x, y in ((9, 8), (24, 11)):
        cm.put(img, x, y, EMBER[4])
    return img


def hearthguard_icon():
    """Hearthguard: a small lilac figure inside a dome of banked flame, brightest at its crown. Defense."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # The figure, as Dawn Aegis draws it: rounded shoulders and a head, lit from the top left.
    for y in range(18, 26):
        for x in range(32):
            if math.hypot(x + 0.5 - 16, y + 0.5 - 26) <= 7.2:
                cm.put(img, x, y, HAND[2] if x < 16 and y < 22 else HAND[1])
    _disc(img, 16, 15, 3.2, HAND[1])
    _disc(img, 15.4, 14.4, 2.4, HAND[2])
    cm.put(img, 14, 13, HAND[3])
    # The dome of flame: a deep band, a bright inner band and tongues licking up from its crown.
    _ring(img, 16, 21, 12.5, 2.4, EMBER[1], 180, 360)
    _ring(img, 16, 21, 11.0, 1.2, EMBER[3], 195, 345)
    _ring(img, 16, 21, 11.0, 1.2, EMBER[5], 245, 295)
    for x, top in ((10, 7), (16, 5), (22, 7)):
        cm.put(img, x, top, EMBER[3])
        cm.put(img, x, top + 1, EMBER[2])
    for x in (4, 27):
        cm.put(img, x, 21, EMBER[2])
        cm.put(img, x, 22, EMBER[1])
    return img


def cinderbolt_icon():
    """Cinderbolt: a burning shard flying from the bottom left to the top right, embers trailing behind it. Damage."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # The trail: a wide deep-red band fading back, a narrower orange core.
    _line(img, (5, 26), (19, 12), EMBER[1], 3)
    _line(img, (8, 23), (20, 11), EMBER[3], 1)
    # The shard: a white-hot diamond with a burning rim.
    _mask(img, ["...a...", "..aba..", ".abcba.", "abcdcba", ".abcba.", "..aba..", "...a..."],
           {"a": EMBER[2], "b": EMBER[3], "c": EMBER[4], "d": EMBER[5]}, 18, 4)
    # Embers shed along the trail.
    for x, y in ((9, 18), (6, 21), (13, 22), (11, 15), (4, 23)):
        cm.put(img, x, y, EMBER[4])
    return img


def hearthflare_icon():
    """Hearthflare: tongues of flame bursting out in a ring from a white-hot centre. Damage, all round you."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # Eight tongues, alternately long and short, each a deep base and a bright tip.
    for index, angle in enumerate(range(0, 360, 45)):
        rad = math.radians(angle - 90)
        reach = 12.5 if index % 2 == 0 else 10.0
        base = (round(16 + math.cos(rad) * 5.0), round(16 + math.sin(rad) * 5.0))
        mid = (round(16 + math.cos(rad) * (reach - 3.0)), round(16 + math.sin(rad) * (reach - 3.0)))
        tip = (round(16 + math.cos(rad) * reach), round(16 + math.sin(rad) * reach))
        _line(img, base, mid, EMBER[2], 2)
        _line(img, mid, tip, EMBER[4], 1)
    # The burst's heart: rings of heat round a white centre.
    _disc(img, 16, 16, 5.6, EMBER[1])
    _disc(img, 16, 16, 4.4, EMBER[3])
    _disc(img, 15.6, 15.6, 3.0, EMBER[4])
    _disc(img, 15.4, 15.4, 1.6, EMBER[5])
    return img


def smoulder_icon():
    """The Smoulder status: a small flame, 18x18 like vanilla's status icons, with a dark outline."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    _mask(img, FLAME_MASK, FLAME, 4, 2)
    # A darker glowing ember bed under the flame.
    for x in range(5, 13):
        cm.put(img, x, 15, EMBER[0] if x in (5, 12) else EMBER[1])
    return _outline(img, (40, 12, 6))


SPELLS = {"hearthspark": hearthspark_icon, "hearthguard": hearthguard_icon, "cinderbolt": cinderbolt_icon,
          "hearthflare": hearthflare_icon}


def textures():
    """Every texture as {(kind, name): image}: the four spell icons and the Smoulder status icon."""
    out = {("spell", name): draw() for name, draw in SPELLS.items()}
    out[("mob_effect", "smoulder")] = smoulder_icon()
    return out
