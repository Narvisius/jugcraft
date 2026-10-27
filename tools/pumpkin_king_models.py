"""GeckoLib bodies for the Pumpkin King and his fight (docs/features/pumpkin-king.md): the King himself, his
Jack-o'-Straws, his head as it rolls (the Head Roll) and the pumpkin he tosses (the Gourd Toss). Geometry and animations
only, as plain dicts; no I/O. tools/pumpkin_king_art.py paints their sheets on the same box-UV regions, at
TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"). The builder and the posing
helpers are Vesperine's (tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0
at the feet, the model facing north (-z) with its left on +x; +x pitches a bone's top forward, -x raises a hanging arm
forward, a right arm turns outwards with +z and a left one with -z, and +y turns the front to the model's right. A
keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. CLIPS lists the clips the Java plays; CONTROLLERS the bones each of the King's controllers
besides "body" owns (a controller sets every channel of each bone it animates, so no bone has two).
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS", "HANG_LIFT", "STANDS"]

# ------------------------------------------------------------------------------------------------- the King
# A scarecrow three times the height of a man (his model stands 5.4 blocks to the top of his pumpkin, his crown's ears
# above it): long straw-stuffed legs in sacking trousers tied at the ankle, straw bursting out below each tie; a patched
# harvest coat of burlap, belted with rope, its tails hanging to his knees, its collar turned up; long arms in its
# sleeves, straw at the cuffs and the shoulders, and hands of twisted corn husk, four long blades a hand; a stick of a
# neck; for a head a giant carved pumpkin, its stem crowned by a band of dried vines and five ears of corn; a crow on his
# left shoulder; and his scythe in his right hand, a snath of bound corn stalks and a long rusted blade. His face is two
# sets of planes before the pumpkin's carved face (the "fire" controller's: his fire, orange or white-hot), his pumpkin is
# the "head" controller's (hidden while it rolls), and the pumpkin in his left hand the "held" controller's.
HIP_Y = 38
KNEE_Y = 19
SHOULDER_Y = 60
NECK_Y = 63
HEAD_Y = 66            # the pumpkin's foot
HEAD_SIZE = (20, 16, 18)
ARM_X = 11
HAND_Y = 29
# Hanging on his post, his arms along the crossbar: the post's crossbar is 5.5 blocks over his feet (tools/crowfield.py
# CROSSBAR_Y, WAIT), so his whole body is lifted HANG_LIFT pixels to bring his shoulders to it, and drawn HANG_FORWARD
# pixels toward the farmyard so his back clears the post.
HANG_LIFT = 27
HANG_FORWARD = 2
# How tall each model stands, in pixels, from its feet to the top of its tallest part (the audit compares them with the
# entities' sizes in Java).
STANDS = {"pumpkin_king": HEAD_Y + HEAD_SIZE[1] + 4, "jack_o_straw": 22}
# His scythe: the snath upright through the grip in his right hand; its blade, five turned segments of rusted iron
# sweeping out to his right and curving down to the point: length, width, angle from level (degrees).
SCYTHE_GRIP = (-ARM_X, 24.0, 0.0)
BLADE = ((9, 7, 10), (9, 6, -4), (9, 5, -18), (8, 4, -34), (6, 3, -52))


def pumpkin_head(m, bone, foot):
    """A giant carved pumpkin on `bone`, its foot at y = `foot`: the pumpkin, its stem, and the crown of dried vines and
    corn ears round the stem. The King's head and his rolling head are both this."""
    w, h, d = HEAD_SIZE
    top = foot + h
    m.cube(bone, "pumpkin", (-w / 2, foot, -d / 2), HEAD_SIZE)
    m.cube(bone, "stem", (-1.5, top, -1.5), (3, 4, 3), rotation=(0, 0, 8), pivot=(0, top, 0))
    # The crown: a band of twisted vines sitting on the pumpkin's shoulders, and the corn ears standing round it, each
    # with a husk leaf behind, leaning out; the front one the tallest.
    m.cube(bone, "crown_band", (-8.5, top - 1, -7.5), (17, 3, 15))
    for x, z, lean, tall in ((-6, -8, (-12, 0, 12), False), (4, -8, (-12, 0, -12), False), (-1, -8.5, (-14, 0, 0), True),
                             (-6, 6, (12, 0, 12), False), (4, 6, (12, 0, -12), False)):
        part, size = ("ear_tall", (2, 9, 2)) if tall else ("ear", (2, 7, 2))
        m.cube(bone, part, (x, top + 1, z), size, rotation=lean, pivot=(x + 1, top + 1, z + 1))
        m.cube(bone, "husk_leaf", (x - 0.5, top + 1, z + 1.5 if z < 0 else z - 0.5), (3, 6, 0), rotation=lean,
               pivot=(x + 1, top + 1, z + 1))


