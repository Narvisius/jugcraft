"""The medium plant's reactor building inside, in game (Justin, 22 October 2026: "i want all the new parts loaded in
... the reactor room"; every machine approved one at a time on 19 October: claude/jugcraft-reactor-building-proposal.md).

Two compiled sites (tools/water_works.py) made from the approved generators, each drawn where the reactor building's
frame puts it (tools/reactor_head.py: the vessel's axis at (C, C), one block a metre, local y = plant y + 6), in the
structure tools/rb_building.py draws (the operating floor, the cavity, bay, canal and upender pit, the primary
shield, the ring girder, ladders and lights):

- reactor_building: at power. The vessel and its integrated head package, the polar crane parked across the
  building, the refuelling machine parked over the canal, the fuel transfer system at rest, the head stand and the
  internals' stand empty, the stud racks empty, the tensioners stored; the SRO's desk, the core map, the area
  radiation monitor and the control point; the four steam generators, coolant pumps and the pressurizer in their
  enclosures; the loop piping.
- reactor_building_refuel: refuelling. The head package on its stand with its studs out, the upper internals on
  theirs in the bay, the studs racked, the polar crane over the cavity, the refuelling machine over the core with an
  assembly in its gripper, the upender upright with an assembly.

The controller (nuclear/ReactorBuildingBlockEntity.java) shows the one the reactor is in (its refuelling opened or
closed from the console), and drives the live parts: the head package's CRDM fans and the coolant pumps turning while
they run, the steam generators', pressurizer's and pumps' gauges, the head's exhaust dial, and the area radiation
monitor's and the frisker's readings.

The site's frame is the generators' with z mirrored (water_works): site cell (x, y, z) is the plant's less S. The
working reactor equipment (vessel, steam generators, coolant pumps, pressurizer) stays in the plant as hidden anchors
under the floor (nuclear/EquipmentAnchorBlock VARIANT 4 on those kinds), joined by buried pipe, so the reactor forms
and runs as before.
"""
import math

import numpy as np

import control_boards as CB
import water_works as WW
import rb_parts as RP
import reactor_head as RH
import polar_crane as PC
import refuelling_machine as RM
import fuel_transfer as FT
import refuel_floor as RF
import refuel_desk as RD
import steam_generator as SG
import coolant_pump as CP
import pressurizer as PZ
import loop_piping as LP
import rb_building as RB

C = RH.C
S = (82, 0, 69)                # plant cell = site cell + S (the vessel's axis at plant (85.5, 65.5))
# local y = plant y + 6: the loop compartments' floor (local 6) level with the grass outside (the plant's 0; the
# approved drawings had it a metre over the ground, Justin: buildings' floors level with the grass)
DY = -6.0
ANCHOR = (85, -3, 57)          # the controller's cell (plant): under the loop floor, north of the vessel's pit
KEYS = {False: "reactor_building", True: "reactor_building_refuel"}

PHASE = [0, 1, 2, 3, 4, 5]     # a turning part's step (a sixth of its repeat)
SG_P = list(range(0, 1250, 50))        # psig on the steam generators' gauges
PZR_P = list(range(0, 3100, 100))      # psig on the pressurizer's
OIL_T = list(range(60, 210, 10))       # F on the pumps' upper oil pots
EXH_T = list(range(100, 210, 10))      # F, the head package's shroud exhaust


def _mat(m):
    """A generator's material as the site's texture: the equipment sheet's cells as the yards have them (ty_...), the
    reactor building's own (rb_...: its legend texture, the floors' epoxy) as they are."""
    if m is None:
        return None
    if isinstance(m, str):
        return "rb_legends" if m == RH.LEGEND else m if m.startswith("rb_") else "ty_" + m
    if isinstance(m, list) and len(m) == 5 and isinstance(m[1], (int, float)):
        return [_mat(m[0]), *m[1:]]
    if isinstance(m, list):
        return [_mat(x) for x in m]
    if isinstance(m, dict):
        return {k: _mat(v) for k, v in m.items()}
    raise ValueError(m)


