"""The Exchange's poker tables (finance/PokerTableBlock): three designs, each 4 blocks wide, 2 deep and 1 high.

Each design is built here from boxes in model pixels over the whole 64 x 32 footprint (x across, z away from the
player who placed it), the same shapes as the contributor approved in the 3D previews (14 October 2026): the Gilded
Oval, the Saloon and the Clockwork Dealer (the showpiece, with turning gears drawn as an animated texture). The boxes
are cut into the eight blocks of the table (part = depth * 4 + across) as block models facing north; the blockstate
turns them for the other facings. The item model is the whole table at a quarter size.

    write_assets(write, rid, assets, data, lang)   models, blockstates, items, loot, recipes and names
    draw_textures(save_block)                      the textures (save_block(image, name, mcmeta or None))
"""
import math
import pathlib

from PIL import Image

MOD = "jugcraft"
DESIGNS = {
    "poker_table_oval": "Gilded Oval Poker Table",
    "poker_table_saloon": "Saloon Poker Table",
    "poker_table_clockwork": "Clockwork Dealer Poker Table",
}
BLOCKS = list(DESIGNS)
# The feature switch that loads the tables' recipes: poker is played in the town, beside the Exchange.
FEATURE = "town"
WIDTH, DEPTH = 4, 2

# Vanilla recipes, a different one for each table (the contributor's choice: vanilla, the clock in the clockwork one).
RECIPES = {
    "poker_table_oval": (["WWW", "GLG", "PPP"], {"W": "minecraft:green_wool", "G": "minecraft:gold_ingot",
                                                 "L": "minecraft:leather", "P": "minecraft:dark_oak_planks"}),
    "poker_table_saloon": (["WWW", "PPP", "S S"], {"W": "minecraft:green_wool", "P": "minecraft:dark_oak_planks",
                                                   "S": "minecraft:stick"}),
    "poker_table_clockwork": (["WWW", "BCB", "PRP"], {"W": "minecraft:green_wool", "B": "minecraft:gold_block",
                                                      "C": "minecraft:clock", "P": "minecraft:dark_oak_planks",
                                                      "R": "minecraft:redstone_block"}),
}

TEXTURES = ["felt", "walnut", "leather", "gold", "gold_dark", "filigree", "gear", "glass"]


# ---------------------------------------------------------------------------------------------------- geometry

def oval(boxes, cx, cz, rx, rz, y0, y1, tex, step=2):
    """An oval of strips step pixels deep: centre (cx, cz), half sizes rx and rz, from y0 to y1."""
    z = -rz
    while z < rz:
        zc = z + step / 2
        half = round(rx * math.sqrt(max(0.0, 1 - (zc * zc) / (rz * rz))))
        if half > 0:
            boxes.append(((cx - half, y0, cz + z), (cx + half, y1, cz + z + step), tex))
        z += step


def box(boxes, a, b, tex):
    boxes.append((tuple(a), tuple(b), tex))


def gear(boxes, cx, y, cz, r, t, vertical):
    """A gear: a hub with the animated gear face and eight teeth, flat (vertical False) or upright facing z."""
    if vertical:
        box(boxes, (cx - r + 1, y - r + 1, cz - t / 2), (cx + r - 1, y + r - 1, cz + t / 2), "gear")
        for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            box(boxes, (cx + dx * (r - 1) - 0.75, y + dy * (r - 1) - 0.75, cz - t / 2 + 0.05),
                (cx + dx * (r - 1) + 0.75 + dx * 0.6, y + dy * (r - 1) + 0.75 + dy * 0.6, cz + t / 2 - 0.05), "gold")
    else:
        box(boxes, (cx - r + 1, y - t / 2, cz - r + 1), (cx + r - 1, y + t / 2, cz + r - 1), "gear")
        for dx, dz in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            box(boxes, (cx + dx * (r - 1) - 0.75, y - t / 2 + 0.05, cz + dz * (r - 1) - 0.75),
                (cx + dx * (r - 1) + 0.75 + dx * 0.6, y + t / 2 - 0.05, cz + dz * (r - 1) + 0.75 + dz * 0.6), "gold")


def shoe(boxes, x, z, y):
    """The dealing shoe."""
    box(boxes, (x - 3, y, z - 2), (x + 3, y + 2, z + 2), "gold")
    box(boxes, (x - 3, y + 2, z), (x + 3, y + 4, z + 2), "gold")
    box(boxes, (x + 3, y + 1, z - 0.5), (x + 4.2, y + 3, z + 0.5), "gold_dark")


