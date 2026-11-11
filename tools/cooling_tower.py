"""The natural-draft cooling towers' structure (Justin, 18 October 2026: the towers brought up to the relay-room
standard, "realism max"; shell option B: smooth curved shell blocks): the hyperbolic concrete shell, its cornice and
lintel ring beam, the basin curb, and the diagonal precast columns that carry the shell over the air inlet.

Real-world basis (written up in the project doc claude/jugcraft-cooling-tower-redesign.md): a US nuclear plant's
natural-draft tower (Vogtle, Byron, Beaver Valley class) is a thin reinforced-concrete hyperboloid of revolution cast
in jump-form lifts about 1 m high, 0.2 to 0.35 m thick over most of its height and thickening toward a ring beam
(lintel) at its foot; the lintel stands on pairs of diagonal precast columns (a continuous row of V's) footed in the
cold-water basin's wall, leaving the air inlet open; the top carries a stiffening ring (cornice) with a drip lip.
Bare concrete, rain streaks from the cornice, damp and algae low down where the rain zone splashes.

The shell is real blocks that drones place. Each block's model is the concrete that falls in it, as smooth quads
(the mesh model format client/MeshItemModels.java reads: "fabric:type" jugcraft:mesh, quads in model pixels with
per-corner normals), so the tower has no steps or seams. The surface is cut on one grid for the whole tower (every
cell 1/2 block along the ring and along the shell's meridian), and each cell belongs to the block holding its middle,
so neighbouring blocks' quads meet exactly. The columns lean at angles a block model cannot turn to, so they are drawn
by the controller's renderer (column_ops), with invisible collision blocks along them.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

CELL = 0.5            # the surface grid (blocks)
TEX = 128             # concrete texture size (px a block)


class Tower:
    """A natural-draft tower; heights in blocks from the grass (y 0), radii from its axis."""

    def __init__(self, key="ct_natural", base_r=52.0, throat_r=31.0, throat_y=115.0, top_y=149.2, lintel_y=10.0):
        self.key = key
        self.R0, self.RT, self.YT = base_r, throat_r, throat_y
        self.B = throat_y / math.sqrt((base_r / throat_r) ** 2 - 1)
        self.Y_LINTEL, self.Y_SHELL, self.Y_TOP = lintel_y, lintel_y + 1.6, top_y
        self.RTP = self.rad(top_y)
        self.TI = self.thick(top_y)
        self.R_IN = self.RTP - self.TI - 0.25       # the cornice's inner face (the rim walkway's parapet)
        self.Y_CTOP = top_y + 1.3                   # top of the cornice
        self.RL = self.rad(lintel_y)
        self.NPHI = int(round(2 * math.pi * base_r / CELL))   # cells round the ring (one grid for the whole tower)
        self.NB = 64                                # column pairs
        self.DOORS = []                             # shell doors: (first ring cell, cells across, sill y, head y)

    def rad(self, y):
        return self.RT * math.sqrt(1 + ((y - self.YT) / self.B) ** 2)

    def thick(self, y):
        return 0.35 + 0.55 * max(0.0, 1 - (y - self.Y_SHELL) / 30)

    # -------------------------------------------------------------- the concrete as surfaces of revolution

    def surfaces(self):
        """(name, polyline [(r, y)] drawn so its outside is on the left going along it, material)."""
        ys = list(np.arange(self.Y_SHELL, self.Y_TOP - 0.3, 0.5)) + [self.Y_TOP - 0.3]
        yl = self.Y_TOP - 0.3
        outer = [(self.rad(y), y) for y in ys] + [(self.rad(self.Y_TOP), self.Y_TOP)]
        inner = [(self.rad(y) - self.thick(y), y) for y in reversed(ys)]
        R, L = self.RTP, self.RL
        return [
            ("shell_out", outer, "shell"),
            ("cornice", [(R, self.Y_TOP), (R + 0.55, self.Y_TOP + 0.15), (R + 0.55, self.Y_CTOP), (self.R_IN, self.Y_CTOP),
                         (self.R_IN, yl), (self.rad(yl) - self.thick(yl), yl)], "cornice"),
            ("shell_in", inner, "inside"),
            ("lintel", [(self.rad(self.Y_SHELL) - self.thick(self.Y_SHELL), self.Y_SHELL), (L - 0.95, self.Y_SHELL),
                        (L - 0.95, self.Y_LINTEL), (L + 0.55, self.Y_LINTEL), (L + 0.55, self.Y_SHELL),
                        (self.rad(self.Y_SHELL), self.Y_SHELL)], "precast"),
            ("curb", [(53.0, 0.0), (53.0, 1.0), (50.4, 1.0), (50.4, -2.0)], "curb"),
            ("basin_floor", [(50.4, -2.0), (0.0, -2.0)], "basin"),
        ]

    # Where the curb is cut away for the cold-water outlet (x0, x1, z0, z1; the outlet's own walls fill it).
    NOTCHES = [(-2.5, 2.5, -53.2, -50.2)]

    def notched(self, x, z):
        return any(x0 < x < x1 and z0 < z < z1 for x0, x1, z0, z1 in self.NOTCHES)

    def door_cut(self, i, y):
        """Whether shell cell (ring index i, middle height y) is cut away for a door. A door's opening runs along the
        surface grid (whole cells across, sill and head on the grid's lines), so the cut is exact."""
        return any(i0 <= i < i0 + n and y0 < y < y1 for i0, n, y0, y1 in self.DOORS)

    def cells(self, phi_range=None):
        """Every surface cell: (block, material, corners [4 x (x, y, z)], normals [4 x 3], (i, k)), the corners
        counter-clockwise seen from outside. phi_range (a0, a1) limits it (radians, phi = atan2(x, z))."""
        out = []
        dphi = 2 * math.pi / self.NPHI
        for name, poly, mat in self.surfaces():
            # Points along the profile every CELL (each straight run cut into equal steps), with the meridian's
            # length so far (the texture's v) and the profile's outward normal at each.
            pts = []
            run = 0.0
            for a, b in zip(poly, poly[1:]):
                seg = math.dist(a, b)
                n = max(1, int(math.ceil(seg / CELL - 1e-6)))
                for k in range(n):
                    t = k / n
                    pts.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, run + seg * t))
                run += seg
            pts.append((poly[-1][0], poly[-1][1], run))
            nrm = []
            for k in range(len(pts)):
                a, b = pts[max(0, k - 1)], pts[min(len(pts) - 1, k + 1)]
                dr, dy = b[0] - a[0], b[1] - a[1]
                ln = math.hypot(dr, dy) or 1
                nrm.append((dy / ln, -dr / ln))          # outward (left of the direction of travel)
            # Sharp corners (the cornice, lintel and curb edges) keep each face's own normal.
            seg_n = []
            for k in range(len(pts) - 1):
                dr, dy = pts[k + 1][0] - pts[k][0], pts[k + 1][1] - pts[k][1]
                ln = math.hypot(dr, dy) or 1
                seg_n.append((dy / ln, -dr / ln))
            for i in range(self.NPHI):
                a0, a1 = i * dphi, (i + 1) * dphi
                am = (a0 + a1) / 2
                if phi_range and (am - phi_range[0]) % (2 * math.pi) > (phi_range[1] - phi_range[0]) % (2 * math.pi):
                    continue
                for k in range(len(pts) - 1):
                    p, q = pts[k], pts[k + 1]
                    sn = seg_n[k]
                    smooth = name.startswith("shell")
                    n0 = nrm[k] if smooth else sn
                    n1 = nrm[k + 1] if smooth else sn
                    C = [(p[0], p[1], a0), (p[0], p[1], a1), (q[0], q[1], a1), (q[0], q[1], a0)]
                    N = [n0, n0, n1, n1]
                    P = [(r * math.sin(a), y, r * math.cos(a)) for r, y, a in C]
                    NN = [(nr * math.sin(a), ny, nr * math.cos(a)) for (nr, ny), (_, _, a) in zip(N, C)]
                    # Its block: the one holding its middle, nudged a hair into the concrete.
                    rm, ym = (p[0] + q[0]) / 2 - sn[0] * 0.02, (p[1] + q[1]) / 2 - sn[1] * 0.02
                    if name == "curb" and self.notched(rm * math.sin(am), rm * math.cos(am)):
                        continue
                    if name.startswith("shell") and self.door_cut(i, (p[1] + q[1]) / 2):
                        continue
                    bx, by, bz = math.floor(rm * math.sin(am)), math.floor(ym), math.floor(rm * math.cos(am))
                    out.append(((bx, by, bz), mat, P, NN, (i, p[2]), name))
        return out

    def material(self, block, mat):
        """The texture a cell of a block uses: the shell's weathering by height, a variant by place."""
        bx, by, bz = block
        v = (bx * 7919 + bz * 104729) % 6
        if mat == "shell":
            if by >= self.Y_TOP - 25:
                return f"ct_concrete_top{min(4, int((self.Y_TOP + 1 - by) / 5))}_{v}"
            if by < self.Y_SHELL + 16:
                return f"ct_concrete_low{max(0, min(7, int((self.Y_SHELL + 16 - by) / 2)))}_{v}"
            return f"ct_concrete_mid_{v}"
        if mat == "inside":
            return f"ct_concrete_in_{v}"
        return {"cornice": f"ct_concrete_flat_{v}", "precast": "ct_precast", "curb": "ct_precast_curb", "basin": "ct_basin"}[mat]

    def block_models(self, phi_range=None):
        """{block: [quad, ...]} with each quad {"t", "v": 4 x [x, y, z (model px), u, v, nx, ny, nz]} in the block's
        own frame, the mesh model format."""
        models = {}
        for (bx, by, bz), mat, P, NN, (i, s), name in self.cells(phi_range):
            u0 = (i % 2) * 0.5
            vv0 = (s % 1.0)
            quad = []
            for k, ((x, y, z), n) in enumerate(zip(P, NN)):
                u = u0 + (0.5 if k in (1, 2) else 0.0)
                v = vv0 + (min(CELL, 1.0 - vv0) if k in (2, 3) else 0.0)
                quad.append([round((x - bx) * 16, 4), round((y - by) * 16, 4), round((z - bz) * 16, 4),
                             round(u, 4), round(1 - v, 4), *[round(c, 4) for c in n]])
            models.setdefault((bx, by, bz), []).append({"t": self.material((bx, by, bz), mat), "v": quad})
        return models

    # -------------------------------------------------------------- the columns (drawn by the controller's renderer)

    def column_ops(self):
        """Footings and diagonal columns as oriented boxes ["o", cx, cy, cz, ux, uy, uz, vx, vy, vz, hu, hv, hw,
        material], in blocks from the tower's axis at grass level (control_boards' format)."""
        ops = []
        for i in range(self.NB):
            ph = 2 * math.pi * i / self.NB
            rv = np.array([math.sin(ph), 0, math.cos(ph)])
            tv = np.array([math.cos(ph), 0, -math.sin(ph)])
            c = rv * 51.7 + [0, 1.25, 0]      # set 0.1 into the curb, so its foot shares no plane with the curb's top
            ops.append(["o", *c, *tv, 0, 1, 0, 1.2, 0.35, 1.2, "ct_precast"])
            for s in (-1, 1):
                p0 = rv * 51.7 + [0, 1.6, 0]
                p1 = np.array([math.sin(ph + s * math.pi / self.NB), 0, math.cos(ph + s * math.pi / self.NB)]) * (self.RL - 0.1) + [0, self.Y_LINTEL + 0.2, 0]
                ax = p1 - p0
                L = np.linalg.norm(ax)
                ax /= L
                side = np.cross(ax, rv)
                side /= np.linalg.norm(side)
                ops.append(["o", *((p0 + p1) / 2), *side, *ax, 0.4, L / 2, 0.4, "ct_column"])
        return ops


# ------------------------------------------------------------------ textures (128 px a block)

