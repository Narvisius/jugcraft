"""The Conclave Lectern's block model (roadmap step 23, tools/concordance_conclave.py).

A reading desk of dark oak on a stone-dark plinth: a square post with a brass collar holding a slanted desk, a brass lip
along the desk's lower edge and the Conclave's star chart open on it. A plain block model (no animation: what the
lectern serves is told in its words). Its one 16x16 texture, jugcraft:block/conclave_lectern, is four 8x8 regions,
each painted in one material by tools/concordance_conclave_art.py: REGIONS gives them, and every face of every element
reads only inside its own.
"""

MOD = "jugcraft"

# Material regions of the texture: (u0, v0, u1, v1).
REGIONS = {"wood": (0, 0, 8, 8), "brass": (8, 0, 16, 8), "chart": (0, 8, 8, 16), "plinth": (8, 8, 16, 16)}

# The desk, its chart and its lip lean back together: one rotation about the desk's centre.
TILT = {"origin": [8, 12, 8], "axis": "x", "angle": -22.5}

# name: (from, to, material, tilted); the chart's top uses the "chart" region, its other faces wood.
ELEMENTS = [
    ("plinth", (2, 0, 2), (14, 2, 14), "plinth", False),
    ("post", (6, 2, 6), (10, 11, 10), "wood", False),
    ("collar", (5.5, 10, 5.5), (10.5, 11, 10.5), "brass", False),
    ("desk", (2, 11, 3), (14, 13, 13), "wood", True),
    ("chart", (3, 13, 4), (13, 13.5, 12), "wood", True),
    ("lip", (2, 13, 3), (14, 14, 4), "brass", True),
]


def _uv(region, width, height):
    u0, v0, u1, v1 = REGIONS[region]
    return [u0, v0, u0 + min(width, u1 - u0), v0 + min(height, v1 - v0)]


def lectern_model():
    elements = []
    for name, start, end, material, tilted in ELEMENTS:
        sx, sy, sz = (end[i] - start[i] for i in range(3))
        faces = {}
        for side, (w, h) in (("north", (sx, sy)), ("south", (sx, sy)), ("east", (sz, sy)), ("west", (sz, sy)),
                             ("up", (sx, sz)), ("down", (sx, sz))):
            region = "chart" if name == "chart" and side == "up" else material
            faces[side] = {"uv": _uv(region, w, h), "texture": "#lectern"}
        element = {"name": name, "from": list(start), "to": list(end), "faces": faces}
        if tilted:
            element["rotation"] = dict(TILT)
        elements.append(element)
    return {"parent": "minecraft:block/block", "textures": {"lectern": f"{MOD}:block/conclave_lectern",
                                                            "particle": f"{MOD}:block/conclave_lectern"},
            "elements": elements}
