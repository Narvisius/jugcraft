"""The GeckoLib body of the Alpha Werewolf (docs/features/alpha-werewolf.md). Geometry and animations only, as plain dicts;
no I/O. tools/alpha_werewolf_art.py paints his sheet on the same box-UV regions, at TEXTURE_SCALE times the size the
model declares (docs/ART_DIRECTION.md, "High resolution"). The builder and the posing helpers are Vesperine's
(tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0 at the feet, the model
facing north (-z) with its left on +x; +x pitches a bone's top forward, -x raises a hanging arm forward, a right arm
turns outwards with +z and a left one with -z, and +y turns the front to the model's right. A keyframed rotation adds to
a bone's rest rotation. His Packmates are drawn with the werewolves' own model (client/WerewolfModel.java), not here.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. CLIPS lists the clips the Java plays.
"""
from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS"]

# A werewolf twice the height of a man, the werewolves' build made massive: a barrel chest hunched forward over narrow
# hips, a hump of silver-grey mane across his shoulders and a ruff at his throat; a wolf's head thrust forward on a
# thick neck, a long muzzle with a black nose, fangs over a jaw that drops open, a heavy brow, cheek tufts, his left ear
# pricked and his right torn short; long heavy arms hanging to his knees, each a furred shoulder, upper arm, forearm and a
# hand of four hooked claws; legs bent like a wolf's hind legs (thigh forward, shin back, a long hock forward again onto
# a clawed paw); and a bushy tail in three parts drooping behind. His eyes are two planes before his skull (the "eyes"
# controller's: shown once he wakes, glowing moon-white); the body's clips never key them.
HIP_Y = 30
KNEE_Y = 18
HOCK_Y = 8
PAW_Y = -1
CHEST_PITCH = 40           # his chest leans this far forward at rest; his head is turned back level by as much
ARM_HANG = -50             # his arms' rest pitch (raised forward against his chest's lean, so they hang a little ahead)
LEG = (-30, 70, -55, 15)   # his legs' rest bends: thigh forward, shin back, hock forward, paw level