def _site_op(op):
    """An operation in the reactor building's frame, in the site's generator frame: lowered by DY, materials renamed."""
    k = op[0]
    o = list(op)
    if k == "b":
        o[2] += DY
        o[5] += DY
        o[7] = _mat(o[7])
    elif k == "o":
        o[2] += DY
        o[13] = _mat(o[13])
    elif k == "q":
        for i in (2, 5, 8, 11):
            o[i] += DY
        o[13] = _mat(o[13])
    elif k == "c":
        o[2] += DY
        o[5] += DY
        o[9] = _mat(o[9])
        o[10] = _mat(o[10])
    else:
        raise ValueError(k)
    return o


def _moved(ops, d):
    return RM.moved(ops, *d)


def _turned(ops, centre, axis, angle):
    """Operations turned by angle (radians) about an axis through centre (the rotor of a fan or a pump's coupling)."""
    a = np.asarray(axis, float)
    a = a / np.linalg.norm(a)
    K = np.array([[0, -a[2], a[1]], [a[2], 0, -a[0]], [-a[1], a[0], 0]])
    R = np.eye(3) + math.sin(angle) * K + (1 - math.cos(angle)) * K @ K
    c = np.asarray(centre, float)

    def pt(p):
        return [round(float(v), 6) for v in R @ (np.asarray(p, float) - c) + c]

    def dr(v):
        return [round(float(x), 6) for x in R @ np.asarray(v, float)]
    out = []
    for op in ops:
        k = op[0]
        o = list(op)
        if k == "b":
            x0, y0, z0, x1, y1, z1 = o[1:7]
            cc = pt(((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2))
            o = ["o", *cc, *dr((1, 0, 0)), *dr((0, 1, 0)), (x1 - x0) / 2, (y1 - y0) / 2, (z1 - z0) / 2, o[7]]
        elif k == "o":
            o[1:4] = pt(o[1:4])
            o[4:7] = dr(o[4:7])
            o[7:10] = dr(o[7:10])
        elif k == "q":
            for i in (1, 4, 7, 10):
                o[i:i + 3] = pt(o[i:i + 3])
        elif k == "c":
            o[1:4] = pt(o[1:4])
            o[4:7] = pt(o[4:7])
        out.append(o)
    return out


def _canon_split(a, b):
    """The operations two pictures of a piece share, and what each has of its own (a needle at two readings)."""
    from collections import Counter
    ka = [WW.canon(o) for o in a]
    kb = Counter(WW.canon(o) for o in b)
    common, own = [], []
    for k, o in zip(ka, a):
        if kb[k] > 0:
            kb[k] -= 1
            common.append(o)
        else:
            own.append(o)
    return common, own


# ---------------------------------------------------------------- the ladders the generators draw (climbable cells)

LADDERS = []
_ladder = RP.ladder


def _recording_ladder(b, lst, x, z, y0, y1, along=(1, 0, 0), half=0.22, top_ext=1.07, standoffs=(), standoff_to=None, wall_dir=None):
    _ladder(b, lst, x, z, y0, y1, along, half, top_ext, standoffs, standoff_to, wall_dir)
    LADDERS.append((id(b), x, z, y0, y1, tuple(along), tuple(wall_dir) if wall_dir else None))


RP.ladder = _recording_ladder


# ---------------------------------------------------------------- the machines, once

class Boards:
    """Every generator built once; the pieces in each picture."""

    def __init__(self):
        del LADDERS[:]
        self.b = {"building": RB.build(), "building_ref": RB.build(refuel=True), "head": RH.build("running"), "crane": PC.build(), "rm": RM.build(), "ft": FT.build(),
                  "desk": RD.build(), "piping": LP.build(), "floor_run": self._floor_running(), "floor_ref": RF.build("refuelling"),
                  "pzr": PZ.build()}
        for loop in (1, 2, 3, 4):
            self.b[f"sg{loop}"] = SG.build(loop)
            self.b[f"rcp{loop}"] = CP.build(loop)
        ids = {id(v): k for k, v in self.b.items() if k != "building_ref"}
        self.ladders = [(ids[i], x, z, y0, y1, a, w) for i, x, z, y0, y1, a, w in LADDERS if i in ids]
        self.cache = {}

    @staticmethod
    def _floor_running():
        """The refuelling floor at power: the head stand and the internals' stand empty, the stud racks empty, the
        tensioners in their storage rack (the head and the internals in the vessel)."""
        b = RF.Floor()
        RF.head_stand(b)
        RF.internals_stand(b)
        RF.stud_racks(b, filled=False)
        RF.tensioner_storage(b)
        return b

    def rotor(self, op, centre=None):
        """Whether an operation is one of the head package's fan rotors (blades and hub)."""
        for (fx, fy, fz, r) in self.b["head"].live["fans"]:
            if centre is not None and (fx, fy, fz) != tuple(centre):
                continue
            if op[0] == "o" and abs(op[2] - fy) < 0.02 and math.hypot(op[1] - fx, op[3] - fz) < 0.35 and op[10] < 0.2:
                return True
            if op[0] == "c" and abs(op[1] - fx) < 1e-6 and abs(op[3] - fz) < 1e-6 and abs(op[2] - (fy - 0.06)) < 0.011:
                return True
        return False

    def bridge(self, refuel):
        k = ("_bridge", refuel)
        if k not in self.cache:
            h = RH.Head()
            RH.cable_bridge(h, raised=refuel)
            RH.seismic_restraints(h, released=refuel)
            self.cache[k] = h
        return self.cache[k]

    def piece(self, key):
        """One piece's (still, lit) operations in the building's frame."""
        if key in self.cache:
            return self.cache[key]
        b = self.b
        name, args = key[0], key[1:]
        if name == "building":
            refuel, = args
            o = b["building_ref" if refuel else "building"].ops()
            out = (o["still"], o["lit"])
        elif name == "vessel":
            o = b["head"].ops()
            out = (o["still"][:b["head"].core], [])
        elif name == "head":
            refuel, = args
            h = RH.Head()
            RH.closure_head(h, studs=not refuel)
            RH.head_package(h, lifted_cables=refuel)
            still = [op for op in h.still if not self.rotor(op)]
            lit = list(h.lit)
            if refuel:
                # (stopped on its stand: its rotors with it)
                still += self.fan(0, 0) + self.fan(1, 0) + self.fan(2, 0)
                still, lit = _moved(still, RF.HEAD_AT), _moved(lit, RF.HEAD_AT)
            br = self.bridge(refuel)
            out = (still + list(br.still), lit + list(br.lit))
        elif name == "fan":
            i, phase = args
            out = (self.fan(i, phase), [])
        elif name == "exh":
            t, = args
            d = self.bridge(False).live["dial"]
            out = (FT.needle_ops(d[0:3], d[3], d[4], d[5], d[6], t), [])
        elif name == "floor":
            refuel, = args
            fl = b["floor_ref"] if refuel else b["floor_run"]
            o = fl.ops()
            still, lit = list(o["still"]), list(o["lit"])
            if refuel:
                still += _moved(fl.live["internals"]["still"], RF.IN_STAND) + _moved(fl.live["rig"]["still"], RF.IN_STAND)
            for r in fl.live["readouts"]:
                lit += RM.led_ops(r["centre"], r["normal"], r["char_h"], "0".rjust(r["chars"]))
            out = (still, lit)
        elif name == "crane":
            refuel, = args
            # parked across the building at power; over the cavity, its hook low, while refuelling
            o = PC.placed(b["crane"], PC.SPAN / 2 + (0.0 if refuel else 6.0), -14.0 if refuel else -3.0, -3.0)
            d = (C - PC.SPAN / 2, 46.0, C - PC.ZC)
            lit = list(o["lit"])
            for (x0, y0, x1, y1, z, side) in b["crane"].live.get("displays", []):
                lit += RM.led_ops(((x0 + x1) / 2, (y0 + y1) / 2, z), (0, 0, side), (y1 - y0) * 0.62, "0.0t")
            out = (_moved(o["still"], d), _moved(lit, d))
        elif name == "rm":
            refuel, = args
            if refuel:
                x, z = RM.cell_xz("H", 8)
                o = RM.placed(b["rm"], x, z, RM.G_UP - 2.0, carrying=True, engaged=True,
                              readings={"bridge": "8", "trolley": "H", "mast_z": "347", "load": "1290"}, lamps=("engaged",))
            else:
                o = RM.placed(b["rm"], C + 11.0, C, RM.G_UP, carrying=False, engaged=False,
                              readings={"bridge": "--", "trolley": "-", "mast_z": "0", "load": "0"}, lamps=("released",))
            out = (o["still"], o["lit"])
        elif name == "ft":
            refuel, = args
            o = FT.placed(b["ft"], 0.0, 90.0 if refuel else 0.0, fuel=refuel, readings={"car_ft": "0.0"},
                          lamps=("power", "car_reactor", "up") if refuel else ("power",), pressure=1480 if refuel else 0)
            out = (ft_lift(o["still"]), ft_lift(o["lit"]))
        elif name == "desk":
            refuel, = args
            cells = {"H-08": "moving"} if refuel else {}
            o = RD.placed(b["desk"], cells, move="047" if refuel else "---", step="3" if refuel else "-", mr="", cpm="",
                          lamps=())
            out = (o["still"], o["lit"])
        elif name == "piping":
            o = b["piping"].ops()
            out = (o["still"], o["lit"])
        elif name == "sg":
            loop, = args
            o = b[f"sg{loop}"].ops()
            out = (list(o["still"]), list(o["lit"]))
        elif name == "sg_needle":
            loop, p = args
            d = b[f"sg{loop}"].live["dial"]
            out = (FT.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], p), [])
        elif name == "pzr":
            o = b["pzr"].ops()
            out = (list(o["still"]), list(o["lit"]))
        elif name == "pzr_needle":
            p, = args
            d = b["pzr"].live["dial"]
            out = (FT.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], p), [])
        elif name == "rcp":
            loop = args[0]
            o = b[f"rcp{loop}"].ops()
            pb = b[f"rcp{loop}"]
            d = pb.live["lift"]
            out = (list(o["still"]) + FT.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], 0), list(o["lit"]))
        elif name == "rcp_spin":
            loop, phase = args
            pb = b[f"rcp{loop}"]
            spin = pb.live["spin"]
            ax = spin["axis"]
            out = (_turned(spin["still"], (ax[0], 0.0, ax[1]), (0, 1, 0), 2 * math.pi * phase / len(PHASE)), [])
        elif name == "rcp_oil":
            loop, t = args
            d = b[f"rcp{loop}"].live["dial"]
            out = (FT.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], t), [])
        else:
            raise KeyError(key)
        self.cache[key] = out
        return out

    def fan(self, i, phase):
        fx, fy, fz, r = self.b["head"].live["fans"][i]
        full = self.b["head"].ops()["still"]
        rot = [op for op in full if self.rotor(op, (fx, fy, fz))]
        return _turned(rot, (fx, fy, fz), (0, 1, 0), 2 * math.pi / 5 * phase / len(PHASE))


