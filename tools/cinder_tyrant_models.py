"""GeckoLib bodies for the Cinder Tyrant and his fight (docs/features/cinder-tyrant.md): the Tyrant himself, his
Cinderlings, the gobs of magma he spits and the cinders his Cinder Rain shakes from the vent. Geometry and animations
only, as plain dicts; no I/O. tools/cinder_tyrant_art.py paints their sheets on the same box-UV regions, at TEXTURE_SCALE
times the size the models declare (docs/ART_DIRECTION.md, "High resolution"). The builder and the posing helpers are
Vesperine's (tools/vesperine_models.py), which also sets out the conventions: pixels (16 = one block), y = 0 at the feet,
every model facing north (-z) with its left on +x; +x pitches a bone's top forward (and so opens a jaw hinged behind
it), and +y turns the front to the model's right. A keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region, and `Model.pack` lays the regions out on the
sheet without overlaps. MODELS, ANIMATIONS and CLIPS are keyed by entity id; CLIPS lists the clips the Java plays.
"""
from vesperine_models import Model, TEXTURE_SCALE, animations, clip, keys, posed, still, wave

__all__ = ["TEXTURE_SCALE", "MODELS", "ANIMATIONS", "ENTITY_MODEL", "CLIPS", "CONTROLLERS", "MOUTH_Z", "MOUTH_Y",
           "CREST_TOP", "BACK_TOP"]

# ------------------------------------------------------------------------------------------------- the Tyrant

# A salamander lord the size of a cart: a long low body on four sprawled legs, armoured along his back in plates of
# obsidian with magma glowing in the seams between them, a crest of obsidian spikes down his spine, a broad flat head
# with a wide jaw and small burning eyes, and a tail ending in a club of basalt. Seven and a half blocks from his jaws to
# the end of his club. His seams glow while he is hot (the "heat" controller's bones): quenched, they go dark.
MOUTH_Z = -44.0           # where his jaws meet, ahead of his feet (pixels): MagmaGobEntity spits from here
MOUTH_Y = 12.0            # and how high, at rest
CREST_TOP = 49.0          # the tallest spike's tip: sunk in the crucible, the crest shows over the slag
BACK_TOP = 30.0           # the top of his back's plates and seams: sunk, they are under the slag
LEGS = (("fl", 1, -12.0), ("fr", -1, -12.0), ("bl", 1, 13.0), ("br", -1, 13.0))   # name, side (+1 left), z of the joint
TAIL_SPIKES = ((1, 24.0, 8), (1, 31.0, 7), (2, 40.0, 6), (2, 47.0, 5))            # tail bone, z, height


