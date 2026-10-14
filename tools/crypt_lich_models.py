"""GeckoLib bodies for the Crypt Lich and his fight (docs/features/crypt-lich.md): the Lich himself, his Restless Dead,
the Soul Bolt he looses and the bones that burst up along his Bone Lanes and round his feet at his Grave Pull.
Geometry and animations only, as plain dicts; no I/O. tools/crypt_lich_art.py paints their sheets on the same box-UV
regions, at TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"). The builder and
the posing helpers are Vesperine's (tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one
block), y = 0 at the feet, every model facing north (-z) with its left on +x; +x pitches a bone's top forward, -x raises
a hanging arm forward, a right arm turns outwards with +z and a left one with -z, and +y turns the front to the model's
right. A keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. MODELS, ANIMATIONS and CLIPS are keyed by model; CLIPS lists the clips the Java plays.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS", "HAND_Z", "HAND_Y", "GRIP", "SEAT_DROP"]

# ------------------------------------------------------------------------------------------------- the Lich

# A crowned skeleton, gaunt and a head taller than a man: a scholar's robe gone to mould, long to the floor, its hem
# ragged, with chains of tarnished gold draped across its front; a mantle over his shoulders and a high collar standing
# behind his skull; long bony arms in bell sleeves, bony fingers; a crown of black iron with points, the soul-fire
# burning on them (the "crown" controller's: dark while he waits, lit, flaring in the Last Name); eyes of cold blue
# flame; his staff a thighbone bound with gold to a sceptre, a soul gem in its prongs. A skull of soul-fire forms in his
# left hand as he readies a Soul Bolt (the "hand" controller's). The body's clips never key the crown's fire or the
# skull in his hand.
GRIP = (-7.5, 9.5, -0.5)    # the staff through his right hand (its middle), at rest
# Where his left hand is as he looses a bolt (the bolt clip at its release): ahead of his feet and over them, in pixels.
# CryptLichEntity's bolt leaves from here (tools/crypt_lich.py BOLT HAND_AHEAD and HAND_UP).
HAND_Z = -21.0
HAND_Y = 27.5
# Seated on his throne: how far his root sinks (pixels), so that his lap rests on the seat.
SEAT_DROP = 7.0


def crypt_lich_model():
    m = Model("crypt_lich", 128, (2.5, 3.0))
    m.bone("root")
    # His robe below the waist: the lap, and the hem from the knees, flaring to the floor, ragged.
    m.bone("lap", "root", (0, 15, 0))
    m.cube("lap", "lap", (-5, 8, -4), (10, 7, 8))
    m.bone("hem", "lap", (0, 8, 0))
    m.cube("hem", "hem", (-6, 0, -5), (12, 8, 10))
    # The robe above the waist, the chains of gold draped before it, the mantle over his shoulders and the high collar
    # behind his skull.
    m.bone("body", "root", (0, 15, 0))
    m.cube("body", "robe", (-5, 15, -3.5), (10, 11, 7))
    m.cube("body", "chains", (-5.5, 16, -4.0), (11, 8, 0))
    m.cube("body", "mantle", (-6.5, 23, -4.5), (13, 4, 9))
    m.cube("body", "collar", (-5.5, 25, 3.5), (11, 8, 1))
    # The skull, its jaw, and the crown of black iron on it: a band and eight points; the soul-fire on the points is the
    # crown controller's (crown_fire).
    m.bone("head", "body", (0, 26, 0))
    m.cube("head", "skull", (-4, 27, -4.5), (8, 7, 8))
    m.bone("jaw", "head", (0, 28, 1))
    m.cube("jaw", "jaw", (-3, 26, -4.5), (6, 2, 5))
    m.bone("crown", "head", (0, 34, -0.5))
    m.cube("crown", "crown_band", (-4.5, 33.5, -5), (9, 2, 9))
    points = [(-4.5, -5), (-0.5, -5), (3.5, -5), (-4.5, -1), (3.5, -1), (-4.5, 3), (-0.5, 3), (3.5, 3)]
    for x, z in points:
        m.cube("crown", "crown_point", (x, 35.5, z), (1, 3, 1))
    m.bone("crown_fire", "crown", (0, 38.5, -0.5))
    for x, z in points:
        m.cube("crown_fire", "crown_flame", (x, 38.5, z), (1, 2, 1))
    # His arms: long, bony, in bell sleeves; the right hand on his staff, the left free for his spells.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0

        def ox(v, w):  # a right-side origin x for a cube `w` wide, mirrored to the left
            return v if side < 0 else -v - w

        m.bone(f"arm_{name}", "body", (side * 7.5, 25, 0))
        m.cube(f"arm_{name}", "upper_sleeve", (ox(-9, 3), 18, -2), (3, 8, 4), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (side * 7.5, 18, 0))
        m.cube(f"forearm_{name}", "lower_sleeve", (ox(-9.5, 4), 11, -2.5), (4, 7, 5), mirror=mirror)
        m.bone(f"hand_{name}", f"forearm_{name}", (side * 7.5, 11, 0))
        m.cube(f"hand_{name}", "hand", (ox(-8.5, 2), 8, -1), (2, 3, 2), mirror=mirror)
        for i, dz in enumerate((-1.0, 0.0)):
            m.cube(f"hand_{name}", "finger", (ox(-8.5 + i, 1), 5, -1 + dz), (1, 3, 1),
                   rotation=(-10 + 12 * i, 0, 0), pivot=(side * (7.5 + 0.5 - i), 8, -0.5 + dz), mirror=mirror)
    # The skull of soul-fire that forms in his left hand (the hand controller's alone).
    m.bone("soul_skull", "hand_left", (7.5, 7, -1.5))
    m.cube("soul_skull", "soul_skull", (5.5, 2, -3.5), (4, 4, 4))
    # His staff turns in his hand against the arm's pitch, so it stands upright however he holds it (staff_grip): a
    # thighbone, knobbed at each end and bound twice with gold, a gold cup on top with four prongs round a soul gem.
    gx, gy, gz = GRIP
    m.bone("staff_grip", "hand_right", GRIP)
    m.bone("staff", "staff_grip", GRIP)
    m.cube("staff", "femur", (gx - 1, gy - 8.5, gz - 1), (2, 28, 2))
    m.cube("staff", "femur_knob", (gx - 1.5, gy - 9.5, gz - 1.5), (3, 3, 3))
    m.cube("staff", "femur_knob", (gx - 1.5, gy + 17.5, gz - 1.5), (3, 3, 3))
    m.cube("staff", "binding", (gx - 1.5, gy + 3, gz - 1.5), (3, 1, 3))
    m.cube("staff", "binding", (gx - 1.5, gy + 12, gz - 1.5), (3, 1, 3))
    top = gy + 20.5
    m.cube("staff", "sceptre_cup", (gx - 1.5, top, gz - 1.5), (3, 2, 3))
    m.cube("staff", "gem", (gx - 1, top + 2, gz - 1), (2, 2, 2))
    for dx, dz in ((-1.5, -1.5), (0.5, -1.5), (-1.5, 0.5), (0.5, 0.5)):
        m.cube("staff", "sceptre_prong", (gx + dx, top + 2, gz + dz), (1, 3, 1))
    return m


# The crown controller's bone, and the hand controller's; the body's clips never key them.
CROWN_BONES = ("crown_fire",)
HAND_BONES = ("soul_skull",)
STAFF_PITCH = ("body", "arm_right", "forearm_right", "hand_right")


def staffed(pose, lean=0.0, roll=0.0):
    """The pose with his staff turned back against his right arm's pitch (and the body's), so it stands upright,
    leaning `lean` degrees forward and `roll` out to his right."""
    pitch = sum(pose.get(bone, (0, 0, 0))[0] if not isinstance(pose.get(bone), dict) else 0 for bone in STAFF_PITCH)
    out = dict(pose)
    out["staff_grip"] = (lean - pitch, 0, -roll - (pose.get("arm_right", (0, 0, 0))[2]))
    return out


def sway(length, cycles=1, scale=1.0):
    """His robe's hem and his jaw stirring, each on its own beat."""
    s = scale
    return {
        "hem": {"rotation": wave(length, cycles, (0, 0, 0), (2 * s, 0, 1.5 * s), phase=0.1)},
        "jaw": {"rotation": wave(length, cycles, (6, 0, 0), (4 * s, 0, 0), phase=0.35)},
    }


