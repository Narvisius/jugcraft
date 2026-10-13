"""Smoke test of the 3D armor toolkit (armor_models, armor_paint, armor_preview): a small test armor, worn on every
bone, run through the exporter, the painter and the renderer, with checks that every rotation, mirror and UV comes
out as documented. Nothing here is registered or written into the mod.

    python3 tools/armor_smoke.py                          the checks, then renders in build/armor_preview/smoke/
    python3 tools/armor_smoke.py --no-render              the checks only
    python3 tools/armor_smoke.py --owner path/to/owner_design.png    also the side-by-side sheet

The test armor exists twice with the same geometry: "smoke_uv" paints every face with the `test` painter (one colour
per face, a black marker at the texture's top-left, the face's letter), so a render shows which face and which way up
each texture lands; "smoke_paint" uses the real painters in the steel palette, and again in bronze.
"""
import argparse
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import armor_models as am
import armor_paint
import armor_preview as pv
from armor_paint import P

ROOT = Path(__file__).resolve().parents[1]
OUT = pv.OUT / "smoke"


# ---------------------------------------------------------------- the test armor
def model(paint, metal="smoke"):
    """{item: {bone: [Part]}}: every helper, rotation kind, inflation, cutout and mirror. paint(role) -> spec."""
    head = [
        am.around("helm", "head", 1.0, y=(-9, -1), paint=paint("shell")),
        am.diamond("crest", (0, -7.5, -5.75), 6, 1, pitch=30, paint=paint("crest")),            # z roll, then x pitch
        *am.pair(am.box("fin_right", (-6.25, -10.5, -3.5), (1, 4, 2), pivot=(-5.75, -6.5, -2.5), rotation=(0, 0, -30),
                        paint=paint("fin"))),                                                   # Euler z, mirrored
        *am.pair(am.box("horn_right", (-5.5, -11.5, -2.5), (1, 3, 1), pivot=(-5, -9, -2), rotation=(-20, 25, 40),
                        paint=paint("fin"))),                                                   # Euler x, y and z
        am.box("visor", (-3, -4.5, -5.6), (6, 1, 0.5), inflate=0.25, paint=paint("gold")),     # inflated
        am.box("grille", (-2, -3.5, -5.5), (4, 2, 0.5), cutout=True, skip="back",                 # see-through texels
               paint=paint("grille")),
    ]
    body = [
        am.box("chest", (-3, -0.5, -3.25), (6, 7, 0.75), paint=paint("chest")),
        *am.chevron("chev", apex=(0, 8.5, -4.25), length=5.5, width=2, thickness=0.75, angle=40, pitch=15,
                    paint=paint("chev")),
        am.box("back", (-3, -0.5, 2.5), (6, 7, 0.75), paint=paint("chest")),
        am.box("hang", (-1, 2, 3.25), (2, 5, 0.5), pivot=(0, 2, 3.5), rotation=(30, 0, 0), paint=paint("fin")),
        am.box("yaw", (-3.5, 9, 3.5), (2, 3, 0.5), pivot=(-2.5, 9, 3.75), rotation=(0, 40, 0), paint=paint("fin")),
    ]
    arm = [
        *am.lames("paul_right", origin=(-6, -4, -3.25), size=(5.5, 1.5, 6.5), count=3, step=(0, 1.75, 0),
                  flare_step=8, edge="left", toward="bottom", paint=paint("paul")),
        *am.rivets("paul_rivet_right", start=(-5, -3.25, -3.25), step=(1.5, 0, 0), count=3, paint=paint("rivet")),
        am.around("vamb_right", "right_arm", 0.75, y=(4, 10.75), paint=paint("vamb")),
        am.hinge(am.box("cuff_right", (-4.75, 5, -2.5), (0.75, 5, 5), paint=paint("cuff")), "bottom", 20),
    ]
    waist = [am.around("belt", "body", 1.25, y=(8, 11), paint=paint("belt")),
             am.around("waist", "body", 1.15, y=(6.5, 8), skip="ends", paint=paint("under"))]
    skirt = [*am.lames("skirt_right", origin=(-3.25, 1, -3.25), size=(5, 3, 6.5), count=3, step=(-0.25, 3, 0),
                       grow=(0, 0, 0.5), skip={"top", "bottom", "left"}, paint=paint("skirt")),
             am.box("tasset_right", (-5, 1, -1.5), (0.75, 6, 3), pivot=(-4.625, 1, 0), rotation=(0, 0, 10),
                    paint=paint("fin"))]                                                         # Euler z on a leg
    boot = [am.span("boot_right", (-3.25, 9, -3.25), (1.75, 12.75, 3.25), skip="left", paint=paint("boot")),
            am.diamond("toe_right", (0, 10.5, -3.75), 2.5, 0.5, pitch=20, paint=paint("toe"))]
    return {
        f"{metal}_helmet": {"head": head},
        f"{metal}_chestplate": {"body": body, "right_arm": arm, "left_arm": am.mirror_all(arm)},
        f"{metal}_leggings": {"body": waist, "right_leg": skirt, "left_leg": am.mirror_all(skirt)},
        f"{metal}_boots": {"right_leg": boot, "left_leg": am.mirror_all(boot)},
    }


