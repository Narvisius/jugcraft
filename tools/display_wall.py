"""The main control room's plant display wall made live (round 32; Justin, 15 October 2026: "Display wall live",
"Overview + safety", "Alarm banner, no horn", "Fixed screens", "Rear-projection cubes").

The wall (plant_display_wall, 9 wide, 1 deep, 4 high, over the reactor control console) is a video wall of rear-
projection cubes: seven across and three high, each 1.2 x 0.9 blocks (a 60 inch 4:3 cube) with hairline seams, in a
graphite steel cabinet: its ends louvred, the light engines' service doors under each column (louvred, a latch, the
cube's status lights and its number), the exhaust grille and the nameplate in the fascia over them. The picture is one
fixed large display panel across all 21 cubes, as a real plant's (the whole crew sees the same thing):

    top          the alarm banner: the boards' alarms as they come in (red trips, amber alarms; flashing until
                 acknowledged at their board, the cleared ones slowly until reset), newest first, and their count
    left         the critical safety functions (Westinghouse status trees: green, yellow, orange, red) and the key
                 parameters, the plant's mode and the time
    middle       the plant mimic: the reactor, the pressurizer, the four loops (each steam generator's level, its
                 reactor coolant pump red running or green stopped, its hot and cold leg temperatures and flow), the
                 steam lines through the MSIVs and the relief valves to the turbine and generator, the condenser, the
                 condensate and main feed pumps, the feed regulating valves, auxiliary feed and the ECCS; every line
                 bright while it carries flow and dim while it does not, every valve red open and green shut
    right        the electrical lineup (the switchyard, the generator and its breaker, the reserve transformers, the
                 4160 V safety buses and their diesels, the 480 V and DC buses) and the trends of the last five minutes

The picture is drawn at full brightness from flat colours (lines, symbols, panels: the game's own geometry, sharp at
any distance), its fixed words from this wall's own legend texture (textures/entity/display_wall_legends.png, 512 px a
block) and its live words in the game's font (client/LiveBoards.java). Live entries, beyond board_live's:

    texts    words the game writes: id, where (x, y the middle of its height h, z), alignment, widest
    levels   a vessel's level: value, lo..hi, its rectangle, its water
    fills    a patch coloured by state: id, quad, a material for each state (the safety functions' colours)
    flows    line segments drawn bright while their signal is true (flowing), dim while not
    pens     the trends: as the recorders' but across (h: true), time running left to right
    alarms   the banner's slots: rectangles at z, and where its count is written
"""
import contextlib
import math

from PIL import ImageDraw

import board_live as L
import control_boards as C

ATLAS = L.Atlas()


@contextlib.contextmanager
def own_atlas():
    keep = L.ATLAS
    L.ATLAS = ATLAS
    try:
        yield
    finally:
        L.ATLAS = keep


KEY = "plant_display_wall"
W, H = 9, 4
COLS, ROWS = 7, 3
CUBE_W, CUBE_H = 1.2, 0.9
X0, Y0 = 0.3, 0.75              # the picture's bottom left (blocks, on the wall)
PW, PH = COLS * CUBE_W, ROWS * CUBE_H
SZ = 0.12                       # the screens' plane (z); the cabinet's face is at FACE
FACE = 0.04
# The picture's layers in front of the screens, each a thousandth nearer (nothing that overlaps shares a plane):
# panels, boxes in them, lines (two layers), symbols' fills, outlines (two layers), words, the game's words, seams.
Z_PANEL, Z_BOX, Z_LINE, Z_LINE2, Z_SYM, Z_RING, Z_RING2, Z_LABEL, Z_TEXT, Z_SEAM = (round(SZ - k / 1000, 3) for k in (2, 3, 4, 5, 6, 7, 8, 9, 10, 12))

# The picture's colours (a high-performance display: dark ground, muted lines, colour kept for state and alarms).
BG = (14, 17, 22)
PANEL = (23, 28, 36)
EDGE = (56, 64, 78)
TITLE = (120, 198, 238)
LABEL = (168, 176, 188)
UNIT = (124, 132, 146)
DIM = (60, 66, 76)
RCS = (214, 146, 108)
STEAM = (194, 202, 214)
FEED = (128, 174, 216)
ECCS = (170, 150, 214)
ELEC = (206, 206, 196)
OUTLINE = (186, 192, 202)
RED = (226, 54, 44)
GREEN = (44, 176, 82)
WATER = (46, 108, 178)
CORE = (236, 146, 50)
CORE_OFF = (96, 46, 30)
CSF_RGB = [(44, 176, 82), (236, 214, 54), (250, 140, 36), (226, 46, 38)]
FONT_PX = 0.72                  # DejaVu's capital height, a fraction of its size


def solid(rgb):
    return L.solid_tex(rgb)


def label_tex(text, h, ink, bg, bold=True):
    """A word on the picture: its capitals h blocks high, ink on the ground it sits on (so it needs no transparency);
    returns the material and its width (blocks)."""
    size = max(8, int(round(h * L.LEGEND_PX / FONT_PX)))
    f = L._font(size, bold)
    from PIL import Image
    probe = ImageDraw.Draw(Image.new("RGBA", (4, 4)))
    tw = probe.textlength(text, font=f)
    pw, ph = int(math.ceil(tw)) + 6, int(round(h * L.LEGEND_PX * 1.5)) + 4

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, img.width - 1, img.height - 1], fill=tuple(bg) + (255,))
        d.text((3, img.height / 2), text, font=f, fill=tuple(ink) + (255,), anchor="lm")
    key = f"dw:{size}:{'b' if bold else 'r'}:{ink}:{bg}:{text}"
    return ATLAS.add(key, pw, ph, draw), pw / L.LEGEND_PX, ph / L.LEGEND_PX


