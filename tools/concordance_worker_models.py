"""GeckoLib bodies for roadmap step 17, spirits, familiars and constructs: the Hearthling (a familiar), the Gathering
Shade (a spirit) and the Clockwork Porter (a construct). Geometry and animations only, as plain dicts; no I/O.
tools/concordance_worker_art.py paints the 64x64 sheets on the same box-UV regions.

Units are pixels (16 = one block); y = 0 is the entity's feet and every model faces north (-z). Left limbs and wings
sit on +x, as in vanilla's Bedrock and Java models. Rotations follow Bedrock's (and Blockbench's) sense, which GeckoLib
keeps: +x pitches a bone's top forward (a head looks down, a hanging arm or leg swings back), -x raises a hanging arm
forward, and +z rolls a bone towards the model's left. A left wing or arm turns outwards and upwards with -z, a right
one with +z; a left wing folds back with -y, a right one with +y.

Each model is 5 to 9 boxes. Every box has its own box-UV region in the sheet, so no two regions overlap: a box of size
(w, h, d) at uv (u, v) uses (2(w + d)) x (d + h) from (u, v), laid out as Bedrock and Blockbench lay it: the top at
(u + d, v), the bottom at (u + d + w, v), then the right side, front (north), left side and back from (u, v + d).

GEO, ANIMATIONS and SIZES are keyed by entity id. SIZES[id] is (UV, SIZES): each box's UV origin and its (w, h, d).
Every clip loops. The Java plays the clips by name (the HearthlingEntity and the other WorkerEntity subclasses).
"""

import math

TEXTURE = 64  # every sheet is 64x64


# ------------------------------------------------------------------------------------------------- helpers

def _cube(uv, sizes, name, origin):
    u, v = uv[name]
    return {"origin": list(origin), "size": list(sizes[name]), "uv": [u, v]}


def _geo(identifier, bones, width, height):
    """A .geo.json body; the visible bounds (in blocks) are centred on the model, which stands on y = 0."""
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": TEXTURE, "texture_height": TEXTURE,
                        "visible_bounds_width": width, "visible_bounds_height": height,
                        "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def _key(seconds):
    return str(round(seconds, 4))


def _vec(values):
    return [round(value, 3) + 0.0 for value in values]  # + 0.0 turns -0.0 into 0.0


def _still(values):
    """A pose held for the whole clip."""
    return {"0.0": _vec(values)}


def _wave(length, cycles, base, amplitude, phase=0.0, steps=8):
    """Keyframes of base + amplitude * sin(2 pi (cycles t / length + phase)), sampled `steps` times a cycle, so the
    linear keyframes GeckoLib reads ease in and out. The first and last keys match, so the loop is seamless."""
    keys = {}
    total = steps * cycles
    for i in range(total + 1):
        wave = math.sin(2 * math.pi * (i / steps + phase))
        keys[_key(length * i / total)] = _vec([b + a * wave for b, a in zip(base, amplitude)])
    return keys


def _clip(length, bones):
    return {"loop": True, "animation_length": length, "bones": bones}


def _animations(clips):
    return {"format_version": "1.8.0", "animations": clips}


# ------------------------------------------------------------------------------------------------- the Hearthling

# A hearth-sprite about half a block tall: a glowing lantern core (with a face) under a little cap and knob, an ember
# drip below it, and two small amber wings at its back. It hovers a pixel off the ground.
HEARTHLING_UV = {"core": (0, 0), "lid": (24, 0), "knob": (40, 0), "tail": (48, 0), "wing_left": (0, 10),
                 "wing_right": (8, 10)}
HEARTHLING_SIZES = {"core": (6, 4, 6), "lid": (4, 1, 4), "knob": (2, 1, 2), "tail": (2, 2, 2), "wing_left": (3, 4, 1),
                    "wing_right": (3, 4, 1)}


def hearthling_geo():
    """root > core (the glowing core and its ember drip) > cap (lid and knob); the wings hang on the root, so a pulse
    of the core does not stretch them."""
    uv, sizes = HEARTHLING_UV, HEARTHLING_SIZES
    return _geo("hearthling", [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "core", "parent": "root", "pivot": [0, 5, 0], "cubes": [
            _cube(uv, sizes, "core", [-3, 3, -3]), _cube(uv, sizes, "tail", [-1, 1, -1])]},
        {"name": "cap", "parent": "core", "pivot": [0, 7, 0], "cubes": [
            _cube(uv, sizes, "lid", [-2, 7, -2]), _cube(uv, sizes, "knob", [-1, 8, -1])]},
        {"name": "wing_left", "parent": "root", "pivot": [3, 6, 1.5], "cubes": [
            _cube(uv, sizes, "wing_left", [3, 4, 1])]},
        {"name": "wing_right", "parent": "root", "pivot": [-3, 6, 1.5], "cubes": [
            _cube(uv, sizes, "wing_right", [-6, 4, 1])]},
    ], width=1, height=1)


