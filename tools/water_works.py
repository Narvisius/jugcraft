"""The medium plant's water works in game (Justin, 19 October 2026: the cooling towers and pump houses, every piece
approved, "build it"): the site compiler. It runs the approved generators (tools/cooling_tower.py, tools/pump_house.py)
and writes what the game needs (claude/jugcraft-towers-pumphouses-build.md, "Build plan"):

- each site's static geometry, cut at block boundaries and filed by block, with collision boxes and the cells that are
  climbable or under water, to assets/jugcraft/sites/<site>.bin.gz (client/SiteModel.java bakes it into the chunk
  meshes; nuclear/SiteBlock.java gives its shapes);
- the live groups: what changes with each state the game drives (a pump running, a valve, a lamp, the lights, a door,
  a meter's needle), found by running the generators with each state changed on its own and comparing; drawn by the
  site's anchor (client/SiteRenderer.java), with the collision of the ones that have any (a door);
- the textures, as block textures (textures/block/site/), sized to multiples of 16 so the block atlas keeps its
  mipmaps; texture regions that change with a state (a figure on a display) become their own small textures per state;
- the blueprint cells: which blocks are site structure (or its ladder), which hold water, where the anchor is.

Site coordinates are the generators' own (blocks; x east, y up from the grass, z south), its origin on the grass: the
tower's axis, the intake's origin (x 0, 10 m inland of the curtain wall's face), the circulating water pump house's
north-west corner.
"""
import hashlib
import json
import math
import struct
import sys
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
from PIL import Image

import cooling_tower as CT
import site_bake as SB

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft"
SITES_OUT = ASSETS / "sites"
CELLS_OUT = Path(__file__).resolve().parent / "site_cells"      # each site's cells, for its blueprint part
TEX_OUT = ASSETS / "textures" / "block" / "site"


def canon(op):
    """A key for comparing operations between runs (floats rounded)."""
    def r(v):
        if isinstance(v, float):
            return round(v, 5)
        if isinstance(v, (list, tuple)):
            return [r(x) for x in v]
        if isinstance(v, dict):
            return {k: r(x) for k, x in v.items()}
        if isinstance(v, np.floating):
            return round(float(v), 5)
        return v
    return json.dumps(r(op), separators=(",", ":"))


def plain(op):
    """An operation with numpy numbers made plain (for JSON)."""
    if isinstance(op, (list, tuple)):
        return [plain(x) for x in op]
    if isinstance(op, dict):
        return {k: plain(v) for k, v in op.items()}
    if isinstance(op, np.generic):
        return op.item()
    return op


class Var:
    """A state the game drives: its values (the first is the base the static geometry is drawn in)."""

    def __init__(self, name, values, kind="discrete"):
        self.name, self.values, self.kind = name, list(values), kind


class Site:
    """A site to compile: subclasses give the generators' operations and textures for a state."""
    key = ""
    variables = []
    exclude = set()          # materials never drawn (water bodies the world's water blocks fill)

    def base(self):
        return {v.name: v.values[0] for v in self.variables}

    def ops(self, state):
        raise NotImplementedError

    def textures(self, state):
        raise NotImplementedError

    def anchor(self):
        """The anchor's cell (site coordinates)."""
        raise NotImplementedError

    def water(self, cell):
        """Whether a cell holds water (a waterlogged structure block, or a water block)."""
        return False

    def ladders(self):
        """Climbable regions: [(a, b, out)]: the rungs' centre line from a to b, out the unit vector toward the climber."""
        return []

    def collision_ops(self, still):
        """The operations that are solid (default: all of the static ones)."""
        return still

    def zones(self):
        """Click zones: [(name, (x0, y0, z0, x1, y1, z1))] in site coordinates."""
        return []

    def displays(self):
        """Live figures drawn in glyphs: [{...}]."""
        return []

    def collision_extra(self, vox):
        """Solids no operation gives (a volume drawn only by its faces)."""

    def extra_textures(self):
        """Textures the game uses that no operation does (the tower shell's)."""
        return []

    def shell(self):
        """The tower shell this site has (drawn by the game from its profile), or None."""
        return None

    def texture_vars(self):
        """The variables whose values change textures (figures, lamps on screens)."""
        return set()

    def group_solid(self, name):
        """Whether a variable's groups collide (a door's leaf)."""
        return name.startswith("door")

    def keep_cell(self, cell):
        """Whether a cell (site coordinates) keeps its geometry: a site standing on the grass leaves out what its
        generators draw below it (footings, risers into the ground), and what reaches into a neighbouring building."""
        return True

    def keep_poly(self, poly):
        """Whether a polygon (site coordinates) is drawn: a site standing on the grass leaves out the faces level with
        the ground's own top (the ground's block draws that face)."""
        return True


