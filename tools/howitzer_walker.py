"""The Howitzer Walker (docs/features/howitzer-walker.md), after the owner's own render of 9 October 2026: a tall
two-legged artillery walker, a riveted olive hull carrying a big howitzer in an open cradle, three headlamps, bumper
rails round every edge, grab hoops on the roof, and reverse-jointed legs on coil-sprung shock absorbers over broad
hinged feet. Modelled first on its own (9 October 2026), then made playable when the owner asked on 13 October 2026:
"implement the Howitzer-Walker into the game using its animations and add functionality to it".

The pilot walks it as the Diesel Walker (its controls and fuel), holds use to lay and fire the howitzer (the walker
turns to face the target and the barrel elevates to the arc that lands a Heavy Shell on it), jumps with a crouch and a
spring, and presses attack to stomp. walker/HowitzerWalker.java holds the game side; client/HowitzerWalkerRenderer
draws the parts exported here (assets/jugcraft/howitzer_walker_quads.json) with the same curves the clips sample.
tools/check_mod_data.py keeps the numbers, joints and curves' constants the same as the Java.

- Hull: a riveted olive box notched at its front top left, where the gun's cradle lies; round bumper rails along
  every edge; a big pivot axle with hubs under its nose and an A-arm linkage on its right down to the hip; dark
  recessed side panels; a chrome rail on brackets under the nose; three chrome-ringed headlamps up the front's right
  and two small lamps low on its left, an amber indicator at the top right; two grab hoops on the roof, two handles
  on the right side, the pilot's hatch ring at the roof's rear left; a louvred vent on the back.
- Gun: a trunnion hub in a cradle of a floor and two walls, open on top; a breech block and cap behind, the barrel
  with a thick jacket at its root and a flared muzzle brake, recoil rods along the cradle. It sits elevated 25
  degrees and recoils when it fires.
- Waist: a riveted plate over the hips with an amber indicator, a centre block, and big hip hubs either side.
- Legs: a hip drum, a broad flat thigh plate angled back under its armour plate, a knee drum, a shin angled forward
  under a shin guard, a chrome shock absorber in a coil spring behind the thigh, an ankle housing and a broad foot
  with a hinge strip, an upturned toe plate in front and a heel behind.
- The blast: a cartoon star burst and three smoke puffs at the muzzle, for the "shoot" clip.

Units are pixels, the walker facing +z with the ground at y = 0. The whole walker is saved as a Blockbench project
(art/howitzer_walker/howitzer_walker.bbmodel): every box in groups with their joints as origins (the shins under the
thighs, the gun under the hull with the burst and puffs at its muzzle), the textures embedded, and two animation clips
sampled from the curves below: "walk" (a floaty, bouncy stride), "shoot" (recoil, a rocking hull, the burst and the
puffs) and "jump" (a crouch, a stretch off the ground, a tucked hop and a squashy, bouncy landing). `python tools/howitzer_walker.py --bbmodel --preview out.png` rewrites the project and renders previews.
"""
import json
import math
from pathlib import Path

import clean_metal
import item_icons
from steampunk_models import box, cyl

MOD = "jugcraft"
ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art" / "howitzer_walker"

# ------------------------------------------------------------------ the game (walker/HowitzerWalker.java)
# Movement and fuel are the Diesel Walker's. The howitzer fires Heavy Shells (artillery.py's HEAVY_SPEED and
# HEAVY_GRAVITY): ticks between shots, degrees the barrel elevates a tick, its limits (the cradle stays inside the hull's
# notch from -15 to 60 degrees: below -15 the breech pokes up through the roof) and its rest, and how far the pilot's
# look is traced for a target. The leap: its speed (about three blocks up) after a crouch of CROUCH_TICKS. The stomp: the
# hop, the slam's speed when it is already in the air, its damage, reach in blocks, throw and ticks between stomps.
GUN_COOLDOWN = 80
LAY_RATE = 3.0
MIN_ELEVATION = -15.0
MAX_ELEVATION = 60.0
REST_ELEVATION = 25.0
AIM_REACH = 250
LEAP = 0.7
CROUCH_TICKS = 6
STOMP_HOP = 0.45
SLAM = -1.2
STOMP_DAMAGE = 14
STOMP_RADIUS = 3.5
STOMP_KNOCKBACK = 1.2
STOMP_COOLDOWN = 40
HEALTH = 80
WIDTH = 2.75
HEIGHT = 5.4
# The roof hatch the pilot sits in, in pixels: the hatch ring's middle, a little over a block under the roof.
HATCH = (-8, 60.8, -7)