# Standing tall, his staff planted at his side, his left hand half raised: his stance between attacks.
IDLE = staffed({"arm_right": (-18, 0, 6), "forearm_right": (-14, 0, 0), "arm_left": (-20, 0, -10),
                "forearm_left": (-30, 0, 0), "head": (4, 0, 0)}, roll=2)
# Seated on his throne: his root sunk to the seat, his lap level and his hem hanging from his knees, his head bowed, the
# staff upright in his right hand, his left hand on his knee.
SEATED = staffed({"root": {"position": (0, -SEAT_DROP, 0)}, "lap": (-90, 0, 0), "hem": (90, 0, 0), "head": (24, 0, 0),
                  "arm_right": (-30, 0, 10), "forearm_right": (-34, 0, 0), "arm_left": (-40, 0, -6),
                  "forearm_left": (-30, 0, 0)}, roll=4)
# Rising: half up, his head lifted, his arms spreading.
RISING = staffed({"root": {"position": (0, -3, 0)}, "lap": (-40, 0, 0), "hem": (40, 0, 0), "head": (-14, 0, 0),
                  "arm_right": (-40, 0, 24), "forearm_right": (-10, 0, 0), "arm_left": (-50, 0, -40),
                  "forearm_left": (-10, 0, 0)}, roll=10)
