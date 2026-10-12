"""GeckoLib bodies for the Yeti King and his fight (docs/features/yeti-king.md): the King himself, his whelps, the blocks
of ice he hurls, the icicles his roar shakes from the vault and the glacial spikes he drives up through his lake.
Geometry and animations only, as plain dicts; no I/O. tools/yeti_king_art.py paints their sheets on the same box-UV
regions, at TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"). The builder and
the posing helpers are Vesperine's (tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one
block), y = 0 at the feet, every model facing north (-z) with its left on +x; +x pitches a bone's top forward (and so
opens a jaw hinged behind it), -x raises a hanging arm or leg forward, a left arm turns outwards with -z and a right one
with +z, and +y turns the front to the model's right. A keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. GEO, ANIMATIONS and SHEETS are keyed by entity id; CLIPS lists the clips the Java plays.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "ENTITY_MODEL", "CLIPS", "CONTROLLERS"]

# ------------------------------------------------------------------------------------------------- the Yeti King

# A great white ape of the glacier, twice a player's height and more: shaggy white fur frosted blue, a bare blue-grey
# face, palms and soles, eyes of glacier blue under a heavy brow, a crown of blue ice. He stands hunched, his long arms
# reaching his knuckles to the snow, and goes on all fours when he charges.
HUNCH = 16.0                                  # how far his torso leans forward at rest (the head looks level again)
ARM_FORWARD = 18.0                            # his arms' rest, raised forward against the hunch so they hang down
ARM_OUT = 6.0
ELBOW = 12.0
HELD_Y = 80.0                                 # the middle of the block of ice he lifts over his head: HurledBoulderEntity
                                              # throws it from where it is held, (HELD_Y - 8) / 16 blocks up


def yeti_king_model():
    m = Model("yeti_king", 256, (5.0, 4.5))
    m.bone("root")
    m.bone("hips", "root", (0, 20, 3.5))
    m.cube("hips", "pelvis", (-9.5, 15, -2.5), (19, 10, 12))
    for side, name in ((1, "left"), (-1, "right")):
        mirror = side < 0

        def ox(v, w):  # a left-side origin x for a cube `w` wide, mirrored to the right
            return v if side > 0 else -v - w

        m.bone(f"leg_{name}", "hips", (side * 5.5, 20, 3.5))
        m.cube(f"leg_{name}", "thigh", (ox(1.5, 8), 9, -1), (8, 12, 9), mirror=mirror)
        m.bone(f"shin_{name}", f"leg_{name}", (side * 5.5, 10, 3.5))
        m.cube(f"shin_{name}", "shin", (ox(2, 7), 2, -0.5), (7, 9, 8), mirror=mirror)
        m.cube(f"shin_{name}", "foot", (ox(1, 9), 0, -6), (9, 3, 12), mirror=mirror)
    # The torso, hunched: his chest and the shaggy mantle over his shoulders.
    m.bone("torso", "hips", (0, 24, 3.5), rotation=(HUNCH, 0, 0))
    m.cube("torso", "chest", (-12, 24, -5), (24, 20, 17))
    m.cube("torso", "mantle", (-14, 37, -6), (28, 9, 19))
    # The head, held level again before his shoulders: skull, brow, muzzle and fangs; the jaw on its own bone.
    m.bone("head", "torso", (0, 43, -3), rotation=(-HUNCH, 0, 0))
    m.cube("head", "skull", (-8, 41, -15), (16, 14, 13))
    m.cube("head", "brow", (-8.5, 50.5, -16), (17, 3, 3))
    m.cube("head", "muzzle", (-5, 41.5, -18), (10, 6, 4))
    m.cube("head", "fang", (-4.5, 39.5, -18), (1, 2, 1))
    m.cube("head", "fang", (3.5, 39.5, -18), (1, 2, 1), mirror=True)
    m.bone("jaw", "head", (0, 43, -6))
    m.cube("jaw", "jaw", (-4.5, 38.5, -17.5), (9, 3, 12))
    # The crown of blue ice: a band and its points, the tallest at the front, the side ones leaning out.
    m.bone("crown", "head", (0, 55, -8.5))
    m.cube("crown", "band", (-8.5, 54, -15.5), (17, 2, 14))
    m.cube("crown", "point_tall", (-1.5, 56, -16), (3, 8, 3))
    m.cube("crown", "point", (-6.5, 56, -15.5), (2, 6, 2), rotation=(0, 0, 14), pivot=(-5.5, 56, -14.5))
    m.cube("crown", "point", (4.5, 56, -15.5), (2, 6, 2), rotation=(0, 0, -14), pivot=(5.5, 56, -14.5), mirror=True)
    m.cube("crown", "point_low", (-8.5, 56, -10), (2, 4, 2), rotation=(0, 0, 18), pivot=(-7.5, 56, -9))
    m.cube("crown", "point_low", (6.5, 56, -10), (2, 4, 2), rotation=(0, 0, -18), pivot=(7.5, 56, -9), mirror=True)
    m.cube("crown", "point_low", (-1, 56, -4), (2, 4, 2), rotation=(-18, 0, 0), pivot=(0, 56, -3))
    # The Fury of the Peaks (the crown controller's alone): blue fire wrapping the crown's points, and flames rising from
    # his eyes. Shown only while his crown blazes.
    m.bone("fury", "crown", (0, 55, -8.5))
    m.cube("fury", "blaze_tall", (-2.5, 55.5, -17), (5, 12, 5))
    m.cube("fury", "blaze", (-7.5, 55.5, -16.5), (4, 9, 4), rotation=(0, 0, 14), pivot=(-5.5, 56, -14.5))
    m.cube("fury", "blaze", (3.5, 55.5, -16.5), (4, 9, 4), rotation=(0, 0, -14), pivot=(5.5, 56, -14.5), mirror=True)
    m.cube("fury", "eye_flare", (-6.5, 49.5, -16.2), (13, 7, 0))
    # The long arms, knuckles to the snow; the fur hanging from each forearm frosted with icicles.
    for side, name in ((1, "left"), (-1, "right")):
        mirror = side < 0

        def ox(v, w):
            return v if side > 0 else -v - w

        m.bone(f"arm_{name}", "torso", (side * 16, 42, 2), rotation=(-ARM_FORWARD, 0, -side * ARM_OUT))
        m.cube(f"arm_{name}", "upper_arm", (ox(12, 9), 25, -2.5), (9, 19, 9), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (side * 16.5, 26, 2), rotation=(-ELBOW, 0, 0))
        m.cube(f"forearm_{name}", "forearm", (ox(12.5, 8), 10, -2.5), (8, 17, 9), mirror=mirror)
        m.cube(f"forearm_{name}", "fist", (ox(11.5, 10), 1, -4), (10, 9, 11), mirror=mirror)
        m.cube(f"forearm_{name}", "arm_fur", (ox(12.5, 8), 5, 6.5), (8, 14, 0), rotation=(14, 0, 0),
               pivot=(side * 16.5, 19, 6.5), mirror=mirror)
    # The block of ice he tears from the lake and lifts over his head for his Boulder Throw: shown only then.
    m.bone("held", "torso", (0, HELD_Y, -6))
    m.cube("held", "held_ice", (-7, HELD_Y - 7, -13), (14, 14, 14))
    return m


SIDES = ("left", "right")
HIDDEN = {"held": {"scale": (0, 0, 0)}}
SHOWN = {"held": {"scale": (1, 1, 1)}}


def _arms(x=0.0, y=0.0, out=ARM_OUT, elbow=0.0, only=SIDES):
    """Both arms (or `only` these) moved from rest: raised `x` degrees more about x (negative is forward and up), turned
    `y` degrees outwards, the hand held `out` degrees away from his side (ARM_OUT at rest; negative crosses before him),
    the elbow bent `elbow` more. Once an arm is raised past level a turn about z swings its hand the other way, so `out`
    is turned to suit."""
    pose = {}
    for name in only:
        s = 1 if name == "left" else -1
        raised = math.cos(math.radians(x - ARM_FORWARD)) < 0
        pose[f"arm_{name}"] = (x, -s * y, (s if raised else -s) * out + s * ARM_OUT)
        pose[f"forearm_{name}"] = (-elbow, 0, 0)
    return pose


def _legs(x=0.0, knee=0.0, out=0.0, only=SIDES):
    """Both legs (or `only` these): the thigh raised `x` degrees about x (negative is forward), the knee bent `knee`
    (positive folds the foot back), the leg swung `out` degrees out to the side."""
    pose = {}
    for name in only:
        s = 1 if name == "left" else -1
        pose[f"leg_{name}"] = (x, 0, -s * out)
        pose[f"shin_{name}"] = (knee, 0, 0)
    return pose


def _pose(*parts, **bones):
    out = dict(HIDDEN)
    for part in parts:
        out.update(part)
    out.update(bones)
    return out


def _sway(length, pose, sways):
    """A looping clip holding `pose` while some of its bones sway about it: `sways` maps a bone to (amplitude, phase) or
    (amplitude, phase, cycles) on its rotation, or to {channel: ...} of those."""
    bones = {}
    for bone, value in pose.items():
        for channel, v in (value if isinstance(value, dict) else {"rotation": value}).items():
            bones.setdefault(bone, {})[channel] = still(v)
    for bone, spec in sways.items():
        for channel, (amplitude, phase, *cycles) in (spec if isinstance(spec, dict) else {"rotation": spec}).items():
            value = pose.get(bone, {})
            base = (value if isinstance(value, dict) else {"rotation": value}).get(channel, (0, 0, 0))
            bones.setdefault(bone, {})[channel] = wave(length, cycles[0] if cycles else 1, base, amplitude, phase=phase)
    return clip(length, bones)


IDLE = _pose()
# Slumped asleep on his throne: sunk onto the pelt, leaning back, chin on his chest, his legs sprawled forward and his
# fists on his knees.
THRONE = _pose(_legs(x=-78, knee=48, out=10), _arms(x=-6, out=8, elbow=58), root={"position": (0, -15, 9)},
               torso=(-22, 0, 0), head=(38, 0, 0))
# Waking: he heaves himself up on the throne, rears and roars.
RISING = _pose(_legs(x=-30, knee=30, out=8), _arms(x=-40, out=20, elbow=20), root={"position": (0, -6, 6)},
               torso=(-6, 0, 0), head=(-10, 0, 0))
WAKE_ROAR = _pose(_arms(x=-110, out=45, elbow=25), root={"position": (0, 0, 3)}, torso=(-24, 0, 0), head=(-34, 0, 0),
                  jaw=(42, 0, 0))
# In the air: arms flung up and back, legs drawn up under him.
LEAP = _pose(_arms(x=-150, out=24, elbow=30), _legs(x=-55, knee=70), torso=(-8, 0, 0), head=(-12, 0, 0))
# Maul Swipe: he rears back on his right, that arm raised out wide behind him; then it rakes across before him.
SWIPE_BACK = _pose(_arms(x=-120, y=20, out=55, elbow=30, only=("right",)), _arms(x=-20, out=10, only=("left",)),
                   torso=(-10, 34, 0), head=(-6, -16, 0), jaw=(14, 0, 0))
SWIPE_THROUGH = _pose(_arms(x=-80, y=-30, out=-25, elbow=6, only=("right",)), _arms(x=10, out=20, only=("left",)),
                      torso=(12, -38, 0), head=(4, 20, 0), jaw=(24, 0, 0))
# Boulder Throw: he digs both fists into the lake, then heaves the block of ice up over his head.
DIG = _pose(_arms(x=-48, out=-4, elbow=-6), _legs(x=-34, knee=40), root={"position": (0, -4, 0)}, torso=(36, 0, 0),
            head=(-30, 0, 0))
HEAVE = _pose(SHOWN, _arms(x=-172, out=-12, elbow=10), _legs(x=-8, knee=10), torso=(-18, 0, 0), head=(-12, 0, 0))
HURL = _pose(_arms(x=-58, out=-4, elbow=-4), torso=(30, 0, 0), head=(-20, 0, 0), jaw=(20, 0, 0))
# Ground Slam: crouched, both fists high; aloft, legs drawn up; then he comes down, fists into the ice.
CROUCH = _pose(_arms(x=-165, out=6, elbow=14), _legs(x=-40, knee=56), root={"position": (0, -5, 0)}, torso=(-12, 0, 0),
               head=(-16, 0, 0), jaw=(10, 0, 0))
ALOFT = _pose(_arms(x=-170, out=4, elbow=10), _legs(x=-60, knee=80), torso=(-14, 0, 0), head=(-10, 0, 0))
SLAMMED = _pose(_arms(x=-62, out=4, elbow=-8), _legs(x=-46, knee=62), root={"position": (0, -7, 0)}, torso=(44, 0, 0),
                head=(-34, 0, 0), jaw=(30, 0, 0))
# Frost Breath: he swells his chest, head back, arms thrown back; then breathes the cold out over the ice before him.
INHALE = _pose(_arms(x=22, out=26, elbow=4), torso=(-14, 0, 0), head=(-24, 0, 0))
EXHALE = _pose(_arms(x=10, out=30), torso=(12, 0, 0), head=(18, 0, 0), jaw=(38, 0, 0))
# Avalanche Charge: down on all fours, pawing the snow; then the gallop.
FOURS = _pose(_arms(x=-48, out=6, elbow=-4), _legs(x=-22, knee=38), root={"position": (0, -6, 0)}, torso=(48, 0, 0),
              head=(-46, 0, 0))
# Stunned: knocked down onto his haunches against the column, legs out, dazed.
REELING = _pose(_arms(x=22, out=24, elbow=10), _legs(x=-74, knee=24, out=22), root={"position": (0, -13, 4)},
                torso=(-24, 0, 0), head=(16, 0, 0), jaw=(16, 0, 0))
# The King's Roar, from his dais: upright, arms flung wide, head thrown back.
ROAR = _pose(_arms(x=-100, out=58, elbow=24), torso=(-26, 0, 0), head=(-36, 0, 0), jaw=(46, 0, 0))
# Icicle Fall: he roars up at the vault, fists raised, and pounds the lake to shake the icicles down.
ROAR_UP = _pose(_arms(x=-160, out=30, elbow=18), torso=(-24, 0, 0), head=(-52, 0, 0), jaw=(44, 0, 0))
POUND = _pose(_arms(x=-56, out=2, elbow=-8), _legs(x=-30, knee=40), root={"position": (0, -5, 0)}, torso=(40, 0, 0),
              head=(-30, 0, 0), jaw=(30, 0, 0))
# Glacial Spikes: his right fist raised high, then driven down through the ice.
FIST_UP = _pose(_arms(x=-165, out=12, elbow=20, only=("right",)), _arms(x=-10, out=16, only=("left",)),
                torso=(-12, -10, 0), head=(-14, 6, 0), jaw=(16, 0, 0))
FIST_DOWN = _pose(_arms(x=-48, out=-6, elbow=-6, only=("right",)), _arms(x=10, out=24, only=("left",)),
                  _legs(x=-30, knee=40), root={"position": (0, -5, 0)}, torso=(42, -6, 0), head=(-32, 0, 0),
                  jaw=(26, 0, 0))
# Kin Call: upright, beating his chest with his fists, calling.
CHEST = _pose(_arms(x=-52, out=-14, elbow=128), torso=(-14, 0, 0), head=(-26, 0, 0), jaw=(36, 0, 0))
# Death: to his knees, then forward onto the ice.
KNEEL = _pose(_arms(x=14, out=18, elbow=6), _legs(x=0, knee=90), root={"position": (0, -9, 0)}, torso=(14, 0, 0),
              head=(24, 0, 0), jaw=(20, 0, 0))
FALLEN = _pose(_arms(x=-130, out=26, elbow=4), _legs(x=-10, knee=90), root={"position": (0, -14, -6)},
               torso=(74, 0, 0), head=(-6, 0, 30), jaw=(24, 0, 0))


def _gait(length, cycles, legs=24.0, arms=22.0, knee=26.0):
    """His lope on two legs and his knuckles: each arm swings with the leg on the other side."""
    out = {}
    for name in SIDES:
        phase = 0.0 if name == "left" else 0.5
        out[f"leg_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (legs, 0, 0), phase=phase)}
        out[f"shin_{name}"] = {"rotation": wave(length, cycles, (knee / 2, 0, 0), (knee / 2, 0, 0), phase=phase + 0.25)}
        out[f"arm_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (-arms, 0, 0), phase=phase)}
        out[f"forearm_{name}"] = {"rotation": wave(length, cycles, (-6, 0, 0), (-6, 0, 0), phase=phase + 0.25)}
    out["torso"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, 4, 3))}
    out["head"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -4, -3))}
    out["root"] = {"position": wave(length, 2 * cycles, (0, 0, 0), (0, 0.8, 0))}
    return out


# The Avalanche Charge on all fours: fists and feet pounding in pairs, the back heaving (sways about FOURS).
GALLOP = {**{f"arm_{name}": ((34, 0, 0), lag) for name, lag in (("left", 0.0), ("right", 0.1))},
          **{f"forearm_{name}": ((-14, 0, 0), lag + 0.25) for name, lag in (("left", 0.0), ("right", 0.1))},
          **{f"leg_{name}": ((-30, 0, 0), lag + 0.5) for name, lag in (("left", 0.0), ("right", 0.1))},
          **{f"shin_{name}": ((24, 0, 0), lag + 0.75) for name, lag in (("left", 0.0), ("right", 0.1))},
          "torso": ((8, 0, 0), 0.25), "root": {"position": ((0, 1.5, 0), 0.25)}}


def yeti_king_animations():
    """His body's clips (the "body" controller) and his crown's (calm ice, or blazing in the Fury of the Peaks). The
    body's never key the fury bone, nor the crown's any other."""
    return animations({
        "animation.yeti_king.throne": _sway(4.0, THRONE, {"torso": ((3, 0, 0), 0.0), "head": ((-4, 0, 2), 0.15)}),
        "animation.yeti_king.wake": posed(1.0, [(0.0, THRONE), (0.35, RISING), (0.6, WAKE_ROAR), (1.0, WAKE_ROAR)]),
        "animation.yeti_king.leap": _sway(0.8, LEAP, {"root": ((6, 0, 0), 0.0)}),
        "animation.yeti_king.idle": _sway(3.0, IDLE, {"torso": ((2.5, 0, 0), 0.0), "head": ((-2, 3, 0), 0.1),
                                                      "arm_left": ((-3, 0, -1), 0.2), "arm_right": ((-3, 0, 1), 0.3)}),
        "animation.yeti_king.walk": posed(1.2, [(0.0, IDLE), (1.2, IDLE)], loop=True, waves=_gait(1.2, 1)),
        "animation.yeti_king.swipe_windup": posed(0.6, [(0.0, IDLE), (0.6, SWIPE_BACK)]),
        "animation.yeti_king.swipe": posed(0.6, [(0.0, SWIPE_BACK), (0.1, SWIPE_THROUGH), (0.3, SWIPE_THROUGH),
                                                 (0.6, IDLE)]),
        "animation.yeti_king.boulder_windup": posed(0.8, [(0.0, IDLE), (0.3, DIG), (0.38, {**DIG, **SHOWN}),
                                                          (0.8, HEAVE)]),
        "animation.yeti_king.throw": posed(0.55, [(0.0, HURL), (0.2, HURL), (0.55, IDLE)]),
        "animation.yeti_king.slam_windup": posed(0.8, [(0.0, IDLE), (0.8, CROUCH)]),
        "animation.yeti_king.slam": posed(1.5, [(0.0, CROUCH), (0.15, ALOFT), (0.7, ALOFT), (0.8, SLAMMED),
                                                (1.2, SLAMMED), (1.5, IDLE)]),
        "animation.yeti_king.breath_windup": posed(1.0, [(0.0, IDLE), (1.0, INHALE)]),
        "animation.yeti_king.breath": _sway(0.6, EXHALE, {"head": ((0, 7, 0), 0.0, 2)}),
        "animation.yeti_king.charge_windup": _sway(0.6, FOURS, {"arm_right": ((-26, 0, 0), 0.0),
                                                                "forearm_right": ((-30, 0, 0), 0.25)}),
        "animation.yeti_king.charge": _sway(0.4, FOURS, GALLOP),
        "animation.yeti_king.stunned": _sway(2.0, REELING, {"head": ((4, 10, 14), 0.0, 2), "torso": ((0, 4, 5), 0.2)}),
        "animation.yeti_king.roar": _sway(0.4, ROAR, {"head": ((0, 6, 3), 0.0, 2), "jaw": ((4, 0, 0), 0.0)}),
        "animation.yeti_king.icicles": posed(2.3, [(0.0, IDLE), (0.5, ROAR_UP), (0.85, ROAR_UP), (1.0, POUND),
                                                   (1.5, POUND), (2.3, IDLE)]),
        "animation.yeti_king.spikes_windup": posed(0.8, [(0.0, IDLE), (0.8, FIST_UP)]),
        "animation.yeti_king.spikes": posed(0.9, [(0.0, FIST_UP), (0.1, FIST_DOWN), (0.45, FIST_DOWN), (0.9, IDLE)]),
        "animation.yeti_king.call": _sway(0.5, CHEST, {"arm_left": ((-16, 0, -6), 0.0), "arm_right": ((-16, 0, 6), 0.5)}),
        "animation.yeti_king.death": posed(2.0, [(0.0, IDLE), (0.6, KNEEL), (1.0, KNEEL), (1.5, FALLEN), (2.0, FALLEN)]),
        # His crown: calm ice, or (the Fury of the Peaks) wrapped in blue fire, his eyes burning.
        "animation.yeti_king.crown_calm": clip(1.0, {"fury": {"scale": still((0, 0, 0))}}),
        "animation.yeti_king.crown_blazing": clip(0.6, {"fury": {"scale": wave(0.6, 2, (1, 1, 1), (0.05, 0.09, 0.05)),
                                                                 "position": wave(0.6, 1, (0, 0, 0), (0, 0.3, 0))}}),
    })


# ------------------------------------------------------------------------------------------------- his whelps

def whelp_model():
    """A yeti whelp from the dens: a cub as tall as a player's chest, all round head and fluff, with a tuft of blue
    frost on its crown."""
    m = Model("yeti_whelp", 64, (1.5, 1.6))
    m.bone("root")
    m.bone("cub", "root", (0, 6, 0))
    m.cube("cub", "cub_body", (-4.5, 5, -3.5), (9, 9, 8))
    m.bone("cub_head", "cub", (0, 14, -1))
    m.cube("cub_head", "cub_skull", (-4.5, 13, -6.5), (9, 8, 8))
    m.cube("cub_head", "cub_muzzle", (-2.5, 13.5, -8), (5, 3, 2))
    m.cube("cub_head", "cub_ear", (-6, 18, -3), (2, 2, 1))
    m.cube("cub_head", "cub_ear", (4, 18, -3), (2, 2, 1), mirror=True)
    m.cube("cub_head", "cub_tuft", (-1.5, 21, -4), (3, 2, 3))
    for side, name in ((1, "left"), (-1, "right")):
        mirror = side < 0

        def ox(v, w):
            return v if side > 0 else -v - w

        m.bone(f"cub_leg_{name}", "cub", (side * 2.5, 6, 0.5))
        m.cube(f"cub_leg_{name}", "cub_leg", (ox(0.5, 4), 0, -1.5), (4, 6, 4), mirror=mirror)
        m.bone(f"cub_arm_{name}", "cub", (side * 5, 13, -0.5), rotation=(-8, 0, -side * 8))
        m.cube(f"cub_arm_{name}", "cub_arm", (ox(4.5, 3), 3, -2), (3, 10, 3), mirror=mirror)
    return m


def whelp_animations():
    walk = {}
    for name in SIDES:
        phase = 0.0 if name == "left" else 0.5
        walk[f"cub_leg_{name}"] = {"rotation": wave(0.5, 1, (0, 0, 0), (32, 0, 0), phase=phase)}
        walk[f"cub_arm_{name}"] = {"rotation": wave(0.5, 1, (0, 0, 0), (-28, 0, 0), phase=phase)}
    walk["cub"] = {"rotation": wave(0.5, 1, (6, 0, 0), (0, 0, 5)), "position": wave(0.5, 2, (0, 0, 0), (0, 0.6, 0))}
    walk["cub_head"] = {"rotation": wave(0.5, 1, (-6, 0, 0), (0, 0, -4))}
    return animations({
        "animation.yeti_whelp.walk": clip(0.5, walk),
        "animation.yeti_whelp.idle": clip(2.0, {
            "cub": {"scale": wave(2.0, 1, (1, 1, 1), (0.02, 0.03, 0.02))},
            "cub_head": {"rotation": wave(2.0, 1, (0, 0, 0), (4, 8, 6))},
            "cub_arm_left": {"rotation": wave(2.0, 1, (0, 0, 0), (-4, 0, -3), phase=0.25)},
            "cub_arm_right": {"rotation": wave(2.0, 1, (0, 0, 0), (-4, 0, 3), phase=0.75)}}),
    })


# ------------------------------------------------------------------------------------------------- his things

def boulder_model():
    """A block of the lake's ice he tore out and hurls, crusted with snow and lumpy, tumbling end over end."""
    m = Model("hurled_boulder", 64, (1.5, 1.5))
    m.bone("root")
    m.bone("tumble", "root", (0, 8, 0))
    m.cube("tumble", "boulder", (-6, 2, -6), (12, 12, 12))
    m.cube("tumble", "lump", (-7.5, 4, -3), (3, 7, 7))
    m.cube("tumble", "lump", (4.5, 6, -4), (3, 7, 7), mirror=True)
    m.cube("tumble", "crust", (-5, 13, -5), (10, 2, 9))
    m.cube("tumble", "chunk", (-3, 0.5, -7), (6, 5, 3))
    return m


