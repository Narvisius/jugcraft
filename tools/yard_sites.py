"""The medium plant's main transformer yard and 345 kV switchyard in game (Justin, 20 October 2026: "Build
transformer/switchyard"; every machine approved one at a time on 19 October: claude/jugcraft-transformer-yard-proposal.md).
Two compiled sites (tools/water_works.py) made from the approved generator (tools/transformer_yard.py), each machine
placed where gsu_scene.build_ipb("yard") placed it:

- transformer_yard: the isolated phase bus on its trestles from the turbine hall's east wall, the main transformers
  MT-1A, 1B, 1C and the installed spare MT-1S in their bays, the unit auxiliary transformers UAT-1A and 1B, the 345 kV
  strain bus on its two dead-end gantries, the deluge valve house;
- switchyard: bay 1's SF6 dead-tank breaker BKR 1-1, the disconnect 89-1 with its earth switch, line 1's line-entry
  equipment on its rigid bus, the startup transformer SAT-1 in its walled bay.

The layout was drawn on the medium site as it was before turbine hall part 1 (the hall's east face at x 203.5, block
centres on whole numbers): here everything moves 20 blocks east with the hall's east wall, and half a block to the
game's block corners (MOCK).

Live states (the controllers, nuclear/YardBlockEntity.java): each transformer's top-oil and winding needles and their red
drag hands, oil level, cooling groups (lamps, contactors, the fans turning, the pumps' flow gauges), HI TEMP; each deluge
zone armed or tripped (lamps, priming gauge); the breaker's flag, lamps and contactor; the disconnect's blades swinging
and its flag; the earth switch's blades and plate; SAT-1's tap position needle and its drag hands; the operations
counters (the breaker's, the tap changer's, the arresters') drawn in figures from the game's numbers.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import control_boards as CB
import transformer_yard as TY
import water_works as WW

# The approved layout's coordinates to the plant's: the hall's east wall 20 blocks east of where the layout had it, and
# the layout's whole numbers are block centres.
MOCK = np.array([20.5, 0.0, 0.5])
ROT = {"north": np.eye(3), "south": np.diag([-1.0, 1.0, -1.0]),
       "east": np.array([[0, 0, -1.0], [0, 1.0, 0], [1.0, 0, 0]]), "west": np.array([[0, 0, 1.0], [0, 1.0, 0], [-1.0, 0, 0]])}
MX = np.diag([-1.0, 1.0, 1.0])
MZ = np.diag([1.0, 1.0, -1.0])
HALL_FACE = 224                     # the turbine hall's east wall's outer face (its wall blocks are x 223)

OIL = list(range(20, 115, 5))       # needle steps, deg C (5 C is 9 degrees of the dial's sweep)
WDG = list(range(20, 140, 5))
LEVEL = list(range(50, 72, 2))      # oil level, %
PHASE = [0, 12, 24, 36, 48, 60]     # a fan's turn in steps (five blades: a step is a fifth of the turn's repeat over six)
TAPS = list(range(12, 23))          # SAT-1's tap changer (the dial runs 1 to 33; the grid keeps it within these)
DS_DEG = [0, 15, 30, 45, 60, 75, 90]
ES_DEG = [1.6, 30, 60, 90]


def _rename(m):
    """A material of the generator's (the equipment sheet's cells, the swatches, the lamp lenses, the legend atlas) as
    the site's texture of the same name (ty_...)."""
    if m is None:
        return None
    if isinstance(m, str):
        return "ty_" + m
    if isinstance(m, list) and len(m) == 5 and isinstance(m[1], (int, float)):
        return ["ty_legends" if m[0] == TY.LEGENDS else "ty_" + m[0], *m[1:]]
    if isinstance(m, list):
        return [_rename(x) for x in m]
    if isinstance(m, dict):
        return {k: _rename(v) for k, v in m.items()}
    raise ValueError(m)


def _pt(L, t, p):
    return [round(float(v), 6) for v in L @ np.asarray(p, float) + t]


def _dir(L, d):
    return [round(float(v), 6) for v in L @ np.asarray(d, float)]


def transform(op, L, t):
    """A drawing operation in a machine's frame, in the generators' frame of the site (a proper rotation: boxes become
    oriented boxes, so every face keeps its texture's way up)."""
    k = op[0]
    if k == "b":
        x0, y0, z0, x1, y1, z1 = op[1:7]
        c = _pt(L, t, ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2))
        return ["o", *c, *_dir(L, (1, 0, 0)), *_dir(L, (0, 1, 0)), round((x1 - x0) / 2, 6), round((y1 - y0) / 2, 6),
                round((z1 - z0) / 2, 6), _rename(op[7])]
    if k == "o":
        return ["o", *_pt(L, t, op[1:4]), *_dir(L, op[4:7]), *_dir(L, op[7:10]), *op[10:13], _rename(op[13])]
    if k == "q":
        return ["q", *_pt(L, t, op[1:4]), *_pt(L, t, op[4:7]), *_pt(L, t, op[7:10]), *_pt(L, t, op[10:13]), _rename(op[13])]
    if k == "c":
        return ["c", *_pt(L, t, op[1:4]), *_pt(L, t, op[4:7]), op[7], op[8], _rename(op[9]), _rename(op[10])]
    raise ValueError(k)


