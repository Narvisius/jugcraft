"""The medium plant's circulating water pump house as a compiled site (tools/water_works.py): pieces 6 and 7 of
claude/jugcraft-pump-houses.md: the forebay and bays fed by the towers' two conduits, the four vertical circulating
water pumps with their hydraulically operated butterfly valves, the HPU, the lines down to the buried mains; the hall
with its 50 t crane, roll-up door, and the 13.8 kV annex with the two trains' switchgear, MCCs and LCP-CW.

Live states: each pump running (its motor's fan and coupling guard, the discharge gauge, the switchgear's flags and
lamps, the panel's lamps and ammeter; the figures on the panel and its screen), each valve open or shut (its disc and
indicator, the panel's lamps), the hall's lights, the roll-up door's travel, the three doors, the annunciator's 24
windows. The forebay's water is the world's own water blocks.
"""
import numpy as np

import cooling_tower as CT
import pump_house as PH
import water_works as WW

ROLL = [0.0, 0.2, 0.4, 0.6, 0.8, 1.0]
# The controller: under the grass past the house's south end wall, in the middle, where the buried mains leave for the
# condenser (the plant's conduit trench passes it).
ANCHOR = (12, -1, -11)


class CircSite(WW.Site):
    key = "cw_pump_house"
    exclude = {"ct_water"}

    def __init__(self):
        v = []
        for p in range(4):
            v.append(WW.Var(f"cwp{p}", [True, False]))
        for p in range(4):
            v.append(WW.Var(f"cwv{p}", [True, False]))
        v += [WW.Var("lights", [False, True]), WW.Var("roll", ROLL, "frames")]
        for n in ("cw_door", "er1", "er2"):
            v.append(WW.Var("door_" + n, [False, True]))
        for k in range(len(PH.ANN_CW)):
            v.append(WW.Var(f"ann{k}", [False, True]))
        self.variables = v
        self._cache = {}
        self._tex = {}

    def _piece(self, name, key, make):
        k = (name, key)
        if k not in self._cache:
            PH.ZONES.clear()
            o = make()
            items = WW._keyed(o["still"], False) + WW._keyed(o["lit"], True)
            self._cache[k] = [it for it in items if not WW._excluded(self, it[1])]
        return self._cache[k]

    def items(self, s):
        run = tuple(s[f"cwp{p}"] for p in range(4))
        vo = tuple(s[f"cwv{p}"] for p in range(4))
        out = []
        out += self._piece("circ", (run, vo), lambda: PH.circ_ops(level=-1.0, running=run, valve_open=vo))
        lit = tuple(PH.ANN_CW[k] for k in range(len(PH.ANN_CW)) if s[f"ann{k}"])
        doors = tuple(n for n in ("cw_door", "er1", "er2") if s["door_" + n])

        def building():
            PH.OPEN.clear()
            PH.OPEN.update(doors)
            return PH.circ_building_ops(lights=s["lights"], roll=s["roll"], running=run, valve_open=vo, ann_lit=lit)
        out += self._piece("building", (s["lights"], s["roll"], run, vo, lit, doors), building)
        PH.OPEN.clear()
        return out

    def ops(self, state):
        raise NotImplementedError

    def texture_vars(self):
        return {f"cwp{p}" for p in range(4)}

    def textures(self, state):
        run = tuple(state[f"cwp{p}"] for p in range(4))
        if run not in self._tex:
            t = {}
            t.update(CT.ops_textures())
            t.update(PH.intake_textures())
            t.update(PH.screens_textures())
            t.update(PH.pumps_textures())
            t.update(PH.elec_textures())
            t.update(PH.building_textures())
            t.update(PH.circ_textures(running=run))
            t.update(PH.circ_building_textures(running=run))
            self._tex[run] = t
        return self._tex[run]

    def anchor(self):
        return ANCHOR

    def water(self, c):
        """The forebay's and the bays' water (level -1.0): every cell of the structure's outline below it."""
        x0, x1 = c[0], c[0] + 1
        d0, d1 = -(c[2] + 1), -c[2]
        if c[1] > -2 or c[1] < -9:
            return False
        return x1 > -0.5 and x0 < PH.CW_W + 0.5 and d1 > PH.CW_D["north"] - 0.5 and d0 < PH.CW_D["back1"]

    def basin_water(self, c):
        """The cells the part fills with water: the forebay and the bays inside the walls."""
        x, d = c[0] + 0.5, -(c[2] + 0.5)
        if not (PH.CW_FLOOR <= c[1] <= -2):
            return False
        if PH.CW_END < x < PH.CW_W - PH.CW_END and PH.CW_D["fore"] < d < PH.CW_D["mouth"]:
            return True
        return any(a < x < b for a, b in (PH.cw_bay_x(i) for i in range(PH.CW_NB))) and PH.CW_D["mouth"] <= d < PH.CW_D["back"]

    def ladders(self):
        out = []
        for c in PH.CW_CONDUITS:                     # down through each forebay hatch
            d = PH.CW_D["fore"] + 1.6 - 0.22
            out.append(((c, -3.8, -d), (c, PH.DECK - PH.SLAB, -d), (0.0, 0.0, 1.0)))
        return out

    def group_solid(self, name):
        return name.startswith("door") or name == "roll"

    def zones(self):
        PH.ZONES.clear()
        PH.circ_ops(level=-1.0)
        PH.circ_building_ops()
        out = list(PH.ZONES)
        PH.ZONES.clear()
        names = [n for n, _ in out]
        assert len(set(names)) == len(names), sorted(n for n in names if names.count(n) > 1)
        return out


