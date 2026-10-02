"""Built-in blueprints (docs/features/blueprint-system.md), the Survey Stake block and the Blueprint item.

A blueprint file (format 1, ``*.jugbp.json``) holds a palette of block states and the structure as
bottom-to-top layers of text rows (one row per z, one character per x; '.' means "nothing required").
``anchor`` is where the Survey Stake stands, in blueprint coordinates. Blueprints are drawn with their
front facing south (+z) and the stake in front of it; placing turns them so the front faces the player.

Run directly (or through generate_material_data.py) to write the built-in files and a preview of each.
"""
import json
from pathlib import Path

MOD = "jugcraft"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "data" / MOD / "blueprint"

BUILT_IN = ["drone_tower_foundation", "arc_furnace", "small_church"]

STAKE = {"survey_stake": {"display": "Survey Stake"}, "blueprint_table": {"display": "Blueprint Table"},
         "creative_energy_cell": {"display": "Creative Energy Cell"},
         "creative_supply_crate": {"display": "Creative Supply Crate"}}
# Blocks that drop themselves (the stake gives its blueprint back instead; the creative cell drops nothing).
SELF_DROP = []
CRAFTING = {
    "blueprint_table": (["PPP", "LCL", "L L"], {"P": "minecraft:paper", "L": "#minecraft:planks", "C": "#c:ingots/copper"}, 1),
    "survey_stake": (["Y", "S", "S"], {"Y": "minecraft:yellow_dye", "S": "minecraft:stick"}, 2),
}
ITEMS = {"blueprint": "Blueprint"}

LANG = {
    "message.jugcraft.blueprint.stage.base": "Blueprint: the foundation (shift+scroll to see what it grows into)",
    "message.jugcraft.blueprint.stage": "Blueprint preview: %s (shift+scroll)",
    "item.jugcraft.blueprint.named": "Blueprint: %s",
    "message.jugcraft.blueprint.placed": "%s staked out (%s). Right-click the stake for progress; sneak-right-click to switch Personal/Party.",
    "message.jugcraft.blueprint.no_room": "No room for the Survey Stake here",
    "message.jugcraft.blueprint.clipped": "Too tall here: the top %s blocks are above the world height limit (y %s) and will be clipped off",
    "message.jugcraft.blueprint.clipped_later": "Warning: as it grows, this build will reach %s blocks above the world height limit (y %s) and be clipped off",
    "message.jugcraft.blueprint.unknown": "Unknown or changed blueprint",
    "message.jugcraft.blueprint.progress": "%s: %s of %s blocks in place (%s)",
    "message.jugcraft.blueprint.missing": "Still needed: %s",
    "message.jugcraft.blueprint.wrong": "%s blocks are in the way (red outline)",
    "message.jugcraft.blueprint.hand_only": "Place by hand: %s",
    "message.jugcraft.blueprint.mode": "Blueprint mode: %s",
    "message.jugcraft.blueprint.not_owner": "Only the player who staked this blueprint (or the party leader) can change it",
    "message.jugcraft.blueprint.complete": "%s is complete! The Survey Stake pops off (the blueprint was used up).",
    "blueprint.jugcraft.drone_tower_foundation": "Drone Tower Foundation",
    "blueprint.jugcraft.small_church": "Small Church",
    "message.jugcraft.blueprint.printed": "Printed: %s",
    "message.jugcraft.creative_only": "Creative only",
    "entity.jugcraft.seat": "Seat",
    "blueprint.jugcraft.arc_furnace": "Arc Furnace",
}


def blueprint(name, description, cells, anchor):
    """Packs {(x, y, z): state} into the file format."""
    xs = [p[0] for p in cells]
    ys = [p[1] for p in cells]
    zs = [p[2] for p in cells]
    assert min(xs) == 0 and min(ys) == 0 and min(zs) == 0, "cells start at 0"
    size = [max(xs) + 1, max(ys) + 1, max(zs) + 1]
    keys = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    palette, index = {}, {}
    for state in sorted(set(cells.values())):
        key = keys[len(palette)]
        palette[key] = state
        index[state] = key
    layers = []
    for y in range(size[1]):
        rows = []
        for z in range(size[2]):
            rows.append("".join(index[cells[(x, y, z)]] if (x, y, z) in cells else "." for x in range(size[0])))
        layers.append(rows)
    return {"format": 1, "name": name, "author": "Jugcraft (built in)", "description": description,
            "size": size, "anchor": list(anchor), "palette": palette, "layers": layers}


