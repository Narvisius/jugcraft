"""Arms motion (batch 43, docs/features/arms-motion.md): keyframed attack, hold and guard animations for the arms of
batch 42, written to assets/jugcraft/arms_motion/<kind>.json and played on the client by client/arms/ArmsMotion.java.

Learned from the combat animation mods the owner sent (Better Combat, Malfu Combat Animation, Player Animation Library,
Fresh Moves; studied for their approach only, nothing of theirs is used):
- an animation is a few keyframes per bone, eased, not a frame per tick: smoothness comes from interpolation;
- every strike has anticipation (a short cock-back), the blow, then follow-through and a settle, and the whole body
  joins in: the torso twists, the head counter-turns, the feet step;
- two-handed arms are held in a guard with the off hand on the grip or haft;
- motion is layered: the keyframed pose on top, the walk cycle kept underneath on the legs.

What is done differently here, for fluid motion that stays cheap:
- keys are joined by cubic Hermite splines with Catmull-Rom tangents, so speed carries through a key instead of stopping
  at every key as ease-in-out does; a key's tension (0 to 1) flattens its tangent for a crisp stop where one is wanted;
- the torso bends at the waist (the model's body turns about its neck, so the neck and shoulders are moved to make the
  hinge), and the arms ride on the torso, their rotation composed with it;
- the clock is vanilla's own attack swing, read with the partial tick, so it is smooth at any frame rate and needs no
  networking: other players' swings already reach every client;
- clips are parsed once into flat float arrays and evaluated with no allocation; far players get the arms only.

Units: rotations in degrees, offsets in model pixels. Model space is Minecraft's: y down, the front is -z, the right
arm at -x. An arm's rx below 0 raises it forwards; a right arm's rz above 0 lifts it out to the side; ry above 0 swings
a raised arm, or turns the body, to the right.
"""
import json
import math

MOD = "jugcraft"

BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "item"]
CHANNELS = ["rx", "ry", "rz", "x", "y", "z"]
# Rest pivots (pixels): where vanilla's HumanoidModel puts each part.
REST = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
        "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}
WAIST = (0.0, 12.0, 0.0)
# Where the right hand holds an arm, in the right arm's space (pixels), from tools/arms.py's held poses through
# ItemInHandLayer; the blade runs from there towards -z.
GRIP = (-1.0, 8.56, -0.67)


# ---------------------------------------------------------------- authoring


def P(**bones):
    """A pose: bone -> (rx, ry, rz) or (rx, ry, rz, x, y, z). Bones left out stay at rest."""
    pose = {}
    for bone, values in bones.items():
        assert bone in BONES, bone
        values = tuple(values) + (0.0,) * (6 - len(values))
        pose[bone] = values
    return pose


def mix(a, b, f):
    """A pose part way from a to b."""
    out = {}
    for bone in set(a) | set(b):
        va = a.get(bone, (0.0,) * 6)
        vb = b.get(bone, (0.0,) * 6)
        out[bone] = tuple(x + (y - x) * f for x, y in zip(va, vb))
    return out


def add(a, b):
    out = dict(a)
    for bone, vb in b.items():
        va = out.get(bone, (0.0,) * 6)
        out[bone] = tuple(x + y for x, y in zip(va, vb))
    return out


class Clip:
    """Keys: (time 0..1, pose, tension 0..1). The first and last are the hold pose, so a clip starts and ends where
    the guard is and nothing snaps."""

    def __init__(self, name, keys):
        self.name = name
        self.keys = sorted(keys, key=lambda k: k[0])
        # A weapon art's clip (Arms V): its length in ticks, whether it holds its last pose until the art's next phase,
        # and the whole body's turn at each key (degrees), if it turns.
        self.ticks = None
        self.hold = False
        self.spin = None

    def bones(self):
        used = set()
        for _t, pose, _k in self.keys:
            used |= set(pose)
        return [b for b in BONES if b in used]


# ---------------------------------------------------------------- evaluation (the same maths as ArmsMotion.java)