def hearthling_animations():
    """Idle: a gentle bob and a slow wing beat. Following: tilted forward, wings beating fast. Supporting: the core
    swells and eases back. Waiting: wings folded back, a slow bob."""
    def wings(length, cycles, spread, beat, lift=12):
        return {"wing_left": {"rotation": _wave(length, cycles, (0, -spread, -lift), (0, -beat, 0))},
                "wing_right": {"rotation": _wave(length, cycles, (0, spread, lift), (0, beat, 0))}}

    def bob(length, cycles, height):
        return {"position": _wave(length, cycles, (0, height / 2, 0), (0, height / 2, 0), phase=-0.25)}

    return _animations({
        "animation.hearthling.idle": _clip(3.0, {"root": bob(3.0, 1, 1.0), **wings(3.0, 3, 20, 15)}),
        "animation.hearthling.following": _clip(1.0, {
            "root": dict(bob(1.0, 2, 0.5), rotation=_still((12, 0, 0))), **wings(1.0, 4, 25, 25, lift=8)}),
        "animation.hearthling.supporting": _clip(1.2, {
            "root": bob(1.2, 1, 0.5),
            "core": {"scale": _wave(1.2, 1, (1.1, 1.1, 1.1), (0.1, 0.1, 0.1), phase=-0.25)},
            **wings(1.2, 2, 8, 8, lift=18)}),
        "animation.hearthling.waiting": _clip(4.0, {
            "root": bob(4.0, 1, 0.5),
            "wing_left": {"rotation": _still((0, -70, -4))}, "wing_right": {"rotation": _still((0, 70, 4))}}),
    })


# ------------------------------------------------------------------------------------------------- the Gathering Shade

# A hooded wisp about 1.6 blocks tall with no legs: a hood with a drooping point, a robed body that tapers into a
# trailing tail, and two long thin arms that hang past its waist.
SHADE_UV = {"body": (0, 0), "hood": (20, 0), "hood_tip": (44, 0), "tail_upper": (0, 12), "tail_tip": (14, 12),
            "arm_left": (22, 12), "arm_right": (30, 12)}
SHADE_SIZES = {"body": (6, 8, 4), "hood": (6, 6, 6), "hood_tip": (2, 2, 2), "tail_upper": (4, 5, 3),
               "tail_tip": (2, 4, 2), "arm_left": (2, 11, 2), "arm_right": (2, 11, 2)}


def shade_geo():
    """root > body > hood, both arms and the tail. The body turns about its middle, so a lean carries the hood forward
    and the tail back."""
    uv, sizes = SHADE_UV, SHADE_SIZES
    return _geo("gathering_shade", [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "body", "parent": "root", "pivot": [0, 12, 0], "cubes": [_cube(uv, sizes, "body", [-3, 11, -2])]},
        {"name": "hood", "parent": "body", "pivot": [0, 19, 0], "cubes": [
            _cube(uv, sizes, "hood", [-3, 18, -3]), _cube(uv, sizes, "hood_tip", [-1, 23, 3])]},
        {"name": "arm_left", "parent": "body", "pivot": [4, 18.5, 0], "cubes": [
            _cube(uv, sizes, "arm_left", [3, 8, -1])]},
        {"name": "arm_right", "parent": "body", "pivot": [-4, 18.5, 0], "cubes": [
            _cube(uv, sizes, "arm_right", [-5, 8, -1])]},
        {"name": "tail", "parent": "body", "pivot": [0, 11, 0.5], "cubes": [
            _cube(uv, sizes, "tail_upper", [-2, 6, -1]), _cube(uv, sizes, "tail_tip", [-1, 2, 0])]},
    ], width=1.5, height=2)


