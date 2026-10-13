"""Approved v3 Drone Tower runtime materials and sculpted industrial parts.

OpenAI Codex (GPT-6). Original imagegen sources remain unchanged in
art/tower-v3-sample. This native asset pipeline makes power-of-two albedo
derivatives and exports closed JSON geometry through model_writer. No drone
entity texture, model, palette or animation is read or modified.

Run python tools/tower_realism_art.py --write --preview for the complete narrow
tower pass. This module is the last material/model override in that pipeline.
"""
from pathlib import Path
import json
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as mw
import tower_realism_v3 as sample

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
PHASE_BLOCKS = (
    "reinforced_concrete", "aluminum_cladding", "carbon_composite_panel",
    "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice",
    "steel_armor_plate", "transformer_casing", "acoustic_wall_panel", "access_floor_tile",
)
# North-facing desk: the operator looks north from the keyboard at local +z.
# Client readouts use the outward +z plane, offset by 0.10px to avoid z-fighting.
DESK_SCREEN = {"x": (1.15, 14.85), "y": (16.15, 24.45), "z": 3.75,
               "overlay_z": 3.85, "normal": (0, 0, 1)}


def _tint(image, rgb, strength=.65):
    pixels = np.asarray(image.convert("RGB"), dtype=np.float32)
    luminance = pixels.mean(axis=2, keepdims=True)
    baseline = float(luminance.mean())
    result = np.array(rgb)[None, None, :] + (luminance - baseline) * strength
    return Image.fromarray(np.clip(result, 0, 255).astype(np.uint8)).convert("RGBA")


def _font(size):
    path = Path("C:/Windows/Fonts/segoeui.ttf")
    return ImageFont.truetype(str(path), size) if path.exists() else ImageFont.load_default()


def materials():
    concrete = Image.open(sample.CONCRETE_SOURCE).convert("RGBA").resize((512, 512), Image.Resampling.LANCZOS)
    steel = Image.open(sample.STEEL_SOURCE).convert("RGBA").resize((512, 512), Image.Resampling.LANCZOS)
    aluminum = sample.metal((154, 160, 160), 501, True)
    dark = _tint(steel, (39, 44, 47), .36)
    light = _tint(steel, (125, 132, 134), .5)
    out = {
        "tower_detail_steel": steel.resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_panel": _tint(steel, (67, 74, 78), .7).resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_dark": dark.resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_light_steel": light.resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_concrete": concrete.resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_copper": sample.metal((156, 100, 62), 502, True),
        "tower_detail_porcelain": sample.metal((208, 208, 193), 503),
        "tower_detail_rubber": _tint(steel, (28, 31, 32), .18).resize((256, 256), Image.Resampling.LANCZOS),
        "tower_detail_amber": sample.metal((184, 139, 49), 504),
        "tower_detail_red": sample.metal((153, 42, 34), 505),
        "tower_detail_cyan": Image.new("RGBA", (256, 256), (104, 193, 199, 255)),
        "tower_detail_amber_lamp": Image.new("RGBA", (256, 256), (238, 209, 145, 255)),
        "tower_detail_glass": _tint(steel, (31, 45, 49), .22).resize((256, 256), Image.Resampling.LANCZOS),
        "tower_fixture_opal": _tint(steel, (219, 224, 216), .035).resize((256, 256), Image.Resampling.LANCZOS),
        "tower_fixture_warm_opal": _tint(steel, (223, 207, 163), .035).resize((256, 256), Image.Resampling.LANCZOS),
        "reinforced_concrete": concrete,
        "concrete_column": concrete,
        "concrete_column_top": concrete,
        "steel_armor_plate": _tint(steel, (84, 91, 96), .8),
        "aluminum_cladding": aluminum.resize((512, 512), Image.Resampling.LANCZOS),
        "carbon_composite_panel": _tint(steel, (44, 49, 51), .4),
        "silicon_carbide_armor": _tint(steel, (76, 83, 84), .6),
        "depleted_uranium_armor": _tint(steel, (58, 69, 65), .6),
        "graphene_lattice": _tint(steel, (35, 41, 44), .4),
        "tungsten_steel_frame": _tint(steel, (76, 82, 86), .7),
        "transformer_casing": _tint(steel, (77, 84, 86), .55),
        "acoustic_wall_panel": _tint(steel, (44, 48, 49), .25),
        "access_floor_tile": _tint(steel, (89, 93, 93), .45),
    }
    # Broad joints belong to the two-metre sheet, never an outlined one-block tile.
    for name in ("steel_armor_plate", "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice", "transformer_casing"):
        draw = ImageDraw.Draw(out[name])
        draw.line((0, 511, 511, 511), fill=(34, 39, 41, 255), width=2)
        draw.line((511, 0, 511, 511), fill=(34, 39, 41, 255), width=2)
    screen = Image.new("RGBA", (256, 256), (13, 23, 27, 255))
    draw = ImageDraw.Draw(screen)
    draw.rectangle((12, 12, 244, 34), fill=(36, 88, 99, 255))
    draw.text((18, 13), "TOWER / CONTROL", font=_font(15), fill=(167, 220, 222, 255))
    for y, label in ((58, "FLEET"), (99, "ENERGY"), (140, "SUPPLY")):
        draw.text((18, y), label, font=_font(13), fill=(112, 169, 174, 255))
        draw.line((104, y + 7, 227, y + 7), fill=(37, 65, 69, 255), width=6)
        draw.line((104, y + 7, 181, y + 7), fill=(83, 160, 167, 255), width=6)
    draw.text((18, 195), "LIVE STATUS", font=_font(14), fill=(184, 149, 68, 255))
    out["tower_detail_screen"] = screen
    out["tower_detail_vent"] = dark.resize((256, 256), Image.Resampling.LANCZOS)
    # Dedicated architectural materials, not shared drone entity atlas regions.
    for name in ("steel_girder", "steel_girder_top", "armored_conduit", "armored_conduit_top",
                 "cooling_fin", "cooling_fin_top", "dock_plating", "landing_platform", "hangar_bay_door"):
        out[name] = out["tower_detail_light_steel" if name.startswith("cooling") else "tower_detail_steel"].copy()
    for name in ("ceramic_insulator", "ceramic_insulator_top"):
        out[name] = out["tower_detail_porcelain"].copy()
    for name in ("copper_busbar", "copper_busbar_top"):
        out[name] = out["tower_detail_copper"].copy()
    for name in ("furniture_frame", "furniture_rack", "furniture_desk_body", "furniture_cable_black"):
        out[name] = out["tower_detail_dark"].copy()
    for name in ("furniture_chrome", "furniture_desk", "furniture_rack_door"):
        out[name] = out["tower_detail_panel"].copy()
    for name in ("furniture_screen_a", "furniture_screen_b", "furniture_screen_c"):
        out[name] = screen.copy()
    for name in ("landing_pad_side", "supply_pickup_side", "hangar_pad_side"):
        out[name] = out["tower_detail_steel"].copy()
    out["carpet_tile"] = _tint(steel, (42, 44, 45), .2).resize((256, 256), Image.Resampling.LANCZOS)
    # Kept texture IDs now describe the actual fitting's metal, never a glowing
    # band painted across a whole metre of architecture.
    for name in ("red_light_strip", "warning_light"):
        out[name] = out["tower_detail_panel"].copy()
    for name, color in (("superconducting_conduit", (102, 183, 190, 255)),):
        image = out["tower_detail_panel"].copy()
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 105, 255, 150), fill=(22, 29, 32, 255))
        draw.rectangle((0, 113, 255, 142), fill=color)
        out[name] = image
    hazard = out["tower_detail_steel"].copy()
    draw = ImageDraw.Draw(hazard)
    for x in range(-256, 512, 80):
        draw.polygon(((x, 0), (x + 38, 0), (x + 68, 30), (x + 30, 30)), fill=(183, 140, 52, 255))
    out["hazard_plating"] = hazard
    out["intake_funnel"] = out["tower_detail_steel"].copy()
    draw = ImageDraw.Draw(out["intake_funnel"])
    draw.rectangle((42, 42, 213, 213), fill=(93, 102, 105, 255))
    draw.rectangle((60, 60, 195, 195), fill=(27, 34, 37, 255))
    out["drone_tower_core_side"] = out["tower_detail_panel"].copy()
    draw = ImageDraw.Draw(out["drone_tower_core_side"])
    draw.rectangle((22, 74, 234, 182), fill=(19, 30, 35, 255))
    draw.text((34, 88), "TOWER CORE", font=_font(24), fill=(146, 198, 202, 255))
    draw.text((34, 132), "INDUSTRIAL / SYSTEM", font=_font(15), fill=(174, 153, 96, 255))
    out["drone_tower_core_top"] = out["tower_detail_panel"].copy()
    return out