def cinder_tyrant_model():
    m = Model("cinder_tyrant", 256, (8.0, 4.0))
    m.bone("root")
    m.bone("body", "root", (0, 14, 12))
    # His torso, a hide of dark basalt scales; on his back four plates of obsidian with seams between them, and a plate
    # on each flank between his legs.
    m.cube("body", "torso", (-15, 6, -18), (30, 20, 40))
    for z in (-17, -8, 1, 10):
        m.cube("body", "plate", (-13, 26, z), (26, 3, 8))
    m.cube("body", "flank", (15, 13, -7), (2, 10, 14))
    m.cube("body", "flank", (-17, 13, -7), (2, 10, 14), mirror=True)
    # The crest down his spine: spikes of obsidian, the tallest behind his head, leaning back.
    m.bone("crest", "body", (0, 29, 2))
    for z, part, height in ((-15.0, "spike_tall", 20), (-6.0, "spike_tall", 20), (3.0, "spike", 16), (12.0, "spike_low", 12)):
        w = 4 if height > 12 else 3
        m.cube("crest", part, (-w / 2, 29, z), (w, height, w), rotation=(-14, 0, 0), pivot=(0, 29, z + w / 2))
    # His seams (the heat controller's): magma between the plates and along the spine, and a line along each flank under
    # its plate. They burn while he is hot.
    m.bone("seams", "body", (0, 16, 2))
    for z in (-9, 0, 9):
        m.cube("seams", "seam", (-12, 27, z), (24, 2, 1))
    m.cube("seams", "spine_seam", (-1, 29, -17), (2, 1, 35))
    m.cube("seams", "flank_seam", (15, 12, -6), (1, 1, 12))
    m.cube("seams", "flank_seam", (-16, 12, -6), (1, 1, 12), mirror=True)
    # The Molten Heart (the heat controller's): white-hot cores swelling out of his seams, shown only then.
    m.bone("heart", "body", (0, 16, 2))
    for z in (-9.5, -0.5, 8.5):
        m.cube("heart", "core", (-10, 27, z), (20, 3, 2))
    m.cube("heart", "spine_core", (-1.5, 29, -17.5), (3, 2, 36))
    # His neck and head: broad and flat, small eyes standing on its top, two horns of the crest behind them; his jaw on
    # its own bone, and under it his throat, which glows as he gathers fire (shown only then).
    m.bone("neck", "body", (0, 16, -18))
    m.cube("neck", "neck", (-10, 8, -26), (20, 15, 8))
    m.bone("head", "neck", (0, 16, -26))
    m.cube("head", "skull", (-12, 12, -44), (24, 10, 18))
    m.cube("head", "brow", (-12.5, 20, -40), (25, 3, 8))
    m.cube("head", "eye", (8, 21, -38), (4, 3, 4))
    m.cube("head", "eye", (-12, 21, -38), (4, 3, 4), mirror=True)
    m.cube("head", "horn", (3, 21, -31), (3, 8, 3), rotation=(-30, 0, -12), pivot=(4.5, 21, -29.5))
    m.cube("head", "horn", (-6, 21, -31), (3, 8, 3), rotation=(-30, 0, 12), pivot=(-4.5, 21, -29.5), mirror=True)
    m.cube("head", "fang", (7, 10, -44), (2, 2, 1))
    m.cube("head", "fang", (-9, 10, -44), (2, 2, 1), mirror=True)
    m.bone("jaw", "head", (0, 12, -28))
    m.cube("jaw", "jaw", (-11, 8, -43), (22, 4, 17))
    m.bone("throat", "jaw", (0, 8, -33))
    m.cube("throat", "throat", (-7, 7, -40), (14, 1, 12))
    # Four sprawled legs: the upper leg out sideways from his flank, the lower leg down to a broad clawed foot.
    for name, side, z in LEGS:
        mirror = side < 0
        back = name[0] == "b"

        def ox(v, w):  # a left-side origin x for a cube `w` wide, mirrored to the right
            return v if side > 0 else -v - w

        m.bone(f"leg_{name}", "body", (side * 15, 14, z))
        upper = "thigh" if back else "upper_leg"
        size = (12, 9, 10) if back else (12, 8, 8)
        m.cube(f"leg_{name}", upper, (ox(15, size[0]), 10, z - size[2] / 2), size, mirror=mirror)
        m.bone(f"foot_{name}", f"leg_{name}", (side * 23, 14, z))
        lower = (8, 14, 8) if back else (7, 13, 7)
        m.cube(f"foot_{name}", "shank" if back else "shin", (ox(19, lower[0]), 2, z - lower[2] / 2), lower, mirror=mirror)
        foot = (12, 3, 13) if back else (11, 3, 12)
        m.cube(f"foot_{name}", "hind_foot" if back else "fore_foot", (ox(17, foot[0]), 0, z - foot[2] / 2 - 3), foot,
               mirror=mirror)
    # His tail, in three bones: its base plated and seamed, its middle, and its end, a club of basalt.
    m.bone("tail_1", "body", (0, 16, 22))
    m.cube("tail_1", "tail_base", (-10, 8, 22), (20, 14, 16))
    m.cube("tail_1", "tail_plate", (-9, 22, 23), (18, 2, 7))
    m.cube("tail_1", "tail_plate", (-9, 22, 31), (18, 2, 7))
    m.bone("tail_2", "tail_1", (0, 15, 38))
    m.cube("tail_2", "tail_mid", (-7, 9, 38), (14, 11, 16))
    m.cube("tail_2", "tail_plate_mid", (-6, 20, 39), (12, 2, 14))
    m.bone("tail_3", "tail_2", (0, 14, 54))
    m.cube("tail_3", "tail_end", (-5, 10, 54), (10, 8, 10))
    m.cube("tail_3", "club", (-8, 7, 62), (16, 14, 12))
    m.cube("tail_3", "club_knob", (-10, 10, 64), (20, 8, 8))
    m.cube("tail_3", "club_knob_top", (-4, 21, 64), (8, 3, 8))
    for bone, z, height in TAIL_SPIKES:
        part = "tail_spike" if height >= 7 else "tail_spike_low"
        size = (3, 8, 3) if height >= 7 else (2, 6, 2)
        top = 24 if bone == 1 else 22
        m.cube(f"tail_{bone}", part, (-size[0] / 2, top, z), size, rotation=(-20, 0, 0), pivot=(0, top, z + size[2] / 2))
    # The tail's seams (the heat controller's), between its plates.
    m.bone("tail_seams", "tail_1", (0, 16, 22))
    m.cube("tail_seams", "tail_seam", (-8, 22.5, 30), (16, 1, 1))
    m.cube("tail_seams", "tail_seam_side", (10, 13, 24), (1, 1, 13))
    m.cube("tail_seams", "tail_seam_side", (-11, 13, 24), (1, 1, 13), mirror=True)
    return m