# ---------------------------------------------------------------- the counters (figures drawn by the game)

COUNTERS = []          # (board id, atlas key, corners tl, bl, br, tr in the machine's frame), as drawn
_face_quad = TY.face_quad


def _recording_face_quad(b, c, normal, w, h, mat, lst=None, up=(0, 1, 0)):
    _face_quad(b, c, normal, w, h, mat, lst, up)
    if isinstance(mat, list) and len(mat) == 1 and str(mat[0]).startswith("@counter:"):
        op = (b.still if lst is None else lst)[-1]
        COUNTERS.append((id(b), mat[0][1:], [op[1:4], op[4:7], op[7:10], op[10:13]]))


TY.face_quad = _recording_face_quad


def blank_counter(n, w_m=0.1, h_m=0.03):
    """An operations counter's window with its number wheels but no figures (the game draws them: SiteLive's displays):
    white wheels, the last red, as TY.counter_img."""
    w, h = int(w_m * TY.PXB), int(h_m * TY.PXB)
    im = Image.new("RGBA", (w, h), (20, 20, 22, 255))
    d = ImageDraw.Draw(im)
    cw = w / 5
    for i in range(5):
        d.rectangle([i * cw + 1, 1, (i + 1) * cw - 2, h - 2], fill=(236, 236, 228) if i < 4 else (200, 40, 40))
    return im


TY.counter_img = blank_counter
GLYPH_W, GLYPH_H = 24, 40


def counter_glyphs(colour):
    """A counter's figures 0 to 9 (and a blank), each in a cell the shape of a wheel's window, on clear."""
    alphabet = "0123456789 "
    sheet = Image.new("RGBA", (WW.mult16(len(alphabet) * GLYPH_W), WW.mult16(GLYPH_H)), (0, 0, 0, 0))
    d = ImageDraw.Draw(sheet)
    f = TY._font(GLYPH_H * 0.86)
    for k, ch in enumerate(alphabet):
        if ch != " ":
            d.text((k * GLYPH_W + GLYPH_W / 2, GLYPH_H / 2), ch, font=f, fill=colour + (255,), anchor="mm")
    return sheet, alphabet


# ---------------------------------------------------------------- machines on the site

class Machine:
    """One approved machine placed on the site: make(state) gives its Board, state(site state) its own state (a tuple of
    the site's variables it reads, so its drawing is cached by it)."""

    def __init__(self, name, make, origin, facing, keys=(), bay=False):
        self.name, self.make, self.origin, self.facing, self.keys, self.bay = name, make, np.asarray(origin, float), facing, keys, bay

    def frame(self, S):
        L = MZ @ ROT[self.facing] @ MX
        t = MZ @ (self.origin + MOCK - np.asarray(S, float))
        return L, t

    def point(self, S, p):
        L, t = self.frame(S)
        return L @ np.asarray(p, float) + t


def _gsu_state(prefix, s):
    return dict(oil=s[prefix + "oil"], oil_max=s[prefix + "oilmax"], wdg=s[prefix + "wdg"], wdg_max=s[prefix + "wdgmax"],
                level=s[prefix + "lvl"], fans=s[prefix + "fans"], pumps=s[prefix + "pumps"], alarms=("HI TEMP",) if s[prefix + "hot"] else ())


def _uat_state(prefix, s):
    return dict(oil=s[prefix + "oil"], oil_max=s[prefix + "oilmax"], wdg=s[prefix + "wdg"], wdg_max=s[prefix + "wdgmax"],
                level=s[prefix + "lvl"], fans=s[prefix + "fans"], alarms=("HI TEMP",) if s[prefix + "hot"] else ())


def _temps(prefix):
    return [WW.Var(prefix + "oil", OIL), WW.Var(prefix + "oilmax", OIL), WW.Var(prefix + "wdg", WDG), WW.Var(prefix + "wdgmax", WDG),
            WW.Var(prefix + "lvl", LEVEL)]