def _pad(kind, width, depth, steel):
    """High-resolution joined installed deck with contact housing and real markings."""
    density = 128
    size = (width * density, depth * density)
    image = _tint(steel.resize(size, Image.Resampling.LANCZOS), (58, 65, 68), .5)
    draw = ImageDraw.Draw(image)
    w, h = size
    amber, ink, silver = (181, 140, 54, 255), (28, 34, 36, 255), (164, 172, 172, 255)
    draw.rectangle((5, 5, w - 6, h - 6), outline=amber, width=5)
    for x in range(0, w, density):
        draw.line((x, 0, x, h), fill=(44, 50, 52, 255), width=1)
    for y in range(0, h, density):
        draw.line((0, y, w, y), fill=(44, 50, 52, 255), width=1)
    for x, y, sx, sy in ((30, 30, 1, 1), (w - 31, 30, -1, 1), (30, h - 31, 1, -1), (w - 31, h - 31, -1, -1)):
        draw.line((x, y, x + sx * 58, y), fill=silver, width=5)
        draw.line((x, y, x, y + sy * 58), fill=silver, width=5)
    cx, cy = w / 2, h / 2
    if kind == "pickup":
        draw.rectangle((cx - 58, cy - 64, cx + 58, cy + 64), fill=(84, 91, 93, 255), outline=silver, width=4)
        draw.line((cx, cy - 60, cx, cy + 60), fill=ink, width=5)
        for y in (cy - 46, cy + 46):
            draw.line((cx - 45, y, cx + 45, y), fill=amber, width=4)
    else:
        radius = min(w, h) * .31
        for a in range(0, 360, 45):
            draw.arc((cx - radius, cy - radius, cx + radius, cy + radius), a, a + 29, fill=silver, width=3)
        for r, color in ((49, silver), (43, ink), (25, (111, 123, 124, 255)), (14, (85, 171, 177, 255))):
            points = [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a))) for a in range(22, 382, 45)]
            draw.polygon(points, fill=color)
    draw.text((24, h - 42), "SUPPLY / TRANSFER" if kind == "pickup" else "AUTONOMOUS / DOCK", font=_font(14), fill=silver)
    return image


def write_textures(assets=ASSETS):
    assets = Path(assets)
    output = assets / "textures/block"
    output.mkdir(parents=True, exist_ok=True)
    textures = materials()
    for name, image in textures.items():
        image.save(output / f"{name}.png")
    import tower_art
    for kind, name in (("landing", "landing_pad"), ("landing", "hangar_pad"), ("pickup", "supply_pickup")):
        _pad(kind, 1, 1, textures["tower_detail_steel"]).resize((256, 256), Image.Resampling.LANCZOS).save(output / f"{name}.png")
    layouts = [("landing", 5, 5, "landing_pad_formed"), ("pickup", 3, 3, "supply_pickup_formed")]
    for kind, width, depth, stem in layouts:
        image = _pad(kind, width, depth, textures["tower_detail_steel"])
        for z in range(depth):
            for x in range(width):
                image.crop((x * 128, z * 128, (x + 1) * 128, (z + 1) * 128)).resize((256, 256), Image.Resampling.LANCZOS).save(
                    output / f"{stem}_{1 + z * width + x}.png")
    for kind, width, depth in tower_art.HANGAR_PAD_LAYOUTS:
        image = _pad(kind, width, depth, textures["tower_detail_steel"])
        for z in range(depth):
            for x in range(width):
                part = tower_art.hangar_pad_part(kind, width, depth, x, z)
                image.crop((x * 128, z * 128, (x + 1) * 128, (z + 1) * 128)).resize((256, 256), Image.Resampling.LANCZOS).save(output / f"hangar_pad_{part}.png")
    entity = assets / "textures/entity"
    entity.mkdir(parents=True, exist_ok=True)
    leaf = _tint(textures["steel_armor_plate"], (78, 85, 88), .8)
    leaf_draw = ImageDraw.Draw(leaf)
    # One large manufactured door sheet: a single horizontal pressed seam and
    # a restrained edge wear strip, with no tiny repeated shutter slats.
    leaf_draw.line((0, 508, 511, 508), fill=(38, 45, 48, 255), width=3)
    leaf_draw.line((0, 5, 511, 5), fill=(119, 126, 128, 255), width=2)
    leaf.save(entity / "tower_industrial_door_leaf.png")
    textures["tower_detail_panel"].save(entity / "tower_industrial_door_hardware.png")
    rail = textures["tower_detail_panel"].copy()
    draw = ImageDraw.Draw(rail)
    for x in range(-256, 512, 64):
        draw.polygon(((x, 16), (x + 32, 16), (x + 248, 232), (x + 216, 232)), fill=(183, 140, 52, 255))
    draw.rectangle((0, 232, 255, 255), fill=(25, 29, 30, 255))
    rail.save(entity / "tower_industrial_door_threshold.png")


def phase_model(block, phase):
    x, y, z = phase & 1, (phase >> 1) & 1, (phase >> 2) & 1
    element = mw.element((0, 0, 0), (16, 16, 16), block, uv=True)
    for face, spec in element["faces"].items():
        if face in ("north", "south"):
            u, v = x, 1 - y
        elif face in ("east", "west"):
            u, v = z, 1 - y
        else:
            u, v = x, z
        # North/east face conventions run their U from the positive world axis.
        if face in ("north", "east"):
            u = 1 - u
        if face == "down":
            v = 1 - v
        spec["uv"] = [u * 8, v * 8, u * 8 + 8, v * 8 + 8]
        spec["cullface"] = face
    return {"parent": "minecraft:block/block", "ambientocclusion": True, "textures": {block: f"jugcraft:block/{block}", "particle": f"jugcraft:block/{block}"}, "elements": [element]}


def _box(a, b, tex="tower_detail_steel", rotation=None):
    item = (a, b, tex)
    return item + ({"rotation": rotation},) if rotation else item