def face_planes(m, bone, foot, z, part):
    """The carved face as planes on `bone` just before a pumpkin whose foot is at `foot`: two eyes, a nose and a grin."""
    m.cube(bone, f"{part}_eye", (-7, foot + 9, z), (4, 3, 0))
    m.cube(bone, f"{part}_eye", (3, foot + 9, z), (4, 3, 0), mirror=True)
    m.cube(bone, f"{part}_nose", (-1, foot + 6.5, z), (2, 2, 0))
    m.cube(bone, f"{part}_grin", (-6, foot + 2, z), (12, 3, 0))


def kings_scythe(m, bone, grip):
    """The King's scythe on `bone`, upright through `grip` (x, y, z): a snath of bound corn stalks with twine round it in
    three places, a nib for his other hand, an iron socket, and the rusted blade; returns the blade's tip."""
    gx, gy, gz = grip
    top = gy + 48.0
    m.cube(bone, "snath", (gx - 1, gy - 22, gz - 1), (2, 70, 2))
    m.cube(bone, "snath_foot", (gx - 1.5, gy - 23, gz - 1.5), (3, 2, 3))
    for y in (gy + 8, gy + 22, gy + 36):
        m.cube(bone, "twine", (gx - 1.5, y, gz - 1.5), (3, 2, 3))
    m.cube(bone, "nib", (gx + 1, gy + 18, gz - 0.5), (5, 1, 1))
    m.cube(bone, "socket", (gx - 2, top - 4, gz - 2), (4, 4, 4))
    x, y = gx - 2.0, top - 0.5
    for i, (length, width, angle) in enumerate(BLADE):
        m.cube(bone, f"blade_{i}", (x - length, y - width, gz - 0.5), (length, width, 1), rotation=(0, 0, angle),
               pivot=(x, y, gz))
        a = math.radians(angle)
        dx = -(length - 1)
        x, y = x + dx * math.cos(a), y - dx * math.sin(a)
    return x, y


def pumpkin_king_model():
    m = Model("pumpkin_king", 256, (6.5, 8.0))
    m.bone("root")
    # His legs from the hips: a stuffed trouser leg each, tied at the ankle, straw bursting out below.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 5
        m.bone(f"thigh_{name}", "root", (x, HIP_Y, 0))
        m.cube(f"thigh_{name}", "thigh", (x - 3, KNEE_Y, -3), (6, 19, 6), mirror=mirror)
        m.bone(f"shin_{name}", f"thigh_{name}", (x, KNEE_Y, 0))
        m.cube(f"shin_{name}", "shin", (x - 2.5, 4, -2.5), (5, 16, 5), mirror=mirror)
        m.cube(f"shin_{name}", "ankle_tie", (x - 3, 3, -3), (6, 2, 6), mirror=mirror)
        m.cube(f"shin_{name}", "foot_straw", (x - 3.5, 0, -4.5), (7, 3, 8), mirror=mirror)
    # His hips, belted with rope; the coat's tails hang from them: two in front, parted, and one behind.
    m.bone("hips", "root", (0, HIP_Y, 0))
    m.cube("hips", "pelvis", (-8, 36, -4.5), (16, 7, 9))
    m.cube("hips", "belt", (-8.5, 41, -5), (17, 2, 10))
    m.bone("coat_back", "hips", (0, 42, 5))
    m.cube("coat_back", "tail_back", (-9, 22, 5), (18, 20, 1))
    m.bone("coat_front_right", "hips", (-4.5, 42, -5.5))
    m.cube("coat_front_right", "tail_front", (-9, 22, -6), (8, 20, 1))
    m.bone("coat_front_left", "hips", (4.5, 42, -5.5))
    m.cube("coat_front_left", "tail_front", (1, 22, -6), (8, 20, 1), mirror=True)
    # His chest, in the coat; its collar turned up; straw at the shoulders; the crow on his left shoulder.
    m.bone("chest", "hips", (0, 42, 0))
    m.cube("chest", "coat", (-9, 42, -5), (18, 20, 10))
    m.cube("chest", "collar", (-7, 60, -5.5), (14, 4, 11))
    m.cube("chest", "shoulder_straw", (-11, 57, -3), (3, 4, 6))
    m.cube("chest", "shoulder_straw", (8, 57, -3), (3, 4, 6), mirror=True)
    m.bone("crow", "chest", (ARM_X, SHOULDER_Y + 1, 0))
    m.cube("crow", "crow_body", (ARM_X - 1.5, SHOULDER_Y + 1, -2), (3, 3, 5))
    m.cube("crow", "crow_head", (ARM_X - 1, SHOULDER_Y + 3.5, -3.5), (2, 2, 2))
    m.cube("crow", "crow_beak", (ARM_X - 0.5, SHOULDER_Y + 4, -5), (1, 1, 2))
    m.cube("crow", "crow_tail", (ARM_X - 1.5, SHOULDER_Y + 1.5, 3), (3, 1, 3))
    # His arms in the coat's sleeves, straw at the cuffs, and hands of twisted corn husk.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * ARM_X
        m.bone(f"arm_{name}", "chest", (x, SHOULDER_Y, 0))
        m.cube(f"arm_{name}", "sleeve", (x - 3, 44, -3), (6, 17, 6), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (x, 45, 0))
        m.cube(f"forearm_{name}", "forearm", (x - 2.5, 31, -2.5), (5, 15, 5), mirror=mirror)
        m.cube(f"forearm_{name}", "cuff_straw", (x - 3, 28, -3), (6, 4, 6), mirror=mirror)
        m.bone(f"hand_{name}", f"forearm_{name}", (x, HAND_Y, 0))
        m.cube(f"hand_{name}", "palm", (x - 2, HAND_Y - 3, -2), (4, 3, 4), mirror=mirror)
        for i, spread in enumerate((-12, -4, 4, 12)):
            m.cube(f"hand_{name}", "husk", (x - 2 + i * 1.1, HAND_Y - 12, -0.5), (1, 10, 1), rotation=(0, 0, spread * side),
                   pivot=(x - 1.5 + i * 1.1, HAND_Y - 3, 0), mirror=mirror)
    # His scythe, turned in his hand by the body's clips through its grip.
    m.bone("scythe_grip", "hand_right", SCYTHE_GRIP)
    kings_scythe(m, "scythe_grip", SCYTHE_GRIP)
    # The pumpkin he lifts to toss (the "held" controller's), in his left hand.
    m.bone("held_pumpkin", "hand_left", (ARM_X, 24, 0))
    m.cube("held_pumpkin", "held", (ARM_X - 4, 17, -4), (8, 7, 8))
    # His neck, and his head: the pumpkin (the "head" controller's), and the fire in its face (the "fire" controller's:
    # orange, or white-hot), two sets of planes just before its carved face.
    m.bone("head", "chest", (0, NECK_Y, 0))
    m.cube("head", "neck", (-1.5, 61, -1.5), (3, 6, 3))
    m.bone("pumpkin", "head", (0, HEAD_Y, 0))
    pumpkin_head(m, "pumpkin", HEAD_Y)
    front = -HEAD_SIZE[2] / 2
    m.bone("flame", "pumpkin", (0, HEAD_Y + 8, front))
    face_planes(m, "flame", HEAD_Y, front - 0.05, "flame")
    m.bone("flame_white", "pumpkin", (0, HEAD_Y + 8, front))
    face_planes(m, "flame_white", HEAD_Y, front - 0.1, "white")
    return m