ITEMS = {"howitzer_walker": "Howitzer Walker"}
TOOLTIPS = {"howitzer_walker": "A tall artillery walker with a howitzer. Walk with the movement keys; look to aim "
                               "(your Range Finder mark takes priority); hold use to fire Heavy Shells; jump to "
                               "spring up; press attack to stomp. Refuel with a diesel or kerosene bucket."}
MESSAGES = {"on_mark": "Laying on your Range Finder mark",
            "out_of_range": "Out of the howitzer's range"}

PLATE, DARK, LEG = "hw_plate", "hw_plate_dark", "hw_leg"
FLASH, FLASH_RIM, SMOKE = "hw_flash", "hw_flash_rim", "hw_smoke"
GUNMETAL, CHROME, BORE, LAMP = "dp_gunmetal", "dp_chrome", "aw_bore", "aw_lamp"

# Joints, in pixels: the hips and the gun's trunnion.
HIPS = {"left": (11, 42, 0), "right": (-11, 42, 0)}
TRUNNION = (-7, 70, 4)
# The gun's rest pitch, degrees about x (negative is up), and how far it recoils along its barrel, in pixels.
ELEVATION = -25
RECOIL_STROKE = 6
# The leg's lengths and angles: the thigh back and the shin forward, degrees about x (vanilla's 22.5 steps).
THIGH, SHIN = 22, 20
THIGH_ANGLE, SHIN_ANGLE = 22.5, -22.5
# The animation (the Blockbench project's "walk" and "shoot" clips sample these curves; a renderer would draw the same).
# Walking: the legs swing at the hips and tuck at the knees; the hull bounces on them, squashing as it lands and
# stretching as it rises, swaying side to side, the gun wobbling a beat behind. Firing: the gun slams back and runs
# out past its rest with a wobble, the hull rocks back and squats, a cartoon star burst pops at the muzzle and smoke
# puffs roll out of it.
STRIDE_RATE = 2.0     # radians of gait a block walked
LEG_SWING = 24.0      # degrees a leg swings at the hip
KNEE_BEND = 20.0      # degrees a shin tucks as the leg swings forward
BOB = 2.5             # pixels the hull bounces a step
SQUASH = 0.06         # how much the hull stretches and squashes with the bounce
ROLL = 3.0            # degrees the hull sways side to side
PITCH = 2.0           # degrees the hull nods a step
GUN_WOBBLE = 6.0      # degrees the gun wobbles behind the hull's bounce
RECOIL_TICKS = 14.0   # ticks the gun takes to slam back and settle
KICK = 6.0            # degrees the hull rocks back on firing
SQUAT = 2.0           # pixels the hull squats on firing
FLASH_TICKS = 8.0     # ticks the star burst lasts
PUFF_TICKS = 24.0     # ticks the smoke puffs last


def turned(point, angle):
    """A point turned about x at the origin, right-handed as Minecraft turns elements."""
    rad = math.radians(angle)
    x, y, z = point
    return [x, y * math.cos(rad) - z * math.sin(rad), y * math.sin(rad) + z * math.cos(rad)]


KNEE = [round(v, 2) for v in turned((0, -THIGH, 0), THIGH_ANGLE)]
ANKLE = [round(KNEE[k] + v, 2) for k, v in enumerate(turned((0, -SHIN, 0), SHIN_ANGLE))]
# The barrel's mouth, from the trunnion: along the barrel's axis, pitched by ELEVATION.
BARREL_END = (0, 1.5, 36)
MUZZLE = tuple(round(TRUNNION[k] + v, 2) for k, v in enumerate(turned(BARREL_END, ELEVATION)))


# ------------------------------------------------------------------ parts