# The heat controller's bones; the body's clips never key them.
HEAT_BONES = ("seams", "heart", "tail_seams")
SIDES = ("left", "right")
HIDDEN = {"throat": {"scale": (0, 0, 0)}}
GLOWING_THROAT = {"throat": {"scale": (1, 1, 1)}}


def _legs(splay=0.0, lift=0.0, knee=0.0, only=("fl", "fr", "bl", "br"), swing=0.0):
    """His legs (or `only` these) moved from their sprawl: the upper leg raised `lift` degrees (knee and foot up off the
    floor) or dropped `splay` (flat out to the side), the lower leg turned back `lift` to hang down again and folded
    `knee` further in under him, and the leg swung `swing` degrees forward. (A leg reaches out sideways: the game mirrors
    x, so a left leg's knee rises with -z and a right one's with +z, and a left leg swings forward with +y.)"""
    pose = {}
    for name in only:
        side = 1 if name in ("fl", "bl") else -1
        pose[f"leg_{name}"] = (0, side * swing, side * (splay - lift))
        pose[f"foot_{name}"] = (0, 0, side * (knee + lift))
    return pose


def _tail(y1=0.0, y2=0.0, y3=0.0, x1=0.0, x2=0.0, x3=0.0):
    """His tail turned `y` degrees at each of its three bones (one way with +, the other with -) and raised `x` (+ lifts
    a bone pointing back)."""
    return {"tail_1": (x1, y1, 0), "tail_2": (x2, y2, 0), "tail_3": (x3, y3, 0)}


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
# Sunk in the crucible: legs folded flat under him, head low, only his crest above the slag.
SUNK = _pose(_legs(splay=12, knee=20), _tail(x1=-4), neck=(6, 0, 0), head=(4, 0, 0))
# Waking: he heaves up out of the slag, head thrown up, roaring.
RISE = _pose(_legs(lift=10, knee=-10), body=(-12, 0, 0), neck=(-18, 0, 0), head=(-16, 0, 0), jaw=(34, 0, 0))
# In the air: legs splayed wide, tail streaming behind.
LEAP = _pose(_legs(splay=18, knee=-16), _tail(x1=-10, x2=-8, x3=-6), body=(-8, 0, 0), neck=(-8, 0, 0), head=(-6, 0, 0))
# Tail Sweep: coiled, his tail raised and curled round to one side; then swept round through his back and flanks.
COIL = _pose(_legs(lift=-4, knee=6), _tail(y1=-34, y2=-30, y3=-24, x1=14, x2=6), body=(0, 22, 0), neck=(0, -18, 0),
             head=(0, -12, 0), jaw=(10, 0, 0))
