"""The plant overview screens made live (round 34; Justin, 15 October 2026: "Overview screens live", "Live site map",
"Dark display style", all four extras: "Alarm strip", "Emergency class", "Radiation readings", "Power trend"; and
"Click a building for detail").

The plant_map_screen (tools/plant_office2.py, 3 wide and 2 high on a wall: the control room's east wall, the
administration building's board room, the plant manager's office) is a slim large-format LCD showing the medium plant
live, drawn like the plant display wall (tools/display_wall.py): flat-colour geometry on its own legend texture
(textures/entity/overview_screen_legends.png, 512 px a block), its fixed words on that texture and its live words in
the game's font (client/OverviewScreenRenderer.java). Its pages, one board section each in control_boards.json.gz:

    plant_map_screen           what every page shows: the title bar (the unit, the emergency class banner while one
                               is declared, the plant's mode, the time and the data link's heartbeat) and the alarm
                               strip (the control room boards' newest alarms, flashing until acknowledged at their
                               board, and their count)
    plant_map_screen/map       the site from above in its real layout (tools/medium_plant.py medium_site): each
                               building filled by what runs in it (green running, light grey standby, amber in alarm,
                               red tripped or on fire), its tag with its live figures and radiation badge; the main
                               steam, feedwater, auxiliary feed, safety injection, generator output, offsite power and
                               circulating water lines bright while they flow; the cooling towers' throats white with
                               steam while they run; beside it the plant status and the power trend (last 5 minutes)
    plant_map_screen/<page>    a building's own page (clicked on the map; 30 seconds, then back to the map): reactor,
                               turbine, electrical, diesels, control, aux, fuel, cooling
    plant_map_screen/nosignal  no control room within reach: the screen says so

Picture coordinates: u across from the active area's left, v up from its bottom (blocks); the screen's board
coordinates are x across from the screen's left edge, y up from its bottom, z the depth from the front of its blocks
toward the wall (the glass is 1.9 px off the wall). Live entries, beyond display_wall's:

    levels   "h": true for a bar filling left to right
    hits     the map's buildings and the pages' back button: where a click goes (page, rectangle in x, y)
"""
import contextlib
import math

from PIL import Image, ImageDraw

import control_boards as C  # (first: it brings in board_live, diesel_set and display_wall in their order)
import board_live as L
import overview_display as D

ATLAS = L.Atlas()


@contextlib.contextmanager
def own_atlas():
    keep = L.ATLAS
    L.ATLAS = ATLAS
    try:
        yield
    finally:
        L.ATLAS = keep


KEY = "plant_map_screen"
W, H = 3, 2
# The active area inside the display's bezel (tools/overview_display.py; model pixels across the whole screen, 48 x 32).
PANEL_EDGE, BEZEL, BEZEL_BOTTOM, GLASS_PX = D.PANEL_EDGE, D.BEZEL, D.BEZEL_BOTTOM, D.GLASS_PX
X0 = (PANEL_EDGE + BEZEL) / 16
Y0 = (PANEL_EDGE + BEZEL_BOTTOM) / 16
PW = (48 - PANEL_EDGE - BEZEL) / 16 - X0
PH = (32 - PANEL_EDGE - BEZEL) / 16 - Y0
ZG = 0.8812                     # the glass's depth (board z: 1 - 1.9 / 16, kept to four places)
LAYERS = ["bg", "panel", "box", "road", "buried", "fill", "detail", "line", "line2", "sym", "ring", "ring2", "tag", "label", "text"]
Z = {name: round(ZG - (k + 1) / 1000, 4) for k, name in enumerate(LAYERS)}

# The picture's colours (as the display wall's: dark ground, muted lines, colour kept for state and alarms).
BG = (14, 17, 22)
PANEL = (23, 28, 36)
BAR = (26, 33, 44)
EDGE = (56, 64, 78)
TITLE = (120, 198, 238)
LABEL = (168, 176, 188)
UNIT = (124, 132, 146)
DIM = (52, 58, 68)
INK = (236, 240, 246)
OUTLINE = (150, 158, 170)
GROUND = (17, 21, 27)
PROTECTED = (21, 26, 33)
LAKE = (20, 34, 52)
LAKE_INK = (70, 110, 150)
ROAD = (33, 38, 46)
FENCE = (64, 72, 86)
STEAM = (198, 206, 218)
FEED = (110, 168, 222)
ECCS = (170, 150, 214)
POWER = (236, 206, 92)
OFFSITE = (214, 214, 200)
WATER = (52, 124, 196)
# Building fills by state (0 no data, 1 standby, 2 running, 3 in alarm, 4 tripped or on fire), and the chips' brighter
# colours of the same states; buildings with no plant of their own a plain neutral.
FILL_RGB = [(28, 33, 42), (46, 52, 64), (22, 60, 40), (96, 70, 20), (100, 28, 26)]
CHIP_RGB = [(60, 66, 76), (214, 218, 224), (60, 200, 100), (240, 176, 40), (236, 64, 52)]
NEUTRAL = (36, 41, 51)
NEUTRAL_INK = (134, 142, 156)
# The emergency classes' banner colours (none, unusual event, alert, site area emergency, general emergency).
CLASS_RGB = [BAR, (214, 218, 224), (240, 176, 40), (244, 120, 32), (214, 40, 36)]
# The pages' symbols: running, closed or open to flow; stopped, open or shut.
ON_RGB, OFF_RGB = (52, 184, 96), (68, 74, 86)
# Radiation badges: no monitor, normal, alert, high.
RAD_RGB = [(60, 66, 76), (60, 200, 100), (240, 176, 40), (236, 64, 52)]
FONT_PX = 0.72                  # DejaVu's capital height, a fraction of its size

PAGES = ["map", "reactor", "turbine", "electrical", "diesels", "control", "aux", "fuel", "cooling", "nosignal"]
PAGE_TITLES = {"reactor": "CONTAINMENT  ·  REACTOR AND REACTOR COOLANT SYSTEM",
               "turbine": "TURBINE BUILDING  ·  TURBINE GENERATOR AND FEEDWATER",
               "electrical": "SWITCHYARD  ·  ELECTRICAL DISTRIBUTION",
               "diesels": "DIESEL BUILDING  ·  EMERGENCY DIESEL GENERATORS",
               "control": "CONTROL BUILDING  ·  HVAC, FIRE PROTECTION, EMERGENCY RESPONSE",
               "aux": "AUXILIARY BUILDING  ·  SAFETY INJECTION AND AUXILIARY FEEDWATER",
               "fuel": "FUEL BUILDING  ·  SPENT FUEL, CASKS AND RADWASTE",
               "cooling": "INTAKE AND COOLING TOWERS  ·  CIRCULATING WATER"}
PAGE_SECONDS = 30
# The values the screen keeps the last five minutes of (client/OverviewScreenRenderer.java draws each trend's pens
# from them; nuclear/OverviewScreenBlockEntity.java samples them every five seconds): BoardPlant.value names, and
# the screen's own (nuclear/OverviewScreen.java value).
PEN_VALUES = ["pr.avg", "gen.mw", "rcs.tavg", "pzr.press", "turb.speed", "swyd.kv", "sip.flow", "afw.total", "ov.cw_flow", "turb.vacuum"]

# The picture's zones (v): the title bar, the body, the alarm strip.
HEAD = (PH - 0.115, PH)
BODY = (0.185, PH - 0.135)
STRIP = (0.0, 0.165)
# The site map: the site seen from above, x east and z south (tools/nuclear_plants.py Site), the lake's near edge.
SITE_Z = (-20, 330)
MAP_SC = (BODY[1] - BODY[0]) / (SITE_Z[1] - SITE_Z[0])
MU0 = 0.02
SIDE = (1.75, PW - 0.02)


def mu(x):
    return MU0 + x * MAP_SC


def mv(z):
    return BODY[1] - (z - SITE_Z[0]) * MAP_SC


def solid(rgb):
    return L.solid_tex(rgb)


def label_tex(text, h, ink, bg, bold=True):
    """A fixed word on the picture: its capitals h blocks high, ink on the ground it sits on (no transparency); returns
    the material, its width and height (blocks)."""
    size = max(8, int(round(h * L.LEGEND_PX / FONT_PX)))
    f = L._font(size, bold)
    probe = ImageDraw.Draw(Image.new("RGBA", (4, 4)))
    tw = probe.textlength(text, font=f)
    pw, ph = int(math.ceil(tw)) + 6, int(round(h * L.LEGEND_PX * 1.5)) + 4

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, img.width - 1, img.height - 1], fill=tuple(bg) + (255,))
        d.text((3, img.height / 2), text, font=f, fill=tuple(ink) + (255,), anchor="lm")
    key = f"ov:{size}:{'b' if bold else 'r'}:{ink}:{bg}:{text}"
    return ATLAS.add(key, pw, ph, draw), pw / L.LEGEND_PX, ph / L.LEGEND_PX


def label_width(text, h, bold=True):
    size = max(8, int(round(h * L.LEGEND_PX / FONT_PX)))
    probe = ImageDraw.Draw(Image.new("RGBA", (4, 4)))
    return (probe.textlength(text, font=L._font(size, bold)) + 6) / L.LEGEND_PX