def alpha_werewolf_model():
    m = Model("alpha_werewolf", 256, (3.0, 4.0))
    m.bone("root")
    # His legs, each from its hip: thigh, shin, a long hock and a paw with four claws; bent at rest like a wolf's.
    thigh, shin, hock, paw = LEG
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 6
        m.bone(f"thigh_{name}", "root", (x, HIP_Y, 2), rotation=(thigh, 0, 0))
        m.cube(f"thigh_{name}", "thigh", (x - 4.5, KNEE_Y, -3), (9, 12, 10), mirror=mirror)
        m.bone(f"shin_{name}", f"thigh_{name}", (x, KNEE_Y, 2), rotation=(shin, 0, 0))
        m.cube(f"shin_{name}", "shin", (x - 3.5, HOCK_Y, -1.5), (7, 10, 7), mirror=mirror)
        m.bone(f"hock_{name}", f"shin_{name}", (x, HOCK_Y, 2), rotation=(hock, 0, 0))
        m.cube(f"hock_{name}", "hock", (x - 2.5, PAW_Y, -0.5), (5, 9, 5), mirror=mirror)
        m.bone(f"paw_{name}", f"hock_{name}", (x, PAW_Y, 2), rotation=(paw, 0, 0))
        m.cube(f"paw_{name}", "paw", (x - 3.5, PAW_Y - 3, -4), (7, 3, 9), mirror=mirror)
        for i in range(4):
            m.cube(f"paw_{name}", "toe_claw", (x - 3 + 1.75 * i, PAW_Y - 3, -5), (1, 2, 1), mirror=mirror)
    # His hips, and the tail drooping from them in three parts.
    m.bone("hips", "root", (0, HIP_Y, 2))
    m.cube("hips", "pelvis", (-8, HIP_Y - 4, -3), (16, 9, 11))
    m.bone("tail", "hips", (0, HIP_Y + 3, 8), rotation=(-35, 0, 0))
    m.cube("tail", "tail_root", (-2, HIP_Y + 1, 8), (4, 4, 8))
    m.bone("tail_mid", "tail", (0, HIP_Y + 3, 16), rotation=(-15, 0, 0))
    m.cube("tail_mid", "tail_brush", (-3, HIP_Y + 0.5, 16), (6, 5, 9))
    m.bone("tail_tip", "tail_mid", (0, HIP_Y + 3, 25), rotation=(-10, 0, 0))
    m.cube("tail_tip", "tail_tip", (-2, HIP_Y + 1, 25), (4, 4, 7))
    # His chest, leaning forward from the waist: the barrel of it, the hump of mane over his shoulders, a ridge of mane
    # down his back and the ruff at his throat.
    m.bone("chest", "hips", (0, HIP_Y + 4, 2), rotation=(CHEST_PITCH, 0, 0))
    m.cube("chest", "chest", (-11, HIP_Y + 4, -6), (22, 17, 15))
    m.cube("chest", "mane", (-12.5, HIP_Y + 15, -4), (25, 11, 15))
    m.cube("chest", "mane_ridge", (-6, HIP_Y + 8, 8), (12, 9, 3))
    m.cube("chest", "ruff", (-9, HIP_Y + 10, -9), (18, 10, 4))
    # His head on a thick neck at the chest's top front, turned back level: the skull, its heavy brow, the long muzzle,
    # the nose, fangs, cheek tufts; the jaw (it drops open as he howls and bites); his ears, the right one torn short.
    m.bone("head", "chest", (0, 52, -4), rotation=(-CHEST_PITCH, 0, 0))
    m.cube("head", "neck", (-5, 47, -9), (10, 9, 7))
    m.cube("head", "skull", (-6, 50, -17), (12, 10, 10))
    m.cube("head", "brow", (-6.5, 58, -18), (13, 2, 3))
    m.cube("head", "muzzle", (-3.5, 51, -27), (7, 5, 10))
    m.cube("head", "nose", (-1.5, 55, -28), (3, 2, 2))
    m.cube("head", "fang", (-3, 49, -26), (1, 2, 1))
    m.cube("head", "fang", (2, 49, -26), (1, 2, 1), mirror=True)
    m.cube("head", "cheek", (-8, 50, -15), (3, 6, 5))
    m.cube("head", "cheek", (5, 50, -15), (3, 6, 5), mirror=True)
    m.bone("jaw", "head", (0, 51, -17))
    m.cube("jaw", "jaw", (-3, 48, -26), (6, 3, 9))
    m.cube("jaw", "fang", (-2.5, 51, -25), (1, 2, 1))
    m.cube("jaw", "fang", (1.5, 51, -25), (1, 2, 1), mirror=True)
    m.bone("ear_left", "head", (4, 60, -11), rotation=(-10, 0, -12))
    m.cube("ear_left", "ear", (2.5, 60, -12.5), (3, 7, 3), mirror=True)
    m.bone("ear_right", "head", (-4, 60, -11), rotation=(-10, 0, 20))
    m.cube("ear_right", "ear_torn", (-5.5, 60, -12.5), (3, 4, 3))
    # His eyes (the eyes controller's): two planes just before the skull's face, under the brow.
    m.bone("eyes", "head", (0, 55.5, -17))
    m.cube("eyes", "eye", (-4, 55, -17.05), (2, 1, 0))
    m.cube("eyes", "eye", (2, 55, -17.05), (2, 1, 0), mirror=True)
    # His arms from his shoulders: a pad of fur, the upper arm, the forearm and a hand of four hooked claws.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 12
        m.bone(f"arm_{name}", "chest", (x, 47, 0), rotation=(ARM_HANG, 0, -side * 8))
        m.cube(f"arm_{name}", "shoulder", (x - 4.5, 42, -4.5), (9, 8, 9), mirror=mirror)
        m.cube(f"arm_{name}", "upper_arm", (x - 3.5, 30, -3.5), (7, 14, 7), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (x, 31, 0), rotation=(-20, 0, 0))
        m.cube(f"forearm_{name}", "forearm", (x - 3, 17, -3), (6, 14, 6), mirror=mirror)
        m.bone(f"hand_{name}", f"forearm_{name}", (x, 17, 0))
        m.cube(f"hand_{name}", "hand", (x - 3.5, 12, -4), (7, 5, 8), mirror=mirror)
        for i in range(4):
            m.cube(f"hand_{name}", "claw", (x - 3 + 1.75 * i, 8, -4.5), (1, 5, 1), mirror=mirror)
    return m


