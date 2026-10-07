"""Dieselworks (batch 45, docs/features/dieselworks.md): building blocks in the look of the dieselpunk giants.

Weathered riveted steel plate, green patina panels, chipped red iron, banded copper dome plate, ribbed pillars, skid iron,
see-through steel grating for catwalks, steel I-beams, porthole windows and amber cage lamps. The patina, red iron, dome
and skid blocks reuse the giants' textures (tools/dieselrust_textures.py, "dr_*"). The steel set (both steel plates, the
Ribbed Steel Pillar, the Riveted Band, the grating, the I-beam, the porthole ring and the lamp's base and cap) has its
own "dw_*" textures, drawn here in one cool steel palette as tiling building blocks (docs/ART_DIRECTION.md, "Tiling
building blocks"); the giants keep their "dr_*" steel.

Java: building/Dieselworks.java registers the blocks; tools/check_mod_data.py keeps the list and strengths the same.
Every recipe turns metal plate (or ingots, nuggets and glass) into blocks; nothing turns a block back into metal.
"""
import math
import sys

from PIL import Image

import clean_metal

MOD = "jugcraft"

# id: (display name, kind, hardness, blast resistance). Kinds: "family" is a full block with its slab and stairs,
# "full" a plain cube, "pillar" a log-like column, "grating" a see-through cube with its slab, "beam" a steel I-beam
# lying along an axis, "glass" a porthole window and "lamp" a small caged lamp.
BLOCKS = {
    "rust_plate": ("Weathered Steel Plate", "family", 5.0, 6.0),
    "riveted_rust_plate": ("Riveted Steel Plate", "family", 5.0, 6.0),
    "patina_plate": ("Patina Plate", "family", 5.0, 6.0),
    "perforated_patina_plate": ("Perforated Patina Plate", "full", 5.0, 6.0),
    "red_iron_plate": ("Red Iron Plate", "family", 5.0, 6.0),
    "copper_dome_plate": ("Copper Dome Plate", "family", 5.0, 6.0),
    "riveted_band_block": ("Riveted Band", "full", 5.0, 6.0),
    "skid_iron_block": ("Skid Iron", "full", 5.0, 6.0),
    "ribbed_patina_pillar": ("Ribbed Patina Pillar", "pillar", 5.0, 6.0),
    "ribbed_rust_pillar": ("Ribbed Steel Pillar", "pillar", 5.0, 6.0),
    "rust_grating": ("Steel Grating", "grating", 3.0, 6.0),
    "steel_i_beam": ("Steel I-Beam", "beam", 5.0, 6.0),
    "porthole_window": ("Porthole Window", "glass", 1.0, 3.0),
    "amber_cage_lamp": ("Amber Cage Lamp", "lamp", 1.5, 3.0),
}
# Textures of the cubes: one name for all faces, or (side, end) for pillars.
TEXTURES = {
    "rust_plate": "dw_steel_plate",
    "riveted_rust_plate": "dw_steel_plate_riveted",
    "patina_plate": "dr_patina",
    "perforated_patina_plate": "dr_perforated",
    "red_iron_plate": "dr_red",
    "copper_dome_plate": "dr_dome",
    "riveted_band_block": "dw_steel_band",
    "skid_iron_block": "dr_skid",
    "ribbed_patina_pillar": ("dr_ribbed_patina", "dr_patina"),
    "ribbed_rust_pillar": ("dw_ribbed_steel", "dw_steel_plate"),
    "rust_grating": "dw_grating",
    "porthole_window": "dw_porthole_steel",
}
LAMP_LIGHT = 14
LAMP_TEXTURES = {"amber_cage_lamp": {"metal": "dw_steel_plate", "bar": "dr_band", "glass": "dr_amber_on"}}
# The I-beam's flanges ("side") and web, both in the Dieselworks steel.
BEAM_TEXTURES = {"steel_i_beam": {"side": "dw_steel_plate", "web": "dw_steel_plate"}}
TOOLTIPS = {
    "rust_grating": "See-through: build catwalks and walkways.",
    "steel_i_beam": "Lies along the axis you place it on, like a log.",
    "amber_cage_lamp": "Gives off warm amber light.",
}


def blocks():
    out = []
    for block, (_, kind, _, _) in BLOCKS.items():
        out.append(block)
        if kind == "family":
            out += [f"{block}_slab", f"{block}_stairs"]
        elif kind == "grating":
            out.append(f"{block}_slab")
    return out