def _closed(boxes, particle="tower_detail_steel", world_uv=False):
    elements = [mw.element(a, b, tex, uv=not world_uv, rotation=opts.get("rotation"))
                for a, b, tex, *options in boxes for opts in [options[0] if options else {}]]
    mw.finish_closed(elements)
    for element in elements:
        if element.get("faces"):
            textures = {spec["texture"] for spec in element["faces"].values()}
            if textures <= {"#tower_detail_screen", "#tower_detail_cyan", "#tower_detail_amber_lamp"}:
                element["light_emission"] = 10
            elif textures <= {"#tower_fixture_opal", "#tower_fixture_warm_opal"}:
                element["light_emission"] = 15
        if element.get("rotation"):
            continue
        for face, spec in element["faces"].items():
            _, _, axis, side = mw.FACE_AXES[face]
            point = element["to"][axis] if side else element["from"][axis]
            if abs(point - (16 if side else 0)) < 1e-5 and all(0 <= element["from"][i] <= element["to"][i] <= 16 for i in range(3)):
                spec["cullface"] = face
    return {"parent": "minecraft:block/block", "ambientocclusion": True, "textures": {**{name: f"jugcraft:block/{name}" for name in mw.texture_names(boxes)},
                                                   "particle": f"jugcraft:block/{particle}"}, "elements": elements}


def girder_models():
    # Same I-section as TowerGirderBlock's collision shape. Joint hardware sits
    # inside its section rather than expanding the beam into a full cube.
    boxes = [_box((0, 0, 0), (16, 16, 3)), _box((0, 0, 13), (16, 16, 16)), _box((6, 0, 3), (10, 16, 13))]
    for x in (5.55, 10):
        boxes.append(_box((x, 5.8, 4.6), (x + .45, 10.2, 11.4), "tower_detail_panel"))
        for y in (6.8, 9.2):
            for z in (5.6, 10.4):
                a, b = (x - .25, y - .32, z - .32), (x + .7, y + .32, z + .32)
                boxes.append(_box(a, b, "tower_detail_light_steel"))
    result = {"steel_girder": _closed(boxes)}
    for axis, order in (("x", (1, 0, 2)), ("z", (0, 2, 1))):
        result[f"steel_girder_{axis}"] = _closed([([a[k] for k in order], [b[k] for k in order], tex) for a, b, tex in boxes])
    result["steel_girder_horizontal"] = {"parent": "jugcraft:block/steel_girder_x"}
    return result


TRAY_ARMS = {"straight": ("west", "east"), "corner": ("west", "north"),
             "tee": ("west", "east", "north"), "cross": ("west", "east", "north", "south"),
             "end": ("west",)}


def cable_tray(shape="straight"):
    """Continuous folded channels, including real elbow/tee/cross/closed-end profiles.

    North preserves the original straight's X direction. Every exit reaches the
    exact cell boundary with identical eight-pixel channel and cable profiles.
    """
    arms = TRAY_ARMS[shape]
    regions = [(4, 4, 12, 12)]
    for arm in arms:
        regions.append({"west": (0, 4, 4, 12), "east": (12, 4, 16, 12),
                        "north": (4, 0, 12, 4), "south": (4, 12, 12, 16)}[arm])
    b = [_box((x0, 12, z0), (x1, 12.8, z1), "tower_detail_panel") for x0, z0, x1, z1 in regions]
    occupied = {(x, z) for x0, z0, x1, z1 in regions for x in range(x0, x1) for z in range(z0, z1)}
    # Folded perimeter lips stop only at actual open exits, never midway
    # through a run. Merge collinear edges before exporting closed solids.
    edges = {}
    for x, z in occupied:
        for direction, dx, dz in (("w", -1, 0), ("e", 1, 0), ("n", 0, -1), ("s", 0, 1)):
            if (x + dx, z + dz) in occupied or not (0 <= x + dx < 16 and 0 <= z + dz < 16):
                continue
            axis, plane, start = ("z", x if dx < 0 else x + 1, z) if dx else ("x", z if dz < 0 else z + 1, x)
            edges.setdefault((axis, plane, direction), []).append(start)
    for (axis, plane, direction), positions in edges.items():
        positions = sorted(positions)
        runs = []
        for p in positions:
            if runs and runs[-1][1] == p:
                runs[-1][1] = p + 1
            else:
                runs.append([p, p + 1])
        lo, hi = (plane, plane + .7) if direction in ("w", "n") else (plane - .7, plane)
        for start, end in runs:
            a, c = ((lo, 12.8, start), (hi, 16, end)) if axis == "z" else ((start, 12.8, lo), (end, 16, hi))
            b.append(_box(a, c))
    for offset, texture in ((-1.8, "tower_detail_rubber"), (0, "tower_detail_copper"), (1.8, "tower_detail_rubber")):
        center = 8 + offset
        if "west" in arms or "east" in arms:
            b.append(_box((0 if "west" in arms else center - .45, 12.8, center - .45),
                          (16 if "east" in arms else center + .45, 13.7, center + .45), texture))
        if "north" in arms or "south" in arms:
            if shape in ("tee", "cross"):
                # Raise only the crossing itself. Every open end must retain
                # the same height as a neighbouring straight/elbow section.
                spans = ((0, 4, 12.8, 13.7), (4, 4.9, 12.8, 14.65),
                         (4.9, 11.1, 13.75, 14.65), (11.1, 12, 12.8, 14.65), (12, 16, 12.8, 13.7))
                if shape == "tee":
                    spans = ((0, 4, 12.8, 13.7), (4, 4.9, 12.8, 14.65),
                             (4.9, center - .45, 13.75, 14.65), (center - .45, center + .45, 12.8, 14.65))
                for z0, z1, y0, y1 in spans:
                    b.append(_box((center - .45, y0, z0), (center + .45, y1, z1), texture))
            else:
                b.append(_box((center - .45, 12.8, 0 if "north" in arms else center - .45),
                              (center + .45, 13.7, 16 if "south" in arms else center + .45), texture))
    for arm in arms:
        if arm in ("west", "east"):
            x = 2 if arm == "west" else 13.45
            b += [_box((x, 14.7, 4.8), (x + .55, 15.15, 11.2), "tower_detail_light_steel"),
                  _box((x, 12.8, 4.8), (x + .55, 14.7, 5.1)), _box((x, 12.8, 10.9), (x + .55, 14.7, 11.2))]
        else:
            z = 2 if arm == "north" else 13.45
            b += [_box((4.8, 14.7, z), (11.2, 15.15, z + .55), "tower_detail_light_steel"),
                  _box((4.8, 12.8, z), (5.1, 14.7, z + .55)), _box((10.9, 12.8, z), (11.2, 14.7, z + .55))]
    return b


def rack():
    b = [_box((1, 0, 2), (15, 1.1, 15)), _box((1, 14.9, 2), (15, 16, 15)),
         _box((1, 1.1, 2), (2, 14.9, 15)), _box((14, 1.1, 2), (15, 14.9, 15)),
         _box((2, 1.1, 14.4), (14, 14.9, 15), "tower_detail_dark")]
    for y in (2, 5.2, 8.4, 11.6):
        b += [_box((2.3, y, 2.7), (13.7, y + 2.55, 13.8), "tower_detail_panel"),
              _box((3, y + .5, 2.3), (8.8, y + 1.8, 2.7), "tower_detail_vent"),
              _box((11.3, y + .7, 2.2), (12.3, y + 1.5, 2.7), "tower_detail_cyan")]
        for x in (2.7, 13.3):
            b.append(_box((x - .2, y + .9, 2.2), (x + .2, y + 1.3, 2.7), "tower_detail_light_steel"))
    return b


