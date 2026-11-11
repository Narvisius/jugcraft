"""The medium plant's natural-draft cooling tower as a compiled site (tools/water_works.py): pieces 2 to 7 of
claude/jugcraft-cooling-tower-redesign.md and the columns of piece 1; the shell itself is computed in the game
(nuclear/TowerShell.java, the same math as cooling_tower.Tower.cells), so only its cells are listed here.

Live states: the stair tower's lights (photocell), the shell door and the crossover door, MU-1 open or shut (its
indicator and the panel's lamps), the obstruction lights (off, white, red), the sprays and rain (circulating water
flowing), the panel's BASIN HI and LO lamps and its four meters (needles at 61 places across each scale). The panel's
three LED windows are drawn in glyphs from the game's numbers. The basin's water is the world's own water blocks.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import cooling_tower as CT
import water_works as WW

T = CT.MEDIUM
ANCHOR = (0, -1, -63)            # the controller: just past the outlet sump's back wall, on the trench from the plant
STEPS = 61                        # needle positions across a meter's scale

LADDERS = []
_fixed_ladder = CT.fixed_ladder


def _recording_ladder(F, tower, ya, yb, ra, rb, inside=False, ext=1.07):
    LADDERS.append((tuple(F.w(0.0, ya, ra)), tuple(F.w(0.0, yb, rb)), tuple(-F.r if inside else F.r)))
    return _fixed_ladder(F, tower, ya, yb, ra, rb, inside, ext)


CT.fixed_ladder = _recording_ladder


def meter_values(key):
    lo, hi = next((m[2], m[3]) for m in CT.METERS if m[0] == key)
    return [lo + (hi - lo) * k / (STEPS - 1) for k in range(STEPS)]


BASE_PANEL = CT.panel_state()


def _dial(key):
    base = BASE_PANEL[key]
    vals = meter_values(key)
    return WW.Var("m_" + key, [base] + vals, "dial")


class TowerSite(WW.Site):
    key = "ct_medium"
    exclude = {"ct_water"}

    def __init__(self):
        self.variables = [
            WW.Var("lights", [False, True]),
            WW.Var("door", [False, True]),
            WW.Var("door2", [False, True]),
            WW.Var("mu1", [True, False]),
            WW.Var("beacon", ["off", "white", "red"]),
            WW.Var("running", [True, False]),
            WW.Var("hi", [False, True]),
            WW.Var("lo", [False, True]),
            _dial("level"), _dial("hot"), _dial("cold"), _dial("flow"),
        ]
        self._cache = {}
        self._tex = None

    # ---------------------------------------------------------------- operations, by piece (cached)
    def _piece(self, name, key, make):
        k = (name, key)
        if k not in self._cache:
            o = make()
            items = WW._keyed(o["still"], False) + WW._keyed(o["lit"], True)
            self._cache[k] = [it for it in items if not WW._excluded(self, it[1])]
        return self._cache[k]

    def items(self, s):
        out = []
        out += self._piece("columns", (), lambda: {"still": T.column_ops(), "lit": []})
        out += self._piece("outlet", (s["mu1"],), lambda: CT.outlet_ops(T, makeup_open=s["mu1"]))
        ps = dict(BASE_PANEL, level=s["m_level"], hot=s["m_hot"], cold=s["m_cold"], flow=s["m_flow"], mu_open=s["mu1"],
                  mu_shut=not s["mu1"], hi=s["hi"], lo=s["lo"])
        out += self._piece("panel", (s["mu1"], s["hi"], s["lo"], s["m_level"], s["m_hot"], s["m_cold"], s["m_flow"]),
                           lambda: CT.panel_ops(state=ps))
        out += self._piece("access", (s["lights"], s["door"]), lambda: CT.access_ops(T, lights=s["lights"], door_open=s["door"]))
        out += self._piece("ladder", (s["door2"],), lambda: CT.ladder_ops(T, crossover_open=s["door2"]))
        sky, flash = {"off": ("day", False), "white": ("day", True), "red": ("night", True)}[s["beacon"]]
        out += self._piece("obstruction", (s["beacon"],), lambda: CT.obstruction_ops(T, sky=sky, flash=flash))
        out += self._piece("inside", (s["running"],), lambda: CT.inside_ops(T, running=s["running"], strip=3.6))
        return out

    def ops(self, state):
        raise NotImplementedError

    # ---------------------------------------------------------------- textures
    def textures(self, state):
        if self._tex is None:
            t = {}
            t.update(CT.textures())
            t.update(CT.ops_textures())
            t.update(CT.panel_textures())
            t.update(CT.access_textures())
            t.update(CT.obstruction_textures())
            t.update(CT.inside_textures())
            t.update(led_textures())
            self._tex = t
        return self._tex

    def extra_textures(self):
        """The shell's concrete (drawn by the game from the tower's profile, TowerShell.java)."""
        return sorted(n for n in CT.textures())

    # ---------------------------------------------------------------- cells
    def anchor(self):
        return ANCHOR

    def shell(self):
        return "medium"

    def water(self, c):
        x, y, z = c[0] + 0.5, c[1] + 0.5, c[2] + 0.5
        if y > 0:
            return False
        if -2 <= c[1] <= -1 and math.hypot(x, z) < 50.4:
            return True
        d = -z
        if abs(x) < 2.0 and 50.4 < d < 59.5:
            floor = -2.0 if d < 51.65 else -3.9
            return y > floor - 0.5 and c[1] <= -1
        return False

    def ladders(self):
        out = list(LADDERS)
        out.append(((1.1, -3.6, -59.28), (1.1, 0.95, -59.28), (0.0, 0.0, 1.0)))      # down into the outlet sump
        return out

    def collision_extra(self, vox):
        f0, f1 = CT.INSIDE["fill"]
        RF = T.RL - 0.95 - 0.05
        vox.cyl((0, f0, 0), (0, f1, 0), RF)                                        # the fill packs
        e0, e1 = CT.INSIDE["elim"]
        RE = T.rad(e1) - T.thick(e1) - 0.03
        vox.cyl((0, e0, 0), (0, e1, 0), RE)                                        # the drift eliminators

    def zones(self):
        out = []
        for k, name in ((0, "door"), (1, "door2")):
            i0, n, YS, YH = T.DOORS[k]
            dphi = 2 * math.pi / T.NPHI
            F = CT.Frame(CT.Ops(), (i0 + n / 2) * dphi)
            r = T.rad(YS)
            pts = [F.w(t, y, rr) for t in (-0.8, 0.8) for y in (YS, YH) for rr in (r - 1.2, r + 1.0)]
            P = np.array(pts)
            out.append((name, (*P.min(0), *P.max(0))))
        px, pd = -6.5, 55.5                                                          # MU-1's handwheel
        out.append(("mu1", (px - 0.34, 0.9, -pd - 0.34, px + 0.34, 1.06, -pd + 0.34)))
        ppx, ppd, DF = 6.2, 57.5, -0.216 + 0.3 + 0.026                              # the panel's two buttons
        for i, name in enumerate(("lamp_test", "alarm_ack")):
            lx = -0.15 + i * 0.3
            x = ppx - lx
            out.append((name, (x - 0.05, 0.71, -(ppd + DF + 0.06), x + 0.05, 0.81, -(ppd + DF - 0.01))))
        return out

    def displays(self):
        """The panel's LED windows (range, approach, wet bulb): their faces in site coordinates and the glyph cells."""
        out = []
        ppx, ppd = 6.2, 57.5
        DF = -0.216 + 0.3 + 0.026
        for i, key in enumerate(("range", "approach", "wet_bulb")):
            cx, cy = -0.3 + i * 0.3, 1.24
            w, h, fr = 0.2, 0.09, 0.016
            x0, x1, y0, y1 = cx - w / 2 + fr, cx + w / 2 - fr, cy - h / 2 + fr, cy + h / 2 - fr
            z = DF + 0.03 + 0.0015
            # the face toward the plant (+d, -z): local lx runs to the viewer's right, world x = px - lx
            corners = [(ppx - x0, y1, -(ppd + z)), (ppx - x0, y0, -(ppd + z)), (ppx - x1, y0, -(ppd + z)), (ppx - x1, y1, -(ppd + z))]
            out.append({"key": key, "corners": [list(map(float, c)) for c in corners], "chars": LED["chars"],
                        "cells": LED["cells"], "glyphs": "ct_led_glyphs", "alphabet": LED["alphabet"], "format": "%4.1f",
                        "glyph_w": LED["glyph_w"], "glyph_h": LED["glyph_h"]})
        return out


