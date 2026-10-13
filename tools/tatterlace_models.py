"""GeckoLib bodies for Madame Tatterlace and her fight (docs/features/tatterlace.md): Tatterlace herself, the thimble she
tosses, the spool she kicks rolling, her egg sacs and her spiderlings. Geometry and animations only, as plain dicts; no
I/O. tools/tatterlace_art.py paints their sheets on the same box-UV regions, at TEXTURE_SCALE times the size the models
declare (docs/ART_DIRECTION.md, "High resolution"). The builder and the posing helpers are Vesperine's
(tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0 at the feet, every
model facing north (-z) with its left on +x; +x pitches a bone's top forward, +y turns its front to the model's right, and
a limb along +x (her left) rises with -z (along -x, her right, with +z).

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. GEO, ANIMATIONS and SHEETS are keyed by entity id; CLIPS lists the clips the Java plays.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "ENTITY_MODEL", "CLIPS", "CONTROLLERS"]

# ------------------------------------------------------------------------------------------------- Tatterlace

# A great spider seamstress, three blocks across: a black body fringed in red, her abdomen raised behind her with a
# stock of gold thimbles on its back, a tall headdress of red and gold, a purple gem at her brow and eight red eyes, gold
# cuffs at every knee. Her palps hold a lace doily before her face, and her right foreleg a long needle.
BODY_Y = 15.0                                 # the legs' hips, on the sides of her thorax
HIPS = (-8.0, -4.0, 0.0, 4.0)                 # z of each pair of legs, front to back
SPREAD = (48.0, 18.0, -14.0, -42.0)           # how far each pair sweeps forward (degrees about y; her left's sign)
FEMUR, TIBIA = 14, 24
# The femur's rise and the knee's bend at rest (degrees): the forelegs held up before her, the rest down to the lace.
LIFT = (65.0, 40.0, 40.0, 40.0)
BEND = (95.0, 120.0, 120.0, 120.0)
NEEDLE = 22                                   # the needle's length beyond her right foreleg's tip
DRAGLINE_Y = 40.0                             # where her dragline leaves her spinnerets as she hangs


def _leg(m, side, i):
    """One leg: a femur rising out from the hip, a gold cuff at the knee (with a red one over it, shown in Frenzied
    Stitching, on a bone of its own), and the tibia bending down. On her right (side -1) everything mirrors; her right
    foreleg carries the needle on beyond its tip."""
    name = f"{'left' if side > 0 else 'right'}_{i}"
    hip = (side * 7.0, BODY_Y, HIPS[i])
    m.bone(f"leg_{name}", "body", hip, rotation=(0, side * SPREAD[i], -side * LIFT[i]))
    x0 = hip[0] if side > 0 else hip[0] - FEMUR
    m.cube(f"leg_{name}", "femur", (x0, BODY_Y - 1.5, HIPS[i] - 1.5), (FEMUR, 3, 3), mirror=side < 0)
    knee = (hip[0] + side * FEMUR, BODY_Y, HIPS[i])
    m.bone(f"knee_{name}", f"leg_{name}", knee, rotation=(0, 0, side * BEND[i]))
    m.cube(f"knee_{name}", "cuff", (knee[0] - 2, BODY_Y - 2, HIPS[i] - 2), (4, 4, 4), mirror=side < 0)
    m.bone(f"cuffs_{name}", f"knee_{name}", knee)
    m.cube(f"cuffs_{name}", "cuff_red", (knee[0] - 2.5, BODY_Y - 2.5, HIPS[i] - 2.5), (5, 5, 5), mirror=side < 0)
    t0 = knee[0] if side > 0 else knee[0] - TIBIA
    m.cube(f"knee_{name}", "tibia", (t0, BODY_Y - 1, HIPS[i] - 1), (TIBIA, 2, 2), mirror=side < 0)
    if side < 0 and i == 0:
        tip = knee[0] - TIBIA
        m.cube(f"knee_{name}", "needle", (tip - NEEDLE, BODY_Y - 0.5, HIPS[i] - 0.5), (NEEDLE, 1, 1))
        m.cube(f"knee_{name}", "needle_eye", (tip - 1, BODY_Y - 1, HIPS[i] - 1), (2, 2, 2))
    return name


def tatterlace_model():
    m = Model("tatterlace", 128, (4.0, 3.0))
    m.bone("root")
    m.bone("body", "root", (0, BODY_Y, 0))
    m.cube("body", "thorax", (-7, 11, -10), (14, 9, 14))
    # The head: her face with its eyes and the gem at her brow, two fangs below.
    m.bone("head", "body", (0, 16, -10))
    m.cube("head", "face", (-5, 12, -15), (10, 8, 5))
    m.cube("head", "gem", (-1, 17.5, -15.5), (2, 2, 1))
    m.cube("head", "fang", (-3, 9, -15), (2, 4, 2))
    m.cube("head", "fang", (1, 9, -15), (2, 4, 2), mirror=True)
    # The headdress: a gold band, a crown of red velvet, and three gold-tipped spires rising from it.
    m.bone("headdress", "head", (0, 20, -12))
    m.cube("headdress", "band", (-5.5, 19.5, -14), (11, 2, 9))
    m.cube("headdress", "crown", (-4, 21.5, -13), (8, 6, 7))
    m.cube("headdress", "spire", (-1, 27.5, -11), (2, 8, 2))
    m.cube("headdress", "spire_side", (-4, 26.5, -10.5), (2, 5, 2))
    m.cube("headdress", "spire_side", (2, 26.5, -10.5), (2, 5, 2), mirror=True)
    # The palps and the doily they hold before her face.
    m.bone("palps", "head", (0, 12, -15))
    m.cube("palps", "palp", (-4, 8, -19), (2, 2, 5))
    m.cube("palps", "palp", (2, 8, -19), (2, 2, 5), mirror=True)
    m.bone("doily", "palps", (0, 9, -20))
    m.cube("doily", "doily", (-6, 3, -20.5), (12, 12, 0))
    # The abdomen, raised behind her, with its fringe of red, the thimbles on its back and the spinnerets at its end.
    m.bone("abdomen", "body", (0, 17, 4))
    m.cube("abdomen", "abdomen", (-9, 12, 3), (18, 15, 21))
    m.cube("abdomen", "fringe", (-9.5, 10, 5), (0, 4, 17))
    m.cube("abdomen", "fringe", (9.5, 10, 5), (0, 4, 17), mirror=True)
    for dx, dz in ((-5, 8), (1, 8), (-2, 14)):
        m.cube("abdomen", "thimble", (dx, 27, dz), (4, 3, 4))
    m.cube("abdomen", "spinnerets", (-2, 15, 24), (4, 3, 2))
    # Her dragline: shown only while she climbs, hangs or drops, rising from where her spinnerets are when she hangs
    # head down (her body pitched forward a quarter turn about its middle).
    m.bone("dragline", "root", (0, DRAGLINE_Y, 0))
    m.cube("dragline", "dragline", (-0.5, DRAGLINE_Y, -2), (1, 48, 1))
    for side in (-1, 1):
        for i in range(4):
            _leg(m, side, i)
    return m


LEGS = [f"{s}_{i}" for s in ("left", "right") for i in range(4)]


def _legs(lift=0.0, bend=0.0, spread=0.0, only=None):
    """Every leg (or `only` these) moved from rest: its femur raised `lift` degrees more, its knee bent `bend` more and
    swept `spread` more forward."""
    out = {}
    for name in only or LEGS:
        side = 1 if name.startswith("left") else -1
        out[f"leg_{name}"] = (0, side * spread, -side * lift)
        out[f"knee_{name}"] = (0, 0, side * bend)
    return out


def _gait(length, cycles, swing=14.0, lift=10.0):
    """A spider's walk: two alternating tetrads (left 0 and 2 with right 1 and 3, and the rest), each leg sweeping and
    lifting in turn."""
    out = {}
    for name in LEGS:
        side = 1 if name.startswith("left") else -1
        i = int(name[-1])
        phase = 0.0 if (side > 0) == (i % 2 == 0) else 0.5
        out[f"leg_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, side * swing, -side * lift), phase=phase)}
        out[f"knee_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, 0, side * lift * 0.6), phase=phase + 0.25)}
    return out


HIDDEN = {"dragline": {"scale": (0, 0, 0)}}
SHOWN = {"dragline": {"scale": (1, 1, 1)}}
IDLE = {**HIDDEN, "abdomen": (-6, 0, 0), "palps": (10, 0, 0), "head": (4, 0, 0)}
# Sewing while she waits: her palps hold the doily low and the needle works at it.
SEW = {**HIDDEN, "abdomen": (-10, 0, 0), "head": (14, 0, 0), "palps": (35, 0, 0), "doily": (70, 0, 0),
       **_legs(lift=-10, bend=-10, spread=25, only=["right_0"])}
# Needlepoint: she rears on her back legs, forelegs drawn back; then two quick stabs.
REAR = {**HIDDEN, "body": (-28, 0, 0), "abdomen": (18, 0, 0), "head": (-8, 0, 0),
        **_legs(lift=45, bend=-55, spread=-30, only=["left_0", "right_0"]),
        **_legs(lift=10, bend=-10, only=["left_1", "right_1"])}
STAB = {**HIDDEN, "body": (-10, 0, 0), "abdomen": (8, 0, 0),
        **_legs(lift=-15, bend=-80, spread=35, only=["left_0", "right_0"])}
# Thimble Toss: a foreleg reaches back over her to the thimbles on her abdomen, then flings one.
PLUCK = {**HIDDEN, "abdomen": (-14, 0, 0), **_legs(lift=80, bend=-150, spread=-60, only=["left_0"])}
FLING = {**HIDDEN, "abdomen": (-4, 0, 0), **_legs(lift=30, bend=-60, spread=45, only=["left_0"])}
# Binding Thread: her abdomen swung up over her back, spinnerets aimed past her head; then the thread flies.
AIM = {**HIDDEN, "body": (8, 0, 0), "abdomen": (-75, 0, 0), "head": (-6, 0, 0), **_legs(lift=-8, bend=8)}
LOOSED = {**HIDDEN, "body": (4, 0, 0), "abdomen": (-55, 0, 0), **_legs(lift=-4, bend=4)}
# Lace Snare: the doily flicked out from her palps.
FLICK_BACK = {**HIDDEN, "palps": (-40, 0, 0), "doily": (-50, 0, 0), "head": (-10, 0, 0)}
FLICK = {**HIDDEN, "palps": (60, 0, 0), "doily": (80, 0, 0), "head": (12, 0, 0)}
# Spool Roll: she braces, front legs planted high on a flange, back legs pushing; then the kick.
BRACE = {**HIDDEN, "body": (-18, 0, 0), **_legs(lift=55, bend=-70, spread=10, only=["left_0", "right_0", "left_1", "right_1"]),
         **_legs(lift=-15, bend=25, only=["left_3", "right_3"])}
KICK = {**HIDDEN, "body": (6, 0, 0), **_legs(lift=20, bend=-90, spread=40, only=["left_0", "right_0"])}
# Climbing and hanging: upside down under her dragline, legs drawn in about the threads.
HANG = {**SHOWN, "body": (90, 0, 0), "abdomen": (-10, 0, 0), "head": (20, 0, 0), **_legs(lift=-10, bend=-20)}
CLIMB = {**SHOWN, "body": (-80, 0, 0), "abdomen": (30, 0, 0), **_legs(lift=40, bend=-40, spread=20)}
# Pin Rain and Unravel from the threads: legs shaking down, or picking at the lace below.
SHAKE = {**HANG, **_legs(lift=40, bend=-60)}
PICK = {**HANG, "palps": (-30, 0, 0), **_legs(lift=-10, bend=30, spread=30, only=["left_0", "right_0"])}
# Drop Strike: legs flung out as she falls, then sprawled on the lace, open to attack.
DROP = {**SHOWN, "body": (10, 0, 0), **_legs(lift=35, bend=-90)}
SPRAWL = {**HIDDEN, "root": {"position": (0, -5, 0)}, "body": (6, 0, 12), **_legs(lift=-20, bend=-40)}
# Brood: she calls to her egg sacs, rearing a little, palps raised.
CALL = {**HIDDEN, "body": (-12, 0, 0), "palps": (-50, 0, 0), "head": (-15, 0, 0), **_legs(lift=15, bend=-15)}
DEATH = {**HIDDEN, "root": {"position": (0, -9, 0)}, "body": (0, 0, 0), "abdomen": (14, 0, 0),
         **_legs(lift=-25, bend=-140)}


def tatterlace_animations():
    """Her body's clips (the "body" controller) and her cuffs' (calm gold, or glowing red in Frenzied Stitching). The
    body's never key a cuff, nor the cuffs' a body bone."""
    breathe = {"abdomen": {"rotation": wave(3.0, 1, (-6, 0, 0), (3, 0, 0))},
               "doily": {"rotation": wave(3.0, 1, (0, 0, 0), (6, 0, 4))}}
    return animations({
        "animation.tatterlace.sewing": posed(2.0, [(0.0, SEW), (2.0, SEW)], loop=True, waves={
            "leg_right_0": {"rotation": wave(2.0, 2, (0, -25, 10), (0, 10, 12))},
            "doily": {"rotation": wave(2.0, 1, (70, 0, 0), (6, 0, 6))}}),
        "animation.tatterlace.idle": posed(3.0, [(0.0, IDLE), (3.0, IDLE)], loop=True, waves=breathe),
        "animation.tatterlace.walk": posed(0.8, [(0.0, IDLE), (0.8, IDLE)], loop=True, waves=_gait(0.8, 2)),
        "animation.tatterlace.stab_windup": posed(0.5, [(0.0, IDLE), (0.5, REAR)]),
        "animation.tatterlace.stab": posed(0.5, [(0.0, REAR), (0.1, STAB), (0.2, REAR), (0.3, STAB), (0.5, IDLE)]),
        "animation.tatterlace.toss": posed(1.0, [(0.0, IDLE), (0.5, PLUCK), (0.65, FLING), (1.0, IDLE)]),
        "animation.tatterlace.thread_windup": posed(0.6, [(0.0, IDLE), (0.6, AIM)]),
        "animation.tatterlace.thread": posed(0.5, [(0.0, AIM), (0.1, LOOSED), (0.5, IDLE)]),
        "animation.tatterlace.snare": posed(1.0, [(0.0, IDLE), (0.5, FLICK_BACK), (0.65, FLICK), (1.0, IDLE)]),
        "animation.tatterlace.brace": posed(0.8, [(0.0, IDLE), (0.8, BRACE)]),
        "animation.tatterlace.kick": posed(0.6, [(0.0, BRACE), (0.15, KICK), (0.6, IDLE)]),
        "animation.tatterlace.climb": posed(1.0, [(0.0, CLIMB), (1.0, CLIMB)], loop=True, waves=_gait(1.0, 2, 10, 14)),
        "animation.tatterlace.hang": posed(2.0, [(0.0, HANG), (2.0, HANG)], loop=True, waves={
            "root": {"position": wave(2.0, 1, (0, 0, 0), (0, 1.0, 0))},
            "abdomen": {"rotation": wave(2.0, 1, (-20, 0, 0), (4, 0, 0))}}),
        "animation.tatterlace.pins": posed(1.0, [(0.0, HANG), (0.3, SHAKE), (1.0, SHAKE)], loop=True),
        "animation.tatterlace.unravel": posed(2.0, [(0.0, HANG), (0.4, PICK), (2.0, PICK)], loop=True),
        "animation.tatterlace.drop_windup": posed(0.8, [(0.0, HANG), (0.8, {**HANG, **_legs(lift=40, bend=-80)})]),
        "animation.tatterlace.drop": posed(0.4, [(0.0, DROP), (0.4, DROP)], loop=True),
        "animation.tatterlace.open": posed(3.0, [(0.0, SPRAWL), (2.6, SPRAWL), (3.0, IDLE)]),
        "animation.tatterlace.brood": posed(1.0, [(0.0, IDLE), (0.5, CALL), (1.0, CALL)]),
        "animation.tatterlace.death": posed(1.5, [(0.0, IDLE), (0.6, DEATH), (1.5, DEATH)]),
        # Her cuffs: gold, or (Frenzied Stitching) the red ones over them, glowing.
        "animation.tatterlace.cuffs_calm": clip(1.0, {f"cuffs_{name}": {"scale": still((0, 0, 0))} for name in LEGS}),
        "animation.tatterlace.cuffs_red": clip(1.0, {f"cuffs_{name}": {"scale": wave(1.0, 2, (1, 1, 1), (0.04, 0.04, 0.04))}
                                                     for name in LEGS}),
    })


# ------------------------------------------------------------------------------------------------- her things

def thimble_model():
    """A gold thimble from her back, tossed: it tumbles end over end about its middle."""
    m = Model("tossed_thimble", 32, (1.0, 1.0))
    m.bone("root")
    m.bone("tumble", "root", (0, 4, 0))
    m.cube("tumble", "thimble_side", (-3, 1, -3), (6, 6, 6))
    m.cube("tumble", "thimble_top", (-2, 7, -2), (4, 1, 4))
    m.cube("tumble", "thimble_rim", (-3.5, 0.5, -3.5), (7, 1, 7))
    return m


def thimble_animations():
    return animations({"animation.tossed_thimble.tumble": clip(0.5, {"tumble": {"rotation": keys({0.0: (0, 0, 0),
                                                                                               0.5: (360, 0, 0)})}})})


def spool_model():
    """One of her own spools of thread, on its side, rolling along its axle (x): two wooden flanges and the thread
    wound between them."""
    m = Model("rolling_spool", 128, (2.0, 2.0))
    m.bone("root")
    m.bone("roll", "root", (0, 12, 0))
    m.cube("roll", "spool_thread", (-9, 3, -9), (18, 18, 18))
    m.cube("roll", "spool_flange", (-12, 0, -12), (3, 24, 24))
    m.cube("roll", "spool_flange", (9, 0, -12), (3, 24, 24), mirror=True)
    m.cube("roll", "spool_axle", (-13, 10, -2), (26, 4, 4))
    return m


def spool_animations():
    return animations({"animation.rolling_spool.roll": clip(1.0, {"roll": {"rotation": keys({0.0: (0, 0, 0),
                                                                                           1.0: (360, 0, 0)})}})})


def egg_sac_model():
    """A cluster of her egg sacs at the doily's edge: three swollen white sacs bound in silk."""
    m = Model("tatter_egg_sac", 64, (1.5, 1.5))
    m.bone("root")
    m.bone("sacs", "root", (0, 0, 0))
    m.cube("sacs", "sac_big", (-5, 0, -4), (9, 10, 9))
    m.cube("sacs", "sac", (3, 0, -2), (6, 7, 6))
    m.cube("sacs", "sac", (-7, 0, 3), (6, 7, 6), mirror=True)
    m.cube("sacs", "sac_wrap", (-6, 3, -5), (12, 2, 11))
    return m


