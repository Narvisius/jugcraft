"""The medium plant's lake intake as a compiled site (tools/water_works.py): pieces 1 to 5 of
claude/jugcraft-pump-houses.md: the concrete intake, the travelling screens with the trash rake gantry and the screen
wash, the six vertical pumps (make-up A and B, service water 1A, 1B, 2A, 2B) with their discharges and the trains'
strainers, the electrical room with the two trains' 4160 V lineups, MCCs and the local control panel LCP-1, and the
pump hall over the deck.

Live states: each pump running (its motor's fan and coupling guard stripes, gauges, check valve, the MCC's flags and
lamps, the panel's lamps and ammeter; the figures on the panel's displays and the operator screen, by the pumps
together), each discharge valve open or shut, each strainer backwashing, each screen turning (its panels' travel in
twelve steps, its spray, its run lamp), the sluice's water, the hall's lights, the roll-up door's travel, the four
doors, the annunciator's 24 windows. The bays' water is the world's own water blocks.

Site coordinates as the generators draw (x across the intake, west to east; y up from the grass; z = -d, d out toward
the lake), the z mirrored into the site's frame as for every site.
"""
import math

import numpy as np

import cooling_tower as CT
import pump_house as PH
import water_works as WW

RUN0 = (True, True, False, True, False, False)    # MUP-A, SWP-1A, SWP-1B, SWP-2A, SWP-2B, MUP-B: A trains, MU-A
PHASES = 12                                         # the screens' travel: twelve steps of 50 mm across a 0.6 m panel
ROLL = [0.0, 0.2, 0.4, 0.6, 0.8, 1.0]               # the roll-up door's travel
INSTALLED = ("screens", "wash", "pumps")
W = 2 * PH.END + PH.NB * PH.BAY_W + (PH.NB - 1) * PH.PIER
# The controller: under the grass past the deck's back edge, in the middle, where the buried lines leave for the plant.
ANCHOR = (8, -2, 8)

LADDERS = []
_ladder_x = PH._ladder_x


def _recording_ladder(F, x, d, y0, y1, out=-1):
    xo = x + out * 0.18
    o = np.asarray(F.w(1.0, 0.0, 0.0)) - np.asarray(F.w(0.0, 0.0, 0.0))
    LADDERS.append((tuple(F.w(xo, y0 + 0.3, d)), tuple(F.w(xo, y1, d)), tuple(o * out)))
    return _ladder_x(F, x, d, y0, y1, out)


PH._ladder_x = _recording_ladder


def _no_bay_water(ops):
    return [o for o in ops if "ct_water" not in WW._mats(o)]


class IntakeSite(WW.Site):
    key = "lake_intake"

    def __init__(self):
        v = []
        for p in range(6):
            v.append(WW.Var(f"pump{p}", [RUN0[p], not RUN0[p]]))
        for p in range(6):
            v.append(WW.Var(f"mov{p}", [True, False]))
        v += [WW.Var("bw0", [False, True]), WW.Var("bw1", [False, True]), WW.Var("wash", [True, False])]
        for i in range(PH.NB):
            v.append(WW.Var(f"tws{i}", [True, False]))
        for i in range(PH.NB):
            v.append(WW.Var(f"phase{i}", [round(0.05 * k, 3) for k in range(PHASES)], "frames"))
        v += [WW.Var("lights", [False, True]), WW.Var("roll", ROLL, "frames")]
        for n in ("hall_s", "hall_n", "er1", "er2"):
            v.append(WW.Var("door_" + n, [False, True]))
        for k in range(len(PH.ANN)):
            v.append(WW.Var(f"ann{k}", [False, True]))
        self.variables = v
        self._cache = {}
        self._tex = {}

    # ---------------------------------------------------------------- operations, by piece (cached)
    def _piece(self, name, key, make):
        k = (name, key)
        if k not in self._cache:
            PH.ZONES.clear()
            o = make()
            items = WW._keyed(o["still"], False) + WW._keyed(o["lit"], True)
            self._cache[k] = [it for it in items if not WW._excluded(self, it[1])]
        return self._cache[k]

    @staticmethod
    def _open(names):
        PH.OPEN.clear()
        PH.OPEN.update(names)

    def items(self, s):
        run = tuple(s[f"pump{p}"] for p in range(6))
        mov = tuple(s[f"mov{p}"] for p in range(6))
        bw = (s["bw0"], s["bw1"])
        tws = tuple(s[f"tws{i}"] for i in range(PH.NB))
        ph = tuple(s[f"phase{i}"] for i in range(PH.NB))
        out = []

        def intake():
            o = PH.intake_ops(installed=INSTALLED)
            return {"still": _no_bay_water(o["still"]), "lit": _no_bay_water(o["lit"])}
        out += self._piece("intake", (), intake)
        out += self._piece("screens", (tws, ph, s["wash"]),
                           lambda: PH.screens_ops(phase=ph, turning=tws, wash=True, sluice=s["wash"]))
        out += self._piece("pumps", (run, mov, bw), lambda: PH.pumps_ops(running=run, mov_open=mov, bw=bw))
        lit = tuple(PH.ANN[k] for k in range(len(PH.ANN)) if s[f"ann{k}"])
        er = tuple(n for n in ("er1", "er2") if s["door_" + n])

        def elec():
            self._open(er)
            return PH.elec_ops(running=run, ann_lit=lit)
        out += self._piece("elec", (run, lit, er), elec)
        hall = tuple(n for n in ("hall_s", "hall_n") if s["door_" + n])

        def building():
            self._open(hall)
            return PH.building_ops(lights=s["lights"], roll=s["roll"])
        out += self._piece("building", (s["lights"], s["roll"], hall), building)
        self._open(())
        return out

    def ops(self, state):
        raise NotImplementedError

    # ---------------------------------------------------------------- textures
    def texture_vars(self):
        return {f"pump{p}" for p in range(6)}

    def textures(self, state):
        run = tuple(state[f"pump{p}"] for p in range(6))
        if run not in self._tex:
            t = {}
            t.update(CT.ops_textures())
            t.update(PH.intake_textures())
            t.update(PH.screens_textures())
            t.update(PH.pumps_textures(running=run))
            t.update(PH.elec_textures(running=run))
            t.update(PH.building_textures())
            self._tex[run] = t
        return self._tex[run]

    # ---------------------------------------------------------------- cells
    def anchor(self):
        return ANCHOR

    def water(self, c):
        """The bays' water (the lake's level, -1.0) in the intake's walls: every cell of the concrete's outline below the
        water line (cells wholly inside the concrete too, so the water never shows a face against it)."""
        x0, x1 = c[0], c[0] + 1
        d0, d1 = -(c[2] + 1), -c[2]
        if c[1] > -2 or c[1] < -8:
            return False
        return x1 > -0.4 and x0 < W + 0.4 and d1 > PH.D_BACK and d0 < PH.D_FACE + 0.4

    def bay_water(self, c):
        """The cells the part fills with water (inside the bays: the walls' cells are waterlogged structure)."""
        x, d = c[0] + 0.5, -(c[2] + 0.5)
        return -7 <= c[1] <= -2 and PH.END < x < W - PH.END and PH.D_BACK + 0.6 < d < PH.D_FACE

    def ladders(self):
        out = list(dict.fromkeys(LADDERS))
        g = 11.0                                                                    # the gantry's ladder up its leg
        lx, ld = g - 1.72, PH.RAILS_D[1] - 0.48
        out.append(((lx, PH.DECK + 0.3, -ld), (lx, PH.DECK + 5.5, -ld), (0.0, 0.0, 1.0)))
        return out

    def group_solid(self, name):
        return name.startswith("door") or name == "roll"

    def zones(self):
        """The click zones, recorded as the base state's pieces draw them."""
        PH.ZONES.clear()
        s = self.base()
        PH.screens_ops(phase=0.0, turning=(True,) * PH.NB, wash=True, sluice=True)
        PH.pumps_ops(running=RUN0, mov_open=(True,) * 6, bw=(False, False))
        PH.elec_ops(running=RUN0)
        PH.building_ops(lights=False, roll=0.0)
        out = list(PH.ZONES)
        PH.ZONES.clear()
        names = [n for n, _ in out]
        assert len(set(names)) == len(names), sorted(n for n in names if names.count(n) > 1)
        return out


