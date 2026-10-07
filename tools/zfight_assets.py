"""Z-fighting between models drawn together (Justin: "fix all z-fighting"), checked on the written assets after the
generators have run (each model on its own is already clean, tools/zfight.py):

- multipart blockstates: every model a state draws at once (a counter's body and its end, a stack's cap, pipe arms);
- pieces bigger than a block: the parts of a wall screen, an oval table, a panel, a storage rack, the blueprint
  table, put together as they stand;
- a block beside or on another of its own kind (a row of counters, stacked drums): elements reaching past the
  block's edge must not lie on a neighbour's faces.

Where two faces of different models (or parts) lie in the same plane facing the same way and overlap, one is pulled
0.02 px out along its normal in its own model file. Run: python3 tools/zfight_assets.py [--check]. Exits 1 if any
overlap is left.
"""
import itertools
import json
import math
import re
import sys
from pathlib import Path

import numpy as np

import zfight

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
MOD = "jugcraft"
EPS = zfight.EPS


class Models:
    """The mod's block models, loaded once; a model's elements come from the first file up its parent chain that has
    them (that file is the one changed)."""

    def __init__(self):
        self.files = {}
        self.dirty = set()

    def path(self, ref):
        ns, _, p = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
        return ASSETS.parent / ns / "models" / f"{p}.json" if ns == MOD else None

    def load(self, ref):
        f = self.path(ref)
        if f is None or not f.exists():
            return None
        if f not in self.files:
            self.files[f] = json.loads(f.read_text())
        return self.files[f]

    def owner(self, ref, depth=0):
        """(file, model) of the file holding the elements of ref, or None."""
        m = self.load(ref)
        if m is None or depth > 8:
            return None
        if m.get("elements") is not None:
            return self.path(ref), m
        parent = m.get("parent")
        return self.owner(parent if ":" in parent else "minecraft:" + parent, depth + 1) if parent else None

    def save(self):
        for f in self.dirty:
            f.write_text(json.dumps(self.files[f], indent=2) + "\n", encoding="utf-8")


def _rot(axis, deg):
    return zfight._rot(axis, deg)


def world_faces(models, ref, xr=0, yr=0, offset=(0, 0, 0), tag=None):
    """[(owner, file, element index, local face, pts, normal, plane)] of a model as a blockstate draws it (x then y
    rotation about the block's centre) at offset (pixels)."""
    got = models.owner(ref)
    if got is None:
        return []
    f, m = got
    R = _rot("y", -yr) @ _rot("x", -xr)
    c = np.array([8.0, 8.0, 8.0])
    out = []
    for i, e in enumerate(m.get("elements", [])):
        for face, pts in zfight_face_points(e):
            pts = (pts - c) @ R.T + c + np.array(offset, float)
            n = np.cross(pts[3] - pts[0], pts[1] - pts[0])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
            n /= ln
            out.append((tag if tag is not None else (str(f), xr, yr, tuple(offset)), f, i, face, pts, n, float(n @ pts[0])))
    return out


def zfight_face_points(e):
    f, t = e["from"], e["to"]
    rot = e.get("rotation")
    for face in e.get("faces", {}):
        pts = np.array(zfight._corners(face, f, t), float)
        if rot:
            o = np.array(rot.get("origin", [8, 8, 8]), float)
            if "axis" in rot:
                M = _rot(rot["axis"], rot["angle"])
                if rot.get("rescale"):
                    k = 1 / math.cos(math.radians(rot["angle"]))
                    sc = np.ones(3) * k
                    sc["xyz".index(rot["axis"])] = 1
                    M = M @ np.diag(sc)
            else:
                M = _rot("x", rot.get("x", 0)) @ _rot("y", rot.get("y", 0)) @ _rot("z", rot.get("z", 0))
            pts = (pts - o) @ M.T + o
        yield face, pts