SWEPT = _pose(_legs(lift=-4, knee=6), _tail(y1=40, y2=34, y3=26, x1=4), body=(0, -24, 0), neck=(0, 20, 0),
              head=(0, 14, 0), jaw=(14, 0, 0))
# Ember Spit: head raised and drawn back, his throat glowing; then thrust forward, jaws wide.
GATHER = _pose(GLOWING_THROAT, body=(-6, 0, 0), neck=(-22, 0, 0), head=(-10, 0, 0), jaw=(8, 0, 0))
SPAT = _pose(GLOWING_THROAT, body=(2, 0, 0), neck=(10, 0, 0), head=(4, 0, 0), jaw=(40, 0, 0))
# Body Slam: rearing up on his hind legs, front legs high, plates flaring; aloft; then flat on the floor, legs splayed.
REAR = _pose(_legs(lift=30, knee=-20, only=("fl", "fr")), _legs(knee=10, only=("bl", "br")), _tail(x1=22, x2=6),
             body=(-34, 0, 0), neck=(16, 0, 0), head=(10, 0, 0), jaw=(16, 0, 0), root={"position": (0, 4, 0)})
SLAMMED = _pose(_legs(splay=26, knee=-10), _tail(x1=-6, x2=-4), body=(4, 0, 0), neck=(8, 0, 0), head=(6, 0, 0),
                jaw=(26, 0, 0), root={"position": (0, -3, 0)})
# Kiln Breath: chest swelled, head back; then head low and forward, jaws wide, the heat pouring out.
INHALE = _pose(GLOWING_THROAT, body=(-10, 0, 0), neck=(-20, 0, 0), head=(-14, 0, 0), jaw=(6, 0, 0))
EXHALE = _pose(GLOWING_THROAT, body=(4, 0, 0), neck=(14, 0, 0), head=(6, 0, 0), jaw=(44, 0, 0))
# Mantle Shed: hunched, shuddering, plates lifted.
SHUDDER = _pose(_legs(knee=12), _tail(x1=6), body=(6, 0, 0), neck=(10, 0, 0), head=(8, 0, 0), jaw=(12, 0, 0))
# The Eruption and the Cinder Rain: he roars up at the vent, head thrown back, front legs braced.
ROAR = _pose(_legs(lift=8, knee=-12, only=("fl", "fr")), _tail(x1=10, x2=6), body=(-18, 0, 0), neck=(-28, 0, 0),
             head=(-24, 0, 0), jaw=(46, 0, 0))
# Lava Wave: tail raised high over his back, then beaten down on the floor.
TAIL_UP = _pose(_tail(x1=36, x2=30, x3=24), body=(6, 0, 0), neck=(8, 0, 0), head=(-6, 0, 0), jaw=(14, 0, 0))
TAIL_DOWN = _pose(_legs(splay=8), _tail(x1=-6, x2=-6, x3=-4), body=(-4, 0, 0), neck=(4, 0, 0), head=(-4, 0, 0),
                  jaw=(24, 0, 0))
# Death: his legs give, he sinks flat, head down, tail limp.
FALLEN = _pose(_legs(splay=30, knee=-20), _tail(y1=10, y2=12, y3=8, x1=-8, x2=-4), body=(4, 0, 6), neck=(16, 0, 0),
               head=(20, -10, 0), jaw=(18, 0, 0), root={"position": (0, -4, 0)})


def _crawl(length, cycles):
    """His crawl: the diagonal legs together (front left with back right), his body swinging from side to side and his
    tail swaying the other way."""
    out = {}
    for name, phase in (("fl", 0.0), ("br", 0.0), ("fr", 0.5), ("bl", 0.5)):
        side = 1 if name in ("fl", "bl") else -1
        out[f"leg_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -side * 26, side * 10), phase=phase)}
        out[f"foot_{name}"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, 0, -side * 8), phase=phase + 0.25)}
    out["body"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, 7, 0))}
    out["neck"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -5, 0))}
    out["tail_1"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -10, 0), phase=0.1)}
    out["tail_2"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -12, 0), phase=0.2)}
    out["tail_3"] = {"rotation": wave(length, cycles, (0, 0, 0), (0, -14, 0), phase=0.3)}
    return out