SPREAD = staffed({"head": (-20, 0, 0), "jaw": (20, 0, 0), "arm_right": (-60, 0, 36), "forearm_right": (-10, 0, 0),
                  "arm_left": (-70, 0, -60), "forearm_left": (-10, 0, 0)}, roll=14)
# Grave Sweep: the staff drawn back over his right shoulder; then swept across his front.
SWEEP_BACK = {"body": (-6, -30, 0), "head": (0, 20, 0), "arm_right": (-150, 0, 30), "forearm_right": (-30, 0, 0),
              "staff_grip": (60, 0, 40), "arm_left": (-40, 0, -24), "forearm_left": (-20, 0, 0)}
SWEEP_THROUGH = {"body": (10, 40, 0), "head": (0, -24, 0), "arm_right": (-80, 0, -20), "forearm_right": (-10, 0, 0),
                 "staff_grip": (100, 0, -60), "arm_left": (-20, 0, -30), "forearm_left": (-20, 0, 0), "hem": (0, 0, -6)}
# Bone Lanes: the staff raised high as he calls a name (his jaw wide), then struck down before him and held.
STAFF_HIGH = staffed({"body": (-10, 0, 0), "head": (-26, 0, 0), "jaw": (26, 0, 0), "arm_right": (-160, 0, 10),
                      "forearm_right": (-6, 0, 0), "arm_left": (-60, 0, -40), "forearm_left": (-20, 0, 0)}, lean=-4)
STAFF_STRUCK = staffed({"body": (16, 0, 0), "head": (10, 0, 0), "arm_right": (-70, 0, 4), "forearm_right": (-10, 0, 0),
                        "arm_left": (-30, 0, -20), "forearm_left": (-20, 0, 0)}, lean=-20)
# Soul Bolt: his left hand raised before him, palm up, as the skull forms in it; then thrust out.
CUP = staffed({"body": (-4, -10, 0), "head": (10, 10, 0), "arm_left": (-40, 0, 10), "forearm_left": (-60, 0, 0),
               "hand_left": (-10, 0, 0), "arm_right": (-18, 0, 6), "forearm_right": (-14, 0, 0)}, roll=2)
THRUST = staffed({"body": (8, 16, 0), "head": (-4, -10, 0), "arm_left": (-96, 0, -6), "forearm_left": (-4, 0, 0),
                  "hand_left": (-10, 0, 0), "arm_right": (-18, 0, 6), "forearm_right": (-14, 0, 0)}, roll=2)
# Raise the Dead: both arms spread wide and lifted, his head thrown back; then swept up.
SUMMON = staffed({"body": (-12, 0, 0), "head": (-30, 0, 0), "jaw": (22, 0, 0), "arm_right": (-20, 0, 75),
                  "forearm_right": (-10, 0, 0), "arm_left": (-30, 0, -80), "forearm_left": (-20, 0, 0)}, roll=10)
