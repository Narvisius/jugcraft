"""GeckoLib bodies for the Cydonian Sentinel and its fight (docs/features/cydonian-sentinel.md): the Sentinel itself, its
Shard Sentries, the Rust Shards it and they loose, the spikes of its Quake Line and the meteors of its Starfall.
Geometry and animations only, as plain dicts; no I/O. tools/cydonian_sentinel_art.py paints their sheets on the same
box-UV regions, at TEXTURE_SCALE times the size the models declare (docs/ART_DIRECTION.md, "High resolution"). The
builder and the posing helpers are Vesperine's (tools/vesperine_models.py), which also sets out the conventions: pixels
(16 = one block), y = 0 at the feet, every model facing north (-z) with its left on +x; +x pitches a bone's top forward,
-x raises a hanging arm forward, a right arm turns outwards with +z and a left one with -z, and +y turns the front to
the model's right. A keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. MODELS, ANIMATIONS and CLIPS are keyed by model; CLIPS lists the clips the Java plays.
"""
import math

from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "CLIPS", "CONTROLLERS", "CORE_Y", "CORE_FRONT", "UNBOUND_DROP",
           "UNBOUND_CORE_Y"]

# ------------------------------------------------------------------------------------------------- the Sentinel

# A construct four blocks tall of red stone plates and rusted iron, built round a floating core of red light: legs like
# stacked blocks on broad feet; a chest of stone round a hollow where the core hangs, closed in front by two plates
# (cut with the altar's red star) that swing open when it charges its lance; pauldrons of rusted iron; arms that are
# pillars, ending in fists of stone; a helm with a carved visor, a red slit of light across it. Rust shards bristle
# along its forearms as it readies a volley.
#
# Its bones fall to four controllers, which never share one: "body" (its poses), "core" (core_lit and core_cold: the
# core dark, lit, flaring, opened, unbound or dimmed), "plates" (the chest plates, shut or open) and "form" (the shell,
# whole or gone, and the ring of shards that orbits the unbound core).
CORE_Y = 41.0          # the core's middle, over its feet (pixels): where its Core Lance leaves from
CORE_FRONT = -6.0      # the chest's front (pixels, z): the lance leaves from here, before the core
CORE_Z = -1.5          # the core's middle, front to back
UNBOUND_DROP = 29.0    # unbound, the core hangs this much lower over its feet than in the chest
UNBOUND_CORE_Y = CORE_Y - UNBOUND_DROP   # so its middle is here (pixels over its feet): 0.75 blocks
ORBIT = 14.0           # the radius the shards orbit the unbound core at