def _noise(size, seed, scale, octaves=3):
    r = np.random.default_rng(seed)
    out = np.zeros((size, size))
    amp = 1.0
    for _ in range(octaves):
        n = max(2, size // max(1, int(scale)))
        g = r.normal(0, 1, (n, n))
        big = np.kron(g, np.ones((size // n + 1, size // n + 1)))[:size, :size]
        img = Image.fromarray(((big - big.min()) / (np.ptp(big) + 1e-9) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(size / n / 2))
        out += (np.asarray(img, float) / 255 - 0.5) * amp
        amp *= 0.5
        scale /= 2
    return out


def concrete(seed, base=(168, 166, 158), zone="mid", damp=0.0, streak=1.0, inside=False, lift=True, size=TEX):
    """Cast concrete, one block: grain and aggregate, a jump-form lift joint along the bottom with its form-tie holes,
    streaks (top: from the cornice drip), damp and algae near the inlet."""
    r = np.random.default_rng(seed)
    a = np.ones((size, size, 3)) * np.array(base, float)
    a += _noise(size, seed, 64)[..., None] * 16 + _noise(size, seed + 1, 12)[..., None] * 8 + r.normal(0, 4.5, (size, size))[..., None]
    for _ in range(size * size // 90):
        x, y = r.integers(0, size, 2)
        a[y, x] += r.choice([-22, -14, 16, 22])
    a *= np.mean(base) / max(1.0, a.mean())
    rs = np.random.default_rng(seed % 6 + 100)
    for _ in range(rs.integers(1, 5)):
        x0, w, k = rs.random(), 0.01 + rs.random() * 0.03, 0.4 + rs.random() * 0.8
        col = slice(int(x0 * size), int(x0 * size) + max(1, int(w * size)))
        s = {"top": 1.0, "mid": 0.45, "low": 0.6}[zone] * streak * (1.4 if inside else 1)
        a[:, col] -= 26 * k * s
    if zone == "low" or inside:
        g = damp if zone == "low" else 0.6
        a *= 1 - 0.2 * g
        for _ in range(int(size * (2.0 if inside else 1.6 * damp))):
            x, y = r.integers(0, size, 2)
            h = int(r.integers(2, 9))
            a[y:y + h, x:x + 1] = a[y:y + h, x:x + 1] * 0.82 + np.array([30, 42, 28]) * 0.18
    if inside:
        a *= 0.78
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    if lift:
        d = ImageDraw.Draw(img)
        d.line([(0, size - 2), (size, size - 2)], fill=(92, 92, 88), width=2)
        d.line([(0, size - 4), (size, size - 4)], fill=(190, 188, 182), width=1)
        for k in range(4):
            cx, cy = int((k + 0.5 + (0.25 if seed % 2 else 0)) * size / 4) % size, size - 12
            d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(150, 148, 142))
            d.ellipse([cx - 2.5, cy - 2.5, cx + 2.5, cy + 2.5], fill=(58, 58, 56))
    return img.convert("RGBA")


def precast(seed, base=(184, 181, 172), size=64, joints=True):
    r = np.random.default_rng(seed)
    a = np.ones((size, size, 3)) * np.array(base, float) + _noise(size, seed, 32)[..., None] * 10 + r.normal(0, 3, (size, size))[..., None]
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(img)
    if joints:
        d.rectangle([0, 0, size - 1, size - 1], outline=tuple(int(c * 0.86) for c in base))   # the precast units' joints
    return img.convert("RGBA")


def textures():
    """{name: image} for every texture the structure uses."""
    out = {}
    for v in range(6):
        for lvl in range(5):
            out[f"ct_concrete_top{lvl}_{v}"] = concrete(10 * v + 1, zone="top", streak=1 - lvl / 5)
        for lvl in range(8):
            out[f"ct_concrete_low{lvl}_{v}"] = concrete(10 * v + 3, zone="low", damp=(lvl + 0.5) / 8)
        out[f"ct_concrete_mid_{v}"] = concrete(10 * v + 2, zone="mid")
        out[f"ct_concrete_in_{v}"] = concrete(70 + v, inside=True)
        out[f"ct_concrete_flat_{v}"] = concrete(90 + v, lift=False)
    out["ct_basin"] = concrete(80, base=(132, 134, 126), zone="low", damp=0.9, lift=False)
    out["ct_precast"] = precast(5)
    out["ct_column"] = precast(6, base=(186, 183, 174), joints=False)   # a column is cast in one piece
    out["ct_precast_curb"] = precast(7, base=(170, 168, 160))
    return out


MEDIUM = Tower()
LARGE = Tower("ct_natural_large", 62.0, 37.0, 135.0, 174.2)
# The shell door at the top of the stair tower and bridge (piece 4): three cells across (1.39 m) facing 175 degrees,
# beside the outlet, from the sill at 12.6 (a grid line, one metre above the lintel) to the head at 14.85.
MEDIUM.DOORS = [(316, 3, 12.6, 14.85),
                # the crossover at the throat (piece 5): from the outside ladder to the inside one, 3 cells (0.89 m)
                (306, 3, 114.6, 116.6)]


# ================================================================== piece 2: the basin's cold-water outlet and make-up
#
# Real-world basis: the basin's water leaves through an outlet flume cut through the basin wall into a sump, past
# stop-log guides (a slot either side; aluminium stop logs are dropped in to dewater the sump and are stored on a rack
# beside it) and an inclined bar screen; the circulating water conduit leaves the sump's back wall buried. The sump
# is covered with bar grating on steel beams, guarded where it stands above grade, with a hatch and fixed ladder for
# entry (a permit-required confined space, signed as such). Basin level and temperature are measured at the sump: a
# level transmitter on a stilling well and a temperature element in a thermowell, their conduits into a junction box
# and away underground. Make-up water comes in buried through an isolating butterfly valve in a precast valve pit:
# gear operator, extension stem (non-rising) up to an indicating floor stand with a handwheel at grade (stem guides
# no more than 10 ft apart; this stem is shorter), and discharges into the basin through the basin wall below water.

class Ops:
    """Drawing operations in control_boards' format, in blocks from the tower's axis at grass level. d is the distance
    out from the axis along -z (the outlet's side: z = -d); x is across. Faces, when given as a dict: out (-z, away
    from the tower), in (+z), left (-x), right (+x), top, bottom; a missing face is left out."""

    ORDER = ("out", "in", "left", "right", "top", "bottom")

    def __init__(self):
        self.still, self.lit = [], []

    def _faces(self, f):
        if isinstance(f, dict):
            return [f.get(k) for k in self.ORDER]
        return f

    def box(self, x0, x1, y0, y1, d0, d1, faces, lit=False):
        (self.lit if lit else self.still).append(["b", x0, y0, -d1, x1, y1, -d0, self._faces(faces)])

    def obox(self, c, u, v, hu, hv, hw, faces):
        """c, u, v in (x, y, d); u and v unit axes."""
        cw = (c[0], c[1], -c[2])
        uw = (u[0], u[1], -u[2])
        vw = (v[0], v[1], -v[2])
        self.still.append(["o", *cw, *uw, *vw, hu, hv, hw, faces])

    def bar(self, a, b, w, t, faces, up=(0, 1, 0)):
        """A rectangular bar from a to b (x, y, d): w across, t through."""
        a, b = np.array(a, float), np.array(b, float)
        ax = b - a
        L = np.linalg.norm(ax)
        ax /= L
        side = np.cross(ax, up)
        if np.linalg.norm(side) < 1e-6:
            side = np.cross(ax, (1, 0, 0))
        side /= np.linalg.norm(side)
        self.obox((a + b) / 2, side, ax, w / 2, L / 2, t / 2, faces)

    def cyl(self, a, b, r, mat, cap=None, sides=12, lit=False):
        (self.lit if lit else self.still).append(["c", a[0], a[1], -a[2], b[0], b[1], -b[2], r, sides, mat,
                                                  cap if cap is not None else mat])

    def quad(self, tl, bl, br, tr, mat, lit=False):
        P = [(p[0], p[1], -p[2]) for p in (tl, bl, br, tr)]
        (self.lit if lit else self.still).append(["q", *P[0], *P[1], *P[2], *P[3], mat])

    def ops(self):
        return {"still": self.still, "lit": self.lit, "blink": []}


TAGS = ["CT-1A OUTLET", "LT-CT1A", "TE-CT1A", "JB-CT1A", "MU-1", "STOP LOGS", "CT-1A SUMP", "MAKEUP",
        # the local panel's (piece 3)
        "BASIN LEVEL", "HOT WATER IN", "COLD WATER OUT", "CIRC WATER FLOW", "RANGE", "APPROACH", "WET BULB", "MU-1 OPEN",
        "MU-1 SHUT", "BASIN HI", "BASIN LO", "OBST LT FAIL", "POWER ON", "LAMP TEST", "ALARM ACK", "HORN",
        "ZS-MU1", "LP-CT1A",
        # the stair tower and shell door's (piece 4)
        "CT-1A STAIR", "CT-1A SHELL DOOR",
        # the ladder's (piece 5)
        "CT-1A CROSSOVER",
        # the obstruction lights' and lightning protection's (piece 6)
        "OBST LT CONTROL", "LP TEST JOINT"]


def tag(i):
    """Engraved aluminium tag i on the ct_tags sheet (8 across, 4 down, each 256 x 64 px)."""
    c, r = i % 8, i // 8
    return ["ct_tags", c / 8, r / 4, (c + 1) / 8, (r + 1) / 4]


def outlet_ops(tower, makeup_open=True, level=-0.4, covers=True):
    """covers=False leaves the deck grating and the pit's cover off (for looking in)."""
    """The outlet sump, its deck, screen, stop logs, instruments, and the make-up valve pit (piece 2)."""
    o = Ops()
    PC, CB = "ct_precast", "ct_precast_curb"
    G, DS, GV, AL, CI, VB, YE = "ct_grating", "ct_steel_dark", "ct_galv", "ct_alum", "ct_cast", "ct_valve", "ct_yellow"
    HOLE, WATER = "ct_hole", "ct_water"
    TOP = 1.03                     # the outlet walls' caps (a hair over the curb's top, so they never share its plane)
    DECK = 1.0
    # -- the flume and sump: side walls, back wall round the conduit's mouth, floors and the step down
    for s in (-1, 1):
        o.box(*sorted((s * 2.0, s * 2.75)), -4.2, TOP, 50.4, 60.0, PC)
    o.box(-2.0, -1.2, -4.2, TOP, 59.5, 60.0, PC)
    o.box(1.2, 2.0, -4.2, TOP, 59.5, 60.0, PC)
    o.box(-1.2, 1.2, -4.2, -3.7, 59.5, 60.0, PC)
    o.box(-1.2, 1.2, -1.3, TOP, 59.5, 60.0, PC)
    o.box(-2.0, 2.0, -2.3, -2.0, 50.4, 51.8, "ct_basin")
    o.box(-2.0, 2.0, -3.9, -2.3, 51.5, 51.8, "ct_basin")
    o.box(-2.0, 2.0, -4.2, -3.9, 51.5, 59.5, "ct_basin")
    # the circulating water conduit's mouth: its lining going away into the dark
    for (tl, bl, br, tr) in (((-1.2, -1.3, 60.0), (-1.2, -1.3, 62.0), (1.2, -1.3, 62.0), (1.2, -1.3, 60.0)),
                             ((-1.2, -3.7, 62.0), (-1.2, -3.7, 60.0), (1.2, -3.7, 60.0), (1.2, -3.7, 62.0)),
                             ((-1.2, -1.3, 62.0), (-1.2, -1.3, 60.0), (-1.2, -3.7, 60.0), (-1.2, -3.7, 62.0)),
                             ((1.2, -1.3, 60.0), (1.2, -1.3, 62.0), (1.2, -3.7, 62.0), (1.2, -3.7, 60.0)),
                             ((-1.2, -1.3, 62.0), (-1.2, -3.7, 62.0), (1.2, -3.7, 62.0), (1.2, -1.3, 62.0))):
        o.quad(tr, br, bl, tl, HOLE)          # (facing into the conduit: the game's front is (tr - tl) x (bl - tl))
    # the water in the flume and sump (live: its level is the basin's)
    o.quad((-2.0, level, 50.4), (-2.0, level, 59.5), (2.0, level, 59.5), (2.0, level, 50.4), WATER, lit=True)
    # -- stop-log guides: a steel channel cast into each wall, open to the flume
    for s in (-1, 1):
        x0, x1 = sorted((s * 2.0, s * 1.86))
        o.box(x0, x1, -3.9, 0.98, 52.12, 52.2, GV)
        o.box(x0, x1, -3.9, 0.98, 52.5, 52.58, GV)
        o.box(*sorted((s * 1.94, s * 2.0)), -3.9, 0.98, 52.2, 52.5, {"right" if s < 0 else "left": DS})
    # -- the bar screen, leaning 15 degrees downstream: side frames, bars at 60 mm, spacers
    lean = math.radians(15)
    yb, yt, db = -3.9, 0.92, 54.3
    dt = db + math.tan(lean) * (yt - yb)
    along = np.array([0, math.cos(lean), math.sin(lean)])
    for s in (-1, 1):
        o.bar((s * 1.93, yb, db), (s * 1.93, yt, dt), 0.12, 0.1, DS)
    for k in range(62):
        x = -1.83 + k * 0.06
        o.bar((x, yb, db), (x, yt, dt), 0.075, 0.012, GV, up=(1, 0, 0))
    for f in (0.15, 0.5, 0.85):
        y = yb + (yt - yb) * f
        dd = db + math.tan(lean) * (y - yb)
        o.box(-1.87, 1.87, y - 0.03, y + 0.03, dd + 0.045, dd + 0.065, DS)
    # -- the deck: beams across on the walls, grating panels between, the hatch over the ladder
    for d in (51.4, 52.95, 54.0, 55.6, 57.0, 58.0, 59.4):
        o.box(-2.0, 2.0, DECK - 0.26, DECK - 0.045, d - 0.05, d + 0.05, DS)
    edges = [50.42, 51.4, 52.95, 54.0, 55.6, 57.0, 58.0, 59.48]
    for a, b in zip(edges, edges[1:]):
        if not covers:
            continue
        if a >= 58.0:
            o.box(-1.98, 0.58, DECK - 0.04, DECK, a + 0.003, b - 0.003, G)
            o.box(1.62, 1.98, DECK - 0.04, DECK, a + 0.003, b - 0.003, G)
        else:
            o.box(-1.98, 1.98, DECK - 0.04, DECK, a + 0.003, b - 0.003, G)
    # hatch: diamond plate in an angle frame, hinges on the far side, a recessed lift handle, the ladder's
    # telescoping safety post folded beside it (yellow)
    o.box(0.6, 1.6, DECK - 0.012, DECK + 0.008, 58.02, 59.46, "ct_diamond")
    for x in (0.75, 1.45):
        o.cyl((x - 0.06, DECK + 0.025, 59.43), (x + 0.06, DECK + 0.025, 59.43), 0.022, GV, sides=8)
    o.box(1.0, 1.2, DECK + 0.008, DECK + 0.02, 58.1, 58.14, GV)
    o.box(1.64, 1.7, DECK, DECK + 0.45, 59.3, 59.36, YE)
    # fixed ladder down the back wall into the sump
    for x in (0.9, 1.3):
        o.box(x - 0.01, x + 0.01, -3.9, DECK - 0.05, 59.25, 59.31, GV)
        for y in (-3.2, -1.4, 0.4):
            o.box(x - 0.01, x + 0.01, y - 0.03, y + 0.03, 59.31, 59.5, GV)
    for k in range(int((DECK - 0.4 + 3.9) / 0.305)):
        y = -3.9 + 0.3 + k * 0.305
        o.cyl((0.91, y, 59.28), (1.29, y, 59.28), 0.0095, GV, sides=6)
    # -- guardrails (OSHA 1910.29: 42 in top rail, 21 in mid rail, toeboard; posts <= 6 ft) on the outlet walls,
    # with a gap at the steps on the +x side
    def rail(pts):
        """A guardrail along a path: posts (one at each corner), rails and toeboard; each run after a corner starts
        at the previous run's side, so no two rails share a face."""
        for i, ((x0, d0), (x1, d1)) in enumerate(zip(pts, pts[1:])):
            L = math.hypot(x1 - x0, d1 - d0)
            n = max(1, math.ceil(L / 1.8))
            for k in range(0 if i == 0 else 1, n + 1):
                x, d = x0 + (x1 - x0) * k / n, d0 + (d1 - d0) * k / n
                o.box(x - 0.025, x + 0.025, TOP, TOP + 1.09, d - 0.025, d + 0.025, GV)
            ux, ud = (x1 - x0) / L, (d1 - d0) / L
            for y, h, t in ((TOP + 1.05, 0.04, 0.04), (TOP + 0.51, 0.03, 0.03), (TOP + 0.056, 0.008, 0.1)):
                tr = 0 if i == 0 else t / 2 + 0.001
                o.bar((x0 + ux * tr, y, d0 + ud * tr), (x1, y, d1), h, t, GV)
    rail([(-2.4, 53.2), (-2.4, 59.75), (2.4, 59.75), (2.4, 57.1)])
    rail([(2.4, 55.9), (2.4, 53.2)])
    # steps from the apron to the deck on the +x side (4 risers of 0.25 m), stringers and a handrail each side
    for k in range(4):
        y = 0.25 * (k + 1)
        xo = 2.75 + 0.3 * (4 - k) - 0.3
        o.box(xo, xo + 0.3, y - 0.04, y, 55.95, 57.05, G)
        o.box(xo - 0.002, xo + 0.03, y - 0.035, y + 0.003, 55.96, 57.04, YE)
    for d in (55.93, 57.07):
        o.bar((3.95, 0.0, d), (2.75, TOP - 0.02, d), 0.2, 0.02, DS, up=(0, 0, 1))
        o.bar((4.05, 0.92, d), (2.75, TOP + 0.92, d), 0.04, 0.04, GV, up=(0, 0, 1))
        o.box(4.03, 4.08, 0.0, 0.94, d - 0.025, d + 0.025, GV)
    # -- confined space sign (ANSI Z535 DANGER) on the back rail, and the outlet's tag on the stop-log guide
    o.box(0.1, 0.55, TOP + 0.62, TOP + 0.92, 59.715, 59.725, {"out": "ct_sign_back", "in": ["ct_sign", 0, 0, 1, 1],
                                                             "left": GV, "right": GV, "top": GV, "bottom": GV})
    for y in (TOP + 0.66, TOP + 0.88):
        o.box(0.14, 0.18, y - 0.015, y + 0.015, 59.725, 59.78, GV)
        o.box(0.47, 0.51, y - 0.015, y + 0.015, 59.725, 59.78, GV)
    o.box(-2.75 + 0.002, -2.75 + 0.004, 0.35, 0.45, 51.4, 51.85, {"left": tag(0)})
    # -- instruments: LT-CT1A on its stilling well, TE-CT1A in a thermowell, conduits into JB-CT1A, away buried
    sx, sd = -1.86, 57.6
    o.cyl((sx, -3.6, sd), (sx, DECK + 0.12, sd), 0.075, GV)
    for y in (-2.8, -1.4, 0.2):
        o.box(-2.0, sx - 0.06, y - 0.03, y + 0.03, sd - 0.09, sd + 0.09, GV)
    o.cyl((sx, DECK + 0.12, sd), (sx, DECK + 0.16, sd), 0.12, GV, sides=16)
    o.cyl((sx, DECK + 0.16, sd), (sx, DECK + 0.34, sd), 0.07, "ct_alum", sides=16)
    o.cyl((sx, DECK + 0.34, sd), (sx, DECK + 0.4, sd), 0.075, "ct_steel_dark", sides=16)
    o.box(sx - 0.08, sx + 0.08, DECK + 0.42, DECK + 0.425, sd - 0.12, sd + 0.12, GV)               # sunshade
    o.box(sx - 0.085, sx + 0.085, DECK + 0.03, DECK + 0.075, sd - 0.085, sd - 0.07, {"in": tag(1), "left": GV, "right": GV,
                                                                                  "top": GV, "bottom": GV})
    tx, td = -1.7, 55.0
    o.cyl((tx, -1.8, td), (tx, DECK + 0.25, td), 0.025, "ct_stainless", sides=8)
    o.box(-2.0, tx - 0.03, DECK - 0.15, DECK - 0.09, td - 0.05, td + 0.05, GV)
    o.cyl((tx, DECK + 0.25, td), (tx, DECK + 0.37, td), 0.05, "ct_alum", sides=12)
    o.box(tx - 0.07, tx + 0.07, DECK + 0.13, DECK + 0.165, td - 0.035, td - 0.02, {"in": tag(2), "left": GV, "right": GV,
                                                                                 "top": GV, "bottom": GV})
    # conduits (25 mm, galvanised): from each head's side down through the grating, under the deck to the wall,
    # through it in a sleeve, then along the wall's outer face into the junction box; one conduit from the box's
    # bottom down into the ground. Straps every metre or so.
    CR = 0.016
    for hx, hy, hd in ((sx - 0.1, DECK + 0.3, sd), (tx - 0.065, DECK + 0.31, td)):
        o.cyl((hx, hy, hd), (hx, 0.8, hd), CR, GV, sides=6)
        o.cyl((hx, 0.8, hd), (-2.0, 0.8, hd), CR, GV, sides=6)
        o.cyl((-2.02, 0.8, hd), (-1.99, 0.8, hd), 0.03, GV, sides=8)                 # the sleeve's collar
        o.cyl((-2.75, 0.8, hd), (-2.82, 0.8, hd), CR, GV, sides=6)
        o.box(-2.86, -2.78, 0.74, 0.86, hd - 0.04, hd + 0.04, GV)                   # conduit body (LB)
        e = 56.65 if hd > 56.3 else 55.95
        o.cyl((-2.82, 0.74, hd), (-2.82, 0.62, hd), CR, GV, sides=6)
        o.cyl((-2.82, 0.62, hd), (-2.82, 0.62, e), CR, GV, sides=6)
        mid = (hd + e) / 2
        o.box(-2.85, -2.75, 0.6, 0.64, mid - 0.03, mid + 0.03, GV)
    o.box(-2.95, -2.75, 0.4, 0.8, 55.95, 56.65, "ct_jb")
    o.box(-2.956, -2.95, 0.68, 0.73, 56.1, 56.5, {"left": tag(3)})
    o.cyl((-2.85, 0.4, 56.3), (-2.85, 0.0, 56.3), 0.02, GV, sides=6)
    o.cyl((-2.85, 0.0, 56.3), (-2.85, -0.4, 56.3), 0.02, GV, sides=6)
    # -- stop logs stored on a rack beside the sump (3 across, 2 high, aluminium, 4.3 m)
    for d in (54.0, 57.6):
        o.box(3.3, 4.6, 0.0, 0.18, d - 0.06, d + 0.06, DS)
        for x in (3.3, 4.55):
            o.box(x, x + 0.05, 0.18, 0.62, d - 0.06, d + 0.06, DS)
    for row in range(2):
        for k in range(3):
            x = 3.42 + k * 0.38
            o.box(x, x + 0.3, 0.18 + row * 0.2, 0.36 + row * 0.2, 53.65, 57.95, AL)
    o.box(4.6, 4.602, 0.4, 0.47, 55.6, 56.0, {"right": tag(5)})
    # -- the make-up valve pit MU-1, its valve and floor stand, the line into the basin
    px, pd = -6.5, 55.5
    for (x0, x1, d0, d1) in ((px - 1.2, px + 1.2, pd - 1.2, pd - 1.0), (px - 1.2, px + 1.2, pd + 1.0, pd + 1.2),
                             (px - 1.2, px - 1.0, pd - 1.0, pd + 1.0), (px + 1.0, px + 1.2, pd - 1.0, pd + 1.0)):
        o.box(x0, x1, -2.2, 0.1, d0, d1, PC)
    o.box(px - 1.0, px + 1.0, -2.2, -2.0, pd - 1.0, pd + 1.0, "ct_basin")
    o.box(px - 1.2, px + 1.2, 0.1, 0.12, pd - 1.2, pd - 1.0, GV)
    o.box(px - 1.2, px + 1.2, 0.1, 0.12, pd + 1.0, pd + 1.2, GV)
    o.box(px - 1.0, px + 1.0, -0.12, 0.105, pd - 0.15, pd + 0.15, DS)       # the floor stand's support channel
    if covers:
        for d0, d1 in ((pd - 0.99, pd - 0.155), (pd + 0.155, pd + 0.99)):
            o.box(px - 0.99, px - 0.005, 0.07, 0.11, d0, d1, G)
            o.box(px + 0.005, px + 0.99, 0.07, 0.11, d0, d1, G)
    yc = -1.2
    o.cyl((px, yc, pd - 1.2), (px, yc, pd - 0.16), 0.305, "ct_pipe")
    o.cyl((px, yc, pd + 0.16), (px, yc, pd + 1.2), 0.305, "ct_pipe")
    for d0, d1 in ((pd - 0.16, pd - 0.08), (pd + 0.08, pd + 0.16)):
        o.cyl((px, yc, d0), (px, yc, d1), 0.42, VB, sides=20)
        for k in range(20):
            a = 2 * math.pi * (k + 0.5) / 20
            bx, by = px + 0.37 * math.cos(a), yc + 0.37 * math.sin(a)
            o.cyl((bx, by, d0 - 0.025), (bx, by, d1 + 0.025), 0.016, CI, sides=6)
    o.cyl((px, yc, pd - 0.08), (px, yc, pd + 0.08), 0.37, VB, sides=20)                 # the valve body
    o.box(px - 0.13, px + 0.13, yc + 0.37, yc + 0.47, pd - 0.08, pd + 0.08, VB)          # its neck
    o.box(px - 0.17, px + 0.17, yc + 0.47, yc + 0.75, pd - 0.14, pd + 0.14, VB)          # the gear operator
    o.box(px - 0.18, px + 0.18, yc + 0.6, yc + 0.62, pd + 0.14, pd + 0.141, {"in": tag(4)})
    o.cyl((px, yc + 0.75, pd), (px, 0.12, pd), 0.03, GV, sides=8)                        # the extension stem
    o.box(px - 1.0, px - 0.2, -0.45, -0.39, pd - 0.04, pd + 0.04, GV)                    # its stem guide off the wall
    o.cyl((px - 0.04, -0.42, pd), (px + 0.04, -0.42, pd), 0.06, "ct_brass", sides=10)
    # indicating floor stand: base flange, column, the indicator's window, the handwheel on top
    o.cyl((px, 0.11, pd), (px, 0.15, pd), 0.22, CI, sides=16)
    o.cyl((px, 0.15, pd), (px, 0.85, pd), 0.1, CI, sides=16)
    o.box(px - 0.06, px + 0.06, 0.56, 0.74, pd - 0.112, pd - 0.098, {"in": ["ct_dial", 0, 0, 1, 1], "left": CI, "right": CI,
                                                                    "top": CI, "bottom": CI})
    # the indicator's pointer (live: from SHUT at the bottom to OPEN at the top as the valve turns)
    yi = 0.705 if makeup_open else 0.595
    o.box(px - 0.035, px + 0.035, yi - 0.004, yi + 0.004, pd - 0.118, pd - 0.112, {"in": "ct_needle"}, lit=True)
    o.cyl((px, 0.85, pd), (px, 0.95, pd), 0.06, CI, sides=12)
    for k in range(16):
        a0, a1 = 2 * math.pi * k / 16, 2 * math.pi * (k + 1) / 16
        o.cyl((px + 0.3 * math.cos(a0), 0.97, pd + 0.3 * math.sin(a0)), (px + 0.3 * math.cos(a1), 0.97, pd + 0.3 * math.sin(a1)),
              0.022, "ct_wheel", sides=6)
    for k in range(3):
        a = k * math.pi / 3
        o.cyl((px - 0.29 * math.cos(a), 0.96, pd - 0.29 * math.sin(a)), (px + 0.29 * math.cos(a), 0.96, pd + 0.29 * math.sin(a)),
              0.014, "ct_wheel", sides=6)
    o.box(px - 0.075, px + 0.075, 0.3, 0.34, pd - 0.104, pd - 0.098, {"in": tag(4)})
    # its limit switches (ZS-MU1, open and shut, read by the local panel): a box on the column's side, its conduit
    # down the column, through the cover into the pit and out through the pit wall toward the panel
    o.box(px + 0.1, px + 0.19, 0.5, 0.64, pd - 0.04, pd + 0.04, "ct_jb")
    o.box(px + 0.075, px + 0.1, 0.55, 0.59, pd - 0.02, pd + 0.02, CI)
    o.box(px + 0.1, px + 0.19, 0.6, 0.63, pd - 0.046, pd - 0.04, {"in": tag(24)})
    o.cyl((px + 0.145, 0.5, pd), (px + 0.145, -0.3, pd), 0.014, GV, sides=6)
    o.cyl((px + 0.145, -0.3, pd), (px + 1.0, -0.3, pd), 0.014, GV, sides=6)
    o.box(px + 0.4, px + 0.44, -0.33, -0.27, pd - 0.03, pd + 0.03, GV)
    # the make-up line comes out under the basin wall into the water (buried between)
    zc = math.sqrt(50.4 ** 2 - px ** 2)
    o.cyl((px, yc, zc), (px, yc, zc - 0.6), 0.305, "ct_pipe")
    o.cyl((px, yc, zc - 0.6), (px, yc, zc - 0.68), 0.42, VB, sides=20)
    return o.ops()


def ops_textures():
    """Textures for piece 2's parts."""
    from PIL import ImageFont
    out = {}

    def flat(rgb, seed, n=32, dirt=0.05, brushed=False):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        if brushed:
            a += np.repeat(r.normal(0, 4, (n, 1)), n, 1)[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    out["ct_galv"] = flat((168, 174, 176), 21, brushed=True)
    out["ct_steel_dark"] = flat((72, 78, 82), 22)
    out["ct_alum"] = flat((196, 200, 202), 23, brushed=True)
    out["ct_stainless"] = flat((190, 194, 197), 24, n=128, dirt=0.008, brushed=True)
    out["ct_cast"] = flat((52, 54, 56), 25)
    out["ct_valve"] = flat((40, 70, 110), 26)          # valve and gear operator enamel
    out["ct_pipe"] = flat((44, 62, 92), 27, dirt=0.08)  # coated ductile-iron make-up line
    out["ct_wheel"] = flat((150, 30, 28), 28)           # handwheel, red
    out["ct_yellow"] = flat((222, 172, 22), 29)
    out["ct_brass"] = flat((176, 140, 70), 30)
    out["ct_hole"] = flat((18, 20, 20), 31, dirt=0.1)
    out["ct_needle"] = flat((20, 20, 20), 32, dirt=0)
    out["ct_water"] = Image.new("RGBA", (16, 16), (52, 86, 104, 210))
    # bar grating: 30 x 5 mm bearing bars at 30 mm, cross rods at 100 mm (cut out between)
    g = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dg = ImageDraw.Draw(g)
    for x in range(0, 64, 2):
        dg.line([(x, 0), (x, 63)], fill=(150, 156, 158, 255))
    for y in range(0, 64, 7):
        dg.line([(0, y), (63, y)], fill=(126, 132, 134, 255))
    out["ct_grating"] = g
    dp = flat((150, 154, 156), 33, n=64, brushed=True)
    dd = ImageDraw.Draw(dp)
    for y in range(0, 64, 8):
        for x in range(0, 64, 8):
            o = 4 if (y // 8) % 2 else 0
            dd.line([(x + o, y + 1), (x + o + 3, y + 5)], fill=(190, 194, 196, 255), width=2)
    out["ct_diamond"] = dp
    # junction box: grey enclosure, cover screws
    jb = flat((150, 156, 158), 34, n=64)
    dj = ImageDraw.Draw(jb)
    dj.rectangle([2, 2, 61, 61], outline=(110, 114, 116, 255))
    for x, y in ((6, 6), (57, 6), (6, 57), (57, 57)):
        dj.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(90, 92, 94, 255))
    out["ct_jb"] = jb
    # engraved aluminium tags: brushed metal, dark letters, a rivet each end
    sheet = Image.new("RGBA", (2048, 256), (0, 0, 0, 0))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 34)
    for i, text in enumerate(TAGS):
        c, r = i % 8, i // 8
        cell = flat((198, 202, 204), 40 + i, n=256, dirt=0.02, brushed=True).resize((256, 64))
        d = ImageDraw.Draw(cell)
        d.rectangle([0, 0, 255, 63], outline=(140, 144, 146, 255), width=2)
        for x in (14, 241):
            d.ellipse([x - 6, 26, x + 6, 38], fill=(160, 164, 166, 255), outline=(110, 112, 114, 255))
        f = font
        size = 34
        while d.textlength(text, font=f) > 206 and size > 18:
            size -= 1
            f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", size)
        d.text((128, 33), text, font=f, fill=(36, 38, 40, 255), anchor="mm")
        sheet.paste(cell, (c * 256, r * 64))
    out["ct_tags"] = sheet
    # ANSI Z535.2 DANGER sign: safety alert symbol and DANGER in white on red, black-bordered; the message below
    sg = Image.new("RGBA", (450, 300), (255, 255, 255, 255))
    ds = ImageDraw.Draw(sg)
    ds.rectangle([0, 0, 449, 299], outline=(0, 0, 0, 255), width=10)
    ds.rectangle([10, 10, 439, 92], fill=(0, 0, 0, 255))
    ds.rounded_rectangle([22, 18, 427, 84], radius=30, fill=(200, 16, 46, 255))
    ds.polygon([(70, 74), (96, 26), (122, 74)], fill=(255, 255, 255, 255))
    ds.text((96, 62), "!", font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 36), fill=(200, 16, 46, 255), anchor="mm")
    ds.text((270, 52), "DANGER", font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 52), fill=(255, 255, 255, 255), anchor="mm")
    fb = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 34)
    for k, line in enumerate(("PERMIT-REQUIRED", "CONFINED SPACE", "DO NOT ENTER")):
        ds.text((225, 140 + k * 46), line, font=fb, fill=(0, 0, 0, 255), anchor="mm")
    out["ct_sign"] = sg
    out["ct_sign_back"] = flat((180, 184, 186), 35)
    # the floor stand's indicator: OPEN / SHUT arc behind a window
    dl = Image.new("RGBA", (128, 160), (232, 230, 220, 255))
    dd = ImageDraw.Draw(dl)
    dd.rectangle([0, 0, 127, 159], outline=(40, 40, 40, 255), width=6)
    fs = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 22)
    dd.text((64, 26), "OPEN", font=fs, fill=(20, 20, 20, 255), anchor="mm")
    dd.text((64, 136), "SHUT", font=fs, fill=(20, 20, 20, 255), anchor="mm")
    for k in range(9):
        y = 44 + k * 9
        dd.line([(40, y), (88 if k % 4 == 0 else 74, y)], fill=(30, 30, 30, 255), width=2)
    out["ct_dial"] = dl
    return out


# ================================================================== piece 3: the local panel LP-CT1A
#
# Real-world basis: a tower's local instrument panel is a NEMA 4X stainless enclosure on a pair of strut posts set in
# a small concrete pad, under a sunshield, near the basin outlet: switchboard meters (black cases standing out of the
# door, the dial set back behind glass, a zero adjuster under it), digital indicators, pilot lights in chrome bezels,
# pushbuttons, an alarm horn on the side; engraved metal tags under or over each device; field wiring up from below
# in conduit through the pad. The make-up valve MU-1 is hand-worked (floor stand), so the panel shows its position
# from limit switches on the floor stand (ZS-MU1), it does not drive it. Readings at full load, as the control room
# shows them: basin level 92 %, hot water 105 F, cold water 85 F, flow 480,000 gpm, wet bulb 72 F (range 20, approach
# 13).

METERS = [("level", "BASIN LEVEL", 0, 100, "%"), ("hot", "HOT WATER IN", 40, 140, "°F"), ("cold", "COLD WATER OUT", 40, 140, "°F"),
          ("flow", "CIRC WATER FLOW", 0, 600, "x1000 GPM")]
LAMPS = [("mu_open", "MU-1 OPEN", (230, 50, 36)), ("mu_shut", "MU-1 SHUT", (40, 190, 70)), ("hi", "BASIN HI", (245, 165, 30)),
         ("lo", "BASIN LO", (245, 165, 30)), ("strobe_fail", "OBST LT FAIL", (245, 165, 30)), ("power", "POWER ON", (250, 244, 226))]
A0, SWEEP = 225.0, 270.0    # the meters' scales: from 225 degrees (lower left) round 270 degrees, clockwise


def panel_state(load=1.0, makeup_open=True):
    """What the panel shows (the same numbers the control room's boards show for the tower)."""
    hot, cold = 70 + 35 * load, 70 + 15 * load
    return {"level": 92.0, "hot": hot, "cold": cold, "flow": 480 * load, "range": hot - cold, "approach": cold - 72 if load > 0 else None,
            "wet_bulb": 72.0, "mu_open": makeup_open, "mu_shut": not makeup_open, "hi": False, "lo": False, "strobe_fail": False,
            "power": True}


def panel_ops(px=6.2, pd=57.5, state=None):
    """LP-CT1A standing on the apron by the outlet, its door toward the plant (out, +d). Local frame: lx to the right
    of a player facing the door, ly up, lz toward the player."""
    st = state or panel_state()
    o = Ops()
    ST, GV, BK, PC = "ct_stainless", "ct_galv", "ct_meter_case", "ct_precast"
    FL = {"front": "out", "back": "in", "right": "left", "left": "right", "top": "top", "bottom": "bottom"}

    def B(lx0, lx1, ly0, ly1, lz0, lz1, faces, lit=False):
        if isinstance(faces, dict):
            faces = {FL[k]: v for k, v in faces.items()}
        o.box(px - lx1, px - lx0, ly0, ly1, pd + lz0, pd + lz1, faces, lit=lit)

    def C(a, b, r, mat, cap=None, sides=12, lit=False):
        o.cyl((px - a[0], a[1], pd + a[2]), (px - b[0], b[1], pd + b[2]), r, mat, cap, sides, lit)

    def Q(tl, bl, br, tr, mat, lit=False):
        f = lambda p: (px - p[0], p[1], pd + p[2])
        o.quad(f(tr), f(br), f(bl), f(tl), mat, lit=lit)    # (lx runs to the viewer's right, so mirrored for the game)

    def T(i, cx, cy, z, w=0.17, h=0.036):
        """An engraved aluminium tag, riveted flat to the door face at depth z."""
        B(cx - w / 2, cx + w / 2, cy - h / 2, cy + h / 2, z, z + 0.004, {"front": tag(i), "left": GV, "right": GV, "top": GV, "bottom": GV})

    # the pad, the posts, the enclosure, the sunshield
    B(-0.75, 0.75, 0.0, 0.15, -0.45, 0.45, PC)
    for x in (-0.38, 0.38):
        B(x - 0.021, x + 0.021, 0.15, 2.02, -0.3, -0.258, GV)
    for y in (0.75, 1.62):
        B(-0.5, 0.5, y - 0.021, y + 0.021, -0.258, -0.216, GV)
    D0 = -0.216 + 0.3                                 # the enclosure's front edge (door face is D0 + 0.025)
    B(-0.5, 0.5, 0.6, 1.8, -0.216, D0, ST)
    B(-0.5, 0.5, 0.6, 1.8, D0, D0 + 0.004, "ct_gasket")
    DF = D0 + 0.026
    B(-0.495, 0.495, 0.605, 1.795, D0 + 0.004, DF, ST)                       # the door
    for y in (0.75, 1.2, 1.65):                                                 # its hinges, left side
        C((-0.5, y - 0.05, D0 + 0.012), (-0.5, y + 0.05, D0 + 0.012), 0.012, GV, sides=8)
    for y in (0.85, 1.55):                                                      # quarter-turn latches, right side
        C((0.45, y, DF), (0.45, y, DF + 0.012), 0.026, ST, sides=12)
        B(0.443, 0.457, y - 0.022, y + 0.022, DF + 0.012, DF + 0.024, "ct_cast")
    for (a, b) in (((-0.64, 2.06, -0.40), (0.64, 1.96, 0.42)),):
        B(-0.64, 0.64, 1.98, 2.0, -0.45, 0.42, ST)                             # the sunshield
    for x in (-0.38, 0.38):
        o.bar((px - x, 2.0, pd - 0.258), (px - x, 1.98, pd + 0.3), 0.03, 0.03, GV)
    # the alarm horn on the right side (a grille-faced projector)
    C((0.5, 1.62, 0.0), (0.56, 1.62, 0.0), 0.05, "ct_cast", sides=10)
    C((0.56, 1.62, 0.0), (0.64, 1.62, 0.0), 0.075, "ct_horn", sides=12)
    T(23, 0.58, 1.5, 0.08, w=0.1)
    # field conduits up through the pad into hubs under the enclosure
    for x in (-0.25, 0.0, 0.25):
        C((x, 0.15, 0.0), (x, 0.6, 0.0), 0.018, GV, sides=6)
        C((x, 0.585, 0.0), (x, 0.6, 0.0), 0.03, GV, sides=8)
    # the title tag
    B(-0.2, 0.2, 1.725, 1.765, DF, DF + 0.004, {"front": ["ct_title", 0, 0, 1, 1], "left": GV, "right": GV, "top": GV, "bottom": GV})
    # -- meters: case out of the door, dial set back, glass, zero screw; needle live
    MS, MD = 0.19, 0.09
    for i, (key, label, lo, hi, unit) in enumerate(METERS):
        cx, cy = -0.345 + i * 0.23, 1.53
        x0, x1, y0, y1 = cx - MS / 2, cx + MS / 2, cy - MS / 2, cy + MS / 2
        fr = 0.018
        B(x0, x1, y0, y0 + fr, DF, DF + MD, BK)
        B(x0, x1, y1 - fr, y1, DF, DF + MD, BK)
        B(x0, x0 + fr, y0 + fr, y1 - fr, DF, DF + MD, BK)
        B(x1 - fr, x1, y0 + fr, y1 - fr, DF, DF + MD, BK)
        fz = DF + 0.03
        B(x0 + fr, x1 - fr, y0 + fr, y1 - fr, DF, fz, {"front": ["ct_dial_" + key, 0, 0, 1, 1]})
        Q((x0 + fr, y1 - fr, DF + MD - 0.008), (x0 + fr, y0 + fr, DF + MD - 0.008), (x1 - fr, y0 + fr, DF + MD - 0.008),
          (x1 - fr, y1 - fr, DF + MD - 0.008), "ct_glass")
        C((cx, y0 + 0.009, DF + MD), (cx, y0 + 0.009, DF + MD + 0.006), 0.006, "ct_cast", sides=8)
        v = st[key]
        a = math.radians(A0 - SWEEP * (min(hi, max(lo, v)) - lo) / (hi - lo))
        pcx, pcy = cx, cy - 0.006
        u = np.array([-math.cos(a), math.sin(a), 0.0]) * -1     # pointing along the needle (local lx, ly)
        ln = 0.065
        tip = (pcx + math.cos(a) * ln, pcy + math.sin(a) * ln)
        nz = fz + 0.012
        o.bar((px - (pcx - math.cos(a) * 0.012), pcy - math.sin(a) * 0.012, pd + nz), (px - tip[0], tip[1], pd + nz), 0.004, 0.002,
              "ct_needle_red", up=(0, 0, 1))
        C((pcx, pcy, fz + 0.006), (pcx, pcy, fz + 0.016), 0.008, BK, sides=10)
        T(8 + i, cx, y0 - 0.032, DF, w=0.19)
    # -- digital indicators: bezel out of the door, the LED window set back (live figures)
    for i, (key, ti) in enumerate((("range", 12), ("approach", 13), ("wet_bulb", 14))):
        cx, cy = -0.3 + i * 0.3, 1.24
        w, h, fr = 0.2, 0.09, 0.016
        x0, x1, y0, y1 = cx - w / 2, cx + w / 2, cy - h / 2, cy + h / 2
        B(x0, x1, y0, y0 + fr, DF, DF + 0.05, BK)
        B(x0, x1, y1 - fr, y1, DF, DF + 0.05, BK)
        B(x0, x0 + fr, y0 + fr, y1 - fr, DF, DF + 0.05, BK)
        B(x1 - fr, x1, y0 + fr, y1 - fr, DF, DF + 0.05, BK)
        B(x0 + fr, x1 - fr, y0 + fr, y1 - fr, DF, DF + 0.03, {"front": ["ct_digit_" + key, 0, 0, 1, 1]}, lit=True)
        Q((x0 + fr, y1 - fr, DF + 0.045), (x0 + fr, y0 + fr, DF + 0.045), (x1 - fr, y0 + fr, DF + 0.045), (x1 - fr, y1 - fr, DF + 0.045), "ct_glass")
        T(ti, cx, y0 - 0.045, DF, w=0.17)
    # -- pilot lights: tag over each, then the lamp (lock ring, chrome bezel, lens; live)
    for i, (key, label, col) in enumerate(LAMPS):
        cx, cy = -0.375 + i * 0.15, 0.98
        T(15 + i, cx, cy + 0.06, DF, w=0.135, h=0.032)
        C((cx, cy, DF), (cx, cy, DF + 0.008), 0.032, ST, sides=12)
        C((cx, cy, DF + 0.008), (cx, cy, DF + 0.028), 0.026, "ct_chrome", sides=12)
        C((cx, cy, DF + 0.028), (cx, cy, DF + 0.042), 0.019, ("ct_lens_on_" if st[key] else "ct_lens_off_") + key,
          sides=12, lit=bool(st[key]))
    # -- pushbuttons: tag over, then the button (guard ring, black head)
    for i, ti in enumerate((21, 22)):
        cx, cy = -0.15 + i * 0.3, 0.76
        T(ti, cx, cy + 0.065, DF, w=0.135, h=0.032)
        C((cx, cy, DF), (cx, cy, DF + 0.01), 0.034, ST, sides=12)
        C((cx, cy, DF + 0.01), (cx, cy, DF + 0.03), 0.03, "ct_chrome", sides=12)
        C((cx, cy, DF + 0.012), (cx, cy, DF + 0.036), 0.022, BK, sides=12)
    return o.ops()


def panel_textures(state=None):
    """Dials, digit windows, lenses, the title tag and the panel's materials."""
    from PIL import ImageFont
    st = state or panel_state()
    out = {}
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"

    def flat(rgb, n=32, seed=1, dirt=0.03, brushed=False):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        if brushed:
            a += np.repeat(r.normal(0, 4, (1, n)), n, 0)[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    out["ct_meter_case"] = flat((24, 25, 27), seed=51)
    out["ct_gasket"] = flat((20, 20, 20), seed=52)
    out["ct_chrome"] = flat((214, 218, 222), seed=53, brushed=True)
    out["ct_needle_red"] = flat((190, 36, 30), seed=54, dirt=0)
    out["ct_horn"] = flat((60, 62, 64), seed=55)
    out["ct_glass"] = Image.new("RGBA", (16, 16), (225, 236, 240, 38))
    for key, label, lo, hi, unit in METERS:
        n = 256
        im = Image.new("RGBA", (n, n), (244, 242, 234, 255))
        d = ImageDraw.Draw(im)
        cx, cy, R = n / 2, n / 2 + 6, 98
        f = ImageFont.truetype(FB, 24)
        for k in range(21):
            a = math.radians(A0 - SWEEP * k / 20)
            big = k % 5 == 0
            r0 = R - (18 if big else 10)
            d.line([(cx + math.cos(a) * r0, cy - math.sin(a) * r0), (cx + math.cos(a) * R, cy - math.sin(a) * R)], fill=(20, 20, 20, 255),
                   width=4 if big else 2)
            if big:
                v = lo + (hi - lo) * k / 20
                d.text((cx + math.cos(a) * (R - 40), cy - math.sin(a) * (R - 40)), f"{v:g}", font=f, fill=(20, 20, 20, 255), anchor="mm")
        d.arc([cx - R, cy - R, cx + R, cy + R], -A0 + 360 - 360, -A0 + SWEEP, fill=(20, 20, 20, 255), width=2)
        fu = ImageFont.truetype(FB, 22 if len(unit) < 4 else 15)
        d.text((cx, cy + (52 if len(unit) < 4 else 30)), unit, font=fu, fill=(40, 40, 40, 255), anchor="mm")
        out["ct_dial_" + key] = im
    fd = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 60)
    for key in ("range", "approach", "wet_bulb"):
        im = Image.new("RGBA", (256, 112), (8, 12, 10, 255))
        d = ImageDraw.Draw(im)
        d.text((236, 58), "88.8", font=fd, fill=(36, 16, 10, 255), anchor="rm")
        v = st[key]
        d.text((236, 58), "--.-" if v is None else f"{v:4.1f}", font=fd, fill=(255, 92, 40, 255), anchor="rm")
        out["ct_digit_" + key] = im
    for key, label, col in LAMPS:
        on = Image.new("RGBA", (32, 32), tuple(col) + (255,))
        ImageDraw.Draw(on).ellipse([6, 5, 15, 13], fill=tuple(min(255, c + 70) for c in col) + (255,))
        out["ct_lens_on_" + key] = on
        out["ct_lens_off_" + key] = Image.new("RGBA", (32, 32), tuple(int(c * 0.33) for c in col) + (255,))
    t = Image.new("RGBA", (640, 64), (198, 202, 204, 255))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, 639, 63], outline=(140, 144, 146, 255), width=2)
    for x in (16, 623):
        d.ellipse([x - 7, 25, x + 7, 39], fill=(160, 164, 166, 255), outline=(110, 112, 114, 255))
    title, size = "LP-CT1A   COOLING TOWER 1A LOCAL PANEL", 30
    while d.textlength(title, font=ImageFont.truetype(FB, size)) > 560:
        size -= 1
    d.text((320, 33), title, font=ImageFont.truetype(FB, size), fill=(36, 38, 40, 255), anchor="mm")
    out["ct_title"] = t
    return out