# The bones each of his controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"fire": ("flame", "flame_white"), "head": ("pumpkin",), "held": ("held_pumpkin",)}

# ------------------------------------------------------------------------------------------------- his poses
# Each a dict of bone -> rotation (added to the bone's rest rotation), or {"rotation": ..., "position": ...}.

REST = {}
# Hanging on his post, arms out along the crossbar, hands of husk drooping, his head lolled, his feet dangling; his scythe
# stands up from his right hand.
HANG = {"root": {"position": (0, HANG_LIFT, -HANG_FORWARD)}, "arm_right": (0, 0, 88), "arm_left": (0, 0, -88),
        "hand_right": (0, 0, -70), "hand_left": (0, 0, 70), "scythe_grip": (0, 0, -18), "thigh_right": (4, 0, 4),
        "thigh_left": (-2, 0, -4), "shin_right": (6, 0, 0), "shin_left": (8, 0, 0), "chest": (6, 0, 0),
        "head": (14, 0, 16), "coat_back": (4, 0, 0), "coat_front_right": (-3, 0, 0), "coat_front_left": (-3, 0, 0)}
# Straining against the post as he tears free: his arms wrenched forward off the bar, his head thrown up.
STRAIN = {**HANG, "arm_right": (-40, 0, 65), "arm_left": (-30, 0, -75), "chest": (-8, 0, 0), "head": (-25, 0, -6),
          "thigh_right": (-25, 0, 4), "shin_right": (35, 0, 0)}
STRAIN_OTHER = {**HANG, "arm_right": (-25, 0, 78), "arm_left": (-45, 0, -60), "chest": (-4, 0, 4), "head": (-18, 0, 10),
                "thigh_left": (-25, 0, -4), "shin_left": (35, 0, 0)}
# Dropped off the post, crouched where he landed.
LANDED = {"root": {"position": (0, -5, 0)}, "thigh_right": (-30, 0, 6), "thigh_left": (-30, 0, -6), "shin_right": (40, 0, 0),
          "shin_left": (40, 0, 0), "chest": (20, 0, 0), "head": (-12, 0, 0), "arm_right": (-25, 0, 30),
          "arm_left": (-25, 0, -30)}
# Drawing his scythe back over his right shoulder (the Scythe Sweep's wind-up); then the sweep across to his left.
SWEEP_BACK = {"chest": (-6, 40, 0), "head": (0, -25, 0), "arm_right": (-70, 0, 55), "forearm_right": (-35, 0, 0),
              "scythe_grip": (-40, 0, 0), "arm_left": (-30, 0, -20), "thigh_right": (-10, 0, 0), "thigh_left": (10, 0, 0)}