def hull():
    """The hull with its waist plate and hips, the notch that holds the gun's cradle, bumper rails along every edge,
    the front pivot axle with its hubs, the A-arm linkage, recessed side panels, the front rail, lamp housings, grab
    hoops and side handles, the hatch ring and the rear vent."""
    m = []
    # The waist: a riveted plate over the hips, a centre block up into the hull, and the hip axle through big hubs.
    m.append(box((-14, 38, -9), (14, 41, 9), {"*": PLATE, "up": DARK}))
    m.append(box((-8, 41, -7), (8, 46, 7), DARK))
    m += cyl("x", 42, 0, 2.2, -14, 14, GUNMETAL, DARK)
    for x0, x1 in ((-16.5, -14), (14, 16.5)):
        m += cyl("x", 42, 0, 4.5, x0, x1, GUNMETAL, CHROME)
    # The hull: a lower box, an upper rear box and an upper right box, leaving a notch at the front top left for the
    # gun's cradle; a bracket plate on the left closes the notch's open side round the trunnion.
    m.append(box((-16, 46, -14), (16, 66, 14), PLATE))
    m.append(box((-16, 66, -14), (16, 80, 0), PLATE))
    m.append(box((2, 66, 0), (16, 80, 14), PLATE))
    m.append(box((-17.5, 64, -2), (-16, 78, 10), {"*": PLATE, "west": DARK}))
    # Bumper rails round every edge: the vertical corners, the bottom and top edges, and the notch's edges.
    for x, z, y0, y1 in ((-16, -14, 46.5, 79.5), (16, -14, 46.5, 79.5), (16, 14, 46.5, 79.5), (-16, 14, 46.5, 65.5),
                         (2, 14, 66, 79.5)):
        m += cyl("y", x, z, 1.4, y0, y1, GUNMETAL)
    for y in (46, 80):
        m += cyl("x", y, -14, 1.4, -16, 16, GUNMETAL)
    m += cyl("x", 46, 14, 1.4, -16, 16, GUNMETAL)
    m += cyl("x", 80, 14, 1.4, 2, 16, GUNMETAL)
    m += cyl("x", 66, 14, 1.4, -16, 2, GUNMETAL)
    m += cyl("x", 80, 0, 1.4, -16, 2, GUNMETAL)
    for x, z0, z1 in ((-16, -14, 0), (16, -14, 14)):
        m += cyl("z", x, 80, 1.4, z0, z1, GUNMETAL)
    for x in (-16, 16):
        m += cyl("z", x, 46, 1.4, -14, 14, GUNMETAL)
    m += cyl("z", -16, 66, 1.4, 0, 14, GUNMETAL)
    m += cyl("z", 2, 80, 1.4, 0, 14, GUNMETAL)
    # The front pivot axle under the hull's nose, with big hubs at its ends.
    m += cyl("x", 49, 15.5, 2.8, -19, 19, GUNMETAL)
    for x0, x1 in ((-21.5, -19), (19, 21.5)):
        m += cyl("x", 49, 15.5, 4.2, x0, x1, GUNMETAL, CHROME)
    # The A-arm on the right: a hub on the hip and two bars up to a bracket on the hull's side.
    m += cyl("x", 44, -8, 2.6, 16.5, 20, GUNMETAL, CHROME)
    for angle in (45, -45):
        m.append(box((17.3, 44, -9), (19.3, 60, -7), DARK, ("x", angle, [18.3, 44, -8])))
    m.append(box((16, 56, -13), (19.5, 60, -3), DARK))
    # Dark recessed panels low on both sides, and the chrome rail on its brackets under the nose.
    m.append(box((16, 49, -10), (16.4, 63, 10), DARK))
    m.append(box((-16.4, 49, -10), (-16, 63, 10), DARK))
    for x in (-11, 11):
        m.append(box((x - 1, 54, 14), (x + 1, 57.5, 17.5), GUNMETAL))
    m += cyl("x", 56, 17, 1.1, -13, 13, CHROME)
    # Lamp housings: three chrome-ringed lamps up the front's right, two small ones low on its left.
    for y in (58, 64, 70):
        m += cyl("z", 11, y, 2.4, 14, 17.5, GUNMETAL)
        m += cyl("z", 11, y, 2.9, 17.5, 18.1, CHROME)
    for x in (-13, -9):
        m += cyl("z", x, 50, 1.5, 14, 16.5, GUNMETAL)
    # Grab hoops on the roof's right, two handles on the right side, and the hatch ring at the roof's rear left.
    for z in (-11, -3):
        for dz in (0, 5):
            m.append(box((10.5, 80, z + dz), (11.5, 84.5, z + dz + 1), GUNMETAL))
        m.append(box((10.5, 84.5, z), (11.5, 85.5, z + 6), GUNMETAL))
    for z in (-10, 2):
        for dz in (0, 5):
            m.append(box((16, 71, z + dz), (18.5, 72, z + dz + 1), GUNMETAL))
        m.append(box((17.5, 71, z), (18.5, 72, z + 6), GUNMETAL))
    m += cyl("y", -8, -7, 5, 80, 81.5, GUNMETAL, DARK)
    # The rear: a riveted plate and a louvred vent.
    m.append(box((-14, 48, -14.5), (14, 56, -14), PLATE))
    m.append(box((-7, 60, -15), (7, 74, -14), DARK))
    for y in (62, 65.5, 69):
        m.append(box((-6.5, y, -15.5), (6.5, y + 1.5, -15), GUNMETAL))
    return m