# ================================================================== piece 4: the stair tower, the bridge and the shell door
#
# Real-world basis: a natural-draft tower's interior (the hot-water distribution level, just above the fill) is reached
# through a steel door in the shell above the ring beam, from a stair tower standing outside the basin and a bridge
# across to the door; the climb to the top goes on by fixed ladder from there (piece 5). US practice (OSHA 1910.25,
# 1910.28, 1910.29): standard stairs between 30 and 50 degrees, risers no more than 9.5 in, treads at least 9.5 in deep,
# at least 22 in wide, no flight rising more than 12 ft between landings, landings at least as deep as the stair is wide;
# handrails 30 to 38 in above the nosings; stair rails and guardrails 42 in (+/- 3) with a mid rail and a toeboard on
# landings and walkways. The tower: galvanised square tube columns on base plates and anchor bolts on a concrete pad,
# X-braced; open-riser grating treads with abrasive yellow nosings between channel stringers; switchback flights. The
# bridge: a galvanised pony truss (Pratt) whose top chord is the top rail, grating deck, lateral bracing under it. The
# door: a hollow-metal door 4 ft by 7 ft in a steel frame, the opening through the shell lined with steel plate, the door
# swinging out (the tower's draft holds an out-swinging door shut), hasp and padlock. Lighting: vapour-tight jar
# fixtures at every landing and over the door, in rigid conduit from a junction box at the foot, switched by a photocell.
#
# This medium tower: deck and sill at 12.6 (a metre over the ring beam's top); five flights of 13 risers of 192 mm on
# 260 mm treads (36.5 degrees), 870 mm clear, landings 1 m deep; the bridge spans 9.7 m from the stair tower to a door
# platform that stands on the ring beam's top ledge.