def _lift_y(y):
    """The upender pit's lift (rb_building.PIT_LIFT) at a height: all of it in the pit, none from the canal's floor up
    (its lines up the pit's wall shortened over the last metre and a half)."""
    lo, hi = RH.CAVITY_FLOOR - 1.5, RH.CAVITY_FLOOR - 0.1
    t = 1.0 if y <= lo else 0.0 if y >= hi else (hi - y) / (hi - lo)
    return y + RB.PIT_LIFT * t


def ft_lift(ops):
    """The fuel transfer system with what stands in the upender pit lifted (rb_building.PIT_LIFT)."""
    out = []
    for op in ops:
        o = list(op)
        k = o[0]
        if k == "b":
            o[2], o[5] = _lift_y(o[2]), _lift_y(o[5])
        elif k == "o":
            o[2] = _lift_y(o[2])
        elif k == "q":
            for i in (2, 5, 8, 11):
                o[i] = _lift_y(o[i])
        elif k == "c":
            o[2], o[5] = _lift_y(o[2]), _lift_y(o[5])
        out.append(o)
    return out


_BOARDS = None


def boards():
    global _BOARDS
    if _BOARDS is None:
        _BOARDS = Boards()
    return _BOARDS


# ---------------------------------------------------------------- the two sites

class ReactorBuilding(WW.Site):
    def __init__(self, refuel=False):
        self.refuel = refuel
        self.key = KEYS[refuel]
        self.B = boards()
        v = []
        if not refuel:
            v += [WW.Var(f"fan{i + 1}", PHASE, "frames") for i in range(3)]
            v += [WW.Var("exh_t", EXH_T)]
        v += [WW.Var(f"rcp{k}", PHASE, "frames") for k in (1, 2, 3, 4)]
        v += [WW.Var(f"sg{k}_p", SG_P) for k in (1, 2, 3, 4)]
        v += [WW.Var("pzr_p", PZR_P)]
        v += [WW.Var(f"oil{k}_t", OIL_T) for k in (1, 2, 3, 4)]
        self.variables = v
        self.extra_live = {"points": {"core": self.point(C, 7.0, C), "head": self.point(C, RH.DOME_TOP, C)}}

    def point(self, x, y, z):
        """A point of the building's frame in site coordinates (the game's frame: z mirrored)."""
        return [round(x, 3), round(y + DY, 3), round(-z, 3)]

    def base(self):
        out = {"rcp1": 0, "rcp2": 0, "rcp3": 0, "rcp4": 0, "sg1_p": 900, "sg2_p": 900, "sg3_p": 900, "sg4_p": 900, "pzr_p": 2200,
               "oil1_t": 140, "oil2_t": 140, "oil3_t": 140, "oil4_t": 140}
        if not self.refuel:
            out.update({"fan1": 0, "fan2": 0, "fan3": 0, "exh_t": 140})
        return out

    def static_keys(self):
        r = self.refuel
        keys = [("building", r), ("vessel",), ("head", r), ("floor", r), ("crane", r), ("rm", r), ("ft", r), ("desk", r), ("piping",),
                ("pzr",)]
        keys += [("sg", k) for k in (1, 2, 3, 4)] + [("rcp", k) for k in (1, 2, 3, 4)]
        return keys

    def var_key(self, name, value):
        if name.startswith("fan"):
            return ("fan", int(name[3]) - 1, value)
        if name == "exh_t":
            return ("exh", value)
        if name.startswith("rcp"):
            return ("rcp_spin", int(name[3]), value)
        if name.startswith("sg"):
            return ("sg_needle", int(name[2]), value)
        if name == "pzr_p":
            return ("pzr_needle", value)
        if name.startswith("oil"):
            return ("rcp_oil", int(name[3]), value)
        raise KeyError(name)

    def direct_diff(self):
        """The static operations and each variable's groups straight from the pieces (each variable moves its own
        piece): what water_works.diff works out by comparing whole pictures, without building them."""
        still, lit = [], []
        for key in self.static_keys():
            s, l = self.B.piece(key)
            still += [_site_op(o) for o in s]
            lit += [_site_op(o) for o in l]
        groups = {}
        for v in self.variables:
            for i, x in enumerate(v.values):
                s, l = self.B.piece(self.var_key(v.name, x))
                groups[(v.name, i)] = [(_site_op(o), False) for o in s] + [(_site_op(o), True) for o in l]
        return still, lit, groups

    def ops(self, state):
        raise NotImplementedError

    def textures(self, state):
        return reactor_textures()

    def anchor(self):
        a = np.asarray(ANCHOR) - np.asarray(S)
        return WW.mirror_cell(tuple(int(v) for v in a))

    def collision_ops(self, still):
        # (no ropes, cables, hoses or fine rails to stand on)
        return [o for o in still if not (o[0] == "c" and o[7] < 0.03)]

    def collision_extra(self, vox):
        """The structure's solids its faces don't give (the slabs drawn as faces, the round walls)."""
        for c in self.B.b["building"].collide:
            if c[0] == "ring":
                _, r0, r1, y0, y1, gaps = c
                n = max(24, int(2 * math.pi * r1 / 0.8))
                for i in range(n):
                    f = 2 * math.pi * (i + 0.5) / n
                    spans = [(y0, y1)]
                    for g in gaps:
                        if _ang_in(f, g[0], g[1]):
                            spans = _cut(spans, g[2], g[3])
                    rm = (r0 + r1) / 2
                    ctr = (C + rm * math.cos(f), 0, C + rm * math.sin(f))
                    u = (math.cos(f), 0, math.sin(f))
                    for ya, yb in spans:
                        vox.obox((ctr[0], (ya + yb) / 2 + DY, ctr[2]), np.array([u, (0, 1, 0), (-u[2], 0, u[0])]),
                                 [(r1 - r0) / 2, (yb - ya) / 2, math.pi * rm / n + 0.03])
            elif c[0] == "disc":
                _, r0, r1, y0, y1 = c
                vox.box(C - r1 * 0.7, y0 + DY, C - r1 * 0.7, C + r1 * 0.7, y1 + DY, C + r1 * 0.7)
            else:
                x0, y0, z0, x1, y1, z1 = c
                vox.box(x0, y0 + DY, z0, x1, y1 + DY, z1)

    def ladders(self):
        out = []
        for key, x, z, y0, y1, along, wall in self.B.ladders:
            if key in ("rm", "crane"):
                continue                       # (they move)
            a = np.asarray(along, float)
            a = a / (np.linalg.norm(a) or 1)
            n = np.array([-a[2], 0.0, a[0]])
            sides = [-np.asarray(wall, float)] if wall else [n, -n]
            for o in sides:
                o = o / np.linalg.norm(o)
                out.append(((x, y0 + DY, z), (x, y1 + DY, z), tuple(o)))
        return out

    def keep_cell(self, cell):
        return keep(cell)

    def displays(self):
        """The area radiation monitor's mR/hr (red LED) and the frisker's cpm (grey LCD), filled in by the game."""
        d = self.B.b["desk"]
        out = []
        for r in d.live["readouts"]:
            if r["name"] == "mr_hr":
                out.append(_display("arm_mr", r["centre"], r["normal"], r["char_h"], r["chars"], (0, 1, 0), "rb_led", RH.LED_CHARS,
                                    "%.1f", True))
        lc = d.live["lcd"]
        out.append(_display("frisker_cpm", lc["centre"], lc["normal"], lc["char_h"], lc["chars"], tuple(lc["up"]), "rb_lcd",
                            RH.LCD_CHARS, "%.0f", False))
        return out

    def extra_textures(self):
        return ["rb_led", "rb_lcd"]

    def zones(self):
        """The vessel: its head package at power (its status), the open vessel while refuelling (fuel in and out)."""
        if self.refuel:
            b = (C - 2.9, CF_ + DY - 0.6, C - 2.9, C + 2.9, CF_ + DY + 0.4, C + 2.9)
        else:
            b = (C - 2.6, CF_ + DY, C - 2.6, C + 2.6, RH.DOME_TOP + DY + 0.5, C + 2.6)
        return [("vessel", b)]