class YardSite(WW.Site):
    """A site of yard machines: items() runs each machine's generator in its state (cached) and places its operations."""
    S = (0, 0, 0)            # the site's origin in the plant (the site's cell coordinates are the plant's less this)
    ANCHOR = (0, -2, 0)      # the controller's cell (plant)

    def __init__(self):
        self.variables = self.make_variables()
        _atlas_warm()
        self._cache = {}
        self.machines = self.layout()
        self._atlas_items = len(TY.ATLAS.items)

    def make_variables(self):
        raise NotImplementedError

    def layout(self):
        raise NotImplementedError

    def _board(self, m, state):
        key = tuple(state[k] for k in m.keys)
        b = m.make(state, key)
        return b

    def _piece(self, m, state):
        key = (m.name, tuple(state[k] for k in m.keys))
        if key not in self._cache:
            for ch in list(TY.FAN_PHASE):
                del TY.FAN_PHASE[ch]
            del COUNTERS[:]
            b = self._board(m, state)
            o = TY.ATLAS.resolve(b.ops()) if m.bay else TY.finished(b)
            assert len(TY.ATLAS.items) == self._atlas_items, f"{m.name}: a legend appeared after the atlas was packed"
            L, t = m.frame(self.S)
            items = WW._keyed([transform(op, L, t) for op in o["still"]], False) + WW._keyed([transform(op, L, t) for op in o["lit"]], True)
            counters = [(k, [_pt(L, t, c) for c in cs]) for bid, k, cs in COUNTERS if bid == id(b)]
            self._cache[key] = (items, counters)
        return self._cache[key]

    def items(self, state):
        out = []
        for m in self.machines:
            out += self._piece(m, state)[0]
        return out

    def ops(self, state):
        raise NotImplementedError

    def counters(self):
        out = []
        for m in self.machines:
            out += self._piece(m, self.base())[1]
        return out

    def textures(self, state):
        return yard_textures()

    def anchor(self):
        a = np.asarray(self.ANCHOR) - np.asarray(self.S)
        return WW.mirror_cell(tuple(int(v) for v in a))

    def keep_cell(self, cell):
        return cell[1] >= 0

    def keep_poly(self, poly):
        pts, _, n, _, _ = poly
        return not (n[1] > 0.99 and np.all(np.abs(np.asarray(pts)[:, 1]) < 0.003))

    def collision_ops(self, still):
        """The solid parts: everything but the wires (the strung bus, its jumpers and taps, the shield wires, the ground
        leads), which nobody stands on."""
        return [o for o in still if not (o[0] == "c" and o[7] < 0.03)]

    def zone_box(self, m, lo, hi):
        """A click zone: a box in a machine's frame, in the generators' frame of the site."""
        pts = [m.point(self.S, (x, y, z)) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]
        a, b = np.min(pts, 0), np.max(pts, 0)
        return tuple(float(v) for v in (*a, *b))

    def machine(self, name):
        return next(m for m in self.machines if m.name == name)

    def site_point(self, name, p):
        """A point of a machine (its frame) in the site's frame (as the game reads its live data)."""
        g = self.machine(name).point(self.S, p)
        return [round(float(g[0]), 3), round(float(g[1]), 3), round(float(-g[2]), 3)]

    def counter_displays(self, names):
        """The operations counters as LED-style displays the game fills in: the first four wheels (dark figures) and the
        last (light on its red wheel), each its own display (key_hi, key_lo)."""
        out = []
        w5 = 1.0 / 5
        found = self.counters()
        for (key, corners), name in zip(found, names):
            for part, cells, glyphs in (("hi", [[k * w5 + 0.02, 0.07, (k + 1) * w5 - 0.04, 0.93] for k in range(4)], "ty_ctr_dark"),
                                        ("lo", [[4 * w5 + 0.02, 0.07, 5 * w5 - 0.04, 0.93]], "ty_ctr_light")):
                # a hair in front of the window (the generators' frame: the normal is the corners' turned about)
                tl, bl, br, tr = (np.asarray(c, float) for c in corners)
                n = np.cross(tr - tl, bl - tl)
                n = n / np.linalg.norm(n)
                cs = [list(map(float, p + n * 0.0015)) for p in (tl, bl, br, tr)]
                out.append({"key": f"{name}_{part}", "corners": cs, "chars": len(cells), "cells": cells, "glyphs": glyphs,
                            "alphabet": "0123456789 ", "format": "%04.0f" if part == "hi" else "%1.0f",
                            "glyph_w": GLYPH_W / WW.mult16(11 * GLYPH_W), "glyph_h": GLYPH_H / WW.mult16(GLYPH_H), "lit": False})
        return out

    def extra_textures(self):
        return ["ty_ctr_dark", "ty_ctr_light"] if self.displays() else []


# ---------------------------------------------------------------- the transformer yard

def _gsu_maker(name, ph, prefix, spare):
    def make(s, key):
        if spare:
            # The installed spare stands cold and dead: no live parts.
            return TY.gsu(name, ph, spare=True, state=dict(oil=20, oil_max=24, wdg=20, wdg_max=24, level=50, fans=0, pumps=False, running=False))
        TY.FAN_PHASE["fans1"] = s[prefix + "f1"]
        TY.FAN_PHASE["fans2"] = s[prefix + "f2"]
        return TY.gsu(name, ph, state=_gsu_state(prefix, s))
    return make


def _uat_maker(name, prefix):
    def make(s, key):
        TY.FAN_PHASE["fans1"] = s[prefix + "f1"]
        return TY.uat(name, state=_uat_state(prefix, s))
    return make


def _bay_maker(*args):
    def make(s, key):
        b = CB.Board(13, 1, face=0.0)
        TY.bay(b, *args)
        return b
    return make


DV_NAMES = [dv for dv, _ in TY.DVH["zones"]]