def test_paint(role):
    if role == "grille":
        return {"front": [P("test"), P("marks", rects=[(1, 1, 1, 1, "clear")], symmetric=True)], "*": P("test")}
    return P("test")


STEEL_ROLES = {
    "shell": {"front": [P("plate"), P("marks", rects=[(1, 3, 2, 1), (3, 4, 1, 2, "seam")], symmetric=True)],
              "sides": P("chevron", corner="tl"), "*": P("plate")},
    "crest": P("chevron", corner="bl"),
    "fin": P("plate", tone="light"),
    "gold": P("gold"),
    "grille": {"front": [P("solid", tone="mid"), P("marks", rects=[(1, 0, 1, 2, "clear")], symmetric=True)],
               "*": P("solid", tone="mid")},
    "chest": {"front": P("chevron", corner="v"), "*": P("plate")},
    "chev": P("lames", rows=1, tones=(("light", "mid"),), centre=None),
    "paul": {"top": P("plate", tone="light"), "*": [P("lames", rows=1, tones=(("light", "mid_light"),), centre=None)]},
    "rivet": P("solid", tone="light"),
    "vamb": {"sides": P("chevron", corner="in", bands=("mid_light", "dark", "seam"), border="mid_light"),
             "*": P("plate")},
    "cuff": [P("plate"), P("rivets", row="top", every=4, offset=1, tone="seam")],
    "belt": {"sides": [P("leather"), P("strap", holes=3, buckle="centre")], "*": P("leather")},
    "under": P("under"),
    "skirt": P("lames", rows=3, tones=(("mid_light", "dark"),), centre="in"),
    "boot": [P("plate", tone="mid"), P("rivets", row="middle", every=3, style="bolt", tone="light")],
    "toe": P("chevron", corner="bl", border=None),
}


def smoke_sets():
    uv = am.ArmorSet("smoke_uv", armor_paint.TEST, model(test_paint))
    steel = am.ArmorSet("smoke_steel", armor_paint.STEEL, model(STEEL_ROLES.get))
    bronze = am.ArmorSet("smoke_bronze", armor_paint.BRONZE, model(STEEL_ROLES.get))
    return uv, steel, bronze


# ---------------------------------------------------------------- checks
class Checks:
    def __init__(self):
        self.failed = 0

    def __call__(self, name, ok, detail=""):
        print(("PASS " if ok else "FAIL ") + name + ("" if ok or not detail else f": {detail}"))
        self.failed += 0 if ok else 1


def unflip(p):
    return np.array([-p[0], -p[1], p[2]], dtype=float)


def _np_axis(axis, deg):
    a = np.array(am.AXES[axis] if isinstance(axis, str) else axis, dtype=float)
    a = a / np.linalg.norm(a)
    k = np.array([[0, -a[2], a[1]], [a[2], 0, -a[0]], [-a[1], a[0], 0]])
    t = math.radians(deg)
    return np.eye(3) + math.sin(t) * k + (1 - math.cos(t)) * (k @ k)   # Rodrigues