class Wall(L.LiveBoard):
    """The wall's board: picture coordinates (u along the picture from its left, v up from its bottom) to the wall's."""

    def __init__(self, seed=1201):
        super().__init__(W, seed, "PLANT OVERVIEW", face=SZ)
        for k in ("texts", "levels", "fills", "flows", "alarms"):
            self.live[k] = []

    # -- coordinates and flat shapes on the picture (all lit)

    @staticmethod
    def xy(u, v):
        return X0 + u, Y0 + v

    def rect(self, u0, v0, u1, v1, rgb, z):
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        self.front_quad(x0, y0, x1, y1, z, solid(rgb), self.lit)

    def poly(self, pts, rgb, z, lst=None):
        """A convex polygon on the picture (u, v corners anticlockwise), as a fan of quads."""
        lst = self.lit if lst is None else lst
        mat = solid(rgb) if isinstance(rgb, tuple) else rgb
        p = [self.xy(*q) for q in pts]
        if len(p) % 2:
            # An odd count: repeat the last corner's neighbour midway so the fan closes in quads.
            a, b = p[-1], p[0]
            p.append(((a[0] + b[0]) / 2, (a[1] + b[1]) / 2))
        for k in range(1, len(p) - 2, 2):
            a, b, c, d = p[0], p[k], p[k + 1], p[k + 2]
            # tl, bl, br, tr: anticlockwise as the viewer sees them, as front_quad's.
            self.quad((a[0], a[1], z), (b[0], b[1], z), (c[0], c[1], z), (d[0], d[1], z), mat, lst)

    def segment(self, a, b, w, rgb, z, lst=None, cap_a=True, cap_b=True, skip_a=False):
        """A straight line from a to b (u, v), w wide: an end with a cap squared off half its width beyond its point
        (a free end, or a corner's outside); skip_a starts it half its width past a (a corner's next leg, so the two
        legs only touch)."""
        (u0, v0), (u1, v1) = a, b
        du, dv = u1 - u0, v1 - v0
        ln = math.hypot(du, dv)
        if ln < 1e-9:
            return
        tu, tv = du / ln * w / 2, dv / ln * w / 2
        nu, nv = -tv, tu
        ka = -1 if cap_a and not skip_a else 1 if skip_a else 0
        kb = 1 if cap_b else 0
        pts = [(u0 + ka * tu - nu, v0 + ka * tv - nv), (u1 + kb * tu - nu, v1 + kb * tv - nv), (u1 + kb * tu + nu, v1 + kb * tv + nv),
               (u0 + ka * tu + nu, v0 + ka * tv + nv)]
        self.poly(pts, rgb, z, lst)

    def polyline(self, pts, w, rgb, z, lst=None, cap0=True, cap1=True):
        """A line through pts: each leg runs on round its corner, the next starts where it ends. Without cap0 (cap1) the
        line stops at its first (last) point: where it meets the side of another line."""
        n = len(pts) - 1
        for i, (a, b) in enumerate(zip(pts, pts[1:])):
            self.segment(a, b, w, rgb, z, lst, cap_a=cap0 if i == 0 else False, cap_b=cap1 if i == n - 1 else True, skip_a=i > 0)

    def line(self, pts, w, rgb, z=Z_LINE, cap0=True, cap1=True):
        self.polyline(pts, w, rgb, z, None, cap0, cap1)

    def flow(self, signal, pts, w, rgb, z=Z_LINE, cap0=True, cap1=True):
        """A line bright (rgb) while its signal is true, dim while not."""
        on, off = solid(rgb), solid(DIM)
        lst = []
        self.polyline(pts, w, on, z, lst, cap0, cap1)
        for op in lst:
            self.live["flows"].append({"s": signal, "q": op[1:13], "on": on, "off": off})

    def outline(self, pts, w, rgb):
        """A closed outline round pts, its sides on the two outline layers in turn (where they cross at the corners
        they never share a plane)."""
        mat = solid(rgb)
        for i in range(len(pts)):
            self.segment(pts[i], pts[(i + 1) % len(pts)], w, mat, Z_RING if i % 2 == 0 else Z_RING2)

    def circle_pts(self, cu, cv, r, n=24):
        return [(cu + r * math.cos(2 * math.pi * k / n), cv + r * math.sin(2 * math.pi * k / n)) for k in range(n)]

    def disc(self, cu, cv, r, rgb, z, n=24):
        self.poly(self.circle_pts(cu, cv, r, n), rgb, z)

    def ring(self, cu, cv, r, w, rgb, z, n=24):
        mat = solid(rgb)
        for k in range(n):
            a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
            pts = [(cu + (r - w / 2) * math.cos(a0), cv + (r - w / 2) * math.sin(a0)), (cu + (r + w / 2) * math.cos(a0), cv + (r + w / 2) * math.sin(a0)),
                   (cu + (r + w / 2) * math.cos(a1), cv + (r + w / 2) * math.sin(a1)), (cu + (r - w / 2) * math.cos(a1), cv + (r - w / 2) * math.sin(a1))]
            self.poly(pts, mat, z)

    def frame(self, u0, v0, u1, v1, w, rgb, z):
        self.rect(u0, v0, u1, v0 + w, rgb, z)
        self.rect(u0, v1 - w, u1, v1, rgb, z)
        self.rect(u0, v0 + w, u0 + w, v1 - w, rgb, z)
        self.rect(u1 - w, v0 + w, u1, v1 - w, rgb, z)

    def capsule(self, cu, v0, v1, wd, rgb, w=0.012):
        """A vessel's outline: straight sides from v0 to v1 and elliptical heads over and under them (their pieces on
        the two outline layers in turn, so where they meet they never share a plane)."""
        r = wd / 2
        mat = solid(rgb)
        self.segment((cu - r, v0), (cu - r, v1), w, mat, Z_RING, cap_a=False, cap_b=False)
        self.segment((cu + r, v0), (cu + r, v1), w, mat, Z_RING2, cap_a=False, cap_b=False)
        n = 12
        for c, sgn in ((v1, 1), (v0, -1)):
            for k in range(n):
                a0, a1 = math.pi * k / n, math.pi * (k + 1) / n
                p0 = (cu + r * math.cos(a0), c + sgn * r * 0.6 * math.sin(a0))
                p1 = (cu + r * math.cos(a1), c + sgn * r * 0.6 * math.sin(a1))
                self.segment(p0, p1, w, mat, Z_RING if k % 2 == 0 else Z_RING2, cap_a=False, cap_b=False)

    def lamp_poly(self, signal, pts, z, on=RED, off=GREEN):
        """A symbol's fill coloured by state: on (red: running, open, closed for a breaker) while its signal is true,
        off (green) while not."""
        mon, moff = solid(on), solid(off)
        p = [self.xy(*q) for q in pts]
        if len(p) % 2:
            a, b = p[-1], p[0]
            p.append(((a[0] + b[0]) / 2, (a[1] + b[1]) / 2))
        for k in range(1, len(p) - 2, 2):
            a, b, c, d = p[0], p[k], p[k + 1], p[k + 2]
            q = [(a[0], a[1], z), (b[0], b[1], z), (c[0], c[1], z), (d[0], d[1], z)]
            self.live["lamps"].append({"s": signal, "q": L._flat(*q), "on": mon, "off": moff, "off_lit": True})

    # -- symbols

    def pump(self, cu, cv, signal, r=0.042):
        """A pump: its ring and the discharge's tangent, the middle red running and green stopped."""
        self.lamp_poly(signal, self.circle_pts(cu, cv, r - 0.006, 20), Z_SYM)
        self.ring(cu, cv, r, 0.01, OUTLINE, Z_RING, 20)
        self.segment((cu, cv + r), (cu + r * 1.1, cv + r), 0.01, OUTLINE, Z_RING2)

    def valve(self, cu, cv, signal, s=0.032, vertical=False):
        """A valve: a bowtie, red open and green shut."""
        e = 0.004
        if vertical:
            a = [(cu - s, cv + s), (cu - e, cv), (cu + e, cv), (cu + s, cv + s)]
            b = [(cu - s, cv - s), (cu + s, cv - s), (cu + e, cv), (cu - e, cv)]
        else:
            a = [(cu - s, cv - s), (cu, cv - e), (cu, cv + e), (cu - s, cv + s)]
            b = [(cu + s, cv - s), (cu + s, cv + s), (cu, cv + e), (cu, cv - e)]
        for pts in (a, b):
            self.lamp_poly(signal, pts, Z_SYM)

    def breaker(self, cu, cv, signal, s=0.03):
        """A breaker: a square, red closed and green open, in a light frame."""
        self.lamp_poly(signal, [(cu - s, cv - s), (cu + s, cv - s), (cu + s, cv + s), (cu - s, cv + s)], Z_SYM)
        self.frame(cu - s - 0.008, cv - s - 0.008, cu + s + 0.008, cv + s + 0.008, 0.008, OUTLINE, Z_RING)

    def xfmr(self, cu, cv, r=0.04):
        """A transformer: two overlapping rings."""
        self.ring(cu, cv + r * 0.7, r, 0.009, ELEC, Z_RING, 20)
        self.ring(cu, cv - r * 0.7, r, 0.009, ELEC, Z_RING2, 20)

    # -- words

    def label(self, u, v, text, h=0.05, ink=LABEL, bg=PANEL, anchor="l", bold=True):
        """A fixed word, its capitals' middle at v; anchor l, c or r at u."""
        mat, w, hh = label_tex(text, h, ink, bg, bold)
        x0 = u - (0 if anchor == "l" else w / 2 if anchor == "c" else w) - 3 / L.LEGEND_PX
        x, y = self.xy(x0, v - hh / 2)
        self.front_quad(x, y, x + w, y + hh, Z_LABEL, mat, self.lit)
        return w

    def text(self, tid, u, v, h=0.07, anchor="l", widest=None, colour=(236, 240, 246)):
        """Words the game writes (client/LiveBoards.java wallText): its capitals' middle at v."""
        x, y = self.xy(u, v)
        e = {"id": tid, "x": round(x, 4), "y": round(y, 4), "z": round(Z_TEXT, 4), "h": round(h, 4), "a": anchor, "c": list(colour)}
        if widest:
            e["w"] = round(widest, 4)
        self.live["texts"].append(e)

    def panel(self, u0, v0, u1, v1, title=None):
        self.rect(u0, v0, u1, v1, PANEL, Z_PANEL)
        self.frame(u0, v0, u1, v1, 0.006, EDGE, Z_LINE)
        if title:
            self.label(u0 + 0.05, v1 - 0.07, title, 0.052, TITLE)

    def level(self, value, lo, hi, u0, v0, u1, v1, rgb=WATER):
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        self.rect(u0, v0, u1, v1, BG, Z_BOX)
        self.live["levels"].append({"v": value, "lo": lo, "hi": hi, "x0": round(x0, 4), "y0": round(y0, 4), "x1": round(x1, 4),
                                    "y1": round(y1, 4), "z": round(Z_SYM, 4), "m": solid(rgb)})


