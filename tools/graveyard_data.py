"""JSON resources for the graveyard pack, from tools/graveyard.py: every headstone's models (one per block of it and
stage of weathering), blockstates, item models, words, loot (the epitaph goes with the item), recipes and tags.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files: blockstate variants, block models,
loot with copy_components from the block entity, stonecutting and shaped recipes. A memorial taller or longer than one
block is cut into one model per block: unturned boxes are cut at the block's edges (only along the way it is tall or
long, so arms and finials may reach out over the blocks beside it), turned boxes go whole to the block their middle is
in. Faces reaching outside a block's texture square are given UVs inside it (decor3_data.fitted).
"""
from decor_data import MOD, HORIZONTAL, rid, turned, block_model
from decor3_data import fitted
from decor7_data import scaled
from graveyard import FEATURE, STAGES, HEADSTONES, STONES, EPITAPH, CHISEL_RECIPE, PARTS
import graveyard_models as gm
import graveyard_grounds as gg

MODELS = {"gothic": gm.gothic, "willow_urn": gm.willow_urn, "winged_skull": gm.winged_skull, "lamb": gm.lamb,
          "broken_column": gm.broken_column, "celtic_cross": gm.celtic_cross, "scroll": gm.scroll, "table_tomb": gm.table_tomb,
          "ledger": gm.ledger, "obelisk": gm.obelisk, "draped_urn": gm.draped_urn, "angel_of_grief": gm.angel_of_grief,
          "trumpet_angel": gm.trumpet_angel, "mortsafe": gm.mortsafe, "hound": gm.hound, "gargoyle": gm.gargoyle, "kerbed_grave": gg.kerbed_grave,
          "planted_grave": gg.planted_grave, "memorial_bench": gg.memorial_bench, "open_grave": gg.open_grave}
AXES = {"north": None, "south": None, "east": None, "west": None}
FACE_AXES = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}


def textures(stone, stage, upper):
    """The texture variables for `stone` at `stage`; above the first block every face is moss-free from the ground."""
    if stone == "iron":
        return {"iron": f"gy_iron_{stage}", "soil": "minecraft:block/coarse_dirt", "ivy": "gy_ivy", "oak": f"gy_oak_{stage}",
                "bronze": f"gy_bronze_{stage}"}
    ground = f"{stage}_upper" if upper else stage
    out = {"stone": f"gy_{stone}_{ground}", "top": f"gy_{stone}_{stage}_upper", "relief": f"gy_{stone}_relief_{ground}",
           "relief_top": f"gy_{stone}_relief_{stage}_upper", "ivy": "gy_ivy"}
    if stone == "granite":
        out.update({"rough": f"gy_granite_rough_{stage}", "knot": f"gy_granite_knot_{stage}_upper"})
    # Metals and timber weather with the stone they stand on; the soil in a mortsafe is vanilla's coarse dirt.
    out.update({"iron": f"gy_iron_{stage}", "bronze": f"gy_bronze_{stage}", "soil": "minecraft:block/coarse_dirt", "oak": f"gy_oak_{stage}",
                "chippings": f"gy_chippings_{stage}", "bed": f"gy_flower_bed_{stage}", "pit": "gy_pit", "straps": "gy_straps"})
    return out


def whole(headstone, stage):
    """Every element of a headstone at `stage`, before it is cut into blocks."""
    info = HEADSTONES[headstone]
    elements = MODELS[info["model"]]()
    if stage == "overgrown":
        elements = elements + gm.overgrowth(info["overgrowth"])
    return elements


def size_of(cells):
    """A memorial's size in blocks: right, up, back."""
    return tuple(max(c[k] for c in cells) + 1 for k in range(3))