LED = {}


def led_textures():
    """The LED windows' dark faces (the unlit 88.8 only) and the lit glyph sheet the game draws figures from."""
    fd = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 60)
    out = {}
    for key in ("range", "approach", "wet_bulb"):
        im = Image.new("RGBA", (256, 112), (8, 12, 10, 255))
        ImageDraw.Draw(im).text((236, 58), "88.8", font=fd, fill=(36, 16, 10, 255), anchor="rm")
        out["ct_digit_" + key] = im
    alphabet = "0123456789.- "
    probe = ImageDraw.Draw(Image.new("RGBA", (8, 8)))
    cw = probe.textlength("8", font=fd)
    bbox = probe.textbbox((236, 58), "8888", font=fd, anchor="rm")
    x_right = 236.0
    cells = [[(x_right - cw * (4 - k)) / 256, bbox[1] / 112, (x_right - cw * (3 - k)) / 256, bbox[3] / 112] for k in range(4)]
    gw, gh = int(math.ceil(cw)), bbox[3] - bbox[1]
    sheet = Image.new("RGBA", (16 * ((len(alphabet) * gw + 15) // 16), 16 * ((gh + 15) // 16)), (0, 0, 0, 0))
    d = ImageDraw.Draw(sheet)
    for k, ch in enumerate(alphabet):
        if ch != " ":
            d.text((k * gw + gw / 2, gh / 2), ch, font=fd, fill=(255, 92, 40, 255), anchor="mm")
    out["ct_led_glyphs"] = sheet
    LED.update({"chars": 4, "cells": cells, "alphabet": alphabet,
                "glyph_w": gw / sheet.size[0], "glyph_h": gh / sheet.size[1]})
    return out


def shell_cells():
    """The blocks the shell's surfaces fall in (cooling_tower.Tower.cells), as the game computes them."""
    return {c[0] for c in T.cells()}


# ------------------------------------------------------------------ the blueprint part

def shell_solid(x, y, z):
    """Whether points (the generators' frame, numpy arrays) are in the shell's concrete (TowerShell.solid)."""
    r = np.hypot(x, z)
    basin = (y < -2) & (r < 50.4) & (y > -3.0)
    curb = (r >= 50.4) & (r <= 53.0) & (y <= 1.0) & (y > -3.0) & ~((x > -2.5) & (x < 2.5) & (z > -53.2) & (z < -50.2))
    lintel = (y >= T.Y_LINTEL) & (y <= T.Y_SHELL) & (r >= T.RL - 0.95) & (r <= T.RL + 0.55)
    yy = np.clip(y, T.Y_SHELL, T.Y_TOP)
    rad = T.RT * np.sqrt(1 + ((yy - T.YT) / T.B) ** 2)
    th = 0.35 + 0.55 * np.maximum(0.0, 1 - (yy - T.Y_SHELL) / 30)
    phi = np.mod(np.arctan2(x, z), 2 * math.pi)
    i = np.floor(phi / (2 * math.pi / T.NPHI))
    cut = np.zeros_like(r, bool)
    for i0, n, y0, y1 in T.DOORS:
        cut |= (i >= i0) & (i < i0 + n) & (y > y0) & (y < y1)
    shell = (y >= T.Y_SHELL) & (y <= T.Y_TOP) & (r <= rad) & (r >= rad - th) & ~cut
    cornice = (y >= T.Y_TOP - 0.3) & (y <= T.Y_CTOP) & (r >= T.R_IN) & (r <= T.RTP + 0.55)
    return basin | curb | lintel | shell | cornice


def shell_cells_mc():
    """The shell's blocks in the site's frame: every block a shell cell falls in, and every block whose middle is in
    the concrete (TowerShell.has / shape)."""
    out = {WW.mirror_cell(c[0]) for c in T.cells()}
    xs = np.arange(-55, 56)
    ys = np.arange(-4, int(T.Y_CTOP) + 2)
    X, Y, Z = np.meshgrid(xs, ys, xs, indexing="ij")
    m = shell_solid(X + 0.5, Y + 0.5, -(Z + 0.5))
    for x, y, z in zip(X[m], Y[m], Z[m]):
        out.add((int(x), int(y), int(z)))
    return out


def tower_part(compiled):
    """The tower as a blueprint part (green): Plant Structure blocks (its ladder where climbed), water in the basin and
    the outlet (waterlogged where the structure stands in it), air in the make-up valve pit and the conduit's mouth, and
    the controller. Turned half round, so its outlet faces the plant (-z) when the part stands unturned in a plant: its
    controller faces north."""
    import nuclear_plants as P
    cells = set(compiled["cells"]) | shell_cells_mc()
    flags = compiled["flags"]
    site = TowerSite()
    water = set()
    for x in range(-52, 53):
        for z in range(-52, 62):
            for y in (-4, -3, -2, -1):
                if site.water(WW.mirror_cell((x, y, z))):
                    water.add((x, y, z))
    air = set()
    for x in (-7, -6):
        for z in (54, 55, 56):
            for y in (-2, -1, 0):
                air.add((x, y, z))
    for x in (-1, 0):
        for z in (60, 61):
            for y in (-4, -3, -2):
                air.add((x, y, z))
    anchor = compiled["anchor"]
    allc = cells | water | air | {anchor}
    xs = [c[0] for c in allc]
    ys = [c[1] for c in allc]
    zs = [c[2] for c in allc]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)
    p = P.Part("ct_medium", "Natural-Draft Cooling Tower", x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, kind="tower")
    p.sink = -y0

    def at(c):
        return (x1 - c[0], c[1] - y0, z1 - c[2])
    for c in cells:
        if c == anchor:
            continue
        wet = c in water or (flags.get(c, 0) & 1) or site.water(WW.mirror_cell(c))
        block = "site_ladder" if flags.get(c, 0) & 2 else "site_structure"
        p.extras[at(c)] = f"jugcraft:{block}[waterlogged={'true' if wet else 'false'}]"
    for c in water - cells - {anchor}:
        p.extras[at(c)] = "minecraft:water"
    for c in air - cells - water - {anchor}:
        p.extras[at(c)] = "minecraft:air"
    p.controller = at(anchor)
    p.extras[p.controller] = "jugcraft:cooling_tower_controller[facing=north,tower=medium]"
    # where the tower's axis is in the part (for placing it on a plant), and the site frame's corner
    p.axis = (x1 + 1.0, -y0, z1 + 1.0)
    # the plant overview screen draws the tower as the old one's footprint (tools/overview_screen.py)
    p.overview = (x1 + 1 - 55, z1 + 1 - 55, x1 + 1 + 55, z1 + 1 + 55)
    return p
