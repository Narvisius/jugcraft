"""GeckoLib bodies for the Iron Dreadnought and his fight (docs/features/iron-dreadnought.md): the Dreadnought himself,
his Sparkers, the hot rivets his battery fires and the mortar shell he lobs. Geometry and animations only, as plain
dicts; no I/O. tools/iron_dreadnought_art.py paints their sheets on the same box-UV regions, at TEXTURE_SCALE times the
size the models declare (docs/ART_DIRECTION.md, "High resolution"). The builder and the posing helpers are Vesperine's
(tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0 at the feet, every
model facing north (-z) with its left on +x; +x pitches a bone's top forward, -x raises a hanging arm forward, a right
arm turns outwards with +z and a left one with -z, and +y turns the front to the model's right. A keyframed rotation adds
to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. MODELS, ANIMATIONS and CLIPS are keyed by model; CLIPS lists the clips the Java plays.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS", "BATTERY_X", "BATTERY_Y", "BATTERY_Z",
           "HATCH_Y", "COIL_TOP", "COIL_X", "MORTAR_Z"]

# ------------------------------------------------------------------------------------------------- the Dreadnought

# A walking fortress of riveted gunmetal, five blocks across his shoulders and six tall to the tops of his coils, on two
# piston legs: a boxy hull of olive-drab plate worn bright at the edges, a skirt of armour round his hips, a sloped glacis
# over his front and a ram striped yellow and black below it; a cupola on his deck with a searchlight for an eye (its
# shutter the "eye" controller's: down while he waits, raised once he wakes); two smoke stacks behind it and a mortar
# under a hatch between them; a hatch in his belly; armoured pauldrons with a ribbed tesla coil on each (the arcs that
# crackle at their tops are the "coils" controller's); a battery of five rivet guns under a steel shutter on his left
# pauldron's front; two arms, each a piston in a housing ending in a riveted fist; and a vent on each flank and one on his
# back, their flaps the "vents" controller's (shut, or blown open on the glowing furnace behind). The body's clips never
# key the shutter, the flaps or the arcs.
LEG_X = 16                 # each leg's line, either side of his middle
HIP_Y = 38
KNEE_Y = 22
ANKLE_Y = 6
HULL_BOTTOM = 36
DECK_Y = 75
SHOULDER_Y = 72           # his pauldrons' tops, where his coils stand
# Where the rivets leave his battery (the middle of its muzzles), at rest: ahead of his feet and over them, in pixels.
# IronDreadnoughtEntity's rivets leave from here (tools/iron_dreadnought.py RIVET BATTERY_AHEAD and BATTERY_UP).
BATTERY_Z = -20.0
BATTERY_Y = 70.0
BATTERY_X = 33.0
# The mortar's muzzle as it stands raised out of its hatch (tools/iron_dreadnought.py SHELL MORTAR_UP).
HATCH_Y = 81.0
# The tops of his coils (the arcs and the discharge leave from here), each over the middle of its pauldron, COIL_X either
# side of his middle (tools/iron_dreadnought.py COILS).
COIL_TOP = 96.0
COIL_X = 33.0
# The mortar's middle, behind his middle (tools/iron_dreadnought.py SHELL MORTAR_BACK).
MORTAR_Z = 8.0


def iron_dreadnought_model():
    m = Model("iron_dreadnought", 256, (6.0, 6.5))
    m.bone("root")
    # His legs: an armoured thigh from the hip, a knee joint, a piston housing for a shin with its chrome rod running
    # down into the ankle, and a broad tread of a foot.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * LEG_X
        m.bone(f"thigh_{name}", "root", (x, HIP_Y, 0))
        m.cube(f"thigh_{name}", "hip_joint", (x - 5, HIP_Y - 5, -5), (10, 8, 10), mirror=mirror)
        m.cube(f"thigh_{name}", "thigh", (x - 6, KNEE_Y + 3, -7), (12, 12, 13), mirror=mirror)
        m.bone(f"shin_{name}", f"thigh_{name}", (x, KNEE_Y, 0))
        m.cube(f"shin_{name}", "knee", (x - 5, KNEE_Y - 2, -6), (10, 6, 10), mirror=mirror)
        m.cube(f"shin_{name}", "shin", (x - 4.5, ANKLE_Y + 6, -4.5), (9, 11, 9), mirror=mirror)
        m.cube(f"shin_{name}", "shin_rod", (x - 2, ANKLE_Y, -2), (4, 7, 4), mirror=mirror)
        m.bone(f"foot_{name}", f"shin_{name}", (x, ANKLE_Y, 0))
        m.cube(f"foot_{name}", "ankle", (x - 4, ANKLE_Y - 2, -4), (8, 4, 8), mirror=mirror)
        m.cube(f"foot_{name}", "foot", (x - 8, 0, -15), (16, 5, 26), mirror=mirror)
        m.cube(f"foot_{name}", "toe", (x - 7, 0, -18), (14, 3, 3), mirror=mirror)
    # The hull: a skirt of armour plates round his hips, the box of his body (its top his deck, a rail along its back)
    # and a sloped glacis on his front.
    m.bone("body", "root", (0, HULL_BOTTOM + 4, 0))
    m.cube("body", "skirt_front", (-28, HULL_BOTTOM, -20), (56, 7, 2))
    m.cube("body", "skirt_front", (-28, HULL_BOTTOM, 18), (56, 7, 2))
    m.cube("body", "skirt_side", (-28, HULL_BOTTOM, -18), (2, 7, 36))
    m.cube("body", "skirt_side", (26, HULL_BOTTOM, -18), (2, 7, 36), mirror=True)
    m.cube("body", "hull", (-26, HULL_BOTTOM + 7, -18), (52, DECK_Y - HULL_BOTTOM - 7, 36))
    m.cube("body", "glacis", (-24, HULL_BOTTOM + 14, -22), (48, 22, 4), rotation=(-14, 0, 0),
           pivot=(0, HULL_BOTTOM + 14, -20))
    m.cube("body", "deck_rail", (-27, DECK_Y, 17), (54, 3, 2))
    # The furnaces behind his vents, glowing (on the glowmask): a grille on each flank and one on his back.
    m.cube("body", "furnace_side", (-26.5, 48, -9), (1, 12, 18))
    m.cube("body", "furnace_side", (25.5, 48, -9), (1, 12, 18), mirror=True)
    m.cube("body", "furnace_back", (-10, 50, 18), (20, 10, 1))
    # The vents' flaps (the vents controller's): hinged along their tops, shut over the furnaces.
    m.bone("vent_right", "body", (-27, 61, 0))
    m.cube("vent_right", "vent_flap", (-29, 47, -10), (2, 14, 20))
    m.bone("vent_left", "body", (27, 61, 0))
    m.cube("vent_left", "vent_flap", (27, 47, -10), (2, 14, 20), mirror=True)
    m.bone("vent_back", "body", (0, 61, 19))
    m.cube("vent_back", "vent_back_flap", (-11, 49, 19), (22, 12, 2))
    # The ram below the glacis, striped yellow and black, on two braces: it lowers for his Trench Charge.
    m.bone("ram", "body", (0, HULL_BOTTOM + 8, -20))
    m.cube("ram", "ram_brace", (-16, HULL_BOTTOM + 4, -26), (4, 4, 8))
    m.cube("ram", "ram_brace", (12, HULL_BOTTOM + 4, -26), (4, 4, 8), mirror=True)
    m.cube("ram", "ram", (-22, HULL_BOTTOM - 2, -30), (44, 14, 4))
    # The hatch in his belly (Sparkers drop from it): hinged at its front.
    m.bone("belly", "body", (0, HULL_BOTTOM, -6))
    m.cube("belly", "belly_hatch", (-8, HULL_BOTTOM - 1, -6), (16, 1, 14))
    # The cupola on his deck and the searchlight in its front: a lamp housing, its lens (glowing) and the shutter over it
    # (the eye controller's: down while he waits).
    m.bone("cupola", "body", (0, DECK_Y, -6))
    m.cube("cupola", "cupola", (-10, DECK_Y, -16), (20, 11, 18))
    m.cube("cupola", "cupola_roof", (-8, DECK_Y + 11, -14), (16, 2, 14))
    m.cube("cupola", "lamp", (-6, DECK_Y, -22), (12, 10, 6))
    m.cube("cupola", "lens", (-4.5, DECK_Y + 1.5, -22.25), (9, 7, 0))
    m.bone("shutter", "cupola", (0, DECK_Y + 9.5, -22.5))
    m.cube("shutter", "lamp_shutter", (-5.5, DECK_Y + 0.5, -23), (11, 9, 1))
    # His two smoke stacks behind the cupola, capped; the mortar between them under its hatch (it rises out for the
    # Mortar).
    for side in (-1, 1):
        m.cube("body", "stack", (side * 12 - 3, DECK_Y, 8), (6, 17, 6), mirror=side > 0)
        m.cube("body", "stack_cap", (side * 12 - 4, DECK_Y + 17, 7), (8, 2, 8), mirror=side > 0)
    m.bone("hatch", "body", (0, DECK_Y, 3))
    m.cube("hatch", "hatch", (-6, DECK_Y, 3), (12, 1, 10))
    m.bone("mortar", "body", (0, DECK_Y, MORTAR_Z))
    m.cube("mortar", "mortar_tube", (-3, DECK_Y - 10, MORTAR_Z - 3), (6, 9, 6))
    m.cube("mortar", "mortar_band", (-3.5, DECK_Y - 5, MORTAR_Z - 3.5), (7, 2, 7))
    # His pauldrons, each with a tesla coil on top: a base, a core with four copper rings round it, a wide toroid and
    # a ball at its top; and a crackle of arcs at the top of each (the coils controller's).
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * COIL_X
        m.bone(f"pauldron_{name}", "body", (x, SHOULDER_Y - 6, 0))
        m.cube(f"pauldron_{name}", "pauldron", (x - 7, SHOULDER_Y - 12, -13), (14, 13, 26), mirror=mirror)
        m.cube(f"pauldron_{name}", "coil_base", (x - 5, SHOULDER_Y + 1, -5), (10, 3, 10), mirror=mirror)
        m.cube(f"pauldron_{name}", "coil_core", (x - 3, SHOULDER_Y + 4, -3), (6, 13, 6), mirror=mirror)
        for i in range(4):
            m.cube(f"pauldron_{name}", "coil_ring", (x - 4.5, SHOULDER_Y + 5 + 3 * i, -4.5), (9, 1, 9), mirror=mirror)
        m.cube(f"pauldron_{name}", "coil_torus", (x - 6, SHOULDER_Y + 17, -6), (12, 3, 12), mirror=mirror)
        m.cube(f"pauldron_{name}", "coil_ball", (x - 2.5, SHOULDER_Y + 20, -2.5), (5, 4, 5), mirror=mirror)
        m.bone(f"arc_{name}", f"pauldron_{name}", (x, COIL_TOP - 2, 0))
        m.cube(f"arc_{name}", "arc", (x - 8, COIL_TOP - 4, 0), (16, 10, 0), mirror=mirror)
        m.cube(f"arc_{name}", "arc_cross", (x, COIL_TOP - 4, -8), (0, 10, 16), mirror=mirror)
    # The rivet battery on his left pauldron's front: its box, five muzzles, and the shutter over them, hinged at its top.
    bx, by, bz = BATTERY_X, BATTERY_Y, BATTERY_Z
    m.bone("battery", "pauldron_left", (bx, by, bz + 6))
    m.cube("battery", "battery", (bx - 7, by - 5, bz + 1), (14, 10, 8))
    for i in range(5):
        m.cube("battery", "muzzle", (bx - 6 + 2.5 * i, by - 1, bz - 1), (2, 2, 3))
    m.bone("battery_shutter", "battery", (bx, by + 5, bz))
    m.cube("battery_shutter", "battery_shutter", (bx - 7.5, by - 5, bz - 1.5), (15, 10, 1))
    # His arms from beneath the pauldrons: an upper arm, an elbow, a piston housing for a forearm, its rod, and a
    # riveted fist on the rod.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        x = side * 35
        m.bone(f"arm_{name}", f"pauldron_{name}", (x, SHOULDER_Y - 10, 0))
        m.cube(f"arm_{name}", "upper_arm", (x - 4, SHOULDER_Y - 26, -4), (8, 18, 8), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (x, SHOULDER_Y - 26, 0))
        m.cube(f"forearm_{name}", "elbow", (x - 5, SHOULDER_Y - 30, -5), (10, 6, 10), mirror=mirror)
        m.cube(f"forearm_{name}", "forearm", (x - 5.5, SHOULDER_Y - 42, -5.5), (11, 12, 11), mirror=mirror)
        m.bone(f"piston_{name}", f"forearm_{name}", (x, SHOULDER_Y - 42, 0))
        m.cube(f"piston_{name}", "piston_rod", (x - 2, SHOULDER_Y - 48, -2), (4, 8, 4), mirror=mirror)
        m.cube(f"piston_{name}", "fist", (x - 6, SHOULDER_Y - 58, -6), (12, 11, 12), mirror=mirror)
    return m


# The eye, vents and coils controllers' bones; the body's clips never key them.
EYE_BONES = ("shutter",)
VENT_BONES = ("vent_right", "vent_left", "vent_back")
COIL_BONES = ("arc_right", "arc_left")


def _rumble(length, cycles=2, scale=1.0):
    """His engines idling: the hull trembling a little, the pistons breathing."""
    s = scale
    return {
        "body": {"rotation": wave(length, cycles, (0, 0, 0), (0.6 * s, 0, 0.6 * s), phase=0.1)},
        "piston_right": {"position": wave(length, cycles, (0, 0, 0), (0, 0.4 * s, 0), phase=0.3)},
        "piston_left": {"position": wave(length, cycles, (0, 0, 0), (0, 0.4 * s, 0), phase=0.55)},
    }


# Standing ready: his arms a little forward and out, fists low.
IDLE = {"arm_right": (-10, 0, 6), "forearm_right": (-16, 0, 0), "arm_left": (-10, 0, -6), "forearm_left": (-16, 0, 0)}
# Dormant in his gantry: settled low on his knees, his hull tipped forward, his arms hanging slack.
DORMANT = {"root": {"position": (0, -3, 0)}, "thigh_right": (-16, 0, 0), "shin_right": (30, 0, 0),
           "foot_right": (-14, 0, 0), "thigh_left": (-16, 0, 0), "shin_left": (30, 0, 0), "foot_left": (-14, 0, 0),
           "body": (10, 0, 0), "cupola": (12, 0, 0), "arm_right": (8, 0, 2), "forearm_right": (-4, 0, 0),
           "arm_left": (8, 0, -2), "forearm_left": (-4, 0, 0)}
# Waking: the boilers fire, he heaves up and throws his arms out.
HEAVE = {"root": {"position": (0, 1, 0)}, "body": (-8, 0, 0), "cupola": (-10, 0, 0), "arm_right": (-40, 0, 30),
         "forearm_right": (-30, 0, 0), "arm_left": (-40, 0, -30), "forearm_left": (-30, 0, 0)}
# Piston Punch: the right arm drawn back, the piston rod sunk into its housing with a hiss; then driven out and
# across.
PUNCH_BACK = {"body": (0, -24, 0), "arm_right": (25, 0, 12), "forearm_right": (-100, 0, 0),
              "piston_right": {"position": (0, 5, 0)}, "arm_left": (-20, 0, -10), "forearm_left": (-20, 0, 0)}
PUNCH_OUT = {"body": (6, 28, 0), "arm_right": (-90, 0, -10), "forearm_right": (-4, 0, 0),
             "piston_right": {"position": (0, -8, 0)}, "arm_left": (-10, 0, -10), "forearm_left": (-20, 0, 0)}
# Rivet Fan: his left shoulder turned to his foe, the battery's shutter snapped up; then the recoil.
AIM = {"body": (0, 20, 0), "battery_shutter": (-110, 0, 0), "arm_left": (-20, 0, -14), "forearm_left": (-20, 0, 0),
       "arm_right": (-10, 0, 6), "forearm_right": (-16, 0, 0)}
RECOIL = {"body": (-4, 20, 0), "battery": (-10, 0, 0), "battery_shutter": (-110, 0, 0), "arm_left": (-16, 0, -14),
          "forearm_left": (-20, 0, 0), "arm_right": (-10, 0, 6), "forearm_right": (-16, 0, 0)}
# Trench Charge: the ram lowered and thrust out, his hull pitched forward, his arms back; then the run.
RAM_DOWN = {"root": {"position": (0, -2, 0)}, "body": (16, 0, 0), "cupola": (-12, 0, 0),
            "ram": {"rotation": (24, 0, 0), "position": (0, -2, -4)}, "thigh_right": (-20, 0, 0), "shin_right": (24, 0, 0),
            "thigh_left": (14, 0, 0), "shin_left": (10, 0, 0), "arm_right": (24, 0, 10), "forearm_right": (-20, 0, 0),
            "arm_left": (24, 0, -10), "forearm_left": (-20, 0, 0)}
# Coil Arc: his arms flung up and out, his coils reaching; then thrust at his foe.
ARMS_UP = {"body": (-6, 0, 0), "cupola": (-8, 0, 0), "arm_right": (-130, 0, 40), "forearm_right": (-20, 0, 0),
           "arm_left": (-130, 0, -40), "forearm_left": (-20, 0, 0)}
ARMS_OUT = {"body": (6, 0, 0), "arm_right": (-90, 0, 10), "forearm_right": (-4, 0, 0), "arm_left": (-90, 0, -10),
            "forearm_left": (-4, 0, 0)}
# Mortar: down on one knee, his hull tipped back, the hatch open and the tube raised out of it; then the recoil.
KNEEL = {"root": {"position": (0, -7, 0)}, "thigh_right": (-60, 0, 0), "shin_right": (60, 0, 0), "foot_right": (0, 0, 0),
         "thigh_left": (10, 0, 0), "shin_left": (70, 0, 0), "foot_left": (-80, 0, 0), "body": (-10, 0, 0),
         "hatch": (110, 0, 0), "mortar": {"rotation": (-18, 0, 0), "position": (0, 7, 0)},
         "arm_right": (-10, 0, 14), "forearm_right": (-30, 0, 0), "arm_left": (-10, 0, -14), "forearm_left": (-30, 0, 0)}
KNEEL_FIRE = dict(KNEEL, mortar={"rotation": (-18, 0, 0), "position": (0, 4, 1)}, body=(-6, 0, 0))
# Blowdown: crouched over, his arms in, steam building; then he rears up and throws his arms wide.
HUNCH = {"root": {"position": (0, -4, 0)}, "thigh_right": (-30, 0, 0), "shin_right": (40, 0, 0),
         "foot_right": (-10, 0, 0), "thigh_left": (-30, 0, 0), "shin_left": (40, 0, 0), "foot_left": (-10, 0, 0),
         "body": (14, 0, 0), "arm_right": (-30, 0, -10), "forearm_right": (-70, 0, 0), "arm_left": (-30, 0, 10),
         "forearm_left": (-70, 0, 0)}
REAR = {"root": {"position": (0, 1, 0)}, "body": (-12, 0, 0), "cupola": (-14, 0, 0), "arm_right": (-30, 0, 60),
        "forearm_right": (-10, 0, 0), "arm_left": (-30, 0, -60), "forearm_left": (-10, 0, 0)}
# Sparkers: his belly hatch dropped open, his legs braced wide.
BELLY_OPEN = {"root": {"position": (0, -2, 0)}, "thigh_right": (0, 0, 10), "foot_right": (0, 0, -10),
              "thigh_left": (0, 0, -10), "foot_left": (0, 0, 10), "belly": (100, 0, 0), "arm_right": (-10, 0, 20),
              "forearm_right": (-16, 0, 0), "arm_left": (-10, 0, -20), "forearm_left": (-16, 0, 0)}
# Charging his coils: his legs planted wide, his hull back, his arms braced out and down.
BRACE = {"root": {"position": (0, -3, 0)}, "thigh_right": (-10, 0, 14), "shin_right": (20, 0, 0),
         "foot_right": (-10, 0, -14), "thigh_left": (-10, 0, -14), "shin_left": (20, 0, 0), "foot_left": (-10, 0, 14),
         "body": (-8, 0, 0), "cupola": (-6, 0, 0), "arm_right": (-20, 0, 40), "forearm_right": (-30, 0, 0),
         "arm_left": (-20, 0, -40), "forearm_left": (-30, 0, 0)}
# Overloaded, or stunned against cover: reeling, his hull twisted, his cupola lolling, an arm thrown up.
REEL = {"root": {"position": (0, -2, 0)}, "body": (-14, 12, 8), "cupola": (16, -10, 10), "thigh_right": (-20, 0, 0),
        "shin_right": (30, 0, 0), "arm_right": (-110, 0, 20), "forearm_right": (-40, 0, 0), "arm_left": (20, 0, -30),
        "forearm_left": (-10, 0, 0)}
# The Torn Plate: his right fist clawing at his left flank; then the plate hurled out to his left.
CLAW = {"body": (8, -30, 0), "arm_right": (-75, -55, 0), "forearm_right": (-40, 0, 0), "arm_left": (-20, 0, -30),
        "forearm_left": (-20, 0, 0)}
HURL = {"body": (4, 30, 0), "arm_right": (-120, 0, -10), "forearm_right": (-10, 0, 0), "arm_left": (-10, 0, -20),
        "forearm_left": (-20, 0, 0)}
# Death: his knees give, he sags forward onto his fists, his cupola drops.
SAG = {"root": {"position": (0, -9, 0)}, "thigh_right": (-60, 0, 0), "shin_right": (80, 0, 0), "foot_right": (-20, 0, 0),
       "thigh_left": (-60, 0, 0), "shin_left": (80, 0, 0), "foot_left": (-20, 0, 0), "body": (24, 0, 4),
       "cupola": (24, 0, 0), "arm_right": (-30, 0, 10), "forearm_right": (-10, 0, 0), "arm_left": (-30, 0, -10),
       "forearm_left": (-10, 0, 0)}
DEAD = dict(SAG, root={"position": (0, -12, 0)}, body=(30, 0, 8), cupola=(30, 0, 6), arm_right=(-10, 0, 30),
            arm_left=(-14, 0, -24))


def _lift(length, phase, high, mid, rest=6):
    """A shin's keys through one stride: straight (bent `rest`) while its foot is down, then bending `high` as the foot
    lifts (from half the stride on) and easing back through `mid`; shifted by `phase` of the stride."""
    points = {0.0: rest, 0.5: rest, 0.65: high, 0.85: mid}
    out = {}
    for t, value in points.items():
        out[round(((t + phase) % 1.0) * length, 4)] = (value, 0, 0)
    first = min(out)
    if first > 0:
        # Fill the start of the clip from the last point before it wraps, so it loops seamlessly.
        before = max(out)
        out[0.0] = out[before] if before > (1 - phase) * length else (rest, 0, 0)
    out[length] = out[0.0]
    return out


def _stride(length, scale=1.0, lean=0.0):
    """His walk: the legs swinging alternately from the hips, each knee bending as its foot lifts and the foot kept
    flat; his hull dipping at each footfall and rolling over the leg he stands on, his arms swinging against his legs."""
    s = scale
    out = {}
    for name, phase in (("right", 0.0), ("left", 0.5)):
        out[f"thigh_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (22 * s, 0, 0), phase=phase)}
        out[f"shin_{name}"] = {"rotation": keys(_lift(length, phase, 34 * s, 20 * s))}
        out[f"foot_{name}"] = {"rotation": wave(length, 1, (0, 0, 0), (-16 * s, 0, 0), phase=phase)}
        arm_phase = (phase + 0.5) % 1.0
        out[f"arm_{name}"] = {"rotation": wave(length, 1, IDLE[f"arm_{name}"], (12 * s, 0, 0), phase=arm_phase)}
    out["forearm_right"] = {"rotation": still(IDLE["forearm_right"])}
    out["forearm_left"] = {"rotation": still(IDLE["forearm_left"])}
    out["body"] = {"rotation": wave(length, 1, (lean, 0, 0), (0, 3 * s, 2.5 * s))}
    out["root"] = {"position": wave(length, 2, (0, -0.5, 0), (0, 0.8 * s, 0), phase=0.25)}
    return out


def iron_dreadnought_animations():
    """His body's clips (the "body" controller), his eye's shutter (dark, lit), his vents (shut, blown open) and the
    arcs on his coils (quiet, crackling). The body's never key the shutter, the flaps or the arcs, nor the others any
    other bone."""
    stride = _stride(1.6)
    run = _stride(0.6, scale=1.4, lean=14)
    run["ram"] = {"rotation": still((24, 0, 0)), "position": still((0, -2, -4))}
    run["cupola"] = {"rotation": still((-12, 0, 0))}
    return animations({
        "animation.iron_dreadnought.dormant": posed(4.0, [(0.0, DORMANT), (4.0, DORMANT)], loop=True),
        "animation.iron_dreadnought.wake": posed(2.5, [(0.0, DORMANT), (0.6, DORMANT), (1.4, HEAVE), (2.5, IDLE)]),
        "animation.iron_dreadnought.idle": posed(2.0, [(0.0, IDLE), (2.0, IDLE)], loop=True, waves=_rumble(2.0)),
        "animation.iron_dreadnought.walk": clip(1.6, stride),
        "animation.iron_dreadnought.punch_windup": posed(0.8, [(0.0, IDLE), (0.55, PUNCH_BACK), (0.8, PUNCH_BACK)]),
        "animation.iron_dreadnought.punch": posed(0.75, [(0.0, PUNCH_BACK), (0.1, PUNCH_OUT), (0.35, PUNCH_OUT),
                                                         (0.75, IDLE)]),
        "animation.iron_dreadnought.rivets_windup": posed(0.9, [(0.0, IDLE), (0.5, AIM), (0.9, AIM)]),
        "animation.iron_dreadnought.rivets": posed(0.65, [(0.0, AIM), (0.05, RECOIL), (0.3, AIM), (0.65, IDLE)]),
        "animation.iron_dreadnought.charge_windup": posed(1.2, [(0.0, IDLE), (0.6, RAM_DOWN), (1.2, RAM_DOWN)],
                                                          waves={"root": {"position": wave(1.2, 6, (0, -2, 0),
                                                                                           (0, 0.5, 0))}}),
        "animation.iron_dreadnought.charge": clip(0.6, run),
        "animation.iron_dreadnought.arc_windup": posed(1.0, [(0.0, IDLE), (0.6, ARMS_UP), (1.0, ARMS_UP)]),
        "animation.iron_dreadnought.arc": posed(0.55, [(0.0, ARMS_UP), (0.08, ARMS_OUT), (0.3, ARMS_OUT),
                                                       (0.55, IDLE)]),
        "animation.iron_dreadnought.mortar_windup": posed(1.5, [(0.0, IDLE), (0.7, KNEEL), (1.5, KNEEL)]),
        "animation.iron_dreadnought.mortar": posed(0.75, [(0.0, KNEEL), (0.05, KNEEL_FIRE), (0.35, KNEEL),
                                                          (0.75, IDLE)]),
        "animation.iron_dreadnought.blowdown_windup": posed(1.0, [(0.0, IDLE), (0.6, HUNCH), (1.0, HUNCH)],
                                                            waves={"body": {"rotation": wave(1.0, 5, HUNCH["body"],
                                                                                             (1.5, 0, 1.5))}}),
        "animation.iron_dreadnought.blowdown": posed(0.8, [(0.0, HUNCH), (0.12, REAR), (0.4, REAR), (0.8, IDLE)]),
        "animation.iron_dreadnought.sparkers_windup": posed(1.2, [(0.0, IDLE), (0.5, BELLY_OPEN), (1.2, BELLY_OPEN)]),
        "animation.iron_dreadnought.sparkers": posed(0.5, [(0.0, BELLY_OPEN), (0.3, BELLY_OPEN), (0.5, IDLE)]),
        "animation.iron_dreadnought.brace": posed(1.0, [(0.0, BRACE), (1.0, BRACE)], loop=True, waves={
            "body": {"rotation": wave(1.0, 4, BRACE["body"], (1.0, 0, 1.0))},
            "cupola": {"rotation": wave(1.0, 3, BRACE["cupola"], (1.5, 2, 0), phase=0.2)}}),
        "animation.iron_dreadnought.reel": posed(1.0, [(0.0, REEL), (1.0, REEL)], loop=True, waves={
            "body": {"rotation": wave(1.0, 1, REEL["body"], (4, 6, 4))},
            "cupola": {"rotation": wave(1.0, 2, REEL["cupola"], (6, 8, 4), phase=0.2)}}),
        "animation.iron_dreadnought.tear": posed(2.0, [(0.0, IDLE), (0.6, CLAW), (1.3, CLAW), (1.55, HURL),
                                                       (2.0, IDLE)], waves={}),
        "animation.iron_dreadnought.death": posed(2.5, [(0.0, IDLE), (0.9, SAG), (1.8, DEAD), (2.5, DEAD)]),
        # His searchlight's shutter: down while he waits, raised once he wakes.
        "animation.iron_dreadnought.eye_dark": clip(1.0, {"shutter": {"rotation": still((0, 0, 0))}}),
        "animation.iron_dreadnought.eye_lit": clip(0.5, {"shutter": {"rotation": keys({0.0: (0, 0, 0),
                                                                                         0.5: (-100, 0, 0)})}},
                                                   loop=False),
        # His vents' flaps: shut, or blown open (and rattling in the blast from the furnaces).
        "animation.iron_dreadnought.vents_shut": clip(1.0, {bone: {"rotation": still((0, 0, 0))} for bone in VENT_BONES}),
        "animation.iron_dreadnought.vents_open": clip(0.5, {
            "vent_right": {"rotation": wave(0.5, 2, (0, 0, -100), (0, 0, 4))},
            "vent_left": {"rotation": wave(0.5, 2, (0, 0, 100), (0, 0, 4), phase=0.25)},
            "vent_back": {"rotation": wave(0.5, 2, (-100, 0, 0), (4, 0, 0), phase=0.5)}}),
        # The arcs on his coils: quiet, or crackling (charging, arcing).
        "animation.iron_dreadnought.coils_quiet": clip(1.0, {bone: {"scale": still((0, 0, 0))} for bone in COIL_BONES}),
        "animation.iron_dreadnought.coils_crackle": clip(0.3, {
            "arc_right": {"scale": keys({0.0: (1, 1, 1), 0.1: (0.6, 1.3, 0.6), 0.2: (1.2, 0.8, 1.2), 0.3: (1, 1, 1)}),
                          "rotation": keys({0.0: (0, 0, 0), 0.15: (0, 60, 0), 0.3: (0, 120, 0)})},
            "arc_left": {"scale": keys({0.0: (1.2, 0.8, 1.2), 0.1: (1, 1, 1), 0.2: (0.6, 1.3, 0.6), 0.3: (1.2, 0.8, 1.2)}),
                         "rotation": keys({0.0: (0, 0, 0), 0.15: (0, -60, 0), 0.3: (0, -120, 0)})}}),
    })


# ------------------------------------------------------------------------------------------------- his Sparkers

def sparker_model():
    """A Sparker: a little coil crawler half a block high, a domed steel shell on four spindly jointed legs, two lit
    lenses in its front, and a tesla coil on its back, its ball glowing violet."""
    m = Model("sparker", 64, (1.0, 1.0))
    m.bone("root")
    m.bone("shell", "root", (0, 5, 0))
    m.cube("shell", "sparker_shell", (-4, 4, -5), (8, 4, 10))
    m.cube("shell", "sparker_belly", (-3, 3, -4), (6, 1, 8))
    m.cube("shell", "sparker_lens", (-3, 5, -5.25), (6, 2, 0))
    m.cube("shell", "sparker_coil", (-1, 8, -1), (2, 4, 2))
    m.cube("shell", "sparker_ring", (-2, 9, -2), (4, 1, 4))
    m.cube("shell", "sparker_ring", (-2, 11, -2), (4, 1, 4))
    m.bone("ball", "shell", (0, 12, 0))
    m.cube("ball", "sparker_ball", (-1.5, 12, -1.5), (3, 3, 3))
    for name, x, z in (("fr", -1, -1), ("fl", 1, -1), ("br", -1, 1), ("bl", 1, 1)):
        mirror = x > 0
        m.bone(f"leg_{name}", "shell", (x * 4, 5, z * 3), rotation=(0, 0, x * 35))
        m.cube(f"leg_{name}", "sparker_thigh", (x * 4 - 0.5, 0, z * 3 - 0.5), (1, 5, 1), mirror=mirror)
    return m


def sparker_animations():
    scuttle = {}
    for name, phase in (("fr", 0.0), ("bl", 0.0), ("fl", 0.5), ("br", 0.5)):
        side = 1 if name.endswith("l") else -1
        scuttle[f"leg_{name}"] = {"rotation": wave(0.4, 1, (0, 0, 0), (30, 0, 12 * side), phase=phase)}
    scuttle["shell"] = {"rotation": wave(0.4, 2, (0, 0, 0), (2, 0, 3)), "position": wave(0.4, 2, (0, 0, 0), (0, 0.4, 0))}
    idle = {"shell": {"rotation": wave(1.2, 1, (0, 0, 0), (2, 6, 0))},
            "ball": {"scale": wave(1.2, 2, (1, 1, 1), (0.08, 0.08, 0.08))}}
    fuse = {"shell": {"position": wave(0.3, 3, (0, -0.5, 0), (0.3, 0.2, 0.3))},
            "ball": {"scale": wave(0.3, 3, (1.4, 1.4, 1.4), (0.4, 0.4, 0.4))}}
    drop = {"shell": {"rotation": keys({0.0: (0, 0, 180), 0.3: (0, 0, 360), 0.5: (0, 0, 360)})},
            "leg_fr": {"rotation": keys({0.0: (-60, 0, 0), 0.5: (0, 0, 0)})},
            "leg_fl": {"rotation": keys({0.0: (-60, 0, 0), 0.5: (0, 0, 0)})},
            "leg_br": {"rotation": keys({0.0: (60, 0, 0), 0.5: (0, 0, 0)})},
            "leg_bl": {"rotation": keys({0.0: (60, 0, 0), 0.5: (0, 0, 0)})}}
    return animations({
        "animation.sparker.drop": clip(0.5, drop, loop=False),
        "animation.sparker.idle": clip(1.2, idle),
        "animation.sparker.walk": clip(0.4, scuttle),
        "animation.sparker.fuse": clip(0.3, fuse),
    })


# ------------------------------------------------------------------------------------------------- his rivets

def hot_rivet_model():
    """A hot rivet in flight: a domed head and its shank, glowing orange, a streak of heat behind it."""
    m = Model("hot_rivet", 32, (0.5, 0.5))
    m.bone("root")
    m.bone("rivet", "root", (0, 2, 0))
    m.cube("rivet", "rivet_head", (-1.5, 0.5, -3), (3, 3, 2))
    m.cube("rivet", "rivet_shank", (-1, 1, -1), (2, 2, 3))
    m.cube("rivet", "rivet_streak", (0, 0, 2), (0, 4, 6))
    return m


def hot_rivet_animations():
    return animations({"animation.hot_rivet.fly": clip(0.4, {
        "rivet": {"rotation": keys({0.0: (0, 0, 0), 0.4: (0, 0, 360)})}})})


# ------------------------------------------------------------------------------------------------- his mortar shell

def mortar_shell_model():
    """A mortar shell: a stubby drab body, a pointed nose, a tail of four fins and a burning fuse at its nose. Built
    nose forward (north); its flight clip tips it from nose-up to nose-down over SHELL_FLIGHT."""
    m = Model("mortar_shell", 64, (1.0, 1.0))
    m.bone("root")
    m.bone("shell", "root", (0, 3, 0))
    m.cube("shell", "shell_body", (-2.5, 0.5, -4), (5, 5, 8))
    m.cube("shell", "shell_nose", (-1.5, 1.5, -7), (3, 3, 3))
    m.cube("shell", "shell_fuse", (-0.5, 2.5, -8), (1, 1, 1))
    m.cube("shell", "shell_fin", (0, -1, 3), (0, 9, 5))
    m.cube("shell", "shell_fin_flat", (-4.5, 3, 3), (9, 0, 5))
    return m


def mortar_shell_animations():
    return animations({"animation.mortar_shell.flight": clip(1.0, {
        "shell": {"rotation": keys({0.0: (-55, 0, 0), 0.5: (0, 0, 0), 1.0: (65, 0, 0)})}}, loop=False)})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"iron_dreadnought": iron_dreadnought_model, "sparker": sparker_model, "hot_rivet": hot_rivet_model,
          "mortar_shell": mortar_shell_model}
ANIMATIONS = {"iron_dreadnought": iron_dreadnought_animations, "sparker": sparker_animations,
              "hot_rivet": hot_rivet_animations, "mortar_shell": mortar_shell_animations}
# The clips the Java names (IronDreadnoughtEntity, SparkerEntity, HotRivetEntity and MortarShellEntity).
CLIPS = {
    "iron_dreadnought": ("dormant", "wake", "idle", "walk", "punch_windup", "punch", "rivets_windup", "rivets",
                         "charge_windup", "charge", "arc_windup", "arc", "mortar_windup", "mortar", "blowdown_windup",
                         "blowdown", "sparkers_windup", "sparkers", "brace", "reel", "tear", "death", "eye_dark",
                         "eye_lit", "vents_shut", "vents_open", "coils_quiet", "coils_crackle"),
    "sparker": ("drop", "idle", "walk", "fuse"),
    "hot_rivet": ("fly",),
    "mortar_shell": ("flight",),
}
# The bones each of the Dreadnought's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"eye": EYE_BONES, "vents": VENT_BONES, "coils": COIL_BONES}


def _check():
    """Every model packs; no clip of the Dreadnought's body keys an eye, vent or coil bone, nor theirs any other."""
    for name, build in MODELS.items():
        build().pack()
    anims = iron_dreadnought_animations()["animations"]
    prefixes = {"eye": "eye_", "vents": "vents_", "coils": "coils_"}
    for clip_name, body in anims.items():
        short = clip_name.split(".")[-1]
        owner = next((c for c, p in prefixes.items() if short.startswith(p)), "body")
        for bone in body["bones"]:
            controller = next((c for c, bones in CONTROLLERS.items() if bone in bones), "body")
            if controller != owner:
                raise ValueError(f"{clip_name} keys {bone}")
    for name, clips in CLIPS.items():
        have = {c.split(".")[-1] for c in ANIMATIONS[name]()["animations"]}
        if set(clips) != have:
            raise ValueError(f"{name}: clips {sorted(set(clips) ^ have)}")
    return math.isfinite(BATTERY_Z)


if __name__ == "__main__":
    print(_check())