def lamps():
    """The lamps' glass, drawn at full brightness: three up the front's right, two low on its left, an amber
    indicator at the top right and one on the waist plate."""
    m = [box((9.3, y - 1.7, 17.5), (12.7, y + 1.7, 18.2), {"*": LAMP}) for y in (58, 64, 70)]
    m += [box((x - 1, 49, 16.5), (x + 1, 51, 16.7), {"*": LAMP}) for x in (-13, -9)]
    m.append(box((11.5, 76.5, 14), (14.5, 78, 14.4), {"*": LAMP}))
    m.append(box((-1.5, 41, 9), (1.5, 42, 9.4), {"*": LAMP}))
    return m


def gun():
    """The howitzer from its trunnion (TRUNNION), barrel along +z, lying in an open cradle: the renderer pitches it by
    ELEVATION and slides it back along the barrel when it fires."""
    m = []
    m += cyl("x", 0, 0, 3.4, -7, 9, GUNMETAL, CHROME)
    # The cradle: a floor and two walls, open on top, the barrel lying in the trough.
    m.append(box((-7, -6.5, -10), (7, -1, 14), DARK))
    m.append(box((-7, -1, -10), (-5.5, 6, 14), DARK))
    m.append(box((5.5, -1, -10), (7, 6, 14), DARK))
    m.append(box((-5, -5, -16), (5, 5, -10), GUNMETAL))
    m += cyl("z", 0, 1, 3.8, -19, -16, GUNMETAL)
    m += cyl("z", 0, 1.5, 4.6, -10, 30, GUNMETAL)
    m += cyl("z", 0, 1.5, 5.4, 0, 14, GUNMETAL)
    m += cyl("z", 0, 1.5, 5.6, 28.5, 34, GUNMETAL)
    m += cyl("z", 0, 1.5, 4.9, 34, 36, GUNMETAL, BORE)
    for x in (-6.2, 6.2):
        m += cyl("z", x, 1, 1.0, -8, 10, CHROME)
    return m


def thigh():
    """The leg's upper part below its hip: the hip drum, a broad flat thigh plate angled back under its armour plate,
    the shock absorber with its coil spring behind the thigh, and the knee drum."""
    m = []
    ky, kz = KNEE[1], KNEE[2]
    m += cyl("x", 0, 0, 4.2, -4, 4, GUNMETAL, DARK)
    m.append(box((-4.5, -THIGH, -1.5), (4.5, 0, 2), LEG, ("x", THIGH_ANGLE, [0, 0, 0])))
    m.append(box((-5, -THIGH + 2, 2), (5, -3, 3.2), {"*": LEG, "south": PLATE}, ("x", THIGH_ANGLE, [0, 0, 0])))
    # The shock absorber: a chrome rod in a coil spring behind the thigh, from a bracket on the thigh down to the knee.
    m.append(box((-1.5, -9, -13.5), (1.5, -5, -2), LEG))
    m.append(box((-1.5, -24, -13.5), (1.5, -20, -10), LEG))
    m += cyl("y", 0, -12, 2.2, -24, -21, LEG)
    m += cyl("y", 0, -12, 1.4, -21, -9, CHROME)
    for y in range(-21, -10, 2):
        m += cyl("y", 0, -12, 2.5, y + 0.5, y + 1.5, GUNMETAL)
    m += cyl("x", ky, kz, 3.4, -4.5, 4.5, GUNMETAL, DARK)
    return m


