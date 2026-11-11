"""The Tri-Pillar's neon light lines (work list item 11, approved by Justin on 23 October 2026 at the second take: "more
of a neon look", "blend in with the surrounding blocks' colour schemes, even if you have to create multiple types", "add
in some neon white thin ones that aren't end rods"), made real 3D neon after his walk through (24 October: the strips'
colour didn't blend with the floor and walls round them, the display case's didn't join, "make the lights be 3D modeled
... neon light"; redesign 2, tools/neon_3d.py has the models). They replace the tower's amber light boxes and end rods:

  * neon_line_<material> (concrete, steel, carbon): the wall's or slab edge's own block (its texture at its phase, as
    its neighbours draw it) with a lined groove round each side open to the air and a round glass neon tube in it,
    bending round a corner, joining a chamfer's next block, running on beside another line, capped at a run's end;
  * neon_floor_<material> (concrete, steel, epoxy, dock): the floor's own block with the same profile as a channel in its
    top, joined to the floor lines beside it;
  * inset_light_<material> (concrete, steel, epoxy, dock): a round lens in a square steel plate flush in the floor, where
    the layout had a floor line on its own;
  * neon_tube: a thin neon white tube on a dark clip rail in a wall's corner (facing: away from the wall; side: the
    corner of the cell it stands in), with a soft halo: the pillars' corner lines, in the end rods' place.

The tower's layout converts its amber boxes and end rods into these (tools/tower_tripillar.py build: convert, after each
step's lights): a box with open air over it (or in a floor at or under the hall's) becomes a floor line in the floor's
material; any other a line in the material round it (steel before carbon before concrete); a rod a tube in the corner
beside it, toward where its wall ends. Then every line takes its sides and joins and every floor line its joins from
its neighbours (as the blocks work them out in game: tower/NeonLineBlock.shaped, NeonFloorBlock.shaped), and a floor
line with none becomes an inset light.

    python3 tools/neon_lines.py     writes the textures, models, blockstates, items, loot, recipes, names and tag
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

import neon_3d as N3

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
TEX = ASSETS / "textures/block"
# the materials: block (the texture and phase models), TowerPanelBlock-phased or a plain cube
PHASED = {"concrete": "reinforced_concrete", "steel": "steel_armor_plate", "carbon": "carbon_composite_panel"}
FLAT = {"epoxy": "epoxy_floor", "dock": "dock_plating"}
LINE_MATS = ("concrete", "steel", "carbon")
FLOOR_MATS = ("concrete", "steel", "epoxy", "dock")
NAMES = {"concrete": "Concrete", "steel": "Steel", "carbon": "Carbon Composite", "epoxy": "Epoxy", "dock": "Dock Plating"}
LINE_LIGHT, TUBE_LIGHT = 12, 14



def line_ids():
    return [f"neon_line_{m}" for m in LINE_MATS]


def floor_ids():
    return [f"neon_floor_{m}" for m in FLOOR_MATS]


def inset_ids():
    return [f"inset_light_{m}" for m in FLOOR_MATS]


def ids():
    return line_ids() + floor_ids() + ["neon_tube"] + inset_ids()


# ---------------------------------------------------------------------------------------------------- textures

def white_textures():
    core = np.zeros((16, 16, 4))
    for x in range(16):
        t = 1 - abs(x - 7.5) / 8
        core[:, x, :3] = np.array((214, 228, 255)) * (1 - t) + np.array((255, 255, 255)) * t
    core[..., 3] = 255
    halo = np.zeros((32, 32, 4))
    for x in range(32):
        t = max(0.0, 1 - abs(x - 15.5) / 16)
        halo[:, x, :3] = (225, 236, 255)
        halo[:, x, 3] = 70 * t ** 2
    rail = np.zeros((16, 16, 4)) + (34, 36, 40, 255)
    return {"neon_white": core, "neon_white_halo": halo, "neon_rail": rail}


# ------------------------------------------------------------------------------------------------------ models



# the tube facing north (its wall at z 16), in the cell's west corner: its clip rail, the core, the halo round it
TUBE_RAIL = ((0, 0, 14.6), (1.4, 16, 16))
TUBE_CORE = ((1.0, 0, 13.6), (2.0, 16, 14.6))
TUBE_HALO = ((0.1, 0, 12.7), (2.9, 16, 15.5))


def tube_model(right):
    def mx(box):
        (x0, y0, z0), (x1, y1, z1) = box
        return ((16 - x1, y0, z0), (16 - x0, y1, z1)) if right else box
    rail, core, halo = mx(TUBE_RAIL), mx(TUBE_CORE), mx(TUBE_HALO)
    els = [{"from": list(rail[0]), "to": list(rail[1]), "faces": {
        **{f: {"texture": "#rail"} for f in ("north", "east", "west")},
        "up": {"texture": "#rail", "cullface": "up"}, "down": {"texture": "#rail", "cullface": "down"}}},
        {"from": list(core[0]), "to": list(core[1]), "light_emission": 15, "shade": False, "faces": {
            f: {"texture": "#core", **({"cullface": f} if f in ("up", "down") else {})}
            for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": list(halo[0]), "to": list(halo[1]), "light_emission": 15, "shade": False, "faces": {
            f: {"texture": "#halo", "uv": [0, 0, 16, 16]} for f in ("north", "south", "east", "west")}}]
    return {"ambientocclusion": False, "textures": {
        "particle": "jugcraft:block/neon_rail", "rail": "jugcraft:block/neon_rail", "core": "jugcraft:block/neon_white",
        "halo": {"sprite": "jugcraft:block/neon_white_halo", "force_translucent": True}}, "elements": els}


FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


# ------------------------------------------------------------------------------------------------------ writing

def _json(path, obj):
    """As tools/generate_material_data.py write: a model's elements finished (tools/model_writer.py)."""
    if isinstance(obj, dict) and obj.get("elements"):
        import model_writer
        model_writer.finish_elements(obj["elements"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def write_assets():
    # the old painted slots (layouts 21-27): their textures and models out
    for mat in sorted(set(LINE_MATS) | set(FLOOR_MATS)):
        (TEX / f"neon_{mat}.png").unlink(missing_ok=True)
    models = ASSETS / "models/block"
    for f in set(models.glob("neon_floor_*.json")) | set(models.glob("neon_line_*.json")) | set(models.glob("inset_light_*.json")):
        f.unlink(missing_ok=True)
    for name, arr in white_textures().items():
        Image.fromarray(arr.astype(np.uint8)).save(TEX / f"{name}.png", optimize=True)
    N3.write(_json)
    for side in ("left", "right"):
        _json(models / f"neon_tube_{side}.json", tube_model(side == "right"))
    _json(ASSETS / "blockstates/neon_tube.json", {"variants": {
        f"facing={f},side={s}": {"model": f"jugcraft:block/neon_tube_{s}", **({"y": y} if y else {})}
        for f, y in FACING_Y.items() for s in ("left", "right")}})
    _json(ASSETS / "models/item/neon_tube.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jugcraft:item/neon_tube"}})
    _json(ASSETS / "items/neon_tube.json", {"model": {"type": "minecraft:model", "model": "jugcraft:item/neon_tube"}})
    import item_icons
    item_icons.draw("neon_tube").save(ASSETS / "textures/item/neon_tube.png")


