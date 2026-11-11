"""The upper floor's room kits (work list item 22 part B) as real blocks.

Writes every kit's models, blockstates and textures into the mod's assets, and the manifest the game registers the
blocks from (src/main/resources/jugcraft/room_kit.json, read by tower/RoomKit.java): each block's properties, the cells
one item places (as the piece faces north, its anchor first, each with its part and half) and every state's hitbox
(from its models, merged into a few boxes). Also each block's item model (the whole piece, scaled to fit a block),
its loot table, its recipe (tools/room_kit_recipes.py), the mineable tag and its English name.

The full generator runs it too (generate_material_data.assets, last before the names are written); the tower lists the
pieces among its blocks (tower.tower_blocks) and their recipes among its own (tower.CRAFTING), so a full run writes the
same files. The desks' page pictures on the laptops' and monitors' screens are tools/data/desk_pages (JUG_PAGES gives
another folder).

Run: python3 tools/room_kit.py
"""
from __future__ import annotations

import importlib
import itertools
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import model_writer as MW  # noqa: E402
from room_kit_recipes import CRAFTING, LIGHT, PIECES  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data/jugcraft"
MANIFEST = ROOT / "src/main/resources/jugcraft/room_kit.json"
# the console desks' page pictures (<role>_<page>.png) for the screens, as the mockups had them
PAGES = os.environ.get("JUG_PAGES") or str(ROOT / "tools/data/desk_pages")

# the room, the kit that makes its pieces
KITS = [("briefing", "training_kit"), ("manager", "manager_kit"), ("lab", "lab_kit"), ("first_aid", "rest_kit"),
        ("planning", "planning_kit"), ("offices", "office_kit"), ("lounge", "lounge_kit"), ("simulators", "sim_kit"),
        ("training", "class_kit"), ("meeting", "meeting_kit"), ("restrooms", "wash_kit"), ("briefing", "room_extra_kit"),
        ("roof", "roof_plant"), ("workshop", "adv_crafting_table"), ("dispatch", "goods_lift"),
        ("hall", "diorama_case")]
# pieces laid out over a grid of cells (along x, then along z): (nx, nz); the rest run along x (part) and up (half)
GRIDS = {"boardroom_table": (2, 5), "office_bench": (3, 2), "lounge_rug": (4, 3), "pendant_light": (1, 5), "exam_couch": (1, 2)}
# the wall screens: (w, h); their parts run to the viewer's right (-x as the screen faces north) and down
WALL = {"ops_wall_screen": (3, 2), "ops_wall_screen_large": (5, 3)}
# pieces drawn live over their models (client/RoomKitDisplayRenderer), and how
DISPLAY = {"ops_wall_screen": "wall_screen", "ops_wall_screen_large": "wall_screen"}
WOOL = ("sofa", "recliner", "bunk", "rug", "couch", "chair", "booth", "beanbag", "ottoman")
WOOD = ("table", "desk", "shelf", "credenza", "bookcase", "counter", "cupboard", "cabinet_wood", "board", "dresser", "bar",
        "lectern", "plinth", "stand", "logo_wall", "rack_wood")
STONE = ("vanity_counter", "mop_sink")
# where the name says otherwise: the whiteboards' aluminium, the wire shelving, the steel wheelchair and stands
MATERIAL = {"supply_shelf": "metal", "rolling_whiteboard": "metal", "class_whiteboard": "metal", "wall_whiteboard": "metal",
            "wheelchair": "metal", "spine_board": "metal", "strip_board": "metal", "thrust_test_stand": "metal",
            "office_bench_single": "wood", "advanced_crafting_table": "metal", "crate_pallet": "wood",
            "tower_diorama_case": "stone"}
NAMES = {
    "av_credenza": "AV Credenza", "wx_kiosk": "Weather Kiosk", "mfp_copier": "Multifunction Copier", "iv_pole": "IV Pole",
    "vc_camera": "Video Call Camera", "notam_board": "NOTAM Board", "cad_desk": "CAD Desk", "tv_wall": "TV Wall",
    "rooftop_ahu": "Rooftop Air Handler", "rf_cabinet": "RF Cabinet", "comms_dish": "Microwave Dish",
    "comms_mast_tie": "Comms Mast Wall Tie", "advanced_crafting_table": "Advanced Workbench",
    "lift_rail": "Lift Guide Rail", "crate_pallet": "Pallet of Drone Crates", "cargo_case_packer": "Case Packer", "cargo_infeed": "Packer Infeed Conveyor",
    "tower_diorama_case": "Tower Diorama Case",
}