def shin():
    """The leg's lower part below its knee (KNEE from the hip): the shin angled forward under a shin guard, the ankle
    and its housing, and the broad foot with its hinge strip, upturned toe plate and heel."""
    m = []
    ay, az = ANKLE[1] - KNEE[1], ANKLE[2] - KNEE[2]
    m.append(box((-2.5, -SHIN, -2.5), (2.5, 0, 2.5), LEG, ("x", SHIN_ANGLE, [0, 0, 0])))
    m.append(box((-3.5, -SHIN + 1, 2.5), (3.5, -3, 3.7), {"*": LEG, "south": PLATE}, ("x", SHIN_ANGLE, [0, 0, 0])))
    m += cyl("x", ay, az, 3, -4, 4, GUNMETAL, DARK)
    m.append(box((-3.5, ay, az - 3.5), (3.5, ay + 4, az + 3.5), GUNMETAL))
    # The foot reaches forward from the ankle (the way the gun faces), as the owner set it in Blockbench on 9 October
    # 2026: a long plate from the ankle forward, its toe plate turning up at the front, a short heel behind the ankle
    # and the hinge strip across the plate.
    foot = ay - 3.2
    m.append(box((-6, foot, az), (6, foot + 3.5, az + 16), {"*": LEG, "up": DARK}))
    m.append(box((-5, foot + 0.5, az + 16), (5, foot + 3, az + 21), LEG, ("x", -22.5, [0, foot + 0.5, az + 16])))
    m.append(box((-5, foot, az - 4), (5, foot + 2.5, az), LEG))
    m.append(box((-6.2, foot + 3, az + 5), (6.2, foot + 4.5, az + 11), GUNMETAL))
    return m


def flash():
    """The cartoon star burst at the muzzle (from the barrel's mouth, +z forward), drawn at full brightness and popped
    up and spun by the renderer: a yellow four-point star with its turned copy, an orange rim star behind, and a
    tongue of flame along the barrel's line."""
    m = []
    # Each star's four bars sit at their own depth, so no two share a face plane.
    for w, h, tex, z0, z1 in ((10, 2.5, FLASH, 1, 4.2), (14, 1.6, FLASH_RIM, 0, 1.2)):
        for i, (angle, across) in enumerate(((0, False), (0, True), (45, False), (45, True))):
            cut = 0.15 * i
            lo, hi = (-h, -w) if across else (-w, -h)
            m.append(box((lo, hi, z0 + cut), (-lo, -hi, z1 - cut), tex, ("z", angle, [0, 0, (z0 + z1) / 2])))
    m += cyl("z", 0, 0, 3.5, 4, 10, FLASH)
    m += cyl("z", 0, 0, 2, 10, 16, FLASH)
    m += cyl("z", 0, 0, 1, 16, 22, FLASH_RIM)
    return m


def puff():
    """Three round smoke puffs at the muzzle, drawn at full brightness, grown, rolled forward and shrunk away by the
    renderer."""
    m = []
    for cx, cy, cz, r in ((0, 0, 0, 5), (-4, 3, 4, 3.5), (4, -2, 6, 3)):
        m += cyl("z", cx, cy, r, cz - r * 0.5, cz + r * 0.5, SMOKE)
        m += cyl("y", cx, cz, r * 0.75, cy - r * 0.8, cy + r * 0.8, SMOKE)
    return m


def parts():
    """The moving parts, each from its joint."""
    return {"hull": hull(), "lamps": lamps(), "gun": gun(), "thigh": thigh(), "shin": shin(), "flash": flash(),
            "puff": puff()}


def export():
    """The parts as quads for client/HowitzerWalkerRenderer, named howitzer_walker_<part>."""
    from zeppelin import tiled_quads
    return {f"howitzer_walker_{name}": tiled_quads(elements) for name, elements in parts().items()}


def shifted(item, offset, pose=None):
    """An element moved by `offset` (and, with `pose`, turned about the offset by that angle about x: a part at rest)."""
    frm, to, texture = item[:3]
    options = dict(item[3]) if len(item) > 3 else {}
    if "rotation" in options:
        axis, angle, origin = options["rotation"][:3]
        options["rotation"] = (axis, angle, [origin[k] + offset[k] for k in range(3)])
    elif pose:
        options["rotation"] = ("x", pose, list(offset))
    moved = ([frm[k] + offset[k] for k in range(3)], [to[k] + offset[k] for k in range(3)], texture)
    return moved + ((options,) if options else ())


def posed():
    """Every box of the walker at rest, in world pixels, for a preview: the gun pitched on its trunnion and a leg at
    each hip."""
    out = list(hull()) + list(lamps())
    out += [shifted(item, TRUNNION, ELEVATION) for item in gun()]
    for hip in HIPS.values():
        out += [shifted(item, hip) for item in thigh()]
        out += [shifted(item, [hip[k] + KNEE[k] for k in range(3)]) for item in shin()]
    return out


MUZZLE_LOCAL = [TRUNNION[k] + BARREL_END[k] for k in range(3)]


