"""Realistic tower wall/hangar material study, never writes runtime assets.

OpenAI Codex (GPT-6), 8 October 2026. Native closed Minecraft JSON geometry,
split through model_writer into block-sized parts. The readback preview uses
those exported JSON parts and PNGs; it is not a generated concept illustration.

    python tools/tower_realism_v3.py

Only build/drone_tower/v3sampleassets and its sibling previews are written.
The concrete image is supplied separately; the metal materials are original,
deterministic, low-contrast procedural coatings, with geometry carrying details.
"""
from pathlib import Path
import argparse
import hashlib
import json
import math
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/drone_tower/v3sampleassets"
CONCRETE_SOURCE = ROOT / "art/tower-v3-sample/cast_concrete_albedo.png"
STEEL_SOURCE = ROOT / "art/tower-v3-sample/painted_structural_steel_albedo.png"
WIDTH, HEIGHT, DEPTH = 64, 96, 32
TEXELS_PER_BLOCK = 256


def _noise(size, seed, cell, sigma=1):
    """Smooth manufacturing variation, not a random color on every texel."""
    rng = np.random.default_rng(seed)
    w, h = size
    small = np.clip(rng.normal(128, 30, (max(2, h // cell), max(2, w // cell))), 0, 255).astype(np.uint8)
    image = Image.fromarray(small).resize(size, Image.Resampling.BICUBIC)
    if sigma:
        image = image.filter(ImageFilter.GaussianBlur(sigma))
    return (np.asarray(image, dtype=np.float32) - 128) / 30


def metal(base, seed, brushed=False):
    size = (256, 256)
    grain = _noise(size, seed, 45, 4) * 1.8 + _noise(size, seed + 1, 9, 1) * .65
    if brushed:
        rng = np.random.default_rng(seed + 2)
        line = rng.normal(0, .9, (256, 1))
        grain += np.repeat(line, 256, axis=1)
        grain += np.sin(np.arange(256)[None, :] * .009) * 2.2
    image = Image.fromarray(np.clip(np.array(base)[None, None, :] + grain[:, :, None], 0, 255).astype(np.uint8)).convert("RGBA")
    return image


def _concrete_image(source=None):
    """Source material is preserved pixel-for-pixel, mapped at two metres per tile."""
    if source:
        return Image.open(source).convert("RGBA")
    size = (1024, 1536)
    grain = _noise(size, 361, 130, 9) * 4 + _noise(size, 362, 24, 2) * 1.2
    texture = Image.fromarray(np.clip(np.array([155, 154, 149])[None, None, :] + grain[:, :, None], 0, 255).astype(np.uint8))
    texture = texture.convert("RGBA")
    draw = ImageDraw.Draw(texture)
    # Formwork lifts are two metres tall, not an outlined tile on every block.
    # Joint geometry is recessed too; these soft edges are the material itself.
    for y in (512, 1024):
        draw.line((0, y - 1, 1023, y - 1), fill=(136, 135, 131, 255), width=2)
        draw.line((0, y, 1023, y), fill=(115, 114, 111, 255), width=2)
        draw.line((0, y + 3, 1023, y + 3), fill=(165, 163, 157, 255), width=2)
    return texture


def write_materials(out, concrete, steel=None):
    textures = out / "textures/block"
    textures.mkdir(parents=True, exist_ok=True)
    materials = {
        "v3_structural_steel": metal((77, 83, 87), 41),
        "v3_steel_edge": metal((112, 117, 119), 42, True),
        "v3_rolled_aluminum": metal((159, 164, 164), 43, True),
        "v3_cladding_dark": metal((68, 75, 78), 44, True),
        "v3_cable_jacket": metal((34, 37, 37), 45),
        "v3_shadow": metal((23, 27, 28), 46),
        "v3_floor": metal((94, 97, 97), 47),
        "v3_amber_paint": metal((183, 136, 46), 48),
        "v3_amber_light": Image.new("RGBA", (256, 256), (255, 231, 170, 255)),
        "v3_red_light": Image.new("RGBA", (256, 256), (220, 64, 43, 255)),
    }
    materials["v3_concrete_atlas"] = _concrete_image(concrete)
    if steel:
        materials["v3_structural_steel"] = Image.open(steel).convert("RGBA")
    # A deck is a single restrained large sheet, with actual raised strips in geometry.
    floor = materials["v3_floor"]
    draw = ImageDraw.Draw(floor)
    for y in range(36, 256, 80):
        for x in range(20, 256, 80):
            draw.line((x, y, x + 22, y - 9), fill=(111, 113, 111, 255), width=2)
    for name, image in materials.items():
        image.save(textures / f"{name}.png")
    # Preserve the actual original albedo files unchanged. New UVs choose their
    # regions; source images are never cropped, repainted or resampled here.
    for source, name in ((concrete, "v3_concrete_atlas"), (steel, "v3_structural_steel")):
        if source:
            shutil.copy2(source, textures / f"{name}.png")
    return materials


def geometry():
    """Four metres wide, six high: large pours and a genuinely deep steel portal."""
    boxes = []

    def box(x0, y0, z0, x1, y1, z1, material="v3_structural_steel", rotation=None):
        item = ((x0, y0, z0), (x1, y1, z1), material)
        boxes.append(item + ({"rotation": rotation},) if rotation else item)

    def bolt(x, y, z, depth=.42, size=.55):
        box(x - size * .7, y - size * .7, z + depth * .75, x + size * .7, y + size * .7, z + depth, "v3_shadow")
        box(x - size / 2, y - size / 2, z, x + size / 2, y + size / 2, z + depth * .75, "v3_steel_edge")

    # Separate actual poured lifts. Their narrow 0.10px recess is only every two
    # metres; the hidden joining faces sit inside the wall, not on a decorative grid.
    for lo, hi in ((0, 31.9), (32.1, 63.9), (64.1, 96)):
        box(0, lo, 8, 13, hi, 32, "v3_concrete_atlas")
        box(51, lo, 8, 64, hi, 32, "v3_concrete_atlas")
    box(13, 0, 8, 51, 7.8, 32, "v3_concrete_atlas")
    box(13, 66, 8, 51, 96, 32, "v3_concrete_atlas")
    # Recessed rather than through-to-the-world: side liners, floor, ceiling,
    # dark rear wall and two broad service panels establish one-metre depth.
    box(13, 7.8, 8, 15.2, 66, 32, "v3_structural_steel")
    box(48.8, 7.8, 8, 51, 66, 32, "v3_structural_steel")
    box(15.2, 7.8, 7, 48.8, 9, 32, "v3_floor")
    box(15.2, 64.7, 8, 48.8, 66, 32, "v3_cladding_dark")
    box(15.2, 9, 30, 48.8, 64.7, 32, "v3_shadow")
    box(16.5, 10.2, 29.2, 31.8, 60.6, 30, "v3_cladding_dark")
    box(32.1, 10.2, 29.2, 47.5, 60.6, 30, "v3_cladding_dark")

    # Closed I-section jambs and lintel. Wide face plates carry sparse real
    # splice joints and bolts, not a tiny frame on every building block.
    for x in (12.0, 48.1):
        box(x, 7.8, 4, x + 3.9, 67.4, 5.25)
        box(x + 1.4, 7.8, 5.25, x + 2.5, 67.4, 13.8)
        box(x, 7.8, 13.8, x + 3.9, 67.4, 15)
        box(x - .65, 7.85, 3.05, x + 4.55, 11.0, 4.0)
        box(x - .45, 39.5, 3.25, x + 4.35, 44.3, 4.0)
        for yy in (8.7, 10.1, 40.3, 43.5):
            for xx in (x + .35, x + 3.55):
                bolt(xx, yy, 2.58 if yy < 12 else 2.78)
    box(12, 62, 4, 52, 67.4, 5.25)
    box(12, 64.0, 5.25, 52, 65.4, 13.8)
    box(12, 62, 13.8, 52, 67.4, 15)
    # Gussets close real corners and make the load path apparent.
    for x in (15.9, 43.8):
        box(x, 57.7, 6.1, x + 4.3, 62, 8.5)
        box(x + .9, 58.6, 5.35, x + 3.4, 61.1, 6.1)
    for x in (16, 48):
        for y in (63.0, 66.2):
            bolt(x, y, 3.5)

    # Broad aluminum rainscreen: a single 1.9m sheet, no per-block picture frame.
    # Only three real folded seams over the six-metre sample.
    box(16.3, 69.3, 6.5, 47.7, 88.2, 7.8, "v3_rolled_aluminum")
    box(16.3, 88.5, 6.5, 47.7, 94.4, 7.8, "v3_rolled_aluminum")
    for x in (16.3, 47.2):
        box(x, 69.3, 6.1, x + .5, 94.4, 7.8, "v3_steel_edge")
    box(16.3, 69.0, 5.8, 47.7, 69.3, 8.2, "v3_shadow")
    box(16.3, 88.2, 5.8, 47.7, 88.5, 8.2, "v3_shadow")
    box(16.3, 94.4, 5.8, 47.7, 94.9, 8.2, "v3_steel_edge")
    for x in (18, 46):
        for y in (71, 86.5, 90):
            bolt(x, y, 5.95, size=.34)

    # Recessed shutter tracks, narrow wear strips and a practical fixture over
    # the bay. The open aperture is the focus, not luminous block borders.
    for x in (16.4, 46.8):
        box(x, 9, 7.1, x + .8, 62, 10.5, "v3_shadow")
        box(x + .22, 9, 6.85, x + .58, 62, 7.1, "v3_steel_edge")
    box(21, 60.2, 9.5, 43, 61.7, 14, "v3_shadow")
    box(21.7, 60.45, 9.1, 42.3, 61.35, 9.5, "v3_amber_light")
    for x in (20.4, 43.0):
        box(x, 59.8, 8.8, x + .6, 62.0, 14.8)
    # Actual hazard paint only at the sill, with a repeated diagonal band on
    # the installed metal, not on surrounding concrete.
    box(15.4, 7.15, 5.8, 48.6, 7.8, 8.7, "v3_amber_paint")
    for x in (17, 21, 25, 29, 33, 37, 41, 45):
        box(x, 7.1, 5.55, x + 1.7, 7.84, 5.8, "v3_shadow")

    # Armored service bundle and real metal hangers on the right concrete pier.
    # 0.06m duct widths keep the services at a plausible human scale.
    for x in (55.0, 56.5, 58.0):
        box(x, 12, 5.9, x + .8, 91, 7.0, "v3_cable_jacket")
        box(x + .13, 12, 5.75, x + .67, 91, 5.9, "v3_cladding_dark")
    for y in (15, 40, 65, 89):
        box(53.7, y, 7, 60.1, y + 1.2, 8.0, "v3_steel_edge")
        box(53.7, y, 5.3, 54.3, y + 1.2, 7.0)
        box(59.5, y, 5.3, 60.1, y + 1.2, 7.0)
        box(54.3, y + .2, 5.1, 59.5, y + .85, 5.75)
        bolt(54, y + .6, 4.85, size=.35)
        bolt(59.8, y + .6, 4.85, size=.35)
    # Junction box, cable glands, simple red state lamp. No decorative screen grid.
    box(53.9, 29, 2.7, 60.1, 36.9, 5.1, "v3_structural_steel")
    box(54.3, 29.5, 2.2, 59.7, 36.5, 2.7, "v3_cladding_dark")
    for x in (55.2, 56.7, 58.2):
        box(x, 36.9, 3.5, x + .7, 37.7, 4.3, "v3_cable_jacket")
    box(58.0, 34.5, 1.8, 59.0, 35.5, 2.2, "v3_shadow")
    box(58.2, 34.7, 1.55, 58.8, 35.3, 1.8, "v3_red_light")
    box(54.9, 30.3, 1.9, 57.9, 31.0, 2.2, "v3_steel_edge")
    # Low-contrast formwork tie pads only once per two-metre lift.
    for x in (4.0, 60.0):
        for y in (18, 50, 82):
            box(x - .75, y - .75, 7.6, x + .75, y + .75, 8, "v3_concrete_atlas")
            box(x - .19, y - .19, 7.45, x + .19, y + .19, 7.6, "v3_cladding_dark")

    # Deep front apron, raised grip bars and a single maintenance rail show scale.
    box(0, 0, 0, 64, 3.5, 8, "v3_concrete_atlas")
    box(13, 3.5, 0, 51, 4.6, 8, "v3_floor")
    for z in (1.2, 3.3, 5.4):
        box(13.5, 4.6, z, 50.5, 4.85, z + .4, "v3_steel_edge")
    # Local stanchion, 1m high, 4cm steel sections, yellow paint used sparingly.
    for x in (3.0, 10.0):
        box(x, 3.5, .8, x + .65, 19.8, 1.45, "v3_amber_paint")
        box(x - .65, 3.5, .3, x + 1.3, 4.0, 2.0)
    box(3.0, 19.8, .8, 10.65, 20.5, 1.45, "v3_amber_paint")
    box(3.65, 11.8, .85, 10, 12.3, 1.4, "v3_amber_paint")
    return boxes


def _world_uv(element, offset, face, concrete):
    low = [element["from"][i] + offset[i] * 16 for i in range(3)]
    high = [element["to"][i] + offset[i] * 16 for i in range(3)]
    if concrete:
        # A material image covers 2m; each part reads the matching half/quarter.
        origin = [math.floor((low[k] + 1e-5) / 32) * 32 for k in range(3)]
        f = [(low[k] - origin[k]) / 2 for k in range(3)]
        t = [(high[k] - origin[k]) / 2 for k in range(3)]
        if face == "north":
            return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]]
        if face == "south":
            return [f[0], 16 - t[1], t[0], 16 - f[1]]
        if face in ("east", "west"):
            return [f[2], 16 - t[1], t[2], 16 - f[1]]
        return [f[0], f[2], t[0], t[2]]
    # Every part maps the same spatial coordinates onto a 256px/m sheet.
    # Slice boundaries coincide with texture boundaries, not decorative marks.
    local_low, local_high = element["from"], element["to"]
    if face == "north":
        return [16 - local_high[0], 16 - local_high[1], 16 - local_low[0], 16 - local_low[1]]
    if face == "south":
        return [local_low[0], 16 - local_high[1], local_high[0], 16 - local_low[1]]
    if face == "east":
        return [16 - local_high[2], 16 - local_high[1], 16 - local_low[2], 16 - local_low[1]]
    if face == "west":
        return [local_low[2], 16 - local_high[1], local_high[2], 16 - local_low[1]]
    return [local_low[0], local_low[2], local_high[0], local_high[2]]


def write_sample(out=OUT, concrete=CONCRETE_SOURCE, steel=STEEL_SOURCE):
    out = Path(out)
    materials = write_materials(out, concrete, steel)
    boxes = geometry()
    footprint = [(x, y, z) for y in range(6) for z in range(2) for x in range(4)]
    parts = mw.split_model("tower_realism_v3", boxes, footprint)
    records = []
    for index, (offset, elements) in enumerate(zip(footprint, parts)):
        if not elements:
            continue
        for element in elements:
            for face, spec in element["faces"].items():
                spec["uv"] = [round(v, 5) for v in _world_uv(element, offset, face, spec["texture"] in ("#v3_concrete_atlas", "#v3_structural_steel"))]
            if all(spec["texture"] in ("#v3_amber_light", "#v3_red_light") for spec in element["faces"].values()):
                element["light_emission"] = 12
        model = {"parent": "minecraft:block/block", "ambientocclusion": True, "textures": {name: f"jugcraft:block/{name}" for name in materials}, "elements": elements}
        name = f"tower_realism_v3_part_{index:02d}"
        mw.write(out / "models/block" / f"{name}.json", model)
        records.append({"model": f"jugcraft:block/{name}", "offset": list(offset)})
    manifest = {"author": "OpenAI Codex (GPT-6)", "status": "material/geometry sample; not applied to runtime assets",
                "dimensions_blocks": [4, 6, 2], "clear_aperture_blocks": [1.85, 3.2],
                "source_material_scale_blocks": 2, "auxiliary_texture_texels_per_block": TEXELS_PER_BLOCK,
                "texture_dimensions": {name: list(image.size) for name, image in materials.items()},
                "concrete_source": str(Path(concrete).resolve()) if concrete else None,
                "source_sha256": hashlib.sha256(Path(concrete).read_bytes()).hexdigest() if concrete else None,
                "steel_source": str(Path(steel).resolve()) if steel else None,
                "steel_source_sha256": hashlib.sha256(Path(steel).read_bytes()).hexdigest() if steel else None,
                "parts": records}
    mw.write(out / "manifest.json", manifest)
    return manifest


def readback(out=OUT):
    """Game-style faces from the exported files, not geometry() source boxes."""
    import art_check
    out = Path(out)
    manifest = json.loads((out / "manifest.json").read_text(encoding="utf-8"))
    quads = []
    cache = {}
    for record in manifest["parts"]:
        model = json.loads((out / "models" / (record["model"].split(":")[1] + ".json")).read_text(encoding="utf-8"))
        lookup = lambda ref: out / "textures/block" / (ref.lstrip("#") + ".png")
        faces, raw = art_check._model_faces(model["elements"], lookup)
        if any(min(uv) < -1e-4 or max(uv) > 16.0001 for uv in raw):
            raise ValueError(f"out-of-range UV: {record['model']}")
        offset = np.array(record["offset"]) * 16
        for face in faces:
            path = str(face.tex)
            if path not in cache:
                cache[path] = np.asarray(Image.open(path).convert("RGBA"))
            quads.append((np.array(face.pts) + offset, np.array(face.uvs), np.array(face.normal), cache[path]))
    return quads


def _font(size):
    for path in (Path("C:/Windows/Fonts/segoeui.ttf"), Path("C:/Windows/Fonts/arial.ttf")):
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def preview(out=OUT):
    import armor_preview as ap
    quads = readback(out)
    view = (-16, 9)
    camera = ap.camera(view)[0]
    visible = [quad for quad in quads if quad[2] @ camera > 1e-5]
    image = ap.render(visible, view, scale=11, background=(209, 209, 202), lighting="blockbench")
    sheet = Image.new("RGB", (image.width + 540, max(1200, image.height + 110)), (228, 227, 219))
    sheet.paste(image, (20, 80))
    draw = ImageDraw.Draw(sheet)
    draw.text((25, 17), "TOWER MATERIAL / HANGAR SAMPLE", font=_font(25), fill=(34, 40, 44))
    draw.text((25, 49), "ACTUAL EXPORTED MINECRAFT JSON + TEXTURE READBACK   /   4m W x 6m H x 2m D", font=_font(15), fill=(69, 75, 77))
    x = image.width + 60
    draw.text((x, 98), "CONTINUOUS CAST CONCRETE", font=_font(20), fill=(42, 47, 48))
    draw.text((x, 129), "2m pours / recessed construction joints", font=_font(16), fill=(71, 76, 77))
    draw.text((x, 164), "PLATED LOAD-BEARING STEEL", font=_font(20), fill=(42, 47, 48))
    draw.text((x, 195), "Deep I-sections / splice plates / 3D bolts", font=_font(16), fill=(71, 76, 77))
    draw.text((x, 230), "BROAD ALUMINUM CLADDING", font=_font(20), fill=(42, 47, 48))
    draw.text((x, 261), "Folded seams / low-contrast brushed coating", font=_font(16), fill=(71, 76, 77))
    draw.text((x, 296), "SERVICES AT HUMAN SCALE", font=_font(20), fill=(42, 47, 48))
    draw.text((x, 327), "Armored cable runs / hangers / junction box", font=_font(16), fill=(71, 76, 77))
    draw.text((x, 362), "RECESSED, LINED BAY", font=_font(20), fill=(42, 47, 48))
    draw.text((x, 393), "Real interior depth / practical overhead lamp", font=_font(16), fill=(71, 76, 77))
    # Actual exported texture crops; no concept art in this sheet.
    for y, name, label in ((455, "v3_concrete_atlas", "Concrete material crop"),
                           (675, "v3_structural_steel", "Coated steel / original albedo"),
                           (895, "v3_rolled_aluminum", "Brushed aluminum / 256px")):
        material = Image.open(Path(out) / "textures/block" / f"{name}.png").convert("RGB")
        if name == "v3_concrete_atlas":
            material = material.crop((96, 96, 352, 352))
        sheet.paste(material.resize((185, 185), Image.Resampling.LANCZOS), (x, y))
        draw.text((x + 205, y + 14), label, font=_font(15), fill=(66, 72, 74))
    path = Path(out).parent / "v3_actual_wall_sample.png"
    sheet.save(path)
    # A straight-on face proves the part assembly is continuous.
    front = [q for q in quads if q[2] @ ap.camera((0, 0))[0] > 1e-5]
    ap.render(front, (0, 0), scale=12, background=(209, 209, 202), lighting="blockbench").save(path.with_name("v3_actual_wall_front.png"))
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--concrete", type=Path, default=CONCRETE_SOURCE)
    parser.add_argument("--steel", type=Path, default=STEEL_SOURCE)
    parser.add_argument("--out", type=Path, default=OUT)
    args = parser.parse_args()
    manifest = write_sample(args.out, args.concrete, args.steel)
    path = preview(args.out)
    print(json.dumps({"sample": str(args.out), "preview": str(path), "model_parts": len(manifest["parts"]),
                      "dimensions_blocks": manifest["dimensions_blocks"]}, indent=2))


if __name__ == "__main__":
    main()