# ---------------------------------------------------------------- the cabinet



def stud(front, side=None):
    """The faces of a part standing off a face (its back against it): seen from the front, the sides, above and below
    (the wall hangs over the boards: its parts' undersides show)."""
    side = side or front
    return [front, None, side, side, side, side]


def louvres(b, x0, x1, y0, y1, n, z):
    """A louvred vent on a face at z: a dark recess behind slats standing off it, their undersides in shadow."""
    b.front_quad(x0, y0, x1, y1, z - 0.001, C.sw("black"), None)
    step = (y1 - y0) / n
    g = C.sw("dgrey")
    for k in range(n):
        ya = y0 + k * step + step * 0.45
        b.box(x0 + 0.004, ya, z - 0.012, x1 - 0.004, ya + step * 0.4, z - 0.002, [g, None, g, g, g, g])


CAB = 0.04                       # the cabinet's face (z); the screens are set back behind its opening at SZ


def cabinet(b):
    # The cabinet in the sheet's own colours (its item shows it), its plates on the wall's legend texture.
    g, gd, blk = C.sw("dgrey"), C.sw("dgrey"), C.sw("black")
    top = Y0 + PH
    # The plinth the cabinet stands on, its ends (louvred), the base under the cubes and the fascia over them: the
    # opening's sides (the ends', the base's top, the fascia's underside) black down to the screens.
    b.box(0.0, 0.0, CAB - 0.01, W, 0.05, 1.0, [blk, None, blk, blk, blk, gd])
    b.box(0.0, 0.05, CAB, X0, top, 1.0, [g, None, g, blk, None, None])
    b.box(X0 + PW, 0.05, CAB, W, top, 1.0, [g, None, blk, g, None, None])
    b.box(X0, 0.05, CAB, X0 + PW, Y0, 1.0, [g, None, None, None, blk, None])
    b.box(0.0, top, CAB, W, H, 1.0, [g, None, g, g, None, blk])
    for x0, x1 in ((0.0, X0), (X0 + PW, W)):
        louvres(b, x0 + 0.07, x1 - 0.07, Y0 + 0.25, top - 0.25, 22, CAB)
    # A black lip round the opening.
    for x0, y0, x1, y1 in ((X0 - 0.025, Y0 - 0.025, X0 + PW + 0.025, Y0), (X0 - 0.025, top, X0 + PW + 0.025, top + 0.025),
                           (X0 - 0.025, Y0, X0, top), (X0 + PW, Y0, X0 + PW + 0.025, top)):
        b.box(x0, y0, CAB - 0.004, x1, y1, CAB, stud(blk))
    # The service doors of the cubes' light engines, one under each column: louvred, the latch, the cube's number and
    # its status lights (POWER lit green, LAMP dark amber).
    for i in range(COLS):
        d0, d1 = X0 + i * CUBE_W + 0.012, X0 + (i + 1) * CUBE_W - 0.012
        b.box(d0, 0.08, CAB - 0.012, d1, Y0 - 0.055, CAB, stud(g, gd))
        louvres(b, d0 + 0.08, d1 - 0.08, 0.3, Y0 - 0.1, 9, CAB - 0.012)
        b.box(d1 - 0.1, 0.13, CAB - 0.026, d1 - 0.05, 0.25, CAB - 0.012, stud(blk))
        b.box(d1 - 0.085, 0.165, CAB - 0.034, d1 - 0.065, 0.215, CAB - 0.026, stud(C.sw("mgrey")))
        mat = L.plate_tex(f"CUBE {i + 1}", 0.14, 0.04, "black")
        b.box(d0 + 0.06, 0.14, CAB - 0.016, d0 + 0.2, 0.18, CAB - 0.012, stud(mat, blk))
        for k, (lens, lit) in enumerate(((C.LENS["green"], True), (C.LENS["off_amber"], False))):
            cx = d0 + 0.26 + k * 0.05
            b.box(cx - 0.012, 0.15, CAB - 0.018, cx + 0.012, 0.174, CAB - 0.012, stud(blk))
            b.front_quad(cx - 0.008, 0.154, cx + 0.008, 0.17, CAB - 0.0185, lens, b.lit if lit else None)
    # The fascia: its nameplate (screwed on) and the cubes' exhaust grille along the top.
    louvres(b, 0.3, W - 0.3, H - 0.16, H - 0.04, 4, CAB)
    mat = L.plate_tex("UNIT 1  PLANT OVERVIEW DISPLAY", 2.2, 0.11, "black")
    b.box(W / 2 - 1.1, top + 0.1, CAB - 0.01, W / 2 + 1.1, top + 0.21, CAB, stud(mat, blk))
    for x in (W / 2 - 1.06, W / 2 + 1.06):
        for y in (top + 0.12, top + 0.19):
            b.cyl((x, y, CAB - 0.01), (x, y, CAB - 0.014), 0.006, "stainless", "stainless", 6)
    b.core = len(b.still)