def boulder_animations():
    return animations({"animation.hurled_boulder.tumble": clip(0.8, {"tumble": {"rotation": keys({
        0.0: (0, 0, 0), 0.4: (180, 40, 0), 0.8: (360, 80, 0)})}})})


def icicle_model():
    """An icicle shaken from the vault, a block and a half long: a broken stump at its top tapering to a point."""
    m = Model("falling_icicle", 64, (1.0, 2.0))
    m.bone("root")
    m.bone("icicle", "root", (0, 12, 0))
    m.cube("icicle", "icicle_stump", (-4, 18, -4), (8, 6, 8))
    m.cube("icicle", "icicle_upper", (-3, 11, -3), (6, 7, 6))
    m.cube("icicle", "icicle_lower", (-2, 5, -2), (4, 6, 4))
    m.cube("icicle", "icicle_tip", (-1, 0, -1), (2, 5, 2))
    return m


def icicle_animations():
    """Turning slowly as it drops, with a shiver."""
    return animations({"animation.falling_icicle.fall": clip(1.0, {"icicle": {
        "rotation": keys({0.0: (0, 0, 0), 0.25: (2, 45, -2), 0.5: (0, 90, 0), 0.75: (-2, 135, 2), 1.0: (0, 180, 0)})}})})


def spike_model():
    """A glacial spike burst up through the lake: a tall shard of clear blue ice with two lesser ones leaning out of its
    foot, in a rubble of broken snow."""
    m = Model("glacial_spike", 64, (1.5, 2.0))
    m.bone("root")
    m.bone("spike", "root", (0, 0, 0))
    m.cube("spike", "shard", (-3, 0, -3), (6, 19, 6), rotation=(0, 45, 0), pivot=(0, 0, 0))
    m.cube("spike", "shard_tip", (-1.5, 19, -1.5), (3, 6, 3), rotation=(0, 45, 0), pivot=(0, 19, 0))
    m.cube("spike", "shard_side", (2, 0, -2), (4, 12, 4), rotation=(8, 0, 26), pivot=(2, 0, 0))
    m.cube("spike", "shard_side", (-6, 0, -2), (4, 12, 4), rotation=(-8, 0, -26), pivot=(-2, 0, 0), mirror=True)
    m.cube("spike", "rubble", (-5.5, 0, -5.5), (11, 2, 11))
    return m