class TransformerYard(YardSite):
    key = "transformer_yard"
    S = (HALL_FACE, 0, 30)
    ANCHOR = (226, -2, 62)
    GSU_PREFIX = {"MT-1A": "mt1a_", "MT-1B": "mt1b_", "MT-1C": "mt1c_"}
    UAT_PREFIX = {"UAT-1A": "uat1a_", "UAT-1B": "uat1b_"}

    def make_variables(self):
        v = []
        for p in self.GSU_PREFIX.values():
            v += _temps(p)
            v += [WW.Var(p + "pumps", [False, True]), WW.Var(p + "fans", [0, 1, 2]), WW.Var(p + "f1", PHASE, "frames"),
                  WW.Var(p + "f2", PHASE, "frames"), WW.Var(p + "hot", [False, True])]
        for p in self.UAT_PREFIX.values():
            v += _temps(p)
            v += [WW.Var(p + "fans", [False, True]), WW.Var(p + "f1", PHASE, "frames"), WW.Var(p + "hot", [False, True])]
        for k in range(len(DV_NAMES)):
            v.append(WW.Var(f"dv{k + 1}", [False, True]))
        return v

    def layout(self):
        Y = TY.YARD
        out = [Machine("ipb", lambda s, k: TY.ipb(), (Y["hall_x"], 0, Y["bus_z"]), "north")]
        for name, ph, gx in Y["gsu"]:
            spare = ph == "S"
            prefix = self.GSU_PREFIX.get(name, "")
            keys = () if spare else tuple(prefix + k for k in ("oil", "oilmax", "wdg", "wdgmax", "lvl", "fans", "pumps", "hot", "f1", "f2"))
            out.append(Machine(name, _gsu_maker(name, ph, prefix, spare), (gx, Y["plinth"], Y["gsu_z"]), "north", keys))
            out.append(Machine(name + " bay", _bay_maker((-2.2, 11.2) if ph == "A" else (-2.2,)), (gx, 0, Y["gsu_z"]), "north", bay=True))
        for name, ux in Y["uat"]:
            prefix = self.UAT_PREFIX[name]
            keys = tuple(prefix + k for k in ("oil", "oilmax", "wdg", "wdgmax", "lvl", "fans", "hot", "f1"))
            out.append(Machine(name, _uat_maker(name, prefix), (ux, Y["plinth"], Y["uat_z"]), "north", keys))
            out.append(Machine(name + " bay", _bay_maker((7.6,) if name == "UAT-1A" else (), TY.UAT_BAY), (ux, 0, Y["uat_z"]), "north", bay=True))
        G = TY.GANTRY
        out.append(Machine("strain bus", lambda s, k: TY.strain_bus(), (G["west"], 0, G["zs"][0]), "north"))
        keys = tuple(f"dv{k + 1}" for k in range(len(DV_NAMES)))
        out.append(Machine("deluge house", lambda s, k: TY.deluge_house(
            state=dict(tripped=tuple(dv for i, dv in enumerate(DV_NAMES) if s[f"dv{i + 1}"]))), (226.0, 0, 36.6), "south", keys))
        return out

    def keep_cell(self, cell):
        # (the bus's end inside the turbine hall's wall: the wall's own blocks stay)
        return cell[1] >= 0 and cell[0] + self.S[0] >= HALL_FACE

    def zones(self):
        """The deluge valve house's manual pull releases (one a valve set), the releasing panel's RESET and SILENCE."""
        m = self.machine("deluge house")
        D = TY.DVH
        zp = D["d"] - D["wall"] - 0.75
        tz = zp - 0.3
        out = []
        for k in range(len(DV_NAMES)):
            x = 1.25 + k * 1.3
            out.append((f"dv_pull:{k}", self.zone_box(m, (x + 0.2, 1.25, tz - 0.06), (x + 0.38, 1.55, tz + 0.06))))
        X = lambda o: 6.6 - o
        y, f = 1.0 + 0.24, D["wall"] + 0.224
        for o, name in ((0.72, "dv_silence"), (0.95, "dv_reset")):
            out.append((name, self.zone_box(m, (X(o) - 0.04, y - 0.04, f - 0.004), (X(o) + 0.04, y + 0.04, f + 0.035))))
        return out

    def light_spots(self):
        """The deluge valve house's two ceiling fittings and the light over its door (plant coordinates)."""
        m = self.machine("deluge house")
        out = []
        # (the door light's block a block out from the wall, over the apron: the wall's own cells hold geometry)
        for p in ((3.0, TY.DVH["h"] - 0.13, 2.8), (7.3, TY.DVH["h"] - 0.13, 2.8), (1.55, 2.2 + 0.22, -1.0)):
            g = m.point(self.S, p)
            out.append((float(g[0] + self.S[0]), float(g[1]), float(-g[2] + self.S[2])))
        return out

    def sprays(self):
        """Each deluge zone's spray nozzles (plant coordinates: the nozzle and the way it sprays), for the game's water
        while a zone is tripped: the rings' nozzles as TY.bay draws them."""
        Y = TY.YARD
        out = {}
        targets = [("MT-1A", "A"), ("MT-1B", "B"), ("MT-1C", "C"), ("MT-1S", "S"), ("UAT-1A", None), ("UAT-1B", None)]
        for k, (name, ph) in enumerate(targets):
            m = self.machine(name + " bay")
            sp = TY.UAT_BAY if ph is None else TY.GSU_BAY
            pts = []
            ax_, az_ = sp["aim"]
            for ri, (y, (x0, x1, z0, z1)) in enumerate(sp["rings"]):
                ring = [(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1), (x0, y, z0)]
                stands = ((x0, z0), (x1, z0), (x0, z1), (x1, z1), ((x0 + x1) / 2, z0), ((x0 + x1) / 2, z1)) if sp["stands"] is None \
                    else sp["stands"][ri]
                for pa, pb in zip(ring, ring[1:]):
                    pa, pb = np.array(pa, float), np.array(pb, float)
                    nn = int(np.linalg.norm(pb - pa) / 1.4)
                    for kk in range(1, nn):
                        q = pa + (pb - pa) * kk / nn
                        if sp["stands"] is None and abs(q[0] - (x0 + x1) / 2) < 0.2:
                            continue
                        if sp["stands"] is not None and any(abs(q[0] - xa) < 0.2 and abs(q[2] - za) < 0.2 for xa, za in stands):
                            continue
                        inn = np.array([ax_ - q[0], 0, az_ - q[2]])
                        inn = inn / np.linalg.norm(inn) * 0.14
                        tip = q + inn * 1.3 - [0, 0.07, 0]
                        g = m.point(self.S, tip)
                        d = ROT[m.facing] @ MX @ (inn / 0.14 + np.array([0, -0.35, 0]))
                        pts.append([round(float(g[0] + self.S[0]), 3), round(float(g[1]), 3), round(float(-g[2] + self.S[2]), 3),
                                    round(float(d[0]), 3), round(float(d[1]), 3), round(float(d[2]), 3)])
            out[DV_NAMES[k]] = pts
        # DV-7 protects SAT-1 in the switchyard: its nozzles are the switchyard's (Switchyard.sprays)
        return out

    def gongs(self):
        """Each valve set's water-motor gong on the house's outside wall (plant coordinates)."""
        m = self.machine("deluge house")
        out = []
        for k in range(len(DV_NAMES)):
            g = m.point(self.S, (1.25 + k * 1.3, 2.4, TY.DVH["d"] + 0.2))
            out.append([round(float(g[0] + self.S[0]), 3), round(float(g[1]), 3), round(float(-g[2] + self.S[2]), 3)])
        return out