def items():
    return []


# ------------------------------------------------------------------ models

def _faces(texture, uv=None):
    return {face: ({"texture": texture, "uv": uv} if uv else {"texture": texture})
            for face in ("north", "south", "east", "west", "up", "down")}


def i_beam_elements():
    """A steel I-beam along y (the blockstate turns it): two flanges joined by a web."""
    out = []
    for x0, x1 in ((1, 4), (12, 15)):
        out.append({"from": [x0, 0, 1], "to": [x1, 16, 15], "faces": _faces("#side")})
    out.append({"from": [4, 0, 6.5], "to": [12, 16, 9.5], "faces": _faces("#web")})
    return out


def lamp_elements():
    """A caged amber lamp: a steel base and cap, four cage bars and the glowing glass between them."""
    return [
        {"from": [4, 0, 4], "to": [12, 2, 12], "faces": _faces("#metal")},
        {"from": [5, 2, 5], "to": [11, 11, 11], "faces": _faces("#glass"), "light_emission": 14},
        {"from": [4.5, 11, 4.5], "to": [11.5, 13, 11.5], "faces": _faces("#metal")},
        {"from": [7, 13, 7], "to": [9, 14, 9], "faces": _faces("#metal")},
    ] + [{"from": [x, 2, z], "to": [x + 1, 11, z + 1], "faces": _faces("#bar")}
         for x in (4.5, 10.5) for z in (4.5, 10.5)] + [
        {"from": [4.5, 6, 4.5], "to": [11.5, 7, 5], "faces": _faces("#bar")},
        {"from": [4.5, 6, 11], "to": [11.5, 7, 11.5], "faces": _faces("#bar")},
    ]


def pillar_blockstate(model):
    return {"variants": {"axis=y": {"model": model}, "axis=z": {"model": model, "x": 90},
                         "axis=x": {"model": model, "x": 90, "y": 90}}}


def write_all(write, assets, data, lang, condition, self_drop):
    write_blocks(write, assets, data, lang, condition, self_drop, sys.modules[__name__])