def design_oval():
    b = []
    for p in (12, 44):
        box(b, (p - 6, 0, 8), (p + 6, 1.5, 24), "gold")
        box(b, (p - 3, 1.5, 12), (p + 3, 10, 20), "walnut")
        box(b, (p - 5, 10, 10), (p + 5, 11.5, 22), "walnut")
    box(b, (12, 7, 15), (44, 9, 17), "walnut")
    oval(b, 32, 16, 32, 16, 11.5, 13, "walnut")
    oval(b, 32, 16, 32, 16, 13, 13.5, "gold")
    oval(b, 32, 16, 32, 16, 13.5, 16, "leather")
    oval(b, 32, 16, 28, 12.5, 13.6, 16.1, "felt")
    shoe(b, 32, 6, 16.1)
    return b


def design_saloon():
    b = []
    for lx, lz in ((3, 3), (61, 3), (3, 29), (61, 29)):
        box(b, (lx - 2, 0, lz - 2), (lx + 2, 1, lz + 2), "gold")
        box(b, (lx - 1.5, 1, lz - 1.5), (lx + 1.5, 6, lz + 1.5), "walnut")
        box(b, (lx - 2, 6, lz - 2), (lx + 2, 7.5, lz + 2), "walnut")
        box(b, (lx - 1.5, 7.5, lz - 1.5), (lx + 1.5, 12, lz + 1.5), "walnut")
    box(b, (2, 12, 0), (62, 16, 32), "walnut")
    box(b, (0, 12, 2), (2, 16, 30), "walnut")
    box(b, (62, 12, 2), (64, 16, 30), "walnut")
    box(b, (5, 15, 5), (59, 16.1, 27), "felt")
    for z0, z1 in ((4, 5), (27, 28)):
        box(b, (4, 15.9, z0), (60, 16.15, z1), "gold")
    for x0, x1 in ((4, 5), (59, 60)):
        box(b, (x0, 15.9, 5), (x1, 16.15, 27), "gold")
    for tx, tz in ((16, 1.5), (48, 1.5), (16, 30.5), (48, 30.5)):
        box(b, (tx - 4, 15.5, tz - 1), (tx + 4, 16.05, tz + 1), "gold_dark")
    for tx in (1.5, 62.5):
        box(b, (tx - 1, 15.5, 12), (tx + 1, 16.05, 20), "gold_dark")
    shoe(b, 58, 16, 16.15)
    return b


def design_clockwork():
    b = []
    oval(b, 32, 16, 16, 9, 0, 1, "gold")
    oval(b, 32, 16, 14.5, 8, 1, 2, "walnut")
    oval(b, 32, 16, 12, 6.5, 2, 10, "walnut")
    oval(b, 32, 16, 12.5, 7, 3.5, 4.3, "gold")
    oval(b, 32, 16, 12.5, 7, 8, 8.8, "gold")
    for side in (-1, 1):
        for gx, r in ((26, 3.2), (31, 2.2), (38, 3.2)):
            gear(b, gx, 6.2, 16 + side * 6.9, r, 1, True)
    oval(b, 32, 16, 18, 10, 10, 11, "gold")
    oval(b, 32, 16, 32, 16, 11, 13.2, "filigree")
    oval(b, 32, 16, 32.4, 16.4, 13.2, 13.8, "gold")
    oval(b, 32, 16, 32, 16, 13.8, 16, "leather")
    oval(b, 32, 16, 29.5, 13.5, 16, 16.3, "gold")
    oval(b, 32, 16, 28.5, 12.5, 14.1, 16.4, "felt")
    for end in (0, 64):
        d = 1 if end == 0 else -1
        x = end + d * 0.5
        box(b, (x - 1, 12, 13), (x + 1, 17, 19), "gold")
        box(b, (x - 1.5, 16, 14), (x + 1.5, 18, 18), "gold")
        box(b, (x - 0.7, 18, 15), (x + 0.7, 19.5, 17), "gold")
    # The automaton.
    oval(b, 32, 16, 6.5, 6.5, 16.4, 17, "gold", 1)
    oval(b, 32, 16, 5.5, 5.5, 17, 19.5, "walnut", 1)
    gear(b, 32, 18.2, 16, 5, 0.8, False)
    oval(b, 32, 16, 6, 6, 19.5, 20.2, "gold", 1)
    oval(b, 32, 16, 4, 4, 20.2, 23, "glass", 1)
    oval(b, 32, 16, 4.5, 4.5, 23, 23.8, "gold", 1)
    for i in range(6):
        a = i / 6 * math.pi * 2
        x, z = 32 + math.cos(a) * 3.4, 16 + math.sin(a) * 3.4
        box(b, (x - 0.6, 23.8, z - 0.6), (x + 0.6, 25.5, z + 0.6), "gold")
    box(b, (31.2, 23.8, 15.2), (32.8, 26.5, 16.8), "gold")
    gear(b, 32, 27, 16, 1.8, 0.4, False)
    # The dealing arm, pointing to the near side.
    box(b, (31.25, 17.6, 6), (32.75, 18.6, 16), "gold")
    box(b, (30.4, 16.4, 5), (33.6, 17.6, 8), "gold_dark")
    return b