CF_ = RH.CAVITY_FLOOR


def _ang_in(f, a, b):
    t = (f - a) % (2 * math.pi)
    return t <= (b - a) % (2 * math.pi) + 1e-9 if (b - a) < 2 * math.pi else True


def _cut(spans, ya, yb):
    out = []
    for a, b in spans:
        if yb <= a or ya >= b:
            out.append((a, b))
        else:
            if ya > a:
                out.append((a, ya))
            if yb < b:
                out.append((yb, b))
    return out


def _display(key, centre, normal, char_h, chars, up, glyphs, alphabet, fmt, lit):
    """A live readout's window as SiteLive draws it (corners tl, bl, br, tr; a cell per character) in the building's
    frame, lowered to the site's (the compiler mirrors it)."""
    n = RM._unit(normal)
    u = RM._right(n, up)
    v = RM._unit(RM._cross(u, n))
    v = (-v[0], -v[1], -v[2])
    if RH._dot(v, up) < 0:
        v = (-v[0], -v[1], -v[2])
    w = char_h * RH.LED_CHAR[0] / RH.LED_CHAR[1]
    c = tuple(centre[i] + n[i] * 0.003 for i in range(3))
    x0 = -chars * w / 2
    P = lambda a, bb: [round(c[i] + u[i] * a + v[i] * bb + (DY if i == 1 else 0.0), 4) for i in range(3)]
    corners = [P(x0, char_h / 2), P(x0, -char_h / 2), P(-x0, -char_h / 2), P(-x0, char_h / 2)]
    cells = [[k / chars, 0.0, (k + 1) / chars, 1.0] for k in range(chars)]
    # (the glyph row is stretched to whole sixteenths when it is written, so a glyph is a share of its width)
    return {"key": key, "corners": corners, "chars": chars, "cells": cells, "glyphs": glyphs, "alphabet": alphabet, "format": fmt,
            "glyph_w": 1.0 / len(alphabet), "glyph_h": 1.0, "lit": lit}