def cut(elements, cell, size):
    """The part of `elements` in the block at `cell` (right, up, back), moved into it. The memorial is designed `size`
    blocks wide, tall and long, facing north: its first column to the placer's right (-x) is design x 16 * (width - 1);
    up is +y; back is +z. Boxes are cut only along the ways it is more than a block, so arms and finials may reach out
    over the blocks beside it; turned boxes go whole to the block their middle is in."""
    width = size[0]
    shift = [16 * (width - 1 - cell[0]), 16 * cell[1], 16 * cell[2]]
    axes = [k for k in range(3) if size[k] > 1]
    out = []
    for element in elements:
        frm, to = element["from"], element["to"]
        if "rotation" in element:
            inside = True
            for k in axes:
                middle = (frm[k] + to[k]) / 2
                index = middle // 16 if k else (width - 1) - middle // 16
                limit = size[k] - 1
                index = min(max(index, 0), limit)
                want = cell[k]
                inside &= index == want
            if inside:
                moved = {**element, "from": [frm[k] - shift[k] for k in range(3)], "to": [to[k] - shift[k] for k in range(3)],
                         "rotation": dict(element["rotation"])}
                moved["rotation"]["origin"] = [element["rotation"]["origin"][k] - shift[k] for k in range(3)]
                out.append(moved)
            continue
        lo, hi = list(frm), list(to)
        faces = dict(element["faces"])
        empty = False
        for k in axes:
            cell_lo, cell_hi = shift[k], shift[k] + 16
            # The outermost blocks keep whatever reaches past the memorial's edge.
            first = shift[k] == 0
            last = shift[k] == 16 * (size[k] - 1)
            a = frm[k] if (first and frm[k] < cell_lo) else max(frm[k], cell_lo)
            z = to[k] if (last and to[k] > cell_hi) else min(to[k], cell_hi)
            if z - a <= 1e-6:
                empty = True
                break
            low_face, high_face = FACE_AXES[k]
            if a > frm[k]:
                faces.pop(low_face, None)
            if z < to[k]:
                faces.pop(high_face, None)
            lo[k], hi[k] = a, z
        if empty or not faces:
            continue
        out.append({**element, "from": [round(lo[k] - shift[k], 4) for k in range(3)],
                    "to": [round(hi[k] - shift[k], 4) for k in range(3)], "faces": faces})
    return out


def model_name(headstone, part, stage):
    return f"{headstone}_{stage}" if len(HEADSTONES[headstone]["cells"]) == 1 else f"{headstone}_{part}_{stage}"


def part_model(headstone, part, stage):
    info = HEADSTONES[headstone]
    cells = info["cells"]
    elements = whole(headstone, stage)
    if len(cells) > 1:
        elements = cut(elements, cells[part], size_of(cells))
    tex = textures(info["stone"], stage, upper=cells[part][1] > 0)
    used = {face["texture"][1:] for e in elements for face in e["faces"].values()}
    tex = {k: v for k, v in tex.items() if k in used}
    particle = next((tex[k] for k in ("stone", "rough", "iron") if k in tex), None) or next(v for v in tex.values() if ":" not in v)
    return fitted(block_model(tex, elements, particle))


def item_model(headstone):
    """A memorial of more than one block, scaled down whole to fit an item's square."""
    info = HEADSTONES[headstone]
    elements = whole(headstone, "clean")
    tex = textures(info["stone"], "clean", upper=True)
    used = {face["texture"][1:] for e in elements for face in e["faces"].values()}
    tex = {k: v for k, v in tex.items() if k in used}
    width = max(e["to"][0] for e in elements)
    height = max(e["to"][1] for e in elements)
    length = max(e["to"][2] for e in elements)
    factor = min(1.0, 16 / max(width, height, length))
    offset = ((16 - width * factor) / 2 if width > 16 else 8 * (1 - factor), 0,
              (16 - length * factor) / 2 if length > 16 else 8 * (1 - factor))
    particle = next((tex[k] for k in ("stone", "rough", "iron") if k in tex), None) or next(v for v in tex.values() if ":" not in v)
    return fitted(block_model(tex, scaled(elements, factor, offset), particle))