def cross_hits(faces):
    """Pairs of faces of different owners: same plane, same way, overlapping."""
    hits = []
    by_plane = {}
    for k, fc in enumerate(faces):
        n, d = fc[5], fc[6]
        key = (round(n[0], 3), round(n[1], 3), round(n[2], 3), round(d, 2))
        by_plane.setdefault(key, []).append(k)
    for ks in by_plane.values():
        for a, b in itertools.combinations(ks, 2):
            fa, fb = faces[a], faces[b]
            if fa[0] == fb[0] or abs(fa[6] - fb[6]) > 1e-3:
                continue
            na = fa[5]
            u = np.cross(na, [0, 1, 0] if abs(na[1]) < 0.9 else [1, 0, 0])
            u /= np.linalg.norm(u)
            v = np.cross(na, u)
            A = zfight._ccw([(float(p @ u), float(p @ v)) for p in fa[4]])
            B = zfight._ccw([(float(p @ u), float(p @ v)) for p in fb[4]])
            inter = A
            for k in range(len(B)):
                inter = zfight._clip(inter, B[k - 1], B[k])
                if not inter:
                    break
            if inter and zfight._area(inter) > 1e-4:
                hits.append((zfight._area(inter), fa, fb))
    return hits


def push(models, fc):
    """Pulls a face out of its plane by EPS (in its own model file)."""
    _, f, i, face, *_ = fc
    e = models.files[f]["elements"][i]
    axis, end, sign = zfight._OUT[face]
    key = "to" if end else "from"
    e[key][axis] = round(e[key][axis] + sign * EPS, 4)
    models.dirty.add(f)


def parse_when(when):
    if "OR" in when:
        return [c for w in when["OR"] for c in parse_when(w)]
    if "AND" in when:
        merged = {}
        for w in when["AND"]:
            for c in parse_when(w):
                merged.update(c)
        return [merged]
    return [{k: str(v).split("|") for k, v in when.items()}]


def multipart_groups(models):
    """For every multipart blockstate: lists of (model, x, y) drawn together in some state."""
    groups = []
    for bs in sorted((ASSETS / "blockstates").glob("*.json")):
        d = json.loads(bs.read_text())
        if "multipart" not in d:
            continue
        parts = []
        domains = {}
        for part in d["multipart"]:
            conds = parse_when(part["when"]) if part.get("when") else [{}]
            ap = part["apply"]
            ap = ap if isinstance(ap, list) else [ap]
            parts.append((conds, ap[0]))
            for c in conds:
                for k, vs in c.items():
                    domains.setdefault(k, set()).update(vs)
        keys = sorted(domains)
        values = [sorted(domains[k]) + ["~other"] for k in keys]
        total = 1
        for v in values:
            total *= len(v)
        combos = itertools.product(*values) if total <= 4096 else ()
        seen = set()
        for combo in combos:
            state = dict(zip(keys, combo))
            applied = tuple(sorted({(ap["model"], ap.get("x", 0), ap.get("y", 0)) for conds, ap in parts
                                    if any(all(state.get(k) in vs for k, vs in c.items()) for c in conds)}))
            if len(applied) > 1 and applied not in seen:
                seen.add(applied)
                groups.append((bs.stem, applied))
    return groups


def furniture_families():
    """Pieces bigger than a block (PlantInterior.java FURNITURE kinds): name -> (width, rows, vertical)."""
    java = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/nuclear/PlantInterior.java").read_text()
    out = {}
    for name, kind in re.findall(r'\{"([a-z0-9_]+)", \d+, "([a-z0-9:.]+)"', java):
        k = kind.split(":")
        if k[0] == "multi":
            out[name] = (int(k[1]), int(k[2]), k[3] not in ("h", "c"))
        elif k[0] == "live":
            out[name] = (int(k[1]), int(k[2]), True)
        elif kind == "rack":
            out[name] = (3, 3, True)
    return out