def title(name):
    return NAMES.get(name) or " ".join(w.upper() if w in ("tv", "pc", "ups", "ac") else w.capitalize() for w in name.split("_"))


# ------------------------------------------------------------------ the kits' outputs
KIT_ATLASES = {}                 # kit -> {its atlases' keys: the atlas} (their pictures' live parts)
# what a kit may give besides its blockstates (tools/roof_plant.py): its multi-block pieces' cells (part n is cell n,
# as the piece faces north), the rotors a renderer turns (client/RoomKitRotorRenderer) and its pieces' displays
KIT_CELLS, KIT_ROTORS = {}, {}


def write_kits():
    """Each kit's write() into the mod's assets; returns {block: (room, kit)} for the blockstates each one wrote."""
    sys.path.insert(0, str(ROOT / "tools"))
    import training_kit as TK
    out = {}
    bs_dir = ASSETS / "blockstates"
    for room, kit in KITS:
        before = {p: p.stat().st_mtime_ns for p in bs_dir.glob("*.json")}
        made = len(TK.ATLASES)
        m = importlib.import_module(kit)
        try:
            m.write(ASSETS, "jugcraft", PAGES)
        except TypeError:
            m.write(ASSETS, "jugcraft")
        KIT_ATLASES[kit] = {a.key: a for a in TK.ATLASES[made:]}
        KIT_CELLS.update(getattr(m, "CELLS", {}))
        KIT_ROTORS.update(getattr(m, "ROTORS", {}))
        DISPLAY.update(getattr(m, "DISPLAY", {}))
        for p in bs_dir.glob("*.json"):
            if before.get(p) != p.stat().st_mtime_ns and p.stem not in out:
                out[p.stem] = (room, kit)
    return out