# The eyes controller's bone; the body's clips never key it.
EYE_BONES = ("eyes",)

# ------------------------------------------------------------------------------------------------- poses
# Each a dict of bone -> rotation (added to the bone's rest rotation), or {"rotation": ..., "position": ...}.

REST = {}
# Crouched on the Howling Stone, his head raised to the moon, his hands on the stone before him.
CROUCH = {"root": {"position": (0, -5, 0)}, "thigh_right": (-25, 0, 0), "shin_right": (30, 0, 0), "hock_right": (-15, 0, 0),
          "paw_right": (10, 0, 0), "thigh_left": (-25, 0, 0), "shin_left": (30, 0, 0), "hock_left": (-15, 0, 0),
          "paw_left": (10, 0, 0), "chest": (12, 0, 0), "head": (-52, 0, 0), "arm_right": (-25, 0, -4),
          "forearm_right": (-10, 0, 0), "arm_left": (-25, 0, 4), "forearm_left": (-10, 0, 0), "tail": (-20, 0, 0)}
# Howling at the moon: risen, his chest up, his head thrown back and his jaw open, his arms out.
HOWL_UP = {"chest": (-18, 0, 0), "head": (-38, 0, 0), "jaw": (32, 0, 0), "arm_right": (10, 0, 18),
           "arm_left": (10, 0, -18), "forearm_right": (-10, 0, 0), "forearm_left": (-10, 0, 0), "tail": (10, 0, 0),
           "ear_left": (12, 0, 0), "ear_right": (12, 0, 0)}
# Filling his chest for the Rending Howl: hackles up, shoulders back, head drawn in.
HACKLES = {"chest": (-12, 0, 0), "head": (14, 0, 0), "arm_right": (20, 0, 12), "arm_left": (20, 0, -12),
           "ear_left": (24, 0, 0), "ear_right": (24, 0, 0), "tail": (25, 0, 0)}
# Reared up for the Claw Rake, both claws spread high.
REAR = {"root": {"position": (0, 1, 0)}, "chest": (-28, 0, 0), "head": (16, 0, 0), "jaw": (14, 0, 0),
        "arm_right": (-115, 0, 30), "forearm_right": (-25, 0, 0), "arm_left": (-115, 0, -30), "forearm_left": (-25, 0, 0)}
# The rakes: his right claw across and down; then his left.
RAKE_RIGHT = {"chest": (10, -18, 0), "head": (6, 0, 0), "jaw": (10, 0, 0), "arm_right": (-30, 0, -26),
              "forearm_right": (-10, 0, 0), "arm_left": (-110, 0, -26), "forearm_left": (-25, 0, 0)}
RAKE_LEFT = {"chest": (10, 18, 0), "head": (6, 0, 0), "jaw": (10, 0, 0), "arm_right": (-50, 0, 14),
             "forearm_right": (-20, 0, 0), "arm_left": (-30, 0, 26), "forearm_left": (-10, 0, 0)}
# Down in a crouch to spring (the Pounce, and before a leap): low, chest forward, arms drawn back, tail up.
COIL = {"root": {"position": (0, -4, 0)}, "thigh_right": (-20, 0, 0), "shin_right": (26, 0, 0), "hock_right": (-12, 0, 0),
        "paw_right": (6, 0, 0), "thigh_left": (-20, 0, 0), "shin_left": (26, 0, 0), "hock_left": (-12, 0, 0),
        "paw_left": (6, 0, 0), "chest": (16, 0, 0), "head": (-14, 0, 0), "arm_right": (35, 0, 10),
        "forearm_right": (-30, 0, 0), "arm_left": (35, 0, -10), "forearm_left": (-30, 0, 0), "tail": (30, 0, 0)}
# In the air: stretched out, his claws reaching ahead and his legs thrown back.
LEAP = {"chest": (8, 0, 0), "head": (-10, 0, 0), "jaw": (20, 0, 0), "arm_right": (-80, 0, 10), "forearm_right": (-5, 0, 0),
        "arm_left": (-80, 0, -10), "forearm_left": (-5, 0, 0), "thigh_right": (50, 0, 0), "shin_right": (-30, 0, 0),
        "thigh_left": (50, 0, 0), "shin_left": (-30, 0, 0), "tail": (25, 0, 0)}