def drone_tower_foundation():
    """What a player builds by hand to start a Drone Tower: the 15x15 chiseled stone brick plinth with the
    Tower Core in its middle. Upgrading the core then builds tier 1 (the Command Post) on it."""
    cells = {(x, 0, z): "minecraft:chiseled_stone_bricks" for x in range(15) for z in range(15)}
    cells[(7, 0, 7)] = f"{MOD}:drone_tower_core"
    return blueprint("Drone Tower Foundation",
                     "The 15x15 chiseled stone brick plinth and the Drone Tower Core in its middle. Build it, put "
                     "modules in the core and press UPGRADE: the core builds the Command Post on top.",
                     cells, (7, 0, 16))


def small_church():
    """A small stone church, 9 x 15 with a pitched oak roof, pews, an altar and a bell tower over the door."""
    W, D = 9, 15
    c = {}
    for x in range(W):
        for z in range(D):
            c[(x, 0, z)] = "minecraft:stone_bricks"
    # Walls (y 1-5): cobblestone with stripped oak log corners, windows on the long sides and the back.
    for y in range(1, 6):
        for x in range(W):
            for z in range(D):
                if not (x in (0, W - 1) or z in (0, D - 1)):
                    continue
                corner = x in (0, W - 1) and z in (0, D - 1)
                window = y in (2, 3) and ((x in (0, W - 1) and z in (3, 6, 8, 11)) or (z == 0 and x == 4))
                door = z == D - 1 and x == 4 and y in (1, 2)
                if door:
                    continue
                c[(x, y, z)] = ("minecraft:stripped_oak_log" if corner else "minecraft:glass" if window
                                else "minecraft:cobblestone")
    # Over the door: a stone lintel and a round window.
    c[(4, 3, D - 1)] = "minecraft:stone_bricks"
    c[(4, 4, D - 1)] = "minecraft:glass"
    # Pews facing the altar (stairs facing south: you sit looking north), an aisle down the middle.
    for z in range(6, 12, 2):
        for x in (1, 2, 3, 5, 6, 7):
            c[(x, 1, z)] = "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]"
    # Altar at the back, a step up.
    for x in range(2, 7):
        c[(x, 1, 1)] = "minecraft:polished_andesite"
        c[(x, 1, 2)] = "minecraft:stone_brick_slab[type=bottom]"
    c[(4, 2, 1)] = "minecraft:white_wool"
    # Pitched roof, ridge along z: dark oak stairs stepping in from both eaves, planks in the gable ends.
    for step in range(4):
        y = 6 + step
        for z in range(D):
            c[(step, y, z)] = "minecraft:dark_oak_stairs[facing=east,half=bottom,shape=straight]"
            c[(W - 1 - step, y, z)] = "minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]"
        for z in (0, D - 1):
            for x in range(step + 1, W - 1 - step):
                c[(x, y, z)] = "minecraft:oak_planks"
    for z in range(D):
        c[(4, 10, z)] = "minecraft:dark_oak_slab[type=bottom]"
    # Bell tower over the front: a 3x3 stone brick belfry with open arches, a slab cap and a cross.
    for y in range(9, 15):
        for x in (3, 4, 5):
            for z in (D - 3, D - 2, D - 1):
                edge = x in (3, 5) or z in (D - 3, D - 1)
                if not edge:
                    c.pop((x, y, z), None)
                    continue
                arch = y in (11, 12) and (x == 4 or z == D - 2)
                if arch:
                    c.pop((x, y, z), None)
                else:
                    c[(x, y, z)] = "minecraft:stone_bricks"
    for x in (3, 4, 5):
        for z in (D - 3, D - 2, D - 1):
            c[(x, 15, z)] = "minecraft:stone_brick_slab[type=bottom]"
    c[(4, 9, D - 2)] = "minecraft:stone_bricks"
    c[(4, 10, D - 2)] = "minecraft:stone_bricks"
    for y in (16, 17, 18):
        c[(4, y, D - 2)] = "minecraft:oak_fence"
    c[(3, 17, D - 2)] = "minecraft:oak_fence"
    c[(5, 17, D - 2)] = "minecraft:oak_fence"
    return blueprint("Small Church",
                     "A small stone church with a pitched dark oak roof, six pews, an altar and a bell tower over "
                     "the door. A test blueprint for drones and hand building.",
                     c, (4, 0, D + 1))