class Screen(L.LiveBoard):
    """One page of the screen: picture coordinates (u, v) to the screen's board coordinates."""

    def __init__(self, title, seed=1301):
        super().__init__(W, seed, title, face=ZG)
        for k in ("texts", "levels", "fills", "flows", "alarms", "hits"):
            self.live[k] = []

    @staticmethod
    def xy(u, v):
        return X0 + u, Y0 + v

    # -- flat shapes (all lit: the screen gives its own light)

    def rect(self, u0, v0, u1, v1, rgb, z):
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        self.front_quad(x0, y0, x1, y1, Z[z], solid(rgb) if isinstance(rgb, tuple) else rgb, self.lit)

    def fan(self, pts, z):
        """A convex polygon's corners (u, v anticlockwise) as quads (tl, bl, br, tr as the viewer sees them)."""
        p = [self.xy(*q) for q in pts]
        if len(p) % 2:
            a, b = p[-1], p[0]
            p.append(((a[0] + b[0]) / 2, (a[1] + b[1]) / 2))
        for k in range(1, len(p) - 2, 2):
            a, b, c, d = p[0], p[k], p[k + 1], p[k + 2]
            yield (a[0], a[1], Z[z]), (b[0], b[1], Z[z]), (c[0], c[1], Z[z]), (d[0], d[1], Z[z])

    def poly(self, pts, rgb, z, lst=None):
        lst = self.lit if lst is None else lst
        mat = solid(rgb) if isinstance(rgb, tuple) else rgb
        for q in self.fan(pts, z):
            self.quad(*q, mat, lst)

    def segment(self, a, b, w, rgb, z, lst=None, cap_a=True, cap_b=True, skip_a=False):
        """A straight line a to b, w wide: a free end capped half its width past its point; skip_a starts it half its
        width past a (a corner's next leg)."""
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
        n = len(pts) - 1
        for i, (a, b) in enumerate(zip(pts, pts[1:])):
            self.segment(a, b, w, rgb, z, lst, cap_a=cap0 if i == 0 else False, cap_b=cap1 if i == n - 1 else True, skip_a=i > 0)

    def line(self, pts, w, rgb, z="line", cap0=True, cap1=True):
        self.polyline(pts, w, rgb, z, None, cap0, cap1)

    def flow(self, signal, pts, w, rgb, z="line", cap0=True, cap1=True, arrow=None):
        """A line bright while its signal is true, dim while not; arrow: an arrowhead at its last point."""
        on, off = solid(rgb), solid(DIM)
        lst = []
        self.polyline(pts, w, on, z, lst, cap0, cap1)
        if arrow:
            (u0, v0), (u1, v1) = pts[-2], pts[-1]
            ln = math.hypot(u1 - u0, v1 - v0)
            du, dv = (u1 - u0) / ln, (v1 - v0) / ln
            s = arrow
            tip = (u1 + du * s * 0.9, v1 + dv * s * 0.9)
            back = (u1 - du * s * 0.4, v1 - dv * s * 0.4)
            self.poly([tip, (back[0] - dv * s * 0.7, back[1] + du * s * 0.7), back, (back[0] + dv * s * 0.7, back[1] - du * s * 0.7)][::1],
                      on, "line2", lst)
        for op in lst:
            self.live["flows"].append({"s": signal, "q": op[1:13], "on": on, "off": off})

    def frame(self, u0, v0, u1, v1, w, rgb, z):
        self.rect(u0, v0, u1, v0 + w, rgb, z)
        self.rect(u0, v1 - w, u1, v1, rgb, z)
        self.rect(u0, v0 + w, u0 + w, v1 - w, rgb, z)
        self.rect(u1 - w, v0 + w, u1, v1 - w, rgb, z)

    def outline(self, pts, w, rgb):
        """A closed outline round pts, its sides on the two outline layers in turn (corners never share a plane)."""
        mat = solid(rgb)
        for i in range(len(pts)):
            self.segment(pts[i], pts[(i + 1) % len(pts)], w, mat, "ring" if i % 2 == 0 else "ring2")

    @staticmethod
    def circle_pts(cu, cv, r, n=28):
        return [(cu + r * math.cos(2 * math.pi * k / n), cv + r * math.sin(2 * math.pi * k / n)) for k in range(n)]

    def disc(self, cu, cv, r, rgb, z, n=28):
        self.poly(self.circle_pts(cu, cv, r, n), rgb, z)

    def ring(self, cu, cv, r, w, rgb, z, n=28):
        mat = solid(rgb)
        for k in range(n):
            a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
            pts = [(cu + (r - w / 2) * math.cos(a0), cv + (r - w / 2) * math.sin(a0)), (cu + (r + w / 2) * math.cos(a0), cv + (r + w / 2) * math.sin(a0)),
                   (cu + (r + w / 2) * math.cos(a1), cv + (r + w / 2) * math.sin(a1)), (cu + (r - w / 2) * math.cos(a1), cv + (r - w / 2) * math.sin(a1))]
            self.poly(pts, mat, z)

    # -- state

    def fill_poly(self, fid, pts, z, rgbs):
        """A patch coloured by state: fid's state picks its colour from rgbs (client: OverviewScreen.fill)."""
        mats = [solid(c) for c in rgbs]
        for q in self.fan(pts, z):
            self.live["fills"].append({"id": fid, "q": L._flat(*q), "m": mats})

    def fill_rect(self, fid, u0, v0, u1, v1, z, rgbs):
        self.fill_poly(fid, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], z, rgbs)

    def lamp_poly(self, signal, pts, z, on, off):
        mon, moff = solid(on), solid(off)
        for q in self.fan(pts, z):
            self.live["lamps"].append({"s": signal, "q": L._flat(*q), "on": mon, "off": moff, "off_lit": True})

    def lamp_rect(self, signal, u0, v0, u1, v1, z, on, off):
        self.lamp_poly(signal, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], z, on, off)

    def chip(self, fid, u, v, s=0.024, rgbs=CHIP_RGB, z="label"):
        """A small square state chip, its middle at (u, v)."""
        self.fill_rect(fid, u - s / 2, v - s / 2, u + s / 2, v + s / 2, z, rgbs)

    def hit(self, page, u0, v0, u1, v1, name):
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        self.live["hits"].append({"page": page, "name": name, "x0": round(x0, 4), "y0": round(y0, 4), "x1": round(x1, 4), "y1": round(y1, 4)})

    # -- words

    def label(self, u, v, text, h=0.03, ink=LABEL, bg=PANEL, anchor="l", bold=True, z="label"):
        """A fixed word, its capitals' middle at v; anchor l, c or r at u. Returns its width."""
        mat, w, hh = label_tex(text, h, ink, bg, bold)
        x0 = u - (0 if anchor == "l" else w / 2 if anchor == "c" else w) - 3 / L.LEGEND_PX
        x, y = self.xy(x0, v - hh / 2)
        self.front_quad(x, y, x + w, y + hh, Z[z], mat, self.lit)
        return w

    def text(self, tid, u, v, h=0.04, anchor="l", widest=None, colour=INK):
        """Words the game writes (client/OverviewScreenRenderer.java), its capitals' middle at v."""
        x, y = self.xy(u, v)
        e = {"id": tid, "x": round(x, 4), "y": round(y, 4), "z": Z["text"], "h": round(h, 4), "a": anchor, "c": list(colour)}
        if widest:
            e["w"] = round(widest, 4)
        self.live["texts"].append(e)

    def panel(self, u0, v0, u1, v1, title=None, h=0.036):
        self.rect(u0, v0, u1, v1, PANEL, "panel")
        self.frame(u0, v0, u1, v1, 0.005, EDGE, "box")
        if title:
            self.label(u0 + 0.035, v1 - 0.045, title, h, TITLE)

    def level(self, value, lo, hi, u0, v0, u1, v1, rgb=WATER, horizontal=False, back=BG, z="box", down=False):
        """A bar filling with its value (lo..hi): up, left to right, or (down) down from its top (a control rod)."""
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        if back:
            self.rect(u0, v0, u1, v1, back, z)
        e = {"v": value, "lo": lo, "hi": hi, "x0": round(x0, 4), "y0": round(y0, 4), "x1": round(x1, 4), "y1": round(y1, 4),
             "z": Z[LAYERS[LAYERS.index(z) + 1]], "m": solid(rgb)}
        if horizontal:
            e["h"] = True
        if down:
            e["d"] = True
        self.live["levels"].append(e)

    def pens(self, u0, v0, u1, v1, pens, z="sym"):
        x0, y0 = self.xy(u0, v0)
        x1, y1 = self.xy(u1, v1)
        self.live["pens"].append({"x0": round(x0, 4), "y0": round(y0, 4), "x1": round(x1, 4), "y1": round(y1, 4), "z": Z[z], "h": True,
                                  "pens": [{"v": v, "i": PEN_VALUES.index(v), "lo": lo, "hi": hi, "c": solid(rgb)} for v, lo, hi, rgb in pens]})

    # -- no z-fighting

    def faces(self):
        """Every flat face the page draws, as (its corners' (x, y), its z, a setter for its z): its fixed quads, then its
        live ones (lamps, flows, fills, the levels' bars at full, the alarm slots)."""
        out = []

        def op_setter(op):
            def put(z):
                for i in (3, 6, 9, 12):
                    op[i] = z
            return put

        def q_setter(e):
            def put(z):
                for i in (2, 5, 8, 11):
                    e["q"][i] = z
            return put

        def key_setter(e):
            def put(z):
                e["z"] = z
            return put
        for op in self.lit + self.still:
            if op[0] == "q":
                out.append(([(op[1], op[2]), (op[4], op[5]), (op[7], op[8]), (op[10], op[11])], op[3], op_setter(op)))
        for key in ("lamps", "flows", "fills"):
            for e in self.live[key]:
                q = e["q"]
                out.append(([(q[0], q[1]), (q[3], q[4]), (q[6], q[7]), (q[9], q[10])], q[2], q_setter(e)))
        for e in self.live["levels"]:
            out.append(([(e["x0"], e["y1"]), (e["x0"], e["y0"]), (e["x1"], e["y0"]), (e["x1"], e["y1"])], e["z"], key_setter(e)))
        for e in self.live["alarms"]:
            for x0, y0, x1, y1 in e["slots"]:
                out.append(([(x0, y1), (x0, y0), (x1, y0), (x1, y1)], e["z"], key_setter(e)))
        return out

    def settle(self, under=()):
        """No two faces in one plane where they overlap (Justin: never any z-fighting): a face overlapping one drawn
        before it on its layer (or a face of `under`, the frame it is drawn over) moves a fifth of a layer nearer, up
        to four times, as tools/plant_interior.py separate nudges a model's faces."""
        import overview_zcheck as ZC
        placed = {}

        def put(poly, z):
            poly = ZC._ccw(poly)
            box = (min(p[0] for p in poly), min(p[1] for p in poly), max(p[0] for p in poly), max(p[1] for p in poly))
            placed.setdefault(round(z, 4), []).append((poly, box))

        def free(poly, z):
            poly = ZC._ccw(poly)
            box = (min(p[0] for p in poly), min(p[1] for p in poly), max(p[0] for p in poly), max(p[1] for p in poly))
            for other, ob in placed.get(round(z, 4), []):
                if box[2] <= ob[0] + 1e-9 or ob[2] <= box[0] + 1e-9 or box[3] <= ob[1] + 1e-9 or ob[3] <= box[1] + 1e-9:
                    continue
                cut = poly
                for k in range(len(other)):
                    cut = ZC._clip(cut, other[k], other[(k + 1) % len(other)])
                    if not cut:
                        break
                if cut and ZC._area(cut) > ZC.EPS:
                    return False
            return True
        for poly, z, _ in under:
            if abs(ZC._area(poly)) > 1e-10:
                put(poly, z)
        for poly, z, setter in self.faces():
            if abs(ZC._area(poly)) < 1e-10:
                continue
            k = 0
            while not free(poly, round(z - k * 0.0002, 4)) and k < 4:
                k += 1
            z1 = round(z - k * 0.0002, 4)
            if k:
                setter(z1)
            put(poly, z1)

    # -- symbols: this screen's convention is its map's (green running, closed or open to flow; grey stopped)

    def pump(self, cu, cv, signal, r=0.034):
        """A pump: its ring and discharge, the middle green running and grey stopped."""
        self.lamp_poly(signal, self.circle_pts(cu, cv, r - 0.006, 20), "sym", ON_RGB, OFF_RGB)
        self.ring(cu, cv, r, 0.008, OUTLINE, "ring", 20)
        self.segment((cu, cv + r), (cu + r * 1.1, cv + r), 0.008, OUTLINE, "ring2")

    def valve(self, cu, cv, signal, s=0.028, vertical=False):
        """A valve: a bowtie, green open and grey shut."""
        e = 0.004
        if vertical:
            halves = ([(cu - s, cv + s), (cu - e, cv), (cu + e, cv), (cu + s, cv + s)], [(cu - s, cv - s), (cu + s, cv - s), (cu + e, cv), (cu - e, cv)])
        else:
            halves = ([(cu - s, cv - s), (cu, cv - e), (cu, cv + e), (cu - s, cv + s)], [(cu + s, cv - s), (cu + s, cv + s), (cu, cv + e), (cu, cv - e)])
        for pts in halves:
            self.lamp_poly(signal, pts, "sym", ON_RGB, OFF_RGB)

    def breaker(self, cu, cv, signal, s=0.024):
        """A breaker: a square, green closed and grey open, in a light frame."""
        self.lamp_poly(signal, [(cu - s, cv - s), (cu + s, cv - s), (cu + s, cv + s), (cu - s, cv + s)], "sym", ON_RGB, OFF_RGB)
        self.frame(cu - s - 0.007, cv - s - 0.007, cu + s + 0.007, cv + s + 0.007, 0.007, OUTLINE, "ring")

    def xfmr(self, cu, cv, r=0.034, rgb=OFFSITE):
        self.ring(cu, cv + r * 0.7, r, 0.008, rgb, "ring", 20)
        self.ring(cu, cv - r * 0.7, r, 0.008, rgb, "ring2", 20)

    def generator(self, cu, cv, signal, r=0.05, h=None):
        """A generator: its ring, the middle green while it turns, its G."""
        self.lamp_poly(signal, self.circle_pts(cu, cv, r - 0.006, 24), "sym", (30, 110, 64), (40, 46, 56))
        self.ring(cu, cv, r, 0.009, OUTLINE, "ring", 24)
        self.text("ov.g", cu, cv, h or r, "c", r * 1.4, INK)

    def lamp(self, signal, cu, cv, rgb, r=0.017):
        """A round indicating lamp, lit in its colour while its signal is true."""
        self.lamp_poly(signal, self.circle_pts(cu, cv, r, 16), "sym", rgb, (40, 44, 52))
        self.ring(cu, cv, r + 0.003, 0.004, (70, 78, 92), "ring", 16)

    def tank(self, u0, v0, u1, v1, value, name, tid, rgb=WATER):
        """A tank: its outline, its level live, its name over it and its level under it."""
        self.rect(u0, v0, u1, v1, (24, 30, 40), "box")
        self.level(value, 0, 100, u0 + 0.012, v0 + 0.012, u1 - 0.012, v1 - 0.012, rgb, back=None, z="box")
        self.frame(u0, v0, u1, v1, 0.006, OUTLINE, "ring")
        self.label((u0 + u1) / 2, v1 + 0.03, name, 0.024, LABEL, PANEL, "c")
        self.text(tid, (u0 + u1) / 2, v0 - 0.035, 0.028, "c", u1 - u0 + 0.08)

# ================================================================ the frame every page shows

def frame_section(b):
    """The active area's ground, the title bar and the alarm strip."""
    b.live["penValues"] = list(PEN_VALUES)
    b.live["pages"] = [p for p in PAGES if p != "map"]
    b.rect(0, 0, PW, PH, BG, "bg")
    # The title bar: the unit and the screen's name; the emergency class banner while one is declared; the plant's
    # mode, the time and the data link's heartbeat (lit a moment each second while the screen hears its boards).
    v0, v1 = HEAD
    vm = (v0 + v1) / 2
    b.rect(0, v0, PW, v1, BAR, "panel")
    b.rect(0, v0, PW, v0 + 0.004, EDGE, "box")
    w = b.label(0.035, vm, "UNIT 1", 0.034, TITLE, BAR)
    b.label(0.035 + w + 0.02, vm, "PLANT OVERVIEW", 0.042, INK, BAR)
    b.fill_rect("ov.class", 0.95, v0 + 0.014, 1.98, v1 - 0.014, "box", CLASS_RGB)
    b.text("ov.class", 1.465, vm, 0.044, "c", 0.98)
    b.text("ov.mode", 2.2, vm, 0.034, "l", 0.3, LABEL)
    b.text("ov.clock", PW - 0.09, vm, 0.044, "r", 0.3)
    b.lamp_poly("ov.beat", b.circle_pts(PW - 0.05, vm, 0.012, 16), "box", (60, 220, 110), (30, 52, 40))
    # The alarm strip: the count, and the newest four alarms of the control room's boards.
    s0, s1 = STRIP
    b.panel(0.02, s0 + 0.012, 0.44, s1 - 0.004)
    b.label(0.055, s1 - 0.05, "ALARMS", 0.032, TITLE)
    b.text("ov.alarm_count", 0.055, s0 + 0.055, 0.036, "l", 0.36)
    slots = []
    n, gap, left = 4, 0.02, 0.46
    sw = (PW - 0.02 - left - (n - 1) * gap) / n
    for i in range(n):
        u0 = left + i * (sw + gap)
        u1 = u0 + sw
        b.rect(u0, s0 + 0.012, u1, s1 - 0.004, PANEL, "panel")
        b.frame(u0, s0 + 0.012, u1, s1 - 0.004, 0.005, EDGE, "box")
        x0, y0 = b.xy(u0 + 0.007, s0 + 0.019)
        x1, y1 = b.xy(u1 - 0.007, s1 - 0.011)
        slots.append([round(x0, 4), round(y0, 4), round(x1, 4), round(y1, 4)])
    mats = {"red": solid((200, 40, 32)), "amber": solid((226, 150, 30)), "red_dim": solid((90, 24, 20)), "amber_dim": solid((102, 68, 16))}
    b.live["alarms"].append({"slots": slots, "z": Z["sym"], "tz": Z["text"], "mats": mats})


