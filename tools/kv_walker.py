"""The KV-1/W walker, after the owner's own miniature of 14 October 2026: a KV-1 tank hull and turret carried on four
jointed spider legs. A model only, by the owner's choice: no entity, recipe, renderer or clips until the model is
approved.

- Hull: a long riveted olive box, its top front edge chamfered into a glacis and its top rear edge chamfered down to
  the engine deck; rivet rows along every edge; on the front a driver's hatch plate with a vision slit and a domed
  periscope bulb; a louvred engine deck behind the turret ring; a battery pack on the left side between the leg brackets; a
  riveted leg bracket with a hinge boss on each side at the front and rear, and a tow hook.
- Turret: a riveted box with its front corners chamfered, a bustle at the back, two round roof hatches (one with a
  lid dome and handle), four periscope blocks, a ribbed vent grille on the front right face, a lifting eye each side, the whip antenna on the roof.
- Gun: a mantlet block on the turret's front with the long barrel, a thick root and a muzzle collar, and the coaxial
  machine gun beside it, pivoting on the mantlet.
- Legs: four, insect-jointed, each in four groups so it can be animated later: a yaw group at the hinge (front legs
  splayed forward, rear legs back), a femur plate rising outward to a knee pin kept below the barrel's sweep, with a hydraulic hose
  along its top, a long tibia plate dropping outward to an ankle pin, and a flat foot pad hinged level on the ground.

Units are pixels, the walker facing +z with the ground at y = 0, about 50 wide across the hull and 100 across the
feet. `python tools/kv_walker.py --bbmodel --preview out.png` writes art/kv_walker/kv_walker.bbmodel and previews.
"""
import math
from pathlib import Path

import clean_metal
from steampunk_models import box, cyl

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art" / "kv_walker"

PLATE, DARK, LEG, RUST, GUNMETAL, CHROME, BORE, GRILLE, LAMP, FLASH, FLASH_RIM, SMOKE = (
    "kv_plate", "kv_plate_dark", "kv_leg", "kv_rust", "kv_gunmetal", "kv_chrome", "kv_bore", "kv_grille", "kv_lamp",
    "kv_flash", "kv_flash_rim", "kv_smoke")

# The hull's box, the turret ring, the gun's pivot on the mantlet.
HULL = ((-20, 7, -20), (20, 25, 20))
TURRET = (0, 26.5, 2)
GUN = (0, 33, 14)
# The legs: hinge pins on the hull's sides, the femur rising outward, the tibia dropping outward, the foot level.
HIPS = {"front_left": (20, 17, 20), "front_right": (-20, 17, 20), "rear_left": (20, 17, -20), "rear_right": (-20, 17, -20)}
FEMUR, TIBIA = 20, 24.5
FEMUR_RISE, TIBIA_LEAN, SPLAY = 25, 25, 45   # the knee pins top out below the barrel (y 31.4)   # degrees: the femur above level, the tibia outward from vertical, the yaw


def rot_z(v, a):
    a = math.radians(a)
    return [v[0] * math.cos(a) - v[1] * math.sin(a), v[0] * math.sin(a) + v[1] * math.cos(a), v[2]]


def rot_y(v, a):
    a = math.radians(a)
    return [v[0] * math.cos(a) + v[2] * math.sin(a), v[1], -v[0] * math.sin(a) + v[2] * math.cos(a)]


def leg_rests(name):
    """The rest rotations of a leg's four groups (yaw, femur, tibia, foot), signed for its side and end."""
    side = 1 if "left" in name else -1
    yaw = -SPLAY * side if "front" in name else SPLAY * side
    return {"yaw": (0, yaw, 0), "femur": (0, 0, FEMUR_RISE * side), "tibia": (0, 0, (TIBIA_LEAN - FEMUR_RISE) * side),
            "foot": (0, 0, -TIBIA_LEAN * side)}


def leg_joints(name):
    """A leg's hip, knee and ankle as Blockbench wants them: each child's origin in its parent's unturned frame (the
    knee straight out from the hip, the ankle straight down from the knee); the groups' rest rotations then swing
    them into place, the femur up and the tibia down and out."""
    side = 1 if "left" in name else -1
    hip = HIPS[name]
    knee = [hip[0] + FEMUR * side, hip[1], hip[2]]
    ankle = [knee[0], knee[1] - TIBIA, knee[2]]
    return hip, knee, ankle


def leg_world(name):
    """Where a leg's knee and ankle stand at rest, in world pixels."""
    side = 1 if "left" in name else -1
    hip = HIPS[name]
    yaw = leg_rests(name)["yaw"][1]
    knee = [hip[k] + v for k, v in enumerate(rot_y(rot_z((FEMUR * side, 0, 0), FEMUR_RISE * side), yaw))]
    ankle = [knee[k] + v for k, v in enumerate(rot_y(rot_z((0, -TIBIA, 0), TIBIA_LEAN * side), yaw))]
    return [round(v, 2) for v in knee], [round(v, 2) for v in ankle]


