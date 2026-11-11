"""The Long Glass Coffee Table (Justin's walk through of the Drone Tower, 24 October 2026, note 16: the small table by
the break area's sofa to become a long glass table; take 1 approved 25 October): for that three-seat sofa.

2.4 m long, 60 cm deep, 42 cm high, three blocks along its length (its middle over the middle block): a 19 mm
toughened glass top (the plant's glass tables' glass, pi_glass_top and pi_glass_edge) on two black steel sled frames of
25 mm square tube, with rails under its long edges; on it two of the plant's mugs, its magazines and the TV's remote.
Built whole along x 0..48 for a piece facing north (its user to the south); each block's boxes cut from it, the glass's
faces on the cuts left out (they would show through the glass as lines). Every face at least 0.1 px from any other in its
plane (no z-fighting); 1 px is 6.25 cm.
"""
from __future__ import annotations

import plant_office4 as O4
import plant_office5 as O5

STEEL = "pi_pnl_black"
X0, X1, Z0, Z1 = 4.8, 43.2, 3.2, 12.8      # the glass top across and deep
GY0, GY1 = 6.4, 6.7                         # its underside and top
TUBE = 0.4                                  # the frame's tube


def _frame():
    out = []
    for a in (X0 + 1.6, X1 - 2.0):                                                # the sleds
        out += [(a, 0, Z0 + 0.4, a + TUBE, 0.4, Z1 - 0.4, STEEL),
                (a, 0.4, Z0 + 0.4, a + TUBE, GY0 - 0.4, Z0 + 0.8, STEEL), (a, 0.4, Z1 - 0.8, a + TUBE, GY0 - 0.4, Z1 - 0.4, STEEL),
                (a, GY0 - 0.4, Z0 + 0.4, a + TUBE, GY0, Z1 - 0.4, STEEL)]
    rails = [(X0 + 2.0, GY0 - 0.4, Z0 + 0.4, X1 - 2.0, GY0, Z0 + 0.8, STEEL),       # under its long edges
             (X0 + 2.0, GY0 - 0.4, Z1 - 0.8, X1 - 2.0, GY0, Z1 - 0.4, STEEL)]
    return out, rails


def _things():
    """What stands on it, each within one block: the mugs, the magazines, the remote."""
    return (O4.mug(9.5, 5.0, GY1) + O4.mug(35.0, 8.6, GY1) + O4.magazines(20.0, 5.2, GY1)
            + [(29.0, GY1, 8.6, 31.8, GY1 + 0.25, 9.5, "pi_remote")])


def _cut(box, c):
    """The share of a box along x in block c, in that block's coordinates (None if none); its faces on the cuts left
    out (they face each other inside it: through the glass they would show as lines)."""
    a, b = max(box[0], c * 16), min(box[3], c * 16 + 16)
    if b - a <= 0.01:
        return None
    t = box[6]
    if a > box[0] or b < box[3]:
        spec = dict(t) if isinstance(t, dict) else {"*": t}
        if a > box[0]:
            spec["west"] = None
        if b < box[3]:
            spec["east"] = None
        t = spec
    return (a - c * 16, box[1], box[2], b - c * 16, box[4], box[5], t)


def part(c, r=0):
    """Block c of the table (0 the user's left end)."""
    glass = (X0, GY0, Z0, X1, GY1, Z1, {"*": "pi_glass_edge", "up": "pi_glass_top", "down": "pi_glass_top"})
    sleds, rails = _frame()
    out = [_cut(glass, c)]
    out += [_cut(b, c) for b in rails]
    out += O5.slice_into(sleds + _things(), c, 0)
    return [b for b in out if b is not None]


def makers():
    """The remote's texture: black plastic, its buttons."""
    from PIL import Image, ImageDraw
    import numpy as np

    def remote():
        rng = np.random.default_rng(6060)
        a = np.clip(np.full((64, 64, 3), 18.0) + rng.normal(0, 1.5, (64, 64, 1)), 0, 255).astype(np.uint8)
        img = Image.fromarray(a, "RGB").convert("RGBA")
        d = ImageDraw.Draw(img)
        for k in range(6):
            for j in range(3):
                d.rectangle((8 + k * 8, 14 + j * 14, 12 + k * 8, 20 + j * 14), fill=(54, 56, 60, 255))
        d.rectangle((52, 26, 58, 36), fill=(170, 40, 40, 255))
        return img
    return {"pi_remote": remote}


PIECES = {"long_glass_table": {"display": "Long Glass Coffee Table", "multi": (3, 1, False), "parts": part, "item3d": True}}
CRAFTING = {"long_glass_table": (["GGG", "N N"], {"G": "minecraft:glass", "N": "#c:plates/iron"}, 1)}