def groups():
    """The Blockbench project's groups: each moving part with its joint as origin and its rest pose as rotation, the
    shins under their thighs and the burst and puffs under the gun at its muzzle."""
    out = [("hull", (0, 0, 0), None, hull()), ("lamps", (0, 0, 0), None, lamps()),
           ("gun", TRUNNION, (ELEVATION, 0, 0), [shifted(item, TRUNNION) for item in gun()], "hull"),
           ("flash", MUZZLE_LOCAL, None, [shifted(item, MUZZLE_LOCAL) for item in flash()], "gun"),
           ("puff", MUZZLE_LOCAL, None, [shifted(item, MUZZLE_LOCAL) for item in puff()], "gun")]
    for side, hip in HIPS.items():
        knee = [hip[k] + KNEE[k] for k in range(3)]
        out.append((f"thigh_{side}", hip, None, [shifted(item, hip) for item in thigh()]))
        out.append((f"shin_{side}", knee, None, [shifted(item, knee) for item in shin()], f"thigh_{side}"))
    return out


# ------------------------------------------------------------------ animation curves (in ticks)

def recoil(t):
    """How far back the gun is, as a share of RECOIL_STROKE: a slam back, then it runs out past its rest and wobbles."""
    if t < 0 or t > 40:
        return 0.0
    if t < 2:
        return t / 2
    u = (t - 2) / (RECOIL_TICKS - 2)
    return math.exp(-u * 3) * math.cos(u * 4.5)


def kick(t):
    """The hull's pitch after firing, degrees: it rocks back, then bounces to rest."""
    return KICK * math.exp(-t / 8) * math.cos(t * 0.45) if 0 <= t < 40 else 0.0


def squat(t):
    return SQUAT * math.sin(math.pi * t / 12) if 0 <= t < 12 else 0.0


def flash_scale(t):
    """The star burst pops up in two ticks and shrinks away by FLASH_TICKS."""
    if t < 0 or t >= FLASH_TICKS:
        return 0.0
    return 1.3 * (t / 2 if t < 2 else 1 - (t - 2) / (FLASH_TICKS - 2))


def flash_spin(t):
    return 60 * t / FLASH_TICKS


def puff_scale(t):
    """The puffs grow quickly, drift, and shrink away in their last quarter."""
    if t < 0 or t >= PUFF_TICKS:
        return 0.0
    u = t / PUFF_TICKS
    grow = 0.3 + 1.3 * (1 - (1 - u) ** 2)
    return grow * (1 - max(0.0, (u - 0.75) / 0.25))


def puff_offset(t):
    """Where the puffs have rolled to, pixels along the barrel and up."""
    return 6 + 1.4 * t, 0.5 * t


def walk_pose(phase, gait=1.0):
    """The gait at `phase` radians: per leg (hip swing, knee bend) in degrees, the hull's (bob px, scale y, scale xz,
    roll, pitch) and the gun's wobble."""
    legs = []
    for sign in (1, -1):
        p = phase + (0 if sign > 0 else math.pi)
        legs.append((LEG_SWING * math.sin(p) * gait, KNEE_BEND * gait * max(0.0, math.sin(p + 0.7))))
    bob = BOB * gait * (0.5 - 0.5 * math.cos(2 * phase))
    stretch = SQUASH * gait * math.sin(2 * phase)
    hull_pose = (bob, 1 + stretch, 1 - stretch / 2, ROLL * gait * math.sin(phase), PITCH * gait * math.sin(2 * phase + 0.4))
    return legs, hull_pose, GUN_WOBBLE * gait * math.sin(2 * phase + 1.0)


JUMP_TICKS = 30