SUMMON_UP = staffed({"body": (-16, 0, 0), "head": (-34, 0, 0), "jaw": (28, 0, 0), "arm_right": (-170, 0, 20),
                     "forearm_right": (0, 0, 0), "arm_left": (-170, 0, -30), "forearm_left": (0, 0, 0)}, roll=0)
# Staggered (his chains broken, or his own bolt struck back at him while warded): reeling back, his head down, his staff
# drooping, his left hand at his skull.
REEL = staffed({"body": (-16, 10, 8), "head": (30, 0, 10), "arm_right": (-10, 0, 30), "forearm_right": (-10, 0, 0),
                "arm_left": (-140, 0, -10), "forearm_left": (-80, 0, 0), "hem": (10, 0, 0)}, lean=-24, roll=20)
# The Erasure: he shrieks, his head thrown back and his jaw wide, his left hand clawing at the air.
SHRIEK = staffed({"body": (-20, 0, 0), "head": (-40, 0, 0), "jaw": (40, 0, 0), "arm_left": (-150, 0, -40),
                  "forearm_left": (-30, 0, 0), "hand_left": (30, 0, 0), "arm_right": (-40, 0, 20),
                  "forearm_right": (-20, 0, 0)}, roll=10)
# Soul Drain: his left hand stretched at his foe, fingers spread, leaning in.
REACH = staffed({"body": (10, -14, 0), "head": (-6, 14, 0), "jaw": (14, 0, 0), "arm_left": (-96, 0, -4),
                 "forearm_left": (-4, 0, 0), "hand_left": (-20, 0, 0), "arm_right": (-18, 0, 10),
                 "forearm_right": (-14, 0, 0)}, roll=4)
# Grave Pull: his staff raised over his head, his left hand sweeping round; then the staff driven down into the floor.
PULL_HIGH = staffed({"body": (-8, 20, 0), "head": (-16, -10, 0), "arm_right": (-170, 0, 0), "forearm_right": (0, 0, 0),
                     "arm_left": (-80, 0, -80), "forearm_left": (-20, 0, 0)}, lean=0)
PULL_DOWN = staffed({"body": (24, 0, 0), "head": (16, 0, 0), "arm_right": (-50, 0, 4), "forearm_right": (-20, 0, 0),
                     "arm_left": (-40, 0, -30), "forearm_left": (-20, 0, 0), "hem": (-8, 0, 0)}, lean=-30)
# Death: he sinks to his knees and slumps forward; his crown rolls from his skull.
KNEEL = staffed({"root": {"position": (0, -6, 0)}, "lap": (-70, 0, 0), "hem": (100, 0, 0), "body": (20, 0, 0),
                 "head": (30, 0, 0), "arm_right": (-10, 0, 20), "forearm_right": (-10, 0, 0), "arm_left": (-10, 0, -20),
                 "forearm_left": (-10, 0, 0)}, lean=20, roll=20)
FALLEN = {"root": {"position": (0, -12, 0)}, "lap": (-80, 0, 0), "hem": (100, 0, 0), "body": (60, 0, 10),
          "head": (40, 0, 10), "arm_right": (20, 0, 50), "forearm_right": (-10, 0, 0), "arm_left": (20, 0, -40),
          "forearm_left": (-10, 0, 0), "staff_grip": (-80, 0, -10),
          "crown": {"rotation": (70, 0, 50), "position": (6, -22, -10)}}


def _glide(length):
    """His glide: leaning a little into it, his hem trailing and swaying, his left hand swinging."""
    out = sway(length, 2, 1.4)
    out["hem"] = {"rotation": wave(length, 2, (14, 0, 0), (4, 0, 4), phase=0.25)}
    out["body"] = {"rotation": wave(length, 1, (8, 0, 0), (2, 4, 2))}
    out["arm_left"] = {"rotation": wave(length, 1, (-20, 0, -10), (14, 0, 0))}
    return out


def _floating(length):
    """A slow float a pixel up and down, as he hangs over the floor."""
    return {"root": {"position": wave(length, 1, (0, 0.5, 0), (0, 0.5, 0))}}


