"""The medium plant's pump houses (Justin, 19 October 2026: brought to the relay-room detail standard, "real US layout",
"everything works"): the lake intake for make-up and service water, and the circulating water pump house by the towers.
Written one piece at a time, each approved on its own sheet (claude/jugcraft-pump-houses.md in the project).

Drawn in control_boards' ops (cooling_tower.Ops / Frame): x across the intake (west to east), y up from the grass, d out
toward the lake (north, -z). Box faces: out (toward the lake), in (toward the land), left (-x), right (+x), top, bottom.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import cooling_tower as CT

# ---- in game (towers and pump houses build, tools/water_works.py): the doors standing open (by name), and the
# controls the player clicks (name, world box), recorded as the pieces are drawn.
OPEN = set()
ZONES = []


def _zone(F, name, t0, t1, y0, y1, r0, r1):
    """Records a click zone: the box (t0..t1, y0..y1, r0..r1) of frame F, as a world box."""
    P = np.array([F.w(t, y, r) for t in (t0, t1) for y in (y0, y1) for r in (r0, r1)], float)
    ZONES.append((name, tuple(float(v) for v in P.min(0)) + tuple(float(v) for v in P.max(0))))


class _Moved:
    """Draws into frame G as if in frame F's coordinates moved so that (t0, r0) is G's origin: a door leaf swung about
    its hinge (G = F.sub(t0, r0, turn))."""

    def __init__(self, G, t0, r0):
        self.G, self.t0, self.r0, self.o = G, t0, r0, G.o

    def w(self, t, y, r):
        return self.G.w(t - self.t0, y, r - self.r0)

    def box(self, t0, t1, y0, y1, r0, r1, faces, lit=False):
        self.G.box(t0 - self.t0, t1 - self.t0, y0, y1, r0 - self.r0, r1 - self.r0, faces, lit=lit)

    def cyl(self, a, b, rad, mat, cap=None, sides=10, lit=False):
        self.G.cyl((a[0] - self.t0, a[1], a[2] - self.r0), (b[0] - self.t0, b[1], b[2] - self.r0), rad, mat, cap, sides, lit)

    def bar(self, a, b, w, th, faces, up="t", lit=False):
        self.G.bar((a[0] - self.t0, a[1], a[2] - self.r0), (b[0] - self.t0, b[1], b[2] - self.r0), w, th, faces, up=up, lit=lit)

    def _quad(self, P, mat, facing=None, lit=False):
        self.G._quad(P, mat, facing, lit)

# ================================================================== piece 1: the concrete intake
#
# Real-world basis: a shoreline intake for a closed-cycle (cooling tower) plant's make-up and service water (US
# practice; intake design after ANSI/HI 9.8 and the EPA 316(b) rules for low through-screen velocity). The structure
# stands out into the lake on piers: a curtain (skimmer) wall across its face reaching below low water keeps floating
# debris and ice out; behind it inclined bar trash racks, raked from the deck; stop-log slots before and after the
# travelling screens, so any bay can be closed off and pumped dry; the screen wells; then the pump bays, where the
# vertical wet-pit pumps hang from soleplates in the deck. The deck is a thick slab on the piers, its openings covered
# (grating over the rack slots, checker plate over the stop-log slots) or filled by the screens and pumps; it carries
# the crane rails for the rake and the gantry. Guardrails and a life ring where the deck stands over open water; a staff
# gauge on a pier nose shows the lake's level; the bays are numbered on the curtain wall. The bank either side is armoured
# with riprap.
#
# This intake: four bays 3.6 m wide on 0.6 m piers (17.4 m across, 0.8 m end walls), 16.6 m from the curtain wall to
# the back wall; deck top 1 m above the grass, its slab 0.6 m deep; lake at -1.0 (normal), bay floors at -7.0. Bays: 1
# make-up A, 2 service water train A, 3 service water train B, 4 make-up B.

BAY_W, PIER, END = 3.6, 0.6, 0.8
NB = 4
DECK, SLAB = 1.0, 0.6
FLOOR = -7.0
D_FACE, D_BACK = 10.0, -6.6                # the curtain wall's face (out over the lake), the back wall's back
LAKE = -1.0

# Along d (from the lake in): the curtain wall, the trash rack slot, stop-log slot 1, the screen well, stop-log slot 2,
# the pump bay.
CURTAIN = (9.4, 10.0)
RACK_SLOT = (8.5, 9.3)
LOG1 = (7.2, 7.7)
SCREEN = (3.8, 6.4)
LOG2 = (2.7, 3.2)
PUMP_D = (-1.4, 0.2)                       # the soleplate openings, centred on d -0.6
BAYS = ["MU-A", "SW-A", "SW-B", "MU-B"]


def bay_x(i):
    """Bay i's clear width (x0, x1)."""
    x0 = END + i * (BAY_W + PIER)
    return x0, x0 + BAY_W


def pump_holes(i):
    """Bay i's soleplate openings (x0, x1): one big make-up pump, or two service water pumps."""
    x0, _ = bay_x(i)
    pumps = [x0 + BAY_W / 2] if BAYS[i].startswith("MU") else [x0 + BAY_W * 0.27, x0 + BAY_W * 0.73]
    return [(c - 0.65, c + 0.65) for c in pumps]


WASH_X = lambda i: (bay_x(i)[0] + 0.15, bay_x(i)[0] + 0.95) if i == 0 else (bay_x(i)[1] - 0.95, bay_x(i)[1] - 0.15)
WASH_D = (0.45, 1.25)                     # (inside the pump hall, piece 5)


def intake_ops(ox=0.0, oz=0.0, level=LAKE, logs=None, cut=False, installed=()):
    """The concrete intake (piece 1), its origin (x 0, grass, the curtain wall's line - 10 m) at world (ox, 0, oz).
    level: the lake's level (live). logs: a bay with its stop logs dropped into the first slot and pumped dry (live:
    the covers come off while the logs are in). cut: leave the deck off (for looking in). installed: what stands in the
    deck's openings ("screens", "wash", "pumps"), so its temporary cover is left off."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    C, CW, GV, DS, G = "ih_concrete", "ih_concrete_wet", "ct_galv", "ct_steel_dark", "ct_grating"
    W = 2 * END + NB * BAY_W + (NB - 1) * PIER

    TX = (0.2, W + 0.25)                         # the cable trench's length along the deck (piece 2)

    def conc(x0, x1, y0, y1, d0, d1, faces=None):
        """Concrete: wet and stained below the lake, dry above (two boxes meeting at the water line)."""
        f = faces or {}
        if y1 == DECK and d0 < TRENCH[0] - 1e-6 and d1 > TRENCH[1] + 1e-6 and x0 < TX[1] and x1 > TX[0]:
            conc(x0, x1, y0, y1, d0, TRENCH[0], faces)               # the deck either side of the cable trench
            conc(x0, x1, y0, y1, TRENCH[1], d1, faces)
            xs = [x0, max(x0, TX[0]), min(x1, TX[1]), x1]
            for k, (xa, xb) in enumerate(zip(xs, xs[1:])):
                if xb - xa > 1e-6:
                    conc(xa, xb, y0, y1 if k != 1 else DECK - 0.35, *TRENCH, faces)
            return
        if y0 < level < y1:
            F.box(x0, x1, y0, level, d0, d1, {k: CW for k in ("out", "in", "left", "right", "bottom")} | {"top": None} | f)
            F.box(x0, x1, level, y1, d0, d1, {k: C for k in ("out", "in", "left", "right", "top")} | {"bottom": None} | f)
        else:
            m = CW if y1 <= level else C
            F.box(x0, x1, y0, y1, d0, d1, {k: m for k in ("out", "in", "left", "right", "bottom")} | {"top": "ih_deck" if y1 == DECK else m} | f)

    # ------------------------------------------------------------ the floor slab, end walls, piers, back wall
    conc(-0.4, W + 0.4, FLOOR - 0.6, FLOOR, D_BACK, D_FACE + 0.4)
    for x0, x1 in ((0.0, END), (W - END, W)):
        conc(x0, x1, FLOOR, DECK - SLAB, D_BACK + 0.6, D_FACE)
    for i in range(1, NB):
        x0 = bay_x(i - 1)[1]
        conc(x0, x0 + PIER, FLOOR, DECK - SLAB, D_BACK + 0.6, D_FACE)
        # its nose, rounded, standing out under the deck's edge
        F.cyl((x0 + PIER / 2, FLOOR, D_FACE), (x0 + PIER / 2, level, D_FACE), PIER / 2, CW, sides=16)
        F.cyl((x0 + PIER / 2, level, D_FACE), (x0 + PIER / 2, DECK - SLAB, D_FACE), PIER / 2, C, sides=16)
    conc(0.0, W, FLOOR, DECK - SLAB, D_BACK, D_BACK + 0.6)
    # the curtain wall across the bays' mouths, from under the deck to below the lowest water
    for i in range(NB):
        x0, x1 = bay_x(i)
        conc(x0, x1, -3.0, DECK - SLAB, *CURTAIN)
    # ------------------------------------------------------------ the deck slab round its openings
    slots = [RACK_SLOT, LOG1, SCREEN, LOG2]
    for i in range(-1, NB if not cut else -1):
        # over a pier (or an end wall): solid the whole way
        if i == -1:
            px0, px1 = -0.4, END
        else:
            px0 = bay_x(i)[1]
            px1 = px0 + PIER if i < NB - 1 else W + 0.4
        conc(px0, px1, DECK - SLAB, DECK, D_BACK, D_FACE + 0.4)
    for i in range(NB if not cut else 0):
        x0, x1 = bay_x(i)
        edges = [D_FACE + 0.4] + [e for s in slots for e in (s[1], s[0])] + [D_BACK]
        # solid strips between the slots, down the bay's width
        for a, b in zip(edges[0::2], edges[1::2]):
            if a - b > 1e-6:
                if b <= PUMP_D[0] < a:            # the pump segment: holes for the soleplates
                    holes = [(xa, xb, PUMP_D[0], PUMP_D[1], "pumps") for xa, xb in pump_holes(i)]
                    if BAYS[i].startswith("MU"):  # and the screen wash pump's (piece 2)
                        holes.append((*WASH_X(i), *WASH_D, "wash"))
                    xs = sorted({x0, x1, *[h[0] for h in holes], *[h[1] for h in holes]})
                    ds = sorted({b, a, *[h[2] for h in holes], *[h[3] for h in holes]})
                    for xa, xb in zip(xs, xs[1:]):
                        run = None
                        for da, db in zip(ds, ds[1:]):
                            inside = any(h[0] <= xa and xb <= h[1] and h[2] <= da and db <= h[3] for h in holes)
                            if not inside:
                                run = (run[0], db) if run else (da, db)
                            elif run:
                                conc(xa, xb, DECK - SLAB, DECK, *run)
                                run = None
                        if run:
                            conc(xa, xb, DECK - SLAB, DECK, *run)
                    for xa, xb, da, db, what in holes:
                        if what in installed:
                            continue
                        # the opening's embedded frame, and its temporary cover until its pump goes in
                        F.box(xa, xb, DECK - 0.03, DECK - 0.002, da, db, {"top": "ct_diamond", "out": GV, "in": GV, "left": GV,
                                                                         "right": GV, "bottom": None})
                        F.box(xa, xb, DECK - 0.09, DECK - 0.03, da + 0.05, db - 0.05, DS)                    # its bearer
                else:
                    conc(x0, x1, DECK - SLAB, DECK, b, a)
        # covers: grating over the rack slot (the rake works through it later), checker plate over the stop-log slots,
        # grating over the screen well until the screens go in (piece 2); each on edge angles cast into the slot's sides
        for (d0, d1), mat in ((RACK_SLOT, G), (LOG1, "ct_diamond"), (SCREEN, G), (LOG2, "ct_diamond")):
            if logs == i and (d0, d1) == LOG1 or "screens" in installed and (d0, d1) in (SCREEN, RACK_SLOT):
                continue
            F.box(x0 + 0.002, x1 - 0.002, DECK - 0.04, DECK - 0.002, d0 + 0.002, d1 - 0.002, mat)
            for dd in (d0, d1 - 0.05):
                F.box(x0, x1, DECK - 0.09, DECK - 0.04, dd, dd + 0.05, DS)
            if mat == "ct_diamond":                         # a lifting eye at each end of a stop-log cover
                for xx in (x0 + 0.3, x1 - 0.3):
                    F.cyl((xx - 0.04, DECK - 0.002 + 0.03, (d0 + d1) / 2), (xx + 0.04, DECK - 0.002 + 0.03, (d0 + d1) / 2), 0.012, GV, sides=6)
    # ------------------------------------------------------------ steel in the bays
    for i in range(NB):
        x0, x1 = bay_x(i)
        # stop-log guides: a channel cast into each pier face at both slots, floor to deck
        for d0, d1 in (LOG1, LOG2):
            for xs, s in ((x0, 1), (x1, -1)):
                F.box(*sorted((xs, xs + s * 0.08)), FLOOR, DECK - SLAB, d0 + 0.08, d0 + 0.13, GV)
                F.box(*sorted((xs, xs + s * 0.08)), FLOOR, DECK - SLAB, d1 - 0.13, d1 - 0.08, GV)
                F.box(*sorted((xs, xs + s * 0.03)), FLOOR, DECK - SLAB, d0 + 0.13, d1 - 0.13, {"left" if s < 0 else "right": DS})
        # the screen well's guides (the travelling screens' frames slide down them in piece 2)
        for xs, s in ((x0, 1), (x1, -1)):
            for dd in (SCREEN[0] + 0.5, SCREEN[1] - 0.5):
                F.box(*sorted((xs, xs + s * 0.12)), FLOOR, DECK - SLAB, dd - 0.1, dd + 0.1, GV)
        # trash racks: flat bars 75 x 12 mm at 100 mm, from the bay floor up to the rake's slot; spacer
        # rods through them; a frame each side
        bot = (FLOOR, 7.85)                         # leaning about 8 degrees, its top under the rake's slot
        top = (DECK - SLAB - 0.02, 8.9)
        for k in range(int(BAY_W / 0.1) - 1):
            x = x0 + 0.1 * (k + 1)
            F.bar((x, bot[0], bot[1]), (x, top[0], top[1]), 0.075, 0.012, GV, up="t")
        for f in (0.2, 0.5, 0.8):
            y = bot[0] + (top[0] - bot[0]) * f
            d = bot[1] + (top[1] - bot[1]) * f
            F.cyl((x0 + 0.02, y, d + 0.05), (x1 - 0.02, y, d + 0.05), 0.012, GV, sides=6)
        for xs in (x0 + 0.04, x1 - 0.04):
            F.bar((xs, bot[0], bot[1]), (xs, top[0], top[1]), 0.12, 0.08, DS, up="t")
        # the bay's number stencilled on the curtain wall above the water, and its service under it
        cx = (x0 + x1) / 2
        F._quad([F.w(cx - 0.3, -0.4, D_FACE + 0.003), F.w(cx + 0.3, -0.4, D_FACE + 0.003), F.w(cx + 0.3, 0.2, D_FACE + 0.003),
                 F.w(cx - 0.3, 0.2, D_FACE + 0.003)], ["ih_stencils", i / 8, 0, (i + 1) / 8, 1], F.r)   # (read from the lake)
    # ------------------------------------------------------------ crane rails on the deck (the rake and gantry, piece 2)
    for d in (D_FACE - 0.3, LOG2[0] - 0.5) if not cut else ():
        F.box(-0.2, W + 0.2, DECK, DECK + 0.012, d - 0.12, d + 0.12, DS)                               # sole plate
        F.box(0.0, W, DECK + 0.012, DECK + 0.03, d - 0.065, d + 0.065, GV)                     # rail foot
        F.box(0.0, W, DECK + 0.03, DECK + 0.1, d - 0.008, d + 0.008, GV)                       # web
        F.box(0.0, W, DECK + 0.1, DECK + 0.14, d - 0.03, d + 0.03, GV)                         # head
        for x0, x1 in ((-0.2, 0.0), (W, W + 0.2)):
            F.box(x0, x1, DECK + 0.012, DECK + 0.9, d - 0.15, d + 0.15, "ct_yellow")                     # end stop (at the buffers)
        for k in range(int((W + 0.4) / 0.75) + 1):                                                   # clips
            x = 0.375 + 0.75 * k
            if x < W - 0.1:
                for s in (-1, 1):
                    F.box(x - 0.04, x + 0.04, DECK + 0.012, DECK + 0.04, *sorted((d + s * 0.07, d + s * 0.11)), DS)
    # the cable trench's covers: checker plate in metre lengths either side of a slot for the gantry's cable, on angles
    if not cut:
        for xa in np.arange(TX[0], TX[1] - 1e-6, 1.0):
            xb = min(xa + 1.0, TX[1])
            for da, db in ((TRENCH[0], sum(TRENCH) / 2 - 0.025), (sum(TRENCH) / 2 + 0.025, TRENCH[1])):
                F.box(xa + 0.003, xb - 0.003, DECK - 0.03, DECK - 0.002, da + 0.002, db - 0.002, "ct_diamond")
        for da, db in ((TRENCH[0], TRENCH[0] + 0.03), (TRENCH[1] - 0.03, TRENCH[1])):
            F.box(TX[0], TX[1], DECK - 0.06, DECK - 0.03, da, db, DS)
    # ------------------------------------------------------------ guardrails over the water, life ring, staff gauge
    if not cut:
        rails_and_ring(F, W)
    # staff gauge off pier 2's nose: an enamelled board marked in feet and tenths on a steel channel, from below the
    # lowest water up to the deck, standing out past the deck's edge so it can be read from the rail above
    gx = bay_x(1)[1]
    gd = D_FACE + 1.2                                      # on its own pile, a metre out past the pier's nose
    F.cyl((gx + 0.3, -4.0, gd - 0.11), (gx + 0.3, DECK - SLAB - 0.05, gd - 0.11), 0.09, DS, sides=12)
    F.box(gx + 0.17, gx + 0.43, -3.6, DECK - SLAB - 0.05, gd, gd + 0.015, {"out": ["ih_staff_gauge", 0, 0, 1, 1],
          "left": GV, "right": GV, "top": GV, "bottom": GV})
    for y in (-3.3, -1.8, -0.3):
        F.box(gx + 0.26, gx + 0.34, y - 0.03, y + 0.03, gd - 0.03, gd, GV)                         # board clips
    for y in (-2.6, -0.1):                                    # struts back to the pier's nose
        F.box(gx + 0.25, gx + 0.35, y - 0.04, y + 0.04, D_FACE + PIER / 2 - 0.02, gd - 0.2, DS)
    # ------------------------------------------------------------ stop logs in a bay (live), the water
    if logs is not None:
        x0, x1 = bay_x(logs)
        n = int((DECK - SLAB - 0.3 - FLOOR) / 0.3)
        for k in range(n):
            F.box(x0 + 0.035, x1 - 0.035, FLOOR + 0.3 * k, FLOOR + 0.3 * (k + 1) - 0.003, LOG1[0] + 0.14, LOG1[1] - 0.14, "ct_alum")
    for i in range(NB):
        x0, x1 = bay_x(i)
        if logs == i:
            spans = [(LOG1[1] - 0.14, CURTAIN[0], level), (D_BACK + 0.6, LOG1[0] + 0.14, FLOOR + 0.06)]
        else:
            spans = [(D_BACK + 0.6, CURTAIN[0], level)]
        for d0, d1, y in spans:
            F._quad([F.w(x0, y, d0), F.w(x1, y, d0), F.w(x1, y, d1), F.w(x0, y, d1)], "ct_water", (0, 1, 0), lit=True)
    return o.ops()


def rails_and_ring(F, W):
    """Guardrails where the deck stands over the water, the life ring on its post."""
    GV = "ct_galv"
    CT._rails(F, [(-0.35, 2.0), (-0.35, D_FACE + 0.35), (W + 0.35, D_FACE + 0.35), (W + 0.35, 2.0)], DECK)
    lx = bay_x(1)[1] + PIER / 2
    F.cyl((lx + 1.6, DECK, D_FACE + 0.2), (lx + 1.6, DECK + 1.5, D_FACE + 0.2), 0.035, "ct_yellow", sides=8)     # life ring post
    for k in range(16):
        a0, a1 = 2 * math.pi * k / 16, 2 * math.pi * (k + 1) / 16
        c = np.array([lx + 1.6, DECK + 1.15, D_FACE + 0.27])
        F.cyl(tuple(c + [0.3 * math.cos(a0), 0.3 * math.sin(a0), 0]), tuple(c + [0.3 * math.cos(a1), 0.3 * math.sin(a1), 0]),
              0.055, "ih_lifering" if k % 4 < 2 else "ih_lifering_w", sides=8)
    F.box(lx + 1.57, lx + 1.63, DECK + 1.38, DECK + 1.44, D_FACE + 0.2, D_FACE + 0.27, GV)                     # its hook


def intake_textures():
    """Piece 1's materials: cast concrete (form lines, tie holes), the same wet and stained below the water, the
    staff gauge, the bay stencils, the life ring."""
    out = {}
    out["ih_concrete"] = CT.concrete(201, base=(170, 168, 160), zone="mid", streak=0.5)
    out["ih_deck"] = CT.concrete(203, base=(176, 174, 166), zone="mid", streak=0.0, lift=False)   # a broomed slab
    wet = CT.concrete(202, base=(118, 122, 112), zone="low", damp=1.0, lift=True)
    out["ih_concrete_wet"] = wet
    # staff gauge (USGS style): white enamel, black E-shaped tenth marks, whole feet numbered in red
    g = Image.new("RGBA", (128, 2048), (246, 246, 240, 255))
    d = ImageDraw.Draw(g)
    span = (DECK - SLAB - 0.05) - (-3.6)
    ft = 0.3048
    fb = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 54)
    n = int(span / ft * 10) + 1
    for k in range(n):
        y = 2048 - int(k * ft / 10 / span * 2048)
        w = 100 if k % 10 == 0 else (70 if k % 5 == 0 else 40)
        d.rectangle([0, y - 6, w, y], fill=(20, 20, 20, 255))
        if k % 10 == 0 and k:
            d.text((112, y - 40), str(k // 10), font=fb, fill=(190, 20, 20, 255), anchor="rm")
    d.rectangle([0, 0, 6, 2047], fill=(20, 20, 20, 255))
    out["ih_staff_gauge"] = g
    # the bay numbers, stencilled black (a stencil's bridges left in)
    s = Image.new("RGBA", (1024, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(s)
    fs = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 92)
    for i, name in enumerate(BAYS):
        cx = i * 128 + 64
        d.text((cx, 64), str(i + 1), font=fs, fill=(28, 28, 28, 235), anchor="mm")
    out["ih_stencils"] = s
    out["ih_lifering"] = Image.new("RGBA", (16, 16), (236, 104, 22, 255))
    out["ih_lifering_w"] = Image.new("RGBA", (16, 16), (240, 240, 236, 255))
    return out


# ================================================================== piece 2: travelling screens, trash rake, screen wash
#
# Real-world basis: each bay has a through-flow vertical travelling water screen in its well: an endless chain of
# mesh basket panels (3/8 in mesh) running up the upstream side and down the back, over a foot sprocket at the bay floor
# and a head sprocket in a sheet-steel head housing above the deck; on each panel's face a lip (a fish bucket, in the
# EPA 316(b) modified "Ristroph" type) carries fish and debris up. The screens turn on a timer and whenever the level
# difference across them rises; then a low-pressure spray lifts fish into a return trough and a high-pressure spray
# blasts the debris off the panels into the same sluice, which runs back into the lake away from the intake. Screen
# wash pumps (small vertical pumps in the make-up bays) feed the spray header; each screen's spray branch has an
# isolation valve, a solenoid valve and a gauge. The trash racks are cleaned by a travelling gantry on the deck's
# rails: its rake runs down a rack's face and lifts the trash to a debris skip; its hoist lifts screens and pumps out for
# maintenance. Drive: motor, reducer and a shear pin on the head shaft, guarded.
#
# Here: four screens (panels 0.6 m, 3.24 m of mesh across, head housing 2.4 m over the deck); two screen wash pumps in
# bays 1 and 4; the sluice along the screens' backs, down the east side and out over the riprap into the lake; the
# gantry (yellow, 3 m wide, 5.6 m high) with a rake trolley over the racks and a hoist trolley over the screens; the
# debris skip at the deck's west end. Live: screens turning, the sprays and the sluice's water, the gantry's place,
# the rake's depth, the skip's fill.

S_FRONT, S_BACK, S_MID = 5.85, 4.35, 5.1
TAGS = ["TWS-1", "TWS-2", "TWS-3", "TWS-4", "TRASH RAKE GANTRY  10 T", "SCWP-1", "SCWP-2", "SCREEN WASH HDR",
        "LT-1A", "LT-1B", "LT-2A", "LT-2B", "LT-3A", "LT-3B", "LT-4A", "LT-4B",
        "TWS-1 LCS", "TWS-2 LCS", "TWS-3 LCS", "TWS-4 LCS", "SCWP-1 STRAINER", "SCWP-2 STRAINER"]


def tag(i):
    """Engraved aluminium tag i on the ih_tags sheet (4 across, 8 down, each 256 x 64 px)."""
    c, r = i % 4, i // 4
    return ["ih_tags", c / 4, r / 8, (c + 1) / 4, (r + 1) / 8]
HEAD_Y = DECK + 1.6                            # the head sprocket's shaft
FOOT_Y = FLOOR + 0.8


def screens_ops(ox=0.0, oz=0.0, level=LAKE, phase=0.0, turning=(True,) * NB, wash=True, gantry_x=11.0, rake_y=-2.0,
                skip=0.6, open_door=None, sluice=None):
    """Piece 2. Live: phase (the panels' travel), turning (per screen), wash (sprays and sluice water), gantry_x (the
    gantry's place), rake_y (the rake's depth), skip (the debris skip's fill), open_door ((screen, door) standing
    open), sluice (the sluice's water when it follows something other than wash: in game, any screen turning)."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    GV, DS, ST, YE, MO = "ct_galv", "ct_steel_dark", "ct_stainless", "ct_yellow", "ih_motor"
    W = 2 * END + NB * BAY_W + (NB - 1) * PIER
    for i in range(NB):
        x0, x1 = bay_x(i)
        # ---------------------------------------------- the screen: side frames in the well's guides, foot, panels
        for xs, s in ((x0, 1), (x1, -1)):
            F.box(*sorted((xs + s * 0.13, xs + s * 0.17)), FOOT_Y - 0.5, DECK, S_BACK - 0.25, S_FRONT + 0.25, DS)
            for dd in (SCREEN[0] + 0.5, SCREEN[1] - 0.5):                   # shoes in the guides
                F.box(*sorted((xs + s * 0.125, xs + s * 0.13)), FOOT_Y - 0.4, DECK - 0.7, dd - 0.06, dd + 0.06, GV)
            F.cyl((xs + s * 0.2, FOOT_Y, S_MID), (xs + s * 0.17, FOOT_Y, S_MID), 0.12, GV, sides=10)  # foot bearing
        F.cyl((x0 + 0.17, FOOT_Y, S_MID), (x1 - 0.17, FOOT_Y, S_MID), 0.05, DS, sides=10)              # foot shaft
        pa, pb = x0 + 0.2, x1 - 0.2
        off = ((phase[i] if isinstance(phase, (list, tuple)) else phase) % 0.6) if turning[i] else 0.0
        for d, sgn in ((S_FRONT, 1), (S_BACK, -1)):                         # up the front, down the back
            y = FOOT_Y + 0.05 + (off if sgn > 0 else 0.6 - off)
            while y + 0.58 < HEAD_Y - 0.05:
                F.box(pa, pb, y, y + 0.58, d - 0.03, d + 0.03, {"out": "ih_mesh", "in": "ih_mesh", "left": GV, "right": GV,
                                                                "top": GV, "bottom": GV})
                F.box(pa, pb, y + 0.58, y + 0.6, d - 0.035, d + 0.035, GV)            # the panel's hinge bar
                if sgn > 0:                                                          # the fish bucket lip
                    F.box(pa + 0.01, pb - 0.01, y, y + 0.03, d + 0.035, d + 0.14, GV)
                    F.box(pa + 0.01, pb - 0.01, y + 0.03, y + 0.12, d + 0.12, d + 0.14, GV)
                y += 0.6
            for xs in (pa - 0.015, pb + 0.015):                                     # the chains
                F.box(xs - 0.012, xs + 0.012, FOOT_Y, HEAD_Y, d - 0.02, d + 0.02, DS)
        # ---------------------------------------------- the head housing over the deck: bolted sheet walls, two access
        # doors in its back (live: open or shut), ribs, lugs, a vent cowl, its tag; inside, the head sprocket with the
        # panels going over it, the spray pipes and the debris trough
        hx0, hx1 = x0 + 0.1, x1 - 0.1
        hb, hf = SCREEN[0] + 0.15, SCREEN[1] - 0.15                                  # its back and front faces
        T_ = 0.02
        HO, HI = {"out": "ih_hood", "in": "ih_hood_in"}, "ih_hood_in"
        F.box(hx0, hx1, DECK, DECK + 2.4, hf - T_, hf, {"out": "ih_hood", "in": HI, "left": "ih_hood", "right": "ih_hood",
                                                       "top": "ih_hood"})
        for xa, xb in ((hx0, hx0 + T_), (hx1 - T_, hx1)):
            F.box(xa, xb, DECK, DECK + 2.4 - T_, hb + T_, hf - T_, {"left" if xa < x0 + 0.5 else "right": "ih_hood",
                                                                   "right" if xa < x0 + 0.5 else "left": HI})
        F.box(hx0, hx1, DECK + 2.4 - T_, DECK + 2.4, hb, hf - T_, {"top": "ih_hood_top", "bottom": HI, "in": "ih_hood", "left": "ih_hood",
                                                                   "right": "ih_hood"})
        doors = [(hx0 + 0.35, hx0 + 1.35), (hx1 - 1.35, hx1 - 0.35)]
        xs_b = [hx0] + [v for d_ in doors for v in d_] + [hx1]
        for xa, xb in zip(xs_b[0::2], xs_b[1::2]):                                 # the back wall round the doors
            F.box(xa, xb, DECK, DECK + 2.4 - T_, hb, hb + T_, {"in": "ih_hood", "out": HI, "top": "ih_hood"})
        for xa, xb in doors:
            F.box(xa, xb, DECK, DECK + 0.3, hb, hb + T_, {"in": "ih_hood", "out": HI, "top": "ih_hood"})
            F.box(xa, xb, DECK + 1.9, DECK + 2.4 - T_, hb, hb + T_, {"in": "ih_hood", "out": HI, "bottom": "ih_hood"})
        for k, (xa, xb) in enumerate(doors):
            open_ = open_door == (i, k)
            Dr = F.sub(xa, hb, -math.radians(100) if open_ else 0.0)             # hinged on its west edge, swinging out
            Dr.box(0.002, 0.998, DECK + 0.302, DECK + 1.898, 0.0, T_, {"in": "ih_hood_door", "out": HI, "left": GV,
                                                                            "right": GV, "top": GV, "bottom": GV})
            for y in (DECK + 0.5, DECK + 1.7):
                Dr.cyl((0.0, y - 0.07, -0.03), (0.0, y + 0.07, -0.03), 0.016, GV, sides=6)            # hinges
            Dr.box(0.84, 0.9, DECK + 1.0, DECK + 1.2, -0.05, -0.025, DS)                                # latch handle
        for xr in np.arange(hx0 + 0.18, hx1 - 0.1, 0.85):                          # stiffening ribs on its front
            F.box(xr - 0.025, xr + 0.025, DECK + 0.05, DECK + 2.35, hf, hf + 0.04, GV)
        for xl in (hx0 + 0.2, hx1 - 0.2):                                          # lifting lugs, a vent cowl
            for dl in (hb + 0.2, hf - 0.2):
                F.box(xl - 0.04, xl + 0.04, DECK + 2.4, DECK + 2.52, dl - 0.01, dl + 0.01, GV)
        F.cyl(((x0 + x1) / 2, DECK + 2.4, S_MID + 0.4), ((x0 + x1) / 2, DECK + 2.7, S_MID + 0.4), 0.12, GV, sides=12)
        F.cyl(((x0 + x1) / 2, DECK + 2.7, S_MID + 0.4), ((x0 + x1) / 2, DECK + 2.74, S_MID + 0.4), 0.2, GV, sides=12)
        F.box(hx0 + 0.69, hx0 + 1.01, DECK + 2.0, DECK + 2.08, hb - 0.003, hb, {"in": tag(i)})                # TWS-n
        # inside: the head sprocket's wheels, the panels going over the top
        Rs = (S_FRONT - S_BACK) / 2
        for xs in (pa - 0.015, pb + 0.015):
            F.cyl((xs - 0.03, HEAD_Y, S_MID), (xs + 0.03, HEAD_Y, S_MID), Rs - 0.03, "ih_sprocket", sides=16)
        for k in range(4):
            a0, a1 = math.pi * k / 4, math.pi * (k + 1) / 4
            p0 = (0.0, HEAD_Y + Rs * math.sin(a0), S_MID + Rs * math.cos(a0))
            p1 = (0.0, HEAD_Y + Rs * math.sin(a1), S_MID + Rs * math.cos(a1))
            F.bar(((pa + pb) / 2, p0[1], p0[2]), ((pa + pb) / 2, p1[1], p1[2]), 0.06, pb - pa - 0.004 * (k % 2),
                  ["ih_mesh"] * 6)
        # the high-pressure spray pipe inside the loop, its nozzles at the back run; the debris trough behind it
        F.cyl((pa, DECK + 1.25, S_BACK + 0.35), (pb, DECK + 1.25, S_BACK + 0.35), 0.035, GV, sides=8)
        for k in range(11):
            xn = pa + 0.15 + k * (pb - pa - 0.3) / 10
            F.cyl((xn, DECK + 1.25, S_BACK + 0.33), (xn, DECK + 1.25, S_BACK + 0.25), 0.012, "ct_brass", sides=6)
            if wash and turning[i]:                                                   # its fan of spray (live)
                c = F.w(xn, DECK + 1.25, S_BACK + 0.25)
                F._quad([F.w(xn - 0.11, DECK + 1.15, S_BACK + 0.04), F.w(xn + 0.11, DECK + 1.15, S_BACK + 0.04), c, c],
                        "ih_spray", (0, 1, 0), lit=True)
        F.box(pa, pb, DECK + 1.0, DECK + 1.02, hb + 0.03, S_BACK - 0.05, ST)                          # debris trough
        F.box(pa, pb, DECK + 1.02, DECK + 1.2, hb + 0.03, hb + 0.045, ST)
        # the head shaft through the housing's sides into its bearings, the drive on the east side (over the pier)
        F.cyl((x0 + 0.02, HEAD_Y, S_MID), (x1 + 0.02, HEAD_Y, S_MID), 0.06, DS, sides=10)
        for xs in (x0 + 0.02, x1 - 0.02):                                           # outside the housing's sides
            F.box(xs - 0.06, xs + 0.06, DECK, HEAD_Y - 0.08, S_MID - 0.12, S_MID + 0.12, DS)            # bearing pedestal
            F.box(xs - 0.07, xs + 0.07, HEAD_Y - 0.08, HEAD_Y + 0.1, S_MID - 0.14, S_MID + 0.14, GV)    # pillow block
        F.box(x1 + 0.045, x1 + 0.5, DECK, DECK + 0.08, S_MID - 0.6, S_MID + 0.6, DS)                   # drive base
        F.box(x1 + 0.06, x1 + 0.46, DECK + 0.08, DECK + 0.55, S_MID - 0.1, S_MID + 0.5, "ih_reducer")  # reducer
        F.cyl((x1 + 0.26, DECK + 0.32, S_MID - 0.1), (x1 + 0.26, DECK + 0.32, S_MID - 0.55), 0.17, MO, sides=14)   # motor
        F.cyl((x1 + 0.26, DECK + 0.32, S_MID - 0.55), (x1 + 0.26, DECK + 0.32, S_MID - 0.6), 0.15, DS, sides=14)   # fan cowl
        F.box(x1 + 0.12, x1 + 0.4, DECK + 0.55, HEAD_Y + 0.22, S_MID + 0.15, S_MID + 0.45, YE)         # chain guard
        F.box(x1 + 0.05, x1 + 0.11, HEAD_Y - 0.06, HEAD_Y + 0.06, S_MID - 0.06, S_MID + 0.06, "ct_brass")  # shear pin hub
        # ---------------------------------------------- the spray branch into the housing's back: valve, solenoid, gauge
        bx = (x0 + x1) / 2
        F.cyl((bx, DECK + 2.2, 3.6), (bx, DECK + 2.2, SCREEN[0] + 0.15), 0.04, GV, sides=8)
        F.cyl((bx, DECK + 2.2, 3.78), (bx, DECK + 2.2, 3.86), 0.07, "ih_valve", sides=10)            # isolation valve
        F.cyl((bx, DECK + 2.2, 3.82), (bx, DECK + 2.45, 3.82), 0.012, GV, sides=6)
        F.cyl((bx - 0.12, DECK + 2.45, 3.82), (bx + 0.12, DECK + 2.45, 3.82), 0.012, "ct_wheel", sides=6)  # its handwheel
        F.box(bx - 0.06, bx + 0.06, DECK + 2.15, DECK + 2.25, 3.88, 3.94, DS)                          # solenoid valve
        F.cyl((bx, DECK + 2.25, 3.91), (bx, DECK + 2.37, 3.91), 0.035, "ih_solenoid", sides=8)
        F.cyl((bx + 0.08, DECK + 2.2, 3.7), (bx + 0.08, DECK + 2.0, 3.7), 0.01, GV, sides=6)          # gauge
        F.cyl((bx + 0.08, DECK + 1.94, 3.68), (bx + 0.08, DECK + 1.94, 3.72), 0.05, "ih_gauge", "ih_gauge_face", sides=12)
    # ---------------------------------------------- the spray header along the screens' backs, from the wash pumps
    HY = DECK + 2.2                              # the spray header, over head height along the housings' backs
    F.cyl((END / 2, HY, 3.6), (W - END / 2, HY, 3.6), 0.06, GV, sides=10)
    for xm in (2.9, 7.1, W - 7.1, W - 2.9):                                      # its markers, the flow toward the middle
        F.box(xm - 0.3, xm + 0.3, HY - 0.045, HY + 0.045, 3.6 - 0.064, 3.6 - 0.062,
              {"in": ["ih_marker", 0, 0 if xm < W / 2 else 0.5, 1, 0.5 if xm < W / 2 else 1]})
    for i in (0, NB - 1):
        wx = sum(WASH_X(i)) / 2
        wd = sum(WASH_D) / 2
        s_ = -1 if i == 0 else 1                     # toward its end wall
        ex = (END / 2) if i == 0 else (W - END / 2)  # the run over the end wall, clear of the gantry's rails
        # the screen wash pump: soleplate, discharge head with its nameplate, motor with fan cowl and terminal box
        F.box(WASH_X(i)[0] - 0.03, WASH_X(i)[1] + 0.03, DECK, DECK + 0.03, WASH_D[0] - 0.03, WASH_D[1] + 0.03, DS)
        for a in (-0.32, 0.32):
            for b in (-0.32, 0.32):
                F.cyl((wx + a, DECK + 0.03, wd + b), (wx + a, DECK + 0.07, wd + b), 0.02, GV, sides=6)
        F.cyl((wx, DECK + 0.03, wd), (wx, DECK + 0.6, wd), 0.2, "ih_valve", sides=14)
        F.cyl((wx, DECK + 0.6, wd), (wx, DECK + 0.66, wd), 0.24, "ih_valve", sides=14)
        F.cyl((wx, DECK + 0.66, wd), (wx, DECK + 1.25, wd), 0.2, MO, sides=14)
        F.cyl((wx, DECK + 1.25, wd), (wx, DECK + 1.36, wd), 0.17, "ih_cowl", sides=14)
        F.box(wx - 0.09, wx + 0.09, DECK + 0.85, DECK + 1.05, wd - 0.3, wd - 0.19, DS)          # terminal box
        F.cyl((wx, DECK + 0.85, wd - 0.25), (wx, DECK - 0.1, wd - 0.25), 0.02, GV, sides=6)      # its conduit into the deck
        F.cyl((wx, DECK + 0.002, wd - 0.25), (wx, DECK + 0.02, wd - 0.25), 0.035, GV, sides=8)
        F.box(wx - 0.06, wx + 0.06, DECK + 0.3, DECK + 0.38, wd - 0.205, wd - 0.2, {"in": tag(5 if i == 0 else 6)})
        F.cyl((wx, -6.0, wd), (wx, DECK - SLAB, wd), 0.1, "ih_valve", sides=10)                        # its column
        F.cyl((wx, -6.4, wd), (wx, -6.0, wd), 0.16, "ih_valve", sides=10)                              # bowl
        # discharge toward the end wall: pressure gauge, check valve, gate valve, duplex strainer, then down through
        # the deck (sleeve) to run in the end wall's slab, under the trench and the rail, up again behind the slot
        y = DECK + 0.35
        p0 = (wx + s_ * 0.2, y, wd)
        F.cyl(p0, (wx + s_ * 0.3, y, wd), 0.05, GV, sides=8)
        F.cyl((wx + s_ * 0.3, y, wd), (wx + s_ * 0.42, y, wd), 0.08, "ih_valve", sides=10)            # check valve
        F.cyl((wx + s_ * 0.36, y + 0.08, wd), (wx + s_ * 0.36, y + 0.12, wd), 0.03, "ih_valve", sides=8)
        F.cyl((wx + s_ * 0.42, y, wd), (wx + s_ * 0.5, y, wd), 0.05, GV, sides=8)
        F.cyl((wx + s_ * 0.5, y, wd), (wx + s_ * 0.6, y, wd), 0.08, "ih_valve", sides=10)             # gate valve
        F.cyl((wx + s_ * 0.55, y + 0.08, wd), (wx + s_ * 0.55, y + 0.4, wd), 0.012, GV, sides=6)
        F.cyl((wx + s_ * 0.55, y + 0.4, wd - 0.14), (wx + s_ * 0.55, y + 0.4, wd + 0.14), 0.012, "ct_wheel", sides=6)
        F.cyl((wx + s_ * 0.6, y, wd), (ex, y, wd), 0.05, GV, sides=8)
        F.cyl((wx + s_ * 0.25, y + 0.05, wd), (wx + s_ * 0.25, y + 0.2, wd), 0.008, GV, sides=6)      # gauge
        F.cyl((wx + s_ * 0.25, y + 0.24, wd - 0.02), (wx + s_ * 0.25, y + 0.24, wd + 0.02), 0.045, "ih_gauge", "ih_gauge_face",
              sides=12)
        # duplex strainer on the end wall: two baskets, the changeover handle; its legs on the deck
        for b in (-0.17, 0.17):
            F.cyl((ex, y - 0.15, wd + b), (ex, y + 0.32, wd + b), 0.11, "ih_valve", sides=12)
            F.cyl((ex, y + 0.32, wd + b), (ex, y + 0.36, wd + b), 0.13, "ih_valve", sides=12)
            F.cyl((ex, DECK, wd + b), (ex, y - 0.15, wd + b), 0.025, DS, sides=6)
        F.box(ex - 0.03, ex + 0.03, y + 0.36, y + 0.42, wd - 0.25, wd + 0.25, DS)
        F.cyl((ex, y + 0.42, wd), (ex, y + 0.6, wd), 0.015, DS, sides=6)
        F.box(ex - 0.02, ex + 0.02, y + 0.58, y + 0.62, wd - 0.35, wd, "ct_wheel")                 # handle
        F.box(ex - 0.12, ex + 0.12, y + 0.06, y + 0.12, wd + 0.29, wd + 0.292, {"out": tag(20 if i == 0 else 21)})
        F.cyl((ex, y, wd - 0.28), (ex, y, wd - 0.45), 0.05, GV, sides=8)
        F.cyl((ex, y, wd - 0.45), (ex, DECK - 0.15, wd - 0.45), 0.05, GV, sides=8)              # down into the slab
        F.cyl((ex, DECK, wd - 0.45), (ex, DECK + 0.03, wd - 0.45), 0.08, GV, sides=10)          # floor sleeve
        F.cyl((ex, DECK, 3.6), (ex, DECK + 0.03, 3.6), 0.08, GV, sides=10)                      # and up again
        F.cyl((ex, DECK - 0.15, 3.6), (ex, HY + 0.06, 3.6), 0.06, GV, sides=10)                 # into the header's end
    # header and sluice stands (every 2.1 m, on the deck strip between the screens and the stop-log slot)
    xs_ = list(np.arange(bay_x(0)[0] + 0.3, W, 2.1))
    for x in xs_:
        F.box(x - 0.04, x + 0.04, DECK + 0.015, HY - 0.075, 3.56, 3.64, DS)
        F.box(x - 0.08, x + 0.08, HY - 0.075, HY - 0.06, 3.5, 3.7, DS)          # the header's saddle
        F.box(x - 0.15, x + 0.15, DECK, DECK + 0.015, 3.45, 3.75, DS)
    # ---------------------------------------------- the fish and debris sluice: along the screens' backs (catching each
    # head's trough), down the east side and out over the riprap into the lake
    yb = DECK + 0.25                              # the sluice low on short stands, clear of eye height
    sl = [(-0.1, 3.3), (W + 0.6, 3.3), (W + 0.6, 13.0)]
    sluice_on = wash if sluice is None else sluice
    def trough(xa, da, xb, db, y0, y1):
        if abs(da - db) < 1e-6:
            F.box(min(xa, xb), max(xa, xb), y0, y0 + 0.012, da - 0.2, da + 0.2, ST)
            for s in (-1, 1):
                F.box(min(xa, xb), max(xa, xb), y0 + 0.012, y0 + 0.32, *sorted((da + s * 0.2, da + s * 0.188)), ST)
            for xf in np.arange(min(xa, xb) + 2.0, max(xa, xb) - 0.3, 2.0):            # bolted joints
                F.box(xf - 0.025, xf + 0.025, y0 - 0.015, y0, da - 0.215, da + 0.215, ST)
                for s in (-1, 1):
                    F.box(xf - 0.025, xf + 0.025, y0, y0 + 0.335, *sorted((da + s * 0.2, da + s * 0.215)), ST)
            if sluice_on:
                F._quad([F.w(xa, y0 + 0.07, da - 0.188), F.w(xb, y0 + 0.07, da - 0.188), F.w(xb, y0 + 0.07, da + 0.188),
                         F.w(xa, y0 + 0.07, da + 0.188)], "ct_water", (0, 1, 0), lit=True)
        else:
            F.box(xa - 0.2, xa + 0.2, y0, y0 + 0.012, min(da, db), max(da, db), ST)
            for s in (-1, 1):
                F.box(*sorted((xa + s * 0.2, xa + s * 0.188)), y0 + 0.012, y0 + 0.32, min(da, db), max(da, db), ST)
            for df in np.arange(min(da, db) + 2.0, max(da, db) - 0.3, 2.0):
                F.box(xa - 0.215, xa + 0.215, y0 - 0.015, y0, df - 0.025, df + 0.025, ST)
                for s in (-1, 1):
                    F.box(*sorted((xa + s * 0.2, xa + s * 0.215)), y0, y0 + 0.335, df - 0.025, df + 0.025, ST)
            if sluice_on:
                F._quad([F.w(xa - 0.188, y0 + 0.07, da), F.w(xa + 0.188, y0 + 0.07, da), F.w(xa + 0.188, y0 + 0.07, db),
                         F.w(xa - 0.188, y0 + 0.07, db)], "ct_water", (0, 1, 0), lit=True)
    trough(-0.1, 3.3, W + 0.4, 3.3, yb, yb)
    F.box(W + 0.4, W + 0.8, yb, yb + 0.012, 3.1, 3.5, ST)                       # the corner
    F.box(W + 0.788, W + 0.8, yb + 0.012, yb + 0.32, 3.1, 3.5, ST)
    F.box(W + 0.4, W + 0.788, yb + 0.012, yb + 0.32, 3.1, 3.112, ST)
    trough(W + 0.6, 3.5, W + 0.6, 13.0, yb, yb)
    F.box(-0.112, -0.1, yb + 0.012, yb + 0.32, 3.1, 3.5, ST)                     # its closed end
    for k, xh in enumerate(sum(bay_x(i)) / 2 for i in range(NB)):                 # each head's trough into it
        # the chute: a sloping open trough from the head's debris trough, out through the housing's back into the sluice
        a_, b_ = (xh, DECK + 1.0, SCREEN[0] + 0.25), (xh, yb + 0.36, 3.35)
        F.bar(a_, b_, 0.4, 0.012, ST, up="y")
        for s in (-1, 1):
            F.bar((xh + s * 0.2, a_[1] + 0.1, a_[2]), (xh + s * 0.2, b_[1] + 0.1, b_[2]), 0.2, 0.012, ST, up="t")
        F.box(xh - 0.22, xh + 0.22, DECK + 0.85, DECK + 1.25, SCREEN[0] + 0.15 - 0.03, SCREEN[0] + 0.15 - 0.022, ST)  # its wall flange
    for d in (5.5, 7.5, 9.5, 11.5):                                              # its stands down the riprap
        yb_bank = max(-4.0, -(d - 2.0) * 4 / 6) if d > 2 else 0.0
        F.box(W + 0.56, W + 0.64, yb_bank - 0.3, yb - 0.015, d - 0.04, d + 0.04, DS)
    # ---------------------------------------------- per bay: the level elements either side of the screen (the
    # differential across it starts the wash and alarms; live readings in the panel, piece 4), the local control station
    for i in range(NB):
        x0, x1 = bay_x(i)
        for k, dl in enumerate((6.8, 0.6)):                    # upstream (A) and downstream (B) of the screen
            xl = x1 - 0.65 if (i == 0 and k == 1) else x0 + 0.25      # (bay 1's B clear of its screen wash pump)
            F.cyl((xl, -3.2, dl), (xl, DECK - SLAB, dl), 0.08, "ih_pvc", sides=10)                 # stilling well
            F.cyl((xl, DECK, dl), (xl, DECK + 0.32, dl), 0.08, GV, sides=10)                       # its stub
            F.cyl((xl, DECK + 0.32, dl), (xl, DECK + 0.35, dl), 0.12, GV, sides=12)                # flange
            F.cyl((xl, DECK + 0.35, dl), (xl, DECK + 0.5, dl), 0.07, "ih_solenoid", sides=12)     # ultrasonic transducer
            F.cyl((xl, DECK + 0.5, dl), (xl, DECK + 0.56, dl), 0.025, GV, sides=8)                 # its gland
            F.cyl((xl, DECK + 0.56, dl), (xl + 0.25, DECK + 0.56, dl), 0.012, GV, sides=6)
            F.cyl((xl + 0.3, DECK, dl), (xl + 0.3, DECK + 0.62, dl), 0.03, GV, sides=8)            # transmitter post
            F.box(xl + 0.2, xl + 0.4, DECK + 0.48, DECK + 0.68, dl + 0.03, dl + 0.13, {"out": ["ih_lt_face", k / 2, 0, (k + 1) / 2, 1], "in": DS,
                  "left": DS, "right": DS, "top": DS, "bottom": DS})                                   # transmitter
            F.box(xl + 0.2, xl + 0.4, DECK + 0.4, DECK + 0.45, dl + 0.13, dl + 0.132, {"out": tag(8 + 2 * i + k)})
            F.cyl((xl + 0.36, DECK + 0.48, dl + 0.08), (xl + 0.36, DECK - 0.1, dl + 0.08), 0.012, GV, sides=6)  # conduit
            F.cyl((xl + 0.36, DECK + 0.002, dl + 0.08), (xl + 0.36, DECK + 0.02, dl + 0.08), 0.025, GV, sides=8)
        # the local control station on the pier east of the screen, facing the lake side where its operator stands
        lx_ = x1 + 0.3
        F.cyl((lx_, DECK + 0.01, 6.76), (lx_, DECK + 1.5, 6.76), 0.04, GV, sides=8)                   # its post, behind it
        F.box(lx_ - 0.12, lx_ + 0.12, DECK, DECK + 0.01, 6.64, 6.88, GV)
        for y in (DECK + 1.12, DECK + 1.38):
            F.box(lx_ - 0.1, lx_ + 0.1, y - 0.02, y + 0.02, 6.72, 6.8, GV)                               # its unistrut
        F.box(lx_ - 0.16, lx_ + 0.16, DECK + 1.05, DECK + 1.45, 6.8, 6.92, {"out": ["ih_lcs", 0, 0, 1, 1], "in": "ih_lcs_side",
              "left": "ih_lcs_side", "right": "ih_lcs_side", "top": "ih_lcs_side", "bottom": "ih_lcs_side"})
        # (seen from the lake side, +x is on the left)
        F.cyl((lx_ + 0.07, DECK + 1.34, 6.92), (lx_ + 0.07, DECK + 1.34, 6.96), 0.03, DS, sides=10)       # hand-off-auto
        F.box(lx_ + 0.06, lx_ + 0.08, DECK + 1.315, DECK + 1.365, 6.96, 6.975, "ct_wheel")
        F.cyl((lx_ - 0.07, DECK + 1.34, 6.92), (lx_ - 0.07, DECK + 1.34, 6.95), 0.022, "ih_lamp_g" if turning[i] else
              "ih_lamp_off", sides=10, lit=turning[i])                                                   # running
        F.cyl((lx_ + 0.07, DECK + 1.17, 6.92), (lx_ + 0.07, DECK + 1.17, 6.945), 0.022, "ih_btn_g", sides=10)  # start
        F.cyl((lx_ - 0.07, DECK + 1.17, 6.92), (lx_ - 0.07, DECK + 1.17, 6.935), 0.02, "ih_btn_r", sides=10)  # stop
        F.cyl((lx_ - 0.07, DECK + 1.17, 6.935), (lx_ - 0.07, DECK + 1.17, 6.955), 0.034, "ih_btn_r", sides=12)
        F.box(lx_ - 0.1, lx_ + 0.1, DECK + 1.057, DECK + 1.102, 6.92, 6.922, {"out": tag(16 + i)})
        _zone(F, f"tws_start:{i}", lx_ + 0.04, lx_ + 0.1, DECK + 1.14, DECK + 1.2, 6.9, 6.97)
        _zone(F, f"tws_stop:{i}", lx_ - 0.11, lx_ - 0.03, DECK + 1.13, DECK + 1.21, 6.9, 6.97)
        F.cyl((lx_ + 0.1, DECK + 1.05, 6.86), (lx_ + 0.1, DECK - 0.1, 6.86), 0.014, GV, sides=6)            # conduit
        F.cyl((lx_ + 0.1, DECK + 0.002, 6.86), (lx_ + 0.1, DECK + 0.02, 6.86), 0.03, GV, sides=8)
    gantry(F, gantry_x, rake_y)
    # ---------------------------------------------- the debris skip at the deck's west end (live fill)
    sx0, sx1, sd0, sd1 = -0.32, 0.75, 8.4, 9.5          # under the rake at the rails' west end, between them
    F.box(sx0, sx1, DECK + 0.1, DECK + 0.14, sd0, sd1, "ih_skip")
    for (a, b, c, d) in ((sx0, sx0 + 0.04, sd0, sd1), (sx1 - 0.04, sx1, sd0, sd1), (sx0 + 0.04, sx1 - 0.04, sd0, sd0 + 0.04),
                         (sx0 + 0.04, sx1 - 0.04, sd1 - 0.04, sd1)):
        F.box(a, b, DECK + 0.14, DECK + 1.1, c, d, "ih_skip")
    for x in (sx0 + 0.1, sx1 - 0.1):
        F.box(x - 0.05, x + 0.05, DECK, DECK + 0.1, sd0 + 0.1, sd1 - 0.1, DS)                       # its skids
    if skip > 0:
        F._quad([F.w(sx0 + 0.04, DECK + 0.14 + 0.9 * skip, sd0 + 0.04), F.w(sx1 - 0.04, DECK + 0.14 + 0.9 * skip, sd0 + 0.04),
                 F.w(sx1 - 0.04, DECK + 0.14 + 0.9 * skip, sd1 - 0.04), F.w(sx0 + 0.04, DECK + 0.14 + 0.9 * skip, sd1 - 0.04)],
                "ih_debris", (0, 1, 0), lit=True)
    # ---------------------------------------------- the guardrail on the land side of the open rack slot
    CT._rails(F, [(-0.346, RACK_SLOT[0] - 0.15), (W + 0.346, RACK_SLOT[0] - 0.15)], DECK, posts=False)   # (teed into the side rails)
    return o.ops()


RAILS_D = (D_FACE - 0.3, LOG2[0] - 0.5)        # the deck's crane rails (lake side, land side)
RAIL_TOP = DECK + 0.14
TRENCH = (1.85, 2.05)                           # the cable trench along the deck, land side of the land rail


def _drive(F, x, y, d, toward, YE, MO, DS):
    """A geared motor on a crane wheel: gearbox on the wheel's axle, motor along x, brake, fan cowl."""
    F.box(x - 0.14, x + 0.14, y - 0.16, y + 0.2, *sorted((d + toward * 0.2, d + toward * 0.42)), "ih_reducer")
    F.cyl((x + 0.14, y + 0.05, d + toward * 0.31), (x + 0.6, y + 0.05, d + toward * 0.31), 0.11, MO, sides=14)
    F.cyl((x + 0.6, y + 0.05, d + toward * 0.31), (x + 0.66, y + 0.05, d + toward * 0.31), 0.125, DS, sides=14)   # brake
    F.cyl((x + 0.66, y + 0.05, d + toward * 0.31), (x + 0.74, y + 0.05, d + toward * 0.31), 0.1, DS, sides=14)    # fan cowl
    F.box(x + 0.28, x + 0.4, y + 0.16, y + 0.24, *sorted((d + toward * 0.26, d + toward * 0.36)), DS)              # terminal box


def gantry(F, g, rake_y):
    """The travelling trash rake gantry (piece 2), centred at x g on the deck's rails: end carriages with flanged
    wheels, drives, buffers, rail sweepers and a rail clamp; I-section legs with ties and knee braces; two box girders
    with stiffeners and trolley rails, end ties; a walkway along the west girder reached by a ladder up a leg; the
    rake trolley over the racks with the rake head on its ropes (live depth) and the hoist trolley over the screens
    with its hook block; a cable reel feeding it from the deck's cable trench; a beacon, a horn, the SWL plate, the
    pendant."""
    YE, DS, GV, MO = "ih_crane", "ct_steel_dark", "ct_galv", "ih_motor"
    W_ = 2 * END + NB * BAY_W + (NB - 1) * PIER
    WY = RAIL_TOP + 0.22                             # wheel axles
    CB, CT_ = DECK + 0.48, DECK + 0.95              # end carriage bottom and top
    GB, GT = DECK + 4.6, DECK + 5.5                 # girder bottom and top
    for side, d in enumerate(RAILS_D):
        toward = -1 if side == 1 else 1             # each carriage's drives face away from the screens
        # the end carriage: a fabricated box, hazard-striped ends, stiffeners down its sides
        F.box(g - 1.8, g + 1.8, CB, CT_, d - 0.2, d + 0.2, {"out": YE, "in": YE, "top": YE, "bottom": YE, "left": "ih_hazard",
                                                          "right": "ih_hazard"})
        for xr in np.arange(g - 1.5, g + 1.6, 0.5):
            if abs(abs(xr - g) - 1.3) > 0.2:
                for s_ in (-1, 1):
                    F.box(xr - 0.015, xr + 0.015, CB + 0.02, CT_ - 0.02, *sorted((d + s_ * 0.2, d + s_ * 0.225)), YE)
        for xw in (g - 1.3, g + 1.3):
            F.cyl((xw, WY, d - 0.05), (xw, WY, d + 0.05), 0.22, "ih_wheel", sides=18)                # tread
            for s_ in (-1, 1):
                F.cyl((xw, WY, d + s_ * 0.05), (xw, WY, d + s_ * 0.07), 0.26, "ih_wheel", sides=18)   # flanges
                F.box(xw - 0.1, xw + 0.1, WY - 0.1, CB, *sorted((d + s_ * 0.09, d + s_ * 0.19)), YE)   # axle boxes
                F.cyl((xw, WY, d + s_ * 0.2), (xw, WY, d + s_ * 0.22), 0.06, DS, sides=10)
            for xs in (xw - 0.34, xw + 0.34):                                                      # rail sweepers
                F.box(xs - 0.02, xs + 0.02, RAIL_TOP + 0.02, CB, d - 0.07, d + 0.07, DS)
        _drive(F, g + 1.3, WY, d, toward, YE, MO, DS)
        for xb, s_ in ((g - 1.8, -1), (g + 1.8, 1)):                                               # buffers
            F.box(*sorted((xb, xb + s_ * 0.03)), CB + 0.1, CT_ - 0.1, d - 0.16, d + 0.16, DS)
            F.cyl((xb + s_ * 0.03, (CB + CT_) / 2, d), (xb + s_ * 0.2, (CB + CT_) / 2, d), 0.1, "ct_rubber", sides=12)
        # the rail clamp (storm anchor) at the carriage's middle: its body and jaws down either side of the rail head
        F.box(g - 0.2, g + 0.2, CB - 0.25, CB, d - 0.17, d + 0.17, YE)
        for s_ in (-1, 1):
            F.box(g - 0.12, g + 0.12, RAIL_TOP - 0.08, CB - 0.25, *sorted((d + s_ * 0.04, d + s_ * 0.09)), DS)
        # legs: I-sections, a tie between them, knee braces under the end tie
        for xl in (g - 1.45, g + 1.45):
            F.box(xl - 0.15, xl + 0.15, CT_ + 0.03, GB + 0.05, d - 0.18, d + 0.18, YE)
            for s_ in (-1, 1):
                F.box(*sorted((xl + s_ * 0.15, xl + s_ * 0.17)), CT_ + 0.03, GB + 0.05, d - 0.21, d + 0.21, YE)
            F.box(xl - 0.22, xl + 0.22, CT_, CT_ + 0.03, d - 0.24, d + 0.24, YE)                    # base plate
        F.box(g - 1.28, g + 1.28, DECK + 2.7, DECK + 2.95, d - 0.12, d + 0.12, YE)                  # tie
        for s_ in (-1, 1):
            F.bar((g + s_ * 1.3, GB - 0.9, d), (g + s_ * 0.75, GB + 0.06, d), 0.14, 0.14, YE, up="r")
        # the end tie over the legs, under and between the girders
        F.box(g - 1.65, g + 1.65, GB + 0.05, GT - 0.05, d - 0.22, d + 0.22, YE)
    # the girders: box sections, flange plates, stiffeners on their outer webs, the trolleys' rails on top
    d0, d1 = RAILS_D[1] - 0.45, RAILS_D[0] + 0.4
    for s_ in (-1, 1):
        xc = g + s_ * 1.15
        F.box(xc - 0.18, xc + 0.18, GB, GT, d0, d1, YE)
        F.box(xc - 0.24, xc + 0.24, GT, GT + 0.03, d0, d1, YE)                                    # top flange
        F.box(xc - 0.24, xc + 0.24, GB - 0.03, GB, d0, d1, YE)                                    # bottom flange
        for dd in np.arange(d0 + 0.6, d1 - 0.3, 0.6):
            if min(abs(dd - RAILS_D[0]), abs(dd - RAILS_D[1])) > 0.35:
                F.box(*sorted((xc + s_ * 0.18, xc + s_ * 0.22)), GB + 0.02, GT - 0.02, dd - 0.012, dd + 0.012, YE)
        F.box(xc - 0.03, xc + 0.03, GT + 0.03, GT + 0.12, d0 + 0.13, d1 - 0.13, DS)              # trolley rail
        for dd in (d0 + 0.05, d1 - 0.13):                                                         # its end stops
            F.box(xc - 0.06, xc + 0.06, GT + 0.03, GT + 0.3, dd, dd + 0.08, YE)
    # the walkway along the west girder on brackets, its guardrail, and the ladder up the land-side west leg
    wx0, wx1 = g - 2.05, g - 1.39
    F.box(wx0, wx1, GT - 0.04, GT, RAILS_D[1] - 0.65, d1, "ct_grating")
    for dd in np.arange(RAILS_D[1] - 0.55, d1, 1.2):
        F.bar((g - 1.39, GT - 0.5, dd), (wx0 + 0.05, GT - 0.06, dd), 0.06, 0.05, YE, up="r")
        F.box(wx0, wx1, GT - 0.1, GT - 0.04, dd - 0.03, dd + 0.03, YE)
    CT._rails(F, [(wx1 - 0.02, d1 - 0.02), (wx0 + 0.02, d1 - 0.02), (wx0 + 0.02, RAILS_D[1] - 0.63)], GT, mat=YE)
    lx, ld = g - 1.72, RAILS_D[1] - 0.48
    for s_ in (-1, 1):
        F.box(lx + s_ * 0.23 - 0.006, lx + s_ * 0.23 + 0.006, DECK + 0.3, GT + 1.07, ld - 0.03, ld + 0.03, YE)
        F.box(lx + s_ * 0.23 - 0.006, lx + s_ * 0.23 + 0.006, DECK + 1.5, DECK + 1.56, ld + 0.03, RAILS_D[1] - 0.25, YE)
        F.box(lx + s_ * 0.23 - 0.006, lx + s_ * 0.23 + 0.006, GB, GB + 0.06, ld + 0.03, RAILS_D[1] - 0.25, YE)
    for k in range(int((GT - DECK - 0.3) / 0.3) + 1):
        y = DECK + 0.3 + 0.3 * k
        F.cyl((lx - 0.22, y, ld), (lx + 0.22, y, ld), 0.012, YE, sides=6)
    # the rake trolley over the racks: wheels on the girders' rails, frame, grooved drum, gearbox, motor, brake
    def trolley(dc, drum_r, rope_x, low, hook):
        TY = GT + 0.12 + 0.12
        for s_ in (-1, 1):
            for sd in (-0.35, 0.35):
                F.cyl((g + s_ * 1.15 - 0.03, TY, dc + sd), (g + s_ * 1.15 + 0.03, TY, dc + sd), 0.12, "ih_wheel", sides=14)
            F.box(g + s_ * 1.15 - 0.15, g + s_ * 1.15 - 0.04, TY - 0.05, TY + 0.2, dc - 0.48, dc + 0.48, YE)
            F.box(g + s_ * 1.15 + 0.04, g + s_ * 1.15 + 0.15, TY - 0.05, TY + 0.2, dc - 0.48, dc + 0.48, YE)
        F.box(g - 1.35, g + 1.35, TY + 0.2, TY + 0.32, dc - 0.5, dc + 0.5, YE)                         # frame deck
        for s_ in (-1, 1):
            F.box(g + s_ * 0.85 - 0.05, g + s_ * 0.85 + 0.05, TY + 0.32, TY + 0.32 + drum_r + 0.15, dc - 0.12, dc + 0.12, YE)
        DY = TY + 0.32 + drum_r + 0.08
        F.cyl((g - 0.8, DY, dc), (g + 0.8, DY, dc), drum_r, "ih_drum", sides=18)
        F.box(g + 0.9, g + 1.2, TY + 0.32, TY + 0.75, dc - 0.2, dc + 0.2, "ih_reducer")
        F.cyl((g + 1.05, TY + 0.55, dc - 0.2), (g + 1.05, TY + 0.55, dc - 0.62), 0.12, MO, sides=14)
        F.cyl((g + 1.05, TY + 0.55, dc - 0.62), (g + 1.05, TY + 0.55, dc - 0.68), 0.13, DS, sides=14)
        F.box(g - 1.3, g - 1.0, TY + 0.32, TY + 0.6, dc - 0.25, dc + 0.25, "ih_panel")             # its controls
        for xr in rope_x:
            F.cyl((g + xr, DY - drum_r, dc), (g + xr, low, dc), 0.011, "ih_rope", sides=4)
        hook(low)
    def rake(low):
        top = low - 0.12                                         # under its rope sockets
        rd = 8.95
        for xs in (g - 0.7, g + 0.7):
            F.box(xs - 0.06, xs + 0.06, top, low, rd - 0.05, rd + 0.05, YE)                             # rope sockets
        F.box(g - 1.75, g + 1.75, top - 0.03, top, rd - 0.3, rd + 0.12, YE)                             # debris apron
        F.box(g - 1.75, g + 1.75, top - 0.38, top - 0.03, rd - 0.08, rd + 0.08, YE)                     # carriage beam
        F.box(g - 1.75, g + 1.75, top - 0.43, top - 0.38, rd - 0.2, rd + 0.12, YE)                      # rake plate
        for k in range(34):
            xt = g - 1.7 + 0.1 * (k + 0.5)
            F.box(xt - 0.012, xt + 0.012, top - 0.68, top - 0.43, rd - 0.2, rd - 0.16, DS)              # tines
        for xs in (g - 1.79, g + 1.79):                                                                # guide rollers
            F.cyl((xs, top - 0.2, rd - 0.26), (xs, top - 0.2, rd + 0.06), 0.07, "ct_rubber", sides=12)
    def hookblock(low):
        F.box(g - 0.2, g + 0.2, low - 0.4, low, S_MID - 0.12, S_MID + 0.12, YE)
        for xs in (-0.1, 0.1):
            F.cyl((g + xs - 0.03, low - 0.12, S_MID), (g + xs + 0.03, low - 0.12, S_MID), 0.16, DS, sides=14)   # sheaves
        F.cyl((g, low - 0.4, S_MID), (g, low - 0.55, S_MID), 0.04, DS, sides=8)
        F.box(g - 0.03, g + 0.03, low - 0.85, low - 0.55, S_MID - 0.04, S_MID + 0.04, "ih_hook")
        F.box(g - 0.03, g + 0.15, low - 0.9, low - 0.85, S_MID - 0.04, S_MID + 0.04, "ih_hook")
        F.box(g + 0.12, g + 0.18, low - 0.85, low - 0.72, S_MID - 0.04, S_MID + 0.04, "ih_hook")
        F.box(g + 0.02, g + 0.13, low - 0.62, low - 0.6, S_MID - 0.03, S_MID + 0.03, "ct_yellow")   # safety latch
    trolley(8.95, 0.28, (-0.7, 0.7), min(rake_y, DECK + 3.0) + 0.38, rake)
    trolley(S_MID, 0.22, (-0.13, -0.07, 0.07, 0.13), DECK + 3.9, hookblock)
    # the cable reel on the east land-side leg, its cable hanging into the trench's feed funnel
    tc = sum(TRENCH) / 2
    cx = g + 2.2                                     # the cable's guide, on an arm off the carriage's east end
    rx, rd = cx - 0.55, 1.78                         # the reel: its cable leaves its east side straight down to the guide
    F.box(rx - 0.08, rx + 0.08, DECK + 2.4, DECK + 3.8, rd + 0.17, RAILS_D[1] - 0.2, YE)
    F.cyl((rx, DECK + 3.1, rd + 0.17), (rx, DECK + 3.1, rd - 0.17), 0.55, "ih_reel", sides=20)
    F.cyl((rx, DECK + 3.1, rd - 0.17), (rx, DECK + 3.1, rd - 0.25), 0.2, MO, sides=14)
    F.cyl((cx, DECK + 3.1, rd), (cx, DECK + 0.5, rd), 0.025, "ct_rubber", sides=6)
    F.cyl((cx, DECK + 0.5, rd), (cx, DECK + 0.36, tc), 0.025, "ct_rubber", sides=6)
    F.cyl((cx, DECK + 0.36, tc), (cx, DECK - 0.3, tc), 0.025, "ct_rubber", sides=6)               # into the slot
    F.cyl((cx, DECK - 0.3, tc), (W_ / 2, DECK - 0.3, tc), 0.025, "ct_rubber", sides=6)            # to the feed point
    F.box(g + 1.75, g + 2.32, CB - 0.04, CB, tc - 0.08, RAILS_D[1] - 0.2, YE)
    for s_ in (-1, 1):
        F.box(cx + s_ * 0.1 - 0.01, cx + s_ * 0.1 + 0.01, DECK + 0.18, CB - 0.04, tc - 0.08, tc + 0.08, YE)   # the fork
        F.cyl((cx - 0.09, DECK + 0.24, tc + s_ * 0.05), (cx + 0.09, DECK + 0.24, tc + s_ * 0.05), 0.022, DS, sides=8)
        F.cyl((cx + s_ * 0.05, DECK + 0.32, tc - 0.07), (cx + s_ * 0.05, DECK + 0.32, tc + 0.07), 0.022, DS, sides=8)
    # beacon and horn on the lake-side end tie; the SWL plate on the land side; the pendant
    F.cyl((g, GT, RAILS_D[0]), (g, GT + 0.08, RAILS_D[0]), 0.1, DS, sides=12)
    F.cyl((g, GT + 0.08, RAILS_D[0]), (g, GT + 0.24, RAILS_D[0]), 0.08, "ih_beacon", sides=12, lit=True)
    F.box(g + 0.3, g + 0.45, GT - 0.25, GT - 0.12, RAILS_D[0] + 0.22, RAILS_D[0] + 0.32, DS)
    F.cyl((g + 0.375, GT - 0.185, RAILS_D[0] + 0.32), (g + 0.375, GT - 0.185, RAILS_D[0] + 0.42), 0.07, DS, sides=12)
    F.box(g - 0.6, g + 0.6, GB + 0.3, GB + 0.6, RAILS_D[1] - 0.225, RAILS_D[1] - 0.22, {"in": tag(4)})
    F.cyl((g + 0.9, GB - 0.03, RAILS_D[1] - 0.3), (g + 0.9, DECK + 1.5, RAILS_D[1] - 0.3), 0.01, "ih_rope", sides=4)
    F.box(g + 0.84, g + 0.96, DECK + 1.15, DECK + 1.5, RAILS_D[1] - 0.35, RAILS_D[1] - 0.25, "ih_pendant")


def screens_textures():
    out = {}
    sheet = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    for i, text in enumerate(TAGS):
        c, r = i % 4, i // 4
        rng = np.random.default_rng(300 + i)
        a = np.ones((64, 256, 3)) * np.array((198, 202, 204), float) + np.repeat(rng.normal(0, 4, (64, 1)), 256, 1)[..., None]
        cell = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(cell)
        d.rectangle([0, 0, 255, 63], outline=(140, 144, 146, 255), width=2)
        for x in (14, 241):
            d.ellipse([x - 6, 26, x + 6, 38], fill=(160, 164, 166, 255), outline=(110, 112, 114, 255))
        size = 34
        while d.textlength(text, font=ImageFont.truetype(FB, size)) > 206 and size > 16:
            size -= 1
        d.text((128, 33), text, font=ImageFont.truetype(FB, size), fill=(36, 38, 40, 255), anchor="mm")
        sheet.paste(cell, (c * 256, r * 64))
    out["ih_tags"] = sheet

    def flat(rgb, seed, n=32, dirt=0.04):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    m = Image.new("RGBA", (64, 64), (0, 0, 0, 0))                    # 3/8 in mesh: wires every 4 px, see-through
    d = ImageDraw.Draw(m)
    for k in range(0, 64, 4):
        d.line([(k, 0), (k, 63)], fill=(150, 156, 152, 255))
        d.line([(0, k), (63, k)], fill=(130, 136, 132, 255))
    out["ih_mesh"] = m
    h = flat((148, 154, 152), 101, n=64, dirt=0.02)                 # galvanised sheet with its seams
    dh = ImageDraw.Draw(h)
    for x in (0, 31):
        dh.line([(x, 0), (x, 63)], fill=(108, 112, 110, 255), width=2)
        for y in range(4, 64, 8):                                      # the seams' bolts
            for xb in (x + 3, x - 3 if x else 61):
                dh.rectangle([xb - 1, y - 1, xb, y], fill=(176, 180, 178, 255))
                dh.point((xb + 1, y + 1), fill=(96, 100, 98, 255))
    out["ih_hood"] = h
    out["ih_hood_in"] = flat((96, 102, 100), 120, n=32, dirt=0.03)      # inside: wet, darker
    out["ih_crane"] = flat((232, 184, 22), 129, n=32, dirt=0.025)        # the gantry's safety yellow enamel
    sp = flat((54, 56, 58), 121, n=64, dirt=0.03)                      # the head sprocket: hub, spokes, teeth
    dsp = ImageDraw.Draw(sp)
    for k in range(12):
        a = 2 * math.pi * k / 12
        dsp.line([(32, 32), (32 + 28 * math.cos(a), 32 + 28 * math.sin(a))], fill=(84, 86, 88, 255), width=3)
    dsp.ellipse([24, 24, 40, 40], fill=(96, 98, 100, 255))
    out["ih_sprocket"] = sp
    out["ih_spray"] = Image.new("RGBA", (16, 16), (214, 232, 240, 120))
    cw = flat((40, 42, 44), 122, n=32)
    for y in range(4, 32, 5):
        ImageDraw.Draw(cw).line([(3, y), (28, y)], fill=(16, 16, 18, 255), width=2)     # the fan cowl's slots
    out["ih_cowl"] = cw
    out["ih_pvc"] = flat((120, 124, 128), 123, n=16)
    out["ih_btn_g"] = flat((30, 140, 60), 124, n=16, dirt=0.02)
    out["ih_btn_r"] = flat((190, 30, 26), 125, n=16, dirt=0.02)
    out["ih_lamp_g"] = Image.new("RGBA", (16, 16), (90, 255, 120, 255))
    out["ih_lamp_off"] = flat((30, 70, 40), 126, n=16, dirt=0.02)
    out["ih_lcs_side"] = flat((168, 170, 168), 127, n=32, dirt=0.02)   # a grey NEMA 4X enclosure
    # its face (0.32 x 0.40 m, 160 x 200 px): door seam, hinges, legend plates under the selector, lamp and buttons
    lc = flat((172, 174, 172), 128, n=1, dirt=0.0).resize((160, 200))
    dl_ = ImageDraw.Draw(lc)
    dl_.rectangle([3, 3, 156, 196], outline=(120, 122, 120, 255), width=2)
    for y in (30, 170):
        dl_.rectangle([0, y - 8, 4, y + 8], fill=(110, 112, 110, 255))
    FR = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    for cx_, cy_, text in ((45, 82, "HAND OFF AUTO"), (115, 82, "RUNNING"), (45, 168, "START"), (115, 168, "STOP")):
        dl_.rectangle([cx_ - 32, cy_ - 8, cx_ + 32, cy_ + 8], fill=(26, 26, 28, 255))
        size = 13
        while dl_.textlength(text, font=ImageFont.truetype(FR, size)) > 60 and size > 7:
            size -= 1
        dl_.text((cx_, cy_), text, font=ImageFont.truetype(FR, size), fill=(236, 236, 236, 255), anchor="mm")
    for cx_, cy_ in ((45, 55), (115, 55), (45, 140), (115, 140)):               # the devices' bezels
        dl_.ellipse([cx_ - 20, cy_ - 20, cx_ + 20, cy_ + 20], fill=(140, 142, 140, 255), outline=(96, 98, 96, 255))
    out["ih_lcs"] = lc
    # the level transmitters' faces: A upstream of the screen, B downstream (clean screens: 50 mm across them)
    lt = Image.new("RGBA", (256, 128), (60, 64, 70, 255))
    dlt = ImageDraw.Draw(lt)
    for k, val in enumerate(("6.00 m", "5.95 m")):
        x = 128 * k
        dlt.rectangle([x + 2, 2, x + 125, 125], outline=(36, 38, 42, 255), width=3)
        dlt.rectangle([x + 16, 22, x + 111, 66], fill=(150, 176, 120, 255), outline=(30, 30, 30, 255))
        dlt.text((x + 64, 44), val, font=ImageFont.truetype(FR, 26), fill=(20, 26, 16, 255), anchor="mm")
        for bx_ in (36, 64, 92):
            dlt.rectangle([x + bx_ - 9, 86, x + bx_ + 9, 100], fill=(30, 30, 34, 255))
    out["ih_lt_face"] = lt
    # pipe markers (ASME A13.1: water, white on green), the flow arrow right (top row) and left (bottom row)
    mk = Image.new("RGBA", (512, 128), (0, 120, 60, 255))
    dmk = ImageDraw.Draw(mk)
    for r, right in ((0, True), (1, False)):
        y = 64 * r + 32
        dmk.text((256 + (-30 if right else 30), y), "SCREEN WASH", font=ImageFont.truetype(FR, 40), fill=(255, 255, 255, 255),
                 anchor="mm")
        ax = 470 if right else 42
        sgn = 1 if right else -1
        dmk.rectangle([min(ax - sgn * 50, ax - sgn * 10), y - 7, max(ax - sgn * 50, ax - sgn * 10), y + 7], fill=(255, 255, 255, 255))
        dmk.polygon([(ax + sgn * 20, y), (ax - sgn * 12, y - 20), (ax - sgn * 12, y + 20)], fill=(255, 255, 255, 255))
    out["ih_marker"] = mk
    out["ih_hood_top"] = flat((140, 146, 144), 102, n=32)
    dr = flat((152, 158, 156), 103, n=64, dirt=0.02)
    ImageDraw.Draw(dr).rectangle([1, 1, 62, 62], outline=(100, 104, 102, 255), width=2)
    out["ih_hood_door"] = dr
    out["ih_motor"] = flat((46, 84, 120), 104)                       # motor enamel
    out["ih_reducer"] = flat((52, 90, 126), 105)
    out["ih_valve"] = flat((40, 70, 110), 106)
    out["ih_solenoid"] = flat((40, 40, 44), 107)
    out["ih_gauge"] = flat((30, 30, 32), 108, n=16)
    g = Image.new("RGBA", (64, 64), (240, 240, 232, 255))
    dg = ImageDraw.Draw(g)
    for k in range(9):
        a = math.radians(225 - 270 * k / 8)
        dg.line([(32 + 22 * math.cos(a), 32 - 22 * math.sin(a)), (32 + 28 * math.cos(a), 32 - 28 * math.sin(a))], fill=(20, 20, 20, 255), width=2)
    dg.line([(32, 32), (32 + 20 * math.cos(math.radians(60)), 32 - 20 * math.sin(math.radians(60)))], fill=(200, 20, 20, 255), width=2)
    out["ih_gauge_face"] = g
    out["ih_rope"] = flat((70, 70, 66), 109, n=16)
    out["ct_rubber"] = flat((24, 24, 24), 110, n=16)
    out["ih_skip"] = flat((40, 92, 60), 111)                         # a green steel skip
    db_ = flat((86, 74, 52), 112, n=64, dirt=0.25)                   # leaves, sticks, weed
    out["ih_debris"] = db_
    hz = Image.new("RGBA", (64, 64), (235, 182, 20, 255))            # hazard stripes, black on yellow at 45 degrees
    dz = ImageDraw.Draw(hz)
    for k in range(-64, 128, 32):
        dz.polygon([(k, 0), (k + 16, 0), (k + 16 - 64, 64), (k - 64, 64)], fill=(24, 24, 24, 255))
    out["ih_hazard"] = hz
    out["ih_wheel"] = flat((60, 62, 64), 113)
    dm = flat((70, 72, 74), 114, n=32)
    ddm = ImageDraw.Draw(dm)
    for y in range(0, 32, 3):                                           # rope grooves
        ddm.line([(0, y), (31, y)], fill=(44, 46, 48, 255))
    out["ih_drum"] = dm
    out["ih_panel"] = flat((200, 200, 194), 115)
    out["ih_hook"] = flat((40, 40, 42), 116)
    out["ih_reel"] = flat((222, 170, 24), 117)
    out["ih_beacon"] = Image.new("RGBA", (16, 16), (255, 150, 20, 255))
    out["ih_pendant"] = flat((230, 180, 30), 118, n=16)
    return out



# ================================================================== piece 3: the make-up and service water pumps
#
# Real-world basis: vertical wet-pit turbine pumps (US practice for lake intakes): the bowl assembly with its suction
# bell hangs near the bay floor on a flanged column; the above-deck discharge head (on a soleplate grouted to the deck)
# carries the side discharge nozzle, the stuffing box and, in its open motor stand (guarded), the coupling; a vertical
# hollow-shaft motor (weather protected type I, 4160 V) stands on top with its non-reverse ratchet under the top cap.
# Each discharge: an air and vacuum valve, a pressure gauge, a dismantling joint, a tilting-disc check valve (lever and
# counterweight outside), a motor-operated butterfly valve; then the make-up lines run straight to the deck's back edge
# and go down into the ground, and the service water pumps of a train join their train's header, pass the train's
# automatic backwash strainer and its flow meter, and go down into the ground the same way (buried lines to the towers'
# basins and to the plant's coolers). Make-up: 2 x 100 %, 15,000 gpm at 160 ft, 800 hp, 24 in. Service water: two
# trains of 2 x 100 %, 12,000 gpm at 230 ft, 1000 hp, 20 in, the train's line 24 in. Live: which pumps run (one make-up
# and one per train normally), the check valves' levers, the valves' positions and lamps, gauges and flow meters.

TAGS3 = ["MUP-A", "SWP-1A", "SWP-1B", "SWP-2A", "SWP-2B", "MUP-B",
         "MOV-MU-A", "MOV-SW-1A", "MOV-SW-1B", "MOV-SW-2A", "MOV-SW-2B", "MOV-MU-B",
         "FT-MU-A", "FT-SW-1", "FT-SW-2", "FT-MU-B", "SW STRAINER 1", "SW STRAINER 2", "MOV-BW-1", "MOV-BW-2",
         "CV-MU-A", "CV-SW-1A", "CV-SW-1B", "CV-SW-2A", "CV-SW-2B", "CV-MU-B"]
PD = -0.6                                      # the pumps' centre line (the soleplate openings' middle)
CY = DECK + 0.84                               # the discharge lines' centre height
D_EDGE = D_BACK                                # the deck's back edge (the lines go down into the ground past it)
SP = 0.09                                      # grout pad and soleplate under each discharge head
GV, DS = "ct_galv", "ct_steel_dark"


def tag3(i):
    c, r = i % 4, i // 4
    return ["ih_tags3", c / 4, r / 8, (c + 1) / 4, (r + 1) / 8]


def plate3(i):
    """Stamped stainless nameplate i (4 across, 4 down)."""
    c, r = i % 4, i // 4
    return ["ih_nameplates", c / 4, r / 4, (c + 1) / 4, (r + 1) / 4]


def pump_list():
    """[(index, bay, x, make-up?)] west to east."""
    out = []
    for i in range(NB):
        for xa, xb in pump_holes(i):
            out.append((len(out), i, (xa + xb) / 2, BAYS[i].startswith("MU")))
    return out


def _axis_pt(axis, x, y, d, s):
    return (x + s, y, d) if axis == "x" else ((x, y + s, d) if axis == "y" else (x, y, d + s))


def _flange(F, x, y, da, db, r, mat, bolts=12, axis="d"):
    """A bolted flange joint from da to db along the axis (through (x, y) or the axis's other coordinates): two
    flanges, the gasket between, studs with nuts both sides."""
    T = db - da
    at = (lambda s: (x, y, s)) if axis == "d" else (lambda s: (s, y, x))   # (along x, (x, y) are read as (d, y))
    F.cyl(at(da), at(da + 0.42 * T), r, mat, sides=28)
    F.cyl(at(da + 0.42 * T), at(db - 0.42 * T), r - 0.035, "ih_gasket", sides=28)
    F.cyl(at(db - 0.42 * T), at(db), r, mat, sides=28)
    sgn = 1 if T > 0 else -1
    for k in range(bolts):
        a = 2 * math.pi * (k + 0.5) / bolts
        u, v = (r - 0.032) * math.cos(a), (r - 0.032) * math.sin(a)
        if axis == "d":
            p = lambda s: (x + u, y + v, s)
        else:
            p = lambda s: (s, y + v, x + u)
        F.cyl(p(da - sgn * 0.03), p(db + sgn * 0.03), 0.011, DS, sides=6)
        F.cyl(p(da - sgn * 0.02), p(da), 0.02, GV, sides=6)                               # nuts
        F.cyl(p(db), p(db + sgn * 0.02), 0.02, GV, sides=6)


def _weld(F, a, b, r):
    """A girth weld bead round a line, centred between a and b (2 cm)."""
    F.cyl(a, b, r + 0.007, "ih_weld", sides=28)


def _gauge(F, x, y, d, val, vmax, facing=-1, r=0.07, dial="ih_dial"):
    """A pressure gauge on a root valve, facing -d (facing -1) or +d: case with bezel, dial, a needle at val (live)."""
    F.cyl((x, y, d), (x, y, d + facing * 0.045), r, "ih_gauge", dial, sides=20)
    F.cyl((x, y, d + facing * 0.045), (x, y, d + facing * 0.05), r + 0.005, GV, "ih_glass", sides=20)
    a = math.radians(225 - 270 * max(0.0, min(val, vmax)) / vmax)
    df = d + facing * 0.051
    F.bar((x, y, df), (x + 0.8 * r * math.cos(a), y + 0.8 * r * math.sin(a), df), 0.007, 0.003, "ih_needle", up="r")
    F.cyl((x, y - r, d + facing * 0.02), (x, y - r - 0.03, d + facing * 0.02), 0.012, "ct_brass", sides=8)   # stem


def _conduit(F, pts, r, mat=GV):
    """Rigid conduit through the points: couplings every 1.5 m, a conduit body (LB) at each bend."""
    for a, b in zip(pts, pts[1:]):
        a, b = np.array(a, float), np.array(b, float)
        L = float(np.linalg.norm(b - a))
        F.cyl(tuple(a), tuple(b), r, mat, sides=8)
        for k in range(1, int(L / 1.5) + 1):
            c = a + (b - a) * (k * 1.5 / L)
            e = (b - a) / L * 0.03
            F.cyl(tuple(c - e), tuple(c + e), r + 0.008, mat, sides=8)
    for p in pts[1:-1]:
        F.box(p[0] - r - 0.02, p[0] + r + 0.02, p[1] - r - 0.02, p[1] + r + 0.02, p[2] - r - 0.02, p[2] + r + 0.02, mat)


def _ball_valve(F, x, y, d, along, r=0.03, open_=True):
    """A small ball valve on a line along 'x', 'y' or 'd', its lever (in line when open)."""
    a, b = _axis_pt(along, x, y, d, -0.05), _axis_pt(along, x, y, d, 0.05)
    F.cyl(a, b, r, "ct_brass", sides=8)
    if along == "y":                                  # on a riser: the stem out to the east, the lever on it
        s0 = (x + r + 0.045, y, d)
        F.cyl((x, y, d), s0, 0.008, DS, sides=6)
        tip = (s0[0], y + 0.12, d) if open_ else (s0[0], y, d - 0.12)
        F.bar(s0, tip, 0.02, 0.006, "ct_wheel", up="t")
    else:
        s0 = (x, y + r + 0.03, d)
        F.cyl((x, y, d), s0, 0.008, DS, sides=6)
        tip = _axis_pt(along if open_ else ("x" if along != "x" else "d"), *s0, 0.12)
        F.bar(s0, tip, 0.02, 0.006, "ct_wheel", up="y")


def _stand(F, x, d, ytop, rp, along_x=False):
    """A pipe stand: anchored base plate, H-section post, cap plate and shoe, a U-bolt over the line with its nuts."""
    F.box(x - 0.15, x + 0.15, DECK, DECK + 0.02, d - 0.15, d + 0.15, DS)
    for a in (-0.11, 0.11):
        for b in (-0.11, 0.11):
            F.cyl((x + a, DECK + 0.02, d + b), (x + a, DECK + 0.05, d + b), 0.014, GV, sides=6)
    yc = ytop - 0.045                                         # the post's top, under the cap and shoe
    if along_x:                                              # (the line runs along x: the H's web along x)
        for s in (-1, 1):
            F.box(x - 0.06, x + 0.06, DECK + 0.02, yc, d + s * 0.06 - 0.006, d + s * 0.06 + 0.006, DS)
        F.box(x - 0.006, x + 0.006, DECK + 0.02, yc, d - 0.054, d + 0.054, DS)
        F.box(x - 0.09, x + 0.09, yc, yc + 0.015, d - 0.2, d + 0.2, DS)
        F.box(x - 0.08, x + 0.08, yc + 0.015, ytop - 0.002, d - 0.012, d + 0.012, DS)
    else:
        for s in (-1, 1):
            F.box(x + s * 0.06 - 0.006, x + s * 0.06 + 0.006, DECK + 0.02, yc, d - 0.06, d + 0.06, DS)
        F.box(x - 0.054, x + 0.054, DECK + 0.02, yc, d - 0.006, d + 0.006, DS)
        F.box(x - 0.2, x + 0.2, yc, yc + 0.015, d - 0.09, d + 0.09, DS)
        F.box(x - 0.012, x + 0.012, yc + 0.015, ytop - 0.002, d - 0.08, d + 0.08, DS)
    # the U-bolt: round bar over the line from one side of the cap plate to the other
    yl = ytop + rp                                            # the line's centre
    n = 10
    for k in range(n):
        a0, a1 = math.pi * k / n, math.pi * (k + 1) / n
        R = rp + 0.012
        if along_x:
            P0 = (x, yl + R * math.sin(a0), d + R * math.cos(a0)); P1 = (x, yl + R * math.sin(a1), d + R * math.cos(a1))
        else:
            P0 = (x + R * math.cos(a0), yl + R * math.sin(a0), d); P1 = (x + R * math.cos(a1), yl + R * math.sin(a1), d)
        F.cyl(P0, P1, 0.01, GV, sides=6)
    for s in (-1, 1):
        R = rp + 0.012
        p = (x, yl, d + s * R) if along_x else (x + s * R, yl, d)
        F.cyl(p, (p[0], yc + 0.015, p[2]), 0.01, GV, sides=6)
        F.cyl((p[0], yc + 0.015, p[2]), (p[0], yc + 0.035, p[2]), 0.02, GV, sides=6)
        F.cyl((p[0], yc - 0.02, p[2]), (p[0], yc, p[2]), 0.02, GV, sides=6)


def _elbow_down(F, x, d0, y0, R, r, mat):
    """A four-piece mitred bend from a line along -d at height y0 (its straight run ending at d0) turning straight
    down, its welds; returns the vertical's (d, y) start."""
    pts = [(d0 - R * math.sin(math.radians(a)), y0 - R + R * math.cos(math.radians(a))) for a in (0, 22.5, 45, 67.5, 90)]
    for (da, ya), (db, yb) in zip(pts, pts[1:]):
        F.cyl((x, ya, da), (x, yb, db), r, mat, sides=28)
    for k, (dd, yy) in enumerate(pts):
        if 0 < k < len(pts) - 1:
            a = math.radians(22.5 * k)
            n = np.array([0.0, -math.sin(a), -math.cos(a)]) * 0.01   # along the bend's tangent (y, d)
            F.cyl((x, yy - n[1], dd - n[2]), (x, yy + n[1], dd + n[2]), r + 0.007, "ih_weld", sides=28)
    return pts[-1]


def _into_ground(F, x, d, y, r, rf):
    """The line's last flange pair at the grass, down into a concrete thrust block (the buried line beyond)."""
    F.cyl((x, y, d), (x, 0.47, d), r, "ih_pipe", sides=28)
    _weld(F, (x, 0.46, d), (x, 0.48, d), r)
    F.cyl((x, 0.48, d), (x, 0.42, d), rf, "ih_pipe", sides=28)
    F.cyl((x, 0.42, d), (x, 0.38, d), rf - 0.035, "ih_gasket", sides=28)
    F.cyl((x, 0.38, d), (x, 0.32, d), rf, "ih_pipe", sides=28)
    for k in range(16):
        a = 2 * math.pi * (k + 0.5) / 16
        u, v = (rf - 0.032) * math.cos(a), (rf - 0.032) * math.sin(a)
        F.cyl((x + u, 0.5, d + v), (x + u, 0.3, d + v), 0.011, DS, sides=6)
        F.cyl((x + u, 0.48, d + v), (x + u, 0.5, d + v), 0.02, GV, sides=6)
        F.cyl((x + u, 0.3, d + v), (x + u, 0.32, d + v), 0.02, GV, sides=6)
    F.cyl((x, 0.32, d), (x, 0.0, d), r, "ih_pipe", sides=28)
    h = r + 0.4
    C = "ih_concrete"
    F.box(x - h, x + h, -0.6, 0.12, d - h, d + h, {"out": C, "in": C, "left": C, "right": C, "top": C, "bottom": None})
    F.box(x - h + 0.04, x + h - 0.04, 0.12, 0.16, d - h + 0.04, d + h - 0.04, {"out": C, "in": C, "left": C, "right": C,
          "top": C, "bottom": None})                                                   # its chamfered top


def _ring_ribs(F, x, d, R, y0, y1, n, skip=(), w=0.024, depth=0.045, mat="ih_mpaint"):
    """n radial ribs round a vertical cylinder of radius R at (x, d) from y0 to y1 (skipping angles in skip)."""
    for k in range(n):
        a = 2 * math.pi * (k + 0.5) / n
        if any(abs((a - s + math.pi) % (2 * math.pi) - math.pi) < 0.35 for s in skip):
            continue
        Fs = F.sub(x, d, a - math.pi / 2)
        Fs.box(-w / 2, w / 2, y0, y1, R - 0.01, R + depth, mat)


def pumps_ops(ox=0.0, oz=0.0, running=(True, True, False, True, False, False), mov_open=(True,) * 6, bw=(False, False)):
    """Piece 3: the six pumps, their discharges, the service water trains' headers and strainers, the lines into the
    ground. Live: running (per pump), mov_open (per pump's discharge valve), bw (a strainer backwashing)."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    CA, PI, MP = "ih_cast", "ih_pipe", "ih_mpaint"
    for n, bay, px, mu in pump_list():
        run = running[n]
        rh, rm, rp, rf = (0.48, 0.62, 0.305, 0.44) if mu else (0.42, 0.54, 0.254, 0.38)
        rc, rb = (0.3, 0.46) if mu else (0.25, 0.4)
        y0 = DECK + SP
        # ---- below the deck: column in flanged lengths, two bowls, suction bell 0.5 m off the floor
        F.cyl((px, FLOOR + 2.3, PD), (px, DECK - 0.002, PD), rc, PI, sides=24)
        for y in (FLOOR + 2.3, -3.9, -0.9):
            F.cyl((px, y, PD), (px, y + 0.06, PD), rc + 0.08, PI, sides=24)
        F.cyl((px, FLOOR + 1.05, PD), (px, FLOOR + 2.3, PD), rb, CA, sides=24)
        for y in (FLOOR + 1.65, FLOOR + 2.24):
            F.cyl((px, y, PD), (px, y + 0.06, PD), rb + 0.05, CA, sides=24)
        F.cyl((px, FLOOR + 0.75, PD), (px, FLOOR + 1.05, PD), rb + 0.08, CA, sides=24)
        F.cyl((px, FLOOR + 0.5, PD), (px, FLOOR + 0.75, PD), rb + 0.22, CA, sides=24)
        # ---- grout pad, soleplate (machined top, levelling screws, anchor bolts with nuts)
        gh = 0.86 if mu else 0.82
        F.box(px - gh, px + gh, DECK, DECK + 0.04, PD - 0.96, PD + 0.96, "ih_grout")
        F.box(px - 0.8, px + 0.8, DECK + 0.04, y0, PD - 0.9, PD + 0.9, {"top": "ih_machined", "out": DS, "in": DS,
              "left": DS, "right": DS, "bottom": DS})
        for a in (-0.7, 0.7):
            for b in (-0.8, 0.8):
                F.cyl((px + a, y0, PD + b), (px + a, y0 + 0.07, PD + b), 0.018, GV, sides=6)
                F.cyl((px + a, y0, PD + b), (px + a, y0 + 0.03, PD + b), 0.032, GV, sides=6)
        # ---- discharge head: base flange with bolts, gussets, body, top flange; nozzle toward the land with gussets
        F.cyl((px, y0, PD), (px, y0 + 0.08, PD), rh + 0.12, CA, sides=32)
        for k in range(12):
            a = 2 * math.pi * (k + 0.5) / 12
            bx, bd = px + (rh + 0.07) * math.cos(a), PD + (rh + 0.07) * math.sin(a)
            F.cyl((bx, y0 + 0.08, bd), (bx, y0 + 0.11, bd), 0.022, GV, sides=6)
        F.cyl((px, y0 + 0.08, PD), (px, DECK + 1.25, PD), rh, CA, sides=32)
        F.cyl((px, DECK + 1.25, PD), (px, DECK + 1.31, PD), rh + 0.06, CA, sides=32)
        for k in range(8):
            a = 2 * math.pi * (k + 0.5) / 8
            Fs = F.sub(px, PD, a - math.pi / 2)
            Fs.prism([(rh - 0.01, y0 + 0.08), (rh + 0.11, y0 + 0.08), (rh + 0.11, y0 + 0.11), (rh - 0.01, y0 + 0.38)],
                     -0.012, 0.012, CA)
        dn = PD - rh - 0.32                          # the nozzle's flange face
        F.cyl((px, CY, PD - rh + 0.12), (px, CY, dn), rp, CA, sides=28)
        for s in (-1, 1):
            F.prism([(PD - rh + 0.02, y0 + 0.08), (dn + 0.08, CY - rp + 0.05), (dn + 0.08, CY - rp + 0.02),
                     (PD - rh + 0.02, y0 + 0.11)], px + s * 0.12 - 0.012, px + s * 0.12 + 0.012, CA)
        F.box(px + rh - 0.012, px + rh + 0.004, DECK + 0.92, DECK + 1.1, PD - 0.13, PD + 0.13, {"right": plate3(6 + n)})
        # stuffing box with its gland bolts, leak-off to the head's drain; shaft, coupling (striped: turns), guard
        F.cyl((px, DECK + 1.31, PD), (px, DECK + 1.44, PD), 0.13, CA, sides=16)
        F.cyl((px, DECK + 1.44, PD), (px, DECK + 1.5, PD), 0.17, CA, sides=16)
        for k in range(2):
            a = math.pi * k
            F.cyl((px + 0.13 * math.cos(a), DECK + 1.44, PD + 0.13 * math.sin(a)),
                  (px + 0.13 * math.cos(a), DECK + 1.58, PD + 0.13 * math.sin(a)), 0.012, GV, sides=6)
        F.cyl((px, DECK + 1.5, PD), (px, DECK + 2.2, PD), 0.055, "ih_shaft", sides=12)
        F.cyl((px, DECK + 1.66, PD), (px, DECK + 1.74, PD), 0.15, "ih_coupling", sides=18)
        F.cyl((px, DECK + 1.74, PD), (px, DECK + 1.82, PD), 0.15, "ih_coupling", sides=18)
        for k in range(6):
            a = 2 * math.pi * k / 6
            F.cyl((px + 0.11 * math.cos(a), DECK + 1.64, PD + 0.11 * math.sin(a)),
                  (px + 0.11 * math.cos(a), DECK + 1.84, PD + 0.11 * math.sin(a)), 0.012, DS, sides=6)
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            Fs = F.sub(px, PD, a - math.pi / 2)
            Fs.box(-0.07, 0.07, DECK + 1.31, DECK + 2.2, rh - 0.13, rh - 0.01, CA)
        F.cyl((px, DECK + 1.33, PD), (px, DECK + 2.18, PD), rh - 0.005, "ih_guard", sides=28)
        F.cyl((px, DECK + 2.2, PD), (px, DECK + 2.26, PD), rh + 0.06, CA, sides=32)
        # ---- the motor: flange with bolts, ribbed frame, louvred top band, drip cover, top cap with eyebolts, ratchet
        F.cyl((px, DECK + 2.26, PD), (px, DECK + 2.34, PD), rm - 0.04, MP, sides=32)
        for k in range(12):
            a = 2 * math.pi * (k + 0.5) / 12
            bx, bd = px + (rh + 0.02) * math.cos(a), PD + (rh + 0.02) * math.sin(a)
            F.cyl((bx, DECK + 2.34, bd), (bx, DECK + 2.37, bd), 0.02, GV, sides=6)
        F.cyl((px, DECK + 2.34, PD), (px, DECK + 3.3, PD), rm, MP, sides=32)
        _ring_ribs(F, px, PD, rm, DECK + 2.42, DECK + 3.26, 28, skip=(math.pi / 2,))
        F.cyl((px, DECK + 3.3, PD), (px, DECK + 3.62, PD), rm + 0.005, "ih_louvre", sides=32)
        F.cyl((px, DECK + 3.62, PD), (px, DECK + 3.67, PD), rm + 0.045, MP, sides=32)
        F.cyl((px, DECK + 3.67, PD), (px, DECK + 3.8, PD), rm + 0.06, MP, sides=32)
        F.cyl((px, DECK + 3.8, PD), (px, DECK + 3.98, PD), rm * 0.62, MP, sides=28)
        F.cyl((px, DECK + 3.98, PD), (px, DECK + 4.1, PD), 0.19, MP, sides=20)
        F.cyl((px, DECK + 4.1, PD), (px, DECK + 4.13, PD), 0.12, MP, sides=16)
        for s in (-1, 1):                                                           # eyebolts on the top cap
            ex = px + s * rm * 0.42
            F.cyl((ex, DECK + 3.98, PD), (ex, DECK + 4.05, PD), 0.016, GV, sides=6)
            for k in range(8):
                a0, a1 = 2 * math.pi * k / 8, 2 * math.pi * (k + 1) / 8
                F.cyl((ex + 0.045 * math.cos(a0), DECK + 4.1 + 0.045 * math.sin(a0), PD),
                      (ex + 0.045 * math.cos(a1), DECK + 4.1 + 0.045 * math.sin(a1), PD), 0.011, GV, sides=6)
        for k in range(4):                                                          # lifting lugs
            a = math.radians(10) + k * math.pi / 2
            Fs = F.sub(px, PD, a - math.pi / 2)
            Fs.box(-0.025, 0.025, DECK + 3.42, DECK + 3.62, rm, rm + 0.08, MP)
        # main terminal box on the lake side: body, bolted cover; liquid-tight flex down to the rigid 4160 V conduit
        tb0, tb1 = PD + rm - 0.02, PD + rm + 0.3
        F.box(px - 0.22, px + 0.22, DECK + 2.5, DECK + 3.0, tb0, tb1, MP)
        F.box(px - 0.2, px + 0.2, DECK + 2.52, DECK + 2.98, tb1, tb1 + 0.018, {"out": "ih_mpaint", "left": MP, "right": MP,
              "top": MP, "bottom": MP})
        for a in (-0.17, 0.17):
            for y in (DECK + 2.55, DECK + 2.75, DECK + 2.95):
                F.cyl((px + a, y, tb1 + 0.018), (px + a, y, tb1 + 0.03), 0.012, GV, sides=6)
        F.box(px - 0.08, px + 0.08, DECK + 2.88, DECK + 2.94, tb1 + 0.018, tb1 + 0.02, {"out": "ih_danger"})
        F.cyl((px + 0.1, DECK + 2.5, tb1 - 0.12), (px + 0.1, DECK + 2.46, tb1 - 0.12), 0.06, GV, sides=10)   # connector
        fl = [(px + 0.1, DECK + 2.46, tb1 - 0.12), (px + 0.1, DECK + 2.3, tb1 - 0.08), (px + 0.1, DECK + 2.18, tb1 + 0.02),
              (px + 0.1, DECK + 2.1, 0.4)]
        for a, b in zip(fl, fl[1:]):
            F.cyl(a, b, 0.05, "ih_flex", sides=10)
        F.cyl((px + 0.1, DECK + 2.1, 0.4), (px + 0.1, DECK + 2.04, 0.4), 0.062, GV, sides=10)
        _conduit(F, [(px + 0.1, DECK + 2.04, 0.4), (px + 0.1, DECK - 0.15, 0.4)], 0.05)
        F.cyl((px + 0.1, DECK + 0.002, 0.4), (px + 0.1, DECK + 0.03, 0.4), 0.08, GV, sides=10)
        # the space heater's box and its conduit
        hd0 = PD + rm * 0.8
        F.box(px - 0.38, px - 0.24, DECK + 2.62, DECK + 2.76, hd0, hd0 + 0.14, MP)
        _conduit(F, [(px - 0.31, DECK + 2.62, hd0 + 0.08), (px - 0.31, DECK + 2.25, hd0 + 0.08), (px - 0.31, DECK + 2.25, 0.4),
                     (px - 0.31, DECK - 0.1, 0.4)], 0.016)
        # ground: a copper cable from the frame's pad to the deck's ground plate
        F.box(px + rm * 0.7 - 0.04, px + rm * 0.7 + 0.04, DECK + 2.4, DECK + 2.47, PD - rm * 0.72 - 0.02, PD - rm * 0.72 + 0.04,
              "ct_brass")
        gxc, gdc = px + rm * 0.7, PD - rm * 0.72 - 0.035                              # straight down to a clamp
        F.cyl((gxc, DECK + 2.42, gdc + 0.035), (gxc, DECK + 2.4, gdc), 0.01, "ih_copper", sides=6)
        F.cyl((gxc, DECK + 2.4, gdc), (gxc, y0 + 0.03, gdc), 0.01, "ih_copper", sides=6)
        F.box(gxc - 0.03, gxc + 0.03, y0, y0 + 0.04, gdc - 0.025, gdc + 0.025, "ct_brass")
        # nameplate and tag on the land side
        F.box(px - 0.16, px + 0.16, DECK + 2.62, DECK + 2.7, PD - rm - 0.006, PD - rm + 0.03, {"in": tag3(n)})
        F.box(px - 0.15, px + 0.15, DECK + 2.74, DECK + 2.94, PD - rm - 0.006, PD - rm + 0.03, {"in": plate3(n)})
        # ---- air and vacuum valve on the nozzle (isolating ball valve under it), its drain to the soleplate's hub
        da_ = PD - rh - 0.13
        F.cyl((px, CY + rp - 0.02, da_), (px, CY + rp + 0.08, da_), 0.025, GV, sides=8)
        _ball_valve(F, px, CY + rp + 0.11, da_, "y")
        F.cyl((px, CY + rp + 0.16, da_), (px, CY + rp + 0.2, da_), 0.06, CA, sides=12)
        F.cyl((px, CY + rp + 0.2, da_), (px, CY + rp + 0.42, da_), 0.075, CA, sides=16)
        F.cyl((px, CY + rp + 0.42, da_), (px, CY + rp + 0.45, da_), 0.09, CA, sides=16)
        F.cyl((px, CY + rp + 0.33, da_), (px + 0.3, CY + rp + 0.33, da_), 0.015, GV, sides=6)
        F.cyl((px + 0.3, CY + rp + 0.33, da_), (px + 0.3, y0 + 0.02, da_), 0.015, GV, sides=6)
        F.cyl((px + 0.3, y0, da_), (px + 0.3, y0 + 0.08, da_), 0.04, DS, sides=10)                  # drain hub
        # the head's pressure gauge on its root valve (live)
        gx = px - rp - 0.16
        F.cyl((px - rp + 0.02, CY, da_), (gx, CY, da_), 0.012, GV, sides=6)
        F.cyl((gx, CY, da_), (gx, CY + 0.1, da_), 0.012, GV, sides=6)
        _ball_valve(F, gx, CY + 0.13, da_, "y", r=0.022)
        F.cyl((gx, CY + 0.16, da_), (gx, CY + 0.3, da_), 0.012, GV, sides=6)
        _gauge(F, gx, CY + 0.3 + 0.08, da_ + 0.02, (65 if mu else 95) if run else 1, 160, r=0.075)
        # ---- dismantling joint: sleeve, two flanged ends, tie rods with nuts
        d = dn
        _flange(F, px, CY, d, d - 0.07, rf, CA, bolts=16 if mu else 12)
        d -= 0.07
        F.cyl((px, CY, d), (px, CY, d - 0.12), rp, PI, sides=28)
        F.cyl((px, CY, d - 0.12), (px, CY, d - 0.3), rp + 0.035, PI, sides=28)
        F.cyl((px, CY, d - 0.3), (px, CY, d - 0.4), rp, PI, sides=28)
        _flange(F, px, CY, d - 0.4, d - 0.47, rf, PI, bolts=16 if mu else 12)
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            u, v = (rf + 0.02) * math.cos(a), (rf + 0.02) * math.sin(a)
            F.cyl((px + u, CY + v, d + 0.05), (px + u, CY + v, d - 0.52), 0.014, GV, sides=6)
            for dd in (d + 0.03, d - 0.5):
                F.cyl((px + u, CY + v, dd), (px + u, CY + v, dd + 0.02), 0.025, GV, sides=6)
        d -= 0.47
        # ---- tilting-disc check valve: body with side ribs, flow arrow, bonnet and its bolts, drain plug; the lever
        _flange(F, px, CY, d, d - 0.07, rf, CA, bolts=16 if mu else 12)
        d -= 0.07
        rcv = rp + 0.08
        F.cyl((px, CY, d), (px, CY, d - 0.08), rp + 0.04, CA, sides=28)
        F.cyl((px, CY, d - 0.08), (px, CY, d - 0.62), rcv, CA, sides=28)
        F.cyl((px, CY, d - 0.62), (px, CY, d - 0.7), rp + 0.04, CA, sides=28)
        for s in (-1, 1):
            F.bar((px + s * rcv, CY, d - 0.1), (px + s * rcv, CY, d - 0.6), 0.06, 0.03, CA, up="y")
        F.cyl((px, CY + rcv - 0.04, d - 0.35), (px, CY + rcv + 0.06, d - 0.35), 0.22, CA, sides=24)    # bonnet
        F.cyl((px, CY + rcv + 0.06, d - 0.35), (px, CY + rcv + 0.12, d - 0.35), 0.18, CA, sides=24)
        for k in range(10):
            a = 2 * math.pi * k / 10
            F.cyl((px + 0.195 * math.cos(a), CY + rcv + 0.06, d - 0.35 + 0.195 * math.sin(a)),
                  (px + 0.195 * math.cos(a), CY + rcv + 0.09, d - 0.35 + 0.195 * math.sin(a)), 0.016, GV, sides=6)
        F.cyl((px, CY - rcv + 0.02, d - 0.35), (px, CY - rcv - 0.04, d - 0.35), 0.035, CA, sides=8)        # drain plug
        F.box(px - rcv - 0.012, px - rcv + 0.04, CY - 0.06, CY + 0.06, d - 0.5, d - 0.2, {"left": "ih_arrow"})
        pv = (px + rcv + 0.06, CY, d - 0.35)
        F.cyl((px + rcv - 0.02, CY, pv[2]), (px + rcv + 0.09, CY, pv[2]), 0.055, CA, sides=12)          # shaft boss
        F.cyl((px + rcv + 0.09, CY, pv[2]), (px + rcv + 0.13, CY, pv[2]), 0.03, DS, sides=8)
        a = math.radians(-55 if not run else 12)
        tip = (CY + 0.55 * math.sin(a), pv[2] - 0.55 * math.cos(a))
        F.bar((px + rcv + 0.11, CY, pv[2]), (px + rcv + 0.11, tip[0], tip[1]), 0.05, 0.025, DS, up="t")
        F.cyl((px + rcv + 0.09, tip[0], tip[1]), (px + rcv + 0.21, tip[0], tip[1]), 0.08, DS, sides=14)  # weight
        F.box(px + rcv - 0.03, px + rcv + 0.042, CY + 0.08, CY + 0.15, d - 0.6, d - 0.44, {"right": tag3(20 + n)})
        d -= 0.7
        _flange(F, px, CY, d, d - 0.07, rf, CA, bolts=16 if mu else 12)
        d -= 0.07
        # ---- the motor-operated butterfly valve: lug body, shaft bosses, gearbox with its position indicator,
        # actuator with its controls, lamps (live), spoked handwheel, nameplate, tag, conduit
        F.cyl((px, CY, d), (px, CY, d - 0.22), rf + 0.02, CA, sides=28)
        for k in range(16 if mu else 12):
            a = 2 * math.pi * (k + 0.5) / (16 if mu else 12)
            u, v = (rf - 0.032) * math.cos(a), (rf - 0.032) * math.sin(a)
            F.cyl((px + u, CY + v, d + 0.03), (px + u, CY + v, d - 0.25), 0.011, DS, sides=6)
            for dd in (d + 0.02, d - 0.24):
                F.cyl((px + u, CY + v, dd), (px + u, CY + v, dd - 0.02), 0.02, GV, sides=6)
        dc = d - 0.11
        top = CY + rf + 0.02
        F.cyl((px, top - 0.02, dc), (px, top + 0.14, dc), 0.08, CA, sides=14)
        F.cyl((px, CY - rf - 0.02, dc), (px, CY - rf - 0.1, dc), 0.07, CA, sides=14)
        F.cyl((px, top + 0.14, dc), (px, top + 0.16, dc), 0.11, CA, sides=16)
        F.box(px - 0.2, px + 0.2, top + 0.16, top + 0.42, dc - 0.17, dc + 0.17, "ih_act")                # gearbox
        F.box(px - 0.08, px + 0.08, top + 0.42, top + 0.44, dc - 0.08, dc + 0.08, {"top": "ih_posind", "out": "ih_act",
              "in": "ih_act", "left": "ih_act", "right": "ih_act"})
        op_ = mov_open[n]
        if op_:                                     # the pointer: along the line when open, across it when shut
            F.bar((px, top + 0.446, dc + 0.065), (px, top + 0.446, dc - 0.065), 0.016, 0.006, "ct_wheel", up="y")
        else:
            F.bar((px - 0.065, top + 0.446, dc), (px + 0.065, top + 0.446, dc), 0.016, 0.006, "ct_wheel", up="y")
        F.box(px + 0.2, px + 0.54, top + 0.18, top + 0.52, dc - 0.17, dc + 0.17, "ih_act")              # actuator
        for y in (top + 0.24, top + 0.46):
            F.box(px + 0.22, px + 0.52, y - 0.01, y + 0.01, dc + 0.17, dc + 0.185, "ih_act")             # cover ribs
        F.cyl((px + 0.54, top + 0.35, dc), (px + 0.72, top + 0.35, dc), 0.13, "ih_act", sides=20)         # motor
        for k in range(8):
            a = 2 * math.pi * k / 8
            F.box(px + 0.56, px + 0.7, top + 0.35 + 0.13 * math.sin(a) - 0.008, top + 0.35 + 0.13 * math.sin(a) + 0.008,
                  dc + 0.13 * math.cos(a) - 0.008, dc + 0.13 * math.cos(a) + 0.008, "ih_act")
        # its controls on the land face: local/off/remote selector, open, stop, close; lamps
        cf = dc - 0.17
        F.box(px + 0.24, px + 0.5, top + 0.22, top + 0.48, cf - 0.006, cf, {"in": "ih_mov_face"})
        F.cyl((px + 0.3, top + 0.42, cf - 0.006), (px + 0.3, top + 0.42, cf - 0.04), 0.025, DS, sides=12)
        F.box(px + 0.288, px + 0.312, top + 0.39, top + 0.45, cf - 0.05, cf - 0.04, "ct_wheel")
        for k, mat in enumerate(("ih_btn_r", "ih_btn_k", "ih_btn_g")):
            F.cyl((px + 0.3 + 0.07 * k, top + 0.3, cf - 0.006), (px + 0.3 + 0.07 * k, top + 0.3, cf - 0.03), 0.018, mat,
                  sides=10)
        F.cyl((px + 0.4, top + 0.42, cf - 0.006), (px + 0.4, top + 0.42, cf - 0.03), 0.02,
              "ih_lamp_r" if op_ else "ih_lamp_off_r", sides=10, lit=op_)                                 # red: open
        F.cyl((px + 0.46, top + 0.42, cf - 0.006), (px + 0.46, top + 0.42, cf - 0.03), 0.02,
              "ih_lamp_off" if op_ else "ih_lamp_g", sides=10, lit=not op_)                              # green: shut
        F.box(px + 0.25, px + 0.49, top + 0.235, top + 0.265, cf - 0.008, cf - 0.006, {"in": tag3(6 + n)})
        _zone(F, f"mov_open:{n}", px + 0.27, px + 0.33, top + 0.27, top + 0.33, cf - 0.04, cf)
        _zone(F, f"mov_close:{n}", px + 0.41, px + 0.47, top + 0.27, top + 0.33, cf - 0.04, cf)
        F.cyl((px - 0.2, top + 0.29, dc), (px - 0.3, top + 0.29, dc), 0.02, DS, sides=8)
        _handwheel(F, px - 0.315, top + 0.29, dc, 0.2)
        _conduit(F, [(px + 0.37, top + 0.18, dc + 0.1), (px + 0.37, top - 0.1, dc + 0.1), (px + 0.37, top - 0.1, dc + 0.32),
                     (px + 0.37, DECK - 0.1, dc + 0.32)], 0.02)
        F.cyl((px + 0.37, DECK + 0.002, dc + 0.32), (px + 0.37, DECK + 0.025, dc + 0.32), 0.04, GV, sides=8)
        d -= 0.22
        if mu:
            # ---- the make-up line: spool, flow meter, pressure transmitter, to the deck's edge and into the ground
            _flange(F, px, CY, d, d - 0.07, rf, PI, bolts=16)
            d -= 0.07
            _weld(F, (px, CY, d - 0.03), (px, CY, d - 0.05), rp)
            fm0 = d - 0.9
            F.cyl((px, CY, d), (px, CY, fm0), rp, PI, sides=28)
            _weld(F, (px, CY, fm0 + 0.05), (px, CY, fm0 + 0.03), rp)
            _flange(F, px, CY, fm0, fm0 - 0.07, rf, PI, bolts=16)
            _magmeter(F, px, CY, fm0 - 0.07, fm0 - 0.67, rp, rf, 12 + (0 if bay == 0 else 3), 0 if bay == 0 else 3)
            _flange(F, px, CY, fm0 - 0.67, fm0 - 0.74, rf, PI, bolts=16)
            de = D_EDGE - 0.3
            F.cyl((px, CY, fm0 - 0.74), (px, CY, de), rp, PI, sides=28)
            _weld(F, (px, CY, fm0 - 0.77), (px, CY, fm0 - 0.79), rp)
            _pt(F, px, CY + rp, fm0 - 1.25)
            dv, yv = _elbow_down(F, px, de, CY, 0.62, rp, PI)
            _into_ground(F, px, dv, yv, rp, rf)
            _marker(F, px, CY, fm0 - 2.0, rp, 0)
            for ds_ in (fm0 - 1.6, D_EDGE + 0.5):
                _stand(F, px, ds_, CY - rp, rp)
        else:
            # ---- on to its train's header (a tee into the header's side)
            _flange(F, px, CY, d, d - 0.07, rf, PI, bolts=12)
            d -= 0.07
            hd = -4.0
            F.cyl((px, CY, d), (px, CY, hd + 0.2), rp, PI, sides=28)
            _weld(F, (px, CY, hd + 0.33), (px, CY, hd + 0.31), rp)
            _stand(F, px, d - 0.35, CY - rp, rp)
    # ---- the service water trains: header, strainer, flow meter, into the ground
    for t, bay in enumerate((1, 2)):
        x0, x1 = bay_x(bay)
        ps = [px for n, b, px, mu in pump_list() if b == bay]
        xm = (x0 + x1) / 2
        hd, rH, rfH = -4.0, 0.305, 0.44
        F.cyl((ps[0] - 0.45, CY, hd), (ps[1] + 0.45, CY, hd), rH, PI, sides=28)
        for xe, sgn in ((ps[0] - 0.45, -1), (ps[1] + 0.45, 1)):                                    # blind flanges
            _flange(F, hd, CY, xe, xe + sgn * 0.08, rfH, PI, bolts=16, axis="x")
        for xs_ in (ps[0] - 0.12, ps[1] + 0.12):
            _stand(F, xs_, hd, CY - rH, rH, along_x=True)
        _marker(F, (ps[0] + xm) / 2 - 0.15, CY, hd, rH, 1 + t, along_x=True)
        # the strainer: inlet spool and flange, body on four pipe legs, girth flange, cover with lugs, vent, drive,
        # differential pressure gauge and switch, drain valve, backwash valve; nameplate and tag
        sd = -5.35
        F.cyl((xm, CY, hd), (xm, CY, sd + 0.83), rH, PI, sides=28)
        _flange(F, xm, CY, sd + 0.83, sd + 0.76, rfH, PI, bolts=16)
        F.cyl((xm, CY, sd + 0.76), (xm, CY, sd + 0.5), rH, "ih_strainer", sides=28)
        ys0, ys1 = DECK + 0.38, DECK + 1.95
        F.cyl((xm, ys0, sd), (xm, ys1, sd), 0.56, "ih_strainer", sides=32)
        F.cyl((xm, DECK + 0.24, sd), (xm, ys0, sd), 0.44, "ih_strainer", sides=28)                     # bottom head
        F.cyl((xm, DECK + 1.15, sd), (xm, DECK + 1.21, sd), 0.63, "ih_strainer", sides=32)             # girth flanges
        F.cyl((xm, DECK + 1.21, sd), (xm, DECK + 1.25, sd), 0.6, "ih_gasket", sides=32)
        F.cyl((xm, DECK + 1.25, sd), (xm, DECK + 1.31, sd), 0.63, "ih_strainer", sides=32)
        for k in range(20):
            a = 2 * math.pi * (k + 0.5) / 20
            u, v = 0.6 * math.cos(a), 0.6 * math.sin(a)
            F.cyl((xm + u, DECK + 1.13, sd + v), (xm + u, DECK + 1.33, sd + v), 0.011, DS, sides=6)
        F.cyl((xm, ys1, sd), (xm, ys1 + 0.07, sd), 0.64, "ih_strainer", sides=32)                     # cover
        F.cyl((xm, ys1 + 0.07, sd), (xm, ys1 + 0.17, sd), 0.5, "ih_strainer", sides=32)
        for k in range(4):
            a = k * math.pi / 2 + math.pi / 4
            Fs = F.sub(xm, sd, a - math.pi / 2)
            Fs.box(-0.025, 0.025, ys1 + 0.17, ys1 + 0.3, 0.32, 0.42, "ih_strainer")
        F.cyl((xm + 0.3, ys1 + 0.17, sd + 0.25), (xm + 0.3, ys1 + 0.26, sd + 0.25), 0.015, GV, sides=6)          # vent
        _ball_valve(F, xm + 0.3, ys1 + 0.28, sd + 0.25, "y", r=0.022)
        F.box(xm - 0.2, xm + 0.2, ys1 + 0.17, ys1 + 0.5, sd - 0.2, sd + 0.2, "ih_reducer")                    # drive
        F.box(xm - 0.12, xm + 0.12, ys1 + 0.5, ys1 + 0.66, sd - 0.12, sd + 0.12, "ih_guard_y")
        F.cyl((xm, ys1 + 0.66, sd), (xm, ys1 + 0.7, sd), 0.16, "ih_mpaint", sides=16)
        F.cyl((xm, ys1 + 0.7, sd), (xm, ys1 + 1.08, sd), 0.15, "ih_mpaint", sides=20)
        _ring_ribs(F, xm, sd, 0.15, ys1 + 0.73, ys1 + 1.05, 16, w=0.014, depth=0.025)
        F.cyl((xm, ys1 + 1.08, sd), (xm, ys1 + 1.16, sd), 0.13, "ih_cowl", sides=16)
        _conduit(F, [(xm + 0.15, ys1 + 0.85, sd), (xm + 0.3, ys1 + 0.85, sd), (xm + 0.3, ys1 + 0.85, sd + 0.66),
                     (xm + 0.3, DECK - 0.1, sd + 0.66)], 0.02)
        F.box(xm - 0.15, xm + 0.15, DECK + 1.5, DECK + 1.7, sd - 0.565, sd - 0.54, {"in": plate3(12 + t)})
        F.box(xm - 0.2, xm + 0.2, DECK + 1.42, DECK + 1.48, sd - 0.565, sd - 0.54, {"in": tag3(16 + t)})
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            lx, ld = xm + 0.5 * math.cos(a), sd + 0.5 * math.sin(a)
            F.cyl((lx, DECK + 0.025, ld), (lx, DECK + 0.62, ld), 0.045, DS, sides=12)
            F.box(lx - 0.1, lx + 0.1, DECK, DECK + 0.025, ld - 0.1, ld + 0.1, DS)
            for a2 in (-0.07, 0.07):
                F.cyl((lx + a2, DECK + 0.025, ld + a2), (lx + a2, DECK + 0.05, ld + a2), 0.014, GV, sides=6)
        # differential pressure: taps in and out, the gauge and switch on a bracket (live: 3 psid, clean)
        gx, gy = xm - 0.72, CY + 0.32
        for dd in (sd + 0.62, sd - 0.62):
            _conduit(F, [(xm - 0.25, CY + 0.1, dd), (xm - 0.25, gy, dd), (gx, gy, dd), (gx, gy, sd)], 0.008, mat="ct_brass")
        F.box(gx - 0.04, gx + 0.04, DECK, gy - 0.03, sd - 0.04, sd + 0.04, DS)
        F.box(gx - 0.08, gx + 0.08, gy - 0.03, gy + 0.2, sd - 0.12, sd + 0.12, DS)
        F.cyl((gx - 0.08, gy + 0.1, sd - 0.05), (gx - 0.13, gy + 0.1, sd - 0.05), 0.065, "ih_gauge", "ih_dpdial", sides=20)
        F.cyl((gx - 0.13, gy + 0.1, sd - 0.05), (gx - 0.135, gy + 0.1, sd - 0.05), 0.07, GV, "ih_glass", sides=20)
        a = math.radians(225 - 270 * 3 / 25)
        F.bar((gx - 0.137, gy + 0.1, sd - 0.05), (gx - 0.137, gy + 0.1 + 0.05 * math.sin(a), sd - 0.05 - 0.05 * math.cos(a)),
              0.007, 0.003, "ih_needle", up="t")
        F.box(gx - 0.14, gx - 0.08, gy + 0.05, gy + 0.17, sd + 0.04, sd + 0.11, "ih_act")                    # dp switch
        # drain valve off the bottom head
        F.cyl((xm, DECK + 0.24, sd), (xm, DECK + 0.13, sd), 0.03, GV, sides=8)
        _ball_valve(F, xm, DECK + 0.1, sd, "y", open_=False)
        # backwash: off the bottom head through its motor-operated valve, to the back wall's slab
        F.cyl((xm + 0.35, DECK + 0.3, sd), (xm + 0.7, DECK + 0.3, sd), 0.06, PI, sides=12)
        F.cyl((xm + 0.7, DECK + 0.3, sd), (xm + 0.76, DECK + 0.3, sd), 0.1, CA, sides=14)
        F.cyl((xm + 0.76, DECK + 0.22, sd), (xm + 0.76, DECK + 0.38, sd), 0.1, CA, sides=14)            # its valve
        F.box(xm + 0.66, xm + 0.86, DECK + 0.4, DECK + 0.64, sd - 0.1, sd + 0.1, "ih_act")
        F.cyl((xm + 0.76, DECK + 0.38, sd), (xm + 0.76, DECK + 0.4, sd), 0.03, CA, sides=8)
        F.cyl((xm + 0.76, DECK + 0.64, sd), (xm + 0.76, DECK + 0.67, sd), 0.025, "ih_lamp_r" if bw[t] else "ih_lamp_g",
              sides=8, lit=True)
        F.box(xm + 0.86, xm + 0.862, DECK + 0.47, DECK + 0.53, sd - 0.1, sd + 0.1, {"right": tag3(18 + t)})
        F.cyl((xm + 0.76, DECK + 0.3, sd - 0.1), (xm + 0.76, DECK + 0.3, D_EDGE + 0.5), 0.06, PI, sides=12)
        F.cyl((xm + 0.76, DECK + 0.36, D_EDGE + 0.5), (xm + 0.76, DECK - 0.2, D_EDGE + 0.5), 0.06, PI, sides=12)
        F.cyl((xm + 0.76, DECK + 0.002, D_EDGE + 0.5), (xm + 0.76, DECK + 0.03, D_EDGE + 0.5), 0.1, GV, sides=12)
        # outlet: spool, flow meter, to the edge and into the ground
        do = sd - 0.56
        F.cyl((xm, CY, sd - 0.5), (xm, CY, do - 0.12), rH, "ih_strainer", sides=28)
        _flange(F, xm, CY, do - 0.12, do - 0.19, rfH, PI, bolts=16)
        _magmeter(F, xm, CY, do - 0.19, do - 0.75, rH, rfH, 13 + t, 1 + t)
        _flange(F, xm, CY, do - 0.75, do - 0.82, rfH, PI, bolts=16)
        de = D_EDGE - 0.3
        F.cyl((xm, CY, do - 0.82), (xm, CY, de), rH, PI, sides=28)
        dv, yv = _elbow_down(F, xm, de, CY, 0.62, rH, PI)
        _into_ground(F, xm, dv, yv, rH, rfH)
    return o.ops()


def _handwheel(F, x, y, d, R):
    """A spoked handwheel turning about x: rim, four spokes, hub."""
    n = 20
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        F.cyl((x, y + R * math.sin(a0), d + R * math.cos(a0)), (x, y + R * math.sin(a1), d + R * math.cos(a1)), 0.016,
              "ct_wheel", sides=8)
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        F.cyl((x, y, d), (x, y + (R - 0.01) * math.sin(a), d + (R - 0.01) * math.cos(a)), 0.01, "ct_wheel", sides=6)
    F.cyl((x - 0.025, y, d), (x + 0.025, y, d), 0.035, "ct_wheel", sides=10)


def _magmeter(F, x, y, d0, d1, rp, rf, ti, cell):
    """A flanged magnetic flow meter: grounding rings, body, the converter on its neck with the display (live), its
    tag and conduit."""
    F.cyl((x, y, d0), (x, y, d0 - 0.012), rf - 0.04, GV, sides=28)                 # grounding rings
    F.cyl((x, y, d1 + 0.012), (x, y, d1), rf - 0.04, GV, sides=28)
    F.cyl((x, y, d0 - 0.012), (x, y, d0 - 0.06), rf, "ih_magmeter", sides=28)
    F.cyl((x, y, d1 + 0.06), (x, y, d1 + 0.012), rf, "ih_magmeter", sides=28)
    F.cyl((x, y, d0 - 0.06), (x, y, d1 + 0.06), rp + 0.07, "ih_magmeter", sides=28)
    dm = (d0 + d1) / 2
    yt = y + rp + 0.07
    F.cyl((x, yt - 0.02, dm), (x, yt + 0.1, dm), 0.055, "ih_magmeter", sides=12)
    F.box(x - 0.15, x + 0.15, yt + 0.1, yt + 0.36, dm - 0.08, dm + 0.08, {"in": "ih_act", "out": "ih_act", "left": "ih_act",
          "right": "ih_act", "top": "ih_act", "bottom": "ih_act"})
    F.box(x - 0.13, x + 0.13, yt + 0.12, yt + 0.34, dm - 0.082, dm - 0.08, {"in": ["ih_ft_face", cell / 4, 0, (cell + 1) / 4, 1]})
    F.box(x - 0.12, x + 0.12, yt + 0.37, yt + 0.43, dm - 0.08, dm - 0.078, {"in": tag3(ti)})
    F.box(x - 0.13, x + 0.13, yt + 0.36, yt + 0.37, dm - 0.08, dm + 0.08, "ih_act")
    _conduit(F, [(x + 0.15, yt + 0.2, dm + 0.03), (x + 0.32, yt + 0.2, dm + 0.03), (x + 0.32, DECK - 0.1, dm + 0.03)], 0.015)
    F.cyl((x + 0.32, DECK + 0.002, dm + 0.03), (x + 0.32, DECK + 0.02, dm + 0.03), 0.03, GV, sides=8)


def _pt(F, x, ytop, d):
    """A pressure transmitter on a root valve on top of a line, its conduit."""
    F.cyl((x, ytop - 0.02, d), (x, ytop + 0.08, d), 0.015, GV, sides=6)
    _ball_valve(F, x, ytop + 0.1, d, "y", r=0.022)
    F.cyl((x, ytop + 0.12, d), (x, ytop + 0.16, d), 0.015, GV, sides=6)
    F.box(x - 0.05, x + 0.05, ytop + 0.16, ytop + 0.22, d - 0.05, d + 0.05, "ih_act")
    F.cyl((x, ytop + 0.22, d), (x, ytop + 0.27, d), 0.055, "ih_act", sides=14)
    F.cyl((x, ytop + 0.27, d), (x, ytop + 0.29, d), 0.05, "ih_act", "ih_pt_face", sides=14)
    _conduit(F, [(x + 0.05, ytop + 0.19, d), (x + 0.4, ytop + 0.19, d), (x + 0.4, DECK - 0.1, d)], 0.012)


def _marker(F, x, y, d, r, k, along_x=False):
    """An ASME A13.1 marker on a line's west side (along d) or land side (along x)."""
    if along_x:
        F.box(x - 0.35, x + 0.35, y - 0.06, y + 0.06, d - r - 0.004, d - r - 0.002, {"in": ["ih_marker3", 0, k / 4, 1, (k + 1) / 4]})
    else:
        F.box(x - r - 0.004, x - r - 0.002, y - 0.06, y + 0.06, d - 0.35, d + 0.35, {"left": ["ih_marker3", 0, k / 4, 1, (k + 1) / 4]})


def _paint(rgb, seed, n=128, mottle=0.03, grain=0.012):
    """A crisp painted surface (n px a metre): a faint low-frequency mottle and a fine grain, no blotches."""
    r = np.random.default_rng(seed)
    lo = Image.fromarray((np.clip(r.normal(0.5, 0.18, (n // 16, n // 16)), 0, 1) * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC)
    m = (np.asarray(lo, float) / 255 - 0.5) * 2 * mottle
    a = np.ones((n, n, 3)) * np.array(rgb, float) * (1 + m + r.normal(0, grain, (n, n)))[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")


def pumps_textures(running=(True, True, False, True, False, False)):
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
    sheet = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
    for i, text in enumerate(TAGS3):
        c, r = i % 4, i // 4
        rng = np.random.default_rng(500 + i)
        a = np.ones((64, 256, 3)) * np.array((198, 202, 204), float) + np.repeat(rng.normal(0, 4, (64, 1)), 256, 1)[..., None]
        cell = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(cell)
        d.rectangle([0, 0, 255, 63], outline=(140, 144, 146, 255), width=2)
        for x in (14, 241):
            d.ellipse([x - 6, 26, x + 6, 38], fill=(160, 164, 166, 255), outline=(110, 112, 114, 255))
        size = 34
        while d.textlength(text, font=ImageFont.truetype(FB, size)) > 206 and size > 16:
            size -= 1
        d.text((128, 33), text, font=ImageFont.truetype(FB, size), fill=(36, 38, 40, 255), anchor="mm")
        sheet.paste(cell, (c * 256, r * 64))
    out["ih_tags3"] = sheet
    # stamped stainless nameplates: 0-5 the motors, 6-11 the pumps, 12-13 the strainers
    np_ = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
    names = TAGS3[:6]
    for i in range(14):
        c, r = i % 4, i // 4
        cell = _paint((196, 198, 196), 700 + i, n=256, mottle=0.02, grain=0.02).resize((256, 128))
        d = ImageDraw.Draw(cell)
        d.rectangle([1, 1, 254, 126], outline=(120, 122, 122, 255), width=3)
        for x, y in ((10, 10), (245, 10), (10, 117), (245, 117)):
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(150, 152, 152, 255), outline=(90, 92, 92, 255))
        if i < 6:
            mu = i in (0, 5)
            lines = ["INDUCTION MOTOR  VHS  WP-I", f"{'800' if mu else '1000'} HP   4160 V   3 PH   60 HZ",
                     f"1180 RPM   FLA {'102' if mu else '127'} A   SF 1.15", f"INS F   FRAME 5810VP   {names[i]}"]
        elif i < 12:
            mu = i in (6, 11)
            lines = ["VERTICAL TURBINE PUMP", f"{'15,000' if mu else '12,000'} GPM   {'160' if mu else '230'} FT TDH",
                     f"1180 RPM   {'2' if mu else '3'} STAGE   ENCL. SHAFT", f"SERIAL 26-{4471 + i}   {names[i - 6]}"]
        else:
            lines = ["AUTOMATIC BACKWASH STRAINER", "24 IN   150 LB   1/16 IN SCREEN", "MAX 150 PSI   12,000 GPM",
                     f"SERIAL 26-{9120 + i}   TRAIN {i - 11}"]
        for k, line in enumerate(lines):
            d.text((128, 24 + 26 * k), line, font=ImageFont.truetype(FM, 14 if k else 15), fill=(40, 42, 44, 255), anchor="mm")
        np_.paste(cell, (c * 256, r * 128))
    out["ih_nameplates"] = np_
    out["ih_pipe"] = _paint((66, 110, 84), 601)                      # painted carbon steel, water green
    out["ih_cast"] = _paint((44, 80, 124), 602, grain=0.022)            # cast iron, enamelled
    out["ih_mpaint"] = _paint((52, 88, 124), 603)                     # motor enamel
    out["ih_act"] = _paint((92, 100, 106), 604)                        # actuators, transmitters
    out["ih_strainer"] = _paint((46, 78, 118), 605)
    out["ih_magmeter"] = _paint((36, 60, 98), 606)
    out["ih_grout"] = _paint((158, 156, 148), 607, mottle=0.04, grain=0.03)
    mc = _paint((150, 154, 156), 608, grain=0.008)                    # the soleplate's machined top: turned rings
    dmc = ImageDraw.Draw(mc)
    for k in range(0, 128, 3):
        dmc.line([(0, k), (127, k)], fill=(140, 144, 146, 255))
    out["ih_machined"] = mc
    sh = _paint((150, 152, 154), 609, grain=0.01)
    out["ih_shaft"] = sh
    wd = Image.new("RGBA", (128, 128), (128, 128, 124, 255))           # weld bead ripples
    dw = ImageDraw.Draw(wd)
    for k in range(0, 128, 4):
        dw.line([(k, 0), (k + 2, 127)], fill=(156, 156, 150, 255), width=1)
    out["ih_weld"] = wd
    out["ih_gasket"] = Image.new("RGBA", (16, 16), (26, 26, 26, 255))
    fx = Image.new("RGBA", (64, 64), (24, 24, 26, 255))                # liquid-tight flex: its ribs
    dfx = ImageDraw.Draw(fx)
    for y in range(0, 64, 4):
        dfx.line([(0, y), (63, y)], fill=(48, 48, 52, 255))
    out["ih_flex"] = fx
    out["ih_copper"] = _paint((184, 116, 56), 610, n=32)
    out["ih_glass"] = Image.new("RGBA", (16, 16), (210, 220, 226, 60))
    out["ih_needle"] = Image.new("RGBA", (4, 4), (20, 20, 20, 255))
    out["ih_lamp_r"] = Image.new("RGBA", (16, 16), (255, 70, 50, 255))
    out["ih_lamp_off_r"] = _paint((84, 30, 26), 611, n=16)
    out["ih_lamp_g"] = Image.new("RGBA", (16, 16), (90, 255, 120, 255))
    out["ih_lamp_off"] = _paint((30, 70, 40), 612, n=16)
    out["ih_btn_k"] = _paint((30, 30, 32), 613, n=16)
    cp = _paint((56, 58, 62), 614, n=64)
    ImageDraw.Draw(cp).rectangle([0, 0, 5, 63], fill=(236, 204, 30, 255))          # the coupling's stripe
    out["ih_coupling"] = cp
    g = Image.new("RGBA", (128, 128), (0, 0, 0, 0))                                # the expanded-metal guard, yellow
    dg = ImageDraw.Draw(g)
    for k in range(-128, 256, 16):
        dg.line([(k, 0), (k + 128, 128)], fill=(222, 184, 22, 255), width=3)
        dg.line([(k + 128, 0), (k, 128)], fill=(200, 164, 18, 255), width=3)
    out["ih_guard"] = g
    out["ih_guard_y"] = _paint((222, 184, 22), 615, n=64)
    lv = _paint((52, 88, 124), 616)                                                 # louvres: slats and mullions
    dl = ImageDraw.Draw(lv)
    for y in range(4, 124, 8):
        dl.rectangle([0, y, 127, y + 3], fill=(18, 30, 44, 255))
        dl.line([(0, y + 4), (127, y + 4)], fill=(92, 128, 164, 255))
    for x in range(0, 128, 42):
        dl.rectangle([x, 0, x + 3, 127], fill=(52, 88, 124, 255))
    out["ih_louvre"] = lv
    ar = _paint((44, 80, 124), 617, n=128).resize((128, 64))                      # the cast flow arrow
    da = ImageDraw.Draw(ar)
    da.rectangle([14, 26, 96, 38], fill=(90, 128, 170, 255))
    da.polygon([(96, 14), (120, 32), (96, 50)], fill=(90, 128, 170, 255))
    out["ih_arrow"] = ar
    pi_ = Image.new("RGBA", (64, 64), (236, 236, 230, 255))                        # position indicator plate
    dpi = ImageDraw.Draw(pi_)
    dpi.text((32, 10), "OPEN", font=ImageFont.truetype(FB, 12), fill=(180, 20, 20, 255), anchor="mm")
    dpi.text((32, 54), "SHUT", font=ImageFont.truetype(FB, 12), fill=(20, 120, 40, 255), anchor="mm")
    out["ih_posind"] = pi_
    mf = _paint((92, 100, 106), 618, n=128)                                       # the actuator's control face
    dmf = ImageDraw.Draw(mf)
    dmf.rectangle([2, 2, 125, 125], outline=(60, 66, 70, 255), width=3)
    for x, y, t in ((30, 52, "L  O  R"), (69, 52, "OPEN"), (96, 52, "SHUT"), (30, 112, "OPEN"), (64, 112, "STOP"),
                    (98, 112, "CLOSE")):
        dmf.text((x, y), t, font=ImageFont.truetype(FB, 10), fill=(236, 236, 236, 255), anchor="mm")
    out["ih_mov_face"] = mf
    ptf = Image.new("RGBA", (64, 64), (60, 66, 70, 255))
    dpt = ImageDraw.Draw(ptf)
    dpt.ellipse([6, 6, 57, 57], fill=(150, 176, 120, 255))
    dpt.text((32, 32), "94.6", font=ImageFont.truetype(FB, 14), fill=(20, 26, 16, 255), anchor="mm")
    out["ih_pt_face"] = ptf
    dg_ = Image.new("RGBA", (64, 32), (250, 250, 250, 255))                         # the TB's voltage label
    ddg = ImageDraw.Draw(dg_)
    ddg.rectangle([0, 0, 63, 11], fill=(200, 20, 20, 255))
    ddg.text((32, 6), "DANGER", font=ImageFont.truetype(FB, 9), fill=(255, 255, 255, 255), anchor="mm")
    ddg.text((32, 22), "4160 VOLTS", font=ImageFont.truetype(FB, 10), fill=(10, 10, 10, 255), anchor="mm")
    out["ih_danger"] = dg_
    for name, vmax, unit, step in (("ih_dial", 160, "PSI", 20), ("ih_dpdial", 25, "PSID", 5)):
        im = Image.new("RGBA", (256, 256), (244, 244, 236, 255))
        di = ImageDraw.Draw(im)
        di.ellipse([3, 3, 252, 252], outline=(30, 30, 30, 255), width=5)
        nmaj = int(vmax / step)
        for k in range(nmaj * 5 + 1):
            a = math.radians(225 - 270 * k / (nmaj * 5))
            r0 = 92 if k % 5 == 0 else 102
            di.line([(128 + r0 * math.cos(a), 128 - r0 * math.sin(a)), (128 + 114 * math.cos(a), 128 - 114 * math.sin(a))],
                    fill=(20, 20, 20, 255), width=4 if k % 5 == 0 else 2)
            if k % 5 == 0:
                di.text((128 + 72 * math.cos(a), 128 - 72 * math.sin(a)), str(int(step * k / 5)),
                        font=ImageFont.truetype(FB, 22), fill=(20, 20, 20, 255), anchor="mm")
        di.text((128, 186), unit, font=ImageFont.truetype(FB, 24), fill=(20, 20, 20, 255), anchor="mm")
        out[name] = im
    # flow meter displays (live): MU-A, SW-1, SW-2, MU-B
    flows = [15000 if running[0] else 0, 12000 * (running[1] + running[2]), 12000 * (running[3] + running[4]),
             15000 if running[5] else 0]
    ft = Image.new("RGBA", (1024, 256), (92, 100, 106, 255))
    df_ = ImageDraw.Draw(ft)
    for k, v in enumerate(flows):
        x = 256 * k
        df_.rectangle([x + 3, 3, x + 252, 252], outline=(56, 62, 66, 255), width=6)
        df_.rectangle([x + 26, 36, x + 230, 140], fill=(150, 176, 120, 255), outline=(30, 30, 30, 255), width=3)
        df_.text((x + 128, 76), f"{v:,}", font=ImageFont.truetype(FB, 50), fill=(20, 26, 16, 255), anchor="mm")
        df_.text((x + 128, 120), "GPM", font=ImageFont.truetype(FB, 22), fill=(20, 26, 16, 255), anchor="mm")
        for bx_ in (78, 128, 178):
            df_.rounded_rectangle([x + bx_ - 18, 180, x + bx_ + 18, 208], 5, fill=(30, 30, 34, 255))
    out["ih_ft_face"] = ft
    mk = Image.new("RGBA", (1024, 512), (0, 120, 60, 255))                       # line markers (ASME A13.1 water)
    dm = ImageDraw.Draw(mk)
    for r, text in enumerate(("MAKE-UP WATER", "SERVICE WATER 1", "SERVICE WATER 2", "MAKE-UP WATER")):
        y = 128 * r + 64
        dm.text((470, y), text, font=ImageFont.truetype(FB, 72), fill=(255, 255, 255, 255), anchor="mm")
        dm.rectangle([860, y - 14, 940, y + 14], fill=(255, 255, 255, 255))
        dm.polygon([(1000, y), (936, y - 40), (936, y + 40)], fill=(255, 255, 255, 255))
    out["ih_marker3"] = mk
    return out


# ================================================================== piece 4: the electrical room and local control panel
#
# Real-world basis: an intake's electrical room (US practice), at the operating floor's level (above flood) on its own
# foundation at the intake's west end, painted block walls, a sealed floor, open-web joists under a steel roof deck.
# Two trains kept apart on opposite walls, each fed from its own plant 4160 V bus: a medium-voltage motor control
# lineup (an incoming section with its load-break switch and metering; vacuum-contactor starters for its make-up and
# service water pumps, each with a motor protection relay, ammeter, lamps and a local/remote switch, its isolating
# switch's handle outside the door; a fused feeder to the train's transformer), a dry-type 4160-480 V transformer, and
# a 480 V motor control centre (the screens' drives, the screen wash pump, the strainer's drive, the motor-operated
# valves, the gantry, the heaters, lighting and the panel's UPS). On the west wall the intake's local control panel:
# bay levels, screen differentials and controls, an annunciator, an operator screen, the pumps' control switches,
# ammeters, flows and header pressures. Cable trays over the lineups, LED fixtures, emergency lights, exit signs,
# extinguishers, rubber mats in front of the 4160 V gear, arc flash labels (NFPA 70E) on every section. The doors open
# out onto the pump deck (NEC 110.33) with panic bars.

ER_X = (-8.4, -0.4)                # the room's outside faces (west, east)
ER_D = (-6.6, 1.5)                 # (south: the land side, north, in line with the pump hall)
ER_WALL = 0.3
ER_CEIL = 4.6                      # the roof deck's underside
ER_DOORS = [(-6.0, -5.0), (-1.6, -0.6)]   # in the east wall, onto the deck
GEAR = "ih_gear"
EPLATES = ["MV MCC-1   4160 V   TRAIN 1", "MV MCC-2   4160 V   TRAIN 2", "INCOMING  FROM BUS 1A", "INCOMING  FROM BUS 2A",
           "MUP-A   800 HP", "SWP-1A   1000 HP", "SWP-1B   1000 HP", "XFMR-1 FEEDER", "MUP-B   800 HP", "SWP-2A   1000 HP",
           "SWP-2B   1000 HP", "XFMR-2 FEEDER", "MCC-1   480 V", "MCC-2   480 V", "XFMR-1  225 KVA", "XFMR-2  225 KVA",
           "TWS-1", "TWS-2", "SCWP-1", "SW STR 1", "MOV-MU-A", "MOV-SW-1A", "MOV-SW-1B", "MOV-BW-1",
           "GANTRY", "LP-1 XFMR", "HVAC-1", "HEATERS", "LCP-1 UPS", "RECEPT", "SPARE", "MAIN 400 A",
           "TWS-3", "TWS-4", "SCWP-2", "SW STR 2", "MOV-MU-B", "MOV-SW-2A", "MOV-SW-2B", "MOV-BW-2",
           "LCP-1 HVAC", "EXH FAN", "HVAC-2", "INTAKE LOCAL CONTROL PANEL  LCP-1", "BAY LEVELS", "SCREENS", "ANNUNCIATOR",
           "PUMPS", "FLOWS", "HEADER PRESSURE", "LP-1  120/208 V", "LOCAL", "REMOTE", "START", "STOP", "RUN", "OFF", "TRIP",
           "BUS ENERGIZED", "VOLTS", "AMPS", "ACK", "SILENCE", "TEST",
           "SWGR CW-A   13.8 KV", "SWGR CW-B   13.8 KV", "MAIN 52-A1  FROM BUS 1C", "MAIN 52-B1  FROM BUS 1D",
           "CWP-1A   3500 HP", "CWP-1B   3500 HP", "CWP-1C   3500 HP", "CWP-1D   3500 HP",
           "XFMR CW-A FEEDER", "XFMR CW-B FEEDER", "MCC CW-A  480 V", "MCC CW-B  480 V", "XFMR CW-A  300 KVA",
           "XFMR CW-B  300 KVA", "HPU PUMP 1", "HPU PUMP 2", "CRANE 50 T", "ROLL-UP DOOR", "EXH FANS", "UNIT HTRS",
           "SUMP PUMP", "LP-CW XFMR", "HVAC", "LCP-CW UPS", "SPARE", "RECEPT", "CIRC WATER LOCAL CONTROL PANEL  LCP-CW",
           "FOREBAY", "PUMPS", "VALVES", "BEARINGS", "DISCH PRESS", "TRIP", "CLOSE", "LOCKOUT 86", "LP-CW  120/208 V"]
ANN = ["BAY 1 LVL LO", "BAY 2 LVL LO", "BAY 3 LVL LO", "BAY 4 LVL LO", "TWS 1 DP HI", "TWS 2 DP HI",
       "TWS 3 DP HI", "TWS 4 DP HI", "TWS TRIP", "WASH PRESS LO", "SCWP TRIP", "RAKE FAULT",
       "MUP-A TRIP", "MUP-B TRIP", "SWP-1A TRIP", "SWP-1B TRIP", "SWP-2A TRIP", "SWP-2B TRIP",
       "SW STR 1 DP HI", "SW STR 2 DP HI", "SW HDR PRESS LO", "MU FLOW LO", "MCC-1 UV", "MCC-2 UV"]


def eplate(i):
    """Engraved lamacoid nameplate i (white on black; 2 across, 64 down, 512 x 64 px each)."""
    c, r = i % 2, i // 2
    return ["ih_eplates", c / 2, r / 64, (c + 1) / 2, (r + 1) / 64]


def _fq(Fl, t0, t1, y0, y1, r, mat, lit=False):
    """A quad on the plane r of a lineup's frame, its front toward -r (to the operator), t to his right. Its material
    is stretched over it whole (or the given part of the sheet), u running to the right as the operator reads it.
    (Frame._quad turns the corners to face him; in the game's frame, the generators' mirrored, that leaves u running
    the right way: towers and pump houses build, the compiled sites read the screens and windows the right way round.)"""
    if isinstance(mat, str):
        mat = [mat, 0.0, 0.0, 1.0, 1.0]
    Fl._quad([Fl.w(t0, y0, r), Fl.w(t1, y0, r), Fl.w(t1, y1, r), Fl.w(t0, y1, r)], mat, -Fl.r, lit=lit)


def _plate(Fl, t, y, w, h, i, r=0.0):
    """An engraved nameplate centred at (t, y) on the face r, standing off it 3 mm."""
    Fl.box(t - w / 2, t + w / 2, y - h / 2, y + h / 2, r - 0.003, r, {"in": eplate(i), "left": "ih_lamacoid",
           "right": "ih_lamacoid", "top": "ih_lamacoid", "bottom": "ih_lamacoid"})


def _lamp(Fl, t, y, r, colour, on, rad=0.016):
    """A pilot lamp: bezel and lens (live)."""
    Fl.cyl((t, y, r), (t, y, r - 0.012), rad + 0.008, "ct_galv", sides=12)
    lens = {"r": ("ih_lamp_r", "ih_lamp_off_r"), "g": ("ih_lamp_g", "ih_lamp_off"), "a": ("ih_lamp_a", "ih_lamp_off_a"),
            "w": ("ih_lamp_w", "ih_lamp_off_w")}[colour][0 if on else 1]
    Fl.cyl((t, y, r - 0.012), (t, y, r - 0.03), rad, lens, sides=12, lit=on)


def _button(Fl, t, y, r, mat, rad=0.016):
    Fl.cyl((t, y, r), (t, y, r - 0.01), rad + 0.007, "ct_galv", sides=12)
    Fl.cyl((t, y, r - 0.01), (t, y, r - 0.025), rad, mat, sides=12)


def _selector(Fl, t, y, r, pos=0):
    """A selector switch: bezel, knob, its bar turned to pos (-1 left, 0 up, 1 right)."""
    Fl.cyl((t, y, r), (t, y, r - 0.01), 0.024, "ct_galv", sides=12)
    Fl.cyl((t, y, r - 0.01), (t, y, r - 0.03), 0.019, "ih_btn_k", sides=12)
    a = math.radians(90 - 45 * pos)
    Fl.bar((t - 0.017 * math.cos(a), y - 0.017 * math.sin(a), r - 0.035), (t + 0.017 * math.cos(a), y + 0.017 * math.sin(a), r - 0.035),
           0.008, 0.012, "ih_btn_k", up="r")


def _pistol(Fl, t, y, r, pos=0, plate="ih_pistol_plate"):
    """A pistol-grip control switch (STOP - AUTO - START, or a breaker's TRIP - CLOSE): escutcheon plate, the grip
    turned to pos."""
    Fl.box(t - 0.045, t + 0.045, y - 0.05, y + 0.05, r - 0.004, r, {"in": [plate, 0, 0, 1, 1]})
    Fl.cyl((t, y, r - 0.004), (t, y, r - 0.02), 0.018, "ih_btn_k", sides=10)
    a = math.radians(90 - 45 * pos)
    Fl.bar((t, y, r - 0.026), (t + 0.045 * math.cos(a), y + 0.045 * math.sin(a), r - 0.026), 0.024, 0.018, "ih_btn_k", up="r")


def _meter(Fl, t, y, r, val, vmax, face, size=0.11):
    """A switchboard meter: square case and bezel, its scale, the pointer at val (live)."""
    h = size / 2
    Fl.box(t - h, t + h, y - h, y + h, r - 0.025, r, {"in": face, "left": "ih_btn_k", "right": "ih_btn_k", "top": "ih_btn_k",
           "bottom": "ih_btn_k"})
    a = math.radians(225 - 270 * max(0, min(val, vmax)) / vmax)
    Fl.bar((t, y, r - 0.027), (t + h * 0.72 * math.cos(a), y + h * 0.72 * math.sin(a), r - 0.027), 0.004, 0.002, "ih_needle_r",
           up="r")
    Fl.cyl((t, y, r - 0.025), (t, y, r - 0.029), 0.006, "ih_btn_k", sides=8)


def _digital(Fl, t, y, r, cell, w=0.12, h=0.06, sheet="ih_digits"):
    """A panel digital indicator: black case, its red LED window (live reading)."""
    Fl.box(t - w / 2, t + w / 2, y - h / 2, y + h / 2, r - 0.02, r, "ih_btn_k")
    _fq(Fl, t - w / 2 + 0.01, t + w / 2 - 0.01, y - h / 2 + 0.01, y + h / 2 - 0.01, r - 0.022,
        [sheet, (cell % 4) / 4, (cell // 4) / 8, (cell % 4 + 1) / 4, (cell // 4 + 1) / 8], lit=True)


def _label(Fl, t, y, w, h, mat, r=0.0):
    mat = [mat, 0.0, 0.0, 1.0, 1.0] if isinstance(mat, str) else mat
    Fl.box(t - w / 2, t + w / 2, y - h / 2, y + h / 2, r - 0.0015, r, {"in": mat})


def _door(Fl, t0, t1, y0, y1, r=0.0, hinge="left", handle=True, louvre=False):
    """A cubicle door: the leaf standing 15 mm proud, its hinges, a quarter-turn latch or handle."""
    Fl.box(t0, t1, y0, y1, r - 0.015, r, {"in": "ih_louvre_g" if louvre else GEAR, "left": GEAR, "right": GEAR,
           "top": GEAR, "bottom": GEAR})
    th = t0 + 0.012 if hinge == "left" else t1 - 0.012
    for y in (y0 + 0.08, y1 - 0.08):
        Fl.cyl((th, y - 0.04, r - 0.02), (th, y + 0.04, r - 0.02), 0.009, "ih_gear_dk", sides=8)
    if handle:
        tl = t1 - 0.05 if hinge == "left" else t0 + 0.05
        ym = (y0 + y1) / 2
        Fl.cyl((tl, ym, r - 0.015), (tl, ym, r - 0.03), 0.018, "ih_gear_dk", sides=10)
        Fl.box(tl - 0.008, tl + 0.008, ym - 0.05, ym + 0.01, r - 0.04, r - 0.03, "ih_gear_dk")


def mv_lineup(Fl, train, running):
    """A 4160 V motor control lineup, three sections 0.914 m wide, 2.29 m tall on its sill, 1.0 m deep, drawn in its
    own frame (t to the operator's right, y up, r back from its front at r 0). train: 0 or 1. running: its three
    pumps' (make-up, service water A, B)."""
    W, Y0, H = 0.914, DECK + 0.1, 2.29
    L = 3 * W
    Fl.box(0.0, L, DECK, Y0, 0.04, 0.96, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})                                                            # the sill channel
    Fl.box(0.0, L, Y0, Y0 + H, 0.0, 1.0, {"in": None, "left": GEAR, "right": GEAR, "top": "ih_gear_top", "out": GEAR,
           "bottom": None})
    _fq(Fl, 0.0, L, Y0, Y0 + H, 0.0, "ih_gear_seam")                                   # the front behind the doors
    for k in range(4):                                                                  # the sections' front posts
        Fl.box(max(0.0, k * W - 0.018), min(L, k * W + 0.018), Y0, Y0 + H, -0.02, 0.0, GEAR)
    for k in range(3):                                                                  # lifting angles, vents on top
        for s in (0.12, W - 0.12):
            Fl.box(k * W + s - 0.04, k * W + s + 0.04, Y0 + H, Y0 + H + 0.08, 0.1, 0.106, GEAR)
        Fl.box(k * W + 0.2, k * W + W - 0.2, Y0 + H, Y0 + H + 0.06, 0.4, 0.75, {"in": "ih_louvre_g", "out": "ih_louvre_g",
               "left": GEAR, "right": GEAR, "top": GEAR})
    pumps = [(4, 5) if train == 0 else (8, 9), (6, 7) if train == 0 else (10, 11)]
    names = [(4, 5), (6, 7)] if train == 0 else [(8, 9), (10, 11)]
    # ---- section 1: incoming metering, load-break switch, cable compartment
    t0, t1 = 0.03, W - 0.03
    _plate(Fl, W * 1.5, Y0 + H - 0.05, 1.2, 0.07, train, r=-0.02)
    _door(Fl, t0, t1, Y0 + 1.6, Y0 + 2.18)
    _plate(Fl, W / 2, Y0 + 2.12, 0.42, 0.045, 2 + train, r=-0.015)
    Fl.box(0.2, 0.42, Y0 + 1.82, Y0 + 2.02, -0.04, -0.015, "ih_btn_k")                 # multifunction meter (live)
    _fq(Fl, 0.215, 0.405, Y0 + 1.85, Y0 + 1.99, -0.042, ["ih_mfm", 0, 0, 1, 1], lit=True)
    _meter(Fl, 0.56, Y0 + 1.92, -0.015, 4160, 6000, "ih_mtr_v")
    _meter(Fl, 0.72, Y0 + 1.92, -0.015, 210 if any(running) else 12, 400, "ih_mtr_a4")
    _plate(Fl, 0.56, Y0 + 1.84, 0.1, 0.022, 59, r=-0.04)
    _plate(Fl, 0.72, Y0 + 1.84, 0.1, 0.022, 60, r=-0.04)
    _selector(Fl, 0.3, Y0 + 1.7, -0.015, 1)
    _lamp(Fl, 0.5, Y0 + 1.7, -0.015, "w", True)
    _plate(Fl, 0.62, Y0 + 1.7, 0.16, 0.022, 58, r=-0.015)
    _door(Fl, t0, t1, Y0 + 0.62, Y0 + 1.56)
    _glass_window(Fl, 0.22, 0.52, Y0 + 1.1, Y0 + 1.35, -0.015)
    _iso_handle(Fl, W - 0.13, Y0 + 1.05, -0.015, True)
    _door(Fl, t0, t1, Y0 + 0.04, Y0 + 0.58, handle=False)
    for a in (0.08, W - 0.08):
        for y in (Y0 + 0.1, Y0 + 0.31, Y0 + 0.52):
            Fl.cyl((a, y, -0.015), (a, y, -0.025), 0.012, "ct_galv", sides=6)
    _label(Fl, W / 2, Y0 + 0.4, 0.22, 0.11, "ih_danger_mv", r=-0.015)
    _label(Fl, W / 2, Y0 + 0.2, 0.2, 0.14, "ih_arcflash", r=-0.015)
    # ---- sections 2 and 3: two units each (starter or the transformer's fused feeder), LV compartment over each
    units = [(1, 1, 0, running[0], names[0][0]), (1, 0, 1, running[1], names[0][1]),
             (2, 1, 2, running[2], names[1][0]), (2, 0, None, None, names[1][1])]
    for sec, upper, pi, run, plate_i in units:
        x0 = sec * W
        yb = Y0 + 0.06 + (1.08 if upper else 0.0)
        # LV compartment: relay (live), ammeter, lamps, local/remote, start/stop
        _door(Fl, x0 + 0.03, x0 + W - 0.03, yb + 0.6, yb + 1.04)
        _plate(Fl, x0 + W / 2, yb + 0.99, 0.42, 0.045, plate_i, r=-0.015)
        if pi is not None:
            Fl.box(x0 + 0.1, x0 + 0.3, yb + 0.66, yb + 0.94, -0.045, -0.015, "ih_btn_k")       # motor protection relay
            _fq(Fl, x0 + 0.115, x0 + 0.285, yb + 0.84, yb + 0.925, -0.047,
                ["ih_relay", 0, (pi % 6) / 6, 1, (pi % 6 + 1) / 6], lit=True)
            for k in range(4):
                _button(Fl, x0 + 0.13 + 0.047 * k, yb + 0.72, -0.045, "ih_btn_k", rad=0.011)
            fla = 102 if pi in (0,) else 127
            _meter(Fl, x0 + 0.42, yb + 0.85, -0.015, (0.89 * fla) if run else 0, 200, "ih_mtr_a")
            _lamp(Fl, x0 + 0.58, yb + 0.88, -0.015, "r", bool(run))
            _lamp(Fl, x0 + 0.65, yb + 0.88, -0.015, "g", not run)
            _lamp(Fl, x0 + 0.72, yb + 0.88, -0.015, "a", False)
            for k, pl in enumerate((55, 56, 57)):
                _plate(Fl, x0 + 0.58 + 0.07 * k, yb + 0.835, 0.06, 0.018, pl, r=-0.015)
            _selector(Fl, x0 + 0.6, yb + 0.72, -0.015, 1)
            _button(Fl, x0 + 0.68, yb + 0.72, -0.015, "ih_btn_g")
            _button(Fl, x0 + 0.76, yb + 0.72, -0.015, "ih_btn_r")
        else:
            _lamp(Fl, x0 + 0.3, yb + 0.85, -0.015, "w", True)
            _meter(Fl, x0 + 0.5, yb + 0.85, -0.015, 22, 50, "ih_mtr_a5")
        # main door: viewing window, isolating switch handle (ON up, live), its hasp; labels
        _door(Fl, x0 + 0.03, x0 + W - 0.03, yb, yb + 0.56)
        _glass_window(Fl, x0 + 0.2, x0 + 0.5, yb + 0.28, yb + 0.48, -0.015)
        _iso_handle(Fl, x0 + W - 0.13, yb + 0.28, -0.015, True)
        _label(Fl, x0 + 0.33, yb + 0.14, 0.2, 0.14, "ih_arcflash", r=-0.015)
    return L


def _glass_window(Fl, t0, t1, y0, y1, r):
    """A viewing window in a door: its gasketed frame and the glass (the plant's framed glass)."""
    for a, b, c, d in ((t0, t1, y0, y0 + 0.02), (t0, t1, y1 - 0.02, y1), (t0, t0 + 0.02, y0 + 0.02, y1 - 0.02),
                       (t1 - 0.02, t1, y0 + 0.02, y1 - 0.02)):
        Fl.box(a, b, c, d, r - 0.012, r, "ih_frame_bronze")
    _fq(Fl, t0 + 0.02, t1 - 0.02, y0 + 0.02, y1 - 0.02, r - 0.006, "ih_fglass")
    _fq(Fl, t0 + 0.02, t1 - 0.02, y0 + 0.02, y1 - 0.02, r - 0.0015, "ih_contactor")     # what shows through it


def _iso_handle(Fl, t, y, r, on):
    """An isolating switch's operating handle in its slotted escutcheon, with the ON/OFF plate and padlock hasp."""
    Fl.box(t - 0.035, t + 0.035, y - 0.13, y + 0.13, r - 0.008, r, {"in": ["ih_onoff", 0, 0, 1, 1]})
    Fl.cyl((t, y, r - 0.008), (t, y, r - 0.035), 0.022, "ih_gear_dk", sides=10)
    a = math.radians(70 if on else -70)
    Fl.bar((t, y, r - 0.04), (t + 0.02 * math.cos(a), y + 0.14 * math.sin(a), r - 0.04), 0.03, 0.02, "ih_handle_red", up="r")
    Fl.box(t + 0.04, t + 0.06, y - 0.15, y - 0.11, r - 0.03, r, "ih_gear_dk")


def xfmr(Fl, train, plate=None):
    """A 225 kVA dry-type transformer, 4160 V to 480 V: ventilated enclosure on its sill, bolted covers louvred top and
    bottom, its temperature monitor (live), nameplate, labels; 1.2 m wide."""
    W, Y0, H = 1.2, DECK + 0.1, 1.9
    Fl.box(0.0, W, DECK, Y0, 0.04, 0.96, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})
    Fl.box(0.0, W, Y0, Y0 + H, 0.0, 1.0, {"in": None, "left": "ih_louvre_g", "right": "ih_louvre_g", "top": "ih_gear_top",
           "out": GEAR, "bottom": None})
    _fq(Fl, 0.0, W, Y0, Y0 + H, 0.0, "ih_gear_seam")
    for y0_, y1_, lv in ((Y0 + 0.03, Y0 + 0.5, True), (Y0 + 0.53, Y0 + 1.37, False), (Y0 + 1.4, Y0 + H - 0.03, True)):
        Fl.box(0.03, W - 0.03, y0_, y1_, -0.012, 0.0, {"in": "ih_louvre_g" if lv else GEAR, "left": GEAR, "right": GEAR,
               "top": GEAR, "bottom": GEAR})
        for a in (0.07, W - 0.07):
            for y in np.linspace(y0_ + 0.05, y1_ - 0.05, 3):
                Fl.cyl((a, y, -0.012), (a, y, -0.022), 0.011, "ct_galv", sides=6)
    _plate(Fl, W / 2, Y0 + 1.3, 0.5, 0.05, 14 + train if plate is None else plate, r=-0.012)
    Fl.box(0.75, 0.97, Y0 + 0.95, Y0 + 1.15, -0.06, -0.012, "ih_btn_k")                    # temperature monitor
    _fq(Fl, 0.765, 0.955, Y0 + 1.04, Y0 + 1.135, -0.062, "ih_tempmon", lit=True)
    _lamp(Fl, 0.8, Y0 + 0.99, -0.06, "g", True, rad=0.01)
    _lamp(Fl, 0.86, Y0 + 0.99, -0.06, "a", False, rad=0.01)
    Fl.box(0.82, 0.9, Y0 + 1.15, Y0 + 1.6, 0.02, 0.06, "ct_galv")                          # its conduit to the top
    _label(Fl, 0.35, Y0 + 0.95, 0.22, 0.11, "ih_danger_mv", r=-0.012)
    _label(Fl, 0.35, Y0 + 0.75, 0.2, 0.14, "ih_arcflash", r=-0.012)
    _fq(Fl, W * 0.5 - 0.3, W * 0.5 + 0.3, Y0 + 1.65, Y0 + 1.82, -0.014, "ih_louvre_g")
    return W


CW_MCC_LIST = [
    [[(3, 31, "brk"), (2, 78, "st"), (2, 80, "brk"), (3, 81, "st"), (2, 88, "brk")],
     [(2, 82, "st"), (2, 83, "brk"), (2, 84, "st"), (2, 85, "brk"), (2, 86, "st"), (2, 87, "brk")],
     [(2, 89, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk")]],
    [[(3, 31, "brk"), (2, 79, "st"), (2, 88, "brk"), (3, 88, "brk"), (2, 88, "brk")],
     [(2, 82, "st"), (2, 83, "brk"), (2, 84, "st"), (2, 88, "brk"), (2, 86, "st"), (2, 87, "brk")],
     [(2, 89, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk"), (2, 88, "brk")]],
]
MCC_LIST = [
    [(3, 31, "brk"), (2, 16, "st"), (2, 17, "st"), (3, 18, "st"), (2, 30, "brk")],
    [(2, 19, "st"), (2, 20, "rev"), (2, 21, "rev"), (2, 22, "rev"), (2, 23, "rev"), (2, 24, "st")],
    [(2, 25, "brk"), (2, 26, "st"), (2, 27, "brk"), (2, 28, "brk"), (2, 29, "brk"), (2, 30, "brk")],
]


def mcc(Fl, train, running_tws=(True, True), units=None):
    """A 480 V motor control centre, three sections 0.508 m wide, 0.51 deep: top and bottom horizontal wireways,
    a column of plug-in units (12 spaces of 152 mm), a vertical wireway door; each unit's door with its breaker
    handle in a slotted escutcheon (ON up), lamps, a nameplate; starters with start/stop and hand-off-auto."""
    W, Y0, H, SPC = 0.508, DECK + 0.1, 2.29, 0.1524
    L = 3 * W
    Fl.box(0.0, L, DECK, Y0, 0.03, 0.48, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})
    Fl.box(0.0, L, Y0, Y0 + H, 0.0, 0.51, {"in": None, "left": GEAR, "right": GEAR, "top": "ih_gear_top", "out": GEAR,
           "bottom": None})
    _fq(Fl, 0.0, L, Y0, Y0 + H, 0.0, "ih_gear_seam")
    _plate(Fl, L / 2, Y0 + H - 0.06, 0.5, 0.05, (12 if units is None else 74) + train, r=-0.012)
    circ = units is not None
    for k, units in enumerate(units or MCC_LIST):
        x0 = k * W
        Fl.box(x0 + 0.02, x0 + W - 0.02, Y0 + H - 0.3, Y0 + H - 0.12, -0.012, 0.0, {"in": "ih_louvre_g", "left": GEAR,
               "right": GEAR, "top": GEAR, "bottom": GEAR})                             # top wireway
        Fl.box(x0 + 0.02, x0 + W - 0.02, Y0 + 0.02, Y0 + 0.14, -0.012, 0.0, GEAR)     # bottom wireway
        Fl.box(x0 + W - 0.12, x0 + W - 0.02, Y0 + 0.17, Y0 + H - 0.32, -0.012, 0.0, GEAR)   # vertical wireway door
        y = Y0 + H - 0.32
        for spaces, pl, kind in units:
            if train == 1 and not circ:
                pl = pl + 16 if pl in range(16, 25) else {25: 41, 26: 42}.get(pl, pl)
            yb = y - spaces * SPC
            _door(Fl, x0 + 0.02, x0 + W - 0.13, yb + 0.004, y - 0.004, handle=False)
            # the breaker's handle in its escutcheon (ON up)
            hx, hy = x0 + 0.08, (yb + y) / 2
            Fl.box(hx - 0.025, hx + 0.025, hy - 0.06, hy + 0.06, -0.02, -0.015, "ih_gear_dk")
            Fl.bar((hx, hy - 0.01, -0.03), (hx, hy + 0.045, -0.03), 0.016, 0.02, "ih_btn_k", up="r")
            _plate(Fl, x0 + 0.24, y - 0.03, 0.18, 0.026, pl, r=-0.015)
            if kind in ("st", "rev"):
                on = (running_tws[0] if pl in (16, 32) else running_tws[1] if pl in (17, 33) else False) if not circ else pl in (78, 82)
                _lamp(Fl, x0 + 0.2, hy + 0.0, -0.015, "r", on, rad=0.011)
                _lamp(Fl, x0 + 0.24, hy + 0.0, -0.015, "g", not on, rad=0.011)
                if spaces >= 2:
                    _button(Fl, x0 + 0.3, hy + 0.0, -0.015, "ih_btn_g", rad=0.011)
                    _button(Fl, x0 + 0.34, hy + 0.0, -0.015, "ih_btn_r", rad=0.011)
                    _selector(Fl, x0 + 0.27, hy - 0.05, -0.015, 1)
            for qy in (yb + 0.03, y - 0.03):
                Fl.cyl((x0 + W - 0.16, qy, -0.015), (x0 + W - 0.16, qy, -0.022), 0.008, "ct_galv", sides=6)
            y = yb
        _label(Fl, x0 + 0.2, Y0 + 0.085, 0.2, 0.1, "ih_arcflash", r=-0.012)
    return L


def lcp(Fl, running, levels, flows, press, ann_lit=()):
    """The intake's local control panel, 3.0 m wide, 0.8 deep, 2.29 tall on its sill: three hinged front doors carrying
    the bay levels and the screens' controls (left), the annunciator, the operator's screen and the alarm buttons
    (middle), the pumps' control switches, lamps and ammeters, the flows and header pressures (right); a horn and the
    title plate on top."""
    W, Y0, H = 3.0, DECK + 0.1, 2.29
    Fl.box(0.0, W, DECK, Y0, 0.04, 0.76, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})
    Fl.box(0.0, W, Y0, Y0 + H, 0.0, 0.8, {"in": None, "left": GEAR, "right": GEAR, "top": "ih_gear_top", "out": GEAR,
           "bottom": None})
    _fq(Fl, 0.0, W, Y0, Y0 + H, 0.0, "ih_gear_seam")
    for k in range(3):
        _door(Fl, k + 0.02, k + 0.98, Y0 + 0.05, Y0 + H - 0.2, hinge="left" if k < 2 else "right")
    _plate(Fl, W / 2, Y0 + H - 0.1, 1.6, 0.09, 43, r=-0.0)
    # ---- left door: bay levels (LT-nA, LT-nB) and the screens (dp edgewise meter, run lamp, dp-hi lamp, HOA)
    _plate(Fl, 0.5, Y0 + 1.95, 0.4, 0.04, 44, r=-0.015)
    for b in range(4):
        for k in range(2):
            t = 0.2 + 0.22 * b + (0.0 if k == 0 else 0.0)
            yy = Y0 + 1.82 - 0.12 * k
            _digital(Fl, t + 0.0, yy, -0.015, 2 * b + k, w=0.17, h=0.07)
            _label(Fl, t, yy - 0.05, 0.14, 0.02, ["ih_ltlabels", 0, (2 * b + k) / 8, 1, (2 * b + k + 1) / 8], r=-0.015)
    _plate(Fl, 0.5, Y0 + 1.42, 0.4, 0.04, 45, r=-0.015)
    for b in range(4):
        t = 0.2 + 0.2 * b
        Fl.box(t - 0.03, t + 0.03, Y0 + 1.06, Y0 + 1.34, -0.04, -0.015, "ih_btn_k")             # edgewise dp meter
        _fq(Fl, t - 0.022, t + 0.022, Y0 + 1.08, Y0 + 1.32, -0.042, "ih_edge")
        yp = Y0 + 1.08 + 0.24 * (2.0 / 12.0)                                                  # 2 in WC of 12
        Fl.box(t - 0.022, t + 0.022, yp - 0.002, yp + 0.002, -0.046, -0.044, "ih_needle_r")
        _lamp(Fl, t - 0.035, Y0 + 0.98, -0.015, "r", True, rad=0.012)
        _lamp(Fl, t + 0.035, Y0 + 0.98, -0.015, "a", False, rad=0.012)
        _selector(Fl, t, Y0 + 0.88, -0.015, 1)
        _plate(Fl, t, Y0 + 0.81, 0.13, 0.022, 16 + b if b < 2 else 32 + b - 2, r=-0.015)
    _label(Fl, 0.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    # ---- middle door: annunciator (4 x 6), operator screen, alarm buttons
    _plate(Fl, 1.5, Y0 + 1.95, 0.4, 0.04, 46, r=-0.015)
    Fl.box(1.08, 1.92, Y0 + 1.5, Y0 + 1.9, -0.07, -0.015, "ih_btn_k")
    for k, legend in enumerate(ANN):
        c, r_ = k % 6, k // 6
        t0 = 1.1 + c * 0.1333
        y1 = Y0 + 1.88 - r_ * 0.095
        lit = legend in ann_lit
        _fq(Fl, t0 + 0.006, t0 + 0.127, y1 - 0.088, y1 - 0.004, -0.072,
            ["ih_ann_on" if lit else "ih_ann", (k % 4) / 4, (k // 4) / 8, (k % 4 + 1) / 4, (k // 4 + 1) / 8], lit=lit)
    Fl.box(1.15, 1.85, Y0 + 0.92, Y0 + 1.4, -0.05, -0.015, "ih_btn_k")                       # operator screen
    _fq(Fl, 1.17, 1.83, Y0 + 0.94, Y0 + 1.38, -0.052, "ih_hmi", lit=True)
    for k, (mat, pl) in enumerate((("ih_btn_k", 61), ("ih_btn_k", 62), ("ih_btn_w", 63), ("ih_btn_r", 57))):
        _button(Fl, 1.29 + 0.14 * k, Y0 + 0.8, -0.015, mat, rad=0.02)
        _plate(Fl, 1.29 + 0.14 * k, Y0 + 0.75, 0.1, 0.02, pl, r=-0.015)
        _zone(Fl, ("ann_ack", "ann_silence", "ann_test", "ann_reset")[k], 1.29 + 0.14 * k - 0.04, 1.29 + 0.14 * k + 0.04,
              Y0 + 0.76, Y0 + 0.84, -0.05, 0.0)
    _label(Fl, 1.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    # ---- right door: pumps (lamps, pistol grip, ammeter), flows, header pressures
    _plate(Fl, 2.5, Y0 + 1.95, 0.4, 0.04, 47, r=-0.015)
    for p in range(6):
        t = 2.1 + 0.16 * p
        run = running[p]
        _lamp(Fl, t - 0.035, Y0 + 1.84, -0.015, "r", run, rad=0.012)
        _lamp(Fl, t, Y0 + 1.84, -0.015, "g", not run, rad=0.012)
        _lamp(Fl, t + 0.035, Y0 + 1.84, -0.015, "a", False, rad=0.012)
        _pistol(Fl, t, Y0 + 1.7, -0.015, 0)
        _zone(Fl, f"pump:{p}", t - 0.05, t + 0.05, Y0 + 1.64, Y0 + 1.76, -0.06, 0.0)
        _label(Fl, t, Y0 + 1.62, 0.12, 0.022, ["ih_tags3", (p % 4) / 4, (p // 4) / 8, (p % 4 + 1) / 4, (p // 4 + 1) / 8], r=-0.015)
        fla = 102 if p in (0, 5) else 127
        _meter(Fl, t, Y0 + 1.5, -0.015, 0.89 * fla if run else 0, 200, "ih_mtr_a", size=0.1)
    _plate(Fl, 2.5, Y0 + 1.34, 0.4, 0.04, 48, r=-0.015)
    for k in range(4):
        _digital(Fl, 2.14 + 0.24 * k, Y0 + 1.24, -0.015, 8 + k, w=0.19, h=0.07)
        _label(Fl, 2.14 + 0.24 * k, Y0 + 1.18, 0.15, 0.02, ["ih_tags3", ((12 + k) % 4) / 4, ((12 + k) // 4) / 8,
                                                              ((12 + k) % 4 + 1) / 4, ((12 + k) // 4 + 1) / 8], r=-0.015)
    _plate(Fl, 2.5, Y0 + 1.06, 0.4, 0.04, 49, r=-0.015)
    for k in range(3):
        _digital(Fl, 2.2 + 0.3 * k, Y0 + 0.96, -0.015, 12 + k, w=0.19, h=0.07)
    _label(Fl, 2.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    # the horn and a beacon on top
    Fl.box(1.4, 1.6, Y0 + H, Y0 + H + 0.12, 0.2, 0.35, "ih_btn_k")
    Fl.cyl((1.5, Y0 + H + 0.06, 0.2), (1.5, Y0 + H + 0.06, 0.08), 0.06, "ih_btn_k", sides=14)
    return W


def elec_ops(ox=0.0, oz=0.0, running=(True, True, False, True, False, False), roof=True, ann_lit=(), frame=None,
             kind="intake", X=None, valve_open=(True,) * 4):
    """Piece 4: the electrical room (its walls, floor, ceiling, doors and fittings) with its equipment, at the intake's
    west end, its floor at the deck's level. Piece 7 builds the circulating water pump house's from the same parts
    (kind "circ", drawn in a frame turned to stand at that hall's east side, X its own length): its trains' 13.8 kV
    switchgear in place of the 4160 V lineups, its own MCC units and panel."""
    o = frame.o if frame is not None else CT.Ops()
    if frame is None:
        F = CT.Frame(o, math.pi)
        F.org = np.array([ox, 0.0, oz])
    else:
        F = frame
    X0, X1 = X or ER_X
    D0, D1 = ER_D
    T = ER_WALL
    IX0, IX1, ID0, ID1 = X0 + T, X1 - T, D0 + T, D1 - T
    C, CMU = "ih_concrete", "ih_cmu"
    # ---- foundation, floor slab, walls (block above the floor, concrete below), the doors' openings
    F.box(X0, X1, -0.9, -0.6, D0, D1, {"out": C, "in": C, "left": C, "right": C, "top": None, "bottom": None})   # footing
    F.box(IX0, IX1, DECK - 0.3, DECK, ID0, ID1, {"top": "ih_floor", "out": None, "in": None, "left": None, "right": None,
          "bottom": None})
    yt = ER_CEIL + 0.25
    def wall(x0, x1, d0, d1, inner):
        F.box(x0, x1, -0.6, DECK, d0, d1, {"out": C, "in": C, "left": C, "right": C, "top": None, "bottom": None})
        f = {"out": "ih_cmu_ext", "in": "ih_cmu_ext", "left": "ih_cmu_ext", "right": "ih_cmu_ext", "top": "ih_coping",
             "bottom": None}
        f[inner] = CMU
        f["top"] = None if roof else "ih_coping"
        F.box(x0, x1, DECK, yt, d0, d1, f)
        if roof:                                                                            # its parapet
            F.box(x0, x1, yt, yt + 0.6, d0, d1, {"out": "ih_cmu_ext", "in": "ih_cmu_ext", "left": "ih_cmu_ext",
                  "right": "ih_cmu_ext", "top": "ih_coping", "bottom": None})
    wall(X0, X0 + T, D0, D1, "right")                                                       # west
    wall(X0 + T, X1 - T, D0, D0 + T, "out")                                                 # south
    wall(X0 + T, X1 - T, D1 - T, D1, "in")                                                  # north
    eds = [D0] + [v for dd in ER_DOORS for v in dd] + [D1]
    for a, b in zip(eds[0::2], eds[1::2]):                                                  # east, round the doors
        f = {"out": "ih_cmu_ext", "in": "ih_cmu_ext", "left": CMU, "right": CMU, "top": "ih_coping", "bottom": None}
        F.box(X1 - T, X1, -0.6, DECK, a, b, {"out": C, "in": C, "left": C, "right": C, "top": None, "bottom": None})
        f["top"] = None if roof else "ih_coping"
        F.box(X1 - T, X1, DECK, yt, a, b, f)
    if roof:
        F.box(X1 - T, X1, yt, yt + 0.6, D0, D1, {"out": "ih_cmu_ext", "in": "ih_cmu_ext", "left": "ih_cmu_ext",
              "right": "ih_cmu_ext", "top": "ih_coping", "bottom": None})
    for a, b in ER_DOORS:
        F.box(X1 - T, X1, -0.6, DECK, a, b, {"out": C, "in": C, "left": C, "right": C, "top": None, "bottom": None})
        F.box(X1 - T, X1, DECK + 2.18, yt, a, b, {"left": CMU, "right": CMU, "bottom": CMU, "top": None if roof else "ih_coping",
              "out": None, "in": None})
        _hm_door(F, X1 - T, X1, a, b, name=f"er{ER_DOORS.index((a, b)) + 1}")
    if roof:
        F.box(IX0, IX1, yt, yt + 0.12, ID0, ID1, {"top": "ih_roof", "out": None, "in": None, "left": None, "right": None,
              "bottom": None})                                                             # the membrane roof
        F.box(IX0, IX1, ER_CEIL, yt, ID0, ID1, {"bottom": "ih_roofdeck", "out": None, "in": None, "left": None, "right": None,
              "top": None})
        # open-web joists across the room (along d), bearing on the side walls
        for xj in np.arange(IX0 + 0.75, IX1 - 0.3, 1.5):
            for y0_, y1_ in ((ER_CEIL - 0.05, ER_CEIL), (ER_CEIL - 0.5, ER_CEIL - 0.45)):
                for s in (-1, 1):
                    F.box(xj + s * 0.025 - 0.02, xj + s * 0.025 + 0.02, y0_, y1_, ID0, ID1, "ih_joist")
            n = int((ID1 - ID0) / 0.6)
            for k in range(n):
                da = ID0 + k * (ID1 - ID0) / n
                db = ID0 + (k + 1) * (ID1 - ID0) / n
                ya, yb_ = (ER_CEIL - 0.45, ER_CEIL - 0.05) if k % 2 == 0 else (ER_CEIL - 0.05, ER_CEIL - 0.45)
                F.cyl((xj, ya, da), (xj, yb_, db), 0.012, "ih_joist", sides=6)
        # LED strip fixtures on chains, in three rows; the emergency lights and exit signs over the doors
        for dl in (-4.4, -2.5, -0.6):
            for xl in np.arange(IX0 + 1.2, IX1 - 0.5, 2.0):
                F.box(xl - 0.6, xl + 0.6, 3.72, 3.8, dl - 0.08, dl + 0.08, "ih_fixture")
                _fq_world(F, xl - 0.58, xl + 0.58, dl - 0.06, dl + 0.06, 3.719, "ih_led")
                for s in (-0.5, 0.5):
                    F.cyl((xl + s, 3.8, dl), (xl + s, ER_CEIL - 0.5, dl), 0.004, "ct_galv", sides=4)
        for a, b in ER_DOORS:
            dm = (a + b) / 2
            F.box(IX1 - 0.07, IX1, DECK + 2.32, DECK + 2.6, dm - 0.2, dm + 0.2, {"left": ["ih_exit", 0, 0, 1, 1], "out": "ih_fixture",
                  "in": "ih_fixture", "top": "ih_fixture", "bottom": "ih_fixture"}, lit=True)
            F.box(IX1 - 0.12, IX1, DECK + 2.7, DECK + 2.95, dm - 0.75, dm - 0.35, "ih_fixture")       # emergency light
            for s in (-0.1, 0.1):
                F.cyl((IX1 - 0.12, DECK + 2.8, dm - 0.55 + s), (IX1 - 0.2, DECK + 2.76, dm - 0.55 + s * 1.6), 0.035,
                      "ih_fixture", "ih_lamp_w", sides=10)
    # ---- the two trains' lineups: 4160 V gear, transformer, 480 V MCC (train 1 on the south wall facing north)
    for train in (0, 1):
        if train == 0:
            Fl = F.sub(-1.5, ID0 + 1.1, math.pi)      # origin at its left end (seen from the aisle), front at d -5.2
        else:
            Fl = F.sub(IX0 + 0.35, ID1 - 1.1, 0.0)
        run3 = ((running[0], running[1], running[2]) if train == 0 else (running[5], running[3], running[4])) if kind == "intake" else None
        t = 0.0
        gear = ("mv", 0.914 * 3) if kind == "intake" else ("sw", 0.914 * 4)
        if train == 0:
            order = [("mcc", 0.508 * 3), ("xf", 1.2), gear]
        else:
            order = [gear, ("xf", 1.2), ("mcc", 0.508 * 3)]
        for kind_, w in order:
            Fs = CT.Frame(o)
            Fs.t, Fs.r, Fs.org = Fl.t, Fl.r, Fl.w(t, 0.0, 0.0)
            if kind_ == "mv":
                mv_lineup(Fs, train, run3)
            elif kind_ == "sw":
                swgr_lineup(Fs, train, running[2 * train:2 * train + 2])
            elif kind_ == "xf":
                xfmr(Fs, train, plate=(14 if kind == "intake" else 76) + train)
            else:
                mcc(Fs, train, units=None if kind == "intake" else CW_MCC_LIST[train])
            t += w + 0.1
        # the rubber mat in front, the cable tray over the lineup on its trapeze hangers
        Fl.box(0.0, t - 0.1, DECK, DECK + 0.006, -1.1, -0.05, "ih_mat")
        if roof:
            dtr = ID0 + 0.45 if train == 0 else ID1 - 0.45
            _tray(F, IX0 + 0.3, IX1 - 0.3, dtr, 3.6)
    if roof:
        _tray_d(F, IX0 + 0.45, ID0 + 0.75, ID1 - 0.75, 3.605)
    # ---- the local control panel on the west wall, facing east
    Fp = CT.Frame(o)
    Fl0 = F.sub(IX0 + 0.85, -4.0, math.pi / 2)
    Fp.t, Fp.r, Fp.org = Fl0.t, Fl0.r, Fl0.org
    if kind == "intake":
        lcp(Fp, running, [6.0, 5.95] * 4, None, None, ann_lit)
    else:
        cw_lcp(Fp, running, valve_open, ann_lit)
    # ---- on the east wall between the doors: lighting panel and its transformer, the one-line, phone, extinguishers
    Fe = CT.Frame(o)
    Fe0 = F.sub(IX1, -1.75, -math.pi / 2)          # facing west: t toward -d (the operator's right), front -r = -x
    Fe.t, Fe.r, Fe.org = Fe0.t, Fe0.r, Fe0.org
    _wall_fittings(Fe, kind)
    # ---- the west wall's air conditioner (through the wall) and thermostat, above the panel's north end
    Fw = CT.Frame(o)
    Fw0 = F.sub(IX0, -0.9, math.pi / 2)
    Fw.t, Fw.r, Fw.org = Fw0.t, Fw0.r, Fw0.org
    Fw.box(0.0, 1.0, DECK + 2.5, DECK + 3.2, -0.35, 0.0, {"in": ["ih_hvac", 0, 0, 1, 1], "left": "ih_hvac_side", "right": "ih_hvac_side",
           "top": "ih_hvac_side", "bottom": "ih_hvac_side"})
    Fw.box(0.05, 0.95, DECK + 2.45, DECK + 2.5, -0.3, 0.0, "ih_hvac_side")
    Fw.cyl((0.9, DECK + 2.45, -0.2), (0.9, DECK + 1.4, -0.2), 0.012, "ih_pvc", sides=6)               # condensate
    Fw.cyl((0.9, DECK + 1.4, -0.2), (0.9, DECK + 1.4, 0.0), 0.012, "ih_pvc", sides=6)
    return o.ops() if frame is None else None


def _fq_world(F, x0, x1, d0, d1, y, mat):
    """A quad facing down (a fixture's lens), lit."""
    F._quad([F.w(x0, y, d0), F.w(x1, y, d0), F.w(x1, y, d1), F.w(x0, y, d1)], mat, (0, -1, 0), lit=True)


def _hm_door(F, x0, x1, d0, d1, name=None):
    """A hollow metal door in its frame, the leaf flush with the wall's outside, opening out onto the deck: three
    hinges, a closer, a kick plate; the panic bar inside; its sign outside. In game the leaf (with its hardware and
    sign) swings out about its hinges while the door is open (name in OPEN), and is a click zone."""
    H = DECK + 2.13
    for a, b in ((d0, d0 + 0.05), (d1 - 0.05, d1)):
        F.box(x0, x1 + 0.01, DECK, H + 0.05, a, b, "ih_hmframe")
    F.box(x0, x1 + 0.01, H, H + 0.05, d0 + 0.05, d1 - 0.05, "ih_hmframe")
    lf0, lf1 = x1 - 0.05, x1 - 0.005
    for y in (DECK + 0.3, DECK + 1.1, DECK + 1.9):                                       # hinges (on its south side)
        F.cyl((x1 - 0.0, y - 0.06, d0 + 0.06), (x1 - 0.0, y + 0.06, d0 + 0.06), 0.012, "ct_galv", sides=8)
    L = _Moved(F.sub(x1, d0 + 0.06, -math.pi / 2 if name in OPEN else 0.0), x1, d0 + 0.06)
    L.box(lf0, lf1, DECK + 0.01, H, d0 + 0.053, d1 - 0.053, {"left": "ih_hmdoor", "right": "ih_hmdoor", "out": "ih_hmdoor",
          "in": "ih_hmdoor", "top": "ih_hmdoor", "bottom": None})
    L.box(lf1, lf1 + 0.003, DECK + 0.02, DECK + 0.27, d0 + 0.08, d1 - 0.08, {"right": "ih_kick"})    # kick plate (out)
    # panic bar inside
    L.box(lf0 - 0.06, lf0, DECK + 0.95, DECK + 1.05, d0 + 0.12, d1 - 0.1, "ih_panic")
    L.box(lf0 - 0.04, lf0 - 0.001, DECK + 0.9, DECK + 1.1, d1 - 0.18, d1 - 0.08, "ih_panic")
    # closer inside at the head
    F.box(lf0 - 0.07, lf0, H - 0.12, H - 0.05, d0 + 0.1, d0 + 0.42, "ih_closer")
    if name not in OPEN:
        F.bar((lf0 - 0.05, H - 0.06, d0 + 0.4), (x0 - 0.02, H + 0.02, d0 + 0.6), 0.02, 0.012, "ih_closer", up="y")
    # its sign outside
    L.box(lf1, lf1 + 0.002, DECK + 1.45, DECK + 1.75, (d0 + d1) / 2 - 0.2, (d0 + d1) / 2 + 0.2, {"right": ["ih_er_sign", 0, 0, 1, 1]})
    if name:
        _zone(F, "door:" + name, x0 - 0.1, x1 + 0.1, DECK, H, d0, d1)


def _tray(F, x0, x1, d, y):
    """A ladder cable tray along x, 0.6 m wide, on trapeze hangers every 1.5 m."""
    for s in (-0.3, 0.3):
        F.box(x0, x1, y, y + 0.1, d + s - 0.005, d + s + 0.005, "ct_galv")
    for xr in np.arange(x0 + 0.15, x1, 0.23):
        F.box(xr - 0.012, xr + 0.012, y, y + 0.025, d - 0.295, d + 0.295, "ct_galv")
    for xh in np.arange(x0 + 0.3, x1, 1.5):
        F.box(xh - 0.02, xh + 0.02, y - 0.04, y, d - 0.4, d + 0.4, "ct_galv")
        for s in (-0.36, 0.36):
            F.cyl((xh, y - 0.04, d + s), (xh, ER_CEIL - 0.5, d + s), 0.006, "ct_galv", sides=6)
    F.box(x0 + 0.02, x1 - 0.02, y + 0.025, y + 0.06, d - 0.25, d + 0.2, "ih_cables")


def _tray_d(F, x, d0, d1, y):
    """The cross tray along d over the panel, joining the two lineups' trays."""
    for s in (-0.3, 0.3):
        F.box(x + s - 0.005, x + s + 0.005, y, y + 0.1, d0, d1, "ct_galv")
    for dr in np.arange(d0 + 0.15, d1, 0.23):
        F.box(x - 0.295, x + 0.295, y, y + 0.025, dr - 0.012, dr + 0.012, "ct_galv")
    for dh in np.arange(d0 + 0.6, d1, 1.5):
        F.box(x - 0.4, x + 0.4, y - 0.04, y, dh - 0.02, dh + 0.02, "ct_galv")
        for s in (-0.36, 0.36):
            F.cyl((x + s, y - 0.04, dh), (x + s, ER_CEIL - 0.5, dh), 0.006, "ct_galv", sides=6)
    F.box(x - 0.25, x + 0.2, y + 0.025, y + 0.06, d0 + 0.62, d1 - 0.62, "ih_cables")


def _wall_fittings(Fe, kind="intake"):
    """The east wall between the doors, in its frame (t along the wall to the operator's right, front at r 0 facing
    -r): lighting panel LP-1 and its 15 kVA transformer, the framed one-line diagram, the phone, an extinguisher."""
    Fe.box(0.3, 0.8, DECK + 1.2, DECK + 2.3, -0.15, 0.0, {"in": ["ih_lp", 0, 0, 1, 1], "left": GEAR, "right": GEAR, "top": GEAR,
           "bottom": GEAR})
    _plate(Fe, 0.55, DECK + 2.25, 0.36, 0.035, 50 if kind == "intake" else 99, r=-0.15)
    Fe.box(0.3, 0.85, DECK, DECK + 0.75, -0.45, 0.0, {"in": "ih_louvre_g", "left": "ih_louvre_g", "right": "ih_louvre_g",
           "top": GEAR, "bottom": None})                                                    # 15 kVA transformer
    _conduit(Fe, [(0.55, DECK + 0.75, -0.2), (0.55, DECK + 1.2, -0.2)], 0.02)
    _conduit(Fe, [(0.42, DECK + 2.3, -0.08), (0.42, 3.6, -0.08)], 0.026)
    # the one-line diagram, framed under glass
    Fe.box(1.1, 2.3, DECK + 1.1, DECK + 2.0, -0.03, 0.0, "ih_frame_bronze")
    _fq(Fe, 1.13, 2.27, DECK + 1.13, DECK + 1.97, -0.031, "ih_oneline" if kind == "intake" else "ih_oneline_cw")
    _fq(Fe, 1.13, 2.27, DECK + 1.13, DECK + 1.97, -0.034, "ih_fglass")
    # the phone, the extinguisher on its hook with the sign over it
    Fe.box(2.6, 2.8, DECK + 1.35, DECK + 1.6, -0.08, 0.0, "ih_phone")
    Fe.box(2.62, 2.66, DECK + 1.4, DECK + 1.58, -0.11, -0.08, "ih_btn_k")
    Fe.cyl((3.0, DECK + 0.25, -0.1), (3.0, DECK + 0.85, -0.1), 0.085, "ih_ext", sides=16)
    Fe.cyl((3.0, DECK + 0.85, -0.1), (3.0, DECK + 0.95, -0.1), 0.03, "ih_btn_k", sides=8)
    Fe.box(2.96, 3.04, DECK + 0.9, DECK + 0.98, -0.02, 0.0, "ih_gear_dk")
    Fe.cyl((3.0, DECK + 0.93, -0.1), (3.08, DECK + 0.6, -0.17), 0.012, "ih_btn_k", sides=6)
    Fe.box(2.85, 3.15, DECK + 1.15, DECK + 1.45, -0.003, 0.0, {"in": ["ih_ext_sign", 0, 0, 1, 1]})



def elec_textures(running=(True, True, False, True, False, False)):
    """Piece 4's materials, crisp (128 px a metre where they repeat)."""
    import framed_glass as FG
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    FR = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf"
    FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
    f = lambda n, path=FB: ImageFont.truetype(path, n)
    out["ih_gear"] = _paint((178, 182, 182), 801, mottle=0.015, grain=0.006)              # ANSI 61 grey enamel
    out["ih_gear_dk"] = _paint((64, 66, 70), 802, mottle=0.02)
    gt = _paint((170, 174, 174), 803, mottle=0.03, grain=0.01)
    out["ih_gear_top"] = gt
    gs = _paint((150, 154, 154), 804)
    out["ih_gear_seam"] = gs
    lv = _paint((178, 182, 182), 805, mottle=0.015, grain=0.006)
    dlv = ImageDraw.Draw(lv)
    for y in range(6, 128, 10):
        dlv.rectangle([8, y, 119, y + 4], fill=(40, 42, 46, 255))
        dlv.line([(8, y + 5), (119, y + 5)], fill=(214, 218, 218, 255))
    out["ih_louvre_g"] = lv
    out["ih_lamacoid"] = Image.new("RGBA", (8, 8), (24, 24, 26, 255))
    ep = Image.new("RGBA", (1024, 4096), (22, 22, 24, 255))
    dep = ImageDraw.Draw(ep)
    for i, text in enumerate(EPLATES):
        c, r = i % 2, i // 2
        x0, y0 = c * 512, r * 64
        size = 40
        while dep.textlength(text, font=f(size)) > 470 and size > 14:
            size -= 1
        dep.text((x0 + 256, y0 + 32), text, font=f(size), fill=(240, 240, 236, 255), anchor="mm")
    out["ih_eplates"] = ep
    # switchboard meters: black case, white 270-degree scale
    for name, vmax, unit, step in (("ih_mtr_a", 200, "AMPS", 50), ("ih_mtr_a4", 400, "AMPS", 100), ("ih_mtr_a5", 50, "AMPS", 10),
                                   ("ih_mtr_v", 6000, "VOLTS", 1500)):
        im = Image.new("RGBA", (256, 256), (26, 26, 28, 255))
        di = ImageDraw.Draw(im)
        di.rectangle([14, 14, 241, 241], fill=(244, 244, 238, 255))
        n = int(vmax / step)
        for k in range(n * 5 + 1):
            a = math.radians(225 - 270 * k / (n * 5))
            r0 = 78 if k % 5 == 0 else 90
            di.line([(128 + r0 * math.cos(a), 128 - r0 * math.sin(a)), (128 + 100 * math.cos(a), 128 - 100 * math.sin(a))],
                    fill=(20, 20, 20, 255), width=4 if k % 5 == 0 else 2)
            if k % 5 == 0:
                v = int(step * k / 5)
                lbl = f"{v / 1000:g}" if vmax >= 1000 else str(v)
                di.text((128 + 58 * math.cos(a), 128 - 58 * math.sin(a)), lbl, font=f(20), fill=(20, 20, 20, 255), anchor="mm")
        di.text((128, 186), "KV" if vmax >= 1000 else unit, font=f(22), fill=(20, 20, 20, 255), anchor="mm")
        out[name] = im
    out["ih_needle_r"] = Image.new("RGBA", (4, 4), (200, 30, 24, 255))
    # LCDs: the multifunction meter, the protection relays (6 rows), the transformer's temperature monitor
    def lcd(w, h, lines, size, bg=(156, 196, 132), ink=(18, 30, 14)):
        im = Image.new("RGBA", (w, h), bg + (255,))
        d = ImageDraw.Draw(im)
        for k, line in enumerate(lines):
            d.text((10, 8 + k * (h - 12) / len(lines)), line, font=f(size, FM), fill=ink + (255,))
        return im
    out["ih_mfm"] = lcd(256, 192, ["V  4162", "A   214", "MW  1.31", "PF  0.88"], 34)
    rl = Image.new("RGBA", (256, 384), (0, 0, 0, 255))
    for k, lines in enumerate((["MUP  RUN", "Ia  91 A"], ["SWP  RUN", "Ia 113 A"], ["SWP STOP", "Ia   0 A"],
                               ["MUP  RUN", "Ia  91 A"], ["SWP  RUN", "Ia 113 A"], ["SWP STOP", "Ia   0 A"])):
        rl.paste(lcd(256, 64, lines, 22, bg=(40, 90, 150), ink=(230, 240, 250)), (0, 64 * k))
    out["ih_relay"] = rl
    out["ih_tempmon"] = lcd(192, 96, ["A 84 B 86", "C 85  C"], 26, bg=(30, 30, 30), ink=(255, 60, 40))
    for name, rgb in (("ih_lamp_a", (255, 184, 40)), ("ih_lamp_w", (255, 252, 236))):
        out[name] = Image.new("RGBA", (16, 16), rgb + (255,))
    out["ih_lamp_off_a"] = _paint((96, 70, 24), 806, n=16)
    out["ih_lamp_off_w"] = _paint((150, 150, 144), 807, n=16)
    out["ih_btn_w"] = _paint((220, 220, 214), 808, n=16)
    out["ih_frame_bronze"] = Image.new("RGBA", (8, 8), FG.FRAME + (255,))
    out["ih_fglass"] = FG._glass_texture(64)
    ct = Image.new("RGBA", (128, 96), (34, 36, 40, 255))                                  # a contactor seen in its window
    dct = ImageDraw.Draw(ct)
    for k in range(3):
        dct.rectangle([12 + 38 * k, 18, 40 + 38 * k, 80], fill=(196, 192, 180, 255), outline=(90, 88, 84, 255))
        dct.rectangle([18 + 38 * k, 28, 34 + 38 * k, 44], fill=(160, 40, 34, 255))
    out["ih_contactor"] = ct
    oo = Image.new("RGBA", (64, 192), (64, 66, 70, 255))
    doo = ImageDraw.Draw(oo)
    doo.rectangle([26, 30, 38, 162], fill=(20, 20, 22, 255))
    doo.text((32, 14), "ON", font=f(18), fill=(240, 240, 236, 255), anchor="mm")
    doo.text((32, 178), "OFF", font=f(16), fill=(240, 240, 236, 255), anchor="mm")
    out["ih_onoff"] = oo
    out["ih_handle_red"] = _paint((190, 34, 28), 809, n=16)
    def label(w, h, head, hcol, hink, lines, sym=False):
        im = Image.new("RGBA", (w, h), (250, 250, 246, 255))
        d = ImageDraw.Draw(im)
        d.rectangle([0, 0, w - 1, int(h * 0.26)], fill=hcol + (255,))
        if sym:
            cx, cy = int(h * 0.15), int(h * 0.13)
            d.polygon([(cx, cy - 18), (cx - 20, cy + 16), (cx + 20, cy + 16)], fill=(255, 220, 0, 255), outline=(0, 0, 0, 255))
            d.text((cx, cy + 4), "!", font=f(22), fill=(0, 0, 0, 255), anchor="mm")
        d.text((w / 2 + (14 if sym else 0), h * 0.13), head, font=f(int(h * 0.17)), fill=hink + (255,), anchor="mm")
        for k, (line, bold) in enumerate(lines):
            d.text((w / 2, h * 0.36 + k * h * 0.64 / len(lines)), line, font=f(int(h * 0.085), FB if bold else FR),
                   fill=(10, 10, 10, 255), anchor="mm")
        d.rectangle([1, 1, w - 2, h - 2], outline=(30, 30, 30, 255), width=2)
        return im
    out["ih_arcflash"] = label(360, 252, "WARNING", (242, 120, 0), (0, 0, 0),
                               [("Arc Flash and Shock Hazard", True), ("Appropriate PPE Required", False),
                                ("Incident energy at 36 in: 6.8 cal/cm2", False), ("Arc flash boundary: 52 in", False),
                                ("Shock hazard: 4160 VAC", True), ("Limited approach: 5 ft 0 in", False),
                                ("PPE category 2", True)], sym=True)
    out["ih_arcflash_lv"] = label(360, 252, "WARNING", (242, 120, 0), (0, 0, 0),
                                  [("Arc Flash and Shock Hazard", True), ("Appropriate PPE Required", False),
                                   ("Incident energy at 18 in: 1.2 cal/cm2", False), ("Arc flash boundary: 19 in", False),
                                   ("Shock hazard: 120 VAC", True), ("Limited approach: 3 ft 6 in", False),
                                   ("PPE category 1", True)], sym=True)
    out["ih_danger_mv"] = label(360, 180, "DANGER", (200, 16, 22), (255, 255, 255),
                                [("HIGH VOLTAGE", True), ("4160 VOLTS", True), ("KEEP OUT", False)])
    out["ih_er_sign"] = label(400, 300, "NOTICE", (0, 92, 168), (255, 255, 255),
                              [("ELECTRICAL ROOM", True), ("Authorized Personnel Only", False), ("Doors must be kept closed", False)])
    out["ih_ext_sign"] = label(256, 256, "FIRE", (200, 16, 22), (255, 255, 255), [("EXTINGUISHER", True), ("CO2", True)])
    pp = Image.new("RGBA", (128, 128), (40, 40, 42, 255))
    dpp = ImageDraw.Draw(pp)
    dpp.ellipse([4, 4, 123, 123], fill=(200, 202, 200, 255))
    for txt, ang in (("STOP", 135), ("AUTO", 90), ("START", 45)):
        a = math.radians(ang)
        dpp.text((64 + 42 * math.cos(a), 64 - 42 * math.sin(a)), txt, font=f(16), fill=(10, 10, 10, 255), anchor="mm")
    out["ih_pistol_plate"] = pp
    # red LED digital indicators: levels (8), flows (4), header pressures (3)
    vals = ["6.00", "5.95"] * 4 + [f"{15000 if running[0] else 0}", f"{12000 * (running[1] + running[2])}",
                                    f"{12000 * (running[3] + running[4])}", f"{15000 if running[5] else 0}", "62", "94", "94", ""]
    dg = Image.new("RGBA", (1024, 512), (14, 6, 6, 255))
    ddg = ImageDraw.Draw(dg)
    for k, v in enumerate(vals):
        c, r = k % 4, k // 4
        ddg.text((c * 256 + 128, r * 64 + 32), v, font=f(46, FM), fill=(255, 52, 36, 255), anchor="mm")
    out["ih_digits"] = dg
    lt = Image.new("RGBA", (256, 512), (22, 22, 24, 255))
    dlt = ImageDraw.Draw(lt)
    for k in range(8):
        dlt.text((128, k * 64 + 32), f"LT-{k // 2 + 1}{'AB'[k % 2]}  BAY {k // 2 + 1}", font=f(34), fill=(240, 240, 236, 255),
                 anchor="mm")
    out["ih_ltlabels"] = lt
    ed = Image.new("RGBA", (64, 256), (240, 240, 232, 255))                              # edgewise dp meter: 0-12 in WC
    ded = ImageDraw.Draw(ed)
    for k in range(13):
        y = 248 - k * 20
        ded.line([(36, y), (60 if k % 2 == 0 else 50, y)], fill=(20, 20, 20, 255), width=2)
        if k % 2 == 0:
            ded.text((20, y), str(k), font=f(16), fill=(20, 20, 20, 255), anchor="mm")
    ded.rectangle([36, 8, 60, 48], fill=(250, 210, 40, 255))
    out["ih_edge"] = ed
    for name, bg, ink in (("ih_ann", (226, 226, 218), (40, 40, 40)), ("ih_ann_on", (255, 236, 160), (20, 20, 20))):
        an = Image.new("RGBA", (512, 512), bg + (255,))
        dan = ImageDraw.Draw(an)
        for k, text in enumerate(ANN):
            c, r = k % 4, k // 4
            x0, y0 = c * 128, r * 64
            dan.rectangle([x0, y0, x0 + 127, y0 + 63], outline=(150, 150, 146, 255), width=2)
            words = text.split(" ")
            l1 = " ".join(words[:2]) if len(words) > 2 else words[0]
            l2 = " ".join(words[2:]) if len(words) > 2 else " ".join(words[1:])
            dan.text((x0 + 64, y0 + 21), l1, font=f(19), fill=ink + (255,), anchor="mm")
            dan.text((x0 + 64, y0 + 44), l2, font=f(19), fill=ink + (255,), anchor="mm")
        out[name] = an
    # the operator screen: the intake's overview (grey high-performance style, red running, green stopped)
    hm = Image.new("RGBA", (640, 420), (200, 202, 204, 255))
    dh = ImageDraw.Draw(hm)
    dh.rectangle([0, 0, 639, 30], fill=(60, 70, 84, 255))
    dh.text((12, 15), "INTAKE OVERVIEW", font=f(20), fill=(240, 240, 240, 255), anchor="lm")
    dh.text((628, 15), "19-OCT-26  14:32", font=f(16), fill=(240, 240, 240, 255), anchor="rm")
    for b in range(4):
        x0 = 30 + b * 150
        dh.rectangle([x0, 60, x0 + 120, 250], outline=(90, 94, 98, 255), width=3)
        dh.rectangle([x0 + 3, 140, x0 + 117, 247], fill=(150, 176, 200, 255))
        dh.text((x0 + 60, 48), f"BAY {b + 1}  {BAYS[b]}", font=f(14), fill=(30, 30, 30, 255), anchor="mm")
        dh.rectangle([x0 + 20, 80, x0 + 100, 100], fill=(120, 124, 128, 255))
        dh.text((x0 + 60, 90), f"TWS-{b + 1}", font=f(13), fill=(255, 255, 255, 255), anchor="mm")
        dh.text((x0 + 60, 118), "DP 2.0 in", font=f(14), fill=(30, 30, 30, 255), anchor="mm")
        dh.text((x0 + 60, 160), "6.00 m", font=f(15), fill=(20, 20, 20, 255), anchor="mm")
    for n, bay, px, mu in pump_list():
        cx = 30 + bay * 150 + (60 if mu else (30 if px < sum(bay_x(bay)) / 2 else 90))
        col = (200, 40, 34) if running[n] else (40, 150, 70)
        dh.ellipse([cx - 18, 200, cx + 18, 236], fill=col + (255,), outline=(30, 30, 30, 255), width=2)
        dh.text((cx, 290), TAGS3[n], font=f(13), fill=(30, 30, 30, 255), anchor="mm")
        dh.line([(cx, 236), (cx, 270)], fill=(60, 60, 60, 255), width=3)
    dh.rectangle([20, 320, 620, 410], fill=(184, 186, 188, 255), outline=(120, 120, 120, 255))
    for k, (lbl, v) in enumerate((("MU FLOW", "15,000 gpm"), ("SW-1 FLOW", "12,000 gpm"), ("SW-2 FLOW", "12,000 gpm"),
                                  ("LAKE", "-1.00 m"), ("SW-1 PRESS", "94 psi"), ("SW-2 PRESS", "94 psi"))):
        x = 40 + (k % 3) * 200
        y = 342 + (k // 3) * 40
        dh.text((x, y), lbl, font=f(15), fill=(40, 40, 40, 255), anchor="lm")
        dh.text((x + 180, y), v, font=f(15), fill=(10, 10, 10, 255), anchor="rm")
    out["ih_hmi"] = hm
    # the room: painted block (8 x 16 in courses), sealed floor, roof, roof deck, joists, fixtures
    cm = _paint((214, 212, 204), 810, mottle=0.02, grain=0.02)
    dcm = ImageDraw.Draw(cm)
    for k in range(5):
        y = int(k * 128 / 5)
        dcm.line([(0, y), (127, y)], fill=(184, 182, 174, 255), width=2)
        for xx in range(0, 128, 52):
            x = (xx + (26 if k % 2 else 0)) % 128
            dcm.line([(x, y), (x, y + 25)], fill=(184, 182, 174, 255), width=2)
    out["ih_cmu"] = cm
    out["ih_floor"] = _paint((176, 176, 170), 811, mottle=0.04, grain=0.015)
    out["ih_roof"] = _paint((90, 92, 94), 812, mottle=0.03, grain=0.03)
    rd = _paint((196, 198, 198), 813, mottle=0.01)
    drd = ImageDraw.Draw(rd)
    for x in range(0, 128, 19):
        drd.rectangle([x, 0, x + 6, 127], fill=(170, 172, 172, 255))
    out["ih_roofdeck"] = rd
    out["ih_joist"] = _paint((120, 124, 128), 814, n=32)
    out["ih_fixture"] = _paint((230, 230, 226), 815, n=32, mottle=0.01)
    out["ih_led"] = Image.new("RGBA", (16, 16), (255, 255, 248, 255))
    ex = Image.new("RGBA", (256, 128), (250, 250, 248, 255))
    dex = ImageDraw.Draw(ex)
    dex.text((128, 64), "EXIT", font=f(80), fill=(220, 20, 20, 255), anchor="mm")
    dex.polygon([(220, 64), (196, 44), (196, 84)], fill=(220, 20, 20, 255))
    out["ih_exit"] = ex
    out["ih_hmframe"] = _paint((112, 118, 124), 816, n=32)
    out["ih_hmdoor"] = _paint((126, 134, 142), 817, mottle=0.015)
    out["ih_kick"] = _paint((190, 192, 192), 818, n=32)
    out["ih_panic"] = _paint((180, 182, 184), 819, n=32)
    out["ih_closer"] = _paint((60, 62, 66), 820, n=32)
    cb = Image.new("RGBA", (64, 64), (26, 26, 28, 255))
    dcb = ImageDraw.Draw(cb)
    for x in range(0, 64, 5):
        dcb.line([(x, 0), (x, 63)], fill=(52, 52, 56, 255), width=2)
    out["ih_cables"] = cb
    lp = _paint((178, 182, 182), 821, n=128)
    dlp = ImageDraw.Draw(lp)
    dlp.rectangle([4, 4, 123, 123], outline=(120, 124, 124, 255), width=2)
    dlp.rectangle([30, 50, 98, 100], fill=(240, 240, 232, 255), outline=(80, 80, 80, 255))
    for k in range(8):
        dlp.line([(34, 56 + 5 * k), (94, 56 + 5 * k)], fill=(120, 120, 120, 255))
    dlp.rectangle([104, 60, 112, 80], fill=(60, 62, 66, 255))
    out["ih_lp"] = lp
    ol = Image.new("RGBA", (512, 384), (248, 248, 242, 255))
    dol = ImageDraw.Draw(ol)
    dol.rectangle([4, 4, 507, 379], outline=(20, 20, 20, 255), width=2)
    dol.text((256, 20), "INTAKE ELECTRICAL ONE-LINE", font=f(18), fill=(10, 10, 10, 255), anchor="mm")
    for c, x in enumerate((128, 384)):
        dol.line([(x, 40), (x, 90)], fill=(10, 10, 10, 255), width=3)
        dol.text((x, 50), f"BUS {c + 1}A 4160 V", font=f(12), fill=(10, 10, 10, 255), anchor="lm")
        dol.line([(x - 100, 90), (x + 100, 90)], fill=(10, 10, 10, 255), width=4)
        for k, xx in enumerate((x - 80, x - 25, x + 30, x + 85)):
            dol.line([(xx, 90), (xx, 240)], fill=(10, 10, 10, 255), width=2)
            dol.ellipse([xx - 14, 240, xx + 14, 268], outline=(10, 10, 10, 255), width=2)
            dol.text((xx, 254), "M" if k < 3 else "T", font=f(12), fill=(10, 10, 10, 255), anchor="mm")
        dol.line([(x + 85, 268), (x + 85, 320)], fill=(10, 10, 10, 255), width=2)
        dol.line([(x + 20, 320), (x + 120, 320)], fill=(10, 10, 10, 255), width=3)
        dol.text((x + 70, 334), f"MCC-{c + 1} 480 V", font=f(12), fill=(10, 10, 10, 255), anchor="mm")
    dol.text((500, 370), "DWG E-1101 REV 3", font=f(10), fill=(10, 10, 10, 255), anchor="rm")
    out["ih_oneline"] = ol
    out["ih_phone"] = _paint((214, 206, 186), 822, n=32)
    out["ih_ext"] = _paint((196, 30, 26), 823, n=32)
    hv = _paint((222, 222, 216), 824, n=128)
    dhv = ImageDraw.Draw(hv)
    for y in range(10, 90, 6):
        dhv.line([(8, y), (119, y)], fill=(150, 150, 146, 255), width=2)
    dhv.rectangle([86, 96, 120, 120], fill=(60, 62, 66, 255))
    out["ih_hvac"] = hv
    out["ih_hvac_side"] = _paint((222, 222, 216), 825, n=32)
    mt = Image.new("RGBA", (64, 64), (30, 30, 32, 255))
    dmt = ImageDraw.Draw(mt)
    for y in range(0, 64, 4):
        dmt.line([(0, y), (63, y)], fill=(44, 44, 46, 255))
    out["ih_mat"] = mt
    return out


# ================================================================== piece 5: the pump hall and the building's outside
#
# Real-world basis: a US intake pump house is a steel-framed building over the pump deck: rigid frames on the deck's
# piers and end walls, insulated metal wall panels on girts over a concrete curb, a low-slope (1:12) standing-seam roof
# on Z purlins with gutters and downspouts, clerestory windows (here the plant's framed glass), intake louvres low and
# upblast exhaust fans on the roof for the motors' heat. A top-running double-girder bridge crane (15 t, for the motors
# and pump sections) runs the hall's length on runway beams on the columns' brackets, its bridge fed by a conductor bar.
# The make-up and service water lines leave through the south wall in sleeves with link seals; the roll-up door in the
# east gable opens onto a truck dock; personnel doors with canopies and wall packs, a landing and stair down to grade.
# The electrical room (piece 4) at the west end is painted block with a parapet and a membrane roof. Signs: the
# building's name and number, authorized personnel only, hearing protection required, overhead door, the crane's
# capacity.

HALL_X = (-0.4, 2 * END + NB * BAY_W + (NB - 1) * PIER + 0.4)     # its outside faces (west, east)
HALL_D = (-6.6, 1.5)                                              # (south, north): clear of the gantry on the deck
EAVE = DECK + 9.0
RIDGE_D = (HALL_D[0] + HALL_D[1]) / 2
RIDGE = EAVE + (HALL_D[1] - HALL_D[0]) / 2 / 12
FRAMES = [-0.25, 4.7, 8.9, 13.1, HALL_X[1] - 0.26]
DOOR_S = (0.1, 1.0)                                               # the personnel door in the south wall (x)
DOOR_N = (6.6, 7.5)                                               # the one onto the screens' deck (x)
ROLLUP = (-4.6, -1.0)                                             # the roll-up door in the east gable (d)
RUNWAY_D = (-5.85, 0.85)
RUNWAY_Y = DECK + 5.6                                             # runway beams' tops
SIGNS5 = ["INTAKE STRUCTURE", "BLDG 41", "AUTHORIZED PERSONNEL ONLY", "15 TON", "CAUTION  OVERHEAD DOOR",
          "KEEP CLEAR", "NO SMOKING"]


def _roof_y(d):
    """The roof's top surface over d (1:12 each way from the ridge)."""
    return RIDGE - abs(d - RIDGE_D) / 12


def _panels(F, along, p0, p1, a0, a1, y0, y1, holes, out_face, faces):
    """A wall's panels from a0 to a1 (along x, the wall between d p0..p1; or along d, between x p0..p1), y0 to y1, less
    the rectangular openings holes [(a0, a1, y0, y1)]: banded by y, run along the wall."""
    ys = sorted({y0, y1, *[h[2] for h in holes if y0 < h[2] < y1], *[h[3] for h in holes if y0 < h[3] < y1]})
    for ya, yb in zip(ys, ys[1:]):
        cuts = sorted((h[0], h[1]) for h in holes if h[2] <= ya and yb <= h[3])
        a = a0
        for c0, c1 in cuts + [(a1, a1)]:
            if c0 - a > 1e-6:
                if along == "x":
                    F.box(a, c0, ya, yb, p0, p1, faces)
                else:
                    F.box(p0, p1, ya, yb, a, c0, faces)
            a = max(a, c1)


def _window(F, along, p, a0, a1, y0, y1, out_sign):
    """A clerestory window in an opening: the bronze frame round it (sill, head, jambs, mullions at 1.5 m) and the
    plant's framed glass in its middle, seen both ways."""
    FR = "ih_frame_bronze"
    n = max(1, round((a1 - a0) / 1.5))
    def bx(b0, b1, z0, z1):
        if along == "x":
            F.box(b0, b1, z0, z1, p - 0.05, p + 0.05, FR)
        else:
            F.box(p - 0.05, p + 0.05, z0, z1, b0, b1, FR)
    bx(a0, a1, y0, y0 + 0.05)
    bx(a0, a1, y1 - 0.05, y1)
    for k in range(n + 1):
        a = a0 + (a1 - a0) * k / n
        bx(max(a0, a - 0.025) if 0 < k < n else (a0 if k == 0 else a1 - 0.05),
           min(a1, a + 0.025) if 0 < k < n else (a0 + 0.05 if k == 0 else a1), y0 + 0.05, y1 - 0.05)
    for s in (-1, 1):
        for k in range(n):
            b0 = a0 + (a1 - a0) * k / n + (0.05 if k == 0 else 0.025)
            b1 = a0 + (a1 - a0) * (k + 1) / n - (0.05 if k == n - 1 else 0.025)
            if along == "x":
                P = [F.w(b0, y0 + 0.05, p + s * 0.002), F.w(b1, y0 + 0.05, p + s * 0.002), F.w(b1, y1 - 0.05, p + s * 0.002),
                     F.w(b0, y1 - 0.05, p + s * 0.002)]
                F._quad(P, "ih_fglass", s * F.r)
            else:
                P = [F.w(p + s * 0.002, y0 + 0.05, b0), F.w(p + s * 0.002, y0 + 0.05, b1), F.w(p + s * 0.002, y1 - 0.05, b1),
                     F.w(p + s * 0.002, y1 - 0.05, b0)]
                F._quad(P, "ih_fglass", s * F.t)


def _wallpack(F, x, y, d, facing, on):
    """An LED wall pack: its housing and the lit lens under it (live: on from dusk)."""
    if facing in ("-d", "+d"):
        s = -1 if facing == "-d" else 1
        F.box(x - 0.16, x + 0.16, y - 0.12, y + 0.12, *sorted((d, d + s * 0.2)), "ih_wallpack")
        F._quad([F.w(x - 0.13, y - 0.121, d + s * 0.02), F.w(x + 0.13, y - 0.121, d + s * 0.02),
                 F.w(x + 0.13, y - 0.121, d + s * 0.18), F.w(x - 0.13, y - 0.121, d + s * 0.18)],
                "ih_lamp_w" if on else "ih_lens_off", (0, -1, 0), lit=on)
    else:
        s = -1 if facing == "-x" else 1
        F.box(*sorted((x, x + s * 0.2)), y - 0.12, y + 0.12, d - 0.16, d + 0.16, "ih_wallpack")
        F._quad([F.w(x + s * 0.02, y - 0.121, d - 0.13), F.w(x + s * 0.18, y - 0.121, d - 0.13),
                 F.w(x + s * 0.18, y - 0.121, d + 0.13), F.w(x + s * 0.02, y - 0.121, d + 0.13)],
                "ih_lamp_w" if on else "ih_lens_off", (0, -1, 0), lit=on)


def _sign(F, x0, x1, y0, y1, d, face, mat):
    """A sign on a wall facing -d (face "out"... "in") or ±x: 3 mm proud."""
    m = [mat, 0.0, 0.0, 1.0, 1.0] if isinstance(mat, str) else mat
    if face == "in":                     # on a south face, read from the south (-d)
        F.box(x0, x1, y0, y1, d - 0.003, d, {"in": m})
    elif face == "out":                  # read from the north
        F.box(x0, x1, y0, y1, d, d + 0.003, {"out": [m[0], m[3], m[2], m[1], m[4]]})
    elif face == "right":                # on the east gable, read from the east
        F.box(d, d + 0.003, y0, y1, x0, x1, {"right": [m[0], m[3], m[2], m[1], m[4]]})
    else:                                # read from the west
        F.box(d - 0.003, d, y0, y1, x0, x1, {"left": m})


def building_ops(ox=0.0, oz=0.0, lights=False, roll=0.0, crane=(9.0, -2.4, DECK + 5.6), roof=True):
    """Piece 5: the pump hall over the deck and the building's outside. Live: lights (the wall packs and the hall's
    lights, from dusk), roll (the roll-up door, 0 shut to 1 open), crane ((bridge x, trolley d, hook y))."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    X0, X1 = HALL_X
    D0, D1 = HALL_D
    SID, LIN, C = "ih_siding", "ih_liner", "ih_concrete"
    STL = "ih_frame_steel"
    # ---- grade beams closing the deck's overhangs at the ends, the curbs under the panels
    F.box(X0, 0.0, -0.6, DECK - SLAB, D0, D1, {"out": C, "in": C, "left": C, "right": None, "top": None, "bottom": None})
    F.box(X1 - 0.4, X1, -0.6, DECK - SLAB, D0, D1, {"out": C, "in": C, "right": C, "left": None, "top": None, "bottom": None})
    for a, b in ((X0, DOOR_S[0] - 0.05), (DOOR_S[1] + 0.05, X1)):
        F.box(a, b, DECK, DECK + 0.2, D0, D0 + 0.15, {"in": C, "out": C, "left": C, "right": C, "top": C, "bottom": None})
    for a, b in ((X0, DOOR_N[0] - 0.05), (DOOR_N[1] + 0.05, X1)):
        F.box(a, b, DECK, DECK + 0.2, D1 - 0.15, D1, {"in": C, "out": C, "left": C, "right": C, "top": C, "bottom": None})
    for a, b in ((D0 + 0.15, ROLLUP[0] - 0.12), (ROLLUP[1] + 0.12, D1 - 0.15)):
        F.box(X1 - 0.15, X1, DECK, DECK + 0.2, a, b, {"in": C, "out": C, "left": C, "right": C, "top": C, "bottom": None})
    # ---- wall panels: south and north (along x), east gable, west gable over the electrical room
    WIN = [(f0 + 0.35, f1 - 0.35, DECK + 6.8, DECK + 7.6) for f0, f1 in zip(FRAMES, FRAMES[1:])]
    LOUV = [(2.0, 3.4, DECK + 1.2, DECK + 2.4), (14.0, 15.4, DECK + 1.2, DECK + 2.4)]
    s_holes = [(DOOR_S[0] - 0.05, DOOR_S[1] + 0.05, DECK, DECK + 2.18)] + WIN
    n_holes = [(DOOR_N[0] - 0.05, DOOR_N[1] + 0.05, DECK, DECK + 2.18)] + WIN + LOUV
    _panels(F, "x", D0, D0 + 0.1, X0, X1, DECK + 0.2, EAVE, s_holes, "in", {"in": SID, "out": LIN, "left": SID, "right": SID,
            "top": SID, "bottom": SID})
    _panels(F, "x", D1 - 0.1, D1, X0, X1, DECK + 0.2, EAVE, n_holes, "out", {"out": SID, "in": LIN, "left": SID, "right": SID,
            "top": SID, "bottom": SID})
    # (the south panels' openings at the door are filled from the curb's gap: the door's frame closes them)
    _panels(F, "d", X1 - 0.1, X1, D0 + 0.1, D1 - 0.1, DECK + 0.2, EAVE, [(ROLLUP[0] - 0.12, ROLLUP[1] + 0.12, DECK, DECK + 4.12)],
            "right", {"right": SID, "left": LIN, "in": SID, "out": SID, "top": SID, "bottom": SID})
    yer = ER_CEIL + 0.25 + 0.6                                         # the electrical room's parapet top
    for d0_, d1_ in ((D0, D0 + 0.1), (D1 - 0.1, D1)):                  # the long walls' ends over its parapet
        F.box(ER_X[1] - 0.3, X0, yer, EAVE, d0_, d1_, {"in": SID if d0_ < 0 else LIN, "out": LIN if d0_ < 0 else SID,
              "left": SID, "top": SID, "bottom": SID, "right": None})
    _panels(F, "d", ER_X[1] - 0.3, ER_X[1] - 0.2, D0 + 0.1, D1 - 0.1, yer, EAVE, [], "left",
            {"left": SID, "right": LIN, "in": SID, "out": SID, "top": SID, "bottom": SID})
    for xa, xb, fa, fb in ((X1 - 0.1, X1, "right", "left"), (ER_X[1] - 0.3, ER_X[1] - 0.2, "left", "right")):
        g = [(D0 + 0.1, EAVE), (RIDGE_D, RIDGE - 0.02), (D1 - 0.1, EAVE), (D1 - 0.1, EAVE)]
        F.prism([(d, y) for d, y in g], xa, xb, SID)                     # the gables' triangles
        for xx, ff in ((xa, fb), (xb, fa)):
            P = [F.w(xx, EAVE, D0 + 0.1), F.w(xx, EAVE, D1 - 0.1), F.w(xx, RIDGE - 0.02, RIDGE_D), F.w(xx, RIDGE - 0.02, RIDGE_D)]
            F._quad(P, SID if ff in ("right",) or xx == ER_X[1] - 0.3 else LIN, (1 if xx == xb else -1, 0, 0))
    # corner trims and base trims
    F.box(X1 - 0.06, X1 + 0.004, DECK + 0.22, EAVE - 0.02, D0 - 0.004, D0 + 0.06, "ih_trim")
    F.box(X1 - 0.06, X1 + 0.004, DECK + 0.22, EAVE - 0.02, D1 - 0.06, D1 + 0.004, "ih_trim")
    # windows, louvres (with their bird screens and the dampers behind)
    for a0, a1, y0, y1 in WIN:
        _window(F, "x", D0 + 0.05, a0, a1, y0, y1, -1)
        _window(F, "x", D1 - 0.05, a0, a1, y0, y1, 1)
    for a0, a1, y0, y1 in LOUV:
        F.box(a0, a1, y0, y1, D1 - 0.12, D1 - 0.02, {"out": "ih_louvre_ext", "in": "ih_damper", "left": "ih_trim",
              "right": "ih_trim", "top": "ih_trim", "bottom": "ih_trim"})
        F.box(a0 + 0.3, a0 + 0.55, y1 - 0.35, y1 - 0.1, D1 - 0.35, D1 - 0.12, "ih_act")      # damper actuator
    # ---- the frames: columns (W14) with base plates on the deck, rafters to the ridge, girts, purlins, bracing
    for k, x in enumerate(FRAMES):
        cols = (D0 + 0.365, D1 - 0.365) if 0 < k < len(FRAMES) - 1 else \
            ((D0 + 0.365, -4.4, -2.6, D1 - 0.365) if k == 0 else (D0 + 0.365, -5.4, -0.2, D1 - 0.365))
        for dc in cols:
            yc = _roof_y(dc) - 0.95
            for s in (-1, 1):
                F.box(x - 0.15, x + 0.15, DECK + 0.03, yc, dc + s * 0.15 - 0.012, dc + s * 0.15 + 0.012, STL)
            F.box(x - 0.006, x + 0.006, DECK + 0.03, yc, dc - 0.138, dc + 0.138, STL)
            F.box(x - 0.22, x + 0.22, DECK, DECK + 0.03, dc - 0.22, dc + 0.22, STL)
            for a in (-0.16, 0.16):
                for b in (-0.17, 0.17):
                    F.cyl((x + a, DECK + 0.03, dc + b), (x + a, DECK + 0.07, dc + b), 0.016, GV, sides=6)
        for (da, db) in ((D0 + 0.2, RIDGE_D), (D1 - 0.2, RIDGE_D)):                     # rafters, under the purlins
            ya, yb = _roof_y(da) - 0.632, _roof_y(db) - 0.632
            db = db - 0.07 if da < db else db + 0.07                                    # (to the ridge's splice)
            yb = _roof_y(db) - 0.632
            F.bar((x, ya, da), (x, yb, db), 0.588, 0.012, STL, up="t")
            for s in (-0.3, 0.3):
                F.bar((x, ya + s, da), (x, yb + s, db), 0.024, 0.2, STL, up="t")
        F.box(x - 0.1, x + 0.1, RIDGE - 0.632 - 0.27, RIDGE - 0.632 + 0.25, RIDGE_D - 0.07, RIDGE_D + 0.07, STL)    # splice
    for y in (DECK + 1.6, DECK + 3.2, DECK + 4.8, DECK + 6.2, DECK + 7.75, DECK + 8.6):             # girts
        for d0_, d1_ in ((D0 + 0.1, D0 + 0.2), (D1 - 0.2, D1 - 0.1)):
            holes = s_holes if d0_ < 0 else n_holes
            segs = [(X0 + 0.15, X1 - 0.15)]
            for h in holes:
                if h[2] - 0.08 < y < h[3] + 0.08:
                    nxt = []
                    for a, c in segs:
                        if h[1] + 0.02 < a or h[0] - 0.02 > c:
                            nxt.append((a, c))
                        else:
                            nxt += [p for p in ((a, min(c, h[0] - 0.02)), (max(a, h[1] + 0.02), c)) if p[1] - p[0] > 0.05]
                    segs = nxt
            for a, b in segs:
                F.box(a, b, y - 0.1, y, d0_, d1_, STL)
    for k in range(1, 6):                                                                    # purlins
        for d in (RIDGE_D - k * 0.8, RIDGE_D + k * 0.8):
            if D0 + 0.3 < d < D1 - 0.3:
                yy = _roof_y(d) - 0.12
                F.box(X0 + 0.1, X1 - 0.1, yy - 0.2, yy, d - 0.03, d + 0.03, "ih_purlin")
    # ---- the roof: two standing-seam slopes, ridge cap, rake trims, gutters and downspouts, exhaust fans
    if roof:
        RX0 = ER_X[1] - 0.3 - 0.3                                       # the roof's west edge, over the west gable
        for side in (-1, 1):
            de = (D0 - 0.25) if side < 0 else (D1 + 0.3)
            ye = _roof_y(de)
            poly = [(de, ye - 0.12), (RIDGE_D, RIDGE - 0.12), (RIDGE_D, RIDGE), (de, ye)]
            F.prism(poly, RX0, X1 + 0.3, "ih_roof_ss")
            F.box(RX0, X1 + 0.3, ye - 0.3, ye - 0.08, *sorted((de, de + side * 0.18)), {"in": "ih_gutter",
                  "out": "ih_gutter", "left": "ih_gutter", "right": "ih_gutter", "top": None, "bottom": "ih_gutter"})
        F.box(RX0 - 0.02, X1 + 0.32, RIDGE - 0.02, RIDGE + 0.06, RIDGE_D - 0.2, RIDGE_D + 0.2, "ih_trim")      # ridge cap
        for x in (4.7, 13.1):                                                                    # south downspouts
            de = D0 - 0.25 - 0.09
            F.cyl((x, _roof_y(D0 - 0.25) - 0.3, de), (x, EAVE - 0.6, D0 - 0.06), 0.05, "ih_gutter", sides=8)
            F.box(x - 0.05, x + 0.05, 0.25, EAVE - 0.6, D0 - 0.11, D0 - 0.01, "ih_gutter")
            F.box(x - 0.05, x + 0.05, 0.1, 0.25, D0 - 0.4, D0 - 0.01, "ih_gutter")
            F.box(x - 0.15, x + 0.15, 0.0, 0.06, D0 - 0.9, D0 - 0.3, C)                          # splash block
        dn = D1 + 0.3 + 0.09                                                                      # north: to the east end
        F.cyl((X1 + 0.15, _roof_y(D1 + 0.3) - 0.3, dn), (X1 + 0.06, EAVE - 0.6, D1 - 0.4), 0.05, "ih_gutter", sides=8)
        F.box(X1 + 0.01, X1 + 0.11, 0.25, EAVE - 0.6, D1 - 0.45, D1 - 0.35, "ih_gutter")
        F.box(X1 + 0.01, X1 + 0.4, 0.1, 0.25, D1 - 0.45, D1 - 0.35, "ih_gutter")
        F.box(X1 + 0.3, X1 + 0.9, 0.0, 0.06, D1 - 0.55, D1 - 0.25, C)
        for x in (2.6, 9.0, 15.4):                                                                # upblast fans
            d = -1.4
            yr = _roof_y(d)
            F.box(x - 0.55, x + 0.55, yr - 0.15, yr + 0.35, d - 0.55, d + 0.55, "ih_trim")
            F.cyl((x, yr + 0.35, d), (x, yr + 0.45, d), 0.62, "ih_fan", sides=24)
            F.cyl((x, yr + 0.45, d), (x, yr + 1.05, d), 0.5, "ih_fan", sides=24)
            F.cyl((x, yr + 1.05, d), (x, yr + 1.12, d), 0.56, "ih_fan", "ih_fan_top", sides=24)
    # ---- doors: the south personnel door (out, with canopy), the north door (in, onto the deck), the roll-up door
    _hall_door(F, DOOR_S, D0, -1, out=True, lights=lights, name="hall_s")
    _hall_door(F, DOOR_N, D1, 1, out=False, lights=lights, name="hall_n")
    _rollup(F, X1, ROLLUP, roll, lights)
    # ---- the line penetrations in the south wall: sleeve, link seal, flashing collar
    for xp, rp in ((2.6, 0.305), (6.8, 0.305), (11.0, 0.305), (15.2, 0.305)):
        F.box(xp - rp - 0.12, xp + rp + 0.12, CY - rp - 0.12, CY + rp + 0.12, D0 - 0.006, D0, {"in": "ih_trim", "left": "ih_trim",
              "right": "ih_trim", "top": "ih_trim", "bottom": "ih_trim"})
        F.cyl((xp, CY, D0 + 0.12), (xp, CY, D0 - 0.03), rp + 0.05, GV, sides=28)
        F.cyl((xp, CY, D0 - 0.03), (xp, CY, D0 - 0.05), rp + 0.025, "ih_gasket", sides=28)
    # ---- the landing and stair down to grade at the south door, their rails
    lx0, lx1, ld = -0.15, 1.45, D0 - 1.2
    F.box(lx0, lx1, 0.0, DECK, ld, D0, {"top": "ih_broom", "in": C, "left": C, "right": C, "out": None, "bottom": None})
    for k in range(4):
        xr = lx0 - 0.28 * (k + 1)
        F.box(xr, lx0 - 0.28 * k, 0.0, DECK - 0.2 * (k + 1), ld, D0 - 0.1, {"top": "ih_broom", "in": C, "left": C,
              "right": None, "out": None, "bottom": None})
    CT._rails(F, [(lx0, ld + 0.06), (lx1 - 0.06, ld + 0.06), (lx1 - 0.06, D0 - 0.05)], DECK, mat="ih_rail_y")
    for s in (0.06,):                                                                           # the stair's rail
        F.cyl((lx0, DECK + 1.07, ld + s), (lx0 - 1.4, 1.07, ld + s), 0.024, "ih_rail_y", sides=8)
        F.cyl((lx0, DECK + 0.53, ld + s), (lx0 - 1.4, 0.53, ld + s), 0.02, "ih_rail_y", sides=8)
        F.cyl((lx0 - 1.4, 0.0, ld + s), (lx0 - 1.4, 1.07, ld + s), 0.024, "ih_rail_y", sides=8)
        F.cyl((lx0 - 0.7, DECK - 0.6, ld + s), (lx0 - 0.7, DECK - 0.6 + 1.07 + 0.0, ld + s), 0.024, "ih_rail_y", sides=8)
    # ---- the truck dock at the roll-up door: apron, bumpers, bollards, dock light, its sign
    F.box(X1, X1 + 9.0, -0.15, 0.04, ROLLUP[0] - 1.0, ROLLUP[1] + 1.0, {"top": "ih_broom", "in": C, "out": C, "right": C,
          "left": None, "bottom": None})
    for d in (ROLLUP[0] + 0.4, ROLLUP[1] - 0.4):
        F.box(X1, X1 + 0.12, DECK - 0.45, DECK - 0.1, d - 0.15, d + 0.15, "ih_bumper")
        for k in range(3):
            F.box(X1 + 0.12, X1 + 0.13, DECK - 0.42 + 0.1 * k, DECK - 0.36 + 0.1 * k, d - 0.15, d + 0.15, "ih_bumper")
    for d in (ROLLUP[0] - 0.6, ROLLUP[1] + 0.6):
        F.cyl((X1 + 0.5, 0.04, d), (X1 + 0.5, 1.2, d), 0.11, "ih_bollard", "ih_bollard", sides=14)
        F.cyl((X1 + 0.5, 1.2, d), (X1 + 0.5, 1.26, d), 0.08, "ih_bollard", sides=14)
    _wallpack(F, X1, DECK + 4.6, (ROLLUP[0] + ROLLUP[1]) / 2, "+x", lights)
    _sign(F, ROLLUP[0] - 1.15, ROLLUP[0] - 0.25, DECK + 1.6, DECK + 2.2, X1, "right", "ih_sign_ohd")
    # ---- lights and signs outside
    _wallpack(F, X1, DECK + 6.0, D0 + 1.0, "+x", lights)
    _wallpack(F, X1, DECK + 6.0, D1 - 1.0, "+x", lights)
    _wallpack(F, 9.0, DECK + 6.2, D0, "-d", lights)
    _wallpack(F, 3.8, DECK + 6.2, D1, "+d", lights)
    _wallpack(F, 12.6, DECK + 6.2, D1, "+d", lights)
    _wallpack(F, ER_X[0], DECK + 3.4, -2.55, "-x", lights)
    _sign(F, 2.0, 4.6, DECK + 3.0, DECK + 3.8, D0 - 0.0, "in", "ih_sign_bldg")
    _sign(F, ER_X[0] + 2.0, ER_X[0] + 4.2, DECK + 2.2, DECK + 2.8, ER_D[0], "in", "ih_sign_elec")
    # ---- the electrical room's roof: drains (scuppers and downspouts), its ladder from grade, the hall's ladder
    yt = ER_CEIL + 0.25
    for d in (ER_D[0] + 1.5, ER_D[1] - 0.9):
        F.box(ER_X[0] - 0.12, ER_X[0], yt + 0.02, yt + 0.22, d - 0.15, d + 0.15, "ih_gutter")
        F.box(ER_X[0] - 0.25, ER_X[0] - 0.05, yt - 0.2, yt + 0.02, d - 0.13, d + 0.13, "ih_gutter")
        F.box(ER_X[0] - 0.2, ER_X[0] - 0.1, 0.25, yt - 0.2, d - 0.05, d + 0.05, "ih_gutter")
        F.box(ER_X[0] - 0.45, ER_X[0] - 0.1, 0.1, 0.25, d - 0.05, d + 0.05, "ih_gutter")
        F.box(ER_X[0] - 1.0, ER_X[0] - 0.4, 0.0, 0.06, d - 0.15, d + 0.15, C)
    _ladder_x(F, ER_X[0], -4.0, 0.0, yt + 0.6, out=-1)
    F.box(ER_X[0] - 0.08, ER_X[0], DECK + 2.5, DECK + 3.2, -0.9, 0.1, {"left": ["ih_hvac_out", 0, 0, 1, 1], "in": "ih_hvac_side",
          "out": "ih_hvac_side", "top": "ih_hvac_side", "bottom": "ih_hvac_side"})          # the air conditioner's grille
    _ladder_x(F, ER_X[1] - 0.3, -5.6, yt + 0.12, EAVE + 0.0, out=-1)
    # ---- inside: the crane, its runway and conductor bar; high-bay lights; unit heaters; the extinguisher
    _crane(F, *crane)
    for x in (2.2, 6.8, 11.0, 15.4):
        for d in (-4.6, -0.6):
            yy = DECK + 8.3
            F.cyl((x, yy, d), (x, _roof_y(d) - 0.32, d), 0.008, GV, sides=4)
            F.cyl((x, yy - 0.25, d), (x, yy, d), 0.28, "ih_fixture", sides=16)
            F.cyl((x, yy - 0.3, d), (x, yy - 0.25, d), 0.26, "ih_fixture", "ih_led", sides=16, lit=True)
    for x in (4.7, 13.1):
        F.box(x - 0.35, x + 0.35, DECK + 3.8, DECK + 4.35, D0 + 0.6, D0 + 1.1, {"out": "ih_heater", "in": "ih_heater_side",
              "left": "ih_heater_side", "right": "ih_heater_side", "top": "ih_heater_side", "bottom": "ih_heater_side"})
        F.box(x - 0.05, x + 0.05, DECK + 4.35, DECK + 4.5, D0 + 0.53, D0 + 0.8, STL)
    F.box(1.3, 1.6, DECK + 1.0, DECK + 1.6, D0 + 0.2, D0 + 0.38, {"out": "ih_ext_cab", "left": "ih_trim", "right": "ih_trim",
          "top": "ih_trim", "bottom": "ih_trim"})
    _sign(F, 1.25, 1.65, DECK + 1.75, DECK + 2.05, D0 + 0.2, "out", "ih_ext_sign")
    return o.ops()


def _hall_door(F, xs, d_face, out_sign, out, lights, name=None):
    """A hollow metal door in the hall's wall at d_face (out_sign: which way is outside): frame, leaf (its leaf at the
    side it opens to), hinges, lever handle, panic bar inside (the exit door), closer, kick plate; the canopy and wall
    pack outside, the signs."""
    x0, x1 = xs
    H = DECK + 2.13
    s = out_sign
    din, dout = d_face - s * 0.1, d_face
    lo, hi = sorted((din, dout))
    for a, b in ((x0 - 0.05, x0), (x1, x1 + 0.05)):
        F.box(a, b, DECK, H + 0.05, lo - 0.01, hi + 0.01, "ih_hmframe")
    F.box(x0, x1, H, H + 0.05, lo - 0.01, hi + 0.01, "ih_hmframe")
    leaf = (dout - s * 0.045, dout) if out else (din, din + s * 0.045)
    la, lb = sorted(leaf)
    fo = lb if s > 0 else la                                                     # its outside face
    fi = la if s > 0 else lb
    hd = fo if out else fi
    for y in (DECK + 0.3, DECK + 1.1, DECK + 1.9):
        F.cyl((x0 + 0.01, y - 0.06, hd), (x0 + 0.01, y + 0.06, hd), 0.012, "ct_galv", sides=8)
    # (in game the leaf and its hardware swing about the hinges while it stands open: name in OPEN)
    turn = (s if out else -s) * math.pi / 2 if name in OPEN else 0.0
    L = _Moved(F.sub(x0, hd, turn), x0, hd)
    L.box(x0 + 0.003, x1 - 0.003, DECK + 0.01, H, la, lb, "ih_hmdoor")
    L.box(x0 + 0.05, x1 - 0.05, DECK + 0.02, DECK + 0.27, *sorted((fo, fo + s * 0.003)), "ih_kick")
    L.cyl((x1 - 0.08, DECK + 1.0, fo), (x1 - 0.08, DECK + 1.0, fo + s * 0.06), 0.012, "ih_panic", sides=8)    # lever
    L.box(x1 - 0.2, x1 - 0.08, DECK + 0.99, DECK + 1.01, *sorted((fo + s * 0.05, fo + s * 0.07)), "ih_panic")
    if out:
        L.box(x0 + 0.12, x1 - 0.1, DECK + 0.95, DECK + 1.05, *sorted((fi, fi - s * 0.06)), "ih_panic")
        L.box(x0 + 0.1, x1 - 0.05, H - 0.12, H - 0.05, *sorted((fi, fi - s * 0.07)), "ih_closer")
    if name:
        _zone(F, "door:" + name, x0, x1, DECK, H, lo - 0.1, hi + 0.1)
    # canopy over it outside, its wall pack and the signs
    F.box(x0 - 0.35, x1 + 0.35, H + 0.45, H + 0.55, *sorted((dout, dout + s * 1.0)), {"top": "ih_trim", "bottom": "ih_soffit",
          "in": "ih_trim", "out": "ih_trim", "left": "ih_trim", "right": "ih_trim"})
    for xx in (x0 - 0.3, x1 + 0.3):
        F.bar((xx, H + 0.55, dout + s * 0.95), (xx, H + 1.4, dout), 0.02, 0.02, "ih_trim", up="t")
    _wallpack(F, (x0 + x1) / 2, H + 1.2, dout, "+d" if s > 0 else "-d", lights)
    face = "out" if s > 0 else "in"
    _sign(L, x0 + 0.15, x1 - 0.15, DECK + 1.3, DECK + 1.7, fo, face, "ih_sign_hearing")


def _rollup(F, x, ds, roll, lights):
    """The roll-up door in the east gable: its slat curtain (live: rolled up by roll), bottom bar, guides, the coil
    and its hood inside, the chain operator."""
    d0, d1 = ds
    Ht = DECK + 4.0
    yb = DECK + 4.0 * roll
    if Ht - yb > 0.02:
        F.box(x - 0.09, x - 0.05, yb + 0.06, Ht, d0, d1, {"right": "ih_rollup", "left": "ih_rollup", "in": GV, "out": GV,
              "top": None, "bottom": None})
        F.box(x - 0.1, x - 0.04, yb, yb + 0.06, d0 + 0.01, d1 - 0.01, "ih_trim")
    for d in (d0 - 0.05, d1 + 0.05):                                            # guides
        F.box(x - 0.14, x - 0.0, DECK, Ht, d - 0.05, d + 0.05, "ih_trim")
    F.box(x - 0.14, x - 0.0, Ht, Ht + 0.1, d0 - 0.05, d1 + 0.05, "ih_trim")
    F.cyl((x - 0.45, Ht + 0.38, d0 - 0.1), (x - 0.45, Ht + 0.38, d1 + 0.1), 0.24 + 0.18 * roll, "ih_rollup", "ih_trim", sides=20)
    F.box(x - 0.82, x - 0.1, Ht + 0.82, Ht + 0.86, d0 - 0.12, d1 + 0.12, "ih_trim")                 # hood
    F.box(x - 0.86, x - 0.82, Ht + 0.1, Ht + 0.86, d0 - 0.12, d1 + 0.12, "ih_trim")
    F.box(x - 0.6, x - 0.3, Ht + 0.2, Ht + 0.55, d1 + 0.12, d1 + 0.45, "ih_act")                    # operator
    F.cyl((x - 0.45, Ht + 0.38, d1 + 0.45), (x - 0.45, Ht + 0.38, d1 + 0.62), 0.1, "ih_mpaint", sides=12)
    for xx in (x - 0.35, x - 0.55):
        F.cyl((xx, Ht + 0.2, d1 + 0.3), (xx, DECK + 1.2, d1 + 0.3), 0.006, DS, sides=4)                # its chain
    F.box(x - 0.3, x - 0.16, DECK + 1.3, DECK + 1.55, d1 + 0.2, d1 + 0.28, "ih_btn_k")                 # its push-buttons
    _zone(F, "rollup", x - 0.32, x - 0.14, DECK + 1.28, DECK + 1.57, d1 + 0.18, d1 + 0.3)


def _ladder_x(F, x, d, y0, y1, out=-1):
    """A fixed ladder on a wall facing -x (out -1) or +x: side rails 0.45 m apart standing off 0.18 m, rungs at 0.3 m,
    the rails carried 1.07 m over the top, wall brackets."""
    xo = x + out * 0.18
    for s in (-0.225, 0.225):
        F.box(*sorted((xo - 0.01, xo + 0.01)), y0, y1 + 1.07, d + s - 0.03, d + s + 0.03, "ih_rail_y")
        for y in np.arange(y0 + 0.6, y1, 1.5):
            F.box(*sorted((x, xo)), y - 0.02, y + 0.02, d + s - 0.02, d + s + 0.02, "ih_rail_y")
    for y in np.arange(y0 + 0.3, y1 + 0.01, 0.3):
        F.cyl((xo, y, d - 0.225), (xo, y, d + 0.225), 0.014, "ih_rail_y", sides=6)


def _crane(F, bx, td, hy, X=None, R=None, RY=None, frames=None, cols=None, cap="ih_cap15", sign_r=(-3.4, -1.4),
           pend_r=-0.4, gd=0.7, gw=0.17, gs=0.55):
    """The 15 t top-running double-girder bridge crane: runway beams on the columns' brackets, rails, end stops, the
    conductor bar; the bridge (end trucks with wheels, drives and bumpers; box girders with walkway-free tops, the
    capacity signs); the trolley (frame, drum, gearbox, motor, brake), the hook block on its ropes (live), the pendant
    on its festoon, the warning horn and beacon."""
    STL, YE, DS, MO = "ih_frame_steel", "ih_crane", "ct_steel_dark", "ih_motor"
    X0, X1 = X or HALL_X
    RY = RY or RUNWAY_Y
    RUNWAY_D = R or globals()["RUNWAY_D"]
    frames = frames or FRAMES
    cols = cols or (HALL_D[0] + 0.365, HALL_D[1] - 0.365)
    for d in RUNWAY_D:
        s = -1 if d < 0 else 1
        F.box(X0 + 0.2, X1 - 0.3, RY - 0.6, RY - 0.58, d - 0.15, d + 0.15, STL)           # W24 runway beam
        F.box(X0 + 0.2, X1 - 0.3, RY - 0.02, RY, d - 0.15, d + 0.15, STL)
        F.box(X0 + 0.2, X1 - 0.3, RY - 0.58, RY - 0.02, d - 0.008, d + 0.008, STL)
        F.box(X0 + 0.2, X1 - 0.3, RY, RY + 0.08, d - 0.03, d + 0.03, "ct_galv")              # rail
        for x in frames:
            dc = cols[0] if d < RUNWAY_D[1] else cols[1]
            s = -1 if d < RUNWAY_D[1] else 1
            F.box(x - 0.14, x + 0.14, RY - 0.95, RY - 0.6, *sorted((dc + s * 0.15, d + s * 0.15)), STL)     # bracket
        for xe in (X0 + 0.2, X1 - 0.3):
            mid = (X0 + X1) / 2
            F.box(xe - 0.06 if xe > mid else xe, xe if xe > mid else xe + 0.06, RY + 0.08, RY + 0.35, d - 0.12, d + 0.12, "ih_bumper")
    dcb = RUNWAY_D[0] - 0.22                                                                  # conductor bar
    F.box(X0 + 0.3, X1 - 0.4, RY - 0.45, RY - 0.38, dcb - 0.03, dcb + 0.03, "ih_rail_y")
    # the bridge
    for d in RUNWAY_D:
        F.box(bx - 1.3, bx + 1.3, RY + 0.18, RY + 0.62, d - 0.17, d + 0.17, {"in": YE, "out": YE, "top": YE, "bottom": YE,
              "left": "ih_hazard", "right": "ih_hazard"})
        for xw in (bx - 1.0, bx + 1.0):
            F.cyl((xw, RY + 0.17, d - 0.06), (xw, RY + 0.17, d + 0.06), 0.16, "ih_wheel", sides=16)
        F.box(bx + 0.4, bx + 0.7, RY + 0.25, RY + 0.55, *sorted((d + (0.17 if d < 0 else -0.17), d + (0.42 if d < 0 else -0.42))),
              "ih_reducer")
        F.cyl((bx + 0.55, RY + 0.4, d + (0.42 if d < 0 else -0.42)), (bx + 0.55, RY + 0.4, d + (0.7 if d < 0 else -0.7)), 0.12, MO,
              sides=12)
        for xb in (bx - 1.3, bx + 1.3):
            F.cyl((xb, RY + 0.4, d), (xb + (-0.18 if xb < bx else 0.18), RY + 0.4, d), 0.09, "ct_rubber", sides=12)
        if d < 0:
            F.box(bx - 0.1, bx + 0.1, RY - 0.38, RY + 0.18, dcb - 0.06, dcb + 0.15, DS)            # collector arm
    gy0, gy1 = RY + 0.62, RY + 0.62 + gd
    for s in (-gs, gs):
        F.box(bx + s - gw, bx + s + gw, gy0, gy1, RUNWAY_D[0] - 0.12, RUNWAY_D[1] + 0.12, YE)
        for dd in np.arange(RUNWAY_D[0] + 0.5, RUNWAY_D[1] - 0.3, 0.7):
            F.box(*sorted((bx + s + (gw if s > 0 else -gw), bx + s + (gw + 0.02 if s > 0 else -gw - 0.02))), gy0 + 0.02, gy1 - 0.02,
                  dd - 0.01, dd + 0.01, YE)
        F.box(bx + s - 0.03, bx + s + 0.03, gy1, gy1 + 0.07, RUNWAY_D[0], RUNWAY_D[1], "ct_galv")     # trolley rail
        sx = bx + s + (gw + 0.02 if s > 0 else -gw - 0.02)
        F.box(*sorted((sx, sx + (0.004 if s > 0 else -0.004))), gy0 + 0.15, gy1 - 0.15, *sign_r,
              {"right" if s > 0 else "left": [cap, 0, 0, 1, 1]})
    # the trolley
    ty = gy1 + 0.07
    for s in (-gs, gs):
        for dd in (td - 0.45, td + 0.45):
            F.cyl((bx + s - 0.03, ty + 0.12, dd), (bx + s + 0.03, ty + 0.12, dd), 0.12, "ih_wheel", sides=14)
    F.box(bx - 0.8, bx + 0.8, ty + 0.12, ty + 0.27, td - 0.62, td + 0.62, YE)
    F.cyl((bx - 0.5, ty + 0.55, td - 0.15), (bx + 0.5, ty + 0.55, td - 0.15), 0.27, "ih_drum", sides=20)
    for s in (-0.55, 0.55):
        F.box(bx + s - 0.05, bx + s + 0.05, ty + 0.27, ty + 0.85, td - 0.3, td, YE)
    F.box(bx + 0.6, bx + 0.85, ty + 0.27, ty + 0.75, td - 0.1, td + 0.45, "ih_reducer")
    F.cyl((bx + 0.72, ty + 0.5, td + 0.45), (bx + 0.72, ty + 0.5, td + 0.62), 0.14, MO, sides=14)
    # the ropes and hook block (live height)
    for xr in (bx - 0.08, bx + 0.08):
        F.cyl((xr, ty + 0.28, td - 0.15 - 0.27), (xr, hy + 0.35, td - 0.15 - 0.27), 0.009, "ih_rope", sides=4)
    hd = td - 0.42
    F.box(bx - 0.2, bx + 0.2, hy, hy + 0.4, hd - 0.13, hd + 0.13, YE)
    F.cyl((bx, hy, hd), (bx, hy - 0.15, hd), 0.04, DS, sides=8)
    F.box(bx - 0.035, bx + 0.035, hy - 0.5, hy - 0.15, hd - 0.04, hd + 0.04, "ih_hook")
    F.box(bx - 0.035, bx + 0.17, hy - 0.56, hy - 0.5, hd - 0.04, hd + 0.04, "ih_hook")
    F.box(bx + 0.13, bx + 0.19, hy - 0.5, hy - 0.36, hd - 0.04, hd + 0.04, "ih_hook")
    # pendant on its festoon from the bridge; horn and beacon under the girder
    F.cyl((bx + 0.85, gy0, pend_r), (bx + 0.85, DECK + 1.4, pend_r), 0.008, "ct_rubber", sides=4)
    F.box(bx + 0.8, bx + 0.9, DECK + 1.05, DECK + 1.4, pend_r - 0.05, pend_r + 0.05, "ih_pendant")
    F.cyl((bx - 0.55, gy0, RUNWAY_D[1] - 0.6), (bx - 0.55, gy0 - 0.1, RUNWAY_D[1] - 0.6), 0.07, "ih_beacon", sides=10, lit=True)



def building_textures():
    """Piece 5's materials (128 px a metre where they repeat)."""
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    FR = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf"
    f = lambda n, path=FB: ImageFont.truetype(path, n)
    sd = _paint((206, 208, 204), 901, mottle=0.02, grain=0.006)             # insulated wall panel: ribs at 300 mm
    dsd = ImageDraw.Draw(sd)
    for k in range(0, 128, 38):
        dsd.rectangle([k, 0, k + 5, 127], fill=(180, 182, 178, 255))
        dsd.line([(k + 6, 0), (k + 6, 127)], fill=(226, 228, 224, 255))
        for x in (k + 14, k + 24):
            dsd.line([(x, 0), (x, 127)], fill=(198, 200, 196, 255))
    out["ih_siding"] = sd
    ln = _paint((236, 236, 232), 902, mottle=0.01, grain=0.004)
    dln = ImageDraw.Draw(ln)
    for k in range(0, 128, 19):
        dln.line([(k, 0), (k, 127)], fill=(214, 214, 210, 255))
    out["ih_liner"] = ln
    out["ih_trim"] = _paint((70, 72, 74), 903, n=64, mottle=0.015)
    out["ih_frame_steel"] = _paint((126, 132, 138), 904, n=64)
    out["ih_purlin"] = _paint((168, 172, 170), 905, n=64)
    rs = _paint((178, 182, 182), 906, mottle=0.02, grain=0.006)              # standing seam roof: seams at 400 mm
    drs = ImageDraw.Draw(rs)
    for k in range(0, 128, 51):
        drs.rectangle([0, k, 127, k + 3], fill=(212, 214, 214, 255))
        drs.line([(0, k + 4), (127, k + 4)], fill=(140, 144, 144, 255))
        drs.line([(k, 0), (k, 127)], fill=(180, 184, 184, 255))
    out["ih_roof_ss"] = rs
    out["ih_gutter"] = _paint((70, 72, 74), 907, n=32)
    cx = _paint((196, 184, 162), 908, mottle=0.03, grain=0.05)              # painted split-face block
    dcx = ImageDraw.Draw(cx)
    for k in range(5):
        y = int(k * 128 / 5)
        dcx.line([(0, y), (127, y)], fill=(160, 150, 130, 255), width=2)
        for xx in range(0, 128, 52):
            x = (xx + (26 if k % 2 else 0)) % 128
            dcx.line([(x, y), (x, y + 25)], fill=(160, 150, 130, 255), width=2)
    out["ih_cmu_ext"] = cx
    out["ih_coping"] = _paint((70, 72, 74), 909, n=32)
    out["ih_roof"] = _paint((70, 72, 76), 910, mottle=0.03, grain=0.02)     # the membrane roof
    lv = _paint((70, 72, 74), 911)
    dlv = ImageDraw.Draw(lv)
    for y in range(4, 128, 12):
        dlv.rectangle([0, y, 127, y + 6], fill=(36, 38, 40, 255))
        dlv.line([(0, y + 7), (127, y + 7)], fill=(110, 112, 114, 255))
    out["ih_louvre_ext"] = lv
    dp = _paint((150, 154, 156), 912)
    ddp = ImageDraw.Draw(dp)
    for y in range(0, 128, 25):
        ddp.rectangle([0, y, 127, y + 3], fill=(110, 114, 116, 255))
    out["ih_damper"] = dp
    out["ih_fan"] = _paint((150, 154, 156), 913, n=64)
    ft = _paint((150, 154, 156), 914, n=64)
    ImageDraw.Draw(ft).ellipse([10, 10, 53, 53], outline=(100, 104, 106, 255), width=3)
    out["ih_fan_top"] = ft
    out["ih_wallpack"] = _paint((52, 54, 58), 915, n=32)
    out["ih_lens_off"] = _paint((200, 200, 196), 916, n=16)
    out["ih_broom"] = _paint((182, 180, 172), 917, mottle=0.03, grain=0.03)
    out["ih_rail_y"] = _paint((232, 186, 24), 918, n=32)
    ru = _paint((176, 180, 182), 919)
    dru = ImageDraw.Draw(ru)
    for y in range(0, 128, 10):
        dru.line([(0, y), (127, y)], fill=(130, 134, 136, 255), width=2)
        dru.line([(0, y + 2), (127, y + 2)], fill=(206, 210, 212, 255))
    out["ih_rollup"] = ru
    out["ih_bumper"] = _paint((24, 24, 26), 920, n=32, grain=0.03)
    bl = Image.new("RGBA", (64, 64), (236, 190, 20, 255))
    dbl = ImageDraw.Draw(bl)
    for y in (12, 40):
        dbl.rectangle([0, y, 63, y + 10], fill=(24, 24, 24, 255))
    out["ih_bollard"] = bl
    out["ih_soffit"] = _paint((220, 220, 216), 921, n=32)
    hv = _paint((196, 198, 196), 922, n=64)
    dhv = ImageDraw.Draw(hv)
    for x in range(6, 64, 7):
        dhv.line([(x, 6), (x, 57)], fill=(60, 62, 64, 255), width=2)
    out["ih_heater"] = hv
    out["ih_heater_side"] = _paint((196, 198, 196), 923, n=32)
    ec = Image.new("RGBA", (64, 128), (196, 30, 26, 255))
    dec = ImageDraw.Draw(ec)
    dec.rectangle([8, 10, 55, 100], fill=(210, 220, 226, 255), outline=(120, 20, 18, 255), width=3)
    dec.rectangle([18, 30, 46, 94], fill=(190, 34, 30, 255))
    out["ih_ext_cab"] = ec
    ho = _paint((222, 222, 216), 924, n=128)
    dho = ImageDraw.Draw(ho)
    for y in range(8, 120, 7):
        dho.line([(6, y), (121, y)], fill=(120, 120, 116, 255), width=3)
    out["ih_hvac_out"] = ho
    cap = Image.new("RGBA", (512, 128), (232, 184, 22, 255))
    ImageDraw.Draw(cap).text((256, 64), "15 TON", font=f(96), fill=(20, 20, 20, 255), anchor="mm")
    out["ih_cap15"] = cap
    def sign(w, h, head, hcol, lines, hink=(255, 255, 255), bg=(250, 250, 246)):
        im = Image.new("RGBA", (w, h), bg + (255,))
        d = ImageDraw.Draw(im)
        if head:
            d.rectangle([0, 0, w - 1, int(h * 0.3)], fill=hcol + (255,))
            d.text((w / 2, h * 0.15), head, font=f(int(h * 0.2)), fill=hink + (255,), anchor="mm")
        top = 0.3 if head else 0.0
        for k, (line, size) in enumerate(lines):
            d.text((w / 2, h * (top + (1 - top) * (k + 0.5) / len(lines))), line, font=f(int(h * size)), fill=(12, 12, 12, 255),
                   anchor="mm")
        d.rectangle([1, 1, w - 2, h - 2], outline=(40, 40, 40, 255), width=3)
        return im
    out["ih_sign_ohd"] = sign(360, 240, "CAUTION", (250, 200, 0), [("OVERHEAD DOOR", 0.13), ("KEEP CLEAR", 0.13)], hink=(0, 0, 0))
    out["ih_sign_hearing"] = sign(300, 200, "NOTICE", (0, 92, 168), [("HEARING PROTECTION", 0.1), ("REQUIRED", 0.12),
                                                                      ("AUTHORIZED PERSONNEL ONLY", 0.075)])
    bs = Image.new("RGBA", (780, 240), (24, 52, 92, 255))                       # the building's name and number
    dbs = ImageDraw.Draw(bs)
    dbs.rectangle([6, 6, 773, 233], outline=(240, 240, 236, 255), width=4)
    dbs.text((390, 82), "INTAKE STRUCTURE", font=f(78), fill=(250, 250, 248, 255), anchor="mm")
    dbs.text((390, 176), "BLDG 41", font=f(64), fill=(250, 250, 248, 255), anchor="mm")
    out["ih_sign_bldg"] = bs
    es = bs.copy()
    des = ImageDraw.Draw(es)
    des.rectangle([10, 10, 769, 229], fill=(24, 52, 92, 255))
    des.text((390, 82), "INTAKE ELECTRICAL", font=f(70), fill=(250, 250, 248, 255), anchor="mm")
    des.text((390, 176), "BLDG 41A", font=f(64), fill=(250, 250, 248, 255), anchor="mm")
    out["ih_sign_elec"] = es
    return out


# ================================================================== piece 6: the circulating water pump house's bays, pumps,
# valves and lines (by the towers)
#
# Real-world basis: with cooling towers, the circulating water pumps stand by the towers (US practice): the towers'
# cold water comes back by gravity through buried box conduits into a covered forebay, then into four pump bays
# (each closed off by stop logs in slots at its mouth, for dewatering), where four 25 % vertical wet-pit mixed-flow
# pumps hang from the operating floor (bay widths, bell clearance and submergence after ANSI/HI 9.8). Each pump: a
# flanged column, two-stage bowl and suction bell near the floor; a fabricated discharge head with an 84 in nozzle and an
# open, guarded motor stand; a 3500 hp, 13.8 kV, 225 rpm vertical motor (oil-lubricated thrust bearing with its sight
# glass and cooling water, RTD and vibration boxes). Each discharge: a rubber expansion joint, an 84 in butterfly valve
# with a hydraulic cylinder operator and counterweight (it closes on a pump trip, from its accumulators), a combination
# air and vacuum valve, a pressure gauge and transmitter; then down through the floor to the buried lines, the two
# pumps of each side joining a 108 in line to the condenser. A hydraulic power unit serves the valves. The stop logs
# are stored on a rack on the floor with their lifting beam. Live: the forebay level, which pumps run, the valves'
# positions, a bay's logs in.

CW_END, CW_BAY, CW_PIER, CW_NB = 1.0, 4.8, 0.8, 4
CW_W = 2 * CW_END + CW_NB * CW_BAY + (CW_NB - 1) * CW_PIER
CW_FLOOR = -8.0
CW_D = {"north": -15.0, "fore": -14.2, "mouth": -6.0, "back": 1.8, "back1": 2.6, "end": 9.4}
CW_LOGS = (-5.6, -5.0)
CW_PD = -1.0                                    # the pumps' centre line
CW_CY = DECK + 1.9                              # the discharge nozzles' centre height
CW_CONDUITS = (CW_END + CW_BAY / 2, CW_W - CW_END - CW_BAY / 2)   # the towers' conduits' centres (west: CT-1A, east: CT-1B)
TAGS6 = ["CWP-1A", "CWP-1B", "CWP-1C", "CWP-1D", "CWV-1A", "CWV-1B", "CWV-1C", "CWV-1D", "CW HPU", "LT-CW1", "LT-CW2",
         "PT-CW1A", "PT-CW1B", "PT-CW1C", "PT-CW1D", "CW LINE A", "CW LINE B", "FROM CT-1A", "FROM CT-1B", "STOP LOGS",
         "LIFTING BEAM 5 T", "CONFINED SPACE"]


def tag6(i):
    c, r = i % 4, i // 4
    return ["ih_tags6", c / 4, r / 8, (c + 1) / 4, (r + 1) / 8]


def cw_bay_x(i):
    x0 = CW_END + i * (CW_BAY + CW_PIER)
    return x0, x0 + CW_BAY


def circ_ops(ox=0.0, oz=0.0, level=-1.0, running=(True,) * 4, valve_open=(True,) * 4, logs=None, cut=False):
    """Piece 6, its origin at the structure's north-west corner on the grass (x across to the east, d south toward the
    condenser). cut leaves the floor's slab off over the water (for looking in)."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    C, CW_, DK, GV, DS = "ih_concrete", "ih_concrete_wet", "ih_deck", "ct_galv", "ct_steel_dark"
    CA, PI, MP = "ih_cast", "ih_pipe", "ih_mpaint"
    W = CW_W
    Dn, Df, Dm, Db, Db1, De = (CW_D[k] for k in ("north", "fore", "mouth", "back", "back1", "end"))

    def conc(x0, x1, y0, y1, d0, d1, top=None):
        """Concrete, wet and stained below the water."""
        if y0 < level < y1:
            F.box(x0, x1, y0, level, d0, d1, {k: CW_ for k in ("out", "in", "left", "right", "bottom")})
            F.box(x0, x1, level, y1, d0, d1, {k: C for k in ("out", "in", "left", "right")} | {"top": top or C})
        else:
            m = CW_ if y1 <= level else C
            F.box(x0, x1, y0, y1, d0, d1, {k: m for k in ("out", "in", "left", "right", "bottom")} | {"top": top or m})

    # ---- the structure: base slab, outer walls, piers with rounded noses, the back wall; the conduits' mouths
    conc(-0.5, W + 0.5, CW_FLOOR - 0.8, CW_FLOOR, Dn - 0.5, Db1 + 0.5)
    conc(0.0, CW_END, CW_FLOOR, DECK - SLAB, Df, Db)                                 # west and east walls
    conc(W - CW_END, W, CW_FLOOR, DECK - SLAB, Df, Db)
    conc(0.0, W, CW_FLOOR, DECK - SLAB, Db, Db1)                                     # back wall
    xs_n = [0.0] + [v for c in CW_CONDUITS for v in (c - 1.2, c + 1.2)] + [W]       # north wall round the conduits
    for a, b in zip(xs_n[0::2], xs_n[1::2]):
        conc(a, b, CW_FLOOR, DECK - SLAB, Dn, Df)
    for c in CW_CONDUITS:
        conc(c - 1.2, c + 1.2, CW_FLOOR, -3.7, Dn, Df)
        conc(c - 1.2, c + 1.2, -1.3, DECK - SLAB, Dn, Df)
        # the conduit: its concrete box going away north, buried (seen in the cut), its manhole to the grass
        F.box(c - 1.6, c + 1.6, -4.1, -0.9, Dn - 14.0, Dn, {"left": C, "right": C, "bottom": C, "top": C, "out": None,
              "in": None})
        F.box(c - 0.75, c + 0.75, -0.9, 0.12, Dn - 8.75, Dn - 7.25, {k: C for k in ("left", "right", "out", "in")} | {"top": C})
        F.cyl((c, 0.12, Dn - 8.0), (c, 0.16, Dn - 8.0), 0.45, "ih_manhole", "ih_manhole", sides=24)
    for i in range(1, CW_NB):
        x0 = cw_bay_x(i - 1)[1]
        conc(x0, x0 + CW_PIER, CW_FLOOR, DECK - SLAB, Dm, Db)
        F.cyl((x0 + CW_PIER / 2, CW_FLOOR, Dm), (x0 + CW_PIER / 2, level, Dm), CW_PIER / 2, CW_, sides=16)
        F.cyl((x0 + CW_PIER / 2, level, Dm), (x0 + CW_PIER / 2, DECK - SLAB, Dm), CW_PIER / 2, C, sides=16)
    # stop-log guides in the piers' and walls' faces at the bays' mouths; the logs (live: a bay closed)
    for i in range(CW_NB):
        x0, x1 = cw_bay_x(i)
        for xs, s in ((x0, 1), (x1, -1)):
            F.box(*sorted((xs, xs + s * 0.1)), CW_FLOOR, DECK - SLAB, CW_LOGS[0] + 0.1, CW_LOGS[0] + 0.15, GV)
            F.box(*sorted((xs, xs + s * 0.1)), CW_FLOOR, DECK - SLAB, CW_LOGS[1] - 0.15, CW_LOGS[1] - 0.1, GV)
            F.box(*sorted((xs, xs + s * 0.03)), CW_FLOOR, DECK - SLAB, CW_LOGS[0] + 0.15, CW_LOGS[1] - 0.15,
                  {"left" if s < 0 else "right": DS})
        if logs == i:
            for k in range(int((DECK - SLAB - 0.4 - CW_FLOOR) / 0.4)):
                F.box(x0 + 0.04, x1 - 0.04, CW_FLOOR + 0.4 * k, CW_FLOOR + 0.4 * (k + 1) - 0.004, CW_LOGS[0] + 0.17,
                      CW_LOGS[1] - 0.17, "ct_alum")
    # the water: the forebay and the bays (live: its level; a bay behind its logs pumped down)
    for (x0, x1, d0, d1, y) in ([(CW_END, W - CW_END, Df, Dm, level)] +
                                [(*cw_bay_x(i), Dm, Db, level if logs != i else CW_FLOOR + 0.1) for i in range(CW_NB)]):
        F._quad([F.w(x0, y, d0), F.w(x1, y, d0), F.w(x1, y, d1), F.w(x0, y, d1)], "ct_water", (0, 1, 0), lit=True)
    # ---- the operating floor: the slab over the forebay and the bays, round the openings; on grade to the south
    if not cut:
        holes = []
        for i in range(CW_NB):
            x0, x1 = cw_bay_x(i)
            xc = (x0 + x1) / 2
            holes.append((xc - 1.7, xc + 1.7, CW_PD - 1.7, CW_PD + 1.7, "pump"))
            holes.append((x0, x1, CW_LOGS[0], CW_LOGS[1], "logs"))
        for c in CW_CONDUITS:
            holes.append((c - 0.6, c + 0.6, Df + 0.4, Df + 1.6, "hatch"))
        _slab_round(F, -0.5, W + 0.5, Dn - 0.5, Db1, holes)
        for x0, x1, d0, d1, kind in holes:
            if kind == "logs":
                F.box(x0 + 0.002, x1 - 0.002, DECK - 0.04, DECK - 0.002, d0 + 0.002, d1 - 0.002, "ct_diamond")
                for xx in (x0 + 0.5, x1 - 0.5):
                    F.cyl((xx - 0.05, DECK + 0.03, (d0 + d1) / 2), (xx + 0.05, DECK + 0.03, (d0 + d1) / 2), 0.015, GV, sides=6)
            elif kind == "hatch":
                _hatch(F, x0, x1, d0, d1)
        F.box(-0.5, W + 0.5, DECK - 0.5, DECK, Db1, De, {"top": DK, "out": C, "left": C, "right": C, "in": None,
              "bottom": None})                                                        # the floor on grade
        F.box(-0.5, W + 0.5, -0.8, DECK - 0.5, Db1, De, {"out": C, "left": C, "right": C, "in": None, "top": None,
              "bottom": None})
    # ---- the pumps
    for i in range(CW_NB):
        x0, x1 = cw_bay_x(i)
        _circ_pump(F, (x0 + x1) / 2, i, running[i], valve_open[i])
    # ---- the hydraulic power unit for the valves, on the floor east of the lines; its headers to the valves
    _hpu(F, (cw_bay_x(2)[0] + cw_bay_x(3)[1]) / 2, 7.9)
    # ---- the stop logs' rack on the floor (by the west wall) and their lifting beam
    _lograck(F, 0.6, Df + 2.2)
    # ---- the forebay's level transmitters on stilling wells by the hatches
    for k, c in enumerate(CW_CONDUITS):
        xl, dl = c + 1.2, Df + 1.0
        F.cyl((xl, -4.5, dl), (xl, DECK - SLAB, dl), 0.1, "ih_pvc", sides=12)
        F.cyl((xl, DECK, dl), (xl, DECK + 0.35, dl), 0.1, GV, sides=12)
        F.cyl((xl, DECK + 0.35, dl), (xl, DECK + 0.38, dl), 0.14, GV, sides=14)
        F.cyl((xl, DECK + 0.38, dl), (xl, DECK + 0.55, dl), 0.08, "ih_solenoid", sides=12)
        F.cyl((xl + 0.35, DECK, dl), (xl + 0.35, DECK + 0.7, dl), 0.03, GV, sides=8)
        F.box(xl + 0.25, xl + 0.45, DECK + 0.55, DECK + 0.75, dl + 0.03, dl + 0.13, {"out": ["ih_lt_face", 0, 0, 0.5, 1],
              "in": DS, "left": DS, "right": DS, "top": DS, "bottom": DS})
        F.box(xl + 0.25, xl + 0.45, DECK + 0.47, DECK + 0.52, dl + 0.13, dl + 0.132, {"out": tag6(9 + k)})
    return o.ops()


def _slab_round(F, x0, x1, d0, d1, holes):
    """The operating floor's slab (0.6 deep, broomed top) over x0..x1, d0..d1, less the rectangular holes: cut into
    strips across x at each hole's edges, then runs along d."""
    xs = sorted({x0, x1, *[h[0] for h in holes], *[h[1] for h in holes]})
    f = {"top": "ih_deck", "bottom": "ih_concrete", "out": "ih_concrete", "in": "ih_concrete", "left": "ih_concrete",
         "right": "ih_concrete"}
    for xa, xb in zip(xs, xs[1:]):
        hs = sorted((h[2], h[3]) for h in holes if h[0] <= xa and xb <= h[1])
        d = d0
        for h0, h1 in hs + [(d1, d1)]:
            if h0 - d > 1e-6:
                F.box(xa, xb, DECK - SLAB, DECK, d, h0, f)
            d = max(d, h1)


def _hatch(F, x0, x1, d0, d1):
    """An access hatch into the forebay: its frame, the aluminium double leaf (shut), the fixed ladder below, its
    confined space sign."""
    F.box(x0, x1, DECK, DECK + 0.08, d0 - 0.06, d0, "ct_galv")
    F.box(x0, x1, DECK, DECK + 0.08, d1, d1 + 0.06, "ct_galv")
    F.box(x0 - 0.06, x0, DECK, DECK + 0.08, d0 - 0.06, d1 + 0.06, "ct_galv")
    F.box(x1, x1 + 0.06, DECK, DECK + 0.08, d0 - 0.06, d1 + 0.06, "ct_galv")
    xm = (x0 + x1) / 2
    for a, b in ((x0, xm - 0.002), (xm + 0.002, x1)):
        F.box(a, b, DECK + 0.04, DECK + 0.07, d0, d1, {"top": "ct_diamond", "out": "ct_alum", "in": "ct_alum", "left": "ct_alum",
              "right": "ct_alum", "bottom": None})
        F.cyl((a + 0.1 if a < xm else b - 0.1, DECK + 0.07, d0 + 0.3), (a + 0.1 if a < xm else b - 0.1, DECK + 0.1, d0 + 0.3),
              0.03, "ct_galv", sides=8)                                                       # lift handle stubs
    for s in (-0.22, 0.22):
        F.box(xm + s - 0.01, xm + s + 0.01, -4.0, DECK - SLAB, d1 - 0.25, d1 - 0.19, "ct_galv")
    for y in np.arange(-3.8, DECK - SLAB, 0.3):
        F.cyl((xm - 0.21, y, d1 - 0.22), (xm + 0.21, y, d1 - 0.22), 0.014, "ct_galv", sides=6)


def _circ_pump(F, xc, i, run, vopen):
    """One 120,000 gpm circulating water pump with its motor, discharge, valve and line down into the ground."""
    GV, DS, CA, PI, MP = "ct_galv", "ct_steel_dark", "ih_cast", "ih_pipe", "ih_mpaint"
    pd = CW_PD
    rh, rm, rp, rf = 1.3, 1.45, 1.07, 1.3
    # below the floor: column (60 in) in flanged lengths, two bowls, the suction bell 1 m off the floor
    F.cyl((xc, CW_FLOOR + 3.2, pd), (xc, DECK - 0.002, pd), 0.76, PI, sides=32)
    for y in (CW_FLOOR + 3.2, -3.4, 0.0):
        F.cyl((xc, y, pd), (xc, y + 0.08, pd), 0.9, PI, sides=32)
    F.cyl((xc, CW_FLOOR + 1.6, pd), (xc, CW_FLOOR + 3.2, pd), 1.05, CA, sides=32)
    F.cyl((xc, CW_FLOOR + 2.4, pd), (xc, CW_FLOOR + 2.48, pd), 1.12, CA, sides=32)
    F.cyl((xc, CW_FLOOR + 1.25, pd), (xc, CW_FLOOR + 1.6, pd), 1.15, CA, sides=32)
    F.cyl((xc, CW_FLOOR + 1.0, pd), (xc, CW_FLOOR + 1.25, pd), 1.35, CA, sides=32)
    # grout, soleplate, head with gussets, the 84 in nozzle south; the motor stand, its guards and the coupling
    y0 = DECK + 0.12
    F.box(xc - 1.95, xc + 1.95, DECK, DECK + 0.05, pd - 1.95, pd + 1.95, "ih_grout")
    F.box(xc - 1.88, xc + 1.88, DECK + 0.05, y0, pd - 1.88, pd + 1.88, {"top": "ih_machined", "out": DS, "in": DS, "left": DS,
          "right": DS, "bottom": DS})
    for a in (-1.7, 1.7):
        for b in (-1.7, 0.0, 1.7):
            F.cyl((xc + a, y0, pd + b), (xc + a, y0 + 0.1, pd + b), 0.03, GV, sides=6)
            F.cyl((xc + a, y0, pd + b), (xc + a, y0 + 0.05, pd + b), 0.055, GV, sides=6)
    F.cyl((xc, y0, pd), (xc, y0 + 0.12, pd), rh + 0.25, CA, sides=40)
    for k in range(24):
        a = 2 * math.pi * (k + 0.5) / 24
        F.cyl((xc + (rh + 0.15) * math.cos(a), y0 + 0.12, pd + (rh + 0.15) * math.sin(a)),
              (xc + (rh + 0.15) * math.cos(a), y0 + 0.17, pd + (rh + 0.15) * math.sin(a)), 0.035, GV, sides=6)
    F.cyl((xc, y0 + 0.12, pd), (xc, DECK + 3.0, pd), rh, CA, sides=40)
    F.cyl((xc, DECK + 3.0, pd), (xc, DECK + 3.1, pd), rh + 0.1, CA, sides=40)
    for k in range(12):
        a = 2 * math.pi * (k + 0.5) / 12
        Fs = F.sub(xc, pd, a - math.pi / 2)
        Fs.prism([(rh - 0.02, y0 + 0.12), (rh + 0.22, y0 + 0.12), (rh + 0.22, y0 + 0.17), (rh - 0.02, y0 + 0.8)], -0.02, 0.02, CA)
    dn = pd + rh + 0.55
    F.cyl((xc, CW_CY, pd + rh - 0.2), (xc, CW_CY, dn), rp, CA, sides=40)
    for s in (-1, 1):
        F.prism([(pd + rh - 0.05, y0 + 0.12), (dn - 0.1, CW_CY - rp + 0.1), (dn - 0.1, CW_CY - rp + 0.05), (pd + rh - 0.05, y0 + 0.17)],
                xc + s * 0.4 - 0.025, xc + s * 0.4 + 0.025, CA)
    F.box(xc + rh - 0.03, xc + rh + 0.005, DECK + 2.2, DECK + 2.5, pd - 0.25, pd + 0.25, {"right": plate6(i)})
    F.cyl((xc, DECK + 3.1, pd), (xc, DECK + 3.3, pd), 0.3, CA, sides=20)                     # stuffing box
    F.cyl((xc, DECK + 3.3, pd), (xc, DECK + 4.3, pd), 0.13, "ih_shaft", sides=16)
    F.cyl((xc, DECK + 3.55, pd), (xc, DECK + 3.75, pd), 0.36, "ih_coupling", sides=24)
    F.cyl((xc, DECK + 3.75, pd), (xc, DECK + 3.95, pd), 0.36, "ih_coupling", sides=24)
    for k in range(8):
        a = 2 * math.pi * k / 8
        F.cyl((xc + 0.29 * math.cos(a), DECK + 3.52, pd + 0.29 * math.sin(a)),
              (xc + 0.29 * math.cos(a), DECK + 3.98, pd + 0.29 * math.sin(a)), 0.025, DS, sides=6)
    for k in range(6):
        a = math.pi / 6 + k * math.pi / 3
        Fs = F.sub(xc, pd, a - math.pi / 2)
        Fs.box(-0.12, 0.12, DECK + 3.1, DECK + 4.3, rh - 0.24, rh - 0.02, CA)
    F.cyl((xc, DECK + 3.12, pd), (xc, DECK + 4.28, pd), rh - 0.01, "ih_guard", sides=40)
    F.cyl((xc, DECK + 4.3, pd), (xc, DECK + 4.42, pd), rh + 0.1, CA, sides=40)
    # vibration probes on the stuffing box housing and their cables to the box
    for a in (0.0, math.pi / 2):
        F.cyl((xc + 0.3 * math.cos(a), DECK + 3.22, pd + 0.3 * math.sin(a)), (xc + 0.48 * math.cos(a), DECK + 3.22, pd + 0.48 * math.sin(a)),
              0.02, GV, sides=8)
    # ---- the motor: 3500 hp, ribbed frame, louvred band, top housing, the thrust bearing's oil and cooling water,
    # its terminal box (13.8 kV) and RTD box, lugs, nameplate, tag
    F.cyl((xc, DECK + 4.42, pd), (xc, DECK + 4.55, pd), rm - 0.05, MP, sides=48)
    F.cyl((xc, DECK + 4.55, pd), (xc, DECK + 6.2, pd), rm, MP, sides=48)
    _ring_ribs(F, xc, pd, rm, DECK + 4.65, DECK + 6.15, 40, skip=(math.pi / 2, 0.0), w=0.035, depth=0.07)
    F.cyl((xc, DECK + 6.2, pd), (xc, DECK + 6.75, pd), rm + 0.005, "ih_louvre", sides=48)
    F.cyl((xc, DECK + 6.75, pd), (xc, DECK + 6.85, pd), rm + 0.08, MP, sides=48)
    F.cyl((xc, DECK + 6.85, pd), (xc, DECK + 7.35, pd), rm * 0.72, MP, sides=40)
    F.cyl((xc, DECK + 7.35, pd), (xc, DECK + 7.5, pd), 0.45, MP, sides=24)
    for k in range(4):
        a = math.radians(20) + k * math.pi / 2
        Fs = F.sub(xc, pd, a - math.pi / 2)
        Fs.box(-0.05, 0.05, DECK + 6.4, DECK + 6.75, rm, rm + 0.14, MP)
        Fs.box(-0.06, 0.06, DECK + 7.0, DECK + 7.35, rm * 0.72 - 0.01, rm * 0.72 + 0.08, MP)
    sg_a = math.radians(225)                                                                   # oil sight glass
    gx_, gd_ = xc + rm * 0.72 * math.cos(sg_a), pd + rm * 0.72 * math.sin(sg_a)
    F.cyl((gx_, DECK + 7.05, gd_), (gx_ + 0.12 * math.cos(sg_a), DECK + 7.05, gd_ + 0.12 * math.sin(sg_a)), 0.07, "ih_btn_k", "ih_oilglass",
          sides=14)
    for s in (-1, 1):                                                                           # bearing cooling water
        a = sg_a + s * 0.25
        ca, sa = math.cos(a), math.sin(a)
        p0 = (xc + rm * 0.72 * ca, DECK + 6.95, pd + rm * 0.72 * sa)
        p1 = (xc + 1.62 * ca, DECK + 6.95, pd + 1.62 * sa)
        F.cyl(p0, p1, 0.025, "ct_brass", sides=8)
        F.cyl(p1, (p1[0], DECK + 0.05, p1[2]), 0.025, "ct_brass", sides=8)
        F.cyl((p1[0], DECK + 0.002, p1[2]), (p1[0], DECK + 0.05, p1[2]), 0.05, GV, sides=8)
        for y in (DECK + 1.0, DECK + 2.5, DECK + 4.0, DECK + 5.4):                              # its clips back to the frame
            r0 = rh + 0.1 if y < DECK + 4.4 else rm + 0.06
            Fs = F.sub(xc, pd, a - math.pi / 2)
            Fs.box(-0.02, 0.02, y - 0.03, y + 0.03, r0, 1.6, GV)
    tb0 = pd - rm + 0.03                                                                        # (on the north side)
    F.box(xc - 0.45, xc + 0.45, DECK + 4.7, DECK + 5.7, tb0 - 0.55, tb0, MP)
    F.box(xc - 0.42, xc + 0.42, DECK + 4.73, DECK + 5.67, tb0 - 0.57, tb0 - 0.55, {"in": MP, "left": MP, "right": MP, "top": MP,
          "bottom": MP})
    for a in (-0.38, 0.0, 0.38):
        for y in (DECK + 4.78, DECK + 5.2, DECK + 5.62):
            F.cyl((xc + a, y, tb0 - 0.57), (xc + a, y, tb0 - 0.59), 0.018, GV, sides=6)
    F.box(xc - 0.12, xc + 0.12, DECK + 5.45, DECK + 5.57, tb0 - 0.574, tb0 - 0.57, {"in": ["ih_danger_mv13", 0, 0, 1, 1]})
    F.cyl((xc, DECK + 4.7, tb0 - 0.3), (xc, DECK + 4.55, tb0 - 0.3), 0.12, GV, sides=12)
    for k in range(3):
        p = [(xc - 0.2 + 0.2 * k, DECK + 4.55, tb0 - 0.3), (xc - 0.2 + 0.2 * k, DECK + 3.9, tb0 - 0.45),
             (xc - 0.2 + 0.2 * k, DECK + 0.05, tb0 - 0.6)]
        for a, b in zip(p, p[1:]):
            F.cyl(a, b, 0.055, "ih_flex", sides=10)
        F.cyl((p[-1][0], DECK + 0.002, p[-1][2]), (p[-1][0], DECK + 0.08, p[-1][2]), 0.09, GV, sides=12)
    F.box(xc + 0.6, xc + 0.95, DECK + 4.9, DECK + 5.3, tb0 - 0.25, tb0 + 0.25, MP)          # RTD and vibration box
    _conduit(F, [(xc + 0.78, DECK + 4.9, tb0 - 0.1), (xc + 0.78, DECK + 4.4, tb0 - 0.1), (xc + 0.78, DECK + 4.4, tb0 - 0.7),
                 (xc + 0.78, DECK - 0.1, tb0 - 0.7)], 0.025)
    F.box(xc - 0.25, xc + 0.25, DECK + 5.85, DECK + 6.05, pd - rm - 0.008, pd - rm + 0.06, {"in": tag6(i)})
    F.box(xc + 0.6, xc + 1.1, DECK + 5.5, DECK + 5.85, tb0 - 0.04, tb0 + 0.06, {"in": plate6(4 + i)})
    # ---- the discharge: rubber expansion joint, the butterfly valve and its hydraulic operator, the air and vacuum
    # valve, gauge and transmitter; down through the floor (the buried 108 in lines below join the pairs)
    d = dn
    _flange(F, xc, CW_CY, d, d + 0.1, rf, CA, bolts=36)
    d += 0.1
    F.cyl((xc, CW_CY, d), (xc, CW_CY, d + 0.15), rp + 0.03, "ih_rubberjoint", sides=40)
    F.cyl((xc, CW_CY, d + 0.15), (xc, CW_CY, d + 0.45), rp + 0.12, "ih_rubberjoint", sides=40)
    F.cyl((xc, CW_CY, d + 0.45), (xc, CW_CY, d + 0.6), rp + 0.03, "ih_rubberjoint", sides=40)
    for k in range(6):
        a = math.pi / 6 + k * math.pi / 3
        u, v = (rf + 0.05) * math.cos(a), (rf + 0.05) * math.sin(a)
        F.cyl((xc + u, CW_CY + v, d - 0.12), (xc + u, CW_CY + v, d + 0.72), 0.025, GV, sides=6)                 # control rods
        for dd in (d - 0.12, d + 0.68):
            F.cyl((xc + u, CW_CY + v, dd), (xc + u, CW_CY + v, dd + 0.04), 0.045, GV, sides=6)
    d += 0.6
    _flange(F, xc, CW_CY, d, d + 0.1, rf, PI, bolts=36)
    d += 0.1
    _flange(F, xc, CW_CY, d, d + 0.1, rf, CA, bolts=36)
    d += 0.1
    F.cyl((xc, CW_CY, d), (xc, CW_CY, d + 0.45), rf + 0.04, CA, sides=40)                  # the 84 in butterfly valve
    dv = d + 0.225
    F.cyl((xc + rf, CW_CY, dv), (xc + rf + 0.35, CW_CY, dv), 0.16, CA, sides=16)           # shaft boss (east)
    F.cyl((xc - rf, CW_CY, dv), (xc - rf - 0.2, CW_CY, dv), 0.14, CA, sides=16)            # (west)
    F.cyl((xc + rf + 0.35, CW_CY, dv), (xc + rf + 0.45, CW_CY, dv), 0.08, DS, sides=12)
    # its hydraulic operator: the lever on the shaft, counterweight arm, the cylinder from a bracket on the floor
    a = math.radians(70 if vopen else 0)
    lx = xc + rf + 0.5
    tip = (CW_CY + 0.9 * math.sin(a), dv - 0.9 * math.cos(a))
    F.bar((lx, CW_CY, dv), (lx, tip[0], tip[1]), 0.18, 0.06, "ih_hyd", up="t")
    F.cyl((lx - 0.05, tip[0], tip[1]), (lx + 0.25, tip[0], tip[1]), 0.24, DS, sides=18)       # counterweight
    cb = (lx, DECK + 0.3, dv + 1.3)
    F.box(lx - 0.15, lx + 0.15, DECK, DECK + 0.4, dv + 1.1, dv + 1.5, "ih_hyd")
    F.cyl(cb, (lx, CW_CY + 0.45 * math.sin(a), dv - 0.45 * math.cos(a) + 0.0), 0.06, "ih_shaft", sides=12)    # rod
    mid = (cb[0], cb[1] + 0.55 * (CW_CY + 0.45 * math.sin(a) - cb[1]), cb[2] + 0.55 * (dv - 0.45 * math.cos(a) - cb[2]))
    F.cyl(cb, mid, 0.13, "ih_hyd", sides=16)                                                  # cylinder
    _flange(F, xc, CW_CY, d + 0.45, d + 0.55, rf, CA, bolts=36)
    _sign_plate(F, xc - rf - 0.2, CW_CY + 0.45, dv, tag6(4 + i))
    d += 0.55
    # spool, the air and vacuum valve, the gauge and transmitter, then the mitred bend down through the floor
    F.cyl((xc, CW_CY, d), (xc, CW_CY, d + 1.6), rp, PI, sides=40)
    _weld(F, (xc, CW_CY, d + 0.05), (xc, CW_CY, d + 0.07), rp)
    av = d + 0.6
    F.cyl((xc, CW_CY + rp - 0.05, av), (xc, CW_CY + rp + 0.2, av), 0.12, PI, sides=16)
    F.cyl((xc, CW_CY + rp + 0.2, av), (xc, CW_CY + rp + 0.26, av), 0.2, PI, sides=16)
    F.cyl((xc, CW_CY + rp + 0.26, av), (xc, CW_CY + rp + 0.62, av), 0.17, CA, sides=18)
    F.cyl((xc, CW_CY + rp + 0.62, av), (xc, CW_CY + rp + 0.7, av), 0.2, CA, sides=18)
    F.cyl((xc + 0.17, CW_CY + rp + 0.5, av), (xc + 0.4, CW_CY + rp + 0.5, av), 0.04, GV, sides=8)
    F.cyl((xc + 0.4, CW_CY + rp + 0.5, av), (xc + 0.4, DECK + 0.1, av), 0.04, GV, sides=8)
    F.cyl((xc + 0.4, DECK + 0.002, av), (xc + 0.4, DECK + 0.1, av), 0.08, DS, sides=10)
    gx = xc - rp - 0.25
    F.cyl((xc - rp + 0.05, CW_CY + 0.3, av), (gx, CW_CY + 0.3, av), 0.015, GV, sides=6)
    _ball_valve(F, gx, CW_CY + 0.45, av, "y", r=0.025)
    F.cyl((gx, CW_CY + 0.3, av), (gx, CW_CY + 0.42, av), 0.015, GV, sides=6)
    F.cyl((gx, CW_CY + 0.48, av), (gx, CW_CY + 0.62, av), 0.015, GV, sides=6)
    _gauge(F, gx, CW_CY + 0.71, av - 0.02, 26 if run else 1, 60, facing=-1, r=0.09, dial="ih_dial60")
    _pt(F, xc + 0.5, CW_CY + rp * 0.86, d + 1.15)
    _marker6(F, xc, CW_CY, d + 1.3, rp, 0 if i < 2 else 1)
    d += 1.6
    dvv, yv = _elbow_down(F, xc, d, CW_CY, 2.2, rp, PI)
    F.cyl((xc, yv, dvv), (xc, DECK - 0.5, dvv), rp, PI, sides=40)
    F.cyl((xc, DECK, dvv), (xc, DECK + 0.05, dvv), rp + 0.12, GV, sides=40)                 # floor sleeve's collar
    for ds_ in (d - 1.2,):
        _stand6(F, xc, ds_, CW_CY - rp)


def _sign_plate(F, x, y, d, mat):
    """An engraved tag on a small plate on the west side of a valve's boss, facing west."""
    F.box(x - 0.006, x, y - 0.06, y + 0.06, d - 0.25, d + 0.25, {"left": mat})


def _stand6(F, x, d, ytop):
    """A heavy saddle support under a large line: a concrete pedestal with a steel saddle."""
    F.box(x - 0.9, x + 0.9, DECK, ytop - 0.35, d - 0.3, d + 0.3, {k: "ih_concrete" for k in ("out", "in", "left", "right", "top")})
    F.box(x - 0.95, x + 0.95, ytop - 0.35, ytop - 0.32, d - 0.32, d + 0.32, "ct_steel_dark")
    for s in (-1, 1):
        F.prism([(d - 0.3, ytop - 0.32), (d + 0.3, ytop - 0.32), (d + 0.3, ytop + 0.25), (d - 0.3, ytop + 0.25)],
                x + s * 0.75 - 0.012, x + s * 0.75 + 0.012, "ct_steel_dark")


def _marker6(F, x, y, d, r, k):
    F.box(x - r - 0.004, x - r - 0.002, y - 0.1, y + 0.1, d - 0.6, d + 0.6, {"left": ["ih_marker6", 0, k / 2, 1, (k + 1) / 2]})


def plate6(i):
    c, r = i % 4, i // 4
    return ["ih_nameplates6", c / 4, r / 2, (c + 1) / 4, (r + 1) / 2]


def _hpu(F, x, d):
    """The valves' hydraulic power unit: reservoir on its skid, two motor-pump sets, filters, a bank of piston
    accumulators, the control panel; its pressure and return headers to the valves in a trench (covered)."""
    GV, DS = "ct_galv", "ct_steel_dark"
    F.box(x - 1.0, x + 1.0, DECK, DECK + 0.12, d - 0.7, d + 0.7, DS)                                      # skid
    F.box(x - 0.9, x + 0.9, DECK + 0.12, DECK + 0.9, d - 0.55, d + 0.55, {"in": "ih_hyd_tank", "out": "ih_hyd_tank",
          "left": "ih_hyd_tank", "right": "ih_hyd_tank", "top": "ih_hyd_tank", "bottom": None})
    F.cyl((x + 0.6, DECK + 0.9, d + 0.3), (x + 0.6, DECK + 0.98, d + 0.3), 0.06, DS, sides=10)           # filler/breather
    F.cyl((x - 0.9, DECK + 0.6, d - 0.3), (x - 0.92, DECK + 0.6, d - 0.3), 0.05, "ih_btn_k", "ih_oilglass", sides=12)
    for s in (-0.5, 0.0):
        F.cyl((x + s, DECK + 0.9, d - 0.2), (x + s, DECK + 1.1, d - 0.2), 0.11, "ih_valve", sides=12)    # pumps
        F.cyl((x + s, DECK + 1.1, d - 0.2), (x + s, DECK + 1.55, d - 0.2), 0.14, "ih_mpaint", sides=16)  # motors
        F.cyl((x + s, DECK + 1.55, d - 0.2), (x + s, DECK + 1.62, d - 0.2), 0.12, "ih_cowl", sides=16)
    for k in range(4):                                                                                    # accumulators
        ax = x - 0.75 + 0.25 * k
        F.cyl((ax, DECK + 0.9, d + 0.3), (ax, DECK + 2.1, d + 0.3), 0.1, "ih_hyd", sides=14)
        F.cyl((ax, DECK + 2.1, d + 0.3), (ax, DECK + 2.18, d + 0.3), 0.06, DS, sides=10)
    F.box(x + 0.35, x + 0.85, DECK + 0.9, DECK + 1.6, d - 0.55, d - 0.35, {"in": ["ih_hpu_panel", 0, 0, 1, 1], "out": "ih_gear",
          "left": "ih_gear", "right": "ih_gear", "top": "ih_gear", "bottom": "ih_gear"})
    F.box(x - 0.3, x + 0.3, DECK + 1.62, DECK + 1.7, d - 0.56, d - 0.554, {"in": tag6(8)})
    for s in (-0.25, 0.25):
        F.cyl((x - 1.0, DECK + 0.4, d + s), (x - 1.6, DECK + 0.4, d + s), 0.03, "ct_galv", sides=8)
        F.cyl((x - 1.6, DECK + 0.4, d + s), (x - 1.6, DECK - 0.2, d + s), 0.03, "ct_galv", sides=8)
        F.cyl((x - 1.6, DECK + 0.002, d + s), (x - 1.6, DECK + 0.03, d + s), 0.06, "ct_galv", sides=10)


def _lograck(F, x, d):
    """The stop logs' storage rack against the west wall: uprights and arms holding twenty aluminium logs, and the
    lifting beam on its stand beside it."""
    GV, DS = "ct_galv", "ct_steel_dark"
    L = CW_BAY - 0.08
    for dd in (d, d + 2.0, d + 4.0):
        F.box(x, x + 0.15, DECK, DECK + 2.2, dd - 0.08, dd + 0.08, "ih_rail_y")
        F.box(x - 0.2, x, DECK, DECK + 0.02, dd - 0.2, dd + 0.2, DS)
    for t in range(4):
        for n in range(5):
            y = DECK + 0.15 + 0.5 * t + 0.08 * n
            F.box(x + 0.15, x + 0.55, y, y + 0.075, d - 0.3, d + 4.3, "ct_alum")
        for dd in (d, d + 2.0, d + 4.0):
            F.box(x + 0.15, x + 0.62, DECK + 0.1 + 0.5 * t, DECK + 0.15 + 0.5 * t, dd - 0.06, dd + 0.06, "ih_rail_y")
    F.box(x + 0.62, x + 0.64, DECK + 1.8, DECK + 1.95, d + 1.6, d + 2.4, {"right": tag6(19)})
    # the lifting beam on its stand
    bx = x + 1.6
    for dd in (d + 0.5, d + 3.5):
        F.box(bx - 0.25, bx + 0.25, DECK, DECK + 0.6, dd - 0.1, dd + 0.1, DS)
    F.box(bx - 0.1, bx + 0.1, DECK + 0.6, DECK + 0.95, d, d + 4.0, "ih_crane")
    for dd in (d + 0.2, d + 3.8):
        F.box(bx - 0.04, bx + 0.04, DECK + 0.3, DECK + 0.6, dd - 0.05, dd + 0.05, DS)
    F.cyl((bx, DECK + 0.95, d + 2.0), (bx, DECK + 1.15, d + 2.0), 0.08, DS, sides=10)
    F.box(bx + 0.1, bx + 0.102, DECK + 0.7, DECK + 0.85, d + 1.6, d + 2.4, {"right": tag6(20)})



def circ_textures(running=(True,) * 4):
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
    f = lambda n, path=FB: ImageFont.truetype(path, n)
    sheet = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
    for i, text in enumerate(TAGS6):
        c, r = i % 4, i // 4
        cell = _paint((198, 202, 204), 1000 + i, n=256, mottle=0.01, grain=0.015).resize((256, 64))
        d = ImageDraw.Draw(cell)
        d.rectangle([0, 0, 255, 63], outline=(140, 144, 146, 255), width=2)
        for x in (14, 241):
            d.ellipse([x - 6, 26, x + 6, 38], fill=(160, 164, 166, 255), outline=(110, 112, 114, 255))
        size = 34
        while d.textlength(text, font=f(size)) > 206 and size > 14:
            size -= 1
        d.text((128, 33), text, font=f(size), fill=(36, 38, 40, 255), anchor="mm")
        sheet.paste(cell, (c * 256, r * 64))
    out["ih_tags6"] = sheet
    npl = Image.new("RGBA", (1024, 256), (0, 0, 0, 0))
    for i in range(8):
        c, r = i % 4, i // 4
        cell = _paint((196, 198, 196), 1100 + i, n=256, mottle=0.02, grain=0.02).resize((256, 128))
        d = ImageDraw.Draw(cell)
        d.rectangle([1, 1, 254, 126], outline=(120, 122, 122, 255), width=3)
        lines = (["VERTICAL MIXED FLOW PUMP", "120,000 GPM   90 FT TDH", "225 RPM   2 STAGE   84 IN", f"SERIAL 26-{8801 + i}  {TAGS6[i]}"]
                 if i < 4 else ["INDUCTION MOTOR  VSS  WP-II", "3500 HP   13,800 V   3 PH", "225 RPM   FLA 139 A   SF 1.15",
                                f"THRUST BRG OIL   {TAGS6[i - 4]}"])
        for k, line in enumerate(lines):
            d.text((128, 24 + 26 * k), line, font=f(14 if k else 15, FM), fill=(40, 42, 44, 255), anchor="mm")
        npl.paste(cell, (c * 256, r * 128))
    out["ih_nameplates6"] = npl
    im = Image.new("RGBA", (256, 256), (244, 244, 236, 255))
    di = ImageDraw.Draw(im)
    di.ellipse([3, 3, 252, 252], outline=(30, 30, 30, 255), width=5)
    for k in range(31):
        a = math.radians(225 - 270 * k / 30)
        r0 = 92 if k % 5 == 0 else 102
        di.line([(128 + r0 * math.cos(a), 128 - r0 * math.sin(a)), (128 + 114 * math.cos(a), 128 - 114 * math.sin(a))],
                fill=(20, 20, 20, 255), width=4 if k % 5 == 0 else 2)
        if k % 5 == 0:
            di.text((128 + 72 * math.cos(a), 128 - 72 * math.sin(a)), str(2 * k), font=f(22), fill=(20, 20, 20, 255), anchor="mm")
    di.text((128, 186), "PSI", font=f(24), fill=(20, 20, 20, 255), anchor="mm")
    out["ih_dial60"] = im
    mk = Image.new("RGBA", (1024, 256), (0, 120, 60, 255))
    dm = ImageDraw.Draw(mk)
    for r, text in enumerate(("CIRC WATER  LINE A", "CIRC WATER  LINE B")):
        y = 128 * r + 64
        dm.text((460, y), text, font=f(66), fill=(255, 255, 255, 255), anchor="mm")
        dm.rectangle([860, y - 14, 940, y + 14], fill=(255, 255, 255, 255))
        dm.polygon([(1000, y), (936, y - 40), (936, y + 40)], fill=(255, 255, 255, 255))
    out["ih_marker6"] = mk
    rj = _paint((30, 30, 32), 1200, mottle=0.02, grain=0.02)
    drj = ImageDraw.Draw(rj)
    for y in range(0, 128, 16):
        drj.line([(0, y), (127, y)], fill=(48, 48, 50, 255), width=2)
    out["ih_rubberjoint"] = rj
    out["ih_hyd"] = _paint((176, 40, 32), 1201, n=64)                   # the hydraulic operators, red
    out["ih_hyd_tank"] = _paint((150, 154, 156), 1202)
    hp = _paint((178, 182, 182), 1203, n=128)
    dhp = ImageDraw.Draw(hp)
    dhp.rectangle([3, 3, 124, 124], outline=(110, 114, 114, 255), width=3)
    dhp.rectangle([20, 18, 108, 50], fill=(150, 176, 120, 255))
    dhp.text((64, 34), "2850 PSI", font=f(16), fill=(20, 26, 16, 255), anchor="mm")
    for k, col in enumerate(((200, 40, 34), (40, 150, 70), (230, 170, 30))):
        dhp.ellipse([24 + 30 * k, 64, 44 + 30 * k, 84], fill=col + (255,))
    dhp.rectangle([30, 96, 98, 112], fill=(24, 24, 26, 255))
    dhp.text((64, 104), "CW HPU", font=f(11), fill=(240, 240, 236, 255), anchor="mm")
    out["ih_hpu_panel"] = hp
    mh = Image.new("RGBA", (128, 128), (60, 60, 62, 255))
    dmh = ImageDraw.Draw(mh)
    for k in range(-128, 256, 10):
        dmh.line([(k, 0), (k + 128, 128)], fill=(80, 80, 82, 255), width=3)
    dmh.ellipse([30, 30, 98, 98], fill=(64, 64, 66, 255))
    dmh.text((64, 64), "CW", font=f(22), fill=(96, 96, 98, 255), anchor="mm")
    out["ih_manhole"] = mh
    og = Image.new("RGBA", (32, 32), (60, 60, 64, 255))
    dog = ImageDraw.Draw(og)
    dog.ellipse([2, 2, 29, 29], fill=(220, 220, 214, 255))
    dog.rectangle([4, 16, 27, 28], fill=(190, 150, 40, 255))
    out["ih_oilglass"] = og
    dg = Image.new("RGBA", (256, 128), (250, 250, 246, 255))
    ddg = ImageDraw.Draw(dg)
    ddg.rectangle([0, 0, 255, 40], fill=(200, 16, 22, 255))
    ddg.text((128, 20), "DANGER", font=f(30), fill=(255, 255, 255, 255), anchor="mm")
    ddg.text((128, 84), "13,800 VOLTS", font=f(30), fill=(10, 10, 10, 255), anchor="mm")
    out["ih_danger_mv13"] = dg
    return out


# ================================================================== piece 7: the circulating water pump house's building,
# crane, electrical room and panel
#
# Real-world basis: the same kind of steel-framed hall as the intake's (piece 5), larger: its 50 t bridge crane must
# lift a 30 t motor clear of its stand and draw a pump's column out of its bay in sections, so the runway stands 13 m
# over the floor and the eaves 16.5 m. The ridge runs north-south with the crane; clerestory glass along both long walls;
# a 5 m roll-up door in the west wall for trucks, a personnel door with its landing and stair. The electrical room is an
# annex on the hall's east side (the intake's room turned about, from the same parts): its trains' 13.8 kV metal-clad
# switchgear (a main breaker from the plant's bus, two pump breakers and the transformer's feeder per train: each with
# its protective relay, ammeter, breaker control switch and lamps, lockout relay, test switches, the drawout breaker
# seen through its window), dry-type 13.8 kV-480 V transformers, 480 V MCCs (the hydraulic unit, the crane, the doors,
# fans, heaters, sump pump, lighting), and the circulating water local control panel.

CH_X = (-0.5, CW_W + 0.5)                                  # the hall's outside faces (west, east)
CH_D = (CW_D["north"] - 0.5, CW_D["end"])                  # (north, south)
CH_EAVE = DECK + 16.5
CH_RIDGE_X = (CH_X[0] + CH_X[1]) / 2
CH_RIDGE = CH_EAVE + (CH_X[1] - CH_X[0]) / 2 / 12
CH_FRAMES = [CH_D[0] + 0.26, -9.8, -4.4, 1.0, CH_D[1] - 0.26]
CH_RUNWAY_Y = DECK + 13.0
CH_ROLLUP = (2.6, 7.6)                                     # in the west wall (d)
CH_DOOR = (-12.6, -11.7)                                   # the personnel door in the west wall (d)
CH_ER = (23.7, -3.0)                                       # where the electrical room's turned frame stands (x, d)
ANN_CW = ["CWP-1A TRIP", "CWP-1B TRIP", "CWP-1C TRIP", "CWP-1D TRIP", "CWP-1A HI VIB", "CWP-1B HI VIB", "CWP-1C HI VIB",
          "CWP-1D HI VIB", "MTR BRG TEMP HI", "MTR WDG TEMP HI", "CWV FAIL TO CLOSE", "CWV FAIL TO OPEN", "HPU PRESS LO",
          "HPU OIL LVL LO", "FOREBAY LVL LO", "FOREBAY LO LO", "DISCH PRESS LO", "BRG CLG WTR LO", "SWGR CW-A UV",
          "SWGR CW-B UV", "86 LOCKOUT", "SUMP LVL HI", "FIRE ALARM", "DC GROUND"]


def ch_roof_y(x):
    return CH_RIDGE - abs(x - CH_RIDGE_X) / 12


def swgr_lineup(Fl, train, run2):
    """A 13.8 kV metal-clad switchgear lineup, four sections 0.914 m wide, 2.29 m tall, 1.0 deep: the main breaker
    from the plant's bus, two circulating water pump breakers, the transformer's feeder. Each section: the relay
    compartment's door (protective relay with its lit display, ammeter, breaker control switch TRIP - CLOSE with red,
    green and amber lamps, lockout relay 86, test switches, nameplate) over the breaker compartment's door (the drawout
    vacuum breaker seen through its window, racking port, T-handle, labels)."""
    W, Y0, H = 0.914, DECK + 0.1, 2.29
    L = 4 * W
    Fl.box(0.0, L, DECK, Y0, 0.04, 0.96, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})
    Fl.box(0.0, L, Y0, Y0 + H, 0.0, 1.0, {"in": None, "left": GEAR, "right": GEAR, "top": "ih_gear_top", "out": GEAR,
           "bottom": None})
    _fq(Fl, 0.0, L, Y0, Y0 + H, 0.0, "ih_gear_seam")
    for k in range(5):
        Fl.box(max(0.0, k * W - 0.018), min(L, k * W + 0.018), Y0, Y0 + H, -0.02, 0.0, GEAR)
    for k in range(4):
        for s in (0.12, W - 0.12):
            Fl.box(k * W + s - 0.04, k * W + s + 0.04, Y0 + H, Y0 + H + 0.08, 0.1, 0.106, GEAR)
        Fl.box(k * W + 0.2, k * W + W - 0.2, Y0 + H, Y0 + H + 0.06, 0.4, 0.75, {"in": "ih_louvre_g", "out": "ih_louvre_g",
               "left": GEAR, "right": GEAR, "top": GEAR})
    _plate(Fl, L / 2, Y0 + H - 0.05, 1.2, 0.07, 64 + train, r=-0.02)
    plates = [66 + train, 68 + 2 * train, 69 + 2 * train, 72 + train]
    for k in range(4):
        x0 = k * W
        closed = True if k in (0, 3) else bool(run2[k - 1])
        # the relay compartment
        _door(Fl, x0 + 0.03, x0 + W - 0.03, Y0 + 1.42, Y0 + 2.18)
        _plate(Fl, x0 + W / 2, Y0 + 2.12, 0.5, 0.045, plates[k], r=-0.015)
        Fl.box(x0 + 0.08, x0 + 0.3, Y0 + 1.72, Y0 + 2.04, -0.05, -0.015, "ih_btn_k")
        _fq(Fl, x0 + 0.095, x0 + 0.285, Y0 + 1.9, Y0 + 2.02, -0.052,
            ["ih_relay_cw", 0, (4 * train + k) / 8, 1, (4 * train + k + 1) / 8], lit=True)
        for j in range(4):
            _button(Fl, x0 + 0.11 + 0.05 * j, Y0 + 1.78, -0.05, "ih_btn_k", rad=0.01)
        if k == 0:
            _meter(Fl, x0 + 0.42, Y0 + 1.95, -0.015, 248 if any(run2) else 10, 400, "ih_mtr_a4")
        elif k == 3:
            _meter(Fl, x0 + 0.42, Y0 + 1.95, -0.015, 9, 50, "ih_mtr_a5")
        else:
            _meter(Fl, x0 + 0.42, Y0 + 1.95, -0.015, 124 if closed else 0, 200, "ih_mtr_a")
        _lamp(Fl, x0 + 0.6, Y0 + 2.0, -0.015, "r", closed)
        _lamp(Fl, x0 + 0.67, Y0 + 2.0, -0.015, "g", not closed)
        _lamp(Fl, x0 + 0.74, Y0 + 2.0, -0.015, "a", False)
        _pistol(Fl, x0 + 0.67, Y0 + 1.85, -0.015, 0, plate="ih_pistol_tc")
        _plate(Fl, x0 + 0.6, Y0 + 1.76, 0.07, 0.018, 96, r=-0.015)
        _plate(Fl, x0 + 0.74, Y0 + 1.76, 0.07, 0.018, 97, r=-0.015)
        Fl.cyl((x0 + 0.42, Y0 + 1.6, -0.015), (x0 + 0.42, Y0 + 1.6, -0.05), 0.04, "ih_btn_k", sides=14)      # lockout 86
        Fl.bar((x0 + 0.42, Y0 + 1.6, -0.055), (x0 + 0.47, Y0 + 1.6, -0.055), 0.02, 0.015, "ih_btn_k", up="r")
        _plate(Fl, x0 + 0.42, Y0 + 1.53, 0.11, 0.018, 98, r=-0.015)
        for j in range(6):                                                             # test switches
            Fl.box(x0 + 0.56 + 0.04 * j, x0 + 0.59 + 0.04 * j, Y0 + 1.5, Y0 + 1.62, -0.035, -0.015, "ih_btn_k")
            Fl.box(x0 + 0.57 + 0.04 * j, x0 + 0.58 + 0.04 * j, Y0 + 1.58, Y0 + 1.61, -0.045, -0.035, "ct_galv")
        # the breaker compartment
        _door(Fl, x0 + 0.03, x0 + W - 0.03, Y0 + 0.05, Y0 + 1.38)
        for a, b, c, d in ((x0 + 0.18, x0 + 0.62, Y0 + 0.62, Y0 + 0.64), (x0 + 0.18, x0 + 0.62, Y0 + 1.14, Y0 + 1.16),
                           (x0 + 0.18, x0 + 0.2, Y0 + 0.64, Y0 + 1.14), (x0 + 0.6, x0 + 0.62, Y0 + 0.64, Y0 + 1.14)):
            Fl.box(a, b, c, d, -0.027, -0.015, "ih_frame_bronze")
        _fq(Fl, x0 + 0.2, x0 + 0.6, Y0 + 0.64, Y0 + 1.14, -0.021, "ih_fglass")
        _fq(Fl, x0 + 0.2, x0 + 0.6, Y0 + 0.64, Y0 + 1.14, -0.0165, ["ih_vcb", 0 if closed else 0.5, 0, 0.5 if closed else 1.0, 1])
        Fl.cyl((x0 + 0.4, Y0 + 0.42, -0.015), (x0 + 0.4, Y0 + 0.42, -0.025), 0.035, "ih_gear_dk", sides=12)     # racking port
        Fl.cyl((x0 + W - 0.1, Y0 + 0.75, -0.015), (x0 + W - 0.1, Y0 + 0.75, -0.04), 0.015, "ih_gear_dk", sides=8)
        Fl.box(x0 + W - 0.14, x0 + W - 0.06, Y0 + 0.74, Y0 + 0.76, -0.05, -0.04, "ih_gear_dk")          # T-handle
        _label(Fl, x0 + 0.4, Y0 + 0.28, 0.22, 0.1, "ih_danger_mv13", r=-0.015)
        _label(Fl, x0 + 0.4, Y0 + 0.13, 0.2, 0.12, "ih_arcflash13", r=-0.015)
    return L


def cw_lcp(Fl, running, valve_open, ann_lit=()):
    """The circulating water local control panel, 3.0 m wide: forebay level and the motors' bearings (left door), the
    annunciator, the operator's screen and the alarm buttons (middle), the pumps' switches, lamps and ammeters, the
    valves' positions, discharge and hydraulic pressures (right)."""
    W, Y0, H = 3.0, DECK + 0.1, 2.29
    Fl.box(0.0, W, DECK, Y0, 0.04, 0.76, {"in": "ih_gear_dk", "left": "ih_gear_dk", "right": "ih_gear_dk", "top": None,
           "bottom": None})
    Fl.box(0.0, W, Y0, Y0 + H, 0.0, 0.8, {"in": None, "left": GEAR, "right": GEAR, "top": "ih_gear_top", "out": GEAR,
           "bottom": None})
    _fq(Fl, 0.0, W, Y0, Y0 + H, 0.0, "ih_gear_seam")
    for k in range(3):
        _door(Fl, k + 0.02, k + 0.98, Y0 + 0.05, Y0 + H - 0.2, hinge="left" if k < 2 else "right")
    _plate(Fl, W / 2, Y0 + H - 0.1, 1.8, 0.09, 90, r=-0.0)
    # left: forebay levels, bearings (thrust bearing temperature, vibration) per pump
    _plate(Fl, 0.5, Y0 + 1.95, 0.4, 0.04, 91, r=-0.015)
    for k in range(2):
        _digital(Fl, 0.3 + 0.4 * k, Y0 + 1.84, -0.015, k, w=0.19, h=0.07, sheet="ih_digits_cw")
        _label(Fl, 0.3 + 0.4 * k, Y0 + 1.78, 0.15, 0.02, tag6(9 + k), r=-0.015)
    _plate(Fl, 0.5, Y0 + 1.6, 0.4, 0.04, 94, r=-0.015)
    for p in range(4):
        t = 0.17 + 0.22 * p
        _digital(Fl, t, Y0 + 1.48, -0.015, 2 + p, w=0.17, h=0.07, sheet="ih_digits_cw")
        _digital(Fl, t, Y0 + 1.36, -0.015, 6 + p, w=0.17, h=0.07, sheet="ih_digits_cw")
        _label(Fl, t, Y0 + 1.29, 0.15, 0.02, tag6(p), r=-0.015)
    _label(Fl, 0.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    # middle: annunciator, the operator's screen, alarm buttons
    _plate(Fl, 1.5, Y0 + 1.95, 0.4, 0.04, 46, r=-0.015)
    Fl.box(1.08, 1.92, Y0 + 1.5, Y0 + 1.9, -0.07, -0.015, "ih_btn_k")
    for k, legend in enumerate(ANN_CW):
        c, r_ = k % 6, k // 6
        t0 = 1.1 + c * 0.1333
        y1 = Y0 + 1.88 - r_ * 0.095
        lit = legend in ann_lit
        _fq(Fl, t0 + 0.006, t0 + 0.127, y1 - 0.088, y1 - 0.004, -0.072,
            ["ih_anncw_on" if lit else "ih_anncw", (k % 4) / 4, (k // 4) / 8, (k % 4 + 1) / 4, (k // 4 + 1) / 8], lit=lit)
    Fl.box(1.15, 1.85, Y0 + 0.92, Y0 + 1.4, -0.05, -0.015, "ih_btn_k")
    _fq(Fl, 1.17, 1.83, Y0 + 0.94, Y0 + 1.38, -0.052, "ih_hmi_cw", lit=True)
    for k, (mat, pl) in enumerate((("ih_btn_k", 61), ("ih_btn_k", 62), ("ih_btn_w", 63), ("ih_btn_r", 57))):
        _button(Fl, 1.29 + 0.14 * k, Y0 + 0.8, -0.015, mat, rad=0.02)
        _plate(Fl, 1.29 + 0.14 * k, Y0 + 0.75, 0.1, 0.02, pl, r=-0.015)
        _zone(Fl, ("ann_ack", "ann_silence", "ann_test", "ann_reset")[k], 1.29 + 0.14 * k - 0.04, 1.29 + 0.14 * k + 0.04,
              Y0 + 0.76, Y0 + 0.84, -0.05, 0.0)
    _label(Fl, 1.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    # right: pumps, valves, pressures
    _plate(Fl, 2.5, Y0 + 1.95, 0.4, 0.04, 92, r=-0.015)
    for p in range(4):
        t = 2.14 + 0.24 * p
        run = running[p]
        _lamp(Fl, t - 0.04, Y0 + 1.84, -0.015, "r", run, rad=0.013)
        _lamp(Fl, t, Y0 + 1.84, -0.015, "g", not run, rad=0.013)
        _lamp(Fl, t + 0.04, Y0 + 1.84, -0.015, "a", False, rad=0.013)
        _pistol(Fl, t, Y0 + 1.7, -0.015, 0)
        _zone(Fl, f"cwp:{p}", t - 0.05, t + 0.05, Y0 + 1.64, Y0 + 1.76, -0.06, 0.0)
        _label(Fl, t, Y0 + 1.62, 0.15, 0.022, tag6(p), r=-0.015)
        _meter(Fl, t, Y0 + 1.5, -0.015, 124 if run else 0, 200, "ih_mtr_a", size=0.11)
    _plate(Fl, 2.5, Y0 + 1.36, 0.4, 0.04, 93, r=-0.015)
    for p in range(4):
        t = 2.14 + 0.24 * p
        op = valve_open[p]
        _lamp(Fl, t - 0.03, Y0 + 1.27, -0.015, "r", op, rad=0.013)
        _lamp(Fl, t + 0.03, Y0 + 1.27, -0.015, "g", not op, rad=0.013)
        _label(Fl, t, Y0 + 1.21, 0.15, 0.02, tag6(4 + p), r=-0.015)
    _plate(Fl, 2.5, Y0 + 1.1, 0.4, 0.04, 95, r=-0.015)
    for p in range(4):
        _digital(Fl, 2.14 + 0.24 * p, Y0 + 1.0, -0.015, 10 + p, w=0.17, h=0.07, sheet="ih_digits_cw")
    _digital(Fl, 2.5, Y0 + 0.86, -0.015, 14, w=0.19, h=0.07, sheet="ih_digits_cw")
    _label(Fl, 2.5, Y0 + 0.8, 0.15, 0.02, tag6(8), r=-0.015)
    _label(Fl, 2.5, Y0 + 0.5, 0.2, 0.14, "ih_arcflash_lv", r=-0.015)
    Fl.box(1.4, 1.6, Y0 + H, Y0 + H + 0.12, 0.2, 0.35, "ih_btn_k")
    Fl.cyl((1.5, Y0 + H + 0.06, 0.2), (1.5, Y0 + H + 0.06, 0.08), 0.06, "ih_btn_k", sides=14)
    return W


def circ_building_ops(ox=0.0, oz=0.0, lights=False, roll=0.0, crane=(-1.0, 6.0, DECK + 9.5), running=(True,) * 4,
                      valve_open=(True,) * 4, roof=True, ann_lit=()):
    """Piece 7: the circulating water pump house's hall, its 50 t crane, the electrical room annex with its gear and
    the local control panel. crane: (the bridge's d, the trolley's x, the hook's height), live."""
    o = CT.Ops()
    F = CT.Frame(o, math.pi)
    F.org = np.array([ox, 0.0, oz])
    X0, X1 = CH_X
    D0, D1 = CH_D
    SID, LIN, C, STL = "ih_siding", "ih_liner", "ih_concrete", "ih_frame_steel"
    xm = CH_RIDGE_X
    # ---- the electrical room annex on the east side (the intake's room, turned about and longer)
    Fer = F.sub(CH_ER[0], CH_ER[1], math.pi)
    elec_ops(running=running, frame=Fer, kind="circ", X=(-10.4, -0.4), valve_open=valve_open, roof=roof, ann_lit=ann_lit)
    er_d = sorted((CH_ER[1] + 6.6, CH_ER[1] - 1.5))                  # the annex's extent along d (world)
    yer = ER_CEIL + 0.25 + 0.6
    # ---- curbs and wall panels: west (roll-up and personnel doors), east (over the annex), north and south gables
    WIN_L = [(f0 + 0.4, f1 - 0.4, DECK + 14.0, DECK + 15.2) for f0, f1 in zip(CH_FRAMES, CH_FRAMES[1:])]
    w_holes = [(CH_ROLLUP[0] - 0.12, CH_ROLLUP[1] + 0.12, DECK, DECK + 6.12), (CH_DOOR[0] - 0.05, CH_DOOR[1] + 0.05, DECK, DECK + 2.18)] + WIN_L
    e_holes = [(er_d[0], er_d[1], DECK, yer)] + WIN_L
    fw = {"left": SID, "right": LIN, "in": SID, "out": SID, "top": SID, "bottom": SID}
    fe = {"right": SID, "left": LIN, "in": SID, "out": SID, "top": SID, "bottom": SID}
    for a, b in ((D0 + 0.15, CH_DOOR[0] - 0.05), (CH_DOOR[1] + 0.05, CH_ROLLUP[0] - 0.12), (CH_ROLLUP[1] + 0.12, D1 - 0.15)):
        F.box(X0, X0 + 0.15, DECK, DECK + 0.2, a, b, {"left": C, "right": C, "top": C, "in": C, "out": C, "bottom": None})
    for a, b in ((D0 + 0.15, er_d[0]), (er_d[1], D1 - 0.15)):
        F.box(X1 - 0.15, X1, DECK, DECK + 0.2, a, b, {"left": C, "right": C, "top": C, "in": C, "out": C, "bottom": None})
    _panels(F, "d", X0, X0 + 0.1, D0 + 0.1, D1 - 0.1, DECK + 0.2, CH_EAVE, w_holes, "left", fw)
    _panels(F, "d", X1 - 0.1, X1, D0 + 0.1, D1 - 0.1, DECK + 0.2, CH_EAVE, e_holes, "right", fe)
    for dd, f_out in ((D0, "out"), (D1, "in")):
        pd0, pd1 = (dd, dd + 0.1) if dd == D0 else (dd - 0.1, dd)
        F.box(X0, X1, DECK, DECK + 0.2, *(sorted((dd, dd + 0.15)) if dd == D0 else sorted((dd - 0.15, dd))),
              {"left": C, "right": C, "top": C, "in": C, "out": C, "bottom": None})
        holes = [(2.0, 4.6, DECK + 1.2, DECK + 2.6), (19.0, 21.6, DECK + 1.2, DECK + 2.6)] if dd == D0 else []
        _panels(F, "x", pd0, pd1, X0, X1, DECK + 0.2, CH_EAVE, holes, f_out,
                {"out": SID if dd == D1 else LIN, "in": SID if dd == D0 else LIN, "left": SID, "right": SID, "top": SID, "bottom": SID})
        for a0, a1, y0, y1 in holes:                                                  # intake louvres (north gable)
            F.box(a0, a1, y0, y1, pd0 + 0.02, pd1 + 0.02, {"in": "ih_louvre_ext", "out": "ih_damper", "left": "ih_trim",
                  "right": "ih_trim", "top": "ih_trim", "bottom": "ih_trim"})
        Fr = F.sub(0.0, 0.0, math.pi / 2)                                              # (t along d, r = -x)
        tri = [(-(X0 + 0.1), CH_EAVE), (-xm, CH_RIDGE - 0.15), (-(X1 - 0.1), CH_EAVE), (-(X1 - 0.1), CH_EAVE)]
        Fr.prism(tri, pd0, pd1, SID)
    for a0, a1, y0, y1 in WIN_L:
        _window(F, "d", X0 + 0.05, a0, a1, y0, y1, -1)
        _window(F, "d", X1 - 0.05, a0, a1, y0, y1, 1)
    # ---- frames: columns on both long walls, rafters to the ridge, splices, girts, purlins
    for k, d in enumerate(CH_FRAMES):
        for xc in (X0 + 0.365, X1 - 0.365):
            yc = ch_roof_y(xc) - 1.05
            for s in (-1, 1):
                F.box(xc + s * 0.18 - 0.016, xc + s * 0.18 + 0.016, DECK + 0.03, yc, d - 0.2, d + 0.2, STL)
            F.box(xc - 0.164, xc + 0.164, DECK + 0.03, yc, d - 0.008, d + 0.008, STL)
            F.box(xc - 0.28, xc + 0.28, DECK, DECK + 0.03, d - 0.28, d + 0.28, STL)
            for a in (-0.2, 0.2):
                for b in (-0.2, 0.2):
                    F.cyl((xc + a, DECK + 0.03, d + b), (xc + a, DECK + 0.08, d + b), 0.02, "ct_galv", sides=6)
        for xa in (X0 + 0.565, X1 - 0.565):
            xb = xm - 0.08 if xa < xm else xm + 0.08
            ya, yb = ch_roof_y(xa) - 0.75, ch_roof_y(xb) - 0.75
            F.bar((xa, ya, d), (xb, yb, d), 0.72, 0.016, STL, up="r")
            for s in (-0.37, 0.37):
                F.bar((xa, ya + s, d), (xb, yb + s, d), 0.03, 0.3, STL, up="r")
        F.box(xm - 0.08, xm + 0.08, CH_RIDGE - 0.75 - 0.34, CH_RIDGE - 0.75 + 0.32, d - 0.12, d + 0.12, STL)
    for y in (DECK + 2.0, DECK + 4.0, DECK + 6.4, DECK + 8.6, DECK + 10.8, DECK + 13.6, DECK + 15.5):
        for x0_, x1_, holes in ((X0 + 0.1, X0 + 0.2, w_holes), (X1 - 0.2, X1 - 0.1, e_holes)):
            segs = [(D0 + 0.15, D1 - 0.15)]
            for h in holes:
                if h[2] - 0.08 < y < h[3] + 0.08:
                    nxt = []
                    for a, c in segs:
                        if h[1] + 0.02 < a or h[0] - 0.02 > c:
                            nxt.append((a, c))
                        else:
                            nxt += [p for p in ((a, min(c, h[0] - 0.02)), (max(a, h[1] + 0.02), c)) if p[1] - p[0] > 0.05]
                    segs = nxt
            for a, c in segs:
                F.box(x0_, x1_, y - 0.12, y, a, c, STL)
    for k in range(1, 12):
        for x in (xm - k * 1.0, xm + k * 1.0):
            if X0 + 0.4 < x < X1 - 0.4:
                yy = ch_roof_y(x) - 0.13
                F.box(x - 0.035, x + 0.035, yy - 0.24, yy, D0 + 0.1, D1 - 0.1, "ih_purlin")
    # ---- the roof: two slopes (east and west), ridge cap, gutters with downspouts, exhaust fans
    if roof:
        Fr = F.sub(0.0, 0.0, math.pi / 2)
        for side in (-1, 1):
            xe = (X0 - 0.3) if side < 0 else (X1 + 0.3)
            ye = ch_roof_y(xe)
            poly = [(-xe, ye - 0.13), (-xm, CH_RIDGE - 0.13), (-xm, CH_RIDGE), (-xe, ye)]
            Fr.prism(poly, D0 - 0.3, D1 + 0.3, "ih_roof_ss2")
            F.box(*sorted((xe, xe + side * 0.2)), ye - 0.32, ye - 0.08, D0 - 0.3, D1 + 0.3, {"in": "ih_gutter", "out": "ih_gutter",
                  "left": "ih_gutter", "right": "ih_gutter", "top": None, "bottom": "ih_gutter"})
            for dd in (-13.2, -4.5, 8.6):
                xs = xe + side * 0.1
                xw = X0 - 0.06 if side < 0 else X1 + 0.06
                F.cyl((xs, ye - 0.32, dd), (xw, CH_EAVE - 0.6, dd), 0.06, "ih_gutter", sides=8)
                if side > 0 and er_d[0] - 0.2 < dd < er_d[1] + 0.2:
                    F.box(xw - 0.06, xw + 0.06, yer + 0.25, CH_EAVE - 0.6, dd - 0.06, dd + 0.06, "ih_gutter")
                    continue
                F.box(xw - 0.06, xw + 0.06, 0.3, CH_EAVE - 0.6, dd - 0.06, dd + 0.06, "ih_gutter")
                F.box(*sorted((xw, xw + side * 0.4)), 0.12, 0.3, dd - 0.06, dd + 0.06, "ih_gutter")
                F.box(*sorted((xw + side * 0.25, xw + side * 0.85)), 0.0, 0.06, dd - 0.15, dd + 0.15, C)
        F.box(xm - 0.25, xm + 0.25, CH_RIDGE - 0.02, CH_RIDGE + 0.07, D0 - 0.32, D1 + 0.32, "ih_trim")
        for dd in (-10.0, -2.5, 5.0):
            yr = ch_roof_y(xm + 1.6)
            F.box(xm + 1.0, xm + 2.2, yr - 0.15, yr + 0.4, dd - 0.6, dd + 0.6, "ih_trim")
            F.cyl((xm + 1.6, yr + 0.4, dd), (xm + 1.6, yr + 0.5, dd), 0.72, "ih_fan", sides=24)
            F.cyl((xm + 1.6, yr + 0.5, dd), (xm + 1.6, yr + 1.2, dd), 0.6, "ih_fan", sides=24)
            F.cyl((xm + 1.6, yr + 1.2, dd), (xm + 1.6, yr + 1.28, dd), 0.66, "ih_fan", "ih_fan_top", sides=24)
    # ---- the west wall's doors: personnel door with its landing and stair, the roll-up door with its dock
    _hall_door_w(F, CH_DOOR, X0, lights)
    lw0, lw1 = CH_DOOR[0] - 0.4, CH_DOOR[1] + 1.0
    F.box(X0 - 1.2, X0, 0.0, DECK, lw0, lw1, {"top": "ih_broom", "left": C, "in": C, "out": C, "right": None, "bottom": None})
    for k in range(4):
        dr = lw1 + 0.28 * (k + 1)
        F.box(X0 - 1.2, X0 - 0.1, 0.0, DECK - 0.2 * (k + 1), lw1 + 0.28 * k, dr, {"top": "ih_broom", "left": C, "in": C,
              "out": None, "right": None, "bottom": None})
    CT._rails(F, [(X0 - 0.05, lw0 + 0.06), (X0 - 1.14, lw0 + 0.06), (X0 - 1.14, lw1)], DECK, mat="ih_rail_y")
    F.cyl((X0 - 1.14, DECK + 1.07, lw1), (X0 - 1.14, 1.07, lw1 + 1.12), 0.024, "ih_rail_y", sides=8)
    F.cyl((X0 - 1.14, DECK + 0.53, lw1), (X0 - 1.14, 0.53, lw1 + 1.12), 0.02, "ih_rail_y", sides=8)
    F.cyl((X0 - 1.14, 0.0, lw1 + 1.12), (X0 - 1.14, 1.07, lw1 + 1.12), 0.024, "ih_rail_y", sides=8)
    _rollup_w(F, X0, CH_ROLLUP, roll, lights)
    F.box(X0 - 9.0, X0, -0.15, 0.04, CH_ROLLUP[0] - 1.0, CH_ROLLUP[1] + 1.0, {"top": "ih_broom", "in": C, "out": C, "left": C,
          "right": None, "bottom": None})
    for d in (CH_ROLLUP[0] + 0.5, CH_ROLLUP[1] - 0.5):
        F.box(X0 - 0.12, X0, DECK - 0.45, DECK - 0.1, d - 0.15, d + 0.15, "ih_bumper")
    for d in (CH_ROLLUP[0] - 0.6, CH_ROLLUP[1] + 0.6):
        F.cyl((X0 - 0.5, 0.04, d), (X0 - 0.5, 1.2, d), 0.11, "ih_bollard", "ih_bollard", sides=14)
    _sign(F, CH_ROLLUP[1] + 0.3, CH_ROLLUP[1] + 1.2, DECK + 1.6, DECK + 2.2, X0, "left", "ih_sign_ohd")
    _sign(F, -7.0, -2.6, DECK + 4.0, DECK + 5.4, X0, "left", "ih_sign_cwph")
    for d in (-14.0, -7.0, -0.5):
        _wallpack(F, X0, DECK + 7.0, d, "-x", lights)
    _wallpack(F, X0, DECK + 6.6, (CH_ROLLUP[0] + CH_ROLLUP[1]) / 2, "-x", lights)
    for x in (2.0, 21.6):
        _wallpack(F, x, DECK + 7.0, D1, "+d", lights)
        _wallpack(F, x + 1.5, DECK + 7.0, D0, "-d", lights)
    # ---- the annex's outside: its roof's scuppers and downspouts, its sign
    xo = CH_ER[0] + 10.4
    for d in (er_d[0] + 0.9, er_d[1] - 0.9):
        F.box(xo, xo + 0.12, ER_CEIL + 0.27, ER_CEIL + 0.47, d - 0.15, d + 0.15, "ih_gutter")
        F.box(xo + 0.05, xo + 0.25, ER_CEIL + 0.05, ER_CEIL + 0.27, d - 0.13, d + 0.13, "ih_gutter")
        F.box(xo + 0.1, xo + 0.2, 0.25, ER_CEIL + 0.05, d - 0.05, d + 0.05, "ih_gutter")
        F.box(xo + 0.1, xo + 0.45, 0.1, 0.25, d - 0.05, d + 0.05, "ih_gutter")
        F.box(xo + 0.4, xo + 1.0, 0.0, 0.06, d - 0.15, d + 0.15, C)
    _sign(F, er_d[0] + 2.4, er_d[0] + 5.0, DECK + 2.2, DECK + 2.9, xo, "right", "ih_sign_cwel")
    _wallpack(F, xo, DECK + 3.4, (er_d[0] + er_d[1]) / 2 + 2.0, "+x", lights)
    # ---- inside: the 50 t crane (runway along d on the columns' brackets), high bays, unit heaters
    Fc = F.sub(xm, (D0 + D1) / 2, math.pi / 2)            # t along d from the middle, r = -(x - xm)
    dm = (D0 + D1) / 2
    bt, bx_, hy = crane
    _crane(Fc, bt - dm, -(bx_ - xm), hy, X=(D0 - dm, D1 - dm), R=(-(X1 - 0.95 - xm), -(X0 + 0.95 - xm)), RY=CH_RUNWAY_Y,
           frames=[d - dm for d in CH_FRAMES], cols=(-(X1 - 0.365 - xm), -(X0 + 0.365 - xm)), cap="ih_cap50", sign_r=(-6.0, -2.0),
           pend_r=-1.5, gd=1.5, gw=0.3, gs=1.1)
    for x in (4.0, 11.8, 19.6):
        for d in (-11.0, -3.0, 5.0):
            yy = DECK + 15.0
            F.cyl((x, yy, d), (x, ch_roof_y(x) - 0.37, d), 0.008, "ct_galv", sides=4)
            F.cyl((x, yy - 0.28, d), (x, yy, d), 0.3, "ih_fixture", sides=16)
            F.cyl((x, yy - 0.33, d), (x, yy - 0.28, d), 0.28, "ih_fixture", "ih_led", sides=16, lit=True)
    for d in (-7.0, -0.2):
        F.box(X0 + 0.55, X0 + 1.05, DECK + 4.0, DECK + 4.6, d - 0.4, d + 0.4, {"right": "ih_heater", "in": "ih_heater_side",
              "out": "ih_heater_side", "left": "ih_heater_side", "top": "ih_heater_side", "bottom": "ih_heater_side"})
        F.box(X0 + 0.55, X0 + 0.75, DECK + 4.6, DECK + 4.75, d - 0.05, d + 0.05, STL)
    return o.ops()


def _hall_door_w(F, ds, x_face, lights, name="cw_door"):
    """The personnel door in the west wall (outswing, to the west): frame, leaf, hinges, lever, panic bar inside,
    closer, kick plate; the canopy, wall pack and notice outside. In game the leaf swings out about its hinges while
    it stands open (name in OPEN)."""
    d0, d1 = ds
    H = DECK + 2.13
    for a, b in ((d0 - 0.05, d0), (d1, d1 + 0.05)):
        F.box(x_face - 0.01, x_face + 0.11, DECK, H + 0.05, a, b, "ih_hmframe")
    F.box(x_face - 0.01, x_face + 0.11, H, H + 0.05, d0, d1, "ih_hmframe")
    for y in (DECK + 0.3, DECK + 1.1, DECK + 1.9):
        F.cyl((x_face, y - 0.06, d0 + 0.01), (x_face, y + 0.06, d0 + 0.01), 0.012, "ct_galv", sides=8)
    L = _Moved(F.sub(x_face, d0 + 0.01, math.pi / 2 if name in OPEN else 0.0), x_face, d0 + 0.01)
    L.box(x_face, x_face + 0.045, DECK + 0.01, H, d0 + 0.003, d1 - 0.003, "ih_hmdoor")
    L.box(x_face - 0.003, x_face, DECK + 0.02, DECK + 0.27, d0 + 0.05, d1 - 0.05, "ih_kick")
    L.cyl((x_face, DECK + 1.0, d1 - 0.08), (x_face - 0.06, DECK + 1.0, d1 - 0.08), 0.012, "ih_panic", sides=8)
    L.box(x_face - 0.07, x_face - 0.05, DECK + 0.99, DECK + 1.01, d1 - 0.2, d1 - 0.08, "ih_panic")
    L.box(x_face + 0.045, x_face + 0.105, DECK + 0.95, DECK + 1.05, d0 + 0.12, d1 - 0.1, "ih_panic")
    F.box(x_face + 0.045, x_face + 0.115, H - 0.12, H - 0.05, d0 + 0.1, d1 - 0.05, "ih_closer")
    _zone(F, "door:" + name, x_face - 0.1, x_face + 0.12, DECK, H, d0, d1)
    F.box(x_face - 1.0, x_face, H + 0.45, H + 0.55, d0 - 0.35, d1 + 0.35, {"top": "ih_trim", "bottom": "ih_soffit", "in": "ih_trim",
          "out": "ih_trim", "left": "ih_trim", "right": "ih_trim"})
    for dd in (d0 - 0.3, d1 + 0.3):
        F.bar((x_face - 0.95, H + 0.55, dd), (x_face, H + 1.4, dd), 0.02, 0.02, "ih_trim", up="r")
    _wallpack(F, x_face, H + 1.2, (d0 + d1) / 2, "-x", lights)
    _sign(F, d0 + 0.15, d1 - 0.15, DECK + 1.3, DECK + 1.7, x_face - 0.0, "left", "ih_sign_hearing")


def _rollup_w(F, x, ds, roll, lights):
    """The 5 m roll-up door in the west wall: curtain (live), bottom bar, guides, coil and hood inside, operator."""
    d0, d1 = ds
    Ht = DECK + 6.0
    yb = DECK + 6.0 * roll
    if Ht - yb > 0.02:
        F.box(x + 0.05, x + 0.09, yb + 0.06, Ht, d0, d1, {"left": "ih_rollup", "right": "ih_rollup", "in": "ct_galv",
              "out": "ct_galv", "top": None, "bottom": None})
        F.box(x + 0.04, x + 0.1, yb, yb + 0.06, d0 + 0.01, d1 - 0.01, "ih_trim")
    for d in (d0 - 0.05, d1 + 0.05):
        F.box(x, x + 0.14, DECK, Ht, d - 0.05, d + 0.05, "ih_trim")
    F.box(x, x + 0.14, Ht, Ht + 0.1, d0 - 0.1, d1 + 0.1, "ih_trim")
    F.cyl((x + 0.5, Ht + 0.45, d0 - 0.1), (x + 0.5, Ht + 0.45, d1 + 0.1), 0.3 + 0.2 * roll, "ih_rollup", "ih_trim", sides=20)
    F.box(x + 0.1, x + 0.95, Ht + 0.95, Ht + 1.0, d0 - 0.12, d1 + 0.12, "ih_trim")
    F.box(x + 0.95, x + 1.0, Ht + 0.1, Ht + 1.0, d0 - 0.12, d1 + 0.12, "ih_trim")
    F.box(x + 0.35, x + 0.7, Ht + 0.2, Ht + 0.6, d1 + 0.12, d1 + 0.5, "ih_act")
    F.cyl((x + 0.52, Ht + 0.4, d1 + 0.5), (x + 0.52, Ht + 0.4, d1 + 0.68), 0.1, "ih_mpaint", sides=12)
    F.box(x + 0.16, x + 0.3, DECK + 1.3, DECK + 1.55, d1 + 0.2, d1 + 0.28, "ih_btn_k")
    _zone(F, "rollup", x + 0.14, x + 0.32, DECK + 1.28, DECK + 1.57, d1 + 0.18, d1 + 0.3)


def circ_building_textures(running=(True,) * 4):
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    FR = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf"
    FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
    f = lambda n, path=FB: ImageFont.truetype(path, n)
    out["ih_roof_ss2"] = building_textures()["ih_roof_ss"]
    def lcd(w, h, lines, size, bg=(40, 90, 150), ink=(230, 240, 250)):
        im = Image.new("RGBA", (w, h), bg + (255,))
        d = ImageDraw.Draw(im)
        for k, line in enumerate(lines):
            d.text((10, 6 + k * (h - 10) / len(lines)), line, font=f(size, FM), fill=ink + (255,))
        return im
    rl = Image.new("RGBA", (256, 512), (0, 0, 0, 255))
    rows = []
    for t, pumps in enumerate((("CWP-1A", "CWP-1B"), ("CWP-1C", "CWP-1D"))):
        rows.append([f"52-{'AB'[t]}1 CLOSED", "13.8kV  248A"])
        for k, pn in enumerate(pumps):
            on = running[2 * t + k]
            rows.append([f"{pn} {'RUN' if on else 'STOP'}", f"Ia {124 if on else 0:>3} A"])
        rows.append([f"XF CW-{'AB'[t]} CLOSED", "Ia   9 A"])
    for k, lines in enumerate(rows):
        rl.paste(lcd(256, 64, lines, 21), (0, 64 * k))
    out["ih_relay_cw"] = rl
    pp = Image.new("RGBA", (128, 128), (40, 40, 42, 255))
    dpp = ImageDraw.Draw(pp)
    dpp.ellipse([4, 4, 123, 123], fill=(200, 202, 200, 255))
    for txt, ang in (("TRIP", 135), ("CLOSE", 45)):
        a = math.radians(ang)
        dpp.text((64 + 42 * math.cos(a), 64 - 42 * math.sin(a)), txt, font=f(16), fill=(10, 10, 10, 255), anchor="mm")
    out["ih_pistol_tc"] = pp
    vb = Image.new("RGBA", (256, 128), (36, 38, 42, 255))                   # the breaker's front: closed | open
    dvb = ImageDraw.Draw(vb)
    for k, (flag, col) in enumerate((("CLOSED", (200, 30, 26)), ("OPEN", (30, 140, 60)))):
        x = 128 * k
        dvb.rectangle([x + 10, 10, x + 117, 117], fill=(70, 74, 80, 255), outline=(20, 20, 22, 255))
        dvb.rectangle([x + 30, 20, x + 98, 42], fill=col + (255,))
        dvb.text((x + 64, 31), flag, font=f(14), fill=(255, 255, 255, 255), anchor="mm")
        dvb.rectangle([x + 30, 50, x + 98, 66], fill=(230, 190, 30, 255))
        dvb.text((x + 64, 58), "CHARGED", font=f(11), fill=(10, 10, 10, 255), anchor="mm")
        dvb.rectangle([x + 46, 76, x + 82, 90], fill=(20, 20, 20, 255))
        dvb.text((x + 64, 83), "0412", font=f(11, FM), fill=(240, 240, 240, 255), anchor="mm")
        for bx_, bc in ((44, (40, 150, 70)), (84, (200, 30, 26))):
            dvb.ellipse([x + bx_ - 9, 96, x + bx_ + 9, 112], fill=bc + (255,))
    out["ih_vcb"] = vb
    el = elec_textures()
    af = el["ih_arcflash"].copy()
    daf = ImageDraw.Draw(af)
    daf.rectangle([4, 70, 355, 245], fill=(250, 250, 246, 255))
    for k, (line, bold) in enumerate((("Arc Flash and Shock Hazard", True), ("Appropriate PPE Required", False),
                                      ("Incident energy at 36 in: 11.4 cal/cm2", False), ("Arc flash boundary: 78 in", False),
                                      ("Shock hazard: 13,800 VAC", True), ("Limited approach: 5 ft 0 in", False),
                                      ("PPE category 3", True))):
        daf.text((180, 252 * 0.36 + k * 252 * 0.64 / 7), line, font=f(int(252 * 0.085), FB if bold else FR), fill=(10, 10, 10, 255),
                 anchor="mm")
    out["ih_arcflash13"] = af
    vals = ["7.00", "7.00"] + ["142"] * 4 + ["1.8"] * 4 + [("26" if r else "0") for r in running] + ["2850", ""]
    dg = Image.new("RGBA", (1024, 512), (14, 6, 6, 255))
    ddg = ImageDraw.Draw(dg)
    for k, v in enumerate(vals):
        c, r = k % 4, k // 4
        ddg.text((c * 256 + 128, r * 64 + 32), v, font=f(46, FM), fill=(255, 52, 36, 255), anchor="mm")
    out["ih_digits_cw"] = dg
    for name, bg, ink in (("ih_anncw", (226, 226, 218), (40, 40, 40)), ("ih_anncw_on", (255, 236, 160), (20, 20, 20))):
        an = Image.new("RGBA", (512, 512), bg + (255,))
        dan = ImageDraw.Draw(an)
        for k, text in enumerate(ANN_CW):
            c, r = k % 4, k // 4
            x0, y0 = c * 128, r * 64
            dan.rectangle([x0, y0, x0 + 127, y0 + 63], outline=(150, 150, 146, 255), width=2)
            words = text.split(" ")
            l1 = " ".join(words[:len(words) // 2 + (len(words) % 2)])
            l2 = " ".join(words[len(words) // 2 + (len(words) % 2):])
            for yy, ln in ((21, l1), (44, l2)):
                size = 19
                while dan.textlength(ln, font=f(size)) > 118 and size > 10:
                    size -= 1
                dan.text((x0 + 64, y0 + yy), ln, font=f(size), fill=ink + (255,), anchor="mm")
        out[name] = an
    hm = Image.new("RGBA", (640, 420), (200, 202, 204, 255))
    dh = ImageDraw.Draw(hm)
    dh.rectangle([0, 0, 639, 30], fill=(60, 70, 84, 255))
    dh.text((12, 15), "CIRCULATING WATER", font=f(20), fill=(240, 240, 240, 255), anchor="lm")
    dh.text((628, 15), "19-OCT-26  14:32", font=f(16), fill=(240, 240, 240, 255), anchor="rm")
    dh.rectangle([30, 50, 610, 110], fill=(150, 176, 200, 255), outline=(90, 94, 98, 255), width=3)
    dh.text((320, 80), "FOREBAY  7.00 m   FROM CT-1A  /  CT-1B", font=f(16), fill=(20, 20, 20, 255), anchor="mm")
    for p in range(4):
        cx = 100 + p * 145
        col = (200, 40, 34) if running[p] else (40, 150, 70)
        dh.ellipse([cx - 26, 140, cx + 26, 192], fill=col + (255,), outline=(30, 30, 30, 255), width=2)
        dh.text((cx, 166), TAGS6[p][-2:], font=f(16), fill=(255, 255, 255, 255), anchor="mm")
        dh.line([(cx, 192), (cx, 240)], fill=(60, 60, 60, 255), width=4)
        dh.polygon([(cx - 14, 240), (cx + 14, 240), (cx, 262)], fill=(200, 40, 34, 255))
        dh.text((cx, 280), "124 A   26 psi", font=f(13), fill=(30, 30, 30, 255), anchor="mm")
    dh.rectangle([20, 320, 620, 410], fill=(184, 186, 188, 255), outline=(120, 120, 120, 255))
    for k, (lbl, v) in enumerate((("FLOW", "480,000 gpm"), ("LINE A", "26 psi"), ("LINE B", "26 psi"), ("HPU", "2850 psi"),
                                  ("CW IN", "84 F"), ("CW OUT", "106 F"))):
        x = 40 + (k % 3) * 200
        y = 342 + (k // 3) * 40
        dh.text((x, y), lbl, font=f(15), fill=(40, 40, 40, 255), anchor="lm")
        dh.text((x + 180, y), v, font=f(15), fill=(10, 10, 10, 255), anchor="rm")
    out["ih_hmi_cw"] = hm
    ol = Image.new("RGBA", (512, 384), (248, 248, 242, 255))
    dol = ImageDraw.Draw(ol)
    dol.rectangle([4, 4, 507, 379], outline=(20, 20, 20, 255), width=2)
    dol.text((256, 20), "CIRC WATER ELECTRICAL ONE-LINE", font=f(18), fill=(10, 10, 10, 255), anchor="mm")
    for c, x in enumerate((128, 384)):
        dol.line([(x, 40), (x, 90)], fill=(10, 10, 10, 255), width=3)
        dol.text((x + 6, 52), f"BUS 1{'CD'[c]} 13.8 KV", font=f(12), fill=(10, 10, 10, 255), anchor="lm")
        dol.line([(x - 100, 90), (x + 100, 90)], fill=(10, 10, 10, 255), width=4)
        for k, xx in enumerate((x - 70, x, x + 70)):
            dol.line([(xx, 90), (xx, 240)], fill=(10, 10, 10, 255), width=2)
            dol.rectangle([xx - 8, 140, xx + 8, 156], outline=(10, 10, 10, 255), width=2)
            dol.ellipse([xx - 14, 240, xx + 14, 268], outline=(10, 10, 10, 255), width=2)
            dol.text((xx, 254), "M" if k < 2 else "T", font=f(12), fill=(10, 10, 10, 255), anchor="mm")
        dol.line([(x + 70, 268), (x + 70, 320)], fill=(10, 10, 10, 255), width=2)
        dol.line([(x + 20, 320), (x + 120, 320)], fill=(10, 10, 10, 255), width=3)
        dol.text((x + 70, 334), f"MCC CW-{'AB'[c]} 480 V", font=f(12), fill=(10, 10, 10, 255), anchor="mm")
    dol.text((500, 370), "DWG E-1201 REV 2", font=f(10), fill=(10, 10, 10, 255), anchor="rm")
    out["ih_oneline_cw"] = ol
    bs = Image.new("RGBA", (780, 240), (24, 52, 92, 255))
    dbs = ImageDraw.Draw(bs)
    dbs.rectangle([6, 6, 773, 233], outline=(240, 240, 236, 255), width=4)
    dbs.text((390, 82), "CIRC WATER PUMP HOUSE", font=f(64), fill=(250, 250, 248, 255), anchor="mm")
    dbs.text((390, 176), "BLDG 42", font=f(64), fill=(250, 250, 248, 255), anchor="mm")
    out["ih_sign_cwph"] = bs
    cap = Image.new("RGBA", (512, 128), (232, 184, 22, 255))
    ImageDraw.Draw(cap).text((256, 64), "50 TON", font=f(96), fill=(20, 20, 20, 255), anchor="mm")
    out["ih_cap50"] = cap
    es = Image.new("RGBA", (780, 240), (24, 52, 92, 255))
    des = ImageDraw.Draw(es)
    des.rectangle([6, 6, 773, 233], outline=(240, 240, 236, 255), width=4)
    des.text((390, 82), "CW ELECTRICAL", font=f(72), fill=(250, 250, 248, 255), anchor="mm")
    des.text((390, 176), "BLDG 42A", font=f(64), fill=(250, 250, 248, 255), anchor="mm")
    out["ih_sign_cwel"] = es
    return out