SWEEP_THROUGH = {"chest": (14, -45, 0), "head": (0, 20, 0), "arm_right": (-85, 0, -35), "forearm_right": (-10, 0, 0),
                 "scythe_grip": (-70, 0, 0), "arm_left": (-20, 0, -40), "thigh_right": (10, 0, 0),
                 "thigh_left": (-14, 0, 0)}
# Stooping to pluck a pumpkin from the rows; then lifting it high overhead in his left hand; then the throw.
PLUCK = {"root": {"position": (0, -3, 0)}, "chest": (40, 0, 0), "head": (-20, 0, 0), "arm_left": (-40, 0, -12),
         "forearm_left": (-20, 0, 0), "thigh_right": (-20, 0, 0), "thigh_left": (-20, 0, 0), "shin_right": (25, 0, 0),
         "shin_left": (25, 0, 0)}
HEFT = {"chest": (-10, 10, 0), "head": (-22, 0, 0), "arm_left": (-175, 0, -12), "forearm_left": (-10, 0, 0),
        "arm_right": (-15, 0, 15)}
THROW = {"chest": (25, -20, 0), "head": (10, 0, 0), "arm_left": (-55, 0, -10), "forearm_left": (-5, 0, 0),
         "arm_right": (-10, 0, 20), "thigh_left": (-18, 0, 0), "thigh_right": (14, 0, 0)}
# Flinging his arms wide for the Crow Storm, his head thrown back.
WIDE = {"chest": (-15, 0, 0), "head": (-30, 0, 0), "arm_right": (-20, 0, 100), "arm_left": (-20, 0, -100),
        "forearm_right": (0, 0, 10), "forearm_left": (0, 0, -10), "coat_back": (12, 0, 0)}
# Arms raised high to call the harvest, the crows crying.
RAISE = {"chest": (-10, 0, 0), "head": (-35, 0, 0), "arm_right": (-168, 0, 25), "arm_left": (-168, 0, -25),
         "forearm_right": (-10, 0, 0), "forearm_left": (-10, 0, 0)}
# Down on one knee, his left hand driven into the earth (the Vine Snare's wind-up); then he yanks it up.
DELVE = {"root": {"position": (0, -11, 0)}, "thigh_right": (-75, 0, 6), "shin_right": (85, 0, 0), "thigh_left": (-20, 0, -6),
         "shin_left": (95, 0, 0), "chest": (50, 0, 0), "head": (-35, 0, 0), "arm_left": (-15, 0, -12),
         "forearm_left": (-10, 0, 0), "arm_right": (-20, 0, 20), "coat_back": (-30, 0, 0)}
YANK = {"root": {"position": (0, -4, 0)}, "chest": (10, 0, 0), "head": (-10, 0, 0), "arm_left": (-125, 0, -30),
        "forearm_left": (-15, 0, 0), "thigh_right": (-30, 0, 6), "shin_right": (35, 0, 0)}
# Lifting his head from his shoulders with both hands; then bending to set it rolling.
LIFT_HEAD = {"arm_right": (-160, 0, 38), "arm_left": (-160, 0, -38), "forearm_right": (-25, 0, 0),
             "forearm_left": (-25, 0, 0), "head": {"rotation": (0, 0, 0), "position": (0, 7, 0)}, "chest": (-6, 0, 0)}
SET_DOWN = {"root": {"position": (0, -6, 0)}, "chest": (55, 0, 0), "arm_right": (-75, 0, 14), "arm_left": (-75, 0, -14),
            "forearm_right": (-15, 0, 0), "forearm_left": (-15, 0, 0),
            "head": {"rotation": (-55, 0, 0), "position": (0, -26, -16)}, "thigh_right": (-35, 0, 0),
            "thigh_left": (-35, 0, 0), "shin_right": (45, 0, 0), "shin_left": (45, 0, 0)}
# Headless while his head rolls: his arms out before him, groping.
GROPE = {"chest": (8, 0, 0), "arm_right": (-70, 0, 18), "arm_left": (-60, 0, -22), "forearm_right": (-10, 0, 0),
         "forearm_left": (-15, 0, 0)}
# Bereft: sagging on his straw legs, his head lolled, his arms hanging.
SAG = {"root": {"position": (0, -8, 0)}, "thigh_right": (-30, 0, 8), "thigh_left": (-26, 0, -8), "shin_right": (45, 0, 0),
       "shin_left": (40, 0, 0), "chest": (35, 0, 8), "head": (25, 10, 22), "arm_right": (12, 0, 6), "arm_left": (8, 0, -4),
       "hand_right": (0, 0, -10), "scythe_grip": (30, 0, 0)}