def assets(root, write, lang):
    models = root / "models" / "block"
    for headstone, info in HEADSTONES.items():
        cells = info["cells"]
        for part in range(len(cells)):
            for stage in STAGES:
                write(models / f"{model_name(headstone, part, stage)}.json", part_model(headstone, part, stage))
        variants = {}
        for facing in HORIZONTAL:
            for part in range(PARTS):
                for weathering, stage in enumerate(STAGES):
                    for waxed in ("false", "true"):
                        model = rid(f"block/{model_name(headstone, min(part, len(cells) - 1), stage)}")
                        variants[f"facing={facing},part={part},waxed={waxed},weathering={weathering}"] = turned(model, facing)
        write(root / "blockstates" / f"{headstone}.json", {"variants": variants})
        if len(cells) == 1:
            item = rid(f"block/{model_name(headstone, 0, 'clean')}")
        else:
            write(models / f"{headstone}_item.json", item_model(headstone))
            item = rid(f"block/{headstone}_item")
        write(root / "items" / f"{headstone}.json", {"model": {"type": "minecraft:model", "model": item}})
        lang[f"block.{MOD}.{headstone}"] = info["display"]
    chisel = EPITAPH["chisel"]
    write(root / "models" / "item" / f"{chisel}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{chisel}")}})
    write(root / "items" / f"{chisel}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{chisel}")}})
    lang[f"item.{MOD}.{chisel}"] = EPITAPH["chisel_display"]
    building_assets(root, write, lang)
    lang.update({
        "screen.jugcraft.epitaph.title": "Cut an Epitaph",
        "screen.jugcraft.epitaph.line": "Line %s",
        "screen.jugcraft.epitaph.hint": "Each line is cut as large as it fits.",
        "tooltip.jugcraft.headstone.epitaph": "Epitaph:",
        "tooltip.jugcraft.headstone.waxed": "Waxed",
        "tooltip.jugcraft.chisel": "Use on a headstone to cut its epitaph",
        "message.jugcraft.epitaph.engraved": "The epitaph is cut",
        "message.jugcraft.epitaph.no_session": "Open the headstone with the chisel again",
        "message.jugcraft.epitaph.no_chisel": "You need the Stonemason's Chisel in hand",
        "message.jugcraft.epitaph.too_far": "Too far from the headstone",
        "message.jugcraft.epitaph.not_allowed": "You may not cut stone here",
        "message.jugcraft.epitaph.not_a_headstone": "That is no longer a headstone",
        "message.jugcraft.epitaph.invalid": "An epitaph has at most 4 lines of 24 characters",
        "message.jugcraft.headstone.waxed": "It is waxed and will not weather",
        "message.jugcraft.headstone.clean": "It is as clean as the day it was cut",
        "message.jugcraft.headstone.overgrown": "It cannot grow any wilder",
    })


def loot(out, write):
    for headstone in HEADSTONES:
        write(out / f"{headstone}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:match_block", "blocks": rid(headstone), "state": {"part": "0"}}]},
            "entries": [{"type": "minecraft:item", "name": rid(headstone), "modifier": [
                {"type": "minecraft:copy_components", "include": [rid(EPITAPH["component"])], "source": "block_entity"}]}],
            "rolls": 1}], "random_sequence": f"{MOD}:blocks/{headstone}"})
    building_loot(out, write)


def recipes(out, write, conditions):
    for headstone, info in HEADSTONES.items():
        recipe = info["recipe"]
        if "stonecutting" in recipe:
            write(out / f"{headstone}_from_stonecutting.json", {"fabric:load_conditions": conditions(), "type": "minecraft:stonecutting",
                                                                "ingredient": recipe["stonecutting"], "result": {"id": rid(headstone), "count": 1}})
        else:
            write(out / f"{headstone}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped",
                                              "category": "building", "pattern": recipe["pattern"], "key": recipe["key"],
                                              "result": {"id": rid(headstone), "count": 1}})
    write(out / f"{EPITAPH['chisel']}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped",
                                             "category": "equipment", "pattern": CHISEL_RECIPE["pattern"], "key": CHISEL_RECIPE["key"],
                                             "result": {"id": rid(EPITAPH["chisel"]), "count": 1}})
    building_recipes(out, write, conditions)


def tags(tags):
    for headstone in HEADSTONES:
        tags.add("block", "minecraft:mineable/pickaxe", rid(headstone))
        tags.add("block", "jugcraft:headstones", rid(headstone))
        tags.add("item", "jugcraft:headstones", rid(headstone))
    building_tags(tags)


# ---------------------------------------------------------------- pack 3: buildings
import math

import graveyard_buildings as gb
from graveyard import BUILDINGS, MAUSOLEUM_DOOR
from decor3_data import default_uv, DOOR_CLOSED, DOOR_OPEN