def cydonian_sentinel_model():
    m = Model("cydonian_sentinel", 128, (3.0, 4.5))
    m.bone("root")
    m.bone("shell", "root", (0, 0, 0))
    # Its hips, and its legs: a thigh, a shin with a knee plate, and a broad foot.
    m.bone("pelvis", "shell", (0, 29, 0))
    m.cube("pelvis", "hips", (-8, 26, -5), (16, 6, 10))
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0

        def ox(v, w):  # a right-side origin x for a cube `w` wide, mirrored to the left
            return v if side < 0 else -v - w

        m.bone(f"leg_{name}", "shell", (side * 5, 28, 0))
        m.cube(f"leg_{name}", "thigh", (ox(-8.5, 7), 15, -3.5), (7, 13, 7), mirror=mirror)
        m.bone(f"shin_{name}", f"leg_{name}", (side * 5, 15, 0))
        m.cube(f"shin_{name}", "shin", (ox(-9, 8), 3, -4), (8, 12, 8), mirror=mirror)
        m.cube(f"shin_{name}", "knee", (ox(-8, 6), 11, -5), (6, 5, 1), mirror=mirror)
        m.cube(f"shin_{name}", "foot", (ox(-9.5, 9), 0, -6.5), (9, 3, 10), mirror=mirror)
    # Its chest: stone round a hollow (x -5..5, y 35..47, z -6..3) where the core hangs; two plates close it in front.
    m.bone("torso", "shell", (0, 32, 0))
    m.cube("torso", "waist", (-6, 30, -4), (12, 3, 8))
    m.cube("torso", "chest_back", (-10, 32, 3), (20, 18, 4))
    m.cube("torso", "chest_side", (-10, 32, -6), (5, 18, 9))
    m.cube("torso", "chest_side", (5, 32, -6), (5, 18, 9), mirror=True)
    m.cube("torso", "chest_top", (-5, 47, -6), (10, 3, 9))
    m.cube("torso", "chest_bottom", (-5, 32, -6), (10, 3, 9))
    for side, name in ((-1, "right"), (1, "left")):
        m.bone(f"plate_{name}", "torso", (side * 5, 41, -6.5))
        m.cube(f"plate_{name}", "plate", ((-5 if side < 0 else 0), 35, -7), (5, 12, 1), mirror=side > 0)
    # Its helm, the visor across it, and a crest.
    m.bone("head", "torso", (0, 50, 0))
    m.cube("head", "helm", (-5, 50, -5), (10, 10, 10))
    m.cube("head", "visor", (-5.5, 53, -6), (11, 3, 1))
    m.cube("head", "crest", (-1, 60, -4), (2, 3, 9))
    # Its arms: pauldrons of rusted iron, pillars of stone, and fists; rust shards along each forearm (scaled in by its
    # volley's clips).
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0

        def ox(v, w):
            return v if side < 0 else -v - w

        m.cube("torso", "pauldron", (ox(-17, 8), 44, -5.5), (8, 7, 11), mirror=mirror)
        m.bone(f"arm_{name}", "torso", (side * 13.5, 47, 0))
        m.cube(f"arm_{name}", "upper_arm", (ox(-17, 7), 35, -3.5), (7, 12, 7), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (side * 13.5, 35, 0))
        m.cube(f"forearm_{name}", "forearm", (ox(-18, 9), 22, -4), (9, 13, 8), mirror=mirror)
        m.bone(f"shards_{name}", f"forearm_{name}", (side * 18, 29, 0))
        for i, (dy, dz) in enumerate(((0.0, -2.5), (3.5, 0.5), (-3.5, 1.5))):
            m.cube(f"shards_{name}", "arm_shard", (side * 18.5 - 0.5, 27 + dy, dz - 0.5), (1, 3, 1),
                   rotation=(0, 0, side * (50 + 12 * i)), pivot=(side * 18, 27 + dy, dz))
        m.bone(f"fist_{name}", f"forearm_{name}", (side * 13.5, 22, 0))
        m.cube(f"fist_{name}", "fist", (ox(-18.5, 10), 13, -4.5), (10, 9, 9), mirror=mirror)
    # The core: a sphere of red light (a cube of it here) hanging in its chest, and the cold stone it is while it waits;
    # unbound, the ring of shards orbiting it.
    m.bone("core_mount", "root", (0, CORE_Y, CORE_Z))
    m.bone("core_lit", "core_mount", (0, CORE_Y, CORE_Z))
    m.cube("core_lit", "core", (-4, CORE_Y - 4, CORE_Z - 4), (8, 8, 8))
    m.cube("core_lit", "core_facet", (-3.5, CORE_Y - 3.5, CORE_Z - 3.5), (7, 7, 7), rotation=(45, 45, 0),
           pivot=(0, CORE_Y, CORE_Z))
    m.bone("core_cold", "core_mount", (0, CORE_Y, CORE_Z))
    m.cube("core_cold", "core_stone", (-4, CORE_Y - 4, CORE_Z - 4), (8, 8, 8))
    m.bone("orbit_ring", "core_mount", (0, CORE_Y, CORE_Z))
    m.bone("orbit_spin", "orbit_ring", (0, CORE_Y, CORE_Z))
    for i in range(6):
        a = math.radians(60 * i)
        x, z = round(math.sin(a) * ORBIT, 3), round(CORE_Z - math.cos(a) * ORBIT, 3)
        m.cube("orbit_spin", "orbit_shard", (x - 1, CORE_Y - 3 + (2 if i % 2 else -2), z - 1), (2, 6, 2),
               rotation=(18, 60 * i, 24), pivot=(x, CORE_Y, z))
    return m


# The core controller's bones, the plates', and the form's; the body's clips never key them.
CORE_BONES = ("core_lit", "core_cold")
PLATE_BONES = ("plate_right", "plate_left")
FORM_BONES = ("shell", "orbit_ring")


def sway(length, cycles=1, scale=1.0):
    """Its stance stirring: a slow lean, the head turning, the fists swaying a little."""
    s = scale
    return {
        "torso": {"rotation": wave(length, cycles, (0, 0, 0), (1.5 * s, 2 * s, 0), phase=0.1)},
        "head": {"rotation": wave(length, cycles, (0, 0, 0), (2 * s, 6 * s, 0), phase=0.35)},
    }