def rivets(points, tex=GUNMETAL, size=0.9):
    return [box((x - size / 2, y - size / 2, z - size / 2), (x + size / 2, y + size / 2, z + size / 2), tex) for x, y, z in points]


# ------------------------------------------------------------------ parts

def hull():
    (x0, y0, z0), (x1, y1, z1) = HULL
    m = []
    m.append(box((x0, y0, z0), (x1, y1, z1), PLATE))
    # The glacis: the top front edge chamfered, and the rear edge chamfered down to the deck.
    m.append(box((x0 + 0.5, y1 - 4.5, z1 - 4.5), (x1 - 0.5, y1 + 1.2, z1 + 1.2), PLATE, ("x", 45, [0, y1 - 1.6, z1 - 1.6])))
    m.append(box((x0 + 0.5, y1 - 4.5, z0 - 1.2), (x1 - 0.5, y1 + 1.2, z0 + 4.5), PLATE, ("x", -45, [0, y1 - 1.6, z0 + 1.6])))
    # Rivet rows along the edges: the side top edges, the front and rear lower edges, and down the front corners.
    m += rivets([(x, y1 - 1, z) for x in (x0 - 0.2, x1 + 0.2) for z in range(-17, 18, 5)])
    m += rivets([(x, y0 + 1.2, z) for x in (x0 - 0.2, x1 + 0.2) for z in range(-15, 16, 5)])
    m += rivets([(x, y, z1 + 0.2) for x in range(-18, 19, 6) for y in (y0 + 1.2, y1 - 6.5)])
    m += rivets([(x, y, z0 - 0.2) for x in range(-18, 19, 6) for y in (y0 + 1.2,)])
    m += rivets([(x, y, z1 + 0.2) for x in (x0 + 1.2, x1 - 1.2) for y in range(10, 22, 4)])
    # The front: the driver's hatch plate with its vision slit, the domed periscope bulb, a tow hook.
    m.append(box((8, 11, z1), (17, 19, z1 + 1.2), {"*": PLATE, "south": DARK}))
    m.append(box((9.5, 15.5, z1 + 1.2), (15.5, 17, z1 + 1.5), BORE))
    m += rivets([(16, 12, z1 + 1.4), (9, 12, z1 + 1.4), (16, 18, z1 + 1.4), (9, 18, z1 + 1.4)], size=0.7)
    m += cyl("z", 2, 15, 3.6, z1, z1 + 2, GUNMETAL, DARK)
    m += cyl("z", 2, 15, 2.6, z1 + 2, z1 + 3.4, CHROME, LAMP)
    m.append(box((-14, 9, z1), (-10, 12, z1 + 3), DARK))
    m.append(box((-13.2, 9.8, z1 + 3), (-10.8, 11.2, z1 + 4.2), GUNMETAL))
    # The deck behind the turret: a raised engine hatch with louvres, the exhaust elbow on its right, the antenna.
    m.append(box((-15, y1, -19), (15, y1 + 1.6, -8), {"*": PLATE, "up": DARK}))
    for z in (-17.5, -15, -12.5, -10):
        m.append(box((-13, y1 + 1.6, z - 0.7), (13, y1 + 2.2, z + 0.7), GUNMETAL))
    # The battery pack on the left side between the leg brackets: a cradle, two cells with their terminals, a
    # strap over them and a cable up into the hull.
    m.append(box((x0 - 3.8, 9, -4.5), (x0, 10.5, 4.5), DARK))
    for za, zb in ((-4.5, -0.6), (0.6, 3.5)):
        m.append(box((x0 - 3.4, 10.5, za), (x0 - 0.2, 18.5, zb), {"*": BORE, "up": DARK}))
        m.append(box((x0 - 3.2, 18.5, za + 1), (x0 - 1.0, 19.3, zb - 1), GRILLE))
        m += cyl("y", x0 - 2.6, (za + zb) / 2 - 0.9, 0.5, 19.3, 20.4, CHROME)
        m += cyl("y", x0 - 1.4, (za + zb) / 2 + 0.9, 0.5, 19.3, 20.4, RUST)
    m.append(box((x0 - 3.6, 13, -5.2), (x0 - 0.1, 14.4, 4.2), PLATE))
    m.append(box((x0 - 2.4, 20.4, -4.2), (x0 - 1.6, 21.2, 4.4), DARK))
    m.append(box((x0 - 2.4, 20.4, 4.4), (x0, 21.2, 5.2), DARK))
    # The turret ring, and a step round it.
    m += cyl("y", 0, 0, 14.5, y1, y1 + 1.5, GUNMETAL, DARK)
    # The leg brackets: a riveted plate across each corner, facing the leg's diagonal, carrying the hinge boss.
    for name, (hx, hy, hz) in HIPS.items():
        yaw = leg_rests(name)["yaw"][1]
        z0, z1 = (hz - 0.8, hz + 2.4) if hz > 0 else (hz - 2.4, hz + 0.8)
        turn = ("y", -yaw, [hx, hy, hz])
        m.append(box((hx - 5, hy - 6, z0), (hx + 5, hy + 4.5, z1), {"*": DARK, "south" if hz > 0 else "north": PLATE}, turn))
        m.append(box((hx - 3.5, hy + 4.5, z0 + 0.3), (hx + 3.5, hy + 6.5, z1 - 0.3), DARK, turn))
        face = z1 + 0.2 if hz > 0 else z0 - 0.2
        m += [box((x - 0.4, y - 0.4, face - 0.4), (x + 0.4, y + 0.4, face + 0.4), GUNMETAL, turn)
              for x in (hx - 4, hx + 4) for y in (hy - 5, hy + 3)]
    return m


