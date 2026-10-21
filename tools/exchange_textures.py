"""The Exchange's emblem (finance batch 1, docs/features/finance-exchange.md).

A 24x24 pixel medallion: a water-cooler jug with a red cap and ribbed sides, in a gold ring with a green laurel.
Original artwork, drawn here. client/ExchangeScreen draws textures/gui/exchange_emblem.png 1:1 in its header, so a
replacement must also be 24x24 pixels.
"""
from PIL import Image

# The left half; each row is mirrored to make the right, so the emblem is exactly symmetrical.
HALF = [
    "........GGGG",
    "......GGYYYY",
    "....GGYYdddd",
    "...GYYdddddd",
    "..GYdddddddd",
    "..GYddddddRR",
    ".GYdddddddRr",
    ".GYddddddddW",
    "GYDddddddWWc",
    "GYDdddddWlcc",
    "GYDddddWlhcc",
    "GYDddddWlccc",
    "GYDddddWwwww",
    "GYDddddWlccc",
    "GYDddddWlhcc",
    "GYDddddWwwww",
    ".GYDdddWlccc",
    ".GYLddddWWWW",
    "..GYLdddddLd",
    "..GYLLdddLLd",
    "...GYYLLLddd",
    "....GGYYdddd",
    "......GGYYYY",
    "........GGGG",
]
EMBLEM = [row + row[::-1] for row in HALF]

COLORS = {
    "G": (0x6B, 0x4C, 0x1B), "Y": (0xE8, 0xC4, 0x63), "D": (0xB8, 0x91, 0x3F), "d": (0x1D, 0x1B, 0x18),
    "W": (0xE8, 0xF3, 0xFC), "w": (0x5F, 0x93, 0xC6), "c": (0x8D, 0xBD, 0xE6), "l": (0xC4, 0xE0, 0xF6),
    "h": (0xFF, 0xFF, 0xFF), "b": (0x6F, 0xA2, 0xD4), "R": (0x8E, 0x2A, 0x2A), "r": (0xC9, 0x4A, 0x3A),
    "L": (0x5A, 0xA8, 0x4A),
}


def emblem():
    img = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    for y, row in enumerate(EMBLEM):
        for x, ch in enumerate(row.ljust(24, ".")[:24]):
            if ch != ".":
                img.putpixel((x, y), COLORS[ch] + (255,))
    return img


# The Market Chest, drawn as a clerk's counter (finance/MarketChestBlock, the design the contributor chose as "C"): a
# walnut body with two drawers and a slate ledger plate, brass edges, and a green baize desk top with an open ledger.
# 16x16 faces in a short palette, clean (no wear), lit from the top left; the block model is a body and an overhanging top.
COUNTER_PALETTE = {
    'k': (0x1F, 0x13, 0x09), 'v': (0x2A, 0x1A, 0x0E), 'w': (0x4A, 0x30, 0x1A), 'W': (0x6B, 0x48, 0x28),
    'b': (0xB8, 0x91, 0x3F), 'B': (0xF0, 0xD0, 0x7A), 'd': (0x6B, 0x4C, 0x1B), 'g': (0xE8, 0xC4, 0x63),
    's': (0x1D, 0x1B, 0x18), 'S': (0x35, 0x30, 0x2A), 'c': (0xEF, 0xE2, 0xC2),
    'e': (0x2F, 0x5B, 0x3A), 'E': (0x3F, 0x7A, 0x4C), 'p': (0xD6, 0xC4, 0x9B), 'P': (0xB0, 0x9A, 0x6E),
}
C_FRONT = [
 "BBBBBBBBBBBBBBBd",
 "dddddddddddddddd",
 "BWWWWWWWWWWWWWWd",
 "BwkkkkkdBkkkkkwd",
 "BwkwwwbdBwwbkwwd",
 "BwddddddBdddddwd",
 "BWWWWWWWWWWWWWWd",
 "BwSSSSSSSSSSSSwd",
 "BwSscccccccccSwd",
 "BwSsgggggggggSwd",
 "BwSssssssssssSwd",
 "BwSSSSSSSSSSSSwd",
 "BWWWWWWWWWWWWWWd",
 "Bwwwwwwwwwwwwwwd",
 "Bvvvvvvvvvvvvvvd",
 "dddddddddddddddd",
]

C_SIDE = [
 "BBBBBBBBBBBBBBBd",
 "dddddddddddddddd",
 "BWWWWWWWWWWWWWWd",
 "Bwwwwwwwwwwwwwwd",
 "Bvvvvvvvvvvvvvvd",
 "BWWWWWWWWWWWWWWd",
 "BwwwBBBBBBBBwwwd",
 "BwwwBkkkkkkdwwwd",
 "BvvvBkWWWWkdvvvd",
 "BWWWBkkkkkkdWWWd",
 "BwwwddddddddwwWd",
 "BWWWWWWWWWWWWWWd",
 "Bwwwwwwwwwwwwwwd",
 "Bvvvvvvvvvvvvvvd",
 "BWWWWWWWWWWWWWWd",
 "dddddddddddddddd",
]

C_TOP = [
 "BBBBBBBBBBBBBBBd",
 "BeeeeeeeeeeeeeEd",
 "BeppppppPpppppEd",
 "BepkkkkpPpkkkpEd",
 "BeppppppPppppkEd",
 "BepkkkkpPpkkkpEd",
 "BeppppppPpppppEd",
 "BepkkkpPPpkkkpEd",
 "BeppppppPpppppEd",
 "BeppppppPppppkEd",
 "BepkkkkpPpkkkpEd",
 "BeppppppPpppppEd",
 "BeeeeeeeeeeegbEd",
 "BeeeeeeeeeeebdEd",
 "BeeeeeeeeeeeeeEd",
 "dddddddddddddddd",
]


def counter_face(rows):
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), COUNTER_PALETTE[ch] + (255,))
    return img


def draw_blocks(save):
    """save(img, kind, name): the Market Chest's three faces."""
    save(counter_face(C_FRONT), "block", "market_chest_front")
    save(counter_face(C_SIDE), "block", "market_chest_side")
    save(counter_face(C_TOP), "block", "market_chest_top")


def draw_items(save):
    """save(img, kind, name): the Exchange Guide's icon, from its map tools/item_icons/exchange_guide.txt."""
    import item_icons
    save(item_icons.draw("exchange_guide"), "item", "exchange_guide")
    draw_blocks(save)


def draw_all(save_gui):
    """save_gui(img, name) writes textures/gui/<name>.png."""
    save_gui(emblem(), "exchange_emblem")