def write_blocks(write, assets, data, lang, condition, self_drop, spec):
    """Writes a building set's models, blockstates, loot, names and recipes. spec is a module with BLOCKS, TEXTURES,
    TOOLTIPS, RECIPES and LAMP_TEXTURES (BEAM_TEXTURES for "beam" blocks, CREST_TEXTURES for "crest" blocks): this one,
    or tools/kaiserworks.py."""
    from construction import stairs_blockstate
    models = assets / "models" / "block"
    BLOCKS, TEXTURES, TOOLTIPS, RECIPES = spec.BLOCKS, spec.TEXTURES, spec.TOOLTIPS, spec.RECIPES
    for block, (name, kind, _, _) in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        model = f"{MOD}:block/{block}"
        if kind == "pillar":
            side, end = TEXTURES[block]
            write(models / f"{block}.json", {"parent": "minecraft:block/cube_column",
                                             "textures": {"side": f"{MOD}:block/{side}", "end": f"{MOD}:block/{end}"}})
            write(assets / "blockstates" / f"{block}.json", pillar_blockstate(model))
        elif kind == "beam":
            textures = {key: f"{MOD}:block/{texture}" for key, texture in spec.BEAM_TEXTURES[block].items()}
            write(models / f"{block}.json", {"textures": {**textures, "particle": textures["side"]},
                                             "elements": i_beam_elements()})
            write(assets / "blockstates" / f"{block}.json", pillar_blockstate(model))
        elif kind == "lamp":
            textures = {key: f"{MOD}:block/{texture}" for key, texture in spec.LAMP_TEXTURES[block].items()}
            write(models / f"{block}.json", {"ambientocclusion": False, "textures": {**textures, "particle": textures["metal"]},
                                             "elements": lamp_elements()})
            write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": model}}})
        elif kind == "crest":
            front, rest = spec.CREST_TEXTURES[block]
            write(models / f"{block}.json", {"parent": "minecraft:block/orientable", "textures": {
                "front": f"{MOD}:block/{front}", "side": f"{MOD}:block/{rest}", "top": f"{MOD}:block/{rest}"}})
            write(assets / "blockstates" / f"{block}.json", {"variants": {
                f"facing={facing}": ({"model": model, "y": y} if y else {"model": model})
                for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        else:
            write(models / f"{block}.json", {"parent": "minecraft:block/cube_all",
                                             "textures": {"all": f"{MOD}:block/{TEXTURES[block]}"}})
            write(assets / "blockstates" / f"{block}.json", {"variants": {"": {"model": model}}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": model}})
        write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))
        if kind not in ("family", "grating"):
            continue
        texture = f"{MOD}:block/{TEXTURES[block]}"
        side = {"bottom": texture, "top": texture, "side": texture}
        slab = f"{block}_slab"
        lang[f"block.{MOD}.{slab}"] = f"{name} Slab"
        write(models / f"{slab}.json", {"parent": "minecraft:block/slab", "textures": side})
        write(models / f"{slab}_top.json", {"parent": "minecraft:block/slab_top", "textures": side})
        write(assets / "blockstates" / f"{slab}.json", {"variants": {
            "type=bottom": {"model": f"{MOD}:block/{slab}"}, "type=top": {"model": f"{MOD}:block/{slab}_top"},
            "type=double": {"model": model}}})
        write(assets / "items" / f"{slab}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{slab}"}})
        table = self_drop(slab)
        table["pools"][0]["entries"][0]["modifier"] = [
            {"type": "minecraft:set_count", "count": 2, "add": False,
             "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{slab}", "state": {"type": "double"}}},
            {"type": "minecraft:explosion_decay"}]
        write(data / "loot_table" / "blocks" / f"{slab}.json", table)
        write(data / "recipe" / f"{slab}.json", shaped(condition, ["BBB"], {"B": f"{MOD}:{block}"}, slab, 6))
        if kind == "grating":
            continue
        stairs = f"{block}_stairs"
        lang[f"block.{MOD}.{stairs}"] = f"{name} Stairs"
        for suffix in ("", "_inner", "_outer"):
            write(models / f"{stairs}{suffix}.json", {"parent": f"minecraft:block/{'stairs' if not suffix else suffix[1:] + '_stairs'}",
                                                     "textures": side})
        write(assets / "blockstates" / f"{stairs}.json", stairs_blockstate(f"{MOD}:block/{stairs}"))
        write(assets / "items" / f"{stairs}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{stairs}"}})
        write(data / "loot_table" / "blocks" / f"{stairs}.json", self_drop(stairs))
        write(data / "recipe" / f"{stairs}.json", shaped(condition, ["B  ", "BB ", "BBB"], {"B": f"{MOD}:{block}"}, stairs, 4))
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text

    recipes = data / "recipe"
    for name, result, pattern, key, count in RECIPES:
        write(recipes / f"{name}.json", shaped(condition, pattern, key, result, count))


R = f"{MOD}:rust_plate"
P = f"{MOD}:patina_plate"
# (recipe name, result, pattern, key, count). One plate of metal makes one block; trims add nuggets or a dye.
RECIPES = [
    ("rust_plate", "rust_plate", ["PP", "PP"], {"P": "#c:plates/iron"}, 4),
    ("riveted_rust_plate", "riveted_rust_plate", [" R ", "RNR", " R "], {"R": R, "N": "minecraft:iron_nugget"}, 4),
    ("patina_plate", "patina_plate", ["PP", "PP"], {"P": "#c:plates/copper"}, 4),
    ("perforated_patina_plate", "perforated_patina_plate", [" P ", "PNP", " P "], {"P": P, "N": "minecraft:iron_nugget"}, 4),
    ("red_iron_plate", "red_iron_plate", ["RRR", "RDR", "RRR"], {"R": R, "D": "minecraft:red_dye"}, 8),
    ("copper_dome_plate", "copper_dome_plate", [" P ", "PCP", " P "], {"P": P, "C": "minecraft:copper_ingot"}, 4),
    ("riveted_band_block", "riveted_band_block", ["NRN", "R R", "NRN"], {"R": R, "N": "minecraft:iron_nugget"}, 4),
    ("skid_iron_block", "skid_iron_block", ["SS", "SS"], {"S": "#c:plates/steel"}, 4),
    ("ribbed_patina_pillar", "ribbed_patina_pillar", ["P", "P"], {"P": P}, 2),
    ("ribbed_rust_pillar", "ribbed_rust_pillar", ["R", "R"], {"R": R}, 2),
    ("rust_grating", "rust_grating", ["BB", "BB"], {"B": "minecraft:iron_bars"}, 4),
    ("steel_i_beam", "steel_i_beam", ["SSS", " S ", "SSS"], {"S": "#c:plates/steel"}, 6),
    ("porthole_window", "porthole_window", [" R ", "RGR", " R "], {"R": R, "G": "minecraft:glass"}, 4),
    ("amber_cage_lamp", "amber_cage_lamp", ["NNN", "NGN", "NRN"],
     {"N": "minecraft:iron_nugget", "G": "minecraft:glowstone", "R": R}, 2),
]


