"""GeckoLib bodies for the Mire Hag and her fight (docs/features/mire-hag.md): the Hag herself (whose model and sheet
her Likenesses share), the Root Knot that crawls on a lamp, the roots that burst along her Root Lanes, the hands of mud
of her Mire Grasp and the flask of her Poison Brew. Geometry and animations only, as plain dicts; no I/O.
tools/mire_hag_art.py paints their sheets on the same box-UV regions, at TEXTURE_SCALE times the size the models declare
(docs/ART_DIRECTION.md, "High resolution"). The builder and the posing helpers are Vesperine's
(tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0 at the feet, every
model facing north (-z) with its left on +x; +x pitches a bone's top forward, -x raises a hanging arm forward, a right
arm turns outwards with +z and a left one with -z, and +y turns the front to the model's right. A keyframed rotation
adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. MODELS, ANIMATIONS and CLIPS are keyed by model; CLIPS lists the clips the Java plays. Nothing
here needs turning to face a way but the Hag and her Root Knot (GeckoLib turns a living body): a root lane is a row of
clumps, each its own entity, and her grasp a ring of hands.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS", "HAND_Z", "HAND_Y", "GRIP", "GRASP_RING"]

# ------------------------------------------------------------------------------------------------- the Hag

# A crone of the deep fen, bent nearly double and a head taller than a man even so: a long robe of rotted sackcloth
# whose ragged hem trails in the water, belted with knotted rope; a shawl of grey moss over her shoulders and humped back,
# its fringe hanging; a gaunt face with a long hooked nose and eyes of sickly marsh-light green, matted hair hung with
# sedge; long thin arms in ragged sleeves, the left hand's fingers long and knotted as roots; in her right hand a crooked
# staff of bog oak, a little lantern swinging from its crook. Her lantern's flame is the "lantern" controller's (lit,
# flaring, or out); the body's clips never key it.
BODY_LEAN = 22.0            # her waist's rest pitch: leaning forward from the hips
TORSO_BEND = 32.0           # her back's, curving over further: bent nearly double
HEAD_LIFT = -40.0           # her head's rest pitch, lifted back up to look ahead
ARM_HANG = -(BODY_LEAN + TORSO_BEND)   # her arms' rest pitch, hanging straight down from the bent shoulders
GRIP = (-6.0, 15.0, -1.0)   # the staff through her right hand (its middle), at rest
# Where her left hand is as she lobs a flask (the brew clip at its release): ahead of her feet and over them, in pixels.
# MireHagEntity's brew leaves from here (tools/mire_hag.py BREW HAND_AHEAD and HAND_UP).
HAND_Z = -22.0
HAND_Y = 30.0
# She is drawn this far back of her feet (pixels), so that her hitbox, a block wide round her feet, holds the most of her:
# her skirt and her bent back, with her face and her staff jutting ahead of it.
BACK = 5.0


def mire_hag_model():
    m = Model("mire_hag", 128, (2.5, 3.0))
    m.bone("root")
    # Her skirt, flaring to its ragged hem in the water; it sways on its own bone.
    m.bone("skirt", "root", (0, 14, 0))
    m.cube("skirt", "skirt", (-7, 0, -6), (14, 14, 12))
    # Her waist, leaning forward from the hips: the robe's upper part, belted with knotted rope, a pouch at the belt.
    m.bone("body", "root", (0, 14, 0), rotation=(BODY_LEAN, 0, 0))
    m.cube("body", "waist", (-6, 11, -5), (12, 13, 10))
    m.cube("body", "belt", (-6.5, 16, -5.5), (13, 2, 11))
    m.cube("body", "pouch", (2.5, 13, -6.5), (3, 4, 2))
    # Her back curving over further: her chest under the shawl of grey moss that is the most of her, over her shoulders
    # and the hump of her back; the shawl's back hanging down behind her and its fringe before her.
    m.bone("torso", "body", (0, 24, 0), rotation=(TORSO_BEND, 0, 0))
    m.cube("torso", "chest", (-5, 22, -4), (10, 11, 8))
    m.cube("torso", "shawl", (-7, 26, -4.5), (14, 9, 12))
    m.bone("shawl_back", "torso", (0, 33, 7.5), rotation=(-40, 0, 0))
    m.cube("shawl_back", "shawl_back", (-6, 20, 6.5), (12, 13, 1))
    m.bone("fringe", "torso", (0, 27, -4.5), rotation=(-(BODY_LEAN + TORSO_BEND), 0, 0))
    m.cube("fringe", "fringe", (-6, 21, -4.5), (12, 6, 0))
    # The head, jutting from her shoulders: a gaunt face and a long hooked nose; her hood of the same moss over its crown,
    # its tail down the back of her neck; matted locks at her cheeks; sedge standing out of the hood in two crossed blades.
    m.bone("head", "torso", (0, 33, -3), rotation=(HEAD_LIFT, 0, 0))
    m.cube("head", "skull", (-3.5, 31, -9), (7, 8, 7))
    m.cube("head", "nose", (-1, 32.5, -11), (2, 4, 2))
    m.cube("head", "nose_tip", (-1, 31.5, -12), (2, 2, 1))
    m.cube("head", "hood", (-4.5, 36, -9.5), (9, 5, 9))
    m.cube("head", "lock", (-4.5, 28, -8), (1, 8, 6))
    m.cube("head", "lock", (3.5, 28, -8), (1, 8, 6), mirror=True)
    m.bone("hood_tail", "head", (0, 36, -0.5))
    m.cube("hood_tail", "hood_tail", (-4.5, 28, -1), (9, 8, 2))
    m.bone("sedge", "head", (0, 41, -5))
    m.cube("sedge", "sedge", (-3.5, 40, -5), (7, 8, 0), rotation=(0, 45, 0), pivot=(0, 40, -5))
    m.cube("sedge", "sedge", (-3.5, 40, -5), (7, 8, 0), rotation=(0, -45, 0), pivot=(0, 40, -5), mirror=True)
    # Her arms, in ragged sleeves; the right hand closed on her staff, the left's long knotted fingers free.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0

        def ox(v, w):  # a right-side origin x for a cube `w` wide, mirrored to the left
            return v if side < 0 else -v - w

        m.bone(f"arm_{name}", "torso", (side * 5.5, 32, -1), rotation=(ARM_HANG, 0, 0))
        m.cube(f"arm_{name}", "upper_arm", (ox(-7, 2), 24, -2), (2, 9, 2), mirror=mirror)
        m.cube(f"arm_{name}", "sleeve", (ox(-7.5, 3), 27, -2.5), (3, 6, 3), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (side * 6, 24, -1))
        m.cube(f"forearm_{name}", "forearm", (ox(-7, 2), 16, -2), (2, 8, 2), mirror=mirror)
        m.cube(f"forearm_{name}", "cuff", (ox(-7.5, 3), 21, -2.5), (3, 3, 3), mirror=mirror)
        m.bone(f"hand_{name}", f"forearm_{name}", (side * 6, 16, -1))
        m.cube(f"hand_{name}", "hand", (ox(-7.5, 3), 13, -2.5), (3, 3, 3), mirror=mirror)
    # The left hand's fingers: three long knotted roots, hanging from its knuckles a little apart.
    for i, (x, z, length) in enumerate(((5.5, -2.5, 6), (6.5, -1.5, 7), (7.5, -2.5, 5))):
        m.cube("hand_left", {5: "finger_short", 6: "finger", 7: "finger_long"}[length], (x, 13 - length, z), (1, length, 1),
               rotation=(-8 + 8 * i, 0, -6 + 6 * i), pivot=(x + 0.5, 13, z + 0.5))
    # Her staff turns in her hand against the arm's pitch, so it stands upright however she holds it (staff_grip, the
    # body's); the lantern swings from its crook (the lantern bone, the body's) and its flame burns, flares or is out (the
    # flame bone, the lantern controller's alone).
    gx, gy, gz = GRIP
    m.bone("staff_grip", "hand_right", GRIP)
    m.bone("staff", "staff_grip", GRIP)
    m.cube("staff", "staff", (gx - 1, gy - 15, gz - 1), (2, 45, 2))
    m.cube("staff", "knot", (gx - 1.5, gy + 4, gz - 1.5), (3, 3, 3))
    m.cube("staff", "knot", (gx - 1.5, gy + 18, gz - 1.5), (3, 3, 3))
    top = gy + 30
    m.cube("staff", "crook", (gx - 1, top - 2, gz - 7), (2, 2, 6))
    m.cube("staff", "crook_tip", (gx - 1, top - 5, gz - 8), (2, 4, 2))
    m.bone("lantern", "staff", (gx, top - 2, gz - 4.5))
    m.cube("lantern", "chain", (gx - 0.5, top - 5, gz - 5), (1, 3, 1))
    m.cube("lantern", "lantern_cap", (gx - 2.5, top - 6, gz - 7), (5, 1, 5))
    m.cube("lantern", "lantern_cage", (gx - 2, top - 11, gz - 6.5), (4, 5, 4))
    m.cube("lantern", "lantern_base", (gx - 2.5, top - 12, gz - 7), (5, 1, 5))
    m.bone("flame", "lantern", (gx, top - 10, gz - 4.5))
    m.cube("flame", "flame", (gx - 1, top - 10.5, gz - 5.5), (2, 3, 2))
    return _shifted(m, BACK)


def _shifted(model, back):
    """`model` moved `back` pixels toward its back (+z): every bone's pivot, every cube's corner and pivot."""
    for bone in model.bones:
        bone["pivot"][2] += back
        for cube in bone["cubes"]:
            cube["origin"][2] = round(cube["origin"][2] + back, 4)
            if "pivot" in cube:
                cube["pivot"][2] = round(cube["pivot"][2] + back, 4)
    return model


# The lantern controller's bone; the body's clips never key it.
LANTERN_BONES = ("flame",)
BODY_PITCH = ("body", "torso", "arm_right", "forearm_right", "hand_right")


def staffed(pose, lean=0.0, roll=0.0):
    """The pose with her staff turned back against her right arm's pitch (and the robe's and torso's), so it stands
    upright, leaning `lean` degrees forward and `roll` out to her right."""
    pitch = sum(pose.get(bone, (0, 0, 0))[0] for bone in BODY_PITCH)
    out = dict(pose)
    out["staff_grip"] = (lean - pitch, 0, -roll - pose.get("arm_right", (0, 0, 0))[2])
    return out


def sway(length, cycles=1, scale=1.0):
    """Her rags and hair stirring: each on its own beat."""
    s = scale
    return {
        "skirt": {"rotation": wave(length, cycles, (0, 0, 0), (2 * s, 0, 1.5 * s))},
        "shawl_back": {"rotation": wave(length, cycles, (0, 0, 0), (4 * s, 0, 1.5 * s), phase=0.15)},
        "fringe": {"rotation": wave(length, cycles, (0, 0, 0), (5 * s, 0, 0), phase=0.3)},
        "hood_tail": {"rotation": wave(length, cycles, (0, 0, 0), (4 * s, 0, 2 * s), phase=0.2)},
        "lantern": {"rotation": wave(length, cycles, (0, 0, 0), (8 * s, 0, 6 * s), phase=0.1)},
    }


# Bent over her staff, leaning on it, her left hand's claws hanging: her stance between attacks.
IDLE = staffed({"arm_right": (-28, 0, 6), "forearm_right": (-24, 0, 0), "arm_left": (-12, 0, -8), "forearm_left": (-20, 0, 0),
                "head": (4, 0, 0)}, roll=4)
# On her porch, bent lower over her cauldron and stirring it with her staff held across her.
STIR = staffed({"torso": (12, 0, 0), "head": (16, 0, 0), "arm_right": (-40, 0, 14), "forearm_right": (-30, 0, 0),
                "arm_left": (-42, 0, -10), "forearm_left": (-28, 0, 0)}, lean=30, roll=-10)
# Waking: she straightens and looks up over the marsh.
LOOK_UP = staffed({"torso": (-14, 0, 0), "head": (-22, 0, 0), "arm_right": (-26, 0, 8), "forearm_right": (-20, 0, 0),
                   "arm_left": (-18, 0, -14), "forearm_left": (-24, 0, 0)}, roll=4)
# Stepping down into the water, her robe flaring and her staff swung ahead.
STEP_DOWN = staffed({"torso": (-6, 0, 0), "head": (-6, 0, 0), "arm_right": (-50, 0, 10), "forearm_right": (-12, 0, 0),
                     "arm_left": (10, 0, -30), "forearm_left": (-20, 0, 0), "skirt": (-5, 0, 0),
                     "shawl_back": (20, 0, 0)}, lean=14, roll=6)
# Clawing Swipe: her left hand drawn back across her, claws raised; then raked across her front.
CLAW_BACK = staffed({"torso": (-6, 18, 0), "head": (0, -14, 0), "arm_left": (-70, 30, 40), "forearm_left": (-40, 0, 0),
                     "hand_left": (-20, 0, 0), "arm_right": (-26, 0, 6), "forearm_right": (-24, 0, 0)}, roll=4)
CLAW_THROUGH = staffed({"torso": (8, -26, 0), "head": (0, 18, 0), "arm_left": (-80, -20, -40), "forearm_left": (-10, 0, 0),
                        "hand_left": (10, 0, 0), "arm_right": (-26, 0, 6), "forearm_right": (-24, 0, 0),
                        "shawl_back": (14, 0, 10)}, roll=4)
# Root Lanes: her staff raised high in both hands, then plunged down into the water before her and held there.
STAFF_HIGH = staffed({"torso": (-18, 0, 0), "head": (-20, 0, 0), "arm_right": (-150, 0, 12), "forearm_right": (-10, 0, 0),
                      "arm_left": (-140, 0, -20), "forearm_left": (-30, 0, 0)}, lean=-6)
STAFF_PLUNGED = staffed({"torso": (16, 0, 0), "head": (10, 0, 0), "arm_right": (-60, 0, 4), "forearm_right": (-10, 0, 0),
                         "arm_left": (-58, 0, -14), "forearm_left": (-16, 0, 0), "shawl_back": (16, 0, 0)}, lean=-10)
# Poison Brew: her left hand at her shawl, drawing out a flask; then up and back over her shoulder, and lobbed.
DRAW = staffed({"torso": (4, -10, 0), "head": (6, 10, 0), "arm_left": (-40, 0, 30), "forearm_left": (-90, 0, 0),
                "arm_right": (-26, 0, 6), "forearm_right": (-24, 0, 0)}, roll=4)
WIND = staffed({"torso": (-10, 20, 0), "head": (-10, -16, 0), "arm_left": (-160, 0, -20), "forearm_left": (-30, 0, 0),
                "arm_right": (-26, 0, 6), "forearm_right": (-24, 0, 0)}, roll=4)
LOB = staffed({"torso": (10, -16, 0), "head": (0, 10, 0), "arm_left": (-110, 0, -10), "forearm_left": (-4, 0, 0),
               "arm_right": (-26, 0, 6), "forearm_right": (-24, 0, 0)}, roll=4)
# Likeness Exchange: arms spread, swaying as the mist thickens; then a shudder as she is elsewhere.
SPREAD = staffed({"torso": (-10, 0, 0), "head": (-12, 0, 0), "arm_right": (-40, 0, 40), "forearm_right": (-10, 0, 0),
                  "arm_left": (-40, 0, -50), "forearm_left": (-10, 0, 0)}, roll=20)
# Lights Out: she cackles, head thrown back, shoulders shaking, her left hand clutched to her chest.
CACKLE = staffed({"torso": (-24, 0, 0), "head": (-34, 0, 0), "arm_left": (-70, 0, 20), "forearm_left": (-80, 0, 0),
                  "arm_right": (-30, 0, 8), "forearm_right": (-20, 0, 0)}, roll=6)
# Mire Grasp: her claws spread and thrust down at the water, her staff held aside.
GRASP_DOWN = staffed({"torso": (20, 0, 0), "head": (12, 0, 0), "arm_left": (-50, 0, -20), "forearm_left": (-10, 0, 0),
                      "hand_left": (30, 0, 0), "arm_right": (-20, 0, 20), "forearm_right": (-20, 0, 0)}, roll=16)
# Snuff: her left hand at her lantern, pinching its wick, her staff drawn up close.
PINCH = staffed({"torso": (-4, 8, 0), "head": (-14, -10, 0), "arm_right": (-70, 0, 10), "forearm_right": (-40, 0, 0),
                 "arm_left": (-120, 0, 40), "forearm_left": (-50, 0, 0)}, lean=-14, roll=12)
# Death: she sags over her staff and sinks into the water, the staff falling from her hand.
SAG = staffed({"torso": (30, 0, 10), "head": (30, 0, 0), "arm_right": (-10, 0, 20), "forearm_right": (-10, 0, 0),
               "arm_left": (10, 0, -10), "forearm_left": (-10, 0, 0)}, lean=20, roll=10)
FALLEN = {"root": {"position": (0, -14, 0)}, "torso": (50, 0, 20), "head": (40, 0, 10), "arm_right": (20, 0, 50),
          "forearm_right": (-10, 0, 0), "arm_left": (20, 0, -40), "forearm_left": (-10, 0, 0), "staff_grip": (-70, 0, -10),
          "body": (20, 0, 12), "skirt": (10, 0, 0)}


def _waded(length):
    """Her wading walk: the robe swinging side to side, the staff planted ahead and swung forward again each stride, her
    head and hump bobbing."""
    out = sway(length, 2, 1.4)
    out["body"] = {"rotation": wave(length, 2, (0, 0, 0), (0, 6, 4))}
    out["skirt"] = {"rotation": wave(length, 2, (0, 0, 0), (5, 0, 4), phase=0.25)}
    out["torso"] = {"rotation": wave(length, 2, (0, 0, 0), (3, 0, 0), phase=0.5)}
    out["arm_left"] = {"rotation": wave(length, 1, (-12, 0, -8), (16, 0, 0))}
    return out


def mire_hag_animations():
    """Her body's clips (the "body" controller) and her lantern's (lit, flaring in the Last Light, or out). The body's
    never key the flame, nor the lantern's any other bone."""
    idle_wave = {**sway(3.0), "torso": {"rotation": wave(3.0, 1, (0, 0, 0), (2, 0, 0))}}
    return animations({
        "animation.mire_hag.stir": posed(3.0, [(0.0, STIR), (3.0, STIR)], loop=True, waves={
            **sway(3.0, 1, 0.6), "staff_grip": {"rotation": wave(3.0, 1, STIR["staff_grip"], (10, 20, 10))},
            "arm_right": {"rotation": wave(3.0, 1, STIR["arm_right"], (8, 10, 6))}}),
        "animation.mire_hag.wake": posed(2.0, [(0.0, STIR), (0.6, LOOK_UP), (1.0, LOOK_UP), (1.4, STEP_DOWN),
                                                (2.0, STEP_DOWN)]),
        "animation.mire_hag.idle": posed(3.0, [(0.0, IDLE), (3.0, IDLE)], loop=True, waves=idle_wave),
        "animation.mire_hag.walk": posed(1.2, [(0.0, IDLE), (1.2, IDLE)], loop=True, waves=_waded(1.2)),
        "animation.mire_hag.swipe_windup": posed(0.6, [(0.0, IDLE), (0.6, CLAW_BACK)]),
        "animation.mire_hag.swipe": posed(0.6, [(0.0, CLAW_BACK), (0.12, CLAW_THROUGH), (0.3, CLAW_THROUGH), (0.6, IDLE)]),
        "animation.mire_hag.lanes_windup": posed(1.2, [(0.0, IDLE), (0.7, STAFF_HIGH), (1.0, STAFF_HIGH),
                                                       (1.2, STAFF_PLUNGED)]),
        "animation.mire_hag.lanes": posed(1.5, [(0.0, STAFF_PLUNGED), (1.0, STAFF_PLUNGED), (1.5, IDLE)]),
        "animation.mire_hag.brew_windup": posed(0.8, [(0.0, IDLE), (0.35, DRAW), (0.8, WIND)]),
        "animation.mire_hag.brew": posed(0.6, [(0.0, WIND), (0.1, LOB), (0.3, LOB), (0.6, IDLE)]),
        "animation.mire_hag.exchange_windup": posed(2.0, [(0.0, SPREAD), (2.0, SPREAD)], loop=True, waves={
            **sway(2.0, 2, 1.5), "torso": {"rotation": wave(2.0, 4, SPREAD.get("torso", (0, 0, 0)), (4, 6, 4))},
            "head": {"rotation": wave(2.0, 2, SPREAD.get("head", (0, 0, 0)), (6, 10, 0), phase=0.2)}}),
        "animation.mire_hag.exchange": posed(0.5, [(0.0, SPREAD), (0.15, {**IDLE, "torso": (20, 0, 0)}), (0.5, IDLE)]),
        "animation.mire_hag.cackle": posed(0.5, [(0.0, CACKLE), (0.5, CACKLE)], loop=True, waves={
            "torso": {"rotation": wave(0.5, 2, CACKLE["torso"], (4, 0, 2))},
            "head": {"rotation": wave(0.5, 2, CACKLE["head"], (6, 4, 0), phase=0.25)},
            "lantern": {"rotation": wave(0.5, 1, (0, 0, 0), (14, 0, 10))}}),
        "animation.mire_hag.grasp_windup": posed(1.2, [(0.0, IDLE), (0.5, GRASP_DOWN), (1.2, GRASP_DOWN)]),
        "animation.mire_hag.grasp": posed(1.5, [(0.0, GRASP_DOWN), (0.2, {**GRASP_DOWN, "hand_left": (60, 0, 0)}),
                                                (1.0, {**GRASP_DOWN, "hand_left": (60, 0, 0)}), (1.5, IDLE)]),
        "animation.mire_hag.snuff_windup": posed(1.5, [(0.0, IDLE), (0.6, PINCH), (1.5, PINCH)]),
        "animation.mire_hag.snuff": posed(0.5, [(0.0, PINCH), (0.5, IDLE)]),
        "animation.mire_hag.death": posed(2.0, [(0.0, IDLE), (0.6, SAG), (1.4, FALLEN), (2.0, FALLEN)]),
        # Her lantern's flame: burning (a slow flicker), flaring in the Last Light, or out.
        "animation.mire_hag.lantern_lit": clip(1.0, {"flame": {"scale": wave(1.0, 2, (1, 1, 1), (0.08, 0.15, 0.08))}}),
        "animation.mire_hag.lantern_flare": clip(0.4, {"flame": {"scale": wave(0.4, 2, (1.5, 1.8, 1.5), (0.15, 0.3, 0.15))}}),
        "animation.mire_hag.lantern_out": clip(1.0, {"flame": {"scale": still((0, 0, 0))}}),
    })