def turret():
    """The turret from its ring (TURRET): the box with chamfered front corners, the bustle, hatches, periscopes, the
    vent grille and the lifting eyes."""
    m = []
    m.append(box((-13, 0, -10), (13, 11.5, 12), PLATE))
    m.append(box((-11, 0.5, -14), (11, 10, -10), PLATE))
    for x in (-13, 13):
        m.append(box((x - 2.6, 0.3, 9.4), (x + 2.6, 11.2, 14.6), PLATE, ("y", 45, [x, 0, 12])))
    # The roof: two round hatches, the left one's lid domed with a handle; four periscope blocks along the front.
    m += cyl("y", -6, -4, 4.4, 11.5, 12.6, GUNMETAL, DARK)
    m += cyl("y", -6, -4, 3.4, 12.6, 14, PLATE)
    m.append(box((-7.5, 14, -4.5), (-4.5, 14.8, -3.5), GUNMETAL))
    m += cyl("y", 6, 2, 4.4, 11.5, 12.6, GUNMETAL, DARK)
    m += cyl("y", 6, 2, 3.4, 12.6, 13.2, PLATE)
    for x in (-10, -3, 3, 10):
        m.append(box((x - 1.4, 11.5, 7), (x + 1.4, 14, 9.6), DARK))
        m.append(box((x - 1, 12.6, 9.6), (x + 1, 13.5, 10), BORE))
    m.append(box((-2, 11.5, -8), (2, 13, -6), DARK))
    m += cyl("y", 11, -8, 1.3, 11.5, 13, DARK)
    m.append(box((10.7, 13, -8.3), (11.3, 34, -7.7), GUNMETAL))
    # The vent grille on the front left face: a dark recess with raised ribs.
    m.append(box((6, 3, 12), (12.4, 9.5, 12.6), GRILLE))
    for y in (4, 5.6, 7.2, 8.8):
        m.append(box((6.4, y - 0.4, 12.6), (12, y + 0.4, 13.1), GUNMETAL))
    # Lifting eyes on the sides, rivets along the roof's edges and the bustle.
    for x in (-13, 13):
        m.append(box((x - 0.8 + (0 if x > 0 else 0), 8, -2), (x + 0.8, 9.2, 2), GUNMETAL))
        m.append(box((x - 0.8, 9.2, -2), (x + 0.8, 10.4, -0.8), GUNMETAL))
        m.append(box((x - 0.8, 9.2, 0.8), (x + 0.8, 10.4, 2), GUNMETAL))
    m += rivets([(x, 10.6, 12.2) for x in range(-11, 12, 4)] + [(x, 10.6, -10.2) for x in range(-11, 12, 4)]
                + [(13.2, 10.6, z) for z in range(-8, 10, 4)] + [(-13.2, 10.6, z) for z in range(-8, 10, 4)], size=0.8)
    m += rivets([(x, 9.4, -14.2) for x in (-9, -4, 1, 6)], size=0.8)
    return m


def gun():
    """The mantlet and barrel from the gun's pivot (GUN), the barrel along +z, and the coaxial machine gun."""
    m = []
    m.append(box((-4.5, -3.5, 0), (4.5, 3.5, 4), {"*": PLATE, "south": DARK}))
    m += cyl("z", 0, 0, 2.4, 4, 9, GUNMETAL)
    m += cyl("z", 0, 0, 1.6, 9, 38, GUNMETAL)
    m += cyl("z", 0, 0, 2.0, 38, 40.5, GUNMETAL, BORE)
    m += cyl("z", -6.5, 0.5, 1.0, 0, 5, GUNMETAL, BORE)
    m += rivets([(-3.6, -2.6, 4.2), (3.6, -2.6, 4.2), (-3.6, 2.6, 4.2), (3.6, 2.6, 4.2)], size=0.7)
    return m