def hermite(p0, p1, m0, m1, u):
    u2 = u * u
    u3 = u2 * u
    return (2 * u3 - 3 * u2 + 1) * p0 + (u3 - 2 * u2 + u) * m0 + (-2 * u3 + 3 * u2) * p1 + (u3 - u2) * m1


def evaluate(clip, t):
    """The clip's pose at time t: per channel, a cubic Hermite spline through the keys with Catmull-Rom tangents scaled
    by (1 - tension)."""
    keys = clip.keys
    t = min(max(t, keys[0][0]), keys[-1][0])
    i = 0
    while i < len(keys) - 2 and t > keys[i + 1][0]:
        i += 1
    t0, a, _ = keys[i]
    t1, b, _ = keys[i + 1]
    span = max(t1 - t0, 1e-6)
    u = (t - t0) / span
    out = {}
    for bone in clip.bones():
        vals = []
        for c in range(6):
            p0 = a.get(bone, (0.0,) * 6)[c]
            p1 = b.get(bone, (0.0,) * 6)[c]
            m0 = tangent(keys, i, bone, c) * span
            m1 = tangent(keys, i + 1, bone, c) * span
            vals.append(hermite(p0, p1, m0, m1, u))
        out[bone] = tuple(vals)
    return out


def tangent(keys, i, bone, c):
    """Catmull-Rom slope (value per unit time) at key i, flattened by its tension; zero at the ends."""
    if i == 0 or i == len(keys) - 1:
        return 0.0
    tp, pp, _ = keys[i - 1]
    tn, pn, _ = keys[i + 1]
    tension = keys[i][2]
    vp = pp.get(bone, (0.0,) * 6)[c]
    vn = pn.get(bone, (0.0,) * 6)[c]
    return (1.0 - tension) * (vn - vp) / max(tn - tp, 1e-6)


