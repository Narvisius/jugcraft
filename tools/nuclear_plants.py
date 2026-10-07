"""Pre-made nuclear plants (layout stage): every building of the three plants as block geometry, one block = one metre.

Each plant is a list of parts placed on a site. Every part is drawn in its own frame with its front facing south (+z),
like a blueprint, and turned when it is placed. The same geometry will later be written out as the green part
blueprints and the blue whole-plant blueprints; for now this only draws the layout previews for approval:

    python tools/nuclear_plants.py OUT_DIR

Materials are vanilla blocks plus the mod's shielding concrete. Equipment (the stage-1 reactor pieces) is listed per
part as footprints: it is placed whole from its item, not built block by block.
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# id: (block, rgb)
MATERIALS = {
    1: ("minecraft:light_gray_concrete", (176, 176, 170)),
    2: ("minecraft:white_concrete", (222, 222, 216)),
    3: ("minecraft:gray_concrete", (104, 107, 110)),
    4: ("minecraft:smooth_stone", (158, 158, 156)),
    5: ("minecraft:iron_block", (204, 208, 212)),
    6: ("minecraft:cyan_terracotta", (96, 104, 106)),
    7: ("minecraft:light_gray_stained_glass", (150, 180, 196)),
    8: ("minecraft:red_concrete", (156, 40, 34)),
    9: ("minecraft:gravel", (132, 126, 120)),
    10: ("minecraft:iron_bars", (92, 94, 98)),
    11: ("minecraft:water", (52, 92, 158)),
    12: ("jugcraft:asphalt", (56, 58, 60)),
    13: ("minecraft:polished_andesite", (132, 135, 132)),
    14: ("jugcraft:shielding_concrete", (186, 182, 172)),
    15: ("minecraft:yellow_concrete", (226, 178, 36)),
    16: ("minecraft:blue_concrete", (48, 64, 140)),
    17: ("minecraft:stone_bricks", (122, 122, 120)),
    18: ("minecraft:light_blue_terracotta", (116, 136, 150)),
    19: ("jugcraft:cooling_tower_fill", (206, 212, 214)),
    20: ("jugcraft:conduit_trench", (150, 150, 146)),
    21: ("jugcraft:steel_fluid_pipe", (120, 128, 136)),
}
NAMES = {k: v[0] for k, v in MATERIALS.items()}
_BY_STATE = {v[0]: k for k, v in MATERIALS.items() if v[0]}


def M(state, rgb=(150, 150, 150)):
    """The material id of a block state (any block, with its properties), added to the table the first time."""
    if state in _BY_STATE:
        return _BY_STATE[state]
    k = max(MATERIALS) + 1
    assert k < 65000
    MATERIALS[k] = (state, rgb)
    _BY_STATE[state] = k
    return k
CONC, WHITE, GREY, SLAB, STEEL, SIDING, GLASS, RED, GRAVEL, BARS, WATER, ROAD, TRIM, SHIELD, YELLOW, BLUE, BRICK, PANEL, FILL, TRENCH, DIST = range(1, 22)


class Part:
    """A building in its own frame: x across its front, z front-to-back toward +z (the front), y up."""

    def __init__(self, key, name, w, h, d, kind="building"):
        self.key, self.name, self.kind = key, name, kind
        self.g = np.zeros((w, h, d), np.uint16)
        self.props = []  # (kind, ax, ay, az, facing): decor equipment standing in the part (placed whole)
        self.extras = {}  # (x, y, z) -> block state text, for blocks with properties (the tower controller)
        self.controller = None
        self.sink = 0  # layers below the ground (a tower's controller sits under its basin floor)

    @property
    def size(self):
        return self.g.shape

    def box(self, x0, y0, z0, x1, y1, z1, m):
        """Fills x0..x1, y0..y1, z0..z1 inclusive (clipped)."""
        w, h, d = self.g.shape
        x0, x1 = max(0, min(x0, x1)), min(w - 1, max(x0, x1))
        y0, y1 = max(0, min(y0, y1)), min(h - 1, max(y0, y1))
        z0, z1 = max(0, min(z0, z1)), min(d - 1, max(z0, z1))
        if x0 <= x1 and y0 <= y1 and z0 <= z1:
            self.g[x0:x1 + 1, y0:y1 + 1, z0:z1 + 1] = m

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.box(x0, y0, z0, x1, y1, z1, 0)

    def shell(self, x0, y0, z0, x1, y1, z1, m):
        self.box(x0, y0, z0, x1, y1, z1, m)
        self.clear(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1)

    def disc(self, cx, cz, r, y0, y1, m, inner=-1.0):
        """A filled (or, with inner, ring) cylinder centred on cx, cz (block centres)."""
        w, h, d = self.g.shape
        xs = np.arange(w)[:, None] + 0.5 - cx
        zs = np.arange(d)[None, :] + 0.5 - cz
        dist = np.sqrt(xs * xs + zs * zs)
        mask = (dist <= r) & (dist > inner)
        for y in range(max(0, y0), min(h, y1 + 1)):
            self.g[:, y, :][mask] = m

    def ring(self, cx, cz, r, y, thick, m):
        self.disc(cx, cz, r, y, y, m, inner=r - thick)

    def blocks(self):
        return int(np.count_nonzero(self.g))

    def put(self, x, y, z, m):
        w, h, d = self.g.shape
        if 0 <= x < w and 0 <= y < h and 0 <= z < d:
            self.g[x, y, z] = m

    def prop(self, kind, x0, y, z0, facing="north"):
        """Decor equipment (drawn by the client, placed whole) with its footprint's corner nearest the origin at
        (x0, z0): its body is emptied and its anchor written. Props go in last."""
        _, w, d, _ = EQUIPMENT_SIZES[kind]
        if facing == "north":
            anchor = (x0, y, z0 + d - 1)
        elif facing == "east":
            anchor = (x0, y, z0)
        elif facing == "south":
            anchor = (x0 + w - 1, y, z0)
        else:
            anchor = (x0 + d - 1, y, z0 + w - 1)
        for c in equipment_cells(kind, *anchor, facing):
            assert 0 <= c[0] < self.size[0] and 0 <= c[1] < self.size[1] and 0 <= c[2] < self.size[2], (self.key, kind, c)
            self.g[c] = 0
            self.extras.pop(c, None)
        self.extras[anchor] = f"jugcraft:{kind}[facing={facing}]"
        self.props.append((kind, *anchor, facing))


# ---------------------------------------------------------------------------------------------------------------
# Building types


def hall(key, name, w, d, h, wall=CONC, roof=GREY, band=SIDING, pilaster=TRIM, windows=(), door=True, base=3):
    """A rectangular building: concrete plinth, clad walls with pilasters every 8, window strips, parapet roof."""
    p = Part(key, name, w, h + 2, d)
    p.shell(0, 0, 0, w - 1, h, d - 1, band)
    p.box(0, 0, 0, w - 1, base - 1, d - 1, wall)
    p.clear(1, 1, 1, w - 2, base - 1, d - 2)
    p.box(0, 0, 0, w - 1, 0, d - 1, SLAB)
    for x in range(0, w, 8):
        p.box(x, 0, 0, x, h, 0, pilaster)
        p.box(x, 0, d - 1, x, h, d - 1, pilaster)
    for z in range(0, d, 8):
        p.box(0, 0, z, 0, h, z, pilaster)
        p.box(w - 1, 0, z, w - 1, h, z, pilaster)
    for c in ((0, 0), (w - 1, 0), (0, d - 1), (w - 1, d - 1)):
        p.box(c[0], 0, c[1], c[0], h, c[1], pilaster)
    for wy0, wy1 in windows:
        for x in range(1, w - 1):
            if x % 8:
                p.box(x, wy0, 0, x, wy1, 0, GLASS)
                p.box(x, wy0, d - 1, x, wy1, d - 1, GLASS)
        for z in range(1, d - 1):
            if z % 8:
                p.box(0, wy0, z, 0, wy1, z, GLASS)
                p.box(w - 1, wy0, z, w - 1, wy1, z, GLASS)
    p.box(0, h, 0, w - 1, h, d - 1, roof)
    p.shell(0, h, 0, w - 1, h + 1, d - 1, TRIM)
    p.box(1, h, 1, w - 2, h, d - 2, roof)
    if door:
        dw = 4 if w > 20 else 2
        p.box(w // 2 - dw // 2, 1, d - 1, w // 2 - dw // 2 + dw - 1, min(5, h - 1), d - 1, YELLOW)
        p.clear(w // 2 - dw // 2 + 1, 1, d - 1, w // 2 - dw // 2 + dw - 2, min(4, h - 2), d - 1)
    return p


def containment(key, name, r, wall_h, dome_h, double=False):
    """A cylindrical reactor building with a dome; double = the EPR's inner and outer walls with an annulus."""
    size = int(math.ceil(r * 2)) + 2
    c = size / 2
    p = Part(key, name, size, wall_h + dome_h + 2, size)
    p.disc(c, c, r + 0.8, 0, 0, SLAB)
    p.disc(c, c, r, 0, wall_h, SHIELD, inner=r - 2)
    if double:
        p.disc(c, c, r - 4, 0, wall_h + 2, CONC, inner=r - 5.5)
    # Ring beam and buttresses.
    p.disc(c, c, r + 0.6, wall_h - 2, wall_h, CONC, inner=r - 2)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        bx, bz = c + math.cos(a) * (r + 0.2), c + math.sin(a) * (r + 0.2)
        p.box(int(bx) - 1, 0, int(bz) - 1, int(bx) + 1, wall_h, int(bz) + 1, CONC)
    # Dome: a shallow ellipsoid, 2 thick.
    # Each layer reaches in to just inside the layer above it, so the shell has no gap where it flattens near the top
    # (round 14: the EPR's dome had a ring open to the sky round its cap, and the core shone out of it).
    for y in range(1, dome_h + 1):
        t = y / dome_h
        rr = r * math.sqrt(max(0.0, 1 - t * t))
        above = r * math.sqrt(max(0.0, 1 - ((y + 1) / dome_h) ** 2))
        p.disc(c, c, rr, wall_h + y, wall_h + y, SHIELD, inner=max(-1, min(rr - 2.6, above - 1.2)))
    p.disc(c, c, 3, wall_h + dome_h, wall_h + dome_h, GREY)
    # Personnel and equipment hatches on the front.
    p.box(int(c) - 3, 1, size - 3, int(c) + 3, 9, size - 1, CONC)
    p.box(int(c) - 2, 2, size - 1, int(c) + 2, 7, size - 1, YELLOW)
    return p