def _keyed(ops, lit):
    return [(canon(o) + ("L" if lit else "S"), o, lit) for o in ops]


def run(site, state):
    if hasattr(site, "items"):
        return site.items(state)
    o = site.ops(state)
    items = _keyed(o["still"], False) + _keyed(o["lit"], True)
    return [it for it in items if not _excluded(site, it[1])]


def _mats(op):
    k = op[0]
    vals = {"b": [op[7]] if k == "b" else None, "o": [op[13]] if k == "o" else None, "q": [op[13]] if k == "q" else None,
            "c": [op[9], op[10]] if k == "c" else None}.get(k) or []
    out = []
    for v in vals:
        if v is None:
            continue
        if isinstance(v, str):
            out.append(v)
        elif isinstance(v, list) and len(v) == 5 and isinstance(v[1], (int, float)):
            out.append(v[0])
        elif isinstance(v, list):
            out += [x if isinstance(x, str) else x[0] for x in v if x is not None]
        elif isinstance(v, dict):
            out += [x if isinstance(x, str) else x[0] for x in v.values() if x is not None]
    return out


def _excluded(site, op):
    return any(m in site.exclude for m in _mats(op))


def diff(site):
    """The static operations and the live groups {(var, value index): [(op, lit)]}: a variable's base group holds every
    operation of the base that some other value of it takes away; each other value's group, what it draws of those and
    what it adds."""
    base = site.base()
    B = run(site, base)
    bkeys = Counter(k for k, _, _ in B)
    groups = {}
    taken = Counter()
    owner = defaultdict(set)
    for v in site.variables:
        runs = []
        g0keys = Counter()
        for i, x in enumerate(v.values[1:], 1):
            st = dict(base)
            st[v.name] = x
            V = run(site, st)
            vkeys = Counter(k for k, _, _ in V)
            g0keys |= (bkeys - vkeys)
            runs.append((i, V, vkeys))
        g0 = []
        want = Counter(g0keys)
        for k, o, l in B:
            if want[k] > 0:
                g0.append((o, l))
                want[k] -= 1
                owner[k].add(v.name)
        groups[(v.name, 0)] = g0
        taken += g0keys
        for i, V, vkeys in runs:
            keep = (vkeys - bkeys) + (g0keys & vkeys)
            gx = []
            for k, o, l in V:
                if keep[k] > 0:
                    gx.append((o, l))
                    keep[k] -= 1
            groups[(v.name, i)] = gx
    still, lit = [], []
    left = Counter(taken)
    for k, o, l in B:
        if left[k] > 0:
            left[k] -= 1
            continue
        (lit if l else still).append(o)
    shared = [k for k, names in owner.items() if len(names) > 1]
    if shared:
        print(f"  {site.key}: {len(shared)} operations change with more than one state", file=sys.stderr)
    return still, lit, groups


# ------------------------------------------------------------------ textures

def mult16(n):
    return max(16, int(math.ceil(n / 16.0)) * 16)