# Landed: crouched over his claws.
LANDED = {"root": {"position": (0, -3, 0)}, "thigh_right": (-16, 0, 0), "shin_right": (22, 0, 0), "hock_right": (-10, 0, 0),
          "paw_right": (4, 0, 0), "thigh_left": (-16, 0, 0), "shin_left": (22, 0, 0), "hock_left": (-10, 0, 0),
          "paw_left": (4, 0, 0), "chest": (14, 0, 0), "head": (6, 0, 0), "arm_right": (-15, 0, 16),
          "forearm_right": (-20, 0, 0), "arm_left": (-15, 0, -16), "forearm_left": (-20, 0, 0)}
# Lunging low for the Hamstring Bite, jaws wide; then the snap.
LUNGE = {"root": {"position": (0, -3, 0)}, "chest": (26, 0, 0), "head": (-6, 0, 0), "jaw": (40, 0, 0),
         "arm_right": (40, 0, 14), "forearm_right": (-20, 0, 0), "arm_left": (40, 0, -14), "forearm_left": (-20, 0, 0),
         "thigh_right": (-16, 0, 0), "shin_right": (20, 0, 0), "thigh_left": (-16, 0, 0), "shin_left": (20, 0, 0)}
SNAP = dict(LUNGE, jaw=(0, 0, 0), head=(4, 0, 0), chest=(30, 0, 0))
# Hiding in a bell clearing: flat to the ground, his head low between his hands, his tail down.
HIDE = {"root": {"position": (0, -9, 0)}, "thigh_right": (-35, 0, 0), "shin_right": (55, 0, 0), "hock_right": (-30, 0, 0),
        "paw_right": (10, 0, 0), "thigh_left": (-35, 0, 0), "shin_left": (55, 0, 0), "hock_left": (-30, 0, 0),
        "paw_left": (10, 0, 0), "chest": (24, 0, 0), "head": (-14, 0, 0), "arm_right": (-40, 0, 6),
        "forearm_right": (-20, 0, 0), "arm_left": (-40, 0, -6), "forearm_left": (-20, 0, 0), "tail": (-15, 0, 0),
        "ear_left": (20, 0, 0), "ear_right": (20, 0, 0)}
# Flushed: staggering, his head shaken, an arm thrown up.
STAGGER = {"chest": (-6, 0, 10), "head": (20, 14, -12), "jaw": (12, 0, 0), "arm_right": (-100, 0, 30),
           "forearm_right": (-30, 0, 0), "arm_left": (10, 0, -24), "forearm_left": (-10, 0, 0), "tail": (-20, 0, 0)}
# Slumped dead on the turf.
SLUMP = {"root": {"position": (0, -12, 0)}, "thigh_right": (-60, 0, 10), "shin_right": (90, 0, 0), "hock_right": (-40, 0, 0),
         "thigh_left": (-60, 0, -10), "shin_left": (90, 0, 0), "hock_left": (-40, 0, 0), "chest": (40, 0, 8),
         "head": (30, 0, 16), "jaw": (10, 0, 0), "arm_right": (-20, 0, 40), "forearm_right": (-10, 0, 0),
         "arm_left": (-20, 0, -40), "forearm_left": (-10, 0, 0), "tail": (-25, 0, 0), "ear_left": (30, 0, 0),
         "ear_right": (30, 0, 0)}


def _breath(length, cycles=1, scale=1.0):
    """Standing: his chest heaving, his head and tail swaying a little."""
    s = scale
    return {
        "chest": {"rotation": wave(length, cycles, (0, 0, 0), (2.0 * s, 0, 0))},
        "head": {"rotation": wave(length, cycles, (0, 0, 0), (2.0 * s, 4.0 * s, 0), phase=0.2)},
        "tail": {"rotation": wave(length, cycles, (0, 0, 0), (0, 10.0 * s, 0), phase=0.4)},
        "tail_mid": {"rotation": wave(length, cycles, (0, 0, 0), (0, 8.0 * s, 0), phase=0.55)},
    }