# ---------------------------------------------------------------- the picture

def screens(b):
    """The cubes' screens (the picture's ground) and the hairline seams between them."""
    for i in range(COLS):
        for j in range(ROWS):
            x0, y0 = b.xy(i * CUBE_W, j * CUBE_H)
            b.front_quad(x0, y0, x0 + CUBE_W, y0 + CUBE_H, SZ, C.sw("black"), b.lit)
    for i in range(1, COLS):
        b.rect(i * CUBE_W - 0.0015, 0, i * CUBE_W + 0.0015, PH, (6, 7, 9), Z_SEAM)
    for j in range(1, ROWS):
        for i in range(COLS):
            b.rect(i * CUBE_W + (0.0015 if i else 0), j * CUBE_H - 0.0015, (i + 1) * CUBE_W - (0.0015 if i < COLS - 1 else 0),
                   j * CUBE_H + 0.0015, (6, 7, 9), Z_SEAM)


BANNER = (PH - 0.31, PH - 0.03)          # the alarm banner's bottom and top (v)
ZONE_TOP = PH - 0.35


def banner(b):
    v0, v1 = BANNER
    b.panel(0.04, v0, CUBE_W - 0.04, v1)
    b.label(0.1, v1 - 0.075, "ALARMS", 0.06, TITLE)
    b.text("dw.alarm_count", 0.1, v0 + 0.085, 0.06, "l", CUBE_W - 0.2)
    slots = []
    for i in range(1, COLS):
        u0, u1 = i * CUBE_W + 0.04, (i + 1) * CUBE_W - 0.04
        b.rect(u0, v0, u1, v1, PANEL, Z_PANEL)
        b.frame(u0, v0, u1, v1, 0.006, EDGE, Z_LINE)
        x0, y0 = b.xy(u0 + 0.008, v0 + 0.008)
        x1, y1 = b.xy(u1 - 0.008, v1 - 0.008)
        slots.append([round(x0, 4), round(y0, 4), round(x1, 4), round(y1, 4)])
    # The slots' colours: a trip's red and an alarm's amber, lit and (a cleared alarm waiting for RESET, flashing
    # slowly) dimmed.
    mats = {"red": solid((200, 40, 32)), "amber": solid((226, 150, 30)), "red_dim": solid((90, 24, 20)), "amber_dim": solid((102, 68, 16))}
    b.live["alarms"].append({"slots": slots, "z": round(Z_SYM, 4), "tz": round(Z_TEXT, 4), "mats": mats})


CSF = [("S", "SUBCRITICALITY"), ("C", "CORE COOLING"), ("H", "HEAT SINK"), ("P", "INTEGRITY"), ("Z", "CONTAINMENT"), ("I", "INVENTORY")]
KEY_ROWS = [("REACTOR POWER", "%", "dw.power"), ("TAVG / TREF", "°F", "dw.tavg"), ("PZR PRESSURE", "PSIG", "dw.pzr_press"),
            ("PZR LEVEL", "%", "dw.pzr_level"), ("RCS SUBCOOLING", "°F", "dw.subcool"), ("CORE EXIT TEMP", "°F", "dw.cet"),
            ("CONTAINMENT PRESS", "PSIG", "dw.cnt_press"), ("RWST LEVEL", "%", "dw.rwst")]


def left_zone(b):
    u0, u1 = 0.04, 2 * CUBE_W - 0.04
    b.panel(u0, 1.22, u1, ZONE_TOP, "CRITICAL SAFETY FUNCTIONS")
    tw, th = 1.08, 0.24
    for k, (letter, name) in enumerate(CSF):
        i, j = k % 2, k // 2
        tu0 = u0 + 0.06 + i * (tw + 0.06)
        tv1 = ZONE_TOP - 0.14 - j * (th + 0.06)
        x0, y0 = b.xy(tu0, tv1 - th)
        x1, y1 = b.xy(tu0 + tw, tv1)
        q = L._flat((x0, y1, Z_SYM), (x0, y0, Z_SYM), (x1, y0, Z_SYM), (x1, y1, Z_SYM))
        b.live["fills"].append({"id": f"dw.csf{k}", "q": q, "m": [solid(c) for c in CSF_RGB]})
        b.frame(tu0 - 0.008, tv1 - th - 0.008, tu0 + tw + 0.008, tv1 + 0.008, 0.008, EDGE, Z_LINE)
        b.text(f"dw.csf_letter{k}", tu0 + 0.1, tv1 - th / 2, 0.12, "c", None, (12, 14, 16))
        b.text(f"dw.csf_name{k}", tu0 + 0.2, tv1 - th / 2 + 0.035, 0.05, "l", tw - 0.24, (12, 14, 16))
        b.text(f"dw.csf_state{k}", tu0 + 0.2, tv1 - th / 2 - 0.05, 0.042, "l", tw - 0.24, (12, 14, 16))
    # Under the trees: the function restoration procedure the worst of them calls for.
    b.text("dw.csf_priority", u0 + 0.07, 1.3, 0.05, "l", u1 - u0 - 0.14)
    b.panel(u0, 0.04, u1, 1.16, "KEY PARAMETERS")
    for k, (name, unit, tid) in enumerate(KEY_ROWS):
        v = 0.98 - k * 0.112
        if k % 2 == 0:
            b.rect(u0 + 0.03, v - 0.05, u1 - 0.03, v + 0.05, (28, 34, 44), Z_BOX)
        b.label(u0 + 0.07, v, name, 0.048, LABEL, (28, 34, 44) if k % 2 == 0 else PANEL)
        b.label(u1 - 0.07, v, unit, 0.04, UNIT, (28, 34, 44) if k % 2 == 0 else PANEL, "r")
        b.text(tid, u1 - 0.36, v, 0.064, "r", 0.6)
    b.text("dw.mode", u0 + 0.07, 0.085, 0.05, "l", 1.4, TITLE)
    b.text("dw.clock", u1 - 0.07, 0.085, 0.05, "r", 0.6, TITLE)