# ------------------------------------------------------------------------------------------------- the Root Knot

def root_knot_model():
    """The Root Knot: a writhing knot of fen-roots a block and a half across, a lump of tangled roots on six tendrils
    that drag it along, and a seventh curled over its back."""
    m = Model("root_knot", 64, (2.0, 1.2))
    m.bone("root")
    m.bone("knot", "root", (0, 6, 0))
    m.cube("knot", "knot", (-5, 3, -5), (10, 8, 10))
    m.cube("knot", "knot_lump", (-3, 10, -3), (6, 4, 6))
    m.cube("knot", "knot_side", (4, 5, -3), (3, 5, 6))
    m.cube("knot", "knot_side", (-7, 5, -3), (3, 5, 6), mirror=True)
    for i in range(6):
        angle = 30 + 60 * i
        m.bone(f"tendril_{i}", "knot", (0, 4, 0), rotation=(0, angle, 0))
        m.cube(f"tendril_{i}", "tendril", (-1, 1, -14), (2, 3, 10), rotation=(-12, 0, 0), pivot=(0, 4, -4))
    m.bone("curl", "knot", (0, 13, 2))
    m.cube("curl", "curl", (-1, 13, -1), (2, 2, 8), rotation=(30, 0, 0), pivot=(0, 13, 0))
    return m


