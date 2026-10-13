"""The Reliquary Shrine's GeckoLib model and animations (roadmap step 20, tools/concordance_relics.py).

A blackstone plinth in two steps, a squat column, an amethyst cradle with a candle at each corner, and above it the
shrine's own crystal focus inside a thin brass halo. Three animations, chosen by what the server says
(ReliquaryShrineBlockEntity): empty (the focus rests, still), holding (a relic is installed but not working: the focus
turns slowly) and working (the focus spins and bobs and the halo turns the other way).

GEO, ANIMATIONS and SIZES are keyed by block id. SIZES[id] is (UV, SIZES): each box's UV origin on the 64x64 sheet and
its (w, h, d); tools/concordance_relics_art.py paints the sheet box by box.
"""

SHRINE_UV = {"base": (0, 0), "step": (0, 17), "column": (40, 17), "cradle": (0, 29), "candle": (32, 29), "flame": (36, 29),
             "focus": (40, 29), "halo_x": (24, 39), "halo_z": (38, 39)}
SHRINE_SIZES = {"base": (14, 3, 14), "step": (10, 2, 10), "column": (6, 6, 6), "cradle": (8, 2, 8), "candle": (1, 3, 1),
                "flame": (1, 1, 1), "focus": (2, 4, 2), "halo_x": (6, 1, 1), "halo_z": (1, 1, 4)}


def _cube(name, origin):
    u, v = SHRINE_UV[name]
    return {"origin": origin, "size": list(SHRINE_SIZES[name]), "uv": [u, v]}


def shrine_geo():
    corners = [(-4, -4), (3, -4), (-4, 3), (3, 3)]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.reliquary_shrine", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 1, 0]},
        "bones": [
            {"name": "plinth", "pivot": [0, 0, 0], "cubes": [_cube("base", [-7, 0, -7]), _cube("step", [-5, 3, -5]),
                                                             _cube("column", [-3, 5, -3])]},
            {"name": "cradle", "parent": "plinth", "pivot": [0, 11, 0], "cubes": [_cube("cradle", [-4, 11, -4])]
             + [_cube("candle", [x, 13, z]) for x, z in corners] + [_cube("flame", [x, 16, z]) for x, z in corners]},
            {"name": "focus", "parent": "cradle", "pivot": [0, 16, 0], "rotation": [0, 45, 0],
             "cubes": [_cube("focus", [-1, 14, -1])]},
            {"name": "halo", "parent": "cradle", "pivot": [0, 16, 0], "cubes": [
                _cube("halo_x", [-3, 16, -3]), _cube("halo_x", [-3, 16, 2]), _cube("halo_z", [-3, 16, -2]),
                _cube("halo_z", [2, 16, -2])]},
        ]}]}


def shrine_animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.reliquary_shrine.empty": {"loop": True, "animation_length": 4.0, "bones": {
            "focus": {"rotation": {"0.0": [0, 0, 0]}, "position": {"0.0": [0, -1, 0]}},
            "halo": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.reliquary_shrine.holding": {"loop": True, "animation_length": 8.0, "bones": {
            "focus": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 180, 0], "8.0": [0, 360, 0]}, "position": {"0.0": [0, 0, 0]}},
            "halo": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.reliquary_shrine.working": {"loop": True, "animation_length": 4.0, "bones": {
            "focus": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 180, 0], "2.0": [0, 360, 0], "3.0": [0, 540, 0], "4.0": [0, 720, 0]},
                      "position": {"0.0": [0, 0, 0], "1.0": [0, 1, 0], "2.0": [0, 0, 0], "3.0": [0, 1, 0], "4.0": [0, 0, 0]}},
            "halo": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, -180, 0], "4.0": [0, -360, 0]}}}},
    }}


GEO = {"reliquary_shrine": shrine_geo}
ANIMATIONS = {"reliquary_shrine": shrine_animations}
SIZES = {"reliquary_shrine": (SHRINE_UV, SHRINE_SIZES)}