def console():
    # Distinct real support legs, continuous worktop, recessed keyboard/control
    # bank, monitor bezel, cable loom and hardware; no hollow open-backed boxes.
    b = [_box((1.2, 0, 2), (4.1, 10.2, 14), "tower_detail_panel"),
         _box((11.9, 0, 2), (14.8, 10.2, 14), "tower_detail_panel"),
         _box((.4, 0, 1), (4.9, .75, 15)), _box((11.1, 0, 1), (15.6, .75, 15)),
         _box((.1, 10.2, 1), (15.9, 11.3, 15), "tower_detail_light_steel"),
         _box((1.5, 11.3, 7.6), (11.8, 11.8, 12.2), "tower_detail_rubber"),
         _box((12.3, 11.3, 8.6), (14.6, 11.9, 11.5), "tower_detail_panel"),
         _box((7, 11.3, 2.8), (9, 16.3, 4.1)),
         _box((.6, 15.6, 2), (15.4, 25, 3.4), "tower_detail_dark"),
         _box((1.15, 16.15, 3.4), (14.85, 24.45, 3.75), "tower_detail_glass"),
         _box((6.5, 11.3, 3.5), (7, 16, 4.0), "tower_detail_rubber")]
    for x in (2.0, 3.3, 4.6, 5.9, 7.2, 8.5, 9.8):
        for z in (8.2, 9.4, 10.6):
            b.append(_box((x, 11.8, z), (x + .85, 12, z + .7), "tower_detail_panel"))
    for x, tex in ((12.7, "tower_detail_amber"), (13.7, "tower_detail_red")):
        b.append(_box((x, 11.9, 9.3), (x + .5, 12.2, 10.1), tex))
    for x in (1.55, 14.0):
        for y in (1.6, 8.8):
            b.append(_box((x, y, 1.65), (x + .45, y + .45, 2.0), "tower_detail_light_steel"))
    return b


def console_model():
    model = _closed(console())
    for element in model["elements"]:
        face = element.get("faces", {}).get("south")
        if face and face["texture"] == "#tower_detail_glass" and abs(element["to"][2] - DESK_SCREEN["z"]) < 1e-5:
            face["uv"] = [0, 0, 16, 16]
            element["light_emission"] = 10
    return model


def floor_light():
    """Solid structural floor tile with a genuinely recessed sealed LED fitting.

    The existing block is full-solid support; only the tiny lens aperture is
    recessed visually. No fixture or fastener projects above its 16px floor.
    """
    b = [_box((0, 0, 0), (16, 14.8, 16), "tower_detail_panel"),
         _box((0, 14.8, 0), (3.4, 16, 16), "tower_detail_panel"),
         _box((12.6, 14.8, 0), (16, 16, 16), "tower_detail_panel"),
         _box((3.4, 14.8, 0), (12.6, 16, 6.3), "tower_detail_panel"),
         _box((3.4, 14.8, 9.7), (12.6, 16, 16), "tower_detail_panel"),
         _box((3.6, 14.8, 6.5), (12.4, 15, 9.5), "tower_detail_rubber"),
         _box((3.7, 15, 6.6), (4.1, 15.75, 9.4), "tower_detail_light_steel"),
         _box((11.9, 15, 6.6), (12.3, 15.75, 9.4), "tower_detail_light_steel"),
         _box((4.1, 15, 6.6), (11.9, 15.75, 7), "tower_detail_light_steel"),
         _box((4.1, 15, 9), (11.9, 15.75, 9.4), "tower_detail_light_steel"),
         _box((4.2, 15, 7.1), (11.8, 15.45, 8.9), "tower_fixture_opal")]
    for x in (3.8, 12.0):
        b.append(_box((x, 15.75, 7.75), (x + .2, 15.95, 8.25), "tower_detail_steel"))
    return b


def warning_fixture():
    """Small sealed beacon: fixing shoe, driver housing, gasket, lens and guard.

    No horizontal state is needed: the four-sided fitting works at legacy
    placements, while its physical size is about half a block rather than a cube.
    """
    b = [_box((3.5, 0, 3.5), (12.5, .8, 12.5), "tower_detail_panel"),
         _box((5, .8, 5), (11, 3, 11), "tower_detail_steel"),
         _box((5.2, 3, 5.2), (10.8, 3.4, 10.8), "tower_detail_rubber"),
         _box((5, 7.3, 5), (11, 8, 11), "tower_detail_panel")]
    b += octagonal_y(8, 8, 3.4, 7.3, 2.25, "tower_fixture_warm_opal")
    for x in (4.3, 11.1):
        for z in (4.3, 11.1):
            b += [_box((x, .8, z), (x + .6, 1.1, z + .6), "tower_detail_light_steel"),
                  _box((x + .25, 1.1, z + .08), (x + .35, 1.2, z + .52), "tower_detail_dark")]
    for x, z in ((5.2, 5.2), (10.4, 5.2), (5.2, 10.4), (10.4, 10.4)):
        b.append(_box((x, 3.4, z), (x + .4, 7.3, z + .4), "tower_detail_light_steel"))
    # A closed cable gland and short cable stub leave the driver casing at rear.
    b += [_box((7.1, 1.1, 11), (8.9, 2.5, 11.7), "tower_detail_dark"),
          _box((7.6, 1.4, 11.7), (8.4, 2.2, 12.1), "tower_detail_rubber")]
    return b


def ceiling_fixture():
    """Sealed opal ceiling luminaire with driver shell, gasket and latch clips."""
    b = [_box((2, 15.3, 4), (14, 16, 12), "tower_detail_panel"),
         _box((2.4, 14.6, 4.4), (13.6, 15.3, 11.6), "tower_detail_light_steel"),
         _box((2.7, 14.4, 4.7), (13.3, 14.6, 11.3), "tower_detail_rubber"),
         _box((3, 14.25, 5), (13, 14.4, 11), "tower_fixture_opal")]
    for x in (3.5, 11.8):
        for z in (4.25, 11.4):
            b.append(_box((x, 14.55, z), (x + .7, 15.4, z + .35), "tower_detail_steel"))
    return b


def wall_task_fixture():
    """North-facing sealed task light; its fixing back touches the intact wall at z16."""
    b = [_box((1, 4, 15.6), (15, 12, 16), "tower_detail_panel"),
         _box((1.4, 4.4, 14.1), (14.6, 11.6, 15.6), "tower_detail_light_steel"),
         _box((1.8, 4.8, 13.8), (14.2, 11.2, 14.1), "tower_detail_rubber"),
         _box((2.2, 5.2, 13.5), (13.8, 10.8, 13.8), "tower_fixture_opal")]
    for x in (2.0, 13.3):
        for y in (4.3, 11.3):
            b.append(_box((x, y, 13.8), (x + .7, y + .4, 15.65), "tower_detail_steel"))
    return b


def linear_ceiling_fixture():
    """Long sealed downward light mounted directly against a solid ceiling."""
    b = [_box((.6, 15.3, 5.5), (15.4, 16, 10.5), "tower_detail_panel"),
         _box((.9, 14.7, 5.8), (15.1, 15.3, 10.2), "tower_detail_light_steel"),
         _box((1.2, 14.5, 6.1), (14.8, 14.7, 9.9), "tower_detail_rubber"),
         _box((1.4, 14.2, 6.3), (14.6, 14.5, 9.7), "tower_fixture_opal")]
    for x in (3, 12.3):
        for z in (5.6, 10.0):
            b.append(_box((x, 14.6, z), (x + .7, 15.4, z + .4), "tower_detail_steel"))
    return b