# Standing: its arms hanging a little before it, its head level.
IDLE = {"arm_right": (-8, 0, 4), "arm_left": (-8, 0, -4), "forearm_right": (-12, 0, 0), "forearm_left": (-12, 0, 0)}
# On its plinth, a statue: its head bowed, its arms straight down, its fists at its thighs.
STATUE = {"head": (14, 0, 0), "arm_right": (0, 0, 2), "arm_left": (0, 0, -2), "forearm_right": (0, 0, 0),
          "forearm_left": (0, 0, 0)}
# Waking: its head lifts and its arms come up and out.
WAKING = {"head": (-12, 0, 0), "torso": (-6, 0, 0), "arm_right": (-30, 0, 24), "arm_left": (-30, 0, -24),
          "forearm_right": (-20, 0, 0), "forearm_left": (-20, 0, 0)}
# Hammerfall: both fists raised over its head; then slammed down before it, crouching into the blow.
HAMMER_UP = {"torso": (-10, 0, 0), "head": (-10, 0, 0), "arm_right": (-170, 0, -12), "arm_left": (-170, 0, 12),
             "forearm_right": (-25, 0, 0), "forearm_left": (-25, 0, 0)}
HAMMER_DOWN = {"root": {"position": (0, -4, 0)}, "torso": (26, 0, 0), "head": (-14, 0, 0), "arm_right": (-62, 0, -8),
               "arm_left": (-62, 0, 8), "forearm_right": (-30, 0, 0), "forearm_left": (-30, 0, 0),
               "leg_right": (-16, 0, 0), "leg_left": (-16, 0, 0), "shin_right": (22, 0, 0), "shin_left": (22, 0, 0)}
# Shard Volley: its arms forward and apart, the shards bristling; then swept in, flinging them.
BRISTLE = {"scale": (2, 2.4, 2)}
STUBS = {"scale": (1, 1, 1)}
VOLLEY_READY = {"torso": (-4, 0, 0), "arm_right": (-80, 0, 26), "arm_left": (-80, 0, -26), "forearm_right": (-10, 0, 0),
                "forearm_left": (-10, 0, 0), "shards_right": BRISTLE, "shards_left": BRISTLE}
VOLLEY_FLING = {"torso": (12, 0, 0), "arm_right": (-92, 0, -8), "arm_left": (-92, 0, 8), "forearm_right": (-4, 0, 0),
                "forearm_left": (-4, 0, 0), "shards_right": STUBS, "shards_left": STUBS}
# Quake Line: it lifts its right foot high, arms out to keep its balance; then stamps, leaning in.
STAMP_UP = {"torso": (-6, 0, -4), "leg_right": (-50, 0, 0), "shin_right": (40, 0, 0), "arm_right": (-24, 0, 34),
            "arm_left": (-24, 0, -34), "forearm_right": (-10, 0, 0), "forearm_left": (-10, 0, 0)}
STAMP_DOWN = {"root": {"position": (0, -2, 0)}, "torso": (16, 0, 0), "head": (-10, 0, 0), "leg_right": (-8, 0, 0),
              "arm_right": (-36, 0, 22), "arm_left": (-36, 0, -22), "forearm_right": (-14, 0, 0), "forearm_left": (-14, 0, 0)}
# Core Lance: its arms flung wide and back, its chest thrust out as its plates open (the plates controller's); then
# braced as it fires.
LANCE_OPEN = {"torso": (-12, 0, 0), "head": (-12, 0, 0), "arm_right": (-14, 0, 64), "arm_left": (-14, 0, -64),
              "forearm_right": (-20, 0, 0), "forearm_left": (-20, 0, 0)}
LANCE_FIRE = {"root": {"position": (0, -2, 0)}, "torso": (-8, 0, 0), "head": (-6, 0, 0), "arm_right": (-20, 0, 56),
              "arm_left": (-20, 0, -56), "forearm_right": (-30, 0, 0), "forearm_left": (-30, 0, 0),
              "leg_right": (-12, 0, 6), "leg_left": (8, 0, -6)}
# Staggered (its core struck as its lance charged): reeling back, its head down and to one side, its arms loose.
REEL = {"torso": (-18, 12, 8), "head": (24, 0, 12), "arm_right": (18, 0, 24), "arm_left": (-34, 0, -10),
        "forearm_right": (-6, 0, 0), "forearm_left": (-40, 0, 0)}