def crypt_lich_animations():
    """His body's clips (the "body" controller), his crown's fire (dark, lit, flaring) and the skull in his hand (gone,
    or forming). The body's never key crown_fire or soul_skull, nor the others any other bone."""
    idle_waves = {**sway(3.0), **_floating(3.0)}
    return animations({
        "animation.crypt_lich.throne": posed(4.0, [(0.0, SEATED), (4.0, SEATED)], loop=True, waves={
            "jaw": {"rotation": wave(4.0, 1, (2, 0, 0), (2, 0, 0))}}),
        "animation.crypt_lich.wake": posed(2.0, [(0.0, SEATED), (0.8, RISING), (1.4, SPREAD), (2.0, IDLE)]),
        "animation.crypt_lich.idle": posed(3.0, [(0.0, IDLE), (3.0, IDLE)], loop=True, waves=idle_waves),
        "animation.crypt_lich.walk": posed(1.6, [(0.0, IDLE), (1.6, IDLE)], loop=True, waves={**_glide(1.6), **_floating(1.6)}),
        "animation.crypt_lich.sweep_windup": posed(0.7, [(0.0, IDLE), (0.7, SWEEP_BACK)]),
        "animation.crypt_lich.sweep": posed(0.7, [(0.0, SWEEP_BACK), (0.15, SWEEP_THROUGH), (0.35, SWEEP_THROUGH),
                                                  (0.7, IDLE)]),
        "animation.crypt_lich.lanes_windup": posed(1.2, [(0.0, IDLE), (0.5, STAFF_HIGH), (1.05, STAFF_HIGH),
                                                         (1.2, STAFF_STRUCK)]),
        "animation.crypt_lich.lanes": posed(1.5, [(0.0, STAFF_STRUCK), (1.0, STAFF_STRUCK), (1.5, IDLE)]),
        "animation.crypt_lich.bolt_windup": posed(0.8, [(0.0, IDLE), (0.4, CUP), (0.8, CUP)]),
        "animation.crypt_lich.bolt": posed(0.65, [(0.0, CUP), (0.1, THRUST), (0.3, THRUST), (0.65, IDLE)]),
        "animation.crypt_lich.raise_windup": posed(1.5, [(0.0, IDLE), (0.6, SUMMON), (1.5, SUMMON)]),
        "animation.crypt_lich.raise": posed(0.6, [(0.0, SUMMON), (0.2, SUMMON_UP), (0.6, IDLE)]),
        "animation.crypt_lich.stagger": posed(1.0, [(0.0, REEL), (1.0, REEL)], loop=True, waves={
            "body": {"rotation": wave(1.0, 1, REEL["body"], (4, 6, 4))},
            "head": {"rotation": wave(1.0, 2, REEL["head"], (6, 8, 0), phase=0.2)}}),
        "animation.crypt_lich.shriek": posed(0.5, [(0.0, SHRIEK), (0.5, SHRIEK)], loop=True, waves={
            "body": {"rotation": wave(0.5, 2, SHRIEK["body"], (3, 4, 3))},
            "head": {"rotation": wave(0.5, 3, SHRIEK["head"], (4, 6, 4), phase=0.25)},
            "jaw": {"rotation": wave(0.5, 4, SHRIEK["jaw"], (6, 0, 0))}}),
        "animation.crypt_lich.drain_windup": posed(1.0, [(0.0, IDLE), (0.5, REACH), (1.0, REACH)]),
        "animation.crypt_lich.drain": posed(0.6, [(0.0, REACH), (0.6, REACH)], loop=True, waves={
            "forearm_left": {"rotation": wave(0.6, 3, REACH["forearm_left"], (2, 3, 2))},
            "hand_left": {"rotation": wave(0.6, 2, REACH["hand_left"], (6, 0, 4))},
            "jaw": {"rotation": wave(0.6, 2, REACH["jaw"], (6, 0, 0))}}),
        "animation.crypt_lich.pull_windup": posed(1.5, [(0.0, IDLE), (0.6, PULL_HIGH), (1.5, PULL_HIGH)]),
        "animation.crypt_lich.pull": posed(1.2, [(0.0, PULL_DOWN), (0.8, PULL_DOWN), (1.2, IDLE)]),
        "animation.crypt_lich.death": posed(2.5, [(0.0, IDLE), (0.8, KNEEL), (1.8, FALLEN), (2.5, FALLEN)]),
        # The soul-fire on his crown: dark while he waits, burning, flaring in the Last Name.
        "animation.crypt_lich.crown_dark": clip(1.0, {"crown_fire": {"scale": still((0, 0, 0))}}),
        "animation.crypt_lich.crown_lit": clip(1.0, {"crown_fire": {"scale": wave(1.0, 2, (1, 1, 1), (0.1, 0.25, 0.1))}}),
        "animation.crypt_lich.crown_flare": clip(0.4, {"crown_fire": {"scale": wave(0.4, 2, (1.4, 2.0, 1.4), (0.15, 0.4, 0.15))}}),
        # The skull in his hand: gone, or forming and burning.
        "animation.crypt_lich.hand_empty": clip(1.0, {"soul_skull": {"scale": still((0, 0, 0))}}),
        "animation.crypt_lich.hand_skull": clip(0.8, {"soul_skull": {
            "scale": keys({0.0: (0.2, 0.2, 0.2), 0.4: (1.0, 1.0, 1.0), 0.6: (1.1, 1.1, 1.1), 0.8: (1.0, 1.0, 1.0)}),
            "rotation": keys({0.0: (0, 0, 0), 0.8: (0, 90, 0)})}}, loop=False),
    })