# Grid cells to try, nearest first, when something drawn falls in a cell the building leaves open (a doorway's
# threshold, a room's floor): the one below, those beside, then those across a corner, then above.
NEIGHBOURS = sorted(((dx, dy, dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if (dx, dy, dz) != (0, 0, 0)),
                    key=lambda d: (d[1] > 0, abs(d[0]) + abs(d[2]) + abs(d[1]), d[1] != -1, d))
EXTRA_TEXTURES = {"floor": "gy_marble_floor", "glass": "gy_stained_glass", "lamp": "gy_lamp_glass", "lantern": "gy_lantern_glass",
                  "door_glass": "gy_door_glass"}


def building_textures(stone, stage, upper):
    tex = textures(stone, stage, upper)
    tex.update({"oak": f"gy_oak_{stage}", "roof": f"gy_roof_slate_{stage}", **EXTRA_TEXTURES})
    return tex


def fit_uvs(elements):
    """Pins the UVs of faces reaching outside the block inside the texture: moved in when they fit, squeezed when a
    face is longer than a block."""
    for element in elements:
        for side, face in element["faces"].items():
            if "uv" in face:
                continue
            u0, v0, u1, v1 = default_uv(side, element["from"], element["to"])
            if min(u0, v0, u1, v1) >= 0 and max(u0, v0, u1, v1) <= 16:
                continue
            out = []
            for a, z in ((u0, u1), (v0, v1)):
                lo, hi = min(a, z), max(a, z)
                if hi - lo > 16:
                    lo, hi = 0.0, 16.0
                else:
                    shift = min(max(lo, 0), 16 - (hi - lo)) - lo
                    lo, hi = lo + shift, hi + shift
                out.append((round(lo, 3), round(hi, 3)) if a <= z else (round(hi, 3), round(lo, 3)))
            face["uv"] = [out[0][0], out[1][0], out[0][1], out[1][1]]
    return elements


def grid_cut(elements, size):
    """`elements` (design pixels) cut into the cells of a grid `size` blocks wide, tall and deep: {cell: fragments}.
    Unturned boxes are cut at every cell boundary inside the grid (and the faces at a cut dropped); what reaches past
    the grid's outer faces stays with its outermost cells. Turned boxes go whole to the cell their middle is in."""
    out = {}
    for e in elements:
        if "rotation" in e:
            middle = [(e["from"][k] + e["to"][k]) / 2 for k in range(3)]
            cell = tuple(min(max(int(math.floor(middle[k] / 16)), 0), size[k] - 1) for k in range(3))
            out.setdefault(cell, []).append(e)
            continue
        ranges = []
        for k in range(3):
            a, z = e["from"][k], e["to"][k]
            first = min(max(int(math.floor(a / 16)), 0), size[k] - 1)
            last = min(max(int(math.floor((z - 1e-6) / 16)), 0), size[k] - 1)
            pieces = []
            for i in range(first, last + 1):
                lo = a if i == 0 else max(a, 16 * i)
                hi = z if i == size[k] - 1 else min(z, 16 * (i + 1))
                if hi - lo > 1e-6:
                    pieces.append((i, lo, hi))
            ranges.append(pieces)
        for ix, x0, x1 in ranges[0]:
            for iy, y0, y1 in ranges[1]:
                for iz, z0, z1 in ranges[2]:
                    faces = dict(e["faces"])
                    lo, hi = (x0, y0, z0), (x1, y1, z1)
                    for k, (low_face, high_face) in FACE_AXES.items():
                        if lo[k] > e["from"][k] + 1e-6:
                            faces.pop(low_face, None)
                        if hi[k] < e["to"][k] - 1e-6:
                            faces.pop(high_face, None)
                    if faces:
                        out.setdefault((ix, iy, iz), []).append({**e, "from": [round(v, 4) for v in lo], "to": [round(v, 4) for v in hi],
                                                                 "faces": faces})
    return out


def building_parts(building):
    """The grid cells of `building`'s parts, part 0 first: every cell holding any of it but those left open."""
    info = BUILDINGS[building]
    frags = grid_cut(getattr(gb, info["model"])(), info["size"])
    open_cells = set(info["open"])
    cells = sorted((c for c in frags if c not in open_cells), key=lambda c: (c != tuple(info["origin"]), c[1], c[2], c[0]))
    if cells[0] != tuple(info["origin"]):
        raise SystemExit(f"{building}: its origin {info['origin']} holds nothing")
    return cells


def building_fragments(building, ivy=False):
    """{part cell: its elements in design pixels}, those falling in open cells given to the nearest part beside them:
    the building itself, or (`ivy`) only the ivy that grows over it when it is overgrown."""
    info = BUILDINGS[building]
    elements = getattr(gb, info["ivy" if ivy else "model"])()
    cells = building_parts(building)
    parts = set(cells)
    frags = grid_cut(elements, info["size"])
    out = {c: [] for c in cells}
    for cell, items in frags.items():
        if cell in parts:
            out[cell] += items
            continue
        for d in NEIGHBOURS:
            near = (cell[0] + d[0], cell[1] + d[1], cell[2] + d[2])
            if near in parts:
                out[near] += items
                break
        else:
            raise SystemExit(f"{building}: nothing beside open cell {cell} to draw what falls in it")
    return out


def moved(elements, cell):
    """`elements` in design pixels moved into the block of grid `cell`."""
    shift = [16 * cell[k] for k in range(3)]
    out = []
    for e in elements:
        m = {**e, "from": [round(e["from"][k] - shift[k], 4) for k in range(3)], "to": [round(e["to"][k] - shift[k], 4) for k in range(3)],
             "faces": {side: dict(face) for side, face in e["faces"].items()}}
        if "rotation" in e:
            m["rotation"] = {**e["rotation"], "origin": [round(e["rotation"]["origin"][k] - shift[k], 4) for k in range(3)]}
        for v in m["from"] + m["to"]:
            if v < -16 or v > 32:
                raise SystemExit(f"An element reaches {v} pixels from its block at cell {cell}: {e['from']} {e['to']}")
        out.append(m)
    return out


def _corners(e):
    """The eight corners of element `e`, turned by its rotation."""
    pts = [(x, y, z) for x in (e["from"][0], e["to"][0]) for y in (e["from"][1], e["to"][1]) for z in (e["from"][2], e["to"][2])]
    if "rotation" not in e:
        return pts
    r = e["rotation"]
    a = math.radians(r["angle"])
    c, s = math.cos(a), math.sin(a)
    o = r["origin"]
    out = []
    for p in pts:
        x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
        if r["axis"] == "z":
            x, y = x * c - y * s, x * s + y * c
        elif r["axis"] == "y":
            x, z = x * c + z * s, -x * s + z * c
        else:
            y, z = y * c - z * s, y * s + z * c
        out.append((x + o[0], y + o[1], z + o[2]))
    return out


def collision(elements, step=2, overlap=0.5):
    """The boxes a part can be walked into and hit by: its elements (their bounds, if turned) in voxels of `step`
    pixels, each filled if an element covers at least `overlap` of it along every axis, merged into as few boxes as
    a greedy sweep finds. Ivy and thin skins (floors drawn over the ground) are left out."""
    n = 16 // step
    grid = [[[False] * n for _ in range(n)] for _ in range(n)]
    for e in elements:
        if any(face.get("texture") == "#ivy" for face in e["faces"].values()):
            continue
        pts = _corners(e)
        lo = [max(0.0, min(p[k] for p in pts)) for k in range(3)]
        hi = [min(16.0, max(p[k] for p in pts)) for k in range(3)]
        if any(hi[k] - lo[k] < 0.3 for k in range(3)):
            continue
        ranges = []
        for k in range(3):
            ranges.append([i for i in range(n) if min(hi[k], (i + 1) * step) - max(lo[k], i * step) >= overlap - 1e-6])
        for i in ranges[0]:
            for j in ranges[1]:
                for k in ranges[2]:
                    grid[i][j][k] = True
    used = [[[False] * n for _ in range(n)] for _ in range(n)]
    boxes = []
    for j in range(n):
        for k in range(n):
            for i in range(n):
                if not grid[i][j][k] or used[i][j][k]:
                    continue
                i1 = i
                while i1 + 1 < n and grid[i1 + 1][j][k] and not used[i1 + 1][j][k]:
                    i1 += 1
                k1 = k
                while k1 + 1 < n and all(grid[a][j][k1 + 1] and not used[a][j][k1 + 1] for a in range(i, i1 + 1)):
                    k1 += 1
                j1 = j
                while j1 + 1 < n and all(grid[a][j1 + 1][c] and not used[a][j1 + 1][c] for a in range(i, i1 + 1) for c in range(k, k1 + 1)):
                    j1 += 1
                for a in range(i, i1 + 1):
                    for bb in range(j, j1 + 1):
                        for c in range(k, k1 + 1):
                            used[a][bb][c] = True
                boxes.append([i * step, j * step, k * step, (i1 + 1) * step, (j1 + 1) * step, (k1 + 1) * step])
    return boxes


def relative(building, cell):
    """Grid `cell` as Java gives a part's cell: right, up and back from part 0."""
    o = BUILDINGS[building]["origin"]
    return [o[0] - cell[0], cell[1] - o[1], cell[2] - o[2]]


def building_layout(building):
    """What GraveyardBuildingBlock.Building reads: cells, collision boxes, inscriptions (relative to part 0), which
    inscription each part cuts, and each part's light."""
    info = BUILDINGS[building]
    cells = building_parts(building)
    clean = building_fragments(building)
    o = info["origin"]
    texts = []
    for t in info["texts"]():
        texts.append({**t, "x": round(t["x"] - 16 * o[0], 4), "y": round(t["y"] - 16 * o[1], 4), "z": round(t["z"] - 16 * o[2], 4),
                      "max_scale": round(t["max_scale"], 6)})
    slots = info["slots"]()
    shapes = []
    for c in cells:
        boxes = collision(moved(clean[c], c))
        if not boxes:
            boxes = [[6, 0, 6, 10, 2, 10]]
        shapes.append(boxes)
    return {"id": building, "stone": info["stone"], "sound": info["sound"], "cells": [relative(building, c) for c in cells],
            "shapes": shapes, "texts": texts, "slots": [slots.get(c, 0) for c in cells],
            "light": [info["light"].get(c, 0) for c in cells]}


def building_model_name(building, part, stage=None):
    return f"{building}_{part}" if stage is None else f"{building}_{part}_{stage}"


def building_assets(root, write, lang):
    """Each building's models: one of every part's shape, a small one for each stage of weathering giving it that
    stage's textures, and one of the ivy over each part it grows on; a blockstate drawing the part at its stage, and
    its ivy when overgrown; the item model; and the layout Java reads."""
    models = root / "models" / "block"
    layouts = []
    for building, info in BUILDINGS.items():
        cells = building_parts(building)
        clean = building_fragments(building)
        ivy = building_fragments(building, ivy=True)
        parts = []
        for n, cell in enumerate(cells):
            upper = cell[1] > 0
            base = fit_uvs(moved(clean[cell], cell))
            tex = building_textures(info["stone"], "clean", upper)
            used = {face["texture"][1:] for e in base for face in e["faces"].values()}
            write(models / f"{building_model_name(building, n)}.json",
                  block_model({k: v for k, v in tex.items() if k in used}, base, _particle(tex, used)))
            for weathering, stage in enumerate(STAGES):
                tex = building_textures(info["stone"], stage, upper)
                textures_used = {k: (v if ":" in v else rid(f"block/{v}")) for k, v in tex.items() if k in used}
                write(models / f"{building_model_name(building, n, stage)}.json",
                      {"parent": rid(f"block/{building_model_name(building, n)}"),
                       "textures": {"particle": rid(f"block/{_particle(tex, used)}"), **textures_used}})
                for facing in HORIZONTAL:
                    parts.append({"when": {"facing": facing, "part": str(n), "weathering": str(weathering)},
                                  "apply": turned(rid(f"block/{building_model_name(building, n, stage)}"), facing)})
            if ivy[cell]:
                write(models / f"{building_model_name(building, n)}_ivy.json",
                      block_model({"ivy": "gy_ivy"}, fit_uvs(moved(ivy[cell], cell)), "gy_ivy"))
                for facing in HORIZONTAL:
                    parts.append({"when": {"facing": facing, "part": str(n), "weathering": "3"},
                                  "apply": turned(rid(f"block/{building_model_name(building, n)}_ivy"), facing)})
        write(root / "blockstates" / f"{building}.json", {"multipart": parts})
        write(models / f"{building}_item.json", building_item_model(building))
        write(root / "items" / f"{building}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{building}_item")}})
        lang[f"block.{MOD}.{building}"] = info["display"]
        layouts.append(building_layout(building))
    write(root.parent.parent / MOD / "graveyard_buildings.json", layouts)
    door_assets(root, write, lang)
    vase_assets(root, write, lang)


