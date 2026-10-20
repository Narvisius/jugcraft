"""Owner-requested walnut furniture palette, preserving the supplied pixel designs.

Always read the preserved owner originals so regeneration cannot darken an
already adapted runtime copy. Only warm wood colours change; alpha and metal
handles remain exact. The approved plank ramp is the shared colour source.
"""
from pathlib import Path

from PIL import Image

from wood_style import wood_ramp

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/owner-library/originals/Edited-2026-10-18-Part2/jimbologics/textures"
TARGET = ROOT / "src/main/resources/assets/jugcraft/textures/menagerie/ecology"
FILES = (
    "block/walnut_door_bottom.png",
    "block/walnut_door_top.png",
    "item/walnut_door.png",
    "block/walnut_trapdoor.png",
    "block/walnut_shelf.png",
    "block/farmersdelight/walnut_cabinet_front.png",
    "block/farmersdelight/walnut_cabinet_front_open.png",
    "block/farmersdelight/walnut_cabinet_side.png",
    "block/farmersdelight/walnut_cabinet_top.png",
)


def texture(name):
    image = Image.open(SOURCE / name).convert("RGBA")
    # The original's dominant light plank is red=139. Its replacement is the
    # approved walnut's base tone; retain relative shadows and bevel highlights.
    base = wood_ramp("walnut")[3]
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a and r > g + 8 and g >= b:
                pixels[x, y] = (*[round(channel * r / 139) for channel in base], a)
    return image


def write_all():
    for name in FILES:
        target = TARGET / name
        target.parent.mkdir(parents=True, exist_ok=True)
        texture(name).save(target, compress_level=0)


if __name__ == "__main__":
    write_all()
