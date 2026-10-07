"""Checks the big guns', the Landship's and the Diesel Walker's moving parts against the parts they move past, over their
whole travel (tools/check_mod_data.py runs it).

On 5 October 2026 the owner found the big guns' barrels "see through" and their textures "conflicting", and the Landship's
barrel "flashing". Part of that was moving parts meeting their mounts: a face of a barrel sliding in the same plane as a
face of the deck or housing it swings past flickers, and a barrel swung through a roof or an engine cuts it. A per-part
check cannot see either, so this one poses the exported quads as the renderers do (client/TowerGunRenderer,
ArtilleryRenderers, LandshipRenderer and DieselWalkerRenderer): every few degrees of elevation and traverse, with each
barrel at rest, half way back and fully back in its recoil, and the walker's legs and arms over their whole swing. It
reports:
- any face of a moving part that lies in the same plane as an overlapping face of a part it moves past, facing the same
  way;
- a moving part sunk into another part's boxes by more than LIMIT of its volume, where it should never be (a barrel's
  breech inside its own gunhouse, like the Triple Battery's housing in its drum or the Landship's breech in its turret,
  is by design and not checked).
It also gives how far each gun reaches (reach()), for the culling boxes its renderer draws it within.
"""
import math

import numpy as np

from model_writer import unpack

LIMIT = 0.005


