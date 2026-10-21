"""Arctus Vale: reuse Jugcraft's classic townsfolk UV and robe helper, never a new rig.

Library magic robe item textures and shady_wizard villager UV were inspected;
neither fits the existing classic player model. Original town helper supplies this skin.
"""
import argparse
from pathlib import Path
from town_skins import OUT, person, dress, faces, OVERLAY


def robes(skin, rng, tone, hair):
    ivory, blue, gold = (231, 226, 207), (56, 92, 158), (183, 147, 65)
    dress(ivory, gold)(skin, rng, tone, hair)
    for part in ("right_pants", "left_pants"):
        skin.fill(part, blue, sides=["front", "back", "left", "right"], rows=(8, 11))
        skin.fill(part, gold, sides=["front", "back", "left", "right"], rows=(11, 12))
    for part in ("right_arm", "left_arm"):
        skin.fill(part, blue, sides=["front", "back", "left", "right"], rows=(7, 9))
    for x in (2, 5):
        for y in range(1, 6):
            skin.dot("jacket", "front", x, y, blue)
    skin.dot("jacket", "front", 3, 3, gold)
    skin.dot("jacket", "front", 4, 3, gold)


def preview(image, destination):
    """Flat front/back UV projection of the actual texture; deliberately not a Minecraft screenshot."""
    from PIL import Image, ImageDraw, ImageFont
    canvas = Image.new("RGB", (720, 500), (21, 29, 44))
    draw = ImageDraw.Draw(canvas)
    title = ImageFont.load_default(size=23)
    body = ImageFont.load_default(size=15)
    draw.text((28, 22), "PROFESSOR ARCTUS VALE", font=title, fill=(235, 228, 207))
    draw.text((28, 55), "Master of Radiance / existing townsfolk model", font=body, fill=(163, 184, 219))
    for direction, origin in (("front", 135), ("back", 440)):
        view = Image.new("RGBA", (24, 32))
        parts = [("head", 8, 0), ("body", 8, 8), ("right_arm", 4, 8), ("left_arm", 16, 8),
                 ("right_leg", 8, 20), ("left_leg", 12, 20)]
        for part, x, y in parts:
            if direction == "back":
                if "arm" in part: x = 20-x
                if "leg" in part: x = 20-x
            for layer in (part, OVERLAY[part]):
                u, v, w, h = faces(layer)[direction]
                view.alpha_composite(image.crop((u, v, u+w, v+h)), (x, y))
        view = view.resize((168, 224), Image.Resampling.NEAREST)
        canvas.paste(view, (origin, 108), view)
        draw.text((origin+57, 350), direction.upper(), font=body, fill=(198, 208, 224))
    draw.text((28, 411), "Actual 64 x 64 skin / ivory robe, blue hem, gold trim", font=body, fill=(222, 225, 231))
    draw.text((28, 439), "Flat UV preview. Runtime lighting and 3D fit remain to verify.", font=body, fill=(147, 167, 194))
    destination.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(destination)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview-only", type=Path, help="render the existing runtime skin without rewriting it")
    args = parser.parse_args()
    if args.preview_only:
        from PIL import Image
        preview(Image.open(OUT / "academy_radiance.png").convert("RGBA"), args.preview_only)
        raise SystemExit(0)
    OUT.mkdir(parents=True, exist_ok=True)
    person(441, robes, clean=True).save(OUT / "academy_radiance.png")