def expected_corners(part):
    """The part's 8 placed corners, worked out independently from the documented rules (numpy, Rodrigues)."""
    g = part.inflate
    lo, hi = np.array(part.origin) - g, np.array(part.origin) + np.array(part.size) + g
    pts = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
    rx, ry, rz = part.rotation
    r = _np_axis("z", rz) @ _np_axis("y", ry) @ _np_axis("x", rx)
    pivot = np.array(part.pivot)
    pts = (pts - pivot) @ r.T + pivot
    for axis, deg, p in part.turns:
        pts = (pts - np.array(p)) @ _np_axis(axis, deg).T + np.array(p)
    return pts


def part_points(s, part):
    """{face: (4 unflipped points, 4 uvs in atlas texels)} as exported."""
    nets, atlas = am.layout(s)
    out = {}
    faces = [f for f in am.FACES if f not in part.skip]
    for face, q in zip(faces, am.part_quads(part, nets[part.key], atlas, s.texture, s.density)):
        out[face] = ([unflip(v[:3]) for v in q["vertices"]], [(v[3] * atlas[0], v[4] * atlas[1]) for v in q["vertices"]],
                     unflip(q["normal"]))
    return out


def find(s, name):
    for item, bone, part in s.parts():
        if part.name == name:
            return item, bone, part
    raise KeyError(name)


def check_quads(check, s):
    quads = set_json(s)
    bad, count = [], 0
    for key, qs in quads.items():
        for i, q in enumerate(qs):
            count += 1
            pts = [unflip(v[:3]) for v in q["vertices"]]
            n = unflip(q["normal"])
            geo = np.cross(pts[1] - pts[0], pts[2] - pts[0])
            ok = (abs(np.linalg.norm(n) - 1) < 1e-3 and float(geo @ n) > 0
                  and float(geo @ n) / np.linalg.norm(geo) > 0.999
                  and abs(float((pts[3] - pts[0]) @ n)) < 2e-3
                  and all(0 <= v[3] <= 1 and 0 <= v[4] <= 1 for v in q["vertices"]))
            if not ok:
                bad.append(f"{key}[{i}]")
    check(f"{s.name}: all {count} quads are wound counter-clockwise from outside, flat, unit normals, UVs in 0..1",
          not bad, ", ".join(bad[:5]))


def set_json(s):
    return json.loads(json.dumps(am.set_quads(s)))


def check_rotations(check, s):
    worst = 0.0
    for item, bone, part in s.parts():
        got = np.array([p for pts, _, _ in part_points(s, part).values() for p in pts])
        want = expected_corners(part)
        d = max(min(np.linalg.norm(want - p, axis=1)) for p in got)
        worst = max(worst, d)
    check(f"{s.name}: every exported vertex is a corner of its part placed by the documented rotation rules "
          f"(independent numpy check, worst {worst:.1e} px)", worst < 2e-3)

    def centre(name, face):
        pts = part_points(s, find(s, name)[2])[face][0]
        return sum(pts) / 4

    def normal(name, face):
        return part_points(s, find(s, name)[2])[face][2]

    hang = find(s, "hang")[2]
    bottom = centre("hang", "bottom")
    check("x turn: a positive x turn swings a hanging plate's bottom back (+z)",
          bottom[2] > hang.origin[2] + hang.size[2] and abs(bottom[2] - 3.5 - 5 * math.sin(math.radians(30))) < 0.3,
          f"bottom z {bottom[2]:.2f}")
    n = normal("yaw", "front")
    check("y turn: a positive y turn swings the front toward the model's right (-x)",
          np.allclose(n, [-math.sin(math.radians(40)), 0, -math.cos(math.radians(40))], atol=1e-3), f"{n}")
    tasset_top, tasset_bottom = centre("tasset_right", "top"), centre("tasset_right", "bottom")
    check("z turn: a positive z turn swings the bottom toward the model's right (-x)",
          tasset_bottom[0] < tasset_top[0] - 0.9, f"top x {tasset_top[0]:.2f}, bottom x {tasset_bottom[0]:.2f}")
    fin_top, fin_bottom = centre("fin_right", "top"), centre("fin_right", "bottom")
    check("Euler z on a pair: the right fin's top leans out (-x), its mirror's out (+x)",
          fin_top[0] < fin_bottom[0] and centre("fin_left", "top")[0] > centre("fin_left", "bottom")[0])
    crest = np.array([p for pts, _, _ in part_points(s, find(s, "crest")[2]).values() for p in pts])
    low, high = crest[np.argmax(crest[:, 1])], crest[np.argmin(crest[:, 1])]
    check("diamond: a corner points straight down, juts forward with positive pitch; the top corner leans back",
          abs(low[0]) < 1e-3 and abs(high[0]) < 1e-3 and low[2] < -5.75 < high[2], f"low {low}, high {high}")
    cuff_top, cuff_bottom = centre("cuff_right", "top"), centre("cuff_right", "bottom")
    check("hinge: the cuff hinged at its wrist end flares its elbow end outward (-x on the right arm, +x on the left)",
          cuff_top[0] < cuff_bottom[0] - 1.0 and centre("cuff_left", "top")[0] > centre("cuff_left", "bottom")[0] + 1.0,
          f"{cuff_top[0]:.2f} vs {cuff_bottom[0]:.2f}")
    lame0, lame2 = find(s, "paul_right_0")[2], find(s, "paul_right_2")[2]
    out0 = part_points(s, lame0)["right"][0]
    drop = centre("paul_right_2", "right")[1] - lame2.centre[1]
    want = (lame2.size[0]) * math.sin(math.radians(16))
    check("lames: the first lame unturned; the third droops 16 degrees at its outer end (toward the bottom)",
          np.ptp([p[1] for p in out0]) < 1.5 + 1e-6 and abs(drop - want) < 0.25, f"drop {drop:.2f}, want {want:.2f}")
    right, left = find(s, "chev_right")[2], find(s, "chev_left")[2]
    pts = np.array([p for pts, _, _ in part_points(s, right).values() for p in pts])
    apex = pts[np.argmax(pts[:, 1])]
    top = pts[np.argmin(pts[:, 1])]
    outer = pts[np.argmin(pts[:, 0])]
    check("chevron: bars meet at the apex (lowest point, x 0), rise outward at 40 degrees, and the apex juts forward "
          "with pitch", abs(apex[0]) < 1e-3 and abs(outer[0] + 5.5 * math.cos(math.radians(40))) < 0.05
          and outer[1] < apex[1] - 3 and apex[2] < top[2] - 0.5, f"apex {apex}, top {top}, outer {outer}")