def page_bar(b, page):
    """A building page's own bar under the title bar: back to the map, the page's name, how long it stays."""
    v1 = BODY[1]
    v0 = v1 - 0.085
    vm = (v0 + v1) / 2
    b.rect(0.02, v0, PW - 0.02, v1, PANEL, "panel")
    b.frame(0.02, v0, PW - 0.02, v1, 0.005, EDGE, "box")
    # The back button: a raised key.
    b.rect(0.032, v0 + 0.012, 0.33, v1 - 0.012, (44, 54, 70), "line")
    b.frame(0.032, v0 + 0.012, 0.33, v1 - 0.012, 0.004, (96, 110, 130), "line2")
    b.poly([(0.052, vm), (0.075, vm - 0.018), (0.075, vm + 0.018)], INK, "line2")
    b.label(0.09, vm, "SITE MAP", 0.032, INK, (44, 54, 70))
    b.hit("map", 0.032, v0 + 0.012, 0.33, v1 - 0.012, "SITE MAP")
    b.label(0.37, vm, PAGE_TITLES[page], 0.034, TITLE)
    b.text("ov.page_left", PW - 0.05, vm, 0.03, "r", 0.5, LABEL)
    return v0 - 0.02


# ================================================================ the site map

def site():
    """The medium plant as tools/medium_plant.py lays it out: its parts' footprints by key (x0, z0, x1, z1), the
    roads, the conduits, the fence round the protected area."""
    import nuclear_plants as P
    s = P.medium_plant()
    parts = {}
    for part, x, z, rot in s.parts:
        fw, fd = P.footprint(part, rot)
        key = part.key
        if key in parts:
            key = key + "_2"
        parts[key] = (x, z, x + fw, z + fd)
        if getattr(part, "overview", None):
            ox0, oz0, ox1, oz1 = part.overview
            parts[key] = (x + ox0, z + oz0, x + ox1, z + oz1)
    return parts, list(s.roads), list(s.pipes)


# Each building: its page, its state's fill (None: a building with no plant of its own, plain), its tag's name, live
# lines and radiation badge, where the tag goes (site x, z of its top left; None: in its middle) and its shape.
BUILDINGS = {
    "pwr_containment": dict(page="reactor", fill="ov.b.reactor", name="REACTOR", lines=["ov.t.reactor"], rad="ov.rad.cntmt", shape="circle",
                            tag=(12, 39), lead=((54, 47), (61.5, 51))),
    "pwr_turbine_hall": dict(page="turbine", fill="ov.b.turbine", name="TURBINE BLDG", lines=["ov.t.turbine"], rad="ov.rad.turb",
                             tag=(170, 65.6), width=0.245),
    "switchyard_medium": dict(page="electrical", fill="ov.b.grid", name="345 KV SWITCHYARD", lines=["ov.t.grid"]),
    "pwr_transformer_yard": dict(page="electrical", fill="ov.b.xfmr", name=None),
    "lake_intake": dict(page="cooling", fill="ov.b.intake", name="INTAKE", lines=["ov.t.intake"], tag=(148, 2)),
    "cw_pump_house": dict(page="cooling", fill="ov.b.cw", name="CIRC WATER", lines=["ov.t.cw"]),
    "aux_building": dict(page="aux", fill="ov.b.aux", name="AUX BLDG", lines=["ov.t.aux"], rad="ov.rad.aux"),
    "radwaste_building": dict(page="fuel", fill=None, name="RAD|WASTE"),
    "fuel_building": dict(page="fuel", fill="ov.b.fuel", name="FUEL", lines=["ov.t.fuel"], rad="ov.rad.fuel"),
    "control_building": dict(page="control", fill="ov.b.control", name="CONTROL", lines=["ov.t.control"], rad="ov.rad.cr"),
    "diesel_building": dict(page="diesels", fill=None, name=None),
    "rwst_tank": dict(page="aux", fill="ov.b.rwst", name="RWST", lines=["ov.t.rwst"], shape="circle", tag=(3, 81.5), oneline=True),
    "condensate_tank": dict(page="aux", fill="ov.b.cst", name="CST", lines=["ov.t.cst"], shape="circle", tag=(213, 135.5), oneline=True),
    "diesel_fuel_tank": dict(page="diesels", fill=None, name=None, shape="circle"),
    "ct_medium": dict(page="cooling", fill="ov.b.tower", name="COOLING TOWER 1", lines=["ov.t.tower"], shape="tower"),
    "ct_medium_2": dict(page="cooling", fill="ov.b.tower", name="COOLING TOWER 2", lines=["ov.t.tower"], shape="tower"),
    "admin_building": dict(page=None, fill=None, name="ADMINISTRATION"),
    "security_gatehouse": dict(page=None, fill=None, name="GATE"),
    "warehouse": dict(page=None, fill=None, name="WAREHOUSE"),
}
NAMES = {"pwr_containment": "Containment", "pwr_turbine_hall": "Turbine Building", "switchyard_medium": "Switchyard",
         "pwr_transformer_yard": "Main Transformer Yard", "lake_intake": "Lake Intake", "cw_pump_house": "Circ Water Pump House", "aux_building": "Auxiliary Building",
         "radwaste_building": "Radwaste Building", "fuel_building": "Fuel Building", "control_building": "Control Building",
         "diesel_building": "Diesel Building", "rwst_tank": "Refuelling Water Storage Tank", "condensate_tank": "Condensate Storage Tank",
         "diesel_fuel_tank": "Diesel Fuel Oil Storage Tank", "ct_medium": "Cooling Tower 1", "ct_medium_2": "Cooling Tower 2",
         "admin_building": "Administration Building", "security_gatehouse": "Security Gatehouse", "warehouse": "Warehouse"}
TAG_NAME_H, TAG_LINE_H = 0.016, 0.023
TAG_ROW, TAG_LINE = 0.031, 0.03
RAD_ICON = 0.022


def tag_size(name, lines, rad=False, chip=True, oneline=False):
    w = label_width(name, TAG_NAME_H) + (0.026 if chip else 0.012) + (RAD_ICON + 0.01 if rad else 0) + 0.008
    if oneline:
        w += 0.075
    h = 0.006 + TAG_ROW + (0 if oneline else len(lines) * TAG_LINE) + 0.004
    return w, h


def rad_icon(b, fid, cu, cv, s=RAD_ICON):
    """A radiation badge: a square coloured by its monitors' state (none, normal, alert, high) with the trefoil on it."""
    b.fill_rect(fid, cu - s / 2, cv - s / 2, cu + s / 2, cv + s / 2, "label", RAD_RGB)
    ink = (14, 16, 20)
    r0, r1 = s * 0.12, s * 0.44
    for k in range(3):
        a = math.pi / 2 + k * 2 * math.pi / 3
        pts = [(cu + r0 * math.cos(a + t), cv + r0 * math.sin(a + t)) for t in (-0.52, 0.52)]
        pts += [(cu + r1 * math.cos(a + t), cv + r1 * math.sin(a + t)) for t in (0.52, 0.26, 0.0, -0.26, -0.52)]
        b.poly(pts, ink, "text")
    b.disc(cu, cv, s * 0.08, ink, "text", 10)


def tag(b, u0, v1, name, lines, rad=None, chip=None, width=None, oneline=False):
    """A building's tag: a dark box (top left u0, v1) with its state chip, its name and radiation badge, its live
    lines under them (or, oneline, its one figure beside the name). Returns the box (u0, v0, u1, v1)."""
    w, h = tag_size(name, lines, bool(rad), bool(chip), oneline)
    w = width or w
    u1, v0 = u0 + w, v1 - h
    b.rect(u0, v0, u1, v1, PANEL, "tag")
    b.frame(u0, v0, u1, v1, 0.003, EDGE, "label")
    row = v1 - 0.006 - TAG_ROW / 2
    nu = u0 + 0.01
    if chip:
        b.chip(chip, u0 + 0.015, row, 0.013)
        nu = u0 + 0.026
    nw = b.label(nu, row, name, TAG_NAME_H, LABEL)
    if rad:
        rad_icon(b, rad, u1 - 0.007 - RAD_ICON / 2, row)
    if oneline:
        b.text(lines[0], u1 - 0.01, row, TAG_LINE_H, "r", u1 - nu - nw - 0.02)
    else:
        for k, tid in enumerate(lines):
            b.text(tid, u0 + 0.01, row - TAG_ROW / 2 - TAG_LINE / 2 - k * TAG_LINE + 0.002, TAG_LINE_H, "l", w - 0.02)
    return u0, v0, u1, v1


def map_section(b):
    parts, roads, pipes = site()
    x_max = 372
    # The ground: outside the fence, the protected area inside it (its double fence dashed), the lake to the north.
    b.rect(mu(0), mv(330), mu(x_max), mv(0), GROUND, "panel")
    b.rect(mu(0), mv(0), mu(x_max), mv(SITE_Z[0]), LAKE, "panel")
    b.label(mu(28), mv(-10), "LAKE", 0.03, LAKE_INK, LAKE)
    # (its east side steps out round the transformer yard north of the diesel building: medium_plant.fence)
    b.rect(mu(0), mv(146), mu(250), mv(26), PROTECTED, "box")
    b.rect(mu(250), mv(96), mu(286), mv(26), PROTECTED, "box")
    dash, gapd = 6, 4
    for (xa, za), (xb, zb) in (((0, 26), (286, 26)), ((286, 26), (286, 96)), ((286, 96), (250, 96)), ((250, 96), (250, 146)),
                               ((250, 146), (128, 146)), ((118, 146), (0, 146)), ((0, 146), (0, 26))):
        ln = math.hypot(xb - xa, zb - za)
        t = 0.0
        while t < ln:
            t1 = min(ln, t + dash)
            pa = (xa + (xb - xa) * t / ln, za + (zb - za) * t / ln)
            pb = (xa + (xb - xa) * t1 / ln, za + (zb - za) * t1 / ln)
            b.segment((mu(pa[0]), mv(pa[1])), (mu(pb[0]), mv(pb[1])), 0.004, FENCE, "road", cap_a=False, cap_b=False)
            t += dash + gapd
    b.label(mu(3), mv(30), "PROTECTED AREA", 0.018, (88, 96, 110), PROTECTED)
    for x0, z0, x1, z1 in roads:
        b.rect(mu(x0), mv(z1), mu(min(x1, x_max)), mv(z0), ROAD, "road")
    # The car park (its bays' lines).
    b.rect(mu(160), mv(200), mu(230), mv(165), (24, 29, 36), "road")
    for x in range(164, 230, 6):
        b.rect(mu(x), mv(182), mu(x) + 0.002, mv(167), (44, 50, 60), "fill")
        b.rect(mu(x), mv(198), mu(x) + 0.002, mv(184), (44, 50, 60), "fill")
    b.label(mu(195), mv(182.5), "PARKING", 0.016, (88, 96, 110), (24, 29, 36), "c")
    # The flows, first: they run under the buildings' tags.
    flows(b, pipes)
    tags = []
    for key, info in BUILDINGS.items():
        x0, z0, x1, z1 = parts[key]
        u0, u1, v0, v1 = mu(x0), mu(x1), mv(z1), mv(z0)
        shape = info.get("shape", "rect")
        if shape in ("circle", "tower"):
            cu, cv, r = (u0 + u1) / 2, (v0 + v1) / 2, (u1 - u0) / 2
            if shape == "tower":
                # A natural draught tower from above: its shell's lip, the throat inside, white with steam while it runs.
                b.fill_poly(info["fill"], b.circle_pts(cu, cv, r, 40), "fill", [FILL_RGB[0], FILL_RGB[1], (30, 56, 52), FILL_RGB[3], FILL_RGB[4]])
                b.ring(cu, cv, r, 0.006, OUTLINE, "ring", 40)
                b.ring(cu, cv, r * 0.62, 0.004, (96, 104, 118), "ring", 40)
                b.lamp_poly("ov.plume", b.circle_pts(cu, cv, r * 0.6, 40), "sym", (150, 158, 168), (22, 26, 33))
                # The steam rising out of it, drifting.
                for k, (dx, dy, rr, rgb) in enumerate(((0.12, 0.1, 0.4, (176, 182, 192)), (0.25, 0.22, 0.2, (214, 218, 226)))):
                    pts = [(cu + r * dx + r * rr * 1.25 * math.cos(a) * math.cos(0.6) - r * rr * 0.8 * math.sin(a) * math.sin(0.6),
                            cv + r * dy + r * rr * 1.25 * math.cos(a) * math.sin(0.6) + r * rr * 0.8 * math.sin(a) * math.cos(0.6))
                           for a in (2 * math.pi * j / 28 for j in range(28))]
                    b.lamp_poly("ov.plume", pts, "ring" if k == 0 else "ring2", rgb, (22, 26, 33))
            elif info["fill"]:
                b.fill_poly(info["fill"], b.circle_pts(cu, cv, r, 32), "fill", FILL_RGB)
                b.ring(cu, cv, r, 0.006, OUTLINE, "ring", 32)
            else:
                b.disc(cu, cv, r, NEUTRAL, "fill", 24)
                b.ring(cu, cv, r, 0.005, OUTLINE, "ring", 24)
        elif key == "diesel_building":
            diesels(b, u0, v0, u1, v1)
        else:
            if info["fill"]:
                b.fill_rect(info["fill"], u0, v0, u1, v1, "fill", FILL_RGB)
            else:
                b.rect(u0, v0, u1, v1, NEUTRAL, "fill")
            b.outline([(u0, v0), (u1, v0), (u1, v1), (u0, v1)], 0.005, OUTLINE)
        if info["page"]:
            b.hit(info["page"], u0, v0, u1, v1, NAMES[key])
        if info.get("name") and not info["fill"] and "lines" not in info:
            # A building with no plant of its own: its name on its roof.
            words = info["name"].split("|")
            for k, word in enumerate(words):
                b.label((u0 + u1) / 2, (v0 + v1) / 2 + (len(words) / 2 - k - 0.5) * 0.038, word, 0.018, NEUTRAL_INK, NEUTRAL, "c", z="tag")
        elif info.get("name"):
            tags.append((key, info, (u0, v0, u1, v1)))
    interiors(b)
    for key, info, (u0, v0, u1, v1) in tags:
        one = info.get("oneline", False)
        w, h = tag_size(info["name"], info["lines"], bool(info.get("rad")), True, one)
        w = max(w, info.get("width", 0.15 if not one else 0))
        if info.get("tag"):
            tag(b, mu(info["tag"][0]), mv(info["tag"][1]), info["name"], info["lines"], info.get("rad"), info["fill"], w, one)
            if info.get("lead"):
                (xa, za), (xb, zb) = info["lead"]
                b.segment((mu(xa), mv(za)), (mu(xb), mv(zb)), 0.003, OUTLINE, "line", cap_a=False)
        else:
            cu, cv = (u0 + u1) / 2, (v0 + v1) / 2
            tag(b, cu - w / 2, cv + h / 2, info["name"], info["lines"], info.get("rad"), info["fill"], w, one)
    # The transformer yard: its name beside it, on the ground.
    x0, z0, x1, z1 = parts["pwr_transformer_yard"]
    b.label(mu(x0) - 0.002, mv(z0) + 0.016, "MAIN XFMR", 0.016, NEUTRAL_INK, PROTECTED, "l")
    x0, z0, x1, z1 = parts["diesel_fuel_tank"]
    b.label((mu(x0) + mu(x1)) / 2, mv(z1) - 0.014, "FUEL OIL", 0.014, NEUTRAL_INK, PROTECTED, "c")
    legend(b)
    north(b)
    side_column(b)


