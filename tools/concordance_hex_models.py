"""GeckoLib models and animations for roadmap step 22 (tools/concordance_hexes.py): the Oneiric Censer (block) and the
dream wisp (entity).

The Oneiric Censer: a dark-oak foot and a brass post with an arm, from which a round brass censer hangs on a short rod;
its lid has a finial, and a violet smoke plume rises from it. Animations, chosen by whether someone dreams by it
(OneiricCenserBlockEntity): idle (the censer hangs still, the plume hidden) and smoking (it swings gently and the plume
rises and turns).

The dream wisp: a small pale-violet core inside a ring of two crossed flat petals. Its one animation, drift, turns the
petals and pulses the core.

GEO, ANIMATIONS and SIZES are keyed by id. SIZES[id] is (UV, SIZES): each box's UV origin and its (w, h, d);
tools/concordance_hexes_art.py paints the sheets box by box (64x64 for the censer, 32x32 for the wisp).
"""

CENSER_UV = {"foot": (0, 0), "post": (0, 14), "arm": (16, 14), "rod": (40, 14), "bowl": (8, 24), "lid": (28, 24), "plume": (0, 38)}
CENSER_SIZES = {"foot": (8, 2, 8), "post": (2, 12, 2), "arm": (8, 1, 2), "rod": (1, 2, 1), "bowl": (5, 3, 5), "lid": (4, 1, 4),
                "plume": (3, 6, 3)}

WISP_UV = {"core": (0, 0), "petal_x": (0, 8), "petal_z": (12, 8)}
WISP_SIZES = {"core": (3, 3, 3), "petal_x": (6, 6, 0), "petal_z": (0, 6, 6)}


def _cube(uv, sizes, name, origin):
    u, v = uv[name]
    return {"origin": origin, "size": list(sizes[name]), "uv": [u, v]}


def censer_geo():
    c = lambda name, origin: _cube(CENSER_UV, CENSER_SIZES, name, origin)  # noqa: E731
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.oneiric_censer", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": [
            {"name": "stand", "pivot": [0, 0, 0], "cubes": [c("foot", [-4, 0, -4]), c("post", [-5, 2, -1]), c("arm", [-5, 14, -1])]},
            {"name": "censer", "parent": "stand", "pivot": [1.5, 14, 0], "cubes": [
                c("rod", [1, 12, -0.5]), c("bowl", [-1, 9, -2.5]), c("lid", [-0.5, 12, -2])]},
            {"name": "plume", "parent": "censer", "pivot": [1.5, 15, 0], "cubes": [c("plume", [0, 15, -1.5])]},
        ]}]}


def censer_animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.oneiric_censer.idle": {"loop": True, "animation_length": 4.0, "bones": {
            "censer": {"rotation": {"0.0": [0, 0, 0]}},
            "plume": {"scale": {"0.0": [0, 0, 0]}}}},
        "animation.oneiric_censer.smoking": {"loop": True, "animation_length": 4.0, "bones": {
            "censer": {"rotation": {"0.0": [0, 0, -6], "2.0": [0, 0, 6], "4.0": [0, 0, -6]}},
            "plume": {"scale": {"0.0": [1, 0.8, 1], "2.0": [1, 1.2, 1], "4.0": [1, 0.8, 1]},
                      "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]}}}},
    }}


def wisp_geo():
    c = lambda name, origin: _cube(WISP_UV, WISP_SIZES, name, origin)  # noqa: E731
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.dream_wisp", "texture_width": 32, "texture_height": 32,
                        "visible_bounds_width": 1, "visible_bounds_height": 1, "visible_bounds_offset": [0, 0.25, 0]},
        "bones": [
            {"name": "body", "pivot": [0, 4, 0], "cubes": [c("core", [-1.5, 2.5, -1.5])]},
            {"name": "petals", "parent": "body", "pivot": [0, 4, 0], "cubes": [c("petal_x", [-3, 1, 0]), c("petal_z", [0, 1, -3])]},
        ]}]}


def wisp_animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.dream_wisp.drift": {"loop": True, "animation_length": 3.0, "bones": {
            "petals": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 180, 0], "3.0": [0, 360, 0]}},
            "body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.15, 1.15, 1.15], "3.0": [1, 1, 1]}}}},
    }}


GEO = {"oneiric_censer": censer_geo, "dream_wisp": wisp_geo}
ANIMATIONS = {"oneiric_censer": censer_animations, "dream_wisp": wisp_animations}
SIZES = {"oneiric_censer": (CENSER_UV, CENSER_SIZES), "dream_wisp": (WISP_UV, WISP_SIZES)}
