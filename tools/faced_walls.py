"""Faced walls: a room's wall with the hall's finish on its one face that looks onto the hall (Justin's first-floor
look-over, 10 November 2026: "some of the walls don't really look that great with the theme of the building"; finish A of
the sheet, concrete like the pillars, with the pillars' light line carried round the rooms).

The rooms' walls are one block thick and painted (the plant's interior kit), so a room in the tower's hall showed its
cream paint to the hall. Two blocks per material (MATS) put that right without touching the room:

  * faced_wall_<material>: FACING is its face onto the hall, in the material at its phase (as the pillars draw it, the
    two-metre sheet running on from block to block); its other faces are the room's: painted, or on the face opposite
    the plant restroom's wall tile or its cap course (INSIDE), so a tiled room keeps its tiles;
  * faced_line_<material>: the same wall with the neon line's groove and tube along its face (the pillars' own line at
    the pillars' height: tools/neon_lines.py, neon_3d.py), running on into a line beside it (LEFT, RIGHT, as you look at
    the face: another faced line or a Neon Line, which takes a faced line as a line too) and capped where there is none.

write(): the models (from the material's phase models and the Neon Lines' bodies and fittings, so they stay in step with
them), blockstates, items, loot, recipes (the Advanced Workbench's, as the tower's other blocks), names and tag.

The layout side (the tower's generator calls these; nothing here knows the tower):

  * hall_air(b, seeds, cells, y0, y1): the hall's air, filled from the seeds through everything that isn't a wall, glass
    or a door;
  * face(b, air, material, ...): every painted or tiled wall block with a face on that air becomes a faced wall (one
    face on the hall and a room behind it), or the plain material (a corner, a pier, a wall with no room behind it); at
    line_y the faced line or the material's Neon Line, wherever the cell in front is empty;
  * line(b, air, cells, y, material): a Neon Line through blocks of the material itself (the stair's wall), where the
    wall is a block higher still;
  * shape(b): every faced line's ends from its neighbours (tools/neon_lines.shape calls it).

    python3 tools/faced_walls.py        writes the assets and data
"""
from __future__ import annotations

import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
MODELS = ASSETS / "models/block"

# the materials a wall is faced in (each needs the Neon Line of that material: neon_lines.LINE_MATS)
MATS = ("concrete",)
PHASED = {"concrete": "reinforced_concrete", "steel": "steel_armor_plate", "carbon": "carbon_composite_panel"}
NAMES = {"concrete": "Concrete", "steel": "Steel", "carbon": "Carbon Composite"}
# what the face opposite shows: the block a wall was before it was faced -> INSIDE, and INSIDE's texture
INSIDE_OF = {"jugcraft:painted_wall": "painted", "jugcraft:restroom_wall_tile": "tile", "jugcraft:restroom_wall_tile_cap": "tile_cap"}
INSIDE_TEXTURE = {"painted": "painted_wall", "tile": "restroom_wall_tile", "tile_cap": "restroom_wall_tile_cap"}
PAINT = "jugcraft:block/painted_wall"
FACINGS = ("north", "east", "south", "west")
DIRS = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}          # LEFT, looking at the face
CCW = {v: k for k, v in CW.items()}                                               # RIGHT
# the Neon Line's body with only this side open (tools/neon_3d.py: north, east, south, west; o open, x not)
BODY = {"north": "oxxx", "east": "xoxx", "south": "xxox", "west": "xxxo"}


def wall_id(mat):
    return f"faced_wall_{mat}"


def line_id(mat):
    return f"faced_line_{mat}"


def ids():
    return [f(m) for m in MATS for f in (wall_id, line_id)]


# ------------------------------------------------------------------------------------------------------ models

def _load(name):
    return json.loads((MODELS / f"{name}.json").read_text(encoding="utf-8"))