def export_textures(images, out_dir, names):
    """Writes each texture used, sized to multiples of 16 (stretched to fit, so its UVs are unchanged)."""
    out_dir.mkdir(parents=True, exist_ok=True)
    layers = {}
    for name in sorted(names):
        img = images[name].convert("RGBA")
        w, h = img.size
        W, H = mult16(w), mult16(h)
        if (W, H) != (w, h):
            img = img.resize((W, H), Image.LANCZOS if img.mode != "RGBA" or np.asarray(img)[..., 3].min() == 255 else Image.NEAREST)
        img.save(out_dir / f"{name}.png", optimize=True)
        layers[name] = SB.texture_layer(img)
    return layers


def region_crop(img, u0, v0, u1, v1):
    w, h = img.size
    x0, x1 = sorted((u0 * w, u1 * w))
    y0, y1 = sorted((v0 * h, v1 * h))
    return img.crop((int(math.floor(x0)), int(math.floor(y0)), int(math.ceil(x1)), int(math.ceil(y1))))


def _diff_mask(a, b):
    a = np.asarray(a.convert("RGBA"), np.int16)
    b = np.asarray(b.convert("RGBA"), np.int16)
    return np.any(a != b, axis=2)


def _uv_clip(P, U, rect):
    """A polygon clipped to a rectangle of its texture (u0, v0, u1, v1; texture fractions), its corners' positions
    following their UVs."""
    pts = [np.asarray(p, float) for p in P]
    uvs = [np.asarray(u, float) for u in U]
    for axis, value, below in ((0, rect[0], False), (0, rect[2], True), (1, rect[1], False), (1, rect[3], True)):
        out_p, out_u = [], []
        n = len(pts)
        for i in range(n):
            p, q, up, uq = pts[i], pts[(i + 1) % n], uvs[i], uvs[(i + 1) % n]
            dp, dq = up[axis] - value, uq[axis] - value
            inp = dp <= 1e-12 if below else dp >= -1e-12
            inq = dq <= 1e-12 if below else dq >= -1e-12
            if inp:
                out_p.append(p)
                out_u.append(up)
            if inp != inq and abs(dp - dq) > 1e-15:
                t = dp / (dp - dq)
                out_p.append(p + (q - p) * t)
                out_u.append(up + (uq - up) * t)
        pts, uvs = out_p, out_u
        if len(pts) < 3:
            return None
    return pts, uvs