# Sentry Call: its right fist raised high; then brought down.
CALL_UP = {"torso": (-6, 0, 0), "head": (-16, 0, 0), "arm_right": (-172, 0, 6), "forearm_right": (0, 0, 0),
           "arm_left": (-10, 0, -10)}
CALL_DOWN = {"torso": (8, 0, 0), "arm_right": (-60, 0, 14), "forearm_right": (-20, 0, 0), "arm_left": (-10, 0, -10)}
# The Red Storm's tell: it plants its arms, crouched, its fists on the ground before it, its head up.
PLANT = {"root": {"position": (0, -5, 0)}, "torso": (34, 0, 0), "head": (-30, 0, 0), "arm_right": (-44, 0, 26),
         "arm_left": (-44, 0, -26), "forearm_right": (-6, 0, 0), "forearm_left": (-6, 0, 0), "leg_right": (-22, 0, 4),
         "leg_left": (-22, 0, -4), "shin_right": (26, 0, 0), "shin_left": (26, 0, 0)}
# The Shell Cracks: it roars, its head thrown back and its arms flung wide, its shards bristling.
ROAR = {"torso": (-18, 0, 0), "head": (-34, 0, 0), "arm_right": (-34, 0, 78), "arm_left": (-34, 0, -78),
        "forearm_right": (-16, 0, 0), "forearm_left": (-16, 0, 0), "shards_right": BRISTLE, "shards_left": BRISTLE}
# The shell bursting: hunched, its arms clutching in at its chest.
HUNCH = {"root": {"position": (0, -3, 0)}, "torso": (24, 0, 0), "head": (20, 0, 0), "arm_right": (-50, 0, -30),
         "arm_left": (-50, 0, 30), "forearm_right": (-70, 0, 0), "forearm_left": (-70, 0, 0)}
# Death in its shell: down on its knees, slumping forward.
KNEEL = {"root": {"position": (0, -12, 0)}, "leg_right": (-80, 0, 4), "leg_left": (-80, 0, -4), "shin_right": (80, 0, 0),
         "shin_left": (80, 0, 0), "torso": (30, 0, 0), "head": (34, 0, 8), "arm_right": (10, 0, 14),
         "arm_left": (10, 0, -14)}

# The unbound core: its mount dropped UNBOUND_DROP lower over its feet (the entity floats CORE_HOVER over the floor).
UNBOUND = (0, -UNBOUND_DROP, 0)


def _spin(length, turns=1):
    """The orbiting shards going round, `turns` times a clip."""
    return {"orbit_spin": {"rotation": keys({0.0: (0, 0, 0), length / 2: (0, 180 * turns, 0), length: (0, 360 * turns, 0)})}}


def _bob(length, base=UNBOUND, height=1.0, cycles=1):
    return {"core_mount": {"position": wave(length, cycles, base, (0, height, 0))}}


def _stride(length):
    """Its stride: heavy steps, the arms swinging against the legs, the body rolling."""
    return {
        "leg_right": {"rotation": wave(length, 1, (0, 0, 0), (18, 0, 0))},
        "leg_left": {"rotation": wave(length, 1, (0, 0, 0), (18, 0, 0), phase=0.5)},
        "shin_right": {"rotation": wave(length, 1, (8, 0, 0), (8, 0, 0), phase=0.25)},
        "shin_left": {"rotation": wave(length, 1, (8, 0, 0), (8, 0, 0), phase=0.75)},
        "arm_right": {"rotation": wave(length, 1, IDLE["arm_right"], (12, 0, 0), phase=0.5)},
        "arm_left": {"rotation": wave(length, 1, IDLE["arm_left"], (12, 0, 0))},
        "torso": {"rotation": wave(length, 1, (2, 0, 0), (0, 4, 2))},
        "root": {"position": wave(length, 2, (0, 0, 0), (0, 0.6, 0))},
    }