# ------------------------------------------------------------------------------------------------- the Restless Dead

def restless_dead_model():
    """A Restless Dead: a skeleton a little shorter than a man, hunched, its jaw hanging, its eyes cold blue flames, in a
    torn grave-shroud over its shoulders and hips; its arms long and reaching."""
    m = Model("restless_dead", 64, (1.5, 2.2))
    m.bone("root")
    m.bone("body", "root", (0, 12, 0), rotation=(14, 0, 0))
    m.cube("body", "pelvis", (-3, 11, -1.5), (6, 2, 3))
    m.cube("body", "spine", (-1, 13, -0.5), (2, 9, 2))
    m.cube("body", "ribs", (-4, 16, -2), (8, 6, 4))
    m.cube("body", "shroud", (-4.5, 15, -2.5), (9, 8, 5))
    m.bone("rag", "body", (0, 15, 2.5))
    m.cube("rag", "rag", (-4, 7, 2.5), (8, 8, 0))
    m.bone("head", "body", (0, 22, 0), rotation=(-10, 0, 0))
    m.cube("head", "dead_skull", (-3.5, 22, -4), (7, 6, 7))
    m.bone("jaw", "head", (0, 23, 1), rotation=(18, 0, 0))
    m.cube("jaw", "dead_jaw", (-2.5, 21, -3.5), (5, 2, 5))
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        m.bone(f"arm_{name}", "body", (side * 5, 21, 0), rotation=(-50, 0, side * -6))
        m.cube(f"arm_{name}", "dead_arm", ((-6 if side < 0 else 4), 9, -1), (2, 12, 2), mirror=mirror)
        m.cube(f"arm_{name}", "dead_claw", ((-6.5 if side < 0 else 3.5), 6, -1.5), (3, 3, 3), mirror=mirror)
        m.bone(f"leg_{name}", "root", (side * 2, 12, 0))
        m.cube(f"leg_{name}", "dead_leg", ((-3 if side < 0 else 1), 0, -1), (2, 12, 2), mirror=mirror)
    return m