def door_mechanism(part):
    """North-facing wall-mounted steel mechanics; backed at z12..16.

    Geometry stays within the occupied cell. The tower places these cells on
    the jamb/header, outside the aperture occupied by the dynamic leaves.
    """
    b = []

    def box(a, c, tex="tower_detail_steel"):
        b.append(_box(a, c, tex))

    def bolt(x, y, z, size=.65):
        box((x - size * .65, y - size * .65, z + .32), (x + size * .65, y + size * .65, z + .5), "tower_detail_dark")
        box((x - size / 2, y - size / 2, z), (x + size / 2, y + size / 2, z + .32), "tower_detail_light_steel")

    if part in (0, 1, 2):
        # Continuous I/box guide section. Its front rails retain a real
        # central channel rather than a solid decorative framed cube.
        box((0, 0, 12), (16, 16, 16), "tower_detail_panel")
        box((2, 0, 8.0), (5.2, 16, 12))
        box((10.8, 0, 8.0), (14, 16, 12))
        box((1.2, 0, 6.2), (6.1, 16, 8.0), "tower_detail_light_steel")
        box((9.9, 0, 6.2), (14.8, 16, 8.0), "tower_detail_light_steel")
        box((6.6, 0, 11.1), (9.4, 16, 12), "tower_detail_rubber")
        if part == 1:
            box((0, 0, 3.2), (16, 3.2, 12), "tower_detail_panel")
            box((.8, 3.2, 5.1), (4.4, 7.2, 12))
            box((11.6, 3.2, 5.1), (15.2, 7.2, 12))
            for x in (2, 14):
                bolt(x, 1.5, 2.7)
                bolt(x, 5.4, 4.6)
        elif part == 2:
            box((.5, 9, 5.3), (15.5, 15.5, 6.2), "tower_detail_panel")
            for x in (2, 14):
                for y in (10.3, 14.2):
                    bolt(x, y, 4.8)
    elif part in (3, 4):
        # Header cassette continues across x; finite endcaps are present but
        # culled at real block boundaries when another full block covers them.
        box((0, 4.0, 12), (16, 16, 16), "tower_detail_panel")
        box((0, 8.0, 7.2), (16, 16, 12))
        box((0, 6.8, 6.2), (16, 8.0, 14), "tower_detail_light_steel")
        box((0, 4.9, 10.3), (16, 6.8, 14), "tower_detail_rubber")
        box((0, 11.8, 6.7), (16, 14.2, 7.2), "tower_detail_dark")
        if part == 4:
            box((2, 7.9, 2.2), (14, 15.2, 6.7), "tower_detail_panel")
            box((3.3, 9.0, 1.1), (12.7, 14.1, 2.2), "tower_detail_dark")
            for y in (9.8, 11.3, 12.8):
                box((4.2, y, .6), (9.5, y + .5, 1.1), "tower_detail_light_steel")
            for x in (3, 13):
                for y in (9, 14):
                    bolt(x, y, 1.65)
            box((11.1, 10.5, .5), (12.0, 11.4, 1.1), "tower_detail_amber")
    else:
        box((0, 0, 0), (16, 2.4, 16), "tower_detail_panel")
        box((0, 2.4, 1.2), (16, 3.0, 3), "tower_detail_amber")
        box((0, 2.4, 5.2), (16, 2.9, 6.0), "tower_detail_light_steel")
        box((0, 2.4, 9.6), (16, 2.9, 10.4), "tower_detail_light_steel")
        box((0, 2.4, 13.8), (16, 3, 16), "tower_detail_rubber")
    return b


def pillar_models(name, boxes):
    result = {name: _closed(boxes)}
    for axis, order in (("x", (1, 0, 2)), ("z", (0, 2, 1))):
        turned = []
        for a, b, tex, *options in boxes:
            rotation = None
            if options and options[0].get("rotation"):
                old = options[0]["rotation"]
                # Each axis permutation swaps two axes: its determinant is -1,
                # so the proper rotated axis has the opposite rotation angle.
                rotation = ("xyz"[order.index("xyz".index(old[0]))], -old[1], [old[2][k] for k in order])
            turned.append(_box([a[k] for k in order], [b[k] for k in order], tex, rotation))
        result[f"{name}_{axis}"] = _closed(turned)
    return result


def octagonal_y(x, z, y0, y1, radius, texture):
    """A closed regular octagonal prism built from four overlapping native boxes.

    Their outer vertices coincide at the regular octagon corners. Distinct
    rotations create the real diagonal faces without a raster cutout silhouette.
    """
    half = radius * math.tan(math.pi / 8)
    return [_box((x - radius, y0, z - half), (x + radius, y1, z + half), texture,
                 None if angle == 0 else ("y", angle, [x, (y0 + y1) / 2, z]))
            for angle in (0, 45, -45)] + [_box((x - half, y0, z - radius), (x + half, y1, z + radius), texture)]


def utility_models():
    result = {}
    conduit = []
    for x in (3.8, 7.4, 11):
        conduit += octagonal_y(x + .9, 12, 0, 16, 1.15, "tower_detail_rubber")
    for y in (3.0, 12.1):
        conduit += [_box((2.5, y, 13.6), (14, y + .9, 15.3)),
                    _box((2.5, y, 9.9), (3.5, y + .9, 13.6)),
                    _box((13, y, 9.9), (14, y + .9, 13.6)),
                    _box((3.5, y + .15, 9.6), (13, y + .7, 10.35), "tower_detail_light_steel")]
    result.update(pillar_models("armored_conduit", conduit))
    copper = [_box((3, 0, 6.4), (7, 16, 8.3), "tower_detail_copper"),
              _box((9, 0, 6.4), (13, 16, 8.3), "tower_detail_copper")]
    for y in (2.4, 12.4):
        copper += [_box((2, y, 8.3), (14, y + 1.2, 11.3), "tower_detail_porcelain"),
                   _box((2, y, 11.3), (14, y + 1.2, 14.0))]
    result.update(pillar_models("copper_busbar", copper))
    fins = [_box((6.9, 0, 6.9), (9.1, 16, 9.1), "tower_detail_steel")]
    for y in (1.0, 3.8, 6.6, 9.4, 12.2):
        fins.append(_box((2, y, 2), (14, y + 1, 14), "tower_detail_light_steel"))
    result.update(pillar_models("cooling_fin", fins))
    ceramic = octagonal_y(8, 8, 0, 16, 1.6, "tower_detail_porcelain")
    for y in (1, 4.5, 8, 11.5):
        ceramic += octagonal_y(8, 8, y, y + .8, 4.8, "tower_detail_porcelain")
        ceramic += octagonal_y(8, 8, y + .8, y + 1.5, 3.5, "tower_detail_porcelain")
    result.update(pillar_models("ceramic_insulator", ceramic))
    return result