def _particle(tex, used):
    for key in ("stone", "rough", "oak", "iron", "bronze"):
        if key in used and key in tex:
            return tex[key]
    return tex[sorted(used)[0]] if used else tex["stone"]


def building_item_model(building, limit=160):
    """The whole building, clean, scaled down to an item's square; its largest elements only, so an item in the hand
    is not as heavy to draw as the building itself."""
    info = BUILDINGS[building]
    elements = getattr(gb, info["model"])()

    def volume(e):
        return (e["to"][0] - e["from"][0]) * (e["to"][1] - e["from"][1]) * (e["to"][2] - e["from"][2])
    elements = sorted(elements, key=volume, reverse=True)[:limit]
    width = max(e["to"][0] for e in elements) - min(e["from"][0] for e in elements)
    height = max(e["to"][1] for e in elements)
    depth = max(e["to"][2] for e in elements) - min(e["from"][2] for e in elements)
    x0 = min(e["from"][0] for e in elements)
    z0 = min(e["from"][2] for e in elements)
    factor = 16 / max(width, height, depth)
    offset = ((16 - width * factor) / 2 - x0 * factor, 0, (16 - depth * factor) / 2 - z0 * factor)
    small = scaled(elements, factor, offset)
    tex = building_textures(info["stone"], "clean", True)
    used = {face["texture"][1:] for e in small for face in e["faces"].values()}
    return fitted(block_model({k: v for k, v in tex.items() if k in used}, small, _particle(tex, used)))