BUILDERS = {"poker_table_oval": design_oval, "poker_table_saloon": design_saloon, "poker_table_clockwork": design_clockwork}


# ---------------------------------------------------------------------------------------------------- models

def _r(v):
    return round(v, 3)


def _faces(x0, y0, z0, x1, y1, z1):
    """Explicit UVs inside the 16 x 16 sprite for every face (heights above a block wrap into the next 16)."""
    base = 16 if y0 >= 16 else 0
    yb = max(0.0, min(16.0, y0 - base))
    yt = max(0.0, min(16.0, y1 - base))
    v0, v1 = _r(16 - yt), _r(16 - yb)
    return {
        "north": [_r(16 - x1), v0, _r(16 - x0), v1],
        "south": [_r(x0), v0, _r(x1), v1],
        "east": [_r(16 - z1), v0, _r(16 - z0), v1],
        "west": [_r(z0), v0, _r(z1), v1],
        "up": [_r(x0), _r(z0), _r(x1), _r(z1)],
        "down": [_r(x0), _r(16 - z1), _r(x1), _r(16 - z0)],
    }


def part_elements(boxes, part):
    """The boxes inside one block of the table, in that block's own model space (facing north)."""
    i, j = part % WIDTH, part // WIDTH
    out = []
    for (ax, ay, az), (bx, by, bz), tex in boxes:
        x0, x1 = max(ax, 16 * i), min(bx, 16 * i + 16)
        z0, z1 = max(az, 16 * j), min(bz, 16 * j + 16)
        if x1 - x0 < 0.01 or z1 - z0 < 0.01 or by - ay < 0.01:
            continue
        # Across is +x; away from the player is -z in a north-facing model.
        mx0, mx1 = x0 - 16 * i, x1 - 16 * i
        mz0, mz1 = 16 - (z1 - 16 * j), 16 - (z0 - 16 * j)
        faces = _faces(mx0, ay, mz0, mx1, by, mz1)
        out.append({"from": [_r(mx0), _r(ay), _r(mz0)], "to": [_r(mx1), _r(by), _r(mz1)],
                    "faces": {side: {"uv": uv, "texture": f"#{tex}"} for side, uv in faces.items()}})
    return out


def item_elements(boxes):
    """The whole table at half size, centred in the item's block (the display transforms shrink it further)."""
    out = []
    for (ax, ay, az), (bx, by, bz), tex in boxes:
        x0, x1 = ax / 2 - 8, bx / 2 - 8
        z0, z1 = az / 2, bz / 2
        y0, y1 = ay / 2 + 4, by / 2 + 4
        if x1 - x0 < 0.1 or z1 - z0 < 0.1 or y1 - y0 < 0.1:
            continue
        faces = _faces(x0, y0, z0, x1, y1, z1)
        out.append({"from": [_r(x0), _r(y0), _r(z0)], "to": [_r(x1), _r(y1), _r(z1)],
                    "faces": {side: {"uv": uv, "texture": f"#{tex}"} for side, uv in faces.items()}})
    return out


