"""Industrial Drone Tower art, generated independently of drone artwork.

All geometry is exported through the shared closed-model writer. The final
tower_realism_runtime override applies the owner-approved realistic v3 albedos,
continuous architectural surfaces and physical hardware. Existing IDs stay stable.
Owner-library references: concrete0.png, concrete_brick0.png, sheetmetal_steel.png,
steel_scaffolding.png (art/owner-library/originals/Blocks); newly painted, no copied pixels.

python tools/tower_realism_art.py --write --preview
Only this feature's textures/models/recipes/loot/language are written by that command.
"""
from pathlib import Path
import argparse
import json
import math

from PIL import Image, ImageDraw

import clean_metal as cm
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data/jugcraft"
STEEL = [(34, 40, 48), (51, 59, 68), (75, 85, 95), (104, 115, 125), (141, 151, 158)]
LIGHT = [(83, 93, 101), (116, 126, 132), (150, 159, 164), (182, 190, 194), (211, 216, 216)]
CONCRETE = [(94, 99, 99), (117, 122, 121), (141, 146, 142), (158, 163, 158), (178, 182, 176)]
DARK = [(19, 24, 29), (29, 36, 42), (42, 50, 57), (61, 70, 78), (84, 94, 102)]
COPPER = [(91, 48, 30), (132, 71, 41), (173, 103, 61), (210, 141, 91), (231, 170, 116)]
PORCELAIN = [(125, 132, 131), (164, 170, 164), (201, 207, 195), (225, 231, 215), (242, 244, 228)]
RED = [(63, 20, 22), (105, 27, 31), (169, 42, 46), (221, 74, 66), (249, 130, 102)]
AMBER = [(82, 56, 23), (140, 97, 29), (204, 151, 42), (238, 190, 69), (255, 220, 127)]
CYAN = [(18, 47, 58), (26, 89, 104), (46, 148, 163), (91, 203, 210), (183, 239, 232)]

DETAIL_BLOCKS = {
    "tower_service_grating": "Tower Maintenance Grating",
    "tower_service_railing": "Tower Safety Railing",
    "tower_vent": "Tower Ventilation Cabinet",
    "tower_ladder": "Tower Caged Maintenance Ladder",
    "tower_door_mechanism": "Industrial Hangar Door Mechanism",
}
CRAFTING = {
    "tower_service_grating": (["S S", "SSS", "S S"], {"S": "#c:ingots/steel"}, 8),
    "tower_service_railing": (["SSS", "S S", "PPP"], {"S": "#c:ingots/steel", "P": "#c:plates/steel"}, 6),
    "tower_vent": (["PPP", "SFS", "PPP"], {"P": "#c:plates/steel", "S": "#c:ingots/steel", "F": "jugcraft:cooling_fin"}, 4),
    "tower_ladder": (["S S", "SSS", "S S"], {"S": "#c:plates/steel"}, 6),
    "tower_door_mechanism": (["PPP", "SCS", "PPP"], {"P": "#c:plates/steel", "S": "#c:ingots/steel", "C": "jugcraft:basic_circuit"}, 4),
}


def _flat(palette, ribs=False):
    image = cm.canvas(palette[2])
    if ribs:
        for y in range(0, 16, 4):
            cm.rect(image, 0, y, 15, y, palette[3])
            cm.rect(image, 0, y + 3, 15, y + 3, palette[1])
    else:
        cm.rect(image, 0, 3, 15, 4, palette[3])
        cm.rect(image, 0, 13, 15, 14, palette[1])
    return image


def _sheet(palette, courses=False):
    image = cm.canvas(palette[2])
    cm.sheet(image, palette[3], palette[2], palette[1])
    if courses:
        for y in (4, 8, 12):
            cm.rect(image, 0, y - 1, 15, y - 1, palette[1])
            cm.rect(image, 0, y, 15, y, palette[3])
    else:
        cm.rect(image, 2, 3, 13, 4, palette[3])
    return image