def jump_pose(t):
    """The jump at tick t: a crouch, a stretch off the ground, a tucked hop, a deep squashy landing and a bouncy
    settle. Returns (rise px, drop px, stretch, hull pitch, gun pitch, thigh angle, shin angle); the hips and the
    hull rise together, and the legs fold so the feet stay about where they were as the hips drop."""
    def smooth(u):
        u = max(0.0, min(1.0, u))
        return u * u * (3 - 2 * u)
    if t < 6:                       # crouch
        c = smooth(t / 6)
        return 0, 6 * c, -0.10 * c, 3 * c, -4 * c, 15 * c, 25 * c
    if t < 9:                       # launch: stretch up off the ground
        u = (t - 6) / 3
        return 0, 6 * (1 - u), 0.12 * u, -8 * u, 8 * u, 15 - 21 * u, 25 - 33 * u
    if t < 20:                      # in the air: a parabola, the legs tucked, nose up then over the top
        u = (t - 9) / 11
        rise = 30 * (1 - (2 * u - 1) ** 2)
        tuck = smooth(u * 2.5) * (1 - smooth((u - 0.7) / 0.3))
        return rise, 0, 0.04 * (1 - abs(2 * u - 1)), -8 + 14 * u, 8 - 14 * u, -6 + 14 * tuck, -8 + 24 * tuck
    if t < 24:                      # landing: a deep squash
        u = math.sin(math.pi * (t - 20) / 4)
        return 0, 8 * u, -0.15 * u, -4 * u, -6 * u, 20 * u, 32 * u
    u = (t - 24) / (JUMP_TICKS - 24)   # settle: a damped bounce back to rest
    w = math.exp(-u * 3) * math.cos(u * 6) * (1 - u)
    return 0, 2 * w, 0.05 * w, 2 * w, -3 * w, 5 * w, 8 * w


def animations():
    """The Blockbench project's clips, sampled from the curves above: "walk" (one stride a second, looping) and
    "shoot" (two seconds, once)."""
    import blockbench_export
    walk = {name: {"rotation": [], "position": [], "scale": []} for name in
            ("hull", "lamps", "gun", "thigh_left", "thigh_right", "shin_left", "shin_right", "flash", "puff")}
    steps = 16
    for i in range(steps + 1):
        time = i / steps
        legs, (bob, sy, sxz, roll, pitch), wobble = walk_pose(2 * math.pi * time)
        for name in ("hull", "lamps"):
            walk[name]["rotation"].append((time, (pitch, 0, roll)))
            walk[name]["position"].append((time, (0, bob, 0)))
            walk[name]["scale"].append((time, (sxz, sy, sxz)))
        # The gun rides the hull (its group is under the hull's), wobbling on its trunnion a beat behind the bounce.
        walk["gun"]["rotation"].append((time, (wobble, 0, 0)))
        walk["gun"]["position"].append((time, (0, -bob * 0.3, 0)))
        for side, (swing, bend) in zip(("left", "right"), legs):
            walk[f"thigh_{side}"]["rotation"].append((time, (swing, 0, 0)))
            walk[f"shin_{side}"]["rotation"].append((time, (bend, 0, 0)))
    for name in ("flash", "puff"):
        walk[name]["scale"].append((0, (0, 0, 0)))
    shoot = {name: {"rotation": [], "position": [], "scale": []} for name in ("hull", "lamps", "gun", "flash", "puff")}
    along = turned((0, 0, 1), ELEVATION)
    for i in range(49):
        t = i * 20 / 24  # ticks, 24 frames a second
        time = i / 24
        for name in ("hull", "lamps"):
            shoot[name]["rotation"].append((time, (kick(t), 0, 0)))
            shoot[name]["position"].append((time, (0, -squat(t), 0)))
        r = recoil(t) * RECOIL_STROKE
        shoot["gun"]["position"].append((time, (0, -r * along[1], -r * along[2])))
        fs = flash_scale(t)
        shoot["flash"]["scale"].append((time, (fs, fs, fs)))
        shoot["flash"]["rotation"].append((time, (0, 0, flash_spin(t))))
        ps = puff_scale(t)
        forward, up = puff_offset(t)
        shoot["puff"]["scale"].append((time, (ps, ps, ps)))
        shoot["puff"]["position"].append((time, (0, up, forward)))

    jump = {name: {"rotation": [], "position": [], "scale": []} for name in
            ("hull", "lamps", "gun", "thigh_left", "thigh_right", "shin_left", "shin_right", "flash", "puff")}
    for i in range(JUMP_TICKS + 1):
        t = i
        time = i / 20
        rise, drop, stretch, pitch, gun_pitch, thigh, shin = jump_pose(t)
        for name in ("hull", "lamps"):
            jump[name]["position"].append((time, (0, rise - drop, 0)))
            jump[name]["rotation"].append((time, (pitch, 0, 0)))
            jump[name]["scale"].append((time, (1 - stretch / 2, 1 + stretch, 1 - stretch / 2)))
        jump["gun"]["rotation"].append((time, (gun_pitch, 0, 0)))
        for side in ("left", "right"):
            jump[f"thigh_{side}"]["position"].append((time, (0, rise - drop, 0)))
            jump[f"thigh_{side}"]["rotation"].append((time, (thigh, 0, 0)))
            jump[f"shin_{side}"]["rotation"].append((time, (shin, 0, 0)))
    for name in ("flash", "puff"):
        jump[name]["scale"].append((0, (0, 0, 0)))

    def clean(tracks):
        return {g: {c: k for c, k in ch.items() if k} for g, ch in tracks.items()}
    return [blockbench_export.animation("walk", 1.0, clean(walk), loop=True),
            blockbench_export.animation("shoot", 2.0, clean(shoot), loop=False),
            blockbench_export.animation("jump", JUMP_TICKS / 20, clean(jump), loop=False)]