def root_knot_animations():
    crawl = {f"tendril_{i}": {"rotation": wave(0.8, 1, (0, 0, 0), (14, 8, 0), phase=i / 6.0)} for i in range(6)}
    crawl["knot"] = {"rotation": wave(0.8, 1, (0, 0, 0), (4, 6, 4))}
    crawl["curl"] = {"rotation": wave(0.8, 1, (0, 0, 0), (12, 0, 0), phase=0.3)}
    return animations({
        "animation.root_knot.crawl": clip(0.8, crawl),
        "animation.root_knot.burst": clip(0.6, {
            "root": {"position": keys({0.0: (0, -14, 0), 0.4: (0, 1, 0), 0.6: (0, 0, 0)})},
            **{f"tendril_{i}": {"rotation": keys({0.0: (-60, 0, 0), 0.6: (0, 0, 0)})} for i in range(6)}}, loop=False),
    })


# ------------------------------------------------------------------------------------------------- her roots

def hag_roots_model():
    """A clump of fen-roots burst up out of the water: a thick root leaning out and two thinner ones round it, wet and
    barked. Her Root Lanes are rows of these, a clump every block and a half (MireHagEntity)."""
    m = Model("hag_roots", 64, (1.5, 1.5))
    m.bone("root")
    m.bone("clump", "root", (0, 0, 0))
    m.cube("clump", "thick_root", (-1.5, -2, -1.5), (3, 15, 3), rotation=(12, 0, 8), pivot=(0, 0, 0))
    m.cube("clump", "thin_root", (1.5, -2, 0), (2, 11, 2), rotation=(-10, 0, -22), pivot=(2.5, 0, 1))
    m.cube("clump", "short_root", (-4, -2, -1), (2, 9, 2), rotation=(12, 0, 24), pivot=(-3, 0, 0))
    m.cube("clump", "short_root", (-1, -2, -4), (2, 9, 2), rotation=(26, 0, -6), pivot=(0, 0, -3), mirror=True)
    return m