def check_mirrors(check, s):
    pairs, bad = 0, []
    parts = {p.name: p for _, _, p in s.parts()}
    for name, part in parts.items():
        if not part.mirror:
            continue
        original = parts[part.net]
        pairs += 1
        a, b = part_points(s, original), part_points(s, part)
        if set(a) != set(b):
            bad.append(f"{name}: faces {sorted(b)} vs {sorted(a)}")
            continue
        for face in a:
            (pa, ua, na), (pb, ub, nb) = a[face], b[face]
            # every vertex of the copy is the reflection of a vertex of the original, carrying the same texel
            for p, uv in zip(pb, ub):
                hits = [i for i, q in enumerate(pa) if np.allclose([-q[0], q[1], q[2]], p, atol=1e-3)]
                if not hits or not np.allclose(ua[hits[0]], uv, atol=1e-3):
                    bad.append(f"{name} {face}")
                    break
            if not np.allclose([-na[0], na[1], na[2]], nb, atol=1e-3):
                bad.append(f"{name} {face} normal")
    check(f"{s.name}: {pairs} mirrored parts are exact reflections showing the same texels (texture mirrored)",
          pairs > 0 and not bad, ", ".join(bad[:5]))


def check_atlas(check, s):
    nets, (w, h) = am.layout(s)
    taken = np.zeros((h, w), dtype=int)
    for key, (u, v, nw, nh, d) in nets.items():
        taken[v:v + d + nh, u:u + 2 * (d + nw)] += 1
    image = armor_paint.paint_atlas(s)
    again = armor_paint.paint_atlas(s)
    check(f"{s.name}: {len(nets)} nets fit a {w}x{h} atlas without overlapping", taken.max() == 1)
    problems = armor_paint.atlas_problems(s, image)
    check(f"{s.name}: every drawn face's texels are opaque (cutout parts excepted)", not problems, "; ".join(problems[:3]))
    check(f"{s.name}: painting is deterministic", image.tobytes() == again.tobytes())
    check(f"{s.name}: export is deterministic", json.dumps(am.set_quads(s)) == json.dumps(am.set_quads(s)))
    grille = find(s, "grille")[2]
    u, v, nw, nh, d = nets[grille.key]
    x0, y0, x1, y1 = am.face_rects(nw, nh, d)["front"]
    alpha = np.asarray(image)[v + y0:v + y1, u + x0:u + x1, 3]
    check(f"{s.name}: the cutout grille keeps see-through texels in its front face", (alpha == 0).any())