def shaped(condition, pattern, key, result, count):
    return {"fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "building",
            "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{result}", "count": count}}


# ------------------------------------------------------------------ art

# The giants' weathered steel (tools/dieselrust_textures.PLATE), drawn in the clean style (tools/clean_metal.py). Only
# dw_porthole still uses it, because the zeppelin and the raiders' quads share that texture; the Porthole Window block
# itself uses dw_porthole_steel.
PLATE = [(44, 42, 44), (66, 63, 63), (86, 82, 80), (106, 101, 97), (128, 122, 116), (152, 146, 138)]

# The Dieselworks steel: cool blue-grey, one palette for every steel block of the set, dark to light. 0 deep shadow,
# 1 seam and shade, 2 fill, 3 sheen and lit edge, 4 rivet glint and lit rail, 5 glint. The owner called the old
# warm-brown plates (the giants' dr_* textures, each framed in near-black) horrific on 5 October 2026; these follow
# bastion concrete, which they liked.
STEEL = [(58, 63, 71), (82, 88, 97), (104, 110, 119), (118, 124, 133), (138, 144, 152), (172, 178, 184)]
# A sheet's lit top row and left column: one shade over the fill, the same as its brushed streaks, so the joint between
# two blocks (seam, then the next block's lit edge) is about as strong as one of bastion's course lines and the wall
# does not read as a grid of framed tiles.
LIT = STEEL[3]
# Rivet heads for clean_metal.bolt(): lit at the top left, shaded at the bottom right only one shade under the fill,
# so they read as raised domes, not as dark holes or dice pips.
RIVET = (STEEL[1], STEEL[3], STEEL[4])


def steel_plate():
    """Weathered Steel Plate: one brushed sheet per block. The seam is split across the edge (lit top row and left
    column, seam on the bottom row and right column), so a wall or floor shows one seam between blocks. Brushed streaks
    one shade up, staggered at five heights; no glint, rust or stain stamped into every block."""
    img = clean_metal.canvas(STEEL[2])
    clean_metal.sheet(img, LIT, STEEL[2], STEEL[1])
    for x0, x1, y in ((2, 8, 3), (7, 13, 6), (1, 4, 9), (9, 13, 10), (3, 8, 12)):
        clean_metal.rect(img, x0, y, x1, y, STEEL[3])
    return img


def riveted_steel_plate():
    """Riveted Steel Plate: the same sheet framed by twelve raised rivets at x and y 1, 5, 9 and 13, with a one-row
    sheen across its upper part. The rivets' period of four carries on across the seam into the next block, so a wall
    shows one even rivet lattice along every joint; no rivet crosses the slab cut (rows 7|8), so slabs and stair steps
    never show half a head."""
    img = clean_metal.canvas(STEEL[2])
    clean_metal.sheet(img, LIT, STEEL[2], STEEL[1])
    clean_metal.rect(img, 3, 3, 12, 3, STEEL[3])
    for x in (1, 5, 9, 13):
        clean_metal.bolt(img, x, 1, RIVET)
        clean_metal.bolt(img, x, 13, RIVET)
    for y in (5, 9):
        clean_metal.bolt(img, 1, y, RIVET)
        clean_metal.bolt(img, 13, y, RIVET)
    return img


def ribbed_steel():
    """The Ribbed Steel Pillar's side: a rib every four rows (lit top, sheen, fill, dark gap), shaded across its width
    like a round column, lit on the left and darker on the right."""
    img = clean_metal.canvas(STEEL[2])
    rows = (4, 3, 2, 0)
    across = (1,) + (0,) * 12 + (-1, -1, -1)
    for y in range(16):
        for x in range(16):
            clean_metal.put(img, x, y, STEEL[max(0, min(5, rows[y % 4] + across[x]))])
    return img