def cydonian_sentinel_animations():
    """Its body's clips (the "body" controller), its core's (dark, lit, flaring, opened, unbound, white-hot, dimmed), its
    plates' (shut, open) and its form's (shell, unbound). The body's never key the core's, plates' or form's bones."""
    idle_waves = sway(3.0)
    unbound_core = {**_bob(2.0), **_spin(2.0)}
    return animations({
        "animation.cydonian_sentinel.statue": posed(4.0, [(0.0, STATUE), (4.0, STATUE)], loop=True),
        "animation.cydonian_sentinel.wake": posed(2.5, [(0.0, STATUE), (0.8, WAKING), (1.6, WAKING), (2.5, IDLE)]),
        "animation.cydonian_sentinel.idle": posed(3.0, [(0.0, IDLE), (3.0, IDLE)], loop=True, waves=idle_waves),
        "animation.cydonian_sentinel.walk": clip(1.6, _stride(1.6)),
        "animation.cydonian_sentinel.hammer_windup": posed(0.8, [(0.0, IDLE), (0.6, HAMMER_UP), (0.8, HAMMER_UP)]),
        "animation.cydonian_sentinel.hammer": posed(0.8, [(0.0, HAMMER_UP), (0.12, HAMMER_DOWN), (0.5, HAMMER_DOWN),
                                                          (0.8, IDLE)]),
        "animation.cydonian_sentinel.volley_windup": posed(0.7, [(0.0, {**IDLE, "shards_right": STUBS, "shards_left": STUBS}),
                                                                 (0.5, VOLLEY_READY), (0.7, VOLLEY_READY)]),
        "animation.cydonian_sentinel.volley": posed(0.65, [(0.0, VOLLEY_READY), (0.12, VOLLEY_FLING), (0.35, VOLLEY_FLING),
                                                           (0.65, {**IDLE, "shards_right": STUBS, "shards_left": STUBS})]),
        "animation.cydonian_sentinel.quake_windup": posed(1.0, [(0.0, IDLE), (0.6, STAMP_UP), (1.0, STAMP_UP)]),
        "animation.cydonian_sentinel.quake": posed(1.1, [(0.0, STAMP_UP), (0.1, STAMP_DOWN), (0.8, STAMP_DOWN), (1.1, IDLE)]),
        "animation.cydonian_sentinel.lance_windup": posed(1.2, [(0.0, IDLE), (0.6, LANCE_OPEN), (1.2, LANCE_OPEN)]),
        "animation.cydonian_sentinel.lance": posed(0.5, [(0.0, LANCE_FIRE), (0.5, LANCE_FIRE)], loop=True, waves={
            "head": {"rotation": wave(0.5, 2, LANCE_FIRE["head"], (2, 0, 2))}}),
        "animation.cydonian_sentinel.stagger": posed(1.0, [(0.0, REEL), (1.0, REEL)], loop=True, waves={
            "torso": {"rotation": wave(1.0, 1, REEL["torso"], (4, 6, 4))},
            "head": {"rotation": wave(1.0, 2, REEL["head"], (6, 8, 0), phase=0.2)}}),
        "animation.cydonian_sentinel.call_windup": posed(1.0, [(0.0, IDLE), (0.5, CALL_UP), (1.0, CALL_UP)]),
        "animation.cydonian_sentinel.call": posed(0.6, [(0.0, CALL_UP), (0.15, CALL_DOWN), (0.6, IDLE)]),
        "animation.cydonian_sentinel.plant": posed(1.0, [(0.0, IDLE), (0.5, PLANT), (1.0, PLANT)]),
        "animation.cydonian_sentinel.planted": posed(1.0, [(0.0, PLANT), (1.0, PLANT)], loop=True, waves={
            "head": {"rotation": wave(1.0, 1, PLANT["head"], (3, 10, 0))}}),
        "animation.cydonian_sentinel.roar": posed(0.5, [(0.0, ROAR), (0.5, ROAR)], loop=True, waves={
            "torso": {"rotation": wave(0.5, 2, ROAR["torso"], (3, 4, 3))},
            "head": {"rotation": wave(0.5, 3, ROAR["head"], (4, 6, 4), phase=0.25)}}),
        "animation.cydonian_sentinel.burst": posed(0.4, [(0.0, HUNCH), (0.4, HUNCH)], loop=True, waves={
            "torso": {"rotation": wave(0.4, 3, HUNCH["torso"], (5, 6, 5))},
            "root": {"position": wave(0.4, 4, HUNCH["root"]["position"], (0.6, 0.4, 0.6))}}),
        "animation.cydonian_sentinel.death": posed(1.0, [(0.0, IDLE), (0.6, KNEEL), (1.0, KNEEL)], waves={
            "core_mount": {"position": keys({0.0: (0, 0, 0), 1.0: (0, -6, 0)})}}),
        # Unbound: the core floats, its shards orbiting; turning and gathering light for its sweep; spinning faster,
        # whining, before its burst, and flung out; rising before the Starfall; sunk to the floor, dimmed, after it.
        "animation.cydonian_sentinel.core_idle": clip(2.0, unbound_core),
        "animation.cydonian_sentinel.core_move": clip(1.0, {**_bob(1.0, height=0.6), **_spin(1.0, 1)}),
        "animation.cydonian_sentinel.sweep_windup": clip(1.0, {**_bob(1.0, height=0.4, cycles=2), **_spin(1.0, 2)},
                                                         loop=False),
        "animation.cydonian_sentinel.sweep": clip(0.5, {**_bob(0.5, height=0.3), **_spin(0.5, 1)}),
        "animation.cydonian_sentinel.burst_windup": clip(0.4, {**_bob(0.4, height=0.3, cycles=2), **_spin(0.4, 1)}),
        "animation.cydonian_sentinel.shard_burst": clip(0.7, {
            "core_mount": {"position": still(UNBOUND)},
            "orbit_spin": {"rotation": keys({0.0: (0, 0, 0), 0.35: (0, 180, 0), 0.7: (0, 360, 0)}),
                           "scale": keys({0.0: (1, 1, 1), 0.15: (2.2, 1, 2.2), 0.7: (1, 1, 1)})}}, loop=False),
        "animation.cydonian_sentinel.starfall_windup": clip(2.0, {
            "core_mount": {"position": keys({0.0: UNBOUND, 1.2: (0, -UNBOUND_DROP + 14, 0), 2.0: (0, -UNBOUND_DROP + 16, 0)})},
            **_spin(2.0, 3)}, loop=False),
        "animation.cydonian_sentinel.sunk": clip(3.0, {**_bob(3.0, height=0.3), **_spin(3.0, 1)}),
        "animation.cydonian_sentinel.core_death": clip(1.0, {
            "core_mount": {"position": keys({0.0: UNBOUND, 1.0: (0, -UNBOUND_DROP - 8, 0)})},
            "orbit_spin": {"scale": keys({0.0: (1, 1, 1), 0.6: (0.6, 0.2, 0.6), 1.0: (0.3, 0.05, 0.3)}),
                           "rotation": keys({0.0: (0, 0, 0), 1.0: (0, 60, 0)})}}, loop=False),
        # Its core: cold stone while it waits; burning red; flaring; bright as its plates open; swollen and unbound;
        # white-hot in the Red Giant; dimmed after a Starfall.
        "animation.cydonian_sentinel.core_dark": clip(1.0, {"core_lit": {"scale": still((0, 0, 0))},
                                                            "core_cold": {"scale": still((1, 1, 1))}}),
        "animation.cydonian_sentinel.core_lit": clip(1.0, {"core_lit": {"scale": wave(1.0, 1, (1, 1, 1), (0.05, 0.05, 0.05))},
                                                           "core_cold": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.core_flare": clip(0.4, {"core_lit": {"scale": wave(0.4, 2, (1.2, 1.2, 1.2), (0.1, 0.1, 0.1))},
                                                             "core_cold": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.core_open": clip(0.3, {"core_lit": {"scale": wave(0.3, 1, (1.12, 1.12, 1.12), (0.08, 0.08, 0.08))},
                                                            "core_cold": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.core_unbound": clip(1.0, {"core_lit": {"scale": wave(1.0, 1, (2, 2, 2), (0.08, 0.08, 0.08))},
                                                               "core_cold": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.core_giant": clip(0.4, {"core_lit": {"scale": wave(0.4, 2, (2.3, 2.3, 2.3), (0.14, 0.14, 0.14))},
                                                             "core_cold": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.core_dim": clip(2.0, {"core_lit": {"scale": wave(2.0, 1, (1.6, 1.6, 1.6), (0.04, 0.04, 0.04))},
                                                           "core_cold": {"scale": still((1.7, 1.7, 1.7))}}),
        # Its chest plates: shut, or swung open on their hinges.
        "animation.cydonian_sentinel.plates_shut": clip(1.0, {"plate_right": {"rotation": still((0, 0, 0))},
                                                              "plate_left": {"rotation": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.plates_open": clip(1.0, {"plate_right": {"rotation": still(PLATE_OPEN)},
                                                              "plate_left": {"rotation": still((0, -PLATE_OPEN[1], 0))}}),
        # Its form: the shell whole and no orbit; or the shell gone and the shards orbiting.
        "animation.cydonian_sentinel.form_shell": clip(1.0, {"shell": {"scale": still((1, 1, 1))},
                                                             "orbit_ring": {"scale": still((0, 0, 0))}}),
        "animation.cydonian_sentinel.form_core": clip(1.0, {"shell": {"scale": still((0, 0, 0))},
                                                            "orbit_ring": {"scale": still((1, 1, 1))}}),
    })


# How far its plates swing open on their hinges (degrees about y, the right plate's; the left's is the mirror).
PLATE_OPEN = (0, -105, 0)


# ------------------------------------------------------------------------------------------------- a Shard Sentry

def shard_sentry_model():
    """A Shard Sentry: three fist-sized stones of red rock circling a red spark."""
    m = Model("shard_sentry", 64, (1.2, 1.2))
    m.bone("root")
    m.bone("sentry", "root", (0, 8, 0))
    m.bone("spark", "sentry", (0, 8, 0))
    m.cube("spark", "spark", (-1.5, 6.5, -1.5), (3, 3, 3))
    m.bone("ring", "sentry", (0, 8, 0))
    for i in range(3):
        a = math.radians(120 * i)
        x, z = round(math.sin(a) * 6, 3), round(-math.cos(a) * 6, 3)
        m.cube("ring", "sentry_stone", (x - 2, 6, z - 2), (4, 4, 4), rotation=(12, 120 * i, 18), pivot=(x, 8, z))
    return m


def shard_sentry_animations():
    hover = {"ring": {"rotation": keys({0.0: (0, 0, 0), 1.0: (0, 180, 0), 2.0: (0, 360, 0)})},
             "sentry": {"position": wave(2.0, 1, (0, 0, 0), (0, 1.0, 0))},
             "spark": {"scale": wave(2.0, 4, (1, 1, 1), (0.15, 0.15, 0.15))}}
    rise = {"sentry": {"position": keys({0.0: (0, -20, 0), 0.7: (0, -4, 0), 1.0: (0, 0, 0)})},
            "ring": {"rotation": keys({0.0: (0, 0, 0), 1.0: (0, 540, 0)})}}
    return animations({
        "animation.shard_sentry.rise": clip(1.0, rise, loop=False),
        "animation.shard_sentry.hover": clip(2.0, hover),
    })


# ------------------------------------------------------------------------------------------------- a Rust Shard

def rust_shard_model():
    """A Rust Shard: a jagged splinter of rusted crystal, a red heart burning down its length."""
    m = Model("rust_shard", 32, (0.6, 0.6))
    m.bone("root")
    m.bone("shard", "root", (0, 3, 0))
    m.cube("shard", "splinter", (-1, 2, -3), (2, 2, 6))
    m.cube("shard", "shard_heart", (-0.5, 2.5, -4.5), (1, 1, 8))
    m.cube("shard", "shard_barb", (-1.5, 1.5, 1.5), (3, 3, 2))
    return m


def rust_shard_animations():
    return animations({"animation.rust_shard.fly": clip(0.5, {
        "shard": {"rotation": keys({0.0: (0, 0, 0), 0.25: (0, 0, 180), 0.5: (0, 0, 360)})}})})


# ------------------------------------------------------------------------------------------------- a Quake Spike

def quake_spike_model():
    """A spike of red rock bursting up out of the floor: a tall one in the middle, three shorter leaning out round it,
    on a broken footing."""
    m = Model("quake_spike", 64, (2.0, 1.5))
    m.bone("root")
    m.bone("spike", "root", (0, 0, 0))
    m.cube("spike", "rubble", (-6, -1, -6), (12, 2, 12))
    m.cube("spike", "spike_tall", (-2, 0, -2), (4, 20, 4), rotation=(4, 0, 6), pivot=(0, 0, 0))
    for i, (angle, lean) in enumerate(((40, 30), (160, 26), (280, 34))):
        a = math.radians(angle)
        x, z = round(math.sin(a) * 4, 2), round(-math.cos(a) * 4, 2)
        m.cube("spike", "spike_short", (x - 1.5, 0, z - 1.5), (3, 12, 3), rotation=(0, angle, lean), pivot=(x, 0, z))
    return m


def quake_spike_animations():
    """Bursting: it shoots up out of the floor and settles; standing: it trembles; sinking: it crumbles back down."""
    return animations({
        "animation.quake_spike.burst": clip(0.3, {"spike": {"scale": keys({0.0: (1, 0.01, 1), 0.12: (1, 1.3, 1),
                                                                         0.2: (1, 1, 1), 0.3: (1, 1, 1)})}}, loop=False),
        "animation.quake_spike.stand": clip(0.6, {"spike": {"scale": wave(0.6, 2, (1, 1, 1), (0.03, 0.05, 0.03)),
                                                            "rotation": wave(0.6, 1, (0, 0, 0), (2, 0, 2))}}),
        "animation.quake_spike.sink": clip(0.5, {"spike": {"scale": keys({0.0: (1, 1, 1), 0.5: (1, 0.01, 1)})}}, loop=False),
    })


# ------------------------------------------------------------------------------------------------- a Meteor

def sentinel_meteor_model():
    """A meteor of the Starfall: a lump of red rock, its heart burning, fire streaming up behind it as it falls."""
    m = Model("sentinel_meteor", 64, (1.2, 2.0))
    m.bone("root")
    m.bone("rock", "root", (0, 5, 0))
    m.cube("rock", "meteor_rock", (-4, 1, -4), (8, 8, 8))
    m.bone("trail", "root", (0, 9, 0))
    m.cube("trail", "meteor_fire", (-3, 8, -3), (6, 14, 6))
    return m


def sentinel_meteor_animations():
    return animations({"animation.sentinel_meteor.fall": clip(0.6, {
        "rock": {"rotation": keys({0.0: (0, 0, 0), 0.3: (180, 90, 0), 0.6: (360, 180, 0)})},
        "trail": {"scale": wave(0.6, 3, (1, 1, 1), (0.1, 0.2, 0.1))}})})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"cydonian_sentinel": cydonian_sentinel_model, "shard_sentry": shard_sentry_model, "rust_shard": rust_shard_model,
          "quake_spike": quake_spike_model, "sentinel_meteor": sentinel_meteor_model}