MUZZLE = (0, 0, 40.5)


def flash():
    """A cartoon star burst at the muzzle (+z forward): two four-point stars and a tongue of flame, popped and spun
    by the fire clip, scaled to nothing at rest."""
    m = []
    for w, h, tex, z0, z1 in ((8, 2, FLASH, 1, 3.4), (11, 1.3, FLASH_RIM, 0, 1), (6, 1.4, FLASH, 3.6, 5)):
        for i, (angle, across) in enumerate(((0, False), (0, True), (45, False), (45, True))):
            cut = 0.12 * i
            lo, hi = (-h, -w) if across else (-w, -h)
            m.append(box((lo, hi, z0 + cut), (-lo, -hi, z1 - cut), tex, ("z", angle, [0, 0, (z0 + z1) / 2])))
    m += cyl("z", 0, 0, 2.6, 5, 10, FLASH)
    m += cyl("z", 0, 0, 1.4, 10, 15, FLASH)
    m += cyl("z", 0, 0, 0.7, 15, 19, FLASH_RIM)
    return m


def puff():
    m = []
    for cx, cy, cz, r in ((0, 0, 0, 4), (-3, 2.5, 3, 2.8), (3, -1.5, 5, 2.4)):
        m += cyl("z", cx, cy, r, cz - r * 0.5, cz + r * 0.5, SMOKE)
        m += cyl("y", cx, cz, r * 0.75, cy - r * 0.8, cy + r * 0.8, SMOKE)
    return m


def femur():
    """From the hinge pin, the left side's femur along +x: the hinge pin, a wide root plate, the long plate, rivets, the
    hose along its top and the knee pin at its end. Mirrored for the right."""
    m = []
    m += cyl("x", 0, 0, 2.8, 2.6, 4.6, GUNMETAL, CHROME)
    m.append(box((3.4, -5, -1.8), (9, 5, 1.8), LEG))
    m.append(box((9, -3.5, -1.4), (FEMUR + 2, 3.5, 1.4), LEG))
    m.append(box((4, -3, -2.2), (8, 3, -1.8), DARK))
    m += rivets([(x, y, -1.6) for x in (11, 15, 19) for y in (-2.2, 2.2)], size=0.7)
    m += rivets([(x, y, 1.6) for x in (11, 15, 19) for y in (-2.2, 2.2)], size=0.7)
    m.append(box((4, 3.5, -0.5), (FEMUR - 1, 4.3, 0.5), DARK))
    m.append(box((FEMUR - 1, 3.2, -0.5), (FEMUR - 0.2, 4.3, 0.5), DARK))
    m += cyl("z", FEMUR, 0, 3.2, -2.4, 2.4, GUNMETAL, CHROME)
    return m


def tibia():
    """From the knee pin, the tibia along -y: a knee bracket, the long plate narrowing to the ankle pin."""
    m = []
    m.append(box((-4.2, -7, -1.2), (4.2, 1.5, 1.2), LEG))
    m.append(box((-2.8, -TIBIA, -1.0), (2.8, -7, 1.0), LEG))
    m.append(box((-3.4, -TIBIA + 2, -1.1), (3.4, -TIBIA + 9, 1.1), DARK))
    m += rivets([(0, y, -1.3) for y in (-11, -16)] + [(0, y, 1.3) for y in (-11, -16)], size=0.7)
    m += cyl("z", 0, -TIBIA, 2.2, -2, 2, GUNMETAL, CHROME)
    return m


def foot():
    """From the ankle pin, the foot pad lying level: a flat plate reaching outward with a chamfered tip and a short
    heel, and the pad's hinge lug."""
    m = []
    m.append(box((-2.5, -3, -1.6), (2.5, 0.5, 1.6), LEG))
    m.append(box((-4, -3.2, -4.5), (11, -0.8, 4.5), {"*": LEG, "up": DARK}))
    m.append(box((11, -3.0, -3.6), (14, -1.0, 3.6), LEG, ("y", 0, [11, -2, 0])))
    m.append(box((-7, -3.0, -3.2), (-4, -1.2, 3.2), LEG))
    return m


def mirrored(elements):
    out = []
    for item in elements:
        frm, to, texture = item[:3]
        options = dict(item[3]) if len(item) > 3 else {}
        if "rotation" in options:
            axis, angle, origin = options["rotation"][:3]
            options["rotation"] = (axis, angle if axis == "x" else -angle, [-origin[0], origin[1], origin[2]])
        if isinstance(texture, dict):
            texture = {("east" if k == "west" else "west" if k == "east" else k): v for k, v in texture.items()}
        out.append(([-to[0], frm[1], frm[2]], [-frm[0], to[1], to[2]], texture) + ((options,) if options else ()))
    return out