def shade_animations():
    """Idle: it floats, arms and tail drifting. Travelling: it leans into its path, arms trailing, the tail swaying.
    Working: both arms reach forward and draw back. Suspended: arms hanging, the slowest sway. Waiting (it lacks
    something: resources, access, a way, room): arms folded across its body, the hood turning from side to side as if
    looking for what it needs, the tail flicking (roadmap step 27)."""
    def float_(length, cycles, height):
        return {"position": _wave(length, cycles, (0, height / 2, 0), (0, height / 2, 0), phase=-0.25)}

    reach = {"0.0": [-15, 0, 0], "0.5": [-80, 0, 0], "0.8": [-80, 0, 0], "1.3": [-25, 0, 0], "1.6": [-15, 0, 0]}
    return _animations({
        "animation.gathering_shade.idle": _clip(4.0, {
            "root": float_(4.0, 1, 1.0),
            "arm_left": {"rotation": _wave(4.0, 1, (-4, 0, -4), (4, 0, 2), phase=0.25)},
            "arm_right": {"rotation": _wave(4.0, 1, (-4, 0, 4), (4, 0, -2), phase=0.25)},
            "tail": {"rotation": _wave(4.0, 1, (6, 0, 0), (0, 0, 5))}}),
        "animation.gathering_shade.travelling": _clip(2.0, {
            "root": float_(2.0, 2, 0.6),
            "body": {"rotation": _wave(2.0, 2, (16, 0, 0), (2, 0, 0))},
            "hood": {"rotation": _still((-8, 0, 0))},
            "arm_left": {"rotation": _wave(2.0, 1, (22, 0, -6), (4, 0, 0))},
            "arm_right": {"rotation": _wave(2.0, 1, (22, 0, 6), (-4, 0, 0))},
            "tail": {"rotation": _wave(2.0, 1, (14, 0, 0), (0, 0, 12))}}),
        "animation.gathering_shade.working": _clip(1.6, {
            "body": {"rotation": {"0.0": [4, 0, 0], "0.5": [10, 0, 0], "1.3": [4, 0, 0], "1.6": [4, 0, 0]}},
            "hood": {"rotation": {"0.0": [0, 0, 0], "0.5": [8, 0, 0], "1.3": [0, 0, 0], "1.6": [0, 0, 0]}},
            "arm_left": {"rotation": {key: [x, 0, 6 if x < -50 else 0] for key, (x, _, _) in reach.items()}},
            "arm_right": {"rotation": {key: [x, 0, -6 if x < -50 else 0] for key, (x, _, _) in reach.items()}},
            "tail": {"rotation": _wave(1.6, 1, (8, 0, 0), (0, 0, 4))}}),
        "animation.gathering_shade.suspended": _clip(8.0, {
            "body": {"rotation": _wave(8.0, 1, (0, 0, 0), (0, 0, 3))},
            "hood": {"rotation": _still((10, 0, 0))},
            "arm_left": {"rotation": _still((2, 0, 0))},
            "arm_right": {"rotation": _still((2, 0, 0))},
            "tail": {"rotation": _wave(8.0, 1, (4, 0, 0), (0, 0, -3))}}),
        "animation.gathering_shade.waiting": _clip(3.0, {
            "root": float_(3.0, 1, 0.6),
            "hood": {"rotation": {"0.0": [0, 0, 0], "0.6": [0, 35, 0], "1.2": [0, 35, 0], "1.8": [0, -35, 0],
                                  "2.4": [0, -35, 0], "3.0": [0, 0, 0]}},
            "arm_left": {"rotation": _still((-55, 0, 40))},
            "arm_right": {"rotation": _still((-55, 0, -40))},
            "tail": {"rotation": _wave(3.0, 3, (6, 0, 0), (0, 0, 10))}}),
    })


# ------------------------------------------------------------------------------------------------- the Clockwork Porter

# A squat brass walker about one block tall: a round body (a drum crossed with a taller, narrower block, so its outline
# is rounded from the front and the side), a small head with a lens, a wicker basket on its back and two stubby legs.
PORTER_UV = {"body_drum": (0, 0), "body_round": (32, 0), "lens": (56, 0), "head": (0, 14), "basket": (16, 14),
             "leg_left": (36, 14), "leg_right": (50, 14)}
PORTER_SIZES = {"body_drum": (8, 6, 8), "body_round": (6, 8, 6), "lens": (2, 2, 1), "head": (4, 4, 4),
                "basket": (6, 5, 4), "leg_left": (3, 5, 4), "leg_right": (3, 5, 4)}