def arc_furnace():
    """The Arc Furnace multiblock: a 3x3x3 cube of arc furnace casing with the controller in the middle of the
    front face, facing out (power goes into the controller's front)."""
    cells = {(x, y, z): f"{MOD}:arc_furnace_casing" for x in range(3) for y in range(3) for z in range(3)}
    cells[(1, 1, 2)] = f"{MOD}:arc_furnace_controller[facing=south]"
    return blueprint("Arc Furnace",
                     "The Arc Furnace multiblock: a solid 3x3x3 cube of arc furnace casing, with the controller "
                     "in the middle of the front face. Power the controller from the front.",
                     cells, (1, 0, 4))


BUILDERS = {"drone_tower_foundation": drone_tower_foundation, "arc_furnace": arc_furnace, "small_church": small_church}


def write_all():
    OUT.mkdir(parents=True, exist_ok=True)
    for name in BUILT_IN:
        data = BUILDERS[name]()
        (OUT / f"{name}.jugbp.json").write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


def stake_texture():
    """The Survey Stake: a wooden post with a cyan-free, dull red survey band."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            wood = (122, 90, 56) if (x + y * 3) % 7 else (100, 72, 44)
            img.putpixel((x, y), wood + (255,))
    for y in (3, 4, 5):
        for x in range(16):
            img.putpixel((x, y), (190, 54, 40, 255) if y != 4 else (226, 84, 62, 255))
    return img


# The blueprint item's colour in the inventory (Blueprint.Kind): a complete build (one structure or a whole
# collection) is blue, one part of a larger build (a single cooling tower) green, a player's import red.
KIND_COLOURS = {"complete": ((40, 90, 180), (210, 228, 255)), "part": ((34, 128, 64), (206, 246, 214)),
                "imported": ((168, 44, 36), (255, 214, 206))}


def blueprint_texture(kind="complete"):
    """The Blueprint item: a rolled sheet with a white grid, coloured by its kind."""
    from PIL import Image
    sheet, grid = KIND_COLOURS[kind]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            line = x % 4 == 1 or y % 4 == 1
            img.putpixel((x, y), grid + (255,) if line else sheet + (255,))
    for y in range(2, 14):
        img.putpixel((1, y), (200, 200, 200, 255))
        img.putpixel((14, y), (200, 200, 200, 255))
    return img


def ghost_texture():
    """The hologram texture for ghost blocks: white with faint scanlines (tinted per ghost when drawn)."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            a = 255 if edge else 150 if y % 3 == 0 else 90
            img.putpixel((x, y), (255, 255, 255, a))
    return img


# The Blueprint Table is a drafting station two blocks wide (64 px textures, realistic like the tower's blocks):
# steel trestle legs, a birch drawing board tilted 22.5 degrees with a full blueprint taped on (split over the two
# blocks), a drafting arm with scales, a pencil ledge, a swing-arm lamp, a shelf of drawings and a plan holder.
TABLE_TEXTURES = ("blueprint_table_board_left", "blueprint_table_board_right", "blueprint_table_side", "blueprint_table_birch",
                  "blueprint_table_alu", "blueprint_table_paper", "blueprint_table_rubber", "blueprint_table_bulb",
                  "blueprint_table_roll", "blueprint_table_roll_end")