def door_assets(root, write, lang):
    """The Bronze Mausoleum Door: one model for each half, the same whichever way it is hung or swung (it is
    symmetric), turned as vanilla turns a door."""
    door = MAUSOLEUM_DOOR["id"]
    models = root / "models" / "block"
    whole = gb.mausoleum_door()
    tex = {"bronze": "gy_bronze_clean", "door_glass": "gy_door_glass"}
    for half, cell in (("bottom", (0, 0, 0)), ("top", (0, 1, 0))):
        frags = grid_cut(whole, (1, 2, 1)).get(cell, [])
        write(models / f"{door}_{half}.json", fitted(block_model(tex, moved(frags, cell), "gy_bronze_clean")))
    variants = {}
    for facing in HORIZONTAL:
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for is_open in ("false", "true"):
                    y = DOOR_OPEN[hinge][facing] if is_open == "true" else DOOR_CLOSED[facing]
                    variant = {"model": rid(f"block/{door}_{part}")}
                    if y:
                        variant["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={is_open}"] = variant
    write(root / "blockstates" / f"{door}.json", {"variants": variants})
    write(root / "models" / "item" / f"{door}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{door}")}})
    write(root / "items" / f"{door}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{door}")}})
    lang[f"block.{MOD}.{door}"] = MAUSOLEUM_DOOR["display"]