def intake_part(compiled):
    """The intake as a blueprint part (green): Plant Structure blocks (its ladders where climbed; waterlogged below the
    lake's level), water in the bays, air in the bays over the water and through the halls (clearing the ground the
    intake is cut into), and the controller. Turned half round, so its lake side faces north (-z) when the part stands
    unturned in a plant: its controller faces north."""
    import nuclear_plants as P
    site = IntakeSite()
    cells = set(compiled["cells"])
    flags = compiled["flags"]
    anchor = compiled["anchor"]
    water, air = set(), set()
    # (site cells: x, y, z with z = d mirrored: z_site = -z_gen - 1)
    for x in range(-12, int(W) + 3):
        for zs in range(int(PH.D_BACK) - 3, int(PH.D_FACE) + 4):
            for y in range(-9, 16):
                gen = WW.mirror_cell((x, y, zs))
                if site.bay_water(gen):
                    water.add((x, y, zs))
    for x in range(-1, int(W) + 1):                                            # the bays over the water, under the deck
        for zs in range(int(PH.D_BACK), int(PH.D_FACE) + 1):
            for y in (-1, 0):
                gen = WW.mirror_cell((x, y, zs))
                cx, d = gen[0] + 0.5, -(gen[2] + 0.5)
                if PH.END < cx < W - PH.END and PH.D_BACK + 0.6 < d < PH.D_FACE:
                    air.add((x, y, zs))
    # the halls and the electrical room inside (and the deck's space over the lake), clear of the ground
    for x in range(int(PH.ER_X[0]), int(W) + 1):
        for zs in range(int(PH.D_BACK) - 1, int(PH.D_FACE) + 1):
            for y in range(0, int(PH.RIDGE) + 2):
                air.add((x, y, zs))
    allc = cells | water | {anchor}
    xs = [c[0] for c in allc]
    ys = [c[1] for c in allc]
    zs_ = [c[2] for c in allc]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs_), max(zs_)
    air = {c for c in air if x0 <= c[0] <= x1 and z0 <= c[2] <= z1 and y0 <= c[1] <= y1}
    p = P.Part("lake_intake", "Lake Intake and Pump House", x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, kind="intake")
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
    # the halls' and the electrical room's light fittings shine (light blocks under them, in the air the part clears)
    for c, st in WW.place_lights(cells, WW.light_spots(site), lambda c: c in air).items():
        p.extras[at(c)] = st
    p.controller = at(anchor)
    p.extras[p.controller] = "jugcraft:water_works_controller[facing=north,works=intake]"
    # the site frame's origin in the part (the intake's x 0, the curtain wall's line - 10 m), for placing it
    p.origin = (x1 + 1.0, -y0, z1 + 1.0)
    return p