# The mimic (picture u, v: the middle three columns, u 2.4 to 6.0).
MU0, MU1 = 2 * CUBE_W + 0.04, 5 * CUBE_W - 0.04
VES = (2.86, 0.48, 1.24, 0.30)          # the vessel: its middle u, bottom and top of its sides, its width
PZR = (3.16, 1.64, 1.98, 0.14)
SG_U = [3.56, 3.92, 4.28, 4.64]
SG_V = (1.14, 1.62)
SG_W = 0.16
HOT_V, COLD_V, RCP_V = 0.98, 0.62, 0.80
FEED_V, STEAM_V, MSIV_V, ARV_V = 1.78, 2.06, 1.94, 1.85
LOOP_V = (0.48, 0.38, 0.28)
LW = 0.018                              # a main line's width
TURB_U = 4.96


def mimic(b):
    b.panel(MU0, 0.04, MU1, ZONE_TOP)
    b.label(MU0 + 0.05, ZONE_TOP - 0.07, "PLANT OVERVIEW", 0.052, TITLE)
    vu, vb, vt, vw = VES
    # The reactor: the vessel, its control rod drives on the head, the core lit by its power, its figures.
    b.capsule(vu, vb, vt, vw, OUTLINE, 0.014)
    for k in range(7):
        x = vu - 0.09 + k * 0.03
        b.segment((x, vt + vw / 2 * 0.6 + 0.012), (x, vt + 0.16), 0.008, OUTLINE, Z_RING)
    b.lamp_poly("dw.critical", [(vu - 0.1, vb + 0.06), (vu + 0.1, vb + 0.06), (vu + 0.1, vb + 0.46), (vu - 0.1, vb + 0.46)], Z_SYM, CORE, CORE_OFF)
    b.text("dw.core_power", vu, vb + 0.26, 0.06, "c", 0.19, (20, 16, 12))
    b.label(vu, vb - 0.12, "REACTOR", 0.045, LABEL, PANEL, "c")
    # The loops: the hot legs out along HOT_V, up into each steam generator; the cold legs down through each pump and
    # back along COLD_V into the vessel.
    right = vu + vw / 2
    bw = LW * 0.4                       # half a branch's width: where a main line runs on to cover its last branch
    b.flow("dw.rcs_flow", [(right, HOT_V), (SG_U[-1] - 0.045 + bw, HOT_V)], LW, RCS, cap0=False, cap1=False)
    b.flow("dw.rcs_flow", [(right, COLD_V), (SG_U[-1] + 0.045 + bw, COLD_V)], LW, RCS, cap0=False, cap1=False)
    for k, cu in enumerate(SG_U):
        n = k + 1
        hot, cold = cu - 0.045, cu + 0.045
        b.flow(f"rcp{n}.run", [(hot, HOT_V + LW / 2), (hot, SG_V[0] - 0.05)], LW * 0.8, RCS, cap0=False)
        b.flow(f"rcp{n}.run", [(cold, SG_V[0] - 0.05), (cold, HOT_V + 0.03)], LW * 0.8, RCS)
        b.flow(f"rcp{n}.run", [(cold, HOT_V - 0.03), (cold, RCP_V + 0.045)], LW * 0.8, RCS)
        b.flow(f"rcp{n}.run", [(cold, RCP_V - 0.045), (cold, COLD_V + LW / 2)], LW * 0.8, RCS, cap1=False)
        b.pump(cold, RCP_V, f"rcp{n}.run", 0.04)
        b.label(cold + 0.055, RCP_V - 0.035, f"RCP {n}", 0.034, LABEL, PANEL)
        # The steam generator: its shell, its level (narrow range), its name and figure.
        b.capsule(cu, SG_V[0], SG_V[1], SG_W, OUTLINE, 0.012)
        b.level(f"sg{n}.nr", 0, 100, cu - SG_W / 2 + 0.012, SG_V[0] + 0.01, cu + SG_W / 2 - 0.012, SG_V[1] - 0.01)
        b.label(cu - 0.025, SG_V[1] + 0.075, f"SG {n}", 0.036, LABEL, PANEL, "r")
        b.text(f"dw.sg{n}_level", cu, (SG_V[0] + SG_V[1]) / 2, 0.05, "c", SG_W - 0.03)
        # The steam line up through its MSIV to the header, its relief valve on a branch; the feed line down from the
        # header through its regulating valve into the shell's upper side.
        b.flow(f"dw.steam{n}", [(cu, SG_V[1] + 0.06), (cu, FEED_V - 0.025)], LW * 0.8, STEAM)
        b.flow(f"dw.steam{n}", [(cu, FEED_V + 0.025), (cu, MSIV_V - 0.035)], LW * 0.8, STEAM)
        b.flow(f"dw.steam_hdr{n}", [(cu, MSIV_V + 0.035), (cu, STEAM_V - LW / 2)], LW * 0.8, STEAM, cap1=False)
        b.valve(cu, MSIV_V, f"msiv{n}.open", 0.03, vertical=True)
        b.flow(f"sg{n}.arv", [(cu - bw, ARV_V), (cu - 0.082, ARV_V)], 0.01, STEAM, cap0=False)
        b.valve(cu - 0.1, ARV_V, f"sg{n}.arv", 0.018)
        b.segment((cu - 0.1, ARV_V + 0.02), (cu - 0.1, ARV_V + 0.06), 0.008, OUTLINE, Z_RING, cap_b=False)
        b.poly([(cu - 0.118, ARV_V + 0.06), (cu - 0.082, ARV_V + 0.06), (cu - 0.1, ARV_V + 0.085)], OUTLINE, Z_RING2)
        fu = cu + 0.12
        b.flow(f"sg{n}.frv", [(fu, FEED_V - bw), (fu, 1.70)], LW * 0.7, FEED, cap0=False)
        b.flow(f"sg{n}.frv", [(fu, 1.62), (fu, 1.50), (cu + SG_W / 2, 1.50)], LW * 0.7, FEED)
        b.valve(fu, 1.66, f"sg{n}.frv", 0.026, vertical=True)
        # Under the loop: its flow and its hot and cold leg temperatures (named once, at the left).
        for j, tid in enumerate(("flow", "thot", "tcold")):
            b.text(f"dw.loop{n}_{tid}", cu, LOOP_V[j], 0.045, "c", 0.3)
    for j, name in enumerate(("FLOW", "TH °F", "TC °F")):
        b.label(SG_U[0] - 0.2, LOOP_V[j], name, 0.03, UNIT, PANEL, "r")
    # The header joins the steam lines over to the turbine's stop valve; the feed header comes back from the feed pumps.
    b.flow("dw.steam_hdr", [(SG_U[0] - bw, STEAM_V), (TURB_U - 0.102, STEAM_V)], LW, STEAM, cap0=False, cap1=False)
    b.valve(TURB_U - 0.07, STEAM_V, "turb.valves", 0.032)
    b.flow("dw.turbine", [(TURB_U - 0.038, STEAM_V), (TURB_U, STEAM_V)], LW, STEAM, cap0=False, cap1=False)
    fw = LW * 0.35                      # half a feed branch's width
    for k in range(len(SG_U) - 1):
        # The feed header runs between the steam lines: broken where each crosses it, each piece running on over the
        # next feed branch's top.
        start = SG_U[k] + 0.12 + (-fw if k == 0 else fw)
        b.flow("dw.feed_hdr", [(start, FEED_V), (SG_U[k + 1] - 0.025, FEED_V)], LW * 0.8, FEED, cap0=False)
        b.flow("dw.feed_hdr", [(SG_U[k + 1] + 0.025, FEED_V), (SG_U[k + 1] + 0.12 + fw, FEED_V)], LW * 0.8, FEED, cap1=False)
    b.label(3.74, STEAM_V + 0.1, "MAIN STEAM", 0.034, UNIT, PANEL, "c")
    b.text("dw.steam_press", 3.98, STEAM_V + 0.1, 0.046, "l", 0.4)
    # The turbine (HP, two LPs on its shaft) and the generator, its breaker out to the switchyard.
    hp = [(TURB_U, STEAM_V - 0.05), (TURB_U + 0.16, STEAM_V - 0.09), (TURB_U + 0.16, STEAM_V + 0.09), (TURB_U, STEAM_V + 0.05)]
    b.poly(hp, (44, 52, 64), Z_SYM)
    lp = [(TURB_U + 0.2, STEAM_V - 0.08), (TURB_U + 0.5, STEAM_V - 0.14), (TURB_U + 0.5, STEAM_V + 0.14), (TURB_U + 0.2, STEAM_V + 0.08)]
    b.poly(lp, (44, 52, 64), Z_SYM)
    for pts in (hp, lp):
        b.outline(pts, 0.01, OUTLINE)
    b.segment((TURB_U + 0.16, STEAM_V), (TURB_U + 0.2, STEAM_V), 0.012, OUTLINE, Z_LINE)
    b.segment((TURB_U + 0.5, STEAM_V), (TURB_U + 0.632, STEAM_V), 0.012, OUTLINE, Z_LINE)
    gu = TURB_U + 0.72
    b.lamp_poly("st.gen_online", b.circle_pts(gu, STEAM_V, 0.085, 24), Z_SYM, (60, 30, 30), (26, 46, 34))
    b.ring(gu, STEAM_V, 0.09, 0.012, OUTLINE, Z_RING)
    b.text("dw.gen_g", gu, STEAM_V, 0.07, "c", 0.1, (230, 232, 236))
    b.flow("gen.bkr", [(gu + 0.096, STEAM_V), (gu + 0.138, STEAM_V)], LW, ELEC, cap0=False, cap1=False)
    b.breaker(gu + 0.17, STEAM_V, "gen.bkr", 0.024)
    b.flow("gen.bkr", [(gu + 0.202, STEAM_V), (MU1 - 0.012, STEAM_V)], LW, ELEC, cap0=False, cap1=False)
    b.label(TURB_U - 0.06, 2.27, "TURBINE", 0.034, UNIT, PANEL)
    b.text("dw.turb_speed", TURB_U + 0.5, 2.27, 0.046, "r", 0.32)
    b.text("dw.gen_mw", MU1 - 0.04, 2.27, 0.046, "r", 0.3)
    # The condenser under the low pressure turbine, its hotwell's condensate pump, the main feed pumps in parallel and
    # the feed header back to the steam generators.
    cu0, cu1, cv0, cv1 = TURB_U + 0.12, TURB_U + 0.58, 1.44, 1.72
    b.rect(cu0, cv0, cu1, cv1, (30, 38, 48), Z_SYM)
    b.frame(cu0, cv0, cu1, cv1, 0.01, OUTLINE, Z_RING)
    b.label((cu0 + cu1) / 2, (cv0 + cv1) / 2 + 0.04, "CONDENSER", 0.034, LABEL, (30, 38, 48), "c")
    b.text("dw.vacuum", (cu0 + cu1) / 2, (cv0 + cv1) / 2 - 0.05, 0.045, "c", 0.42)
    b.flow("dw.turbine", [(TURB_U + 0.35, STEAM_V - 0.11), (TURB_U + 0.35, cv1)], LW * 0.8, STEAM, cap0=False, cap1=False)
    pu, ju, ru = TURB_U + 0.35, TURB_U + 0.22, TURB_U - 0.02
    b.flow("dw.feed_pumps", [(pu, cv0), (pu, 1.31 + 0.036)], LW * 0.8, FEED, cap0=False, cap1=False)
    b.pump(pu, 1.31, "dw.condensate", 0.036)
    b.label(pu + 0.05, 1.25, "COND PMP", 0.03, UNIT, PANEL)
    # The condensate pump's discharge over to the feed pumps' suction (it runs on over the branches' joint), the two
    # feed pumps side by side, their discharges into the riser up to the feed header.
    b.flow("dw.feed_pumps", [(pu, 1.31 - 0.036), (pu, 1.2), (ju, 1.2)], LW * 0.8, FEED, cap0=False)
    for k, (v, pid) in enumerate(((1.27, "a"), (1.13, "b"))):
        side = 1 if v > 1.2 else -1
        b.flow(f"mfp_{pid}.run", [(ju, 1.2 + side * LW * 0.4), (ju, v), (TURB_U + 0.134, v)], LW * 0.7, FEED, cap0=False, cap1=False)
        b.pump(TURB_U + 0.1, v, f"mfp_{pid}.run", 0.034)
        b.flow(f"mfp_{pid}.run", [(TURB_U + 0.066, v), (ru + LW * 0.4, v)], LW * 0.7, FEED, cap0=False, cap1=False)
        b.label(TURB_U + 0.1, v - 0.06, f"MFP {pid.upper()}", 0.028, UNIT, PANEL, "c")
    b.flow("dw.feed_hdr", [(ru, 1.13 - fw), (ru, FEED_V), (SG_U[-1] + 0.12 + fw, FEED_V)], LW * 0.8, FEED, cap0=False, cap1=False)
    # Auxiliary feed: its three pumps and the condensate storage tank.
    au0, au1, av0, av1 = TURB_U - 0.02, MU1 - 0.04, 0.40, 1.0
    b.rect(au0, av0, au1, av1, (28, 34, 44), Z_BOX)
    b.label(au0 + 0.04, av1 - 0.05, "AUX FEEDWATER", 0.036, TITLE, (28, 34, 44))
    for k, (pid, name, sig) in enumerate((("a", "MDP A", "afw_a.run"), ("td", "TDP", "afw_td.run"), ("b", "MDP B", "afw_b.run"))):
        u = au0 + 0.14 + k * 0.28
        b.pump(u, 0.78, sig, 0.036)
        b.label(u, 0.69, name, 0.03, LABEL, (28, 34, 44), "c")
        b.text(f"dw.afw_{pid}", u, 0.6, 0.042, "c", 0.26)
    b.label(au0 + 0.04, 0.48, "TOTAL GPM", 0.03, UNIT, (28, 34, 44))
    b.text("dw.afw_total", au1 - 0.04, 0.48, 0.05, "r", 0.3)
    b.label(MU1 - 0.24, 0.26, "CST", 0.034, LABEL, PANEL, "c")
    b.level("cst.level", 0, 100, MU1 - 0.3, 0.07, MU1 - 0.18, 0.21)
    b.frame(MU1 - 0.31, 0.06, MU1 - 0.17, 0.22, 0.008, OUTLINE, Z_RING)
    b.text("dw.cst", MU1 - 0.06, 0.14, 0.042, "r", 0.12)
    # The pressurizer on loop 2's hot leg: its level, pressure and level figures.
    pu_, pv0, pv1, pw = PZR
    b.capsule(pu_, pv0, pv1, pw, OUTLINE, 0.012)
    b.level("pzr.level", 0, 100, pu_ - pw / 2 + 0.01, pv0 + 0.01, pu_ + pw / 2 - 0.01, pv1 - 0.01)
    b.flow("dw.rcs_flow", [(pu_, pv0 - 0.05), (pu_, HOT_V + LW / 2)], 0.01, RCS, cap1=False)
    b.label(pu_ - 0.12, pv1 + 0.02, "PRESSURIZER", 0.036, LABEL, PANEL, "r")
    b.text("dw.pzr_p", pu_ - 0.12, pv1 - 0.09, 0.055, "r", 0.5)
    b.text("dw.pzr_l", pu_ - 0.12, pv1 - 0.2, 0.055, "r", 0.5)
    # The ECCS: each train's charging, safety injection and RHR pumps, the RWST, its status.
    eu0, eu1, ev0, ev1 = MU0 + 0.04, MU0 + 0.82, 0.05, 0.30
    b.rect(eu0, ev0, eu1, ev1, (28, 34, 44), Z_BOX)
    b.label(eu0 + 0.04, ev1 - 0.045, "ECCS", 0.034, TITLE, (28, 34, 44))
    b.text("dw.si_state", eu1 - 0.04, ev1 - 0.045, 0.04, "r", 0.5)
    for k, (name, sig) in enumerate((("CCP", "ccp"), ("SI", "sip"), ("RHR", "rhr"))):
        u = eu0 + 0.14 + k * 0.25
        for t, tr in enumerate(("a", "b")):
            b.pump(u - 0.05 + t * 0.1, 0.16, f"{sig}_{tr}.run", 0.028)
        b.label(u, 0.085, name, 0.028, LABEL, (28, 34, 44), "c")
    # The reactor's state: at power, shut down, tripped and why.
    b.rect(TURB_U - 0.02, 0.05, TURB_U + 0.6, 0.30, (28, 34, 44), Z_BOX)
    b.label(TURB_U + 0.02, 0.255, "REACTOR", 0.034, TITLE, (28, 34, 44))
    b.text("dw.rx_state", TURB_U + 0.02, 0.16, 0.05, "l", 0.56)
    b.text("dw.rx_cause", TURB_U + 0.02, 0.085, 0.036, "l", 0.56)