def hag_roots_animations():
    """Bursting: the clump shoots up out of the water, overshooting, and settles; standing: it quivers; sinking: it is
    drawn back down."""
    return animations({
        "animation.hag_roots.burst": clip(0.3, {"clump": {"scale": keys({0.0: (1, 0.01, 1), 0.12: (1, 1.25, 1), 0.2: (1, 1, 1),
                                                                        0.3: (1, 1, 1)})}}, loop=False),
        "animation.hag_roots.stand": clip(0.6, {"clump": {"scale": wave(0.6, 2, (1, 1, 1), (0.04, 0.07, 0.04)),
                                                          "rotation": wave(0.6, 1, (0, 0, 0), (3, 0, 3))}}),
        "animation.hag_roots.sink": clip(0.5, {"clump": {"scale": keys({0.0: (1, 1, 1), 0.5: (1, 0.01, 1)})}}, loop=False),
    })


# ------------------------------------------------------------------------------------------------- her grasp

GRASP_RING = 24            # the hands' ring: its radius, in pixels (a block and a half)


def mire_grasp_model():
    """Her Mire Grasp: six hands of mud reaching up out of the water in a ring three blocks across, each a dripping
    forearm and a hand of long fingers curled inward."""
    m = Model("mire_grasp", 64, (3.5, 1.5))
    m.bone("root")
    for i in range(6):
        m.bone(f"reach_{i}", "root", (0, 0, 0), rotation=(0, 60 * i, 0))
        m.bone(f"mud_hand_{i}", f"reach_{i}", (0, 0, -GRASP_RING), rotation=(-14, 0, 0))
        m.cube(f"mud_hand_{i}", "mud_arm", (-1.5, -4, -GRASP_RING - 1.5), (3, 14, 3))
        m.cube(f"mud_hand_{i}", "mud_palm", (-2, 10, -GRASP_RING - 2), (4, 3, 3))
        for j, x in enumerate((-2.0, -0.5, 1.0)):
            m.cube(f"mud_hand_{i}", "mud_finger", (x, 13, -GRASP_RING - 1.5), (1, 5, 1), rotation=(24, 0, (j - 1) * 10),
                   pivot=(x + 0.5, 13, -GRASP_RING - 1))
    return m