DETAIL = (78, 90, 106)


def interiors(b):
    """What stands inside the buildings, faint, as a site plan shows it: the reactor and its four loops, the turbine
    generator set, the main transformer, the switchyard's buses and bays."""
    for cx, cz, r in ((85.5, 65.5, 3.5), (75.5, 55.5, 3.5), (75.5, 75.5, 3.5), (95.5, 55.5, 3.5), (95.5, 75.5, 3.5), (85, 55.5, 2)):
        b.ring(mu(cx), mv(cz), r * MAP_SC, 0.0035, DETAIL, "detail", 16)
    for x0, x1, h in ((160, 170, 3.5), (170, 182, 5.5), (182, 194, 5.5), (194, 206, 4.5), (206, 210, 2.5)):
        u0, u1 = mu(x0) + 0.0015, mu(x1) - 0.0015
        b.frame(u0, mv(59.5 + h), u1, mv(59.5 - h), 0.0035, DETAIL, "detail")
    # The transformer yard (transformer yard and switchyard build: tools/yard_sites.py): the isolated phase bus along
    # z 59.5 from the turbine hall, the main transformers MT-1A, 1B, 1C and the spare in their row north of it, the unit
    # auxiliary transformers under it; the switchyard's bay 1 (breaker, disconnect, line entry) on the strain bus's
    # line, SAT-1 in its bay.
    b.rect(mu(224), mv(60.2), mu(258.1), mv(58.8), DETAIL, "detail")
    for o in (236.7, 250.1, 263.5, 276.9):
        b.rect(mu(o - 8.45), mv(49.7), mu(o - 0.55), mv(45.9), (84, 90, 70), "detail")
    for o in (247.0, 238.0):
        b.rect(mu(o - 5.4), mv(61.1), mu(o - 0.6), mv(57.9), (84, 90, 70), "detail")
    for z in (43.8, 47.8, 51.8):
        b.rect(mu(231.5), mv(z) - 0.0015, mu(311.5), mv(z) + 0.0015, DETAIL, "detail")
    for x0, x1 in ((287.1, 291.9), (295.0, 300.2), (300.0, 311.5)):
        b.frame(mu(x0), mv(53.0), mu(x1), mv(41.0), 0.0025, DETAIL, "line")
    b.frame(mu(290.7), mv(80.1), mu(304.8), mv(69.1), 0.0025, DETAIL, "line")
    b.rect(mu(293.9), mv(76.1), mu(301.9), mv(72.1), (84, 90, 70), "detail")


def diesels(b, u0, v0, u1, v1):
    """The diesel building: its two generator rooms, each filled by its diesel's state with its tag."""
    um = (u0 + u1) / 2
    for k, (a0, a1, edg) in enumerate(((u0, um, "a"), (um, u1, "b"))):
        b.fill_rect(f"ov.b.edg_{edg}", a0, v0, a1, v1, "fill", FILL_RGB)
        w, h = tag_size("EDG 1A", ["x"])
        w = max(w, 0.13)
        tag(b, (a0 + a1) / 2 - w / 2, (v0 + v1) / 2 + h / 2, f"EDG 1{edg.upper()}", [f"ov.t.edg_{edg}"], None, f"ov.b.edg_{edg}", width=w)
    b.outline([(u0, v0), (u1, v0), (u1, v1), (u0, v1)], 0.005, OUTLINE)
    b.rect(um - 0.002, v0 + 0.005, um + 0.002, v1 - 0.005, OUTLINE, "line")


def flows(b, pipes):
    """The plant's lines on the map, bright while they flow."""
    w = 0.009
    # Main steam out of the containment's pipe bridge to the turbine's inlet; feedwater back from its pump.
    b.flow("ov.steam", [(mu(110), mv(56)), (mu(150), mv(56)), (mu(150), mv(59.5)), (mu(157), mv(59.5))], w + 0.003, STEAM, arrow=0.016)
    b.flow("ov.feed", [(mu(150), mv(70)), (mu(110.5), mv(70))], w, FEED, arrow=0.014)
    # Auxiliary feed and safety injection, up out of the auxiliary building into the containment.
    for sig, x, rgb in (("ov.si", 66, ECCS), ("ov.afw", 76, FEED)):
        edge = 65.5 + math.sqrt(25.5 ** 2 - (x - 85.5) ** 2)
        b.flow(sig, [(mu(x), mv(95)), (mu(x), mv(edge - 1.5))], w, rgb, cap1=False)
    # The generator's output through the main transformer to the switchyard; offsite power back to the plant's buses.
    # (the isolated phase bus out of the hall to the main transformers, the strain bus on to the switchyard's bay 1;
    # SAT-1 in its bay back to the plant's buses: transformer yard and switchyard build)
    b.flow("ov.gen", [(mu(210), mv(59.5)), (mu(240), mv(59.5)), (mu(240), mv(47.8)), (mu(292), mv(47.8))], w + 0.003, POWER, arrow=0.016)
    b.flow("ov.offsite", [(mu(291), mv(76)), (mu(236), mv(76)), (mu(236), mv(86.5)), (mu(166), mv(86.5)), (mu(166), mv(88))], w, OFFSITE)
    # Circulating water: the towers' basins through the circulating water pump house to the condenser and back to the
    # towers (the buried conduit; towers and pump houses build: the lake intake only makes the basins up).
    pts = [(mu(172.5), mv(60)), (mu(172.5), mv(205.5)), (mu(120.5), mv(205.5)), (mu(120.5), mv(232))]
    # (Buried: drawn under the buildings, over the ground and roads.)
    b.flow("ov.cw", pts, w, WATER, cap1=False, z="buried")
    b.flow("ov.cw", [(mu(173.5), mv(205.5)), (mu(250.5), mv(205.5)), (mu(250.5), mv(232))], w, WATER, cap0=False, cap1=False, z="buried")


def legend(b):
    """The map's key, in the corner by the second tower (over the ground: its own layers, the tags')."""
    u0, v0, u1, v1 = 1.425, BODY[0] + 0.012, MU0 + 372 * MAP_SC - 0.01, BODY[0] + 0.44
    b.rect(u0, v0, u1, v1, PANEL, "tag")
    b.frame(u0, v0, u1, v1, 0.005, EDGE, "label")
    b.label(u0 + 0.025, v1 - 0.04, "KEY", 0.024, TITLE)
    rows = [("RUNNING", CHIP_RGB[2]), ("STANDBY", CHIP_RGB[1]), ("ALARM", CHIP_RGB[3]), ("TRIP / FIRE", CHIP_RGB[4])]
    for k, (name, rgb) in enumerate(rows):
        v = v1 - 0.085 - k * 0.042
        b.rect(u0 + 0.025, v - 0.011, u0 + 0.047, v + 0.011, rgb, "label")
        b.label(u0 + 0.06, v, name, 0.019, LABEL)
    lines = [("STEAM", STEAM), ("FEED", FEED), ("POWER", POWER), ("CIRC WATER", WATER)]
    for k, (name, rgb) in enumerate(lines):
        v = v1 - 0.27 - k * 0.036
        b.rect(u0 + 0.022, v - 0.004, u0 + 0.05, v + 0.004, rgb, "label")
        b.label(u0 + 0.06, v, name, 0.017, LABEL)


def north(b):
    """North and the scale, over the lake."""
    cu, cv = MU0 + 372 * MAP_SC - 0.045, BODY[1] - 0.045
    b.poly([(cu, cv + 0.028), (cu - 0.014, cv - 0.016), (cu, cv - 0.006), (cu + 0.014, cv - 0.016)], (170, 190, 210), "line")
    b.label(cu, cv - 0.036, "N", 0.018, (170, 190, 210), LAKE, "c")
    u0 = MU0 + 372 * MAP_SC - 0.36
    b.rect(u0, cv - 0.032, u0 + 100 * MAP_SC, cv - 0.027, (130, 160, 190), "line")
    for k in (0, 50, 100):
        b.rect(u0 + k * MAP_SC - 0.0015, cv - 0.04, u0 + k * MAP_SC + 0.0015, cv - 0.022, (130, 160, 190), "line2")
    b.label(u0 + 50 * MAP_SC, cv - 0.006, "100 M", 0.016, (130, 160, 190), LAKE, "c")


STATUS_ROWS = [("REACTOR POWER", "ov.power", "ov.b.reactor"), ("GENERATOR", "ov.gen_mw", "ov.b.turbine"), ("345 KV GRID", "ov.grid", "ov.b.grid"),
               ("PLANT MODE", "ov.mode_name", None), ("DIESELS", "ov.edgs", "ov.st.edg"), ("CR HVAC", "ov.hvac", "ov.st.hvac"),
               ("FIRE PROTECTION", "ov.fire", "ov.st.fire"), ("SITE RADIATION", "ov.rad", "ov.st.rad"), ("EMERGENCY CLASS", "ov.class_row", "ov.st.class")]


def side_column(b):
    u0, u1 = SIDE
    top = BODY[1]
    split = BODY[0] + 0.66
    b.panel(u0, split + 0.02, u1, top, "PLANT STATUS")
    for k, (name, tid, chip) in enumerate(STATUS_ROWS):
        v = top - 0.1 - k * 0.0905
        if k:
            b.rect(u0 + 0.02, v + 0.045, u1 - 0.02, v + 0.0475, (34, 40, 50), "box")
        if chip:
            b.chip(chip, u0 + 0.04, v, 0.022, CHIP_RGB if not chip.startswith("ov.st.rad") else RAD_RGB)
        b.label(u0 + 0.065, v, name, 0.026, LABEL)
        b.text(tid, u1 - 0.03, v, 0.036, "r", 0.55)
    # The power trend: reactor power and the generator's output over the last five minutes.
    b.panel(u0, BODY[0], u1, split, "POWER  LAST 5 MIN")
    charts = [("REACTOR", "%", "pr.avg", 0, 120, (240, 150, 60), "ov.trend_power"), ("GENERATOR", "MWE", "gen.mw", 0, 1200, POWER, "ov.trend_mw")]
    for k, (name, unit, value, lo, hi, rgb, tid) in enumerate(charts):
        ct = split - 0.075 - k * 0.285
        cu0, cu1, cv0, cv1 = u0 + 0.37, u1 - 0.03, ct - 0.23, ct
        b.rect(cu0, cv0, cu1, cv1, BG, "box")
        for g in range(1, 5):
            gu = cu0 + (cu1 - cu0) * g / 5
            b.rect(gu - 0.0015, cv0, gu + 0.0015, cv1, (34, 40, 50), "road")
        for g in range(1, 4):
            gv = cv0 + (cv1 - cv0) * g / 4
            b.rect(cu0, gv - 0.0015, cu1, gv + 0.0015, (34, 40, 50), "fill")
        b.frame(cu0, cv0, cu1, cv1, 0.004, EDGE, "line")
        b.label(u0 + 0.035, ct - 0.025, name, 0.024, LABEL)
        b.label(u0 + 0.035, ct - 0.07, unit, 0.022, UNIT)
        b.text(tid, u0 + 0.035, ct - 0.15, 0.05, "l", 0.27, rgb)
        b.label(cu0 - 0.008, cv1 - 0.012, f"{hi:g}", 0.018, UNIT, PANEL, "r")
        b.label(cu0 - 0.008, cv0 + 0.012, f"{lo:g}", 0.018, UNIT, PANEL, "r")
        if k == 1:
            for g, word in ((0, "-5 MIN"), (5, "NOW")):
                b.label(cu0 + (cu1 - cu0) * g / 5, cv0 - 0.02, word, 0.016, UNIT, PANEL, "l" if g == 0 else "r")
        b.pens(cu0 + 0.006, cv0 + 0.006, cu1 - 0.006, cv1 - 0.006, [(value, lo, hi, rgb)])