ANIMATIONS = {"cydonian_sentinel": cydonian_sentinel_animations, "shard_sentry": shard_sentry_animations,
              "rust_shard": rust_shard_animations, "quake_spike": quake_spike_animations,
              "sentinel_meteor": sentinel_meteor_animations}
# The clips the Java names (CydonianSentinelEntity, ShardSentryEntity, RustShardEntity, QuakeSpikeEntity and
# SentinelMeteorEntity).
CLIPS = {
    "cydonian_sentinel": ("statue", "wake", "idle", "walk", "hammer_windup", "hammer", "volley_windup", "volley",
                          "quake_windup", "quake", "lance_windup", "lance", "stagger", "call_windup", "call", "plant",
                          "planted", "roar", "burst", "death", "core_idle", "core_move", "sweep_windup", "sweep",
                          "burst_windup", "shard_burst", "starfall_windup", "sunk", "core_death", "core_dark", "core_lit",
                          "core_flare", "core_open", "core_unbound", "core_giant", "core_dim", "plates_shut", "plates_open",
                          "form_shell", "form_core"),
    "shard_sentry": ("rise", "hover"),
    "rust_shard": ("fly",),
    "quake_spike": ("burst", "stand", "sink"),
    "sentinel_meteor": ("fall",),
}
# The bones each of the Sentinel's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"core": CORE_BONES, "plates": PLATE_BONES, "form": FORM_BONES}


def controller_of(clip_name):
    """Which of its controllers plays a clip of the Sentinel's."""
    short = clip_name.split(".")[-1]
    if short.startswith("core_") and short not in ("core_idle", "core_move", "core_death"):
        return "core"
    if short.startswith("plates_"):
        return "plates"
    if short.startswith("form_"):
        return "form"
    return "body"


def _check():
    """Every model packs; no clip of one of the Sentinel's controllers keys another's bone."""
    for build in MODELS.values():
        build().pack()
    owned = {bone: controller for controller, bones in CONTROLLERS.items() for bone in bones}
    for clip_name, body in cydonian_sentinel_animations()["animations"].items():
        for bone in body["bones"]:
            if owned.get(bone, "body") != controller_of(clip_name):
                raise ValueError(f"{clip_name} keys {bone}")
    return True


if __name__ == "__main__":
    print(_check())