def mire_grasp_animations():
    rise, hold, sink = {}, {}, {}
    for i in range(6):
        rise[f"mud_hand_{i}"] = {"position": keys({0.0: (0, -18, 0), 0.25: (0, 1, 0), 0.4: (0, 0, 0)}),
                                 "rotation": keys({0.0: (-40, 0, 0), 0.25: (10, 0, 0), 0.4: (0, 0, 0)})}
        hold[f"mud_hand_{i}"] = {"rotation": wave(0.8, 1, (0, 0, 0), (8, 0, 4), phase=i / 6.0)}
        sink[f"mud_hand_{i}"] = {"position": keys({0.0: (0, 0, 0), 0.5: (0, -18, 0)})}
    return animations({
        "animation.mire_grasp.rise": clip(0.4, rise, loop=False),
        "animation.mire_grasp.hold": clip(0.8, hold),
        "animation.mire_grasp.sink": clip(0.5, sink, loop=False),
    })


# ------------------------------------------------------------------------------------------------- her brew

def poison_brew_model():
    """A flask of her brew: a squat round bottle of green glass, its venom glowing inside, a stub of a neck and a cork,
    tumbling as it flies."""
    m = Model("poison_brew", 32, (0.6, 0.6))
    m.bone("root")
    m.bone("flask", "root", (0, 4, 0))
    m.cube("flask", "bottle", (-2.5, 1, -2.5), (5, 5, 5))
    m.cube("flask", "neck", (-1, 6, -1), (2, 2, 2))
    m.cube("flask", "cork", (-1.5, 8, -1.5), (3, 1, 3))
    return m