def steel_band():
    """The Riveted Band: a dark steel strap across the sheet, lit along its top and casting a shadow under it, with a
    raised rivet every four pixels. The strap and its rivets carry on unbroken across the block edge, so a course of
    bands reads as one long strap; the sheet above and below keeps the split seam."""
    img = clean_metal.canvas(STEEL[2])
    clean_metal.sheet(img, LIT, STEEL[2], STEEL[1])
    clean_metal.rect(img, 0, 4, 15, 4, STEEL[3])
    clean_metal.rect(img, 0, 5, 15, 10, STEEL[1])
    clean_metal.rect(img, 0, 11, 15, 11, STEEL[0])
    clean_metal.rect(img, 0, 12, 15, 12, STEEL[1])
    for x in (1, 5, 9, 13):
        clean_metal.bolt(img, x, 7, RIVET)
    return img


def grating():
    """Steel bar grating in the Dieselworks steel, see-through so catwalks show what is below. Bearing bars run down
    the block every four pixels (x 0, 4, 8 and 12), so they carry on evenly across every joint. Each half of the block
    is framed by its own pair of cross rails, lit along its top row (0 and 8) and shaded along its bottom row (7 and
    15). The rail pair at the slab cut (7|8) is therefore drawn exactly like the pair at the block edge (15|0), and both
    slab halves are closed at the top and bottom: a slab or a stair step never ends in open prongs. The slots are
    3 x 6."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rails = {0: STEEL[4], 7: STEEL[1], 8: STEEL[4], 15: STEEL[1]}
    for y in range(16):
        for x in range(16):
            if y in rails:
                clean_metal.put(img, x, y, rails[y])
            elif x % 4 == 0:
                clean_metal.put(img, x, y, STEEL[2])
    return img


def porthole():
    """A round porthole: a bolted steel ring, lit on its upper half, round clear glass with a bright glint."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r > 7.6:
                c = PLATE[1]
            elif r > 5.2:
                c = PLATE[3] if dx + dy < -2 else PLATE[1] if dx + dy > 2 else PLATE[2]
            elif r > 4.4:
                c = PLATE[0]
            else:
                continue
            img.putpixel((x, y), c + (255,))
    for angle in range(0, 360, 45):
        x = round(7.5 + 6.4 * math.cos(math.radians(angle)))
        y = round(7.5 + 6.4 * math.sin(math.radians(angle)))
        img.putpixel((x, y), PLATE[5] + (255,))
    for x, y in ((5, 5), (6, 4), (4, 6)):
        img.putpixel((x, y), (230, 240, 236, 255))
    return img


def porthole_steel():
    """The Porthole Window block: the same bolted ring and clear round glass as porthole(), set in a Dieselworks steel
    sheet (lit top row and left column, seam on the bottom row and right column) so it sits flush in a steel wall. The
    ring stays inside the sheet's edges, so the seams of the wall run on past the window unbroken."""
    img = clean_metal.canvas(STEEL[2])
    clean_metal.sheet(img, LIT, STEEL[2], STEEL[1])
    for y in range(1, 15):
        for x in range(1, 15):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r > 7.0:
                continue
            if r > 5.2:
                c = STEEL[4] if dx + dy < -2 else STEEL[1] if dx + dy > 2 else STEEL[3]
            elif r > 4.4:
                c = STEEL[0]
            else:
                img.putpixel((x, y), (0, 0, 0, 0))
                continue
            clean_metal.put(img, x, y, c)
    for angle in range(0, 360, 45):
        x = round(7.5 + 6.1 * math.cos(math.radians(angle)))
        y = round(7.5 + 6.1 * math.sin(math.radians(angle)))
        clean_metal.put(img, x, y, STEEL[5])
    for x, y in ((5, 5), (6, 4), (4, 6)):
        img.putpixel((x, y), (230, 240, 236, 255))
    return img


def draw_all(save):
    save(grating(), "block", "dw_grating")
    save(porthole(), "block", "dw_porthole")
    save(porthole_steel(), "block", "dw_porthole_steel")
    save(steel_plate(), "block", "dw_steel_plate")
    save(riveted_steel_plate(), "block", "dw_steel_plate_riveted")
    save(ribbed_steel(), "block", "dw_ribbed_steel")
    save(steel_band(), "block", "dw_steel_band")