def family_groups(models):
    """For each piece bigger than a block, facing north: its parts' models placed where they stand (per variant)."""
    groups = []
    for name, (w, rows, vertical) in furniture_families().items():
        bs = ASSETS / "blockstates" / f"{name}.json"
        if not bs.exists():
            continue
        variants = json.loads(bs.read_text()).get("variants", {})
        by_variant = {}
        for key, v in variants.items():
            props = dict(kv.split("=") for kv in key.split(","))
            if props.get("facing") != "north" or "part" not in props:
                continue
            v = v[0] if isinstance(v, list) else v
            p = int(props["part"])
            c, r = p % w, p // w
            offset = (16 * c, 16 * r, 0) if vertical else (16 * c, 0, 16 * r)
            by_variant.setdefault(props.get("variant", "0"), []).append((v["model"], v.get("x", 0), v.get("y", 0), offset))
        for variant, parts in by_variant.items():
            groups.append((f"{name}[{variant}]", parts))
    # The blueprint table: its side half to the right of the main half (facing north: east).
    bs = ASSETS / "blockstates" / "blueprint_table.json"
    if bs.exists():
        variants = json.loads(bs.read_text()).get("variants", {})
        main = next((v for k, v in variants.items() if "facing=north" in k and "part=main" in k), None)
        side = next((v for k, v in variants.items() if "facing=north" in k and "part=side" in k), None)
        if main and side:
            groups.append(("blueprint_table", [(main["model"], main.get("x", 0), main.get("y", 0), (0, 0, 0)),
                                               (side["model"], side.get("x", 0), side.get("y", 0), (16, 0, 0))]))
    return groups


def tiling():
    """Blocks that stand in a run of their own kind, and along which axes (model frame, facing north): rows of
    counters, benches and desks side by side (x), stacks of lockers and racks (y), railing (x, z), shafts (z)."""
    java = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/nuclear/PlantInterior.java").read_text()
    out = {"guard_rail": "x", "railing": "xz"}
    for name, kind in re.findall(r'\{"([a-z0-9_]+)", \d+, "([a-z0-9:.]+)"', java):
        if kind.startswith("row:"):
            out[name] = "x"
        elif kind == "stack" or kind.startswith(("stack:", "locker:")):
            out[name] = "y"
    for bs in (ASSETS / "blockstates").glob("*.json"):
        d = json.loads(bs.read_text())
        if any(k.startswith("axis=") or ",axis=" in k for k in d.get("variants", {})):
            out.setdefault(bs.stem, "z")
    return out


def _body(bs_name):
    """The model(s) a block draws in its plainest state facing north (or along z), as (model, x, y)."""
    bs = ASSETS / "blockstates" / f"{bs_name}.json"
    if not bs.exists():
        return []
    d = json.loads(bs.read_text())
    if "variants" in d:
        for key, v in d["variants"].items():
            props = dict(kv.split("=") for kv in key.split(",") if "=" in kv)
            if props.get("facing", "north") == "north" and props.get("axis", "z") == "z":
                v = v[0] if isinstance(v, list) else v
                return [(v["model"], v.get("x", 0), v.get("y", 0))]
        return []
    out = []
    for part in d["multipart"]:
        when = part.get("when", {})
        conds = parse_when(when) if when else [{}]
        if any(set(c) <= {"facing"} and "north" in c.get("facing", ["north"]) for c in conds):
            ap = part["apply"] if not isinstance(part["apply"], list) else part["apply"][0]
            out.append((ap["model"], ap.get("x", 0), ap.get("y", 0)))
    return out


def neighbour_groups(models):
    """A block beside (or on) another of its own kind: the faces of the two must not lie on each other."""
    groups = []
    for name, axes in sorted(tiling().items()):
        body = _body(name)
        if not body:
            continue
        for a in axes:
            off = [0, 0, 0]
            off["xyz".index(a)] = 16
            parts = [(ref, x, y, (0, 0, 0)) for ref, x, y in body] + [(ref, x, y, tuple(off)) for ref, x, y in body]
            groups.append((f"{name}+{a}", parts))
    return groups


def collect(models):
    left = []
    for name, applied in multipart_groups(models):
        faces = []
        for k, (ref, xr, yr) in enumerate(applied):
            faces += world_faces(models, ref, xr, yr, (0, 0, 0), tag=(ref, k))
        left += [(("multipart", name), h) for h in cross_hits(faces)]
    for name, parts in family_groups(models) + neighbour_groups(models):
        faces = []
        for k, (ref, xr, yr, off) in enumerate(parts):
            faces += world_faces(models, ref, xr, yr, off, tag=(ref, k))
        left += [(("parts", name), h) for h in cross_hits(faces)]
    return left