def check_problems(check):
    T = armor_paint.TEST
    bad = am.ArmorSet("smoke_bad", T, {
        "bad_chestplate": {"body": [am.box("a", (-4, 0, -3), (8, 4, 1)), am.box("b", (-2, 1, -3.05), (4, 2, 0.5)),
                                    am.box("c", (-4.5, 5, -2.5), (9, 3, 0.25)),
                                    am.box("d", (-4.5, 9, -2.25), (9, 2, 0.25)),
                                    am.box("e", (-3, 0.5, -4), (6, 4, 0.5), pivot=(0, 2.5, -3.75), rotation=(20, 0, 0)),
                                    am.box("f", (-3, 1.5, -4.05), (6, 2, 0.5), pivot=(0, 2.5, -3.8), rotation=(20, 0, 0))]},
        "bad_leggings": {"right_leg": [am.span("seam_right", (-2.5, 2, -3.25), (2.5, 5, -2.75)),
                                       am.span("roll_right", (-3.5, 6, -3.2), (2.1, 9, 3.2), pivot=(2.1, 6, 0),
                                               rotation=(0, 0, 8))],
                         "left_leg": [am.span("seam_left", (-2.5, 2, -3.25), (2.5, 5, -2.75))]},
        "bad_helmet": {"head": [am.around("shell", "head", 1), am.box("mesh", (-2, -4, -5.5), (4, 2, 0.5), cutout=True)]},
        "bad_boots": {"body": [am.around("band", "body", 0.75, y=(9, 10))],
                      "right_arm": [am.around("bracer_right", "right_arm", 0.75, y=(6, 8))]},
    })
    found = am.problems(bad)
    want = {"coplanar faces": "a front and b front are 0.05 px apart", "skin layer": "d front is 0.25 px out",
            "turned coplanar faces": "e front and f front are 0.05 px apart",
            "a rolled plate dipping into the leg": "roll_right left runs -0.32 to 0.10 px out from the body",
            "vanilla shell": "on vanilla armor's 0.5 shell", "leg seam": "seam_right front and seam_left front meet",
            "an arm against the body": "band front and bracer_right front meet in one plane standing",
            "a cutout touching a plate": "shell front and mesh back touch, and a cutout's"}
    for what, text in want.items():
        check(f"problems() finds {what}", any(text in line for line in found), "\n  ".join(found))
    check("problems() leaves the body's own 0.5 alone (only the head wears a 0.5 layer)",
          not any("c front" in line and "skin layer" in line for line in found), "\n  ".join(found))
    hat = am.problems(am.ArmorSet("smoke_hat", T, {"hat_helmet": {"head": [am.around("band", "head", 0.5, y=(-3, -2))]}}))
    check("problems() finds a face on the hat layer", any("band front is 0.50 px out" in line for line in hat),
          "\n  ".join(hat))
    # the same rolled plate, its inner side lined further out (as the knight's skirt): hidden, so not reported
    lined = am.problems(am.ArmorSet("smoke_lined", T, {"lined_leggings": {"right_leg": [
        am.span("roll_right", (-3.5, 6, -3.2), (2.1, 9, 3.2), pivot=(2.1, 6, 0), rotation=(0, 0, 8)),
        am.span("lining_right", (2.4, 5, -2.6), (2.5, 10, 2.6), skip="right")]}}))
    check("problems() accepts a face inside the skin layer that another plate hides", not lined, "\n  ".join(lined))
    # am.chevron's two bars share a plane where they cross above the apex (knight_armor.vee staggers one); the smoke
    # armor keeps them unstaggered, so the mirror checks see exact reflections, and expects just that report
    clean = [line for s in smoke_sets()[:1] for line in am.problems(s)]
    known = [line for line in clean if "chev_right" in line and "chev_left" in line]
    check("problems() finds the pitched chevron's two bars in one plane at the apex, and nothing else in the smoke "
          "armor", len(known) == 2 and len(clean) == 2, "\n  ".join(clean))
    many = am.ArmorSet("smoke_big", T, {"big_helmet": {"head": [am.box(f"b{i}", (0, -i, 0), (1, 1, 1))
                                                                 for i in range(40)]}})
    for what, s in (("over the quad cap", many),
                    ("an unknown bone", am.ArmorSet("x", T, {"x_helmet": {"tail": [am.box("t", (0, 0, 0), (1, 1, 1))]}})),
                    ("a repeated name", am.ArmorSet("x", T, {"x_helmet": {"head": [am.box("t", (0, 0, 0), (1, 1, 1))] * 2}})),
                    ("a shared net of another size",
                     am.ArmorSet("x", T, {"x_helmet": {"head": [am.box("t", (0, 0, 0), (1, 1, 1)),
                                                                am.box("u", (0, 0, 0), (2, 1, 1), net="t")]}}))):
        try:
            am.set_quads(s)
            check(f"the exporter refuses {what}", False)
        except ValueError as e:
            check(f"the exporter refuses {what}", True, str(e))