class Frame:
    """Draws into an Ops in a frame turned to face the tower at angle phi (phi = atan2(x, z)): t across (+t is +x at the
    outlet, the side of rising phi is -t), y up, r out from the axis. Box faces: out (+r), in (-r), left (-t), right
    (+t), top, bottom. Coordinates here are world blocks (x, y, z), not the outlet's (x, y, d)."""

    def __init__(self, o, phi=0.0):
        self.o = o
        self.r = np.array([math.sin(phi), 0.0, math.cos(phi)])
        self.t = np.array([-math.cos(phi), 0.0, math.sin(phi)])
        self.org = np.zeros(3)

    def w(self, t, y, r):
        return self.org + t * self.t + r * self.r + np.array([0.0, y, 0.0])

    def sub(self, t, r, turn):
        """A frame with its origin at (t, r), turned by turn radians (positive turns +t toward +r)."""
        f = Frame(self.o)
        c, s = math.cos(turn), math.sin(turn)
        f.t, f.r = c * self.t + s * self.r, -s * self.t + c * self.r
        f.org = self.w(t, 0.0, r)
        return f

    def _put(self, op, lit):
        (self.o.lit if lit else self.o.still).append(op)

    def box(self, t0, t1, y0, y1, r0, r1, faces, lit=False):
        c = self.w((t0 + t1) / 2, (y0 + y1) / 2, (r0 + r1) / 2)
        self._put(["o", *map(float, c), *map(float, self.t), 0.0, 1.0, 0.0, abs(t1 - t0) / 2, abs(y1 - y0) / 2,
                   abs(r1 - r0) / 2, self.o._faces(faces)], lit)

    def bar(self, a, b, w, th, faces, up="t", lit=False):
        """A rectangular bar from a to b (t, y, r): w across in the plane holding the bar and the up axis's normal,
        th along the up axis (t: the bar lies in a plane of constant t, th is its thickness through that plane)."""
        A, B = self.w(*a), self.w(*b)
        ax = B - A
        L = float(np.linalg.norm(ax))
        ax /= L
        upv = {"t": self.t, "r": self.r, "y": np.array([0.0, 1.0, 0.0])}[up]
        side = np.cross(ax, upv)
        side /= np.linalg.norm(side)
        c = (A + B) / 2
        self._put(["o", *map(float, c), *map(float, side), *map(float, ax), w / 2, L / 2, th / 2,
                   faces if isinstance(faces, list) else [faces] * 6], lit)

    def prism(self, poly, t0, t1, mat):
        """A plate whose outline poly [(r, y) x 4, counter-clockwise seen from +t] is extruded from t0 to t1."""
        P0 = [self.w(t0, y, r) for r, y in poly]
        P1 = [self.w(t1, y, r) for r, y in poly]
        mid = np.mean(P0 + P1, axis=0)
        for k in range(4):
            a, b = k, (k + 1) % 4
            Q = [P0[a], P0[b], P1[b], P1[a]]
            self._quad(Q, mat, np.mean(Q, axis=0) - mid)
        self._quad([P1[0], P1[1], P1[2], P1[3]], mat, self.t)
        self._quad([P0[3], P0[2], P0[1], P0[0]], mat, -self.t)

    def _quad(self, P, mat, facing=None, lit=False):
        """P: four corners round the quad (bl, br, tr, tl, counter-clockwise seen from its front), turned if need be to
        face `facing`. The game draws a quad's front where (tr - tl) x (bl - tl) points."""
        bl, br, tr, tl = [np.asarray(p, float) for p in P]
        loop = [tr, br, bl, tl]                     # Newell's normal (sound for a quad with a corner doubled)
        n = sum(np.cross(loop[k], loop[(k + 1) % 4]) for k in range(4))
        if facing is not None and n @ np.asarray(facing, float) < 0:
            bl, br, tr, tl = br, bl, tl, tr
        self._put(["q", *map(float, tl), *map(float, bl), *map(float, br), *map(float, tr), mat], lit)

    def cyl(self, a, b, rad, mat, cap=None, sides=10, lit=False):
        A, B = self.w(*a), self.w(*b)
        self._put(["c", *map(float, A), *map(float, B), rad, sides, mat, cap if cap is not None else mat], lit)


ACCESS = {"A0": 60.0, "flight": 3.12, "landing": 1.0, "rise": 2.5, "flights": 5, "pad": 0.1, "TC": 1.125, "deck_out": 50.3,
          "plat_right": 2.3}