def egg_sac_animations():
    """Pulsing slowly while they wait; swelling and splitting as they hatch."""
    return animations({
        "animation.tatter_egg_sac.pulse": clip(2.0, {"sacs": {"scale": wave(2.0, 1, (1, 1, 1), (0.05, 0.08, 0.05))}}),
        "animation.tatter_egg_sac.hatch": clip(1.0, {"sacs": {"scale": keys({0.0: (1, 1, 1), 0.6: (1.25, 1.35, 1.25),
                                                                             1.0: (1.1, 0.2, 1.1)})}}, loop=False),
    })


def spiderling_model():
    """One of her brood: a small quick spider, half a block across, red-eyed."""
    m = Model("tatter_spiderling", 32, (1.0, 0.5))
    m.bone("root")
    m.bone("body", "root", (0, 3, 0))
    m.cube("body", "ling_head", (-1.5, 2, -4), (3, 2, 2))
    m.cube("body", "ling_body", (-2, 2, -2), (4, 3, 5))
    for side, name in ((1, "left"), (-1, "right")):
        for i, z in enumerate((-2.0, 0.0, 2.0)):
            m.bone(f"ling_leg_{name}_{i}", "body", (side * 2.0, 3, z), rotation=(0, side * (30 - 30 * i), -side * 30))
            x0 = 2.0 if side > 0 else -7.0
            m.cube(f"ling_leg_{name}_{i}", "ling_leg", (x0, 2.5, z - 0.5), (5, 1, 1), mirror=side < 0)
    return m