def building_loot(out, write):
    for building in BUILDINGS:
        write(out / f"{building}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:match_block", "blocks": rid(building), "state": {"part": "0"}}]},
            "entries": [{"type": "minecraft:item", "name": rid(building), "modifier": [
                {"type": "minecraft:copy_components", "include": [rid(EPITAPH["component"]), rid("inscriptions")], "source": "block_entity"}]}],
            "rolls": 1}], "random_sequence": f"{MOD}:blocks/{building}"})
    door = MAUSOLEUM_DOOR["id"]
    write(out / f"{door}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:survives_explosion"},
            {"type": "minecraft:match_block", "blocks": rid(door), "state": {"half": "lower"}}]},
        "entries": [{"type": "minecraft:item", "name": rid(door)}], "rolls": 1}], "random_sequence": f"{MOD}:blocks/{door}"})
    vase_loot(out, write)


def building_recipes(out, write, conditions):
    for building, info in list(BUILDINGS.items()) + [(MAUSOLEUM_DOOR["id"], MAUSOLEUM_DOOR)]:
        recipe = info["recipe"]
        write(out / f"{building}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped",
                                         "category": "building", "pattern": recipe["pattern"], "key": recipe["key"],
                                         "result": {"id": rid(building), "count": recipe.get("count", 1)}})
    vase_recipes(out, write, conditions)


def building_tags(tags):
    for building, info in BUILDINGS.items():
        tags.add("block", f"minecraft:mineable/{info['tool']}", rid(building))
        tags.add("block", "jugcraft:graveyard_buildings", rid(building))
        tags.add("item", "jugcraft:graveyard_buildings", rid(building))
    door = MAUSOLEUM_DOOR["id"]
    tags.add("block", "minecraft:mineable/pickaxe", rid(door))
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:doors", rid(door))
    vase_tags(tags)


# ---------------------------------------------------------------- pack 4: the grave vase and the lamp post
from graveyard import GRAVE_VASE, LAMP_POST