def shifted(item, offset):
    frm, to, texture = item[:3]
    options = dict(item[3]) if len(item) > 3 else {}
    if "rotation" in options:
        axis, angle, origin = options["rotation"][:3]
        options["rotation"] = (axis, angle, [origin[k] + offset[k] for k in range(3)])
    moved = ([frm[k] + offset[k] for k in range(3)], [to[k] + offset[k] for k in range(3)], texture)
    return moved + ((options,) if options else ())


def groups():
    out = [("hull", (0, 0, 0), None, hull()),
           ("turret", TURRET, None, [shifted(i, TURRET) for i in turret()], "hull"),
           ("gun", GUN, None, [shifted(i, GUN) for i in gun()], "turret")]
    muzzle = [GUN[k] + MUZZLE[k] for k in range(3)]
    out.append(("flash", muzzle, None, [shifted(i, muzzle) for i in flash()], "gun"))
    out.append(("puff", muzzle, None, [shifted(i, muzzle) for i in puff()], "gun"))
    for name in HIPS:
        side = 1 if "left" in name else -1
        hip, knee, ankle = leg_joints(name)
        rests = leg_rests(name)
        parts = {"femur": femur(), "tibia": tibia(), "foot": foot()}
        if side < 0:
            parts = {k: mirrored(v) for k, v in parts.items()}
        out.append((f"leg_{name}", hip, rests["yaw"], [], "hull"))
        out.append((f"femur_{name}", hip, rests["femur"], [shifted(i, hip) for i in parts["femur"]], f"leg_{name}"))
        out.append((f"tibia_{name}", knee, rests["tibia"], [shifted(i, knee) for i in parts["tibia"]], f"femur_{name}"))
        out.append((f"foot_{name}", ankle, rests["foot"], [shifted(i, ankle) for i in parts["foot"]], f"tibia_{name}"))
    return out


def posed(pose=None):
    pose = pose or {}
    parts = groups()
    tree = {g[0]: g for g in parts}
    parents = {g[0]: (g[4] if len(g) > 4 else None) for g in parts}

    def chain(name):
        turns = []
        while name:
            g = tree[name]
            rx, ry, rz, dx, dy, dz = pose.get(name, tuple(g[2] or (0, 0, 0)) + (0, 0, 0))
            for axis, angle in (("z", rz), ("y", ry), ("x", rx)):
                if angle:
                    turns.append((axis, angle, list(g[1])))
            if dx or dy or dz:
                turns.append(("move", (dx, dy, dz)))
            name = parents[name]
        return turns

    out = []
    for g in parts:
        turns = chain(g[0])
        for item in g[3]:
            frm, to, texture = item[:3]
            options = dict(item[3]) if len(item) > 3 else {}
            options["rotations"] = turns
            out.append((frm, to, texture, options))
    return out


# ------------------------------------------------------------------ animation curves (ticks)

def smooth(u):
    u = max(0.0, min(1.0, u))
    return u * u * (3 - 2 * u)


def with_rest(pose):
    out = {}
    for g in groups():
        base = tuple(g[2] or (0, 0, 0)) + (0, 0, 0)
        delta = pose.get(g[0], (0, 0, 0, 0, 0, 0))
        out[g[0]] = tuple(base[i] + delta[i] for i in range(6))
    return out


def leg_delta(name, yaw=0.0, rise=0.0, tuck=0.0, foot=0.0):
    """Deltas for a leg's four groups: `yaw` sweeps the foot forward (positive) or back about the hinge, `rise`
    lifts the femur, `tuck` folds the tibia in under the knee, `foot` tips the pad's toe up."""
    side = 1 if "left" in name else -1
    return {f"leg_{name}": (0, -side * yaw, 0, 0, 0, 0), f"femur_{name}": (0, 0, side * rise, 0, 0, 0),
            f"tibia_{name}": (0, 0, -side * tuck, 0, 0, 0), f"foot_{name}": (0, 0, side * (tuck - rise + foot), 0, 0, 0)}


def crouch_rise(depth):
    """The femur's extra rise that lifts the ankle by `depth` pixels, so a hull dropped by that much keeps its feet
    on the ground."""
    return math.degrees(math.asin(min(1.0, math.sin(math.radians(FEMUR_RISE)) + depth / FEMUR))) - FEMUR_RISE


PHASES = {"front_left": 0.0, "rear_right": 0.0, "front_right": math.pi, "rear_left": math.pi}
FLASH_SCALE = (0, 0, 0)


def gait(t, period, swing, direction=None, turn=0.0, lift=14.0, tuck=20.0):
    """A trot: diagonal pairs alternate. A leg's foot sweeps back through its stance (the body moving forward, or
    turning when `turn` is set) and swings forward lifted. `direction` is +1 forward, -1 back; `turn` +1 left."""
    p = 2 * math.pi * t / period
    pose = {}
    for name, phase in PHASES.items():
        q = p + phase
        side = 1 if "left" in name else -1
        up = max(0.0, -math.sin(q))
        yaw = 0.0
        if direction:
            yaw = -direction * swing * math.cos(q)
        if turn:
            yaw += -turn * side * swing * math.cos(q)
        pose.update(leg_delta(name, yaw=yaw, rise=lift * up, tuck=tuck * up, foot=8 * up))
    return pose