# ------------------------------------------------------------------ data

def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    for key, text in MESSAGES.items():
        lang[f"message.{MOD}.howitzer_walker.{key}"] = text
    lang[f"entity.{MOD}.howitzer_walker"] = "Howitzer Walker"
    # Written compact: it is thousands of quads.
    (assets / "howitzer_walker_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    # A Diesel Walker given a howitzer: two steel blocks for the barrel, a dispenser for its breech, steel plate for the
    # cradle and pistons for the coil-sprung legs. A branch beside the Armoured Walker, not an upgrade of it.
    write(data / "recipe" / "howitzer_walker.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["SDS", "PWP", "R R"],
        "key": {"S": f"{MOD}:steel_block", "D": "minecraft:dispenser", "P": "#c:plates/steel", "W": f"{MOD}:diesel_walker",
                "R": "minecraft:piston"},
        "result": {"id": f"{MOD}:howitzer_walker", "count": 1}})


def write_bbmodel(folder=ART):
    import blockbench_export
    return blockbench_export.write(folder / "howitzer_walker.bbmodel", "howitzer_walker", groups(),
                                   draw=lambda name: TEXTURES[name](), animations=animations())


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

OLIVE = [(40, 44, 34), (56, 62, 46), (74, 82, 62), (92, 100, 78), (112, 120, 96), (136, 144, 118)]
DARK_OLIVE = [(30, 34, 30), (42, 48, 42), (54, 60, 52), (66, 74, 64), (82, 90, 78), (100, 108, 96)]
BROWN = [(58, 54, 48), (74, 70, 62), (92, 86, 76), (112, 106, 94), (132, 126, 112), (150, 144, 130)]


def riveted(palette):
    """A riveted plate in the owner's olive: a bevelled panel with a rivet near each corner and one midway along each
    edge, kept quiet; no rust."""
    img = clean_metal.canvas(palette[2])
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13), (2, 7), (13, 7)):
        clean_metal.put(img, x, y, palette[-1])
        clean_metal.put(img, x + 1, y + 1, palette[1])
    clean_metal.rect(img, 4, 6, 11, 6, palette[3])
    return img


def plain(palette):
    """A plain dark plate: a bevelled panel with one brushed line, for recesses and the legs' limbs."""
    img = clean_metal.canvas(palette[2])
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    clean_metal.rect(img, 3, 9, 12, 9, palette[3])
    return img


def flat(colour, edge):
    """A flat cartoon colour with a one-pixel edge a shade off, for the burst and the puffs."""
    img = clean_metal.canvas(colour)
    for i in range(16):
        clean_metal.put(img, i, 0, edge)
        clean_metal.put(img, 0, i, edge)
    return img


TEXTURES = {PLATE: lambda: riveted(OLIVE), DARK: lambda: plain(DARK_OLIVE), LEG: lambda: plain(BROWN),
            FLASH: lambda: flat((255, 226, 90), (255, 244, 170)), FLASH_RIM: lambda: flat((240, 130, 40), (255, 170, 70)),
            SMOKE: lambda: flat((214, 214, 210), (236, 236, 232))}


def draw_all(save):
    for name, draw in TEXTURES.items():
        save(draw(), "block", name)
    for item in ITEMS:
        save(item_icons.draw(item), "item", item)


if __name__ == "__main__":
    import sys
    for name, elements in parts().items():
        print(f"{name}: {len(elements)} boxes")
    print(f"knee {KNEE}, ankle {ANKLE}, muzzle {MUZZLE}")
    if "--bbmodel" in sys.argv:
        print(write_bbmodel())
    if "--preview" in sys.argv:
        import box_preview
        for yaw, suffix in ((35, "front_left"), (-35, "front_right"), (-145, "back_right")):
            img = box_preview.render(posed(), scale=4, yaw=yaw, draw=lambda n: TEXTURES[n]())
            path = sys.argv[-1].replace(".png", f"_{suffix}.png")
            img.save(path)
            print(path)