def drafting_sheet(w=128, h=64):
    """The drawing on the board, both halves: a birch rim, cyanotype paper with a grid, a gabled elevation with a
    dimension line on the left, a plan with a door swing and a title block on the right."""
    from PIL import Image, ImageDraw
    L = (232, 240, 252)
    img = Image.new("RGB", (w, h), (206, 178, 132))                    # birch rim
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, w - 4, h - 4], fill=(30, 72, 154))
    for x in range(3, w - 3, 4):
        d.line([x, 3, x, h - 4], fill=(46, 90, 168) if (x - 3) % 16 else (62, 106, 182))
    for y in range(3, h - 3, 4):
        d.line([3, y, w - 4, y], fill=(46, 90, 168) if (y - 3) % 16 else (62, 106, 182))
    d.rectangle([6, 6, w - 7, h - 7], outline=L)
    d.rectangle([12, 26, 50, 48], outline=L)                             # elevation
    d.line([10, 26, 31, 14, 52, 26], fill=L)
    for k in range(3):
        d.rectangle([16 + k * 11, 31, 21 + k * 11, 37], outline=L)
    d.rectangle([28, 40, 34, 48], outline=L)
    d.line([12, 53, 50, 53], fill=L)
    d.line([12, 51, 12, 55], fill=L); d.line([50, 51, 50, 55], fill=L)
    d.rectangle([72, 12, 108, 38], outline=L)                            # plan
    d.line([90, 12, 90, 28], fill=L); d.line([72, 28, 98, 28], fill=L)
    d.arc([90, 28, 98, 36], 270, 360, fill=L)
    d.rectangle([90, 44, w - 8, h - 8], outline=L)                       # title block
    d.line([90, 49, w - 8, 49], fill=L)
    d.line([104, 49, 104, h - 8], fill=L)
    return img


def table_textures():
    """{name: 64 px image} for the drafting station."""
    import numpy as np
    from PIL import Image
    from tower_art import pnoise, grain, colorize, bevel, groove, grime, to_image, N
    sheet = drafting_sheet().resize((2 * N, N), Image.NEAREST)
    steel = colorize((70, 74, 82), grain(211) * 0.5 + pnoise(212, 6) * 0.5, 9)
    bevel(steel, 0, 0, N, N, 2)
    grime(steel, 213, 0.1)
    birch = colorize((214, 190, 150), grain(221, horizontal=True) * 0.6 + pnoise(222, 8) * 0.4, (16, 14, 10))
    alu = colorize((196, 200, 206), grain(231) * 0.6 + pnoise(232, 5) * 0.4, 10)
    for x in range(0, N, 4):                                             # scale ticks along the edge
        alu[0:3 if x % 16 else 6, x] *= 0.45
    paper = colorize((236, 232, 220), pnoise(241, 5) * 0.3, 8)
    for y in range(4, N, 6):
        paper[y, :] *= 0.9
    rubber = colorize((40, 42, 40), pnoise(251, 3) * 0.5, 6)
    bulb = colorize((255, 238, 196), pnoise(261, 8) * 0.2, 6)
    roll = colorize((40, 80, 156), grain(271, horizontal=False) * 0.3, 10)
    for x in range(0, N, 8):
        roll[:, x] = roll[:, x] * 0.6 + np.array((180, 204, 240)) * 0.4
    roll[20:26, :] = (214, 196, 150)
    yy, xx = np.mgrid[0:N, 0:N]
    rr = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    end = colorize((232, 228, 214), np.sin(rr * 1.2), 10)
    end[(np.sin(rr * 1.2) > 0.85)] = (70, 110, 180)
    end[rr > 30] *= 0.7
    return {"blueprint_table_board_left": sheet.crop((0, 0, N, N)).convert("RGBA"),
            "blueprint_table_board_right": sheet.crop((N, 0, 2 * N, N)).convert("RGBA"),
            "blueprint_table_side": to_image(steel), "blueprint_table_birch": to_image(birch), "blueprint_table_alu": to_image(alu),
            "blueprint_table_paper": to_image(paper), "blueprint_table_rubber": to_image(rubber), "blueprint_table_bulb": to_image(bulb),
            "blueprint_table_roll": to_image(roll), "blueprint_table_roll_end": to_image(end)}


def creative_cell_texture():
    """The Creative Energy Cell: a dark cell with a bright magenta-and-white infinity band."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (40, 20, 48, 255) if edge else (24, 18, 30, 255))
    for x in range(2, 14):
        for y in (7, 8):
            img.putpixel((x, y), (236, 90, 220, 255))
    for x, y in ((4, 6), (5, 5), (6, 6), (9, 9), (10, 10), (11, 9), (4, 9), (5, 10), (6, 9), (9, 6), (10, 5), (11, 6)):
        img.putpixel((x, y), (255, 230, 255, 255))
    for x in (2, 13):
        for y in range(2, 14):
            img.putpixel((x, y), (120, 40, 110, 255))
    return img


def creative_crate_texture():
    """The Creative Supply Crate: a dark crate with magenta corner bands and a white infinity mark."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            diag = abs(x - y) <= 1 or abs(x + y - 15) <= 1
            img.putpixel((x, y), (200, 70, 190, 255) if edge else (70, 50, 40, 255) if diag else (52, 40, 32, 255))
    for x, y in ((5, 7), (6, 6), (7, 7), (8, 8), (9, 9), (10, 8), (5, 8), (6, 9), (9, 6), (10, 7)):
        img.putpixel((x, y), (255, 236, 255, 255))
    return img