def recipes():
    """{id: (pattern, key, count)}: a line from six of its material, glowstone and orange dye; a floor line the same in
    another shape; the tube from glass panes and glowstone."""
    out = {}
    for mat in LINE_MATS:
        out[f"neon_line_{mat}"] = (["MMM", "GDG", "MMM"], {"M": f"jugcraft:{PHASED[mat]}", "G": "minecraft:glowstone_dust",
                                                           "D": "minecraft:orange_dye"}, 6)
    for mat in FLOOR_MATS:
        out[f"neon_floor_{mat}"] = (["MGM", "MDM", "MGM"], {"M": f"jugcraft:{PHASED.get(mat) or FLAT[mat]}",
                                                           "G": "minecraft:glowstone_dust", "D": "minecraft:orange_dye"}, 6)
    out["neon_tube"] = (["P", "G", "P"], {"P": "minecraft:glass_pane", "G": "minecraft:glowstone_dust"}, 6)
    for mat in FLOOR_MATS:
        out[f"inset_light_{mat}"] = (["MPM", "MGM", "MMM"], {"M": f"jugcraft:{PHASED.get(mat) or FLAT[mat]}", "P": "minecraft:glass_pane",
                                                            "G": "minecraft:glowstone_dust"}, 4)
    return out


def lang():
    out = {f"block.jugcraft.neon_line_{m}": f"Neon Line ({NAMES[m]})" for m in LINE_MATS}
    out.update({f"block.jugcraft.neon_floor_{m}": f"Neon Floor Line ({NAMES[m]})" for m in FLOOR_MATS})
    out["block.jugcraft.neon_tube"] = "Neon Tube"
    out.update({f"block.jugcraft.inset_light_{m}": f"Inset Floor Light ({NAMES[m]})" for m in FLOOR_MATS})
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
    path = ASSETS / "lang/en_us.json"
    names = json.loads(path.read_text(encoding="utf-8"))
    sorted_before = list(names) == sorted(names)
    names.update(lang())
    if sorted_before:
        names = dict(sorted(names.items()))
    path.write_text(json.dumps(names, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in ids():
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


# --------------------------------------------------------------------------------------------- the layout

AMBER_BOX = "jugcraft:amber_light_strip"
END_ROD = "minecraft:end_rod"
MATERIAL_OF = {"jugcraft:reinforced_concrete": "concrete", "jugcraft:steel_armor_plate": "steel",
               "jugcraft:carbon_composite_panel": "carbon", "jugcraft:ceiling_tile": "steel", "jugcraft:epoxy_floor": "epoxy",
               "jugcraft:dock_plating": "dock", "jugcraft:access_floor_tile": "steel", "jugcraft:hazard_plating": "steel"}
LINE_ORDER = ("steel", "carbon", "concrete")
FLOOR_ORDER = ("epoxy", "concrete", "steel", "dock")
DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
LEFT = {"north": "west", "west": "south", "south": "east", "east": "north"}
# what a wall behind a tube may be: anything solid (not glass, a rod, a ladder or a tube, nor a girder or the cradles'
# struts standing off a wall: layout 22)
NOT_WALL = ("glass", "end_rod", "tower_ladder", "neon_tube", "fglass", "steel_girder", "cradle_struts")


def _name(s):
    return (s or "").split("[")[0]


def _wall(s):
    return s is not None and not any(w in s for w in NOT_WALL)


def convert(b):
    """The amber boxes and end rods of a layout ({(x, y, z): material}) turned into the neon lines, in place; returns how
    many of each kind."""
    count = {"line": 0, "floor": 0, "tube": 0}
    for (x, y, z), s in list(b.items()):
        n = _name(s)
        if n == AMBER_BOX:
            floor = y <= 0 or (y not in (3, 10) and (x, y + 1, z) not in b)
            votes = {}
            near = [(x + dx, y, z + dz) for dx, dz in DIRS.values()] + ([] if floor else [(x, y - 1, z), (x, y + 1, z)])
            for p in near:
                m = MATERIAL_OF.get(_name(b.get(p)))
                if m:
                    votes[m] = votes.get(m, 0) + 1
            if floor:
                cand = [m for m in FLOOR_ORDER if m in votes] or ["concrete"]
                mat = max(cand, key=lambda m: (votes.get(m, 0), -FLOOR_ORDER.index(m)))
                b[(x, y, z)] = f"jugcraft:neon_floor_{mat}"
                count["floor"] += 1
            else:
                cand = [m for m in LINE_ORDER if m in votes] or ["steel"]
                mat = max(cand, key=lambda m: (votes.get(m, 0), -LINE_ORDER.index(m)))
                b[(x, y, z)] = f"jugcraft:neon_line_{mat}"
                count["line"] += 1
        elif n == END_ROD:
            wall = next((f for f, (dx, dz) in DIRS.items() if _wall(b.get((x + dx, y, z + dz)))), None)
            if wall is None:
                del b[(x, y, z)]
                continue
            facing = OPP[wall]
            lx, lz = DIRS[LEFT[facing]]
            wx, wz = DIRS[wall]
            left_open = not _wall(b.get((x + lx + wx, y, z + lz + wz)))
            b[(x, y, z)] = f"jugcraft:neon_tube[facing={facing},side={'left' if left_open else 'right'}]"
            count["tube"] += 1
    count["inset"] = shape(b)
    return count


# The blocks a line's side meets as a wall (BlockState.isSolidRender in game: opaque full cubes; tower/NeonLineBlock);
# anything else (air, glass, a door, a tube, a slab, furniture) leaves the side open. Checked against the game by
# bootcheck's NeonCheck.
SOLID = {f"jugcraft:{n}" for n in (
    "reinforced_concrete", "hazard_plating", "steel_armor_plate", "aluminum_cladding", "tungsten_steel_frame", "carbon_composite_panel",
    "silicon_carbide_armor", "depleted_uranium_armor", "acoustic_wall_panel", "access_floor_tile", "carpet_tile", "transformer_casing",
    "dock_plating", "epoxy_floor", "ceiling_tile", "painted_wall", "vinyl_floor", "restroom_floor_tile")}
# (a faced wall, tools/faced_walls.py, is one more opaque cube; its line, a faced line, is a line to the lines beside it)
SOLID |= {f"jugcraft:faced_wall_{m}" for m in LINE_MATS}
LINE = "jugcraft:neon_line_"
FACED_LINE = "jugcraft:faced_line_"
FLOOR = "jugcraft:neon_floor_"
D4 = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
DIAG = {"ne": ((1, -1), ("north", "east")), "se": ((1, 1), ("east", "south")), "sw": ((-1, 1), ("south", "west")),
        "nw": ((-1, -1), ("west", "north"))}


def side_of(s):
    """(Framed glass is a wall to a line, as tower/NeonLineBlock.sideOf has it: the line stops at a pane with a cap and
    never turns onto the reveal beside one. "fglass:" is the tower's generator's glass before it is resolved.)"""
    n = _name(s)
    if n.startswith(LINE) or n.startswith(FACED_LINE):
        return "line"
    return "wall" if n in SOLID or n == "jugcraft:framed_glass" or n.startswith("fglass:") else "open"


def line_state(b, x, y, z):
    """A wall line's sides ({side: open / line / wall}) and joins (corners whose two sides are open, another line in the
    cell diagonally beyond), as tower/NeonLineBlock.shaped works them out."""
    sides = {k: side_of(b.get((x + dx, y, z + dz))) for k, (dx, dz) in D4.items()}
    joins = {c for c, ((dx, dz), pair) in DIAG.items()
             if all(sides[s] == "open" for s in pair) and _name(b.get((x + dx, y, z + dz))).startswith(LINE)}
    return sides, joins


def shape(b):
    """Every line's sides and joins and every floor line's joins from its neighbours, in place; a floor line with none
    becomes an inset light; every faced line's ends (tools/faced_walls.py). Returns how many inset lights."""
    out, inset = {}, 0
    for (x, y, z), s in b.items():
        n = _name(s)
        if n.startswith(LINE):
            sides, joins = line_state(b, x, y, z)
            props = {**sides, **{c: "true" if c in joins else "false" for c in DIAG}}
            out[(x, y, z)] = n + "[" + ",".join(f"{k}={props[k]}" for k in sorted(props)) + "]"
        elif n.startswith(FLOOR):
            conn = {k: _name(b.get((x + dx, y, z + dz))).startswith(FLOOR) for k, (dx, dz) in D4.items()}
            if not any(conn.values()):
                out[(x, y, z)] = "jugcraft:inset_light_" + n[len(FLOOR):]
                inset += 1
            else:
                out[(x, y, z)] = n + "[" + ",".join(f"{k}={'true' if conn[k] else 'false'}" for k in sorted(conn)) + "]"
    b.update(out)
    import faced_walls              # (the faced lines' ends, from the same neighbours)
    faced_walls.shape(b)
    return inset


if __name__ == "__main__":
    write_assets()
    write_data()
    print("ok", len(ids()), "blocks")
