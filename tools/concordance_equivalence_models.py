"""The Assayer's Scale's block model (roadmap step 21, tools/concordance_equivalence.py).

A balance on a dark-oak plinth: a brass pillar and beam with a finial, and a brass pan hanging on iron chains from each
end. A plain block model (no animation: the scale's state is in its words and Jade, not its pose). Its one 16x16
texture, jugcraft:block/assayers_scale, is four 8x8 regions, each painted in one material by
tools/concordance_equivalence_art.py: REGIONS gives them, and every face of every element reads only inside its own.
"""

MOD = "jugcraft"

# Material regions of the texture: (u0, v0, u1, v1).
REGIONS = {"wood": (0, 0, 8, 8), "brass": (8, 0, 16, 8), "iron": (0, 8, 8, 16), "pan": (8, 8, 16, 16)}

# name: (from, to, material); the pans' tops use the "pan" region (their dished inside), their other faces brass.
ELEMENTS = [
    ("base", (3, 0, 3), (13, 2, 13), "wood"),
    ("pillar", (7.5, 2, 7.5), (8.5, 12, 8.5), "brass"),
    ("beam", (1, 12, 7.5), (15, 13, 8.5), "brass"),
    ("finial", (7.25, 13, 7.25), (8.75, 14.5, 8.75), "brass"),
    ("chain_west", (1.75, 8, 7.75), (2.25, 12, 8.25), "iron"),
    ("chain_east", (13.75, 8, 7.75), (14.25, 12, 8.25), "iron"),
    ("pan_west", (0, 7, 6), (4, 8, 10), "brass"),
    ("pan_east", (12, 7, 6), (16, 8, 10), "brass"),
]


def _uv(region, width, height):
    u0, v0, u1, v1 = REGIONS[region]
    return [u0, v0, u0 + min(width, u1 - u0), v0 + min(height, v1 - v0)]


def scale_model():
    elements = []
    for name, start, end, material in ELEMENTS:
        sx, sy, sz = (end[i] - start[i] for i in range(3))
        faces = {}
        for side, (w, h) in (("north", (sx, sy)), ("south", (sx, sy)), ("east", (sz, sy)), ("west", (sz, sy)),
                             ("up", (sx, sz)), ("down", (sx, sz))):
            region = "pan" if name.startswith("pan") and side == "up" else material
            faces[side] = {"uv": _uv(region, w, h), "texture": "#scale"}
        elements.append({"name": name, "from": list(start), "to": list(end), "faces": faces})
    return {"parent": "minecraft:block/block", "textures": {"scale": f"{MOD}:block/assayers_scale",
                                                            "particle": f"{MOD}:block/assayers_scale"},
            "elements": elements}