def cinder_tyrant_animations():
    """His body's clips (the "body" controller) and his heat's (his seams burning, dark, flaring back, or white-hot in
    the Molten Heart). The body's never key a heat bone, nor the heat's any other."""
    return animations({
        "animation.cinder_tyrant.sunk": _sway(4.0, SUNK, {"body": {"position": ((0, 0.6, 0), 0.0)},
                                                          "head": ((-3, 0, 0), 0.2)}),
        "animation.cinder_tyrant.rise": posed(1.0, [(0.0, SUNK), (0.4, RISE), (1.0, RISE)]),
        "animation.cinder_tyrant.leap": _sway(0.8, LEAP, {"tail_2": ((0, 8, 0), 0.0), "tail_3": ((0, 10, 0), 0.2)}),
        "animation.cinder_tyrant.idle": _sway(3.0, IDLE, {"body": {"position": ((0, 0.4, 0), 0.0)},
                                                          "neck": ((0, 4, 0), 0.1), "head": ((-2, 3, 0), 0.2),
                                                          "tail_1": ((0, 5, 0), 0.0), "tail_2": ((0, 7, 0), 0.15),
                                                          "tail_3": ((0, 9, 0), 0.3)}),
        "animation.cinder_tyrant.walk": posed(1.0, [(0.0, IDLE), (1.0, IDLE)], loop=True, waves=_crawl(1.0, 1)),
        "animation.cinder_tyrant.sweep_windup": posed(0.8, [(0.0, IDLE), (0.8, COIL)]),
        "animation.cinder_tyrant.sweep": posed(0.7, [(0.0, COIL), (0.2, SWEPT), (0.4, SWEPT), (0.7, IDLE)]),
        "animation.cinder_tyrant.spit_windup": posed(1.2, [(0.0, IDLE), (0.5, GATHER), (1.2, GATHER)]),
        "animation.cinder_tyrant.spit": posed(0.65, [(0.0, GATHER), (0.1, SPAT), (0.35, SPAT), (0.65, IDLE)]),
        "animation.cinder_tyrant.slam_windup": posed(1.0, [(0.0, IDLE), (1.0, REAR)]),
        "animation.cinder_tyrant.slam": posed(1.5, [(0.0, REAR), (0.15, LEAP), (0.75, LEAP), (0.8, SLAMMED),
                                                    (1.2, SLAMMED), (1.5, IDLE)]),
        "animation.cinder_tyrant.breath_windup": posed(1.5, [(0.0, IDLE), (1.5, INHALE)]),
        "animation.cinder_tyrant.breath": _sway(0.5, EXHALE, {"head": ((0, 6, 0), 0.0), "jaw": ((4, 0, 0), 0.25, 2)}),
        "animation.cinder_tyrant.shed_windup": _sway(0.25, SHUDDER, {"body": ((2, 3, 2), 0.0, 2),
                                                                     "tail_1": ((0, 6, 0), 0.25, 2)}),
        "animation.cinder_tyrant.shed": posed(0.5, [(0.0, SHUDDER), (0.15, {**SHUDDER, "body": (-8, 0, 0)}),
                                                    (0.5, IDLE)]),
        "animation.cinder_tyrant.roar": _sway(0.5, ROAR, {"head": ((0, 5, 3), 0.0, 2), "jaw": ((4, 0, 0), 0.0)}),
        "animation.cinder_tyrant.wave_windup": posed(1.5, [(0.0, IDLE), (0.35, TAIL_UP), (0.55, TAIL_DOWN),
                                                           (0.85, TAIL_UP), (1.05, TAIL_DOWN), (1.35, TAIL_UP),
                                                           (1.5, TAIL_UP)]),
        "animation.cinder_tyrant.wave": posed(2.0, [(0.0, TAIL_UP), (0.12, TAIL_DOWN), (1.6, TAIL_DOWN), (2.0, IDLE)]),
        "animation.cinder_tyrant.death": posed(2.0, [(0.0, IDLE), (0.5, SHUDDER), (1.2, FALLEN), (2.0, FALLEN)]),
        # His heat: seams burning (hot), dark (quenched), flaring back over a second (reheating), or white-hot cores
        # swelling out of them (the Molten Heart).
        "animation.cinder_tyrant.heat_hot": clip(1.0, {"seams": {"scale": still((1, 1, 1))},
                                                       "tail_seams": {"scale": still((1, 1, 1))},
                                                       "heart": {"scale": still((0, 0, 0))}}),
        "animation.cinder_tyrant.heat_quenched": clip(1.0, {"seams": {"scale": still((0, 0, 0))},
                                                            "tail_seams": {"scale": still((0, 0, 0))},
                                                            "heart": {"scale": still((0, 0, 0))}}),
        "animation.cinder_tyrant.heat_reheat": clip(1.0, {
            "seams": {"scale": keys({0.0: (0, 0, 0), 0.6: (1, 1.6, 1), 1.0: (1, 1, 1)})},
            "tail_seams": {"scale": keys({0.0: (0, 0, 0), 0.6: (1, 1.6, 1), 1.0: (1, 1, 1)})},
            "heart": {"scale": still((0, 0, 0))}}, loop=False),
        "animation.cinder_tyrant.heat_heart": clip(0.8, {"seams": {"scale": still((1, 1, 1))},
                                                         "tail_seams": {"scale": still((1, 1, 1))},
                                                         "heart": {"scale": wave(0.8, 2, (1, 1, 1), (0.04, 0.25, 0.04))}}),
    })