def access_ops(tower, lights=False, door_open=False):
    """The stair tower, the bridge, the door platform and the shell door with its lining; landing and door lights
    (live: on from dusk to dawn, by the photocell); the door (live: shut and padlocked, or standing open)."""
    o = Ops()
    i0, n, YS, YH = tower.DOORS[0]
    dphi = 2 * math.pi / tower.NPHI
    F = Frame(o, (i0 + n / 2) * dphi)
    GV, DS, G, YE, AL, CI = "ct_galv", "ct_steel_dark", "ct_grating", "ct_yellow", "ct_alum", "ct_cast"
    LAMP = "ct_lamp_on" if lights else "ct_lamp_off"
    DY = YS                                     # the deck: the stair tower's top landing, the bridge, the platform
    A0, RUN, LD = ACCESS["A0"], ACCESS["flight"], ACCESS["landing"]
    RA = (A0, A0 + LD)                          # landing A (the tower's end), landing B (the far end)
    RB = (A0 + LD + RUN, A0 + 2 * LD + RUN)
    Y0 = ACCESS["pad"]
    NF = ACCESS["flights"]
    H = (DY - Y0) / (NF * 13)                   # 13 risers a flight
    TR = RUN / 12                               # 12 treads a flight
    LV = [Y0 + (DY - Y0) * k / NF for k in range(NF + 1)]     # pad, then every landing
    TC, CW = ACCESS["TC"], 0.075                # column centre lines (t), half a column
    TOPA, TOPB = DY + 1.07, LV[NF - 1] + 1.07   # column tops at either end (the top guardrails)
    RC = [RA[0] + CW, RA[1], RB[0], RB[1] - CW]
    PR1 = ACCESS["deck_out"]                    # the door platform's outer edge, where the bridge lands

    def rails(path, yf, opening=None, toe=True, posts=True):
        """Pipe guardrail along a path [(t, r), ...] on a floor at yf: posts (none at the ends if posts is False,
        where columns stand), top rail at 42 in, mid rail at 21 in, toeboard."""
        for k, ((t0, r0), (t1, r1)) in enumerate(zip(path, path[1:])):
            L = math.hypot(t1 - t0, r1 - r0)
            m = max(1, math.ceil(L / 1.8))
            for j in range(m + 1):
                if not posts and (k == 0 and j == 0 or k == len(path) - 2 and j == m):
                    continue
                if j == 0 and k > 0:
                    continue
                f = j / m
                F.cyl((t0 + (t1 - t0) * f, yf, r0 + (r1 - r0) * f), (t0 + (t1 - t0) * f, yf + 1.09, r0 + (r1 - r0) * f), 0.024, GV,
                      sides=8)
            for y in (yf + 1.07, yf + 0.53):
                F.cyl((t0, y, r0), (t1, y, r1), 0.021, GV, sides=8)
            if toe:                              # (after a corner, starting past the last one's thickness)
                c = 0.0031 if k > 0 else 0.0
                if abs(t1 - t0) < 1e-6:
                    a = r0 + math.copysign(c, r1 - r0)
                    F.box(t0 - 0.003, t0 + 0.003, yf, yf + 0.1, min(a, r1), max(a, r1), GV)
                else:
                    a = t0 + math.copysign(c, t1 - t0)
                    F.box(min(a, t1), max(a, t1), yf, yf + 0.1, r0 - 0.003, r0 + 0.003, GV)

    def fixture(t, y, r, toward):
        """A vapour-tight jar light hung off a conduit body at (t, y, r): its cast box, the globe pointing down, the
        guard. toward: +1 or -1, which way along r the nipple runs from the riser to the light."""
        rb = r + toward * 0.27
        F.box(t - 0.05, t + 0.05, y - 0.05, y + 0.05, r - 0.04, r + 0.04, AL)                     # the conduit body
        F.cyl((t, y, r + toward * 0.04), (t, y, rb - toward * 0.05), 0.016, GV, sides=6)          # the nipple
        F.box(t - 0.055, t + 0.055, y - 0.06, y + 0.04, rb - 0.05, rb + 0.05, AL)                 # the light's box
        F.cyl((t, y - 0.06, rb), (t, y - 0.2, rb), 0.045, LAMP, sides=10, lit=lights)             # the globe
        for a in range(4):
            dt, dr = 0.055 * math.cos(math.pi / 4 + a * math.pi / 2), 0.055 * math.sin(math.pi / 4 + a * math.pi / 2)
            F.cyl((t + dt, y - 0.06, rb + dr), (t + dt, y - 0.21, rb + dr), 0.005, CI, sides=4)
        F.cyl((t, y - 0.21, rb), (t, y - 0.222, rb), 0.06, CI, sides=10)

    # ------------------------------------------------------------ the stair tower
    F.box(-1.6, 1.6, -0.3, Y0, RA[0] - 0.45, RB[1] + 0.48, "ct_concrete_flat_0")                  # the pad
    for tc in (-TC, TC):
        for k, rc in enumerate(RC):
            top = TOPA if k < 2 else TOPB
            F.box(tc - 0.15, tc + 0.15, Y0 + 0.001, Y0 + 0.02, rc - 0.15, rc + 0.15, GV)            # base plate
            for a in (-0.1, 0.1):
                for b in (-0.1, 0.1):
                    F.cyl((tc + a, Y0 + 0.02, rc + b), (tc + a, Y0 + 0.05, rc + b), 0.018, GV, sides=6)
            F.box(tc - CW, tc + CW, Y0 + 0.02, top, rc - CW, rc + CW, GV)                           # the column
            F.box(tc - CW - 0.01, tc + CW + 0.01, top, top + 0.012, rc - CW - 0.01, rc + CW + 0.01, GV)
    # landings: channel frame, grating, the yellow nosing where a flight arrives
    for k in range(1, NF + 1):
        L = LV[k]
        at_a = k % 2 == 1                       # odd flights (lane -t) rise toward the tower, landing on A
        r0, r1 = RA if at_a else RB
        hdr = (r1 - 0.06, r1) if at_a else (r0, r0 + 0.06)
        end = (r0, r0 + 0.06) if at_a else (r1 - 0.06, r1)
        for rr in (hdr, end):
            F.box(-1.05, 1.05, L - 0.24, L - 0.04, *rr, DS)
        for s in (-1, 1):
            F.box(*sorted((s * 0.99, s * 1.05)), L - 0.24, L - 0.04, r0 + 0.06, r1 - 0.06, DS)
        F.box(-1.045, 1.045, L - 0.04, L, r0, r1, G)
        lane = (-0.983, -0.117) if at_a else (0.117, 0.983)
        e = r1 if at_a else r0
        sgn = 1 if at_a else -1
        F.box(*lane, L - 0.035, L + 0.003, *sorted((e - sgn * 0.035, e + sgn * 0.002)), YE)
        # guardrails: the far end, and both sides between the columns
        re = RC[0] if at_a else RC[3]
        if k == NF:                              # the top: the bridge leaves between the rails, lane +t is open below
            rails([(-1.05, re), (-0.6, re)], L, posts=False)
            F.cyl((-0.6, L, re), (-0.6, L + 1.09, re), 0.024, GV, sides=8)
            rails([(0.6, re), (1.05, re)], L, posts=False)
            F.cyl((0.6, L, re), (0.6, L + 1.09, re), 0.024, GV, sides=8)
            rails([(0.03, r1), (1.05, r1)], L, posts=False, toe=False)
            F.cyl((0.03, L, r1), (0.03, L + 1.09, r1), 0.024, GV, sides=8)
        else:
            rails([(-1.05, re), (1.05, re)], L, posts=False)
            F.cyl((0.0, L, re), (0.0, L + 1.09, re), 0.024, GV, sides=8)
        for s in (-1, 1):
            ra, rb = (RC[0] + CW, RC[1] - CW) if at_a else (RC[2] + CW, RC[3] - CW)
            for y in (L + 1.07, L + 0.53):
                F.cyl((s * TC, y, ra), (s * TC, y, rb), 0.021, GV, sides=8)
            F.box(*sorted((s * 1.04, s * 1.046)), L, L + 0.1, ra, rb, GV)
    # flights: channel stringers, grating treads with yellow nosings, stair rails and handrails
    for k in range(1, NF + 1):
        y0 = LV[k - 1]
        odd = k % 2 == 1
        s = -1 if odd else 1                    # which way along r the flight climbs
        rs = RB[0] if odd else RA[1]
        ln = (-1.05, -0.05) if odd else (0.05, 1.05)
        side = -1 if odd else 1                 # the lane's outer side
        slope = H / TR
        ynose = lambda r: y0 + H + s * (r - rs) * slope
        cut = y0 if k == 1 else y0 - 0.045       # (on the pad, or 5 mm into the landing's header beam)
        re = rs + s * RUN
        dv = 0.25 / math.cos(math.atan(slope))  # the stringer's depth, measured plumb
        r_toe = rs + s * (cut - 0.07 - y0 - H) / slope
        r_heel = rs + s * (cut + dv - 0.07 - y0 - H) / slope
        poly = [(r_toe, cut), (r_heel, cut), (re, ynose(re) + 0.07 - dv), (re, ynose(re) + 0.07)]
        if s < 0:
            poly = [poly[1], poly[0], poly[3], poly[2]]
        for ta, tb in ((ln[0] + 0.002, ln[0] + 0.067), (ln[1] - 0.067, ln[1] - 0.002)):
            F.prism(poly, ta, tb, DS)
        for j in range(1, 13):
            yt = y0 + j * H
            front = rs + s * (j - 1) * TR - s * 0.025
            back = rs + s * j * TR
            F.box(ln[0] + 0.067, ln[1] - 0.067, yt - 0.032, yt, *sorted((front, back)), G)
            F.box(ln[0] + 0.069, ln[1] - 0.069, yt - 0.03, yt + 0.003, *sorted((front - s * 0.002, front + s * 0.035)), YE)
        # rails: outer posts on the outer stringer's face, top rail 42 in and mid rail over the nosings, a handrail
        # at 34 in on brackets; on the inner side, a handrail on posts
        to = side * 1.071
        ti = side * 0.029
        th = side * 0.975
        for j in (2, 7, 11):
            rp = rs + s * (j - 0.5) * TR
            yb = ynose(rp) + 0.07 - dv + 0.03
            F.cyl((to, yb, rp), (to, ynose(rp) + 1.09, rp), 0.021, GV, sides=8)
            F.cyl((ti, yb, rp), (ti, ynose(rp) + 0.88, rp), 0.021, GV, sides=8)
            F.cyl((to, ynose(rp) + 0.86, rp), (th, ynose(rp) + 0.86, rp), 0.012, GV, sides=6)
        ra, rb = rs + s * 0.5 * TR, re - s * 0.3 * TR
        for y in (1.07, 0.53):
            F.cyl((to, ynose(ra) + y, ra), (to, ynose(rb) + y, rb), 0.021, GV, sides=8)
        F.cyl((th, ynose(ra) + 0.9, ra), (th, ynose(rb) + 0.9, rb), 0.021, GV, sides=8)
        F.cyl((ti, ynose(ra) + 0.9, ra), (ti, ynose(rb) + 0.9, rb), 0.021, GV, sides=8)
    # bracing: ties and X-braces (angles back to back) on the long faces in the stair bay, and on both ends where
    # nothing passes (the far end above the way in)
    for s in (-1, 1):
        for L in LV[1:NF]:
            F.box(*sorted((s * 1.205, s * 1.265)), L - 0.06, L, RC[1] + CW, RC[2] - CW, GV)
        for ya, yb in zip(LV[:NF - 1], LV[1:NF]):
            ya = max(ya, Y0 + 0.3)
            F.bar((s * 1.225, ya, RC[1] + CW), (s * 1.225, yb - 0.06, RC[2] - CW), 0.05, 0.05, GV)
            F.bar((s * 1.275, yb - 0.06, RC[1] + CW), (s * 1.275, ya, RC[2] - CW), 0.05, 0.05, GV)
    for rf, sgn, tiers in ((RA[0], -1, list(zip(LV[:NF - 1], LV[1:NF]))), (RB[1], 1, list(zip(LV[2:NF - 1], LV[3:NF])))):
        for ya, yb in tiers:
            ya = max(ya, Y0 + 0.3)
            F.bar((-TC + CW, ya, rf + sgn * 0.025), (TC - CW, yb - 0.06, rf + sgn * 0.025), 0.05, 0.05, GV, up="r")
            F.bar((TC - CW, ya, rf + sgn * 0.075), (-TC + CW, yb - 0.06, rf + sgn * 0.075), 0.05, 0.05, GV, up="r")
        for L in (LV[2], LV[4]) if sgn < 0 else (LV[3],):
            F.box(-TC + CW, TC - CW, L - 0.06, L, *sorted((rf + sgn * 0.005, rf + sgn * 0.055)), GV)
    F.box(-TC - 0.07, -TC + 0.07, 1.48, 1.515, RB[1], RB[1] + 0.004, {"out": tag(26)})            # the stair's tag
    # ------------------------------------------------------------ the bridge: a Pratt pony truss each side
    span = A0 - PR1
    nodes = [PR1 + span * k / 6 for k in range(7)]
    for s in (-1, 1):
        F.box(*sorted((s * 0.5, s * 0.565)), DY - 0.29, DY - 0.04, PR1, A0, DS)                       # bottom chord
        F.box(*sorted((s * 0.495, s * 0.575)), DY + 0.99, DY + 1.07, PR1, A0, GV)                    # top chord
        for k, rn in enumerate(nodes):
            rn = min(max(rn, PR1 + 0.034), A0 - 0.034)
            F.box(*sorted((s * 0.5025, s * 0.5625)), DY - 0.04, DY + 0.99, rn - 0.03, rn + 0.03, GV)
        for k in range(6):
            a, b = nodes[k], nodes[k + 1]
            a, b = max(a, PR1 + 0.034) + 0.03, min(b, A0 - 0.034) - 0.03     # between the verticals' faces
            if (a + b) / 2 < (PR1 + A0) / 2:
                p, q = (s * 0.5325, DY + 0.99, a), (s * 0.5325, DY - 0.04, b)
            else:
                p, q = (s * 0.5325, DY - 0.04, a), (s * 0.5325, DY + 0.99, b)
            F.bar(p, q, 0.05, 0.05, GV)
            F.cyl((s * 0.5325, DY + 0.53, a + 0.03), (s * 0.5325, DY + 0.53, b - 0.03), 0.018, GV, sides=8)    # mid rail
        F.box(*sorted((s * 0.488, s * 0.494)), DY, DY + 0.1, PR1, A0, GV)                            # toeboard
    for rn in nodes[1:-1]:
        F.box(-0.5, 0.5, DY - 0.115, DY - 0.04, rn - 0.03, rn + 0.03, DS)                              # floor beams
    for k in range(6):
        a, b = nodes[k] + 0.04, nodes[k + 1] - 0.04
        F.bar((-0.5, DY - 0.2, a), (0.5, DY - 0.2, b), 0.04, 0.012, GV, up="y")
        F.bar((0.5, DY - 0.214, a), (-0.5, DY - 0.214, b), 0.04, 0.012, GV, up="y")
    F.box(-0.497, 0.497, DY - 0.04, DY, PR1, A0, G)
    # ------------------------------------------------------------ the door platform on the ring beam's ledge
    # (wider on the +t side, where the fixed ladder to the top starts: piece 5)
    rso = tower.rad(DY)
    TL, TR = -1.1, ACCESS["plat_right"]
    F.box(TL, TR, DY - 0.04, DY, rso + 0.02, PR1, G)

    def at_shell(tt, y):
        """Where an anchor plate's face is, at height y, a distance tt across: 13 mm out from the shell."""
        return math.sqrt(tower.rad(y) ** 2 - tt ** 2) + 0.013

    beams = [((TL, TL + 0.06), PR1), ((-0.565, -0.5), PR1 - 0.06), ((0.5, 0.565), PR1 - 0.06), ((1.04, 1.1), PR1 - 0.06),
             ((TR - 0.06, TR), PR1)]
    for (ta, tb), rend in beams:
        # the beams, cut to the shell's slope against their anchor plates
        tt = (ta + tb) / 2
        F.prism([(at_shell(tt, DY - 0.24), DY - 0.24), (rend, DY - 0.24), (rend, DY - 0.04), (at_shell(tt, DY - 0.04), DY - 0.04)],
                ta, tb, DS)
        y = DY - 0.14
        sl = (tower.rad(y + 0.01) - tower.rad(y - 0.01)) / 0.02
        h = math.hypot(1, sl)
        rs_ = math.sqrt(tower.rad(y) ** 2 - tt ** 2)
        c = (tt, y - sl / h * 0.007, rs_ + 0.007 / h)
        F.bar((c[0], c[1] - 0.16 / h, c[2] - 0.16 * sl / h), (c[0], c[1] + 0.16 / h, c[2] + 0.16 * sl / h), 0.012, 0.12, GV)
    for tp in (TL + 0.03, 1.07, TR - 0.03):
        # posts on the ring beam's top ledge, base plates and anchors
        F.box(tp - 0.03, tp + 0.03, tower.Y_SHELL + 0.016, DY - 0.24, 49.15, 49.25, GV)
        F.box(tp - 0.12, tp + 0.12, tower.Y_SHELL, tower.Y_SHELL + 0.016, 49.05, 49.35, GV)
        for a_ in (-0.09, 0.09):
            for b_ in (49.08, 49.32):
                F.cyl((tp + a_, tower.Y_SHELL + 0.016, b_), (tp + a_, tower.Y_SHELL + 0.045, b_), 0.014, GV, sides=6)
    F.box(TL + 0.06, TR - 0.06, DY - 0.24, DY - 0.04, PR1 - 0.06, PR1, DS)                           # outer beam
    rails([(TL + 0.03, 48.5), (TL + 0.03, PR1 - 0.03), (-0.65, PR1 - 0.03)], DY)
    rails([(TR - 0.03, 48.5), (TR - 0.03, PR1 - 0.03), (0.65, PR1 - 0.03)], DY)
    # the ladder's sign (ANSI Z535 CAUTION) on the rail beside the ladder's foot, facing whoever comes to it
    ts = TR - 0.058
    F.box(ts - 0.004, ts, DY + 0.556, DY + 1.044, 49.0, 49.4, {"left": ["ct_sign_ladder", 0, 0, 1, 1], "right": "ct_sign_back",
          "out": GV, "in": GV, "top": GV, "bottom": GV})
    for y in (DY + 0.53, DY + 1.07):                        # clips round the rails
        for rr_ in (49.06, 49.34):
            F.box(ts - 0.006, TR - 0.008, y - 0.026, y + 0.026, rr_ - 0.015, rr_ + 0.015, GV)
    # ------------------------------------------------------------ the shell door
    shell_door(o, tower, 0, door_open=door_open, padlock=True, tag_i=27)
    # ------------------------------------------------------------ lighting: two risers from junction boxes at the foot
    CR = 0.016
    for tc, rc, lands, toward in ((TC, RC[0], [LV[1], LV[3], LV[5]], 1), (-TC, RC[3], [LV[2], LV[4]], -1)):
        s = 1 if tc > 0 else -1
        tr = tc + s * (CW + 0.022)
        F.box(*sorted((tc + s * CW, tc + s * (CW + 0.1))), 0.55, 0.95, rc - 0.09, rc + 0.09, "ct_jb")
        F.cyl((tr, 0.55, rc), (tr, 0.35, rc), CR, GV, sides=6)                  # out past the base plate, into the pad
        F.cyl((tr, 0.35, rc), (s * 1.45, 0.35, rc), CR, GV, sides=6)
        F.cyl((s * 1.45, 0.35, rc), (s * 1.45, Y0 - 0.2, rc), CR, GV, sides=6)
        top = lands[-1] + 2.3
        F.cyl((tr, 0.95, rc), (tr, top + 0.05, rc), CR, GV, sides=6)
        ctop = TOPA if tc > 0 else TOPB
        F.cyl((tc, ctop + 0.012, rc), (tc, top + 0.25, rc), 0.05, GV, sides=10)                     # a mast on the column
        F.cyl((tc, top + 0.25, rc), (tc, top + 0.262, rc), 0.06, GV, sides=10)
        for y in list(np.arange(1.6, ctop, 1.5)) + [top - 0.4]:
            f = CW if y < ctop else 0.04
            F.box(*sorted((tc + s * f, tr + s * 0.02)), y - 0.02, y + 0.02, rc - 0.025, rc + 0.025, GV)       # straps
        for L in lands:
            fixture(tr, L + 2.3, rc, toward)
        if tc > 0:                              # the photocell on top
            F.cyl((tr, top + 0.05, rc), (tr, top + 0.12, rc), 0.025, CI, sides=8)
            F.cyl((tr, top + 0.12, rc), (tr, top + 0.14, rc), 0.032, "ct_photocell", sides=8)
    # the door light: off riser A along the top landing's end beam, the bridge's chord and the platform's beams, up a pole
    yc = DY - 0.15
    trA = TC + CW + 0.022
    # (under the bridge's chords across the landing's end, then along the -t chord: the +t side is the ladder's)
    yl = DY - 0.35
    F.box(trA - 0.04, trA + 0.04, yl - 0.05, yl + 0.05, RC[0] - 0.04, RC[0] + 0.04, AL)
    rr = A0 - 0.03
    F.cyl((trA, yl, RC[0] - 0.04), (trA, yl, rr), CR, GV, sides=6)
    F.cyl((trA, yl, rr), (-0.585, yl, rr), CR, GV, sides=6)
    F.cyl((-0.585, yl, rr), (-0.585, yc, rr), CR, GV, sides=6)
    F.cyl((-0.585, yc, rr), (-0.585, yc, PR1 + 0.03), CR, GV, sides=6)
    F.cyl((-0.585, yc, PR1 + 0.03), (-1.12, yc, PR1 + 0.03), CR, GV, sides=6)
    F.cyl((-1.12, yc, PR1 + 0.03), (-1.12, yc, 48.75), CR, GV, sides=6)
    for rS in list(np.arange(51.0, 59.9, 1.6)):
        F.box(-0.6, -0.565, yc - 0.022, yc + 0.022, rS - 0.02, rS + 0.02, GV)
    F.cyl((-1.135, DY - 0.24, 48.75), (-1.135, YH - 0.1, 48.75), 0.035, GV, sides=10)               # the pole
    F.cyl((-1.135, YH - 0.1, 48.75), (-1.135, YH - 0.088, 48.75), 0.045, GV, sides=10)
    F.cyl((-1.135, YH - 0.2, 48.75), (-0.62, YH - 0.2, 48.75), 0.02, GV, sides=8)                    # its arm
    F.box(-0.62, -0.53, YH - 0.25, YH - 0.15, 48.7, 48.8, AL)
    F.cyl((-0.575, YH - 0.25, 48.75), (-0.575, YH - 0.39, 48.75), 0.045, LAMP, sides=10, lit=lights)
    F.cyl((-0.575, YH - 0.39, 48.75), (-0.575, YH - 0.4, 48.75), 0.055, CI, sides=10)
    return o.ops()