def texture_states(site, still_items):
    """Figures and pictures that change with the states the game drives (a flow on a display, a pump's symbol on an
    operator screen): for each static operation whose texture region changes, the changed patch of it is drawn over it
    (8/10 mm in front) per state, cropped from that state's texture. Each patch follows the variables that change it:
    one (a pump's own figure) or several together (a train's flow, two pumps' sum: every combination of their values).
    Returns (the static operations, untouched, extra groups, extra images, joint variables)."""
    base = site.base()
    T0 = site.textures(base)
    names = site.texture_vars()
    vars_ = [v for v in site.variables if v.name in names]
    # what each variable's other values change, pixel by pixel
    masks = defaultdict(dict)
    for v in vars_:
        for i, x in enumerate(v.values[1:], 1):
            st = dict(base)
            st[v.name] = x
            T1 = site.textures(st)
            for n in T0:
                if n in T1 and T0[n].size == T1[n].size:
                    m = _diff_mask(T0[n], T1[n])
                    if m.any():
                        masks[n][v.name] = masks[n].get(v.name, np.zeros_like(m)) | m
    extra_groups = defaultdict(list)
    images = {}
    joints = []
    crops = {}
    if not masks:
        return still_items, extra_groups, images, joints
    for o, lit in still_items:
        for m in _mat_regions(o):
            if m[0] not in masks:
                continue
            img = T0[m[0]]
            w, h = img.size
            x0, x1 = sorted((m[1] * w, m[3] * w))
            y0, y1 = sorted((m[2] * h, m[4] * h))
            X0, X1, Y0, Y1 = int(math.floor(x0)), int(math.ceil(x1)), int(math.floor(y0)), int(math.ceil(y1))
            # each variable's changed box in this region, grown 2 px; boxes that overlap join
            boxes = []
            for vn, mask in masks[m[0]].items():
                sub = mask[Y0:Y1, X0:X1]
                if not sub.any():
                    continue
                ys, xs = np.nonzero(sub)
                boxes.append(({vn}, [max(X0, X0 + xs.min() - 2), max(Y0, Y0 + ys.min() - 2), min(X1, X0 + xs.max() + 3),
                                     min(Y1, Y0 + ys.max() + 3)]))
            merged = True
            while merged:
                merged = False
                for i in range(len(boxes)):
                    for j in range(i + 1, len(boxes)):
                        a, b = boxes[i][1], boxes[j][1]
                        if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                            boxes[i] = (boxes[i][0] | boxes[j][0], [min(a[0], b[0]), min(a[1], b[1]), max(a[2], b[2]), max(a[3], b[3])])
                            boxes.pop(j)
                            merged = True
                            break
                    if merged:
                        break
            polys = [p for p in SB.tessellate([o] if not lit else [], [o] if lit else []) if p[3] == m[0]]
            for vset, (bx0, by0, bx1, by1) in boxes:
                members = [v for v in vars_ if v.name in vset]
                rect = (bx0 / w, by0 / h, bx1 / w, by1 / h)
                if len(members) == 1:
                    gname = members[0].name
                    combos = [(k,) for k in range(len(members[0].values))]
                else:
                    gname = "joint:" + "+".join(v.name for v in members)
                    if gname not in [j["name"] for j in joints]:
                        joints.append({"name": gname, "members": [v.name for v in members]})
                    combos = []
                    total = 1
                    for v in members:
                        total *= len(v.values)
                    for idx in range(total):
                        c, r = [], idx
                        for v in members:
                            c.append(r % len(v.values))
                            r //= len(v.values)
                        combos.append(tuple(c))
                for gi, combo in enumerate(combos):
                    st = dict(base)
                    for v, k in zip(members, combo):
                        st[v.name] = v.values[k]
                    T = site.textures(st)
                    crop = T[m[0]].crop((bx0, by0, bx1, by1)).convert("RGBA")
                    key = hashlib.md5(crop.tobytes() + bytes(str(crop.size), "ascii")).hexdigest()[:10]
                    nm = crops.get(key)
                    if nm is None:
                        nm = f"{m[0]}__{key}"
                        crops[key] = nm
                        images[nm] = crop
                    for P, U, n, tex, plit in polys:
                        c = _uv_clip(P, U, rect)
                        if c is None:
                            continue
                        cp, cu = c
                        off = np.asarray(n, float) * 0.0008
                        cp = [p + off for p in cp]
                        cu = [((u[0] - rect[0]) / (rect[2] - rect[0]), (u[1] - rect[1]) / (rect[3] - rect[1])) for u in cu]
                        extra_groups[(gname, gi)].append(("poly", (np.array(cp), np.array(cu), n, nm, plit)))
    return still_items, extra_groups, images, joints


def _mat_regions(op):
    """[name, u0, v0, u1, v1] for each region material of an operation (whole textures are left as they are)."""
    out = []
    for m in _all_mats(op):
        if isinstance(m, list) and len(m) == 5 and isinstance(m[1], (int, float)):
            out.append([m[0], float(m[1]), float(m[2]), float(m[3]), float(m[4])])
    return out


def _all_mats(op):
    k = op[0]
    idx = {"b": [7], "o": [13], "q": [13], "c": [9, 10]}.get(k, [])
    out = []
    for i in idx:
        v = op[i]
        if isinstance(v, dict):
            out += list(v.values())
        elif isinstance(v, list) and not (len(v) == 5 and isinstance(v[1], (int, float))):
            out += v
        else:
            out.append(v)
    return [m for m in out if m is not None]