def sample(image, frame, scale, point, view):
    sx, sy, _ = pv.project(np.array([point]), view)
    x, y = (sx[0] - frame[0]) * scale, (frame[3] - sy[0]) * scale
    return tuple(int(c) for c in image.getpixel((int(x), int(y))))


def world_of(bone, point, pose="stand"):
    m = pv.bone_matrix(*pv.pose_bones(pose)[bone])
    p = m @ np.array([*point, 1.0])
    return (-p[0], 24 - p[1], p[2])


def check_render(check, s):
    """Renders smoke_uv unlit and reads the colours back: each face's top-left texel (the marker) must land at the
    top-left of the face as seen from outside, mirrored copies at the top-right; turned faces where the rules say."""
    textures = pv.Textures({s.texture: armor_paint.paint_atlas(s)})
    entries = set_json(s)
    scene = pv.world_quads(entries, "stand", textures)
    T = armor_paint.TEST
    mark = T["t_mark"]
    scale = 24
    views = {}

    def look(view):
        if view not in views:
            frame = pv.frame_of(scene, [view])
            views[view] = (pv.render(scene, view, scale, frame, unlit=True), frame)
        return views[view]

    def texel(bone, part_name, face, tx, ty, view):
        """Colour of texel (tx, ty) of an unturned part's face, found from geometry only (not from its UVs)."""
        part = find(s, part_name)[2]
        x0, y0, z0 = part.origin
        x1, y1, z1 = part.to
        w, h, d = am.net_size(part)
        fx, fy = (tx + 0.5) / {"front": w, "back": w, "right": d, "left": d, "top": w, "bottom": w}[face], (ty + 0.5)
        if face == "front":     # seen from the front: texture-left at the model's right (-x), top at -y
            p = (x0 + fx * (x1 - x0), y0 + fy / h * (y1 - y0), z0)
        elif face == "back":    # seen from behind: texture-left at the model's left (+x)
            p = (x1 - fx * (x1 - x0), y0 + fy / h * (y1 - y0), z1)
        elif face == "right":   # seen from the right: texture-left at the back
            p = (x0, y0 + fy / h * (y1 - y0), z1 - fx * (z1 - z0))
        elif face == "left":    # seen from the left: texture-left at the front
            p = (x1, y0 + fy / h * (y1 - y0), z0 + fx * (z1 - z0))
        elif face == "top":     # seen from above (back at the top of the picture): texture-left at the model's right
            p = (x0 + fx * (x1 - x0), y0, z1 - fy / d * (z1 - z0))
        else:                   # bottom, seen from below (front at the top): texture row 0 is the back, at the bottom
            p = (x0 + fx * (x1 - x0), y1, z1 - fy / d * (z1 - z0))
        image, frame = look(view)
        return sample(image, frame, scale, world_of(bone, p), view)

    cases = [("body", "chest", "front", "front"), ("body", "back", "back", "back"), ("head", "helm", "right", "right"),
             ("head", "helm", "left", "left"), ("head", "helm", "top", "top"),
             ("right_arm", "paul_right_0", "front", "front"), ("right_leg", "boot_right", "bottom", "bottom")]
    for bone, name, face, view in cases:
        part = find(s, name)[2]
        w, h, d = am.net_size(part)
        tw, th = {"right": (d, h), "left": (d, h), "top": (w, d), "bottom": (w, d)}.get(face, (w, h))
        tl, tr = texel(bone, name, face, 0, 0, view), texel(bone, name, face, tw - 1, 0, view)
        bl = texel(bone, name, face, 0, th - 1, view)
        where = ("at the back-right corner (vanilla's layout: row 0 is the back, seen from below the picture's bottom)"
                 if face == "bottom" else "at the top-left as seen from outside")
        check(f"render: {name} {face} face seen from {view}: its texture's top-left texel is {where}",
              tl == mark and tr != mark and bl != mark and T[f"t_{face}"] in (tr, bl),
              f"top-left {tl}, top-right {tr}, bottom-left {bl}")
    # Mirrored copies: the left pauldron's front shows the right one's front texture mirrored (marker at top-right);
    # its outer (+x) side shows the right one's outer (right) face, mirrored.
    for name, face, view, shown in (("paul_left_0", "front", "front", "front"), ("paul_left_0", "left", "left", "right")):
        part = find(s, name)[2]
        x0, y0, z0 = part.origin
        x1, y1, z1 = part.to
        w, h, d = am.net_size(part)
        image, frame = look(view)
        row = y0 + 0.5 / h * (y1 - y0)
        if face == "front":
            left, right = (x0 + 0.5 / w * (x1 - x0), row, z0), (x1 - 0.5 / w * (x1 - x0), row, z0)
        else:
            left, right = (x1, row, z0 + 0.5 / d * (z1 - z0)), (x1, row, z1 - 0.5 / d * (z1 - z0))
        tl = sample(image, frame, scale, world_of("left_arm", left), view)
        tr = sample(image, frame, scale, world_of("left_arm", right), view)
        check(f"render: mirrored {name} {face} side seen from {view} shows the {shown} texture mirrored "
              f"(marker at the top-right)", tr == mark and tl != mark, f"top-left {tl}, top-right {tr}")
    # A turned face: the crest's texture top-left texel ends at the diamond's left corner (the model's right).
    image, frame = look("front")
    corner = (-3 * math.sqrt(2) + 0.6, -7.75, -6.5)
    got = sample(image, frame, scale, world_of("head", corner), "front")
    check("render: the rolled and pitched crest's top-left texel is at its left corner", got == mark, f"{got}")
    # Cutout: through the grille's hole the helm's front shows.
    grille = find(s, "grille")[2]
    hole = (grille.origin[0] + 1.5, grille.origin[1] + 1.5, -6)
    got = sample(image, frame, scale, world_of("head", hole), "front")
    check("render: the grille's cut-out texel shows the helm behind it", got == T["t_front"], f"{got}")