def poison_brew_animations():
    return animations({"animation.poison_brew.spin": clip(0.5, {"flask": {
        "rotation": keys({0.0: (0, 0, 0), 0.25: (180, 90, 30), 0.5: (360, 180, 0)})}})})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"mire_hag": mire_hag_model, "root_knot": root_knot_model, "hag_roots": hag_roots_model,
          "mire_grasp": mire_grasp_model, "poison_brew": poison_brew_model}
ANIMATIONS = {"mire_hag": mire_hag_animations, "root_knot": root_knot_animations, "hag_roots": hag_roots_animations,
              "mire_grasp": mire_grasp_animations, "poison_brew": poison_brew_animations}
# The clips the Java names (MireHagEntity and HagLikenessEntity, and her things).
CLIPS = {
    "mire_hag": ("stir", "wake", "idle", "walk", "swipe_windup", "swipe", "lanes_windup", "lanes", "brew_windup", "brew",
                 "exchange_windup", "exchange", "cackle", "grasp_windup", "grasp", "snuff_windup", "snuff", "death",
                 "lantern_lit", "lantern_flare", "lantern_out"),
    "root_knot": ("crawl", "burst"),
    "hag_roots": ("burst", "stand", "sink"),
    "mire_grasp": ("rise", "hold", "sink"),
    "poison_brew": ("spin",),
}
# The bones each of the Hag's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"lantern": LANTERN_BONES}


def _check():
    """Every model packs; no clip of the Hag's body keys a lantern bone, nor the lantern's any other."""
    for name, build in MODELS.items():
        build().pack()
    anims = mire_hag_animations()["animations"]
    for clip_name, body in anims.items():
        lantern = clip_name.split(".")[-1].startswith("lantern_")
        for bone in body["bones"]:
            if (bone in LANTERN_BONES) != lantern:
                raise ValueError(f"{clip_name} keys {bone}")
    return math.isfinite(HAND_Z)


if __name__ == "__main__":
    print(_check())