def circ_part(compiled):
    """The pump house as a blueprint part: Plant Structure (waterlogged below the forebay's level), water in the forebay
    and bays, air inside the hall and over the water, and the controller. Turned half round, so the towers' side faces
    south (+z) and the condenser's north when the part stands unturned in a plant: its controller faces north."""
    import nuclear_plants as P
    site = CircSite()
    cells = set(compiled["cells"])
    flags = compiled["flags"]
    anchor = compiled["anchor"]
    water, air = set(), set()
    X0, X1 = -2, int(PH.CH_X[1]) + 12
    Z0, Z1 = int(PH.CH_D[0]) - 2, int(PH.CH_D[1]) + 3
    for x in range(X0, X1):
        for zs in range(Z0, Z1):
            gen = WW.mirror_cell((x, 0, zs))
            for y in range(-10, 0):
                if site.basin_water((gen[0], y, gen[2])):
                    water.add((x, y, zs))
            cx, d = gen[0] + 0.5, -(gen[2] + 0.5)
            if PH.CW_END < cx < PH.CW_W - PH.CW_END and PH.CW_D["fore"] < d < PH.CW_D["back"]:
                for y in (-1, 0):
                    air.add((x, y, zs))
            if PH.CH_X[0] <= cx <= PH.CH_X[1] + 10.5 and PH.CH_D[0] <= d <= PH.CH_D[1]:
                for y in range(1, int(PH.CH_RIDGE) + 2):
                    air.add((x, y, zs))
    allc = cells | water | {anchor}
    xs = [c[0] for c in allc]
    ys = [c[1] for c in allc]
    zs_ = [c[2] for c in allc]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs_), max(zs_)
    air = {c for c in air if x0 <= c[0] <= x1 and z0 <= c[2] <= z1 and y0 <= c[1] <= y1}
    p = P.Part("cw_pump_house", "Circulating Water Pump House", x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, kind="pump_house")
    p.sink = -y0

    def at(c):
        return (x1 - c[0], c[1] - y0, z1 - c[2])
    for c in cells:
        if c == anchor:
            continue
        wet = bool(flags.get(c, 0) & 1)
        block = "site_ladder" if flags.get(c, 0) & 2 else "site_structure"
        p.extras[at(c)] = f"jugcraft:{block}[waterlogged={'true' if wet else 'false'}]"
    for c in water - cells - {anchor}:
        p.extras[at(c)] = "minecraft:water"
    for c in air - cells - water - {anchor}:
        p.extras[at(c)] = "minecraft:air"
    for c, st in WW.place_lights(cells, WW.light_spots(site), lambda c: c in air).items():
        p.extras[at(c)] = st
    p.controller = at(anchor)
    p.extras[p.controller] = "jugcraft:water_works_controller[facing=north,works=circ]"
    p.origin = (x1 + 1.0, -y0, z1 + 1.0)
    return p