# ================================================================ the buildings' pages

CT = BODY[1] - 0.105            # a page's panels' top (under its bar), and their bottom
CB = BODY[0]
MIM = (0.02, 1.86)              # a page's mimic, and its figures beside it
FIG = (1.89, PW - 0.02)
BOX = (27, 33, 42)              # a box inside a panel
RCS_HOT, RCS_COLD = (226, 120, 90), (110, 168, 222)
STEP_KW = [300, 1500, 1100, 900, 1000, 900, 700, 400]   # the diesels' load sequencer's steps (nuclear/DieselFuel.java)


def figure_rows(b, u0, u1, top, rows, step=0.066, name_h=0.024, value_h=0.031, bg=PANEL):
    """Rows of a name (a state chip before it) and its live figure right aligned, a hairline between."""
    for k, row in enumerate(rows):
        name, tid = row[0], row[1]
        v = top - k * step
        if k:
            b.rect(u0 + 0.015, v + step / 2 - 0.0012, u1 - 0.015, v + step / 2 + 0.0012, (34, 40, 50), "box")
        nu = u0 + 0.03
        if len(row) > 2 and row[2]:
            b.chip(row[2], u0 + 0.036, v, 0.02, row[3] if len(row) > 3 else CHIP_RGB)
            nu = u0 + 0.058
        w = b.label(nu, v, name, name_h, LABEL, bg)
        b.text(tid, u1 - 0.03, v, value_h, "r", u1 - 0.05 - nu - w)
    return top - (len(rows) - 1) * step


def rad_rows(b, u0, u1, top, monitors, step=0.062):
    """Radiation monitors: the badge, RM-n and its name, the reading."""
    for k, (n, name) in enumerate(monitors):
        v = top - k * step
        rad_icon(b, f"ov.rm{n}", u0 + 0.042, v, 0.03)
        w = b.label(u0 + 0.07, v, f"RM-{n}  {name}", 0.023, LABEL)
        b.text(f"ov.rm{n}", u1 - 0.03, v, 0.03, "r", u1 - u0 - w - 0.12)


def chart(b, u0, v0, u1, v1, title, pens):
    """A trend of the last five minutes: its title, each pen's colour, name and live figure, the grid, the traces
    (pens: value, lo, hi, colour, name, figure's id; the first's scale on the left, the second's on the right)."""
    b.label(u0, v1 - 0.016, title, 0.022, TITLE)
    half = (u1 - u0) / len(pens)
    for k, (value, lo, hi, rgb, name, tid) in enumerate(pens):
        lu = u0 + k * half
        b.rect(lu, v1 - 0.061, lu + 0.026, v1 - 0.055, rgb, "line")
        w = b.label(lu + 0.034, v1 - 0.058, name, 0.019, LABEL)
        b.text(tid, lu + 0.044 + w, v1 - 0.058, 0.026, "l", half - w - 0.06, rgb)
    cu0, cu1, cv0, cv1 = u0 + 0.07, u1 - (0.07 if len(pens) > 1 else 0.01), v0 + 0.035, v1 - 0.09
    b.rect(cu0, cv0, cu1, cv1, BG, "box")
    for g in range(1, 5):
        gu = cu0 + (cu1 - cu0) * g / 5
        b.rect(gu - 0.0015, cv0, gu + 0.0015, cv1, (34, 40, 50), "road")
    for g in range(1, 4):
        gv = cv0 + (cv1 - cv0) * g / 4
        b.rect(cu0, gv - 0.0015, cu1, gv + 0.0015, (34, 40, 50), "fill")
    b.frame(cu0, cv0, cu1, cv1, 0.004, EDGE, "line")
    for k, (value, lo, hi, rgb, name, tid) in enumerate(pens[:2]):
        x, a = (cu0 - 0.008, "r") if k == 0 else (cu1 + 0.008, "l")
        b.label(x, cv1 - 0.01, f"{hi:g}", 0.017, tuple(int(c * 0.8) for c in rgb), PANEL, a)
        b.label(x, cv0 + 0.01, f"{lo:g}", 0.017, tuple(int(c * 0.8) for c in rgb), PANEL, a)
    b.label(cu0, cv0 - 0.018, "-5 MIN", 0.015, UNIT, PANEL, "l")
    b.label(cu1, cv0 - 0.018, "NOW", 0.015, UNIT, PANEL, "r")
    b.pens(cu0 + 0.005, cv0 + 0.005, cu1 - 0.005, cv1 - 0.005, [(p[0], p[1], p[2], p[3]) for p in pens])


def figures_panel(b, rows, trend=None, rad=None, title="FIGURES", step=0.066):
    u0, u1 = FIG
    b.panel(u0, CB, u1, CT, title)
    figure_rows(b, u0, u1, CT - 0.105, rows, step)
    bottom = CB + 0.01
    if trend:
        chart(b, u0 + 0.03, CB + 0.03, u1 - 0.03, CB + 0.4, trend[0], trend[1])
        bottom = CB + 0.42
    if rad:
        top = bottom + 0.045 + (len(rad) - 1) * 0.062
        b.label(u0 + 0.035, top + 0.055, "RADIATION", 0.024, TITLE)
        rad_rows(b, u0, u1, top, rad)


def capsule(b, cu, v0, v1, wd, rgb, w=0.008, z="ring"):
    """A vessel's outline: straight sides and elliptical heads."""
    r = wd / 2
    mat = solid(rgb)
    b.segment((cu - r, v0), (cu - r, v1), w, mat, z, cap_a=False, cap_b=False)
    b.segment((cu + r, v0), (cu + r, v1), w, mat, z, cap_a=False, cap_b=False)
    n = 12
    for c, sgn in ((v1, 1), (v0, -1)):
        for k in range(n):
            a0, a1 = math.pi * k / n, math.pi * (k + 1) / n
            p0 = (cu + r * math.cos(a0), c + sgn * r * 0.6 * math.sin(a0))
            p1 = (cu + r * math.cos(a1), c + sgn * r * 0.6 * math.sin(a1))
            b.segment(p0, p1, w, mat, z, cap_a=False, cap_b=False)


def vessel(b, cu, vb, vt):
    """The reactor vessel: its control rod drive housings over the head, the core glowing by power (dark unloaded,
    dull shut down, orange at power, amber hot, red damaged), the five banks' rods hanging into it as far as they
    are in, the hot and cold leg nozzles (bright while the coolant flows)."""
    r = 0.105
    b.rect(cu - r + 0.01, vb + 0.02, cu + r - 0.01, vt - 0.02, (22, 28, 36), "box")
    core0, core1 = vb + 0.12, vb + 0.5
    b.fill_rect("ov.core", cu - 0.075, core0, cu + 0.075, core1, "fill",
                [(34, 26, 22), (92, 46, 30), (236, 146, 50), (240, 176, 40), (226, 54, 44)])
    for k in range(1, 6):
        x = cu - 0.075 + k * 0.025
        b.rect(x - 0.0015, core0, x + 0.0015, core1, (40, 30, 24), "detail")
    for k in range(5):
        x = cu - 0.0625 + k * 0.03125
        b.rect(x - 0.0015, core1, x + 0.0015, vt + 0.02, (96, 104, 118), "road")
        b.level(f"ov.rod{k}", 0, 1, x - 0.005, core0, x + 0.005, core1, (200, 206, 216), back=None, z="detail", down=True)
    for k in range(5):
        x = cu - 0.0625 + k * 0.03125
        b.rect(x - 0.007, vt + 0.02, x + 0.007, vt + 0.1, (60, 68, 80), "box")
        b.frame(x - 0.007, vt + 0.02, x + 0.007, vt + 0.1, 0.0025, OUTLINE, "line")
    capsule(b, cu, vb + 0.04, vt - 0.04, 2 * r, OUTLINE)
    nv = core1 + 0.12
    b.flow("ov.rcs_flow", [(cu + r, nv), (cu + r + 0.12, nv)], 0.022, RCS_HOT, cap0=False, arrow=0.02)
    b.flow("ov.rcs_flow", [(cu - r - 0.12, nv), (cu - r, nv)], 0.022, RCS_COLD, cap1=False, arrow=0.02)
    b.label(cu + r + 0.02, nv + 0.04, "HOT LEG", 0.018, UNIT, PANEL)
    b.label(cu - r - 0.02, nv + 0.04, "COLD LEG", 0.018, UNIT, PANEL, "r")
    b.label(cu, vb - 0.025, "VESSEL AND CORE", 0.02, UNIT, PANEL, "c")


def page_reactor(b):
    page_bar(b, "reactor")
    u0, u1 = MIM
    b.panel(u0, CB, u1, CT, "REACTOR AND REACTOR COOLANT SYSTEM")
    b.text("ov.power_big", 0.06, CT - 0.15, 0.1, "l", 0.5)
    b.label(0.06, CT - 0.245, "REACTOR POWER  %", 0.022, UNIT)
    vessel(b, 0.33, CB + 0.24, CT - 0.42)
    b.label(0.06, CB + 0.11, "TRIP CAUSE", 0.022, LABEL)
    b.text("ov.rx_cause", 0.06, CB + 0.055, 0.028, "l", 0.56)
    # The four loops: each its steam generator (its level live), its pump, its figures.
    lw = (1.84 - 0.66 - 3 * 0.02) / 4
    lt, lb = CT - 0.07, CB + 0.6
    for n in range(1, 5):
        x0 = 0.66 + (n - 1) * (lw + 0.02)
        x1 = x0 + lw
        b.rect(x0, lb, x1, lt, BOX, "box")
        b.frame(x0, lb, x1, lt, 0.004, EDGE, "road")
        b.label(x0 + 0.02, lt - 0.035, f"LOOP {n}", 0.026, TITLE, BOX)
        sx = x0 + 0.07
        s0, s1 = lb + 0.26, lt - 0.12
        b.rect(sx - 0.035, s0 - 0.01, sx + 0.035, s1 + 0.01, (24, 30, 40), "road")
        b.level(f"sg{n}.nr", 0, 100, sx - 0.03, s0, sx + 0.03, s1, WATER, back=None, z="road")
        capsule(b, sx, s0, s1, 0.08, OUTLINE, 0.006)
        b.pump(sx, lb + 0.15, f"rcp{n}.run", 0.034)
        b.label(sx, lb + 0.075, f"RCP 1{'ABCD'[n - 1]}", 0.019, LABEL, BOX, "c")
        b.text(f"ov.rcp{n}", sx, lb + 0.035, 0.024, "c", 0.12)
        for k, (name, tid) in enumerate((("FLOW", f"ov.loop{n}_flow"), ("T HOT", f"ov.loop{n}_thot"), ("T COLD", f"ov.loop{n}_tcold"),
                                         ("SG LEVEL", f"ov.sg{n}_level"), ("SG PRESS", f"ov.sg{n}_press"))):
            v = lt - 0.1 - k * 0.12
            b.label(x0 + 0.13, v, name, 0.018, LABEL, BOX)
            b.text(tid, x1 - 0.015, v - 0.042, 0.027, "r", lw - 0.15)
    # The reactor's state, two columns under the loops; the pressurizer's heaters, spray and relief valves.
    figure_rows(b, 0.64, 1.24, lb - 0.06, [("STATE", "ov.rx_state"), ("MODE", "ov.mode_name"), ("ROD CONTROL", "ov.rods")], 0.066)
    figure_rows(b, 1.26, 1.86, lb - 0.06, [("CONTROL BANK D", "ov.bank_d"), ("STARTUP RATE", "ov.sur"), ("CORE", "ov.fuel_count")], 0.066)
    lv = CB + 0.1
    b.label(0.66, lv, "PRESSURIZER", 0.022, TITLE)
    for k, (name, sig, rgb) in enumerate((("HEATERS", "ov.pzr_htr", (240, 176, 40)), ("SPRAY", "pzr.spray_a", (52, 184, 96)),
                                         ("PORV", "pzr.porv", (236, 64, 52)), ("SAFETIES", "pzr_safety", (236, 64, 52)))):
        cu = 0.94 + k * 0.23
        b.lamp(sig, cu, lv, rgb)
        b.label(cu + 0.03, lv, name, 0.02, LABEL)
    figures_panel(b, [("TAVG / TREF", "ov.tavg"), ("PZR PRESSURE", "ov.pzr_press"), ("PZR LEVEL", "ov.pzr_level"), ("SUBCOOLING", "ov.subcool"),
                      ("CORE EXIT TEMP", "ov.cet"), ("CNTMT PRESSURE", "ov.cnt_press"), ("SAFETY INJECTION", "ov.si", "ov.st.si"),
                      ("CNTMT ISOLATION", "ov.cnt_iso", "ov.st.iso")],
                  ("LAST 5 MIN", [("rcs.tavg", 540, 600, (226, 120, 200), "TAVG", "ov.tr_tavg"), ("pzr.press", 1700, 2500, TITLE, "PZR", "ov.tr_pzr")]),
                  [(2, "CNTMT AREA"), (3, "HIGH RANGE"), (12, "ATMOSPHERE")])


def turbine_body(b, a0, a1, h0, h1, sv, label):
    b.poly([(a0, sv - h0), (a1, sv - h1), (a1, sv + h1), (a0, sv + h0)], (44, 52, 64), "fill")
    b.lamp_poly("ov.turbine", [(a0 + 0.006, sv - h0 + 0.006), (a1 - 0.006, sv - h1 + 0.008), (a1 - 0.006, sv + h1 - 0.008),
                               (a0 + 0.006, sv + h0 - 0.006)], "detail", (34, 112, 66), (40, 46, 56))
    b.label((a0 + a1) / 2, sv + h1 + 0.03, label, 0.02, UNIT, PANEL, "c")