def _concrete():
    image = _sheet(CONCRETE, True)
    for y, x in ((2, 3), (6, 11), (10, 3), (14, 11)):
        cm.put(image, x, y, CONCRETE[0])
        cm.put(image, x + 1, y, CONCRETE[1])
    # Short formwork board ends are staggered across the same continuous courses.
    for x, y in ((7, 1), (15, 5), (7, 9), (15, 13)):
        cm.rect(image, x, y, x, y + 1, CONCRETE[1])
    return image


def _composite():
    image = _sheet(DARK)
    for y in (4, 8, 12):
        for x in range(2, 14, 4):
            cm.rect(image, x + ((y // 4) % 2), y, x + 1 + ((y // 4) % 2), y + 1, DARK[3])
    return image


def _ceramic():
    image = _sheet(STEEL, True)
    for y in range(0, 16, 4):
        x = 4 if y % 8 else 12
        cm.rect(image, x, y, x, y + 3, STEEL[1])
    return image


def _light(palette, vertical=False):
    image = _sheet(DARK)
    cm.rect(image, 0, 6, 15, 9, palette[1])
    cm.rect(image, 0, 7, 15, 8, palette[3])
    return image.transpose(Image.Transpose.ROTATE_90) if vertical else image


def _door():
    image = _flat(STEEL, True)
    cm.rect(image, 0, 14, 15, 15, DARK[1])
    for x in range(16):
        cm.put(image, x, 14, AMBER[2] if (x // 4) % 2 else DARK[0])
    return image


def _floor():
    image = _sheet(STEEL)
    for y in (3, 11):
        for x in (3, 11):
            cm.rect(image, x, y, x + 2, y, STEEL[3])
            cm.rect(image, x + 1, y + 1, x + 3, y + 1, STEEL[1])
    return image


def _screen():
    image = cm.canvas(DARK[0])
    cm.rect(image, 1, 1, 14, 1, CYAN[3])
    cm.rect(image, 2, 4, 7, 4, CYAN[2])
    cm.rect(image, 2, 6, 11, 6, CYAN[1])
    cm.rect(image, 2, 8, 9, 8, CYAN[2])
    cm.rect(image, 2, 11, 5, 13, AMBER[2])
    cm.rect(image, 8, 11, 13, 11, CYAN[3])
    cm.rect(image, 8, 13, 11, 13, CYAN[1])
    return image


def textures():
    """Fresh block-only textures; no drone entity sprite or palette is modified."""
    materials = {
        "steel": STEEL, "light_steel": LIGHT, "dark": DARK, "concrete": CONCRETE,
        "copper": COPPER, "porcelain": PORCELAIN, "rubber": DARK, "amber": AMBER,
        "red": RED, "cyan": CYAN, "panel": STEEL,
    }
    out = {f"tower_detail_{name}": _flat(p) for name, p in materials.items()}
    out["tower_detail_screen"] = _screen()
    out["tower_detail_vent"] = _flat(DARK, True)
    out["tower_detail_glass"] = _sheet(DARK)
    out["reinforced_concrete"] = _concrete()
    out["concrete_column"] = _concrete()
    out["concrete_column_top"] = _sheet(CONCRETE)
    out["steel_girder"] = _flat(STEEL)
    out["steel_girder_top"] = _flat(STEEL)
    out["aluminum_cladding"] = _sheet(LIGHT, True)
    out["tungsten_steel_frame"] = _sheet(STEEL)
    out["carbon_composite_panel"] = _composite()
    out["silicon_carbide_armor"] = _ceramic()
    out["depleted_uranium_armor"] = _sheet([(29, 37, 39), (44, 53, 54), (65, 76, 75), (89, 101, 98), (118, 131, 125)])
    out["graphene_lattice"] = _composite()
    out["red_light_strip"] = _light(RED)
    out["superconducting_conduit"] = _light(CYAN, True)
    out["warning_light"] = _light(RED)
    out["hangar_bay_door"] = _door()
    out["acoustic_wall_panel"] = _sheet(DARK, True)
    out["access_floor_tile"] = _sheet(STEEL)
    out["carpet_tile"] = _flat(DARK, True)
    out["transformer_casing"] = _sheet(STEEL)
    out["cooling_fin"] = _flat(LIGHT, True)
    out["cooling_fin_top"] = _flat(LIGHT, True)
    out["ceramic_insulator"] = _flat(PORCELAIN, True)
    out["ceramic_insulator_top"] = _flat(PORCELAIN)
    out["copper_busbar"] = _flat(COPPER)
    out["copper_busbar_top"] = _flat(COPPER)
    out["armored_conduit"] = _flat(STEEL, True)
    out["armored_conduit_top"] = _flat(STEEL)
    out["dock_plating"] = _floor()
    out["landing_platform"] = _floor()
    glass = Image.new("RGBA", (16, 16), (53, 72, 81, 88))
    draw = ImageDraw.Draw(glass)
    draw.line((0, 0, 15, 0), fill=STEEL[3] + (255,))
    draw.line((0, 0, 0, 15), fill=STEEL[3] + (255,))
    draw.line((0, 15, 15, 15), fill=STEEL[1] + (255,))
    draw.line((15, 0, 15, 15), fill=STEEL[1] + (255,))
    draw.line((2, 8, 8, 2), fill=(130, 174, 183, 130))
    draw.line((6, 13, 13, 6), fill=(130, 174, 183, 130))
    out["blast_glass"] = glass
    out["drone_tower_core_side"] = _light(CYAN)
    out["drone_tower_core_top"] = _sheet(STEEL)
    # Building textures keep chunky 16 texels per block, stored at 32px for the existing HD set.
    return {name: image if name.startswith("tower_detail_") else image.resize((32, 32), Image.Resampling.NEAREST)
            for name, image in out.items()}


def _model(boxes, particle="tower_detail_steel", ambient=True):
    elements = [mw.element(frm, to, texture, uv=True, rotation=opts.get("rotation"))
                for frm, to, texture, *options in boxes for opts in [options[0] if options else {}]]
    used = mw.texture_names(boxes)
    model = {"parent": "minecraft:block/block", "ambientocclusion": ambient, "textures": {name: f"jugcraft:block/{name}" for name in used},
             "elements": elements}
    model["textures"]["particle"] = f"jugcraft:block/{particle}"
    for element in elements:
        if all(face["texture"] in ("#tower_detail_screen", "#tower_detail_cyan")
               for face in element["faces"].values()):
            element["light_emission"] = 10
    mw.finish_closed(elements)
    return model


def _box(x0, y0, z0, x1, y1, z1, texture="tower_detail_steel"):
    return ((x0, y0, z0), (x1, y1, z1), texture)


def _recessed(material, frame="tower_detail_steel"):
    """Closed cube outline with one-pixel-deep real recessed wall panels on every face."""
    boxes = [_box(1, 1, 1, 15, 15, 15, material)]
    # Edge members seal the outline; no transparent texels fake the recesses.
    for axis in range(3):
        other = [k for k in range(3) if k != axis]
        for a in (0, 15):
            for b in (0, 15):
                frm, to = [0, 0, 0], [16, 16, 16]
                frm[other[0]], to[other[0]] = a, a + 1
                frm[other[1]], to[other[1]] = b, b + 1
                boxes.append((frm, to, frame))
    return boxes


def girder_models():
    boxes = [_box(0, 0, 0, 16, 16, 3), _box(0, 0, 13, 16, 16, 16),
             _box(6, 0, 3, 10, 16, 13)]
    result = {"steel_girder": _model(boxes)}
    for axis, order in (("x", (1, 0, 2)), ("z", (0, 2, 1))):
        result[f"steel_girder_{axis}"] = _model([
            ([frm[k] for k in order], [to[k] for k in order], texture) for frm, to, texture in boxes])
    return result


def _grating():
    b = [_box(0, 0, 0, 1.5, 3, 16), _box(14.5, 0, 0, 16, 3, 16),
         _box(1.5, 0, 0, 14.5, 3, 1.5), _box(1.5, 0, 14.5, 14.5, 3, 16)]
    b += [_box(x, 1.4, 1.5, x + 0.65, 2.8, 14.5) for x in (3, 5, 7, 9, 11, 13)]
    b += [_box(1.5, 0.2, z, 14.5, 1.4, z + 0.8, "tower_detail_dark") for z in (4, 11)]
    return b


def _railing():
    return [_box(0, 0, 13.8, 1.8, 16, 16), _box(14.2, 0, 13.8, 16, 16, 16),
            _box(1.8, 14.2, 14.1, 14.2, 16, 15.8, "tower_detail_light_steel"),
            _box(1.8, 7.5, 14.2, 14.2, 8.8, 15.6), _box(1.8, 0, 14, 14.2, 2, 15.7),
            _box(0.3, 1, 13.55, 1.5, 2.3, 13.85, "tower_detail_amber"),
            _box(14.5, 1, 13.55, 15.7, 2.3, 13.85, "tower_detail_amber")]


def _vent():
    b = [_box(0, 0, 4, 16, 16, 16, "tower_detail_panel"),
         _box(0, 0, 0, 2, 16, 4), _box(14, 0, 0, 16, 16, 4),
         _box(2, 0, 0, 14, 2, 4), _box(2, 14, 0, 14, 16, 4),
         _box(2, 2, 3.3, 14, 14, 4.1, "tower_detail_dark")]
    for y in (2.7, 4.7, 6.7, 8.7, 10.7, 12.7):
        b.append((_box(2.2, y, 1.2, 13.8, y + 0.75, 3.4, "tower_detail_light_steel")
                  + ({"rotation": ("x", 22.5, [8, y + 0.4, 2.3])},)))
    return b


def _ladder():
    b = [_box(3, 0, 13.8, 4.5, 16, 15.8), _box(11.5, 0, 13.8, 13, 16, 15.8)]
    b += [_box(4.5, y, 13.1, 11.5, y + 1.2, 14.4, "tower_detail_light_steel")
          for y in (2, 6, 10, 14)]
    # Caged service ladder: guard hoops and longitudinal guard rails, every piece closed.
    for y in (1, 9):
        b += [_box(1, y, 7, 15, y + 0.8, 7.8),
              _box(1, y, 7.8, 1.8, y + 0.8, 15.8),
              _box(14.2, y, 7.8, 15, y + 0.8, 15.8)]
    b += [_box(x, 0, 7, x + 0.6, 16, 7.6) for x in (1, 7.7, 14.4)]
    return b


def _console():
    b = [_box(1, 0, 2, 4, 10, 14, "tower_detail_dark"),
         _box(12, 0, 2, 15, 10, 14, "tower_detail_dark"),
         _box(0, 10, 1, 16, 12, 15, "tower_detail_panel"),
         _box(2, 11.9, 7.5, 12.5, 12.6, 12.5, "tower_detail_dark"),
         _box(2.3, 12.5, 8, 12.2, 12.75, 12, "tower_detail_screen"),
         _box(13.2, 12, 9, 14.5, 12.65, 11.5, "tower_detail_rubber"),
         _box(7, 12, 2, 9, 16, 4),
         _box(0.5, 15, 2, 15.5, 25, 3.5, "tower_detail_dark"),
         _box(1.3, 15.8, 1.7, 14.7, 24.2, 2.05, "tower_detail_screen"),
         _box(0.7, 13, 3.5, 1.8, 14.3, 4, "tower_detail_amber"),
         _box(14.2, 13, 3.5, 15.3, 14.3, 4, "tower_detail_cyan")]
    b += [_box(x, 1.5, 1.7, x + 1, 8.5, 2.1, "tower_detail_light_steel") for x in (1.5, 13.5)]
    return b


def _holo_table():
    """One 3x3 console table, with an open leg space and a recessed projection bed."""
    b = [_box(0, 10, 0, 48, 11.3, 48, "tower_detail_panel"),
         _box(1.8, 11.3, 1.8, 46.2, 12.35, 46.2, "tower_detail_glass"),
         _box(0, 11.3, 0, 48, 13, 1.8), _box(0, 11.3, 46.2, 48, 13, 48),
         _box(0, 11.3, 1.8, 1.8, 13, 46.2), _box(46.2, 11.3, 1.8, 48, 13, 46.2)]
    for x in (2, 38):
        for z in (2, 38):
            b += [_box(x, 0, z, x + 8, 1.5, z + 8, "tower_detail_dark"),
                  _box(x + 1.5, 1.5, z + 1.5, x + 6.5, 10, z + 6.5),
                  _box(x + 2, 2, z + 1.1, x + 6, 8.5, z + 1.5, "tower_detail_vent")]
    # Four narrow emission rails and tactile controls keep the live hologram unobscured.
    for z in (3, 43.5):
        b += [_box(5, 12.3, z, 18, 12.55, z + 1.5, "tower_detail_cyan"),
              _box(30, 12.3, z, 43, 12.55, z + 1.5, "tower_detail_cyan")]
    for x, z in ((3, 16), (42, 16), (16, 3), (16, 42)):
        b.append(_box(x, 12.35, z, x + 3, 12.65, z + 9, "tower_detail_screen"))
    b += [_box(19, 11.8, 19, 29, 12.6, 29, "tower_detail_dark"),
          _box(21, 12.6, 21, 27, 12.9, 27, "tower_detail_cyan"),
          _box(23, 12.9, 23, 25, 13, 25, "tower_detail_glass")]
    return b


def _formed_pad(kind, width, depth):
    """A joined deck at 16 texels per block: markings and contacts have real visual hierarchy."""
    w, d = width * 16, depth * 16
    image = Image.new("RGBA", (w, d), DARK[2] + (255,))
    draw = ImageDraw.Draw(image)
    for y in range(0, d, 16):
        for x in range(0, w, 16):
            draw.line((x, y, x + 15, y), fill=STEEL[2] + (255,))
            draw.line((x, y + 15, x + 15, y + 15), fill=DARK[1] + (255,))
            for sx, sy in ((4, 5), (10, 11)):
                draw.line((x + sx, y + sy, x + sx + 2, y + sy), fill=STEEL[1] + (255,))
    for y in range(d):
        for x in range(w):
            edge = min(x, y, w - 1 - x, d - 1 - y)
            if edge < 2:
                image.putpixel((x, y), (AMBER[2] if ((x + y) // 4) % 2 else DARK[0]) + (255,))
            elif edge == 4:
                image.putpixel((x, y), CYAN[3] + (255,))
    for x, y, sx, sy in ((8, 8, 1, 1), (w - 9, 8, -1, 1),
                          (8, d - 9, 1, -1), (w - 9, d - 9, -1, -1)):
        draw.line((x, y, x + sx * 7, y), fill=LIGHT[3] + (255,), width=2)
        draw.line((x, y, x, y + sy * 7), fill=LIGHT[3] + (255,), width=2)
    cx, cy = (w - 1) / 2, (d - 1) / 2
    if kind == "pickup":
        x0, y0, x1, y1 = round(cx - 9), round(cy - 9), round(cx + 9), round(cy + 9)
        draw.rectangle((x0 - 2, y0 - 2, x1 + 2, y1 + 2), fill=STEEL[3] + (255,))
        draw.rectangle((x0, y0, x1, y1), fill=STEEL[1] + (255,))
        draw.line((cx, y0, cx, y1), fill=DARK[0] + (255,), width=2)
        for y in (y0 + 4, y1 - 4):
            draw.line((x0 + 3, y, x1 - 3, y), fill=AMBER[2] + (255,), width=2)
        for x, y in ((x0 - 1, y0 - 1), (x1 + 1, y0 - 1), (x0 - 1, y1 + 1), (x1 + 1, y1 + 1)):
            draw.rectangle((x, y, x + 1, y + 1), fill=CYAN[3] + (255,))
    else:
        radius = max(9, min(w, d) * 0.32)
        for a in range(0, 360, 24):
            draw.arc((cx - radius, cy - radius, cx + radius, cy + radius), a, a + 14,
                     fill=LIGHT[3] + (255,), width=1)
        # Machined contact housing, a nested octagon with visible contact fingers.
        for r, color in ((8, STEEL[3]), (6, DARK[0]), (4, CYAN[2]), (2, CYAN[4])):
            points = [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
                      for a in range(22, 382, 45)]
            draw.polygon(points, fill=color + (255,))
        for sx in (-1, 1):
            for sy in (-1, 1):
                x, y = round(cx + sx * 6), round(cy + sy * 6)
                draw.rectangle((x, y, x + 1, y + 1), fill=COPPER[3] + (255,))
    return image


def models():
    result = girder_models()
    result["steel_girder_horizontal"] = {"parent": "jugcraft:block/steel_girder_x"}
    for block in ("aluminum_cladding", "carbon_composite_panel", "silicon_carbide_armor",
                  "depleted_uranium_armor", "graphene_lattice"):
        result[block] = _model(_recessed(block, "tower_detail_light_steel" if block == "aluminum_cladding" else "tower_detail_steel"), block)
    frame = _recessed("tower_detail_dark")
    for z in (0.4, 14.85):
        for angle in (-45, 45):
            frame.append((_box(7.3, 1.5, z, 8.7, 14.5, z + 0.75)
                          + ({"rotation": ("z", angle, [8, 8, z + 0.4])},)))
    result["tungsten_steel_frame"] = _model(frame)
    result.update({"tower_service_grating": _model(_grating()), "tower_service_railing": _model(_railing()),
                   "tower_vent": _model(_vent()), "tower_ladder": _model(_ladder()),
                   "drone_depot_terminal": _model(_console())})
    return result


def write_textures(root=ASSETS):
    root = Path(root)
    out = root / "textures/block"
    out.mkdir(parents=True, exist_ok=True)
    for name, image in textures().items():
        image.save(out / f"{name}.png")
    # Door renderer resources; these are tower-only, never drone fuselage or rotor sprites.
    ent = root / "textures/entity"
    ent.mkdir(parents=True, exist_ok=True)
    _door().resize((64, 64), Image.Resampling.NEAREST).save(ent / "hangar_door_slats.png")
    rail = _flat(STEEL)
    for y in range(3, 13):
        for x in range(16):
            cm.put(rail, x, y, AMBER[2] if ((x + y) // 4) % 2 else DARK[0])
    cm.rect(rail, 0, 13, 15, 15, DARK[0])
    rail.resize((64, 64), Image.Resampling.NEAREST).save(ent / "hangar_door_rail.png")
    # Pad layout/part numbering remains compatible with existing worlds and the placement system.
    for kind, width, depth, stem in (("landing", 5, 5, "landing_pad_formed"), ("pickup", 3, 3, "supply_pickup_formed")):
        image = _formed_pad(kind, width, depth)
        for z in range(depth):
            for x in range(width):
                image.crop((x * 16, z * 16, (x + 1) * 16, (z + 1) * 16)).resize((64, 64), Image.Resampling.NEAREST).save(
                    out / f"{stem}_{1 + z * width + x}.png")
    import tower_art
    for kind, width, depth in tower_art.HANGAR_PAD_LAYOUTS:
        image = _formed_pad(kind, width, depth)
        for z in range(depth):
            for x in range(width):
                part = tower_art.hangar_pad_part(kind, width, depth, x, z)
                image.crop((x * 16, z * 16, (x + 1) * 16, (z + 1) * 16)).resize((64, 64), Image.Resampling.NEAREST).save(
                    out / f"hangar_pad_{part}.png")
    # The owner approved the realistic v3 native asset sample on 8 October.
    # Original albedos stay intact; this final override writes runtime derivatives.
    import tower_realism_runtime
    tower_realism_runtime.write_textures(root)


def write_assets(write=mw.write, rid=mw.rid, assets=ASSETS, lang=None):
    assets = Path(assets)
    for name, model in models().items():
        write(assets / "models/block" / f"{name}.json", model)
    write(assets / "blockstates/steel_girder.json", {"variants": {
        "axis=y": {"model": rid("block/steel_girder")},
        "axis=x": {"model": rid("block/steel_girder_x")},
        "axis=z": {"model": rid("block/steel_girder_z")}}})
    for name in DETAIL_BLOCKS:
        write(assets / "blockstates" / f"{name}.json", {"variants": {
            f"facing={facing}": {"model": rid(f"block/{name}"), **({"y": angle} if angle else {})}
            for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
        if lang is not None:
            lang[f"block.jugcraft.{name}"] = DETAIL_BLOCKS[name]
    write_holo_assets(write, rid, assets)
    import tower_realism_runtime
    tower_realism_runtime.write_assets(write, assets)


def write_holo_assets(write=mw.write, rid=mw.rid, assets=ASSETS):
    boxes = _holo_table()
    footprint = [(x, 0, z) for z in range(3) for x in range(3)]
    parts = mw.split_model("holo_table", boxes, footprint)
    textures = {name: rid(f"block/{name}") for name in mw.texture_names(boxes)}
    textures["particle"] = rid("block/tower_detail_panel")
    variants = {}
    for index, elements in enumerate(parts, start=1):
        name = f"holo_table_formed_{index}"
        for element in elements:
            if all(face["texture"] in ("#tower_detail_screen", "#tower_detail_cyan")
                   for face in element["faces"].values()):
                element["light_emission"] = 10
        write(Path(assets) / "models/block" / f"{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
        variants[f"part={index}"] = {"model": rid(f"block/{name}")}
    # Loose section remains one block. Its gallery projection appears only when the nine sections form.
    loose = [_box(1, 0, 1, 15, 10, 15, "tower_detail_panel"),
             _box(0, 10, 0, 16, 11.5, 16), _box(1, 11.5, 1, 15, 13, 15, "tower_detail_glass")]
    write(Path(assets) / "models/block/holo_table.json", _model(loose))
    variants["part=0"] = {"model": rid("block/holo_table")}
    write(Path(assets) / "blockstates/holo_table.json", {"variants": variants})


def write_data(write=mw.write, data=DATA):
    # Same feature conditions and loot schema as tower.py/generate_material_data.py.
    conditions = [{"condition": "jugcraft:feature_enabled", "feature": name} for name in ("machines", "drones")]
    for name, (pattern, key, count) in CRAFTING.items():
        recipe = {"fabric:load_conditions": conditions, "type": "minecraft:crafting_shaped", "category": "building",
                  "pattern": pattern, "key": key, "result": {"id": f"jugcraft:{name}", "count": count}}
        write(Path(data) / "recipe" / f"{name}.json", recipe)
        write(Path(data) / "loot_table/blocks" / f"{name}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1.0,
                "entries": [{"type": "minecraft:item", "name": f"jugcraft:{name}"}],
                "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"jugcraft:blocks/{name}"})
    tag = Path(data).parent / "minecraft/tags/block/climbable.json"
    content = json.loads(tag.read_text(encoding="utf-8")) if tag.exists() else {"replace": False, "values": []}
    if "jugcraft:tower_ladder" not in content["values"]:
        content["values"].append("jugcraft:tower_ladder")
    write(tag, content)


def preview(path=None):
    """Reads exported JSON and PNGs, culls back faces, and z-buffers actual surfaces."""
    import numpy as np
    import art_check
    import armor_preview as ap
    names = ["steel_girder", "steel_girder_x", "aluminum_cladding", "tungsten_steel_frame",
             "tower_service_grating", "tower_service_railing", "tower_vent", "tower_ladder",
             "drone_depot_terminal", "carbon_composite_panel", "reinforced_concrete", "depleted_uranium_armor"]
    tiles = []
    view = (35, 25)
    camera = ap.camera(view)[0]
    for name in names:
        model = json.loads((ASSETS / "models/block" / f"{name}.json").read_text())
        elements, lookup, _ = art_check._resolve(model, (ASSETS,))
        if not elements:
            elements = [mw.element((0, 0, 0), (16, 16, 16), name, uv=True)]
            lookup = lambda ref, n=name: ASSETS / f"textures/block/{n}.png"
        faces, _ = art_check._model_faces(elements, lookup)
        quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), np.array(Image.open(f.tex).convert("RGBA")))
                 for f in faces if np.array(f.normal) @ camera > 0]
        picture = ap.render(quads, view, scale=7, background=(33, 38, 43), lighting="blockbench")
        tile = Image.new("RGB", (240, 250), (33, 38, 43))
        tile.paste(picture, ((240 - picture.width) // 2, 25 + max(0, (190 - picture.height) // 2)))
        ImageDraw.Draw(tile).text((8, 229), name.replace("tower_", "").replace("_", " "), fill=(210, 220, 224))
        tiles.append(tile)
    result = ap.sheet(tiles, 4, gap=8)
    path = Path(path or ROOT / "build/drone_tower/industrial_materials.png")
    path.parent.mkdir(parents=True, exist_ok=True)
    result.save(path)
    # Operations table and floor plates: assemble the very part files the client draws.
    members = [(ASSETS / "models/block" / f"holo_table_formed_{1 + z * 3 + x}.json", (x * 16, 0, z * 16))
               for z in range(3) for x in range(3)]
    faces, boxes = art_check._assembly(members, (ASSETS,))
    issues = art_check._cross_errors("holo_table", faces, boxes, {(x, 0, z) for z in range(3) for x in range(3)})
    if issues:
        raise ValueError("; ".join(issues))
    quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), np.array(Image.open(f.tex).convert("RGBA")))
             for f in faces if np.array(f.normal) @ camera > 0]
    table = ap.render(quads, view, scale=5, background=(33, 38, 43), lighting="blockbench")
    operations = Image.new("RGB", (1060, 450), (33, 38, 43))
    operations.paste(table, (24, 65))
    draw = ImageDraw.Draw(operations)
    draw.text((24, 20), "EXPORTED 3 x 3 HOLOGRAM TABLE / live hologram drawn in game above projection bed", fill=(210, 220, 224))
    for x0, stem, size, title in ((470, "landing_pad_formed", 5, "LANDING PAD / contact rings, edge lighting, approach marks"),
                                 (780, "supply_pickup_formed", 3, "SUPPLY PICKUP / twin lift hatch")):
        tile_size = Image.open(ASSETS / "textures/block" / f"{stem}_1.png").width
        picture = Image.new("RGBA", (size * tile_size, size * tile_size))
        for z in range(size):
            for x in range(size):
                picture.paste(Image.open(ASSETS / "textures/block" / f"{stem}_{1 + z * size + x}.png"), (x * tile_size, z * tile_size))
        operations.paste(picture.resize((260, 260), Image.Resampling.NEAREST), (x0, 105))
        draw.text((x0, 386), title, fill=(210, 220, 224))
    operations.save(path.with_name("industrial_operations.png"))
    import tower_realism_runtime
    tower_realism_runtime.preview()
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--preview", action="store_true")
    args = parser.parse_args()
    if args.write:
        write_textures()
        langpath = ASSETS / "lang/en_us.json"
        lang = json.loads(langpath.read_text(encoding="utf-8"))
        write_assets(lang=lang)
        mw.write(langpath, lang)
        write_data()
    if args.preview:
        print(preview())
    if not args.write and not args.preview:
        parser.print_help()


if __name__ == "__main__":
    main()