def _swap(op, m, name):
    """The operation with region material m replaced by the whole of texture name (its crop)."""
    def sub(v):
        if isinstance(v, list) and len(v) == 5 and isinstance(v[1], (int, float)) and [v[0], float(v[1]), float(v[2]), float(v[3]),
                                                                                         float(v[4])] == m:
            u0, v0, u1, v1 = m[1:]
            return [name, 0.0 if u1 >= u0 else 1.0, 0.0 if v1 >= v0 else 1.0, 1.0 if u1 >= u0 else 0.0, 1.0 if v1 >= v0 else 0.0]
        if isinstance(v, dict):
            return {k: sub(x) for k, x in v.items()}
        if isinstance(v, list):
            return [sub(x) for x in v]
        return v
    k = op[0]
    o = list(op)
    for i in {"b": [7], "o": [13], "q": [13], "c": [9, 10]}.get(k, []):
        o[i] = sub(o[i])
    return o


# ------------------------------------------------------------------ compiling a site

def mirror_cell(c):
    """A cell between the generators' frame and the site's: the generators' z mirrored (their coordinates are drawn
    mirrored, as equipment's are, which is why their tags and stencils read the right way round), an involution."""
    return (c[0], c[1], -c[2] - 1)


def mirror_polys(polys):
    m = np.array([1.0, 1.0, -1.0])
    return [(np.asarray(pts) * m, uvs, np.asarray(n) * m, tex, lit) for pts, uvs, n, tex, lit in polys]


def mirror_vox(vox):
    out = SB.Voxels()
    for b, arr in vox.blocks.items():
        out.blocks[mirror_cell(b)] = arr[:, :, ::-1].copy()
    return out