# ---------------------------------------------------------------- the switchyard

class Switchyard(YardSite):
    key = "switchyard"
    S = (287, 0, 34)
    ANCHOR = (300, -2, 66)

    def make_variables(self):
        p = "sat_"
        v = _temps(p)
        v += [WW.Var(p + "fans", [0, 1, 2]), WW.Var(p + "f1", PHASE, "frames"), WW.Var(p + "f2", PHASE, "frames"),
              WW.Var(p + "hot", [False, True]), WW.Var("tap", TAPS), WW.Var("tapmin", TAPS), WW.Var("tapmax", TAPS)]
        v += [WW.Var("bkr", [True, False]), WW.Var("ds", DS_DEG, "frames"), WW.Var("dsflag", [True, False]),
              WW.Var("es", ES_DEG, "frames"), WW.Var("esflag", [False, True])]
        return v

    def layout(self):
        def sat(s, key):
            TY.FAN_PHASE["fans1"] = s["sat_f1"]
            TY.FAN_PHASE["fans2"] = s["sat_f2"]
            st = _uat_state("sat_", s)
            st.update(tap=s["tap"], tap_min=s["tapmin"], tap_max=s["tapmax"])
            return TY.sat("SAT-1", state=st)

        def ds(s, key):
            return TY.disconnect("89-1", state=dict(closed=s["dsflag"], earthed=s["esflag"], ds_deg=s["ds"], es_deg=s["es"]))
        return [Machine("breaker", lambda s, k: TY.sf6_breaker("BKR 1-1", state=dict(closed=s["bkr"])), (266.0, 0, 41.8), "west", ("bkr",)),
                Machine("disconnect", ds, (274.0, 0, 41.8), "west", ("ds", "dsflag", "es", "esflag")),
                Machine("line entry", lambda s, k: TY.line_entry("LINE 1", traps="AC"), (280.0, 0, 41.8), "west"),
                # line 1's outgoing dead-end gantry (power lines round 2): the line strung from it is anchored at
                # LINE_GANTRY under its beam's middle (powerline/LineKind.LINE_GANTRY, its attach points)
                Machine("line gantry", lambda s, k: TY.line_gantry(), (294.0, 0, 40.3), "north"),
                Machine("SAT-1", sat, (282.0, 0.12, 70.0), "north", ("sat_oil", "sat_oilmax", "sat_wdg", "sat_wdgmax", "sat_lvl", "sat_fans",
                                                                     "sat_hot", "sat_f1", "sat_f2", "tap", "tapmin", "tapmax")),
                Machine("SAT-1 bay", _bay_maker((-2.5, 12.0), TY.SAT_BAY), (282.0, 0, 70.0), "north", bay=True)]

    def displays(self):
        # The counters in the order the machines draw them: the breaker's, the arresters' (A, B, C), the tap changer's.
        return self.counter_displays(["bkr_ops", "arr_a", "arr_b", "arr_c", "ltc_ops"])

    def zones(self):
        """The disconnect's motor operator (OPEN, CLOSE), the earth switch's hand operator, the breaker's control switch
        (which only tells that the breakers follow the plant)."""
        D = TY.DS
        m = self.machine("disconnect")
        x = D["poles"][1] - 0.3
        xr = x + 0.36
        f = (D["cols"][0] - 0.3) + 0.12 - 0.06
        out = []
        for dx, name in ((-0.09, "ds_open"), (0.09, "ds_close")):
            out.append((name, self.zone_box(m, (xr + dx - 0.035, 1.29 - 0.035, f - 0.035), (xr + dx + 0.035, 1.29 + 0.035, f + 0.004))))
        xa, zt = D["poles"][0] + 0.5, D["z_jaw"] + 0.35
        out.append(("es_toggle", self.zone_box(m, (xa - 0.13, 1.08, zt - 0.32), (xa + 0.25, 1.37, zt + 0.13))))
        b = self.machine("breaker")
        ix, f = TY.BKR["cab"][1] - 0.42, 3.3 - 0.06
        out.append(("bkr_info", self.zone_box(b, (ix - 0.045, 1.38, f - 0.06), (ix + 0.045, 1.48, f + 0.002))))
        return out

    def sprays(self):
        m = self.machine("SAT-1 bay")
        sp = TY.SAT_BAY
        pts = []
        ax_, az_ = sp["aim"]
        for ri, (y, (x0, x1, z0, z1)) in enumerate(sp["rings"]):
            ring = [(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1), (x0, y, z0)]
            stands = sp["stands"][ri]
            for pa, pb in zip(ring, ring[1:]):
                pa, pb = np.array(pa, float), np.array(pb, float)
                nn = int(np.linalg.norm(pb - pa) / 1.4)
                for kk in range(1, nn):
                    q = pa + (pb - pa) * kk / nn
                    if any(abs(q[0] - xa) < 0.2 and abs(q[2] - za) < 0.2 for xa, za in stands):
                        continue
                    inn = np.array([ax_ - q[0], 0, az_ - q[2]])
                    inn = inn / np.linalg.norm(inn) * 0.14
                    g = m.point(self.S, q + inn * 1.3 - [0, 0.07, 0])
                    d = ROT[m.facing] @ MX @ (inn / 0.14 + np.array([0, -0.35, 0]))
                    pts.append([round(float(g[0] + self.S[0]), 3), round(float(g[1]), 3), round(float(-g[2] + self.S[2]), 3),
                                round(float(d[0]), 3), round(float(d[1]), 3), round(float(d[2]), 3)])
        return {"DV-7": pts}

    def terminals(self):
        """Line 1's connection (plant cells): the grade-level cells of the line traps' and phase B's bus end pedestals at
        the line entry's far end, where a cable or a line's box takes the plant's output."""
        m = self.machine("line entry")
        L = TY.LE
        out = []
        for xp in L["poles"]:
            for z in (L["trap"], L["supports"][1]):
                g = m.point(self.S, (xp, 0.05, z))
                c = (int(math.floor(g[0] + self.S[0])), 0, int(math.floor(-g[2] + self.S[2])))
                if c not in out:
                    out.append(c)
        return out