# The Crowning: back on his post, flaring, his head thrown back.
CROWNED = {**HANG, "chest": (-12, 0, 0), "head": (-32, 0, 0), "arm_right": (-10, 0, 92), "arm_left": (-10, 0, -92)}
# Fallen: his head has dropped from his shoulders and rolled away before him; his straw collapsed.
HEAD_OFF = {"head": {"rotation": (160, 0, 40), "position": (0, -48, -30)}, "chest": (-10, 0, 0)}
COLLAPSED = {"root": {"position": (0, -26, 0)}, "thigh_right": (-85, 0, 14), "thigh_left": (-80, 0, -12),
             "shin_right": (100, 0, 0), "shin_left": (95, 0, 0), "chest": (75, 0, 12), "arm_right": (-20, 0, 50),
             "arm_left": (-15, 0, -45), "head": {"rotation": (200, 0, 60), "position": (0, -40, -60)},
             "scythe_grip": (80, 0, 0), "coat_back": (-40, 0, 0)}


def looped(length, pose, waves):
    """A looping clip holding `pose`, with `waves` ({bone: {channel: keyframes}}) moving those of its bones about their
    place in it (each wave's base is the pose's own), and the rest of it held still."""
    bones = {}
    for bone, value in pose.items():
        for channel, held in (value if isinstance(value, dict) else {"rotation": value}).items():
            bones.setdefault(bone, {})[channel] = still(held)
    for bone, extra in waves.items():
        bones.setdefault(bone, {}).update(extra)
    return clip(length, bones)


def _sway(length, cycles=1, scale=1.0):
    """Standing: his chest swaying a little, his head tilting, his coat's tails and his crow stirring."""
    s = scale
    return {
        "chest": {"rotation": wave(length, cycles, (0, 0, 0), (1.5 * s, 0, 2 * s))},
        "head": {"rotation": wave(length, cycles, (0, 0, 0), (3 * s, 6 * s, 4 * s), phase=0.2)},
        "coat_back": {"rotation": wave(length, cycles, (3, 0, 0), (2 * s, 0, 0), phase=0.4)},
        "coat_front_right": {"rotation": wave(length, cycles, (-2, 0, 0), (-2 * s, 0, 0), phase=0.3)},
        "coat_front_left": {"rotation": wave(length, cycles, (-2, 0, 0), (-2 * s, 0, 0), phase=0.55)},
        "crow": {"rotation": wave(length, cycles * 2, (0, 0, 0), (0, 25 * s, 0), phase=0.1)},
    }