def chair():
    b = [_box((2, .5, 7), (14, 1.4, 9)), _box((7, .5, 2), (9, 1.4, 14)),
         _box((6.6, 1.4, 6.6), (9.4, 4.6, 9.4), "tower_detail_light_steel"),
         _box((5.9, 4.6, 5.9), (10.1, 6, 10.1), "tower_detail_panel"),
         _box((3, 6, 3), (13, 7, 13), "tower_detail_panel"),
         _box((3.3, 7, 3.3), (12.7, 8.6, 12.7), "tower_detail_rubber"),
         _box((3.5, 8, 12.8), (12.5, 19, 14), "tower_detail_panel"),
         _box((4.1, 8.7, 12.0), (11.9, 18.5, 12.8), "tower_detail_rubber"),
         _box((6.5, 18.5, 12.8), (9.5, 20.8, 13.7), "tower_detail_light_steel"),
         _box((5, 20, 12.1), (11, 22.4, 14), "tower_detail_rubber")]
    for x in (1.5, 13.5):
        b += [_box((x, 7, 8.8), (x + 1, 11.4, 10.2)),
              _box((x - .45, 11.4, 4.8), (x + 1.45, 12.2, 11), "tower_detail_rubber"),
              _box((x + .2, 8.6, 8.5), (x + .8, 9.4, 8.8), "tower_detail_light_steel")]
    # Four separate caster forks with paired closed wheels. This is an
    # industrial swivel chair rather than an upholstered cube on a post.
    for x, z in ((2.4, 7.6), (12.8, 7.6), (7.6, 2.4), (7.6, 12.8)):
        b += [_box((x - .4, .6, z - .4), (x + 1.2, 1.8, z + 1.2), "tower_detail_panel"),
              _box((x - .55, 0, z - .7), (x + .2, 1.1, z + 1.45), "tower_detail_rubber"),
              _box((x + .65, 0, z - .7), (x + 1.4, 1.1, z + 1.45), "tower_detail_rubber")]
    b += [_box((9.5, 4.9, 6.2), (13.8, 5.4, 6.8), "tower_detail_light_steel"),
          _box((13.1, 5.1, 5.5), (14.1, 5.8, 7.5), "tower_detail_rubber")]
    return b


def table():
    b = [_box((0, 9.8, 0), (48, 11.15, 48), "tower_detail_panel"),
         _box((1.5, 11.15, 1.5), (46.5, 12.2, 46.5), "tower_detail_glass"),
         _box((0, 11.15, 0), (48, 13, 1.5), "tower_detail_light_steel"),
         _box((0, 11.15, 46.5), (48, 13, 48), "tower_detail_light_steel"),
         _box((0, 11.15, 1.5), (1.5, 13, 46.5), "tower_detail_light_steel"),
         _box((46.5, 11.15, 1.5), (48, 13, 46.5), "tower_detail_light_steel")]
    for x in (3, 38):
        for z in (3, 38):
            b += [_box((x - 1, 0, z - 1), (x + 8, 1, z + 8)),
                  _box((x + 1, 1, z + 1), (x + 6, 4, z + 6), "tower_detail_panel"),
                  _box((x + 2, 4, z + 2), (x + 5, 9.8, z + 5), "tower_detail_light_steel"),
                  _box((x + 1.3, 8.7, z + 1.3), (x + 5.7, 9.8, z + 5.7), "tower_detail_dark")]
            for y in (1.8, 3.1):
                b.append(_box((x + 1.5, y, z + .7), (x + 5.5, y + .35, z + 1), "tower_detail_vent"))
    # Backed service compartments with removable doors, handle and bolt heads.
    for z in (2, 42):
        b += [_box((15, 3.8, z), (33, 9.8, z + 4), "tower_detail_dark"),
              _box((15.7, 4.5, z - .5), (32.3, 9.1, z), "tower_detail_panel"),
              _box((29, 5.3, z - .9), (30, 8.3, z - .5), "tower_detail_light_steel")]
        for x in (17, 31):
            for y in (5.3, 8.3):
                b.append(_box((x, y, z - .8), (x + .45, y + .45, z - .5), "tower_detail_light_steel"))
    # Mechanical projector cradle, faceted lens mount and actual shallow optics.
    b += [_box((18, 11.9, 18), (30, 12.3, 30), "tower_detail_dark"),
          _box((18.5, 12.3, 19), (20.2, 12.9, 29), "tower_detail_panel"),
          _box((27.8, 12.3, 19), (29.5, 12.9, 29), "tower_detail_panel")]
    # Reserve room for the writer's anti-flicker cap separation; nothing may
    # rise beyond the existing 13px hologram tabletop collision plane.
    b += octagonal_y(24, 24, 12.0, 12.4, 3.8, "tower_detail_light_steel")
    b += octagonal_y(24, 24, 12.4, 12.7, 2.6, "tower_detail_cyan")
    for x in (5, 34):
        for z in (4, 42):
            b += [_box((x, 12.2, z), (x + 9, 12.65, z + 2), "tower_detail_dark"),
                  _box((x + .4, 12.65, z + .4), (x + 8.6, 12.8, z + 1.6), "tower_detail_cyan")]
    for x, z in ((3, 18), (39, 18), (18, 3), (18, 39)):
        b += [_box((x, 12.2, z), (x + 6, 12.8, z + 9), "tower_detail_panel"),
              _box((x + .5, 12.8, z + .5), (x + 5.5, 13, z + 7), "tower_detail_screen")]
    return b


def write_table(write, assets):
    boxes = table()
    footprint = [(x, 0, z) for z in range(3) for x in range(3)]
    parts = mw.split_model("holo_table", boxes, footprint)
    textures = {name: f"jugcraft:block/{name}" for name in mw.texture_names(boxes)}
    for index, elements in enumerate(parts, 1):
        for element in elements:
            if all(spec["texture"] in ("#tower_detail_cyan", "#tower_detail_screen") for spec in element["faces"].values()):
                element["light_emission"] = 10
        write(assets / "models/block" / f"holo_table_formed_{index}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})


def pad_geometry(kind, width, depth, height):
    w, d = width * 16, depth * 16
    y = height - .55
    b = [_box((0, 0, 0), (w, y, d), {"up": "tower_pad_surface", "*": "tower_detail_steel"})]
    # Edge housings protect practical inset lamps. They remain within the
    # collision plate; their gaps and fastener relief are actual geometry.
    for x in (1, w - 1.8):
        b.append(_box((x, y, 1), (x + .8, height, d - 1), "tower_detail_panel"))
    for z in (1, d - 1.8):
        b.append(_box((1.8, y, z), (w - 1.8, height, z + .8), "tower_detail_panel"))
    for x in (4, w - 9):
        for z in (3, d - 3):
            b += [_box((x, y, z - .6), (x + 5, height, z + .6), "tower_detail_dark"),
                  _box((x + .4, y + .2, z - .3), (x + 4.6, height - .1, z + .3), "tower_detail_amber_lamp")]
    for x in (3, w - 3):
        for z in (3, d - 3):
            b.append(_box((x - .35, y, z - .35), (x + .35, height - .08, z + .35), "tower_detail_light_steel"))
    cx, cz = w / 2, d / 2
    if kind == "pickup":
        # Hatch surround and two recessed closed leaves. The client supplies
        # the moving cargo lift above this installed loading plate.
        radius = 3.9 if width == depth == 1 else 6.9
        for x in (cx - radius, cx + radius - .9):
            b.append(_box((x, y, cz - radius), (x + .9, height, cz + radius), "tower_detail_light_steel"))
        for z in (cz - radius, cz + radius - .9):
            b.append(_box((cx - radius + .9, y, z), (cx + radius - .9, height, z + .9), "tower_detail_light_steel"))
        inside = radius - 1.1
        b += [_box((cx - inside, y, cz - inside), (cx - .15, height - .2, cz + inside), "tower_detail_panel"),
              _box((cx + .15, y, cz - inside), (cx + inside, height - .2, cz + inside), "tower_detail_panel")]
        for x in (cx - inside + 1.3, cx + inside - 1.3):
            b.append(_box((x - .4, y + .1, cz - 1.1), (x + .4, height - .08, cz + 1.1), "tower_detail_dark"))
    else:
        # A machined rectangular contact cassette fits the 3px installed deck.
        # Separate rails surround genuinely lower contacts without overlapping
        # rotated cap planes that the shared writer would have to lift.
        for x in (cx - 4.4, cx + 3.6):
            b.append(_box((x, y, cz - 4.4), (x + .8, height, cz + 4.4), "tower_detail_panel"))
        for z in (cz - 4.4, cz + 3.6):
            b.append(_box((cx - 3.6, y, z), (cx + 3.6, height, z + .8), "tower_detail_panel"))
        b += [_box((cx - 3.5, y, cz - 3.5), (cx + 3.5, y + .2, cz + 3.5), "tower_detail_dark"),
              _box((cx - 1.1, y + .2, cz - 1.1), (cx + 1.1, height - .15, cz + 1.1), "tower_detail_cyan")]
        for x in (cx - 2.6, cx + 2.0):
            for z in (cz - 2.6, cz + 2.0):
                b.append(_box((x, y + .3, z), (x + .6, height - .06, z + .6), "tower_detail_copper"))
    return b


def write_pad(write, assets, kind, width, depth, stem, first_part, height):
    boxes = pad_geometry(kind, width, depth, height)
    footprint = [(x, 0, z) for z in range(depth) for x in range(width)]
    parts = mw.split_model(stem, boxes, footprint)
    names = mw.texture_names(boxes)
    for index, elements in enumerate(parts):
        part = first_part + index
        model = f"{stem}_{part}"
        tex = {name: f"jugcraft:block/{name}" for name in names if name != "tower_pad_surface"}
        tex["tower_pad_surface"] = f"jugcraft:block/{model}"
        for element in elements:
            for face, spec in element["faces"].items():
                if spec["texture"] == "#tower_pad_surface":
                    a, b = element["from"], element["to"]
                    spec["uv"] = [a[0], a[2], b[0], b[2]]
            if element.get("faces") and all(spec["texture"] in ("#tower_detail_amber_lamp", "#tower_detail_cyan") for spec in element["faces"].values()):
                element["light_emission"] = 10
        write(assets / "models/block" / f"{model}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": elements})


