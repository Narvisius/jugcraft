"""The Spire Heart's GeckoLib model and animations (roadmap step 25, the Concord Spire).

The heart is the keystone a Concord Spire is founded at: a column of polished deepslate is raised on its top, so the
whole model stays inside its own block (here -8..8 across and 0..16 up, the block's 0..16 on every axis) and its top is
a flat plate the full block wide for the column to sit on. From the ground up, each course stepped half a pixel:

- the foot, a slab of dark dressed stone the full block wide (two pixels);
- the lower strap, a band of blackened steel half a pixel in from the foot (two pixels), its top the window sills and
  the floor of the chamber inside;
- four stone corner posts, three pixels square, half a pixel in from the straps (eight pixels), so each side shows a
  window eight pixels wide and tall between them;
- the upper strap (two pixels), its underside the window heads and the chamber's ceiling;
- the top plate, dark steel the full block wide (two pixels), the seat the column stands on.

Inside the chamber the core, a violet-white gem (a 3x3x3 cube stood on one corner, its long diagonal upright: the gem
bone's rotation [45, 0, 35.264] turns it about x and then z, as GeckoLib applies a bone's rotation), floats in a brass
cage: two square frames, seven pixels across, half a pixel above the lower strap and below the upper one, joined at
their corners by four ribs. One rib carries a violet inlay, so a turn reads. At rest the ribs stand by the posts' inner
corners, clear of the windows, and each window shows the cage's frame round the gem; turning, every rib crosses every
window. The cage and the gem turn about the block's centre line, and nothing moving comes nearer than 0.7 of a pixel to
a post (the ribs reach 4.95 from it, the posts' inner corners stand at 5.66) or half a pixel to a strap.

Three animations, chosen by the status the server sends (SpireHeartBlockEntity):
- dormant: everything still; the gem small (scale 0.55) and resting a pixel low in its cage, so little of it glows;
- raising: the cage still; the gem breathes slowly, scale 0.72 to 0.86 with a quarter-pixel rise and fall, once in four
  seconds;
- active: the cage turns steadily, a full turn in eight seconds, and the gem turns the other way while it pulses, scale
  0.94 to 1.06 every two seconds.
Pulses are sines sampled eight times a cycle; every loop ends where it starts (a turn ends at 360 degrees), so nothing
pops at the seam.

GEO, ANIMATIONS and SIZES are keyed by block id. SIZES[id] is (UV, SIZES): each box's UV origin on the 64x64 sheet and
its (w, h, d); tools/concordance_spire_art.py paints the sheet box by box. Boxes of one shape that are painted alike
share a UV region: the two straps, the four posts, the frames' bars of each direction and three of the ribs.
"""
import math

HEART_UV = {"foot": (0, 0), "plate": (0, 18), "strap": (0, 36), "post": (0, 53), "gem": (12, 53),
            "frame_x": (24, 53), "frame_z": (40, 53), "rib": (52, 53), "rib_mark": (56, 53)}
HEART_SIZES = {"foot": (16, 2, 16), "plate": (16, 2, 16), "strap": (15, 2, 15), "post": (3, 8, 3), "gem": (3, 3, 3),
               "frame_x": (7, 1, 1), "frame_z": (1, 1, 5), "rib": (1, 5, 1), "rib_mark": (1, 5, 1)}

# The cage and the gem turn and scale about the block's vertical centre line, at the gem's middle.
PIVOT = [0, 8, 0]
# The gem's tilt: a turn of 45 degrees about x and then atan(1 / sqrt 2) about z stands a cube on one corner.
GEM_TILT = [45, 0, round(math.degrees(math.atan(1 / math.sqrt(2))), 3)]
# How far the moving parts reach from the centre line (pixels), and where the posts' inner corners stand.
CAGE_REACH = 3.5 * math.sqrt(2)
POST_CORNER = 4 * math.sqrt(2)


def _cube(name, origin):
    u, v = HEART_UV[name]
    return {"origin": origin, "size": list(HEART_SIZES[name]), "uv": [u, v]}