def draw_textures():
    return {"survey_stake": stake_texture(), **table_textures(),
            "creative_energy_cell": creative_cell_texture(), "creative_supply_crate": creative_crate_texture()}


def draw_item_textures():
    return {"blueprint": blueprint_texture(), "blueprint_part": blueprint_texture("part"),
            "blueprint_imported": blueprint_texture("imported")}


def write_assets(write, rid, assets, lang):
    lang.update(LANG)
    lang[f"block.{MOD}.survey_stake"] = STAKE["survey_stake"]["display"]
    lang[f"item.{MOD}.blueprint"] = ITEMS["blueprint"]
    write(assets / "models" / "block" / "survey_stake.json", {
        "parent": "minecraft:block/block", "textures": {"post": rid("block/survey_stake"), "particle": rid("block/survey_stake")},
        "elements": [{"from": [6, 0, 6], "to": [10, 14, 10], "faces": {
            f: {"texture": "#post", "uv": [6, 0, 10, 14] if f not in ("up", "down") else [6, 6, 10, 10]}
            for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [5, 9, 5], "to": [11, 12, 11], "faces": {
                f: {"texture": "#post", "uv": [0, 3, 6, 6]} for f in ("north", "south", "east", "west", "up", "down")}}]})
    write(assets / "blockstates" / "survey_stake.json", {"variants": {"": {"model": rid("block/survey_stake")}}})
    write(assets / "items" / "survey_stake.json", {"model": {"type": "minecraft:model", "model": rid("block/survey_stake")}})
    lang[f"block.{MOD}.blueprint_table"] = STAKE["blueprint_table"]["display"]
    lang[f"block.{MOD}.creative_energy_cell"] = STAKE["creative_energy_cell"]["display"]
    lang[f"block.{MOD}.creative_supply_crate"] = STAKE["creative_supply_crate"]["display"]
    write(assets / "models" / "block" / "creative_supply_crate.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/creative_supply_crate")}})
    write(assets / "blockstates" / "creative_supply_crate.json", {"variants": {"": {"model": rid("block/creative_supply_crate")}}})
    write(assets / "items" / "creative_supply_crate.json", {"model": {"type": "minecraft:model", "model": rid("block/creative_supply_crate")}})
    def box(a, b, faces, **extra):
        return {"from": a, "to": b, "faces": faces, **extra}

    def faces(default, **over):
        return {f: {"texture": over.get(f, default)} for f in ("north", "south", "east", "west", "up", "down")}
    tex = {k: rid(f"block/blueprint_table_{k}") for k in ("side", "birch", "alu", "paper", "rubber", "bulb", "roll", "roll_end")}
    for part in ("main", "side"):
        write(assets / "models" / "block" / f"blueprint_table_{part}.json", {
            "parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {**tex, "board": rid(f"block/blueprint_table_board_{'left' if part == 'main' else 'right'}"),
                         "particle": tex["side"]},
            "elements": table_elements(part)})
    write(assets / "blockstates" / "blueprint_table.json", {"variants": {
        f"facing={face},part={part}": {"model": rid(f"block/blueprint_table_{part}"), **({"y": y} if y else {})}
        for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for part in ("main", "side")}})
    # The item shows the whole station: both halves side by side, centred on the block.
    whole = [shift(e, 8) for e in table_elements("main")] + [shift(e, -8) for e in table_elements("side")]
    write(assets / "models" / "item" / "blueprint_table.json", {
        "parent": "minecraft:block/block",
        "textures": {**tex, "board_left": rid("block/blueprint_table_board_left"), "board_right": rid("block/blueprint_table_board_right"),
                     "particle": tex["side"]},
        "elements": [retexture(e, "#board", "#board_left" if i < len(table_elements("main")) else "#board_right") for i, e in enumerate(whole)],
        "display": {"gui": {"rotation": [30, 160, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
                    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
                    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}})
    write(assets / "items" / "blueprint_table.json", {"model": {"type": "minecraft:model", "model": rid("item/blueprint_table")}})
    write(assets / "models" / "block" / "creative_energy_cell.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/creative_energy_cell")}})
    write(assets / "blockstates" / "creative_energy_cell.json", {"variants": {"": {"model": rid("block/creative_energy_cell")}}})
    write(assets / "items" / "creative_energy_cell.json", {"model": {"type": "minecraft:model", "model": rid("block/creative_energy_cell")}})
    for name in ("blueprint", "blueprint_part", "blueprint_imported"):
        write(assets / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    # The item picks its colour from the kind BlueprintItem.stack writes into custom_model_data (strings[0]).
    write(assets / "items" / "blueprint.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
        "cases": [{"when": "part", "model": {"type": "minecraft:model", "model": rid("item/blueprint_part")}},
                  {"when": "imported", "model": {"type": "minecraft:model", "model": rid("item/blueprint_imported")}}],
        "fallback": {"type": "minecraft:model", "model": rid("item/blueprint")}}})