_WARM = []


def _atlas_warm():
    """Draws every machine of both sites once, so every legend is on the atlas, and packs it, once and for good: both
    sites draw from the one legend texture (ty_legends), whichever is compiled."""
    if _WARM:
        return
    _WARM.append(True)
    TY._faces()
    for cls in (TransformerYard, Switchyard):
        site = cls.__new__(cls)
        site.variables = cls.make_variables(site)
        for m in cls.layout(site):
            m.make(site.base(), None)
    TY.ATLAS.pack()


# ---------------------------------------------------------------- textures

_TEX = {}


def yard_textures():
    """The sites' textures: the equipment sheet's cells the generator draws in, the control boards' swatches and lamp
    lenses (cells of the same sheet), the legend atlas (its letters' edges made hard, so it draws as cutout), the
    counters' figure sheets."""
    if _TEX:
        return _TEX
    import nuclear as N
    sheet = Image.open(WW.ASSETS / "textures" / "entity" / "nuclear_equipment.png").convert("RGBA")
    for name in N.EQ_MATERIALS:
        c, r = N.eq_slot(name)
        cell = sheet.crop((c * N.EQ_CELL, r * N.EQ_CELL, (c + 1) * N.EQ_CELL, (r + 1) * N.EQ_CELL))
        a = np.asarray(cell).copy()
        if a[..., 3].min() >= 250:
            a[..., 3] = 255
        else:
            a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
        _TEX["ty_" + name] = Image.fromarray(a)
    a = np.asarray(TY.ATLAS.image.convert("RGBA")).copy()
    a[..., 3] = np.where(a[..., 3] >= 110, 255, 0)
    _TEX["ty_legends"] = Image.fromarray(a)
    _TEX["ty_ctr_dark"] = counter_glyphs((22, 22, 24))[0]
    _TEX["ty_ctr_light"] = counter_glyphs((244, 244, 240))[0]
    return _TEX