def page_turbine(b):
    page_bar(b, "turbine")
    b.panel(MIM[0], CB, MIM[1], CT, "MAIN STEAM, TURBINE GENERATOR, CONDENSATE AND FEEDWATER")
    sv = CT - 0.36
    b.label(0.06, sv + 0.1, "MAIN STEAM", 0.022, LABEL)
    b.text("ov.steam_press", 0.06, sv + 0.05, 0.026, "l", 0.2)
    b.flow("ov.steam", [(0.06, sv), (0.27, sv)], 0.018, STEAM)
    b.valve(0.3, sv, "turb.valves", 0.028)
    b.label(0.3, sv - 0.06, "STOP / CONTROL", 0.017, UNIT, PANEL, "c")
    b.flow("ov.steam", [(0.33, sv), (0.42, sv)], 0.018, STEAM, cap1=False)
    b.rect(0.6, sv - 0.006, 1.36, sv + 0.006, (110, 118, 130), "road")
    turbine_body(b, 0.42, 0.6, 0.07, 0.11, sv, "HP")
    turbine_body(b, 0.68, 0.94, 0.11, 0.18, sv, "LP A")
    turbine_body(b, 1.0, 1.26, 0.11, 0.18, sv, "LP B")
    b.text("ov.turb_speed", 0.51, sv - 0.16, 0.03, "c", 0.24)
    b.generator(1.42, sv, "st.online", 0.075, 0.06)
    b.text("ov.gen_mw", 1.42, sv - 0.12, 0.03, "c", 0.24)
    b.flow("st.online", [(1.495, sv), (1.58, sv)], 0.016, POWER)
    b.breaker(1.61, sv, "gen.bkr", 0.026)
    b.label(1.61, sv + 0.065, "GEN BKR", 0.019, LABEL, PANEL, "c")
    b.flow("gen.bkr", [(1.643, sv), (1.8, sv)], 0.016, POWER, arrow=0.02)
    b.label(1.73, sv - 0.045, "TO MAIN XFMR", 0.017, UNIT, PANEL, "c")
    # The condenser under the LP turbines, its hotwell; circulating water through it.
    cv = sv - 0.5
    for x in (0.81, 1.13):
        b.flow("ov.steam", [(x, sv - 0.185), (x, cv + 0.11)], 0.012, STEAM, cap1=False)
    b.rect(0.64, cv - 0.12, 1.3, cv + 0.11, (30, 38, 50), "box")
    b.level("turb.hotwell", 0, 100, 0.656, cv - 0.104, 1.284, cv - 0.04, WATER, back=(20, 24, 32), z="road")
    b.frame(0.64, cv - 0.12, 1.3, cv + 0.11, 0.006, OUTLINE, "fill")
    b.label(0.97, cv + 0.06, "CONDENSER", 0.024, LABEL, (30, 38, 50), "c")
    b.text("ov.vacuum", 0.97, cv + 0.005, 0.026, "c", 0.4)
    b.flow("ov.cw", [(0.42, cv + 0.02), (0.64, cv + 0.02)], 0.014, WATER, cap1=False, arrow=None)
    b.label(0.42, cv + 0.065, "CIRC WATER", 0.018, UNIT, PANEL)
    b.flow("ov.cw", [(1.3, cv + 0.02), (1.5, cv + 0.02)], 0.014, WATER, cap0=False, arrow=0.02)
    b.label(1.5, cv + 0.065, "TO TOWERS", 0.018, UNIT, PANEL, "r")
    # Condensate pumps, the feedwater heaters, the main feed pumps, to the steam generators.
    fv = CB + 0.2
    b.flow("ov.condensate", [(1.06, cv - 0.12), (1.06, fv), (0.99, fv)], 0.011, FEED, cap0=False)
    b.pump(0.96, fv, "ov.condensate", 0.03)
    b.label(0.96, fv - 0.065, "COND PUMPS", 0.018, UNIT, PANEL, "c")
    b.flow("ov.condensate", [(0.93, fv), (0.8, fv)], 0.011, FEED)
    b.rect(0.6, fv - 0.06, 0.8, fv + 0.06, (30, 38, 50), "box")
    b.frame(0.6, fv - 0.06, 0.8, fv + 0.06, 0.005, OUTLINE, "fill")
    for k in range(4):
        b.rect(0.62 + k * 0.045, fv - 0.04, 0.65 + k * 0.045, fv + 0.04, (52, 62, 78), "detail")
    b.label(0.7, fv + 0.085, "FW HEATERS", 0.018, UNIT, PANEL, "c")
    b.flow("ov.feed", [(0.6, fv), (0.5, fv)], 0.011, FEED)
    for k, pmp in enumerate(("a", "b")):
        y = fv + 0.075 - k * 0.15
        b.flow(f"mfp_{pmp}.run", [(0.5, fv), (0.5, y), (0.45, y)], 0.011, FEED, cap0=False)
        b.pump(0.42, y, f"mfp_{pmp}.run", 0.03)
        b.label(0.42, y + 0.055 if k == 0 else y - 0.055, f"MFP {pmp.upper()}", 0.019, LABEL, PANEL, "c")
        b.flow(f"mfp_{pmp}.run", [(0.39, y), (0.3, y), (0.3, fv)], 0.011, FEED, cap1=False)
    b.flow("ov.feed", [(0.3, fv), (0.08, fv)], 0.014, FEED, cap0=False, arrow=0.02)
    b.label(0.08, fv + 0.045, "TO SGS", 0.019, UNIT, PANEL)
    b.text("ov.fw_press", 0.08, fv - 0.045, 0.024, "l", 0.2)
    figures_panel(b, [("TURBINE", "ov.turb_state", "ov.b.turbine"), ("TURBINE SPEED", "ov.turb_speed"), ("STEAM HEADER", "ov.steam_press"),
                      ("GENERATOR", "ov.gen_mw"), ("REACTIVE", "ov.mvar"), ("VOLTAGE", "ov.gen_kv"), ("FREQUENCY", "ov.gen_hz"),
                      ("CONDENSER VACUUM", "ov.vacuum"), ("HOTWELL LEVEL", "ov.hotwell"), ("FEED HEADER", "ov.fw_press")],
                  ("LAST 5 MIN", [("turb.speed", 0, 2000, (120, 198, 238), "RPM", "ov.tr_rpm"), ("gen.mw", 0, 1200, POWER, "MWE", "ov.trend_mw")]),
                  [(7, "TURBINE BLDG")])


def battery(b, cu, cv, signal):
    """A station battery: its cells' plates, long and short, under its DC bus."""
    for k in range(4):
        x = cu - 0.06 + k * 0.04
        b.rect(x - 0.002, cv - 0.03, x + 0.002, cv + 0.03, OFFSITE, "line")
        b.rect(x + 0.016, cv - 0.016, x + 0.022, cv + 0.016, OFFSITE, "line")


def page_electrical(b):
    page_bar(b, "electrical")
    b.panel(MIM[0], CB, MIM[1], CT, "ONE LINE")
    sy = CT - 0.16
    b.flow("st.offsite", [(0.08, sy), (1.8, sy)], 0.022, OFFSITE)
    b.label(0.08, sy + 0.045, "345 KV SWITCHYARD", 0.022, LABEL)
    b.text("ov.swyd_kv", 1.8, sy + 0.045, 0.03, "r", 0.4)
    # The generator through its breaker and the main transformer.
    gx = 0.18
    b.flow("gen.bkr", [(gx, sy - 0.011), (gx, sy - 0.09)], 0.014, POWER, cap0=False)
    b.xfmr(gx, sy - 0.15, 0.04, POWER)
    b.label(gx + 0.06, sy - 0.15, "MAIN XFMR", 0.021, LABEL)
    b.flow("gen.bkr", [(gx, sy - 0.21), (gx, sy - 0.3)], 0.014, POWER)
    b.breaker(gx, sy - 0.335, "gen.bkr", 0.024)
    b.label(gx + 0.05, sy - 0.335, "GEN BKR", 0.021, LABEL)
    b.flow("st.online", [(gx, sy - 0.367), (gx, sy - 0.45)], 0.014, POWER)
    b.generator(gx, sy - 0.53, "st.online", 0.075, 0.06)
    b.text("ov.gen_mw", gx + 0.09, sy - 0.505, 0.03, "l", 0.2)
    b.text("ov.gen_hz", gx + 0.09, sy - 0.555, 0.026, "l", 0.2, LABEL)
    # Offsite power through the reserve transformers to the 4160 V safety buses; each bus's diesel and its feed to the
    # load centre; the load centre's motor control centres.
    bus_v = sy - 0.62
    lc_v = bus_v - 0.47
    for x, name, bus in ((0.78, "RAT A", "a"), (1.46, "RAT B", "b")):
        b.flow("st.offsite", [(x, sy - 0.011), (x, sy - 0.09)], 0.014, OFFSITE, cap0=False)
        b.xfmr(x, sy - 0.15, 0.04)
        b.label(x + 0.06, sy - 0.15, name, 0.021, LABEL)
        b.flow(f"bus.rat_{bus}", [(x, sy - 0.21), (x, bus_v + 0.1)], 0.014, OFFSITE)
        b.breaker(x, bus_v + 0.065, f"bus.rat_{bus}", 0.02)
        b.flow(f"bus.rat_{bus}", [(x, bus_v + 0.038), (x, bus_v + 0.011)], 0.014, OFFSITE, cap1=False)
    b.flow("ov.lc_bus", [(0.9, lc_v), (1.82, lc_v)], 0.02, OFFSITE)
    b.label(1.82, lc_v + 0.04, "480 V LOAD CENTRE", 0.02, LABEL, PANEL, "r")
    for k, (u0, u1, bus, edg) in enumerate(((0.52, 1.12, "1a", "a"), (1.2, 1.82, "1b", "b"))):
        b.flow(f"dw.bus{bus}", [(u0, bus_v), (u1, bus_v)], 0.022, OFFSITE)
        b.label(u0, bus_v + 0.04, f"4160 V BUS {bus.upper()}", 0.021, LABEL)
        b.text(f"ov.bus{bus}_kv", u1, bus_v + 0.04, 0.026, "r", 0.18)
        ex = u0 + 0.08
        b.flow(f"edg_{edg}.bkr", [(ex, bus_v - 0.011), (ex, bus_v - 0.1)], 0.014, OFFSITE, cap0=False)
        b.breaker(ex, bus_v - 0.13, f"edg_{edg}.bkr", 0.02)
        b.flow(f"edg_{edg}.running", [(ex, bus_v - 0.157), (ex, bus_v - 0.21)], 0.014, OFFSITE)
        b.generator(ex, bus_v - 0.27, f"edg_{edg}.running", 0.06, 0.045)
        b.label(ex + 0.075, bus_v - 0.245, f"EDG 1{edg.upper()}", 0.021, LABEL)
        b.text(f"ov.edg_{edg}_kw", ex + 0.075, bus_v - 0.295, 0.026, "l", 0.25)
        lx = u1 - 0.08
        b.flow(f"bus.lc_{bus}", [(lx, bus_v - 0.011), (lx, bus_v - 0.1)], 0.014, OFFSITE, cap0=False)
        b.breaker(lx, bus_v - 0.13, f"bus.lc_{bus}", 0.02)
        b.label(lx - 0.04, bus_v - 0.13, f"LC {bus.upper()}", 0.019, LABEL, PANEL, "r")
        b.flow(f"bus.lc_{bus}", [(lx, bus_v - 0.157), (lx, lc_v + 0.011)], 0.014, OFFSITE, cap1=False)
    for x, m, sig in ((1.1, "MCC 1A", "st.mcc_a"), (1.6, "MCC 1B", "st.mcc_b")):
        b.flow(sig, [(x, lc_v - 0.011), (x, lc_v - 0.08)], 0.012, OFFSITE, cap0=False)
        b.breaker(x, lc_v - 0.105, sig, 0.018)
        b.flow(sig, [(x, lc_v - 0.13), (x, CB + 0.12)], 0.012, OFFSITE, cap1=False)
        b.rect(x - 0.11, CB + 0.02, x + 0.11, CB + 0.12, BOX, "box")
        b.frame(x - 0.11, CB + 0.02, x + 0.11, CB + 0.12, 0.004, OUTLINE, "road")
        b.label(x, CB + 0.094, m, 0.02, LABEL, BOX, "c")
        b.text("ov.mcc_" + m[-1].lower(), x, CB + 0.05, 0.024, "c", 0.18)
    # The DC buses and their batteries.
    for k, (name, tid) in enumerate((("125 VDC A", "ov.dc_a"), ("125 VDC B", "ov.dc_b"))):
        x = 0.14 + k * 0.36
        dv = CB + 0.26
        b.flow(f"st.dc_{'ab'[k]}", [(x - 0.08, dv), (x + 0.2, dv)], 0.016, OFFSITE)
        b.label(x - 0.08, dv + 0.04, name, 0.02, LABEL)
        b.flow(f"st.dc_{'ab'[k]}", [(x, dv - 0.008), (x, dv - 0.07)], 0.01, OFFSITE, cap0=False)
        battery(b, x + 0.02, dv - 0.11, f"st.dc_{'ab'[k]}")
        b.text(tid, x + 0.12, dv - 0.11, 0.026, "l", 0.13)
    figures_panel(b, [("345 KV GRID", "ov.grid", "ov.b.grid"), ("MW TO GRID", "ov.mw_out"), ("GENERATOR", "ov.gen_mw"), ("4160 V BUS 1A", "ov.bus1a_kv"),
                      ("4160 V BUS 1B", "ov.bus1b_kv"), ("480 V LOAD CENTRE", "ov.lc"), ("MCC 1A", "ov.mcc_a"), ("MCC 1B", "ov.mcc_b"),
                      ("125 VDC A", "ov.dc_a"), ("125 VDC B", "ov.dc_b"), ("120 VAC VITAL", "ov.vital")],
                  ("LAST 5 MIN", [("swyd.kv", 300, 380, OFFSITE, "KV", "ov.tr_kv"), ("gen.mw", 0, 1200, POWER, "MWE", "ov.trend_mw")]))