def _r(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return ((1, 0, 0), (0, c, -s), (0, s, c))
    if axis == "y":
        return ((c, 0, s), (0, 1, 0), (-s, 0, c))
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


def mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


def mv(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def zyx(rx, ry, rz):
    """ModelPart's rotation: Rz * Ry * Rx (degrees)."""
    return mul(mul(_r("z", rz), _r("y", ry)), _r("x", rx))


def euler_zyx(m):
    """Back from a matrix to ModelPart's (rx, ry, rz) in degrees."""
    sy = max(-1.0, min(1.0, -m[2][0]))
    ry = math.asin(sy)
    if abs(sy) < 0.9999:
        rx = math.atan2(m[2][1], m[2][2])
        rz = math.atan2(m[1][0], m[0][0])
    else:
        rx = math.atan2(-m[1][2], m[1][1])
        rz = 0.0
    return math.degrees(rx), math.degrees(ry), math.degrees(rz)


def skeleton(pose):
    """Each part's pivot and rotation matrix in model space: the torso bends at the waist and carries the head and
    arms; the legs stand at the hips."""
    parts = {}
    b = pose.get("body", (0.0,) * 6)
    rb = zyx(b[0], b[1], b[2])
    hinge = mv(rb, (0.0, -WAIST[1], 0.0))
    neck = (WAIST[0] + hinge[0] + b[3], WAIST[1] + hinge[1] + b[4], WAIST[2] + hinge[2] + b[5])
    parts["body"] = (neck, rb)
    h = pose.get("head", (0.0,) * 6)
    parts["head"] = ((neck[0] + h[3], neck[1] + h[4], neck[2] + h[5]), mul(rb, zyx(h[0], h[1], h[2])))
    for arm in ("right_arm", "left_arm"):
        a = pose.get(arm, (0.0,) * 6)
        shoulder = mv(rb, REST[arm])
        pivot = (neck[0] + shoulder[0] + a[3], neck[1] + shoulder[1] + a[4], neck[2] + shoulder[2] + a[5])
        parts[arm] = (pivot, mul(rb, zyx(a[0], a[1], a[2])))
    if "left_arm_abs" in pose:
        a = pose["left_arm_abs"]
        parts["left_arm"] = (parts["left_arm"][0], zyx(a[0], a[1], a[2]))
    for leg in ("right_leg", "left_leg"):
        g = pose.get(leg, (0.0,) * 6)
        rest = REST[leg]
        parts[leg] = ((rest[0] + g[3], rest[1] + g[4], rest[2] + g[5]), zyx(g[0], g[1], g[2]))
    return parts


# The item frame: ItemInHandLayer turns the hand by XP -90 then YP 180 and moves to the hand point; the item bone's
# rotation applies there (the game's PoseStack), so the preview applies it the same way. In that frame the blade runs
# along +y: an item rx above 0 tips the blade up (when the arm hangs), rz swings it sideways, ry spins it.
ITEM_FRAME = None
HAND = (-1.0, 10.0, -2.0)
BLADE_ARM = (0.0, 0.18, -1.0)


def _item_frame():
    global ITEM_FRAME
    if ITEM_FRAME is None:
        ITEM_FRAME = mul(_r("x", -90), _r("y", 180))
    return ITEM_FRAME


def transpose(m):
    return tuple(tuple(m[j][i] for j in range(3)) for i in range(3))


def blade_direction(pose):
    """The blade's direction in the right arm's space, after the item bone's wrist turn."""
    f = _item_frame()
    it = pose.get("item", (0.0,) * 6)
    wrist = zyx(it[0], it[1], it[2])
    d = mv(mul(mul(f, wrist), transpose(f)), BLADE_ARM)
    n = math.sqrt(sum(c * c for c in d))
    return tuple(c / n for c in d)


def weapon(pose, parts, length, grip_back=3.0):
    """The weapon in model space: (butt, grip, tip), held in the right hand with the item bone's wrist turn."""
    pivot, rot = parts["right_arm"]
    it = pose.get("item", (0.0,) * 6)
    hand = (HAND[0] + it[3], HAND[1] + it[4], HAND[2] + it[5])
    d = blade_direction(pose)
    grip = tuple(hand[i] + d[i] * 1.5 for i in range(3))
    tip = tuple(grip[i] + d[i] * length for i in range(3))
    butt = tuple(grip[i] - d[i] * grip_back for i in range(3))

    def world(p):
        q = mv(rot, p)
        return tuple(pivot[i] + q[i] for i in range(3))
    return world(butt), world(grip), world(tip)


def reach(parts, arm, target):
    """Two-handed grips: the off arm pointed from its shoulder at a point on the weapon (rz kept at 0). Returns
    (rx, ry) in degrees, absolute in model space."""
    shoulder = parts[arm][0]
    d = tuple(target[i] - shoulder[i] for i in range(3))
    n = math.sqrt(sum(c * c for c in d)) or 1.0
    dx, dy, dz = (c / n for c in d)
    s = -math.sqrt(max(0.0, 1.0 - dy * dy))
    rx = math.degrees(math.atan2(s, dy))
    ry = math.degrees(math.atan2(-dx, -dz)) if s < -1e-6 else 0.0
    return rx, ry


def two_handed(pose, length, spread):
    """The pose with the left hand on the weapon `spread` pixels below the right hand: the arm aimed at it from the
    left shoulder (ArmsMotion.java does the same each frame)."""
    parts = skeleton(pose)
    butt, grip, tip = weapon(pose, parts, length)
    d = tuple(tip[i] - grip[i] for i in range(3))
    n = math.sqrt(sum(c * c for c in d))
    target = tuple(grip[i] - d[i] / n * spread for i in range(3))
    rx, ry = reach(parts, "left_arm", target)
    out = dict(pose)
    old = pose.get("left_arm", (0.0,) * 6)
    out["left_arm_abs"] = (rx, ry, 0.0, old[3], old[4], old[5])
    return out


# ---------------------------------------------------------------- export


def clip_json(clip):
    bones = clip.bones()
    return {"name": clip.name, "bones": bones,
            "keys": [{"t": round(t, 4), "tension": round(k, 3),
                      "pose": {b: [round(v, 3) for v in pose.get(b, (0.0,) * 6)] for b in bones}}
                     for t, pose, k in clip.keys]}


def pose_json(pose):
    return {b: [round(v, 3) for v in pose[b]] for b in BONES if b in pose}


def flat(pose):
    """A pose as 42 numbers: the bones in BONES order, each rx, ry, rz, x, y, z."""
    out = []
    for bone in BONES:
        out.extend(round(v, 3) for v in pose.get(bone, (0.0,) * 6))
    return out


def mask(*poses):
    """Which bones a set of poses moves (bit i for BONES[i])."""
    m = 0
    for pose in poses:
        for bone in pose:
            if bone in BONES:
                m |= 1 << BONES.index(bone)
    return m


def kind_json(moves):
    """One kind's motion file: the hold and use poses, the attacks' keys, the two-handed grip, and which bones it
    moves (the spear's and lance's leave the arms to vanilla)."""
    hold = moves["hold"]
    clips = moves["attacks"]
    arts = moves.get("arts", [])
    use = moves.get("use")
    bones = mask(hold, *(pose for c in clips + arts for _t, pose, _k in c.keys), *([use] if use else []))
    if moves.get("body_only"):
        bones &= ~(mask({"right_arm": 0, "left_arm": 0, "item": 0}))
    fp_hold = moves.get("fp_hold", (0.0,) * 6)

    def fp_keys(c):
        return getattr(c, "fp", None) or [(0.0, fp_hold, 0.0), (1.0, fp_hold, 0.0)]

    def clip_entry(c):
        return {"name": c.name, "times": [round(t, 4) for t, _p, _k in c.keys],
                "tension": [round(k, 3) for _t, _p, k in c.keys],
                "keys": [flat(p) for _t, p, _k in c.keys],
                "fp_times": [round(t, 4) for t, _p, _k in fp_keys(c)],
                "fp_tension": [round(k, 3) for _t, _p, k in fp_keys(c)],
                "fp_keys": [[round(v, 3) for v in p] for _t, p, _k in fp_keys(c)]}
    out = {
        "bones": bones,
        "two_handed": moves.get("two_handed", 0),
        "hold": flat(hold),
        "use": flat(use) if use else None,
        # First person: the held arm's turn and offset on screen (rx, ry, rz, x, y, z); while it is used (a parry),
        # vanilla's own blocking pose takes over.
        "fp_hold": [round(v, 3) for v in fp_hold],
        "attacks": [clip_entry(c) for c in clips],
    }
    if arts:
        # A weapon art's phases (Arms V), played when the server says each began: its ticks, whether it holds its last
        # pose until the next phase, and the whole body's turn at each key.
        out["arts"] = [{**clip_entry(c), "ticks": c.ticks, "hold": c.hold,
                        "spin": [round(v, 3) for v in c.spin] if c.spin else None} for c in arts]
    return out


def write_all(write, assets):
    """assets/jugcraft/arms_motion/<kind>.json for every kind (tools/arms_moves.py)."""
    import arms_moves
    for kind, moves in arms_moves.MOVES.items():
        write(assets / "arms_motion" / f"{kind}.json", kind_json(moves))


def spline(keys, t):
    """A first-person track at time t: keys (time, (rx, ry, rz, x, y, z), tension) on the same splines as evaluate."""
    t = min(max(t, keys[0][0]), keys[-1][0])
    i = 0
    while i < len(keys) - 2 and t > keys[i + 1][0]:
        i += 1
    t0, a, _ = keys[i]
    t1, b, _ = keys[i + 1]
    span = max(t1 - t0, 1e-6)
    u = (t - t0) / span

    def slope(j, c):
        if j == 0 or j == len(keys) - 1:
            return 0.0
        return (1.0 - keys[j][2]) * (keys[j + 1][1][c] - keys[j - 1][1][c]) / max(keys[j + 1][0] - keys[j - 1][0], 1e-6)
    return tuple(hermite(a[c], b[c], slope(i, c) * span, slope(i + 1, c) * span, u) for c in range(6))


def evaluate_fp(clip, t):
    return spline(clip.fp, t)