def check_blockbench(check, sets):
    """The Blockbench bridge (tools/bbmodel.py): each smoke set written as a project and read back, both as raw entries
    and as a set of its own (load_set), gives the same quads and atlas; and a set read from a project exports again."""
    import bbmodel
    for s in sets:
        problems, exact = bbmodel.roundtrip(s)
        check(f"{s.name}: through a Blockbench project and back, the same quads and atlas", not problems,
              "; ".join(problems[:2]))
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "smoke.bbmodel"
        path.write_text(bbmodel.dumps(bbmodel.export(sets[0])), encoding="utf-8")
        loaded = bbmodel.load_set(sets[0].name, sets[0].palette, path, items=list(sets[0].pieces), texture=sets[0].texture)
        again = json.loads(json.dumps(am.set_quads(loaded)))
        problems, _ = bbmodel.roundtrip(loaded)
        check("a set read from a project exports again unchanged", not problems, "; ".join(problems[:2]))
        check("a set read from a project has the flicker and skin findings of the set it was written from",
              len(am.problems(loaded)) == len(am.problems(sets[0])),
              f"{len(am.problems(loaded))} against {len(am.problems(sets[0]))}")
        check("a set read from a project draws the same quads as the set it was written from",
              not bbmodel.compare(json.loads(json.dumps(am.set_quads(sets[0]))), again, cyclic=True))