# ------------------------------------------------------------------------------------------------- his Cinderlings

def cinderling_model():
    """A Cinderling, a shard of his mantle crawled off as a little salamander of glowing slag: a block long nose to
    tail, cracked and burning all over, two bright eyes."""
    m = Model("cinderling", 64, (1.2, 0.8))
    m.bone("root")
    m.bone("ling", "root", (0, 4, 0))
    m.cube("ling", "ling_body", (-3.5, 2, -5), (7, 5, 10))
    m.cube("ling", "ling_ridge", (-1, 7, -4), (2, 2, 8))
    m.bone("ling_head", "ling", (0, 4.5, -5))
    m.cube("ling_head", "ling_skull", (-3, 2.5, -10), (6, 4, 5))
    m.cube("ling_head", "ling_eye", (1.5, 6.5, -8.5), (2, 1, 2))
    m.cube("ling_head", "ling_eye", (-3.5, 6.5, -8.5), (2, 1, 2), mirror=True)
    m.bone("ling_tail", "ling", (0, 4.5, 5))
    m.cube("ling_tail", "ling_tail", (-1.5, 3, 5), (3, 3, 8))
    for name, side, z in (("fl", 1, -3.0), ("fr", -1, -3.0), ("bl", 1, 3.0), ("br", -1, 3.0)):
        mirror = side < 0
        m.bone(f"ling_leg_{name}", "ling", (side * 3.5, 3, z))
        m.cube(f"ling_leg_{name}", "ling_leg", ((3.5 if side > 0 else -7.5), 0, z - 1.5), (4, 3, 3), mirror=mirror)
    return m


def cinderling_animations():
    walk = {}
    for name, phase in (("fl", 0.0), ("br", 0.0), ("fr", 0.5), ("bl", 0.5)):
        side = 1 if name in ("fl", "bl") else -1
        walk[f"ling_leg_{name}"] = {"rotation": wave(0.4, 1, (0, 0, 0), (0, -side * 34, side * 12), phase=phase)}
    walk["ling"] = {"rotation": wave(0.4, 1, (0, 0, 0), (0, 9, 0))}
    walk["ling_tail"] = {"rotation": wave(0.4, 1, (0, 0, 0), (0, -18, 0), phase=0.15)}
    walk["ling_head"] = {"rotation": wave(0.4, 1, (0, 0, 0), (0, -6, 0))}
    return animations({
        "animation.cinderling.walk": clip(0.4, walk),
        "animation.cinderling.idle": clip(2.0, {
            "ling": {"scale": wave(2.0, 2, (1, 1, 1), (0.03, 0.06, 0.03))},
            "ling_head": {"rotation": wave(2.0, 1, (0, 0, 0), (-6, 10, 0))},
            "ling_tail": {"rotation": wave(2.0, 1, (0, 0, 0), (0, 16, 0), phase=0.3)}}),
    })