def _stride(length, scale=1.0):
    """Striding: his long legs swinging from the hips, each shin folding as its foot lifts, his arms swinging loose
    against his legs, his coat's tails flapping and his head bobbing."""
    s = scale
    out = {}
    for name, phase in (("right", 0.0), ("left", 0.5)):
        out[f"thigh_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (28 * s, 0, 0), phase=phase)}
        out[f"shin_{name}"] = {"rotation": wave(length, 1, (14 * s, 0, 0), (14 * s, 0, 0), phase=phase + 0.25)}
        out[f"arm_{name}"] = {"rotation": wave(length, 1, (0, 0, 6 if name == "right" else -6), (22 * s, 0, 0),
                                               phase=(phase + 0.5) % 1.0)}
        out[f"forearm_{name}"] = {"rotation": wave(length, 1, (-8, 0, 0), (-8 * s, 0, 0), phase=(phase + 0.75) % 1.0)}
    out["chest"] = {"rotation": wave(length, 2, (4, 0, 0), (2 * s, 0, 3 * s))}
    out["head"] = {"rotation": wave(length, 2, (-4, 0, 0), (-3 * s, 0, -4 * s), phase=0.1)}
    out["coat_back"] = {"rotation": wave(length, 2, (8, 0, 0), (5 * s, 0, 0))}
    out["coat_front_right"] = {"rotation": wave(length, 1, (-6, 0, 0), (-8 * s, 0, 0))}
    out["coat_front_left"] = {"rotation": wave(length, 1, (-6, 0, 0), (-8 * s, 0, 0), phase=0.5)}
    out["root"] = {"position": wave(length, 2, (0, 0, 0), (0, 1.2 * s, 0), phase=0.25)}
    return out


def _fire(flame, white, flicker=0.0, cycles=4):
    """A fire clip: the orange planes at `flame`, flickering by `flicker`, and the white-hot planes at `white`."""
    lit = wave(1.0, cycles, (flame, flame, flame), (flicker, flicker, 0.0)) if flicker else still((flame, flame, flame))
    return clip(1.0, {"flame": {"scale": lit}, "flame_white": {"scale": still((white, white, white))}})


def pumpkin_king_animations():
    """His body's clips (the "body" controller), his fire (dark, lit, white-hot, guttering), his head (on his shoulders,
    or off them, rolling) and the pumpkin in his hand. The body's never key a bone of the others, nor theirs a body bone."""
    return animations({
        "animation.pumpkin_king.hang": looped(4.0, HANG, {
            "hand_right": {"rotation": wave(4.0, 1, HANG["hand_right"], (6, 0, 4))},
            "hand_left": {"rotation": wave(4.0, 1, HANG["hand_left"], (6, 0, -4), phase=0.5)},
            "coat_back": {"rotation": wave(4.0, 1, HANG["coat_back"], (3, 0, 0), phase=0.3)},
            "crow": {"rotation": wave(4.0, 2, (0, 0, 0), (0, 30, 0))}}),
        "animation.pumpkin_king.tear_free": posed(2.5, [(0.0, HANG), (0.4, STRAIN), (0.75, STRAIN_OTHER), (1.1, STRAIN),
                                                        (1.45, STRAIN_OTHER), (1.8, LANDED), (2.15, LANDED),
                                                        (2.5, REST)]),
        "animation.pumpkin_king.idle": clip(2.5, _sway(2.5)),
        "animation.pumpkin_king.walk": clip(1.2, _stride(1.2)),
        "animation.pumpkin_king.sweep_windup": posed(0.8, [(0.0, REST), (0.55, SWEEP_BACK), (0.8, SWEEP_BACK)]),
        "animation.pumpkin_king.sweep": posed(0.7, [(0.0, SWEEP_BACK), (0.12, SWEEP_THROUGH), (0.35, SWEEP_THROUGH),
                                                    (0.7, REST)]),
        "animation.pumpkin_king.toss_windup": posed(1.0, [(0.0, REST), (0.35, PLUCK), (0.75, HEFT), (1.0, HEFT)]),
        "animation.pumpkin_king.toss": posed(0.6, [(0.0, HEFT), (0.15, THROW), (0.6, REST)]),
        "animation.pumpkin_king.storm_windup": posed(1.2, [(0.0, REST), (0.6, WIDE), (1.2, WIDE)]),
        "animation.pumpkin_king.storm": looped(1.0, WIDE, {
            "forearm_right": {"rotation": wave(1.0, 2, WIDE["forearm_right"], (0, 0, 12))},
            "forearm_left": {"rotation": wave(1.0, 2, WIDE["forearm_left"], (0, 0, -12), phase=0.5)},
            "coat_front_right": {"rotation": wave(1.0, 3, (-20, 0, 0), (-10, 0, 0))},
            "coat_front_left": {"rotation": wave(1.0, 3, (-20, 0, 0), (-10, 0, 0), phase=0.4)}}),
        "animation.pumpkin_king.call": posed(1.5, [(0.0, REST), (0.5, RAISE), (1.5, RAISE)], waves={
            "head": {"rotation": wave(1.5, 3, RAISE["head"], (4, 0, 6))}}),
        "animation.pumpkin_king.snare_windup": posed(1.2, [(0.0, REST), (0.6, DELVE), (1.2, DELVE)]),
        "animation.pumpkin_king.snare": posed(0.65, [(0.0, DELVE), (0.2, YANK), (0.65, REST)]),
        "animation.pumpkin_king.roll_windup": posed(1.5, [(0.0, REST), (0.6, LIFT_HEAD), (1.2, SET_DOWN), (1.5, SET_DOWN)]),
        "animation.pumpkin_king.roll": looped(1.0, GROPE, {
            "chest": {"rotation": wave(1.0, 1, GROPE["chest"], (2, 8, 0))},
            "arm_right": {"rotation": wave(1.0, 2, GROPE["arm_right"], (8, 0, 6))},
            "arm_left": {"rotation": wave(1.0, 2, GROPE["arm_left"], (8, 0, -6), phase=0.5)}}),
        "animation.pumpkin_king.stagger": looped(1.0, SAG, {
            "chest": {"rotation": wave(1.0, 1, SAG["chest"], (5, 0, 8))},
            "head": {"rotation": wave(1.0, 2, SAG["head"], (6, 10, 6), phase=0.2)},
            "root": {"position": wave(1.0, 1, SAG["root"]["position"], (1.0, 1.0, 0))}}),
        "animation.pumpkin_king.crown": posed(2.0, [(0.0, REST), (0.4, HANG), (0.7, CROWNED), (1.7, CROWNED),
                                                   (2.0, REST)]),
        "animation.pumpkin_king.death": posed(2.5, [(0.0, REST), (0.3, {"chest": (-15, 0, 0), "head": (-20, 0, 0)}),
                                                    (0.9, HEAD_OFF), (1.8, COLLAPSED), (2.5, COLLAPSED)]),
        # His fire: dark on his post and when he falls, burning orange, white-hot (the Crowning, the Last Harvest), and
        # guttering low while he is bereft.
        "animation.pumpkin_king.fire_dark": _fire(0.0, 0.0),
        "animation.pumpkin_king.fire_lit": _fire(1.0, 0.0, flicker=0.05),
        "animation.pumpkin_king.fire_white": _fire(0.0, 1.0),
        "animation.pumpkin_king.fire_gutter": _fire(0.55, 0.0, flicker=0.3, cycles=6),
        # His head on his shoulders, or off them (rolling).
        "animation.pumpkin_king.head_on": clip(1.0, {"pumpkin": {"scale": still((1, 1, 1))}}),
        "animation.pumpkin_king.head_off": clip(1.0, {"pumpkin": {"scale": still((0, 0, 0))}}),
        # The pumpkin he lifts to toss.
        "animation.pumpkin_king.held_none": clip(1.0, {"held_pumpkin": {"scale": still((0, 0, 0))}}),
        "animation.pumpkin_king.held_pumpkin": clip(1.0, {"held_pumpkin": {"scale": still((1, 1, 1))}}),
    })


# ------------------------------------------------------------------------------------------------- his Jacks
# A Jack-o'-Straw: a little straw man (22 pixels, under a block and a half) in a sacking tunic belted with twine, legs
# and arms of bound straw, hands of husk, and for a head a carved gourd lit inside (its face on the glowmask).

def jack_o_straw_model():
    m = Model("jack_o_straw", 64, (1.2, 1.6))
    m.bone("root")
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 1.5
        m.bone(f"leg_{name}", "root", (x, 8, 0))
        m.cube(f"leg_{name}", "jack_leg", (x - 1, 0, -1), (2, 8, 2), mirror=mirror)
    m.bone("body", "root", (0, 8, 0))
    m.cube("body", "tunic", (-3, 7, -2), (6, 7, 4))
    m.cube("body", "jack_belt", (-3.5, 9, -2.5), (7, 1, 5))
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 4
        m.bone(f"arm_{name}", "body", (x, 13, 0))
        m.cube(f"arm_{name}", "jack_arm", (x - 1, 6, -1), (2, 7, 2), mirror=mirror)
        m.cube(f"arm_{name}", "jack_husk", (x - 1, 3, -1), (2, 3, 2), mirror=mirror)
    m.bone("head", "body", (0, 14, 0))
    m.cube("head", "gourd", (-3.5, 14, -3.5), (7, 6, 7))
    m.cube("head", "gourd_stem", (-0.5, 20, -0.5), (1, 2, 1))
    return m


JACK_REST = {}
JACK_CROUCHED = {"root": {"position": (0, -12, 0)}, "body": (-10, 0, 0), "head": (-30, 0, 0), "arm_right": (-170, 0, 20),
                 "arm_left": (-170, 0, -20)}
JACK_RISING = {"root": {"position": (0, -5, 0)}, "body": (30, 0, 0), "head": (-15, 0, 0), "arm_right": (-90, 0, 30),
               "arm_left": (-90, 0, -30), "leg_right": (-40, 0, 0), "leg_left": (20, 0, 0)}
JACK_RAISE = {"arm_right": (-150, 0, 20), "body": (-8, 10, 0), "head": (-10, 0, 0)}
JACK_SLASH = {"arm_right": (-30, 0, -30), "body": (20, -20, 0), "head": (10, 0, 0), "arm_left": (-20, 0, -10)}
JACK_FALLEN = {"root": {"position": (0, -7, 0)}, "body": (70, 0, 10), "head": {"rotation": (40, 0, 30),
                                                                              "position": (0, -3, -2)},
               "arm_right": (-20, 0, 40), "arm_left": (-10, 0, -50), "leg_right": (-80, 0, 10), "leg_left": (-70, 0, -15)}


def jack_o_straw_animations():
    return animations({
        "animation.jack_o_straw.climb": posed(1.0, [(0.0, JACK_CROUCHED), (0.5, JACK_RISING), (1.0, JACK_REST)]),
        "animation.jack_o_straw.idle": clip(1.5, {
            "body": {"rotation": wave(1.5, 1, (4, 0, 0), (2, 0, 4))},
            "head": {"rotation": wave(1.5, 1, (0, 0, 0), (4, 12, 6), phase=0.25)},
            "arm_right": {"rotation": wave(1.5, 1, (-10, 0, 6), (6, 0, 4))},
            "arm_left": {"rotation": wave(1.5, 1, (-10, 0, -6), (6, 0, -4), phase=0.5)}}),
        "animation.jack_o_straw.walk": clip(0.5, {
            "leg_right": {"rotation": wave(0.5, 1, (0, 0, 0), (40, 0, 0))},
            "leg_left": {"rotation": wave(0.5, 1, (0, 0, 0), (-40, 0, 0))},
            "arm_right": {"rotation": wave(0.5, 1, (-20, 0, 6), (-30, 0, 0))},
            "arm_left": {"rotation": wave(0.5, 1, (-20, 0, -6), (30, 0, 0))},
            "body": {"rotation": wave(0.5, 2, (10, 0, 0), (3, 0, 0))},
            "head": {"rotation": wave(0.5, 2, (-6, 0, 0), (-4, 0, 5), phase=0.2)},
            "root": {"position": wave(0.5, 2, (0, 0, 0), (0, 0.8, 0), phase=0.25)}}),
        "animation.jack_o_straw.swipe": posed(0.4, [(0.0, JACK_REST), (0.1, JACK_RAISE), (0.25, JACK_SLASH), (0.4, JACK_REST)]),
        "animation.jack_o_straw.dazed": clip(1.0, {
            "head": {"rotation": wave(1.0, 2, (10, 0, 0), (8, 25, 15))},
            "body": {"rotation": wave(1.0, 1, (6, 0, 0), (4, 0, 10), phase=0.3)},
            "arm_right": {"rotation": wave(1.0, 1, (5, 0, 10), (5, 0, 5))},
            "arm_left": {"rotation": wave(1.0, 1, (5, 0, -10), (5, 0, -5), phase=0.5)},
            "root": {"position": wave(1.0, 1, (0, -1, 0), (0.6, 0, 0))}}),
        "animation.jack_o_straw.crumble": posed(1.0, [(0.0, JACK_REST), (0.5, JACK_FALLEN), (1.0, JACK_FALLEN)]),
    })


# ------------------------------------------------------------------------------------------------- his head, rolling
# The King's head as it rolls: his pumpkin and its crown, turned about its middle (the entity's feet on the floor).

def kings_head_model():
    m = Model("kings_head", 128, (2.0, 2.0))
    m.bone("spin", None, (0, HEAD_SIZE[1] / 2, 0))
    pumpkin_head(m, "spin", 0)
    face_planes(m, "spin", 0, -HEAD_SIZE[2] / 2 - 0.05, "flame")
    return m


def kings_head_animations():
    # Rolling forward (its top over toward its way) about four times a second: 18 blocks in 1.2 seconds.
    return animations({"animation.kings_head.roll": clip(0.25, {"spin": {"rotation": keys({0.0: (0, 0, 0),
                                                                                           0.25: (360, 0, 0)})}})})


# ------------------------------------------------------------------------------------------------- the tossed pumpkin
# A pumpkin from his rows, carved, burning inside, tumbling as it flies.

def kings_pumpkin_model():
    m = Model("kings_pumpkin", 64, (1.0, 1.0))
    m.bone("tumble", None, (0, 4.5, 0))
    m.cube("tumble", "small_pumpkin", (-5, 0, -5), (10, 9, 10))
    m.cube("tumble", "small_stem", (-1, 9, -1), (2, 2, 2))
    m.cube("tumble", "small_eye", (-4, 5, -5.05), (3, 2, 0))
    m.cube("tumble", "small_eye", (1, 5, -5.05), (3, 2, 0), mirror=True)
    m.cube("tumble", "small_grin", (-3, 2, -5.05), (6, 2, 0))
    return m


def kings_pumpkin_animations():
    return animations({"animation.kings_pumpkin.tumble": clip(0.6, {"tumble": {"rotation": keys({0.0: (0, 0, 0),
                                                                                                 0.6: (360, 180, 0)})}})})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"pumpkin_king": pumpkin_king_model, "jack_o_straw": jack_o_straw_model, "kings_head": kings_head_model,
          "kings_pumpkin": kings_pumpkin_model}