# ---------------------------------------------------------------- which cells the site draws in

_CONTAINMENT = None


def containment_cells():
    """The containment part's blocks (plant cells): its shell, liner, base slab, loop floor, airlock, hatch."""
    global _CONTAINMENT
    if _CONTAINMENT is None:
        import nuclear_plants as P
        import medium_plant as MP
        s = P.Site("rb_probe", "probe", 372, 330, water=(0, -40, 372, 0))
        b = MP.containment(s, 60, 40)
        g = b.g
        xs, ys, zs = np.nonzero(g)
        sink = getattr(b, "sink", 1)
        _CONTAINMENT = {(60 + int(x), int(y) - sink, 40 + int(z)) for x, y, z in zip(xs, ys, zs)}
    return _CONTAINMENT


def _local_xz(cell_site):
    """A site cell's middle in the building's frame (x, z)."""
    x, y, z = cell_site
    return x + 0.5, -(z + 0.5)


def in_patch(x, z):
    return any(p[0] <= x <= p[2] and p[1] <= z <= p[3] for p in RB.PATCHES)


def keep(cell):
    """Whether the site draws in a cell (site coordinates): not where the containment's blocks are (its shell and
    liner; the loop floor but round the pits, where the site draws the floor itself), not under the loop floor but in
    the pits and the transfer tube's run."""
    x, y, z = cell
    plant = (x + S[0], y, z + S[2])
    lx, lz = _local_xz(cell)
    pits = in_patch(lx, lz)
    tube = (RB.CANAL_X[1] - 0.5 <= lx <= FT.TUBE_END + 0.5 and RB.PIT_Z[0] - 0.5 <= lz <= RB.PIT_Z[1] + 0.5)
    if plant in containment_cells():
        return pits and y >= -1
    if y - DY < RB.LOOP:         # (local y of the cell's bottom under the loop floor's top)
        return pits or tube
    return True