# ---------------------------------------------------------------- the blueprint parts

LINE_GANTRY = (314, 0, 47)       # the line 1 gantry's anchor (plant cell, under the beam's middle)


def gantry_points():
    """Line 1's dead-end clamp ends on the gantry's outer face (plant coordinates), from the anchor's bottom centre
    (LineKind.LINE_GANTRY's attach points)."""
    m = Machine("line gantry", None, (294.0, 0, 40.3), "north")
    S = Switchyard.S
    G = TY.GANTRY
    xb = 294.0 + 0.5 + 0.1 + (G["string"] - 0.3)
    out = []
    for ph, zc in G["phases"].items():
        for dz in (-0.225, 0.225):
            g = m.point(S, (294.0 - (xb + 0.42), G["beam_y"] - 0.06, zc + dz - 40.3))
            out.append([round(float(g[0] + S[0]) - (LINE_GANTRY[0] + 0.5), 3), round(float(g[1]), 3),
                        round(float(-g[2] + S[2]) - (LINE_GANTRY[2] + 0.5), 3)])
    return out


def yard_part(site, compiled, key, name, gravel, concrete=(), fence=None, lights=(), blocks=None):
    """A compiled yard as a blueprint part (green): Plant Structure blocks, the ground under it (gravel; concrete under
    the deluge valve house), air over the yard (clearing what stood there), light blocks under the house's lights, the
    controller under the grass. Not turned: its controller faces south (the site as compiled). gravel / concrete: plant
    cell boxes (x0, z0, x1, z1 inclusive) of ground; fence: a box whose edges get a fence (iron bars, two high) with a
    gate gap (x0, z0, x1, z1, gate_x) on its south side."""
    import nuclear_plants as P
    S = tuple(int(v) for v in site.S)
    cells = {(int(c[0]) + S[0], int(c[1]), int(c[2]) + S[2]) for c in compiled["cells"]}
    flags = {(int(c[0]) + S[0], int(c[1]), int(c[2]) + S[2]): f for c, f in compiled["flags"].items()}
    a = compiled["anchor"]
    anchor = (int(a[0]) + S[0], int(a[1]), int(a[2]) + S[2])
    assert anchor == tuple(site.ANCHOR), (anchor, site.ANCHOR)
    ground = {}
    for (x0, z0, x1, z1) in gravel:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                ground[(x, -1, z)] = "minecraft:gravel"
    for (x0, z0, x1, z1) in concrete:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                ground[(x, -1, z)] = "minecraft:light_gray_concrete"
    extra = {}
    if fence:
        fx0, fz0, fx1, fz1, gate = fence
        for x in range(fx0, fx1 + 1):
            for z in (fz0, fz1):
                if z == fz1 and abs(x - gate) <= 2:
                    continue
                for y in (0, 1):
                    extra[(x, y, z)] = "minecraft:iron_bars"
        for z in range(fz0, fz1 + 1):
            for x in (fx0, fx1):
                for y in (0, 1):
                    extra[(x, y, z)] = "minecraft:iron_bars"
    top = max(c[1] for c in cells) + 1
    allc = cells | set(ground) | set(extra) | {anchor}
    x0, x1 = min(c[0] for c in allc), max(c[0] for c in allc)
    z0, z1 = min(c[2] for c in allc), max(c[2] for c in allc)
    y0 = min(c[1] for c in allc)
    p = P.Part(key, name, x1 - x0 + 1, top - y0 + 1, z1 - z0 + 1, kind="yard")
    p.sink = -y0
    at = lambda c: (c[0] - x0, c[1] - y0, c[2] - z0)
    # air over the yard (its ground's footprint), up to the top of what stands on it
    for (x, _, z) in ground:
        for y in range(0, top + 1):
            if (x, y, z) not in cells:
                p.extras[at((x, y, z))] = "minecraft:air"
    for c, st in ground.items():
        if c not in cells:
            p.extras[at(c)] = st
    for c, st in extra.items():
        if c not in cells:
            p.extras[at(c)] = st
    for c, st in (blocks or {}).items():
        assert c not in cells, (key, c)
        p.extras[at(c)] = st
    for c in cells:
        block = "site_ladder" if flags.get(c, 0) & 2 else "site_structure"
        p.extras[at(c)] = f"jugcraft:{block}[waterlogged=false]"
    for c, st in WW.place_lights(cells, lights, lambda c: c not in cells and c[1] >= 0).items():
        p.extras[at(c)] = st
    p.controller = at(anchor)
    p.extras[p.controller] = f"jugcraft:yard_controller[facing=south,works={'yard' if key.endswith('transformer_yard') else 'switchyard'}]"
    p.at = (int(x0), int(z0))       # where it stands in the medium plant
    return p