def shell_door(o, tower, k, door_open=False, padlock=True, tag_i=27):
    """Door k of tower.DOORS: the opening's steel lining (on the cut faces of the surface grid, so it meets the shell
    exactly), the frame in a vertical plane just inside the inner face at the sill, the hollow-metal leaf hinged on the
    +t side and swinging out (the draft holds it shut), lever, hinges, kick plate, tag; the hasp and padlock if
    padlock."""
    i0, n, YS, YH = tower.DOORS[k]
    dphi = 2 * math.pi / tower.NPHI
    F = Frame(o, (i0 + n / 2) * dphi)
    GV, AL = "ct_galv", "ct_alum"

    def grid(y, f):
        g = tower.Y_SHELL + math.floor((y - tower.Y_SHELL) / 0.5 + 1e-9) * 0.5
        u = (y - g) / 0.5
        return f(g) * (1 - u) + f(g + 0.5) * u
    ro = lambda y: grid(y, tower.rad)
    ri = lambda y: grid(y, lambda g: tower.rad(g) - tower.thick(g))
    P = lambda r, y, ph: (r * math.sin(ph), y, r * math.cos(ph))
    ph0, ph1 = i0 * dphi, (i0 + n) * dphi
    mid = F.w(0, (YS + YH) / 2, (ro(YS) + ri(YS)) / 2)
    rows = int(round((YH - YS) / 0.25))
    for ph in (ph0, ph1):
        for j in range(rows):
            ya, yb = YS + 0.25 * j, YS + 0.25 * (j + 1)
            Q = [P(ri(ya), ya, ph), P(ro(ya), ya, ph), P(ro(yb), yb, ph), P(ri(yb), yb, ph)]
            F._quad(Q, GV, mid - np.mean(Q, axis=0))
    for j in range(n):
        pa, pb = ph0 + j * dphi, ph0 + (j + 1) * dphi
        for y, up in ((YS, 1), (YH, -1)):
            F._quad([P(ri(y), y, pa), P(ro(y), y, pa), P(ro(y), y, pb), P(ri(y), y, pb)], GV, (0, up, 0))
    ys = [YS + 0.25 * j for j in range(rows + 1)]
    RD = max(ri(y) for y in ys) + 0.052          # the door's plane: its frame (RD +/- 0.05) inside the concrete throughout
    assert RD + 0.05 < min(ro(y) for y in ys), "the shell is too thin here for the frame"
    hw = RD * math.tan(n * dphi / 2) - 0.001     # half the opening at the frame
    lh = hw - 0.076                              # half the leaf
    fr = "ct_door_frame"
    for s in (-1, 1):
        F.box(*sorted((s * (lh + 0.002), s * hw)), YS, YH - 0.001, RD - 0.05, RD + 0.05, {"out": fr, "in": fr,
              "left" if s > 0 else "right": fr, "top": fr})
    F.box(-lh - 0.002, lh + 0.002, YH - 0.1, YH - 0.001, RD - 0.05, RD + 0.05, {"out": fr, "in": fr, "bottom": fr})
    F.box(-lh - 0.002, lh + 0.002, YS, YS + 0.018, RD - 0.045, RD + 0.15, AL)                          # threshold
    W = 2 * lh
    D = F.sub(lh, RD + 0.045, -math.pi / 2 if door_open else 0.0)
    D.box(-W, 0.0, YS + 0.025, YH - 0.105, -0.045, 0.0, "ct_door")
    D.box(-W + 0.04, -0.04, YS + 0.045, YS + 0.25, 0.0, 0.002, "ct_stainless")                       # kick plate
    D.box(-lh - 0.15, -lh + 0.15, YS + 1.35, YS + 1.425, 0.0, 0.003, {"out": tag(tag_i)})
    D.cyl((-W + 0.1, YS + 1.0, 0.0), (-W + 0.1, YS + 1.0, 0.016), 0.03, "ct_stainless", sides=10)      # lever handle
    D.box(-W + 0.095, -W + 0.24, YS + 0.988, YS + 1.012, 0.016, 0.04, "ct_stainless")
    for y in (YS + 0.33, (YS + YH) / 2, YH - 0.3):
        D.cyl((0.004, y - 0.06, 0.012), (0.004, y + 0.06, 0.012), 0.013, GV, sides=8)
        D.box(-0.09, -0.004, y - 0.05, y + 0.05, 0.0, 0.004, GV)
    if padlock:
        # the hasp on the leaf, the staple on the frame and the padlock (shut through the hasp, or hung on the staple
        # while the door is open)
        D.box(-W - 0.04, -W + 0.1, YS + 0.67, YS + 0.75, 0.0, 0.006, GV)
        F.box(-lh - 0.06, -lh - 0.015, YS + 0.69, YS + 0.73, RD + 0.05, RD + 0.08, GV)
        F.box(-lh - 0.06, -lh - 0.005, YS + 0.53, YS + 0.62, RD + 0.065, RD + 0.095, "ct_brass")
        for tt in (-lh - 0.05, -lh - 0.015):
            F.cyl((tt, YS + 0.62, RD + 0.08), (tt, YS + 0.71, RD + 0.08), 0.006, "ct_stainless", sides=6)
    return F


def access_textures():
    """Piece 4's own materials: the door and its frame, the light globes, the photocell."""
    out = {}

    def flat(rgb, seed, n=32, dirt=0.03):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    out["ct_door"] = flat((148, 156, 150), 61, n=64, dirt=0.025)          # hollow metal, factory grey primer
    out["ct_door_frame"] = flat((128, 134, 130), 62)
    out["ct_lamp_on"] = Image.new("RGBA", (16, 16), (255, 238, 196, 255))
    out["ct_lamp_off"] = flat((186, 194, 196), 63, n=16)
    out["ct_photocell"] = flat((60, 64, 70), 64, n=16)
    # ANSI Z535.2 CAUTION sign at the ladder's foot: safety alert symbol and CAUTION in black on yellow, the message
    from PIL import ImageFont
    FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
    FC = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
    sg = Image.new("RGBA", (400, 490), (255, 255, 255, 255))
    d = ImageDraw.Draw(sg)
    d.rectangle([0, 0, 399, 489], outline=(0, 0, 0, 255), width=8)
    d.rectangle([8, 8, 391, 96], fill=(250, 204, 0, 255))
    d.polygon([(40, 82), (68, 26), (96, 82)], fill=(0, 0, 0, 255))
    d.text((68, 68), "!", font=ImageFont.truetype(FB, 34), fill=(250, 204, 0, 255), anchor="mm")
    d.text((245, 54), "CAUTION", font=ImageFont.truetype(FB, 50), fill=(0, 0, 0, 255), anchor="mm")
    for k, line in enumerate(("FALL PROTECTION", "REQUIRED", "", "ATTACH SLEEVE TO", "LADDER SAFETY RAIL", "BEFORE CLIMBING")):
        if line:
            d.text((200, 150 + k * 54), line, font=ImageFont.truetype(FC, 36), fill=(0, 0, 0, 255), anchor="mm")
    out["ct_sign_ladder"] = sg
    return out



# ================================================================== piece 5: the fixed ladder to the top
#
# Real-world basis: the climb to the rim goes by fixed ladder from the shell door's platform: up the outside of the
# shell to the throat (below it the shell leans in, so a ladder against it leans away from the climber like an ordinary
# ladder), through the shell at the throat by a crossover door, and up the inside to the rim (above the throat the shell
# flares out, so the ladder goes inside to keep leaning away from the climber); a walkway round the rim on the cornice
# for the obstruction lights. US rules (OSHA 1910.23 and 1910.28): fixed ladders pitched 75 to 90 degrees; rungs at
# least 3/4 in across, 10 to 14 in apart and evenly spaced, at least 16 in clear between the side rails; at least 7 in
# from the rungs' centreline to the structure behind; side rails extending 42 in above the landing; a ladder over 24 ft
# has a ladder safety system (a rigid rail the climber's sleeve rides; cages no longer count since 2018), with rest
# platforms no more than 150 ft apart; landings offset from the ladder (side-step), their openings closed by
# self-closing gates; platforms guarded with 42 in rails, mid rails and toeboards. This tower: 18 in clear between
# rails, 1 in rungs at 12 in, the ladder 0.25 m off the shell at its ends, standoff brackets every 2.4 m anchored to
# the shell with plates; rest platforms at 37.6, 62.6 and 88.6 m, the crossover at the throat (114.6 m) and one more
# inside at 132.6 m; the rim walkway is the cornice's top, 1.15 m wide, railed both sides.

LADDER = {"half": 0.2345, "pitch": 0.305, "off": 0.25, "sep": 0.026, "start_t": 1.65}


def _wall(tower, y, t, inside):
    """The shell's face at height y (its outer face, or its inner one, the cornice's above the shell's top), as r along
    a frame's axis a distance t across."""
    if inside:
        R = tower.R_IN if y > tower.Y_TOP - 0.3 else tower.rad(y) - tower.thick(y)
    else:
        R = tower.rad(y)
    return math.sqrt(R * R - t * t)


def _rails(F, path, yf, posts=True, toe=True, mat="ct_galv"):
    """Pipe guardrail along a path [(t, r), ...] on a floor at yf: posts (none at the ends if posts is False), top rail
    at 42 in, mid rail at 21 in, toeboard."""
    for k, ((t0, r0), (t1, r1)) in enumerate(zip(path, path[1:])):
        L = math.hypot(t1 - t0, r1 - r0)
        m = max(1, math.ceil(L / 1.8))
        for j in range(m + 1):
            if not posts and (k == 0 and j == 0 or k == len(path) - 2 and j == m):
                continue
            if j == 0 and k > 0:
                continue
            f = j / m
            F.cyl((t0 + (t1 - t0) * f, yf, r0 + (r1 - r0) * f), (t0 + (t1 - t0) * f, yf + 1.09, r0 + (r1 - r0) * f), 0.024, mat, sides=8)
        for y in (yf + 1.07, yf + 0.53):
            F.cyl((t0, y, r0), (t1, y, r1), 0.021, mat, sides=8)
        if toe:
            c = 0.0031 if k > 0 else 0.0
            if abs(t1 - t0) < 1e-6:
                a = r0 + math.copysign(c, r1 - r0)
                F.box(t0 - 0.003, t0 + 0.003, yf, yf + 0.1, min(a, r1), max(a, r1), mat)
            else:
                a = t0 + math.copysign(c, t1 - t0)
                F.box(min(a, t1), max(a, t1), yf, yf + 0.1, r0 - 0.003, r0 + 0.003, mat)


def _plate(F, tower, t, y, inside, w=0.1, h=0.16, bolts=True):
    """An anchor plate bolted flat to the shell's face at (t, y), following its slope."""
    sg = -1 if inside else 1
    sl = (_wall(tower, y + 0.01, t, inside) - _wall(tower, y - 0.01, t, inside)) / 0.02
    hh = math.hypot(1, sl)
    nr, ny = sg / hh, -sg * sl / hh                  # out of the concrete
    r = _wall(tower, y, t, inside)
    c = (t, y + ny * 0.007, r + nr * 0.007)
    F.bar((t, c[1] - h / 2 / hh, c[2] - h / 2 * sl / hh), (t, c[1] + h / 2 / hh, c[2] + h / 2 * sl / hh), 0.012, w, "ct_galv")
    if bolts:
        for k in (-1, 1):
            p = (t, c[1] + k * h * 0.33 / hh + ny * 0.006, c[2] + k * h * 0.33 * sl / hh + nr * 0.006)
            F.cyl(p, (t, p[1] + ny * 0.012, p[2] + nr * 0.012), 0.011, "ct_galv", sides=6)


def fixed_ladder(F, tower, ya, yb, ra, rb, inside=False, ext=1.07):
    """A fixed ladder in frame F's plane t = 0, from its foot at ya (on a deck) to the landing at yb, its rungs'
    centreline from r ra (at ya) to rb (at yb): side rails running on 42 in above the landing, rungs every 12 in up to
    the landing, the ladder safety system's rigid rail on the climbing side clamped to the rungs, standoff brackets
    every 2.4 m on anchor plates. Returns the least clearance from the rungs to the shell (at least 7 in)."""
    GV = "ct_galv"
    sg = -1 if inside else 1
    rl = lambda y: ra + (rb - ra) * (y - ya) / (yb - ya)
    hw = LADDER["half"]
    top = yb + ext
    for s in (-1, 1):
        F.bar((s * hw, ya, rl(ya)), (s * hw, top, rl(top)), 0.064, 0.012, GV)
    least = 9.0
    k = 1
    while ya + k * LADDER["pitch"] <= yb + 0.001:
        y = ya + k * LADDER["pitch"]
        F.cyl((-hw + 0.006, y, rl(y)), (hw - 0.006, y, rl(y)), 0.0127, GV, sides=8)
        least = min(least, abs(rl(y) - _wall(tower, y, 0.0, inside)))
        if k % 4 == 2:                                  # the safety rail's clamps
            F.bar((0.0, y, rl(y)), (0.0, y, rl(y) + sg * 0.048), 0.05, 0.03, GV)
        k += 1
    F.bar((0.0, ya + 0.25, rl(ya + 0.25) + sg * 0.06), (0.0, top, rl(top) + sg * 0.06), 0.025, 0.032, GV)
    y = ya + 1.2
    while y < yb - 0.3:
        for s in (-1, 1):                               # bolted to the rail's outer side
            tb = s * (hw + 0.012)
            rw = _wall(tower, y, tb, inside)
            F.bar((tb, y, rl(y) - sg * 0.02), (tb, y, rw + sg * 0.013), 0.05, 0.012, GV)
            _plate(F, tower, tb, y, inside)
        y += 2.4
    return least


def _deck(F, tower, y, ta, tb, depth, inside, gate=None, wall_rail=False):
    """A platform against the shell at deck height y across [ta, tb], depth out from the shell (inward if inside): a
    grating deck on channels whose ends are cut to the shell's slope and anchored, knee braces down to the shell, a
    guardrail round its open sides; gate ("a" or "b"): that side's opening by the wall for a side-stepping climber,
    closed by a self-closing gate. Returns (the deck's edge by the wall, its outer edge)."""
    GV, DS, G = "ct_galv", "ct_steel_dark", "ct_grating"
    sg = -1 if inside else 1
    ts = [ta, tb] + ([0.0] if ta < 0 < tb else [])
    walls = [_wall(tower, yy, t, inside) for t in ts for yy in (y, y - 0.04)]
    rn = (max(walls) if not inside else min(walls)) + sg * 0.04
    ro = rn + sg * depth
    F.box(ta, tb, y - 0.04, y, *sorted((rn, ro)), G)
    lines = [(ta, ta + 0.06), (tb - 0.06, tb)]
    if tb - ta > 1.4:
        c = (ta + tb) / 2
        lines.append((c - 0.03, c + 0.03))
    for k, (a, b) in enumerate(lines):
        tt = (a + b) / 2
        re = ro if k < 2 else ro - sg * 0.06            # (the middle one ends at the outer beam)
        poly = [(_wall(tower, y - 0.24, tt, inside) + sg * 0.013, y - 0.24), (re, y - 0.24), (re, y - 0.04),
                (_wall(tower, y - 0.04, tt, inside) + sg * 0.013, y - 0.04)]
        F.prism(poly, a, b, DS)
        _plate(F, tower, tt, y - 0.14, inside)
        yb = y - 1.15
        F.bar((tt, y - 0.24, ro - sg * 0.12), (tt, yb, _wall(tower, yb, tt, inside) + sg * 0.03), 0.06, 0.05, DS)   # knee brace
        _plate(F, tower, tt, yb, inside)
    F.box(ta + 0.06, tb - 0.06, y - 0.24, y - 0.04, *sorted((ro - sg * 0.06, ro)), DS)
    ra, rb_ = ro - sg * 0.03, rn + sg * 0.05
    path = [(ta + 0.03, rn + sg * (0.75 if gate == "a" else 0.05)), (ta + 0.03, ra), (tb - 0.03, ra),
            (tb - 0.03, rn + sg * (0.75 if gate == "b" else 0.05))]
    _rails(F, path, y)
    if gate:
        tg = ta + 0.03 if gate == "a" else tb - 0.03
        r0, r1 = rn + sg * 0.09, rn + sg * 0.71
        for yy in (y + 0.53, y + 1.0):
            F.cyl((tg, yy, r0), (tg, yy, r1), 0.017, "ct_yellow", sides=8)
        for rr in (r0, r1):
            F.cyl((tg, y + 0.15, rr), (tg, y + 1.0, rr), 0.017, "ct_yellow", sides=8)
        F.box(tg - 0.03, tg + 0.03, y + 0.45, y + 0.6, *sorted((r1 + sg * 0.012, r1 + sg * 0.03)), DS)       # spring hinge
        F.box(tg - 0.03, tg + 0.03, y + 0.9, y + 1.0, *sorted((r1 + sg * 0.012, r1 + sg * 0.03)), DS)
    return rn, ro