def restless_dead_animations():
    shamble = {
        "leg_right": {"rotation": wave(1.2, 1, (0, 0, 0), (24, 0, 0))},
        "leg_left": {"rotation": wave(1.2, 1, (0, 0, 0), (24, 0, 0), phase=0.5)},
        "arm_right": {"rotation": wave(1.2, 1, (0, 0, 0), (8, 0, 4), phase=0.5)},
        "arm_left": {"rotation": wave(1.2, 1, (0, 0, 0), (8, 0, 4))},
        "body": {"rotation": wave(1.2, 2, (0, 0, 0), (2, 0, 4))},
        "head": {"rotation": wave(1.2, 1, (0, 0, 0), (4, 8, 6), phase=0.2)},
        "rag": {"rotation": wave(1.2, 2, (0, 0, 0), (8, 0, 0), phase=0.3)},
    }
    idle = {
        "body": {"rotation": wave(2.0, 1, (0, 0, 0), (2, 0, 2))},
        "head": {"rotation": wave(2.0, 1, (0, 0, 0), (4, 10, 4), phase=0.25)},
        "jaw": {"rotation": wave(2.0, 2, (0, 0, 0), (6, 0, 0))},
        "rag": {"rotation": wave(2.0, 1, (0, 0, 0), (6, 0, 0), phase=0.4)},
    }
    climb = {
        "root": {"position": keys({0.0: (0, -26, 0), 0.6: (0, -8, 0), 1.0: (0, 0, 0)})},
        "body": {"rotation": keys({0.0: (40, 0, 0), 0.6: (30, 0, 0), 1.0: (0, 0, 0)})},
        "arm_right": {"rotation": keys({0.0: (-130, 0, 0), 0.3: (-90, 0, 10), 0.6: (-40, 0, 0), 1.0: (0, 0, 0)})},
        "arm_left": {"rotation": keys({0.0: (-110, 0, 0), 0.3: (-130, 0, -10), 0.6: (-60, 0, 0), 1.0: (0, 0, 0)})},
    }
    attack = {
        "arm_right": {"rotation": keys({0.0: (0, 0, 0), 0.15: (-60, 0, 20), 0.3: (30, 0, -10), 0.6: (0, 0, 0)})},
        "arm_left": {"rotation": keys({0.0: (0, 0, 0), 0.2: (-50, 0, -20), 0.35: (30, 0, 10), 0.6: (0, 0, 0)})},
        "body": {"rotation": keys({0.0: (0, 0, 0), 0.25: (10, 0, 0), 0.6: (0, 0, 0)})},
        "jaw": {"rotation": keys({0.0: (0, 0, 0), 0.2: (20, 0, 0), 0.6: (0, 0, 0)})},
    }
    return animations({
        "animation.restless_dead.climb": clip(1.0, climb, loop=False),
        "animation.restless_dead.idle": clip(2.0, idle),
        "animation.restless_dead.walk": clip(1.2, shamble),
        "animation.restless_dead.attack": clip(0.6, attack, loop=False),
    })


# ------------------------------------------------------------------------------------------------- the Soul Bolt

def soul_bolt_model():
    """A Soul Bolt: a skull of soul-fire, its jaw chattering, flames streaming back from it."""
    m = Model("soul_bolt", 64, (1.0, 1.0))
    m.bone("root")
    m.bone("bolt", "root", (0, 5, 0))
    m.cube("bolt", "bolt_skull", (-3, 3, -3), (6, 5, 6))
    m.bone("bolt_jaw", "bolt", (0, 3, 1))
    m.cube("bolt_jaw", "bolt_jaw", (-2, 2, -3), (4, 1, 4))
    m.bone("trail", "bolt", (0, 5.5, 3))
    m.cube("trail", "trail", (0, 2, 3), (0, 7, 8))
    m.cube("trail", "trail_side", (-3.5, 5.5, 3), (7, 0, 7))
    return m


def soul_bolt_animations():
    return animations({"animation.soul_bolt.drift": clip(0.6, {
        "bolt": {"rotation": wave(0.6, 1, (0, 0, 0), (6, 0, 8)), "position": wave(0.6, 1, (0, 0, 0), (0, 0.6, 0))},
        "bolt_jaw": {"rotation": wave(0.6, 2, (12, 0, 0), (12, 0, 0))},
        "trail": {"rotation": wave(0.6, 2, (0, 0, 0), (10, 12, 0), phase=0.25),
                  "scale": wave(0.6, 2, (1, 1, 1), (0.05, 0.15, 0.15))},
    })})


# ------------------------------------------------------------------------------------------------- his bones