def wall_model(mat, facing, inside, phase):
    """A cube: the material on FACING as its phase model draws that face, INSIDE's finish opposite, paint elsewhere."""
    block = PHASED[mat]
    drawn = _load(f"{block}_phase_{phase}")["elements"][0]["faces"][facing]
    faces = {}
    for f in ("north", "east", "south", "west", "up", "down"):
        if f == facing:
            faces[f] = {"texture": "#face", "uv": list(drawn["uv"]), "cullface": f}
        else:
            faces[f] = {"texture": "#inside" if f == OPP[facing] else "#side", "cullface": f}
    return {"parent": "minecraft:block/block", "ambientocclusion": True,
            "textures": {"particle": f"jugcraft:block/{block}", "face": f"jugcraft:block/{block}",
                         "inside": f"jugcraft:block/{INSIDE_TEXTURE[inside]}", "side": PAINT},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def line_model(mat, facing, phase):
    """The Neon Line's body with its groove on FACING alone, every other outside face painted (the groove's own faces
    and the face it is cut in stay the material's)."""
    import model_writer
    model = copy.deepcopy(_load(f"neon_line_{mat}_{BODY[facing]}_p{phase}"))
    model["textures"]["side"] = PAINT
    for e in model["elements"]:
        for f, face in e["faces"].items():
            if f != facing and face.get("cullface") and face["texture"] == "#base":
                face["texture"] = "#side"
                face["uv"] = model_writer.auto_uv(f, e["from"], e["to"])
    return model


def fitting(facing, left, right):
    """The Neon Line fitting (liner, tube, caps: neon_line_fit_<north east south west>) for a faced line: open on its
    face, a wall behind, and on each side a line running on (l) or a cap (w)."""
    sides = {facing: "o", OPP[facing]: "w", CW[facing]: "l" if left else "w", CCW[facing]: "l" if right else "w"}
    return "neon_line_fit_" + "".join(sides[f] for f in FACINGS)


def item_model(mat):
    """The faced line as an item: its body facing north with a capped tube."""
    model = line_model(mat, "north", 0)
    fit = _load(fitting("north", False, False))
    model["textures"].update({k: v for k, v in fit["textures"].items() if k != "particle"})
    model["elements"] += copy.deepcopy(fit["elements"])
    return model


def wall_blockstate(mat):
    return {"variants": {f"facing={f},inside={i},phase={k}": {"model": f"jugcraft:block/{wall_id(mat)}_{f}_{i}_p{k}"}
                         for f in FACINGS for i in INSIDE_TEXTURE for k in range(8)}}


def line_blockstate(mat):
    parts = [{"when": {"facing": f, "phase": str(k)}, "apply": {"model": f"jugcraft:block/{line_id(mat)}_{f}_p{k}"}}
             for f in FACINGS for k in range(8)]
    parts += [{"when": {"facing": f, "left": str(left).lower(), "right": str(right).lower()},
               "apply": {"model": f"jugcraft:block/{fitting(f, left, right)}"}}
              for f in FACINGS for left in (False, True) for right in (False, True)]
    return {"multipart": parts}


# ------------------------------------------------------------------------------------------------------ writing

def _json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def write_assets():
    for f in set(MODELS.glob("faced_wall_*.json")) | set(MODELS.glob("faced_line_*.json")):
        f.unlink()
    for mat in MATS:
        for facing in FACINGS:
            for k in range(8):
                for inside in INSIDE_TEXTURE:
                    _json(MODELS / f"{wall_id(mat)}_{facing}_{inside}_p{k}.json", wall_model(mat, facing, inside, k))
                _json(MODELS / f"{line_id(mat)}_{facing}_p{k}.json", line_model(mat, facing, k))
                assert (MODELS / f"neon_line_{mat}_{BODY[facing]}_p{k}.json").is_file()
            for left in (False, True):
                for right in (False, True):
                    assert (MODELS / f"{fitting(facing, left, right)}.json").is_file(), fitting(facing, left, right)
        _json(MODELS / f"{line_id(mat)}_item.json", item_model(mat))
        _json(ASSETS / f"blockstates/{wall_id(mat)}.json", wall_blockstate(mat))
        _json(ASSETS / f"blockstates/{line_id(mat)}.json", line_blockstate(mat))
        _json(ASSETS / f"items/{wall_id(mat)}.json",
              {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{wall_id(mat)}_north_painted_p0"}})
        _json(ASSETS / f"items/{line_id(mat)}.json",
              {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{line_id(mat)}_item"}})


def recipes():
    """{id: (pattern, key, count)}: a faced wall from a painted wall and a block of its material (two); the line from six
    of them, glowstone and orange dye, as a Neon Line is made from its material."""
    out = {}
    for mat in MATS:
        out[wall_id(mat)] = (["PM"], {"P": "jugcraft:painted_wall", "M": f"jugcraft:{PHASED[mat]}"}, 2)
        out[line_id(mat)] = (["MMM", "GDG", "MMM"], {"M": f"jugcraft:{wall_id(mat)}", "G": "minecraft:glowstone_dust",
                                                    "D": "minecraft:orange_dye"}, 6)
    return out


def lang():
    out = {}
    for mat in MATS:
        out[f"block.jugcraft.{wall_id(mat)}"] = f"{NAMES[mat]}-Faced Wall"
        out[f"block.jugcraft.{line_id(mat)}"] = f"Neon Line ({NAMES[mat]}-Faced Wall)"
    return out


def write_data():
    for i in ids():
        _json(DATA / "jugcraft/loot_table/blocks" / f"{i}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{i}"}],
            "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": f"jugcraft:blocks/{i}"})
    for i, (pattern, key, count) in recipes().items():
        _json(DATA / "jugcraft/recipe" / f"{i}.json", {
            "fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                       {"condition": "jugcraft:feature_enabled", "feature": "drones"}],
            "type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key,
            "result": {"id": f"jugcraft:{i}", "count": count}})
    import advanced_crafting              # made on the Advanced Workbench (the same cost, a shape bigger than 3 x 3)
    advanced_crafting.convert(DATA / "jugcraft/recipe", list(recipes()))
    # JEI's list of the Advanced Workbench's recipes (recipe_view.json "advanced_crafting": advanced_crafting.view)
    path = ASSETS / "recipe_view.json"
    text = path.read_text(encoding="utf-8")
    view = json.loads(text)
    view["advanced_crafting"] = advanced_crafting.view(DATA / "jugcraft/recipe")
    indent = 2 if text.startswith("{\n  ") else None
    path.write_text(json.dumps(view, indent=indent) + "\n" if indent else json.dumps(view, separators=(",", ":")) + "\n", encoding="utf-8")
    path = ASSETS / "lang/en_us.json"
    text = path.read_text(encoding="utf-8")
    names = json.loads(text)
    sorted_before = list(names) == sorted(names)
    names.update(lang())
    if sorted_before:
        names = dict(sorted(names.items()))
    # (written as it stands: its accented letters as themselves if that is how the file has them, so nothing else changes)
    plain = json.dumps(json.loads(text), indent=2, ensure_ascii=False) + "\n" == text
    path.write_text(json.dumps(names, indent=2, ensure_ascii=not plain) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in ids():
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write():
    write_assets()
    write_data()


# ------------------------------------------------------------------------------------------------- the layout

WALL_NAME = {m: f"jugcraft:{wall_id(m)}" for m in PHASED}
LINE_NAME = {m: f"jugcraft:{line_id(m)}" for m in PHASED}
FACED_LINE = "jugcraft:faced_line_"
NEON_LINE = "jugcraft:neon_line_"
# what a wall is to the hall's air: an opaque full cube (as the Neon Lines know them), a tiled or faced wall, glass, a door
OPAQUE = {f"jugcraft:{n}" for n in (
    "reinforced_concrete", "hazard_plating", "steel_armor_plate", "aluminum_cladding", "tungsten_steel_frame", "carbon_composite_panel",
    "silicon_carbide_armor", "depleted_uranium_armor", "acoustic_wall_panel", "access_floor_tile", "carpet_tile", "transformer_casing",
    "dock_plating", "epoxy_floor", "ceiling_tile", "painted_wall", "vinyl_floor", "restroom_floor_tile", "restroom_wall_tile",
    "restroom_wall_tile_cap", "wall_panel", "steel_girder")} | set(WALL_NAME.values())


def name(state):
    return (state or "").split("[")[0]


def props(state):
    if "[" not in state:
        return {}
    return dict(kv.split("=") for kv in state[state.index("[") + 1:-1].split(","))


def is_glass(state):
    return state.startswith("fglass:") or name(state).endswith("framed_glass")


def is_door(state):
    return "door" in name(state)


def is_wall(state):
    """Whether a block closes a cell to the hall's air: an opaque cube, a line in one, glass or a door."""
    n = name(state)
    return n in OPAQUE or n.startswith(NEON_LINE) or n.startswith(FACED_LINE) or is_glass(state) or is_door(state)


def hall_air(b, seeds, cells, y0, y1):
    """The hall's air: every cell reached from the seeds (x, y, z) through `cells` ((x, z)) at y0..y1 without passing a
    wall, glass or a door (so not the rooms; furniture, signs and lights are not in the way)."""
    seen = set()
    stack = [s for s in seeds]
    for s in stack:
        assert (s[0], s[2]) in cells and not is_wall(b.get(s) or ""), ("the seed stands in a wall", s, b.get(s))
    seen.update(stack)
    while stack:
        x, y, z = stack.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or not (y0 <= n[1] <= y1) or (n[0], n[2]) not in cells:
                continue
            s = b.get(n)
            if s is not None and is_wall(s):
                continue
            seen.add(n)
            stack.append(n)
    return seen


def face(b, air, mat, y0, y1, line_y=None, edge=()):
    """Every painted or tiled wall block (INSIDE_OF) at y0..y1 with a face on the hall's air, in place. One face on the
    hall and a room on another: a faced wall, that face in the material (a tiled one only if its tiles face the other
    way: else it is left). Otherwise (a corner or a pier with the hall on two sides, a wall with no room behind it): the
    material itself. A face counts as on the hall through a glass cell that stands on the hall too (the reveal beside a
    glass front; never the building's own curtain wall, `edge`). At line_y the faced line, or the material's Neon
    Line, where the hall's cell in front is empty. Returns how many of each."""
    count = {"faced": 0, "plain": 0, "lines": 0, "left": 0}
    plain = f"jugcraft:{PHASED[mat]}"
    out = {}
    for (x, y, z), s in b.items():
        inside = INSIDE_OF.get(name(s))
        if inside is None or not (y0 <= y <= y1):
            continue
        hall, through, room = [], [], False
        for f, (dx, dz) in DIRS.items():
            n = (x + dx, y, z + dz)
            t = b.get(n)
            if n in air:
                hall.append(f)
            elif t is not None and is_glass(t) and (n[0], n[2]) not in edge \
                    and any((n[0] + ex, y, n[2] + ez) in air for ex, ez in DIRS.values()):
                through.append(f)
            elif t is None or not (name(t) in OPAQUE or name(t).startswith(NEON_LINE) or name(t).startswith(FACED_LINE)):
                room = True
        seen = hall + through
        if not seen:
            continue
        lit = y == line_y and any(b.get((x + DIRS[f][0], y, z + DIRS[f][1])) is None for f in hall)
        if len(seen) == 1 and room:
            if inside != "painted" and props(s).get("facing") != OPP[seen[0]]:
                count["left"] += 1
                continue
            if lit and inside == "painted":
                out[(x, y, z)] = f"{LINE_NAME[mat]}[facing={seen[0]},left=false,right=false]"
                count["lines"] += 1
            else:
                out[(x, y, z)] = f"{WALL_NAME[mat]}[facing={seen[0]},inside={inside}]"
            count["faced"] += 1
        else:
            out[(x, y, z)] = f"{NEON_LINE}{mat}" if lit else plain
            count["lines" if lit else "plain"] += 1
    b.update(out)
    return count


def line(b, air, cells, y, mat):
    """A Neon Line through the material's own blocks in `cells` ((x, z)) at y, where a face is on the hall's air with the
    cell in front empty and the wall goes on a block higher (never a wall's top course). Returns how many."""
    plain = f"jugcraft:{PHASED[mat]}"
    n = 0
    for (x, z) in cells:
        if b.get((x, y, z)) != plain or b.get((x, y + 1, z)) != plain:
            continue
        if any((x + dx, y, z + dz) in air and b.get((x + dx, y, z + dz)) is None for dx, dz in DIRS.values()):
            b[(x, y, z)] = f"{NEON_LINE}{mat}"
            n += 1
    return n


def joins(state):
    """Whether a faced line's tube runs on into this neighbour: a line of either kind."""
    n = name(state)
    return n.startswith(NEON_LINE) or n.startswith(FACED_LINE)


def shape(b):
    """Every faced line's LEFT and RIGHT from the blocks beside it, in place (as tower/FacedLineBlock.shaped)."""
    out = {}
    for (x, y, z), s in b.items():
        n = name(s)
        if not n.startswith(FACED_LINE):
            continue
        f = props(s)["facing"]
        lx, lz = DIRS[CW[f]]
        rx, rz = DIRS[CCW[f]]
        left, right = joins(b.get((x + lx, y, z + lz))), joins(b.get((x + rx, y, z + rz)))
        out[(x, y, z)] = f"{n}[facing={f},left={str(left).lower()},right={str(right).lower()}]"
    b.update(out)
    return len(out)


if __name__ == "__main__":
    write()
    print("ok", len(ids()), "blocks:", ", ".join(ids()))