# ------------------------------------------------------------------------------------------------- his things

def gob_model():
    """A gob of magma he spits: a lumpy glowing blob, a crust cooling on one side, turning as it flies."""
    m = Model("magma_gob", 64, (1.0, 1.0))
    m.bone("root")
    m.bone("gob", "root", (0, 5, 0))
    m.cube("gob", "gob_core", (-4, 1, -4), (8, 8, 8))
    m.cube("gob", "gob_lump", (-5.5, 3, -2.5), (2, 4, 5))
    m.cube("gob", "gob_lump", (3.5, 2, -2), (2, 4, 5), mirror=True)
    m.cube("gob", "gob_crust", (-3, 8.5, -3), (6, 2, 6))
    m.cube("gob", "gob_drip", (-1, -1, -1), (2, 2, 2))
    return m


def gob_animations():
    return animations({"animation.magma_gob.spin": clip(0.6, {"gob": {
        "rotation": keys({0.0: (0, 0, 0), 0.3: (180, 60, 20), 0.6: (360, 120, 0)}),
        "scale": wave(0.6, 2, (1, 1, 1), (0.06, -0.06, 0.06))}})})


def cinder_model():
    """A cinder shaken from the vent: a jagged lump of glowing rock trailing a wisp of smoke."""
    m = Model("falling_cinder", 64, (1.0, 1.5))
    m.bone("root")
    m.bone("cinder", "root", (0, 5, 0))
    m.cube("cinder", "cinder_rock", (-3.5, 1, -3.5), (7, 7, 7))
    m.cube("cinder", "cinder_shard", (-2, 7.5, -1), (3, 3, 3), rotation=(20, 0, 25), pivot=(-0.5, 7.5, 0.5))
    m.cube("cinder", "cinder_shard_low", (1, -1, -2), (3, 2, 3))
    m.bone("trail", "root", (0, 9, 0))
    m.cube("trail", "trail", (-2, 9, -2), (4, 10, 4))
    return m


def cinder_animations():
    """Turning as it drops, its smoke trail wavering above it."""
    return animations({"animation.falling_cinder.fall": clip(1.0, {
        "cinder": {"rotation": keys({0.0: (0, 0, 0), 0.5: (40, 180, -20), 1.0: (0, 360, 0)})},
        "trail": {"scale": wave(1.0, 2, (1, 1, 1), (0.15, 0.1, 0.15))}})})


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"cinder_tyrant": cinder_tyrant_model, "cinderling": cinderling_model, "magma_gob": gob_model,
          "falling_cinder": cinder_model}
ANIMATIONS = {"cinder_tyrant": cinder_tyrant_animations, "cinderling": cinderling_animations, "magma_gob": gob_animations,
              "falling_cinder": cinder_animations}
ENTITY_MODEL = {name: name for name in MODELS}
# The clips the Java names (CinderTyrantEntity and his things).
CLIPS = {
    "cinder_tyrant": ("sunk", "rise", "leap", "idle", "walk", "sweep_windup", "sweep", "spit_windup", "spit",
                      "slam_windup", "slam", "breath_windup", "breath", "shed_windup", "shed", "roar", "wave_windup",
                      "wave", "death", "heat_hot", "heat_quenched", "heat_reheat", "heat_heart"),
    "cinderling": ("walk", "idle"),
    "magma_gob": ("spin",),
    "falling_cinder": ("fall",),
}
# The bones each of the Tyrant's controllers owns besides "body": no bone is animated by two.
CONTROLLERS = {"heat": HEAT_BONES}