def lich_bones_model():
    """A clump of old bones bursting up out of the floor, three blocks across: a thighbone standing tall in the middle,
    four more leaning out round it, two curved ribs between them, all on a ragged footing of bone. His Bone Lanes are rows
    of these down a lane's middle, a clump every two blocks, and his Grave Pull a ring of them round his feet."""
    m = Model("lich_bones", 64, (3.0, 1.5))
    m.bone("root")
    m.bone("clump", "root", (0, 0, 0))
    m.cube("clump", "bone_base", (-7, -1, -7), (14, 2, 14))
    m.cube("clump", "tall_bone", (-1.5, 0, -1.5), (3, 18, 3), rotation=(6, 0, 4), pivot=(0, 0, 0))
    for i, (angle, lean, length) in enumerate(((20, 28, 14), (110, 34, 12), (200, 26, 14), (290, 36, 12))):
        a = math.radians(angle)
        x, z = round(math.sin(a) * 13, 2), round(-math.cos(a) * 13, 2)
        part = "long_bone" if length == 14 else "short_bone"
        m.cube("clump", part, (x - 1, 0, z - 1), (2, length, 2), rotation=(0, angle, lean), pivot=(x, 0, z))
    for angle in (65, 245):
        a = math.radians(angle)
        x, z = round(math.sin(a) * 8, 2), round(-math.cos(a) * 8, 2)
        m.cube("clump", "rib", (x - 0.5, 0, z - 4), (1, 10, 8), rotation=(0, angle, 22), pivot=(x, 0, z))
    return m


def lich_bones_animations():
    """Bursting: the clump shoots up out of the floor, overshooting, and settles; standing: it quivers; sinking: it is
    drawn back down."""
    return animations({
        "animation.lich_bones.burst": clip(0.3, {"clump": {"scale": keys({0.0: (1, 0.01, 1), 0.12: (1, 1.25, 1),
                                                                         0.2: (1, 1, 1), 0.3: (1, 1, 1)})}}, loop=False),
        "animation.lich_bones.stand": clip(0.6, {"clump": {"scale": wave(0.6, 2, (1, 1, 1), (0.03, 0.06, 0.03)),
                                                           "rotation": wave(0.6, 1, (0, 0, 0), (2, 0, 2))}}),
        "animation.lich_bones.sink": clip(0.5, {"clump": {"scale": keys({0.0: (1, 1, 1), 0.5: (1, 0.01, 1)})}}, loop=False),
    })


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"crypt_lich": crypt_lich_model, "restless_dead": restless_dead_model, "soul_bolt": soul_bolt_model,
          "lich_bones": lich_bones_model}
ANIMATIONS = {"crypt_lich": crypt_lich_animations, "restless_dead": restless_dead_animations,
              "soul_bolt": soul_bolt_animations, "lich_bones": lich_bones_animations}
# The clips the Java names (CryptLichEntity, RestlessDeadEntity, SoulBoltEntity and LichBonesEntity).
CLIPS = {
    "crypt_lich": ("throne", "wake", "idle", "walk", "sweep_windup", "sweep", "lanes_windup", "lanes", "bolt_windup",
                   "bolt", "raise_windup", "raise", "stagger", "shriek", "drain_windup", "drain", "pull_windup", "pull",
                   "death", "crown_dark", "crown_lit", "crown_flare", "hand_empty", "hand_skull"),
    "restless_dead": ("climb", "idle", "walk", "attack"),
    "soul_bolt": ("drift",),
    "lich_bones": ("burst", "stand", "sink"),
}
# The bones each of the Lich's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"crown": CROWN_BONES, "hand": HAND_BONES}


def _check():
    """Every model packs; no clip of the Lich's body keys a crown or hand bone, nor theirs any other."""
    for name, build in MODELS.items():
        build().pack()
    anims = crypt_lich_animations()["animations"]
    for clip_name, body in anims.items():
        short = clip_name.split(".")[-1]
        owner = "crown" if short.startswith("crown_") else "hand" if short.startswith("hand_") else "body"
        for bone in body["bones"]:
            controller = next((c for c, bones in CONTROLLERS.items() if bone in bones), "body")
            if controller != owner:
                raise ValueError(f"{clip_name} keys {bone}")
    return math.isfinite(HAND_Z)


if __name__ == "__main__":
    print(_check())