def vase_assets(root, write, lang):
    """The grave vase: one model empty and one for each bouquet, fresh and wilted; its blockstate by flowers and
    wilting; its item, the empty vase."""
    vase = GRAVE_VASE["block"]
    models = root / "models" / "block"
    tex = {"rough": "gy_granite_rough_clean", "stone": "gy_granite_clean", "top": "gy_granite_clean", "bronze": "gy_bronze_worn",
           "pit": "gy_pit", "leaves": "gy_leaves"}

    def model(elements, petals):
        t = dict(tex)
        if petals:
            t["petals"] = f"gy_petals_{petals}"
        used = {face["texture"][1:] for e in elements for face in e["faces"].values()}
        return fitted(block_model({k: v for k, v in t.items() if k in used}, elements, "gy_bronze_worn"))

    write(models / f"{vase}.json", model(gg.grave_vase(), None))
    variants = {"flowers=none,wilted=false": {"model": rid(f"block/{vase}")}, "flowers=none,wilted=true": {"model": rid(f"block/{vase}")}}
    for colour in GRAVE_VASE["colours"]:
        write(models / f"{vase}_{colour}.json", model(gg.grave_vase(colour), colour))
        write(models / f"{vase}_{colour}_wilted.json", model(gg.grave_vase(colour, wilted=True), "wilted"))
        variants[f"flowers={colour},wilted=false"] = {"model": rid(f"block/{vase}_{colour}")}
        variants[f"flowers={colour},wilted=true"] = {"model": rid(f"block/{vase}_{colour}_wilted")}
    write(root / "blockstates" / f"{vase}.json", {"variants": variants})
    write(root / "items" / f"{vase}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{vase}")}})
    lang[f"block.{MOD}.{vase}"] = GRAVE_VASE["display"]
    lang["message.jugcraft.grave_vase.not_a_flower"] = "Only small flowers go in a grave vase"

    post = LAMP_POST["block"]
    whole = {True: gg.lamp_post(True), False: gg.lamp_post(False)}
    variants = {}
    for part in range(3):
        for lit_now in (False, True):
            frags = grid_cut(whole[lit_now], (1, 3, 1)).get((0, part, 0), [])
            name = f"{post}_{part}" + ("_lit" if lit_now and part == 2 else "")
            if lit_now and part != 2:
                continue
            t = {"iron": "gy_iron_clean", "lantern": "gy_lantern_glass" if lit_now else "gy_lantern_unlit"}
            used = {face["texture"][1:] for e in frags for face in e["faces"].values()}
            write(models / f"{name}.json", fitted(block_model({k: v for k, v in t.items() if k in used}, moved(frags, (0, part, 0)), "gy_iron_clean")))
        for facing in HORIZONTAL:
            for lit_now in ("false", "true"):
                name = f"{post}_{part}" + ("_lit" if lit_now == "true" and part == 2 else "")
                variants[f"facing={facing},lit={lit_now},part={part}"] = turned(rid(f"block/{name}"), facing)
    write(root / "blockstates" / f"{post}.json", {"variants": variants})
    small = scaled(whole[False], 16 / 48, (16 / 3, 0, 16 / 3))
    write(models / f"{post}_item.json", fitted(block_model({"iron": "gy_iron_clean", "lantern": "gy_lantern_unlit"}, small, "gy_iron_clean")))
    write(root / "items" / f"{post}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{post}_item")}})
    lang[f"block.{MOD}.{post}"] = LAMP_POST["display"]


def vase_loot(out, write):
    vase, post = GRAVE_VASE["block"], LAMP_POST["block"]
    write(out / f"{vase}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": rid(vase)}], "rolls": 1}],
        "random_sequence": f"{MOD}:blocks/{vase}"})
    write(out / f"{post}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:survives_explosion"},
            {"type": "minecraft:match_block", "blocks": rid(post), "state": {"part": "0"}}]},
        "entries": [{"type": "minecraft:item", "name": rid(post)}], "rolls": 1}], "random_sequence": f"{MOD}:blocks/{post}"})


def vase_recipes(out, write, conditions):
    for info in (GRAVE_VASE, LAMP_POST):
        recipe = info["recipe"]
        write(out / f"{info['block']}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped",
                                              "category": "building", "pattern": recipe["pattern"], "key": recipe["key"],
                                              "result": {"id": rid(info["block"]), "count": 1}})


def vase_tags(tags):
    for info in (GRAVE_VASE, LAMP_POST):
        tags.add("block", "minecraft:mineable/pickaxe", rid(info["block"]))
    for colour, flowers in GRAVE_VASE["flowers"].items():
        for flower in flowers:
            tags.add("item", f"jugcraft:grave_flowers/{colour}", flower)
        tags.add("item", "jugcraft:grave_flowers", f"#jugcraft:grave_flowers/{colour}")
    for flower in GRAVE_VASE["others"]:
        tags.add("item", "jugcraft:grave_flowers", flower)
    # Vanilla's small flowers, and those of any pack that adds to them.
    tags.add("item", "jugcraft:grave_flowers", {"id": "#minecraft:small_flowers", "required": False})