# ---------------------------------------------------------------- textures

_TEX = {}


def glyph_sheet(name):
    """The LED (or LCD) characters in a row, cut from the legend texture at half its size."""
    if name in _TEX:
        return _TEX[name]
    from PIL import Image
    full = RH.legend_image().convert("RGBA")
    m = RH.legend("LED_DIGITS" if name == "rb_led" else "LCD_DIGITS")
    W, H = full.size
    box = (round(m[1] * W), round(m[2] * H), round(m[3] * W), round(m[4] * H))
    img = full.crop(box)
    img = img.resize((max(1, img.size[0] // 2), max(1, img.size[1] // 2)), Image.LANCZOS)
    a = np.asarray(img).copy()
    if name == "rb_led":
        a[..., 3] = np.where(a[..., 3] >= 110, 255, 0)
    else:
        a[..., 3] = 255
    _TEX[name] = Image.fromarray(a)
    return _TEX[name]


def reactor_textures():
    """The equipment sheet's cells the generators draw in (as the yards have them, ty_...), the reactor building's
    legend texture at half its drawn size (256 px a block), its letters' edges made hard so it draws as cutout; the
    readouts' glyph rows; the floors' epoxy (the plant's block)."""
    if "rb_legends" in _TEX:
        return dict(_TEX)
    import nuclear as N
    from PIL import Image
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
    img = RH.legend_image().convert("RGBA")
    w, h = img.size
    Hh = ((h + 63) // 64) * 64
    full = Image.new("RGBA", (w, Hh), (0, 0, 0, 0))
    full.paste(img, (0, 0))
    half = full.resize((w // 2, Hh // 2), Image.LANCZOS)
    a = np.asarray(half).copy()
    a[..., 3] = np.where(a[..., 3] >= 110, 255, 0)
    _TEX["rb_legends"] = Image.fromarray(a)
    glyph_sheet("rb_led")
    glyph_sheet("rb_lcd")
    _TEX["rb_epoxy"] = Image.open(WW.ASSETS / "textures" / "block" / "epoxy_floor.png").convert("RGBA")
    return dict(_TEX)


# ---------------------------------------------------------------- compiling

_diff = WW.diff


def _direct(site):
    return site.direct_diff() if hasattr(site, "direct_diff") else _diff(site)


WW.diff = _direct


def compile_sites():
    out = {}
    for refuel in (False, True):
        site = ReactorBuilding(refuel)
        out[site.key] = WW.compile_site(site)
    return out


if __name__ == "__main__":
    compile_sites()


# ---------------------------------------------------------------- the medium plant's part

def to_plant(p):
    """A point of the building's frame (x, y, z) as plant coordinates (x, y, z) (floats)."""
    return (p[0] + S[0], p[1] + DY + S[1], -p[2] + S[2])


def interior(c):
    """A plant cell inside the containment's liner, over its loop floor."""
    x, y, z = c
    return math.hypot(x + 0.5 - (C + S[0]), z + 0.5 - (S[2] - C)) < 21.6 and 1 <= y <= 40


def void_cells(cells):
    """Cells under the ground the plant keeps open (the vessel's pit, the upender pit): structure blocks with nothing
    of the site in them (they stand as air) where the site has no cell."""
    bb = RB.build()
    out = set()
    for v in bb.voids:
        if v[0] == "ring":
            _, r, y0, y1 = v
            box = (C - r, y0, C - r, C + r, y1, C + r)
            inside = lambda x, z: math.hypot(x - C, z - C) < r
        else:
            box = v
            inside = lambda x, z, b=v: b[0] <= x <= b[3] and b[2] <= z <= b[5]
        p0, p1 = to_plant(box[0:3]), to_plant(box[3:6])
        for px in range(math.floor(min(p0[0], p1[0])), math.ceil(max(p0[0], p1[0]))):
            for pz in range(math.floor(min(p0[2], p1[2])), math.ceil(max(p0[2], p1[2]))):
                lx, lz = px + 0.5 - S[0], -(pz + 0.5 - S[2])
                if not inside(lx, lz):
                    continue
                for py in range(max(-4, math.floor(min(p0[1], p1[1]))), math.ceil(max(p0[1], p1[1]))):
                    c = (px, py, pz)
                    if c not in cells:
                        out.add(c)
    return out


def reactor_part():
    """Both compiled sites as one blueprint part (Plant Structure blocks where either has a cell, a ladder's where it
    is one; empty ones keeping the pits open), the controller under the loop floor, light blocks by the fittings and
    over the floors (13 to 15)."""
    import nuclear_plants as P
    cells, flags = set(), {}
    anchor = None
    for key in KEYS.values():
        c = WW.cells_of(key)
        a = c["anchor"]
        an = (int(a[0]) + S[0], int(a[1]) + S[1], int(a[2]) + S[2])
        assert anchor in (None, an), (anchor, an)
        anchor = an
        for cell in c["cells"]:
            pc = (int(cell[0]) + S[0], int(cell[1]) + S[1], int(cell[2]) + S[2])
            cells.add(pc)
            flags[pc] = flags.get(pc, 0) | c["flags"].get(tuple(cell), 0)
    assert anchor == tuple(ANCHOR), (anchor, ANCHOR)
    voids = void_cells(cells)
    bb = RB.build()
    lights = {}
    taken = cells | voids | {anchor}
    for spots, level in ((bb.lights, 15), (bb.fill, 15)):
        pts = [to_plant(p) for p in spots]
        for c, st in WW.place_lights(taken | set(lights), pts, lambda c: interior(c) and c not in containment_cells(), level).items():
            lights[c] = st
    allc = cells | voids | {anchor} | set(lights)
    x0, x1 = min(c[0] for c in allc), max(c[0] for c in allc)
    y0, y1 = min(c[1] for c in allc), max(c[1] for c in allc)
    z0, z1 = min(c[2] for c in allc), max(c[2] for c in allc)
    p = P.Part("pwr_reactor_building", "Reactor Building (inside)", x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, kind="yard")
    p.sink = -y0
    at = lambda c: (c[0] - x0, c[1] - y0, c[2] - z0)
    for c in cells:
        block = "site_ladder" if flags.get(c, 0) & 2 else "site_structure"
        p.extras[at(c)] = f"jugcraft:{block}[waterlogged=false]"
    for c in voids:
        p.extras[at(c)] = "jugcraft:site_structure[waterlogged=false]"
    for c, st in lights.items():
        p.extras[at(c)] = st
    p.controller = at(anchor)
    p.extras[p.controller] = "jugcraft:reactor_building_controller[facing=south]"
    p.at = (int(x0), int(z0))
    p.counts = {"cells": len(cells), "voids": len(voids), "lights": len(lights)}
    return p