def transformer_yard_part():
    site = TransformerYard.__new__(TransformerYard)
    site.S, site.ANCHOR = TransformerYard.S, TransformerYard.ANCHOR
    c = WW.cells_of("transformer_yard")
    lights = _cached_meta("transformer_yard")["lights"]
    # the yard's gravel (the mockup's), concrete under the deluge valve house
    return yard_part(site, c, "pwr_transformer_yard", "Main Transformer Yard", gravel=[(224, 38, 282, 66), (245, 30, 258, 37)],
                     concrete=[(246, 31, 257, 37)], lights=[tuple(v) for v in lights])


def switchyard_part():
    site = Switchyard.__new__(Switchyard)
    site.S, site.ANCHOR = Switchyard.S, Switchyard.ANCHOR
    c = WW.cells_of("switchyard")
    # (its fence's west side is the protected area's outer fence, x 286)
    return yard_part(site, c, "switchyard_medium", "Switchyard", gravel=[(287, 35, 319, 83)], fence=(286, 34, 320, 84, 312),
                     blocks={LINE_GANTRY: "jugcraft:line_gantry[facing=south]"})


def _cached_meta(key):
    import gzip
    import json
    with gzip.open(WW.CELLS_OUT / f"{key}.meta.json.gz", "rt", encoding="utf-8") as f:
        return json.load(f)


def compile_all(keys=None):
    """Compiles both sites (water_works.compile_site) and writes what the plant and the game take from them besides
    (lights, sprays, gongs, terminals, the bus port) to the site's live data and tools/site_cells/<key>.meta.json.gz."""
    import gzip
    import json
    sites = {"transformer_yard": TransformerYard, "switchyard": Switchyard}
    for key in keys or list(sites):
        site = sites[key]()
        meta = {}
        if key == "transformer_yard":
            meta["lights"] = site.light_spots()
            meta["sprays"] = site.sprays()
            meta["gongs"] = site.gongs()
            meta["bus_port"] = [HALL_FACE, 9, 59]
            meta["house_feed"] = [240, -1, 59]          # (the buried feed to the plant's load centres, medium_plant)
            # with no switchyard near, the main transformers' power goes into a cable at the east gantry's feet
            meta["terminals"] = [[281, 0, 41], [281, 0, 54]]
            live_hum = [site.site_point(n, (4.5, 3.0, 3.5)) for n in ("MT-1A", "MT-1B", "MT-1C")]
            live_hum += [site.site_point(n, (3.0, 2.0, 2.9)) for n in ("UAT-1A", "UAT-1B")]
            live_points = {}
        else:
            meta["sprays"] = site.sprays()
            meta["terminals"] = [list(c) for c in site.terminals()]
            meta["line_gantry"] = list(LINE_GANTRY)
            D = TY.DS
            live_hum = [site.site_point("SAT-1", (4.6, 3.0, 3.6))]
            live_points = {"breaker": site.site_point("breaker", (5.5, 2.9, 3.0)),
                           "disconnect": site.site_point("disconnect", (5.5, D["base_y"] + D["post_h"] + 0.25, (D["z_hinge"] + D["z_jaw"]) / 2)),
                           "sat": site.site_point("SAT-1", (4.6, 3.0, 3.6))}
        # (in the live data: plant coordinates made the site's own, as the game reads them)
        S = np.asarray(site.S, float)
        live = {}
        if "sprays" in meta:
            live["sprays"] = {k: [[p[0] - S[0], p[1], p[2] - S[2], p[3], p[4], p[5]] for p in v] for k, v in meta["sprays"].items()}
        if "gongs" in meta:
            live["gongs"] = [[p[0] - S[0], p[1], p[2] - S[2]] for p in meta["gongs"]]
        if "terminals" in meta:
            live["terminals"] = [[c[0] - S[0], c[1], c[2] - S[2]] for c in meta["terminals"]]
        if "line_gantry" in meta:
            live["line_gantry"] = [meta["line_gantry"][0] - S[0], meta["line_gantry"][1], meta["line_gantry"][2] - S[2]]
        if "bus_port" in meta:
            live["bus_port"] = [meta["bus_port"][0] - S[0], meta["bus_port"][1], meta["bus_port"][2] - S[2]]
        live["hum"] = live_hum
        live["points"] = live_points
        if "house_feed" in meta:
            live["house_feed"] = [meta["house_feed"][0] - S[0], meta["house_feed"][1], meta["house_feed"][2] - S[2]]
        site.extra_live = live
        WW.compile_site(site)
        WW.CELLS_OUT.mkdir(parents=True, exist_ok=True)
        with gzip.open(WW.CELLS_OUT / f"{key}.meta.json.gz", "wt", encoding="utf-8") as f:
            json.dump(meta, f, separators=(",", ":"))


if __name__ == "__main__":
    import sys
    compile_all(sys.argv[1:] or None)
