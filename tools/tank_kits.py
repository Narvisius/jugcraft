"""The storage tank kits (nuclear/TankKitItem.java; Justin, Oct 7: "place down just a multi-block structure, and then
it's just that silo"). Each kit item places one whole storage tank at once: its foundation set into the ground, the
shell and roof of storage tank plates, the outlet, the inlet with its fill line, the caged ladder, the roof railing
and gate, the hatch and the fittings of its rating, drawn by the same code as the plant's tanks (medium_plant.tank_in).

    general / flammable / corrosive, each small (5 across, ~200 buckets), medium (9, ~1,200) and large (13, ~2,500)
    cryogenic: one size, tall and narrow, on legs, with its vaporizer

Each is written as a format 2 blueprint, data/jugcraft/tank_kit/<kit>.jugbp.json, cropped to what it places; its
stake (the anchor) is the cell just in front of it at ground level + 1, under the tank's middle, so the tank reaches
away from whoever places it. Its front (south in the file) is the tank's clear side, where the label is painted; the
outlet is on its left (west), the inlet behind (north), the ladder on its right (east).
"""
import json
import math
from pathlib import Path

import numpy as np

import nuclear_plants as P
from nuclear_plants import M
import medium_plant as MP

OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "data" / "jugcraft" / "tank_kit"
# size: (shell radius, shell height)
SIZES = {"small": (2.6, 6), "medium": (4.5, 8), "large": (6.5, 10)}
CRYO = (2.6, 11)
NAMES = {"small": "Small", "medium": "Medium", "large": "Large"}
PAD = M("minecraft:smooth_stone", (158, 158, 156))
LEG = M("jugcraft:tank_leg[facing=north]", (170, 174, 178))


class Raised:
    """Part b seen from dy higher up, for drawing a tank that stands on legs with tank_in."""

    def __init__(self, b, dy):
        self.b, self.dy = b, dy
        self.g = b.g

    def p(self, x, y, z, m):
        self.b.p(x, y + self.dy, z, m)

    def get(self, x, y, z):
        return self.b.get(x, y + self.dy, z)

    def disc(self, cx, cz, r, y0, y1, m, inner=-1.0):
        self.b.disc(cx, cz, r, y0 + self.dy, y1 + self.dy, m, inner=inner)


def rated(rating, size):
    """A general, flammable or corrosive tank of one size on its foundation (a corrosive one's wider, with its spill
    bund and the safety shower)."""
    r, h = SIZES[size]
    corrosive = rating == "corrosive"
    fr = r + (3 if corrosive else 1)
    w = 2 * math.ceil(fr) + 3   # odd, so the tank's middle is a block's middle (5, 9 and 13 across)
    c = w / 2
    b = MP.Building(f"tank_kit_{rating}_{size}", f"{NAMES[size]} {rating.title()} Storage Tank", w, w, h + 1, grounded=True)
    MP.tank_in(b, c, c, r, h, outlet="w", inlet="n", ladder="e", manway=None if size == "small" else "s",
               vent=(0, -1) if size == "small" else (0, 0), rating=rating, bund=corrosive, shower=corrosive,
               foundation_r=fr, wall=1.0 if size == "small" else 1.2)
    MP.finish_railings(b)
    return b, int(c)


def cryogenic():
    """The cryogenic tank: a concrete pad, four legs, a floor of plates, a tall narrow shell with the relief valves on
    its roof (no ladder, railing or hatch: nobody goes up there), and the ambient vaporizer's two finned columns standing
    on the pad beside the outlet."""
    r, h = CRYO
    w = 11
    c = w / 2
    mx = int(c)
    b = MP.Building("tank_kit_cryogenic", "Cryogenic Storage Tank", w, w, h + 3, grounded=True)
    b.disc(c, c, 4.6, 0, 0, PAD)
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        b.p(mx + dx, 1, mx + dz, LEG)
    out_cell, _, _ = MP.tank_in(Raised(b, 2), c, c, r, h, outlet="w", inlet="n", ladder=None, manway=None,
                                foundation=MP.TANK_PLATE, foundation_r=r, rating="cryogenic")
    ox, oz = out_cell
    assert (ox, oz) == (mx - 3, mx), out_cell
    for vz in (oz - 2, oz + 2):
        for y in range(1, 5):
            b.p(ox - 1, y, vz, M("jugcraft:cryogenic_vaporizer[facing=west]", (206, 212, 216)))
    return b, mx


def clear_inside(b, r, h, y1):
    """The space inside the shell is air in the blueprint, so placing the kit clears it (grass or flowers in there
    would keep the tank from forming) and refuses where something solid stands in it."""
    w, _, d = b.g.shape
    c = w / 2
    for x in range(w):
        for z in range(d):
            if math.hypot(x + 0.5 - c, z + 0.5 - c) < r and b.get(x, y1, z) == 0:
                for y in range(y1, y1 + h):
                    assert b.get(x, y, z) == 0
                    b.extras[(x, y, z)] = "minecraft:air"


def blueprint(b, centre, name, description):
    """The kit's blueprint, cropped to its blocks; the stake in front of the tank's middle, one above the ground."""
    cells = P.part_cells(b)
    xs = [x for x, _, _ in cells]
    ys = [y for _, y, _ in cells]
    zs = [z for _, _, z in cells]
    x0, y0, z0 = min(xs), min(ys), min(zs)
    size = (max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1)
    assert y0 == 0, "the foundation is laid in the ground"
    moved = {(x - x0, y - y0, z - z0): st for (x, y, z), st in cells.items()}
    palette, layers = P.rle_layers(moved, size)
    return {"format": 2, "name": name, "author": "Jugcraft (built in)", "kind": "individual", "description": description,
            "size": list(size), "anchor": [centre - x0, 1 - y0, size[2]], "palette": palette, "layers": layers}


def kits():
    """{kit id: (part, blueprint)} for every kit."""
    out = {}
    for rating in ("general", "flammable", "corrosive"):
        for size in SIZES:
            b, centre = rated(rating, size)
            b.capacity = capacity(b, *SIZES[size], 1)
            clear_inside(b, *SIZES[size], 1)
            out[f"{rating}_{size}"] = (b, blueprint(b, centre, b.name, f"A {size} storage tank rated for {rating} liquids, "
                                                                      "placed whole by its kit."))
    b, centre = cryogenic()
    b.capacity = capacity(b, *CRYO, 3)
    clear_inside(b, *CRYO, 3)
    out["cryogenic"] = (b, blueprint(b, centre, b.name, "A vacuum-insulated tank for liquid oxygen, placed whole by its kit."))
    return out


def capacity(b, r, h, y1):
    """Buckets the tank holds: four for every block of space inside the shell (StorageTankPortBlockEntity), the
    shell's courses from y1 up, h of them."""
    w, _, d = b.g.shape
    c = w / 2
    inside = sum(1 for x in range(w) for z in range(d)
                 if math.hypot(x + 0.5 - c, z + 0.5 - c) < r and b.get(x, y1, z) == 0)
    return inside * h * 4


def write(out=OUT):
    out.mkdir(parents=True, exist_ok=True)
    report = []
    for kit, (b, bp) in kits().items():
        (out / f"{kit}.jugbp.json").write_text(json.dumps(bp, separators=(",", ":")) + "\n", encoding="utf-8")
        report.append(f"{kit}: {bp['size'][0]} x {bp['size'][2]}, {bp['size'][1]} tall, about {b.capacity:,} buckets")
    return report


if __name__ == "__main__":
    for line in write():
        print(line)