def compile_site(site, out_dir=SITES_OUT, tex_dir=TEX_OUT, verbose=True):
    still, lit, groups = diff(site)
    items = [(o, False) for o in still] + [(o, True) for o in lit]
    items, tgroups, timages, joints = texture_states(site, items)
    for k, g in tgroups.items():
        groups.setdefault(k, []).extend(g)
    still = [o for o, l in items if not l]
    lit = [o for o, l in items if l]
    # static geometry by block (from here on in the site's frame: the generators' z mirrored)
    polys = [p for p in mirror_polys(SB.tessellate(still, lit)) if site.keep_poly(p)]
    blocks = {b: q for b, q in SB.bake(polys).items() if site.keep_cell(b)}
    # collision
    vox = SB.Voxels()
    for op in site.collision_ops(still):
        vox.op(op, min_thick=0.015, min_radius=0.015)
    site.collision_extra(vox)
    vox = mirror_vox(vox)
    solids = {b: vox.merged(b) for b in list(vox.blocks) if site.keep_cell(b)}
    solids = {b: v for b, v in solids.items() if v}
    # live groups: tessellated, collision of each
    variables = list(site.variables)
    for j in joints:
        members = [next(v for v in site.variables if v.name == m) for m in j["members"]]
        total = 1
        for v in members:
            total *= len(v.values)
        variables.append(Var(j["name"], [str(k) for k in range(total)], "joint"))
    joint_members = {j["name"]: j["members"] for j in joints}
    live_groups = []
    for v in variables:
        for i in range(len(v.values)):
            g = groups.get((v.name, i), [])
            ops_ = [(o, l) for o, l in g if not (isinstance(o, str) and o == "poly")]
            raw = [l for o, l in g if isinstance(o, str) and o == "poly"]
            gpolys = mirror_polys(SB.tessellate([o for o, l in ops_ if not l], [o for o, l in ops_ if l]) + raw)
            gv = SB.Voxels()
            for o, l in ops_:
                if not l:
                    gv.op(o, min_thick=0.015, min_radius=0.015)
            gv = mirror_vox(gv)
            boxes = []
            for b in gv.blocks:
                for bx in gv.merged(b):
                    boxes.append((b[0] + bx[0] / 16, b[1] + bx[1] / 16, b[2] + bx[2] / 16, b[0] + bx[3] / 16, b[1] + bx[4] / 16,
                                  b[2] + bx[5] / 16))
            live_groups.append((v.name, i, gpolys, boxes if site.group_solid(v.name) else []))
    # cells: structure, ladder, water
    flags = defaultdict(int)
    ladder = set()
    for a, b, out in site.ladders():
        a, b, out = np.asarray(a, float), np.asarray(b, float), np.asarray(out, float)
        L = np.linalg.norm(b - a)
        side = np.cross(out, (b - a) / L)
        for s in np.arange(0.0, L + 0.01, 0.2):
            p = a + (b - a) * (s / L)
            for o_ in (0.05, 0.35, 0.6):
                for t in (-0.25, 0.0, 0.25):
                    q = p + out * o_ + side * t
                    ladder.add(mirror_cell((math.floor(q[0]), math.floor(q[1]), math.floor(q[2]))))
    cells = set(blocks) | set(solids) | ladder
    anchor = mirror_cell(tuple(site.anchor()))
    if anchor in cells:
        print(f"  {site.key}: the anchor's cell {anchor} has geometry; it is left out", file=sys.stderr)
        cells.discard(anchor)
        blocks.pop(anchor, None)
        solids.pop(anchor, None)
    for c in cells:
        f = 0
        if site.water(mirror_cell(c)):
            f |= 1
        if c in ladder:
            f |= 2
        flags[c] = f
    # textures
    T = site.textures(site.base())
    T.update(timages)
    used = {p[3] for p in polys}
    for _, _, gp, _ in live_groups:
        used |= {p[3] for p in gp}
    missing = sorted(n for n in used if n not in T)
    if missing:
        raise SystemExit(f"{site.key}: textures missing: {missing}")
    # (the LED displays' glyph sheets too: the game draws their figures from them)
    sprites = sorted(used | set(site.extra_textures()) | {dsp["glyphs"] for dsp in site.displays()})
    layers = export_textures(T, tex_dir, sprites)
    # live data: variables, groups (quads in site coordinates), zones, displays
    def mz(b):
        return [round(float(c), 4) for c in (b[0], b[1], -b[5], b[3], b[4], -b[2])]
    displays = []
    for dsp in site.displays():
        dsp = dict(dsp)
        dsp["corners"] = [[c[0], c[1], -c[2]] for c in dsp["corners"]]
        displays.append(dsp)
    live = {"anchor": list(anchor), "variables": [dict({"name": v.name, "values": [str(x) for x in v.values], "kind": v.kind},
                                                      **({"members": joint_members[v.name]} if v.name in joint_members else {}))
                                                 for v in variables],
            "zones": [{"name": n, "box": mz(b)} for n, b in site.zones()],
            "displays": displays}
    if site.shell():
        live["shell"] = site.shell()
    live.update(getattr(site, "extra_live", {}))      # (what a site's game logic reads besides: a yard's sprays, its terminals)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{site.key}.bin.gz"
    size = write(path, blocks, solids, flags, cells, sprites, layers, live, live_groups)
    if verbose:
        nq = sum(len(v) for v in blocks.values())
        print(f"{site.key}: {len(cells)} cells, {nq} quads, {sum(len(v) for v in solids.values())} boxes, "
              f"{len(live_groups)} live groups ({sum(len(g[2]) for g in live_groups)} quads), {len(sprites)} textures, "
              f"{size / 1e6:.1f} MB raw, {path.stat().st_size / 1e6:.1f} MB")
    CELLS_OUT.mkdir(parents=True, exist_ok=True)
    import gzip
    with gzip.open(CELLS_OUT / f"{site.key}.json.gz", "wt", encoding="utf-8") as f:
        json.dump({"anchor": list(anchor), "cells": sorted([list(c) + [flags.get(c, 0)] for c in cells])}, f, separators=(",", ":"))
    return {"cells": cells, "flags": flags, "anchor": anchor, "blocks": blocks}


def light_spots(site, material="ih_led"):
    """Where a site's light fittings shine from (site coordinates): the middle of every lit operation of the base state
    drawn in the fittings' lit material (the game's light comes from light blocks put in under them)."""
    out = []
    for _, o, lit in run(site, site.base()):
        if not lit or material not in _mats(o):
            continue
        if o[0] == "q":
            P = np.array(o[1:13], float).reshape(4, 3)
            c = P.mean(0)
        elif o[0] in ("c", "r"):
            c = (np.array(o[1:4], float) + np.array(o[4:7], float)) / 2
        else:
            c = np.array(o[1:4], float)
        out.append((float(c[0]), float(c[1]), float(-c[2])))
    return out