def page_diesels(b):
    page_bar(b, "diesels")
    half = (PW - 0.07) / 2
    for k, edg in enumerate("ab"):
        u0 = 0.02 + k * (half + 0.03)
        u1 = u0 + half
        b.panel(u0, CB, u1, CT, f"EMERGENCY DIESEL GENERATOR 1{edg.upper()}  ·  TRAIN {edg.upper()}")
        sv = CT - 0.13
        b.fill_rect(f"ov.b.edg_{edg}", u0 + 0.03, sv - 0.05, u0 + 0.62, sv + 0.045, "box", [FILL_RGB[0], (60, 66, 78), (30, 110, 64), (150, 104, 24), (150, 36, 30)])
        b.text(f"ov.edg_{edg}_state", u0 + 0.325, sv - 0.002, 0.046, "c", 0.56)
        b.text(f"ov.edg_{edg}_since", u0 + 0.66, sv - 0.002, 0.026, "l", u1 - u0 - 0.7, LABEL)
        # The engine and its generator, the output breaker onto its bus.
        mv_ = sv - 0.25
        b.rect(u0 + 0.05, mv_ - 0.075, u0 + 0.44, mv_ + 0.075, (40, 48, 60), "box")
        b.lamp_rect(f"edg_{edg}.running", u0 + 0.058, mv_ - 0.067, u0 + 0.432, mv_ + 0.067, "road", (34, 112, 66), (44, 52, 64))
        for c in range(10):
            cx = u0 + 0.08 + c * 0.034
            b.rect(cx - 0.01, mv_ + 0.025, cx + 0.01, mv_ + 0.058, (70, 78, 92), "fill")
        b.text("ov.engine", u0 + 0.245, mv_ - 0.03, 0.024, "c", 0.3, INK)
        b.rect(u0 + 0.44, mv_ - 0.008, u0 + 0.48, mv_ + 0.008, (110, 118, 130), "box")
        b.generator(u0 + 0.545, mv_, f"edg_{edg}.running", 0.065, 0.05)
        b.flow(f"edg_{edg}.running", [(u0 + 0.61, mv_), (u0 + 0.71, mv_)], 0.014, OFFSITE)
        b.breaker(u0 + 0.74, mv_, f"edg_{edg}.bkr", 0.024)
        b.label(u0 + 0.74, mv_ + 0.06, "OUTPUT BKR", 0.018, LABEL, PANEL, "c")
        b.flow(f"edg_{edg}.bkr", [(u0 + 0.771, mv_), (u0 + 0.9, mv_)], 0.014, OFFSITE)
        b.flow(f"dw.bus1{edg}", [(u0 + 0.9, mv_ + 0.1), (u0 + 0.9, mv_ - 0.1)], 0.022, OFFSITE)
        b.label(u0 + 0.93, mv_ + 0.075, f"4160 V BUS 1{edg.upper()}", 0.02, LABEL)
        b.text(f"ov.bus1{edg}_kv", u0 + 0.93, mv_ - 0.075, 0.026, "l", 0.3)
        # Speed, load and starting air.
        bv = mv_ - 0.17
        for j, (name, value, lo, hi, tid, rgb) in enumerate((("ENGINE SPEED", f"edg_{edg}.rpm", 0, 600, f"ov.edg_{edg}_rpm", (120, 198, 238)),
                                                             ("GENERATOR LOAD", f"edg_{edg}.kw", 0, 7000, f"ov.edg_{edg}_kw", POWER),
                                                             ("STARTING AIR", f"edg_{edg}.air", 0, 250, f"ov.edg_{edg}_air", (170, 190, 210)))):
            v = bv - j * 0.1
            b.label(u0 + 0.04, v + 0.022, name, 0.02, LABEL)
            b.level(value, lo, hi, u0 + 0.04, v - 0.04, u0 + 0.95, v + 0.002, rgb, horizontal=True)
            b.frame(u0 + 0.04, v - 0.04, u0 + 0.95, v + 0.002, 0.003, EDGE, "line2")
            b.text(tid, u1 - 0.03, v - 0.019, 0.032, "r", 0.36)
        # The load sequencer: its eight steps, lit as each picks up its load.
        sq = bv - 0.33
        b.label(u0 + 0.04, sq + 0.045, "LOAD SEQUENCER", 0.02, LABEL)
        b.text(f"ov.edg_{edg}_seq", u1 - 0.03, sq + 0.045, 0.024, "r", 0.4, LABEL)
        for s, kw in enumerate(STEP_KW):
            cu = u0 + 0.08 + s * 0.17
            b.rect(cu - 0.06, sq - 0.025, cu + 0.06, sq + 0.017, BOX, "box")
            b.lamp_rect(f"ov.seq_{edg}{s + 1}", cu - 0.055, sq - 0.02, cu + 0.055, sq + 0.012, "road", (52, 184, 96), (40, 46, 56))
            b.label(cu, sq - 0.05, f"{kw} KW", 0.016, UNIT, PANEL, "c")
        figure_rows(b, u0, u1, sq - 0.12, [("VOLTAGE", f"ov.edg_{edg}_kv"), ("FREQUENCY", f"ov.edg_{edg}_hz"), ("CONTROL", f"ov.edg_{edg}_mode"),
                                            ("DAY TANK", f"ov.edg_{edg}_day")], 0.064)
        b.level(f"edg_{edg}.day", 0, 100, u0 + 0.3, sq - 0.12 - 3 * 0.064 - 0.016, u0 + 0.75, sq - 0.12 - 3 * 0.064 + 0.016, (200, 150, 60), horizontal=True)


def page_control(b):
    page_bar(b, "control")
    w3 = (PW - 0.1) / 3
    cols = [0.02 + k * (w3 + 0.03) for k in range(3)]
    state_rgbs = [FILL_RGB[0], (60, 66, 78), (30, 110, 64), (150, 104, 24), (150, 36, 30)]
    # Control room HVAC: its mode, each train's machines, the intake dampers, the room, its monitors.
    u0 = cols[0]
    u1 = u0 + w3
    b.panel(u0, CB, u1, CT, "CONTROL ROOM HVAC")
    b.fill_rect("ov.st.hvac", u0 + 0.03, CT - 0.18, u1 - 0.03, CT - 0.075, "box", state_rgbs)
    b.text("ov.hvac", (u0 + u1) / 2, CT - 0.127, 0.048, "c", w3 - 0.1)
    b.label(u0 + 0.5, CT - 0.225, "TRAIN A", 0.02, UNIT, PANEL, "c")
    b.label(u0 + 0.72, CT - 0.225, "TRAIN B", 0.02, UNIT, PANEL, "c")
    for k, (name, part) in enumerate((("EMERGENCY FILTRATION", 0), ("AIR HANDLING UNIT", 1), ("CHILLER", 2), ("CHILLED WATER PUMP", 3))):
        v = CT - 0.29 - k * 0.066
        if k:
            b.rect(u0 + 0.015, v + 0.032, u1 - 0.015, v + 0.0344, (34, 40, 50), "box")
        b.label(u0 + 0.03, v, name, 0.021, LABEL)
        for t, x in enumerate((0.5, 0.72)):
            b.lamp(f"ov.hv{part}{'ab'[t]}", u0 + x, v, (52, 184, 96))
    figure_rows(b, u0, u1, CT - 0.6, [("INTAKE DAMPERS", "ov.dampers"), ("ROOM TEMPERATURE", "ov.cr_temp"), ("ROOM PRESSURE", "ov.cr_dp")], 0.066)
    b.label(u0 + 0.035, CB + 0.255, "RADIATION", 0.024, TITLE)
    rad_rows(b, u0, u1, CB + 0.19, [(1, "CONTROL ROOM"), (9, "CR INTAKE A"), (10, "CR INTAKE B")])
    # The cable spreading room's pre-action sprinklers.
    u0 = cols[1]
    u1 = u0 + w3
    b.panel(u0, CB, u1, CT, "CABLE SPREADING ROOM FIRE")
    b.fill_rect("ov.st.fire", u0 + 0.03, CT - 0.18, u1 - 0.03, CT - 0.075, "box", state_rgbs)
    b.text("ov.fire", (u0 + u1) / 2, CT - 0.127, 0.048, "c", w3 - 0.1)
    lamps = [("POWER", "csr_power", (52, 184, 96)), ("SUPERVISORY", "csr_sup", (240, 176, 40)), ("ALARM", "csr_alarm", (236, 64, 52)),
             ("TROUBLE", "csr_trbl", (240, 176, 40)), ("RELEASED", "csr_rel", (236, 64, 52)), ("LOW AIR", "csr_air", (240, 176, 40))]
    for k, (name, sig, rgb) in enumerate(lamps):
        i, j = k % 3, k // 3
        cu, v = u0 + 0.06 + i * 0.29, CT - 0.24 - j * 0.065
        b.lamp(f"ov.{sig}", cu, v, rgb)
        b.label(cu + 0.03, v, name, 0.02, LABEL)
    b.rect(u0 + 0.03, CT - 0.46, u1 - 0.03, CT - 0.35, (12, 24, 16), "box")
    b.frame(u0 + 0.03, CT - 0.46, u1 - 0.03, CT - 0.35, 0.004, (60, 70, 64), "road")
    b.text("ov.csr_line1", u0 + 0.05, CT - 0.38, 0.026, "l", w3 - 0.1, (110, 230, 140))
    b.text("ov.csr_line2", u0 + 0.05, CT - 0.43, 0.026, "l", w3 - 0.1, (110, 230, 140))
    figure_rows(b, u0, u1, CT - 0.53, [("VALVE", "ov.csr_valve"), ("PIPING AIR", "ov.csr_air_psi"), ("SPRINKLER HEADS", "ov.csr_heads"),
                                       ("DETECTORS", "ov.csr_detectors")], 0.066)
    # Its riser: the supply, the pre-action valve, the dry piping up to the heads (water in it once released).
    sv_ = CB + 0.09
    b.flow("ov.csr_supply", [(u0 + 0.05, sv_), (u0 + 0.2, sv_)], 0.012, WATER, cap1=False)
    b.valve(u0 + 0.23, sv_, "ov.csr_open", 0.026)
    b.label(u0 + 0.23, sv_ - 0.05, "PRE-ACTION VALVE", 0.017, UNIT, PANEL, "c")
    b.label(u0 + 0.05, sv_ + 0.035, "SUPPLY", 0.017, UNIT)
    hv = sv_ + 0.2
    b.flow("ov.csr_water", [(u0 + 0.26, sv_), (u0 + 0.36, sv_), (u0 + 0.36, hv), (u1 - 0.05, hv)], 0.01, WATER, cap0=False)
    for h in range(5):
        x = u0 + 0.45 + h * 0.1
        b.flow("ov.csr_water", [(x, hv - 0.005), (x, hv - 0.045)], 0.006, WATER, cap0=False)
        b.poly([(x - 0.016, hv - 0.055), (x, hv - 0.04), (x + 0.016, hv - 0.055)], OUTLINE, "ring")
    b.label(u0 + 0.65, hv + 0.035, "DRY PIPING TO THE HEADS", 0.017, UNIT, PANEL, "c")
    # Emergency response: the class, its clocks and notifications, the wind; the building's own buses.
    u0 = cols[2]
    u1 = u0 + w3
    b.panel(u0, CB, u1, CT, "EMERGENCY RESPONSE")
    b.fill_rect("ov.class", u0 + 0.03, CT - 0.18, u1 - 0.03, CT - 0.075, "box", CLASS_RGB)
    b.text("ov.class_big", (u0 + u1) / 2, CT - 0.127, 0.044, "c", w3 - 0.1)
    figure_rows(b, u0, u1, CT - 0.24, [("DECLARED", "ov.declared"), ("TSC", "ov.tsc"), ("STATE / LOCAL", "ov.notify_state"),
                                       ("NRC", "ov.notify_nrc"), ("WIND", "ov.wind")], 0.066)
    b.label(u0 + 0.035, CT - 0.62, "CONTROL BUILDING BUSES", 0.024, TITLE)
    figure_rows(b, u0, u1, CT - 0.69, [("480 V LOAD CENTRE", "ov.lc"), ("MCC 1A", "ov.mcc_a"), ("MCC 1B", "ov.mcc_b"),
                                       ("125 VDC A", "ov.dc_a"), ("125 VDC B", "ov.dc_b"), ("120 VAC VITAL", "ov.vital")], 0.066)