def ladder_ops(tower, crossover_open=False):
    """The fixed ladder from the shell door's platform to the rim (piece 5): four flights up the outside with rest
    platforms, the crossover through the shell at the throat, two flights up the inside, the rim walkway.
    crossover_open: the crossover door standing open (live, in game)."""
    o = Ops()
    dphi = 2 * math.pi / tower.NPHI
    i0, n, YS, _ = tower.DOORS[0]
    j0, m, YT, _ = tower.DOORS[1]
    ph1 = (i0 + n / 2) * dphi
    ph2 = (j0 + m / 2) * dphi
    SEP = LADDER["sep"]
    phA = ph1 - math.atan(LADDER["start_t"] / (tower.rad(YS) + LADDER["off"]))
    phB = phA - SEP
    phC = ph2 - 1.0 / 30.6
    phD = phC + SEP
    out_levels = [YS, 37.6, 62.6, 88.6, YT]
    least = []
    off = LADDER["off"]
    # outside: four flights alternating A, B, A, B; rest platforms between
    for k in range(4):
        ya, yb = out_levels[k], out_levels[k + 1]
        ph = phA if k % 2 == 0 else phB
        FL = Frame(o, ph)
        least.append(fixed_ladder(FL, tower, ya, yb, _wall(tower, ya, 0, False) + off, _wall(tower, yb, 0, False) + off))
    for k, y in enumerate(out_levels[1:4]):
        arr, dep = (phA, phB) if k % 2 == 0 else (phB, phA)
        pm = (arr + dep) / 2
        FP = Frame(o, pm)
        r = tower.rad(y) + off
        ta_, td_ = r * math.sin(pm - arr), r * math.sin(pm - dep)
        if ta_ < td_:
            _deck(FP, tower, y, ta_ + 0.42, td_ + 0.65, 1.0, False, gate="a")
        else:
            _deck(FP, tower, y, td_ - 0.65, ta_ - 0.42, 1.0, False, gate="b")
    # the crossover: a platform outside, the door, a platform inside
    F2 = Frame(o, ph2)
    rB = (tower.rad(YT) + off) * math.sin(ph2 - phB)
    _deck(F2, tower, YT, rB + 0.42, 0.8, 1.0, False, gate="a")
    shell_door(o, tower, 1, door_open=crossover_open, padlock=False, tag_i=28)
    tC = (tower.rad(YT) - tower.thick(YT) - off) * math.sin(ph2 - phC)
    _deck(F2, tower, YT, -0.62, tC + 0.6, 1.1, True)
    # inside: two flights, C then D, a rest platform between, D arriving at the rim through the walkway's gate
    yi = 132.6
    FC = Frame(o, phC)
    least.append(fixed_ladder(FC, tower, YT, yi, _wall(tower, YT, 0, True) - 0.4, _wall(tower, yi, 0, True) - 0.4, inside=True))
    FD = Frame(o, phD)
    YC = tower.Y_CTOP
    least.append(fixed_ladder(FD, tower, yi, YC, _wall(tower, yi, 0, True) - 0.4, tower.R_IN - off, inside=True))
    pm = (phC + phD) / 2
    FP = Frame(o, pm)
    r = tower.rad(yi) - tower.thick(yi) - 0.4
    tc_, td_ = r * math.sin(pm - phC), r * math.sin(pm - phD)
    _deck(FP, tower, yi, td_ - 0.65, tc_ - 0.42, 1.0, True, gate="b")
    # the rim walkway: the cornice's top, railed both sides; the inner rail's gate where ladder D comes up
    GV = "ct_galv"
    RI_, RO_ = tower.R_IN + 0.06, tower.RTP + 0.55 - 0.06
    gap = 0.7 / RI_

    def rim_line(R, a0, a1, nseg, ring):
        pts = [a0 + (a1 - a0) * k / nseg for k in range(nseg + 1)]
        for k, a in enumerate(pts):
            if ring and k == nseg:
                break
            Fk = Frame(o, a)
            Fk.cyl((0, YC + 0.013, R), (0, YC + 1.09, R), 0.024, GV, sides=8)
            Fk.box(-0.06, 0.06, YC + 0.001, YC + 0.013, R - 0.06, R + 0.06, GV)
            for b in (-0.04, 0.04):
                Fk.cyl((b, YC + 0.013, R - 0.04), (b, YC + 0.035, R - 0.04), 0.011, GV, sides=6)
                Fk.cyl((b, YC + 0.013, R + 0.04), (b, YC + 0.035, R + 0.04), 0.011, GV, sides=6)
        for a, b in zip(pts, pts[1:]):
            Fm = Frame(o, (a + b) / 2)
            hc = R * math.sin((b - a) / 2)
            rc = R * math.cos((b - a) / 2)
            for yy in (YC + 1.07, YC + 0.53):
                Fm.cyl((-hc, yy, rc), (hc, yy, rc), 0.021, GV, sides=8)
            Fm.box(-hc + 0.02, hc - 0.02, YC + 0.002, YC + 0.1, rc - 0.003, rc + 0.003, GV)
    rim_line(RO_, 0.0, 2 * math.pi, 120, True)
    a0, a1 = phD + gap / 2, phD - gap / 2 + 2 * math.pi
    rim_line(RI_, a0, a1, int(math.ceil((a1 - a0) * RI_ / 1.8)), False)
    # the gate, and ties from the ladder's rails to the gate's posts
    hg = RI_ * math.sin(gap / 2)
    rg = RI_ * math.cos(gap / 2)
    for yy in (YC + 0.53, YC + 1.0):
        FD.cyl((-hg + 0.06, yy, rg), (hg - 0.06, yy, rg), 0.017, "ct_yellow", sides=8)
    for tt in (-hg + 0.06, hg - 0.06):
        FD.cyl((tt, YC + 0.15, rg), (tt, YC + 1.0, rg), 0.017, "ct_yellow", sides=8)
    FD.box(hg - 0.05, hg - 0.03, YC + 0.45, YC + 0.6, rg - 0.03, rg + 0.03, "ct_steel_dark")
    FD.box(hg - 0.05, hg - 0.03, YC + 0.88, YC + 1.0, rg - 0.03, rg + 0.03, "ct_steel_dark")
    rtop = tower.R_IN - off
    for s in (-1, 1):
        FD.cyl((s * LADDER["half"], YC + 0.9, rtop + 0.01), (s * hg, YC + 0.9, rg), 0.012, GV, sides=6)
    LADDER["least"] = least                      # each flight's least rung clearance (checked: at least 7 in)
    return o.ops()



# ================================================================== piece 6: obstruction lights and lightning protection
#
# Real-world basis. Obstruction lights (FAA AC 70/7460-1M): a structure about 490 ft high gets red flashing L-864 lights
# at night, and to leave the concrete unpainted, medium-intensity white L-865 by day and twilight (dual lighting:
# "other methods of marking may be omitted" up to 700 ft); high-intensity white L-856 is not recommended at or below
# 700 ft. Chimneys and cooling towers: the top level may sit as low as 20 ft below the top; cooling towers up to 600 ft
# need no intermediate level; at least four lights a level (the AC's count for 100 ft across; this rim is 220 ft across,
# so eight, about 27 m apart); all flash together, 40 a minute; a photocell switches day (white, 20,000 cd), twilight
# (white) and night (red, 2,000 cd); failures alarm (OBST LT FAIL on LP-CT1A). Here: eight dual LED beacons on
# pedestals on the rim walkway, their heads above the outer rail, fed in rigid conduit round the rim and down the shell
# to a junction box at the shell door, then along the bridge and down the stair tower to the controller at its foot.
# Lightning protection (NFPA 780's heavy-duty stack rules taken as the model): air terminals round the top no more than
# 8 ft apart standing 18 to 30 in above it, a loop conductor joining them, at least two down conductors equally spaced,
# loop conductors at no more than 200 ft, conductors fastened every 4 ft and held off the concrete, lead-coated copper in
# the top 25 ft, a guard over the bottom of each down conductor, a test joint, the ground ring with electrodes; metal
# (ladder, conduit) bonded where conductors pass.

TOP6 = {"beacons": 8, "terminals": 90, "loops": (60.0, 105.0), "footings": (28, 60)}


def obstruction_ops(tower, sky="day", flash=True):
    """The dual obstruction lights, their conduit and controller, and the lightning protection (piece 6). sky: day,
    twilight or night (live, from the photocell); flash: whether this frame is in a flash (40 a minute, all
    together)."""
    o = Ops()
    GV, CU, PB = "ct_galv", "ct_copper", "ct_lead"
    dphi = 2 * math.pi / tower.NPHI
    i0, n, YS, _ = tower.DOORS[0]
    ph1 = (i0 + n / 2) * dphi
    YC, RO_ = tower.Y_CTOP, tower.RTP + 0.55 - 0.06
    REDGE = tower.RTP + 0.55                       # the cornice's outer face
    j0, m_, _, _ = tower.DOORS[1]
    phC = (j0 + m_ / 2) * dphi - 1.0 / 30.6
    phD = phC + LADDER["sep"]
    lens = ("ct_beacon_red" if sky == "night" else "ct_beacon_white") if flash else "ct_beacon_lens"

    def meridian(F, y0, y1, off, step=1.0):
        """Points (t=0) down the shell's outer face from y0 to y1, held off it by off along its normal."""
        ys = list(np.arange(y0, y1, -step)) + [y1]
        pts = []
        for y in ys:
            sl = (tower.rad(y + 0.01) - tower.rad(y - 0.01)) / 0.02
            hh = math.hypot(1, sl)
            pts.append((0.0, y - sl * off / hh, tower.rad(y) + off / hh))
        return pts

    def run(F, pts, rad, mat, clamp=None):
        """A conductor or conduit through pts; clamp (every 1.2 m): a fastener back to the surface at -off along r."""
        for a, b in zip(pts, pts[1:]):
            F.cyl(a, b, rad, mat, sides=6)
        for a, b in zip(pts, pts[1:]):
            if not clamp:
                continue
            L = math.dist(a, b)
            k = 0.6
            while k < L:
                f = k / L
                p = tuple(a[j] + (b[j] - a[j]) * f for j in range(3))
                F.cyl(p, (p[0], p[1], p[2] - clamp), 0.006, GV, sides=4)
                k += 1.2

    # ------------------------------------------------------------ beacons, the rim conduit
    N = 120
    rr = RO_ - 0.045                               # the conduit just inside the outer rail's posts
    ring = [(rr * math.sin(2 * math.pi * k / N), YC + 0.25, rr * math.cos(2 * math.pi * k / N)) for k in range(N + 1)]
    for a, b in zip(ring, ring[1:]):
        o.still.append(["c", *a, *b, 0.016, 6, GV, GV])
    for k in range(N):
        Fk = Frame(o, 2 * math.pi * k / N)
        Fk.cyl((0, YC + 0.25, rr), (0, YC + 0.25, RO_ - 0.02), 0.012, GV, sides=6)          # clamp to the post
    phase = phD + math.pi / 8
    for k in range(TOP6["beacons"]):
        Fb = Frame(o, phase + 2 * math.pi * k / TOP6["beacons"])
        rp = RO_ - 0.42
        Fb.box(-0.12, 0.12, YC + 0.001, YC + 0.016, rp - 0.12, rp + 0.12, GV)                    # base plate
        for a in (-0.09, 0.09):
            for b in (-0.09, 0.09):
                Fb.cyl((a, YC + 0.016, rp + b), (a, YC + 0.04, rp + b), 0.011, GV, sides=6)
        Fb.cyl((0, YC + 0.016, rp), (0, YC + 1.12, rp), 0.05, GV, sides=12)                     # pedestal
        Fb.cyl((0, YC + 1.12, rp), (0, YC + 1.15, rp), 0.12, GV, sides=16)
        Fb.cyl((0, YC + 1.15, rp), (0, YC + 1.29, rp), 0.2, "ct_beacon_body", sides=20)        # the beacon
        Fb.cyl((0, YC + 1.29, rp), (0, YC + 1.47, rp), 0.185, lens, sides=20, lit=flash)
        Fb.cyl((0, YC + 1.47, rp), (0, YC + 1.51, rp), 0.2, "ct_beacon_body", sides=20)
        Fb.cyl((0, YC + 1.51, rp), (0, YC + 1.53, rp), 0.08, "ct_beacon_body", sides=12)
        ab = (phase + 2 * math.pi * k / TOP6["beacons"]) % (2 * math.pi / N) - math.pi / N
        ra = rr * math.cos(math.pi / N) / math.cos(ab)                                            # the rim conduit there
        Fb.cyl((0, YC + 0.25, ra), (0, YC + 0.25, rp + 0.05), 0.012, GV, sides=6)               # its cable, into the pedestal
    # ------------------------------------------------------------ down to the shell door, the bridge, the controller
    phS = ph1 + math.atan(0.95 / 48.45)
    FS = Frame(o, phS)
    lip_y = lambda r: tower.Y_TOP + (r - tower.RTP) * (0.15 / 0.55)
    top = [(0, YC + 0.25, rr * math.cos(math.pi / N)), (0, YC + 0.25, REDGE + 0.16), (0, lip_y(REDGE) - 0.15, REDGE + 0.16),
           (0, lip_y(tower.RTP + 0.1) - 0.17, tower.RTP + 0.1)]
    down = meridian(FS, tower.Y_TOP - 0.2, 14.0, 0.1)
    run(FS, top + down, 0.016, GV)
    run(FS, down, 0.016, GV, clamp=0.1)
    yj = (13.3, 14.0)
    rj = tower.rad(13.3) + 0.03
    FS.box(-0.15, 0.15, *yj, rj, rj + 0.16, "ct_jb")                                                # the junction box
    for y in (13.4, 13.9):
        FS.bar((0, y, tower.rad(y) + 0.013), (0, y, rj), 0.04, 0.2, GV)
    F = Frame(o, ph1)
    yu = DYU = YS - 0.27
    F.cyl((-0.95, 13.3, rj + 0.08), (-0.95, DYU, rj + 0.08), 0.016, GV, sides=6)               # down through the grating
    PR1 = ACCESS["deck_out"]
    path = [(-0.95, DYU, rj + 0.08), (-0.95, DYU, PR1 + 0.03), (-0.585, DYU, PR1 + 0.03), (-0.585, DYU, ACCESS["A0"] - 0.1),
            (-ACCESS["TC"] - 0.097, DYU, ACCESS["A0"] - 0.1), (-ACCESS["TC"] - 0.097, DYU, ACCESS["A0"] + 0.075),
            (-ACCESS["TC"] - 0.097, 1.8, ACCESS["A0"] + 0.075)]
    for a, b in zip(path, path[1:]):
        F.cyl(a, b, 0.016, GV, sides=6)
    for rS in np.arange(51.8, 59.6, 1.6):
        F.box(-0.6, -0.565, DYU - 0.02, DYU + 0.02, rS - 0.02, rS + 0.02, GV)
    tcol = -ACCESS["TC"]
    for y in np.arange(2.6, DYU - 0.2, 1.5):
        F.box(tcol - 0.075 - 0.04, tcol - 0.075, y - 0.02, y + 0.02, ACCESS["A0"] + 0.05, ACCESS["A0"] + 0.1, GV)
    # the controller: a stainless enclosure on the stair tower's column, its photocell on top, its feed into the pad
    rc = ACCESS["A0"] + 0.075
    F.box(tcol - 0.075 - 0.25, tcol - 0.075, 0.8, 1.8, rc - 0.22, rc + 0.22, "ct_stainless")
    F.box(tcol - 0.075 - 0.254, tcol - 0.075 - 0.25, 1.55, 1.6, rc - 0.15, rc + 0.15, {"left": tag(29)})
    F.cyl((tcol - 0.2, 1.8, rc - 0.12), (tcol - 0.2, 1.88, rc - 0.12), 0.02, GV, sides=8)
    F.cyl((tcol - 0.2, 1.88, rc - 0.12), (tcol - 0.2, 1.9, rc - 0.12), 0.03, "ct_photocell", sides=8)
    F.cyl((tcol - 0.25, 0.8, rc + 0.12), (tcol - 0.25, -0.2, rc + 0.12), 0.02, GV, sides=6)
    # ------------------------------------------------------------ lightning protection
    # air terminals round the top on the cornice's outer face, the top loop joining them
    NT = TOP6["terminals"]
    RT = REDGE + 0.06
    for k in range(NT):
        Ft = Frame(o, 2 * math.pi * (k + 0.5) / NT)
        Ft.cyl((0, YC - 0.45, RT), (0, YC + 0.6, RT), 0.008, PB, sides=6)
        for y in (YC - 0.4, YC - 0.08):
            Ft.cyl((0, y, REDGE), (0, y, RT), 0.007, GV, sides=4)
    lp = [((RT - 0.006) * math.sin(2 * math.pi * (k + 0.5) / NT), YC - 0.35, (RT - 0.006) * math.cos(2 * math.pi * (k + 0.5) / NT))
          for k in range(NT + 1)]
    for a, b in zip(lp, lp[1:]):
        o.still.append(["c", *a, *b, 0.008, 6, PB, PB])
    # loop conductors round the shell, bonded to the ladder and the conduit where they pass
    for yl in TOP6["loops"]:
        sl = (tower.rad(yl + 0.01) - tower.rad(yl - 0.01)) / 0.02
        R = tower.rad(yl) + 0.05 / math.hypot(1, sl)
        NL = int(2 * math.pi * R / 1.2)
        pts = [(R * math.sin(2 * math.pi * k / NL), yl, R * math.cos(2 * math.pi * k / NL)) for k in range(NL + 1)]
        for k, (a, b) in enumerate(zip(pts, pts[1:])):
            o.still.append(["c", *a, *b, 0.008, 6, CU, CU])
            Fk = Frame(o, 2 * math.pi * k / NL)
            Fk.cyl((0, yl, tower.rad(yl)), (0, yl, R), 0.006, GV, sides=4)
        # bonds: the conduit (at phS, 0.1 off) and the ladder's rail (flight B passes both loops)
        FS.cyl((0, yl, R * math.cos(0) + 0.0), (0, yl, tower.rad(yl) + 0.1), 0.006, CU, sides=4)
        phB = ph1 - math.atan(LADDER["start_t"] / (tower.rad(YS) + LADDER["off"])) - LADDER["sep"]
        FB = Frame(o, phB)
        FB.cyl((LADDER["half"] + 0.03, yl, R), (LADDER["half"] + 0.006, yl, tower.rad(yl) + LADDER["off"] - 0.02), 0.006, CU, sides=4)
    # down conductors: from the top loop down the cornice's face, under its drip lip, down the shell, over the ring
    # beam, down a column and its footing, over the curb into the ground; guarded at the bottom, a test joint
    for i in TOP6["footings"]:
        ph = 2 * math.pi * i / tower.NB
        d = ph + math.pi / tower.NB
        Fd = Frame(o, d)
        head = [(0, YC - 0.35, RT - 0.006), (0, lip_y(REDGE) - 0.06, RT - 0.006), (0, lip_y(tower.RTP + 0.05) - 0.06, tower.RTP + 0.05)]
        body = meridian(Fd, tower.Y_TOP - 0.15, tower.Y_SHELL + 0.05, 0.05)
        RLo = tower.RL + 0.55 + 0.05
        foot = [(0, tower.Y_SHELL + 0.05, RLo), (0, tower.Y_LINTEL - 0.05, RLo)]
        pts = head + body + foot
        for a, b in zip(pts, pts[1:]):
            Fd.cyl(a, b, 0.008, PB if min(a[1], b[1]) > YC - 7.6 else CU, sides=6)
        run(Fd, body, 0.008, CU, clamp=0.05)
        # the column
        rv = np.array([math.sin(ph), 0, math.cos(ph)])
        p0 = rv * 51.7 + [0, 1.6, 0]
        p1 = np.array([math.sin(d), 0, math.cos(d)]) * (tower.RL - 0.1) + [0, tower.Y_LINTEL + 0.2, 0]
        ax = (p1 - p0) / np.linalg.norm(p1 - p0)
        side = np.cross(ax, rv)
        side /= np.linalg.norm(side)
        w = np.cross(side, ax)
        w = w if w @ rv > 0 else -w
        q = lambda y: p0 + (p1 - p0) * ((y - 0.45 * w[1] - p0[1]) / (p1[1] - p0[1])) + 0.45 * w
        qa, qb = q(tower.Y_LINTEL - 0.05), q(1.65)
        under = Fd.w(0, tower.Y_LINTEL - 0.05, RLo)
        for a, b in ((under, qa), (qa, qb)):
            o.still.append(["c", *map(float, a), *map(float, b), 0.008, 6, CU, CU])
        L = float(np.linalg.norm(qa - qb))
        for k in np.arange(0.6, L, 1.2):
            p = qb + (qa - qb) * (k / L)
            o.still.append(["c", *map(float, p), *map(float, p - 0.05 * w), 0.006, 4, GV, GV])
        Ff = Frame(o, ph)
        rq = float(qb @ rv)
        low = [(0, 1.65, rq), (0, 1.65, 52.95), (0, 1.05, 52.95), (0, 1.05, 53.05), (0, -0.3, 53.05)]
        for a, b in zip(low, low[1:]):
            Ff.cyl(a, b, 0.008, CU, sides=6)
        Ff.box(-0.04, 0.04, 0.0, 1.0, 53.0, 53.1, "ct_pvc")                                           # the guard
        Ff.box(-0.04, 0.04, 1.0, 1.1, 52.9, 53.1, "ct_pvc")
        Ff.box(-0.07, 0.07, 1.22, 1.38, 52.9, 52.97, "ct_brass")                                     # test joint
        Ff.box(-0.06, 0.06, 1.42, 1.45, 52.9, 52.903, {"in": tag(30)})
        Ff.cyl((0, 0.002, 53.6), (0, 0.014, 53.6), 0.12, "ct_cast", sides=16)                         # ground rod well
    return o.ops()