def write_assets(write, rid, assets, data, lang):
    assets = pathlib.Path(assets)
    data = pathlib.Path(data)
    textures = {name: rid(f"block/poker_{name}") for name in TEXTURES}
    textures["particle"] = rid("block/poker_walnut")
    for block, title in DESIGNS.items():
        boxes = BUILDERS[block]()
        variants = {}
        for part in range(WIDTH * DEPTH):
            write(assets / "models" / "block" / f"{block}_part{part}.json",
                  {"parent": "minecraft:block/block", "textures": textures, "elements": part_elements(boxes, part)})
            for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                variant = {"model": rid(f"block/{block}_part{part}")}
                if turn:
                    variant["y"] = turn
                variants[f"facing={facing},part={part}"] = variant
        write(assets / "blockstates" / f"{block}.json", {"variants": variants})
        write(assets / "models" / "item" / f"{block}.json", {
            "parent": "minecraft:block/block", "textures": textures, "elements": item_elements(boxes),
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.45, 0.45, 0.45]},
                        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.22, 0.22, 0.22]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
        write(data / MOD / "loot_table" / "blocks" / f"{block}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(block)}],
                       "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": rid(f"blocks/{block}")})
        pattern, key = RECIPES[block]
        write(data / MOD / "recipe" / f"{block}.json", {
            "fabric:load_conditions": [{"condition": f"{MOD}:feature_enabled", "feature": FEATURE}],
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": rid(block), "count": 1}})
        lang[f"block.{MOD}.{block}"] = title


# ---------------------------------------------------------------------------------------------------- textures

FELT = (0x2F, 0x5B, 0x3A)
FELT_FLECK = (0x34, 0x63, 0x41)


def _img():
    return Image.new("RGBA", (16, 16))


def _fill(img, color, x0=0, y0=0, x1=16, y1=16):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color + (255,))


def felt():
    img = _img()
    _fill(img, FELT)
    for y in range(16):
        for x in range(16):
            if (x * 7 + y * 13) % 11 == 0:
                img.putpixel((x, y), FELT_FLECK + (255,))
    return img


def walnut():
    img = _img()
    _fill(img, (0x4A, 0x30, 0x1A))
    for y in range(0, 16, 4):
        _fill(img, (0x58, 0x39, 0x1F), 0, y, 16, y + 1)
        _fill(img, (0x3D, 0x27, 0x15), 0, y + 3, 16, y + 4)
    return img


def leather():
    img = _img()
    _fill(img, (0x5A, 0x1F, 0x17))
    _fill(img, (0x6D, 0x28, 0x1E), 0, 0, 16, 2)
    _fill(img, (0x43, 0x16, 0x0F), 0, 14, 16, 16)
    for x in range(2, 16, 4):
        img.putpixel((x, 7), (0xB8, 0x91, 0x3F, 255))
    return img


def gold():
    img = _img()
    _fill(img, (0xE8, 0xC4, 0x63))
    _fill(img, (0xF6, 0xDE, 0x8A), 0, 0, 16, 1)
    _fill(img, (0xF6, 0xDE, 0x8A), 0, 0, 1, 16)
    _fill(img, (0xA7, 0x7D, 0x24), 0, 15, 16, 16)
    _fill(img, (0xA7, 0x7D, 0x24), 15, 0, 16, 16)
    return img


def gold_dark():
    img = _img()
    _fill(img, (0x8A, 0x6A, 0x2C))
    for x in range(0, 16, 4):
        _fill(img, (0xB8, 0x91, 0x3F), x, 0, x + 2, 16)
    return img


def filigree():
    img = _img()
    _fill(img, (0x3A, 0x26, 0x15))
    gold_c = (0xE8, 0xC4, 0x63, 255)
    _fill(img, (0xE8, 0xC4, 0x63), 0, 0, 16, 1)
    _fill(img, (0xE8, 0xC4, 0x63), 0, 15, 16, 16)
    for x, y in ((3, 4), (4, 3), (5, 4), (4, 5), (11, 4), (12, 3), (13, 4), (12, 5), (3, 11), (4, 10), (5, 11),
                 (4, 12), (11, 11), (12, 10), (13, 11), (12, 12), (8, 7), (7, 8), (9, 8), (8, 9)):
        img.putpixel((x, y), gold_c)
    _fill(img, (0xB8, 0x91, 0x3F), 6, 3, 10, 4)
    _fill(img, (0xB8, 0x91, 0x3F), 6, 12, 10, 13)
    return img


def glass():
    img = _img()
    _fill(img, (0xCF, 0xE8, 0xF5))
    _fill(img, (0xFF, 0xFF, 0xFF), 2, 2, 3, 8)
    _fill(img, (0xA9, 0xCF, 0xE2), 0, 15, 16, 16)
    return img


def gear_frames(frames=8):
    """A gold gear turning: eight frames, a step of 45 / frames degrees each (the gear has eight teeth)."""
    strip = Image.new("RGBA", (16, 16 * frames))
    rim = (0xE8, 0xC4, 0x63, 255)
    face = (0xB8, 0x91, 0x3F, 255)
    back = (0x6B, 0x4C, 0x1B, 255)
    hub = (0x3A, 0x26, 0x15, 255)
    for f in range(frames):
        turn = f * (math.pi / 4) / frames
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy)
                a = (math.atan2(dy, dx) - turn) % (math.pi / 4)
                tooth = a < math.pi / 8
                if r < 1.6:
                    c = hub
                elif r < 4.6:
                    c = face
                elif r < 6.0 or (r < 7.6 and tooth):
                    c = rim
                else:
                    c = back
                strip.putpixel((x, 16 * f + y), c)
    return strip


def draw_textures(save_block):
    """save_block(image, name, mcmeta): every poker table texture (the gear is animated)."""
    for name, painter in (("felt", felt), ("walnut", walnut), ("leather", leather), ("gold", gold),
                          ("gold_dark", gold_dark), ("filigree", filigree), ("glass", glass)):
        save_block(painter(), f"poker_{name}", None)
    save_block(gear_frames(), "poker_gear", {"animation": {"frametime": 3}})