def spike_animations():
    """It bursts up over its rise (RISE_TICKS, 0.2 s), overshooting a little; stands; and crumbles as it goes."""
    return animations({"animation.glacial_spike.burst": clip(1.2, {"spike": {"scale": keys({
        0.0: (0.4, 0.05, 0.4), 0.15: (1.1, 1.15, 1.1), 0.2: (1, 1, 1), 1.0: (1, 1, 1), 1.2: (0.7, 0.25, 0.7)})}},
        loop=False)})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"yeti_king": yeti_king_model, "yeti_whelp": whelp_model, "hurled_boulder": boulder_model,
          "falling_icicle": icicle_model, "glacial_spike": spike_model}
ANIMATIONS = {"yeti_king": yeti_king_animations, "yeti_whelp": whelp_animations, "hurled_boulder": boulder_animations,
              "falling_icicle": icicle_animations, "glacial_spike": spike_animations}
ENTITY_MODEL = {name: name for name in MODELS}
# The clips the Java names (YetiKingEntity and his things).
CLIPS = {
    "yeti_king": ("throne", "wake", "leap", "idle", "walk", "swipe_windup", "swipe", "boulder_windup", "throw",
                  "slam_windup", "slam", "breath_windup", "breath", "charge_windup", "charge", "stunned", "roar",
                  "icicles", "spikes_windup", "spikes", "call", "death", "crown_calm", "crown_blazing"),
    "yeti_whelp": ("walk", "idle"),
    "hurled_boulder": ("tumble",),
    "falling_icicle": ("fall",),
    "glacial_spike": ("burst",),
}
# The bones each of the King's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"crown": ("fury",)}
