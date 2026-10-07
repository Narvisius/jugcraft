"""Plastic building blocks in sixteen colours (batch 27, docs/features/gear-and-plastic.md), after Mekanism:
Additions' plastic blocks (MIT; no code or art taken). Keep COLORS in sync with chemistry/PetroBlocks.java;
tools/check_mod_data.py checks it.

Eight plastic sheets around a dye make eight blocks: a use for the plastic the oil line makes.
"""
from PIL import Image

MOD = "jugcraft"
# Dye colour id -> (display word, base RGB). Our own picks, close to each dye's hue.
COLORS = {
    "white": ("White", (236, 238, 240)), "orange": ("Orange", (232, 122, 34)), "magenta": ("Magenta", (190, 70, 180)),
    "light_blue": ("Light Blue", (92, 172, 222)), "yellow": ("Yellow", (240, 206, 52)), "lime": ("Lime", (128, 196, 46)),
    "pink": ("Pink", (236, 142, 172)), "gray": ("Gray", (78, 84, 90)), "light_gray": ("Light Gray", (156, 158, 154)),
    "cyan": ("Cyan", (34, 146, 154)), "purple": ("Purple", (120, 50, 168)), "blue": ("Blue", (54, 70, 168)),
    "brown": ("Brown", (118, 76, 44)), "green": ("Green", (82, 108, 38)), "red": ("Red", (166, 46, 40)),
    "black": ("Black", (30, 32, 36)),
}


def block_id(color):
    return f"{color}_plastic"


def blocks():
    return [block_id(color) for color in COLORS]


def texture(rgb):
    """Smooth moulded plastic: an even colour with a soft sheen across the top left and a faint moulded rim."""
    img = Image.new("RGBA", (16, 16))

    def shade(f):
        return tuple(max(0, min(255, int(c * f))) for c in rgb) + (255,)
    for y in range(16):
        for x in range(16):
            f = 1.0
            if x in (0, 15) or y in (0, 15):
                f = 0.9  # moulded rim
            elif x + y < 9:
                f = 1.06  # sheen
            elif x + y > 24:
                f = 0.96
            img.putpixel((x, y), shade(f))
    for i in range(2, 6):
        img.putpixel((i, 2), shade(1.14))  # a highlight streak
    return img


def draw_all(save):
    for color, (_, rgb) in COLORS.items():
        save(texture(rgb), "block", block_id(color))


def write_all(write, assets, data, lang, condition, self_drop):
    for color, (word, _) in COLORS.items():
        name = block_id(color)
        lang[f"block.{MOD}.{name}"] = f"{word} Plastic"
        write(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        write(assets / "models" / "block" / f"{name}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{name}"}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{name}"}})
        write(data / "loot_table" / "blocks" / f"{name}.json", self_drop(name))
        write(data / "recipe" / f"{name}.json", {
            "fabric:load_conditions": condition("crude_oil"), "type": "minecraft:crafting_shaped",
            "category": "building", "pattern": ["PPP", "PDP", "PPP"],
            "key": {"P": f"{MOD}:plastic_sheet", "D": f"minecraft:{color}_dye"},
            "result": {"id": f"{MOD}:{name}", "count": 8}})