def porter_geo():
    """root > body (drum) > head (with its lens) and basket; the legs hang on the root, so the body can bob and slump
    while the feet stay on the ground. A leg's top stays inside the body as it swings."""
    uv, sizes = PORTER_UV, PORTER_SIZES
    return _geo("clockwork_porter", [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "body", "parent": "root", "pivot": [0, 4, 0], "cubes": [
            _cube(uv, sizes, "body_drum", [-4, 5, -4]), _cube(uv, sizes, "body_round", [-3, 4, -3])]},
        {"name": "head", "parent": "body", "pivot": [0, 12, -1], "cubes": [
            _cube(uv, sizes, "head", [-2, 12, -3]), _cube(uv, sizes, "lens", [-1, 13, -4])]},
        {"name": "basket", "parent": "body", "pivot": [0, 9, 4], "cubes": [_cube(uv, sizes, "basket", [-3, 7, 4])]},
        {"name": "leg_left", "parent": "root", "pivot": [2.5, 4.5, 0], "cubes": [
            _cube(uv, sizes, "leg_left", [1, 0, -2])]},
        {"name": "leg_right", "parent": "root", "pivot": [-2.5, 4.5, 0], "cubes": [
            _cube(uv, sizes, "leg_right", [-4, 0, -2])]},
    ], width=1.5, height=1.5)


def porter_animations():
    """Idle: its head turns a little to either side. Walking: the legs swing in turn and the body bobs and waddles.
    Working: it leans round to its basket and looks into it. Broken: slumped, the body pitched and tilted, the head
    hanging; it does not move. Waiting (it lacks something: goods, access, a way, room, a loaded destination): it taps
    one foot and its head tips up and round, looking for what it needs (roadmap step 27)."""
    lean = {"0.0": [0, 0, 0], "0.5": [0, 25, 8], "1.5": [0, 25, 8], "2.0": [0, 0, 0]}
    look = {"0.0": [0, 0, 0], "0.5": [12, 65, 0], "1.5": [12, 65, 0], "2.0": [0, 0, 0]}
    tip = {"0.0": [0, 0, 0], "0.5": [-12, 0, -6], "1.5": [-12, 0, -6], "2.0": [0, 0, 0]}
    return _animations({
        "animation.clockwork_porter.idle": _clip(4.0, {"head": {"rotation": _wave(4.0, 1, (0, 0, 0), (0, 20, 0))}}),
        "animation.clockwork_porter.walking": _clip(0.8, {
            "leg_left": {"rotation": _wave(0.8, 1, (0, 0, 0), (-25, 0, 0))},
            "leg_right": {"rotation": _wave(0.8, 1, (0, 0, 0), (25, 0, 0))},
            "body": {"position": _wave(0.8, 2, (0, 0.5, 0), (0, 0.5, 0), phase=-0.25),
                     "rotation": _wave(0.8, 1, (0, 0, 0), (0, 0, 3))},
            "head": {"rotation": _wave(0.8, 1, (0, 0, 0), (0, 0, -2))}}),
        "animation.clockwork_porter.working": _clip(2.0, {
            "body": {"rotation": lean}, "head": {"rotation": look}, "basket": {"rotation": tip}}),
        "animation.clockwork_porter.broken": _clip(1.0, {
            "body": {"rotation": _still((20, 0, 12)), "position": _still((0, -1, 0))},
            "head": {"rotation": _still((40, 0, 15))},
            "basket": {"rotation": _still((-10, 0, 0))},
            "leg_left": {"rotation": _still((0, 0, -6))},
            "leg_right": {"rotation": _still((0, 0, 8))}}),
        "animation.clockwork_porter.waiting": _clip(1.6, {
            "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [-14, 0, 0], "0.4": [0, 0, 0], "0.6": [-14, 0, 0],
                                       "0.8": [0, 0, 0], "1.6": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [-10, 0, 0], "0.8": [-10, 30, 0], "1.2": [-10, -30, 0], "1.6": [-10, 0, 0]}},
            "body": {"rotation": _still((-4, 0, 0))}}),
    })


# ------------------------------------------------------------------------------------------------- the tables

GEO = {"hearthling": hearthling_geo, "gathering_shade": shade_geo, "clockwork_porter": porter_geo}
ANIMATIONS = {"hearthling": hearthling_animations, "gathering_shade": shade_animations,
              "clockwork_porter": porter_animations}
SIZES = {"hearthling": (HEARTHLING_UV, HEARTHLING_SIZES), "gathering_shade": (SHADE_UV, SHADE_SIZES),
         "clockwork_porter": (PORTER_UV, PORTER_SIZES)}
# The clips the Java plays, by entity (each must exist in that entity's ANIMATIONS).
CLIPS = {"hearthling": ("idle", "following", "supporting", "waiting"),
         "gathering_shade": ("idle", "travelling", "working", "suspended", "waiting"),
         "clockwork_porter": ("idle", "walking", "working", "broken", "waiting")}