def split(models, blockstate, ref):
    """Gives each turned use of a model in a multipart blockstate its own copy (so a face of one can be moved without
    moving it in the others, which lie on it)."""
    bs = ASSETS / "blockstates" / f"{blockstate}.json"
    d = json.loads(bs.read_text())
    owner = models.owner(ref)
    if owner is None:
        return
    whole = json.loads(json.dumps(owner[1]))
    own = models.load(ref)
    if own is not owner[1]:
        # A model drawn from a parent's elements with its own textures: the copy carries both.
        whole.setdefault("textures", {}).update(own.get("textures", {}))
        for k, v in own.items():
            if k not in ("parent", "textures", "elements"):
                whole[k] = v
    first = None
    for part in d["multipart"]:
        aps = part["apply"] if isinstance(part["apply"], list) else [part["apply"]]
        for ap in aps:
            if ap["model"] != ref:
                continue
            turn = (ap.get("x", 0), ap.get("y", 0))
            if first is None or first == turn:
                first = turn
                continue
            copy = f"{ref}_r{turn[0]}_{turn[1]}"
            f = models.path(copy)
            f.write_text(json.dumps(whole, indent=2) + "\n", encoding="utf-8")
            ap["model"] = copy
    bs.write_text(json.dumps(d, indent=2) + "\n", encoding="utf-8")


def run(fix=True, passes=16):
    left = []
    ever = set()
    count = {}
    for _ in range(passes):
        models = Models()
        left = collect(models)
        if not left or not fix:
            break
        splits = {(name[1], fa[0][0]) for name, (area, fa, fb) in left
                  if name[0] == "multipart" and fa[1] == fb[1] and fa[0] != fb[0]}
        if splits:
            for blockstate, ref in sorted(splits):
                split(models, blockstate, ref)
            continue
        moved = set()
        for name, (area, fa, fb) in left:
            if fa[1] == fb[1] and fa[2] == fb[2]:
                continue  # the same element in two copies of a block: can't be told apart (reported)
            # Move a face that hasn't been moved before (two faces taking turns would never part).
            if (fb[1], fb[2], fb[3]) in ever and (fa[1], fa[2], fa[3]) not in ever:
                fa, fb = fb, fa
            key = (fb[1], fb[2], fb[3])
            if key in moved:
                continue
            ever.add(key)
            e = models.files[fb[1]]["elements"][fb[2]]
            # A face moved again goes further each time, so three or more faces in one plane all end up apart.
            count[key] = count.get(key, 0) + 1
            zfight.nudge(e, fb[3], EPS * count[key])
            models.dirty.add(fb[1])
            moved.add(key)
        # A moved face must not land on another face of its own model either.
        for f in models.dirty:
            models.files[f] = zfight.fix(models.files[f])
        models.save()
    else:
        left = collect(Models()) if fix else left
    return left


def inside_overlaps():
    """Model files (the mod's and its resource packs') with z-fighting inside them."""
    bad = []
    for f in sorted(ROOT.glob("src/main/resources/**/models/**/*.json")):
        m = json.loads(f.read_text())
        if m.get("elements") and zfight.overlaps(m["elements"]):
            bad.append(f)
    return bad


if __name__ == "__main__":
    check = "--check" in sys.argv
    left = run(fix=not check)
    for f in inside_overlaps():
        print(f"{f.relative_to(ROOT)}: faces of the model itself overlap")
        left.append(f)
    for name, (area, fa, fb) in left[:60]:
        print(f"{name[1]}: {area:.3f} px2  {Path(fa[1]).stem} el{fa[2]}.{fa[3]}  vs  {Path(fb[1]).stem} el{fb[2]}.{fb[3]}")
    print(f"{len(left)} overlaps between models drawn together")
    sys.exit(1 if left else 0)