def place_lights(part_cells, spots, inside, level=13):
    """Light blocks under light fittings: for each spot, the first cell below it (within three) that is no site cell
    and inside the building (inside(cell)). Returns {cell: state}."""
    out = {}
    for x, y, z in spots:
        c0 = (math.floor(x), math.floor(y), math.floor(z))
        for dy in range(0, 4):
            c = (c0[0], c0[1] - dy, c0[2])
            if c in part_cells or not inside(c):
                continue
            out[c] = f"minecraft:light[level={level},waterlogged=false]"
            break
    return out


def cells_of(key):
    """A compiled site's cells for its blueprint part: {"anchor": (x, y, z), "cells": set, "flags": {cell: flags}}."""
    import gzip
    with gzip.open(CELLS_OUT / f"{key}.json.gz", "rt", encoding="utf-8") as f:
        d = json.load(f)
    cells = {tuple(c[:3]) for c in d["cells"]}
    return {"anchor": tuple(d["anchor"]), "cells": cells, "flags": {tuple(c[:3]): c[3] for c in d["cells"]}}


def main():
    """Compiles every site (tools/ww_*.py) into the mod's assets and the cell lists the blueprints are made from."""
    import ww_circ
    import ww_intake
    import ww_tower
    sites = {"ct_medium": ww_tower.TowerSite, "lake_intake": ww_intake.IntakeSite, "cw_pump_house": ww_circ.CircSite}
    want = sys.argv[1:] or list(sites)
    for key in want:
        compile_site(sites[key]())


def write(path, blocks, solids, flags, cells, sprites, layers, live, live_groups):
    import gzip
    index = {name: i for i, name in enumerate(sprites)}
    out = bytearray(b"JSIT")
    out += struct.pack("<HH", 1, len(sprites))
    for name in sprites:
        e = name.encode()
        out += struct.pack("<H", len(e)) + e + struct.pack("<B", layers.get(name, 0))
    keys = sorted(cells)
    out += struct.pack("<I", len(keys))
    for b in keys:
        quads = blocks.get(b, [])
        boxes = solids.get(b, [])
        out += struct.pack("<hhhBIB", b[0], b[1], b[2], flags.get(b, 0), len(quads), len(boxes))
        for P, U, n, tex, lit in quads:
            out += struct.pack("<HBbbb", index[tex], 1 if lit else 0, *(SB._q(c, 127, -127, 127) for c in n))
            for p, u in zip(P, U):
                out += struct.pack("<hhhHH", SB._q(p[0], 2048, -32768, 32767), SB._q(p[1], 2048, -32768, 32767),
                                   SB._q(p[2], 2048, -32768, 32767), SB._q(u[0], 65535, 0, 65535), SB._q(u[1], 65535, 0, 65535))
        for bx in boxes:
            out += struct.pack("<6B", *bx)
    out += struct.pack("<I", len(live_groups))
    for name, i, polys, boxes in live_groups:
        quads = []
        for pts, uvs, n, tex, lit in polys:
            for QP, QU in SB.fan_quads(list(pts), list(uvs)):
                quads.append((QP, QU, n, tex, lit))
        e = name.encode()
        out += struct.pack("<H", len(e)) + e + struct.pack("<HII", i, len(quads), len(boxes))
        for P, U, n, tex, lit in quads:
            out += struct.pack("<HBbbb", index[tex], 1 if lit else 0, *(SB._q(c, 127, -127, 127) for c in n))
            for p, u in zip(P, U):
                out += struct.pack("<fffHH", float(p[0]), float(p[1]), float(p[2]), SB._q(u[0], 65535, 0, 65535), SB._q(u[1], 65535, 0, 65535))
        for bx in boxes:
            out += struct.pack("<6f", *bx)
    js = json.dumps(plain(live), separators=(",", ":")).encode()
    out += struct.pack("<I", len(js)) + js
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(bytes(out))
    return len(out)


if __name__ == "__main__":
    main()