def write_pads(write, assets):
    import tower_art
    write_pad(write, assets, "landing", 5, 5, "landing_pad_formed", 1, 3)
    write_pad(write, assets, "pickup", 3, 3, "supply_pickup_formed", 1, 3)
    first = 1
    for kind, width, depth in tower_art.HANGAR_PAD_LAYOUTS:
        write_pad(write, assets, kind, width, depth, "hangar_pad", first, 16)
        first += width * depth
    for name, kind, height in (("landing_pad", "landing", 3), ("supply_pickup", "pickup", 3), ("hangar_pad", "landing", 16)):
        boxes = pad_geometry(kind, 1, 1, height)
        elements = mw.split_model(name, boxes, [(0, 0, 0)])[0]
        textures = {t: f"jugcraft:block/{t}" for t in mw.texture_names(boxes) if t != "tower_pad_surface"}
        textures["tower_pad_surface"] = f"jugcraft:block/{name}"
        for element in elements:
            for face, spec in element["faces"].items():
                if spec["texture"] == "#tower_pad_surface":
                    a, b = element["from"], element["to"]
                    spec["uv"] = [a[0], a[2], b[0], b[2]]
        write(assets / "models/block" / f"{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})


def write_assets(write=mw.write, assets=ASSETS):
    assets = Path(assets)
    for name in PHASE_BLOCKS:
        variants = {}
        for phase in range(8):
            model = f"{name}_phase_{phase}"
            write(assets / "models/block" / f"{model}.json", phase_model(name, phase))
            variants[f"phase={phase}"] = {"model": f"jugcraft:block/{model}"}
        write(assets / "models/block" / f"{name}.json", {"parent": f"jugcraft:block/{name}_phase_0"})
        write(assets / "blockstates" / f"{name}.json", {"variants": variants})
    models = girder_models()
    models.update(utility_models())
    models.update({"cable_tray": _closed(cable_tray(), world_uv=True), "equipment_rack": _closed(rack()),
                   "drone_depot_terminal": console_model(), "console_desk": console_model(),
                   "operator_chair": _closed(chair()),
                   "red_light_strip": _closed(floor_light()),
                   "warning_light": _closed(warning_fixture()),
                   "ceiling_light_panel": _closed(ceiling_fixture()),
                   "wall_task_light": _closed(wall_task_fixture()),
                   "linear_ceiling_light": _closed(linear_ceiling_fixture())})
    # Replace the obsolete decorative cube cage with an actual open structural
    # truss. Its bounding/collision cube remains compatible with old saves.
    truss = [_box((0, 0, 0), (2, 16, 2)), _box((14, 0, 0), (16, 16, 2)),
             _box((0, 0, 14), (2, 16, 16)), _box((14, 0, 14), (16, 16, 16)),
             _box((2, 0, 0), (14, 1.5, 2)), _box((2, 14.5, 0), (14, 16, 2)),
             _box((2, 0, 14), (14, 1.5, 16)), _box((2, 14.5, 14), (14, 16, 16))]
    for z in (2.4, 12.2):
        truss.append(_box((7.5, 1, z), (8.5, 15, z + 1.4), rotation=("z", 45, [8, 8, z + .7])))
    models["tungsten_steel_frame"] = _closed(truss)
    for name, model in models.items():
        write(assets / "models/block" / f"{name}.json", model)
    for name in ("armored_conduit", "copper_busbar", "cooling_fin", "ceramic_insulator"):
        write(assets / "blockstates" / f"{name}.json", {"variants": {
            "axis=y": {"model": f"jugcraft:block/{name}"},
            "axis=x": {"model": f"jugcraft:block/{name}_x"},
            "axis=z": {"model": f"jugcraft:block/{name}_z"}}})
    variants = {}
    for part in range(6):
        name = f"tower_door_mechanism_{part}"
        write(assets / "models/block" / f"{name}.json", _closed(door_mechanism(part)))
        for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variants[f"facing={facing},part={part}"] = {"model": f"jugcraft:block/{name}", **({"y": angle} if angle else {})}
    write(assets / "blockstates/tower_door_mechanism.json", {"variants": variants})
    write(assets / "models/block/tower_door_mechanism.json", {"parent": "jugcraft:block/tower_door_mechanism_0"})
    write(assets / "items/tower_door_mechanism.json", {"model": {"type": "minecraft:model", "model": "jugcraft:block/tower_door_mechanism_0"}})
    write_table(write, assets)
    write_pads(write, assets)
    write_surface_details(write, assets)


def write_surface_details(write=mw.write, assets=ASSETS):
    """Narrow regeneration of new fixture and connected-tray assets only."""
    assets = Path(assets)
    for name, boxes in (("wall_task_light", wall_task_fixture()), ("linear_ceiling_light", linear_ceiling_fixture())):
        write(assets / "models/block" / f"{name}.json", _closed(boxes))
        write(assets / "blockstates" / f"{name}.json", {"variants": {
            f"facing={facing}": {"model": f"jugcraft:block/{name}", **({"y": angle} if angle else {})}
            for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{name}"}})
    variants = {}
    for shape in TRAY_ARMS:
        name = "cable_tray" if shape == "straight" else f"cable_tray_{shape}"
        write(assets / "models/block" / f"{name}.json", _closed(cable_tray(shape), world_uv=True))
        for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variants[f"facing={facing},shape={shape}"] = {"model": f"jugcraft:block/{name}", **({"y": angle} if angle else {})}
    write(assets / "blockstates/cable_tray.json", {"variants": variants})


def write_all(assets=ASSETS):
    write_textures(assets)
    write_assets(assets=assets)


def fixture_preview(assets=ASSETS):
    """Readback views of actual floor/ceiling/beacon fittings and the desk front."""
    import art_check
    import armor_preview as ap
    tiles = []
    for name, view, scale, title, caption in (
            ("red_light_strip", (35, 35), 14, "Inset floor fitting", "Solid steel tile / recessed sealed opal lens"),
            ("warning_light", (35, 18), 31, "Sealed warning beacon", "Driver housing / gasket / lens / metal guard"),
            ("ceiling_light_panel", (35, -28), 24, "Sealed ceiling luminaire", "Aluminum shell / gasket / diffuser / clips"),
            ("console_desk", (160, 13), 13, "Desk from operator side", "Screen faces the keyboard and chair")):
        model = json.loads((Path(assets) / "models/block" / f"{name}.json").read_text(encoding="utf-8"))
        elements, lookup, _ = art_check._resolve(model, (Path(assets),))
        faces, _ = art_check._model_faces(elements, lookup)
        camera = ap.camera(view)[0]
        quads = [(np.array(face.pts), np.array(face.uvs), np.array(face.normal), np.asarray(Image.open(face.tex).convert("RGBA")))
                 for face in faces if np.array(face.normal) @ camera > 0]
        picture = ap.render(quads, view, scale=scale, background=(215, 216, 211), lighting="blockbench")
        tile = Image.new("RGB", (500, 540), (215, 216, 211))
        tile.paste(picture, ((500 - picture.width) // 2, 35 + max(0, (390 - picture.height) // 2)))
        draw = ImageDraw.Draw(tile)
        draw.text((20, 461), title, font=_font(24), fill=(38, 44, 46))
        draw.text((20, 502), caption, font=_font(17), fill=(66, 72, 73))
        tiles.append(tile)
    path = ROOT / "build/drone_tower/industrial_fixtures.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    ap.sheet(tiles, 2, gap=10).save(path)
    return path


def surface_detail_preview(assets=ASSETS):
    """Actual native fixture/junction geometry and all three unchanged cable materials."""
    import art_check as ac
    import armor_preview as ap
    assets = Path(assets)

    def read(name, offset=(0, 0, 0), variant=None):
        model = json.loads((assets / "models/block" / f"{name}.json").read_text(encoding="utf-8"))
        elements, lookup, _ = ac._resolve(model, (assets,))
        faces, _ = ac._model_faces(elements, lookup)
        variant = variant or {}
        turn_x, normal_x = ac._turner({"axis": "x", "angle": -variant.get("x", 0), "origin": (8, 8, 8)})
        turn_y, normal_y = ac._turner({"axis": "y", "angle": -variant.get("y", 0), "origin": (8, 8, 8)})
        return [(np.array([turn_y(turn_x(p)) for p in f.pts]) + offset, np.array(f.uvs),
                 np.array(normal_y(normal_x(f.normal))), np.array(Image.open(f.tex).convert("RGBA"))) for f in faces]

    scenes = [
        ("Wall fixture on intact backing", (35, 18), read("wall_task_light") + read("reinforced_concrete_phase_0", (0, 0, 16))),
        ("Sealed downward linear fixture", (35, -25), read("linear_ceiling_light") + read("steel_armor_plate_phase_0", (0, 16, 0))),
        ("Continuous tray elbow", (35, 35), read("cable_tray_corner")),
        ("Joined tray branch", (35, 35), read("cable_tray_tee")),
        ("Raised insulated crossing", (35, 35), read("cable_tray_cross")),
        ("Tray with closed termination", (35, 35), read("cable_tray_end")),
    ]
    for material in ("copper", "aluminum", "silver"):
        quads = read("energy_exchange_part26")
        quads += read(f"{material}_cable_core", (0, 0, -16))
        quads += read(f"{material}_cable_arm", (0, 0, -16), {"y": 180})
        quads += read(f"{material}_cable_arm", (0, 0, -16))
        scenes.append((f"Sealed gland / {material} cable", (35, 18), quads))
    tiles = []
    for title, view, quads in scenes:
        camera = ap.camera(view)[0]
        quads = [q for q in quads if np.dot(q[2], camera) > 0]
        picture = ap.render(quads, view, scale=8.0, background=(216, 218, 213), lighting="blockbench")
        tile = Image.new("RGB", (420, 380), (216, 218, 213))
        tile.paste(picture, ((420 - picture.width) // 2, 15 + (300 - picture.height) // 2))
        draw = ImageDraw.Draw(tile)
        draw.text((14, 329), title, font=_font(19), fill=(38, 44, 46))
        draw.text((14, 357), "Exported JSON + PNG / offline asset view", font=_font(14), fill=(66, 72, 73))
        tiles.append(tile)
    target = ROOT / "build/drone_tower/surface-details-native.png"
    ap.sheet(tiles, 3, gap=10).save(target)
    return target


def preview(assets=ASSETS):
    """Large material/part inspection, reading only actual exported JSON/PNGs."""
    import art_check
    import armor_preview as ap
    assets = Path(assets)
    names = ["steel_girder", "steel_girder_x", "tower_door_mechanism_0", "tower_door_mechanism_1",
             "tower_door_mechanism_2", "tower_door_mechanism_3", "tower_door_mechanism_4", "tower_door_mechanism_5",
             "armored_conduit", "copper_busbar", "cooling_fin", "ceramic_insulator", "cable_tray",
             "equipment_rack", "drone_depot_terminal", "tower_vent", "operator_chair", "holo_table_assembly",
             "landing_pad_formed_13", "supply_pickup_formed_5"]
    tiles = []
    view = (-27, 17)
    camera = ap.camera(view)[0]
    for name in names:
        part_view = (160, 17) if name == "drone_depot_terminal" else view
        camera = ap.camera(part_view)[0]
        if name == "holo_table_assembly":
            members = [(assets / "models/block" / f"holo_table_formed_{1 + z * 3 + x}.json", (x * 16, 0, z * 16))
                       for z in range(3) for x in range(3)]
            faces, _ = art_check._assembly(members, (assets,))
        else:
            model = json.loads((assets / "models/block" / f"{name}.json").read_text(encoding="utf-8"))
            elements, lookup, _ = art_check._resolve(model, (assets,))
            faces, _ = art_check._model_faces(elements, lookup)
        quads = [(np.array(face.pts), np.array(face.uvs), np.array(face.normal), np.asarray(Image.open(face.tex).convert("RGBA")))
                 for face in faces if np.array(face.normal) @ camera > 0]
        picture = ap.render(quads, part_view, scale=4.5 if name == "holo_table_assembly" else 13, background=(207, 208, 202), lighting="blockbench")
        tile = Image.new("RGB", (350, 410), (227, 227, 220))
        tile.paste(picture, ((350 - picture.width) // 2, 25 + max(0, (320 - picture.height) // 2)))
        ImageDraw.Draw(tile).text((12, 377), name.replace("tower_", "").replace("_", " "), font=_font(16), fill=(44, 49, 51))
        tiles.append(tile)
    result = ap.sheet(tiles, 4, gap=10)
    target = ROOT / "build/drone_tower/industrial_runtime_parts.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    result.save(target)
    fixture_preview(assets)
    surface_detail_preview(assets)
    return target


if __name__ == "__main__":
    write_all()
    print("Wrote approved v3 tower runtime assets; drone assets unchanged.")