def heart_geo():
    posts = [(-7, -7), (4, -7), (-7, 4), (4, 4)]
    frames = []
    for y in (4.5, 10.5):
        frames += [_cube("frame_x", [-3.5, y, -3.5]), _cube("frame_x", [-3.5, y, 2.5]),
                   _cube("frame_z", [-3.5, y, -2.5]), _cube("frame_z", [2.5, y, -2.5])]
    ribs = [_cube("rib_mark", [2.5, 5.5, 2.5]), _cube("rib", [-3.5, 5.5, 2.5]), _cube("rib", [2.5, 5.5, -3.5]),
            _cube("rib", [-3.5, 5.5, -3.5])]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.spire_heart", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 1, 0]},
        "bones": [
            {"name": "plinth", "pivot": [0, 0, 0], "cubes": [
                _cube("foot", [-8, 0, -8]), _cube("strap", [-7.5, 2, -7.5])]
             + [_cube("post", [x, 4, z]) for x, z in posts]
             + [_cube("strap", [-7.5, 12, -7.5]), _cube("plate", [-8, 14, -8])]},
            {"name": "cage", "parent": "plinth", "pivot": list(PIVOT), "cubes": frames + ribs},
            {"name": "core", "parent": "plinth", "pivot": list(PIVOT), "cubes": []},
            {"name": "gem", "parent": "core", "pivot": list(PIVOT), "rotation": list(GEM_TILT),
             "cubes": [_cube("gem", [-1.5, 6.5, -1.5])]},
        ]}]}


def _t(seconds):
    """A keyframe's time as GeckoLib's JSON keys write it ("0.0", "0.25", "4.0")."""
    text = f"{seconds:.3f}".rstrip("0")
    return text + "0" if text.endswith(".") else text


def _wave(length, period, mid, amplitude, vector, samples=8):
    """Keyframes of mid + amplitude * sin(2 pi t / period) over the whole animation, sampled `samples` times a cycle
    and repeated until `length`; vector(value) gives each keyframe's [x, y, z]. The last keyframe equals the first."""
    frames = {}
    for i in range(round(length / period * samples) + 1):
        value = round(mid + amplitude * math.sin(2 * math.pi * i / samples), 3) + 0.0
        frames[_t(i * period / samples)] = vector(value)
    return frames


def _uniform(value):
    return [value, value, value]


def _rise(value):
    return [0, value, 0]


DORMANT_SCALE, DORMANT_DROP = 0.55, -1
RAISING_SCALE, RAISING_BOB = (0.79, 0.07), 0.25
ACTIVE_SCALE = (1.0, 0.06)


def heart_animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.spire_heart.dormant": {"loop": True, "animation_length": 4.0, "bones": {
            "core": {"rotation": {"0.0": [0, 0, 0]}, "position": {"0.0": _rise(DORMANT_DROP)},
                     "scale": {"0.0": _uniform(DORMANT_SCALE)}},
            "cage": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.spire_heart.raising": {"loop": True, "animation_length": 4.0, "bones": {
            "core": {"rotation": {"0.0": [0, 0, 0]}, "position": _wave(4.0, 4.0, 0, RAISING_BOB, _rise),
                     "scale": _wave(4.0, 4.0, RAISING_SCALE[0], RAISING_SCALE[1], _uniform)},
            "cage": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.spire_heart.active": {"loop": True, "animation_length": 8.0, "bones": {
            "core": {"rotation": {"0.0": [0, 0, 0], "8.0": [0, -360, 0]}, "position": {"0.0": [0, 0, 0]},
                     "scale": _wave(8.0, 2.0, ACTIVE_SCALE[0], ACTIVE_SCALE[1], _uniform)},
            "cage": {"rotation": {"0.0": [0, 0, 0], "8.0": [0, 360, 0]}}}},
    }}


GEO = {"spire_heart": heart_geo}
ANIMATIONS = {"spire_heart": heart_animations}
SIZES = {"spire_heart": (HEART_UV, HEART_SIZES)}