def cooling_tower(key, name, base_r, height, throat_r, throat_y, kind, inlet=10, band=True):
    """A natural-draft hyperbolic cooling tower, working inside like a real one.

    Warm water comes up the riser in the middle, spreads through the distribution headers and sprays down over the
    fill (packs of corrugated sheets on concrete columns), where the air drawn in through the inlet between the raker
    columns cools it; it rains into the basin and goes back to the plant. The basin's water is in the blueprint but not
    built by drones: it fills itself when the tower is finished. The controller is under the basin floor."""
    b = throat_y / math.sqrt((base_r / throat_r) ** 2 - 1)

    def radius(y):
        return throat_r * math.sqrt(1 + ((y - throat_y) / b) ** 2)

    size = int(math.ceil(base_r * 2)) + 6
    c = size / 2
    p = Part(key, name, size, height + 2, size, kind="tower")
    g = 1  # the basin floor (layer 0 is below it: the controller and the water riser's foot)
    p.disc(c, c, base_r + 2.5, g, g, CONC)
    p.disc(c, c, base_r + 2.5, g + 1, g + 2, CONC, inner=base_r + 0.5)
    p.disc(c, c, base_r + 0.5, g + 1, g + 1, WATER)
    # Raker columns: crossing pairs under the shell, standing in the basin rim.
    n = int(2 * math.pi * base_r / 5)
    for i in range(n):
        for side in (-1, 1):
            prev = None
            for y in range(g + 1, g + inlet + 1):
                t = (y - g) / inlet
                a = 2 * math.pi * i / n + side * t * (math.pi / n)
                rr = radius(inlet) + (base_r - radius(inlet)) * (1 - t)
                x1, z1 = int(c + math.cos(a) * rr), int(c + math.sin(a) * rr)
                p.box(x1, y, z1, x1, y, z1, CONC)
                # Each step of the leaning column overlaps the one below, so the column is one piece.
                if y > g + 1:
                    p.box(x1, y, prev[1], x1, y, prev[1], CONC)
                    p.box(prev[0], y, prev[1], prev[0], y, prev[1], CONC)
                prev = (x1, z1)
    for y in range(g + inlet, height + 1):
        rr = radius(y - g)
        m = WHITE
        if band and height - 14 <= y <= height - 9:
            m = RED
        p.ring(c, c, rr + 0.5, y, 1.4, m)
    p.ring(c, c, radius(inlet) + 0.8, g + inlet, 2.2, CONC)
    p.ring(c, c, radius(height - g) + 0.9, height, 1.8, GREY)
    # The fill: two layers of packs just above the air inlet, on a grid of columns standing in the basin.
    fy = g + inlet + 1
    fill_r = radius(inlet + 1) - 1.5
    for x in range(size):
        for z in range(size):
            dx, dz = x + 0.5 - c, z + 0.5 - c
            if dx * dx + dz * dz < (fill_r - 1) ** 2 and x % 6 == 0 and z % 6 == 0:
                p.box(x, g + 1, z, x, fy - 1, z, CONC)
    p.disc(c, c, fill_r, fy, fy + 1, FILL, inner=3.5)
    # The hot water riser up the middle, and the distribution headers over the fill: eight spokes and two rings.
    p.disc(c, c, 3.5, g + 1, fy + 3, CONC)
    hy = fy + 3
    for k in range(8):
        a = k * math.pi / 4
        for r10 in range(35, int(fill_r * 10) - 10, 5):
            r = r10 / 10
            p.box(int(c + math.cos(a) * r), hy, int(c + math.sin(a) * r), int(c + math.cos(a) * r), hy, int(c + math.sin(a) * r), DIST)
    for rr in (fill_r * 0.45, fill_r * 0.8):
        p.ring(c, c, rr + 0.5, hy, 1.0, DIST)
    # The headers rest on the fill's support beams.
    under = p.g[:, hy, :] == DIST
    layer = p.g[:, hy - 1, :]
    layer[under & (layer == 0)] = CONC
    # The controller, under the basin floor in the middle, where the cold water leaves for the plant.
    m = size // 2
    p.controller = (m, 0, m)
    p.extras[p.controller] = f"jugcraft:cooling_tower_controller[facing=south,tower={kind}]"
    p.sink = 1
    return p