def idle_pose(t, period=60):
    p = 2 * math.pi * t / period
    pose = {"hull": (0.4 * math.sin(p), 0, 0.3 * math.sin(p + 1), 0, 0.5 * math.sin(p), 0),
            "turret": (0, 7 * math.sin(p / 2), 0, 0, 0, 0), "gun": (-1.5 * math.sin(p / 2 + 0.7), 0, 0, 0, 0, 0),
            "flash": (0, 0, 0, 0, 0, 0)}
    for name in HIPS:
        pose.update(leg_delta(name, rise=0.8 * math.sin(p + PHASES[name]), tuck=0.8 * math.sin(p + PHASES[name])))
    return with_rest(pose), (1, 1, 1)


def walk_pose(t, period=24):
    p = 2 * math.pi * t / period
    pose = gait(t, period, swing=16, direction=1)
    pose["hull"] = (1.5 * math.sin(2 * p + 0.5), 0, 2 * math.sin(p), 0, 1.4 * (0.5 - 0.5 * math.cos(2 * p)), 0)
    pose["turret"] = (0, -1.5 * math.sin(p), 0, 0, 0, 0)
    pose["gun"] = (-1.2 * math.sin(2 * p - 0.6), 0, 0, 0, 0, 0)
    return with_rest(pose), (1, 1, 1)


def turn_pose(t, direction, period=32):
    p = 2 * math.pi * t / period
    pose = gait(t, period, swing=14, turn=direction, lift=12, tuck=16)
    pose["hull"] = (0, 0, -2 * direction * math.sin(p) ** 2, 0, 1.0 * (0.5 - 0.5 * math.cos(2 * p)), 0)
    pose["turret"] = (0, 3 * direction, 0, 0, 0, 0)
    return with_rest(pose), (1, 1, 1)


JUMP_TICKS = 40


def jump_pose(t):
    """A crouch, a spring off all four legs with the hull rising and the legs stretched then tucked under, a hang,
    and a heavy landing that squats and settles."""
    if t < 8:
        u = smooth(t / 8)
        depth, rise, stretch, tuck, pitch = 6 * u, 0, 0, 0, 2 * u
    elif t < 12:
        u = smooth((t - 8) / 4)
        depth, rise, stretch, tuck, pitch = 6 * (1 - u), 10 * u, -14 * u, 0, 2 - 8 * u
    elif t < 26:
        u = (t - 12) / 14
        fold = smooth(u * 2) * (1 - smooth((u - 0.6) / 0.4))
        depth, rise, stretch, tuck, pitch = 0, 10 + 16 * math.sin(math.pi * u), -14 * (1 - fold) + 22 * fold, 30 * fold, -6 + 10 * u
    elif t < 30:
        u = smooth((t - 26) / 4)
        depth, rise, stretch, tuck, pitch = 7 * u, 0, 22 * (1 - u), 30 * (1 - u), 4 - 1 * u
    else:
        u = (t - 30) / (JUMP_TICKS - 30)
        w = math.exp(-u * 3) * math.cos(u * 6)
        depth, rise, stretch, tuck, pitch = 7 * w * (1 - u), 0, 0, 0, 3 * w * (1 - u)
    pose = {"hull": (pitch, 0, 0, 0, rise - depth, 0), "turret": (0, 0, 0, 0, 0, 0)}
    extra = crouch_rise(depth)
    for name in HIPS:
        pose.update(leg_delta(name, rise=extra + stretch, tuck=tuck, foot=(0 if depth else 10 * (tuck > 0))))
    squash = 0.05 * depth / 7
    return with_rest(pose), (1 + squash, 1 - squash * 1.5, 1 + squash)


def climb_pose(t, period=40):
    """Climbing a step: the hull pitched nose-up and heaved forward, each front leg in turn reaching high and
    forward then hauling back, the rear legs pushing low."""
    p = 2 * math.pi * t / period
    pose = {"hull": (-14 + 2 * math.sin(2 * p), 0, 3 * math.sin(p), 0, 2 * (0.5 - 0.5 * math.cos(2 * p)), 0),
            "turret": (0, 0, 0, 0, 0, 0), "gun": (6, 0, 0, 0, 0, 0)}
    for name, phase in PHASES.items():
        q = p + phase
        up = max(0.0, -math.sin(q))
        if "front" in name:
            pose.update(leg_delta(name, yaw=-14 * math.cos(q), rise=18 + 26 * up, tuck=10 + 30 * up, foot=16 * up))
        else:
            pose.update(leg_delta(name, yaw=-12 * math.cos(q), rise=-12 + 10 * up, tuck=-6 + 14 * up, foot=6 * up))
    return with_rest(pose), (1, 1, 1)