def obstruction_textures():
    """Piece 6's materials: beacons, copper and lead-coated conductors, the guard."""
    out = {}

    def flat(rgb, seed, n=16, dirt=0.04):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    out["ct_copper"] = flat((128, 84, 52), 71)          # weathered bare copper
    out["ct_lead"] = flat((122, 126, 130), 72)          # lead-coated copper (the top 25 ft)
    out["ct_pvc"] = flat((96, 100, 104), 73)
    out["ct_beacon_body"] = flat((70, 74, 78), 74)
    out["ct_beacon_lens"] = flat((206, 214, 218), 75, dirt=0.02)
    out["ct_beacon_white"] = Image.new("RGBA", (16, 16), (255, 255, 255, 255))
    out["ct_beacon_red"] = Image.new("RGBA", (16, 16), (255, 40, 30, 255))
    return out


# ================================================================== piece 7: inside (fill, sprays, eliminators, riser)
#
# Real-world basis: a counterflow natural-draft tower's packing sits just above the air inlet: hot water comes up the
# central riser from the buried circulating-water conduit into a distribution head, out along main pipes (FRP) and
# laterals (PVC) to downward spray nozzles with splash targets, which spread it over the fill (cross-fluted PVC film
# packs, about 1.2 m deep, sitting on a grid of precast beams and girders on columns standing in the basin); it rains
# from the fill's underside through the rain zone into the basin while air drawn in through the inlet rises through it.
# Over the sprays, drift eliminators (PVC blade packs) on their own beams catch the droplets; a walkway runs over the
# eliminators along the main from the shell door, guarded both sides. This tower: fill 10.1 to 11.3 m, laterals 1.8 m
# apart at 11.75 m with nozzles every 1.8 m (about 1,300), eliminators 12.42 to 12.57 m, walkway at the door's sill
# (12.6 m) along the main across the whole tower; columns on a 5.4 m grid; the riser 3.2 m across. Live: the sprays
# and the rain run when circulating water flows; the basin level is the panel's.

INSIDE = {"fill": (10.1, 11.3), "lat_y": 11.75, "elim": (12.42, 12.57), "grid": 5.4, "pitch": 1.8, "riser": 1.6}


def inside_ops(tower, running=True, lifted=None, rain=True, strip=None):
    """The tower's inside (piece 7), in the shell door's frame (the main and the walkway along its r axis, through the
    centre). lifted: (t0, t1, r0, r1) where eliminator panels are lifted off for inspection. strip (in game): what lies
    between the fill and the eliminators (the laterals, nozzles and sprays, the eliminators' beams and underside, the
    fill's top) only within that distance of the walkway, the only place it can be seen from (through its grating)."""
    def seen(t):
        return strip is None or abs(t) < strip
    o = Ops()
    dphi = 2 * math.pi / tower.NPHI
    i0, n, YS, _ = tower.DOORS[0]
    F = Frame(o, (i0 + n / 2) * dphi)
    GV, DS, PC = "ct_galv", "ct_steel_dark", "ct_precast"
    f0, f1 = INSIDE["fill"]
    e0, e1 = INSIDE["elim"]
    YL = INSIDE["lat_y"]
    P = INSIDE["pitch"]
    RF = tower.RL - 0.95 - 0.05                                  # the lintel's inner face, less a gap
    inner = lambda y: tower.rad(y) - tower.thick(y)
    RE = inner(e1) - 0.03
    RL_ = inner(YL) - 0.1
    RR = INSIDE["riser"]

    def layer(y, R, facing, mat, cut=0.0, hole=0.0, skip=None, sides=None, hidden=False):
        """A flat layer's face at y inside radius R, in strips across t, cut along the walkway (|t| < cut) and round
        the riser (hole); sides: (y0, y1) to close the walkway's edges."""
        edges = [cut] if cut else [0.0]
        while edges[-1] < R:
            edges.append(min(R, edges[-1] + P))
        strips = [(a, b) for a, b in zip(edges, edges[1:])]
        strips = strips + [(-b, -a) for a, b in strips]
        for ta, tb in strips:
            rm = lambda t: math.sqrt(max(0.0, R * R - t * t))
            ra, rb = rm(ta), rm(tb)
            full = min(ra, rb)
            rs = [-full + k * P for k in range(int(2 * full / P) + 1)] + [full]
            cells = [((ta, r0), (tb, r0), (tb, r1), (ta, r1)) for r0, r1 in zip(rs, rs[1:]) if r1 - r0 > 1e-3]
            cells.append(((ta, full), (tb, full), (tb, rb), (ta, ra)))
            cells.append(((ta, -ra), (tb, -rb), (tb, -full), (ta, -full)))
            for c in cells:
                tm = sum(p[0] for p in c) / 4
                rmid = sum(p[1] for p in c) / 4
                if hole and math.hypot(tm, rmid) < hole:
                    continue
                if skip and skip[0] < tm < skip[1] and skip[2] < rmid < skip[3]:
                    continue
                if hidden and not seen(tm):
                    continue
                if abs(c[2][1] - c[1][1]) < 1e-4 and abs(c[3][1] - c[0][1]) < 1e-4:
                    continue
                F._quad([F.w(t, y, r) for t, r in c], mat, (0, facing, 0))
            if sides and abs(min(abs(ta), abs(tb)) - cut) < 1e-6:
                te = ta if ta > 0 else tb
                for r0, r1 in zip(rs, rs[1:]):
                    if skip and skip[0] < (te + math.copysign(0.1, te)) < skip[1] and skip[2] < (r0 + r1) / 2 < skip[3]:
                        continue
                    F._quad([F.w(te, sides[0], r0), F.w(te, sides[0], r1), F.w(te, sides[1], r1), F.w(te, sides[1], r0)],
                            "ct_eliminator", F.t * -math.copysign(1, te))

    # ------------------------------------------------------------ the riser and its distribution head
    F.cyl((0, -2.0, 0), (0, f1, 0), RR, "ct_column", sides=24)
    F.cyl((0, f1, 0), (0, 12.15, 0), RR + 0.15, DS, sides=24)
    F.cyl((0, 12.15, 0), (0, 12.17, 0), 0.6, GV, sides=12)                               # its vent and manhole cover
    # ------------------------------------------------------------ columns, girders and beams carrying the fill
    G = INSIDE["grid"]
    tl = [G / 2 + G * i for i in range(int(RF / G) + 1)]
    tl = [-t for t in reversed(tl)] + tl
    for t in tl:
        rmF = math.sqrt(max(0.0, RF * RF - t * t)) - 0.1
        rmE = math.sqrt(max(0.0, RE * RE - t * t)) - 0.1
        if rmF < 1.0:
            continue
        cols = [G * j for j in range(-int(rmF / G), int(rmF / G) + 1) if abs(G * j) < rmF - 0.6]
        for r in cols:
            F.box(t - 0.45, t + 0.45, -2.0, -1.8, r - 0.45, r + 0.45, PC)                  # pad on the basin floor
            top = 11.95 if abs(r) < rmE - 0.3 else 9.5
            F.box(t - 0.2, t + 0.2, -1.8, top, r - 0.2, r + 0.2, "ct_column")
        F.box(t - 0.15, t + 0.15, 9.5, 9.8, -rmF, rmF, PC)                                 # fill girder
        if rmE > 1.0 and seen(t):
            F.box(t - 0.125, t + 0.125, 11.95, 12.2, -rmE, rmE, PC)                        # eliminator girder
    for k in range(-int(RF / 1.2), int(RF / 1.2) + 1):                                      # fill support beams
        r = 1.2 * k
        tm = math.sqrt(max(0.0, RF * RF - r * r)) - 0.05
        if tm < 0.5:
            continue
        segs = [(-tm, tm)]
        if abs(r) < RR + 0.3:
            th_ = math.sqrt((RR + 0.3) ** 2 - r * r)
            segs = [(-tm, -th_), (th_, tm)]
        for a, b in segs:
            F.box(a, b, 9.8, f0, r - 0.075, r + 0.075, PC)
    for k in range(-int(RE / P), int(RE / P) + 1):                                          # eliminator beams
        r = P * k
        tm = math.sqrt(max(0.0, RE * RE - r * r)) - 0.05
        if strip is not None:
            tm = min(tm, strip)
        if tm > 0.5:
            F.box(-tm, tm, 12.2, e0, r - 0.06, r + 0.06, "ct_frp")
    # ------------------------------------------------------------ the fill
    layer(f1, RF, 1, "ct_fill_top", hole=RR + 0.05, hidden=True)
    layer(f0, RF, -1, "ct_fill", hole=RR + 0.05)
    # ------------------------------------------------------------ distribution: the main, laterals, nozzles, sprays
    for s in (-1, 1):
        F.cyl((0, YL, s * (RR + 0.1)), (0, YL, s * RL_), 0.4, "ct_frp", sides=16)
        for r in np.arange(6.0, RL_ - 1, 6.0):
            F.cyl((0, YL, s * (r - 0.04)), (0, YL, s * (r + 0.04)), 0.46, "ct_frp", sides=16)
        for k in range(1, int(RE / P) + 1):                                                 # hangers from the beams
            if k * P < RL_:
                F.cyl((0, 12.2, s * k * P), (0, YL + 0.4, s * k * P), 0.012, GV, sides=4)
    nn = 0
    for k in range(int(RL_ / P) + 1):
        for s in (-1, 1):
            r = s * (P / 2 + P * k)
            tm = math.sqrt(max(0.0, RL_ * RL_ - r * r))
            if tm < 1.0:
                continue
            for u in (-1, 1):
                F.cyl((u * 0.38, YL, r), (u * (tm if strip is None else min(tm, strip)), YL, r), 0.075, "ct_pvc_white", sides=8)
                t = 1.3
                while t < tm - 0.3 and seen(t):
                    tt = u * t
                    F.cyl((tt, YL - 0.07, r), (tt, YL - 0.2, r), 0.025, "ct_pvc_white", sides=6)
                    F.cyl((tt, YL - 0.2, r), (tt, YL - 0.27, r), 0.045, "ct_nozzle", sides=8)
                    F.cyl((tt, YL - 0.27, r), (tt, YL - 0.33, r), 0.008, "ct_nozzle", sides=4)
                    F.cyl((tt, YL - 0.33, r), (tt, YL - 0.345, r), 0.07, "ct_nozzle", sides=10)
                    nn += 1
                    if running:
                        apex = F.w(tt, YL - 0.345, r)
                        ring = [F.w(tt + 0.8 * math.cos(2 * math.pi * j / 8), f1 + 0.01, r + 0.8 * math.sin(2 * math.pi * j / 8))
                                for j in range(9)]
                        for j in range(8):
                            F._quad([ring[j], ring[j + 1], apex, apex], "ct_spray", (0, 1, 0), lit=True)
                    t += P
    INSIDE["nozzles"] = nn
    # ------------------------------------------------------------ eliminators and the walkway over them
    layer(e1, RE, 1, "ct_eliminator", cut=0.65, skip=lifted, sides=(e0, e1))
    layer(e0, RE, -1, "ct_eliminator", cut=0.65, skip=lifted, hidden=True)
    rw0, rw1 = -(inner(YS) - 0.05), inner(YS) - 0.05
    for s in (-1, 1):
        F.box(*sorted((s * 0.55, s * 0.62)), e0, YS - 0.04, rw0, rw1, DS)
    F.box(-0.62, 0.62, YS - 0.04, YS, rw0, rw1, "ct_grating")
    for s in (-1, 1):
        _rails(F, [(s * 0.6, rw1 - 0.25), (s * 0.6, rw0 + 0.25)], YS)
    _rails(F, [(-0.6, rw0 + 0.25), (0.6, rw0 + 0.25)], YS, posts=False, toe=False)
    F.box(-0.596, 0.596, YS, YS + 0.1, rw0 + 0.247, rw0 + 0.253, GV)
    if lifted:                                                    # the lifted panels, stacked on the walkway
        for k in range(3):
            F.box(0.15, 0.5, YS + k * 0.15, YS + (k + 1) * 0.15 - 0.002, (lifted[2] + lifted[3]) / 2 - 0.9,
                  (lifted[2] + lifted[3]) / 2 + 0.9, "ct_eliminator")
    # ------------------------------------------------------------ the rain under the fill (live)
    if running and rain:
        rng = np.random.default_rng(5)
        for _ in range(3000):
            a = rng.uniform(0, 2 * math.pi)
            rr = math.sqrt(rng.uniform(4, (RF - 0.5) ** 2))
            t, r = rr * math.cos(a), rr * math.sin(a)
            y1 = rng.uniform(0.0, f0 - 0.4)
            h = rng.uniform(0.6, 1.6)
            F._quad([F.w(t - 0.012, y1, r), F.w(t + 0.012, y1, r), F.w(t + 0.012, min(f0 - 0.3, y1 + h), r),
                     F.w(t - 0.012, min(f0 - 0.3, y1 + h), r)], "ct_rain", F.r if (t * 7 + r) % 2 > 1 else -F.r, lit=True)
    return o.ops()


def inside_textures():
    out = {}
    from PIL import ImageDraw as D

    def flat(rgb, seed, n=32, dirt=0.04):
        r = np.random.default_rng(seed)
        a = np.ones((n, n, 3)) * np.array(rgb, float)
        a *= 1 + r.normal(0, dirt, (n, n))[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    f = flat((54, 58, 60), 81, n=64, dirt=0.02)
    d = D.Draw(f)
    for i in range(-64, 64, 6):                                    # cross-fluted film, seen from below
        d.line([(i, 0), (i + 38, 63)], fill=(86, 92, 96, 255))
        d.line([(i + 38, 0), (i, 63)], fill=(70, 76, 80, 255))
    for x in range(0, 64, 8):
        d.line([(x, 0), (x, 63)], fill=(36, 38, 40, 255))
    out["ct_fill"] = f
    t = flat((60, 64, 66), 82, n=64, dirt=0.02)
    d = D.Draw(t)
    for x in range(0, 64, 4):
        d.line([(x, 0), (x, 63)], fill=(40, 42, 44, 255))
    for y in range(0, 64, 32):
        d.line([(0, y), (63, y)], fill=(30, 32, 34, 255))
    out["ct_fill_top"] = t
    e = flat((176, 182, 180), 83, n=64, dirt=0.02)                 # pale PVC blade packs, seen from above
    d = D.Draw(e)
    for x in range(0, 64, 4):
        d.line([(x, 0), (x, 63)], fill=(120, 126, 124, 255))
    for y in range(0, 64, 21):
        d.line([(0, y), (63, y)], fill=(96, 100, 98, 255), width=2)
    out["ct_eliminator"] = e
    out["ct_frp"] = flat((150, 152, 120), 84)                       # FRP pipe and beams
    out["ct_pvc_white"] = flat((218, 220, 216), 85)
    out["ct_nozzle"] = flat((60, 64, 70), 86)
    out["ct_spray"] = Image.new("RGBA", (16, 16), (214, 230, 238, 120))
    out["ct_rain"] = Image.new("RGBA", (16, 16), (205, 222, 232, 150))
    return out