def mech_tower_bank(key, name, cells, kind="mechanical", cell=16, h=14):
    """A row of mechanical-draft cooling cells. In each: air comes in through louvres on both long sides, the water
    sprays from a header over two layers of fill and rains into the cell's basin; the fan on top (in its stack) pulls
    the air up through it. The basins fill themselves when the bank is finished; the controller is under the floor."""
    w, d = cells * cell, cell + 4
    p = Part(key, name, w, h + 9, d, kind="tower")
    g = 1
    p.box(0, g, 0, w - 1, g + 2, d - 1, CONC)
    for i in range(cells):
        x0 = i * cell
        p.shell(x0, g + 2, 2, x0 + cell - 1, g + h - 1, d - 3, CONC)
        # Louvres both sides, the basin water, the fill and the header.
        for y in range(g + 4, g + 9):
            if y % 2:
                p.box(x0 + 1, y, 2, x0 + cell - 2, y, 2, BARS)
                p.box(x0 + 1, y, d - 3, x0 + cell - 2, y, d - 3, BARS)
        p.box(x0 + 1, g + 3, 3, x0 + cell - 2, g + 3, d - 4, WATER)
        p.box(x0 + 1, g + 9, 3, x0 + cell - 2, g + 10, d - 4, FILL)
        for z in range(4, d - 4, 3):
            p.box(x0 + 2, g + 12, z, x0 + cell - 3, g + 12, z, DIST)
        p.box(x0, g + h - 1, 2, x0 + cell - 1, g + h - 1, d - 3, GREY)
        cx, cz = x0 + cell / 2, d / 2
        p.disc(cx, cz, cell / 2 - 1.5, g + h, g + h + 6, SIDING, inner=cell / 2 - 2.6)
        p.disc(cx, cz, cell / 2 - 2.6, g + h - 1, g + h - 1, 0)
    p.controller = (w // 2, 0, d // 2)
    p.extras[p.controller] = f"jugcraft:cooling_tower_controller[facing=south,tower={kind}]"
    p.sink = 1
    return p


def switchyard(key, name, w, d, bays):
    p = Part(key, name, w, 16, d)
    p.box(0, 0, 0, w - 1, 0, d - 1, GRAVEL)
    for x in range(w):
        p.box(x, 1, 0, x, 2, 0, BARS)
        p.box(x, 1, d - 1, x, 2, d - 1, BARS)
    for z in range(d):
        p.box(0, 1, z, 0, 2, z, BARS)
        p.box(w - 1, 1, z, w - 1, 2, z, BARS)
    p.box(w // 2 - 2, 1, d - 1, w // 2 + 1, 2, d - 1, 0)
    step = (w - 8) // max(1, bays)
    for i in range(bays + 1):
        x = 4 + i * step
        for z in (4, d // 2, d - 5):
            p.box(x, 1, z, x, 13, z, STEEL)
        p.box(x, 13, 4, x, 13, d - 5, STEEL)
        if i < bays:
            for z in range(8, d - 8, 6):
                p.box(x + step // 2 - 1, 1, z, x + step // 2, 4, z + 1, GREY)  # breakers
                p.box(x + step // 2, 5, z, x + step // 2, 8, z, STEEL)
    for z in (4, d // 2, d - 5):
        p.box(4, 13, z, 4 + bays * step, 13, z, STEEL)
    p.box(2, 1, 2, 9, 4, 6, CONC)  # relay kiosk
    return p


def pump_house(key, name, w, d, h, bays):
    """The circulating-water intake: a pump building on a deck that juts into the water, with screened bays.
    Its back (z = 0) faces the water; the Water Intake Port sits in each bay."""
    p = Part(key, name, w, h + 2, d)
    p.box(0, 0, 0, w - 1, 1, d - 1, CONC)
    q = hall(key, name, w, d - 6, h, wall=CONC, band=PANEL, windows=((h - 4, h - 3),))
    p.g[:, :, 6:] = np.maximum(p.g[:, :, 6:], q.g[:, : h + 2, :])
    bw = w // bays
    for i in range(bays):
        # Each bay is open to the lake below, behind a screen.
        p.clear(i * bw + 1, 0, 0, i * bw + bw - 2, 1, 5)
        p.box(i * bw + 1, 2, 1, i * bw + bw - 2, 2, 1, BARS)
    p.box(0, 2, 0, w - 1, 2, 0, TRIM)
    p.bays = bays
    return p


# ---------------------------------------------------------------------------------------------------------------
# Plants


class Site:
    """A whole plant in site coordinates: x east, z south (the lake is north, at z < 0), y up from the ground (0 is the
    first layer on the ground; below ground only the intake conduit legs). Parts are buildings (green blueprints);
    equipment, pipes and conduits only exist in the whole plant's blue blueprint."""

    def __init__(self, key, name, w, d, water):
        self.key, self.name, self.w, self.d = key, name, w, d
        self.water = water  # (x0, z0, x1, z1): the lake or river the plant draws from
        self.parts = []  # (part, x, z, rot)
        self.roads = []
        self.pipes = []  # (x0, z0, x1, z1) buried circulating water lines, for the plan
        self.equipment = []  # (kind, ax, ay, az, facing): the anchor (front left bottom corner) and the way it faces
        self.lines = {}  # (x, y, z) -> pipe id
        self.towers = []  # (x, y, z, tower kind) controllers
        self.decor = {}  # (x, y, z) -> material: what the plant adds besides parts (pipe galleries)

    def add(self, part, x, z, rot=0):
        self.parts.append((part, x, z, rot))
        return part

    def eq(self, kind, x0, z0, facing="north", y=1):
        """Places equipment by its footprint's corner nearest the origin (x0, z0): works out the anchor for the facing."""
        _, w, d, _ = EQUIPMENT_SIZES[kind]
        if facing == "north":
            anchor = (x0, y, z0 + d - 1)
        elif facing == "east":
            anchor = (x0, y, z0)
        elif facing == "south":
            anchor = (x0 + w - 1, y, z0)
        else:
            anchor = (x0 + d - 1, y, z0 + w - 1)
        self.equipment.append((kind, *anchor, facing))

    def line(self, kind, a, b):
        """An axis-aligned run of pipe from a to b inclusive."""
        (x0, y0, z0), (x1, y1, z1) = a, b
        assert (x0 != x1) + (y0 != y1) + (z0 != z1) <= 1, (a, b)
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.lines[(x, y, z)] = kind

    def conduit(self, *points):
        """Circulating water conduit buried just under the ground (y -1) through points (x, z): flush concrete trench
        covers across open ground, hidden under roads and floors. Stops short of tower controllers (it reaches them)."""
        stop = {(x, y, z) for x, y, z, _ in self.towers}
        for (x0, z0), (x1, z1) in zip(points, points[1:]):
            self.line(TRENCH_KIND, (x0, -1, z0), (x1, -1, z1))
            self.pipes.append((min(x0, x1), min(z0, z1), max(x0, x1) + 1, max(z0, z1) + 1))
        for c in stop:
            self.lines.pop(c, None)

    def riser(self, x, z):
        """The conduit coming up through the floor (y 0) under a condenser, from the trench below."""
        self.line(TRENCH_KIND, (x, -1, z), (x, -1, z))
        self.line(CONDUIT, (x, 0, z), (x, 0, z))

    def gallery(self, x0, x1, z0, z1, y0, y1, legs):
        """An enclosed pipe bridge round exposed steam lines between buildings: concrete box with a window band, a roof,
        and columns down to the ground at (x, z) in legs."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if not edge:
                        continue
                    side = (z in (z0, z1)) and y0 < y < y1 and not (x in (x0, x1))
                    m = GLASS if side and y == y0 + 2 else CONC if y == y0 else TRIM if y == y1 else PANEL
                    self.decor[(x, y, z)] = m
        for lx, lz in legs:
            for y in range(0, y0):
                self.decor[(lx, y, lz)] = TRIM

    def tower(self, part, cx, z, kind):
        """A cooling tower part centred on x = cx, with its controller where the part has it (under the ground)."""
        w, _, d = part.size
        x0 = cx - w // 2
        self.add(part, x0, z)
        lx, ly, lz = part.controller
        self.towers.append((x0 + lx, ly - part.sink, z + lz, kind))

    def intake(self, ph, x0, z0):
        """The pump house's intake: a conduit leg down into the lake in each bay, joined by a header buried under its
        floor. Returns the header's middle (x, z) for the main conduit."""
        bays = ph.bays
        bw = ph.size[0] // bays
        legs = [x0 + i * bw + bw // 2 for i in range(bays)]
        for x in legs:
            self.line(CONDUIT, (x, -4, z0 + 3), (x, -1, z0 + 3))
            self.line(CONDUIT, (x, -1, z0 + 3), (x, -1, z0 + 6))
        self.line(TRENCH_KIND, (legs[0], -1, z0 + 7), (legs[-1], -1, z0 + 7))
        return legs[len(legs) // 2], z0 + 7


def rotate(g, rot):
    """Turns a part's grid by rot quarter turns clockwise seen from above (front +z faces -x, -z, +x after 1, 2, 3)."""
    for _ in range(rot % 4):
        g = np.rot90(g, k=1, axes=(0, 2))
    return g


def footprint(part, rot):
    w, _, d = part.size
    return (d, w) if rot % 2 else (w, d)


PIPE_IDS = {"coolant": "reactor_coolant_pipe", "steam": "insulated_steam_line", "conduit": "circulating_water_conduit",
            "trench": "conduit_trench", "bus": "isolated_phase_bus"}
COOLANT, STEAM, CONDUIT, TRENCH_KIND, BUS = "coolant", "steam", "conduit", "trench", "bus"
# id: (name, wide, deep, tall): the equipment table in tools/nuclear.py (the same as EquipmentKind.java).
from nuclear import EQUIPMENT as EQUIPMENT_SIZES  # noqa: E402


def four_loops(s, ox, oz, c, span, vessel="reactor_pressure_vessel", vw=7, floor=0):
    """A four-loop primary inside a containment whose local centre is (c, c), placed at (ox, oz) on the site.

    The vessel in the middle; a steam generator on each diagonal; coolant pipe from the vessel east and west with a
    branch to each steam generator; a coolant pump against the pipe beside each steam generator; the pressurizer on a
    pipe north of the vessel. The steam generators' tops join on a steam header one block above them (y 22), which
    leaves east at the vessel's row. floor: the site y of the containment's floor slab (-1 when it is level with the
    ground). Returns the site position where the steam header leaves (x, z)."""
    y = floor + 1
    cv = int(c)  # the vessel's middle cell row/column
    v0 = cv - vw // 2
    s.eq(vessel, ox + v0, oz + v0, y=y)
    g = [cv - span - 3, cv + span - 3]  # steam generator corners
    for gx in g:
        for gz in g:
            s.eq("reactor_steam_generator", ox + gx, oz + gz, y=y)
    west, east = v0 - 1, v0 + vw
    s.line(COOLANT, (ox + g[0], y, oz + cv), (ox + west, y, oz + cv))
    s.line(COOLANT, (ox + east, y, oz + cv), (ox + g[1] + 6, y, oz + cv))
    for gx in g:
        bx = gx + 3
        s.line(COOLANT, (ox + bx, y, oz + g[0] + 6), (ox + bx, y, oz + cv - 1))
        s.line(COOLANT, (ox + bx, y, oz + cv + 1), (ox + bx, y, oz + g[1]))
    for px in (west - 4, east):
        s.eq("reactor_coolant_pump", ox + px, oz + cv - 3, y=y)
        s.eq("reactor_coolant_pump", ox + px, oz + cv + 1, y=y)
    s.line(COOLANT, (ox + cv, y, oz + v0 - 1), (ox + cv, y, oz + v0 - 5))
    s.eq("pressurizer", ox + cv - 2, oz + v0 - 8, y=y)
    top = floor + 22
    a, b = g[0] + 3, g[1] + 3
    s.line(STEAM, (ox + a, top, oz + a), (ox + b, top, oz + a))
    s.line(STEAM, (ox + a, top, oz + b), (ox + b, top, oz + b))
    s.line(STEAM, (ox + b, top, oz + a), (ox + b, top, oz + b))
    return ox + b, oz + cv


def medium_plant():
    """The medium plant is designed building by building in tools/medium_plant.py."""
    import medium_plant as MP
    return MP.medium_site()


def large_plant():
    s = Site("large_epr", "Large Plant (EPR)", 440, 336, water=(0, -44, 440, 0))
    rb = s.add(containment("epr_containment", "Reactor Building (EPR, double wall)", 29, 52, 22, double=True), 148, 62)
    sx, sz = four_loops(s, 148, 62, 30, 11, vessel="epr_reactor_vessel", vw=8)
    for x, z in ((118, 66), (210, 66)):
        s.add(hall("safeguard_building", "Safeguard Building", 28, 52, 38, band=CONC, windows=((30, 32),)), x, z)
    s.add(hall("safeguard_pair", "Safeguard Buildings 2+3 (with control room)", 66, 26, 40, band=CONC,
               windows=((30, 32), (34, 36))), 144, 34)
    s.eq("reactor_control_console", 168, 49)
    s.add(hall("epr_turbine_hall", "Turbine Hall (EPR)", 124, 46, 42, windows=((30, 38),)), 220, 18)
    # Two turbine-generators side by side, each with its condenser and its transformer outside the end wall.
    for tz in (22, 46):
        s.eq("turbine_generator", 294, tz, "east")
        s.eq("condenser", 288, tz, "east")
        s.eq("main_transformer", 344, tz + 3, "east")
    # Steam: out of the reactor building north, then east along z 50 into the hall, down onto both turbines.
    s.line(STEAM, (sx, 22, sz), (sx, 22, 50))
    s.line(STEAM, (sx, 22, 50), (300, 22, 50))
    s.line(STEAM, (300, 22, 27), (300, 22, 50))
    s.line(STEAM, (300, 9, 50), (300, 22, 50))
    s.line(STEAM, (300, 9, 27), (300, 22, 27))
    s.line(STEAM, (230, 1, 50), (230, 22, 50))
    s.eq("feedwater_pump", 229, 51)
    s.add(hall("epr_fuel_building", "Fuel Building (EPR)", 44, 36, 40, wall=SHIELD, band=CONC, windows=()), 155, 124)
    s.add(hall("epr_aux_building", "Nuclear Auxiliary Building (EPR)", 46, 42, 30, band=CONC, windows=((20, 22),)), 96, 124)
    # The radwaste building (round 14: two layers of wall, an airlock) where the waste treatment hall stood.
    import radwaste_store
    radwaste_store.place(s, 54, 122)
    for x in (40, 244):
        s.add(hall("diesel_building_large", "Emergency Diesel Building", 36, 20, 16, band=CONC, windows=()), x, 76)
    s.add(hall("access_building", "Access and Office Building", 32, 22, 16, band=PANEL, windows=((5, 7), (11, 13))), 60, 34)
    s.add(transformer_yard("epr_transformer_yard", "Main Transformer Yard", 22, 46), 344, 18)
    s.add(switchyard("switchyard_large", "Switchyard", 70, 64, 6), 362, 74)
    ph = s.add(pump_house("pump_house_large", "Intake Pump House", 36, 28, 14, 5), 182, -8)
    hx, hz = s.intake(ph, 182, -8)
    t = cooling_tower("ct_natural_large", "Large Natural-Draft Cooling Tower", 62, 175, 37, 135, "large")
    for cx in (110, 300):
        s.tower(t, cx, 196, "large")
    s.conduit((hx, hz), (hx, 40), (291, 40), (291, 22))
    s.conduit((291, 40), (291, 180), (110, 180), (110, 261))
    s.conduit((291, 180), (300, 180), (300, 261))
    s.riser(291, 27)
    s.riser(291, 51)
    s.gallery(210, 219, 48, 52, 20, 24, [(211, 48), (211, 52), (218, 48), (218, 52)])
    s.roads += [(0, 176, 440, 184), (30, 0, 36, 184), (346, 0, 352, 16), (352, 64, 358, 184)]
    return s


def small_plant():
    s = Site("small_modular", "Small Modular Plant", 270, 196, water=(0, -36, 270, 0))
    s.add(smr_reactor_building("smr_reactor_building", "Modular Reactor Building", 6), 30, 36)
    s.add(hall("smr_turbine_building", "Turbine Building (6 units)", 96, 30, 20, band=PANEL, windows=((12, 15),)), 30, 98)
    # Six modules in the pool, each with its own steam line south to its own small turbine-generator.
    for i in range(6):
        mx = 38 + 13 * i
        s.eq("small_modular_reactor", mx, 46)
        s.eq("small_turbine_generator", mx + 1, 104)
        s.line(STEAM, (mx + 2, 18, 51), (mx + 2, 18, 103))
        s.line(STEAM, (mx + 2, 1, 103), (mx + 2, 17, 103))
        s.eq("reactor_control_console", 134 + 4 * i, 51)
    s.add(hall("smr_control_building", "Control Building", 28, 24, 16, band=PANEL, windows=((5, 7), (11, 13))), 132, 40)
    s.add(hall("radwaste_treatment", "Radwaste Treatment Building", 30, 22, 14, band=CONC, windows=()), 132, 68)
    # The radwaste building (round 14: two layers of wall, an airlock) across the road from the treatment building.
    import radwaste_store
    radwaste_store.place(s, 164, 50)
    bank = mech_tower_bank("ct_mechanical", "Mechanical-Draft Cooling Tower (6 cells)", 6)
    for z in (140, 166):
        s.tower(bank, 78, z, "mechanical")
    s.add(switchyard("switchyard_small", "Switchyard", 60, 44, 4), 190, 98)
    s.add(transformer_yard("smr_transformer_yard", "Transformer Yard", 22, 30), 166, 98)
    ph = s.add(pump_house("pump_house_small", "Intake Pump House", 22, 22, 10, 3), 160, -8)
    hx, hz = s.intake(ph, 160, -8)
    # Under every small turbine (each is its own condenser), then on to both tower banks.
    s.conduit((hx, hz), (hx, 106), (39, 106))
    s.conduit((78, 106), (78, 150))
    s.conduit((79, 106), (79, 176))
    for i in range(6):
        s.riser(38 + 13 * i + 2, 106)
    s.gallery(37, 108, 88, 97, 16, 20, [(37, 88), (37, 97), (72, 88), (72, 97), (108, 88), (108, 97)])
    s.roads += [(0, 88, 270, 94), (150, 0, 156, 196)]
    return s


def transformer_yard(key, name, w, d):
    p = Part(key, name, w, 10, d)
    p.box(0, 0, 0, w - 1, 0, d - 1, GRAVEL)
    for z in range(12, d - 2, 12):
        p.box(1, 1, z, w - 2, 7, z, CONC)  # firewalls between transformers
    p.box(0, 1, 0, 0, 2, d - 1, BARS)
    p.box(w - 1, 1, 0, w - 1, 2, d - 1, BARS)
    return p


def smr_reactor_building(key, name, modules):
    """The modules stand in one pool, under water to their tops as in the real plant (the water is their shielding),
    in a building of shielding concrete, roof included (round 14: radiation reached outside through clad walls). The
    door is behind a shield wall, so no line runs from a module to the open."""
    w, d, h = 16 + modules * 13, 52, 28
    p = hall(key, name, w, d, h, wall=SHIELD, roof=SHIELD, band=SHIELD, pilaster=SHIELD, windows=())
    p.box(0, h - 6, 0, w - 1, h - 5, d - 1, BLUE)
    p.clear(1, h - 6, 1, w - 2, h - 5, d - 2)
    # The reactor pool: shielding concrete walls, water to just over the modules' tops (they are 22 tall from y 1).
    p.shell(4, 1, 6, w - 5, 24, 24, SHIELD)
    p.clear(5, 1, 7, w - 6, 24, 23)
    p.box(5, 1, 7, w - 6, 23, 23, WATER)
    # A shield wall inside the door.
    cx = w // 2
    p.box(cx - 5, 1, d - 5, cx + 4, 6, d - 5, SHIELD)
    p.box(w - 14, h + 2 - 2, 10, w - 4, h + 2 - 1, 22, GREY)  # crane house
    return p


PLANTS = [small_plant, medium_plant, large_plant]


# ---------------------------------------------------------------------------------------------------------------
# Assembly and drawing


# Site grid offsets: the intake legs go this far below the ground, and the pump house reaches this far over the lake.
BELOW, OVER = 4, 10
# Render-only materials for what the plant blueprint adds (the parts never use them).
EQ_RENDER, COOLANT_RENDER, STEAM_RENDER, CONDUIT_RENDER, CONTROLLER_RENDER = 30, 31, 32, 33, 34
MATERIALS.update({EQ_RENDER: ("", (170, 176, 182)), COOLANT_RENDER: ("", (190, 196, 200)), STEAM_RENDER: ("", (236, 236, 230)),
                  CONDUIT_RENDER: ("", (90, 96, 104)), CONTROLLER_RENDER: ("", (60, 110, 190))})
PIPE_RENDER = {COOLANT: COOLANT_RENDER, STEAM: STEAM_RENDER, CONDUIT: CONDUIT_RENDER, BUS: COOLANT_RENDER}
DIRS = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}


def equipment_solid(kind, x, y, z):
    """EquipmentKind.solid: which local cells of a piece are its body (same rules as the Java)."""
    _, w, d, h = EQUIPMENT_SIZES[kind]
    cx, cz = x + 0.5 - w / 2, z + 0.5 - d / 2
    r2 = cx * cx + cz * cz
    if kind == "reactor_pressure_vessel":
        return r2 <= 3.3 ** 2 if y < 14 else r2 <= 2.4 ** 2
    if kind == "reactor_steam_generator":
        return r2 <= 2.5 ** 2 if y < 10 else r2 <= 3.3 ** 2
    if kind == "reactor_coolant_pump":
        return y < 4 or (y < 9 and z >= 1)
    if kind == "pressurizer":
        return cx * cx <= 1.6 ** 2
    if kind == "turbine_generator":
        return y < 3 or (2 <= x <= 8 and y < 8)
    if kind == "main_transformer":
        return y < 5 and x < 5 and z < 5
    if kind == "epr_reactor_vessel":
        return r2 <= 3.8 ** 2 if y < 15 else r2 <= 2.8 ** 2
    if kind == "small_modular_reactor":
        return r2 <= 2.4 ** 2
    if kind == "emergency_diesel_generator":
        if 1 <= x <= 5:
            return y < 5 and (z < 13 or (2 <= x <= 4 and y < 4))
        return (1 <= z <= 8 and y <= 1) or (z == 9 and y <= 1) or (z in (10, 11) and y == 0) or (x == 0 and z == 0 and y <= 2)
    return True


def equipment_cells(kind, ax, ay, az, facing):
    """The body of a placed piece in site coordinates (EquipmentKind.toWorld), its anchor included."""
    _, w, d, h = EQUIPMENT_SIZES[kind]
    fx, fz = DIRS[facing]
    rx, rz = DIRS[CLOCKWISE[facing]]
    out = {(ax, ay, az)}
    for ly in range(h):
        for lz in range(d):
            for lx in range(w):
                if equipment_solid(kind, lx, ly, lz):
                    out.add((ax + rx * lx + fx * lz, ay + ly, az + rz * lx + fz * lz))
    return out


def assemble(site, render=True):
    """The whole site as one grid: g[x, y + BELOW, z + OVER]. Roads, then the parts, then the plant's own blocks:
    equipment bodies are cut out of whatever building they stand in, and pipes run through walls. With render, the
    equipment, pipes and controllers get their preview colours; without, the grid only holds the parts and roads."""
    top = max(p.size[1] for p, *_ in site.parts) + 2
    g = np.zeros((site.w, top + BELOW, site.d + OVER), np.uint16)
    for x0, z0, x1, z1 in site.roads:
        g[max(0, x0):min(site.w, x1), BELOW - 1, max(0, z0) + OVER:min(site.d, z1) + OVER] = ROAD
    for part, x, z, rot in site.parts:
        pg = rotate(part.g, rot)
        w, h, d = pg.shape
        y0 = BELOW - part.sink
        sub = g[x:x + w, y0:y0 + h, z + OVER:z + OVER + d]
        np.copyto(sub, pg[:sub.shape[0], :, :sub.shape[2]], where=pg[:sub.shape[0], :, :sub.shape[2]] > 0)
    for (x, y, z), m in site.decor.items():
        g[x, y + BELOW, z + OVER] = m
    if not render:
        return g
    for kind, ax, ay, az, facing in site.equipment:
        for (x, y, z) in equipment_cells(kind, ax, ay, az, facing):
            g[x, y + BELOW, z + OVER] = EQ_RENDER
    for (x, y, z), kind in site.lines.items():
        g[x, y + BELOW, z + OVER] = TRENCH if kind == TRENCH_KIND else PIPE_RENDER[kind]
    for x, y, z, kind in site.towers:
        g[x, y + BELOW, z + OVER] = CONTROLLER_RENDER
    return g


# ---------------------------------------------------------------------------------------------------------------
# Checking that each plant works (the same joining rules as ReactorUnit.java), before anything is built in game


def verify(site):
    """Asserts every reactor forms a unit: no two bodies or pipes overlap, and following the pipes (and touching
    equipment) the way ReactorUnit.java does finds everything a working unit needs; the condensers' conduit reaches
    water (the lake) and the cooling tower controllers. Returns a short report."""
    bodies = {}
    owner = {}
    for i, (kind, ax, ay, az, facing) in enumerate(site.equipment):
        cells = equipment_cells(kind, ax, ay, az, facing)
        bodies[i] = cells
        for c in cells:
            assert c not in owner, f"{site.key}: {kind} at {(ax, ay, az)} overlaps {site.equipment[owner[c]][0]} at {c}"
            assert c not in site.lines, f"{site.key}: {kind} at {(ax, ay, az)} overlaps a {site.lines[c]} pipe at {c}"
            owner[c] = i
    for x, y, z, kind in site.towers:
        assert (x, y, z) not in owner and (x, y, z) not in site.lines, f"{site.key}: tower controller at {(x, y, z)} is covered"
    # Decor props standing in the buildings must not be in the way of the plant's own equipment or pipes.
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            for (x, y, z) in equipment_cells(kind, ax, ay, az, facing):
                c = (x + px, y - part.sink, z + pz)
                assert c not in owner, f"{site.key}: {part.key}'s {kind} overlaps {site.equipment[owner[c]][0]} at {c}"
                assert c not in site.lines, f"{site.key}: {part.key}'s {kind} overlaps a {site.lines[c]} pipe at {c}"
    kinds = {i: e[0] for i, e in enumerate(site.equipment)}
    takes = {COOLANT: {"reactor_pressure_vessel", "epr_reactor_vessel", "reactor_steam_generator", "reactor_coolant_pump", "pressurizer"},
             STEAM: {"reactor_steam_generator", "turbine_generator", "feedwater_pump", "condenser", "small_modular_reactor",
                     "small_turbine_generator"},
             CONDUIT: {"condenser", "small_turbine_generator", "circulating_water_pump"},
             BUS: {"turbine_generator", "main_transformer"}}
    near = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    tower_cells = {(x, y, z) for x, y, z, _ in site.towers}
    lines = {c: (CONDUIT if k == TRENCH_KIND else k) for c, k in site.lines.items()}

    def outside(i):
        out = set()
        for (x, y, z) in bodies[i]:
            for dx, dy, dz in near:
                c = (x + dx, y + dy, z + dz)
                if c not in bodies[i]:
                    out.add(c)
        return out

    def touching(i):
        return {owner[c] for c in outside(i) if c in owner and owner[c] != i}

    def joined(start, kind):
        found, seen, queue = set(), set(), []
        for i in start:
            for c in outside(i):
                if lines.get(c) == kind and c not in seen:
                    seen.add(c)
                    queue.append(c)
            found |= {j for j in touching(i) if kinds[j] in takes[kind]}
        towers, water = set(), False
        while queue:
            x, y, z = queue.pop()
            for dx, dy, dz in near:
                c = (x + dx, y + dy, z + dz)
                if lines.get(c) == kind:
                    if c not in seen:
                        seen.add(c)
                        queue.append(c)
                elif c in owner and kinds[owner[c]] in takes[kind]:
                    found.add(owner[c])
                elif c in tower_cells:
                    towers.add(c)
                elif kind == CONDUIT and c[1] < 0 and c[2] < 0:
                    water = True  # a leg below the ground, out in the lake
        return found - set(start), towers, water

    report = []
    for i, kind in kinds.items():
        if kind not in ("reactor_pressure_vessel", "epr_reactor_vessel", "small_modular_reactor"):
            continue
        if kind == "small_modular_reactor":
            steam, _, _ = joined([i], STEAM)
            turbines = [j for j in steam if kinds[j] == "small_turbine_generator"]
            assert turbines, f"{site.key}: module {i} has no small turbine on its steam line"
            coolers = turbines
            report.append(f"module {i}: {len(turbines)} small turbine")
        else:
            primary, _, _ = joined([i], COOLANT)
            sgs = [j for j in primary if kinds[j] == "reactor_steam_generator"]
            pumps = [j for j in primary if kinds[j] == "reactor_coolant_pump"]
            press = [j for j in primary if kinds[j] == "pressurizer"]
            assert len(sgs) == 4 and len(pumps) == 4 and press, f"{site.key}: primary has {len(sgs)} SG, {len(pumps)} pumps, {len(press)} pressurizer"
            secondary, _, _ = joined(sgs, STEAM)
            turbines = [j for j in secondary if kinds[j] == "turbine_generator"]
            feed = [j for j in secondary if kinds[j] == "feedwater_pump"]
            coolers = {j for j in secondary if kinds[j] == "condenser"}
            transformers = set()
            for t in turbines:
                for j in touching(t):
                    if kinds[j] == "condenser":
                        coolers.add(j)
                    if kinds[j] == "main_transformer":
                        transformers.add(j)
            bus, _, _ = joined(turbines, BUS)
            transformers |= {j for j in bus if kinds[j] == "main_transformer"}
            assert turbines and feed and coolers and transformers, f"{site.key}: secondary {len(turbines)} turbines, {len(feed)} feedwater, {len(coolers)} condensers, {len(transformers)} transformers"
            want = 2 if kind == "epr_reactor_vessel" else 1
            assert len(turbines) >= want and len(transformers) >= want, f"{site.key}: needs {want} turbines with transformers"
            report.append(f"{kind}: 4 loops, {len(turbines)} turbine(s), {len(coolers)} condenser(s), {len(transformers)} transformer(s)")
        towers = set()
        pumps = set()
        for c in coolers:
            found, t, water = joined([c], CONDUIT)
            assert water, f"{site.key}: condenser {c} conduit does not reach the lake"
            towers |= t
            pumps |= {j for j in found if kinds[j] == "circulating_water_pump"}
        if any(k == "circulating_water_pump" for k in kinds.values()):
            assert pumps, f"{site.key}: no circulating water pump on the conduit"
        assert towers, f"{site.key}: no cooling tower on the conduit"
        report[-1] += f", {len(towers)} cooling tower(s), {len(pumps)} CW pump(s) and the lake on its conduit"
    return report


# ---------------------------------------------------------------------------------------------------------------
# Blueprints (format 2: run-length rows; a plant lists its parts and adds its own blocks on top)

BLUEPRINT_OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "data" / "jugcraft" / "blueprint"
KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&*+-/:;<=>?@^_|~" + "".join(chr(c) for c in range(0x100, 0x250))
# (More block kinds than ASCII keys: Latin Extended-A letters, which are never digits.)
TURN180 = {"north": "south", "south": "north", "east": "west", "west": "east"}


def rle_layers(cells, size):
    """{(x, y, z): state} to format 2 layers; returns (palette, layers)."""
    states = sorted(set(cells.values()))
    assert len(states) <= len(KEYS), "too many block kinds"
    key = {st: KEYS[i] for i, st in enumerate(states)}
    sx, sy, sz = size
    rows_by = {}
    for (x, y, z), st in cells.items():
        rows_by.setdefault((y, z), {})[x] = key[st]
    layers = []
    for y in range(sy):
        rows = []
        for z in range(sz):
            row = rows_by.get((y, z))
            if not row:
                rows.append("")
                continue
            out, x, last = [], 0, max(row)
            while x <= last:
                ch = row.get(x, ".")
                n = 1
                while x + n <= last and row.get(x + n, ".") == ch:
                    n += 1
                out.append(ch if n == 1 else f"{n}{ch}")
                x += n
            rows.append("".join(out))
        layers.append(rows)
    return {k: st for st, k in key.items()}, layers


def part_cells(part):
    """A part's blocks as {(x, y, z): state}, in its own frame."""
    cells = {}
    xs, ys, zs = np.nonzero(part.g)
    for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
        cells[(x, y, z)] = MATERIALS[int(part.g[x, y, z])][0]
    cells.update(part.extras)
    return cells


def part_blueprint(part):
    w, h, d = part.size
    palette, layers = rle_layers(part_cells(part), (w, h, d))
    return {"format": 2, "name": part.name, "author": "Jugcraft (built in)", "kind": "part",
            "description": f"One part of a pre-made nuclear plant: {part.name}, {w} x {d}, {h} tall.",
            "size": [w, h, d], "anchor": [w // 2, part.sink, d + 1], "palette": palette, "layers": layers}


def plant_blueprint(site, name):
    """The whole plant: its parts turned half round (so the lake is in front, where the stake goes on the shore and
    the plant reaches inland away from the player), and on top the roads, equipment, pipes and conduits, and air
    where an equipment body cuts through a building."""
    W, D = site.w, site.d
    built = assemble(site, render=False)
    H = built.shape[1]

    def to_file(x, y, z):
        return W - 1 - x, y + BELOW, D - 1 - z

    extra = {}
    import re
    for (x, y, z), m in site.decor.items():
        # The file is the site turned round, so facings turn too.
        extra[to_file(x, y, z)] = re.sub(r"facing=(north|south|east|west)", lambda f: "facing=" + TURN180[f.group(1)], MATERIALS[m][0])
    for x0, z0, x1, z1 in site.roads:
        for x in range(max(0, x0), min(W, x1)):
            for z in range(max(0, z0), min(D, z1)):
                if built[x, BELOW - 1, z + OVER] == ROAD:
                    extra[to_file(x, -1, z)] = MATERIALS[ROAD][0]
    for kind, ax, ay, az, facing in site.equipment:
        for (x, y, z) in equipment_cells(kind, ax, ay, az, facing):
            if built[x, y + BELOW, z + OVER]:
                extra[to_file(x, y, z)] = "minecraft:air"
        extra[to_file(ax, ay, az)] = f"jugcraft:{kind}[facing={TURN180[facing]}]"
    for (x, y, z), kind in site.lines.items():
        extra[to_file(x, y, z)] = f"jugcraft:{PIPE_IDS[kind]}"
    parts = []
    for part, x, z, rot in site.parts:
        assert rot == 0
        w, h, d = part.size
        parts.append({"id": part.key, "at": [W - x - w, BELOW - part.sink, D - z - d], "turn": 2})
    size = [W, H, D + OVER]
    palette, layers = rle_layers(extra, size)
    # The stake on the shore, near the middle, on a free spot of the ground layer.
    occupied = {(x, z) for (x, y, z) in extra if y == BELOW}
    shore = D - 1
    for dx in range(W // 2):
        for fx in (W // 2 + dx, W // 2 - dx):
            sx_, sz_ = W - 1 - fx, 0  # site x of this file column, on the shore row
            if (fx, shore) not in occupied and built[sx_, BELOW:, OVER].max() == 0:
                anchor = [fx, BELOW, shore]
                break
        else:
            continue
        break
    return {"format": 2, "name": name, "author": "Jugcraft (built in)", "kind": "set",
            "description": f"{site.name}: every building, the reactor equipment, pipes and conduits. Stake it on a lake shore "
                           f"facing inland; the intake reaches {OVER} blocks out over the water.",
            "size": size, "anchor": anchor, "palette": palette, "parts": parts, "layers": layers}


PLANT_NAMES = {"small_modular": "Small Modular Nuclear Plant", "medium_pwr": "Medium Nuclear Plant (4-loop PWR)",
               "large_epr": "Large Nuclear Plant (EPR)"}


def blueprint_id(key):
    return f"nuclear_{key}"


def write_blueprints(out=BLUEPRINT_OUT):
    """Writes every part (green) and plant (blue) blueprint, and large.txt listing them (each side reads them from its
    own jar; they are too big to send). Returns the ids."""
    out.mkdir(parents=True, exist_ok=True)
    sites = [f() for f in PLANTS]
    ids = []
    seen = set()
    for site in sites:
        for part, *_ in site.parts:
            if part.key in seen:
                continue
            seen.add(part.key)
            (out / f"{part.key}.jugbp.json").write_text(json.dumps(part_blueprint(part), separators=(",", ":")) + "\n", encoding="utf-8")
            ids.append(part.key)
    for site in sites:
        verify(site)
        pid = blueprint_id(site.key)
        (out / f"{pid}.jugbp.json").write_text(json.dumps(plant_blueprint(site, PLANT_NAMES[site.key]), separators=(",", ":")) + "\n",
                                               encoding="utf-8")
        ids.append(pid)
    (out / "large.txt").write_text("\n".join(ids) + "\n", encoding="utf-8")
    return ids


def iso(g, scale=4, light=(1.0, 0.82, 0.64), sky=(0, 0, 0, 0), ground=None, water_edge=None, plumes=(), ground_y=0):
    """Draws a voxel grid seen from the south-east, above. Returns an RGBA image."""
    W, H, D = g.shape
    c = math.cos(math.radians(30))
    width = int((W + D) * c * scale) + 8
    height = int((W + D) * 0.5 * scale + H * scale) + 8
    ox = D * c * scale + 4
    oy = H * scale + 4

    def P(x, y, z):
        return (ox + (x - z) * c * scale, oy + (x + z) * 0.5 * scale - y * scale)

    img = Image.new("RGBA", (width, height), sky)
    dr = ImageDraw.Draw(img)
    if ground:
        gy = ground_y
        dr.polygon([P(-30, gy, -30), P(W + 30, gy, -30), P(W + 30, gy, D + 30), P(-30, gy, D + 30)], fill=ground)
        if water_edge:
            x0, z0, x1, z1 = water_edge
            dr.polygon([P(x0 - 30, gy - 1, z0), P(x1 + 30, gy - 1, z0), P(x1 + 30, gy - 1, z1), P(x0 - 30, gy - 1, z1)], fill=(52, 92, 158))
    occ = g > 0
    pad = np.pad(occ, ((0, 1), (0, 1), (0, 1)))
    top = occ & ~pad[:-1, 1:, :-1]
    east = occ & ~pad[1:, :-1, :-1]
    south = occ & ~pad[:-1, :-1, 1:]
    vis = top | east | south
    xs, ys, zs = np.nonzero(vis)
    order = np.lexsort((ys, xs + zs + ys))
    rng = np.random.default_rng(3)
    jitter = rng.integers(-6, 7, size=len(order))
    cols = {k: np.array(v[1]) for k, v in MATERIALS.items()}
    for n, i in enumerate(order):
        x, y, z = int(xs[i]), int(ys[i]), int(zs[i])
        base = cols[int(g[x, y, z])] + jitter[n]
        if top[x, y, z]:
            f = tuple(int(v) for v in np.clip(base * light[0], 0, 255))
            dr.polygon([P(x, y + 1, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], fill=f)
        if east[x, y, z]:
            f = tuple(int(v) for v in np.clip(base * light[1], 0, 255))
            dr.polygon([P(x + 1, y, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x + 1, y, z + 1)], fill=f)
        if south[x, y, z]:
            f = tuple(int(v) for v in np.clip(base * light[2], 0, 255))
            dr.polygon([P(x, y, z + 1), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], fill=f)
    if plumes:
        from PIL import ImageChops
        alpha = Image.new("L", img.size, 0)
        prng = np.random.default_rng(7)
        for (px, py, pz, r) in plumes:
            for k in range(18):
                for _ in range(3):
                    jx, jy = prng.normal(0, r * 0.25, 2)
                    cx, cy = P(px - k * 1.6 + jx, py + 4 + k * r * 0.22, pz - k * 0.8)
                    rr = (r * (0.55 + k * 0.07)) * scale * prng.uniform(0.7, 1.0)
                    blob = Image.new("L", img.size, 0)
                    ImageDraw.Draw(blob).ellipse([cx + jy - rr, cy - rr * 0.75, cx + jy + rr, cy + rr * 0.75],
                                                 fill=int(225 * (1 - k / 19) ** 0.7))
                    alpha = ImageChops.lighter(alpha, blob)
        alpha = alpha.filter(ImageFilter.GaussianBlur(scale * 2.5))
        layer = Image.new("RGBA", img.size, (246, 247, 249, 0))
        layer.putalpha(alpha)
        img = Image.alpha_composite(img, layer)
    return img


def font(size, bold=False):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    try:
        return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/{name}", size)
    except OSError:
        return ImageFont.load_default()


def plan(site, g, scale=3):
    """A top-down site plan: shaded roof heights, every part outlined and labelled with its size."""
    g = g[:, BELOW:, :]
    W, H, D = g.shape
    D -= OVER
    m = 90
    wz = -site.water[1]
    img = Image.new("RGB", (W * scale + 2 * m + 420, max((D + wz) * scale + 2 * m + 60, 120 + wz * scale + 44 * len(site.parts) + 60)), (236, 234, 228))
    dr = ImageDraw.Draw(img)
    oz = m + wz * scale
    dr.rectangle([m, m, m + W * scale, oz], fill=(70, 110, 170))
    dr.text((m + 8, m + 6), "WATER (lake or river: the intake draws from here)", fill=(230, 240, 255), font=font(14, True))
    dr.rectangle([m, oz, m + W * scale, oz + D * scale], fill=(132, 160, 102))
    occ = g > 0
    heights = np.where(occ.any(axis=1), H - 1 - np.argmax(occ[:, ::-1, :], axis=1), -1)
    cols = np.array([MATERIALS.get(i, ("", (0, 0, 0)))[1] for i in range(max(MATERIALS) + 1)])
    topm = np.take_along_axis(g, np.clip(heights, 0, None)[:, None, :], axis=1)[:, 0, :]
    rgb = cols[topm].astype(float)
    gx = np.gradient(heights.astype(float), axis=0)
    gz = np.gradient(heights.astype(float), axis=1)
    shade = np.clip(1 - 0.05 * (gx + gz), 0.6, 1.25)[..., None]
    shade *= (0.82 + 0.18 * np.clip(heights, 0, 150)[..., None] / 150)
    rgb = np.clip(rgb * shade, 0, 255).astype(np.uint8)
    mask = heights >= 0
    arr = np.array(img)
    big = np.repeat(np.repeat(rgb.transpose(1, 0, 2), scale, 0), scale, 1)
    bm = np.repeat(np.repeat(mask.T, scale, 0), scale, 1)
    region = arr[oz - OVER * scale:oz + D * scale, m:m + W * scale]
    region[bm] = big[bm]
    img = Image.fromarray(arr)
    dr = ImageDraw.Draw(img)
    for x0, z0, x1, z1 in site.pipes:
        dr.rectangle([m + x0 * scale, oz + z0 * scale, m + x1 * scale, oz + z1 * scale], outline=(40, 90, 200), width=2)
    # Chunk grid.
    for x in range(0, W + 1, 16):
        dr.line([m + x * scale, oz, m + x * scale, oz + D * scale], fill=(116, 142, 92), width=1)
    labels = []
    for part, x, z, rot in site.parts:
        fw, fd = footprint(part, rot)
        w, h, d = part.size
        box = [m + x * scale, oz + z * scale, m + (x + fw) * scale - 1, oz + (z + fd) * scale - 1]
        col = (40, 160, 70)
        dr.rectangle(box, outline=col, width=2)
        labels.append((part, box, w, h, d))
    for kind, ax, ay, az, facing in site.equipment:
        cells = equipment_cells(kind, ax, ay, az, facing)
        x0, x1 = min(c[0] for c in cells), max(c[0] for c in cells) + 1
        z0, z1 = min(c[2] for c in cells), max(c[2] for c in cells) + 1
        dr.rectangle([m + x0 * scale, oz + z0 * scale, m + x1 * scale - 1, oz + z1 * scale - 1], outline=(230, 120, 20), width=2)
    lx = m + W * scale + 30
    dr.text((lx, oz - 30), "Parts (each one a green blueprint)", fill=(20, 20, 20), font=font(15, True))
    for n, (part, box, w, h, d) in enumerate(labels, 1):
        cx = (box[0] + box[2]) / 2
        cy = (box[1] + box[3]) / 2
        dr.ellipse([cx - 13, cy - 13, cx + 13, cy + 13], fill=(255, 255, 255), outline=(40, 150, 60), width=3)
        t = str(n)
        dr.text((cx - dr.textlength(t, font=font(14, True)) / 2, cy - 9), t, fill=(20, 20, 20), font=font(14, True))
        tall = h - 2 if part.kind == "building" else h
        y = oz + (n - 1) * 44
        dr.ellipse([lx, y, lx + 24, y + 24], fill=(255, 255, 255), outline=(40, 150, 60), width=3)
        dr.text((lx + 12 - dr.textlength(t, font=font(13, True)) / 2, y + 4), t, fill=(20, 20, 20), font=font(13, True))
        dr.text((lx + 34, y), part.name, fill=(20, 20, 20), font=font(14, True))
        dr.text((lx + 34, y + 19), f"{w} x {d} blocks, {tall} tall, {part.blocks():,} blocks", fill=(70, 70, 70), font=font(12))
    # Title, scale bar, compass.
    dr.text((m, 14), f"{site.name}: site plan", fill=(20, 20, 20), font=font(24, True))
    dr.text((m, 44), f"Site {W} x {D} blocks (1 block = 1 m). Green outlines: parts that are also green blueprints. "
                     f"Orange: equipment placed from items.",
            fill=(60, 60, 60), font=font(13))
    dr.text((m, 60), "Blue lines: buried circulating-water pipe (Water Conduit). Faint lines: chunk borders (16 blocks).",
            fill=(60, 60, 60), font=font(13))
    by = oz + D * scale + 24
    dr.rectangle([m, by, m + 50 * scale, by + 8], fill=(20, 20, 20))
    dr.rectangle([m + 25 * scale, by, m + 50 * scale, by + 8], fill=(240, 240, 240), outline=(20, 20, 20))
    dr.text((m + 50 * scale + 8, by - 4), "50 blocks", fill=(20, 20, 20), font=font(13))
    nx = m + W * scale - 30
    dr.polygon([(nx, by - 16), (nx - 8, by + 8), (nx + 8, by + 8)], fill=(20, 20, 20))
    dr.text((nx - 5, by + 10), "N", fill=(20, 20, 20), font=font(14, True))
    return img


def plume_spots(site):
    """Plume puffs in grid coordinates (x, y, z, radius)."""
    out = []
    for part, x, z, rot in site.parts:
        if part.key.startswith("ct_natural"):
            w, h, d = part.size
            out.append((x + w / 2, h + BELOW, z + OVER + d / 2, w * 0.22))
        elif part.key == "ct_mechanical":
            w, h, d = part.size
            for i in range(6):
                out.append((x + i * 16 + 8, h - 2 + BELOW, z + OVER + d / 2, 5))
    return out


def render_site(site, out, scale=3):
    g = assemble(site)
    img = iso(g, scale=scale, sky=(198, 214, 228, 255), ground=(124, 152, 96), water_edge=(0, site.water[1] + OVER, site.w, OVER),
              plumes=plume_spots(site), ground_y=BELOW)
    img = img.convert("RGB")
    dr = ImageDraw.Draw(img)
    dr.text((20, 16), f"{site.name}: seen from the land side (south-east)", fill=(20, 20, 20), font=font(28, True))
    img.save(out / f"{site.key}_3d_land.png")
    plan(site, g).save(out / f"{site.key}_plan.png")
    # From the north-west: the same site turned half a turn.
    g2 = g[::-1, :, ::-1]
    D2 = g.shape[2]
    img = iso(g2, scale=scale, sky=(198, 214, 228, 255), ground=(124, 152, 96),
              water_edge=(0, D2 - OVER, site.w, D2 - OVER - site.water[1]), ground_y=BELOW,
              plumes=[(site.w - x, y, D2 - z, r) for x, y, z, r in plume_spots(site)]).convert("RGB")
    ImageDraw.Draw(img).text((20, 16), f"{site.name}: full size, seen from the water side (north-west)", fill=(20, 20, 20), font=font(28, True))
    img.save(out / f"{site.key}_3d.png")
    return g


def catalog(sites, out):
    """Every distinct part (one green blueprint each) drawn alone with its size and block count."""
    seen = {}
    for s in sites:
        for part, *_ in s.parts:
            seen.setdefault(part.key, (part, s.name))
    tiles = []
    for key, (part, plant) in seen.items():
        g = part.g
        sc = max(1, min(6, int(260 / max(g.shape[0] + g.shape[2], g.shape[1] * 1.2))))
        im = iso(g, scale=sc)
        bb = im.getbbox()
        im = im.crop(bb) if bb else im
        tiles.append((part, plant, im))
    cw, chh, cols = 340, 330, 5
    rows = math.ceil(len(tiles) / cols)
    sheet = Image.new("RGB", (cols * cw, rows * chh + 70), (240, 241, 236))
    dr = ImageDraw.Draw(sheet)
    dr.text((16, 14), "Green part blueprints (each also inside its plant's blue blueprint)", fill=(20, 20, 20), font=font(24, True))
    for i, (part, plant, im) in enumerate(tiles):
        cx, cy = (i % cols) * cw, (i // cols) * chh + 70
        im.thumbnail((cw - 20, chh - 80))
        sheet.paste(im, (cx + (cw - im.width) // 2, cy + (chh - 70 - im.height) // 2), im)
        w, h, d = part.size
        dr.rectangle([cx + 6, cy + chh - 64, cx + cw - 6, cy + chh - 8], fill=(214, 236, 214), outline=(40, 150, 60))
        dr.text((cx + 12, cy + chh - 60), part.name[:42], fill=(20, 20, 20), font=font(13, True))
        dr.text((cx + 12, cy + chh - 42), f"{w} x {d} x {h} tall, {part.blocks():,} blocks", fill=(40, 40, 40), font=font(12))
        dr.text((cx + 12, cy + chh - 26), f"id: {part.key}", fill=(80, 80, 80), font=font(11))
    sheet.save(out / "parts_catalog.png")
    return tiles


def main(out):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    sites = [f() for f in PLANTS]
    for s in sites:
        for line in verify(s):
            print("  ok:", line)
        g = render_site(s, out)
        total = int(np.count_nonzero(g))
        print(f"{s.name}: {s.w} x {s.d}, {g.shape[1]} tall, {total:,} blocks, {len(s.parts)} parts")
        for part, *_ in s.parts:
            print(f"   {part.key:26s} {str(part.size):18s} {part.blocks():>8,}")
    catalog(sites, out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "plant_previews")