FIRE_TICKS = 24


def fire_pose(t):
    """The gun slams back and runs out with a wobble, the burst pops and spins at the muzzle, the smoke rolls out,
    the hull rocks back and squats on its legs, and the turret shivers."""
    if t < 3:
        u = t / 3
        recoil, kick, depth = -5 * u, -5 * u, 2.5 * u
    elif t < 14:
        u = (t - 3) / 11
        w = math.exp(-u * 3) * math.cos(u * 7)
        recoil, kick, depth = -5 * (1 - smooth(u)) - 1.5 * w, -5 * w, 2.5 * (1 - smooth(u)) + 0.8 * w
    else:
        recoil, kick, depth = 0, 0, 0
    flash = (0, 0, 0) if t > 8 or t < 1 else tuple(v * (1.3 * math.sin(math.pi * (t - 1) / 7)) for v in (1, 1, 1))
    puff = (0, 0, 0) if t < 2 or t > 22 else tuple(v for v in (1, 1, 1))
    pu = max(0.0, min(1.0, (t - 2) / 20))
    puff_s = (0.3 + 1.4 * pu) * (1 - smooth((pu - 0.6) / 0.4)) if 2 <= t <= 22 else 0
    pose = {"hull": (kick, 0, 0, 0, -depth, 0), "turret": (0, 0.8 * kick, 0, 0, 0, 0), "gun": (kick * 0.6, 0, 0, 0, 0, recoil),
            "flash": (0, 0, 60 * t, 0, 0, 0), "puff": (0, 0, 0, 0, 1.5 * pu * 4, 6 * pu + 2 * pu * pu)}
    extra = crouch_rise(depth)
    for name in HIPS:
        pose.update(leg_delta(name, rise=extra))
    return with_rest(pose), (1, 1, 1), {"flash": flash, "puff": (puff_s, puff_s, puff_s)}


def animations():
    import blockbench_export
    rests = {g[0]: tuple(g[2] or (0, 0, 0)) for g in groups()}
    names = list(rests)

    def clip(name, length, frames, poses, loop):
        tracks = {n: {"rotation": [], "position": [], "scale": []} for n in names}
        for i in range(frames + 1):
            time = round(length * i / frames, 4)
            result = poses(i, frames)
            pose, scale = result[0], result[1]
            scales = result[2] if len(result) > 2 else {}
            for n in names:
                rx, ry, rz, dx, dy, dz = pose.get(n, rests[n] + (0, 0, 0))
                rest = rests[n]
                tracks[n]["rotation"].append((time, (rx - rest[0], ry - rest[1], rz - rest[2])))
                tracks[n]["position"].append((time, (dx, dy, dz)))
            tracks["hull"]["scale"].append((time, scale))
            for n in ("flash", "puff"):
                tracks[n]["scale"].append((time, scales.get(n, (0, 0, 0))))
        tracks = {g: {c: k for c, k in ch.items() if any(v != (0, 0, 0) and v != (1, 1, 1) for _, v in k)
                      or (g in ("flash", "puff") and c == "scale")} for g, ch in tracks.items()}
        return blockbench_export.animation(name, length, tracks, loop=loop)

    return [clip("idle", 3.0, 30, lambda i, n: idle_pose(60 * i / n), True),
            clip("walk", 1.2, 24, lambda i, n: walk_pose(24 * i / n), True),
            clip("turn_left", 1.6, 32, lambda i, n: turn_pose(32 * i / n, 1), True),
            clip("turn_right", 1.6, 32, lambda i, n: turn_pose(32 * i / n, -1), True),
            clip("jump", 2.0, 40, lambda i, n: jump_pose(JUMP_TICKS * i / n), False),
            clip("climb", 2.0, 40, lambda i, n: climb_pose(40 * i / n), True),
            clip("fire", 1.2, 24, lambda i, n: fire_pose(FIRE_TICKS * i / n), False)]


def write_bbmodel(folder=ART):
    import blockbench_export
    return blockbench_export.write(folder / "kv_walker.bbmodel", "kv_walker", groups(), draw=lambda name: TEXTURES[name](),
                                   animations=animations())


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

