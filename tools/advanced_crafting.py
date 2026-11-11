"""The Advanced Crafting Table's recipes (jugcraft:advanced_crafting, crafting/AdvancedCraftingRecipe): the 6 x 6 recipes
of the drones and the advanced kit, approved by Justin 26 October 2026 (recipe sheet take 1).

They replace these items' 3 x 3 crafting recipes. The generators that wrote those (generate_material_data.drone_recipes
for drones.DRONE_CRAFTING and tower.CRAFTING; holo_projector.py, big_map_table.py, holo_command_table.py,
tower_control_console.py) still keep their 3 x 3 tables (the drones' feature switches are worked out from them) but call
apply() after writing, which turns each of these files into its 6 x 6 recipe: the same file name, the switches it had
plus those of any new ingredient.

- The nine drones keep the parts they had, laid out bigger (the T1 adds a basic circuit and a second lead-acid pack).
- (The holo kit, made fairly expensive here at first, has had no recipe since 28 October 2026: the holograms are the
  tower's own, tower/Holograms. TOWER_ONLY; the core and consoles take the emitter's glass, glowstone and circuits.)
- The tower core (a fairly expensive mid-game item) and its consoles; the tower's exchanges (the same parts).

A pattern is at most 6 x 6 and always wider or taller than 3 (the recipe type refuses anything that fits a crafting
table); a space is an empty cell; it matches anywhere on the grid, and mirrored.

The rest of the tower's blocks (Justin, 28 October 2026: "start making recipes for the blocks for the drone tower ...
use the advanced crafting bench ... so that it doesn't interfere with any kind of vanilla recipes"; "Same cost, bigger
shape"): every crafting-table recipe that makes a block of the tower or of the drone depot (convert()) becomes an
Advanced Workbench recipe with the same ingredients in the same shape, each cell doubled (across, or down for a pattern
one wide), making twice as many: the same cost a block, so the tower's tier costs stay as they were. The generators keep
writing their 3 x 3s; convert() turns them (generate_material_data last of all, the tower's own tools after writing).

Run: python3 tools/advanced_crafting.py (rewrites the 21 recipe files and converts the tower's)
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECIPES_DIR = ROOT / "src/main/resources/data/jugcraft/recipe"

ST, AL, TI, BR, BS, IN, IR = "#c:plates/steel", "#c:plates/aluminum", "#c:plates/titanium", "#c:plates/bronze", "#c:plates/brass", "#c:plates/invar", "#c:plates/iron"
SB = "#c:storage_blocks/steel"


def J(n):
    return "jugcraft:" + n


def M(n):
    return "minecraft:" + n


# result: (pattern, key, count); "." in a pattern is an empty cell (written as a space)
RECIPES = {
    # the drones: the same parts as their 3 x 3s, each tier holding the one before
    "drone_t1": (["R....R", ".B..B.", "..FC..", "..PP..", ".B..B.", "R....R"],
                 {"R": J("rotor_assembly"), "B": BR, "F": J("flight_controller"), "C": J("basic_circuit"), "P": J("lead_acid_pack")}, 1),
    "drone_t2": (["R..R", "ZDCZ", ".IW."],
                 {"R": J("rotor_assembly"), "Z": BS, "D": J("drone_t1"), "C": J("basic_circuit"), "I": IN, "W": J("cargo_winch")}, 1),
    "drone_t3": (["R.A.R", ".CDL.", "R.A.R"],
                 {"R": J("aluminum_rotor_assembly"), "A": AL, "C": J("advanced_circuit"), "D": J("drone_t2"), "L": J("lithium_cell")}, 1),
    "drone_t4": (["F.L.F", ".CDU.", "F.L.F"],
                 {"F": J("ducted_fan"), "L": J("lithium_cell"), "C": J("advanced_circuit"), "D": J("drone_t3"), "U": J("speed_upgrade")}, 1),
    "drone_t5": (["N.A.N", ".CDP.", ".LWL."],
                 {"N": J("tilt_rotor_nacelle"), "A": AL, "C": J("processor"), "D": J("drone_t4"), "P": J("plastic_sheet"),
                  "L": J("lithium_cell"), "W": J("cargo_winch")}, 1),
    "drone_t6": ([".R.", "CTC", ".D.", "LWL", ".R."],
                 {"R": J("composite_rotor_assembly"), "C": J("processor"), "T": TI, "D": J("drone_t5"), "L": J("lithium_cell"),
                  "W": J("cargo_winch")}, 1),
    "drone_t7": ([".HHH.", "F.D.F", ".CLC."],
                 {"H": J("hydrogen_lift_cell"), "F": J("ducted_fan"), "D": J("drone_t6"), "C": J("processor"), "L": J("lithium_cell")}, 1),
    "drone_t8": (["E.E.E", ".FDF.", ".TCT."],
                 {"E": J("ion_emitter"), "F": J("ducted_fan"), "D": J("drone_t7"), "T": TI, "C": J("processor")}, 1),
    "drone_t9": ([".F.F.", "F.C.F", "..D..", "F.B.F"],
                 {"F": J("superconducting_lift_fan"), "C": J("processor"), "D": J("drone_t8"), "B": J("lithium_battery_bank")}, 1),
    # (the holo kit has no recipe since 28 October 2026: holograms, the tower's own; TOWER_ONLY below)
    # the tower core and its consoles
    # (the core and the consoles had a holo panel emitter in them: now its glass pane, glowstone and circuits, 28 Oct)
    "drone_tower_core": (["BSPSB", "AWMWA", "BSPSB", "LC.AL"],
                         {"B": SB, "S": ST, "P": J("processor"), "A": J("advanced_circuit"), "W": M("glass_pane"),
                          "M": J("drone_depot_terminal"), "L": M("glowstone_dust"), "C": J("basic_circuit")}, 1),
    "drone_depot_terminal": (["PGGP", "AMCA", "P..P"],
                             {"P": AL, "G": M("glass"), "A": J("advanced_circuit"), "M": J("machine_casing"), "C": J("copper_cable")}, 1),
    "tower_master_console": (["SDDS", "AWPA", "SLAS"],
                             {"S": ST, "D": J("console_desk"), "A": J("advanced_circuit"), "W": M("glass_pane"), "P": J("processor"),
                              "L": M("glowstone_dust")}, 1),
    "tower_control_console": (["GGGGG", "AWMWA", "SLPLS", ".C.A."],
                              {"G": J("blast_glass"), "A": J("advanced_circuit"), "W": M("glass_pane"), "M": J("tower_master_console"),
                               "P": J("processor"), "S": ST, "L": M("glowstone_dust"), "C": J("basic_circuit")}, 1),
    "tower_info_totem": (["LGA", "PWP", "PAP", "PPP"],
                         {"G": M("black_stained_glass_pane"), "P": IR, "W": M("glass_pane"), "L": M("glowstone_dust"),
                          "A": J("advanced_circuit")}, 1),
    # the tower's exchanges
    "cargo_exchange": (["SRPRS", "S.R.S", "..C.."],
                       {"S": ST, "R": J("intake_funnel"), "P": J("cargo_exchange_port"), "C": J("advanced_circuit")}, 1),
    "energy_exchange": (["STPTS", "S.T.S", "..C.."],
                        {"S": ST, "T": J("transformer_casing"), "P": J("energy_exchange_port"), "C": J("advanced_circuit")}, 1),
}


def check():
    """Every pattern well formed (rows of one length, at most 6 x 6, bigger than 3 x 3, every symbol keyed, every key
    used) and no two recipes the same grid of ingredients, either way round."""
    seen = {}
    for name, (pattern, key, count) in RECIPES.items():
        w, h = len(pattern[0]), len(pattern)
        assert all(len(r) == w for r in pattern), f"advanced_crafting {name}: rows of different lengths"
        assert w <= 6 and h <= 6 and (w > 3 or h > 3), f"advanced_crafting {name}: {w} x {h}"
        used = {c for r in pattern for c in r if c != "."}
        assert used == set(key), f"advanced_crafting {name}: pattern {sorted(used)} != key {sorted(key)}"
        for p in (pattern, [r[::-1] for r in pattern]):
            grid = tuple(tuple(key.get(c) for c in r) for r in p)
            assert seen.get(grid, name) == name, f"advanced_crafting: {name} and {seen.get(grid)} are the same recipe"
            seen[grid] = name


def _item_features(item):
    """The feature switches an ingredient item is registered under (drones.ITEM_FEATURES, tower.ITEM_FEATURES), as the
    generator gates the drones' and the tower's recipes: a recipe naming an item that is not registered fails to load."""
    if not item.startswith("jugcraft:"):
        return []
    import contextlib
    import io
    import drones
    with contextlib.redirect_stdout(io.StringIO()):
        import tower
    name = item.split(":")[1]
    return list(drones.ITEM_FEATURES.get(name, [])) + list(tower.ITEM_FEATURES.get(name, []))


def recipe(name, conditions):
    pattern, key, count = RECIPES[name]
    return {"fabric:load_conditions": conditions, "type": "jugcraft:advanced_crafting",
            "pattern": [r.replace(".", " ") for r in pattern], "key": dict(key),
            "result": {"id": f"jugcraft:{name}", "count": count}}


# The holograms: no recipe at all (tower/Holograms; Justin, 28 October 2026: "make sure people can't craft any of the
# holograms. I want those to be exclusive to the tower"); apply() and convert() delete any recipe that makes one.
TOWER_ONLY = {"holo_table", "holo_command_table", "holo_projector", "big_map_table", "holo_emitter"}
# Their blocks, with the parts (tower/Holograms.BLOCKS): no wither or dragon breaks one (generate_material_data's tags).
HOLOGRAM_BLOCKS = sorted(TOWER_ONLY | {"holo_projector_part", "big_map_table_part"})
# Not converted: the Advanced Workbench itself (made on a crafting table, its own 3 x 3: Justin, "Keep it"), and a block a
# drone-gated recipe makes that isn't the tower's (a storage tank kit takes a drone part).
CONVERT_EXCLUDE = {"advanced_crafting_table", "tank_kit_cryogenic"}
CRAFTING = ("minecraft:crafting_shaped", "minecraft:crafting_shapeless")


def _tower_blocks():
    import contextlib
    import io
    import drones
    with contextlib.redirect_stdout(io.StringIO()):
        import tower
    return set(tower.tower_blocks()) | set(drones.drone_blocks())


def _target(d, blockstates, tower_blocks):
    """The block a crafting recipe makes, if it is one of the tower's (or the depot's) to convert; else None: the tower's
    lists, and any other block whose recipe is switched on by the drones feature (the tower's later pieces)."""
    res = d.get("result")
    rid = res.get("id") if isinstance(res, dict) else None
    if d.get("type") not in CRAFTING or not isinstance(rid, str) or not rid.startswith("jugcraft:"):
        return None
    name = rid.split(":")[1]
    if name in CONVERT_EXCLUDE or name in RECIPES:
        return None
    features = {c.get("feature") for c in d.get("fabric:load_conditions") or [] if isinstance(c, dict)}
    if name in tower_blocks or ("drones" in features and (blockstates / f"{name}.json").is_file()):
        return name
    return None


def _trim(rows):
    width = max(len(r) for r in rows)
    rows = [r.ljust(width) for r in rows]
    rows = [r for r in rows if r.strip()]
    cols = [i for i in range(width) if any(r[i] != " " for r in rows)]
    return [r[min(cols):max(cols) + 1] for r in rows]


def widen(d):
    """A crafting-table recipe as the Advanced Workbench's: its shape (a shapeless one laid out in rows of three) with every
    cell doubled across (or, one wide, down), so it is bigger than 3 x 3; twice the ingredients, twice the result."""
    if d["type"] == "minecraft:crafting_shapeless":
        symbols, key, cells = "ABCDEFGHI", {}, []
        for ing in d["ingredients"]:
            found = next((c for c, v in key.items() if v == ing), None)
            if found is None:
                found = symbols[len(key)]
                key[found] = ing
            cells.append(found)
        rows = ["".join(cells[i:i + 3]) for i in range(0, len(cells), 3)]
        rows = [r.ljust(len(rows[0])) for r in rows]
    else:
        rows, key = _trim(d["pattern"]), dict(d["key"])
    w, h = len(rows[0]), len(rows)
    sx, sy = (2, 1) if 2 <= w <= 3 else (1, 2) if 2 <= h <= 3 else (4, 1)
    assert w * sx <= 6 and h * sy <= 6 and (w * sx > 3 or h * sy > 3), f"advanced_crafting: can't widen {rows}"
    pattern = ["".join(c * sx for c in r) for r in rows for _ in range(sy)]
    result = dict(d["result"])
    result["count"] = result.get("count", 1) * sx * sy
    assert result["count"] <= 64, f"advanced_crafting: {result['id']} would make {result['count']}"
    return {"fabric:load_conditions": d.get("fabric:load_conditions") or [], "type": "jugcraft:advanced_crafting",
            "pattern": pattern, "key": {c: key[c] for c in sorted(key) if any(c in r for r in pattern)}, "result": result}


def _grid(d):
    def ing(v):
        if isinstance(v, list):
            return tuple(sorted(json.dumps(x, sort_keys=True) for x in v))
        return json.dumps(v, sort_keys=True)
    rows = _trim(d["pattern"])
    grid = tuple(tuple(ing(d["key"][c]) if c != " " else None for c in r) for r in rows)
    return min(str(grid), str(tuple(tuple(reversed(r)) for r in grid)))


def check_all(out_dir=RECIPES_DIR):
    """No two Advanced Workbench recipes the same grid of ingredients, either way round (only one could ever be made)."""
    seen = {}
    for f in sorted(Path(out_dir).glob("**/*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        if d.get("type") == "jugcraft:advanced_crafting":
            g = _grid(d)
            assert g not in seen, f"advanced_crafting: {f.name} and {seen[g]} are the same recipe"
            seen[g] = f.name
    return len(seen)


def remove_tower_only(out_dir=RECIPES_DIR, names=None):
    """Deletes every recipe in {@code out_dir} that makes a hologram (TOWER_ONLY; or only those of {@code names}); returns
    the files' paths under it."""
    gone = []
    for f in sorted(Path(out_dir).glob("**/*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        res = d.get("result")
        rid = res.get("id") if isinstance(res, dict) else res
        name = rid.split(":")[1] if isinstance(rid, str) and rid.startswith("jugcraft:") else None
        if name in TOWER_ONLY and (names is None or name in names):
            f.unlink()
            gone.append(f.relative_to(out_dir).as_posix())
    return gone


def convert(out_dir=RECIPES_DIR, names=None):
    """Turns the crafting-table recipes of the tower's blocks in {@code out_dir} (all, or those making {@code names}) into
    Advanced Workbench recipes (widen); the ones already turned are left as they are."""
    out_dir = Path(out_dir)
    remove_tower_only(out_dir, names)
    blockstates = out_dir.parents[2] / "assets" / "jugcraft" / "blockstates"
    tower_blocks = _tower_blocks()
    done = []
    for f in sorted(out_dir.glob("**/*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        name = _target(d, blockstates, tower_blocks)
        if name is None or (names is not None and name not in names):
            continue
        f.write_text(json.dumps(widen(d), indent=2) + "\n", encoding="utf-8")
        done.append(f.stem)
    check_all(out_dir)
    return done


def apply(out_dir=RECIPES_DIR, names=None):
    """Writes these items' 6 x 6 recipes over their 3 x 3s in {@code out_dir}: the switches each file had, plus those a
    new ingredient is registered under (a processor's silicon...)."""
    check()
    out_dir = Path(out_dir)
    remove_tower_only(out_dir, names)
    wanted = [n for n in RECIPES if names is None or n in names]
    for name in wanted:
        f = out_dir / f"{name}.json"
        old = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {}
        conditions = list(old.get("fabric:load_conditions") or [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                                                 {"condition": "jugcraft:feature_enabled", "feature": "drones"}])
        for ingredient in RECIPES[name][1].values():
            for feature in _item_features(ingredient):
                c = {"condition": "jugcraft:feature_enabled", "feature": feature}
                if c not in conditions:
                    conditions.append(c)
        f.write_text(json.dumps(recipe(name, conditions), indent=2) + "\n", encoding="utf-8")
    convert(out_dir, names)
    return wanted


def view(out_dir=RECIPES_DIR):
    """Every Advanced Workbench recipe for JEI (recipe_view.json "advanced_crafting", client/compat/JugcraftJeiAdvanced):
    its pattern (a space an empty cell), its key (an item, a "#tag" or a list of either), and what it makes."""
    rows = []
    for f in sorted(Path(out_dir).glob("**/*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        if d.get("type") == "jugcraft:advanced_crafting":
            rows.append({"id": f"jugcraft:{f.relative_to(out_dir).with_suffix('').as_posix()}", "pattern": d["pattern"],
                         "key": d["key"], "out": [d["result"]["id"], d["result"].get("count", 1)]})
    return sorted(rows, key=lambda r: (r["out"][0], r["id"]))


if __name__ == "__main__":
    converted = convert()
    print(len(apply()), "advanced kit recipes written;", len(converted), "tower recipes converted;", check_all(), "Advanced Workbench recipes in all")