def _faces(default, **over):
    return {f: ({"texture": over[f]} if isinstance(over.get(f), str) else over[f]) if f in over else {"texture": default}
            for f in ("north", "south", "east", "west", "up", "down")}


BOARD_TILT = {"origin": [8, 11, 1], "axis": "x", "angle": -22.5}


def table_elements(part):
    """The model of one half of the drafting station, facing north (the pencil ledge towards the player). The main
    half is the player's left (its outer end at x=16), the side half the right (outer end at x=0), with the lamp."""
    outer = 16 if part == "main" else 0
    def x(a, b):  # an x span measured from the outer end inwards
        return (outer - b, outer - a) if outer == 16 else (a, b)
    def box(x0, y0, z0, x1, y1, z1, faces, tilt=False, **extra):
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces, **extra}
        if tilt:
            e["rotation"] = dict(BOARD_TILT)
        return e
    steel, alu = _faces("#side"), _faces("#alu")
    els = []
    lx0, lx1 = x(1, 3)
    els += [box(lx0, 1, 2, lx1, 11, 4, steel), box(lx0, 1, 12, lx1, 15, 14, steel),          # front and back legs
            box(*x(0.5, 3.5)[:1], 0, 1, x(0.5, 3.5)[1], 1, 15, _faces("#rubber")),              # foot
            box(lx0, 10, 2, lx1, 11, 14, steel)]                                                 # top rail
    sx0, sx1 = x(3, 16)
    els += [box(sx0, 3, 7.5, sx1, 4, 8.5, steel),                                               # stretcher
            box(sx0, 5, 3, sx1, 5.5, 13, _faces("#birch"))]                                     # shelf
    board_uv = {"texture": "#board", "uv": [16, 16, 0, 0]}
    els.append(box(0, 11, 1, 16, 11.8, 16, _faces("#birch", up=board_uv), tilt=True))         # the drawing board
    els.append(box(0, 10.4, 0, 16, 11.8, 1.4, alu))                                            # pencil ledge
    els.append(box(0, 11.8, 14.6, 16, 12.6, 15.6, alu, tilt=True))                             # drafting-machine track
    if part == "main":
        els += [box(2, 5.5, 4, 12, 6.6, 12, _faces("#paper")),                                  # a stack of drawings
                box(6, 11.8, 0.3, 11, 12.3, 0.8, _faces("#bulb")),                              # pencils on the ledge
                box(3, 11.8, 0.4, 5, 12.2, 0.9, _faces("#rubber")),
                box(10, 11.8, 7, 10.8, 12.3, 14.6, alu, tilt=True),                             # the arm
                box(3, 11.8, 6, 13, 12.2, 7, alu, tilt=True),                                   # horizontal scale
                box(3, 11.8, 6, 4, 12.2, 13, alu, tilt=True)]                                   # vertical scale
    else:
        roll = _faces("#roll", east="#roll_end", west="#roll_end")
        els += [box(4, 5.5, 4, 15, 7, 5.5, roll), box(5, 5.5, 6, 16, 7, 7.5, roll),           # rolled plans on the shelf
                box(4, 7, 4.8, 13, 8.4, 6.2, roll),
                box(1.5, 10, 15, 2.5, 26, 16, steel),                                           # lamp post, arm, shade
                box(1.5, 25, 6, 2.5, 26, 15, steel),
                box(0.8, 23, 4.8, 3.2, 25.2, 7.2, steel),                                      # shade: a cap over
                box(0, 21, 4, 4, 23, 8, _faces("#side", down="#bulb")),                         # a flared rim
                box(0.8, 20.4, 4.8, 3.2, 21, 7.2, _faces("#bulb"), light_emission=15)]
    return els