def page_aux(b):
    page_bar(b, "aux")
    b.panel(MIM[0], CB, MIM[1], CT, "EMERGENCY CORE COOLING AND AUXILIARY FEEDWATER")
    # ECCS: the refuelling water storage tank to the charging, safety injection and RHR pumps, into the RCS.
    et = CT - 0.11
    b.tank(0.07, et - 0.5, 0.25, et - 0.04, "rwst.level", "RWST", "ov.rwst")
    pumps = [("CCP A", "ccp_a.run", "ov.ccp_a"), ("CCP B", "ccp_b.run", "ov.ccp_b"), ("SI A", "sip_a.run", "ov.sip_a"),
             ("SI B", "sip_b.run", "ov.sip_b"), ("RHR A", "rhr_a.run", "ov.rhr_a"), ("RHR B", "rhr_b.run", "ov.rhr_b")]
    p0, step = et - 0.02, 0.1
    b.flow("ov.eccs", [(0.25, et - 0.27), (0.4, et - 0.27)], 0.014, ECCS, cap1=False)
    # (The header's middle: its pumps either side.)
    b.flow("ov.eccs", [(0.4, p0), (0.4, p0 - 5 * step)], 0.014, ECCS)
    for k, (name, sig, tid) in enumerate(pumps):
        v = p0 - k * step
        b.flow(sig, [(0.4, v), (0.5, v)], 0.01, ECCS, cap0=False)
        b.pump(0.535, v, sig, 0.03)
        b.label(0.6, v, name, 0.022, LABEL)
        b.text(tid, 1.08, v, 0.028, "r", 0.36)
        b.flow(sig, [(1.1, v), (1.22, v)], 0.01, ECCS, cap1=False)
    b.flow("ov.eccs_out", [(1.22, p0), (1.22, p0 - 5 * step)], 0.014, ECCS)
    b.flow("ov.eccs_out", [(1.22, et - 0.27), (1.46, et - 0.27)], 0.016, ECCS, cap0=False, arrow=0.02)
    capsule(b, 1.62, et - 0.4, et - 0.14, 0.16, OUTLINE)
    b.label(1.62, et - 0.24, "RCS", 0.026, LABEL, PANEL, "c")
    b.label(1.62, et - 0.3, "COLD LEGS", 0.017, UNIT, PANEL, "c")
    # AFW: the condensate storage tank to the motor and turbine driven pumps, to the steam generators.
    at = et - 0.68
    b.tank(0.07, at - 0.46, 0.25, at - 0.04, "cst.level", "CST", "ov.cst")
    b.flow("ov.afw", [(0.25, at - 0.25), (0.4, at - 0.25)], 0.014, FEED, cap1=False)
    b.flow("ov.afw", [(0.4, at - 0.1), (0.4, at - 0.4)], 0.014, FEED)
    for k, (name, sig, tid) in enumerate((("MD AFW A", "afw_a.run", "ov.afw_a"), ("TD AFW", "afw_td.run", "ov.afw_td"), ("MD AFW B", "afw_b.run", "ov.afw_b"))):
        v = at - 0.1 - k * 0.15
        b.flow(sig, [(0.4, v), (0.5, v)], 0.01, FEED, cap0=False)
        b.pump(0.535, v, sig, 0.03)
        b.label(0.6, v, name, 0.022, LABEL)
        b.text(tid, 1.08, v, 0.028, "r", 0.36)
        b.flow(sig, [(1.1, v), (1.22, v)], 0.01, FEED, cap1=False)
    b.flow("ov.afw", [(1.22, at - 0.1), (1.22, at - 0.4)], 0.014, FEED)
    b.flow("ov.afw", [(1.22, at - 0.25), (1.46, at - 0.25)], 0.016, FEED, cap0=False, arrow=0.02)
    for k in range(4):
        cu = 1.53 + k * 0.08
        capsule(b, cu, at - 0.38, at - 0.12, 0.05, OUTLINE, 0.005)
    b.label(1.65, at - 0.06, "STEAM GENERATORS", 0.019, UNIT, PANEL, "c")
    figures_panel(b, [("SAFETY INJECTION", "ov.si", "ov.st.si"), ("SI FLOW", "ov.si_flow"), ("RHR FLOW", "ov.rhr_flow"), ("CHARGING FLOW", "ov.chg_flow"),
                      ("CCW", "ov.ccw"), ("AFW FLOW", "ov.afw_total"), ("RWST LEVEL", "ov.rwst"), ("CST LEVEL", "ov.cst")],
                  ("LAST 5 MIN", [("sip.flow", 0, 1500, ECCS, "SI GPM", "ov.tr_si"), ("afw.total", 0, 2000, FEED, "AFW GPM", "ov.tr_afw")]),
                  [(6, "AUX BUILDING"), (11, "PLANT VENT")])


def page_fuel(b):
    page_bar(b, "fuel")
    b.panel(MIM[0], CB, MIM[1], CT, "SPENT FUEL POOL, TRANSFER CANAL AND CASKS")
    # The pool from above: the transfer canal from the containment, the racks, the cask pit, the bridge over it all.
    pu0, pv0, pu1, pv1 = 0.24, CB + 0.24, 1.2, CT - 0.15
    b.rect(0.06, pv1 - 0.36, 0.2, pv1 - 0.16, (18, 44, 70), "box")
    b.frame(0.06, pv1 - 0.36, 0.2, pv1 - 0.16, 0.006, OUTLINE, "road")
    b.label(0.13, pv1 - 0.395, "TRANSFER", 0.016, UNIT, PANEL, "c")
    b.label(0.13, pv1 - 0.43, "CANAL", 0.016, UNIT, PANEL, "c")
    b.rect(pu0, pv0, pu1, pv1, (18, 44, 70), "box")
    b.frame(pu0, pv0, pu1, pv1, 0.008, OUTLINE, "road")
    for i in range(11):
        for j in range(9):
            x = pu0 + 0.035 + i * 0.075
            y = pv0 + 0.035 + j * 0.105
            if i >= 9 and j < 3:
                continue
            b.rect(x, y, x + 0.065, y + 0.095, (34, 70, 100), "fill")
            b.frame(x, y, x + 0.065, y + 0.095, 0.003, (70, 110, 150), "detail")
    b.rect(pu1 - 0.17, pv0 + 0.02, pu1 - 0.02, pv0 + 0.3, (14, 34, 54), "fill")
    b.frame(pu1 - 0.17, pv0 + 0.02, pu1 - 0.02, pv0 + 0.3, 0.004, (90, 130, 170), "detail")
    b.label(pu1 - 0.095, pv0 + 0.17, "CASK", 0.016, UNIT, (14, 34, 54), "c")
    b.label(pu1 - 0.095, pv0 + 0.135, "PIT", 0.016, UNIT, (14, 34, 54), "c")
    b.rect(pu0 + 0.43, pv0 - 0.02, pu0 + 0.47, pv1 + 0.02, (150, 130, 60), "sym")
    b.label(pu0 + 0.45, pv1 + 0.035, "FUEL HANDLING BRIDGE", 0.018, UNIT, PANEL, "c")
    b.label((pu0 + pu1) / 2, pv0 - 0.035, "SPENT FUEL POOL  ·  RACKS", 0.019, UNIT, PANEL, "c")
    # The dry cask storage pad.
    b.rect(1.3, CB + 0.6, 1.82, CB + 1.0, (30, 38, 50), "box")
    b.frame(1.3, CB + 0.6, 1.82, CB + 1.0, 0.006, OUTLINE, "road")
    for i in range(4):
        for j in range(2):
            b.disc(1.37 + i * 0.125, CB + 0.7 + j * 0.2, 0.045, (70, 76, 88), "fill", 20)
            b.ring(1.37 + i * 0.125, CB + 0.7 + j * 0.2, 0.045, 0.006, OUTLINE, "detail", 20)
    b.label(1.56, CB + 1.04, "DRY CASK STORAGE PAD", 0.019, UNIT, PANEL, "c")
    b.label(0.06, CB + 0.13, "CORE", 0.024, TITLE)
    b.level("ov.core_loaded", 0, 1, 0.2, CB + 0.11, 1.2, CB + 0.15, (236, 146, 50), horizontal=True)
    b.frame(0.2, CB + 0.11, 1.2, CB + 0.15, 0.003, EDGE, "line2")
    b.text("ov.fuel", 0.2, CB + 0.065, 0.026, "l", 1.0)
    figures_panel(b, [("FUEL IN CORE", "ov.fuel_count"), ("BURNUP", "ov.burnup"), ("REACTOR HEAD", "ov.head"), ("FUEL DAMAGE", "ov.fuel_damage", "ov.st.fuel"),
                      ("CASKS ON PAD", "ov.casks")], None, [(4, "SPENT FUEL POOL"), (5, "FUEL HANDLING"), (8, "CASK STORAGE")])


def page_cooling(b):
    page_bar(b, "cooling")
    b.panel(MIM[0], CB, MIM[1], CT, "CIRCULATING WATER")
    lv = CT - 0.15
    b.rect(0.05, lv - 0.06, 1.83, lv + 0.07, LAKE, "box")
    # (towers and pump houses build: the circulating water pump house draws from its forebay, the towers' basins; the lake
    # intake makes them up)
    b.label(0.08, lv + 0.005, "FOREBAY  ·  TOWER BASINS", 0.024, LAKE_INK, LAKE)
    for k in range(4):
        x = 0.55 + k * 0.25
        b.flow(f"ov.cwp{k}", [(x, lv - 0.06), (x, lv - 0.19)], 0.012, WATER, cap0=False)
        b.pump(x, lv - 0.23, f"ov.cwp{k}", 0.034)
        b.label(x + 0.05, lv - 0.29, f"CWP 1{'ABCD'[k]}", 0.02, LABEL, PANEL, "l")
        b.flow(f"ov.cwp{k}", [(x, lv - 0.265), (x, lv - 0.37)], 0.012, WATER, cap1=False)
    b.flow("ov.cw", [(0.54, lv - 0.37), (1.31, lv - 0.37)], 0.016, WATER)
    b.flow("ov.cw", [(0.93, lv - 0.37), (0.93, lv - 0.47)], 0.016, WATER, cap0=False, cap1=False)
    cv = lv - 0.56
    b.rect(0.7, cv - 0.09, 1.16, cv + 0.09, (30, 38, 50), "box")
    b.frame(0.7, cv - 0.09, 1.16, cv + 0.09, 0.006, OUTLINE, "road")
    b.label(0.93, cv + 0.04, "CONDENSER", 0.024, LABEL, (30, 38, 50), "c")
    b.text("ov.vacuum", 0.93, cv - 0.025, 0.028, "c", 0.4)
    b.flow("ov.cw", [(0.93, cv - 0.09), (0.93, cv - 0.18)], 0.016, WATER, cap1=False)
    tv = CB + 0.2
    b.flow("ov.cw", [(0.36, cv - 0.18), (1.3, cv - 0.18)], 0.016, WATER)
    for k, x in enumerate((0.36, 1.3)):
        b.flow("ov.cw", [(x, cv - 0.18), (x, tv + 0.16)], 0.016, WATER, cap0=False)
        b.fill_poly("ov.b.tower", b.circle_pts(x, tv, 0.16, 36), "fill", [FILL_RGB[0], FILL_RGB[1], (30, 56, 52), FILL_RGB[3], FILL_RGB[4]])
        b.ring(x, tv, 0.16, 0.007, OUTLINE, "ring", 36)
        b.ring(x, tv, 0.1, 0.004, (96, 104, 118), "ring", 36)
        b.lamp_poly("ov.plume", b.circle_pts(x, tv, 0.096, 32), "sym", (150, 158, 168), (22, 26, 33))
        b.lamp_poly("ov.plume", b.circle_pts(x + 0.015, tv + 0.012, 0.055, 24), "ring2", (210, 214, 222), (22, 26, 33))
        b.label(x + 0.19, tv + 0.03, f"COOLING TOWER {k + 1}", 0.022, LABEL)
        b.text("ov.t.tower", x + 0.19, tv - 0.03, 0.028, "l", 0.33)
    figures_panel(b, [("CW PUMPS", "ov.cw_pumps", "ov.b.cw"), ("CIRC WATER FLOW", "ov.cw_flow"), ("CONDENSER VACUUM", "ov.vacuum"),
                      ("TOWER LOAD", "ov.tower_load"), ("TOWERS", "ov.t.tower", "ov.b.tower"), ("LAKE INTAKE", "ov.t.intake", "ov.b.intake")],
                  ("LAST 5 MIN", [("ov.cw_flow", 0, 120, WATER, "FLOW %", "ov.tr_cw"), ("turb.vacuum", 0, 30, (170, 190, 210), "VAC IN HG", "ov.tr_vac")]))


def page_nosignal(b):
    b.rect(0.02, BODY[0], PW - 0.02, BODY[1], PANEL, "panel")
    b.label(PW / 2, BODY[0] + 0.95, "NO PLANT DATA", 0.09, (240, 176, 40), PANEL, "c")
    b.label(PW / 2, BODY[0] + 0.78, "This screen hears no control room boards within 256 blocks.", 0.034, LABEL, PANEL, "c", bold=False)
    b.label(PW / 2, BODY[0] + 0.71, "It shows the plant whose display wall or boards are nearest.", 0.034, LABEL, PANEL, "c", bold=False)


PAGE_MAKERS = {"map": map_section, "reactor": page_reactor, "turbine": page_turbine, "electrical": page_electrical, "diesels": page_diesels,
               "control": page_control, "aux": page_aux, "fuel": page_fuel, "cooling": page_cooling, "nosignal": page_nosignal}


def section(page=None):
    """The board section of a page (None: the frame every page shows), its faces settled (drawn over the frame's)."""
    with own_atlas():
        frame = Screen("PLANT OVERVIEW")
        frame_section(frame)
        frame.settle()
        frame.core = 0
        if page is None:
            return frame
        b = Screen(page.upper())
        PAGE_MAKERS[page](b)
        b.settle(frame.faces())
        b.core = 0
        return b


def legend_image():
    ATLAS.items.clear()
    section()
    for p in PAGES:
        section(p)
    return ATLAS.pack()


C.SECTIONS[KEY] = section
for _p in PAGES:
    C.SECTIONS[f"{KEY}/{_p}"] = (lambda p=_p: section(p))