def _rx(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def _ry(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _rz(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


_FIXED = (np.eye(3), np.zeros(3))


def _kicked(m, t, kick):
    """A barrel's pose moved back along its own axis by its recoil, as the renderers translate it."""
    return m, t + m @ np.array([0.0, 0.0, -kick])


def _range(low, high, step):
    return sorted(set(list(np.arange(low, high, step)) + [high]))


def _gun_poses(pose, pitches, yaws, recoil, step):
    """Every (description, placement) of a gun: each traverse, each elevation and each stage of the recoil."""
    out = []
    for yaw in yaws:
        for pitch in _range(pitches[0], pitches[1], step):
            for kick in sorted({0.0, recoil / 2, float(recoil)}):
                out.append((f"at elevation {pitch:g}, traverse {yaw:g} and recoil {kick:g}", pose(pitch, yaw, kick)))
    return out


def _gun_checks(turret):
    """What a turntable gun's parts must keep clear of: the cradle and barrel of the turntable (unless the gun is a turret,
    like the Triple Battery, whose housing and barrels sit in its drum by design) and of the base, and the turntable of
    the base; the barrel slides inside its cradle by design, so of the cradle it need only keep clear of shared planes."""
    return [("cradle", "turntable", not turret), ("cradle", "base", True), ("barrel", "turntable", not turret),
            ("barrel", "base", True), ("barrel", "cradle", False), ("turntable", "base", False)]


def assemblies(artillery, tower_guns, landship, mech, step=5):
    """Every moving model: (name, {label: exported part}, [(moving label, other label, check sinking?)], poses), where each
    pose is (description, {label: (rotation, offset)}) in pixels from the model's feet."""
    out = []
    for gun, g in tower_guns.GUNS.items():
        table, trunnion = np.array(g["turntable"], float), np.array(g["trunnion"], float)

        def pose(pitch, yaw, kick, table=table, trunnion=trunnion):
            m = _ry(-yaw)
            elevated = (m @ _rx(-pitch), table + m @ (trunnion - table))
            return {"base": _FIXED, "turntable": (m, table), "cradle": elevated, "barrel": _kicked(*elevated, kick)}
        parts = {kind: f"{gun}_{kind}" for kind in ("base", "turntable", "cradle", "barrel")}
        out.append((gun, parts, _gun_checks(gun == "triple_battery"), _gun_poses(pose, g["pitch"], (0.0, 37.0), g["recoil"], step)))

    table, trunnion = np.array(artillery.MORTAR_TURNTABLE, float), np.array(artillery.MORTAR_TRUNNION, float)

    def mortar(pitch, yaw, kick):
        m = _ry(-yaw)
        elevated = (m @ _rx(-pitch), table + m @ (trunnion - table))
        return {"base": _FIXED, "turntable": (m, table), "cradle": elevated, "barrel": _kicked(*elevated, kick)}
    out.append(("siege_mortar", {"base": "mortar_base", "turntable": "mortar_turntable", "cradle": "mortar_cradle",
                                 "barrel": "mortar_barrel"}, _gun_checks(False),
                _gun_poses(mortar, artillery.MORTAR_PITCH, (0.0, 37.0), artillery.MORTAR_RECOIL, step)))

    gun = np.array(artillery.HOWITZER_GUN, float)
    arc = artillery.HOWITZER_ARC
    out.append(("self_propelled_howitzer", {"body": "howitzer_body", "cradle": "howitzer_gun", "barrel": "howitzer_barrel"},
                [("cradle", "body", True), ("barrel", "body", True), ("barrel", "cradle", False)],
                _gun_poses(lambda p, y, k: {"body": _FIXED, "cradle": (_ry(-y) @ _rx(-p), gun),
                                            "barrel": _kicked(_ry(-y) @ _rx(-p), gun, k)},
                           artillery.HOWITZER_PITCH, (-arc, -arc / 2, 0.0, arc / 2, arc), artillery.HOWITZER_RECOIL, step)))

    head = np.array(artillery.FLAK_HEAD, float)
    out.append(("flak_gun", {"mount": "flak_mount", "head": "flak_head", "barrels": "flak_barrels"},
                [("head", "mount", True), ("barrels", "mount", True), ("barrels", "head", False)],
                _gun_poses(lambda p, y, k: {"mount": _FIXED, "head": (_ry(-y) @ _rx(-p), head),
                                            "barrels": _kicked(_ry(-y) @ _rx(-p), head, k)},
                           artillery.FLAK_PITCH, (0.0, 30.0, 45.0, 60.0), artillery.FLAK_RECOIL, step)))

    # The Landship: the turret turns all the way round on the hull, and the barrel pitches (the renderer turns it by the
    # driver's view pitch, so up is negative) and recoils in the turret's mantlet.
    turret, pivot = np.array(landship.TURRET, float), np.array(landship.BARREL, float)
    poses = []
    for yaw in range(0, 360, 10):
        for pitch in _range(landship.BARREL_PITCH[0], landship.BARREL_PITCH[1], 7):
            for kick in (0.0, landship.RECOIL / 2, float(landship.RECOIL)):
                m = _ry(-yaw)
                poses.append((f"at turret yaw {yaw}, barrel pitch {pitch:g} and recoil {kick:g}",
                              {"body": _FIXED, "turret": (m, turret), "barrel": _kicked(m @ _rx(pitch), turret + m @ pivot, kick)}))
    out.append(("landship", {"body": "landship_body", "turret": "landship_turret", "barrel": "landship_barrel"},
                [("turret", "body", True), ("barrel", "body", True), ("barrel", "turret", False)], poses))

    # The Diesel Walker: the legs swing opposite ways, the fist arm follows them a little or punches forward, the drill
    # arm follows them or is raised to drill, and the bit spins in its housing.
    hips, shoulders = mech.HIPS, mech.SHOULDERS
    bit = np.array(mech.BIT, float)
    poses = []
    for swing in _range(-mech.LEG_SWING, mech.LEG_SWING, mech.LEG_SWING / 4):
        follow = swing * mech.ARM_FOLLOW
        for punch in (0.0, 0.5, 1.0):
            for drill, spin in ((follow, 0.0), (-mech.DRILL_RAISE, 0.0), (-mech.DRILL_RAISE, 22.5)):
                d = _rx(drill)
                poses.append((f"with the legs at {swing:g}, the punch {punch:g} of the way out and the drill arm at {drill:g}",
                              {"body": _FIXED, "core": _FIXED,
                               "left_leg": (_rx(swing), np.array(hips["left"], float)),
                               "right_leg": (_rx(-swing), np.array(hips["right"], float)),
                               "fist": (_rx(-punch * mech.PUNCH_SWING - follow), np.array(shoulders["fist"], float)),
                               "drill": (d, np.array(shoulders["drill"], float)),
                               "bit": (d @ _rz(spin), np.array(shoulders["drill"], float) + d @ bit)}))
    parts = {"body": "walker_body", "core": "walker_core", "left_leg": "walker_leg", "right_leg": "walker_leg",
             "fist": "walker_fist_arm", "drill": "walker_drill_arm", "bit": "walker_drill_bit"}
    # Each limb's top turns inside its socket in the body, by design; the limbs must stay clear of each other.
    checks = [(leg, other, False) for leg in ("left_leg", "right_leg") for other in ("body", "core")]
    checks += [("fist", "body", False), ("drill", "body", False), ("bit", "drill", False), ("left_leg", "right_leg", True),
               ("fist", "left_leg", True), ("drill", "right_leg", True)]
    out.append(("diesel_walker", parts, checks, poses))
    return out


def _arrays(quads):
    """A part's quads as arrays: their corners and normals."""
    return np.array([[p[:3] for p in q["vertices"]] for q in quads], float), np.array([q["normal"] for q in quads], float)


def _planes(arrays, m, t):
    """The quads posed, as arrays: unit normals, plane offsets, corners, and a key per quad for finding coplanar ones."""
    v = arrays[0] @ m.T + t
    n = arrays[1] @ m.T
    n /= np.linalg.norm(n, axis=1)[:, None]
    d = np.einsum("ij,ij->i", n, v[:, 0])
    return n, d, v, _keys(n, d)


def _keys(n, d, shift=0):
    """A number per quad that is the same for quads in about the same plane facing the same way."""
    k = np.round(n * 100).astype(np.int64) + 128
    return ((k[:, 0] * 256 + k[:, 1]) * 256 + k[:, 2]) * (1 << 32) + np.round(d * 20).astype(np.int64) + shift + (1 << 31)


def _overlap(n, a, b):
    """Overlap area of two coplanar convex quads, by clipping a against b in their plane."""
    h = np.array([1.0, 0, 0]) if abs(n[0]) < 0.9 else np.array([0, 1.0, 0])
    e1 = np.cross(n, h)
    e1 /= np.linalg.norm(e1)
    e2 = np.cross(n, e1)
    pa, pb = [tuple(p) for p in a @ np.array([e1, e2]).T], [tuple(p) for p in b @ np.array([e1, e2]).T]

    def area(poly):
        return 0.5 * sum(poly[i][0] * poly[i - 1][1] - poly[i - 1][0] * poly[i][1] for i in range(len(poly)))
    if area(pa) > 0:
        pa = pa[::-1]
    if area(pb) > 0:
        pb = pb[::-1]
    poly = pa
    for i in range(len(pb)):
        a0, a1 = pb[i - 1], pb[i]
        clipped = []
        for j in range(len(poly)):
            p, q = poly[j - 1], poly[j]
            side_p = (a1[0] - a0[0]) * (p[1] - a0[1]) - (a1[1] - a0[1]) * (p[0] - a0[0])
            side_q = (a1[0] - a0[0]) * (q[1] - a0[1]) - (a1[1] - a0[1]) * (q[0] - a0[0])
            if side_q <= 1e-9:
                if side_p > 1e-9:
                    clipped.append(_cut(p, q, side_p, side_q))
                clipped.append(q)
            elif side_p <= 1e-9:
                clipped.append(_cut(p, q, side_p, side_q))
        poly = clipped
        if len(poly) < 3:
            return 0.0
    return abs(area(poly))


def _cut(p, q, sp, sq):
    t = sp / (sp - sq)
    return (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)


def _coplanar(moving, fixed):
    """The area over which faces of `moving` lie in the same plane as faces of `fixed`, facing the same way."""
    n, d, v, keys = moving
    n2, d2, v2, keys2 = fixed
    area = 0.0
    near = np.isin(keys, keys2) | np.isin(keys + 1, keys2) | np.isin(keys - 1, keys2)
    lo2, hi2 = v2.min(axis=1), v2.max(axis=1)
    for i in np.nonzero(near)[0]:
        lo, hi = v[i].min(axis=0), v[i].max(axis=0)
        span = np.minimum(hi, hi2) - np.maximum(lo, lo2)
        touching = (np.abs(keys2 - keys[i]) <= 1) & np.all(span > -1e-4, axis=1) & ((span > 1e-4).sum(axis=1) >= 2)
        for j in np.nonzero(touching)[0]:
            if abs(d2[j] - d[i]) < 1e-3 and np.linalg.norm(n2[j] - n[i]) < 1e-3:
                area += _overlap(n[i], v[i], v2[j])
    return area


def _samples(elements):
    pts = []
    for e in elements:
        frm, to, _, options = unpack(e)
        if options.get("rotation"):
            continue
        f, t = np.array(frm, float), np.array(to, float)
        steps = np.maximum(1, np.ceil(t - f).astype(int))
        axes = [f[k] + (np.arange(steps[k]) + 0.5) * (t[k] - f[k]) / steps[k] for k in range(3)]
        pts.append(np.stack(np.meshgrid(*axes, indexing="ij"), -1).reshape(-1, 3))
    return np.concatenate(pts)


def _boxes(elements):
    lo, hi = [], []
    for e in elements:
        frm, to, _, options = unpack(e)
        if not options.get("rotation"):
            lo.append(frm)
            hi.append(to)
    return np.array(lo, float), np.array(hi, float)


def sources(artillery, tower_guns, landship, mech):
    """Every moving model's parts as generator elements, by exported part name."""
    out = {}
    for gun, fns in tower_guns.PARTS.items():
        for kind, fn in zip(("base", "turntable", "barrel"), fns):
            out[f"{gun}_{kind}"] = fn()
        out[f"{gun}_cradle"] = tower_guns.CRADLES[gun]()
    for name in ("mortar_base", "mortar_turntable", "mortar_cradle", "mortar_barrel", "howitzer_body", "howitzer_gun",
                 "howitzer_barrel", "flak_mount", "flak_head", "flak_barrels"):
        out[name] = getattr(artillery, name)()
    out.update({"landship_body": landship.body(), "landship_turret": landship.turret(), "landship_barrel": landship.barrel()})
    out.update({"walker_body": mech.body(), "walker_core": mech.core(), "walker_leg": mech.leg(),
                "walker_fist_arm": mech.fist_arm(), "walker_drill_arm": mech.drill_arm(), "walker_drill_bit": mech.drill_bit()})
    return out


def problems(artillery, tower_guns, landship, mech, quads, step=5):
    """Every coplanar or sunk pose, as text; `quads` maps exported part names to their quads."""
    source = sources(artillery, tower_guns, landship, mech)
    out = []
    for name, parts, checks, poses in assemblies(artillery, tower_guns, landship, mech, step):
        cache, samples, boxes = {}, {}, {}
        arrays = {part: _arrays(quads[part]) for part in set(parts.values())}

        def placed(label, m, t):
            key = (parts[label], m.round(6).tobytes(), t.round(6).tobytes())
            if key not in cache:
                cache[key] = _planes(arrays[parts[label]], m, t)
            return cache[key]
        for description, pose in poses:
            for a, b, sinking in checks:
                area = _coplanar(placed(a, *pose[a]), placed(b, *pose[b]))
                if area > 0.01:
                    out.append(f"{name}: {description}, {parts[a]} ({a}) shares a plane with {parts[b]} ({b}) over "
                               f"{area:.2f} px^2 (it flickers)")
                if not sinking:
                    continue
                if a not in samples:
                    samples[a] = _samples(source[parts[a]])
                if b not in boxes:
                    boxes[b] = _boxes(source[parts[b]])
                (am, at), (bm, bt) = pose[a], pose[b]
                local = ((samples[a] @ am.T + at) - bt) @ bm
                lo, hi = boxes[b]
                near = np.all((local > lo.min(axis=0)) & (local < hi.max(axis=0)), axis=1)
                points = local[near]
                if not len(points):
                    continue
                crossing = np.all((lo < points.max(axis=0)) & (hi > points.min(axis=0)), axis=1)
                lo, hi = lo[crossing] + 0.01, hi[crossing] - 0.01
                inside = np.all((points[:, None, :] > lo) & (points[:, None, :] < hi), axis=2).any(axis=1).sum() / len(local)
                if inside > LIMIT:
                    out.append(f"{name}: {description}, {inside:.1%} of {parts[a]} ({a}) is inside {parts[b]} ({b})")
    return out


def reach(artillery, tower_guns, landship, mech, quads, step=5):
    """How far each gun reaches over its whole travel, in pixels from its feet: {gun: (highest, lowest, furthest out)}."""
    out = {}
    for name, parts, _, poses in assemblies(artillery, tower_guns, landship, mech, step):
        top, bottom, radius = 0.0, 0.0, 0.0
        corners = {label: np.array([p[:3] for q in quads[part] for p in q["vertices"]], float) for label, part in parts.items()}
        for _, pose in poses:
            for label, (m, t) in pose.items():
                v = corners[label] @ m.T + t
                top, bottom = max(top, v[:, 1].max()), min(bottom, v[:, 1].min())
                radius = max(radius, float(np.hypot(v[:, 0], v[:, 2]).max()))
        out[name] = (top, bottom, radius)
    return out