def shift(element, dx):
    e = {**element, "from": [element["from"][0] + dx, *element["from"][1:]], "to": [element["to"][0] + dx, *element["to"][1:]]}
    if "rotation" in e:
        e["rotation"] = {**e["rotation"], "origin": [e["rotation"]["origin"][0] + dx, *e["rotation"]["origin"][1:]]}
    return e


def retexture(element, old, new):
    return {**element, "faces": {f: ({**v, "texture": new} if v["texture"] == old else v) for f, v in element["faces"].items()}}


def write_loot(write, rid, out, self_drop):
    for block in SELF_DROP:
        write(out / f"{block}.json", self_drop(block))
    # The drafting station drops from its main half only (breaking the side half breaks the main one too).
    table = self_drop("blueprint_table")
    table["pools"][0]["entries"][0]["condition"] = {"type": "minecraft:match_block", "blocks": f"{MOD}:blueprint_table",
                                                    "state": {"part": "main"}}
    write(out / "blueprint_table.json", table)



def preview(name, path, scale=12):
    """An isometric-ish preview: front (south) elevation and a top view side by side."""
    from PIL import Image, ImageDraw
    data = BUILDERS[name]()
    w, h, d = data["size"]
    colours = {"stone_bricks": (122, 122, 122), "cobblestone": (110, 110, 110), "stripped_oak_log": (176, 138, 84),
               "glass": (170, 210, 230), "spruce_stairs": (104, 76, 46), "polished_andesite": (136, 138, 136),
               "stone_brick_slab": (128, 128, 128), "white_wool": (236, 236, 236), "dark_oak_stairs": (66, 44, 24),
               "oak_planks": (160, 128, 78), "dark_oak_slab": (66, 44, 24), "oak_fence": (160, 128, 78),
               "chiseled_stone_bricks": (118, 118, 118), "drone_tower_core": (150, 38, 30)}
    cells = {}
    for y, layer in enumerate(data["layers"]):
        for z, row in enumerate(layer):
            for x, ch in enumerate(row):
                if ch != ".":
                    block = data["palette"][ch].split(":")[1].split("[")[0]
                    cells[(x, y, z)] = colours.get(block, (200, 0, 200))
    img = Image.new("RGB", ((w + d + 3) * scale, (max(h, d) + 2) * scale), (26, 24, 28))
    dr = ImageDraw.Draw(img)
    for x in range(w):  # front: nearest z wins
        for y in range(h):
            best = None
            for z in range(d):
                if (x, y, z) in cells:
                    best = (z, cells[(x, y, z)])
            if best:
                k = 0.6 + 0.4 * best[0] / max(1, d - 1)
                col = tuple(int(v * k) for v in best[1])
                dr.rectangle([(x + 1) * scale, (h - y) * scale, (x + 2) * scale - 1, (h - y + 1) * scale - 1], fill=col)
    ox = (w + 2) * scale
    for x in range(w):  # top: highest y wins
        for z in range(d):
            for y in range(h - 1, -1, -1):
                if (x, y, z) in cells:
                    k = 0.55 + 0.45 * y / max(1, h - 1)
                    col = tuple(int(v * k) for v in cells[(x, y, z)])
                    dr.rectangle([ox + x * scale, (z + 1) * scale, ox + (x + 1) * scale - 1, (z + 2) * scale - 1], fill=col)
                    break
    ax, _, az = data["anchor"]
    dr.ellipse([ox + ax * scale + 2, (az + 1) * scale + 2, ox + (ax + 1) * scale - 2, (az + 2) * scale - 2], fill=(226, 84, 62))
    img.save(path)


if __name__ == "__main__":
    write_all()
    import sys
    if len(sys.argv) > 1:
        for n in BUILT_IN:
            preview(n, Path(sys.argv[1]) / f"{n}_preview.png")