def _lope(length, scale=1.0, lean=0.0):
    """Loping: his legs swinging alternately from the hips, each shin folding as its paw lifts, his arms swinging
    against his legs, his chest dipping at each stride and his tail streaming."""
    s = scale
    out = {}
    for name, phase in (("right", 0.0), ("left", 0.5)):
        out[f"thigh_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (28 * s, 0, 0), phase=phase)}
        out[f"shin_{name}"] = {"rotation": wave(length, 1, (12 * s, 0, 0), (14 * s, 0, 0), phase=phase + 0.25)}
        out[f"hock_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (-10 * s, 0, 0), phase=phase + 0.25)}
        out[f"arm_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (32 * s, 0, 0), phase=(phase + 0.5) % 1.0)}
        out[f"forearm_{name}"] = {"rotation": wave(length, 1, (-10, 0, 0), (-10 * s, 0, 0), phase=(phase + 0.75) % 1.0)}
    out["chest"] = {"rotation": wave(length, 2, (lean, 0, 0), (3 * s, 0, 0))}
    out["head"] = {"rotation": wave(length, 2, (-lean, 0, 0), (-3 * s, 0, 0), phase=0.1)}
    out["tail"] = {"rotation": wave(length, 1, (20 * s, 0, 0), (0, 12 * s, 0))}
    out["root"] = {"position": wave(length, 2, (0, -0.5, 0), (0, 1.0 * s, 0), phase=0.25)}
    return out


def _gallop(length):
    """Bounding to ground: legs and arms together, reaching and gathering, his body stretched low."""
    out = {}
    for name in ("right", "left"):
        lag = 0.08 if name == "left" else 0.0
        out[f"thigh_{name}"] = {"rotation": wave(length, 1, (10, 0, 0), (40, 0, 0), phase=lag)}
        out[f"shin_{name}"] = {"rotation": wave(length, 1, (10, 0, 0), (-25, 0, 0), phase=lag + 0.25)}
        out[f"arm_{name}"] = {"rotation": wave(length, 1, (-30, 0, 0), (55, 0, 0), phase=0.5 + lag)}
        out[f"forearm_{name}"] = {"rotation": wave(length, 1, (-10, 0, 0), (-20, 0, 0), phase=0.75 + lag)}
    out["chest"] = {"rotation": wave(length, 1, (14, 0, 0), (8, 0, 0), phase=0.25)}
    out["head"] = {"rotation": wave(length, 1, (-14, 0, 0), (-6, 0, 0), phase=0.25)}
    out["tail"] = {"rotation": wave(length, 1, (35, 0, 0), (8, 0, 0))}
    out["root"] = {"position": wave(length, 1, (0, 0, 0), (0, 2.0, 0), phase=0.0)}
    return out


def alpha_werewolf_animations():
    """His body's clips (the "body" controller) and his eyes (dark, lit). The body's never key the eyes."""
    return animations({
        "animation.alpha_werewolf.crouch": posed(4.0, [(0.0, CROUCH), (4.0, CROUCH)], loop=True, waves={
            "tail": {"rotation": wave(4.0, 1, CROUCH["tail"], (0, 6, 0))}}),
        "animation.alpha_werewolf.wake": posed(2.5, [(0.0, CROUCH), (0.5, CROUCH), (1.0, HOWL_UP), (2.1, HOWL_UP),
                                                     (2.5, REST)]),
        "animation.alpha_werewolf.idle": clip(2.0, _breath(2.0)),
        "animation.alpha_werewolf.walk": clip(0.8, _lope(0.8)),
        "animation.alpha_werewolf.run": clip(0.5, _gallop(0.5)),
        "animation.alpha_werewolf.rake_windup": posed(0.7, [(0.0, REST), (0.45, REAR), (0.7, REAR)]),
        "animation.alpha_werewolf.rake": posed(0.9, [(0.0, REAR), (0.1, RAKE_RIGHT), (0.3, RAKE_RIGHT), (0.4, RAKE_LEFT),
                                                     (0.6, RAKE_LEFT), (0.9, REST)]),
        "animation.alpha_werewolf.pounce_windup": posed(1.2, [(0.0, REST), (0.5, COIL), (1.2, COIL)],
                                                        waves={"tail": {"rotation": wave(1.2, 4, COIL["tail"], (0, 14, 0))}}),
        "animation.alpha_werewolf.pounce": posed(1.1, [(0.0, COIL), (0.1, LEAP), (0.45, LEAP), (0.55, LANDED),
                                                       (1.1, REST)]),
        "animation.alpha_werewolf.howl_windup": posed(1.0, [(0.0, REST), (0.6, HACKLES), (1.0, HACKLES)],
                                                      waves={"chest": {"position": wave(1.0, 2, (0, 0.5, 0), (0, 0.5, 0))}}),
        "animation.alpha_werewolf.howl": posed(0.75, [(0.0, HACKLES), (0.15, HOWL_UP), (0.55, HOWL_UP), (0.75, REST)]),
        "animation.alpha_werewolf.mark": posed(1.0, [(0.0, REST), (0.35, HOWL_UP), (0.8, HOWL_UP), (1.0, REST)]),
        "animation.alpha_werewolf.hide": posed(2.0, [(0.0, HIDE), (2.0, HIDE)], loop=True, waves={
            "chest": {"position": wave(2.0, 1, (0, 0, 0), (0, 0.4, 0))},
            "tail": {"rotation": wave(2.0, 1, HIDE["tail"], (0, 5, 0))}}),
        "animation.alpha_werewolf.spring": posed(0.6, [(0.0, COIL), (0.15, LEAP), (0.6, LEAP)]),
        "animation.alpha_werewolf.stagger": posed(1.0, [(0.0, STAGGER), (1.0, STAGGER)], loop=True, waves={
            "chest": {"rotation": wave(1.0, 1, STAGGER["chest"], (6, 0, 10))},
            "head": {"rotation": wave(1.0, 2, STAGGER["head"], (6, 18, 6), phase=0.2)},
            "root": {"position": wave(1.0, 1, (0, 0, 0), (1.0, 0, 0))}}),
        "animation.alpha_werewolf.bite_windup": posed(0.9, [(0.0, REST), (0.6, LUNGE), (0.9, LUNGE)]),
        "animation.alpha_werewolf.bite": posed(0.65, [(0.0, LUNGE), (0.08, SNAP), (0.3, SNAP), (0.65, REST)]),
        "animation.alpha_werewolf.gather": posed(2.0, [(0.0, REST), (0.25, COIL), (0.4, LEAP), (0.6, CROUCH),
                                                       (0.9, HOWL_UP), (1.8, HOWL_UP), (2.0, REST)]),
        "animation.alpha_werewolf.death": posed(2.5, [(0.0, REST), (0.4, HOWL_UP), (1.0, HOWL_UP), (1.8, SLUMP),
                                                      (2.5, SLUMP)]),
        # His eyes: dark while he waits on the stone, burning once he wakes.
        "animation.alpha_werewolf.eyes_dark": clip(1.0, {"eyes": {"scale": still((0, 0, 0))}}),
        "animation.alpha_werewolf.eyes_lit": clip(1.0, {"eyes": {"scale": still((1, 1, 1))}}),
    })


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"alpha_werewolf": alpha_werewolf_model}
ANIMATIONS = {"alpha_werewolf": alpha_werewolf_animations}
# The clips the Java names (AlphaWerewolfEntity).
CLIPS = {
    "alpha_werewolf": ("crouch", "wake", "idle", "walk", "run", "rake_windup", "rake", "pounce_windup", "pounce",
                       "howl_windup", "howl", "mark", "hide", "spring", "stagger", "bite_windup", "bite", "gather", "death",
                       "eyes_dark", "eyes_lit"),
}
# The bones each of his controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"eyes": EYE_BONES}


def _check():
    """The model packs; no clip of his body keys his eyes, nor theirs any other bone; every clip the Java plays is here."""
    for build in MODELS.values():
        build().pack()
    anims = alpha_werewolf_animations()["animations"]
    for clip_name, body in anims.items():
        owner = "eyes" if clip_name.split(".")[-1].startswith("eyes_") else "body"
        for bone in body["bones"]:
            if ("eyes" if bone in EYE_BONES else "body") != owner:
                raise ValueError(f"{clip_name} keys {bone}")
    have = {c.split(".")[-1] for c in anims}
    if set(CLIPS["alpha_werewolf"]) != have:
        raise ValueError(f"clips {sorted(set(CLIPS['alpha_werewolf']) ^ have)}")
    return True


if __name__ == "__main__":
    print(_check(), alpha_werewolf_model().pack() and "packed")