# The right two columns: the electrical lineup and the trends.
EU0, EU1 = 5 * CUBE_W + 0.04, PW - 0.04
SWYD_V, BUS_V = 2.17, 1.74


def right_zone(b):
    b.panel(EU0, 1.08, EU1, ZONE_TOP, "ELECTRICAL")
    # The switchyard, the main transformer and the generator's breaker; the reserve auxiliary transformers down to the
    # safety buses; on each bus its diesel's breaker and the diesel, its load centre's breaker.
    b.flow("st.offsite", [(EU0 + 0.08, SWYD_V), (EU1 - 0.08, SWYD_V)], 0.022, ELEC)
    b.label(EU0 + 0.5, SWYD_V + 0.06, "345 KV SWITCHYARD", 0.032, UNIT, PANEL)
    b.text("dw.swyd_kv", EU1 - 0.08, SWYD_V + 0.06, 0.042, "r", 0.4)
    gx = EU0 + 0.22
    b.flow("gen.bkr", [(gx, SWYD_V - 0.011), (gx, SWYD_V - 0.07)], 0.014, ELEC, cap0=False)
    b.xfmr(gx, SWYD_V - 0.13, 0.036)
    b.flow("gen.bkr", [(gx, SWYD_V - 0.19), (gx, SWYD_V - 0.23)], 0.014, ELEC)
    b.breaker(gx, SWYD_V - 0.27, "gen.bkr", 0.022)
    b.label(gx + 0.05, SWYD_V - 0.13, "MAIN XFMR", 0.028, UNIT, PANEL)
    b.label(gx + 0.05, SWYD_V - 0.27, "GEN BKR", 0.028, UNIT, PANEL)
    b.flow("st.gen_online", [(gx, SWYD_V - 0.3), (gx, SWYD_V - 0.36)], 0.014, ELEC)
    b.lamp_poly("st.gen_online", b.circle_pts(gx, SWYD_V - 0.42, 0.05, 20), Z_SYM, (60, 30, 30), (26, 46, 34))
    b.ring(gx, SWYD_V - 0.42, 0.054, 0.01, OUTLINE, Z_RING, 20)
    b.text("dw.gen_g", gx, SWYD_V - 0.42, 0.045, "c", 0.07, (230, 232, 236))
    b.text("dw.gen_mw", gx + 0.08, SWYD_V - 0.39, 0.042, "l", 0.33)
    b.text("dw.gen_hz", gx + 0.08, SWYD_V - 0.46, 0.038, "l", 0.33)
    for x, tag, sig, bus in ((EU0 + 0.95, "RAT A", "swyd.rat_a", "a"), (EU0 + 1.7, "RAT B", "swyd.rat_b", "b")):
        b.flow("st.offsite", [(x, SWYD_V - 0.011), (x, SWYD_V - 0.07)], 0.014, ELEC, cap0=False)
        b.xfmr(x, SWYD_V - 0.13, 0.036)
        b.label(x + 0.05, SWYD_V - 0.13, tag, 0.028, UNIT, PANEL)
        b.flow(f"bus.rat_{bus}", [(x, SWYD_V - 0.19), (x, BUS_V + 0.09)], 0.014, ELEC)
        b.breaker(x, BUS_V + 0.06, f"bus.rat_{bus}", 0.02)
        b.flow(f"bus.rat_{bus}", [(x, BUS_V + 0.032), (x, BUS_V + 0.011)], 0.014, ELEC, cap1=False)
    for k, (u0, u1, bus, edg) in enumerate(((EU0 + 0.66, EU0 + 1.15, "1a", "a"), (EU0 + 1.35, EU1 - 0.08, "1b", "b"))):
        b.flow(f"dw.bus{bus}", [(u0, BUS_V), (u1, BUS_V)], 0.022, ELEC)
        b.label(u0 + (0.0 if k == 0 else 0.6), BUS_V + 0.05, f"BUS {bus.upper()}", 0.03, LABEL, PANEL)
        ex = u0 + 0.12 if k == 0 else u1 - 0.36
        b.flow(f"edg_{edg}.bkr", [(ex, BUS_V - 0.011), (ex, BUS_V - 0.1)], 0.014, ELEC, cap0=False)
        b.breaker(ex, BUS_V - 0.13, f"edg_{edg}.bkr", 0.02)
        b.flow(f"edg_{edg}.bkr", [(ex, BUS_V - 0.155), (ex, BUS_V - 0.2)], 0.014, ELEC)
        b.lamp_poly(f"edg_{edg}.running", b.circle_pts(ex, BUS_V - 0.25, 0.045, 20), Z_SYM)
        b.ring(ex, BUS_V - 0.25, 0.048, 0.01, OUTLINE, Z_RING, 20)
        b.label(ex + 0.07, BUS_V - 0.22, f"EDG 1{edg.upper()}", 0.03, LABEL, PANEL)
        b.text(f"dw.edg_{edg}_kw", ex + 0.07, BUS_V - 0.29, 0.04, "l", 0.3)
        lx = u1 - 0.1 if k == 0 else u0 + 0.12
        b.flow(f"bus.lc_{bus}", [(lx, BUS_V - 0.011), (lx, BUS_V - 0.1)], 0.014, ELEC, cap0=False)
        b.breaker(lx, BUS_V - 0.13, f"bus.lc_{bus}", 0.02)
        b.flow(f"bus.lc_{bus}", [(lx, BUS_V - 0.155), (lx, BUS_V - 0.205)], 0.014, ELEC, cap1=False)
        b.poly([(lx - 0.02, BUS_V - 0.205), (lx, BUS_V - 0.235), (lx + 0.02, BUS_V - 0.205)], ELEC, Z_RING)
        b.label(lx + 0.04, BUS_V - 0.13, f"LC {bus.upper()}", 0.028, UNIT, PANEL)
    # The low voltage systems' status: the load centre, the MCCs, the DC buses.
    rows = [("4160V BUS 1A", "dw.bus1a_kv"), ("4160V BUS 1B", "dw.bus1b_kv"), ("480V LC", "dw.lc"), ("MCC 1A", "dw.mcc_a"),
            ("MCC 1B", "dw.mcc_b"), ("125 VDC A", "dw.dc_a"), ("125 VDC B", "dw.dc_b"), ("120 VAC VITAL", "dw.vital")]
    b.rect(EU0 + 0.04, 1.11, EU1 - 0.04, 1.42, (28, 34, 44), Z_BOX)
    for k, (name, tid) in enumerate(rows):
        u = EU0 + 0.08 + (k % 4) * 0.56
        v = 1.365 - (k // 4) * 0.145
        b.label(u, v, name, 0.026, UNIT, (28, 34, 44))
        b.text(tid, u, v - 0.055, 0.038, "l", 0.5)
    # The trends.
    b.panel(EU0, 0.04, EU1, 1.02, "TRENDS  LAST 5 MIN")
    charts = [("REACTOR POWER", "%", "pr.avg", 0, 120, (240, 150, 60)), ("PZR PRESSURE", "PSIG", "pzr.press", 1700, 2500, (120, 198, 238)),
              ("TAVG", "°F", "rcs.tavg", 540, 600, (226, 120, 200))]
    for k, (name, unit, value, lo, hi, rgb) in enumerate(charts):
        top = 0.86 - k * 0.28
        u0, u1, v0, v1 = EU0 + 0.56, EU1 - 0.06, top - 0.22, top
        b.rect(u0, v0, u1, v1, BG, Z_BOX)
        for g in range(1, 5):
            gu = u0 + (u1 - u0) * g / 5
            b.rect(gu - 0.002, v0, gu + 0.002, v1, (36, 42, 52), Z_LINE)
        for g in range(1, 3):
            gv = v0 + (v1 - v0) * g / 3
            b.rect(u0, gv - 0.002, u1, gv + 0.002, (36, 42, 52), Z_LINE2)
        b.frame(u0, v0, u1, v1, 0.006, EDGE, Z_RING)
        b.label(EU0 + 0.06, top - 0.04, name, 0.03, LABEL, PANEL)
        b.label(EU0 + 0.06, top - 0.09, unit, 0.026, UNIT, PANEL)
        b.text(f"dw.trend{k}", EU0 + 0.06, top - 0.17, 0.05, "l", 0.34, rgb)
        b.label(u0 - 0.01, v1 - 0.01, f"{hi:g}", 0.022, UNIT, PANEL, "r")
        b.label(u0 - 0.01, v0 + 0.012, f"{lo:g}", 0.022, UNIT, PANEL, "r")
        x0, y0 = b.xy(u0 + 0.01, v0 + 0.01)
        x1, y1 = b.xy(u1 - 0.01, v1 - 0.01)
        b.live["pens"].append({"x0": round(x0, 4), "y0": round(y0, 4), "x1": round(x1, 4), "y1": round(y1, 4), "z": round(Z_LABEL, 4),
                               "h": True, "pens": [{"v": value, "lo": lo, "hi": hi, "c": solid(rgb)}]})


def plant_display_wall(seed=1201):
    with own_atlas():
        b = Wall(seed)
        cabinet(b)
        screens(b)
        banner(b)
        left_zone(b)
        mimic(b)
        right_zone(b)
        return b


def legend_image():
    ATLAS.items.clear()
    plant_display_wall()
    return ATLAS.pack()


C.SECTIONS[KEY] = plant_display_wall