# ------------------------------------------------------------------ live faces (B2)
# A face's corners as seen from outside it (top-left, top-right, bottom-right, bottom-left), from its box (pixels)
def _face_corners(face, f, t):
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {"north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
            "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
            "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
            "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
            "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]}[face]


def _rotate(p, rot):
    """A point turned by a model element's rotation (right-handed about its axis, through its origin)."""
    if not rot or not rot.get("angle"):
        return p
    a = math.radians(rot["angle"])
    c, s = math.cos(a), math.sin(a)
    o = rot.get("origin", [8, 8, 8])
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    if rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def _turn_y(p, y):
    """A point of a model turned as a blockstate's y turns it (clockwise from above)."""
    x, yy, z = p
    for _ in range((y // 90) % 4):
        x, z = 16 - z, x
    return (x, yy, z)


def _model_live(model, atlases, cache={}):
    """The live faces of a model: each face showing a picture with a live part (its atlas's live()), the live part's
    corners on the face (top-left, top-right, bottom-left; pixels, the model unturned) and what it is."""
    key = (model, id(atlases))
    if key in cache:
        return cache[key]
    out = []
    ns, path = model.split(":")
    f = ROOT / f"src/main/resources/assets/{ns}/models/{path}.json"
    m = json.loads(f.read_text()) if f.exists() else {}
    import training_kit as TK
    textures = m.get("textures", {})
    for e in m.get("elements", []):
        for face, spec in e.get("faces", {}).items():
            var = spec.get("texture", "").lstrip("#")
            atlas = atlases.get(var)
            whole = TK.TEXTURE_LIVE.get(textures.get(var, ""), [])
            if (atlas is None and not whole) or "uv" not in spec:
                continue
            fu0, fv0, fu1, fv1 = spec["uv"]
            lo_u, hi_u, lo_v, hi_v = min(fu0, fu1), max(fu0, fu1), min(fv0, fv1), max(fv0, fv1)
            # an atlas's pictures' live parts (by the region each is in), or a whole texture's (the texture its region)
            parts = [(atlas.regions[k], i) for k, infos in atlas.live.items() for i in infos] if atlas is not None else []
            parts += [((0, 0, 16, 16), i) for i in whole]
            for (ru0, rv0, ru1, rv1), info in parts:
                if not (ru0 - 1e-4 <= lo_u and hi_u <= ru1 + 1e-4 and rv0 - 1e-4 <= lo_v and hi_v <= rv1 + 1e-4):
                    continue
                lx0, ly0, lx1, ly1 = info["rect"]
                lu0, lu1 = ru0 + lx0 * (ru1 - ru0), ru0 + lx1 * (ru1 - ru0)
                lv0, lv1 = rv0 + ly0 * (rv1 - rv0), rv0 + ly1 * (rv1 - rv0)
                # A piece cut across its blocks shows a picture on several faces: the face with the live part's top-left
                # corner takes all of it (the picture runs on across the faces in one plane, so its corners do too).
                tu = lu0 if fu0 <= fu1 else lu0
                if not (lo_u - 1e-4 <= tu < hi_u - 1e-4 and lo_v - 1e-4 <= lv0 < hi_v - 1e-4):
                    continue
                fc = _face_corners(face, e["from"], e["to"])
                r = (spec.get("rotation", 0) // 90) % 4
                ttl, ttr, tbl = fc[r % 4], fc[(1 + r) % 4], fc[(3 + r) % 4]

                def at(u, v):
                    a, b = (u - fu0) / (fu1 - fu0), (v - fv0) / (fv1 - fv0)
                    return tuple(ttl[i] + a * (ttr[i] - ttl[i]) + b * (tbl[i] - ttl[i]) for i in range(3))
                # (a picture turned upside down in its region: its top-left is the region's bottom-right)
                pts = ((lu1, lv1), (lu0, lv1), (lu1, lv0)) if info.get("turn") == 180 else ((lu0, lv0), (lu1, lv0), (lu0, lv1))
                corners = [_rotate(at(*p), e.get("rotation")) for p in pts]
                out.append((corners, {k: v for k, v in info.items() if k not in ("rect", "turn")}))
    cache[key] = out
    return out


def state_live(bs, state, atlases):
    """The live faces of a block state: each one's corners in the block (blocks: top-left, top-right, bottom-left)."""
    out = []
    for model, y in state_models(bs, state):
        for corners, info in _model_live(model, atlases):
            tl, tr, bl = ([round(c / 16, 5) for c in _turn_y(p, y)] for p in corners)
            out.append({**info, "tl": tl, "tr": tr, "bl": bl})
    return out


# ------------------------------------------------------------------ properties
def conditions(when):
    """A multipart condition as a list of {prop: set(values)} alternatives (OR)."""
    if "OR" in when:
        return [c for w in when["OR"] for c in conditions(w)]
    if "AND" in when:
        alts = [{}]
        for w in when["AND"]:
            new = []
            for a in alts:
                for c in conditions(w):
                    m = dict(a)
                    for k, v in c.items():
                        m[k] = m[k] & v if k in m else v
                    new.append(m)
            alts = new
        return alts
    return [{k: set(str(v).split("|")) for k, v in when.items()}]


# values a piece's states take that no part of its blockstate names (a test cage's side with nothing on it, inside a
# bigger cage; its upper blocks with no floor)
EXTRA_VALUES = {"test_cage": {"north": {"none"}, "south": {"none"}, "east": {"none"}, "west": {"none"}, "floor": {"none"}}}


def properties(bs, name=None):
    vals = {k: set(v) for k, v in EXTRA_VALUES.get(name, {}).items()}
    if "variants" in bs:
        for key in bs["variants"]:
            for kv in key.split(","):
                if "=" in kv:
                    a, b = kv.split("=")
                    vals.setdefault(a, set()).add(b)
    else:
        for part in bs["multipart"]:
            for alt in conditions(part.get("when", {})):
                for k, v in alt.items():
                    vals.setdefault(k, set()).update(v)
    props = []
    for name in sorted(vals):
        v = vals[name]
        if name == "facing":
            props.append({"name": name, "type": "facing"})
        elif name == "half" and v <= {"lower", "upper"}:
            props.append({"name": name, "type": "half"})
        elif v <= {"true", "false"}:
            props.append({"name": name, "type": "bool"})
        elif all(x.isdigit() for x in v):
            props.append({"name": name, "type": "int", "max": max(int(x) for x in v)})
        else:
            props.append({"name": name, "type": "enum", "values": sorted(v, key=lambda s: (s not in ("bottom", "lower", "net"), s))})
    return props


def values(p):
    t = p["type"]
    if t == "facing":
        return ["north", "east", "south", "west"]
    if t == "half":
        return ["lower", "upper"]
    if t == "bool":
        return ["false", "true"]
    if t == "int":
        return [str(i) for i in range(p["max"] + 1)]
    return p["values"]


def cells(name, props):
    """The cells one item places, the piece facing north, anchor first: [dx, dy, dz, {part, half}]."""
    by = {p["name"]: p for p in props}
    part = by.get("part")
    if name in KIT_CELLS:                                          # part = the cell's index in the kit's list
        return [[x, y, z, {"part": i}] for i, (x, y, z) in enumerate(KIT_CELLS[name])]
    if name in WALL:                                               # part = row * w + col from the top left as seen
        w, h = WALL[name]
        return [[-c, -r, 0, {"part": r * w + c}] for r in range(h) for c in range(w)]
    if name == "pilot_simulator":                                  # part = layer * 4 + row * 2 + col
        return [[col, layer, row, {"part": layer * 4 + row * 2 + col}] for layer in range(2) for row in range(2) for col in range(2)]
    n = part["max"] + 1 if part and part["type"] == "int" else 1
    nx, nz = GRIDS.get(name, (n, 1))
    halves = ["lower", "upper"] if "half" in by else [None]
    out = []
    for hi, h in enumerate(halves):
        for p in range(n):
            c = {}
            if part and part["type"] == "int":
                c["part"] = p
            if h:
                c["half"] = h
            out.append([p % nx, hi, p // nx, c])
    return out


# ------------------------------------------------------------------ hitboxes
def model_boxes(model, cache={}):
    if model in cache:
        return cache[model]
    ns, path = model.split(":")
    f = ROOT / f"src/main/resources/assets/{ns}/models/{path}.json"
    boxes = []
    if f.exists():
        m = json.loads(f.read_text())
        if "elements" not in m and "parent" in m and ":" in m["parent"]:
            boxes = model_boxes(m["parent"])
        for e in m.get("elements", []):
            (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
            boxes.append((min(x0, x1), min(y0, y1), min(z0, z1), max(x0, x1), max(y0, y1), max(z0, z1)))
    cache[model] = boxes
    return boxes


def turned(b, y):
    x0, y0, z0, x1, y1, z1 = b
    for _ in range((y // 90) % 4):
        x0, z0, x1, z1 = 16 - z1, x0, 16 - z0, x1
    return (x0, y0, z0, x1, y1, z1)


def merge(boxes, limit=20):
    """Boxes (pixels, any count) as a few boxes covering the same: rasterised at 1, 2 or 4 px and merged greedily."""
    for res in (1, 2, 4):
        n = 16 // res
        g = np.zeros((n, n, n), bool)
        for x0, y0, z0, x1, y1, z1 in boxes:
            a = [max(0, min(n, int(math.floor(v / res + 1e-6)))) for v in (x0, y0, z0)]
            b = [max(0, min(n, int(math.ceil(v / res - 1e-6)))) for v in (x1, y1, z1)]
            if b[0] <= a[0] or b[1] <= a[1] or b[2] <= a[2]:
                continue
            g[a[0]:b[0], a[1]:b[1], a[2]:b[2]] = True
        out = []
        done = np.zeros_like(g)
        for y in range(n):
            for z in range(n):
                for x in range(n):
                    if not g[x, y, z] or done[x, y, z]:
                        continue
                    x1 = x
                    while x1 + 1 < n and g[x1 + 1, y, z] and not done[x1 + 1, y, z]:
                        x1 += 1
                    z1 = z
                    while z1 + 1 < n and g[x:x1 + 1, y, z1 + 1].all() and not done[x:x1 + 1, y, z1 + 1].any():
                        z1 += 1
                    y1 = y
                    while y1 + 1 < n and g[x:x1 + 1, y1 + 1, z:z1 + 1].all() and not done[x:x1 + 1, y1 + 1, z:z1 + 1].any():
                        y1 += 1
                    done[x:x1 + 1, y:y1 + 1, z:z1 + 1] = True
                    out.append([x * res, y * res, z * res, (x1 + 1) * res, (y1 + 1) * res, (z1 + 1) * res])
        if len(out) <= limit:
            return out
    return out


def state_models(bs, state):
    """The (model, y) pairs a state shows."""
    if "variants" in bs:
        for key, v in bs["variants"].items():
            want = dict(kv.split("=") for kv in key.split(",") if "=" in kv)
            if all(state.get(k) == val for k, val in want.items()):
                v = v[0] if isinstance(v, list) else v
                return [(v["model"], v.get("y", 0))]
        return []
    out = []
    for part in bs["multipart"]:
        when = part.get("when")
        if when is None or any(all(state.get(k) in v for k, v in alt.items()) for alt in conditions(when)):
            for a in (part["apply"] if isinstance(part["apply"], list) else [part["apply"]]):
                out.append((a["model"], a.get("y", 0)))
    return out


# ------------------------------------------------------------------ item models
# pieces whose models put their fronts to the south (against a wall, their backs to it): the item turned to show them
FRONT_SOUTH = {"briefing_sign", "kitchen_island", "kitchen_run", "kitchenette", "office_bookcase", "lab_tool_cabinet",
               "lipo_cabinet", "crew_sofa", "av_credenza", "plan_chest", "scrub_sink", "instructor_station", "exec_credenza"}
DISPLAY_SCALE = {"gui": ([30, 225, 0], 0.625), "ground": ([0, 0, 0], 0.25), "fixed": ([0, 0, 0], 0.5),
                 "thirdperson_righthand": ([75, 45, 0], 0.375), "firstperson_righthand": ([0, 45, 0], 0.4)}


def _window(cl, size):
    """The cells within `size` cells of the anchor along each axis (the whole piece when it is no bigger)."""
    keep = []
    for i in range(3):
        vals = sorted({c[i] for c in cl})
        if len(vals) <= size:
            keep.append(set(vals))
        elif vals[0] >= 0:
            keep.append(set(range(0, size)))
        elif vals[-1] <= 0:
            keep.append(set(range(-size + 1, 1)))
        else:
            keep.append(set(range(-(size // 2), size - size // 2)))
    return [c for c in cl if all(c[i] in keep[i] for i in range(3))]


# a cell's offset from the anchor (the piece facing north) as the piece faces another way
TURN_CELL = {"north": lambda dx, dz: (dx, dz), "east": lambda dx, dz: (-dz, dx), "south": lambda dx, dz: (-dx, -dz),
             "west": lambda dx, dz: (dz, -dx)}


def _composite(bs, props, cl):
    """The piece's models together, at the facing whose models are drawn unturned (most face north; some south)."""
    def state_of(c, facing):
        state = {"facing": facing, **({k: str(v) for k, v in c.items()})}
        for p in props:
            state.setdefault(p["name"], values(p)[0] if p["type"] != "bool" else "false")
        return state
    facing = next((f for f in ("north", "south", "east", "west")
                   if all(y == 0 for c in cl[:1] for _, y in state_models(bs, state_of(c[3], f)))), "north")
    textures, els = {}, []
    for dx, dy, dz, c in cl:
        state = state_of(c, facing)
        dx, dz = TURN_CELL[facing](dx, dz)
        for model, y in state_models(bs, state):
            if y:
                continue
            ns, path = model.split(":")
            m = json.loads((ROOT / f"src/main/resources/assets/{ns}/models/{path}.json").read_text())
            rename = {}                             # a key the parts give different textures (each part's own face)
            for key, val in m.get("textures", {}).items():
                if key in textures and textures[key] != val:
                    rename[key] = f"{key}_{len(textures)}"
                    textures[rename[key]] = val
                else:
                    textures[key] = val
            for e in m.get("elements", []):
                q = json.loads(json.dumps(e))
                for spec in q.get("faces", {}).values():
                    if spec.get("texture", "").lstrip("#") in rename:
                        spec["texture"] = "#" + rename[spec["texture"].lstrip("#")]
                for face, spec in q.get("faces", {}).items():   # its texels where they were (an automatic UV follows the box)
                    spec.setdefault("uv", MW.auto_uv(face, q["from"], q["to"]))
                for k in ("from", "to"):
                    q[k] = [q[k][0] + 16 * dx, q[k][1] + 16 * dy, q[k][2] + 16 * dz]
                if "rotation" in q:
                    o = q["rotation"]["origin"]
                    q["rotation"]["origin"] = [o[0] + 16 * dx, o[1] + 16 * dy, o[2] + 16 * dz]
                for fc in q.get("faces", {}).values():
                    fc.pop("cullface", None)
                els.append(q)
    return textures, els


def _problems(model):
    """What the art check (tools/art_check.py) would find in the model: flush or near faces drawn differently, holes."""
    import art_check as AC
    elements, lookup, _ = AC._resolve(model, (ASSETS,))
    faces, raw = AC._model_faces(elements, lookup)
    n = sum(1 for i, j, gap, overlap, u, v, nn, d in AC._pairs(faces, AC.N1_GAP)
            if not (gap == 0 and AC._layered(elements, faces[i], faces[j])) and AC._differ(faces[i], faces[j], overlap, u, v, nn, d))
    return n + len(AC._open_faces(elements, faces))


def item_model(name, bs, props, cl):
    """The piece facing its models' way, its parts' models at their cells, centred on the block. A model may reach a
    block beyond it each way (48 pixels across): a piece no bigger is shown at its true size; a bigger one scaled to fit
    if that keeps its faces apart (scaling brings faces held apart in the blocks closer: they would flicker), otherwise
    the three blocks of it nearest its anchor at their true size. The displays scale it to the slot, hand and ground."""
    whole = _composite(bs, props, cl)[1]
    if not whole:
        return None
    span = max(max(max(e["from"][i], e["to"][i]) for e in whole) - min(min(e["from"][i], e["to"][i]) for e in whole) for i in range(3))
    tries = ([(len(cl), 48 / span)] if span > 48 else []) + [(3, 1.0), (2, 1.0), (1, 1.0)]
    for size, s in tries:
        textures, els = _composite(bs, props, cl if size == len(cl) else _window(cl, size))
        lo = [min(min(e["from"][i], e["to"][i]) for e in els) for i in range(3)]
        hi = [max(max(e["from"][i], e["to"][i]) for e in els) for i in range(3)]
        if max(hi[i] - lo[i] for i in range(3)) * s > 48.001 and size > 1:
            continue
        c = [(lo[i] + hi[i]) / 2 for i in range(3)]

        def f(p):
            return [round((p[i] - c[i]) * s + 8, 4) for i in range(3)]
        for e in els:
            e["from"], e["to"] = f(e["from"]), f(e["to"])
            if "rotation" in e:
                e["rotation"]["origin"] = f(e["rotation"]["origin"])
        MW.close_open_faces(els)                    # the cut edges of a piece shown in part
        MW.fit_uvs(els)
        extent = max(16.0, max(hi[i] - lo[i] for i in range(3)) * s)
        turn = 180 if name in FRONT_SOUTH else 0
        display = {k: {"rotation": [r[0], (r[1] + turn) % 360, r[2]], "translation": [0, 0, 0], "scale": [round(sc * 16 / extent, 4)] * 3}
                   for k, (r, sc) in DISPLAY_SCALE.items()}
        model = {"parent": "minecraft:block/block", "textures": textures, "elements": els, "display": display}
        if s == 1.0 and _problems(model):
            # a multipart's parts shown together without the turned ones that bury their edges: pulled apart
            MW.separate_coplanar(els, pin_uv=False)
            MW.close_open_faces(els)
            MW.fit_uvs(els)
        if size == 1 or _problems(model) == 0:
            return model
    return model


def material(name):
    if name in MATERIAL:
        return MATERIAL[name]
    if any(k in name for k in STONE):
        return "stone"
    if any(k in name for k in WOOL):
        return "wool"
    if any(k in name for k in WOOD):
        return "wood"
    return "metal"


# ------------------------------------------------------------------ all of it
def build(blocks=None, lang=None):
    """Every piece's manifest entry, item model and loot table; its name into lang (the generator's), or into the lang
    file when run alone."""
    blocks = blocks or write_kits()
    if set(blocks) != set(PIECES):
        raise SystemExit(f"the kits made {sorted(set(blocks) - set(PIECES))} not in room_kit_recipes.ROOMS, "
                         f"and not {sorted(set(PIECES) - set(blocks))}")
    shapes, shape_index, out = [], {}, []
    lives, live_index = [], {}
    lang_f = ASSETS / "lang/en_us.json"
    names = json.loads(lang_f.read_text()) if lang is None else lang
    for name in sorted(blocks):
        room = PIECES[name]
        bs = json.loads((ASSETS / f"blockstates/{name}.json").read_text())
        props = properties(bs, name)
        cl = cells(name, props)
        states, live = {}, {}
        atlases = KIT_ATLASES.get(blocks[name][1], {})
        for combo in itertools.product(*[values(p) for p in props]):
            state = {p["name"]: v for p, v in zip(props, combo)}
            boxes = [turned(b, y) for model, y in state_models(bs, state) for b in model_boxes(model)]
            key = json.dumps(merge(boxes))
            if key not in shape_index:
                shape_index[key] = len(shapes)
                shapes.append(json.loads(key))
            skey = ",".join(f"{k}={state[k]}" for k in sorted(state))
            states[skey] = shape_index[key]
            faces = state_live(bs, state, atlases)
            if faces:
                lkey = json.dumps(faces, sort_keys=True)
                if lkey not in live_index:
                    live_index[lkey] = len(lives)
                    lives.append(faces)
                live[skey] = live_index[lkey]
        out.append({"id": name, "room": room, "light": LIGHT.get(name, 0), "material": material(name), "props": props,
                    "cells": cl, "states": states, **({"display": DISPLAY[name]} if name in DISPLAY else {}),
                    **({"rotors": KIT_ROTORS[name]} if name in KIT_ROTORS else {}),
                    **({"live": live} if live else {})})
        im = item_model(name, bs, props, cl)
        if im:
            (ASSETS / f"models/item/{name}.json").parent.mkdir(parents=True, exist_ok=True)
            (ASSETS / f"models/item/{name}.json").write_text(json.dumps(im) + "\n")
            (ASSETS / f"items/{name}.json").parent.mkdir(parents=True, exist_ok=True)
            (ASSETS / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"jugcraft:item/{name}"}}, indent=2) + "\n")
        (DATA / "loot_table/blocks").mkdir(parents=True, exist_ok=True)
        (DATA / f"loot_table/blocks/{name}.json").write_text(json.dumps({
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{name}"}],
                                                   "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"jugcraft:blocks/{name}"}, indent=2) + "\n")
        names.setdefault(f"block.jugcraft.{name}", title(name))
    if lang is None:
        lang_f.write_text(json.dumps(dict(sorted(names.items())), indent=2) + "\n")
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({"blocks": out, "shapes": shapes, "live": lives}, separators=(",", ":")) + "\n")
    return out, shapes


def write_assets(lang):
    """The full generator's call (generate_material_data.assets): the kits, the manifest, the item models, the loot."""
    return build(lang=lang)


def write_recipes_and_tags():
    """Run alone: each piece's recipe as the generator writes the tower's (generate_material_data.recipes: the machines
    and drones switches, and those of what it is made of), and the pieces in the pickaxe tag (machine_blocks')."""
    sys.path.insert(0, str(ROOT / "tools"))
    import generate_material_data as g
    import tower
    import drones
    for result, (pattern, key, count) in CRAFTING.items():
        found = set()
        for ref in key.values():
            if ref.startswith(("#c:plates/", "#c:ingots/", "#c:gears/", "#c:wires/", "#c:dusts/")):
                metal = ref.split("/")[-1]
                if metal not in ("copper", "iron", "gold"):
                    found.add(g.feature_of(f"{metal}_ingot"))
            elif ref.startswith("jugcraft:"):
                found.update(tower.ITEM_FEATURES.get(ref.split(":")[1], []) + drones.ITEM_FEATURES.get(ref.split(":")[1], []))
        features = [g.MACHINE_FEATURE, drones.FEATURE] + sorted(found - {g.MACHINE_FEATURE, drones.FEATURE})
        recipe = g.shaped(g.MACHINE_FEATURE, pattern, key, result, count, "building")
        recipe["fabric:load_conditions"] = [c for f in features for c in g.condition(f)]
        g.write(DATA / "recipe" / f"{result}.json", recipe)
    import advanced_crafting              # made on the Advanced Workbench (the same cost, a shape bigger than 3 x 3)
    advanced_crafting.convert(DATA / "recipe", list(CRAFTING))
    tag = DATA.parent / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(tag.read_text())
    have = set(v if isinstance(v, str) else v["id"] for v in t["values"])
    t["values"] += [f"jugcraft:{n}" for n in sorted(PIECES) if f"jugcraft:{n}" not in have]
    tag.write_text(json.dumps(t, indent=2) + "\n")


if __name__ == "__main__":
    out, shapes = build()
    write_recipes_and_tags()
    live = [b for b in out if "live" in b]
    print(len(out), "blocks;", len(shapes), "shapes;", sum(len(b["states"]) for b in out), "states;",
          MANIFEST.stat().st_size, "bytes;", len(live), "with live faces:", " ".join(b["id"] for b in live))