def spiderling_animations():
    out = {}
    for side, name in ((1, "left"), (-1, "right")):
        for i in range(3):
            phase = 0.0 if (side > 0) == (i % 2 == 0) else 0.5
            out[f"ling_leg_{name}_{i}"] = {"rotation": wave(0.4, 1, (0, 0, 0), (0, side * 18, -side * 14), phase=phase)}
    return animations({
        "animation.tatter_spiderling.walk": clip(0.4, {**out, "body": {"position": wave(0.4, 2, (0, 0, 0), (0, 0.4, 0))}}),
        "animation.tatter_spiderling.idle": clip(2.0, {"body": {"rotation": wave(2.0, 1, (0, 0, 0), (3, 0, 0))}}),
    })


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"tatterlace": tatterlace_model, "tossed_thimble": thimble_model, "rolling_spool": spool_model,
          "tatter_egg_sac": egg_sac_model, "tatter_spiderling": spiderling_model}
ANIMATIONS = {"tatterlace": tatterlace_animations, "tossed_thimble": thimble_animations,
              "rolling_spool": spool_animations, "tatter_egg_sac": egg_sac_animations,
              "tatter_spiderling": spiderling_animations}
ENTITY_MODEL = {name: name for name in MODELS}
# The clips the Java names (TatterlaceEntity and her things).
CLIPS = {
    "tatterlace": ("sewing", "idle", "walk", "stab_windup", "stab", "toss", "thread_windup", "thread", "snare", "brace",
                   "kick", "climb", "hang", "pins", "unravel", "drop_windup", "drop", "open", "brood", "death",
                   "cuffs_calm", "cuffs_red"),
    "tossed_thimble": ("tumble",),
    "rolling_spool": ("roll",),
    "tatter_egg_sac": ("pulse", "hatch"),
    "tatter_spiderling": ("walk", "idle"),
}
# The bones each of Tatterlace's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"cuffs": tuple(f"cuffs_{name}" for name in LEGS)}