def check_poses(check, s):
    textures = pv.Textures({s.texture: armor_paint.paint_atlas(s)})
    entries = {k: v for k, v in set_json(s).items()}

    def middle(pose, key, below=None):
        """Mean world position of the entry's quads (only those below bone y `below`: the forearm)."""
        quads = entries[key] if below is None else [q for q in entries[key]
                                                    if all(-v[1] > below for v in q["vertices"])]
        quads = pv.world_quads({key: quads}, pose, textures, body=False)
        return np.mean([q[0].mean(axis=0) for q in quads], axis=0)

    arm_r, arm_l = "smoke_chestplate_right_arm", "smoke_chestplate_left_arm"
    stand_r, walk_r = middle("stand", arm_r, 3.9), middle("walk", arm_r, 3.9)
    stand_l, walk_l = middle("stand", arm_l, 3.9), middle("walk", arm_l, 3.9)
    check("pose walk: the right forearm's parts swing forward (-z) and the left's back, with their arms",
          walk_r[2] < stand_r[2] - 0.5 and walk_l[2] > stand_l[2] + 0.5)
    stand_skirt, sneak_skirt = middle("stand", "smoke_leggings_right_leg"), middle("sneak", "smoke_leggings_right_leg")
    stand_back, sneak_back = middle("stand", "smoke_chestplate_body"), middle("sneak", "smoke_chestplate_body")
    check("pose sneak: leg parts move back 4 px with the legs; the torso's parts tilt with it",
          abs(sneak_skirt[2] - stand_skirt[2] - 4) < 0.2 and sneak_back[2] > stand_back[2] + 0.5)


def check_legacy(check):
    """The file the generator writes, rebuilt here exactly as generate_material_data.py builds it."""
    import exosuit
    import kinetic_rotors
    import tool_models
    flat = exosuit.worn_models(kinetic_rotors.quads)
    data = {"rocket_pack": kinetic_rotors.quads(tool_models.ITEMS["rocket_pack"]), **flat}
    data.update(am.entries(taken=data))
    text = json.dumps(data, indent=2) + "\n"
    current = pv.WORN.read_text(encoding="utf-8")
    check("worn_models.json rebuilt with the toolkit's entries is byte-identical to the committed file "
          f"({len(am.sets())} sets registered; exosuit keys unchanged)", text == current)


def save_atlas(s, out_dir, zoom=4):
    image = armor_paint.paint_atlas(s)
    path = out_dir / f"{s.name}_atlas.png"
    image.resize((image.width * zoom, image.height * zoom), Image.NEAREST).save(path)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--no-render", action="store_true")
    parser.add_argument("--owner", help="the owner's design image, for the side-by-side sheet")
    parser.add_argument("--out", type=Path, default=OUT)
    args = parser.parse_args()
    check = Checks()
    uv, steel, bronze = smoke_sets()
    for s in (uv, steel, bronze):
        check_quads(check, s)
    check_rotations(check, uv)
    check_mirrors(check, uv)
    for s in (uv, steel, bronze):
        check_atlas(check, s)
    check(f"bronze is a palette swap: same quads as steel but for the texture name",
          json.dumps(am.set_quads(steel)).replace(steel.texture, "T") ==
          json.dumps(am.set_quads(bronze)).replace(bronze.texture, "T"))
    check_problems(check)
    check_blockbench(check, [uv, steel])
    check_render(check, uv)
    check_poses(check, uv)
    check_legacy(check)
    for s in (uv, steel):
        print("\n".join(am.summary(s)))
    if not args.no_render:
        args.out.mkdir(parents=True, exist_ok=True)
        for s, unlit in ((uv, True), (steel, False), (bronze, False)):
            entries, atlases = pv.set_scene([s])
            for path in pv.render_all(s.name, entries, pv.Textures(atlases), args.out, unlit=unlit):
                print(path.relative_to(ROOT) if path.is_relative_to(ROOT) else path)
            print(save_atlas(s, args.out).relative_to(ROOT))
        if args.owner:
            entries, atlases = pv.set_scene([steel])
            print(pv.compare(entries, pv.Textures(atlases), args.owner, args.out / "smoke_steel_compare.png"))
    print(f"{check.failed} failed" if check.failed else "all checks passed")
    return 1 if check.failed else 0


if __name__ == "__main__":
    sys.exit(main())