OLIVE = [(44, 50, 36), (62, 70, 48), (84, 94, 64), (104, 116, 80), (126, 138, 100), (150, 160, 124)]
DARK_OLIVE = [(30, 34, 28), (42, 48, 40), (56, 64, 50), (70, 78, 62), (86, 96, 76), (104, 114, 94)]
WORN = [(50, 54, 40), (68, 74, 52), (88, 96, 66), (106, 114, 80), (122, 130, 96), (140, 150, 114)]
RUSTY = [(66, 32, 16), (96, 48, 22), (132, 68, 30), (160, 90, 42), (186, 114, 58), (210, 142, 80)]
STEEL = [(36, 38, 42), (52, 54, 60), (70, 72, 80), (88, 90, 98), (108, 110, 118), (132, 134, 142)]
BRIGHT = [(96, 100, 108), (132, 136, 144), (166, 170, 178), (196, 200, 206), (220, 222, 228), (242, 244, 248)]


def riveted(palette):
    img = clean_metal.canvas(palette[2])
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13), (2, 7), (13, 7)):
        clean_metal.put(img, x, y, palette[-1])
        clean_metal.put(img, x + 1, y + 1, palette[1])
    clean_metal.rect(img, 4, 6, 11, 6, palette[3])
    return img


def plain(palette):
    img = clean_metal.canvas(palette[2])
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    clean_metal.rect(img, 3, 9, 12, 9, palette[3])
    return img


def worn(palette, rust):
    """A worn olive plate: bevelled, with a few rust specks and a scratch, for the legs."""
    img = plain(palette)
    for x, y in ((3, 4), (12, 11), (9, 3), (5, 12), (13, 6)):
        clean_metal.put(img, x, y, rust[3])
        clean_metal.put(img, x + 1, y, rust[2])
    clean_metal.rect(img, 6, 7, 10, 7, palette[4])
    return img


def grille(palette):
    img = clean_metal.canvas(palette[1])
    for y in (2, 6, 10, 14):
        clean_metal.rect(img, 1, y, 14, y, palette[3])
        clean_metal.rect(img, 1, y + 1, 14, y + 1, palette[0])
    return img


def flat(colour, edge):
    img = clean_metal.canvas(colour)
    for i in range(16):
        clean_metal.put(img, i, 0, edge)
        clean_metal.put(img, 0, i, edge)
    return img


TEXTURES = {PLATE: lambda: riveted(OLIVE), DARK: lambda: plain(DARK_OLIVE), LEG: lambda: worn(WORN, RUSTY),
            RUST: lambda: plain(RUSTY), GUNMETAL: lambda: plain(STEEL), CHROME: lambda: plain(BRIGHT),
            BORE: lambda: flat((18, 18, 20), (30, 30, 34)), GRILLE: lambda: grille(DARK_OLIVE),
            LAMP: lambda: flat((228, 220, 160), (250, 246, 200)),
            FLASH: lambda: flat((255, 226, 90), (255, 244, 170)), FLASH_RIM: lambda: flat((240, 130, 40), (255, 170, 70)),
            SMOKE: lambda: flat((214, 214, 210), (236, 236, 232))}


def draw_all(save):
    for name, draw in TEXTURES.items():
        save(draw(), "block", name)


if __name__ == "__main__":
    import sys
    print(sum(len(g[3]) for g in groups()), "boxes in", len(groups()), "groups")
    for name in HIPS:
        print(name, leg_world(name))
    if "--bbmodel" in sys.argv:
        print(write_bbmodel())
    if "--preview" in sys.argv:
        import box_preview
        draw = lambda n: TEXTURES[n]()
        out = sys.argv[-1]
        owners = [g[0] for g in groups() for _ in g[3]]

        def view(pose=None, burst=False):
            keep = [e for e, o in zip(posed(pose), owners) if burst or o not in ("flash", "puff")]
            return keep

        for yaw, pitch, suffix in ((35, 14, "front_left"), (-35, 14, "front_right"), (90, 8, "side"), (-145, 14, "back_right"), (0, 50, "top")):
            box_preview.render(view(), scale=4, yaw=yaw, pitch=pitch, draw=draw).save(out.replace(".png", f"_{suffix}.png"))
        from PIL import Image
        rows = ([walk_pose(t)[0] for t in (0, 6, 12, 18)], [turn_pose(t, 1)[0] for t in (0, 8, 16, 24)],
                [jump_pose(t)[0] for t in (7, 11, 19, 27)], [climb_pose(t)[0] for t in (0, 10, 20, 30)],
                [fire_pose(t)[0] for t in (1, 3, 6, 12)])
        frames = []
        for r, row in enumerate(rows):
            for pose in row:
                frames.append(box_preview.render(view(pose, burst=(r == 4)), scale=2, yaw=-35, pitch=14, draw=draw))
        w = max(f.width for f in frames)
        h = max(f.height for f in frames)
        sheet = Image.new("RGB", (4 * (w + 8), len(rows) * (h + 8)), (236, 238, 242))
        for i, f in enumerate(frames):
            sheet.paste(f, ((i % 4) * (w + 8) + (w - f.width) // 2, (i // 4) * (h + 8) + h - f.height))
        sheet.save(out.replace(".png", "_frames.png"))
        print(out)