ANIMATIONS = {"pumpkin_king": pumpkin_king_animations, "jack_o_straw": jack_o_straw_animations,
              "kings_head": kings_head_animations, "kings_pumpkin": kings_pumpkin_animations}
# The clips the Java names (PumpkinKingEntity, JackOStrawEntity, KingsHeadEntity, KingsPumpkinEntity).
CLIPS = {
    "pumpkin_king": ("hang", "tear_free", "idle", "walk", "sweep_windup", "sweep", "toss_windup", "toss", "storm_windup",
                     "storm", "call", "snare_windup", "snare", "roll_windup", "roll", "stagger", "crown", "death",
                     "fire_dark", "fire_lit", "fire_white", "fire_gutter", "head_on", "head_off", "held_none",
                     "held_pumpkin"),
    "jack_o_straw": ("climb", "idle", "walk", "swipe", "dazed", "crumble"),
    "kings_head": ("roll",),
    "kings_pumpkin": ("tumble",),
}
# The clip that belongs to each of the King's controllers besides "body", by its name's start.
CONTROLLER_CLIPS = {"fire": "fire_", "head": "head_", "held": "held_"}


def owner(clip_name):
    """Which of the King's controllers plays `clip_name` (its last part)."""
    for controller, prefix in CONTROLLER_CLIPS.items():
        if clip_name.startswith(prefix):
            return controller
    return "body"


def _check():
    """The models pack; no clip of the King's body keys a bone of his other controllers, nor theirs any other bone; every
    clip the Java plays is here."""
    for build in MODELS.values():
        build().pack()
    owned = {bone: controller for controller, bones in CONTROLLERS.items() for bone in bones}
    for clip_name, body in pumpkin_king_animations()["animations"].items():
        mine = owner(clip_name.split(".")[-1])
        for bone in body["bones"]:
            if owned.get(bone, "body") != mine:
                raise ValueError(f"{clip_name} keys {bone}")
    for entity, build in ANIMATIONS.items():
        have = {c.split(".")[-1] for c in build()["animations"]}
        if set(CLIPS[entity]) != have:
            raise ValueError(f"{entity} clips {sorted(set(CLIPS[entity]) ^ have)}")
    return True


if __name__ == "__main__":
    print(_check())
